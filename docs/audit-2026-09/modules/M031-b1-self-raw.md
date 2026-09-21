CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M031 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T04:31:15Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main 508b2e87。
- 模块 M031「功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第1段」3 文件，52 判据（BEQR 7、BSM 14、PGM 13、GEN 18）。B3 累计 0。
- 三态：AccessController.java、AccessRepository.java 与 HEAD 一致；AccessService.java 有一处已暂存改动（V0.19→V0.20，`changePositionTemplateLifecycle` 对最高权限放行 `requirePlatformAdmin`）。3 文件相对 origin/main 的差异来自本地未合并提交 124cc4b7、523be51e（OPEN PR #447）与该暂存改动。审计对象＝主工作区现状文件。
- 顺序：Grok 已先派（modules/M031-b1-grok-raw.md 存在=True），本报告落盘前未读其正文。只读承诺：未改任何源码/测试/文档；数据库只对 complex_test SELECT；两次一次性 worktree（origin/main）用后已按 D-028 还原并移除。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/access/AccessController.java | 332 | 17089 | fbdb4667cf1a0811adae1e668dc299b42a07a82ba5abda879cc098bca0c60146 | 全读（L1-332） |
| backend/src/main/java/com/complex/platform/access/AccessRepository.java | 298 | 12799 | 6fc11453fee5aefbfe97bb270d4f6f88a294101daaab88c62abcf768338b6cca | 全读（L1-298） |
| backend/src/main/java/com/complex/platform/access/AccessService.java | 2361 | 128036 | f38bff0b90f42703756a4069fafe560a16b834e8c30b34b9bb1b6a57d3e580bb | 全读（L1-2361） |

## 2. 可执行证据原文

基线一（主工作区，/home/james/audit-tmp/m031_b1_run.log）：

