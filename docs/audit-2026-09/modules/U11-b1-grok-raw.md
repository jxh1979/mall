CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U11 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 11 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U11 — 未发布材料第 11 段「员工回归测试」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：8 文件，物理行 2247，标记 NORMAL；main 主归属：**M056**（5 个既有文件）+ 3 个未发布新增文件（main 无此路径）
- 冻结判据：M056 映射行并集 52 项（相对开工提示 MISSING=[] EXTRA=[]）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U11-001、GR-U11-002、GR-U11-003、GR-U11-004、GR-U11-005、GR-U11-006、GR-U11-007、GR-U11-008
- 添加项（B1 只登记，不代用户回答 2.2）：**GR-U11-002** = `M010RegressionTests.java:321-330` `withSelfOperated(true)`，与已定稿 U04-001 / U05-001 / U08-001 / U10-002 同根；操作员 2026-09-09 已在 `questions/U04-001.md` 答「删除」。**不再弹窗。**
- 同根已定稿、不新开 2.2：U05-002 / U05-003 / U05-004 / U05-005 / U05-006 / U05-008 / U06-007 / U09-004 / U10-001 / U10-003
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。
- 本席独立实跑：未发布树 `/tmp/u11_grok_backend` `mvn -o test-compile` EXIT 0；单测 `StaffScopeTests`+`StaffAttendanceLocalZoneTests` 7/7 PASS；隔离空库 `u11_grok_probe`（**非** `complex_test*`）Flyway 387 条迁到 **V409** 后本段 7 类 **92/92 PASS**，随后 DROP；`complex_test` 事后只读仍为 max=**410**。92/92 **只证明副本树自洽**，不能消解 HTML 冲突或对 main 的分叉。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md`（SHA256 `b9339b09ee3980808d8c5a35c7251b891b8b1e0036ec4ca37812b976ebddf6b6`） | 隔离库 `u11_grok_probe` 空库迁到 V409 跑 92 测后 DROP |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md`（SHA256 `808fc5717aa88ad65efff312b11c186294d3e6ee301afb584e2f86599b137787`） | 只借读错误→复现→追数据流（本段测试 → U05/U06/U09/U10 副本 → main 对照 → HTML） |
| verification-before-completion | superpowers 同名（SHA256 `2befe7fc55bcadaa3d97dd9e8efeb633d2561c0ebe74c5a8b17c4d9e7e4520b3`） | 先跑命令再声称；92/92 不能写成 HTML 通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md`（SHA256 `40df33b58fc6ef889b93585733feb9566b76e9586efa7f376785c1e995197ac0`） | 只列可删复杂度（自营布尔测例、死 stub），不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md`（SHA256 `a842323e664e5af104eac5c97ad22fda929ebeb62d81c501161ac1f6f482db58`） | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md`（SHA256 `6f9ae7c1c295d7e67eda1e3a616c337c057f45f1a5bf2955efb07d539a655b79`） | 本段验证均秒～十几秒，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：套件文档写明已并入 `grilling`，无独立技能可装
- `ubiquitous-language`：本机技能树未找到
- Arc `audit` 会派评审子代理：与双席/§7 冲突，**不调用**
- `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、MCP、第三主体。

内部 grilling 树（自答，未问操作员）：

1. 这 8 个文件必须存在吗？必须有**等价回归**。删掉它们，BSM L1433 两条注册、L1443 排班审批/锁定、L1444 可见天数、L1445/BEQR L280 打卡三方式取最早、L1450 事假扣薪、L1458 20% 封顶三审、PGM L579 私人手机绑定、PGM L582 寄存柜、PGM L870 证件/手机加密、BEQR L240 五态任职、铁律 6 存档删除，在可执行断言上都没有本段这一层网。文件作为测试是细化，不是添加。
2. 整段按现文件叠进当前 main、首轮会不会过编译？**不会。** 只把 8 个测试文件覆到 HEAD 源码：`mvn -o test-compile` EXIT 1，**106** 条 `[ERROR] /tmp/.../*.java`（4 个文件：`M010RegressionTests` / `StaffJdbcIntegrationTests` / `StaffAttendanceLocalZoneTests` / `StaffScopeTests`），缺 `StaffLockerService`、`StaffRegistrationService`、`StaffApprovalService`、`StaffNumericSettingRequest`、`selfOperated()`、`publicationStatus()`/`reviewId()`、`listShiftsEndingBetween` 等。PayrollSafety 与 ArchiveDelete 对 main 符号面能编过，但仍依赖新增 `StaffTestActors`。
3. 只合本段测试、库仍是当前 `complex_test`（V410、无 `parent_entity_id`、无 `staff_policy_settings`），首轮会不会红？**会。** `M010RegressionTests:334` `INSERT ... parent_entity_id` 对 V410 已删列；`shiftVisibility*` 读策略表；`migrationCreatesStaffThreeTablesAtLatestVersionAndSeedsPermissions` 把 DB flyway 与 classpath `MigrationVersions.latest()` 绑死——未发布 classpath 最大 V409，HEAD 库是 V410。
4. 副本树自己能不能绿？能。隔离空库 `u11_grok_probe` Flyway 校验 **387** 条、迁到 **V409**，本段 **92/92 PASS**，随后 DROP。这只证明「测试与未发布 U05–U10 实现 + V400–V409 一致」，**不能**证明与 HTML 或与当前 main 一致。
5. 92 个绿测是不是把错误口径锁死了？是。事假扣薪期望 5000/0、封顶 2200、可见天数先断言 7 再自插行、柜机双身份、公开自助注册无设备公钥、`withSelfOperated(true)`——都是首轮正常路径上的锁，不是两三件不巧才暴露。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，属正常）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- M056 映射行 L6435，52 ID 与开工提示逐项相同（MISSING=[] EXTRA=[]）；定义区均从锁全文读过；HTML 出处行本席对现行正本逐条 HIT（见 §1.2）

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.73，499667 bytes，SHA256 `3ac033d028a20fa2c3922322d6d35211ab8fbb39c01c9bbc810401230dcc56ed`（与开工提示一致）。findings V1.63，628454 bytes，SHA256 `cece0010040e21167e65ec80677b5418d5beeae1ef3dc713191858f0d119c90a`（与开工提示一致）。台账中与本段直接相关的已定稿项（背景，不继承 PASS）：

- U00-000：未发布树相对 242f4872 全 76 文件与 main 分叉
- U05-002：事假扣薪基数＝当期应发；点名 U11 `:637-675` 两处期望
- U05-003 / U06-002 / U07-004：自助注册设备公钥/指纹；U07 permitAll
- U05-004：封顶分母按 HTML 总薪资
- U05-005 / U06-005 / U10-003：`shift_visible_days` 缺行不得空上下限；U11 `:203-215` 先断言 7
- U05-006：柜机双身份；第 11 段同时注入两身份
- U05-008 / U06-007 / U07-005 / U09-001 / U10-001：以 main 为底重放，迁移 ≥V411，禁止整树覆盖
- U09-004：U11 `M010RegressionTests` 只 `isInstanceOf(ApiException.class)` 不断言错误码
- U04-001 / U08-001 / U10-002：`is_self_operated` 已答删除
- U06-004：顶班记录入口（本段无对应可失败用例）
- U06-010：`missing_checkout` 第六态 main 既有，归 M054

其它只读哈希：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| `plan/runbook.md` V0.02 | 10409 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `plan/modules.md` V0.04 | 421826 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |
| `160-audit-prompt.md` V0.17 | 65856 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `unpublished-03acdb98-input.json` V0.01 | 17547 | `10eb8240ee08ee2fae053235250f19335cacc2ff8d9a2ed5edbe69be366293e9` |
| `docs/business-enum-quick-reference-v0.1.html` | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

三 HTML 只读、未改。本段冻结集含 HTML 派生 ID；分类对照现行正本原文（类型 1）。三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。BSM-004（删除须他人审）**不在** M056 映射，本席不把它写成新锁 ID。

清单本段 8 条与独立 `sha256sum`/`wc -l`/`wc -c` **8/8 MATCH**。副本 SHA 与树 `e28dad05:<path>` blob（`git hash-object`）**8/8 COPY==TREE**。全 76 文件相对基线 `git diff --shortstat 242f4872 e28dad05` = **76 files changed, 4745 insertions(+), 1084 deletions(-)**（与开工提示一致）。

对象核验：`git cat-file -t 03acdb98…` fatal（无 commit）；`e28dad05…` = tree；`242f4872…` = commit。

本段相对发布基线逐文件 numstat（`git diff --numstat 242f4872 e28dad05 -- <path>`）：

