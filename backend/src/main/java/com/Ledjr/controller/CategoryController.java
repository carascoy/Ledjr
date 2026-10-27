package com.Ledjr.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Ledjr.dto.CategoryResponse;
import com.Ledjr.dto.CreateCategoryRequest;
import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Category;
import com.Ledjr.service.AppUserService;
import com.Ledjr.service.CategoryService;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/categories")
public class CategoryController {

    private static final int MAX_NAME_LENGTH = 40;

    private final CategoryService categoryService;
    private final AppUserService appUserService;

    public CategoryController(CategoryService categoryService, AppUserService appUserService){
        this.categoryService = categoryService;
        this.appUserService = appUserService;
    }

    /** GET /categories?userId=1 - user's categories, A to Z */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(@RequestParam Long userId){
        List<CategoryResponse> body = categoryService.findByUserIdSorted(userId)
            .stream()
            .map(CategoryController::toResponse)
            .toList();
        return ResponseEntity.ok(body);
    }

    /** POST /categories - make a category */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateCategoryRequest request){

        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Category name can't be empty.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Category name must be " + MAX_NAME_LENGTH + " characters or fewer.");
        }
        if (!"income".equals(request.kind()) && !"expense".equals(request.kind())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Kind must be income or expense.");
        }

        Optional<AppUser> user = appUserService.findById(request.userId());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        if (categoryService.nameTaken(user.get().getId(), name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("You already have a category called \"" + name + "\".");
        }

        Category saved = categoryService.create(user.get(), name, Category.Kind.valueOf(request.kind()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    /**
     * POST /categories/defaults?userId=1 - gives a user the starter categories.
     * only adds them if they have none yet, so calling it twice is fine.
     * new accounts already get these at sign-up, this is for older accounts
     */
    @PostMapping("/defaults")
    public ResponseEntity<?> seedDefaults(@RequestParam Long userId){
        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
        categoryService.seedDefaults(user.get());
        return list(userId);
    }

    /** entity -> DTO so entities (and the password hash) never leave the server */
    static CategoryResponse toResponse(Category c){
        return new CategoryResponse(c.getId(), c.getName(), c.getKind().name(), c.getColorSlot());
    }
}
