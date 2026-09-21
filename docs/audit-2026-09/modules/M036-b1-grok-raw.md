CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M036 B1 席乙 Grok 独立审计报告

- 席位：乙（Grok 4.6 / reasoning xhigh）
- 步骤：B1 修复前全量审计（本模块首次开工，本版历史未审）
- 报告版本：V0.01（指定路径原先不存在，本次创建）
- 控制提示词：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11（本席独立 `sha256sum`，见首行）
- 正本：`/home/james/complex-management` main 主工作区
- 互盲：未读取 `modules/M036-b1-self-raw.md`，未使用 MCP / 子代理 / 第三席
- 本席结论：**FAIL**（1 条工程缺陷 GR-01 成立，入拟修复队列；4 条仅未发布版成立的 W 项按 D-029 不入队）

---

## 0. 技能预检（按环境如实）

本席在本机实际目录核过、本轮按收窄口径用过：

| 技能 | 路径核验 | 本轮用法 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` 存在 | 开工先核技能 |
| `graft` | `/home/james/.claude/skills/graft/SKILL.md`；CLI `/home/james/.npm-global/bin/graft` | skeleton 三测试 + AccessService + AuthorizationService |
| `grilling` | `mattpocock-skills/.../productivity/grilling/SKILL.md` 存在 | 对抗线用其问答结构**自问自答**（B1 禁止等人，不向操作员发问） |
| `grill-me` | 同插件 `productivity/grill-me` 存在 | 未启动交互式会话；结构并入 grilling 自问 |
| `ponytail` ultra | `ponytail/4.9.0/skills/ponytail/SKILL.md` 存在 | 不为理论路径补需求；缺测试只在本聚合关键分支且首次正常使用可复现时立项 |
| `systematic-debugging` | superpowers 存在 | 只借「读错误 → 复现 → 追数据流」。未发布混跑 7 条 ERROR 已追到未发布 SUT，不改代码 |
| `verification-before-completion` | superpowers 存在 | 先实跑再下可执行结论 |
| `long-running-background-tasks` | grok bundled 存在 | 本次 Maven 11s 内结束，前台跑完，未建 monitor |
| `i-have-adhd` | marketplace/cache 存在 | **未按该技能压缩全文**。V0.17 要求完整 raw 与该技能极简输出冲突，以提示词完整落盘为准 |

明确不用 / 缺口：

- Arc `audit` 默认派并行 reviewer：用户与 V0.17 禁止第三席与子代理，**未派**。
- `writing-plans` / `test-driven-development` / `requesting-code-review`：B1 只读，不适用。
- `batch-grill-me`：本环境技能目录**未核到**，记缺口，不冒充已调用。
- `research`：V0.17 §6 已划掉。
- 未使用 MCP。

graft 本轮 tokens saved ≈ 26,271 + 4,635 + 4,731 ≈ 35,637（三份测试骨架 + 两份被测服务骨架）。

---

## 1. 环境事实（命令实算，非手抄）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```

sidecar `plan/criteria-lock.sha256` 同行相等。ledger 顶部同值。本席只报实算，不改 ledger。

| 项 | 值 |
|---|---|
| 主工作区 HEAD | `e456ba96c84185c8d39f18f548011daac48a6466` |
| origin/main | `3ca1b5076028215a945ede2ae47b71f5786a1c70`（Merge PR #476 M035 检查点） |
| HEAD 相对 origin/main | 落后；`git rev-list --count origin/main..HEAD` = 49（未推） |
| complex_test Flyway | `SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1` → **430**（success=t） |
| staff_levels | `SELECT count(*) FROM staff_levels` → **30**；`level_rank` CHECK 1..30；M1–M10 × lower/middle/upper |

三文件主工作区：

| 文件 | 行数 | SHA256 | git hash-object | 相对 origin/main |
|---|---|---|---|---|
| `AccessServiceTests.java` | 2134 | `bf931a403f1217b4a23f184e4234e19b1f78cd8f1e1b5342b58ebf2805aebf1a` | `da9feeb5…` | **逐字节相同**（工作区对 HEAD 显示 ` M`，因 M031/M032 经独立 worktree 发布、主工作区未提交） |
| `AuthorizationServiceTests.java` | 595 | `0f4c11d753cedc0f5310fda294b9101bcb75b18e5b91282677a3f66fe71fca28` | `714c10a2…` | **不同**。工作区 V0.03 / 24 `@Test`；origin V0.01 / 564 行 / SHA256 `0ddd8777da6df884af91e259c802607df903ccbe0a7761ddb0fdd0d775ba7222` / 21 `@Test` |
| `M006JdbcRegressionTests.java` | 319 | `49ca8eadee2ec90f0fc310d9285c6e2b01b449bc8e9289091ff9ca73322b9b91` | `37c1310c…` | **逐字节相同** |
| 合计 | **3048** | | | 与派发口径一致 |

D-029：发布口径是 origin/main。两版都读；只在未发布版成立的记 W，不入修复队列。

被测追证（只读，不重开已闭环模块）：AccessService / AccessRepository（M031）、JdbcAccessRepository / AuthorizationService（M032）、DTO（M033）、权限迁移（M034）、同聚合第 1 段测试（M035）。主工作区 `JdbcAccessRepository.java` 与 `AuthorizationService.java` 含未发布工作流；`SessionIssuer.MASTER_ACCOUNT_TYPE` 仅主工作区有，origin/main 的 `SessionIssuer.java` 无该常量。

---

## 2. 实跑（可执行产物，2.3 类型 2/4）

后端只导出三个 `COMPLEX_DATABASE_*`（逐行读 `/etc/complex/complex.env`，URL 库名改为 `complex_test`），未 source 该文件。`COMPLEX_DATABASE_APP_*` 等未进入环境。`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`（首次误用 `java-21-openjdk-amd64`，该树无 `javac`，`release 21 not supported`，exit 1；改 Temurin 后重跑）。

### 2.1 发布口径：origin/main 一次性 worktree

```
git worktree add --detach /home/james/wt/m036-b1-origin-main origin/main
# HEAD = 3ca1b5076028215a945ede2ae47b71f5786a1c70
# 临时拷入主工作区未发布 V427（origin 无此文件，complex_test 已应用到 430）
cp .../V427__m009_project_profiles_and_associations.sql $WT/backend/src/main/resources/db/migration/
mvn -f backend/pom.xml -o test -Dtest=com.complex.platform.access.AccessServiceTests,com.complex.platform.access.AuthorizationServiceTests,com.complex.platform.access.M006JdbcRegressionTests
```

结果（exit 0）：

```
M006JdbcRegressionTests        Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
AuthorizationServiceTests      Tests run: 21, Failures: 0, Errors: 0, Skipped: 0   # V0.01
AccessServiceTests             Tests run: 54, Failures: 0, Errors: 0, Skipped: 0
Tests run: 87, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS  Total time: 11.009 s
```

`assumeTrue` 未跳过 JDBC 类。跑完删除 V427、删除 worktree `backend/target`，`git worktree remove`（无 `--force`）成功。主工作区 V427 仍在。

### 2.2 未发布混跑：主工作区 HEAD+暂存 SUT（仅对照，非发布口径）

同一三变量、同一三个测试类，编的是主工作区未发布 `AuthorizationService` / `JdbcAccessRepository` / `SupervisorReviewService`。

```
Tests run: 90, Failures: 0, Errors: 7, Skipped: 0  BUILD FAILURE
```

