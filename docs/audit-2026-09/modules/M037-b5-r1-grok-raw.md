CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M037 B5-r1 修复后整模块全量审计（Grok 席乙）

- 席位：乙（Grok 4.6 / reasoning xhigh）
- 步骤：B5-r1（B3 累计 1 之后的整模块三线重审，不只看 diff，不复用 B2/B4 票面）
- 报告版本：V0.01（指定路径原先不存在，本次创建）
- 控制提示词：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 B1/B5 及三线
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 — M037 功能权限点、职级三十档、权限模板与最终权限计算／后台验证／第3段（1 文件）
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11（本席独立三工具实算，见首行）
- 正本：`/home/james/complex-management` main 主工作区
- findings：V3.16；M037-001、002 队列「否（已修）」；不把全部否当成模块已全对
- 互盲：未打开 `modules/M037-b5-r1-self-raw.md`。全仓检索曾命中该路径一行片段，按派发令**丢弃不采信**。旧 B1/B2/B4 可读。
- 本席结论：**PASS**（1 文件两版全覆盖；52 项逐项记录；三线完成；发布口径 39/39 绿；001/002 对应闸本席独立变异均红；无新入队缺陷；W-M037-1…2 仍只在未发布版成立，不入队）

以上仅为席乙本轮 B5-r1 票，不等于双席合取，也不等于模块收口。

---

## 0. 技能预检（本环境实核，按收窄口径用）

本席按 V0.17 §6 在本机路径核验，不抄席甲预检表。用户指令优先：只允许两席、禁止 MCP/子代理/第三席、不问人、不修业务/HTML/锁/findings/ledger、不 fetch/commit/push/merge/deploy、除一次性 worktree 外不建脚本日志临时产物、不对任何数据库执行写语句。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工先核技能 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 识别 Arc 默认全量审计与本轮冲突，**不执行** Arc `audit` 工作流 | 本体要派评审子代理并写 `docs/arc/audits/`，记缺口 |
| `graft` | `~/.claude/skills/graft/SKILL.md` | 文件已由计划点名；本席全文读测试类。收尾 `graft skeleton` 该文件一次 | 无 |
| `grilling` / `grill-me` | mattpocock `productivity/grilling/SKILL.md`；`grill-me` 仅转发 `/grilling` | 对抗线用追问树**自问自答**；作废等用户/派子代理 | 无 |
| `batch-grill-me` | — | — | **本机技能树不存在**，记缺口，不冒充 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | 用 seam/adapter 看 Mockito 仓储桩是否换成测试自己的合成 | 不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环=本席在 origin/main worktree 对目标闸做只读变异再还原；断言能否对目标症状变红 | 未加诊断桩 |
| `systematic-debugging` | superpowers | 只借读错误→追数据流；作废插桩与无 ID 改码 | 无 |
| `verification-before-completion` | superpowers | PASS 前本席实跑发布口径 39 绿、未发布对照 39 绿、五处目标闸变异红、occupancy/reauth 对照变异红、W 编译失败 | 无 |
| `tdd` / `test-driven-development` | 存在 | B5 只读：核已落断言的红绿能力，不新写测试 | 测缝须用户确认的本体作废 |
| `receiving-code-review` | superpowers | 只借「先核实再接受或反驳」；B5 禁止 IMPLEMENT | 无 |
| `requesting-code-review` | superpowers | 本席自身即外审；不用 `HEAD~1`/子代理 | 本体路径作废 |
| `ponytail` ultra | `ponytail/4.9.0/skills/ponytail/SKILL.md` | 不为理论路径补需求；B5 不修 | 无 |
| `ponytail-review` | 同包 | 只找可删的越界添加；本模块测试网 `Lean already` | 不落地 |
| `writing-for-agents` | mattpocock productivity | raw 按下席可复核的完成准则写 | 无 |
| `long-running-background-tasks` | grok bundled | Maven 同步跑完；变异命令曾被宿主后台化，本席等到 exit 0 再采信 | 无遗留 monitor |
| `git-guardrails-claude-code` | mattpocock misc | 存在；本席未 push/reset/clean；worktree remove 未用 `--force` | 未安装 hook |
| `i-have-adhd` | `~/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 结论先行、编号。与「raw 必须全文」冲突时 160 优先，**未按该技能压缩正文** | 无 |
| `resolving-merge-conflicts` | 存在 | 工作区无进行中 merge/rebase | 节点不适用 |
| Trail of Bits `audit-context-building` | 存在 | 本体强制 function-analyzer 子代理 | **不用**（两席） |
| Arc `audit` | howells | 见上 | **不用** |

明确不用（提示词划掉或本步冲突）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`（一次性 `git worktree add --detach origin/main` 是本轮用户/D-029 允许的只读核，不是该技能本体；核完已 `git worktree remove`，不用 `--force`）、`research`、mattpocock `code-review`、`domain-modeling`、`ponytail-audit`。未使用 MCP。未写 findings/ledger/HTML/业务代码。

graft 本轮 tokens saved ≈ 12,730（1 次 skeleton）。

