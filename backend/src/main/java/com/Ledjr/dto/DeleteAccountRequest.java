package com.Ledjr.dto;

/** your password again, so a session someone else grabbed can't wipe your account */
public record DeleteAccountRequest(
    String password
) {}
