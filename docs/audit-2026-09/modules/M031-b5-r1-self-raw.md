CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M031 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；AccessService.java 的现行改动为席甲 B3 所写，且席甲 B1 漏了 GR-03，以 Grok 票为准）。2026-09-21T05:18:34Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main `508b2e87`。
- 模块 M031 3 文件（§1 全读；AccessService.java 现 V0.21），52 判据；B3 累计 1。环境前提同 B1（D-028）：测试实跑在基于 origin/main 的一次性 worktree 里（已还原移除）。
- 顺序：本席取证脚本先跑完，随后落本报告；落盘前未读 Grok 正文（modules/M031-b5-r1-grok-raw.md 存在=False）。只读承诺：未改任何已跟踪文件；数据库只对 complex_test SELECT（测试类自身写入除外）。
- 本轮补的方法（B1 漏了 GR-03）：对每个生命周期动作追到记录类与 Jdbc SQL 实际写了哪几列，再问「下一步正常操作还能不能做」。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/access/AccessController.java | 332 | 17089 | fbdb4667cf1a0811adae1e668dc299b42a07a82ba5abda879cc098bca0c60146 | 全读 |
| backend/src/main/java/com/complex/platform/access/AccessRepository.java | 298 | 12799 | 6fc11453fee5aefbfe97bb270d4f6f88a294101daaab88c62abcf768338b6cca | 全读 |
| backend/src/main/java/com/complex/platform/access/AccessService.java | 2393 | 130307 | 4556c02cf35206e8e5032855a0e9c6d8915beb3c43f2ae6f77fb1ce615dfeb0d | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m031_b5_run.log）

