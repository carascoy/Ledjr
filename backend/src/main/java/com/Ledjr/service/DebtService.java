package com.Ledjr.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Debt;
import com.Ledjr.repository.DebtRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DebtService {

    private final DebtRepository debtRepository;
    private static final Logger log = LoggerFactory.getLogger(DebtService.class);

    public DebtService(DebtRepository debtRepository){
        this.debtRepository = debtRepository;
    }

    public Debt newDebt(AppUser user, String creditorName, BigDecimal balance, BigDecimal apr, BigDecimal minPayment, Integer dueDate){
        Debt debt = new Debt();
        debt.setUser(user);
        debt.setCreditorName(creditorName);
        debt.setBalance(balance);
        debt.setApr(apr);
        debt.setMinPayment(minPayment);
        debt.setDueDate(dueDate);
        debtRepository.save(debt);
        log.info("User id# {} added a new debt to {} with balance {}", user.getId(), creditorName, balance);
        return debt;
    }

    public List<Debt> findByUserId(Long userId){
        return debtRepository.findByUserId(userId);
    }

    public List<Debt> findByCreditorName(String creditorName){
        return debtRepository.findByCreditorName(creditorName);
    }

    /** Avalanche order: highest interest rate first. */
    public List<Debt> findByUserIdAvalanche(Long userId){
        return debtRepository.findByUserIdOrderByAprDesc(userId);
    }

    /** Snowball order: smallest balance first. */
    public List<Debt> findByUserIdSnowball(Long userId){
        return debtRepository.findByUserIdOrderByBalanceAsc(userId);
    }

    public List<Debt> findByUserIdPriorityOrder(Long userId){
        return debtRepository.findByUserIdOrderByPriorityRank(userId);
    }

}
