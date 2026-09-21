CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M035 B5-r1 席乙（Grok）整模块全量重审 raw

- 版本：V0.01（指定路径此前不存在，本次创建；完整报告自上方独占锁行至文末）
- 步骤：B5-r1（修复后全量，非整份 diff；B3 累计 1）
- 席位：审计席乙，只读，grok-4.6 / xhigh
- 正本：`/home/james/complex-management` 分支 `main` 主工作区；HEAD `e456ba96c84185c8d39f18f548011daac48a6466`
- 计划：`plan/modules.md` V0.04；判据锁 V0.11；runbook V0.02；提示词 V0.17
- 独立性：未读 `modules/M035-b5-r1-self-raw.md`；全仓检索若扫到该路径片段一律丢弃。旧 B1/B2/B4 raw 与 `findings.md` V3.10 台账可读，不把「001–003 已否」当成模块已全对，不复用 B2/B4 票代替本轮逐项重审。
- 本席未改业务 / HTML / 锁 / findings / ledger；未 fetch / commit / push / merge / deploy；未建脚本、日志、临时产物；未对库发写语句（跑既有测试类除外）；psql 只 SELECT；未 source `/etc/complex/complex.env`。

## 0. 技能预检（环境实核，按意图执行）

本席实际读取并按提示词 §6 收窄口径使用：

| 技能 | 核到路径 | 本步用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能门；划掉项未调用 |
| using-arc | Arc `skills/using-arc/SKILL.md` + `references/platform-tools.md` | 用户指令优先于 Arc 全量派审（会第三席） |
| graft | `~/.claude/skills/graft/SKILL.md` | `graft skeleton AccessController.java`（tokens saved ≈ 2379） |
| verification-before-completion | superpowers 同目录 | PASS 前本席实跑 42 测 |
| grilling / grill-me | mattpocock `skills/productivity/grilling`、`grill-me` | 自问自答；grill-me 本体只转发 grilling；**不派子代理、不问人** |
| codebase-design | mattpocock engineering | 控制器 seam（WebMvcTest）vs 仓储/授权 seam（JDBC） |
| diagnosing-bugs / systematic-debugging | 各自 SKILL.md | 无失败需修；用于「改坏是否红」根因，不改代码 |
| tdd | mattpocock engineering | 对照反模式核假绿；B5 不写测试 |
| ponytail-review | ponytail 4.9.0 | 只找可删；001/003 已删豁免与过声称 |
| writing-for-agents | mattpocock productivity | 本 raw 完成判据可检查 |
| i-have-adhd | cache `i-have-adhd/0.1.0/skills/i-have-adhd` | 编号、无寒暄；与「CLI 不回正文」一并收窄 |
| long-running-background-tasks | grok bundled | Maven 一次跑完；**未**另建 watcher 日志（用户禁临时产物） |
| git-guardrails-claude-code | mattpocock misc（可选） | 存在；本席无 git 写，未启用 hooks |

缺口 / 划掉（如实，不冒充已调用）：

