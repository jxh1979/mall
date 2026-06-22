# C3a 前端 effective-permissions 可编码实施规格 V0.1（2026-06-18）

> **任务**：R6 backlog **C3a**（业务已确认"出规格交研发"），前端 admin + access 域，本期 **P0**。
> **裁决依据**：`docs/r5-spec-T13-frontend-effective-permissions-dev-design-20260618.md`（裁决⑪——前端按后端 effective-permissions 渲染、DENY 优先、与后端 `requirePermission` 同口径）。本规格在 T13 设计基础上，落到**可直接编码的接口契约、改造点、验收用例与排期**。
> **唯一权限事实源**：`docs/p0-operation-permission-matrix-v0.1.md`（点式 `domain.resource.action`，DENY 优先级最高，见其 §2.2 第 5/6 条）。
> **无新迁移**：复用既有 access 权限表（permissions / roles / role_permissions / user_roles / permission_templates / permission_template_applications / temporary_grants / user_permission_overrides 等），见 `AccessRepository.findPermissionEffects`。
> **本规格只出规格、不改代码、不跑测试/构建。**

---

## 0. 现状核查结论（已只读核验）

| 核查项 | 结论 | 证据 |
|---|---|---|
| 后端是否已有 effective-permissions 端点 | **无**。全仓库（`*.java/*.kt/*.yaml/*.json/*.ts`）无 `effectivePermissions` / `effective-permissions` / `effective_permissions` 任何引用 | grep 全仓零命中 |
| OpenAPI 是否声明该路径 | **无**。`docs/openapi/openapi-p0.yaml` / `openapi-p0.json` access 域只有 `permission-templates*`、`permissions`、`roles`、`temporary-grants`、`users`、`staff-levels`，**无 effective-permissions** | grep `docs/openapi/` |
| 后端 DENY 优先逻辑 | **已具备**，但**仅在 `requirePermission(单个 code)` 内**，无"列出有效集"的对外方法 | `AuthorizationService.requirePermission`（DENY 命中即拒、GRANT 取最小 sourceId、单次授权消费） |
| 四源权限计算 | **已具备**：`AccessRepository.findPermissionEffects(userId, checkedAt)` 返回 `PermissionEffect{permissionCode, effect(GRANT/DENY), source(ROLE/TEMPLATE_APPLICATION/TEMPORARY_GRANT/USER_OVERRIDE), validFrom, validUntil, singleUse, consumed}` | `AccessRepository.java` §112-135 |
| 前端 PermissionGate | **硬编码角色→权限映射**，无 DENY、无后端拉取、通配 `*` | `apps/web/src/admin/security/PermissionGate.tsx` |
| 前端测试 | **固化了错误实现**：`PermissionGate.test.ts` 直接调 `userHasPermissions(user(role), …)`，把"硬编码角色矩阵"当作正确性基准断言 | `PermissionGate.test.ts` §44-67 |

**结论：后端端点需新增**（复用现有四源计算与 DENY 语义，不新增表、不新增迁移）。

---

## 1. 现状基准（必须被本期推翻）

### 1.1 PermissionGate 硬编码（`apps/web/src/admin/security/PermissionGate.tsx`）
- 模块级常量 `adminRolePermissions: Record<string, string[]>`：把 21 个 `accountType` 写死成权限数组，`platform_admin`/`super_admin` = `["*"]`。
- `grantedPermissionsFor(user)` = `adminRolePermissions[normalizeAccountType(user.accountType)] ∪ user.permissions`。
- `userHasPermissions(user, perms)` = `granted.includes("*") || perms.every(p => granted.includes(p))`。

**问题（对照 T13 与权限矩阵 §2.2）**：
1. 角色→权限映射在前端**第二处事实源**，与后端 role_permissions/模板/临时授权完全脱钩，必然漂移。
2. **无 DENY 语义**：用户级/模板级 `DENY` 在前端不可表达，被 deny 的入口仍会显示（违反矩阵 §2.2 第 6 条"拒绝优先级最高"）。
3. 通配 `*` 由 `accountType` 字符串决定，绕过后端 `isPlatformAdminBridge` 的 `status=active` 校验。
4. **无 fail-safe**：不拉后端，无"加载失败兜底隐藏"概念。
5. 临时授权有效期 / 单次消费 / 模板停用等动态状态前端完全感知不到。

