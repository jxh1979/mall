-- 文件版本：V0.01
-- 完整工资依据与不可变原件；不回填或改写历史已付工资。
CREATE TABLE staff_payroll_scheme_versions (
 id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64) NOT NULL,
 project_id VARCHAR(64) NOT NULL, merchant_id VARCHAR(64), payer_company_id VARCHAR(64) NOT NULL,
 version VARCHAR(16) NOT NULL, effective_from DATE NOT NULL, effective_to DATE,
 position_code VARCHAR(64) NOT NULL, components JSONB NOT NULL, performance_rules JSONB,
 worktime_terms JSONB NOT NULL, payment_basis TEXT NOT NULL,
 reason TEXT NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'draft',
 review_id VARCHAR(64), evidence_attachment_ids JSONB NOT NULL DEFAULT '[]',
 created_by VARCHAR(64) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), approved_at TIMESTAMPTZ,
 CHECK(effective_to IS NULL OR effective_to>=effective_from),
 CHECK(status IN ('draft','pending_review','approved','rejected')),
 UNIQUE(user_id,version)
);
CREATE INDEX idx_payroll_scheme_employee ON staff_payroll_scheme_versions(user_id,effective_from);
CREATE TABLE staff_payroll_calendar_versions (
 id VARCHAR(64) PRIMARY KEY, project_id VARCHAR(64) NOT NULL,
 version VARCHAR(16) NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL,
 days JSONB NOT NULL, source_basis TEXT NOT NULL, reason TEXT NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'draft', review_id VARCHAR(64),
 evidence_attachment_ids JSONB NOT NULL DEFAULT '[]', created_by VARCHAR(64) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), approved_at TIMESTAMPTZ,
 CHECK(end_date>=start_date), CHECK(status IN ('draft','pending_review','approved','rejected'))
);
CREATE TABLE staff_payroll_period_facts (
 id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64) NOT NULL, month VARCHAR(7) NOT NULL,
 project_id VARCHAR(64) NOT NULL, merchant_id VARCHAR(64), payer_company_id VARCHAR(64) NOT NULL,
 scheme_version_id VARCHAR(64) NOT NULL REFERENCES staff_payroll_scheme_versions(id),
 calendar_version_id VARCHAR(64) NOT NULL REFERENCES staff_payroll_calendar_versions(id),
 component_facts JSONB NOT NULL, performance_facts JSONB, cycle_start DATE NOT NULL, cycle_end DATE NOT NULL,
 cycle_standard_minutes BIGINT NOT NULL CHECK(cycle_standard_minutes>=0),
 rest_compensated_minutes JSONB NOT NULL DEFAULT '{}', agreed_due_date DATE NOT NULL,
 statutory_due_date DATE, evidence_attachment_ids JSONB NOT NULL DEFAULT '[]',
 reason TEXT NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'draft',review_id VARCHAR(64),
 created_by VARCHAR(64) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),approved_at TIMESTAMPTZ,
 CHECK(cycle_end>=cycle_start),CHECK(status IN ('draft','pending_review','approved','rejected'))
);
CREATE INDEX idx_payroll_period_employee ON staff_payroll_period_facts(user_id,month);
CREATE TABLE staff_payroll_worktime_runs (
 id VARCHAR(64) PRIMARY KEY,user_id VARCHAR(64) NOT NULL,month VARCHAR(7) NOT NULL,
 project_id VARCHAR(64) NOT NULL,merchant_id VARCHAR(64),
 facts_id VARCHAR(64) NOT NULL REFERENCES staff_payroll_period_facts(id),
 cycle_start DATE NOT NULL,cycle_end DATE NOT NULL,source_fingerprint VARCHAR(64) NOT NULL,
 input_snapshot JSONB NOT NULL,result_snapshot JSONB NOT NULL,status VARCHAR(24) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),CHECK(status IN ('confirmed','cycle_pending','needs_review'))
);
ALTER TABLE staff_payroll_statements
 ADD COLUMN statement_kind VARCHAR(24) NOT NULL DEFAULT 'monthly',
 ADD COLUMN origin_event_id VARCHAR(64),ADD COLUMN scheme_version_id VARCHAR(64),
 ADD COLUMN facts_id VARCHAR(64),ADD COLUMN merchant_id VARCHAR(64),
 ADD COLUMN calculation_snapshot JSONB NOT NULL DEFAULT '{}',
 ADD COLUMN payment_due_date DATE,ADD COLUMN payment_due_basis JSONB NOT NULL DEFAULT '{}',
 ADD COLUMN ar_ap_item_id VARCHAR(64),ADD COLUMN paid_amount_cent BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN allowance_cent BIGINT NOT NULL DEFAULT 0,ADD COLUMN performance_cent BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN overtime_cent BIGINT NOT NULL DEFAULT 0,ADD COLUMN correction_cent BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN tip_cent BIGINT NOT NULL DEFAULT 0,ADD COLUMN service_fee_cent BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN cash_penalty_requested_cent BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN cash_penalty_cap_base_cent BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN cash_penalty_cap_cent BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN pending_items JSONB NOT NULL DEFAULT '[]',
 ADD CONSTRAINT chk_payroll_statement_kind CHECK(statement_kind IN ('monthly','termination','supplemental'));
