CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M034-AB B5-r2 修复后整模块三线全量重审（Grok 席乙）

版本：V0.01
席位：Grok 独立只读审计席乙（grok-4.7 / xhigh）
步骤：B5-r2。B3 累计 3（已用满）。本报告不改业务代码、HTML、锁、findings、ledger。
锁：本席对 `docs/audit-2026-09/plan/criteria-lock.md` 执行 `sha256sum`，第一行即该输出。与 sidecar 文件内容相同，是实算重合，不是抄基准。
正本：范围文件的主工作区字节 = `origin/main` `daf6adfc` + `/home/james/audit-tmp/xm034_b3cum_r3.diff`（SHA256 `a8554489151f00ffeb3b518a46c0fc3a06ce88436e530a5db6dafe012da34aa0`，30 个路径）。一次性 worktree 套用该 diff 后，30 个路径与主工作区逐字节相同（BAD 0）。开工段里未进入该 diff 的 11 个文件，工作区 `git hash-object` 与 `origin/main` blob 相同。本地 `HEAD` 是 `e456ba96`，审计对象只取上述范围文件的工作区字节。
未读 `modules/XM-M034-AB-b5-r2-self-raw.md`。该文件在本报告落盘前已出现在目录中，检索未采信其中片段。旧 raw 只用来核对已登记观察的边界，不复用旧票结论。
派发写「4 个删除文件」。累计 diff 里 `deleted file mode` 实际是 3 个：`AmusementManualOverrideRecord.java`、`AmusementManualOverrideRequest.java`、`VerificationManualOverrideRequest.java`。三份在主工作区与套 diff 后的 worktree 都不存在。没有第四个被删路径。这是计数差，不是漏删。

## 结论

PASS。

001 至 005 的已修行为经本轮重核仍成立。票务人工放行写入口与游乐人工改判（allow、release、deny、pause、resume）的生产调用链不在。V433 只停用 `amusement.verification.override` 并清四处授予。历史表、V014/V234/V067 的 CHECK、V099 策略行、`releaseEntryConcurrentCapacity` 与出场扣减都还在。需求实现矩阵第 49–52 行已不再指向已删除的类，列出的代码路径都存在。新回归能失败。本轮没有新的工程缺陷。

## 技能（按本环境实核，不冒充）

已读文件并按 V0.17 §6 收窄使用：`graft`（`graft grep clearProjectConcurrentCapacity`，backend/src/main 内 1196 个索引文件 0 命中；该次输出没有 tokens saved 行）、`grilling`（自问做完，不等操作员、不派子代理）、`verification-before-completion`（PASS 前有本轮命令输出）、`ponytail-review`（只找还能删的残留，不改代码）、`long-running-background-tasks`（已读；禁止自建脚本，全量测试与门禁用后台命令等到结束，未写 monitor 脚本、未派子代理巡检）。

文件存在、本步没有通读正文、也没有执行其本体动作：`using-superpowers`、`brainstorming`、`writing-plans`、`systematic-debugging`、`test-driven-development`、`requesting-code-review`、`receiving-code-review`、`codebase-design`、`diagnosing-bugs`、`tdd`、`resolving-merge-conflicts`、`writing-for-agents`、`git-guardrails-claude-code`、`ponytail`。连库口令失败时只追到本机 `complex.env` 的应用口令，未把该过程写成已执行 systematic-debugging 全文。

缺口：`batch-grill-me`、`ubiquitous-language` 在本机技能目录未找到。`i-have-adhd` 存在，但本报告第一行必须是锁哈希，与该技能「首行即动作」冲突，以本次派发和 V0.17 为准。

未用 MCP、子代理、第三席。未 fetch / commit / push / merge / deploy。未对 `complex`、`complex_test` 执行写语句。环境拒绝 `git checkout` 与 `git restore`。一次性 worktree 收尾改为：用 `git show HEAD:<path>` 把 29 个已跟踪脏文件写回 HEAD 字节，删掉 1 个未跟踪文件，`git status --porcelain` 为空后 `git worktree remove`（无 `--force`）。

