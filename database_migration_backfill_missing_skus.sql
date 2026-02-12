-- Backfill missing product SKUs (only where sku is NULL or blank).
-- SKU policy:
-- - Derived from existing product snapshot fields (human-friendly base)
-- - Appended with a short random suffix to avoid collisions
-- - Max length 50 chars
--
-- NOTE: This does not overwrite existing SKUs.

BEGIN;

WITH missing AS (
  SELECT
    id,
    COALESCE(NULLIF(BTRIM(product_type), ''), '') AS product_type,
    name,
    brand,
    make,
    model,
    car_year AS year,
    part_category
  FROM products
  WHERE sku IS NULL OR BTRIM(sku) = ''
),
bases AS (
  SELECT
    id,
    CASE
      WHEN LOWER(product_type) = 'part' THEN
        CONCAT_WS('-',
          'PART',
          NULLIF(REGEXP_REPLACE(UPPER(COALESCE(part_category, '')), '[^A-Z0-9]+', '-', 'g'), ''),
          NULLIF(REGEXP_REPLACE(UPPER(COALESCE(make, '')), '[^A-Z0-9]+', '-', 'g'), ''),
          NULLIF(REGEXP_REPLACE(UPPER(COALESCE(brand, '')), '[^A-Z0-9]+', '-', 'g'), ''),
          NULLIF(REGEXP_REPLACE(UPPER(COALESCE(name, '')), '[^A-Z0-9]+', '-', 'g'), '')
        )
      WHEN LOWER(product_type) = 'car' THEN
        CONCAT_WS('-',
          NULLIF(REGEXP_REPLACE(UPPER(COALESCE(make, '')), '[^A-Z0-9]+', '-', 'g'), ''),
          NULLIF(REGEXP_REPLACE(UPPER(COALESCE(model, '')), '[^A-Z0-9]+', '-', 'g'), ''),
          NULLIF(REGEXP_REPLACE(UPPER(COALESCE(name, '')), '[^A-Z0-9]+', '-', 'g'), ''),
          NULLIF(COALESCE(year::text, ''), '')
        )
      ELSE
        NULLIF(REGEXP_REPLACE(UPPER(COALESCE(name, 'PROD')), '[^A-Z0-9]+', '-', 'g'), '')
    END AS base
  FROM missing
),
normalized AS (
  SELECT
    id,
    REGEXP_REPLACE(REGEXP_REPLACE(COALESCE(base, 'PROD'), '-{2,}', '-', 'g'), '(^-+)|(-+$)', '', 'g') AS base_norm
  FROM bases
),
final AS (
  SELECT
    id,
    LEFT(base_norm, 43) || '-' || UPPER(SUBSTR(MD5(RANDOM()::text || CLOCK_TIMESTAMP()::text || id::text), 1, 6)) AS new_sku
  FROM normalized
)
UPDATE products p
SET sku = f.new_sku
FROM final f
WHERE p.id = f.id;

COMMIT;

-- Verification (run manually after):
-- 1) Check remaining missing:
--    SELECT COUNT(*) FROM products WHERE sku IS NULL OR BTRIM(sku) = '';
-- 2) Check duplicates:
--    SELECT sku, COUNT(*) FROM products GROUP BY sku HAVING COUNT(*) > 1;