| 路径 | + | − | 树 blob | 副本 SHA256 | 清单/字节/行 |
|---|---:|---:|---|---|---|
| `.../M010RegistrationHttpTests.java` | 38 | 0 | `9ff9d0c8f39937de6cf93e306e86c985ee9ba0f0` | `1972b4506db19caf3ace491d766baf8150a4f410dad1c189f2b976187f846fbd` | MATCH 2291 / 38 |
| `.../M010RegressionTests.java` | 676 | 0 | `abf83db189260ead0400174056d5fe3bb49266a0` | `9805d8eb5614ae4aa38196aab889b2cb71859d96cf7104561eefd0f1f91596cd` | MATCH 44746 / 676 |
| `.../PayrollSafetyJdbcIntegrationTests.java` | 5 | 0 | `2d92752c0c88cf12da8ff8687283ed6350bf32ca` | `854d792d1b0115a15cb0ec10451e44c1318da0a22fc367aa22c9438331aebddb` | MATCH 10978 / 207 |
| `.../StaffAttendanceLocalZoneTests.java` | 27 | 14 | `27dfa4bc458cbef8a7c07533d1b6a2b38ce83c6e` | `559a74ad36ede377005c33a22d31dda69a49529793be280f7d810b854127be48` | MATCH 10221 / 165 |
| `.../StaffJdbcIntegrationTests.java` | 90 | 78 | `bff84d4e809005f303983d979fc528a67cc3a9c6` | `8ac57d3efeb7b8f2a12d0dea5719b9cb22df79d94d8447b510764ec22a523883` | MATCH 58934 / 942 |
| `.../StaffProfileArchiveDeleteJdbcIntegrationTests.java` | 12 | 4 | `305993f41f612453a66af4704663eb7059d60b9f` | `4a010733c614bd6ca6e5620b27ddae2ed6cba14a764da6cba630d24973ba5632` | MATCH 5064 / 110 |
| `.../StaffScopeTests.java` | 13 | 6 | `b2cb852ba5d8eb09068de20182ba5968bf74701c` | `96993632643830c3e5967e45e7da4f1ac87558556f8b71e4a9bd8dd062ad46b8` | MATCH 4359 / 80 |
| `.../StaffTestActors.java` | 29 | 0 | `b40bf94f4e758c54842527e61a2c517f91bafbba` | `229787b24976e4fb14c7abee65165715a5ec64437a55f43a4a10415cbe55e9c8` | MATCH 1892 / 29 |
| **合计** | **890** | **102** | | | 2247 行 |

`git diff --stat 242f4872 e28dad05 -- <8 paths>`：`8 files changed, 890 insertions(+), 102 deletions(-)`。

HEAD 同名：3/8 MISSING（两个 M010* + `StaffTestActors`）；5/8 HAS 且 **BASE blob == HEAD blob**（`git log 242f4872..HEAD -- 五路径` 空）。M009–M012 **未改本段既有测试文件**。

`@Test` 计数（含 `@org.junit.jupiter.api.Test`）：2+43+7+4+29+4+3+0 = **92**。

### 1.1 与 main 分叉对照（对抗式固定问题）

本段 5 个既有文件 **BASE==HEAD**，按同名路径用 TREE 覆盖 HEAD **不会回退** M009–M012 在这些测试文件里的修复（那些检查点根本没碰它们）。冲突面是模式、符号与整树：

1. **只合本段测试、不合 U05–U10（GR-U11-001）**。HEAD `StaffService` 无 `staffApprovals` 字段、无 `StaffLockerService`/`StaffRegistrationService`/`StaffApprovalService`。本席 `/tmp/u11_grok_main_plus_u11tests`（main backend + 仅 8 个 U11 测试）`mvn -o test-compile` **EXIT 1**，106 条文件级 `[ERROR]`。首轮即红。
2. **`parent_entity_id` vs 已发布 V410**。`M010RegressionTests:334` `INSERT INTO business_entities(...,parent_entity_id,...)`。HEAD `V410__m010_business_entity_topology_and_role_isolation.sql:9` `DROP COLUMN IF EXISTS parent_entity_id`。本席 docker exec 只读 `complex_test`：`information_schema` 该列 **0 行**；flyway 成功版本 **410,399,398**（无 400–409）。把本段测跑在当前共享库上，该 INSERT 首轮即失败。
3. **Flyway latest 断言**。`StaffJdbcIntegrationTests:104` `currentFlywayVersion().isEqualTo(MigrationVersions.latest())`。未发布 classpath 最大 V409；HEAD 库 410。同测跑当前库即红。
4. **自营布尔（GR-U11-002）**。`:321-330` `merchant.withSelfOperated(true)` / `selfOperated()`。main `MerchantRecord` 无此方法（main+U11 编译已报 `cannot find symbol method selfOperated()`）。U04-001 已答删除。
5. **整棵未发布树覆盖**仍会回退其它段 M009–M012（U03/U08 已证实）。本段文件不是那些同名路径的载体，但发布说明必须禁止整树覆盖，且本段必须与 U05+U06+U07+U09+U10+U12 **同一 PR**、以 main 445a49f4 为底重放、迁移 ≥V411。

V410 正文不改 `staff_*` 测夹具列集。失败点是**编号/已删列/已删符号**，不是这 8 个测试文件被 M009–M012 改过。

### 1.2 HTML 锚点（本席对现行正本逐行 HIT）

锁出处行 → 正本该行含锚点标题/关键字，34/34 HIT：

| ID | 锁出处 | HIT |
|---|---|---|
| BEQR-157 | beqr:240 员工状态 | HIT 待入职、在职、试岗、停岗、离职 |
| BEQR-182 | beqr:279 考勤日结果 | HIT 出勤、缺勤、迟到、早退、请假；无排班=休息 |
| BEQR-183 | beqr:280 打卡方式 | HIT NFC、扫码、离开充电设施，取最早；晚到即迟到 |
| BEQR-184 | beqr:281 请假类型 | HIT 事假、病假、年假、调休 |
| BEQR-185 | beqr:282 请假单状态 | HIT 待审批、已批准、已驳回、已撤销、已销假 |
| BEQR-186 | beqr:283 薪资单构成 | HIT 总薪资=基础工资、各项补贴、绩效工资与奖金之和；扣除≤20% |
| BEQR-187 | beqr:285 员工提成发放 | HIT 月结、代扣个税、不允许现金现抽 |
| BSM-001 | bsm:505 | HIT 整体说明；L2295 未填上下限的参数不得上线 |
| BSM-003 | bsm:563 | HIT 系统通用能力：数据可改可删…（删除细则在 **BSM-004**，本模块未映射） |
| BSM-009 | bsm:596 | HIT 基础管理 |
| BSM-028 | bsm:698 | HIT 六类合作方式；多对多不是固定隶属链；员工注册审批 |
| BSM-029 | bsm:714 | HIT 用户权限要知道员工注册、设备绑定、私人/工作手机 |
| BSM-073 | bsm:1313 | HIT 商户专区主要功能（本段测试无商户专区实现面） |
| BSM-084 | bsm:1427 | HIT 员工手机端 |
| BSM-085 | bsm:1431 | HIT 两条注册路径；自营视同公司自有 |
| BSM-086 | bsm:1441 | HIT 排班上一级审批、开始即锁定、顶班、可见天数出厂 7 须上下限、打卡三方式、事假按应发比例扣 |
| BSM-087 | bsm:1455 | HIT 处罚与事假分栏；20% 上限三审 |
| BSM-088 | bsm:1467 | HIT 提成进月结、不允许现抽 |
| BSM-114 | bsm:1827 | HIT 各类入口只看自己该看的 |
| BSM-115 | bsm:1897 | HIT 基础管理连所有业务 |
| BSM-120 | bsm:1957 | HIT 已拍板：私人手机设备标识；寄存柜扫码不设手工登记 |
| PGM-001 | pgm:256 | HIT 平台治理层底座 |
| PGM-008 | pgm:362 | HIT 员工和手机卡片 |
| PGM-022 | pgm:463 | HIT 空间和点位 |
| PGM-024 | pgm:476 | HIT 设备和员工班次要知道责任区域 |
| PGM-026 | pgm:492 | HIT 六类合作方式 |
| PGM-032 | pgm:572 | HIT 人、私人手机、工作手机要能对应起来 |
| PGM-033 | pgm:576 | HIT 两条注册；安装时绑定设备标识；寄存柜扫码 |
| PGM-034 | pgm:586 | HIT 工作手机必须使用企业微信（本段测试无企微实现面） |
| PGM-035 | pgm:599 | HIT 提成进月结 |
| PGM-036 | pgm:611 | HIT 设备绑定员工、班次、点位 |
| PGM-048 | pgm:749 | HIT 商户侧授权范围内全开放（本段无商户专区页） |
| PGM-059 | pgm:868 | HIT 手机号/身份信息加密；日常界面脱敏 |
| PGM-066 | pgm:951 | HIT 界面全中文；系统内部标识符可用英文 |

## 2. 逐文件全量三线

数据流固定：本段测试 → 未发布副本 U05（`JdbcStaffRepository`/`StaffRegistrationService`/`StaffLockerService`/`StaffController`）/ U06（`StaffService`）/ U09（`SupervisorReviewService`）/ U10（V400–V409）→ main 当前对应源码 → SQL/迁移 → HTML。每文件整份读完（`StaffJdbcIntegrationTests` 942 行按结构段覆盖，见 2.5）。

### 2.1 `M010RegistrationHttpTests.java`（新增，38 行，2 测）

**覆盖**：L1–38 全文。`unregisteredEmployeeCanSubmitButReceivesOnlyPendingReceipt`：无认证 POST `/api/v1/staff/self-registrations`，断言 200、`registration_status=submitted`、响应无 `phone`/`user_id`；再 GET `/api/v1/staff/registration-companies` 200。`openingSelfRegistrationDoesNotOpenManagementOrApproval`：无认证 GET profiles / POST review → 401。

**第一性原理**：公开自助入口必须有测试；删掉则 U07 permitAll 无 HTTP 网。声称「只收待审回执、不开管理面」与 U05 `StaffRegistrationService.selfRegister` + U07 `SecurityConfig` 一致，**且把「无设备公钥也能提交」锁成绿**。

**顺从式**：与 PGM-033「提交资料后后台审批」、PGM-059 响应不含明文手机 同向。未测设备指纹（PGM-033/BSM-120「安装时绑定设备标识」）。

**对抗式**：公司 ID 写死 `be_proj_changsha`（V354 种子，空库有）。无频控/设备证明。与 U05-003 同根。硬编码 `complex_test`。

### 2.2 `M010RegressionTests.java`（新增，676 行，43 测）

**覆盖**：L1–676 全文，43 个 `@Test` 均读完。关键锁点：

