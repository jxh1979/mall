# T13 前端 effective-permissions 渲染 可编码开发规格 V0.1（2026-06-18）

> **任务**：R5 backlog **T13**（W2），前端 + access 域。
> **裁决依据**：裁决⑪——前端按后端 effective-permissions 渲染（新增拉取接口），与后端 DENY 优先口径一致，本期 P0。
> **现状基准**（已核）：后端 `AuthorizationService` 有 `requirePermission`（DENY 优先四源），但**无对外"列出有效权限集"方法**；前端 `apps/web/src/admin/security/PermissionGate.tsx` 用**硬编码 `adminRolePermissions` 角色映射** + `user.permissions` 合并，`granted.includes("*")` 通配，**无 DENY 语义**、无后端拉取。
> **无新迁移**：复用既有权限表（permissions/roles/role_permissions/user_roles/user_permission_overrides/permission_templates 等）。

---

## 1. 后端：新增有效权限集接口
- `AuthorizationService.effectivePermissions(userId)`：复用既有四源计算（角色模板 + 角色 + 用户授权 + 临时授权），返回 **DENY 优先解析后的最终授予集**（grants 减去 denies），形如：
```
{ userId, grants: ["ticket.verify.execute", ...], denies: ["finance.refund.approve", ...], wildcard: false }
```
- 端点：`GET /api/v1/access/effective-permissions`（当前登录用户自身，scope 安全）；可选 `?userId=`（需 user.read 权限，平台管理用）。
- DENY 优先：deny 覆盖 grant（与 `requirePermission` 同口径，D11）；`wildcard=true` 仅超管。

## 2. 前端：PermissionGate 改造
- 启动/登录后调 `GET /access/effective-permissions` 缓存到上下文（React context / store），**替换硬编码 `adminRolePermissions` 映射**。
- `userHasPermissions` 改为：`wildcard || (required.every(p => grants.includes(p)) && required.none(p => denies.includes(p)))`——**DENY 优先**（被 deny 的即使在 grants 也拒绝），修正现状无 DENY 语义。
- `PermissionGate` 渲染：有效集未加载时显示 loading；加载后按上式判定，不足显示既有 `admin-permission-denied`。
- 兜底：拉取失败 → fail-safe 隐藏敏感入口（不放行）。

## 3. API / 事件 / 权限
- `GET /api/v1/access/effective-permissions`（self）；`GET /api/v1/access/users/{id}/effective-permissions`（权限 `user.read`）。
- 权限点（纳入 T13 全域预置）：复用 `user.read`。

## 4. 验收（GWT）
- Given 用户有角色授予 X 但被用户级 deny X，When 查 effective-permissions，Then X 不在最终集（DENY 优先）；前端对应入口隐藏，与后端 requirePermission 拒绝一致。
- Given 前端渲染，When 加载有效集，Then 入口按后端结果显隐，不再依赖前端硬编码角色映射。
- Given 接口拉取失败，Then 敏感入口 fail-safe 隐藏。
- 前后端一致性：同一用户对同一权限点，前端显隐 == 后端 requirePermission 放行/拒绝。

## 5. 测试 / 依赖 / claims
- 后端：effectivePermissions 四源 + DENY 优先单测（覆盖 grant-only/deny-override/wildcard/临时授权过期）。
- 前端：PermissionGate DENY 优先、loading/fail-safe、与后端契约测试。
- 依赖：既有 access 模块（无迁移）。
- **claims**：无新增表；矩阵 REQ-58/59 备注补"前端按 effective-permissions 渲染、DENY 优先前后端一致"。

## 6. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T13 规格（后端 effective-permissions 接口 DENY 优先+前端 PermissionGate 改读后端集替硬编码映射+fail-safe） | R5 backlog T13；裁决⑪；现状取自 AuthorizationService/PermissionGate.tsx |
