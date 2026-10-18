-- 文件版本：V0.01
-- 月结落单的项目及发放主体须与批准税务原件一致；不回填历史工资。
CREATE FUNCTION guard_payroll_tax_payer() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.tax_assessment_id IS NOT NULL AND NOT EXISTS (
      SELECT 1 FROM payroll_tax_assessments a
       WHERE a.id=NEW.tax_assessment_id
         AND a.project_id IS NOT DISTINCT FROM NEW.project_id
         AND a.payer_company_id IS NOT DISTINCT FROM NEW.payer_company_id) THEN
    RAISE EXCEPTION USING ERRCODE='23514',
      MESSAGE='PAYROLL_TAX_PAYER_CHANGED: 工资落单项目及发放主体须与原批准税务认定一致';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_payroll_tax_payer_lock
  BEFORE INSERT OR UPDATE ON staff_payroll_statements
  FOR EACH ROW EXECUTE FUNCTION guard_payroll_tax_payer();
