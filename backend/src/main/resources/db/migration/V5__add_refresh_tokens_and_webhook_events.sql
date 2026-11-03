CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_refresh_tokens_user_active
    ON refresh_tokens (user_id, revoked_at);
CREATE INDEX ix_refresh_tokens_family_active
    ON refresh_tokens (family_id, revoked_at);

CREATE TABLE webhook_events (
    event_key VARCHAR(64) PRIMARY KEY,
    request_id VARCHAR(128) NOT NULL,
    resource_id VARCHAR(80) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_webhook_events_request_resource UNIQUE (request_id, resource_id)
);