### 1.2 测试固化错误实现（`apps/web/src/admin/security/PermissionGate.test.ts`）
- `documentedPrimaryRoleMatrix`（29 行 pageId→roles）+ 两个 `it`，全部以 `userHasPermissions(user(role), route.requiredPermissions)` 为真值。
- 该测试**不验证与后端一致性**，只验证"硬编码映射自洽"。一旦改成读后端集合，此测试必然失败且其断言无意义——**本期必须替换**，否则会把错误实现钉死。

### 1.3 消费点
- `apps/web/src/admin/AdminApp.tsx`：
  - §185 侧边栏导航 `const allowed = userHasPermissions(user, item.requiredPermissions)`（控制 `aria-disabled`）。
  - §208 `<PermissionGate user={user} permissions={route.requiredPermissions}>` 包裹页面内容。
- `apps/web/src/admin/types.ts::AdminUser` 当前字段：`id/username/accountType/organizationId/merchantId/permissions?`。

---

## 2. 后端：`GET /api/v1/access/effective-permissions` 契约

### 2.1 端点
| 项 | 值 |
|---|---|
| 方法/路径（self） | `GET /api/v1/access/effective-permissions` |
| 方法/路径（指定用户，平台管理用，可选） | `GET /api/v1/access/users/{id}/effective-permissions` |
| 控制器 | `AccessController`（`@RequestMapping("/api/v1/access")`） |
| self 端鉴权 | 仅需已登录（`authenticated`）；返回**当前 actor 自身**有效权限集，scope 安全，不需额外权限点 |
| 指定用户端鉴权 | `authorizationService.requirePermission("access.user.view")`（矩阵 §3.2 已存在权限码） |
| 认证上下文 | `x-auth-context = staff-token`（员工后台），非客人/设备 |
| 幂等 | GET，无需 `Idempotency-Key` |

> 注：T13 原文写 `user.read`；本规格按权限矩阵 §3.2 实际权威码改用 **`access.user.view`**（账号列表/查看口径一致）。研发以矩阵为准。

### 2.2 响应体（统一 `ApiResponse<T>` 包裹，键 snake_case 出参）
```jsonc
{
  "code": "OK",
  "data": {
    "user_id": "u-123",
    "grants": ["ticket.order.view", "bar.table.open_close", "..."],
    "denies": ["finance.refund.create", "..."],
    "wildcard": false,
    "computed_at": "2026-06-18T08:30:00Z"
  }
}
```
字段语义：
- `grants`：四源 GRANT 去重、且**已剔除当下未生效项**（`consumed` / 未到 `validFrom` / 已过 `validUntil`）后的授予集合。
- `denies`：四源 DENY 去重、当下生效集合。**DENY 与 grants 取交集时不在 grants 里预扣**——前端按"DENY 优先"自行求差（见 §3.3），便于前端展示"冲突来源/原因"（矩阵 §2.2 第 6 条）。
  - 备选实现 A（推荐，前端最简）：后端直接返回 `grants = 已扣减 denies 的最终集` 且仍单独返回 `denies` 供展示。两选一，需在研发评审锁定；本规格验收用例同时覆盖两形态的"有效判定"，对前端显隐结果一致。
- `wildcard`：`true` 仅当 actor 命中 `isPlatformAdminBridge`（`status=active && accountType=platform_admin`）。`wildcard=true` 时前端视为全部放行，但**仍受 `denies` 约束**（DENY 优先，避免超管误授后无法收口）。
- `computed_at`：服务端计算时间，供前端缓存判活/排障。

