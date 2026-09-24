CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M035 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；AccessJdbcIntegrationTests 里 M032-001、M034-001 两条回归为席甲本轮所写）。2026-09-21T09:10:35Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main c3a48ccd。
- 模块 M035「功能权限点、职级三十档、权限模板与最终权限计算／后台验证／第1段」2 个测试文件 1200 行，52 判据。B3 累计 0。两个文件与 origin/main 逐字节相同。
- 顺序：Grok 已先派（modules/M035-b1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件；真库测试为类级事务回滚。
- 方法（测试模块）：逐个测试问「把被测实现改坏，它会不会红」；再反过来从被测控制器的每个带分支或安全含义的门控出发，问「有没有哪个测试会因它变坏而红」。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| AccessControllerTests.java | 327 | 15455 | 466b780026d8bab9c814f5a6269fb76bfd0755c520cba5f1797778bc7a30ea0e | 全读 |
| AccessJdbcIntegrationTests.java | 873 | 45407 | 291e657bc4363c3b202b08c3b02b88de032911d46f762fdb19828596d898ff96 | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m035_b1_run.log）

```
START=2026-09-21T09:08:28Z HEAD=e456ba96 origin/main=c3a48ccd
== 0. files (lines/bytes/sha256 | status | vs origin/main)
AccessControllerTests.java 327 15455 466b780026d8bab9c814f5a6269fb76bfd0755c520cba5f1797778bc7a30ea0e | [] | []
AccessJdbcIntegrationTests.java 873 45407 291e657bc4363c3b202b08c3b02b88de032911d46f762fdb19828596d898ff96 | [ M] | []
== 1. 测试形状与控制器映射覆盖
== AccessControllerTests: 13 tests
   unauthenticatedAccessEndpointReturnsUnauthorizedOrForbidden                                     lines= 10 asserts= 1 
   createTemplateWithoutPermissionReturnsPermissionDenied                                          lines= 20 asserts= 2 
   listPermissionsReturnsApiResponsePageShape                                                      lines= 24 asserts= 4 
   createPermissionTemplateMissingIdempotencyKeyReturnsValidationFailed                            lines= 15 asserts= 2 
   assignUserRoleWithoutRoleAssignPermissionReturnsPermissionDenied                                lines= 20 asserts= 2 
   createTemporaryGrantMissingValidUntilReturnsValidationFailed                                    lines= 17 asserts= 2 
   userPermissionOverrideEndpointGatesOnRoleAssignAndReturnsEffectivePermissions                   lines= 42 asserts= 6 
   listStaffLevelsReturnsApiResponsePageShape                                                      lines= 16 asserts= 4 
   assignStaffLevelReturnsOkWithAssignment                                                         lines= 23 asserts= 4 
   assignStaffLevelMissingIdempotencyKeyReturnsValidationFailed                                    lines= 14 asserts= 2 
   effectivePermissionsSelfReturnsGrantsDeniesAndWildcard                                          lines= 21 asserts= 7 
   userEffectivePermissionsWithoutUserViewPermissionReturnsPermissionDenied                        lines= 12 asserts= 2 
   userEffectivePermissionsReturnsTargetUserSetWithUserViewPermission                              lines= 37 asserts= 5 
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
   manualOverridePermissionIsRetiredAndGrantedToNobody                                             lines= 14 asserts= 2 
   staffLevelChangeRunsInOneTransactionLikePositionAssignment                                      lines= 12 asserts= 2 
   seededDisplayNamesAreChinese                                                                    lines= 14 asserts= 3 
   userPermissionOverridesAreUpsertedReadAsEffectsAndRemovable                                     lines= 22 asserts= 3 
   moneyAndSensitiveExportPermissionsRequireApproval                                               lines= 12 asserts= 1 
   scopeChangeAndHighRiskReleaseRequestTypesAreAcceptedByTheGrantRequestCheck                      lines= 17 asserts= 2 
   revokeTemplateApplicationsMarksOnlyTheGivenActiveApplicationsRevoked                            lines= 20 asserts= 4 
   sensitiveBusinessDataPermissionsRequireApprovalAndStayOutOfThePlatformAdminBridge               lines= 12 asserts= 2 
   dailyCodesReachThePlatformAdminBridgeAndRiskTiersAreTheFourGovernedOnes                         lines= 20 asserts= 4 
   fiveCategoryHighRiskCodesRequireApprovalBeforeGrant                                             lines= 11 asserts= 2 
   platformAdminRoleSeedYieldsGovernanceWriteCodesButNotBusinessHighRisk                           lines= 71 asserts= 3 
== controller mappings=33; exercised by AccessControllerTests=10
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
   NOT exercised: PUT /users/{id}/position
   NOT exercised: PUT /users/{id}/scope
== 2. 关键门控分支在全部测试里的引用
lifecycle 端点: 0 个测试文件
grant-requests review 端点: 0 个测试文件
requirePermissions(双码) 的 verify/doThrow: 0 处
ArgumentCaptor 校验路径 id 覆盖请求体 user_id: 0 处
== 3. 实跑
MVN_EXIT=0
   AccessControllerTests tests=13 errors=0 skipped=0 failures=0
   AccessJdbcIntegrationTests tests=26 errors=0 skipped=0 failures=0
   AuthorizationServiceTests tests=24 errors=0 skipped=0 failures=0
   TicketingControllerTests tests=20 errors=0 skipped=0 failures=0
== 4. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T09:08:34Z
```

