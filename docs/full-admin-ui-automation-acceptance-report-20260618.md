# 完整后台管理端 UI 自动化验收报告 2026-06-18

> 性质：前端 UI goal 模式实测报告。所有"通过"均为本轮在 `/home/james/complex-management` 当前工作树实际执行命令的输出。
> 范围：新完整后台 `AdminApp`（`/admin/*`，30 个页面）。`/` 的 legacy console 不在本轮目标内。
> 方法：读页面矩阵/路由/组件/测试 → 建 UI 验收矩阵 → 4 个并行子代理逐页审计 → 按清单修复 → TDD 补 Playwright 真实断言 → 复跑 typecheck/build/vitest/playwright。

## 1. 基线（修复前实测）

| 闸口 | 结果 |
|---|---|
| `npm run typecheck` | exit 0 |
| `npm run build --workspace apps/web` | exit 0，最大 chunk `vendor-antd` 452 kB(<500kB)，**无主包过大 warning** |
| `npm run test:unit`（Vitest） | 16 passed |
| `npx playwright test` | **27 passed / 1 failed**（`ADM-VIS`+`ADM-REM` 等 28 用例）|
| 失败项 | `ADM-REM-01`（遍历全部 22 页的最重测试）登录点击在并发负载下超过 30s 超时 |

需求依据：`docs/full-admin-page-permission-field-matrix-v0.1.csv`（page_id/key_fields/primary_actions/validation/audit_events）、`docs/full-admin-ui-acceptance-matrix-v0.1.md`、`apps/web/src/admin/adminRoutes.ts`（30 路由）。

## 2. 审计发现（4 子代理逐页核查 30 页）

| 类别 | 发现 |
|---|---|
| **共享状态无 testid（最高杠杆）** | `AdminDataState` 的 loading/error/empty 三个分支只有 CSS class、无 `data-testid`，约 20 个页面的加载/错误/空态无法被 Playwright 稳定选中 |
| **裸表格（无 `<thead>`、无横向滚动容器、无 testid）** | `AccessUsersPage`、`PermissionTemplatesPage`、`BarTableSessionsPage`、`FinanceArApLedgerPage`、`IntegrationAdaptersPage`、`OpsReadinessPage` —— 窄屏溢出风险、列无表头 |
| **页面根缺 `data-testid`** | 上述 6 页 + `TicketingPoliciesPage` |
| **金额/价格无数值校验** | `TicketingPoliciesPage`（平日/周末/节假日价可为 0/负/NaN）、`FinanceArApLedgerPage`（核销金额）、`DynamicPricingPage`（调整幅度）|
| **矩阵要求但缺失的功能** | `OverviewPage` 缺 `date_range` 时间筛选（矩阵 primary_actions: 筛选时间）|
| **硬编码/语义弱的动作** | `PermissionTemplatesPage` 复制/应用硬编码 `template-basic`/`user-1`，忽略数据行 |
| **高量日志页缺筛选/分页** | IntegrationEvents/RiskAlerts/AuditEvents/TicketOrders（P2，本轮记录未全改）|
| **重复错误渲染（P3）** | AuditEvents/BusinessAnalytics/MerchantScope 同时渲染 inline error + AdminDataState error |
| **已核查良好** | 多数治理/财务/游乐页已有完善 testid、thead+滚动、空错加载态、行选择、必填校验、审计结果可追踪；移动端 CSS（≤720px 单列壳、横向导航滚动、KPI 堆叠、表格 overflow-x）完善 |

## 3. 本轮修复（均已 TDD 验证）

