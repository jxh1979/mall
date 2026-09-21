CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M036 B5-r1 席乙 Grok 独立审计报告

- 席位：乙（Grok 4.6 / reasoning xhigh）
- 步骤：B5 修复后整模块全量重审（B3 累计 1，r1）
- 报告版本：V0.01（指定路径原先不存在，本次创建）
- 控制提示词：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 B1/B5 三线
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M036（3 文件）
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11（本席独立 `sha256sum`，见首行；不得抄基准）
- 正本：`/home/james/complex-management` main 主工作区
- after tree：`0b38ca49734ed5f50c724214285b7119ecf0c809`（本席 `ls-tree` 复核三文件 blob）
- 互盲：未读取 `modules/M036-b5-r1-self-raw.md` 正文；全仓检索若扫到该路径只见过 `stat` 字节数，不采信。旧 B1/B2/B4 可读，本席读了 B1 grok-raw 与 findings 台账头部以核对「已定只登记观察不得无新证据改判」，不复用 B2/B4 票面当本轮证据。
- 本席结论：**PASS**（001、002 修复在发布口径上被本席独立变异打红后还原全绿；整模块 52 项逐项核对完毕；无新工程缺陷入队；W-M036-1…3 仍只在未发布版成立，不入队）

---

## 0. 技能预检（按环境如实）

本席在本机实际目录核过、本轮按 V0.17 §6 收窄口径用过：

| 技能 | 路径核验 | 本轮用法 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` 存在 | 开工先核技能 |
| `graft` | `/home/james/.claude/skills/graft/SKILL.md`；CLI `/home/james/.npm-global/bin/graft` | skeleton `AuthorizationService`；grep `isActive` / `replacePositionGrants` / `findPermissionEffects` |
| `grilling` / `grill-me` | mattpocock `productivity/grilling`、`grill-me` 存在 | 对抗线用其问答结构**自问自答**；不向操作员发问、不派子代理 |
| `ponytail` ultra / `ponytail-review` | `ponytail/4.9.0/skills/` 存在 | 不为理论路径补需求；三处新断言 lean，不删 |
| `systematic-debugging` | superpowers 存在 | 只借「读错误 → 复现 → 追数据流」。变异失败栈追到对应断言行。未改业务代码 |
| `verification-before-completion` | superpowers 存在 | 先实跑 54+22+13、再独立三变异、还原后再跑三断言，才写 PASS |
| `diagnosing-bugs` | mattpocock engineering 存在 | 红环：三变异命令即 red-capable loop |
| `tdd` | mattpocock engineering 存在 | 用「断言能否因实现变坏而红、缝在公开接口」审测试，不写新测试 |
| `codebase-design` | mattpocock engineering 存在 | 内存仓 vs Jdbc 是同一 `AccessRepository` 缝上的两个 adapter |
| `writing-for-agents` | mattpocock productivity 存在 | 本 raw 按步骤+完成判据落盘 |
| `long-running-background-tasks` | grok bundled 存在 | 发布口径 Maven 约 9s 前台结束；变异串约 38s 曾被后台接管，完成后读退出码 |
| `git-guardrails-claude-code` | mattpocock misc 存在（可选） | 不 fetch/commit/push/merge；worktree remove 不用 `--force` |
| `resolving-merge-conflicts` | 存在 | 本轮无合并冲突，未调用本体步骤 |
| `i-have-adhd` | marketplace/cache 存在 | **未按该技能压缩全文**。V0.17 要求完整 raw，与极简输出冲突，以提示词完整落盘为准 |

明确不用 / 缺口：

- Arc `audit` 默认派并行 reviewer：用户与 V0.17 禁止第三席与子代理，**未派**。
- `writing-plans` / `test-driven-development`（superpowers 落测试）/ `requesting-code-review`：B5 只读，不修。
- `batch-grill-me` / `ubiquitous-language`：本环境技能目录**未核到**，记缺口，不冒充已调用。
- `research`：V0.17 §6 已划掉。
- 未使用 MCP、未派子代理、未问人。
- `using-git-worktrees` 技能本体不用；本轮一次性 origin/main worktree 是派发硬性取证，跑完已删除。

graft 本轮 tokens saved ≈ 4,635 + 5,608 + 52,356 + 57,804 ≈ 120,403。

---

## 1. 环境事实（命令实算，非手抄）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```

sidecar `plan/criteria-lock.sha256` 同行相等。ledger 顶部同值。本席只报实算，不改 ledger / findings / 锁。

| 项 | 值 |
|---|---|
| 主工作区 HEAD | `e456ba96c84185c8d39f18f548011daac48a6466` |
| origin/main | `3ca1b5076028215a945ede2ae47b71f5786a1c70` |
| after tree | `0b38ca49734ed5f50c724214285b7119ecf0c809` |
| JAVA_HOME | `/usr/lib/jvm/temurin-21-jdk-amd64`（21.0.12.1） |
| complex_test Flyway | `SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1` → **430** |
| staff_levels | `SELECT count(*) FROM staff_levels` → **30** |

三文件主工作区（after tree 口径）与发布变体：

