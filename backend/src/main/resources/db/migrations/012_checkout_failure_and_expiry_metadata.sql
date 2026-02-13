-- Checkout failure/cancel/expiry metadata and lookup/index support.
-- Idempotent for PostgreSQL.

ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(32);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS failed_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS canceled_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS expired_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS inventory_released_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS failure_code VARCHAR(128);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS failure_message VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_orders_status_created_date ON orders (status, created_date);
CREATE INDEX IF NOT EXISTS idx_orders_payment_intent_id ON orders (payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_orders_checkout_session_id ON orders (stripe_checkout_session_id);
