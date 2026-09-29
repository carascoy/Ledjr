package com.Ledjr.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;

@Entity 
@Table(name="recurring_template")
public class RecurringTemplate {

    public enum Frequency {
        daily, weekly, biweekly, monthly, annually,
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne 
    @JoinColumn(name="app_user", nullable = false)
    private Long user_id;

    @ManyToOne 
    @JoinColumn(name="category", nullable = false)
    private Long category_id;

    @Column(nullable = false)
    private String name;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(nullable = false) 
    private LocalDate date;
}
