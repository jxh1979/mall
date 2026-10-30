# 完整后台首批 MVP 页面开发包 V0.1

**来源矩阵**：`docs/full-admin-page-permission-field-matrix-v0.1.csv`
**关联 SRS**：`docs/full-admin-srs-v0.1.md`
**关联 UI 验收**：`docs/full-admin-ui-acceptance-matrix-v0.1.md`
**版本**：V0.1
**日期**：2026-06-16
**状态**：可进入编码计划拆分，尚未代表页面已实现

> 本开发包只拆首批完整后台 MVP 页面，不扩写业务范围。当前 Web 端仍是轻量管理入口；本包目标是把它推进为可验收的完整后台第一批页面。所有代码仍应在 160 仓库 `/home/james/complex-management` 完成。

## 1. 目标与边界

首批 MVP 页面覆盖 7 个业务主题、8 个页面 ID：总控驾驶舱、员工账号、权限模板、票务策略、卡座与订台、应收应付台账、供应商适配器、生产 blocker 与证据。

本包不做以下事项：

1. 不把所有完整后台页面一次性实现。
2. 不引入新的 `/api/admin/...` 非正式接口；前端先使用已进入 OpenAPI 的 `/api/v1/...` 路径。
3. 不把当前 `apps/web/src/main.tsx` 继续无限堆大；首批页面编码必须同步拆出后台页面、API、类型和测试模块。
4. 不把第三方真实供应商资料写死到前端；只展示配置、凭证引用、联调状态和 mock/测试结果。
5. 不绕过权限、审计、错误状态和移动端关键路径验收。

## 2. 当前代码事实

| 项目 | 当前事实 | 对开发包的影响 |
|---|---|---|
| Web 入口 | `apps/web/src/main.tsx` 仍是单文件大入口，约 2967 行 | 首批后台页面必须拆模块，否则回归风险继续上升 |
| UI 自动化 | 已有 `tests/e2e/web-ui-acceptance.spec.ts` 和 mock API | 新页面必须扩展独立 `full-admin-mvp` 验收，不得只复用旧轻量入口断言 |
| 后端契约 | OpenAPI 已有 access、ticketing、bar、finance、integration、ops、analytics 相关路径 | 本包优先使用现有 `/api/v1` 契约；缺口走 OpenAPI 变更任务 |
| 生产保护 | 160 编码主机保护、P0 契约、安全基线、生产证据门禁已接入 | 本包完成后至少跑 `typecheck`、`build`、`verify:ui`、`verify:p0-contract` 或等效子集 |

## 3. 页面范围与接口映射

| 页面 ID | 页面 | 路由 | 首批接口映射 | 首批实现级别 |
|---|---|---|---|---|
| ADM-OV-001 | 总控驾驶舱 | `/admin/overview` | `GET /api/v1/analytics/dashboards/p0-overview`、风险/审计/接口摘要沿用现有聚合数据 | 可操作只读 + 异常跳转 |
| ADM-ACC-001 | 员工账号 | `/admin/access/users` | `GET/POST /api/v1/access/users`、角色/权限读取 `GET /api/v1/access/roles`、`GET /api/v1/access/permissions` | 列表、新增、禁用/状态展示、角色分配入口 |
| ADM-ACC-002 | 权限模板 | `/admin/access/permission-templates` | `GET/POST /api/v1/access/permission-templates`、`POST /copy`、`POST /apply`、临时授权路径 | 模板复制、编辑、临时模板、单次有效提示 |
| ADM-TKT-001 | 票务策略 | `/admin/ticketing/policies` | `GET/POST /api/v1/ticketing/ticket-types`、`POST /publish`、entitlements、动态定价只显示入口 | 基础价配置、票种发布、固定/派生互斥校验 |
| ADM-BAR-001 | 卡座与订台 | `/admin/bar/table-sessions` | `GET/POST /api/v1/bar/tables`、`GET/POST /api/v1/bar/reservations`、`POST /check-in-guest`、订单摘要 | 卡座列表、预订、入场人数、转卡/合并操作入口和拦截（无续卡） |
| ADM-FIN-001 | 应收应付台账 | `/admin/finance/ar-ap-ledger` | `GET/POST /api/v1/finance/ar-ap-items`、`POST /api/v1/finance/ar-ap-items/{id}/payments` | 台账列表、生成、部分核销、调整原因、导出留痕入口 |
| ADM-INT-001 | 供应商适配器 | `/admin/integrations/adapters` | `GET/POST/PATCH /api/v1/integration/provider-adapters`、`/mark-production-ready`、`/test-mock` | 适配器配置、凭证引用、字段映射摘要、mock 测试 |
| ADM-OPS-001 | 生产 blocker 与证据 | `/admin/ops/readiness` | `GET/PATCH /api/v1/ops/production-readiness/items`、`GET /checks` | 证据登记、状态更新、Runbook/证据引用、过期提醒 |

