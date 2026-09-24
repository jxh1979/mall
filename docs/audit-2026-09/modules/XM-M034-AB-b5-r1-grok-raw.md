CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M034-AB B5-r1 修复后整模块三线全量重审（Grok 席乙）

版本：V0.01
席位：Grok 独立只读审计席乙（grok-4.7 / xhigh）
步骤：B5-r1。B3 累计 2。本报告不改业务代码、HTML、锁、findings、ledger。
锁：本席对 `docs/audit-2026-09/plan/criteria-lock.md` 执行 `sha256sum`，第一行即为该输出。与 sidecar、ledger 首行相同，是实算结果重合，不是抄基准。
正本：`/home/james/complex-management` 范围文件的工作区字节 = `origin/main` `daf6adfc` + `/home/james/audit-tmp/xm034_b3cum.diff`（126694 字节，SHA256 `21e4a442ddf404285e2b3a289a36acb6daae6b454549256afce3eccc65bb8797`，30 个路径）。一次性 worktree 套用该 diff 后，30 个路径与主工作区逐字节相同（BAD 0）。另 11 个开工段内未改文件的工作区 `hash-object` 与 `origin/main` blob 相同。
未读 `modules/XM-M034-AB-b5-r1-self-raw.md`。旧 raw 只用来核对已登记观察的边界，不复用旧票结论。
冻结 diff 实际删除 3 个 Java 文件，不是 4 个：`AmusementManualOverrideRecord.java`、`AmusementManualOverrideRequest.java`、`VerificationManualOverrideRequest.java`。三份在主工作区与 worktree 都不存在。

## 结论

FAIL。

001 至 004 的行为修复成立：票务人工放行写入口与游乐人工改判（allow、release、deny、pause、resume）的生产调用链已不在；V433 只把 `amusement.verification.override` 改为 `retired` 并清四处授予；历史表、CHECK、V099 策略行、`releaseEntryConcurrentCapacity` 与出场扣减仍在。新回归在空库上执行通过，且把该码改回 `active` 后断言失败。

新缺陷 1 条（GR-01）：需求实现矩阵第 49–52 行「相关代码文件」仍写已删除的 `AmusementManualOverrideRecord.java`。GEN-016。有工程缺陷，建议修复队列为「是」。不因此重开 001–004，也不改判 B2 只登记项。

## 技能（按本环境实核，不冒充）

已读并按 V0.17 §6 收窄使用：`using-superpowers`、`grilling`（自问，不派人、不等操作员）、`grill-me`（存在，追问并入 grilling）、`codebase-design`（看删除后接口是否变浅）、`ponytail` / `ponytail-review`（只找可删残留，不改代码）、`verification-before-completion`（PASS/FAIL 前有本轮命令输出）、`systematic-debugging` / `diagnosing-bugs`（第一次 Maven 失败追到 JRE 无 javac，不改生产代码）、`graft grep clearProjectConcurrentCapacity`（1940 个索引文件 0 命中）。

存在但本步不调用其本体：`test-driven-development` / `tdd`（B5 不修、不落测试）、`requesting-code-review`（B4 才派审）、`receiving-code-review`（不实施）、`resolving-merge-conflicts`（无冲突）、`writing-for-agents`（收口文件不由本席写）、`git-guardrails-claude-code`（无合并）。

缺口：`batch-grill-me`、`ubiquitous-language` 在本机技能目录未找到。`i-have-adhd` 存在，但落盘第一行必须是锁哈希，与该技能「首行即动作」冲突，以本次派发和 V0.17 为准。

未用 MCP、子代理、第三席。未 fetch / commit / push / merge / deploy。未对 `complex`、`complex_test` 执行写语句。

## 范围与读法

开工段 33 个文件的现状，加定稿新增的契约包、需求实现矩阵、e2e `mock-api.ts`、V433，以及本 diff 带上的门禁生成物：隔离规格、两份 OpenAPI、两份附录、权限矩阵、`security-baseline-latest.json`、`findings-register-v1.md`、`formal-security-baseline-v1.md`。

Java、SQL、测试、短文档的 diff 全文已读。OpenAPI 两份用 operationId 计数与字符串检索核对，差异只删 `/api/v1/amusement/manual-overrides` 的 GET/POST、`AmusementManualOverrideRequest` 组件。未改字节与 `origin/main` 相同，不把「相同」写成「重新发明了每条业务规则」。调用方用仓库检索核过，不采信自审 raw 路径。

