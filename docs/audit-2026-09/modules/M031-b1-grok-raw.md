CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M031 B1 席乙（Grok）修复前全量审计

- 报告版本：V0.01（本路径原先不存在，本次创建）
- 席位：乙 / Grok 4.6 xhigh / 独立发现，未读 `modules/M031-b1-self-raw.md`
- 提示词：160-audit-prompt.md V0.17 阶段 B1
- 模块：`plan/modules.md` V0.04 M031「功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第1段」
- 生命周期：ledger 已 IN_PROGRESS；本席只写本 raw，不改 ledger/findings/计划
- 审计对象：`/home/james/complex-management` main 主工作区**现状文件**（含未推提交与已暂存改动）
- 结论：**FAIL**（有成立的工程缺陷；见 GR-01、GR-03）
- 时间：2026-09-21T04:27:27Z / 宿主 y9000p-ai

## 0. 环境事实（本席实算）

| 项 | 实算 |
|---|---|
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466` |
| origin/main | `508b2e87ca43a0e40f1fa81aaf1561aa9ba7def6`（PR #471 已合并） |
| criteria-lock.md | V0.11；`sha256sum` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 sidecar、ledger 顶行同值，本席独立实算，未抄期待值） |
| LOCK-AMEND-COUNT | ledger 顶行 1（只读） |
| AccessController.java | 332 行 / SHA256 `fbdb4667cf1a0811adae1e668dc299b42a07a82ba5abda879cc098bca0c60146` / 文件版本 V0.05 |
| AccessRepository.java | 298 行 / SHA256 `6fc11453fee5aefbfe97bb270d4f6f88a294101daaab88c62abcf768338b6cca` / 文件版本 V0.09 |
| AccessService.java | 2361 行 / SHA256 `f38bff0b90f42703756a4069fafe560a16b834e8c30b34b9bb1b6a57d3e580bb` / 文件版本 V0.20 |
| 合计 | 2991 行 |
| 相对 origin/main | 3 文件 `64 insertions, 3 deletions`。未推提交 `124cc4b7`（岗位派生重算、有效权限与范围闸）、`523be51e`（岗位模板保存 `@Transactional`）。另 AccessService **已暂存**：V0.19→V0.20，`changePositionTemplateLifecycle` 在 `requirePlatformAdmin` 前加 `isMasterAuthority` 放行 |
| AuthorizationService.java | **别的模块**。工作区已暂存：`isMasterAuthority` 改为 MASTER 账号类型（不再要求 platform_admin 桥）；`currentMerchantScope()` **无商户即抛** `MERCHANT_SCOPE_REQUIRED`。`git show origin/main:...currentMerchantScope`：平台管理员桥返回 `Optional.empty()`。**本模块 AccessService 未调用 `currentMerchantScope()`**；列表/写路径用 `isPlatformAdmin` + `actor.merchantId()` / `requireProjectScope`。凡依赖授权服务的结论已同时核 origin/main 并在此注明 |
| 未读 | `M031-b1-self-raw.md`；未 fetch/commit/push/merge/deploy；未改源码/测试；未对库执行写语句 |

本模块 3 文件相对 origin/main 的现行差（摘）：AccessController 将 `GET /users/{id}/effective-permissions` 从直接 `authorizationService.effectivePermissionsForUser` 改为 `service.effectivePermissionsForUser`（同组织闸）；AccessRepository 增 `listUserIdsAssignedToPosition`；AccessService 增岗位模板保存后重算、范围变更目标闸、`@Transactional`、以及已暂存的最高权限生命周期放行。

## 1. 技能预检（Grok 环境实核，缺则记缺口）

按提示词 §6 与 runbook：开工前读技能全文，按收窄口径用；不派子代理、不等操作员。

| 技能 | 本席环境 | 本轮用法 |
|---|---|---|
| using-superpowers | 有 `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 |
| using-arc | 有 | 路由：本任务是审计席乙，不走 Arc 修复流 |
| graft | 有 CLI；对 Controller/Repository 跑了 `graft skeleton`（约 2379+1271 tokens） | 范围已被用户钉死三文件且要求全文读不抽样，骨架后仍全文 Read，不抽样结案 |
| grilling / grill-me | 有；grill-me 指向 grilling | **只借追问结构自问自答**；本体「等用户回答 / dispatch sub-agent」作废 |
| batch-grill-me | **缺口** | 未冒充调用 |
| ubiquitous-language | **缺口** | 未冒充调用 |
| codebase-design | 有 | 用 module/interface/seam 看 AccessService↔AccessRepository↔Jdbc |
| diagnosing-bugs / systematic-debugging | 有 | 只借读错误→复现→追数据流；**不改代码、不加探针** |
| verification-before-completion | 有 | PASS/FAIL 前先跑命令 |
| brainstorming / writing-plans | 有 | B1 不用出方案；收窄为自问自答，不问操作员 |
| test-driven-development / tdd | 有 | B1 禁止修复，不落新测试 |
| requesting-code-review | 有 | 本体派子代理作废；本席即外审 |
| receiving-code-review | 有 | B1 不修复 |
| ponytail / ponytail-review | 有 | 找可删；不扫全仓 ponytail-audit |
| i-have-adhd | 有（`disable-model-invocation`） | 对操作员回执：无开场白；**结论全文只在本 raw** |
| git-guardrails-claude-code | 有 | 本轮无 git 写操作 |
| resolving-merge-conflicts / writing-for-agents | 有 | 无冲突；本 raw 给下游 B2 读 |
| long-running-background-tasks | 有 | Maven 测试前已读；前台跑完 |
| 明确不用 | executing-plans / dispatching-parallel-agents / subagent-driven-development / using-git-worktrees / finishing-a-development-branch / research / ponytail-audit | 按提示词划掉；**未调用 MCP、未派子代理、无第三席** |

## 2. 全文覆盖（三文件，不抽样）

`graft skeleton` 仅作目录；覆盖以 Read 全文为准。

### 2.1 AccessController.java（L1–L332）

`@RequestMapping("/api/v1/access")`。每个端点的权限门、幂等、是否经 Service：

