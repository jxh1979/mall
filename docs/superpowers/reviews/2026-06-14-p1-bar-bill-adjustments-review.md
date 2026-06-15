# P1 酒吧账单调整差异与安全审查报告

## 1. 审查对象

本次审查对象为 P1-BAR-02「酒吧账单调整」最小编码切片，覆盖：

- 新增接口：`POST /api/v1/bar/bills/{bill_id}/adjustments`
- 新增权限：`bar.bill.adjust`
- 新增数据库表：`bar_bill_adjustments`
- 新增能力：服务费、低消差额补齐、挂账登记
- 新增契约文档：`docs/p1-bar-bill-adjustment-api-contract-v0.1.md`

本次不审查完整拆单、合单、转台、配方成本、员工提成结算和第三方 POS 深度接入。

## 2. 审查限制

| 项目 | 结果 |
|---|---|
| Git diff | 当前工作目录不是 Git 仓库，无法使用 `git diff`。本次按变更文件清单、代码定位和测试证据做差异审查。 |
| Arc 完整参考资料 | local-skills-pack 中 `arc-review` / `arc-refactor` 引用的完整 `references/`、`disciplines/` 资料未随包落盘；本次使用已读技能说明和人工架构审查替代。 |
| Semgrep | 初审时 160 机器未安装 `semgrep`；后续已在 160 安装 Semgrep 1.166.0 并补充扫描，详见 `docs/superpowers/reviews/2026-06-14-semgrep-scan-report.md`。 |

## 3. 审查结论

结论：本切片已达到可继续进入下一项 P1 编码的质量门槛。

剩余高置信安全漏洞：未发现。

已发现并修复的问题：

| 编号 | 等级 | 问题 | 修复 |
|---|---|---|---|
| REVIEW-001 | P1 | 调整记录 ID 原先只由 `adjustmentType + idempotencyKey` 生成。两个不同账单使用相同幂等键和相同调整类型时，会生成相同主键，导致数据库写入冲突。 | 调整记录 ID 改为包含 `billId + adjustmentType + idempotencyKey`；新增红绿测试 `billAdjustmentIdIsScopedByBillToAvoidCrossBillIdempotencyCollision`。 |

## 4. 代码审查

| 检查项 | 结论 |
|---|---|
| 控制器权限 | `BarController` 对新增端点强制 `authorizationService.requirePermission("bar.bill.adjust")`。 |
| 幂等 | 控制器拒绝空 `idempotencyKey`；服务层使用 `adjustBarBill:{billId}` 作为操作域；数据库层使用 `(bill_id, idempotency_key)` 唯一索引兜底。 |
| 金额规则 | `service_fee` 和 `min_spend_top_up` 要求正数；`on_account` 要求金额为 0；负数由 `nonNegative` 拦截。 |
| 状态机 | 已结清账单拒绝调整；挂账只改支付状态和结算状态，不改变应付金额。 |
| 票务/卡座边界 | 低消补差逻辑不读取、不抵扣、不消耗票务、通票、卡座入场权益。 |
| 审计留痕 | 成功事件 `bar.bill.adjusted`，失败事件 `bar.bill.adjust_failed`，业务类型 `bar_bill_adjustment`。 |
| 数据库写入 | JDBC 使用命名参数和静态 SQL 文本，未发现新增动态 SQL 拼接。 |
| 过度设计 | 未引入新框架或复杂状态引擎；保留为 P1 最小账单调整切片。 |

## 5. 安全审查

| 风险点 | 检查结果 |
|---|---|
| 越权 | 新端点有独立高风险权限点 `bar.bill.adjust`，并在 Flyway 种子中配置为 `requires_approval=true`。 |
| IDOR / 对象级权限 | 当前权限体系仍以权限点为主，未在本切片新增按酒吧/商户范围的对象级授权；这属于既有平台权限模型约束，不是本次新增回归。 |
| SQL 注入 | 新增 JDBC 读写均使用 `NamedParameterJdbcTemplate` 参数绑定；未发现新增字符串拼接 SQL。 |
| 金额篡改 | 客户端可提交金额，但服务端按调整类型做金额规则校验；账单应付由服务端当前账单值计算。 |
| 幂等绕过 | 已补跨账单同幂等键 ID 碰撞测试；同账单同幂等键由服务层和数据库双重保护。 |
| 敏感信息 | 新增切片未引入密码、令牌、JWT、对象存储密钥等敏感字段。 |
| 日志/审计 | 写操作经 `AuditEventService` 留痕；未新增直接日志输出请求体的代码。 |

## 6. 架构审查

| 维度 | 结论 |
|---|---|
| 模块边界 | 继续沿用 `BarController -> BarService -> BarRepository -> JdbcBarRepository`，没有把财务应收台账、POS 高级能力塞进本切片。 |
| 数据模型 | 新增 `BarBillAdjustmentRecord` 独立承载调整流水，避免把所有服务费/低消/挂账细节直接塞回 `bar_bills`。 |
| 迁移策略 | V020 只增表、索引、权限种子，不破坏既有 P0/P1 表结构。 |
| 可测试性 | 服务、控制器、JDBC 三层均有新增覆盖；完整后端回归已通过。 |
| 后续演进 | 拆单、合单、转台、配方成本、员工提成结算可在此流水模型上继续扩展，但不建议在当前切片继续膨胀。 |

## 7. 验证证据

所有验证均在 192.168.88.160 上执行。

| 命令 | 结果 |
|---|---|
| `mvn -Dtest=BarServiceTests test` | 红绿验证完成；新增碰撞测试先失败后通过，最终 17 个测试通过。 |
| `mvn -Dtest=BarServiceTests,BarControllerTests,BarJdbcIntegrationTests test` | 62 个测试通过。 |
| `npm run verify:p0-contract` | P0 契约闸口通过；181 个 OpenAPI 操作检查同步。 |
| `mvn test` | 后端完整回归 382 个测试通过。 |

## 8. 后续建议

- P1 下一项建议继续做酒吧账单增强中的「挂账后应收台账联动」或「拆单/合单/转台」之一，不要同时开太多账单子能力。
- 如果要进入财务联动，应先明确 `bar_bill_adjustments` 到财务应收台账的映射口径：业务单据号、应收主体、结算主体、冲销/坏账/退款处理。
- Semgrep 已在 160 安装并完成本轮补扫；后续建议把 Important-only Semgrep 扫描固化为 P1/P2 安全检查项。
