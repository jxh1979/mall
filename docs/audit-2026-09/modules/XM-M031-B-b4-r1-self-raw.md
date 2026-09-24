CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M031-B B4-r1 修复 diff 审计 · 席甲报告
版本：V0.01

## 0. 输入与方法

- 席甲：Claude Opus 5.5（作者自审票，B3 由本席实施）。2026-09-24T05:52:05Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（V0.14，LOCK-AMEND-COUNT 1）。定稿：findings V4.04「XM-M031-B B2-r2 双 PASS 定稿」段及其所引 B2-r2 候选与 B2-r1 定稿，修复队列「是」XM-M031-B-001。
- diff：before tree 70775ec209d7c10729b8d35f11369b3e716f2ef9 → after tree b0073bee235305853d226ddc84785805ec390b99；冻结 /home/james/audit-tmp/xm031b_b3r1.diff（6149bytes SHA256 3967a92b09840dda73033e5bd01d1ddd4880608b9466b790d044653d73955a6b），2 files changed, 75 insertions；无删除、无文件模式变更。落盘前未读 Grok 本次正文。

## 1. 可执行证据

```
START=2026-09-24T05:49:00Z HEAD=e456ba96 origin/main=4a066def
scratch at 4a066def
== RED（只落测试补丁）
MVN_EXIT(red)=1
  red: suites=263 tests=3539 red=2
  RED 相对原样基线（origin/main 4a066def 0 红）新增红：
    NEW-RED: TraceabilityJdbcIntegrationTests.failureEventIsKeptAndTheOriginalErrorSurvivesWhenTheCallerTransactionIsAborted
    NEW-RED: TraceabilityJdbcIntegrationTests.failureEventOfATransactionalWriteSurvivesItsRollback
== GREEN（测试＋生产补丁）
MVN_EXIT(green)=0
  green: suites=263 tests=3539 red=0
  GREEN 全部红（预期 0）：
  GREEN 相对基线转绿条数：0
  scratch dirty after cleanup: 0
  scratch removed
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-24T05:50:46Z
```

RED 失败信息（/home/james/audit-tmp/xm031b_b3_red.msgs）：

```
TraceabilityJdbcIntegrationTests.failureEventIsKeptAndTheOriginalErrorSurvivesWhenTheCallerTransactionIsAborted | failure | java.lang.AssertionError |  Expecting actual throwable to be an instance of:   org.springframework.dao.DataIntegrityViolationException but was:   org.springframework.jdbc.UncategorizedSQLException: PreparedStatementCallback; uncategorized SQLException for SQL [INSERT INTO audit_events (     id, occurred_at, event_name, event_result, actor_type, actor_id, auth_context,     terminal, permission_code, business_type, business_i | at com.complex.platform.evidence.TraceabilityJdbcIntegrationTests.failureEventIsKeptAndTheOriginalErrorSurvivesWhenTheCallerTransactionIsAborted(TraceabilityJdbcIntegrationTests.java:185)
TraceabilityJdbcIntegrationTests.failureEventOfATransactionalWriteSurvivesItsRollback | failure | org.opentest4j.AssertionFailedError |  expected: 1  but was: 0 | at com.complex.platform.evidence.TraceabilityJdbcIntegrationTests.failureEventOfATransactionalWriteSurvivesItsRollback(TraceabilityJdbcIntegrationTests.java:167)
```

```
主工作区落补丁（XM-M031-B B3-r1）2026-09-24T05:51:02Z
写入前 2 个文件与 origin/main 4a066def 相同
before tree=70775ec209d7c10729b8d35f11369b3e716f2ef9
after tree=b0073bee235305853d226ddc84785805ec390b99
frozen diff /home/james/audit-tmp/xm031b_b3r1.diff: 6149 bytes sha256 3967a92b09840dda73033e5bd01d1ddd4880608b9466b790d044653d73955a6b
 2 files changed, 75 insertions(+)
主工作区 diff 与一次性 worktree 验证过的 diff：same
```

## 2. 逐块归因（2 块）

| 文件 | 块 | 块头 | ± | 归因 ID | 首条改动行（截断） |
|---|---:|---|---|---|---|
| AuditEventService.java | 1 | `@@ -56,6 +56,12` | +6/−0 | XM-M031-B-001 | /** |
| TraceabilityJdbcIntegrationTests.java | 1 | `@@ -130,4 +130,73` | +69/−0 | XM-M031-B-001 | // ===== XM-M031-B：失败留痕不随调用方事务回滚丢失（audit_events 只追加，V140；按本次唯一键断言，不清理） |

共 2 块，+75/−0，逐块归因齐全。

归因不到定稿 ID 的行：0。

## 3. 内容审计

- 生产：只在 recordFailure 方法上加 `@Transactional(propagation = Propagation.REQUIRES_NEW)` 与说明注释（全限定名，未改 import）；record()、两个 recordSuccess、searchOperationEvents 未动。注释所述「成功事件仍随调用方事务」与代码一致。生产调用都经 Spring 代理（类内无自调用）；AuthService:64 的开发用 `new AuditEventService(event -> event)` 不落库，不受影响。
- 测试：只在 TraceabilityJdbcIntegrationTests 末尾追加一个注释分隔、三个注入字段（AuditEventService、AccessService、PlatformTransactionManager）、一个计数辅助方法与三条方法级 NOT_SUPPORTED 回归；类原有 5 条用例、@AfterEach 与注解未改；无 DELETE、无 TRUNCATE，按本次唯一键断言。
  - ①同一平台管理员主体对缺失用户做 assignUserRole 与 assignUserPosition，两条失败事件各 1 行；RED 下定岗那条 0 行而红。
  - ②打开的事务里 `SELECT 1/0`，catch 里 recordFailure 后 throw 原异常，不包内层 try；RED 下 recordFailure 的 UncategorizedSQLException 直接冒出、断言原异常种类即红；GREEN 下原异常为 DataIntegrityViolationException、失败事件 1 行。
  - ③会回滚的事务里 recordSuccess，成功事件 0 行；修复前后都绿，守住不把独立事务加宽到成功路径。
- 持久副作用与定稿一致：audit_events 按本次唯一键若干行；赋角色失败另留 idempotency_records 一行。
- GREEN 全量 3539 测 0 红：独立事务未碰到其余测试的精确计数（B2 登记的验证项已满足）。
- 借机删弱、新业务、新依赖：无。

## 4. 结论

- 归因票：**PASS**（2 块全部归因到 XM-M031-B-001，越界 0）。
- 内容票：**PASS**。
- 席甲 B4-r1 票：**PASS**。
