package com.Ledjr.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

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
     * Saves a transaction AND applies it to its debt, as one database transaction:
     *   payment           -> the debt's balance goes down by the amount
     *   expense on a card -> the card's balance goes up by the amount
     * @Transactional means both writes succeed together or neither happens,
     * so a transaction row can never exist without its balance change (or the other way round).
     */
    @Transactional
    public Transaction record(AppUser user, Category category, Debt debt, String type, BigDecimal amount, LocalDate txnDate, String description){
        if (debt != null) {
            BigDecimal change = "payment".equals(type) ? amount.negate() : amount;
            debt.setBalance(debt.getBalance().add(change));
            debtRepository.save(debt);
            log.info("Debt id# {} balance changed by {} ({})", debt.getId(), change, type);
        }
        return newTransaction(user, category, debt, type, amount, txnDate, description);
    }

    /** debt can be null for a transaction that isn't a debt payment. */
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

    /** Every transaction in one calendar month, newest first. */
    public List<Transaction> findForMonth(Long userId, YearMonth month){
        return transactionRepository.findForPeriodNewestFirst(userId, month.atDay(1), month.atEndOfMonth());
    }

    public List<Transaction> findUncategorized(Long userId){
        return transactionRepository.findByUserIdAndCategoryIsNull(userId);
    }

}
