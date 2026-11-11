package com.Ledjr.dto;

import java.time.LocalDateTime;

public record SigninResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    LocalDateTime createdAt,
    boolean setupDone,      // false = show the optional setup
    boolean demo            // true = a "Try the demo" account, the frontend shows the demo bar
) {}
