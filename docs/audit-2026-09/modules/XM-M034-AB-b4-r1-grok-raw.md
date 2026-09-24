CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# XM-M034-AB B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 只读独立审计席乙（grok-4.7/xhigh）
计划：160-audit-prompt.md V0.17 的 B4 与 §5 闸 3
本文件路径：docs/audit-2026-09/modules/XM-M034-AB-b4-r1-grok-raw.md
本文件原先不存在，本次新建完整报告。本次完整报告自上一行独占锁行起至文末。正文不再另起同名独占锁行。

修复内容结论：PASS。
范围归因结论：FAIL。
总结论：FAIL。

以上只是席乙本轮 B4-r1 票。不是双 PASS，不是 B5，也不是模块 PASS。归因与内容分开投票，任一张 FAIL 则本席总结论 FAIL。本席不修代码、不改 findings、不改 ledger、不改锁。

未读：modules/XM-M034-AB-b4-r1-self-raw.md 正文。仅 `ls -l`：23681 字节，mtime 2026-09-24 01:08 -0700。未对其 cat、head、grep。席甲归因表不作为本席证据。旧 raw 可读，本席用了自己的 B2-r1（modules/XM-M034-AB-b1-grok-raw.md V0.02 段）核对当时写死的边界。

当地时间：2026-09-24T01:21:20-0700；UTC：2026-09-24T08:21:20Z。

---

## 1. 输入与锁

正本：/home/james/complex-management，分支 main 主工作区。审计对象是两个 tree，不是 HEAD，也不是事后工作区里多出来的审计文件。

| 项 | 值 |
|---|---|
| before tree | d6e19dfb67d8583a3c1257c8f3f0f820f2743dd4 |
| after tree | 46ea3be99b8fbe5ffd382ebbfaa0da0238a1ccb6 |
| 两者 `git cat-file -t` | tree / tree |
| 冻结 diff | /home/james/audit-tmp/xm034_b3r1.diff |
| 冻结 diff 字节 / SHA256 | 127358 / 2c163c613e0e3924d79c7eafc2a6f5a3ac3767df74e582efc7c66b44db4dacc3 |
| 本席重算 `git diff --binary <before> <after>` 的 SHA256 | 与冻结 diff 相同 |
| `git diff --stat` | 30 files changed, 71 insertions(+), 831 deletions(-) |
| `git diff --check` | exit 0 |
| `git diff --raw` 模式 | 存活文件与新文件均为 100644；无 100755；无仅改模式的路径 |
| 路径数 | before 4036，after 4034 |
| 路径增减 | 删除 3 个文件，新增 V433。净减 2。派发语「4 个删除文件」与 tree 不符；定稿要求删除的就是这两个记录类型加票务 DTO，共 3 个，tree 与定稿一致 |
| origin/main | daf6adfc0bada1e1d972aa8e4499eb117925e244 |
| HEAD（对照，非本 diff 基线） | e456ba96c84185c8d39f18f548011daac48a6466 |
| 三份 HTML before=after | BEQR 86a717930f9303d4f131ee2864478baa14450e6d；BSM 7002bb80afb0ef6060e01a440daa361a99bcf040；PGM 6e9d813c985406aaf14c1645af2b494d342ae657 |
| 锁 / findings / ledger 两树 blob | 相同，本 diff 未改 |
| 合并状态 | 无 MERGE_HEAD、rebase-merge、rebase-apply、CHERRY_PICK_HEAD |

锁实算（先对文件做哈希，再对照 sidecar 与 ledger；对照值不是实算来源）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具输出相同，写入首行。文件 891182 字节，版本仍为 V0.14。

写入前：上述 30 条路径里，29 个已存在文件的 before blob 与 origin/main daf6adfc 逐字节相同（`git rev-parse <tree>:<path>` 比对）。V433 在 before tree 与 origin/main 都不存在（`git cat-file -e` exit 128）。派发「写入前与 origin/main 逐字节相同」对这 30 条成立。

diff 全文不重复贴进本报告。冻结文件的 SHA256 已与 `git diff --binary` 两个 tree 的输出逐字节相同，下面的逐块表覆盖该 diff 的全部 30 个路径、全部 hunk。一次性 worktree 在 xm034_b3_run.log 末尾已删除（scratch removed）。本席不能再对那个目录做字节比较。xm034_b3_apply.log 写「主工作区 diff 与一次性 worktree 验证过的 diff：same」，并给出与本席相同的 tree id 与冻结 SHA。本席独立复核的是冻结 diff 与两 tree。

