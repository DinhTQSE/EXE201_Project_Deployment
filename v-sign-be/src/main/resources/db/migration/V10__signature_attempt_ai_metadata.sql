alter table signature_attempt_logs add column ai_status varchar(40);
alter table signature_attempt_logs add column target_gloss varchar(120);
alter table signature_attempt_logs add column predicted_gloss varchar(120);
alter table signature_attempt_logs add column confidence double precision;
alter table signature_attempt_logs add column is_correct boolean;
alter table signature_attempt_logs add column frames_processed int;
alter table signature_attempt_logs add column hands_detected_frames int;
alter table signature_attempt_logs add column inference_ms double precision;