ALTER TABLE staff_payroll_statements DROP CONSTRAINT uq_payroll_statement;
CREATE UNIQUE INDEX uq_payroll_statement_monthly ON staff_payroll_statements(user_id,month) WHERE statement_kind IN ('monthly','termination');
CREATE UNIQUE INDEX uq_payroll_statement_origin ON staff_payroll_statements(origin_event_id) WHERE origin_event_id IS NOT NULL;
ALTER TABLE staff_payroll_statements DROP CONSTRAINT chk_payroll_statement_status;
ALTER TABLE staff_payroll_statements ADD CONSTRAINT chk_payroll_statement_status CHECK(status IN ('closed','partial_paid','paid','no_payment_due'));
ALTER TABLE staff_payroll_payouts DROP CONSTRAINT uq_payroll_payout;
ALTER TABLE staff_payroll_payouts ADD COLUMN finance_payment_id VARCHAR(64);
CREATE UNIQUE INDEX uq_payroll_finance_payment ON staff_payroll_payouts(finance_payment_id) WHERE finance_payment_id IS NOT NULL;
CREATE TABLE staff_payroll_statement_lines (
 id VARCHAR(64) PRIMARY KEY,statement_id VARCHAR(64) NOT NULL REFERENCES staff_payroll_statements(id),
 component_code VARCHAR(64) NOT NULL,component_type VARCHAR(32) NOT NULL,description TEXT NOT NULL,
 gross_cent BIGINT NOT NULL,embedded_leave_cent BIGINT NOT NULL DEFAULT 0,
 additional_leave_cent BIGINT NOT NULL DEFAULT 0,amount_cent BIGINT NOT NULL,
 source_type VARCHAR(64),source_id VARCHAR(64),calculation_snapshot JSONB NOT NULL,
 UNIQUE(statement_id,component_code,source_type,source_id)
);
CREATE UNIQUE INDEX uq_payroll_bar_allocation ON staff_payroll_statement_lines(source_id) WHERE source_type='bar_payroll_allocation';
CREATE TABLE staff_payroll_corrections (
 id VARCHAR(64) PRIMARY KEY,user_id VARCHAR(64) NOT NULL,project_id VARCHAR(64) NOT NULL,merchant_id VARCHAR(64),
 original_statement_id VARCHAR(64) NOT NULL REFERENCES staff_payroll_statements(id),
 source_review_id VARCHAR(64) NOT NULL UNIQUE,source_adjustment_id VARCHAR(64) NOT NULL,
 original_month VARCHAR(7) NOT NULL,amount_cent BIGINT,calculation_snapshot JSONB NOT NULL,
 status VARCHAR(24) NOT NULL,target_statement_id VARCHAR(64) REFERENCES staff_payroll_statements(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),paid_at TIMESTAMPTZ,
 CHECK(amount_cent IS NULL OR amount_cent>=0),CHECK(status IN ('pending','attached','paid','no_difference','needs_review'))
);
CREATE INDEX idx_payroll_correction_pending ON staff_payroll_corrections(user_id,status);
CREATE TABLE staff_performance_score_adjustments (
 id VARCHAR(64) PRIMARY KEY,user_id VARCHAR(64) NOT NULL,month VARCHAR(7) NOT NULL,
 project_id VARCHAR(64) NOT NULL,merchant_id VARCHAR(64),points NUMERIC(12,4) NOT NULL CHECK(points>0),
 rule_code VARCHAR(64) NOT NULL,scheme_version_id VARCHAR(64) NOT NULL REFERENCES staff_payroll_scheme_versions(id),
 reason TEXT NOT NULL,review_id VARCHAR(64) NOT NULL UNIQUE,status VARCHAR(24) NOT NULL,
 reversal_of VARCHAR(64) UNIQUE REFERENCES staff_performance_score_adjustments(id),
 created_by VARCHAR(64) NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT now(),approved_at TIMESTAMPTZ,
 CHECK(status IN ('pending_review','approved','rejected'))
);

