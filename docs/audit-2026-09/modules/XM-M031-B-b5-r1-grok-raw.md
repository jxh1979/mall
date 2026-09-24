CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# XM-M031-B B5-r1 修复后整模块三线全量重审（Grok 席乙）

版本：V0.01
席位：Grok 独立只读审计席乙，grok-4.7 / xhigh
步骤：B5-r1（B3 累计 1）
正本：/home/james/complex-management main 主工作区
结论：PASS
本次新问题：0

未读 modules/XM-M031-B-b5-r1-self-raw.md。未用 MCP、子代理或第三席。未改业务代码、HTML、锁、findings、ledger。未 fetch / commit / push / merge / deploy。未对 complex、complex_test 执行写语句。

## 1. 锁与范围

独立实算 `sha256sum docs/audit-2026-09/plan/criteria-lock.md`，结果即首行。锁文件版本头为 V0.14。M092 与 M095 的映射列表相同，并集 46 项：BEQR-063、BEQR-138、BEQR-139、BEQR-140、BEQR-141、BEQR-142、BSM-001、BSM-003、BSM-022、BSM-028、BSM-093、BSM-094、BSM-095、BSM-096、BSM-097、BSM-114、BSM-115、BSM-120、GEN-001 至 GEN-018、PGM-001、PGM-014、PGM-023、PGM-026、PGM-037、PGM-061、PGM-062、PGM-063、PGM-064、PGM-066。

开工段 6 个文件加 B3 改到的 TraceabilityJdbcIntegrationTests，均在主工作区全读。主工作区这 7 个文件相对 origin/main（4a066def20758a4d8b06de619c3515cf526d253b）的 `git diff --binary` 与冻结 diff `/home/james/audit-tmp/xm031b_b3r1.diff` 逐字节相同，两边 SHA256 都是 3967a92b09840dda73033e5bd01d1ddd4880608b9466b790d044653d73955a6b（6149 字节，2 files changed, 75 insertions）。其余 5 个文件相对 origin/main 无差异。

| 文件 | 主工作区行数 |
|---|---:|
| backend/src/main/java/com/complex/platform/evidence/AuditEventService.java | 136 |
| backend/src/main/java/com/complex/platform/evidence/AuditEventRepository.java | 18 |
| backend/src/main/java/com/complex/platform/evidence/JdbcAuditEventRepository.java | 111 |
| backend/src/main/java/com/complex/platform/evidence/AuditedOperationExecutor.java | 188 |
| backend/src/main/java/com/complex/platform/evidence/AuditEvent.java | 53 |
| backend/src/test/java/com/complex/platform/evidence/AuditEventServiceTests.java | 58 |
| backend/src/test/java/com/complex/platform/evidence/TraceabilityJdbcIntegrationTests.java | 202 |

判断所需的调用方与表，已读：AccessService.assignUserRole（341 行起，无 @Transactional，352 行 recordFailure）、assignUserPosition（577 行 @Transactional，589 行 recordFailure）；IdempotencyService 构造与 runAndComplete（35–55、160–218 行）；AuditedOperationExecutor.executeWrite（113–155 行）；AuthService 开发构造（63–64 行 `new AuditEventService(event -> event)`）；audit_events 定义 V002、V045、V101、V140，并在一次性空库上 `\d audit_events`。

findings.md 现为 V4.05：XM-M031-B-001 修复队列「否（已修）」，当前「是」为 0。本次不把该「否」当成整模块已全对，下面按现码与现跑重核。

## 2. 技能（按环境实核，未调用的不记成已调用）

已读并按其与 V0.17 §6 收窄后的口径用了：using-superpowers（开工核对技能目录存在；划掉的子代理技能未调用）、verification-before-completion（PASS 之前先跑完下面的变异与全量）、grilling（追问自己做，不派子代理、不问操作员）、diagnosing-bugs 与 systematic-debugging（以回归测试为红绿回路：去掉注解复现 25P02 与 0 行，加回注解后全绿；未往生产代码加插桩）、codebase-design（事务边界收在 recordFailure 一个公开方法上，record / recordSuccess 仍并入调用方）、long-running-background-tasks（全量 `mvn -o test` 后台跑完并读到 BUILD SUCCESS；未另写监视脚本）。

