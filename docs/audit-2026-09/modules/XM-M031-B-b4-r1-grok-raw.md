CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M031-B B4-r1 修复 diff 复审（席乙）

版本：V0.01
席位：Grok，grok-4.7/xhigh，独立只读。步骤：B4-r1，只审本轮修复 diff。不是 B5，不是模块收口。
正本：`/home/james/complex-management` 主工作区，分支 main。
模块状态：ledger 开工段把 XM-M031-B 记为 IN_PROGRESS（ledger 第 54 行）。B3 段记本轮已执行 1 次、累计 1/3。本席不改 ledger。
定稿来源：findings.md 版本行 V4.04。归因只认顶部「当前 XM-M031-B B2-r2 双 PASS 定稿」及其所引的 B2-r2 候选与 B2-r1 定稿。修复队列「是」只有 XM-M031-B-001。
未读 `modules/XM-M031-B-b4-r1-self-raw.md`，也未读其 audit-tmp 副本。旧 raw（本席 B1、B2-r1、B2-r2）读过，只作边界。未用 MCP、子代理、第三席。未问人。未改业务、HTML、锁、findings、ledger。未 fetch、commit、push、merge、deploy。未装依赖。未对 `complex` 或 `complex_test` 执行语句。本轮没有重跑 Maven。核对 diff 时曾把两棵 tree 的 `git diff --binary` 写入 `/tmp/xm031b_b4_tree.diff`，比对后已删除，该路径现在不存在。

## 技能

| 技能 | 本轮 |
|---|---|
| using-superpowers | 已读开头。用户「只允许两席、不派子代理、不问人、不修」优先于派代理和问人。 |
| using-arc | 已读开头。不走 Arc `audit`，不写 `docs/arc/`。 |
| graft | 已读全文。跑了一次 `graft grep recordFailure --in backend/src/main/java/com/complex/platform/evidence/AuditEventService.java`。命中 after 工作区上的 `recordFailure`（公开方法从第 65 行起）。该工作区这两个文件的 blob 与 after tree 相同，所以这一命中对得上 after。diff 是否越界以两棵 tree 的 `git diff` 为准，不以 graft 索引为准。节省约 1175 token。无美元计价行。 |
| verification-before-completion | 已读。下面的票在 `sha256sum`、`git diff --binary`、`cmp`、源码与日志读完之后才写。本席没有重跑 3539 项测试，不把席甲的退出码说成本席实跑。 |
| systematic-debugging | 已读。只取「读错误、对行号、沿调用追到同一条已中止的事务」。本体的插桩和改代码作废。 |
| diagnosing-bugs | 已读。红绿环用已落盘的 surefire 失败行，行号与本 diff 的测试源码一致。本轮没有新建反馈环，也没有建库。 |
| tdd | 已读。红绿以席甲脚本的两段为准：只落测试时两条新测试红，测试加生产注解后退出码 0。本体「测缝须经用户确认」按铁律 5 作废；缝在 B2 已定稿。 |
| grilling | 已读。追问自己用仓库回答。不派人，不等操作员。 |
| ponytail、ponytail-review | 已读。diff 是一处传播注解加定稿点名的三条回归。没有第二数据源、没有新依赖、没有改调用方。ponytail-review：没有可删的多余层。 |
| i-have-adhd | 已读。路径型回执不把本报告正文打到 CLI，这条优先于该技能的首行动作格式。 |
| codebase-design、writing-for-agents、requesting-code-review、receiving-code-review、git-guardrails-claude-code | 本轮未读这些文件，不记成已调用。本席是被请审的一方，不另开审阅。没有合并，没有提交。 |
| audit-context-building、research、dispatching-parallel-agents、subagent-driven-development | 未调用。前两个会带出第三席或额外 Markdown；后两个按 V0.17 划掉。 |
| batch-grill-me、ubiquitous-language | 环境里没有，记缺口。 |

