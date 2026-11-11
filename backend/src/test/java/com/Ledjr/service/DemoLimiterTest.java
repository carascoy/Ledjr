package com.Ledjr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DemoLimiterTest {

    private final LoginAttemptLimiterTest.FakeClock clock = new LoginAttemptLimiterTest.FakeClock();
    private final DemoLimiter limiter = new DemoLimiter(clock);

    @Test
    @DisplayName("5 demos from one IP, then the 6th waits")
    void sixthWaits(){
        for (int i = 0; i < DemoLimiter.MAX_PER_IP; i++) {
            assertEquals(0, limiter.tryStart("1.1.1.1"));
        }
        assertTrue(limiter.tryStart("1.1.1.1") > 0);
    }

    @Test
    @DisplayName("another IP isn't affected")
    void otherIpsAreFine(){
        for (int i = 0; i < DemoLimiter.MAX_PER_IP; i++) limiter.tryStart("1.1.1.1");
        assertEquals(0, limiter.tryStart("2.2.2.2"));
    }

    @Test
    @DisplayName("waits until the oldest one is an hour old")
    void waitTime(){
        limiter.tryStart("1.1.1.1");
        clock.forward(Duration.ofMinutes(20));
        for (int i = 1; i < DemoLimiter.MAX_PER_IP; i++) limiter.tryStart("1.1.1.1");
        assertEquals(Duration.ofMinutes(40).toSeconds(), limiter.tryStart("1.1.1.1"));
        clock.forward(Duration.ofMinutes(40));
        assertEquals(0, limiter.tryStart("1.1.1.1"));
    }

    @Test
    @DisplayName("being blocked doesn't push the wait back further")
    void blockedTriesDontCount(){
        for (int i = 0; i < DemoLimiter.MAX_PER_IP; i++) limiter.tryStart("1.1.1.1");
        for (int i = 0; i < 10; i++) limiter.tryStart("1.1.1.1");
        clock.forward(DemoLimiter.WINDOW);
        assertEquals(0, limiter.tryStart("1.1.1.1"));
    }
}
