create table signature_attempt_logs (
    attempt_id varchar(120) primary key,
    user_key varchar(255) not null,
    practice_item_id varchar(120) not null,
    user_story_id varchar(120),
    document_upload_id varchar(120),
    signature_vector_hash varchar(64) not null,
    duration_ms bigint not null,
    status varchar(40) not null,
    score int not null,
    feedback_codes text not null,
    created_at timestamp with time zone not null default now()
);