| 文件 | 行（newline 计数） | SHA256 | git hash-object | 相对 after tree |
|---|---|---|---|---|
| `AccessServiceTests.java` V0.09 | 2160 | `be3e9be417a860350f57d091e401bf5c9cef772b308849335454035911600766` | `ebdf8cae74f11fd4163d8c6454d9a6fee8743a69` | 与 after tree blob **相同** |
| `AuthorizationServiceTests.java` 主工作区 V0.04 | 615 | `6d409fb7524492bc217c79c4afbe172787d1f530465a5b927734dc7b4f2a9c5e` | `f3e040d0f5393d8b65c7e84bc7e99be6cddcb2d8` | 与 after tree blob **相同**（25 `@Test`，含未发布工作流） |
| `AuthorizationServiceTests` 发布变体 V0.02 | 584 | `3b3b6c2b2b6c042d92bb0c8dd2721d80def1ace85417197fd907f5d570f6671a` | `cefc8dc8e32ebe44da6dd82bdb73031b3a731436` | 不在 after tree；`/home/james/audit-tmp/m036_AuthorizationServiceTests.publish.java`（22 `@Test`） |
| `M006JdbcRegressionTests.java` V0.14 | 342 | `b16e34e2db9d05f6a78d8a08342cc0d3c97363705634e7aba0af53aba07e2cf9` | `ece6ff7e649d2eec2472b7563361e1ffbd4a2ba6` | 与 after tree blob **相同** |

D-029：发布口径是 origin/main 的 SUT + 上述三份修后测试（AuthorizationServiceTests 用发布变体）。两版都读；只在未发布版成立的记 W-M036-n，不入修复队列。

被测追证（只读，不重开已闭环模块）：origin/main worktree 内 `AuthorizationService.isActive` L529–534、`JdbcAccessRepository.replacePositionGrants` L835–857、`findPermissionEffects` 岗位 JOIN L873–881、`AccessService.assignUserPositionDirect` L1515–1524。主工作区未发布 `AuthorizationService.currentMerchantScope` 不再对平台管理员返回 empty；未发布 `JdbcAccessRepository.findPermissionEffects` / `markSingleUsePermissionConsumed` 使用 `grant_group_id`。

---

## 2. 实跑（可执行产物，2.3 类型 2/4）

后端只从 `/etc/complex/complex.env` **逐行读出三个键**写入环境（URL 库名 `complex`→`complex_test`），**未 source** 该文件。未导出 `COMPLEX_DATABASE_APP_*` / `OWNER_*`。进程里原先已有 `COMPLEX_CONFIG_DIR` / `COMPLEX_HOME` / `COMPLEX_LOG_DIR`（不是本席 source 出来的），本席未再加。`psql` 只 SELECT。

### 2.1 发布口径：origin/main 一次性 worktree

```
git worktree add --detach /tmp/m036-b5-r1-origin-main origin/main
# HEAD = 3ca1b5076028215a945ede2ae47b71f5786a1c70
cp 主工作区 AccessServiceTests.java、M006JdbcRegressionTests.java
cp /home/james/audit-tmp/m036_AuthorizationServiceTests.publish.java → AuthorizationServiceTests.java
cp 主工作区未发布 V427 → worktree db/migration/   # complex_test 已应用到 430，缺文件 Flyway 校验失败
```

三文件 SHA 与源逐字节相同（本席 `sha256sum` 对照）。`@Test` 计数：54 / 22 / 13。

`psql -d complex_test -c 'SELECT current_database(), current_user'` → `complex_test` / `complex`。

```
mvn --batch-mode -Dtest=com.complex.platform.access.AccessServiceTests,com.complex.platform.access.AuthorizationServiceTests,com.complex.platform.access.M006JdbcRegressionTests test
```

结果（exit 0）：

```
M006JdbcRegressionTests        Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
AuthorizationServiceTests      Tests run: 22, Failures: 0, Errors: 0, Skipped: 0   # 发布变体 V0.02
AccessServiceTests             Tests run: 54, Failures: 0, Errors: 0, Skipped: 0
Tests run: 89, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS  Total time:  9.252 s
```

`assumeTrue` 未跳过 JDBC 类。

### 2.2 本席独立三变异（发布口径 worktree，打完还原）

只改 origin/main **被测实现**，不改测试。每发一枪只跑对应新断言，再 `git checkout --` 还原生产文件。

| 变异 | 改动 | 命令 | 结果 |
|---|---|---|---|
| ignore_valid_from | `AuthorizationService.isActive` 把 `validFrom` 闸改成 `if (false && …)` | `AuthorizationServiceTests#grantWhoseValidityHasNotStartedDoesNotAuthorize` | **FAIL** L351 `assertPermissionDenied`：Expecting code to raise a throwable（未到生效时间被放行） |
| no_position_role_join | `JdbcAccessRepository.findPermissionEffects` 岗位 JOIN 加 `AND 1=0` | `M006JdbcRegressionTests#positionDerivedRoleFeedsPermissionEffectsUntilThePositionGrantsAreCleared` | **FAIL** L268：实际 `[]`，期望 `("position.only.code", ROLE, GRANT)` |
| assign_passes_no_app_ids | `AccessService.assignUserPositionDirect` 把 `replacePositionGrants` 的应用 id 改成 `List.of()` | `AccessServiceTests#savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants` | **FAIL** L1616：实际 `["active","active"]`，期望 `["revoked","active"]` |

还原后三断言再跑：**Tests run: 3, Failures: 0**。`git diff` 三份被测文件为空。

### 2.3 未发布对照（非发布口径）

主工作区 `AuthorizationServiceTests` V0.04（25 `@Test`）编的是未发布 `AuthorizationService`：

```
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0  BUILD SUCCESS  0.926 s
```

新补丁 `grantWhoseValidityHasNotStartedDoesNotAuthorize` 在未发布版同样绿。平台管理员商户范围三条 MASTER 测试只在未发布 SUT 上有意义（W-M036-1）。

