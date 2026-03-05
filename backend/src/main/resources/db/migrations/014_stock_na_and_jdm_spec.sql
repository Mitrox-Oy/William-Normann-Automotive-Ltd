-- Add stock N/A toggle support and rename cars-jdm display label.

ALTER TABLE products
ADD COLUMN IF NOT EXISTS stock_na BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE categories
SET
  name = 'JDM-Spec',
  description = 'Japanese domestic market specification vehicles',
  updated_date = NOW()
WHERE slug = 'cars-jdm'
  AND (name IS DISTINCT FROM 'JDM-Spec'
       OR description IS DISTINCT FROM 'Japanese domestic market specification vehicles');
