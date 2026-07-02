-- M3:清指标定义/异常两表的 V017 演示种子(冒真数据),真实管道接管——
-- 定义由代码指标目录(单一事实源)每次 ETL 同步为可查询投影;异常由日环比突变检测写入。
-- 注意先删异常(FK 引用定义表)。
DELETE FROM analytics_metric_anomalies WHERE id = 'anomaly_p0';
DELETE FROM analytics_metric_definitions WHERE metric_key = 'gmv_amount_cents';

-- 异常检测阈值(2026-07-02 拍板:环比突变,阈值后台可调):±50% 记可疑、±80% 记高风险。
CREATE TABLE IF NOT EXISTS analytics_anomaly_config (
    id VARCHAR(16) PRIMARY KEY,
    surge_pct NUMERIC(6, 4) NOT NULL,
    high_pct NUMERIC(6, 4) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_anomaly_pct CHECK (surge_pct > 0 AND high_pct >= surge_pct)
);
INSERT INTO analytics_anomaly_config (id, surge_pct, high_pct, enabled)
VALUES ('default', 0.5, 0.8, TRUE)
ON CONFLICT (id) DO NOTHING;
