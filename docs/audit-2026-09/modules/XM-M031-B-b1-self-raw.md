CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M031-B B1 修复前全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Opus 5.5（作者自审票）。2026-09-24T04:52:25Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（V0.14；实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。基线 origin/main 4a066def。
- 范围：开工段 6 个文件（M092 审计写入链 AuditEventService、AuditEventRepository、JdbcAuditEventRepository、AuditedOperationExecutor、AuditEvent；M095 AuditEventServiceTests）全读；为判断另读 IdempotencyService（M084）事务模板写法、AccessService 带事务写方法与 catch 结构、audit_events 建表（V002）、application.yml 连接池、EvidenceEventJdbcIntegrationTests 结构。判据 M092、M095 映射并集 46 项。
- 顺序：本席先取证、落本报告；落盘前未读 Grok 本次正文。未改任何已跟踪文件；探针在一次性 worktree 与一次性空库内做，已删。

## 1. 文件覆盖

| 文件 | 所属模块 | 行数 | blob |
|---|---|---:|---|
| backend/src/main/java/com/complex/platform/evidence/AuditEventService.java | M092 | 130 | 12a6a8300cfd |
| backend/src/main/java/com/complex/platform/evidence/AuditEventRepository.java | M092 | 18 | 07eaa506678b |
| backend/src/main/java/com/complex/platform/evidence/JdbcAuditEventRepository.java | M092 | 111 | b2adff87b9db |
| backend/src/main/java/com/complex/platform/evidence/AuditedOperationExecutor.java | M092 | 188 | 64bc6e8ce174 |
| backend/src/main/java/com/complex/platform/evidence/AuditEvent.java | M092 | 53 | 230ba0728e39 |
| backend/src/test/java/com/complex/platform/evidence/AuditEventServiceTests.java | M095 | 58 | c766ea68cc6f |

## 2. 证据（/home/james/audit-tmp/xm031b_probe.log）

```
START=2026-09-24T04:49:57Z scratch at 4a066def DB=xm031bp_test PATCH=none
scratch db xm031bp_test created
MVN_EXIT=0
XM031BPROBE aborted_tx original=DataIntegrityViolationException recordFailure=threw:UncategorizedSQLException(current transaction is aborted) propagated=DataIntegrityViolationException failure_rows=0
XM031BPROBE plain_failure_rows=1 transactional_failure_rows=0
scratch db dropped
scratch dirty after cleanup: 0
scratch removed
END=2026-09-24T04:50:15Z
```

- 探针一（M031 原探针，/home/james/audit-tmp/M031FailureAuditProbeTests.java）：同一主体同类失败，不带事务的 AccessService.assignUserRole 失败事件 1 行，带 `@Transactional` 的 assignUserPosition（AccessService:577-578）失败事件 0 行。
- 探针二（/home/james/audit-tmp/XM031BAbortedTxProbeTests.java）：TransactionTemplate 内执行 `SELECT 1/0` 使 Postgres 事务中止，随后在同一事务内 recordFailure → 抛 UncategorizedSQLException「current transaction is aborted」，失败事件 0 行。真实服务的 catch 结构是「recordFailure(...); throw exception;」（如 AccessService:336-338），recordFailure 一抛，原异常即被顶掉。

## 3. 三线审计

