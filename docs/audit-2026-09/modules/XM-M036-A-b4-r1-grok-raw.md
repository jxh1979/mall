CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M036-A B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 独立只读审计席乙（grok-4.7 / xhigh）
时间：2026-09-24T04:15:01Z
计划：160-audit-prompt.md V0.17 B4 与 §5 闸 3
本文件路径：`docs/audit-2026-09/modules/XM-M036-A-b4-r1-grok-raw.md`
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：自文首独占锁行起至文末。正文不再出现同名独占字段行。

模块：合成模块 XM-M036-A，B3 第 1 次之后的 B4。
归因票：PASS。越界行 0。
内容票：PASS。
总结论：PASS。
以上只是席乙本轮 B4-r1 的票。任一侧 FAIL 即本轮 FAIL。本席不修。这不等于两席合取，也不等于 B5 或模块收口。

未读取：`modules/XM-M036-A-b4-r1-self-raw.md`。落盘时该路径已存在，本席只做了 `stat`（11759 字节，mtime 2026-09-23 21:04:50 -0700），没有打开。ledger 顶部派发段在定位本轮任务时进入过视野，不作为归因表，也不作为本席票的依据。已读：`160-audit-prompt.md` V0.17 的 B4、§5 闸 3、§6；`findings.md` V4.00 顶部「当前 XM-M036-A B2-r1 双 PASS 定稿」段及其上方候选段；本席旧 raw `modules/XM-M036-A-b1-grok-raw.md` 里 B2-r1 写死的实施读法；冻结 diff；两个 tree；红绿名单与失败信息；after tree 上相关方法正文。未用 MCP、子代理、第三席。未改业务、HTML、锁、findings、ledger。未 fetch、commit、push、merge、deploy。未装依赖。未对 `complex`、`complex_test` 执行写语句。未另建 worktree、未另建空库。本席没有重跑 Maven。

---

## 0. 技能预检（本环境实核）

用户指令禁止子代理、MCP、第三席、问人、修业务。技能本体与之冲突的动作作废，只借结构。下表是本席这次实际打开过的正文，不是凭记忆宣称。

| 技能 | 本环境 | 本节点 | 记录 |
|---|---|---|---|
| `using-superpowers` | superpowers 6.3.0，已读 | 开工自检。用户「只允许两席、不用子代理」优先于派代理 | 无缺口 |
| `using-arc` | 已读前段 | 不走 Arc audit。本体要派评审子代理并另写审计产物 | 不适用 |
| `graft` | 已读 `SKILL.md` | **本轮未调用** | 审计对象是两个 tree id。工作区 HEAD 是 `e456ba96`，另有大量与本 diff 无关的未提交改动。graft 索引的是工作区现状。定位用 `git show <tree>:<path>`、`git grep <tree>`、`git diff` |
| `verification-before-completion` | 已读 | 投票前实算锁哈希、`cmp` 冻结 diff、`--raw` 模式、红名单 `comm`、对照 after tree 源码 | 本席未重跑 Maven。3536 的红绿以本席读过的 `.red` / `.msgs` / `run.log` 为准 |
| `systematic-debugging` | 已读 | 只借读错误、追数据流：红名单栈、`currentMerchantScope`、主键插入、两条删除路径 | 本体「先加诊断插桩」「最小改代码验证假说」作废。本节点不修 |
| `diagnosing-bugs` | 已读 | 反馈环用已落盘的 RED 失败信息（每条红的异常原文都读了）。未新跑 Maven，未加插桩 | 本体「问人、自己修」作废 |
| `grilling` | mattpocock，已读开头 | 对抗式自问：列表切回平台管理员算不算越界、自审闸删了算不算扩大、新回归在旧主键下会不会红。事实自己读 tree | 本体「等用户回答、派子代理查事实」作废 |
| `requesting-code-review` | 存在，未按本体执行 | 本席就是 B4 外审。基线是两个 tree id | 不派同会话子代理 |
| `receiving-code-review` | 存在 | 本节点不适用。本席出意见，不修 | — |
| `test-driven-development` / mattpocock `tdd` | 存在 | 核 RED 里新断言的失败原文，以及 GREEN 后这些名字消失。本席不新写测试 | — |
| `ponytail` | 4.9.0，存在 | 只审有没有顺手加的抽象和新业务。未落地修改 | — |
| `codebase-design` | 存在 | 看 `isMasterActor` 是否新业务类型。它只是三条写入体共用的既有 `isMasterAuthority` 判断 | 不派第二设计 |
| `writing-for-agents` | 存在 | 本 raw 写给合取席 | — |
| `resolving-merge-conflicts` | 存在 | 无进行中的 merge / rebase | 节点不适用 |
| `batch-grill-me` | 技能树中无本轮命中的 SKILL.md | — | **缺口**，不冒充已调用 |
| `ubiquitous-language` | 无 | — | **缺口** |

明确不用：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`、`research`、mattpocock `code-review`、`domain-modeling`。未使用 MCP。本轮无 graft 调用，无 graft 节省数。

---

## 1. 输入与独立实算

锁哈希先算，再对照 sidecar 与 ledger 顶部基准。对照不是抄写来源。

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
python3 hashlib.sha256 同一文件
openssl dgst -sha256 同一文件
```

三工具同值，与文首独占行一致。文件 891182 字节。判据锁版本 V0.14。sidecar 一行同值。ledger 顶部基准同值。`LOCK-AMEND-COUNT` 仍为 1。本轮未 LOCK-AMEND，未新增判据。

