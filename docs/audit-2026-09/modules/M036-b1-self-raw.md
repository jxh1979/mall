CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M036 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；AccessServiceTests 里 M031 的 4 条测试与 M031、M032 的内存仓改动为席甲本轮所写）。2026-09-21T10:34:43Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main 3ca1b507。
- 模块 M036「功能权限点、职级三十档、权限模板与最终权限计算／后台验证／第2段」3 个测试文件，主工作区版 3048 行，52 判据。B3 累计 0。
- 两条基线（D-029）：AccessServiceTests.java、M006JdbcRegressionTests.java 与 origin/main 逐字节相同；AuthorizationServiceTests.java 主工作区版 V0.03 含未发布工作流改动（24 个测试），origin/main 版 V0.01（21 个测试）为发布口径。两版都全读，下文每条结论注明在哪一版成立。
- 顺序：本席取证脚本先跑完，随后才派 Grok（两边都要跑 Maven，错开）；本报告落盘前未读 Grok 正文（modules/M036-b1-grok-raw.md 存在=True）。只读承诺：未改任何已跟踪文件；变异只发生在一次性 worktree 里且已删除；对库只做 SELECT 与跑既有测试类。
- 方法（测试模块）：逐个测试问「把被测实现改坏，它会不会红」；再反过来从被测的 AuthorizationService、AccessService 每个带安全含义的分支出发，用变异实跑全量后端测试，问「有没有哪个测试会因它变坏而红」；内存仓与临时表逐项对 JdbcAccessRepository 的 SQL 与真表结构。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| AccessServiceTests.java（主工作区版） | 2134 | 126922 | bf931a403f1217b4a23f184e4234e19b1f78cd8f1e1b5342b58ebf2805aebf1a | 全读 |
| AuthorizationServiceTests.java（主工作区版） | 595 | 25843 | 0f4c11d753cedc0f5310fda294b9101bcb75b18e5b91282677a3f66fe71fca28 | 全读 |
| M006JdbcRegressionTests.java（主工作区版） | 319 | 23696 | 49ca8eadee2ec90f0fc310d9285c6e2b01b449bc8e9289091ff9ca73322b9b91 | 全读 |
| AuthorizationServiceTests.java（origin/main 版 V0.01，发布口径） | 564 | 23963 | 0ddd8777da6df884af91e259c802607df903ccbe0a7761ddb0fdd0d775ba7222 | 全读 |

## 2. 可执行证据原文

### 2.1 两条基线（/home/james/audit-tmp/m036_b1_run.log）

