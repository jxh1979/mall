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
| 状态 | not_ready_for_commit_or_patch_package |
| 分支 | feat/business-html-audit-and-coding-closure |
| HEAD | 910f8e7 |
| 暂存文件数 | 0 |
| 未跟踪文件 | 2 |
| 混合/未暂存变更 | 56 |
| 缺失关键文件 | 21 |
| name-status 指纹 | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` |

## 2. 状态计数

| Git 状态 | 数量 |
| --- | ---: |

## 3. 顶层目录计数

| 目录 | 暂存文件数 |
| --- | ---: |

## 4. 关键文件纳入检查

| 关键文件 | 状态 |
| --- | --- |
| backend/src/main/java/com/complex/platform/bar/BarEntryService.java | 缺失 |
| backend/src/main/java/com/complex/platform/bar/BarOrderService.java | 缺失 |
| backend/src/main/java/com/complex/platform/bar/BarTableSessionService.java | 缺失 |
| backend/src/main/java/com/complex/platform/bar/BarBillingService.java | 缺失 |
| backend/src/main/java/com/complex/platform/ticketing/TicketPricingService.java | 缺失 |
| backend/src/main/java/com/complex/platform/ticketing/TicketOrderService.java | 缺失 |
| backend/src/main/java/com/complex/platform/ticketing/TicketVerificationService.java | 缺失 |
| backend/src/main/java/com/complex/platform/ticketing/TicketCatalogService.java | 缺失 |
| backend/src/main/resources/db/migration/V041__ticket_order_dynamic_pricing_trace.sql | 缺失 |
| apps/web/src/admin/AdminApp.tsx | 缺失 |
| apps/web/src/admin/security/PermissionGate.test.ts | 缺失 |
| tests/e2e/full-admin-mvp.spec.ts | 缺失 |
| tests/e2e/full-admin-remaining.spec.ts | 缺失 |
| tests/e2e/web-ui-real-backend.spec.ts | 缺失 |
| playwright.real-backend.config.ts | 缺失 |
| tools/run-real-backend-ui-e2e.mjs | 缺失 |
| tools/generate-production-closure-gap-report.mjs | 缺失 |
| security-scans/semgrep-latest-summary.json | 缺失 |
| docs/runtime-acceptance-evidence-latest.json | 缺失 |
| docs/closed-loop-current-state-snapshot-latest.json | 缺失 |
| docs/production-evidence-closure-gap-report-v0.1.md | 缺失 |

## 5. 暂存文件样例

| 状态 | 路径 |
| --- | --- |

## 6. 交接规则

1. 交接前必须运行 `npm run verify:staged-handoff-manifest`。
2. 不允许存在 `??` 未跟踪文件；新增服务类、迁移、Playwright 测试、运行证据和安全扫描必须进入索引。
3. 不允许存在混合状态；同一文件如果还有未暂存修改，必须重新生成清单并暂存。
4. 本清单只证明补丁包输入完整，不替代 `npm run verify:delivery-gates`、`npm run verify:closed-loop-runtime`、Semgrep 或生产证据回填。
