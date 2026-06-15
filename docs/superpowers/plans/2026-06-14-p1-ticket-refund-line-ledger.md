# P1 票务退款权益级拆分明细实施计划

## 目标

在不引入完整复杂退款规则引擎的前提下，先把 P1 票务退款的“按权益逐项拆分、可审计、可审批留痕”落成最小可编码切片。P0 已支持按订单申请退款和审批；本切片补齐退款明细行，让财务、客服和测试能看到每笔退款如何分摊到具体权益。

## 范围

- 新增退款明细表 `ticket_refund_lines`。
- 退款申请时按订单已发权益生成明细行；无已发权益时生成订单级明细行。
- 退款审批通过后同步把明细行状态置为 `approved`。
- 新增查询接口查看某笔退款的明细行。
- 不实现自定义比例规则、手续费规则、跨订单退款、第三方支付真实退款调用。

## 修改文件

修改：
- `backend/src/main/java/com/complex/platform/ticketing/TicketingRepository.java`
- `backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java`
- `backend/src/main/java/com/complex/platform/ticketing/TicketingService.java`
- `backend/src/main/java/com/complex/platform/ticketing/TicketingController.java`
- `backend/src/test/java/com/complex/platform/ticketing/TicketingServiceTests.java`
- `backend/src/test/java/com/complex/platform/ticketing/TicketingControllerTests.java`
- `backend/src/test/java/com/complex/platform/ticketing/TicketingJdbcIntegrationTests.java`
- 所有断言最新 Flyway 版本的 JDBC 测试。

新增：
- `backend/src/main/java/com/complex/platform/ticketing/TicketRefundLineRecord.java`
- `backend/src/main/resources/db/migration/V031__p1_ticket_refund_line_ledger.sql`
- `docs/p1-ticket-refund-line-ledger-api-contract-v0.1.md`

## 数据结构/接口变化

新增表：
- `ticket_refund_lines`

新增接口：
- `GET /api/v1/ticketing/refunds/{refund_id}/lines`

新增权限：
- `ticket.refund.view`

## 测试点

- 退款申请按已发权益生成退款明细，金额总和等于退款申请金额。
- 明细行保存权益 ID、权益类型、退款金额、退款前已用/剩余次数和状态。
- 审批退款后，明细行状态同步为 `approved`。
- 查询退款明细要求 `ticket.refund.view`。
- JDBC 迁移创建表、索引、权限并授权平台管理员。

## 验收标准

- 聚焦测试通过：`mvn -Dtest=TicketingServiceTests,TicketingControllerTests,TicketingJdbcIntegrationTests test`
- 与既有 P0/P1 接口不冲突。
- P0 契约校验通过。
- Semgrep 聚焦扫描无高风险发现。

## 风险和回滚

- 风险：误把 P1 明细当成完整退款规则引擎。缓解：本切片只做审计明细和审批同步，不做规则配置。
- 风险：退款金额与权益价值不完全一致。缓解：按权益顺序分摊，最后一行吃掉尾差；金额总和必须等于退款金额。
- 回滚：未应用环境删除 V031 和新增类；已应用环境通过前向迁移停用 `ticket.refund.view`，保留明细表只读归档。

## 执行清单

- [x] 编写失败测试。
- [x] 新增退款明细记录和迁移。
- [x] 扩展 Repository/JDBC。
- [x] 扩展 Service/Controller。
- [x] 新增 P1 API 合同文档。
- [ ] 在 160 上运行聚焦测试、P0 契约、Semgrep 和全量测试。
