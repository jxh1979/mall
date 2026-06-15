# P1-INVENTORY-ALERTS-RECON-01 库存预警与供应商对账开发计划

## 目标

补齐 P1 供应链库存增强的最小可编码闭环：安全库存预警、缺货提醒、批次保质期预警和供应商对账留痕。P1 不做自动补货预测、复杂 BOM 成本核算、供应商评分和招采比价。

## 修改文件

- 修改 `backend/src/main/java/com/complex/platform/inventory/InventoryRepository.java`
- 修改 `backend/src/main/java/com/complex/platform/inventory/InventoryService.java`
- 修改 `backend/src/main/java/com/complex/platform/inventory/InventoryController.java`
- 修改 `backend/src/main/java/com/complex/platform/inventory/JdbcInventoryRepository.java`
- 新增库存 P1 记录与 DTO
- 新增 `backend/src/main/resources/db/migration/V027__p1_inventory_alerts_reconciliation.sql`
- 修改/新增库存相关测试
- 新增 `docs/p1-inventory-alerts-reconciliation-api-contract-v0.1.md`

## 数据结构/接口变化

- `inventory_stock_batches`：记录库存批次、供应商、库位、数量、到期时间和状态。
- `inventory_risk_alerts`：记录安全库存、缺货、保质期预警。
- `inventory_supplier_reconciliations`：记录供应商对账周期、应付/实付或应核/实核金额、差异和证据附件。
- API：
  - `GET /api/v1/inventory/stock-batches`
  - `POST /api/v1/inventory/stock-batches`
  - `GET /api/v1/inventory/risk-alerts`
  - `POST /api/v1/inventory/risk-alerts/scan`
  - `GET /api/v1/inventory/supplier-reconciliations`
  - `POST /api/v1/inventory/supplier-reconciliations`

## 测试点

- 有库存余额低于物品 `min_quantity` 时生成 `safety_stock_low`。
- 可用库存为 0 时生成 `out_of_stock`。
- 有活动批次在阈值天数内到期时生成 `expiry_warning`。
- 风险扫描需要幂等键，重复扫描不重复生成。
- 供应商对账自动计算差异金额，并保存证据附件。
- Controller 验证权限和幂等键。
- JDBC 集成测试验证 V027 表、权限和读写映射。

## 验收标准

- focused tests 通过：`InventoryServiceTests`、`InventoryControllerTests`、`InventoryJdbcIntegrationTests`。
- `npm run verify:p0-contract` 通过。
- focused Semgrep 对库存新增/修改文件和 V027 迁移无发现。
- `mvn test` 全量通过。

## 风险与回滚

- 风险：预警与财务/采购真实状态混淆。控制方式：预警只做风险提示，不自动生成采购单或付款。
- 风险：保质期数据源不足。控制方式：P1 新增手工/入库批次表，真实供应商批次导入后再映射。
- 回滚：删除 V027 迁移、新增记录/DTO/接口和测试；不影响 P0 库存余额、出入库和存酒闭环。

## 执行清单

- [x] 写入 Service 失败测试并确认失败
- [x] 实现记录、DTO、Repository、Service 并确认 Service 测试通过
- [x] 写入/补齐 Controller 失败测试并确认失败
- [x] 实现 Controller 并确认 Controller 测试通过
- [x] 写入 JDBC 失败测试并确认失败
- [x] 新增 V027 迁移并确认 JDBC 测试通过
- [x] 补充 P1 API 合同文档
- [x] 运行 focused tests、P0 契约、Semgrep、全量测试
