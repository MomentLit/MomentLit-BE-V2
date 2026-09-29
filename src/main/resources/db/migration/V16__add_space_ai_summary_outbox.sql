ALTER TABLE spaces.spaces
    ADD COLUMN IF NOT EXISTS ai_summary_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUESTED',
    ADD COLUMN IF NOT EXISTS ai_summary_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS spaces.space_ai_summary_outbox (
    id VARCHAR(36) PRIMARY KEY,
    space_id BIGINT NOT NULL,
    summary_version BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(6) NOT NULL,
    locked_at TIMESTAMP(6),
    last_error TEXT,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_space_ai_summary_outbox_claim
    ON spaces.space_ai_summary_outbox (status, next_attempt_at, created_at);
