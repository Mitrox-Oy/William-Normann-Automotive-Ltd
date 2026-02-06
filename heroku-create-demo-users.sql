-- Create Demo Customer for PostgreSQL
-- Run this in your PostgreSQL console

-- Create demo customer user
-- Password is 'password' hashed with BCrypt (strength 10)
INSERT INTO users (username, password, first_name, last_name, role, created_at, updated_at)
VALUES (
    'customer@example.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'Demo',
    'Customer',
    'CUSTOMER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;

-- Verify
SELECT id, username, first_name, last_name, role, created_at
FROM users
WHERE username IN ('customer@example.com')
ORDER BY role;

-- Note: Privileged OWNER/ADMIN accounts are managed separately.
-- Use: node scripts/sync-privileged-users.js (requires DATABASE_URL + WNA_* env vars)