| 类 | 未发布结果 | 根因（只读追数据流） |
|---|---|---|
| AuthorizationServiceTests V0.03 | 24/24 绿 | 测的是未发布 SUT（含 MASTER_ACCOUNT_TYPE、商户范围不再对平台管理员返回 empty） |
| AccessServiceTests（与 origin 逐字节相同） | 54 中 6 ERROR | `SupervisorReviewService.approve`：「退款、免单、授权与资金事项必须登记直接受益人」。origin worktree 同测试全绿 → 只在未发布审批链成立 |
| M006JdbcRegressionTests（与 origin 逐字节相同） | 12 中 1 ERROR | `temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce`：未发布 `JdbcAccessRepository.markSingleUsePermissionConsumed` 使用 `WHERE grant_group_id = ?`，本测试 TEMP 表无该列。origin 同方法为 `WHERE id::text = :source_id`，worktree 12/12 绿 |

按 D-029 记 W-1…W-4，不入本模块修复队列。

psql 只对 `complex_test` 做 SELECT，无写语句。

---

## 3. 全文覆盖（不抽样）

### 3.1 `AccessServiceTests.java` V0.08（主工作区 = origin/main）2134 行 / 54 `@Test`

被测：`new AccessService(InMemoryAccessRepository, …)` + 同实例 `AuthorizationService`。无 Spring 容器、无真库。

| 行 | 测试 | 是否执行声称行为 | 断言能否红 |
|---|---|---|---|
| 83–132 | copyPermissionTemplateKeepsSnapshotWhenSourceChangesAndWritesAuditEvent | 是：copy 后改源，读 copy 子项 | 能（子项被改则红） |
| 134–168 | applyPermissionTemplateRequiresApprovalWhenTemplateHasHighRiskPermission | 是：高危模板 apply 403 | 能 |
| 170–207 | applyPermissionTemplateAppliesToManyUsersAndWritesAuditEvent | 是：两人落地 + 审计 | 能 |
| 209–235 | temporaryGrantCreateAndRevokeWriteAuditEvents | 是：创建/撤销 | 能 |
| 239–256 | createAccessUserWithPlatformAdminAccountTypeThrowsValidationError | 是：禁止创建 platform_admin | 能 |
| 258–275 | createAccessUserWithAllowedAccountTypeSucceeds | 循环只含 `outsourced_vendor` 一种 | 能，但覆盖窄（O-1） |
| 277–301 | platformAdminWithMerchantAffiliationCanCreateUserForAnyMerchant | 是：平台管理员跨商户开户 | 能 |
| 303–320 | createAccessUserRequiresOrganizationScope | 是 | 能 |
| 322–352 | createAccessUserStoresManualEvidenceAndProvisionsCredential | 是：证据/凭证/审计；`phoneHash()` null | 能 |
| 354–373 | merchantScopedCreatorCannotCreateUserForAnotherMerchant | 是：CROSS_MERCHANT | 能 |
| 377–389 | assignStaffLevelSetsLevelAndDefaultsCurrencyToCny | 是 | 能 |
| 391–400 | assignStaffLevelRejectsUnknownLevel | 是 | 能 |
| 402–411 | assignStaffLevelRejectsNonCnyCurrency | 是 | 能 |
| 413–425 | assignStaffLevelRejectsCrossOrganization | 是 | 能 |
| 427–438 | effectivePermissionsForUserRejectsCrossOrganization | 是 | 能 |
| 440–453 | changeUserScopeRejectsTargetOutsideActorProject | 是 | 能 |
| 455–460 | listStaffLevelsReturnsSeededLevels | 只断言夹具 2 档 id，不断言 30 | 能，但与库内 30 档脱节（O-2） |
| 462–485 | assignAndUnassignUserRoleUpdateUserRoles | 是 | 能 |
| 487–531 | assignUserPositionAppliesMappedRoleAndTemplate | 写岗位/模板应用；**不读有效集** | 对 positionRoles/userRoles 能红；对「最终权限」不能（并入 GR-01） |
| 533–551 | assignUserRoleRejectsRolePermissionsActorDoesNotHold | 是：GRANT_EXCEEDS_OWN_PERMISSIONS | 能 |
| 553–574 | assignUserRoleRequiresApprovalWhenHighRiskGrantFlagIsEnabled | 是 | 能 |
| 579–610 | positionMappingAndAssignmentDoNotRequireTheConfigurerToHoldTheHeldHighRiskCodes | 是：普通码立即生效、高危挂起 | 能（view 来自模板，与内存仓缺口正交） |
| 612–661 | assignUserPositionLandsNormalPartAndHoldsHighRiskPartUntilChainApproves | 是：DENY 挂起→审批释放 | 发布口径能红；未发布混跑在 reviewGrant 被直接受益人闸打断（W-4） |
| 665–701 | scopeChangeRevokesOldDerivedGrantsImmediatelyAndNewScopeWaitsForApproval | 是：旧派生 positionRoles 清空、新范围待审 | 对 positionRoles 能红 |
| 705–737 | scopeChangeApprovalRejectsWhenTheNewScopePositionMappingChangedWhilePending | 是：GRANT_SNAPSHOT_CHANGED | 能 |
| 741–772 | scopeChangeAcrossProjectsLandsWhenTheApproverStaysInTheOldProject | 是 | 能 |
| 776–809 | scopeChangeCancelsOldScopeHighRiskHoldsAndTheirDenyOverrides | 是：旧挂起 rejected、独立 DENY 处理 | 能 |
| 813–854 | scopeChangeFreezesExtraGrantsBeyondTheNewTemplate | 是：直接角色/模板/临时/覆盖一并收 | 能 |
| 858–889 | removingTheHighRiskHoldDenyRequiresApproval | 是 | 发布口径能；未发布 W-4 |
| 893–924 | preExistingExplicitDenyIsNotReusedByTheHighRiskHold | 是 | 能 |
| 927–955 | remainingValidationMessagesAreChinese | 三条公开入口 + **反射私有** `normalizeGrantRequestType` | 公开路径能红；反射不是产品入口（O-3） |
| 957–965 | scopeChangeRejectsUnchangedScope | 是 | 能 |
| 969–981 | grantPolicyRejectsAnyUnclassifiedPermissionInTheRequestedRole | applyGrant 含 unclassified；`grantRequests` 为空 | 能（空请求表比单纯 isInstanceOf 更能红） |
| 983–1005 | grantRequestRejectsPermissionsThatDoNotRequireApproval | 是：GRANT_APPROVAL_NOT_REQUIRED | 能 |
| 1007–1039 | approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment | 是 | 发布口径能；未发布 W-4 |
| 1041–1072 | approvedTemporaryGrantExpiresByEffectivePermissionRead | 是：过期窗口审批后有效集不含 | 发布口径能；未发布 W-4 |
| 1074–1118 | auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse | 是：requirePermission 真走授权 | 发布口径能；未发布 W-4 |
| 1120–1147 | grantSummaryReportsFrequentTemporaryRequestsByPositionAndUser | 是：改政策 JSON 阈值 | 能（`String.replace` 全替换 value:3） |
| 1151–1186 | applyPermissionTemplateWithCrossOrgTargetUserThrowsForbidden | 是 | 能 |
| 1190–1218 | revokeTemporaryGrantOfOtherOrgUserThrowsForbidden | 是 | 能 |
| 1222–1253 | updatePermissionTemplateOfOtherOrgThrowsForbidden | 是 | 能 |
| 1257–1273 | createTemporaryGrantRejectsGrantingPermissionActorDoesNotHold | 是 | 能 |
| 1275–1294 | applyPlatformOwnedTemplateByOrgUserToSameOrgUserIsForbidden | 是 | 能 |
| 1298–1308 | listAccessUsersFiltersToCurrentOrganization | 是 | 能 |
| 1310–1319 | listAccessUsersPlatformAdminSeesAllOrganizations | 是 | 能 |
| 1321–1331 | listAccessUsersFiltersToCurrentMerchantWhenMerchantScoped | 是 | 能 |
| 1373–1396 | userPermissionOverrideDenyLandsRemovesAndHighRiskGrantNeedsApproval | 是：DENY 覆盖 GRANT | 能 |
| 1399–1428 | moneyPermissionApplicationsAreMarkedForTheShorterDeadline | 是：涉钱 actionType | 能 |
| 1432–1455 | dailyTemporaryPermissionApplicationEntersSupervisorChain | 是 | 发布口径能；未发布 W-4 |
| 1480–1498 | listGrantRequestsFiltersRowsStampedWithOtherProjects | 是：过滤在 AccessService 不在仓 | 能 |
| 1507–1526 | restoringAnArchivedPositionMappingMakesItAssignableAgain | 是：存档不可定岗、恢复可定岗 | 能 |
| 1529–1547 | restoringIsRejectedWhileAnActiveReplacementMappingExists | 是：409 POSITION_TEMPLATE_ACTIVE_EXISTS | 能 |
| 1569–1617 | savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants | **名称/注释声称重算有效权限并收回派生；断言只打 positionRoles 与薪资** | 见 GR-01 |
| 1620–1632 | positionTemplateLifecycleAndGrantPolicyFailuresAreAudited | 是：失败审计事件 | 能 |