### 2.3 后端实现要点（复用既有，不新增表）
1. `AuthorizationService.effectivePermissions(String userId)`：
   - `checkedAt = Instant.now()`；`effects = accessRepository.findPermissionEffects(userId, checkedAt)`。
   - 复用 `requirePermission` 同款 `isActive(effect, checkedAt)` 过滤（`!consumed && validFrom 已到 && validUntil 未过`）。
   - `denies = effects.filter(DENY && active).map(code).distinct()`。
   - `grants = effects.filter(GRANT && active).map(code).distinct()`（若取实现 A，再 `removeAll(denies)`）。
   - `wildcard = isPlatformAdmin(actorOf(userId))`。
   - **不在此处消费单次授权**（`markSingleUsePermissionConsumed`）——列表查询是只读，单次消费只发生在真正执行动作的 `requirePermission`，避免"看一眼菜单就把单次权限烧掉"。验收用例须断言：调用本端点不改变 `consumed` 状态。
2. self 端：`userId = currentActor().id()`；指定用户端：先 `requirePermission("access.user.view")` 再按 `{id}` 计算。
3. 留痕：self 端高频只读，**默认不写审计事件**（避免噪声）；指定用户端可写 `permission.viewed`（矩阵已列 `access.permission.view` 的留痕事件 `permission.viewed`，此处复用语义，研发评审确认是否落审计）。

### 2.4 OpenAPI / 权限点 / 事件影响
- **OpenAPI**：在 `docs/openapi/openapi-p0.yaml`+`.json` access 域新增 1~2 个 operation：
  - `GET /api/v1/access/effective-permissions`：`x-permission = authenticated`、`x-auth-context = staff-token`、响应 schema `EffectivePermissions`。
  - `GET /api/v1/access/users/{id}/effective-permissions`：`x-permission = access.user.view`。
  - 需同步 `tools/verify-openapi-contract.mjs`、`p0-openapi-contract-closure-appendix-v0.1.md`、`p0-openapi-operation-acceptance-matrix-v0.1.md` 的反向生成（研发跑既有 `tools/generate-*` 脚本）。
- **权限点**：复用 `authenticated`（通用状态）+ `access.user.view`，**不新增权限码**。
- **事件**：无新增业务事件；如指定用户端留痕，复用 `permission.viewed`。
- **迁移**：**无**（沿用 R5 现状 Flyway 版本，不新增 V0xx）。

---

## 3. 前端：PermissionGate 改造

### 3.1 数据获取
- 登录成功后（`AdminApp` 拿到 token / user 后），调用 `api.get("/api/v1/access/effective-permissions")` 拉取自身有效集，缓存到一处**有效权限上下文**（React context 或 AdminApp 顶层 state；本仓 admin 体量小，顶层 state + props 透传即可，不强制引入 store）。
- 形状：`{ grants: string[]; denies: string[]; wildcard: boolean; computedAt?: string }`。
- `adminClient` 的 `unwrap` 会把 `ApiResponse.data` 解包并 camelCase 化（见 `adminClient.ts` §37-45），前端拿到的是 `{ userId, grants, denies, wildcard, computedAt }`。

### 3.2 类型与状态
- `apps/web/src/admin/types.ts` 新增：
  ```ts
  export type EffectivePermissions = {
    grants: string[];
    denies: string[];
    wildcard: boolean;
    computedAt?: string;
  };
  export type EffectivePermissionsState =
    | { status: "loading" }
    | { status: "ready"; value: EffectivePermissions }
    | { status: "error" };
  ```
- `AdminUser.permissions?` 字段及其在 PermissionGate 中的合并逻辑**移除**（权限只来自后端有效集，不再用 `user.permissions`）。如其他处仍读 `user.permissions`，研发需一并清理（grep 当前仅 PermissionGate 使用）。