闸 3 唯一来源：findings.md V4.08 顶部「当前 XM-M034-AB B2-r1 双 PASS 定稿（按 D-033「删除」）」段，加上该段所引「按 D-033「删除」重写」候选里 001…004 的最小处置。修复队列「是」：XM-M034-AB-001、002、003、004。修复队列「否」：0。本 diff 未把队列改成「否」。

定稿吸收的本席 B2 边界（不另开 ID），本轮用来核内容：

- AmusementControllerTests.writeRoutesRejectMissingIdempotencyKey 的 CsvSource 第 195 行一并删除。
- 门禁生成物点名进入写入集合：数据隔离规格（最新迁移戳 V432→V433）；security-scans/formal-security-baseline-v1.md 与 findings-register-v1.md（OpenAPI operation count 732→730）；两份契约附录。domain-ownership-scope 与 matrix-vs-migration 不受 V433 影响。
- 002/003 不另写 allow/release 拒绝逻辑，随整接口删除。releaseEntryConcurrentCapacity 四处不动。权限矩阵第 78、269 行只改 amusement.verification.override 的括号；第 78 行事件列去掉已不再发出的 amusement.verification_overridden；amusement.verification.execute 留在格内。
- 生成物在 origin/main 加补丁的一次性 worktree 内产出，再落入主工作区。

---

## 2. 技能

按 V0.17 §6 在本环境核过存在性。未用 MCP、子代理、第三席。未对人发问。未改业务、HTML、锁、findings、ledger。未 fetch、commit、push、merge、deploy。未装依赖。未对 complex、complex_test 执行写语句。未新建一次性 worktree 或空库（本轮结论不依赖再跑 Maven）。/tmp/xm034_small.diff 用完已删。

| 技能 | 处置 |
|---|---|
| using-superpowers | 已读。开工先核技能，再审 diff |
| using-arc | 已读。本任务走 V0.17 两席审计，不转 Arc 的 ideate/implement/audit 流程 |
| graft | 已读。本轮证据是冻结 tree 上的 `git grep <tree>` 与 `git diff`，不是活索引。未调用 graft 命令，无节省行 |
| systematic-debugging | 已读。只借「读错误、追数据流」。本体的加诊断日志、改代码试假说作废。追的是首轮 predeploy 红在 verify:production-readiness-evidence.mjs:119-120，未重跑该链 |
| verification-before-completion | 已读。票面之前本席自己跑了锁哈希、diff SHA、`--raw`、`--check`、OpenAPI 深相等、operation/通道计数、CSV 列差分、Java/TS 残留 grep，并读了校验器源码与红测日志原文。未把「日志说绿」写成自己重跑的退出码 |
| grilling | 已读。只借追问：有没有 403 壳、releaseEntry 有没有被删、旧迁移下新回归会不会绿、生成物有没有夹带无关句、证据文件是不是顺手改。事实自己读。本体的等用户回答、派子代理作废 |
| ponytail / ponytail-review | 已读。代码侧是删除，没有新抽象。证据 JSON 多出来的三个字段是本轮可删的部分。见 §6 |
| diagnosing-bugs | 已读。红环是日志里的那条断言（expected retired, was active）。本席读了该段，没有另起 Maven 循环，也没有加探针 |
| tdd / test-driven-development | 技能在。B4 不写测试。本席只核 diff 里已有回归，以及日志里的红绿记录 |
| requesting-code-review | 本席就是被请来的 B4 外审。不另派审 |
| receiving-code-review | 本轮是出票，不是收票。未调用 |
| codebase-design | 已核存在。本轮不改接口、不提新抽象 |
| resolving-merge-conflicts | 已核存在。工作区不在合并或变基中，未用 |
| writing-for-agents | 已读。报告按「锁、票、逐块表、内容、证据文件判定」排，diff 正文用冻结文件的哈希锚定，不把 12 万字节再抄一份 |
| git-guardrails-claude-code | 存在。本轮无 git 写操作，未调用 |
| batch-grill-me、ubiquitous-language | 在 mattpocock 技能目录中未找到。记缺口，未冒充调用 |
| to-spec、wait-what、triage、code-review、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、executing-plans、ponytail-audit、ponytail-debt、ponytail-gain | 按 V0.17 划掉或不适用。存在与否不影响本票。未调用 |
| i-have-adhd | 提示词要求首行即下一步、CLI 回进度。本次派发要求 CLI 不回正文、不回机器标记，报告只进本文件。派发优先 |

