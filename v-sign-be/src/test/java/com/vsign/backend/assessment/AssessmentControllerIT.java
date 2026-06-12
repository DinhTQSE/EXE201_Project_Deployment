package com.vsign.backend.assessment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssessmentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listsAssessmentSummaries() throws Exception {
        mockMvc.perform(get("/api/v1/assessments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].id").value("asl-basics-placement"))
                .andExpect(jsonPath("$.data[0].title").value("ASL Basics Placement"))
                .andExpect(jsonPath("$.data[0].questionCount").value(3))
                .andExpect(jsonPath("$.data[0].passingScore").value(70));
    }

    @Test
    void returnsAssessmentDetailWithQuestions() throws Exception {
        mockMvc.perform(get("/api/v1/assessments/asl-basics-placement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("asl-basics-placement"))
                .andExpect(jsonPath("$.data.questions.length()").value(3))
                .andExpect(jsonPath("$.data.questions[0].id").value("q-hello"))
                .andExpect(jsonPath("$.data.questions[0].options.length()").value(3));
    }

    @Test
    void submitsAnswersAndReturnsScoreResult() throws Exception {
        String requestBody = """
                {
                  "userId": "learner-001",
                  "answers": [
                    {"questionId": "q-hello", "selectedOptionId": "hello"},
                    {"questionId": "q-thank-you", "selectedOptionId": "thank-you"},
                    {"questionId": "q-school", "selectedOptionId": "school"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/assessments/asl-basics-placement/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assessmentId").value("asl-basics-placement"))
                .andExpect(jsonPath("$.data.userId").value("learner-001"))
                .andExpect(jsonPath("$.data.score").value(100))
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.correctAnswers").value(3))
                .andExpect(jsonPath("$.data.totalQuestions").value(3))
                .andExpect(jsonPath("$.data.awardedXp").value(30));
    }

    @Test
    void rejectsSubmissionWithMissingAnswers() throws Exception {
        String requestBody = """
                {
                  "userId": "learner-001",
                  "answers": []
                }
                """;

        mockMvc.perform(post("/api/v1/assessments/asl-basics-placement/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void returnsLessonQuizWithoutCorrectAnswerIds() throws Exception {
        mockMvc.perform(get("/api/v1/lessons/lesson-greetings/quiz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lessonId").value("lesson-greetings"))
                .andExpect(jsonPath("$.data.quizId").value("quiz-greetings"))
                .andExpect(jsonPath("$.data.attemptId").isNotEmpty())
                .andExpect(jsonPath("$.data.questions.length()").value(2))
                .andExpect(jsonPath("$.data.questions[0].id").value("quiz-q-hello"))
                .andExpect(jsonPath("$.data.questions[0].correctAnswerId").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].options[0].correct").doesNotExist());
    }

    @Test
    void rejectsPremiumLessonQuizUntilPremiumAccessIsAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/lessons/lesson-premium-conversation/quiz"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PREMIUM_REQUIRED"));
    }

    @Test
    void returnsDomainErrorForMissingLessonQuiz() throws Exception {
        mockMvc.perform(get("/api/v1/lessons/lesson-missing/quiz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LESSON_NOT_FOUND"));
    }

    @Test
    void submitsQuizAttemptAndReturnsResultFields() throws Exception {
        String requestBody = """
                {
                  "answers": [
                    {"questionId": "quiz-q-hello", "selectedAnswerId": "answer-hello"},
                    {"questionId": "quiz-q-thanks", "selectedAnswerId": "answer-thanks-wrong"}
                  ],
                  "durationSeconds": 95
                }
                """;

        mockMvc.perform(post("/api/v1/quiz-attempts/attempt-greetings-ready/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attemptId").value("attempt-greetings-ready"))
                .andExpect(jsonPath("$.data.score").value(50))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andExpect(jsonPath("$.data.xpAwarded").value(10))
                .andExpect(jsonPath("$.data.reviewAvailable").value(true))
                .andExpect(jsonPath("$.data.timedOut").value(false))
                .andExpect(jsonPath("$.data.unansweredCount").value(0));
    }

    @Test
    void fetchesUsableQuizAttemptsAfterEarlierAttemptWasSubmitted() throws Exception {
        String firstAttemptId = fetchLessonQuizAttemptId();
        String secondAttemptId = fetchLessonQuizAttemptId();

        submitCorrectAttempt(firstAttemptId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptId").value(firstAttemptId));

        submitCorrectAttempt(secondAttemptId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptId").value(secondAttemptId));
    }

    @Test
    void returnsSubmittedQuizAttemptReviewWithAnswersAndExplanation() throws Exception {
        mockMvc.perform(get("/api/v1/quiz-attempts/attempt-greetings-reviewed/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attemptId").value("attempt-greetings-reviewed"))
                .andExpect(jsonPath("$.data.questions[0].selectedAnswerId").value("answer-hello"))
                .andExpect(jsonPath("$.data.questions[0].correctAnswerId").value("answer-hello"))
                .andExpect(jsonPath("$.data.questions[0].explanation").isNotEmpty());
    }

    @Test
    void countsUnansweredQuizQuestionsAsIncorrect() throws Exception {
        String requestBody = """
                {
                  "answers": [
                    {"questionId": "quiz-q-hello", "selectedAnswerId": "answer-hello"}
                  ],
                  "durationSeconds": 65
                }
                """;

        mockMvc.perform(post("/api/v1/quiz-attempts/attempt-greetings-partial/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(50))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andExpect(jsonPath("$.data.unansweredCount").value(1));
    }

    @Test
    void rejectsMixedKnownAndUnknownQuizQuestions() throws Exception {
        String requestBody = """
                {
                  "answers": [
                    {"questionId": "quiz-q-hello", "selectedAnswerId": "answer-hello"},
                    {"questionId": "quiz-q-unknown", "selectedAnswerId": "answer-unknown"}
                  ],
                  "durationSeconds": 55
                }
                """;

        mockMvc.perform(post("/api/v1/quiz-attempts/attempt-greetings-mixed/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsQuizAnswerThatDoesNotBelongToQuestionBeforeAttemptIsConsumed() throws Exception {
        String attemptId = fetchLessonQuizAttemptId();
        String invalidRequestBody = """
                {
                  "answers": [
                    {"questionId": "quiz-q-hello", "selectedAnswerId": "answer-not-an-option"}
                  ],
                  "durationSeconds": 30
                }
                """;

        mockMvc.perform(post("/api/v1/quiz-attempts/{attemptId}/submit", attemptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        submitCorrectAttempt(attemptId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptId").value(attemptId));
    }

    @Test
    void rejectsQuizAttemptResubmission() throws Exception {
        String requestBody = """
                {
                  "answers": [
                    {"questionId": "quiz-q-hello", "selectedAnswerId": "answer-hello"}
                  ],
                  "durationSeconds": 30
                }
                """;

        mockMvc.perform(post("/api/v1/quiz-attempts/attempt-greetings-reviewed/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTEMPT_ALREADY_SUBMITTED"));
    }

    @Test
    void returnsDomainErrorForMissingQuizAttempt() throws Exception {
        mockMvc.perform(get("/api/v1/quiz-attempts/attempt-missing/review"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ATTEMPT_NOT_FOUND"));
    }

    private String fetchLessonQuizAttemptId() throws Exception {
        String response = mockMvc.perform(get("/api/v1/lessons/lesson-greetings/quiz"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode data = objectMapper.readTree(response).path("data");
        return data.path("attemptId").asText();
    }

    private org.springframework.test.web.servlet.ResultActions submitCorrectAttempt(String attemptId) throws Exception {
        String requestBody = """
                {
                  "answers": [
                    {"questionId": "quiz-q-hello", "selectedAnswerId": "answer-hello"},
                    {"questionId": "quiz-q-thanks", "selectedAnswerId": "answer-thanks"}
                  ],
                  "durationSeconds": 35
                }
                """;
        return mockMvc.perform(post("/api/v1/quiz-attempts/{attemptId}/submit", attemptId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }
}