```
START=2026-09-21T10:15:59Z HEAD=e456ba96 origin/main=3ca1b507
== 0. files (lines/bytes/sha256 | vs origin/main | origin/main 版 sha256)
AccessServiceTests.java 2134 126922 bf931a403f1217b4a23f184e4234e19b1f78cd8f1e1b5342b58ebf2805aebf1a | [] | bf931a403f1217b4a23f184e4234e19b1f78cd8f1e1b5342b58ebf2805aebf1a 2134 行
AuthorizationServiceTests.java 595 25843 0f4c11d753cedc0f5310fda294b9101bcb75b18e5b91282677a3f66fe71fca28 | [ 1 file changed, 40 insertions(+), 9 deletions(-)] | 0ddd8777da6df884af91e259c802607df903ccbe0a7761ddb0fdd0d775ba7222 564 行
M006JdbcRegressionTests.java 319 23696 49ca8eadee2ec90f0fc310d9285c6e2b01b449bc8e9289091ff9ca73322b9b91 | [] | 49ca8eadee2ec90f0fc310d9285c6e2b01b449bc8e9289091ff9ca73322b9b91 319 行
未发布提交（origin/main..HEAD）触及本模块文件：
e9b88faf fix(access): 非密钥主体审批层级不超过上两级
52074492 fix(access): 平台管理员不再整类关掉商户项目隔离
523be51e audit M031: transactional position-template save and cross-scope tests
== 1A. 测试形状（主工作区版）
== AccessServiceTests.java: 54 tests, 2134 lines
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
   savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants                     L1569  lines= 49 asserts= 5 
   positionTemplateLifecycleAndGrantPolicyFailuresAreAudited                                            L1620  lines= 13 asserts= 5 
   nested/inner types: AccessServiceTests, RecordingCredentialProvisioner, CredentialProvisionRequest, InMemoryAccessRepository, InMemorySupervisorReviewRepository
   annotations: 
== AuthorizationServiceTests.java: 24 tests, 595 lines
   requirePermissionReturnsForbiddenPermissionDeniedWhenUserLacksPermission                             L53    lines= 10 asserts= 3 
   denyWinsWhenGrantAndDenyExistForSamePermission                                                       L64    lines= 12 asserts= 3 
   expiredTemporaryTemplateGrantDoesNotAuthorize                                                        L77    lines= 19 asserts= 2 
   singleUseTemplateGrantIsConsumedAfterFirstSuccessfulAuthorization                                    L97    lines= 22 asserts= 3 
   localSeedBridgeAuthorizesPlatformAdminButNotArbitraryStaffWithSmokeRole                              L120   lines= 10 asserts= 2 
   singleUsePermissionConcurrentConsumerIsDeniedWhenAtomicUpdateReturnsZeroRows                         L131   lines= 23 asserts= 2 
   isPlatformAdminReturnsTrueForPlatformAdminActor                                                      L155   lines=  5 asserts= 1 
   isPlatformAdminReturnsFalseForStaffActor                                                             L161   lines=  5 asserts= 1 
   outsourcedVendorIsNotPlatformScopeAndRequiresProjectScope                                            L167   lines= 13 asserts= 6 
   outsourcedFacilityTemporaryGrantAllowsAssignedWorkAndEquipmentReadButNotAccept                       L181   lines= 17 asserts= 0 
   outsourcedTemporaryGrantExpiresAndNoLongerAuthorizes                                                 L199   lines=  7 asserts= 0 
   outsourcedSecurityCleaningTemporaryGrantOnlyGrantsAssignedInspectionAndExceptionPermissions          L207   lines= 18 asserts= 0 
   currentMerchantScopeReturnsBoundMerchantForPlatformAdmin                                             L226   lines=  5 asserts= 1 
   currentMerchantIdQuietlyReturnsBoundMerchantForPlatformAdmin                                         L232   lines=  5 asserts= 1 
   productionMasterIdentityGetsExistingAuthorityWithoutBypassingPermissionDenials                       L238   lines= 13 asserts= 5 
   platformAdminAndIncompleteMasterIdentityDoNotBecomeHighestAuthority                                  L252   lines= 11 asserts= 4 
   currentSupervisorLevelIsThreeOnlyForMasterAuthority                                                  L264   lines=  7 asserts= 1 
   currentMerchantIdQuietlyReturnsMerchantIdForRealMerchantUser                                         L272   lines=  6 asserts= 1 
   currentMerchantScopeReturnsMerchantIdForMerchantUser                                                 L279   lines=  6 asserts= 1 
   currentMerchantScopeThrowsForbiddenWhenMerchantIdIsAbsent                                            L286   lines= 16 asserts= 3 
   effectivePermissionsRemovesDenyCoveredCodesFromGrantsAndListsDenies                                  L303   lines= 15 asserts= 5 
   effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard                                      L319   lines=  5 asserts= 1 
   effectivePermissionsQueryDoesNotConsumeSingleUseGrant                                                L325   lines= 22 asserts= 3 
   effectivePermissionsExcludesExpiredGrant                                                             L348   lines= 17 asserts= 1 
   nested/inner types: AuthorizationServiceTests, InMemoryAccessRepository
   annotations: 
== M006JdbcRegressionTests.java: 12 tests, 319 lines
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
   legacyPositionMigrationPreservesIndependentGrantsAndCountsOccupiedDefinitions                        L258   lines= 37 asserts= 8 
   nested/inner types: -
   annotations: 
== 2. 发布口径基线：origin/main 一次性 worktree
scratch at 3ca1b507
== 1B. 测试形状（origin/main 版 AuthorizationServiceTests）
== AuthorizationServiceTests.java: 21 tests, 564 lines
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
   nested/inner types: AuthorizationServiceTests, InMemoryAccessRepository
   annotations: 
temp migrations (Flyway validate 用，跑完即删): V427__m009_project_profiles_and_associations.sql
MVN_EXIT(origin/main)=0
   AccessServiceTests tests=54 errors=0 skipped=0 failures=0
   AuthorizationServiceTests tests=21 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=12 errors=0 skipped=0 failures=0
scratch dirty after cleanup: 0
scratch removed
== 3. 主工作区现状基线
MVN_EXIT(main worktree)=1
   AccessServiceTests tests=54 errors=6 skipped=0 failures=0
       error assignUserPositionLandsNormalPartAndHoldsHighRiskPartUntilChainApproves | 退款、免单、授权与资金事项必须登记直接受益人
       error dailyTemporaryPermissionApplicationEntersSupervisorChain | 退款、免单、授权与资金事项必须登记直接受益人
       error approvedTemporaryGrantExpiresByEffectivePermissionRead | 退款、免单、授权与资金事项必须登记直接受益人
       error auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse | 退款、免单、授权与资金事项必须登记直接受益人
       error approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment | 退款、免单、授权与资金事项必须登记直接受益人
       error removingTheHighRiskHoldDenyRequiresApproval | 退款、免单、授权与资金事项必须登记直接受益人
   AuthorizationServiceTests tests=24 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=12 errors=1 skipped=0 failures=0
       error temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce | PreparedStatementCallback; bad SQL grammar [UPDATE temporary_permission_grants&#10;   SET consumed = TRUE,&#10;       consumed_at = ?&#10; W
[ERROR] com.complex.platform.access.M006JdbcRegressionTests.temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce -- Time elapsed: 0.045 s <<< ERROR!
[ERROR] com.complex.platform.access.AccessServiceTests.assignUserPositionLandsNormalPartAndHoldsHighRiskPartUntilChainApproves -- Time elapsed: 0.006 s <<< ERROR!
[ERROR] com.complex.platform.access.AccessServiceTests.dailyTemporaryPermissionApplicationEntersSupervisorChain -- Time elapsed: 0.004 s <<< ERROR!
[ERROR] com.complex.platform.access.AccessServiceTests.approvedTemporaryGrantExpiresByEffectivePermissionRead -- Time elapsed: 0.004 s <<< ERROR!
[ERROR] com.complex.platform.access.AccessServiceTests.auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse -- Time elapsed: 0.004 s <<< ERROR!
[ERROR] com.complex.platform.access.AccessServiceTests.approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment -- Time elapsed: 0.004 s <<< ERROR!
[ERROR] com.complex.platform.access.AccessServiceTests.removingTheHighRiskHoldDenyRequiresApproval -- Time elapsed: 0.005 s <<< ERROR!
[ERROR] Errors: 
[ERROR]   AccessServiceTests.approvedTemporaryGrantExpiresByEffectivePermissionRead:1064 » Api 退款、免单、授权与资金事项必须登记直接受益人
[ERROR]   AccessServiceTests.approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment:1034 » Api 退款、免单、授权与资金事项必须登记直接受益人
[ERROR]   AccessServiceTests.assignUserPositionLandsNormalPartAndHoldsHighRiskPartUntilChainApproves:656 » Api 退款、免单、授权与资金事项必须登记直接受益人
[ERROR]   AccessServiceTests.auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse:1112 » Api 退款、免单、授权与资金事项必须登记直接受益人
[ERROR]   AccessServiceTests.dailyTemporaryPermissionApplicationEntersSupervisorChain:1451 » Api 退款、免单、授权与资金事项必须登记直接受益人
[ERROR]   AccessServiceTests.removingTheHighRiskHoldDenyRequiresApproval:887 » Api 退款、免单、授权与资金事项必须登记直接受益人
[ERROR]   M006JdbcRegressionTests.temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce:71 » BadSqlGrammar PreparedStatementCallback; bad SQL grammar [UPDATE temporary_permission_grants
== 4. complex_test Flyway 顶
430
== 5. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T10:16:15Z
```

