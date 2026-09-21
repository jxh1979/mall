CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M037 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；本文件里 M031 的一处桩与一句断言为席甲本轮所写）。2026-09-21T11:50:26Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main 40fcc8f1。
- 模块 M037「功能权限点、职级三十档、权限模板与最终权限计算／后台验证／第3段」1 个测试文件，52 判据。B3 累计 0。
- 两条基线（D-029）：origin/main 版 V0.30（39 个测试）为发布口径；主工作区版含未发布工作流的暂存改动（39 个测试）。两版都读，下文每条结论注明在哪一版成立。
- 顺序：本席基线探针先跑完，随后才派 Grok；变异在一次性 worktree 里跑，与 Grok 并行但互不共用目录。本报告落盘前未读 Grok 正文（modules/M037-b1-grok-raw.md 存在=True）。只读承诺：未改任何已跟踪文件；一次性 worktree 均已删除。
- 方法：先把 25 处「只断言抛 ApiException、不核错误码」的断言逐处列出，对每处问「守卫拿掉以后，它是会红，还是会因为别的守卫也抛错而照样绿」；能靠 `verify(never)`／状态断言兜住的记下，其余用变异在全量后端测试上实跑（按 M036 的教训：不凭印象写「别处已守住」）。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| M006SecurityRegressionTests.java（origin/main 版 V0.30，发布口径） | 809 | 58350 | c2d8b8a69779b7c5555cc49c7776f233b0e16bc875c22bfc96149799f48698c3 | 全读 |
| M006SecurityRegressionTests.java（主工作区版，含未发布暂存改动） | 823 | 59529 | 167733bc7c2b9fb07271779bb17d9447ccbefe6c5f7c56da3a6329b12c0b40b6 | 全读（与发布口径的差异 26＋/12－ 逐行读） |

## 2. 可执行证据原文

### 2.1 两条基线与断言清单（/home/james/audit-tmp/m037_b1_run.log）

