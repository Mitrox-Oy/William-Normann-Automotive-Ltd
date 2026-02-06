-- NOTE:
-- The BYTEA -> TEXT repair is executed by ProductSchemaRepairService at startup.
-- This migration remains as a tracked no-op to preserve migration ordering.
SELECT 1;