- 第一性原理：失败留痕要做到「谁在什么时候对什么做了什么、被拒绝或失败的原因」事后查得到（治理地图第 892 行起、BSM 第 1574、1578 行起）。实际：AuditEventService.record（:73-128）经 JdbcAuditEventRepository.save（:22-54）用同一 NamedParameterJdbcTemplate 写入，参与调用方事务；调用方带事务时失败事件随回滚消失。生产代码调 recordFailure 的 47 个文件中 27 个带 `@Transactional`。未验证前提：27 个文件里每一处 catch 是否都在事务内（多数是方法级注解，结论不变）。
- 顺从式：46 项逐项见 §6。
- 对抗式（grilling）：
  - 问：能不能在各调用方把 recordFailure 挪到事务外？答：要改 27 个文件、上百处，且 M031 两席已定单点处置、不推翻 M006-G-07 的事务注解；单点在 AuditEventService 更小。
  - 问：成功事件要不要也独立？答：不要。业务回滚时成功事件必须一起回滚，否则留下假的成功痕迹。只动 recordFailure。
  - 问：独立事务会不会自锁？答：audit_events 建表（V002）无外键，插入不等外层未提交行；主键随机 UUID，不与外层冲突。
  - 问：内存单测怎么办？答：测试里 119 处 `new AuditEventService(repository)`；仿 IdempotencyService.setTransactionManager（`@Autowired(required = false)` 可选注入），不注入时维持直写，签名不变。
  - 问：测试类级 `@Transactional` 的真库测试会不会受影响？答：失败事件改为独立提交，会留在库里不随测试回滚。全量须在一次性空库上实跑确认没有测试按不唯一的键数失败事件；这是 B3 的验证项。
  - 问：连接池？答：application.yml 默认 10。每次失败写多借一个连接片刻；要有 10 个以上带事务的请求同时失败并同时等第二个连接才会卡到超时，属极端并发，只登记。
- 本范围外同根：IdempotencyService 注释「reserve 仍在外先提交……失败留痕（markRetryableFailure）在外照常落」在调用方带 `@Transactional` 时不成立（占位与失败标记同样随回滚消失），只登记给 M084。

## 4. 问题（临时编号）

- **CX-01（细化；治理地图第 892 行起「平台治理层要保证每个重要动作能追溯……结果是什么，都要能查到」及「必须留痕的动作」列「权限变更、临时授权和敏感权限使用」「终端开关……被拒绝访问」；BSM 第 1578 行起「核销成功、失败、拒绝、撤销」；枚举速查第 236 行「留痕是特殊写操作」）** AuditEventService.recordFailure（:59-71）经 record（:73-128）在调用方事务内写 audit_events，带 `@Transactional` 的写方法失败时失败事件随业务回滚丢失。正常触发：带事务写方法校验失败或权限拒绝（如 assignUserPosition 目标用户不存在）。证据：探针一。最小处置：recordFailure 的写入走独立事务（TransactionTemplate，传播 REQUIRES_NEW），事务管理器按 IdempotencyService 同款可选 setter 注入，未注入（内存单测）时照旧直写；recordSuccess 与 record 的成功路径不动；补真库回归：带事务的写方法失败后失败事件仍在库，且同一事务里的成功事件随回滚消失。
- **CX-02（技术性，错误处理）** 调用方事务已因 SQL 错误中止时，recordFailure 在同一事务内写入即抛 UncategorizedSQLException（current transaction is aborted），调用方「recordFailure(...); throw exception;」里的原异常被顶掉，接口按顶替后的异常返回，失败事件也没有。证据：探针二。最小处置：同 CX-01（独立事务写失败事件，与外层中止无关）；补真库回归：事务中止后 recordFailure 不抛、失败事件在库、原异常照常抛出。

## 5. 只登记

- 观察（→M084）：IdempotencyService 在调用方带 `@Transactional` 时，占位（reserve）与 markRetryableFailure 同随外层回滚，与其注释「reserve 仍在外先提交……失败留痕在外照常落」不符；影响为失败后重试即新占位，未见数据错，只登记。
- 观察：独立事务写失败事件每次多借一个连接片刻，连接池默认 10，极端并发下可能等连接超时；不为此加需求。
- 回归位置建议：M095 的 EvidenceEventJdbcIntegrationTests 已注入 JdbcAuditEventRepository 与 JdbcTemplate，类级 `@Transactional`；回归方法用 `@Transactional(propagation = NOT_SUPPORTED)` 并在 finally 里按唯一键清掉自己提交的行（同仓 PlatformJdbcIntegrationTests 并发用例范式）。该文件不在开工段 6 个文件内，拟在 B2 候选写明是否纳入写入集合。

## 6. 46 判据逐项

