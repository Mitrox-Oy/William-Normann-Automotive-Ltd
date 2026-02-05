-- ============================================================================
-- Database Migration: Shop Topics (Root Categories)
-- ============================================================================
-- This migration creates the four root categories for the shop topics:
-- CARS, PARTS, TOOLS, CUSTOM
-- All existing categories will be moved under the PARTS root category.
-- ============================================================================

-- Step 1: Create the four root topic categories
-- These have no parent_id (NULL = root category)

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'Cars', 'cars', 'Browse our selection of cars and vehicles', '/images/topics/cars.jpg', true, 1, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'Parts', 'parts', 'OEM and aftermarket automotive parts', '/images/topics/parts.jpg', true, 2, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'Tools', 'tools', 'Professional automotive tools and equipment', '/images/topics/tools.jpg', true, 3, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'tools');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'Custom', 'custom', 'Custom parts and modifications', '/images/topics/custom.jpg', true, 4, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'custom');

-- Step 2: Move all existing root categories (except the new topic categories) under PARTS
-- This preserves existing products under their categories while organizing them under the Parts topic

-- First, get the ID of the Parts category
-- Then update all root categories that aren't one of the four topic categories

UPDATE categories 
SET parent_id = (SELECT id FROM categories WHERE slug = 'parts')
WHERE parent_id IS NULL 
  AND slug NOT IN ('cars', 'parts', 'tools', 'custom')
  AND active = true;

-- Step 3: Create some example subcategories for each topic (optional)
-- You can customize these based on your needs

-- Example subcategories for CARS topic
INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'BMW', 'cars-bmw', 'BMW vehicles', '/images/brands/bmw.jpg', true, 1, 
       (SELECT id FROM categories WHERE slug = 'cars'), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-bmw');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'Mercedes-Benz', 'cars-mercedes', 'Mercedes-Benz vehicles', '/images/brands/mercedes.jpg', true, 2, 
       (SELECT id FROM categories WHERE slug = 'cars'), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-mercedes');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'Audi', 'cars-audi', 'Audi vehicles', '/images/brands/audi.jpg', true, 3, 
       (SELECT id FROM categories WHERE slug = 'cars'), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-audi');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'Porsche', 'cars-porsche', 'Porsche vehicles', '/images/brands/porsche.jpg', true, 4, 
       (SELECT id FROM categories WHERE slug = 'cars'), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-porsche');

-- Example subcategories for TOOLS topic
INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'Hand Tools', 'tools-hand', 'Hand tools and wrenches', '/images/tools/hand-tools.jpg', true, 1, 
       (SELECT id FROM categories WHERE slug = 'tools'), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'tools-hand');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'Power Tools', 'tools-power', 'Electric and pneumatic tools', '/images/tools/power-tools.jpg', true, 2, 
       (SELECT id FROM categories WHERE slug = 'tools'), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'tools-power');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'Diagnostic Equipment', 'tools-diagnostic', 'OBD scanners and diagnostic tools', '/images/tools/diagnostic.jpg', true, 3, 
       (SELECT id FROM categories WHERE slug = 'tools'), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'tools-diagnostic');

-- Example subcategories for CUSTOM topic
INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'Performance Parts', 'custom-performance', 'Performance upgrades and modifications', '/images/custom/performance.jpg', true, 1, 
       (SELECT id FROM categories WHERE slug = 'custom'), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'custom-performance');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'Exterior Styling', 'custom-exterior', 'Body kits and exterior modifications', '/images/custom/exterior.jpg', true, 2, 
       (SELECT id FROM categories WHERE slug = 'custom'), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'custom-exterior');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT 'Interior Upgrades', 'custom-interior', 'Interior modifications and upgrades', '/images/custom/interior.jpg', true, 3, 
       (SELECT id FROM categories WHERE slug = 'custom'), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'custom-interior');

-- ============================================================================
-- Verification queries (run these to verify the migration worked)
-- ============================================================================

-- Check root categories (should show cars, parts, tools, custom)
-- SELECT id, name, slug, parent_id FROM categories WHERE parent_id IS NULL AND active = true ORDER BY sort_order;

-- Check children of parts category
-- SELECT id, name, slug, parent_id FROM categories WHERE parent_id = (SELECT id FROM categories WHERE slug = 'parts') AND active = true ORDER BY sort_order;

-- Check children of cars category
-- SELECT id, name, slug, parent_id FROM categories WHERE parent_id = (SELECT id FROM categories WHERE slug = 'cars') AND active = true ORDER BY sort_order;