---

## 3. 逐块归因

规则：每一行必须能归到 001…004 里仍归属「是」的最小处置，或归到定稿写明吸收的边界。验证性测试归到它所验证的 ID。归因不到的行是越界。一块里只要有越界行，归因票就是 FAIL。

| # | 路径 | hunk 在做什么 | ID | 判定 |
|---|---|---|---|---|
| 1 | AmusementController.java | 删 GET/POST `/manual-overrides` 两个方法。同包类型，无多余 import 改动 | 004 | 可归因 |
| 2 | AmusementManualOverrideRecord.java | 整文件删除，17 行 | 004 | 可归因 |
| 3 | AmusementManualOverrideRequest.java | 整文件删除，14 行 | 004 | 可归因 |
| 4 | AmusementRepository.java | 删 `clearProjectConcurrentCapacity` 声明 | 002 | 可归因 |
| 5 | 同上 | 删 `saveManualOverride`、`listManualOverrides`、`listManualOverridesByMerchant`。`releaseEntryConcurrentCapacity` 声明留在上下文里，未改 | 004 | 可归因 |
| 6 | AmusementService.java | 删 `OVERRIDE_TYPES`（allow/deny/pause/resume/release 整集）。003 的 allow、002 的 release 不单留拒绝分支，随 004 整接口删除 | 004 | 可归因 |
| 7 | 同上 | 删 `listManualOverrides`、`createManualOverride` | 004 | 可归因 |
| 8 | 同上 | 删 `createManualOverrideInternal`。其中 release 调 clear、allow→approved 随整方法删除，不另写拒绝 | 004（覆盖 002 的 release 分支与 003 的 allow 分支） | 可归因 |
| 9 | 同上 | 删返回值 id 方法里 `AmusementManualOverrideRecord` 分支。相邻其他类型分支保留 | 004 | 可归因 |
| 10 | JdbcAmusementRepository.java | 删 `clearProjectConcurrentCapacity` 实现（point_device/project 清零） | 002 | 可归因 |
| 11 | 同上 | 删 save/list/listByMerchant 与 `manualOverrideParameters` | 004 | 可归因 |
| 12 | JdbcAmusementRowMapper.java | 删 `mapManualOverride` | 004 | 可归因 |
| 13 | TicketVerificationService.java | 删 `manualOverride`（写 status=manual_overridden）。冲正方法保留 | 001 | 可归因 |
| 14 | TicketingController.java | 删 `VerificationManualOverrideRequest` import，删 POST `/ticketing/verifications/manual-override`。无 403 空壳 | 001 | 可归因 |
| 15 | TicketingService.java | 删 `manualOverrideVerification` 及其 GuardedAction | 001 | 可归因 |
| 16 | VerificationManualOverrideRequest.java | 整文件删除 | 001 | 可归因 |
| 17 | V433__retire_amusement_verification_override.sql | 新文件 16 行。见 §4.4，与 V429 同形 | 004 | 可归因 |
| 18 | AmusementControllerTests.java | 删 list/create 桩、权限 CsvSource 两行、bodyFor 的 allow 分支、样例方法 | 004（样例里的 allow/approved 即 003） | 可归因 |
| 19 | 同上 | before 文件第 195 行 `"/api/v1/amusement/manual-overrides"` 位于 `writeRoutesRejectMissingIdempotencyKey` 的 CsvSource，已删。方法名在第 224 行，其余路径保留 | 004（定稿点名的边界） | 可归因 |
| 20 | AmusementJdbcIntegrationTests.java | `permissionCodes()` 全量清单第 55 行仍含 `amusement.verification.override`，before 与 after 该段逐行相同 | 004 要求保留 | 本块无改动 |
| 21 | 同上 | `platformAdminPermissionCodes()` 去掉该码。其余码保留，含 `amusement.verification.execute`（原第 91 行，删一行后为第 90 行） | 004 | 可归因 |
| 22 | 同上 | 从 saveAndList 流程删掉 `saveManualOverride` 与 `listManualOverrides` 断言。同方法其余保存与断言保留 | 004 | 可归因 |
| 23 | 同上 | 新增 `amusementOverridePermissionIsRetiredAndGrantedToNobody`：status=retired、四处授予之和为 0、表仍在 | 004 | 可归因。是这条 finding 的验证测试 |
| 24 | AmusementServiceTests.java | 删 `createManualOverrideRejectsCrossMerchantProject` 与 `manualOverrideRequiresReasonAndRecordsOverride`（后者断言 allow/approved 成功） | 004（含 003） | 可归因 |
| 25 | 同上 | 删内存仓 Map，以及 clear / save / list / listByMerchant 四个实现。`releaseEntryConcurrentCapacity` 内存实现仍在第 3021 行 | clear 归 002，其余归 004 | 可归因 |
| 26 | AmusementExitReleaseJdbcIntegrationTests.java | 删 import、类注释里的主管释放、`@AfterEach` 里只为清改判行的 DELETE、`supervisorReleaseClearsConcurrentCapacityWithAudit` 整测试；`sessionBatchCapacityUnaffectedByExitAndSupervisorRelease` 去掉末段主管释放并改名为 `sessionBatchCapacityUnaffectedByExit`；证伪注释改为只点 releaseEntry，用例序号随删除重排 | 002 | 可归因。出场扣减用例 freeFlow、repeatExit、并发唯一约束、无入场即出场仍在 |
| 27 | TicketingControllerTests.java | 删四条：无权限 403、缺幂等键、成功 manual_overridden、跨商户 403 | 001 | 可归因。定稿要求删这五条里的四条，不留 403 壳 |
| 28 | TicketingServiceTests.java | 删 `manualOverrideRecordsManualOverriddenVerification` | 001 | 可归因。第五条 |
| 29 | data-isolation-and-tenant-scope-security-spec-v0.1.md | 仅自动区块标题：最新迁移 V432→V433。表数仍为 172。无其他行 | 004 | 可归因 |
| 30 | openapi-p0.json 与 openapi-p0.yaml | 各三处删除：路径上 GET list 与 POST create；requestBodies 与 schemas 的 AmusementManualOverrideRequest。无新增 operation，无其他组件键增减 | 004（003 的 allow 枚举在被删的 schema 里） | 可归因 |
| 31 | p0-api-contract-pack-v0.1.md:334 | 「待代码轮下线，不得新增调用」改为「接口已下线」。路径行还在 | 004。B2 允许删行或改成接口已下线，这里取后者 | 可归因 |
| 32 | p0-openapi-contract-closure-appendix-v0.1.md | 计数与两操作相关行，见 §4.7 | 004。定稿要求用 docs:openapi-closure 重生成 | 可归因 |
| 33 | p0-openapi-operation-acceptance-matrix-v0.1.md | 标题 732→730；删 GET/POST 两行。无其他行 | 004。定稿要求用 docs:operation-matrix 重生成 | 可归因 |
| 34 | p0-operation-permission-matrix-v0.1.md:63 | `verification.manual_override`（停用，待代码轮下线）→（停用，写入口已下线）。事件列 `verification.manual_overridden`（停用）未动 | 001 | 可归因 |
| 35 | 同文件:78 | 括号内改为「停用，游乐改判接口已下线」。事件列去掉 `amusement.verification_overridden`。`amusement.verification.execute` 与 succeeded/failed 留在格内 | 004（定稿边界：只改该码括号，并去掉不再发出的事件） | 可归因 |
| 36 | 同文件:251 | 括号由「停用」改为「停用，写入口已下线」。`verification.reverse` 留在格内。改前这行并没有「待代码轮下线」五字，改动仍只在括号内 | 001 | 可归因 |
| 37 | 同文件:269 | 括号内改为「停用，改判接口已下线」。`amusement.verification.execute` 留在格内 | 004 | 可归因 |
| 38 | requirements-code-implementation-matrix-v0.1.csv | 仅第 49–52 行（REQ-45…48）各删两处接口引用，共 8 处。added 为空。表名 `amusement_manual_overrides` 与代码列里的 `AmusementManualOverrideRecord.java` 仍在 | 004。B2 写明不为此重写整行 | 可归因 |
| 39 | findings-register-v1.md | operation count 732→730；OpenAPI SHA-256 从 before 文件的哈希换成 after 文件的哈希 | 004。定稿点名 docs:security-baseline | 可归因 |
| 40 | formal-security-baseline-v1.md | 同上两行，另加 staff-token 677→675、全部 operation 732→730、写 operation 441→440、staff_work_phone 81→80、tablet 103→102、web 694→693。Java/Spring 版本等行未动 | 004。计数与删掉的两个 operation 一致，见 §4.7 | 可归因 |
| 41 | mock-api.ts | 删 `"/api/v1/amusement/manual-overrides": empty` 一行 | 004 | 可归因 |
| 42 | security-baseline-latest.json | `openapi_operation_count` 732→730 | 004 | 可归因。理由见 §5，不把这一行算越界 |
| 43 | 同文件 | `verified_at` 2026-09-10T05:00:53Z→2026-09-24T08:04:55Z | 无 | 越界 |
| 44 | 同文件 | `notes` 整段从 U-FIX/732 叙述换成 XM-M034-AB 新叙述 | 无 | 越界 |
| 45 | 同文件 | `version` V0.03→V0.04 | 无 | 越界 |