| 行 | 测例 | 三线要点 |
|---|---|---|
| 94–125 | lockerScan… | 先 staff 会话再注入 `verified_terminal=kiosk`；U05 `StaffLockerService:40-52` 两闸。同根 U05-006 |
| 127–146 | employmentState* | 五态 `pending_entry/employed/trial/suspended/departed` 拒 `active`。对齐 BEQR-157 |
| 162–194 | attendanceRejectsManual / earliest / nfc≠checkout / 他人手机 | 服务只接受 nfc/qr/charging_disconnect；取最早一次。对齐 BEQR-183 |
| 203–228 | shiftVisibility* | **先** `listMyShifts().hasSize(7)`（缺行回落 7、上下限 null），**再**自插测试行。同根 U05-005/U10-003 |
| 230–256 | shiftIsNotPublishedUntil… | 上一级审批后才写入；修改追加。对齐 BSM-086 审批/留痕。`isInstanceOf(ApiException)` 无错误码 |
| 258–265 | startedShiftCannotBeChanged | 已开始不可改。文案在 U06 `:637` 提顶班，**本段无顶班入口测**（U06-004） |
| 267–304 | penaltyCapNeedsThreeDepartment… | hr/finance/legal 后 master；已关账月不改写。对齐三审。缺行码断言 |
| 306–330 | selfRegistration* / **selfOperated*** | 无账号；**`withSelfOperated(true)`**。同根 U04-001 |
| 332–346 | merchantRegistrationIsReviewedOnlyByItsDomainCompany | **`parent_entity_id` INSERT**。对 V410 首轮失败 |
| 348–354 | registrationInitiatorCannotApproveOwnApplication | 自审分离。对齐铁律 6/注册 |
| 398–426 | 加密/脱敏 | DB `enc:v1:`，JSON 掩码。对齐 PGM-059 |
| 538–549 | masterOwnDeletionDoesNotRequireAnotherApprovalStep | master 申请即 `approved`。U06 `:388-391` 自批。BSM-004 未映射，见观察 |
| 615–625 | crossMonth…BelongToEndMonth | 班次结束归属次月。对齐 BSM-086 跨月口径 |
| 637–655 | personalLeaveUsesScheduledHours… | **期望 leave=5000（月薪 10000 的一半）、全月事假 total=0**。基数＝固定工资。同根 U05-002 |
| 657–675 | ordinaryPenaltyCap… | **penalty=2200＝20%×(10000+1000 bonus)**；提成/事假排除。分母＝底薪+奖金。同根 U05-004 |

**第一性原理**：这是未发布员工能力的主回归网。43 个绿测在副本树上自洽（本席 43/43 PASS），同时把错误公式与已删布尔锁死。

**顺从式**：加密、五态、打卡三方式取最早、排班审批、跨月归属、商户/公司隔离、注册自审分离——与 HTML 同向。可见天数/事假基数/封顶分母/自营布尔/公开注册设备——与 HTML 冲突或与已答删除冲突。

**对抗式**：27 处 `isInstanceOf(ApiException.class)` 无 `code()`、无 `hasMessageContaining`（U09-004）。`role` 一次授出 12 个 staff.* + 主管审批。硬编码 `complex_test`。

### 2.3 `PayrollSafetyJdbcIntegrationTests.java`（既有，207 行，+5，7 测）

**覆盖**：L1–207 全文。相对 HEAD 仅加版本头 + `StaffTestActors.grant(usr_admin)`（未发布授权不再给 platform_admin 通配 staff.*）。四项资金安全：已发放不重结改写、手工提成/奖惩业务键去重、`staff.payroll.manage requires_approval`、税档区间排他。

**第一性原理**：删掉则 V238–V241 资金安全无 JDBC 网。本段增量是夹具，不是新口径。

**顺从式**：对齐 BSM-087 月结定格/重结规则与 GEN-006/007。`authenticateAsPlatformAdmin` 仍用 `AuthService.UserRecord`，与 `grant` 的 AccessActor 双轨；首轮在副本树上 7/7 绿。

**对抗式**：`seedProfile` 仍插 `phone_hash`/`id_doc_hash`（U10-004：旧列保留）。不覆盖 HTML 20% 总薪资公式（那在 Regression）。BASE==HEAD 主体。

### 2.4 `StaffAttendanceLocalZoneTests.java`（既有，165 行，+27/−14，4 测）

**覆盖**：L1–165 全文。增量：mock `IdempotencyService` 放行、注入档案、工作手机 `TerminalDeviceRecord`、`verified_terminal=staff_work_phone`。四个测例：跨零点下班早退/正常、±12h 窗外打卡绑最近班且 judgement=`normal`、月视图北京日历窗。

**第一性原理**：夜班配班必须按时间区间。main 已有此文件；未发布把 punch 改成必须工作手机，测试跟着加桩。

**顺从式**：跨零点命中夜班对齐 BEQR-183/BSM-086。月视图窗口 `2026-07-31T16:00Z`–`2026-08-31T16:00Z` 是北京 8 月。

**对抗式**：`punchFarOutsideAnyShiftFallsBackToNearestShift`（北京 03:00 打 09:00 班，判 `normal`）main 已有断言，未发布未加重。HTML 未定义「提前 6 小时算正常」；属实现口径，观察。对 main 编译：`listShiftsEndingBetween` 符号缺失（GR-U11-001）。

### 2.5 `StaffJdbcIntegrationTests.java`（既有，942 行，+90/−78，29 测）——OVERSIZE 按结构段

| 段 | 行 | 内容 | 覆盖 |
|---|---:|---|---|
| A | 1–79 | 类头、SpringBootTest、`StaffTestActors.grant`、直属上级 | 全文 |
| B | 80–317 | 原 T1：分页、flyway latest、注册审批、换机唯一 active、完整性、CHECK、商户 scope、加密、通知同意 | 全文 |
| C | 320–353 | `submitRegistration` 无商户走 `selfRegister`；`approvedProfile` 改为直接夹具；`seedPublishedShift`/`approveShift` | 全文 |
| D | 355–376 | v210 严格迟到/早退；无班 `no_shift`；已开始改班抛错 | 全文 |
| E | 378–508 | 请假重叠；v211 月结构成/重结/劳务个税/月视图 leave/absent/rest；事假无排班工时扣 0 | 全文 |
| F | 510–624 | 项目隔离、幂等、失败审计事件 | 全文 |
| G | 626–695 | 周排跨零点 + `publicationStatus=pending_review` + `approveShift` | 全文 |
| H | 723–776 | 项目口径月结不碰他项目 | 全文 |
| I | 778–941 | BYOD 本人提成/档案；`role_staff_self`；notice.view / shift.manage 守卫 | 全文 |

**第一性原理**：这是 main M056 主 JDBC 网。未发布把「upsertShift 立即生效」改成「草稿+审批」，测试用 `seedPublishedShift` 绕过审批去测打卡，用 `approveShift` 测周排。`submitRegistration(null)` 改为公开 `selfRegister`。

**顺从式**：换机唯一 active、跨商户完整性拒绝、加密、请假重叠、项目月结隔离、自助只见本人——与 BSM-114/PGM-059/GEN-005/007 同向。`currentFlywayVersion()==latest()` 在未发布树上等于 409（本席隔离库已见 Current version 409）。

**对抗式**：
- `:375` 无班打卡 `judgement=no_shift`：BEQR-182 日结果闭集无此态（那是日视图；punch 层 main 既有，观察）。
- `:323-325` 无认证 `selfRegister`（U05-003）。
- `:558-562` 直插 `method='manual'`：V405 CHECK 仍含 `manual`（历史可读），与服务拒 manual 不矛盾。
- 对 main：缺 `StaffRegistrationService`、`StaffSelfRegistrationRequest`、`phone()`、`publicationStatus()`/`reviewId()`。

### 2.6 `StaffProfileArchiveDeleteJdbcIntegrationTests.java`（既有，110 行，+12/−4，4 测）

**覆盖**：L1–110 全文。增量：`StaffTestActors.grant` 两用户；审批人从 `TestSecurityActors.asPlatformAdmin("usr_approver")` 改为 `StaffTestActors.master()`；自审测例改名并断言「最高权限」。软删：行仍在、前台 `findProfileById` 空。占用考勤拦删。先存档才能申请。

**第一性原理**：铁律 6 / BSM-003 标题下的删除网。main 已有；未发布把审批人收成 master。

**顺从式**：存档→申请→最高权限批准；占用拦删；申请人不能批。与 BSM 删除口径同向（细则在未映射的 BSM-004）。

**对抗式**：占用夹具 `method='manual'` 仍被 V405 允许。BASE==HEAD 主体 + master 夹具。对 main 编译本文件未进 106 条错误（符号仍在）。

### 2.7 `StaffScopeTests.java`（既有，80 行，+13/−6，3 测）

**覆盖**：L1–80 全文。增量：`currentActor` stub；`StaffApprovalService` mock + `ReflectionTestUtils.setField(service,"staffApprovals",…)`；日期改 2099。跨项目排班/批假拒 `CROSS_PROJECT_FORBIDDEN`；同项目不抛。

**第一性原理**：跨综合体隔离必须有单测。main 已有。

**顺从式**：拒绝路径仍走 `requireUserInScope` → `findUserProjectId`，断言错误码，有效。

**对抗式**：同项目通过路径未发布改为 `staffApprovals.submitShifts`，`when(repo.upsertShift)` **成为死桩**，通过路径只断言不抛、不 `verify` 审批提交。U05-012 观察 19 已点名，本席独立确认。对 main：`StaffApprovalService` 不存在。

### 2.8 `StaffTestActors.java`（新增，29 行，0 测）

