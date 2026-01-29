-- Migration: Create analytics_dashboards table for AnalyticsDashboard entity

CREATE TABLE IF NOT EXISTS analytics_dashboards (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    type VARCHAR(50) NOT NULL,
    config_json TEXT,
    owner_id BIGINT NOT NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);