本步是 B5，未改代码，故未用 ponytail 做修复、未用 tdd / test-driven-development 新写测试、未用 requesting-code-review / receiving-code-review、未遇到合并故未用 resolving-merge-conflicts。writing-for-agents 只体现在本 raw 的可复核写法，未另写收口文件。brainstorming、writing-plans 属于阶段 A，本步未用。graft 技能已读；本审要求全读文件与 46 项全文，未用 graft 的截断检索代替读源。i-have-adhd 的 CLI 首行模式与「CLI 不回正文」冲突，正文只留在本文件。

缺口：本机无 spring-tx-7.0.8-sources.jar，传播语义不引未读到的框架原文，以实测为准。git-guardrails-claude-code 未作为本步动作调用；实际约束是未 fetch / commit / push / merge / reset。batch-grill-me、ubiquitous-language 未单独开跑。

## 3. 第一性三问

recordFailure 上的 `@Transactional(propagation = REQUIRES_NEW)`（AuditEventService.java 59–64 行）为什么必须存在：治理地图 893 行要求重要动作的结果能查到，899 行点名权限变更，902 行点名被拒绝访问，960 行要求凡有过程的业务动作都留痕；BSM 1583 行把核销的失败与拒绝列进必须关注的动作。带 @Transactional 的写方法在 catch 里调用 recordFailure 时，若插入与业务同一事务，失败行会随回滚消失，上述句子在这条路径上做不到。去掉这 6 行注解后，同一条回归里定岗失败事件为 0 行（见第 5 节）。成功事件不在此列：record / recordSuccess 无该注解，业务回滚时一并回滚，由第三条回归钉住。

声称的行为有没有被执行验证：有。第 5 节两次实跑都在 origin/main 一次性 worktree 加一次性空库上，JAVA_HOME 为 Temurin 21。无注解时两条失败回归红；注解按主工作区原文件加回后，TraceabilityJdbcIntegrationTests 8 测 0 失败，后端全量 263 套 3539 测 0 失败。

哪条前提是拿来当真的：生产调用经 Spring 代理才套上 REQUIRES_NEW。范围内类内无 recordFailure 自调用。唯一直接 `new AuditEventService` 的生产构造是 AuthService.java 64 行，仓储为 `event -> event`，不落库。内存单测 `new AuditEventService(repository)` 同样不经代理，只断言字段，不声称传播。audit_events 无外键、INSERT 不触发只追加触发器，这两条用空库 `\d` 核对过，不是沿用旧票。

## 4. 焦点行为

失败事件是否留住。assignUserRole（AccessService.java 341 行）无方法级事务，catch 在 352 行记 `access.role.assign_failed`。assignUserPosition（577 行）有 @Transactional，catch 在 589 行记 `access.user_position.assign_failed`。回归 TraceabilityJdbcIntegrationTests.java 151–168 行对同一个不存在的用户各失败一次，按本次唯一键断言两行都是 1。无注解时第二行断言 `expected: 1 but was: 0`（167 行）。有注解时该方法在全量里通过（surefire 时间 0.025 s，无 failure 子节点）。

事务中止时原异常是否还被顶掉。回归 170–187 行用 TransactionTemplate 执行 `SELECT 1/0`，catch 里按生产形状 `recordFailure(...); throw original;`，不包内层 try，断言抛出的是 DataIntegrityViolationException，且失败事件 1 行。无注解时实际抛出 UncategorizedSQLException，SQL state 25P02，原文 `ERROR: current transaction is aborted, commands ignored until end of transaction block`，插入语句就是 audit_events 的 INSERT（断言在 185 行）。有注解时该方法通过（0.188 s）。

