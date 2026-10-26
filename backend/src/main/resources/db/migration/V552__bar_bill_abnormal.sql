-- 文件版本：V0.01
-- V552（第十六批第 16 条，操作员裁决补充第 2 条）：酒吧账单「异常」。ENUM L101「账单状态 待结、部分支付、已结、部分退款、已退款、异常」；
-- ENUM L221「任何单据进入“异常”后，必须由主管在系统内选择“更正后恢复”或“作废并冲正”两种方式之一处置，填写原因并留痕；
-- 恢复后回到进入异常之前的那个状态」。操作员选「系统自动＋员工登记（推荐）」：客人对账单提出争议时员工登记置异常，之后都走主管二选一。
-- 系统自动那一半（收款金额与账单不符、支付结果未知）：现行酒吧账单收款全由员工登记，没有渠道回调或查单结果可判，本迁移不涉及。
-- ① abnormal_prior_status：进入异常前的 payment_status（恢复时回到它）；只在异常期间有值。
-- ② bar.bill.anomaly.resolve：主管处置权限（高风险，同 V524 票务核销异常处置的做法），授予已持有反结权限 bar.bill.reopen 的角色与权限模板及平台管理员。
ALTER TABLE bar_bills ADD COLUMN abnormal_prior_status VARCHAR(32);
ALTER TABLE bar_bills ADD CONSTRAINT chk_bar_bills_abnormal_prior
    CHECK ((payment_status = 'abnormal') = (abnormal_prior_status IS NOT NULL));

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('bar.bill.anomaly.resolve', '处置酒吧账单异常（更正后恢复或作废并冲正）', 'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT DISTINCT role_id, 'bar.bill.anomaly.resolve'
  FROM role_permissions
 WHERE permission_code = 'bar.bill.reopen'
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'bar.bill.anomaly.resolve')
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO permission_template_permissions (template_id, permission_code)
SELECT DISTINCT template_id, 'bar.bill.anomaly.resolve'
  FROM permission_template_permissions
 WHERE permission_code = 'bar.bill.reopen'
ON CONFLICT DO NOTHING;
