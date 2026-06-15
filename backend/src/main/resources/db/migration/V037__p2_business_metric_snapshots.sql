CREATE TABLE IF NOT EXISTS analytics_business_metric_snapshots (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    metric_key VARCHAR(128) NOT NULL,
    domain VARCHAR(64) NOT NULL,
    period_type VARCHAR(32) NOT NULL,
    business_date DATE NOT NULL,
    segment_type VARCHAR(64) NOT NULL,
    segment_id VARCHAR(128),
    actual_value NUMERIC(18, 4) NOT NULL,
    baseline_value NUMERIC(18, 4),
    variance_rate NUMERIC(8, 2),
    trend_direction VARCHAR(16) NOT NULL,
    projection_method VARCHAR(64) NOT NULL,
    projected_value NUMERIC(18, 4),
    confidence_score NUMERIC(5, 4),
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_analytics_metric_snapshot_period CHECK (period_type IN ('daily', 'weekly', 'monthly')),
    CONSTRAINT chk_analytics_metric_snapshot_trend CHECK (trend_direction IN ('up', 'down', 'flat', 'unknown')),
    CONSTRAINT chk_analytics_metric_snapshot_projection CHECK (projection_method IN ('manual_baseline', 'moving_average', 'external_model_placeholder', 'not_applicable')),
    CONSTRAINT chk_analytics_metric_snapshot_payload CHECK (jsonb_typeof(payload_json) = 'object')
);

CREATE INDEX IF NOT EXISTS idx_analytics_business_metric_snapshots_lookup
    ON analytics_business_metric_snapshots (business_date DESC, domain, metric_key, segment_type, segment_id);

INSERT INTO analytics_business_metric_snapshots (
    snapshot_id, metric_key, domain, period_type, business_date, segment_type, segment_id,
    actual_value, baseline_value, variance_rate, trend_direction, projection_method,
    projected_value, confidence_score, payload_json, created_at
)
VALUES
    (
        'metric_snapshot_p2_gmv_20260614', 'gmv_amount_cents', 'finance', 'daily', DATE '2026-06-14',
        'project', 'project_1', 128800, 118800, 8.42, 'up', 'manual_baseline', 135000, 0.7000,
        '{"source":"p2_seed","note":"manual baseline only; no AI forecast applied"}'::jsonb,
        TIMESTAMPTZ '2026-06-14T00:00:00Z'
    ),
    (
        'metric_snapshot_p2_ticket_orders_20260614', 'ticket_order_count', 'ticketing', 'daily', DATE '2026-06-14',
        'project', 'project_1', 23, 20, 15.00, 'up', 'manual_baseline', 25, 0.6500,
        '{"source":"p2_seed"}'::jsonb, TIMESTAMPTZ '2026-06-14T00:00:00Z'
    ),
    (
        'metric_snapshot_p2_bar_revenue_20260614', 'bar_revenue_amount_cents', 'bar', 'daily', DATE '2026-06-14',
        'merchant', 'merchant_p0', 68800, 72000, -4.44, 'down', 'manual_baseline', 70000, 0.6200,
        '{"source":"p2_seed"}'::jsonb, TIMESTAMPTZ '2026-06-14T00:00:00Z'
    )
ON CONFLICT (snapshot_id) DO UPDATE SET
    actual_value = EXCLUDED.actual_value,
    baseline_value = EXCLUDED.baseline_value,
    variance_rate = EXCLUDED.variance_rate,
    trend_direction = EXCLUDED.trend_direction,
    projection_method = EXCLUDED.projection_method,
    projected_value = EXCLUDED.projected_value,
    confidence_score = EXCLUDED.confidence_score,
    payload_json = EXCLUDED.payload_json,
    created_at = EXCLUDED.created_at;