夹具：`InMemoryAccessRepository` L1676–2081、`InMemorySupervisorReviewRepository` L2083–2133、`RecordingCredentialProvisioner` L1664–1671。`@AfterEach` 清 SecurityContext。测试间不共享静态可变状态（每测 `new` repository）。`markSingleUsePermissionConsumed` L2077–2080 **恒返回 true**，本类无单次消耗路径依赖它；单次消耗由 AuthorizationServiceTests + JDBC 类覆盖。

### 3.2 `AuthorizationServiceTests.java`

#### origin/main V0.01（发布口径，564 行 / 21 `@Test`）— 本席 `git show origin/main:…` 全文读

被测：`new AuthorizationService(InMemoryAccessRepository)`。仓是效果列表桩，不组装岗位/角色 SQL，专门打 AuthorizationService 判定。

| 测试 | 行为 | 断言 |
|---|---|---|
| requirePermissionReturnsForbiddenPermissionDeniedWhenUserLacksPermission | 缺权 403 | 能红 |
| denyWinsWhenGrantAndDenyExistForSamePermission | 同码 GRANT+DENY → DENY | 能红 |
| expiredTemporaryTemplateGrantDoesNotAuthorize | 过期模板授权 | 能红 |
| singleUseTemplateGrantIsConsumedAfterFirstSuccessfulAuthorization | 第一次 require 成功，第二次 DENY；consumedSourceIds | 能红 |
| localSeedBridgeAuthorizesPlatformAdminButNotArbitraryStaffWithSmokeRole | 平台管理员桥 vs 普通员工 ROLE_P0_SMOKE | 能红 |
| singleUsePermissionConcurrentConsumerIsDeniedWhenAtomicUpdateReturnsZeroRows | simulateAlreadyConsumedAtDb → 0 行 | 能红 |
| isPlatformAdminReturnsTrue/False | 身份谓词 | 能红 |
| outsourcedVendorIsNotPlatformScopeAndRequiresProjectScope | 外包非平台范围 | 能红 |
| outsourcedFacilityTemporaryGrantAllowsAssignedWorkAndEquipmentReadButNotAccept | 最小外包权限集 | 能红 |
| outsourcedTemporaryGrantExpiresAndNoLongerAuthorizes | 过期 | 能红 |
| outsourcedSecurityCleaningTemporaryGrantOnlyGrantsAssignedInspectionAndExceptionPermissions | 保洁/巡检外包 | 能红 |
| currentMerchantScopeReturnsEmptyForPlatformAdmin | authenticate 带 merchantId=merchant，平台管理员 **empty** | 能红（对 origin SUT） |
| currentMerchantIdQuietlyReturnsEmptyForPlatformAdminEvenWhenSeedBoundMerchantId | 同上 quietly | 能红 |
| currentMerchantIdQuietlyReturnsMerchantIdForRealMerchantUser | 员工仍按商户号 | 能红 |
| currentMerchantScopeReturnsMerchantIdForMerchantUser | 是 | 能红 |
| currentMerchantScopeThrowsForbiddenWhenMerchantIdIsAbsent | MERCHANT_SCOPE_REQUIRED | 能红 |
| effectivePermissionsRemovesDenyCoveredCodesFromGrantsAndListsDenies | DENY 从 grants 扣除 | 能红 |
| effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard | 只断言 `wildcard()==false`（SUT 构造器第 4 参写死 false） | 改 grants 为全量字典本测试仍绿（O-4） |
| effectivePermissionsQueryDoesNotConsumeSingleUseGrant | 两次只读不消费 | 能红 |
| effectivePermissionsExcludesExpiredGrant | 是 | 能红 |

origin worktree：21/21 绿。

#### 主工作区 V0.03（595 行 / 24 `@Test`）— 未发布

相对 origin：删除「平台管理员 merchant empty」两条，改为 `contains("merchant")`；新增 `productionMasterIdentityGetsExistingAuthorityWithoutBypassingPermissionDenials`、`platformAdminAndIncompleteMasterIdentityDoNotBecomeHighestAuthority`、`currentSupervisorLevelIsThreeOnlyForMasterAuthority`，依赖 `SessionIssuer.MASTER_ACCOUNT_TYPE`（origin 无此常量）。主工作区混跑 24/24 绿。只在未发布版成立 → W-1、W-2。

### 3.3 `M006JdbcRegressionTests.java` V0.13（主工作区 = origin/main）319 行 / 12 `@Test`

真 JDBC：单连接 TEMP 表 + `JdbcAccessRepository` + Spring 事务拦截。`assumeTrue` URL 匹配 `jdbc:postgresql:.*?/complex_test`。本轮未跳过。

| 行 | 测试 | 与真表/真 SQL |
|---|---|---|
| 69–77 | temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce | TEMP `temporary_permission_grants` 无 `grant_group_id`/`subject_type`。origin SQL `id::text` 足够；未发布 SQL `grant_group_id` 不够（W-3） |
| 79–86 | auditFailureRollsBackConsumptionThroughActualSpringTransaction | CHECK 约束失败回滚 consumed | 能红 |
| 88–97 | inheritedTemplateSingleUseCanBeConsumed | 应用 single_use=false、模板 true；与 JDBC `(pta.single_use OR pt.single_use)` 一致 | 能红 |
| 100–113 | grantReviewReadLocksTheRequestRow | `LIKE public.access_grant_requests` + RowShareLock | 能红 |
| 117–134 | grantFactsRemainStableUntilTheGrantTransactionCommits | TEMP 影子表 + `LOCK TABLE … IN SHARE MODE` 未限定 schema，TEMP 生效 | 能红 |
| 138–155 | migrationAppliesAndGrantPolicyRoundTripsInPostgres | classpath 读 V386；4 参 `AccessGrantPolicy` 便捷构造仍可用 | 能红 |
| 158–166 | approvalFingerprintIsWriteOnceAndMissingIsExplicit | 写一次 | 能红 |
| 170–186 | positionReplacementRevokesOnlyItsOwnPreviousApplications | **真 SQL 撤销旧岗位应用、独立应用仍 active** | 能红 |
| 190–206 | createCollisionCannotOverwriteExistingUserOrRole | `LIKE … INCLUDING ALL` 唯一约束 | 能红 |
| 210–223 | grantPaginationKeepsScopeAndCanReachTheSecondPage | 范围 + 第二页 | 能红 |
| 227–249 | positionTemplateSoftDeleteCanBeRestoredWithoutLosingItsDefinition | 生命周期列；硬删抛 IllegalStateException | 能红 |
| 258–294 | legacyPositionMigrationPreservesIndependentGrantsAndCountsOccupiedDefinitions | 回放 V386 后换岗 | 能红 |

`openTemporaryTables` / `preparePositionMigrationTables` / `migrationSql` 已全文读。TEMP 列对所测 SQL 足够；不是全列克隆（无 `subject_type`），但未调用需要该列的 `findPermissionEffects`。