---

## 1. 环境与锁（命令实算，非手抄）

本席哈希（独立，未把 sidecar/ledger/派发期望值当输入）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 hashlib.sha256(open(...,'rb').read()).hexdigest()
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 字节）。sidecar 一行同值、ledger 顶部同值：只作对照，不是本席填值来源。锁版本仍 V0.11。LOCK-AMEND-COUNT 仍为 1。未 LOCK-AMEND。findings V3.16 全文 SHA256 `563ac4e8d698573964c4e6eb675a6398b9bb610c63709df06371059286d49baf`。

| 项 | 值 |
|---|---|
| 主工作区 HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`（branch `main`） |
| origin/main | `40fcc8f1b3429d2874ccf856a404b214364decc7` |
| after tree（派发给定，本席 `git cat-file -t` = tree） | `4ad8f54ce984346dc01eb680498030753c92620f` |
| JAVA_HOME | `/usr/lib/jvm/temurin-21-jdk-amd64`（javac 21.0.12.1） |
| 本类 | 无 `@SpringBootTest`，纯 JUnit + Mockito，**不连库**。未 source `/etc/complex/complex.env`。未对任何数据库执行语句（含 SELECT） |
| 进程预存在 | `JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true`；主工作区另有 `COMPLEX_HOME`/`COMPLEX_CONFIG_DIR`/`COMPLEX_LOG_DIR`（非本席 export）。本类不读它们 |

本模块 1 文件两版（D-029）：

| 版本 | 行数 | SHA256 | git hash-object / blob | `@Test` | 文件头 |
|---|---|---|---|---|---|
| 主工作区工作树（= after tree 内容） | 833 | `ce3a9006cb546eb6cb0e743cbe6a47f4c7c2b979f60d41e5bd2b009b8de78f7d` | `1202d4a39804230addaa904342f089d4df9f7515` | 39 | V0.31 |
| 发布变体 `/home/james/audit-tmp/m037_M006SecurityRegressionTests.publish.java` | 835 | `c459a5d2246f05f40ea0a454fc6103e0bdf5ed07ac9cc89fb4f70e85ccd28273` | — | 39 | V0.31 |
| origin/main | 809 | `c2d8b8a69779b7c5555cc49c7776f233b0e16bc875c22bfc96149799f48698c3` | — | 39 | （未打本轮补丁） |

`git show 4ad8f54…:…M006SecurityRegressionTests.java | sha256sum` = 工作树 SHA，逐字节相同。工作区该路径 `git status --porcelain` 为 `MM`（index blob `3629dd5d…` 与 HEAD blob `05b9afeb…` 均不同于工作树）；本席审的是工作树=after tree 这一版，未改暂存。测试方法名集合两版相同。

发布口径 = origin/main 的 SUT + 发布变体测试（origin 版 + M037-001 + M037-002）。主工作区版另含未发布工作流夹具（`SessionIssuer.MASTER_ACCOUNT_TYPE`、排期夹具、建模板主体 `template-admin`），按 D-029 分开写。

被测追证（只读，不重开已闭环模块）：origin worktree 的 `AccessService.updatePermissionTemplate` L174–175 两闸、`changePositionTemplateLifecycleAudited` L500–502 / L538–555、`requireActorCanGrant` L810–821、`requireGrantApprovalIfNeeded` L1893–1901、`requirePlatformAdmin` L2216–2220。主工作区 `AuthorizationService.isMasterAuthority` 依赖 `MASTER_ACCOUNT_TYPE`（origin `SessionIssuer` **无该常量**；origin `isMasterAuthority` = `isPlatformAdminBridge` ∧ `MASTER_USER_ID` ∧ `MASTER_USERNAME`）。

---

## 2. 实跑（2.3 类型 2 / 4）

### 2.1 发布口径：origin/main 一次性 worktree

```
git worktree add --detach /home/james/wt-m037-b5-r1 origin/main
# HEAD = 40fcc8f1b3429d2874ccf856a404b214364decc7
cp /home/james/audit-tmp/m037_M006SecurityRegressionTests.publish.java \
   $WT/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
