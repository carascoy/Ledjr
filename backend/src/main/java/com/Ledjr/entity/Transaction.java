package com.Ledjr.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;

@Entity
@Table(name="transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="user_id", nullable = false)
    private AppUser user;

    @ManyToOne
    @JoinColumn(name="category_id", nullable = true)
    private Category category;

    @ManyToOne
    @JoinColumn(name="debt_id", nullable = true)
    private Debt debt;

    @Column
    private String type;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column
    private LocalDate txnDate;

    @Column
    private String description;

    /** the recurring bill/paycheck this paid, if any (V16) */
    @ManyToOne
    @JoinColumn(name="recurring_template_id", nullable = true)
    private RecurringTemplate recurringTemplate;

    /** which due date of that recurring item it paid, e.g. the Nov 1 rent */
    @Column
    private LocalDate dueDate;

    public RecurringTemplate getRecurringTemplate() {
        return recurringTemplate;
    }

    public void setRecurringTemplate(RecurringTemplate recurringTemplate) {
        this.recurringTemplate = recurringTemplate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Debt getDebt() {
        return debt;
    }

    public void setDebt(Debt debt) {
        this.debt = debt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getTxnDate() {
        return txnDate;
    }

    public void setTxnDate(LocalDate txnDate) {
        this.txnDate = txnDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