### 3.3 判定函数（替换 `userHasPermissions`）
```ts
// DENY 优先 + wildcard + fail-safe
export function hasPermissions(
  state: EffectivePermissionsState,
  required: string[],
): boolean {
  if (state.status !== "ready") return false;      // loading/error → fail-safe 隐藏
  const { grants, denies, wildcard } = state.value;
  if (required.some((p) => denies.includes(p))) return false;  // DENY 优先（含 wildcard 也拒）
  if (wildcard) return true;
  return required.every((p) => grants.includes(p));
}
```
- **DENY 优先**：任一 required 命中 `denies` 即拒，**优先于 wildcard 与 grants**（对齐矩阵 §2.2 第 6 条、后端 `requirePermission` 先判 DENY 再判 GRANT）。
- **wildcard**：仅在无 DENY 命中时放行全部。
- **fail-safe**：`loading` 与 `error` 都返回 `false`，敏感入口默认隐藏/禁用，**不放行**。

### 3.4 PermissionGate 组件
```tsx
export function PermissionGate({
  permissionsState, permissions, children,
}: { permissionsState: EffectivePermissionsState; permissions: string[]; children: React.ReactNode }) {
  if (permissionsState.status === "loading") {
    return <div className="admin-loading" data-testid="admin-permission-loading">权限加载中…</div>;
  }
  if (!hasPermissions(permissionsState, permissions)) {
    return <div className="admin-denied" data-testid="admin-permission-denied">权限不足：当前账号不能访问该后台页面。</div>;
  }
  return <>{children}</>;
}
```
- 保留既有 `data-testid="admin-permission-denied"` 以兼容现有 E2E/选择器；新增 `admin-permission-loading`。
- `error` 态走 `hasPermissions=false` → 渲染 denied（fail-safe）；可叠加全局错误提示，但**入口必须隐藏**。

### 3.5 消费点改造（`apps/web/src/admin/AdminApp.tsx`）
- 顶层维护 `effectivePermissions: EffectivePermissionsState`（初始 `loading`，登录后拉取，失败置 `error`）。
- §185 导航：`const allowed = hasPermissions(effectivePermissions, item.requiredPermissions)`（loading/error → `aria-disabled` 且不可点）。
- §208：`<PermissionGate permissionsState={effectivePermissions} permissions={route.requiredPermissions}>`。
- 删除 `import { …userHasPermissions }`，改 `hasPermissions`。

### 3.6 移除项（硬编码清退）
- 删除 `adminRolePermissions` 常量。
- 删除 `grantedPermissionsFor`、旧 `userHasPermissions`（或保留兼容签名但内部转调，**不建议**，本期目标是清退第二事实源）。
- 删除 `normalizeAccountType`（若无其他引用）。

---

## 4. 验收口径（GWT）

### 4.1 前后端一致性（核心）
- **Given** 任意账号 + 任意 `requiredPermissions`，**When** 前端 `hasPermissions` 与后端对同组权限逐个 `requirePermission`，**Then** 前端显隐结果 == 后端放行/拒绝结果（同口径）。
- 落地为**契约测试**：以后端 `effective-permissions` 返回的同一 `{grants,denies,wildcard}` 喂给前端 `hasPermissions`，对每个 `adminRoutes[].requiredPermissions` 断言结果，与后端 `requirePermission` 行为一致。

### 4.2 DENY 优先反例
- **Given** 用户角色授予 `finance.ar_ap.view`、但用户级 `USER_OVERRIDE` 对其 `DENY`，**When** 拉 effective-permissions，**Then** `denies` 含该码；前端对应入口**隐藏**，与后端 `requirePermission("finance.ar_ap.view")` 抛 `PERMISSION_DENIED` 一致。
- **Given** `wildcard=true`（平台管理员）但 `denies` 含某码，**When** 判定该码，**Then** 仍**拒绝**（DENY 优先于 wildcard）。

### 4.3 fail-safe 反例
- **Given** effective-permissions 接口返回 5xx / 网络失败，**When** 渲染后台，**Then** `state=error`，所有敏感入口**隐藏/禁用**，`PermissionGate` 渲染 denied，**绝不**放行。
- **Given** 接口尚未返回（`loading`），**When** 渲染，**Then** 入口禁用、内容区显示 `admin-permission-loading`，不提前放行。

### 4.4 只读不消费
- **Given** 用户持有 `singleUse` 临时授权，**When** 调用 effective-permissions（可多次），**Then** 该授权 `consumed` 不变，仍出现在 `grants`；只有真正执行动作的 `requirePermission` 才消费。

