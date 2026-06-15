ALTER TABLE temporary_permission_grants
    ADD COLUMN IF NOT EXISTS grant_group_id VARCHAR(64);

UPDATE temporary_permission_grants
   SET grant_group_id = id::text
 WHERE grant_group_id IS NULL;

ALTER TABLE temporary_permission_grants
    ALTER COLUMN grant_group_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_temporary_permission_grants_group
    ON temporary_permission_grants (grant_group_id, status);
