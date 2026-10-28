package com.Ledjr.service;

import static com.Ledjr.Fakes.fake;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.PasswordResetToken;
import com.Ledjr.repository.AppUserRepository;
import com.Ledjr.repository.PasswordResetTokenRepository;

/** forgot password links: work once, run out, only the newest works, and never sit in the database as-is */
class PasswordResetServiceTest {

    static class FakeClock extends Clock {
        Instant now = Instant.parse("2026-10-27T20:00:00Z");
        @Override public ZoneId getZone(){ return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone){ return this; }
        @Override public Instant instant(){ return now; }
    }

    private final FakeClock clock = new FakeClock();
    private final List<PasswordResetToken> saved = new ArrayList<>();
    private final List<String> emails = new ArrayList<>();
    private final List<String> signedOut = new ArrayList<>();
    private AppUser yan;
    private PasswordResetService service;

    @BeforeEach
    void setUp(){
        yan = new AppUser();
        yan.setId(7L);
        yan.setEmail("yan@x.com");
        yan.setFirstName("Yan");
        yan.setPasswordHash("hashed:old");

        PasswordResetTokenRepository tokens = fake(PasswordResetTokenRepository.class, Map.of(
            "save", a -> { PasswordResetToken t = (PasswordResetToken) a[0]; if (!saved.contains(t)) saved.add(t); return t; },
            "findByTokenHash", a -> saved.stream().filter(t -> t.getTokenHash().equals(a[0])).findFirst(),
            "findByUserIdAndUsedAtIsNull", a -> saved.stream().filter(t -> t.getUsedAt() == null).toList()));
        AppUserRepository users = fake(AppUserRepository.class, Map.of(
            "findByEmail", a -> "yan@x.com".equals(a[0]) ? Optional.of(yan) : Optional.empty(),
            "save", a -> a[0]));
        EmailSender email = (to, subject, text) -> emails.add(to + "\n" + text);
        Map<String, Session> sessions = new LinkedHashMap<>();
        sessions.put("laptop", fake(Session.class, Map.of("getId", a -> "laptop")));
        sessions.put("phone", fake(Session.class, Map.of("getId", a -> "phone")));
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> sessionRepo = fake(FindByIndexNameSessionRepository.class, Map.of(
            "findByPrincipalName", a -> sessions,
            "deleteById", a -> { signedOut.add((String) a[0]); return null; }));
        PasswordEncoder encoder = fake(PasswordEncoder.class, Map.of("encode", a -> "hashed:" + a[0]));

        service = new PasswordResetService(tokens, users, email, new SessionKiller(sessionRepo), encoder,
            "https://ledjr.dev", clock);
    }

    /** pulls the code out of the last email's link */
    private String codeFromEmail(){
        Matcher m = Pattern.compile("https://ledjr\\.dev/reset-password\\?token=([A-Za-z0-9_-]+)").matcher(emails.get(emails.size() - 1));
        assertTrue(m.find(), "the email has a reset link");
        return m.group(1);
    }

    @Test
    @DisplayName("asking for a reset emails a link, and the database only has a hash of the code")
    void emailsALink(){
        service.requestReset(" yan@x.com ");
        assertEquals(1, emails.size());
        assertTrue(emails.get(0).startsWith("yan@x.com\n"));
        String code = codeFromEmail();
        assertEquals(43, code.length());
        assertEquals(1, saved.size());
        assertFalse(saved.get(0).getTokenHash().contains(code), "the real code is never stored");
        assertEquals(PasswordResetService.hash(code), saved.get(0).getTokenHash());
    }

    @Test
    @DisplayName("an email with no account: no link, nothing saved (and the controller answers the same either way)")
    void unknownEmail(){
        service.requestReset("nobody@x.com");
        assertEquals(0, emails.size());
        assertEquals(0, saved.size());
    }

    @Test
    @DisplayName("a good link sets the new password and signs out every device")
    void resetWorks(){
        service.requestReset("yan@x.com");
        String code = codeFromEmail();
        assertTrue(service.isValid(code));
        assertTrue(service.resetPassword(code, "new!pass99").isPresent());
        assertEquals("hashed:new!pass99", yan.getPasswordHash());
        assertEquals(List.of("laptop", "phone"), signedOut);
    }

    @Test
    @DisplayName("a link only works once")
    void onlyOnce(){
        service.requestReset("yan@x.com");
        String code = codeFromEmail();
        assertTrue(service.resetPassword(code, "new!pass99").isPresent());
        assertFalse(service.isValid(code));
        assertTrue(service.resetPassword(code, "another!1").isEmpty());
        assertEquals("hashed:new!pass99", yan.getPasswordHash());
    }

    @Test
    @DisplayName("a link runs out after 30 minutes")
    void expires(){
        service.requestReset("yan@x.com");
        String code = codeFromEmail();
        clock.now = clock.now.plus(Duration.ofMinutes(29));
        assertTrue(service.isValid(code));
        clock.now = clock.now.plus(Duration.ofMinutes(1));
        assertFalse(service.isValid(code));
        assertTrue(service.resetPassword(code, "new!pass99").isEmpty());
        assertEquals("hashed:old", yan.getPasswordHash());
    }

    @Test
    @DisplayName("asking again kills the older link, only the newest one works")
    void newestLinkWins(){
        service.requestReset("yan@x.com");
        String first = codeFromEmail();
        service.requestReset("yan@x.com");
        String second = codeFromEmail();
        assertFalse(first.equals(second));
        assertFalse(service.isValid(first));
        assertTrue(service.isValid(second));
    }

    @Test
    @DisplayName("made up or empty codes don't work")
    void garbageCodes(){
        service.requestReset("yan@x.com");
        assertFalse(service.isValid("not-a-real-code"));
        assertFalse(service.isValid(""));
        assertFalse(service.isValid(null));
    }
}
