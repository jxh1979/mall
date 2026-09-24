CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M031-B B1 修复前全量审计（席乙）

版本：V0.01
席位：Grok，grok-4.7/xhigh，独立只读。
模块：合成模块 XM-M031-B，阶段 B1，修复前。
正本：`/home/james/complex-management` 主工作区。代码与表结构以 `origin/main` `4a066def20758a4d8b06de619c3515cf526d253b` 为准。
主工作区 HEAD 是 `e456ba96c84185c8d39f18f548011daac48a6466`，相对 origin/main 为 ahead 49、behind 76，工作区有未提交改动。本范围 6 个文件 `git diff --quiet origin/main` 均为空，行号与 blob 用这棵树。
锁：`docs/audit-2026-09/plan/criteria-lock.md` 版本 V0.14。本席对工作区该文件执行 `sha256sum`，摘要写在第一行。该文件与 `origin/main` 无差异。sidecar `plan/criteria-lock.sha256` 与 ledger 顶部基准行与这次实算相同。LOCK-AMEND-COUNT 仍是 1。本轮不改锁。
映射：锁内 M092 与 M095 两行（`criteria-lock.md` 6477、6480）列出的 46 个 ID 完全相同，并集 46。只读引用。
未读 `modules/XM-M031-B-b1-self-raw.md`。开工时该路径不存在。未用 MCP、子代理、第三席。未改业务代码、HTML、锁、findings、ledger。未 fetch、commit、push、merge、deploy。未对任何数据库执行语句，包括建库。

## 技能（按本轮实际使用记录）

环境里能找到、本阶段用了的：`using-superpowers`（开工自检）、`systematic-debugging`（只取读错误、对照能留下的路径与留不下的路径、沿调用追到事务连接；本体的插桩和改代码作废）、`diagnosing-bugs`（红环用操作员已落盘的探针日志；本轮禁止建库，没有重跑真库探针）、`grilling`（追问结构自己用仓库事实回答；本体的等人答复和派子代理作废）、`codebase-design`（缝放在 `AuditEventService.recordFailure`，一处改动覆盖调用方；本体的并行另案设计作废）、`verification-before-completion`（声称范围内测试能跑且钉不住回滚之前，先在一次性 worktree 实跑）、`ponytail-review`（处置取一条传播注解，不加第二数据源、不改 46 个调用方）、`writing-for-agents`（本文件就是给另一席读的唯一产物）。

存在但本阶段不调用：`brainstorming`、`writing-plans`（阶段 A）、`test-driven-development` / `tdd`、`requesting-code-review`、`receiving-code-review`（B3/B4）、`resolving-merge-conflicts`（没有合并）、`ponytail` 本体（没有改代码）。`git-guardrails-claude-code` 存在；本席没有 fetch/commit/push/reset。

明确不作证据的：`graft`。它索引的是脏的主工作区，不是 `4a066def`。源码用 `git show origin/main:` 与 `git grep origin/main`。

缺口，没有冒充已调用：`batch-grill-me`、`ubiquitous-language`、`i-have-adhd` 技能文件。`research` 按 V0.17 划掉，未用。

## 范围核对

| 文件 | 行 | blob |
|---|---:|---|
| `backend/src/main/java/com/complex/platform/evidence/AuditEventService.java` | 130 | `12a6a8300cfdeea67c4e7c352fb7bca81ee1b807` |
| `backend/src/main/java/com/complex/platform/evidence/AuditEventRepository.java` | 18 | `07eaa506678b3b148d69013ec654b34d99161875` |
| `backend/src/main/java/com/complex/platform/evidence/JdbcAuditEventRepository.java` | 111 | `b2adff87b9db69b813a8e5fde0766b9c82a1b76b` |
| `backend/src/main/java/com/complex/platform/evidence/AuditedOperationExecutor.java` | 188 | `64bc6e8ce1747a8b11729a67508e988af5faf11f` |
| `backend/src/main/java/com/complex/platform/evidence/AuditEvent.java` | 53 | `230ba0728e39f8c04361b35b3b3610bd3eed81aa` |
| `backend/src/test/java/com/complex/platform/evidence/AuditEventServiceTests.java` | 58 | `c766ea68cc6f20333cfb866d5d91471ee248dcde` |

六个文件都已全文读完。为判断另读了 `IdempotencyService` 的事务模板、`audit_events` 的建表与后续变更、`recordFailure` 的生产调用，以及探针日志。M092 其余 20 个生产文件、M095 其余 5 个测试文件不在本合成模块的写入范围，不在那些文件上开缺陷。

## 第一性三问

这段东西为什么必须存在。删掉 `AuditEventService.record` / `recordFailure`，下面这些已经写在 HTML 里的句子会做不到：失败和拒绝也要留下可查的记录（业务地图第 1583、1599、1845 行；治理地图第 620、893、899、902、960 行；业务地图已确认规则第 1995 行）。成功路径的 `recordDetail` 注释还要求操作明细与业务写入同一事务落库（`AuditedOperationExecutor.java` 第 157–159 行）。所以写入链要留，不能删。

声称的行为有没有被执行验证。范围内两条测试只在内存仓库里断言字段，本席实跑是绿的，但那两条不断言事务。真库行为来自已有探针日志，不是本席新跑的数据库：同一主体、同类失败，不带事务的 `assignUserRole` 失败事件 1 行，带事务的 `assignUserPosition` 失败事件 0 行。旧代码上的同形结果在 `m031_probe.log`。事务已被 SQL 错误中止后再调 `recordFailure`，插入本身抛出、失败行 0，见 `xm031b_probe.log` 第 4 行。

哪条前提没验证就不能当真。不能把「调用了 `recordFailure`」当成「失败事件留在库里」。不能把类级 `@Transactional` 测试里 `COUNT(*) >= 1` 当成「提交后还在」。不能把 `IdempotencyService` 注释里的「失败留痕在外」当成外层还有 `@Transactional` 时审计插入会独立提交。不能把登记里的「47 个文件、27 个带 `@Transactional`」当成 `4a066def` 上的计数，下面按这棵树重数。

## 一、机制与影响面

机制有两截，根子是同一个：`AuditEventService` 上没有任何事务注解。`record` 第 128 行 `repository.save` 使用当前线程已经绑定的 JDBC 连接。`JdbcAuditEventRepository.save` 第 22–52 行就是一条 `INSERT INTO audit_events`。

没有外层事务时，`JdbcTemplate` 按语句自动提交，失败行留下。这是 `assignUserRole`。该方法在 `AccessService.java` 第 341 行，方法上没有 `@Transactional`。它的 catch 调用 `recordFailure`，事件名 `access.role.assign_failed`。`IdempotencyService.runAndComplete`（第 166–177 行）在没有外层事务时用 `TransactionTemplate` 新建事务；动作抛错后这个模板自己回滚，然后异常落到业务方法的 catch，此时线程上已经没有事务，失败插入提交。

有外层 `@Transactional` 时，插入加入该事务。业务方法再把异常抛出，Spring 回滚，失败行一起消失。这是 `assignUserPosition`：`AccessService.java` 第 577–578 行有 `@Transactional`，第 589 行 catch 里 `recordFailure`，事件名 `access.user_position.assign_failed`，随后 `throw exception`。`TransactionTemplate` 的默认传播是 `REQUIRED`（`IdempotencyService.java` 第 167–168 行写明：无外层则新建，有则并入）。并入之后，模板在异常上只把外层标成只回滚，不自己提交或回滚。catch 里的失败插入仍在这条只回滚的事务上。

`xm031b_probe.log`（2026-09-24，worktree `4a066def`，一次性空库，`MVN_EXIT=0`）第 5 行：`plain_failure_rows=1 transactional_failure_rows=0`。`m031_probe.log` 第 5 行是旧代码上的同一对数字。定岗这条探针断言抛出的是 `ApiException` 并且 Maven 退出码为 0，所以这条路径上 `recordFailure` 没有抛错：插入执行过，随后被回滚掉。不是「插入根本没被调用」。

