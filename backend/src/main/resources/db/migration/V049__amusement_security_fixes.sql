-- P0/P1 security fixes for amusement domain:
-- add merchant_id to tables that need data-level tenant isolation

ALTER TABLE amusement_safety_templates ADD COLUMN merchant_id VARCHAR(64);
ALTER TABLE amusement_projects         ADD COLUMN merchant_id VARCHAR(64);
ALTER TABLE amusement_safety_notices   ADD COLUMN merchant_id VARCHAR(64);
ALTER TABLE amusement_incidents        ADD COLUMN merchant_id VARCHAR(64);
