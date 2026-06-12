package com.vsign.backend.assessment.service;

import com.vsign.backend.assessment.dto.AnswerRequest;
import com.vsign.backend.assessment.dto.AssessmentDetailResponse;
import com.vsign.backend.assessment.dto.AssessmentSubmissionRequest;
import com.vsign.backend.assessment.dto.AssessmentSubmissionResultResponse;
import com.vsign.backend.assessment.dto.AssessmentSummaryResponse;
import com.vsign.backend.assessment.dto.OptionResponse;
import com.vsign.backend.assessment.dto.QuestionResponse;
import com.vsign.backend.assessment.persistence.AssessmentEntity;
import com.vsign.backend.assessment.persistence.AssessmentOptionEntity;
import com.vsign.backend.assessment.persistence.AssessmentOptionRepository;
import com.vsign.backend.assessment.persistence.AssessmentQuestionEntity;
import com.vsign.backend.assessment.persistence.AssessmentQuestionRepository;
import com.vsign.backend.assessment.persistence.AssessmentRepository;
import com.vsign.backend.assessment.persistence.AssessmentSubmissionAnswerEntity;
import com.vsign.backend.assessment.persistence.AssessmentSubmissionAnswerRepository;
import com.vsign.backend.assessment.persistence.AssessmentSubmissionEntity;
import com.vsign.backend.assessment.persistence.AssessmentSubmissionRepository;
import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssessmentService {
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentOptionRepository optionRepository;
    private final AssessmentSubmissionRepository submissionRepository;
    private final AssessmentSubmissionAnswerRepository submissionAnswerRepository;

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            AssessmentQuestionRepository questionRepository,
            AssessmentOptionRepository optionRepository,
            AssessmentSubmissionRepository submissionRepository,
            AssessmentSubmissionAnswerRepository submissionAnswerRepository
    ) {
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.submissionRepository = submissionRepository;
        this.submissionAnswerRepository = submissionAnswerRepository;
    }

    public List<AssessmentSummaryResponse> listAssessments() {
        return assessmentRepository.findByPublishedTrueOrderByOrderIndexAsc().stream()
                .map(assessment -> new AssessmentSummaryResponse(
                        assessment.getAssessmentId(),
                        assessment.getTitle(),
                        (int) questionRepository.countByAssessmentId(assessment.getAssessmentId()),
                        assessment.getPassingScore()
                ))
                .toList();
    }

    public AssessmentDetailResponse getAssessment(String assessmentId) {
        AssessmentEntity assessment = findAssessment(assessmentId);
        List<AssessmentQuestionEntity> questions = questionRepository.findByAssessmentIdOrderByOrderIndexAsc(assessmentId);
        Map<String, List<AssessmentOptionEntity>> optionsByQuestion = optionsByQuestion(questions);
        return new AssessmentDetailResponse(
                assessment.getAssessmentId(),
                assessment.getTitle(),
                assessment.getPassingScore(),
                questions.stream()
                        .map(question -> toQuestion(question, optionsByQuestion.getOrDefault(question.getQuestionId(), List.of())))
                        .toList()
        );
    }

    @Transactional
    public AssessmentSubmissionResultResponse submit(String assessmentId, AssessmentSubmissionRequest request) {
        AssessmentEntity assessment = findAssessment(assessmentId);
        if (request.answers() == null || request.answers().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        List<AssessmentQuestionEntity> questions = questionRepository.findByAssessmentIdOrderByOrderIndexAsc(assessmentId);
        Map<String, AssessmentQuestionEntity> questionsById = questions.stream()
                .collect(Collectors.toMap(AssessmentQuestionEntity::getQuestionId, question -> question));
        Map<String, List<AssessmentOptionEntity>> optionsByQuestion = optionsByQuestion(questions);
        Map<String, Set<String>> optionIdsByQuestion = optionsByQuestion.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(AssessmentOptionEntity::getOptionId).collect(Collectors.toSet())
                ));

        Map<String, String> answers = request.answers().stream()
                .collect(Collectors.toMap(AnswerRequest::questionId, AnswerRequest::selectedAnswer, (left, right) -> right));
        validateAnswers(answers, questionsById, optionIdsByQuestion);

        int correct = 0;
        for (AssessmentQuestionEntity question : questions) {
            if (question.getCorrectAnswerId().equals(answers.get(question.getQuestionId()))) {
                correct++;
            }
        }
        int score = questions.isEmpty() ? 0 : correct * 100 / questions.size();
        boolean passed = score >= assessment.getPassingScore();
        int awardedXp = passed ? 30 : 0;

        AssessmentSubmissionEntity submission = submissionRepository.save(new AssessmentSubmissionEntity(
                assessmentId,
                request.userId(),
                score,
                passed,
                correct,
                questions.size(),
                awardedXp
        ));
        for (AssessmentQuestionEntity question : questions) {
            String selectedAnswer = answers.get(question.getQuestionId());
            submissionAnswerRepository.save(new AssessmentSubmissionAnswerEntity(
                    submission.getId(),
                    question.getQuestionId(),
                    selectedAnswer,
                    question.getCorrectAnswerId().equals(selectedAnswer)
            ));
        }

        return new AssessmentSubmissionResultResponse(
                assessmentId,
                request.userId(),
                score,
                passed,
                correct,
                questions.size(),
                awardedXp
        );
    }

    private AssessmentEntity findAssessment(String assessmentId) {
        return assessmentRepository.findByAssessmentIdAndPublishedTrue(assessmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private Map<String, List<AssessmentOptionEntity>> optionsByQuestion(List<AssessmentQuestionEntity> questions) {
        List<String> questionIds = questions.stream()
                .map(AssessmentQuestionEntity::getQuestionId)
                .toList();
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return optionRepository.findByQuestionIdInOrderByOrderIndexAsc(questionIds).stream()
                .collect(Collectors.groupingBy(AssessmentOptionEntity::getQuestionId));
    }

    private void validateAnswers(
            Map<String, String> answers,
            Map<String, AssessmentQuestionEntity> questionsById,
            Map<String, Set<String>> optionIdsByQuestion
    ) {
        for (Map.Entry<String, String> answer : answers.entrySet()) {
            if (!questionsById.containsKey(answer.getKey())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            if (answer.getValue() == null || !optionIdsByQuestion.getOrDefault(answer.getKey(), Set.of()).contains(answer.getValue())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
        }
    }

    private QuestionResponse toQuestion(AssessmentQuestionEntity question, List<AssessmentOptionEntity> options) {
        return new QuestionResponse(
                question.getQuestionId(),
                question.getPrompt(),
                options.stream()
                        .map(option -> new OptionResponse(option.getOptionId(), option.getText(), null))
                        .toList(),
                null
        );
    }
}
