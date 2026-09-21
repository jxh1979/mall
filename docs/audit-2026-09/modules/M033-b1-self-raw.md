CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M033 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T06:59:28Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main 124902b7。
- 模块 M033「功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第3段」13 文件 229 行，52 判据。B3 累计 0。13 个文件与 origin/main 逐字节相同，不受未发布工作流影响。
- 顺序：Grok 已先派（modules/M033-b1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件；未对数据库执行写语句。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| AccessGrantApplyRequest.java | 48 | 1766 | 26907d4bdd81d538b6a2a08619919b1ab37ced7de85120b7015ffae531d0bea8 | 全读 |
| AccessGrantReviewRequest.java | 12 | 264 | a12c80e8d3cd44db2ea5e0827d88666467307e3facf5e455cb85990e671d3d19 | 全读 |
| AccessUserRequest.java | 36 | 1287 | 51ffd29cb122c336d1a964f29d7572569ff09257d4ecf19c7b8a546f00cf0fb3 | 全读 |
| PermissionTemplateApplyRequest.java | 13 | 305 | da109c37fefc054678c70ab15e7c739c86f280a9e814d16ed47012b4407d3090 | 全读 |
| PermissionTemplateRequest.java | 19 | 479 | ce2fcc2dfe284bd2f847d1fcd85bbff1b910db71862f266630bdd639284a70ca | 全读 |
| PositionRoleTemplateRequest.java | 12 | 281 | e154bff2c91cf9a9a7a5751dee44decd7904e6638d89dc67bcf36363fe088999 | 全读 |
| RoleAssignmentRequest.java | 9 | 190 | 00e229f0511e24f168068827b6a39348b8d5a325ae9c1731e6d9a455cfe02ea9 | 全读 |
| RoleRequest.java | 14 | 319 | bdd4a6297423cda981f62693a5a6879c6f08e50948d30933a8fc949bc460027b | 全读 |
| StaffLevelAssignmentRequest.java | 12 | 297 | c569cd7da131ee7c7850ff3e8c368a3b80ed5505f49f50280b8c43a9a25fc031 | 全读 |
| StatusChangeRequest.java | 11 | 227 | 6d4ac51fe553cb890897d6d609fb5b5eff5cb311dd6852978751457a4bcd3dd6 | 全读 |
| TemporaryGrantRequest.java | 17 | 410 | 6ea363657bdb78e4f427d6b03592e9c4a00e7d930e908b79ad6d8276d47b2f49 | 全读 |
| UserPermissionOverrideRequest.java | 13 | 387 | a871b6b46c7889e1cc020b8d37865dc5a92d7835f3a3a39281d287f11065bab0 | 全读 |
| UserPositionAssignmentRequest.java | 13 | 328 | ae0f2cc0e0575b5196489347fe1428e21eea53e4675c22a111107a41d994f0d1 | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m033_b1_run.log）