### 2.2 变异核对（/home/james/audit-tmp/m036_mutation.log；origin/main 一次性 worktree，逐个变异跑全量后端测试，列相对基线新增的红；基线红 20 条另列于 §6 XM-M036-A）

```
START=2026-09-21T10:21:48Z origin/main=3ca1b507
scratch at 3ca1b507
temp migrations: V427__m009_project_profiles_and_associations.sql
MVN_EXIT(base)=1
  suites=263 tests=3471 red=20
  baseline seconds=48
mutated multi_first_only AuthorizationService.java
MVN_EXIT(multi_first_only)=1
  suites=263 tests=3471 red=21
    NEW-RED[multi_first_only]: M006SecurityRegressionTests.twoPermissionReviewConsumesOneSourceOnlyAfterBothChecksPass
mutated no_staff_registration AccessService.java
MVN_EXIT(no_staff_registration)=1
  suites=263 tests=3471 red=21
    NEW-RED[no_staff_registration]: M006SecurityRegressionTests.manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval
mutated unclassified_as_low AccessService.java
MVN_EXIT(unclassified_as_low)=1
  suites=263 tests=3471 red=21
    NEW-RED[unclassified_as_low]: AccessServiceTests.grantPolicyRejectsAnyUnclassifiedPermissionInTheRequestedRole
mutated ignore_valid_from AuthorizationService.java
MVN_EXIT(ignore_valid_from)=1
  suites=263 tests=3471 red=20
mutated ignore_actor_status AuthorizationService.java
MVN_EXIT(ignore_actor_status)=1
  suites=263 tests=3471 red=20
mutated ignore_unknown_permission AuthorizationService.java
MVN_EXIT(ignore_unknown_permission)=1
  suites=263 tests=3471 red=20
mutated no_outsourced_assignee_scope AuthorizationService.java
MVN_EXIT(no_outsourced_assignee_scope)=1
  suites=263 tests=3471 red=28
    NEW-RED[no_outsourced_assignee_scope]: FacilityInspectionServiceTests.outsourcedCanRecordOwnInspectionButCannotRecordOthersOrRecheck
    NEW-RED[no_outsourced_assignee_scope]: FacilityInspectionServiceTests.outsourcedCanReportExceptionForOwnTaskAndTriggersWorkOrderPath
    NEW-RED[no_outsourced_assignee_scope]: FacilityInspectionServiceTests.outsourcedTaskListDetailAndRecordsAreLimitedToOwnAssigneeAndProject
    NEW-RED[no_outsourced_assignee_scope]: FacilityWorkOrderServiceTests.outsourcedCanProcessOwnWorkOrderButCannotAcceptClosure
    NEW-RED[no_outsourced_assignee_scope]: FacilityWorkOrderServiceTests.outsourcedListAndDetailAreLimitedToOwnAssigneeAndProject
    NEW-RED[no_outsourced_assignee_scope]: SiteEntryReportServiceTests.outsourcedCannotOperateOthersOrPropertyReviewAcceptance
    NEW-RED[no_outsourced_assignee_scope]: SiteEntryReportServiceTests.outsourcedCanUploadCompletionForOwnInProgressReportOnly
    NEW-RED[no_outsourced_assignee_scope]: SiteEntryReportServiceTests.outsourcedListAndDetailAreLimitedToOwnAssigneeAndProject
mutated via_role_ignores_deny AuthorizationService.java
MVN_EXIT(via_role_ignores_deny)=1
  suites=263 tests=3471 red=20
scratch dirty after cleanup: 0
scratch removed
END=2026-09-21T10:28:48Z
```