| 端点 | 权限门 | 幂等 | 审计（成功/失败） | 备注 |
|---|---|---|---|---|
| GET `/permission-templates` | `access.permission_template.view` | GET 无 | Service 成功事件 | 平台管理员 org=null 见 GR-01 |
| POST `/permission-templates` | `access.permission_template.manage` | `requireIdempotencyKey` | Service 成/败 | |
| PUT `/permission-templates/{id}` | 同上 | 有 | 成/败 | |
| POST `.../apply` | `access.permission_template.apply` | 有 | 成/败 | 高危码 `GRANT_REQUIRES_APPROVAL` |
| POST `.../copy` | manage | 有 | 成/败 | |
| GET `/permissions` | `access.permission.view` | GET | 成功 | 权限目录 |
| GET `/roles` | `access.role.view` | GET | 成功 | GR-01 |
| POST `/roles` | `access.role.manage` | 有 | 成/败 | |
| POST `/role-assignments` | `access.role.assign` | 有 | 成/败 | |
| POST `/role-assignments/unassign` | assign | 有 | 成/败 | |
| GET `/grant-policy` | `access.grant_request.view` | GET | 读政策，无单独失败审计 | |
| POST `/grant-policy` | `admission.policy.manage` | 有 | 成功；失败无 `recordFailure` | 观察 |
| POST `/grant-requests` | `access.grant_request.apply` | 有 | 成/败 | |
| GET `/grant-requests` | view | GET | 成功；Jdbc `pageGrantRequests` **允许** org=null | |
| POST `/grant-requests/{id}/review` | `access.grant_request.review` **且** `governance.supervisor_review.approve` | 有 | 成/败 | 非 pending 再审 → 找不到 |
| GET `/grant-requests/summary` | view | GET | **Service 无 recordSuccess/Failure** | 观察 |
| GET `/position-role-templates` | `access.role.assign` | GET | 成功 | GR-01 |
| POST/PUT `/position-role-templates` | assign | 有 | 成/败 | 另 `requirePlatformAdmin` |
| POST `.../lifecycle` | assign；approve/reject_delete 再加 supervisor_review.approve | 有 | 成功（失败走幂等抛错，无统一 recordFailure） | 已暂存最高权限放行 |
| DELETE `.../{id}` | assign | 无（恒冲突） | Service 直接 `ARCHIVE_APPROVAL_REQUIRED` | 状态机二次 DELETE 仍拒 |
| POST `/temporary-grants` | `access.temporary_grant.create` | 有 | 成/败 | 控制器还校验失效时间 |
| POST `.../revoke` | `access.temporary_grant.revoke` | 有 | 成/败 | |
| GET `/users` | `access.user.view` | GET | 成功 | 平台管理员走无参 `listUsers()`，不踩 GR-01 |
| POST `/users` | `access.user.manage` | 有 | 成/败 | 非外包须注册审批 |
| GET `/effective-permissions` | **无 requirePermission**；`currentActor` 未登录抛 UNAUTHENTICATED | GET | 在 AuthorizationService | self 端，注释写明不消费单次授权 |
| GET `/users/{id}/effective-permissions` | `access.user.view` | GET | 经 Service 同组织闸（相对 origin/main 的现行差） | |
| GET `/staff-levels` | `access.staff_level.view` | GET | 成功 | |
| PUT `/users/{id}/staff-level` | manage | 有 | 成/败 | 已定岗改职级走定岗重算 |
| PUT `/users/{id}/position` | assign | 有 | 成/败 | |
| PUT `/users/{id}/scope` | assign | 有 | 成/败 | 现行差：`requireActorCanAssignToScope` |
| POST `/users/{id}/permission-overrides` | assign | 有 | 成/败 | |
| POST `.../remove` | assign | 有 | 成/败 | |

报错文案抽查：控制器与 Service 抛给用户的 `ApiException` 消息为中文；`code` 为内部标识（PGM-001 允许的四样之一）。未发现把内部栈/SQL 回显给调用方。

### 2.2 AccessRepository.java（L1–L298）

接口 + 默认方法。注释契约与 Jdbc 实现对照（Jdbc 属 M032，只读追证）：

- `listTemplates(null)` 注释与默认：**平台管理员全量**（转无参 `listTemplates()`）。Jdbc **覆盖后** `requireProjectScopeId(null)` 抛 PROJECT_SCOPE_REQUIRED（GR-01）。
- `listRoles(String)` 默认同样 null=全量；Jdbc 覆盖同样拒绝 null。
- `listUsers(null)` 默认全量；Jdbc 无参 `listUsers()` 另实现，Service 平台管理员走无参，此条不炸。
- `listPositionRoleTemplates` 默认直接 `List.of()`（桩）；Jdbc 覆盖且拒绝 null。
- `listUserIdsAssignedToPosition` 默认：`listUsers()` + 逐人 `findUserPositionAssignment`。Jdbc **未覆盖**，生产走 N+1；Jdbc **有** `findUserPositionAssignment`，重算功能不是死代码。观察 GEN-012。
- 写方法默认 `UnsupportedOperationException`（中英混）；Jdbc 均有覆盖。
- `Effect` {GRANT, DENY}、`Source` {ROLE, TEMPLATE_APPLICATION, TEMPORARY_GRANT, USER_OVERRIDE} 与 Jdbc `findPermissionEffects` 五段查询一致（角色、岗位派生角色、模板应用、临时授权、员工覆盖）。
- `markSingleUsePermissionConsumed` 接口约定原子消耗。生产表 `temporary_permission_grants.grant_group_id` 存在（`psql \d`）；M006JdbcRegressionTests 自建 TEMP 表缺该列导致 1 ERROR，属测试夹具，不立项为生产缺列。

库内约束（`psql` SELECT，complex_test）：

- `staff_levels`：30 行，M1–M10 × lower/middle/upper，`level_rank` 1–30；CHECK main/sub/rank；UNIQUE (main,sub)、(rank)。
- `position_role_templates`：status CHECK `active|inactive`；UNIQUE 部分索引 `uq_position_role_templates_active_scope` 仅 `status='active'`。
- `access_grant_requests`：status CHECK `pending|applied|rejected`；type CHECK 含 `user_role, user_position, temporary_permission, role_permission, user_scope_change, position_high_risk_release`。Service `normalizeGrantRequestType` 不接受 `role_permission`（更严）。`user_scope_change` / `position_high_risk_release` 与 V392 闭集一致。
- `user_permission_overrides` PK `(user_id, permission_code, effect)` 与「同一 (user, code, effect) 幂等覆盖」注释一致。

### 2.3 AccessService.java（L1–L2361）

计算链（对照治理地图权限规则原文，即使 PGM-040/041 未映射到本模块 52 项，仍作第一性 HTML 依据）：

- **最终权限**不在本文件里算，委托 `AuthorizationService.computeEffectivePermissions`：GRANT 来源并集 − DENY；平台管理员桥把日常码（active、无需审批、low/medium）并入 grants。`EffectivePermissions.wildcard` 构造恒为 `false`，故本文件 `requireActorCanGrant` 的 `if (self.wildcard()) return;` **死分支**。origin/main 与工作区此点相同。追证：工作区 `isMasterAuthority` 不再等于平台管理员桥，最高权限 self 有效集不加日常通配。
- **岗位模板保存**（`savePositionRoleTemplate`）：`requirePlatformAdmin`（最高权限不能走这条，与「平台管理员配置模板」一致）；立即 `recomputeUsersOnPositionTemplate`；停用则 `replacePositionGrants` 清空派生；启用则对在岗人员 `assignUserPositionDirect(..., true)`（普通立即生效，高危挂起）。
- **定岗** `assignUserPositionDirect`：普通码随保存生效；`grantApprovalEnabled`（生产构造器写死 true）时高危 `holdHighRiskPositionPermissions`（显式 DENY + `position_high_risk_release` 待审）。独立 DENY 不复用挂起。
- **范围变更** `changeUserScope`：旧派生立即 `replacePositionGrants` 清空；旧高危挂起作废并删挂起 DENY；非派生 GRANT（角色/模板/临时/覆盖）冻结撤销，显式拒绝保留；改归属；整单进主管链。审批落地 `assignUserPositionDirect(..., false)`——高危已在范围变更审批里按最高风险取档，不再二次挂起。现行差增加 `requireActorCanAssignToScope`（非平台管理员不能改挂他项目/他商户）。
- **授权申请/复核**：`applyGrant` 锁表；临时权限无论日常/高危都进链；角色/岗位空审批码则 `GRANT_APPROVAL_NOT_REQUIRED`。`reviewGrant` 只吃 `pending`；受益人/申请人自审拒绝；指纹不一致拒绝；链未完返回原 pending（不落地）；通过才 `applyApprovedGrant`。再审已 applied → 找不到。符合「第二次不能当第一次过」。
- **生命周期**：archive/restore/request_delete/approve_delete/reject_delete；删除须最高权限；占用检查；U 盘本人 request_delete 可直接删。**restore 走 `withLifecycle` 把 status 写死 inactive（GR-03）**。
- **直接 DELETE** 恒 `ARCHIVE_APPROVAL_REQUIRED`。
- **临时授权 POST**：`requireGrantApprovalIfNeeded` 只拦 `requiresApproval=true` 的码；日常码可直接落地。员工申请走 `applyGrant` 的 `dailyTemporary` 才进一级主管链。HTML 写的是「员工可申请…主管审批」，未禁止管理员直接授日常临时权 → **不按冲突立项**（见对抗式）。
- `slug()` L2313–2318 无调用。观察 GEN-001。
- 失败审计：`codeOf` 只把 `ApiException.code` 或类名写入审计元数据，不回显给 HTTP 正文。

