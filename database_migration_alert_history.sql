-- Migration: Create alert_history table for AlertHistory entity

CREATE TABLE IF NOT EXISTS alert_history (
    id BIGSERIAL PRIMARY KEY,
    alert_id BIGINT NOT NULL,
    triggered_at TIMESTAMP NOT NULL,
    metric_value VARCHAR(100),
    message VARCHAR(500),
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by BIGINT,
    acknowledged_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);