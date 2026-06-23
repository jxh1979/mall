# 代码库架构 Zoom-out 审计 V0.1

> 审计日期：2026-06-17
> 执行位置：192.168.88.160 `/home/james/complex-management`
> 目的：从长期维护角度检查模块边界、抽象深度、重复逻辑、领域语言和后续演进风险。

## 1. 当前架构结论

当前代码库已经从“单文件/大 facade 交付”进入“可维护分层交付”状态：正式技术栈为 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway，前端为 Vite/React/Ant Design，旧 TypeScript/SQLite 原型路径已被交付闸口阻断。

## 2. 模块边界现状

| 区域 | 当前边界 | 状态 | 后续建议 |
|---|---|---|---|
| Web 入口 | `apps/web/src/main.tsx` 仅负责 admin/customer 路由分流（legacy 控制台 2026-06-23 退役删除） | 良好 | 保持入口薄层，不再塞业务逻辑 |
| 完整后台 | `AdminApp` + `adminRoutes` + `adminPageConfigs` + `GenericAdminPage` + `DynamicPricingPage` + `TicketOrdersPage` + `BarOrdersPage` + `StoredWinePage` + `BarCommissionsPage` + `InventoryLedgerPage` + `ReconciliationPage` + `MerchantWorkOrdersPage` + `MerchantScopePage` + `AmusementSafetyPage` + `SupervisorReviewPage` + `SubjectBindingsPage` + `ContractApprovalPage` + `RiskAlertsPage` + `IntegrationEventsPage` + `AuditEventsPage` + `BusinessAnalyticsPage` + `AdmissionAuditsPage` + `GovernanceTopologyPage` + `OrganizationMerchantPage` + `AmusementSessionsPage` + `AmusementGroupBookingsPage` | 可接受 | 关键高频页面逐步从 Generic 页面升级为专用页面 |
| 票务后端 | `TicketingService` facade + pricing/order/verification/refund/catalog 子服务 | 良好 | 保持 facade 只做事务入口和幂等装配 |
| 酒吧后端 | `BarService` facade + billing/order/session/entry/age 子服务 + `BarValidation`/`BarBillRecords` 公共辅助 | 良好 | 公共校验、账单记录重建、审计快照和 ID slug 已集中；主要酒吧子服务均低于 400 行 |
| 第三方集成后端 | `IntegrationService` + `OpenPlatformCrypto` + `IntegrationValidation` | 可接受 | 开放平台签名/哈希/token 与适配器/对账校验已从服务编排中拆出；后续再按 adapter/order-sync/reconciliation 子服务继续瘦身 |
| 生产证据 | production evidence 文档、工具和 admin ops 页面 | 良好 | 真实证据回填后复跑交付闸口 |

## 3. 已关闭的主要架构风险

| 风险编号 | 原问题 | 当前处理 |
|---|---|---|
| ARCH-P1-001 | 完整后台页面矩阵与实现不一致 | `verify:full-admin-matrix` 已纳入交付闸口，30 个页面均有路由、权限、UI 与测试覆盖 |
| ARCH-P1-002 | Web 主入口过大且混合 DTO/API/页面/动作 | `main.tsx` 已降为 31 行；legacy 控制台已拆出 DTO、API client、snapshot、页面、动作模块 |
| ARCH-P1-003 | P0/P1/P2 文档和代码缺少闭环证据 | `requirements-code-implementation-matrix-v0.1.csv`、最终闭环验收报告和交付闸口已建立证据链 |
| ARCH-P2-001 | 票务/酒吧大 facade 难维护 | `TicketingService` 332 行、`BarService` 356 行，主要业务已进入子服务 |
| ARCH-P2-002 | Web 构建主包过大 warning | Admin 入口懒加载、vendor 分包后，主包约 108 kB，无 warning |
| ARCH-P2-003 | 安全扫描未闭环 | Semgrep 已执行，latest summary results=0 |
| ARCH-P2-004 | `IntegrationService` 同时承担开放平台加密、适配器校验和业务编排 | 已拆出 `OpenPlatformCrypto` 70 行、`IntegrationValidation` 246 行；`IntegrationService` 从 1306 行降至 1011 行，目标测试和全后端测试通过 |

