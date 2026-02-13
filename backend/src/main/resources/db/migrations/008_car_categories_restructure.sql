-- Restructure CARS subcategories to generic groups used by shop UX.
-- Target categories under cars root:
-- 1) JDM        (cars-jdm)
-- 2) Euro       (cars-euro)
-- 3) Luxury     (cars-luxury)
-- 4) Super Cars (cars-super-cars)

-- 1) Rename legacy seeded brand categories if present (preserves product assignments).
UPDATE categories
SET
  name = 'JDM',
  slug = 'cars-jdm',
  description = 'Japanese Domestic Market vehicles',
  sort_order = 1,
  updated_date = NOW()
WHERE slug = 'cars-bmw'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-jdm');

UPDATE categories
SET
  name = 'Euro',
  slug = 'cars-euro',
  description = 'European vehicles',
  sort_order = 2,
  updated_date = NOW()
WHERE slug = 'cars-audi'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-euro');

UPDATE categories
SET
  name = 'Luxury',
  slug = 'cars-luxury',
  description = 'Premium and executive vehicles',
  sort_order = 3,
  updated_date = NOW()
WHERE slug = 'cars-mercedes'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-luxury');

UPDATE categories
SET
  name = 'Super Cars',
  slug = 'cars-super-cars',
  description = 'High-performance supercars',
  sort_order = 4,
  updated_date = NOW()
WHERE slug = 'cars-porsche'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-super-cars');

-- 2) Ensure all target categories exist under cars root (idempotent inserts).
INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'JDM',
  'cars-jdm',
  'Japanese Domestic Market vehicles',
  '/images/topics/cars.jpg',
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
  '/images/topics/cars.jpg',
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
  '/images/topics/cars.jpg',
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
  '/images/topics/cars.jpg',
  true,
  4,
  (SELECT id FROM categories WHERE slug = 'cars'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'cars-super-cars')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'cars');

-- 3) Normalize sort order for final cars groups.
UPDATE categories SET sort_order = 1, updated_date = NOW() WHERE slug = 'cars-jdm';
UPDATE categories SET sort_order = 2, updated_date = NOW() WHERE slug = 'cars-euro';
UPDATE categories SET sort_order = 3, updated_date = NOW() WHERE slug = 'cars-luxury';
UPDATE categories SET sort_order = 4, updated_date = NOW() WHERE slug = 'cars-super-cars';