第二截：PostgreSQL 在事务里某条语句失败后，同一条连接上的后续命令会被拒绝，直到回滚。`xm031b_probe.log` 第 4 行：`original=DataIntegrityViolationException recordFailure=threw:UncategorizedSQLException(current transaction is aborted) propagated=DataIntegrityViolationException failure_rows=0`。产生这一行的是 `XM031BAbortedTxProbeTests`：在 `TransactionTemplate` 里执行 `SELECT 1/0`，catch 里调 `recordFailure`，探针自己再 catch 插入异常并重新抛出原来的 `DataIntegrityViolationException`。所以日志里的 `propagated=DataIntegrityViolationException` 是探针的重新抛出，不是生产 catch 的形状。生产 catch（`assignUserPosition` 第 586 行起、`AuditedOperationExecutor.executeWrite` 第 143–153 行）是 `recordFailure(...); throw exception;`，中间没有内层 try。插入若抛 `UncategorizedSQLException`，原来的异常到不了 `throw exception`。事务已中止的正常触发：带事务的写方法 catch 到约束失败、唯一冲突这一类 `DataAccessException` 后再记失败。Java 里先抛出的 `ApiException`（缺用户、校验失败）不中止 PostgreSQL 事务，只发生第一截回滚，原异常仍抛出。

`IdempotencyService` 第 35–36 行和第 168 行的「失败留痕在外」指的是模板 catch 里的 `markRetryableFailure`（第 203–218 行），相对模板自己新建的那次事务。外层还有 `@Transactional` 时，这句话对审计插入不成立，对幂等失败标记也不成立：标记更新同样并入外层，业务回滚时标记一并消失。幂等占位若也在外层事务里，回滚后重试可以再占位，这与「失败不得留下半截业务写入」一致，不单列为本模块缺陷，也不把最小处置扩到 `IdempotencyService`。

影响面（`4a066def` 上重数，不是沿用登记的 47/27）：

- 生产树 `recordFailure(` 命中 47 个文件。其中 `AuditEventService.java` 是方法定义，点调用 0 次。调用方 46 个文件。
- 全文 `.recordFailure(` 155 处。
- 调用方文件里正文含 `@Transactional` 的有 25 个，不是登记写的 27。
- 方法签名上方直接有 `@Transactional`、且方法体内有点调用的：31 处、11 个文件。`AccessService` 9（含 `assignUserPosition` 第 589 行），`StaffLockerService` 2，`StaffRegistrationService` 1，`ProductionAuthService` 3，`CustomerService` 3，`LoyaltyConfigAdminService` 3，`TerminalCanaryService` 3，`TerminalDeviceService` 3，`TicketCredentialService` 2，`TicketTransferService` 1，`VoucherTransferService` 1。
- 另外 14 个文件含有 `@Transactional`，但扫描到的 `recordFailure` 不在那个注解的方法体内：`AmusementSafetyService`、`AmusementService`、`EvidenceEventService`、`FinanceService`、`IntegrationService`、`InventoryHighValueReviewService`、`InventoryService`、`PurchaseRequestService`、`SupplierQualificationService`、`SupplyContractService`、`WarehouseService`、`MerchantBillingService`、`MerchantServiceService`、`MdmService`。这些调用只有在更外层已经开了事务时才会并进去。
- `AuditedOperationExecutor` 自己没有 `@Transactional`。`executeWrite` 第 144–152 行的失败插入在幂等回调之外，仍在调用方的外层事务之内。`readPage` / `readPageResult` / `readSingle` 的失败插入同样如此。

登记的现象（带事务的失败事件随回滚丢失、不带事务的能留下、不应逐个摘掉调用方的 `@Transactional`、不应推翻 M006-G-07）与 `4a066def` 上的代码和探针一致。文件个数要改成上面的 46 个调用方、25 个文件含注解、31 处调用落在注解方法里。M006-G-07 在 `AccessJdbcIntegrationTests.java` 第 661–672 行：只断言 `assignUserPosition` 与 `assignStaffLevel` 两个方法的事务属性非空，不执行四步写入。摘掉这两个注解会让这条测试变红，也会拆开「旧派生同步失效」要的同一次事务。

要求失败也留下的 HTML 原句（类型 1）：

- 业务地图第 1583 行，BSM-094：「核销成功、失败、拒绝、撤销。」
- 业务地图第 1599 行，BSM-095：「核销、收款、退款、放行等敏感操作，操作设备定位落在该项目综合体授权范围之外时一律拒绝并留痕」。
- 业务地图第 1845 行，BSM-114：「Web 端同样适用：核销、收款、退款、放行等敏感操作在授权位置范围之外一律拒绝并留痕。」
- 治理地图第 620 行，PGM-037：「通过企业级手机统一管控手段阻止违规访问并留痕。」
- 治理地图第 893 行，PGM-061：「谁发起、谁销售、谁服务、谁第一个处理异常、为什么、谁审批、结果是什么，都要能查到。」
- 治理地图第 899 行，PGM-062：「权限变更、临时授权和敏感权限使用。」探针的定岗/赋角色就是权限变更的失败。
- 治理地图第 902 行，PGM-062：「终端开关启用、停用、灰度开放和被拒绝访问。」
- 治理地图第 960 行，PGM-066：「所有有过程的业务动作都必须留痕」。
- 业务地图第 1995 行，BSM-120，同一句「所有有过程的业务动作都必须留痕」。

BSM-093 标题是「重要动作留得住，出了问题追得回」（业务地图第 1574 行）。这些句子要求的是失败或拒绝之后还能查到记录。它们没有规定必须使用哪一种事务传播。传播级别是这些句子的下位实现。

## 二、单点处置与风险

缝放在 `recordFailure` 这一个公开方法上。生产调用都来自别的 bean，会走 Spring 代理。类内没有自调用 `recordFailure`。`record()` 是私有方法，在代理已经开好的事务里执行。`new AuditEventService(repository)` 不走代理，注解不生效，现有内存测试不用改构造器。

最小处置：只在 `AuditEventService.recordFailure` 上加 `@Transactional(propagation = Propagation.REQUIRES_NEW)`。不要加在 `record()`、`recordSuccess` 或 `recordDetail` 上。不要摘调用方的 `@Transactional`。不要改 `AccessJdbcIntegrationTests` 第 661 行那条 M006-G-07。不要改 `IdempotencyService`。

逐项风险：

外层事务挂起。会挂起。`spring-jdbc` 7.0.8 的 `DataSourceTransactionManager.doSuspend` 把连接持有者置空，并 `TransactionSynchronizationManager.unbindResource`。这是独立提交所要的，不是副作用缺陷。

第二个连接。会取。同一类的 `doBegin` 在没有已同步的连接持有者时调用 `DataSource.getConnection()`。外层那条连接在挂起期间仍被占用，新事务另取一条。窗口只有这一次 `INSERT`。依据是本席对已安装的 `spring-jdbc-7.0.8.jar` 做的 `javap -protected -c`。`spring-tx-7.0.8.jar` 的 `Propagation` 含有 `REQUIRES_NEW`。

连接池。`application.yml` 第 24 行 `maximum-pool-size` 默认 10（`COMPLEX_DATABASE_POOL_SIZE`）。一次失败占用 2 条连接，只在插入期间。要占满池，需要同时有大约池大小那么多个请求都停在 `recordFailure` 里。单次正常失败不会。不为此加第二数据源，也不改池大小。

审计表外键与自锁。不发生。`V002__p0_foundation_first_slice.sql` 的 `audit_events` 没有 `REFERENCES`。`V045` 只加可空的 `merchant_id VARCHAR(64)` 和索引。`V101` 只加可空的 `event_category`、`subject_type` 和索引。全部分支迁移里没有指向 `audit_events` 或从 `audit_events` 指出去的外键。新行的主键是随机 UUID，插入不等外层事务里的业务行锁。

