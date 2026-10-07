-- 文件版本：V0.01
-- XM-M002-J（操作员 2026-09-25 答复：单次授权只在业务成功并返回结果时才算用掉；同一请求编号重放应返回第一次的成功结果、
-- 不重复扣减、不重复执行）：
-- ① 消耗日志记下用掉单次来源的那次请求：幂等键 + 请求摘要（方法、路径与查询串的 SHA-256，十六进制 64 位）；
-- ② 该请求成功完成的幂等记录记下消耗人与请求摘要。
-- 同一操作人以同一幂等键重放同一请求、且二者都对得上时，权限核验按重放放行、不再消耗，由幂等层返回第一次的结果。
-- 只加可空列，不回填：存量消耗没有请求信息，其重放照旧拒绝。消耗日志仍只追加（V140 触发器不变）。
ALTER TABLE permission_template_usage_logs
    ADD COLUMN idempotency_key TEXT,
    ADD COLUMN request_digest VARCHAR(64);

CREATE INDEX idx_permission_template_usage_logs_request
    ON permission_template_usage_logs (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE idempotency_records
    ADD COLUMN single_use_actor_id VARCHAR(64),
    ADD COLUMN single_use_request_digest VARCHAR(64);

CREATE INDEX idx_idempotency_records_single_use_request
    ON idempotency_records (idempotency_key, single_use_actor_id)
    WHERE single_use_actor_id IS NOT NULL;