说明：探针 1 的「controller mappings=33」是方法级映射数（类级 `@RequestMapping` 不计）；断言计数只认 assertThat/andExpect/verify/assertThatThrownBy/assertThatCode，两个计数为 0 的真库测试实际用 `authorizationService.requirePermission(...)` 不抛与自带的 `assertPermissionDenied` 助手断言，已人工核对不是空测试。探针 3 输出里 AuthorizationServiceTests、TicketingControllerTests 两行是上一轮留在 target/surefire-reports 的旧报告，不是本次运行。

## 3. 三线审计

- 第一性原理线。用途：守住权限后台的接口层门控（AccessControllerTests）与仓储/字典/种子的库内终态（AccessJdbcIntegrationTests）。实际行为：39 个测试全绿，断言均能失败。未经验证的前提：「控制器上带分支或安全含义的门控都有测试守着」——不成立，三处为 0（CX-M035-001）。
- 顺从式线：52 判据逐项见 §4；两个文件全读。
- 对抗式线（grilling）：把生命周期端点 approve_delete 的双权限改成单权限→无测试变红；把授权审批端点的第二个权限码删掉→无测试变红；把职级端点改成采信请求体 user_id→无测试变红（现有 `assignStaffLevelReturnsOkWithAssignment` 不捕获入参）；把建模板端点的门控删掉→`createTemplateWithoutPermission…` 变红（有守）；把到期清理改坏→lapsed* 两条变红（有守）；把 V429 撤掉→`manualOverridePermissionIsRetired…` 变红（有守）。

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
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 适用｜「权限分岗」的门控在控制器层；AccessControllerTests 只对 4 个端点验证了「无权限即 403」（建模板、装配角色、员工级覆盖、查他人有效权限），两处「须同时持两个权限码」的门控（岗位模板删除的批准/驳回、授权申请审批）没有任何测试（CX-M035-001）｜不通过（CX-M035-001） |
| BSM-009 基础管理 | 无本面｜本模块是两个测试类，不承载、不展示「基础管理」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-028 主要功能 | 无本面｜本模块是两个测试类，不承载、不展示「主要功能」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-029 和谁有关 | 无本面｜本模块是两个测试类，不承载、不展示「和谁有关」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-073 主要功能 | 无本面｜本模块是两个测试类，不承载、不展示「主要功能」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 无本面｜本模块是两个测试类，不承载、不展示「员工手机端：排班、考勤、请假、薪资，自己手机上都能办」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-085 日常工作 | 无本面｜本模块是两个测试类，不承载、不展示「日常工作」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-086 排班、考勤与请假 | 无本面｜本模块是两个测试类，不承载、不展示「排班、考勤与请假」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-087 我的薪资 | 无本面｜本模块是两个测试类，不承载、不展示「我的薪资」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-088 员工提成、活动执行与人员服务边界 | 无本面｜本模块是两个测试类，不承载、不展示「员工提成、活动执行与人员服务边界」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 适用｜「只做自己该做的」：职级与定岗端点以路径里的账号为准、忽略请求体里的 user_id（AccessController L262-300），该行为无测试（CX-M035-001）｜不通过（CX-M035-001） |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 无本面｜本模块是两个测试类，不承载、不展示「相互关系：为什么这些子系统必须连在一起」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| BSM-120 已确认规则和实施前输入项 | 无本面｜本模块是两个测试类，不承载、不展示「已确认规则和实施前输入项」的对象或界面文字；其被测对象的相应判据已在 M031–M034 审过 |
| GEN-001 必要性与最小性 | 适用｜39 个测试（13＋26）无重复场景；2 个不含 `assertThat` 的真库测试用的是「`requirePermission` 不抛即通过＋`assertPermissionDenied`」的写法，不是空断言（探针 1 的断言计数只认常见断言名，已人工核对）｜通过 |
| GEN-002 可执行正确性 | 适用｜两个测试类实跑 13/13、26/26 全绿（探针 3；日志里另两行是上一轮残留的 surefire 报告，非本次运行）｜通过 |
| GEN-003 测试有效性 | 适用｜控制器 33 个方法级映射里 AccessControllerTests 调到 10 个（探针 1）；未覆盖的 23 个里有三处是带分支或安全含义的门控——①岗位模板生命周期端点按 action 分两种权限要求（approve_delete/reject_delete 须同时持 `access.role.assign` 与 `governance.supervisor_review.approve`）；②授权申请审批须同时持 `access.grant_request.review` 与 `governance.supervisor_review.approve`；③职级、定岗端点用路径 id 覆盖请求体 user_id。三处在全部测试里 0 处引用（探针 2）。把这三处实现改坏（例如把双权限改成单权限、改成采信请求体 user_id），现有测试全绿（CX-M035-001）｜不通过（CX-M035-001） |
| GEN-004 信任边界校验 | 适用｜缺幂等键、缺失效时间的 400 有测试；其余必填校验在服务层测试（M036）｜通过 |
| GEN-005 授权与最小权限 | 见 GEN-003／BSM-003：双权限门控与路径 id 覆盖无测试（CX-M035-001）｜不通过（CX-M035-001） |
| GEN-006 数据完整性 | 适用｜到期判定的真库测试（L556-626）用固定时刻造数据，并把判定时刻显式传给 `findPermissionEffects(userId, checkedAt)`，结果不随运行当天变化｜通过 |
| GEN-007 并发与幂等 | 适用｜真库类为类级 `@Transactional`，每个测试结束回滚，不留残留、不依赖执行顺序；`staffLevelChangeRunsInOneTransactionLikePositionAssignment` 只断言注解存在（M006-G-07 的既定写法；与 XM-M031-B 的关系已在 M031 登记）｜通过 |
| GEN-008 接口与模式一致性 | 适用｜真库测试直接打 complex_test 的真实表与种子（字典、角色、模板、职级、授权政策行），不是内存夹具；`@SpringBootTest` 里数据源写死 127.0.0.1:5432/complex_test、口令取环境变量｜通过 |
| GEN-009 错误处理与恢复 | 适用｜未见吞异常的 try/catch（探针 1 无 catch 标记）｜通过 |
| GEN-010 安全与敏感信息 | 适用｜测试数据不含真实口令或个人信息｜通过 |
| GEN-011 可观测与审计证据 | 适用｜到期/单次消耗写用量日志的断言在真库测试里｜通过 |
| GEN-012 资源与性能边界 | 无本面｜测试无性能面 |
| GEN-013 构建配置与依赖 | 适用｜依赖 JUnit5、AssertJ、Mockito、Spring Test；真库类需 complex_test 可达｜通过 |
| GEN-014 数据库迁移正确性 | 适用｜真库测试断言的是迁移叠加后的库内终态（V180/V181/V387/V393/V394/V429 等）｜通过 |
| GEN-015 界面可用性与可访问性 | 无本面｜测试内的英文桩文案（"permission denied"）不是界面文字 |
| GEN-016 文档可验证性 | 适用｜测试名与注释声称的场景与实际断言一致；`effectivePermissionsSelfReturnsGrantsDeniesAndWildcard` 名称含 Wildcard、断言 wildcard=false，与实现现状一致；`userEffectivePermissionsReturns…` 的桩返回 wildcard=true 是现实里不会出现的值（OBS-1）｜通过（登记 OBS-1） |
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

