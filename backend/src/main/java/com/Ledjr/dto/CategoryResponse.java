package com.Ledjr.dto;

/** What GET/POST /categories return. colorSlot (0-7) picks the tag color on the frontend. */
public record CategoryResponse(
    Long id,
    String name,
    String kind,          // "income" or "expense"
    int colorSlot
) {}