- `batch-grill-me`、`ubiquitous-language`：本环境无独立技能目录。
- `dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`research`、`ponytail-audit`：提示词划掉；未用 MCP / 子代理 / 第三席。
- Arc `audit` 默认派多审：与「只允许两席」冲突，未走。

## 1. 范围、行数、SHA（实算，不抄 modules.md）

`plan/modules.md` M035 文件清单仍是 2 个测试类。派发要求连同本轮新增前向迁移 `V430__permission_names_no_latin_ai.sql` 一起看（B2 已定：V430 是 M035-001 最小完整处置的一部分，不是扩范围）。after tree `e6db773a3ba42ff53c5b0fcb59a4847d8e00c0cb` 三者均在树内，工作区字节与树内 blob SHA 全等。

| 路径 | 行数 | SHA256 | git |
|---|---|---|---|
| `backend/src/test/java/com/complex/platform/access/AccessControllerTests.java` | 400 | `aa510618f93abe513d0429f4e630735b0e4732fbf854baa36ef0953e2964dff6` | tracked，工作区已改；16 个 `@Test`（另 1 处 `@TestConfiguration`） |
| `backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java` | 875 | `103a81c16e60e8a483689d8c758feab6d68e19ac9f8a9ee2ffe2f5b45d2259c0` | tracked，工作区已改；26 个 `@Test` |
| `backend/src/main/resources/db/migration/V430__permission_names_no_latin_ai.sql` | 8 | `d3229b9c7223127afb44b4527a4e7362c6eb7571e51cd84e1fea2dc11fc7bb47` | **未跟踪**；模式 644 |

`git ls-files -z -- <两测试> \| xargs -0 cat \| wc -l` = **1275**（400+875）。`modules.md` 写 1166，与现值不符——计量漂移，不是业务缺陷（B1 O-7 同类；不升队）。V430 不在 `modules.md` FILES 列表，本轮按 001 配套审。

判据锁：本席 `sha256sum docs/audit-2026-09/plan/criteria-lock.md` 独立实算，结果写在本文第一行；与 sidecar、ledger 顶部同值。未抄基准。`LOCK-AMEND-COUNT` 现值 1（只读观察）。

## 2. 取证命令（类型 2 / 4）

环境：从 `/etc/complex/complex.env` **解析**（不是 source）三个键 `COMPLEX_DATABASE_URL` / `COMPLEX_DATABASE_USERNAME` / `COMPLEX_DATABASE_PASSWORD`，URL 库名改 `complex_test`。解析结果：`PARSED_DB=complex_test`，`PARSED_USER=complex`，`SOURCED_ENV=no`。口令不写入本 raw。

```
psql -h 127.0.0.1 -U complex -d complex_test  （只 SELECT）
mvn -o test -Dtest=com.complex.platform.access.AccessControllerTests,com.complex.platform.access.AccessJdbcIntegrationTests
```

测前 SELECT：`flyway_schema_history` 最高 `430 success=t`；`analytics.risk_alert.view` 名＝「查看经营分析风险预警和人工智能质量台账」；`wecom.ai_qc.manage` 名＝「管理企微人工智能语义质检开关和适配器」；`permission_name ~ '[A-Za-z]'`＝0；`roles.role_name` 拉丁＝0；`permission_templates.template_name` 拉丁＝0；`permission_name ~ 'AI'`＝0；`permission_code ~ 'ai'`＝**10**（机读码，含 `wecom.ai_qc.manage` 等，V430 按设计不动）；夹具 `agr_m087_%` / `usr_jdbc_%` / UUID `2222…` 行数均为 0。

Maven：`Tests run: 16` / `26`，Failures 0，Errors 0，Skipped 0；合计 **42 / 0**；`BUILD SUCCESS`；exit 0。Flyway 日志：`Successfully validated 398 migrations`；`Current version of schema "public": 430`；`Schema "public" is up to date. No migration necessary.` JDBC 连的是 `jdbc:postgresql://127.0.0.1:5432/complex_test`。

测后 SELECT：Flyway 429/430 仍 success；拉丁权限名 / 名含 AI / 三类夹具残留仍全 0。类级 `@Transactional` 回滚成立，本轮未见顺序污染。

## 3. 第一性原理三问

### 3.1 这段东西为什么必须存在？删掉它，三份 HTML 哪一条做不到？

两测试 + V430 **不是** HTML 业务实现本身，是后台验证证据。删掉它们，权限分岗、职级三十档、模板/临时授权、全中文展示名、门控仍可存在于实现模块；HTML 不会立刻做不到。答不上「必须存在」≠该删，走 2.1：测试与前向迁移是已有权限/治理条目的下位验证与数据修复，**细化 / 技术性**，不是添加。V430 把上屏 `permission_name` 里的「AI」写成「人工智能」，对应 PGM-001「展示给用户的一切文字必须全部为中文，不得出现任何英文；可以不用中文的只有四样……没有第五样」。`permission_code` 属「系统内部标识符」，保留拉丁合法。

