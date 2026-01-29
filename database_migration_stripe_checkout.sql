-- Migration: Add Stripe Checkout Session support
-- Date: 2025-11-05
-- Description: Add fields to support Stripe Checkout Session flow

-- Add new columns to orders table
ALTER TABLE orders 
ADD COLUMN stripe_checkout_session_id VARCHAR(255) AFTER payment_intent_id;

ALTER TABLE orders 
ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'eur' AFTER stripe_checkout_session_id;

-- Note: New OrderStatus values (CHECKOUT_CREATED, FAILED) are handled by the enum in Java
-- No database changes needed for enum values in most RDBMS when using JPA @Enumerated(EnumType.STRING)

-- Create index for faster session lookup
CREATE INDEX idx_orders_checkout_session ON orders(stripe_checkout_session_id);

-- Update existing orders to have currency if null
UPDATE orders SET currency = 'eur' WHERE currency IS NULL OR currency = '';

-- Migration complete