未再整跑主工作区 `AccessServiceTests` / `M006JdbcRegressionTests`：未发布 `SupervisorReviewService` 要直接受益人、未发布 JDBC 用 `grant_group_id`，与 B1 已登记 W-M036-3 / W-M036-2 同源。本席核源：未发布 `JdbcAccessRepository.java` L888–889 `grant_group_id::text AS source_id`、L1041 `WHERE grant_group_id = :source_id`；新真库测试调用 `findPermissionEffects`，未发布混跑会在同一列上炸。不升新号。

### 2.4 临时表不写 public（SELECT）

```
SELECT count(*) FROM public.user_position_assignments WHERE user_id='user'  → 0
SELECT count(*) FROM public.roles WHERE id='position-role'                  → 0
SELECT count(*) FROM public.role_permissions WHERE permission_code='position.only.code' → 0
SELECT count(*) FROM public.user_roles WHERE user_id='user'                 → 0
```

### 2.5 worktree 拆除

删除 V427、`.m036-env`、`backend/target`，tracked 测试文件 `git checkout --` 回 origin，然后 `git worktree remove /tmp/m036-b5-r1-origin-main`（**无 `--force`**）exit 0，目录不存在。主工作区 V427 与三测试文件仍在。

---

## 3. 全文覆盖（不抽样）

### 3.1 `AccessServiceTests.java` V0.09（2160 行 / 54 `@Test`）

被测：`new AccessService(InMemoryAccessRepository, …)` + 同实例 `AuthorizationService`。无 Spring 容器、无真库。`@BeforeEach` 新仓；`@AfterEach` 清 SecurityContext。无测试间共享静态可变状态。

