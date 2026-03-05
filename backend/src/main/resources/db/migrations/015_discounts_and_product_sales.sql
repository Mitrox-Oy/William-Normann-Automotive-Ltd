-- Add product-level sale pricing, reusable discount codes, and order discount metadata.

ALTER TABLE products
ADD COLUMN IF NOT EXISTS sale_price NUMERIC(10, 2);

COMMENT ON COLUMN products.sale_price IS
'Optional on-sale unit price. When set lower than price, storefront uses this discounted amount.';

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS discount_code VARCHAR(64);

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN orders.discount_code IS
'Applied discount code at checkout time, if any';

COMMENT ON COLUMN orders.discount_amount IS
'Absolute amount discounted from order subtotal due to discount code';

CREATE TABLE IF NOT EXISTS discount_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(500),
    percentage NUMERIC(5, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    applies_to_all_products BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_discount_codes_active
    ON discount_codes(active);

CREATE TABLE IF NOT EXISTS discount_code_categories (
    discount_code_id BIGINT NOT NULL REFERENCES discount_codes(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (discount_code_id, category_id)
);

CREATE INDEX IF NOT EXISTS idx_discount_code_categories_category
    ON discount_code_categories(category_id);