## 第一性三问

1. 为什么还必须存在。
   票务 `POST /api/v1/ticketing/verifications/manual-override` 与游乐 `GET/POST /api/v1/amusement/manual-overrides` 删掉之后，枚举速查第 43 行「系统不提供人工放行……任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过」和业务地图第 1263 行「任何岗位（含主管与最高权限）都不得手工修改、清零或修正在玩人数」仍然做得到，而且只有删掉写入口才做得到。这两段已不该存在。
   仍须存在的是：`amusement_manual_overrides` 表与历史行、V014/V234 的类型 CHECK（含 `release`）、V067 的 `manual_overridden` CHECK、V099 种下的 `amusement_manual_override_over_scope` 策略行、`releaseEntryConcurrentCapacity` 与 `exitVerification` 的按入场扣减。删掉它们，历史行会违反约束，或业务地图「出场自动扣减、同一人重复经过不重复扣减」做不到。004 已写明保留。
2. 声称的行为有没有被执行验证。
   有。见下方「空库全量」与「变异」。文档侧 `verify:predeploy` 退出码 0。
3. 哪条前提曾未验证就当真。
   V170 会把该码 `status` 写回 `active`（`V170__access_grant_requests.sql:180-188`）。前提是 V433 在它之后执行。空库 Flyway 停在 433，且该码最终为 `retired`，这个顺序已被执行验证，不再是假设。
   「测试通过即行为正确」不采用。全量 0 红之外，另做了把 V433 改成 `active` 的一次失败。

## 行为核对（重点）

### 写入口已不在

`backend/src/main/java` 检索 `manualOverride`、`ManualOverride`、`manual-overrides`、`clearProjectConcurrentCapacity`、`createManualOverride`、`saveManualOverride`、`listManualOverrides`、`mapManualOverride`、`OVERRIDE_TYPES`、`verification.manual_override`、`amusement.verification.override`：无匹配。

`apps/` 与 `tests/` 检索 `manual-overrides`、`manualOverride`、`amusement.verification.override`：无匹配。e2e `mock-api.ts` 已去掉该路径一行。

graft 对 `clearProjectConcurrentCapacity`：0 命中。

控制器差异只删游乐两端点（原 `AmusementController` 约 39–52 行）和票务 `POST /ticketing/verifications/manual-override`（原 `TicketingController` 约 495–507 行）及其 import。`TicketingService.manualOverrideVerification`、`TicketVerificationService.manualOverride`（原写 `status=manual_overridden`）已删。`AmusementService` 的 `OVERRIDE_TYPES`、list/create、`createManualOverrideInternal`（allow→approved、release 调清零）及返回值 id 分支已删。仓储四个方法、`manualOverrideParameters`、行映射、两个 record 已删。`clearProjectConcurrentCapacity` 的接口、Jdbc 实现、测试内存仓实现已删。

生产 Java 里不再出现 `amusement_manual_overrides` 的 INSERT/SELECT。`JdbcAdmissionFactRepository.java:135` 仍是注释「amusement_manual_override（写接口首参非 String……）」，blob 与开工段 `fdc205c9c940` 相同。B2 定为只登记，本轮无新证据，不改判。

`JdbcSettlementTransactionRepository.java:137` 的 SQL 注释排除历史 `manual_overridden`，不写入该状态。文件不在本模块差异里。V067 CHECK 仍允许历史值，排除它与「不得再写」不矛盾，不单独立项。

### V433 只停用该码并清四处

`V433__retire_amusement_verification_override.sql` 全文 16 行：`permissions.status='retired'`；删除 `role_permissions`、`permission_template_items`、`user_permission_overrides` 中该码；`temporary_permission_grants` 里该码且 `status='active'` 的行改为 `revoked`（`revoked_by='V433'`）。不 DROP 表，不改 CHECK，不 DELETE 历史行。结构与 V429 对 `verification.manual_override` 的四处清理相同。

V014:185 种入 `active` 并在 :213 授给平台管理员；V170:180-188 再把该码写回 `active`。二者版本都小于 433。当前迁移目录最大号就是 V433。

空库 `xm034b5g_test`（只读查询，不是 `complex` / `complex_test`）在全量迁移之后：

