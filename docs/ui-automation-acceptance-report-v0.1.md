# UI 自动化验收报告 V0.1（2026-06-16）

## 1. 审计结论

Web 管理端已补齐首批可复跑 Playwright UI 自动化验收。验收目标不是证明页面能打开，而是覆盖 P0/P1 用户路径、表单校验、权限拦截、空状态、失败场景、窄屏布局、完整后台视觉回归，以及真实后端登录链路。

本轮同步修正两个口径：

1. 票务动态定价已经升级为 P2 已发布规则影响正式订单价格；草稿规则仍只用于模拟。
2. Web 管理端当前仍是轻量管理/运营入口，不替代完整后台管理系统；完整后台能力以后端接口和后续正式运营后台为准。

## 2. UI 验收矩阵

| 编号 | 用户路径 | 前置条件 | 操作步骤 | 预期结果 | 页面 | 接口 | 测试数据 | 自动化证据 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| P0-UI-01 | 登录、查询控制台、视觉检查、退出 | Mock API 返回平台管理员 token 和基础业务数据 | 打开 `/`，输入账号密码，进入控制台，检查 Weekend Pass，截图比对，点击退出 | 登录成功、控制台可查询、视觉基线稳定、退出回到登录页 | 控制台 | `/api/v1/auth/login`、`/api/v1/analytics/dashboards/p0-overview` 等 | `admin/admin-pass`、Weekend Pass | `tests/e2e/web-ui-acceptance.spec.ts`、`dashboard-shell` 截图 |
| P0-UI-02 | 登录表单校验 | 未输入账号密码 | 直接提交登录表单 | 账号和密码必填错误出现，未发起成功登录 | 登录页 | 无后端成功调用 | 空表单 | `form validation blocks empty login` |
| P0-UI-03 | 票务出票、动态定价创建、发布、模拟、订单接入 | 已有票种、权益模板、动态定价草稿 | 进入票务页，提交出票，创建动态定价草稿，确认发布，模拟价格，再创建正式订单 | 返回票号/权益码；动态定价返回 `draft`；发布后 `published`；模拟返回 `finalPriceCent`；订单返回 `dynamicPricingRuleId`、`baseAmountCent`、`dynamicAdjustmentCent`、`amountCent` | 票务核销 | `/api/v1/ticketing/ticket-types`、`/issue`、`/dynamic-pricing-rules`、`/publish`、`/dynamic-pricing/simulations`、`/api/v1/ticketing/orders` | Weekend Pass、Weekend Boost | `P0-UI-03 ticket issuing...` |
| P1-UI-01 | 第三方供应商适配器创建、团体/包场释放 | 已有 provider adapter 和 approved group booking | 进入第三方接口页创建适配器；重新登录到游乐页，释放已锁定容量 | 适配器创建返回 `provider-2`；释放返回 `released` | 第三方接口、游乐运营 | `/api/v1/integration/provider-adapters`、`/api/v1/amusement/group-bookings/{id}/release` | mock-video、Team A | `P1-UI-01 provider adapter...` |
| P0-UI-04 | 权限/后端错误态 | Mock API 对 P0 overview 返回 403 | 登录后加载数据 | 管理台壳仍可见，错误提示包含 403 | 全局错误态 | `/api/v1/analytics/dashboards/p0-overview` | mock forbidden | `P0-UI-04 permission...` |
| P0-UI-05 | 空状态 | 所有业务列表为空 | 登录到票务页 | 空表格出现 AntD Empty，占位态可见 | 票务核销 | 各 list API 返回空数组 | `empty=true` | `P0-UI-05 empty state...` |
| P0-UI-06 | 窄屏控制台布局 | 390px 宽 viewport | 登录控制台，收起/展开导航，截图 | 页面壳可用，横向溢出在可控阈值内，移动端截图稳定 | 控制台窄屏 | 同 P0-UI-01 | 390x844 | `mobile-dashboard` 截图 |
| P0-UI-07 | 角色矩阵允许路径 | Mock 登录为 `finance_operator` | 登录并读取运营总览 | 允许角色可进入对应页面并看到业务数据 | 控制台/财务 | `/api/v1/auth/login`、overview | finance_operator | `P0-UI-07 role matrix...` |
| P0-UI-08 | 角色矩阵拒绝路径 | Mock 登录为无权限角色 | 登录后访问受限数据 | UI 显示 forbidden，不误判为空状态 | 控制台/权限 | overview 403 | clerk | `P0-UI-08 role matrix...` |
| P0-UI-09 | 登录失败 | Mock 登录返回 401 | 输入错误账号密码并提交 | 登录失败提示明确，不进入应用壳 | 登录页 | `/api/v1/auth/login` | bad-user/bad-pass | `P0-UI-09 failed login...` |
| P0-UI-10 | 业务操作失败 | Mock POST 返回 500 | 提交业务表单 | 显示失败提示，页面保持可继续操作 | 票务页 | POST error path | backend failure | `P0-UI-10 failed business action...` |
| P0-UI-11 | 移动端酒吧关键路径 | 390px 宽 viewport | 进入酒吧页，检查卡座/订单/账单关键内容 | 酒吧关键内容在窄屏可读可操作 | 酒吧运营 | bar flow API | mobile bar data | `P0-UI-11 mobile bar...` |
| P0-UI-12 | 移动端游乐关键路径 | 390px 宽 viewport | 进入游乐页，检查场次/安全/团体释放关键内容 | 游乐关键内容在窄屏可读可操作 | 游乐运营 | amusement flow API | mobile amusement data | `P0-UI-12 mobile amusement...` |
| P2-UI-13 | 平台治理拓扑/集团视图 | Mock API 返回组织、商户、综合体、区域和点位 | 进入平台地基页，检查拓扑摘要和组织-商户-综合体-点位关系 | 显示组织/商户/综合体/区域/点位计数，节点含 Platform Org、Main Merchant、Main Venue、Gate A | 平台地基 | `/api/v1/platform/business-entities`、`/api/v1/platform/merchants`、`/api/v1/platform/projects`、`/api/v1/platform/spaces` | Platform Org/Main Merchant/Main Venue/Gate A | `P2-UI-13 platform governance topology...` |
| ADM-VIS-01 | 完整后台桌面视觉回归 | Mock API 返回完整后台基础数据 | 登录 `/admin/overview`，截图总控驾驶舱；进入 `/admin/ops/readiness`，截图生产 blocker 与证据页 | 两个桌面页无文档级横向溢出，截图基线稳定，生产证据页保留 NAS/blocker 可见信息 | 完整后台 | `/api/v1/analytics/dashboards/p0-overview`、`/api/v1/ops/production-readiness/items` | 1440x900、mock readiness | `tests/e2e/full-admin-visual.spec.ts`、`full-admin-overview-desktop`、`full-admin-ops-readiness-desktop` |
| ADM-VIS-02 | 完整后台移动端视觉和响应式关键路径 | 390px viewport；Mock API 返回酒吧订单和动态定价数据 | 登录 `/admin/bar/orders` 截图；横向导航切到 `/admin/ticketing/dynamic-pricing` 截图 | 移动端导航、表格和主内容可用，无材料级横向溢出，截图基线稳定 | 完整后台 | `/api/v1/bar/orders`、`/api/v1/ticketing/dynamic-pricing-rules` | 390x844、ORD-1、Weekend Boost | `tests/e2e/full-admin-visual.spec.ts`、`full-admin-bar-orders-mobile`、`full-admin-dynamic-pricing-mobile` |
| REAL-UI-01 | 真实后端健康检查、登录、控制台、退出 | 160 临时后端或真实后端可访问，具备测试管理员凭证 | 检查 `/api/v1/system/health`，调用真实登录接口，打开 Web 登录并退出 | 真实后端健康、登录成功、控制台出现、无全局错误、退出回登录页 | Web + 真实后端 | `REAL_BACKEND_BASE_URL`、`/api/v1/auth/login` | 测试管理员凭证 | `tests/e2e/web-ui-real-backend.spec.ts` |

