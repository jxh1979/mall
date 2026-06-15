# P1 酒吧出品票据状态闭环实施计划

## 目标

补齐 P1 酒吧 POS/SaaS 增强中的出品票据状态闭环，使订单生成后可以由吧台/后厨执行接单、制作、备好、取消、异常登记，并保留事件明细与审计日志。该切片不改变 P0 点酒、账单、支付、库存扣减和送达闭环。

## 范围

- 复用现有 `bar_production_tickets`，新增状态流转接口和事件表。
- 新增出品票据事件记录，保存操作人、工位、原因、附件、幂等键、前后状态。
- 新增更细权限：`bar.production_ticket.view`、`bar.production_ticket.manage`。
- 保持 `deliverOrder` 仍可作为送达/库存扣减入口；送达后同步将对应出品票据标记为 `delivered`。

## 不做

- 不做厨房屏/前端页面。
- 不做机器人配送调度。
- 不做配方 BOM 自动扣料。
- 不把出品状态强制反向阻断现有 P0 送达流程，避免破坏已上线的服务员送达闭环。

## 修改文件

- 修改：
  - `backend/src/main/java/com/complex/platform/bar/BarService.java`
  - `backend/src/main/java/com/complex/platform/bar/BarController.java`
  - `backend/src/main/java/com/complex/platform/bar/BarRepository.java`
  - `backend/src/main/java/com/complex/platform/bar/JdbcBarRepository.java`
  - `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`
  - `backend/src/test/java/com/complex/platform/bar/BarControllerTests.java`
  - `backend/src/test/java/com/complex/platform/bar/BarJdbcIntegrationTests.java`
- 新增：
  - `backend/src/main/java/com/complex/platform/bar/BarProductionTicketEventRecord.java`
  - `backend/src/main/java/com/complex/platform/bar/BarProductionTicketTransitionRequest.java`
  - `backend/src/main/resources/db/migration/V029__p1_bar_production_ticket_workflow.sql`
  - `docs/p1-bar-production-ticket-workflow-api-contract-v0.1.md`

## 数据结构/接口变化

- 新增表：`bar_production_ticket_events`
- 新增接口：
  - `GET /api/v1/bar/production-tickets/{ticket_id}/events`
  - `POST /api/v1/bar/production-tickets/{ticket_id}/transition`
- 状态流转：
  - `created -> accepted`
  - `accepted -> preparing`
  - `preparing -> ready`
  - `created|accepted|preparing -> cancelled|exception`
  - `ready -> delivered` 由送达入口同步产生

## 测试点

- 服务层：
  - 合法流转保存事件并更新票据状态。
  - 非法跳转被拒绝，票据状态不变。
  - 送达订单时同步将未取消/未异常票据置为 `delivered`。
- 控制器：
  - 新接口要求正确权限。
  - 写接口缺少幂等键返回校验错误。
- JDBC：
  - V029 表、索引、权限存在。
  - 票据事件可以写入并按时间倒序读取。

## 验收标准

- 聚焦测试通过：`mvn -Dtest=BarServiceTests,BarControllerTests,BarJdbcIntegrationTests test`
- P0 契约不漂移：`npm run verify:p0-contract`
- Semgrep 聚焦扫描无高风险发现。
- 后端全量测试通过：`mvn test`

## 风险和回滚

- 风险：更细权限可能影响旧调用方。缓解：迁移中将新权限授予平台管理员；保留查询接口路径不变，仅权限粒度更明确。
- 风险：出品状态与送达状态出现不一致。缓解：送达入口同步将对应票据置为 `delivered`，取消/异常票据不被自动送达覆盖。
- 回滚：未应用环境删除 V029、事件类和新增接口；已应用环境用前向迁移停用新增权限并保留事件表归档，不做破坏性删表。

## 执行清单

- [ ] 编写失败测试。
- [ ] 实现状态流转请求/事件对象。
- [ ] 扩展 Repository 与 JDBC。
- [ ] 扩展 Service 与 Controller。
- [ ] 新增 V029 迁移和 P1 API 合同文档。
- [ ] 在 160 上运行聚焦测试、P0 契约、Semgrep 和全量测试。