锁文件实算命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`。摘要只写在本文件第一行。sidecar `plan/criteria-lock.sha256` 与 ledger 顶部基准行的值与这次实算相同。LOCK-AMEND-COUNT 仍是 1。本轮不改锁。

## 输入核对

| 项 | 本席实算 |
|---|---|
| 冻结 diff | `/home/james/audit-tmp/xm031b_b3r1.diff`，6149 字节，SHA-256 `3967a92b09840dda73033e5bd01d1ddd4880608b9466b790d044653d73955a6b` |
| 两棵 tree | `git diff --binary 70775ec209d7c10729b8d35f11369b3e716f2ef9 b0073bee235305853d226ddc84785805ec390b99` 与冻结 diff `cmp` 相同，SHA-256 相同 |
| 工作区验证过的 diff | `/home/james/audit-tmp/xm031b_b3_scratch.diff` 与冻结 diff SHA-256 相同，`cmp` 相同 |
| 统计 | `git diff --numstat`：2 文件，`6 0` 与 `69 0`，合计 75 行新增、0 行删除 |
| 模式 | `--raw` 两侧都是 `100644`。没有旧模式/新模式行 |
| 写入前 blob | `AuditEventService.java` 与 `TraceabilityJdbcIntegrationTests.java` 在 `4a066def` 与 before tree `70775ec` 上分别是 `12a6a8300cfdeea67c4e7c352fb7bca81ee1b807`、`aa0f0502c2fccc7043e89b03595ac84a7f3f3a32`，逐字节相同 |
| after blob | `25cd91bef09435836cfa9108da7c7a1be29ed57d`、`f8f03ac118c02bd6d459430857f18551b2e30e77`。主工作区这两个路径的 `git hash-object` 与 after tree 相同 |
| 其他路径 | 这两棵 tree 之间没有第三个文件 |

## 定稿边界（本席对照用，不重开）

B2-r2 双 PASS 定稿改了两句，其余沿用 B2-r1：

- 生产只改 `AuditEventService.recordFailure`，加 `@Transactional(propagation = REQUIRES_NEW)`。`record()`、`recordSuccess`、成功明细不加。不摘调用方注解，不改 M006-G-07，不改 `IdempotencyService`，不改连接池。
- 回归放在 `TraceabilityJdbcIntegrationTests`。方法级 `NOT_SUPPORTED`。另注入容器里的 `AuditEventService`、`AccessService`、`PlatformTransactionManager`。不 `DELETE`，不 `TRUNCATE`。按本次唯一键断言。
- 三条：同一主体对缺失用户做 `assignUserRole` 与 `assignUserPosition`，两条失败事件各 1 行；打开的事务里 SQL 失败后 `recordFailure(...); throw 原异常;`，不包内层 try，不用缺失用户的定岗代替；会回滚的事务里记下的成功事件 0 行。
- 宿主类夹具无提交写。赋角色失败另留 `idempotency_records` 一行。定岗、事务中止、成功回滚不留幂等行。断言仍只打审计表。
- 不改这个类已有的方法、`@AfterEach`、`EvidenceEventJdbcIntegrationTests`、`AuditEventServiceTests`。

## 逐块归因

机械行（`diff --git`、`index`、`---`、`+++`、`@@`）是两块的块头，没有产品内容，随块归入 XM-M031-B-001。下面的行号是 after 文件行号。75 行新增每一行都在表内。删除行 0。越界行 0。

| 块 | after 行 | 新增 | ID | 行在定稿里的位置 |
|---|---|---:|---|---|
| A | `AuditEventService.java` 59–64 | 6 | XM-M031-B-001 | 59–62 说明独立事务与成功路径仍随调用方回滚；63–64 只在 `recordFailure` 上加 `REQUIRES_NEW` |
| B1 | `TraceabilityJdbcIntegrationTests.java` 133–134 | 2 | XM-M031-B-001 | 段注释：只追加、按本次唯一键、不清理 |
| B2 | 同文件 135–143 | 9 | XM-M031-B-001 | 三个容器 Bean：`AuditEventService`、`AccessService`、`PlatformTransactionManager` |
| B3 | 同文件 144–149 | 6 | XM-M031-B-001 | `auditRows`：`event_name` 加 `idempotency_key` 的 `COUNT(*)`，参数用占位符 |
| B4 | 同文件 150–168 | 19 | XM-M031-B-001 | 方法级 `NOT_SUPPORTED`。同一主体、缺失用户，赋角色与定岗各失败一次，两条失败事件各 1 行 |
| B5 | 同文件 169–187 | 19 | XM-M031-B-001 | 方法级 `NOT_SUPPORTED`。`TransactionTemplate` 里 `SELECT 1/0` 失败后 `recordFailure` 再 `throw original`，外面断言原异常种类与失败事件 1 行 |
| B6 | 同文件 188–201 | 14 | XM-M031-B-001 | 方法级 `NOT_SUPPORTED`。回滚事务里的 `recordSuccess` 按本次键断言 0 行 |

6+2+9+6+19+19+14 = 75。

块头里的未改上下文（生产侧 `recordFailure` 的原签名三行；测试侧 `regclass` 与类的结束括号）行数不变，不是新增。

## 内容审计

### 生产注解

after 全文只多了第 59–64 行。`recordFailure` 仍是原来的七参公开方法，方法体仍是调用私有 `record(...)`。类上只有 `@Service`。`recordSuccess` 的两个重载（第 27–38 行、第 44–57 行）和私有 `record`（第 79–135 行）都没有事务注解。diff 没有第二处生产文件，调用方、`IdempotencyService`、连接池、M006-G-07 都不在这个 diff 里。

注解只有 `propagation = REQUIRES_NEW`，没有隔离级别、超时、`rollbackFor`。类不是 `final`，方法是 `public`，类内没有 `this.recordFailure`。注入进来的是容器代理，私有 `record` 跑在这个新事务里面。

`audit_events` 建表（`V002`）以及后续 `V045` 的 `merchant_id`、`V101` 的类目与主体列都没有 `REFERENCES`。这次独立提交不会因为审计表自己的外键去等调用方还没提交的行。

### 注释与出处

第 60–61 行写的是：失败写入走独立事务；调用方回滚或事务已因 SQL 错误中止时失败记录还在，并且不再被「当前事务已中止」顶掉原异常；成功事件仍随调用方事务回滚。这与 after 源码一致：注解只在 `recordFailure` 上，成功路径没有这层注解。

出处对过 HTML 原文：

- 治理地图第 893 行是「谁发起……结果是什么，都要能查到」。
- 第 899 行是「权限变更、临时授权和敏感权限使用」。
- 第 902 行是「终端开关启用、停用、灰度开放和被拒绝访问」。
- 业务地图第 1583 行是「核销成功、失败、拒绝、撤销」。

四行都在，和定稿分类格引用的是同一句。注释没有另写一条产品规则。

### 回归宿主与旧夹具

测试 diff 从原第 130 行的 `regclass` 之后插入，`numstat` 删除 0。类上的 `@SpringBootTest` 数据源三行、类级 `@Transactional`、原有三个字段、`@AfterEach clearActor`、原有五个测试、`responsibilityFor`、`regclass` 都还在原处。文件模式仍是 `100644`。`EvidenceEventJdbcIntegrationTests` 与 `AuditEventServiceTests` 不在 diff 里。

新增 SQL 只有 `SELECT COUNT(*) ... WHERE event_name = ? AND idempotency_key = ?` 和 `SELECT 1/0`。没有 `DELETE`，没有 `TRUNCATE`。

三个字段都是 `@Autowired`：`AuditEventService`（同包，容器里的 `@Service`）、全限定的 `AccessService`、全限定的 `PlatformTransactionManager`。RED 能跑到断言，说明这三个 Bean 注得进、上下文起得来。

### 三条断言，修复前红还是守边界

定岗与赋角色写在同一个 `NOT_SUPPORTED` 方法里。`AccessService` 类上没有类级 `@Transactional`。`assignUserRole`（第 341–362 行）方法上没有事务注解，catch 是 `recordFailure("access.role.assign_failed", ...)` 再抛。`assignUserPosition`（第 577–599 行）有方法级 `@Transactional`，catch 是 `recordFailure("access.user_position.assign_failed", ...)` 再抛。请求记录的第一列是 `idempotencyKey`，测试把本次键放在第一列，目标用户是 `usr_xm031b_missing`。`TestSecurityActors.asPlatformAdmin(String)` 只写 `SecurityContextHolder`，不写库。

RED 日志（`xm031b_b3_red.mvn.log` 第 717、741–746、2450–2453 行）把这个类记成 8 项测试、2 项失败、0 错误、0 跳过。8 = 原有 5 项 + 新增 3 项。失败点：

- `failureEventOfATransactionalWriteSurvivesItsRollback` 第 167 行：`expected: 1 but was: 0`。第 167 行是定岗那条 `isEqualTo(1)`。同一方法里第 166 行赋角色的 `isEqualTo(1)` 先执行并通过，否则失败点会停在 166。所以修复前赋角色已经是 1 行，定岗是 0 行。定岗这条修复前会失败；赋角色这条守住「无事务路径仍是 1 行」。0 行也说明计数发生在业务事务结束之后：若测试方法仍参加类级事务，未提交的失败插入在同一条连接上仍能数到，断言不会先红。
- `failureEventIsKeptAndTheOriginalErrorSurvivesWhenTheCallerTransactionIsAborted` 第 185 行：期望 `DataIntegrityViolationException`，实际是 `UncategorizedSQLException`，SQL 状态 `25P02`，原文 `current transaction is aborted, commands ignored until end of transaction block`，语句是 `INSERT INTO audit_events`。失败点在种类断言，还没走到第 186 行的行数断言。插入抛在 `recordFailure` 上，说明它和 `SELECT 1/0` 用的是同一条已经中止的事务，不是另一条自动提交的连接。

事务中止方法的形状是：`TransactionTemplate(transactionManager).executeWithoutResult` 里面，`try` 只包住 `SELECT 1/0`；`catch (DataAccessException original)` 里先 `recordFailure(...)` 再 `throw original`。`recordFailure` 不在内层 `try` 里。这不是缺失用户的定岗。`TransactionTemplate` 的默认传播是 `REQUIRED`，测试方法是 `NOT_SUPPORTED`，所以模板自己开一条事务。修复前种类断言会红。修复后这条测试能通过，才同时说明原异常种类是 `DataIntegrityViolationException`、并且失败行是 1（第 186 行在种类断言之后，整段测试不再出现在失败名单里）。

成功回滚方法不在这两条失败里。类 8 项里只红了上面两条，所以 `successEventOfARolledBackTransactionIsNotKept` 在只落测试补丁时已经通过：`recordSuccess` 写在会回滚的 `TransactionTemplate` 里，抛 `IllegalStateException`，回滚之后按本次键数到 0。它守的是不要把 `REQUIRES_NEW` 加到 `recordSuccess`。修复前它不该红，日志里也没有红。

### 红绿日志与库

本席没有重跑 Maven。读了 `/home/james/audit-tmp/xm031b_b3_run.sh` 和日志。脚本在 `origin/main` 的一次性 worktree `/home/james/scratch-xm031bb3` 上建库 `xm031bb3_test`，只在该 worktree 里把测试源码中的 `5432/complex_test` 改成这只库，跑完 `DROP DATABASE`，再 `git checkout -- backend`。现在该 worktree 路径不存在。日志里的 JDBC URL 是 `jdbc:postgresql://127.0.0.1:5432/xm031bb3_test`。