```
START=2026-09-21T11:32:21Z HEAD=e456ba96 origin/main=40fcc8f1
== 0. file (lines/bytes/sha256)
主工作区版 823 59529 167733bc7c2b9fb07271779bb17d9447ccbefe6c5f7c56da3a6329b12c0b40b6 | vs origin/main [ 1 file changed, 26 insertions(+), 12 deletions(-)] | 暂存 [ 1 file changed, 27 insertions(+), 13 deletions(-)] | 未暂存 [ 1 file changed, 3 insertions(+), 2 deletions(-)]
origin/main 版 809 58350 c2d8b8a69779b7c5555cc49c7776f233b0e16bc875c22bfc96149799f48698c3
未发布提交（origin/main..HEAD）触及本文件：0 个
== 1. 发布口径基线：origin/main 一次性 worktree
scratch at 40fcc8f1
== 1A. 测试形状（origin/main 版）
== M006SecurityRegressionTests.java: 39 tests, 809 lines
   administratorDenyWinsAndHighPermissionNeedsExplicitGrant                                             L105   lines=  9 asserts= 2 
   administratorKeepsDailyPermissionsWithoutAutomaticHighRole                                           L116   lines=  5 asserts= 0 
   administratorHoldsGovernanceWritePermissionsFromDefaultRoleButNotBusinessHighRisk                    L124   lines= 13 asserts= 1 
   untrustedPrincipalIsRejected                                                                         L139   lines=  5 asserts= 1 
   missingOrganizationCannotListAllUsersOrResolveGlobalProject                                          L146   lines=  6 asserts= 2 
   organizationReadWorksButCrossMerchantWriteDoesNot                                                    L154   lines= 12 asserts= 3 
   collidingUserSlugsRemainDifferentUsers                                                               L168   lines=  6 asserts= 2 
   templateEditCannotGrantAnUnownedHighPermission                                                       L176   lines=  8 asserts= 2 
   templateEditPreservesOwnerAndRejectsMissingTemplate                                                  L186   lines=  9 asserts= 2 
   emptyPermissionsAndInvertedWindowAreRejectedBeforeGrantWrite                                         L197   lines= 11 asserts= 3 
   invalidStatusAndBlankPasswordHaveNoWrites                                                            L210   lines=  8 asserts= 3 
   manualAccountDoesNotPersistOrReturnPhoneDigest                                                       L220   lines=  5 asserts= 1 
   administratorWithoutApprovalRoleHasNoSupervisorLevel                                                 L227   lines=  3 asserts= 1 
   twoPermissionReviewConsumesOneSourceOnlyAfterBothChecksPass                                          L232   lines= 17 asserts= 4 
   credentialAdapterDisablesInvitedAccountsAndRejectsBlankPassword                                      L251   lines= 14 asserts= 3 
   developmentCredentialAdapterRejectsMissingAccount                                                    L267   lines=  7 asserts= 1 
   changedRoleCannotBeAppliedUnderAnOldApproval                                                         L276   lines= 16 asserts= 3 
   beneficiaryCannotApproveOwnGrant                                                                     L294   lines= 14 asserts= 3 
   positionChangeDropsDerivedRoleButKeepsIndependentAssignment                                          L325   lines= 35 asserts= 2 
   appliedTemplateCannotExtendTimeOrRemoveSingleUse                                                     L363   lines= 14 asserts= 3 
   pendingTemporaryGrantRejectsInvertedTimeWindow                                                       L379   lines=  9 asserts= 2 
   topAuthorityReviewRequiresMasterSessionRatherThanRankAlone                                           L390   lines= 31 asserts= 3 
   manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval                               L424   lines=  9 asserts= 2 
   accountJsonDoesNotExposeStoredPhoneDigest                                                            L435   lines=  6 asserts= 1 
   missingCredentialAdapterCannotReportSuccessfulCreation                                               L443   lines=  8 asserts= 2 
   employeeApprovalWithoutCredentialAdapterWritesNoAccount                                              L455   lines= 22 asserts= 3 
   registrationCreatesAccountOnlyAfterCompanyApproval                                                   L479   lines= 35 asserts= 8 
   summaryQueryCannotOverrideBusinessThresholds                                                         L517   lines=  4 asserts= 1 
   referenceDictionaryDoesNotPretendItWasTruncatedAtFifty                                               L523   lines=  8 asserts= 2 
   grantListHasADefaultTimeWindow                                                                       L533   lines=  6 asserts= 2 
   directPositionTemplateDeleteCannotBypassArchiveAndApproval                                           L542   lines=  8 asserts= 2 
   positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval                           L553   lines= 56 asserts=10 
   companyRequestsHaveIndependentResultsAndRetryOnlyOnce                                                L655   lines= 25 asserts= 5 
   invalidAccountTypeResponseIsChinese                                                                  L683   lines= 18 asserts= 3 catch
   temporaryGrantMissingValidUntilAndUnauthenticatedMessagesAreChinese                                  L706   lines= 23 asserts= 6 catch
   invalidDecisionMessageIsChinese                                                                      L730   lines= 11 asserts= 3 catch
   platformAdminEffectivePermissionsDoNotQueryDictionaryPerPermission                                   L745   lines= 14 asserts= 3 
   orphanStaffCopyingOrganizationTemplateIsRejectedNotCrashed                                           L762   lines=  8 asserts= 2 
   staffLevelChangeRederivesPositionGrants                                                              L774   lines= 34 asserts= 3 
   nested/inner types: M006SecurityRegressionTests
   annotations: 
== 1B. 只断言异常类型、不核错误码的断言（origin/main 版；行号:原文）
142:        assertThatThrownBy(authorization::currentActor).isInstanceOf(ApiException.class);
149:        assertThatThrownBy(service::listAccessUsers).isInstanceOf(ApiException.class);
150:        assertThatThrownBy(authorization::currentProjectScopeQuietly).isInstanceOf(ApiException.class);
163:                .isInstanceOf(ApiException.class);
181:                .isInstanceOf(ApiException.class);
193:                .isInstanceOf(ApiException.class);
201:                .isInstanceOf(ApiException.class);
205:                .isInstanceOf(ApiException.class);
212:                .isInstanceOf(ApiException.class);
214:                .isInstanceOf(ApiException.class);
242:        assertThatThrownBy(() -> controller.reviewGrant("pending", request)).isInstanceOf(ApiException.class);
262:                .isInstanceOf(ApiException.class);
271:                .isInstanceOf(ApiException.class);
369:                .isInstanceOf(ApiException.class);
372:                .isInstanceOf(ApiException.class);
385:                .isInstanceOf(ApiException.class);
412:        assertThatThrownBy(() -> reviews.approve("top", request)).isInstanceOf(ApiException.class);
428:                    .isInstanceOf(ApiException.class);
448:                .isInstanceOf(ApiException.class);
473:                        "no-credential-approval", null, "approve", null, "password"))).isInstanceOf(ApiException.class);
504:                        "merchant-review", null, "approve", null, "password"))).isInstanceOf(ApiException.class);
519:                .isInstanceOf(ApiException.class);
547:        assertThatThrownBy(() -> service.deletePositionRoleTemplate("mapping")).isInstanceOf(ApiException.class);
589:                new StatusChangeRequest("in-use", "request_delete", "删除申请", List.of()))).isInstanceOf(ApiException.class);
593:                new StatusChangeRequest("ordinary-admin", "approve_delete", "同意", List.of()))).isInstanceOf(ApiException.class);
计数：bare=25 coded=4 assertDenied=5
MVN_EXIT(origin/main)=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
scratch dirty: 0
scratch removed
== 2. 主工作区现状基线（含未发布改动，非发布口径）
== 2A. 测试形状（主工作区版）
== M006SecurityRegressionTests.java: 39 tests, 823 lines
   administratorDenyWinsAndHighPermissionNeedsExplicitGrant                                             L105   lines=  9 asserts= 2 
   administratorKeepsDailyPermissionsWithoutAutomaticHighRole                                           L116   lines=  5 asserts= 0 
计数：bare=23 coded=7
MVN_EXIT(main worktree)=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
== 3. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T11:32:35Z
```

