-- 文件版本：V0.01
-- HP-H11：完整税表与真实税务认定；没有初始批准版本，不回填历史工资。
CREATE TABLE payroll_tax_versions (
    id varchar(64) PRIMARY KEY,
    category varchar(64) NOT NULL,
    version varchar(16) NOT NULL,
    effective_from date NOT NULL,
    applicability_basis text NOT NULL,
    calculation_basis text NOT NULL,
    rounding_rule varchar(40) NOT NULL CHECK(rounding_rule='truncate_cent_each_step'),
    brackets jsonb NOT NULL CHECK(jsonb_typeof(brackets)='array' AND jsonb_array_length(brackets)>0),
    reason text NOT NULL,
    status varchar(16) NOT NULL CHECK(status IN ('draft','pending_review','approved','rejected')),
    review_id varchar(64) UNIQUE REFERENCES supervisor_review_decisions(review_id),
    evidence_attachment_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_by varchar(64) NOT NULL REFERENCES access_users(id),
    created_at timestamptz NOT NULL,
    decided_at timestamptz,
    UNIQUE(category,version)
);
CREATE UNIQUE INDEX uq_payroll_tax_version_date ON payroll_tax_versions(category,effective_from) WHERE status='approved';
CREATE TABLE payroll_tax_assessments (
    id varchar(64) PRIMARY KEY,
    user_id varchar(64) NOT NULL REFERENCES access_users(id),
    month varchar(7) NOT NULL,
    project_id varchar(64) NOT NULL,
    merchant_id varchar(64),
    payer_company_id varchar(64) NOT NULL,
    category varchar(64) NOT NULL,
    tax_date date NOT NULL,
    identity_basis text NOT NULL,
    transaction_basis text NOT NULL,
    professional_basis text NOT NULL,
    income_cent bigint NOT NULL CHECK(income_cent>=0),
    source_lines jsonb NOT NULL,
    source_fingerprint varchar(64) NOT NULL,
    tax_version_id varchar(64) NOT NULL REFERENCES payroll_tax_versions(id),
    tax_cent bigint NOT NULL CHECK(tax_cent>=0),
    calculation_snapshot jsonb NOT NULL,
    reason text NOT NULL,
    status varchar(16) NOT NULL CHECK(status IN ('draft','pending_review','approved','rejected')),
    review_id varchar(64) UNIQUE REFERENCES supervisor_review_decisions(review_id),
    evidence_attachment_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_by varchar(64) NOT NULL REFERENCES access_users(id),
    created_at timestamptz NOT NULL,
    decided_at timestamptz
);
CREATE INDEX idx_payroll_tax_assessment_employee ON payroll_tax_assessments(user_id,month,status,created_at DESC);
ALTER TABLE staff_payroll_statements ADD COLUMN tax_assessment_id varchar(64) REFERENCES payroll_tax_assessments(id);
ALTER TABLE staff_payroll_statements ADD COLUMN tax_version_id varchar(64) REFERENCES payroll_tax_versions(id);

CREATE FUNCTION guard_payroll_tax_proposal() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP='DELETE' THEN RAISE EXCEPTION '税费版本和认定原件不得删除'; END IF;
  IF OLD.status='draft' AND NEW.status='pending_review' THEN
    IF (to_jsonb(OLD)-ARRAY['status','review_id','evidence_attachment_ids']) IS DISTINCT FROM
       (to_jsonb(NEW)-ARRAY['status','review_id','evidence_attachment_ids'])
       OR NEW.review_id IS NULL OR jsonb_array_length(NEW.evidence_attachment_ids)=0 THEN
      RAISE EXCEPTION '税费草案提交不得改变原始内容，且须有真实审批和凭证';
    END IF;
  ELSIF OLD.status='pending_review' AND NEW.status IN ('approved','rejected') THEN
    IF (to_jsonb(OLD)-ARRAY['status','decided_at']) IS DISTINCT FROM
       (to_jsonb(NEW)-ARRAY['status','decided_at']) OR NEW.decided_at IS NULL THEN
      RAISE EXCEPTION '税费审批不得改变申请事实';
    END IF;
  ELSE RAISE EXCEPTION '税费原件或终态不可改写';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_payroll_tax_version_immutable BEFORE UPDATE OR DELETE ON payroll_tax_versions
  FOR EACH ROW EXECUTE FUNCTION guard_payroll_tax_proposal();
