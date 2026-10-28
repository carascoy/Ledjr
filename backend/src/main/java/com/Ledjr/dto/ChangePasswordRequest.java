package com.Ledjr.dto;

public record ChangePasswordRequest(
    String currentPassword,
    String newPassword
) {}
