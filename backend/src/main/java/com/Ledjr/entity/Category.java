package com.Ledjr.entity;

import java.math.BigDecimal;

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

@Entity
@Table(name="category")
public class Category {

    public enum Kind {
        income, expense, debt
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Column(precision = 10, scale = 2)
    private BigDecimal budgetedAmount;

    /** which of the 8 palette colors the tag uses (0-7). set once when it's made */
    @Column(nullable = false)
    private Integer colorSlot;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public BigDecimal getBudgetedAmount() {
        return budgetedAmount;
    }

    public void setBudgetedAmount(BigDecimal budgetedAmount) {
        this.budgetedAmount = budgetedAmount;
    }

    public Integer getColorSlot() {
        return colorSlot;
    }

    public void setColorSlot(Integer colorSlot) {
        this.colorSlot = colorSlot;
    }

}
