-- Lead intake + WhatsApp delivery tracking
CREATE TABLE IF NOT EXISTS lead_requests (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    phone VARCHAR(60),
    interest VARCHAR(120),
    message VARCHAR(4000) NOT NULL,
    consent BOOLEAN NOT NULL DEFAULT FALSE,
    source VARCHAR(80) NOT NULL,
    website VARCHAR(255),
    product_name VARCHAR(200),
    part_number VARCHAR(120),
    requested_quantity VARCHAR(30),
    whatsapp_recipient VARCHAR(30),
    whatsapp_message_id VARCHAR(160),
    whatsapp_status VARCHAR(30) NOT NULL,
    whatsapp_error VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lead_requests_created_at ON lead_requests(created_at);
CREATE INDEX IF NOT EXISTS idx_lead_requests_source ON lead_requests(source);
CREATE INDEX IF NOT EXISTS idx_lead_requests_whatsapp_status ON lead_requests(whatsapp_status);