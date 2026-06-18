-- V26: Seed missing gamification profiles for existing users
INSERT INTO gamification_profiles (email, user_id, full_name, total_xp, current_streak, longest_streak, created_at, updated_at)
SELECT u.email, 'user-' || cast(u.id as varchar), u.full_name, 0, 0, 0, now(), now()
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM gamification_profiles gp WHERE gp.email = u.email
);