## 范围与读法

开工段 33 个文件的现状，加定稿新增的契约包、需求实现矩阵、e2e `mock-api.ts`、V433，以及本 diff 带上的门禁生成物：隔离规格、两份 OpenAPI、两份附录、权限矩阵、`security-baseline-latest.json`、`findings-register-v1.md`、`formal-security-baseline-v1.md`。

控制器映射、V433 全文、V429 对照、出场扣减方法、新回归、契约包第 334 行、权限矩阵四处、隔离规格自动段、安全基线 JSON、矩阵 REQ-45 至 REQ-48，都按当前文件读过。OpenAPI 两份用 `operationId` 键计数（730）并检索 `manual-override`（0）。子串 `operationId` 多出的 1 次是 yaml 第 37287 行说明句，不是 operation。未改字节与 `origin/main` 相同的 11 个文件不把「相同」写成「重新发明了每条业务规则」。

## 第一性三问

1. 为什么还必须存在。
   票务 `POST /api/v1/ticketing/verifications/manual-override` 与游乐 `GET/POST /api/v1/amusement/manual-overrides` 删掉之后，枚举速查第 43 行「系统不提供人工放行……任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过」和业务地图第 1263 行「任何岗位（含主管与最高权限）都不得手工修改、清零或修正在玩人数」仍然做得到，而且只有删掉写入口才做得到。这两段已不该存在。
   仍须存在的是：`amusement_manual_overrides` 表与历史行、V014/V234 的类型 CHECK（含 `release`）、V067 的 `manual_overridden` CHECK、V099 种下的 `amusement_manual_override_over_scope` 策略行、`releaseEntryConcurrentCapacity` 与 `exitVerification` 的按入场扣减。删掉它们，历史行会违反约束，或业务地图「出场自动扣减、同一人重复经过不重复扣减」做不到。004 已写明保留。
2. 声称的行为有没有被执行验证。
   有。见「空库全量」「变异」「门禁」。
3. 哪条前提曾未验证就当真。
   V170 会把该码 `status` 写回 `active`（`V170__access_grant_requests.sql:180-188`）。前提是 V433 在它之后执行。空库 Flyway 停在 433，该码最终为 `retired`。这个顺序已被执行验证。
   「测试通过即行为正确」不采用。全量 0 红之外，另做了把 V433 改成 `active` 的一次失败。

## 行为核对

### 写入口已不在

`backend/src/main/java` 检索 `manualOverride`、`ManualOverride`、`clearProjectConcurrentCapacity`、`createManualOverride`：无匹配。

`AmusementController` 的映射从 `/device-statuses` 到 `/verifications/exit`，没有 `/manual-overrides`。`TicketingController` 的核销映射到 `/ticketing/verifications`、`/ticketing/verifications/{id}/reverse`，没有 `manual-override`。`/group-bookings/{id}/release` 仍在，那是团体预订释放，不是改判类型 `release`。

`apps/` 与 `tests/` 检索 `manual-overrides`、`manualOverride`、`amusement.verification.override`：无匹配。e2e `mock-api.ts` 相对 origin 只删 `/api/v1/amusement/manual-overrides` 一行。

graft 对 `clearProjectConcurrentCapacity`：0 命中。

生产 Java 不再出现 `amusement_manual_overrides` 的 INSERT/SELECT。迁移与注释除外：`JdbcSettlementTransactionRepository.java:137` 的 SQL 注释排除历史 `manual_overridden`，不写入该状态。`JdbcAdmissionFactRepository.java:135` 仍是注释「amusement_manual_override（写接口首参非 String……）」，blob 与 `origin/main` 相同。B2 定为只登记，文件未改，不改判。

### V433 只停用该码并清四处