## 3. 代码修复与测试资产

| 修复项 | 说明 | 文件 |
| --- | --- | --- |
| UI 自动化扩展 | UI 验收从 7 条扩展到 14 条，覆盖角色矩阵、失败场景、移动端关键路径和平台治理拓扑；前端单元测试新增完整后台主责角色权限矩阵保护 | `tests/e2e/web-ui-acceptance.spec.ts`、`apps/web/src/admin/security/PermissionGate.test.ts` |
| Mock API 扩展 | 补齐 GET/POST 成功、失败、权限和空状态数据 | `tests/e2e/support/mock-api.ts` |
| 稳定选择器 | 登录、业务表单、结果区、发布/释放确认等补充 `data-testid` | `apps/web/src/main.tsx` |
| 完整后台视觉回归 | 新增桌面/移动端完整后台截图基线，覆盖总控驾驶舱、生产 blocker 与证据、酒吧订单/出品/账单、动态定价移动端，并断言无材料级横向溢出 | `tests/e2e/full-admin-visual.spec.ts`、`tests/e2e/full-admin-visual.spec.ts-snapshots/*.png` |
| 真实后端联调 | 新增真实后端 Playwright 配置和真实后端验收 spec | `playwright.real-backend.config.ts`、`tests/e2e/web-ui-real-backend.spec.ts` |
| 一键真实后端验收 | 自动打包 Java 后端、启动临时后端、运行真实后端 UI 验收并自动停止 | `tools/run-real-backend-ui-e2e.mjs`、`package.json` |
| Web 构建拆包 | Vite Rollup 按 React、AntD、icons、vendor 拆 chunk，消除主包过大 warning | `apps/web/vite.config.ts` |
| Semgrep 误报收口 | Android launcher exported 和运行脚本 Host 误报均以注释/变量重命名收口 | `apps/android-terminal/android-app/app/src/main/AndroidManifest.xml`、`ops/160/complex-runtime-config-check.sh` |