逐个说明：
- multi_first_only（多权限门控只核第一个码）→ M006SecurityRegressionTests.twoPermissionReviewConsumesOneSourceOnlyAfterBothChecksPass 变红：有守（在同聚合第 3 段 M037）。
- no_staff_registration（去掉「员工须先提交注册申请」分支）→ M006SecurityRegressionTests.manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval 变红：有守（M037）。
- unclassified_as_low（未配置审批策略的风险等级改按最低档）→ AccessServiceTests.grantPolicyRejectsAnyUnclassifiedPermissionInTheRequestedRole 变红：有守（本模块）。
- no_outsourced_assignee_scope → 8 个设施巡检/工单/进场报备测试变红：有守。
- ignore_actor_status（停用主体不再拒绝）→ 0 变红。上游第一道闸在 ProductionAuthService.authenticateToken：解析访问令牌时 `.filter(session -> "active".equals(session.userStatus()))`（origin/main L304，其注释写明停用或锁定的凭据令已签发的令牌立即失效），停用账号到不了这里；本处是第二道闸，登记 OBS-6，不立项。
- ignore_unknown_permission（未登记的权限码不再拒绝）→ 0 变红。去掉后普通主体仍因无授予而被拒、平台管理员桥只认字典里的日常码；只有最高权限主体会对未登记码放行，无业务后果。登记 OBS-6，不立项。
- via_role_ignores_deny（按角色持有判断忽略显式拒绝）→ 0 变红。该判断只用于审批身份（currentSupervisorLevel），而审批入口先过 `requirePermissions(..., "governance.supervisor_review.approve")`，显式拒绝在那里已经拒掉；本处是第二道闸。登记 OBS-6，不立项。
- ignore_valid_from（生效时间未到也算生效）→ 0 变红，且没有上游闸：见 CX-M036-001。

