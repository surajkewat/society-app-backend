-- Schema designed for Spring Security + OTP (passwordless) + JWT refresh.
-- - users: principal = phone (or email); no password column (OTP-only auth).
--   enabled = UserDetails.isEnabled(); account_locked_until = UserDetails.isAccountNonLocked().
-- - otp_verifications: one row per OTP send; verify then set used_at.
-- - refresh_tokens: store hash for rotation/revocation (Spring does not provide this; we use custom JWT).

-- Users: phone login (phone unique), email nullable
CREATE TABLE users (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                 TEXT,
    phone                 TEXT,
    name                  TEXT NOT NULL DEFAULT '',
    enabled               BOOLEAN NOT NULL DEFAULT true,
    account_locked_until  TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_users_email ON users (email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX idx_users_phone ON users (phone) WHERE phone IS NOT NULL;

-- OTP: one row per send; verify then mark used
CREATE TABLE otp_verifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone      TEXT NOT NULL,
    code       TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_otp_phone_expires ON otp_verifications (phone, expires_at) WHERE used_at IS NULL;

-- Refresh tokens: store hash for rotation/revocation
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens (token_hash) WHERE revoked_at IS NULL;
