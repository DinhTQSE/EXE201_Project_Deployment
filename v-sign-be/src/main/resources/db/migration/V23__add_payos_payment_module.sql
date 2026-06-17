-- V22: PayOS tier subscription tables

CREATE TABLE IF NOT EXISTS tier (
    tier_id       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(50) NOT NULL,
    amount        INT         NOT NULL,
    no_month      INT         NOT NULL,
    limited_token INT         NOT NULL,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP,
    deleted_at    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_tier (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    uid        UUID      NOT NULL REFERENCES users(id),
    tier_id    UUID      NOT NULL REFERENCES tier(tier_id),
    start_time TIMESTAMP NOT NULL,
    end_time   TIMESTAMP NOT NULL,
    is_active  BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment_order (
    order_id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    uid             UUID        NOT NULL REFERENCES users(id),
    tier_id         UUID        NOT NULL REFERENCES tier(tier_id),
    order_code      BIGINT      NOT NULL UNIQUE,
    amount          INT         NOT NULL,
    payment_link_id VARCHAR(255),
    checkout_url    VARCHAR(500),
    qr_code         TEXT,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    paid_at         TIMESTAMP,
    expired_at      TIMESTAMP,
    description     TEXT,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    deleted_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment_transaction (
    transaction_id   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id         UUID        NOT NULL REFERENCES payment_order(order_id),
    amount           INT         NOT NULL,
    payment_link_id  VARCHAR(255),
    payload          TEXT,
    status           VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    transaction_date TIMESTAMP,
    reference        VARCHAR(255),
    is_active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP,
    deleted_at       TIMESTAMP
);

INSERT INTO tier (tier_id, title, amount, no_month, limited_token, is_active, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'free', 0,     1, 20,  TRUE, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000002', 'plus', 49000, 1, 100, TRUE, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000003', 'pro',  99000, 1, 999, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_tier_is_active       ON tier                (is_active, deleted_at);
CREATE INDEX IF NOT EXISTS idx_user_tier_uid        ON user_tier           (uid, is_active);
CREATE INDEX IF NOT EXISTS idx_user_tier_tier_id    ON user_tier           (tier_id);
CREATE INDEX IF NOT EXISTS idx_payment_order_uid    ON payment_order       (uid, created_at);
CREATE INDEX IF NOT EXISTS idx_payment_order_code   ON payment_order       (order_code);
CREATE INDEX IF NOT EXISTS idx_payment_order_status ON payment_order       (status);
CREATE INDEX IF NOT EXISTS idx_payment_tx_order     ON payment_transaction (order_id, created_at);
CREATE INDEX IF NOT EXISTS idx_payment_tx_reference ON payment_transaction (reference);