## 4. 建议新增与修改文件

### 4.1 前端新增文件

| 文件 | 用途 |
|---|---|
| `apps/web/src/admin/AdminApp.tsx` | 完整后台应用壳、路由、菜单、角色上下文入口 |
| `apps/web/src/admin/adminRoutes.tsx` | 8 个 MVP 页面路由定义、页面 ID、权限元数据 |
| `apps/web/src/admin/types.ts` | 后台页面 DTO、角色、权限、状态、表格行类型 |
| `apps/web/src/admin/api/adminApi.ts` | 后台 API 客户端，集中封装 `/api/v1` 路径、错误归一化和 token |
| `apps/web/src/admin/security/PermissionGate.tsx` | 页面、按钮、字段级权限守卫，前端只做体验层拦截 |
| `apps/web/src/admin/components/AdminPageHeader.tsx` | 页面标题、状态、刷新、导出和主操作区 |
| `apps/web/src/admin/components/AdminDataState.tsx` | 加载、空、错误、部分失败、重试状态 |
| `apps/web/src/admin/components/AuditTrailHint.tsx` | 显示操作将产生的审计事件和 trace 入口 |
| `apps/web/src/admin/components/HighRiskConfirm.tsx` | 禁用、发布、核销、补偿、证据验证等二次确认 |
| `apps/web/src/admin/pages/OverviewPage.tsx` | `ADM-OV-001` |
| `apps/web/src/admin/pages/AccessUsersPage.tsx` | `ADM-ACC-001` |
| `apps/web/src/admin/pages/PermissionTemplatesPage.tsx` | `ADM-ACC-002` |
| `apps/web/src/admin/pages/TicketingPoliciesPage.tsx` | `ADM-TKT-001` |
| `apps/web/src/admin/pages/BarTableSessionsPage.tsx` | `ADM-BAR-001` |
| `apps/web/src/admin/pages/FinanceArApLedgerPage.tsx` | `ADM-FIN-001` |
| `apps/web/src/admin/pages/IntegrationAdaptersPage.tsx` | `ADM-INT-001` |
| `apps/web/src/admin/pages/OpsReadinessPage.tsx` | `ADM-OPS-001` |
| `apps/web/src/admin/__tests__/adminApi.test.ts` | API payload 归一化、错误归一化、权限元数据测试，如项目未配置单测则作为后续测试基座任务 |

### 4.2 前端修改文件

| 文件 | 修改点 |
|---|---|
| `apps/web/src/main.tsx` | 保留现有轻量入口，同时挂载 `/admin/*` 完整后台入口；逐步移出后台相关渲染和 API 调用 |
| `apps/web/src/styles.css` | 抽出后台布局、表格、窄屏、状态组件样式；避免继续堆页面私有样式 |
| `apps/web/vite.config.ts` | 若需要多入口或 chunk 策略，确保后台页面不会重新引入主包过大 warning |
| `tests/e2e/support/mock-api.ts` | 增加 8 个页面所需 fixture、错误、权限、空状态、重复提交和超时分支 |
| `tests/e2e/web-ui-acceptance.spec.ts` | 保留现有轻量入口验收，不把完整后台验收混入旧用例 |
| `playwright.config.ts` | 如新增截图基线，配置快照目录和移动端项目 |

### 4.3 测试新增文件

| 文件 | 用途 |
|---|---|
| `tests/e2e/full-admin-mvp.spec.ts` | 首批 8 页完整后台 Playwright 验收 |
| `tests/e2e/full-admin-permissions.spec.ts` | 角色矩阵、页面/按钮/字段权限拦截 |
| `tests/e2e/full-admin-mobile.spec.ts` | 390px 窄屏关键查看、审批、异常处理路径 |
| `tests/e2e/full-admin-real-backend.spec.ts` | 真实后端抽样，优先覆盖登录、总控、票务、卡座、财务、供应商、生产证据 |

### 4.4 后端与 OpenAPI

首批优先不新增后端域模型，只使用现有 OpenAPI。若编码中发现当前接口不能支撑页面字段或状态，必须先补 OpenAPI 和后端测试，再写前端调用。不得在前端临时拼接未登记路径。