## 3. 第一性原理三问

### 3.1 为什么必须存在？删掉哪条 HTML 做不到？

本三段是后台权限域的写入口与仓储契约：权限点目录、角色、权限模板、岗位↔职级映射、临时授权、高危授出审批、员工覆盖（授予/拒绝）、职级三十档与定岗、跨商户/跨项目改挂。删掉则治理地图「权限按单一功能最小颗粒度」「角色模板 + 员工自定义」「最终权限 = 角色 + 基础模板 + 有效临时 + 单独授予 − 显式拒绝」「岗位变更立即重算」「跨商户跨项目每次先审批」「职级 M1–M10 上中下」以及业务地图「管理员工、岗位、职级、权限」在本后台面无法执行。答得上 → 有业务上位。HTML 沉默处（例如列表分页默认 50、政策 JSON 存 `supervisor_review_policies`）走 2.1 细化/技术性，不发明需求。

### 3.2 声称的行为有没有被实际执行验证？

| 声称 | 证据 |
|---|---|
| 职级三十档闭集 | `psql` complex_test：`staff_levels` count=30，rank 1–30，CHECK 与 UNIQUE。类型 4 |
| 授权申请类型/状态闭集 | `psql` CHECK 与 Service 常量一致（另库允 `role_permission`，API 不收）。类型 4 |
| 控制器门控与中文校验 | `AccessControllerTests` 13/13 绿；临时授权缺失效时间中文「必须填写授权失效时间」。类型 2 |
| 岗位存档/恢复/最高权限删 | `M006SecurityRegressionTests` 39/39 绿；其中恢复后 **断言 status=inactive**（把 GR-03 钉成现状）。类型 2 |
| Jdbc 集成主路径 | `AccessJdbcIntegrationTests` 24/24 绿。类型 2 |
| 复核落地（定岗高危释放、日常临时申请批准、角色申请批准等） | `AccessServiceTests` 50 例中 **6 ERROR**：`reviewGrant` → `SupervisorReviewService.approve` 「必须登记直接受益人」。夹具 `InMemorySupervisorReviewRepository` **未实现** `scheduleSupervisorReview`，`findSupervisorReviewSchedule` 空。AccessService.`blockGrantRequest` 生产路径会把 `targetUserId` 传入 `blockForAdmission(..., beneficiaryId)`。**不能把这 6 个 ERROR 写成生产复核不可用**；只能写测试网当前不能证明复核落地。类型 2（失败输出） |
| 平台管理员列出模板/角色/岗位映射 | **无**命中 Jdbc 覆盖方法的绿测。`listTemplates(null)` 契约只在接口默认与内存假仓成立。类型 4：见 GR-01 命令摘录 |
| 单次消耗 | 生产表有 `grant_group_id`；回归测试 TEMP 表无该列 → 1 ERROR。不立项缺列 |

### 3.3 哪个前提未验证就当真？

1. 「接口 `organizationId==null` 表示平台管理员全量」——被 Jdbc 覆盖打破（GR-01）。
2. 「取消存档/恢复后模板可再用于定岗」——`withLifecycle` 恒写 `inactive`，`findPositionRoleTemplate` 要求 `status='active'`（GR-03）。
3. 「AccessServiceTests 绿 = 复核链可用」——当前 6 ERROR，夹具未存受益人日程。
4. 「`currentMerchantScope` 工作区改动影响本模块列表」——本模块未调用该方法；origin/main 平台管理员桥返回空，工作区无商户即抛。已注明，未据此立项。
5. 「`EffectivePermissions.wildcard()==true` 时授权人可绕过持有集」——构造恒 false，死代码。

## 4. 顺从式：52 项判据

映射（lock 原文）：BEQR 7 + BSM 14 + PGM 13 + GEN 18。每项读了定义全文与 HTML 原文。**适用** = 本三文件有对应实现面须核；**无本面** = 本段不承载该锚点的可执行行为（不因此扩锁、不发明需求）。

核心权限专章 **PGM-040～044、PGM-009** 未出现在 M031 映射（适用标签多写 governance）。本席仍用其 HTML 原文作法理，**不把未映射 ID 当冻结判据**，也不走 LOCK-AMEND。

| ID | 适用? | 理由与证据 |
|---|---|---|
| GEN-001 | 适用 | 三文件有业务上位；`slug()` 无引用（观察，不单独立项） |
| GEN-002 | 适用 **FAIL** | GR-01 列表主路径；GR-03 恢复后不能定岗。声称与执行不一致 |
| GEN-003 | 适用 | 有测试网；`AccessControllerTests`/`M006SecurityRegressionTests`/`AccessJdbcIntegrationTests` 绿；`AccessServiceTests` 6 ERROR 使复核落地断言未执行。测试通过不替代行为 |
| GEN-004 | 适用 | 输入经 `requireText`/`requireKnownPermissions`/状态闭集；幂等键；路径 id 与 body 重组 |
| GEN-005 | 适用 **FAIL** | 写入口均服务端 `requirePermission`（self 有效集除外）。平台管理员被 Jdbc 项目闸误拒，等于日常配置权限不可用（GR-01） |
| GEN-006 | 适用 | 定岗/范围变更/申请复核 `@Transactional`；指纹防审中偷换；占用未清不能删 |
| GEN-007 | 适用 | 写路径 `idempotencyService`；`lockGrantAuthorizationFacts` SHARE；单次消耗接口约定原子 UPDATE。DELETE 无幂等但恒冲突 |
| GEN-008 | 适用 **FAIL** | 接口「null=全量」与 Jdbc 覆盖不一致（GR-01）；申请类型闭集与库 CHECK 对齐 |
| GEN-009 | 适用 | 失败抛中文 ApiException，无伪成功；生命周期非法状态 `INVALID_STATE` |
| GEN-010 | 适用 | 跨组织/商户闸；提权 `GRANT_EXCEEDS_OWN_PERMISSIONS`；不回显内部栈。密码只进凭证供给，审计只记 `credential_provisioned` 布尔 |
| GEN-011 | 适用 | 多数写有成/败审计；`grantSummary`、`updateGrantPolicy` 失败路径弱（观察） |
| GEN-012 | 适用 | `listUserIdsAssignedToPosition` 默认全表 N+1（观察，首次有人的模板保存可复现但未失败） |
| GEN-013 | 无本面 | 无构建脚本 |
| GEN-014 | 无本面 | 无迁移文件；约束只追证 |
| GEN-015 | 无本面 | 无 UI。职级 `sub_level` 库内 upper/middle/lower 属内部标识，展示中文化不在本三文件 |
| GEN-016 | 适用 | 注释「null=平台管理员全量」「生产恒开审批」须可复现；前者与 Jdbc 不符（GR-01），后者生产构造器 `grantApprovalEnabled=true` 可复现 |
| GEN-017 | 适用 | 本报告按冲突→细化→技术性→添加分类；未把 HTML 沉默当删除令 |
| GEN-018 | 适用 | 每条 finding 落到四类之一 |
| BEQR-157 员工状态 | 无本面 | 枚举待入职/在职/试岗/停岗/离职。本段 `AccessUser.status` 为 active/invited/disabled（账号），且非外包创建被 `STAFF_REGISTRATION_REQUIRED` 拦住 |
| BEQR-182 考勤日结果 | 无本面 | 无考勤写/读 |
| BEQR-183 打卡方式 | 无本面 | |
| BEQR-184 请假类型 | 无本面 | |
| BEQR-185 请假单状态 | 无本面 | |
| BEQR-186 薪资单构成 | 无本面 | 只存职级绑定的薪资大范围+具体分；不出薪资单 |
| BEQR-187 员工提成发放 | 无本面 | |
| BSM-001 整体说明 | 适用（弱） | 「谁能操作」底座；无冲突 |
| BSM-003 通用能力/权限分岗 | 适用 **FAIL** | 标题含权限分岗与可改可删。GR-01/GR-03 打在这伞下 |
| BSM-009 基础管理卡片 | 适用（弱） | 「员工、权限」入口卡片 |
| BSM-028 基础管理主要功能 | 适用 | 「管理员工、岗位、班次、职级、权限」。职级三十档+岗位模板在本面 |
| BSM-029 和谁有关 | 适用 | 「用户权限要知道职级和审批状态」 |
| BSM-073 商户管理主要功能 | 适用（弱） | 「授权范围内操作…员工」。本段有商户账号列表闸 |
| BSM-084 员工手机端 | 无本面 | 手机端功能不在这三文件 |
| BSM-085 日常工作 | 无本面 | 注册两条路径仅在创建账号时拒绝手工员工（细化到 STAFF_REGISTRATION_REQUIRED） |
| BSM-086 排班考勤请假 | 无本面 | |
| BSM-087 我的薪资 | 无本面 | |
| BSM-088 提成/人员服务边界 | 无本面 | |
| BSM-114 各类入口 | 适用（弱） | 「不同人只看自己该看的」；本段做后台 API 隔离 |
| BSM-115 相互关系 | 适用（弱） | 「基础管理连所有业务 / 权限」 |
| BSM-120 已拍板 | 适用 | 「员工权限按单一功能最小颗粒度」——权限目录+模板+覆盖写路径 |
| PGM-001 治理层定位 | 适用 **FAIL** | 资格/权限/全中文报错/先成功幂等。GR-01 使平台管理员日常资格落空 |
| PGM-008 员工和手机卡片 | 无本面 | 卡片级；人员注册/手机不在本段实现 |
| PGM-022 空间点位 | 无本面 | |
| PGM-024 影响业务 | 无本面 | 「班次责任区域」非本段 |
| PGM-026 商户主体主要功能 | 适用（弱） | 「合同、结算、权限引用同一绑定」；本段按 organization/merchant 闸 |
| PGM-032 员工和手机 h2 | 无本面 | 手机绑定/换机不在本段 |
| PGM-033 主要功能（注册路径） | 适用（窄） | 手工建员工账号被拒绝，只留外包；与「须先注册审批」一致 |
| PGM-034 工作手机企微 | 无本面 | |
| PGM-035 提成发放 | 无本面 | |
| PGM-036 设备管理 | 无本面 | |
| PGM-048 商户侧全开放 | 适用（窄） | 商户操作员 `listUsersByMerchant`；跨商户写拒绝 |
| PGM-059 个人信息保护 | 无本面 | 无客人查询/明文手机号；创建用户不存手机摘要（回归测试声称） |
| PGM-066 已拍板 | 适用 | 与 BSM-120 同条权限颗粒度；权限点预置不在本段扩枚举 |