# 拷入后 sha256 = c459a5d2… 与发布变体 IDENTICAL
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
mvn -f backend/pom.xml -Dtest=com.complex.platform.access.M006SecurityRegressionTests test
```

结果（exit 0）：

```
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
Time elapsed: 1.034 s
BUILD SUCCESS  Total time: 9.245 s
Finished at: 2026-09-21T05:33:40-07:00
```

未连库。未拷迁移。

### 2.2 未发布对照：主工作区工作树测试 + 未发布 SUT（非发布口径）

```
cd /home/james/complex-management/backend
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 mvn -o -Dtest=com.complex.platform.access.M006SecurityRegressionTests test
```

```
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
Time elapsed: 1.229 s
BUILD SUCCESS  Total time: 2.152 s
```

按 D-029 分开写，不把未发布全绿当成发布口径。

### 2.3 本席独立变异（发布口径 worktree，改完即 `git checkout -- AccessService.java`）

对象：origin/main `AccessService.java`。测试：发布变体。每步只改一处。

| # | 变异 | 跑的测试 | 结果 | 原句/行（发布变体） |
|---|---|---|---|---|
| 1 | 删 `updatePermissionTemplate` 里对 `added` 的 `requireActorCanGrant` | `templateEditCannotGrantAnUnownedHighPermission` | **红** L186：expected `GRANT_EXCEEDS_OWN_PERMISSIONS` but was `GRANT_REQUIRES_APPROVAL` | `assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS")` 对不持有的 HIGH |
| 2 | 删同处 `requireGrantApprovalIfNeeded(added)` | 同上 | **红** L190：`Expecting code to raise a throwable` | 持有 HIGH 后须 `GRANT_REQUIRES_APPROVAL`；闸没了则写入成功 |
| 3 | 跳过 `requirePlatformAdmin(actor)` | `positionTemplateLifecycle…` | **红** L597：`Expecting code to raise a throwable` | staff `archive` 须抛 |
| 4 | `boolean approve = true`（`reject_delete` 当批准） | 同上 | **红** L618：`deletedAt` expected null but was 时间戳 | `assertThat(mapping.get().deletedAt()).isNull()` |
| 5 | 跳过 `MASTER_AUTHORITY_REQUIRED` 抛出 | 同上 | **红** L611：expected `MASTER_AUTHORITY_REQUIRED` but was `SUPERVISOR_SELF_REVIEW_FORBIDDEN` | 正是 002③ 原先假绿的替代异常 |

对照（不升队，只验证 O-1「单一守卫」是否仍成立）：

| 对照 | 变异 | 结果 |
|---|---|---|
| 占用 | `requirePositionTemplateUnused` 空实现 | **红**（`Tests run: 1, Failures: 1`）。占用闸拿掉后 in-use 的 `request_delete` 不再抛，`isInstanceOf(ApiException)` 变红——属「抛 vs 成功」单一守卫，不是 002③ 那种「换一个 ApiException 仍绿」 |
| 扩权须重授权 | `TEMPLATE_REAUTHORIZATION_REQUIRED` 条件恒假 | **红**（同上）。延长/取消一次性成功则 `thrownBy` 失败 |

五处目标闸全部能把对应测试打红。001 的第三码（不持有、不须审批的 `ops.production_readiness.check`）在变异 1 时若只靠 HIGH 会走到审批闸；现有第二支断言锁日常未持有码。变异 5 证明错误码收紧锁住了「自审顶替最高权限闸」。

### 2.4 W-M037-1 编译探针（仍不入队）

同一 origin/main worktree 把**主工作区未发布测试**拷进去 `test-compile`：

```
cannot find symbol: variable MASTER_ACCOUNT_TYPE
location: interface com.complex.platform.auth.SessionIssuer
…Tests.java:[427,121]
…Tests.java:[613,121]
```

exit ≠ 0。确认只在未发布版成立。随后 `git checkout --` 还原测试文件与 `AccessService.java`，`git status --porcelain` 空，`git worktree remove /home/james/wt-m037-b5-r1`（**未** `--force`），目录已不存在。主工作区测试 SHA 仍为 `ce3a9006…`。

---

## 3. 全文覆盖（不抽样）

范围只有 `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java`。主工作区 833 行与发布变体 835 行均通读。夹具：`mock(AccessRepository)` 对接口 default 走 `CALLS_REAL_METHODS`；`AuthorizationService`/`AccessService` 真实 `new`（两处控制器故意 mock 门禁除外）。无 `TestInstance(PER_CLASS)`；`@BeforeEach` 新实例；`@AfterEach` 清 `SecurityContextHolder`。测试之间不共享可变状态，无顺序依赖。`reviews()` / 生命周期测试内重赋 `service` 只作用于当次方法。

### 3.1 每个 `@Test`（行号：工作区 / 发布变体若不同则另注 PB）

