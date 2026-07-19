-- V313 券包会员间转赠（2026-07-19 用户拍板）。范式对齐 V287 门票转赠 / 存酒转让：
--   出让人发起冻结 → 接收方绑定（手机号盲索引 or 高熵一次性领取码）→ 领取 CAS 改归属 / 出让人撤回，
--   双花窗口 = 同一券实例同时最多一笔在途转赠（部分唯一索引物理保证）。
-- 🔴 业务口径提醒（务必人工确认）：业务 HTML 现措辞「积分不支持会员间转赠」（积分转赠已随 V172 下线）。
--   本迁移针对的是「积分兑换出的券实例」（统一台账，此时积分已消耗），与「积分本身不可转」不直接冲突，
--   但仍属业务口径变更，须经用户确认并同步业务 HTML 增补（子代理不改业务 HTML，仅出清单）。
--   仅建表/授权，无存量回填（测试环境上线前无历史转赠）。
CREATE TABLE IF NOT EXISTS voucher_transfers (
    id                    varchar(64) PRIMARY KEY,
    voucher_instance_id   varchar(64) NOT NULL REFERENCES voucher_instances(id),
    initiator_customer_id varchar(64) NOT NULL,
    channel               varchar(16) NOT NULL,               -- phone（手机号）| link（二维码/微信同为 link）
    receiver_phone        text,                                -- phone 渠道：密文（复用 PiiCipher）
    receiver_phone_hash   varchar(128),                        -- phone 渠道：盲索引（复用 BlindIndex）
    receiver_phone_masked varchar(32),                         -- phone 渠道：展示用掩码
    claim_code            varchar(64) UNIQUE,                  -- link 渠道：一次性高熵领取码
    receiver_customer_id  varchar(64),                         -- 领取成功后回填
    status                varchar(16) NOT NULL DEFAULT 'pending',
    void_reason           varchar(64),                         -- voucher_redeemed / voucher_revoked / voucher_expired 等
    created_at            timestamptz NOT NULL DEFAULT now(),
    claimed_at            timestamptz,
    revoked_at            timestamptz,
    voided_at             timestamptz,
    updated_at            timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_vt_channel CHECK (channel IN ('phone', 'link')),
    CONSTRAINT chk_vt_status CHECK (status IN ('pending', 'claimed', 'revoked', 'voided')),
    CONSTRAINT chk_vt_channel_target CHECK (
        (channel = 'phone' AND receiver_phone_hash IS NOT NULL) OR
        (channel = 'link'  AND claim_code IS NOT NULL))
);
-- 双花窗口物理保证：同一券实例同时最多一笔在途转赠（撞此索引 = 409 已在途，须先撤回）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_voucher_transfers_pending
    ON voucher_transfers (voucher_instance_id) WHERE status = 'pending';
-- 受赠人「待领取列表」（手机号盲索引匹配 pending）。
CREATE INDEX IF NOT EXISTS idx_voucher_transfers_recv_phone
    ON voucher_transfers (receiver_phone_hash) WHERE status = 'pending';
CREATE INDEX IF NOT EXISTS idx_voucher_transfers_initiator ON voucher_transfers (initiator_customer_id);

-- 生产运行时授权（V301 范式；上轮 V296/V297 曾漏授直接生产 500，硬性检查项）：
--   新表显式 GRANT complex_app；转赠状态机有 CAS UPDATE，授 SELECT/INSERT/UPDATE（不授 DELETE）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON voucher_transfers TO complex_app';
    END IF;
END $$;
