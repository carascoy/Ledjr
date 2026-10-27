package com.Ledjr.dto;

public record CreateCategoryRequest(
    Long userId,          // temporary until sessions (Phase 5)
    String name,
    String kind           // "income" or "expense"
) {}