| WS 行 | 测试 | 执行声称行为？ | 改坏实现会红？ | 备注 |
|---|---|---|---|---|
| 105–113 | administratorDenyWinsAndHighPermissionNeedsExplicitGrant | 是：DENY 后 require/has/effective；再显式 GRANT HIGH | 能。`assertDenied` 核 `PERMISSION_DENIED` | **两版同** |
| 116–120 | administratorKeepsDailyPermissionsWithoutAutomaticHighRole | 是：日常日桥；`role_platform_admin` 的 HIGH 被滤 | 能 | **两版同** |
| 124–136 | administratorHoldsGovernanceWritePermissionsFromDefaultRoleButNotBusinessHighRisk | 是：治理本职码；资金 HIGH 不行；再 DENY | 能 | 注释引未映射 BSM-007/PGM-040，见顺从式。**两版同** |
| 139–143 | untrustedPrincipalIsRejected | 是：String principal → `currentActor` | 能（不抛则红）。未核码 | O-1 单一守卫。**两版同** |
| 146–151 | missingOrganizationCannotListAllUsersOrResolveGlobalProject | 是 | 能。未核码 | O-1。**两版同** |
| 154–165 | organizationReadWorksButCrossMerchantWriteDoesNot | 是 | 能：`never().assignUserRole` | 写入闸能红。未核码 O-1。**两版同** |
| 168–173 | collidingUserSlugsRemainDifferentUsers | 是 | 能 | **两版同** |
| 177–193 | templateEditCannotGrantAnUnownedHighPermission | 是：真实 create/update | **能（本席变异 1、2）** | **M037-001 已修，两版同文**：不持有 HIGH → `GRANT_EXCEEDS_OWN_PERMISSIONS`；不持有日常第三码 → 同码；持有 HIGH → `GRANT_REQUIRES_APPROVAL`；三项后 `items` 仍仅 LOW |
| 196–204 | templateEditPreservesOwnerAndRejectsMissingTemplate | 是 | 能 | 未核码 O-1。**两版同** |
| 207–217 | emptyPermissionsAndInvertedWindowAreRejectedBeforeGrantWrite | 是 | 能：`never().createTemporaryGrant` | **两版同** |
| 220–227 | invalidStatusAndBlankPasswordHaveNoWrites | 是 | 能：`never().createUser` + `verifyNoInteractions(credentials)` | **两版同** |
| 230–234 | manualAccountDoesNotPersistOrReturnPhoneDigest | 是 | 能 | **两版同** |
| 237–239 | administratorWithoutApprovalRoleHasNoSupervisorLevel | 是 | 能（非 0 则红） | **两版同** |
| 242–258 | twoPermissionReviewConsumesOneSourceOnlyAfterBothChecksPass | 是：真实 AuthorizationService；AccessService 故意 mock | 能：缺第二码 `consumed` 空 | **两版同** |
| 261–274 | credentialAdapterDisablesInvitedAccountsAndRejectsBlankPassword | 是：真实 Jdbc provisioner | 能：status=disabled；空白后 `verifyNoMoreInteractions` | **两版同** |
| 277–283 | developmentCredentialAdapterRejectsMissingAccount | 是 | 能：`verifyNoInteractions(auth)` | **两版同** |
| 286–301 | changedRoleCannotBeAppliedUnderAnOldApproval | 是 | 能：核 `GRANT_SNAPSHOT_CHANGED`；`never().approve` | **两版同** |
| 304–317 | beneficiaryCannotApproveOwnGrant | 是 | 能：核 `BENEFICIARY_SELF_REVIEW_FORBIDDEN` | **两版同** |
| 335–365 | positionChangeDropsDerivedRoleButKeepsIndependentAssignment | 是：真实定岗 → `replacePositionGrants` | 对仓储指针能红；不经 `effectivePermissions()`（O-2） | WS：`template-admin`+platform_admin（W-M037-1）。PB：`MASTER_USER_ID`+`platform_admin` 后再写 AccessActor |
| 369–382 / PB 373–386 | appliedTemplateCannotExtendTimeOrRemoveSingleUse | 是 | **能（对照变异 reauth 红）** | 未核 `TEMPLATE_REAUTHORIZATION_REQUIRED`；属抛 vs 成功单一守卫。缩权成功路径有断言 |
| 385–393 / PB 389–397 | pendingTemporaryGrantRejectsInvertedTimeWindow | 是 | 能：`never().blockForAdmission` | **两版同** |
| 396–431 / PB 400–430 | topAuthorityReviewRequiresMasterSessionRatherThanRankAlone | 是：真实 `SupervisorReviewService.approve` | PB：职级 30 只 `isInstanceOf(ApiException)` + `never().saveSupervisorReview`——写入闸能红。WS：收紧 `MASTER_AUTHORITY_REQUIRED` + `findSupervisorReviewSchedule` + `MASTER_ACCOUNT_TYPE`（W-M037-1、W-M037-2） | 被测属复核模块，本文件只读追证 |
| 435–443 / PB 434–442 | manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval | 是 | 能：`never().createUser` | **两版同** |
| 446–451 / PB 445–450 | accountJsonDoesNotExposeStoredPhoneDigest | 是 | 能 | **两版同** |
| 454–461 / PB 453–460 | missingCredentialAdapterCannotReportSuccessfulCreation | 是 | 能：`never().createUser` | **两版同** |
| 466–487 / PB 465–486 | employeeApprovalWithoutCredentialAdapterWritesNoAccount | 是 | 能：`never().createUser` | 反射注入 `registrations`（O-5） |
| 490–524 / PB 489–523 | registrationCreatesAccountOnlyAfterCompanyApproval | 是 | 能：商户自批 `thrownBy`（不抛则红）；公司批后角色+凭证 | **两版同** |
| 528–531 / PB 527–530 | summaryQueryCannotOverrideBusinessThresholds | 是 | 能 | 未核码 O-1。**两版同** |
| 534–541 / PB 533–540 | referenceDictionaryDoesNotPretendItWasTruncatedAtFifty | 是 | 能：`pageSize==55` | **两版同** |
| 544–549 / PB 543–548 | grantListHasADefaultTimeWindow | 是 | 能：cutoff null 则红 | 不断言 30 天（O-3）。**两版同** |
| 553–560 / PB 552–559 | directPositionTemplateDeleteCannotBypassArchiveAndApproval | 是 | 能：`never().deletePositionRoleTemplate` | **两版同** |
| 564–634 / PB 563–634 | positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval | 是：存档/占用/申请/主会话驳回/再申请/批准/恢复/自审 | **能（本席变异 3、4、5；占用对照亦红）** | **M037-002**：见 §7。PB 另有 `never().savePositionTemplateLifecycle`。WS 无该 verify，但 `PLATFORM_ADMIN_REQUIRED` 的 `thrownBy` 在闸缺失时仍红（抛 vs 成功） |
| 681–705 | companyRequestsHaveIndependentResultsAndRetryOnlyOnce | 是：真实控制器 + 幂等按组织分钥 | 能 | **两版同** |
| 709–726 | invalidAccountTypeResponseIsChinese | 是：控制器 + 反射全局处理器 | 能：码 + 中文 + 无拉丁 | **两版同** |
| 732–754 | temporaryGrantMissingValidUntilAndUnauthenticatedMessagesAreChinese | 是 | 能 | **两版同** |
| 756–766 | invalidDecisionMessageIsChinese | 反射 `normalizeDecision` | 能 | O-4。**两版同** |
| 771–784 | platformAdminEffectivePermissionsDoNotQueryDictionaryPerPermission | 是 | 能：日常条数或调用次数>2 则红 | **两版同** |
| 788–795 | orphanStaffCopyingOrganizationTemplateIsRejectedNotCrashed | 是 | 能：核 `PROJECT_SCOPE_REQUIRED` | **两版同** |
| 800–831 | staffLevelChangeRederivesPositionGrants | 是 | 同岗位测试：锁仓储指针（O-2） | WS：`template-admin`（W-M037-1）。PB：MASTER_USER_ID+platform_admin |

