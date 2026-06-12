create index if not exists idx_learning_units_published_order
    on learning_units (is_published, order_index);

create index if not exists idx_learning_chapters_unit_published_order
    on learning_chapters (unit_id, is_published, order_index);

create index if not exists idx_learning_lessons_chapter_published_order
    on learning_lessons (chapter_id, is_published, order_index);

create index if not exists idx_practice_items_lesson_published_order
    on practice_items (lesson_id, is_published, order_index);

create index if not exists idx_lesson_progress_user_lesson
    on lesson_progress (user_key, lesson_id);
