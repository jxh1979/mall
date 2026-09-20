CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M015 B4-r3 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r3
席位：固定 Grok 外审席乙（grok-4.6 / xhigh）
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁；不问人）
本文件路径：docs/audit-2026-09/modules/M015-b4-r3-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r3 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲为作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。本轮为该模块第 3 次 B3（上限）之后的 B4。

未读取：任何 `M015-b4-r3-self-raw.md`（落盘时该路径已存在，本席按独立性禁令整文件不打开）。已读：`160-audit-prompt.md` V0.17 B4 与 §5 闸 3、findings.md V2.51 顶部「当前 M015 B2-r3 双PASS定稿」、本席旧 `M015-b4-r2-grok-raw.md` 仅作落盘格式参照、冻结 diff 与两 tree。定稿队列以 findings.md V2.51 为准：本批修复队列「是」仅 **M015-015、M015-016**；001–014 保持「否」，本轮不重审其内容，只确认本 diff 未改它们且 GREEN 未回退。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；015/016 是否按定稿最小处置实现；是否最懒可行（ponytail ultra）；是否引入新缺陷或新业务发明；每一变更行能否归因到 015 或 016；是否越界改 AuthorizationService / Controller / OpenAPI / 三份 HTML / 锁 / sidecar / ledger / 迁移 / 权限种子 / 其它模块源码；测试是否先红后绿且断言有效。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例（isPlatformAdmin 与 requirePlatformScope 桥是否同谓词、桩改写后商户/项目员工是否回潮、已发布任务再发已发布候选是否改道放行、016 例 RED 是否打中旧文案、无 ID 行含版本头/注释），未冒充调用 grilling 子代理。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 正文一行同值。ledger 顶部基准同值。criteria-lock 版本仍 V0.11。本批 B3-r3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。sidecar blob 两树均为 `41f575026b693af74d2a36e6faf86d260ef6f6ca`。