成功事件是否仍随业务回滚。必须仍随业务回滚。`recordDetail` 第 157–159 行写明由调用方在业务事务内调用，与业务写入同事务原子落库。成功插入若改成独立提交，回滚掉的业务写会留下一条「已成功」记录，与 GEN-006「失败不得留下部分写入」相反，也和「留得住的是发生过的动作」相反。独立事务只包失败。

测试事务里的失败事件会不会留下来。会。`StaffJdbcIntegrationTests` 与 `ServiceOrderJdbcIntegrationTests` 是类级 `@Transactional`。今天失败插入加入测试事务，断言在回滚前执行，方法结束时行被回滚掉。改成 `REQUIRES_NEW` 之后，失败行在测试回滚后仍然提交。`V140__append_only_enforcement_and_runtime_role.sql` 把 `audit_events` 放在不可变表名单里，`BEFORE UPDATE OR DELETE` 触发器拒绝删除。回归不能靠 `DELETE` 清理。现有失败断言是 `isGreaterThanOrEqualTo(1)`（员工测试第 609、616、623 行；服务点单测试第 238、245、252 行），本席在测试树里没有找到对 `event_result='failure'` 的 `isZero`。多留下的失败行不会单凭这些断言把全量打红。成功计数按业务 id 精确等于 1，成功路径不改传播，仍随测试事务回滚。串扰的真实内容是：共享库上的失败行会永久留下；一次性空库用完即丢则没有后续库。B3 仍应在一次性空库上跑后端全量，确认没有别的精确计数被多出来的失败行碰到。

无事务管理器的内存构造。`AuditEventServiceTests` 用 `new AuditEventService(repository)`。没有代理就没有注解。两条测试继续只钉字段。不要为了单测去加 `TransactionTemplate` 构造参数。传播是否生效，用真库回归钉。

考虑过、不采用的做法：在 `afterCompletion` 里补插。代码更多，而且完成回调与连接是否已经回滚的次序容易插错；调用方若吞掉异常并提交，只在回滚时补写会丢掉今天能够提交的失败行。`REQUIRES_NEW` 更短，两条症状都盖住。

## 三、范围内测试与真库回归

范围内只有 `AuditEventServiceTests` 两例：成功字段、失败字段和 `error_code`。没有事务管理器，没有数据库。

