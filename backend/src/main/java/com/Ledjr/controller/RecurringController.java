package com.Ledjr.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.Ledjr.dto.CalendarItem;
import com.Ledjr.dto.PayRecurringRequest;
import com.Ledjr.dto.RecurringRequest;
import com.Ledjr.dto.RecurringResponse;
import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Category;
import com.Ledjr.entity.RecurringTemplate;
import com.Ledjr.service.AppUserService;
import com.Ledjr.service.CalendarService;
import com.Ledjr.service.CategoryService;
import com.Ledjr.service.RecurringTemplateService;
import com.Ledjr.service.TransactionService;

/** recurring bills + paychecks, and the calendar that shows them */
@RestController
public class RecurringController {

    private static final int MAX_NAME_LENGTH = 60;
    // NUMERIC(10,2) maxes out at 99,999,999.99
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");
    // a calendar month view needs ~6 weeks, give it some room but don't let someone ask for 10 years
    private static final long MAX_CALENDAR_DAYS = 100;

    private final RecurringTemplateService recurringTemplateService;
    private final CalendarService calendarService;
    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final AppUserService appUserService;

    public RecurringController(RecurringTemplateService recurringTemplateService, CalendarService calendarService,
                               TransactionService transactionService, CategoryService categoryService, AppUserService appUserService){
        this.recurringTemplateService = recurringTemplateService;
        this.calendarService = calendarService;
        this.transactionService = transactionService;
        this.categoryService = categoryService;
        this.appUserService = appUserService;
    }

    /** GET /recurring - your recurring bills + paychecks, A to Z */
    @GetMapping("/recurring")
    public ResponseEntity<List<RecurringResponse>> list(@AuthenticationPrincipal Long userId){
        LocalDate today = LocalDate.now();
        List<RecurringResponse> body = recurringTemplateService.findByUserIdSorted(userId)
            .stream()
            .map(r -> toResponse(r, today))
            .toList();
        return ResponseEntity.ok(body);
    }

    /** POST /recurring - add one */
    @PostMapping("/recurring")
    public ResponseEntity<?> create(@AuthenticationPrincipal Long userId, @RequestBody RecurringRequest request){
        return save(userId, request, null);
    }

    /** PUT /recurring/{id} - edit it (changes every unpaid due date) */
    @PutMapping("/recurring/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal Long userId, @PathVariable Long id, @RequestBody RecurringRequest request){
        Optional<RecurringTemplate> existing = findOwned(id, userId);
        if (existing.isEmpty()) {
            return notFound();
        }
        return save(userId, request, existing.get());
    }

    /** DELETE /recurring/{id} - stops it. transactions it already paid stay */
    @DeleteMapping("/recurring/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id){
        Optional<RecurringTemplate> existing = findOwned(id, userId);
        if (existing.isEmpty()) {
            return notFound();
        }
        recurringTemplateService.delete(existing.get());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /recurring/{id}/pay - "mark paid" on the calendar.
     * logs a real transaction (so it shows in the charts) tied to that due date.
     * undo = delete that transaction (DELETE /transactions/{id})
     */
    @PostMapping("/recurring/{id}/pay")
    public ResponseEntity<?> pay(@AuthenticationPrincipal Long userId, @PathVariable Long id, @RequestBody PayRecurringRequest request){
        Optional<RecurringTemplate> found = findOwned(id, userId);
        if (found.isEmpty()) {
            return notFound();
        }
        RecurringTemplate template = found.get();

        LocalDate due = request.dueDate();
        // has to be a real due date of this item, not just any day
        if (due == null || !RecurringTemplateService.occurrences(template, due, due).contains(due)) {
            return badRequest("That's not one of " + template.getName() + "'s due dates.");
        }
        if (transactionService.isRecurringPaid(template.getId(), due)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(template.getName() + " is already marked paid for that day.");
        }

        BigDecimal amount = request.amount() != null ? request.amount() : template.getAmount();
        if (amount.signum() <= 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            return badRequest("Amount must be more than $0.");
        }

        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        // marking an old one paid = it happened on its due date. paying ahead = it happened today.
        // that keeps it in the right month on the charts
        LocalDate today = LocalDate.now();
        LocalDate txnDate = due.isAfter(today) ? today : due;

        transactionService.recordRecurring(user.get(), template, due, amount, txnDate);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** GET /calendar?from=2026-09-27&to=2026-11-07 - everything due in that range, paid or not */
    @GetMapping("/calendar")
    public ResponseEntity<?> calendar(@AuthenticationPrincipal Long userId, @RequestParam String from, @RequestParam String to){
        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(from);
            end = LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            return badRequest("Dates must look like 2026-10-27.");
        }
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > MAX_CALENDAR_DAYS) {
            return badRequest("Pick a range of up to " + MAX_CALENDAR_DAYS + " days.");
        }
        List<CalendarItem> body = calendarService.itemsBetween(userId, start, end);
        return ResponseEntity.ok(body);
    }