```
START=2026-09-21T05:18:17Z HEAD=e456ba96 origin/main=508b2e87 DB=complex_test
== 0. files (lines/bytes/sha256)
backend/src/main/java/com/complex/platform/access/AccessController.java 332 17089 fbdb4667cf1a0811adae1e668dc299b42a07a82ba5abda879cc098bca0c60146
backend/src/main/java/com/complex/platform/access/AccessRepository.java 298 12799 6fc11453fee5aefbfe97bb270d4f6f88a294101daaab88c62abcf768338b6cca
backend/src/main/java/com/complex/platform/access/AccessService.java 2393 130307 4556c02cf35206e8e5032855a0e9c6d8915beb3c43f2ae6f77fb1ce615dfeb0d
== 1. endpoints: mappings=34 gated=33
== 2. per public method: idempotency / success audit / failure audit / tx (AccessService; --all 含私有)
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
  486 public  changePositionTemplateLifecycle        len=  11 idem=0 ok=0 fail=1 try=1 tx=1
  497 private changePositionTemplateLifecycleAudited len=  74 idem=1 ok=1 fail=0 try=0 tx=0
  578 public  assignUserPosition                     len=  31 idem=1 ok=0 fail=1 try=1 tx=1
  609 public  changeUserScope                        len= 121 idem=1 ok=1 fail=1 try=1 tx=1
  836 public  createTemporaryGrant                   len=  24 idem=1 ok=0 fail=1 try=1 tx=0
  860 public  revokeTemporaryGrant                   len=  40 idem=1 ok=1 fail=1 try=1 tx=0
  900 public  applyGrant                             len=  59 idem=1 ok=1 fail=1 try=1 tx=1
  959 public  reviewGrant                            len=  75 idem=1 ok=1 fail=1 try=1 tx=1
 1034 public  listGrantRequests                      len=  14 idem=0 ok=1 fail=0 try=0 tx=0
 1048 public  listGrantRequests                      len=  17 idem=0 ok=1 fail=0 try=0 tx=0
 1065 public  grantSummary                           len=  55 idem=0 ok=0 fail=0 try=0 tx=0
 1148 public  listAccessUsers                        len=  22 idem=0 ok=1 fail=0 try=0 tx=0
 1170 public  createAccessUser                       len=  78 idem=1 ok=1 fail=1 try=1 tx=0
 1280 public  listStaffLevels                        len=  19 idem=0 ok=1 fail=0 try=0 tx=0
 1299 public  assignStaffLevel                       len=  76 idem=1 ok=1 fail=1 try=1 tx=1
 1398 public  setUserPermissionOverrides             len=   5 idem=0 ok=0 fail=0 try=0 tx=1
 1403 public  removeUserPermissionOverrides          len=   4 idem=0 ok=0 fail=0 try=0 tx=1
 1407 private changeUserPermissionOverrides          len=  78 idem=1 ok=1 fail=1 try=1 tx=0
 1933 public  grantPolicy                            len=  11 idem=0 ok=0 fail=0 try=1 tx=0
 1944 public  updateGrantPolicy                      len=  11 idem=0 ok=0 fail=1 try=1 tx=1
 1955 private updateGrantPolicyAudited               len=  16 idem=1 ok=1 fail=0 try=1 tx=0
 2143 public  effectivePermissionsForUser            len=   7 idem=0 ok=0 fail=0 try=0 tx=0
 2237 private executeTemplateWrite                   len=  42 idem=1 ok=1 fail=1 try=1 tx=0
counts: execute=18 recordFailure=18 recordSuccess=28 transactional=11
== 3. restore 新路径与两处失败外壳原文
    @Transactional
    public PositionRoleTemplate changePositionTemplateLifecycle(String id, StatusChangeRequest request) {
        // M031-003（GEN-011）：生命周期操作失败也留痕，与同文件其余写口同一范式。
        try {
            return changePositionTemplateLifecycleAudited(id, request);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("access.position_role_template.lifecycle_failed", "access.role.assign",
                    "position_role_template", id, null, request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    private PositionRoleTemplate changePositionTemplateLifecycleAudited(String id, StatusChangeRequest request) {
        requireRequest(request);
                case "restore" -> {
                    if (current.archivedAt() == null) throw ApiException.conflict("INVALID_STATE", "模板未存档");
                    if (current.deleteReviewId() != null && current.deletedAt() == null) throw ApiException.conflict("DELETE_REVIEW_PENDING", "须先处理删除复核");
                    // M031-001（BSM-004「想恢复点"取消存档"即可」）：恢复即回到可定岗的启用态；同范围已有启用中的替代映射时拒绝，不静默留成停用。
                    if (repository.findPositionRoleTemplate(current.organizationId(), current.positionCode(), current.staffLevelId())
                            .filter(active -> java.util.Objects.equals(active.organizationId(), current.organizationId())).isPresent()) {
                        throw ApiException.conflict("POSITION_TEMPLATE_ACTIVE_EXISTS", "同岗位同职级已有启用中的岗位模板，须先停用或存档后再恢复");
                    }
                    updated = current.withLifecycle(null, null, null, null, now);
                }
                case "request_delete" -> {
            repository.savePositionTemplateLifecycle(updated, authorizationService.currentActor().id());
            if ("restore".equals(action)) {
                updated = new PositionRoleTemplate(updated.id(), updated.positionCode(), updated.staffLevelId(), updated.roleId(),
                        updated.templateId(), updated.organizationId(), "active", updated.createdBy(), updated.createdAt(), now);
                repository.savePositionRoleTemplate(updated);
            }
            auditEventService.recordSuccess("access.position_role_template." + action, "access.role.assign",
                    "position_role_template", id, null, request.idempotencyKey(), Map.of("reason", reason));
            return updated;
        }, PositionRoleTemplate.class);
    @Transactional
    public AccessGrantPolicy updateGrantPolicy(AccessGrantPolicy.Update request) {
        // M031-003（GEN-011）：授权政策保存失败也留痕。
        try {
            return updateGrantPolicyAudited(request);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("access.grant_policy.change_failed", "admission.policy.manage",
                    "supervisor_review_policy", "access_grant", null, request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    private AccessGrantPolicy updateGrantPolicyAudited(AccessGrantPolicy.Update request) {
        if (request == null || request.policy() == null) throw ApiException.badRequest("VALIDATION_FAILED", "授权政策不能为空");
== 4. 一次性 worktree 实跑（origin/main＋本模块 3 文件＋AccessServiceTests/AccessControllerTests＋M006SecurityRegressionTests 发布变体）
publish variant sha c2d8b8a69779b7c5555cc49c7776f233b0e16bc875c22bfc96149799f48698c3; vs origin/main:  1 file changed, 3 insertions(+), 2 deletions(-)
b5 MVN_EXIT=0
   AccessControllerTests tests=13 errors=0 skipped=0 failures=0
   AccessServiceTests tests=54 errors=0 skipped=0 failures=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
scratch removed
== 5. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T05:18:31Z
```

