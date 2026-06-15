# P1 第三方回流、重试、补偿和对账实施计划

## 目标

在供应商 SDK/API 尚未确定前，先把 P1 第三方接入的可验证后台骨架落地：餐饮/零售订单与券核销回流流水、重试任务、补偿任务、对账批次和对账差异处理。所有供应商私有字段只进入 `provider_payload` 或 `payload_json`，不写死进票务、酒吧、财务或库存主模型。

## 范围

- 新增 P1 集成任务与对账表。
- 新增服务层方法与管理接口，支持创建同步流水、创建/触发重试、创建/处理补偿、创建对账批次、创建/处理对账差异。
- 保留现有 P0 Open Platform Webhook、Adapter、mock、签名和幂等能力。
- 不做真实供应商 SDK 调用，不做定时任务调度器，不做餐饮/零售业务主表入账。

## 修改文件

修改：
- `backend/src/main/java/com/complex/platform/integration/IntegrationRepository.java`
- `backend/src/main/java/com/complex/platform/integration/JdbcIntegrationRepository.java`
- `backend/src/main/java/com/complex/platform/integration/IntegrationService.java`
- `backend/src/main/java/com/complex/platform/integration/IntegrationController.java`
- `backend/src/test/java/com/complex/platform/integration/IntegrationServiceTests.java`
- `backend/src/test/java/com/complex/platform/integration/IntegrationControllerTests.java`
- `backend/src/test/java/com/complex/platform/integration/IntegrationJdbcIntegrationTests.java`

新增：
- `backend/src/main/java/com/complex/platform/integration/IntegrationOrderSyncRecord.java`
- `backend/src/main/java/com/complex/platform/integration/IntegrationTaskRecord.java`
- `backend/src/main/java/com/complex/platform/integration/ReconciliationBatchRecord.java`
- `backend/src/main/java/com/complex/platform/integration/ReconciliationDifferenceRecord.java`
- `backend/src/main/java/com/complex/platform/integration/IntegrationOrderSyncRequest.java`
- `backend/src/main/java/com/complex/platform/integration/IntegrationTaskRequest.java`
- `backend/src/main/java/com/complex/platform/integration/IntegrationTaskResolveRequest.java`
- `backend/src/main/java/com/complex/platform/integration/ReconciliationBatchRequest.java`
- `backend/src/main/java/com/complex/platform/integration/ReconciliationDifferenceRequest.java`
- `backend/src/main/java/com/complex/platform/integration/ReconciliationDifferenceResolveRequest.java`
- `backend/src/main/resources/db/migration/V030__p1_integration_retry_compensation_reconciliation.sql`
- `docs/p1-integration-retry-compensation-reconciliation-api-contract-v0.1.md`

## 数据结构/接口变化

新增表：
- `integration_order_syncs`
- `integration_retry_tasks`
- `integration_compensation_tasks`
- `integration_reconciliation_batches`
- `integration_reconciliation_differences`

新增接口：
- `GET/POST /api/v1/integration/order-syncs`
- `GET/POST /api/v1/integration/retry-tasks`
- `POST /api/v1/integration/retry-tasks/{id}/run-now`
- `GET/POST /api/v1/integration/compensation-tasks`
- `POST /api/v1/integration/compensation-tasks/{id}/resolve`
- `GET/POST /api/v1/integration/reconciliation/batches`
- `GET/POST /api/v1/integration/reconciliation/differences`
- `POST /api/v1/integration/reconciliation/differences/{id}/resolve`

## 测试点

- 服务层：
  - 餐饮/零售同步流水按 provider + external id 幂等 upsert。
  - 重试任务 `run-now` 增加次数并更新下一次执行时间/状态。
  - 补偿任务必须记录处理人、处理结果和附件。
  - 对账差异处理必须关联批次并保留差异金额。
- 控制器：
  - 写接口强制幂等键。
  - 不同能力使用细分权限。
- JDBC：
  - V030 表、索引、权限存在。
  - 记录可保存、查询和更新。

## 验收标准

- 聚焦测试通过：`mvn -Dtest=IntegrationServiceTests,IntegrationControllerTests,IntegrationJdbcIntegrationTests test`
- P0 契约不漂移：`npm run verify:p0-contract`
- Semgrep 聚焦扫描无高风险发现。
- 后端全量测试通过：`mvn test`

## 风险和回滚

- 风险：历史文档中的 `/api/integration/*` 与正式路径冲突。缓解：本切片统一落在 `/api/v1/integration/*`。
- 风险：真实供应商字段提前污染主模型。缓解：私有字段仅进入 JSON payload，不进入业务主表。
- 回滚：未应用环境删除 V030 和新增类；已应用环境通过前向迁移停用新增权限并保留流水归档。

## 执行清单

- [x] 编写失败测试。
- [x] 新增记录和请求对象。
- [x] 扩展 Repository/JDBC。
- [x] 扩展 Service/Controller。
- [x] 新增 V030 迁移和 P1 API 合同文档。
- [ ] 在 160 上运行聚焦测试、P0 契约、Semgrep 和全量测试。
