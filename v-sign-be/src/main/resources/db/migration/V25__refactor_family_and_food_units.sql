-- V25: Refactor Gia đình (Unit 1) và Hoạt động thường ngày (Unit 3)

-- 1. Đổi tên và mô tả các Chủ đề (Units)
UPDATE learning_units
SET title = 'Gia đình',
    description = 'Học các ký hiệu về người thân, ông bà và thế hệ anh chị em.'
WHERE unit_id = 'unit-mvp-family';

UPDATE learning_units
SET title = 'Hoạt động thường ngày',
    description = 'Học các ký hiệu về món ăn thường ngày và ăn uống.'
WHERE unit_id = 'unit-mvp-food';

-- 2. Đổi tên và mô tả Chương 1 của Gia đình và Chương 1 của Hoạt động thường ngày
UPDATE learning_chapters
SET title = 'Gia đình hạt nhân & Ông bà',
    description = 'Học các ký hiệu về bố, mẹ, ông bà, con trai và con gái.',
    order_index = 1
WHERE chapter_id = 'chapter-mvp-family-core';

UPDATE learning_chapters
SET title = 'Món ăn thường ngày',
    description = 'Học các ký hiệu món ăn thường ngày và ăn uống.',
    order_index = 1
WHERE chapter_id = 'chapter-mvp-food-core';

-- 3. Tạo chương mới: "Thế hệ anh chị em" dưới Gia đình (Unit 1)
-- Dọn dẹp trước nếu chạy lại migration
DELETE FROM quiz_options WHERE question_id IN ('quiz-q-mvp-ongba-word', 'quiz-q-mvp-ongba-video');
DELETE FROM quiz_questions WHERE question_id IN ('quiz-q-mvp-ongba-word', 'quiz-q-mvp-ongba-video');
DELETE FROM lesson_quizzes WHERE quiz_id = 'quiz-mvp-ongba';
DELETE FROM practice_item_rubrics WHERE practice_item_id = 'practice-mvp-ongba';
DELETE FROM practice_items WHERE practice_item_id = 'practice-mvp-ongba';
DELETE FROM learning_lessons WHERE lesson_id = 'lesson-mvp-ongba';
DELETE FROM learning_chapters WHERE chapter_id = 'chapter-mvp-family-siblings';

INSERT INTO learning_chapters (chapter_id, unit_id, title, description, is_premium, is_published, order_index)
VALUES ('chapter-mvp-family-siblings', 'unit-mvp-family', 'Thế hệ anh chị em', 'Học các ký hiệu anh hai, chị và em trong gia đình với các biến thể vùng miền.', false, true, 2);

-- 4. Phân bổ các bài học (Lessons) và đổi tên/cập nhật chương học
-- Di chuyển các bài học hạt nhân & con cái về chapter-mvp-family-core
UPDATE learning_lessons
SET chapter_id = 'chapter-mvp-family-core',
    order_index = 1
WHERE lesson_id = 'lesson-mvp-bo';

UPDATE learning_lessons
SET chapter_id = 'chapter-mvp-family-core',
    order_index = 2
WHERE lesson_id = 'lesson-mvp-me';

UPDATE learning_lessons
SET chapter_id = 'chapter-mvp-family-core',
    order_index = 4
WHERE lesson_id = 'lesson-mvp-contrai';

UPDATE learning_lessons
SET chapter_id = 'chapter-mvp-family-core',
    order_index = 5
WHERE lesson_id = 'lesson-mvp-congai';

-- Di chuyển các bài học anh chị em về chapter-mvp-family-siblings và đổi tên theo yêu cầu
UPDATE learning_lessons
SET chapter_id = 'chapter-mvp-family-siblings',
    title = 'Anh hai (miền trung)',
    order_index = 1
WHERE lesson_id = 'lesson-mvp-anhhai-bt';

UPDATE learning_lessons
SET chapter_id = 'chapter-mvp-family-siblings',
    title = 'Anh hai (miền nam)',
    order_index = 2
WHERE lesson_id = 'lesson-mvp-anhhai';

