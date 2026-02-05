-- Filter attributes system migration.
-- Adds nullable columns for backwards compatibility and backfills product_type from category roots.

ALTER TABLE products ADD COLUMN IF NOT EXISTS product_type VARCHAR(20);
ALTER TABLE products ADD COLUMN IF NOT EXISTS product_condition VARCHAR(20);
ALTER TABLE products ADD COLUMN IF NOT EXISTS oem_type VARCHAR(20);

ALTER TABLE products ADD COLUMN IF NOT EXISTS compatibility_mode VARCHAR(20);
ALTER TABLE products ADD COLUMN IF NOT EXISTS compatible_makes TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS compatible_models TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS compatible_year_start INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS compatible_year_end INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS vin_compatible BOOLEAN;

ALTER TABLE products ADD COLUMN IF NOT EXISTS make VARCHAR(100);
ALTER TABLE products ADD COLUMN IF NOT EXISTS model VARCHAR(100);
ALTER TABLE products ADD COLUMN IF NOT EXISTS car_year INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS mileage INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS fuel_type VARCHAR(40);
ALTER TABLE products ADD COLUMN IF NOT EXISTS transmission VARCHAR(40);
ALTER TABLE products ADD COLUMN IF NOT EXISTS body_type VARCHAR(40);
ALTER TABLE products ADD COLUMN IF NOT EXISTS drive_type VARCHAR(40);
ALTER TABLE products ADD COLUMN IF NOT EXISTS power_kw INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS color VARCHAR(100);
ALTER TABLE products ADD COLUMN IF NOT EXISTS warranty_included BOOLEAN;

ALTER TABLE products ADD COLUMN IF NOT EXISTS part_category VARCHAR(120);
ALTER TABLE products ADD COLUMN IF NOT EXISTS part_number VARCHAR(120);
ALTER TABLE products ADD COLUMN IF NOT EXISTS part_position TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS material VARCHAR(120);
ALTER TABLE products ADD COLUMN IF NOT EXISTS reconditioned BOOLEAN;

ALTER TABLE products ADD COLUMN IF NOT EXISTS tool_category VARCHAR(120);
ALTER TABLE products ADD COLUMN IF NOT EXISTS power_source VARCHAR(40);
ALTER TABLE products ADD COLUMN IF NOT EXISTS voltage INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS torque_min_nm INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS torque_max_nm INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS drive_size VARCHAR(40);
ALTER TABLE products ADD COLUMN IF NOT EXISTS professional_grade BOOLEAN;
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_kit BOOLEAN;

ALTER TABLE products ADD COLUMN IF NOT EXISTS custom_category VARCHAR(120);
ALTER TABLE products ADD COLUMN IF NOT EXISTS style_tags TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS finish VARCHAR(80);
ALTER TABLE products ADD COLUMN IF NOT EXISTS street_legal BOOLEAN;
ALTER TABLE products ADD COLUMN IF NOT EXISTS installation_difficulty VARCHAR(40);

-- Backfill product_type from the root category slug.
WITH RECURSIVE ancestors AS (
    SELECT c.id AS category_id, c.parent_id, c.slug, 0 AS depth
    FROM categories c
    UNION ALL
    SELECT a.category_id, p.parent_id, p.slug, a.depth + 1
    FROM ancestors a
    JOIN categories p ON p.id = a.parent_id
    WHERE a.parent_id IS NOT NULL
),
roots AS (
    SELECT DISTINCT ON (category_id) category_id, slug AS root_slug
    FROM ancestors
    WHERE parent_id IS NULL
    ORDER BY category_id, depth DESC
)
UPDATE products p
SET product_type = CASE roots.root_slug
    WHEN 'cars' THEN 'car'
    WHEN 'parts' THEN 'part'
    WHEN 'tools' THEN 'tool'
    WHEN 'custom' THEN 'custom'
    ELSE p.product_type
END
FROM roots
WHERE p.category_id = roots.category_id
  AND (p.product_type IS NULL OR p.product_type = '');

-- Indexes for common filtering dimensions.
CREATE INDEX IF NOT EXISTS idx_products_product_type ON products(product_type);
CREATE INDEX IF NOT EXISTS idx_products_product_condition ON products(product_condition);
CREATE INDEX IF NOT EXISTS idx_products_oem_type ON products(oem_type);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_brand ON products(brand);
CREATE INDEX IF NOT EXISTS idx_products_make_model_year ON products(make, model, car_year);
CREATE INDEX IF NOT EXISTS idx_products_compatibility_mode ON products(compatibility_mode);
CREATE INDEX IF NOT EXISTS idx_products_compatibility_years ON products(compatible_year_start, compatible_year_end);
CREATE INDEX IF NOT EXISTS idx_products_part_category ON products(part_category);
CREATE INDEX IF NOT EXISTS idx_products_tool_category ON products(tool_category);
CREATE INDEX IF NOT EXISTS idx_products_custom_category ON products(custom_category);
