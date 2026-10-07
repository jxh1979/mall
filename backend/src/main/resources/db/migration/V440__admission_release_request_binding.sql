-- 文件版本：V0.01
-- XM-ONESHOT-RETURN B01（治理地图第 303 行「全局原则 · 同时操作以先成功为准」：同一个操作被重复提交，系统只认一次；
-- 操作员 2026-09-25 确认的追加句：只能用一次的动作，只有这次操作成功完成、返回结果确认后才算用掉；
-- 同一次操作重复提交时，返回第一次的结果）：
-- 准入闸逐笔放行时，在复核单上记下用掉它的那次请求——幂等键 + 请求摘要（方法、路径与查询串的 SHA-256，十六进制 64 位）。
-- 以同一幂等键重放同一请求、且放行人那次请求的幂等记录已成功完成时，准入闸按重放放行，不再用掉放行单、
-- 不再新建待审复核单；能不能拿到第一次的结果由幂等层按提交范围判（同范围返回第一次的结果，其他范围拒绝）。
-- 只加可空列，不回填：存量放行没有请求信息，其重放照旧拦截。复核单仍只禁删除（V148 触发器不变）。
ALTER TABLE supervisor_review_decisions
    ADD COLUMN released_idempotency_key TEXT,
    ADD COLUMN released_request_digest VARCHAR(64);

CREATE INDEX idx_supervisor_review_decisions_released_request
    ON supervisor_review_decisions (released_idempotency_key, released_request_digest)
    WHERE released_idempotency_key IS NOT NULL;