## 5. 对抗式（grilling 结构，自问自答）

❓ **Q1 平台管理员打开权限模板列表，第一次会怎样？**  
➡️ AccessService 因 `isPlatformAdmin` 把 `orgId=null` 传入 `listTemplates`。接口默认会转全量；Jdbc 覆盖先 `requireProjectScopeId(null)` → `PROJECT_SCOPE_REQUIRED`。角色列表、岗位映射列表同构。**第一次正常操作就失败。** → GR-01

❓ **Q2 取消存档后能否马上按该岗位定人？**  
➡️ `restore` 调用 `withLifecycle(..., status="inactive")`。列表过滤只看 `archivedAt/deletedAt`，恢复后重新出现在默认列表；`findPositionRoleTemplate` 要求 `status='active'`。定岗找不到映射。`M006SecurityRegressionTests` 还断言恢复后 inactive。**第一次「存档→恢复→定岗」失败。** → GR-03

❓ **Q3 临时授权能否不经主管？**  
➡️ 员工申请 `applyGrant`+`temporary_permission`：日常也进一级链（测试在复核步因夹具 ERROR，申请步代码在）。管理员 `POST /temporary-grants`：只拦 `requiresApproval`。HTML 句子是「员工可申请…主管审批」，未禁止管理员直接授日常临时权。不立项。

❓ **Q4 高危能否借岗位模板免审批？**  
➡️ 不能：定岗拆成普通立即 + 高危 DENY 挂起。范围变更整单按最高风险进链，落地 `enforceApproval=false` 不再挂起——审批已经覆盖高危。与「不得因在模板内免除授出前审批」不冲突。

❓ **Q5 复核第二次当第一次过？**  
➡️ `reviewGrant` 过滤 `pending`；applied/rejected 再审 not found。链中途 approve 返回原 pending。代码如此；本席未能用绿测跑完整落地（夹具）。

❓ **Q6 跨项目改挂？**  
➡️ 现行 `requireActorCanAssignToScope`：非平台管理员不能改到他项目/他商户。平台管理员可。HTML 跨项目每次先审批：范围变更一律进链，包括平台管理员。

❓ **Q7 `currentMerchantScope` 工作区改动会不会让平台管理员看申请列表变空/抛错？**  
➡️ 本模块分页列表不调用它，平台管理员 org/merchant 传 null，Jdbc `pageGrantRequests` 允许 null。origin/main 桥返回空 vs 工作区抛错 **不构成本模块列表缺陷**。

❓ **Q8 通配授权人？**  
➡️ `requireActorCanGrant` 看 `wildcard()`，但 AuthorizationService 构造恒 false。平台管理员靠 grants 里并入的日常码，不能直接授高危（须申请）。与 BSM-007「资金/敏感/删除/最终审批不因平台管理员自动获得」同向。

❓ **Q9 英文报错？**  
➡️ Service 消息中文。ControllerTests 里 mock 的 `"permission denied"` 是测试桩，不是生产文案。

❓ **Q10 可删？**（ponytail-review）  
➡️ `slug()` 死方法；`wildcard()` 短路死分支。净减很少，不是本轮主缺陷。

## 6. Findings（临时 GR；正式 ID 待 B2）

分类顺序：冲突 → 细化 → 技术性 → 添加。

### GR-01 — 平台管理员列出权限模板/角色/岗位映射首次即 PROJECT_SCOPE_REQUIRED

- **文件:行**
  - `AccessService.java:106-109`（模板列表；角色 `291-294`、岗位映射 `411-414` 同构）
  - `AccessRepository.java:19-28`（契约：Null = 全量）
  - 追证 `JdbcAccessRepository.java:73-75`、`307-309`、`416-418`、`1473-1475`
- **原文**
  ```
  String orgId = authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor);
  List<PermissionTemplate> templates = repository.listTemplates(orgId);
  ```
  接口：「Lists templates visible to the given organization. Null = platform admin (all templates).」
  Jdbc：`requireProjectScopeId(organizationId);` 且 `organizationId == null` → `ApiException.forbidden("PROJECT_SCOPE_REQUIRED", "此操作必须在项目范围内进行")`。
- **更长上下文**：`listUsers` 平台管理员改走无参 `listUsers()`，故账号列表不炸；`pageGrantRequests` SQL 允许 `:organization_id IS NULL`，申请列表不炸。唯模板/角色/岗位映射三条列表走「null 表示全量」却撞上 Jdbc 硬闸。
- **正常触发**：以 `accountType=platform_admin` 且已登录的主体 GET `/api/v1/access/permission-templates`（或 `/roles`、`/position-role-templates`）。HTML：平台管理员配置岗位权限模板、拥有日常功能。第一次打开配置页即 403。
- **影响**：日常权限治理入口对平台管理员不可用；内存假仓/Mockito 不覆盖 Jdbc 该方法，单测绿、生产/Jdbc 路径未覆盖。
- **最小拟处置**：平台管理员改调无参 `listTemplates()`/`listRoles()`；Jdbc `listPositionRoleTemplates` 在 `organizationId==null` 时不 `requireProjectScopeId` 并查出全部（或与接口默认对齐）。能改调用点就不要扩抽象。
- **四分类**：冲突（先于细化）。与 PGM-001「再管资格：员工是否有权限」、BSM-003「权限分岗」、映射外但原文存在的「平台管理员拥有全部日常功能权限 / 配置各岗位权限模板」矛盾。
- **工程缺陷是否成立**：是
- **冻结 ID**：PGM-001，BSM-003，GEN-002，GEN-005，GEN-008
- **2.3 依据**：类型 4（`nl -ba` 上列行）+ 类型 1（PGM-001 / BSM-003 HTML）+ 类型 2（无 Jdbc 列表绿测；`AccessJdbcIntegrationTests` 24 绿但不打这条路径）