本席实跑：`git worktree add --detach /home/james/scratch-xm031b-grok-b1 origin/main`。第一次用主工作区默认的 OpenJDK 21 JRE（没有 `javac`）编译，Maven 报 `release version 21 not supported`。改用 `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64` 后，在该 worktree 的 `backend` 执行 `mvn -o -Dtest=AuditEventServiceTests -Dsurefire.failIfNoSpecifiedTests=false test`。结果 `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。随后 `git clean -fd`，再 `git worktree remove`（没有 `--force`）。`git worktree list` 已无该路径。测试没有连接数据库。

绿，只说明字段拷贝与现在的实现一致。把 `recordFailure` 的插入放进会回滚的事务，这两条仍然绿。它们钉不住本缺陷。

范围外、但说明「现有真库断言也不钉提交后仍在」：上面两个类级 `@Transactional` 测试在测试事务结束前计数 `>= 1`。计数时行还看得见，回滚后行没了，测试仍然绿。这就是缺陷能留到全量 0 红的原因。

需要的真库回归（B3 再落，本阶段不写测试）：一次性空库，不要在测试方法上加 `@Transactional`。同一主体，`assignUserRole` 的 `access.role.assign_failed` 按本次幂等键计数为 1，`assignUserPosition` 的 `access.user_position.assign_failed` 按本次幂等键计数为 1。修复前第二条是 0，所以这条在修复前必须能失败。再加一段：事务内先执行会中止事务的 SQL，catch 里调 `recordFailure` 后把原异常重新抛出；断言插入不抛 `current transaction is aborted`，原异常种类不变，按幂等键计数为 1。断言用本次键相等，不用全表 `>= 1`。不要 `DELETE`。库用完丢掉。成功路径另断言一条会回滚的业务写不会留下 `event_result=success` 的行，防止独立事务被加宽到 `record()`。

## 四、46 项

判据全文在锁的定义区。下表只记本 6 个文件的适用面。锚点里列出的具体业务动作（核销实现、沽清、风险四级状态机等）不在这 6 个文件里，记无本面，不把「未实现该动作」升成缺陷。

| ID | 判定 |
|---|---|
| GEN-001 | 适用，通过。写入与查询有调用方；缺陷是留不住，不是多余抽象。 |
| GEN-002 | 适用，不通过。见 GR-01。失败路径与「记下一行失败」不一致。 |
| GEN-003 | 适用，不通过。见 GR-01。范围内测试不断言回滚后的行。 |
| GEN-004 | 适用，通过。查询值走命名参数。本面没有对外入口要做的类型校验。不为内部事件名再加一套长度闸。 |
| GEN-005 | 无本面。6 个文件没有 HTTP 入口。`searchOperationEvents` 的商户条件由调用方传入。 |
| GEN-006 | 适用，通过（业务写入）。定岗失败不留下业务行。成功留痕继续与业务同一事务。失败证据被一起回滚，不把本条理解成失败证据也必须消失。 |
| GEN-007 | 适用，通过（本面）。`executeWrite` 把幂等键交给范围外的 `IdempotencyService`。本面没有新的重复生效。 |
| GEN-008 | 适用，通过。INSERT 列与 V002、V045、V101 一致。`auth_context` 截断到 64，列是 `VARCHAR(64)`。类目与主体可空。 |
| GEN-009 | 适用，不通过。见 GR-01 的中止事务分支：插入抛错会盖住原来的数据异常。认证提取的空 catch（第 105–107 行）不替换业务异常，不单列。 |
| GEN-010 | 适用，通过。SQL 使用绑定参数。操作者写入留痕是 HTML 要求的内容。 |
| GEN-011 | 适用，不通过。见 GR-01。关键失败没有可再查的结构化行。 |
| GEN-012 | 适用，通过（现状）。查询 `limit` 封在 1 到 1000。池占用见处置风险，不是现状缺陷。 |
| GEN-013 | 无本面。 |
| GEN-014 | 无本面。迁移不在这 6 个文件。 |
| GEN-015 | 无本面。 |
| GEN-016 | 适用，通过。`recordDetail` 注释与成功路径相符，没有把失败留痕写成已经独立提交。 |
| GEN-017 | 适用。GR-01 按冲突、细化、技术性、添加的顺序归为细化。 |
| GEN-018 | 适用。本报告的行为结论落在 HTML 原文、探针输出和 `javap` 输出。 |
| BEQR-063 | 无本面。沽清、免单、存酒作废不在这 6 个文件。 |
| BEQR-138 | 无本面。风险四级不是 `AuditEvent` 的字段。 |
| BEQR-139 | 无本面。七个事件状态不是这里的 `success`/`failure`。 |
| BEQR-140 | 无本面。主管复核强制项不在这 6 个文件。 |
| BEQR-141 | 无本面。经营异常处置状态不在这 6 个文件。 |
| BEQR-142 | 无本面。异常单据的统一出口不在这 6 个文件。 |
| BSM-001 | 无本面。总述不规定这条事务边界。 |
| BSM-003 | 无本面。冻结原文是通用能力导语。 |
| BSM-022 | 无本面。入口卡片上的退款、免单等动作不在这 6 个文件。 |
| BSM-028 | 无本面。基础管理清单不在这 6 个文件。 |
| BSM-093 | 适用，不通过。见 GR-01。「留得住」「出了问题追得回」。 |
| BSM-094 | 适用面只有「核销成功、失败、拒绝、撤销」这一句，不通过，见 GR-01。同锚点其余动作无本面。 |
| BSM-095 | 适用面只有「一律拒绝并留痕」这一句，不通过，见 GR-01。同锚点其余句子无本面。 |
| BSM-096 | 无本面（动作清单与清理规则）。处置时引用 V140：失败行提交后不能 DELETE。不把永久保存实现缺口算进这 6 个文件。 |
| BSM-097 | 无本面。风险表不在这 6 个文件。 |
| BSM-114 | 适用面只有 Web 端「一律拒绝并留痕」这一句，不通过，见 GR-01。入口表其余行无本面。 |
| BSM-115 | 无本面。 |
| BSM-120 | 适用面是第 1995 行「所有有过程的业务动作都必须留痕」，不通过，见 GR-01。同锚点其他已拍板事项无本面。 |
| PGM-001 | 无本面。底座定位导语。 |
| PGM-014 | 无本面。入口卡片「谁发起、谁审批」不在这 6 个文件展开；失败留不住已由 PGM-061/062 记。 |
| PGM-023 | 无本面。空间点位功能不在这 6 个文件。 |
| PGM-026 | 无本面。商户主体功能不在这 6 个文件。 |
| PGM-037 | 适用面是第 620 行「阻止违规访问并留痕」，不通过，见 GR-01。设备台账其余句子无本面。 |
| PGM-061 | 适用，不通过。见 GR-01。「结果是什么，都要能查到。」 |
| PGM-062 | 适用面是第 899 行权限变更与第 902 行被拒绝访问，不通过，见 GR-01。同锚点其余动作无本面。 |
| PGM-063 | 无本面。复核种类不在这 6 个文件。 |
| PGM-064 | 无本面。挂起订单不在这 6 个文件。 |
| PGM-066 | 适用面是第 960 行「都必须留痕」，不通过，见 GR-01。同锚点其他已拍板事项无本面。 |

46 项都有判定。不通过的都指向 GR-01，没有第二项独立缺陷。

## 对抗

失败行是 0，会不会只是计数早于提交。不会。计数在业务方法已经抛出之后。不带事务的对照在同一次进程里留下 1 行。定岗路径抛出的是 `ApiException`，说明 `recordFailure` 已返回，行是后来被回滚的。

应不应该改成摘掉 `@Transactional`。不应该。那会拆开同一次业务写入，并让 M006-G-07 的注解断言失败。HTML 要的是失败记录还在，不是把业务事务拆开。

成功路径应不应该一并独立提交。不应该。回滚的业务写会留下成功记录。`recordDetail` 写明要同事务。

`propagated=DataIntegrityViolationException` 是不是说明生产不会被盖住。不是。那是探针 catch 住插入异常后重新抛出原异常。生产 catch 没有这层内层 try。

47 与 27 要不要照登。不要。`4a066def` 上是 46 个调用方文件、25 个文件含 `@Transactional`、31 处调用在注解方法体内。现象仍然成立。

`terminal` 恒为 `"web"`（`AuditEventService.java` 第 117 行）要不要立案。不立案。HTML 要求拒绝要留痕，没有要求这个列必须写成真实终端类型。不为此加终端识别。

`Map.of("error_code", errorCode)` 在 `errorCode` 为 null 时会抛空指针。`ApiException.code()` 可以是 null，但正常工厂都传入非空码。要同时碰上空码才发生，不立案。

读路径把坏 JSON 收成空 map（`JdbcAuditEventRepository.java` 第 102–109 行）。本服务写出的 JSON 可以读回。不是第一次正常写入会撞上的路径，不立案。

幂等失败标记在外层事务里也会回滚。相对业务回滚是一致的，重试可以再占位。不在这 6 个文件里，不入队，最小处置不改 `IdempotencyService`。

## 问题

### GR-01

文件与原句。`AuditEventService.java` 第 59–71 行 `recordFailure`，第 128 行 `repository.save`。方法上没有事务注解。`AuditedOperationExecutor.java` 第 144–152 行在 catch 里调用它，第 157–159 行写明成功明细与业务写入同事务。`AccessService.java` 第 577–578 行 `@Transactional`，第 589 行记录 `access.user_position.assign_failed`。同文件第 341 行 `assignUserRole` 没有该注解，记录 `access.role.assign_failed`。

正常触发。同一主体对缺失用户做赋角色与定岗：前者失败事件提交后仍为 1 行，后者为 0 行。另一条正常触发：事务内 SQL 已经失败并中止事务后，同一条连接上的 `recordFailure` 插入抛出 `UncategorizedSQLException`（`current transaction is aborted`），失败行仍为 0；生产 catch 不包内层 try 时，这个异常会代替原来的数据异常抛出。

四分类顺序。先看冲突：代码没有写一条与 HTML 相反的规则，catch 的目的就是记下失败。冲突的处置是删除，删掉 `recordFailure` 会更查不到失败。不归冲突。再看细化：上面列出的 HTML 句子已经要求失败、拒绝、权限变更的结果能查到；独立提交是这些句子的下位实现，当前实现没有把行留住。归细化。有工程缺陷，进入修复队列。不是技术性豁免，也不是添加：没有新的业务对象或新枚举。

已有判据。GEN-002、GEN-003、GEN-009、GEN-011；BSM-093、BSM-094、BSM-095、BSM-114、BSM-120；PGM-037、PGM-061、PGM-062、PGM-066。

证据类型。类型 1：上列 HTML 原句。类型 4：`/home/james/audit-tmp/xm031b_probe.log` 第 4–5 行与 `/home/james/audit-tmp/m031_probe.log` 第 5 行；本席对 `AuditEventServiceTests` 的 Maven 输出（2 测 0 失败）；`javap` 看到的 `doBegin` 取连接与 `doSuspend` 解绑。表结构来自 `origin/main` 的 V002、V045、V101、V140。

最小处置。只给 `recordFailure` 加 `REQUIRES_NEW`。成功路径维持加入调用方事务。不摘调用方注解，不改 M006-G-07。真库回归用一次性空库，按本次幂等键断言两条失败都为 1 行，并断言事务中止后插入不再抛出且原异常仍在；不要 `DELETE`。内存测试保持 `new` 构造。不新增数据源，不改连接池，不改 `IdempotencyService`。

## 结论

FAIL。

范围内收齐的队列问题只有 GR-01。46 项里与失败留痕直接相关的判据不通过，其余适用项通过或无本面。没有添加项，没有要问操作员的业务口径，没有 HTML 修改申请。

## XM-M031-B B2-r1 整批复核（Grok席乙，V0.02）

版本：V0.02。前文 B1 报告保持 V0.01，本段起把文件版本记为 V0.02。不改前文。本段不重算四值。

### 0. 输入、边界、技能

正本 `/home/james/complex-management`。代码以 `origin/main` `4a066def20758a4d8b06de619c3515cf526d253b` 为准。工作区 HEAD 仍是 `e456ba96c84185c8d39f18f548011daac48a6466`，相对 HEAD 有未提交改动。本段点名的生产与测试行都用 `git show origin/main:` 与 `git grep origin/main`。`.git/MERGE_HEAD` 不存在。

输入：本席 B1 `modules/XM-M031-B-b1-grok-raw.md` V0.01（28762 字节，SHA-256 `f53409a6998f6c408d37e846471d48dbdbdc6e172cfc419b8c995923e761852e`，FAIL，GR-01）；席甲 B1 `modules/XM-M031-B-b1-self-raw.md` 只取前 16987 字节（SHA-256 `a3e7fc91d8c7405766b5e05fc07a942669972c18a2001448f49ed0e9c1d186d1`，与候选所记一致，FAIL，CX-01、CX-02）。候选是 `findings.md` 顶部「当前 XM-M031-B B2-r1 整批候选」。本段只核这批的归并、分类、队列和最小修复边界，不重审模块，不重跑 46 项。

按字节截取席甲 raw 时，截断点之后的标题开头进入了预览（「XM-M031-B B2-r1 整批候选复核（席甲票，V0.02）」及时间戳前缀）。本席停止续读，下列结论不引用该段。为核对 M092、M095 是否仍为 PENDING，读了 ledger 派发段，见到席甲 B2 已记 PASS 的一句；不把该句当本席证据。未用 MCP、子代理、第三席。未对人发问。未改业务、HTML、锁、findings、ledger。未 fetch、commit、push、merge、deploy。未对数据库执行语句。计数时曾把调用方文件名写入 `/tmp/rf47.txt`，同一命令内删除，结束后该路径不存在。

| 技能 | 本轮 |
|---|---|
| using-superpowers | 已读开头。用户「只允许两席、不派子代理、不问人、不修」优先于派代理和问人。 |
| using-arc | 已读开头。不走 Arc `audit` 派席，不写 `docs/arc/audits/`。 |
| graft | 已读全文。未跑 graft 命令。它索引的是脏工作区，不是 `4a066def`。无 graft 节省数。 |
| verification-before-completion | 已读。本段不宣称缺陷已修好、回归已转绿。行号、计数、触发器、测试类结构、`spring-test` 7.0.8 监听器字节码都在本轮命令输出之后才写结论。落盘后核对前文 28762 字节哈希。 |
| receiving-code-review、grilling、codebase-design、ponytail、writing-for-agents、diagnosing-bugs、systematic-debugging | 本轮未重读这些文件。没有照候选文句同意：差集、AuthService 第 64 行、31 处里的私有方法、测试类 `TRUNCATE` 都按 `origin/main` 收窄。不新跑探针，沿用已有 `/home/james/audit-tmp/xm031b_probe.log` 与两份探针源。 |
| audit-context-building | 未调用。它要求子代理，与只允许两席冲突。 |
| batch-grill-me、ubiquitous-language | 不记成已调用。 |
| research | 按 V0.17 划掉，未用。 |

### 1. 差集

GR-01、CX-01、CX-02 都进 XM-M031-B-001。没有第二条队列项。两席的观察仍是观察。本席 B1 不立案的三项（终端字面量 `web`、`errorCode` 为空时 `Map.of` 抛空指针、读路径坏 JSON 收成空 map）候选写明席甲同意，仍不立案。

CX-01 是细化：带事务的失败事件随业务回滚消失。CX-02 是技术性：事务已被 SQL 错误中止后，`recordFailure` 抛出 `UncategorizedSQLException`，生产 catch 里的原异常到不了 `throw exception`。两条是同一个根：`recordFailure` 没有独立事务。锁内 GEN-017（`criteria-lock.md` 第 94 行）的顺序是冲突、细化、技术性、添加。合并后的一条取细化，技术性症状留在事实格、GEN-009 和第三条回归里。这是按顺序收成一条，没有改掉 CX-02 要保住的行为：插入不抛、失败行还在、原异常仍抛出。

处置从席甲 B1 的 `TransactionTemplate` 加可选 setter，收到本席的方法注解。候选前言写明了这个选择。结果相同：失败插入独立提交，成功路径仍加入调用方事务，内存里的 `new` 不改构造器。席甲 B1 第 5 节要在 `finally` 里按唯一键删掉自己提交的行；候选改成不 DELETE。这是按 V140 修正清理办法，缺陷本身没有换。

冻结判据格写了 GEN-002、GEN-003、GEN-009、GEN-011。分类格里的句子本轮在 `origin/main` 上对过行号：治理地图第 893、899、902、960 行，业务地图第 1583、1599、1845、1995 行，都是候选所引的那句。PGM-037 第 620 行和 BSM-093 的标题没有单列进分类格；同根的留痕句子已经在。不构成漏并。

漏并：无。没有一条 GR、CX 或只登记项没有去处。

### 2. 001 的注解与绕过代理

同意最小处置：只在 `AuditEventService.recordFailure` 上加 `@Transactional(propagation = Propagation.REQUIRES_NEW)`。`record()`、`recordSuccess` 和成功明细不加。不摘调用方的 `@Transactional`，不改 M006-G-07，不改 `IdempotencyService`，不改连接池。

`AuditEventService` 在 `origin/main` 上是 `@Service`，类不是 `final`，`recordFailure` 是 public。类内唯一的 `recordFailure` 就是这个定义。`recordSuccess` 和 `recordFailure` 都进入私有 `record()`。私有方法跑在代理已经开好的事务里，这不是绕过。成功路径不进入 `recordFailure`。没有 `this.recordFailure`，没有子类。

`src/main` 里 `new AuditEventService` 只有一处，就是候选说的 `AuthService.java` 第 64 行：两参构造 `this(seedUsername, seedPassword, new AuditEventService(event -> event))`。`AuditEventRepository` 的抽象方法只有 `save`，这个 lambda 把事件原样返回，不插入。该类第 24 行是 `@Profile({"local", "dev", "test"})`。Spring 用的是第 37–44 行的四参构造，把容器注入的 `AuditEventService` 赋给字段，不会走到第 64 行。走到第 64 行的是测试里的 `new AuthService("", "")`。注解在这个手工对象上不生效，这里本来也不落库。同意「不受影响」。

`src/main` 里没有 `@Configuration` 文件提到 `AuditEventService`，没有 `AopProxyUtils` / `Advised` 去拆代理。测试里有 `@Bean` 直接 `return new AuditEventService(...)`（`AuthControllerTests` 第 273 行、`AuthCookieCsrfTests` 第 197 行；`MemberWalletServiceTests` 第 31 行是空写 lambda）。那是测试配置，不在生产调用链，不为此扩大处置。`src/test` 里 `new AuditEventService` 本轮重数为 119，与候选一致。

因此：生产调用走别的 bean 上注入的代理；类内无自调用；没有第二处生产手工构造。001 的注解盖得住。

### 3. 写入集合

同意把 `EvidenceEventJdbcIntegrationTests` 纳入写入集合，生产只改 `AuditEventService`。`AuditEventServiceTests` 不改。

M095 的六个测试文件在 `plan/modules.md` 第 1579–1586 行。这个类在开工段六个文件之外，在 M095 之内，与 M092 的判据并集相同（锁第 6477、6480 行两行 ID 列表一致）。六个测试里，它是已经 `@SpringBootTest`、注入 `JdbcTemplate`、并且对 `audit_events` 做读写的那个（类第 16–31 行）。`TraceabilityJdbcIntegrationTests` 也有类级事务和 `JdbcTemplate`，不碰 `audit_events`，更不合适。新建测试文件会落到模块文件清单外面。所以这是模块边界内的最小位置。

定稿须写死的实施边界，不因此反对 001：

- 只加测试方法和它需要的注入：容器里的 `AuditEventService`、`AccessService`、`PlatformTransactionManager`。不改这个类已有的方法。
- 回归必须调用容器里的 `AuditEventService`。本类已有字段是 `JdbcAuditEventRepository`（第 31 行），直接 `save` 不经过服务，注解加了测试也不变。
- 赋角色、定岗走 `AccessService` 现成公开方法。失败事件上的键是请求的 `idempotencyKey()`（`AccessService` 赋角色 catch 约第 353–359 行，定岗 catch 的事件名在第 590 行），不是 `assignUserRole:` 那段范围字符串。探针已对缺失用户得到 `ApiException`，不必先插入用户。
- 不 DELETE。V140 把 `audit_events` 放进不可变表（第 32 行），`trg_append_only_ud` 是 `BEFORE UPDATE OR DELETE` 的行触发器，注释写明对 owner 也触发。这个新方法也不要 `TRUNCATE audit_events`。触发器不拦 `TRUNCATE`。类里第 40、63 行的 `TRUNCATE` 跑在类级事务里，结束会回滚；`NOT_SUPPORTED` 方法里同样一句会提交，把表清空。
- 类上 `@BeforeEach`（第 33–35 行）会 `TRUNCATE evidence_events`。`NOT_SUPPORTED` 不为测试方法开事务，这次截断会提交。`evidence_events` 同样在 V140 不可变名单里，行级 DELETE 触发器不拦 `TRUNCATE`。所以这条回归只在一次性空库上跑。该类 `@SpringBootTest` 把 URL 写死为 `complex_test`（第 17 行），B3 须把数据源指到一次性空库，与既有探针同一办法。

`NOT_SUPPORTED` 本身成立，而且必须有。本轮对已安装的 `spring-test-7.0.8.jar` 做了 `javap -c`：`TransactionalTestExecutionListener.beforeTestMethod` 在 `getPropagationBehavior()` 等于 4 或 5 时直接 `return`，不开测试事务。`spring-tx-7.0.8` 里 `PROPAGATION_NOT_SUPPORTED` 的常量值是 4，`NEVER` 是 5。方法注解盖过类级 `@Transactional`（第 21 行）。没有这层的话，断言发生在测试事务回滚之前，修复前定岗的失败行还看得见，`= 1` 不会先红。

ledger 第 4653、4656 行：M092、M095 都是 PENDING。失效范围不用把已 PASS 的模块改回。同意候选这句。

### 4. 回归三条

三条都保留。修复前会不会红，按已有探针和 `origin/main` 上的调用形状分开看。本轮没有重跑 Maven。

- 赋角色失败按本次幂等键 1 行：修复前已经是 1（`xm031b_probe.log` 第 5 行 `plain_failure_rows=1`）。单独这条在修复前是绿的，用来守住「无事务路径仍然留下 1 行」。
- 定岗失败按本次幂等键 1 行：修复前是 0（同一行 `transactional_failure_rows=0`）。写在 `NOT_SUPPORTED` 方法里、在业务方法抛出之后再数，这条修复前会失败。和赋角色合成一条测试时，整条也会因此先红。
- 事务中止后 `recordFailure` 不抛、原异常种类不变、失败事件 1 行：修复前会失败。探针第 4 行是 `recordFailure=threw:UncategorizedSQLException(current transaction is aborted)` 且 `failure_rows=0`。同一行的 `propagated=DataIntegrityViolationException` 是探针在内层 catch 住插入异常后再抛原异常，不是生产 catch 的形状。回归要让 `recordFailure` 的异常直接冒出来，再看调用方按 `recordFailure(...); throw exception;` 冒出的种类。只复制探针的内层 catch，则「原异常种类不变」在修复前也成立，不能单独当红测；「不抛」和「1 行」仍然会先红。
- 会回滚的事务里记下的成功事件 0 行：修复前已经是 0。成功插入今天就在调用方事务里。这条在修复前不会失败。它拦住把 `REQUIRES_NEW` 加到 `record()` 或 `recordSuccess` 上。

够用。两条症状各有一条修复前会红的断言，成功路径有一条加宽后会红的断言。不再加第四条，也不把幂等占位的回滚写进这批回归。

### 5. 计数与观察

接受计数更正。本轮在 `origin/main` 的 `backend/src/main/java` 上重数：

- 字面量 `.recordFailure` 的调用方文件 46 个。`AuditEventService.java` 是定义，点调用 0 次。`TerminalSwitchGuard.java` 只在注释里出现这个词，没有调用。
- 调用点 155 处。
- 这 46 个文件里正文含 `@Transactional` 的 25 个。候选要替换的登记数 47/27 在这棵树上不成立。
- 注解直接标在含调用的方法上的，本轮数到 30 处、11 个文件，文件名单与本席 B1 的 11 个相同，其中 `CustomerService` 是 2 处（第 230、474 行）。第 31 处是 `CustomerService.changeAccountStatus` 第 1173 行：私有方法自身没有 `@Transactional`，只被第 1121–1142 行四个带注解的公开方法调用（`suspendCustomer`、`blacklistCustomer`、`reinstateCustomer`、`deregisterAccount`）。运行时这次失败插入仍在那些事务里。接受候选的 31 处、11 个文件；队列不因 30 与 31 的这个差别改动。

观察四条都接受，维持不入队：

- `IdempotencyService` 第 36、168 行的「失败留痕在外」只相对它自己的模板成立。文件属于 M084（`plan/modules.md` 该段文件列表含这个类）。调用方带事务时占位和 `markRetryableFailure` 并入外层、随回滚消失，重试可以再占位，与失败不留半截业务写入一致。不改这个类。
- 连接池 `application.yml` 第 24 行默认 10。独立事务只在一次失败插入期间多占一条连接。不为此加数据源，不改池。
- 类级 `@Transactional` 的真库测试在改后会把失败行提交留下。`StaffJdbcIntegrationTests` 第 601 行和 `ServiceOrderJdbcIntegrationTests` 第 231 行的失败断言是按事件名 `COUNT(*) >= 1`（第 610、617、624 行和第 239、246、253 行），多留下的失败行不会单凭这些断言打红。一次性空库全量仍要确认没有别的精确计数被碰到。这是 B3 验证项，不入本批队列。
- 本席 B1 不立案的三项维持不立案。

### 6. 添加项与业务口径

没有添加项。没有要问操作员的业务口径。失败留痕的句子已经在分类格里；独立提交是这些句子的下位实现。成功事件仍随业务回滚，两席 B1 已经按同一方向排除，不再拿传播级别去问人。

### 7. 对候选 ID

同意 XM-M031-B-001。分类细化，工程缺陷，修复队列「是」。事实两截都在，最小处置是一处方法注解，写入集合是上面这一处生产文件加 `EvidenceEventJdbcIntegrationTests`，回归三条够用，计数与观察按第 5 节接受。第 3 节的实施边界写入定稿，不另开 ID，也不把 001 退回。

修复队列「是」：1（XM-M031-B-001）。修复队列「否」：0。

## 结论

PASS。

本席 B2-r1 票：PASS。同意候选 XM-M031-B-001 入修复队列。无添加项，无 questions。此票只覆盖本批归并、分类、队列和最小修复边界。B1 的 FAIL 仍在上文，不因本段 PASS 变成模块 PASS，也不代替 B3。须另有席甲本批 B2 PASS 才能定稿。

## XM-M031-B B2-r2 复核（Grok席乙，V0.03）

版本：V0.03。前文到 B2-r1 为止保持 V0.02，本段起把文件版本记为 V0.03。不改前文。本段只审 001 回归位置从 `EvidenceEventJdbcIntegrationTests` 改到 `TraceabilityJdbcIntegrationTests`。不重审模块，不重开 001 的注解、分类、队列和其余处置。

### 0. 输入、边界、技能

正本 `/home/james/complex-management`。代码以 `origin/main` `4a066def20758a4d8b06de619c3515cf526d253b` 为准。工作区 HEAD 仍是 `e456ba96c84185c8d39f18f548011daac48a6466`。本段点名的测试与生产行用 `git show origin/main:`、`git grep origin/main`，并在 `git diff --quiet origin/main` 为空之后读工作区同路径。两个测试文件与 `origin/main` 一致：`TraceabilityJdbcIntegrationTests.java` blob `aa0f0502c2fccc7043e89b03595ac84a7f3f3a32`，`EvidenceEventJdbcIntegrationTests.java` blob `7bcdc34a6018d8e40c1ccfefbab7ccc44649e50a`。`AccessService.java`、`IdempotencyService.java`、`AuditEventService.java`、`TestSecurityActors.java`、`application.yml`、`plan/modules.md`、`plan/criteria-lock.md` 与 `origin/main` 的差异为空。候选在工作区 `findings.md` 顶部「当前 XM-M031-B B2-r2 候选」（该文件相对 `origin/main` 有差异，候选不在 `4a066def` 上）。本席 V0.02 全文在追加前是 43314 字节，SHA-256 `dba6f5e9488fab8c39797f19872ceeec5731078ea32f6799b390399d068f6015`，与候选所记一致。

席甲 `modules/XM-M031-B-b1-self-raw.md` 开工时 19485 字节，长于 V0.02 的 18489 字节。检索扫到第 137 行、也就是 18489 字节之后的标题时间戳一句。本席停止续读，下面的结论不引用该段。未用 MCP、子代理、第三席。未对人发问。未改业务、HTML、锁、findings、ledger。未 fetch、commit、push、merge、deploy。未对数据库执行语句。未建脚本、日志或临时文件。本轮没有把三条回归写进测试类，也没有跑 Maven。

| 技能 | 本轮 |
|---|---|
| using-superpowers | 已读开头。用户「只允许两席、不派子代理、不问人、不修」优先于派代理和问人。 |
| using-arc | 已读开头。不走 Arc `audit` 派席，不写 `docs/arc/audits/`。 |
| graft | 已读全文。未跑 graft 命令。它索引的是脏工作区（HEAD 不是 `4a066def`），不能当本段证据。无 graft 节省数。 |
| verification-before-completion | 已读。行号、监听器字节码、Hikari 默认自动提交、JUnit 回调顺序都在本轮命令输出之后才写。没有宣称三条回归已经转红或转绿。 |
| systematic-debugging、receiving-code-review、grilling、ponytail、codebase-design、diagnosing-bugs、writing-for-agents | 本轮未重读这些文件。位置判断按 `origin/main` 的类正文和字节码，不沿用候选原句。 |
| audit-context-building | 未调用。它要求子代理，与只允许两席冲突。 |
| research | 按 V0.17 划掉，未用。 |

修复前的行数和异常种类沿用本轮重读的 `/home/james/audit-tmp/xm031b_probe.log`：`START` 行写明 scratch 在 `4a066def`、`PATCH=none`、库名 `xm031bp_test`；第 4 行 `recordFailure=threw:UncategorizedSQLException(current transaction is aborted)` 且 `failure_rows=0`，同一行的 `propagated=DataIntegrityViolationException` 是探针自己再抛的；第 5 行 `plain_failure_rows=1 transactional_failure_rows=0`。换测试类不改变这条生产控制流。

### 1. 新位置的夹具与持久副作用

结论：`TraceabilityJdbcIntegrationTests` 自身没有会在方法级 `NOT_SUPPORTED` 下变成真提交的夹具写。类级注解和数据源与证据类同一写法。放进三条回归之后，持久副作用比候选写的「只剩本次唯一键的 `audit_events` 行」多一行幂等占位，而且只来自赋角色这条。

类全文 133 行，本轮读完。类上只有两处注解：`@SpringBootTest`（第 14–18 行）和类级 `@Transactional`（第 19 行，默认 `REQUIRED`）。数据源三行写死为 `jdbc:postgresql://127.0.0.1:5432/complex_test`、用户 `complex`、口令 `${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}`。字段是 `JdbcTraceabilityRepository`、`TraceabilityService`、`JdbcTemplate`（第 22–29 行）。生命周期方法只有 `@AfterEach clearActor`（第 31–34 行），调用 `TestSecurityActors.clear()`，也就是 `SecurityContextHolder.clearContext()`（`TestSecurityActors.java` 第 64–66 行），不写库。没有父类，没有 `@Nested`。`git grep origin/main` 在这个文件上对 `@Before`、`@Sql`、`@Commit`、`@Rollback`、`TRUNCATE`、`DELETE`、`INSERT`、`update(`、`execute(` 零命中。类里唯一的 JDBC 是 `regclass` 的 `SELECT to_regclass`（第 130–132 行）。已有四条测试的写入走服务，包在类级事务里，方法结束由监听器回滚；那是这个类今天已有的行为，不是这三条新方法加上去的提交。