- `flyway_schema_history` 最新成功版本 `433`
- `amusement.verification.override` 的 `status` = `retired`
- `verification.manual_override` 的 `status` = `retired`
- 两码在四处授予上的计数都是 0
- `to_regclass('public.amusement_manual_overrides')` 有表
- `chk_amusement_manual_overrides_type` 仍为 `allow, deny, pause, resume, release`
- `chk_ticket_verifications_status` 仍为 `success, failed, rejected, reversed, manual_overridden`
- `supervisor_review_policies` 中 `amusement_manual_override_over_scope` / `amusement.manual_override` / `enabled=t` 仍在一行（V099:51，表名是 `supervisor_review_policies`）。无 Java 调用方，按 004 保留，不立项。

### 出场扣减保留

`AmusementRepository.releaseEntryConcurrentCapacity` 仍在。`JdbcAmusementRepository.java:1084-1106`：先条件更新入场 `released_at`（已释放则返回 false，不重复减），再按该入场人数对 `point_device` 与 `project` 两维做 `occupied_count - N`（下限 0），然后写 `direction=exit`。这不是把项目下 `occupied_count>0` 的行一律置 0。

`AmusementService.exitVerification`（约 1371–1414 行）仍按未释放入场定位；没有未释放入场则返回既有 exit 或 `ENTITLEMENT_NO_ACTIVE_ENTRY`。控制器 `exitVerification` 仍在。B1 已把它与五种改判里的 `release` 分开。本轮 diff 没有改这个方法的语句。不改判。

`JdbcAmusementRepository.java:1004` 注释仍写「出场/主管释放」。方法 `decrementConcurrentSnapshot` 是按维度键减 N，只被 `releaseEntryConcurrentCapacity` 调用。注释是旧词，不是清零入口。不把注释升级成新需求。

### 契约、附录、基线、隔离规格

- `openapi-p0.yaml` 与 `openapi-p0.json` 的 `operationId` 都是 730。两份都不再含 `manual-overrides` / `manual-override`。json SHA256 `d6a2085004caaa10b33f5057c7fa973da19b1064a30579562a3f7df64bbb9e2a`，与 `findings-register-v1.md`、`formal-security-baseline-v1.md` 所写相同。
- 附录计数：operation 732→730，权限行 329→328，事件 1464→1460，写接口 441→440，任务追溯 732→730。与删掉 1 个 POST、1 个 GET、4 个事件名、1 个权限码行一致。`staff-token` 677→675；`staff_work_phone` 81→80；`tablet` 103→102；`web` 694→693。
- `security-baseline-latest.json` 相对 origin 只改 `openapi_operation_count` 732→730。`notes` 里「同步 732 项接口」是 U-FIX 历史句，B4-r2 已否决改写 notes / `verified_at` / version。计数字段已是 730。无新证据，不改判。
- 隔离规格自动段只把最新迁移 V432 改为 V433，表数仍为 172。`amusement_manual_overrides` 行仍在。
- 权限矩阵四处措辞改为写入口或改判接口已下线，并去掉已不再发出的 `amusement.verification_overridden`。中文权限名迁移 V251/V254 字节未改。B2 登记「权限中文名不改」，不改判。
- 契约包 `docs/p0-api-contract-pack-v0.1.md:334` 仍有一行 `POST /api/v1/amusement/manual-overrides`，单元格写明「接口已下线」和「停用」。这是 004「同改」的那一处，不是活契约。OpenAPI 与附录已不含该 operation。`verify:predeploy` 报告 OpenAPI 与两份附录 730 项对齐。不把纪念行判成残留调用。

### 测试

删掉的断言都守着已废止能力：控制器把 allow/approved 当成功、服务测试把人工放行写成 `manual_overridden`、Jdbc 的 `saveManualOverride`、出场集成测试里 `supervisorReleaseClearsConcurrentCapacityWithAudit` 以及场次用例末段的主管清零。留下的出场用例仍断言并发在场回到 0、场次 `used_capacity` 不变；注释写明注掉 `decrementConcurrentSnapshot` 会红。

`AmusementJdbcIntegrationTests.migrationCreatesAmusementTablesAndSeedsPermissions` 仍 `contains("amusement.verification.override")`，但 `permissionCodes()` 取的是字典全部行，retired 行仍在，所以这句不证明「仍启用」。平台管理员授予列表已去掉该码。专测 `amusementOverridePermissionIsRetiredAndGrantedToNobody`（475–488 行）断言 `retired`、四处之和为 0、表仍在。

