-- Parts taxonomy + branch-specific filter attributes.
-- Backwards compatible: all new fields nullable and legacy part_category preserved.

ALTER TABLE products ADD COLUMN IF NOT EXISTS parts_main_category VARCHAR(80);
ALTER TABLE products ADD COLUMN IF NOT EXISTS parts_sub_category VARCHAR(120);
ALTER TABLE products ADD COLUMN IF NOT EXISTS parts_deep_category VARCHAR(160);

ALTER TABLE products ADD COLUMN IF NOT EXISTS wheel_diameter_inch DECIMAL(6,2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS wheel_width_inch DECIMAL(6,2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS wheel_bolt_pattern VARCHAR(40);
ALTER TABLE products ADD COLUMN IF NOT EXISTS wheel_offset_et INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS wheel_material VARCHAR(40);
ALTER TABLE products ADD COLUMN IF NOT EXISTS wheel_color VARCHAR(40);
ALTER TABLE products ADD COLUMN IF NOT EXISTS center_bore DECIMAL(6,2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS hub_centric_rings_needed BOOLEAN;

ALTER TABLE products ADD COLUMN IF NOT EXISTS engine_type VARCHAR(40);
ALTER TABLE products ADD COLUMN IF NOT EXISTS engine_displacement_cc INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS engine_cylinders INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS engine_power_hp INTEGER;

ALTER TABLE products ADD COLUMN IF NOT EXISTS turbo_type VARCHAR(40);
ALTER TABLE products ADD COLUMN IF NOT EXISTS turbo_flange_type VARCHAR(40);
ALTER TABLE products ADD COLUMN IF NOT EXISTS wastegate_type VARCHAR(40);

ALTER TABLE products ADD COLUMN IF NOT EXISTS rotor_diameter_mm INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS pad_compound VARCHAR(40);

ALTER TABLE products ADD COLUMN IF NOT EXISTS suspension_adjustable_height BOOLEAN;
ALTER TABLE products ADD COLUMN IF NOT EXISTS suspension_adjustable_damping BOOLEAN;

ALTER TABLE products ADD COLUMN IF NOT EXISTS lighting_voltage VARCHAR(20);
ALTER TABLE products ADD COLUMN IF NOT EXISTS bulb_type VARCHAR(40);

-- Ensure old parts rows have a compatibility mode.
UPDATE products
SET compatibility_mode = 'universal'
WHERE (compatibility_mode IS NULL OR TRIM(compatibility_mode) = '')
  AND LOWER(COALESCE(product_type, '')) = 'part';

-- Best-effort legacy part_category mapping into taxonomy slugs.
UPDATE products
SET parts_main_category = CASE
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%brake%' THEN 'brakes'
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%wheel%' OR LOWER(COALESCE(part_category, '')) LIKE '%tire%' THEN 'wheels-tires'
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%susp%' OR LOWER(COALESCE(part_category, '')) LIKE '%steer%' THEN 'suspension-steering'
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%elect%' OR LOWER(COALESCE(part_category, '')) LIKE '%light%' THEN 'electrical-lighting'
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%interior%' THEN 'interior'
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%cool%' OR LOWER(COALESCE(part_category, '')) LIKE '%hvac%' THEN 'cooling-hvac'
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%service%' OR LOWER(COALESCE(part_category, '')) LIKE '%maint%' THEN 'maintenance-service'
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%body%' OR LOWER(COALESCE(part_category, '')) LIKE '%exterior%' OR LOWER(COALESCE(part_category, '')) LIKE '%aero%' THEN 'exterior-body'
        WHEN LOWER(COALESCE(part_category, '')) <> '' THEN 'engine-drivetrain'
        ELSE parts_main_category
    END,
    parts_sub_category = COALESCE(parts_sub_category, CASE
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%turbo%' THEN 'turbochargers'
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%engine%' THEN 'engines'
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%wheel%' THEN 'wheels'
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%tire%' THEN 'tires'
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%coilover%' THEN 'coilovers'
        WHEN LOWER(COALESCE(part_category, '')) LIKE '%brake%' THEN 'brake-kits'
        ELSE NULL
    END)
WHERE LOWER(COALESCE(product_type, '')) = 'part'
  AND (parts_main_category IS NULL OR TRIM(parts_main_category) = '');

UPDATE products
SET parts_main_category = 'uncategorized'
WHERE LOWER(COALESCE(product_type, '')) = 'part'
  AND (parts_main_category IS NULL OR TRIM(parts_main_category) = '');

CREATE INDEX IF NOT EXISTS idx_products_parts_main_category ON products(parts_main_category);
CREATE INDEX IF NOT EXISTS idx_products_parts_sub_category ON products(parts_sub_category);
CREATE INDEX IF NOT EXISTS idx_products_parts_deep_category ON products(parts_deep_category);
CREATE INDEX IF NOT EXISTS idx_products_parts_taxonomy ON products(parts_main_category, parts_sub_category, parts_deep_category);

CREATE INDEX IF NOT EXISTS idx_products_wheel_bolt_pattern ON products(wheel_bolt_pattern);
CREATE INDEX IF NOT EXISTS idx_products_wheel_offset_et ON products(wheel_offset_et);
CREATE INDEX IF NOT EXISTS idx_products_engine_type ON products(engine_type);
CREATE INDEX IF NOT EXISTS idx_products_engine_displacement_cc ON products(engine_displacement_cc);
CREATE INDEX IF NOT EXISTS idx_products_engine_power_hp ON products(engine_power_hp);
CREATE INDEX IF NOT EXISTS idx_products_turbo_type ON products(turbo_type);
CREATE INDEX IF NOT EXISTS idx_products_rotor_diameter_mm ON products(rotor_diameter_mm);
CREATE INDEX IF NOT EXISTS idx_products_bulb_type ON products(bulb_type);