```
START=2026-09-21T04:17:47Z HEAD=e456ba96 origin/main=508b2e87 DB=complex_test
== 0. files (lines/bytes/sha256) + three states
backend/src/main/java/com/complex/platform/access/AccessController.java 332 17089 fbdb4667cf1a0811adae1e668dc299b42a07a82ba5abda879cc098bca0c60146 | status[] vsHEAD[] vsOM[ 1 file changed, 1 insertion(+), 1 deletion(-)]
backend/src/main/java/com/complex/platform/access/AccessRepository.java 298 12799 6fc11453fee5aefbfe97bb270d4f6f88a294101daaab88c62abcf768338b6cca | status[] vsHEAD[] vsOM[ 1 file changed, 11 insertions(+)]
backend/src/main/java/com/complex/platform/access/AccessService.java 2361 128036 f38bff0b90f42703756a4069fafe560a16b834e8c30b34b9bb1b6a57d3e580bb | status[M ] vsHEAD[ 1 file changed, 5 insertions(+), 2 deletions(-)] vsOM[ 1 file changed, 52 insertions(+), 2 deletions(-)]
523be51e audit M031: transactional position-template save and cross-scope tests
124cc4b7 audit M031: recompute position grants; gate effective-permission and scope target
== 1. endpoints and gates (AccessController)
34
204:    @DeleteMapping("/position-role-templates/{id}")
244:    @GetMapping("/effective-permissions")
== 2. per public method: idempotency / success audit / failure audit / tx (AccessService)
  106 public  listPermissionTemplates                len=  16 idem=0 ok=1 fail=0 try=0 tx=0
  122 public  createPermissionTemplate               len=  17 idem=0 ok=0 fail=0 try=0 tx=0
  139 public  updatePermissionTemplate               len=  43 idem=0 ok=0 fail=0 try=0 tx=0
  182 public  copyPermissionTemplate                 len=  24 idem=0 ok=0 fail=0 try=0 tx=0
  206 public  applyPermissionTemplate                len=  71 idem=1 ok=1 fail=1 try=1 tx=0
  277 public  listPermissions                        len=  14 idem=0 ok=1 fail=0 try=0 tx=0
  291 public  listRoles                              len=  16 idem=0 ok=1 fail=0 try=0 tx=0
  307 public  createRole                             len=  34 idem=1 ok=1 fail=1 try=1 tx=0
  341 public  assignUserRole                         len=  24 idem=1 ok=0 fail=1 try=1 tx=0
  365 public  unassignUserRole                       len=  42 idem=1 ok=1 fail=1 try=1 tx=0
  407 public  listPositionRoleTemplates              len=   4 idem=0 ok=0 fail=0 try=0 tx=0
  411 public  listPositionRoleTemplates              len=  18 idem=0 ok=1 fail=0 try=0 tx=0
  429 public  createPositionRoleTemplate             len=  28 idem=1 ok=0 fail=1 try=1 tx=1
  457 public  updatePositionRoleTemplate             len=  24 idem=1 ok=0 fail=1 try=1 tx=1
  481 public  deletePositionRoleTemplate             len=   5 idem=0 ok=0 fail=0 try=0 tx=0
  486 public  changePositionTemplateLifecycle        len=  64 idem=1 ok=1 fail=0 try=0 tx=1
  557 public  assignUserPosition                     len=  31 idem=1 ok=0 fail=1 try=1 tx=1
  588 public  changeUserScope                        len= 121 idem=1 ok=1 fail=1 try=1 tx=1
  815 public  createTemporaryGrant                   len=  24 idem=1 ok=0 fail=1 try=1 tx=0
  839 public  revokeTemporaryGrant                   len=  40 idem=1 ok=1 fail=1 try=1 tx=0
  879 public  applyGrant                             len=  59 idem=1 ok=1 fail=1 try=1 tx=1
  938 public  reviewGrant                            len=  75 idem=1 ok=1 fail=1 try=1 tx=1
 1013 public  listGrantRequests                      len=  14 idem=0 ok=1 fail=0 try=0 tx=0
 1027 public  listGrantRequests                      len=  17 idem=0 ok=1 fail=0 try=0 tx=0
 1044 public  grantSummary                           len=  55 idem=0 ok=0 fail=0 try=0 tx=0
 1127 public  listAccessUsers                        len=  22 idem=0 ok=1 fail=0 try=0 tx=0
 1149 public  createAccessUser                       len=  78 idem=1 ok=1 fail=1 try=1 tx=0
 1259 public  listStaffLevels                        len=  19 idem=0 ok=1 fail=0 try=0 tx=0
 1278 public  assignStaffLevel                       len=  76 idem=1 ok=1 fail=1 try=1 tx=1
 1377 public  setUserPermissionOverrides             len=   5 idem=0 ok=0 fail=0 try=0 tx=1
 1382 public  removeUserPermissionOverrides          len=   4 idem=0 ok=0 fail=0 try=0 tx=1
 1912 public  grantPolicy                            len=  11 idem=0 ok=0 fail=0 try=1 tx=0
 1923 public  updateGrantPolicy                      len=  16 idem=1 ok=1 fail=0 try=1 tx=1
 2111 public  effectivePermissionsForUser            len=   7 idem=0 ok=0 fail=0 try=0 tx=0
== 3. operator-facing messages without CJK (expect none)
(end)
== 4. recompute path scope facts
backend/src/main/java/com/complex/platform/access/AccessRepository.java:149:    default List<String> listUserIdsAssignedToPosition(String positionCode, String staffLevelId) {
backend/src/main/java/com/complex/platform/access/AccessService.java:2119:        List<String> userIds = repository.listUserIdsAssignedToPosition(saved.positionCode(), saved.staffLevelId());
    default List<String> listUserIdsAssignedToPosition(String positionCode, String staffLevelId) {
        return listUsers().stream()
                .filter(user -> findUserPositionAssignment(user.id())
                        .filter(assignment -> positionCode.equals(assignment.positionCode())
                                && staffLevelId.equals(assignment.staffLevelId()))
                        .isPresent())
                .map(AccessUser::id)
                .toList();
    }
1742:            PositionRoleTemplate mapping = repository.findPositionRoleTemplate(
1834:        PositionRoleTemplate mapping = repository.findPositionRoleTemplate(
CREATE UNIQUE INDEX uq_position_role_templates_active_scope ON public.position_role_templates USING btree (COALESCE(organization_id, ''::character varying), position_code, staff_level_id) WHERE ((status)::text = 'active'::text)
user_id,position_code,staff_level_id,organization_id,merchant_id,assigned_by,assigned_at,assigned_role_id,assigned_position_template_id,position_application_ids
tests mentioning recompute / two-org position data: 0
== 5. existing tests (main worktree, complex_test)
MVN_EXIT=1
AccessControllerTests tests=13 errors=0 skipped=0 failures=0
AccessServiceTests tests=50 errors=6 skipped=0 failures=0
== 6. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T04:17:50Z
```