全量该测执行过且通过（非 skipped）。变异：在一次性 worktree 里只把 V433 那句改成 `status='active'`，另建空库 `xm034b5g_red`，只跑这一测。结果 `MUT_EXIT=1`，`AmusementJdbcIntegrationTests.java:480`：`expected: "retired" but was: "active"`。测完把该句改回 `retired`。断言能失败。

### 空库全量与门禁

第一次 `mvn` 把 `JAVA_HOME` 指到 `/usr/lib/jvm/java-21-openjdk-amd64`。该路径没有 `javac`，编译报 `release version 21 not supported`。改用 Temurin `21.0.12.1`（`/usr/lib/jvm/temurin-21-jdk-amd64`，有 javac）后重跑。

`origin/main` 一次性 worktree 套累计 diff；库名 `xm034b5g_test`；只在该 worktree 把测试里的 `5432/complex_test` 改指该库，并设 `SETTLE_TEST_DB_URL` 与改写后的 `COMPLEX_DATABASE_URL`。`mvn -o -q test`：`MVN_EXIT=0`，`SUITES=263 TESTS=3529 FAILURES=0 ERRORS=0 SKIPPED=17`。

17 条跳过与 XM-M031-B 已登记的名单相同：`M006JdbcRegressionTests` 13 条、`M006StoredValueJdbcTests` 4 条（库名必须恰为 `complex_test` 的既有 `assumeTrue`）。本模块无新增跳过。不改判。

源码恢复为 `complex_test` 后，同一 worktree（主工作区 `node_modules` 软链）`npm run verify:predeploy`：`VERIFY_EXIT=0`。其中 OpenAPI 契约通过、验收矩阵与附录均「730 operations checked」、安全基线通过、`TEST_DB_ISOLATION_PASS`（测试源用 `complex_test`，不引用生产库 `complex`）。

事后：测试 URL 已还原；软链已卸；`git checkout` + `git clean` 后 porcelain 0；`git worktree remove` 未加 `--force`；`DROP DATABASE` 仅 `xm034b5g_test` 与 `xm034b5g_red`。`complex` 与 `complex_test` 仍在。主工作区源码无 `xm034b5g`。主工作区 V433 第 7 行仍是 `retired`。

## GR-01 需求实现矩阵仍指向已删除的类

- 文件行：`docs/requirements-code-implementation-matrix-v0.1.csv` 第 49–52 行（REQ-45、REQ-46、REQ-47、REQ-48），列「相关代码文件」。
- 原句：该列仍含 `backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRecord.java`。同列其余抽到的路径存在。API 列里的 `manual-overrides` 两个 operation 已去掉。数据表列里的 `amusement_manual_overrides` 仍在，这与保留历史表一致，不在本条里。
- 正常触发：按矩阵第 49 行去打开这个类文件。工作区没有该路径（本 diff 已删除该文件）。第一次打开就落空，不用额外巧合。
- 四分类顺序：不是冲突（三份 HTML 没有要求矩阵必须留下这个类名，也没有与「文件不存在」相抵的明文）。不是细化（指不出三份 HTML 里某一格是这一列的上位句）。是技术性：索引路径，没有新的业务实体、枚举、状态或流程。技术性仍要过正确性；路径失真是工程缺陷。不到添加。
- 已有判据：GEN-016「文档中的路径……不得把计划或旧证据写成当前事实」。GEN-002 作为旁证：文档把该文件写成当前相关代码，树里已经没有它。
- 证据类型：4。对第 49–52 行列出的 `backend/`、`apps/`、`tests/`、`docs/` 路径做存在性检查，缺失的只有这一个类，四行各一次。diff 显示这四行在本轮被整行改写，API 引用已删，类名被留在改写后的行里。
- 最小处置：从这四行的「相关代码文件」删掉该路径。不要删 `amusement_manual_overrides` 表名，不要改 OpenAPI，不要改 V433。

001–004 的行为修复不因此失效。本条是 004 要改的矩阵上多留下的一个假路径，正式 ID 由 B2 定。

## 不改判（无新证据）

- O-01 / O-02：`PROJECT_STATUSES`、`SESSION_STATUSES` 本 diff 未改。BEQR-085、BEQR-086 维持只登记。
- `JdbcAdmissionFactRepository.java:135` 注释、设计文档里「待代码轮下线」的旧句（不在本 diff 的 30 个路径里）、V251/V254 权限中文名：维持 B2 只登记。
- `security-baseline-latest.json` 的 notes 仍出现「732」：计数字段是 730；notes 按 B4-r2 保持 origin 原文。
- 契约包第 334 行纪念行：写明已下线，属 004 的「同改」。
- 17 条 `assumeTrue` 跳过：名单与已登记的 M006 17 条相同。
- `exitVerification` / `releaseEntryConcurrentCapacity`：按入场扣减，不是人工清零。