### 2.3 真表结构（/home/james/audit-tmp/m036_schema.log；complex_test 只读查询）

```
permission_template_applications.id character varying(64) NOT NULL
permission_template_applications.template_id character varying(64) NOT NULL
permission_template_applications.target_type character varying(64) NOT NULL
permission_template_applications.target_id character varying(64) NOT NULL
permission_template_applications.valid_from timestamp with time zone
permission_template_applications.valid_until timestamp with time zone
permission_template_applications.single_use boolean NOT NULL DEFAULT false
permission_template_applications.consumed boolean NOT NULL DEFAULT false
permission_template_applications.consumed_at timestamp with time zone
permission_template_applications.status character varying(32) NOT NULL DEFAULT 'active'::character varying
permission_template_applications.applied_by character varying(128)
permission_template_applications.applied_at timestamp with time zone NOT NULL
permission_template_applications.revoked_by character varying(128)
permission_template_applications.revoked_at timestamp with time zone
permission_template_usage_logs.id uuid NOT NULL
permission_template_usage_logs.application_id character varying(64)
permission_template_usage_logs.user_id character varying(64)
permission_template_usage_logs.permission_code character varying(128)
permission_template_usage_logs.result character varying(32) NOT NULL
permission_template_usage_logs.reason character varying(128)
permission_template_usage_logs.occurred_at timestamp with time zone NOT NULL
permission_templates.id character varying(64) NOT NULL
permission_templates.template_name character varying(128) NOT NULL
permission_templates.description text
permission_templates.created_at timestamp with time zone NOT NULL DEFAULT now()
permission_templates.template_code character varying(128) NOT NULL
permission_templates.template_type character varying(32) NOT NULL DEFAULT 'base'::character varying
permission_templates.source_template_id character varying(64)
permission_templates.owner_scope_type character varying(64)
permission_templates.owner_scope_id character varying(64)
permission_templates.usage_mode character varying(64) NOT NULL DEFAULT 'merge'::character varying
permission_templates.valid_from timestamp with time zone
permission_templates.valid_to timestamp with time zone
permission_templates.single_use boolean NOT NULL DEFAULT false
permission_templates.status character varying(32) NOT NULL DEFAULT 'active'::character varying
permission_templates.reason text
permission_templates.approved_by character varying(128)
permission_templates.approved_at timestamp with time zone
permission_templates.updated_at timestamp with time zone NOT NULL
temporary_permission_grants.id uuid NOT NULL
temporary_permission_grants.subject_type character varying(64) NOT NULL
temporary_permission_grants.subject_id character varying(128) NOT NULL
temporary_permission_grants.permission_code character varying(128) NOT NULL
temporary_permission_grants.reason text NOT NULL
temporary_permission_grants.granted_at timestamp with time zone NOT NULL
temporary_permission_grants.expires_at timestamp with time zone NOT NULL
temporary_permission_grants.granted_by character varying(128)
temporary_permission_grants.status character varying(32) NOT NULL DEFAULT 'active'::character varying
temporary_permission_grants.valid_from timestamp with time zone NOT NULL
temporary_permission_grants.revoked_by character varying(128)
temporary_permission_grants.revoked_at timestamp with time zone
temporary_permission_grants.single_use boolean NOT NULL DEFAULT false
temporary_permission_grants.consumed boolean NOT NULL DEFAULT false
temporary_permission_grants.consumed_at timestamp with time zone
temporary_permission_grants.grant_group_id character varying(64) NOT NULL
permission_template_usage_logs permission_template_usage_logs_application_id_fkey FOREIGN KEY (application_id) REFERENCES permission_template_applications(id) ON DELETE SET NULL
```

## 3. 三线审计

