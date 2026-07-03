-- 审计修复(2026-07-03 P0 租户隔离):V210-V213 新增的员工/服务点单行数据表补 project 隔离列。
-- 口径:员工与服务点单归属综合体(project)——project_id 由员工档案(staff_profiles,最新一条)推导;
-- service_orders 按履约人(performer_user_id)归属。存量行回填,后续写入由仓储 SQL 同步推导。
-- 目录/收款码/公司收款配置(service_catalog_items/staff_payment_codes/service_pay_config)为
-- 单综合体经营参数,维持 single_complex_default 登记口径(多综合体前加列),本迁移不动。

ALTER TABLE staff_shift_schedules    ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);
ALTER TABLE staff_attendance_records ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);
ALTER TABLE staff_base_salaries      ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);
ALTER TABLE staff_payroll_adjustments ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);
ALTER TABLE staff_commission_ledger  ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);
ALTER TABLE staff_payroll_statements ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);
ALTER TABLE staff_leave_requests     ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);
ALTER TABLE service_orders           ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);

-- 回填:每个 user 取最新员工档案的 project_id(与仓储运行时推导同口径)。
WITH sp AS (
    SELECT DISTINCT ON (user_id) user_id, project_id
      FROM staff_profiles
     ORDER BY user_id, created_at DESC
)
UPDATE staff_shift_schedules t SET project_id = sp.project_id
  FROM sp WHERE sp.user_id = t.user_id AND t.project_id IS NULL;

WITH sp AS (
    SELECT DISTINCT ON (user_id) user_id, project_id
      FROM staff_profiles
     ORDER BY user_id, created_at DESC
)
UPDATE staff_attendance_records t SET project_id = sp.project_id
  FROM sp WHERE sp.user_id = t.user_id AND t.project_id IS NULL;

WITH sp AS (
    SELECT DISTINCT ON (user_id) user_id, project_id
      FROM staff_profiles
     ORDER BY user_id, created_at DESC
)
UPDATE staff_base_salaries t SET project_id = sp.project_id
  FROM sp WHERE sp.user_id = t.user_id AND t.project_id IS NULL;

WITH sp AS (
    SELECT DISTINCT ON (user_id) user_id, project_id
      FROM staff_profiles
     ORDER BY user_id, created_at DESC
)
UPDATE staff_payroll_adjustments t SET project_id = sp.project_id
  FROM sp WHERE sp.user_id = t.user_id AND t.project_id IS NULL;

WITH sp AS (
    SELECT DISTINCT ON (user_id) user_id, project_id
      FROM staff_profiles
     ORDER BY user_id, created_at DESC
)
UPDATE staff_commission_ledger t SET project_id = sp.project_id
  FROM sp WHERE sp.user_id = t.user_id AND t.project_id IS NULL;

WITH sp AS (
    SELECT DISTINCT ON (user_id) user_id, project_id
      FROM staff_profiles
     ORDER BY user_id, created_at DESC
)
UPDATE staff_payroll_statements t SET project_id = sp.project_id
  FROM sp WHERE sp.user_id = t.user_id AND t.project_id IS NULL;

WITH sp AS (
    SELECT DISTINCT ON (user_id) user_id, project_id
      FROM staff_profiles
     ORDER BY user_id, created_at DESC
)
UPDATE staff_leave_requests t SET project_id = sp.project_id
  FROM sp WHERE sp.user_id = t.user_id AND t.project_id IS NULL;

WITH sp AS (
    SELECT DISTINCT ON (user_id) user_id, project_id
      FROM staff_profiles
     ORDER BY user_id, created_at DESC
)
UPDATE service_orders t SET project_id = sp.project_id
  FROM sp WHERE sp.user_id = t.performer_user_id AND t.project_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_staff_shift_project     ON staff_shift_schedules (project_id);
CREATE INDEX IF NOT EXISTS idx_staff_att_project       ON staff_attendance_records (project_id);
CREATE INDEX IF NOT EXISTS idx_payroll_adj_project     ON staff_payroll_adjustments (project_id);
CREATE INDEX IF NOT EXISTS idx_commission_ledger_project ON staff_commission_ledger (project_id);
CREATE INDEX IF NOT EXISTS idx_payroll_stmt_project    ON staff_payroll_statements (project_id);
CREATE INDEX IF NOT EXISTS idx_leave_project           ON staff_leave_requests (project_id);
CREATE INDEX IF NOT EXISTS idx_svc_orders_project      ON service_orders (project_id);