### GR-03 — 岗位模板 restore 后 status 仍为 inactive，不能再用于定岗

- **文件:行**
  - `AccessService.java:504-508` restore 分支
  - 追证 `PositionRoleTemplate.java:30-32` `withLifecycle` 把 status 写死 `"inactive"`（M032，只读）
  - `JdbcAccessRepository.java:449-453` `findPositionRoleTemplate` 要求 `status = 'active' AND archived_at IS NULL AND deleted_at IS NULL`
- **原文**
  ```
  case "restore" -> {
      ...
      updated = current.withLifecycle(null, null, null, null, now);
  }
  ```
  `withLifecycle`：`new PositionRoleTemplate(..., "inactive", ..., archived, deleted, ...)`
- **更长上下文**：`listPositionRoleTemplates` 默认只滤 `archivedAt==null && deletedAt==null`，恢复后重新出现在列表。定岗 `resolvePositionGrant` 用 `findPositionRoleTemplate` 要 active。UNIQUE 部分索引只约束 active 行，恢复保持 inactive 可避开与替代 active 行冲突，但「列表可见却不能选」是用户可复现的失败。
- **正常触发**：archive（或删后 restore）→ 对同一 `positionCode/staffLevelId` 定岗。`M006SecurityRegressionTests` 615-617 行断言恢复后 `status().isEqualTo("inactive")`（39 例全绿，把该行为钉死）。
- **影响**：HTML「存档…取消存档即可」一次恢复（BSM-003 伞下 / BSM-004 原文，后者未映射）做不到；须再 PUT 把 status 改 active 才定岗。
- **最小拟处置**：restore 在无替代 active 行时写回 `active`；若存在替代 active 行则明确冲突中文，而不是静默 inactive。
- **四分类**：细化（可改可删的下位实现；能指到 BSM-003/存档恢复语义）。不是添加。
- **工程缺陷是否成立**：是
- **冻结 ID**：GEN-002，BSM-003
- **2.3 依据**：类型 2（M006SecurityRegressionTests 断言 inactive）+ 类型 4（nl 源码）+ 类型 1（BSM-003 可改可删；BSM-004「取消存档即可」作对照原文，不扩锁）

### 观察（不立项）

- **O-1** `POST /temporary-grants` 日常码不经主管链；员工申请走 `applyGrant`。HTML 未禁止管理员直接授日常临时权。
- **O-2** `grantSummary` 无审计事件；`updateGrantPolicy` 无失败 `recordFailure`。
- **O-3** `slug()` 无引用；`requireActorCanGrant` 的 `wildcard()` 短路死代码。
- **O-4** `listUserIdsAssignedToPosition` 默认 N+1；首次有人的模板保存可走通，未失败。
- **O-5** `AccessServiceTests` 6 ERROR 因测试夹具无 `ReviewSchedule.beneficiaryId`；生产 `blockGrantRequest` 有传受益人。不把夹具缺口写成生产复核失效。
- **O-6** `M006JdbcRegressionTests` TEMP 表缺 `grant_group_id` 与现行 Jdbc SQL 不一致；公共表该列存在。
- **O-7** 映射未含 PGM-040～044（权限规则/职级规则正文）。本席用 HTML 原文审计算链，未启用锁外判据。
- **O-8** AuthorizationService 工作区 `currentMerchantScope` 与 origin/main 不同；本模块未调用。`isMasterAuthority` 工作区与平台管理员分立，和已暂存生命周期放行一致。
- **O-9** 直接 DELETE 岗位模板恒冲突，二次仍冲突，状态机正确。
- **O-10** 高危挂起不复用独立 DENY；移除会使高危生效的 DENY 须审批。

无「添加」项。无 HTML 自身问题需铁律 4 暂停。

## 7. 命令与测试摘要（类型 2/4）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

psql complex_test（只 SELECT）：staff_levels=30；grant/type/status CHECK 见上。

mvn -o -Dtest=AccessServiceTests,AccessControllerTests,M006SecurityRegressionTests test
# AccessControllerTests 13/13
# M006SecurityRegressionTests 39/39
# AccessServiceTests 50，Errors 6（reviewGrant / BENEFICIARY_REQUIRED / 夹具）
# 合计 102，Errors 6，exit 1