**覆盖**：L1–29 全文。`grant`：建用户、固定 `role_m010_fixture`、把全部 `staff.%` + `governance.supervisor_review.approve` 授出。`actor`/`master` 写 SecurityContext。

**第一性原理**：未发布收紧「管理员类型 ≠ 全部 staff 权限」后，集成测必须显式授权。删除则 PayrollSafety/Archive/Jdbc 的 BeforeEach 无法在新授权下绿。

**顺从式**：注释写明「不依赖管理员类型自动拥有敏感权限」，与 GEN-005 同向。

**对抗式**：一次授出全部 `staff.%`，测试过宽；`ON CONFLICT DO NOTHING` 角色 id 固定，并行类共享。夹具不是生产路径。观察。

## 3. 适用冻结判据逐项（52/52）

| ID | 本段结论 | 证据 |
|---|---|---|
| GEN-001 | 部分 FAIL | 8 文件均有用途；`selfOperated` 测例承载已答删除的布尔（GR-U11-002） |
| GEN-002 | FAIL | 副本树 92/92 自洽；叠 main 编译 106 错；V410 库跑 `parent_entity_id` 必红（GR-U11-001） |
| GEN-003 | FAIL | 92 绿锁错误公式/缺种子/双身份/无设备注册；27 处无错误码（GR-U11-003–008） |
| GEN-004 | FAIL | HTTP/服务自助注册无设备证明（GR-U11-007）；管理面 401 测有效 |
| GEN-005 | 通过（登记） | 跨商户/跨公司/跨项目测有效；`listProfiles` 自助角色 PERMISSION_DENIED；夹具过宽见观察 |
| GEN-006 | 通过（登记） | 失败批不留班、唯一 active、税档排他、已发放不改写；事假/封顶公式错归 003/004 |
| GEN-007 | 通过（登记） | 幂等键/业务键去重测存在 |
| GEN-008 | FAIL | 测试/DTO/V410 列/自营布尔/OpenAPI 字段不一致（001/002） |
| GEN-009 | 通过（登记） | 失败审计事件测存在；中文校验文案多处 |
| GEN-010 | FAIL | 公开注册无设备/频控（007）；PII 加密测有效 |
| GEN-011 | 通过（登记） | 失败路径 `audit_events`；locker `recordSuccess` |
| GEN-012 | 无实现面 | 本段无无界批处理生产代码；周排 32 天拒绝是服务行为，测试覆盖 |
| GEN-013 | FAIL | 硬编码 `complex_test`；只合本段依赖未声明的 U05–U10 类型（001） |
| GEN-014 | FAIL | latest() vs V410；`parent_entity_id`；策略表仅在 V402（001/005） |
| GEN-015 | 通过（登记） | 展示中文断言为主；JSON 状态码属系统内部标识符 |
| GEN-016 | FAIL | 测例注释「出厂七天」「六态口径」相对 HTML 过宽/过窄；可见天数注释不能当出厂上下限证据 |
| GEN-017 | FAIL | 自营布尔测例＝添加已答删除；不得伪装技术性（002） |
| GEN-018 | 通过 | 本席结论落到 HTML 原文、隔离 92/92、main 编译失败、V410 只读 |
| BEQR-157 | 通过（登记） | 五态循环 + 拒 `active` |
| BEQR-182 | 通过（登记） | 日视图 present/absent/leave/rest；punch `no_shift` 为 main 既有层，观察 |
| BEQR-183 | 通过（登记） | 三方式取最早、拒 manual、NFC 不能下班 |
| BEQR-184 | 通过（登记） | personal/sick/annual 路径有测；调休无专门测，不升冲突 |
| BEQR-185 | 无实现面（登记） | 测 pending/approved/rejected；撤销/销假基线既有缺口归 M054/U06-009，本段未加重 |
| BEQR-186 | FAIL | 封顶分母锁底薪+奖金（004）；结构化补贴/绩效无列，HTML 要求总薪资 |
| BEQR-187 | 通过（登记） | 提成进月结、个税测、无现金现抽入口 |
| BSM-001 | FAIL | 可见天数缺行空上下限被测成 7（005）；「未填上下限不得上线」 |
| BSM-003 | 通过（登记） | 软删/先存档/占用拦/最高权限；master 自批见观察（细则在未映射 BSM-004） |
| BSM-009 | 通过（登记） | 员工/权限/设备测试承载基础管理 |
| BSM-028 | FAIL | 自营测例走独立布尔而非六类码（002） |
| BSM-029 | FAIL | 私人手机绑定测存在，自助注册无设备标识（007） |
| BSM-073 | 无实现面 | 商户专区页面不在本段 |
| BSM-084 | 通过（登记） | 自助排班/考勤/薪资/通知只见本人 |
| BSM-085 | FAIL | 两条路径有测；自营视同公司自有锁在已删布尔（002） |
| BSM-086 | FAIL | 审批/锁定/跨月有测；可见天数/事假基数/顶班缺口（003/005；顶班观察） |
| BSM-087 | FAIL | 分栏/三审有测；20% 分母错（004） |
| BSM-088 | 通过（登记） | 提成进月结 |
| BSM-114 | 通过（登记） | 商户/公司/项目隔离多测 |
| BSM-115 | 通过（登记） | 员工连主体/权限/设备 |
| BSM-120 | FAIL | 寄存柜扫码有测但双身份（006）；设备标识拍板被 007 打穿 |
| PGM-001 | 通过（登记） | 底座资格/权限 |
| PGM-008 | 通过（登记） | 注册/手机/岗位状态有测 |
| PGM-022 | 通过（登记） | 打卡带 space/tagRef |
| PGM-024 | 通过（登记） | 班次点位 |
| PGM-026 | FAIL | 六类合作 vs 自营布尔（002） |
| PGM-032 | FAIL | 人机对应有测；自助注册无设备标识（007） |
| PGM-033 | FAIL | 同 007/006 |
| PGM-034 | 无实现面 | 企微会话不在本段测试 |
| PGM-035 | 通过（登记） | 提成月结 |
| PGM-036 | 通过（登记） | 工作手机 bound + staffId |
| PGM-048 | 无实现面 | 商户侧页面不在本段 |
| PGM-059 | 通过（登记） | 加密+掩码；公开回执无 phone |
| PGM-066 | 通过（登记） | 报错中文为主；内部码英文 |

## 4. Findings（临时 ID，B2 分配正式号）

### GR-U11-001 — 只合本段无法叠当前 main（编译/V410 列/latest 断言）

- **文件:行号**：`M010RegressionTests.java:334`（`parent_entity_id`）；`StaffJdbcIntegrationTests.java:104`（`MigrationVersions.latest()`）；`StaffScopeTests.java:39`（`StaffApprovalService`）；`M010RegressionTests.java:27,38`（`StaffLockerService`/`StaffRegistrationService`）；发布面为整段 8 文件
- **完整原文（摘）**：
  - `:334` `jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,parent_entity_id,status,created_at,updated_at) VALUES(?,'回归娱乐平台','management_company','m010_p','active',now(),now())",company);`
  - `:104` `assertThat(currentFlywayVersion()).isEqualTo(MigrationVersions.latest());`
  - HEAD `V410:9` `ALTER TABLE business_entities DROP COLUMN IF EXISTS parent_entity_id;`
- **业务影响**：按现文件合进 445a49f4，首轮编译或跑库即红；整树覆盖另回退 M009–M012（他段已证）。不是 HTML 新规则。
- **可复现**：
  1. `/tmp/u11_grok_main_plus_u11tests` = HEAD backend + 仅 8 个 U11 测试 → `JAVA_HOME=temurin-21 mvn -o -DskipTests test-compile` **EXIT 1**，106 条文件级 ERROR（4 文件）。
  2. docker exec 只读 `complex_test`：flyway max **410**；`parent_entity_id` 列 **不存在**；`staff_policy_settings` **无表**。
  3. 对照：未发布 overlay `/tmp/u11_grok_backend` 同命令 **EXIT 0**。
- **冻结判据**：GEN-002、GEN-008、GEN-013、GEN-014
- **2.3 依据类型**：4（命令）+ 2（可执行编译）
- **四分类**：**技术性**
- **无业务发明理由**：编号/已删列/未发布类型是发布叠法，不改六类合作或打卡闭集。
- **工程缺陷**：是。建议登记：是（同根 U05-008 / U06-007 / U10-001）
- **最小处置（发布者）**：以 main 445a49f4 为底重放本段；`parent_entity_id` INSERT 改为 V410 后的主体拓扑（三角色、无父列）；`latest()` 随 ≥V411 重编号；与 U05+U06+U07+U09+U10+U12 同一 PR；禁止整树覆盖。

### GR-U11-002 — 测例把已答删除的独立自营布尔锁成绿

- **文件:行号**：`M010RegressionTests.java:321-330`
- **完整原文**：
```
@Test void selfOperatedMerchantUsesCompanySelfRegistrationAndFlagSurvivesArchive() {
    var merchant = platform.findMerchantById(registrationMerchant).orElseThrow();
    platform.saveMerchant(merchant.withSelfOperated(true).withArchive(...).withArchive(null,null,...));
    assertThat(platform.findMerchantById(registrationMerchant).orElseThrow().selfOperated()).isTrue();
    ...
    var receipt = registrations.selfRegister(new StaffSelfRegistrationRequest(...,registrationMerchant,...));
    assertThat(...registrationSource()).isEqualTo("company_self");
}
```
- **业务影响**：HTML BSM L701/PGM L495 六类含「自营」，BSM L1433/PGM L578「标注为自营视同公司自有」落在 `cooperation_form=='self_operated'`。操作员已答删除独立布尔。本测例是该发明的可失败锁。
- **可复现**：main+U11 编译 `cannot find symbol method selfOperated()`；U05 `StaffRegistrationService:66` 读 `merchant.selfOperated()`。
- **冻结判据**：GEN-001、GEN-008、GEN-017、BSM-028、BSM-085、PGM-026
- **2.3 依据类型**：1（HTML）+ 4（编译）
- **四分类**：**添加**（操作员已答删除，引用 `questions/U04-001.md`，**不再问**）
- **工程缺陷**：是。建议登记：是（同根 U04-001 / U05-001 / U08-001 / U10-002）
- **最小处置**：删除本测例对 `withSelfOperated`/`selfOperated()` 的依赖；改断言 `cooperationForm=="self_operated"` 走公司自有路径（随 U05-001）。

