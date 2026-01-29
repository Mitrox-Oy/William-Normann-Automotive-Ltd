-- Database Migration: Expand Product Info Sections from 3 to 10
-- Purpose: Add 7 additional info sections (section 4 through 10) to products table
-- Date: 2025-09-30
-- Author: System

-- Add info section 4 fields
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section4_title VARCHAR(120);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section4_content VARCHAR(2000);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section4_enabled BOOLEAN DEFAULT FALSE;

-- Add info section 5 fields
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section5_title VARCHAR(120);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section5_content VARCHAR(2000);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section5_enabled BOOLEAN DEFAULT FALSE;

-- Add info section 6 fields
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section6_title VARCHAR(120);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section6_content VARCHAR(2000);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section6_enabled BOOLEAN DEFAULT FALSE;

-- Add info section 7 fields
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section7_title VARCHAR(120);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section7_content VARCHAR(2000);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section7_enabled BOOLEAN DEFAULT FALSE;

-- Add info section 8 fields
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section8_title VARCHAR(120);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section8_content VARCHAR(2000);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section8_enabled BOOLEAN DEFAULT FALSE;

-- Add info section 9 fields
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section9_title VARCHAR(120);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section9_content VARCHAR(2000);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section9_enabled BOOLEAN DEFAULT FALSE;

-- Add info section 10 fields
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section10_title VARCHAR(120);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section10_content VARCHAR(2000);
ALTER TABLE products ADD COLUMN IF NOT EXISTS info_section10_enabled BOOLEAN DEFAULT FALSE;

-- Verify the changes
SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'products' 
  AND column_name LIKE 'info_section%'
ORDER BY column_name;

-- Success message
DO $$
BEGIN
    RAISE NOTICE 'Migration completed successfully. Product table now supports 10 info sections.';
END $$;

