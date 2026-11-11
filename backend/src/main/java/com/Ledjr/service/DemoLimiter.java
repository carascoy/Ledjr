package com.Ledjr.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/** at most 5 demos per IP per hour, so nobody can fill the database with them */
@Component
public class DemoLimiter {

    static final int MAX_PER_IP = 5;
    static final Duration WINDOW = Duration.ofHours(1);

    private final Map<String, Deque<Instant>> starts = new HashMap<>();
    private final Clock clock;

    public DemoLimiter(){
        this(Clock.systemUTC());
    }

    DemoLimiter(Clock clock){
        this.clock = clock;
    }

    /** 0 = go ahead (and it counts), otherwise how many seconds until this IP can start another */
    public synchronized long tryStart(String ip){
        Instant now = clock.instant();
        Instant cutoff = now.minus(WINDOW);
        starts.values().forEach(times -> {
            while (!times.isEmpty() && !times.peekFirst().isAfter(cutoff)) times.removeFirst();
        });
        starts.values().removeIf(Deque::isEmpty);

        Deque<Instant> times = starts.computeIfAbsent(ip == null ? "unknown" : ip, k -> new ArrayDeque<>());
        if (times.size() >= MAX_PER_IP) {
            return Math.max(1, Duration.between(now, times.peekFirst().plus(WINDOW)).toSeconds());
        }
        times.addLast(now);
        return 0;
    }
}