---

## 4. 第一性原理三问

### 4.1 AccessServiceTests.java

1. **为何必须存在？** 删掉则 BSM-028「管理员工、岗位、班次、职级、权限」、BSM-003 存档恢复/权限分岗、PGM-066「员工权限按单一功能最小颗粒度…角色和员工级完全自定义」、红线④ 商户/项目隔离在服务层没有可失败断言。有业务上位。
2. **声称行为是否执行？** origin worktree 54/54 绿。多数测试走真实 `AccessService` 写路径。例外见 GR-01：停用映射「收回派生」只断言内存 `positionRoles`，未断言有效集或模板应用状态。
3. **未验证前提？** 内存仓 `findPermissionEffects` 等于 JDBC 的岗位 ROLE 并集；`replacePositionGrants` 会撤销旧岗位模板应用。这两条前提不成立（对照 origin `JdbcAccessRepository` L829–874 与本文件 L1687–1688、L2011–2075）。

### 4.2 AuthorizationServiceTests.java

1. **为何必须存在？** 最终权限计算：DENY 覆盖 GRANT、过期、单次消耗、TOCTOU、外包最小集、商户范围、平台管理员桥。对应 BSM-003/028、PGM-048/066、GEN-005/007。
2. **是否执行？** origin 21/21 绿，效果是注入的，判定在 AuthorizationService（非假绿替换整段授权算法）。V0.03 的商户/最高权限断言是另一套未发布口径。
3. **未验证前提？** 发布口径：`currentMerchantScope`/`Quietly` 对平台管理员返回 empty（origin SUT L422–445 先 `isPlatformAdminBridge`）。未发布 SUT 改为有商户号就过滤。桩仓不实现 JDBC 组装，不代替 M006JdbcRegressionTests。

### 4.3 M006JdbcRegressionTests.java

1. **为何必须存在？** 内存仓明确把锁/单次消耗/换岗撤销交给本类。删掉则 JDBC 语义无真库断言。
2. **是否执行？** origin 12/12 绿，Skipped=0。消费、回滚、行锁、SHARE 锁、指纹、换岗撤销、碰撞、分页、软删恢复、V386 回放均打到 `JdbcAccessRepository` SQL。
3. **未验证前提？** TEMP 表列集 = 真表。对所执行语句成立；`findPermissionEffects` 的岗位 JOIN / `subject_type` **本类未测**（与 GR-01 同一缺口的 JDBC 面）。`assumeTrue` 缺环境会整类跳过——本轮有三变量，未空转。

---

## 5. 顺从式：52 项映射逐项

锁定义已读全文；下列 HTML 行已用当前三份 HTML 核对标题/td.k 原文存在：BEQR-157@240、BEQR-182@279、BSM-001@505、BSM-003@563（其下含「想恢复点"取消存档"即可」「平台管理员拥有全部日常功能权限」）、BSM-009@596、BSM-028@698、PGM-001@256、PGM-008@362、PGM-032@572、PGM-066@951。

| ID | 适用? | 理由 |
|---|---|---|
| GEN-001 | 适用 | 三文件均承载权限/职级/模板/有效集测试，非空转发 |
| GEN-002 | 适用 | origin 87/87 可执行；未发布混跑 7 ERROR 另计 |
| GEN-003 | 适用（本面未完全满足） | GR-01：停用派生/岗位角色并入有效集的断言与夹具不足 |
| GEN-004 | 适用 | 组织/商户/账号类型/职级 id/币种校验 |
| GEN-005 | 适用 | DENY>GRANT、跨组织、授出不超过自身、高危审批、外包最小集 |
| GEN-006 | 适用 | JDBC 消费回滚；指纹写一次；创建碰撞 |
| GEN-007 | 适用 | 单次消耗 TOCTOU；幂等键；行锁 |
| GEN-008 | 适用（本面未完全满足） | 内存仓岗位授权语义 ≠ origin JDBC；DTO/政策 JSON 含 deadlineWorkingDays 与 M032 一致 |
| GEN-009 | 适用 | 中文校验文案；失败审计；不吞异常（未发布 6 条是未发布审批链抛错，非吞错） |
| GEN-010 | 适用 | 禁止创建 platform_admin；跨商户 403；不测注入 |
| GEN-011 | 适用 | 模板/授权/职级/失败事件名 |
| GEN-012 | 无本面 | 无无界批处理/连接泄漏面；分页有页大小 |
| GEN-013 | 适用 | 三变量 + assumeTrue；本轮未跳过 |
| GEN-014 | 适用 | TEMP 回放 V386；不改已应用迁移 |
| GEN-015 | 无本面 | 无界面 |
| GEN-016 | 无本面 | 本模块非文档 |
| GEN-017 | 适用 | 见第 7 节四分类 |
| GEN-018 | 适用 | HTML 原文 / Maven 输出 / psql / git show |
| BEQR-157 | 无本面 | 员工状态五值未出现；测试用账号 `active`，不是待入职/在职/试岗/停岗/离职 |
| BEQR-182 | 无本面 | 无考勤日结果枚举或断言 |
| BEQR-183 | 无本面 | 无打卡方式 |
| BEQR-184 | 无本面 | 无请假类型 |
| BEQR-185 | 无本面 | 无请假单状态 |
| BEQR-186 | 无本面 | 只测职级赋值与 CNY，无薪资单构成/绩效档 |
| BEQR-187 | 无本面 | 无提成发放路径 |
| BSM-001 | 适用（弱） | 组织/商户/项目归属出现；不管票务酒吧主流程 |
| BSM-003 | 适用 | 岗位模板存档/恢复；权限分岗；日常权限 vs 高危；平台管理员桥（AuthorizationServiceTests） |
| BSM-009 | 适用 | 基础管理：员工与权限 |
| BSM-028 | 适用 | 「管理员工、岗位、…职级、权限」；红线④ 商户隔离（listAccessUsers / CROSS_MERCHANT） |
| BSM-029 | 适用（弱） | 权限卡住后续（缺权 403） |
| BSM-073 | 适用 | 商户范围内员工/权限，不看他商户 |
| BSM-084 | 无本面 | 员工手机端排班考勤请假薪资 |
| BSM-085 | 无本面 | 日常工作注册/工单/代订 |
| BSM-086 | 无本面 | 排班考勤请假 |
| BSM-087 | 无本面 | 我的薪资 |
| BSM-088 | 无本面 | 提成/人员服务边界 |
| BSM-114 | 适用 | 不同人只看自己范围（组织/商户过滤、平台管理员全量） |
| BSM-115 | 适用 | 基础管理连权限；合同/基础不允许则卡住 |
| BSM-120 | 适用（弱） | 已拍板条目多与票务供应无关；权限颗粒度以 PGM-066 为准 |
| PGM-001 | 适用 | 权限是治理底座「再管资格」 |
| PGM-008 | 无本面 | 入口卡「员工和手机」；本文件不测注册/绑机/企微 |
| PGM-022 | 无本面 | 空间点位 |
| PGM-024 | 无本面 | 低消/游乐容量/库位 |
| PGM-026 | 适用 | 商户绑定进入账号 merchantId 与过滤 |
| PGM-032 | 无本面 | 人/私人手机/工作手机对应 |
| PGM-033 | 无本面 | 注册路径/设备标识 |
| PGM-034 | 无本面 | 企微 |
| PGM-035 | 无本面 | 提成发放 |
| PGM-036 | 无本面 | 设备台账 |
| PGM-048 | 适用 | 商户侧授权范围内开放、不能跨商户（merchant 过滤测试） |
| PGM-059 | 适用（弱） | 手动开户 `phoneHash()` 为 null，不把明文手机当哈希留下 |
| PGM-066 | 适用 | 「员工权限按单一功能最小颗粒度…角色和员工级完全自定义」「权限点首批预置」；界面全中文在 remainingValidationMessagesAreChinese 触及后台报错 |

