-- Initialize database for production
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional schemas if needed
-- CREATE SCHEMA IF NOT EXISTS ecommerce;

-- Set default privileges
GRANT ALL PRIVILEGES ON DATABASE ecommerce_prod TO postgres;
