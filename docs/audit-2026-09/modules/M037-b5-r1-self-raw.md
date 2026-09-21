CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M037 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；本轮测试改动为席甲 B3 所写，且席甲 B1 在生命周期那条测试上漏了 Grok GR-01 的两处，以 Grok 票为准）。2026-09-21T12:27:50Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main `40fcc8f1`。
- 模块 M037 1 个测试文件（§1，两版全读），52 判据；B3 累计 1。
- 顺序：本席取证脚本先跑完，随后落本报告；落盘前未读 Grok 正文（modules/M037-b5-r1-grok-raw.md 存在=False）。只读承诺：未改任何已跟踪文件；一次性 worktree 已删除。
- 本轮补的方法（B1 漏了 GR-01 两处）：除了从断言出发问「守卫拿掉会不会红」，再从被测方法的每个分支出发问「有没有测试走到」。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| M006SecurityRegressionTests.java（发布变体 V0.31＝origin/main 版＋001＋002） | 835 | 60946 | c459a5d2246f05f40ea0a454fc6103e0bdf5ed07ac9cc89fb4f70e85ccd28273 | 全读 |
| M006SecurityRegressionTests.java（主工作区版 V0.31，含未发布改动＋001） | 833 | 60661 | ce3a9006cb546eb6cb0e743cbe6a47f4c7c2b979f60d41e5bd2b009b8de78f7d | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m037_b5_run.log）

```
START=2026-09-21T12:27:32Z HEAD=e456ba96 origin/main=40fcc8f1
== 0. files (lines/bytes/sha256)
主工作区版 833 60661 ce3a9006cb546eb6cb0e743cbe6a47f4c7c2b979f60d41e5bd2b009b8de78f7d | vs origin/main [ 1 file changed, 38 insertions(+), 14 deletions(-)]
发布变体 835 60946 c459a5d2246f05f40ea0a454fc6103e0bdf5ed07ac9cc89fb4f70e85ccd28273
== 1. 发布口径：origin/main 一次性 worktree＋发布变体
scratch at 40fcc8f1
scratch 改动文件： M backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java  [ 1 file changed, 29 insertions(+), 3 deletions(-)]
== 1A. 测试形状（发布变体）
== M006SecurityRegressionTests.java: 39 tests, 835 lines
   administratorDenyWinsAndHighPermissionNeedsExplicitGrant                                             L105   lines=  9 asserts= 2 
   administratorKeepsDailyPermissionsWithoutAutomaticHighRole                                           L116   lines=  5 asserts= 0 
   administratorHoldsGovernanceWritePermissionsFromDefaultRoleButNotBusinessHighRisk                    L124   lines= 13 asserts= 1 
   untrustedPrincipalIsRejected                                                                         L139   lines=  5 asserts= 1 
   missingOrganizationCannotListAllUsersOrResolveGlobalProject                                          L146   lines=  6 asserts= 2 
   organizationReadWorksButCrossMerchantWriteDoesNot                                                    L154   lines= 12 asserts= 3 
   collidingUserSlugsRemainDifferentUsers                                                               L168   lines=  6 asserts= 2 
   templateEditCannotGrantAnUnownedHighPermission                                                       L177   lines= 17 asserts= 7 
   templateEditPreservesOwnerAndRejectsMissingTemplate                                                  L196   lines=  9 asserts= 2 
   emptyPermissionsAndInvertedWindowAreRejectedBeforeGrantWrite                                         L207   lines= 11 asserts= 3 
   invalidStatusAndBlankPasswordHaveNoWrites                                                            L220   lines=  8 asserts= 3 
   manualAccountDoesNotPersistOrReturnPhoneDigest                                                       L230   lines=  5 asserts= 1 
   administratorWithoutApprovalRoleHasNoSupervisorLevel                                                 L237   lines=  3 asserts= 1 
   twoPermissionReviewConsumesOneSourceOnlyAfterBothChecksPass                                          L242   lines= 17 asserts= 4 
   credentialAdapterDisablesInvitedAccountsAndRejectsBlankPassword                                      L261   lines= 14 asserts= 3 
   developmentCredentialAdapterRejectsMissingAccount                                                    L277   lines=  7 asserts= 1 
   changedRoleCannotBeAppliedUnderAnOldApproval                                                         L286   lines= 16 asserts= 3 
   beneficiaryCannotApproveOwnGrant                                                                     L304   lines= 14 asserts= 3 
   positionChangeDropsDerivedRoleButKeepsIndependentAssignment                                          L335   lines= 35 asserts= 2 
   appliedTemplateCannotExtendTimeOrRemoveSingleUse                                                     L373   lines= 14 asserts= 3 
   pendingTemporaryGrantRejectsInvertedTimeWindow                                                       L389   lines=  9 asserts= 2 
   topAuthorityReviewRequiresMasterSessionRatherThanRankAlone                                           L400   lines= 31 asserts= 3 
   manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval                               L434   lines=  9 asserts= 2 
   accountJsonDoesNotExposeStoredPhoneDigest                                                            L445   lines=  6 asserts= 1 
   missingCredentialAdapterCannotReportSuccessfulCreation                                               L453   lines=  8 asserts= 2 
   employeeApprovalWithoutCredentialAdapterWritesNoAccount                                              L465   lines= 22 asserts= 3 
   registrationCreatesAccountOnlyAfterCompanyApproval                                                   L489   lines= 35 asserts= 8 
   summaryQueryCannotOverrideBusinessThresholds                                                         L527   lines=  4 asserts= 1 
   referenceDictionaryDoesNotPretendItWasTruncatedAtFifty                                               L533   lines=  8 asserts= 2 
   grantListHasADefaultTimeWindow                                                                       L543   lines=  6 asserts= 2 
   directPositionTemplateDeleteCannotBypassArchiveAndApproval                                           L552   lines=  8 asserts= 2 
   positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval                           L563   lines= 72 asserts=16 
   companyRequestsHaveIndependentResultsAndRetryOnlyOnce                                                L681   lines= 25 asserts= 5 
   invalidAccountTypeResponseIsChinese                                                                  L709   lines= 18 asserts= 3 catch
   temporaryGrantMissingValidUntilAndUnauthenticatedMessagesAreChinese                                  L732   lines= 23 asserts= 6 catch
   invalidDecisionMessageIsChinese                                                                      L756   lines= 11 asserts= 3 catch
   platformAdminEffectivePermissionsDoNotQueryDictionaryPerPermission                                   L771   lines= 14 asserts= 3 
   orphanStaffCopyingOrganizationTemplateIsRejectedNotCrashed                                           L788   lines=  8 asserts= 2 
   staffLevelChangeRederivesPositionGrants                                                              L800   lines= 34 asserts= 3 
   nested/inner types: M006SecurityRegressionTests
   annotations: 
只断言异常类型：bare=23 coded=9
MVN_EXIT(publish)=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
scratch dirty after cleanup: 0
scratch removed
== 2. 主工作区现状对照（含未发布改动，非发布口径）
MVN_EXIT(main worktree)=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
== 3. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T12:27:45Z
```