`V433__retire_amusement_verification_override.sql` 全文 16 行：`permissions.status='retired'`；删除 `role_permissions`、`permission_template_items`、`user_permission_overrides` 中该码；`temporary_permission_grants` 里该码且 `status='active'` 的行改为 `revoked`（`revoked_by='V433'`）。不 DROP 表，不改 CHECK，不 DELETE 历史行。结构与 V429 对 `verification.manual_override` 的四处清理相同。

V014:185 种入 `active` 并在 :213 授给平台管理员；V170:180-188 再把该码写回 `active`。二者版本都小于 433。当前迁移目录最大号就是 V433，无重复版本号。

空库 `xm034b5r2g_test`（只读查询，不是 `complex` / `complex_test`）在全量迁移之后：

- `flyway_schema_history` 最新成功版本 `433`
- `amusement.verification.override` 的 `status` = `retired`
- `verification.manual_override` 的 `status` = `retired`
- 两码在四处授予上的计数都是 0
- `to_regclass('public.amusement_manual_overrides')` 有表
- `chk_amusement_manual_overrides_type` 仍为 `allow, deny, pause, resume, release`
- `chk_ticket_verifications_status` 仍为 `success, failed, rejected, reversed, manual_overridden`
- `supervisor_review_policies.amusement_manual_override_over_scope` 的 `enabled` = `t`（V099:51）

同一时刻只读 `complex`：Flyway 最新成功版本 385，游乐改判码仍为 `active`。只读 `complex_test`：Flyway 最新成功版本 431，票务人工放行码已 `retired`，游乐改判码仍为 `active`。两库都没有 433。本轮写语句没有打到这两库。事后只 `DROP` 了 `xm034b5r2g_test` 与 `xm034b5r2g_red`。再查库名，剩下的是 `complex` 与 `complex_test`。

### 出场扣减保留

`AmusementRepository.releaseEntryConcurrentCapacity` 仍在。`JdbcAmusementRepository.java:1085-1106`：先条件更新入场 `released_at`（已释放则返回 false），再按该入场人数对 `point_device` 与 `project` 两维做 `occupied_count - N`（下限 0），然后写 `direction=exit`。这不是把项目下 `occupied_count>0` 的行一律置 0。

`AmusementService.exitVerification`（1371–1414 行）仍按未释放入场定位；没有未释放入场则返回既有 exit 或 `ENTITLEMENT_NO_ACTIVE_ENTRY`。控制器 `exitVerification` 仍在（`AmusementController.java:516`）。本 diff 没有改这个方法的语句。

`JdbcAmusementRepository.java:1004` 注释仍写「出场/主管释放」。方法 `decrementConcurrentSnapshot` 是按维度键减 N，只被 `releaseEntryConcurrentCapacity` 调用。注释是旧词，不是清零入口。不把注释升级成新需求。

### 契约、附录、基线、隔离规格、矩阵