RED：`mvn -q -o test` 的 surefire 汇总是 `Tests run: 3539, Failures: 2, Errors: 0, Skipped: 17`。两条失败就是上面两个新方法，消息见 `xm031b_b3_red.msgs`。GREEN：同一脚本在测试加生产补丁之后取 Maven 退出码，`xm031b_b3_run.log` 写 `MVN_EXIT(green)=0`，并由 surefire XML 汇总成 `suites=263 tests=3539 red=0`。`xm031b_b3_green.red` 与 `xm031b_b3_green.msgs` 都是 0 字节。`-q` 成功时不打印 `Tests run` 汇总，所以 green 的 mvn 日志尾部没有这一行；0 红来自退出码和 surefire 解析，不是本席重跑。3539 比 ledger 所记 origin/main 全量 3536 多 3，与新增三个测试方法一致，套件数仍是 263。

GREEN 为 0 红，说明生产补丁之后：定岗失败事件变为 1 行，事务中止后原异常种类保持 `DataIntegrityViolationException` 且失败事件 1 行，成功回滚仍是 0 行，原有五个追溯测试和其他套件没有被这三条提交下来的审计行打红。赋角色留下的幂等占位是定稿已经写明的副作用，测试没有为它 `DELETE`。

### 有没有新缺陷或新业务

