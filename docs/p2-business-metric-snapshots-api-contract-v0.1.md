# P2-D 经营指标快照 API 契约 V0.1

## 1. 定位

经营指标快照用于沉淀综合体、票务、酒吧、商户、财务等业务域的分析指标，为后续经营看板和预测能力提供稳定数据底座。本期只读，不提供自动预测或经营决策。

## 2. 权限

- `analytics.dashboard.view`：查看经营指标快照。

## 3. 数据模型

### BusinessMetricSnapshotRecord

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `snapshot_id` | string | 快照 ID |
| `metric_key` | string | 指标键，例如 `gmv_amount_cents` |
| `domain` | string | 业务域，例如 `finance`、`ticketing`、`bar` |
| `period_type` | string | `daily`、`weekly`、`monthly` |
| `business_date` | date | 业务日期 |
| `segment_type` | string | 分段类型，例如 `project`、`merchant` |
| `segment_id` | string/null | 分段对象 ID |
| `actual_value` | decimal | 实际值 |
| `baseline_value` | decimal/null | 基准值 |
| `variance_rate` | decimal/null | 偏差率 |
| `trend_direction` | string | `up`、`down`、`flat`、`unknown` |
| `projection_method` | string | `manual_baseline`、`moving_average`、`external_model_placeholder`、`not_applicable` |
| `projected_value` | decimal/null | 投影值 |
| `confidence_score` | decimal/null | 置信度，仅作展示参考 |
| `payload` | object | 扩展信息 |
| `created_at` | datetime | 创建时间 |

## 4. 接口

### 查看经营指标快照

`GET /api/v1/analytics/business-metric-snapshots`

响应：`PageResponse<BusinessMetricSnapshotRecord>`

示例响应片段：

```json
{
  "snapshot_id": "metric_snapshot_p2_gmv_20260614",
  "metric_key": "gmv_amount_cents",
  "domain": "finance",
  "period_type": "daily",
  "business_date": "2026-06-14",
  "segment_type": "project",
  "segment_id": "project_1",
  "actual_value": 128800,
  "baseline_value": 118800,
  "variance_rate": 8.42,
  "trend_direction": "up",
  "projection_method": "manual_baseline",
  "projected_value": 135000,
  "confidence_score": 0.7000,
  "payload": {
    "source": "p2_seed"
  }
}
```

## 5. 口径说明

- `actual_value` 为分析读模型值，不替代财务结算表。
- `projection_method=manual_baseline` 表示人工/规则基线，不代表 AI 模型预测。
- 第三方 BI、数据仓库、机器学习模型接入均不在本期。

## 6. 审计事件

- 成功查看：`analytics.business_metric_snapshot.viewed`
- 查看失败：`analytics.business_metric_snapshot.view_failed`
