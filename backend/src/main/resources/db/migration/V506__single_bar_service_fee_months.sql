-- 文件版本：V0.01
-- V506：FS-013 单一酒吧门票综合服务费按计费月汇总封顶、单独挂应收、整张退款成功回原计费月重算。
-- 依据：BSM L755 / ENUM L57「单一酒吧门票按同一物理项目、同一酒吧的计费月汇总各票应计服务费，再按该月原锁定合同约定的
--   月度封顶确定当月应收总额……计费基数一律是该张票订单上保存的最终选中价（客人实付价）……单一酒吧门票实际退款成功时，
--   即按退票后仍应计费的各票重算原计费月服务费总额……只登记尚未被既有记录覆盖的差额……跨月退款仍重算原计费月，在当前账期
--   生成关联原计费月的调整，不改变当前计费月的封顶额度……单店门票综合服务费按前述计费月汇总后的应收总额另行收取」；
--   操作员裁决 2026-10-19 第 3 条「售出月，只退款剔除」：按客人付款成功（售出）所在月计入；之后只有整张退款成功才从原月剔除重算；
--   到期没用的票照常计费。
-- 计费月：项目 × 酒吧 × 付款成功所在月一行；该月首张票写入时锁定封顶（cap_cent 空＝合同未设封顶），之后改配置不影响；
--   月结过账时按 min(Σ未退票逐票费, 封顶) 单独挂酒吧应收（ar_ap_item_id，source_type=single_bar_service_fee）。
CREATE TABLE single_bar_service_fee_months (
    id              varchar(64)  PRIMARY KEY,
    project_id      varchar(64),
    bar_merchant_id varchar(64)  NOT NULL,
    billing_month   date         NOT NULL,
    cap_cent        bigint       CHECK (cap_cent >= 0),
    ar_ap_item_id   varchar(128) REFERENCES ar_ap_items(id),
    created_at      timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_sbf_months_project_month ON single_bar_service_fee_months(project_id, billing_month);

-- 逐票快照：出票时锁定计费基数（订单实付按张均分）、费率或固定额与规则 id、逐票应计费（未封顶）；
--   整张退款成功只填 refund_id / refunded_at 标记剔除，不改原锁定值。
CREATE TABLE single_bar_service_fee_tickets (
    parent_ticket_id  varchar(64) PRIMARY KEY,
    order_id          varchar(64) NOT NULL,
    month_id          varchar(64) NOT NULL REFERENCES single_bar_service_fee_months(id),
    base_cent         bigint      NOT NULL CHECK (base_cent >= 0),
    config_id         varchar(64) NOT NULL,
    fee_bps           integer,
    fixed_amount_cent bigint,
    fee_cent          bigint      NOT NULL CHECK (fee_cent >= 0),
    refund_id         varchar(64),
    refunded_at       timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_sbf_tickets_month ON single_bar_service_fee_tickets(month_id);

-- 退款差额调整（每笔退款一行，关联原计费月，记当前账期）：重算前已登记总额、重算后总额，调减待收部分（V344 冲正＋重述）
--   与超出部分的应退（source_type=single_bar_service_fee，direction=payable，source_id=退款单 id）。
CREATE TABLE single_bar_service_fee_adjustments (
    refund_id          varchar(64)  PRIMARY KEY,
    month_id           varchar(64)  NOT NULL REFERENCES single_bar_service_fee_months(id),
    accounting_period  varchar(7)   NOT NULL,
    before_cent        bigint       NOT NULL,
    after_cent         bigint       NOT NULL,
    pending_cut_cent   bigint       NOT NULL DEFAULT 0 CHECK (pending_cut_cent >= 0),
    refund_due_cent    bigint       NOT NULL DEFAULT 0 CHECK (refund_due_cent >= 0),
    reversal_item_id   varchar(128) REFERENCES ar_ap_items(id),
    restated_item_id   varchar(128) REFERENCES ar_ap_items(id),
    refund_due_item_id varchar(128) REFERENCES ar_ap_items(id),
    created_at         timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_sbf_adjustments_month ON single_bar_service_fee_adjustments(month_id);

GRANT SELECT, INSERT, UPDATE ON single_bar_service_fee_months, single_bar_service_fee_tickets,
    single_bar_service_fee_adjustments TO complex_app;
