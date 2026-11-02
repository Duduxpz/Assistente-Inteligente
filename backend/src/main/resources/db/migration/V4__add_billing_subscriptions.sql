CREATE TABLE subscriptions (
    user_id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    provider_subscription_id VARCHAR(80) UNIQUE,
    plan_key VARCHAR(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    checkout_url VARCHAR(2048),
    current_period_end TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_subscriptions_plan CHECK (plan_key IN ('plus', 'ultra')),
    CONSTRAINT ck_subscriptions_status CHECK (status IN ('pending', 'authorized', 'cancelled', 'paused', 'expired'))
);

CREATE INDEX ix_subscriptions_status_period_end ON subscriptions (status, current_period_end);