### 4.5 测试替换（关键）
- **替换** `apps/web/src/admin/security/PermissionGate.test.ts`：删除 `documentedPrimaryRoleMatrix` 角色硬编码矩阵驱动的两个 `it`（它们固化了错误实现）。
- 新测试覆盖：`hasPermissions` 的 grant-only / deny-override / wildcard / wildcard+deny / loading / error / 空 required 七类；以及 §4.1 契约一致性（mock effective-permissions 响应 → 遍历 `adminRoutes`）。
- 后端新增 `AuthorizationServiceTests` / `AccessControllerTests` 用例：`effectivePermissions` 四源合并、DENY 覆盖、wildcard、临时授权过期不入集、单次授权查询不消费、self vs `{id}`（后者需 `access.user.view`）。

### 4.6 OpenAPI 契约
- `tools/verify-openapi-contract.mjs` 通过；新 operation 出现在反向生成的 closure appendix 与 acceptance matrix。

---

## 5. 开发风险与排期建议（跨前后端）

### 5.1 风险
| 风险 | 说明 | 缓解 |
|---|---|---|
| `grants/denies` 形态二义 | §2.2 实现 A（预扣 denies）vs B（不预扣）未锁 | 研发评审第一项决策；前端 `hasPermissions` 对两形态显隐结果一致，验收用例兜底 |
| 单次授权被查询误消费 | 若 effectivePermissions 误调 `markSingleUsePermissionConsumed` | 明确只读路径不消费，§4.4 用例守门 |
| `user.permissions` 残留引用 | 移除后若他处仍读会编译/行为异常 | 全仓 grep（现仅 PermissionGate），随改随清 |
| 现有 E2E 选择器 | 依赖 `admin-permission-denied` | 保留该 testid，仅新增 loading testid |
| 旧测试钉死错误实现 | 不替换则改造必红 | §4.5 明确删除并重写，作为本期交付物 |
| 权限码与矩阵漂移 | T13 写 `user.read`，矩阵实为 `access.user.view` | 统一以权限矩阵为准（本规格已采用 `access.user.view`） |
| OpenAPI 多文件同步 | yaml/json + 反向生成文档 | 走既有 `tools/generate-*` 脚本，勿手改生成物 |

### 5.2 排期建议（建议 W2，约 2.5~3.5 人日）
1. **D1 后端**：`AuthorizationService.effectivePermissions` + `AccessController` 两端点 + DTO；后端单测（四源/DENY/wildcard/过期/不消费/`access.user.view` 守卫）。
2. **D1.5 OpenAPI**：新增 operation + 跑 `tools/verify-openapi-contract.mjs` 与反向生成。
3. **D2 前端**：`types.ts` 增类型、AdminApp 拉取与状态、`hasPermissions`、PermissionGate 改造、移除 `adminRolePermissions`、清 `user.permissions`。
4. **D2.5 前端测试**：替换 `PermissionGate.test.ts`，新增契约一致性测试。
5. **D3 联调验收**：§4 全量 GWT；前后端同口径回归。

依赖：现有 access 模块与权限表（无迁移）。前端可在后端端点合并前用 mock 响应并行开发（D1 与 D2 可并行，D3 收口）。

---

## 6. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 C3a 可编码实施规格：核实后端无 effective-permissions 端点（需新增、复用四源+DENY、无迁移）；定义 self/`{id}` 两端点契约与响应体；前端 PermissionGate 改读后端集、DENY 优先、loading/fail-safe，移除 `adminRolePermissions` 与 `user.permissions`；替换固化错误实现的 `PermissionGate.test.ts`；验收 GWT 与跨前后端排期 | R6 C3a；`r5-spec-T13`；`p0-operation-permission-matrix-v0.1.md`；现状取自 `AuthorizationService.java`/`AccessRepository.java`/`AccessController.java`/`PermissionGate.tsx`/`PermissionGate.test.ts`/`AdminApp.tsx`/OpenAPI |