### 2.2 变异核对第一轮（/home/james/audit-tmp/m037_mutation.log；origin/main 一次性 worktree，逐个变异跑全量后端测试，列相对基线新增的红；基线红 20 条即 XM-M036-A）

```
START=2026-09-21T11:34:33Z origin/main=40fcc8f1
scratch at 40fcc8f1
temp migrations: V427__m009_project_profiles_and_associations.sql
MVN_EXIT(base)=1
  suites=263 tests=3473 red=20
  baseline seconds=47
mutated lifecycle_no_master access/AccessService.java
MVN_EXIT(lifecycle_no_master)=1
  suites=263 tests=3473 red=20
mutated review_master_node_open governance/review/SupervisorReviewService.java
MVN_EXIT(review_master_node_open)=1
  suites=263 tests=3473 red=23
    NEW-RED[review_master_node_open]: M006SecurityRegressionTests.topAuthorityReviewRequiresMasterSessionRatherThanRankAlone
    NEW-RED[review_master_node_open]: SupervisorReviewServiceTests.penaltyCapAdjustNeedsDepartmentReviewsBeforeMasterAndPublishesTerminalDecision
    NEW-RED[review_master_node_open]: SupervisorReviewServiceTests.topAuthorityReviewForwardedByMasterCanBeDecidedByForwardee
mutated staff_company_approval_open access/staff/StaffService.java
MVN_EXIT(staff_company_approval_open)=1
  suites=263 tests=3473 red=21
    NEW-RED[staff_company_approval_open]: M006SecurityRegressionTests.registrationCreatesAccountOnlyAfterCompanyApproval
mutated actor_can_grant_open access/AccessService.java
MVN_EXIT(actor_can_grant_open)=1
  suites=263 tests=3473 red=22
    NEW-RED[actor_can_grant_open]: AccessServiceTests.assignUserRoleRejectsRolePermissionsActorDoesNotHold
    NEW-RED[actor_can_grant_open]: AccessServiceTests.createTemporaryGrantRejectsGrantingPermissionActorDoesNotHold
mutated cross_merchant_write_open access/AccessService.java
MVN_EXIT(cross_merchant_write_open)=1
  suites=263 tests=3473 red=21
    NEW-RED[cross_merchant_write_open]: M006SecurityRegressionTests.organizationReadWorksButCrossMerchantWriteDoesNot
mutated widens_window_open access/AccessService.java
MVN_EXIT(widens_window_open)=1
  suites=263 tests=3473 red=21
    NEW-RED[widens_window_open]: M006SecurityRegressionTests.appliedTemplateCannotExtendTimeOrRemoveSingleUse
mutated summary_override_open access/AccessService.java
MVN_EXIT(summary_override_open)=1
  suites=263 tests=3473 red=21
    NEW-RED[summary_override_open]: M006SecurityRegressionTests.summaryQueryCannotOverrideBusinessThresholds
mutated position_in_use_open access/AccessService.java
MVN_EXIT(position_in_use_open)=1
  suites=263 tests=3473 red=21
    NEW-RED[position_in_use_open]: M006SecurityRegressionTests.positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval
scratch dirty after cleanup: 0
scratch removed
END=2026-09-21T11:41:34Z
```

### 2.3 变异核对第二轮（/home/james/audit-tmp/m037_mutation2.log；模板编辑路径两道闸各自单独拿掉）

