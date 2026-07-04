-- V218 AI/对标阈值可配置化(治理铁律:业务策略参数禁止硬编码)。
--   原:AnalyticsService 写死两常量——
--     LOCAL_LLM_MIN_CONFIDENCE=0.60(本地大模型异常建议自动采纳的最低置信度,低于此转人工复核)
--     BENCHMARK_THRESHOLD=0.20(集团下钻对标:低于同类均值 20% 记「低于对标」告警)
--   改:落单例配置表,AnalyticsService 从表读;经营参数页维护(复用 analytics.operating_params.manage)。
--   阈值变更改配置不改代码。单例 id='default',仿 V207 analytics_anomaly_config 范式。

CREATE TABLE IF NOT EXISTS analytics_ai_thresholds (
    id                       VARCHAR(16) PRIMARY KEY,
    local_llm_min_confidence NUMERIC(4, 2) NOT NULL,   -- AI建议自动采纳最低置信度[0,1]
    benchmark_deviation      NUMERIC(4, 2) NOT NULL,   -- 对标偏离告警线[0,1](低于均值该比例记低于对标)
    updated_by               VARCHAR(64),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_ai_conf CHECK (local_llm_min_confidence >= 0 AND local_llm_min_confidence <= 1),
    CONSTRAINT chk_ai_dev  CHECK (benchmark_deviation >= 0 AND benchmark_deviation <= 1)
);
-- 种子=原硬编码值(行为不变)。
INSERT INTO analytics_ai_thresholds (id, local_llm_min_confidence, benchmark_deviation)
VALUES ('default', 0.60, 0.20)
ON CONFLICT (id) DO NOTHING;