```
START=2026-09-21T06:57:48Z HEAD=e456ba96 origin/main=124902b7
== 0. files (lines/bytes/sha256 | status | vs origin/main)
AccessGrantApplyRequest.java 48 1766 26907d4bdd81d538b6a2a08619919b1ab37ced7de85120b7015ffae531d0bea8 | [] | []
AccessGrantReviewRequest.java 12 264 a12c80e8d3cd44db2ea5e0827d88666467307e3facf5e455cb85990e671d3d19 | [] | []
AccessUserRequest.java 36 1287 51ffd29cb122c336d1a964f29d7572569ff09257d4ecf19c7b8a546f00cf0fb3 | [] | []
PermissionTemplateApplyRequest.java 13 305 da109c37fefc054678c70ab15e7c739c86f280a9e814d16ed47012b4407d3090 | [] | []
PermissionTemplateRequest.java 19 479 ce2fcc2dfe284bd2f847d1fcd85bbff1b910db71862f266630bdd639284a70ca | [] | []
PositionRoleTemplateRequest.java 12 281 e154bff2c91cf9a9a7a5751dee44decd7904e6638d89dc67bcf36363fe088999 | [] | []
RoleAssignmentRequest.java 9 190 00e229f0511e24f168068827b6a39348b8d5a325ae9c1731e6d9a455cfe02ea9 | [] | []
RoleRequest.java 14 319 bdd4a6297423cda981f62693a5a6879c6f08e50948d30933a8fc949bc460027b | [] | []
StaffLevelAssignmentRequest.java 12 297 c569cd7da131ee7c7850ff3e8c368a3b80ed5505f49f50280b8c43a9a25fc031 | [] | []
StatusChangeRequest.java 11 227 6d4ac51fe553cb890897d6d609fb5b5eff5cb311dd6852978751457a4bcd3dd6 | [] | []
TemporaryGrantRequest.java 17 410 6ea363657bdb78e4f427d6b03592e9c4a00e7d930e908b79ad6d8276d47b2f49 | [] | []
UserPermissionOverrideRequest.java 13 387 a871b6b46c7889e1cc020b8d37865dc5a92d7835f3a3a39281d287f11065bab0 | [] | []
UserPositionAssignmentRequest.java 13 328 ae0f2cc0e0575b5196489347fe1428e21eea53e4675c22a111107a41d994f0d1 | [] | []
== 1. 全局 JSON 命名策略
backend/src/main/resources/application.yml:16:    property-naming-strategy: SNAKE_CASE
backend/src/main/java/com/complex/platform/common/api/ApiResponse.java:7: * 统一响应包络。Jackson SNAKE_CASE 序列化 → success / code / message / data / request_id / server_time / timestamp
== 2. 各组件后端读取（按访问器名；共享名另列精确行）
== AccessGrantApplyRequest  used-by: AccessController.java, AccessService.java
== AccessGrantReviewRequest  used-by: AccessController.java, AccessService.java
== AccessUserRequest  used-by: AccessController.java, AccessService.java
   phone                        reads= 0 
== PermissionTemplateApplyRequest  used-by: AccessController.java, AccessService.java
== PermissionTemplateRequest  used-by: AccessController.java, AccessService.java
   sourceTemplateId             reads= 0 
== PositionRoleTemplateRequest  used-by: AccessController.java, AccessService.java
== RoleAssignmentRequest  used-by: AccessController.java, AccessService.java
== RoleRequest  used-by: AccessController.java, AccessService.java
== StaffLevelAssignmentRequest  used-by: AccessController.java, AccessService.java, StaffService.java
== StatusChangeRequest  used-by: AccessController.java, AccessService.java
== TemporaryGrantRequest  used-by: AccessController.java, AccessService.java
== UserPermissionOverrideRequest  used-by: AccessController.java, AccessService.java
== UserPositionAssignmentRequest  used-by: AccessController.java, AccessService.java
-- evidenceAttachmentIds: backend/src/main/java/com/complex/platform/access/AccessService.java:548 backend/src/main/java/com/complex/platform/access/AccessService.java:989 backend/src/main/java/com/complex/platform/access/AccessService.java:1224 
-- templateId: backend/src/main/java/com/complex/platform/access/AccessService.java:1740 backend/src/main/java/com/complex/platform/access/AccessService.java:1749 backend/src/main/java/com/complex/platform/access/AccessService.java:1750 backend/src/main/java/com/complex/platform/access/AccessService.java:1994 backend/src/main/java/com/complex/platform/access/AccessService.java:2028 backend/src/main/java/com/complex/platform/access/AccessService.java:2102 backend/src/main/java/com/complex/platform/access/AccessService.java:2104 
-- sourceTemplateId: 
-- phone: 
-- finalApproverId: backend/src/main/java/com/complex/platform/access/AccessService.java:690 backend/src/main/java/com/complex/platform/access/AccessService.java:928 
== 3. 向后兼容构造器的调用方（主代码 / 测试）
AccessGrantApplyRequest( main=0 test=14
AccessUserRequest( main=0 test=10
== 4. 口令与手机号去向
logger usage in AccessController/AccessService: backend/src/main/java/com/complex/platform/access/AccessController.java:0 backend/src/main/java/com/complex/platform/access/AccessService.java:0 
1210:                        requireText(request.password(), "password");
1228:                        credentialProvisioner.provisionCredential(saved.id(), request.password(), saved.status(), now);
1236:                                accessUserAuditMetadata(saved, request.password() != null && !request.password().isBlank())
80:        String scopedId = changedScope ? "caller:" + hashJson(java.util.List.of(scope, operationId)) : legacyId;
85:        String requestHash = hashJson(request);
221:    private String hashJson(Object value) {
232:            return objectMapper.writeValueAsString(value);
== 5. 契约字段对照（openapi-p0.yaml components.schemas）
AccessGrantApplyRequest @L36584: DTO 有而契约无=['final_approver_id']；契约有而 DTO 无=[]；契约 required=['idempotency_key', 'request_type', 'target_user_id']
AccessGrantReviewRequest @L36669: DTO 有而契约无=[]；契约有而 DTO 无=[]；契约 required=['idempotency_key', 'decision']
AccessUserRequest @L31895: DTO 有而契约无=[]；契约有而 DTO 无=[]；契约 required=['username', 'account_type', 'organization_id']
PermissionTemplateApplyRequest @L29405: DTO 有而契约无=[]；契约有而 DTO 无=[]；契约 required=['target_user_ids', 'template_id']
PermissionTemplateRequest @L29336: DTO 有而契约无=[]；契约有而 DTO 无=[]；契约 required=['template_name', 'permission_codes']
PositionRoleTemplateRequest @L36504: DTO 有而契约无=[]；契约有而 DTO 无=[]；契约 required=['idempotency_key', 'position_code', 'staff_level_id', 'role_id', 'template_id']
RoleAssignmentRequest @L36479: DTO 有而契约无=[]；契约有而 DTO 无=[]；契约 required=['idempotency_key', 'user_id', 'role_id']
RoleRequest @L29287: DTO 有而契约无=[]；契约有而 DTO 无=[]；契约 required=['role_name', 'permission_codes']
StaffLevelAssignmentRequest @L33368: DTO 有而契约无=['user_id']；契约有而 DTO 无=[]；契约 required=['staff_level_id']
StatusChangeRequest @L28556: DTO 有而契约无=[]；契约有而 DTO 无=[]；契约 required=['idempotency_key', 'action', 'reason']
TemporaryGrantRequest @L29447: DTO 有而契约无=[]；契约有而 DTO 无=[]；契约 required=['target_user_id', 'permission_codes', 'valid_until']
UserPermissionOverrideRequest: 契约内无同名 schema
UserPositionAssignmentRequest @L36545: DTO 有而契约无=['user_id']；契约有而 DTO 无=[]；契约 required=['idempotency_key', 'position_code', 'staff_level_id']
== 6. 既有测试（主工作区）
MVN_EXIT=0
   AccessControllerTests tests=13 errors=0 skipped=0 failures=0
== 7. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T06:57:52Z
```

