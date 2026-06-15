# P2-D 经营分析/预测看板指标沉淀开发计划

## 1. 目标

为经营分析和后续预测看板建立稳定的指标快照数据层。P2-D 先沉淀指标快照和只读查询接口，不引入预测模型、不自动给经营建议、不替代财务结算口径。

## 2. 本期边界

### 包含

- 新增 `analytics_business_metric_snapshots` 指标快照表。
- 支持按日/周/月、业务域、指标键、分段对象沉淀实际值、基准值、趋势方向和人工基线预测值。
- 新增只读接口 `GET /api/v1/analytics/business-metric-snapshots`。
- 复用 `analytics.dashboard.view` 权限。
- 查询写审计事件 `analytics.business_metric_snapshot.viewed`。

### 不包含

- 不做机器学习预测。
- 不自动生成经营决策建议。
- 不改变 P0/P1 财务主口径、应收应付台账、票务/酒吧/游乐交易闭环。
- 不接入第三方 BI 或数据仓库。

## 3. 文件变更

### 修改文件

- `backend/src/main/java/com/complex/platform/analytics/AnalyticsRecords.java`
- `backend/src/main/java/com/complex/platform/analytics/AnalyticsRepository.java`
- `backend/src/main/java/com/complex/platform/analytics/JdbcAnalyticsRepository.java`
- `backend/src/main/java/com/complex/platform/analytics/AnalyticsService.java`
- `backend/src/main/java/com/complex/platform/analytics/AnalyticsController.java`
- `backend/src/test/java/com/complex/platform/analytics/AnalyticsServiceTests.java`
- `backend/src/test/java/com/complex/platform/analytics/AnalyticsControllerTests.java`
- `backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java`
- `backend/src/test/java/**/*JdbcIntegrationTests.java` 中的 Flyway 版本断言。

### 新增文件

- `backend/src/main/resources/db/migration/V037__p2_business_metric_snapshots.sql`
- `docs/p2-business-metric-snapshots-api-contract-v0.1.md`
- `docs/p2-business-metric-snapshots-completion-audit-v0.1.md`

## 4. 数据结构

`analytics_business_metric_snapshots` 记录：指标快照 ID、指标键、业务域、周期、业务日期、分段类型、分段 ID、实际值、基准值、偏差率、趋势方向、预测/投影方法、投影值、置信度、扩展载荷、创建时间。

## 5. 测试点

- 服务层返回指标快照，并记录精确审计事件。
- 控制器使用 `analytics.dashboard.view` 权限。
- PostgreSQL 迁移创建表、写入种子数据、查询 JSONB 载荷。
- Flyway 最新版本推进到 `037`。

## 6. 风险与回滚

- 风险：把人工基线预测误解成 AI 预测。处理：字段和文档明确 `manual_baseline` 不是模型预测。
- 风险：指标值与财务结算口径冲突。处理：P2-D 只做分析快照，不作为结算依据。
- 回滚：撤回 V037 和 analytics 只读接口，不影响交易、核销、财务、票务和酒吧主流程。