基线二（一次性 worktree＝origin/main＋本模块 3 个现行文件；/home/james/audit-tmp/m031_b1_scratch_base.log，测试文件取 origin/main 版）：

```
START=2026-09-21T04:24:16Z scratch=/home/james/scratch-m031base at 508b2e87 DB=complex_test with=[]
 M backend/src/main/java/com/complex/platform/access/AccessController.java
 M backend/src/main/java/com/complex/platform/access/AccessRepository.java
 M backend/src/main/java/com/complex/platform/access/AccessService.java
MVN_EXIT=1
AccessControllerTests tests=13 errors=0 skipped=0 failures=1
   failure userEffectivePermissionsReturnsTargetUserSetWithUserViewPermission | No value at JSON path &quot;$.data.user_id&quot;
AccessServiceTests tests=48 errors=0 skipped=0 failures=0
scratch removed
END=2026-09-21T04:24:28Z
```

基线二补充（同一脚本加 with-tests，即再带入主工作区的 AccessServiceTests.java 与 AccessControllerTests.java）：MVN_EXIT=0；AccessControllerTests tests=13 errors=0 failures=0；AccessServiceTests tests=50 errors=0 failures=0；worktree 已移除。

如实说明：
- 基线一里 AccessServiceTests 6 个报错的消息都是「退款、免单、授权与资金事项必须登记直接受益人」，抛出点在 `governance/review/SupervisorReviewService.java`（主工作区该文件相对 origin/main 有未发布差异（diff --stat 增删合计 152 行），属别的模块）；基线二在 origin/main 上同一批测试全绿，故这 6 个报错不归本模块 3 个文件（OBS-1）。
- 基线二里 AccessControllerTests 唯一失败 `userEffectivePermissionsReturnsTargetUserSetWithUserViewPermission` 的原因：本地提交 e13157c4 把该测试的桩从 `authorizationService.effectivePermissionsForUser` 改到 `service.effectivePermissionsForUser`（1 行），与控制器改走服务层同组织闸配套；带入该测试文件后 13/13 绿。检查点须同批带入这两份测试文件（OBS-2）。
- 探针 3 的 grep 对「纯英文报错文案」无命中。探针 5 的 mvn 为离线增量，耗时数秒。
- complex_test 只读查询：`position_role_templates` 现 5 行、`organization_id` 全为空、全为 active；`audit_events` 内无 access./permission_template./role. 前缀事件（测试不向共享审计表留行），故失败留痕的库内表现无现成样本。
- 契约面：`docs/openapi/openapi-p0.yaml` 内 `/api/v1/access/*` 共 21 条路径；控制器 34 个映射里 `/grant-policy`（GET、POST）、`/position-role-templates/{id}/lifecycle`、`/users/{id}/scope`、`/users/{id}/permission-overrides`（及 `/remove`）6 个在契约内 0 处（OBS-8，契约文件属别的模块）。`revokeTemporaryGrant` 成功事件名 `permission_template.revoked` 与契约 `x-audit-event-success` 一致。

## 3. 三线审计