-- 普通月薪可支付已确认部分，未结周期/分配来源保留，不伪装零元结案。
CREATE TABLE staff_payroll_pending_sources (
 id VARCHAR(64) PRIMARY KEY,user_id VARCHAR(64) NOT NULL,project_id VARCHAR(64) NOT NULL,merchant_id VARCHAR(64),
 original_statement_id VARCHAR(64) NOT NULL REFERENCES staff_payroll_statements(id),
 source_type VARCHAR(64) NOT NULL,source_id VARCHAR(128) NOT NULL,message TEXT NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'pending',resolved_statement_id VARCHAR(64) REFERENCES staff_payroll_statements(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),resolved_at TIMESTAMPTZ,
 CHECK(status IN ('pending','resolved')),UNIQUE(user_id,source_type,source_id)
);
CREATE UNIQUE INDEX uq_payroll_consumed_source ON staff_payroll_statement_lines(source_type,source_id) WHERE source_type IN ('staff_commission_ledger','bar_commission','payroll_bonus','payroll_correction','worktime_delta');

CREATE TABLE staff_performance_rule_versions (
 id varchar(64) PRIMARY KEY,project_id varchar(64) NOT NULL,merchant_id varchar(64),owner_company_id varchar(64) NOT NULL,
 version varchar(16) NOT NULL,effective_from date NOT NULL,deductions jsonb NOT NULL,reason text NOT NULL,
 status varchar(24) NOT NULL DEFAULT 'draft' CHECK(status IN ('draft','pending_review','approved','rejected')),
 review_id varchar(64) UNIQUE REFERENCES supervisor_review_decisions(review_id),evidence_attachment_ids jsonb NOT NULL DEFAULT '[]',
 created_by varchar(64) NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),approved_at timestamptz
);
CREATE UNIQUE INDEX uq_staff_performance_rule_effective ON staff_performance_rule_versions(project_id,COALESCE(merchant_id,''),owner_company_id,effective_from) WHERE status='approved';
CREATE TABLE staff_payroll_calculation_drafts (
 id varchar(64) PRIMARY KEY,user_id varchar(64) NOT NULL,month varchar(7) NOT NULL,
 statement_kind varchar(24) NOT NULL CHECK(statement_kind IN ('monthly','termination','supplemental')),
 origin_event_id varchar(64),scheme_version_id varchar(64) NOT NULL REFERENCES staff_payroll_scheme_versions(id),
 facts_id varchar(64) NOT NULL REFERENCES staff_payroll_period_facts(id),project_id varchar(64) NOT NULL,
 merchant_id varchar(64),payer_company_id varchar(64) NOT NULL,before_tax_cent bigint NOT NULL CHECK(before_tax_cent>=0),
 source_fingerprint varchar(64) NOT NULL,calculation_snapshot jsonb NOT NULL,
 status varchar(24) NOT NULL DEFAULT 'calculated' CHECK(status IN ('calculated','settled')),
 statement_id varchar(64) UNIQUE REFERENCES staff_payroll_statements(id),created_by varchar(64) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE payroll_tax_assessments ADD COLUMN payroll_calculation_id varchar(64) REFERENCES staff_payroll_calculation_drafts(id),
 ADD COLUMN professional_calculation_reference text;
CREATE INDEX idx_payroll_complete_tax_source ON payroll_tax_assessments(payroll_calculation_id,status);
CREATE FUNCTION guard_staff_payroll_proposal() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION '工资依据原件不得删除'; END IF;
 IF OLD.status='draft' AND NEW.status='pending_review' THEN
  IF (to_jsonb(OLD)-ARRAY['status','review_id','evidence_attachment_ids']) IS DISTINCT FROM (to_jsonb(NEW)-ARRAY['status','review_id','evidence_attachment_ids'])
   OR NEW.review_id IS NULL OR jsonb_array_length(NEW.evidence_attachment_ids)=0 THEN RAISE EXCEPTION '工资依据须有真实复核和原件，提交不得变更原始内容'; END IF;
 ELSIF OLD.status='pending_review' AND NEW.status IN ('approved','rejected') THEN
  IF (to_jsonb(OLD)-ARRAY['status','approved_at']) IS DISTINCT FROM (to_jsonb(NEW)-ARRAY['status','approved_at']) THEN RAISE EXCEPTION '工资审批不得变更原始内容'; END IF;
 ELSE RAISE EXCEPTION '工资依据和终态不得改写'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_payroll_scheme_immutable BEFORE UPDATE OR DELETE ON staff_payroll_scheme_versions FOR EACH ROW EXECUTE FUNCTION guard_staff_payroll_proposal();
CREATE TRIGGER trg_payroll_calendar_immutable BEFORE UPDATE OR DELETE ON staff_payroll_calendar_versions FOR EACH ROW EXECUTE FUNCTION guard_staff_payroll_proposal();
CREATE TRIGGER trg_payroll_period_immutable BEFORE UPDATE OR DELETE ON staff_payroll_period_facts FOR EACH ROW EXECUTE FUNCTION guard_staff_payroll_proposal();
CREATE TRIGGER trg_performance_rule_immutable BEFORE UPDATE OR DELETE ON staff_performance_rule_versions FOR EACH ROW EXECUTE FUNCTION guard_staff_payroll_proposal();
CREATE FUNCTION guard_staff_payroll_calculation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION '工资税前原件不得删除'; END IF;
 IF OLD.status<>'calculated' OR NEW.status<>'settled' OR NEW.statement_id IS NULL
  OR (to_jsonb(OLD)-ARRAY['status','statement_id']) IS DISTINCT FROM (to_jsonb(NEW)-ARRAY['status','statement_id']) THEN
  RAISE EXCEPTION '完整工资税前原件不可改写，只能绑定实际定格工资'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_payroll_calculation_immutable BEFORE UPDATE OR DELETE ON staff_payroll_calculation_drafts FOR EACH ROW EXECUTE FUNCTION guard_staff_payroll_calculation();
CREATE FUNCTION guard_staff_payroll_statement() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION '工资定格原件不得删除'; END IF;
 IF OLD.scheme_version_id IS NOT NULL AND (to_jsonb(OLD)-ARRAY['status','ar_ap_item_id','paid_amount_cent']) IS DISTINCT FROM
    (to_jsonb(NEW)-ARRAY['status','ar_ap_item_id','paid_amount_cent']) THEN RAISE EXCEPTION '工资定格金额及计算依据不可改写'; END IF;
 IF NEW.paid_amount_cent<OLD.paid_amount_cent OR NEW.paid_amount_cent>NEW.total_cent THEN RAISE EXCEPTION '工资已付累计必须来自真实成功付款'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_staff_payroll_statement_immutable BEFORE UPDATE OR DELETE ON staff_payroll_statements FOR EACH ROW EXECUTE FUNCTION guard_staff_payroll_statement();
CREATE FUNCTION reject_staff_payroll_fact_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION '工资计算明细和原始工时事实只可新增，不得改写或删除'; END $$;
CREATE TRIGGER trg_staff_payroll_line_immutable BEFORE UPDATE OR DELETE ON staff_payroll_statement_lines FOR EACH ROW EXECUTE FUNCTION reject_staff_payroll_fact_change();
CREATE TRIGGER trg_staff_payroll_worktime_immutable BEFORE UPDATE OR DELETE ON staff_payroll_worktime_runs FOR EACH ROW EXECUTE FUNCTION reject_staff_payroll_fact_change();
CREATE TRIGGER trg_staff_payroll_payout_immutable BEFORE UPDATE OR DELETE ON staff_payroll_payouts FOR EACH ROW EXECUTE FUNCTION reject_staff_payroll_fact_change();

INSERT INTO permissions(permission_code,permission_name,domain,risk_level,requires_approval,status)
VALUES('staff.performance.rule.approve','批准经营主体绩效扣分分值表','staff','high',true,'active') ON CONFLICT(permission_code) DO NOTHING;
-- 不自动授予任何角色；正式角色/岗位派生仍须按现有高危授出流程批准。
CREATE UNIQUE INDEX uq_payroll_scheme_effective ON staff_payroll_scheme_versions(user_id,effective_from) WHERE status='approved';

