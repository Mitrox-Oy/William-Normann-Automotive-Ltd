-- Database migration script for adding quote_only column to products table
-- This script adds a new boolean column to support quote-only products
-- Safe migration that handles existing data properly

-- Step 1: Add column as nullable with default
ALTER TABLE products
ADD COLUMN IF NOT EXISTS quote_only BOOLEAN DEFAULT FALSE;

-- Step 2: Update all existing NULL values to FALSE
UPDATE products SET quote_only = FALSE WHERE quote_only IS NULL;

-- Step 3: Make the column NOT NULL now that all values are set
ALTER TABLE products ALTER COLUMN quote_only SET NOT NULL;

-- Add comment to explain the column
COMMENT ON COLUMN products.quote_only IS 'If true, product cannot be added to cart and must be purchased via quote request';

-- Optional: Set quote_only to TRUE for all products in the CARS category
-- This assumes you have a category with slug 'cars' in your database
-- Uncomment the following lines if you want to apply this default behavior:

-- UPDATE products 
-- SET quote_only = TRUE 
-- WHERE category_id IN (
--     SELECT id FROM categories WHERE slug = 'cars'
-- );

-- Display the updated products table structure
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default
FROM information_schema.columns
WHERE table_name = 'products'
ORDER BY ordinal_position;
