package com.Ledjr.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.PasswordResetToken;
import com.Ledjr.repository.AppUserRepository;
import com.Ledjr.repository.PasswordResetTokenRepository;

/**
 * forgot password. the link has a random code in it; we only keep a hash of that code,
 * so it works like a one-time password that runs out after 30 minutes
 */
@Service
public class PasswordResetService {

    static final Duration LINK_LIFETIME = Duration.ofMinutes(30);

    private final PasswordResetTokenRepository tokens;
    private final AppUserRepository users;
    private final EmailSender emailSender;
    private final SessionKiller sessionKiller;
    private final PasswordEncoder passwordEncoder;
    private final String frontendOrigin;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    @Autowired
    public PasswordResetService(PasswordResetTokenRepository tokens, AppUserRepository users, EmailSender emailSender,
                                SessionKiller sessionKiller, PasswordEncoder passwordEncoder,
                                @Value("${app.frontend-origin}") String frontendOrigin){
        this(tokens, users, emailSender, sessionKiller, passwordEncoder, frontendOrigin, Clock.systemDefaultZone());
    }

    /** tests pass a fake clock so they can jump past the 30 minutes */
    PasswordResetService(PasswordResetTokenRepository tokens, AppUserRepository users, EmailSender emailSender,
                         SessionKiller sessionKiller, PasswordEncoder passwordEncoder, String frontendOrigin, Clock clock){
        this.tokens = tokens;
        this.users = users;
        this.emailSender = emailSender;
        this.sessionKiller = sessionKiller;
        this.passwordEncoder = passwordEncoder;
        this.frontendOrigin = frontendOrigin;
        this.clock = clock;
    }

    /**
     * emails a reset link if there's an account with that email. if there isn't, does nothing,
     * and the controller sends back the same answer either way so nobody can check who has an account
     */
    @Transactional
    public void requestReset(String email){
        if (email == null) return;
        Optional<AppUser> found = users.findByEmail(email.trim());
        if (found.isEmpty()) return;
        AppUser user = found.get();

        // only the newest link works
        expireOpenLinks(user.getId());

        String code = newCode();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash(code));
        token.setExpiresAt(now().plus(LINK_LIFETIME));
        tokens.save(token);

        String link = frontendOrigin + "/reset-password?token=" + code;
        emailSender.send(user.getEmail(), "Reset your Ledjr password",
            "Hi " + user.getFirstName() + ",\n\n"
            + "Someone (hopefully you) asked to reset your Ledjr password. Open this link to pick a new one:\n\n"
            + link + "\n\n"
            + "It works once and runs out in 30 minutes. If you didn't ask for this, ignore this email, your password stays the same.");
        log.info("User id# {} asked for a password reset link", user.getId());
    }

    /** is this link still good? (the reset page checks before showing the form) */
    public boolean isValid(String code){
        return findUsable(code).isPresent();
    }

    /**
     * sets the new password if the link is still good. the link stops working, any other links stop
     * working, and every device gets signed out. empty = bad or expired link
     */
    @Transactional
    public Optional<AppUser> resetPassword(String code, String newPassword){
        Optional<PasswordResetToken> found = findUsable(code);
        if (found.isEmpty()) return Optional.empty();

        AppUser user = found.get().getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        expireOpenLinks(user.getId());
        sessionKiller.signOutEverywhereExcept(user.getId(), null);
        log.info("User id# {} reset their password with a link", user.getId());
        return Optional.of(user);
    }

    private Optional<PasswordResetToken> findUsable(String code){
        if (code == null || code.isBlank()) return Optional.empty();
        return tokens.findByTokenHash(hash(code))
            .filter(t -> t.getUsedAt() == null)
            .filter(t -> now().isBefore(t.getExpiresAt()));
    }

    private void expireOpenLinks(Long userId){
        for (PasswordResetToken t : tokens.findByUserIdAndUsedAtIsNull(userId)) {
            t.setUsedAt(now());
            tokens.save(t);
        }
    }

    /** 32 random bytes as url-safe text (43 characters). that's way too many to guess */
    private String newCode(){
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256, not bcrypt like passwords: the code is already fully random so there's nothing to
     * brute force, and a plain hash lets us look it up directly in the database
     */
    static String hash(String code){
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("every java has SHA-256", e);
        }
    }

    private LocalDateTime now(){
        return LocalDateTime.now(clock);
    }
}