## 5. 本次 Findings

### CX-M035-001（细化；工程缺陷成立）控制器三处带分支或安全含义的门控零测试

- 位置：`AccessControllerTests.java`（整类 13 个测试）；被测 `AccessController.java:148-156`（授权申请审批：`requirePermissions("access.grant_request.review", "governance.supervisor_review.approve")`）、`:192-202`（岗位模板生命周期：`approve_delete`/`reject_delete` 须 `requirePermissions("access.role.assign", "governance.supervisor_review.approve")`，其余动作只须 `access.role.assign`）、`:262-300`（职级、定岗：用路径 id 重建请求，忽略请求体 user_id）。
- 证据：探针 2——生命周期端点、授权审批端点在全部测试文件里 0 处；`requirePermissions(` 在 access 测试目录 0 处；AccessControllerTests 无 ArgumentCaptor。探针 1——33 个方法级映射只调到 10 个。
- 正常触发：这三处正是「谁能批准删除配置」「谁能审批授权」「改的是谁的职级/岗位」。任何一处回退（例如重构时把双权限写回单权限）都不会被现有测试发现。
- 依据：GEN-003「测试须实际执行目标行为、具备能失败的断言，并覆盖本模块关键分支」；上位是治理地图关于删除配置须经最高权限审批、授权申请按主管链审批的规定（原句见 M031、M034 各 raw 的引用）。
- 最小拟处置：AccessControllerTests 补三条——①生命周期端点：action=approve_delete 时校验调用的是双权限、action=archive 时是单权限；②授权审批端点：校验双权限，并在其被拒时返回 403；③职级与定岗端点：请求体带别人的 user_id，用 ArgumentCaptor 断言传给服务层的是路径 id。不改被测代码。