| 判据 | 出处 | 适用｜证据｜结论 |
|---|---|---|
| BEQR-063 高危动作（需审批/双证据） | business-enum-quick-reference-v0.1.html:111 | 适用｜高危动作需审批/双证据：失败尝试留痕是证据链一环｜通过（问题并入 CX-01） |
| BEQR-138 风险四级 | business-enum-quick-reference-v0.1.html:217 | 无本面｜风险分级未触及｜通过 |
| BEQR-139 事件状态 | business-enum-quick-reference-v0.1.html:218 | 无本面｜事件状态字典未触及｜通过 |
| BEQR-140 主管复核强制项 | business-enum-quick-reference-v0.1.html:219 | 无本面｜本合成模块范围（6 个文件）不触及「主管复核强制项」所述对象｜通过 |
| BEQR-141 经营异常处置状态 | business-enum-quick-reference-v0.1.html:220 | 无本面｜本合成模块范围（6 个文件）不触及「经营异常处置状态」所述对象｜通过 |
| BEQR-142 异常状态的统一出口 | business-enum-quick-reference-v0.1.html:221 | 无本面｜本合成模块范围（6 个文件）不触及「异常状态的统一出口」所述对象｜通过 |
| BSM-001 整体说明：这套系统到底管什么 | business-system-map-for-operations-v0.1.html:505 | 无本面｜本合成模块范围（6 个文件）不触及「整体说明：这套系统到底管什么」所述对象｜通过 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | business-system-map-for-operations-v0.1.html:563 | 无本面｜数据可改可删与引导未触及｜通过 |
| BSM-022 留痕与异常 | business-system-map-for-operations-v0.1.html:666 | 适用｜留痕与异常：失败动作留痕在带事务路径丢失｜通过（问题并入 CX-01） |
| BSM-028 主要功能 | business-system-map-for-operations-v0.1.html:698 | 无本面｜本合成模块范围（6 个文件）不触及「主要功能」所述对象｜通过 |
| BSM-093 留痕与异常：重要动作留得住，出了问题追得回 | business-system-map-for-operations-v0.1.html:1574 | 适用｜第 1574 行「只要一个动作有过程，就要尽量在业务发生时自动留下记录」｜不通过（随 CX-01） |
| BSM-094 必须关注的动作 | business-system-map-for-operations-v0.1.html:1578 | 适用｜第 1578 行起「核销成功、失败、拒绝、撤销」须关注：带事务的核销/拒绝路径失败留痕丢失｜不通过（随 CX-01） |
| BSM-095 怎样减少人工填写 | business-system-map-for-operations-v0.1.html:1596 | 无本面｜本合成模块范围（6 个文件）不触及「怎样减少人工填写」所述对象｜通过 |
| BSM-096 处理结果 | business-system-map-for-operations-v0.1.html:1607 | 无本面｜本合成模块范围（6 个文件）不触及「处理结果」所述对象｜通过 |
| BSM-097 风险等级与风控规则 | business-system-map-for-operations-v0.1.html:1619 | 无本面｜本合成模块范围（6 个文件）不触及「风险等级与风控规则」所述对象｜通过 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | business-system-map-for-operations-v0.1.html:1827 | 无本面｜入口可见范围未触及｜通过 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | business-system-map-for-operations-v0.1.html:1897 | 无本面｜本合成模块范围（6 个文件）不触及「相互关系：为什么这些子系统必须连在一起」所述对象｜通过 |
| BSM-120 已确认规则和实施前输入项 | business-system-map-for-operations-v0.1.html:1957 | 适用｜已确认规则中的留痕要求见 PGM-062｜通过（问题并入 CX-01） |
| GEN-001 必要性与最小性 | 通用 | 适用｜单点在 AuditEventService.recordFailure 处置即覆盖 27 个带事务调用文件；不逐个摘调用方注解（M031 两席定稿、M006-G-07）｜通过（本项不单独致 FAIL） |
| GEN-002 可执行正确性 | 通用 | 适用｜真库探针：带事务写方法失败后失败事件 0 行（不带事务 1 行）；事务被 SQL 错误中止后 recordFailure 自身抛 UncategorizedSQLException（current transaction is aborted）（xm031b_probe.log）｜不通过（见 CX-01、CX-02） |
| GEN-003 测试有效性 | 通用 | 适用｜AuditEventServiceTests 两条只用内存仓储，钉不住事务语义；无真库回归覆盖失败事件存续｜不通过（随 CX-01/02） |
| GEN-004 信任边界校验 | 通用 | 无本面｜不涉信任边界输入｜通过 |
| GEN-005 授权与最小权限 | 通用 | 无本面｜不改授权判定｜通过 |
| GEN-006 数据完整性 | 通用 | 适用｜成功事件须随业务回滚（否则留下假成功痕迹）；失败事件须独立存续｜不通过（随 CX-01） |
| GEN-007 并发与幂等 | 通用 | 适用｜独立事务写失败事件借第二个连接；audit_events 无外键，不会被外层未提交行锁住（V002 建表无 REFERENCES）；连接池默认 10，极端并发下的池耗尽只登记｜通过（观察另列） |
| GEN-008 接口与模式一致性 | 通用 | 适用｜AuditEventService 构造签名须保持（测试 119 处 new AuditEventService(repository)）；可选注入仿 IdempotencyService.setTransactionManager 同款｜通过 |
| GEN-009 错误处理与恢复 | 通用 | 适用｜SQL 错误中止的事务里 recordFailure 抛错，调用方 catch 里的 throw exception 到不了，原异常被 UncategorizedSQLException 顶掉，接口返回错误码失真｜不通过（见 CX-02） |
| GEN-010 安全与敏感信息 | 通用 | 无本面｜不涉敏感信息｜通过 |
| GEN-011 可观测与审计证据 | 通用 | 适用｜失败与拒绝事件是审计证据的一半，带事务路径全部丢失｜不通过（见 CX-01） |
| GEN-012 资源与性能边界 | 通用 | 适用｜独立事务每次失败多占一个连接片刻｜通过（观察另列） |
| GEN-013 构建配置与依赖 | 通用 | 无本面｜无新依赖（spring-tx 既有）｜通过 |
| GEN-014 数据库迁移正确性 | 通用 | 无本面｜无迁移｜通过 |
| GEN-015 界面可用性与可访问性 | 通用 | 无本面｜不涉界面｜通过 |
| GEN-016 文档可验证性 | 通用 | 适用｜IdempotencyService 注释称「失败留痕在外照常落」，在调用方带 @Transactional 时不成立（观察另列，M084）｜通过（观察另列） |
| GEN-017 业务来源分类 | 通用 | 适用｜CX-01 细化（留痕句子的下位实现缺口）、CX-02 技术性（错误处理）｜通过 |
| GEN-018 第一性原理证据 | 通用 | 适用｜结论落到 HTML 原句、源码行与真库探针输出｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | platform-governance-map-for-operations-v0.1.html:256 | 适用｜治理底座：见 PGM-061/062｜通过（问题并入 CX-01） |
| PGM-014 留痕复核 | platform-governance-map-for-operations-v0.1.html:393 | 适用｜「重要动作可追溯：谁发起、谁审批、谁复核、谁放行」：带事务写动作的失败与拒绝不可追溯｜不通过（随 CX-01） |
| PGM-023 主要功能 | platform-governance-map-for-operations-v0.1.html:467 | 无本面｜本合成模块范围（6 个文件）不触及「主要功能」所述对象｜通过 |
| PGM-026 主要功能 | platform-governance-map-for-operations-v0.1.html:492 | 无本面｜本合成模块范围（6 个文件）不触及「主要功能」所述对象｜通过 |
| PGM-037 主要功能 | platform-governance-map-for-operations-v0.1.html:615 | 无本面｜本合成模块范围（6 个文件）不触及「主要功能」所述对象｜通过 |
| PGM-061 留痕和复核：重要动作不能只靠口头说明 | platform-governance-map-for-operations-v0.1.html:892 | 适用｜第 892 行「平台治理层要保证每个重要动作能追溯……结果是什么，都要能查到」：失败结果查不到｜不通过（随 CX-01） |
| PGM-062 必须留痕的动作 | platform-governance-map-for-operations-v0.1.html:896 | 适用｜必须留痕：「权限变更、临时授权和敏感权限使用」「终端开关……被拒绝访问」——AccessService 11 个带事务写方法的失败/拒绝留痕丢失（M031 登记即由此起）｜不通过（随 CX-01） |
| PGM-063 常见复核 | platform-governance-map-for-operations-v0.1.html:911 | 适用｜复核依赖留痕；失败事件缺失使复核无据｜通过（问题并入 CX-01） |
| PGM-064 正式挂起订单审计与结案 | platform-governance-map-for-operations-v0.1.html:923 | 无本面｜本合成模块范围（6 个文件）不触及「正式挂起订单审计与结案」所述对象｜通过 |
| PGM-066 已拍板规则和待排期问题 | platform-governance-map-for-operations-v0.1.html:951 | 适用｜已拍板规则中的留痕要求见 PGM-062｜通过（问题并入 CX-01） |

