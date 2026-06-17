package com.vsign.backend.auth.service;

import com.vsign.backend.auth.persistence.UserEntity;
import com.vsign.backend.auth.persistence.UserRepository;
import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserActivityService {
    private static final int DEFAULT_HEARTBEAT_SECONDS = 60;
    private static final int MAX_DAILY_SECONDS = 8 * 60 * 60;

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    public UserActivityService(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordHeartbeat(String email, Integer requestedActiveSeconds) {
        String normalizedEmail = normalizeEmail(email);
        int activeSeconds = normalizeActiveSeconds(requestedActiveSeconds);
        OffsetDateTime now = OffsetDateTime.now();
        LocalDate today = now.toLocalDate();

        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setLastSeenAt(now);
        userRepository.save(user);

        jdbcTemplate.update(
                """
                        insert into user_activity_events (user_email, event_type, metadata_json, created_at)
                        values (?, ?, ?, ?)
                        """,
                normalizedEmail,
                "HEARTBEAT",
                "{\"activeSeconds\":" + activeSeconds + "}",
                now
        );

        List<Integer> currentValues = jdbcTemplate.query(
                "select active_seconds from user_usage_daily where user_email = ? and activity_date = ?",
                (rs, rowNum) -> rs.getInt("active_seconds"),
                normalizedEmail,
                Date.valueOf(today)
        );

        if (currentValues.isEmpty()) {
            jdbcTemplate.update(
                    """
                            insert into user_usage_daily
                            (user_email, activity_date, active_seconds, lesson_completions, ai_attempts, quiz_attempts, last_seen_at)
                            values (?, ?, ?, 0, 0, 0, ?)
                            """,
                    normalizedEmail,
                    Date.valueOf(today),
                    activeSeconds,
                    now
            );
            return;
        }

        int nextActiveSeconds = Math.min(MAX_DAILY_SECONDS, currentValues.get(0) + activeSeconds);
        jdbcTemplate.update(
                """
                        update user_usage_daily
                        set active_seconds = ?, last_seen_at = ?
                        where user_email = ? and activity_date = ?
                        """,
                nextActiveSeconds,
                now,
                normalizedEmail,
                Date.valueOf(today)
        );
    }

    private int normalizeActiveSeconds(Integer requestedActiveSeconds) {
        if (requestedActiveSeconds == null) {
            return DEFAULT_HEARTBEAT_SECONDS;
        }
        return Math.max(1, Math.min(120, requestedActiveSeconds));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