方法级 `@Transactional(propagation = NOT_SUPPORTED)` 盖过类级注解，而且监听器不会再开测试事务。本轮对已安装的 `spring-test-7.0.8.jar`（Spring Boot 4.1.0 BOM 的 `spring-framework.version` 就是 7.0.8）做了 `javap -c`：`TransactionalTestExecutionListener.beforeTestMethod` 在 `getPropagationBehavior()` 等于 4 或 5 时，字节码 178 直接 `return`，后面的 `startTransaction`（字节码 243）走不到。`spring-tx-7.0.8` 的常量池里 `PROPAGATION_NOT_SUPPORTED` 是 4，`NEVER` 是 5。JUnit Jupiter 引擎 6.0.3 的 `TestMethodTestDescriptor.execute` 顺序是 `invokeBeforeEachCallbacks`、`invokeBeforeEachMethods`、测试方法、`invokeAfterEachMethods`、`invokeAfterEachCallbacks`。`SpringExtension.beforeEach` 调的是 `TestContextManager.beforeTestMethod`。所以 `@BeforeEach` 发生在监听器决定开不开事务之后。这个类没有 `@BeforeEach`，这条顺序在这里没有夹具可提交。`afterTestMethod` 只在已经放进 `TransactionContext` 时才 `endTransaction`；`NOT_SUPPORTED` 没有放入，方法里已经提交的行不会被测试监听器滚掉。