- 第一性原理线。用途：后台权限接口——权限模板、角色、岗位映射（岗位＋职级→角色＋模板）、定岗、改职级、范围变更、临时授权、授权申请与审批、员工单独授予/拒绝、授权政策、账号。实际行为：全部写口带幂等；授出类动作统一过「同组织闸→已知权限码→不得超出授权人自身有效集→高危须审批」；定岗把普通码随保存生效、高危码先写显式拒绝并进审批链；范围变更同事务撤旧派生、撤非派生增权、作废旧挂起、新范围整单进审批；审批前比对授权事实指纹。未经验证的前提三条：①保存岗位映射后的重算分支真的按新映射重算、停用真的收回——无任何测试执行（CX-M031-001）；②失败留痕在带事务的方法里能留下——`AuditEventService`/`JdbcAuditEventRepository` 无独立事务传播，失败事件与被回滚的业务写同一事务（OBS-9，未复现，属审计证据模块）；③主工作区 AuthorizationService 的未发布改动不影响本模块结论——已用 origin/main 基线二验证测试全绿。
- 顺从式线：52 判据逐项见 §4；3 文件 2991 行全读。
- 对抗式线（grilling）：重复提交同一幂等键→`idempotencyService.execute` 回放，不重算；对已存档模板再存档→INVALID_STATE；对已通过申请再审→404（只取 pending）；审批期间模板/映射/账号归属变化→GRANT_SNAPSHOT_CHANGED；受益人或申请人自审→403；项目管理员改别的项目的人→CROSS_ORG_FORBIDDEN；商户主体改别的商户的人→CROSS_MERCHANT_FORBIDDEN；套用平台或他组织模板→CROSS_ORG_FORBIDDEN；复制再套用提权→`requireActorCanGrant` 拦；移除显式拒绝使高危生效→须审批（L1415-1426）；保存岗位映射时重算会不会覆盖在岗人员薪资→不会，L2129-2138 先取现有薪资再回写；重算会不会把已获批的高危再挂起→会（`holdHighRiskPositionPermissions` 对无现存显式拒绝的高危码重新写拒绝并新开释放单），与治理地图「按变更后的岗位权限模板重算…既有高危…不得因处于岗位模板内而免除授出前审批」字面一致，HTML 未另说已获批者可免，登记 OBS-10 不立项；岗位映射存档后在岗人员的派生权→不收回（存档≠停用），治理地图无「存档」字样，属 M087 既有裁决，不反推；失败时有没有留痕→16 处有、2 处没有（CX-M031-002）。

