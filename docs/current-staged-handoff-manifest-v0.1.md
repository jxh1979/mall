# 当前暂存交接清单 V0.1

> 本文由 `tools/generate-staged-handoff-manifest.mjs` 从 160 机器当前 Git 索引生成，用于防止提交或补丁包交接时漏掉新增核心文件。它不宣称生产上线就绪。

机器标记：

- STAGED_HANDOFF_MANIFEST_READY
- NO_UNTRACKED_CORE_FILES
- NO_MIXED_STAGED_WORKTREE
- CRITICAL_SPLIT_SERVICES_INCLUDED
- PATCH_PACKAGE_INPUTS_TRACEABLE

## 1. 结论

| 项目 | 当前值 |
|---|---|
| 状态 | ready_for_commit_or_patch_package |
| 分支 | master |
| HEAD | d80f502 |
| 暂存文件数 | 657 |
| 未跟踪文件 | 0 |
| 混合/未暂存变更 | 0 |
| 缺失关键文件 | 0 |
| name-status 指纹 | `df2b97f9d880cca952ab0c8e39361cc6e4100dbbe652231b269186e4b2e0231a` |

## 2. 状态计数

| Git 状态 | 数量 |
| --- | ---: |
| A | 570 |
| M | 87 |

## 3. 顶层目录计数

| 目录 | 暂存文件数 |
| --- | ---: |
| security-scans | 373 |
| apps | 85 |
| docs | 70 |
| backend | 61 |
| tools | 46 |
| tests | 12 |
| ops | 5 |
| .gitignore | 1 |
| package-lock.json | 1 |
| package.json | 1 |
| playwright.config.ts | 1 |
| playwright.real-backend.config.ts | 1 |

## 4. 关键文件纳入检查

| 关键文件 | 状态 |
| --- | --- |
| backend/src/main/java/com/complex/platform/bar/BarEntryService.java | 已纳入索引 |
| backend/src/main/java/com/complex/platform/bar/BarOrderService.java | 已纳入索引 |
| backend/src/main/java/com/complex/platform/bar/BarTableSessionService.java | 已纳入索引 |
| backend/src/main/java/com/complex/platform/bar/BarBillingService.java | 已纳入索引 |
| backend/src/main/java/com/complex/platform/ticketing/TicketPricingService.java | 已纳入索引 |
| backend/src/main/java/com/complex/platform/ticketing/TicketOrderService.java | 已纳入索引 |
| backend/src/main/java/com/complex/platform/ticketing/TicketVerificationService.java | 已纳入索引 |
| backend/src/main/java/com/complex/platform/ticketing/TicketCatalogService.java | 已纳入索引 |
| backend/src/main/resources/db/migration/V041__ticket_order_dynamic_pricing_trace.sql | 已纳入索引 |
| apps/web/src/admin/AdminApp.tsx | 已纳入索引 |
| apps/web/src/admin/security/PermissionGate.test.ts | 已纳入索引 |
| tests/e2e/full-admin-mvp.spec.ts | 已纳入索引 |
| tests/e2e/full-admin-remaining.spec.ts | 已纳入索引 |
| tests/e2e/web-ui-acceptance.spec.ts | 已纳入索引 |
| tests/e2e/web-ui-real-backend.spec.ts | 已纳入索引 |
| playwright.real-backend.config.ts | 已纳入索引 |
| tools/run-real-backend-ui-e2e.mjs | 已纳入索引 |
| tools/generate-production-closure-gap-report.mjs | 已纳入索引 |
| security-scans/semgrep-latest-summary.json | 已纳入索引 |
| docs/runtime-acceptance-evidence-latest.json | 已纳入索引 |
| docs/closed-loop-current-state-snapshot-latest.json | 已纳入索引 |
| docs/production-evidence-closure-gap-report-v0.1.md | 已纳入索引 |

## 5. 暂存文件样例