生产 diff 只有这一处传播注解和与之相符的四行注释。没有新事件名、没有改 catch 的业务分支、没有改成功路径。测试里的 `xm031b.*` 事件名只出现在回归方法里，用来把事务中止和成功回滚钉在本次键上，不进入生产调用链。

没有看到会让修复前该红的断言变绿的写法：定岗计数在业务方法抛出之后；事务中止的 `recordFailure` 在尚未结束的、已被 `25P02` 证明已中止的事务里，且没有内层 try。也没有看到把 `REQUIRES_NEW` 加宽到 `record` / `recordSuccess` 的写法：成功那条在只落测试时已经是 0 行，GREEN 仍是 0 红。

## 票

归因票：PASS。75 行新增全部归入已定稿且归属为「是」的 XM-M031-B-001。越界 0。删除 0。文件模式未变。没有第三文件。

内容票：PASS。实现与 B2-r2 定稿的最小处置和本席 B2-r1、B2-r2 写死的边界一致。注释与源码、HTML 行号一致。三条断言里，定岗行数与事务中止的原异常种类在修复前会失败；赋角色 1 行与成功回滚 0 行在修复前已经成立，用来守边界。未引入新的产品行为。

总票：PASS。

## 冻结 diff 全文

下面是冻结文件原文，与两棵 tree 的 `git diff --binary` 逐字节相同。

