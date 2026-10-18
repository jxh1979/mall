-- 文件版本：V0.01；ROOT-02，企微会话永久保存及逐次授权清理。
-- 不关联消息外键，消息删除后预览与执行证据仍永久保留；原文仅保留摘要指纹。
CREATE TABLE wecom_cleanup_previews (
    id VARCHAR(64) PRIMARY KEY,
    created_by VARCHAR(64) NOT NULL,
    candidates JSONB NOT NULL CHECK (jsonb_typeof(candidates) = 'array'),
    scope_digest VARCHAR(64) NOT NULL,
    message_count INTEGER NOT NULL CHECK (message_count > 0 AND message_count = jsonb_array_length(candidates)),
    risk_hit_count INTEGER NOT NULL CHECK (risk_hit_count >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    executed_at TIMESTAMPTZ,
    executed_by VARCHAR(64),
    key_fingerprint VARCHAR(64),
    CONSTRAINT chk_wecom_cleanup_execution CHECK (
        (executed_at IS NULL AND executed_by IS NULL AND key_fingerprint IS NULL)
        OR (executed_at IS NOT NULL AND executed_by IS NOT NULL AND key_fingerprint IS NOT NULL)
    )
);
CREATE INDEX idx_wecom_cleanup_created ON wecom_cleanup_previews (created_at DESC, id);
COMMENT ON TABLE wecom_cleanup_previews IS '企微手动清理预览及执行证据，永久保存，不属于任何清理范围';