- 第一性原理线。用途：守住最终权限计算（AuthorizationServiceTests）、权限后台服务层写路径（AccessServiceTests）与仓储真实 SQL 的并发/回滚/迁移语义（M006JdbcRegressionTests）。实际行为：发布口径 87 个测试全绿，断言均能失败。未经验证的前提：「授权按时间窗生效的两头都有测试守着」——只守了到期一头，未到生效时间那一头为 0（CX-M036-001）。
- 顺从式线：52 判据逐项见 §4；三个文件（含 AuthorizationServiceTests 两版）全读。
- 对抗式线（grilling）：Q 内存仓会不会把被测逻辑整个替换掉？A 被测的是 AccessService/AuthorizationService 的判断，内存仓只供数；三处与 Jdbc 语义不同的点（OBS-1）服务层测试都没有拿来断言。Q 临时表会不会让 SQL 在假表上过、真表上挂？A 对照真表（§2.3），origin/main 版被测 SQL 用到的列与非空约束都满足；主工作区未发布版里单次消耗 SQL 改用 grant_group_id，手写临时表没有该列，M006JdbcRegressionTests 即报 bad SQL grammar（W-3）——说明手写表能暴露「SQL 用了表里没有的列」，但暴露不了「真表多了非空列」。Q assumeTrue 会不会让门禁空转？A 缺环境变量时整类 12 个测试记为跳过而不是失败；后端测试本就不在 CI 的 predeploy-gates 里，只在 160 手工跑（OBS-4）。Q 到期测试会不会随运行当天漂移？A 用 2020 与 2099 的固定时刻，不漂移。

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
| GEN-002 可执行正确性 | 适用｜发布口径（origin/main 一次性 worktree）54＋21＋12 全绿；主工作区现状 AccessServiceTests 6 个报错、M006JdbcRegressionTests 1 个报错，均来自别的模块未发布的源码改动（W-5、W-3），AuthorizationServiceTests 主工作区版 24 全绿（探针 2、3）｜通过（发布口径；W 项登记） |
| GEN-003 测试有效性 | 适用｜逐个测试核「改坏会不会红」并跑了 8 个变异（§2.2）：5 个被现有测试抓到或由上游第一道闸守住；`ignore_valid_from`（把「生效时间未到不算生效」的判断去掉）在全量 3471 个后端测试里 0 个变红，而 `findPermissionEffects` 的 SQL 不按生效时间过滤（只回传 valid_from 由 Java 判），权限模板页又提供「生效时间」输入——这是首次正常使用就能走到、且只有这一处守着的分支（CX-M036-001）｜不通过（CX-M036-001） |
| GEN-004 信任边界校验 | 适用｜账号类型白名单、缺组织、未知职级、非人民币、状态非法、缺失效时间等必填/取值校验有测试｜通过 |
| GEN-005 授权与最小权限 | 适用｜见 BSM-003；授权按时间窗生效的下半边（未到生效时间）无测试（CX-M036-001）｜不通过（CX-M036-001） |
| GEN-006 数据完整性 | 适用｜M006JdbcRegressionTests 用连接内临时表验证真实 SQL：单次消耗只成功一次、审计失败回滚消耗、指纹只写一次、换岗只撤自己派生的应用、创建碰撞不覆盖、软删可恢复。其中 4 张手写临时表（permission_templates、permission_template_applications、temporary_permission_grants、permission_template_usage_logs）只建了被测 SQL 用到的列；与真表对照（§2.3）：被测 UPDATE/INSERT 用到的列真表都有、类型相容，用量日志 INSERT 给全了真表的非空列，reason 最长 63 字符小于真表 128（OBS-2）｜通过（登记 OBS-2） |
| GEN-007 并发与幂等 | 适用｜行锁、表锁保持到事务结束、审计失败经真实 Spring 事务回滚，有真库测试；每个测试一条独立连接、临时表随连接销毁，不留残留、不依赖顺序｜通过 |
| GEN-008 接口与模式一致性 | 适用｜AccessServiceTests 内存仓与 JdbcAccessRepository 的语义核对：岗位映射 upsert 不动存档/删除列、生命周期保存恒写 inactive（M031 已对齐）；内存仓 `findPermissionEffects` 不含岗位派生角色、`replacePositionGrants` 只记角色指针不撤旧应用、`markSingleUsePermissionConsumed` 恒 true——相应真实语义分别由 M006JdbcRegressionTests（换岗撤旧应用、单次消耗）与 AccessJdbcIntegrationTests 守住，服务层测试未断言这些点（OBS-1）｜通过（登记 OBS-1） |
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
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 适用｜到期失效、单次消耗、并发二次消耗被拒均有测试；「生效时间未到」无任何测试（CX-M036-001）｜不通过（CX-M036-001） |
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

## 5. 本次 Findings