### 3.2 声称的行为有没有被实际执行验证过？

有。类型 2：42 测全绿 + Flyway 已在 430。类型 4：上节命令与 SELECT。类型 1：PGM-001 / PGM-066 全中文原文对照现行 SQL（已无 `replace(...,'AI','')`）。WebMvcTest 执行的是**真** `AccessController`（桩的是 `AccessService` / `AuthorizationService`），门控 verify / doThrow 打在真控制器调用上，不是用桩替换被测控制器。JDBC 类打真库、真 `JdbcAccessRepository`、真 `AuthorizationService`。

### 3.3 哪个前提未经验证就当真？

已核：Flyway 430、展示名拉丁计数、测后夹具残留、after tree 三文件 SHA。仍属观察、不升队的前提：JUnit 5 默认不并行（类上无 `TestMethodOrder` / `ExecutionMode`；两测共用 UUID `2222…`，并行才会撞，B1 O-4）；`@Transactional` 包住 `expireLapsedEffects` 的 UPDATE（无 `REQUIRES_NEW`，测后残留 0）；WebMvcTest 默认放行未 stub 的 `requirePermission`（所以成功路径必须靠 `verify` / 拒绝路径靠 `doThrow` 才能锁门控——002 已补的三条 verify 与拒绝测锁得住，其余无分支单权限入口仍按 B2「不铺矩阵」只登记）。

## 4. 顺从式：52 项已冻结映射逐项

映射原文：`criteria-lock.md` `M035 → [BEQR-157, BEQR-182…187, BSM-001,003,009,028,029,073,084…088,114,115,120, GEN-001…018, PGM-001,008,022,024,026,032…036,048,059,066]`，计数 7+14+18+13＝**52**，定义区标题 52 个全在映射区之前找到。不新增判据。

「适用」＝本模块文件对这条有可观察断言或配套数据；「无本面」＝本两测试+V430 不承载该锚点的实现/断言，不是漏审。

