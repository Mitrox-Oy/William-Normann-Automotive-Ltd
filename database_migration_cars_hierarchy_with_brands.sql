-- ============================================================================
-- Database Migration: Cars hierarchy with subgroups + brands
-- ============================================================================
-- cars
--   ├─ JDM
--   ├─ Euro
--   ├─ Luxury
--   └─ Super Cars
-- and brand categories nested under these groups.
-- ============================================================================

-- 1) Ensure group categories exist directly under cars root
INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'JDM',
  'cars-jdm',
  'Japanese Domestic Market vehicles',
  '/JDM.jpg',
  true,
  1,
  (SELECT id FROM categories WHERE slug = 'cars'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-jdm')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Euro',
  'cars-euro',
  'European vehicles',
  '/Euro-Spec.jpg',
  true,
  2,
  (SELECT id FROM categories WHERE slug = 'cars'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-euro')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Luxury',
  'cars-luxury',
  'Premium and executive vehicles',
  '/Luxury.jpg',
  true,
  3,
  (SELECT id FROM categories WHERE slug = 'cars'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-luxury')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Super Cars',
  'cars-super-cars',
  'High-performance supercars',
  '/Supercars.jpg',
  true,
  4,
  (SELECT id FROM categories WHERE slug = 'cars'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-super-cars')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars');

-- 2) Ensure brand categories exist
INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'BMW',
  'cars-bmw',
  'BMW vehicles',
  '/images/brands/bmw.jpg',
  true,
  1,
  (SELECT id FROM categories WHERE slug = 'cars-jdm'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-bmw')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-jdm');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Audi',
  'cars-audi',
  'Audi vehicles',
  '/images/brands/audi.jpg',
  true,
  1,
  (SELECT id FROM categories WHERE slug = 'cars-euro'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-audi')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-euro');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Mercedes-Benz',
  'cars-mercedes',
  'Mercedes-Benz vehicles',
  '/images/brands/mercedes.jpg',
  true,
  1,
  (SELECT id FROM categories WHERE slug = 'cars-luxury'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-mercedes')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-luxury');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Porsche',
  'cars-porsche',
  'Porsche vehicles',
  '/images/brands/porsche.jpg',
  true,
  1,
  (SELECT id FROM categories WHERE slug = 'cars-super-cars'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-porsche')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-super-cars');

-- 3) Re-parent existing brand categories under group categories
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'cars-jdm'),
    updated_date = NOW()
WHERE slug = 'cars-bmw'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-jdm')
  AND (parent_id IS DISTINCT FROM (SELECT id FROM categories WHERE slug = 'cars-jdm'));

UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'cars-euro'),
    updated_date = NOW()
WHERE slug = 'cars-audi'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-euro')
  AND (parent_id IS DISTINCT FROM (SELECT id FROM categories WHERE slug = 'cars-euro'));

UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'cars-luxury'),
    updated_date = NOW()
WHERE slug = 'cars-mercedes'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-luxury')
  AND (parent_id IS DISTINCT FROM (SELECT id FROM categories WHERE slug = 'cars-luxury'));

UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'cars-super-cars'),
    updated_date = NOW()
WHERE slug = 'cars-porsche'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-super-cars')
  AND (parent_id IS DISTINCT FROM (SELECT id FROM categories WHERE slug = 'cars-super-cars'));

-- 4) Keep group sort order stable
UPDATE categories SET sort_order = 1, updated_date = NOW() WHERE slug = 'cars-jdm';
UPDATE categories SET sort_order = 2, updated_date = NOW() WHERE slug = 'cars-euro';
UPDATE categories SET sort_order = 3, updated_date = NOW() WHERE slug = 'cars-luxury';
UPDATE categories SET sort_order = 4, updated_date = NOW() WHERE slug = 'cars-super-cars';

-- Verification
-- SELECT c.id, c.name, c.slug, c.parent_id, c.sort_order
-- FROM categories c
-- WHERE c.slug LIKE 'cars-%' OR c.slug = 'cars'
-- ORDER BY c.parent_id NULLS FIRST, c.sort_order, c.name;
