package com.Ledjr.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * stops password guessing. counts wrong passwords per account and per IP address in the last 15 min:
 * 5 wrong for one account, or 20 wrong from one IP (someone trying lots of accounts), and that's
 * blocked until the oldest failure is 15 min old. a right password clears the account's count.
 *
 * kept in memory, so it resets when the server restarts and only works with one server running.
 * fine for this, a bigger app would keep it in redis or the database
 */
@Component
public class LoginAttemptLimiter {

    static final int MAX_PER_ACCOUNT = 5;
    static final int MAX_PER_IP = 20;
    static final Duration WINDOW = Duration.ofMinutes(15);
    /** past this many keys, sweep out the expired ones so the map can't grow forever */
    private static final int SWEEP_AT = 10_000;

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptLimiter(){
        this(Clock.systemUTC());
    }

    /** tests pass a fake clock so they don't have to wait 15 minutes */
    LoginAttemptLimiter(Clock clock){
        this.clock = clock;
    }

    /** how many seconds until they can try again. 0 = go ahead */
    public long retryAfterSeconds(String account, String ip){
        return Math.max(
            waitFor(accountKey(account), MAX_PER_ACCOUNT),
            waitFor(ipKey(ip), MAX_PER_IP));
    }

    public void recordFailure(String account, String ip){
        Instant now = clock.instant();
        add(accountKey(account), now);
        add(ipKey(ip), now);
        if (failures.size() > SWEEP_AT) {
            failures.keySet().forEach(key -> failures.computeIfPresent(key, (k, times) -> prune(times, now)));
        }
    }

    /** right password: that account starts fresh. the IP count stays, so one good login can't reset a spray */
    public void recordSuccess(String account){
        failures.remove(accountKey(account));
    }

    private long waitFor(String key, int max){
        Instant now = clock.instant();
        Deque<Instant> times = failures.computeIfPresent(key, (k, t) -> prune(t, now));
        if (times == null || times.size() < max) return 0;
        synchronized (times) {
            // blocked until the oldest one in the window ages out
            return Math.max(1, Duration.between(now, times.peekFirst().plus(WINDOW)).toSeconds());
        }
    }

    private void add(String key, Instant now){
        failures.compute(key, (k, times) -> {
            Deque<Instant> t = times == null ? new ArrayDeque<>() : prune(times, now);
            if (t == null) t = new ArrayDeque<>();
            t.addLast(now);
            return t;
        });
    }

    /** drops failures older than the window. returns null when nothing's left, which removes the key */
    private static Deque<Instant> prune(Deque<Instant> times, Instant now){
        synchronized (times) {
            Instant cutoff = now.minus(WINDOW);
            while (!times.isEmpty() && !times.peekFirst().isAfter(cutoff)) {
                times.removeFirst();
            }
            return times.isEmpty() ? null : times;
        }
    }

    // emails are compared the same no matter the case or spaces
    private static String accountKey(String account){
        return "acct:" + (account == null ? "" : account.trim().toLowerCase());
    }

    private static String ipKey(String ip){
        return "ip:" + (ip == null ? "unknown" : ip);
    }

    /** "Try again in 12 minutes." for the error message */
    public static String tooManyMessage(long seconds){
        long minutes = Math.max(1, (seconds + 59) / 60);
        return "Too many wrong passwords. Try again in " + minutes + (minutes == 1 ? " minute." : " minutes.");
    }
}