| 行 | 测试 | 是否执行声称行为 | 改坏实现会不会红 |
|---|---|---|---|
| 83–132 | copyPermissionTemplateKeepsSnapshotWhenSourceChangesAndWritesAuditEvent | copy 后改源，读 copy 子项 | 能 |
| 134–168 | applyPermissionTemplateRequiresApprovalWhenTemplateHasHighRiskPermission | 高危模板 apply 403 | 能 |
| 170–207 | applyPermissionTemplateAppliesToManyUsersAndWritesAuditEvent | 两人落地 + 审计 | 能 |
| 209–235 | temporaryGrantCreateAndRevokeWriteAuditEvents | 创建/撤销 | 能 |
| 239–256 | createAccessUserWithPlatformAdminAccountTypeThrowsValidationError | 禁止创建 platform_admin | 能 |
| 258–275 | createAccessUserWithAllowedAccountTypeSucceeds | 循环只含 `outsourced_vendor` | 能，覆盖窄（O-1，B2 已只登记） |
| 277–301 | platformAdminWithMerchantAffiliationCanCreateUserForAnyMerchant | 平台管理员跨商户开户 | 能 |
| 303–320 | createAccessUserRequiresOrganizationScope | VALIDATION_FAILED | 能 |
| 322–352 | createAccessUserStoresManualEvidenceAndProvisionsCredential | 证据/凭证/审计 | 能 |
| 354–373 | merchantScopedCreatorCannotCreateUserForAnotherMerchant | CROSS_MERCHANT | 能 |
| 377–389 | assignStaffLevelSetsLevelAndDefaultsCurrencyToCny | 职级+CNY | 能 |
| 391–400 | assignStaffLevelRejectsUnknownLevel | UNKNOWN_STAFF_LEVEL | 能 |
| 402–411 | assignStaffLevelRejectsNonCnyCurrency | UNSUPPORTED_CURRENCY | 能 |
| 413–425 | assignStaffLevelRejectsCrossOrganization | CROSS_ORG | 能 |
| 427–438 | effectivePermissionsForUserRejectsCrossOrganization | CROSS_ORG | 能 |
| 440–453 | changeUserScopeRejectsTargetOutsideActorProject | CROSS_ORG | 能 |
| 455–460 | listStaffLevelsReturnsSeededLevels | 夹具 2 档 id | 能；与库 30 档脱节（O-2） |
| 462–485 | assignAndUnassignUserRoleUpdateUserRoles | 装配/卸下 | 能 |
| 487–531 | assignUserPositionAppliesMappedRoleAndTemplate | 写岗位/模板应用；本条仍不读有效集 | 对 positionRoles/userRoles 能红；有效集由 1569 条与 JDBC 新测试锁 |
| 533–551 | assignUserRoleRejectsRolePermissionsActorDoesNotHold | GRANT_EXCEEDS_OWN_PERMISSIONS | 能 |
| 553–574 | assignUserRoleRequiresApprovalWhenHighRiskGrantFlagIsEnabled | GRANT_REQUIRES_APPROVAL | 能 |
| 579–610 | positionMappingAndAssignmentDoNotRequireTheConfigurerToHoldTheHeldHighRiskCodes | 普通码立即生效、高危挂起 | 能（现内存仓会把岗位角色码并入 GRANT，DENY 挂起仍压住） |
| 612–661 | assignUserPositionLandsNormalPartAndHoldsHighRiskPartUntilChainApproves | DENY 挂起→审批释放 | 发布口径能红；未发布混跑 W-M036-3 |
| 665–701 | scopeChangeRevokesOldDerivedGrantsImmediatelyAndNewScopeWaitsForApproval | 旧派生清空、新范围待审 | 能 |
| 705–737 | scopeChangeApprovalRejectsWhenTheNewScopePositionMappingChangedWhilePending | GRANT_SNAPSHOT_CHANGED | 能 |
| 741–772 | scopeChangeAcrossProjectsLandsWhenTheApproverStaysInTheOldProject | 跨项目审批落地 | 能 |
| 776–809 | scopeChangeCancelsOldScopeHighRiskHoldsAndTheirDenyOverrides | 旧挂起 rejected | 能 |
| 813–854 | scopeChangeFreezesExtraGrantsBeyondTheNewTemplate | 直接角色/模板/临时/覆盖一并收 | 能 |
| 858–889 | removingTheHighRiskHoldDenyRequiresApproval | 不能无审批拆 DENY | 发布口径能；未发布 W-M036-3 |
| 893–924 | preExistingExplicitDenyIsNotReusedByTheHighRiskHold | 独立拒绝不进挂起单 | 能 |
| 927–955 | remainingValidationMessagesAreChinese | 三条公开入口 + 反射私有 normalizeGrantRequestType | 公开路径能红；反射不是产品入口（O-3） |
| 957–965 | scopeChangeRejectsUnchangedScope | SCOPE_UNCHANGED | 能 |
| 969–981 | grantPolicyRejectsAnyUnclassifiedPermissionInTheRequestedRole | unclassified 拒 + 请求表空 | 能 |
| 983–1005 | grantRequestRejectsPermissionsThatDoNotRequireApproval | GRANT_APPROVAL_NOT_REQUIRED | 能 |
| 1007–1039 | approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment | 审批落地角色 | 发布口径能；未发布 W-M036-3 |
| 1041–1072 | approvedTemporaryGrantExpiresByEffectivePermissionRead | 过期窗口有效集不含 | 发布口径能；未发布 W-M036-3 |
| 1074–1118 | auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse | requirePermission 真走授权 | 发布口径能；未发布 W-M036-3 |
| 1120–1147 | grantSummaryReportsFrequentTemporaryRequestsByPositionAndUser | 改政策 JSON 阈值 | 能 |
| 1151–1186 | applyPermissionTemplateWithCrossOrgTargetUserThrowsForbidden | CROSS_ORG | 能 |
| 1190–1218 | revokeTemporaryGrantOfOtherOrgUserThrowsForbidden | CROSS_ORG | 能 |
| 1222–1253 | updatePermissionTemplateOfOtherOrgThrowsForbidden | CROSS_ORG | 能 |
| 1257–1273 | createTemporaryGrantRejectsGrantingPermissionActorDoesNotHold | GRANT_EXCEEDS_OWN_PERMISSIONS | 能 |
| 1275–1294 | applyPlatformOwnedTemplateByOrgUserToSameOrgUserIsForbidden | CROSS_ORG | 能 |
| 1298–1308 | listAccessUsersFiltersToCurrentOrganization | 本组织过滤 | 能 |
| 1310–1319 | listAccessUsersPlatformAdminSeesAllOrganizations | 平台管理员全量 | 能 |
| 1321–1331 | listAccessUsersFiltersToCurrentMerchantWhenMerchantScoped | 商户过滤 | 能 |
| 1373–1396 | userPermissionOverrideDenyLandsRemovesAndHighRiskGrantNeedsApproval | DENY 覆盖 GRANT | 能 |
| 1399–1428 | moneyPermissionApplicationsAreMarkedForTheShorterDeadline | 涉钱 actionType | 能 |
| 1432–1455 | dailyTemporaryPermissionApplicationEntersSupervisorChain | 日常临时走链 | 发布口径能；未发布 W-M036-3 |
| 1480–1498 | listGrantRequestsFiltersRowsStampedWithOtherProjects | 项目谓词 | 能 |
| 1507–1526 | restoringAnArchivedPositionMappingMakesItAssignableAgain | 存档不可定岗、恢复可定岗 | 能 |
| 1529–1547 | restoringIsRejectedWhileAnActiveReplacementMappingExists | 409 POSITION_TEMPLATE_ACTIVE_EXISTS | 能 |
| 1569–1632 | savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants | **现经 effectivePermissionsForUser 断言定岗/改映射/停用后的有效集 + 模板应用状态 + 薪资不变** | **能**（本席 mut3：应用 id 空列表 → L1616 红） |
| 1635–1647 | positionTemplateLifecycleAndGrantPolicyFailuresAreAudited | 失败审计事件 | 能 |

夹具要点（001 修复面）：

- `replacePositionGrants` L1703–1709：先 `revokeTemplateApplications(userId, positionApplicationIds…)`，再记新角色与新应用 id。与 origin `JdbcAccessRepository` L842–853 同语义。
- `applyTemplate` L1939–1966：id 为 `"app_" + userId + "_" + (size+1)`，同一次多用户因 userId 不同而不撞；顺序再应用因 size 增长而不撞。
- `findPermissionEffects` L2040–2062：把 `positionRoles` 指向的 **active** 角色权限码以 `Source.ROLE` 并入。与 origin JDBC 岗位 JOIN L873–881 同语义。
- `markSingleUsePermissionConsumed` 仍恒 true（O-6）；本类无单次消耗路径依赖它。

名称/注释与断言：1569 条注释写「按新映射重算有效权限、停用则收回派生」，现断言打到 `grants()` 与应用 `revoked`/`active`，名实相符。

### 3.2 `AuthorizationServiceTests.java`

被测：`new AuthorizationService(InMemoryAccessRepository)`。仓是效果列表桩，专门打判定层（DENY/过期/未生效/单次/TOCTOU/外包/商户范围/平台管理员桥）。

#### 发布变体 V0.02（584 行 / 22 `@Test`）— 发布口径实跑 22/22 绿