未改、且定稿要求不动的对象，不在 diff 里：V067、V270、V429、V014、V234、V099、V251、V254、domain-ownership-scope、matrix-migration-claims、三份 HTML、criteria-lock、findings、ledger。

Java/TS/TSX/KT 残留：对 after tree 检索 clearProjectConcurrentCapacity、manualOverrideVerification、两个记录类型、票务 DTO、create/listManualOverrides、mapManualOverride、manualOverrideParameters、两个 operationId、manual-overrides、manual-override。零命中。V270 的 SQL 注释仍写着旧方法名，定稿要求不改 V270。security-scans/semgrep-latest-summary.json 仍把两个已删 Java 路径列在历史扫描清单里；该文件不在本 diff，不是调用，定稿未要求改它。

`releaseEntryConcurrentCapacity` 四处仍在 after 树，且不在被删行里：AmusementRepository.java:244 声明、AmusementService.java:1409 调用、JdbcAmusementRepository.java:1085 实现、AmusementServiceTests.java:3021 内存实现。出场测试里只剩注释中的这个名字。

---

## 4. 内容

### 4.1 001

票务写入口链删干净：控制器映射与 import、TicketingService.manualOverrideVerification、TicketVerificationService.manualOverride、只被该入口使用的 DTO。测试五条全删（控制器四条、服务一条），没有改成 404/403 空壳。权限矩阵第 63 行与第 251 行只改人工放行码的括号措辞，reverse 与 manual_overridden 事件格保留。V067、V270、V429 不在 diff 中。

