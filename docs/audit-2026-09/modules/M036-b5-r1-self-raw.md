CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M036 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；本轮测试改动为席甲 B3 所写，且席甲 B1 把 GR-01 只记成观察并误称已有测试守住，以 Grok 票为准）。2026-09-21T11:11:32Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main `3ca1b507`。
- 模块 M036 3 个测试文件（§1 全读；AuthorizationServiceTests 两版），52 判据；B3 累计 1。
- 顺序：本席取证脚本先跑完，随后落本报告；落盘前未读 Grok 正文（modules/M036-b5-r1-grok-raw.md 存在=False）。只读承诺：未改任何已跟踪文件；一次性 worktree 已删除。
- 本轮补的方法（B1 把 GR-01 判成「别处已守住」）：凡要写「某语义已由另一个测试守住」，先用变异在全量测试上验一次，抓得到才写。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| AccessServiceTests.java（主工作区版） | 2160 | 129164 | be3e9be417a860350f57d091e401bf5c9cef772b308849335454035911600766 | 全读 |
| AuthorizationServiceTests.java（主工作区版） | 615 | 26759 | 6d409fb7524492bc217c79c4afbe172787d1f530465a5b927734dc7b4f2a9c5e | 全读 |
| M006JdbcRegressionTests.java（主工作区版） | 342 | 25804 | b16e34e2db9d05f6a78d8a08342cc0d3c97363705634e7aba0af53aba07e2cf9 | 全读 |
| AuthorizationServiceTests.java（发布变体 V0.02＝origin/main 版＋补丁） | 584 | 24879 | 3b3b6c2b2b6c042d92bb0c8dd2721d80def1ace85417197fd907f5d570f6671a | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m036_b5_run.log）

