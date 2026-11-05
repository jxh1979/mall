-- 文件版本：V0.01
-- V600（第十八批 T 段第 157 条；157-R2 建表与权限码，157-R3、157-R4 的列随后并入本迁移）：正式挂起订单（只限包厢）。
-- 依据：BSM:1105「状态为已挂起待付款、已结清、取消审批中、已取消、待退款、坏账损耗处理中、已核销结案；禁止无日志删除。」；
--   BSM:1106「收银员可以直接挂起，不设单笔金额和当班累计金额上限；必须填写原因并记录金额、桌台、责任人、操作时间和完整日志。未结清订单阻止交班和日结。」；
--   PGM:699「正式挂单：收银员挂起、经理审批取消；未结清订单阻止交班和日结；全部动作强制留痕。」；
--   第十八批裁决 37 选项原文「散台、卡座下单必须当场付款、不能挂起；包厢关台时没付清就发经理待办，超时按通用时限升级」；
--   DEC-R8-98-07「挂起＝订单级并行状态机，账单六态照旧不动」。
-- 七态英码取开发设计文档已定的码；amount_cent＝挂起时本单未结清账单应付＋小费－实收；responsible_user_id 为空＝门店（本单开台会话此刻
-- 没有主责服务员）；shift_id＝挂起人此刻在本酒吧的开班班次（含加入的同一钱箱），交班闸按它挡。
-- 不建防删触发器：应用不提供删除挂起记录的接口，每次状态变更写审计（禁止无日志删除）。研发期无存量（UD-07）。
CREATE TABLE bar_order_suspensions (
    id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL UNIQUE REFERENCES bar_orders(id),
    bill_id VARCHAR(64) NOT NULL REFERENCES bar_bills(id),
    bar_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64),
    project_id VARCHAR(64),
    table_id VARCHAR(64) NOT NULL REFERENCES bar_tables(id),
    shift_id VARCHAR(64) REFERENCES bar_shifts(id),
    status VARCHAR(32) NOT NULL,
    amount_cent BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    responsible_user_id VARCHAR(64),
    suspended_by VARCHAR(64) NOT NULL,
    suspended_at TIMESTAMPTZ NOT NULL,
    settled_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_order_suspensions_status CHECK (status IN ('hanging_unpaid', 'settled', 'cancel_pending_approval',
        'cancelled', 'pending_refund', 'bad_debt_processing', 'written_off_closed'))
);
CREATE INDEX idx_bar_order_suspensions_bar_status ON bar_order_suspensions(bar_id, status);
CREATE INDEX idx_bar_order_suspensions_shift_status ON bar_order_suspensions(shift_id, status);
COMMENT ON TABLE bar_order_suspensions IS '正式挂起订单（只限包厢，第十八批裁决 37）：订单级并行状态，账单六态不动';

-- 权限码取开发设计文档已定的两个（PGM:699 只有收银员挂起、经理审批取消两个权限点）：bar.bill.hang 收银员挂起，授予已持交班权限
-- bar.shift.close 的角色与权限模板；bar.bill.hang_cancel 经理审批取消与确认坏账，授予已持退菜审批权限 bar.order_item.void_decide 的
-- 角色与权限模板；两者均授平台管理员（照 V552 写法）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('bar.bill.hang', '正式挂起包厢未付款订单（收银员）', 'bar', 'high', TRUE, 'active'),
       ('bar.bill.hang_cancel', '审批取消正式挂起订单、确认坏账（经理）', 'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT DISTINCT role_id, 'bar.bill.hang' FROM role_permissions WHERE permission_code = 'bar.shift.close'
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT DISTINCT role_id, 'bar.bill.hang_cancel' FROM role_permissions WHERE permission_code = 'bar.order_item.void_decide'
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'bar.bill.hang'), ('role_platform_admin', 'bar.bill.hang_cancel')
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO permission_template_permissions (template_id, permission_code)
SELECT DISTINCT template_id, 'bar.bill.hang' FROM permission_template_permissions WHERE permission_code = 'bar.shift.close'
ON CONFLICT DO NOTHING;

INSERT INTO permission_template_permissions (template_id, permission_code)
SELECT DISTINCT template_id, 'bar.bill.hang_cancel' FROM permission_template_permissions WHERE permission_code = 'bar.order_item.void_decide'
ON CONFLICT DO NOTHING;

