# 代码实现对抗式验收审计 V0.1

> 审计日期：2026-06-16
> 执行位置：192.168.88.160 `/home/james/complex-management`
> 审计目标：确认当前代码库是否按开发文档实现到核心业务可运行，并列出未完成与风险项。

## 1. 需求到代码矩阵

- 矩阵文件：`docs/requirements-code-implementation-matrix-v0.1.csv`
- 总需求数：97
- 已完整实现：88
- 部分实现：9
- 未实现：0
- 实现与文档不一致：0
- 有代码但不可运行：0

部分实现项集中在：

- 第三方真实供应商资料、生产证据、生产凭证、生产回调等尚未到位的接口边界。
- 当前已按 mock、adapter、Runbook、blocker register、生产证据台账方式保留验收与补齐路径，不得标记为真实生产闭环。

## 2. 本次修复

- 修复 `android:p0:smoke` 依赖人工预先启动后端的问题，并将新增 smoke runner/wrapper 纳入交付闸口。
- 完整后台首批与非首批页面均已实现；`full-admin-page-permission-field-matrix-v0.1.csv` 中 30 个页面均有路由、权限、UI 与 Playwright 覆盖。
- 新增 `PermissionGate.test.ts`，按完整后台页面矩阵保护主责角色到页面读取权限；前端单元测试从 14 条扩展到 16 条。
- `verify:delivery-gates` 已纳入完整后台矩阵、生产证据、NAS 当前状态漂移、运行态验收证据和外部证据追踪，防止旧报告误关生产阻断。
- 将新增核心文件纳入 Git 索引，避免交付补丁包缺文件。

## 3. 可运行业务路径

- Web 管理端：登录、登出、仪表盘、票务、酒吧、游乐、第三方适配、角色权限、空状态、错误状态、窄屏路径。
- 后端 API：OpenAPI P0 207 个操作契约校验通过，97 个前端路由模板和 74 个方法级调用匹配 OpenAPI。
- 票务：基础售票、动态定价字段留痕、订单和核销拆分服务、迁移 V041。
- 酒吧：入场、点单、桌台会话、账单结算拆分服务；卡座/桌台相关增强已纳入测试。
- Android 终端：登录、设备绑定、在线扫码预览与核销、离线权益包、离线核销队列、人工补录、同步、归还拦截、事件上报。
- 生产前置：生产证据台账、blocker register、owner action、提交包、回填优先级、回填执行账、NAS/凭据/轮换相关验证闸口。

## 4. 第三方接口 mock / stub 清单

- 支付/聚合支付：保留 adapter 和契约边界，真实通道资料未到位前以 mock/沙箱验收。
- 短信/通知：以供应商待接入边界和生产证据台账管理。
- 设备/视频/机器人：统一双向开放方案，保留 provider adapter、Webhook、轮询校验、补偿任务和对账差异对象。
- AI/本地大模型：T3D 本地 LLM 质检交付闸口通过，真实生产模型配置仍需证据回填。
- 文件/对象存储/NAS：NAS SMB 信息和证据台账已纳入生产前置验证口径。

## 5. 验证证据

- `npm run verify:p0`：通过。
- `npm run verify:delivery-gates`：未通过。2026-11-02 01:49 UTC 在工作树逐项跑子项（总链遇红即停），仍红：`verify:coding-host-protection`（写死主仓路径；用 `COMPLEX_EXPECTED_CODING_REPO_ROOT` 指向工作树复跑仍红在基线文档与证据里的 canonical_repo_root）、`verify:final-closed-loop-audit`（只剩 integration-ready、business-acceptance-ready 两条就绪结论校验，运行验收未就绪）；`verify:nas-current-state-drift`、`verify:nas-diagnostics` 的现场探测未跑；其余子项通过（`verify:unit` 第一次实跑 1 例失败、整套重跑通过，见终验 §7「Web 单元测试」一行）（同终验 §7「交付总闸口」一行）。
- `npm run verify:ui`：47 条 Playwright 用例（源码定义计数），常规界面验收（接口全部 mock）实跑 40 条通过，覆盖完整后台 MVP、非首批页面、视觉回归、移动端、错误态和角色权限路径。
- `npm run verify:ui:real-backend:seeded`：3 条，按第十八批裁决 25 未跑。
- `security-scans/semgrep-latest-summary.json`：scan_ref=`security-scans/semgrep-20260617-044736-p-default.json`，results=0，errors=0，paths_scanned=1033。
- 新增 smoke runner/wrapper 单独 Semgrep：0 findings。
- `git diff --cached --check`：通过。
- Playwright MCP：通过本机 SSH 转发打开 160 静态构建产物，页面标题为“综合体管理系统”，已生成首屏截图。

## 6. P0/P1/P2/P3 问题

### P0

- 当前未发现阻塞核心业务运行的 P0 问题。
- 已修复：Android P0 smoke 原先依赖人工启动后端，现已可由 `verify:p0` 自动完成。

### P1

- 第三方真实供应商资料、生产账号、真实回调、真实对账数据未到位时，只能进入 mock/沙箱联调，不能宣称生产闭环完成。
- 8 条“部分实现”需求均为真实供应商资料、生产账号、真实回调、真实对账或线下设备/交接证据未到位；应按 `external-evidence-requirement-traceability-v0.1.md` 和生产 evidence manifest 逐项回填。

### P2

- 已关闭：`TicketingService` 已从约 961 行瘦身到 332 行，`BarService` 已从约 625 行瘦身到 356 行；审计/权限/业务类型元数据已迁移到 `TicketingReadOperation`、`TicketingWriteOperation`、`BarReadOperation`、`BarWriteOperation`，facade 仅保留入口编排、ID 校验和子服务委托。
- 已关闭首批实现：平台治理 HTML 拓扑页/多综合体集团视图已在 Web 平台地基页补齐，基于组织、商户、综合体、区域和点位数据展示关系，并由 P2-UI-13 自动化验收覆盖。

### P3

- 无阻塞性格式问题；暂存补丁行尾空白检查已通过。

## 7. 结论

- 核心业务路径：运行验收未就绪（真实后端界面验收按第十八批裁决 25 未跑），暂不下可以进入联调/验收的结论。
- 上线结论：不建议直接生产上线；需补齐真实第三方供应商资料、生产凭证、生产回调、真实对账、生产证据回填后再走上线门禁。
- 下一步优先级：供应商真实资料回填与联调 > 生产 evidence manifest 回填 > NAS/MDM/支付/人脸/视频/机器人或人工交接生产证据复核；不再把完整后台 MVP 编码列为未完成项。

## 完整后台 MVP 实现回填

`docs/full-admin-mvp-coding-package-v0.1.md` 对应首批后台页面已实现独立懒加载入口；非首批 22 个页面也已完成，证据见 `docs/full-admin-mvp-requirements-code-matrix-v0.1.csv`、`docs/full-admin-remaining-requirements-code-matrix-v0.1.csv`、`tests/e2e/full-admin-mvp.spec.ts`、`tests/e2e/full-admin-remaining.spec.ts`、`apps/web/src/admin/security/PermissionGate.test.ts`。