共 46 项；逐项给出适用与结论的 33 项，其余 13 项无本面。

## 7. 结论

席甲 B1 票：**FAIL**（CX-01、CX-02；观察另列）。范围全读、46 项逐项、三线完成；未改任何已跟踪文件；落盘前未读 Grok 本次正文。


## XM-M031-B B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-24T05:13:43Z。输入：两席 B1 完整 raw（席甲 V0.01＝上方本报告、Grok modules/XM-M031-B-b1-grok-raw.md V0.01）与候选 findings.md V4.02 顶部「当前 XM-M031-B B2-r1 整批候选」段（全文件 SHA256 79a9cbea78fd1e8a0e9714bf7814da1b32b2487c0ccc1fc4183208affa612861）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-01＝CX-01＋CX-02→001（同一根：recordFailure 参与调用方事务）。两席其余观察全部登记。无漏并、无改写。
- 自审纠正：B1 沿用登记的 47/27 未在 4a066def 上重数，采 Grok 的 46/25/31。B1 拟的「TransactionTemplate＋可选 setter」写法比注解多一层，采 Grok 的方法级注解；已核生产里唯一直接 new 的 AuditEventService（AuthService:64）是开发用空写仓储，不受代理缺席影响。
- 写入集合：回归放进 M095 的 EvidenceEventJdbcIntegrationTests。它不在开工段 6 个文件里，但同在判据并集的 M095 单元集内；开工段唯一测试是纯内存单测，钉不住传播语义，不扩到别的模块。
- 分类：001 细化（多条留痕原句的下位实现缺口；非冲突、非添加）。
- 最小性：生产只加一行注解（及 import）；测试只加真库回归，不改现有断言。
- 无添加项、无 questions、不 BLOCKED。
- 席甲 B2-r1 票：**PASS**（同意候选：001 入队；观察只登记）。


## XM-M031-B B2-r2 候选复核（席甲票，V0.03）

- 时间：2026-09-24T05:33:21Z。输入：findings.md V4.03 顶部「当前 XM-M031-B B2-r2 候选」段（全文件 SHA256 cbfcf8ec9ac25f54c69e84e5e87b7a72d37dec44aa8a4bfe8135a12850314af3）；B2-r1 两席追加段。本票落盘前未读 Grok B2-r2 追加段。不写独占锁行。
- 自审纠正：B2-r1 候选只看了 EvidenceEventJdbcIntegrationTests 已注入仓储与 JdbcTemplate，没读它的 @BeforeEach；Grok 读出清表会在 NOT_SUPPORTED 下提交。选回归宿主时须先读 @BeforeEach/@AfterEach 与类级注解，确认方法级 NOT_SUPPORTED 不会把夹具里的写操作变成真提交。
- 改写只动回归位置，001 的分类、处置、三条回归内容与 B2-r1 定稿相同。TraceabilityJdbcIntegrationTests 已核：类级 @Transactional、无 @BeforeEach、无 TRUNCATE/DELETE、@AfterEach 只清安全上下文。
- 无添加项、无 questions、不 BLOCKED。
- 席甲 B2-r2 票：**PASS**。