| 项 | 本席实算 |
|---|---|
| findings | V4.00，SHA256 `94254e17dc3c941fb041b4c0ce6d56a74e36c9d53848ff593b6b8eabed6f4015`。队列「是」仍是 XM-M036-A-001…008 |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md`，SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| before / after | `bfe6bea44dcede386aa433ef71971ad8cf8852c5` / `b99e79c1e08757c04f6c9f869b402e2e3e68f31d`，`git cat-file -t` 均为 `tree` |
| 冻结 diff | `/home/james/audit-tmp/xm036a_b3r1.diff`，32761 字节，SHA256 `800824fe472eebd5d75dca3fc235a5d88367cb5e90c79d05400eb6daec194d4e` |
| `git diff --binary <before> <after>` | 管道 `sha256sum` 与冻结文件相同；`cmp -s` 退出码 0。全文逐字节相同 |
| 规模 | `git diff --shortstat`：12 files changed, 150 insertions(+), 40 deletions(-)。本席另数 +/- 行（去掉 `+++` / `---`）为 150 / 40。hunk 头 30 个 |
| 模式 | `git diff --raw` 12 行都是 `100644 → 100644`。`git diff --summary` 空。无新增、删除、改名、改模式 |
| 工作区这 12 个文件 | `git diff <after> -- <12 paths>` 为空。工作区字节与 after tree 相同。工作区其他脏文件不在本 diff 内 |
| 写入前 12 个文件 | 每个路径 `git rev-parse <before>:<path>` 等于 `4f233e7b:<path>`。与 origin/main 逐字节相同 |

before tree 相对 `4f233e7b` 还有 13 条路径不同（graft 钩子、`.grok`、`AGENTS.md`、`findings.md`、`ledger.md`、两份 B1 raw、`index` 等）。这 13 条在 before 与 after 之间没有差异，不出现在本轮 diff 里。闸 3 只审 before → after 的行。它们不是本轮修复行。

定稿口径：001…008 用 findings「当前 XM-M036-A B2-r1 双 PASS 定稿」段，加上方候选段的最小处置，并对照本席 B2-r1 写死的实施读法。只登记的 XM-M036A-1、XM-M036A-2 与观察不在修复队列，不能单独支撑任何改动行。

本席 B2-r1 写死、本轮用来核内容的读法：

- 005：最高权限发起的直接通过不得再撞 `approveDelete` 的申请人自审闸。该闸在「审批只认最高权限」之后没有适用对象，照 `StaffService.approveDeleteProfile` 不设。
- 006：审批闸只在 `decideDelete`，且在结论值校验之后。恢复只在 `deleteStatus = approved` 的墓碑分支收成最高权限，普通取消存档仍走 manage。最高权限发起在写入体内直接按通过，不进 `decideDelete` 的自审闸。跳过 `checkProjectOwnership` 只写在这三条写入体，不改该方法本体，不改范围方法。
- 007：只在 `approveDeleteProfile` 与 `requestDeleteProfile` 上，最高权限按存在取档案。`requireProfileInScope`、`requireRegistrationScope` 本体不改。

---

## 2. 逐块归因

上下文行不是改动，不计入。下表 30 块覆盖全部 150 行新增与 40 行删除。每一块的增删行只归一个已定稿且归属「是」的 ID。越界行 0。

| # | 文件与 hunk | 增/删 | ID | 行在做什么 |
|---|---|---|---|---|
| 1 | `StaffService.java` `@@ -382,7 +382,10` | +3 / −1 | 007 | `requestDeleteProfile`：最高权限改 `findProfileById`，不存在则 `STAFF_NOT_FOUND`；其他人仍 `requireProfileInScope`。注释点明只校验存在 |
| 2 | `StaffService.java` `@@ -409,7 +412,9` | +2 / −1 | 007 | `approveDeleteProfile` 在既有最高权限闸之后，把 `requireProfileInScope` 换成按 id 取档案。注释写明留痕仍走既有删除事件 |
| 3 | `AmusementService.java` `@@ -359,7 +359,7` | +1 / −1 | 006 | 恢复方法注释由「持审批权」改为「墓碑只能由最高权限本人恢复」 |
| 4 | `AmusementService.java` `@@ -377,14 +377,17` | +6 / −3 | 006 | 恢复写入体：最高权限跳过 `checkProjectOwnership`；仅 `deleteStatus = approved` 且非最高权限时抛 `MASTER_APPROVAL_REQUIRED`。删掉可授出码 `requirePermission(approve)` |
| 5 | `AmusementService.java` `@@ -410,7 +413,10` | +3 / −1 | 006 | 申请删除写入体：最高权限跳过 `checkProjectOwnership`，其他人照旧 |
| 6 | `AmusementService.java` `@@ -419,6 +425,10` | +4 / 0 | 006 | 同一写入体内，最高权限在 `requestDelete` 已落待审之后调用 `repository.decideDeleteProjectEntitlement(..., true)`。不调用服务层 `decideDelete` |
| 7 | `AmusementService.java` `@@ -426,7 +436,7` | +1 / −1 | 006 | 审批方法注释改为「审批人只认最高权限本人」，并保留「申请人禁自审」 |
| 8 | `AmusementService.java` `@@ -438,6 +448,9` | +3 / 0 | 006 | 结论值校验之后、`executeWrite` 之前：非最高权限抛 `MASTER_APPROVAL_REQUIRED` |
| 9 | `AmusementService.java` `@@ -448,7 +461,6` | 0 / −1 | 006 | 审批写入体去掉 `checkProjectOwnership`。能进入该体的只有已过第 8 块闸的最高权限 |
| 10 | `AmusementService.java` `@@ -464,6 +476,11` | +5 / 0 | 006 | 私有 `isMasterActor()`，只转调 `authorizationService.isMasterAuthority(currentActor())`。全文件调用点只有恢复、申请删除、审批三处 |
| 11 | `FieldLabels.java` `@@ -72,6 +72,7` | +1 / 0 | 004 | `p("business_entity_id", "经营主体")`。after tree 该文件仅此一处此键 |
| 12 | `FacilityEquipmentService.java` `@@ -108,12 +108,21` | +4 / 0 | 005 | `requestDelete` 在落待审和 `delete_requested` 留痕之后，最高权限调用 `approveDelete(..., approved)`，返回状态改为 `approved` |
| 13 | `FacilityEquipmentService.java` `@@ -124,10 +133,7` | +4 / −3 | 005 | `approveDelete` 首个可执行语句是非最高权限 `MASTER_APPROVAL_REQUIRED`。删掉申请人自审三行，换一句说明该闸已无适用对象。`requirePermission` 与后面的占用计数仍在 |
| 14 | `JdbcPlatformRepository.java` `@@ -394,7 +394,7` | +1 / −1 | 008 | 主键由 `stdr_` + 种类号 + 毫秒改为 `stdr_` + `UUID.randomUUID()`。`space_type_id`、`requested_by`、`requested_at` 的绑定未改 |
| 15 | `StaffProfileArchiveDeleteJdbcIntegrationTests.java` `@@ -107,4 +107,15` | +11 / 0 | 007 | 新增 `masterRequestedDeletionTakesEffectDirectly`：平台管理员建档存档，`StaffTestActors.master()` 发起删除，断言状态 `approved` 且 `findProfileById` 为空。原有两条断言未改 |
| 16 | `AmusementServiceTests.java` `@@ -1482,11 +1482,13` | +3 / −2 | 006 | 留墓碑用例的审批人由平台管理员改为 `asMaster()`。审批返回值断言 `approved` 之后，切回 `asPlatformAdmin()` 再查列表。列表两条断言（默认空、含归档 1 条）原文仍在 |
| 17 | `AmusementServiceTests.java` `@@ -1500,7 +1502,7` | +1 / −1 | 006 | 驳回用例的审批人由平台管理员改为 `asMaster()`。`deleteStatus = none` 的断言未改 |
| 18 | `AmusementServiceTests.java` `@@ -1508,36 +1510,57` | +16 / −5 | 006 | 自审用例改名为 `nonMasterCannotApproveProjectEntitlementDeletion`：发起人与另一名平台管理员都断言码 `MASTER_APPROVAL_REQUIRED`，并断言仍为 `pending`。新增最高权限发起即 `approved` 且在役种类列表为空。墓碑恢复用例改名，平台管理员恢复期望码改为 `MASTER_APPROVAL_REQUIRED`，成功路径改 `asMaster()`，删掉 `permissionExists` 打桩 |
| 19 | `AmusementServiceTests.java` `@@ -1545,6 +1568,16` | +10 / 0 | 006 | 测试私有 `asMaster()`：`MASTER_USER_ID` / `MASTER_ACCOUNT_TYPE`，所属公司 `platform-company`，商户号空 |
| 20 | `ComplianceControllerTests.java` `@@ -71,7 +71,7` | +1 / −1 | 002 | `contract-key-dates` 无幂等键的 POST 加上请求属性 |
| 21 | `ComplianceControllerTests.java` `@@ -93,7 +93,7` | +1 / −1 | 002 | 同一路径带幂等键的 POST 加上同一属性 |
| 22 | `ComplianceControllerTests.java` `@@ -153,7 +153,7` | +1 / −1 | 002 | `reminders/scan` 的 POST 加上同一属性 |
| 23 | `FacilityEquipmentArchiveDeleteJdbcIntegrationTests.java` `@@ -50,7 +50,7` | +1 / −1 | 005 | 无引用则删除成功：审批人改 `asMaster()` |
| 24 | 同文件 `@@ -86,7 +86,7` | +1 / −1 | 005 | 被工单引用则拒绝：审批人改 `asMaster()`。抛错断言未改 |
| 25 | 同文件 `@@ -94,7 +94,7` | +1 / −1 | 005 | 方法名由 `selfApproveForbidden` 改为 `ordinaryApplicantCannotApproveMasterOnlyDeletion` |
| 26 | 同文件 `@@ -102,6 +102,31` | +25 / −1 | 005 | 期望文案由「发起人」改为「最高权限」，并断言 `delete_status` 仍为 `pending`。新增最高权限发起即 `approved`、库内状态 `approved`、`findEquipmentById` 为空。同文件私有 `asMaster()` 与第 19 块同一会话形状 |
| 27 | `FinanceControllerTests.java` `@@ -52,6 +52,10` | +4 / 0 | 001 | 类字段 `@MockitoBean StoredValueService`，全限定注解，无额外 import 行 |
| 28 | `FinanceControllerTests.java` `@@ -416,11 +420,6` | 0 / −5 | 001 | 删掉 `TestBeans` 里交出 `StoredValueService` mock 的 `@Bean`。`FinanceService`、`AuthorizationService`、`AuthService` 三个 `@Bean` 仍在 |
| 29 | `CustomerGuardedActionAndRejectionAuditTests.java` `@@ -39,7 +39,7` | +1 / −1 | 003 | 参数行 `consume` 改为 `payTicketOrder`。期望码 `customer.stored_value.consume` 未改 |
| 30 | `PlatformJdbcIntegrationTests.java` `@@ -560,6 +560,19` | +13 / 0 | 008 | 新增 `backToBackSpaceTypeDeleteRequestsNeverCollideOnId`：同一种类连续 50 次申请并驳回完结，断言历史行数为 50。无 `sleep`，无吞异常。原 `completingSpaceTypeRequestKeepsPriorRequestHistory` 未改、未删 |

002 的属性字面量是 `com.complex.platform.gate.verified_terminal`，值是 `TerminalIdentityHolder.VerifiedTerminal("web", null, true)`。after tree 上 `TerminalIdentityHolder` 第 20 行 `ATTR` 就是这个字符串。三个 `post(` 都带了该属性，文件里没有第四个 POST。`TerminalSwitchGuard` 的 import 与第 236 行 mock `@Bean` 仍在。拦截器源文件不在这 12 个路径里。

006 的 `checkProjectOwnership` 方法正文，本席用括号配对从两棵 tree 抽出后逐字相同。`git grep -c checkProjectOwnership` 由 22 行降到 21 行，少的那一行就是第 9 块删掉的调用。`AuthorizationService.java`、`AmusementController.java` 相对两棵 tree 的 diff 为空。

007 的 `requireProfileInScope`（第 1176 行）与 `requireRegistrationScope`（第 1198 行）不在任何 hunk 里。其余调用点（登记、在职、销售岗、凭证）仍走 `requireProfileInScope`。

---

## 3. 内容审计

### 001

`@MockitoBean` 只标在 `StoredValueService` 一个字段上。`TestBeans` 只少了这一个 `@Bean`。生产 `StoredValueService` 不在 diff 里。

RED 阶段这 12 条 `FinanceControllerTests` 全部离开红名单。基线里它们是红的。测试补丁单独就让上下文能起来。GREEN 后仍不在红名单。

### 002

三个失败 POST 都带与 `TerminalIdentityHolder.ATTR` 相同的键，终端类型 `web`。guard Bean 还在。拦截器未改。RED 阶段三条 `ComplianceControllerTests` 离开红名单。

### 003

只改了那一个方法名。期望码仍是 `customer.stored_value.consume`。after tree 上 `StoredValueService.payTicketOrder` 第 264–265 行的 `@GuardedAction` 就是这个码。没有新增空的 `consume`。基线红 `[3]` 在 RED 阶段已不红。

### 004

只加一行「经营主体」。`FieldLabelsTests` 不在本 diff。RED 阶段该测试仍红，失败信息是缺中文名的键 `["business_entity_id"]`。生产补丁加上这一行之后，GREEN 红名单为空，这条随之转绿。测试门禁文件没有改。

### 005

`approveDelete` 的第一个可执行语句是最高权限闸，码 `MASTER_APPROVAL_REQUIRED`，文案「设施设备删除须由最高权限本人批准」。其后的 `requirePermission("facility.equipment.approve")` 保留。

`requestDelete` 先 `updateEquipment` 为待审，先记 `facility.equipment_delete_requested`，然后最高权限才调用 `approveDelete`。`approveDelete` 的通过分支仍先 `countEquipmentReferences`，引用大于 0 抛 `FACILITY_EQUIPMENT_IN_USE`，然后才 `deleteEquipment` 并记 `delete_approved`。占用检查在最高权限发起的路径上照走。自调用不经过代理，方法体内的闸和占用计数都会执行。

删掉的只有自审三行（比对自己与 `deleteRequestedBy`，码 `FACILITY_EQUIPMENT_SELF_APPROVE_FORBIDDEN`）。定稿读法写明：审批只认最高权限之后，这条闸没有适用对象，照员工档案批准方法不设。最高权限发起走的是直接通过，不再被这三行挡住。

测试：成功与占用两条的审批人改为最高权限，抛错与成功断言未改。原自审用例改为平台管理员申请人不能批，期望文案含「最高权限」，并断言库内仍是 `pending`。另补最高权限发起即 `approved`、库状态 `approved`、按 id 查找为空。RED 里前一条的失败原文是「没有操作权限：facility.equipment.approve」，不含「最高权限」，所以新断言在旧生产上会失败。后一条失败原文是期望 `approved`、实际 `pending`。没有删掉原断言来换一条更弱的。

### 006

审批闸只出现在 `decideDeleteProjectEntitlement`。顺序是：结论必须为 `approved` 或 `rejected`，否则 `VALIDATION_FAILED`；然后才是非最高权限 `MASTER_APPROVAL_REQUIRED`；然后才进 `executeWrite`。恢复方法和申请删除方法没有这道审批闸。

墓碑恢复的拒绝写在 `deleteStatus` 等于 `approved` 且调用者不是最高权限的分支，码 `MASTER_APPROVAL_REQUIRED`。普通取消存档不进这个分支，方法上的能力码仍是 `amusement.project_entitlement.manage`。

最高权限发起时，在申请删除的 lambda 里直接 `repository.decideDeleteProjectEntitlement(id, type, true)`。仓储方法把 `true` 写成 `delete_status = approved`，并且 `WHERE delete_status = 'pending'`。这条路径不调用服务层 `decideDelete`，因此碰不到该服务方法里仍保留的自审闸（`PROJECT_ENTITLEMENT_SELF_APPROVE_FORBIDDEN`，第 468–470 行，本 diff 未删）。非最高权限的申请仍只落到 `pending`，由最高权限另审，自审闸留在审批方法里。

跳过归属检查的三个位置：恢复 lambda、申请删除 lambda、审批 lambda（审批是直接删掉调用，因为非最高权限在进 lambda 之前已经被拒）。`checkProjectOwnership` 正文与 `currentMerchantScope` 未改。其余游乐写路径仍调用它。`listProjectEntitlements` 第 295 行仍无条件调用它，本 diff 没有改这个方法。

两处原先描述「审批权 / 只授平台管理员」的注释已改成最高权限本人：恢复方法的 javadoc，以及审批方法的 javadoc。墓碑分支里的行注释一并改成同一现状。没有改到这三条路径以外的注释。

`isMasterActor` 没有新的权限种类，只是三次重复判断的私有包装。调用点没有第四处。

测试对照定稿：留墓碑与驳回两条的审批人改为最高权限；自审用例改成发起人与另一名平台管理员都不能批，码是 `MASTER_APPROVAL_REQUIRED`，状态保持 `pending`；补了最高权限发起即删除；墓碑恢复的成功路径改最高权限，平台管理员恢复期望码由 `PERMISSION_DENIED` 改为 `MASTER_APPROVAL_REQUIRED`，并删掉把审批码桩成存在的 `permissionExists`。RED 里 `nonMasterCannotApproveProjectEntitlementDeletion` 的失败原文是期望 `MASTER_APPROVAL_REQUIRED`、实际 `PROJECT_ENTITLEMENT_SELF_APPROVE_FORBIDDEN`。其余四条游乐新红的栈停在 `AuthorizationService.currentMerchantScope:434`，文案「此操作必须在商户范围内进行」。新断言在旧生产上会失败。`permissionExists` 打桩被去掉，没有借机把断言放宽。

列表切回平台管理员：见第 4 节。本席判定这几行归 006，不归只登记的 XM-M036A-1，也不越界。

### 007

`requestDeleteProfile`：最高权限走 `findProfileById`，没有记录则 `STAFF_NOT_FOUND`；其他人仍走 `requireProfileInScope`。后面的已存档、待审、落待审、`delete_requested`，以及原有的「最高权限发起再调批准」都还在。

`approveDeleteProfile`：原有的非最高权限 403 在取档案之前。过了闸才 `findProfileById`。因此这条路径上能取到档案的只有最高权限，取法只校验存在。

`requireProfileInScope` 与 `requireRegistrationScope` 的方法体没有 hunk。公司分支和登记范围没有被整体豁免。

新测试用开工段里已有的 `StaffTestActors.master()`。该助手的组织号是 null，RED 的失败原文是「请填写所属公司」，栈在 `StaffRegistrationService.requireText`。这是旧路径仍走公司分支的证据。生产最高权限会话的所属公司是 `platform-company`，而新分支只看 `isMasterAuthority`，不读组织号，两种会话进的是同一条存在性查找。GREEN 后这条新测试不红。原有两条员工档案删除测试的断言没有改弱；它们在 RED 阶段仍因「请填写所属公司」失败，GREEN 后转绿。

### 008

主键表达式只改了 id 的生成。插入列仍是 `id, space_type_id, reason, requested_by, requested_at, status`。`findSpaceTypeDeleteRequestedBy` 按 `space_type_id` 加 `status = pending`，`ORDER BY requested_at DESC`。`completeSpaceTypeDeleteRequest` 的子查询按 `space_type_id`、`pending`、`requested_at DESC, id DESC` 取一行。两处都不解析主键字符串。查询语义不变。

新回归在旧主键下会失败。RED 失败原文：`duplicate key value violates unique constraint "space_type_delete_requests_pkey"`，键 `(id)=(stdr_m036a_type_burst_1790222480909)`，栈在 `saveSpaceTypeDeleteRequest` 第 393 行、测试第 568 行。键还是「种类号 + 毫秒」。同一次 RED 里，未改过的 `completingSpaceTypeRequestKeepsPriorRequestHistory` 也以同一约束失败，键是 `stdr_m009_type_request_1790222480920`。这条不在基线 23 红里，是旧主键在这一次运行中撞上了；它不是 diff 行。新测试没有 `sleep`，没有捕获唯一约束。行数断言是 50，历史被覆盖时也会失败。原历史用例还在文件里。GREEN 红名单为空，两案都过。

### 新缺陷与新业务

本席沿 diff 对过的点，没有单独构成新缺陷或新业务的行：

- 设施最高权限发起调用的是既有 `approveDelete`，占用、审计事件名、删除执行都沿用原通过分支。返回值把状态写成 `approved`，与员工档案发起即删除的既有返回形状相同。GREEN 中该测试不红，库内 `delete_status` 与返回值一致。
- 游乐最高权限发起写的是仓储既有更新，只改 `delete_status`。没有往这条路径加删前占用检查。候选段把「游乐权益删除审批的仓储更新没有占用检查」登记为观察，并写明 006 不改该仓储语义。`JdbcAmusementRepository` 不在本 diff。
- 最高权限在恢复整段 lambda 里跳过归属检查，包括普通取消存档。权限闸仍只收紧墓碑分支。这是定稿「跳过写在三条写入体、普通取消存档照旧」的读法，不是把普通取消存档改成最高权限专属。
- 没有新的权限码、新的状态值、新的表或新的接口。错误码 `MASTER_APPROVAL_REQUIRED` 是员工档案批准方法里已经在用的码。

---

## 4. 游乐列表切回平台管理员

`listProjectEntitlements` 第 295 行调用 `checkProjectOwnership`，后者无条件调用 `currentMerchantScope`。最高权限没有商户号，也不是平台管理员，这里会抛「此操作必须在商户范围内进行」。这是只登记的 XM-M036A-1，不在本批修复队列。

留墓碑用例原先后半是：平台管理员审批之后，用同一安全上下文查默认列表为空、含归档列表为 1。006 要求审批人改为最高权限。若审批之后不换回平台管理员，后两条列表断言会在查列表时被范围方法拒绝，生产列表口径却不在 006 的处置里。

diff 的实际顺序：`asMaster()` 调用 `decideDelete`，先断言返回值 `deleteStatus = approved`，再 `TestSecurityActors.asPlatformAdmin()`，然后执行原来的两条列表断言和在役种类为空的断言。生产 `listProjectEntitlements` 没有改。注释写明列表口径本批不改。

这三行（注释、切回平台管理员、以及为此把审批人从「一直是平台管理员」拆开）归 006。它们让定稿要求保留的墓碑可见性断言仍由平台管理员执行。它们没有实现 XM-M036A-1 的单据级列表放行。不越界。

---

## 5. 红绿证据（本席未重跑）

本席读了 `/home/james/audit-tmp/xm036a_b3_run.log`、`xm036a_b3_red.red`、`xm036a_b3_red.msgs`、`xm036a_b3_green.red`、`xm036a_b3_green.msgs`、`xm036a_b3_apply.log`，并用 `comm` 对过 `xmd029-ckpt.red`。

| 事实 | 本席看到的原文 |
|---|---|
| RED | 只落测试补丁。`MVN_EXIT(red)=1`。263 套，3536 测，13 红 |
| GREEN | 测试加生产补丁。`MVN_EXIT(green)=0`。3536 测，红名单文件 0 字节，失败信息文件 0 字节 |
| 基线 | `xmd029-ckpt.red` 23 行 |
| RED 中仍在基线里的 | 3 条：`FieldLabelsTests.everyFieldKeyThrownInMainSourcesHasChineseLabel`，以及两条员工档案删除（所属公司） |
| RED 中基线没有的名字 | 10 条，与 run log 的 NEW-RED 列表一致 |
| 这 10 条里未改测试源码的 | 1 条：`completingSpaceTypeRequestKeepsPriorRequestHistory`，失败是旧主键撞键，见 008 |
| 基线 23 条在 GREEN | 绿名单为空，23 条都不红。run log 写「相对基线转绿条数：23」 |
| 主工作区与 worktree | `xm036a_b3_apply.log` 写两者 diff 相同。本席另行 `cmp` 冻结 diff 与 `git diff --binary` 两个 tree，退出码 0 |

10 条新名字里，9 条是本 diff 新增或改名的测试，失败信息与第 3 节对应：游乐四条范围拒绝、一条自审码不符；设施一条文案不符、一条状态仍为 `pending`；空间种类新回归撞旧主键；员工新回归「请填写所属公司」。这些失败说明新断言在旧生产上站不住。GREEN 0 红说明生产补丁让它们和原来的 23 条一起过。本席没有自己执行 Maven，不把「日志写了 0」写成自己的测试命令输出。源码与失败原文已经足够判断处置是否按定稿落地。

`xm036a_b3_apply.log` 还写了写入前 12 个文件无 PRE-DIRTY。本席用 blob 比较复核了这句话。

---

## 6. 投票

归因票：PASS。30 块、150 增、40 删都能归到 XM-M036-A-001…008 里归属为「是」的 ID。没有归不到的行。文件模式未变。没有范围外文件。游乐列表切回平台管理员的测试行归 006。

内容票：PASS。八条都按定稿最小处置和本席 B2-r1 写死的读法落地。001 只换一处 Bean。002 三处属性与持有者常量一致，guard 与拦截器未动。003 只改方法名。004 只加「经营主体」。005 的最高权限闸在 `approveDelete` 首句，发起即通过且占用检查仍在，自审闸的删除符合「无适用对象」。006 的审批闸位置、墓碑分支、普通取消存档、写入体内直接通过、三处跳过归属检查、方法本体与范围方法不动，都与读法一致。007 两条删除路径按存在取档案，两个范围方法本体未改。008 主键改为随机，查询不依赖主键格式，新回归在本轮 RED 里以旧主键撞键失败。没有借机删弱断言，没有新业务。

总结论：PASS。

---

## 附录：冻结 diff 全文

本附录是 `/home/james/audit-tmp/xm036a_b3r1.diff` 的逐字节副本。上文 `cmp` 已确认它与 `git diff --binary` 两个 tree 的输出相同。

```diff
diff --git a/backend/src/main/java/com/complex/platform/access/staff/StaffService.java b/backend/src/main/java/com/complex/platform/access/staff/StaffService.java
index 3990501a..07500a44 100644
--- a/backend/src/main/java/com/complex/platform/access/staff/StaffService.java
+++ b/backend/src/main/java/com/complex/platform/access/staff/StaffService.java
@@ -382,7 +382,10 @@ public class StaffService {
     @org.springframework.transaction.annotation.Transactional
     public StaffProfileRecord requestDeleteProfile(String id,
             com.complex.platform.access.staff.dto.StaffProfileDeleteRequest request) {
-        StaffProfileRecord profile = requireProfileInScope(id);
+        // 最高权限逐笔开放（治理地图第 528 行第④类）：最高权限发起的删除只校验档案存在，不按公司或登记范围。
+        StaffProfileRecord profile = authorizationService.isMasterAuthority(authorizationService.currentActor())
+                ? repository.findProfileById(id).orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND", "员工档案不存在：" + id))
+                : requireProfileInScope(id);
         if (profile.archivedAt() == null) {
             throw ApiException.badRequest("STAFF_PROFILE_NOT_ARCHIVED", "员工档案必须先存档才能申请删除");
         }
@@ -409,7 +412,9 @@ public class StaffService {
         if (!authorizationService.isMasterAuthority(authorizationService.currentActor())) {
             throw ApiException.forbidden("MASTER_APPROVAL_REQUIRED", "员工档案删除须由最高权限本人批准");
         }
-        StaffProfileRecord profile = requireProfileInScope(id);
+        // 最高权限逐笔开放（治理地图第 528 行第④类）：不按公司或登记范围取档案，只校验存在；留痕由 delete_approved/rejected 承担。
+        StaffProfileRecord profile = repository.findProfileById(id)
+                .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND", "员工档案不存在：" + id));
         if (!"pending".equals(profile.deleteStatus())) {
             throw ApiException.badRequest("STAFF_PROFILE_DELETE_NOT_PENDING", "该员工档案没有待审批的删除申请");
         }
diff --git a/backend/src/main/java/com/complex/platform/amusement/AmusementService.java b/backend/src/main/java/com/complex/platform/amusement/AmusementService.java
index 033eea33..6897b66d 100644
--- a/backend/src/main/java/com/complex/platform/amusement/AmusementService.java
+++ b/backend/src/main/java/com/complex/platform/amusement/AmusementService.java
@@ -359,7 +359,7 @@ public class AmusementService {
 
     /**
      * 恢复（取消存档 / 墓碑复活）：清存档标记 + 删除轴回退 none。
-     * 已被审批通过删除（墓碑）的绑定恢复须持审批权 amusement.project_entitlement.approve（只授 role_platform_admin）。
+     * 已被审批通过删除（墓碑）的绑定只能由最高权限本人恢复（BSM 第 570 行）。
      */
     @com.complex.platform.gate.GuardedAction(capability = "amusement.project_entitlement.manage")
     @Transactional
@@ -377,14 +377,17 @@ public class AmusementService {
                 "amusement.project_entitlement.manage",
                 "amusement_project_entitlement",
                 () -> {
-                    checkProjectOwnership(id);
+                    boolean master = isMasterActor();
+                    if (!master) {
+                        checkProjectOwnership(id);
+                    }
                     AmusementProjectEntitlementRecord existing = requireProjectEntitlement(id, type);
                     if (existing.active()) {
                         throw ApiException.conflict("PROJECT_ENTITLEMENT_NOT_ARCHIVED", "该权益绑定处于生效中，无需恢复");
                     }
-                    if ("approved".equals(existing.deleteStatus())) {
-                        // 墓碑复活=撤销一次最高权限批准的删除，按同级最高权限把守。
-                        authorizationService.requirePermission("amusement.project_entitlement.approve");
+                    if ("approved".equals(existing.deleteStatus()) && !master) {
+                        // 墓碑复活=撤销一次最高权限批准的删除，只认最高权限本人（BSM 第 570 行）。
+                        throw ApiException.forbidden("MASTER_APPROVAL_REQUIRED", "已删除的项目权益绑定须由最高权限本人恢复");
                     }
                     repository.restoreProjectEntitlement(id, type);
                     return requireProjectEntitlement(id, type);
@@ -410,7 +413,10 @@ public class AmusementService {
                 "amusement.project_entitlement.manage",
                 "amusement_project_entitlement",
                 () -> {
-                    checkProjectOwnership(id);
+                    boolean master = isMasterActor();
+                    if (!master) {
+                        checkProjectOwnership(id);
+                    }
                     AmusementProjectEntitlementRecord existing = requireProjectEntitlement(id, type);
                     if (existing.archivedAt() == null) {
                         throw ApiException.badRequest("PROJECT_ENTITLEMENT_NOT_ARCHIVED", "请先存档再申请删除");
@@ -419,6 +425,10 @@ public class AmusementService {
                         throw ApiException.conflict("PROJECT_ENTITLEMENT_DELETE_ALREADY_PENDING", "该权益绑定已有待审批的删除申请");
                     }
                     repository.requestDeleteProjectEntitlement(id, type, deleteReason, currentActorId());
+                    if (master) {
+                        // BSM 第 570 行：持授权 U 盘密钥的最高权限本人发起的删除不需要经过任何审批。
+                        repository.decideDeleteProjectEntitlement(id, type, true);
+                    }
                     return requireProjectEntitlement(id, type);
                 }
         );
@@ -426,7 +436,7 @@ public class AmusementService {
 
     /**
      * 审批删除申请：approved → delete_status='approved'（墓碑，行留库、全读取路径过滤、可恢复，不物理删）；
-     * rejected → 回 none。权限 amusement.project_entitlement.approve（只授 role_platform_admin），且申请人禁自审。
+     * rejected → 回 none。审批人只认最高权限本人（BSM 第 570 行），且申请人禁自审。
      */
     @com.complex.platform.gate.GuardedAction(capability = "amusement.project_entitlement.approve")
     @Transactional
@@ -438,6 +448,9 @@ public class AmusementService {
         if (!"approved".equals(verdict) && !"rejected".equals(verdict)) {
             throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
         }
+        if (!isMasterActor()) {
+            throw ApiException.forbidden("MASTER_APPROVAL_REQUIRED", "项目权益绑定删除须由最高权限本人批准");
+        }
         return executeWrite(
                 "decideDeleteAmusementProjectEntitlement:" + id + ":" + type,
                 idempotencyKey,
@@ -448,7 +461,6 @@ public class AmusementService {
                 "amusement.project_entitlement.approve",
                 "amusement_project_entitlement",
                 () -> {
-                    checkProjectOwnership(id);
                     AmusementProjectEntitlementRecord existing = requireProjectEntitlement(id, type);
                     if (!"pending".equals(existing.deleteStatus())) {
                         throw ApiException.badRequest("PROJECT_ENTITLEMENT_DELETE_NOT_PENDING", "该权益绑定没有待审批的删除申请");
@@ -464,6 +476,11 @@ public class AmusementService {
         );
     }
 
+    /** BSM 第 570 行：配置数据删除审批与墓碑恢复只认持授权 U 盘密钥的最高权限本人；这些单据级路径对其逐笔放行（经 executeWrite 留痕）。 */
+    private boolean isMasterActor() {
+        return authorizationService.isMasterAuthority(authorizationService.currentActor());
+    }
+
     private AmusementProjectEntitlementRecord requireProjectEntitlement(String projectId, String entitlementType) {
         return repository.findProjectEntitlement(projectId, entitlementType)
                 .orElseThrow(() -> ApiException.notFound("PROJECT_ENTITLEMENT_NOT_FOUND", "游乐项目权益绑定不存在或已被删除"));
diff --git a/backend/src/main/java/com/complex/platform/common/api/FieldLabels.java b/backend/src/main/java/com/complex/platform/common/api/FieldLabels.java
index 51d75c79..39861e21 100644
--- a/backend/src/main/java/com/complex/platform/common/api/FieldLabels.java
+++ b/backend/src/main/java/com/complex/platform/common/api/FieldLabels.java
@@ -72,6 +72,7 @@ public final class FieldLabels {
         p("org_category", "组织类别");
         p("entity_name", "主体名称");
         p("entity_role", "主体角色");
+        p("business_entity_id", "经营主体");
         p("subject_id", "主体");
         p("subject_type", "主体类型");
         p("partner_id", "合作方");
diff --git a/backend/src/main/java/com/complex/platform/facility/FacilityEquipmentService.java b/backend/src/main/java/com/complex/platform/facility/FacilityEquipmentService.java
index 891c8edc..4607ceb7 100644
--- a/backend/src/main/java/com/complex/platform/facility/FacilityEquipmentService.java
+++ b/backend/src/main/java/com/complex/platform/facility/FacilityEquipmentService.java
@@ -108,12 +108,21 @@ public class FacilityEquipmentService {
         repository.updateEquipment(updated);
         auditEventService.recordSuccess("facility.equipment_delete_requested", PERM, "facility_equipment", id,
                 null, null, Map.of());
+        if (authorizationService.isMasterAuthority(authorizationService.currentActor())) {
+            // BSM 第 577 行：持授权 U 盘密钥的最高权限本人发起的删除不需要经过任何审批（占用检查照走）。
+            approveDelete(id, new com.complex.platform.facility.dto.FacilityEquipmentDeleteApprovalRequest("approved", null));
+            return updated.withDelete("approved", updated.deleteRequestedBy(), now, reason, now);
+        }
         return updated;
     }
 
     @Transactional
     public com.complex.platform.facility.dto.FacilityEquipmentDeleteDecision approveDelete(String id,
             com.complex.platform.facility.dto.FacilityEquipmentDeleteApprovalRequest request) {
+        // BSM 第 577 行：设施设备删除一律须由具备最高权限的人审批通过后执行，不设例外。
+        if (!authorizationService.isMasterAuthority(authorizationService.currentActor())) {
+            throw ApiException.forbidden("MASTER_APPROVAL_REQUIRED", "设施设备删除须由最高权限本人批准");
+        }
         authorizationService.requirePermission("facility.equipment.approve");
         FacilityEquipmentRecord cur = load(id);
         if (!"pending".equals(cur.deleteStatus())) {
@@ -124,10 +133,7 @@ public class FacilityEquipmentService {
         if (!"approved".equals(decision) && !"rejected".equals(decision)) {
             throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
         }
-        String actor = archiveActorId();
-        if (actor != null && actor.equals(cur.deleteRequestedBy())) {
-            throw ApiException.forbidden("FACILITY_EQUIPMENT_SELF_APPROVE_FORBIDDEN", "删除申请发起人不能审批自己的申请");
-        }
+        // 审批人只能是最高权限本人；最高权限发起的删除直接生效、不经审批，普通申请人无从审批自己的申请（BSM 第 577 行）。
         if ("rejected".equals(decision)) {
             repository.updateEquipment(cur.withDelete("none", null, null, null, Instant.now()));
             auditEventService.recordSuccess("facility.equipment_delete_rejected", "facility.equipment.approve",
diff --git a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
index f4c086ab..0c8d1847 100644
--- a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
@@ -394,7 +394,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 INSERT INTO space_type_delete_requests (id, space_type_id, reason, requested_by, requested_at, status)
                 VALUES (:id, :space_type_id, :reason, :requested_by, :requested_at, 'pending')
                 """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
-                .addValue("id", "stdr_" + spaceTypeId + "_" + java.time.Instant.now().toEpochMilli())
+                .addValue("id", "stdr_" + java.util.UUID.randomUUID())
                 .addValue("space_type_id", spaceTypeId)
                 .addValue("reason", reason)
                 .addValue("requested_by", requestedBy)
diff --git a/backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java
index 305993f4..0b0aaf99 100644
--- a/backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java
@@ -107,4 +107,15 @@ class StaffProfileArchiveDeleteJdbcIntegrationTests {
                 .isInstanceOf(ApiException.class)
                 .hasMessageContaining("最高权限");
     }
+
+    @Test
+    void masterRequestedDeletionTakesEffectDirectly() {
+        TestSecurityActors.asPlatformAdmin();
+        seed("staff_master", "user_master_1");
+        service.archiveProfile("staff_master", true, "测试存档理由");
+        StaffTestActors.master();
+        StaffProfileRecord deleted = service.requestDeleteProfile("staff_master", new StaffProfileDeleteRequest("最高权限直接删除"));
+        assertThat(deleted.deleteStatus()).isEqualTo("approved");
+        assertThat(repository.findProfileById("staff_master")).isEmpty();
+    }
 }
diff --git a/backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java b/backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java
index eaa3385b..017dd851 100644
--- a/backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java
@@ -1482,11 +1482,13 @@ class AmusementServiceTests {
         assertThat(pending.deleteReason()).isEqualTo("配错了不再用");
 
         // 审批通过 = 落墓碑，行仍在库（includeArchived 可见），默认视图不显示。
-        // 职责分离：换一名平台管理员审批（同人审批见 selfApprove 用例）。
-        TestSecurityActors.asPlatformAdmin("usr_admin_approver");
+        // BSM 第 570 行：删除由最高权限本人审批（非最高权限见 nonMaster 用例）。
+        asMaster();
         AmusementProjectEntitlementRecord tomb = service.decideDeleteProjectEntitlement(
                 project.id(), "amusement_pass", "approved", "ent-del-appr");
         assertThat(tomb.deleteStatus()).isEqualTo("approved");
+        // 列表视图由运营侧平台管理员查看（最高权限的列表口径另见登记 XM-M036A-1，本批不改）。
+        TestSecurityActors.asPlatformAdmin();
         assertThat(service.listProjectEntitlements(project.id()).items()).isEmpty();
         assertThat(service.listProjectEntitlements(project.id(), true).items()).hasSize(1);
         assertThat(amusementRepository.listProjectEntitlementTypes(project.id())).isEmpty();
@@ -1500,7 +1502,7 @@ class AmusementServiceTests {
         service.archiveProjectEntitlement(project.id(), "amusement_pass", "先下线", "ent-rej-arch");
         service.requestDeleteProjectEntitlement(project.id(), "amusement_pass", "误提", "ent-rej-req");
 
-        TestSecurityActors.asPlatformAdmin("usr_admin_approver");
+        asMaster();
         AmusementProjectEntitlementRecord decided = service.decideDeleteProjectEntitlement(
                 project.id(), "amusement_pass", "rejected", "ent-rej-dec");
         assertThat(decided.deleteStatus()).isEqualTo("none");
@@ -1508,36 +1510,57 @@ class AmusementServiceTests {
     }
 
     @Test
-    void deleteRequesterCannotApproveOwnProjectEntitlementDeletion() {
+    void nonMasterCannotApproveProjectEntitlementDeletion() {
         AmusementProjectRecord project = projectWithTemplate("ent-self");
         service.createProjectEntitlement(project.id(),
                 new AmusementProjectEntitlementRequest("ent-self-key", null, List.of(), "amusement_pass"));
         service.archiveProjectEntitlement(project.id(), "amusement_pass", "先下线", "ent-self-arch");
         service.requestDeleteProjectEntitlement(project.id(), "amusement_pass", "误配", "ent-self-req");
 
-        // 发起人=当前主体（usr_admin）→ 禁自审。
+        // 发起人本人与另一名平台管理员都不是最高权限 → 不能审批，申请仍待审批（BSM 第 570 行）。
         assertThatThrownBy(() -> service.decideDeleteProjectEntitlement(
                 project.id(), "amusement_pass", "approved", "ent-self-appr"))
                 .isInstanceOf(ApiException.class).extracting("code")
-                .isEqualTo("PROJECT_ENTITLEMENT_SELF_APPROVE_FORBIDDEN");
+                .isEqualTo("MASTER_APPROVAL_REQUIRED");
+        TestSecurityActors.asPlatformAdmin("usr_admin_approver");
+        assertThatThrownBy(() -> service.decideDeleteProjectEntitlement(
+                project.id(), "amusement_pass", "approved", "ent-other-appr"))
+                .isInstanceOf(ApiException.class).extracting("code")
+                .isEqualTo("MASTER_APPROVAL_REQUIRED");
+        assertThat(service.listProjectEntitlements(project.id(), true).items())
+                .extracting(AmusementProjectEntitlementRecord::deleteStatus).containsExactly("pending");
+    }
+
+    @Test
+    void masterRequestedProjectEntitlementDeletionTakesEffectDirectly() {
+        AmusementProjectRecord project = projectWithTemplate("ent-master");
+        service.createProjectEntitlement(project.id(),
+                new AmusementProjectEntitlementRequest("ent-master-key", null, List.of(), "amusement_pass"));
+        service.archiveProjectEntitlement(project.id(), "amusement_pass", "先下线", "ent-master-arch");
+        asMaster();
+        AmusementProjectEntitlementRecord tomb = service.requestDeleteProjectEntitlement(
+                project.id(), "amusement_pass", "最高权限直接删除", "ent-master-req");
+        assertThat(tomb.deleteStatus()).isEqualTo("approved");
+        assertThat(amusementRepository.listProjectEntitlementTypes(project.id())).isEmpty();
     }
 
     @Test
-    void tombstonedProjectEntitlementRestoreRequiresApprovePermission() {
+    void tombstonedProjectEntitlementRestoreRequiresMasterAuthority() {
         AmusementProjectRecord project = projectWithTemplate("ent-tomb");
         service.createProjectEntitlement(project.id(),
                 new AmusementProjectEntitlementRequest("ent-tomb-key", null, List.of(), "amusement_pass"));
         service.archiveProjectEntitlement(project.id(), "amusement_pass", "先下线", "ent-tomb-arch");
         service.requestDeleteProjectEntitlement(project.id(), "amusement_pass", "作废", "ent-tomb-req");
-        TestSecurityActors.asPlatformAdmin("usr_admin_approver");
+        asMaster();
         service.decideDeleteProjectEntitlement(project.id(), "amusement_pass", "approved", "ent-tomb-appr");
 
-        // 权限库里没有 amusement.project_entitlement.approve（permissionExists=false）→ 墓碑恢复被拒。
+        // 平台管理员与最高权限是两个角色：平台管理员不能复活墓碑。
+        TestSecurityActors.asPlatformAdmin("usr_admin_approver");
         assertThatThrownBy(() -> service.restoreProjectEntitlement(project.id(), "amusement_pass", "ent-tomb-res1"))
-                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("PERMISSION_DENIED");
+                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("MASTER_APPROVAL_REQUIRED");
 
-        // 授予审批权（最高权限 role_platform_admin 持有）后墓碑可恢复——删除铁律「可恢复」。
-        when(accessRepository.permissionExists("amusement.project_entitlement.approve")).thenReturn(true);
+        // 最高权限本人可复活墓碑——删除「可恢复」（BSM 第 571 行）。
+        asMaster();
         AmusementProjectEntitlementRecord restored =
                 service.restoreProjectEntitlement(project.id(), "amusement_pass", "ent-tomb-res2");
         assertThat(restored.deleteStatus()).isEqualTo("none");
@@ -1545,6 +1568,16 @@ class AmusementServiceTests {
         assertThat(amusementRepository.listProjectEntitlementTypes(project.id())).containsExactly("amusement_pass");
     }
 
+    /** 持授权 U 盘密钥的最高权限本人（生产会话形状：master_authority，所属公司 platform-company）。 */
+    private static void asMaster() {
+        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
+                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
+                        new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME,
+                                com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"),
+                        "token", java.util.List.of()));
+    }
+
     @Test
     void decideDeleteProjectEntitlementRejectsUnknownDecision() {
         AmusementProjectRecord project = projectWithTemplate("ent-dec");
diff --git a/backend/src/test/java/com/complex/platform/compliance/ComplianceControllerTests.java b/backend/src/test/java/com/complex/platform/compliance/ComplianceControllerTests.java
index a5dee424..8a775e86 100644
--- a/backend/src/test/java/com/complex/platform/compliance/ComplianceControllerTests.java
+++ b/backend/src/test/java/com/complex/platform/compliance/ComplianceControllerTests.java
@@ -71,7 +71,7 @@ class ComplianceControllerTests {
 
     @Test
     void createContractKeyDateWithoutIdempotencyKeyReturnsValidationFailed() throws Exception {
-        mockMvc.perform(post("/api/v1/compliance/contract-key-dates")
+        mockMvc.perform(post("/api/v1/compliance/contract-key-dates").requestAttr("com.complex.platform.gate.verified_terminal", new com.complex.platform.gate.TerminalIdentityHolder.VerifiedTerminal("web", null, true))
                         .with(user("ops").roles("P0_SMOKE"))
                         .contentType(MediaType.APPLICATION_JSON)
                         .content("""
@@ -93,7 +93,7 @@ class ComplianceControllerTests {
     void createContractKeyDateRequiresManagePermissionAndReturnsRecord() throws Exception {
         when(service.createContractKeyDate(any(ContractKeyDateRequest.class))).thenReturn(contractDateRecord());
 
-        mockMvc.perform(post("/api/v1/compliance/contract-key-dates")
+        mockMvc.perform(post("/api/v1/compliance/contract-key-dates").requestAttr("com.complex.platform.gate.verified_terminal", new com.complex.platform.gate.TerminalIdentityHolder.VerifiedTerminal("web", null, true))
                         .with(user("ops").roles("P0_SMOKE"))
                         .contentType(MediaType.APPLICATION_JSON)
                         .content("""
@@ -153,7 +153,7 @@ class ComplianceControllerTests {
                 ))
         ));
 
-        mockMvc.perform(post("/api/v1/compliance/reminders/scan")
+        mockMvc.perform(post("/api/v1/compliance/reminders/scan").requestAttr("com.complex.platform.gate.verified_terminal", new com.complex.platform.gate.TerminalIdentityHolder.VerifiedTerminal("web", null, true))
                         .with(user("ops").roles("P0_SMOKE"))
                         .contentType(MediaType.APPLICATION_JSON)
                         .content("""
diff --git a/backend/src/test/java/com/complex/platform/facility/FacilityEquipmentArchiveDeleteJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/facility/FacilityEquipmentArchiveDeleteJdbcIntegrationTests.java
index f6cb05ab..c20a9c92 100644
--- a/backend/src/test/java/com/complex/platform/facility/FacilityEquipmentArchiveDeleteJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/facility/FacilityEquipmentArchiveDeleteJdbcIntegrationTests.java
@@ -50,7 +50,7 @@ class FacilityEquipmentArchiveDeleteJdbcIntegrationTests {
         seed("feq_del");
         service.archive("feq_del", true, "测试存档理由");
         service.requestDelete("feq_del", new FacilityEquipmentDeleteRequest("报废"));
-        TestSecurityActors.asPlatformAdmin("usr_approver");
+        asMaster(); // BSM 第 577 行：删除由最高权限本人审批
         FacilityEquipmentDeleteDecision decision = service.approveDelete(
                 "feq_del", new FacilityEquipmentDeleteApprovalRequest("approved", null));
         assertThat(decision.deleted()).isTrue();
@@ -86,7 +86,7 @@ class FacilityEquipmentArchiveDeleteJdbcIntegrationTests {
                 INSERT INTO facility_work_orders (id, code, work_order_type, title, equipment_id, created_at, updated_at)
                 VALUES ('fwo_use', 'FWO-USE-1', 'repair', '设备维修', 'feq_use', ?, ?)
                 """, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
-        TestSecurityActors.asPlatformAdmin("usr_approver");
+        asMaster(); // BSM 第 577 行：删除由最高权限本人审批
         assertThatThrownBy(() -> service.approveDelete("feq_use",
                 new FacilityEquipmentDeleteApprovalRequest("approved", null)))
                 .isInstanceOf(ApiException.class)
@@ -94,7 +94,7 @@ class FacilityEquipmentArchiveDeleteJdbcIntegrationTests {
     }
 
     @Test
-    void selfApproveForbidden() {
+    void ordinaryApplicantCannotApproveMasterOnlyDeletion() {
         TestSecurityActors.asPlatformAdmin("usr_same");
         seed("feq_self");
         service.archive("feq_self", true, "测试存档理由");
@@ -102,6 +102,31 @@ class FacilityEquipmentArchiveDeleteJdbcIntegrationTests {
         assertThatThrownBy(() -> service.approveDelete("feq_self",
                 new FacilityEquipmentDeleteApprovalRequest("approved", null)))
                 .isInstanceOf(ApiException.class)
-                .hasMessageContaining("发起人");
+                .hasMessageContaining("最高权限");
+        assertThat(jdbcTemplate.queryForObject(
+                "SELECT delete_status FROM facility_equipment WHERE id = ?", String.class, "feq_self")).isEqualTo("pending");
+    }
+
+    @Test
+    void masterRequestedDeletionTakesEffectDirectly() {
+        TestSecurityActors.asPlatformAdmin();
+        seed("feq_master");
+        service.archive("feq_master", true, "测试存档理由");
+        asMaster();
+        FacilityEquipmentRecord deleted = service.requestDelete("feq_master", new FacilityEquipmentDeleteRequest("最高权限直接删除"));
+        assertThat(deleted.deleteStatus()).isEqualTo("approved");
+        assertThat(jdbcTemplate.queryForObject(
+                "SELECT delete_status FROM facility_equipment WHERE id = ?", String.class, "feq_master")).isEqualTo("approved");
+        assertThat(repository.findEquipmentById("feq_master")).isEmpty();
+    }
+
+    /** 持授权 U 盘密钥的最高权限本人（生产会话形状：master_authority，所属公司 platform-company）。 */
+    private static void asMaster() {
+        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
+                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
+                        new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME,
+                                com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"),
+                        "token", java.util.List.of()));
     }
 }
diff --git a/backend/src/test/java/com/complex/platform/finance/FinanceControllerTests.java b/backend/src/test/java/com/complex/platform/finance/FinanceControllerTests.java
index 0c570c9b..1c6d668d 100644
--- a/backend/src/test/java/com/complex/platform/finance/FinanceControllerTests.java
+++ b/backend/src/test/java/com/complex/platform/finance/FinanceControllerTests.java
@@ -52,6 +52,10 @@ class FinanceControllerTests {
     @Autowired
     private MockMvc mockMvc;
 
+    // StoredValueService 有必需的 @Autowired 字段；@Bean 交出的 mock 仍会被字段注入，故用 @MockitoBean。
+    @org.springframework.test.context.bean.override.mockito.MockitoBean
+    private com.complex.platform.customer.StoredValueService storedValueService;
+
     @Autowired
     private FinanceService service;
 
@@ -416,11 +420,6 @@ class FinanceControllerTests {
             return Mockito.mock(AuthorizationService.class);
         }
 
-        @Bean
-        com.complex.platform.customer.StoredValueService storedValueService() {
-            return Mockito.mock(com.complex.platform.customer.StoredValueService.class);
-        }
-
         @Bean
         AuthService authService() {
             return new AuthService("", "");
diff --git a/backend/src/test/java/com/complex/platform/gate/CustomerGuardedActionAndRejectionAuditTests.java b/backend/src/test/java/com/complex/platform/gate/CustomerGuardedActionAndRejectionAuditTests.java
index f0e4fa4e..79eff26a 100644
--- a/backend/src/test/java/com/complex/platform/gate/CustomerGuardedActionAndRejectionAuditTests.java
+++ b/backend/src/test/java/com/complex/platform/gate/CustomerGuardedActionAndRejectionAuditTests.java
@@ -39,7 +39,7 @@ class CustomerGuardedActionAndRejectionAuditTests {
                 // 储值（资金：充值/消费/退卡/冻结）
                 Arguments.of(StoredValueService.class, "topup", "customer.stored_value.topup"),
                 Arguments.of(StoredValueService.class, "settleTopup", "customer.stored_value.topup"),
-                Arguments.of(StoredValueService.class, "consume", "customer.stored_value.consume"),
+                Arguments.of(StoredValueService.class, "payTicketOrder", "customer.stored_value.consume"),
                 Arguments.of(StoredValueService.class, "refund", "customer.stored_value.refund"),
                 Arguments.of(StoredValueService.class, "freeze", "customer.stored_value.freeze"),
                 Arguments.of(StoredValueService.class, "unfreeze", "customer.stored_value.freeze"),
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
index 40f0068a..6ce23ea1 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
@@ -560,6 +560,19 @@ class PlatformJdbcIntegrationTests {
                 Integer.class, "m009_type_request")).isZero();
     }
 
+    @Test
+    void backToBackSpaceTypeDeleteRequestsNeverCollideOnId() {
+        Instant now = Instant.now();
+        repository.saveSpaceType(new SpaceTypeRecord("m036a_type_burst", "m036a_type_burst", "连续申请", 100, "archived", now, now));
+        for (int i = 0; i < 50; i++) {
+            repository.saveSpaceTypeDeleteRequest("m036a_type_burst", "第" + i + "次申请", "actor-" + i);
+            repository.completeSpaceTypeDeleteRequest("m036a_type_burst", "rejected");
+        }
+        assertThat(jdbcTemplate.queryForObject(
+                "SELECT count(*) FROM space_type_delete_requests WHERE space_type_id = ?",
+                Integer.class, "m036a_type_burst")).isEqualTo(50);
+    }
+
 
     @Test
     @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
```
