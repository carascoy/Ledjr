package com.Ledjr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="debt")
public class Debt {

    /** loan = installment debt (% paid off), card = revolving debt (% of limit used) */
    public enum Kind {
        loan, card
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String creditorName;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal balance;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal originalBalance;

    @Column(precision = 5, scale = 2)
    private BigDecimal apr;

    @Column(precision = 10, scale = 2)
    private BigDecimal minPayment;

    @Column
    private Integer dueDate;

    @Column
    private Integer priorityRank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind = Kind.loan;

    /** when it was added (V16). the calendar only counts due dates from then on */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** cards only. utilization = balance / creditLimit */
    @Column(precision = 10, scale = 2)
    private BigDecimal creditLimit;

    /** month you want it paid off by (1st of the month), null = no plan */
    @Column
    private LocalDate payoffTarget;

    public LocalDate getPayoffTarget() {
        return payoffTarget;
    }

    public void setPayoffTarget(LocalDate payoffTarget) {
        this.payoffTarget = payoffTarget;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public String getCreditorName() {
        return creditorName;
    }

    public void setCreditorName(String creditorName) {
        this.creditorName = creditorName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getOriginalBalance() {
        return originalBalance;
    }

    public void setOriginalBalance(BigDecimal originalBalance) {
        this.originalBalance = originalBalance;
    }

    public BigDecimal getApr() {
        return apr;
    }

    public void setApr(BigDecimal apr) {
        this.apr = apr;
    }

    public BigDecimal getMinPayment() {
        return minPayment;
    }

    public void setMinPayment(BigDecimal minPayment) {
        this.minPayment = minPayment;
    }

    public Integer getDueDate() {
        return dueDate;
    }

    public void setDueDate(Integer dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getPriorityRank() {
        return priorityRank;
    }

    public void setPriorityRank(Integer priorityRank) {
        this.priorityRank = priorityRank;
    }

}
