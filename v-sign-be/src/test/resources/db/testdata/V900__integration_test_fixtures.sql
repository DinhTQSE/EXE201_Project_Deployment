insert into dictionary_entries (
    id, word, category, difficulty, difficulty_level, description, video_url, thumbnail_url, is_published
)
values
    (1, 'Hello', 'greeting', 'CO_BAN', 1, 'Basic greeting', 'https://media.example.invalid/hello.mp4', 'https://media.example.invalid/hello.jpg', true),
    (2, 'Thank you', 'greeting', 'CO_BAN', 1, 'Polite phrase', 'https://media.example.invalid/thanks.mp4', 'https://media.example.invalid/thanks.jpg', true),
    (3, 'School', 'place', 'TRUNG_BINH', 2, 'Place sign', 'https://media.example.invalid/school.mp4', 'https://media.example.invalid/school.jpg', true),
    (4, 'Family', 'family', 'CO_BAN', 1, 'Family topic', 'https://media.example.invalid/family.mp4', 'https://media.example.invalid/family.jpg', true),
    (5, 'Market', 'place', 'TRUNG_BINH', 2, 'Market place', 'https://media.example.invalid/market.mp4', 'https://media.example.invalid/market.jpg', true),
    (6, 'Emergency', 'safety', 'NANG_CAO', 3, 'Advanced safety sign', 'https://media.example.invalid/emergency.mp4', 'https://media.example.invalid/emergency.jpg', true);

insert into users
(id, email, password_hash, full_name, avatar_url, account_type, role, is_active, current_streak, longest_streak, total_xp, created_at, updated_at, last_seen_at)
values
('00000000-0000-0000-0000-000000000101', 'learner.basic@vsign.test', null, 'Learner Basic', null, 'BASIC', 'USER', true, 0, 0, 0, '2026-05-01T08:00:00+07:00', '2026-05-01T08:00:00+07:00', '2026-05-10T08:00:00+07:00'),
('00000000-0000-0000-0000-000000000102', 'learner.premium@vsign.test', null, 'Learner Premium', null, 'PREMIUM', 'USER', true, 3, 7, 420, '2026-05-03T08:00:00+07:00', '2026-05-03T08:00:00+07:00', '2026-05-11T08:00:00+07:00'),
('00000000-0000-0000-0000-000000000901', 'admin@vsign.test', null, 'V-Sign Admin', null, 'BASIC', 'ADMIN', true, 0, 0, 0, '2026-04-20T08:00:00+07:00', '2026-04-20T08:00:00+07:00', '2026-05-12T08:00:00+07:00'),
('00000000-0000-0000-0000-000000000902', 'superadmin@vsign.test', null, 'V-Sign Super Admin', null, 'BASIC', 'SUPER_ADMIN', true, 0, 0, 0, '2026-04-19T08:00:00+07:00', '2026-04-19T08:00:00+07:00', '2026-05-12T08:00:00+07:00'),
('00000000-0000-0000-0000-000000000903', 'reviewer@vsign.test', null, 'Content Reviewer', null, 'BASIC', 'CONTENT_REVIEWER', true, 0, 0, 0, '2026-04-22T08:00:00+07:00', '2026-04-22T08:00:00+07:00', null),
('00000000-0000-0000-0000-000000000103', 'inactive@vsign.test', null, 'Inactive Learner', null, 'BASIC', 'USER', false, 0, 0, 0, '2026-04-25T08:00:00+07:00', '2026-04-25T08:00:00+07:00', null);

insert into user_usage_daily
(user_email, activity_date, active_seconds, lesson_completions, ai_attempts, quiz_attempts, last_seen_at)
values
('learner.basic@vsign.test', '2026-05-10', 900, 1, 2, 1, '2026-05-10T08:00:00+07:00'),
('learner.premium@vsign.test', '2026-05-11', 1800, 2, 3, 2, '2026-05-11T08:00:00+07:00');

insert into gamification_profiles (email, user_id, full_name, total_xp, current_streak, longest_streak) values
('learner.one@vsign.test', 'learner-001', 'Learner One', 320, 4, 7),
('learner.two@vsign.test', 'learner-002', 'Learner Two', 280, 2, 3),
('learner.three@vsign.test', 'learner-003', 'Learner Three', 150, 1, 1);

insert into gamification_badges (user_id, badge_id, name, earned_at) values
('learner-001', 'badge-first-lesson', 'Khoi Dau', '2026-05-01T00:00:00Z'),
('learner-002', 'badge-first-lesson', 'Khoi Dau', '2026-05-01T00:00:00Z'),
('learner-003', 'badge-first-lesson', 'Khoi Dau', '2026-05-01T00:00:00Z');

