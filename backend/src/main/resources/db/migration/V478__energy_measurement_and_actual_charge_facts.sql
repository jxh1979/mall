-- 文件版本：V0.01
-- 能源初值、换表、异常复核及实际费用；不回填缺失的计量或费用事实。
ALTER TABLE energy_meter_readings DROP CONSTRAINT uq_energy_meter_readings_meter_period;
ALTER TABLE energy_meter_readings ALTER COLUMN prev_reading_value DROP NOT NULL;
ALTER TABLE energy_meter_readings ALTER COLUMN consumption DROP NOT NULL;
ALTER TABLE energy_meter_readings DROP CONSTRAINT chk_energy_meter_readings_abnormal;
ALTER TABLE energy_meter_readings ADD CONSTRAINT chk_energy_meter_readings_unverified CHECK (
    (prev_reading_value IS NOT NULL AND consumption IS NOT NULL)
    OR (prev_reading_value IS NULL AND consumption IS NULL AND (status='abnormal' OR status='reviewed')));
CREATE UNIQUE INDEX uq_energy_meter_reading_time ON energy_meter_readings(meter_id,read_at);

CREATE TABLE energy_meter_openings(
    meter_id VARCHAR(64) PRIMARY KEY REFERENCES energy_meters(id),
    initial_value NUMERIC NOT NULL CHECK(initial_value>=0),initial_at TIMESTAMPTZ NOT NULL,
    recorded_by VARCHAR(64) NOT NULL REFERENCES access_users(id),note TEXT NOT NULL,
    attachment_ids JSONB NOT NULL CHECK(jsonb_typeof(attachment_ids)='array' AND jsonb_array_length(attachment_ids)>0)
);
CREATE TABLE energy_reading_evidence(
    reading_id VARCHAR(64) PRIMARY KEY REFERENCES energy_meter_readings(id),
    attachment_ids JSONB NOT NULL CHECK(jsonb_typeof(attachment_ids)='array'),note TEXT
);
CREATE TABLE energy_meter_replacements(
    id VARCHAR(64) PRIMARY KEY,old_meter_id VARCHAR(64) NOT NULL UNIQUE REFERENCES energy_meters(id),
    new_meter_id VARCHAR(64) NOT NULL UNIQUE REFERENCES energy_meters(id),
    period VARCHAR(16) NOT NULL,old_final_value NUMERIC NOT NULL CHECK(old_final_value>=0),
    new_initial_value NUMERIC NOT NULL CHECK(new_initial_value>=0),replaced_at TIMESTAMPTZ NOT NULL,
    old_reading_id VARCHAR(64) NOT NULL UNIQUE REFERENCES energy_meter_readings(id),note TEXT NOT NULL,
    attachment_ids JSONB NOT NULL CHECK(jsonb_typeof(attachment_ids)='array' AND jsonb_array_length(attachment_ids)>0),
    recorded_by VARCHAR(64) NOT NULL REFERENCES access_users(id),CHECK(old_meter_id<>new_meter_id)
);
CREATE TABLE energy_reading_verifications(
    id VARCHAR(64) PRIMARY KEY,reading_id VARCHAR(64) NOT NULL REFERENCES energy_meter_readings(id),
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),merchant_id VARCHAR(64),
    verified_value NUMERIC NOT NULL,verified_opening_value NUMERIC NOT NULL,
    method VARCHAR(32) NOT NULL CHECK(method IN('reread','onsite_check')),reason TEXT NOT NULL,
    attachment_ids JSONB NOT NULL CHECK(jsonb_typeof(attachment_ids)='array' AND jsonb_array_length(attachment_ids)>0),
    verified_by VARCHAR(64) NOT NULL REFERENCES access_users(id),review_id VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL CHECK(status IN('pending_review','approved','rejected')),verified_at TIMESTAMPTZ NOT NULL,
    CHECK(verified_value>=verified_opening_value AND verified_opening_value>=0)
);
CREATE TABLE energy_meter_parent_transfers(
    id BIGSERIAL PRIMARY KEY,meter_id VARCHAR(64) NOT NULL REFERENCES energy_meters(id),
    old_parent_id VARCHAR(64) NOT NULL REFERENCES energy_meters(id),new_parent_id VARCHAR(64) NOT NULL REFERENCES energy_meters(id),
    effective_at TIMESTAMPTZ NOT NULL,replacement_id VARCHAR(64) NOT NULL REFERENCES energy_meter_replacements(id),
    UNIQUE(meter_id,replacement_id),CHECK(old_parent_id<>new_parent_id)
);
CREATE UNIQUE INDEX uq_energy_reading_open_verification ON energy_reading_verifications(reading_id) WHERE status IN('pending_review','approved');
CREATE VIEW energy_effective_meter_readings AS
    SELECT r.*,COALESCE(v.verified_value,r.reading_value) AS effective_reading_value,
        CASE WHEN v.id IS NOT NULL THEN v.verified_value-v.verified_opening_value ELSE r.consumption END AS effective_consumption
    FROM energy_meter_readings r LEFT JOIN energy_reading_verifications v ON v.reading_id=r.id AND v.status='approved';