## 3. 三线审计

- 第一性原理线：用途/实际行为同 B1。修后：模板编辑两道闸、生命周期三处的回归各自能红。
- 顺从式线：52 判据逐项见 §4。
- 对抗式线（grilling）：Q 001 往字典里加第三码会不会影响别的测试？A `when(repository.listPermissions())` 的改写只在该测试实例内，字段每个测试一份。Q 002 的 staff 主体失败会不会留下状态？A 失败在幂等执行之前抛出，未调任何写方法（已 verify），随后切回平台管理员走原流程。Q 驳回后再申请用新幂等键吗？A 是（request-again）。Q 从被测方法分支反查还有没有没走到的？A `changePositionTemplateLifecycle` 的 archive、request_delete（占用拒绝、正常、最高权限本人直删）、approve_delete、reject_delete、restore 现都有调用；INVALID_STATE 与 INVALID_ACTION 两个输入错误分支无测试，属无安全含义的参数校验，登记不立项。无新缺陷、无添加项、无 HTML 问题。

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
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 适用｜「权限分岗」的回归：显式拒绝压过管理员日常桥、高危须逐项授予、双权限复核先整体核验再消耗、最高权限事项须主会话、商户不得自批员工注册、模板编辑的两道闸、岗位模板生命周期的平台管理员闸与最高权限闸，均有测试且各自变异下能红（探针；B1 §2.2；B3 日志）｜通过 |
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
| GEN-003 测试有效性 | 适用｜B1 的 12 个变异加 B3 的 5 个 RED：模板编辑两道闸、生命周期三处现各自能红（001、002）；其余 23 处只断言异常类型的写法仍由变异验证、`verify(never)`／状态断言或单一守卫兜住（登记）｜通过（登记） |
| GEN-004 信任边界校验 | 适用｜空权限、时间窗倒置、非法状态、空口令、非法账号类型、非法审批结论均有「先拒后写」的断言｜通过 |
| GEN-005 授权与最小权限 | 适用｜见 BSM-003：模板编辑路径的授出能力闸与审批闸现各有能失败的断言（001）｜通过 |
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
| GEN-016 文档可验证性 | 适用｜测试名与注释声称的场景与断言逐个对照一致：模板编辑那条现分清「不持有」与「须审批」两个条件；生命周期那条现真正核到「须独立的最高权限审批」｜通过 |
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
| PGM-066 已拍板规则和待排期问题 | 适用｜「既有高危不得因处于岗位模板内而免除授出前审批」在模板编辑路径上现由 001 单独锁住；岗位模板删除须最高权限、可驳回后再申请、占用中不得删、最高权限本人删除不再生成复核单，有测试（002）｜通过 |

## 5. 已定稿 001、002 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。W-M037-1…2、XM-M037-A 登记未变。

## 6. 本次 Findings

无新增。

## 7. 结论

席甲 B5-r1 票：**PASS**。1 个文件两版全读、52 判据逐项、取证脚本实跑复核；未改任何已跟踪文件；落盘前未读 Grok 本次正文。