| 编号 | 模块/文件 | 修复 |
|---|---|---|
| U-01 | `components/AdminDataState.tsx` | loading/error/empty + 重试按钮加 `data-testid`（`admin-loading`/`admin-error`/`admin-empty`/`admin-retry`），一处修复覆盖全部使用方；强化单测断言 testid |
| U-02 | `pages/GenericAdminPage.tsx` | loading 态加 `admin-generic-loading` testid |
| U-03 | `AccessUsersPage` | 根 testid + 表格 `<thead>`+`admin-table-scroll`+`access-users-table` testid |
| U-04 | `PermissionTemplatesPage` | 根/表格 testid + thead+滚动；复制/应用改为基于真实首行模板 id（不再硬编码），无数据时按钮禁用 |
| U-05 | `BarTableSessionsPage` | 根/表格 testid + thead+滚动；入场人数改 `type=number min=1` |
| U-06 | `FinanceArApLedgerPage` | 根 testid + thead+滚动；核销金额加"必须大于 0"数值校验 + `type=number` |
| U-07 | `IntegrationAdaptersPage` | 根/表格 testid + thead+滚动 |
| U-08 | `OpsReadinessPage` | 表格 testid + thead+滚动 |
| U-09 | `TicketingPoliciesPage` | 根 testid + 三档价格数值校验（>0）+ 发布按钮 busy 禁用 + 网络错误 try/catch |
| U-10 | `OverviewPage` | 新增 `date_range` 时间筛选（起始/结束/筛选时间，重新拉取仪表盘）+ 6 张 KPI 卡 testid |
| U-11 | `tests/e2e/full-admin-remaining.spec.ts` | `ADM-REM-01`/`ADM-REM-02` 加 `test.slow()`（最重的全页遍历测试，并发下三倍超时），修复基线唯一失败项 |
| U-12 | `tests/e2e/full-admin-visual.spec.ts-snapshots/` | 因 Overview 新增筛选行，回归基线截图（已人工查验：筛选行对齐、KPI 3×2 网格整齐、无溢出/空白）|
| U-13 | `components/rowSearch.ts`（新）+ `RiskAlertsPage`/`IntegrationEventsPage`/`TicketOrdersPage` | 高量日志页补客户端搜索筛选：抽出共享 `rowMatchesKeyword`，三页加搜索框 + 匹配计数 + 独立 filter-empty 态（与 data-empty 区分）；AuditEvents 原已有搜索 |
| U-14 | `components/TablePager.tsx`（新）+ 风控/集成事件/出票/审计四页 + `styles.css` + `tests/e2e/support/mock-api.ts` | 高量日志页补客户端**分页**：抽出共享 `usePagedRows`+`TablePager`（上一页/页码状态/下一页，testid 全覆盖），先筛选后分页、改关键词自动回第 1 页；扩充 mock 至每接口 12 行（命名原始行保持首位，确保 RT-1/CP-1/SYNC-1/VCH-1、TO-MOCK-1、Mock risk、event-trace-1 留在第 1 页，现有断言不破）；补 `.admin-pager` 样式 |
| U-15 | `AuditEventsPage`/`BusinessAnalyticsPage`/`MerchantScopePage` + `full-admin-remaining.spec.ts` | 清理 3 处重复错误渲染：删除各页 `<AdminDataState>` 之前的 inline error div（`*-error`），统一由 `AdminDataState` 的 `admin-error`（含重试）呈现；同步将 ADM-REM-03 对 `audit-events-error` 的断言改为 `admin-error` |

## 4. 新增 Playwright 测试（真实断言）

