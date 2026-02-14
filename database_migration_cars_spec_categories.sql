-- ============================================================================
-- Database Migration: Cars Euro-spec + Us-Spec main categories
-- ============================================================================
-- Updates Cars main groups to:
-- JDM, Euro-spec, Us-Spec, Luxury, Super Cars
-- and removes legacy Euro from active listings.
-- ============================================================================

-- 1) Convert existing cars-euro to cars-euro-spec when euro-spec does not already exist.
UPDATE categories
SET
  name = 'Euro-spec',
  slug = 'cars-euro-spec',
  description = 'European specification vehicles',
  sort_order = 2,
  updated_date = NOW()
WHERE slug = 'cars-euro'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-euro-spec');

-- 2) Ensure Euro-spec exists under cars root.
INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Euro-spec',
  'cars-euro-spec',
  'European specification vehicles',
  '/images/topics/cars.jpg',
  true,
  2,
  (SELECT id FROM categories WHERE slug = 'cars'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-euro-spec')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars');

-- 3) Ensure Us-Spec exists under cars root.
INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Us-Spec',
  'cars-us-spec',
  'United States specification vehicles',
  '/images/topics/cars.jpg',
  true,
  3,
  (SELECT id FROM categories WHERE slug = 'cars'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-us-spec')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars');

-- 4) If both old Euro and new Euro-spec exist, migrate references and deactivate old Euro.
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'cars-euro-spec'),
    updated_date = NOW()
WHERE parent_id = (SELECT id FROM categories WHERE slug = 'cars-euro')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-euro')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-euro-spec');

UPDATE products
SET category_id = (SELECT id FROM categories WHERE slug = 'cars-euro-spec')
WHERE category_id = (SELECT id FROM categories WHERE slug = 'cars-euro')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-euro')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-euro-spec');

UPDATE categories
SET
  name = 'Euro (Legacy)',
  slug = 'cars-euro-legacy',
  active = false,
  sort_order = 99,
  updated_date = NOW()
WHERE slug = 'cars-euro'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-euro-spec');

-- 5) Normalize target group sort order.
UPDATE categories
SET sort_order = 1,
    updated_date = NOW()
WHERE slug = 'cars-jdm';

UPDATE categories
SET sort_order = 2,
    active = true,
    updated_date = NOW()
WHERE slug = 'cars-euro-spec';

UPDATE categories
SET sort_order = 3,
    active = true,
    updated_date = NOW()
WHERE slug = 'cars-us-spec';

UPDATE categories
SET sort_order = 4,
    updated_date = NOW()
WHERE slug = 'cars-luxury';

UPDATE categories
SET sort_order = 5,
    updated_date = NOW()
WHERE slug = 'cars-super-cars';

-- Verification
-- SELECT id, name, slug, parent_id, active, sort_order
-- FROM categories
-- WHERE parent_id = (SELECT id FROM categories WHERE slug = 'cars')
-- ORDER BY sort_order, name;