| 项 | 值 |
|---|---|
| 模块 | M015 空间点位、楼层平面图与营业时段／后台实现／第1段 |
| 阶段 | B4-r3 只读审计 B3 第3次（上限）整批修复的「修复内容」本身 |
| 本轮修复队列 | 仅 015、016（B2-r3 定稿「是」）；001–014 本轮不重审内容 |
| B3 | 已执行 3 次（本轮为第 3 次，上限） |
| before（对象类型 tree，可解析） | `991f4df7a79773fd7a179cb0a0cb374ba345ea57` |
| after（对象类型 tree，可解析） | `3036d0c089905520aaa86fddb7a75eb75409b019` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 991f4df7a79773fd7a179cb0a0cb374ba345ea57 3036d0c089905520aaa86fddb7a75eb75409b019`（无路径过滤） |
| 实际差异 | 20661 bytes；SHA256 `865c853787ab36e4734305ba68523ea6ce924200bfe1e2a28296b828c7324fa5`；4 files changed, 147 insertions(+), 59 deletions(-)；17 hunk |
| 与冻结文件 `/home/james/audit-tmp/m015_b3_r3.diff` | 字节、SHA256、内容全等（本席自算 live SHA 同值；python 比较 `equal True`） |
| `git diff --check` | exit 0 |
| `git diff --name-status` | 4 处 `M`，无 A/D/R |
| 两树路径清单 | 无增删路径（`comm -3` 空；本席按 name-status 4M 闭集复核） |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`（工作区 after 四文件 blob 与 after tree 全等，修复已在工作区、未入主索引提交） |
| findings.md / ledger.md | 两树 blob 相同（本 diff 未改）：findings `3cd81adfa2eac568d71f4548c19f0dbe6d3ffee1`，ledger `3b2115cde53ffb4fbb858c03931ca07b17ae129d` |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3`，完全相同；工作区 `git hash-object` 同值 |
| OpenAPI | 两树 blob 相同：yaml `1f056b360dc60c37b6c026c761f0fa57b3688e63` / json `32231790707d8a93a303ca47033b7819ee0e3b71` |
| AuthorizationService | 两树 blob 相同：`4bfae6d2ddded7e2810666bac72bfce818ff9e22` |
| FloorPlanController | 两树 blob 相同：`723f582bf253a1e34a172a568bd26bce5804df1f` |
| PlatformController（营业时段入口） | 两树 blob 相同：`52d3f0d27191d6fd4cc923a1607a9263bc3eb7fa` |
| FieldLabels | 两树 blob 相同：`51d75c79e8f1bfb40e3c67d5ac47e5c094b42f78` |
| 迁移 | `git diff --name-only ... -- backend/src/main/resources/db/migration` 空 |

四文件 blob（工作区 `git hash-object` = after tree）：

| 文件 | before | after / work |
|---|---|---|
| `backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java` | `1a08caed13819272c42a9eb1c6d90edd6733c12f` | `fc17314f95b90500be1ba4a8c9ac538b30d35444` |
| `backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java` | `2192b92b9ecd7fed3e918a226b6c9277e1a398a4` | `d61b8496c2a83d429a258dae887a221ac2f1da77` |
| `backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java` | `9f3fb0ab42d5eb82824e6d8d04b5a90f36fb7e8f` | `a96b9f656bbb6886faea6217a5153bc9ac8b0101` |
| `backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java` | `302a5cdc1730c61f12c3c2ad644e0411c406610a` | `bc51adb9c65fe251306b77f83f780f948e1d0aa2` |

版本头与派发一致：FloorPlanService V0.03→V0.04；VenueBusinessHoursService V0.01→V0.02；FloorPlanServiceTests V0.04→V0.05；VenueBusinessHoursServiceTests V0.02→V0.03。

本席未改任何源文件，只新建本 raw。验证：主仓只读 mvn 测试 + `git show`/`git diff`；未 commit/push/PR/merge/部署；未对主索引 `git add`；未 source `/etc/complex/complex.env`；未连生产库（本席复跑 URL 库名为 `complex_test`；Flyway `Current version of schema "public": 427` / `No migration necessary`）。误写过 `/tmp/.m015_b4r3_env_check` 空文件，当即删除，无残留。

---

## 2. 锁、分类与本轮队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M015 的集合。本轮只用定稿 015/016 已引用的 ID。不 LOCK-AMEND。四分类本轮不重开。

B2-r3 定稿（findings.md V2.51）：

- **M015-015** 细化／工程缺陷是／队列是。最小处置：两服务各加私有 `projectScopeOrAll()`：`authorizationService.isPlatformAdmin(authorizationService.currentActor())` 为真 → `Optional.empty()`（全量，与 `requirePlatformScope` 的平台管理员桥同口径），否则 `currentProjectScopeQuietly()`（无归属员工继续 fail-closed 抛 `PROJECT_SCOPE_REQUIRED`）；五处调用改用该方法；两测试类桩改为生产语义并各加一例先红；不改 `AuthorizationService`、Controller、OpenAPI、HTML。
- **M015-016** 细化／工程缺陷是／队列是。最小处置：发布前置改为任务状态 ∈ {parsed, published} 且所选候选均 pending（`failed`/`cancelled` 仍拒）；文案「只有解析完成或已发布的导入任务才能继续发布」；加一例先红（先发 1 个候选、再发剩余 1 个成功，任务仍 published、图形共 2 个、剩余候选 published）；不改 DTO/OpenAPI/HTML、不新增候选状态。

派发补充（与定稿同方向、更具体）：测试桩只替换 `currentActor`（AccessActor），让 `currentMerchantIdQuietly` / `currentProjectScopeQuietly` / `isPlatformAdmin` 走真实现。本席按定稿+该补充核实现。

---

## 3. 技能预检与本步收窄口径

本席环境实核（列出目录 / 读 SKILL.md；缺则记缺口，不冒充已调用）：

| 技能 | 存在 | 本步用法 |
|---|---|---|
| using-superpowers | 是 | 开工技能路由；用户 160 提示词优先于技能本体 |
| using-arc | 是 | 用户指令优先；本步是 160 B4 只读，不走 Arc audit 子代理 |
| graft | 是 | `graft grep projectScopeOrAll` / `isPlatformAdmin`（五处调用闭集、桥谓词落点） |
| long-running-background-tasks | 是 | 已读；mvn 在本席命令内跑完，未另挂 monitor |
| grilling / grill-me | 是 | 只借追问结构自答；本体「等用户」「dispatch sub-agent」作废 |
| batch-grill-me | **缺口**（本环境无此技能目录） | 未冒充 |
| ubiquitous-language | **缺口** | 未冒充 |
| ponytail（ultra）+ ponytail-review | 是 | 核最小处置；结论 Lean already（两服务各加私有 helper 为定稿要求，不是多余抽象） |
| systematic-debugging | 是 | 只借读错误→复现→追数据流；不插桩、不改生产 |
| test-driven-development / tdd | 是 | 核 RED 日志失败原因与 GREEN 断言；测缝确认按铁律 5 不作废询问 |
| requesting-code-review | 是 | B4 请审=本席；不派子代理、不用 HEAD~1 |
| receiving-code-review | 是 | 本席是外审否决席，不 IMPLEMENT |
| verification-before-completion | 是 | PASS 前主仓复跑 mvn，计数以 TEST-*.xml 为准 |
| diagnosing-bugs | 是 | RED 日志作 tight loop；不进入修复 |
| codebase-design | 是 | 私有 helper 为模块内缝，未抬成跨服务接口 |
| writing-for-agents | 是 | 本 raw 给席甲合取 / 下游 B5 读 |
| git-guardrails-claude-code | 是（可选） | 本步未装 hook；等价约定：不 fetch/commit/push/merge/hard reset |
| i-have-adhd | 是 | CLI 按 V0.17 适配器收窄，不套其开场白 |
| resolving-merge-conflicts | 是 | 本步无冲突，未用本体流程 |
| brainstorming / writing-plans | 是 | 阶段 A 技能，B4 不适用 |
| 划掉的 skills（worktrees / 并行子代理 / executing-plans / research 等） | — | 按提示词 §6 不用 |

对抗式自问（grilling 结构，本席自答，不等操作员）：

1. `isPlatformAdmin` 是否就是 `requirePlatformScope` 的平台管理员桥？是：`isPlatformAdmin` 直接 `return isPlatformAdminBridge(actor)`；`requirePlatformScope` 首支同样 `if (isPlatformAdminBridge(actor)) return;`。谓词：`active` + `accountType` 规范化后为 `platform_admin`。
2. 五处是否全改、有无漏网 `currentProjectScopeQuietly` 当全量？生产路径已无：FloorPlanService 仅 helper 体内再调 quietly；Venue 同。graft 9 hits / 7 symbols / 2 files，与定稿五处+两 helper 闭集一致。
3. 商户员工是否被 `projectScopeOrAll` 误伤？列表/创建/所有权：商户支仍先 `currentMerchantIdQuietly()`，有商户号则不进项目 helper。RED 中 `accessingFloorPlanBelongingToDifferentMerchantIsForbidden` 与 `createFloorPlanWithSameKeyAcrossMerchantsDoesNotOverwrite` 未红（只替换 currentActor 后真 quietly 对商户员工仍抛项目 scope，但商户路径不调用它）。
4. 已发布任务再发**已发布**候选会不会因任务状态放行而成功？不会：候选环仍要求 `pending`，否则 `VALIDATION_FAILED`「只有待处理的候选项才能发布」。`publishImportTaskRejectsAlreadyPublishedTask` GREEN 仍过（只断言 code，文案由源码锁定）。
5. 有无归因不到 015/016 的行（含版本头/注释）？无。版本头随该文件本批所属 ID；注释均点名 015 或 016。

---

## 4. diff 逐块归因（闸 3）

闭集：4 文件、17 hunk、+147/−59。每一变更行（含上下文外的 `+/-` 行、版本头、注释、import、测试桩）均归因到本批队列「是」的 ID。无第三 ID，无「顺手优化」。

| # | 文件 | hunk | +/- | ID | 说明 |
|---|---|---|---|---|---|
| 01 | FloorPlanService.java | `@@ -1,5 +1,5 @@` | +1/−1 | 015+016 | 版本 V0.03→V0.04。本文件本批同时承载 015 与 016 |
| 02 | FloorPlanService.java | `@@ -72,9 +72,10 @@` | +2/−1 | 015 | 列表：M015-015 注释；`currentProjectScopeQuietly()`→`projectScopeOrAll()`。保留 M015-003 商户优先句 |
| 03 | FloorPlanService.java | `@@ -362,8 +363,9 @@` | +3/−2 | 016 | 发布前置 parsed∨published；文案改为「只有解析完成或已发布的导入任务才能继续发布」；注释写明 failed/cancelled 仍拒 |
| 04 | FloorPlanService.java | `@@ -510,7 +512,7 @@` | +1/−1 | 015 | 创建项目校验改 `projectScopeOrAll()` |
| 05 | FloorPlanService.java | `@@ -603,12 +605,21 @@` | +10/−1 | 015 | `checkProjectOwnership` 改用 helper；新增私有 `projectScopeOrAll()`（isPlatformAdmin→empty，否则 quietly）及两行注释 |
| 06 | VenueBusinessHoursService.java | `@@ -1,5 +1,5 @@` | +1/−1 | 015 | 版本 V0.01→V0.02。本文件本批只承载 015 |
| 07 | VenueBusinessHoursService.java | `@@ -41,7 +41,8 @@` | +2/−1 | 015 | list：注释+`projectScopeOrAll()` |
| 08 | VenueBusinessHoursService.java | `@@ -51,6 +52,15 @@` | +9/−0 | 015 | 私有 `projectScopeOrAll()` 与 FloorPlan 同口径 |
| 09 | VenueBusinessHoursService.java | `@@ -62,7 +72,7 @@` | +1/−1 | 015 | save 改 `projectScopeOrAll()` |
| 10 | FloorPlanServiceTests.java | `@@ -1,11 +1,12 @@` | +2/−1 | 015+016 | 版本 V0.04→V0.05（本文件两 ID）；`import AccessActor` 为 015 桩（016 新例用默认 `service`，不直接引用 AccessActor） |
| 11 | FloorPlanServiceTests.java | `@@ -274,6 +275,37 @@` | +31/−0 | 016 | `publishRemainingPendingCandidatesAfterPartialPublish`：2 候选、先发 1、再发剩余、任务 published、图形 A101/A102、候选全 published |
| 12 | FloorPlanServiceTests.java | `@@ -442,22 +474,53 @@` | +43/−12 | 015 | 删「永不抛 empty」桩；加平台管理员全量例、无归属员工 fail-closed 例；`noScopeAuth`/`orphanStaffAuth`/`actorAuth` 只覆写 `currentActor` |
| 13 | FloorPlanServiceTests.java | `@@ -468,23 +531,8 @@` | +2/−17 | 015 | 项目员工桩改为 `AccessActor(..., "staff", projectId, null, "active")`，scope 方法走真实现 |
| 14 | FloorPlanServiceTests.java | `@@ -494,23 +542,8 @@` | +2/−17 | 015 | 商户员工桩改为 `AccessActor(..., "merchant_staff", null, merchantId, "active")` |
| 15 | VenueBusinessHoursServiceTests.java | `@@ -1,9 +1,10 @@` | +2/−1 | 015 | 版本 V0.02→V0.03；import AccessActor |
| 16 | VenueBusinessHoursServiceTests.java | `@@ -86,11 +87,18 @@` | +9/−2 | 015 | `auth(null)`→平台管理员 actor；有 projectScope→项目员工 actor；`actorAuth` 只覆写 `currentActor` |
| 17 | VenueBusinessHoursServiceTests.java | `@@ -122,6 +130,32 @@` | +26/−0 | 015 | 平台管理员 listAll 例（`projectQueries` 空）；无归属员工 list/save 抛 `PROJECT_SCOPE_REQUIRED` 且不落库 |

行数核对：hunk +/- 合计 +147/−59，与 `--stat` 一致。无第 5 文件、无锁/HTML/OpenAPI/AuthorizationService/Controller/迁移行。

越界对照（两树 blob 全等，不在 diff 内）：AuthorizationService、FloorPlanController、PlatformController、FieldLabels、openapi yaml/json、三 HTML、criteria-lock 与 sidecar、findings、ledger。

**范围归因票：PASS。** 无不可归因行。

---

## 5. 内容审计

### 5.1 M015-015

实现与定稿逐句对齐：

- 两服务各一份**私有** `projectScopeOrAll()`，未抽公共类、未改 AuthorizationService（M006 红线④：`currentProjectScopeQuietly` 仍 `Optional.of(requireProjectScope(actor))`，无组织即抛，不因管理员返回空）。
- 条件：`authorizationService.isPlatformAdmin(authorizationService.currentActor())` 为真 → `Optional.empty()`；否则 quietly。
- 五处调用已改：`listFloorPlans`、`validateCreateRequest`、`checkProjectOwnership`、`VenueBusinessHoursService.list`、`save`。
- 测试桩只覆写 `currentActor`。`AccessActor` 字段序为 `(id, username, accountType, organizationId, merchantId, status)`，与构造一致。`isPlatformAdmin` / `currentMerchantIdQuietly` / `currentProjectScopeQuietly` 均不依赖 repository，`AuthorizationService(null)` 子类可走真实现。
- 平台管理员：`accountType=platform_admin`、`status=active`、organizationId/merchantId 空 → `isPlatformAdmin` 真 → 全量。
- 无归属员工：`accountType=staff`、无 organizationId → 非管理员 → quietly → `PROJECT_SCOPE_REQUIRED`。

与 `requirePlatformScope` 平台管理员桥：同谓词 `isPlatformAdminBridge`。注意 `requirePlatformScope` 在桥之后还放行「非外包、无 merchantId」的普通平台员；015 的 empty-全量**只**给桥上的平台管理员，普通无归属员工 fail-closed。这正是定稿要的，不是把 quietly 当成全量。

商户/项目员工：桩改为真实现后，GREEN 中 `listFloorPlansFiltersToMerchantScope`、`accessingFloorPlanBelongingToDifferentMerchantIsForbidden`、`createFloorPlanWithSameKeyAcrossMerchantsDoesNotOverwrite`、`createFloorPlanOutsideProjectScopeIsForbidden`、`listFloorPlansFiltersToProjectScope`、`accessingFloorPlanBelongingToDifferentProjectIsForbidden`、`listFiltersByProjectScope`、`rejectsVenueOutsideProjectScope` 均在 88 例内且 0 失败。无回归。

Controller / OpenAPI / HTML / AuthorizationService：未改。营业时段无独立 Controller，入口 `PlatformController` blob 未变。

ponytail ultra：两处重复私有方法是定稿「各加私有」，不是多余抽象。未新增字段、接口、状态。Lean already. `net: -0`。

**015 内容票：PASS。**

### 5.2 M015-016

```
if (!"parsed".equals(task.status()) && !"published".equals(task.status())) {
    throw ApiException.badRequest("VALIDATION_FAILED", "只有解析完成或已发布的导入任务才能继续发布");
}
```

候选环未改：空列表拒、归属校验、`!"pending".equals(candidate.status())` →「只有待处理的候选项才能发布」。任务写回仍 `published`。未改 DTO/OpenAPI/HTML，未新增候选状态。`IMPORT_TASK_STATUSES` 仍含 `failed`/`cancelled`；二者不是 parsed/published，前置仍拒。失败任务例 `importTaskWithOnlyErrorsIsFailedAndCannotBePublished` 仍断言 `VALIDATION_FAILED`（GREEN 过）。

分批发布例断言：2 候选、第一次 published、第二次成功、图形 `A101`/`A102`、候选 status 全 `published`。与定稿一致。

已发布任务再发**已发布**候选：`publishImportTaskRejectsAlreadyPublishedTask` 先发该唯一候选，再发同一 id；任务已是 published 故通过 016 新前置，随后在候选环被拒。`ApiException.badRequest` 把文案放进 `RuntimeException` 消息，源码文案仍是「只有待处理的候选项才能发布」。该例只断言 `VALIDATION_FAILED` 未锁中文整句——既有例强度，不是本批新发明，不升缺陷。

**016 内容票：PASS。**

### 5.3 测试有效性（TDD / 先红后绿）

B3 自报：`/home/james/audit-tmp/m015_b3r3_run.log` — 先只落测试 `RED_MVN_EXIT=1`，再改实现 `GREEN_MVN_EXIT=0`、XML 88/0。本席读 `m015_b3r3_red.log`：

- FloorPlanServiceTests：39 例，Failures 2 / Errors 30。平台管理员全量例、016 分批例、orphan 例均在 Errors 中，根因均为 `validateCreateRequest` → 真 `currentProjectScopeQuietly` → `PROJECT_SCOPE_REQUIRED`（`AuthorizationService.java:292/506`）。这就是 015 的生产契约（旧桩伪造 empty、旧实现把 quietly 当全量）。
- 2 个 Failure：原期望 `VALIDATION_FAILED` 的非法尺寸/幂等例，实际先撞 `PROJECT_SCOPE_REQUIRED`——同样证明桩已走真 quietly、实现尚未桥接管理员。
- VenueBusinessHoursServiceTests：平台管理员 list/save 例 ERROR 同因；`rejectsDayOfWeekOutOfRange` 等 Failure 为 expected VALIDATION_FAILED but was PROJECT_SCOPE_REQUIRED。`staffWithoutProjectScopeIsRejected`（Venue）**未**出现在 Errors/Failures，RED 已绿——该例锁定的是红线④既有 fail-closed，不是 015 新缺口；定稿「各加一例先红」由平台管理员例承担。
- 商户纯路径两例 RED 未红，说明 015 桩改写没有误伤商户支。
- 016 分批例在 RED 止于 `createFloorPlan`（015 症状），**未**打到旧文案「只有解析完成的导入任务才能发布」。这是整批先落测试、016 例复用默认平台管理员 `service` 的耦合，不是假断言。GREEN 该例跑过且断言是定稿指定的行为。非阻断：若再修测试隔离，应用 `serviceWithProjectScope` 让 016 能独立红；本批不因此 FAIL（无新 ID、无错误实现）。

本席主仓复跑（只导出三变量，URL 库名改 `complex_test`，未 source，未设 `SPRING_PROFILES_ACTIVE`）：

```
mvn -o -q --file backend/pom.xml \
  -Dtest=FloorPlanControllerTests,FloorPlanImportParserTests,FloorPlanJdbcIntegrationTests,FloorPlanServiceTests,VenueBusinessHoursServiceTests \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