-- 挂起入口 @GuardedAction(capability = bar.bill.hang) 的跨端全局显式开启行（照 V595；PGM L742 缺行即关）。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_optin_bar.bill.hang', '*', 'bar.bill.hang', 'global', 'global', TRUE,
     '已发布在用能力显式开启（PGM L742 opt-in）', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

-- 157-R3：经理确认坏账与财务核销结案。依据：BSM:1109「经理确认最终无法收款后转为坏账损耗处理中并解除该订单对交班、日结的阻断」；
--   BSM:1110「服务员责任以商品出品时该桌绑定的主责服务员为准……出品时没有主责服务员的记为门店损耗」；BSM:1111「商户核销未收售价……同时建立实际商品成本赔偿记录。
--   财务确认未收售价、库存成本和赔偿记录均完成后，订单转为已核销结案」；BSM:1112「审查通过前只登记」；第十八批裁决 38「收入与出库成本都不进分红净利润」。
-- 转坏账时固化：unpaid_price_cent＝本单未结清账单应付＋小费－实收；inventory_cost_cent＝本单出库流水成本合计；compensation_lines＝逐个未作废已出品明细
-- [{order_item_id, responsible_user_id（空＝门店损耗）, cost_cent}]，只登记、不扣工资绩效提成。bad_debt_confirmed_at 非空即该单不产生销售提成、不进营业额与分红收入。
ALTER TABLE bar_order_suspensions
    ADD COLUMN bad_debt_reason VARCHAR(1000),
    ADD COLUMN bad_debt_confirmed_by VARCHAR(64),
    ADD COLUMN bad_debt_confirmed_at TIMESTAMPTZ,
    ADD COLUMN unpaid_price_cent BIGINT,
    ADD COLUMN inventory_cost_cent BIGINT,
    ADD COLUMN compensation_lines JSONB,
    ADD COLUMN written_off_by VARCHAR(64),
    ADD COLUMN written_off_at TIMESTAMPTZ;

-- 确认坏账入口 @GuardedAction(capability = bar.bill.hang_cancel) 的跨端全局显式开启行（照 V595；PGM L742 缺行即关）。核销结案入口沿用
-- finance.ar_ap.manage，已由 V554 开启。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_optin_bar.bill.hang_cancel', '*', 'bar.bill.hang_cancel', 'global', 'global', TRUE,
     '已发布在用能力显式开启（PGM L742 opt-in）', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

-- 157-R4：高价值商品转坏账仍走原证据与主管复核。依据：BSM:1112「达到既有高价值标准的商品仍执行原证据和审批要求。」；
--   ENUM:219「主管复核强制项：……高价值报损/盘点差异/存酒作废」；PGM:920「高价值酒水报损、盘点差异、存酒作废。」；BSM:1109「在转入该状态前继续阻断」。
-- 本单未作废已出品明细任一项达高价值（全平台统一判定）时，经理确认无法收款须带照片＋视频双证据，不直接转坏账，改建库存高价值复核单
-- （subject_type＝bar_order_bad_debt，subject_id＝酒吧订单）；挂起保持已挂起待付款（继续挡交班、日结、关台），bad_debt_review_id 记待复核的复核单，
-- 复核批准才转坏账（批准后保留），驳回即清空、可重新发起。
ALTER TABLE bar_order_suspensions ADD COLUMN bad_debt_review_id VARCHAR(64) REFERENCES inventory_high_value_reviews(id);
ALTER TABLE inventory_high_value_reviews DROP CONSTRAINT IF EXISTS chk_inv_hv_subject;
ALTER TABLE inventory_high_value_reviews ADD CONSTRAINT chk_inv_hv_subject
    CHECK (subject_type IN ('damage_report', 'stock_variance', 'saved_bottle_void', 'saved_bottle_stock_disposal', 'bar_order_bad_debt'));

-- 157-R7：挂起订单风险看板的审计导出。依据：BSM:1113「不设额度时，风险看板和审计导出必须持续展示挂起金额、账龄、责任人和未结清数量。」；
--   BSM:1840「看板按权限看，但把数据导出带走须单独授权并留痕」。导出记录 export_files.export_type 闭集加 bar_suspended_orders。
ALTER TABLE export_files DROP CONSTRAINT export_files_export_type_check;
ALTER TABLE export_files ADD CONSTRAINT export_files_export_type_check CHECK (export_type IN (
    'analytics_monthly_report', 'finance_ar_ap_ledger', 'audit_operation_events', 'settlement_invoice_items', 'bar_suspended_orders'));