## 对抗（grilling 自问，事实已读）

1. 删了端点之后，权限码若仍是 active，最高权限是否还能绕过？不能。空库该码为 `retired`，四处授予为 0。专测锁住这一点，且改回 active 会红。
2. V170 会不会在 V433 之后把码重新启用？不会。V170 号更小，目录里没有大于 433 的迁移。
3. 历史 CHECK 里的 allow/release 会不会变成新的写入口？不会。没有 Java 再 INSERT 该表。CHECK 只是旧行还能通过约束。
4. 出场扣减会不会被一起删掉？没有。`releaseEntryConcurrentCapacity` 与出场测试的「场次席位不变」断言还在。
5. 测试是不是只删了会失败的活断言？抽过的删除都绑在已删方法上。留下的出场断言仍能在减错维度时失败（类注释写了证伪点）。新专测已实证能失败。
6. 两份契约会不会一份删了、一份还在？不会。两份 operationId 都是 730，字符串都没有 `manual-override`。门禁对 json 与两份附录做了 730 项对齐。
7. 矩阵假路径会不会被门禁抓住？本轮 `verify:predeploy` 为 0，说明门禁不检查这一列的文件是否存在。所以它是文档缺陷，不是「门禁已绿所以不存在」。

## 顺从式 181 项

锁内 M039、M114、M132、M133、M134、M135、M230、M232、M233、M237、M245、M246、M248、M389、M390、M391、M392 并集 181（BEQR 72、BSM 45、GEN 18、PGM 46），无重复。下列「见上文」的条目有本轮行为证据。其余条目：本轮差异已全读，差异没有改该锚点的行为；对应字节与 `origin/main` `daf6adfc` 相同，或该锚点的主语不在这批文件的实现里。无新冲突、无新缺陷。这不是把旧票结论抄过来。