```
START=2026-09-21T11:11:04Z HEAD=e456ba96 origin/main=3ca1b507
== 0. files (lines/bytes/sha256 | vs origin/main)
AccessServiceTests.java（主工作区版） 2160 129164 be3e9be417a860350f57d091e401bf5c9cef772b308849335454035911600766 | [ 1 file changed, 34 insertions(+), 8 deletions(-)]
AuthorizationServiceTests.java（主工作区版） 615 26759 6d409fb7524492bc217c79c4afbe172787d1f530465a5b927734dc7b4f2a9c5e | [ 1 file changed, 60 insertions(+), 9 deletions(-)]
M006JdbcRegressionTests.java（主工作区版） 342 25804 b16e34e2db9d05f6a78d8a08342cc0d3c97363705634e7aba0af53aba07e2cf9 | [ 1 file changed, 24 insertions(+), 1 deletion(-)]
AuthorizationServiceTests.java（发布变体） 584 24879 3b3b6c2b2b6c042d92bb0c8dd2721d80def1ace85417197fd907f5d570f6671a
== 1. 发布口径：origin/main 一次性 worktree＋三个修后文件
scratch at 3ca1b507
scratch 改动文件： M backend/src/test/java/com/complex/platform/access/AccessServiceTests.java  M backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java  M backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java 
== 1A. 测试形状（发布口径三文件）
== AccessServiceTests.java: 54 tests, 2160 lines
   copyPermissionTemplateKeepsSnapshotWhenSourceChangesAndWritesAuditEvent                              L83    lines= 50 asserts= 2 
   applyPermissionTemplateRequiresApprovalWhenTemplateHasHighRiskPermission                             L134   lines= 35 asserts= 4 
   applyPermissionTemplateAppliesToManyUsersAndWritesAuditEvent                                         L170   lines= 38 asserts= 6 
   temporaryGrantCreateAndRevokeWriteAuditEvents                                                        L209   lines= 27 asserts= 2 
   createAccessUserWithPlatformAdminAccountTypeThrowsValidationError                                    L239   lines= 18 asserts= 3 
   createAccessUserWithAllowedAccountTypeSucceeds                                                       L258   lines= 18 asserts= 1 
   platformAdminWithMerchantAffiliationCanCreateUserForAnyMerchant                                      L277   lines= 25 asserts= 1 
   createAccessUserRequiresOrganizationScope                                                            L303   lines= 18 asserts= 3 
   createAccessUserStoresManualEvidenceAndProvisionsCredential                                          L322   lines= 31 asserts=10 
   merchantScopedCreatorCannotCreateUserForAnotherMerchant                                              L354   lines= 20 asserts= 3 
   assignStaffLevelSetsLevelAndDefaultsCurrencyToCny                                                    L377   lines= 13 asserts= 5 
   assignStaffLevelRejectsUnknownLevel                                                                  L391   lines= 10 asserts= 3 
   assignStaffLevelRejectsNonCnyCurrency                                                                L402   lines= 10 asserts= 3 
   assignStaffLevelRejectsCrossOrganization                                                             L413   lines= 13 asserts= 3 
   effectivePermissionsForUserRejectsCrossOrganization                                                  L427   lines= 12 asserts= 3 
   changeUserScopeRejectsTargetOutsideActorProject                                                      L440   lines= 14 asserts= 3 
   listStaffLevelsReturnsSeededLevels                                                                   L455   lines=  6 asserts= 1 
   assignAndUnassignUserRoleUpdateUserRoles                                                             L462   lines= 24 asserts= 4 
   assignUserPositionAppliesMappedRoleAndTemplate                                                       L487   lines= 45 asserts= 8 
   assignUserRoleRejectsRolePermissionsActorDoesNotHold                                                 L533   lines= 19 asserts= 3 
   assignUserRoleRequiresApprovalWhenHighRiskGrantFlagIsEnabled                                         L553   lines= 22 asserts= 4 
   positionMappingAndAssignmentDoNotRequireTheConfigurerToHoldTheHeldHighRiskCodes                      L579   lines= 32 asserts= 4 
   assignUserPositionLandsNormalPartAndHoldsHighRiskPartUntilChainApproves                              L612   lines= 50 asserts= 8 
   scopeChangeRevokesOldDerivedGrantsImmediatelyAndNewScopeWaitsForApproval                             L665   lines= 37 asserts=11 
   scopeChangeApprovalRejectsWhenTheNewScopePositionMappingChangedWhilePending                          L705   lines= 33 asserts= 5 
   scopeChangeAcrossProjectsLandsWhenTheApproverStaysInTheOldProject                                    L741   lines= 32 asserts= 6 
   scopeChangeCancelsOldScopeHighRiskHoldsAndTheirDenyOverrides                                         L776   lines= 34 asserts= 7 
   scopeChangeFreezesExtraGrantsBeyondTheNewTemplate                                                    L813   lines= 42 asserts= 7 
   removingTheHighRiskHoldDenyRequiresApproval                                                          L858   lines= 32 asserts= 6 
   preExistingExplicitDenyIsNotReusedByTheHighRiskHold                                                  L893   lines= 32 asserts= 5 
   remainingValidationMessagesAreChinese                                                                L927   lines= 29 asserts= 8 
   scopeChangeRejectsUnchangedScope                                                                     L957   lines=  9 asserts= 2 
   grantPolicyRejectsAnyUnclassifiedPermissionInTheRequestedRole                                        L969   lines= 13 asserts= 2 
   grantRequestRejectsPermissionsThatDoNotRequireApproval                                               L983   lines= 23 asserts= 4 
   approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment                                   L1007  lines= 33 asserts= 4 
   approvedTemporaryGrantExpiresByEffectivePermissionRead                                               L1041  lines= 32 asserts= 3 
   auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse                                 L1074  lines= 45 asserts= 4 
   grantSummaryReportsFrequentTemporaryRequestsByPositionAndUser                                        L1120  lines= 28 asserts= 4 
   applyPermissionTemplateWithCrossOrgTargetUserThrowsForbidden                                         L1151  lines= 36 asserts= 3 
   revokeTemporaryGrantOfOtherOrgUserThrowsForbidden                                                    L1190  lines= 29 asserts= 3 
   updatePermissionTemplateOfOtherOrgThrowsForbidden                                                    L1222  lines= 32 asserts= 3 
   createTemporaryGrantRejectsGrantingPermissionActorDoesNotHold                                        L1257  lines= 17 asserts= 3 
   applyPlatformOwnedTemplateByOrgUserToSameOrgUserIsForbidden                                          L1275  lines= 20 asserts= 3 
   listAccessUsersFiltersToCurrentOrganization                                                          L1298  lines= 11 asserts= 2 
   listAccessUsersPlatformAdminSeesAllOrganizations                                                     L1310  lines= 10 asserts= 1 
   listAccessUsersFiltersToCurrentMerchantWhenMerchantScoped                                            L1321  lines= 11 asserts= 2 
   userPermissionOverrideDenyLandsRemovesAndHighRiskGrantNeedsApproval                                  L1373  lines= 24 asserts= 7 
   moneyPermissionApplicationsAreMarkedForTheShorterDeadline                                            L1399  lines= 30 asserts= 4 
   dailyTemporaryPermissionApplicationEntersSupervisorChain                                             L1432  lines= 24 asserts= 4 
   listGrantRequestsFiltersRowsStampedWithOtherProjects                                                 L1480  lines= 19 asserts= 2 
   restoringAnArchivedPositionMappingMakesItAssignableAgain                                             L1507  lines= 20 asserts= 6 
   restoringIsRejectedWhileAnActiveReplacementMappingExists                                             L1529  lines= 19 asserts= 5 
   savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants                     L1569  lines= 64 asserts=10 
   positionTemplateLifecycleAndGrantPolicyFailuresAreAudited                                            L1635  lines= 13 asserts= 5 
   nested/inner types: AccessServiceTests, RecordingCredentialProvisioner, CredentialProvisionRequest, InMemoryAccessRepository, InMemorySupervisorReviewRepository
   annotations: 
== AuthorizationServiceTests.java: 22 tests, 584 lines
   requirePermissionReturnsForbiddenPermissionDeniedWhenUserLacksPermission                             L52    lines= 10 asserts= 3 
   denyWinsWhenGrantAndDenyExistForSamePermission                                                       L63    lines= 12 asserts= 3 
   expiredTemporaryTemplateGrantDoesNotAuthorize                                                        L76    lines= 19 asserts= 2 
   singleUseTemplateGrantIsConsumedAfterFirstSuccessfulAuthorization                                    L96    lines= 22 asserts= 3 
   localSeedBridgeAuthorizesPlatformAdminButNotArbitraryStaffWithSmokeRole                              L119   lines= 10 asserts= 2 
   singleUsePermissionConcurrentConsumerIsDeniedWhenAtomicUpdateReturnsZeroRows                         L130   lines= 23 asserts= 2 
   isPlatformAdminReturnsTrueForPlatformAdminActor                                                      L154   lines=  5 asserts= 1 
   isPlatformAdminReturnsFalseForStaffActor                                                             L160   lines=  5 asserts= 1 
   outsourcedVendorIsNotPlatformScopeAndRequiresProjectScope                                            L166   lines= 13 asserts= 6 
   outsourcedFacilityTemporaryGrantAllowsAssignedWorkAndEquipmentReadButNotAccept                       L180   lines= 17 asserts= 0 
   outsourcedTemporaryGrantExpiresAndNoLongerAuthorizes                                                 L198   lines=  7 asserts= 0 
   outsourcedSecurityCleaningTemporaryGrantOnlyGrantsAssignedInspectionAndExceptionPermissions          L206   lines= 18 asserts= 0 
   currentMerchantScopeReturnsEmptyForPlatformAdmin                                                     L225   lines=  5 asserts= 1 
   currentMerchantIdQuietlyReturnsEmptyForPlatformAdminEvenWhenSeedBoundMerchantId                      L231   lines=  9 asserts= 1 
   currentMerchantIdQuietlyReturnsMerchantIdForRealMerchantUser                                         L241   lines=  6 asserts= 1 
   currentMerchantScopeReturnsMerchantIdForMerchantUser                                                 L248   lines=  6 asserts= 1 
   currentMerchantScopeThrowsForbiddenWhenMerchantIdIsAbsent                                            L255   lines= 16 asserts= 3 
   effectivePermissionsRemovesDenyCoveredCodesFromGrantsAndListsDenies                                  L272   lines= 15 asserts= 5 
   effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard                                      L288   lines=  5 asserts= 1 
   effectivePermissionsQueryDoesNotConsumeSingleUseGrant                                                L294   lines= 22 asserts= 3 
   effectivePermissionsExcludesExpiredGrant                                                             L317   lines= 17 asserts= 1 
   grantWhoseValidityHasNotStartedDoesNotAuthorize                                                      L336   lines= 18 asserts= 1 
   nested/inner types: AuthorizationServiceTests, InMemoryAccessRepository
   annotations: 
== M006JdbcRegressionTests.java: 13 tests, 342 lines
   temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce                                                 L69    lines=  9 asserts= 5 
   auditFailureRollsBackConsumptionThroughActualSpringTransaction                                       L79    lines=  8 asserts= 2 
   inheritedTemplateSingleUseCanBeConsumed                                                              L88    lines= 10 asserts= 2 
   grantReviewReadLocksTheRequestRow                                                                    L100   lines= 14 asserts= 2 
   grantFactsRemainStableUntilTheGrantTransactionCommits                                                L117   lines= 18 asserts= 2 
   migrationAppliesAndGrantPolicyRoundTripsInPostgres                                                   L138   lines= 18 asserts= 6 
   approvalFingerprintIsWriteOnceAndMissingIsExplicit                                                   L158   lines=  9 asserts= 4 
   positionReplacementRevokesOnlyItsOwnPreviousApplications                                             L170   lines= 17 asserts= 5 
   createCollisionCannotOverwriteExistingUserOrRole                                                     L190   lines= 17 asserts= 4 
   grantPaginationKeepsScopeAndCanReachTheSecondPage                                                    L210   lines= 14 asserts= 4 
   positionTemplateSoftDeleteCanBeRestoredWithoutLosingItsDefinition                                    L227   lines= 23 asserts= 7 
   positionDerivedRoleFeedsPermissionEffectsUntilThePositionGrantsAreCleared                            L253   lines= 20 asserts= 2 
   legacyPositionMigrationPreservesIndependentGrantsAndCountsOccupiedDefinitions                        L281   lines= 37 asserts= 8 
   nested/inner types: -
   annotations: 
temp migrations (Flyway validate 用，跑完即删): V427__m009_project_profiles_and_associations.sql
MVN_EXIT(publish)=0
   AccessServiceTests tests=54 errors=0 skipped=0 failures=0
   AuthorizationServiceTests tests=22 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=13 errors=0 skipped=0 failures=0
scratch dirty after cleanup: 0
scratch removed
== 2. 主工作区现状对照（含未发布改动，非发布口径）
MVN_EXIT(main worktree)=1
   AccessServiceTests tests=54 errors=6 skipped=0 failures=0
       error assignUserPositionLandsNormalPartAndHoldsHighRiskPartUntilChainApproves | 退款、免单、授权与资金事项必须登记直接受益人
       error dailyTemporaryPermissionApplicationEntersSupervisorChain | 退款、免单、授权与资金事项必须登记直接受益人
       error approvedTemporaryGrantExpiresByEffectivePermissionRead | 退款、免单、授权与资金事项必须登记直接受益人
       error auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse | 退款、免单、授权与资金事项必须登记直接受益人
       error approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment | 退款、免单、授权与资金事项必须登记直接受益人
       error removingTheHighRiskHoldDenyRequiresApproval | 退款、免单、授权与资金事项必须登记直接受益人
   AuthorizationServiceTests tests=25 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=13 errors=2 skipped=0 failures=0
       error positionDerivedRoleFeedsPermissionEffectsUntilThePositionGrantsAreCleared | PreparedStatementCallback; bad SQL grammar [SELECT subject_id AS user_id, permission_code, &apos;grant&apos; AS effect, &apos;TEMPORARY_GRAN
       error temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce | PreparedStatementCallback; bad SQL grammar [UPDATE temporary_permission_grants&#10;   SET consumed = TRUE,&#10;       consumed_at = ?&#10; W
主工作区报错原因计数：      2 column "grant_group_id" does not exist      12 必须登记直接受益人 
== 3. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T11:11:18Z
```