    /** create and edit share the same checks. existing = null means create */
    private ResponseEntity<?> save(Long userId, RecurringRequest request, RecurringTemplate existing){
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            return badRequest("Give it a name.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return badRequest("Name must be " + MAX_NAME_LENGTH + " characters or fewer.");
        }

        RecurringTemplate.Type type;
        if ("income".equals(request.type())) {
            type = RecurringTemplate.Type.income;
        } else if ("expense".equals(request.type())) {
            type = RecurringTemplate.Type.expense;
        } else {
            return badRequest("Type must be income or expense.");
        }

        if (request.amount() == null || request.amount().signum() <= 0 || request.amount().compareTo(MAX_AMOUNT) > 0) {
            return badRequest("Amount must be more than $0.");
        }

        // the table also allows daily, but nothing real repeats daily so the app doesn't offer it
        RecurringTemplate.Frequency frequency;
        try {
            frequency = RecurringTemplate.Frequency.valueOf(request.frequency() == null ? "" : request.frequency());
        } catch (IllegalArgumentException e) {
            frequency = null;
        }
        if (frequency == null || frequency == RecurringTemplate.Frequency.daily) {
            return badRequest("Repeats must be weekly, biweekly, monthly or annually.");
        }

        if (request.startDate() == null) {
            return badRequest("Pick the first due date.");
        }

        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        // category is optional, but has to be theirs and match income/expense
        Category category = null;
        if (request.categoryId() != null) {
            Optional<Category> found = categoryService.findById(request.categoryId());
            // same 404 if it doesn't exist or isn't theirs
            if (found.isEmpty() || !found.get().getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Category not found.");
            }
            category = found.get();
            if (!category.getKind().name().equals(type.name())) {
                return badRequest("\"" + category.getName() + "\" is an " + category.getKind().name() + " category.");
            }
        }

        if (existing == null) {
            RecurringTemplate saved = recurringTemplateService.newRecurringTemplate(
                user.get(), category, type, name, request.amount(), frequency, request.startDate());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved, LocalDate.now()));
        }
        RecurringTemplate saved = recurringTemplateService.update(
            existing, category, type, name, request.amount(), frequency, request.startDate());
        return ResponseEntity.ok(toResponse(saved, LocalDate.now()));
    }

    /** same 404 if it doesn't exist or isn't theirs */
    private Optional<RecurringTemplate> findOwned(Long id, Long userId){
        return recurringTemplateService.findById(id)
            .filter(r -> r.getUser().getId().equals(userId));
    }

    private static ResponseEntity<String> notFound(){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recurring item not found.");
    }

    private static ResponseEntity<String> badRequest(String message){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    /** entity -> DTO so entities (and the password hash) never leave the server */
    private static RecurringResponse toResponse(RecurringTemplate r, LocalDate today){
        Category c = r.getCategory();
        return new RecurringResponse(
            r.getId(), r.getType().name(), r.getName(), r.getAmount(), r.getFrequency().name(),
            r.getStartDate(), RecurringTemplateService.nextDue(r, today),
            c == null ? null : c.getId(),
            c == null ? null : c.getName(),
            c == null ? null : c.getColorSlot());
    }
}