## 3. 三线审计

- 第一性原理线：用途/实际行为同 B1。修后：存档的岗位映射恢复即回到可定岗的启用态（先清生命周期标记、再 upsert 写回 active，Jdbc 下顺序正确）；同范围已有启用中的替代映射时拒绝且零写；重算两分支有测试执行；18 处幂等执行点全部带失败留痕。仍未在本模块解决、已登记的前提：带事务方法的失败事件真库留不住（XM-M031-B→M092）；主工作区 JdbcAccessRepository 的未发布改动会让平台管理员列表报 PROJECT_SCOPE_REQUIRED（XM-M031-A→M032，origin/main 不复现）。
- 顺从式线：52 判据逐项见 §4；B1 不通过的 BSM-003/PGM-001/PGM-033/GEN-002/003/011/016 由 001、002、003 闭合。
- 对抗式线（grilling）：恢复两次→第二次 INVALID_STATE（archivedAt 已空）；恢复已软删除的映射→同一路径回到启用（删除前已核未被占用）；恢复时自己会不会被当成「替代映射」→不会，查询只认启用且未存档的行，此时自己仍是存档＋停用；组织行与全局行并存→只在 organizationId 相同（含双方皆空）时拒绝，与库内按组织的部分唯一索引一致；幂等键重放→回放已存响应（启用态对象），不再写；恢复会不会触发重算、把已获批高危重新挂起→不会，走的是仓储 upsert 而不是会重算的私有保存方法；失败外壳会不会改成功路径→原方法体原样下沉为私有方法，外壳只在 RuntimeException 时记事件并原样再抛；`updateGrantPolicy(null)`→VALIDATION_FAILED 且留失败事件（幂等键为空）。
- 新登记观察（不立项）：映射先「停用」（在岗人员派生已收回）再存档、再恢复，映射回到启用，但在岗人员的派生权不会自动恢复（本模块定稿处置写明恢复不触发重算，以免走「启用→存档→恢复」这条常见路径时把已获批高危重新挂起）；结果偏保守（少权不多权），再保存一次映射或重新定岗即恢复。

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
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 适用｜「权限分岗」：33 个带门控的端点逐个有 `requirePermission(s)`；「可改可删」：岗位映射存档后恢复即回到可定岗的启用态，同范围已有启用中的替代映射时 409 中文拒绝且零写（001 已修，§2 探针 3 原文、§2 探针 4 两条新测试绿）｜通过 |
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
| GEN-002 可执行正确性 | 适用｜一次性 worktree（origin/main＋本模块 3 文件＋同批测试文件＋M006 发布变体）实跑：AccessControllerTests 13、AccessServiceTests 54、M006SecurityRegressionTests 39 全绿（§2 探针 4）；恢复后可定岗（001）｜通过 |
| GEN-003 测试有效性 | 适用｜重算两分支、restore 两条路径（恢复可定岗／有替代映射被拒）、两个写口失败留痕均有能失败的断言；内存仓改为与 Jdbc 同语义（生命周期保存恒写 inactive、upsert 不动生命周期列），顺序写反或只 upsert 都会红｜通过 |
| GEN-004 信任边界校验 | 适用｜请求体空值 `requireRequest`、必填 `requireText`、权限码 `requireKnownPermissions`、审批结论/申请类型/账号状态/覆盖效果均为闭集校验；时间窗与阈值不许查询覆盖（L1046-1049）｜通过 |
| GEN-005 授权与最小权限 | 适用｜提权防护 `requireActorCanGrant`（L789-801）覆盖建角色、改模板增码、套模板、临时授权、员工单独授予、解除角色；岗位映射与定岗走 `requireActorCanGrantImmediate`（高危由挂起单审批）；模板 apply 校验模板归属（`applyPermissionTemplate` 内 A1-05 段）；岗位映射保存限平台管理员（L2062）；生命周期 approve/reject 限最高权限（L526-528）。AuthorizationService 在主工作区有已暂存未发布改动，但本条结论只依赖 `isPlatformAdmin`/`requireProjectScope`/`effectivePermissions` 三个方法的既有语义，origin/main 版同名方法均存在（基线二编译通过）｜通过 |
| GEN-006 数据完整性 | 适用｜授权窗口 `validFrom < validUntil`（L1540-1543、L1678-1680）；金额整数分非负（L1302-1304、L1856-1858）；币种仅 CNY｜通过 |
| GEN-007 并发与幂等 | 适用｜申请/审批/范围变更先 `lockGrantAuthorizationFacts()`；审批前比对指纹；`findPositionRoleTemplate` 带 FOR SHARE；状态机二次操作：已存档再存档→INVALID_STATE、已恢复再恢复→INVALID_STATE（`archivedAt == null`）、非 pending 申请再审→404、删除复核链未走完模板状态不变；restore 的两步写在同一幂等动作闭包、同一事务内，替代映射检查在任何写之前｜通过 |
| GEN-008 接口与模式一致性 | 适用｜AccessRepository 40 个 default 方法中 38 个有 JdbcAccessRepository 覆盖；未覆盖的 2 个（`listPendingGrantRequests`、`listUserIdsAssignedToPosition`）是建立在已覆盖方法之上的组合，Jdbc 下行为成立（OBS-5 记其全表扫描）｜通过（登记 OBS-5） |
| GEN-009 错误处理与恢复 | 适用｜异常统一 ApiException 带中文文案；`codeOf` 对非 ApiException 记类名；`grantPolicy` JSON 解析失败→409 中文；未见吞错或伪成功｜通过 |
| GEN-010 安全与敏感信息 | 适用｜口令只传给 `credentialProvisioner`，审计只记是否已建凭据；报错不回显口令；报错文案里带权限码/账号 id/模板 id（OBS-6，属操作对象标识，不是内部堆栈）｜通过（登记 OBS-6） |
| GEN-011 可观测与审计证据 | 适用｜成功事件与失败事件计数见探针 2；`changePositionTemplateLifecycle`、`updateGrantPolicy` 现有失败事件（003）；`grantSummary`、`effectivePermissionsForUser` 两个读口无查看事件（登记）；带事务方法的失败事件真库留不住（XM-M031-B 登记）｜通过（登记两项） |
| GEN-012 资源与性能边界 | 适用｜`listUserIdsAssignedToPosition` 默认实现先取全部账号再逐个查定岗（N+1）、`listPendingGrantRequests` 取全表再过滤；均只在平台管理员保存岗位映射/范围变更时触发，complex_test 现 5 条映射，未见首次正常使用下的可复现超时，不为理论规模立项（OBS-5）｜通过 |
| GEN-013 构建配置与依赖 | 适用｜只依赖 Spring、Jackson、JDK；`new ObjectMapper()` 就地创建（L1752、L1916、L1928）功能正确｜通过 |
| GEN-014 数据库迁移正确性 | 无本面｜本模块无迁移文件；库内约束只读核对：`uq_position_role_templates_active_scope`（组织+岗位+职级，仅 active）与 `findPositionRoleTemplate` 的组织优先、全局兜底查询一致 |
| GEN-015 界面可用性与可访问性 | 适用｜面向操作者的报错全部中文（探针 3：无纯英文文案）；事件名/错误码为英文机器标识，不是界面文字｜通过 |
| GEN-016 文档可验证性 | 适用｜方法注释逐条对照实现；L2117 起「保存岗位模板后立即按新映射重算在岗人员派生权；停用则收回派生」现有测试执行；新增两处注释（M031-001、M031-003）与实现一致｜通过 |
| GEN-017 业务来源分类 | 适用｜未见另造业务规则；岗位映射保存时 `organizationId` 恒写 null（L2092，即接口只能建全局映射），与库内按组织唯一的索引并存，HTML 对「按组织设岗位映射」沉默，不反推（OBS-3）｜通过（登记 OBS-3） |
| GEN-018 第一性原理证据 | 适用｜结论落到源码原文、两份一次性 worktree 测试输出、complex_test 只读查询与 HTML 原句｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 适用｜底座留痕：18 处幂等执行点现全部带失败留痕（003 已修；探针 2 计数 execute=18、recordFailure=18）；真库上带事务方法的失败事件仍随回滚丢失，属已登记的 XM-M031-B（→M092）｜通过（XM-M031-B 登记） |
| PGM-008 员工和手机 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「员工和手机」的对象、状态或界面文字 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「空间和点位：所有业务都要知道“发生在哪”」的对象、状态或界面文字 |
| PGM-024 影响业务 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「影响业务」的对象、状态或界面文字 |
| PGM-026 主要功能 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「主要功能」的对象、状态或界面文字 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「员工和手机：人、私人手机、工作手机要能对应起来」的对象、状态或界面文字 |
| PGM-033 主要功能 | 适用｜定岗、改职级、范围变更三条写路径与治理地图「保存岗位、职责或…所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效」原句一致；岗位映射保存后的重算两分支现有测试执行（002 已补，变异核对能红）｜通过 |
| PGM-034 工作手机和企业微信 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「工作手机和企业微信」的对象、状态或界面文字 |
| PGM-035 提成发放和人员服务治理 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 3 个文件不读写、不展示、不声称该对象（定岗时的基本工资只作字段透传，不是薪资单） |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | 无本面｜本模块 3 个文件是权限/账号后台接口、仓储接口与服务，不承载、不展示、不声称「设备管理：谁领了什么设备，设备能不能用」的对象、状态或界面文字 |
| PGM-048 商户侧：在授权业务范围内全开放 | 适用｜商户侧边界：`requireSameOrganization` L2159-2170 同时卡项目与商户；`createAccessUser` L1169-1178 商户主体只能在本商户建号；`canViewGrantRequest` 商户主体只见本商户申请｜通过 |
| PGM-059 客人与员工的个人信息保护 | 适用｜个人信息：账号审计元数据（L1247-1257）只记类型、归属、是否有同意凭据与凭据是否已建，不记口令、手机号；薪资只经定岗/改职级写路径传递，不进审计元数据｜通过 |
| PGM-066 已拍板规则和待排期问题 | 适用｜已拍板规则：删除岗位模板须先存档、填写理由、最高权限审批后软删除（`deletePositionRoleTemplate` L481-483 恒拒、`changePositionTemplateLifecycle` L485-548 四动作）；受益人与申请人不得自审（L952-957）；申请指纹钉住授权事实（L1714-1757）｜通过 |

## 5. 已定稿 001、002、003 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。XM-M031-A/B/C 登记未变。

## 6. 本次 Findings

无新增。

## 7. 观察（登记）

- 同 B2-r1 定稿的登记项；另加 §3 末的一条（停用→存档→恢复后在岗人员派生不自动恢复，偏保守）。

## 8. 结论

席甲 B5-r1 票：**PASS**。3 文件全读、52 判据逐项、取证脚本实跑复核；未改任何已跟踪文件；落盘前未读 Grok 本次正文。
