package com.Ledjr.dto;

public record CreateCategoryRequest(
    Long userId,          // temp until sessions (phase 5)
    String name,
    String kind           // "income" or "expense"
) {}