| 状态 | 路径 |
| --- | --- |
| M | .gitignore |
| M | apps/android-terminal/README.md |
| M | apps/android-terminal/android-app/app/build.gradle.kts |
| M | apps/android-terminal/android-app/app/src/main/AndroidManifest.xml |
| M | apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt |
| M | apps/web/index.html |
| M | apps/web/package.json |
| A | apps/web/src/admin/AdminApp.tsx |
| A | apps/web/src/admin/adminPageConfigs.ts |
| A | apps/web/src/admin/adminRoutes.ts |
| A | apps/web/src/admin/client/adminClient.test.ts |
| A | apps/web/src/admin/client/adminClient.ts |
| A | apps/web/src/admin/components/AdminDataState.test.tsx |
| A | apps/web/src/admin/components/AdminDataState.tsx |
| A | apps/web/src/admin/components/AdminPageHeader.tsx |
| A | apps/web/src/admin/components/AuditTrailHint.tsx |
| A | apps/web/src/admin/components/HighRiskConfirm.tsx |
| A | apps/web/src/admin/pages/AccessUsersPage.tsx |
| A | apps/web/src/admin/pages/AdmissionAuditsPage.tsx |
| A | apps/web/src/admin/pages/AmusementGroupBookingsPage.tsx |
| A | apps/web/src/admin/pages/AmusementSafetyPage.tsx |
| A | apps/web/src/admin/pages/AmusementSessionsPage.tsx |
| A | apps/web/src/admin/pages/AuditEventsPage.tsx |
| A | apps/web/src/admin/pages/BarCommissionsPage.tsx |
| A | apps/web/src/admin/pages/BarOrdersPage.tsx |
| A | apps/web/src/admin/pages/BarTableSessionsPage.tsx |
| A | apps/web/src/admin/pages/BusinessAnalyticsPage.tsx |
| A | apps/web/src/admin/pages/ContractApprovalPage.tsx |
| A | apps/web/src/admin/pages/DynamicPricingPage.tsx |
| A | apps/web/src/admin/pages/FinanceArApLedgerPage.tsx |
| A | apps/web/src/admin/pages/GenericAdminPage.tsx |
| A | apps/web/src/admin/pages/GovernanceTopologyPage.tsx |
| A | apps/web/src/admin/pages/IntegrationAdaptersPage.tsx |
| A | apps/web/src/admin/pages/IntegrationEventsPage.tsx |
| A | apps/web/src/admin/pages/InventoryLedgerPage.tsx |
| A | apps/web/src/admin/pages/MerchantScopePage.tsx |
| A | apps/web/src/admin/pages/MerchantWorkOrdersPage.tsx |
| A | apps/web/src/admin/pages/OpsReadinessPage.tsx |
| A | apps/web/src/admin/pages/OrganizationMerchantPage.tsx |
| A | apps/web/src/admin/pages/OverviewPage.tsx |
| A | apps/web/src/admin/pages/PermissionTemplatesPage.tsx |
| A | apps/web/src/admin/pages/ReconciliationPage.tsx |
| A | apps/web/src/admin/pages/RiskAlertsPage.tsx |
| A | apps/web/src/admin/pages/StoredWinePage.tsx |
| A | apps/web/src/admin/pages/SubjectBindingsPage.tsx |
| A | apps/web/src/admin/pages/SupervisorReviewPage.tsx |
| A | apps/web/src/admin/pages/TicketOrdersPage.tsx |
| A | apps/web/src/admin/pages/TicketingPoliciesPage.tsx |
| A | apps/web/src/admin/security/PermissionGate.test.ts |
| A | apps/web/src/admin/security/PermissionGate.tsx |
| A | apps/web/src/admin/types.ts |
| A | apps/web/src/legacy-console/AuthShell.tsx |
| A | apps/web/src/legacy-console/LegacyAppShell.tsx |
| A | apps/web/src/legacy-console/LegacyConsoleApp.tsx |
| A | apps/web/src/legacy-console/LegacyConsoleContent.tsx |
| A | apps/web/src/legacy-console/amusementActions.ts |
| A | apps/web/src/legacy-console/apiClient.test.ts |
| A | apps/web/src/legacy-console/apiClient.ts |
| A | apps/web/src/legacy-console/barActions.ts |
| A | apps/web/src/legacy-console/floorPlanActions.ts |
| A | apps/web/src/legacy-console/formatters.test.ts |
| A | apps/web/src/legacy-console/formatters.ts |
| A | apps/web/src/legacy-console/foundationActions.ts |
| A | apps/web/src/legacy-console/pages/AmusementPage.tsx |
| A | apps/web/src/legacy-console/pages/AnalyticsReadOnlyPage.tsx |
| A | apps/web/src/legacy-console/pages/BarPage.tsx |
| A | apps/web/src/legacy-console/pages/EventsPage.tsx |
| A | apps/web/src/legacy-console/pages/FloorPlanPage.tsx |
| A | apps/web/src/legacy-console/pages/FoundationPage.tsx |
| A | apps/web/src/legacy-console/pages/InterfacesPage.tsx |
| A | apps/web/src/legacy-console/pages/SecurityPage.tsx |
| A | apps/web/src/legacy-console/pages/TicketingPage.tsx |
| A | apps/web/src/legacy-console/riskAndIntegrationActions.ts |
| A | apps/web/src/legacy-console/shell.tsx |
| A | apps/web/src/legacy-console/snapshotClient.ts |
| A | apps/web/src/legacy-console/stockMovementRequest.test.ts |
| A | apps/web/src/legacy-console/stockMovementRequest.ts |
| A | apps/web/src/legacy-console/ticketingActions.ts |
| A | apps/web/src/legacy-console/types.ts |
| A | apps/web/src/legacy-console/ui.tsx |
| M | apps/web/src/main.tsx |
| M | apps/web/src/styles.css |
| A | apps/web/src/test/setup.ts |
| M | apps/web/tsconfig.tsbuildinfo |
| M | apps/web/vite.config.ts |
| A | apps/web/vitest.config.ts |
| M | backend/pom.xml |
| A | backend/src/main/java/com/complex/platform/bar/BarAgeVerificationService.java |
| A | backend/src/main/java/com/complex/platform/bar/BarBillRecords.java |
| A | backend/src/main/java/com/complex/platform/bar/BarBillingService.java |
| A | backend/src/main/java/com/complex/platform/bar/BarEntryService.java |
| A | backend/src/main/java/com/complex/platform/bar/BarOperationExecutor.java |
| A | backend/src/main/java/com/complex/platform/bar/BarOrderService.java |
| A | backend/src/main/java/com/complex/platform/bar/BarReadOperation.java |
| M | backend/src/main/java/com/complex/platform/bar/BarService.java |
| A | backend/src/main/java/com/complex/platform/bar/BarTableSessionService.java |
| A | backend/src/main/java/com/complex/platform/bar/BarValidation.java |
| A | backend/src/main/java/com/complex/platform/bar/BarWriteOperation.java |
| A | backend/src/main/java/com/complex/platform/evidence/AuditedOperation.java |
| A | backend/src/main/java/com/complex/platform/evidence/AuditedOperationExecutor.java |
| M | backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java |
| M | backend/src/main/java/com/complex/platform/governance/MerchantRecord.java |
| M | backend/src/main/java/com/complex/platform/governance/PlatformService.java |
| M | backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java |
| A | backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewApprovalRequest.java |
| A | backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewController.java |
| A | backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewDecisionRecord.java |
| A | backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewEvaluateRequest.java |
| A | backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewPolicyRecord.java |
| A | backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java |
| A | backend/src/main/java/com/complex/platform/ticketing/DynamicPriceResult.java |
| M | backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java |
| A | backend/src/main/java/com/complex/platform/ticketing/TicketCatalogService.java |
| A | backend/src/main/java/com/complex/platform/ticketing/TicketCredentialService.java |
| A | backend/src/main/java/com/complex/platform/ticketing/TicketEntitlementMapper.java |
| A | backend/src/main/java/com/complex/platform/ticketing/TicketOfflinePackageService.java |
| M | backend/src/main/java/com/complex/platform/ticketing/TicketOrderRecord.java |
| A | backend/src/main/java/com/complex/platform/ticketing/TicketOrderService.java |
| A | backend/src/main/java/com/complex/platform/ticketing/TicketPricingService.java |
| A | backend/src/main/java/com/complex/platform/ticketing/TicketRefundService.java |
| A | backend/src/main/java/com/complex/platform/ticketing/TicketVerificationService.java |
| A | backend/src/main/java/com/complex/platform/ticketing/TicketingOperationExecutor.java |
| A | backend/src/main/java/com/complex/platform/ticketing/TicketingReadOperation.java |
| M | backend/src/main/java/com/complex/platform/ticketing/TicketingService.java |
| A | backend/src/main/java/com/complex/platform/ticketing/TicketingWriteOperation.java |
| M | backend/src/main/java/com/complex/platform/ticketing/dto/TicketOrderRequest.java |
| A | backend/src/main/resources/db/migration/V041__ticket_order_dynamic_pricing_trace.sql |
| A | backend/src/main/resources/db/migration/V042__merchant_profile_extended_fields.sql |
| A | backend/src/main/resources/db/migration/V043__supervisor_review_permissions.sql |
| M | backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java |
| M | backend/src/test/java/com/complex/platform/amusement/AmusementSafetyJdbcIntegrationTests.java |
| M | backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java |
| M | backend/src/test/java/com/complex/platform/bar/BarJdbcIntegrationTests.java |
| M | backend/src/test/java/com/complex/platform/bar/BarServiceTests.java |
| M | backend/src/test/java/com/complex/platform/compliance/ComplianceJdbcIntegrationTests.java |
| M | backend/src/test/java/com/complex/platform/customer/CustomerJdbcIntegrationTests.java |
| M | backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java |
| M | backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java |
| M | backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java |
| M | backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java |
| A | backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewControllerTests.java |
| A | backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java |
| M | backend/src/test/java/com/complex/platform/integration/IntegrationJdbcIntegrationTests.java |
| M | backend/src/test/java/com/complex/platform/inventory/InventoryJdbcIntegrationTests.java |
| M | backend/src/test/java/com/complex/platform/merchantbilling/MerchantBillingJdbcIntegrationTests.java |
| M | backend/src/test/java/com/complex/platform/merchantservice/MerchantServiceJdbcIntegrationTests.java |
| M | backend/src/test/java/com/complex/platform/ticketing/TicketingServiceTests.java |
| M | docs/00-current-effective-document-entry-v0.1.md |
| A | docs/closed-loop-current-state-snapshot-latest.json |
| A | docs/closed-loop-current-state-snapshot-v0.1.md |
| A | docs/closed-loop-implementation-ui-acceptance-report-v0.1.md |
| A | docs/code-implementation-acceptance-audit-v0.1.md |
| A | docs/codebase-architecture-zoom-out-audit-v0.1.md |
| A | docs/current-staged-handoff-manifest-latest.json |
| A | docs/current-staged-handoff-manifest-v0.1.md |
| M | docs/delivery-package-manifest-v0.1.md |
| M | docs/delivery-readiness-gates-v0.1.md |
| A | docs/development-documentation-adversarial-audit-v0.1.md |
| A | docs/development-documentation-audit-map-v0.1.csv |
| A | docs/development-ready-spec-draft-v0.1.md |
|  | ... 497 more |

## 6. 交接规则

1. 交接前必须运行 `npm run verify:staged-handoff-manifest`。
2. 不允许存在 `??` 未跟踪文件；新增服务类、迁移、Playwright 测试、运行证据和安全扫描必须进入索引。
3. 不允许存在混合状态；同一文件如果还有未暂存修改，必须重新生成清单并暂存。
4. 本清单只证明补丁包输入完整，不替代 `npm run verify:delivery-gates`、`npm run verify:closed-loop-runtime`、Semgrep 或生产证据回填。