辅助方法已读，无独立 `@Test`。测试名/注释与断言场景对照：M006-001 DENY/日常/高危、M006-005 两闸与扩权、M006-014 双权限一次消耗、M006-006 快照、M006-007 受益人自审、M006-026 删除入口与生命周期、M006-011 登记、M006-032/034 中文，名实相符。M037-001/002 注释等于所补断言。

---

## 4. 顺从式：52 项映射

映射原文：`criteria-lock.md` L6416，实数 52、无重复。定义区全文已读；HTML 原文抽查三份源文件（BEQR-157、BSM-003、BSM-028、BSM-114、BSM-120 L1977「职级可按 M1-M10，并可分上中下」、PGM-001、PGM-033、PGM-048、PGM-059、PGM-066）。阶段 B 不新增判据。

对**测试模块**：「须与锚点原文一致」= 本文件声称/锁住的行为不得与该锚点冲突；锚点若无对应测试面则记无本面，不为无本面补测。

| ID | 适用? | 证据（注明版本） |
|---|---|---|
| BEQR-157 | 无本面（人事五态）/ 部分（账号状态） | HTML「待入职、在职、试岗、停岗、离职」。本文件测 `active`/`invited`/`typo`/`disabled`，不声称那五态。不冲突。**两版** |
| BEQR-182 | 无本面 | 考勤日结果。无打卡断言 |
| BEQR-183 | 无本面 | 打卡方式 |
| BEQR-184 | 无本面 | 请假类型 |
| BEQR-185 | 无本面 | 请假单状态 |
| BEQR-186 | 无本面 | 薪资单构成 |
| BEQR-187 | 无本面 | 提成发放 |
| BSM-001 | 适用（背景） | 「谁能操作」。本文件锁权限/范围/最高权限，不发明综合体经营规则。**两版** |
| BSM-003 | 适用 | 「权限分岗」；配置存档/删除审批。DENY 优先、生命周期、跨商户。002 补了非平台管理员存档与驳回删除。**发布口径已锁；未发布版亦锁（夹具不同）** |
| BSM-009 | 适用 | 基础管理含员工与权限 |
| BSM-028 | 适用 | 「管理员工、岗位、职级、权限、员工注册审批」。定岗、改职级、注册、模板是主面。三十档枚举不在本 mock（O-6） |
| BSM-029 | 适用（部分） | 「用户权限要知道职级和审批状态」。有职级 30 不能代替 USB、注册审批；无设备绑定 |
| BSM-073 | 无本面 | 商户专区账单/工单。跨商户隔离归 PGM-048/BSM-114 |
| BSM-084 | 适用（部分） | `registrationCreatesAccount…` verify `role_staff_self`。排班考勤无本面 |
| BSM-085 | 适用（部分） | 商户代提交 / 商户不得自批 / 公司批。私人手机自助注册无本面 |
| BSM-086 | 无本面 | 排班考勤请假 |
| BSM-087 | 无本面 | 我的薪资 |
| BSM-088 | 无本面 | 提成/人员服务边界 |
| BSM-114 | 适用 | 不同人只做该做的：跨商户、缺组织、平台管理员 vs 最高权限、员工须登记。**两版** |
| BSM-115 | 适用（背景） | 「基础管理连所有业务」含权限卡住。不测票码/酒吧 |
| BSM-120 | 适用（部分） | 「职级可按 M1-M10，并可分上中下」——测试用 rank 1/4/30，不枚举 30 行（O-6）；「员工权限按单一功能最小颗粒度」——LOW/HIGH 分码 |
| GEN-001 | 适用 | 安全回归有用途；无纯转发空类 |
| GEN-002 | 适用 | 发布口径 worktree 39/39 绿（类型 2）；未发布对照 39/39 绿（分开写） |
| GEN-003 | 适用 | **001/002 修复后本席变异 1–5 均红**。其余只 `ApiException` 处有 never / 状态 / 抛vs成功单一守卫（占用、扩权对照变异亦红）。不把测试通过本身当证据 |
| GEN-004 | 适用 | 状态/密码/时间窗/账号类型在写入前校验 |
| GEN-005 | 适用 | 服务端授权、默认拒绝、DENY 优先、主会话 vs 职级。001 锁授出能力闸与审批闸；002 锁平台管理员存档与最高权限删批 |
| GEN-006 | 适用（部分） | 指纹/快照、占用检查；无真事务（单元测试） |
| GEN-007 | 适用（部分） | 幂等按组织分钥；无并发测试，不立项 |
| GEN-008 | 适用 | 错误码/中文/账号类型与服务层一致。001/002 现核具体码 |
| GEN-009 | 适用 | 缺适配器/空白密码/未知主体失败而非伪成功 |
| GEN-010 | 适用 | 手机号不入 JSON；不测加密算法（仓储层） |
| GEN-011 | 适用（部分） | 本文件主要 verify 写入/不写入；失败留痕在服务层 |
| GEN-012 | 适用 | 平台管理员有效集字典 ≤2 次 |
| GEN-013 | 适用 | 单测不依赖本机库；本类不连库 |
| GEN-014 | 无本面 | 无迁移文件 |
| GEN-015 | 无本面 | 无 UI。中文报错走 PGM-001/GEN-008 |
| GEN-016 | 适用 | 注释 M006/M037 编号与断言场景对照见 §3；无把计划写成现状 |
| GEN-017 | 适用 | 本文件是 HTML 权限规则的下位测试（细化）。001 分类技术性（测试有效性，无业务发明）已定稿，本席无新证据改判 |
| GEN-018 | 适用 | 本报告依据 HTML / 实跑 / 命令输出，不用席位意见当依据 |
| PGM-001 | 适用 | 资格/权限底座；中文报错三测 |
| PGM-008 | 适用（卡片） | 员工注册/岗位状态；企微/工作手机无本面 |
| PGM-022 | 无本面 | 空间点位 |
| PGM-024 | 无本面 | 低消/容量/库位 |
| PGM-026 | 无本面（主体档案）/ 部分（注册 merchant） | 商户代提交用 merchant 夹具，不实现四类主体绑定 |
| PGM-032 | 适用（部分） | 「审核是否通过」——登记审批。设备标识无本面 |
| PGM-033 | 适用（部分） | 注册两条路径，同 BSM-085 |
| PGM-034 | 无本面 | 企微会话 |
| PGM-035 | 无本面 | 提成发放 |
| PGM-036 | 无本面 | 设备领用 |
| PGM-048 | 适用 | 商户不能跨商户——`organizationReadWorksButCrossMerchantWriteDoesNot` |
| PGM-059 | 适用（部分） | `phoneHash` 忽略。不测加密存放 |
| PGM-066 | 适用 | 单一功能颗粒度、全中文、权限点预置（字典 55 条不分页截断）；「已拍板：员工权限按单一功能最小颗粒度配置」 |