| ID | 本面 | 证据 |
|---|---|---|
| GEN-001 | 适用 | 两测试+V430 均有可验证用途；001 去掉豁免、003 收窄过声称，无新抽象 |
| GEN-002 | 适用 | 42 测可执行且与断言一致；Flyway 430 已应用 |
| GEN-003 | 适用 | 主判据。逐测见 §6；001/002/003 原假绿/过声称已不在 |
| GEN-004 | 适用 | 路径 id 覆盖请求体 `user_id`（控制器 L262–298 + 测试 L358–381）；幂等键 / `validUntil` 校验仍在 |
| GEN-005 | 适用 | 拒绝路径 doThrow + 三条成功路径 verify + 生命周期双/单码 + 审批双码；默认拒绝由 mock+doThrow 锁 |
| GEN-006 | 适用 | JDBC 真写入/撤销/到期；003 只锁 `@Transactional` 声明（名实已相符）；四步原子性不在本文件（M036） |
| GEN-007 | 适用 | 覆盖 upsert 幂等、到期日志 size=1、revokeTemplateApplications 第二次 0 |
| GEN-008 | 适用 | API 页形状 `code/data.items`；schema CHECK `user_scope_change`；风险四档约束 |
| GEN-009 | 适用 | `PERMISSION_DENIED` / `VALIDATION_FAILED` JSON；未吞错 |
| GEN-010 | 适用 | 未登录 401/403；他人有效集要 `access.user.view`；本文件无注入点。测试类密码回退字面量是夹具默认，不是生产密钥 |
| GEN-011 | 适用 | 到期写 `permission_template_usage_logs`；人工放行码授予源计数 |
| GEN-012 | 无本面 | 无批处理/连接泄漏断言 |
| GEN-013 | 适用 | 测试写死 `complex_test` + 三环境变量；离线 `-o` 可复现 |
| GEN-014 | 适用 | V430 顺序 430、测中 validate 398、不再 migrate；`permission_code` 不改；已应用不回改 |
| GEN-015 | 适用 | `seededDisplayNamesAreChinese` 现对 `[A-Za-z]` 直接红；夹具英文名不是界面（观察） |
| GEN-016 | 适用 | 003 测试名/注释与断言一致；V430 头注释与 SQL 一致 |
| GEN-017 | 适用 | 本轮无新业务发明；V430「人工智能」仍是同义中文写法（B2 已定，无新反证） |
| GEN-018 | 适用 | 结论落到 HTML 原文、42 测输出、SELECT |
| BEQR-157 员工状态 | 无本面 | 夹具 `access_users.status='active'` 不是「待入职/在职/试岗/停岗/离职」五态 |
| BEQR-182 考勤日结果 | 无本面 | 无出勤/缺勤断言 |
| BEQR-183 打卡方式 | 无本面 | 无 NFC/扫码打卡 |
| BEQR-184 请假类型 | 无本面 | 无请假 |
| BEQR-185 请假单状态 | 无本面 | 无请假单 |
| BEQR-186 薪资单构成 | 无本面 | `staff_level_assignments` 只落职级底薪字段，不是薪资单公式/20% 上限 |
| BEQR-187 员工提成发放 | 无本面 | 无提成/银行发放 |
| BSM-001 整体说明 | 适用（弱） | 「谁能操作」属基础盘子；本模块只验证不发明经营策略 |
| BSM-003 权限分岗 | 适用 | 门控、有效集、岗位模板、职级 |
| BSM-009 基础管理 | 适用 | 员工/权限/职级测试面 |
| BSM-028 主要功能 | 适用 | 「管理员工、岗位、班次、职级、权限」——三十档种子、定岗/职级入口、权限字典 |
| BSM-029 和谁有关 | 适用（弱） | 「用户权限要知道……职级」；注册/设备无本面 |
| BSM-073 商户专区 | 无本面 | 无商户门户账单/工单 |
| BSM-084 员工手机端 | 无本面 | 无私人手机 App |
| BSM-085 日常工作 | 无本面 | 无注册路径/代订 |
| BSM-086 排班考勤请假 | 无本面 | 无排班 |
| BSM-087 我的薪资 | 无本面 | 无工资条 |
| BSM-088 提成与人员服务 | 无本面 | 无提成现抽断言 |
| BSM-114 各类入口 | 适用（弱） | 审计/外包/平台管理员有效集隔离；入口矩阵本身在前端模块 |
| BSM-115 相互关系 | 适用（弱） | 基础管理连权限；无票码/库存连接 |
| BSM-120 已确认规则 | 无本面 | 本段拍板以购票/积分/通票为主，无本文件可执行点 |
| PGM-001 治理底座 | 适用 | 全中文段（HTML：「必须全部为中文，不得出现任何英文」；四样豁免无第五样）。资格/权限底座由门控与字典测试支撑。金额版本化等无本面 |
| PGM-008 员工和手机 | 无本面 | 不测私人手机绑定/企微设备 |
| PGM-022 空间和点位 | 无本面 | 无楼层图 |
| PGM-024 影响业务 | 无本面 | 无低消/容量 |
| PGM-026 商户主要功能 | 无本面 | 无招商线索 |
| PGM-032 员工和手机 h2 | 无本面 | 同 PGM-008 |
| PGM-033 主要功能 | 无本面 | 无注册双路径 |
| PGM-034 工作手机和企微 | 无本面 | 字典里 `wecom.*` 只作为展示名/审批标记出现，不测会话留存 |
| PGM-035 提成发放 | 无本面 | 同 BEQR-187 |
| PGM-036 设备管理 | 无本面 | 无领用/闸机 |
| PGM-048 商户侧 | 适用（弱） | 审计外部 `data_scope=project` 且无运行时 role_permissions；不能看平台全局的完整商户门户不在本文件 |
| PGM-059 个人信息保护 | 无本面 | 无手机号加密/脱敏断言（日志有 PII_BACKFILL_CLEAN，不是本测试断言） |
| PGM-066 已拍板 | 适用 | 「界面全中文……可以不用中文的只有……四样」+「权限点首批由平台统一预置」——展示名测试 + 权限种子 contains |

