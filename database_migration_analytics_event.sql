-- Migration: Create analytics_event table for AnalyticsEvent entity

CREATE TABLE IF NOT EXISTS analytics_event (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(64) NOT NULL,
    session_id VARCHAR(128),
    user_id BIGINT,
    path VARCHAR(512),
    product_id BIGINT,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);