三条按候选的调用形状放进来之后，夹具仍然零提交。会留下的是测试动作自己的行：

- 赋角色 `assignUserRole`（`AccessService.java` 第 341–362 行）方法上没有 `@Transactional`。类第 47 行也没有类级 `@Transactional`，全文第一处方法注解在第 428 行。`IdempotencyService.execute` 第 99 行 `tryReserve` 在事务模板之外。`JdbcIdempotencyRepository` 第 47–55 行是 `INSERT INTO idempotency_records`。没有外层事务、Hikari 自动提交为真时，这一行提交。动作抛错后，模板回滚事务内的写入；第 175 行 catch 调用第 203–214 行 `markRetryableFailure`，再在模板外把同一行更新为 `FAILED_RETRYABLE` 并提交。然后 `assignUserRole` 的 catch 第 352–360 行 `recordFailure`，失败审计行也提交。`idempotency_records` 不在 V140 的不可变表名单里（`V140__append_only_enforcement_and_runtime_role.sql` 的 `immutable_tables` 含 `audit_events`、`evidence_events`，`git grep` 对该文件搜 `idempotency_records` 零命中）。所以赋角色这条留下两行：本次唯一键的失败审计，加上同一键的幂等占位。缺失用户在 `resolveRoleGrant` 第 1827 行 `requireTargetUser` 就抛 `ApiException`，动作内部没有业务插入。
- 定岗 `assignUserPosition`（第 577–599 行）有方法级 `@Transactional`。从 `NOT_SUPPORTED` 的测试方法看，这是一条新事务。占位、动作、失败标记都在这条事务里。缺失用户在 `resolvePositionGrant` 第 1845 行抛 `ApiException`，不中止 PostgreSQL 事务。修复前 `recordFailure` 也在这条事务里，方法抛错后整笔回滚，审计 0 行，幂等行也不留。修复后只有 `REQUIRES_NEW` 的失败审计行提交。
- 事务中止那条若按「测试自己用 `TransactionTemplate` 执行 SQL，catch 里 `recordFailure(...); throw 原异常;`」来写，不进入 `IdempotencyService`，不留幂等行。`SELECT 1/0` 不写表。修复前插入抛错、事务回滚，审计 0 行。修复后留下本次失败审计一行。
- 会回滚事务里的成功事件，计数发生在回滚之后时，提交行数是 0。

