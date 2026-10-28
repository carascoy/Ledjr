package com.Ledjr.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** the same 3 rules as the sign up form. this is the check that actually counts */
class PasswordRulesTest {

    @Test
    @DisplayName("8+ characters, a number and a special character")
    void rules(){
        assertNull(AuthController.passwordProblem("hunter2!x"));
        assertEquals("Password must be at least 8 characters.", AuthController.passwordProblem("a1!"));
        assertEquals("Password must be at least 8 characters.", AuthController.passwordProblem(null));
        assertEquals("Password needs at least 1 number.", AuthController.passwordProblem("hunter!!x"));
        assertEquals("Password needs at least 1 special character.", AuthController.passwordProblem("hunter22x"));
        assertNull(AuthController.passwordProblem("correct horse battery 9"), "a space counts as special");
    }
}