### 4.2 002

`clearProjectConcurrentCapacity` 三处都删了：接口声明、Jdbc 实现、内存仓实现。没有第四个实现者留在 after 树。没有新增 release 拒绝分支。出场测试按 B2 第 3 节切分：主管释放整例删除；场次用例去掉末段并改名；类注释与证伪注释同步；清理改判行的 DELETE 与 import 删除。留下的出场断言仍是：入场后出场占用回 0、重复出场只释放一次、并发双入场被唯一约束拒绝、无未释放入场则拒绝、场次 used_capacity 在出场后仍为 1。这是删掉人工清零，不是把出场扣减弱掉。

### 4.3 003 与 004 的删除面

allow/approved 随整方法、OpenAPI 组件和成功样例一起删除，没有单独的拒绝枚举。两端点、OVERRIDE_TYPES、list/create、createManualOverrideInternal、id 分支、四个仓储方法与参数助手、行映射、两个记录类型文件都在 diff 里，after 树 Java/TS 零残留。V099 准入策略行、表、历史行、V014/V234 CHECK 未改。权限中文名（V251/V254）未改。

V433 与 origin/main 上的 V429 同形，只换权限码与注释：

- `UPDATE permissions SET status='retired'`
- `DELETE` role_permissions、permission_template_items、user_permission_overrides
- `UPDATE temporary_permission_grants SET status='revoked', revoked_by='V433', revoked_at=now()`，条件是该码且 status=active

不 `DELETE FROM permissions`，不改表。V014:185 把该码种成 active，V014:213 授平台管理员，V170:180 再次写成 active。V251/V254 只改中文名。before 树的迁移里没有更早的 retired。V433 排在这些语句之后，最终状态由它决定。

### 4.4 真库回归

