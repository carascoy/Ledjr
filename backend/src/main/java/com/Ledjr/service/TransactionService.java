package com.Ledjr.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Category;
import com.Ledjr.entity.Debt;
import com.Ledjr.entity.Transaction;
import com.Ledjr.repository.DebtRepository;
import com.Ledjr.repository.TransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final DebtRepository debtRepository;
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(TransactionRepository transactionRepository, DebtRepository debtRepository){
        this.transactionRepository = transactionRepository;
        this.debtRepository = debtRepository;
    }

    /**
     * saves the transaction AND updates the debt in one go:
     *   payment           -> debt balance goes down
     *   expense on a card -> card balance goes up
     * @Transactional = both writes happen or neither does, so you never get
     * a transaction saved without the balance changing (or the other way around)
     */
    @Transactional
    public Transaction record(AppUser user, Category category, Debt debt, String type, BigDecimal amount, LocalDate txnDate, String description){
        apply(debt, type, amount);
        return newTransaction(user, category, debt, type, amount, txnDate, description);
    }

    /**
     * edit = undo the old version's effect on its debt, then apply the new one.
     * so changing a $500 payment to $300 puts $200 back on the loan, and moving
     * a charge from one card to another moves the money too. all or nothing again
     */
    @Transactional
    public Transaction update(Transaction t, Category category, Debt debt, String type, BigDecimal amount, LocalDate txnDate, String description){
        undo(t);
        // same debt as before? use the instance we just changed, not a stale copy
        Debt target = debt != null && t.getDebt() != null && t.getDebt().getId().equals(debt.getId()) ? t.getDebt() : debt;
        apply(target, type, amount);
        t.setCategory(category);
        t.setDebt(target);
        t.setType(type);
        t.setAmount(amount);
        t.setTxnDate(txnDate);
        t.setDescription(description);
        transactionRepository.save(t);
        log.info("Transaction id# {} edited", t.getId());
        return t;
    }

    /** delete = undo its effect on the debt first, then remove it */
    @Transactional
    public void delete(Transaction t){
        undo(t);
        transactionRepository.delete(t);
        log.info("Transaction id# {} deleted", t.getId());
    }

    /** what the debt's balance would be if this transaction never happened */
    public BigDecimal balanceWithout(Transaction t, Debt debt){
        if (t != null && t.getDebt() != null && t.getDebt().getId().equals(debt.getId())) {
            return debt.getBalance().subtract(effect(t.getType(), t.getAmount()));
        }
        return debt.getBalance();
    }

    /** payment pushes the balance down, a card charge pushes it up */
    private static BigDecimal effect(String type, BigDecimal amount){
        return "payment".equals(type) ? amount.negate() : amount;
    }

    private void apply(Debt debt, String type, BigDecimal amount){
        if (debt == null) {
            return;
        }
        BigDecimal change = effect(type, amount);
        debt.setBalance(debt.getBalance().add(change));
        debtRepository.save(debt);
        log.info("Debt id# {} balance changed by {} ({})", debt.getId(), change, type);
    }

    /**
     * reverse a transaction's effect on its debt. throws if that would put the balance
     * under $0 (e.g. deleting an old charge on a card you've since paid off), which also
     * rolls back the whole @Transactional method
     */
    private void undo(Transaction t){
        Debt debt = t.getDebt();
        if (debt == null) {
            return;
        }
        BigDecimal back = debt.getBalance().subtract(effect(t.getType(), t.getAmount()));
        if (back.signum() < 0) {
            throw new BalanceWouldGoNegativeException(debt.getCreditorName());
        }
        debt.setBalance(back);
        debtRepository.save(debt);
        log.info("Debt id# {} balance put back to {} (undoing txn #{})", debt.getId(), back, t.getId());
    }

    public Optional<Transaction> findById(Long id){
        if (id == null) {
            return Optional.empty();
        }
        return transactionRepository.findById(id);
    }

    /** thrown by undo(), the controller turns it into a 409 */
    public static class BalanceWouldGoNegativeException extends RuntimeException {
        public BalanceWouldGoNegativeException(String debtName){
            super("Can't do that, " + debtName + " would drop below $0. It's probably been paid down since.");
        }
    }

    /** debt is null if it's not touching a debt */
    public Transaction newTransaction(AppUser user, Category category, Debt debt, String type, BigDecimal amount, LocalDate txnDate, String description){
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setDebt(debt);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setTxnDate(txnDate);
        transaction.setDescription(description);
        transactionRepository.save(transaction);
        log.info("User id# {} logged a {} transaction of {} on {}", user.getId(), type, amount, txnDate);
        return transaction;
    }

    public List<Transaction> findByUserId(Long userId){
        return transactionRepository.findByUserId(userId);
    }

    public List<Transaction> findByCategoryId(Long categoryId){
        return transactionRepository.findByCategoryId(categoryId);
    }

    public List<Transaction> findByDebtId(Long debtId){
        return transactionRepository.findByDebtId(debtId);
    }

    public List<Transaction> findByUserIdBetweenDates(Long userId, LocalDate start, LocalDate end){
        return transactionRepository.findByUserIdAndTxnDateBetween(userId, start, end);
    }

    public List<Transaction> findForMonth(Long userId, YearMonth month){
        return transactionRepository.findForPeriodNewestFirst(userId, month.atDay(1), month.atEndOfMonth());
    }

    public List<Transaction> findUncategorized(Long userId){
        return transactionRepository.findByUserIdAndCategoryIsNull(userId);
    }

}