`tests/e2e/full-admin-ui-hardening.spec.ts`（6 用例，全部通过）：
- **ADM-UI-HARDEN-01**：6 个重构页根 testid 可见、表格 `<thead>` 含正确表头文案、页面无横向溢出（scrollWidth-clientWidth ≤ 80）。
- **ADM-UI-HARDEN-02**：空数据 → `admin-empty` 可见；错误（errorPath mock）→ `admin-error` 含"读取失败"且 `admin-retry` 可见。
- **ADM-UI-HARDEN-03**：Overview 填起止日期、点"筛选时间" → `overview-date-applied` 含所选区间；KPI testid 可寻址且筛选后仍渲染。
- **ADM-UI-HARDEN-04**：票务价格填 0 → `ticket-policy-error` 含"平日价"且**未触发任何写动作**（`admin-action-result` count 0）；财务核销金额填 0 → `finance-ledger-error` 含"大于 0"。
- **ADM-UI-HARDEN-05**：风控/集成事件/出票三页搜索框——非匹配关键词 → `*-filter-empty` 可见且表格 count 0；匹配关键词（`staff_behavior`/`mock`/`TO-MOCK`）→ 表格恢复并含目标行。
- **ADM-UI-HARDEN-06**：四页分页——风控页 12 行/页大小 10 → 显示"第 1/2 页 共 12 条"，命名行在第 1 页、上一页禁用；点下一页 → 第 2/2 页、下一页禁用、出现 Filler 行；搜索后回"第 1/1 页 共 1 条"；集成事件 RT-1/CP-1/SYNC-1/VCH-1 全在第 1 页；出票点下一页见 `TO-FILL`；审计共 12 条且 `event-trace-1` 留第 1 页。

## 5. 修复后复跑（全绿）

| 闸口 | 结果 |
|---|---|
| `npm run typecheck` | exit 0 |
| `npm run build --workspace apps/web` | exit 0，无主包过大 warning |
| `npm run test:unit`（Vitest） | **16 passed** |
| `npx playwright test` | **34 passed / 0 failed**（28→34：+6 新用例，原 `ADM-REM-01` 失败已修，视觉基线已更新）|

## 6. UI 验收矩阵（核心路径，全部 Playwright 覆盖且有真实断言）

| 验收 ID | 页面 | 覆盖断言（节选） | 测试 |
|---|---|---|---|
| AUTH | 登录/退出 | 正确登录进壳、失败留登录页且显示后端原因、退出回登录 | web-ui-acceptance P0-UI-01/09 |
| 权限拦截 | 受限路由 | 无权角色 `admin-permission-denied` 含"权限不足" | full-admin-remaining ADM-REM-04 |
| OV-001 | 总控驾驶舱 | KPI testid 可寻址、时间筛选可操作 | hardening 03 |
| ACC-001/002 | 账号/权限模板 | 根+表头表格、空/错/重试态 | hardening 01/02 |
| TKT-001 | 票务策略 | 缺价不可发布、非正价被拦 | hardening 04、ADM-MVP-03 |
| FIN-001 | 应收应付台账 | 金额>0 校验、表头表格 | hardening 01/04、ADM-MVP-05 |
| BAR/AMU/INT/RISK/EVD/ANA/MER 等 | 各业务页 | 路由、证据、列表、动作可审计、空/错/移动端 | full-admin-mvp/remaining、web-ui-acceptance |

## 7. 结论与移交

- 后台 30 页可登录、可路由、核心查询/动作可用、空/错/加载/移动端状态可见且可被 Playwright 稳定断言；金额类输入有数值校验；总控时间筛选补齐。
- typecheck/build/vitest/playwright 全绿，无主包过大 warning，无文字溢出/裸表格/空白页/不可点按钮的阻断问题。
- **续修（U-13/U-14）**：高量日志页（风控/集成事件/出票/审计）已补客户端**搜索筛选 + 分页**——抽出共享 `rowMatchesKeyword`/`usePagedRows`/`TablePager`，扩充 mock 至每接口 12 行并保命名行于首位；扩充后 legacy 总控仪表盘"待处理风险"计数随之变化，已回归其 2 张视觉基线（人工查验无破版）。
- **续修（U-15）**：3 处重复错误渲染（审计/经营分析/商户授权）已清理，统一由 `AdminDataState` 的 `admin-error` 呈现。
- **未在本轮处理（记录移交，非阻断）**：部分页 `key_fields` 仍为固定验收 payload 而非可编辑表单、若干"完整 CRUD/转卡-合并-续卡-回滚"动作仍为验收占位——这些属功能增强，需产品确认信息架构后再扩展。
