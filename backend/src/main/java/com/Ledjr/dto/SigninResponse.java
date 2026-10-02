package com.Ledjr.dto;

import java.time.LocalDateTime;

public record SigninResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    LocalDateTime createdAt
) {}