说明：探针 2 的 `reads=` 按访问器名在 access 包主代码里统计 `request.x()`/`r.x()`；共享名（evidenceAttachmentIds、templateId 等）另列精确行号后人工归到具体 DTO——`evidenceAttachmentIds` 的 3 处读取分别属于 StatusChangeRequest（L548）、AccessGrantReviewRequest（L989）、AccessUserRequest（L1224）；`templateId` 的读取都属于 AccessGrantRequestRecord 与 PositionRoleTemplateRequest，没有一处属于 PermissionTemplateApplyRequest。

## 3. 三线审计

- 第一性原理线。用途：权限后台各写口的请求体形状。实际行为：全局蛇形命名策略下自动映射；必填校验在服务层；两个带用户 id 的端点由控制器用路径 id 覆盖请求体。未经验证的前提：①「DTO 上有的字段后端都会用」——不成立，见 OBS-1/2，但 HTML 未要求这些动作留证据或回显这些字段；②「契约与 DTO 一致」——有 3 处字段差与 1 个缺失 schema，契约属 M392。
- 顺从式线：52 判据逐项见 §4；13 文件全读。
- 对抗式线（grilling）：请求体里塞别人的 userId→被路径 id 覆盖；套模板时请求体 templateId 与路径不一致→后端只认路径，契约却标必填（OBS-2）；给 `phone` 传值→被忽略；给授权申请附证据→被丢弃、审批人看不到（HTML 未要求，OBS-1）；口令会不会进日志→控制器/服务层无日志点；驼峰键会不会也被接受→全局策略下只认蛇形，`AccessUserRequest` 的别名与蛇形同名，无第二种写法。

