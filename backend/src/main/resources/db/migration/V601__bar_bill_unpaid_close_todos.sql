-- 文件版本：V0.01
-- V601（第十八批 T 段第 157 条 157-R5）：包厢关台时没付清的经理待办与超时升级。
-- 依据：第十八批裁决 37 选项原文「散台、卡座下单必须当场付款、不能挂起；包厢关台时没付清就发经理待办，超时按通用时限升级」；
--   BSM:1058「账单未结清（含挂起未结清）不得关台」；BSM:1109「经理未及时确认时，任务升级给店长或夜班值班经理，在转入该状态前继续阻断」；
--   PGM:529「涉钱审批出厂默认 1 个工作日……不得默认通过，也不得默认拒绝」。
-- 每张账单一行：包厢关台首次被拦时登记（notified_at；due_at＝涉钱审批通用时限 1 个工作日后），到期仍未付清且未转坏账的升级一次（escalated_at）。
-- 不建外键：在关台被拦的独立事务里写，关台事务此刻正锁着开台会话。不在 supervisor_review_policies 插行（该表按动作类型被准入闸读取）。
-- 研发期无存量（UD-07）。
CREATE TABLE bar_bill_unpaid_close_todos (
    bill_id VARCHAR(64) PRIMARY KEY,
    bar_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64),
    project_id VARCHAR(64),
    session_id VARCHAR(64) NOT NULL,
    notified_at TIMESTAMPTZ NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    escalated_at TIMESTAMPTZ
);
CREATE INDEX idx_bar_bill_unpaid_close_todos_due ON bar_bill_unpaid_close_todos(due_at) WHERE escalated_at IS NULL;
COMMENT ON TABLE bar_bill_unpaid_close_todos IS '包厢关台时未付清账单的经理待办（第十八批裁决 37）：到期未处理升级给店长或夜班值班经理';

-- 店长接收挂起订单升级待办并确认坏账（BSM:1109 升级给店长）；范式同 V559 第 5 段：只授平台管理员，由运营分配给店长角色；
-- 风险与审批要求同 bar.bill.hang_cancel（V600）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('bar.bill.hang_escalation', '接收挂起订单升级待办并确认坏账（店长）', 'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'bar.bill.hang_escalation')
ON CONFLICT (role_id, permission_code) DO NOTHING;