```diff
diff --git a/backend/src/main/java/com/complex/platform/evidence/AuditEventService.java b/backend/src/main/java/com/complex/platform/evidence/AuditEventService.java
index 12a6a830..25cd91be 100644
--- a/backend/src/main/java/com/complex/platform/evidence/AuditEventService.java
+++ b/backend/src/main/java/com/complex/platform/evidence/AuditEventService.java
@@ -56,6 +56,12 @@ public class AuditEventService {
                 metadata, eventCategory, subjectType);
     }
 
+    /**
+     * 失败事件在独立事务里写入（XM-M031-B）：调用方事务回滚或已因 SQL 错误中止时，失败记录仍须留住、且不得顶掉原异常
+     * （治理地图第 893、899、902 行；BSM 第 1583 行）。成功事件仍随调用方事务，业务回滚时一起回滚。
+     */
+    @org.springframework.transaction.annotation.Transactional(
+            propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
     public AuditEvent recordFailure(
             String eventName,
             String permissionCode,
diff --git a/backend/src/test/java/com/complex/platform/evidence/TraceabilityJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/evidence/TraceabilityJdbcIntegrationTests.java
index aa0f0502..f8f03ac1 100644
--- a/backend/src/test/java/com/complex/platform/evidence/TraceabilityJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/evidence/TraceabilityJdbcIntegrationTests.java
@@ -130,4 +130,73 @@ class TraceabilityJdbcIntegrationTests {
     private String regclass(String tableName) {
         return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
     }
+
+    // ===== XM-M031-B：失败留痕不随调用方事务回滚丢失（audit_events 只追加，V140；按本次唯一键断言，不清理） =====
+
+    @Autowired
+    private AuditEventService auditEventService;
+
+    @Autowired
+    private com.complex.platform.access.AccessService accessService;
+
+    @Autowired
+    private org.springframework.transaction.PlatformTransactionManager transactionManager;
+
+    private int auditRows(String eventName, String idempotencyKey) {
+        return jdbcTemplate.queryForObject(
+                "SELECT COUNT(*) FROM audit_events WHERE event_name = ? AND idempotency_key = ?",
+                Integer.class, eventName, idempotencyKey);
+    }
+
+    @Test
+    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
+    void failureEventOfATransactionalWriteSurvivesItsRollback() {
+        TestSecurityActors.asPlatformAdmin("usr_xm031b");
+        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
+        String plainKey = "xm031b-plain-" + suffix;
+        String transactionalKey = "xm031b-tx-" + suffix;
+        // 不带事务的赋角色与带 @Transactional 的定岗，对同一个不存在的用户各失败一次。
+        org.assertj.core.api.Assertions.assertThatThrownBy(() -> accessService.assignUserRole(
+                        new com.complex.platform.access.dto.RoleAssignmentRequest(plainKey, null, "usr_xm031b_missing", "role_xm031b_missing")))
+                .isInstanceOf(com.complex.platform.common.api.ApiException.class);
+        org.assertj.core.api.Assertions.assertThatThrownBy(() -> accessService.assignUserPosition(
+                        new com.complex.platform.access.dto.UserPositionAssignmentRequest(transactionalKey, null, "usr_xm031b_missing",
+                                "ops_clerk", "lvl_m1_lower", null, null, null)))
+                .isInstanceOf(com.complex.platform.common.api.ApiException.class);
+        assertThat(auditRows("access.role.assign_failed", plainKey)).isEqualTo(1);
+        assertThat(auditRows("access.user_position.assign_failed", transactionalKey)).isEqualTo(1);
+    }
+
+    @Test
+    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
+    void failureEventIsKeptAndTheOriginalErrorSurvivesWhenTheCallerTransactionIsAborted() {
+        String key = "xm031b-abort-" + java.util.UUID.randomUUID().toString().substring(0, 8);
+        // 与生产 catch 同形：recordFailure(...); throw original; 不包内层 try。
+        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
+                        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
+                            try {
+                                jdbcTemplate.queryForObject("SELECT 1/0", Integer.class);
+                            } catch (org.springframework.dao.DataAccessException original) {
+                                auditEventService.recordFailure("xm031b.aborted_write_failed", "xm031b.probe", "xm031b_probe",
+                                        key, null, key, "DIVISION_BY_ZERO");
+                                throw original;
+                            }
+                        }))
+                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
+        assertThat(auditRows("xm031b.aborted_write_failed", key)).isEqualTo(1);
+    }
+
+    @Test
+    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
+    void successEventOfARolledBackTransactionIsNotKept() {
+        String key = "xm031b-success-" + java.util.UUID.randomUUID().toString().substring(0, 8);
+        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
+                        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
+                            auditEventService.recordSuccess("xm031b.rolled_back_write", "xm031b.probe", "xm031b_probe",
+                                    key, null, key, java.util.Map.of());
+                            throw new IllegalStateException("业务回滚");
+                        }))
+                .isInstanceOf(IllegalStateException.class);
+        assertThat(auditRows("xm031b.rolled_back_write", key)).isZero();
+    }
 }
```