## 4. 52 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BEQR-157 员工状态 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「员工状态」的对象、状态、计算或界面文字 |
| BEQR-182 考勤日结果 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「考勤日结果」的对象、状态、计算或界面文字 |
| BEQR-183 打卡方式 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「打卡方式」的对象、状态、计算或界面文字 |
| BEQR-184 请假类型 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「请假类型」的对象、状态、计算或界面文字 |
| BEQR-185 请假单状态 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「请假单状态」的对象、状态、计算或界面文字 |
| BEQR-186 薪资单构成 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「薪资单构成」的对象、状态、计算或界面文字 |
| BEQR-187 员工提成发放 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「员工提成发放」的对象、状态、计算或界面文字 |
| BSM-001 整体说明：这套系统到底管什么 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「整体说明：这套系统到底管什么」的对象、状态、计算或界面文字 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 适用｜「权限分岗」各写口的入参形状：角色、权限模板、岗位映射、定岗、职级、临时授权、授权申请与审批、员工单独授予/拒绝、生命周期动作各有一个记录类，字段与已闭环 M031 服务层读取一致（探针 2）｜通过 |
| BSM-009 基础管理 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「基础管理」的对象、状态、计算或界面文字 |
| BSM-028 主要功能 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「主要功能」的对象、状态、计算或界面文字 |
| BSM-029 和谁有关 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「和谁有关」的对象、状态、计算或界面文字 |
| BSM-073 主要功能 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「主要功能」的对象、状态、计算或界面文字 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「员工手机端：排班、考勤、请假、薪资，自己手机上都能办」的对象、状态、计算或界面文字 |
| BSM-085 日常工作 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「日常工作」的对象、状态、计算或界面文字 |
| BSM-086 排班、考勤与请假 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「排班、考勤与请假」的对象、状态、计算或界面文字 |
| BSM-087 我的薪资 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「我的薪资」的对象、状态、计算或界面文字 |
| BSM-088 员工提成、活动执行与人员服务边界 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「员工提成、活动执行与人员服务边界」的对象、状态、计算或界面文字 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「各类入口：不同人只看自己该看的、只做自己该做的」的对象、状态、计算或界面文字 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「相互关系：为什么这些子系统必须连在一起」的对象、状态、计算或界面文字 |
| BSM-120 已确认规则和实施前输入项 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「已确认规则和实施前输入项」的对象、状态、计算或界面文字 |
| GEN-001 必要性与最小性 | 适用｜13 个记录类 229 行；后端从不读取的组件 3 个：`AccessUserRequest.phone`（且 JsonIgnore）、`PermissionTemplateRequest.sourceTemplateId`、`PermissionTemplateApplyRequest.templateId`；另有 5 个 DTO 的 `evidenceAttachmentIds` 无读取（OBS-1/2）。两个向后兼容构造器只剩测试在用（14、10 处）｜通过（登记 OBS-1、2、5） |
| GEN-002 可执行正确性 | 适用｜全局 `property-naming-strategy: SNAKE_CASE`（application.yml:16），记录组件驼峰↔接口蛇形自动映射；`AccessUserRequest` 的 `@JsonAlias` 与全局策略同名，冗余无害；AccessControllerTests 13/13 绿（探针 6）｜通过 |
| GEN-003 测试有效性 | 适用｜DTO 无逻辑；由 AccessControllerTests（JSON 反序列化与门控）与 AccessServiceTests 间接执行｜通过（间接） |
| GEN-004 信任边界校验 | 适用｜DTO 不带 Bean Validation 注解，必填与闭集校验全部在服务层（`requireText`、`requireKnownPermissions`、`normalizeDecision` 等，M031 已审）；控制器上的 `@Valid` 对这些记录类是空操作（OBS-7）｜通过 |
| GEN-005 授权与最小权限 | 适用｜`/users/{{id}}/staff-level` 与 `/users/{{id}}/position` 的请求体虽有 `userId` 组件，控制器一律用路径 id 重建请求（AccessController L262-300），请求体里的 userId 不生效，不存在「路径审 A、实际改 B」｜通过 |
| GEN-006 数据完整性 | 无本面｜无计算、无时间或金额运算（金额与时间字段只是透传） |
| GEN-007 并发与幂等 | 无本面｜不可变记录类，无状态与并发面 |
| GEN-008 接口与模式一致性 | 适用｜13 个 DTO 对契约 components.schemas：12 个有同名 schema，字段差异 3 处——`final_approver_id` 契约无、`user_id`（两个）契约无且由路径提供；`UserPermissionOverrideRequest` 契约内无 schema（端点本就不在契约，XM-M031-C）；契约把 `PermissionTemplateApplyRequest.template_id` 标为必填而后端不读（探针 5）。契约文件属 M392，登记 OBS-3/4｜通过（登记） |
| GEN-009 错误处理与恢复 | 无本面｜不抛不吞异常 |
| GEN-010 安全与敏感信息 | 适用｜记录类自动 `toString` 会带出口令，但控制器/服务层无任何日志输出点；幂等层只存请求 JSON 的 SHA-256（JSON 内含随机幂等键），不存明文（探针 4）｜通过（登记 OBS-6） |
| GEN-011 可观测与审计证据 | 无本面｜不产生审计事件 |
| GEN-012 资源与性能边界 | 无本面｜无性能面 |
| GEN-013 构建配置与依赖 | 适用｜只依赖 JDK 与 Jackson 注解｜通过 |
| GEN-014 数据库迁移正确性 | 无本面｜不是迁移 |
| GEN-015 界面可用性与可访问性 | 无本面｜无界面文字 |
| GEN-016 文档可验证性 | 适用｜两条类注释（AccessGrantApplyRequest 的 M087 最终审批人、UserPermissionOverrideRequest 的 effect 取值）与服务层实现一致｜通过 |
| GEN-017 业务来源分类 | 适用｜未见另造业务字段；HTML 对「授权类动作是否须附证据」沉默，不因 DTO 留了 `evidenceAttachmentIds` 就反推必须落库（OBS-1）｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到源码原文、探针输出与契约原文｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「整体定位：平台治理层不是一个业务部门，而是所有业务的底座」的对象、状态、计算或界面文字 |
| PGM-008 员工和手机 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「员工和手机」的对象、状态、计算或界面文字 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「空间和点位：所有业务都要知道“发生在哪”」的对象、状态、计算或界面文字 |
| PGM-024 影响业务 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「影响业务」的对象、状态、计算或界面文字 |
| PGM-026 主要功能 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「主要功能」的对象、状态、计算或界面文字 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「员工和手机：人、私人手机、工作手机要能对应起来」的对象、状态、计算或界面文字 |
| PGM-033 主要功能 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「主要功能」的对象、状态、计算或界面文字 |
| PGM-034 工作手机和企业微信 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「工作手机和企业微信」的对象、状态、计算或界面文字 |
| PGM-035 提成发放和人员服务治理 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「提成发放和人员服务治理」的对象、状态、计算或界面文字 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「设备管理：谁领了什么设备，设备能不能用」的对象、状态、计算或界面文字 |
| PGM-048 商户侧：在授权业务范围内全开放 | 无本面｜本模块是 13 个请求记录类（纯入参形状），不承载、不展示、不声称「商户侧：在授权业务范围内全开放」的对象、状态、计算或界面文字 |
| PGM-059 客人与员工的个人信息保护 | 适用｜`AccessUserRequest.phone` 带 `@JsonIgnore`，接口不接收手机号；口令只经 `request.password()` 传给凭证服务（探针 4），控制器与服务层无日志输出（logger 0 处）｜通过 |
| PGM-066 已拍板规则和待排期问题 | 适用｜`AccessGrantApplyRequest.finalApproverId`（M087：发起单据时指定最终审批人）有读取（AccessService L690、L928）；`StatusChangeRequest` 的 action/reason/证据三件套支撑「先存档、填理由、最高权限审批」｜通过 |

