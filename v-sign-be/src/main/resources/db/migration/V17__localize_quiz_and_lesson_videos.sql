-- V17: Việt hóa nội dung quiz/assessment và thêm video URL cho các bài học seeded

-- ── Lesson quiz questions ─────────────────────────────────────────────────────
UPDATE lesson_quizzes
SET title = 'Kiểm tra Chào hỏi'
WHERE quiz_id IN ('quiz-greetings', 'quiz-greetings-catalog');

UPDATE quiz_questions SET prompt = 'Ký hiệu trong video có nghĩa là từ nào?'
WHERE question_id IN ('quiz-q-hello', 'quiz-q-hello-catalog');

UPDATE quiz_questions SET prompt = 'Chọn từ đúng cho ký hiệu "Cảm ơn" trong tiếng Việt:'
WHERE question_id IN ('quiz-q-thanks', 'quiz-q-thanks-catalog');

UPDATE quiz_questions SET prompt = 'Ký hiệu nào thể hiện "Hội thoại" trong ngôn ngữ ký hiệu?'
WHERE question_id = 'quiz-q-premium';

-- ── Lesson quiz options ───────────────────────────────────────────────────────
UPDATE quiz_options SET text = 'Xin chào'   WHERE question_id = 'quiz-q-hello'         AND answer_id = 'answer-hello';
UPDATE quiz_options SET text = 'Cảm ơn'     WHERE question_id = 'quiz-q-hello'         AND answer_id = 'answer-hello-wrong';
UPDATE quiz_options SET text = 'Cảm ơn'     WHERE question_id = 'quiz-q-thanks'        AND answer_id = 'answer-thanks';
UPDATE quiz_options SET text = 'Trường học'  WHERE question_id = 'quiz-q-thanks'        AND answer_id = 'answer-thanks-wrong';
UPDATE quiz_options SET text = 'Xin chào'   WHERE question_id = 'quiz-q-hello-catalog' AND answer_id = 'answer-hello';
UPDATE quiz_options SET text = 'Cảm ơn'     WHERE question_id = 'quiz-q-hello-catalog' AND answer_id = 'answer-hello-wrong';
UPDATE quiz_options SET text = 'Cảm ơn'     WHERE question_id = 'quiz-q-thanks-catalog' AND answer_id = 'answer-thanks';
UPDATE quiz_options SET text = 'Trường học'  WHERE question_id = 'quiz-q-thanks-catalog' AND answer_id = 'answer-thanks-wrong';
UPDATE quiz_options SET text = 'Hội thoại'  WHERE question_id = 'quiz-q-premium'       AND answer_id = 'answer-conversation';
UPDATE quiz_options SET text = 'Trường học'  WHERE question_id = 'quiz-q-premium'       AND answer_id = 'answer-conversation-wrong';

-- ── Assessment questions & options ────────────────────────────────────────────
UPDATE assessment_questions SET prompt = 'Chọn ký hiệu biểu thị lời "Xin chào"'  WHERE question_id = 'q-hello';
UPDATE assessment_questions SET prompt = 'Chọn ký hiệu biểu thị lời "Cảm ơn"'    WHERE question_id = 'q-thank-you';
UPDATE assessment_questions SET prompt = 'Chọn ký hiệu biểu thị "Trường học"'     WHERE question_id = 'q-school';

UPDATE assessment_options SET text = 'Xin chào'   WHERE question_id = 'q-hello'    AND option_id = 'hello';
UPDATE assessment_options SET text = 'Cảm ơn'     WHERE question_id = 'q-hello'    AND option_id = 'thanks';
UPDATE assessment_options SET text = 'Trường học'  WHERE question_id = 'q-hello'    AND option_id = 'school';
UPDATE assessment_options SET text = 'Xin chào'   WHERE question_id = 'q-thank-you' AND option_id = 'hello';
UPDATE assessment_options SET text = 'Cảm ơn'     WHERE question_id = 'q-thank-you' AND option_id = 'thank-you';
UPDATE assessment_options SET text = 'Gia đình'    WHERE question_id = 'q-thank-you' AND option_id = 'family';
UPDATE assessment_options SET text = 'Chợ'         WHERE question_id = 'q-school'   AND option_id = 'market';
UPDATE assessment_options SET text = 'Trường học'  WHERE question_id = 'q-school'   AND option_id = 'school';
UPDATE assessment_options SET text = 'Nhà'         WHERE question_id = 'q-school'   AND option_id = 'home';

-- ── Video URLs cho các bài học seeded (dùng file đầu tiên của practice items) ─
-- Base: https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/<filename>
UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W00489.mp4'
WHERE lesson_id = 'lesson-greetings-1'     AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0151B.mp4'
WHERE lesson_id = 'lesson-greetings-2'     AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W00042.mp4'
WHERE lesson_id = 'lesson-daily-food-1'    AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0485.mp4'
WHERE lesson_id = 'lesson-daily-home-1'    AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03673B.mp4'
WHERE lesson_id = 'lesson-school-1'        AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0001N.mp4'
WHERE lesson_id = 'lesson-places-1'        AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W00432.mp4'
WHERE lesson_id = 'lesson-health-1'        AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0355.mp4'
WHERE lesson_id = 'lesson-health-2'        AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W02348.mp4'
WHERE lesson_id = 'lesson-tech-business-1' AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W01261.mp4'
WHERE lesson_id = 'lesson-civic-business-1' AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0146.mp4'
WHERE lesson_id = 'lesson-numbers-1'       AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0410B.mp4'
WHERE lesson_id = 'lesson-time-1'          AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03889B.mp4'
WHERE lesson_id = 'lesson-emotions-1'      AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0128B.mp4'
WHERE lesson_id = 'lesson-description-1'   AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W00122.mp4'
WHERE lesson_id = 'lesson-transport-1'     AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0015.mp4'
WHERE lesson_id = 'lesson-geography-1'     AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W01441.mp4'
WHERE lesson_id = 'lesson-jobs-1'          AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W01928.mp4'
WHERE lesson_id = 'lesson-workplace-1'     AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W00664B.mp4'
WHERE lesson_id = 'lesson-holidays-1'      AND video_url IS NULL;

UPDATE learning_lessons SET video_url = 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0059.mp4'
WHERE lesson_id = 'lesson-animals-1'       AND video_url IS NULL;
