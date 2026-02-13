-- Ensure main PARTS categories exist under parts root for admin category selection.
-- Idempotent and safe to run multiple times.

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Engine & Drivetrain',
  'parts-engine-drivetrain',
  'Engines, driveline, intake, exhaust, and related components',
  '/images/topics/parts.jpg',
  true,
  1,
  (SELECT id FROM categories WHERE slug = 'parts'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'parts-engine-drivetrain')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Suspension & Steering',
  'parts-suspension-steering',
  'Suspension, steering, and chassis handling components',
  '/images/topics/parts.jpg',
  true,
  2,
  (SELECT id FROM categories WHERE slug = 'parts'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'parts-suspension-steering')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Brakes',
  'parts-brakes',
  'Brake kits, rotors, pads, calipers, and brake hardware',
  '/images/topics/parts.jpg',
  true,
  3,
  (SELECT id FROM categories WHERE slug = 'parts'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'parts-brakes')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Wheels & Tires',
  'parts-wheels-tires',
  'Wheels, tires, fitment hardware, and accessories',
  '/images/topics/parts.jpg',
  true,
  4,
  (SELECT id FROM categories WHERE slug = 'parts'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'parts-wheels-tires')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Electrical & Lighting',
  'parts-electrical-lighting',
  'Electrical systems, wiring, batteries, and lighting',
  '/images/topics/parts.jpg',
  true,
  5,
  (SELECT id FROM categories WHERE slug = 'parts'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'parts-electrical-lighting')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Exterior & Body',
  'parts-exterior-body',
  'Body panels, aero, and exterior components',
  '/images/topics/parts.jpg',
  true,
  6,
  (SELECT id FROM categories WHERE slug = 'parts'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'parts-exterior-body')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Interior',
  'parts-interior',
  'Interior trim, controls, seats, and cabin components',
  '/images/topics/parts.jpg',
  true,
  7,
  (SELECT id FROM categories WHERE slug = 'parts'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'parts-interior')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Cooling & HVAC',
  'parts-cooling-hvac',
  'Cooling systems and HVAC-related components',
  '/images/topics/parts.jpg',
  true,
  8,
  (SELECT id FROM categories WHERE slug = 'parts'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'parts-cooling-hvac')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

INSERT INTO categories (name, slug, description, image_url, active, sort_order, parent_id, created_date, updated_date)
SELECT
  'Maintenance & Service',
  'parts-maintenance-service',
  'Service items, filters, fluids, and consumables',
  '/images/topics/parts.jpg',
  true,
  9,
  (SELECT id FROM categories WHERE slug = 'parts'),
  NOW(),
  NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'parts-maintenance-service')
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

-- Keep ordering and parent stable.
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'parts'), sort_order = 1, updated_date = NOW()
WHERE slug = 'parts-engine-drivetrain'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'parts'), sort_order = 2, updated_date = NOW()
WHERE slug = 'parts-suspension-steering'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'parts'), sort_order = 3, updated_date = NOW()
WHERE slug = 'parts-brakes'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'parts'), sort_order = 4, updated_date = NOW()
WHERE slug = 'parts-wheels-tires'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'parts'), sort_order = 5, updated_date = NOW()
WHERE slug = 'parts-electrical-lighting'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'parts'), sort_order = 6, updated_date = NOW()
WHERE slug = 'parts-exterior-body'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'parts'), sort_order = 7, updated_date = NOW()
WHERE slug = 'parts-interior'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'parts'), sort_order = 8, updated_date = NOW()
WHERE slug = 'parts-cooling-hvac'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');

UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE slug = 'parts'), sort_order = 9, updated_date = NOW()
WHERE slug = 'parts-maintenance-service'
  AND EXISTS (SELECT 1 FROM categories WHERE slug = 'parts');