映射含 PGM-008/032–036 等员工手机章节，是聚合并集，不是本三文件实现面。不把未映射的 PGM-009「权限和职级」入口卡当成锁外新判据。

---

## 6. 对抗式（grilling 自问自答）

❓ **Q1 内存仓是否把授权算法整个换掉？**
➡️ AccessServiceTests 的仓实现仓储，AuthorizationService 仍按 effects 算 DENY/过期/日常桥。不是整段替换 AuthorizationService。但 `findPermissionEffects` 漏岗位 ROLE 并集，等于换掉 JDBC 组装的一半（GR-01）。AuthorizationServiceTests 的仓是效果桩，故意只测判定层，可接受。

❓ **Q2 DENY 覆盖 GRANT、单次消耗、审批层级有没有假绿？**
➡️ DENY：两套测试都注入 GRANT+DENY 或 override DENY，断言 grants 不含。单次消耗：AuthorizationServiceTests 真改 consumed 列表 + TOCTOU 返回 false；AccessServiceTests 的 `markSingleUsePermissionConsumed` 恒 true，但本类不走单次 require。审批层级：policyCode `permission_grant_l1/l2` 有断言。未发布混跑在 approve 被「直接受益人」打断，不能用来证明发布口径假绿。

❓ **Q3 测试名是否等于断言？**
➡️ `savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants` 注释写「重算有效权限」「派生权限同步失效」，断言是 `positionRoles` 与薪资。停用后内存仓仍保留 `templateApplications` active（`replacePositionGrants` 只 `positionRoles.put`）。**名称大于断言。** JDBC 换岗撤销由 `positionReplacementRevokesOnlyItsOwnPreviousApplications` 另测，不能给 AccessService 停用路径的有效集背书。

❓ **Q4 把被测改坏会不会红？**
➡️ 删 AccessService 里停用分支的 `replacePositionGrants`：内存测试会红（positionRoles 仍旧值）。删 JDBC `findPermissionEffects` 的岗位 JOIN：本模块三文件**都不红**（内存不走该 SQL；JDBC 类不调 findPermissionEffects）。这是关键分支缺口。

❓ **Q5 30 档职级首次使用会不会在本模块测试里翻车？**
➡️ 库内 30 行已用 SELECT 核实。`listStaffLevelsReturnsSeededLevels` 只放 2 档。首次「列出三十档」走 JDBC 列表，不在本内存测试。属数据/迁移面（M034 已闭环），不为本理论缺口补需求（O-2）。

❓ **Q6 未发布测试是否应入本模块队列？**
➡️ 否。V0.03 与 origin V0.01 对平台管理员商户范围断言相反；MASTER_* 依赖 origin 没有的常量。入队会把未发布工作流塞进 origin/main 检查点，违反 D-029。

❓ **Q7 assumeTrue 会不会让门禁空转？**
➡️ 缺三变量时 JDBC 类整类 skip。本轮有变量、Skipped=0。CI/AGENTS.md 已写死三变量。不单独立项；记观察。

---

## 7. Findings

### GR-01（席乙临时编号）— 入拟修复队列

- **文件:行：** `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java:1567-1617`（测试与注释）；`:1687-1688`（`replacePositionGrants`）；`:2011-2075`（`findPermissionEffects`）。对照 origin/main `JdbcAccessRepository.java:829-849`（换岗撤销旧 `position_application_ids` 并更新 `assigned_role_id`）、`:866-874`（`user_position_assignments` ⋈ `roles` ⋈ `role_permissions` 并入 GRANT）。
- **原文（测试名/注释）：**
  ```
  // M031-002（治理地图「系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效」）：
  // 保存岗位映射后在岗人员按新映射重算、薪资不变；映射停用则收回派生。
  @Test
  void savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants()
  ```
  断言实际是 `positionRoles` 从 `role_recompute_old` → `role_recompute_new` → `null`，以及薪资 500000 不变。同文件 `:520-521` 明确岗位角色**不**写入 `userRoles`。
- **更长上下文：** `replacePositionGrants` 内存实现整段为 `positionRoles.put(userId, roleId)`，忽略 `applicationIds`，不改 `templateApplications`。`findPermissionEffects` 只扫 overrides / `userRoles` / `templateApplications` / `temporaryGrants`，**不读 `positionRoles`**。AccessService 停用映射时调用 `replacePositionGrants(userId, null, null, List.of(), …)`（`AccessService.java:2154-2157`）。
- **正常触发 / 影响：** 第一次正常「改岗位映射 / 停用映射后看有效权限」：生产 JDBC 会撤销岗位派生模板应用并把岗位角色并入有效集；本测试用内存仓 + 不读 `AuthorizationService.effectivePermissionsForUser`。因此：(1) 删掉 JDBC 岗位 JOIN，本模块测试仍绿；(2) 停用后内存里模板应用仍是 active，若有人用有效集断言会与生产不一致。`assignUserPositionAppliesMappedRoleAndTemplate` 只证明写了 positionRoles，不证明最终权限含岗位角色独有码。
- **最小拟处置：** 内存 `replacePositionGrants` 按 JDBC 语义撤销该用户旧岗位 `applicationIds` 对应的 active 模板应用，并让 `findPermissionEffects` 并入 `positionRoles`→`roles.permissionCodes`；`savingAPositionMapping…` 在停用后断言有效集不再含派生码、旧岗位模板应用为 revoked。不改产品代码（除非对照后发现 SUT 真漏，本轮 origin SUT 有 JOIN 与撤销 SQL）。
- **四分类顺序：** 冲突？否（与 HTML「重算有效权限/派生失效」同向，不是相反）。细化？是（PGM-066 / BSM-003 权限分岗的测试下位）。技术性？夹具不一致也是工程缺陷，但能指向上位 HTML，停在细化。添加？否。
- **工程缺陷是否成立：** **是**（GEN-003 测试名/有效断言、GEN-008 夹具与 JDBC 约定）。
- **冻结 ID：** GEN-003、GEN-008；业务上位 PGM-066、BSM-003（不另造锁外判据）。
- **2.3 依据：** 类型 4：全文引用上述行；类型 2：origin worktree 54 绿不能证明该有效集分支；类型 1：BSM-003 HTML「权限分岗」/ PGM-066「角色和员工级完全自定义」。

### W 项（仅未发布版，不入修复队列，D-029）

| ID | 哪一版 | 事实 |
|---|---|---|
| W-1 | 仅主工作区 V0.03 + 未发布 AuthorizationService | `currentMerchantScopeReturnsBoundMerchantForPlatformAdmin` / `currentMerchantIdQuietlyReturnsBoundMerchantForPlatformAdmin` 断言 `contains("merchant")`。origin V0.01 同场景断言 **empty**，且注释写明 seed 误绑 merchant_id 时平台管理员必须看全量。origin SUT 先 `isPlatformAdminBridge` 再 empty；未发布 SUT 有商户号就返回该号。 |
| W-2 | 仅 V0.03 | 三条 MASTER 测试 import `SessionIssuer.MASTER_ACCOUNT_TYPE`。`git show origin/main:…/SessionIssuer.java` 无该常量；origin+V0.03 无法编译。 |
| W-3 | 仅未发布 JdbcAccessRepository | `M006JdbcRegressionTests.temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce` 混跑 BadSqlGrammar `column "grant_group_id" does not exist`（TEMP 表无该列）。origin 同测试 12/12 绿（SQL 用 `id::text`）。 |
| W-4 | 仅未发布 SupervisorReviewService | AccessServiceTests 6 条 `reviewGrant`：`退款、免单、授权与资金事项必须登记直接受益人`。测试文件与 origin 逐字节相同；origin worktree 54/54 绿。 |