新测试断言 status 等于 retired、四处计数之和为 0，SQL 与 AccessJdbcIntegrationTests.manualOverridePermissionIsRetiredAndGrantedToNobody 的四处求和同形；状态断言写成直接读 `permissions.status`，比 Access 的 `permissionExists`（只数 active）更贴定稿「该码 retired」。另断言 `amusement_manual_overrides` 表仍在，对应保留表。

旧迁移下会失败，有日志原文，不是推断：xm034_b3_red.mvn.log 在只落测试补丁、未落 V433 时失败于

`AmusementJdbcIntegrationTests.amusementOverridePermissionIsRetiredAndGrantedToNobody`

`expected: "retired" but was: "active"`

（该次运行栈指向测试文件第 480 行）。xm034_b3_run.log 记 RED：3529 测 1 红，且新增红就是这条。全补丁后的 GREEN 日志记 3529 测 0 红。本席没有重跑 Maven。

全量权限清单保留、平台管理员清单去码，已在 §3 第 20–21 行核对 before/after 原文。

### 4.5 契约

after 树两份 OpenAPI：`node` + `yaml` 对 paths 与 components 做与 tools/verify-openapi-contract.mjs 同口径的深比较，diff 数 0。相对 before，operation 只少这两条，没有新增：

- GET `/api/v1/amusement/manual-overrides` listAmusementManualOverrides，staff-token，通道 web、tablet
- POST 同路径 createAmusementManualOverride，staff-token，通道 staff_work_phone

组件只少 requestBodies 与 schemas 的 AmusementManualOverrideRequest。yaml 的 `operationId:` 行数 before 732、after 730。

### 4.6 权限矩阵与需求矩阵

第 78 与 269 行只动该码括号；第 78 行事件列只去掉 `amusement.verification_overridden`。execute 留在两行的权限格。第 63 与 251 行是 001 的措辞，不是 004 的括号。需求矩阵 98 行对 98 行，只有第 49–52 行各去掉那两段 `POST ...#createAmusementManualOverride; GET ...#listAmusementManualOverrides; `，added 为空，共 8 处引用。

### 4.7 四类生成物

本席用 after 树的 openapi-p0.json 按 generate-formal-security-baseline.mjs 的算法重数：operation 730、写 operation 440、staff-token 675、staff_work_phone 80、tablet 102、web 693。与基线 diff 后的数字一致。相对 before 的减少量等于被删的两个 operation：staff-token −2，写操作 −1（只有 POST），web/tablet/staff_work_phone 各 −1。

OpenAPI 文件 SHA-256：before 内容 `c7c4857abec5a2f009da6e725b08ab8067b741acb90112280a45ef7781df846e`，after 内容 `d6a2085004caaa10b33f5057c7fa973da19b1064a30579562a3f7df64bbb9e2a`。基线与 findings register 的前后哈希就是这两值。生成器对 json 原文做 sha256，不是 git blob。

闭包附录的计数变化与这两条 operation 的附属行一致：operation 732→730，权限码行 −1（329→328，未覆盖仍 227，因为删的是「已在权限矩阵正文出现」的那一行），事件 −4（1464→1460；其中 1 条原在正文、3 条只在附录，未覆盖 1422→1419），任务追溯未覆盖 726→724，写接口 441→440。删掉的行就是该权限码、四个事件名、create/list 两行。验收矩阵只改总数并删这两行。隔离规格只改迁移戳。domain-ownership 与 matrix-vs-migration 不在 diff 里。

这些生成物没有夹带与这两个接口、接口数、迁移戳无关的句子。

### 4.8 有没有借机删弱或新业务

没有新端点、新权限、新拒绝分支、新表、新依赖。文件模式未变。票务侧删的是把人工放行断言为成功的测试，不是冲正或普通核销。游乐侧删的是改判入口和人工清零，出场扣减路径还在。契约包保留「接口已下线」一行、需求矩阵代码列仍指向已删的 Record 文件，都是定稿/B2 已经允许的残留，不是本 diff 新加的业务。

内容票不把 §5 的三行算成新业务或新功能缺陷：它们不改变核销结果、在玩人数或授权行为，校验器也不读它们。它们的问题是越界，记在归因票。

本席未重跑 `verify:predeploy`。xm034_b3_run.log 第二段记 verify:predeploy=0，且在此之前 docs:operation-matrix、docs:openapi-closure、docs:data-isolation-spec、docs:security-baseline 的退出码都是 0。首轮日志（xm034_b3_run_first.log）在还没有改证据 JSON 时，verify:predeploy=1，原文是 `security evidence openapi_operation_count must be 730, got 732`。这与 §5 的源码一致。

