# P2-C 票务动态定价规则容器开发计划

## 1. 目标

在不改变现有售票下单价格计算路径的前提下，新增动态定价规则容器，支持运营创建草稿、人工发布、按日期/渠道/上下文模拟价格。P2-C 只提供规则沉淀与模拟能力，不自动影响已售票、不接入外部渠道、不接入预测模型。

## 2. 边界

### 本期包含

- 动态定价规则表、权限种子和管理员默认授权。
- 后台接口：列表、创建草稿、发布、模拟。
- 规则匹配：票种、日期范围、销售渠道、条件 JSON、优先级。
- 调价方式：百分比增减、固定金额增减。
- 价格保护：最低价、最高价钳制。
- 全链路幂等、权限和审计事件。

### 本期不包含

- 不把动态定价自动接入正式下单价格。
- 不改 P0 平日/周末/节假日价和多人/新人/学生/渠道价闭环。
- 不做 OTA/第三方渠道自动推价。
- 不做机器学习预测或自动发布。

## 3. 文件变更

### 修改文件

- `backend/src/main/java/com/complex/platform/ticketing/TicketingController.java`
- `backend/src/main/java/com/complex/platform/ticketing/TicketingService.java`
- `backend/src/main/java/com/complex/platform/ticketing/TicketingRepository.java`
- `backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java`
- `backend/src/test/java/com/complex/platform/ticketing/TicketingServiceTests.java`
- `backend/src/test/java/com/complex/platform/ticketing/TicketingControllerTests.java`
- `backend/src/test/java/com/complex/platform/ticketing/TicketingJdbcIntegrationTests.java`
- `backend/src/test/java/**/*JdbcIntegrationTests.java` 中的 Flyway 版本断言。

### 新增文件

- `backend/src/main/resources/db/migration/V036__p2_ticket_dynamic_pricing_rules.sql`
- `backend/src/main/java/com/complex/platform/ticketing/TicketDynamicPricingRuleRecord.java`
- `backend/src/main/java/com/complex/platform/ticketing/TicketDynamicPricingRuleRequest.java`
- `backend/src/main/java/com/complex/platform/ticketing/TicketDynamicPricingSimulationRequest.java`
- `backend/src/main/java/com/complex/platform/ticketing/TicketDynamicPricingSimulationResponse.java`
- `docs/p2-ticket-dynamic-pricing-api-contract-v0.1.md`
- `docs/p2-ticket-dynamic-pricing-completion-audit-v0.1.md`

## 4. 数据结构与接口变化

- 新增 `ticket_dynamic_pricing_rules`：规则 ID、票种、名称、优先级、生效日期、销售渠道、调价类型、调价率/调价金额、价格上下限、状态、条件 JSON、操作说明、证据附件、创建/更新时间。
- 新增权限：`ticket.dynamic_pricing.view`、`ticket.dynamic_pricing.manage`、`ticket.dynamic_pricing.publish`、`ticket.dynamic_pricing.simulate`。
- 新增接口：
  - `GET /api/v1/ticketing/dynamic-pricing-rules`
  - `POST /api/v1/ticketing/dynamic-pricing-rules`
  - `POST /api/v1/ticketing/dynamic-pricing-rules/{id}/publish`
  - `POST /api/v1/ticketing/dynamic-pricing/simulations`

## 5. 测试点

- 创建规则默认 `draft`，并写 `ticket_dynamic_pricing.changed` 审计事件。
- 模拟命中规则后返回基础价、调整额、最终价和命中规则 ID。
- 发布规则写 `ticket_dynamic_pricing.published` 审计事件。
- 未命中规则时最终价等于基础价。
- 现有正式下单仍只使用基础票价/票价变体，不被动态定价影响。
- HTTP 接口使用精确权限，并要求幂等键。
- PostgreSQL JSONB 条件、附件列表、权限种子和 Flyway 036 迁移可验证。

## 6. 验收标准

- 票务服务测试、控制器测试、JDBC 集成测试通过。
- 后端全量测试通过。
- P0 契约闸口通过。
- Semgrep 对票务动态定价相关 Java 和 SQL 文件扫描无高危发现。
- 文档明确“模拟/人工发布”和“不自动影响正式下单”的边界。

## 7. 风险与回滚

- 风险：运营误以为发布动态定价会直接影响售票。本期接口和文档均明确只用于模拟和后续接入准备。
- 风险：条件 JSON 过度自由导致解释歧义。本期仅按键值完全匹配，复杂表达式留到后续版本。
- 回滚：撤回 V036 相关代码和接口即可；迁移只新增表和权限，不修改现有票务订单价格表。
