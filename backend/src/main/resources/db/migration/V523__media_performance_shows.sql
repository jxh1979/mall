-- 文件版本：V0.01
-- V523：TK-29 传媒逐场演出合同记录、实际履约登记与确认、三种结算（第十批）。
-- 依据：BSM L1144「酒吧门票上的演出加价部分，随酒吧门票由娱乐平台公司售票收款后分给传媒公司」；
--   BSM L1145「按演出合同可以同时存在三种情形……①涉及门票的……按演出合同逐场约定的比例或金额分给传媒公司，并在后台配置页面逐场填写、
--   随合同固化版本；②签订固定费用的，由该酒吧向传媒公司支付合同约定的固定费用；③约定酒水提成的，按合同约定的比例分给传媒公司，
--   计提基数不含通票赠饮和酒吧门票赠饮的酒水……后台提供按演出时段、按当场全店等多种基数模板供选择；每场演出适用哪几种情形、
--   各自的数值与合同版本，必须逐场记录……由传媒经办人在现有逐场合同记录中登记实际履约时间、已完成和未完成内容及依据；到酒吧演出的，
--   由酒吧指定现场责任人确认；传媒自办场次，由传媒指定主管确认……尚未取得确认、实际履约事实存在争议或合同未约定相应处理时，
--   相关结算保持待处理……传媒自办场次……不因本条确认新增酒吧应付；已形成结算需要调整时，关联原记录……不覆盖原结果」；
--   操作员裁决（2026-10-20）「逐场填每张加价×已核销张数」：逐场合同记录里填「本场每张门票加价金额」并指定适用的酒吧门票票种，
--   基数＝该票种当场已核销入场张数×每张加价；未核销部分不分传媒。