成功事件是否仍随业务回滚。recordSuccess 两处重载（27–57 行）都进 private record（79 行），无事务注解。回归 190–201 行在会回滚的事务里 recordSuccess 再抛 IllegalStateException，断言该键 0 行。无注解的那一轮此类 8 测里只有上述两条失败，这条成功回归未红。有注解的全量里它通过（0.002 s）。注解没有加到 record 或 recordSuccess。AuditedOperationExecutor.recordDetail（161–165 行）走 recordSuccess，仍与调用方事务同进退。

独立事务的自锁、连接与测试串扰。空库 `\d audit_events`：主键 id，idempotency_key 只是普通索引 `idx_audit_events_idempotency_key`，无 UNIQUE、无 FOREIGN KEY。触发器仅 `trg_append_only_ud BEFORE DELETE OR UPDATE`，调用 `complex_block_mutation()`，INSERT 不触发。失败插入不需要等待调用方未提交的父行，也没有插入触发器去锁调用方正在改的表。连接池多借一个连接仍是 B2 已登记的观察：全量 52.96 s 结束，无等连接、无死锁、无超时。类级事务测试在失败路径上会留下已提交的失败行，B2 要求全量确认没有精确计数被碰到；本次全量 0 失败，该项没有新证据要升队。

回归断言能否失败。能。worktree 里只删掉 59–64 行注解、测试与空库保持不动，`mvn -o -Dtest=TraceabilityJdbcIntegrationTests test` 结果是 Tests run: 8, Failures: 2。两条红就是上面的 0 行与 25P02。其余 6 条（含成功回滚与原有四条溯源）未红。随后把主工作区 AuditEventService.java 拷回 worktree（`cmp` 一致，REQUIRES_NEW 在 64 行），丢掉该库并重建同名空库，再跑全量。

新引入问题。未发现应入队的新缺陷。注解只加在 recordFailure。插入列与表列一致（JdbcAuditEventRepository.java 24–33 行对照 `\d` 的 17 列）。幂等键不是唯一约束，失败行留下后再成功记一行不会撞唯一键。

## 5. 取证

环境：主工作区默认 JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 没有 javac，`mvn` 报 `release version 21 not supported`。实跑改用 /usr/lib/jvm/temurin-21-jdk-amd64，`java -version` 为 21.0.12.1 Temurin。口令只从 /etc/complex/complex.env 的对应行读入环境变量，不回显、不写入本文件。

worktree：`git worktree add --detach /home/james/scratch-xm031b-g5 origin/main`，HEAD 4a066def。`git apply /home/james/audit-tmp/xm031b_b3r1.diff` 成功。库名 xm031b_grok_b5，在 postgres 库上 CREATE DATABASE，未对 complex、complex_test 写。COMPLEX_DATABASE_URL 只把库名从 complex 换成该库；SETTLE_TEST_DB_URL=jdbc:postgresql://127.0.0.1:5432/xm031b_grok_b5。worktree 内 110 个测试文件的 `5432/complex_test` 改成 `5432/xm031b_grok_b5`。改完后 worktree 内该字面量为 0。

变异（注解已删，库已由该轮 Flyway 建到 v432，日志写明 Successfully applied 399 migrations）：

```
Tests run: 8, Failures: 2, Errors: 0, Skipped: 0
failureEventOfATransactionalWriteSurvivesItsRollback:167
  expected: 1
   but was: 0
failureEventIsKeptAndTheOriginalErrorSurvivesWhenTheCallerTransactionIsAborted:185
  Expecting ... DataIntegrityViolationException
  but was: UncategorizedSQLException ... SQL state [25P02] ...
  ERROR: current transaction is aborted, commands ignored until end of transaction block
```

全量（注解已按主工作区文件恢复，空库已重建，`mvn -o test`，MVN_EXIT=0）：

```
suites=263 tests=3539 failures=0 errors=0 skipped=17 red=0
BUILD SUCCESS
TraceabilityJdbcIntegrationTests Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
```

三条回归在该次 surefire 中均无 failure/error 子节点：failureEventOfATransactionalWriteSurvivesItsRollback、failureEventIsKeptAndTheOriginalErrorSurvivesWhenTheCallerTransactionIsAborted、successEventOfARolledBackTransactionIsNotKept。AuditEventServiceTests 2 测 0 失败。