## 5. 本次 Findings

无。

## 6. 观察（登记，不立项）

- OBS-1：5 个 DTO（AccessGrantApplyRequest、PermissionTemplateApplyRequest、PermissionTemplateRequest、RoleRequest、TemporaryGrantRequest）留有 `evidenceAttachmentIds`，后端不读、不落库；HTML 对这些动作是否须附证据沉默，不反推。
- OBS-2：`PermissionTemplateApplyRequest.templateId`、`PermissionTemplateRequest.sourceTemplateId` 后端不读（一律用路径 id）；契约把前者标为必填。
- OBS-3（→M392）：契约 `AccessUserRequest` 列有 `phone`，DTO 以 `@JsonIgnore` 拒收。
- OBS-4（→M392）：契约缺 `final_approver_id`；缺 `UserPermissionOverrideRequest` schema（同 XM-M031-C）。
- OBS-5：两个向后兼容构造器主代码 0 处调用，只剩测试在用。
- OBS-6：记录类自动 toString 含口令；现无日志输出点。
- OBS-7：DTO 无 Bean Validation 注解，控制器 `@Valid` 为空操作；校验在服务层。

## 7. 结论

席甲 B1 票：**PASS**（无工程缺陷；OBS-1…7 登记）。13 文件 229 行全读、52 判据逐项、三线完成；未改任何文件；落盘前未读 Grok 本次正文。按 V0.17：即使两席 B1 均无发现，也须经 B2 登记定稿、跳过 B3/B4、B5 全量确认后才收口。


## M033 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-21T07:21:39Z。输入：modules/M033-b1-grok-raw.md V0.01（35607 bytes，SHA256 e9097cb55ae9e1f8e2221aed1cd3376f6a043b870a853c0e6566f2ffa941a771）、modules/M033-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M033 B2-r1 整批候选」段（全文件 SHA256 d6b3b353e90dbf779907d94cfdd6695008a0166ad8b3e70d6abaea61e3dc94be）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-01→001、GR-02→002、GR-03→003；Grok O-1（契约缺 final_approver_id）→XM-M033-C；其余两席观察全部登记；无漏并、无改写。
- 自审纠正：席甲 B1 把 phone 与证据附件只记成观察，没有拿契约原文去对（契约写明 phone 会哈希入库、mode 三枚举与实读不符）；三条事实成立，工程缺陷「是」，B1 票 PASS 撤回为 FAIL。
- 处置意见：三条的缺陷所在都不在本模块 13 个文件——001 说谎的是契约描述，DTO 的 JsonIgnore 是 M006-025 的既定处置且有回归测试靠该组件钉住；002 三方里只有契约枚举不对；003 是全项目统一信封，删 5 处既不改变任何可观察行为，又要动已闭环 M031。故队列「否」、转 XM。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