W-1…W-4 与 M032 已登记未发布工作流同源，本模块新证据是**本三文件在未发布 SUT 上的红/断言反转**。不重复把 SUT 拉进本模块队列。

### 观察（不立项）

- **O-1** `createAccessUserWithAllowedAccountTypeSucceeds` 循环仅 `outsourced_vendor`。
- **O-2** `listStaffLevelsReturnsSeededLevels` 只含 2 档；psql 证实库内 30 档。30 档目录属已闭环迁移，本内存测试不是该列表的权威面。
- **O-3** `remainingValidationMessagesAreChinese` 反射私有 `normalizeGrantRequestType`。
- **O-4** `effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard` 只读写死的 `wildcard()` 标志，不读 grants 是否等于权限字典。
- **O-5** `assumeTrue` 缺环境会 skip JDBC 类；本轮未发生。
- **O-6** AccessServiceTests `markSingleUsePermissionConsumed` 恒 true；本类无依赖，单次消耗由另外两类覆盖。

不为「角色码与模板码完全无交集」的纯理论组合另开添加项。GR-01 针对的是夹具已声明「角色不进 userRoles」且 JDBC 另有岗位 JOIN 这一**现有**分叉。

---

## 8. 三线收口

- 第一性原理：三文件均作答；未验证前提已点名内存仓 ≠ JDBC 岗位授权组装。
- 顺从式：52 项均给适用或无本面及理由；GEN-003/008 因 GR-01 未完全满足。
- 对抗式：grilling 七问已自答；DENY/单次/外包在 AuthorizationServiceTests+JDBC 上能红；岗位最终权限在 AccessServiceTests 上不能证明改坏 JDBC JOIN 会红。

HTML 三份只读，未改。未改计划/findings/ledger，未 fetch/commit/push/merge/deploy。未建脚本/日志产物（一次性 worktree 已删除）。

---

## 9. 本席 B1 结论

**FAIL**

理由：存在成立的工程缺陷 GR-01（细化，GEN-003/GEN-008），须进 B2 定稿后才能谈修复；不得因 origin 87/87 绿或「看起来像测了有效权限」而 PASS。W-1…W-4 不入队。无添加项、无 HTML 修改申请、无 questions。跳过 B3 的前提不成立。

（完）


# M036 B2-r1 整批归并复核（Grok 乙席）

- 席位：Grok 固定只读外审席乙（grok-4.6 / xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 正本：`/home/james/complex-management` main 主工作区。
- 提示词：`160-audit-prompt.md` V0.17；runbook V0.02。已冻结判据不加判据。
- 输入（本席独立实算）：
  - 本文件上方 V0.01 B1 前缀 **36104** bytes，SHA256 `7cd36b38f3da7ab31d88464c781b9650aec7633a4b58cc5b5deab1c5baa41242`（FAIL：GR-01，W-1…W-4，观察 O-1…O-6）
  - `modules/M036-b1-self-raw.md` **仅 V0.01 前 53858 bytes**，SHA256 `f933fa98ec14188bb8158654855e10fc2410c6ee4ce15a2b88b74b9318c81fcc`（席甲 FAIL：CX-M036-001，OBS-1…6、W-3/5/6、XM-M036-A）。全文现 55470 bytes（SHA256 `98b60d6f9fa3a39ed0316d7c98caaadfbe9b66943a1d2c66aafdd8e363cabbf2`）在该前缀之后另有追加段；本席按派发禁令 **未把该追加段当证据、未采信其票面**。
  - `findings.md` V3.11 全文件 SHA256 `f25edd57002f0ed62f219eb427321c3164e0b8e6e414c744bfb52919d91131b4`；只读顶部「当前 M036 B2-r1 整批候选」段。其下历史模块定稿段不作本批差集材料。
- 锁：本席再算 `plan/criteria-lock.md` SHA256 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 B1 独占行同值）。**本段不写新的独占锁哈希字段行。**
- 范围：只审本批归并 / 分类 / 队列 / 最小修复边界，不重新审模块。未改源码 / 文档 / HTML / lock / findings / ledger；未 fetch / commit / push / merge / deploy；未写新产物；未对任何数据库执行写语句（本票只读已有日志与源码，未跑 Maven、未 psql 写）；未用 MCP 或子代理。
- 本席 B2-r1 票：**PASS**（整批候选同意；001–002 逐 ID 全 PASS；添加 0、不发 2.2；无 HTML 申请）。

### 完整差集

| 正式ID / 登记号 | 本次来源（不改写原意） | 候选分类 / 缺陷 / 队列 | 本席票 |
|---|---|---|---|
| M036-001 | 本席 GR-01（席甲 B1 只记成 OBS-1，并误称相应真实语义已由别的测试守住） | 细化 / 是 / 是 | **同意入本模块队列**。内存仓 `applyTemplate` 改为不重复 id、以及本模块 `M006JdbcRegressionTests` 补岗位 JOIN／置空收回，属本席最小拟处置要对齐 Jdbc 语义时的必要部分，不是扩范围 |
| M036-002 | 席甲 CX-M036-001（本席 B1 未报） | 技术性 / 是 / 是 | **同意**。证据独立成立；是本聚合「最终权限计算」测试面 `isActive` 的关键未锁分支 |
| W-M036-1…3 | 本席 W-1＋W-2、W-3、W-4 ＝ 席甲 W-6、W-3、W-5 | D-029 只登记不入队 | **同意改号**，避免与 M032 的 W-1…W-4 撞号 |
| XM-M036-A | 席甲 §6：origin/main 全量 3471 测中 20 红 | →各自模块 | **同意登记**；20 条无一应由本模块三文件处置 |
| （无新 ID） | 两席其余观察＋席甲 7 个变异去向 | 只登记不入队 | **同意；无漏并、无应升队项**；三个幸存者判第二道闸只登记，本席同意 |

冲突入队 0。细化入队 1（001）。技术性入队 1（002）。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 2。本模块写入集合＝`backend/src/test/java/com/complex/platform/access/AccessServiceTests.java`、`backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java`、`backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java`。不改被测代码、不加迁移。AuthorizationServiceTests 发布变体＝origin/main 版＋同一处补丁（D-029）。

**整批候选：同意。**

---

### 一、差集核实（逐项独立）

**一、M036-001（GR-01，细化）。票：同意。两点补丁仍属最小。**

独立核源（不重审模块）：