## 4. 52 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BEQR-157 员工状态 | 无本面｜原文「待入职、在职、试岗、停岗、离职」是员工档案状态；本模块 3 个文件只处理登录账号状态（启用/待激活/禁用，`requireAccountStatus` L1239-1245），不读写、不展示员工状态，也未另造员工状态值 |
| BEQR-182 考勤日结果 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 3 个文件不读写、不展示、不声称该对象（定岗时的基本工资只作字段透传，不是薪资单） |
| BEQR-183 打卡方式 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 3 个文件不读写、不展示、不声称该对象（定岗时的基本工资只作字段透传，不是薪资单） |
| BEQR-184 请假类型 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 3 个文件不读写、不展示、不声称该对象（定岗时的基本工资只作字段透传，不是薪资单） |
| BEQR-185 请假单状态 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 3 个文件不读写、不展示、不声称该对象（定岗时的基本工资只作字段透传，不是薪资单） |
| BEQR-186 薪资单构成 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 3 个文件不读写、不展示、不声称该对象（定岗时的基本工资只作字段透传，不是薪资单） |
| BEQR-187 员工提成发放 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 3 个文件不读写、不展示、不声称该对象（定岗时的基本工资只作字段透传，不是薪资单） |
| BSM-001 整体说明：这套系统到底管什么 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「整体说明：这套系统到底管什么」的对象、状态或界面文字 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 适用｜「权限分岗」：33 个带门控的端点逐个有 `requirePermission(s)`（探针 1 与 §2 补充清单），唯一无权限码的 `/effective-permissions` 只返回本人；岗位→角色+模板→最终权限链见 §3｜通过（除 CX-M031-001/002） |
| BSM-009 基础管理 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「基础管理」的对象、状态或界面文字 |
| BSM-028 主要功能 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「主要功能」的对象、状态或界面文字 |
| BSM-029 和谁有关 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「和谁有关」的对象、状态或界面文字 |
| BSM-073 主要功能 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「主要功能」的对象、状态或界面文字 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 3 个文件不读写、不展示、不声称该对象（定岗时的基本工资只作字段透传，不是薪资单） |
| BSM-085 日常工作 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 3 个文件不读写、不展示、不声称该对象（定岗时的基本工资只作字段透传，不是薪资单） |
| BSM-086 排班、考勤与请假 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 3 个文件不读写、不展示、不声称该对象（定岗时的基本工资只作字段透传，不是薪资单） |
| BSM-087 我的薪资 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 3 个文件不读写、不展示、不声称该对象（定岗时的基本工资只作字段透传，不是薪资单） |
| BSM-088 员工提成、活动执行与人员服务边界 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 3 个文件不读写、不展示、不声称该对象（定岗时的基本工资只作字段透传，不是薪资单） |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 适用｜「不同人只看自己该看的」：列表类读口按平台管理员/商户/项目三档收窄（`listAccessUsers` L1127-1147、`visibleGrantRequests` L1099-1125、`listPermissionTemplates` L106-120）；`effectivePermissionsForUser` L2111-2115 先过同组织闸｜通过 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「相互关系：为什么这些子系统必须连在一起」的对象、状态或界面文字 |
| BSM-120 已确认规则和实施前输入项 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「已确认规则和实施前输入项」的对象、状态或界面文字 |
| GEN-001 必要性与最小性 | 适用｜3 文件 2991 行；未见死分支，唯私有方法 `slug`（L2313-2319）本文件内 0 处调用（grep `slug(` 只命中定义行，见 OBS-7）｜通过（登记 OBS-7） |
| GEN-002 可执行正确性 | 适用｜origin/main 加本模块 3 个现行文件：AccessServiceTests 48/48、带入两份同批测试改动后 50/50 与 AccessControllerTests 13/13 全绿（§2 基线二）；主工作区 6 个报错来自别的模块未发布改动（OBS-1）｜通过 |
| GEN-003 测试有效性 | 适用｜`recomputeUsersOnPositionTemplate` L2117-2140 两个分支与 `listUserIdsAssignedToPosition`（AccessRepository L149-157）在 AccessServiceTests 内 0 处引用、0 条断言（探针 4 末行）；同批本地提交新增的两条测试只测跨组织拒绝｜不通过（CX-M031-001） |
| GEN-004 信任边界校验 | 适用｜请求体空值 `requireRequest`、必填 `requireText`、权限码 `requireKnownPermissions`、审批结论/申请类型/账号状态/覆盖效果均为闭集校验；时间窗与阈值不许查询覆盖（L1046-1049）｜通过 |
| GEN-005 授权与最小权限 | 适用｜提权防护 `requireActorCanGrant`（L789-801）覆盖建角色、改模板增码、套模板、临时授权、员工单独授予、解除角色；岗位映射与定岗走 `requireActorCanGrantImmediate`（高危由挂起单审批）；模板 apply 校验模板归属（`applyPermissionTemplate` 内 A1-05 段）；岗位映射保存限平台管理员（L2062）；生命周期 approve/reject 限最高权限（L526-528）。AuthorizationService 在主工作区有已暂存未发布改动，但本条结论只依赖 `isPlatformAdmin`/`requireProjectScope`/`effectivePermissions` 三个方法的既有语义，origin/main 版同名方法均存在（基线二编译通过）｜通过 |
| GEN-006 数据完整性 | 适用｜授权窗口 `validFrom < validUntil`（L1540-1543、L1678-1680）；金额整数分非负（L1302-1304、L1856-1858）；币种仅 CNY｜通过 |
| GEN-007 并发与幂等 | 适用｜申请/审批/范围变更先 `lockGrantAuthorizationFacts()`；审批前比对指纹（L959-962）；`findPositionRoleTemplate` 带 FOR SHARE；状态机二次操作：已存档再存档→INVALID_STATE、非 pending 申请再审→404、删除复核链未走完模板状态不变（L534-536）｜通过 |
| GEN-008 接口与模式一致性 | 适用｜AccessRepository 40 个 default 方法中 38 个有 JdbcAccessRepository 覆盖；未覆盖的 2 个（`listPendingGrantRequests`、`listUserIdsAssignedToPosition`）是建立在已覆盖方法之上的组合，Jdbc 下行为成立（OBS-5 记其全表扫描）｜通过（登记 OBS-5） |
| GEN-009 错误处理与恢复 | 适用｜异常统一 ApiException 带中文文案；`codeOf` 对非 ApiException 记类名；`grantPolicy` JSON 解析失败→409 中文；未见吞错或伪成功｜通过 |
| GEN-010 安全与敏感信息 | 适用｜口令只传给 `credentialProvisioner`，审计只记是否已建凭据；报错不回显口令；报错文案里带权限码/账号 id/模板 id（OBS-6，属操作对象标识，不是内部堆栈）｜通过（登记 OBS-6） |
| GEN-011 可观测与审计证据 | 适用｜成功事件 28 处、失败事件 16 处；`changePositionTemplateLifecycle`（L485-548）与 `updateGrantPolicy`（L1922-1937）无失败事件，首次正常失败（重复存档、非最高权限审批删除、非平台管理员）不留痕（CX-M031-002）；`grantSummary`、`effectivePermissionsForUser` 两个读口无查看事件（OBS-4）｜不通过（CX-M031-002） |
| GEN-012 资源与性能边界 | 适用｜`listUserIdsAssignedToPosition` 默认实现先取全部账号再逐个查定岗（N+1）、`listPendingGrantRequests` 取全表再过滤；均只在平台管理员保存岗位映射/范围变更时触发，complex_test 现 5 条映射，未见首次正常使用下的可复现超时，不为理论规模立项（OBS-5）｜通过 |
| GEN-013 构建配置与依赖 | 适用｜只依赖 Spring、Jackson、JDK；`new ObjectMapper()` 就地创建（L1752、L1916、L1928）功能正确｜通过 |
| GEN-014 数据库迁移正确性 | 无本面｜本模块无迁移文件；库内约束只读核对：`uq_position_role_templates_active_scope`（组织+岗位+职级，仅 active）与 `findPositionRoleTemplate` 的组织优先、全局兜底查询一致 |
| GEN-015 界面可用性与可访问性 | 适用｜面向操作者的报错全部中文（探针 3：无纯英文文案）；事件名/错误码为英文机器标识，不是界面文字｜通过 |
| GEN-016 文档可验证性 | 适用｜方法注释逐条对照实现：L581-586（范围变更）、L708、L717、L728、L785-788、L803-807、L1273-1276、L1372-1375、L2117（「保存岗位模板后立即按新映射重算在岗人员派生权；停用则收回派生」——实现存在，但无任何测试执行它，归 CX-M031-001）｜通过（除 CX-M031-001） |
| GEN-017 业务来源分类 | 适用｜未见另造业务规则；岗位映射保存时 `organizationId` 恒写 null（L2092，即接口只能建全局映射），与库内按组织唯一的索引并存，HTML 对「按组织设岗位映射」沉默，不反推（OBS-3）｜通过（登记 OBS-3） |
| GEN-018 第一性原理证据 | 适用｜结论落到源码原文、两份一次性 worktree 测试输出、complex_test 只读查询与 HTML 原句｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 适用｜底座留痕：18 处幂等执行点中 16 处带失败留痕，`changePositionTemplateLifecycle` 与 `updateGrantPolicy` 两处没有（CX-M031-002）｜不通过（CX-M031-002） |
| PGM-008 员工和手机 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「员工和手机」的对象、状态或界面文字 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「空间和点位：所有业务都要知道“发生在哪”」的对象、状态或界面文字 |
| PGM-024 影响业务 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「影响业务」的对象、状态或界面文字 |
| PGM-026 主要功能 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「主要功能」的对象、状态或界面文字 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「员工和手机：人、私人手机、工作手机要能对应起来」的对象、状态或界面文字 |
| PGM-033 主要功能 | 适用｜员工与账号主要功能里的定岗、改职级、范围变更三条写路径（L557-706、L1277-1352）：同事务、旧派生先失效、新范围整单进审批、高危挂起，与治理地图「保存岗位、职责或…所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效」原句一致；岗位映射保存后的重算分支零测试（CX-M031-001）｜不通过（CX-M031-001） |
| PGM-034 工作手机和企业微信 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「工作手机和企业微信」的对象、状态或界面文字 |
| PGM-035 提成发放和人员服务治理 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 3 个文件不读写、不展示、不声称该对象（定岗时的基本工资只作字段透传，不是薪资单） |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「设备管理：谁领了什么设备，设备能不能用」的对象、状态或界面文字 |
| PGM-048 商户侧：在授权业务范围内全开放 | 适用｜商户侧边界：`requireSameOrganization` L2159-2170 同时卡项目与商户；`createAccessUser` L1169-1178 商户主体只能在本商户建号；`canViewGrantRequest` 商户主体只见本商户申请｜通过 |
| PGM-059 客人与员工的个人信息保护 | 适用｜个人信息：账号审计元数据（L1247-1257）只记类型、归属、是否有同意凭据与凭据是否已建，不记口令、手机号；薪资只经定岗/改职级写路径传递，不进审计元数据｜通过 |
| PGM-066 已拍板规则和待排期问题 | 适用｜已拍板规则：删除岗位模板须先存档、填写理由、最高权限审批后软删除（`deletePositionRoleTemplate` L481-483 恒拒、`changePositionTemplateLifecycle` L485-548 四动作）；受益人与申请人不得自审（L952-957）；申请指纹钉住授权事实（L1714-1757）｜通过 |

