CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M006 B1 Grok 原始审计报告

- 版本：V0.01
- 控制：V0.17
- 席位：Grok（审计席乙）
- 步骤：B1 修复前整模块全量只读审计
- 模块：M006 `10-backend-main-access-01`（NORMAL）
- 仓库 HEAD：`395e6fbab027020da1c38237fad17e8edcd7a132`（与派发声明一致）
- ledger 本模块状态（只读状态字段）：`IN_PROGRESS`；B3=0；方案复核已修=2；子代理失败重跑=0
- 本席独立实算 `criteria-lock.md` SHA256（全文）：`23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`
- sidecar `plan/criteria-lock.sha256`：同值
- ledger 顶部 `CRITERIA-LOCK-SHA256`：同值
- 审计结论：**FAIL**
- 互盲：未读任何 `*-raw.md`、`findings.md`、历史审计报告；未派子代理；未改源/SQL/三份业务 HTML/ledger/findings

---

## 0. 技能预检（本席环境实核）

按意图执行，未核到的不写已核实。

| 技能 | 本席路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 | 无 |
| grilling / grill-me | mattpocock `skills/productivity/grilling`、`grill-me` | 对抗式追问结构，自问自答，不派子代理、不等操作员 | 无 |
| codebase-design | mattpocock `skills/engineering/codebase-design` | 用 module/interface/seam/adapter/depth 描述本包 | 无 |
| diagnosing-bugs | mattpocock `skills/engineering/diagnosing-bugs` | 只借反馈环与数据流，不改生产代码 | 无 |
| systematic-debugging | superpowers | 只借读错误→复现→追数据流；禁止加 instrumentation | 无 |
| verification-before-completion | superpowers | PASS/FAIL 前先跑命令 | 无 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review` | 只列可删项，不修 | 无 |
| writing-for-agents | mattpocock productivity | 报告步骤与完成标准 | 无 |
| resolving-merge-conflicts | 存在 | 本步无合并冲突，未调用本体流程 | 无（节点不适用） |
| tdd / test-driven-development | 存在 | B1 禁止修复，不写测试 | 节点不适用 |
| ponytail | 存在 | 仅 B3 默认强度，本步不用 | 节点不适用 |
| git-guardrails-claude-code | 存在 | 本体要改 hook/settings；B1 不改仓库。本步未 push/reset | 未安装 hook（可选） |
| i-have-adhd | `~/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd` | 存在；完整 52 判据/全 GR 清单与「列表≤5」冲突时以审计合同为准 | 无 |
| batch-grill-me | 本席 `mattpocock-skills` 树未找到 | — | **缺口：不存在，未冒充调用** |
| ubiquitous-language | 同上未找到 | — | **缺口：不存在，未冒充调用** |
| brainstorming / writing-plans | 存在 | 阶段 A 技能，本步 B1 不用 | 节点不适用 |

划掉不用（与合同一致，未调用）：dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、executing-plans、ponytail-audit/debt/gain、research。

---

## 1. 范围与字节 manifest（独立实算）

`plan/modules.md` M006 FILES-BEGIN/END 14 文件。计量：`git ls-files -z -- <14 paths> | xargs -0 cat | wc -l` → **2988**（含空行与注释）。与逐文件 `count('\\n')` 合计一致。

| 路径 | 字节 | 行 | SHA256 |
|---|---:|---:|---|
| backend/src/main/java/com/complex/platform/access/AccessActor.java | 223 | 11 | `5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a` |
| backend/src/main/java/com/complex/platform/access/AccessController.java | 13769 | 272 | `7e2e079b981ca7e6e048ac1daae88bc55c3d8a176bcc7f493ef88133a36ddd59` |
| backend/src/main/java/com/complex/platform/access/AccessCredentialProvisioner.java | 335 | 12 | `c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b` |
| backend/src/main/java/com/complex/platform/access/AccessGrantPositionSummary.java | 232 | 10 | `08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df` |
| backend/src/main/java/com/complex/platform/access/AccessGrantRequestRecord.java | 934 | 35 | `b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89` |
| backend/src/main/java/com/complex/platform/access/AccessGrantSummary.java | 219 | 9 | `7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96` |
| backend/src/main/java/com/complex/platform/access/AccessGrantUserSummary.java | 155 | 8 | `80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e` |
| backend/src/main/java/com/complex/platform/access/AccessRepository.java | 7725 | 208 | `5213cd76819dfd11d7dacfd8d942b553955770fb434ac1f3eb7cf1ca0f893344` |
| backend/src/main/java/com/complex/platform/access/AccessService.java | 85228 | 1793 | `b4ab5f0e63d07313a7a579efdc247bde32a5ab9b2596871409d768126aba836c` |
| backend/src/main/java/com/complex/platform/access/AccessUser.java | 972 | 35 | `dd7b7b83e0646b6ffda0115960cf52adedd92076a04e56406fe0a947f6f38455` |
| backend/src/main/java/com/complex/platform/access/AuthorizationService.java | 21846 | 472 | `d91c76b8a898f383bddf3d4afc9dd013909723233f6a66676cd3c37f1fae760d` |
| backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java | 1523 | 40 | `1eb91915baa87681d5db416140a5b6715916ef8fe8986d8c7ded27a2a7f9abc8` |
| backend/src/main/java/com/complex/platform/access/EffectivePermissions.java | 882 | 23 | `68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e` |
| backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java | 2577 | 60 | `9cfab4cb9412e8c794376e1eaf6682c497f2b43c5c9e8bf289ca30d2dc2310a9` |
| **合计** | **136620** | **2988** | |

窄读（仅为这 14 个接口的实现/消费者/测试/SQL/过滤器，不把 M007 当全量审计）：

- 实现：`JdbcAccessRepository.java`（`AccessRepository` 的 JDBC adapter；`deletePositionRoleTemplate` 为物理 `DELETE`）
- 过滤器：`BearerAuthenticationFilter`（principal=`AuthService.UserRecord`）、`ProductionBearerAuthenticationFilter`（principal=`AccessActor`）、`CustomerTokenAuthenticationFilter`（构造 `AccessActor(..., "customer", ..., "active")`）
- 闸：`ActionGateAspect` 调用 `AuthorizationService.requirePermission`
- 审批：`SupervisorReviewService.blockForAdmission` / `approve`（自审拒绝在该服务，不在本 14 文件）
- 员工自助角色种子：`V322__staff_self_service_role.sql`（说明审批通过路径在 `StaffService`，不在 `AccessService.createAccessUser`）
- 表：`V003__p0_access_permission_authz_slice.sql` `access_users.status` 无 CHECK；`V086__staff_tables.sql` 员工档案不在本模块文件集
- 测试：`AuthorizationServiceTests`、`AccessServiceTests`（已跑）；`AccessControllerTests`、`AccessJdbcIntegrationTests`（源码审、未跑）
- 前端消费者：`apps/web/src/admin/pages/AccessGovernancePage.tsx` 直接展示 `recommendation` / `summary`

消费者枚举方法：`git ls-files -z` 列名后排除 `docs/audit-2026-09`、`.scratch`、`docs/_archive`、`node_modules`、`test-results`、`playwright-report`，再对显式源码/测试文件做类型名扫描。未做语义索引、未扫全仓内容搜索。

---

## 2. 第一性原理（2.3 三问）

### 2.1 这段东西为什么必须存在？

本包是权限与账号的服务端 seam：`AccessController` 暴露 `/api/v1/access/**`，`AccessService` 管模板/角色/岗位/临时授权/授权申请/用户/职级薪资，`AuthorizationService` 是全平台 `requirePermission` 与 scope 的 adapter。

删掉后做不到的 HTML（类型 1）：

- BSM-009：`管理综合体、空间、商户、合同、员工、设备、权限。它是所有业务的准入基础。`（`business-system-map-for-operations-v0.1.html:596-598`）
- BSM-028：`管理员工、岗位、班次、职级、权限、工作设备和员工注册审批。`（同文件约 L698 列表）
- BSM-114 标题：`各类入口：不同人只看自己该看的、只做自己该做的`（同文件 L1827）
- PGM-001：`再管资格` → `员工是否有权限`（`platform-governance-map-for-operations-v0.1.html:264-265`）
- PGM-008：`管理员工注册、私人手机设备绑定、工作手机、企微和岗位状态。`（同文件 L362-365；本 14 文件只覆盖注册账号与岗位模板，手机/企微不在本文件集）
- PGM-066：`员工权限按单一功能最小颗粒度配置，支持角色和员工级完全自定义。`（同文件已拍板列表）

答得上 → 有业务上位需求。不等于当前实现与原文一致。

### 2.2 声称的行为有没有被实际执行验证？

已执行（类型 2/4）：

1. 离线单元（`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`，未 source 整 env，未下依赖）：
   - `AuthorizationServiceTests`：21 tests, failures 0, errors 0
   - `AccessServiceTests`：34 tests, failures 0, errors 0
   - 合计 55 / BUILD SUCCESS
2. 内存反例 `AdminDenyHarness`：`platform_admin` 在存在 `DENY` 的 `finance.refund.create` 上 `requirePermission` **不拒绝**；`effectivePermissions.denies=[finance.refund.create]` 且 `wildcard=true`；同 DENY 的 `staff` 得 `PERMISSION_DENIED`。
3. `UserDetailsFallbackHarness`：Spring `UserDetails("ops")` → `AccessActor(id=ops, accountType=staff, org=null, merchant=null, status=active)`。
4. Python：`active ∈ {待入职,在职,试岗,停岗,离职}` = False；SHA-256(`13800000002`)=`64ac4842ab2fa04b62673f8418af7648def08bb6f2d09b5452b544007f4bf1ed`（64 hex，不含明文）。

未执行、不得声称通过：`AccessControllerTests`、`AccessJdbcIntegrationTests`、任何连 `complex`/`complex_test` 的 SQL、真实 HTTP 写接口。

现有 55 个通过测试**不能**证明 HTML 口径（GEN-003）：它们把 `active`、英文 recommendation、platform_admin 通配写成期望行为。

### 2.3 哪个前提未验证就当真了？

- `accountType=platform_admin` 且 `status=active` 等于「可执行一切已登记权限点」，包括资金类（`finance.refund.create`）和 DENY 覆盖项。
- `AccessUser.status` 的业务含义是登录开关 `active/disabled`，不是 BEQR-157 员工状态。
- SHA-256 盲索引等于 PGM-059「加密存放」。
- 后台 `POST /users` 创建的在职员工不必获得「员工自助」角色（与 BSM-084 / V322 口径分裂）。
- `UserDetails` 不会作为生产 principal（dev 过滤器走 `UserRecord`，prod 走 `AccessActor`，但 `currentActor` 仍保留伪造分支）。
- 岗位角色模板物理删除没有占用与审批。

---

## 3. 全方法覆盖（顺从式：假定实现意图正确再对口径）

### 3.1 AccessActor（record）

字段：`id, username, accountType, organizationId, merchantId, status`。无行为。消费者：`AuthorizationService.currentActor`、过滤器、多域服务。覆盖：读完全文件 11 行。

### 3.2 AccessController（`/api/v1/access`）

每个写入口先 `requirePermission` 再 `requireIdempotencyKey`（删除岗位模板除外，无幂等键）。`effectivePermissions()` 仅依赖已登录（`currentActor` 未认证抛 `UNAUTHENTICATED`）。`reviewGrant` 额外要 `governance.supervisor_review.approve`。英文报错见 GR-04。覆盖：全部 28 个 public 映射方法。

### 3.3 AccessCredentialProvisioner

`provisionCredential`；`noop()` 空实现。生产 ctor 缺 bean 时落到 noop（`AccessService` L80-104）。

### 3.4 AccessGrant* records

岗位建议/用户异常/申请单字段。`permissionCodes` 空则 `List.of()` 拷贝。`grantSummary` 写入英文 `recommendation`/`summary`（GR-04）。

### 3.5 AccessRepository

接口 + 默认内存过滤（`listUsers(org)`、`listUsersByMerchant`、`listRoles(org)`）。默认写方法 `UnsupportedOperationException`。真正持久化在 `JdbcAccessRepository`（窄读）。`findPermissionEffects` / `markSingleUsePermissionConsumed` 是授权正确性的数据面。

### 3.6 AccessService（1793 行，全部 public 方法）

| 方法 | 行（约） | 覆盖结论 |
|---|---|---|
| list/create/update/copy 权限模板 | 111-191 | 组织隔离；平台模板 `ownerScopeId=null`；copy 允许非管理员复制平台模板（owner 为 null 时不拦），apply 另拦 |
| applyPermissionTemplate | 193-267 | 跨组织、模板归属、`requireActorCanGrant`、高风险 `GRANT_REQUIRES_APPROVAL` |
| listPermissions / listRoles / createRole | 269-331 | 已知权限；不能授自己没有的码 |
| assign/unassignUserRole | 333-397 | 同组织；高风险须走申请 |
| position-role-template CRUD | 399-482 | 创建/更新/删除要求平台管理员；删除无存档 |
| assignUserPosition | 485-507 | 职级+岗位+角色+模板+薪资一次落 |
| create/revokeTemporaryGrant | 527-596 | `valid_until` 英文校验；跨组织拦 |
| applyGrant / reviewGrant | 598-711 | 无高风险则 `GRANT_APPROVAL_NOT_REQUIRED`；复核走 `SupervisorReviewService`（自审拒绝在那边） |
| listGrantRequests / grantSummary | 713-777 | 商户/项目可见性；摘要英文 |
| listAccessUsers / createAccessUser | 807-897 | 商户 isolation；账号类型白名单；**status 无白名单**；电话 SHA-256；**不分配 `role_staff_self`** |
| listStaffLevels / assignStaffLevel | 937-1023 | 职级存在性；CNY；分非负 → 对齐 BSM-120「职级只给薪资大范围，具体薪资仍需单独填写」 |

私有辅助（`requireActorCanGrant`、`requireGrantApprovalIfNeeded`、`grantReviewPolicy`、`visibleGrantRequests` 等）均已读。

### 3.7 AccessUser

12 字段 record + 7 参「legacy」构造（`createdVia="legacy"`，电话空）。覆盖全文件。

### 3.8 AuthorizationService（472 行，全部 public）

`requirePermission` 顺序：未知权限码 deny → **platform_admin 桥直接 return** → 客人储值白名单 return → DENY 优先 → GRANT → 单次授权原子消费。`effectivePermissions` 预扣 DENY 但仍标 `wildcard`。`currentActor` 识别 `AccessActor` / `UserRecord` / **UserDetails 伪造 staff+active** / 其它 principal 再伪造。scope：`currentMerchantScope`、`currentMerchantIdQuietly`（平台管理员忽略误绑 merchant）、`requirePlatformScope`、`requirePlatformComplianceScope`、`currentProjectScopeQuietly`、外包 `enforceOutsourcedAssigneeScope`。`currentSupervisorLevel`：平台管理员恒为 3；否则解析 `supervisor_lN`（与 `createAccessUser` 允许的 `"supervisor"` 不一致）。`holdsPermissionViaRole` / `levelRankOf` 供审批双轨，本模块测试未直接覆盖这两方法。

### 3.9 DevAccessCredentialProvisioner / JdbcAccessCredentialProvisioner

Dev：`local|dev`，空白密码直接 return；按 `findUserById` 调 `AuthService.provisionUser`。Jdbc：`prod`，BCrypt(10)+`SecureRandom` upsert `auth_credentials`；**非 `disabled` 一律写成 `active`**。

### 3.10 EffectivePermissions

文档写「denies 优先于 wildcard」；`requirePermission` 对平台管理员不读 denies。覆盖全文件。

---

## 4. 52 判据三线

图例：顺从=对照实现是否按该判据做事；对抗=反例；1P=第一性原理。N/A 必须给具体原因。

### GEN-001 必要性与最小性

- 顺从：主路径有 HTML 上位。7 参 legacy 构造、`page()` 假分页、UserDetails 伪造分支是多余/有害表面。
- 对抗：删 UserDetails 分支不影响 prod 过滤器（principal 已是 Actor/UserRecord）。
- 1P：类型 1。Finding：GR-08、GR-11。
- 结论：有缺陷。

### GEN-002 可执行正确性

- 顺从：55 单元绿；声明的拒绝码与测试一致。
- 对抗：测试期望 ≠ HTML。
- 1P：类型 2/4。不能把 55 绿写成业务正确。
- 结论：工程可运行，业务口径 FAIL。

### GEN-003 测试有效性

- 顺从：单次授权 TOCTOU、DENY 优先（员工）、商户 isolation、高风险须审批等有能失败断言。
- 对抗：无测试断言「平台管理员+DENY 必须失败」；无测试断言员工状态五值；`grantSummary` 不断言中文；`createAccessUserStoresManualEvidence` 把 SHA-256 当成功。
- 1P：类型 4。Finding 支撑 GR-01/02/03/04。
- 结论：有缺陷。

### GEN-004 信任边界

- 顺从：权限码须存在；组织/商户校验 fail-closed。
- 对抗：UserDetails 无库核对即 `staff/active`（GR-08）；客人桥在权限表命中后绕过 effects。
- 1P：类型 2/4。
- 结论：有缺陷。

### GEN-005 授权与最小权限

- 顺从：默认 deny；DENY 覆盖 GRANT（员工）；不能授自己没有的权限；高风险须申请。
- 对抗：平台管理员通配 + 主管级恒 3（GR-01）；客人储值白名单（GR-07）。
- 1P：类型 1+2。
- 结论：有缺陷。

### GEN-006 数据完整性

- 顺从：授权申请/岗位分配 `@Transactional`；单次授权原子 consume。
- 对抗：`DELETE FROM position_role_templates WHERE id=:id` 无占用检查、非软删（GR-06）。
- 1P：类型 1+4（SQL 原文）。
- 结论：有缺陷。

### GEN-007 并发与幂等

- 顺从：写路径 `IdempotencyService`；单次授权并发测试存在。
- 对抗：`resolve()` 异常返回 `""` 可能打穿商户幂等隔离（未做可执行反例，不升 GR）。
- 删除岗位模板无幂等键。
- 结论：主路径可接受；删除路径弱。

### GEN-008 接口与模式一致性

- 顺从：DTO/服务/SQL 多用同一英文内部码。
- 对抗：`EffectivePermissions` 文档 vs `requirePermission`；`supervisor` vs `supervisor_lN`；API `status=active` vs HTML 中文枚举；OpenAPI/前端展示英文建议。
- Finding：GR-01、GR-02、GR-04、GR-09。
- 结论：有缺陷。

### GEN-009 错误处理

- 顺从：多数失败 `recordFailure` 后抛出。
- 对抗：空白密码仍 200 且用户已建（GR-10）；未知权限对平台管理员在 knownPermission 阶段仍 deny（意外地限制了未登记码）。
- 结论：有缺陷（GR-10）。

### GEN-010 安全与敏感信息

- 顺从：密码 BCrypt；审计元数据只记 `credential_provisioned` 布尔；电话不回说明文。
- 对抗：SHA-256 ≠ 加密（GR-03）；平台管理员可读/做资金权限（GR-01）。
- 结论：有缺陷。

### GEN-011 可观测与审计

- 顺从：成功/失败审计事件码齐全。
- 对抗：无。
- 结论：本模块审计埋点可接受（不覆盖 HTML 留痕全文，那是 evidence 域）。

### GEN-012 资源边界

- 顺从：摘要窗口默认 30 天。
- 对抗：`page()` 把全量列表塞进 pageSize=50 的一页；`listGrantRequests(null)` cutoff=null 全表。
- Finding：GR-11。
- 结论：有缺陷。

### GEN-013 构建配置

- 顺从：profile 分流 Dev/Jdbc provisioner。
- 对抗：缺 provisioner → noop。未在 prod 启动验证（禁止连生产）。
- 结论：本模块文件内可接受；prod 装配未在本步验证。

### GEN-014 数据库迁移

- N/A（具体）：本 14 文件不含迁移。窄读 V003 `access_users.status` 无 CHECK，作为 GR-02 证据，不把迁移模块当本模块全量审。

### GEN-015 界面可用性/中文

- 本 14 文件无前端。API 报错与 summary 文案被后台页直接展示（GR-04）。
- 结论：有缺陷。

### GEN-016 文档可验证性

- N/A：模块文件无独立 md。Java 注释引用 `p0-operation-permission-matrix` 不在本 14 文件，未当依据。

### GEN-017 业务来源分类

- 本报告按冲突→细化→技术性→添加执行。GR-07 为添加。

### GEN-018 第一性原理证据

- 本报告结论落到 HTML 原文、命令输出或 SQL/源码行。未用「测试绿所以对」。

### BEQR-157 员工状态

- HTML（`business-enum-quick-reference-v0.1.html:240`）：`待入职、在职、试岗、停岗、离职`。
- 顺从失败：`createAccessUser` 默认 `"active"`，无五值校验；授权桥与凭证把 `"active"`/`"disabled"` 当登录态；离职/停岗只要不是字面 `disabled` 仍发 `active` 凭证。
- 对抗：Python `STATUS_IN_BEQR157 False`。
- 结论：**冲突** GR-02。不能用「这是内部标识」回避：该字段经 `AccessUser` API 返回，且决定能否上岗登录。

### BEQR-182 考勤日结果 / BEQR-183 打卡方式 / BEQR-184 请假类型 / BEQR-185 请假单状态

- N/A（具体）：14 文件无考勤日结果、打卡、请假类型/请假单状态机。相关权限码只作为字符串出现在 V322 种子（`staff.attendance.punch` / `staff.leave.submit`），本模块不解释其业务枚举。不把 staff 包当本模块审。

### BEQR-186 薪资单构成

- 本模块只存 `baseSalaryAmountCent` + 自由文本 `salaryDetail` + 职级，不实现绩效 ABC、20% 扣除上限、小费栏。
- 与 BSM-120「职级只给薪资大范围，具体薪资仍需单独填写」一致 → **细化**，不是薪资单。未发明绩效公式。
- 不把未实现的薪资单判为本模块冲突（实现未声称是薪资单）。

### BEQR-187 员工提成发放

- N/A：无实时提成、月结、代扣个税、银行发放、禁止现金现抽的代码。

### BSM-001 整体说明

- 顺从：权限是「谁能操作」底座的一部分。
- 对抗：通配管理员削弱「按同一套规则」。
- 结论：上位存在；资格执行见 GR-01。

### BSM-003 系统通用能力：…权限分岗

- 锁摘录仅 h2+lead；HTML 原文该节后续 h3 含删除审批与平台管理员日常/高危分界（BSM-004/007 未映射本模块，**不引用未映射 ID 作依据**）。
- 本锚点标题含「权限分岗」。平台管理员通配与「分岗」冲突（GR-01）。
- 「数据可改可删」：岗位模板可删，但是物理删（GR-06，细化+完整性缺陷）。

### BSM-009 基础管理

- 顺从：本包实现「员工、权限」切片，不是综合体/空间/合同/设备。
- 结论：适用切片覆盖；未越权发明综合体模型。

### BSM-028 主要功能

- 顺从：员工账号、岗位模板、职级、权限、授权审批有实现；班次/设备/注册审批不在这 14 文件（StaffService）。
- 结论：部分实现为细化；遗漏不在本文件集的班次/设备不升本模块 GR。

### BSM-029 和谁有关

- HTML：`用户权限要知道员工注册信息、设备绑定、私人手机、公司工作手机、职级和审批状态。`
- 本模块有用户、职级、授权审批状态；设备/双手机不在 14 文件。
- 结论：部分细化；设备/手机 N/A 到 staff/terminal 模块。

### BSM-073 商户侧主要功能

- 顺从：`listUsersByMerchant`、创建用户商户闸、授权申请按 `merchantId` 过滤。
- 对抗：平台管理员看全部（与「商户不能看平台全局」不对称，管理员侧）。
- 结论：商户隔离主路径成立（有测试）；不因此放过 GR-01。

### BSM-084 员工手机端…员工自助权限

- HTML：`每位审批通过的员工都自动获得一份“员工自助”权限：可查看本人（且仅本人）的工资条…`
- V322 为 `role_staff_self` 赋权；`StaffService.reviewRegistration` 分配该角色。
- `AccessService.createAccessUser`（后台手动建号，`createdVia=manual_admin`，默认可登录）**不**分配该角色。
- 结论：细化 BSM-084 + **遗漏** GR-05。后台新建员工账号是 BSM-005 已列动作，不能当成「不是审批通过所以不用给自助权限」。

### BSM-085 日常工作（注册两条路径）

- 本模块 `createAccessUser` 是后台代建，不是「本人 App 注册」或「商户代提交」。两条路径在 staff 包。
- 结论：本文件集不实现注册状态机 → 不判冲突；也不把 admin 建号写成 HTML 路径的完整实现。

### BSM-086 / BSM-087 / BSM-088

- N/A：无排班锁定、打卡三方式、请假四类、薪资单列示、提成禁现金现抽。职级薪资字段见 BEQR-186/BSM-120。

### BSM-114 各类入口

- 顺从：权限码按入口功能切分；商户/项目 scope 存在。
- 对抗：平台管理员对已登记权限点不做分入口拒绝（GR-01）。客人入口表未列储值，却有储值桥（GR-07）。
- 结论：冲突 GR-01；添加 GR-07。

### BSM-115 相互关系

- HTML：`基础管理连所有业务`…`如果合同或基础定义不允许，后续流程必须先被卡住`。
- 本模块提供卡住用的权限闸（`ActionGateAspect` → `requirePermission`）。闸被平台管理员桥短路则上游全线失效。
- 结论：依附 GR-01。

### BSM-120 已确认规则

- 相关已拍板：职级 M1-M10 分上中下且薪资另填（本模块 `StaffLevelRecord`+`assignStaffLevel` 对齐，测试夹具 `lvl_m1_lower`/`lvl_m10_upper`）；员工权限最小颗粒度（与 GR-01 冲突）；私人手机设备标识（不在 14 文件）。
- 结论：职级切片通过；最小颗粒度 FAIL（GR-01）。

### PGM-001 整体定位

- 「员工是否有权限」、界面全中文、金额到分。
- 分字段：薪资用分（cent）对齐到分；展示/报错英文冲突（GR-04）；资格被通配架空（GR-01）。
- 结论：有冲突。

### PGM-008 员工和手机（卡片）

- 本模块覆盖员工注册账号与岗位状态的后台面；私人手机/工作手机/企微不在 14 文件。
- 结论：卡片级部分细化；手机 N/A。

### PGM-022 / PGM-024 空间和点位

- 本模块用 `organizationId` 当项目 scope，无楼层/卡座/点位。
- N/A（具体）：不实现空间主数据；班次责任区域不在本文件。不发明空间规则。

### PGM-026 商户和主体主要功能

- `merchantId` 绑定、商户管理员只能建本商户用户：细化「合同、结算、权限…引用同一组绑定」中的权限切片。
- 「申请人不得审批自己提交的调整」出现在公司主体权责段，不是授权申请专条；授权自审由 `SupervisorReviewService` 以 `initiatedBy` 拦截（窄读 L154-158）。本 14 文件未重复实现，也不绕过。
- 结论：商户绑定通过；不把权责配置全文套到本模块。

### PGM-032 / PGM-033 员工和手机主要功能

- 注册两条路径、设备标识、换机、破解失效、寄存柜：不在 14 文件。
- `createAccessUser` 是第三条后台路径（HTML 其它节允许新建员工账号）。
- N/A 到 staff 设备；后台路径见 GR-05。

### PGM-034 工作手机和企业微信

- N/A：无企微会话存储/审计代码。

### PGM-035 提成发放和人员服务治理

- N/A：无提成发放、营销打卡、陪场商品。与 BEQR-187 同。

### PGM-036 设备管理

- N/A：无设备台账/领用。`AccessActor` 不是设备。

### PGM-048 商户侧全开放

- 顺从：商户账号数据隔离；不能看他商户用户权限清单（`effectivePermissionsForUser` CROSS_MERCHANT）。
- 测试：`merchantScopedCreatorCannotCreateUserForAnotherMerchant` 等。
- 结论：本切片通过（平台管理员除外，见 GR-01）。

### PGM-059 客人与员工的个人信息保护

- HTML（`platform-governance-map-for-operations-v0.1.html:870`）：`客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。`
- 实现：`MessageDigest.getInstance("SHA-256")` 存 `phone_hash`，明文丢弃。哈希单向、非加密。测试还把「64 位且不含明文」写成正确。
- 结论：**冲突** GR-03。类型 1+4。不引用未映射的 PIPL 注释当依据。

### PGM-066 已拍板

- 最小颗粒度、界面全中文：与 GR-01、GR-04 冲突。
- 其它拍板（集团视图、日结、企微永久保存、下单时间戳等）不在 14 文件 → N/A。

---

## 5. Findings（GR 临时编号；发现第一项后继续审完整模块）

分类顺序均为冲突 → 细化 → 技术性 → 添加。工程缺陷「是」表示 B2 应进修复队列（添加项先问操作员）。

### GR-01 平台管理员通配绕过 DENY 与分岗

- 文件:行：`AuthorizationService.java:33-47`、`444-447`、`189-197`、`245-248`；`EffectivePermissions.java:13-14`
- 证据（类型 1+2+4）：
  - HTML BSM-114 标题（`business-system-map-for-operations-v0.1.html:1827`）：`各类入口：不同人只看自己该看的、只做自己该做的`
  - HTML BSM-003 标题（同文件 L563）：`系统通用能力：数据可改可删、操作有引导、权限分岗`
  - HTML PGM-066（治理地图已拍板）：`员工权限按单一功能最小颗粒度配置，支持角色和员工级完全自定义。`
  - HTML PGM-001（L264-265）：`再管资格` / `员工是否有权限`
  - 命令：`AdminDenyHarness` 输出  
    `ADMIN_REQUIRE_PERMISSION_DENIED=false`  
    `ADMIN_WILDCARD=true`  
    `ADMIN_EFFECTIVE_DENIES=[finance.refund.create]`  
    `STAFF_REQUIRE_PERMISSION_DENIED=true` `STAFF_ERR=PERMISSION_DENIED`
  - 同方法把 `currentSupervisorLevel` 对平台管理员写成 3，等于自动最高主管级。
- 分类：**冲突**
- 2.3 依据：1 与 2/4
- 冻结 ID：BSM-114, BSM-003, PGM-066, PGM-001, GEN-005, GEN-008
- 工程缺陷：是
- 说明：未映射的 BSM-007「平台管理员拥有全部日常功能、资金/删除/最终审批须单授」不得当作本模块放行依据。即使用日常功能解释，资金权限 `finance.refund.create` 仍被通配放行，与「只做自己该做的」矛盾。`effectivePermissions` 仍列出 denies，前后端若按文档「denies 优先于 wildcard」会与服务端放行分裂。

### GR-02 账号/员工状态发明英文 active，未实现员工状态五值

- 文件:行：`AccessService.java:870`（`trimToDefault(request.status(), "active")`，无枚举校验）；`AuthorizationService.java:445,451,217-218`（比较 `"active"`）；`JdbcAccessCredentialProvisioner.java:33`（非 `disabled` → 凭证 `active`）；`V003` `access_users.status VARCHAR(32)` 无 CHECK
- 证据（类型 1+4）：
  - HTML BEQR-157（枚举速查 L240）：`员工状态` = `待入职、在职、试岗、停岗、离职`
  - PGM-001（治理地图 L5034 一带）：展示枚举必须中文
  - Python：`STATUS_IN_BEQR157 False`；`impl_default=active`
  - 停岗/离职只要不是字面 `disabled` 仍发登录凭证
- 分类：**冲突**（另行发明 `active/disabled` 且遗漏五值）
- 2.3：1+4
- 冻结 ID：BEQR-157, PGM-001, GEN-008, GEN-017
- 工程缺陷：是
- 不是「内部标识豁免」：该值经列表 API 返回，并控制能否作为主体操作。本模块是 access 标签下唯一用户实体，不能把五值推给未在本步全审的 staff 表后声称本模块 N/A。

### GR-03 员工手机号存 SHA-256，不是加密

- 文件:行：`AccessService.java:911-922` `hashNullable`；`AccessUser.phoneHash`；测试 `AccessServiceTests.java:323` 断言 64 hex
- 证据（类型 1+4）：
  - HTML PGM-059（治理地图 L870）：`客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。`
  - `SHA256_PHONE 64ac4842ab2fa04b62673f8418af7648def08bb6f2d09b5452b544007f4bf1ed`，`CONTAINS_PLAIN False`，`HASH_ONE_WAY True`
  - 源码 `MessageDigest.getInstance("SHA-256")`，无密钥、不可逆
- 分类：**冲突**
- 2.3：1+4
- 冻结 ID：PGM-059, GEN-010
- 工程缺陷：是
- 测试把哈希当正确实现，GEN-003 失效。

### GR-04 展示/报错英文

- 文件:行：
  - `AccessService.java:764,773` `frequent temporary grants; consider fixed permission` / `frequent temporary permission requests; review abnormal pattern`
  - `AccessService.java:1108,1252,1540,1570` `valid_until is required` 等
  - `AccessController.java:188` 同样英文
  - 消费者 `AccessGovernancePage.tsx:341,454-455` 把 `recommendation`/`summary` 填进表格「建议」列
- 证据（类型 1+4）：
  - PGM-001：`必须全部为中文，不得出现任何英文`；报错在列明范围内；例外仅单据号/设备编号/二维码/系统内部标识符
  - PGM-066 已拍板界面全中文
  - 建议句是完整英语句子，不是内部标识符
- 分类：**冲突**
- 2.3：1
- 冻结 ID：PGM-001, PGM-066, GEN-015
- 工程缺陷：是

### GR-05 后台创建员工不授予「员工自助」

- 文件:行：`AccessService.java:829-890` `createAccessUser` 无 `assignUserRole(..., "role_staff_self")`
- 窄读对照：`V322__staff_self_service_role.sql` 注释写明审批通过路径曾漏分配；`StaffService` 审批通过才分配。后台建号是另一入口。
- 证据（类型 1）：BSM-084（系统地图 L1427-1428）`每位审批通过的员工都自动获得一份“员工自助”权限`
- 分类：**细化**（上位：员工自助权限；实现漏了手动建号=生效账号）
- 2.3：1
- 冻结 ID：BSM-084, GEN-002
- 工程缺陷：是

### GR-06 岗位角色模板物理删除

- 文件:行：`AccessService.java:466-482` `deletePositionRoleTemplate`；`JdbcAccessRepository.java:520-525` `DELETE FROM position_role_templates WHERE id = :id`；Controller L176-180 无幂等键
- 证据（类型 1+4）：BSM-003 标题含「数据可改可删」；未映射 BSM-004 的审批/软删除程序**不引用为冲突依据**。工程上删除不可恢复、无占用检查，违反 GEN-006。
- 分类：**细化**（可删的下位实现）+ 完整性缺陷
- 2.3：1+4
- 冻结 ID：BSM-003, GEN-006
- 工程缺陷：是

### GR-07 客人储值权限白名单（业务发明）

- 文件:行：`AuthorizationService.java:21-25,42-47,449-452`
- 证据（类型 1）：BSM-114 客人 App/小程序「主要做什么」为 `官方购票、预定/订台、查订单、查权益、看积分摘要、出示验票二维码；礼物兑换。` 无储值充值/消费。实现以 `accountType=customer` 跳过权限 effects。
- 分类：**添加**
- 2.3：1
- 冻结 ID：BSM-114, GEN-005, GEN-017
- 工程缺陷：B2 按添加询问；若保留仍须过授权正确性（白名单绕过 effects）
- HTML 原文对照：三份 HTML 本模块映射条目无「客人自助储值权限桥」；最接近 BSM-114 客人入口行。

### GR-08 UserDetails principal 伪造 staff/active

- 文件:行：`AuthorizationService.java:146-149`
- 证据（类型 2/4）：`UserDetailsFallbackHarness`  
  `ID=ops USERNAME=ops ACCOUNT_TYPE=staff ORG=null MERCHANT=null STATUS=active`
- 生产过滤器不走该分支（Bearer→UserRecord，Prod Bearer→AccessActor），但是编译进主代码，测试 `user("ops")` 形态会触发。
- 分类：**技术性**（无 HTML 业务实体；是信任边界实现）
- 无业务发明理由：不引入新角色名到产品口径，只是把框架 User 填成内部 actor。
- 2.3：4
- 冻结 ID：GEN-004, GEN-005
- 工程缺陷：是（默认拒绝被旁路）

### GR-09 主管账号类型与主管级解析不一致

- 文件:行：`AccessService.java:52-53` 允许 `"supervisor"`；`AuthorizationService.java:245-257` 解析 `"supervisor_l" + N`
- 证据（类型 4）：`createAccessUserWithAllowedAccountTypeSucceeds` 使用 `"supervisor"`；该账号 `currentSupervisorLevel()` 为 0，任何主管复核政策都不够。
- 分类：**细化**（分岗/审批级别的下位实现错误）
- 2.3：4
- 冻结 ID：GEN-008, BSM-003, GEN-005
- 工程缺陷：是

### GR-10 无密码仍创建成功

- 文件:行：`AccessService.java:878-879` 无 `requireText(password)`；`DevAccessCredentialProvisioner.java:27-28`、`JdbcAccessCredentialProvisioner.java:30-31` 空白直接 return
- 证据：源码控制流；`createAccessUserWithAllowedAccountTypeSucceeds` 传 `password=null` 仍成功。
- 分类：**技术性**（凭证装配，无新业务规则）
- 2.3：4
- 冻结 ID：GEN-009
- 工程缺陷：是（伪成功：API 200 但无法登录）

### GR-11 分页名不副实 / 列表无界

- 文件:行：`AccessService.java:1733-1734` `new PageResponse<>(items, 1, 50, items.size())` 不切片；`listGrantRequests` `windowDays==null` → cutoff null
- 分类：**技术性**
- 2.3：4
- 冻结 ID：GEN-012, GEN-008
- 工程缺陷：是

---

## 6. ponytail-review（只列可删，不修）

`AuthorizationService.java:L146-149: delete: UserDetails/generic principal 伪造 staff/active。生产过滤器已提供 Actor/UserRecord。`  
`AccessUser.java:L24-34: delete: 7 参 legacy 构造。调用方改用 12 参。`  
`AccessService.java:L1733-1734: shrink: 假分页。真正切片或去掉 pageSize 谎言。`  
`AccessService.java:L764-773: delete: 英文建议句。改中文或删建议列。`  
`net: -约 40 行 possible（不含业务修复）。`

Lean already 不成立。

---

## 7. codebase-design 观察（非新判据）

`AuthorizationService` 是深模块：小接口（`requirePermission` + scope）后面是 DENY/单次/桥。问题是接口隐藏了「platform_admin 不受 DENY 约束」这一调用方必须知道的不变量，深度变成陷阱。`AccessRepository` 默认方法是浅 adapter；JDBC 是第二个 adapter，seam 成立。`AccessService` 表面大、把审批/薪资/模板堆在同一接口，对调用方 leverage 一般。不另开锁外判据。

---

## 8. 真实验证命令与输出

环境：`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`（进程内 export，未 source 整 env）。`mvn -o` 未下载依赖。未连 `complex`。未跑迁移。

**限制：** `-Dproject.build.directory` 未把产物打进 `.scratch`（Maven 仍写 `backend/target`）。与「不要共用 backend/target」不符。事后用刚编译的 class（时间戳 2026-09-05 08:55，源码一致）跑反例。未删除他人可能使用的 `backend/target`。

```
AuthorizationServiceTests  Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
AccessServiceTests         Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
Results: Tests run: 55, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

```
ADMIN_REQUIRE_PERMISSION_DENIED=false
ADMIN_ERR=none
ADMIN_WILDCARD=true
ADMIN_EFFECTIVE_DENIES=[finance.refund.create]
STAFF_REQUIRE_PERMISSION_DENIED=true
STAFF_ERR=PERMISSION_DENIED
COUNTEREXAMPLE_OK platform_admin bypasses DENY; staff is denied
```

```
UserDetails fallback: ID=ops ACCOUNT_TYPE=staff ORG=null MERCHANT=null STATUS=active
```

```
SHA256_PHONE 64ac4842ab2fa04b62673f8418af7648def08bb6f2d09b5452b544007f4bf1ed
STATUS_IN_BEQR157 False
```

未跑：`AccessControllerTests`、`AccessJdbcIntegrationTests`、任何真实 HTTP 写、`complex_test`。

---

## 9. 技能缺口 / 未验证范围

- `batch-grill-me`、`ubiquitous-language`：本席技能树不存在。
- git-guardrails：未装 hook。
- 隔离 target 失败，编译进了 `backend/target`。
- 未跑 Controller MockMvc、JDBC 集成、生产过滤器端到端。
- `holdsPermissionViaRole` / `levelRankOf` 无本模块直接测试。
- 未验证 OpenAPI 文件与 Controller 逐字段（文档在其它模块）。
- 三份 HTML 只读；未发现需铁律 4 暂停的 HTML 自身损坏。

---

## 10. 结论

**FAIL**

理由：存在未定稿缺陷（GR-01..GR-06、GR-08..GR-11）及添加项 GR-07。无有条件通过。B1 不修复、不写 findings 台账、不进 B2。

修复队列预告（供 B2，非正式 ID）：冲突 GR-01/02/03/04 删除或改到与 HTML 一致；细化缺陷 GR-05/06/09 修实现；技术性缺陷 GR-08/10/11 修；添加 GR-07 问操作员。

---

## 11. 52 判据逐条索引（每 ID 一行，三线已在 §4 展开）

| ID | 三线摘要 | 适用 | Finding |
|---|---|---|---|
| GEN-001 | 1P：有 HTML 上位；顺从：主路径必要；对抗：伪造分支/假分页可删 | 适用 | GR-08 GR-11 |
| GEN-002 | 1P：55 绿≠业务对；顺从：接口可跑；对抗：测试口径偏 HTML | 适用 | 全部 GR |
| GEN-003 | 1P：测试未编码 HTML；顺从：部分能失败断言；对抗：缺 DENY+admin、缺五值 | 适用 | GR-01..04 |
| GEN-004 | 1P：UserDetails 无库核；顺从：权限码/组织校验；对抗：GR-08 | 适用 | GR-08 |
| GEN-005 | 1P：通配=非最小权限；顺从：员工 DENY 优先；对抗：admin 桥/客人桥 | 适用 | GR-01 GR-07 GR-09 |
| GEN-006 | 1P：物理 DELETE；顺从：事务与单次消费；对抗：GR-06 | 适用 | GR-06 |
| GEN-007 | 1P：写路径有幂等；顺从：单次 TOCTOU 测试；对抗：删除无幂等键 | 适用 | 弱，并入 GR-06 |
| GEN-008 | 1P：文档/枚举/账号类型不一致；顺从：DTO-SQL 英文码同源；对抗：GR-01/02/04/09 | 适用 | GR-01 GR-02 GR-04 GR-09 |
| GEN-009 | 1P：无密码仍 200；顺从：失败多有 recordFailure；对抗：GR-10 | 适用 | GR-10 |
| GEN-010 | 1P：哈希非加密；顺从：BCrypt/审计不记密码；对抗：GR-03 GR-01 | 适用 | GR-03 |
| GEN-011 | 1P：关键写有审计事件；顺从：成功失败成对；对抗：无 | 适用 | 无 |
| GEN-012 | 1P：假分页/全表列表；顺从：摘要默认 30 天；对抗：GR-11 | 适用 | GR-11 |
| GEN-013 | 1P：profile 分流；顺从：Dev/Jdbc；对抗：noop 缺 bean。prod 装配未跑 | 适用 | 无独立 GR |
| GEN-014 | 1P：14 文件无迁移 | **N/A**（迁移不在本文件集；V003 status 无 CHECK 仅作 GR-02 证据） | — |
| GEN-015 | 1P：API 英文被后台展示；顺从：无本包 UI；对抗：GR-04 | 适用 | GR-04 |
| GEN-016 | 1P：无模块文档 | **N/A** | — |
| GEN-017 | 1P：分类顺序已执行 | 适用 | GR-07 |
| GEN-018 | 1P：结论落到四类证据 | 适用 | 无 |
| BEQR-157 | 1P：必须五值；顺从：存 active；对抗：Python False | 适用 | GR-02 |
| BEQR-182 | 1P：考勤日结果五类+休息 | **N/A**（14 文件无考勤日结果枚举/计算） | — |
| BEQR-183 | 1P：打卡三方式取最早 | **N/A**（无领取工作手机/NFC/扫码/离充） | — |
| BEQR-184 | 1P：事假病假年假调休 | **N/A**（无请假类型） | — |
| BEQR-185 | 1P：请假单五态 | **N/A**（授权申请 pending/applied/rejected 不是请假单） | — |
| BEQR-186 | 1P：薪资单结构 | 仅职级+基本工资分 **细化** BSM-120，非薪资单 | 无 |
| BEQR-187 | 1P：提成进月结禁现抽 | **N/A**（无发放路径） | — |
| BSM-001 | 1P：权限属基础盘子；顺从：有闸；对抗：通配 | 适用 | 依附 GR-01 |
| BSM-003 | 1P：权限分岗；顺从：角色/岗位模板；对抗：通配/硬删 | 适用 | GR-01 GR-06 |
| BSM-009 | 1P：基础管理含权限员工；顺从：本切片；对抗：无越权发明空间合同 | 适用 | 无独立 GR |
| BSM-028 | 1P：员工岗位职级权限；顺从：有；班次设备注册审批不在 14 文件 | 适用（部分） | GR-05 注册生效 |
| BSM-029 | 1P：权限要知道注册/设备/手机/职级/审批；顺从：用户职级审批；设备手机 N/A | 适用（部分） | 无 |
| BSM-073 | 1P：商户授权范围内员工；顺从：merchant scope 测试绿；对抗：admin 全看 | 适用 | 不替代 GR-01 |
| BSM-084 | 1P：自动员工自助权限；顺从：V322+StaffService；对抗：createAccessUser 不分配 | 适用 | GR-05 |
| BSM-085 | 1P：注册两条路径 | **N/A**（路径在 staff 包；本包仅后台建号） | GR-05 仅建号副作用 |
| BSM-086 | 1P：排班考勤请假 | **N/A** | — |
| BSM-087 | 1P：我的薪资列示 | **N/A**（无工资条） | — |
| BSM-088 | 1P：提成/活动/人员服务边界 | **N/A** | — |
| BSM-114 | 1P：分入口分岗；顺从：权限码切分；对抗：admin 通配、客人储值 | 适用 | GR-01 GR-07 |
| BSM-115 | 1P：基础卡住后续；顺从：ActionGateAspect；对抗：桥短路 | 适用 | 依附 GR-01 |
| BSM-120 | 1P：职级 M1-M10+另填薪资、最小颗粒度；顺从：职级切片；对抗：颗粒度 | 适用 | GR-01；职级无 GR |
| PGM-001 | 1P：资格/中文/到分；顺从：cent；对抗：通配与英文 | 适用 | GR-01 GR-04 |
| PGM-008 | 1P：员工和手机卡片；顺从：账号/岗位；手机企微不在 14 文件 | 适用（部分） | — |
| PGM-022 | 1P：业务要知道发生在哪 | **N/A**（无空间点位模型；仅 organizationId 项目 scope） | — |
| PGM-024 | 1P：班次责任区域等 | **N/A** | — |
| PGM-026 | 1P：商户绑定进权限；顺从：merchantId；自审在 SupervisorReviewService | 适用（权限切片） | 无 |
| PGM-032 | 1P：人-私人手机-工作手机对应 | **N/A**（设备绑定不在 14 文件） | — |
| PGM-033 | 1P：注册路径/换机/破解 | **N/A**（staff 包） | — |
| PGM-034 | 1P：企微永久保存 | **N/A** | — |
| PGM-035 | 1P：提成月结禁现抽 | **N/A** | — |
| PGM-036 | 1P：设备领用状态 | **N/A** | — |
| PGM-048 | 1P：商户授权范围隔离；顺从：CROSS_MERCHANT 测试 | 适用 | 无（admin 见 GR-01） |
| PGM-059 | 1P：加密存放；顺从失败：SHA-256；对抗：Python | 适用 | GR-03 |
| PGM-066 | 1P：最小颗粒度+全中文；对抗：GR-01 GR-04；其它拍板 N/A | 适用（两条） | GR-01 GR-04 |

上表 52 行，与冻结映射一一对应。N/A 均有「不在这 14 文件 / 无该业务状态机」的具体原因，未用 18 条 GEN 代替业务判据。


---

# M006 首次 B2 整批候选复核（Grok 席）

- 版本：V0.02
- 控制：V0.17
- 席位：Grok（审计席乙）
- 步骤：B2 整批候选差集 / 分类 / 队列 / 范围复核
- 模块：M006 `10-backend-main-access-01`（6/322，NORMAL，14文件2988行）
- 仓库 HEAD：`395e6fbab027020da1c38237fad17e8edcd7a132`（与派发声明一致）
- ledger 本模块状态（只读）：`IN_PROGRESS`；B3=0；方案复核已修=2；子代理失败重跑=0
- 本席独立实算 `plan/criteria-lock.md` 全文 SHA256：`23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（与 sidecar、ledger 顶部、本席既有 B1 报告内最后一次哈希叙述一致）。本次 B2 按派发不写机器哈希标记行，本段仅为正文叙述。
- LOCK-AMEND-COUNT（只读 ledger）：0
- 审计结论：**PASS**
- 未重开 B1，未进入 B3，未改源码 / 三份业务 HTML / lock / sidecar / ledger / findings / questions；未派子代理；未把原始 JSON 落盘。

---

## 0. 技能预检（本节点实核）

按意图执行：B2 只用结构，不修代码、不派子代理、不等操作员。未核到的不写已核实。

| 技能 | 本席路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| using-superpowers | superpowers 6.3.0 | 开工技能自检；用户本条指令覆盖默认“先 brainstorm 再动手” | 无 |
| receiving-code-review | superpowers 6.3.0 | 只借“先核实再接受或反驳、禁止表演性同意”；禁止 IMPLEMENT | 无 |
| grilling / grill-me | mattpocock productivity | 对抗式追问结构，自问自答 | 无 |
| verification-before-completion | superpowers | PASS/FAIL 前先核指纹与源码 | 无 |
| ponytail-review | ponytail 4.9.0 | 复核 §6 可删建议（019 构造器） | 无 |
| codebase-design / diagnosing-bugs / systematic-debugging | 存在 | 只读定位，不改生产代码 | 无 |
| writing-for-agents | 存在 | 报告步骤与完成标准 | 无 |
| tdd / test-driven-development / ponytail | 存在 | B2 禁止修复，不用 | 节点不适用 |
| resolving-merge-conflicts | 存在 | 无合并冲突 | 节点不适用 |
| git-guardrails-claude-code | 存在 | 未装 hook；本步未 push/reset | 可选，未装 hook |
| i-have-adhd | cache `i-have-adhd/0.1.0` | 存在；完整 30ID 清单优先于“列表≤5” | 无 |
| batch-grill-me / ubiquitous-language | 本席技能树未找到 | — | **缺口：不存在，未冒充调用** |
| brainstorming / writing-plans | 存在 | 阶段 A 技能，本步 B2 不用 | 节点不适用 |

划掉不用（与合同一致，未调用）：dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、executing-plans、ponytail-audit/debt/gain、research。

---

## 1. 输入指纹（本席实算，与派发声明对照）

| 输入 | 字节 | SHA256 | 对照 |
|---|---:|---|---|
| `docs/audit-2026-09/modules/M006-b1-self-raw.md` 全文 | 81666 | `08039a69fc562fdea0291c64cbaf8920a70754c628071d89a7e4b0f5eb0e0e5f` | 与派发一致 |
| 同上前 73231 字节（独立 B1 V0.05） | 73231 | `1ee77bccf61c3b418028f42866202a4655246349d7c55ef37d9e3f36def61ab4` | 与派发一致；其后 V0.06/V0.07 为接收核实与用户裁决，**不记新独立 B1 票** |
| 本席 `docs/audit-2026-09/modules/M006-b1-grok-raw.md` 追加前 | 44940 | `96f0165d01b18753284b2556d2474c17623259bdb10ddca45cc80b7a0b5da071` | 与派发一致；旧字节全部保留 |
| 候选 `docs/audit-2026-09/findings.md` V0.53 | 212573 | `edce17bce74536bfd6f2c09688f283f1eeb58ce458e0c769b11739117d1faa5b` | 与派发一致；**只审 M006 当前 30ID 表、逐项证据/拟修范围和 V0.53 说明，不审 M001—005 历史段** |
| `docs/audit-2026-09/questions/M006-027.md` V0.02 | 4512 | `78dc40b44de19363c50dfa54ccba1145774a0b051a65dfed32ed260350435a79` | 与派发一致 |
| HEAD | — | `395e6fbab027020da1c38237fad17e8edcd7a132` | 与派发一致 |

`## 操作员答复` 段首个非空行精确为 `删除`。答复后原话逐字：“客人不是只能看吗？他要操作什么东西呢？储值钱包他只能看这个交易记录和余额，他要操作什么。” 判定：新的实际用户回复，不是自动预选/沉默，也不是 M003 旧答复。没有“再批准一次”人闸。本批添加项仅 027，待答 0。

---

## 2. 差集方法

B2 无互盲。全集：

- Codex 独立 B1：CX-01—CX-23（self-raw 前 73231 字节 / V0.05）
- 本席独立 B1：GR-01—GR-11 及 §6 四条可删意见（本文件追加前 44940 字节 / V0.01）
- 候选正式 ID：M006-001—030（findings V0.53 当前表）

映射核对（与候选声明一致，无漏并）：

- CX-01—23 → 001—023
- GR-01→001、02→024、03→025、04→015、05→011、06→026、07→027、08→028、09→010、10→029、11→030
- §6 未编号兼容构造器删除建议 → 019
- §6 其余：UserDetails/未知主体删除 → 028；英文建议句 → 015；假分页收缩 → 030

Codex V0.06/V0.07 只作接收说明，不另计票。本席未把 V0.06/07 当新 B1 缺陷。

---

## 3. §5 闸 2：未映射但已在冻结定义区的 HTML 原文

M006 映射（`criteria-lock.md` 模块行，只读）含 GEN-001—018、BEQR-157/182—187、BSM-001/003/009/028/029/073/084—088/114/115/120、PGM-001/008/022/024/026/032—036/048/059/066。**不含** BSM-004、BSM-005、BSM-007、BSM-035、PGM-029、PGM-041。

锁内定义区已冻结这些锚点的 HTML 原文。候选 **没有** 把它们写入 M006 映射，也没有新增判据 ID。001/007/009/011/026/027 的“已冻结判据”栏仍只列本模块已映射 ID（主要是 GEN-005/006/017 及 BSM-003/084/085/114 等），未映射原文只作 2.1 分类的类型 1 证据。

本席裁定：**合规，不触发 LOCK-AMEND，不构成悄悄扩锁。**

理由（完整 §5）：

1. 闸 2 禁止的是锁外新判据。上列 ID 已在定义区，不是锁外。
2. “阶段 B 只准引用此集合中的 ID”约束的是不得旁路发明新 ID。候选没有把未映射 ID 填进 M006 映射列表。
3. 2.1「冲突」对照的是三份 HTML 明文，不是“仅对照本模块映射正文”。GEN-017 已映射，授权按冲突→细化→技术性→添加分类。
4. 已映射 BSM-003 锁摘录含 lead：「下面这几条不属于某一个子系统，而是整套系统都遵守的通用规则。」该句是系统级指针，不是把 BSM-004 正文并进 BSM-003 锚点。因此 026 的冲突依据是 **GEN-017 + 三份 HTML 明文 + GEN-006**，不是“BSM-003 正文已含软删程序”。
5. 若要把 BSM-004/007 等 **写成 M006 映射义务**，才必须走 LOCK-AMEND 五步。候选明确不做这一步。后续模块仍按计划全审。

必要边界（写明，不是新闸）：未映射原文可以给本模块已发现实现缺陷做上下位事实；不得据此宣称那些 ID 已进入本模块映射，也不得用未映射条文 **放行** 已映射冲突（本席 B1 对 BSM-007 的原禁令仍成立：不得拿它给平台管理员通配开脱）。候选 001 用 BSM-007 收紧日常/高危，方向与禁令相反，接受。

---

## 4. 主要分歧逐条裁决（先核实源码，再改判或维持）

原 B1 报告不覆写。下列是 B2 对候选分类/队列/范围的本席票。

### 024（GR-02）账号状态 vs 员工雇佣五态

- 本席 B1：冲突（`active/disabled` 发明英文且未实现 BEQR-157 五值）。
- 候选：细化；工程缺陷是；队列是。真实缺陷收窄为：写入口无状态白名单；非 `disabled` 一律发 `active` 凭证。
- 核实：`AccessService.java:870` `trimToDefault(..., "active")` 无枚举校验；`JdbcAccessCredentialProvisioner.java:33` 非 disabled→凭证 active；`AuthorizationService.java:445,451,217-218` 比较 `"active"`；`AccessUsersPage.tsx:339-342` 选项为启用/待激活/禁用（值 `active/invited/disabled`）；177/280 经 `formatBusinessStatus` 显示中文（`active→有效`，`invited→待激活`，`disabled→已禁用`）；`ALLOWED_ACCOUNT_TYPES` 含 merchant/outsourced_vendor；`StaffService.java` 的 `StaffProfileRecord` 另有 `registrationStatus`/`status`；`ProductionAuthService.java:283-287` 登录另检 `userStatus==active` 与 `credentialUsable`。Python：`active ∈ {待入职,在职,试岗,停岗,离职}` = False。
- **裁决：同意候选。分类细化，队列是。** 把账号登录三态强行改成雇佣五态，会把商户/外包账号发明成员工雇佣状态，本身是业务发明。BEQR-157 仍映射本模块，但落点不在 `access_users.status` 这一账号字段。内部码属系统内部标识，页面已中文。必须修的是未知状态不拒绝、`invited` 等非启用仍发可用凭证、账号状态与凭证状态不一致。不得把 `active` 改成「在职」来假装修完。员工注册审批归 011。

### 015（GR-04）英文展示

- 本席 B1：冲突，含成功建议句及 `valid_until is required` 等异常字符串。
- 候选：冲突保留成功两句英文；异常字符串因 `GlobalExceptionHandler` 翻译而不做无依据全包替换。
- 核实：`AccessService.java:764,773` 两句英文成功写入 `recommendation`/`summary`；`AccessGovernancePage.tsx:341,454-455` 原样渲染。`GlobalExceptionHandler.java:48-61` 对 `ApiException.getMessage` 调 `FieldLabels.translate`；`FieldLabels.java:504-529` 纯英文无匹配走中文 `FALLBACK`（`提交内容有误，请检查后重新提交`）。`AccessController.java:188` 英文 `valid_until is required` 走该通道，**HTTP 成功面不泄漏该英文**。
- **裁决：同意候选。分类冲突，队列是。** 拟修只改两条用户可见成功说明为中文。本席 B1 把异常源码英文字符串当成对客英文，过宽；不以源码搜英文字符要求全包替换。内部权限码保留。

### 011（CX-11；GR-05）后台新建 vs 注册审批 vs 员工自助

- 本席 B1 GR-05：细化（后台建号允许，但生效员工必须有员工自助）。
- Codex CX-11：冲突（第三条免审批激活入口）。
- 候选：冲突；保留 582 分步新建表单；封闭未经公司批准即激活并发密码；获批后自动 `role_staff_self`；不给未获批账号发自助来掩盖审批。
- 核实：HTML 581-582「新建员工账号」是分步引导表单，无免审批句。HTML 1433 两条注册路径均须公司批准后生效（BSM-085 已映射）。HTML 1428「每位审批通过的员工都自动获得一份“员工自助”权限」（BSM-084 已映射）。`createAccessUser`：`status` 默认 active、`createdVia=manual_admin`、发密码、roles=0。`StaffService.java:199-212` 审批通过才 `createUser` 并 `assignUserRole(..., "role_staff_self")`，且调用 7 参构造器。
- **裁决：同意候选冲突分类与拟修边界，队列是。** 表单 ≠ 豁免。GR-05 的自助义务并入本 ID，不是漏并。不得给未获批账号直接赋自助。

### 025（GR-03）与 021

- 核实：`AccessService.java:911-922` 无密钥 SHA-256；`AccessUser.phoneHash` 经 JDBC 返回；Python SHA256(`13800000002`)=`64ac4842ab2fa04b62673f8418af7648def08bb6f2d09b5452b544007f4bf1ed`。PGM-059 原文「一律加密存放」。`PiiCipher.encrypt` 为 AES/GCM/NoPadding；`BlindIndex.hash` 为带密钥 HMAC。021 只登记 BCrypt/UTF-8/JDBC 机制。
- **裁决：025 冲突、队列是；021 技术性、队列否。** 不得把无密钥摘要说成加密。最小方向优先去掉无消费用途的采集/对外返回；确需保存则复用已有 PiiCipher/BlindIndex，不扩成全仓 PII 运动、不做存量回填。

### 026（GR-06）物理删除

- 本席 B1：细化 + GEN-006 完整性缺陷；拒绝用未映射 BSM-004 作冲突依据。
- 候选：冲突；队列是；删除物理 DELETE，复用已有存档/审批/留存。
- 核实：`AccessService.java:466-482` 直接删；`JdbcAccessRepository.java:520-524` `DELETE FROM position_role_templates`；Controller 176-180 无幂等键。HTML 563-578 要求先存档、审批、软删除、库内可恢复、查占用。BSM-003 已映射，BSM-004 未映射但定义区已冻结。见 §3。
- **裁决：同意冲突分类与队列是。** 岗位角色模板是配置类主数据/权限配置，物理抹除与 HTML 明文矛盾。拟修必须走已有存档/审批/软删/占用检查，不能只改 `status=inactive` 宣称完成。不把 BSM-004 写入本模块映射。

### 030（GR-11）假分页

- 本席 B1：技术性。
- 候选：细化；队列是；禁止无出口截前 50。
- 核实：`AccessService.java:1733-1734` `PageResponse<>(items, 1, 50, items.size())` 不切片；`listGrantRequests` 在 `windowDays==null` 时 cutoff=null。Controller 多数 GET 无 page 参数，UI 依赖全量选项。
- **裁决：同意细化、队列是。** 列表是 BSM-028 权限管理的下位实现。元数据不实必须修；若分页则必须能取后续页。不能把技术 pageSize 当成业务政策，也不能无负载证据把小字典全量称为灾难。与 016 的频繁阈值分开。

### 019（CX-19；§6 构造器）

- 本席 §6：建议删除 7 参 legacy 构造。
- 候选：技术性、工程缺陷否、队列否；有调用，不为删而删。
- 核实：`AccessUser.java:24-34` 7 参把 `createdVia="legacy"`。生产调用：`StaffService.java:199-207` `new AccessUser(..., "active", now)` 七参。测试夹具多处同构。`JdbcAccessRepository.mapUser` 走 12 参。
- **裁决：同意队列否。** 删除构造器会迫使无本 ID 的全仓调用改写，违反闸 3 归因。本项只登记容器/兼容机制。

### 027（GR-07；操作员删除）

- 本席 B1：添加（客人入口表无储值，却有自助写桥）。
- 操作员：删除客人钱包写入；只看本人余额和交易记录。
- 核实：`AuthorizationService.java:21-25,42-47,449-452` 白名单 `customer.stored_value.topup/consume`。`MemberWalletController.java:56-85` 三个写入口；88-108 为本人卡查询。`WalletPage.tsx` 全 181 行：66-77 充值、79-94 消费（`reason=钱包消费`，DTO 无订单号）、141-175 金额框与按钮；页面只展示卡/余额，**无交易明细**。`customerService.ts:139-146` 有 topup/consume 客户端。`StoredValueConsumeRequest` 无订单字段。HTML 867 要求本人余额与逐笔记录；832 描述储值业务，不授权该页自助写账。
- **裁决：分类保持添加；操作员已答删除；队列是。** 拟修边界接受：关按钮+关客户专用写接口+关本项 customer 权限桥；保留本人只读卡/余额；复用现有 `customer_stored_value_transactions` 台账做本人只读流水（当前客人端无 list 接口，允许为这一删除项补只读查询/展示，不新建平行台账、不发明充值/退款规则）；员工柜台、经授权入账、订单支付、`consumeInternal` 内核保留。不改三 HTML。本席撤回任何把 832「有储值」写成「钱包可操作」的建议（该建议原在 Codex 侧，本席 B1 本就按添加走 2.2）。

---

## 5. 三十 ID 逐条结论

图例：差集=原票是否完整进入候选；分类/队列=本席 B2 是否接受当前值。

| ID | 来源 | 候选分类 | 工程缺陷 | 队列 | 本席 B2 | 说明 |
|---|---|---|---|---|---|---|
| 001 | CX-01；GR-01 | 冲突 | 是 | 是 | 接受 | 通配先于 DENY；finance/高危被放行；管理员 currentSupervisorLevel=3 与 010 共修。日常自动权限按 HTML 保留，不得用通配覆盖资金/删除/最终审批。 |
| 002 | CX-02 | 冲突 | 是 | 是 | 接受 | `requireSameOrganization` 只比 org。未重跑 CROSS_MERCHANT 探针，源码边界成立，采信 B1 已落盘输出。 |
| 003 | CX-03 | 细化 | 是 | 是 | 接受 | 空组织当全量。 |
| 004 | CX-04 | 细化 | 是 | 是 | 接受 | slug 当全局主键 + upsert。 |
| 005 | CX-05 | 细化 | 是 | 是 | 接受 | 改已应用模板旁路审批。 |
| 006 | CX-06 | 细化 | 是 | 是 | 接受 | 批 A 执行 B。 |
| 007 | CX-07 | 冲突 | 是 | 是 | 接受 | 受益人自审；PGM-029 作类型 1 证据，未写入映射。 |
| 008 | CX-08 | 细化 | 是 | 是 | 接受 | 不同幂等键并发双生效。 |
| 009 | CX-09 | 冲突 | 是 | 是 | 接受 | 换岗只追加；PGM-041 作类型 1 证据，未写入映射。 |
| 010 | CX-10；GR-09 | 细化 | 是 | 是 | 接受 | `supervisor` 与 `supervisor_lN` 分裂。 |
| 011 | CX-11；GR-05 | 冲突 | 是 | 是 | 接受 | 见 §4。 |
| 012 | CX-12 | 细化 | 是 | 是 | 接受 | 有效权限查询只认商户 scope。 |
| 013 | CX-13 | 细化 | 是 | 是 | 接受 | 空权限/倒置时间窗仍 active。 |
| 014 | CX-14 | 细化 | 是 | 是 | 接受 | 同一请求两次 require 烧掉单次授权。 |
| 015 | CX-15；GR-04 | 冲突 | 是 | 是 | 接受 | 见 §4。 |
| 016 | CX-16 | 冲突 | 是 | 是 | 接受 | 阈值/窗口/风险层级写死；BSM-001 已映射。源码 `@Value` 默认 3、`Math.max`、`riskRank` 已读。 |
| 017 | CX-17 | 细化 | 是 | 是 | 接受 | `markSingleUsePermissionConsumed` 把临时 grant UUID 写入 `application_id`。未重跑 TEMP SQL，源码足以支持缺陷存在。 |
| 018 | CX-18 | 细化 | 是 | 是 | 接受 | 改模板把 owner 清空。 |
| 019 | CX-19；§6 构造器 | 技术性 | 否 | 否 | 接受 | 见 §4。无业务发明。 |
| 020 | CX-20 | 技术性 | 否 | 否 | 接受 | profile 装配；空密码伪成功归 029。无业务发明。 |
| 021 | CX-21 | 技术性 | 否 | 否 | 接受 | BCrypt/UTF-8/JDBC；手机号归 025。无业务发明。 |
| 022 | CX-22 | 技术性 | 否 | 否 | 接受 | HTTP/仓储默认失败；假分页归 030。无业务发明。 |
| 023 | CX-23 | 细化 | 否 | 否 | 接受 | 记录结构有 BSM-028/120 上位；计算错误已单列。 |
| 024 | GR-02 | 细化 | 是 | 是 | 接受 | 见 §4。原冲突主张保留在证据栏，本席改判细化。 |
| 025 | GR-03 | 冲突 | 是 | 是 | 接受 | 见 §4。 |
| 026 | GR-06 | 冲突 | 是 | 是 | 接受 | 见 §4。本席 B1 细化主张保留在证据栏，本席改判冲突。 |
| 027 | GR-07；操作员删除 | 添加 | 是 | 是 | 接受 | 见 §4。答复有效，待答 0。 |
| 028 | GR-08 | 技术性 | 是 | 是 | 接受 | UserDetails 与未知 principal 均填 staff/active（`AuthorizationService.java:146-149`）。`SecurityConfig.java:81-82` 关 basic/form，110-113 的 UserDetailsService 恒抛；不夸大已证网络攻击。无业务发明。 |
| 029 | GR-10 | 技术性 | 是 | 是 | 接受 | 空密码仍 200。页面已要求初始密码。无业务发明。 |
| 030 | GR-11 | 细化 | 是 | 是 | 接受 | 见 §4。本席 B1 技术性主张保留在证据栏，本席改判细化。 |

队列合计：**25 是、5 否（019—023）、待答 0。** 与候选当前表一致。

修复队列归属规则核对：冲突 / 有工程缺陷 / 操作员答删除的添加 → 是；只登记且无工程缺陷 → 否。无「有 ID 即待修」误置。

无漏并：23 CX + 11 GR + §6 构造器全部进入 30 ID。无把本席票改写成只审 11 GR 或只审 027。无把 Codex 反例（002—018、016、017 等）降级或默删。

---

## 6. 技术性「无业务发明」复核（2.1）

| ID | 无业务发明理由 | 本席 |
|---|---|---|
| 019 | List.copyOf / 兼容构造，不引入资格或审批规则 | 同意技术性 |
| 020 | Spring profile 与凭证桥，不新增授权规则 | 同意技术性 |
| 021 | BCrypt/标准库/JDBC 适配；手机号用途不在本项放行 | 同意技术性 |
| 022 | MVC/ApiResponse/默认失败；分页行为不在本项放行 | 同意技术性 |
| 028 | 框架 principal 适配既有主体，不发明新角色名 | 同意技术性；有工程缺陷，队列是 |
| 029 | 未执行已有「初始密码必填」输入契约 | 同意技术性；有工程缺陷，队列是 |

无一项被本席改判为添加。027 是唯一添加项，已答复删除。

---

## 7. 真实验证命令与缺口

本次 B2 **未**运行会写 `backend/target` 的 Maven/测试（本席 B1 已披露隔离 target 失败）。未连生产库、未 HTTP 写、未安装依赖、未 source 整 env、未改 SQL/HTML/配置/台账/问题。

实跑（只读）：

```
git rev-parse HEAD
→ 395e6fbab027020da1c38237fad17e8edcd7a132

python3 计算四份必读输入字节与 SHA256
→ 与派发声明逐项相同；self-raw 前 73231 字节 SHA 与独立 B1 声明相同

sha256sum docs/audit-2026-09/plan/criteria-lock.md
→ 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
cat docs/audit-2026-09/plan/criteria-lock.sha256
→ 同值

python3 hashlib.sha256(b'13800000002')
→ 64ac4842ab2fa04b62673f8418af7648def08bb6f2d09b5452b544007f4bf1ed

python3：'active' in {待入职,在职,试岗,停岗,离职} → False
```

窄读（直接引用，非全仓内容搜索、非其他模块全审）：M006 十四文件关键行；`JdbcAccessRepository` 删除/消费留痕；`StaffService` 审批建号；`GlobalExceptionHandler`/`FieldLabels`；`AccessUsersPage`/`AccessGovernancePage`/`WalletPage`；`MemberWalletController`/`customerService.ts`/`StoredValueConsumeRequest`；`PiiCipher`/`BlindIndex`；`ProductionAuthService.authenticateToken`；`SecurityConfig` basic/form 与 UserDetailsService；三份 HTML 563-588、832、867、1428-1433、1827-1885、枚举 240、治理 870；lock 定义区 BSM-003/004/005/007 与 M006 映射行。

为核实 019 调用，对 `new AccessUser(` 做了类型名定向检索（已知 record 构造，不是模块外全审）。命中生产 `StaffService` 与本模块测试夹具。

**缺口（如实）：**

- 未重跑本席 B1 的 55 单测、AdminDenyHarness、UserDetailsFallbackHarness。
- 未重跑 Codex B1 的 48 源 javac 探针、双线程审批、complex_test TEMP 外键。
- 未跑 AccessControllerTests / AccessJdbcIntegrationTests / 真实 HTTP。
- 客人储值消费在无 merchant 时是否实际成功，未新跑；拟修是关闭写入口，不依赖该路径已可用。
- 未在 `.scratch/m006-grok-*` 落临时编译产物（本步不需要可执行反例重跑）。

上述缺口不构成待补审计项：B2 审的是候选归并，不是再做一遍全模块 B1。不以未重跑否定已落盘 B1 输出；分类争议均落到本次源码/HTML 原文。

---

## 8. 整体结论

**PASS**

- 30 ID 无漏并、无改写原缺陷事实、无把 Codex 反例丢掉。
- 四分类与修复队列当前值本席全部接受（含 024/026/030 的分类改判，及 015/011/019/025/027 范围）。
- 添加项 027 答复有效为删除；25 是 / 5 否 / 待答 0。
- 未映射冻结 HTML 作类型 1 证据合规，未扩锁、未改 52 映射。
- 无待补项、无未答复 questions、无铁律 4 HTML 修改申请。
- 不存在有条件通过。本票只定 B2 候选复核；B3 未执行。

本席 FAIL 否决权未动用。双 PASS 仍须 Codex 本批 B2 自票；任一方 FAIL 则整批 FAIL。


---

# M006 B2 整批候选复核（Grok 席）

- 版本：V0.03
- 控制：V0.17
- 席位：Grok（审计席乙）
- 步骤：B2 整批候选差集 / 分类 / 队列 / 范围复核
- 模块：M006 `10-backend-main-access-01`（6/322，NORMAL，14文件2988行，52映射）
- 仓库 HEAD：`395e6fbab027020da1c38237fad17e8edcd7a132`（与派发声明一致）
- ledger 本模块状态（只读）：`IN_PROGRESS`；B3=0；方案复核已修=2；子代理失败重跑=0
- 本席独立实算 `plan/criteria-lock.md` 全文 SHA256（正文叙述，按派发不写机器哈希标记行）：`23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（与 sidecar、ledger 顶部一致）
- LOCK-AMEND-COUNT（只读 ledger）：0
- 审计结论：**FAIL**
- 未重开 B1，未进入 B3，未改源码 / 三份业务 HTML / lock / sidecar / ledger / findings / questions；未派子代理；未把原始 JSON 落盘。
- 本席旧票 `本文件` V0.02（钱包只读 PASS）是历史，**不能作为当前票**。

---

## 0. 技能预检（本节点实核）

按意图执行：B2 只用结构，不修代码、不派子代理、不等操作员。未核到的不写已核实。grilling 的「等用户回答 / 派子代理查事实」作废，自问自答并自己读源码。receiving-code-review 的 IMPLEMENT 作废。verification-before-completion：PASS/FAIL 前先核指纹与源码。

| 技能 | 本席路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| using-superpowers | superpowers 6.3.0 | 开工技能自检；用户本条指令覆盖默认“先 brainstorm 再动手” | 无 |
| receiving-code-review | superpowers 6.3.0 | 只借“先核实再接受或反驳、禁止表演性同意”；禁止 IMPLEMENT | 无 |
| grilling / grill-me | mattpocock productivity | 对抗式追问结构，自问自答 | 无 |
| verification-before-completion | superpowers | FAIL 前核指纹、HTML、源码 | 无 |
| ponytail-review | ponytail 4.9.0 | 复核 §6 可删建议（019 构造器；027 不得只删权限桥） | 无 |
| codebase-design / diagnosing-bugs / systematic-debugging | 存在 | 只读定位，不改生产代码 | 无 |
| writing-for-agents | 存在 | 报告步骤与完成标准 | 无 |
| tdd / test-driven-development / ponytail | 存在 | B2 禁止修复，不用 | 节点不适用 |
| resolving-merge-conflicts | 存在 | 无合并冲突 | 节点不适用 |
| git-guardrails-claude-code | 存在 | 未装 hook；本步未 push/reset | 可选，未装 hook |
| i-have-adhd | cache `i-have-adhd/0.1.0` | 存在；完整 30ID 清单优先于“列表≤5” | 无 |
| batch-grill-me / ubiquitous-language | 本席技能树未找到 | — | **缺口：不存在，未冒充调用** |
| brainstorming / writing-plans | 存在 | 阶段 A 技能，本步 B2 不用 | 节点不适用 |

划掉不用（与合同一致，未调用）：dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、executing-plans、ponytail-audit/debt/gain、research。

---

## 1. 输入指纹（本席实算，与派发声明对照）

调用开始时实算（本席以此为 B2 输入；其后并发增量不当输入）：

| 输入 | 字节 | SHA256 | 对照 |
|---|---:|---|---|
| `docs/audit-2026-09/modules/M006-b1-self-raw.md` 调用开始全文 | 89316 | `1b0cd898739111c912c2a20ff63815eb1a82d6ab22d0aa81ec21ac2435858915` | 与派发一致 |
| 同上前 73231 字节（独立 B1 V0.05） | 73231 | `1ee77bccf61c3b418028f42866202a4655246349d7c55ef37d9e3f36def61ab4` | 与派发一致；其后 V0.06—09 为接收/操作员裁决记录，**不是新增独立 B1 票** |
| 本席 `docs/audit-2026-09/modules/M006-b1-grok-raw.md` 追加前 | 67898 | `7a13ac5e95b2e1ea473e6287e64c64fa40ba8cb3b6d450c873cc8cd306853d75` | 与派发一致；旧字节全部保留 |
| 同上前 44940 字节（独立 B1 V0.01） | 44940 | `96f0165d01b18753284b2556d2474c17623259bdb10ddca45cc80b7a0b5da071` | 与派发一致 |
| 候选 `docs/audit-2026-09/findings.md` V0.55 | 220658 | `f8d033a33dce3ed64c34af0d478e01be97b658697e69d46294e521820c841c15` | 与派发一致；**只审 M006 当前完整 30ID 表、逐项证据/拟修范围和 V0.55 最新说明，不审 M001—005 历史段** |
| `docs/audit-2026-09/questions/M006-027.md` V0.04 | 10147 | `d1696ddd6a546789b3e44b446c063466f48861ac79780df686e9e7ba88ca9c94` | 与派发一致 |
| HEAD | — | `395e6fbab027020da1c38237fad17e8edcd7a132` | 与派发一致 |

落盘前复查：self-raw 已变为 102097 字节 / `b1544896ea6a9d68d4a6997f4bd6e53a0b1eac335193bf76424dccb947c2c2f0`。增量始于调用开始后的 `## V0.10 Codex B2 当前完整候选复核` 标题。本席**不把该增量当输入、不采其结论**。findings / questions / 本席 grok-raw 追加前字节与派发仍一致。

`## 操作员答复` 段首个非空行精确为 `保留`。其后完整原话（充值拉起微信/支付宝/银联、渠道成功确认后入账、POS；App 已有订单余额支付；现场原出示二维码后改为报手机号短信扣款）附于同一文件。判定：最新明确业务范围规范化，不是默认预选、不是沉默、不是沿用 M003 答复。V0.02 历史「删除」、V0.03 二维码及 A/B HTML 申请均已撤回，不采迟到旧稿。本批添加项询问已完成，待答 0。不要求用户再确认已明确业务。无待批 HTML 修改，不触发铁律 4。

---

## 2. 差集方法

B2 无互盲。全集：

- Codex 独立 B1：CX-01—CX-23（self-raw 前 73231 字节 / V0.05）
- 本席独立 B1：GR-01—GR-11 及 §6 四条可删意见（本文件前 44940 字节 / V0.01）
- 候选正式 ID：M006-001—030（findings V0.55 当前表）

映射核对（与候选声明一致，无漏并）：

- CX-01—23 → 001—023
- GR-01→001、02→024、03→025、04→015、05→011、06→026、07→027、08→028、09→010、10→029、11→030
- §6 未编号兼容构造器删除建议 → 019
- §6 其余：UserDetails/未知主体删除 → 028；英文建议句 → 015；假分页收缩 → 030

Codex V0.06—09 只作接收说明，不另计票。本席 V0.02 旧 B2 只读钱包 PASS 不参与本票合取。未拼接旧 29 项 PASS 免审。

---

## 3. §5 闸 2：未映射但已在冻结定义区的 HTML 原文

M006 映射行含 52 个 ID：GEN-001—018、BEQR-157/182—187、BSM-001/003/009/028/029/073/084—088/114/115/120、PGM-001/008/022/024/026/032—036/048/059/066。**不含** BSM-004、BSM-005、BSM-006、BSM-007、BSM-035、PGM-029、PGM-041。

锁内定义区已冻结这些锚点的 HTML 原文。候选 **没有** 把它们写入 M006 映射，也没有新增判据 ID。001/007/009/011/026/027 的“已冻结判据”栏仍只列本模块已映射 ID；未映射原文只作 GEN-005/006/017 分类的类型 1 证据。

本席裁定：**合规，不触发 LOCK-AMEND，不构成悄悄扩锁，不重开 M001。** 三 HTML 当前不改。

---

## 4. 本批否决项（一次列齐；发现第一处未退出）

### FAIL-1　M006-027 四分类应为细化，候选写成添加

- 候选当前值：分类 **添加**；工程缺陷 **是**；队列 **是**。
- 本席裁定：分类 **细化**；工程缺陷 **是**；队列 **是**（队列与拟修范围接受，分类不接受）。
- 2.1 顺序：能指到 HTML 上位条目 = 细化；添加要求「不隶属于任何 HTML 条目 **且** 构成业务发明」。
- 可指认上位（类型 1，未映射锚点仅作 GEN-017 证据，不扩 52 映射）：
  - `business-system-map-for-operations-v0.1.html:832`（锁内 BSM-035 储值行）：`客人可充值到余额、消费时扣余额；充值必须支付成功才入账、不凭空加余额`
  - 同文件 L585（锁内 BSM-006）：`客人付款走第三方微信、支付宝，或用账户预存款经手机号加短信验证支付，系统自己不生成“付款出示码”`
  - 同文件 L867：`我的余额：查看本人储值卡余额，含本金与赠送金分列。我的余额同时提供逐笔记录：…`
  - 同文件 L1065：支付方式含 POS、储值卡、客人扫码等
- 本席 B1 GR-07 以 BSM-114 客人入口摘要「官方购票、预定/订台…」无储值，判添加。该摘要不能压过同文件客人端 867 与 832 的明文能力。操作员已保留限定充值/消费，且声明落在现有 832/585/867/1065 之内。故权限桥 `customer.stored_value.topup/consume` 是 832 的下位鉴权实现，不是无上位的业务发明。
- 现实现与上位/保留范围不符（工程缺陷成立，故队列仍是）：
  - `WalletPage.tsx:66-77,173-174`：发起充值后提示「请到柜台完成支付收款后到账」，未拉起微信/支付宝/银联
  - 同文件 `79-94`：任意金额 + `reason=钱包消费` 调用 consume；DTO `StoredValueConsumeRequest` 无订单/账单/短信字段；页面 `message.success("消费成功")`
  - `StoredValueService.consume` 经 `consumeInternal` 按金额直接扣余额，无订单/账单/SMS 绑定
  - 钱包页只展示卡与余额，无 867 要求的逐笔记录
  - `settleTopup` 已要求支付意图 `succeeded` 才入账（832 支付成功条款在服务端有内核）；客人端未走渠道确认后入账，另有柜台 `confirmOfflineTopupAndSettle`
- 拟修范围：**接受候选 V0.55 当前限定范围**，不另发明流程闸。保留限定授权通路，不得只删权限桥；不得放宽任意客人/商户；不得让客户端自行宣称付款成功入账；HTML585 手机号+短信验证继续适用，不得豁免 App 订单余额支付；现场报本人手机号、短信验证码通过后按该账单金额扣款，不生成储值付款二维码；取消钱包页脱离订单/账单的任意金额扣款；保留本人余额与逐笔记录查询。三 HTML / 锁 / 映射不改。
- 实际交易台账名以源码为准：`JdbcCustomerRepository` 写入 `customer_stored_value_transactions`（V090 建表）。本席按此名引用，**不据此另建表、不建平行台账**。
- 操作员已答保留，待答 0；改分类后不再走 2.2。

无第二处分类/漏并/队列否决。下列 001—026、028—030 本席接受候选当前值。

---

## 5. 主要分歧逐条裁决（先核实源码，再改判或维持）

原 B1 报告不覆写。下列是本票对候选分类/队列/范围的本席票。

### 024（GR-02）账号状态 vs 员工雇佣五态

- 本席 B1：冲突（须改成 BEQR-157 五值）。
- 候选：细化；缺陷是；队列是。收窄为写入口无状态白名单、非 disabled 发 active 凭证。
- 核实：`AccessService.java:870` 默认 `"active"` 无枚举校验；`JdbcAccessCredentialProvisioner.java:33` 非 disabled→凭证 active；`AuthorizationService` 比较 `"active"`；`AccessUsersPage.tsx:339-342` 选项启用/待激活/禁用（`active/invited/disabled`）；`statusLabels.ts` `active→有效`、`invited→待激活`、`disabled→已禁用`；`ALLOWED_ACCOUNT_TYPES` 含 merchant/outsourced_vendor。Python：`active ∈ {待入职,在职,试岗,停岗,离职}` = False。
- **裁决：同意候选细化、队列是。** 把登录三态改成雇佣五态，会把商户/外包账号发明成员工状态。必须修未知状态不拒绝、非启用仍发可用凭证。不得把 `active` 改成「在职」假装修完。员工注册审批归 011。

### 015（GR-04）英文展示

- 本席 B1：冲突，含成功建议句及 `valid_until is required` 等异常字符串。
- 候选：冲突；只修两条成功英文。
- 核实：`AccessService.java:764,773` 两句英文写入 recommendation/summary；`AccessGovernancePage.tsx:341,454-455` 原样渲染。`GlobalExceptionHandler.java:48-61` 调 `FieldLabels.translate`；`FieldLabels.java:503-529` 纯英文无匹配走中文 `FALLBACK`（`提交内容有误，请检查后重新提交`）。`AccessController.java:188` 英文走异常通道，HTTP 成功面不泄漏。
- **裁决：同意候选。** 拟修只改两条用户可见成功说明为中文。不以源码搜英文字符要求全包替换。内部权限码保留。

### 011（CX-11；GR-05）后台新建 vs 注册审批 vs 员工自助

- 本席 B1 GR-05：细化（后台建号允许，生效员工必须有员工自助）。
- Codex CX-11：冲突（第三条免审批激活入口）。
- 候选：冲突；保留 582 分步新建；封闭未经公司批准即激活并发密码；获批后自动 `role_staff_self`。
- 核实：HTML 581-582「新建员工账号」是分步引导表单，无免审批句。HTML 1433 两条注册路径均须公司批准后生效（BSM-085 已映射）。HTML 1428 审批通过自动员工自助（BSM-084 已映射）。`createAccessUser`：默认 active、`createdVia=manual_admin`、发密码、不赋 `role_staff_self`。`StaffService.java:199-212` 审批通过才 `createUser` 并 `assignUserRole(..., "role_staff_self")`。
- **裁决：同意候选冲突分类与拟修边界，队列是。** 表单 ≠ 豁免。GR-05 自助义务并入本 ID。不得给未获批账号直接赋自助。

### 025（GR-03）与 021

- 核实：`AccessService.java:911-922` 无密钥 SHA-256；`AccessUser.phoneHash` 经 JDBC 返回；Python SHA256(`13800000002`)=`64ac4842ab2fa04b62673f8418af7648def08bb6f2d09b5452b544007f4bf1ed`。PGM-059「一律加密存放」。021 只登记 BCrypt/UTF-8/JDBC。
- **裁决：025 冲突、队列是；021 技术性、队列否。** 不得把无密钥摘要说成加密。最小方向优先去掉无消费用途的采集/对外返回；确需保存则复用已有 PiiCipher/BlindIndex，不扩成全仓 PII、不做存量回填。

### 026（GR-06）物理删除

- 本席 B1：细化 + GEN-006；拒绝用未映射 BSM-004 作冲突依据。
- 候选：冲突；删除物理 DELETE，复用已有存档/审批/留存。
- 核实：`AccessService.java:466-482` 直接删；`JdbcAccessRepository.java:520-524` `DELETE FROM position_role_templates`；Controller 176-180 无幂等键。HTML 563-578 要求先存档、审批、软删除、库内可恢复、查占用。BSM-003 已映射；BSM-004 未映射但定义区已冻结，作 GEN-006/017 类型 1 证据。
- **裁决：同意冲突分类与队列是。** 岗位角色模板是配置类主数据。不能只改 `status=inactive` 宣称删除审批完成。不把 BSM-004 写入本模块映射。

### 030（GR-11）假分页

- 本席 B1：技术性。
- 候选：细化；禁止无出口截前 50。
- 核实：`AccessService.java:1733-1734` `PageResponse<>(items, 1, 50, items.size())` 不切片；`listGrantRequests` 在 `windowDays==null` 时 `days=0`、`cutoff=null`。Controller 多数 GET 无 page 参数。
- **裁决：同意细化、队列是。** 列表是 BSM-028 下位实现。元数据不实必须修；若分页必须能取后续页。禁止无出口截前 50。不能把 pageSize 当成业务政策。与 016 频繁阈值分开。

### 019（CX-19；§6 构造器）

- 本席 §6：建议删除 7 参 legacy 构造。
- 候选：技术性、缺陷否、队列否；有真实调用。
- 核实：`AccessUser.java:24-34` 7 参。生产调用：`StaffService.java:199-207` `new AccessUser(..., "active", now)` 七参。`JdbcAccessRepository.mapUser` 走 12 参。
- **裁决：同意队列否。** 删除构造器会迫使无本 ID 的调用改写，违反闸 3。本项只登记容器/兼容机制。

### 027（GR-07；操作员保留）

见 §4 FAIL-1。分类改判细化；队列是；拟修范围接受 V0.55 当前文字。V0.53 只读关闭写通路、V0.54 二维码及 HTML 申请 A/B 均不执行。

---

## 6. 三十 ID 逐条结论

图例：差集=原票是否完整进入候选；本席 B2=是否接受候选**当前**分类/队列/范围。

| ID | 来源 | 候选分类 | 工程缺陷 | 队列 | 本席 B2 | 说明 |
|---|---|---|---|---|---|---|
| 001 | CX-01；GR-01 | 冲突 | 是 | 是 | 接受 | 通配先于 DENY（`AuthorizationService.java:39-46`）；`currentSupervisorLevel` 对管理员直接返回 3。日常自动权限按 HTML 保留，不得用通配覆盖资金/删除/最终审批。BSM-007 原文作 GEN-005 证据，未写入映射。 |
| 002 | CX-02 | 冲突 | 是 | 是 | 接受 | `requireSameOrganization` 只比 org（`AccessService.java:1615-1622`）。未重跑 CROSS_MERCHANT 探针，源码边界成立，采信 B1 已落盘输出。 |
| 003 | CX-03 | 细化 | 是 | 是 | 接受 | 空组织当全量（`listPermissionTemplates` 非管理员传 `organizationId`，空则不过滤）。 |
| 004 | CX-04 | 细化 | 是 | 是 | 接受 | `slug()` 当全局主键 + upsert。 |
| 005 | CX-05 | 细化 | 是 | 是 | 接受 | 改已应用模板旁路审批。未重跑探针，源码 update 不核授出能力。 |
| 006 | CX-06 | 细化 | 是 | 是 | 接受 | 批 A 执行 B。 |
| 007 | CX-07 | 冲突 | 是 | 是 | 接受 | 受益人自审；PGM-029 作类型 1 证据，未写入映射。 |
| 008 | CX-08 | 细化 | 是 | 是 | 接受 | 不同幂等键并发双生效。未重跑双线程，采信 B1 输出 + 无 CAS 的源码边界。 |
| 009 | CX-09 | 冲突 | 是 | 是 | 接受 | 换岗只追加；PGM-041 作类型 1 证据，未写入映射。 |
| 010 | CX-10；GR-09 | 细化 | 是 | 是 | 接受 | `supervisor` 与 `supervisor_lN` 分裂（`AccessService.java:52-53` vs `AuthorizationService.java:245-257`）。 |
| 011 | CX-11；GR-05 | 冲突 | 是 | 是 | 接受 | 见 §5。 |
| 012 | CX-12 | 细化 | 是 | 是 | 接受 | `effectivePermissionsForUser` 无条件 `currentMerchantScope`（`AuthorizationService.java:86-97`）。 |
| 013 | CX-13 | 细化 | 是 | 是 | 接受 | 空权限/倒置时间窗仍可返回。 |
| 014 | CX-14 | 细化 | 是 | 是 | 接受 | `reviewGrant` 连续两次 `requirePermission`（`AccessController.java:137-138`）。 |
| 015 | CX-15；GR-04 | 冲突 | 是 | 是 | 接受 | 见 §5。 |
| 016 | CX-16 | 冲突 | 是 | 是 | 接受 | `@Value` 默认 3、`Math.max`、`riskRank` 写死层级；BSM-001 已映射。 |
| 017 | CX-17 | 细化 | 是 | 是 | 接受 | 单次消费留痕外键。未重跑 TEMP SQL，源码足以支持缺陷存在。 |
| 018 | CX-18 | 细化 | 是 | 是 | 接受 | 管理员改模板把 owner 置 null（`AccessService.java:161-164`）。 |
| 019 | CX-19；§6 构造器 | 技术性 | 否 | 否 | 接受 | 见 §5。无业务发明。 |
| 020 | CX-20 | 技术性 | 否 | 否 | 接受 | profile 装配；空密码伪成功归 029。无业务发明。 |
| 021 | CX-21 | 技术性 | 否 | 否 | 接受 | BCrypt/UTF-8/JDBC；手机号归 025。无业务发明。 |
| 022 | CX-22 | 技术性 | 否 | 否 | 接受 | HTTP/仓储默认失败；假分页归 030。无业务发明。 |
| 023 | CX-23 | 细化 | 否 | 否 | 接受 | 记录结构有 BSM-028/120 上位；计算错误已单列。 |
| 024 | GR-02 | 细化 | 是 | 是 | 接受 | 见 §5。原冲突主张保留在证据栏，本席改判细化。 |
| 025 | GR-03 | 冲突 | 是 | 是 | 接受 | 见 §5。 |
| 026 | GR-06 | 冲突 | 是 | 是 | 接受 | 见 §5。本席 B1 细化主张保留在证据栏，本席改判冲突。 |
| 027 | GR-07；操作员保留 | 添加 | 是 | 是 | **不接受分类** | 见 §4 FAIL-1。分类应为细化；队列是、范围接受。 |
| 028 | GR-08 | 技术性 | 是 | 是 | 接受 | UserDetails 与未知 principal 均填 staff/active（`AuthorizationService.java:146-149`）。不夸大已证网络攻击。无业务发明。 |
| 029 | GR-10 | 技术性 | 是 | 是 | 接受 | 空密码仍成功返回（`AccessService.java:878-879`；两 provisioner 空白直接 return）。无业务发明。 |
| 030 | GR-11 | 细化 | 是 | 是 | 接受 | 见 §5。本席 B1 技术性主张保留在证据栏，本席改判细化。 |

候选表队列合计 25 是 / 5 否（019—023）/ 待答 0。本席改判 027 分类后，**队列计数不变**（仍 25 是 / 5 否）。修复队列规则：冲突 / 有工程缺陷 / 操作员答删除的添加 → 是；只登记且无工程缺陷 → 否。027 在细化+缺陷下仍是，不因保留而出队。

无漏并：23 CX + 11 GR + §6 构造器全部进入 30 ID。无把本席票改写成只审 11 GR 或只审 027。无把 Codex 反例（002—018 等）降级或默删。

---

## 7. 技术性「无业务发明」复核（2.1）

| ID | 无业务发明理由 | 本席 |
|---|---|---|
| 019 | List.copyOf / 兼容构造，不引入资格或审批规则 | 同意技术性 |
| 020 | Spring profile 与凭证桥，不新增授权规则 | 同意技术性 |
| 021 | BCrypt/标准库/JDBC 适配；手机号用途不在本项放行 | 同意技术性 |
| 022 | MVC/ApiResponse/默认失败；分页行为不在本项放行 | 同意技术性 |
| 028 | 框架 principal 适配既有主体，不发明新角色名 | 同意技术性；有工程缺陷，队列是 |
| 029 | 未执行已有「初始密码必填」输入契约 | 同意技术性；有工程缺陷，队列是 |

无一项被本席从技术性改判为添加。027 本席从添加改判细化（FAIL-1），不是技术性。

---

## 8. ponytail-review（只列可删，不修）

`AuthorizationService.java:L146-149: delete: UserDetails/generic principal 伪造 staff/active。生产过滤器已提供 Actor/UserRecord。`（归 028，待修）  
`AccessUser.java:L24-34: 不删。StaffService 七参真实调用。`（归 019，队列否）  
`AccessService.java:L1733-1734: shrink: 假分页。真正切片或去掉 pageSize 谎言，且必须能取后续记录。`（归 030）  
`WalletPage 任意金额消费入口: delete: 脱离订单/账单的金额框扣款。权限桥不删。`（归 027）  
`net: 假分页/伪造主体/任意金额消费可删或收缩；7 参构造器不可删。`

---

## 9. 真实验证命令与缺口

本次 B2 **未**运行会写 `backend/target` 的 Maven/测试。未连生产库、未 HTTP 写、未安装依赖、未 source 整 env、未改 SQL/HTML/配置/台账/问题。未创建 `.scratch/m006-grok-*`（本步源码/HTML 原文足够，无需新可执行反例）。

实跑（只读）：

```
git rev-parse HEAD
→ 395e6fbab027020da1c38237fad17e8edcd7a132

python3 计算四份必读输入字节与 SHA256
→ 调用开始与派发声明逐项相同（self 89316 / grok 67898 / findings 220658 / questions 10147）
→ 独立 B1 前缀 self[:73231] = 1ee77bcc… ；grok[:44940] = 96f0165d…

sha256sum docs/audit-2026-09/plan/criteria-lock.md
→ 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
cat docs/audit-2026-09/plan/criteria-lock.sha256
→ 同值

python3 hashlib.sha256(b'13800000002')
→ 64ac4842ab2fa04b62673f8418af7648def08bb6f2d09b5452b544007f4bf1ed

python3：'active' in {待入职,在职,试岗,停岗,离职} → False

questions/M006-027.md 「## 操作员答复」后首个非空行 → 保留
```

窄读（直接引用，非全仓内容搜索、非其他模块全审）：M006 十四文件关键行；`JdbcAccessRepository` 删除 SQL；`StaffService` 审批建号与七参构造；`GlobalExceptionHandler`/`FieldLabels`；`AccessUsersPage`/`AccessGovernancePage`/`WalletPage`/`statusLabels`；`customer/MemberWalletController`/`StoredValueService`/`StoredValueConsumeRequest`/`customerService.ts`/`JdbcCustomerRepository` INSERT；三份 HTML 563-588、832、867、1065、1428-1433、1827-1885、枚举 240、治理 870；lock 定义区 BSM-003/004/005/006/007/035 与 M006 映射行（52 ID，确认未含 BSM-004/005/006/007/035）。

为核实 019 调用，对 `new AccessUser(` 做了类型名定向检索（已知 record 构造，不是模块外全审）。命中生产 `StaffService` 与 `JdbcAccessRepository`（12 参）。

**缺口（如实）：**

- 未重跑本席 B1 的 55 单测、AdminDenyHarness、UserDetailsFallbackHarness。
- 未重跑 Codex B1 的 48 源 javac 探针、双线程审批、complex_test TEMP 外键。
- 未跑 AccessControllerTests / AccessJdbcIntegrationTests / 真实 HTTP。
- 客人储值消费在无 merchant 时是否实际成功，未新跑；拟修是纠正任意金额消费并保留合法订单/账单路径，不依赖该路径当前已可用。
- 未在 `.scratch/m006-grok-*` 落临时编译产物。
- 落盘前 self-raw 并发增量为 Codex B2 标题，本席未读其正文、不采信。

上述缺口不构成待补审计项：B2 审的是候选归并。不以未重跑否定已落盘 B1 输出；分类争议均落到本次源码/HTML 原文。不以旧 V0.02 PASS 或并发 Codex B2 标题折算本票。

---

## 10. 整体结论

**FAIL**

否决权动用一次，全批如下（无第二处）：

1. **M006-027 四分类**：候选「添加」不成立。应按 2.1 改为 **细化**。工程缺陷是、队列是、V0.55 拟修范围接受。待答 0。不改三 HTML、不扩锁、不重开 M001。

其余 29 ID：无漏并、无改写原缺陷事实、四分类与队列本席接受（含 024/026/030 的分类改判，及 015/011/019/025 范围）。未映射冻结 HTML 作类型 1 证据合规。无未答复 questions。无铁律 4 待批申请。

不存在有条件通过。本票只定 B2 候选复核；B3 未执行。Codex 须按 FAIL-1 改正候选分类后重复本批 B2 ②；不得以自审 PASS 覆盖本席 FAIL。


# M006 B2 整批候选复核（Grok 席）

- 版本：V0.04
- 控制：V0.17
- 席位：Grok（审计席乙）
- 步骤：B2 整批候选差集 / 分类 / 队列 / 范围复核
- 模块：M006 `10-backend-main-access-01`（6/322，NORMAL，14文件2988行，52映射）
- 仓库 HEAD：`395e6fbab027020da1c38237fad17e8edcd7a132`（与派发声明一致）
- ledger 本模块状态（只读）：`IN_PROGRESS`；B3=0；方案复核已修=2；子代理失败重跑=0
- 本席独立实算 `plan/criteria-lock.md` 全文 SHA256（正文叙述，按派发不写机器哈希标记行）：`23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（与 sidecar、ledger 顶部一致）
- LOCK-AMEND-COUNT（只读 ledger）：0
- 审计结论：**FAIL**
- 未重开 B1，未进入 B3，未改源码 / 三份业务 HTML / lock / sidecar / ledger / findings / questions；未派子代理；未把原始 JSON 落盘。
- 本席旧票：V0.02（钱包只读 PASS）、V0.03（唯一 FAIL-1=027 分类应为细化）均为历史，**不能作为当前票**。不拼接旧 29 项代替本整批。

---

## 0. 技能预检（本节点实核）

按意图执行：B2 只用结构，不修代码、不派子代理、不等操作员。grilling 的「等用户回答 / 派子代理查事实」作废，自问自答并自己读源码。receiving-code-review 的 IMPLEMENT 作废。verification-before-completion：PASS/FAIL 前先核指纹与源码。

| 技能 | 本席路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| using-superpowers | superpowers 6.3.0 | 开工技能自检；用户本条指令覆盖默认“先 brainstorm 再动手” | 无 |
| receiving-code-review | superpowers 6.3.0 | 只借“先核实再接受或反驳、禁止表演性同意”；禁止 IMPLEMENT | 无 |
| grilling / grill-me | mattpocock productivity | 对抗式追问结构，自问自答 | 无 |
| verification-before-completion | superpowers | FAIL 前核指纹、HTML、源码 | 无 |
| ponytail-review | ponytail 4.9.0 | 复核 §6 可删建议（019 构造器；027 不得只删权限桥、不得伪造渠道成功） | 无 |
| codebase-design / diagnosing-bugs / systematic-debugging | 存在 | 只读定位，不改生产代码 | 无 |
| writing-for-agents | 存在 | 报告步骤与完成标准 | 无 |
| tdd / test-driven-development / ponytail | 存在 | B2 禁止修复，不用 | 节点不适用 |
| resolving-merge-conflicts | 存在 | 无合并冲突 | 节点不适用 |
| git-guardrails-claude-code | 存在 | 未装 hook；本步未 push/reset | 可选，未装 hook |
| i-have-adhd | cache `i-have-adhd/0.1.0` | 存在；完整 30ID 清单优先于“列表≤5” | 无 |
| batch-grill-me / ubiquitous-language | 本席技能树未找到 | — | **缺口：不存在，未冒充调用** |
| brainstorming / writing-plans | 存在 | 阶段 A 技能，本步 B2 不用 | 节点不适用 |

划掉不用（与合同一致，未调用）：dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、executing-plans、ponytail-audit/debt/gain、research。

---

## 1. 输入指纹（本席实算，与派发声明对照）

调用开始时实算（本席以此为 B2 输入；其后并发增量不当输入）：

| 输入 | 字节 | SHA256 | 对照 |
|---|---:|---|---|
| `docs/audit-2026-09/modules/M006-b1-self-raw.md` 调用开始全文 | 103335 | `470e4c6ea4cad5862411bbdee8f3578f3691e9bbdfc4638ccb99b958a046e71e` | 与派发一致 |
| 同上前 73231 字节（独立 B1 V0.05） | 73231 | `1ee77bccf61c3b418028f42866202a4655246349d7c55ef37d9e3f36def61ab4` | 与派发一致；其后接收/旧 B2 票**不是新增独立 B1** |
| 本席 `docs/audit-2026-09/modules/M006-b1-grok-raw.md` 追加前 | 93035 | `489614955b1aa9a3c903a901720b795bbc9b1973fd4939db2a526b97ef35c501` | 与派发一致；旧字节全部保留 |
| 同上前 44940 字节（独立 B1 V0.01） | 44940 | `96f0165d01b18753284b2556d2474c17623259bdb10ddca45cc80b7a0b5da071` | 与派发一致 |
| 候选 `docs/audit-2026-09/findings.md` V0.56 | 221921 | `668e4a199457bf464bc53cb263f64f05a15ff56a578defcad079db2a7ba7dfd6` | 与派发一致；**只审 M006 当前完整 30ID 表、逐项证据/拟修范围和 V0.56 最新说明，不审 M001—005 历史段** |
| `docs/audit-2026-09/questions/M006-027.md` V0.05 | 10423 | `70ff002a0fdcc16712c030fdc0b5b9ae0ee186bcf5af55164d1b9517d332debc` | 与派发一致 |
| HEAD | — | `395e6fbab027020da1c38237fad17e8edcd7a132` | 与派发一致 |
| lock 实算 / sidecar / ledger 基准 | — | `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de` | 与派发一致；COUNT=0 |

`questions/M006-027.md` `## 操作员答复` 段首个非空行精确为 `保留`。其后完整原话（充值拉起微信/支付宝/银联、渠道成功确认后入账、POS；App 已有订单余额支付；现场原出示二维码后改为报手机号短信扣款）附于同一文件。判定：最新明确业务范围规范化，不是默认预选、不是沉默、不是沿用 M003 答复。V0.02 历史「删除」、V0.03 二维码及 A/B HTML 申请均已撤回，不采迟到旧稿。本批 0 添加待问。无待批 HTML 修改，不触发铁律 4。候选 027 证据栏写「questions … V0.04」为版本标签陈旧，实际文件 V0.05；答复首行与拟修业务文字与 V0.05 一致，**不单列否决**。

V0.56 说明已接受：本席 V0.03 唯一 FAIL-1（027 分类添加→细化）已改；Codex 未以 self PASS 覆盖本席 FAIL、未删除改写本席原票；交易表名纠错为 `customer_stored_value_transactions`（本席原名正确，Codex 先前指正错误），实读 `JdbcCustomerRepository` 1181/1212/1223 与 `V090__member_stored_value_paid_membership_transfer.sql` 第 31 行一致，修复须复用该表、不改原 raw、未改源或造表。

---

## 2. 差集方法

B2 无互盲。全集：

- Codex 独立 B1：CX-01—CX-23（self-raw 前 73231 字节 / V0.05）
- 本席独立 B1：GR-01—GR-11 及 §6 四条可删意见（本文件前 44940 字节 / V0.01）
- 候选正式 ID：M006-001—030（findings V0.56 当前表）

映射核对（与候选声明一致，无漏并）：

- CX-01—23 → 001—023
- GR-01→001、02→024、03→025、04→015、05→011、06→026、07→027、08→028、09→010、10→029、11→030
- §6 未编号兼容构造器删除建议 → 019
- §6 其余：UserDetails/未知主体删除 → 028；英文建议句 → 015；假分页收缩 → 030

Codex 独立 B1 之后的接收/旧 B2 票不另计新 B1。本席 V0.02/V0.03 旧 B2 不参与本票合取。未拼接旧 29 项 PASS 免审。本次仍全 30 项整批复核，不只审 027 差异。

---

## 3. §5 闸 2：未映射但已在冻结定义区的 HTML 原文

M006 映射行含 52 个 ID：GEN-001—018、BEQR-157/182—187、BSM-001/003/009/028/029/073/084—088/114/115/120、PGM-001/008/022/024/026/032—036/048/059/066。**不含** BSM-004、BSM-005、BSM-006、BSM-007、BSM-035、PGM-029、PGM-041，也不含第三方接入 h2（HTML1480）与枚举「统一原则」（枚举 210）。

锁内定义区已冻结这些锚点的 HTML 原文。候选 **没有** 把它们写入 M006 映射，也没有新增判据 ID。001/007/009/011/026/027 的“已冻结判据”栏仍只列本模块已映射 ID；未映射原文只作 GEN-005/006/017 分类的类型 1 证据。

本席裁定：**未扩 52 映射、不触发 LOCK-AMEND、不重开 M001。** 三 HTML 当前不改。未映射冻结 HTML 仍只作已映射 GEN-005/006/017 上位事实。HTML1480/枚举210 同此口径：可约束 027 **拟修最小范围**，不得写成新判据或改映射。

---

## 4. 本批否决项（一次列齐；发现第一处未退出）

### FAIL-1　M006-027 拟修最小范围不完整（分类/队列已接受，范围不接受）

- 候选当前值：分类 **细化**；工程缺陷 **是**；队列 **是**。
- 本席裁定：分类 **细化**（接受 V0.56 对 V0.03 FAIL-1 的改判）；工程缺陷 **是**；队列 **是**；**拟修边界不接受**。
- 已接受部分（不再否决分类）：
  - 2.1：能指到 HTML 832/585/867/1065 上位 = 细化，不是添加。本席 B1 GR-07 以 BSM-114 客人入口摘要无储值判添加，该摘要不能压过同文件 832/867 明文。0 添加待问。
  - 操作员保留限定充值/POS/已有订单余额支付/现场报本人手机号+短信验证码按账单扣款；不生成储值付款二维码；取消钱包页脱离订单/账单任意金额扣款；保留本人余额与逐笔记录；不能只删权限桥；不能放宽任意客人/商户。
  - 现实现与上位/保留范围不符，缺陷成立：`WalletPage.tsx:66-77` 充值后提示柜台；`79-94` 任意金额 + `reason=钱包消费`；`StoredValueConsumeRequest` 无订单/账单/短信字段；页面 `message.success("消费成功")`；钱包页无 867 逐笔记录；`StoredValueService.consume` 无订单/账单/SMS 绑定。`settleTopup` 已要求支付意图 `succeeded` 才入账。
- **否决原因（最小范围）**：V0.56 027 拟修边界写「客人在充值页选择金额并拉起微信、支付宝或银联线上支付，服务端核验对应渠道支付成功确认后入账」，但 **未写入** 下列已冻结 HTML 与既有实现事实。B3 若按「拉起微信/支付宝/银联」字面实施，会把本项读成接通真实渠道或伪造成功，超出本模块可归因的最小修复，并与 HTML1480/枚举210 冲突。
  1. HTML1480（业务地图第三方接入 lead）：`当前还没有真实供应商接口资料，所以先开发自己的系统和开放接口`。聚合支付行：拿回各渠道支付与退款结果；`不自研支付通道、不沉淀资金`。短信平台行：验证码下发与送达结果；`不自研短信通道`。
  2. 枚举速查 210「统一原则」：`预留接口 + 后台开关默认关 + 不阻塞自研 + 不伪造对账结果`。
  3. 现实现：`ReservedPaymentGateway` 在开关开且无真实厂商时返回 `NOT_CONFIGURED`，**不是**已接微信/支付宝/银联 SDK。`DisabledPaymentGateway` 在 `payment.gateway.enabled=false` 时返回 `RESERVED`。V093 显式 seed 该开关 `FALSE`（默认关）。不得把 ReservedPaymentGateway 称为已接真实厂商。
  4. 储值充值内核已存在且应对齐：`StoredValueService.topup` 只建支付意图不入账；`settleTopupInternal` 校验 `intent.status==succeeded` 才写 `customer_stored_value_transactions`；POS/柜台走 `confirmOfflineTopupAndSettle`。注释写明测试期 `markPaymentIntentSucceeded`。拟修未禁止把该测试入口或客户端成功提示当作渠道确认。
  5. HTML585 储值支付须「手机号加短信验证」继续适用，不得豁免 App 订单余额支付。现 `consume`/`StoredValueConsumeRequest` 无验证码字段；`notifyStoredValueAfterCommit` 是金额变动通知模板，不是验证码通过。拟修未写明：复用既有短信验证通道；未配置短信能力不得当作已验证通过；不得以「已发短信/收到通知」代替验证码通过。
- 拟修边界 **必须补入**（不扩锁、不改 HTML、不重开 M001、不把本项写成开启未配置能力的授权）：
  - 充值：客人端走已有 `createPaymentIntent` → 渠道/POS 成功由服务端核验 → `settleTopup` 入账；重复确认不得重复加余额。渠道未配置或开关默认关时，不得入账、不得伪造 `SUCCEEDED`、不得打开 `payment.gateway.enabled`、不得改 `ReservedPaymentGateway` 去假装已接厂商。
  - POS：复用已有柜台/线下确认链，不另造支付通道。
  - App 已有订单余额支付、现场账单报本人手机号+短信验证码扣款：须绑定订单/账单，走 HTML585 短信验证；取消钱包页任意金额 `consume`；权限桥 `customer.stored_value.topup/consume` 保留为限定合法路径，不得只删桥。
  - 不得让客户端自行宣称收款成功入账。
  - 逐笔记录复用 `customer_stored_value_transactions`，不建平行台账（V0.56 说明已接受表名，拟修边界须写进以免 B3 另建）。
- 分类细化、队列是、0 添加、无 HTML 修改、无 LOCK-AMEND：这些 **接受**。只否决拟修最小范围。

无第二处分类/漏并/队列否决。下列 001—026、028—030 本席接受候选当前值。

---

## 5. 主要分歧逐条裁决（先核实源码，再改判或维持）

原 B1 报告不覆写。下列是本票对候选分类/队列/范围的本席票。

### 024（GR-02）账号状态 vs 员工雇佣五态

- 本席 B1：冲突（须改成 BEQR-157 五值）。
- 候选：细化；缺陷是；队列是。收窄为写入口无状态白名单、非 disabled 发 active 凭证。
- 核实：`AccessService.java:870` 默认 `"active"` 无枚举校验；`JdbcAccessCredentialProvisioner.java:33` 非 disabled→凭证 active；`AuthorizationService` 比较 `"active"`；`ALLOWED_ACCOUNT_TYPES` 含 merchant/outsourced_vendor。Python：`active ∈ {待入职,在职,试岗,停岗,离职}` = False。
- **裁决：同意候选细化、队列是。** 把登录三态改成雇佣五态，会把商户/外包账号发明成员工状态。必须修未知状态不拒绝、非启用仍发可用凭证。不得把 `active` 改成「在职」假装修完。员工注册审批归 011。

### 015（GR-04）英文展示

- 本席 B1：冲突，含成功建议句及 `valid_until is required` 等异常字符串。
- 候选：冲突；只修两条成功英文。
- 核实：`AccessService.java:764,773` 两句英文写入 recommendation/summary。`GlobalExceptionHandler.java:48-61` 调 `FieldLabels.translate`；`FieldLabels.java:27,504-529` 纯英文无匹配走中文 `FALLBACK`（`提交内容有误，请检查后重新提交`）。
- **裁决：同意候选。** 拟修只改两条用户可见成功说明为中文。不以源码搜英文字符要求全包替换。内部权限码保留。

### 011（CX-11；GR-05）后台新建 vs 注册审批 vs 员工自助

- 本席 B1 GR-05：细化（后台建号允许，生效员工必须有员工自助）。
- Codex CX-11：冲突（第三条免审批激活入口）。
- 候选：冲突；保留 582 分步新建；封闭未经公司批准即激活并发密码；获批后自动 `role_staff_self`。
- 核实：HTML 581-582「新建员工账号」是分步引导表单，无免审批句。HTML 1433 两条注册路径均须公司批准后生效（BSM-085 已映射）。HTML 1428 审批通过自动员工自助（BSM-084 已映射）。`createAccessUser`：默认 active、`createdVia=manual_admin`、发密码、不赋 `role_staff_self`。`StaffService.java:199-212` 审批通过才 `createUser` 并 `assignUserRole(..., "role_staff_self")`。
- **裁决：同意候选冲突分类与拟修边界，队列是。** 表单 ≠ 豁免。GR-05 自助义务并入本 ID。不得给未获批账号直接赋自助。

### 025（GR-03）与 021

- 核实：`AccessService.java:911-922` 无密钥 SHA-256；`AccessUser.phoneHash` 经 API 返回。PGM-059「一律加密存放」。021 只登记 BCrypt/UTF-8/JDBC。
- **裁决：025 冲突、队列是；021 技术性、队列否。** 不得把无密钥摘要说成加密。最小方向优先去掉无消费用途的采集/对外返回；确需保存则复用已有 PiiCipher/BlindIndex，不扩成全仓 PII、不做存量回填。

### 026（GR-06）物理删除

- 本席 B1：细化 + GEN-006；拒绝用未映射 BSM-004 作冲突依据。
- 候选：冲突；删除物理 DELETE，复用已有存档/审批/留存。
- 核实：`AccessService.java:466-482` 直接删；`JdbcAccessRepository.java:520-524` `DELETE FROM position_role_templates`。HTML 563-578 要求先存档、审批、软删除、库内可恢复、查占用。BSM-003 已映射；BSM-004 未映射但定义区已冻结，作 GEN-006/017 类型 1 证据。
- **裁决：同意冲突分类与队列是。** 岗位角色模板是配置类主数据。不能只改 `status=inactive` 宣称删除审批完成。不把 BSM-004 写入本模块映射。

### 030（GR-11）假分页

- 本席 B1：技术性。
- 候选：细化；禁止无出口截前 50。
- 核实：`AccessService.java:50-51,1733-1734` `PageResponse<>(items, 1, 50, items.size())` 不切片；`listGrantRequests` 在 `windowDays==null` 时 `days=0`、`cutoff=null`。
- **裁决：同意细化、队列是。** 列表是 BSM-028 下位实现。元数据不实必须修；若分页必须能取后续页。禁止无出口截前 50。不能把 pageSize 当成业务政策。与 016 频繁阈值分开。

### 019（CX-19；§6 构造器）

- 本席 §6：建议删除 7 参 legacy 构造。
- 候选：技术性、缺陷否、队列否；有真实调用。
- 核实：`AccessUser.java:24-34` 7 参。生产调用：`StaffService.java:199-207` `new AccessUser(..., "active", now)` 七参。测试夹具多处同构。
- **裁决：同意队列否。** 删除构造器会迫使无本 ID 的调用改写，违反闸 3。本项只登记容器/兼容机制。

### 027（GR-07；操作员保留）

见 §4 FAIL-1。分类改判细化 **接受**；队列是 **接受**；拟修范围 **不接受**，须补 HTML1480/枚举210 预留接口、默认关、不伪造、不开启未配置支付/短信、不称 ReservedPaymentGateway 已接真实厂商。V0.53 只读关闭写通路、V0.54 二维码及 HTML 申请 A/B 均不执行。

---

## 6. 三十 ID 逐条结论

图例：差集=原票是否完整进入候选；本席 B2=是否接受候选**当前**分类/队列/范围。

| ID | 来源 | 候选分类 | 工程缺陷 | 队列 | 本席 B2 | 说明 |
|---|---|---|---|---|---|---|
| 001 | CX-01；GR-01 | 冲突 | 是 | 是 | 接受 | 通配先于 DENY（`AuthorizationService.java:39-46`）；`currentSupervisorLevel` 对管理员直接返回 3。日常自动权限按 HTML 保留，不得用通配覆盖资金/删除/最终审批。BSM-007 原文作 GEN-005 证据，未写入映射。 |
| 002 | CX-02 | 冲突 | 是 | 是 | 接受 | `requireSameOrganization` 只比 org（`AccessService.java:1615-1622`）。未重跑 CROSS_MERCHANT 探针，源码边界成立，采信 B1 已落盘输出。 |
| 003 | CX-03 | 细化 | 是 | 是 | 接受 | 空组织当全量。 |
| 004 | CX-04 | 细化 | 是 | 是 | 接受 | `slug()` 当全局主键 + upsert。 |
| 005 | CX-05 | 细化 | 是 | 是 | 接受 | 改已应用模板旁路审批。未重跑探针，源码 update 不核授出能力。 |
| 006 | CX-06 | 细化 | 是 | 是 | 接受 | 批 A 执行 B。 |
| 007 | CX-07 | 冲突 | 是 | 是 | 接受 | 受益人自审；PGM-029 作类型 1 证据，未写入映射。 |
| 008 | CX-08 | 细化 | 是 | 是 | 接受 | 不同幂等键并发双生效。未重跑双线程，采信 B1 输出 + 无 CAS 的源码边界。 |
| 009 | CX-09 | 冲突 | 是 | 是 | 接受 | 换岗只追加；PGM-041 作类型 1 证据，未写入映射。 |
| 010 | CX-10；GR-09 | 细化 | 是 | 是 | 接受 | `supervisor` 与 `supervisor_lN` 分裂（`AccessService.java:52-53` vs `AuthorizationService.java:245-257`）。 |
| 011 | CX-11；GR-05 | 冲突 | 是 | 是 | 接受 | 见 §5。 |
| 012 | CX-12 | 细化 | 是 | 是 | 接受 | `effectivePermissionsForUser` 无条件 `currentMerchantScope`。 |
| 013 | CX-13 | 细化 | 是 | 是 | 接受 | 空权限/倒置时间窗仍可返回。 |
| 014 | CX-14 | 细化 | 是 | 是 | 接受 | `reviewGrant` 连续两次 `requirePermission`。 |
| 015 | CX-15；GR-04 | 冲突 | 是 | 是 | 接受 | 见 §5。 |
| 016 | CX-16 | 冲突 | 是 | 是 | 接受 | `@Value` 默认 3、`Math.max`、`riskRank` 写死层级；BSM-001 已映射。 |
| 017 | CX-17 | 细化 | 是 | 是 | 接受 | 单次消费留痕外键。未重跑 TEMP SQL，源码足以支持缺陷存在。 |
| 018 | CX-18 | 细化 | 是 | 是 | 接受 | 管理员改模板把 owner 置 null。 |
| 019 | CX-19；§6 构造器 | 技术性 | 否 | 否 | 接受 | 见 §5。无业务发明。 |
| 020 | CX-20 | 技术性 | 否 | 否 | 接受 | profile 装配；空密码伪成功归 029。无业务发明。 |
| 021 | CX-21 | 技术性 | 否 | 否 | 接受 | BCrypt/UTF-8/JDBC；手机号归 025。无业务发明。 |
| 022 | CX-22 | 技术性 | 否 | 否 | 接受 | HTTP/仓储默认失败；假分页归 030。无业务发明。 |
| 023 | CX-23 | 细化 | 否 | 否 | 接受 | 记录结构有 BSM-028/120 上位；计算错误已单列。 |
| 024 | GR-02 | 细化 | 是 | 是 | 接受 | 见 §5。原冲突主张保留在证据栏，本席改判细化。 |
| 025 | GR-03 | 冲突 | 是 | 是 | 接受 | 见 §5。 |
| 026 | GR-06 | 冲突 | 是 | 是 | 接受 | 见 §5。本席 B1 细化主张保留在证据栏，本席改判冲突。 |
| 027 | GR-07；操作员保留 | 细化 | 是 | 是 | **不接受拟修范围** | 见 §4 FAIL-1。分类细化与队列是接受；拟修须补预留支付/短信约束。 |
| 028 | GR-08 | 技术性 | 是 | 是 | 接受 | UserDetails 与未知 principal 均填 staff/active（`AuthorizationService.java:146-149`）。不夸大已证网络攻击。无业务发明。 |
| 029 | GR-10 | 技术性 | 是 | 是 | 接受 | 空密码仍成功返回（`AccessService.java:878-879`；两 provisioner 空白直接 return）。无业务发明。 |
| 030 | GR-11 | 细化 | 是 | 是 | 接受 | 见 §5。本席 B1 技术性主张保留在证据栏，本席改判细化。 |

候选表队列合计 25 是 / 5 否（019—023）/ 待答 0。分类统计 9 冲突 / 15 细化 / 6 技术性 / 0 添加。本席改判只触及 027 **拟修文字**，**队列计数不变**（仍 25 是 / 5 否）。修复队列规则：冲突 / 有工程缺陷 / 操作员答删除的添加 → 是；只登记且无工程缺陷 → 否。027 在细化+缺陷下仍是，不因保留而出队。

无漏并：23 CX + 11 GR + §6 构造器全部进入 30 ID。无把本席票改写成只审 11 GR 或只审 027。无把 Codex 反例（002—018 等）降级或默删。无改写原 finding 事实。

---

## 7. 技术性「无业务发明」复核（2.1）

| ID | 无业务发明理由 | 本席 |
|---|---|---|
| 019 | List.copyOf / 兼容构造，不引入资格或审批规则 | 同意技术性 |
| 020 | Spring profile 与凭证桥，不新增授权规则 | 同意技术性 |
| 021 | BCrypt/标准库/JDBC 适配；手机号用途不在本项放行 | 同意技术性 |
| 022 | MVC/ApiResponse/默认失败；分页行为不在本项放行 | 同意技术性 |
| 028 | 框架 principal 适配既有主体，不发明新角色名 | 同意技术性；有工程缺陷，队列是 |
| 029 | 未执行已有「初始密码必填」输入契约 | 同意技术性；有工程缺陷，队列是 |

无一项被本席从技术性改判为添加。027 分类细化已接受，不是技术性，也不是添加。

---

## 8. ponytail-review（只列可删，不修）

`AuthorizationService.java:L146-149: delete: UserDetails/generic principal 伪造 staff/active。生产过滤器已提供 Actor/UserRecord。`（归 028，待修）  
`AccessUser.java:L24-34: 不删。StaffService 七参真实调用。`（归 019，队列否）  
`AccessService.java:L1733-1734: shrink: 假分页。真正切片或去掉 pageSize 谎言，且必须能取后续记录。`（归 030）  
`WalletPage 任意金额消费入口: delete: 脱离订单/账单的金额框扣款。权限桥不删。`（归 027）  
`ReservedPaymentGateway / payment.gateway.enabled: 不删接缝，也不开开关、不改成伪造 SUCCEEDED。`（归 027 拟修约束）  
`net: 假分页/伪造主体/任意金额消费可删或收缩；7 参构造器不可删；预留支付接缝不可冒充已接通。`

---

## 9. 真实验证命令与缺口

本次 B2 **未**运行会写 `backend/target` 的 Maven/测试。未连生产库、未 HTTP 写、未安装依赖、未 source 整 env、未改 SQL/HTML/配置/台账/问题。未创建 `.scratch/m006-grok-*`（本步源码/HTML 原文足够，无需新可执行反例）。未派子代理。未启动其他模块全审。未读索引/无关历史。

实跑（只读）：

```
git rev-parse HEAD
→ 395e6fbab027020da1c38237fad17e8edcd7a132
git rev-parse --abbrev-ref HEAD
→ main

sha256sum docs/audit-2026-09/plan/criteria-lock.md
→ 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
cat docs/audit-2026-09/plan/criteria-lock.sha256
→ 同值
grep ledger 顶部两行
→ CRITERIA-LOCK-SHA256 同值；LOCK-AMEND-COUNT: 0

python3 计算四份必读输入字节与 SHA256
→ 调用开始与派发声明逐项相同
→ self 103335 / grok 93035 / findings 221921 / questions 10423
→ 独立 B1 前缀 self[:73231] = 1ee77bcc… ；grok[:44940] = 96f0165d…
```

窄读（直接引用，非全仓内容搜索、非其他模块全审）：M006 十四文件关键行；`JdbcAccessRepository` 删除 SQL；`StaffService` 七参建号；`GlobalExceptionHandler`/`FieldLabels`；`WalletPage`/`MemberWalletController`/`StoredValueConsumeRequest`/`StoredValueService` topup/settle/consume；`JdbcCustomerRepository` 1181/1212/1223；`V090` 第 31 行建表；`ReservedPaymentGateway`/`DisabledPaymentGateway`/`V093` 开关默认关；三份 HTML 563-588、832、867、1065、1428-1433、1480-1533、枚举 210；lock M006 映射行。为核实 019 调用，对 `new AccessUser(` 做了类型名定向检索（已知 record 构造）。命中生产 `StaffService` 与本模块测试夹具。

**缺口（如实）：**

- 未重跑本席 B1 的 55 单测、AdminDenyHarness、UserDetailsFallbackHarness。
- 未重跑 Codex B1 的 48 源 javac 探针、双线程审批、complex_test TEMP 外键。
- 未跑 AccessControllerTests / AccessJdbcIntegrationTests / 真实 HTTP。
- 客人储值消费在无 merchant 时是否实际成功，未新跑；拟修是取消任意金额消费并绑定订单/账单，不依赖该路径已可用。
- 未在 `.scratch/m006-grok-*` 落临时编译产物。

上述缺口不构成待补审计项：B2 审的是候选归并，不是再做一遍全模块 B1。不以未重跑否定已落盘 B1 输出；分类与范围争议均落到本次源码/HTML 原文。

---

## 10. 整体结论

**FAIL**

- 30 ID 无漏并、无改写原缺陷事实、无把 Codex 反例丢掉。
- 027 分类添加→细化已按本席 V0.03 改判，**接受**；队列仍 25 是 / 5 否 / 待答 0 / 0 添加，**接受**。
- **唯一否决 FAIL-1**：027 拟修最小范围未写入 HTML1480/枚举210 预留接口、能力开关默认关、不伪造结果、不得开启未配置支付/短信、不得称 ReservedPaymentGateway 已接真实厂商、不得让客户端自行宣称渠道成功。其余 29 项分类/队列/范围接受（含 024/015/011/025/021/026/030/019 原分歧）。
- 未映射冻结 HTML 作类型 1 证据合规，未扩锁、未改 52 映射、不重开 M001、无待批 HTML。
- 表名 `customer_stored_value_transactions` 以源码为准，V0.56 说明纠错接受。
- 不存在有条件通过。本票只定 B2 候选复核；B3 未执行。

本席 FAIL 具否决权。Codex 自审 PASS 不得覆盖。须按 FAIL-1 补 027 拟修边界后重复本批 B2 ②；不得以自审 PASS 覆盖本席 FAIL。

# M006 B2 整批候选复核（Grok 席）
- 版本：V0.05
- 角色：审计席乙（独立；第一性原理 / 对抗式 + 顺从式）
- 模块：M006 `10-backend-main-access-01`（6/322）
- 步骤：B2 整批候选差集 / 分类 / 队列 / 范围复核
- 控制：V0.17；ledger `IN_PROGRESS`；B3=0；planrepair=2；runtimefailure=0
- 本席只读追加本文件；不重开 B1、不进入 B3、不修代码、不改方案、不改 HTML/锁/台账/问题。
- 本席旧票：V0.02（钱包只读 PASS）、V0.03（唯一 FAIL-1=027 分类应为细化）、V0.04（唯一 FAIL-1=027 拟修缺预留支付/短信边界）均为历史，**不能作为当前票**。不拼接旧 29 项代替本整批。其后接收与旧 B2 报告不当新 B1 票，也不替本次整批票。

## 0. 技能预检（本节点实核）

按意图执行：B2 只用结构，不修代码、不派子代理、不等操作员。grilling 的「等用户回答 / 派子代理查事实」作废，自问自答并自己读源码。receiving-code-review 的 IMPLEMENT 作废。verification-before-completion：PASS/FAIL 前先核指纹与源码。ponytail-review 只列可删不修。

| 技能 | 本节点 | 缺口 |
|---|---|---|
| using-superpowers | 开工前自检 | 无 |
| grilling | 只用追问结构 | 不等操作员、不派子代理 |
| verification-before-completion | PASS 前实算指纹/源码/HTML | 无 |
| receiving-code-review | 先核实再接受 V0.57 对 V0.04 FAIL-1 的补写 | IMPLEMENT 作废 |
| ponytail-review | §8 | 不修 |
| diagnosing-bugs / systematic-debugging | 只借读源码→数据流 | 不插桩、不改码 |
| codebase-design | 非新判据观察 | 无 |
| tdd / test-driven-development | B2 不落测试 | 不适用 |
| resolving-merge-conflicts | 无合并 | 不适用 |
| writing-for-agents | 本报告给调度席读 | 无 |
| batch-grill-me / ubiquitous-language | 本席技能树不存在 | 记缺口，未冒充已调用 |
| git-guardrails-claude-code | 未装 hook | 记缺口 |

未核到的不写已核实。未派子代理。未启动其他模块全审。未读索引/无关历史。未全仓内容搜索；对已知类名做了 `backend/src/main` 文件名定位（支付网关在 `finance/gateway/`，StaffService 在 `access/staff/`，FieldLabels 在 `common/api/`）。

## 1. 输入指纹（本席实算，与派发声明对照）

调用开始时实算（本席以此为 B2 输入；其后并发增量不当输入）：

| 路径 | 字节 | SHA256 | 对照 |
|---|---:|---|---|
| `docs/audit-2026-09/modules/M006-b1-self-raw.md` 前 117360 字节 | 117360 | `57c2e793791b534696e1010d8849c2bd927d2c6928dc29fc09ddc33e0361c64e` | 与派发一致；当时整文件亦 117360 |
| 同上前 73231 字节（独立 B1 V0.05） | 73231 | `1ee77bccf61c3b418028f42866202a4655246349d7c55ef37d9e3f36def61ab4` | 与派发一致；其后接收/旧 B2 票**不是新增独立 B1** |
| `docs/audit-2026-09/modules/M006-b1-grok-raw.md` 前 120180 字节 | 120180 | `7c68d350332b25ad1a8610d791c0c60e41683f71fa94adf084cfaf23f4e78a4c` | 与派发一致；本追加前整文件亦 120180 |
| 同上前 44940 字节（独立 B1 V0.01） | 44940 | `96f0165d01b18753284b2556d2474c17623259bdb10ddca45cc80b7a0b5da071` | 与派发一致 |
| `docs/audit-2026-09/findings.md` V0.57 | 223966 | `f20a175d6024c22e51bebd7395554b68abff8c37771f9d4972d28a13ccfb9fe9` | 与派发一致 |
| `docs/audit-2026-09/questions/M006-027.md` V0.06 | 11550 | `c5c6705ec86c8a848e99cdf6bbe8fd6547a80365bf2375d35539ded5080fab17` | 与派发一致 |

本席独立实算 `docs/audit-2026-09/plan/criteria-lock.md` 全文哈希 = `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（sidecar 与 ledger 顶部同值；锁修订计数 0）。本 B2 报告不新增该机读哈希行。`git rev-parse HEAD` = `395e6fbab027020da1c38237fad17e8edcd7a132`，分支 `main`。

`questions/M006-027.md`：`## 操作员答复` 段首个非空行精确为 `保留`。两份旧 HTML 申请 A/B 正文保留为历史且标明撤回。findings 027「当前拟修边界」与 questions 最新「当前限定范围」逐字相同（各 1015 字）。

V0.57 说明已接受：本席 V0.04 唯一 FAIL-1（027 拟修正文缺预留支付/短信边界）写入 027「当前拟修边界」正文并同步 question，不再只放 self 或提示。本票**逐句核对该正文**，不以页首/派发前缀代替条目。

## 2. 差集方法

B2 无互盲。全集：

- Codex 独立 B1（self-raw 前 73231 字节）CX-01…CX-23
- 本席独立 B1（本文件前 44940 字节）GR-01…GR-11 及 §6 未编号意见
- 当前候选 `findings.md` V0.57 全部 30 个连续 ID 的分类 / 工程缺陷 / 队列 / 拟修范围
- 只审 M006；不审 M001—005 历史

映射（与候选表一致，本席逐项对照，不另编号）：CX01—23→001—023；GR01/02/03/04/05/06/07/08/09/10/11→001/024/025/015/011/026/027/028/010/029/030；§6 构造器建议→019；§6 英文建议→015；§6 假分页→030；§6 UserDetails fallback→028。

Codex 独立 B1 之后的接收/旧 B2 票不另计新 B1。本席 V0.02/V0.03/V0.04 旧 B2 不参与本票合取。未拼接旧 29 项 PASS 免审。本次仍全 30 项整批复核，不只审 027 差异。

## 3. §5 闸 2：未映射但已在冻结定义区的 HTML 原文

候选仍用下列已冻结原文作类型 1 证据，**未**把它们写入 M006 映射（映射仍 52 ID，本席数过）：

- BSM-007（HTML 588，日常管理员 ≠ 资金/删除/最终审批自动授予）→ 001，经 GEN-005/017
- PGM-029（申请人与直接受益人不得自审）→ 007，经 GEN-005/017
- PGM-041（换岗重算、旧岗位权限失效）→ 009，经 GEN-005/017
- BSM-004 程序（HTML 563-578 存档/审批/软删除）→ 026，经 GEN-006/017；BSM-003 已映射
- BSM-035 储值（HTML 832）与 BSM-006 短信验证（HTML 585）→ 027，经 GEN-005/017
- HTML 1480 第三方接入 lead / 聚合支付「不自研支付通道、不沉淀资金」/ 短信平台「不自研短信通道」及枚举 210 统一原则 → 027 拟修约束的类型 1 出处，不扩映射

不改 52 映射、不扩锁、不重开 M001、无待批 HTML、无 LOCK-AMEND。

## 4. 本批否决项（一次列齐；发现第一处未退出）

无。V0.04 唯一 FAIL-1 已进入 027「当前拟修边界」正文（并与 question 同步）。下列 §4.1 为逐句覆盖核验，不是新的否决。

### 4.1 M006-027 拟修正文逐句覆盖（相对 V0.04 FAIL-1 / 派发必补清单）

候选当前值：分类 **细化**；工程缺陷 **是**；队列 **是**。questions 首答 **保留**。0 添加待问。

已接受且本票维持：2.1 能指到 HTML 832/585/867/1065 上位 = 细化；操作员保留限定充值/POS/已有订单余额支付/现场报本人手机号+短信验证码按账单扣款；不生成储值付款二维码；取消钱包页脱离订单/账单任意金额扣款；保留本人余额与逐笔记录；不能只删权限桥；不能放宽任意客人/商户。现实现与该范围不符，缺陷仍成立：`WalletPage.tsx:66-77` 充值后提示柜台；`79-94` 任意金额 + `reason=钱包消费`；`StoredValueConsumeRequest` 无订单/账单/短信字段；页面 `message.success("消费成功")`；`StoredValueService.consume` 无订单/账单/SMS 绑定。`settleTopupInternal` 已要求 `intent.status==succeeded` 才入账。

| # | V0.04 / 派发要求 | 现「当前拟修边界」原文落点 | 覆盖 |
|---|---|---|---|
| 1 | HTML1480：尚无真实供应商资料，先开放接口；聚合支付拿回渠道结果，不自研支付通道、不沉淀资金；短信平台不自研短信通道 | 开篇「在HTML1480及枚举210既有…阶段内」+「不把本项当成…接通真实厂商的授权」+「不另造支付通道」+「复用既有CustomerOtpVerifier」 | 是 |
| 2 | 枚举210：预留接口 + 后台开关默认关 + 不阻塞自研 + 不伪造对账结果 | 「预留接口、后台开关默认关、不阻塞自研、不伪造结果」（「不伪造结果」⊇「不伪造对账结果」） | 是 |
| 3 | 未配置或关闭的在线渠道不能入账或伪造 SUCCEEDED | 「开关关闭、RESERVED或NOT_CONFIGURED时应明确提示未启用/未配置，不能显示已支付、不能入账」+「不修改ReservedPaymentGateway来伪造SUCCEEDED」 | 是 |
| 4 | 不打开 `payment.gateway.enabled` | 「不打开payment.gateway.enabled」；收尾「开关启用状态不修改」 | 是 |
| 5 | 不假称 `ReservedPaymentGateway` 已接厂商 | 「不把本项当成…接通真实厂商的授权」；「仅在真实渠道已配置且后台显式启用时使用真实拉起信息」；RESERVED/NOT_CONFIGURED 须提示未启用。源码：`ReservedPaymentGateway` 返回 `NOT_CONFIGURED`，`DisabledPaymentGateway` 在开关关时返回 `RESERVED`，V093 显式 seed `FALSE` | 是 |
| 6 | 不用测试 `markPaymentIntentSucceeded` / 前端成功作真实确认 | 「不拿测试期markPaymentIntentSucceeded或前端成功提示代替真实渠道回执」；「客户端不得自行宣称收款成功」 | 是 |
| 7 | 既有 `createPaymentIntent` / `PaymentGatewayPort` → 服务端核验真实渠道成功 / 金额 / 绑定本人卡 → `settleTopup` | 「经既有createPaymentIntent及PaymentGatewayPort申请支付」；「只有服务端核验对应渠道的真实支付成功确认、金额和已绑定本人卡范围后，才复用settleTopup/既有入账内核」 | 是 |
| 8 | 重复回执不重复入账 | 「重复确认不得重复加余额」 | 是 |
| 9 | POS 独立保留既有 `confirmOfflineTopupAndSettle` 柜台/线下确认链，不新造支付通道 | 「POS刷卡充值复用confirmOfflineTopupAndSettle等既有柜台/线下确认链，不另造支付通道」 | 是 |
| 10 | App 真实订单与现场已有明细/总价账单绑定本人卡，执行 HTML585 手机号短信验证码；不豁免 App 订单 | 「两类余额支付都绑定真实订单/账单与本人卡并执行HTML585的手机号短信验证，不豁免App订单」 | 是 |
| 11 | 现场按用户最新取消二维码、报手机号，不生成付款出示码 | 「现场…报本人手机号、短信验证码验证通过后按该笔账单金额扣款，不生成储值付款二维码」 | 是 |
| 12 | 既有 `CustomerOtpVerifier` 等短信通道未配置则拒绝 | 「复用既有CustomerOtpVerifier等短信验证接口；短信能力未配置时拒绝验证/扣款」。prod `FailClosedCustomerOtpVerifier` 抛 `CUSTOMER_OTP_PROVIDER_UNAVAILABLE` | 是 |
| 13 | 不把发送/送达/变动通知当验证码通过 | 「不以“已发短信”“已送达”或变动通知notifyStoredValueAfterCommit当作验证通过」。源码该方法只调 `notificationService.notify`，模板为储值变动通知 | 是 |
| 14 | 不拿本地固定码充生产验证 | 「不以本地测试固定码冒充生产短信验证」。`FixedCustomerOtpVerifier` 仅 `local/dev/test` profile，默认 `000000` | 是 |
| 15 | 不删合法限定权限桥、不放宽任意客人/商户、取消无账单任意金额消费 | 「取消钱包页…consume入口及对应孤立调用，保留限定合法customer.stored_value.topup/consume通路…也不放宽任意客人/商户授权」。「不仅删除权限桥」与前句「保留…通路」合读为不得只删桥；B3 不得删 `CUSTOMER_SELF_SERVICE_PERMISSIONS` | 是 |
| 16 | 本人余额/记录复用真实 `customer_stored_value_transactions`，不建平行台账 | 「查询复用真实既有customer_stored_value_transactions及现有卡/账务数据，不建平行台账」。V090 第 31 行建表；`JdbcCustomerRepository` 1181/1212/1223 写该表名 | 是 |
| 17 | 三 HTML / 锁 / 映射 / 开关启用状态不改，无新的用户人闸 | 「三HTML、锁定义、映射及开关启用状态不修改，不重开M001；整批B2双PASS前不实施」。A/B 已撤回；questions 无新询问 | 是 |

不扩锁、不把本项写成开启未配置能力的授权。V0.53 只读关闭写通路、V0.54 二维码及 HTML 申请 A/B 均不执行。

残余（**不构成否决**）：027「证据与分类分歧」仍有一句「Grok已明确接受…拟修范围」，那是 V0.03 时态，V0.04 否决的是范围；B3 归因只认「当前拟修边界」，该句不缩短范围。

## 5. 主要分歧逐条裁决（先核实源码，再改判或维持）

原 B1 报告不覆写。下列是本票对候选分类/队列/范围的本席票。

### 024（GR-02）账号状态 vs 员工雇佣五态

- 本席 B1：冲突（须改成 BEQR-157 五值）。
- 候选：细化；缺陷是；队列是。收窄为写入口无状态白名单、非 disabled 发 active 凭证。
- 核实：`AccessService.java:870` 默认 `"active"` 无枚举校验；`JdbcAccessCredentialProvisioner.java:33` 非 disabled→凭证 active；`ALLOWED_ACCOUNT_TYPES` 含 merchant/outsourced_vendor。`AccessUsersPage.tsx:340-342` 账号三态启用/待激活/禁用。Python 口径：`active ∈ {待入职,在职,试岗,停岗,离职}` = False。BEQR-157（枚举 L240）是员工雇佣五态，不是全部 access 账号。
- **裁决：同意候选细化、队列是。** 把登录三态改成雇佣五态，会把商户/外包账号发明成员工状态。必须修未知状态不拒绝、非启用仍发可用凭证。不得把 `active` 改成「在职」假装修完。员工注册审批归 011。

### 015（GR-04）英文展示

- 本席 B1：冲突，含成功建议句及 `valid_until is required` 等异常字符串。
- 候选：冲突；只修两条成功英文。
- 核实：`AccessService.java:764,773` 两句英文写入 recommendation/summary。`GlobalExceptionHandler.java:48-61` 调 `FieldLabels.translate`；`FieldLabels.java:27,504-529` 纯英文无匹配走中文 `FALLBACK`（`提交内容有误，请检查后重新提交`）。
- **裁决：同意候选。** 拟修只改两条用户可见成功说明为中文。不以源码搜英文字符要求全包替换。内部权限码保留。

### 011（CX-11；GR-05）后台新建 vs 注册审批 vs 员工自助

- 本席 B1 GR-05：细化（后台建号允许，生效员工必须有员工自助）。
- Codex CX-11：冲突（第三条免审批激活入口）。
- 候选：冲突；保留 582 分步新建；封闭未经公司批准即激活并发密码；获批后自动 `role_staff_self`。
- 核实：HTML 581-582「新建员工账号」是分步引导表单，无免审批句。HTML 1433 两条注册路径均须公司批准后生效（BSM-085 已映射）。HTML 1428 审批通过自动员工自助（BSM-084 已映射）。`createAccessUser`：默认 active、`createdVia=manual_admin`、发密码、不赋 `role_staff_self`。`StaffService.java:199-212`（`access/staff/`）审批通过才 `createUser` 并 `assignUserRole(..., "role_staff_self")`。
- **裁决：同意候选冲突分类与拟修边界，队列是。** 表单 ≠ 豁免。GR-05 自助义务并入本 ID。不得给未获批账号直接赋自助。

### 025（GR-03）与 021

- 核实：`AccessService.java:911-922` 无密钥 SHA-256；`AccessUser.phoneHash` 经 API 返回。PGM-059 L870「一律加密存放」。021 只登记 BCrypt/UTF-8/JDBC。
- **裁决：025 冲突、队列是；021 技术性、队列否。** 不得把无密钥摘要说成加密。最小方向优先去掉无消费用途的采集/对外返回；确需保存则复用已有 PiiCipher/BlindIndex，不扩成全仓 PII、不做存量回填。

### 026（GR-06）物理删除

- 本席 B1：细化 + GEN-006；拒绝用未映射 BSM-004 作冲突依据。
- 候选：冲突；删除物理 DELETE，复用已有存档/审批/留存。
- 核实：`AccessService.java:466-482` 直接删；`JdbcAccessRepository.java:520-524` `DELETE FROM position_role_templates`。HTML 563-578 要求先存档、审批、软删除、库内可恢复、查占用。BSM-003 已映射；BSM-004 未映射但定义区已冻结，作 GEN-006/017 类型 1 证据。
- **裁决：同意冲突分类与队列是。** 岗位角色模板是配置类主数据。不能只改 `status=inactive` 宣称删除审批完成。不把 BSM-004 写入本模块映射。

### 030（GR-11）假分页

- 本席 B1：技术性。
- 候选：细化；禁止无出口截前 50。
- 核实：`AccessService.java:50-51,1733-1734` `PageResponse<>(items, 1, 50, items.size())` 不切片；`listGrantRequests` 在 `windowDays==null` 时 `days=0`、`cutoff=null`（713-716 行）。
- **裁决：同意细化、队列是。** 列表是 BSM-028 下位实现。元数据不实必须修；若分页必须能取后续页。禁止无出口截前 50。不能把 pageSize 当成业务政策。与 016 频繁阈值分开。

### 019（CX-19；§6 构造器）

- 本席 §6：建议删除 7 参 legacy 构造。
- 候选：技术性、缺陷否、队列否；有真实调用。
- 核实：`AccessUser.java:24-34` 7 参。生产调用：`access/staff/StaffService.java:199-207` `new AccessUser(..., "active", now)` 七参。
- **裁决：同意队列否。** 删除构造器会迫使无本 ID 的调用改写，违反闸 3。本项只登记容器/兼容机制。

### 027（GR-07；操作员保留）

见 §4.1。分类细化 **接受**；队列是 **接受**；拟修范围 **接受**（V0.04 FAIL-1 已写入正文）。

## 6. 三十 ID 逐条结论

图例：差集=原票是否完整进入候选；本席 B2=是否接受候选**当前**分类/队列/范围。

| ID | 来源 | 候选分类 | 工程缺陷 | 队列 | 本席 B2 | 说明 |
|---|---|---|---|---|---|---|
| 001 | CX-01；GR-01 | 冲突 | 是 | 是 | 接受 | 通配先于 DENY（`AuthorizationService.java:39-46`）；`currentSupervisorLevel` 对管理员直接返回 3（245-248）。日常自动权限按 HTML 保留，不得用通配覆盖资金/删除/最终审批。BSM-007 原文作 GEN-005 证据，未写入映射。 |
| 002 | CX-02 | 冲突 | 是 | 是 | 接受 | `requireSameOrganization` 只比 org（`AccessService.java:1615-1622`）。未重跑 CROSS_MERCHANT 探针，源码边界成立，采信 B1 已落盘输出。 |
| 003 | CX-03 | 细化 | 是 | 是 | 接受 | 空组织当全量。 |
| 004 | CX-04 | 细化 | 是 | 是 | 接受 | `slug()` 当全局主键 + upsert。 |
| 005 | CX-05 | 细化 | 是 | 是 | 接受 | 改已应用模板旁路审批。未重跑探针，源码 update 不核授出能力。 |
| 006 | CX-06 | 细化 | 是 | 是 | 接受 | 批 A 执行 B。 |
| 007 | CX-07 | 冲突 | 是 | 是 | 接受 | 受益人自审；PGM-029 作类型 1 证据，未写入映射。 |
| 008 | CX-08 | 细化 | 是 | 是 | 接受 | 不同幂等键并发双生效。未重跑双线程，采信 B1 输出 + 无 CAS 的源码边界。 |
| 009 | CX-09 | 冲突 | 是 | 是 | 接受 | 换岗只追加；PGM-041 作类型 1 证据，未写入映射。 |
| 010 | CX-10；GR-09 | 细化 | 是 | 是 | 接受 | `supervisor` 与 `supervisor_lN` 分裂（`AccessService.java:52-53` vs `AuthorizationService.java:245-257`）。 |
| 011 | CX-11；GR-05 | 冲突 | 是 | 是 | 接受 | 见 §5。 |
| 012 | CX-12 | 细化 | 是 | 是 | 接受 | `effectivePermissionsForUser` 无条件 `currentMerchantScope`。 |
| 013 | CX-13 | 细化 | 是 | 是 | 接受 | 空权限/倒置时间窗仍可返回。 |
| 014 | CX-14 | 细化 | 是 | 是 | 接受 | `reviewGrant` 连续两次 `requirePermission`。 |
| 015 | CX-15；GR-04 | 冲突 | 是 | 是 | 接受 | 见 §5。 |
| 016 | CX-16 | 冲突 | 是 | 是 | 接受 | `@Value` 默认 3、`Math.max`、`riskRank` 写死层级；BSM-001 已映射。 |
| 017 | CX-17 | 细化 | 是 | 是 | 接受 | 单次消费留痕外键。未重跑 TEMP SQL，源码足以支持缺陷存在。 |
| 018 | CX-18 | 细化 | 是 | 是 | 接受 | 管理员改模板把 owner 置 null。 |
| 019 | CX-19；§6 构造器 | 技术性 | 否 | 否 | 接受 | 见 §5。无业务发明。 |
| 020 | CX-20 | 技术性 | 否 | 否 | 接受 | profile 装配；空密码伪成功归 029。无业务发明。 |
| 021 | CX-21 | 技术性 | 否 | 否 | 接受 | BCrypt/UTF-8/JDBC；手机号归 025。无业务发明。 |
| 022 | CX-22 | 技术性 | 否 | 否 | 接受 | HTTP/仓储默认失败；假分页归 030。无业务发明。 |
| 023 | CX-23 | 细化 | 否 | 否 | 接受 | 记录结构有 BSM-028/120 上位；计算错误已单列。 |
| 024 | GR-02 | 细化 | 是 | 是 | 接受 | 见 §5。原冲突主张保留在证据栏，本席改判细化。 |
| 025 | GR-03 | 冲突 | 是 | 是 | 接受 | 见 §5。 |
| 026 | GR-06 | 冲突 | 是 | 是 | 接受 | 见 §5。本席 B1 细化主张保留在证据栏，本席改判冲突。 |
| 027 | GR-07；操作员保留 | 细化 | 是 | 是 | **接受拟修范围** | 见 §4.1。分类细化与队列是维持接受。 |
| 028 | GR-08 | 技术性 | 是 | 是 | 接受 | UserDetails 与未知 principal 均填 staff/active（`AuthorizationService.java:146-149`）。不夸大已证网络攻击。无业务发明。 |
| 029 | GR-10 | 技术性 | 是 | 是 | 接受 | 空密码仍成功返回（`AccessService.java:878-879`；两 provisioner 空白直接 return）。无业务发明。 |
| 030 | GR-11 | 细化 | 是 | 是 | 接受 | 见 §5。本席 B1 技术性主张保留在证据栏，本席改判细化。 |

候选表队列合计 25 是 / 5 否（019—023）/ 待答 0。分类统计 9 冲突 / 15 细化 / 6 技术性 / 0 添加。机检 30 个连续唯一 ID 各一次。修复队列规则：冲突 / 有工程缺陷 / 操作员答删除的添加 → 是；只登记且无工程缺陷 → 否。027 在细化+缺陷+答复保留下仍是，不因保留而出队。

无漏并：23 CX + 11 GR + §6 构造器全部进入 30 ID。无把本席票改写成只审 11 GR 或只审 027。无把 Codex 反例（002—018 等）降级或默删。无改写原 finding 事实。无新添加项、无新用户人闸。

## 7. 技术性「无业务发明」复核（2.1）

| ID | 无业务发明理由 | 本席 |
|---|---|---|
| 019 | List.copyOf / 兼容构造，不引入资格或审批规则 | 同意技术性 |
| 020 | Spring profile 与凭证桥，不新增授权规则 | 同意技术性 |
| 021 | BCrypt/标准库/JDBC 适配；手机号用途不在本项放行 | 同意技术性 |
| 022 | MVC/ApiResponse/默认失败；分页行为不在本项放行 | 同意技术性 |
| 028 | 框架 principal 适配既有主体，不发明新角色名 | 同意技术性；有工程缺陷，队列是 |
| 029 | 未执行已有「初始密码必填」输入契约 | 同意技术性；有工程缺陷，队列是 |

无一项被本席从技术性改判为添加。027 分类细化，不是技术性，也不是添加。

## 8. ponytail-review（只列可删，不修）

`AuthorizationService.java:L146-149: delete: UserDetails/generic principal 伪造 staff/active。生产过滤器已提供 Actor/UserRecord。`（归 028，待修）  
`AccessUser.java:L24-34: 不删。StaffService 七参真实调用。`（归 019，队列否）  
`AccessService.java:L1733-1734: shrink: 假分页。真正切片或去掉 pageSize 谎言，且必须能取后续记录。`（归 030）  
`WalletPage 任意金额消费入口: delete: 脱离订单/账单的金额框扣款。权限桥不删。`（归 027）  
`ReservedPaymentGateway / payment.gateway.enabled: 不删接缝，也不开开关、不改成伪造 SUCCEEDED。`（归 027 拟修约束）  
`net: 假分页/伪造主体/任意金额消费可删或收缩；7 参构造器不可删；预留支付接缝不可冒充已接通。`

## 9. 真实验证命令与缺口

本次 B2 **未**运行会写 `backend/target` 的 Maven/测试。未连生产库、未 HTTP 写、未安装依赖、未 source 整 env、未改 SQL/HTML/配置/台账/问题。未创建 `.scratch/m006-grok-*`（本步源码/HTML 原文足够，无需新可执行反例）。未派子代理。未启动其他模块全审。未读索引/无关历史。

实跑（只读）：

```
git rev-parse HEAD
→ 395e6fbab027020da1c38237fad17e8edcd7a132
git rev-parse --abbrev-ref HEAD
→ main

sha256sum docs/audit-2026-09/plan/criteria-lock.md
→ 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
cat docs/audit-2026-09/plan/criteria-lock.sha256
→ 同值
grep ledger 顶部两行
→ CRITERIA-LOCK-SHA256 同值；LOCK-AMEND-COUNT: 0

python3 计算四份必读输入字节与 SHA256
→ 调用开始与派发声明逐项相同
→ self 117360 / grok 120180 / findings 223966 / questions 11550
→ 独立 B1 前缀 self[:73231] = 1ee77bcc… ；grok[:44940] = 96f0165d…
→ findings 027 当前拟修边界 == questions 当前限定范围（1015 字）
→ questions 操作员答复首个非空行 = 保留
→ M006 映射 52 ID；队列表 9 冲突 / 15 细化 / 6 技术性 / 0 添加；25 是 / 5 否
```

窄读（直接引用，非全仓内容搜索、非其他模块全审）：M006 access 包关键行；`JdbcAccessRepository` 删除 SQL；`access/staff/StaffService` 七参建号；`GlobalExceptionHandler`/`common/api/FieldLabels`；`WalletPage`/`MemberWalletController`/`StoredValueConsumeRequest`/`StoredValueService` topup/settle/consume/notifyStoredValueAfterCommit/confirmOfflineTopupAndSettle；`JdbcCustomerRepository` 1181/1212/1223；`V090` 第 31 行建表；`finance/gateway/{PaymentGatewayPort,ReservedPaymentGateway,DisabledPaymentGateway}`；`V093` 开关默认关；`CustomerOtpVerifier` / `FailClosedCustomerOtpVerifier` / `FixedCustomerOtpVerifier`；`AccessUsersPage.tsx:340-342`；三份 HTML 563-588、832、867、1065、1428-1433、1480-1533、枚举 210、PGM-059 L870；lock M006 映射行。为核实 019 调用，按已知路径读 `StaffService` 七参，未做全仓 `grep`。

**缺口（如实）：**

- 未重跑本席 B1 的 55 单测、AdminDenyHarness、UserDetailsFallbackHarness。
- 未重跑 Codex B1 的 48 源 javac 探针、双线程审批、complex_test TEMP 外键。
- 未跑 AccessControllerTests / AccessJdbcIntegrationTests / 真实 HTTP。
- 客人储值消费在无 merchant 时是否实际成功，未新跑；拟修是取消任意金额消费并绑定订单/账单，不依赖该路径已可用。
- 未在 `.scratch/m006-grok-*` 落临时编译产物。

上述缺口不构成待补审计项：B2 审的是候选归并，不是再做一遍全模块 B1。不以未重跑否定已落盘 B1 输出；分类与范围争议均落到本次源码/HTML 原文。

## 10. 整体结论

**PASS**

- 30 ID 无漏并、无改写原缺陷事实、无把 Codex 反例丢掉。
- 027 分类细化、队列是、0 添加、待答 0：**维持接受**。
- V0.04 唯一 FAIL-1：027「当前拟修边界」现已写入 HTML1480/枚举210 预留接口、能力开关默认关、不伪造结果、不得开启未配置支付/短信、不得称 ReservedPaymentGateway 已接真实厂商、不得让客户端/测试入口冒充渠道成功、POS 走既有柜台链、HTML585 短信对 App 订单不豁免、复用 `customer_stored_value_transactions`。**本票接受该拟修范围。**
- 其余 29 项分类/队列/范围接受（含 024/015/011/025/021/026/030/019 原分歧）。
- 未映射冻结 HTML 作类型 1 证据合规，未扩锁、未改 52 映射、不重开 M001、无待批 HTML。
- 表名 `customer_stored_value_transactions` 以源码为准，不据 Codex 旧误写造表。
- 不存在有条件通过。本票只定 B2 候选复核；B3 未执行。


# M006 B2 整批候选复核（Grok 席）
- 版本：V0.06
- 角色：审计席乙（独立；第一性原理 / 对抗式 + 顺从式）
- 模块：M006 `10-backend-main-access-01`（6/322）
- 步骤：B2（由 B5-r2 双 FAIL 回退的整批归并复核）；不执行 B3/B4/B5，不重开其他模块
- 控制：V0.17；ledger `IN_PROGRESS`；B3=2；planrepair=2；runtimefailure=0
- 本席只读追加本文件；不修代码、不改方案、不改 HTML/锁/台账/问题、不创建其他产物或测试文件、不共享 Maven 编译、不派第三席
- 全批结论：**PASS**

## 0. 技能预检（本节点实核）

按意图执行：B2 只用结构，不修代码、不派子代理、不等操作员。grilling 的「等用户回答 / 派子代理查事实」作废，自问自答并自己读源码。receiving-code-review 的 IMPLEMENT 作废。verification-before-completion：PASS/FAIL 前先核指纹与源码。ponytail-review 只列可删不修。systematic-debugging 只借读错误→复现→追数据流，不加诊断桩。

| 技能 | 本节点 | 缺口 |
|---|---|---|
| using-superpowers | 开工前自检 | 无 |
| grilling | 只用追问结构；033 自问自答 | 不等操作员、不派子代理 |
| verification-before-completion | PASS 前实算指纹/git tree/源码/HTML | 无 |
| receiving-code-review | 先核实再接受或驳回候选；禁止表演性同意 | IMPLEMENT 作废 |
| ponytail-review | §8 | 不修 |
| diagnosing-bugs / systematic-debugging | 只借读源码→数据流 | 不插桩、不改码 |
| codebase-design | resolve() 为幂等隔离接缝；AccessGrantPolicy 为 016 政策类型 | 无 |
| tdd / test-driven-development | B2 不落测试 | 不适用 |
| resolving-merge-conflicts | 无合并 | 不适用 |
| writing-for-agents | 本报告给调度席读 | 无 |
| batch-grill-me / ubiquitous-language | 本席技能树不存在 | 记缺口，未冒充已调用 |
| git-guardrails-claude-code | 未装 hook | 记缺口 |

未核到的不写已核实。未派子代理。未启动其他模块全审。

## 1. 输入指纹（本席实算，与派发声明对照）

| 路径 | 字节 | SHA256 | 对照 |
|---|---:|---|---|
| `docs/audit-2026-09/modules/M006-b5-r2-self-raw.md` 全文（Codex B5 V0.01） | 36285 | `7c947c128b3ec0a7a6b891a795b613bf900db78aed9002e3e1e8e01f844a7395` | 与派发一致 |
| `docs/audit-2026-09/modules/M006-b5-r2-grok-raw.md` 全文 | 45265 | `0f7c1000403b9f254988c63041f3f4dc3a1cd12ada32741a01c5ba396b60e6c9` | 与派发一致 |
| 同上前 26892 字节（旧报告，**本票不采信**） | 26892 | `52c17c3c1aac346d0ea12998b283f7a5b7956e90420d17f17bbafd397451f2a7` | 前缀保留；不当成功票 |
| 同上自第 26893 字节起（本席 B5 V0.02 本次完整报告） | 18373 | `1c8b6d43dad24c118bc3bf013487cd9488d1fd80d7c029b6150cef7bd037daae` | 与候选声明一致；本票只采信此段 |
| `docs/audit-2026-09/findings.md` V0.61 | 240776 | `026a5b60efddf981412de9250e613bf64ad5e6c765f2f461691c3bb3bba96d5b` | 与派发一致 |
| 本文件追加前 | 147529 | `4595d291ca5b293690c8ca2e9642f3b95948901242cb744fa4af7ad89835f5f2` | 与派发「旧 147529 字节保持」一致 |

本席独立实算 `docs/audit-2026-09/plan/criteria-lock.md` 全文哈希 = `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（sidecar 与 ledger 顶部同值；`LOCK-AMEND-COUNT: 0`）。**本 B2 报告不新增该机读哈希独占行。** `git rev-parse HEAD` = `395e6fbab027020da1c38237fad17e8edcd7a132`，分支 `main`。

本次输入 B5 raw 路径（固定）：
- `docs/audit-2026-09/modules/M006-b5-r2-self-raw.md`
- `docs/audit-2026-09/modules/M006-b5-r2-grok-raw.md`（只采信 26892 字节之后的完整 V0.02）

候选版本：findings.md **V0.61** / SHA256 `026a5b60efddf981412de9250e613bf64ad5e6c765f2f461691c3bb3bba96d5b`。

## 2. 差集方法

B2 无互盲。全集：

- Codex B5-r2 本次完整报告（self-raw 全文 V0.01）临时编号 **CX-B5-01**
- 本席 B5-r2 本次完整报告（grok-raw 自 26892 字节起 V0.02）临时编号 **GR-01、GR-02**
- 当前候选 `findings.md` V0.61：33 个连续 ID；001—030 队列否；031/032 是；033 否（待本席裁定）
- 只审本批 3 条新意见的归并、分类、工程缺陷、队列与拟修边界；旧 30 项保留原票，不拼接历史票、不重开已出队项

映射（与候选表一致，本席逐项对照，不另编号）：CX-B5-01→031；GR-01→032；GR-02→033。机检 33 个连续唯一 ID 各一次。无第四条 B5 意见被漏并。

## 3. 重点独立复核 M006-033（本席原 GR-02）

### 3.1 原票（不改写）

本席 B5 V0.02 GR-02 原文主张保留：

- 文件：`AccessService.java:777,790,807,1549-1573`；`AccessController.java:119-128`
- 分类当时写 **技术性**；工程缺陷 **是**
- 事实：`git ls-files` 无 `AccessGrantPolicy.java`；`git status` 为 `??`；`plan/scope.txt` / `modules.md` / `module-union.txt` 均无该路径；对 14 个模块文件、classpath 去掉 `AccessGrantPolicy*` 后再编 `AccessService` → javac 退出 1，`cannot find symbol`

候选未删、未改写上述原票，只另给分类/缺陷/队列提案，并写明「原始 FAIL 不被本方覆盖」。合差集规则。

### 3.2 本席本轮独立核到的事实（类型 4 + 类型 1）

| 检查 | 本席命令/阅读结果 |
|---|---|
| 工作区文件 | 存在；2184 字节；SHA256 `6f0de0f7052efa45430347547746379da6ec376235db46fe9e19f5017e087fb7` |
| `git ls-files --error-unmatch` | pathspec 不匹配（未跟踪） |
| `git status --porcelain` | `?? backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java` |
| B3-r2 after tree `2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9` | `git cat-file -t` = tree；`git ls-tree` 有该路径 `100644 blob`；`git show tree:path` 字节与工作区 **逐字相同** |
| 冻结清单 | `scope.txt` / `modules.md` / `module-union.txt` **均无**该路径 |
| 源码自证 | 文件头注释「M006-016：沿用主管复核策略表保存授权政策」；4 个 `public record`（外层 + BoundedInt + ReviewRule + Update） |
| 当前必要源计数 | `access/*.java` + `access/dto/*.java` + IdempotencyService + SupervisorReviewService + AuditEventService = **45**，其中含 `AccessGrantPolicy.java` |
| 公开入口 | `AccessController.java:119-128` `GET/POST /grant-policy`；`AccessService.grantPolicy/updateGrantPolicy` 1549-1573 |
| BSM-001 原文 | `criteria-lock.md` 定义区 / 业务地图：「业务策略一律可配置」；「预警阈值、各种有效期」须后台配置；「还必须写明取值的上限与下限」 |
| 候选 javac | 复制 target、删除 4 个 Policy 缓存 class、javac 当前 45 源，退出 0，生成 4 个 Policy class。本席 **未** 重跑该 javac（禁止共享 Maven 编译、禁止创建其他产物）；独立核对其前提（45 源含 Policy、after tree 字节相同、4 个 nested record）后采信已落盘输出 |

V0.17 当前工作区与 B3 untracked 规则（本席核对，不增减冻结 scope）：

- B3 **从不提交**；B4 `git add -A && git write-tree` 必须覆盖 tracked / staged / **untracked**
- 范围在阶段 A 冻结后，阶段 B **不得扩大或缩小** `scope.txt` / 模块映射
- 铁律 5 未跟踪人闸 **只在阶段 A 那一次实跑时判**；阶段 B 因修复产生的未跟踪文件不触发该闸

### 3.3 grilling 自问（不等操作员）

1. 「未进 git ls-files」是不是当前必须进修复队列的工程缺陷？  
   不是。B3 合法新源在模块发布前提交前保持 `??`，是流程规定，不是漏交付。B4 after tree 已包含该文件且与现字节相同。
2. 「未进 scope / modules.md」能不能靠本批 B3 修掉？  
   不能。阶段 B 扩冻结清单违法。把 033 标「队列是」会逼出非法扩表，或把 016 的政策类型内联回 `AccessService`（撤回已定稿修复）。
3. 本席 B5 的 14 文件去 class 编译失败，是否仍证明「当前工作区不能构建」？  
   否。那次检查对象是「去掉依赖且不提供现有源码、只编 14 文件」。当前工作区 **已有** 该源。候选 45 源在移除旧 Policy 缓存 class 后 javac 退出 0，与「依赖未声明本机 class 缓存」（GEN-013）不是同一事实。
4. 2.1 通道？  
   本席 B5 已写「授权政策上下限是 BSM-001 已有可配+上下限的实现细节」，却标成技术性，与「能指到 HTML 上位则先归细化、不进技术性通道」冲突。本票改判细化。原技术性主张留在证据栏。

### 3.4 本席裁定

- **分类：细化。** 上位 BSM-001（已映射）「业务策略一律可配置 + 阈值/有效期 + 必须有上下限」。`AccessGrantPolicy.BoundedInt` / `ReviewRule` 是 016 已定稿修复的下位实现，不是新业务发明，也不是纯构建配置。
- **工程缺陷：否（本次可成立的待修缺陷不成立）。** 未跟踪是 B3 不提交的过程态；当前必要全源可编过，不依赖旧 Policy class 缓存；不能把「只编 14 文件且故意不提供已存在的 B3 新源」当作当前工作区构建失败。
- **修复队列：否。** 只登记。模块发布时随全批修复一起跟踪提交。不修业务、不扩锁、不扩 scope/模块映射、不把 016 政策类型拆回旧文件。
- 本席 B5 FAIL 作为「当时 14 文件去 class 编不过」的证据仍保留在原 raw；B2 裁定的是 **当前队列要不要修代码**。不以自审覆盖方式抹掉 GR-02，而是接受候选对「当前必要源」检查对象的更正。

## 4. M006-031（CX-B5-01）

候选：冲突；工程缺陷是；队列是。拟修：收窄无 merchant 时共享 `p` 的幂等范围，沿用既有组织/商户字段，同主体重试仍只生效一次，核对旧记录重试。

本席核实（源码，未重跑其隔离测试）：

- `AuthorizationService.resolve()` 199-218：有 merchant 则 `m:{id}`，否则一律返回 `"p"`。`AccessActor` 已有 `organizationId`，resolve 没用它。
- `IdempotencyService.scopedOperationId` 114-119：把 resolve 结果接到 `operationId` 前；查找/去重键是 `(scopedOperationId, idempotencyKey)`（Jdbc 23/37 `ON CONFLICT (operation_id, idempotency_key)`）。
- `AccessService.createRole` 300-330：真实 `idempotencyService.execute("createRole", request.idempotencyKey(), …)`；`AccessController` 把该调用暴露为创建角色入口。
- self raw 输出：`scope_a=p scope_b=p roles_written=1`；org-b 的 controller 返回 success 与 org-a 的 `role_91975467-…`；断言 org-b 自己的角色失败。`RoleRecord.organizationId` 有 `@JsonIgnore`，响应里组织字段为空，不能据此称没有串用。

行为与业务影响：两个不同项目公司的员工都有建角色权限，各自第一次用同一请求编号创建本组织角色时，后到的一方不会在本组织写下角色，却收到先到一方已经建好的角色。后台显示成功，实际权限对象属于别人的组织。这不是「同一人重复点提交」，是把别人组织的对象当成自己的创建结果。

分类 **冲突** 接受。HTML 红线「看不到别的商户和别的项目的数据」（治理地图 528 / 业务地图 707）与「不同人只看自己该看的、只做自己该做的」（业务地图 1827）；PGM-001「同一个操作被重复提交，系统只认一次」不能把两个主体的首次创建收成一次。拟修不发明新身份、不改 HTML，边界可接受。队列 **是**。

## 5. M006-032（本席原 GR-01）

候选保留原票：冲突；工程缺陷是；队列是。拟修：只改本模块账号类型错误分支，去掉给用户看的英文枚举和输入原值，改用中文提示；不重写全局 `FieldLabels`。

本席核实：

- `AccessService.java:48-49` `ALLOWED_ACCOUNT_TYPES` 为 `staff/merchant/…` 英文字面量。
- `921-923` 用户可见消息：`账号类型「" + accountType + "」不允许，可选值：" + ALLOWED_ACCOUNT_TYPES`。
- `FieldLabels.java:529`：已含汉字则原样透传。`GlobalExceptionHandler.java:49-61` 把该字符串写入 HTTP 响应。
- 候选真实 controller → `createAccessUser(accountType=platform_admin)` → `INVALID_ACCOUNT_TYPE` → 真实 handler：`账号类型「platform_admin」不允许，可选值：[staff, merchant, merchant_admin, outsourced_vendor, supervisor, sales]`。与本席 B5 `FieldLabels.translate` 探针同一泄漏。

行为与业务影响：操作员在后台手工建号时填了不允许的账号类型，页面报错里直接出现 `platform_admin`、`staff` 等英文。治理地图 287：「提示、报错…必须全部为中文，不得出现任何英文」；可以不中文的只有单据号、设备编号、二维码、系统内部标识符。给操作员看的「可选值」清单不是这四样。

分类/缺陷/队列/拟修 **全部接受**。原票未改写。不把内部权限码或 DTO 字段名扩成全文件英文字符清扫。

## 6. 旧 001—030

队列表 001—030「修复队列归属」全部为 **否**，与 V0.60 出队后状态一致。本票不重审、不拼接、不改其分类/拟修/历史证据。本批新 ID 只有 031/032/033。

## 7. 四分类与添加项

本批 3 条：冲突 2（031/032）、细化 1（033）、技术性 0、添加 0。无 2.2 待问。033 不进技术性通道（见 §3.4）。无一项被本席从候选改判为添加。

技术性「无业务发明」本批无新条。019—023 / 028 / 029 保持历史登记，不在本票重开。

## 8. ponytail-review（只列可删，不修）

- `AuthorizationService.java:L215: shrink: 无 merchant 时不要所有公司组织共用 "p"。用已有 organizationId 收窄；不要改幂等表结构。`（归 031）
- `AccessService.java:L921-923: shrink: 账号类型报错不要拼接英文字面量和输入原值。中文说明可选类别；不要重写 FieldLabels。`（归 032）
- `AccessGrantPolicy.java: 不删、不内联回 AccessService。016 已定稿政策类型。不扩 scope.txt。`（归 033，队列否）
- net: 031/032 收缩展示与隔离键；033 零代码。

## 9. 真实验证命令与缺口

本次 B2 **未**运行会写 `backend/target` 的 Maven/测试。未创建 `.scratch/` 或 `/tmp` 编译产物。未连生产库、未 HTTP 写、未改 SQL/HTML/配置/台账。未派子代理。未启动其他模块。

实跑（只读）：

```
git rev-parse HEAD
→ 395e6fbab027020da1c38237fad17e8edcd7a132
git rev-parse --abbrev-ref HEAD
→ main
git ls-files --error-unmatch .../AccessGrantPolicy.java
→ pathspec 不匹配
git status --porcelain -- .../AccessGrantPolicy.java
→ ?? backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java
git cat-file -t 2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9
→ tree
git show 2e346ad1…:backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java
→ 与工作区 2184 字节逐字相同（SHA256 6f0de0f7052efa45430347547746379da6ec376235db46fe9e19f5017e087fb7）
python3 计 45 源、4 个 nested record、队列表 33 行
→ COUNT 45；HAS_POLICY True；001-030 全否；031/032 是；033 细化/否/否
sha256sum findings / 两份 B5 raw / 本文件追加前
→ 与 §1 表逐项相同
```

窄读：`AuthorizationService.resolve` 199-218；`AccessService` 48-49、300-330、921-923、1549-1573；`AccessController` 119-128；`IdempotencyService` 72、114-119；`JdbcIdempotencyRepository` 23/37；`FieldLabels` 529；`GlobalExceptionHandler` 49-61；`AccessActor`；`RoleRecord.organizationId` `@JsonIgnore`；`AccessGrantPolicy.java` 全文；BSM-001 锁内原文；治理地图 287/528；业务地图 707/1827。

**缺口（如实）：**

- 未重跑 Codex 跨组织 controller 探针与非法账号类型 HTTP 探针（禁止创建测试文件 / 禁止共享 Maven 编译）。源码边界与已落盘输出足以支持 031/032。
- 未重跑候选「45 源去 Policy 缓存 javac」。前提已独立核对，采信候选 exit 0 输出。
- 未重跑本席 B5 的 14 文件去 class javac；该实验对象与当前工作区全源不同，本票不再用它证明「当前不能构建」。

上述缺口不构成待补审计项。

## 10. 三十三 ID 逐条结论

图例：差集=原票是否完整进入候选；本席 B2=是否接受候选**当前**分类/队列/范围。

| ID | 来源 | 候选分类 | 工程缺陷 | 队列 | 本席 B2 | 说明 |
|---|---|---|---|---|---|---|
| 001—030 | 历史定稿 | 不变 | 不变 | **否** | 维持 | 不拼接、不重审、不改拟修。 |
| 031 | B5-r2 CX-B5-01 | 冲突 | 是 | 是 | **接受** | 无 merchant 的不同组织共享 `p`，创建角色回放另一组织结果。真实 controller/幂等/resolver。 |
| 032 | B5-r2 GR-01 | 冲突 | 是 | 是 | **接受** | 非法账号类型报错把英文枚举和输入原值送给用户。真实 HTTP 响应已复现。原票未改写。 |
| 033 | B5-r2 GR-02 | 细化 | 否 | 否 | **接受** | 见 §3。原技术性/缺陷是主张保留在证据栏；本席改判细化、当前无待修缺陷、队列否。 |

候选表队列合计 **2 是**（031/032）/ **31 否**（001—030 与 033）/ 待答 0。分类统计相对旧 30 项：新增冲突 2、细化 1。机检 33 个连续唯一 ID 各一次。

无漏并：CX-B5-01 + GR-01 + GR-02 全部进入 031/032/033。无改写本席 GR-01 事实。无把 GR-02 从台账抹掉。无新添加项、无新用户人闸、无 HTML 申请、无 LOCK-AMEND。冻结 scope 不增不减。

## 11. 整体结论

**PASS**

- 输入：`modules/M006-b5-r2-self-raw.md`（36285 / `7c947c12…`）；`modules/M006-b5-r2-grok-raw.md` 本次完整报告为 26892 字节之后的 V0.02（18373 / `1c8b6d43…`）。候选 findings.md **V0.61** / `026a5b60efddf981412de9250e613bf64ad5e6c765f2f461691c3bb3bba96d5b`。
- 全批 3 条意见归并正确：031 冲突/是/是，032 冲突/是/是，033 细化/否/否。旧 30 项队列否不变。
- 033 独立裁定：接受候选。BSM-001 上位故细化；B3 合法新源在 after tree、当前 45 源去缓存可编过；不扩冻结 scope；只登记、发布时跟踪提交。
- 0 添加、待答 0。不存在有条件通过。本票只定本批 B2 候选复核；不进入 B3。

# M006 B2 整批候选复核（Grok 席）
- 版本：V0.07
- 角色：审计席乙（独立；第一性原理 / 对抗式 + 顺从式）
- 模块：M006 `10-backend-main-access-01`（6/322）
- 步骤：B2（由 B5-r3 完整双 FAIL 回退后的整批归并复核）；不执行 B3/B4/B5，不重开其他模块，不修复源代码
- 控制：V0.17；ledger `IN_PROGRESS`；B3 累计=3；planrepair=2；runtimefailure=0
- 操作员一次性例外（原话）：「仅限本次，你把经过3次修复后仍未通过的问题交给claude cli,调用fable 5.1 模型 high 模式看看能否修复」。本席只把该句理解为：解除本次 M006 `BLOCKED-3ROUNDS`、允许 Codex 在本批 B2 双 PASS 后把修复执行交给 Claude CLI Fable 5.1 / high。不改控制文件，不授权本席修复，不把后续模块默认角色/上限扩成 Claude。
- 本席只读追加本文件；不修代码、不改方案、不改 HTML/锁/台账/问题、不创建其他产物或测试文件、不共享 Maven 编译、不派第三席
- 不以 Codex 已落盘的 B2 V0.16 为据；本票只由 B5-r3 两席本次完整报告 + 候选必要全文 + 四处源码/HTML 原文独立给出
- 全批结论：**PASS**

本次 B2 按派发不写 `CRITERIA-LOCK-SHA256:` 独占行。正文叙述本席实算锁哈希（见 §1），不得被闸 1 误取为本步④。

## 0. 技能预检（本节点实核）

按意图执行：B2 只用结构，不修代码、不派子代理、不等操作员。grilling 的「等用户回答 / 派子代理查事实」作废，自问自答并自己读源码。receiving-code-review 的 IMPLEMENT 作废。verification-before-completion：PASS 前实算指纹/源码/HTML。ponytail-review 只列可删不修。systematic-debugging 只借读错误→复现→追数据流，不加诊断桩。

| 技能 | 本节点 | 缺口 |
|---|---|---|
| using-superpowers | 开工前自检 | 无 |
| grilling | 只用追问结构；034 四处自问自答 | 不等操作员、不派子代理 |
| verification-before-completion | PASS 前实算指纹、队列表、四处抛点、HTML 287 | 无 |
| receiving-code-review | 先核实再接受或驳回候选；禁止表演性同意 | IMPLEMENT 作废 |
| ponytail-review | §8 | 不修 |
| diagnosing-bugs / systematic-debugging | 只借读源码→FieldLabels 透传 | 不插桩、不改码 |
| codebase-design | 缺陷在人读 message 接缝，不在鉴权/币种判定 | 无 |
| tdd / test-driven-development | B2 不落测试 | 不适用 |
| resolving-merge-conflicts | 无合并 | 不适用 |
| writing-for-agents | 本报告给调度席读 | 无 |
| batch-grill-me / ubiquitous-language / i-have-adhd | 本席技能树不存在独立 SKILL.md | 记缺口，未冒充已调用 |
| git-guardrails-claude-code | 未装 hook | 记缺口 |

未核到的不写已核实。未派子代理。未启动其他模块全审。未扩大为 B5 重审。

## 1. 输入指纹（本席实算，与派发声明对照）

| 路径 | 字节 | SHA256 | 对照 |
|---|---:|---|---|
| `docs/audit-2026-09/modules/M006-b5-r3-self-raw.md` 全文 | 40760 | `2640e139a0c4ce55f1c67a0a37cd574b855294fc363e14c139249b4469c9c8d1` | 含 V0.01 独立票 + V0.02 接收追加 |
| 同上前 34586 字节（Codex B5-r3 独立票 V0.01） | 34586 | `72da471a14607a68d3c363a0a65c7ec426dfe7b2649da01da7f9b8f767f681a3` | 与 self 自述一致；本票差集用该独立票的 CX-B5-01，V0.02 只作接收记录 |
| `docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md` 全文（本席 B5 V0.01） | 21532 | `06506fb252baac014c9ed4d1f89c546b92479e54aa84289255d6b78c3498b0bb` | 本次完整报告，无后续追加审计票 |
| `docs/audit-2026-09/findings.md` V0.65 | 250382 | `0974cdae8f4cfc954a302f68773d08853e35fb63a3dc78b5bc55d866889fec7a` | 与派发一致 |
| 本文件追加前 | 165814 | `4e0d7aa205a52b2f1b84cf040b4080033e5a621fb4330b88e09b51f8e44025a4` | 与派发「原 165814 字节前缀保留」一致 |

本席独立实算 `docs/audit-2026-09/plan/criteria-lock.md` 全文哈希 = `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（sidecar `plan/criteria-lock.sha256` 与 `ledger.md` 顶部同值；`LOCK-AMEND-COUNT: 0`）。**本 B2 报告不新增该机读哈希独占行。** `git rev-parse HEAD` = `395e6fbab027020da1c38237fad17e8edcd7a132`，分支 `main`。

本次输入 B5 raw 路径（固定）：
- `docs/audit-2026-09/modules/M006-b5-r3-self-raw.md`
- `docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md`

候选版本：findings.md **V0.65** / SHA256 `0974cdae8f4cfc954a302f68773d08853e35fb63a3dc78b5bc55d866889fec7a`。

`questions/M006-027.md` 的 `## 操作员答复` 段首个非空行精确为 `保留`。本批无新添加项、无新 2.2 文件。

## 2. 差集方法

B2 无互盲。全集：

- Codex B5-r3 本次完整独立票（self-raw 前 34586 字节 / V0.01）临时编号 **CX-B5-01**（1 条）
- 本席 B5-r3 本次完整报告（grok-raw 全文 V0.01）临时编号 **GR-01**（1 条）
- 当前候选 `findings.md` V0.65：34 个连续唯一 ID；001—033 队列否；**仅 034 队列是**
- 只审本批 2 条新意见的归并、分类、工程缺陷、队列与拟修边界；旧 33 项保留原票，不拼接历史票、不重开已出队项、不把台账写成「34 项新缺陷」

映射（与候选表一致，本席逐项对照，不另编号）：**CX-B5-01 ∪ GR-01 → M006-034**。机检 34 个连续唯一 ID 各一次，无重复。无第三条 B5-r3 意见被漏并。

## 3. 原始 2 意见到 034 的完整映射

原票不覆写。下列是 B2 对候选归并的本席核实。

| 原始意见 | 原分类 / 工程缺陷 | 原位置 | 冻结判据 / 依据类型 | 映射 | 并集处置 |
|---|---|---|---|---|---|
| Codex CX-B5-01 | 冲突 / 是 | `AccessService.java:1689—1694`（根因抛点）；同文件 703—724（`reviewGrant` 执行路径）；`AccessController.java:147—155`（HTTP 入口）；`GlobalExceptionHandler.java:49—61` 与 `FieldLabels.java:504—529`（出口透传） | PGM-001、PGM-066、GEN-015；类型 4 | **M006-034** | 1694 为人读报错源。入口与 `reviewGrant` 不是另三处缺陷，只说明 1694 如何到达 HTTP。候选来源位置未再抄 147—155/703—724，不构成漏并。 |
| 本席 GR-01 | 冲突 / 是 | `AccessService.java:1694`（主证）；同文件 `:515`、`:1061`、`:1491`（同类出口） | PGM-001、PGM-066、GEN-002、GEN-015；类型 1+4 | **M006-034** | 四处全部进入 034。候选未把本席意见缩成仅 1694。 |

并集四抛点（当前源码逐字，文件头仍为 V0.13；测试文件头仍为 V0.20；**源代码修复尚未开始**）：

| 行 | 当前人读 message | 本席 B5 探针（已落盘，本票未重跑） |
|---:|---|---|
| 515 | `删除配置须由 USB 最高权限审批` | `HTTP_CODE=MASTER_AUTHORITY_REQUIRED … LATIN=true` |
| 1061 | `薪资币种当前仅支持人民币（CNY），实际为：` + `currency` | 与 1491 同文案；探针 `薪资币种当前仅支持人民币（CNY），实际为：USD` / `LATIN=true` |
| 1491 | 与 1061 同一字符串模板 | 同上 |
| 1694 | `审批结论必须为通过（approve）或驳回（reject）` | `HTTP_CODE=VALIDATION_FAILED MSG=审批结论必须为通过（approve）或驳回（reject） LATIN=true`；Codex 真实 Controller→Handler 同文案，断言 `[A-Za-z]` 失败 |

**不是 032 未修好、也不是重开 032。** 本席 B5 已实测 `INVALID_ACCOUNT_TYPE` → `账号类型不允许，请重新选择` / `LATIN=false`。032 定稿边界只针对账号类型分支，B4-r3 已出队。034 是全模块复审发现的同类残留，必须新 ID。

**1686 `request_type is invalid` 不进 034。** 本席 B5 探针：`FieldLabels` 将其译为「申请类型格式不正确」，`HAS_LATIN=false`。两席本次意见均未把它列为缺陷；B2 不另发明。

无漏并：2 条原意见全部进入 034。无改写本席 GR-01 四处范围。无把 CX-B5-01 的真实 HTTP 复现从证据链抹掉。无把旧 001—033 改回队列是。

## 4. 四处精确修复边界

候选拟修与本席核实后的 **当前可接受边界**（B3/Claude 归因只能落在这些行；越出即闸 3 越界）：

1. **`AccessService.java:515`**
   只改 `ApiException.forbidden("MASTER_AUTHORITY_REQUIRED", …)` 的**人读第二参数**。保留错误码、`isMasterAuthority` 判定、`approve_delete`/`reject_delete` 动作字面量、主管复核调用。参考最小文字：`删除配置须由持授权密钥的最高权限人员审批`。治理地图正文用语是「持授权 U 盘密钥的最高权限」；展示句不得再出现拉丁 `USB`。不得把最高权限改成职级 3 或日常管理员。

2. **`AccessService.java:1061`**（`assignStaffLevel` 币种拒绝）
   只改 `UNSUPPORTED_CURRENCY` 的人读第二参数。保留 `trimToDefault(..., "CNY")`、`if (!"CNY".equals(currency))`、金额分存储、错误码。参考最小文字：`薪资币种当前仅支持人民币`。不得把允许币种扩成 USD，不得在 message 里回显 `CNY`/`USD`/输入原值。

3. **`AccessService.java:1491`**（岗位授予上下文币种拒绝）
   与 1061 同一显示缺陷的第二抛点，必须**同样只改人读第二参数**，不得只修一处。内部允许值与错误码同上。

4. **`AccessService.java:1694`**（`normalizeDecision`）
   只改非法 `decision` 的人读第二参数。保留 `List.of("approve", "reject")` 内部取值、`VALIDATION_FAILED`、`reviewGrant` 权限与自审拒绝。参考最小文字：`审批结论必须为通过或驳回`。请求体字段名 `approve`/`reject` 可作为内部标识保留，**展示给人的 message 不能再列出英文取值**。

**版本：** `AccessService.java` 文件头 V0.13→V0.14；既有 `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` V0.20→V0.21。测试须覆盖上述 4 个错误出口的真实 Handler 响应无拉丁字母，并断言允许的审批/最高权限/人民币路径仍保持。夹具复用现有测试，不另加测试框架。

**明确不修：** 内部枚举与动作字面量；币种允许值 `CNY`；权限码与 `isMasterAuthority`；三份业务 HTML；`FieldLabels` / `GlobalExceptionHandler` 通用翻译器；账号/支付/存储逻辑；032 已修分支；001—033 已出队项。

候选「来源位置」把 Handler:49—61 与 FieldLabels:504—529 写成透传路径，本席接受为**解释**而非修复文件。`FieldLabels.translate` 对已含 CJK 的句子原样返回（源码 528—529），故夹英文的中文句子必须改抛点，不能靠翻译器。

## 5. grilling 自问（不等操作员）

1. 能否把 034 缩成只修 1694？
   不能。那是 CX-B5-01 的子集，会改写本席 GR-01。候选已取并集。
2. 能否把 USB/CNY 辩成「系统内部标识符」留下？
   不能。治理地图 287：「这四样之外……不得以“这是标识”为由保留英文。」USB/CNY/approve/reject 出现在 HTTP `message`，不是单据号/设备编号/二维码/内部标识符。
3. 参考句「持授权密钥的最高权限人员」是否发明新权限？
   否。判定仍是现有 `isMasterAuthority`。HTML 已有「持授权 U 盘密钥的最高权限」。这是显示映射，不是新角色。
4. 034 是添加还是冲突？
   冲突。判定顺序冲突→细化→技术性→添加。能指到 PGM-001/066 明文「提示、报错、枚举取值必须全部为中文」。
5. 旧 33 项会不会被这 1 个新 ID 说成 34 项新缺陷？
   不会。001—033 队列仍否；仅 034 为新待修。台账行数 34 ≠ 34 项新缺陷。

## 6. 四分类

本批新意见只有 034：

| 分类 | 本批新条 | 处置 |
|---|---|---|
| **冲突** | M006-034 | 进入修复队列（是） |
| 细化 | 0 | — |
| 技术性 | 0 | — |
| 添加 | 0 | 不走 2.2 |

旧 001—033 分类保持历史值（冲突 11 + 本批 034 使表内冲突格为 12；细化 16；技术性 6）。本票不重开、不改其分类。无一项被本席从候选改判为添加。技术性「无业务发明」本批无新条。

HTML 原文（`docs/platform-governance-map-for-operations-v0.1.html` 第 287 行 / PGM-001；PGM-066 已拍板同文）：

> 展示给用户的一切文字，包括菜单、页面标题、字段名、状态、枚举取值、按钮、提示、报错……必须全部为中文，不得出现任何英文……可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符。这四样之外，凡是展示给人看的文字一律中文，不得以“这是标识”为由保留英文。

冻结引用：PGM-001、PGM-066、GEN-002、GEN-015（并集；CX 未写 GEN-002，本席原票有，候选已列入）。

## 7. 工程缺陷与修复队列

| ID | 工程缺陷 | 修复队列归属 | 本席 B2 |
|---|---|---|---|
| M006-001—033 | 历史值不变 | **否** | **维持** |
| M006-034 | **是** | **是** | **接受** |

冲突且人读报错已被两席命令/HTTP 输出钉住，故工程缺陷是、队列是。有 ID 且入队后，本批双 PASS 定稿才可交给本次授权的 Claude 修；本席不修。

队列表机检：34 行、34 个唯一连续 ID、队列「是」=1（仅 034）、「否」=33、待答 0。

## 8. ponytail-review（只列可删，不修）

- `AccessService.java:515`: shrink 人读句里的 `USB`；不要改 `isMasterAuthority`。
- `AccessService.java:1061` 与 `:1491`: shrink 人读句里的 `CNY`/输入币种；不要放宽允许值。
- `AccessService.java:1694`: shrink 人读句里的 `approve`/`reject`；不要改内部允许列表。
- `FieldLabels.java` / `GlobalExceptionHandler.java`: 不改。032 已证明改抛点即可。
- net: 4 处字符串 + 1 个既有测试类最小回归；零新框架、零 HTML。

## 9. 真实验证命令与缺口

本次 B2 **未**运行会写 `backend/target` 的 Maven/测试。未创建 `.scratch/` 或 `/tmp` 编译产物。未连生产库、未 HTTP 写、未改 SQL/HTML/配置/台账。未派子代理。未启动其他模块。未读 Codex B2 V0.16 正文作投票依据。

实跑（只读）：

```
git rev-parse HEAD
→ 395e6fbab027020da1c38237fad17e8edcd7a132
git rev-parse --abbrev-ref HEAD
→ main
sha256sum findings.md
→ 0974cdae8f4cfc954a302f68773d08853e35fb63a3dc78b5bc55d866889fec7a  （250382 字节，V0.65）
sha256sum plan/criteria-lock.md
→ 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
python3 读 AccessService 515/1061/1491/1694 与文件头 V0.13；测试文件头 V0.20
→ 四处英文夹排仍在；修复未开始
python3 解析队列表
→ 34 唯一 ID；001—033 队列否；034 冲突/是/是
```

窄读：`AccessService` 515、1058—1062、1488—1492、1681—1695、703—724；`AccessController` 147—155；`FieldLabels.translate` 504—529；`GlobalExceptionHandler` 49—61；治理地图 287 / 「持授权 U 盘密钥的最高权限」；`questions/M006-027.md` 答复首行。

**缺口（如实）：**

- 未重跑本席 B5 TranslateProbe / 99 项测试，也未重跑 Codex 的 `b5InvalidDecisionResponseIsChinese`。源码四处与已落盘 B5 输出足以支持 034。
- 未把 `request_type is invalid` 或其它未在两席本次意见中的英文键扩进本批。

上述缺口不构成待补审计项。B2 审的是候选归并，不是再做一遍全模块 B5。

## 10. 三十四 ID 逐条结论

图例：差集=原票是否完整进入候选；本席 B2=是否接受候选**当前**分类/队列/范围。

| ID | 来源 | 候选分类 | 工程缺陷 | 队列 | 本席 B2 | 说明 |
|---|---|---|---|---|---|---|
| 001—033 | 历史定稿（含 B5-r2 的 031/032/033） | 不变 | 不变 | **否** | 维持 | 不拼接、不重审、不改拟修。不能把本批说成 34 项新缺陷。 |
| 034 | B5-r3 CX-B5-01 ∪ GR-01 | 冲突 | 是 | 是 | **接受** | 四处人读报错夹英文；只修显示文本与对应真实回归/版本。 |

候选表队列合计 **1 是**（034）/ **33 否**（001—033）/ 待答 0。分类相对旧 33 项：新增冲突 1。机检 34 个连续唯一 ID 各一次。

无漏并：CX-B5-01 + GR-01 全部进入 034。无改写本席 GR-01 事实。无新添加项、无新用户人闸、无 HTML 申请、无 LOCK-AMEND。冻结 scope 不增不减。本席不修复、不把 Claude 授权扩到后续模块。

## 11. 整体结论

**PASS**

- 输入：`modules/M006-b5-r3-self-raw.md`（全文 40760 / `2640e139…`；独立票前 34586 / `72da471a…`）；`modules/M006-b5-r3-grok-raw.md`（21532 / `06506fb2…`）。候选 findings.md **V0.65** / `0974cdae8f4cfc954a302f68773d08853e35fb63a3dc78b5bc55d866889fec7a`。
- 全批 2 条意见归并正确：034 冲突 / 工程缺陷是 / 队列是；旧 001—033 队列否不变。
- 四处精确边界：`AccessService.java` 515、1061、1491、1694 仅人读 message；测试 V0.20→V0.21；源版本 V0.13→V0.14。内部枚举、币种允许值、权限、业务 HTML、翻译器均保留。
- 0 添加、待答 0。不存在有条件通过。本票只定本批 B2 候选复核；不进入 B3、不写源代码。
- 实际修复尚未开始。本席 FAIL 否决权未动用。双 PASS 仍须 Codex 本批 B2 自票合取；任一方 FAIL 则整批 FAIL。本席不得以任何方式覆盖或改写对方票。

# M006 B2 整批候选复核（Grok 席）
- 版本：V0.08
- 角色：审计席乙（独立；第一性原理 / 对抗式 + 顺从式）
- 模块：M006 `10-backend-main-access-01`（6/322）
- 步骤：B2（由 B5-r4 合取 FAIL 回退后的整批归并复核）；不执行 B3/B4/B5，不重开其他模块，不修复源代码
- 控制：V0.17；ledger `IN_PROGRESS`；B3 累计=4（含操作员例外，已超提示词 3 次上限）；planrepair=2；runtimefailure=0
- 席位：本轮「你（Codex）」职责由 Claude Fable 5.1 承担（席甲）；本席仍为席乙。定稿后第 5 次修复须操作员另行指令。
- 本席只读追加本文件；不修代码、不改方案、不改 HTML/锁/台账/问题、不创建其他产物或测试文件（探针仅在 `.scratch/m006-b2-r4-grok-verify/`）、不共享 Maven 编译、不派第三席
- 互盲：本步可读两份 b5-r4 raw 与 findings.md；**未读取** `modules/M006-b1-self-raw.md`（无论其末尾是否已有本批 B2 追加）
- 不以本席 B5-r4 PASS 票直接否决席甲两条新意见，也不表演性同意；事实以本次 javac 探针与源码/锁原文为准
- 全批结论：**PASS**

本次 B2 按派发不写 `CRITERIA-LOCK-SHA256:` 独占行。正文叙述本席实算锁哈希（见 §1），不得被闸 1 误取为本步④。

## 0. 技能预检（本节点实核）

按意图执行：B2 只用结构，不修代码、不派子代理、不等操作员。grilling 的「等用户回答 / 派子代理查事实」作废，自问自答并自己读源码、自己跑探针。receiving-code-review 的 IMPLEMENT 作废。verification-before-completion：PASS 前实算指纹并跑探针。ponytail-review 只列可删不修。systematic-debugging 只借读错误→复现→追数据流，不加诊断桩。

| 技能 | 本节点 | 缺口 |
|---|---|---|
| using-superpowers | 开工前自检 | 无 |
| grilling / grill-me | 只用追问结构；035/036 及登记项自问自答 | 不等操作员、不派子代理 |
| verification-before-completion | PASS 前实算指纹、跑 `.scratch` 探针 G/H | 无 |
| receiving-code-review | 先核实再接受或驳回候选；禁止表演性同意 | IMPLEMENT 作废 |
| ponytail-review | §8 | 不修 |
| diagnosing-bugs / systematic-debugging | 构造可失败回路 B2Probe（计数 listPermissions；orphan copy） | 不插桩、不改生产码 |
| codebase-design | 缺陷接缝在 isDailyPermission 每次重查字典、copy 隔离先解引用 organizationId | 无 |
| tdd / test-driven-development | B2 不落生产测试 | 不适用 |
| resolving-merge-conflicts | 无合并 | 不适用 |
| writing-for-agents | 本报告给调度席读 | 无 |
| i-have-adhd | 核到存在；完整差集表与「列表≤5」冲突时以审计合同为准 | 无 |
| git-guardrails-claude-code | 核到 `mattpocock-skills/.../misc/git-guardrails-claude-code`；本席只读、未改 hook | 未调用本体 |
| batch-grill-me / ubiquitous-language | 本席技能树不存在独立 SKILL.md | **缺口：不存在，未冒充已调用** |

划掉不用（与合同一致，未调用）：dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、executing-plans、ponytail-audit/debt/gain、research、ponytail（仅 B3 默认强度）。

未核到的不写已核实。未派子代理。未启动其他模块全审。未扩大为 B5 重审。未改三份业务 HTML。

## 1. 输入指纹（本席实算，与派发声明对照）

| 路径 | 字节 | SHA256 | 对照 |
|---|---:|---|---|
| `docs/audit-2026-09/modules/M006-b5-r4-self-raw.md` 全文 | 21923 | `af7113cfa9b5bb2e1aa11e3f3fe1257c604dc8798116fe18409389b52b354189` | 含 V0.01 独立票 + V0.02 接收追加 |
| 同上前 20933 字节（席甲 B5-r4 独立票 V0.01） | 20933 | `73e12799fdb8bc193e2a046241098a66338aa72cb1fde4e0daf914496a4cdb9a` | 与候选台账声明一致；本票差集用该独立票的 CX-B5-01/02，V0.02 只作接收记录 |
| `docs/audit-2026-09/modules/M006-b5-r4-grok-raw.md` 全文（本席 B5 V0.01） | 27075 / 372 行 | `2e9767db6979830e1385e71938d5ff06f6994ccc4a490aab28ba315472efa8f8` | 与候选台账声明一致；本次完整报告，无后续追加审计票 |
| `docs/audit-2026-09/findings.md` V0.68 | 259119 | `d92504e7453ab411f8881fb50c7b9f83d200c50331ea8aad639a0c6d2ec46837` | 本席独立 `sha256sum`；候选段标题「## M006 B5-r4 双席结论与 B2 候选（V0.68）」 |
| 本文件追加前 | 183202 | `52161f380ead116fd07a269566f29cd6ef328708a7433eff415e3e5e2eba6e33` | 保留已有 V0.01–V0.07 前缀，不得覆盖 |

本席独立实算 `docs/audit-2026-09/plan/criteria-lock.md` 全文哈希 = `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（sidecar `plan/criteria-lock.sha256` 内容同行；`ledger.md` 顶部同值；`LOCK-AMEND-COUNT: 0`）。**本 B2 报告不新增该机读哈希独占行。** `git rev-parse HEAD` = `395e6fbab027020da1c38237fad17e8edcd7a132`，分支 `main`。

本次输入 B5 raw 路径（固定）：
- `docs/audit-2026-09/modules/M006-b5-r4-self-raw.md`
- `docs/audit-2026-09/modules/M006-b5-r4-grok-raw.md`

候选版本：findings.md **V0.68** / SHA256 `d92504e7453ab411f8881fb50c7b9f83d200c50331ea8aad639a0c6d2ec46837`。

`questions/M006-027.md` 的 `## 操作员答复` 段首个非空行精确为 `保留`。本批无新添加项、无新 2.2 文件、无 HTML 申请、无 LOCK-AMEND。

本席结论：**PASS**

## 2. 差集方法

B2 无互盲。全集：

- 席甲 B5-r4 本次完整独立票（self-raw 前 20933 字节 / V0.01）临时编号 **CX-B5-01、CX-B5-02**（2 条）
- 本席 B5-r4 本次完整报告（grok-raw 全文 V0.01）临时编号 **无新 GR**（0 条缺陷）；ponytail/分类备忘入登记
- 当前候选 `findings.md` V0.68：旧 001—034 队列否、不重开；**候选新 ID 仅 035、036，队列是**；登记段不入队
- 只审本批 2 条新意见的归并、2.1 分类、冻结判据、工程缺陷、队列与拟修边界，以及登记段是否应升格或剔除；不拼接历史票、不把台账写成「36 项新缺陷」

映射（与候选表一致，本席逐项对照，不另编号）：**CX-B5-01 → M006-035**；**CX-B5-02 → M006-036**。机检 M006-001…036 连续唯一；035/036 各出现 1 次，无复用旧号。无第三条 B5-r4 缺陷被漏并。

## 3. 独立核实（类型 4；禁止用本席 B5 PASS 代替）

探针目录：`.scratch/m006-b2-r4-grok-verify/`（`B2Probe.java`）。`javac --release 21`，`-d` 仅该目录 `classes`，不写 `backend/target`。用 mock `AccessRepository` 计数 `listPermissions`；orphan staff（`organizationId=null`）对 org 拥有模板调用 `copyPermissionTemplate`。

```text
G_EFFECTIVE listPermissions_calls=201 grants=133 wildcard=false
G_REQUIRE_DAILY listPermissions_calls=1
G_STAFF_EFFECTIVE listPermissions_calls=0
G_RESULT PASS
H_ORPHAN kind=NullPointerException detail=Cannot invoke "String.equals(Object)" because the return value of "com.complex.platform.access.AccessActor.organizationId()" is null
H_SAME_ORG kind=NO_EXCEPTION
H_ORPHAN_UPDATE kind=ApiException:PROJECT_SCOPE_REQUIRED
H_RESULT PASS
PROBE_EXIT 0
JAVA_EXIT=0
```

与席甲 raw §4 探针 G/H 数字与异常类型一致。本席另跑对照：同一 orphan 走 `updatePermissionTemplate` 得 `ApiException:PROJECT_SCOPE_REQUIRED`（L146 先 `requireProjectScope`），证明 036 拟修范式已在相邻写入口成立，copy 是漏接而非新业务规则。

`GlobalExceptionHandler.java:162-167` `@ExceptionHandler(Exception.class)` → HTTP 500 / `INTERNAL_ERROR` / 「服务出现异常，请稍后重试」。本席未把 copy 打到真实 HTTP 口；500 归因为源码映射 + 服务层 NPE（类型 4）。不把未跑 HTTP 写成已验证出口，也不因此否定 NPE 本身。

本席 B5-r4 未计数 `listPermissions`、未构造 orphan copy，故当时 GEN-009/012 写「满足」——那是漏检，不是反证。本次探针推翻该两处「满足」，不推翻旧 34 ID 的出队证据。

## 4. 2.1 分类与冻结判据（冲突→细化→技术性→添加）

锁定义（本席直接读 `criteria-lock.md`）：
- GEN-012：「循环、查询、批处理、缓存、连接、线程和文件句柄须有与实际负载相称的边界并正确释放，不制造无界放大。」
- GEN-009：「错误须可检测、可定位并安全失败；不得吞错、伪成功、泄漏半成品或用不相关默认值掩盖失败。」
- BSM-114 HTML 原文标题：「各类入口：不同人只看自己该看的、只做自己该做的」（`business-system-map-for-operations-v0.1.html:1827`）
- PGM-048 HTML 原文标题：「商户侧：在授权业务范围内全开放」及「不能跨商户和看平台全局」
- M006 映射列表含 GEN-001/009/012、BSM-114、PGM-048。引用成立，无需 LOCK-AMEND。

**035：** HTML 无「有效权限计算不得 N+1」明文 → 非冲突。不能把查询次数指到某一 HTML 锚点作为下位实现 → 非细化。无业务发明、不与 HTML 冲突 → **技术性**。候选分类正确。冻结 GEN-012 成立：一次 `effectivePermissions()` 对 200 条字典触发 201 次 `listPermissions()`（Jdbc 实现每调一次即一条无缓存 SELECT，该文件属 M007，作调用链窄读，缺陷本体在 M006 `AuthorizationService`）。放大倍数 = 字典规模；平台管理员加载有效权限是第一次正常跑就会撞上的路径，不是两三件巧合。工程缺陷 **是**。技术性 + 缺陷 → 修复队列 **是**。拟修（单次计算内取一次字典、不改判定语义、不加缓存框架、补可失败断言）不发明业务。候选「不超过 2 次」是 B3 断言上界，不是新判据。

**036：** HTML 未规定必须抛 `ApiException` 而非 NPE → 非冲突。copy 的跨组织比较是 BSM-114「只看该看的」的下位实现（PGM-048 作商户侧隔离的邻近上位，偏弱但同属隔离族，不把分类拉成添加）→ **细化**。冻结 GEN-009 成立：fail-closed、无越权、无伪成功，但 NPE 对人读不可定位（将落 500 通用句），且早于 L193 `requireProjectScope`。orphan 是 M006-003 已承认的真实主体形态（测试 `missingOrganizationCannotListAllUsersOrResolveGlobalProject`）。工程缺陷 **是**。细化 + 缺陷 → 修复队列 **是**。拟修与 L146 同范式。席甲 B5 §2 GEN-017 行曾写「两条均为技术性」，与 CX-B5-02 正文「细化」不一致；**候选采用正文细化，未改写 finding**。本席同意以正文为准。

无添加项。Grok 未把技术性改判添加。

## 5. 有没有漏并 / 改写席甲 finding

| 原票 | 候选 | 差集 |
|---|---|---|
| CX-B5-01 位置 L89-94 / L141-168 / L96-101；201/1/0；GEN-012；技术性；缺陷是；队列是 | M006-035 同行号、同数字、同分类/队列/拟修 | **无漏并、无改写事实** |
| CX-B5-02 位置 L186-191；orphan NPE；对照同组织正常；细化 BSM-114/PGM-048；GEN-009；队列是 | M006-036 同位置、同异常、同分类/队列/拟修 | **无漏并、无改写事实** |
| 本席 B5 无新 GR | 未捏造 GR、未把 PASS 写成「无问题故 035/036 不成立」 | 无漏并本席缺陷（本无） |
| 本席 ponytail：`slug()`、wildcard 恒 false、`listGrantRequests(Integer)`、版本头 | 登记段均在 | 无漏并 |
| 本席分类备忘：FieldLabels 英文字段键已译；权限码属内部标识符 | 登记段末条，席甲同意 | 无漏并 |
| 席甲登记：单参 `listGrantRequests`；StaffService:363 属模块外 | 登记段均在；StaffService 标明 M008 | 无漏并、未升格进 M006 队列 |

未把 001—034 重开。未把 Jdbc 窄读扩成新模块 finding。

## 6. 登记段逐条裁定（不入队）

| 项 | 本席核实 | 裁定 |
|---|---|---|
| `AccessService.listGrantRequests(Integer)` 单参重载 | 控制器 `AccessController.java:144` 走双参；`src/main` 无其它调用方；测试 `listGrantRequests(null)` 仍用单参。首次正常 HTTP 不撞上。GEN-001 死代码。 | **维持登记**。不升缺陷。 |
| `AccessService.java:1898` 私有 `slug()` | 本文件仅定义、0 处调用。 | **维持登记**。不升缺陷。 |
| `EffectivePermissions.wildcard` 恒 false | 探针 G：`platform_admin` 的 `wildcard=false`。`computeEffectivePermissions` 固定传 `false`。唯一消费 `AccessService.java:571` 走 false 分支；日常权限已折入 grants，DENY 已预扣。注释漂移，行为正确。 | **维持登记**。不升缺陷。 |
| 文件版本头 AccessService V0.13、测试 V0.20 未随第 4 次 B3 递增 | 源码头仍 V0.13 / 测试仍 V0.20。非用户可见文案、非冻结判据。 | **维持登记**。可随下一批修复一并处理，不单独入队。 |
| `StaffService.java:363` 「通过（approved）或驳回（rejected）」 | 源码属实。`plan/modules.md` M008（L133 起）含该文件。 | **维持登记为模块外**。不升为 M006 缺陷。 |
| FieldLabels / 权限码备忘 | 与本席 B5-r4 TranslateProbe 一致；非缺陷。 | **维持登记**。 |

无登记项应剔除。无登记项本批应升为 035/036 以外的新 ID。

## 7. 旧 001—034

候选声明「当前队列均否，不重开」。本席 B5-r4 已按行为证据维持出队，本 B2 不重审、不回退。035/036 不是 034 回归（034 为人读中文；本两处为查询次数与 NPE）。

## 8. ponytail-review（只列可删，不修）

`AccessService.java:L789-804 delete:` 单参 `listGrantRequests(Integer)`，测试改双参即可。`AccessService.java:L1898 delete:` 未使用 `slug()`。`net: 可删死代码，不构成本批入队项。` 035/036 的拟修是收窄已有实现（复用一次字典；先 `requireProjectScope`），不是加抽象。

## 9. 对抗式自问（不等操作员）

1. 能否用「字典有界约 219、不是无界」否决 035？——GEN-012 同时要求与负载相称的边界。单次有效权限请求对字典逐项重复全表查询，放大倍数随字典线性增长；平台管理员加载该入口第一次就会撞上。否决不成立。
2. 能否用「orphan 无 manage 权限则控制器先 403」否决 036？——服务方法在 `executeTemplateWrite` 内不先 `requirePermission`；M006-003 已承认无组织主体；同入口 `update` 已对 orphan 抛 `PROJECT_SCOPE_REQUIRED`。错误路径不可定位成立。否决不成立。
3. 035 是否应改细化/添加？——指不出 HTML 锚点；无业务发明。技术性正确。
4. 036 是否应改冲突？——实现试图隔离但崩溃，并未明文允许跨组织。细化 + GEN-009 正确。
5. 本席 B5 PASS 能否覆盖？——不能。合取已 FAIL；本 B2 以探针接受席甲两条。

## 10. 三十六 ID 与本批裁定

图例：差集=原票是否完整进入候选；本席 B2=是否接受候选**当前**分类/队列/范围。

| ID | 来源 | 候选分类 | 工程缺陷 | 队列 | 本席 B2 | 说明 |
|---|---|---|---|---|---|---|
| 001—034 | 历史定稿（含 B5-r3 的 034） | 不变 | 不变 | **否** | 维持 | 不拼接、不重审、不改拟修。不能把本批说成 36 项新缺陷。 |
| 035 | B5-r4 CX-B5-01 | 技术性 | 是 | 是 | **接受** | GEN-012；N+1 已复现 201/1/0。 |
| 036 | B5-r4 CX-B5-02 | 细化 | 是 | 是 | **接受** | GEN-009 + BSM-114；orphan copy NPE 已复现；update 对照为 ApiException。 |

候选表本批新队列合计 **2 是**（035/036）/ 旧 **34 否**（001—034）/ 待答 0。无新添加。机检 36 个连续唯一 ID；035/036 未占用旧号。

无漏并：CX-B5-01 + CX-B5-02 全部进入 035/036。无改写席甲事实。冻结 scope 不增不减。本席不修复。双 PASS 仍须席甲本批 B2 自票合取；任一方 FAIL 则整批 FAIL。

## 11. 整体结论

**PASS**

- 输入：`modules/M006-b5-r4-self-raw.md`（全文 21923 / `af7113cfa9…`；独立票前 20933 / `73e12799fdb8bc193e2a046241098a66338aa72cb1fde4e0daf914496a4cdb9a`）；`modules/M006-b5-r4-grok-raw.md`（27075 / `2e9767db6979830e1385e71938d5ff06f6994ccc4a490aab28ba315472efa8f8`）。候选 findings.md **V0.68** / `d92504e7453ab411f8881fb50c7b9f83d200c50331ea8aad639a0c6d2ec46837`。
- 全批 2 条新意见归并正确：035 技术性 / 工程缺陷是 / 队列是；036 细化 / 工程缺陷是 / 队列是。旧 001—034 队列否不变。登记 6 项均维持不入队、不升格、不剔除。
- 0 添加、待答 0。不存在有条件通过。候选台账无需按本席意见改写。
- 本票只定本批 B2 候选复核；不进入 B3、不写源代码。B3 已 4 次，第 5 次修复须操作员另行指令。
- 实际修复尚未开始。本席 FAIL 否决权未动用。双 PASS 仍须席甲本批 B2 自票合取。
