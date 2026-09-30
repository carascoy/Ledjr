package com.Ledjr.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Category;
import com.Ledjr.entity.Debt;
import com.Ledjr.entity.Transaction;
import com.Ledjr.repository.TransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionService {

    private final TransactionRepository transactionRepository;
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(TransactionRepository transactionRepository){
        this.transactionRepository = transactionRepository;
    }

    /** debt can be null for a transaction that isn't a debt payment. */
    public Transaction newTransaction(AppUser user, Category category, Debt debt, String type, BigDecimal amount, LocalDate txnDate, String description){
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setDebt(debt);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setTxn_date(txnDate);
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

    /** NOTE: relies on TransactionRepository.findByUserIdAndTxnDateBetween, which is currently broken - see mistakes list. */
    public List<Transaction> findByUserIdBetweenDates(Long userId, LocalDate start, LocalDate end){
        return transactionRepository.findByUserIdAndTxnDateBetween(userId, start, end);
    }

    public List<Transaction> findUncategorized(Long userId){
        return transactionRepository.findByUserIdAndCategoryIsNull(userId);
    }

}