mvn -o -Dtest=M006JdbcRegressionTests,AccessJdbcIntegrationTests test
# 仅导出 COMPLEX_DATABASE_URL（库名改为 complex_test）、USERNAME、PASSWORD
# AccessJdbcIntegrationTests 24/24；Flyway 396 migrations，schema 428
# M006JdbcRegressionTests 12，Errors 1（TEMP 表无 grant_group_id）
```

未 source `/etc/complex/complex.env`；未打印口令。

## 8. 结论

- 三文件全文覆盖完成。
- 52 项逐条适用/无本面已记录。
- 三线已跑。
- 成立工程缺陷：GR-01（冲突）、GR-03（细化）。
- **本席 B1：FAIL**。无添加项、无 HTML 修改申请、无未答 questions。B2 归并前不得修复。

（CLI 不附机器标记。完整报告到此为止。）


## M031 B2-r1 整批候选复核（席乙票，V0.02）

- 时间：2026-09-21T04:48:49Z。席位：乙 / Grok 4.6 xhigh / 只读外审，正本 `/home/james/complex-management` main 主工作区。
- 输入：`modules/M031-b1-grok-raw.md` V0.01（33364 bytes，SHA256 `6e6f5e32715f1c48d964c94b042ba17300e72c5a08a8d3668ad51d1591b7805d`，本席 B1：FAIL，GR-01、GR-03，观察 O-1…O-10）；`modules/M031-b1-self-raw.md` V0.01 封存前缀（34141 bytes，SHA256 `29d4f0d1db873f374d86972fcb331352d41211182e9b0a56ac4a5616e3127e08`，席甲 B1：FAIL，CX-M031-001、CX-M031-002，OBS-1…10）。席甲 raw 现文件更长，本票只读该 34141 字节前缀，未采信其后追加的 B2 结论段。
- 候选：`docs/audit-2026-09/findings.md` V2.97 顶部「当前 M031 B2-r1 整批候选」段（全文件 SHA256 `9ac5dfef77a1b0bb9e8d213d0bbd59427a5324a19644ded9ed1d5deef953bafb`）。B1 后四值仍全等 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（本席本步只读复核，不写新的独占 CRITERIA-LOCK-SHA256 行）。
- 范围：只审本批归并 / 分类 / 队列 / 最小修复边界，不重新审模块。未改源码 / 文档 / HTML / lock / findings / ledger；未 fetch / commit / push / merge / deploy；未写新产物；未对任何数据库执行写语句；未用 MCP 或子代理。探针日志与探针测试只读。
- 本席 B2-r1 票：**PASS**（整批候选同意）。

### 完整差集

| 正式ID / 登记号 | 本次来源（不改写原意） | 候选分类 / 缺陷 / 队列 | 本席票 |
|---|---|---|---|
| M031-001 | 本席 GR-03（席甲 B1 漏：读到 restore 分支但未追 `withLifecycle` 恒写 inactive） | 细化 / 是 / 是 | **同意入队** |
| M031-002 | 席甲 CX-M031-001（本席 O-4 只记 N+1 与「首次有人可走通」，未论测试面） | 细化 / 是 / 是 | **同意入队** |
| M031-003 | 席甲 CX-M031-002 ＋ 本席 O-2 的 `updateGrantPolicy` 失败留痕缺口（`grantSummary` 读口仍只登记） | 技术性 / 是 / 是 | **同意入队** |
| XM-M031-A（→M032） | 本席 GR-01 | 跨模块只登记，不入本模块队列 | **同意登记，不同意在 M031 入队** |
| XM-M031-B（→M092） | 席甲 OBS-9，B2 真库探针复现 | 跨模块只登记，不入本模块队列 | **同意登记；不同意在 M031 摘 `@Transactional`** |
| XM-M031-C（→M392） | 席甲 OBS-8 | 跨模块只登记 | **同意登记** |
| （无新 ID） | 两席其余观察 | 只登记不入队 | **同意；三项漏并只补登记、不升队、不否决整批** |

冲突入队 0。细化入队 2（001、002）。技术性入队 1（003）。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001、002、003（3）；本批新开「否」ID = 0。跨模块登记 3。B1 两席均 FAIL，合取 FAIL，本批不撤回任何 B1 票。

**整批候选：同意。**

---

### 一、差集核实（逐项独立）

**一、GR-03 采纳为 001。票：同意入队；同意只改 `AccessService.java`，不动 M032 的 `PositionRoleTemplate.java` 与 `JdbcAccessRepository.java`；同意两步写；Jdbc 路径成立；无更小的本模块改法。**

核源（不重审模块，只核候选处置能否在 Jdbc 下成立）：

- restore 现状：`AccessService.java:504-508` `current.withLifecycle(null, null, null, null, now)` → `PositionRoleTemplate.java:30-32` 把 `status` 写死 `"inactive"` → `JdbcAccessRepository.savePositionTemplateLifecycle:519-526` SQL 亦恒 `SET status = 'inactive'`，同时按入参写 `archived_at` / `deleted_at` / 理由列，并在 `archived_at IS NULL` 时把 `archived_by` 置空。
- 定岗查找：`findPositionRoleTemplate:449-461` 要求 `status = 'active' AND archived_at IS NULL AND deleted_at IS NULL`，`FOR SHARE`。恢复后列表可因 `archived_at` 清空而重新出现，定岗仍找不到。`M006SecurityRegressionTests.java:617` 断言 `status().isEqualTo("inactive")`，把该现状钉住。与本席 B1 GR-03 同一事实。
- upsert 会不会动生命周期列：`JdbcAccessRepository.savePositionRoleTemplate:467-485` `INSERT (id, position_code, staff_level_id, role_id, template_id, organization_id, status, created_by, created_at, updated_at) ON CONFLICT (id) DO UPDATE SET` 只更新岗位/职级/角色/模板/组织/`status`/`updated_at`。**不写 `archived_at`、`deleted_at`、`delete_review_id`、`delete_reason`、`archived_by`、`deleted_by`。**
- 因此「只 upsert、不先走 lifecycle」在 Jdbc 下不成立：`status` 可变 `active`，但库内 `archived_at` 仍在，`findPositionRoleTemplate` 继续空。内存假仓 `AccessServiceTests` 的 `put` 会整对象替换、反而能藏掉这条 Jdbc 缺口，不能当更小改法。
- 候选两步顺序（先 `savePositionTemplateLifecycle` 清存档/删除标记并被 SQL 写成 inactive，再 `repository.savePositionRoleTemplate` 按同一 `id` upsert 把 `status` 写成 `active`）在 Jdbc 下成立：第一步已把 `archived_at`/`deleted_at` 置空，第二步只补 `status`，不回头改生命周期列。顺序不可反：lifecycle SQL 恒写 `inactive`，后调会把 upsert 的 `active` 盖掉。
- 替代映射检查：`findPositionRoleTemplate` 只认 active 且未存档/未删除，故检查发生在任何写之前时**不会命中自己**（当前行仍是 archived/inactive）。命中且 `organizationId` 与 `current` 相同（含双方皆 null）→ 409，零写，状态不变。命中的是组织兜底全局行而 `current` 是组织行时，候选「organizationId 相同」不触发 409，与部分唯一索引 `uq_position_role_templates_active_scope (COALESCE(organization_id,''), position_code, staff_level_id) WHERE status='active'` 相容（全局与组织行可并存）。
- 两步写是否同在幂等事务内：**是。** `changePositionTemplateLifecycle` 现有 `@Transactional`（L485）；两步都落在 `idempotencyService.execute` 的 action 闭包（L494-547）。`IdempotencyService.runAndComplete:167-172`：有 `TransactionTemplate` 时 `REQUIRED` 并入外层，「action(多写)+complete」同一事务；无模板时单测直跑。`markRetryableFailure` 在闭包外（L174-176），与头注「失败留痕在外」一致。409 发生在写之前，不进这两步。
- 必须走 `repository.savePositionRoleTemplate`，禁止走私有 `AccessService.savePositionRoleTemplate`（L2060-2108 会 `recomputeUsersOnPositionTemplate`）。候选已写「存档期间派生权本来就没收回，恢复不触发重算」，同意。
- 更小改法：改 `withLifecycle` 或改 Jdbc lifecycle SQL 用 `mapping.status()` 一步写完——更短，但目标都是 M032 文件，本模块检查点带不走。本模块内没有比两步写更小且 Jdbc 成立的改法。

最小处置同意候选原文。B3 实施边界（仍在候选写入集合内，不另开文件、不否决）：

1. restore 分支：先 `findPositionRoleTemplate(current.organizationId(), positionCode, staffLevelId)`，命中且 `java.util.Objects.equals(hit.organizationId(), current.organizationId())` → 409 中文「同岗位同职级已有启用中的岗位模板，须先停用或存档后再恢复」，不调用任何 save。
2. 无则照旧 `savePositionTemplateLifecycle(withLifecycle(null,null,null,null,now), actorId)`，再构造 `status="active"` 且生命周期字段为空的同一条映射，`repository.savePositionRoleTemplate` 后**返回该启用态对象**（不要返回 `withLifecycle` 的 inactive 对象）。
3. `M006SecurityRegressionTests.java:617` 改为 `active`；**同一测试**须给 `savePositionRoleTemplate` 桩把 `mapping` 更新为入参，或改断言 restore **返回值**（现桩只接 lifecycle，upsert 默认返回 null，只改 617 会假红）。
4. AccessServiceTests 两条：存档→恢复→可定岗；已有替代启用映射时恢复被拒且状态不变。内存假仓须实现 `savePositionTemplateLifecycle`（按 Jdbc 语义写 inactive + 时间戳），否则测不到「必须先 lifecycle 再 upsert」。
5. 不动 `PositionRoleTemplate.java`、`JdbcAccessRepository.java`、唯一索引、不在 restore 触发重算。

**二、GR-01 不入队 → XM-M031-A。票：同意登记为 XM-M031-A 转 M032；没有必须在 M031 入队的技术理由。**

逐点核（按用户列出的判据，本席独立实算）：

| 声称 | 本席实核 |
|---|---|
| `requireProjectScopeId` 在 `origin/main` 的 `JdbcAccessRepository.java` 内 0 处 | `git show origin/main:backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java` 后计数 **0**。origin/main 带参列表是 `if (organizationId == null) return listTemplates()` / `listRoles()` / `listUsers()`；`listPositionRoleTemplates(null)` 走无 WHERE 的全表 SELECT |
| 由本地未发布提交 `52074492` 引入 | `52074492bd36f286e13ad94e8888f494fbf52ee0` 标题「fix(access): 平台管理员不再整类关掉商户项目隔离」；Jdbc 把上述 null 回退改成 `requireProjectScopeId`。`git merge-base --is-ancestor 52074492 origin/main` 退出 1（**不在** origin/main）；对 HEAD 退出 0（主工作区有）。该提交还出现在 `origin/audit-m032-r1` 等远程分支 |
| `AccessRepository.java:19-28` 契约 Null＝平台管理员全量 | 属本模块三文件之一，原文仍在：「Null = platform admin (all templates).」角色列表 L79-82 同构。违约方是 Jdbc **覆盖**，不是本接口 |
| 本模块检查点只把 M031 三个文件带到 origin/main | `plan/modules.md` M031 FILES＝`AccessController.java`、`AccessRepository.java`、`AccessService.java`。`JdbcAccessRepository.java` 与 `PositionRoleTemplate.java` 在 M032 FILES |
| origin/main 上平台管理员列表不受影响 | AccessService 平台管理员把 `orgId=null` 传入带参列表；origin/main Jdbc 对 null 回退全量。合入本模块三文件后该路径不复现 PROJECT_SCOPE_REQUIRED |
| M032 是下一个模块，审计对象含这份未发布改动 | M032 文件列表含 `JdbcAccessRepository.java`；`52074492` 的 Jdbc 差正在该文件 |
| 先例 M025 GR-03 | findings 已定稿：Grok GR-03 的触发证据只在主工作区未发布的 M009 改动里、origin/main 0 命中，登记为跨模块、不入 M025 队列。同型：缺陷成立于「现状工作区」，但本模块检查点合入 origin/main 后不复现，违约文件属下一模块 |

本席 B1 拟处置「平台管理员改调无参 `listTemplates()`/`listRoles()`」是对**未发布 Jdbc 覆盖**的绕开。把它写进 M031 检查点，等于在 origin/main 上为一条那里不存在的闸加调用点分支；`listPositionRoleTemplates` 接口也没有无参重载，绕开还不完整。违约方是 M032 文件的未发布改动，应在 M032 恢复「null＝全量」或一并改调用点。

**明确同意：GR-01 不在 M031 入队，登记 XM-M031-A（→M032，关联该未发布提交）。** 无必须在 M031 入队的技术理由。

**三、002 是否成立。票：同意入队；O-4 是读码结论不是实际执行；测试缺口足够按 GEN-003 立项，不撤回。**

- `recomputeUsersOnPositionTemplate`（`AccessService.java:2118-2140`）两分支与 `listUserIdsAssignedToPosition`（`AccessRepository.java:149-157`）在 `AccessServiceTests.java` 内 **0 处引用、0 条断言**（对该测试文件 `rg` 无命中）。graft 调用边：生产代码只有 `savePositionRoleTemplate` → recompute → `listUserIdsAssignedToPosition`。
- `git diff origin/main -- AccessServiceTests.java` 新增的只有 `effectivePermissionsForUserRejectsCrossOrganization` 与 `changeUserScopeRejectsTargetOutsideActorProject`，与席甲一致。
- origin/main 的 AccessService **0 处** `recomputeUsersOnPositionTemplate`；该分支来自本地提交 `124cc4b7`，确是本模块相对 origin/main 的主要新增行为。
- 现有 `createPositionRoleTemplate` 测试（如 AccessServiceTests ~L509、M006SecurityRegressionTests ~L343）都是**先建映射再定岗**，走的是 `assignUserPositionDirect`，不是「已有在岗人员时改/停映射」。`scopeChangeApprovalRejectsWhenTheNewScopePositionMappingChangedWhilePending` 在待审期间 `updatePositionRoleTemplate`，当时该人尚未落地定岗（断言 `positionRoles` 为 null），recompute 扫到空列表，测不到两分支。
- GEN-003 原文：「测试须实际执行目标行为、具备能失败的断言，并覆盖本模块关键分支；测试通过本身不替代行为证据。」关键新增分支零执行，立项成立。分类细化：上位是治理地图「保存岗位、职责或…所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效」（`platform-governance-map-for-operations-v0.1.html:658`），不是纯工具注释。

本席 B1 **O-4**「首次有人的模板保存可走通，未失败」：**读码结论，不是实际执行。** 依据是默认方法存在、Jdbc 未覆盖但 `findUserPositionAssignment` 有实现，故 N+1 不是死代码；GEN-012 下 complex_test 现存量看不到首次超时。当时**没有**跑「先定岗再改/停映射」的用例，也没有断言派生角色/模板应用/高危挂起。O-4 只挡住把 N+1 当缺陷，**不能**挡住 GEN-003。无「测试缺口不够立项」的技术理由。

最小处置同意：AccessServiceTests 补两条（改映射断言派生替换且薪资不变；停用断言派生收回）。RED 若暴露实现缺陷，按缺陷最小修并在 B3 完成节写明——写入集合已含 `AccessService.java`。

**四、003 与 XM-M031-B。票：同意 003 入队；明确同意 XM-M031-B 转 M092；不同意在 M031 处置事务注解。**

核源与探针（只读）：

- `changePositionTemplateLifecycle`（L485-548）与 `updateGrantPolicy`（L1922-1937）无 `recordFailure`。本文件 `idempotencyService.execute` **18** 处，`recordFailure` **16** 处，缺的就是这两处。同文件 `createPositionRoleTemplate` / `updatePositionRoleTemplate` / `executeTemplateWrite` 已是 try/catch → `recordFailure` 再抛。
- 本席 B1 O-2 写了 `updateGrantPolicy` 无失败留痕，并把 `grantSummary` 无事件放在同一观察里。候选把**写路径失败留痕**并进 003，把 `grantSummary` / `effectivePermissionsForUser` 读口留在只登记（席甲 OBS-4），不改写 O-2 原意。
- `@Transactional`：现行 AccessService **11** 处（origin/main **9** 处；多出的 2 处是 `createPositionRoleTemplate`/`updatePositionRoleTemplate`，提交 `523be51e`）。`assignUserRole`（L341）**无**注解且 catch 里有 `recordFailure`；`assignUserPosition`（L556）**有**注解且 catch 里有 `recordFailure`。
- `AuditEventService.record`（L73-129）无 `@Transactional`、无 `REQUIRES_NEW`，经同一仓储 `save`。`JdbcAuditEventRepository` 普通 `INSERT INTO audit_events`。`IdempotencyService` L35-36 / L167-168 写明 action+complete 包进事务、**失败留痕在外**；外层再加 `@Transactional` 后 catch 里的 `recordFailure` 被并入同一事务，随业务回滚丢失。
- 探针只读：`/home/james/audit-tmp/m031_probe.log` `M031PROBE plain_failure_rows=1 transactional_failure_rows=0`（worktree＝origin/main＋本模块文件＋主工作区未发布 V427 以过 Flyway）。`M031FailureAuditProbeTests.java` 同一主体、同类失败：`assignUserRole` vs `assignUserPosition`，按 idempotency_key 数 `access.role.assign_failed` / `access.user_position.assign_failed`。与上列代码同构，本席采信该只读日志，未重跑、未对库写。
- 全后端同时含 `@Transactional` 与 `recordFailure` 的 `*Service.java`：**25** 个，与候选一致。`AccessJdbcIntegrationTests.staffLevelChangeRunsInOneTransactionLikePositionAssignment`（M006-G-07）只钉 `assignUserPosition` 与 `assignStaffLevel` 两个注解——旧派生同步失效的四步写入需要整单事务，不是失败留痕闸。

**明确同意：** 单点处置归 M092 的 `AuditEventService`（独立事务传播或等价，让失败事件不随业务回滚丢）；本模块登记 XM-M031-B，**不**逐个摘 AccessService 的 11 个注解，**不**推翻 M006-G-07。003 只把两个缺失败留痕的方法按同文件 `executeTemplateWrite` 范式补齐，并如实注明真库生效依赖 XM-M031-B。

必须在 M031 内摘注解的技术理由：**无。** 这两方法补上的 `recordFailure` 若仍在外层 `@Transactional` 内，真库一样丢；摘掉 lifecycle 的注解会让 001 的两步写失去原子性；摘掉 `updateGrantPolicy` 也不同时修复另外 9 个已有 `recordFailure` 的同型方法。最小改法不在本模块。

**五、其余观察。票：全部只登记；三项漏并补进只登记段，无应升队项。**

候选只登记段已覆盖：席甲 OBS-1 / 本席 O-5（6 个 ERROR 来自别的模块未发布改动与夹具，origin/main 基线绿）；OBS-2（检查点同批带入 AccessServiceTests、AccessControllerTests，及 001 改动的 M006SecurityRegressionTests）；OBS-3（保存映射 `organizationId` 恒 null）；OBS-4 读口无查看事件；OBS-5 / 本席 O-4 的 N+1 与 `listPendingGrantRequests` 全表过滤；OBS-6（五处 `"access-api"`）；OBS-7 / 本席 O-3 的 `slug()`；OBS-10（保存映射把已获批高危重新挂起，字面合 HTML）；本席 O-1（管理员直发日常临时授权，HTML 未禁止）；O-6（M006Jdbc TEMP 表缺 `grant_group_id`，属 M037）；O-7（映射未含 PGM-040…044，未启用锁外判据）；O-9（DELETE 恒拒、二次仍拒）；席甲 OBS-8→XM-M031-C。

漏并（只登记，不升队，不否决整批）：

1. 本席 O-3 后半：`requireActorCanGrant` 的 `wildcard()` 短路死代码（`EffectivePermissions.wildcard` 构造恒 false）。候选只写了 `slug()`。
2. 本席 O-8：`AuthorizationService.currentMerchantScope` / `isMasterAuthority` 主工作区未发布改动与 origin/main 不同；本模块列表不调用 `currentMerchantScope`。与 OBS-1 不是同一文件。
3. 本席 O-10：高危挂起不复用独立 DENY；移除会使高危生效的 DENY 须审批。与 OBS-10「保存映射重新挂起已获批高危」相邻但不是同一句，候选只收了 OBS-10。

不应升队：N+1（GEN-012，无首次超时）；读口无查看事件（HTML 未要求每次查看留痕）；OpenAPI 六路径（契约文件 M392）；`organizationId` 恒 null（HTML 沉默）；`"access-api"` 经办人（审计事件另有真实操作人）；管理员直发日常临时权（HTML 未禁止）；夹具缺列与 6 ERROR（别的模块）；DELETE 恒拒（状态机正确）；未映射的 PGM-040…044（不扩锁）。GR-01 与 OBS-9 已走跨模块登记，不回头入本模块队列。

**六、本批无添加项、无 HTML 申请、无 questions。票：同意。**

`docs/audit-2026-09/questions/` 无 `M031*`。001 不发明「恢复必须自动启用」之外的业务态；409 是把 UNIQUE 冲突说成人话，不是新规则。002 只补测试（及 RED 时的实现最小修）。003 只补同文件已有的失败留痕范式。不按铁律 4 申请改三 HTML。BSM-004「想恢复点"取消存档"即可」在 `business-system-map-for-operations-v0.1.html:569`，**不在** M031 映射（映射有 BSM-003 无 BSM-004）；候选冻结列写 BSM-003 / GEN-002，与本席 B1「BSM-004 作对照原文、不扩锁」一致。分类栏写 BSM-004 原文作上位句可以，**不得** LOCK-AMEND。

---

### 二、分类理由

四分类顺序：冲突 → 细化 → 技术性 → 添加。

- **001 细化。** 能指到业务地图可改可删 / 「取消存档即可」（BSM-003 伞下；BSM-004 原文对照、不扩锁）。不是与 HTML 不能同时成立的冲突句（HTML 未写「恢复后 status 必须仍是 inactive」）。不是添加。同意。
- **002 细化。** 上位是治理地图岗位模板变更立即重算句；缺口是该行为的测试面（GEN-003），不是无 HTML 可指的注释。同意。冻结列仅 GEN-003 足够；席甲 B1 另用的 PGM-033 已在上位句里，不要求改候选表。
- **003 技术性。** 三 HTML 未点名这两处失败事件名；依据是同文件 16:2 范式与 GEN-011 / PGM-001 底座留痕。同意不升冲突、不升细化。
- XM-A/B/C 只登记，不进四分类入队。

工程缺陷三条均为「是」。队列三「是」零「否」。

---

### 三、最小修复边界

写入集合＝`backend/src/main/java/com/complex/platform/access/AccessService.java` ＋ `AccessServiceTests.java`（M036，PENDING）＋ `M006SecurityRegressionTests.java`（M037，PENDING）。一次 B3 禁止改 `JdbcAccessRepository.java`、`PositionRoleTemplate.java`、`AuditEventService`、OpenAPI、三 HTML、锁。AccessControllerTests（M035）按 OBS-2 随检查点带入，本批无代码改动。

| ID | 允许 | 禁止 |
|---|---|---|
| 001 | 只改 restore 分支：替代映射 409；否则 lifecycle 再 upsert 同一 id 为 active 并返回启用态；改 M006 断言并补桩/改断言返回值；AccessServiceTests 两条；内存仓补 lifecycle 语义 | 改 M032 两文件；只 upsert 不清 `archived_at`；先 upsert 后 lifecycle；走私有 `savePositionRoleTemplate` 触发重算；摘 `@Transactional` |
| 002 | AccessServiceTests 两条能失败的断言（改映射 / 停用）。RED 暴露实现缺陷则最小修 AccessService 并在 B3 完成节写明 | 借机改 N+1 为定向 SQL（OBS-5，不入队）；扩组织维度岗位映射（OBS-3，HTML 沉默） |
| 003 | 两方法按 `executeTemplateWrite` 包 try/catch `recordFailure` 再抛；事件名 `access.position_role_template.lifecycle_failed`、`access.grant_policy.change_failed`；测试各一条失败落事件；完成节注明真库依赖 XM-M031-B | 摘 11 个 `@Transactional`；改 M006-G-07；在本模块给 `AuditEventService` 加 `REQUIRES_NEW`；把 `grantSummary` 读口塞进本条 |

禁止把 GR-01 改成本模块调用点绕开；禁止把 OBS-9 当本模块逐方法事务修复。

---

### 四、添加／HTML／questions

无添加。无 HTML 修改申请。无 questions。不 BLOCKED-ASK。

---

### 分类／队列／允许的最小修复边界（收口）

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。

- 001：细化／是／是。restore 先查替代 active 映射（organizationId 用 `Objects.equals`），冲突 409 且不写；否则 `savePositionTemplateLifecycle` 再 `repository.savePositionRoleTemplate` 写回 active 并返回启用态；两步留在现有 `@Transactional`＋幂等 action 内。测试：M006 L617→active 并接 upsert 桩；AccessServiceTests 两条。不动 M032。
- 002：细化／是／是。AccessServiceTests 补重算两分支。
- 003：技术性／是／是。两写方法补失败留痕；真库留得住依赖 XM-M031-B。
- XM-M031-A（GR-01→M032）、XM-M031-B（失败留痕随回滚丢失→M092）、XM-M031-C（OpenAPI→M392）只登记。
- 观察三项漏并（wildcard 死分支、AuthorizationService 未发布差、高危挂起不复用独立 DENY）只补登记，不升队。

### 结论

全批差集无原意改写。GR-03→001；CX-001→002；CX-002＋O-2 写路径→003；GR-01→XM-M031-A（明确同意不入 M031）；OBS-9→XM-M031-B（明确同意不在 M031 摘注解）。Jdbc 下 001 两步写成立，upsert 不动生命周期列，故必须先 lifecycle 再 upsert，且须同在幂等事务内——候选满足。O-4 是读码不是执行，不构成对 002 的反驳。003 补范式、事务单点归 M092。无添加、无 HTML 申请、无 questions。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M031 IN_PROGRESS；B3=0。
