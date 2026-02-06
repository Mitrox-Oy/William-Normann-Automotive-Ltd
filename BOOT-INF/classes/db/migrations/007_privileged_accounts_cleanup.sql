-- Privileged accounts cleanup / guardrails.
-- Note: passwords are NOT set here (do that via scripts/env-driven bootstrap).
--
-- IMPORTANT:
-- These migrations are executed via Spring's ScriptUtils, which splits statements on ';'
-- and does NOT reliably handle PostgreSQL `DO $$ ... $$` blocks. Keep this file to plain SQL.

-- 1) Remove legacy demo owner account fully (if it exists).
-- Delete dependent rows first to avoid FK violations. All statements are safe no-ops if the user doesn't exist.
DELETE FROM wishlist
WHERE user_id IN (SELECT id FROM users WHERE username = 'owner@example.com');

DELETE FROM shipping_addresses
WHERE user_id IN (SELECT id FROM users WHERE username = 'owner@example.com');

DELETE FROM cart_items
WHERE cart_id IN (
  SELECT id FROM carts
  WHERE user_id IN (SELECT id FROM users WHERE username = 'owner@example.com')
);

DELETE FROM carts
WHERE user_id IN (SELECT id FROM users WHERE username = 'owner@example.com');

DELETE FROM order_items
WHERE order_id IN (
  SELECT id FROM orders
  WHERE user_id IN (SELECT id FROM users WHERE username = 'owner@example.com')
);

DELETE FROM orders
WHERE user_id IN (SELECT id FROM users WHERE username = 'owner@example.com');

DELETE FROM users
WHERE username = 'owner@example.com';

-- Also remove the whitelist entry for the legacy demo owner account (if present).
DELETE FROM owner_whitelist
WHERE email = 'owner@example.com';

-- 2) Ensure the intended privileged emails are whitelisted for /api/auth/owner/login.
INSERT INTO owner_whitelist (email, is_active, created_at, updated_at, created_by, notes)
VALUES
  ('johannes.hurmerinta@mitrox.io', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'migration', 'Privileged admin account'),
  ('blackwilliamstar@gmail.com',   true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'migration', 'Privileged owner account'),
  ('salokorpi.antti@gmail.com',    true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'migration', 'Privileged owner account')
ON CONFLICT (email) DO UPDATE
  SET is_active = true,
      updated_at = CURRENT_TIMESTAMP;

-- 3) If those users exist, enforce their roles (creation happens elsewhere).
UPDATE users SET role = 'ADMIN' WHERE username = 'johannes.hurmerinta@mitrox.io';
UPDATE users SET role = 'OWNER' WHERE username = 'blackwilliamstar@gmail.com';
UPDATE users SET role = 'OWNER' WHERE username = 'salokorpi.antti@gmail.com';