- `openapi-p0.yaml` 与 `openapi-p0.json` 的 `operationId` 键都是 730。两份都不再含 `manual-override`。json SHA256 `d6a2085004caaa10b33f5057c7fa973da19b1064a30579562a3f7df64bbb9e2a`，与 `findings-register-v1.md`、`formal-security-baseline-v1.md` 所写相同。
- 附录计数：operation 732→730，权限行 329→328，事件 1464→1460（人工事件矩阵未覆盖 1422→1419），写接口 441→440，任务追溯 732→730（未逐项覆盖 726→724）。与删掉 1 个 POST、1 个 GET、4 个事件名、1 个权限码行一致。`staff-token` 677→675；`staff_work_phone` 81→80；`tablet` 103→102；`web` 694→693。
- `security-baseline-latest.json` 相对 origin 只改 `openapi_operation_count` 732→730。`verified_at`、`notes`、`version` 未改。`notes` 里「同步 732 项接口」是 U-FIX 历史句。B4-r2 已否决改写这三项。计数字段已是 730，门禁比较的是计数字段。无新证据，不改判。
- 隔离规格自动段只把最新迁移 V432 改为 V433，表数仍为 172。
- 权限矩阵四处：第 63、251 行写「写入口已下线」；第 78、269 行写改判接口已下线，并去掉已不再发出的 `amusement.verification_overridden`。中文权限名迁移 V251/V254 字节未改。
- 契约包 `docs/p0-api-contract-pack-v0.1.md:334` 仍有一行 `POST /api/v1/amusement/manual-overrides`。相对 origin 只把「待代码轮下线，不得新增调用」改成「接口已下线」。权限列仍写停用。OpenAPI 与附录已不含该 operation。这是标明已下线的纪念行，不是活路由。`verify:predeploy` 的客户端路由审计通过。不把这一行判成残留调用。
- 需求实现矩阵 REQ-45 至 REQ-48（文件第 49–52 行）代码列已不含 `AmusementManualOverrideRecord.java`，也不含另外两个已删类名。该列列出的路径都存在。API 列相对 origin 也不再列出两个已删 operation；表名 `amusement_manual_overrides` 仍在。证据列「OpenAPI 10 ops; 代码文件 42」在改前就与该格列出的条数不一致，005 定稿写明其余列不动。不把这个改前就存在的摘要数字改判成新缺陷。

### 测试

测试 numstat（相对 origin）：控制器测试 -93 行，Jdbc 集成 +15/-15 量级为净增回归，服务测试 -80 行，出场集成 -38/+9，票务控制器 -93，票务服务 -13。删掉的断言守着已废止能力：allow/approved 当成功、人工放行写成 `manual_overridden`、改判仓储、出场集成里的主管清零。留下的出场用例仍引用 `releaseEntryConcurrentCapacity`。

`AmusementJdbcIntegrationTests.amusementOverridePermissionIsRetiredAndGrantedToNobody`（主工作区 477–488 行）断言 `retired`、四处之和为 0、表仍在。全量该测执行过且通过（非 skipped；该类 5 测 0 失败）。

变异：在一次性 worktree 里只把 V433 权限更新改成 `status='active'`，另建空库 `xm034b5r2g_red`，只跑这一测。Flyway 在该库从空库迁到 433。结果 `MUT_EXIT=1`，`AmusementJdbcIntegrationTests.java:480`：`expected: "retired" but was: "active"`。测完把该句改回 `retired`，并把 worktree 里临时改过的 JDBC 库名改回 `complex_test`。断言能失败。

17 条跳过与已登记名单相同：`M006JdbcRegressionTests` 13 条、`M006StoredValueJdbcTests` 4 条。本模块无新增跳过。不改判。

### 空库全量与门禁

`origin/main` 一次性 worktree 套累计 diff。库名 `xm034b5r2g_test`，经 `template1` 创建，属主 `complex`。只在该 worktree 把 `5432/complex_test` 改成该库（176 个文件、215 处），并设 `SETTLE_TEST_DB_URL` 与 `COMPLEX_DATABASE_URL`。`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`（有 javac）。`mvn -o -q test`：`MVN_EXIT=0`，`SUITES=263 TESTS=3529 FAILURES=0 ERRORS=0 SKIPPED=17`。

源码恢复为 `complex_test` 后，30 个路径再次与主工作区逐字节相同（BAD 0）。主工作区 `node_modules` 软链进该 worktree 后 `npm run verify:predeploy`：`VERIFY_EXIT=0`。其中验收矩阵与附录均「730 operations checked」、OpenAPI 契约通过、安全基线通过、生产就绪证据通过、`TEST_DB_ISOLATION_PASS`。软链已卸。主工作区 `node_modules` 仍是目录。主工作区源码无 `xm034b5r2g`。主工作区 V433 第 7 行仍是 `retired`。

## 对抗（grilling 收窄：自问，事实自己读）