`@AfterEach` 清的是线程上的认证，不是表。这三条不调用 `TraceabilityService`，不写三张追溯表。

候选第 6 行「唯一留下的是按本次唯一键写入的 `audit_events` 行」过宽。定稿要把赋角色那条幂等占位写上。断言仍按审计表的本次唯一键，不为了清幂等行去 `DELETE` 或 `TRUNCATE` `audit_events`。这一行用本次新键，不撞别的键。它落在 B2-r1 已经留下的 B3 项里：一次性空库全量确认没有精确的全表计数被碰到。不因此新开 ID，也不因此把回归留在证据类。

### 2. 空库约束留在证据类，以及改位置是否必要、最小

结论：「只在一次性空库上跑」留在 `EvidenceEventJdbcIntegrationTests` 上，约束不住这次回归会提交的 `TRUNCATE`。改到 `TraceabilityJdbcIntegrationTests` 必要，而且在 M095 文件清单里是最小的一处。

证据类第 16–20 行的 URL 与追溯类第 14–18 行相同，都是 `127.0.0.1:5432/complex_test`。第 33–36 行 `@BeforeEach` 执行 `TRUNCATE evidence_events`。类级 `@Transactional` 的现有方法里，这次截断包在测试事务中，结束回滚。新方法若标 `NOT_SUPPORTED`，第 1 节的监听器路径不开测试事务，`@BeforeEach` 仍会跑。`application.yml` 第 19–25 行只设了池大小，没有改自动提交。HikariCP 7.0.2 的 `HikariConfig()` 字节码 87–88 行把 `isAutoCommit` 置为 `iconst_1`。自动提交打开时，这条 `TRUNCATE` 在语句结束时提交。V140 第 72 行的触发器是 `BEFORE UPDATE OR DELETE`，不包含 `TRUNCATE`，拦不住清空；`evidence_events` 在第 33 行的不可变名单里，行级 `DELETE` 同样被拒，清空之后也不能靠删行补回来。

