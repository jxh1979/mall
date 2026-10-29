-- 文件版本：V0.01
-- 第十八批 I 段第 77 条 77-R3 商户服务工单处理时限矩阵。BSM:1385「响应/处理时限按「工单类型×优先级」矩阵后台配置，超时自动升级通知主管」
-- 「处理时限对商户公开；商户服务工单不设首次响应环节」；ENUM:264「超过时限自动升级并标记」。
-- 只有处理时限、没有响应时限；不预置出厂值（HTML 只写「后台配置」）：未配置的组合建单时没有截止时间、不升级。
-- 类型与优先级闭集与 V022 merchant_service_work_orders 一致。

CREATE TABLE IF NOT EXISTS merchant_service_work_order_policies (
    work_order_type VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    handle_minutes INTEGER NOT NULL,
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (work_order_type, priority),
    CONSTRAINT chk_merchant_service_work_order_policy_type CHECK (work_order_type IN ('repair', 'complaint', 'rectification', 'service')),
    CONSTRAINT chk_merchant_service_work_order_policy_priority CHECK (priority IN ('low', 'medium', 'high', 'critical')),
    CONSTRAINT chk_merchant_service_work_order_policy_handle_minutes CHECK (handle_minutes > 0)
);

COMMENT ON TABLE merchant_service_work_order_policies IS '商户服务工单处理时限（工单类型×优先级，分钟）；建单时截止时间＝建单时刻＋处理时限，未配置的组合不设截止时间（第 77 条 77-R3）';
