-- 文件版本：V0.01
-- V587（第十八批 M 段第 117 条 117-R1、117-R2、117-R5）。
-- BSM:1380「用料必须从指定库房出库并按当前平均进货价计成本，同时在工单上标明费用承担方：公区设施与楼宇主体设备的用料由物业承担；
-- 商户自有设施、以及因商户责任导致损坏的用料由该商户承担，并自动生成一笔应收进入其费用账单。承担方有争议的，工单挂起转上级判定，
-- 判定结果与依据留痕后才能结案。」
-- ① work_order_cost_bearers：设施工单与商户服务工单共用的费用承担方，按工单标一个（不逐行）；商户承担记商户，结案生成的一笔应收回写 ar_ap_item_id。
-- ② work_order_bearer_disputes：承担方争议（设施工单与商户服务工单共用，按 business_type 区分），走统一审批一级上级判定；判定结果（approved
--    按主张改定、rejected 维持原承担方）、判定人、判定时间与判定依据（上级审批意见）留痕；同一工单同时只有一条待判定。设施工单提出即挂起；
--    商户服务工单不加状态，停在原状态转上级判定，判定前不能转已处理待关闭、不能关闭（第十八批裁决 29）。
-- ③ 用料出库流水 inventory_stock_movements 以 business_type/business_id 挂工单，补按工单查的索引；成本取出库流水移动加权均价（V296），不另算。

CREATE TABLE work_order_cost_bearers (
    business_type VARCHAR(40) NOT NULL,
    work_order_id VARCHAR(64) NOT NULL,
    cost_bearer VARCHAR(16) NOT NULL,
    merchant_id VARCHAR(64) REFERENCES merchants(id),
    ar_ap_item_id VARCHAR(64) REFERENCES ar_ap_items(id),
    marked_by VARCHAR(64) NOT NULL,
    marked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (business_type, work_order_id),
    CONSTRAINT chk_wocb_business_type CHECK (business_type IN ('facility_work_order', 'merchant_service_work_order')),
    CONSTRAINT chk_wocb_cost_bearer CHECK (cost_bearer IN ('property', 'merchant')),
    CONSTRAINT chk_wocb_merchant CHECK ((cost_bearer = 'merchant') = (merchant_id IS NOT NULL))
);

CREATE TABLE work_order_bearer_disputes (
    id VARCHAR(64) PRIMARY KEY,
    business_type VARCHAR(40) NOT NULL,
    work_order_id VARCHAR(64) NOT NULL,
    review_id VARCHAR(64) NOT NULL,
    from_cost_bearer VARCHAR(16) NOT NULL,
    from_merchant_id VARCHAR(64),
    claimed_cost_bearer VARCHAR(16) NOT NULL,
    claimed_merchant_id VARCHAR(64),
    reason TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    raised_by VARCHAR(64) NOT NULL,
    raised_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ruled_by VARCHAR(64),
    ruled_at TIMESTAMPTZ,
    ruling_basis TEXT,
    CONSTRAINT chk_wobd_business_type CHECK (business_type IN ('facility_work_order', 'merchant_service_work_order')),
    CONSTRAINT chk_wobd_claimed CHECK (claimed_cost_bearer IN ('property', 'merchant')),
    CONSTRAINT chk_wobd_status CHECK (status IN ('pending', 'approved', 'rejected'))
);

CREATE UNIQUE INDEX uq_wobd_pending ON work_order_bearer_disputes (business_type, work_order_id) WHERE status = 'pending';
CREATE INDEX idx_wobd_review ON work_order_bearer_disputes (review_id);
CREATE INDEX IF NOT EXISTS idx_inventory_stock_movements_business ON inventory_stock_movements (business_type, business_id);

COMMENT ON TABLE work_order_cost_bearers IS '工单用料费用承担方（设施工单、商户服务工单共用；BSM:1380；第十八批 M 段第 117 条）';
COMMENT ON TABLE work_order_bearer_disputes IS '工单费用承担方争议与上级判定留痕（设施工单、商户服务工单共用；BSM:1380；第十八批 M 段第 117 条 117-R2、117-R5，裁决 29）';