## 5. 开发任务拆分

### Slice 0：后台壳与权限基座

目标：让 `/admin/*` 成为独立后台入口，具备导航、登录态、权限元数据、统一错误/空状态。

任务：

1. 新增 `AdminApp.tsx`、`adminRoutes.tsx`、`PermissionGate.tsx`、`AdminDataState.tsx`。
2. 在 `main.tsx` 中按路由挂载后台入口，保留现有轻量入口。
3. 为所有页面定义 `page_id`、`route`、`requiredPermissions`、`auditEvents`、`data-testid`。
4. 增加 mock 权限数据和权限不足状态。

验收：

- Given 未登录用户访问 `/admin/overview`，When 页面加载，Then 跳转或显示登录要求。
- Given 无 `overview.read` 的用户访问总控，When 直接输入 URL，Then 不展示总控数据。
- Given 任何后台 API 失败，When 页面渲染，Then 使用统一错误状态且保留重试动作。

### Slice 1：总控驾驶舱 ADM-OV-001

目标：提供完整后台总控首页，显示核心指标、异常摘要、待处理入口和部分失败状态。

任务：

1. 新增 `OverviewPage.tsx`。
2. 接入 `GET /api/v1/analytics/dashboards/p0-overview` 和审计/接口摘要数据。
3. 做时间范围筛选、综合体筛选、异常跳转。
4. 增加部分接口失败 mock。

验收：

- Given 总控账号登录，When 打开页面，Then 能看到票务、酒吧、游乐、财务、第三方、审计摘要。
- Given 财务摘要接口失败，When 页面加载，Then 其他卡片正常显示，财务卡显示错误和重试。
- Given 普通角色访问，When 页面渲染，Then 财务金额字段被隐藏或脱敏。

### Slice 2：账号权限 ADM-ACC-001 / ADM-ACC-002

目标：把员工账号和权限模板作为完整后台安全基座，而不是轻量表单。

任务：

1. 新增 `AccessUsersPage.tsx`、`PermissionTemplatesPage.tsx`。
2. 接入用户、角色、权限、权限模板、复制模板、临时授权路径。
3. 实现基础模板、复制模板、临时模板、单次有效和到期展示。
4. 增加禁用账号、同步失败重试、越权编辑和模板失效用例。

验收：

- Given IT 管理员复制模板，When 修改新模板，Then 原模板权限不变。
- Given 临时模板设置单次有效，When 使用一次后，Then 模板状态变为失效或不可再次使用。
- Given 员工被禁用，When 重新登录或刷新权限，Then 后台入口不可继续操作。

### Slice 3：票务策略 ADM-TKT-001

目标：实现平日、周末、节假日基础价优先，并支持多人票、新人票、学生票、渠道价的派生/固定价口径。

任务：

1. 新增 `TicketingPoliciesPage.tsx`。
2. 在 UI 中区分基础价、派生策略、固定价策略。
3. 使用现有 ticket-types、entitlements、publish 接口；动态定价只放 P2 入口和当前状态提示。
4. 增加发布前校验：基础价缺失、固定价与派生价互斥、渠道价缺渠道。

验收：

- Given 节假日价为空，When 发布依赖节假日价策略，Then 阻止发布并定位到节假日字段。
- Given 新人票选择派生策略，When 输入折扣或固定金额，Then 页面展示派生后价格预览。
- Given 固定价策略已填写，When 再输入派生折扣，Then 系统提示两者不可同时生效。

### Slice 4：卡座与订台 ADM-BAR-001

目标：让卡座预订、入场人数、跨酒吧限制、转卡、合并具备页面入口和可验收拦截。

任务：

1. 新增 `BarTableSessionsPage.tsx`。
2. 接入 tables、reservations、check-in-guest、orders 摘要。
3. 实现卡座状态、预订、入场人数、跨酒吧校验展示。
4. 转卡、合并首批可作为有权限按钮和弹窗流程；若后端接口未齐，必须以禁用状态说明缺口，不得假成功。

验收：

- Given 卡座允许 6 人，When 第 7 人登记入场，Then 页面拒绝并显示人数超限。
- Given 卡座属于酒吧 X，When 尝试登记酒吧 Y 入场，Then 页面拒绝并记录越权尝试提示。
- Given 卡座存在未结账订单，When 点击转卡，Then 弹窗要求先结账或主管审批。

### Slice 5：应收应付台账 ADM-FIN-001