```
START=2026-09-21T11:42:28Z origin/main=40fcc8f1
scratch at 40fcc8f1
temp migrations: V427__m009_project_profiles_and_associations.sql
MVN_EXIT(base)=1
  suites=263 tests=3473 red=20
  baseline seconds=47
mutated template_edit_no_own_gate access/AccessService.java
MVN_EXIT(template_edit_no_own_gate)=1
  suites=263 tests=3473 red=20
mutated template_edit_no_approval_gate access/AccessService.java
MVN_EXIT(template_edit_no_approval_gate)=1
  suites=263 tests=3473 red=20
scratch dirty after cleanup: 0
scratch removed
END=2026-09-21T11:44:51Z
```

### 2.4 25 处只断言异常类型的断言现在实际抛出的错误码（/home/james/audit-tmp/m037_codes.log；一次性 worktree 里把每处临时改成打印错误码）

```
scratch at 40fcc8f1
rewritten 25
MVN_EXIT(codes)=0
      1 M037CODE L142 UNAUTHENTICATED
      1 M037CODE L149 PROJECT_SCOPE_REQUIRED
      1 M037CODE L150 PROJECT_SCOPE_REQUIRED
      1 M037CODE L163 CROSS_MERCHANT_FORBIDDEN
      1 M037CODE L181 GRANT_EXCEEDS_OWN_PERMISSIONS
      1 M037CODE L193 TEMPLATE_NOT_FOUND
      1 M037CODE L201 VALIDATION_FAILED
      1 M037CODE L205 VALIDATION_FAILED
      1 M037CODE L212 INVALID_ACCOUNT_STATUS
      1 M037CODE L214 VALIDATION_FAILED
      1 M037CODE L242 PERMISSION_DENIED
      1 M037CODE L262 VALIDATION_FAILED
      1 M037CODE L271 USER_NOT_FOUND
      1 M037CODE L369 TEMPLATE_REAUTHORIZATION_REQUIRED
      1 M037CODE L372 TEMPLATE_REAUTHORIZATION_REQUIRED
      1 M037CODE L385 VALIDATION_FAILED
      1 M037CODE L412 MASTER_AUTHORITY_REQUIRED
      5 M037CODE L428 STAFF_REGISTRATION_REQUIRED
      1 M037CODE L448 CREDENTIAL_PROVIDER_UNAVAILABLE
      1 M037CODE L473 CREDENTIAL_PROVIDER_UNAVAILABLE
      1 M037CODE L504 COMPANY_APPROVAL_REQUIRED
      1 M037CODE L519 QUERY_POLICY_OVERRIDE_FORBIDDEN
      1 M037CODE L547 ARCHIVE_APPROVAL_REQUIRED
      1 M037CODE L589 POSITION_TEMPLATE_IN_USE
      1 M037CODE L593 MASTER_AUTHORITY_REQUIRED
scratch dirty: 0
scratch removed
```

### 2.5 lifecycle_no_master 变异下该处实际抛出的码（/home/james/audit-tmp/m037_equiv.log；变异后把该处断言临时收紧为核 MASTER_AUTHORITY_REQUIRED）

```
scratch at 40fcc8f1
mutated lifecycle_no_master access/AccessService.java
assertion tightened: code==MASTER_AUTHORITY_REQUIRED and message contains USB (SupervisorReviewService 的文案；AccessService 的文案是「U 盘密钥」)
MVN_EXIT(equiv)=1
   M006SecurityRegressionTests tests=1 errors=0 skipped=0 failures=1
       failure positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval | &#10;expected: &quot;MASTER_AUTHORITY_REQUIRED&quot;&#10; but was: &quot;SUPERVISOR_SELF_REVIEW_FORBIDDEN&quot;
scratch dirty: 0
scratch removed
```

逐个说明：
- review_master_node_open、staff_company_approval_open、cross_merchant_write_open、widens_window_open、summary_override_open、position_in_use_open → 本类对应测试变红：这 6 处只断言异常类型的写法在守卫被拿掉后能红。
- lifecycle_no_master（AccessService 里岗位模板删除批准/驳回的最高权限闸）→ 0 红。§2.5 实测：拿掉后该处抛的是 SUPERVISOR_SELF_REVIEW_FORBIDDEN——测试里来批准的「普通管理员」就是刚发起删除申请的那个人，被「不得自审」拦下，与最高权限无关。即这条断言在最高权限闸被拿掉后照样绿：见 CX-M037-001②。（复核服务那一层的最高权限节点闸另由 `topAuthorityReviewRequiresMasterSessionRatherThanRankAlone` 锁住，变异 review_master_node_open 能红。）
- actor_can_grant_open（把「授出不得超过自身权限」的闸整体放开）→ 只红 AccessServiceTests 的两条（装配角色、建临时授权）；本类的 `templateEditCannotGrantAnUnownedHighPermission` 不红。
- template_edit_no_own_gate、template_edit_no_approval_gate（模板编辑路径 `requireActorCanGrant(added)`、`requireGrantApprovalIfNeeded(added)` 各自单独删掉）→ 均 0 红：见 CX-M037-001。