### GR-U11-003 — 事假扣薪期望把基数锁成固定工资

- **文件:行号**：`M010RegressionTests.java:637-655`
- **完整原文（断言）**：
  - 月薪 10000，一天 9h 班×2，请一天 → `leave_deduction_cent ... isEqualTo(5000L)`
  - 全月事假 → `total_cent ... isZero()`
- **业务影响**：HTML BSM L1450「从其当期**应发薪资**中扣减」。U05 `JdbcStaffRepository:210-212` 只乘 `monthly_salary_cent`。本断言把错误公式锁绿。同文件 `:657-675` 同期还有 bonus/commission，不进入本测的应发。
- **可复现**：隔离库本测 PASS（本席 43/43 含此条）。对照 HTML L1450 与 U05 `:210`。
- **冻结判据**：BSM-086、BSM-120、GEN-002、GEN-003
- **2.3 依据类型**：1 + 2（92/92 中该条绿）
- **四分类**：**冲突**
- **工程缺陷**：是。建议登记：是（同根 U05-002；处置含「同批改第 11 段两处期望值」）
- **最小处置**：基数改为当期应发（固定+提成+奖金等应发项，个税/处罚不入）；同步改 5000/0 期望；工时比例与零下限保留。

### GR-U11-004 — 处罚封顶期望把「总薪资」锁成底薪+奖金

- **文件:行号**：`M010RegressionTests.java:657-675`
- **完整原文（断言）**：月薪 10000 + bonus 1000 + 两笔 penalty 3000 + 提成 9000 + 半天事假 → `penalty_cent isEqualTo(2200L)`、`total_cent isEqualTo(12800L)`。2200＝20%×(10000+1000)。
- **业务影响**：HTML BEQR L283 / BSM L1458「总薪资指基础工资、各项补贴、绩效工资与奖金之和」；提成排除、事假另栏——这两点测试做对了。分母写成底薪+奖金，注释/实现 U05 `:195-206` 同错。
- **可复现**：隔离库该条 PASS。U05 `:204-206` `LEAST(penalty, ROUND((monthly_salary_cent+bonus)*cap/10000))`。
- **冻结判据**：BEQR-186、BSM-087、GEN-003、GEN-008
- **2.3 依据类型**：1 + 2
- **四分类**：**冲突**
- **工程缺陷**：是。建议登记：是（同根 U05-004）
- **最小处置**：随 U05-004 改分母；现有列先取全（固定+奖金），补贴/绩效列落地后并入；本测期望随公式改；注释不得称底薪+奖金为总薪资。

### GR-U11-005 — 可见天数测例用 Java 默认 7 掩盖缺出厂行

- **文件:行号**：`M010RegressionTests.java:203-228`；对照 U05 `JdbcStaffRepository.java:61-67`；U10 `V402:16-17`
- **完整原文**：
  - `:212` `assertThat(service.listMyShifts().items()).hasSize(7);`（此时策略表无该商户行）
  - `:213-215` 才 INSERT `shift_visible_days` 测试行
  - `:227` 另一雇主 `getShiftVisibility().value() isEqualTo(7)`
- **业务影响**：HTML BSM L1444「出厂默认 7 天，**须同时配置上下限**」；BSM-001「未填写上下限的参数不得上线」。U05 缺行返回 `(7,null,null)`；V402 只种子处罚上限。本测把「缺行=7 且绿」写成合同。
- **可复现**：隔离库该两条 PASS。只读 `complex_test` 无 `staff_policy_settings` 表。U10-003 已定稿。
- **冻结判据**：BSM-001、BSM-086、GEN-003、GEN-008
- **2.3 依据类型**：1 + 2 + 4
- **四分类**：**冲突**
- **工程缺陷**：是。建议登记：是（同根 U05-005 / U10-003）
- **最小处置**：出厂行由发布者写入 7+批准上下限，或缺行 fail-closed；本测改为读到种子行的 min/max，禁止靠 Java 默认 7 绿。

### GR-U11-006 — 寄存柜测例同时注入员工会话与已验签柜机

- **文件:行号**：`M010RegressionTests.java:94-125`；对照 U05 `StaffLockerService.java:40-52`
- **完整原文**：`owner(a)` 后无柜机 → 抛错；再 `terminals.save(kiosk)` + `RequestContextHolder` 设 `verified_terminal=kiosk` + 仍保持员工 SecurityContext → `lockers.record` 成功。
- **业务影响**：HTML PGM L582 / BSM L938「由柜子自动记录，不设手工登记」。生产 76 文件无单端调用方。测试是唯一「双身份」客户端。
- **可复现**：隔离库该条 PASS。`StaffLockerService:41` `requirePermission("staff.profile.self")` 且 `:46-47` 必须 kiosk 已验签。
- **冻结判据**：BSM-120、PGM-033、PGM-036、GEN-003、GEN-004
- **2.3 依据类型**：1 + 2
- **四分类**：**技术性**
- **无业务发明理由**：不改「扫码留痕、不手工登记」；只改身份由谁承载。
- **工程缺陷**：是。建议登记：是（同根 U05-006）
- **最小处置**：随 U05-006 改单端契约后，补「仅柜机签名+扫码载荷」可失败用例；删除必须同时持有员工会话+柜机验签才能绿的写法。

### GR-U11-007 — 自助注册 HTTP/服务测把「无设备证明的公开提交」锁成绿

- **文件:行号**：`M010RegistrationHttpTests.java:22-29`；`M010RegressionTests.java:306-319`；`StaffJdbcIntegrationTests.java:323-325`
- **完整原文**：
  - HTTP：无认证 POST body 仅 idempotency/company/name/phone → 200 `submitted`
  - 服务：`new StaffSelfRegistrationRequest(key(),"m010_p",null,"本人申请","13800138000","110101199001011234")` 无公钥/指纹
- **业务影响**：HTML PGM L579 / BSM L935「安装时生成并绑定该手机专属设备标识」。U06 DTO 六字段、U07 permitAll。首轮正常客人/员工安装路径就会打到这个入口。
- **可复现**：隔离库 HTTP 2/2 与 Regression 自助两条 PASS。
- **冻结判据**：PGM-032、PGM-033、BSM-120、BSM-029、GEN-003、GEN-004、GEN-010
- **2.3 依据类型**：1 + 2
- **四分类**：**冲突**
- **工程缺陷**：是。建议登记：是（同根 U05-003 / U06-002 / U07-004）
- **最小处置**：随 U05-003 增加设备公钥/指纹后，本段改为：无指纹 4xx；有指纹才 pending。公开公司列表是否保留见 U05-012 OBS-4（不单开 2.2）。

### GR-U11-008 — 主回归 27 处只断言 ApiException 类型

- **文件:行号**：`M010RegressionTests.java` 27 处 `isInstanceOf(ApiException.class)`（本席计数：plain=27，`isInstanceOfSatisfying`=0，`hasMessageContaining`=0，`.code()`=0）。代表行：`:241` 排班自批、`:264` 已开始改班、`:275/:289` 处罚上限、`:353` 注册自审。
- **业务影响**：绿测不能区分 `SHIFT_STARTED` / `MASTER_APPROVAL_REQUIRED` / `POLICY_CHANGED` / 校验失败。U09-004 已要求 U11 补错误码。不是新业务规则。
- **可复现**：`grep -c 'isInstanceOf(ApiException.class)'` = 27。隔离库这些抛错条仍 PASS。
- **冻结判据**：GEN-003、GEN-002
- **2.3 依据类型**：4
- **四分类**：**技术性**
- **无业务发明理由**：只加强断言，不改审批/打卡/薪资口径。
- **工程缺陷**：是。建议登记：是（同根 U09-004）
- **最小处置**：至少对排班自批、已开始锁定、处罚三审未齐、注册自审补 `e.code()`（或中文消息）可失败断言。

## 5. 观察（无新根因，完整登记，建议队列「否」）

