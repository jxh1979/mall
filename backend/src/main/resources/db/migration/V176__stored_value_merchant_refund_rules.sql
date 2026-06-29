-- B1d⑤ phase3 商户级卡退款规则：发卡商户自设「是否可退 + 退款比例」。
-- 仅对商户级卡（merchant_id 非空）生效；综合体通用卡不受此表约束（默认全额可退，沿用既有行为）。
-- refund_allowed=false → 该商户的商户卡一律拒退；refund_ratio_bps=可退回客户的比例（基点，0..10000），
-- 余下部分为商户留存（手续费/违约金）。无配置=默认允许、100% 可退。
CREATE TABLE IF NOT EXISTS stored_value_merchant_refund_rules (
    merchant_id VARCHAR(64) PRIMARY KEY,
    refund_allowed BOOLEAN NOT NULL DEFAULT true,
    refund_ratio_bps INTEGER NOT NULL DEFAULT 10000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_svmrr_ratio CHECK (refund_ratio_bps BETWEEN 0 AND 10000)
);