顺从式对照：本面与 HTML **无新冲突**。原 GR-03（挖掉 AI）已不在。映射未含 BEQR-149/151、PGM-040/041（权限公式/职级枚举正文）——B1 O-5，不扩锁。

## 5. 对抗式（grilling 结构，自答）

❓ **Q1 展示名测试现在对任何拉丁字母都红吗？**
➡️ 是。`AccessJdbcIntegrationTests.java:679-687` SQL 为 `permission_name ~ '[A-Za-z]'`（及 role_name / template_name），全文无 `replace(...,'AI','')`。SELECT 三类计数 0；两码现名含「人工智能」不含拉丁。往名里写 `AI` 或 `X` 都会让 `isEmpty()` 红。机读 `permission_code` 仍 10 条含 `ai`，不在本断言范围，与 PGM-001 第四样一致。

❓ **Q2 V430 会不会二次替换或改机读码？**
➡️ `replace(permission_name,'AI','人工智能')` 大小写敏感；「人工智能」不含 `AI`，再跑是空更新。Flyway 版本锁一次。SQL 只 UPDATE `permissions.permission_name`。SELECT：两码 code 仍 `analytics.risk_alert.view` / `wecom.ai_qc.manage`。

❓ **Q3 新补门控测试与成功路径 verify 是否真锁门控？**
➡️ **002 范围内是。** `listPermissions` / `listStaffLevels` / `assignStaffLevel` 成功路径有 `verify(authorizationService).requirePermission("<码>")`（L109/220/244）。对应控制器 L88/258/267。删这三处 `requirePermission`，verify 失败。生命周期：`approve_delete` 验复数 `requirePermissions(access.role.assign, governance.supervisor_review.approve)` 且 `never().requirePermission(access.role.assign)`；`archive` 相反（L315-336 vs 控制器 L196-200）。审批：`doThrow` 打在复数方法上，期望 403 且 `never().reviewGrant`（L341-354 vs 控制器 L153）。路径 id：Captor 断言 `userId()=="usr_path"` 而 body 是 `usr_body`（L358-381 vs 控制器 L269-277、L288-297）。`reject_delete` 与 `approve_delete` 同一 `if`，B2 已定测其一即可——无新反证。

❓ **Q4 PUT `/users/{id}/position` 成功路径去掉 `access.role.assign` 会红吗？**
➡️ **不会。** 新测 L371-380 只Captor `userId`，没有 `verify`/`doThrow` 该码。这是无分支单权限入口。B2 明文「不铺 33 入口矩阵 / 其余无分支单权限不铺矩阵」。本轮新证据只是「该入口现在被路径 id 测打到了」，缺陷类型没有从「未行使」变成「已行使成功路径假绿」所要求的新关键分支（关键分支已按 002 锁在双码 if/else、审批双码、路径覆盖）。**不改判、不升队。** 记观察 O-B5-1。

❓ **Q5 桩有没有替换被测逻辑造成假绿？**
➡️ 控制器测：被测是真 `AccessController`；桩是协作方。门控断言打在 `authorizationService` 调用上，删控制器里的调用会红（002 已补处）。JDBC：无 Mockito 替换 `JdbcAccessRepository`/`AuthorizationService`。`positionAndStaffLevelEntryPointsDeclareATransaction` 只读 `TransactionAttributeSource`（L665-671），**不调用**四步写入——测试名/注释已改成「只断言两个入口带事务声明」，与 003 定稿一致，不是假绿。

❓ **Q6 真库测试依赖残留或顺序吗？**
➡️ 测前测后夹具 ID 均为 0。无并行配置。UUID `2222…` 跨 `auditorReadOnly…` 与 `lapsedTemporaryGrant…` 两法；要并行才撞（O-4，要两件不巧）。`expireLapsedEffects` 无独立事务。`saveGrantPolicy` 是同连接 UPDATE，类事务回滚。

❓ **Q7 001/002/003 是否回归？**
➡️ 否。见 §7。

