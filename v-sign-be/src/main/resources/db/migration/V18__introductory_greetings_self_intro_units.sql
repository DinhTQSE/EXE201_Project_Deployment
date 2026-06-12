-- V18: Curated beginner units from requested greeting and self-introduction vocabulary.
-- Mapping rule:
-- 1. Prefer exact label.csv matches.
-- 2. If exact phrase is unavailable, use only a semantically correct equivalent in the same communication context.
-- 3. If no correct equivalent exists, do not create a misleading lesson.

insert into learning_units (unit_id, title, description, thumbnail_url, is_published, order_index) values
('unit-intro-greetings', 'Chào hỏi nhập môn', 'Nhóm ký hiệu chào hỏi và kết thúc hội thoại có video đúng ngữ cảnh trong dataset.', null, true, 9),
('unit-self-introduction', 'Giới thiệu bản thân', 'Nhóm ký hiệu thành phần dùng để hỏi tên, nói nơi sống và bắt đầu giao tiếp cá nhân.', null, true, 10);

insert into learning_chapters (chapter_id, unit_id, title, description, is_premium, is_published, order_index) values
('chapter-intro-greetings-basic', 'unit-intro-greetings', 'Lời chào và kết thúc hội thoại', 'Chỉ dùng các ký hiệu đúng chức năng giao tiếp; không thay thế bằng từ sai nghĩa.', false, true, 1),
('chapter-self-introduction-basic', 'unit-self-introduction', 'Thông tin cá nhân cơ bản', 'Các ký hiệu thành phần có thể ghép vào hội thoại giới thiệu bản thân.', false, true, 1);