## 3. 三线审计

- 第一性原理线。用途：把 M006 历次审计定稿的安全缺陷逐条钉成回归测试。实际行为：发布口径 39 个全绿。未经验证的前提：「每条回归测试在它所守的那道闸被拿掉时会红」——对模板编辑路径的两道闸、对 AccessService 里岗位模板删除的最高权限闸不成立（CX-M037-001）。
- 顺从式线：52 判据逐项见 §4；文件两版全读。
- 对抗式线（grilling）：Q 桩会不会把被测逻辑整个替换掉？A 被测的是 AccessService／AuthorizationService／SupervisorReviewService／StaffService 的真实实现，Mockito 只替仓储；两条岗位派生测试断言的是桩里的指针（OBS-2），证明的是「服务层带着新角色调了 replacePositionGrants」，不是库内语义。Q 只断言 ApiException 会不会因别的原因抛错而假绿？A 逐处核过：25 处里有 2 处会——模板编辑那条（两道闸互相掩护）与岗位模板删除须最高权限那处（发起人自己来批，被不得自审顶替；起初席甲以为是两层同码的等价变异，实测抛的码不同，§2.5）。Q verify(never) 能证明未写入吗？A 能证明没调仓储的写方法；写方法本身是桩，不涉库。Q 反射与字段注入与生产装配一致吗？A `registrations` 字段在生产里由 Spring 注入，测试按同一拓扑手工注入；反射调用的两个私有方法只用于取文案（OBS-3）。