❓ **Q8 测试名/注释是否等于断言场景？**
➡️ 003 现名 `positionAndStaffLevelEntryPointsDeclareATransaction` + 注释 L661-663 与两条 `getTransactionAttribute` 断言相符。其余方法名与断言场景相符；`userPermissionOverrideEndpointGatesOnRoleAssignAndReturnsEffectivePermissions` 一段内含成功/缺幂等/拒绝，名称覆盖三者，不断言未发生的事。

## 6. 每个测试：改坏被测会不会红（GEN-003）

### 6.1 AccessControllerTests（16）

| 测试 | 行 | 改坏被测 | 会红？ |
|---|---|---|---|
| unauthenticatedAccessEndpointReturnsUnauthorizedOrForbidden | 57-65 | 无登录放行 200 | 会（`isIn(401,403)`）。改回 403 空 body **不会**（O-1，B2 只登记） |
| createTemplateWithoutPermissionReturnsPermissionDenied | 67-85 | 去掉 `access.permission_template.manage` | 会（doThrow 不触发 → 非 403） |
| listPermissionsReturnsApiResponsePageShape | 87-110 | 去掉 `access.permission.view` | **会**（L109 verify）。页形状坏也会红 |
| createPermissionTemplateMissingIdempotencyKeyReturnsValidationFailed | 112-125 | 去掉幂等校验 | 会 |
| assignUserRoleWithoutRoleAssignPermissionReturnsPermissionDenied | 127-145 | 去掉 `access.role.assign`（本入口） | 会 |
| createTemporaryGrantMissingValidUntilReturnsValidationFailed | 147-161 | 去掉 validUntil 校验 | 会。本入口门控无 verify（O-9 矩阵外） |
| userPermissionOverrideEndpointGatesOnRoleAssignAndReturnsEffectivePermissions | 164-204 | 去掉覆盖写入门控 | 会（同测第三段 doThrow）。成功段本身无 verify，靠拒绝段锁 |
| listStaffLevelsReturnsApiResponsePageShape | 206-221 | 去掉 `access.staff_level.view` | **会**（L220） |
| assignStaffLevelReturnsOkWithAssignment | 223-245 | 去掉 `access.staff_level.manage` | **会**（L244） |
| assignStaffLevelMissingIdempotencyKeyReturnsValidationFailed | 247-259 | 去掉幂等 | 会 |
| effectivePermissionsSelfReturnsGrantsDeniesAndWildcard | 261-280 | 序列化坏 | 会。夹具 `wildcard=true` 而实现恒 false（EffectivePermissions.java:13-20）；测的是字段能写出 true（O-2）。给 self 入口**多加**门控，因 mock 默认放行，本测不红——不升队 |
| userEffectivePermissionsWithoutUserViewPermissionReturnsPermissionDenied | 282-292 | 去掉 `access.user.view` | 会 |
| userEffectivePermissionsReturnsTargetUserSetWithUserViewPermission | 294-311 | 形状坏 | 会。门控由上一测锁 |
| positionTemplateLifecycleGatesDeleteDecisionsOnBothPermissions | 314-337 | 两分支门控形状反了或删了 | **会**（verify + never） |
| grantReviewRequiresBothReviewAndSupervisorApprovePermissions | 340-355 | 删复数门控或仍进服务层 | **会**（403 + never reviewGrant） |
| staffLevelAndPositionEndpointsUseThePathUserIdNotTheBodyUserId | 358-381 | 把路径 id 换成 body `usr_body` | **会**（Captor）。PUT position **只**锁 user_id，不锁门控（§5 Q4） |

WebMvcTest 日志出现 `PERMISSION_DENIED` 与 `VALIDATION_FAILED`（含「必须填写授权失效时间」「idempotency_key is required」），与拒绝/校验测相符。

### 6.2 AccessJdbcIntegrationTests（26）

类 `@SpringBootTest` + 类 `@Transactional`；注解写死 `complex_test`。无并行。桩：无。改种子/仓储实现，下列断言会红：

