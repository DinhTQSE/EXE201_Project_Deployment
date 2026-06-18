package com.vsign.backend.admin.service;

import com.vsign.backend.admin.dto.AdminUserActivityResponse;
import com.vsign.backend.admin.dto.AdminUserDetailResponse;
import com.vsign.backend.admin.dto.AdminUserListResponse;
import com.vsign.backend.admin.dto.AdminUserResponse;
import com.vsign.backend.admin.dto.AdminUserUpdateRequest;
import com.vsign.backend.assessment.persistence.QuizAttemptRepository;
import com.vsign.backend.auth.persistence.UserEntity;
import com.vsign.backend.auth.persistence.UserRepository;
import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import com.vsign.backend.learning.persistence.LessonProgressRepository;
import com.vsign.backend.learning.persistence.SignatureAttemptLogRepository;
import com.vsign.backend.monetization.persistence.UserSubscriptionRepository;
import com.vsign.backend.payment.persistence.UserTierEntity;
import com.vsign.backend.payment.persistence.UserTierRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminUserService {
    private static final List<String> ALLOWED_ACCOUNT_TYPES = List.of("BASIC", "PREMIUM");
    private static final List<String> ALLOWED_ROLES = List.of("USER", "ADMIN", "SUPER_ADMIN", "CONTENT_REVIEWER");

    private final UserRepository userRepository;
    private final AdminAuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    private final LessonProgressRepository lessonProgressRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final SignatureAttemptLogRepository signatureAttemptLogRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserTierRepository userTierRepository;

    public AdminUserService(
            UserRepository userRepository,
            AdminAuditService auditService,
            JdbcTemplate jdbcTemplate,
            LessonProgressRepository lessonProgressRepository,
            QuizAttemptRepository quizAttemptRepository,
            SignatureAttemptLogRepository signatureAttemptLogRepository,
            UserSubscriptionRepository userSubscriptionRepository,
            UserTierRepository userTierRepository
    ) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
        this.lessonProgressRepository = lessonProgressRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.signatureAttemptLogRepository = signatureAttemptLogRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.userTierRepository = userTierRepository;
    }

    public AdminUserListResponse listUsers(String search, String role, String status, int page, int size) {
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(100, size));
        String normalizedSearch = normalizeOptional(search);
        String normalizedRole = normalizeOptional(role);
        String normalizedStatus = normalizeOptional(status);

        List<AdminUserResponse> filtered = userRepository.findAll().stream()
                .filter(user -> matchesSearch(user, normalizedSearch))
                .filter(user -> normalizedRole == null || user.getRole().equalsIgnoreCase(normalizedRole))
                .filter(user -> normalizedStatus == null || statusOf(user).equalsIgnoreCase(normalizedStatus))
                .sorted(Comparator.comparing(UserEntity::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();

        List<AdminUserResponse> pageItems = filtered.stream()
                .skip((long) normalizedPage * normalizedSize)
                .limit(normalizedSize)
                .toList();
        int totalPages = (int) Math.ceil((double) filtered.size() / normalizedSize);
        return new AdminUserListResponse(pageItems, normalizedPage, normalizedSize, filtered.size(), totalPages);
    }

    public AdminUserDetailResponse getUser(String userId) {
        UserEntity user = requireUser(userId);
        return new AdminUserDetailResponse(toResponse(user), toActivityResponse(user));
    }

    @Transactional
    public AdminUserDetailResponse updateUser(String userId, AdminUserUpdateRequest request, String actorEmail, String actorRole) {
        UserEntity user = requireUser(userId);
        String normalizedActorEmail = normalizeEmail(actorEmail);
        boolean nextActive = request.active() == null ? user.isActive() : request.active();
        String nextRole = request.role() == null || request.role().isBlank()
                ? user.getRole()
                : normalizeAllowed(request.role(), ALLOWED_ROLES, "role");

        if (normalizedActorEmail.equalsIgnoreCase(user.getEmail()) && !nextActive) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Admins cannot disable themselves.");
        }
        if (normalizedActorEmail.equalsIgnoreCase(user.getEmail()) && !nextRole.equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Admins cannot change their own role.");
        }
        if (!nextRole.equalsIgnoreCase(user.getRole()) && !"SUPER_ADMIN".equalsIgnoreCase(actorRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only SUPER_ADMIN can change roles.");
        }
        ensureNotRemovingLastSuperAdmin(user, nextRole, nextActive);

        if (request.displayName() != null) {
            String displayName = request.displayName().trim();
            if (displayName.isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Display name is required.");
            }
            user.setFullName(displayName);
        }
        if (request.accountType() != null && !request.accountType().isBlank()) {
            user.setAccountType(normalizeAllowed(request.accountType(), ALLOWED_ACCOUNT_TYPES, "accountType"));
        }
        user.setRole(nextRole);
        user.setActive(nextActive);

        UserEntity updated = userRepository.save(user);
        auditService.recordAction(
                actorEmail,
                "USER_UPDATED",
                "USER",
                updated.getEmail(),
                reasonOrDefault(request.reason(), "Admin user update")
        );
        return new AdminUserDetailResponse(toResponse(updated), toActivityResponse(updated));
    }

    @Transactional
    public AdminUserDetailResponse deactivateUser(String userId, String actorEmail, String reason) {
        UserEntity user = requireUser(userId);
        if (normalizeEmail(actorEmail).equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Admins cannot delete themselves.");
        }
        ensureNotRemovingLastSuperAdmin(user, user.getRole(), false);
        user.setActive(false);
        UserEntity updated = userRepository.save(user);
        auditService.recordAction(
                actorEmail,
                "USER_DEACTIVATED",
                "USER",
                updated.getEmail(),
                reasonOrDefault(reason, "Admin user deactivate")
        );
        return new AdminUserDetailResponse(toResponse(updated), toActivityResponse(updated));
    }

    public int activeUsers() {
        return Math.toIntExact(userRepository.countByActiveTrue());
    }

    public int premiumUsers() {
        Set<String> premiumEmails = new HashSet<>();
        userSubscriptionRepository.findAll().stream()
                .filter(sub -> "ACTIVE".equalsIgnoreCase(sub.getStatus()))
                .map(sub -> sub.getEmail().trim().toLowerCase(Locale.ROOT))
                .forEach(premiumEmails::add);
        userTierRepository.findActivePaidUserEmails(LocalDateTime.now()).stream()
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .forEach(premiumEmails::add);
        return premiumEmails.size();
    }

    private String resolveAccountType(UserEntity user) {
        if ("ADMIN".equalsIgnoreCase(user.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
            return "ADMIN";
        }
        var subOpt = userSubscriptionRepository.findById(user.getEmail());
        if (subOpt.isPresent() && "ACTIVE".equalsIgnoreCase(subOpt.get().getStatus())) {
            return "PREMIUM";
        }
        List<UserTierEntity> activeTiers = userTierRepository.findCurrentActiveByUserId(user.getId(), LocalDateTime.now());
        boolean hasPaidTier = activeTiers.stream().anyMatch(ut -> ut.getTier() != null && ut.getTier().getAmount() > 0);
        if (hasPaidTier) {
            return "PREMIUM";
        }
        return user.getAccountType();
    }

    private AdminUserResponse toResponse(UserEntity user) {
        return new AdminUserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                statusOf(user),
                resolveAccountType(user),
                user.getCreatedAt().toString(),
                user.getUpdatedAt().toString(),
                user.getLastSeenAt() == null ? null : user.getLastSeenAt().toString(),
                user.getTotalXp(),
                user.getCurrentStreak()
        );
    }

    private AdminUserActivityResponse toActivityResponse(UserEntity user) {
        String email = normalizeEmail(user.getEmail());
        int activeSeconds = totalActiveSeconds(email);
        int completedLessons = (int) lessonProgressRepository.findAll().stream()
                .filter(progress -> email.equalsIgnoreCase(progress.getUserKey()))
                .filter(progress -> "COMPLETED".equalsIgnoreCase(progress.getStatus()))
                .count();
        int quizAttempts = (int) quizAttemptRepository.findAll().stream()
                .filter(attempt -> email.equalsIgnoreCase(attempt.getUserKey()))
                .filter(attempt -> attempt.isSubmitted())
                .count();
        int aiAttempts = (int) signatureAttemptLogRepository.findAll().stream()
                .filter(attempt -> email.equalsIgnoreCase(attempt.getUserKey()))
                .count();
        int aiPassedAttempts = (int) signatureAttemptLogRepository.findAll().stream()
                .filter(attempt -> email.equalsIgnoreCase(attempt.getUserKey()))
                .filter(attempt -> Boolean.TRUE.equals(attempt.getCorrect()) || "PASSED".equalsIgnoreCase(attempt.getStatus()))
                .count();
        return new AdminUserActivityResponse(
                activeSeconds,
                completedLessons,
                quizAttempts,
                aiAttempts,
                aiPassedAttempts,
                user.getLastSeenAt() == null ? null : user.getLastSeenAt().toString()
        );
    }

    private int totalActiveSeconds(String email) {
        Number total = jdbcTemplate.queryForObject(
                "select coalesce(sum(active_seconds), 0) from user_usage_daily where user_email = ?",
                Number.class,
                email
        );
        return total == null ? 0 : total.intValue();
    }

    private UserEntity requireUser(String userId) {
        try {
            return userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private void ensureNotRemovingLastSuperAdmin(UserEntity user, String nextRole, boolean nextActive) {
        if (!"SUPER_ADMIN".equalsIgnoreCase(user.getRole()) || (!user.isActive())) {
            return;
        }
        if (nextActive && "SUPER_ADMIN".equalsIgnoreCase(nextRole)) {
            return;
        }
        long activeSuperAdmins = userRepository.findAll().stream()
                .filter(candidate -> candidate.isActive() && "SUPER_ADMIN".equalsIgnoreCase(candidate.getRole()))
                .count();
        if (activeSuperAdmins <= 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot remove the last SUPER_ADMIN.");
        }
    }

    private boolean matchesSearch(UserEntity user, String search) {
        if (search == null) {
            return true;
        }
        String haystack = (user.getEmail() + " " + user.getFullName()).toLowerCase(Locale.ROOT);
        return haystack.contains(search.toLowerCase(Locale.ROOT));
    }

    private String statusOf(UserEntity user) {
        return user.isActive() ? "ACTIVE" : "DISABLED";
    }

    private String normalizeAllowed(String value, List<String> allowed, String field) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid " + field + ".");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String reasonOrDefault(String reason, String fallback) {
        return reason == null || reason.isBlank() ? fallback : reason.trim();
    }
}