| 测试 | 行为 | 改坏会不会红 |
|---|---|---|
| requirePermissionReturnsForbiddenPermissionDeniedWhenUserLacksPermission | 缺权 403 | 能 |
| denyWinsWhenGrantAndDenyExistForSamePermission | 同码 GRANT+DENY → DENY | 能 |
| expiredTemporaryTemplateGrantDoesNotAuthorize | 过期模板授权 | 能 |
| singleUseTemplateGrantIsConsumedAfterFirstSuccessfulAuthorization | 第一次成功，第二次 DENY | 能 |
| localSeedBridgeAuthorizesPlatformAdminButNotArbitraryStaffWithSmokeRole | 平台管理员桥 vs 普通员工 | 能 |
| singleUsePermissionConcurrentConsumerIsDeniedWhenAtomicUpdateReturnsZeroRows | 0 行原子更新 | 能 |
| isPlatformAdminReturnsTrue/False | 身份谓词 | 能 |
| outsourcedVendorIsNotPlatformScopeAndRequiresProjectScope | 外包非平台范围 | 能 |
| outsourcedFacilityTemporaryGrantAllowsAssignedWorkAndEquipmentReadButNotAccept | 最小外包权限集 | 能 |
| outsourcedTemporaryGrantExpiresAndNoLongerAuthorizes | 过期 | 能 |
| outsourcedSecurityCleaningTemporaryGrantOnlyGrantsAssignedInspectionAndExceptionPermissions | 保洁/巡检外包 | 能 |
| currentMerchantScopeReturnsEmptyForPlatformAdmin | 平台管理员 empty（origin SUT 先 `isPlatformAdminBridge`） | 能（对 origin SUT） |
| currentMerchantIdQuietlyReturnsEmptyForPlatformAdminEvenWhenSeedBoundMerchantId | 同上 quietly | 能 |
| currentMerchantIdQuietlyReturnsMerchantIdForRealMerchantUser | 员工仍按商户号 | 能 |
| currentMerchantScopeReturnsMerchantIdForMerchantUser | 是 | 能 |
| currentMerchantScopeThrowsForbiddenWhenMerchantIdIsAbsent | MERCHANT_SCOPE_REQUIRED | 能 |
| effectivePermissionsRemovesDenyCoveredCodesFromGrantsAndListsDenies | DENY 从 grants 扣除 | 能 |
| effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard | 只断言 `wildcard()==false`（返回值第 4 参写死 false） | 改 grants 为全量字典本测试仍绿（O-4，B2 已只登记） |
| effectivePermissionsQueryDoesNotConsumeSingleUseGrant | 两次只读不消费 | 能 |
| effectivePermissionsExcludesExpiredGrant | 过期不进有效集 | 能 |
| **grantWhoseValidityHasNotStartedDoesNotAuthorize** | validFrom=2099-01-01、validUntil=2099-01-02 的 TEMPORARY_GRANT；`requirePermission` 拒且 `grants()` 不含该码。未复用 `temporaryGrant` 助手（助手 validFrom 固定 2020） | **能**（本席 mut1：去掉 validFrom 闸 → L351 不抛） |

origin `isActive`（worktree L529–534）：`consumed` → `validFrom != null && checkedAt.isBefore(validFrom)` → `validUntil`。新测试锁的是第二条；过期测试锁第三条。两条闸不是同一断言。

#### 主工作区 V0.04（615 行 / 25 `@Test`）— 未发布，25/25 绿

相对发布变体：

- 删除「平台管理员 merchant empty」两条，改为 `currentMerchantScopeReturnsBoundMerchantForPlatformAdmin` / `currentMerchantIdQuietlyReturnsBoundMerchantForPlatformAdmin`，断言 `contains("merchant")`。对齐未发布 SUT（有商户号就返回，不因平台管理员 empty）。
- 新增 `productionMasterIdentityGetsExistingAuthorityWithoutBypassingPermissionDenials`、`platformAdminAndIncompleteMasterIdentityDoNotBecomeHighestAuthority`、`currentSupervisorLevelIsThreeOnlyForMasterAuthority`，依赖 `SessionIssuer.MASTER_ACCOUNT_TYPE`。origin/main 无该常量；origin＋V0.04 无法编译。
- **新补丁 `grantWhoseValidityHasNotStartedDoesNotAuthorize` 与发布变体逐字同一方法体**（仅文件版本号 V0.04 vs V0.02）。本席主工作区 25/25 绿，故 002 在两版都成立。

只在未发布版成立的商户/MASTER 项 → W-M036-1，不入队。

### 3.3 `M006JdbcRegressionTests.java` V0.14（342 行 / 13 `@Test`）

真 JDBC：单连接 TEMP 表 + `JdbcAccessRepository` + Spring 事务拦截。`assumeTrue` URL 匹配 `jdbc:postgresql:.*?/complex_test`。本轮未跳过。`@AfterEach` 关连接，TEMP 随会话消失。测试间无残留。

