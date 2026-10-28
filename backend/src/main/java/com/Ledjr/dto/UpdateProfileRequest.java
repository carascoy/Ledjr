package com.Ledjr.dto;

public record UpdateProfileRequest(
    String firstName,
    String lastName,
    String email
) {}
