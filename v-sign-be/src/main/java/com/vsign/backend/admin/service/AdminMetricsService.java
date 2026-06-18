package com.vsign.backend.admin.service;

import com.vsign.backend.admin.dto.AdminMetricsOverviewResponse;
import com.vsign.backend.admin.dto.AdminTopUserResponse;
import com.vsign.backend.admin.dto.AdminUsageMetricsResponse;
import com.vsign.backend.admin.dto.AdminUsagePointResponse;
import com.vsign.backend.assessment.persistence.QuizAttemptEntity;
import com.vsign.backend.assessment.persistence.QuizAttemptRepository;
import com.vsign.backend.auth.persistence.UserEntity;
import com.vsign.backend.auth.persistence.UserRepository;
import com.vsign.backend.learning.persistence.LessonProgressEntity;
import com.vsign.backend.learning.persistence.LessonProgressRepository;
import com.vsign.backend.learning.persistence.SignatureAttemptLogEntity;
import com.vsign.backend.learning.persistence.SignatureAttemptLogRepository;
import com.vsign.backend.payment.persistence.PayOSOrderEntity;
import com.vsign.backend.payment.persistence.PayOSOrderRepository;
import com.vsign.backend.payment.persistence.PaymentOrderStatus;
import com.vsign.backend.payment.persistence.UserTierRepository;
import com.vsign.backend.monetization.persistence.UserSubscriptionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminMetricsService {
    private final UserRepository userRepository;
    private final PayOSOrderRepository orderRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserTierRepository userTierRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final SignatureAttemptLogRepository signatureAttemptLogRepository;
    private final AdminContentReviewService contentReviewService;
    private final JdbcTemplate jdbcTemplate;

    public AdminMetricsService(
            UserRepository userRepository,
            PayOSOrderRepository orderRepository,
            UserSubscriptionRepository userSubscriptionRepository,
            UserTierRepository userTierRepository,
            LessonProgressRepository lessonProgressRepository,
            QuizAttemptRepository quizAttemptRepository,
            SignatureAttemptLogRepository signatureAttemptLogRepository,
            AdminContentReviewService contentReviewService,
            JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.userTierRepository = userTierRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.signatureAttemptLogRepository = signatureAttemptLogRepository;
        this.contentReviewService = contentReviewService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminMetricsOverviewResponse overview(LocalDate fromDate, LocalDate toDate) {
        List<UserEntity> users = userRepository.findAll();
        List<UsageRow> usageRows = usageRows(fromDate, toDate);
        Set<String> activeEmailsInRange = activeEmailsInRange(users, usageRows, fromDate, toDate);

        List<PayOSOrderEntity> successfulPayments = orderRepository.findAll().stream()
                .filter(payment -> PaymentOrderStatus.PAID == payment.getStatus())
                .filter(payment -> withinRange(payment.getCreatedAt(), fromDate, toDate))
                .toList();

        List<LessonProgressEntity> completedLessons = lessonProgressRepository.findAll().stream()
                .filter(progress -> "COMPLETED".equalsIgnoreCase(progress.getStatus()))
                .filter(progress -> withinRange(progress.getUpdatedAt(), fromDate, toDate))
                .toList();

        List<QuizAttemptEntity> submittedQuizAttempts = quizAttemptRepository.findAll().stream()
                .filter(QuizAttemptEntity::isSubmitted)
                .filter(attempt -> withinRange(attempt.getSubmittedAt(), fromDate, toDate))
                .toList();

        List<SignatureAttemptLogEntity> aiAttempts = signatureAttemptLogRepository.findAll().stream()
                .filter(attempt -> withinRange(attempt.getCreatedAt(), fromDate, toDate))
                .toList();
        long aiPassedAttempts = aiAttempts.stream()
                .filter(attempt -> Boolean.TRUE.equals(attempt.getCorrect()) || "PASSED".equalsIgnoreCase(attempt.getStatus()))
                .count();

        int activeSeconds = usageRows.stream().mapToInt(UsageRow::activeSeconds).sum();
        int averageActiveSeconds = activeEmailsInRange.isEmpty() ? 0 : activeSeconds / activeEmailsInRange.size();

        Set<String> premiumEmails = new HashSet<>();
        userSubscriptionRepository.findAll().stream()
                .filter(sub -> "ACTIVE".equalsIgnoreCase(sub.getStatus()))
                .map(sub -> sub.getEmail().trim().toLowerCase(Locale.ROOT))
                .forEach(premiumEmails::add);
        userTierRepository.findActivePaidUserEmails(LocalDateTime.now()).stream()
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .forEach(premiumEmails::add);

        return new AdminMetricsOverviewResponse(
                users.size(),
                (int) users.stream().filter(user -> withinRange(user.getCreatedAt(), fromDate, toDate)).count(),
                (int) users.stream().filter(UserEntity::isActive).count(),
                activeEmailsInRange.size(),
                premiumEmails.size(),
                successfulPayments.stream().mapToLong(PayOSOrderEntity::getAmount).sum(),
                successfulPayments.size(),
                contentReviewService.pendingReviewCount(),
                completedLessons.size(),
                submittedQuizAttempts.size(),
                aiAttempts.size(),
                aiAttempts.isEmpty() ? 0 : (double) aiPassedAttempts / aiAttempts.size(),
                averageActiveSeconds,
                topActiveUsers(users, usageRows)
        );
    }

    public AdminUsageMetricsResponse usage(LocalDate fromDate, LocalDate toDate, String granularity) {
        String resolvedGranularity = granularity == null || granularity.isBlank()
                ? "daily"
                : granularity.trim().toLowerCase(Locale.ROOT);
        List<UsageRow> usageRows = usageRows(fromDate, toDate);
        Map<LocalDate, UsageBucket> byDate = new LinkedHashMap<>();
        usageRows.stream()
                .sorted(Comparator.comparing(UsageRow::activityDate))
                .forEach(row -> byDate.computeIfAbsent(row.activityDate(), UsageBucket::new).add(row));
        List<AdminUsagePointResponse> points = byDate.values().stream()
                .map(UsageBucket::toResponse)
                .toList();
        return new AdminUsageMetricsResponse(resolvedGranularity, points);
    }

    private List<AdminTopUserResponse> topActiveUsers(List<UserEntity> users, List<UsageRow> usageRows) {
        Map<String, String> displayNamesByEmail = new HashMap<>();
        users.forEach(user -> displayNamesByEmail.put(normalizeEmail(user.getEmail()), user.getFullName()));

        Map<String, Integer> secondsByEmail = new HashMap<>();
        usageRows.forEach(row -> secondsByEmail.merge(normalizeEmail(row.userEmail()), row.activeSeconds(), Integer::sum));
        return secondsByEmail.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new AdminTopUserResponse(
                        entry.getKey(),
                        displayNamesByEmail.getOrDefault(entry.getKey(), entry.getKey()),
                        entry.getValue()
                ))
                .toList();
    }

    private Set<String> activeEmailsInRange(List<UserEntity> users, List<UsageRow> usageRows, LocalDate fromDate, LocalDate toDate) {
        Set<String> emails = new HashSet<>();
        usageRows.stream()
                .filter(row -> row.activeSeconds() > 0)
                .map(row -> normalizeEmail(row.userEmail()))
                .forEach(emails::add);
        users.stream()
                .filter(user -> user.getLastSeenAt() != null)
                .filter(user -> withinRange(user.getLastSeenAt(), fromDate, toDate))
                .map(user -> normalizeEmail(user.getEmail()))
                .forEach(emails::add);
        return emails;
    }

    private List<UsageRow> usageRows(LocalDate fromDate, LocalDate toDate) {
        List<UsageRow> rows = jdbcTemplate.query(
                """
                        select user_email, activity_date, active_seconds, lesson_completions, quiz_attempts, ai_attempts
                        from user_usage_daily
                        """,
                (rs, rowNum) -> new UsageRow(
                        rs.getString("user_email"),
                        rs.getDate("activity_date").toLocalDate(),
                        rs.getInt("active_seconds"),
                        rs.getInt("lesson_completions"),
                        rs.getInt("quiz_attempts"),
                        rs.getInt("ai_attempts")
                )
        );
        return rows.stream()
                .filter(row -> withinRange(row.activityDate(), fromDate, toDate))
                .toList();
    }

    private boolean withinRange(LocalDateTime dateTime, LocalDate fromDate, LocalDate toDate) {
        if (dateTime == null) {
            return false;
        }
        return withinRange(dateTime.toLocalDate(), fromDate, toDate);
    }

    private boolean withinRange(OffsetDateTime dateTime, LocalDate fromDate, LocalDate toDate) {
        if (dateTime == null) {
            return false;
        }
        return withinRange(dateTime.toLocalDate(), fromDate, toDate);
    }

    private boolean withinRange(LocalDate date, LocalDate fromDate, LocalDate toDate) {
        if (date == null) {
            return false;
        }
        boolean afterStart = fromDate == null || !date.isBefore(fromDate);
        boolean beforeEnd = toDate == null || !date.isAfter(toDate);
        return afterStart && beforeEnd;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record UsageRow(
            String userEmail,
            LocalDate activityDate,
            int activeSeconds,
            int lessonCompletions,
            int quizAttempts,
            int aiAttempts
    ) {
    }

    private static final class UsageBucket {
        private final LocalDate date;
        private final List<UsageRow> rows = new ArrayList<>();

        private UsageBucket(LocalDate date) {
            this.date = date;
        }

        private void add(UsageRow row) {
            rows.add(row);
        }

        private AdminUsagePointResponse toResponse() {
            return new AdminUsagePointResponse(
                    date.toString(),
                    rows.stream().mapToInt(UsageRow::activeSeconds).sum(),
                    rows.stream().mapToInt(UsageRow::lessonCompletions).sum(),
                    rows.stream().mapToInt(UsageRow::quizAttempts).sum(),
                    rows.stream().mapToInt(UsageRow::aiAttempts).sum()
            );
        }
    }
}