1. 测试闸与夹具分叉。`AccessServiceTests.java:1567-1617` 名称／注释写「按新映射重算有效权限、停用则收回派生」；断言只打 `positionRoles` 与薪资 500000。同文件 `:520-521` 明确岗位角色不进 `userRoles`。内存 `replacePositionGrants`（L1687-1688）整段 `positionRoles.put`，忽略 `applicationIds`，不改 `templateApplications`。内存 `findPermissionEffects`（L2011-2075）只扫 overrides／`userRoles`／模板应用／临时授权，**不读 `positionRoles`**。停用路径 `AccessService.recomputeUsersOnPositionTemplate`（L2154-2157）调用 `replacePositionGrants(userId, null, null, List.of(), …)`。本席 B1 Q4：删 JDBC 岗位 JOIN，本三文件都不红。
2. 生产语义。origin/main `JdbcAccessRepository.replacePositionGrants` 先把 `position_application_ids` 指向的旧应用置 revoked，再写新指针；`findPermissionEffects` 另有 `user_position_assignments ⋈ roles ⋈ role_permissions`，来源标 ROLE（origin 与主工作区该查询文本相同）。`assignUserPositionDirect`（L1515-1524）先 `applyTemplate` 再把返回的应用 id 交给 `replacePositionGrants`。
3. 席甲 OBS-1 误称。席甲 B1 GEN-008／OBS-1 写「相应真实语义分别由 M006JdbcRegressionTests 与 AccessJdbcIntegrationTests 守住」。本席核：`M006JdbcRegressionTests` **0 处**调用 `findPermissionEffects`；`positionReplacementRevokesOnlyItsOwnPreviousApplications` 只断言换岗撤旧应用、不断言岗位角色码进有效集。`AccessJdbcIntegrationTests` 对 `findPermissionEffects` 的调用是临时模板／整组撤销／到期／覆盖／冻结，**无** `assigned_role_id`／岗位 JOIN。M035 已闭环（检查点 PR #476），本模块不能改那份文件。
4. 变异 no_position_role_join。只读 `/home/james/audit-tmp/m036_mutation2.log`：origin/main `3ca1b507` 一次性 scratch，基线 suites=263 tests=3471 red=20；`mutated no_position_role_join JdbcAccessRepository.java` 后仍 red=20、无 NEW-RED 行。岗位派生角色并入有效权限在内存层与真库层都没有测试。席甲 B1「已有测试守住」不成立，升队正确。
5. 补丁其一：不重复的应用 id。内存 `applyTemplate`（L1928）现写 `"app_" + userId`，同一用户两次套模板 id 碰撞。Jdbc 生产是 `"pta_" + UUID`（`JdbcAccessRepository.java:584`）。若只把 `replacePositionGrants` 改成「先撤上一次记录的岗位应用 id、再记新 id」，第二次 `applyTemplate` 仍返回 `app_<userId>`，撤旧会把刚插入的新应用一并 revoked，定岗后有效集断言必假红。本文件无其它断言依赖 `app_` 前缀（全文件仅 L1928 一处）。故改成不重复 id 是对齐 Jdbc 撤旧语义的前提，不是第三条缺陷、不是扩范围。B3 与 Jdbc 一样用 UUID（或等价唯一键）即可。
6. 补丁其二：本模块 JDBC 面。本席 B1 §4.3 已写「`findPermissionEffects` 的岗位 JOIN 本类未测（与 GR-01 同一缺口的 JDBC 面）」。候选落到 `M006JdbcRegressionTests`（本模块 FILES），补 TEMP 表：`findPermissionEffects` 返回岗位派生角色码（ROLE 来源），`replacePositionGrants(user, null, null, [], …)` 之后不再返回。这是把 Q4 的生产 JOIN 锁住；只改内存夹具仍会出现「删 JOIN、全库 0 新红」。AccessJdbcIntegrationTests 已闭环，不能改。同意。
7. 测试断言加严。候选要求旧角色／新角色／模板各一个互不相同的码，并经 `authorizationService.effectivePermissionsForUser` 断言定岗／改映射／停用三态。现测三处都用 `ops.production_readiness.check`，有效集无法区分来源。改用互不相同的码是让断言能红，不是本席 B1 明确拒绝的「为无交集纯理论组合另开添加项」。
8. 分类。冲突？否（与 PGM-066「保存岗位…立即按变更后的岗位权限模板重算有效权限，旧岗位…派生权限同步失效」同向）。细化？是（测试下位；停在细化，不落到技术性）。添加？否。冻结 GEN-003／GEN-008，上位 BSM-003、PGM-066。不扩锁。
9. 边界。只改本模块两个测试类里的夹具＋该测试＋`M006JdbcRegressionTests` 一条；不改 `AccessService`／`JdbcAccessRepository`；不加迁移。写入集合三文件与候选一致。

**明确同意**：001 细化／是／是。两点补丁仍属最小完整处置。

**二、M036-002（CX-M036-001，技术性）。票：同意。本聚合关键分支；本席 B1 漏报。**

独立核源：

1. 闸只在 Java。origin/main `AuthorizationService.isActive` L529-536（主工作区 V0.13 同文 L520-528）：`validFrom != null && checkedAt.isBefore(validFrom)` → false；否则看 `validUntil`。`requirePermissions`／`computeEffectivePermissions`／`currentActorHasPermission`／`holdsPermissionViaRole` 都先 `filter(isActive)`。`JdbcAccessRepository.findPermissionEffects` 模板／临时授权 SQL **SELECT** `valid_from`，WHERE 只有 `status='active'`，不按生效时间过滤。`expireLapsedEffects` 只按 `expires_at`／`valid_until` 把到期行标 expired，**不管** `valid_from` 未到。
2. 测试面零锁。origin/main `AuthorizationServiceTests` 21 个测试：`expiredTemporaryTemplateGrantDoesNotAuthorize`、`effectivePermissionsExcludesExpiredGrant`、`outsourcedTemporaryGrantExpiresAndNoLongerAuthorizes` 都是到期一头。`temporaryGrant` 助手把 `validFrom` 写死 `2020-01-01`，参数 2099 是 **validUntil**（仍有效），不是「尚未生效」。主工作区 V0.03 24 个测试同样没有未来 `validFrom`。两版都成立。
3. 变异 ignore_valid_from。只读 `/home/james/audit-tmp/m036_mutation.log`：同基线 3471／红 20；`mutated ignore_valid_from AuthorizationService.java` 后仍 red=20、无 NEW-RED。全库没有测试因「未到生效时间也放行」变红。
4. 首次正常使用。`PermissionTemplatesPage.tsx` L381、L501 标签「生效时间」（`permission-template-edit-valid-from`／`permission-template-valid-from`）。`PermissionTemplateRequest`／`TemporaryGrantRequest` 均有 `validFrom` 组件。管理员把模板生效时间设到明天再套用，是页面提供的输入，不是理论路径。
5. 是否本聚合关键分支。M036 聚合维度是「最终权限计算／后台验证／第2段」；`AuthorizationServiceTests` 正是锁 `isActive` 的文件。`isActive` 三支：consumed（已有单次消耗测试）、validUntil（已有到期测试）、validFrom（0）。缺的是同一函数剩下的时间窗端，不是旁路或防御纵深。同意入队，不同意降为观察。
6. 分类。冲突？否（SUT 已实现该闸，与「可设有效期」同向）。细化？否（不补业务规则、不改被测代码）。**技术性**（测试缺口，无业务发明）。添加？否。冻结 GEN-003／GEN-005；上位 PGM-066 原文 L656「临时授权可设有效期或单次使用」、L659「有时间限制、到期自动失效」作旁证，不扩锁。
7. 边界。AuthorizationServiceTests 补一条：给员工一条 `validFrom` 在 2099 年的授予，断言 `requirePermission` 拒绝且 `effectivePermissions().grants()` 不含该码。不改被测代码。两版打同一处补丁（插在 `effectivePermissionsExcludesExpiredGrant` 之后、私有助手之前，两版都有这段）；发布用 origin/main V0.01＋补丁（D-029）。B3 注意：不得复用现助手的 2099＝截止时间；必须把 **validFrom** 设到未来。

**明确同意**。本席 B1 只核了到期测试、未做 `ignore_valid_from` 变异，漏报成立；候选未改写席甲原意。

**三、W-M036-1…3。票：同意改号，不入本模块队列。**

对应关系独立核对，与候选一致：

| 新号 | 本席 B1 | 席甲 B1 | 事实（仅未发布版） |
|---|---|---|---|
| W-M036-1 | W-1＋W-2 | W-6 | V0.03 两条改名测试把平台管理员商户范围断言反转为 `contains("merchant")`；三条 MASTER 测试 import `SessionIssuer.MASTER_ACCOUNT_TYPE`，origin/main 无该常量，origin＋V0.03 无法编译 |
| W-M036-2 | W-3 | W-3 | 未发布 `markSingleUsePermissionConsumed` 用 `grant_group_id`，本类 TEMP 表无该列，`temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce` bad SQL grammar；即 M032 W-3 在本模块文件上的表现 |
| W-M036-3 | W-4 | W-5 | 未发布 SupervisorReviewService 要求直接受益人，AccessService 发起授权申请不带，AccessServiceTests 6 条 ERROR；该工作流发布时须同批改调用点与这些测试 |

