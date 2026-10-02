package com.Ledjr.dto;

public record SigninResponse(
    Long id,
    String email,
    String firstName,
    String lastName
) {}
