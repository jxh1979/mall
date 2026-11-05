# 闭环实现与前端体验验收报告 V0.1

> 本文由 `tools/generate-ui-experience-acceptance-report.mjs` 从运行态验收证据、闭环当前态快照、Semgrep 摘要、Playwright spec 和截图基线生成。它用于证明前端体验验收报告没有漂移，不替代真实生产证据回填。

机器标记：

- UI_EXPERIENCE_ACCEPTANCE_REPORT_READY
- GENERATED_FROM_RUNTIME_ACCEPTANCE_EVIDENCE
- GENERATED_FROM_PLAYWRIGHT_SPECS_AND_SNAPSHOTS
- REAL_BACKEND_UI_INCLUDED
- VISUAL_REGRESSION_BASELINES_TRACKED
- NO_STALE_UI_ACCEPTANCE_COUNTS

更新时间：2026-11-05T05:12:57.130Z
执行位置：192.168.88.160，`/home/james/complex-management`

## 1. 本轮闭环结论

当前项目的 Web 管理端、完整后台首批/非首批页面、真实后端 UI 抽样、视觉回归和运行态证据的验收状态见下表与第 8 节；报告中的数字来自机器证据，不再保留旧口径。

| 项目 | 当前值 | 证据 |
|---|---:|---|
| 需求矩阵完整实现 | 88/97 | `docs/closed-loop-current-state-snapshot-latest.json` |
| 完整后台 MVP | 9/9 | `docs/full-admin-mvp-requirements-code-matrix-v0.1.csv` |
| 完整后台非首批 | 22/22 | `docs/full-admin-remaining-requirements-code-matrix-v0.1.csv` |
| 后端测试 | 6347 | `backend/target/surefire-reports` |
| Web 单元测试定义 | 1584 | `apps/web/src/**/*.test.*` |
| Playwright 常规 UI | 47 | `playwright-report/index.html` |
| Playwright 真实后端 UI | 3（报告缺失，未跑） | `playwright-report-real-backend/index.html` |
| 视觉回归截图基线 | 4 | `tests/e2e/**/*-snapshots/*.png` |
| Semgrep | results=0, errors=0, paths_scanned=1984 | `semgrep-20260703-delivery-gates-clean` |
| 可进入联调 | 否 | `closed-loop-current-state-snapshot-latest.json` |
| 可进入业务验收 | 否 | `closed-loop-current-state-snapshot-latest.json` |
| 可宣称生产上线就绪 | 否 | 生产证据与 NAS 当前状态 |

## 2. 前端构建与包体

| 项目 | 当前值 |
|---|---:|
| main JS | apps/web/dist/assets/main-CE9C2Z6z.js，4337 bytes |
| largest asset | apps/web/dist/assets/vendor-antd-CzvkKKQu.js，251112 bytes |
| main bundle warning | 不预期出现 |
| dist index | 存在 |
| route indexes | 3/3 |

## 3. UI 自动化验收矩阵