CREATE TABLE energy_actual_charges(
    id VARCHAR(64) PRIMARY KEY,contract_id VARCHAR(64) NOT NULL REFERENCES contracts(id),
    meter_id VARCHAR(64) NOT NULL REFERENCES energy_meters(id),merchant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),period VARCHAR(16) NOT NULL,
    amount_cent BIGINT NOT NULL CHECK(amount_cent>=0),due_date DATE NOT NULL,supplier_bill_reference TEXT NOT NULL,
    attachment_ids JSONB NOT NULL CHECK(jsonb_typeof(attachment_ids)='array' AND jsonb_array_length(attachment_ids)>0),
    reading_ids JSONB NOT NULL CHECK(jsonb_typeof(reading_ids)='array' AND jsonb_array_length(reading_ids)>0),
    consumption NUMERIC NOT NULL CHECK(consumption>=0),recorded_by VARCHAR(64) NOT NULL REFERENCES access_users(id),
    created_at TIMESTAMPTZ NOT NULL,ar_ap_item_id VARCHAR(64) REFERENCES ar_ap_items(id),settled_at TIMESTAMPTZ,
    UNIQUE(meter_id,period)
);
CREATE INDEX idx_energy_actual_charges_period ON energy_actual_charges(project_id,merchant_id,period);
CREATE TABLE energy_review_settings(
    id VARCHAR(64) PRIMARY KEY,project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    days INTEGER NOT NULL,min_days INTEGER NOT NULL,max_days INTEGER NOT NULL,
    supervisor_id VARCHAR(64) NOT NULL REFERENCES access_users(id),review_id VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL CHECK(status IN('pending_review','approved','rejected')),created_at TIMESTAMPTZ NOT NULL,
    CHECK(min_days>0 AND max_days>=min_days AND days BETWEEN min_days AND max_days)
);
CREATE TABLE energy_anomaly_escalations(
    reading_id VARCHAR(64) PRIMARY KEY REFERENCES energy_meter_readings(id),
    supervisor_id VARCHAR(64) NOT NULL REFERENCES access_users(id),deadline_at TIMESTAMPTZ NOT NULL,escalated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE energy_verifier_assignments(
    id BIGSERIAL PRIMARY KEY,reading_id VARCHAR(64) NOT NULL REFERENCES energy_meter_readings(id),
    verifier_id VARCHAR(64) NOT NULL REFERENCES access_users(id),reason TEXT NOT NULL,
    assigned_by VARCHAR(64) NOT NULL REFERENCES access_users(id),assigned_at TIMESTAMPTZ NOT NULL
);

CREATE FUNCTION energy_preserve_original_fact() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN RAISE EXCEPTION 'energy original fact cannot be deleted'; END IF;
    IF TG_TABLE_NAME='energy_meter_readings' THEN
        IF (to_jsonb(NEW)-ARRAY['status','reviewed_by','reviewed_at','updated_at']) IS DISTINCT FROM
           (to_jsonb(OLD)-ARRAY['status','reviewed_by','reviewed_at','updated_at'])
           OR (OLD.status='reviewed' AND to_jsonb(NEW) IS DISTINCT FROM to_jsonb(OLD)) THEN
            RAISE EXCEPTION 'energy original reading cannot be rewritten';
        END IF;
    ELSIF TG_TABLE_NAME IN('energy_reading_verifications','energy_review_settings') THEN
        IF (to_jsonb(NEW)-'status') IS DISTINCT FROM (to_jsonb(OLD)-'status') OR OLD.status<>'pending_review'
           OR NEW.status NOT IN('approved','rejected') THEN RAISE EXCEPTION 'energy review fact cannot be rewritten'; END IF;
    ELSIF TG_TABLE_NAME='energy_actual_charges' THEN
        IF (to_jsonb(NEW)-ARRAY['ar_ap_item_id','settled_at']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['ar_ap_item_id','settled_at'])
           OR OLD.settled_at IS NOT NULL OR NEW.settled_at IS NULL THEN RAISE EXCEPTION 'energy charge fact cannot be rewritten'; END IF;
    ELSE RAISE EXCEPTION 'energy fact is append only'; END IF;
    RETURN NEW;
END $$;
DO $$ DECLARE target TEXT; BEGIN
    FOREACH target IN ARRAY ARRAY['energy_meter_readings','energy_meter_openings','energy_reading_evidence',
        'energy_meter_replacements','energy_reading_verifications','energy_actual_charges','energy_review_settings',
        'energy_anomaly_escalations','energy_verifier_assignments','energy_meter_parent_transfers'] LOOP
        EXECUTE format('CREATE TRIGGER trg_energy_fact_immutable BEFORE UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION energy_preserve_original_fact()',target);
    END LOOP;
END $$;

INSERT INTO permissions(permission_code,permission_name,domain,risk_level,requires_approval,status)
VALUES('energy.reading.approve','确认能源异常核表','platform','high',TRUE,'active')
ON CONFLICT(permission_code) DO NOTHING;
