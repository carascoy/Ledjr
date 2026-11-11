package com.Ledjr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminStats(
    Users users,
    Demos demos,
    List<Day> days,
    List<RecentUser> recentSignups,
    Content content,
    String databaseSize
) {
    /** real accounts only, demos never count here */
    public record Users(long total, long today, long last7Days, long last30Days, long addedData, long activeLast7Days) {}
    public record Demos(long total, long today, long last7Days, long liveNow) {}
    public record Day(LocalDate day, long signups, long demos) {}
    public record RecentUser(String firstName, LocalDateTime joined, LocalDateTime lastSeen, long transactions) {}
    public record Content(long transactions, long recurring, long debts, long budgets) {}
}
