-- Create Demo Users for Heroku PostgreSQL
-- Run this in Heroku PostgreSQL console

-- First, let's check if the users table exists
SELECT EXISTS (
   SELECT FROM information_schema.tables 
   WHERE table_name = 'users'
);

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

-- Create demo owner user  
-- Password is 'password' hashed with BCrypt (strength 10)
INSERT INTO users (username, password, first_name, last_name, role, created_at, updated_at)
VALUES (
    'owner@example.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'Store',
    'Owner',
    'OWNER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;

-- Verify the users were created
SELECT id, username, first_name, last_name, role, created_at
FROM users
WHERE username IN ('customer@example.com', 'owner@example.com')
ORDER BY role;
