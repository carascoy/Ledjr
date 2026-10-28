package com.Ledjr.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.Ledjr.dto.ChangePasswordRequest;
import com.Ledjr.dto.DeleteAccountRequest;
import com.Ledjr.dto.UpdateProfileRequest;
import com.Ledjr.entity.AppUser;
import com.Ledjr.service.AccountService;
import com.Ledjr.service.AppUserService;
import com.Ledjr.service.LoginAttemptLimiter;
import com.Ledjr.service.SessionKiller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** the Settings page: your name/email, password, deleting your account */
@RestController
@RequestMapping("/me")
public class AccountController {

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 254;

    private final AppUserService appUserService;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptLimiter limiter;
    private final SessionKiller sessionKiller;

    public AccountController(AppUserService appUserService, AccountService accountService, PasswordEncoder passwordEncoder,
                             LoginAttemptLimiter limiter, SessionKiller sessionKiller){
        this.appUserService = appUserService;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.limiter = limiter;
        this.sessionKiller = sessionKiller;
    }

    /**
     * same limit as sign in, so someone at your unlocked laptop can't guess your password here either.
     * null = password's right, go ahead. otherwise the error to send back
     */
    private ResponseEntity<?> checkPassword(AppUser user, String password, HttpServletRequest httpRequest, String wrongMessage){
        String key = "user:" + user.getId();
        String ip = httpRequest.getRemoteAddr();
        long wait = limiter.retryAfterSeconds(key, ip);
        if (wait > 0) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(wait))
                .body(LoginAttemptLimiter.tooManyMessage(wait));
        }
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            limiter.recordFailure(key, ip);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrongMessage);
        }
        limiter.recordSuccess(key);
        return null;
    }

    /** PUT /me - change your name and email */
    @PutMapping
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal Long userId, @RequestBody UpdateProfileRequest request){
        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String first = request.firstName() == null ? "" : request.firstName().trim();
        String last = request.lastName() == null ? "" : request.lastName().trim();
        String email = request.email() == null ? "" : request.email().trim();
        if (first.isEmpty() || last.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("First and last name can't be empty.");
        }
        if (first.length() > MAX_NAME_LENGTH || last.length() > MAX_NAME_LENGTH) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Names must be " + MAX_NAME_LENGTH + " characters or fewer.");
        }
        // just "something@something", a real check would be sending a confirmation email
        if (email.length() > MAX_EMAIL_LENGTH || !email.matches("[^@\\s]+@[^@\\s]+")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("That doesn't look like an email.");
        }
        Optional<AppUser> owner = appUserService.findByEmail(email);
        if (owner.isPresent() && !owner.get().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("That email is already used by another account.");
        }

        AppUser saved = appUserService.updateProfile(user.get(), first, last, email);
        return ResponseEntity.ok(AuthController.toResponse(saved));
    }

    /**
     * PUT /me/password - needs the current one, so someone at your unlocked laptop can't just change it.
     * signs out every other device too: if someone else was logged in as you, changing it kicks them out
     */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal Long userId, @RequestBody ChangePasswordRequest request,
                                            HttpServletRequest httpRequest){
        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ResponseEntity<?> wrong = checkPassword(user.get(), request.currentPassword(), httpRequest, "Current password is wrong.");
        if (wrong != null) return wrong;

        String problem = AuthController.passwordProblem(request.newPassword());
        if (problem != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
        }
        appUserService.changePassword(user.get(), passwordEncoder.encode(request.newPassword()));

        HttpSession current = httpRequest.getSession(false);
        sessionKiller.signOutEverywhereExcept(userId, current == null ? null : current.getId());
        return ResponseEntity.noContent().build();
    }

    /** DELETE /me {password} - deletes the account and everything in it, then logs you out */
    @DeleteMapping
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal Long userId, @RequestBody DeleteAccountRequest request,
                                           HttpServletRequest httpRequest){
        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ResponseEntity<?> wrong = checkPassword(user.get(), request.password(), httpRequest, "Password is wrong.");
        if (wrong != null) return wrong;

        accountService.deleteEverything(user.get());

        // every other device's login goes too, then this one (same as signing out, so the cookie is useless)
        sessionKiller.signOutEverywhereExcept(userId, null);
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