| 测试 | 行 | 锁住的行为 |
|---|---|---|
| migrationCreatesPermissionTablesAndSeedsAccessAndOpsPermissions | 45-64 | 表存在 + 若干权限码 |
| v181MovesOutsourcedFacilityPermissionsToTimeBoxTemplatesWithoutRuntimeRolePermissions | 66-147 | 外包权限在模板不在 role_permissions |
| v181SeedsConstructionCompletionUploadAndKeepsRuntimeRoleTimeBoxed | 149-210 | 进场表列 + 施工模板码 + 经理审核码 |
| v180SeedsAuditorRolesTemplatesAsReadOnlyWithoutChangingApprovalFlags | 212-263 | 审计只读；写后缀正则近似（O-3） |
| auditorReadOnlyAccessAndTemporaryGrantExpiryUseExistingAuthorizationEffects | 265-341 | 真 `requirePermission` / 到期拒绝 |
| outsourcedTemporaryGrantExpiryUsesExistingAuthorizationEffects | 343-402 | 外包临时窗 |
| migrationCreatesStaffLevelTablesAndSeedsThirtyContiguousRanks | 404-419 | **30** 档 rank 1..30 |
| staffLevelAssignmentPersistsLevelAndSalary | 421-434 | 职级+分+币种落库 |
| saveTemplatePersistsGrantAndDenyItems | 436-453 | GRANT/DENY 项 |
| copyTemplateStoresSnapshotIndependentFromLaterSourceChanges | 455-471 | 拷贝快照 |
| appliedTemporaryTemplateCanBeReadAsActivePermissionEffect | 473-502 | 应用后有效集 |
| revokingTemporaryGrantIdRevokesEveryPermissionCodeInGrantGroup | 504-554 | 整组撤销 |
| lapsedTemporaryGrantIsExpiredAndLoggedWhenEffectsAreRead | 557-589 | 到期 expired + 日志幂等 |
| lapsedTemplateApplicationIsExpiredAndLoggedWhenEffectsAreRead | 591-625 | 模板应用到期 |
| storedGrantPolicyParsesAndSavingKeepsDeadlineWorkingDays | 629-645 | 政策键保留（M032-001） |
| manualOverridePermissionIsRetiredAndGrantedToNobody | 649-659 | 人工放行码非 active 且四源 0（M034-001） |
| positionAndStaffLevelEntryPointsDeclareATransaction | 664-672 | **只**锁两入口 `@Transactional` 元数据；改坏四步写入**不红**（003 已改名实，不立项） |
| seededDisplayNamesAreChinese | 677-688 | **任何**展示名拉丁字母；含「AI」 |
| userPermissionOverridesAreUpsertedReadAsEffectsAndRemovable | 691-710 | 覆盖 upsert/移除 |
| moneyAndSensitiveExportPermissionsRequireApproval | 713-721 | 分红/导出/内部价须审批 |
| scopeChangeAndHighRiskReleaseRequestTypesAreAcceptedByTheGrantRequestCheck | 725-739 | CHECK 接受两类型、拒未知 |
| revokeTemplateApplicationsMarksOnlyTheGivenActiveApplicationsRevoked | 742-758 | 冻结幂等 |
| sensitiveBusinessDataPermissionsRequireApprovalAndStayOutOfThePlatformAdminBridge | 762-769 | 四码审批且不进日常桥 |
| dailyCodesReachThePlatformAdminBridgeAndRiskTiersAreTheFourGovernedOnes | 774-789 | 四风险档 + 日常桥 + 自助申请码 |
| fiveCategoryHighRiskCodesRequireApprovalBeforeGrant | 794-802 | 五类高危 |
| platformAdminRoleSeedYieldsGovernanceWriteCodesButNotBusinessHighRisk | 805-815 | 治理写码有、资金/内部价/审批码无 |

夹具英文模板名（`jdbc-items` 等）只活在方法事务里；测后 `latin_tpl=0`。

## 7. 已定稿 001 / 002 / 003 回归（不把「否」当过关）

