package com.Ledjr.dto;

public record CreateCategoryRequest(
    String name,
    String kind           // "income" or "expense"
) {}
