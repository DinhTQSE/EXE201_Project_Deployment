-- Deterministic test accounts for local/Supabase QA.
-- Shared password for all active accounts: Vsign@123
-- Uses WHERE NOT EXISTS instead of ON CONFLICT for H2/PostgreSQL compatibility.

insert into reference_roles (code, description)
select 'CONTENT_REVIEWER', 'Content moderation and review staff'
where not exists (
    select 1 from reference_roles where code = 'CONTENT_REVIEWER'
);

insert into users (id, email, password_hash, full_name, avatar_url, account_type, role, is_active, current_streak, longest_streak, total_xp, created_at, updated_at)
select '11111111-1111-1111-1111-111111111111', 'learner.basic@vsign.test', '$2a$10$tf4Vm/Lj0AeVP0XcZm075.e6bX3AivXgqXQW15BWFEBtuPM5DSIm6', 'Basic Learner', null, 'BASIC', 'USER', true, 1, 3, 60, now(), now()
where not exists (select 1 from users where email = 'learner.basic@vsign.test');

insert into users (id, email, password_hash, full_name, avatar_url, account_type, role, is_active, current_streak, longest_streak, total_xp, created_at, updated_at)
select '22222222-2222-2222-2222-222222222222', 'learner.premium@vsign.test', '$2a$10$tf4Vm/Lj0AeVP0XcZm075.e6bX3AivXgqXQW15BWFEBtuPM5DSIm6', 'Premium Learner', null, 'PREMIUM', 'USER', true, 7, 14, 720, now(), now()
where not exists (select 1 from users where email = 'learner.premium@vsign.test');

insert into users (id, email, password_hash, full_name, avatar_url, account_type, role, is_active, current_streak, longest_streak, total_xp, created_at, updated_at)
select '33333333-3333-3333-3333-333333333333', 'admin@vsign.test', '$2a$10$tf4Vm/Lj0AeVP0XcZm075.e6bX3AivXgqXQW15BWFEBtuPM5DSIm6', 'V-Sign Admin', null, 'STAFF', 'ADMIN', true, 0, 0, 0, now(), now()
where not exists (select 1 from users where email = 'admin@vsign.test');

insert into users (id, email, password_hash, full_name, avatar_url, account_type, role, is_active, current_streak, longest_streak, total_xp, created_at, updated_at)
select '44444444-4444-4444-4444-444444444444', 'superadmin@vsign.test', '$2a$10$tf4Vm/Lj0AeVP0XcZm075.e6bX3AivXgqXQW15BWFEBtuPM5DSIm6', 'V-Sign Super Admin', null, 'STAFF', 'SUPER_ADMIN', true, 0, 0, 0, now(), now()
where not exists (select 1 from users where email = 'superadmin@vsign.test');

insert into users (id, email, password_hash, full_name, avatar_url, account_type, role, is_active, current_streak, longest_streak, total_xp, created_at, updated_at)
select '55555555-5555-5555-5555-555555555555', 'reviewer@vsign.test', '$2a$10$tf4Vm/Lj0AeVP0XcZm075.e6bX3AivXgqXQW15BWFEBtuPM5DSIm6', 'Content Reviewer', null, 'STAFF', 'CONTENT_REVIEWER', true, 0, 0, 0, now(), now()
where not exists (select 1 from users where email = 'reviewer@vsign.test');

insert into users (id, email, password_hash, full_name, avatar_url, account_type, role, is_active, current_streak, longest_streak, total_xp, created_at, updated_at)
select '66666666-6666-6666-6666-666666666666', 'inactive@vsign.test', '$2a$10$tf4Vm/Lj0AeVP0XcZm075.e6bX3AivXgqXQW15BWFEBtuPM5DSIm6', 'Suspended Learner', null, 'BASIC', 'USER', false, 0, 0, 0, now(), now()
where not exists (select 1 from users where email = 'inactive@vsign.test');

insert into user_subscriptions (email, plan_type, status, started_at, expires_at)
select 'learner.basic@vsign.test', null, 'INACTIVE', null, null
where not exists (select 1 from user_subscriptions where email = 'learner.basic@vsign.test');

insert into user_subscriptions (email, plan_type, status, started_at, expires_at)
select 'learner.premium@vsign.test', 'YEARLY', 'ACTIVE', '2026-05-01T00:00:00+07:00', '2027-05-01T00:00:00+07:00'
where not exists (select 1 from user_subscriptions where email = 'learner.premium@vsign.test');

insert into gamification_profiles (email, user_id, full_name, total_xp, current_streak, longest_streak, created_at, updated_at)
select 'learner.basic@vsign.test', 'test-basic-learner', 'Basic Learner', 60, 1, 3, now(), now()
where not exists (select 1 from gamification_profiles where email = 'learner.basic@vsign.test');

insert into gamification_profiles (email, user_id, full_name, total_xp, current_streak, longest_streak, created_at, updated_at)
select 'learner.premium@vsign.test', 'test-premium-learner', 'Premium Learner', 720, 7, 14, now(), now()
where not exists (select 1 from gamification_profiles where email = 'learner.premium@vsign.test');

insert into admin_user_accounts (id, email, display_name, role, status, account_type, created_at)
select 'usr-test-basic', 'learner.basic@vsign.test', 'Basic Learner', 'USER', 'ACTIVE', 'FREE', '2026-05-26T00:00:00+07:00'
where not exists (select 1 from admin_user_accounts where email = 'learner.basic@vsign.test');

insert into admin_user_accounts (id, email, display_name, role, status, account_type, created_at)
select 'usr-test-premium', 'learner.premium@vsign.test', 'Premium Learner', 'USER', 'ACTIVE', 'PREMIUM', '2026-05-26T00:00:00+07:00'
where not exists (select 1 from admin_user_accounts where email = 'learner.premium@vsign.test');

insert into admin_user_accounts (id, email, display_name, role, status, account_type, created_at)
select 'usr-test-superadmin', 'superadmin@vsign.test', 'V-Sign Super Admin', 'SUPER_ADMIN', 'ACTIVE', 'STAFF', '2026-05-26T00:00:00+07:00'
where not exists (select 1 from admin_user_accounts where email = 'superadmin@vsign.test');