### CX-M036-001（技术性；工程缺陷成立；两版均成立）「生效时间未到的授权不放行」零测试

- 位置：`AuthorizationServiceTests.java`（origin/main 版 21 个测试、主工作区版 24 个测试，均无此场景）；被测 `AuthorizationService.java` 的 `isActive`：`if (effect.validFrom() != null && checkedAt.isBefore(effect.validFrom())) return false;`（origin/main 版 L533-535）。
- 证据：变异 ignore_valid_from 把这一判断去掉后，全量 3471 个后端测试 0 个新增变红（§2.2）。`JdbcAccessRepository.findPermissionEffects` 的 SQL 不按生效时间过滤，只把 valid_from 回传给 Java 判（origin/main 版该查询的模板应用分支 `COALESCE(pta.valid_from, pt.valid_from) AS valid_from`、临时授权分支 `valid_from`，WHERE 里只有 status='active'），所以这一行是唯一的闸。现有时间窗测试只覆盖到期一头：`expiredTemporaryTemplateGrantDoesNotAuthorize`、`outsourcedTemporaryGrantExpiresAndNoLongerAuthorizes`、`effectivePermissionsExcludesExpiredGrant`。
- 正常触发：权限模板页有「生效时间」输入（apps/web/src/admin/pages/PermissionTemplatesPage.tsx L381、L501），临时授权请求也带 valid_from；管理员把模板设成明天生效并套给员工，是首次正常使用就会发生的事。这一行若在重构中回退，员工当天就拿到权限，没有测试会发现。
- 依据：GEN-003「测试须实际执行目标行为、具备能失败的断言，并覆盖本模块关键分支」；上位是治理地图 L656「临时授权可设有效期或单次使用」、L659「有时间限制、到期自动失效」。
- 最小拟处置：AuthorizationServiceTests 补一条——给员工一条生效时间在 2099 年的授予，断言 `requirePermission` 拒绝、`effectivePermissions().grants()` 不含该码。不改被测代码。两版文件打同一处补丁；发布用「origin/main 版＋补丁」。

## 6. 观察与登记（不立项）

- OBS-1：AccessServiceTests 内存仓与 Jdbc 语义的三处差异（`findPermissionEffects` 不含岗位派生角色；`replacePositionGrants` 只记角色指针、不撤旧派生应用；`markSingleUsePermissionConsumed` 恒 true）。相应真实语义由 M006JdbcRegressionTests 与 AccessJdbcIntegrationTests 守住；服务层测试没有拿这三点做断言，不构成假绿。
- OBS-2：M006JdbcRegressionTests 的 4 张手写临时表只含被测 SQL 用到的列；origin/main 版下与真表相容（§2.3）。
- OBS-3：`createAccessUserWithAllowedAccountTypeSucceeds` 的单元素循环与恒假条件是缩表后留下的死代码；「非外包类型须先注册」分支由 M037 的测试守住（变异 no_staff_registration）。
- OBS-4：M006JdbcRegressionTests 缺环境变量时整类跳过；后端测试不在 CI 门禁里。
- OBS-5：`copyPermissionTemplateKeepsSnapshot…` 源模板前后权限项相同；副本快照的真实语义由 AccessJdbcIntegrationTests 守住。
- OBS-6：三个变异幸存但属第二道闸（停用主体、未登记权限码、按角色持有判断的显式拒绝），见 §2.2。
- W-5（只在主工作区未发布版成立，不入队、不发布）：主工作区的 SupervisorReviewService 含未发布改动（提交 934e944f、7d047472、c308dc1c、0ef8c1a0、a0cf742a：直接受益人必填等），AccessService 发起授权申请时不带受益人，导致 AccessServiceTests 6 个测试报「必须登记直接受益人」（探针 3）。该工作流发布时须同批改 AccessService 调用点与这些测试。
- W-3（已登记于 M032，此处为其在本模块文件上的表现）：主工作区未发布的单次授权整组消耗改动用到 grant_group_id，M006JdbcRegressionTests 的手写临时表没有该列，`temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce` 报 bad SQL grammar。
- W-6（只在主工作区未发布版成立）：AuthorizationServiceTests V0.03 多出的 3 个测试与 2 个改名测试锁的是未发布的最高权限单独识别与「平台管理员不再整类关掉商户隔离」行为；随该工作流一并发布，不随本模块发布。
- XM-M036-A（→各自模块）：origin/main 3ca1b507 上全量后端测试 3471 个里有 20 个红，与本模块无关：FinanceControllerTests 12 个（ApplicationContext 加载失败）、ComplianceControllerTests 3 个（期望 200/400 实得 403）、FacilityEquipmentArchiveDeleteJdbcIntegrationTests 3 个（含「没有操作权限：facility.equipment.approve」）、AmusementServiceTests 1 个（「没有操作权限：amusement.project_entitlement.approve」）、CustomerGuardedActionAndRejectionAuditTests 1 个（StoredValueService#consume 应带 @GuardedAction）。清单：