## 5. 本次 Findings

### CX-M031-001（细化；工程缺陷成立）岗位映射保存后的重算分支零测试覆盖

- 位置：`AccessService.java:2107`（调用点）、`:2117-2140`（`recomputeUsersOnPositionTemplate`：启用→对每个在岗人员走 `assignUserPositionDirect(…, true)`；非启用→`replacePositionGrants(userId, null, null, List.of(), …)` 收回派生）、`AccessRepository.java:149-157`（`listUserIdsAssignedToPosition`）。
- 原文：L2117 注释「保存岗位模板后立即按新映射重算在岗人员派生权；停用则收回派生。」
- 证据：探针 4 末行 `tests mentioning recompute / two-org position data: 0`；`git diff origin/main -- AccessServiceTests.java` 新增的只有 `effectivePermissionsForUserRejectsCrossOrganization` 与 `changeUserScopeRejectsTargetOutsideActorProject` 两条跨组织拒绝测试。该分支是本模块相对 origin/main 的主要新增行为，会批量改写在岗人员的角色、模板应用、定岗行与高危挂起。
- 正常触发：平台管理员修改一条已有在岗人员的岗位映射（换角色或模板），或把它停用。影响：重算错了没人会先知道——少收回＝越权残留，多收回＝整岗人员失权。
- 依据：GEN-003「测试须实际执行目标行为、具备能失败的断言，并覆盖本模块关键分支」；治理地图原句「系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效」。
- 最小拟处置：在 AccessServiceTests 补两条——①在岗人员存在时把映射改到另一角色/模板，断言该人员派生角色与模板应用换成新的、旧的失效、薪资不变；②把映射停用，断言派生被收回。若 RED 暴露实现缺陷，再按缺陷最小修。

