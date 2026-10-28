package com.Ledjr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** too many wrong passwords. uses a fake clock so nothing waits 15 real minutes */
class LoginAttemptLimiterTest {

    /** a clock the test moves forward by hand */
    static class FakeClock extends Clock {
        private Instant now = Instant.parse("2026-10-27T20:00:00Z");
        @Override public ZoneId getZone(){ return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone){ return this; }
        @Override public Instant instant(){ return now; }
        void forward(Duration d){ now = now.plus(d); }
    }

    private final FakeClock clock = new FakeClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

    private void fail(int times, String account, String ip){
        for (int i = 0; i < times; i++) limiter.recordFailure(account, ip);
    }

    @Test
    @DisplayName("5 wrong for one account blocks it for 15 minutes, from any IP")
    void blocksAnAccount(){
        fail(4, "yan@x.com", "1.1.1.1");
        assertEquals(0, limiter.retryAfterSeconds("yan@x.com", "1.1.1.1"));
        fail(1, "yan@x.com", "1.1.1.1");
        assertEquals(900, limiter.retryAfterSeconds("yan@x.com", "1.1.1.1"));
        assertTrue(limiter.retryAfterSeconds(" YAN@x.com", "2.2.2.2") > 0, "email case + spaces don't matter");
        assertEquals(0, limiter.retryAfterSeconds("someone-else@x.com", "1.1.1.1"));
    }

    @Test
    @DisplayName("the block runs out 15 minutes after the oldest wrong password")
    void unblocksAfterTheWindow(){
        fail(5, "yan@x.com", "1.1.1.1");
        clock.forward(Duration.ofMinutes(10));
        assertEquals(300, limiter.retryAfterSeconds("yan@x.com", "1.1.1.1"));
        clock.forward(Duration.ofMinutes(5));
        assertEquals(0, limiter.retryAfterSeconds("yan@x.com", "1.1.1.1"));
    }

    @Test
    @DisplayName("the right password resets that account's count")
    void successResets(){
        fail(4, "a@x.com", "3.3.3.3");
        limiter.recordSuccess("a@x.com");
        fail(1, "a@x.com", "3.3.3.3");
        assertEquals(0, limiter.retryAfterSeconds("a@x.com", "3.3.3.3"));
    }

    @Test
    @DisplayName("20 wrong from one IP across lots of accounts blocks that IP")
    void blocksAnIpSprayingAccounts(){
        for (int i = 0; i < 19; i++) limiter.recordFailure("user" + i + "@x.com", "6.6.6.6");
        assertEquals(0, limiter.retryAfterSeconds("new@x.com", "6.6.6.6"));
        limiter.recordFailure("user19@x.com", "6.6.6.6");
        assertTrue(limiter.retryAfterSeconds("new@x.com", "6.6.6.6") > 0);
        assertEquals(0, limiter.retryAfterSeconds("new@x.com", "7.7.7.7"));
    }

    @Test
    @DisplayName("the message rounds up to whole minutes")
    void message(){
        assertEquals("Too many wrong passwords. Try again in 15 minutes.", LoginAttemptLimiter.tooManyMessage(900));
        assertEquals("Too many wrong passwords. Try again in 2 minutes.", LoginAttemptLimiter.tooManyMessage(61));
        assertEquals("Too many wrong passwords. Try again in 1 minute.", LoginAttemptLimiter.tooManyMessage(5));
    }
}