`MVN_EXIT=0`。库：`jdbc:postgresql://127.0.0.1:5432/complex_test`。Flyway 校验 395、schema 427、无迁移。TEST-*.xml：

| XML | tests | failures | errors |
|---|---|---|---|
| FloorPlanControllerTests | 14 | 0 | 0 |
| FloorPlanImportParserTests | 14 | 0 | 0 |
| FloorPlanJdbcIntegrationTests | 7 | 0 | 0 |
| FloorPlanServiceTests | 39 | 0 | 0 |
| VenueBusinessHoursServiceTests | 14 | 0 | 0 |
| **合计** | **88** | **0** | **0** |

XML 中确认新例名均在：`platformAdminWithoutOrganizationSeesAllFloorPlans`、`staffWithoutProjectScopeIsRejected`（两测试类）、`publishRemainingPendingCandidatesAfterPartialPublish`、`platformAdminWithoutOrganizationListsAllVenues`。

断言有效：管理员例断言跨项目可见且（Venue）`projectQueries` 空；orphan 断言 code=`PROJECT_SCOPE_REQUIRED`；016 例断言任务/图形/候选状态，不是 tautology。

**测试有效性票：PASS**（016 RED 原因被 015 挡住，记非阻断，不升 FAIL）。

---

## 6. 派发点名项对票