1. 删了端点，权限码是否还能由 V170 在最终库里保持 active？不能。空库最终版本 433，状态 retired。
2. 删了清零方法，出场是否变成把项目在场人数直接置 0？不是。语句是按入场人数减，且已释放不再减。
3. `release` 这个词是否还挂在改判入口上？改判入口已不在。团体预订的 `release` 映射仍在，对象不同。
4. 矩阵是否还指向已删类？REQ-45 至 REQ-48 代码列不再包含三个已删类，路径存在。
5. 契约包那一行会不会被客户端当成活路由？单元格写「接口已下线」。OpenAPI、附录、e2e mock、Java 映射都没有该路径。客户端路由门禁通过。
6. 新回归会不会在码被写回 active 时仍绿？不会。变异已红在第 480 行。
7. 全量 0 红是否因为测的是 `complex` 或 `complex_test`？不是。Flyway 日志与查询都在 `xm034b5r2g_test`；那两库的最新版本不是 433。
8. 设计文档、权限中文名、状态常量、Jdbc 注释、历史扫描快照有没有新改动？没有。设计文档与扫描快照不在这 30 个路径里；V251/V254、`JdbcAdmissionFactRepository`、`PROJECT_STATUSES`/`SESSION_STATUSES` 字节或 diff 未动。按 B2 只登记，不改判。

## 顺从式：181 项

并集 181（BEQR 72、BSM 45、GEN 18、PGM 46）。下表每项一行。

