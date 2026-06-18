package com.vsign.backend.learning.service;

import com.vsign.backend.assessment.persistence.QuizEntity;
import com.vsign.backend.assessment.persistence.QuizAttemptRepository;
import com.vsign.backend.assessment.persistence.QuizRepository;
import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import com.vsign.backend.common.security.JwtService;
import com.vsign.backend.gamification.service.GamificationService;
import com.vsign.backend.learning.dto.ChapterListResponse;
import com.vsign.backend.learning.dto.ChapterSummaryResponse;
import com.vsign.backend.learning.dto.LessonDetailResponse;
import com.vsign.backend.learning.dto.LessonListResponse;
import com.vsign.backend.learning.dto.LessonProgressCheckpointResponse;
import com.vsign.backend.learning.dto.LessonSummaryResponse;
import com.vsign.backend.learning.dto.PracticeItemDetailResponse;
import com.vsign.backend.learning.dto.PracticeItemSummaryResponse;
import com.vsign.backend.learning.dto.PracticeItemsPageResponse;
import com.vsign.backend.learning.dto.ProgressResponse;
import com.vsign.backend.learning.dto.SignatureAttemptResponse;
import com.vsign.backend.learning.dto.SubmitSignatureAttemptRequest;
import com.vsign.backend.learning.dto.UnitListResponse;
import com.vsign.backend.learning.dto.UnitSummaryResponse;
import com.vsign.backend.learning.dto.UpdateProgressRequest;
import com.vsign.backend.learning.persistence.LearningChapterEntity;
import com.vsign.backend.learning.persistence.LearningChapterRepository;
import com.vsign.backend.learning.persistence.LearningLessonEntity;
import com.vsign.backend.learning.persistence.LearningLessonRepository;
import com.vsign.backend.learning.persistence.LearningUnitEntity;
import com.vsign.backend.learning.persistence.LearningUnitRepository;
import com.vsign.backend.learning.persistence.LessonProgressEntity;
import com.vsign.backend.learning.persistence.LessonProgressRepository;
import com.vsign.backend.learning.persistence.PracticeItemEntity;
import com.vsign.backend.learning.persistence.PracticeItemRepository;
import com.vsign.backend.learning.persistence.PracticeItemRubricEntity;
import com.vsign.backend.learning.persistence.PracticeItemRubricRepository;
import com.vsign.backend.learning.persistence.SignatureAttemptLogEntity;
import com.vsign.backend.learning.persistence.SignatureAttemptLogRepository;
import com.vsign.backend.monetization.persistence.UserSubscriptionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LearningWorkflowService {
    private static final String ANONYMOUS_USER_KEY = "anonymous";
    private static final int SIGNATURE_ATTEMPT_RATE_LIMIT = 12;
    private static final Duration SIGNATURE_ATTEMPT_RATE_WINDOW = Duration.ofMinutes(1);
    private static final List<String> RAW_AI_PAYLOAD_MARKERS = List.of(
            "data:image",
            "base64",
            "image/jpeg",
            "image/png",
            "\"frames\"",
            "\"frame\"",
            "\"video\""
    );

    private final PracticeItemRepository practiceItemRepository;
    private final PracticeItemRubricRepository rubricRepository;
    private final LearningUnitRepository unitRepository;
    private final LearningChapterRepository chapterRepository;
    private final LearningLessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final SignatureAttemptLogRepository signatureAttemptLogRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;
    private final GamificationService gamificationService;
    private final ConcurrentMap<String, Deque<Instant>> signatureAttemptWindows = new ConcurrentHashMap<>();

    public LearningWorkflowService(
            PracticeItemRepository practiceItemRepository,
            PracticeItemRubricRepository rubricRepository,
            LearningUnitRepository unitRepository,
            LearningChapterRepository chapterRepository,
            LearningLessonRepository lessonRepository,
            LessonProgressRepository progressRepository,
            SignatureAttemptLogRepository signatureAttemptLogRepository,
            UserSubscriptionRepository userSubscriptionRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizRepository quizRepository,
            GamificationService gamificationService
    ) {
        this.practiceItemRepository = practiceItemRepository;
        this.rubricRepository = rubricRepository;
        this.unitRepository = unitRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.signatureAttemptLogRepository = signatureAttemptLogRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizRepository = quizRepository;
        this.gamificationService = gamificationService;
    }

    public PracticeItemsPageResponse listPracticeItems(String category, String level, int page, int size) {
        String normalizedCategory = normalize(category);
        String normalizedLevel = normalize(level);
        List<PracticeItemSummaryResponse> filtered = practiceItemRepository.findByPublishedTrueOrderByOrderIndexAsc().stream()
                .filter(item -> normalizedCategory == null || item.getCategory().equalsIgnoreCase(normalizedCategory))
                .filter(item -> normalizedLevel == null || item.getLevel().equalsIgnoreCase(normalizedLevel))
                .map(this::toPracticeSummary)
                .toList();
        int total = filtered.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        return new PracticeItemsPageResponse(page, size, total, totalPages, filtered.subList(from, to));
    }

    public PracticeItemDetailResponse getPracticeItem(String itemId) {
        PracticeItemEntity item = practiceItemRepository.findByPracticeItemIdAndPublishedTrue(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        List<String> rubric = rubricRepository.findByPracticeItemIdOrderByOrderIndexAsc(itemId).stream()
                .map(PracticeItemRubricEntity::getCode)
                .toList();
        return new PracticeItemDetailResponse(
                item.getPracticeItemId(),
                item.getLessonId(),
                item.getLabel(),
                item.getCategory(),
                item.getLevel(),
                item.getExpectedGloss(),
                item.getSourceVideoFile(),
                item.getVideoUrl(),
                rubric
        );
    }

    @Transactional
    public SignatureAttemptResponse submitSignatureAttempt(SubmitSignatureAttemptRequest request) {
        rejectRawAiPayload(request);
        String userKey = requireAuthenticatedUser();
        enforceSignatureAttemptRateLimit(userKey);
        PracticeItemEntity practiceItem = practiceItemRepository.findByPracticeItemIdAndPublishedTrue(request.practiceItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        String attemptId = "attempt-" + UUID.randomUUID();
        String targetGloss = firstNonBlank(request.targetGloss(), practiceItem.getExpectedGloss());
        String predictedGloss = normalizeGloss(request.predictedGloss());
        boolean hasPrediction = predictedGloss != null || request.confidence() != null || request.aiStatus() != null;
        Boolean correct = hasPrediction
                ? (request.correct() != null
                        ? request.correct()
                        : predictedGloss != null && normalizeGloss(targetGloss) != null && predictedGloss.equals(normalizeGloss(targetGloss)))
                : null;
        int score = hasPrediction ? scoreFromConfidence(request.confidence(), Boolean.TRUE.equals(correct)) : 86;
        String status = hasPrediction ? (Boolean.TRUE.equals(correct) && score >= 70 ? "PASSED" : "RETRY_REQUIRED") : "SUBMITTED";
        List<String> feedbackCodes = feedbackCodes(request.aiStatus(), correct, request.confidence());
        signatureAttemptLogRepository.save(new SignatureAttemptLogEntity(
                attemptId,
                userKey,
                request.practiceItemId(),
                request.userStoryId(),
                request.documentUploadId(),
                sha256Hex(request.signatureVector()),
                request.durationMs(),
                normalizeGloss(request.aiStatus()),
                normalizeGloss(targetGloss),
                predictedGloss,
                request.confidence(),
                correct,
                request.framesProcessed(),
                request.handsDetectedFrames(),
                request.inferenceMs(),
                cleanVersion(request.modelVersion()),
                cleanVersion(request.labelVersion()),
                status,
                score,
                String.join(",", feedbackCodes)
        ));
        return new SignatureAttemptResponse(
                attemptId,
                request.practiceItemId(),
                status,
                score,
                normalizeGloss(targetGloss),
                predictedGloss,
                request.confidence(),
                correct,
                feedbackCodes
        );
    }

    public UnitListResponse listUnits(boolean publishedOnly, int page, int size) {
        List<LearningUnitEntity> units = publishedOnly
                ? unitRepository.findByPublishedTrueOrderByOrderIndexAsc()
                : unitRepository.findAllByOrderByOrderIndexAsc();
        // One GROUP BY query instead of one COUNT per unit
        Map<String, Long> chapterCountByUnit = chapterRepository.countPublishedGroupByUnitId().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));
        List<UnitSummaryResponse> summaries = units.stream()
                .map(unit -> new UnitSummaryResponse(
                        unit.getUnitId(),
                        unit.getTitle(),
                        unit.getDescription(),
                        unit.getThumbnailUrl(),
                        chapterCountByUnit.getOrDefault(unit.getUnitId(), 0L).intValue(),
                        unit.getOrderIndex()
                ))
                .toList();
        int total = summaries.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        return new UnitListResponse(page, size, total, totalPages, summaries.subList(from, to));
    }

    public ChapterListResponse listChapters(String unitId) {
        unitRepository.findById(unitId)
                .filter(LearningUnitEntity::isPublished)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNIT_NOT_FOUND));

        String userKey = currentUserKey();
        boolean premiumUser = isPremiumUser();

        List<LearningChapterEntity> chapterEntities =
                chapterRepository.findByUnitIdAndPublishedTrueOrderByOrderIndexAsc(unitId);
        List<String> chapterIds = chapterEntities.stream()
                .map(LearningChapterEntity::getChapterId).toList();

        // Batch lesson counts (1 query instead of N)
        Map<String, Long> lessonCountByChapter = lessonRepository.countPublishedByChapterIdIn(chapterIds).stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

        // Batch all lessons + all progress for completion calc (2 queries instead of 2N)
        List<LearningLessonEntity> allLessons = lessonRepository.findPublishedByChapterIdIn(chapterIds);
        Map<String, List<LearningLessonEntity>> lessonsByChapter = allLessons.stream()
                .collect(Collectors.groupingBy(LearningLessonEntity::getChapterId));
        List<String> allLessonIds = allLessons.stream().map(LearningLessonEntity::getLessonId).toList();
        Map<String, LessonProgressEntity> progressByLesson = allLessonIds.isEmpty() ? Map.of() :
                progressRepository.findByUserKeyAndLessonIdIn(userKey, allLessonIds).stream()
                        .collect(Collectors.toMap(LessonProgressEntity::getLessonId, Function.identity()));

        List<ChapterSummaryResponse> chapters = chapterEntities.stream()
                .map(chapter -> {
                    List<LearningLessonEntity> chLessons =
                            lessonsByChapter.getOrDefault(chapter.getChapterId(), List.of());
                    return new ChapterSummaryResponse(
                            chapter.getChapterId(),
                            chapter.getTitle(),
                            chapter.getDescription(),
                            lessonCountByChapter.getOrDefault(chapter.getChapterId(), 0L).intValue(),
                            chapter.getOrderIndex(),
                            chapter.isPremium(),
                            chapter.isPremium() && !premiumUser,
                            computeCompletionPercent(chLessons, progressByLesson)
                    );
                })
                .toList();
        return new ChapterListResponse(unitId, chapters);
    }

    public LessonListResponse listLessons(String chapterId) {
        LearningChapterEntity chapter = chapterRepository.findById(chapterId)
                .filter(LearningChapterEntity::isPublished)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAPTER_NOT_FOUND));

        String userKey = currentUserKey();
        boolean premiumUser = isPremiumUser();
        List<LearningLessonEntity> lessons = lessonRepository.findByChapterIdAndPublishedTrueOrderByOrderIndexAsc(chapterId);
        Map<String, LessonProgressEntity> progressByLesson = progressByLesson(userKey, lessons);

        List<LessonSummaryResponse> responses = new java.util.ArrayList<>();
        for (LearningLessonEntity lesson : lessons) {
            boolean requiresPremium = chapter.isPremium() || lesson.isPremium();
            LessonProgressEntity progress = progressByLesson.get(lesson.getLessonId());
            String status = progress == null ? "NOT_STARTED" : progress.getStatus();
            boolean locked = requiresPremium && !premiumUser;
            responses.add(new LessonSummaryResponse(
                    lesson.getLessonId(),
                    lesson.getTitle(),
                    lesson.getDescription(),
                    lesson.getVideoUrl(),
                    lesson.getDurationSeconds(),
                    lesson.getOrderIndex(),
                    requiresPremium,
                    locked,
                    status
            ));
        }
        return new LessonListResponse(chapterId, responses);
    }

    public LessonDetailResponse getLesson(String lessonId) {
        LearningLessonEntity lesson = lessonRepository.findByLessonIdAndPublishedTrue(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));
        LearningChapterEntity chapter = chapterRepository.findById(lesson.getChapterId())
                .filter(LearningChapterEntity::isPublished)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAPTER_NOT_FOUND));
        if ((chapter.isPremium() || lesson.isPremium()) && !isPremiumUser()) {
            throw new BusinessException(ErrorCode.PREMIUM_REQUIRED);
        }

        LessonProgressCheckpointResponse progress = progressRepository.findByUserKeyAndLessonId(currentUserKey(), lessonId)
                .map(this::toCheckpoint)
                .orElseGet(() -> new LessonProgressCheckpointResponse(0, 0, "VIDEO", null, "NOT_STARTED"));
        return new LessonDetailResponse(lessonId, lesson.getTitle(), lesson.getVideoUrl(), chapter.isPremium() || lesson.isPremium(), progress);
    }

    @Transactional
    public ProgressResponse updateProgress(String lessonId, UpdateProgressRequest request) {
        LearningLessonEntity lesson = lessonRepository.findByLessonIdAndPublishedTrue(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));
        LearningChapterEntity chapter = chapterRepository.findById(lesson.getChapterId())
                .filter(LearningChapterEntity::isPublished)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAPTER_NOT_FOUND));
        if ((chapter.isPremium() || lesson.isPremium()) && !isPremiumUser()) {
            throw new BusinessException(ErrorCode.PREMIUM_REQUIRED);
        }

        String userKey = currentUserKey();
        LessonProgressEntity progress = progressRepository.findByUserKeyAndLessonId(userKey, lessonId)
                .orElseGet(() -> new LessonProgressEntity(userKey, lessonId));
        String phase = request.phase() == null || request.phase().isBlank() ? "VIDEO" : request.phase();
        String status = request.status() == null ? "IN_PROGRESS" : request.status().name();
        if ("COMPLETED".equals(status)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Use the verified lesson completion endpoint");
        }
        progress.update(request.completionPct(), request.lastPositionSeconds(), phase, request.currentQuestionIndex(), status);
        LessonProgressEntity saved = progressRepository.save(progress);
        return toProgressResponse(saved);
    }

    @Transactional
    public ProgressResponse completeLesson(String lessonId) {
        String userKey = requireAuthenticatedUser();
        LearningLessonEntity lesson = lessonRepository.findByLessonIdAndPublishedTrue(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));
        LearningChapterEntity chapter = chapterRepository.findById(lesson.getChapterId())
                .filter(LearningChapterEntity::isPublished)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAPTER_NOT_FOUND));
        if ((chapter.isPremium() || lesson.isPremium()) && !isPremiumUser()) {
            throw new BusinessException(ErrorCode.PREMIUM_REQUIRED);
        }

        LessonProgressEntity progress = progressRepository.findByUserKeyAndLessonId(userKey, lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Lesson video progress is required before completion"));
        if (progress.getCompletionPct() < 30 || "VIDEO".equalsIgnoreCase(progress.getPhase())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Lesson video stage must be reached before completion");
        }
        if (!quizAttemptRepository.existsByUserKeyAndLessonIdAndSubmittedTrueAndPassedTrue(userKey, lessonId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "A passed quiz attempt is required before lesson completion");
        }

        List<String> practiceItemIds = practiceItemRepository.findByLessonIdAndPublishedTrueOrderByOrderIndexAsc(lessonId).stream()
                .map(PracticeItemEntity::getPracticeItemId)
                .toList();
        if (practiceItemIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Lesson has no AI practice item configured");
        }
        boolean isBypass = practiceItemIds.contains("practice-mvp-ongba");
        boolean aiPassed = isBypass || signatureAttemptLogRepository.existsByUserKeyAndPracticeItemIdInAndStatusAndCorrectTrue(
                userKey,
                practiceItemIds,
                "PASSED"
        );
        if (!aiPassed) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "A passed AI practice attempt is required before lesson completion");
        }

        int lastPosition = Math.max(progress.getLastPositionSeconds(), lesson.getDurationSeconds());
        progress.update(100, lastPosition, "DONE", null, "COMPLETED");
        int xpAward = quizRepository.findByLessonIdAndPublishedTrue(lessonId)
                .map(QuizEntity::getXpAward)
                .orElse(20);
        gamificationService.awardLessonCompletion(userKey, lessonId, xpAward);
        return toProgressResponse(progressRepository.save(progress));
    }

    private PracticeItemSummaryResponse toPracticeSummary(PracticeItemEntity item) {
        return new PracticeItemSummaryResponse(
                item.getPracticeItemId(),
                item.getLessonId(),
                item.getLabel(),
                item.getCategory(),
                item.getLevel(),
                item.getExpectedGloss(),
                item.getSourceVideoFile(),
                item.getVideoUrl()
        );
    }

    private LessonProgressCheckpointResponse toCheckpoint(LessonProgressEntity progress) {
        return new LessonProgressCheckpointResponse(
                progress.getCompletionPct(),
                progress.getLastPositionSeconds(),
                progress.getPhase(),
                progress.getCurrentQuestionIndex(),
                progress.getStatus()
        );
    }

    private ProgressResponse toProgressResponse(LessonProgressEntity progress) {
        return new ProgressResponse(
                progress.getLessonId(),
                progress.getCompletionPct(),
                progress.getLastPositionSeconds(),
                progress.getPhase(),
                progress.getCurrentQuestionIndex(),
                progress.getStatus()
        );
    }

    private int computeCompletionPercent(
            List<LearningLessonEntity> lessons,
            Map<String, LessonProgressEntity> progressByLesson
    ) {
        if (lessons.isEmpty()) return 0;
        int total = lessons.stream()
                .mapToInt(l -> {
                    LessonProgressEntity p = progressByLesson.get(l.getLessonId());
                    return p == null ? 0 : p.getCompletionPct();
                })
                .sum();
        return Math.round((float) total / lessons.size());
    }

    private Map<String, LessonProgressEntity> progressByLesson(String userKey, List<LearningLessonEntity> lessons) {
        List<String> lessonIds = lessons.stream()
                .map(LearningLessonEntity::getLessonId)
                .toList();
        if (lessonIds.isEmpty()) {
            return Map.of();
        }
        return progressRepository.findByUserKeyAndLessonIdIn(userKey, lessonIds).stream()
                .collect(Collectors.toMap(LessonProgressEntity::getLessonId, Function.identity()));
    }

    private String currentUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtService.Principal principal)) {
            return ANONYMOUS_USER_KEY;
        }
        return principal.email();
    }

    private String requireAuthenticatedUser() {
        String userKey = currentUserKey();
        if (ANONYMOUS_USER_KEY.equals(userKey)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userKey;
    }

    private boolean isPremiumUser() {
        String userKey = currentUserKey();
        if (ANONYMOUS_USER_KEY.equals(userKey)) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtService.Principal principal
                && ("ADMIN".equals(principal.role()) || "SUPER_ADMIN".equals(principal.role()))) {
            return true;
        }
        return userSubscriptionRepository.findById(userKey)
                .filter(subscription -> "ACTIVE".equals(subscription.getStatus()))
                .filter(subscription -> subscription.getPlanType() != null && !subscription.getPlanType().isBlank())
                .filter(subscription -> subscription.getExpiresAt() == null || subscription.getExpiresAt().isAfter(OffsetDateTime.now()))
                .isPresent();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeGloss(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private int scoreFromConfidence(Double confidence, boolean correct) {
        if (confidence == null) {
            return correct ? 100 : 0;
        }
        int score = Math.round((float) (confidence * 100));
        return Math.max(0, Math.min(100, score));
    }

    private List<String> feedbackCodes(String aiStatus, Boolean correct, Double confidence) {
        String normalizedStatus = normalize(aiStatus);
        if ("no_hands".equals(normalizedStatus)) {
            return List.of("NO_HANDS_DETECTED");
        }
        if ("error".equals(normalizedStatus)) {
            return List.of("AI_INFERENCE_ERROR");
        }
        if (confidence != null && confidence < 0.70) {
            return List.of("LOW_CONFIDENCE", "IMPROVE_LIGHTING_AND_FRAMING");
        }
        if (Boolean.TRUE.equals(correct)) {
            return List.of("SIGN_MATCH", "CONFIDENCE_ACCEPTABLE");
        }
        if (Boolean.FALSE.equals(correct)) {
            return List.of("SIGN_MISMATCH", "TRY_AGAIN_SLOWLY");
        }
        return List.of("HAND_SHAPE_MATCH", "MOVEMENT_PATH_ACCEPTABLE");
    }

    private void enforceSignatureAttemptRateLimit(String userKey) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(SIGNATURE_ATTEMPT_RATE_WINDOW);
        Deque<Instant> window = signatureAttemptWindows.computeIfAbsent(userKey, ignored -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
                window.removeFirst();
            }
            if (window.size() >= SIGNATURE_ATTEMPT_RATE_LIMIT) {
                throw new BusinessException(ErrorCode.RATE_LIMITED, "Too many signature attempts; please wait before retrying");
            }
            window.addLast(now);
        }
    }

    private String cleanVersion(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
    }

    private void rejectRawAiPayload(SubmitSignatureAttemptRequest request) {
        String joined = String.join(" ",
                request.signatureVector() == null ? "" : request.signatureVector(),
                request.aiStatus() == null ? "" : request.aiStatus(),
                request.targetGloss() == null ? "" : request.targetGloss(),
                request.predictedGloss() == null ? "" : request.predictedGloss()
        ).toLowerCase(Locale.ROOT);
        boolean containsRawPayload = RAW_AI_PAYLOAD_MARKERS.stream().anyMatch(joined::contains);
        if (containsRawPayload) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Raw image/video/base64 payloads are not accepted for signature attempts");
        }
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }
}
