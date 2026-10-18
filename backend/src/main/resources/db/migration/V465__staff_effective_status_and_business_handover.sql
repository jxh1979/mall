-- 文件版本：V0.01
-- HP-H04：审批生效与撤权、业务交接分开；无存量回填。
ALTER TABLE staff_employment_status_revisions
    ADD COLUMN effective_at TIMESTAMPTZ NOT NULL DEFAULT now();
CREATE INDEX idx_staff_employment_due
    ON staff_employment_status_revisions(effective_at, review_id) WHERE applied_at IS NULL;

CREATE TABLE staff_business_handovers (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) NOT NULL REFERENCES staff_profiles(id),
    review_id VARCHAR(64) NOT NULL REFERENCES staff_employment_status_revisions(review_id),
    business_type VARCHAR(64) NOT NULL CHECK (business_type IN
        ('approval_node','facility_work_order','facility_inspection','merchant_work_order',
         'service_order','leasing_lead','integration_difference','bar_table_session','risk_alert')),
    business_id VARCHAR(128) NOT NULL,
    previous_user_id VARCHAR(64) NOT NULL,
    target_user_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    reason VARCHAR(1000) NOT NULL,
    assigned_by VARCHAR(64) NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    source_state JSONB NOT NULL CHECK (jsonb_typeof(source_state)='object'),
    responsibility_started_at TIMESTAMPTZ,
    CHECK (previous_user_id<>target_user_id),
    CHECK (responsibility_started_at IS NULL OR responsibility_started_at<=assigned_at)
);
CREATE INDEX idx_staff_handover_profile ON staff_business_handovers(profile_id,assigned_at,id);
CREATE INDEX idx_staff_handover_business ON staff_business_handovers(business_type,business_id,assigned_at,id);