## 4. 52 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BEQR-157 员工状态 | 无本面｜本模块是一个测试类，不承载、不展示「员工状态」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-182 考勤日结果 | 无本面｜本模块是一个测试类，不承载、不展示「考勤日结果」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-183 打卡方式 | 无本面｜本模块是一个测试类，不承载、不展示「打卡方式」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-184 请假类型 | 无本面｜本模块是一个测试类，不承载、不展示「请假类型」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-185 请假单状态 | 无本面｜本模块是一个测试类，不承载、不展示「请假单状态」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-186 薪资单构成 | 无本面｜本模块是一个测试类，不承载、不展示「薪资单构成」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-187 员工提成发放 | 无本面｜本模块是一个测试类，不承载、不展示「员工提成发放」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-001 整体说明：这套系统到底管什么 | 无本面｜本模块是一个测试类，不承载、不展示「整体说明：这套系统到底管什么」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 适用｜「权限分岗」的回归：显式拒绝压过管理员日常桥、高危须逐项授予、双权限复核先整体核验再消耗、最高权限事项须主会话、商户不得自批员工注册，均有测试且变异下能红（§2.2）；模板编辑路径的两道闸见 CX-M037-001｜不通过（CX-M037-001） |
| BSM-009 基础管理 | 无本面｜本模块是一个测试类，不承载、不展示「基础管理」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-028 主要功能 | 无本面｜本模块是一个测试类，不承载、不展示「主要功能」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-029 和谁有关 | 无本面｜本模块是一个测试类，不承载、不展示「和谁有关」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-073 主要功能 | 无本面｜本模块是一个测试类，不承载、不展示「主要功能」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 无本面｜本模块是一个测试类，不承载、不展示「员工手机端：排班、考勤、请假、薪资，自己手机上都能办」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-085 日常工作 | 无本面｜本模块是一个测试类，不承载、不展示「日常工作」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-086 排班、考勤与请假 | 无本面｜本模块是一个测试类，不承载、不展示「排班、考勤与请假」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-087 我的薪资 | 无本面｜本模块是一个测试类，不承载、不展示「我的薪资」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-088 员工提成、活动执行与人员服务边界 | 无本面｜本模块是一个测试类，不承载、不展示「员工提成、活动执行与人员服务边界」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 适用｜缺组织不得列全量用户、同组织可查而跨商户不可写（变异 cross_merchant_write_open 能红）、无组织员工复制他组织模板得到可定位的拒绝｜通过 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 无本面｜本模块是一个测试类，不承载、不展示「相互关系：为什么这些子系统必须连在一起」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-120 已确认规则和实施前输入项 | 无本面｜本模块是一个测试类，不承载、不展示「已确认规则和实施前输入项」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| GEN-001 必要性与最小性 | 适用｜39 个测试各对应一个 M006 编号场景，无重复｜通过 |
| GEN-002 可执行正确性 | 适用｜发布口径（origin/main 一次性 worktree）39/39 全绿；主工作区版 39/39 全绿（探针 1、2）｜通过 |
| GEN-003 测试有效性 | 适用｜origin/main 版有 25 处只断言异常类型、不核错误码（探针 1B）。25 处现在抛的都是各自所守的那个码（§2.4）；逐处核「守卫拿掉以后会不会红」：6 处经变异验证能红，其余靠紧随其后的 `verify(never)`／状态断言或单一守卫兜住。会被别的守卫顶替而照样绿的有 2 处：①`templateEditCannotGrantAnUnownedHighPermission` 用「既不持有、又须审批」的同一个码去撞两道闸，单独拿掉任何一道全量 3473 个后端测试都 0 红；②`positionTemplateLifecycle…RequiresIndependentMasterApproval` 里「普通管理员批准删除」由发起删除申请的同一个人来批，拿掉最高权限闸后改由「不得自审」抛错，照样绿（CX-M037-001）｜不通过（CX-M037-001） |
| GEN-004 信任边界校验 | 适用｜空权限、时间窗倒置、非法状态、空口令、非法账号类型、非法审批结论均有「先拒后写」的断言｜通过 |
| GEN-005 授权与最小权限 | 见 BSM-003／GEN-003：模板编辑路径的授出能力闸与审批闸各自都没有能失败的测试（CX-M037-001）｜不通过（CX-M037-001） |
| GEN-006 数据完整性 | 适用｜创建碰撞不覆盖、模板编辑不转移所有权、软删保留定义可恢复，有断言｜通过 |
| GEN-007 并发与幂等 | 适用｜幂等按组织与操作人区分首次请求、同人重试只落一次，有测试；各测试的状态都是每个测试实例自己的字段，`@AfterEach` 清认证上下文，不依赖顺序｜通过 |
| GEN-008 接口与模式一致性 | 适用｜仓储是 Mockito 桩（默认走接口默认方法，少数方法由测试内的 Map 支撑）；授权政策 JSON 为不含 deadlineWorkingDays 的旧形状，M032 后两种形状都能解析；凭证适配器、注册服务经构造器与一次字段注入装配（OBS-3）｜通过（登记 OBS-3） |
| GEN-009 错误处理与恢复 | 适用｜4 处 try/catch 只为取出异常再断言其码与文案，未吞异常｜通过 |
| GEN-010 安全与敏感信息 | 适用｜测试数据不含真实口令或个人信息｜通过 |
| GEN-011 可观测与审计证据 | 无本面｜本类不对审计事件做断言（审计留痕在 AccessServiceTests） |
| GEN-012 资源与性能边界 | 适用｜`platformAdminEffectivePermissionsDoNotQueryDictionaryPerPermission` 以读字典次数≤2 锁住 M006-035｜通过 |
| GEN-013 构建配置与依赖 | 适用｜依赖 JUnit5、AssertJ、Mockito；不连库｜通过 |
| GEN-014 数据库迁移正确性 | 无本面｜本类不涉迁移 |
| GEN-015 界面可用性与可访问性 | 适用｜三条中文文案测试断言不含拉丁字母｜通过 |
| GEN-016 文档可验证性 | 适用｜测试名与注释声称的场景逐个对照断言：除 CX-M037-001 的两条（一条名称含 Unowned 与 High 两个条件、断言分不清是哪个条件起作用；一条名称称须独立的最高权限审批、断言实际由不得自审满足）外一致｜不通过（CX-M037-001） |
| GEN-017 业务来源分类 | 适用｜未见测试自造业务规则｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到测试源码原文、探针输出、两轮变异结果与被测代码行｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 适用｜一次性来源只消耗一次、申请快照变更即拒、受益人不得自审、时间窗倒置先拒后写，均有测试｜通过 |
| PGM-008 员工和手机 | 无本面｜本模块是一个测试类，不承载、不展示「员工和手机」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | 无本面｜本模块是一个测试类，不承载、不展示「空间和点位：所有业务都要知道“发生在哪”」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-024 影响业务 | 无本面｜本模块是一个测试类，不承载、不展示「影响业务」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-026 主要功能 | 无本面｜本模块是一个测试类，不承载、不展示「主要功能」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | 无本面｜本模块是一个测试类，不承载、不展示「员工和手机：人、私人手机、工作手机要能对应起来」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-033 主要功能 | 适用｜换岗撤派生角色而保留独立装配、改职级按新映射重算派生，两条测试断言的是服务层调用 `replacePositionGrants` 后桩里的指针（OBS-2）；真库语义由 M036-001 补的测试守住｜通过（登记 OBS-2） |
| PGM-034 工作手机和企业微信 | 无本面｜本模块是一个测试类，不承载、不展示「工作手机和企业微信」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-035 提成发放和人员服务治理 | 无本面｜本模块是一个测试类，不承载、不展示「提成发放和人员服务治理」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | 无本面｜本模块是一个测试类，不承载、不展示「设备管理：谁领了什么设备，设备能不能用」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-048 商户侧：在授权业务范围内全开放 | 适用｜员工账号须经注册审批、手工建号只留外包（变异 no_staff_registration 在 M036 已验能红）、商户不得自批（变异 staff_company_approval_open 能红）｜通过 |
| PGM-059 客人与员工的个人信息保护 | 适用｜手机号摘要不落库、不进账号 JSON 有测试｜通过 |
| PGM-066 已拍板规则和待排期问题 | 适用｜「既有高危不得因处于岗位模板内而免除授出前审批」在模板编辑路径上的闸没有被单独锁住（CX-M037-001）；岗位模板删除须最高权限、占用中不得删、最高权限本人删除不再生成复核单，有测试｜不通过（CX-M037-001） |