| 行 | 测试 | 与真表/真 SQL | 改坏会不会红 |
|---|---|---|---|
| 69–77 | temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce | TEMP 无 `grant_group_id`。origin SQL `id::text` 足够；未发布 `grant_group_id` 不够（W-M036-2） | origin 能 |
| 79–86 | auditFailureRollsBackConsumptionThroughActualSpringTransaction | CHECK 失败回滚 consumed | 能 |
| 88–97 | inheritedTemplateSingleUseCanBeConsumed | 应用 single_use=false、模板 true | 能 |
| 100–113 | grantReviewReadLocksTheRequestRow | LIKE public + RowShareLock | 能 |
| 117–134 | grantFactsRemainStableUntilTheGrantTransactionCommits | TEMP 影子表 SHARE 锁 | 能 |
| 138–155 | migrationAppliesAndGrantPolicyRoundTripsInPostgres | classpath 读 V386 | 能 |
| 158–166 | approvalFingerprintIsWriteOnceAndMissingIsExplicit | 写一次 | 能 |
| 170–186 | positionReplacementRevokesOnlyItsOwnPreviousApplications | 换岗只撤旧岗位应用 | 能 |
| 190–206 | createCollisionCannotOverwriteExistingUserOrRole | LIKE INCLUDING ALL 唯一约束 | 能 |
| 210–223 | grantPaginationKeepsScopeAndCanReachTheSecondPage | 范围 + 第二页 | 能 |
| 227–249 | positionTemplateSoftDeleteCanBeRestoredWithoutLosingItsDefinition | 生命周期列 | 能 |
| **253–272** | **positionDerivedRoleFeedsPermissionEffectsUntilThePositionGrantsAreCleared** | TEMP `LIKE public` 建 roles/role_permissions/user_position_assignments 等；INSERT 岗位角色码 `position.only.code`；`findPermissionEffects` 必须以 ROLE/GRANT 返回；`replacePositionGrants(user, null, null, [], …)` 之后空。只碰连接内 TEMP（`pg_temp` 优先）；SELECT 证实 public 对应行仍为 0 | **能**（本席 mut2：岗位 JOIN 恒空 → L268 红） |
| 281–317 | legacyPositionMigrationPreservesIndependentGrantsAndCountsOccupiedDefinitions | 回放 V386 后换岗 | 能 |

新测试名/注释等于断言场景。`openTemporaryTables` 手写 TEMP 仍无 `grant_group_id`/`subject_type`；新测试对 `permission_templates` / `permission_template_applications` / `temporary_permission_grants` 做 `ALTER TABLE pg_temp.… ADD COLUMN` 补 `status`/`target_type`/`subject_type`，使 origin `findPermissionEffects` 能跑，仍不写 public。

---

## 4. 第一性原理三问

### 4.1 AccessServiceTests.java

1. **为何必须存在？** 删掉则 BSM-028「管理员工、岗位、班次、职级、权限」、BSM-003 存档恢复/权限分岗、PGM-066「员工权限按单一功能最小颗粒度…角色和员工级完全自定义」、红线④ 商户/项目隔离在服务层没有可失败断言。有业务上位。
2. **声称行为是否执行？** 发布口径 54/54 绿。001 修复后，`savingAPositionMapping…` 经 `effectivePermissionsForUser` 断言定岗含旧角色码+模板码、改映射后含新角色码不含旧码、停用后三者都不含且应用为 revoked。本席 mut3 证明「定岗不把应用 id 交给 replacePositionGrants」会红。
3. **未验证前提？** 内存仓 `findPermissionEffects` 含岗位 ROLE、`replacePositionGrants` 先撤旧岗位应用——现与 origin JDBC 对齐。剩余分叉（模板 `status=active` 过滤、`expireLapsedEffects`、`subject_type`、无岗位行时 JDBC 抛错而内存 put）**当前测试路径第一次正常跑不会撞上**，不为这些理论缝补需求。

### 4.2 AuthorizationServiceTests.java

1. **为何必须存在？** 最终权限计算：DENY 覆盖 GRANT、过期、**未到生效时间**、单次消耗、TOCTOU、外包最小集、商户范围、平台管理员桥。对应 BSM-003/028、PGM-048/066、治理地图临时授权有效期、GEN-005/007。
2. **是否执行？** 发布变体 22/22 绿；未发布 V0.04 25/25 绿。效果是注入的，判定在 `AuthorizationService.isActive` / `permissionGrant`（非假绿替换整段算法）。002 新测试 validFrom 在 2099，未复用 2020 起算的 `temporaryGrant` 助手。
3. **未验证前提？** 发布口径：`currentMerchantScope`/`Quietly` 对平台管理员返回 empty（origin SUT）。未发布 SUT 改为有商户号就过滤——只在 V0.04 上成立（W-M036-1）。桩仓不实现 JDBC 组装，不代替 M006JdbcRegressionTests。

### 4.3 M006JdbcRegressionTests.java

1. **为何必须存在？** 内存仓把锁/单次消耗/换岗撤销/岗位 JOIN 交给本类。删掉则 JDBC 语义无真库断言。
2. **是否执行？** 发布口径 13/13 绿，Skipped=0。新测试打到 origin `findPermissionEffects` 岗位 JOIN 与 `replacePositionGrants` 置空。本席 mut2 证明删 JOIN 会红。
3. **未验证前提？** TEMP 列集 = 所执行 SQL 用到的列，不是全列克隆。未发布 `grant_group_id` 仍会使本类（含新测试）在未发布 SUT 上坏 SQL（W-M036-2）。`assumeTrue` 缺环境会整类跳过——本轮有三变量，未空转（O-5）。

---

## 5. 顺从式：52 项映射逐项

锁定义已读全文。映射原文：

`M036 → [BEQR-157, BEQR-182, BEQR-183, BEQR-184, BEQR-185, BEQR-186, BEQR-187, BSM-001, BSM-003, BSM-009, BSM-028, BSM-029, BSM-073, BSM-084, BSM-085, BSM-086, BSM-087, BSM-088, BSM-114, BSM-115, BSM-120, GEN-001 … GEN-018, PGM-001, PGM-008, PGM-022, PGM-024, PGM-026, PGM-032, PGM-033, PGM-034, PGM-035, PGM-036, PGM-048, PGM-059, PGM-066]`

