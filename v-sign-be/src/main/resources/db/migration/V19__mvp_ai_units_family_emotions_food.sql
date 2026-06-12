alter table quiz_options add column if not exists video_url text;

alter table dictionary_entries alter column id restart with 10000;

create temporary table tmp_mvp_ai_signs (
    slug varchar(80) primary key,
    unit_id varchar(80) not null,
    unit_title varchar(160) not null,
    unit_description text not null,
    unit_order int not null,
    chapter_id varchar(80) not null,
    chapter_title varchar(160) not null,
    chapter_description text not null,
    category varchar(60) not null,
    lesson_title varchar(160) not null,
    lesson_description text not null,
    dictionary_word varchar(120) not null,
    ai_label varchar(120) not null,
    source_video_file varchar(120) not null,
    source_region_code varchar(20) not null,
    source_region_name varchar(80) not null,
    secondary_video_file varchar(120),
    secondary_region_code varchar(20),
    secondary_region_name varchar(80),
    order_index int not null,
    distractor_slug varchar(80) not null
) on commit drop;

insert into tmp_mvp_ai_signs values
('anhhai-bt', 'unit-mvp-family', 'Người thân trong gia đình', 'Các ký hiệu người thân đã train AI, có biến thể vùng miền khi cần.', 101, 'chapter-mvp-family-core', 'Người thân cơ bản', 'Học các ký hiệu người thân trong gia đình và biến thể vùng miền.', 'family', 'Anh hai (Bắc/Trung)', 'Ký hiệu anh hai, anh cả dùng cho miền Bắc và miền Trung.', 'anh hai, anh cả', 'ANHHAI_BT', 'D0143B.mp4', 'B', 'Miền Bắc', 'D0143T.mp4', 'T', 'Miền Trung', 1, 'anhhai'),
('anhhai', 'unit-mvp-family', 'Người thân trong gia đình', 'Các ký hiệu người thân đã train AI, có biến thể vùng miền khi cần.', 101, 'chapter-mvp-family-core', 'Người thân cơ bản', 'Học các ký hiệu người thân trong gia đình và biến thể vùng miền.', 'family', 'Anh hai (Nam)', 'Ký hiệu anh hai, anh cả dùng cho miền Nam.', 'anh hai, anh cả', 'ANHHAI', 'D0143N.mp4', 'N', 'Miền Nam', null, null, null, 2, 'bo'),
('bo', 'unit-mvp-family', 'Người thân trong gia đình', 'Các ký hiệu người thân đã train AI, có biến thể vùng miền khi cần.', 101, 'chapter-mvp-family-core', 'Người thân cơ bản', 'Học các ký hiệu người thân trong gia đình và biến thể vùng miền.', 'family', 'Bố', 'Ký hiệu bố.', 'bố', 'BO', 'W00325.mp4', 'ALL', 'Toàn quốc', null, null, null, 3, 'me'),
('chi-bt', 'unit-mvp-family', 'Người thân trong gia đình', 'Các ký hiệu người thân đã train AI, có biến thể vùng miền khi cần.', 101, 'chapter-mvp-family-core', 'Người thân cơ bản', 'Học các ký hiệu người thân trong gia đình và biến thể vùng miền.', 'family', 'Chị (Bắc/Trung)', 'Ký hiệu chị dùng cho miền Bắc và miền Trung.', 'chị', 'CHI_BT', 'W00570B.mp4', 'B', 'Miền Bắc', 'W00570T.mp4', 'T', 'Miền Trung', 4, 'chi'),
('chi', 'unit-mvp-family', 'Người thân trong gia đình', 'Các ký hiệu người thân đã train AI, có biến thể vùng miền khi cần.', 101, 'chapter-mvp-family-core', 'Người thân cơ bản', 'Học các ký hiệu người thân trong gia đình và biến thể vùng miền.', 'family', 'Chị (Nam)', 'Ký hiệu chị dùng cho miền Nam.', 'chị', 'CHI', 'W00570N.mp4', 'N', 'Miền Nam', null, null, null, 5, 'emgai'),
('congai', 'unit-mvp-family', 'Người thân trong gia đình', 'Các ký hiệu người thân đã train AI, có biến thể vùng miền khi cần.', 101, 'chapter-mvp-family-core', 'Người thân cơ bản', 'Học các ký hiệu người thân trong gia đình và biến thể vùng miền.', 'family', 'Con gái', 'Ký hiệu con gái.', 'con gái', 'CONGAI', 'W00760.mp4', 'ALL', 'Toàn quốc', null, null, null, 6, 'contrai'),
('contrai', 'unit-mvp-family', 'Người thân trong gia đình', 'Các ký hiệu người thân đã train AI, có biến thể vùng miền khi cần.', 101, 'chapter-mvp-family-core', 'Người thân cơ bản', 'Học các ký hiệu người thân trong gia đình và biến thể vùng miền.', 'family', 'Con trai', 'Ký hiệu con trai.', 'con trai', 'CONTRAI', 'W04068.mp4', 'ALL', 'Toàn quốc', null, null, null, 7, 'congai'),
('emgai', 'unit-mvp-family', 'Người thân trong gia đình', 'Các ký hiệu người thân đã train AI, có biến thể vùng miền khi cần.', 101, 'chapter-mvp-family-core', 'Người thân cơ bản', 'Học các ký hiệu người thân trong gia đình và biến thể vùng miền.', 'family', 'Em gái', 'Ký hiệu em gái.', 'em gái', 'EMGAI', 'W01350.mp4', 'ALL', 'Toàn quốc', null, null, null, 8, 'emtrai'),
('emtrai', 'unit-mvp-family', 'Người thân trong gia đình', 'Các ký hiệu người thân đã train AI, có biến thể vùng miền khi cần.', 101, 'chapter-mvp-family-core', 'Người thân cơ bản', 'Học các ký hiệu người thân trong gia đình và biến thể vùng miền.', 'family', 'Em trai (Bắc)', 'Ký hiệu em trai dùng cho miền Bắc.', 'em trai', 'EMTRAI', 'W01352B.mp4', 'B', 'Miền Bắc', null, null, null, 9, 'emtrai-nt'),
('emtrai-nt', 'unit-mvp-family', 'Người thân trong gia đình', 'Các ký hiệu người thân đã train AI, có biến thể vùng miền khi cần.', 101, 'chapter-mvp-family-core', 'Người thân cơ bản', 'Học các ký hiệu người thân trong gia đình và biến thể vùng miền.', 'family', 'Em trai (Nam/Trung)', 'Ký hiệu em trai dùng cho miền Nam và miền Trung.', 'em trai', 'EMTRAI_NT', 'W01352N.mp4', 'N', 'Miền Nam', 'W01352T.mp4', 'T', 'Miền Trung', 10, 'emtrai'),
('me', 'unit-mvp-family', 'Người thân trong gia đình', 'Các ký hiệu người thân đã train AI, có biến thể vùng miền khi cần.', 101, 'chapter-mvp-family-core', 'Người thân cơ bản', 'Học các ký hiệu người thân trong gia đình và biến thể vùng miền.', 'family', 'Mẹ', 'Dataset dùng ký hiệu má với nghĩa mẹ.', 'má (giống: mẹ)', 'ME', 'W02110.mp4', 'ALL', 'Toàn quốc', null, null, null, 11, 'bo'),
('buon', 'unit-mvp-emotions', 'Cảm xúc', 'Các ký hiệu cảm xúc đã train AI cho MVP.', 102, 'chapter-mvp-emotions-core', 'Cảm xúc cơ bản', 'Học các ký hiệu cảm xúc thông dụng.', 'emotion', 'Buồn thảm', 'Ký hiệu buồn thảm.', 'buồn thảm', 'BUON', 'D0064.mp4', 'ALL', 'Toàn quốc', null, null, null, 1, 'hoangso'),
('hoangso', 'unit-mvp-emotions', 'Cảm xúc', 'Các ký hiệu cảm xúc đã train AI cho MVP.', 102, 'chapter-mvp-emotions-core', 'Cảm xúc cơ bản', 'Học các ký hiệu cảm xúc thông dụng.', 'emotion', 'Hoảng sợ', 'Ký hiệu hoảng sợ.', 'hoảng sợ', 'HOANGSO', 'W01650.mp4', 'ALL', 'Toàn quốc', null, null, null, 2, 'buon'),
('noigian', 'unit-mvp-emotions', 'Cảm xúc', 'Các ký hiệu cảm xúc đã train AI cho MVP.', 102, 'chapter-mvp-emotions-core', 'Cảm xúc cơ bản', 'Học các ký hiệu cảm xúc thông dụng.', 'emotion', 'Nổi giận', 'Ký hiệu nổi giận.', 'nổi giận', 'NOIGIAN', 'W02537.mp4', 'ALL', 'Toàn quốc', null, null, null, 3, 'thuongyeu'),
('thuongyeu-bt', 'unit-mvp-emotions', 'Cảm xúc', 'Các ký hiệu cảm xúc đã train AI cho MVP.', 102, 'chapter-mvp-emotions-core', 'Cảm xúc cơ bản', 'Học các ký hiệu cảm xúc thông dụng.', 'emotion', 'Thương yêu (Bắc/Trung)', 'Ký hiệu thương yêu dùng cho miền Bắc và miền Trung.', 'thương yêu', 'THUONGYEU_BT', 'W03373B.mp4', 'B', 'Miền Bắc', 'W03373T.mp4', 'T', 'Miền Trung', 4, 'thuongyeu'),
('thuongyeu', 'unit-mvp-emotions', 'Cảm xúc', 'Các ký hiệu cảm xúc đã train AI cho MVP.', 102, 'chapter-mvp-emotions-core', 'Cảm xúc cơ bản', 'Học các ký hiệu cảm xúc thông dụng.', 'emotion', 'Thương yêu (Nam)', 'Ký hiệu thương yêu dùng cho miền Nam.', 'thương yêu', 'THUONGYEU', 'W03373N.mp4', 'N', 'Miền Nam', null, null, null, 5, 'noigian'),
('vuive-bt', 'unit-mvp-emotions', 'Cảm xúc', 'Các ký hiệu cảm xúc đã train AI cho MVP.', 102, 'chapter-mvp-emotions-core', 'Cảm xúc cơ bản', 'Học các ký hiệu cảm xúc thông dụng.', 'emotion', 'Vui sướng (Bắc/Trung)', 'Ký hiệu vui sướng dùng cho miền Bắc và miền Trung.', 'vui sướng', 'VUIVE_BT', 'W03890B.mp4', 'B', 'Miền Bắc', 'W03890T.mp4', 'T', 'Miền Trung', 6, 'vuive'),
('vuive', 'unit-mvp-emotions', 'Cảm xúc', 'Các ký hiệu cảm xúc đã train AI cho MVP.', 102, 'chapter-mvp-emotions-core', 'Cảm xúc cơ bản', 'Học các ký hiệu cảm xúc thông dụng.', 'emotion', 'Vui sướng (Nam)', 'Ký hiệu vui sướng dùng cho miền Nam.', 'vui sướng', 'VUIVE', 'W03890N.mp4', 'N', 'Miền Nam', null, null, null, 7, 'buon'),
('banhmi-nt', 'unit-mvp-food', 'Món ăn thường ngày', 'Các ký hiệu món ăn thường ngày đã train AI cho MVP.', 103, 'chapter-mvp-food-core', 'Món ăn hằng ngày', 'Học các ký hiệu món ăn thường gặp và biến thể vùng miền.', 'food', 'Bánh mì (Nam/Trung)', 'Ký hiệu bánh mì dùng cho miền Nam và miền Trung.', 'bánh mì', 'BANHMI_NT', 'W00137N.mp4', 'N', 'Miền Nam', 'W00137T.mp4', 'T', 'Miền Trung', 1, 'banhmi'),
('banhmi', 'unit-mvp-food', 'Món ăn thường ngày', 'Các ký hiệu món ăn thường ngày đã train AI cho MVP.', 103, 'chapter-mvp-food-core', 'Món ăn hằng ngày', 'Học các ký hiệu món ăn thường gặp và biến thể vùng miền.', 'food', 'Bánh mì (Bắc)', 'Ký hiệu bánh mì dùng cho miền Bắc.', 'bánh mì', 'BANHMI', 'W00137B.mp4', 'B', 'Miền Bắc', null, null, null, 2, 'pho'),
('buncha', 'unit-mvp-food', 'Món ăn thường ngày', 'Các ký hiệu món ăn thường ngày đã train AI cho MVP.', 103, 'chapter-mvp-food-core', 'Món ăn hằng ngày', 'Học các ký hiệu món ăn thường gặp và biến thể vùng miền.', 'food', 'Bún chả', 'Ký hiệu bún chả.', 'bún chả', 'BUNCHA', 'D0084.mp4', 'ALL', 'Toàn quốc', null, null, null, 3, 'bundau'),
('bundau', 'unit-mvp-food', 'Món ăn thường ngày', 'Các ký hiệu món ăn thường ngày đã train AI cho MVP.', 103, 'chapter-mvp-food-core', 'Món ăn hằng ngày', 'Học các ký hiệu món ăn thường gặp và biến thể vùng miền.', 'food', 'Bún đậu', 'Ký hiệu bún đậu.', 'bún đậu', 'BUNDAU', 'D0087B.mp4', 'B', 'Miền Bắc', null, null, null, 4, 'buncha'),
('bunmam', 'unit-mvp-food', 'Món ăn thường ngày', 'Các ký hiệu món ăn thường ngày đã train AI cho MVP.', 103, 'chapter-mvp-food-core', 'Món ăn hằng ngày', 'Học các ký hiệu món ăn thường gặp và biến thể vùng miền.', 'food', 'Bún mắm', 'Ký hiệu bún mắm.', 'bún mắm', 'BUNMAM', 'D0083.mp4', 'ALL', 'Toàn quốc', null, null, null, 5, 'bunngang'),
('bunngang', 'unit-mvp-food', 'Món ăn thường ngày', 'Các ký hiệu món ăn thường ngày đã train AI cho MVP.', 103, 'chapter-mvp-food-core', 'Món ăn hằng ngày', 'Học các ký hiệu món ăn thường gặp và biến thể vùng miền.', 'food', 'Bún ngan', 'Ký hiệu bún ngan.', 'bún ngan', 'BUNNGANG', 'D0085.mp4', 'ALL', 'Toàn quốc', null, null, null, 6, 'bunmam'),
('bunoc', 'unit-mvp-food', 'Món ăn thường ngày', 'Các ký hiệu món ăn thường ngày đã train AI cho MVP.', 103, 'chapter-mvp-food-core', 'Món ăn hằng ngày', 'Học các ký hiệu món ăn thường gặp và biến thể vùng miền.', 'food', 'Bún ốc', 'Ký hiệu bún ốc.', 'bún ốc', 'BUNOC', 'D0086.mp4', 'ALL', 'Toàn quốc', null, null, null, 7, 'chao'),
('chao', 'unit-mvp-food', 'Món ăn thường ngày', 'Các ký hiệu món ăn thường ngày đã train AI cho MVP.', 103, 'chapter-mvp-food-core', 'Món ăn hằng ngày', 'Học các ký hiệu món ăn thường gặp và biến thể vùng miền.', 'food', 'Cháo', 'Dataset dùng video cháo sườn cho ký hiệu cháo.', 'cháo sườn', 'CHAO', 'D0090.mp4', 'ALL', 'Toàn quốc', null, null, null, 8, 'com'),
('com', 'unit-mvp-food', 'Món ăn thường ngày', 'Các ký hiệu món ăn thường ngày đã train AI cho MVP.', 103, 'chapter-mvp-food-core', 'Món ăn hằng ngày', 'Học các ký hiệu món ăn thường gặp và biến thể vùng miền.', 'food', 'Cơm', 'Ký hiệu cơm.', 'cơm', 'COM', 'W00884.mp4', 'ALL', 'Toàn quốc', null, null, null, 9, 'chao'),
('pho-nt', 'unit-mvp-food', 'Món ăn thường ngày', 'Các ký hiệu món ăn thường ngày đã train AI cho MVP.', 103, 'chapter-mvp-food-core', 'Món ăn hằng ngày', 'Học các ký hiệu món ăn thường gặp và biến thể vùng miền.', 'food', 'Phở (Nam/Trung)', 'Ký hiệu phở dùng cho miền Nam và miền Trung.', 'phở', 'PHO_NT', 'W02696N.mp4', 'N', 'Miền Nam', 'W02696T.mp4', 'T', 'Miền Trung', 10, 'pho'),
('pho', 'unit-mvp-food', 'Món ăn thường ngày', 'Các ký hiệu món ăn thường ngày đã train AI cho MVP.', 103, 'chapter-mvp-food-core', 'Món ăn hằng ngày', 'Học các ký hiệu món ăn thường gặp và biến thể vùng miền.', 'food', 'Phở (Bắc)', 'Ký hiệu phở dùng cho miền Bắc.', 'phở', 'PHO', 'W02696B.mp4', 'B', 'Miền Bắc', null, null, null, 11, 'banhmi');