## 5. 本次 Findings

### CX-M037-001（技术性；工程缺陷成立）两条回归测试的「只断言抛 ApiException」会被别的守卫顶替，所守的闸被拿掉也不红

**① 模板编辑（两版均成立）**

- 位置：`M006SecurityRegressionTests.java` origin/main 版 L175-183（主工作区版同文同行）：
  ```
  // M006-005：已应用模板的内容编辑不能把不持有的高危权限发出去。
  @Test void templateEditCannotGrantAnUnownedHighPermission() {
      actor("manager", "staff", "org", null);
      effect("manager", LOW, AccessRepository.Effect.GRANT, "own", false);
      PermissionTemplate t = service.createPermissionTemplate(template("create", LOW));
      assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), template("edit", HIGH)))
              .isInstanceOf(ApiException.class);
      assertThat(items.get(t.id())).extracting(PermissionTemplateItem::permissionCode).containsExactly(LOW);
  }
  ```
  被测 `AccessService.updatePermissionTemplate`（origin/main 版 L172-175）：`requireActorCanGrant(added); requireGrantApprovalIfNeeded(added);`。
- 证据：HIGH 这个码既是操作人不持有的、又是须审批的，两道闸谁先抛都满足「抛 ApiException 且模板项不变」。单独删掉第一道（template_edit_no_own_gate）或第二道（template_edit_no_approval_gate），全量 3473 个后端测试都 0 个新增变红（§2.3）；把第一道闸整体放开（actor_can_grant_open）本测试也不红（§2.2）。
- 正常触发：M006-005 当初定稿的缺陷正是「修改已应用模板既不核验授出能力也不走审批」（findings.md M006-005）。两道闸守的是两件不同的事：①模板管理员把自己不持有的日常权限加进已应用的模板（不须审批的码，只有第一道闸拦）；②模板管理员把自己持有的高危权限加进已应用的模板、直接发给已套用该模板的员工（只有第二道闸拦）。任何一道在重构中回退，现有测试都发现不了。
- 依据：GEN-003「测试须实际执行目标行为、具备能失败的断言，并覆盖本模块关键分支」；上位是治理地图 L658／L703「属于既有高危权限的，不得因处于岗位模板内而免除授出前审批」与 L660 高危授权审批五类（含「提权」）。
- ①的最小拟处置：只改这一条测试——原场景（不持有且高危）断言错误码为 `GRANT_EXCEEDS_OWN_PERMISSIONS`（第一道闸先抛）；补「不持有但不须审批的码」→ `GRANT_EXCEEDS_OWN_PERMISSIONS`；补「持有的高危码」→ `GRANT_REQUIRES_APPROVAL`；三种情形后模板项均不变。不改被测代码。两版文件打同一处补丁；发布用「origin/main 版＋补丁」。