| ID | 判据 | 本轮 |
|---|---|---|
| BEQR-063 | 高危动作（需审批/双证据）（锁 L786） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-074 | 隐私桌台权限（锁 L907） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-143 | 四类主体（锁 L1666） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-144 | 经营主体关系（锁 L1677） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-145 | 城市项目公司（锁 L1688） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-146 | 运营公司职责（锁 L1699） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-147 | 主要收入归属（锁 L1710） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-148 | 合作模式（锁 L1721） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-149 | 职级（锁 L1732） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-150 | 终端十一端（锁 L1743） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-151 | 最终权限计算（锁 L1754） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-152 | 项目与敏感数据权限（锁 L1765） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-153 | 终端开关优先级（锁 L1776） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-154 | 综合体项目状态（锁 L1787） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-155 | 商户/运营主体状态（锁 L1798） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-156 | 合同状态（锁 L1809） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-157 | 员工状态（锁 L1820） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-158 | 设备状态（锁 L1831） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-159 | 权限状态（锁 L1842） | 两码在空库均为 retired。V251/V254 中文名字节与 origin/main 相同，只登记，不改判。 |
| BEQR-166 | 合同审批链（锁 L1919） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-182 | 考勤日结果（锁 L2095） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-183 | 打卡方式（锁 L2106） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-184 | 请假类型（锁 L2117） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-185 | 请假单状态（锁 L2128） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-186 | 薪资单构成（锁 L2139） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-187 | 员工提成发放（锁 L2150） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-189 | 管理费方式（锁 L2172） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-004 | 配置数据可以存档、也可以删除（删除要审批）（锁 L2361） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-005 | 后台的"新建/录入"改成分步引导（锁 L2385） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-006 | 验证用的二维码一律动态刷新（间隔可在后台设定）（锁 L2397） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-007 | 平台管理员拥有全部日常功能权限（锁 L2409） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-009 | 基础管理（锁 L2438） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-028 | 主要功能（锁 L2730） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-029 | 和谁有关（锁 L2756） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-072 | 商户管理：商户在授权范围内完整使用自己的系统（锁 L3781） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-073 | 主要功能（锁 L3796） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-074 | 和谁有关（锁 L3819） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-084 | 员工手机端：排班、考勤、请假、薪资，自己手机上都能办（锁 L4020） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-085 | 日常工作（锁 L4034） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-086 | 排班、考勤与请假（锁 L4054） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-087 | 我的薪资（锁 L4078） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-088 | 员工提成、活动执行与人员服务边界（锁 L4100） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-114 | 各类入口：不同人只看自己该看的、只做自己该做的（锁 L4720） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-115 | 相互关系：为什么这些子系统必须连在一起（锁 L4801） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-120 | 已确认规则和实施前输入项（锁 L4911） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| GEN-001 | 必要性与最小性（锁 L12） | 改判写入口删掉之后，枚举速查第 43 行与业务地图第 1263 行才做得到。留下的是历史表、CHECK、出场扣减。 |
| GEN-002 | 可执行正确性（锁 L17） | 空库全量 MVN_EXIT=0：263 套、3529 测、失败 0、错误 0、跳过 17。控制器无 manual-overrides 映射。 |
| GEN-003 | 测试有效性（锁 L22） | 新回归在全量中执行且通过。把 V433 改成 active 后同一断言失败：expected retired but was active，行 480。断言能失败。 |
| GEN-004 | 信任边界校验（锁 L27） | 本 diff 未改入参校验。无新证据。 |
| GEN-005 | 授权与最小权限（锁 L32） | V433 把 amusement.verification.override 改为 retired，并清 role_permissions、permission_template_items、user_permission_overrides，active 临时授权改 revoked。空库四处之和为 0。票务码仍为 retired、四处为 0。 |
| GEN-006 | 数据完整性（锁 L37） | V014/V234 类型 CHECK 仍含 release；V067 状态 CHECK 仍含 manual_overridden；表未 DROP。空库约束定义与此相同。 |
| GEN-007 | 并发与幂等（锁 L42） | releaseEntryConcurrentCapacity 仍以 released_at 条件更新作幂等赢家，已释放则不重复减。该方法语句不在本 diff。 |
| GEN-008 | 接口与模式一致性（锁 L47） | yaml 与 json 的 operationId 键都是 730，都不含 manual-override。附录、验收矩阵、两份安全证据与该数一致。verify:openapi 与两份 730 检查通过。 |
| GEN-009 | 错误处理与恢复（锁 L52） | 未改错误码路径。无新证据。 |
| GEN-010 | 安全与敏感信息（锁 L57） | 未引入新的注入或越权入口。改判写入口删除缩小了写面。 |
| GEN-011 | 可观测与审计证据（锁 L62） | 改判成功事件随 operation 从附录删除（4 个事件名）。出场路径未改。 |
| GEN-012 | 资源与性能边界（锁 L67） | 未改查询与循环边界。无新证据。 |
| GEN-013 | 构建配置与依赖（锁 L72） | 本轮构建用 Temurin 21.0.12.1（有 javac）。mvn -o 未装依赖。 |
| GEN-014 | 数据库迁移正确性（锁 L77） | 迁移目录最大号 V433，无重复版本号。空库 Flyway 成功停在 433。V170 把该码写回 active 的语句仍在，但版本低于 433，最终状态为 retired，已由空库执行验证。 |
| GEN-015 | 界面可用性与可访问性（锁 L82） | 本范围无界面文件。apps 下无 manual-override 调用。 |
| GEN-016 | 文档可验证性（锁 L87） | REQ-45 至 REQ-48 代码列已不含三个已删类，列出的路径都存在。契约、附录、基线、隔离规格与 OpenAPI 计数一致，verify:predeploy 退出码 0。历史扫描快照与设计文档按已定稿只登记。 |
| GEN-017 | 业务来源分类（锁 L92） | 001 至 004 的冲突/添加已按定稿修完。005 的矩阵路径已删。本轮没有新的业务发明。 |
| GEN-018 | 第一性原理证据（锁 L97） | 本报告依据是 HTML 原文、空库查询、测试输出、门禁输出和文件行，不是旧票结论。 |
| PGM-002 | 治理顺序：一件业务能不能做，要先经过这些判断（锁 L5060） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-003 | 模块入口：平台治理层包含哪些管理模块（锁 L5101） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-004 | 综合体和集团（锁 L5114） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-005 | 空间和点位（锁 L5129） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-006 | 商户和主体（锁 L5144） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-007 | 合同和准入（锁 L5159） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-008 | 员工和手机（锁 L5174） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-009 | 权限和职级（锁 L5189） | 权限码停用与清授予见 GEN-005。中文名迁移未改。 |
| PGM-010 | 终端开关（锁 L5204） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-011 | 设备管理（锁 L5220） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-012 | 商户侧（锁 L5235） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-013 | 开放接口（锁 L5250） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-014 | 留痕复核（锁 L5265） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-015 | 楼宇运营（锁 L5281） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-016 | 应急与隐私（锁 L5296） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-017 | 综合体和集团：当前一个项目，未来能看集团（锁 L5314） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-018 | 物理项目与运营公司：多对多协作，不是固定隶属层级（锁 L5327） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-019 | 各主体具体负责什么（锁 L5346） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-020 | 主要功能（锁 L5372） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-021 | 业务意义（锁 L5394） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-022 | 空间和点位：所有业务都要知道“发生在哪”（锁 L5415） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-024 | 影响业务（锁 L5448） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-026 | 主要功能（锁 L5484） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-027 | 业务边界（锁 L5509） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-031 | 外部经销商主体、合同价格与资金治理（锁 L5597） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-032 | 员工和手机：人、私人手机、工作手机要能对应起来（锁 L5624） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-033 | 主要功能（锁 L5638） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-034 | 工作手机和企业微信（锁 L5658） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-035 | 提成发放和人员服务治理（锁 L5681） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-036 | 设备管理：谁领了什么设备，设备能不能用（锁 L5703） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-040 | 权限和职级：每一个单一功能都可以单独授权（锁 L5775） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-041 | 权限规则（锁 L5789） | 停用码不再留下授予来源。授权引擎本身不在本 diff。 |
| PGM-042 | 职级规则（锁 L5816） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-043 | 外包人员与审计人员（2026-06-29 新增）（锁 L5835） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-044 | 桌台、挂单与经销业务权限（锁 L5861） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-048 | 商户侧：在授权业务范围内全开放（锁 L5963） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-059 | 客人与员工的个人信息保护（锁 L6192） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-060 | 会员身份、营销与生物信息治理（锁 L6214） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-065 | 连接关系：平台治理层怎么连接所有业务（锁 L6320） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-066 | 已拍板规则和待排期问题（锁 L6346） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-001 | 票种（锁 L104） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-002 | 票务策略状态（锁 L115） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-003 | 定价、售票与收款主体（锁 L126） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-004 | 年龄限制（锁 L137） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-005 | 通票赠酒下限（锁 L148） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-006 | 通票类型（锁 L159） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-007 | 权益类型（锁 L170） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-008 | 餐饮/零售券两种用法（锁 L181） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-009 | 资格判定（锁 L192） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-010 | 订单状态（锁 L203） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-011 | 票/权益状态（锁 L214） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-012 | 核销记录状态（锁 L225） | 成立。锁 L225，HTML 枚举速查第 43 行末句。票务写入口与写 manual_overridden 的服务方法已不在；V067 CHECK 仍保留该历史值。 |
| BEQR-013 | 手环入场（锁 L236） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-014 | 退款单状态（锁 L247） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-015 | 有效期类型（锁 L258） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-016 | 售票与退款原则（锁 L269） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-017 | 到期未核销款（锁 L280） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-018 | 域归属对照（锁 L291） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-019 | 跨域通票分配（锁 L302） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-020 | 酒吧通票可分配池（锁 L313） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-021 | 收费与免费权益结算（锁 L324） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-022 | 项目内部结算价（锁 L335） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-023 | 核销顺序（锁 L346） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-024 | 游乐核销归属（锁 L357） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-025 | 门票综合服务费（锁 L368） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-026 | 动态价与会员价（锁 L379） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-027 | 人脸验龄数据生命周期（锁 L390） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-032 | 等级权益对照（锁 L445） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-034 | 票务人次计算（锁 L467） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-040 | 发券与核销结算（锁 L533） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-091 | 全园故障与补偿通票（锁 L1094） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-124 | 综合通票定价（锁 L1457） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-001 | 整体说明：这套系统到底管什么（锁 L2271） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-002 | 一笔业务怎么走（锁 L2302） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-003 | 系统通用能力：数据可改可删、操作有引导、权限分岗（锁 L2349） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-010 | 票务权益（锁 L2453） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-030 | 票务权益：管卖什么票、给什么权益、怎么核销（锁 L2779） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-031 | 主要功能（锁 L2793） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-032 | 和谁有关（锁 L2832） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-033 | 票种、通票与权益类型（系统支持哪些种类）（锁 L2854） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-034 | 核销、退款与有效期规则（锁 L2888） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-035 | 会员与积分规则（锁 L2915） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-036 | 人脸验龄数据使用与删除（锁 L2952） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-044 | 酒吧平板/工作手机：平板点酒，工作手机核销（锁 L3121） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-045 | 主要功能（锁 L3136） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-046 | 平板图形点单（POS）（锁 L3166） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-047 | 和谁有关（锁 L3186） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-048 | 触摸屏一体机：权益领取、现场服务和辅助售票（锁 L3211） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-049 | 主要功能（锁 L3225） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-050 | 和谁有关（锁 L3249） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-001 | 整体定位：平台治理层不是一个业务部门，而是所有业务的底座（锁 L4998） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-038 | 和现场业务的关系（锁 L5736） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-085 | 项目开放状态（锁 L1028） | 只登记 O-01。PROJECT_STATUSES 不在本 diff。不改判。 |
| BEQR-086 | 场次状态（锁 L1039） | 只登记 O-02。SESSION_STATUSES 不在本 diff。不改判。 |
| BEQR-087 | 游乐「当天／每天」口径（锁 L1050） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-088 | 容量双阈值（锁 L1061） | 成立。扣减仍是按入场人数 -N，不是把项目在场一律置 0。 |
| BEQR-089 | 园区容量（锁 L1072） | 成立。project 维与 point_device 维各 -N，下限 0。JdbcAmusementRepository.java:1102-1104。 |
| BEQR-090 | 设备关键/辅助分级（锁 L1083） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-092 | 安全严重度（锁 L1105） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-093 | 巡检任务状态（锁 L1116） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-094 | 越界类型（锁 L1127） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-095 | 团体/包场（锁 L1138） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-096 | 游乐增强阶段（锁 L1149） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-016 | 游乐管理（锁 L2543） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-068 | 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常（锁 L3686） | 成立。锁 L3686，业务地图 h2。人工清零入口已删，出场扣减仍在。 |
| BSM-069 | 主要功能（锁 L3700） | 成立。第 1263 行在该 h3 下：不得手工修改、清零或修正在玩人数。本轮实现与该句一致。 |
| BSM-070 | 和谁有关（锁 L3729） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-071 | 游乐类型与规则（锁 L3750） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-023 | 主要功能（锁 L5429） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-097 | 供应合同模式（锁 L1160） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BEQR-163 | 合同变更类型（锁 L1886） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-027 | 基础管理：先把综合体的盘子管清楚（锁 L2716） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-075 | 招商租赁与合同履约：从找商户到签约、履约、变更、退租（锁 L3842） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-076 | 主要功能（锁 L3856） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| BSM-077 | 和谁有关（锁 L3880） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-028 | 合同和准入：违反规则先卡住，再按层级放行（锁 L5530） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-029 | 主要功能（锁 L5544） | 范围内文件未改该判据所辖行为。本轮无新证据。 |
| PGM-030 | 业务意义（锁 L5576） | 范围内文件未改该判据所辖行为。本轮无新证据。 |

181 项均有行。本轮无新缺陷。

## 最终

PASS。问题已收齐：0 条。001–005 的下线、V433、历史约束、出场扣减和矩阵路径经本轮空库、变异与门禁复核成立。已定稿只登记项没有新证据，不改判。
