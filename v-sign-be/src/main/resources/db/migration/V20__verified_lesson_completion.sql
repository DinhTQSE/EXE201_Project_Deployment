alter table quiz_attempts add column user_key varchar(255) not null default 'anonymous';

create index idx_quiz_attempts_user_lesson_passed
    on quiz_attempts(user_key, lesson_id, passed, submitted_at desc);

create index idx_signature_attempt_logs_user_practice_passed
    on signature_attempt_logs(user_key, practice_item_id, status, is_correct, created_at desc);
