CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M035 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；本轮测试改动与 V430 为席甲 B3 所写，且席甲 B1 漏了 GR-03，以 Grok 票为准）。2026-09-21T09:57:21Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main `c3a48ccd`。
- 模块 M035 2 个测试文件＋本轮新增 V430（§1 全读），52 判据；B3 累计 1。complex_test 现 Flyway 最高 430。
- 顺序：本席取证脚本先跑完，随后落本报告；落盘前未读 Grok 正文（modules/M035-b5-r1-grok-raw.md 存在=False）。只读承诺：未改任何已跟踪文件；真库测试为类级事务回滚。
- 本轮补的方法（B1 漏了 GR-03）：测试里的每条 SQL、正则与过滤条件都逐字读，凡「先挖掉某样东西再断言」的写法，拿被挖掉的那样东西去对 HTML 原文。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| AccessControllerTests.java | 400 | 20215 | aa510618f93abe513d0429f4e630735b0e4732fbf854baa36ef0953e2964dff6 | 全读 |
| AccessJdbcIntegrationTests.java | 875 | 45515 | 103a81c16e60e8a483689d8c758feab6d68e19ac9f8a9ee2ffe2f5b45d2259c0 | 全读 |
| V430__permission_names_no_latin_ai.sql | 8 | 693 | d3229b9c7223127afb44b4527a4e7362c6eb7571e51cd84e1fea2dc11fc7bb47 | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m035_b5_run.log）