insert into user_subscriptions (email, plan_type, status, started_at, expires_at) values
('learner.basic@vsign.test', null, 'INACTIVE', null, null),
('learner.premium@vsign.test', 'YEARLY', 'ACTIVE', '2026-05-01T00:00:00+07:00', '2027-05-01T00:00:00+07:00');

insert into user_tier (id, uid, tier_id, start_time, end_time, is_active, created_at) values
('00000000-0000-0000-0000-000000002002', '00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000003', '2026-05-01T00:00:00+07:00', '2027-05-01T00:00:00+07:00', true, '2026-05-01T00:00:00+07:00');

insert into payment_orders
(transaction_id, provider_transaction_id, provider, plan_id, plan_type, amount, currency, status, qr_code_data, deep_link, expires_at, qr_code_url, expires_in_seconds, retryable, user_email, created_at, updated_at)
values
('txn-1001', 'MOMO-seed-1001', 'MOMO', 'pro-monthly', 'MONTHLY', 49000, 'VND', 'PENDING', 'VSIGN|MOMO|MONTHLY|txn-1001|49000', 'momo://payment/txn-1001', '2026-05-10T08:05:00+07:00', 'https://payments.example.invalid/qr/txn-1001', 300, true, 'learner.one@vsign.test', '2026-05-10T08:00:00+07:00', '2026-05-10T08:00:00+07:00'),
('txn-1002', 'ZALOPAY-seed-1002', 'ZALOPAY', 'pro-yearly', 'YEARLY', 199000, 'VND', 'PAID', 'VSIGN|ZALOPAY|YEARLY|txn-1002|199000', 'zalopay://payment/txn-1002', '2026-05-11T08:05:00+07:00', 'https://payments.example.invalid/qr/txn-1002', 300, false, 'learner.two@vsign.test', '2026-05-11T08:00:00+07:00', '2026-05-11T08:03:00+07:00');

insert into payment_order
(order_id, uid, tier_id, order_code, amount, payment_link_id, checkout_url, qr_code, status, paid_at, expired_at, description, is_active, created_at, updated_at)
values
('00000000-0000-0000-0000-000000001001', '00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000002', 1001, 49000, 'link-1001', 'https://checkout.example/1001', 'qr-1001', 'PENDING', null, '2026-05-10T08:05:00+07:00', null, true, '2026-05-10T08:00:00+07:00', '2026-05-10T08:00:00+07:00'),
('00000000-0000-0000-0000-000000001002', '00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000003', 1002, 199000, 'link-1002', 'https://checkout.example/1002', 'qr-1002', 'PAID', '2026-05-11T08:03:00+07:00', '2026-05-11T08:05:00+07:00', null, true, '2026-05-11T08:00:00+07:00', '2026-05-11T08:03:00+07:00');

insert into admin_user_accounts
(id, email, display_name, role, status, account_type, created_at)
values
('usr-1001', 'learner.one@vsign.test', 'Learner One', 'USER', 'ACTIVE', 'FREE', '2026-05-01T08:00:00+07:00'),
('usr-1002', 'learner.two@vsign.test', 'Learner Two', 'USER', 'ACTIVE', 'PREMIUM', '2026-05-03T08:00:00+07:00'),
('usr-9001', 'admin@vsign.test', 'V-Sign Admin', 'ADMIN', 'ACTIVE', 'STAFF', '2026-04-20T08:00:00+07:00'),
('usr-9002', 'reviewer@vsign.test', 'Content Reviewer', 'CONTENT_REVIEWER', 'ACTIVE', 'STAFF', '2026-04-22T08:00:00+07:00'),
('usr-1003', 'inactive@vsign.test', 'Inactive Learner', 'USER', 'SUSPENDED', 'FREE', '2026-04-25T08:00:00+07:00');

insert into admin_review_queue
(content_id, title, content_type, submitted_by, status, submitted_at, reviewed_by, reviewed_at, reason)
values
('doc-2001', 'Bai hoc chu cai A', 'LESSON', 'content@vsign.test', 'PENDING', '2026-05-12T09:00:00+07:00', null, null, null),
('doc-2002', 'Tu dien: Cam on', 'DICTIONARY_ENTRY', 'content@vsign.test', 'PENDING', '2026-05-13T09:00:00+07:00', null, null, null),
('doc-2003', 'Quiz unit 1', 'ASSESSMENT', 'content@vsign.test', 'PENDING', '2026-05-14T09:00:00+07:00', null, null, null);