| 类型 | 测试路径 | 证据文件 |
| --- | --- | --- |
| 常规 UI | CUS-LIFE-01 封禁→拒动作拒登录→恢复→拉黑永久拒→自助注销后数据保留 | tests/e2e/customer-account-lifecycle-live-evidence.spec.ts |
| 常规 UI | CUS-LIFE-02 ADM-CUS-001 客人账号管理页在真实后端查询并截图 | tests/e2e/customer-account-lifecycle-live-evidence.spec.ts |
| 常规 UI | CUST-01 phone login then logout returns to login view | tests/e2e/customer-journey.spec.ts |
| 常规 UI | CUST-02 buy ticket creates order and shows payment unavailable when no gateway is configured | tests/e2e/customer-journey.spec.ts |
| 常规 UI | CUST-03 bar reservation shows table rules then deposit payment is unavailable | tests/e2e/customer-journey.spec.ts |
| 常规 UI | CUST-04 points page shows balance/level and check-in accrues streak | tests/e2e/customer-journey.spec.ts |
| 常规 UI | CUST-05 wallet shows per-card stored value balances | tests/e2e/customer-journey.spec.ts |
| 常规 UI | CUST-06 my orders reflect a freshly created ticket order | tests/e2e/customer-journey.spec.ts |
| 常规 UI | CUST-07 buy ticket surfaces backend rejection as a visible error | tests/e2e/customer-journey.spec.ts |
| 常规 UI | ADM-MVP-01 login, overview route and eight MVP navigation entries | tests/e2e/full-admin-mvp.spec.ts |
| 常规 UI | ADM-MVP-02 account and permission template workflows are actionable | tests/e2e/full-admin-mvp.spec.ts |
| 常规 UI | ADM-MVP-03 ticketing policy validates weekday/weekend/holiday base price before publish | tests/e2e/full-admin-mvp.spec.ts |
| 常规 UI | ADM-MVP-04 bar table session transfer and merge | tests/e2e/full-admin-mvp.spec.ts |
| 常规 UI | ADM-MVP-05 finance ledger requires adjustment reason and supports partial payment | tests/e2e/full-admin-mvp.spec.ts |
| 常规 UI | ADM-MVP-06 integration adapter requires provider code, type and mode and supports mock test | tests/e2e/full-admin-mvp.spec.ts |
| 常规 UI | ADM-MVP-07 ops readiness cannot verify blocker without evidence ref | tests/e2e/full-admin-mvp.spec.ts |
| 常规 UI | ADM-MVP-08 role permission and mobile layout are enforceable | tests/e2e/full-admin-mvp.spec.ts |
| 常规 UI | ADM-MVP-09 failed login surfaces business wording and stays on auth screen | tests/e2e/full-admin-mvp.spec.ts |
| 常规 UI | ADM-MVP-10 forbidden backend access surfaces business wording without hiding the audit trail | tests/e2e/full-admin-mvp.spec.ts |
| 常规 UI | ADM-STF-003 displays the complete tax version and real draft form | tests/e2e/full-admin-remaining.spec.ts |
| 常规 UI | ADM-AMU-008 submits future business day configuration for approval | tests/e2e/full-admin-remaining.spec.ts |
| 常规 UI | ADM-INV-003 separates qualified receipt and rejection with actual upload bytes | tests/e2e/full-admin-remaining.spec.ts |
| 常规 UI | ADM-FAC-002 ADM-FAC-003 review and acceptance send real material version | tests/e2e/full-admin-remaining.spec.ts |
| 常规 UI | ADM-FIN-009 reviews the whole recovery amount before a finance waiver | tests/e2e/full-admin-remaining.spec.ts |
| 常规 UI | ADM-TKT-004 ADM-FIN-008 config approval and bill objection remain distinct | tests/e2e/full-admin-remaining.spec.ts |
| 常规 UI | NAV-GOV-01 主体绑定页展示真实字段与原绑定明细 | tests/e2e/full-admin-remaining.spec.ts |
| 常规 UI | NAV-GOV-01 准入审计关联真实商户，资料齐全不等于审批通过 | tests/e2e/full-admin-remaining.spec.ts |
| 常规 UI | ADM-REM-01 exposes all non-first admin pages with route, evidence and readable content | tests/e2e/full-admin-remaining.spec.ts |
| 常规 UI | ADM-REM-02 governance, contract, ticketing, bar and amusement actions are auditable | tests/e2e/full-admin-remaining.spec.ts |
| 常规 UI | ADM-REM-03 risk, audit, analytics and integration pages cover empty/error/mobile states | tests/e2e/full-admin-remaining.spec.ts |
| 常规 UI | ADM-REM-04 merchant scoped role can use authorized merchant workspace but is denied platform pages | tests/e2e/full-admin-remaining.spec.ts |
| 常规 UI | ADM-REM-05 outsourced vendor can use assigned workbench pages only | tests/e2e/full-admin-remaining.spec.ts |
| 常规 UI | ADM-UI-HARDEN-01 restructured pages expose root + headered scrollable tables | tests/e2e/full-admin-ui-hardening.spec.ts |
| 常规 UI | ADM-UI-HARDEN-02 shared empty and error states expose stable testids | tests/e2e/full-admin-ui-hardening.spec.ts |
| 常规 UI | ADM-UI-HARDEN-03 overview date-range filter is operable and KPIs are addressable | tests/e2e/full-admin-ui-hardening.spec.ts |
| 常规 UI | ADM-UI-HARDEN-04 money inputs reject non-positive values before any write | tests/e2e/full-admin-ui-hardening.spec.ts |
| 常规 UI | ADM-UI-HARDEN-05 high-volume log pages support client-side search filter | tests/e2e/full-admin-ui-hardening.spec.ts |
| 常规 UI | ADM-UI-HARDEN-06 log pages paginate filtered rows and reset on search | tests/e2e/full-admin-ui-hardening.spec.ts |
| 常规 UI | ADM-UI-HARDEN-07 organization merchant create exposes editable fields with phone validation | tests/e2e/full-admin-ui-hardening.spec.ts |
| 常规 UI | ADM-VIS-01 desktop overview and production evidence pages keep a stable visual baseline | tests/e2e/full-admin-visual.spec.ts |
| 常规 UI | ADM-VIS-02 mobile admin routes keep nav, tables and primary actions usable | tests/e2e/full-admin-visual.spec.ts |
| 常规 UI | LEASE-CONTRACT 前端来源 source=${source} 被真实后端接受（2xx） | tests/e2e/leasing-contract-live-evidence.spec.ts |
| 常规 UI | LEASE-CONTRACT 已移除来源 source=${source} 仍被真实后端 400 拒 | tests/e2e/leasing-contract-live-evidence.spec.ts |
| 常规 UI | LEASE-CONTRACT 前端推进阶段 stage=${stage} 通过真实后端枚举校验 | tests/e2e/leasing-contract-live-evidence.spec.ts |
| 常规 UI | LEASE-CONTRACT 已移除阶段 stage=${stage} 仍被真实后端 400 拒 | tests/e2e/leasing-contract-live-evidence.spec.ts |
| 常规 UI | TICKETS-LIVE-01 本人订单可列票实例，他人 403，不存在 404，无 token 401 | tests/e2e/order-tickets-live-evidence.spec.ts |
| 常规 UI | ADM-FIN-004 物业费管理：费率设置 + 账单生成走通 | tests/e2e/property-fee-evidence.spec.ts |
| 真实后端 UI | REAL-UI-01 health, login, admin overview and logout use the real backend | tests/e2e/web-ui-real-backend.spec.ts |
| 真实后端 UI | REAL-UI-02 admin core business pages load real backend data without global errors | tests/e2e/web-ui-real-backend.spec.ts |
| 真实后端 UI | REAL-UI-03 protected business endpoints reject unauthenticated access (server-side denial, not just frontend hiding) | tests/e2e/web-ui-real-backend.spec.ts |

