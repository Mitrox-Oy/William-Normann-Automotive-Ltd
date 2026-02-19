-- ============================================================================
-- Database Migration: Simplify Parts Categories to 3
-- ============================================================================
-- Simplifies the parts shop from 9 categories down to 3:
--   1. Engine & Drivetrain (kept as-is)
--   2. Wheels (renamed from Wheels & Tires)
--   3. Others (new category for miscellaneous parts)
--
-- This migration:
--   - Deletes products from 7 removed categories
--   - Creates new "Others" category
--   - Renames "Wheels & Tires" to "Wheels"
--   - Removes 7 old category records completely
--   - Updates sort order for remaining categories
-- ============================================================================

-- Step 1: Delete products from categories being removed
-- Categories to remove: suspension-steering, brakes, electrical-lighting,
--                       exterior-body, interior, cooling-hvac, maintenance-service
DELETE FROM products 
WHERE category_id IN (
  SELECT id FROM categories WHERE slug IN (
    'parts-suspension-steering',
    'parts-brakes',
    'parts-electrical-lighting',
    'parts-exterior-body',
    'parts-interior',
    'parts-cooling-hvac',
    'parts-maintenance-service'
  )
);

-- Step 2: Create new "Others" category
INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Others',
  'parts-others',
  'Miscellaneous automotive parts and accessories',
  '/images/topics/parts.jpg',
  true,
  3,
  (SELECT id FROM categories WHERE slug = 'parts'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'parts-others')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

-- Step 3: Rename "Wheels & Tires" to "Wheels"
-- Update both name and slug
UPDATE categories 
SET 
  name = 'Wheels',
  slug = 'parts-wheels',
  description = 'Wheels, tires, fitment hardware, and accessories',
  image_url = '/Wheels.jpg',
  sort_order = 2,
  updated_date = NOW()
WHERE slug = 'parts-wheels-tires';

-- Step 4: Update "Engine & Drivetrain" sort order and image
UPDATE categories 
SET 
  sort_order = 1,
  image_url = '/Engines _ Drivetrain.jpg',
  updated_date = NOW() 
WHERE slug = 'parts-engine-drivetrain';

-- Step 5: Delete the 7 removed category records completely
DELETE FROM categories 
WHERE slug IN (
  'parts-suspension-steering',
  'parts-brakes',
  'parts-electrical-lighting',
  'parts-exterior-body',
  'parts-interior',
  'parts-cooling-hvac',
  'parts-maintenance-service'
);

-- Final verification: Parts should now have exactly 3 child categories
-- Expected result:
--   1. parts-engine-drivetrain (sort_order=1)
--   2. parts-wheels (sort_order=2)
--   3. parts-others (sort_order=3)