目标：P0 财务主口径可在后台查看、生成、核销、调整、导出并留痕。

任务：

1. 新增 `FinanceArApLedgerPage.tsx`。
2. 接入 ar-ap-items、payments。
3. 实现方向、来源单据、交易对方、金额、币种、到期日、状态、已核销、余额展示。
4. 调整金额必须要求原因；导出按钮必须展示审计提示。

验收：

- Given 应收部分核销，When 查看详情，Then 原金额、已核销、剩余金额和核销明细正确。
- Given 财务调整金额未填写原因，When 保存，Then 阻止提交并定位原因字段。
- Given 用户导出台账，When 操作完成，Then 页面提示导出已留痕。

### Slice 6：供应商适配器 ADM-INT-001

目标：让第三方双向开放能力在后台可配置、可测试、可留痕。

任务：

1. 新增 `IntegrationAdaptersPage.tsx`。
2. 接入 provider-adapters、mark-production-ready、test-mock。
3. 字段映射、签名方式、Webhook、轮询间隔、凭证引用必须可见。
4. 密钥只允许 credential_ref，不允许明文展示和提交。

验收：

- Given 用户输入明文密钥，When 保存适配器，Then 页面拒绝并提示只能填写凭证引用。
- Given 字段映射缺必填字段，When 启用适配器，Then 阻止启用。
- Given mock 测试返回失败或超时，When 页面展示结果，Then 明确失败原因和重试入口。

### Slice 7：生产 blocker 与证据 ADM-OPS-001

目标：生产前置证据可在后台查看、登记、更新状态和追踪到期。

任务：

1. 新增 `OpsReadinessPage.tsx`。
2. 接入 production-readiness items、checks。
3. 展示 blocker、owner、due_at、status、evidence_ref、credential_ref、Runbook 链接。
4. 禁止保存真实密码、密钥、令牌；只允许保存引用。

验收：

- Given 证据缺失，When 用户尝试标记 verified，Then 系统拒绝并提示先登记 evidence_ref。
- Given 到期时间已过，When 页面加载，Then 显示逾期状态和责任人。
- Given 用户输入疑似密钥明文，When 保存，Then 页面拒绝并提示改用 credential_ref。

## 6. 数据结构与接口变化

首批前端类型建议统一放在 `apps/web/src/admin/types.ts`。

| 类型 | 关键字段 | 来源 |
|---|---|---|
| `AdminRouteDefinition` | `pageId`、`route`、`title`、`requiredPermissions`、`auditEvents`、`phase` | 页面矩阵 |
| `AdminUserAccount` | `id`、`username`、`accountType`、`status`、`organizationId`、`roles`、`dataScope` | `/api/v1/access/users` |
| `PermissionTemplateView` | `id`、`name`、`baseTemplateId`、`permissions`、`validUntil`、`singleUse`、`status` | `/api/v1/access/permission-templates` |
| `TicketPolicyDraft` | `weekdayPrice`、`weekendPrice`、`holidayPrice`、`ticketType`、`channelId`、`pricingMode` | ticket-types + UI 草稿 |
| `BarTableSessionView` | `barId`、`tableId`、`sessionId`、`guestLimit`、`checkedInCount`、`minSpend`、`status` | tables + reservations + orders |
| `FinanceArApLedgerView` | `sourceDoc`、`counterparty`、`direction`、`amount`、`currency`、`dueDate`、`paidAmount`、`balance`、`status` | ar-ap-items + payments |
| `ProviderAdapterView` | `provider`、`endpointKey`、`authType`、`credentialRef`、`mappingVersion`、`webhookEnabled`、`pollingInterval` | provider-adapters |
| `OpsReadinessItemView` | `blockerId`、`owner`、`dueAt`、`status`、`evidenceRef`、`credentialRef`、`runbookRef` | production-readiness |

接口变化原则：

1. 现有 OpenAPI 可满足的页面，不新增后端接口。
2. 页面矩阵中的 `/api/admin/...` 不直接编码，必须映射到 `/api/v1/...` 或先补 OpenAPI。
3. 任何新增路径都必须同步 `docs/openapi/openapi-p0.yaml`、operation matrix、closure appendix、mock API、E2E。
4. 前端 payload 字段必须通过既有 client payload policy 校验，不允许临时 camel/snake 混写。

## 7. UI 自动化验收计划