17 条跳过是既有 `assumeTrue`，要求 JDBC URL 的库名正好是 complex_test：M006JdbcRegressionTests.java 31 行 13 条、M006StoredValueJdbcTests.java 35 行 4 条。按 D-032 把库指到空库后它们不执行。这不是本轮注解引入的，也不是失败。席甲 B3 记录的 3539 测 0 红与本次计数同一口径（含跳过、红为 0）。

收尾：worktree `git checkout -- .` 后 `git clean -fdx`，status 行数为 0，`git worktree remove` 未加 --force，路径已消失。只 DROP xm031b_grok_b5。之后 `pg_database` 中仍有 complex 与 complex_test，没有 xm031b_grok_b5。/tmp 下本次环境文件已删。

## 6. 顺从 46 项

每条要么给出范围内证据，要么写明无本面（范围内文件不实现该锚点，也没有与原文相反的分支）。HTML 原文以 criteria-lock 定义区为准，此处不整段复抄。

GEN-001 适用。删掉 59–64 行后，带事务写方法的失败事件为 0 行，893/899/902/960 行与 BSM 1583 行在该路径上落空。注解留着。

GEN-002 适用。三条回归与全量 0 失败，和第 4 节读到的传播一致。内存单测只覆盖字段，不冒充传播证据。

GEN-003 适用。两条失败回归在无注解时失败、有注解时通过，断言能失败。成功回归在两种情况下都要求 0 行，用来拦住把 REQUIRES_NEW 加宽到成功路径。

GEN-004 适用面在查询参数。JdbcAuditEventRepository.searchOperationEvents（66–87 行）过滤项全部绑定参数，merchantScope 非空才加 `merchant_id = :merchant_scope`。谁有权把 scope 传 null 不在这 7 个文件里。

GEN-005 无本面。这 7 个文件不发权限、不设岗位。留痕查询的授权在调用方。

GEN-006 适用。业务写入与成功事件仍在调用方事务里，回滚则一起没。失败留痕按 960 行必须留下，不是半截业务数据。表无外键，失败插入不会留下指向未提交父行的约束悬空。

GEN-007 适用。audit_events.idempotency_key 不是唯一约束，同一键可以先有失败行再有成功行。IdempotencyService 占位与 markRetryableFailure 的事务边界未改，见第 8 节维持项。

GEN-008 适用。INSERT 列与 `\d` 列一致，含 V045 merchant_id 与 V101 event_category、subject_type。AuditEvent 15 参构造把后两列置 null（32–51 行），与可空列一致。

GEN-009 适用。25P02 顶掉原异常的路径已由独立事务拆掉，全量中原异常种类断言通过。recordFailure 自身若在插入时失败仍会把异常抛给调用方；那是插入失败，不是已中止事务上的下一句。B2 定稿没有要求吞掉插入自身的失败，本次不另加要求。

GEN-010 适用。写入与查询都是绑定参数。109 行起的鉴权提取失败被吃掉，注释写明不影响被审计操作；业务异常不在这个 catch 里。未发现新的明文秘密或把失败记成成功。

GEN-011 适用。失败结果现在留在 audit_events，可按 event_name + idempotency_key 数到。成功结果仍只在业务事务提交后留下。

GEN-012 适用。REQUIRES_NEW 在方法返回前提交并归还连接，不是泄漏。池耗尽仍只登记，全量没有出现该症状。

GEN-013 适用到本次实跑。`-o` 离线、JDK 21 可编译。JRE-only 的 java-21-openjdk 没有 javac，这是本机运行时事实，不是模块缺陷。

GEN-014 无本轮迁移。现表与插入语句一致；V140 只追加触发器仍在，INSERT 不受阻。

GEN-015 无本面。范围内无界面。

GEN-016 适用。新注释引用的 893、899、902、960、1583 行与当前 HTML 该行原文一致（893 行「结果是什么，都要能查到」；899 行「权限变更、临时授权和敏感权限使用」；902 行「终端开关启用、停用、灰度开放和被拒绝访问」；960 行「所有有过程的业务动作都必须留痕」；1583 行「核销成功、失败、拒绝、撤销」）。

