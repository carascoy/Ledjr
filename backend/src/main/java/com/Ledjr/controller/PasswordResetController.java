package com.Ledjr.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Ledjr.dto.ForgotPasswordRequest;
import com.Ledjr.dto.ResetPasswordRequest;
import com.Ledjr.entity.AppUser;
import com.Ledjr.service.LoginAttemptLimiter;
import com.Ledjr.service.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;

/** forgot password. these work without being signed in (see SecurityConfig) */
@RestController
@RequestMapping("/password")
public class PasswordResetController {

    private static final String BAD_LINK = "This reset link doesn't work anymore. It might have expired or already been used, ask for a new one.";

    private final PasswordResetService resetService;
    private final LoginAttemptLimiter limiter;

    public PasswordResetController(PasswordResetService resetService, LoginAttemptLimiter limiter){
        this.resetService = resetService;
        this.limiter = limiter;
    }

    /**
     * POST /password/forgot {email} - emails a reset link if there's an account.
     * always the same 204 whether there is or not, so this can't be used to check who has an account.
     * capped like wrong passwords (5 per email / 20 per IP in 15 min) so nobody can spam someone's inbox
     */
    @PostMapping("/forgot")
    public ResponseEntity<?> forgot(@RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest){
        String key = "reset:" + (request.email() == null ? "" : request.email().trim().toLowerCase());
        String ip = httpRequest.getRemoteAddr();
        long wait = limiter.retryAfterSeconds(key, ip);
        if (wait > 0) {
            long minutes = Math.max(1, (wait + 59) / 60);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(wait))
                .body("Too many reset requests. Try again in " + minutes + (minutes == 1 ? " minute." : " minutes."));
        }
        limiter.recordFailure(key, ip);
        resetService.requestReset(request.email());
        return ResponseEntity.noContent().build();
    }

    /** GET /password/reset/check?token=... - 204 if the link still works, 400 if not. lets the page say so before you type */
    @GetMapping("/reset/check")
    public ResponseEntity<?> check(@RequestParam String token){
        if (!resetService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BAD_LINK);
        }
        return ResponseEntity.noContent().build();
    }

    /** POST /password/reset {token, newPassword} - sets the new password, then you sign in with it */
    @PostMapping("/reset")
    public ResponseEntity<?> reset(@RequestBody ResetPasswordRequest request){
        String problem = AuthController.passwordProblem(request.newPassword());
        if (problem != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
        }
        Optional<AppUser> user = resetService.resetPassword(request.token(), request.newPassword());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BAD_LINK);
        }
        // they proved they own the email, so clear any "too many wrong passwords" block on sign in
        limiter.recordSuccess(user.get().getEmail());
        return ResponseEntity.noContent().build();
    }
}