## 3. 三线审计

- 第一性原理线：用途/实际行为同 B1。修后：岗位派生角色并入有效权限、映射停用收回派生、生效时间未到不放行三处有测试锁住，内存仓不再与 Jdbc 语义分叉。
- 顺从式线：52 判据逐项见 §4。
- 对抗式线（grilling）：Q 内存仓改动会不会让别的测试由红变绿或掩盖问题？A 54 条在发布口径下全绿，改动只让有效集多出岗位角色的码、让旧岗位应用被撤，凡此前对有效集做「不含」断言的测试（高危挂起、范围变更）仍靠显式拒绝或指针置空成立。Q 新真库测试会不会碰 public 表？A 全部 `CREATE TEMP TABLE … LIKE public.…` 与 `ALTER TABLE pg_temp.…`，连接关闭即销毁。Q 新授权测试会不会因助手的 2099 截止时间而假绿？A 未用助手，validFrom 直接设为 2099-01-01。Q 其余同 B1：无新缺陷、无添加项、无 HTML 问题。

## 4. 52 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BEQR-157 员工状态 | 无本面｜本模块是三个测试类，不承载、不展示「员工状态」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-182 考勤日结果 | 无本面｜本模块是三个测试类，不承载、不展示「考勤日结果」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-183 打卡方式 | 无本面｜本模块是三个测试类，不承载、不展示「打卡方式」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-184 请假类型 | 无本面｜本模块是三个测试类，不承载、不展示「请假类型」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-185 请假单状态 | 无本面｜本模块是三个测试类，不承载、不展示「请假单状态」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-186 薪资单构成 | 无本面｜本模块是三个测试类，不承载、不展示「薪资单构成」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-187 员工提成发放 | 无本面｜本模块是三个测试类，不承载、不展示「员工提成发放」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-001 整体说明：这套系统到底管什么 | 无本面｜本模块是三个测试类，不承载、不展示「整体说明：这套系统到底管什么」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 适用｜「权限分岗」在服务层的守护：显式拒绝压过授予、跨组织/跨商户拒绝、授出不得超过自身权限、高危须审批，AccessServiceTests 与 AuthorizationServiceTests 均有能失败的断言；多权限门控「全部持有才放行、同源只消耗一次」由同聚合第 3 段的 M006SecurityRegressionTests 守住（变异 multi_first_only 被其抓到）｜通过 |
| BSM-009 基础管理 | 无本面｜本模块是三个测试类，不承载、不展示「基础管理」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-028 主要功能 | 无本面｜本模块是三个测试类，不承载、不展示「主要功能」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-029 和谁有关 | 无本面｜本模块是三个测试类，不承载、不展示「和谁有关」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-073 主要功能 | 无本面｜本模块是三个测试类，不承载、不展示「主要功能」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 无本面｜本模块是三个测试类，不承载、不展示「员工手机端：排班、考勤、请假、薪资，自己手机上都能办」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-085 日常工作 | 无本面｜本模块是三个测试类，不承载、不展示「日常工作」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-086 排班、考勤与请假 | 无本面｜本模块是三个测试类，不承载、不展示「排班、考勤与请假」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-087 我的薪资 | 无本面｜本模块是三个测试类，不承载、不展示「我的薪资」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-088 员工提成、活动执行与人员服务边界 | 无本面｜本模块是三个测试类，不承载、不展示「员工提成、活动执行与人员服务边界」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 适用｜「只看自己该看的」：账号列表按组织/商户过滤、授权申请列表按项目过滤、外包只能碰派给自己的工单（变异 no_outsourced_assignee_scope 被 8 个设施/报备测试抓到）｜通过 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 无本面｜本模块是三个测试类，不承载、不展示「相互关系：为什么这些子系统必须连在一起」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-120 已确认规则和实施前输入项 | 无本面｜本模块是三个测试类，不承载、不展示「已确认规则和实施前输入项」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| GEN-001 必要性与最小性 | 适用｜发布口径 87 个测试（54＋21＋12）无重复场景；`createAccessUserWithAllowedAccountTypeSucceeds` 对只含一个元素的列表做循环、循环体内 `Set.of("merchant","merchant_admin","sales").contains(type)` 恒为 false，是缩表后留下的死条件（OBS-3）｜通过（登记 OBS-3） |
| GEN-002 可执行正确性 | 适用｜发布口径（origin/main 一次性 worktree＋三个修后文件，AuthorizationServiceTests 用发布变体）54＋22＋13 全绿（探针）；主工作区现状的报错均属未发布改动（W-M036-2、3）｜通过（发布口径；W 项登记） |
| GEN-003 测试有效性 | 适用｜B1 的 8 个变异加 B3 的 3 个变异：岗位派生角色并入有效权限、停用收回、生效时间未到三处现有测试锁住（001、002，各自变异下只红对应新断言）；三个幸存变异属第二道闸（登记）｜通过 |
| GEN-004 信任边界校验 | 适用｜账号类型白名单、缺组织、未知职级、非人民币、状态非法、缺失效时间等必填/取值校验有测试｜通过 |
| GEN-005 授权与最小权限 | 适用｜见 BSM-003；授权时间窗两头（未到生效、已过期）现都有测试（002）｜通过 |
| GEN-006 数据完整性 | 适用｜M006JdbcRegressionTests 13 条用连接内临时表验证真实 SQL；新增一条守住 `findPermissionEffects` 的岗位 JOIN 与置空后的收回（001）；手写临时表与真表的相容性同 B1（登记）｜通过（登记） |
| GEN-007 并发与幂等 | 适用｜行锁、表锁保持到事务结束、审计失败经真实 Spring 事务回滚，有真库测试；每个测试一条独立连接、临时表随连接销毁，不留残留、不依赖顺序｜通过 |
| GEN-008 接口与模式一致性 | 适用｜AccessServiceTests 内存仓现与 JdbcAccessRepository 同语义：岗位派生角色并入 ROLE 来源授予、`replacePositionGrants` 先撤上一次岗位派生的模板应用再记新指针、应用 id 不重复（001）；`markSingleUsePermissionConsumed` 恒 true 仍为登记项（本类无依赖）｜通过（登记） |
| GEN-009 错误处理与恢复 | 适用｜三个文件无吞异常的 try/catch（探针 1 无 catch 标记）；`grantPolicyRejectsAnyUnclassifiedPermissionInTheRequestedRole` 只断言「抛 ApiException 且未落申请」，变异 unclassified_as_low 下它会红，断言能失败｜通过 |
| GEN-010 安全与敏感信息 | 适用｜测试数据不含真实口令或个人信息；库口令取环境变量｜通过 |
| GEN-011 可观测与审计证据 | 适用｜模板复制/应用/撤销、账号变更、定岗、员工级覆盖、生命周期与授权政策失败留痕均有事件名断言；单次消耗写用量日志有真库断言｜通过 |
| GEN-012 资源与性能边界 | 无本面｜测试无性能面 |
| GEN-013 构建配置与依赖 | 适用｜依赖 JUnit5、AssertJ、Spring JDBC/TX；M006JdbcRegressionTests 在缺 COMPLEX_DATABASE_URL 或库名不是 complex_test 时 `assumeTrue` 整类跳过（OBS-4）｜通过（登记 OBS-4） |
| GEN-014 数据库迁移正确性 | 适用｜M006JdbcRegressionTests 从类路径读 V386 原文在临时表上回放，先去掉 LIKE 复制来的后续列，使测试与库的迁移水位无关；不改已应用迁移｜通过 |
| GEN-015 界面可用性与可访问性 | 适用｜`remainingValidationMessagesAreChinese` 断言 4 条校验文案不含拉丁字母；夹具里的英文角色名/模板名不是界面文字｜通过 |
| GEN-016 文档可验证性 | 适用｜测试名与注释声称的场景与实际断言逐个核对一致；`copyPermissionTemplateKeepsSnapshotWhenSourceChangesAndWritesAuditEvent` 里源模板改的是名称与类型、权限项前后相同，副本「不随源变」的真实语义由 AccessJdbcIntegrationTests 的 `copyTemplateStoresSnapshotIndependentFromLaterSourceChanges` 守住（OBS-5）｜通过（登记 OBS-5） |
| GEN-017 业务来源分类 | 适用｜未见测试自造业务规则；origin/main 版 `currentMerchantScopeReturnsEmptyForPlatformAdmin` 等两条锁的是 origin/main 现行行为，其业务口径（七类红线第四条是否约束平台管理员）已在 M032 登记待操作员裁决，本模块不重复立项｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到测试源码原文、探针输出、变异结果、库结构只读查询与实跑结果｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 适用｜到期失效、单次消耗、并发二次消耗被拒、生效时间未到不放行（002，变异能红）均有测试｜通过 |
| PGM-008 员工和手机 | 无本面｜本模块是三个测试类，不承载、不展示「员工和手机」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | 无本面｜本模块是三个测试类，不承载、不展示「空间和点位：所有业务都要知道“发生在哪”」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-024 影响业务 | 无本面｜本模块是三个测试类，不承载、不展示「影响业务」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-026 主要功能 | 无本面｜本模块是三个测试类，不承载、不展示「主要功能」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | 无本面｜本模块是三个测试类，不承载、不展示「员工和手机：人、私人手机、工作手机要能对应起来」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-033 主要功能 | 适用｜职级设定、未知职级、币种、跨组织、定岗套映射与模板、高危部分挂起待审、范围变更的旧派生立即失效与新范围待审，均有服务层测试｜通过 |
| PGM-034 工作手机和企业微信 | 无本面｜本模块是三个测试类，不承载、不展示「工作手机和企业微信」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-035 提成发放和人员服务治理 | 无本面｜本模块是三个测试类，不承载、不展示「提成发放和人员服务治理」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | 无本面｜本模块是三个测试类，不承载、不展示「设备管理：谁领了什么设备，设备能不能用」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-048 商户侧：在授权业务范围内全开放 | 适用｜外包账号的范围与限时授权（到期后拒绝、只放行被授的派工类权限）有测试｜通过 |
| PGM-059 客人与员工的个人信息保护 | 适用｜手工建外包账号的授权凭据与操作备注落库有断言（createAccessUserStoresManualEvidenceAndProvisionsCredential）；测试数据为虚构号码｜通过 |
| PGM-066 已拍板规则和待排期问题 | 适用｜高危须审批、岗位模板内高危不免审、显式拒绝不被挂起单复用等已拍板规则有测试｜通过 |

## 5. 已定稿 001、002 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。W-M036-1…3、XM-M036-A 登记未变。

## 6. 本次 Findings

无新增。

## 7. 结论

席甲 B5-r1 票：**PASS**。3 个文件（AuthorizationServiceTests 两版）全读、52 判据逐项、取证脚本实跑复核；未改任何已跟踪文件；落盘前未读 Grok 本次正文。