| ID | 标题 | 本轮 |
|---|---|---|
| BEQR-012 | 核销记录状态 | 见上文。写入口已删。HTML 第 43 行末句成立。历史 `manual_overridden` 只留在 V067 CHECK。 |
| BSM-068 | 游乐管理：管项目开放、项目容量和安全异常 | 见上文。人工清零方法已删。 |
| BSM-069 | 主要功能 | 见上文。第 1263 行人工调整已无入口；出场自动扣减仍在。 |
| BEQR-088 | 容量双阈值 | 见上文。本 diff 未改容量判定语句。 |
| BEQR-089 | 园区容量 | 见上文。同上。 |
| BEQR-159 | 权限状态 | 见上文。`retired` 沿用 V429 对「已停用」的既有写法，不是新枚举。空库该码为 retired。 |
| PGM-040 | 每一个单一功能都可以单独授权 | 见上文。废止码被单独停用并清授予，没有并进 `amusement.verification.execute`。 |
| GEN-001 | 必要性与最小性 | 见上文。废止入口已删。GR-01 是删入口后留下的无用路径。 |
| GEN-002 | 可执行正确性 | 见上文。全量 3529 测 0 失败。GR-01 使矩阵与树不一致。 |
| GEN-003 | 测试有效性 | 见上文。专测已执行；变异后 :480 红。 |
| GEN-005 | 授权与最小权限 | 见上文。两码 retired，四处授予 0。 |
| GEN-006 | 数据完整性 | 见上文。表与两道 CHECK 保留；不再有清零写入。 |
| GEN-008 | 接口与模式一致性 | 见上文。两份 OpenAPI 与两份附录 730 对齐。契约包纪念行标明已下线。 |
| GEN-014 | 数据库迁移正确性 | 见上文。V433 在空库执行到版本 433，只停用并清四处。 |
| GEN-016 | 文档可验证性 | 缺陷。GR-01。其余门禁文档路径经 `verify:predeploy` 0。 |
| GEN-017 | 业务来源分类 | 本条按冲突→细化→技术性→添加走完，见 GR-01。 |
| GEN-018 | 第一性原理证据 | 本报告结论落在 HTML 原文、空库查询、Maven/npm 输出，不落在旧票。 |
| BEQR-063 | 高危动作（需审批/双证据） | 未改、无新证据 |
| BEQR-074 | 隐私桌台权限 | 未改、无新证据 |
| BEQR-143 | 四类主体 | 未改、无新证据 |
| BEQR-144 | 经营主体关系 | 未改、无新证据 |
| BEQR-145 | 城市项目公司 | 未改、无新证据 |
| BEQR-146 | 运营公司职责 | 未改、无新证据 |
| BEQR-147 | 主要收入归属 | 未改、无新证据 |
| BEQR-148 | 合作模式 | 未改、无新证据 |
| BEQR-149 | 职级 | 未改、无新证据 |
| BEQR-150 | 终端十一端 | 未改、无新证据 |
| BEQR-151 | 最终权限计算 | 未改、无新证据 |
| BEQR-152 | 项目与敏感数据权限 | 未改、无新证据 |
| BEQR-153 | 终端开关优先级 | 未改、无新证据 |
| BEQR-154 | 综合体项目状态 | 未改、无新证据 |
| BEQR-155 | 商户/运营主体状态 | 未改、无新证据 |
| BEQR-156 | 合同状态 | 未改、无新证据 |
| BEQR-157 | 员工状态 | 未改、无新证据 |
| BEQR-158 | 设备状态 | 未改、无新证据 |
| BEQR-166 | 合同审批链 | 未改、无新证据 |
| BEQR-182 | 考勤日结果 | 未改、无新证据 |
| BEQR-183 | 打卡方式 | 未改、无新证据 |
| BEQR-184 | 请假类型 | 未改、无新证据 |
| BEQR-185 | 请假单状态 | 未改、无新证据 |
| BEQR-186 | 薪资单构成 | 未改、无新证据 |
| BEQR-187 | 员工提成发放 | 未改、无新证据 |
| BEQR-189 | 管理费方式 | 未改、无新证据 |
| BSM-004 | 配置数据可以存档、也可以删除（删除要审批） | 未改、无新证据 |
| BSM-005 | 后台的"新建/录入"改成分步引导 | 未改、无新证据 |
| BSM-006 | 验证用的二维码一律动态刷新（间隔可在后台设定） | 未改、无新证据 |
| BSM-007 | 平台管理员拥有全部日常功能权限 | 未改、无新证据 |
| BSM-009 | 基础管理 | 未改、无新证据 |
| BSM-028 | 主要功能 | 未改、无新证据 |
| BSM-029 | 和谁有关 | 未改、无新证据 |
| BSM-072 | 商户管理：商户在授权范围内完整使用自己的系统 | 未改、无新证据 |
| BSM-073 | 主要功能 | 未改、无新证据 |
| BSM-074 | 和谁有关 | 未改、无新证据 |
| BSM-084 | 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 未改、无新证据 |
| BSM-085 | 日常工作 | 未改、无新证据 |
| BSM-086 | 排班、考勤与请假 | 未改、无新证据 |
| BSM-087 | 我的薪资 | 未改、无新证据 |
| BSM-088 | 员工提成、活动执行与人员服务边界 | 未改、无新证据 |
| BSM-114 | 各类入口：不同人只看自己该看的、只做自己该做的 | 未改、无新证据 |
| BSM-115 | 相互关系：为什么这些子系统必须连在一起 | 未改、无新证据 |
| BSM-120 | 已确认规则和实施前输入项 | 未改、无新证据 |
| GEN-004 | 信任边界校验 | 未改、无新证据 |
| GEN-007 | 并发与幂等 | 未改、无新证据。出场重复扣减的幂等分支未改。 |
| GEN-009 | 错误处理与恢复 | 未改、无新证据 |
| GEN-010 | 安全与敏感信息 | 未改、无新证据 |
| GEN-011 | 可观测与审计证据 | 未改、无新证据。改判成功事件已随方法删除；附录已去掉对应四行。 |
| GEN-012 | 资源与性能边界 | 未改、无新证据 |
| GEN-013 | 构建配置与依赖 | 未改、无新证据。本轮构建用 Temurin 21。 |
| GEN-015 | 界面可用性与可访问性 | 无本面。本范围无界面文件。 |
| PGM-002 | 治理顺序：一件业务能不能做，要先经过这些判断 | 未改、无新证据 |
| PGM-003 | 模块入口：平台治理层包含哪些管理模块 | 未改、无新证据 |
| PGM-004 | 综合体和集团 | 未改、无新证据 |
| PGM-005 | 空间和点位 | 未改、无新证据 |
| PGM-006 | 商户和主体 | 未改、无新证据 |
| PGM-007 | 合同和准入 | 未改、无新证据 |
| PGM-008 | 员工和手机 | 未改、无新证据 |
| PGM-009 | 权限和职级 | 未改、无新证据 |
| PGM-010 | 终端开关 | 未改、无新证据 |
| PGM-011 | 设备管理 | 未改、无新证据 |
| PGM-012 | 商户侧 | 未改、无新证据 |
| PGM-013 | 开放接口 | 未改、无新证据 |
| PGM-014 | 留痕复核 | 未改、无新证据 |
| PGM-015 | 楼宇运营 | 未改、无新证据 |
| PGM-016 | 应急与隐私 | 未改、无新证据 |
| PGM-017 | 综合体和集团：当前一个项目，未来能看集团 | 未改、无新证据 |
| PGM-018 | 物理项目与运营公司：多对多协作，不是固定隶属层级 | 未改、无新证据 |
| PGM-019 | 各主体具体负责什么 | 未改、无新证据 |
| PGM-020 | 主要功能 | 未改、无新证据 |
| PGM-021 | 业务意义 | 未改、无新证据 |
| PGM-022 | 空间和点位：所有业务都要知道“发生在哪” | 未改、无新证据 |
| PGM-024 | 影响业务 | 未改、无新证据 |
| PGM-026 | 主要功能 | 未改、无新证据 |
| PGM-027 | 业务边界 | 未改、无新证据 |
| PGM-031 | 外部经销商主体、合同价格与资金治理 | 未改、无新证据 |
| PGM-032 | 员工和手机：人、私人手机、工作手机要能对应起来 | 未改、无新证据 |
| PGM-033 | 主要功能 | 未改、无新证据 |
| PGM-034 | 工作手机和企业微信 | 未改、无新证据 |
| PGM-035 | 提成发放和人员服务治理 | 未改、无新证据 |
| PGM-036 | 设备管理：谁领了什么设备，设备能不能用 | 未改、无新证据 |
| PGM-041 | 权限规则 | 未改、无新证据 |
| PGM-042 | 职级规则 | 未改、无新证据 |
| PGM-043 | 外包人员与审计人员（2026-06-29 新增） | 未改、无新证据 |
| PGM-044 | 桌台、挂单与经销业务权限 | 未改、无新证据 |
| PGM-048 | 商户侧：在授权业务范围内全开放 | 未改、无新证据 |
| PGM-059 | 客人与员工的个人信息保护 | 未改、无新证据 |
| PGM-060 | 会员身份、营销与生物信息治理 | 未改、无新证据 |
| PGM-065 | 连接关系：平台治理层怎么连接所有业务 | 未改、无新证据 |
| PGM-066 | 已拍板规则和待排期问题 | 未改、无新证据 |
| BEQR-001 | 票种 | 未改、无新证据 |
| BEQR-002 | 票务策略状态 | 未改、无新证据 |
| BEQR-003 | 定价、售票与收款主体 | 未改、无新证据 |
| BEQR-004 | 年龄限制 | 未改、无新证据 |
| BEQR-005 | 通票赠酒下限 | 未改、无新证据 |
| BEQR-006 | 通票类型 | 未改、无新证据 |
| BEQR-007 | 权益类型 | 未改、无新证据 |
| BEQR-008 | 餐饮/零售券两种用法 | 未改、无新证据 |
| BEQR-009 | 资格判定 | 未改、无新证据 |
| BEQR-010 | 订单状态 | 未改、无新证据 |
| BEQR-011 | 票/权益状态 | 未改、无新证据 |
| BEQR-013 | 手环入场 | 未改、无新证据 |
| BEQR-014 | 退款单状态 | 未改、无新证据 |
| BEQR-015 | 有效期类型 | 未改、无新证据 |
| BEQR-016 | 售票与退款原则 | 未改、无新证据 |
| BEQR-017 | 到期未核销款 | 未改、无新证据 |
| BEQR-018 | 域归属对照 | 未改、无新证据 |
| BEQR-019 | 跨域通票分配 | 未改、无新证据 |
| BEQR-020 | 酒吧通票可分配池 | 未改、无新证据 |
| BEQR-021 | 收费与免费权益结算 | 未改、无新证据 |
| BEQR-022 | 项目内部结算价 | 未改、无新证据 |
| BEQR-023 | 核销顺序 | 未改、无新证据 |
| BEQR-024 | 游乐核销归属 | 未改、无新证据 |
| BEQR-025 | 门票综合服务费 | 未改、无新证据 |
| BEQR-026 | 动态价与会员价 | 未改、无新证据 |
| BEQR-027 | 人脸验龄数据生命周期 | 未改、无新证据 |
| BEQR-032 | 等级权益对照 | 未改、无新证据 |
| BEQR-034 | 票务人次计算 | 未改、无新证据 |
| BEQR-040 | 发券与核销结算 | 未改、无新证据 |
| BEQR-091 | 全园故障与补偿通票 | 未改、无新证据 |
| BEQR-124 | 综合通票定价 | 未改、无新证据 |
| BSM-001 | 整体说明：这套系统到底管什么 | 未改、无新证据 |
| BSM-002 | 一笔业务怎么走 | 未改、无新证据 |
| BSM-003 | 系统通用能力：数据可改可删、操作有引导、权限分岗 | 未改、无新证据 |
| BSM-010 | 票务权益 | 未改、无新证据 |
| BSM-030 | 票务权益：管卖什么票、给什么权益、怎么核销 | 未改、无新证据 |
| BSM-031 | 主要功能 | 未改、无新证据 |
| BSM-032 | 和谁有关 | 未改、无新证据 |
| BSM-033 | 票种、通票与权益类型（系统支持哪些种类） | 未改、无新证据 |
| BSM-034 | 核销、退款与有效期规则 | 未改、无新证据 |
| BSM-035 | 会员与积分规则 | 未改、无新证据 |
| BSM-036 | 人脸验龄数据使用与删除 | 未改、无新证据 |
| BSM-044 | 酒吧平板/工作手机：平板点酒，工作手机核销 | 未改、无新证据 |
| BSM-045 | 主要功能 | 未改、无新证据 |
| BSM-046 | 平板图形点单（POS） | 未改、无新证据 |
| BSM-047 | 和谁有关 | 未改、无新证据 |
| BSM-048 | 触摸屏一体机：权益领取、现场服务和辅助售票 | 未改、无新证据 |
| BSM-049 | 主要功能 | 未改、无新证据 |
| BSM-050 | 和谁有关 | 未改、无新证据 |
| PGM-001 | 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 未改、无新证据 |
| PGM-038 | 和现场业务的关系 | 未改、无新证据 |
| BEQR-085 | 项目开放状态 | 只登记 O-01。常量未改，不改判。 |
| BEQR-086 | 场次状态 | 只登记 O-02。常量未改，不改判。 |
| BEQR-087 | 游乐「当天／每天」口径 | 未改、无新证据 |
| BEQR-090 | 设备关键/辅助分级 | 未改、无新证据 |
| BEQR-092 | 安全严重度 | 未改、无新证据 |
| BEQR-093 | 巡检任务状态 | 未改、无新证据 |
| BEQR-094 | 越界类型 | 未改、无新证据 |
| BEQR-095 | 团体/包场 | 未改、无新证据 |
| BEQR-096 | 游乐增强阶段 | 未改、无新证据 |
| BSM-016 | 游乐管理 | 未改、无新证据 |
| BSM-070 | 和谁有关 | 未改、无新证据 |
| BSM-071 | 游乐类型与规则 | 未改、无新证据 |
| PGM-023 | 主要功能 | 未改、无新证据 |
| BEQR-097 | 供应合同模式 | 未改、无新证据 |
| BEQR-163 | 合同变更类型 | 未改、无新证据 |
| BSM-027 | 基础管理：先把综合体的盘子管清楚 | 未改、无新证据 |
| BSM-075 | 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | 未改、无新证据 |
| BSM-076 | 主要功能 | 未改、无新证据 |
| BSM-077 | 和谁有关 | 未改、无新证据 |
| PGM-028 | 合同和准入：违反规则先卡住，再按层级放行 | 未改、无新证据 |
| PGM-029 | 主要功能 | 未改、无新证据 |
| PGM-030 | 业务意义 | 未改、无新证据 |

181 项均有行。其中 GEN-016 为缺陷，其余本轮无新缺陷。

## 最终

FAIL。问题已收齐：GR-01 一条。001–004 的下线、V433、历史约束与出场扣减经本轮空库与门禁复核成立，不把它们再判成未修。
