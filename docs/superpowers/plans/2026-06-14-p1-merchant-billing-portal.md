# P1-MERCHANT-BILLING-PORTAL-01 商户账单门户开发计划

## 目标

补齐 P1 商户账单门户闭环：基于 P0 应收应付台账形成商户可查看、可确认、可异议、可催缴留痕的账单服务。该模块不替代财务 AR/AP 台账，财务真相仍以 P0 `ar_ap_items` 为准；P1 只做商户侧账单呈现与协作流程。

## 修改文件

- 新增 `backend/src/main/java/com/complex/platform/merchantbilling/*`
- 新增 `backend/src/main/java/com/complex/platform/merchantbilling/dto/*`
- 新增 `backend/src/main/resources/db/migration/V026__p1_merchant_billing_portal.sql`
- 新增 `backend/src/test/java/com/complex/platform/merchantbilling/*`
- 新增 `docs/p1-merchant-billing-portal-api-contract-v0.1.md`
- 更新已有 JDBC 集成测试的 Flyway 最新版本断言为 `026`

## 数据结构/接口变化

- `merchant_bills`：商户账单主表，引用 `ar_ap_item_id`，记录账期、到期日、总金额、状态、账龄。
- `merchant_bill_lines`：账单费用明细，支持租金、物业费、能源费、公摊/CAM、押金、其他费用。
- `merchant_bill_disputes`：账单异议，记录原因、描述、证据附件和处理状态。
- `merchant_bill_dunning_records`：催缴记录，记录渠道、模板、状态、发送时间。
- API：
  - `GET /api/v1/merchant-billing/bills`
  - `POST /api/v1/merchant-billing/bills`
  - `POST /api/v1/merchant-billing/bills/{id}/confirm`
  - `POST /api/v1/merchant-billing/bills/{id}/disputes`
  - `POST /api/v1/merchant-billing/bills/{id}/dunning-records`

## 测试点

- 创建商户账单时校验商户、账期、到期日、金额和费用明细。
- 创建商户账单时可引用已有 `ar_ap_item_id`，但不修改 AR/AP 台账。
- 商户账单确认后状态从 `issued/viewed/overdue` 变为 `confirmed`。
- 异议提交后账单进入 `disputed`，并保留证据附件。
- 催缴记录只允许对未关闭账单写入，并记录渠道、模板、操作者和发送时间。
- 所有写操作需要幂等键，重复请求不重复写。
- Controller 层验证权限和幂等键。
- JDBC 集成测试验证 V026 表、权限、读写映射。

## 验收标准

-  focused tests 通过：`MerchantBillingServiceTests`、`MerchantBillingControllerTests`、`MerchantBillingJdbcIntegrationTests`。
- `npm run verify:p0-contract` 通过，证明 P0 OpenAPI 未漂移。
- focused Semgrep 对新增包和 V026 迁移无发现。
- `mvn test` 全量通过。

## 风险与回滚

- 风险：账单门户和 P0 AR/AP 口径重复。控制方式：账单表只做商户侧展示和协作，金额口径来自请求或 AR/AP 引用，不反写 AR/AP。
- 风险：商户侧真实登录体系尚未落地。控制方式：P1 先提供后台/门户服务接口并使用权限码保护，后续商户端登录接入后复用接口。
- 回滚：删除新增 `merchantbilling` 包、V026 迁移、P1 API 文档和相关测试；不影响 P0 表和既有业务。

## 执行清单

- [x] 写入 Service 失败测试并确认失败
- [x] 实现领域记录、DTO、Repository、Service 并确认 Service 测试通过
- [x] 写入 Controller 失败测试并确认失败
- [x] 实现 Controller 并确认 Controller 测试通过
- [x] 写入 JDBC 失败测试并确认失败
- [x] 新增 V026 迁移并确认 JDBC 测试通过
- [x] 补充 P1 API 合同文档
- [x] 运行 focused tests、P0 契约、Semgrep、全量测试