-- 1) 逐场演出合同记录：三种情形各自可空（＝只有其中一种或两种）；locked 后数值不可改，改动新建版本行（同 show_group_id、contract_version+1），
--    新版本锁定后旧行置 superseded、旧行保留；结算只读 locked 行。
CREATE TABLE IF NOT EXISTS media_performance_shows (
    id VARCHAR(64) PRIMARY KEY,
    show_group_id VARCHAR(64) NOT NULL,
    contract_version INTEGER NOT NULL DEFAULT 1,
    project_id VARCHAR(64) NOT NULL,
    bar_merchant_id VARCHAR(64),
    media_merchant_id VARCHAR(64) NOT NULL,
    show_kind VARCHAR(16) NOT NULL,
    show_date DATE NOT NULL,
    planned_start_at TIMESTAMPTZ,
    planned_end_at TIMESTAMPTZ,
    contract_ref VARCHAR(160),
    ticket_share_mode VARCHAR(16) NOT NULL DEFAULT 'none',
    ticket_share_bps INTEGER,
    ticket_share_amount_cent BIGINT,
    ticket_surcharge_per_ticket_cent BIGINT,
    ticket_type_id VARCHAR(64),
    fixed_fee_cent BIGINT,
    drink_commission_bps INTEGER,
    drink_base_template VARCHAR(32),
    status VARCHAR(16) NOT NULL DEFAULT 'draft',
    locked_at TIMESTAMPTZ,
    locked_by VARCHAR(128),
    created_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_media_shows_group_version UNIQUE (show_group_id, contract_version),
    CONSTRAINT chk_media_shows_kind CHECK (show_kind IN ('bar_show', 'self_hosted')),
    CONSTRAINT chk_media_shows_status CHECK (status IN ('draft', 'locked', 'superseded')),
    CONSTRAINT chk_media_shows_ticket_mode CHECK (ticket_share_mode IN ('none', 'ratio', 'fixed')),
    CONSTRAINT chk_media_shows_ticket_values CHECK (
        (ticket_share_mode = 'none' AND ticket_share_bps IS NULL AND ticket_share_amount_cent IS NULL
            AND ticket_surcharge_per_ticket_cent IS NULL AND ticket_type_id IS NULL)
        OR (ticket_share_mode = 'ratio' AND ticket_share_bps BETWEEN 1 AND 10000 AND ticket_share_amount_cent IS NULL
            AND ticket_surcharge_per_ticket_cent > 0 AND ticket_type_id IS NOT NULL)
        OR (ticket_share_mode = 'fixed' AND ticket_share_amount_cent > 0 AND ticket_share_bps IS NULL
            AND ticket_surcharge_per_ticket_cent > 0 AND ticket_type_id IS NOT NULL)),
    CONSTRAINT chk_media_shows_fixed_fee CHECK (fixed_fee_cent IS NULL OR fixed_fee_cent > 0),
    CONSTRAINT chk_media_shows_drink CHECK (
        (drink_commission_bps IS NULL AND drink_base_template IS NULL)
        OR (drink_commission_bps BETWEEN 1 AND 10000 AND drink_base_template IN ('show_window', 'whole_store_session'))),
    -- 自办场次不因本条新增酒吧应付：无酒吧、无固定费、无酒水提成、无酒吧门票加价分成（仍按原售票收款与收入归属规则办理）。
    CONSTRAINT chk_media_shows_self_hosted CHECK (
        (show_kind = 'bar_show' AND bar_merchant_id IS NOT NULL)
        OR (show_kind = 'self_hosted' AND bar_merchant_id IS NULL AND fixed_fee_cent IS NULL
            AND drink_commission_bps IS NULL AND ticket_share_mode = 'none')),
    CONSTRAINT chk_media_shows_locked CHECK (status = 'draft' OR (locked_at IS NOT NULL AND locked_by IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_media_shows_project_date ON media_performance_shows (project_id, show_date);
CREATE INDEX IF NOT EXISTS idx_media_shows_ticket_type_date ON media_performance_shows (ticket_type_id, show_date)
    WHERE ticket_type_id IS NOT NULL;

-- 2) 实际履约登记（传媒经办）与确认 / 争议 / 裁决：一场一条记录。
CREATE TABLE IF NOT EXISTS media_performance_fulfillments (
    id VARCHAR(64) PRIMARY KEY,
    show_id VARCHAR(64) NOT NULL REFERENCES media_performance_shows(id),
    actual_start_at TIMESTAMPTZ NOT NULL,
    actual_end_at TIMESTAMPTZ NOT NULL,
    completed_content TEXT NOT NULL,
    uncompleted_content TEXT,
    basis TEXT NOT NULL,
    recorded_by VARCHAR(128) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status VARCHAR(16) NOT NULL DEFAULT 'recorded',
    confirmed_by VARCHAR(128),
    confirmed_at TIMESTAMPTZ,
    dispute_reason TEXT,
    disputed_by VARCHAR(128),
    disputed_at TIMESTAMPTZ,
    resolution_note TEXT,
    resolved_by VARCHAR(128),
    resolved_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_media_fulfillments_show UNIQUE (show_id),
    CONSTRAINT chk_media_fulfillments_status CHECK (status IN ('recorded', 'confirmed', 'disputed', 'resolved')),
    CONSTRAINT chk_media_fulfillments_window CHECK (actual_end_at > actual_start_at),
    CONSTRAINT chk_media_fulfillments_dispute CHECK (status <> 'disputed' OR dispute_reason IS NOT NULL),
    CONSTRAINT chk_media_fulfillments_resolved CHECK (status <> 'resolved' OR (resolution_note IS NOT NULL AND resolved_by IS NOT NULL))
);

-- 3) 三种结算：共同引用已确认事实、分别按锁定合同出账；未确认/争议/未约定保持 pending；调整新建行回指原行，原行只改状态不改金额。
CREATE TABLE IF NOT EXISTS media_performance_settlements (
    id VARCHAR(64) PRIMARY KEY,
    show_id VARCHAR(64) NOT NULL REFERENCES media_performance_shows(id),
    fulfillment_id VARCHAR(64) REFERENCES media_performance_fulfillments(id),
    fee_kind VARCHAR(32) NOT NULL,
    base_amount_cent BIGINT NOT NULL DEFAULT 0,
    rate_bps INTEGER,
    fixed_amount_cent BIGINT,
    amount_cent BIGINT NOT NULL DEFAULT 0,
    payer_merchant_id VARCHAR(64),
    payer_entity_id VARCHAR(64),
    payee_entity_id VARCHAR(64),
    status VARCHAR(16) NOT NULL,
    pending_reason VARCHAR(32),
    ar_ap_item_id VARCHAR(64),
    adjusts_settlement_id VARCHAR(64) REFERENCES media_performance_settlements(id),
    adjust_reason TEXT,
    created_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_media_settlements_kind CHECK (fee_kind IN ('ticket_share', 'fixed_fee', 'drink_commission')),
    CONSTRAINT chk_media_settlements_status CHECK (status IN ('pending', 'posted', 'adjusted')),
    CONSTRAINT chk_media_settlements_pending CHECK (
        (status = 'pending' AND pending_reason IN ('unconfirmed', 'disputed', 'contract_silent'))
        OR (status <> 'pending' AND pending_reason IS NULL)),
    CONSTRAINT chk_media_settlements_amount CHECK (amount_cent >= 0 AND base_amount_cent >= 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_media_settlements_show_kind_live
    ON media_performance_settlements (show_id, fee_kind) WHERE status <> 'adjusted';

-- 4) 权限码（范式同 V513）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('media.performance.view', '查看逐场演出合同与履约结算', 'settlement', 'low', FALSE, 'active'),
    ('media.performance.contract.manage', '维护逐场演出合同记录与锁定版本', 'settlement', 'high', TRUE, 'active'),
    ('media.performance.fulfillment.record', '传媒经办登记实际履约', 'settlement', 'medium', FALSE, 'active'),
    ('media.performance.fulfillment.confirm', '现场责任人/传媒主管确认或提出履约争议', 'settlement', 'medium', FALSE, 'active'),
    ('media.performance.dispute.resolve', '主管裁决履约争议', 'settlement', 'high', TRUE, 'active'),
    ('media.performance.settle', '按锁定合同结算演出三项费用与调整', 'settlement', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES
    ('role_platform_admin', 'media.performance.view'),
    ('role_platform_admin', 'media.performance.contract.manage'),
    ('role_platform_admin', 'media.performance.fulfillment.record'),
    ('role_platform_admin', 'media.performance.fulfillment.confirm'),
    ('role_platform_admin', 'media.performance.dispute.resolve'),
    ('role_platform_admin', 'media.performance.settle')
ON CONFLICT (role_id, permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON media_performance_shows TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON media_performance_fulfillments TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON media_performance_settlements TO complex_app';
    END IF;
END $$;