| 点名 | 结论 |
|---|---|
| isPlatformAdmin 与 requirePlatformScope 平台管理员桥同口径 | 是，同一 `isPlatformAdminBridge` |
| AuthorizationService / Controller / OpenAPI / HTML 未改 | 是（两树 blob 全等；营业时段入口为 PlatformController） |
| 商户员工与项目员工无回归 | 是（真桩 + GREEN 既有例） |
| 已发布任务再发已发布候选仍被「只有待处理的候选项才能发布」拒绝 | 是（源码候选环；既有 republish 例 GREEN；文案在 `ApiException` message） |
| 归因不到 015/016 的行（含注释、版本头） | 无 |

---

## 7. 非阻断观察（不升 ID、不挡本席 PASS）

1. 016 新例 RED 未观测到旧发布文案，被 015 创建路径挡住。
2. `publishImportTaskRejectsAlreadyPublishedTask` 只锁 `VALIDATION_FAILED`，不锁中文整句；拒绝路径与文案在实现里仍在。
3. 无单独 `cancelled` 任务发布例；前置 `parsed∨published` 已覆盖。
4. 平台管理员若同时带 `merchantId`，楼层列表仍走 003 商户优先，不会因 015 变成全量。这是既有 003 口径，015 未改商户支。

---

## 8. 最终票

| 轴 | 票 |
|---|---|
| 内容（015 最小处置、016 最小处置、无回归、无新发明） | PASS |
| 归因（闸 3，逐行 015/016） | PASS |
| 测试（先红后绿、断言有效、本席 XML 88/0） | PASS |
| **总结论** | **PASS** |

任一轴 FAIL 即整轮 FAIL。本席三轴均 PASS。本票不等于 B4 双 PASS。

本席未读席甲本次 raw。未修业务/HTML/锁/findings/ledger。未 fetch/commit/push/merge/deploy。

graft 本席检索：`projectScopeOrAll` tokens saved ≈ 8,551；`isPlatformAdmin` tokens saved ≈ 58,845；合计 ≈ 67,396。
