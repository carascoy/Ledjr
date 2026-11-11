package com.Ledjr.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.Ledjr.dto.AdminStats;

/** plain SQL counts for the admin page. it's all aggregates, so no point loading entities */
@Service
public class AdminStatsService {

    private static final String USERS = """
        SELECT count(*) AS total,
               count(*) FILTER (WHERE created_at >= ?) AS today,
               count(*) FILTER (WHERE created_at >= ?) AS week,
               count(*) FILTER (WHERE created_at >= ?) AS month,
               count(*) FILTER (WHERE EXISTS (SELECT 1 FROM transaction t WHERE t.user_id = u.id)
                                   OR EXISTS (SELECT 1 FROM recurring_template r WHERE r.user_id = u.id)
                                   OR EXISTS (SELECT 1 FROM debt d WHERE d.user_id = u.id)) AS added_data,
               count(*) FILTER (WHERE last_seen_at >= ?) AS active
        FROM app_user u
        WHERE NOT demo
        """;

    private static final String DEMOS = """
        SELECT count(*) AS total,
               count(*) FILTER (WHERE started_at >= ?) AS today,
               count(*) FILTER (WHERE started_at >= ?) AS week,
               (SELECT count(*) FROM app_user WHERE demo) AS live
        FROM demo_start
        """;

    private static final String DAYS = """
        SELECT d::date AS day,
               (SELECT count(*) FROM app_user u
                 WHERE NOT u.demo AND u.created_at >= d AND u.created_at < d + interval '1 day') AS signups,
               (SELECT count(*) FROM demo_start s
                 WHERE s.started_at >= d AND s.started_at < d + interval '1 day') AS demos
        FROM generate_series(?::date, ?::date, interval '1 day') AS d
        ORDER BY d
        """;

    private static final String RECENT = """
        SELECT u.first_name, u.created_at, u.last_seen_at,
               (SELECT count(*) FROM transaction t WHERE t.user_id = u.id) AS transactions
        FROM app_user u
        WHERE NOT u.demo
        ORDER BY u.created_at DESC
        LIMIT 10
        """;

    private static final String CONTENT = """
        SELECT (SELECT count(*) FROM transaction x JOIN app_user u ON u.id = x.user_id WHERE NOT u.demo) AS transactions,
               (SELECT count(*) FROM recurring_template x JOIN app_user u ON u.id = x.user_id WHERE NOT u.demo) AS recurring,
               (SELECT count(*) FROM debt x JOIN app_user u ON u.id = x.user_id WHERE NOT u.demo) AS debts,
               (SELECT count(*) FROM budget x JOIN app_user u ON u.id = x.user_id WHERE NOT u.demo) AS budgets
        """;

    private static final String DB_SIZE = "SELECT pg_size_pretty(pg_database_size(current_database()))";

    private final JdbcTemplate jdbc;

    public AdminStatsService(JdbcTemplate jdbc){
        this.jdbc = jdbc;
    }

    public AdminStats stats(LocalDate today){
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime weekAgo = today.minusDays(6).atStartOfDay();
        LocalDateTime monthAgo = today.minusDays(29).atStartOfDay();

        AdminStats.Users users = jdbc.queryForObject(USERS, (rs, i) -> new AdminStats.Users(
            rs.getLong("total"), rs.getLong("today"), rs.getLong("week"), rs.getLong("month"),
            rs.getLong("added_data"), rs.getLong("active")),
            startOfToday, weekAgo, monthAgo, weekAgo);

        AdminStats.Demos demos = jdbc.queryForObject(DEMOS, (rs, i) -> new AdminStats.Demos(
            rs.getLong("total"), rs.getLong("today"), rs.getLong("week"), rs.getLong("live")),
            startOfToday, weekAgo);

        List<AdminStats.Day> days = jdbc.query(DAYS, (rs, i) -> new AdminStats.Day(
            rs.getObject("day", LocalDate.class), rs.getLong("signups"), rs.getLong("demos")),
            today.minusDays(29), today);

        List<AdminStats.RecentUser> recent = jdbc.query(RECENT, (rs, i) -> new AdminStats.RecentUser(
            rs.getString("first_name"), rs.getObject("created_at", LocalDateTime.class),
            rs.getObject("last_seen_at", LocalDateTime.class), rs.getLong("transactions")));

        AdminStats.Content content = jdbc.queryForObject(CONTENT, (rs, i) -> new AdminStats.Content(
            rs.getLong("transactions"), rs.getLong("recurring"), rs.getLong("debts"), rs.getLong("budgets")));

        String size = jdbc.queryForObject(DB_SIZE, String.class);

        return new AdminStats(users, demos, days, recent, content, size);
    }
}
