# P1 酒吧挂账应收台账联动审查记录

## 范围

本轮完成 P1-BAR-03：酒吧账单执行 `on_account` 挂账时，自动创建财务 AR/AP 应收台账，并把 `ar_ap_item_id` 回写到 `bar_bill_adjustments`。

涉及文件：
- `backend/src/main/java/com/complex/platform/bar/BarService.java`
- `backend/src/main/java/com/complex/platform/bar/BarBillAdjustmentRecord.java`
- `backend/src/main/java/com/complex/platform/bar/BarReceivableLedgerService.java`
- `backend/src/main/java/com/complex/platform/bar/FinanceBarReceivableLedgerService.java`
- `backend/src/main/java/com/complex/platform/bar/JdbcBarRepository.java`
- `backend/src/main/java/com/complex/platform/finance/FinanceService.java`
- `backend/src/main/resources/db/migration/V021__p1_bar_on_account_receivable_link.sql`
- `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`
- `backend/src/test/java/com/complex/platform/bar/FinanceBarReceivableLedgerServiceTests.java`
- `backend/src/test/java/com/complex/platform/bar/BarJdbcIntegrationTests.java`
- `backend/src/test/java/com/complex/platform/bar/BarControllerTests.java`
- `backend/src/test/java/com/complex/platform/finance/FinanceServiceTests.java`
- `docs/p1-bar-bill-adjustment-api-contract-v0.1.md`

160 目录当前不是 Git 仓库，无法使用 `git diff` / `git blame` 做标准差异审查；本记录基于本轮明确变更文件、测试红绿过程、静态扫描结果和手工代码审查。

## TDD 证据

红灯 1：
- 命令：`mvn -Dtest=BarServiceTests,BarJdbcIntegrationTests,FinanceServiceTests test`
- 结果：编译失败，原因包括缺少 `BarReceivableLedgerService`、`BarBillAdjustmentRecord.arApItemId()`、新构造参数和数据库列。

绿灯 1：
- 命令：`mvn -Dtest=BarServiceTests,BarJdbcIntegrationTests,FinanceServiceTests,FinanceJdbcIntegrationTests,BarControllerTests test`
- 结果：75 个测试通过。

红灯 2：
- 命令：`mvn -Dtest=FinanceBarReceivableLedgerServiceTests test`
- 结果：同一账单不同调整幂等键会保存 2 条应收，失败信息为 `expected: 1 but was: 2`。

红灯 3：
- 命令：`mvn -Dtest=FinanceBarReceivableLedgerServiceTests test`
- 结果：改为账单级幂等键后，由于财务幂等服务会比较请求体，第二次调用触发 `IDEMPOTENCY_CONFLICT`。

最终绿灯：
- 命令：`mvn -Dtest=BarServiceTests,FinanceBarReceivableLedgerServiceTests,BarJdbcIntegrationTests,FinanceServiceTests,FinanceJdbcIntegrationTests,BarControllerTests test`
- 结果：76 个测试通过。

## 代码审查结论

未发现需要阻断的问题。

确认点：
- `BarService.applyBillAdjustmentInternal` 在 `on_account` 分支拒绝二次挂账，返回 `BAR_BILL_ALREADY_ON_ACCOUNT`，避免顺序重复挂账。
- `FinanceBarReceivableLedgerService` 使用账单维度的财务幂等键 `bar-on-account-{billId}`，并且财务请求体只包含稳定账单映射字段，避免不同外层幂等键导致财务侧重复建账。
- `FinanceService` 允许 `source_type=bar_bill`，使财务台账可直接追溯到酒吧账单。
- `JdbcBarRepository` 对 `ar_ap_item_id` 的 INSERT、SELECT、参数映射、mapper 均已覆盖。
- `V021` 迁移只新增 nullable 列和索引，不破坏历史流水。

## 安全审查结论

未发现高置信安全漏洞。

关注点：
- 权限：对外入口仍复用原 `bar.bill.adjust`，没有新增绕过权限的 API。
- SQL：本轮 JDBC 仍使用命名参数，没有字符串拼接 SQL。
- 金额：挂账仍强制 `amountCent=0`；服务费和低消补差仍要求正数。
- 业务防重：同一账单二次挂账被拒绝；财务侧按账单维度幂等。
- 审计：外层成功/失败仍走 `bar.bill.adjusted` / `bar.bill.adjust_failed`；财务台账创建仍走 `finance_ar_ap.created`。

Semgrep：
- 命令：`semgrep scan --metrics=off --severity WARNING --severity ERROR --config p/java --config p/findsecbugs --config p/secrets ...`
- 输出：`security-scans/static_analysis_semgrep_3/results/results.sarif`
- 结果：1 条命中，规则 `gitlab.find_sec_bugs.HARD_CODE_KEY-4`，位置 `BarService.java:55`。
- 判定：误报。该行是年龄核验结果字符串 `"passed"` / `"accepted"`，不是密钥、密码或令牌。

## 验证结果

- `npm run verify:p0-contract`：通过。181 个 OpenAPI operation 检查同步，77 个客户端路由模板覆盖，95 个方法级调用匹配。
- `mvn test`：通过。385 个测试，0 失败，0 错误。

## 剩余风险

- 并发极端场景下，如果两个事务同时读取到未挂账账单，财务侧账单维度幂等可以防止重复建账；其中一个事务仍可能在酒吧账单状态更新阶段因业务顺序或唯一约束变化失败。当前已由测试覆盖财务侧防重，后续若要进一步增强，可在 P1 后续切片为账单调整引入行级锁或状态版本号。
- 财务 AR/AP 台账的催收、坏账、账龄、第三方财务系统同步仍不在本切片内。