## 4. 验证命令

在 192.168.88.160 的 `/home/james/complex-management` 执行：

| 命令 | 结果口径 |
| --- | --- |
| `npm run typecheck --workspace apps/web` | Web 类型检查通过 |
| `npm run build --workspace apps/web` | Web 构建通过，无主包过大 warning |
| `npm run verify:ui -- --reporter=line` | 28 条 UI 自动化验收通过，含旧控制台、完整后台 MVP、完整后台剩余页面和完整后台视觉回归 |
| `npm run verify:ui:real-backend:seeded` | 自动启动临时真实后端并通过 2 条真实后端 UI 验收；覆盖登录/退出以及票务、酒吧、游乐、第三方接口页面真实快照读取 |
| `npm run verify:p0-contract` | OpenAPI、样例、operation matrix、closure appendix、client route/openapi/payload 均通过 |
| `npm run test:backend` | 后端全量测试通过 |
| `~/.local/bin/semgrep scan --config p/secrets --config p/owasp-top-ten ... --error` | 安全扫描 0 finding |
| `npm run verify:p0` | P0 总闸口通过，含契约、安全基线、后端、Web、Android 环境/构建/冒烟/APK |

## 5. 尚未覆盖和不应误判的内容

| 项目 | 状态 | 原因/下一步 |
| --- | --- | --- |
| 完整后台管理端 | 规格已正式化，页面未全量实现 | 当前 Web 已补平台治理拓扑首批视图，但仍是轻量管理/运营入口，不是全量后台替代；已新增 `full-admin-srs-v0.1.md`、`full-admin-page-permission-field-matrix-v0.1.csv`、`full-admin-ui-acceptance-matrix-v0.1.md`，后续按矩阵扩展 UI 自动化 |
| 真实第三方 SDK/API 联调 | 未覆盖 | 等供应商字段、签名、回调、沙箱资料到位后进入联调测试 |
| 真实 MDM、支付、人脸、NAS 自动化、credential 轮换证据 | 未全部关闭 | 属于生产前置证据，不允许用 mock 或文档假关闭 |
| 细粒度 RBAC 页面级矩阵 | 已覆盖 | `PermissionGate.test.ts` 按 `full-admin-page-permission-field-matrix-v0.1.csv` 的主责角色保护全部 30 个后台路由读取权限；Playwright 继续覆盖核心允许/拒绝路径 |
| 通用物理删除能力 | 未作为通用 UI 路径覆盖 | 当前业务多采用状态流转、释放、撤销、作废，不做通用物理删除 |

## 6. 一键真实后端验收入口

`npm run verify:ui:real-backend:seeded` 会先打包 Java 后端，使用测试管理员凭证在 `18080` 启动临时后端，等待 `/api/v1/system/health` 通过后运行 `verify:ui:real-backend`，覆盖真实登录/退出和票务、酒吧、游乐、第三方接口页面真实快照读取，最后自动停止临时后端。

生产凭证不写入脚本。如需覆盖真实账号，应通过 `REAL_BACKEND_USERNAME`、`REAL_BACKEND_PASSWORD`、`REAL_BACKEND_BASE_URL` 显式注入。

## 7. 结论

Web UI 自动化验收已具备首批可执行基线，可作为后续服务拆分、接口联调和页面扩展前的回归闸口。生产 blocker register 中依赖真实外部证据的事项仍保持打开，等待采购、IT、供应商和生产环境回填真实证据后逐项关闭。

## 完整后台 MVP 追加验收

新增 `tests/e2e/full-admin-mvp.spec.ts`，覆盖 `/admin/*` 首批 8 个 MVP 页面、权限拦截、核心表单校验、第三方 mock、生产证据和 390px 窄屏布局。

## 完整后台视觉回归追加验收

新增 `tests/e2e/full-admin-visual.spec.ts`，覆盖 1440px 桌面总控驾驶舱、生产 blocker 与证据页，以及 390px 移动端酒吧订单/出品/账单、动态定价页面。截图基线位于 `tests/e2e/full-admin-visual.spec.ts-snapshots/`，用于发现布局漂移、文字遮挡、表格裁切和移动端横向溢出。