CREATE TRIGGER trg_payroll_tax_assessment_immutable BEFORE UPDATE OR DELETE ON payroll_tax_assessments
  FOR EACH ROW EXECUTE FUNCTION guard_payroll_tax_proposal();

-- 与认定/工资定格共同串行化真实服务收入的新增、更改与移除，避免“已核”金额在同事务中漂移。
CREATE FUNCTION lock_payroll_tax_income() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE k text;
BEGIN
  FOR k IN SELECT DISTINCT x FROM unnest(ARRAY[
      CASE WHEN TG_OP<>'INSERT' THEN 'payroll-tax:'||OLD.user_id||':'||OLD.month END,
      CASE WHEN TG_OP<>'DELETE' THEN 'payroll-tax:'||NEW.user_id||':'||NEW.month END]) x
      WHERE x IS NOT NULL ORDER BY x LOOP
    PERFORM pg_advisory_xact_lock(hashtextextended(k,0));
  END LOOP;
  IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;
CREATE TRIGGER trg_payroll_tax_income_lock BEFORE INSERT OR UPDATE OR DELETE ON staff_commission_ledger
  FOR EACH ROW EXECUTE FUNCTION lock_payroll_tax_income();

CREATE FUNCTION guard_payroll_tax_lock() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE recalculating boolean;
BEGIN
  recalculating := TG_OP='INSERT';
  IF TG_OP='UPDATE' THEN
    IF OLD.tax_version_id IS NOT NULL AND NEW.tax_version_id IS DISTINCT FROM OLD.tax_version_id THEN
      RAISE EXCEPTION '工资已锁税表版本，不得用新规则重算旧单';
    END IF;
    IF OLD.tax_assessment_id IS NOT NULL AND NEW.tax_assessment_id IS DISTINCT FROM OLD.tax_assessment_id THEN
      RAISE EXCEPTION '工资已锁税务认定，不得替换原计算事实';
    END IF;
    recalculating := ROW(OLD.base_cent,OLD.commission_cent,OLD.bonus_cent,OLD.penalty_cent,
      OLD.leave_deduction_cent,OLD.tax_cent,OLD.total_cent,OLD.closed_at) IS DISTINCT FROM
      ROW(NEW.base_cent,NEW.commission_cent,NEW.bonus_cent,NEW.penalty_cent,
      NEW.leave_deduction_cent,NEW.tax_cent,NEW.total_cent,NEW.closed_at);
  END IF;
  -- 历史单仅支付/状态改变且不新增引用，不追溯要求回填税务认定。
  IF recalculating AND NEW.tax_assessment_id IS NULL AND EXISTS(
      SELECT 1 FROM staff_commission_ledger l WHERE l.user_id=NEW.user_id AND l.month=NEW.month AND l.source_type='service_order') THEN
    RAISE EXCEPTION '服务收入缺少已批准税务认定，不得按零税关闭工资';
  END IF;
  IF NEW.tax_assessment_id IS NOT NULL AND NOT EXISTS(
      SELECT 1 FROM payroll_tax_assessments a WHERE a.id=NEW.tax_assessment_id
        AND a.user_id=NEW.user_id AND a.month=NEW.month AND a.status='approved'
        AND a.tax_version_id=NEW.tax_version_id AND a.tax_cent=NEW.tax_cent) THEN
    RAISE EXCEPTION '工资税额须引用本人本月已批准的税务认定';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_payroll_tax_statement_lock BEFORE INSERT OR UPDATE ON staff_payroll_statements
  FOR EACH ROW EXECUTE FUNCTION guard_payroll_tax_lock();
UPDATE permissions SET permission_name='维护薪资税表版本与税务认定' WHERE permission_code='staff.tax_bracket.manage';