---

## 5. 证据文件是否越界

文件：docs/production-readiness-evidence/security-baseline-latest.json。

定稿点名的门禁写入集合没有这个文件。它也没有生成器。tools/generate-formal-security-baseline.mjs 只写 formal-security-baseline-v1.md 与 findings-register-v1.md。全仓 tools/*.mjs 里，读这个 JSON 的是 verify-production-readiness-evidence.mjs。

该脚本实际比较的字段（第 107–121 行）是：status、evidence_type、verification_command、baseline_path、findings_register_path、plaintext_secret_present，以及 `openapi_operation_count` 必须等于 openapi-p0.json 的 operation 数。不读 verified_at、notes、version、verified_on_host、security_baseline_version。

首轮 GREEN 的失败与这段源码是同一句话。004 的最小处置第 ⑦ 步要求跑全链 verify:predeploy。删掉两个 operation 之后，不改这个计数字段，链会在这一关停住。定稿点名时漏了这个文件，是因为 B2 没有扫到这个校验器；漏点名不能把闸门正在比较的字段说成与 004 无关。所以 `openapi_operation_count` 732→730 归 004，不是越界。

另外三行不是这条链变绿的条件：

- verified_at。改不改，第 119 行的比较都一样。旧时间戳也不包含「732」这个数字，与计数字段不构成同一句矛盾。
- notes。旧文是 U-FIX 那次同步的说明，里面写着「同步 732 项接口」。计数字段改成 730 之后，若叙述仍把当前接口数写成 732，文件会自相矛盾。消掉这个矛盾的最小改动，是把叙述里的当前接口数从 732 改成 730，留下 U-FIX 旧说明。现在的 diff 把整段换成 XM-M034-AB 的新叙述（票务写入口、两个 operationId、生成器重生两件）。新叙述与事实相符，但不是最小处置，也不是定稿点名的行。
- version V0.03→V0.04。校验器不读。V0.03 这个记号本身不写着 732。抬版本不是让 predeploy 通过的条件，也不是消除「732」矛盾的条件。

这三行归因不到 001、002、003、004。按 §5 闸 3，归因不到的行即越界。归因票因此 FAIL。

若证据文件只把 openapi_operation_count 改为 730，并在 notes 里只把当前接口数 732 改成 730，而不改写其余旧说明、不改 version、不改 verified_at，本席会把该文件整段归因到 004。当前 diff 没有做成这样。

---

## 6. ponytail-review

Java、测试、契约与四类生成物：要删的入口已经删掉，没有留下 403 壳或 allow/release 拒绝层。Lean already。

`docs/production-readiness-evidence/security-baseline-latest.json`：verified_at、notes 整段、version 三处改动。替换：只保留 openapi_operation_count=730；notes 若要动，只改其中的当前接口数。net: 这三处不该出现在本轮 diff 里。

---

## 7. 票

内容：PASS。001 只删票务写入口链和矩阵第 63、251 行措辞，V067/V270/V429 未动。002 只删 clearProjectConcurrentCapacity 三处，releaseEntryConcurrentCapacity 四处未动，出场测试按 B2 第 3 节切分。003 与 004 的下线删除面在 after 树没有 Java/TS 残留调用，两个记录类型已删。V433 与 V429 同形。平台管理员清单去码，全量权限清单保留。新回归在未落 V433 时失败于 status 仍为 active，日志有原文。控制器测试第 195 行已删。两份契约删路径与组件且 paths/components 深相等。四类生成物的变更是接口数、迁移戳、这两个接口及其生成器必然重写的计数与哈希。权限矩阵第 78 与 269 行只改括号，第 78 行同时去掉事件列里的 verification_overridden。需求矩阵只去掉 8 处接口引用。没有借机删弱，没有新业务。文件模式未变。

归因：FAIL。security-baseline-latest.json 的 verified_at、notes、version 三行越界。同文件的 openapi_operation_count 一行可归因到 004。其余 29 个文件的改动行都能归因到 001…004。

总结论：FAIL。任一张 FAIL 即 FAIL。本席否决本轮 B4。不代替席甲票，也不把 B2 的 PASS 当成这一张。

回退时按 V0.17：越界行没有已定稿且仍在修复队列的 ID。本席不改 findings、不分配新 ID、不修 diff。
