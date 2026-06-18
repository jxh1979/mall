# 完整后台 MVP 需求到代码实现审计 V0.1

**日期**：2026-06-16
**审计方式**：documentation-specialist + PM Critic + acceptance criteria + edge cases + code review + security review + Playwright 验收
**范围**：`docs/full-admin-mvp-coding-package-v0.1.md` 中 8 个首批后台 MVP 页面。

## 1. 审计结论

首批完整后台 MVP 页面已从旧轻量入口中拆出独立 `/admin/*` 入口，并以懒加载方式挂载，避免继续扩大主包。当前 8 个页面均具备页面、API 调用、权限体验层、关键表单校验、错误/空状态基础和 Playwright 自动化验收。

- 需求完成度：8/8 页面主流程完成，权限/移动端专项完成，首批 MVP 覆盖率按页面口径为 100%。
- 第三方接口：供应商真实 SDK/API 未到位的部分使用 mock/stub；页面只保存 `credential_ref`，不保存明文密钥。
- 架构状态：新增 `apps/web/src/admin/`，API 客户端放在 `client/`，避免被客户端 API 契约扫描误判为非正式 `/api/admin`。
- 可联调性：前端调用均使用 OpenAPI 已登记 `/api/v1/*` 或 `/api/open/*` 路径。

## 2. 需求到代码矩阵

详见：`docs/full-admin-mvp-requirements-code-matrix-v0.1.csv`。

## 3. 自动化验收覆盖

| 测试文件 | 覆盖内容 | 状态 |
|---|---|---|
| `tests/e2e/full-admin-mvp.spec.ts` | 8 个完整后台 MVP 页面、权限、移动端、核心表单、异常拦截 | 通过，8/8 |
| `tests/e2e/web-ui-acceptance.spec.ts` | 旧轻量入口 P0/P1/P2 核心路径回归 | 通过，14/14 |

## 4. 第三方 mock/stub 清单

| 类型 | 路径 | Mock 行为 | 覆盖异常 |
|---|---|---|---|
| 登录 | `/api/v1/auth/login` | 成功/失败登录 | 401 鉴权失败 |
| 供应商适配器 | `/api/v1/integration/provider-adapters`、`/test-mock` | 保存适配器、mock 测试通过 | 明文密钥前端拒绝；可用 postErrorPaths 模拟 500 |
| 生产证据 | `/api/v1/ops/production-readiness/items`、`/checks` | 证据项读取、状态更新 | 无证据引用前端拒绝 |
| 票务 | `/api/v1/ticketing/ticket-types`、`/publish` | 创建票种并发布 | 缺节假日价前端拒绝 |
| 卡座 | `/api/v1/bar/reservations/{id}/check-in-guest` | 入场登记 | 超人数前端拒绝 |
| 财务 | `/api/v1/finance/ar-ap-items/{id}/payments` | 部分核销 | 缺调整原因前端拒绝 |

## 5. 剩余风险

1. P1：当前权限守卫是前端体验层，真实权限仍必须以后端鉴权为准；后端已存在对应权限/认证基础，但完整后台真实角色矩阵仍需在真实后端联调中抽样。
2. P1：完整后台 MVP 页面目前是首批可运行面，不代表完整后台全页面范围全部完成；非首批页面仍按 `full-admin-page-permission-field-matrix` 后续拆包。
3. P2：生产证据和供应商真实资料仍需上线前回填真实 `credential_ref`、证据 URI、供应商字段映射和回调样例。

## 6. 下一步修复顺序

1. 跑完整验证链：`verify:ui`、`verify:p0-contract`、`build`、`test:backend`、安全扫描。
2. 为完整后台接入真实后端抽样 E2E，覆盖登录、票务发布、卡座入场、财务核销、供应商 mock、生产证据。
3. 按非首批后台页面继续拆第二批开发包。