GEN-017 适用。独立事务是已有留痕句子的实现机制，无新业务实体、状态或权限。不构成添加。分类顺序仍是细化（上位句子在，工程缺陷原已入队并已修）。

GEN-018 适用。本文件结论落在 HTML 原行、变异输出、全量输出和空库 `\d` 上。

BEQR-063 无本面。沽清、免单、双证据不在这 7 个文件。

BEQR-138 无本面。风险四级不在这 7 个文件。

BEQR-139 无本面。复核状态机（已记录、待补充、待复核等）不在这 7 个文件。event_result 的 success/failure 是动作结果，不是该状态机。只追加触发器与「不能删除」同向，见 BSM-096。

BEQR-140 无本面。主管复核强制项清单不在这 7 个文件。

BEQR-141 无本面。经营异常处置状态不在这 7 个文件。

BEQR-142 无本面。异常单的更正或冲正出口不在这 7 个文件。

BSM-001 无本面。策略可配置、版本化计费不在这 7 个文件。

BSM-003 部分适用。锚点标题下的通用改删规则不由审计表承担。audit_events 禁 UPDATE/DELETE 与更具体的留痕「不能删除」一致，见下条 BSM-096。没有把业务单据改成不可改。

BSM-022 无本面。卡片只指向留痕子系统，具体句子在 BSM-093 起。

BSM-028 无本面。综合体、合同、红线七类的实现不在这 7 个文件。幂等占位行为未改。

BSM-093 适用。专章要求有过程的动作留下记录。失败路径现在留得住，证据在第 5 节。

BSM-094 部分适用。清单里的具体动作由各调用方决定记什么。本范围提供的是写入与按条件查询，不缺少清单中的某一种业务记录器，也不与「核销失败也要记」相反。

BSM-095 部分适用。1583 行附近的自动填写、1599 行「一律拒绝并留痕」里的地理范围判断不在这 7 个文件。留痕这一半由 recordFailure 承担，且不再随业务回滚丢失。

BSM-096 适用。「普通动作只记录」由 record/recordSuccess 与 recordDetail 承担。「留痕记录默认永久保存、不能删除」与 V140 触发器一致：空库上该触发器仍是 BEFORE DELETE OR UPDATE。高风险进复核队列、人工补录时限、清理留存的最高权限入口不在这 7 个文件。

BSM-097 无本面。风险四级、异常预警页面、事件状态机不在这 7 个文件。

BSM-114 无本面。各入口谁能看、谁能做，不在这 7 个文件。

BSM-115 无本面。子系统之间的连接表不在这 7 个文件。

BSM-120 无本面。购票、团购、订台、存酒等已拍板规则不在这 7 个文件。同文件 1995 行的留痕总句由 PGM-066 同一句覆盖，机制见 GEN-011。

PGM-001 部分适用。「最后管责任」是留痕的上位句子，本范围负责把动作结果写入可查询的 audit_events。「全链可点击」是展示规则，范围内无界面。searchOperationEvents 提供按业务对象、操作人、事件名、时间与商户的查询（14–17、66–87 行），不制造孤立写入。

PGM-014 无本面。卡片摘要；复核队列不在这 7 个文件。

PGM-023 无本面。楼层、点位、平面图发布不在这 7 个文件。

PGM-026 无本面。商户档案、招商、主体权责不在这 7 个文件。

PGM-037 无本面。设备台账与手机管控不在这 7 个文件。

PGM-061 适用。893 行「结果是什么，都要能查到」。失败结果现在查得到，成功结果仍随业务提交。

PGM-062 适用。899 行权限变更、902 行被拒绝访问。assignUserRole / assignUserPosition 的失败事件在全量中留存。清单里其余动作的事件名由各自调用方传入，本范围不改那些调用方。

PGM-063 无本面。免单、退款、调账等复核清单不在这 7 个文件。

