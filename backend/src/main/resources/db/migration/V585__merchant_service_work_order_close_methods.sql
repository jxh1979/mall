-- 文件版本：V0.01
-- V585（第十八批 L 段第 102 条 102-R3；第十八批裁决 26，2026-11-01 操作员选「同物业，按后台配置」：「在商户工单的处理时限配置里，
-- 给报修、其他服务请求各选一种关单方式：「处理人完成即结束」「员工验收后关闭」或「须商户确认」」）。
-- BSM:1385「验收按工单类型分别配置——保洁等无争议类由处理人完成即结束；……商户投诉须由商户确认投诉已解决」：保洁、投诉照 HTML 写死，不入本表。
-- 报修、其他服务请求每类一种关单方式；处理时限表 merchant_service_work_order_policies（V577）按类型×优先级一行、不预置行，放不下按类型一种的出厂值，
-- 故同前缀另建本表，在后台 ADM-MER-001「处理时限」小节里选。
-- 出厂值照物业工单出厂配置同类型的验收方式（V166 facility_work_order_policy.acceptance_by：报修 supervisor 主管验收→员工验收后关闭；其他 other handler→处理人完成即结束）。
-- 码：handler（处理人完成即结束）、staff_acceptance（员工验收后关闭）、merchant_confirm（须商户确认）。

CREATE TABLE IF NOT EXISTS merchant_service_work_order_close_methods (
    work_order_type VARCHAR(32) PRIMARY KEY,
    close_method VARCHAR(32) NOT NULL,
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_merchant_service_work_order_close_method_type CHECK (work_order_type IN ('repair', 'service')),
    CONSTRAINT chk_merchant_service_work_order_close_method CHECK (close_method IN ('handler', 'staff_acceptance', 'merchant_confirm'))
);

INSERT INTO merchant_service_work_order_close_methods (work_order_type, close_method) VALUES
    ('repair', 'staff_acceptance'),
    ('service', 'handler')
ON CONFLICT (work_order_type) DO NOTHING;

COMMENT ON TABLE merchant_service_work_order_close_methods IS '商户服务工单关单方式（报修、其他服务请求各一种；保洁、投诉照 BSM:1385 写死不入表）（第 102 条 102-R3，裁决 26）';