共 52，无缺号。不新增判据。

| ID | 适用? | 理由 |
|---|---|---|
| GEN-001 | 适用 | 三文件承载权限/职级/模板/有效集测试，非空转发 |
| GEN-002 | 适用 | 发布口径 89/89 可执行；未发布对照另计 |
| GEN-003 | 适用 | 001/002 新断言本席变异可红；旧测试除已登记观察外能红 |
| GEN-004 | 适用 | 组织/商户/账号类型/职级 id/币种校验 |
| GEN-005 | 适用 | DENY>GRANT、跨组织、授出不超过自身、高危审批、外包最小集、未到生效时间不放行 |
| GEN-006 | 适用 | JDBC 消费回滚；指纹写一次；创建碰撞；TEMP 不写 public（SELECT 0 行） |
| GEN-007 | 适用 | 单次消耗 TOCTOU；幂等键；行锁 |
| GEN-008 | 适用 | 内存仓岗位授权语义现与 origin JDBC 对齐；DTO/政策 JSON 含 deadlineWorkingDays |
| GEN-009 | 适用 | 中文校验文案；失败审计；不吞异常 |
| GEN-010 | 适用 | 禁止创建 platform_admin；跨商户 403；TEMP 不落 public |
| GEN-011 | 适用 | 模板/授权/职级/失败事件名 |
| GEN-012 | 无本面 | 无无界批处理/连接泄漏面；分页有页大小但不构成本模块性能边界审点 |
| GEN-013 | 适用 | 三变量 + assumeTrue；本轮未跳过 |
| GEN-014 | 适用 | TEMP 回放 V386；worktree 临时 V427 仅 Flyway 校验，跑完删除 |
| GEN-015 | 无本面 | 无界面 |
| GEN-016 | 无本面 | 本模块非文档 |
| GEN-017 | 适用 | 见第 7 节四分类；001 细化、002 技术性，已修不改类 |
| GEN-018 | 适用 | HTML 原文 / Maven 输出 / psql / git ls-tree / 变异命令 |
| BEQR-157 | 无本面 | 员工状态五值未出现；测试用账号 `active`，不是待入职/在职/试岗/停岗/离职 |
| BEQR-182 | 无本面 | 无考勤日结果枚举或断言 |
| BEQR-183 | 无本面 | 无打卡方式 |
| BEQR-184 | 无本面 | 无请假类型 |
| BEQR-185 | 无本面 | 无请假单状态 |
| BEQR-186 | 无本面 | 只测职级赋值与 CNY，无薪资单构成/绩效档 |
| BEQR-187 | 无本面 | 无提成发放路径 |
| BSM-001 | 适用（弱） | 组织/商户/项目归属出现；不管票务酒吧主流程 |
| BSM-003 | 适用 | 岗位模板存档/恢复；权限分岗；日常 vs 高危；平台管理员桥 |
| BSM-009 | 适用 | 基础管理：员工与权限 |
| BSM-028 | 适用 | 「管理员工、岗位、职级、权限」；红线④ 商户隔离 |
| BSM-029 | 适用（弱） | 权限卡住后续（缺权 403） |
| BSM-073 | 适用 | 商户范围内员工/权限，不看他商户 |
| BSM-084 | 无本面 | 员工手机端排班考勤请假薪资 |
| BSM-085 | 无本面 | 日常工作注册/工单/代订 |
| BSM-086 | 无本面 | 排班考勤请假 |
| BSM-087 | 无本面 | 我的薪资 |
| BSM-088 | 无本面 | 提成/人员服务边界 |
| BSM-114 | 适用 | 不同人只看自己范围（组织/商户过滤、平台管理员全量） |
| BSM-115 | 适用 | 基础管理连权限；不允许则卡住 |
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
| PGM-048 | 适用 | 商户侧授权范围内开放、不能跨商户 |
| PGM-059 | 适用（弱） | 手动开户 `phoneHash()` 为 null |
| PGM-066 | 适用 | 「员工权限按单一功能最小颗粒度…角色和员工级完全自定义」；临时授权有效期（002）；界面全中文在 remainingValidationMessagesAreChinese 触及后台报错 |

映射含 PGM-008/032–036 等员工手机章节，是聚合并集，不是本三文件实现面。不把未映射的 PGM 权限入口卡当成锁外新判据。

---

## 6. 对抗式（grilling 自问自答）

❓ **Q1** 内存仓是否仍制造「测 AccessService、绿的是假仓」？

➡️ 岗位 ROLE 并入与换岗撤应用这两条，001 已按 JDBC 语义改夹具，且 `savingAPositionMapping…` 经 `effectivePermissionsForUser` 读有效集。本席 mut3 打的是 **AccessService 调用 replacePositionGrants 的参数**，不是只打夹具内部。AuthorizationServiceTests 的仓仍是效果桩，故意只测判定层，可接受。

❓ **Q2** 新三断言是否名实不符、或复用了会让闸永远为真的助手？

➡️ 002 未调用 `temporaryGrant(…, "2099-…")`（该助手 validFrom=2020-01-01，只测截止）。新测试 validFrom=2099-01-01、validUntil=2099-01-02。只把 validUntil 放到未来而忽略 validFrom 时，过期闸不会拒绝——mut1 正是如此，测试红了。001 注释与 `grants()` / 应用状态断言一致。

