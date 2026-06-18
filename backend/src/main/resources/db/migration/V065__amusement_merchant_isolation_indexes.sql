-- R3 修复 F4（ISO-GAP-03, P1）：V049 amusement 4 表 merchant_id 此前无索引（其余隔离域均建了部分索引）。
-- 补部分索引，保证按 merchant 过滤的性能/一致性与其余域一致。
CREATE INDEX IF NOT EXISTS idx_amusement_safety_templates_merchant
    ON amusement_safety_templates(merchant_id) WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_amusement_projects_merchant
    ON amusement_projects(merchant_id) WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_amusement_safety_notices_merchant
    ON amusement_safety_notices(merchant_id) WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_amusement_incidents_merchant
    ON amusement_incidents(merchant_id) WHERE merchant_id IS NOT NULL;