## 4. 当前剩余架构风险

| 风险编号 | 等级 | 位置 | 说明 | 建议 |
|---|---|---|---|---|
| ARCH-P2-004 | 已关闭 | `IntegrationService` 1011 行、`OpenPlatformCrypto` 70 行、`IntegrationValidation` 246 行 | 开放平台签名/哈希/token 与校验规则已拆出，服务类减少约 295 行；剩余仍偏大但已从“工具混杂”降为“业务编排偏厚” | 下一步按 adapter/order-sync/reconciliation 三条业务线拆子服务，不再把签名或通用校验塞回 facade |
| ARCH-P2-005 | 已关闭 | `BarBillingService` 323 行、`BarTableSessionService` 387 行、`BarOrderService` 383 行、`BarValidation` 132 行、`BarBillRecords` 107 行 | 公共校验、账单记录重建、金额/数量边界和 JSON 证据辅助已集中，主要子服务均降至 400 行内 | 后续仅按业务边界继续演进，不再为行数强拆 |
| ARCH-P2-006 | P2 | `GenericAdminPage` + 专用后台页面集 | 当前页面矩阵已无业务页面依赖配置页，业务深交互表达有限 | 保留 Generic 页仅作为未知路由/未来低频页面兜底，新增业务页面默认优先专用组件 |
| ARCH-P2-007 | P2 | 第三方/生产证据需求 | 真实供应商、真实凭证、真实视频/人脸/POS 证据未到位 | 继续用 production evidence 台账管理，不伪造完成 |
| ARCH-P3-001 | 已关闭 | `ops/160/complex-backup.sh`、`ops/160/complex-restore-drill.sh` | 已将权限模式检查和恢复报告写入逻辑改为 Semgrep 可解析写法；全量 Semgrep 不再排除这两个 Bash 脚本 | 继续保留 `bash -n` 作为运维脚本基础语法闸口 |

## 5. 分步改善计划

1. 短期：`IntegrationService` 已完成第一刀拆分；继续为 adapter/order-sync/reconciliation 三条业务线补 characterization tests，再分离 `ProviderAdapterService`、`IntegrationOrderSyncService`、`IntegrationReconciliationService`。
2. 短期：`BarBillingService` 行数风险已关闭；继续为账单调整、低消补差、挂账、提成计算补 characterization tests，防止后续业务规则扩展时回潮。
3. 中期：只有当账单调整、拆合账单、提成计算出现独立变化节奏时，再拆 `BarBillAdjustmentService`、`BarBillOperationService`、`BarCommissionService`，避免浅模块化。
4. 中期：把 `BarTableSessionService` 中的会话状态、卡座转卡/合并/续卡规则和入场名额校验继续下沉到策略类。
5. 中期：ADM-TKT-002、ADM-TKT-003、ADM-BAR-002、ADM-BAR-003、ADM-BAR-004、ADM-INV-001、ADM-FIN-002、ADM-MER-001、ADM-MER-002、ADM-AMU-003、ADM-GOV-006、ADM-GOV-001、ADM-GOV-002、ADM-GOV-003、ADM-GOV-004、ADM-GOV-005、ADM-RISK-001、ADM-INT-002、ADM-EVD-001、ADM-ANA-001 已升级为专用组件；完整后台当前矩阵页面已全部退出 `GenericAdminPage`；后续新增页面默认以专用组件承载复杂状态流。
6. 生产前：真实回填供应商、支付/POS、人脸/视频、NAS/MDM/凭证轮换证据，复跑交付闸口、Semgrep 和真实后端 UI 验收。

## 6. 验证依据

- `npm run verify:delivery-gates` 通过。
- `npm run verify:ui` 通过，28 条 Playwright 验收。
- `npm run build --workspace apps/web` 通过，无主包过大 warning。
- `security-scans/semgrep-latest-summary.json` 显示 Semgrep results=0、errors=0、paths_scanned=1543，最新 scan_dir=`semgrep-20260617-064855-manual-request`；`excluded_from_semgrep_due_to_bash_parser_warnings=[]`，全量扫描不再排除 ops/160 Bash 脚本。
- `git diff --cached --check` 通过。
