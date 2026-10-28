package com.Ledjr.dto;

public record ResetPasswordRequest(
    String token,         // the code from the link
    String newPassword
) {}
