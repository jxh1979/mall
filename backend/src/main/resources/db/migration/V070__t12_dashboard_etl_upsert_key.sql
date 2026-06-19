-- T12 经营看板真实聚合（ETL 刷新快照）。
-- analytics_business_metric_snapshots 在 V037 以合成主键 snapshot_id 建表，
-- 没有按业务维度的唯一键，ETL 无法做 (metric_key, period_type, business_date, segment_type, segment_id) 幂等 upsert。
-- 本迁移补该唯一键，并把 ETL 写入的 projection_method='etl_aggregate' 纳入 CHECK 白名单。
-- 不新建快照表（复用 V037 既有表），只补约束。

-- segment_id 允许 NULL（平台口径无具体商户/项目），用 COALESCE 归一为占位串构造唯一键，
-- 避免 NULL 在唯一索引下各自独立导致重复平台行。
CREATE UNIQUE INDEX IF NOT EXISTS uq_analytics_business_metric_snapshots_etl_key
    ON analytics_business_metric_snapshots (
        metric_key,
        period_type,
        business_date,
        segment_type,
        COALESCE(segment_id, '__platform__')
    );

ALTER TABLE analytics_business_metric_snapshots
    DROP CONSTRAINT IF EXISTS chk_analytics_metric_snapshot_projection;

ALTER TABLE analytics_business_metric_snapshots
    ADD CONSTRAINT chk_analytics_metric_snapshot_projection
    CHECK (projection_method IN (
        'manual_baseline',
        'moving_average',
        'external_model_placeholder',
        'not_applicable',
        'etl_aggregate'
    ));
