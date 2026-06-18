package com.vsign.backend.assessment.service;

import com.vsign.backend.assessment.dto.OptionResponse;
import com.vsign.backend.assessment.dto.QuestionResponse;
import com.vsign.backend.assessment.dto.QuizAnswerRequest;
import com.vsign.backend.assessment.dto.QuizResponse;
import com.vsign.backend.assessment.dto.QuizResultResponse;
import com.vsign.backend.assessment.dto.QuizReviewQuestionResponse;
import com.vsign.backend.assessment.dto.QuizReviewResponse;
import com.vsign.backend.assessment.dto.SubmitAttemptRequest;
import com.vsign.backend.assessment.persistence.QuizAttemptAnswerEntity;
import com.vsign.backend.assessment.persistence.QuizAttemptAnswerRepository;
import com.vsign.backend.assessment.persistence.QuizAttemptEntity;
import com.vsign.backend.assessment.persistence.QuizAttemptRepository;
import com.vsign.backend.assessment.persistence.QuizEntity;
import com.vsign.backend.assessment.persistence.QuizOptionEntity;
import com.vsign.backend.assessment.persistence.QuizOptionRepository;
import com.vsign.backend.assessment.persistence.QuizQuestionEntity;
import com.vsign.backend.assessment.persistence.QuizQuestionRepository;
import com.vsign.backend.assessment.persistence.QuizRepository;
import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import com.vsign.backend.common.security.JwtService;
import com.vsign.backend.learning.persistence.LearningChapterEntity;
import com.vsign.backend.learning.persistence.LearningChapterRepository;
import com.vsign.backend.learning.persistence.LearningLessonEntity;
import com.vsign.backend.learning.persistence.LearningLessonRepository;
import com.vsign.backend.payment.persistence.UserTierRepository;
import com.vsign.backend.payment.persistence.UserTierEntity;
import com.vsign.backend.payment.persistence.TierFeatureRepository;
import com.vsign.backend.payment.persistence.TierFeatureEntity;
import java.time.LocalDateTime;
import java.util.List;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QuizAttemptService {
    private static final String ANONYMOUS_USER_KEY = "anonymous";

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizOptionRepository optionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAttemptAnswerRepository attemptAnswerRepository;
    private final LearningLessonRepository lessonRepository;
    private final LearningChapterRepository chapterRepository;
    private final UserTierRepository userTierRepository;
    private final TierFeatureRepository tierFeatureRepository;

    public QuizAttemptService(
            QuizRepository quizRepository,
            QuizQuestionRepository questionRepository,
            QuizOptionRepository optionRepository,
            QuizAttemptRepository attemptRepository,
            QuizAttemptAnswerRepository attemptAnswerRepository,
            LearningLessonRepository lessonRepository,
            LearningChapterRepository chapterRepository,
            UserTierRepository userTierRepository,
            TierFeatureRepository tierFeatureRepository
    ) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.lessonRepository = lessonRepository;
        this.chapterRepository = chapterRepository;
        this.userTierRepository = userTierRepository;
        this.tierFeatureRepository = tierFeatureRepository;
    }

    @Transactional
    public QuizResponse getLessonQuiz(String lessonId) {
        QuizEntity quiz = quizRepository.findByLessonIdAndPublishedTrue(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));

        LearningLessonEntity lesson = lessonRepository.findByLessonIdAndPublishedTrue(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));
        LearningChapterEntity chapter = chapterRepository.findById(lesson.getChapterId())
                .filter(LearningChapterEntity::isPublished)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAPTER_NOT_FOUND));
        
        boolean premiumUser = isPremiumUser();
        List<LearningChapterEntity> chaptersInUnit = chapterRepository.findByUnitIdAndPublishedTrueOrderByOrderIndexAsc(chapter.getUnitId());
        String firstChapterId = chaptersInUnit.isEmpty() ? null : chaptersInUnit.get(0).getChapterId();
        boolean isFirstChapter = chapter.getChapterId().equals(firstChapterId);
        int chapterLimit = getFeatureLimit("chapter_access");
        boolean chapterLockedForFree = (chapterLimit == 1 && !isFirstChapter);

        if ((quiz.isRequiresPremium() || chapter.isPremium() || lesson.isPremium() || chapterLockedForFree) && !premiumUser) {
            throw new BusinessException(ErrorCode.PREMIUM_REQUIRED);
        }


        String attemptId = "attempt-" + UUID.randomUUID();
        attemptRepository.save(new QuizAttemptEntity(attemptId, quiz.getQuizId(), lessonId, currentUserKey()));
        List<QuizQuestionEntity> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getQuizId());
        Map<String, List<QuizOptionEntity>> optionsByQuestion = optionsByQuestion(questions);
        return new QuizResponse(
                lessonId,
                quiz.getQuizId(),
                attemptId,
                questions.stream()
                        .map(question -> toPublicQuestion(question, optionsByQuestion.getOrDefault(question.getQuestionId(), List.of())))
                        .toList()
        );
    }

    @Transactional
    public QuizResultResponse submit(String attemptId, SubmitAttemptRequest request) {
        QuizAttemptEntity attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTEMPT_NOT_FOUND));
        requireAttemptOwner(attempt);
        if (attempt.isSubmitted()) {
            throw new BusinessException(ErrorCode.ATTEMPT_ALREADY_SUBMITTED);
        }

        QuizEntity quiz = quizRepository.findById(attempt.getQuizId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        List<QuizQuestionEntity> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getQuizId());
        Map<String, QuizQuestionEntity> questionsById = questions.stream()
                .collect(Collectors.toMap(QuizQuestionEntity::getQuestionId, question -> question));
        Map<String, Set<String>> answerIdsByQuestion = optionsByQuestion(questions).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(QuizOptionEntity::getAnswerId).collect(Collectors.toSet())
                ));

        List<QuizAnswerRequest> answers = request.answers() == null ? List.of() : request.answers();
        validateAnswers(answers, questionsById, answerIdsByQuestion);
        Map<String, String> answerMap = answers.stream()
                .collect(Collectors.toMap(QuizAnswerRequest::questionId, QuizAnswerRequest::selectedAnswerId, (left, right) -> right));

        int correct = 0;
        for (QuizQuestionEntity question : questions) {
            if (question.getCorrectAnswerId().equals(answerMap.get(question.getQuestionId()))) {
                correct++;
            }
        }
        int unanswered = (int) questions.stream()
                .filter(question -> !answerMap.containsKey(question.getQuestionId()))
                .count();
        int score = questions.isEmpty() ? 0 : correct * 100 / questions.size();
        boolean passed = score >= quiz.getPassingScore();

        attemptAnswerRepository.deleteByAttemptId(attemptId);
        for (QuizQuestionEntity question : questions) {
            String selectedAnswerId = answerMap.get(question.getQuestionId());
            attemptAnswerRepository.save(new QuizAttemptAnswerEntity(
                    attemptId,
                    question.getQuestionId(),
                    selectedAnswerId,
                    question.getCorrectAnswerId().equals(selectedAnswerId)
            ));
        }
        attempt.submit(score, passed, request.durationSeconds(), false);
        attemptRepository.save(attempt);

        return new QuizResultResponse(attemptId, score, passed, quiz.getXpAward(), true, false, unanswered);
    }

    public QuizReviewResponse review(String attemptId) {
        QuizAttemptEntity attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTEMPT_NOT_FOUND));
        requireAttemptOwner(attempt);
        if (!attempt.isSubmitted()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Attempt has not been submitted");
        }

        List<QuizQuestionEntity> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(attempt.getQuizId());
        Map<String, QuizAttemptAnswerEntity> answersByQuestion = attemptAnswerRepository.findByAttemptIdOrderByIdAsc(attemptId).stream()
                .collect(Collectors.toMap(QuizAttemptAnswerEntity::getQuestionId, answer -> answer, (left, right) -> right));
        List<QuizReviewQuestionResponse> reviewQuestions = questions.stream()
                .map(question -> {
                    QuizAttemptAnswerEntity answer = answersByQuestion.get(question.getQuestionId());
                    String selectedAnswerId = answer == null ? null : answer.getSelectedAnswerId();
                    return new QuizReviewQuestionResponse(
                            question.getQuestionId(),
                            selectedAnswerId,
                            question.getCorrectAnswerId(),
                            question.getCorrectAnswerId().equals(selectedAnswerId),
                            "Review the hand shape and movement for " + question.getPrompt()
                    );
                })
                .toList();
        return new QuizReviewResponse(attemptId, attempt.getScore(), attempt.isPassed(), reviewQuestions);
    }

    private void validateAnswers(
            List<QuizAnswerRequest> answers,
            Map<String, QuizQuestionEntity> questionsById,
            Map<String, Set<String>> answerIdsByQuestion
    ) {
        for (QuizAnswerRequest answer : answers) {
            if (!questionsById.containsKey(answer.questionId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            if (!answerIdsByQuestion.getOrDefault(answer.questionId(), Set.of()).contains(answer.selectedAnswerId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
        }
    }

    private Map<String, List<QuizOptionEntity>> optionsByQuestion(List<QuizQuestionEntity> questions) {
        List<String> questionIds = questions.stream()
                .map(QuizQuestionEntity::getQuestionId)
                .toList();
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return optionRepository.findByQuestionIdInOrderByOrderIndexAsc(questionIds).stream()
                .collect(Collectors.groupingBy(QuizOptionEntity::getQuestionId));
    }

    private QuestionResponse toPublicQuestion(QuizQuestionEntity question, List<QuizOptionEntity> options) {
        return new QuestionResponse(
                question.getQuestionId(),
                question.getPrompt(),
                options.stream()
                        .map(option -> new OptionResponse(option.getAnswerId(), option.getText(), option.getVideoUrl()))
                        .toList(),
                null
        );
    }

    private void requireAttemptOwner(QuizAttemptEntity attempt) {
        String currentUserKey = currentUserKey();
        if (!attempt.getUserKey().equals(currentUserKey)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Quiz attempt does not belong to the current user");
        }
    }

    private String currentUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtService.Principal principal)) {
            return ANONYMOUS_USER_KEY;
        }
        return principal.email();
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
        List<UserTierEntity> active = userTierRepository.findCurrentActiveByEmail(userKey, LocalDateTime.now());
        if (active.isEmpty()) {
            return false;
        }
        String title = active.get(0).getTier().getTitle();
        return "plus".equalsIgnoreCase(title) || "pro".equalsIgnoreCase(title);
    }

    private int getFeatureLimit(String featureKey) {
        String userKey = currentUserKey();
        if (ANONYMOUS_USER_KEY.equals(userKey)) {
            if ("chapter_access".equals(featureKey)) return 1;
            return 0;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtService.Principal principal
                && ("ADMIN".equals(principal.role()) || "SUPER_ADMIN".equals(principal.role()))) {
            return -1;
        }
        
        String tierTitle = "FREE";
        List<UserTierEntity> active = userTierRepository.findCurrentActiveByEmail(userKey, LocalDateTime.now());
        if (!active.isEmpty()) {
            tierTitle = active.get(0).getTier().getTitle();
        }
        
        List<TierFeatureEntity> features = tierFeatureRepository.findByTier_TitleIgnoreCase(tierTitle);
        for (TierFeatureEntity feature : features) {
            if (feature.getFeatureKey().equalsIgnoreCase(featureKey)) {
                return feature.getLimitValue();
            }
        }
        if ("chapter_access".equals(featureKey)) return 1;
        return 0;
    }
}

