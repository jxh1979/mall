-- AUD-077 门票权益转赠状态机(方案3)。依赖 V285 父票实例 + issued_entitlements.parent_ticket_id/slot_index。
-- 用户口径:测试环境上线前全删,不做任何存量回填;本迁移只建表/加权限,无回填 SQL。
-- 转赠单位 = 人员权益组(slot) = (parent_ticket_id, slot_index)。单人通票/多次票=1 个 slot;多人票=N 个 slot。

-- 1) 人员权益组持有人。方案1 的 current_holder_customer_id 落在父票级(整票粒度),
--    转赠单位是 slot,多人票必须按 slot 记持有人 → 独立小表,不动 issued_entitlements 巨型行。
--    出票时每个 slot 写一行(holder=购票人);转赠领取成功改受赠人。取码/我的票授权从"订单归属"改"slot 持有人"。
CREATE TABLE ticket_slot_holders (
    parent_ticket_id    VARCHAR(64) NOT NULL REFERENCES ticket_instances(id),
    slot_index          INTEGER     NOT NULL,
    holder_customer_id  VARCHAR(64) NOT NULL REFERENCES customers(id),
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (parent_ticket_id, slot_index)
);
CREATE INDEX idx_ticket_slot_holders_holder ON ticket_slot_holders (holder_customer_id);

-- 2) 转赠记录表(本身就是完整流水,不另建历史表)。
CREATE TABLE ticket_transfers (
    id                    VARCHAR(64) PRIMARY KEY,
    parent_ticket_id      VARCHAR(64) NOT NULL REFERENCES ticket_instances(id),
    slot_index            INTEGER     NOT NULL,                 -- 人员权益组槽位(1..N)
    initiator_customer_id VARCHAR(64) NOT NULL REFERENCES customers(id),  -- 发起时的持有人
    channel               VARCHAR(16) NOT NULL CHECK (channel IN ('phone','link')),  -- 微信/二维码同为 link
    receiver_phone        TEXT,                                 -- phone 渠道:密文(复用 PiiCipher)
    receiver_phone_hash   VARCHAR(128),                         -- phone 渠道:盲索引(复用 BlindIndex)
    receiver_phone_masked VARCHAR(32),                          -- phone 渠道:展示用掩码
    claim_code            VARCHAR(64) UNIQUE,                   -- link 渠道:一次性领取码(高熵随机)
    receiver_customer_id  VARCHAR(64) REFERENCES customers(id), -- 领取成功后回填
    status                VARCHAR(16) NOT NULL CHECK (status IN ('pending','claimed','revoked','voided')),
    void_reason           VARCHAR(64),                          -- ticket_expired / ticket_refunded 等
    created_at            TIMESTAMPTZ NOT NULL,
    claimed_at            TIMESTAMPTZ,
    revoked_at            TIMESTAMPTZ,
    voided_at             TIMESTAMPTZ,
    updated_at            TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_transfer_channel_target CHECK (
        (channel = 'phone' AND receiver_phone_hash IS NOT NULL) OR
        (channel = 'link'  AND claim_code IS NOT NULL))
);
-- 冻结语义的物理保证:同一人员权益组同时最多一笔在途转赠(撞此索引=409 已在途)。
CREATE UNIQUE INDEX uq_ticket_transfers_pending
    ON ticket_transfers (parent_ticket_id, slot_index) WHERE status = 'pending';
-- 受赠人"待领取列表"(手机号盲索引匹配)。
CREATE INDEX idx_ticket_transfers_recv_phone
    ON ticket_transfers (receiver_phone_hash) WHERE status = 'pending';
CREATE INDEX idx_ticket_transfers_initiator ON ticket_transfers (initiator_customer_id);

-- 3) 客服只读查询转赠记录权限码(纠纷排查,首期只读不撤回)。平台管理员直授(V206/V279 范式)。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('ticket.transfer.read', 'View ticket transfer records', 'ticket', 'low', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'ticket.transfer.read'
ON CONFLICT (role_id, permission_code) DO NOTHING;