## 4. 视觉回归基线

| 截图基线 |
| --- |
| tests/e2e/full-admin-visual.spec.ts-snapshots/full-admin-bar-orders-mobile-chromium-linux.png |
| tests/e2e/full-admin-visual.spec.ts-snapshots/full-admin-dynamic-pricing-mobile-chromium-linux.png |
| tests/e2e/full-admin-visual.spec.ts-snapshots/full-admin-ops-readiness-desktop-chromium-linux.png |
| tests/e2e/full-admin-visual.spec.ts-snapshots/full-admin-overview-desktop-chromium-linux.png |

## 5. 架构与可维护性约束

| 文件 | 当前行数 |
| --- | ---: |
| apps/web/src/main.tsx | 50 |
| backend/src/main/java/com/complex/platform/ticketing/TicketingService.java | 1200 |
| backend/src/main/java/com/complex/platform/bar/BarService.java | 1009 |
| backend/src/main/java/com/complex/platform/bar/BarOrderService.java | 1749 |
| backend/src/main/java/com/complex/platform/bar/BarBillingService.java | 1018 |
| backend/src/main/java/com/complex/platform/bar/BarTableSessionService.java | 724 |

## 6. 可复跑命令

| 脚本 | 命令 |
| --- | --- |
| docs_ui_experience_acceptance | node tools/generate-ui-experience-acceptance-report.mjs |
| verify_ui_experience_acceptance | node tools/generate-ui-experience-acceptance-report.mjs --check |
| verify_ui | playwright test |
| verify_ui_real_backend_seeded | node tools/run-real-backend-ui-e2e.mjs |
| verify_closed_loop_runtime | npm run test:backend && npm run typecheck --workspace apps/web && npm run build --workspace apps/web && npm run verify:ui && npm run verify:ui:real-backend:seeded |

## 7. 仍不能宣称生产就绪的原因

- 15 production evidence submissions awaiting, including 13 P0
- NAS current state is failed; SEC-P0-OPS-RECOVERY-001 cannot close
- 12 production blocker register items remain open (6 readiness, 4 credential, 2 security findings)
- 9 external evidence or supplier-backed requirements remain partial

## 8. 验收判断

- 可进入开发联调：不可以。
- 可进入业务验收：不可以。
- 可宣称生产完全就绪：不可以。
- 下一步重点：继续回填真实生产证据、恢复 NAS 当前挂载/演练证据，并保持 `npm run verify:ui-experience-acceptance`、`npm run verify:runtime-acceptance-evidence` 和 `npm run verify:delivery-gates` 绿色。