### CX-M031-002（技术性；工程缺陷成立，处置受 OBS-9 牵连）两个写方法无失败留痕

- 位置：`AccessService.java:485-548` `changePositionTemplateLifecycle`、`:1922-1937` `updateGrantPolicy`。
- 证据：探针 2——两方法 `fail=0`；同文件 18 处幂等执行点中其余 16 处都带 `recordFailure`（含同为岗位映射的 create/update）。
- 正常触发：对已存档模板再点存档（INVALID_STATE）、非最高权限点「批准删除」（MASTER_AUTHORITY_REQUIRED）、非平台管理员调生命周期（PLATFORM_ADMIN_REQUIRED）、授权政策缺理由（VALIDATION_FAILED）——都不留失败事件；其中「非最高权限尝试批准删除配置」正是事后要查的动作。
- 依据：GEN-011「关键操作须留下可关联、可复现的结构化证据；…审计记录不得相互矛盾」；同文件 16:2 的既有范式；先例 M020-005（该例另有契约声明失败事件名，本例两个端点不在契约内，见 OBS-8）。
- 最小拟处置：两方法体包 try/catch RuntimeException → `recordFailure`（事件名沿用同域范式：`access.position_role_template.lifecycle_failed`、`access.grant_policy.change_failed`）再抛；测试各补一条失败落事件。
- 牵连说明：两方法都带 `@Transactional`；若 OBS-9 成立，事务内记下的失败事件会随回滚一起丢，本处置在真库上不生效。B2 时与 Grok 对齐：要么连同 OBS-9 的归属一并登记，要么证明 OBS-9 不成立后入队。

## 6. 观察（登记，不立项）