M032 已占用 W-1…W-4 登记 2026-09-13 未发布工作流。本模块改用 W-M036-n 避免台账撞号，同意。不入本模块修复队列、不随本模块发布（D-029）。

**四、XM-M036-A。票：同意登记转各自模块；20 条无一应由本模块处置。**

变异日志基线 20 红与席甲 raw §6 清单逐条相同：FinanceControllerTests 12（ApplicationContext 加载失败）、ComplianceControllerTests 3（期望 200/400 实得 403）、FacilityEquipmentArchiveDeleteJdbcIntegrationTests 3、AmusementServiceTests 1、CustomerGuardedActionAndRejectionAuditTests 1。没有任何一条属于 `AccessServiceTests`／`AuthorizationServiceTests`／`M006JdbcRegressionTests`。`ignore_valid_from`／`no_position_role_join` 都不改变这 20 条。后端测试不在 CI predeploy-gates——席甲已记，不升本模块队列。本席不把其中任何一条收回本模块。

**五、其余观察。票：全部只登记；无漏并须升队项。三个幸存者同意判第二道闸。**

候选「只登记不入队」已覆盖两席观察合取：

- `createAccessUserWithAllowedAccountTypeSucceeds` 单元素循环与恒假条件（本席 O-1＝席甲 OBS-3）；「非外包须先注册」由 M037 变异 `no_staff_registration` 抓到
- `markSingleUsePermissionConsumed` 内存恒 true（本席 O-6＝席甲 OBS-1 第三点）；单次消耗由另外两类覆盖
- `assumeTrue` 缺环境整类跳过（本席 O-5＝席甲 OBS-4）
- M006JdbcRegressionTests 4 张手写 TEMP 只含被测 SQL 用到的列（席甲 OBS-2）；origin/main 版与真表相容
- `copyPermissionTemplateKeepsSnapshot…` 源模板前后权限项相同（席甲 OBS-5）；快照真语义在 AccessJdbcIntegrationTests
- `listStaffLevelsReturnsSeededLevels` 内存只 2 档（本席 O-2）；30 档属已闭环迁移
- `remainingValidationMessagesAreChinese` 反射私有方法（本席 O-3）
- `effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard` 只读恒 false 的标志位（本席 O-4）

席甲 OBS-1 前两点（岗位 JOIN／换岗不撤旧应用）已升入 001，不再当观察，正确。

席甲另跑的 7 个变异（相对 002 的 ignore_valid_from 之外），去向独立核日志：

| 变异 | 相对基线 NEW-RED | 去向 |
|---|---|---|
| multi_first_only | M006SecurityRegressionTests.twoPermissionReview… | 有守，M037 |
| no_staff_registration | M006SecurityRegressionTests.manualEmployeeCreation… | 有守，M037 |
| unclassified_as_low | AccessServiceTests.grantPolicyRejectsAnyUnclassified… | 有守，本模块已有测试 |
| no_outsourced_assignee_scope | 设施巡检／工单／进场报备 8 条 | 有守，非本模块 |
| ignore_actor_status | 0 | 见下 |
| ignore_unknown_permission | 0 | 见下 |
| via_role_ignores_deny | 0 | 见下 |

三个幸存者——同意只登记，不升队：

1. **停用主体**（ignore_actor_status）。`permissionGrant` L72 拒非 active。上游 `ProductionAuthService.authenticateToken` L307-308 `.filter(session -> "active".equals(session.userStatus()))`，注释写明停用或锁定的凭据令已签发令牌立即失效。停用账号到不了 AuthorizationService；本处是第二道闸。HTTP 正常使用不单独暴露。
2. **未登记权限码**（ignore_unknown_permission）。去掉 `knownPermission` 后，普通主体仍因无 GRANT 被拒；平台管理员桥只认字典日常码。`isMasterAuthority` 在 knownPermission 之后短路放行，只有最高权限会对未登记码放行，无业务后果。不立项。
3. **按角色持有忽略显式拒绝**（via_role_ignores_deny）。`holdsPermissionViaRole` 的 DENY 只服务 `currentSupervisorLevel`。审批入口先 `requirePermissions(..., "governance.supervisor_review.approve")`（`AccessController.java:153、197`），显式拒绝在 requirePermissions 已拒。本处是第二道闸。

与 002 的差别：002 的 validFrom **没有**上游闸（SQL 不滤、expireLapsedEffects 不管未到生效），故入队；这三处有上游或无业务后果，保持观察。

无应升队项。本席不为「角色码与模板码无交集」另开添加项——001 处置里改用互不相同的码是让既有测试能红，不是新需求。

**六、本批无添加项、无 HTML 申请、无 questions。票：同意。**

`docs/audit-2026-09/questions/` 无 M036 文件。不申请改三 HTML。不 BLOCKED-ASK。

---

### 三、最小修复边界

一次 B3 允许的写入集合＝三个测试文件。禁止改被测 Java、已应用迁移、前端、OpenAPI、三 HTML、锁、已闭环 M035 的 `AccessJdbcIntegrationTests`。

| ID | 允许（本模块 B3） | 禁止 |
|---|---|---|
| 001 | 内存仓：`applyTemplate` 生成不重复应用 id（与 Jdbc `pta_`+UUID 同形即可）；`replacePositionGrants` 先把该用户上次岗位应用 id 置 revoked，再记新角色与新应用 id；`findPermissionEffects` 把 `positionRoles` 指向的启用角色权限码并为 ROLE 来源。`savingAPositionMapping…` 改用三个互不相同的码，经 `effectivePermissionsForUser` 断言定岗含旧角色码＋模板码、改映射含新角色码且不含旧角色码、停用后三者都不含且旧岗位模板应用为 revoked。`M006JdbcRegressionTests` 补一条 TEMP 表测试锁岗位 JOIN 与 `replacePositionGrants(…, null, null, [], …)` 之后不再返回该 ROLE 码 | 改 `AccessService`／`JdbcAccessRepository`；改已闭环 AccessJdbcIntegrationTests；只改测试名不改夹具；继续用 `app_`+userId |
| 002 | 两版 `AuthorizationServiceTests` 同一处补一条：授予 `validFrom=2099`，`requirePermission` 拒绝且 `effectivePermissions().grants()` 不含该码。发布＝origin/main V0.01＋该补丁 | 改 `AuthorizationService.isActive` 或 Jdbc SQL；把 2099 写在 validUntil 上冒充本条；随本模块带出 V0.03 的 MASTER／商户范围反转 |

B3 实施非否决提示（不改变本票）：`M006JdbcRegressionTests.openTemporaryTables` 默认 TEMP `permission_templates` 只有 `id/single_use`，新测须为 `findPermissionEffects` 实际 JOIN 的表自建够用的 TEMP 列（含 `roles.status`、`user_position_assignments.assigned_role_id` 等），不得写 public 业务表。002 不得复用 `temporaryGrant(...)` 助手（其 2099 是截止时间）。

---

### 四、添加／HTML／questions

无添加。无 HTML 修改申请。无 questions。不 BLOCKED-ASK。

---

### 分类／队列／允许的最小修复边界（收口）

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止开工 B3。一次 B3 只改上表写入集合。

- 001：细化／是／是。不重复 id 与 JDBC JOIN 补测是最小完整处置的必要部分。
- 002：技术性／是／是。本席 B1 漏报，候选未改写；属本聚合 `isActive` 关键分支。
- W-M036-1…3：D-029 登记，同意改号。
- XM-M036-A：同意；20 红不收回本模块。
- 观察全部只登记，三个第二道闸幸存者不升队。无添加、无 HTML 申请、无 questions。

### 结论

全批差集无原意改写、无漏并、无应升队观察。GR-01→001（席甲 OBS-1 撤回「已有测试守住」，采本席细化）；CX-M036-001→002（本席 B1 漏，核源成立，采技术性）。逐 ID：001 PASS、002 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M036 IN_PROGRESS；B3=0。

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **36104** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