create temporary table tmp_mvp_ai_variants as
select dictionary_word, source_video_file, source_region_code as region_code, source_region_name as region_name, true as is_default
from tmp_mvp_ai_signs
union all
select dictionary_word, secondary_video_file, secondary_region_code, secondary_region_name, false
from tmp_mvp_ai_signs
where secondary_video_file is not null;

insert into dictionary_entries (word, category, difficulty, difficulty_level, description, video_url, thumbnail_url, is_published)
select
    dictionary_word,
    category,
    'CO_BAN',
    1,
    'Video minh họa ký hiệu: ' || dictionary_word,
    'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/' || source_video_file,
    null,
    true
from tmp_mvp_ai_signs s
where not exists (
    select 1 from dictionary_entries d where lower(d.word) = lower(s.dictionary_word)
)
and s.order_index = (
    select min(s2.order_index)
    from tmp_mvp_ai_signs s2
    where lower(s2.dictionary_word) = lower(s.dictionary_word)
);

update dictionary_entries d
set video_url = coalesce(d.video_url, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/' || s.source_video_file),
    category = case when d.category in ('general', 'greeting', 'place', 'safety') then d.category else s.category end,
    updated_at = now()
from (
    select s.dictionary_word, s.source_video_file, s.category
    from tmp_mvp_ai_signs s
    where s.order_index = (
        select min(s2.order_index)
        from tmp_mvp_ai_signs s2
        where lower(s2.dictionary_word) = lower(s.dictionary_word)
    )
) s
where lower(d.word) = lower(s.dictionary_word);

create temporary table tmp_mvp_ai_resolved_variants as
select d.id as dictionary_entry_id,
       v.source_video_file,
       v.region_code,
       v.region_name,
       'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/' || v.source_video_file as video_url,
       v.is_default
from tmp_mvp_ai_variants v
join dictionary_entries d on d.id = (
    select min(match.id)
    from dictionary_entries match
    where lower(match.word) = lower(v.dictionary_word)
);

delete from dictionary_entry_video_variants existing
where exists (
    select 1
    from tmp_mvp_ai_resolved_variants resolved
    where existing.dictionary_entry_id = resolved.dictionary_entry_id
      and existing.source_video_file = resolved.source_video_file
);

insert into dictionary_entry_video_variants (dictionary_entry_id, source_video_file, region_code, region_name, video_url, is_default)
select resolved.dictionary_entry_id,
       resolved.source_video_file,
       resolved.region_code,
       resolved.region_name,
       resolved.video_url,
       resolved.is_default
from tmp_mvp_ai_resolved_variants resolved
where not exists (
    select 1 from dictionary_entry_video_variants existing
    where existing.dictionary_entry_id = resolved.dictionary_entry_id
      and existing.source_video_file = resolved.source_video_file
);

delete from quiz_options where question_id like 'quiz-q-mvp-%';
delete from quiz_questions where question_id like 'quiz-q-mvp-%';
delete from lesson_quizzes where quiz_id like 'quiz-mvp-%';
delete from practice_item_rubrics where practice_item_id like 'practice-mvp-%';
delete from practice_items where practice_item_id like 'practice-mvp-%';
delete from learning_lessons where lesson_id like 'lesson-mvp-%';
delete from learning_chapters where chapter_id in (select distinct chapter_id from tmp_mvp_ai_signs);
delete from learning_units where unit_id in (select distinct unit_id from tmp_mvp_ai_signs);

insert into learning_units (unit_id, title, description, thumbnail_url, is_published, order_index)
select distinct unit_id, unit_title, unit_description, null, true, unit_order
from tmp_mvp_ai_signs;

insert into learning_chapters (chapter_id, unit_id, title, description, is_premium, is_published, order_index)
select distinct chapter_id, unit_id, chapter_title, chapter_description, false, true, 1
from tmp_mvp_ai_signs;

insert into learning_lessons (lesson_id, chapter_id, title, description, video_url, duration_seconds, is_premium, is_published, order_index)
select
    'lesson-mvp-' || slug,
    chapter_id,
    lesson_title,
    lesson_description,
    coalesce(v.video_url, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/' || s.source_video_file),
    60,
    false,
    true,
    order_index
from tmp_mvp_ai_signs s
left join dictionary_entry_video_variants v on v.source_video_file = s.source_video_file;

insert into practice_items (practice_item_id, lesson_id, category, level, label, expected_gloss, source_video_file, is_published, order_index, video_url)
select
    'practice-mvp-' || s.slug,
    'lesson-mvp-' || s.slug,
    s.category,
    'beginner',
    s.lesson_title,
    s.ai_label,
    s.source_video_file,
    true,
    300 + s.order_index,
    coalesce(v.video_url, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/' || s.source_video_file)
from tmp_mvp_ai_signs s
left join dictionary_entry_video_variants v on v.source_video_file = s.source_video_file;

insert into practice_item_rubrics (practice_item_id, code, order_index)
select 'practice-mvp-' || slug, rubric.code, rubric.order_index
from tmp_mvp_ai_signs s
cross join (values
    ('HAND_SHAPE_MATCH', 1),
    ('PALM_ORIENTATION_MATCH', 2),
    ('MOVEMENT_PATH_MATCH', 3)
) as rubric(code, order_index)
where not exists (
    select 1 from practice_item_rubrics r
    where r.practice_item_id = 'practice-mvp-' || s.slug
      and r.code = rubric.code
);

insert into lesson_quizzes (quiz_id, lesson_id, title, passing_score, xp_award, requires_premium, is_published)
select 'quiz-mvp-' || slug, 'lesson-mvp-' || slug, 'Kiểm tra: ' || lesson_title, 70, 10, false, true
from tmp_mvp_ai_signs;

insert into quiz_questions (question_id, quiz_id, prompt, correct_answer_id, order_index)
select 'quiz-q-mvp-' || slug || '-word',
       'quiz-mvp-' || slug,
       'Xem video mẫu và chọn từ đúng.',
       'answer-mvp-' || slug || '-word-correct',
       1
from tmp_mvp_ai_signs;

insert into quiz_questions (question_id, quiz_id, prompt, correct_answer_id, order_index)
select 'quiz-q-mvp-' || slug || '-video',
       'quiz-mvp-' || slug,
       'Chọn video đúng cho: ' || lesson_title,
       'answer-mvp-' || slug || '-video-correct',
       2
from tmp_mvp_ai_signs;

insert into quiz_options (question_id, answer_id, text, video_url, order_index)
select 'quiz-q-mvp-' || s.slug || '-word',
       'answer-mvp-' || s.slug || '-word-correct',
       s.lesson_title,
       null,
       1
from tmp_mvp_ai_signs s
where not exists (
    select 1 from quiz_options o
    where o.question_id = 'quiz-q-mvp-' || s.slug || '-word'
      and o.answer_id = 'answer-mvp-' || s.slug || '-word-correct'
);

insert into quiz_options (question_id, answer_id, text, video_url, order_index)
select 'quiz-q-mvp-' || s.slug || '-word',
       'answer-mvp-' || s.slug || '-word-distractor',
       d.lesson_title,
       null,
       2
from tmp_mvp_ai_signs s
join tmp_mvp_ai_signs d on d.slug = s.distractor_slug
where not exists (
    select 1 from quiz_options o
    where o.question_id = 'quiz-q-mvp-' || s.slug || '-word'
      and o.answer_id = 'answer-mvp-' || s.slug || '-word-distractor'
);

insert into quiz_options (question_id, answer_id, text, video_url, order_index)
select 'quiz-q-mvp-' || s.slug || '-video',
       'answer-mvp-' || s.slug || '-video-correct',
       'Video A',
       coalesce(v.video_url, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/' || s.source_video_file),
       1
from tmp_mvp_ai_signs s
left join dictionary_entry_video_variants v on v.source_video_file = s.source_video_file
where not exists (
    select 1 from quiz_options o
    where o.question_id = 'quiz-q-mvp-' || s.slug || '-video'
      and o.answer_id = 'answer-mvp-' || s.slug || '-video-correct'
);

insert into quiz_options (question_id, answer_id, text, video_url, order_index)
select 'quiz-q-mvp-' || s.slug || '-video',
       'answer-mvp-' || s.slug || '-video-distractor',
       'Video B',
       coalesce(v.video_url, 'https://pub-aaf79542519744cfaf424549ccf4588c.r2.dev/videos/' || d.source_video_file),
       2
from tmp_mvp_ai_signs s
join tmp_mvp_ai_signs d on d.slug = s.distractor_slug
left join dictionary_entry_video_variants v on v.source_video_file = d.source_video_file
where not exists (
    select 1 from quiz_options o
    where o.question_id = 'quiz-q-mvp-' || s.slug || '-video'
      and o.answer_id = 'answer-mvp-' || s.slug || '-video-distractor'
);
