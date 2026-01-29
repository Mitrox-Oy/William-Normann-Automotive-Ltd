-- Cart reservation and order inventory tracking
ALTER TABLE cart_items
    ADD COLUMN IF NOT EXISTS reserved_until TIMESTAMP;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS inventory_locked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_cart_items_reserved_until ON cart_items(reserved_until);