未映射但测试注释引用的 BSM-007 / PGM-040：不扩锁；行为由 BSM-003/028/PGM-066 与 GEN-003 覆盖。不 LOCK-AMEND。

---

## 5. 第一性原理三问

**Q1 这段东西为什么必须存在？删掉它，三份 HTML 哪一条做不到？**

`M006SecurityRegressionTests` 是本聚合在后台的安全回归网。删掉它：

- BSM-003 / BSM-114 / PGM-066：DENY 优先、日常 vs 高危、跨商户、单一功能码，没有本切片失败断言。
- PGM-001 全中文：控制器/异常处理中文没有本切片锁。
- BSM-003 配置存档/删除审批：生命周期与 DELETE 入口。
- BSM-085 / PGM-033：员工注册须公司批、手工入口只留外包。

答得上 → 有业务上位。文件本身是细化（测试），不是添加。001 是测试有效性补强（技术性）；002 是同一 HTML 下位测试缺口（细化）。均无业务发明。

**Q2 声称的行为有没有被实际执行验证过？**

有。类型 2：origin/main worktree + 发布变体 39/39 绿；主工作区未发布 39/39 绿。类型 4：全文 + origin SUT 对照 + 本席五处目标闸变异均红。001 现执行三支（不持有高危 / 不持有日常 / 持有高危）。002 现执行 staff 存档、`reject_delete`、普通管理员批准删除核 `MASTER_AUTHORITY_REQUIRED`。例外仍只观察：岗位两条读测试自己的 `findPermissionEffects` 合成（O-2）；`grantList` 不锁 30 天（O-3）。

