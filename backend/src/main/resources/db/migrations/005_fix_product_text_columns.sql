-- Fix legacy PostgreSQL schema where products.description or products.brand
-- were created as BYTEA instead of text-compatible types.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'products'
          AND column_name = 'description'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE products
            ALTER COLUMN description TYPE TEXT
            USING encode(description, 'escape');
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'products'
          AND column_name = 'brand'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE products
            ALTER COLUMN brand TYPE TEXT
            USING encode(brand, 'escape');
    END IF;
END $$;