## 6. 观察（登记，不立项）

- OBS-1：`userEffectivePermissionsReturnsTargetUserSetWithUserViewPermission` 的桩返回 `wildcard=true`，现实实现恒为 false（M032-002 已把类注释改为现状）；该测试只验 JSON 形状，不立项。
- OBS-2：`unauthenticatedAccessEndpointReturnsUnauthorizedOrForbidden` 接受 401 或 403 两种（先例 M011 OBS-02 同型登记）。
- OBS-3：其余 20 个未被调到的映射多为无分支的单权限门控加透传，服务层行为在 AccessServiceTests／M006SecurityRegressionTests（M036／M037）；不为逐端点补「403 样板测试」立项。
- OBS-4：`staffLevelChangeRunsInOneTransactionLikePositionAssignment` 只断言注解存在（M006-G-07 既定写法）；与 XM-M031-B 的关系已登记。

## 7. 结论

席甲 B1 票：**FAIL**（CX-M035-001；OBS-1…4 登记）。2 文件 1200 行全读、52 判据逐项、三线完成；未改任何文件；落盘前未读 Grok 本次正文。


## M035 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-21T09:27:10Z。输入：modules/M035-b1-grok-raw.md V0.01（38958 bytes，SHA256 a03bf5ae91c906a9f4903ea9a0b5bf4a6d6bbf7751f42a38eae8f9c68228abdf）、modules/M035-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M035 B2-r1 整批候选」段（全文件 SHA256 adeec3a274b3ff35dd320f9e1106860c265054adae4c29e174e6226e2df39081）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-03→001；CX-M035-001＋GR-01→002；GR-02（＝席甲 OBS-4）→003；两席其余观察全部登记；无漏并、无改写。
- 自审纠正：①GR-03 席甲漏——B1 把 `seededDisplayNamesAreChinese` 算作「有真库断言」，没去读它 SQL 里的 `replace(…,'AI','')`，也没拿注释里的豁免句去对治理地图原文；席甲在 M034 B1 的探针里自己也写过同样的「AI 除外」过滤，属同一盲点。②GR-02 席甲只记成观察——测试名与注释声称的场景不等于断言的场景，按 GEN-003／GEN-016 应立项。
- 001 的配套迁移：只改测试会在主干留下一条已知红的测试；「AI」→「人工智能」是同义中文写法，不改权限含义；permission_code 不动；已应用迁移不回改。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
