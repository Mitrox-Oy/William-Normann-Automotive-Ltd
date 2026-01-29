-- Phase 1 Enhanced Cart System - Database Migration
-- Run this script to update existing cart tables with new fields

-- 1. Add new columns to carts table
ALTER TABLE carts 
ADD COLUMN IF NOT EXISTS total_items INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS session_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS last_activity TIMESTAMP;

-- 2. Add new columns to cart_items table
ALTER TABLE cart_items 
ADD COLUMN IF NOT EXISTS original_price DECIMAL(10,2),
ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10,2) DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS is_available BOOLEAN DEFAULT TRUE;

-- 3. Update existing data
-- Set total_items for existing carts
UPDATE carts 
SET total_items = (
    SELECT COALESCE(SUM(quantity), 0) 
    FROM cart_items 
    WHERE cart_items.cart_id = carts.id
) 
WHERE total_items IS NULL OR total_items = 0;

-- Set original_price for existing cart items
UPDATE cart_items 
SET original_price = unit_price 
WHERE original_price IS NULL;

-- Set expiration for existing active carts (24 hours from now)
UPDATE carts 
SET expires_at = NOW() + INTERVAL '24 hours',
    last_activity = updated_date
WHERE is_active = TRUE AND expires_at IS NULL;

-- Set session IDs for existing carts
UPDATE carts 
SET session_id = 'legacy-' || id::text 
WHERE session_id IS NULL;

-- 4. Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_carts_user_active 
ON carts(user_id, is_active);

CREATE INDEX IF NOT EXISTS idx_carts_expires_at 
ON carts(expires_at);

CREATE INDEX IF NOT EXISTS idx_carts_session_id 
ON carts(session_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_availability 
ON cart_items(is_available);

-- 5. Add constraints
ALTER TABLE carts 
ADD CONSTRAINT chk_total_items_non_negative 
CHECK (total_items >= 0);

ALTER TABLE cart_items 
ADD CONSTRAINT chk_discount_non_negative 
CHECK (discount_amount >= 0);

-- 6. Create view for cart analytics
CREATE OR REPLACE VIEW cart_analytics_view AS
SELECT 
    c.id as cart_id,
    c.user_id,
    c.created_date,
    c.updated_date,
    c.last_activity,
    c.total_amount,
    c.total_items,
    c.is_active,
    c.expires_at,
    CASE WHEN c.expires_at < NOW() THEN TRUE ELSE FALSE END as is_expired,
    COUNT(ci.id) as unique_products,
    CASE 
        WHEN c.total_items > 0 THEN c.total_amount / c.total_items 
        ELSE 0 
    END as average_item_price
FROM carts c
LEFT JOIN cart_items ci ON c.id = ci.cart_id
GROUP BY c.id, c.user_id, c.created_date, c.updated_date, c.last_activity, 
         c.total_amount, c.total_items, c.is_active, c.expires_at;

-- 7. Create function to cleanup expired carts
CREATE OR REPLACE FUNCTION cleanup_expired_carts()
RETURNS void AS $$
BEGIN
    -- Deactivate expired carts
    UPDATE carts 
    SET is_active = FALSE 
    WHERE expires_at < NOW() AND is_active = TRUE;
    
    -- Optional: Delete very old cart items (older than 30 days)
    DELETE FROM cart_items 
    WHERE cart_id IN (
        SELECT id FROM carts 
        WHERE expires_at < NOW() - INTERVAL '30 days'
    );
    
    -- Optional: Delete very old carts (older than 30 days)
    DELETE FROM carts 
    WHERE expires_at < NOW() - INTERVAL '30 days';
END;
$$ LANGUAGE plpgsql;

-- 8. Create function to update cart activity
CREATE OR REPLACE FUNCTION update_cart_activity()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE carts 
    SET last_activity = NOW(),
        updated_date = NOW()
    WHERE id = NEW.cart_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 9. Create trigger to auto-update cart activity on cart item changes
DROP TRIGGER IF EXISTS trigger_update_cart_activity ON cart_items;
CREATE TRIGGER trigger_update_cart_activity
    AFTER INSERT OR UPDATE OR DELETE ON cart_items
    FOR EACH ROW
    EXECUTE FUNCTION update_cart_activity();

-- 10. Verify migration
SELECT 'Migration completed successfully!' as status;

-- Check updated table structure
SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns 
WHERE table_name IN ('carts', 'cart_items')
ORDER BY table_name, ordinal_position;
