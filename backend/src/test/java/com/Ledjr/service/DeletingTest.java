package com.Ledjr.service;

import static com.Ledjr.Fakes.fake;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Budget;
import com.Ledjr.entity.Category;
import com.Ledjr.entity.Debt;
import com.Ledjr.entity.RecurringTemplate;
import com.Ledjr.entity.Transaction;
import com.Ledjr.entity.UnallocatedFunds;
import com.Ledjr.repository.AppUserRepository;
import com.Ledjr.repository.BudgetRepository;
import com.Ledjr.repository.CategoryRepository;
import com.Ledjr.repository.DebtRepository;
import com.Ledjr.repository.RecurringTemplateRepository;
import com.Ledjr.repository.TransactionRepository;
import com.Ledjr.repository.UnallocatedFundsRepository;

/** deleting a category, deleting an account, and signing out other devices */
class DeletingTest {

    @Test
    @DisplayName("deleting a category keeps its transactions + recurring bills (now uncategorized) and drops its budgets")
    void deleteCategoryKeepsMoneyData(){
        AppUser user = new AppUser();
        user.setId(7L);
        Category groceries = new Category();
        groceries.setId(1L);
        groceries.setName("Groceries");
        groceries.setUser(user);
        Transaction a = new Transaction(), b = new Transaction();
        a.setCategory(groceries);
        b.setCategory(groceries);
        RecurringTemplate costco = new RecurringTemplate();
        costco.setCategory(groceries);
        List<Object> deletedBudgets = new ArrayList<>();
        List<String> calls = new ArrayList<>();

        CategoryService service = new CategoryService(
            fake(CategoryRepository.class, Map.of(), calls),
            fake(TransactionRepository.class, Map.of("findByCategoryId", args -> List.of(a, b), "save", args -> args[0]), calls),
            fake(RecurringTemplateRepository.class, Map.of("findByCategoryId", args -> List.of(costco), "save", args -> args[0]), calls),
            fake(BudgetRepository.class, Map.of(
                "findByCategoryId", args -> List.of(new Budget(), new Budget()),
                "deleteAll", args -> { ((Iterable<?>) args[0]).forEach(deletedBudgets::add); return null; }), calls));
        service.delete(groceries);

        assertNull(a.getCategory());
        assertNull(b.getCategory());
        assertNull(costco.getCategory());
        assertEquals(2, deletedBudgets.size());
        assertEquals("Category.delete", calls.get(calls.size() - 1), "the category itself goes last");
    }

    @Test
    @DisplayName("deleting an account wipes everything, in foreign key order, with the user last")
    void deleteAccountOrder(){
        AppUser user = new AppUser();
        user.setId(7L);
        List<String> calls = new ArrayList<>();
        AccountService service = new AccountService(
            fake(TransactionRepository.class, Map.of("findByUserId", args -> List.of(new Transaction())), calls),
            fake(BudgetRepository.class, Map.of("findByUserId", args -> List.of(new Budget())), calls),
            fake(RecurringTemplateRepository.class, Map.of("findByUserId", args -> List.of(new RecurringTemplate())), calls),
            fake(DebtRepository.class, Map.of("findByUserId", args -> List.of(new Debt())), calls),
            fake(CategoryRepository.class, Map.of("findByUserId", args -> List.of(new Category())), calls),
            fake(UnallocatedFundsRepository.class, Map.of("findByUserId", args -> Optional.of(new UnallocatedFunds())), calls),
            fake(AppUserRepository.class, Map.of(), calls));
        service.deleteEverything(user);

        List<String> deletes = calls.stream().filter(c -> c.contains(".delete")).toList();
        assertEquals(List.of("Transaction.deleteAll", "Budget.deleteAll", "RecurringTemplate.deleteAll",
            "Debt.deleteAll", "Category.deleteAll", "UnallocatedFunds.delete", "AppUser.delete"), deletes);
    }

    @Test
    @DisplayName("changing your password signs out your other devices but not this one")
    void signOutOtherDevices(){
        Map<String, Session> sessions = new LinkedHashMap<>();
        // the real Session interface has a bunch of methods, the code only ever reads the id
        for (String id : List.of("laptop", "phone", "tablet")) sessions.put(id, fake(Session.class, Map.of("getId", args -> id)));
        List<String> deleted = new ArrayList<>();
        List<Object> lookedUp = new ArrayList<>();
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> repo = fake(FindByIndexNameSessionRepository.class, Map.of(
            "findByPrincipalName", args -> { lookedUp.add(args[0]); return sessions; },
            "deleteById", args -> { deleted.add((String) args[0]); return null; }));
        SessionKiller killer = new SessionKiller(repo);

        assertEquals(2, killer.signOutEverywhereExcept(7L, "laptop"));
        assertEquals(List.of("phone", "tablet"), deleted);
        assertEquals("7", lookedUp.get(0), "sessions are stored under the user id as text");

        deleted.clear();
        killer.signOutEverywhereExcept(7L, null);
        assertEquals(List.of("laptop", "phone", "tablet"), deleted);
    }
}
