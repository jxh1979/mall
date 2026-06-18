# T12 经营看板 真实聚合 + 行级隔离 + DTO 对齐 可编码开发规格 V0.1（2026-06-18）

> **任务**：R5 backlog **T12**（W2），数据分析域。
> **裁决依据**：裁决⑩——真实聚合（替换种子 ETL）+ 商户行级隔离 + 前后端 DTO 字段对齐，三项 P0。**口径见 `r5-aggregation-metrics-draft-20260618.md`**（七类指标 metric_code + 源表，待财务/运营复核）。
> **现状基准**（已核）：analytics 指标直读 `analytics_*` 快照表，值来自迁移手写 INSERT 种子（V017/V037，manual baseline）；前端 `apps/web/src/admin/types.ts:35-37` 期望 `issuedTickets/verifications/barOrders`，后端 `AnalyticsRecords.DashboardSnapshotRecord` 字段为 `revenueAmount/orderCount/riskAlertCount` 等——**字段名零交集，6 卡全 0**；analytics 包 **0 处** `currentMerchantScope`。
> **迁移**：取下一连续版本（示意 **V075**，若仅刷新逻辑可无迁移）；测试用 `MigrationVersions.latest()`。

---

## 1. 三件事
1. **真实聚合**：用按 `r5-aggregation-metrics-draft` §2 的七类 metric_code 从真实业务表聚合，替换种子；两种实现择一（**建议 B**）：
   - A 按需聚合：查询时实时 JOIN 业务表（实时但重）。
   - **B 定时刷新快照**：`@Scheduled` ETL 把聚合写入 `analytics_business_metric_snapshots`（按 metric_key + scope + date），看板读快照（与现有快照表结构一致，只是数据来源从种子改为 ETL）。
2. **行级隔离**：analytics 所有查询注入 `currentMerchantScope`——平台角色见全量、商户角色仅见本 merchant_id；ETL 按 (metric_key, merchant_id/project_id) 维度落快照。
3. **DTO 对齐**：以聚合草案 metric_code 为权威，统一前后端字段（修 6 卡全 0）。

## 2. DTO 收口（权威字段 = metric_code）
后端总览响应改为 `metrics: { [metricCode]: { value, dimension? } }` 形式（或显式字段与前端逐一对齐）。建议统一为 map：
```
GET /api/v1/analytics/dashboards/p0-overview?scope=...
→ { asOf, scope, metrics: {
     "revenue.net": 1234500, "revenue.ticket": ..., "traffic.entry": ...,
     "verify.rate": 0.92, "member.new": ..., "supply.balance": ...,
     "risk.pending_review": ..., "finance.daily_close_status": {...} } }
```
前端 `types.ts`/`OverviewPage.tsx`/legacy-console 改读 `metrics[metricCode]`，删除 `issuedTickets/barOrders` 旧字段（或后端补这些 metricCode 别名，二选一，**建议改前端读 metric_code**）。

## 3. 下钻
总览 → 票务/酒吧/供应链/游乐/商户/异常/对账明细：`GET /api/v1/analytics/dashboards/{domain}`，同样经 scope 隔离，按维度（项目/渠道/商户/时段）下钻。

## 4. 逻辑（`AnalyticsService` + ETL）
- `DashboardAggregationJob`（@Scheduled）：按聚合草案公式从 ticket_orders/bar_bills/integration_order_syncs/amusement_verifications/inventory_*/customer_points_*/supervisor_review_decisions 等聚合 → upsert `analytics_business_metric_snapshots(metric_key, scope_type, scope_id, business_date, value)`。
- `AnalyticsService.overview(scope)`：注入 `currentMerchantScope`，读快照按 metric_code 组装 map。
- 标注：采购到货/补录数量等依赖 T6/留痕的指标，落地前按"数据未就绪"返回 null + 备注（不伪造）。

## 5. 验收（GWT）
- Given 真实业务数据，When 查总览，Then 各 metric_code 有非种子聚合值、6 卡有值（非全 0）。
- Given 商户角色登录，When 查看板，Then 仅见本 merchant_id 范围数据（跨商户不可见）。
- Given 前端，When 渲染总览，Then 字段名与后端 metric_code 一致。
- Given 下钻，When 点某域，Then 返回该域明细且受 scope 隔离。

## 6. 测试 / 依赖 / claims
- 测试：聚合数值正确（对拍真实业务表）、scope 隔离（商户只见自身）、前后端 DTO 契约测试、null 指标不伪造；`MigrationVersions.latest()`。
- 依赖：聚合口径草案（待财务复核定稿）；T6/T7（供应链/财务指标源）；`currentMerchantScope`。
- **claims**：无新增"声称未落"表；矩阵 REQ-81/82 行下修转"真实聚合+隔离已落地"。
- **未决**：聚合口径定稿（财务/运营复核草案后）。

## 7. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T12 规格（ETL 刷新快照替种子+currentMerchantScope 行级隔离+metric_code DTO 对齐+下钻） | R5 backlog T12；裁决⑩；口径 r5-aggregation-metrics-draft；现状取自 types.ts/AnalyticsRecords |