findings V3.10 队列均为否（已修）。本席按现行文件+库+42 测重核：

**M035-001（冲突，PGM-001 / GEN-003 / GEN-015）**  
原句已不在：不再有 `replace(permission_name, 'AI', '')`。现断言 L679-687。V430 全文：

```
UPDATE permissions
   SET permission_name = replace(permission_name, 'AI', '人工智能')
 WHERE permission_name ~ 'AI';
```

库内两码已是「人工智能…」；拉丁计数 0。把实现改回带拉丁的展示名，本测会红。**回归不成立。**

**M035-002（技术性，GEN-003 / GEN-005）**  
三条 verify 在 L109/220/244；生命周期 L315-336；审批 L341-354；路径 id L358-381。删对应控制器门控或改用 body user_id，这些测会红。未改被测代码、未铺 33 矩阵。**回归不成立。**

**M035-003（技术性，GEN-003 / GEN-016）**  
方法现名 `positionAndStaffLevelEntryPointsDeclareATransaction`（L665）。注释 L661-663：「只断言 assignUserPosition 与 assignStaffLevel 两个入口带事务声明，不执行四步写入本身」。断言仍是两条 `getTransactionAttribute` `isNotNull`（AccessService L577、L1298 确有 `@Transactional`）。**名实相符；回归不成立。**

## 8. 四分类（本轮新问题）

按 冲突 → 细化 → 技术性 → 添加。本轮 **无新工程缺陷**。无需添加项、不写 questions、不申请改三 HTML。

V430「人工智能」：仍归 001 的细化/技术性修复（同义中文，无新业务对象）。无新证据把它改判为添加。

## 9. ponytail-review（可删）

001 的 `replace(...,'AI','')` 与 003 过声称注释已不在。V430 三行 SQL，无多余抽象。控制器新测没有第二套矩阵。`net: 0`。Lean already. Ship（就本模块修复面）。

## 10. 观察（只登记，无新证据不改判）

沿用 B2 双席已定为只登记者，现状核对：

- **O-1** 未登录 `isIn(401,403)` 仍不能为回归 403 变红。SecurityConfig.java:64-66 仍把无登录 403 当已修缺陷。
- **O-2** 夹具 `wildcard=true` vs 实现恒 false。
- **O-3** 审计只读后缀正则近似。
- **O-4** UUID `2222…` 跨两测；测后 0 行；无并行。
- **O-5** 映射不含 BEQR-149/151、PGM-040/041；不扩锁。
- **O-6** 控制器方法级映射约 33，不是「34」。
- **O-7** `modules.md` 1166 vs 实算 1275（+V430 8 行未入 FILES）。
- **O-8** DENY 覆盖 GRANT 的组合在 M036，本文件无。
- **O-9** 其余无分支单权限入口不铺矩阵。
- **O-10** 控制器夹具 `"View production readiness"` / `"Ops template"` 不是界面文字。
- **O-11** 可删的豁免/过声称已由 001/003 删掉，本项关闭。
- **O-B5-1**（新观察，不升队）PUT position 现被路径 id 测打到，仍无 `access.role.assign` 的 verify/doThrow；分类仍是 O-9 的无分支单权限，不是 002 关键分支。见 §5 Q4。

## 11. findings（本席临时编号）

无。不分配 GR-B5-*。不把观察升队。

## 12. 覆盖声明

- 文件：`AccessControllerTests.java` 400 行全文；`AccessJdbcIntegrationTests.java` 875 行全文；`V430__permission_names_no_latin_ai.sql` 8 行全文。无抽样。
- 判据：52/52 逐项记录适用或无本面。
- 三线：第一性原理 §3、顺从式 §4、对抗式 §5–6 均跑完再下结论。
- 未中途回退或修复。

## 13. 结论

001、002、003 在现行两测试 + V430 + `complex_test` Flyway 430 上落地，42 测全绿，无回归，无新的可立项缺陷。观察保持只登记。

**本席 B5-r1 审计结论：PASS**