| 测试文件 | 覆盖内容 |
|---|---|
| `tests/e2e/full-admin-mvp.spec.ts` | 8 个页面 happy path、列表、表单、保存、错误、空状态 |
| `tests/e2e/full-admin-permissions.spec.ts` | SUPER_ADMIN、IT_ADMIN、FINANCE、BAR_MANAGER、普通员工的页面/按钮/字段权限 |
| `tests/e2e/full-admin-mobile.spec.ts` | 390px 窄屏下总控、卡座、财务、生产证据关键路径 |
| `tests/e2e/full-admin-real-backend.spec.ts` | 真实后端抽样：登录、总控、票务策略、卡座、应收应付、供应商、生产证据 |

最低断言要求：

1. 每页至少有页面标题、核心表格、筛选/主操作、空状态、错误状态断言。
2. 每个高风险操作至少有二次确认或审计提示断言。
3. 每个写操作至少断言请求 payload 和成功后页面状态变化。
4. 每个权限测试至少断言敏感按钮不可用，且直接调用后端写路径被拒绝或 mock 拦截为 403。
5. 每个移动端测试至少断言主按钮、状态、核心字段没有遮挡。

## 8. 边界场景清单

| 场景 | 预期处理 | 优先级 |
|---|---|---|
| 登录过期后提交表单 | 保留表单，提示重新登录，登录后可重试 | P0 |
| API 部分失败 | 页面局部错误，不清空已加载模块 | P0 |
| 无权限直达路由 | 不展示敏感数据，给出无权限状态 | P0 |
| 重复点击提交 | 前端禁用按钮，后端幂等，页面只显示一次成功 | P0 |
| 明文密钥输入 | 阻止保存，提示使用 credential_ref | P0 |
| 财务调整无原因 | 阻止保存，定位原因字段 | P0 |
| 卡座跨酒吧入场 | 拒绝并显示规则说明 | P0 |
| 卡座人数超过权益 | 拒绝并显示当前人数/上限 | P0 |
| 票务基础价缺失 | 阻止发布并定位缺失价格 | P0 |
| 临时权限过期 | 页面刷新权限并移除临时能力 | P0 |
| 生产证据缺失却标记 verified | 阻止状态流前进 | P0 |
| 窄屏表格字段过多 | 使用折叠详情或横向滚动，不遮挡主操作 | P1 |

## 9. 验证命令

首批开发完成后，至少运行：

```bash
npm run typecheck
npm run build
npm run verify:client-routes
npm run verify:client-openapi
npm run verify:client-payload
npm run verify:ui
npm run verify:ui:real-backend:seeded
npm run verify:p0-contract
npm run verify:delivery-gates
```

如果只完成单页切片，可先跑：

```bash
npm run typecheck
npm run build
npx playwright test tests/e2e/full-admin-mvp.spec.ts --project=chromium
```

## 10. 风险与回滚

| 风险 | 影响 | 规避 | 回滚方式 |
|---|---|---|---|
| 继续修改 `main.tsx` 大文件 | 页面互相影响，回归难 | 首批先拆 `apps/web/src/admin/*` | 可关闭 `/admin/*` 路由，保留旧轻量入口 |
| 矩阵接口与 OpenAPI 不一致 | 前端调用未入契约路径 | 以 OpenAPI `/api/v1` 为准，缺口先补契约 | 回滚新增 API 调用，保留只读页面 |
| 权限只做前端隐藏 | 安全验收失败 | 前端体验拦截 + 后端 403/mock 断言 | 回滚写操作，保留只读页面 |
| 高风险操作缺审计提示 | 运营不可追责 | 所有高风险按钮接 `HighRiskConfirm` 和 audit hint | 禁用高风险按钮直到审计事件补齐 |
| UI 自动化不稳定 | 阻塞交付 | 全部使用 `data-testid` 和 fixture | 临时隔离不稳定用例，但不得删除验收缺口记录 |

## 11. 交付完成定义

本开发包只有在以下条件全部满足后，才能标记为“首批完整后台 MVP 页面完成”：

1. 8 个页面均有独立路由、页面组件、数据加载、空/错/加载状态、权限状态。
2. 8 个页面均登记稳定 `data-testid`，并进入 Playwright 验收。
3. 账号权限、票务策略、卡座订台、应收应付、供应商适配器、生产证据至少有一个写操作或明确禁用原因。
4. 所有高风险操作有二次确认、审计提示或后端审计事件证据。
5. `npm run typecheck`、`npm run build`、`npm run verify:ui`、`npm run verify:p0-contract` 通过。
6. 若任何页面因后端接口缺口未完成，必须在实现矩阵中标记“接口缺口”并附 OpenAPI 修复任务。