❓ **Q3** 临时表测试有没有写 public？

➡️ CREATE TEMP + 无 schema 限定名，`pg_temp` 优先。跑完 `SELECT` public 四表目标键均为 0。FK 即使 LIKE 复制，也只是读父表做存在性检查，本轮无 public 行残留。

❓ **Q4** 测试之间有无顺序/残留依赖？

➡️ AccessServiceTests / AuthorizationServiceTests 每测新仓，AfterEach 清 SecurityContext。JDBC 每测新连接，TEMP 随连接结束。`grantSummary` 改政策 JSON 后写回，但下一测 anyway 新仓。无顺序依赖。

❓ **Q5** 把「全部否」当成模块已全对，会不会漏回归？

➡️ 本席未把 001/002 队列否当作收口。整模块 54+22+13 重跑，并对三处修复做独立变异。O-1…O-6 无新证据，不改判。W-M036-1…3 源码仍在，不入队。

❓ **Q6** 还有没有第一次正常使用就会撞上、且锁外/未覆盖的闸？

➡️ 未发现。内存仓仍不实现 `expireLapsedEffects` / 模板 status 过滤，当前测试第一次正常跑不依赖这两条。不为理论缝加需求。

❓ **Q7**（ponytail-review）三处新代码有没有可删的空转？

➡️ Lean already. 新测试各锁一条生产闸；内存仓三处改动是 001 最小对齐，删掉会回到假绿。

---

## 7. Findings

### 已定稿 ID 本轮复核

| ID | 本轮 | 证据 | 四分类 |
|---|---|---|---|
| M036-001 | **已修成立**（发布口径 + 主工作区测试文件）。队列保持「否」 | 内存仓 L1703–1709 / L2040–2062；测试 L1569–1632；JDBC L253–272。mut2、mut3 红，还原绿。类型 2+4 | 细化（B2 已定，不改类） |
| M036-002 | **已修成立**（发布变体 **和** 未发布 V0.04）。队列保持「否」 | 两版同一方法体；发布 22/22、未发布 25/25；mut1 红。类型 2+4 | 技术性（无业务发明：只锁已有 `isActive` 分支） |

不把「全部否」写成模块已全对的前提；以上是本轮重审证据。

### 新工程缺陷

无。无 GR 临时编号入队。无添加项。无 HTML 修改申请。无 questions。

### W 项（仅未发布版，不入修复队列，D-029）

B2 已登记，本轮源码仍在，**无新证据升队**：

| 号 | 哪一版成立 | 现况 |
|---|---|---|
| W-M036-1 | 仅主工作区 V0.04 + 未发布 AuthorizationService | 两条改名测试断言平台管理员 `contains("merchant")`；三条 MASTER 测试要 `SessionIssuer.MASTER_ACCOUNT_TYPE`。origin 无该常量。发布变体 22 条与 origin SUT 一致（empty）。 |
| W-M036-2 | 仅未发布 JdbcAccessRepository | TEMP 表无 `grant_group_id`。未发布 `findPermissionEffects` L889 与 `markSingleUsePermissionConsumed` L1041 用该列。origin 用 `id::text`。新真库测试在未发布 SUT 上会同一原因坏 SQL，仍归本号，不新开。 |
| W-M036-3 | 仅未发布 SupervisorReviewService | AccessService 发起授权申请不带直接受益人；AccessServiceTests 走 `reviewGrant` 的 6 条在未发布混跑 ERROR。发布口径 54/54 绿。 |

### 观察（不立项，无新证据不改判）

- **O-1** `createAccessUserWithAllowedAccountTypeSucceeds` 循环仅 `outsourced_vendor`。
- **O-2** `listStaffLevelsReturnsSeededLevels` 只含 2 档；psql 证实库内 30 档。30 档属已闭环迁移。
- **O-3** `remainingValidationMessagesAreChinese` 反射私有 `normalizeGrantRequestType`。
- **O-4** `effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard` 只读写死的 `wildcard()` 标志。
- **O-5** `assumeTrue` 缺环境会 skip JDBC 类；本轮未发生。
- **O-6** AccessServiceTests `markSingleUsePermissionConsumed` 恒 true；本类无依赖，单次消耗由另外两类覆盖。

XM-M036-A（origin 全量后端 20 红、与本模块无关）不在本三文件，本轮不重开。

---

## 8. 三线收口

- 第一性原理：三文件三问均落到 HTML / Maven / psql / 变异命令。
- 顺从式：52 项均给适用或无本面及理由；GEN-003/008 因 001/002 已修，本面满足。
- 对抗式：grilling 七问；三变异独立红；TEMP 不写 public；无新缺陷。

禁止项核对：未修业务/HTML/锁/findings/ledger；未 fetch/commit/push/merge/deploy；未建持久脚本日志（worktree 内 `.m036-env` 已随拆除删除）；psql 只 SELECT；未读本次 self-raw 正文。

---

## 9. 本席 B5-r1 结论

**PASS**

理由：整模块三文件（AuthorizationServiceTests 两版）全覆盖；52 项逐项记录；三线完成；发布口径 54+22+13 全绿；001/002 对应三处新断言被本席独立变异打红后还原全绿；无未答 questions；无未定稿新缺陷。W-M036-1…3 仍只在未发布版成立，按 D-029 登记不入队。O-1…O-6 无新证据，维持只登记。

本票不是合取结论。合取须待席甲 self raw 已封存后由调度方比对；本席落盘前未读对方正文。