insert into learning_lessons (lesson_id, chapter_id, title, description, video_url, duration_seconds, is_premium, is_published, order_index) values
('lesson-intro-greet-chao', 'chapter-intro-greetings-basic', 'Chào', 'Từ yêu cầu: Xin chào. Dataset không có exact "xin chào"; "chào" là ký hiệu chào hỏi đúng ngữ cảnh.', 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W00489.mp4', 60, false, true, 1),
('lesson-intro-greet-xin-loi', 'chapter-intro-greetings-basic', 'Xin lỗi', 'Exact match trong dataset: xin lỗi.', 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03990.mp4', 60, false, true, 2),
('lesson-intro-greet-tam-biet', 'chapter-intro-greetings-basic', 'Tạm biệt', 'Exact match trong dataset: tạm biệt.', 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03075.mp4', 60, false, true, 3),
('lesson-intro-greet-hen-gap-lai', 'chapter-intro-greetings-basic', 'Hẹn gặp lại', 'Dataset không có exact "hẹn gặp lại"; dùng "tạm biệt" vì cùng chức năng kết thúc hội thoại/farewell.', 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03075.mp4', 60, false, true, 4),
('lesson-self-ten-rieng', 'chapter-self-introduction-basic', 'Tên riêng', 'Từ yêu cầu: Tên. Dùng "tên riêng" để dạy khái niệm tên trong giới thiệu bản thân.', 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03130N.mp4', 60, false, true, 1),
('lesson-self-ten-la-gi', 'chapter-self-introduction-basic', 'Tên là gì?', 'Cụm hỏi tên đúng ngữ cảnh hội thoại làm quen.', 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03129N.mp4', 60, false, true, 2),
('lesson-self-sinh-song', 'chapter-self-introduction-basic', 'Sinh sống', 'Từ yêu cầu: Sống/Tôi sống ở. "Sinh sống" là ký hiệu đúng ngữ cảnh nói về nơi sống.', 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W02957.mp4', 60, false, true, 3),
('lesson-self-dia-chi', 'chapter-self-introduction-basic', 'Địa chỉ', 'Thành phần bổ trợ cho mẫu câu "Tôi sống ở..." khi nói về nơi ở/địa chỉ.', 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0001N.mp4', 60, false, true, 4),
('lesson-self-gap-go', 'chapter-self-introduction-basic', 'Gặp gỡ', 'Từ yêu cầu: Gặp. "Gặp gỡ" là cụm đúng nghĩa gặp/làm quen.', 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W01395.mp4', 60, false, true, 5),
('lesson-self-ban', 'chapter-self-introduction-basic', 'Bạn', 'Thành phần trong mẫu câu "Rất vui được gặp bạn".', 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W00110B.mp4', 60, false, true, 6),
('lesson-self-vui-mung', 'chapter-self-introduction-basic', 'Vui mừng', 'Thành phần cảm xúc trong mẫu câu "Rất vui được gặp bạn". Không thay thế toàn bộ câu.', 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03889B.mp4', 60, false, true, 7);

insert into practice_items (practice_item_id, lesson_id, category, level, label, expected_gloss, source_video_file, is_published, order_index, video_url) values
('practice-intro-greet-chao', 'lesson-intro-greet-chao', 'greeting', 'beginner', 'chào', 'CHAO', 'W00489.mp4', true, 201, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W00489.mp4'),
('practice-intro-greet-xin-loi', 'lesson-intro-greet-xin-loi', 'greeting', 'beginner', 'xin lỗi', 'XIN_LOI', 'W03990.mp4', true, 202, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03990.mp4'),
('practice-intro-greet-tam-biet', 'lesson-intro-greet-tam-biet', 'greeting', 'beginner', 'tạm biệt', 'TAM_BIET', 'W03075.mp4', true, 203, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03075.mp4'),
('practice-intro-greet-hen-gap-lai', 'lesson-intro-greet-hen-gap-lai', 'greeting', 'beginner', 'tạm biệt', 'TAM_BIET', 'W03075.mp4', true, 204, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03075.mp4'),
('practice-self-ten-rieng', 'lesson-self-ten-rieng', 'self_intro', 'beginner', 'tên riêng', 'TEN_RIENG', 'W03130N.mp4', true, 205, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03130N.mp4'),
('practice-self-ten-la-gi', 'lesson-self-ten-la-gi', 'self_intro', 'beginner', 'tên là gì?', 'TEN_LA_GI', 'W03129N.mp4', true, 206, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03129N.mp4'),
('practice-self-sinh-song', 'lesson-self-sinh-song', 'self_intro', 'beginner', 'sinh sống', 'SINH_SONG', 'W02957.mp4', true, 207, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W02957.mp4'),
('practice-self-dia-chi', 'lesson-self-dia-chi', 'self_intro', 'beginner', 'địa chỉ', 'DIA_CHI', 'D0001N.mp4', true, 208, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/D0001N.mp4'),
('practice-self-gap-go', 'lesson-self-gap-go', 'self_intro', 'beginner', 'gặp gỡ', 'GAP_GO', 'W01395.mp4', true, 209, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W01395.mp4'),
('practice-self-ban', 'lesson-self-ban', 'self_intro', 'beginner', 'bạn', 'BAN', 'W00110B.mp4', true, 210, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W00110B.mp4'),
('practice-self-vui-mung', 'lesson-self-vui-mung', 'self_intro', 'beginner', 'vui mừng', 'VUI_MUNG', 'W03889B.mp4', true, 211, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/W03889B.mp4');

insert into practice_item_rubrics (practice_item_id, code, order_index)
select practice_item_id, 'HAND_SHAPE_MATCH', 1
from practice_items
where practice_item_id like 'practice-intro-%' or practice_item_id like 'practice-self-%';

insert into practice_item_rubrics (practice_item_id, code, order_index)
select practice_item_id, 'PALM_ORIENTATION_MATCH', 2
from practice_items
where practice_item_id like 'practice-intro-%' or practice_item_id like 'practice-self-%';

insert into practice_item_rubrics (practice_item_id, code, order_index)
select practice_item_id, 'MOVEMENT_PATH_MATCH', 3
from practice_items
where practice_item_id like 'practice-intro-%' or practice_item_id like 'practice-self-%';

insert into lesson_quizzes (quiz_id, lesson_id, title, passing_score, xp_award, requires_premium, is_published) values
('quiz-intro-greet-chao', 'lesson-intro-greet-chao', 'Kiểm tra: Chào', 70, 10, false, true),
('quiz-intro-greet-xin-loi', 'lesson-intro-greet-xin-loi', 'Kiểm tra: Xin lỗi', 70, 10, false, true),
('quiz-intro-greet-tam-biet', 'lesson-intro-greet-tam-biet', 'Kiểm tra: Tạm biệt', 70, 10, false, true),
('quiz-intro-greet-hen-gap-lai', 'lesson-intro-greet-hen-gap-lai', 'Kiểm tra: Hẹn gặp lại', 70, 10, false, true),
('quiz-self-ten-rieng', 'lesson-self-ten-rieng', 'Kiểm tra: Tên riêng', 70, 10, false, true),
('quiz-self-ten-la-gi', 'lesson-self-ten-la-gi', 'Kiểm tra: Tên là gì?', 70, 10, false, true),
('quiz-self-sinh-song', 'lesson-self-sinh-song', 'Kiểm tra: Sinh sống', 70, 10, false, true),
('quiz-self-dia-chi', 'lesson-self-dia-chi', 'Kiểm tra: Địa chỉ', 70, 10, false, true),
('quiz-self-gap-go', 'lesson-self-gap-go', 'Kiểm tra: Gặp gỡ', 70, 10, false, true),
('quiz-self-ban', 'lesson-self-ban', 'Kiểm tra: Bạn', 70, 10, false, true),
('quiz-self-vui-mung', 'lesson-self-vui-mung', 'Kiểm tra: Vui mừng', 70, 10, false, true);

insert into quiz_questions (question_id, quiz_id, prompt, correct_answer_id, order_index) values
('quiz-q-intro-greet-chao', 'quiz-intro-greet-chao', 'Ký hiệu trong video có nghĩa là từ nào?', 'answer-chao', 1),
('quiz-q-intro-greet-xin-loi', 'quiz-intro-greet-xin-loi', 'Ký hiệu trong video có nghĩa là từ nào?', 'answer-xin-loi', 1),
('quiz-q-intro-greet-tam-biet', 'quiz-intro-greet-tam-biet', 'Ký hiệu trong video có nghĩa là từ nào?', 'answer-tam-biet', 1),
('quiz-q-intro-greet-hen-gap-lai', 'quiz-intro-greet-hen-gap-lai', 'Ký hiệu farewell nào phù hợp với "Hẹn gặp lại"?', 'answer-tam-biet', 1),
('quiz-q-self-ten-rieng', 'quiz-self-ten-rieng', 'Ký hiệu trong video có nghĩa là từ nào?', 'answer-ten-rieng', 1),
('quiz-q-self-ten-la-gi', 'quiz-self-ten-la-gi', 'Ký hiệu trong video có nghĩa là câu nào?', 'answer-ten-la-gi', 1),
('quiz-q-self-sinh-song', 'quiz-self-sinh-song', 'Ký hiệu trong video có nghĩa là từ nào?', 'answer-sinh-song', 1),
('quiz-q-self-dia-chi', 'quiz-self-dia-chi', 'Ký hiệu trong video có nghĩa là từ nào?', 'answer-dia-chi', 1),
('quiz-q-self-gap-go', 'quiz-self-gap-go', 'Ký hiệu trong video có nghĩa là từ nào?', 'answer-gap-go', 1),
('quiz-q-self-ban', 'quiz-self-ban', 'Ký hiệu trong video có nghĩa là từ nào?', 'answer-ban', 1),
('quiz-q-self-vui-mung', 'quiz-self-vui-mung', 'Ký hiệu trong video có nghĩa là từ nào?', 'answer-vui-mung', 1);

insert into quiz_options (question_id, answer_id, text, order_index) values
('quiz-q-intro-greet-chao', 'answer-chao', 'Chào', 1),
('quiz-q-intro-greet-chao', 'answer-xin-loi', 'Xin lỗi', 2),
('quiz-q-intro-greet-xin-loi', 'answer-xin-loi', 'Xin lỗi', 1),
('quiz-q-intro-greet-xin-loi', 'answer-chao', 'Chào', 2),
('quiz-q-intro-greet-tam-biet', 'answer-tam-biet', 'Tạm biệt', 1),
('quiz-q-intro-greet-tam-biet', 'answer-chao', 'Chào', 2),
('quiz-q-intro-greet-hen-gap-lai', 'answer-tam-biet', 'Tạm biệt', 1),
('quiz-q-intro-greet-hen-gap-lai', 'answer-xin-loi', 'Xin lỗi', 2),
('quiz-q-self-ten-rieng', 'answer-ten-rieng', 'Tên riêng', 1),
('quiz-q-self-ten-rieng', 'answer-gap-go', 'Gặp gỡ', 2),
('quiz-q-self-ten-la-gi', 'answer-ten-la-gi', 'Tên là gì?', 1),
('quiz-q-self-ten-la-gi', 'answer-ban', 'Bạn', 2),
('quiz-q-self-sinh-song', 'answer-sinh-song', 'Sinh sống', 1),
('quiz-q-self-sinh-song', 'answer-dia-chi', 'Địa chỉ', 2),
('quiz-q-self-dia-chi', 'answer-dia-chi', 'Địa chỉ', 1),
('quiz-q-self-dia-chi', 'answer-sinh-song', 'Sinh sống', 2),
('quiz-q-self-gap-go', 'answer-gap-go', 'Gặp gỡ', 1),
('quiz-q-self-gap-go', 'answer-vui-mung', 'Vui mừng', 2),
('quiz-q-self-ban', 'answer-ban', 'Bạn', 1),
('quiz-q-self-ban', 'answer-ten-rieng', 'Tên riêng', 2),
('quiz-q-self-vui-mung', 'answer-vui-mung', 'Vui mừng', 1),
('quiz-q-self-vui-mung', 'answer-ban', 'Bạn', 2);