1. 本段 5 个既有文件 BASE blob == HEAD blob；M009–M012 未改这些路径。同名覆盖不回退那些检查点**在本文件内**的修复。
2. 整棵 76 文件树覆盖仍回退他段 M009–M012（U03/U08）。发布禁令继续有效。
3. 未发布 overlay 编译 EXIT 0；隔离空库 387 迁移 → V409 → **92/92 PASS** → DROP；`complex_test` 仍 410。92/92 ≠ HTML 通过。
4. 第一次用 docker `POSTGRES_PASSWORD`（长度 15）跑隔离测：TCP 认证失败，85 error / 7 单测绿。改用只导出三个 `COMPLEX_DATABASE_*`（密码长度 48）后 92/92。不把第一次失败算业务 finding。
5. 硬编码 `jdbc:.../complex_test`：按现文件在开发机直跑会写共享库。main 既有同模式。本席未对 `complex_test*` 写。
6. `StaffScopeTests` 同项目通过路径死桩 `repo.upsertShift`（U05-012 观察 19）。拒绝路径仍有效。
7. `v210` punch `no_shift`：BEQR-182 是日结果闭集；punch 层 main 既有。未发布把 orphan 从 `manual` 改成 `nfc`+工作手机，未新发明第六日态。
8. `punchFarOutsideAnyShiftFallsBackToNearestShift` 提前 6 小时判 normal：main 既有断言，未发布未加重。
9. `masterOwnDeletionDoesNotRequireAnotherApprovalStep` + U06 `:388-391` master 申请即自批。HTML 删除「申请人不能审批自己的申请」写在 **BSM-004**（本模块未映射）。不升本段冲突；登记给调度。ArchiveDelete 普通申请人拒批仍有效。
10. 无顶班记录测例。冲突在 U06-004 实现；本段 GEN-003 缺口，不单开冲突。
11. `StaffTestActors.grant` 一次授全部 `staff.%`。夹具过宽，非生产。
12. 存储/JSON 英文码（`submitted`/`nfc`/`pending_entry`）属系统内部标识符；界面中文不在本段前端文件。
13. `seedProfile` 继续写 `phone_hash`/`id_doc_hash`（U10-004 旧列保留）。
14. `M010-xxx` 自编号与审计模块 M010 撞名（U05-012 / U10-004）。
15. 请假撤销/销假无测：main 既有缺口 U06-009 归 M054。
16. PGM-034 企微、BSM-073/PGM-048 商户专区：本段无实现面。
17. 本席不采信席甲 `u11_scratch` 92/92 为己证；上列 92/92 是本席 `u11_grok_probe` 独立实跑。
18. 未发布 `StaffService` 主构造与 main 同为 6 参 ObjectProvider；Clock 重载与 `@Autowired staffApprovals` 为字段注入。单测必须反射设字段。

## 6. 命令证据（只读 / `/tmp` 隔离）

| ID | 命令 | exit | 关键输出 |
|---|---|---:|---|
| CMD-LOCK | `sha256sum docs/audit-2026-09/plan/criteria-lock.md` | 0 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| CMD-HEAD | `git rev-parse HEAD origin/main` | 0 | 均为 `445a49f409f0cfbe6dd616ae079be1299824c48f` |
| CMD-OBJ | `git cat-file -t` 三对象 | 0/128 | commit 无；tree；commit |
| CMD-STAT76 | `git diff --shortstat 242f4872 e28dad05` | 0 | 76 files, +4745/−1084 |
| CMD-STAT8 | `git diff --stat 242f4872 e28dad05 -- <8>` | 0 | 8 files, +890/−102 |
| CMD-HASH8 | 8 文件 sha256/wc | 0 | 8/8 MATCH 清单；8/8 COPY==TREE |
| CMD-BLOB | BASE/HEAD/TREE blob | 0 | 5 既有 BASE==HEAD≠TREE；3 新增 HEAD MISSING |
| CMD-HTML | 正本 34 锚点行 | 0 | 34/34 HIT |
| CMD-DBRO | docker exec 只读 complex_test | 0 | flyway 410；无 parent_entity_id；无 staff_policy_settings |
| CMD-CC-U | `/tmp/u11_grok_backend` `mvn -o -DskipTests test-compile` JAVA_HOME=temurin-21 | 0 | BUILD SUCCESS，1196+269 源 |
| CMD-UT | `mvn -o -Dtest=StaffScopeTests,StaffAttendanceLocalZoneTests test` | 0 | 7 run / 0 fail |
| CMD-CC-M | `/tmp/u11_grok_main_plus_u11tests` 同 test-compile | 1 | BUILD FAILURE；106 条文件级 ERROR |
| CMD-ISO | 空库 `u11_grok_probe`（非 complex_test*）+ 只导出三 DB 变量 + 5 测类 URL 改指向隔离库 | 0 | Flyway 387 validated，迁到 **409**；**Tests run: 92, Failures: 0, Errors: 0**；`DROP DATABASE`；complex_test 仍 410 |
| CMD-ISO0 | 同隔离库但误用 docker 15 字符口令 | 1 | 85 error（TCP 认证失败）+ 7 单测绿；随后 DROP。不计入业务 finding |

未跑：不对 `complex_test` 跑本段 SpringBootTest；不跑会清理共享库的脚本。

## 7. 审计结论

**FAIL**

闭合条件核对：

- 8/8 文件全文覆盖（942 行按结构段）且有三线记录
- 52/52 冻结判据逐项有结论
- 三线均执行（grilling 自答、顺从式表、对抗式分叉+反例）
- 声称均有类型 1/2/4 证据；隔离 92/92 不升 HTML 通过
- 无未答 2.2（添加项引用已答 U04-001）
- 无有条件通过
- 锁哈希行有效且独占第一行
- 存在须修缺陷 GR-U11-001…008 → 单席结论 FAIL

发布者最小动作：U05–U12 同一 PR、以 main 为底、迁移 ≥V411、按上表改断言与夹具、禁止整树覆盖。本轮不修源。

## U11 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 49927 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录「未发布材料第 11 段：员工回归测试」（8 文件 2247 行，main 主归属 M056；适用判据＝M056 映射行并集 52 项）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论。
- 本席 B2-r1：**PASS**（001–009 逐 ID 全 PASS；添加 1 已答删除、不发新 2.2；发布前须修「是」=001–008，「否」=009）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。判据总数 401；M056 映射行 L6435 共 52 ID，MISSING=[] EXTRA=[]；**BSM-004 不在本段映射**。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（输入哈希、blob、numstat、副本 8 测试原文、main V410:9、HTML L1450/L1458/L1444/L521/L935/L579/L938、副本仓储/柜机/删除路径、`questions/U04-001.md` 答复首行「删除」、已定稿 U01/U05/U06/U09/U10 同行）；未重跑隔离库、未装依赖、未碰 `complex_test*`。追加稿仅经 `/tmp/u11-b2-append.md` 中转，落盘后删除。

此票只判断 findings.md V1.64 顶部「当前 U11 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U11-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 49927 | `7803e95672af00d12be0d6eeed9293fb508765ab8bd0c33fb1355a36f1dca2e8` |
| `docs/audit-2026-09/modules/U11-b1-self-raw.md` | V0.01 | 26783 | `16b34ffb0571ec4cc22f6b035bc3f9dd4f9c07803d69c70855d1501a3ef8d79b` |
| `docs/audit-2026-09/findings.md` | V1.64 | 638189 | `30ed0249d843e175ac15f7964b42a371e72f133a2c06891088da866e1db3c39b` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.74 | 501484 | `6705fd93313f31d9da13863e0a1a0c9bad3b506443049fae95c63d82a5052d6e` |
| `docs/audit-2026-09/questions/U04-001.md`（只读，沿用） | V0.01 | 2282 | `5a9e9dab904fe9b82ccb2e414ae07515ea7192c7ed404685c31828a126c2b3d1`；答复首行精确「删除」 |