PGM-064 无本面。正式挂起订单不在这 7 个文件。

PGM-066 适用。960 行「所有有过程的业务动作都必须留痕」由本范围的失败/成功分界实现：失败留下，成功随业务回滚则不留。界面全中文见第 8 节终端字面量，维持只登记。下单时间与支付时间、物品流转时间戳不在这 7 个文件。

## 7. 对抗（grilling，自问自答）

去掉注解，定岗失败事件是否回到 0 行，原异常是否再次被 25P02 顶掉。是，第 5 节变异即此。

注解会不会让成功事件也留下。不会。成功回归在全量中通过，要求 0 行；注解只在 recordFailure 上。

INSERT 会不会因为外键或触发器与调用方事务自锁。不会。空库表无外键，触发器不覆盖 INSERT。

全量会不会被留下的失败行打红。没有。0 失败。

类内自调用会不会让注解失效。AuditEventService 里没有对 recordFailure 的自调用。AuthService 64 行的 new 不落库。

errorCode 为 null、终端恒为 web、读路径坏 JSON、IdempotencyService 注释、连接池，有没有新证据要改判。没有。代码位置与 B2 登记时相同，本轮 diff 没有碰它们。见第 8 节。

17 条跳过是不是把回归藏起来了。不是。三条回归都在 TraceabilityJdbcIntegrationTests 里执行完，跳过的是要求库名恰好为 complex_test 的 M006 两类，且该要求与「必须改指空库」的跑法冲突，是既有 assumeTrue，不是本轮测试。

## 8. 问题

本次新问题：0。没有新的临时编号。

以下各项维持 B2 双席「只登记、不入队」。无新证据，不改判，不构成这次 FAIL。

1. IdempotencyService 注释与带事务调用方。原句在 35–36 行与 167–168 行：「reserve 仍在外先提交」「失败留痕在外」。这只相对它自己的 TransactionTemplate 成立。调用方方法再标 @Transactional 时，占位与 markRetryableFailure 并入外层，随业务回滚消失，重试可以再占位。正常触发：带 @Transactional 的写方法失败。分类顺序：细化之下的观察，无新的与 HTML 明文相冲突的业务发明。判据 GEN-007。证据类型：读码（类型 4 的源文），本轮未改该文件。

2. 独立事务多借一个连接。正常触发：调用方事务未结束时进入 recordFailure。分类：技术性观察。判据 GEN-012。全量未出现池等待。不改池。

3. 终端字面量。AuditEventService.java 123 行 `record` 写入 `"web"`。正常触发：任何 recordSuccess / recordFailure。分类：技术性观察（B2 不立案）。判据 PGM-066 界面全中文不直接约束这个库存字段；本次不升为冲突。证据：该行仍在，diff 未改它。

4. errorCode 为空。74–76 行 `Map.of("error_code", errorCode)`，errorCode 为 null 时 Map.of 抛空指针，失败事件写不进，还可能顶掉原异常。正常触发：调用方把 null 传进 recordFailure。B2 记明正常工厂都传非空码。本次调用点未改，无新的 null 调用证据。分类：技术性观察。判据 GEN-009。不升队。

5. 读路径坏 JSON。JdbcAuditEventRepository.java 102–109 行 catch 后返回空 map。正常触发：metadata 列不是对象 JSON。分类：技术性观察。判据 GEN-009 / GEN-010。diff 未改。不升队。

6. 类级 @Transactional 测试会留下已提交的失败行；既有失败断言是「≥1」。这是 B2 交给全量确认的项。本次全量 0 失败，确认没有精确计数被打红。维持只登记。

XM-M031-B-001 本身：队列已是「否」。本次用去掉注解的红与加回注解的绿复核原症状已消失，不把「否」写成「没复核」。

## 9. 结论

PASS

整模块按 B1 三线重审完：7 个文件全读，调用方与 audit_events 表已核对，46 项逐项有适用证据或无本面，焦点五问均有实跑或表结构证据。新问题 0。B2 只登记项不改判。修复队列没有新的「是」。
