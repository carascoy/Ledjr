package com.Ledjr.dto;

/** PUT /categories/{id}. leave a field null to keep it as is */
public record UpdateCategoryRequest(
    String name,
    Integer colorSlot   // 0-7, see categoryColors.ts
) {}
