# gstack 需求验收与交付审计报告 V0.1（2026-06-16）

> ⚠️ 历史快照（2026-06-16）。本文 operation/测试计数（如 198 ops、562 后端测试）为历史快照，已过期。当前以 `p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `npm run verify:*` 实时输出为准（当前 209 ops / 729 测试 / Flyway V060）；文档审计当前态见 `development-documentation-audit-v0.2.md` 与 `closed-loop-live-acceptance-audit-20260618.md`。下文历史结论保留作追溯。

## 1. 审计口径

本报告按 `gstack-spec`、`gstack-autoplan`、`gstack-review`、`gstack-ship` 的组合口径执行：先从有效需求文档建立验收矩阵，再映射到代码、接口、数据库和测试证据，最后用构建、测试、安全扫描和交付风险检查判断当前是否达标。

本次只检查和修复 192.168.88.160 上 `/home/james/complex-management` 的正式代码库；本机不作为编码仓库。

## 2. 需求验收矩阵

| 需求/范围 | 文档依据 | 代码/接口证据 | 测试/验证证据 | 状态 |
| --- | --- | --- | --- | --- |
| P0 正式技术栈与旧原型隔离 | `docs/00-current-effective-document-entry-v0.1.md`、`docs/code-contract-difference-and-migration-audit-v0.1.md` | Java 21 + Spring Boot 4.1 + PostgreSQL + Flyway；`apps/api` 旧原型未回流 | `npm run verify:p0` 内含 `NO_LEGACY_API_PROTOTYPE_PASS` | 已完整实现 |
| P0 OpenAPI/权限/事件/客户端契约 | `docs/openapi/openapi-p0.json`、`docs/p0-openapi-operation-acceptance-matrix-v0.1.md` | 198 个 operation；客户端 93 个 route template、104 个 method call 匹配 OpenAPI | `npm run verify:p0-contract` 通过 | 已完整实现 |
| 票务动态定价订单接入 | `docs/p2-ticket-dynamic-pricing-api-contract-v0.1.md`、`docs/p2-ticket-dynamic-pricing-completion-audit-v0.1.md` | `TicketPricingService.applyPublishedDynamicPricing`、`TicketOrderService.createOrder`、`TicketOrderRecord`、`V041__ticket_order_dynamic_pricing_trace.sql` | `TicketingServiceTests.publishedDynamicPricingUsesHighestPriorityMatchingRuleForFormalOrder`；UI P0-UI-03 | 已完整实现 |
| Web 管理端 UI 自动化 | `docs/frontend-admin-development-design.md`、`docs/ui-automation-acceptance-report-v0.1.md` | `apps/web/src/main.tsx`、`playwright.config.ts`、`playwright.real-backend.config.ts` | `npm run verify:ui -- --reporter=line`，26 条通过；`npm run verify:ui:real-backend:seeded`，2 条真实后端通过 | 已补齐首批验收基线 |
| Web 主包过大 warning | `docs/ui-automation-acceptance-report-v0.1.md` | `apps/web/vite.config.ts` 拆分 React、AntD、icons、vendor | `npm run build --workspace apps/web` 通过，未出现主包过大 warning | 已修复 |
| TicketingService 拆分降风险 | 服务拆分审计与当前代码 | `TicketPricingService`、`TicketOrderService`、`TicketVerificationService`、`TicketingService` facade | 后端全量 562 测试通过 | 已实现 |
| BarService 拆分降风险 | P1 酒吧 SaaS/POS 增强与服务拆分审计 | `BarBillingService`、`BarEntryService`、`BarOrderService`、`BarTableSessionService`、`BarService` facade | `BarServiceTests` 与后端全量测试通过 | 已实现 |
| 真实后端 UI 验收一键化 | UI 验收报告与交付可复跑要求 | `tools/run-real-backend-ui-e2e.mjs`、`package.json` 脚本 `verify:ui:real-backend:seeded` | 自动打包后端、启动 18080、健康检查、真实登录、退出，并抽样票务/酒吧/游乐/第三方接口页面，测试通过 | 本次已修复 |
| 安全扫描 | 正式安全基线与 gstack-review 安全闸口 | Semgrep 扫描已跟踪文件与 11 个未跟踪新代码文件 | `p/secrets` + `p/owasp-top-ten` 均 0 finding | 已通过 |
| Android 终端 P0 | Android 端设计与 P0 验收 | Android Gradle 工程、设备绑定/离线包/核销冒烟 | `verify:p0` 内 Android env/build/smoke/apk 全通过 | 已完整实现 |
| 生产前置证据 | `docs/production-readiness-*`、NAS/MDM/credential Runbook | 文档、脚本和证据模板已在仓库内 | `verify:p0` 不等于生产上线；真实 MDM/支付/人脸/供应商证据仍需回填 | 不阻断联调，阻断生产上线 |

## 3. 本次对抗性发现与修复

| 编号 | 严重级别 | 发现 | 影响 | 本次处理 |
| --- | --- | --- | --- | --- |
| G-AUDIT-01 | P1 | 动态定价完成审计中残留“正式订单不受影响”的旧口径 | 会误导产品、测试和运营验收，认为订单接入未完成 | 已修正为“published 且匹配规则影响正式订单，draft 只模拟” |
| G-AUDIT-02 | P1 | 真实后端 UI 验收需要人工先起后端 | 回归不可复跑，交接成本高 | 新增 `verify:ui:real-backend:seeded` 一键脚本并验证通过 |
| G-AUDIT-03 | P1 | Semgrep 默认只扫 git 已跟踪文件，未跟踪新服务类会漏扫 | 安全审计结论可能漏掉新增代码 | 已单独扫描 11 个未跟踪新代码文件，0 finding |
| G-AUDIT-04 | P2 | UI 自动化报告曾出现乱码/问号 | 文档不可用于验收交接 | 已重写为 UTF-8 中文报告 |
| G-AUDIT-05 | P2 | 当前工作区仍有未提交/未跟踪核心文件 | 若打包或提交时漏掉新文件，会导致编译或功能缺失 | 已在本报告列为 ship 风险；后续提交/补丁包必须包含全部未跟踪核心文件 |

## 4. 验证证据

| 命令 | 结果 |
| --- | --- |
| `git diff --check` | 通过，无空白错误 |
| `npm run build --workspace apps/web` | 通过，无主包过大 warning |
| `npm run verify:ui -- --reporter=line` | 13 passed |
| `npm run verify:ui:real-backend:seeded` | 2 passed，自动启动临时后端并自动停止；覆盖真实登录/退出和核心业务页面真实快照读取 |
| `npm run verify:p0` | 通过；后端 562 tests，Web 构建，Android env/build/smoke/apk 均通过 |
| `~/.local/bin/semgrep scan --config p/secrets --config p/owasp-top-ten --exclude ... --error .` | 已跟踪文件 0 finding |
| 未跟踪新代码 Semgrep 专扫 | 11 个文件 0 finding |

## 5. 质量结论

- 当前代码实现与 P0/P1/P2 已编码需求基本一致，核心阻断项已关闭。
- 票务动态定价、Web UI 自动化、真实后端 UI 验收、服务拆分、Semgrep 扫描均有可复跑证据。
- 代码可以进入联调/验收；不建议直接宣布生产上线，因为 MDM、支付、人脸、供应商 SDK/API、真实生产 credential/轮换证据仍属于生产前置资料。

## 6. 剩余修复项与顺序

| 顺序 | 项目 | 级别 | 建议 |
| --- | --- | --- | --- |
| 1 | 提交/补丁包完整性 | P1 | 形成一次交接补丁包或提交，必须包含所有未跟踪服务类、迁移、Playwright 配置、真实后端 UI 测试和脚本 |
| 2 | 生产前置证据回填 | P1 | 继续按 blocker register 回填 MDM、支付、人脸、NAS 自动化、credential 轮换和供应商证据 |
| 3 | UI 自动化扩展到完整后台菜单 | P2 | 当前是首批 P0/P1 路径；正式后台信息架构确定后扩展到全菜单角色矩阵 |
| 4 | Mockito 动态 agent 警告 | P3 | 当前不影响测试通过；后续可在 Maven 测试配置中显式加入 Mockito agent，防未来 JDK 策略变化 |

## 7. 进入阶段结论

- 可以进入联调：可以。
- 可以进入产品/测试验收：可以，按本报告矩阵逐条验收。
- 可以进入生产上线：暂不建议。原因不是业务代码未完成，而是生产前置证据和外部供应商资料仍需真实回填。
- 可以执行 gstack-ship 的提交/PR 阶段：代码和验证证据具备条件，但当前我未执行 commit/push/PR；若进入提交阶段，必须先把未跟踪核心文件纳入提交或补丁包。