**Q3 依赖的前提哪一条没验证就当真？**

1. Mockito `findPermissionEffects` 合成 ≡ Jdbc SQL（O-2；M036 已补有效集/真库，无新证据升队）。
2. origin `isMasterAuthority` ≡ platform_admin 桥 + 主用户 ID；工作区 ≡ `MASTER_ACCOUNT_TYPE`（W-M037-1，本席 `test-compile` 证实 origin+工作区测试无法编译）。
3. 反射 `normalizeDecision` / `handleApiException` / `StaffService.registrations` 等于生产装配（O-4/O-5）。
4. `listGrantRequests` 非 null cutoff ≡「配置窗口」而非任意过去时刻（O-3）。
5. **已不再成立的旧前提**：origin 生命周期从 setUp 的 platform_admin 起手、把 `requirePlatformAdmin`/`reject_delete`/最高权限闸当成已锁——发布变体已补断言，本席变异 3–5 打红。

---

## 6. 对抗式（grilling 自问自答，不等操作员）

❓ **Q1 改坏 `requireActorCanGrant` / `requireGrantApprovalIfNeeded`（只动模板编辑这一处），测试会红吗？**

➡️ **会。** 变异 1：不持有 HIGH 改走审批闸，L186 码对不上。变异 2：持有 HIGH 不再抛，L190 不抛。第三码锁「不须审批且未持有」；模板项 `containsExactly(LOW)` 兜住误写入。**成立于发布口径与主工作区（001 两版同文）。**

❓ **Q2 改坏 `requirePlatformAdmin`，生命周期测试会红吗？**

➡️ **会。** 变异 3：staff archive 不再抛，L597 红。发布变体另有 `never().savePositionTemplateLifecycle`。主工作区无该 verify，但 `thrownBy`+`PLATFORM_ADMIN_REQUIRED` 在闸缺失时仍红。**002① 两版都锁；verify never 只在发布变体。**

❓ **Q3 `reject_delete` 做成批准，会红吗？**

➡️ **会。** 变异 4：`deletedAt` 非 null，L618 红；随后 `deleteReviewId` 清空与再申请路径也不会走到。**002② 成立于发布口径；主工作区同有 reject 后再申请（未发布夹具）。**

❓ **Q4 拿掉最高权限闸，普通管理员批准删除是否仍绿（自审顶替）？**

➡️ **不再绿。** 变异 5：expected `MASTER_AUTHORITY_REQUIRED` but was `SUPERVISOR_SELF_REVIEW_FORBIDDEN`。这正是定稿 002③ 的回归点。**发布口径已锁。主工作区同核该码。**

❓ **Q5 只断言 `ApiException` 的其余各处，是否仍由 never / 状态 / 单一守卫兜住？**

➡️ 逐条核过发布变体 23 处 plain `isInstanceOf(ApiException)`：跨商户/空窗/坏状态/DELETE/缺凭证等有 `verify never`；占用 `request_delete`、扩权须重授权本席对照变异为**抛 vs 成功**（拿掉闸则不抛，测试红），不是换码仍绿。职级 30 顶层审批 PB 有 `never().saveSupervisorReview`。无新的「换一个 ApiException 仍绿」证据。不把 O-1 升队。

❓ **Q6 Mockito 有没有把授权逻辑换成桩返回值？**

➡️ `twoPermissionReview` mock 的是 `AccessService.reviewGrant`，核验在真实 `AuthorizationService.requirePermissions`。岗位两条断言测试自己写的 effects 合成（O-2），不经 `effectivePermissions()`。无新证据改判。

❓ **Q7 测试名/注释是否等于断言场景？有无顺序或共享状态？**

➡️ 名实相符，见 §3。无 `PER_CLASS`；每方法新实例；`SecurityContextHolder` 方法结束清除。无顺序依赖。

❓ **Q8 未发布项有没有混进发布口径？**

➡️ 发布变体主会话仍是 `MASTER_USER_ID` + `"platform_admin"`，无 `MASTER_ACCOUNT_TYPE`。本席 origin worktree 39 绿。主工作区测试在 origin SUT 上 `MASTER_ACCOUNT_TYPE` 无法解析（§2.4）。W-M037-1…2 维持只登记。

❓ **Q9 XM-M037-A 要不要在本文件补套用/建角色/员工授予/岗位路径的失败面？**

➡️ 不要。B2 双席已登记待操作员决定是否重开 M036。本席变异只动了 `updatePermissionTemplate` 的两闸调用点，未整体放开 `requireActorCanGrant`。无新证据改判。

