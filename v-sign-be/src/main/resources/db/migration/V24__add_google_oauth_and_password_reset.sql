-- V24: Google OAuth and Password Reset Support
ALTER TABLE users ADD COLUMN IF NOT EXISTS pwd_reset_token_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS pwd_reset_expiry TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_pwd_reset_token_hash ON users (pwd_reset_token_hash);