UPDATE learning_lessons
SET chapter_id = 'chapter-mvp-family-siblings',
    title = 'Chị (miền nam)',
    order_index = 3
WHERE lesson_id = 'lesson-mvp-chi';

UPDATE learning_lessons
SET chapter_id = 'chapter-mvp-family-siblings',
    title = 'Chị (miền trung)',
    order_index = 4
WHERE lesson_id = 'lesson-mvp-chi-bt';

UPDATE learning_lessons
SET chapter_id = 'chapter-mvp-family-siblings',
    title = 'Em gái',
    order_index = 5
WHERE lesson_id = 'lesson-mvp-emgai';

UPDATE learning_lessons
SET chapter_id = 'chapter-mvp-family-siblings',
    title = 'Em trai (miền nam)',
    order_index = 6
WHERE lesson_id = 'lesson-mvp-emtrai-nt';

UPDATE learning_lessons
SET chapter_id = 'chapter-mvp-family-siblings',
    title = 'Em trai (miền trung)',
    order_index = 7
WHERE lesson_id = 'lesson-mvp-emtrai';

-- 5. Thêm bài học mới "Ông Bà" dưới chapter-mvp-family-core
INSERT INTO learning_lessons (lesson_id, chapter_id, title, description, video_url, duration_seconds, is_premium, is_published, order_index)
VALUES (
    'lesson-mvp-ongba',
    'chapter-mvp-family-core',
    'Ông Bà',
    'Ký hiệu ông bà trong gia đình.',
    'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0151B.mp4',
    60,
    false,
    true,
    3
);

-- Thêm practice item cho bài học Ông Bà
INSERT INTO practice_items (practice_item_id, lesson_id, category, level, label, expected_gloss, source_video_file, is_published, order_index, video_url)
VALUES (
    'practice-mvp-ongba',
    'lesson-mvp-ongba',
    'family',
    'beginner',
    'Ông bà',
    'ONG_BA',
    'D0151B.mp4',
    true,
    303,
    'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0151B.mp4'
);

-- Thêm rubrics cho practice-mvp-ongba
INSERT INTO practice_item_rubrics (practice_item_id, code, order_index)
VALUES 
('practice-mvp-ongba', 'HAND_SHAPE_MATCH', 1),
('practice-mvp-ongba', 'PALM_ORIENTATION_MATCH', 2),
('practice-mvp-ongba', 'MOVEMENT_PATH_MATCH', 3);

-- Thêm Quiz cho bài học Ông Bà
INSERT INTO lesson_quizzes (quiz_id, lesson_id, title, passing_score, xp_award, requires_premium, is_published)
VALUES ('quiz-mvp-ongba', 'lesson-mvp-ongba', 'Kiểm tra: Ông Bà', 70, 10, false, true);

INSERT INTO quiz_questions (question_id, quiz_id, prompt, correct_answer_id, order_index)
VALUES 
('quiz-q-mvp-ongba-word', 'quiz-mvp-ongba', 'Xem video mẫu và chọn từ đúng.', 'answer-mvp-ongba-word-correct', 1),
('quiz-q-mvp-ongba-video', 'quiz-mvp-ongba', 'Chọn video đúng cho: Ông Bà', 'answer-mvp-ongba-video-correct', 2);

INSERT INTO quiz_options (question_id, answer_id, text, video_url, order_index)
VALUES 
('quiz-q-mvp-ongba-word', 'answer-mvp-ongba-word-correct', 'Ông Bà', null, 1),
('quiz-q-mvp-ongba-word', 'answer-mvp-ongba-word-distractor', 'Bố', null, 2),
('quiz-q-mvp-ongba-video', 'answer-mvp-ongba-video-correct', 'Video A', 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0151B.mp4', 1),
('quiz-q-mvp-ongba-video', 'answer-mvp-ongba-video-distractor', 'Video B', 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W00325.mp4', 2);

-- Cập nhật label/ expected gloss cho bài học Ông Bà trong các bảng liên quan nếu cần thiết
-- Bảng practice_items đã được liên kết đúng.
