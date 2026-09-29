package com.Ledjr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;

import java.math.BigDecimal;

@Entity 
@Table(name="debt")
public class Debt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne 
    @JoinColumn(name="app_user", nullable = false)
    private Long user_id;

    @Column(nullable = false)
    private String creditor_name;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal balance;

    @Column(precision = 5, scale = 2)
    private BigDecimal apr;

    @Column(precision = 5, scale = 2)
    private BigDecimal min_payment;

    @Column 
    private int due_date;

    @Column 
    private int priority_rank;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUser_id() {
        return user_id;
    }

    public void setUser_id(Long user_id) {
        this.user_id = user_id;
    }

    public String getCreditor_name() {
        return creditor_name;
    }

    public void setCreditor_name(String creditor_name) {
        this.creditor_name = creditor_name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getApr() {
        return apr;
    }

    public void setApr(BigDecimal apr) {
        this.apr = apr;
    }

    public BigDecimal getMin_payment() {
        return min_payment;
    }

    public void setMin_payment(BigDecimal min_payment) {
        this.min_payment = min_payment;
    }

    public int getDue_date() {
        return due_date;
    }

    public void setDue_date(int due_date) {
        this.due_date = due_date;
    }

    public int getPriority_rank() {
        return priority_rank;
    }

    public void setPriority_rank(int priority_rank) {
        this.priority_rank = priority_rank;
    }

}