**② 岗位模板删除须最高权限（只在 origin/main 版成立；主工作区未发布版该行已核 MASTER_AUTHORITY_REQUIRED）**

- 位置：origin/main 版 L592-593，`positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval`：
  ```
  assertThatThrownBy(() -> service.changePositionTemplateLifecycle("lifecycle",
          new StatusChangeRequest("ordinary-admin", "approve_delete", "同意", List.of()))).isInstanceOf(ApiException.class);
  ```
  被测 `AccessService.changePositionTemplateLifecycle` 的 approve_delete／reject_delete 分支（origin/main 版 L542-543）：非最高权限即抛 MASTER_AUTHORITY_REQUIRED「删除配置须由最高权限（U 盘密钥）审批」。
- 证据：变异 lifecycle_no_master 全量 0 红（§2.2）；该变异下此处实际抛 SUPERVISOR_SELF_REVIEW_FORBIDDEN（§2.5），因为来批准的就是发起申请的同一个 admin。测试名称所称的「须独立的最高权限审批」，断言并没有核到。
- 影响与边界：复核服务那一层另有最高权限节点闸且有测试锁住，AccessService 这一层回退后对外仍被拦；缺的是这一层自己的回归。
- ②的最小拟处置：该处断言改为核错误码 `MASTER_AUTHORITY_REQUIRED`（与主工作区未发布版该行的写法相同）。只动 origin/main 版这一行；主工作区版该行已是此写法，不再改。

## 6. 观察与登记（不立项）

- OBS-1：origin/main 版 25 处只断言异常类型的写法（主工作区未发布版已把其中两处改成核 MASTER_AUTHORITY_REQUIRED）。除 CX-M037-001 的两处外：6 处变异验证能红；`untrustedPrincipalIsRejected`、缺模板、缺组织等为单一守卫、无第二个抛错点；其余由紧随的 `verify(never)`／状态断言兜住。不为其余 23 处逐一改成核码立项。
- OBS-2：`positionChangeDropsDerivedRoleButKeepsIndependentAssignment`、`staffLevelChangeRederivesPositionGrants` 断言的是测试自己的桩（`replacePositionGrants` 记指针、`findPermissionEffects` 由指针算码）；能证明服务层带新角色调了仓储，库内语义由 M036-001 的真库测试守住。
- OBS-3：`withRegistrations` 用反射做一次字段注入；两条文案测试用反射调私有方法。与生产装配一致，不立项。
- W-M037-1（只在主工作区未发布版成立，不入队、不发布）：主工作区版的 26＋/12－ 依赖未发布的 `SessionIssuer.MASTER_ACCOUNT_TYPE`、`PLATFORM_ADMIN_REQUIRED`、reject_delete 后重新申请、复核排期夹具；该版在主工作区 39/39 绿，放到 origin/main 上无法编译。随该工作流一并发布。
- XM-M037-A（→M036 的 AccessServiceTests，已闭环）：变异 actor_can_grant_open 把「授出不得超过自身权限」整体放开后，全量只红装配角色、建临时授权两条；该闸的另外几处调用点（套用模板、建角色、员工单独授予、岗位路径）没有各自能失败的测试。不属本模块文件，登记待操作员决定是否重开 M036。

## 7. 结论

席甲 B1 票：**FAIL**（CX-M037-001；OBS-1…3、W-M037-1、XM-M037-A 登记）。文件两版全读、52 判据逐项、三线完成；未改任何已跟踪文件；落盘前未读 Grok 本次正文。


## M037 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-21T11:56:51Z。输入：modules/M037-b1-grok-raw.md V0.01（34828 bytes，SHA256 57ce8fc531d664a542b2e7151e6056da90d0a62ade15bb5938a184d136b98ca8）、modules/M037-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M037 B2-r1 整批候选」段（全文件 SHA256 e80c7dc97cdec168331535d9df9c1f7c3fd5515efa1fcac4e46ff21a21495941）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：CX-M037-001①→001；GR-01＋CX-M037-001②→002；Grok W-1＋W-3＋W-4→W-M037-1、W-2→W-M037-2；席甲 XM-M037-A 登记；两席其余观察全部登记；无漏并、无改写。
- 自审纠正：同一条生命周期测试里，席甲 B1 只从「只断言异常类型」这一个角度去查，看到了第三处，没有反过来从被测方法的每个分支去问「测试走没走到」，漏了非平台管理员存档与 reject_delete 两处。读 Grok 报告后补跑两个变异，全量 0 红，成立。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