❓ **Q10 有没有为理论场景加需求的冲动？**

➡️ 占用/扩权对照变异证明它们已被「必须抛」锁住，不另立项。三十档列表属别模块（O-6）。不为未映射 BSM-007 扩锁。

---

## 7. 已定稿 ID 与只登记项（无新证据不改判）

### M037-001（技术性，队列否／已修）

- **两版都成立。** 发布变体与主工作区 `templateEditCannotGrantAnUnownedHighPermission` 同文（WS/PB L175–193）。
- 原句：注释「两道闸各自锁住」；三支 `e.code()` + `items … containsExactly(LOW)`。
- 正常触发：模板管理员把不持有的日常码或持有的高危码加进已有 GRANT 的模板。
- 本席证据：变异 1、2（类型 2）+ origin SUT L174–175（类型 4）+ 治理地图高危须审批（类型 1，经已冻结 GEN-005）。
- 四分类复核：冲突？否。细化？测试有效性缺口，HTML 未规定「测试必须分闸」这一写法。技术性？是（无业务发明）。添加？否。**顺序仍命中技术性。** 不改分类。

### M037-002（细化，队列否／已修）

- **发布口径成立**（origin 夹具 + 三处断言 + `never().savePositionTemplateLifecycle`）。**主工作区未发布版亦有三处错误码/reject 路径**，用 `MASTER_ACCOUNT_TYPE`，不能随本模块发布。
- 原句（PB L595–611、L615–619）：staff archive → `PLATFORM_ADMIN_REQUIRED`；`never().savePositionTemplateLifecycle`；普通管理员 `approve_delete` → `MASTER_AUTHORITY_REQUIRED`；主会话 `reject_delete` 后 `deletedAt`/`deleteReviewId` 空，再申请可批。
- 本席证据：变异 3、4、5（类型 2）+ origin SUT L500–502、L538–554（类型 4）+ BSM-003 存档/删除审批（类型 1）。
- 四分类：冲突？否。细化？是（BSM-003/028 下位）。技术性？上位是权限规则。添加？否。**维持细化。**

### W 项（仅未发布版，不入队）

| ID | 本席本轮核实 | 处置 |
|---|---|---|
| W-M037-1 | 主会话改 `MASTER_ACCOUNT_TYPE`；建岗位模板主体 `template-admin`。origin `SessionIssuer` 无该常量。本席 origin+工作区测试 `test-compile` 两处 cannot find symbol（L427、L613） | 维持登记，不入队 |
| W-M037-2 | `topAuthorityReview` 的 `findSupervisorReviewSchedule` 只在未发布 `SupervisorReviewService.approve` 的受益人闸下需要。发布变体无该夹具，origin 39 绿 | 维持登记，不入队 |

### XM-M037-A

→ 已闭环 M036 的 AccessServiceTests。本文件未补套用/建角色/员工授予/岗位路径失败面。无新证据。维持登记。

### 观察（B1/B2 已只登记，无新证据不改判）

- **O-1** 其余只 `ApiException`：有 never / 状态 / 抛vs成功单一守卫。占用与扩权本席对照变异为红，加强而非推翻该观察。
- **O-2** 定岗/改职级断言测试合成的 `findPermissionEffects`。
- **O-3** `grantListHasADefaultTimeWindow` 只断言 cutoff 非 null。
- **O-4** 反射 `normalizeDecision`。
- **O-5** 反射注入 `StaffService.registrations`。
- **O-6** 本 mock 不列出 30 档。
- **O-7** XM-M036-A 全量基线红属别模块。
- **O-8** 映射无 BSM-007 / PGM-040 / BEQR-151，不扩锁。

ponytail-review：001 第三码夹具与 002 三处断言是定稿最小集，不是可删抽象。`Lean already. Ship.`

---

## 8. 本轮新 findings

无。

- 无冲突。
- 无新细化/技术性工程缺陷。
- 无添加项，不写 `questions/`。
- 无 HTML 修改申请。
- 不为理论场景加判据或需求。

001、002 修复落地，本席独立红绿证据成立，不重新入队。

---

## 9. 模块结论

| 项 | 值 |
|---|---|
| 全文覆盖 | 是（1 文件：主工作区 V0.31 833 行 + 发布变体 V0.31 835 行） |
| 52 判据 | 逐项记录，不新增 |
| 三线 | 第一性 / 顺从 / grilling 对抗均完成 |
| 发布口径实跑 | origin/main 一次性 worktree 39/39 绿；worktree 已 remove、未 --force |
| 未发布对照 | 主工作区 39/39 绿（分开写） |
| 001/002 | 已修；本席变异 1–5 均红 |
| W-M037-1…2 | 仍只在未发布版；编译探针证实 1 |
| XM-M037-A | 维持登记 |
| 添加项 | 0 |
| HTML 申请 | 无 |
| 新入队缺陷 | 0 |
| 本席票 | **PASS** |

下一步（席甲调度，本席不修、不写 ledger）：两席 raw 封存后合取；四值哈希校验。本席 B5-r1 结束。
