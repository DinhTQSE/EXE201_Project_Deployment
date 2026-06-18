CREATE TABLE IF NOT EXISTS tier_features (
    id           UUID PRIMARY KEY,
    tier_id      UUID NOT NULL REFERENCES tier(tier_id),
    feature_key  VARCHAR(80) NOT NULL,
    limit_value  INTEGER NOT NULL,
    is_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tier_feature UNIQUE(tier_id, feature_key)
);

-- Seed data cho các gói
INSERT INTO tier_features (id, tier_id, feature_key, limit_value, is_enabled) VALUES
    -- FREE
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'chapter_access', 1, TRUE),
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'standalone_ai', 0, FALSE),
    -- PLUS (Giới hạn 500 lượt)
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002', 'chapter_access', -1, TRUE),
    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000002', 'standalone_ai', 0, FALSE),
    -- PRO (Giới hạn 999 lượt)
    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000003', 'chapter_access', -1, TRUE),
    ('10000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000003', 'standalone_ai', -1, TRUE)
ON CONFLICT DO NOTHING;

-- Cập nhật giới hạn gói Plus lên 500 lượt
UPDATE tier SET limited_token = 500 WHERE title = 'plus';

