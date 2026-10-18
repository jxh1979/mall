-- 文件版本：V0.01
-- 实际清场原件与系统释放来源；不改旧核销、不把确认清场冒充出场扫码或退款。
CREATE TABLE amusement_session_endings (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE REFERENCES amusement_sessions(id),
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id),
    complex_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    config_version_id VARCHAR(64) NOT NULL REFERENCES amusement_business_day_configs(id),
    business_date DATE NOT NULL,
    business_start_at TIMESTAMPTZ NOT NULL,
    business_end_at TIMESTAMPTZ NOT NULL,
    mode VARCHAR(16) NOT NULL CHECK(mode IN ('natural','early')),
    planned_end_at TIMESTAMPTZ NOT NULL,
    actual_end_at TIMESTAMPTZ NOT NULL,
    confirmed_by VARCHAR(64) REFERENCES access_users(id),
    confirmed_at TIMESTAMPTZ NOT NULL,
    reason TEXT NOT NULL CHECK(length(trim(reason)) > 0),
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    cleared_people INTEGER NOT NULL CHECK(cleared_people >= 0),
    CHECK(business_start_at < business_end_at),
    CHECK(mode <> 'early' OR (confirmed_by IS NOT NULL AND actual_end_at < planned_end_at))
);
CREATE TRIGGER amusement_session_endings_immutable
    BEFORE UPDATE OR DELETE ON amusement_session_endings
    FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();

ALTER TABLE amusement_verifications
    ADD COLUMN complex_id VARCHAR(64) REFERENCES projects(id),
    ADD COLUMN business_day_version_id VARCHAR(64) REFERENCES amusement_business_day_configs(id),
    ADD COLUMN business_date DATE,
    ADD COLUMN business_start_at TIMESTAMPTZ,
    ADD COLUMN business_end_at TIMESTAMPTZ,
    ADD COLUMN release_source VARCHAR(32) CHECK(release_source IN ('exit','session_end','business_day_end')),
    ADD COLUMN release_source_id VARCHAR(64);

ALTER TABLE amusement_concurrent_reset_log
    ADD COLUMN source_id VARCHAR(64),
    ADD COLUMN complex_id VARCHAR(64) REFERENCES projects(id),
    ADD COLUMN config_version_id VARCHAR(64) REFERENCES amusement_business_day_configs(id);

CREATE TABLE amusement_session_followups (
    id VARCHAR(64) PRIMARY KEY,
    ending_id VARCHAR(64) NOT NULL REFERENCES amusement_session_endings(id),
    source_type VARCHAR(32) NOT NULL CHECK(source_type IN ('entry','reservation','group_booking')),
    source_id VARCHAR(64) NOT NULL,
    entitlement_id VARCHAR(64) REFERENCES issued_entitlements(id),
    order_id VARCHAR(64) REFERENCES ticket_orders(id),
    status VARCHAR(16) NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','completed')),
    resolution_type VARCHAR(32),
    resolution_id VARCHAR(64),
    resolved_by VARCHAR(64) REFERENCES access_users(id),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(ending_id, source_type, source_id),
    CHECK((status='pending' AND resolved_at IS NULL)
       OR (status='completed' AND resolution_type IS NOT NULL AND resolution_id IS NOT NULL
           AND resolved_by IS NOT NULL AND resolved_at IS NOT NULL))
);
CREATE INDEX idx_amusement_followups_ending ON amusement_session_followups(ending_id,status);
INSERT INTO permissions(permission_code,permission_name,domain,risk_level,requires_approval,status) VALUES
    ('amusement.session.end','确认实际清场并提前结束游乐场次','amusement','high',false,'active');

ALTER TABLE amusement_sessions
    ADD COLUMN complex_id VARCHAR(64) REFERENCES projects(id),
    ADD COLUMN business_day_version_id VARCHAR(64) REFERENCES amusement_business_day_configs(id),
    ADD COLUMN business_date DATE,
    ADD COLUMN business_start_at TIMESTAMPTZ,
    ADD COLUMN business_end_at TIMESTAMPTZ;

DROP INDEX uq_amusement_concurrent_reset_log_day;
CREATE UNIQUE INDEX uq_amusement_concurrent_reset_log_day_legacy
    ON amusement_concurrent_reset_log(project_id,business_date)
    WHERE scope='business_day_end' AND config_version_id IS NULL;
CREATE UNIQUE INDEX uq_amusement_concurrent_reset_log_window
    ON amusement_concurrent_reset_log(project_id,config_version_id,boundary_at)
    WHERE scope='business_day_end' AND config_version_id IS NOT NULL;
CREATE UNIQUE INDEX uq_amusement_concurrent_reset_log_source
    ON amusement_concurrent_reset_log(source_id) WHERE source_id IS NOT NULL;
