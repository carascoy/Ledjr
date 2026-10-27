package com.Ledjr.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** how much you plan to spend in one category for one month (V17) */
@Entity
@Table(name="budget")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="user_id", nullable = false)
    private AppUser user;

    @ManyToOne
    @JoinColumn(name="category_id", nullable = false)
    private Category category;

    /** always the 1st of the month, e.g. 2026-10-01 = October */
    @Column(nullable = false)
    private LocalDate month;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public LocalDate getMonth() {
        return month;
    }

    public void setMonth(LocalDate month) {
        this.month = month;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