候选写进 findings 的那句「只在一次性空库上跑」不会被测试执行。注解里的 URL 指向套件已经在用的 `complex_test`。开发者跑这个类或全量，就会在这个库上提交清空。要把库真的换成一次性空库，得改这行 URL，或者把 `127.0.0.1:5432` 上的 `complex_test` 换成另一台服务器。那比挪三条例回归更大，而且仍然靠运行方式记住，不靠代码。所以这条约束不能留在证据类上当这次改写的控制。

改位置必要，就为了去掉这个 `@BeforeEach`。最小性：M095 的六个文件在 `plan/modules.md` 第 1579–1586 行，追溯类在第 1585 行，证据类在第 1583 行。锁第 6477 行与第 6480 行的判据 ID 列表相同。其余四个测试接不住这三条：`AuditEventServiceTests` 与 `AuditControllerTests` 是内存或 mock，没有数据源；`EvidenceEventControllerTests` 是 `@WebMvcTest`，自带 `@BeforeEach`；`EvidenceEventServiceTests` 是内存仓库，也有 `@BeforeEach`。新建测试文件会落到这六个路径外面。改证据类的 `@BeforeEach` 会动已有方法的夹具。追溯类已经有 `@SpringBootTest`、类级事务和 `JdbcTemplate`，夹具不写库。两条类打到同一个库，改位置不更换数据库，只去掉会提交的截断。

### 3. 三条回归在新位置、修复前会不会失败，要另注入什么

结论：定岗那条和事务中止那条，按下面的形状放进这个类，修复前会失败。赋角色「1 行」和成功事件「0 行」修复前已经成立，用来守边界。这个类现在的三个字段不够，必须另注入容器里的 `AuditEventService`、`AccessService`，以及 `PlatformTransactionManager`。本轮没有新跑 Maven。

- 赋角色失败按本次幂等键 1 行：修复前探针已经是 1。单独这条在修复前不失败。主体用本类已有的 `TestSecurityActors.asPlatformAdmin()`（内存里的 `AccessActor`，`currentActor` 第 209–216 行只读 `SecurityContextHolder`，不插入用户）。目标用户缺失时第 1827 行抛 `USER_NOT_FOUND`，catch 仍会 `recordFailure`。
- 定岗失败按本次幂等键 1 行：修复前探针是 0。写在 `NOT_SUPPORTED` 方法里，业务方法抛出、事务结束之后再数，修复前失败。和赋角色写在同一个方法里时，整个方法因此先红。缺失用户是 `ApiException`，不中止 PostgreSQL 事务，所以这条红的是行数，不是异常种类被顶掉。
- 事务中止后按 `recordFailure(...); throw 原异常;`，不包内层 try：修复前会失败，但只有 `recordFailure` 走在同一条已经被 SQL 错误中止的事务上才成立。探针第 4 行是插入抛 `UncategorizedSQLException`、失败行 0；`propagated=DataIntegrityViolationException` 是探针接住插入异常后再抛原异常。回归要让插入异常直接冒出来，断言原异常种类才会在修复前失败。只再调一次缺失用户的定岗，异常种类在修复前也是原来的 `ApiException`，钉不住这一截。形状是测试里打开事务、执行会失败的 SQL、catch 里直接 `recordFailure` 再 `throw` 原异常。`JdbcTemplate` 这个类已经有，用来执行那条 SQL 和按唯一键计数。
- 会回滚事务里的成功事件 0 行：修复前已经是 0。`record` / `recordSuccess` 今天就加入调用方事务（`AuditEventService.java` 第 27–38、73–128 行，`record` 没有事务注解）。这条在修复前不失败。它拦住把 `REQUIRES_NEW` 加到 `record()` 或 `recordSuccess` 上。计数必须在回滚之后、按本次唯一键。成功的 `assignUserPosition` 会提交，不能拿来当这条。

要另注入的：

- `AuditEventService`：必须是容器里的代理。这个类现在没有这个字段。直接拿 `JdbcAuditEventRepository.save` 不经过 `recordFailure`，注解加上也不变。证据类第 31 行那个仓储字段这里没有，也不要添来绕过服务。
- `AccessService`：赋角色和定岗用它的公开方法。这个类现在没有。
- `PlatformTransactionManager`：②和③用来 `new TransactionTemplate(manager)`。`IdempotencyService` 第 167–168 行写明这个一参构造的默认传播是 `REQUIRED`（无外层则新建，有则并入）。`NOT_SUPPORTED` 的测试方法上没有测试事务，模板会新建一条。不注入它的话，②在另一条连接上调用 `recordFailure`，修复前插入会自动提交，原异常种类也对，这条在修复前是绿的，钉不住事务中止。

不需要为这三条再插用户、再造角色，也不需要改这个类已有的四个测试和 `@AfterEach`。

### 4. 添加项与业务口径

没有添加项。没有要问操作员的口径。失败留痕和成功事件仍随业务回滚，都是 B2-r1 已经定稿的处置，这次只改回归放在哪个类。幂等占位在模板外提交是 `IdempotencyService` 第 35–36 行已经写明的行为，B2-r1 把它留在观察、指向 M084，本段不改那个类，也不把它升成新的队列项。

### 5. 对这次改写

同意把 001 的回归改放 `TraceabilityJdbcIntegrationTests`，方法级 `NOT_SUPPORTED`，不 `DELETE`、不 `TRUNCATE`，按本次唯一键断言。生产仍只改 `AuditEventService.recordFailure`。`EvidenceEventJdbcIntegrationTests` 与 `AuditEventServiceTests` 不改。

定稿要改候选里的两句，不因此反对改位置，也不把 001 退回：

- 「唯一留下的是 `audit_events` 行」改成：夹具没有提交写；赋角色失败另留 `idempotency_records` 一行（本次唯一键，模板外占位再标成可重试失败）；定岗那条、事务中止那条、成功回滚那条不留幂等行。
- 注入清单写上容器里的 `AuditEventService`、`AccessService`、`PlatformTransactionManager`。事务中止那条用「打开的事务里 SQL 失败，接着 `recordFailure(...); throw 原异常;`」，不包内层 try，不用缺失用户的定岗代替。

修复队列「是」仍是 1（XM-M031-B-001）。修复队列「否」：0。

## 结论

PASS。

本席 B2-r2 票：PASS。同意把 001 的回归位置改到 `TraceabilityJdbcIntegrationTests`。无添加项，无 questions。定稿按第 5 节改掉「唯一留下」和注入清单两句。此票只覆盖这次位置改写。B1 的 FAIL 与 B2-r1 的 PASS 都仍在上文。上文 B2-r1 写的写入集合仍是证据类，以本段改写为准。不代替 B3。
