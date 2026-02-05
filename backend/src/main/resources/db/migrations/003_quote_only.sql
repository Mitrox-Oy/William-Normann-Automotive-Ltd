-- Add quote-only support to products.

ALTER TABLE products
ADD COLUMN IF NOT EXISTS quote_only BOOLEAN DEFAULT FALSE;

UPDATE products
SET quote_only = FALSE
WHERE quote_only IS NULL;

ALTER TABLE products
ALTER COLUMN quote_only SET NOT NULL;

COMMENT ON COLUMN products.quote_only IS
'If true, product cannot be added to cart and must be purchased via quote request';