```
START=2026-09-21T09:55:53Z HEAD=e456ba96 origin/main=c3a48ccd
== 0. files (lines/bytes/sha256 | vs origin/main)
AccessControllerTests.java 400 20215 aa510618f93abe513d0429f4e630735b0e4732fbf854baa36ef0953e2964dff6 | [ 1 file changed, 73 insertions(+)]
AccessJdbcIntegrationTests.java 875 45515 103a81c16e60e8a483689d8c758feab6d68e19ac9f8a9ee2ffe2f5b45d2259c0 | [ 1 file changed, 7 insertions(+), 5 deletions(-)]
V430__permission_names_no_latin_ai.sql 8 693 d3229b9c7223127afb44b4527a4e7362c6eb7571e51cd84e1fea2dc11fc7bb47 | []
== 1. 测试形状与控制器映射覆盖
== AccessControllerTests: 16 tests
   unauthenticatedAccessEndpointReturnsUnauthorizedOrForbidden                                     lines= 10 asserts= 1 
   createTemplateWithoutPermissionReturnsPermissionDenied                                          lines= 20 asserts= 2 
   listPermissionsReturnsApiResponsePageShape                                                      lines= 25 asserts= 5 
   createPermissionTemplateMissingIdempotencyKeyReturnsValidationFailed                            lines= 15 asserts= 2 
   assignUserRoleWithoutRoleAssignPermissionReturnsPermissionDenied                                lines= 20 asserts= 2 
   createTemporaryGrantMissingValidUntilReturnsValidationFailed                                    lines= 17 asserts= 2 
   userPermissionOverrideEndpointGatesOnRoleAssignAndReturnsEffectivePermissions                   lines= 42 asserts= 6 
   listStaffLevelsReturnsApiResponsePageShape                                                      lines= 17 asserts= 5 
   assignStaffLevelReturnsOkWithAssignment                                                         lines= 24 asserts= 5 
   assignStaffLevelMissingIdempotencyKeyReturnsValidationFailed                                    lines= 14 asserts= 2 
   effectivePermissionsSelfReturnsGrantsDeniesAndWildcard                                          lines= 21 asserts= 7 
   userEffectivePermissionsWithoutUserViewPermissionReturnsPermissionDenied                        lines= 12 asserts= 2 
   userEffectivePermissionsReturnsTargetUserSetWithUserViewPermission                              lines= 20 asserts= 5 
   positionTemplateLifecycleGatesDeleteDecisionsOnBothPermissions                                  lines= 26 asserts= 6 
   grantReviewRequiresBothReviewAndSupervisorApprovePermissions                                    lines= 18 asserts= 3 
   staffLevelAndPositionEndpointsUseThePathUserIdNotTheBodyUserId                                  lines= 43 asserts= 6 
== AccessJdbcIntegrationTests: 26 tests
   migrationCreatesPermissionTablesAndSeedsAccessAndOpsPermissions                                 lines= 21 asserts= 5 
   v181MovesOutsourcedFacilityPermissionsToTimeBoxTemplatesWithoutRuntimeRolePermissions           lines= 83 asserts=10 
   v181SeedsConstructionCompletionUploadAndKeepsRuntimeRoleTimeBoxed                               lines= 63 asserts= 6 
   v180SeedsAuditorRolesTemplatesAsReadOnlyWithoutChangingApprovalFlags                            lines= 53 asserts= 8 
   auditorReadOnlyAccessAndTemporaryGrantExpiryUseExistingAuthorizationEffects                     lines= 78 asserts= 0 
   outsourcedTemporaryGrantExpiryUsesExistingAuthorizationEffects                                  lines= 61 asserts= 0 
   migrationCreatesStaffLevelTablesAndSeedsThirtyContiguousRanks                                   lines= 17 asserts= 6 
   staffLevelAssignmentPersistsLevelAndSalary                                                      lines= 15 asserts= 1 
   saveTemplatePersistsGrantAndDenyItems                                                           lines= 19 asserts= 2 
   copyTemplateStoresSnapshotIndependentFromLaterSourceChanges                                     lines= 18 asserts= 1 
   appliedTemporaryTemplateCanBeReadAsActivePermissionEffect                                       lines= 31 asserts= 2 
   revokingTemporaryGrantIdRevokesEveryPermissionCodeInGrantGroup                                  lines= 53 asserts= 6 
   lapsedTemporaryGrantIsExpiredAndLoggedWhenEffectsAreRead                                        lines= 34 asserts= 4 
   lapsedTemplateApplicationIsExpiredAndLoggedWhenEffectsAreRead                                   lines= 38 asserts= 3 
   storedGrantPolicyParsesAndSavingKeepsDeadlineWorkingDays                                        lines= 20 asserts= 3 
   manualOverridePermissionIsRetiredAndGrantedToNobody                                             lines= 15 asserts= 2 
   positionAndStaffLevelEntryPointsDeclareATransaction                                             lines= 13 asserts= 2 
   seededDisplayNamesAreChinese                                                                    lines= 14 asserts= 3 
   userPermissionOverridesAreUpsertedReadAsEffectsAndRemovable                                     lines= 22 asserts= 3 
   moneyAndSensitiveExportPermissionsRequireApproval                                               lines= 12 asserts= 1 
   scopeChangeAndHighRiskReleaseRequestTypesAreAcceptedByTheGrantRequestCheck                      lines= 17 asserts= 2 
   revokeTemplateApplicationsMarksOnlyTheGivenActiveApplicationsRevoked                            lines= 20 asserts= 4 
   sensitiveBusinessDataPermissionsRequireApprovalAndStayOutOfThePlatformAdminBridge               lines= 12 asserts= 2 
   dailyCodesReachThePlatformAdminBridgeAndRiskTiersAreTheFourGovernedOnes                         lines= 20 asserts= 4 
   fiveCategoryHighRiskCodesRequireApprovalBeforeGrant                                             lines= 11 asserts= 2 
   platformAdminRoleSeedYieldsGovernanceWriteCodesButNotBusinessHighRisk                           lines= 71 asserts= 3 
== controller mappings=33; exercised by AccessControllerTests=11
   NOT exercised: GET /permission-templates
   NOT exercised: PUT /permission-templates/{id}
   NOT exercised: POST /permission-templates/{id}/apply
   NOT exercised: POST /permission-templates/{id}/copy
   NOT exercised: GET /roles
   NOT exercised: POST /roles
   NOT exercised: POST /role-assignments/unassign
   NOT exercised: GET /grant-policy
   NOT exercised: POST /grant-policy
   NOT exercised: POST /grant-requests
   NOT exercised: GET /grant-requests
   NOT exercised: POST /grant-requests/{id}/review
   NOT exercised: GET /grant-requests/summary
   NOT exercised: GET /position-role-templates
   NOT exercised: POST /position-role-templates
   NOT exercised: PUT /position-role-templates/{id}
   NOT exercised: POST /position-role-templates/{id}/lifecycle
   NOT exercised: DELETE /position-role-templates/{id}
   NOT exercised: POST /temporary-grants/{id}/revoke
   NOT exercised: GET /users
   NOT exercised: POST /users
   NOT exercised: PUT /users/{id}/scope
== 2. 关键门控引用
requirePermissions(双码) 在 AccessControllerTests: 3 处；ArgumentCaptor: 2 处；成功路径 verify 门控码: 3 处
展示名测试 SQL: 681: WHERE permission_name ~ '[A-Za-z]'
== 3. 实跑
MVN_EXIT=0
   AccessControllerTests tests=16 errors=0 skipped=0 failures=0
   AccessJdbcIntegrationTests tests=26 errors=0 skipped=0 failures=0
== 4. 库内展示名终态
flyway_top=430 latin_permission_names=0 latin_role_names=0 latin_template_names=0 analytics.risk_alert.view=查看经营分析风险预警和人工智能质量台账 wecom.ai_qc.manage=管理企微人工智能语义质检开关和适配器 
== 5. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T09:55:59Z
```