- OBS-1（跨模块）：主工作区跑 AccessServiceTests 有 6 个报错（BENEFICIARY_REQUIRED），origin/main 基线全绿；来源是 SupervisorReviewService 等别的模块的未发布本地改动。
- OBS-2：本模块 3 个文件的检查点须同批带入 AccessServiceTests.java 与 AccessControllerTests.java（本地提交 523be51e、e13157c4 的配套测试改动），否则 origin/main 上 AccessControllerTests 红 1 条。
- OBS-3：接口保存岗位映射时 `organizationId` 恒为 null（L2092），库内却有按组织唯一的索引与「组织优先、全局兜底」的查询；complex_test 5 条映射全为全局。HTML 对「按组织设岗位映射」沉默，不反推。
- OBS-4：`grantSummary`（L1044）与 `effectivePermissionsForUser`（L2111）两个读口无查看事件，其余列表读口都有；HTML 未要求每次查看留痕，不立项。
- OBS-5：`listUserIdsAssignedToPosition`（N+1）与 `listPendingGrantRequests`（全表过滤）无 Jdbc 定向查询；后者源码已有 ponytail 注释写明上限与升级路径，前者没有。
- OBS-6：`applyTemplate`、`revokeTemporaryGrant`、`createTemporaryGrantDirect`、`templateFromRequest`、`assignStaffLevel` 非定岗分支把经办人写成字面量 `"access-api"`（L248、L850、L1323、L1561、L2264），行内查不到是谁；审计事件里有真实操作人。
- OBS-7：私有方法 `slug`（L2313-2319）本文件内无调用。
- OBS-8（跨模块）：6 个控制器映射不在 openapi-p0.yaml 内（§2）。
- OBS-9（跨模块，未复现）：`AuditEventService.record` 经 `JdbcAuditEventRepository`（同一 `NamedParameterJdbcTemplate`）写入，全后端主代码无 REQUIRES_NEW 的审计写入；本模块 9 个 `@Transactional` 写方法在 catch 里记失败事件后再抛，事务回滚时失败事件按 Spring 语义一并回滚。未做真库复现（需 Spring 上下文写共享审计表），归审计证据模块审计时核。
- OBS-10：保存岗位映射会把该岗位在岗人员已获批的高危权限重新挂起并新开释放单；与治理地图字面一致，HTML 未说已获批者可免；若操作员认为应免，属业务裁决。

## 7. 结论

席甲 B1 票：**FAIL**（CX-M031-001 细化、CX-M031-002 技术性；OBS-1…10 登记）。3 文件 2991 行全读、52 判据逐项、三线完成；未改任何文件；落盘前未读 Grok 本次正文。


## M031 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-21T04:40:39Z。输入：modules/M031-b1-grok-raw.md V0.01（33364 bytes，SHA256 6e6f5e32715f1c48d964c94b042ba17300e72c5a08a8d3668ad51d1591b7805d）、modules/M031-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M031 B2-r1 整批候选」段（全文件 SHA256 9ac5dfef77a1b0bb9e8d213d0bbd59427a5324a19644ded9ed1d5deef953bafb）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-03→001；CX-M031-001→002；CX-M031-002＋Grok O-2→003；GR-01→XM-M031-A（登记）；OBS-9→XM-M031-B（登记）；OBS-8→XM-M031-C（登记）；两席其余观察全部登记；无漏并、无改写。
- 自审纠正：①GR-03 席甲漏——B1 读了 restore 分支，也注意到存档不收回派生，但没追 `withLifecycle` 把 status 写死 `inactive`、Jdbc SQL 也恒写 `inactive`，于是没发现「恢复后不能定岗」。核源成立，采 Grok。②B1 把「事务内失败留痕丢失」只记成未复现的 OBS-9；B2 用真库探针复现成立（不带事务 1 行、带事务 0 行），并据此给 003 加了如实限定。
- GR-01 复核：`git show origin/main:backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java | grep -c requireProjectScopeId` = 0；`git log -S` 定位到本地未发布提交 52074492。本模块 3 个文件对「null＝平台管理员全量」的用法与 AccessRepository 接口契约一致，违约的是 M032 文件的未发布改动。登记转 M032，不在本模块入队。
- 001 处置边界：只改 AccessService.java。`savePositionRoleTemplate` 的 upsert 只更新 position_code、staff_level_id、role_id、template_id、organization_id、status、updated_at，不碰 archived_at、deleted_at、delete_review_id、delete_reason；两步写都在同一个幂等动作里，同一事务。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
