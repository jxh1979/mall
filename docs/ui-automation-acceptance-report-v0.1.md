# UI 自动化验收报告 V0.1

## 1. 审计结论

本轮已为 Web 管理端补齐首批 Playwright UI 自动化验收。验收目标不是证明页面能打开，而是用可重复测试覆盖 P0/P1 用户路径、异常场景、权限拦截、空状态和窄屏布局。

两个范围结论同步收口：

1. 票务动态定价当前不是 P0/P1 正式售票价引擎，而是 P2 规则容器和模拟能力。已覆盖创建草稿、发布和模拟；未覆盖自动改写正式订单实收价，这是需求边界，不是遗漏。
2. Web 管理端当前是轻量管理/只读与有限操作入口，不是完整后台管理系统替代品。完整后台能力仍以后端接口、正式运营后台和后续专项为准。

## 2. UI 验收矩阵

| 编号 | 用户路径 | 前置条件 | 操作步骤 | 预期结果 | 页面 | 接口 | 测试数据 | 自动化证据 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| P0-UI-01 | 登录、查询控制台、视觉检查、退出 | Mock API 返回平台管理员 token 和基础业务数据 | 打开 `/`，输入账号密码，进入控制台，检查 Weekend Pass，截图比对，点击退出 | 登录成功、控制台可查询、视觉基线稳定、退出回到登录页 | 控制台 | `/api/v1/auth/login`、`/api/v1/analytics/dashboards/p0-overview` 等 | `admin/admin-pass`、Weekend Pass | `tests/e2e/web-ui-acceptance.spec.ts`，`dashboard-shell` 截图 |
| P0-UI-02 | 登录表单校验 | 未输入账号密码 | 直接提交登录表单 | 账号和密码必填错误出现 | 登录页 | 无后端调用 | 空表单 | `form validation blocks empty login` |
| P0-UI-03 | 票务出票、动态定价草稿、发布、模拟 | 已有票种、权益模板、动态定价草稿 | 进入 `/?page=ticketing`，提交出票，创建动态定价草稿，确认发布，模拟价格 | 返回票号/权益码；动态定价返回 draft；发布后 active；模拟返回 finalPriceCent | 票务核销 | `/api/v1/ticketing/ticket-types`、`/issue`、`/dynamic-pricing-rules`、`/publish`、`/dynamic-pricing/simulations` | Weekend Pass、Weekend Boost | `ticket issuing and P2 dynamic pricing container...` |
| P1-UI-01 | 第三方供应商适配器创建、团体/包场释放 | 已有 provider adapter 和 approved group booking | 进入第三方接口页创建适配器；重新登录到游乐页，释放已锁定容量 | 适配器创建返回 provider-2；释放返回 released | 第三方接口、游乐运营 | `/api/v1/integration/provider-adapters`、`/api/v1/amusement/group-bookings/{id}/release` | mock-video、Team A | `provider adapter creation and group booking release...` |
| P0-UI-04 | 权限/后端错误态 | Mock API 对 P0 overview 返回 403 | 登录后加载数据 | 管理台壳仍可见，错误提示包含 403 | 全局错误态 | `/api/v1/analytics/dashboards/p0-overview` | mock forbidden | `permission and backend error state are visible` |
| P0-UI-05 | 空状态 | 所有业务列表为空 | 登录到票务页 | 空表格出现 AntD Empty，占位态可见 | 票务核销 | 各 list API 返回空数组 | empty=true | `empty state is explicit...` |
| P0-UI-06 | 窄屏布局 | 390px 宽 viewport | 登录控制台，收起/展开导航，截图 | 页面壳可用，横向溢出在可控阈值内，移动端截图稳定 | 控制台窄屏 | 同 P0-UI-01 | 390x844 | `mobile-dashboard` 截图 |

## 3. 本轮代码修复

| 修复项 | 说明 | 文件 |
| --- | --- | --- |
| 第三方适配器接口模型修复 | 原前端把 `/api/v1/integration/provider-adapters` 同时当作接口目录和开放应用数组，和后端返回不一致；已改为 `ProviderAdapter` 并从适配器生成接口目录 | `apps/web/src/main.tsx` |
| 明文账号密码表单移除 | 旧“开放应用”表单提交账号/密码字段，后端不接收也不应由管理台明文处理；已改为供应商适配器配置 | `apps/web/src/main.tsx` |
| 高影响确认改为受控 Modal | 静态 `Modal.confirm` 在自动化中不可稳定验收；已改为组件内受控 Modal，并补确认按钮测试选择器 | `apps/web/src/main.tsx` |
| 稳定选择器 | 登录、业务表单、结果区、发布/释放确认等补充 `data-testid` | `apps/web/src/main.tsx` |
| 构建拆包 | Vite Rollup 按 React、AntD、icons、vendor 拆 chunk，消除主包过大 warning | `apps/web/vite.config.ts` |
| UI 自动化 | 新增 Playwright 配置、mock API、UI 验收 spec 和截图基线 | `playwright.config.ts`、`tests/e2e/*` |

## 4. 验证命令

在 192.168.88.160 的 `/home/james/complex-management` 执行：

| 命令 | 结果 |
| --- | --- |
| `npm run typecheck` | 通过 |
| `npm run test:ui:update` | 通过，7 个 UI 验收，生成/更新 2 张截图基线 |
| `npm run verify:ui` | 通过，7 个 UI 验收 |
| `npm run build` | 通过，未出现主包过大 warning；主入口约 89KB，React/AntD/icons/vendor 已拆分 |
| `npm run verify:p0` | 通过，含契约、安全基线、后端 557 个测试、Web 构建、Android 环境/构建/冒烟/APK |

## 5. 尚未覆盖和不应误判的内容

| 项目 | 状态 | 原因/下一步 |
| --- | --- | --- |
| 动态定价自动影响正式售票订单 | 未覆盖 | 当前需求边界是 P2 规则容器和模拟，不接入正式实收价 |
| Web 全量后台管理端 | 未覆盖 | 当前 Web 口径是轻量管理/只读与有限操作，不是全量后台替代 |
| 真实第三方 SDK/API 联调 | 未覆盖 | 等供应商字段、签名、回调、沙箱资料到位后进入联调测试 |
| 真实 MDM、支付、人脸、NAS 自动化、credential 轮换证据 | 未关闭 | 属于生产前置证据，不允许用 mock 或文档假关闭 |
| 硬删除能力 | 未作为通用 UI 路径覆盖 | 当前业务多采用状态流转、释放、撤销、作废，不做通用物理删除 |

## 6. 结论

Web UI 自动化验收已具备首批可执行基线，可作为后续联调前的页面回归闸口。生产 blocker register 中依赖真实外部证据的事项仍保持打开，等待采购/IT/供应商/生产环境回填真实证据后逐项关闭。
