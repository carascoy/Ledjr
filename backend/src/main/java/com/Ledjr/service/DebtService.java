package com.Ledjr.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.Ledjr.dto.DebtSummary;
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

    /**
     * originalBalance = what the debt started at. Pass null to use today's balance
     * (i.e. "I'm starting to track this now"), which means 0% paid off so far.
     */
    public Debt newDebt(AppUser user, Debt.Kind kind, String creditorName, BigDecimal balance, BigDecimal originalBalance,
                        BigDecimal creditLimit, BigDecimal apr, BigDecimal minPayment, Integer dueDate){
        Debt debt = new Debt();
        debt.setUser(user);
        debt.setKind(kind);
        debt.setCreditLimit(kind == Debt.Kind.card ? creditLimit : null);
        debt.setCreditorName(creditorName);
        debt.setBalance(balance);
        debt.setOriginalBalance(originalBalance != null ? originalBalance : balance);
        debt.setApr(apr);
        debt.setMinPayment(minPayment);
        debt.setDueDate(dueDate);
        debtRepository.save(debt);
        log.info("User id# {} added a new debt to {} with balance {}", user.getId(), creditorName, balance);
        return debt;
    }

    public Optional<Debt> findById(Long id){
        if (id == null) {
            return Optional.empty();
        }
        return debtRepository.findById(id);
    }

    public List<Debt> findByUserId(Long userId){
        return debtRepository.findByUserId(userId);
    }

    /** The user's debts, biggest balance first. */
    public List<Debt> findByUserIdLargestFirst(Long userId){
        return debtRepository.findByUserId(userId).stream()
            .sorted(Comparator.comparing(Debt::getBalance).reversed())
            .toList();
    }

    /** Totals for the dashboard: loans as "% paid off", cards as "% of limit used". */
    public DebtSummary getSummary(Long userId){
        List<Debt> debts = debtRepository.findByUserId(userId);
        List<Debt> loans = debts.stream().filter(d -> d.getKind() == Debt.Kind.loan).toList();
        List<Debt> cards = debts.stream().filter(d -> d.getKind() == Debt.Kind.card).toList();

        BigDecimal loanOriginal = sum(loans.stream().map(Debt::getOriginalBalance).toList());
        BigDecimal loanRemaining = sum(loans.stream().map(Debt::getBalance).toList());
        // paid / started-at; clamped because interest can push a balance above where it started
        int loanPercentPaid = clamp(percent(loanOriginal.subtract(loanRemaining), loanOriginal), 0, 100);

        BigDecimal cardBalance = sum(cards.stream().map(Debt::getBalance).toList());
        BigDecimal cardLimit = sum(cards.stream().map(Debt::getCreditLimit).toList());
        // not clamped at 100: being over the limit is worth seeing
        int cardPercentUsed = clamp(percent(cardBalance, cardLimit), 0, 999);

        return new DebtSummary(
            loanRemaining.add(cardBalance), debts.size(),
            loans.size(), loanOriginal, loanRemaining, loanPercentPaid,
            cards.size(), cardBalance, cardLimit, cardPercentUsed);
    }

    private static BigDecimal sum(List<BigDecimal> values){
        return values.stream()
            .filter(v -> v != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** part / whole as a rounded whole-number percent; 0 when whole is 0. */
    private static int percent(BigDecimal part, BigDecimal whole){
        if (whole.signum() <= 0) {
            return 0;
        }
        return part.multiply(BigDecimal.valueOf(100))
            .divide(whole, 0, RoundingMode.HALF_UP)
            .intValue();
    }

    private static int clamp(int value, int min, int max){
        return Math.max(min, Math.min(max, value));
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
