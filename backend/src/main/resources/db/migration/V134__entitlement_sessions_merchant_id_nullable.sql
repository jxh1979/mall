-- P1 修复（用户裁定：迁移放宽 nullable）：entitlement_sessions.merchant_id 由 NOT NULL 放宽为可空。
-- 背景：V129 将 merchant_id 定为 NOT NULL（direct scope 自动归类），但 initiateEntitlementSession 在平台视角
-- （currentMerchantScope() 为空，orElse(null)）发起权益会话时插入 merchant_id=NULL，违反 NOT NULL 约束 → 500。
-- 语义：merchant_id 为空 = 平台级发起、无归属商户；非空 = 归属商户（direct scope 行级隔离仍生效）。
-- 列仍保留，domain-ownership 仍按 merchant_id 列归类 direct scope，仅允许平台级行为空。
ALTER TABLE entitlement_sessions
    ALTER COLUMN merchant_id DROP NOT NULL;