读数说明：上面「exercised by AccessControllerTests=11」是工具的读数——工具只把 `usr_…` 形状的路径段归一成 `{id}`，`prt_1`、`agr_1` 没归一，所以把 `POST /position-role-templates/{id}/lifecycle` 与 `POST /grant-requests/{id}/review` 误列进了 NOT exercised。按测试文件里实际出现的请求路径逐条数，调到的映射是 13 个（B1 时 10 个，本轮新增生命周期、审批、定岗三个），未调到 20 个。

## 3. 三线审计

- 第一性原理线：用途/实际行为同 B1。修后：控制器带分支或安全含义的门控有测试锁住；展示名测试对任何拉丁字母都红；事务测试名实相符。
- 顺从式线：52 判据逐项见 §4。
- 对抗式线（grilling）：逐字再扫一遍两个文件里「先过滤再断言」的写法——`v180…ReadOnly` 的只读正则以后缀近似写操作（登记，种子现态下成立）；`dailyCodesReach…` 等按风险档/审批位过滤，是被测规则本身不是豁免；未再见把某类值挖掉的断言。新增三条控制器测试在被测代码变坏时红（变异核对）；V430 撤掉则展示名测试红；其余同 B1。无新缺陷、无添加项、无 HTML 问题。

## 4. 52 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BEQR-157 员工状态 | 无本面｜本模块是两个测试类，不承载、不展示「员工状态」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-182 考勤日结果 | 无本面｜本模块是两个测试类，不承载、不展示「考勤日结果」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-183 打卡方式 | 无本面｜本模块是两个测试类，不承载、不展示「打卡方式」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-184 请假类型 | 无本面｜本模块是两个测试类，不承载、不展示「请假类型」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-185 请假单状态 | 无本面｜本模块是两个测试类，不承载、不展示「请假单状态」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-186 薪资单构成 | 无本面｜本模块是两个测试类，不承载、不展示「薪资单构成」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BEQR-187 员工提成发放 | 无本面｜本模块是两个测试类，不承载、不展示「员工提成发放」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-001 整体说明：这套系统到底管什么 | 无本面｜本模块是两个测试类，不承载、不展示「整体说明：这套系统到底管什么」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 适用｜「权限分岗」的门控：除原有 4 个端点的「无权限即 403」外，两处双权限门控（岗位模板删除的批准/驳回、授权申请审批）现有测试锁住，三条成功路径测试也校验了各自的门控码（002；变异核对能红）｜通过 |
| BSM-009 基础管理 | 无本面｜本模块是两个测试类，不承载、不展示「基础管理」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-028 主要功能 | 无本面｜本模块是两个测试类，不承载、不展示「主要功能」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-029 和谁有关 | 无本面｜本模块是两个测试类，不承载、不展示「和谁有关」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-073 主要功能 | 无本面｜本模块是两个测试类，不承载、不展示「主要功能」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 无本面｜本模块是两个测试类，不承载、不展示「员工手机端：排班、考勤、请假、薪资，自己手机上都能办」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-085 日常工作 | 无本面｜本模块是两个测试类，不承载、不展示「日常工作」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-086 排班、考勤与请假 | 无本面｜本模块是两个测试类，不承载、不展示「排班、考勤与请假」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-087 我的薪资 | 无本面｜本模块是两个测试类，不承载、不展示「我的薪资」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-088 员工提成、活动执行与人员服务边界 | 无本面｜本模块是两个测试类，不承载、不展示「员工提成、活动执行与人员服务边界」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 适用｜职级与定岗端点以路径里的账号为准、请求体 user_id 不生效，现有 ArgumentCaptor 测试锁住（002）｜通过 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 无本面｜本模块是两个测试类，不承载、不展示「相互关系：为什么这些子系统必须连在一起」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-120 已确认规则和实施前输入项 | 无本面｜本模块是两个测试类，不承载、不展示「已确认规则和实施前输入项」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| GEN-001 必要性与最小性 | 适用｜39 个测试（13＋26）无重复场景；2 个不含 `assertThat` 的真库测试用的是「`requirePermission` 不抛即通过＋`assertPermissionDenied`」的写法，不是空断言（探针 1 的断言计数只认常见断言名，已人工核对）｜通过 |
| GEN-002 可执行正确性 | 适用｜两个测试类实跑 16/16、26/26 全绿（探针）｜通过 |
| GEN-003 测试有效性 | 适用｜控制器测试现 16 个：新增生命周期条件门控、审批双权限、路径 id 覆盖三条，变异核对三条全红；展示名测试现对任何拉丁字母都红（001，RED 曾抓到两条含「AI」的权限名）；事务测试的名称与注释已与其断言一致（003）。其余未调到的映射为无分支单权限门控加透传，不铺矩阵（登记）｜通过 |
| GEN-004 信任边界校验 | 适用｜缺幂等键、缺失效时间的 400 有测试；其余必填校验在服务层测试（M036）｜通过 |
| GEN-005 授权与最小权限 | 适用｜见 BSM-003／BSM-114：双权限门控与路径 id 覆盖现有测试锁住｜通过 |
| GEN-006 数据完整性 | 适用｜到期判定的真库测试（L556-626）用固定时刻造数据，并把判定时刻显式传给 `findPermissionEffects(userId, checkedAt)`，结果不随运行当天变化｜通过 |
| GEN-007 并发与幂等 | 适用｜真库类为类级 `@Transactional`，每个测试结束回滚，不留残留、不依赖执行顺序；`staffLevelChangeRunsInOneTransactionLikePositionAssignment` 只断言注解存在（M006-G-07 的既定写法；与 XM-M031-B 的关系已在 M031 登记）｜通过 |
| GEN-008 接口与模式一致性 | 适用｜真库测试直接打 complex_test 的真实表与种子（字典、角色、模板、职级、授权政策行），不是内存夹具；`@SpringBootTest` 里数据源写死 127.0.0.1:5432/complex_test、口令取环境变量｜通过 |
| GEN-009 错误处理与恢复 | 适用｜未见吞异常的 try/catch（探针 1 无 catch 标记）｜通过 |
| GEN-010 安全与敏感信息 | 适用｜测试数据不含真实口令或个人信息｜通过 |
| GEN-011 可观测与审计证据 | 适用｜到期/单次消耗写用量日志的断言在真库测试里｜通过 |
| GEN-012 资源与性能边界 | 无本面｜测试无性能面 |
| GEN-013 构建配置与依赖 | 适用｜依赖 JUnit5、AssertJ、Mockito、Spring Test；真库类需 complex_test 可达｜通过 |
| GEN-014 数据库迁移正确性 | 适用｜真库测试断言的是迁移叠加后的库内终态（含 V429、V430）；V430 为前向迁移，已应用迁移未回改｜通过 |
| GEN-015 界面可用性与可访问性 | 适用｜展示名测试不再给「AI」豁免，库内权限名/角色名/模板名含拉丁字母均 0（V430 后；探针终态）｜通过 |
| GEN-016 文档可验证性 | 适用｜测试名与注释声称的场景与实际断言一致；`positionAndStaffLevelEntryPointsDeclareATransaction` 如实写明只断言事务声明；`userEffectivePermissionsReturns…` 的桩 wildcard=true 仍为现实里不会出现的值（登记）｜通过（登记） |
| GEN-017 业务来源分类 | 适用｜未见测试自造业务规则｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到测试源码原文、探针输出与实跑结果｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 适用｜到期、单次消耗留痕有真库测试（lapsed* 两条）；授权政策审批时限键保留有真库测试（M032-001）｜通过 |
| PGM-008 员工和手机 | 无本面｜本模块是两个测试类，不承载、不展示「员工和手机」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | 无本面｜本模块是两个测试类，不承载、不展示「空间和点位：所有业务都要知道“发生在哪”」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-024 影响业务 | 无本面｜本模块是两个测试类，不承载、不展示「影响业务」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-026 主要功能 | 无本面｜本模块是两个测试类，不承载、不展示「主要功能」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | 无本面｜本模块是两个测试类，不承载、不展示「员工和手机：人、私人手机、工作手机要能对应起来」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-033 主要功能 | 适用｜职级 30 档连续、职级与薪资落库有真库测试；定岗/范围变更的服务层行为在 AccessServiceTests（M036）｜通过 |
| PGM-034 工作手机和企业微信 | 无本面｜本模块是两个测试类，不承载、不展示「工作手机和企业微信」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-035 提成发放和人员服务治理 | 无本面｜本模块是两个测试类，不承载、不展示「提成发放和人员服务治理」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | 无本面｜本模块是两个测试类，不承载、不展示「设备管理：谁领了什么设备，设备能不能用」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| PGM-048 商户侧：在授权业务范围内全开放 | 适用｜外包与外部审计的限时模板、到期后拒绝有真库测试（v181* 两条、auditor*/outsourced* 两条）｜通过 |
| PGM-059 客人与员工的个人信息保护 | 无本面｜两个测试类不涉个人信息字段的读写断言 |
| PGM-066 已拍板规则和待排期问题 | 适用｜人工放行权限码已停用且无授予来源（M034-001）、五类高危须审批、平台管理员角色种子只给治理写码与日常码，均有真库断言｜通过 |

## 5. 已定稿 001、002、003 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。XM-M035-A 登记未变。

## 6. 本次 Findings

无新增。

## 7. 结论

席甲 B5-r1 票：**PASS**。3 文件全读、52 判据逐项、取证脚本实跑复核；未改任何已跟踪文件；落盘前未读 Grok 本次正文。