```
AmusementServiceTests.tombstonedProjectEntitlementRestoreRequiresApprovePermission
ComplianceControllerTests.createContractKeyDateRequiresManagePermissionAndReturnsRecord
ComplianceControllerTests.createContractKeyDateWithoutIdempotencyKeyReturnsValidationFailed
ComplianceControllerTests.scanRemindersRequiresScanPermissionAndReturnsGeneratedCount
FacilityEquipmentArchiveDeleteJdbcIntegrationTests.approveDeleteRejectedWhenReferencedByWorkOrder
FacilityEquipmentArchiveDeleteJdbcIntegrationTests.archiveThenApproveDeleteSucceedsWhenNoReferences
FacilityEquipmentArchiveDeleteJdbcIntegrationTests.selfApproveForbidden
FinanceControllerTests.createRefundRejectsNonPositiveAmount
FinanceControllerTests.paymentIntentUsesPaymentIntentCreatePermission
FinanceControllerTests.createPaymentIntentRejectsNonPositiveAmount
FinanceControllerTests.paymentRegistrationUsesPaymentRegisterPermission
FinanceControllerTests.dailyCloseUsesDailyCloseLockPermission
FinanceControllerTests.offlineConfirmationUsesPaymentRegisterPermission
FinanceControllerTests.createArApItemMissingIdempotencyKeyReturnsValidationFailed
FinanceControllerTests.refundUsesRefundCreatePermission
FinanceControllerTests.listArApItemsUsesOpenApiViewPermissionAndPageShape
FinanceControllerTests.reconciliationEndpointsUseOpenApiPermissions
FinanceControllerTests.createArApItemRejectsNonPositiveAmount
FinanceControllerTests.exportArApItemsUsesDedicatedExportPermissionAndReturnsCsvSnapshot
CustomerGuardedActionAndRejectionAuditTests.customerActionsCarryNonEmptyCapability(Class, String, String)[3]
```

## 7. 结论

席甲 B1 票：**FAIL**（CX-M036-001；OBS-1…6、W-3/5/6、XM-M036-A 登记）。3 个文件（含 AuthorizationServiceTests 两版）全读、52 判据逐项、三线完成；未改任何已跟踪文件；落盘前未读 Grok 本次正文。


## M036 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-21T10:39:54Z。输入：modules/M036-b1-grok-raw.md V0.01（36104 bytes，SHA256 7cd36b38f3da7ab31d88464c781b9650aec7633a4b58cc5b5deab1c5baa41242）、modules/M036-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M036 B2-r1 整批候选」段（全文件 SHA256 f25edd57002f0ed62f219eb427321c3164e0b8e6e414c744bfb52919d91131b4）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-01→001；CX-M036-001→002；Grok W-1＋W-2→W-M036-1、W-3→W-M036-2、W-4→W-M036-3；席甲 XM-M036-A 登记；两席其余观察全部登记；无漏并、无改写。
- 自审纠正：上方 B1 报告 OBS-1 与 GEN-008 行写「相应真实语义分别由 M006JdbcRegressionTests 与 AccessJdbcIntegrationTests 守住」，对「岗位派生角色并入有效权限」这一点不成立——读 Grok 报告后补跑变异 no_position_role_join，全量 0 红。已有测试守住的只是换岗撤旧应用（M006JdbcRegressionTests）与单次消耗。席甲当时只按测试名去找对应的真库测试，没有拿变异去验，属同一类「凭印象判已覆盖」。
- 001 处置比 Grok 原拟多两点的理由：应用 id 不唯一时按 Jdbc 语义撤旧会误撤新应用，属对齐语义的必要部分；真库面零测试由同一变异证实，AccessJdbcIntegrationTests 已闭环不能改，本模块的 M006JdbcRegressionTests 是唯一可落点。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