候选形态：V1.64 顶部 U11-001…009；questions 0（引用 U04-001 已答删除，不新开 2.2）；添加 1 入队；无 HTML 修改申请。U11 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希与 B1 一致：BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`、BSM `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa`、PGM `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c`。副本本段 8/8 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算）。`git diff --stat 242f4872 e28dad05 --` 本 8 路径 = 8 files, +890/−102。全树 `git diff --shortstat 242f4872 e28dad05` = 76 files, +4745/−1084。5 个既有测试 BASE blob == HEAD blob；3 个新增 HEAD 无路径。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-arc / using-superpowers | 先核技能；用户/提示词覆盖技能本体；本节点为指定 B2 归并，不改走 Arc audit |
| receiving-code-review | 先核 HTML/测试原文/V410/已定稿 U01–U10 再接受或反驳候选单元格；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：(a) 四分类与 GEN-017/U05 同根、(b) 001 叠法是否一条、(c) 009 第 9/10/15 条是否写错原意、(d) 92/92 表述 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/git cat-file/diff/原文行号；本票不声称本轮新迁库或 92/92 复跑 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（本段测试 → U05/U06/U09/U10 副本 → main V410 → HTML）；本轮不建隔离库 |
| ponytail-review | 只评最小删除/收窄，不落地。002 只去布尔；001 只改已删列写法；003/004 只改期望随公式 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

内部 grilling（自答，未问操作员）：

1. 席甲把测试面记技术性、候选改冲突，是否把业务发明伪装回去？否。见 (a)。
2. `:334` / `:104` / 新类型编译红能否并成一条叠法？能。见 (b)。
3. 第 9 条写成「归 U06/主流程」是否把「登记给调度」改成已定稿 U06-xxx？未并入 U06-004。见 (c)。
4. 两席 92/92 能否写成 HTML 通过或互相采信？不能。见 (d)。

### 完整差集（GR-001…008、§5 观察 1–18、席甲 CX-001…003、OBS-1…8）

漏并：无。两席 B1 全部临时 ID / 观察仍落在 001–009。未把本席 finding 降观察。席甲 OBS-8 由候选升队，与本席 GR-U11-005 同向，不改写本席原意。席甲独有项无超出 CX-001…003 与 OBS-1…8。席甲 B1 漏判四处（`:334` 对 V410 已删列、封顶分母、柜机双身份、27 处无错误码）并把测试面一律记技术性——候选已勘误接受本席并按 GEN-017 改记冲突，本席独立复核后维持。

| 临时 ID | 落入正式 ID | 是否改写本席原意 |
|---|---|---|
| GR-U11-001 | U11-001 | 否：技术性叠法；重放、无父列、`latest()` 随 ≥V411、同 PR、禁整树 |
| GR-U11-002 | U11-002 | 否：添加已答删除；去 `withSelfOperated`/`selfOperated()`；改 `cooperationForm`；不再问 |
| GR-U11-003 | U11-003 | 否：冲突；事假基数＝当期应发；同批改 `:637-655` 期望 |
| GR-U11-004 | U11-004 | 否：冲突；封顶分母按总薪资；同根 U05-004；席甲 CX-001 只记事假基数，本条独立 |
| GR-U11-005 | U11-005 | 否：冲突；禁止 Java 默认 7 绿；同根 U05-005/U10-003 |
| GR-U11-006 | U11-006 | 否：技术性；柜机双身份；同根 U05-006 |
| GR-U11-007 | U11-007 | 否：冲突；无设备证明的公开提交；同根 U05-003/U06-002/U07-004。HTTP 自报类型头并入本条不改冲突结论（见 007 修正） |
| GR-U11-008 | U11-008 | 否：技术性；27 处只断言类型；同根 U09-004 |
| §5 1–18 | U11-009 | 「含」为闭集转述加调度去向注释；第 9/10/15 条未把观察升冲突，也未把 finding 降观察（见 (c)） |
| 席甲 CX-U11-001 | U11-003（事假）+ U11-004（封顶，勘误拆出） | 席甲捆在一条并以固定工资解释 12800；拆条与本席 003/004 同向 |
| 席甲 CX-U11-002 | U11-007 | 分类由技术性改冲突，与 GR-007 / U05-003 一致 |
| 席甲 CX-U11-003 | U11-002 | 分类由技术性改添加已答删除，与 GR-002 一致 |
| 席甲 OBS-8 | U11-005 | 升队勘误，不改写本席「冲突/须修」 |
| 席甲 OBS-1…7 | U11-009 | 维持观察 |
| 席甲 GEN-002/003 空库 92/92 | 不采信为 HTML 通过 | 与本席 §5.3/17 一致；候选未把 92/92 写成冲突已覆盖 |

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U11-001 | GR-U11-001 + 席甲 §4 勘误 | 技术性（发布叠法，同根 U05-008/U10-001）/是/是 | **PASS（技术性叠法/是/是；只引用既有重放、≥V411、同 PR）** |
| U11-002 | GR-U11-002 + 席甲 CX-U11-003 | 添加（操作员已答删除：questions/U04-001.md；同根 U04-001/U05-001/U08-001/U10-002）/是/是 | **PASS（添加已答删除/是/是；不再问）** |
| U11-003 | GR-U11-003 + 席甲 CX-U11-001 | 冲突（同根 U05-002）/是/是 | **PASS（冲突/是/是）** |
| U11-004 | GR-U11-004 + 席甲 CX-U11-001 勘误拆出 | 冲突（同根 U05-004）/是/是 | **PASS（冲突/是/是）** |
| U11-005 | GR-U11-005 + 席甲 OBS-8 升队 | 冲突（同根 U05-005/U10-003）/是/是 | **PASS（冲突/是/是）** |
| U11-006 | GR-U11-006（席甲漏） | 技术性（同根 U05-006）/是/是 | **PASS（技术性/是/是）** |
| U11-007 | GR-U11-007 + 席甲 CX-U11-002 | 冲突（同根 U05-003/U06-002/U07-004/U01-001/U02-001）/是/是 | **PASS（冲突/是/是；同根 ID 可补 U01-007/U02-002，不改票）** |
| U11-008 | GR-U11-008（席甲漏） | 技术性（同根 U09-004）/是/是 | **PASS（技术性/是/是）** |
| U11-009 | 双方观察归并 | 登记/否/否 | **PASS（登记/否/否；第 9/10/15 条未写错「不升本段冲突」）** |

技术性入队 3（001 叠法、006 柜机双身份、008 错误码）。添加入队 1（002，已答删除）。冲突入队 4（003 事假基数、004 封顶分母、005 可见天数默认、007 无设备证明注册）。登记观察 1（009）。questions 0。无 HTML 修改申请。

发布前须修「是」=001–008（8）；「否」=009（1）。与候选汇总一致。席甲 B1 票 FAIL 但漏 001/004/006/008 且测试面分类过低，属低估；候选接受本席八项须修并勘误，本席独立复核后维持。

### 重点 (a) 003/004/005/007＝冲突，001/006/008＝技术性，002＝添加已答删除；席甲测试面改冲突 — **PASS（与 GR-001…008 四分类一致；GEN-017 顺序与同根 U05 项一致）**

独立核，不采信席甲「按 GEN-017 改记冲突」六个字为己证。

GEN-017 原文：每项内容按 **冲突→细化→技术性→添加** 顺序分类；不得把业务发明伪装成技术实现。

本席 B1 四分类 vs 候选：

| GR | 本席四分类 | 候选 | 同根已定稿 |
|---|---|---|---|
| 001 | 技术性 | 技术性 | U05-008 / U10-001 技术性叠法 |
| 002 | 添加（已答删除） | 添加（已答删除） | U04-001 / U08-001 / U10-002 添加；U05-001 服务读路径记冲突（层不同，见下） |
| 003 | 冲突 | 冲突 | U05-002 冲突 |
| 004 | 冲突 | 冲突 | U05-004 冲突 |
| 005 | 冲突 | 冲突 | U05-005 / U10-003 冲突 |
| 006 | 技术性 | 技术性 | U05-006 技术性 |
| 007 | 冲突 | 冲突 | U05-003 / U06-002 / U07-004 冲突 |
| 008 | 技术性 | 技术性 | U09-004 技术性 |

席甲 B1 把 CX-001（事假基数断言）、CX-002（自报终端/匿名注册）、CX-003（自营布尔）一律记技术性。本轮原文：

- `:652-654` 月薪 10000、两班请一班 → `leave_deduction_cent=5000`；全月事假 `total_cent` 为零。HTML BSM L1450「从其当期**应发薪资**中扣减」。断言内容与 HTML 原句相反 → **冲突优先**，不得因「这是测试」降技术性。同根 U05-002 已是冲突。
- `:672` `penalty_cent=2200`＝20%×(10000+1000)。HTML BEQR L283 / BSM L1458 总薪资＝基础工资、各项补贴、绩效工资与奖金之和。同根 U05-004 冲突。席甲 CX-001 用「以固定工资为事假基数推得」把本条捆进事假项，漏了分母冲突；候选拆 004 成立。
- `:212` 无策略行时 `listMyShifts` 长度 7。HTML BSM L1444「出厂默认 7 天，**须同时配置上下限**」；L521 段末「未填写上下限的参数不得上线」。同根 U05-005 冲突。席甲 OBS-8 升队与本席 GR-005 同向。
- HTTP `:24` 无认证、`X-Terminal-Type: app`、body 无设备标识 → 200；服务 `:308` 六字段无公钥。HTML PGM L579 / BSM L935「安装时生成并绑定该手机专属的设备标识」。同根 U05-003 冲突。

002 不改冲突、维持**添加**：测例锁的是已答删除的独立布尔 API（`withSelfOperated`/`selfOperated()`），不是「六类码写错」。U05-001 把**服务读路径**记冲突（第二开关 vs 六类码），本段测例是该发明的可失败锁，与 U04-001/U08-001/U10-002 同层记添加。GEN-017「不得把业务发明伪装成技术实现」——席甲 CX-003 记技术性才是伪装。候选改添加、引用 `questions/U04-001.md` 答复「删除」、不再问：与 GR-002 一致。

001/006/008 保持技术性：编号/已删列/未发布类型是叠法；柜机双身份不改「扫码留痕、不手工登记」；补 `e.code()` 不改审批/打卡/薪资口径。与 U05-008/U05-006/U09-004 一致。无业务发明。

票 **PASS**。

### 重点 (b) U11-001 把 `:334` parent_entity_id、`:104` latest()、新类型编译红并为一条叠法 — **PASS（成立；处置只引用既有重放、≥V411、同 PR）**

独立核：

1. 副本 `M010RegressionTests.java:334` 原文：`INSERT INTO business_entities(id,entity_name,entity_role,parent_entity_id,status,created_at,updated_at) VALUES(?,'回归娱乐平台','management_company','m010_p','active',now(),now())`。
2. main `V410__m010_business_entity_topology_and_role_isolation.sql:9` 原文：`ALTER TABLE business_entities DROP COLUMN IF EXISTS parent_entity_id;`（`:8` 先 DROP 索引）。三角色白名单在其后，本 INSERT 的 `entity_role='management_company'` 已落在 V410 闭集内；失败点是**已删列**，不是角色码。
3. 副本 `StaffJdbcIntegrationTests.java:104`：`assertThat(currentFlywayVersion()).isEqualTo(MigrationVersions.latest());`。`latest()` 读类路径最大版本，不写死 409。未发布 classpath 最大 V409；HEAD 库已是 V410（本席 B1 只读）。空库迁到 V409 的 92/92 **看不见**该冲突——席甲 §4「重排后本段测试不需改动」对 `latest()` 半对、对 `:334` 错。候选勘误成立。
4. 新类型：`M010RegressionTests.java:27` `@Autowired StaffLockerService`、`:38` `@Autowired StaffRegistrationService`；`StaffScopeTests.java:39` `StaffApprovalService`。本轮 `backend/src/main/java` 无这三个类、无 `selfOperated()`。本席 B1 `/tmp/u11_grok_main_plus_u11tests` test-compile EXIT 1、106 条文件级 ERROR。本轮不重跑编译，源码符号面仍成立。
5. 5 个既有测试 BASE==HEAD≠TREE；3 个新增 HEAD 无路径。只合本段测试不会回退 M009–M012 **在这些文件内**的修复，但过不了编译/V410 库。整树覆盖仍回退他段（U03/U08 已证）。

三条失败形态不同（编译符号、已删列、latest 绑死），**同一发布动作**：以 main `445a49f4` 为底重放本段，并与 U05–U10/U12 同 PR、迁移 ≥V411。同根已定稿 U05-008 / U10-001 已是这条叠法。并成一条成立，不是把无关缺陷捆进「技术性」。

最小处置只引用既有，无业务发明：

- 以 main 为底重放（U05-008/U10-001/U00-000）。
- `:334` 去掉 `parent_entity_id`，按 V410 后的主体列集插入（三角色已在本 INSERT 内）。
- `latest()` 动态取类路径最大号，重编号 ≥V411 且库迁到新最大号后断言自然对齐，**不必**改成字面量 411。
- 与 U05+U06+U07+U09+U10+U12 同一 PR；禁止整树覆盖。

「重放 U05 后跑库 `:334` 仍红」成立：补上未发布类型只能过编译，过不了已删列。

票 **PASS**。

### 重点 (c) U11-009 第 9/10/15 条调度去向 — **PASS（未写错「不升本段冲突」；无应升队被降观察项）**

独立核本席 §5 原文，不采信候选转述为己证。

**第 9 条。** 本席原文：「`masterOwnDeletionDoesNotRequireAnotherApprovalStep` + U06 `:388-391` master 申请即自批。HTML 删除「申请人不能审批自己的申请」写在 **BSM-004**（本模块未映射）。不升本段冲突；登记给调度。ArchiveDelete 普通申请人拒批仍有效。」

本轮核：副本测试 `:538-548` master `requestDeleteProfile` 后 `deleteStatus==approved`；副本 `StaffService.java:388-391` `isMasterAuthority` 则立即 `approveDeleteProfile`。M056 映射无 BSM-004。铁律 6 / BSM-003 标题在映射内，细则与「最高权限本人发起不需他人审」在 BSM-004（已定稿 M009-009 引过该例外）。本段不把未映射 ID 写成新锁、不升冲突——候选仍放 009、须修否，**未写错原意**。

「登记给调度归 U06/主流程模块」是调度去向注释，本席原文只写「登记给调度」、**未**点名已定稿 U06-xxx。U06 已 REVIEWED-FAIL。**不得**读成已由 U06-004 吸收。U06-004 是顶班入口（`:636-637`），与 master 自删自批不是一条。因 U06 已收口，调度按未映射 BSM-004 另挂主流程/删除生命周期，不在本段升队。非阻断。

**第 10 条。** 本席原文：「无顶班记录测例。冲突在 U06-004 实现；本段 GEN-003 缺口，不单开冲突。」候选「归 U06-004」与原文一致。U06-004 已定稿冲突、须修是。本段不重复立项。

**第 15 条。** 本席原文：「请假撤销/销假无测：main 既有缺口 U06-009 归 M054。」候选「归 U06-009/M054」与原文一致。U06-009 已定稿「冲突（main 既有）、须修否（归 M054）」。沿用 U01「main 既有」口径：本材料未触及状态机，不在本段升须修。

**应升队而被降观察？无。** 本席 GR-001…008 全部入 001–008 且须修是；OBS-8 升 005 与 GR-005 同向。第 9 条本席 B1 即要求不升本段冲突。席甲 OBS-5（`:196-201` 只覆盖保留 `staff_id`）/OBS-6（存档权限放宽锁成期望，同根 U05-012 OBS-1）本席 B1 未立项，维持观察合法。

票 **PASS**。

### 重点 (d) 两席各自空库 92/92 互不采信、只证自洽不证 HTML 通过 — **PASS（候选表述准确）**

候选文首：「两席各自建一次性空库实跑本段 7 类 92/92（席甲 `u11_scratch`、Grok `u11_grok_probe`，均迁到 V409 后 DROP，complex_test 未写）——92/92 只证明测试与未发布实现自洽，不证明 HTML 通过。」009：「3/17 两席各自隔离库 92/92 互不采信」。

与本席 §5.3「92/92 ≠ HTML 通过」、§5.17「不采信席甲 `u11_scratch` 为己证」同向。席甲 GEN-002 把 92/92 写成判据通过，是把自洽当成 HTML 通过；候选未沿用该升格。本轮不重跑、不采信席甲日志为己证；空库到 V409 也看不到 V410 已删列（见 (b)）。

票 **PASS**。

### 001 独立裁定（技术性叠法／须修是）

见 (b)。工程缺陷是。发布前须修是。最小处置不发明业务。票 **PASS**。

### 002 独立裁定（添加已答删除／须修是；不再问）

见 (a)。副本 `:321-330` `withSelfOperated(true)` / `selfOperated()`；main 无该方法。`questions/U04-001.md` 答复首行精确「删除」。处置：去掉布尔依赖，商户 `cooperationForm` 置 `self_operated` 后走公司自有路径（随 U05-001）。不新开 2.2。票 **PASS**。

### 003 独立裁定（冲突／须修是；同根 U05-002）

见 (a)。`:637-655` 基数锁固定工资。副本 `JdbcStaffRepository.java:210-212` 只乘 `monthly_salary_cent`。HTML L1450 当期应发。工时比例与零下限保留。同批改期望。票 **PASS**。

### 004 独立裁定（冲突／须修是；同根 U05-004）

见 (a)。`:657-675` `2200`＝20%×(底薪+奖金)；`12800`＝10000+1000+9000−2200−5000，事假半班仍按固定工资。提成排除与事假另栏两点测试正确。分母随 U05-004；现有列先取固定+奖金，补贴/绩效列落地后并入；注释不得称底薪+奖金为总薪资。票 **PASS**。

### 005 独立裁定（冲突／须修是；同根 U05-005/U10-003）

见 (a)。`:212` 先断言 7，`:213-215` 才 INSERT `(2,1,14)`，`:227` 另一雇主再回落 7。副本仓储 `:61-67` 缺行 `(7,null,null)`。V402 只种子处罚上限（U10-003）。出厂行 7+发布者写入的上下限，或缺行 fail-closed；本测改为读到种子行 min/max 才绿。不新开 2.2、不代填上下限数字。票 **PASS**。

### 006 独立裁定（技术性／须修是；同根 U05-006）

`:94-125`：`owner(a)` 无柜机抛错；再 `terminals.save(kiosk)` + `verified_terminal=kiosk` 且保持员工会话后 `lockers.record` 成功。副本 `StaffLockerService.java:41` `requirePermission("staff.profile.self")`，`:46-47` 必须已验签 kiosk。76 文件无单端调用方。HTML PGM L582 / BSM L938 不改。随 U05-006 改单端契约后补「仅柜机签名+扫码载荷」可失败用例，删除双身份才绿的写法。票 **PASS**。

### 007 独立裁定（冲突／须修是；同根 U05-003）

HTTP `:22-29` 无认证、自报 `X-Terminal-Type: app`、body 仅 idempotency/company/name/phone → 200 `submitted`。服务 `:306-319` 与 `StaffJdbcIntegrationTests.java:323-325` 同无公钥/指纹。HTML PGM L579 / BSM L935。随 U05-003：无指纹 4xx、有指纹才 pending；HTTP 改发设备签名头而非自报类型（U01-001 面）。同 PR。同根 ID 以 U05-003/U06-002/U07-004/U01-007/U02-002 为无标识契约链；U01-001/U02-001 只覆盖类型头，处置已写「改发设备签名头」，不改分类/须修。票 **PASS**。

### 008 独立裁定（技术性／须修是；同根 U09-004）

本轮复算 `M010RegressionTests.java`：`isInstanceOf(ApiException.class)`＝**27**，`isInstanceOfSatisfying`＝0，`hasMessageContaining`＝0，`.code()`＝0。代表行 `:241` 排班自批、`:264` 已开始改班、`:275/:289` 处罚上限、`:353` 注册自审。U09-004 已要求 U11 补错误码。至少对上述四类补 `e.code()`（或中文消息）可失败断言。不改业务口径。票 **PASS**。

### 009 独立裁定（登记／须修否）

见 (c)(d)。双方观察归并，工程缺陷否，发布前须修否。硬编码 `complex_test` 按 U01「main 既有」口径记**同模式**（新文件不是 main 同位置），维持观察。票 **PASS**。

### 最小修正意见（非阻断，不改票）

1. U11-001 事实格 `:27,38` 定稿写全路径 `M010RegressionTests.java:27,38`，避免跟在 `StaffJdbcIntegrationTests.java:104` 后被误读。分类/处置/须修不变。
2. U11-002 冻结判据格可补本段映射内的 BSM-028、BSM-085、PGM-026（本席 GR-002 有；候选只留 GEN）。分类仍为添加已答删除。
3. U11-003 冻结判据里的 BSM-120 过宽（设备/柜机拍板；与 U05-002 同根照抄）。本条 HTML 锚是 BSM L1450 / BSM-086。定稿可删 BSM-120，不改冲突/须修。
4. U11-007 同根补 U01-007/U02-002（无设备标识契约）。U01-001/U02-001 只覆盖自报类型头；处置已含「改发设备签名头」，不得把 007 读成只改请求头。
5. U11-009 第 9 条「归 U06/主流程」不得读成已由 U06-004（顶班）吸收；U06 已 REVIEWED。调度按未映射 BSM-004 另挂删除生命周期。第 10/15 条去向正确。
6. 009 「含」对席甲 OBS-5/6 维持观察；本席 §5.4 误用 docker 口令、§5.18 构造/字段注入未单独点名，定稿不得当成「Grok 无这两条」或升格入队。

全批差集无漏并。四分类与 GR-001…008 一致；席甲测试面改冲突符合 GEN-017 与同根 U05。001 叠法成立且只引用既有门禁。009 未写错第 9/10/15 条原意，无应升队被降观察项。92/92 表述准确。逐 ID 001–009 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U11 或 main 模块 PASS。步骤仍 B2；U11 IN_PROGRESS；材料未发布，不做 B3。
