-- 请假自动扣薪(用户 2026-07-05 拍板):月结按请假类型分类扣减。
-- 口径:病假(sick)不扣;事假(personal,无薪假)按日工资×天数扣;
--       年假/调休等法定或带薪假(annual/comp)不扣。日工资=月薪÷21.75(法定月计薪天数)。
-- 薪资单增独立"请假扣薪"列,与"处罚"分离——事假扣款不并入处罚,员工看得清、审计分得开。
ALTER TABLE staff_payroll_statements
    ADD COLUMN IF NOT EXISTS leave_deduction_cent BIGINT NOT NULL DEFAULT 0;
