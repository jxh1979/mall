# 代码契约差异与迁移审计 V0.1

## 1. 文档定位

本文档用于阻断一个高风险误用：把历史预研原型代码当作正式 P0 开发入口。

当前正式实现只认以下基线：

| 项目 | 正式口径 |
|---|---|
| 后端技术栈 | Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven + springdoc-openapi |
| P0 接口契约 | `docs/openapi/openapi-p0.yaml`、`docs/openapi/openapi-p0.json` |
| P0 权限事实源 | `docs/p0-operation-permission-matrix-v0.1.md` |
| P0 验收事实源 | `docs/p0-openapi-operation-acceptance-matrix-v0.1.md`、`docs/p0-openapi-contract-closure-appendix-v0.1.md`、`docs/p0-task-api-permission-event-acceptance-map-v0.1.md` |

本文档不是新增业务需求，也不是要求删除原型；它是正式编码前的差异清单和迁移闸口。

## 2. 对抗式审计结论

| 编号 | 发现 | 风险等级 | 结论 |
|---|---|---:|---|
| D1 | `apps/api` 仍是 TypeScript / Fastify / Prisma / SQLite 预研原型 | P0 | 已标记为 legacy，且子脚本已改为阻断提示；不得启动、部署、联调或做安全审计基线 |
| D2 | `apps/web` 旧 `/api/admin/*`、`/api/integration/*`、`/api/auth/*` 等非正式路径已迁入 `/api/v1/*` 或 `/api/open/*` 命名空间 | P0 Gate | `npm run verify:p0-contract` 已通过；占位 operation 不代表 P1/P2/P3 深度能力生产就绪 |
| D3 | `apps/android-terminal` 旧 `/api/auth/login`、`/api/admin/*`、`/api/terminal/*`、`/api/health` 等非正式路径已迁入 `/api/v1/*` 命名空间 | P0 Gate | `npm run verify:p0-contract` 已通过；端侧 offline/sync/return 等能力仍须按阶段边界和验收策略实现 |
| D4 | `backend/` 已形成 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 初始后端脚手架 | P0 Gate | 仍须按 BE-000 在 160 或等效验收环境完成 Maven、启动、连库、Flyway baseline、springdoc 输出和契约差异校验后，才允许把业务域标记为正式开发完成 |
| D5 | 成熟系统对照文档仍包含旧 API 表格 | P1 | 仅作为市场参考和增强建议；不得作为 P0 接口实现依据 |
| D6 | 160 运维脚本曾残留 Node/Fastify/SQLite 发布和备份路径 | P0 | 已切换为 Java jar 发布、回滚、健康检查、BE-000/P0 验收；备份不再纳入 SQLite 原型库 |

## 3. 当前可参考与禁止使用范围

| 目录或文件 | 可参考内容 | 禁止事项 |
|---|---|---|
| `apps/api/**` | 早期流程、字段草稿、mock 数据、历史验证思路；子脚本只允许输出 legacy 阻断提示 | 不得作为正式 API 服务、数据库模型、Flyway 迁移、部署脚本、seed/smoke 脚本或安全扫描对象 |
| `backend/**` | 正式 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 后端工程入口 | 不得反向兼容旧 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*` 作为正式路径 |
| `ops/160/*.sh` | 160 Java 后端发布、回滚、健康检查、BE-000/P0 验收和备份脚本 | 不得启动旧 Node API，不得备份 SQLite 原型库作为正式资产 |
| `apps/web/**` | 页面布局、交互经验、业务对象展示方式 | 不得用旧接口路径或旧字段名反向要求后端兼容；正式联调前必须通过 `npm run verify:p0-contract` |
| `apps/android-terminal/**` | 端侧登录、扫码、fixture、设备交互预研经验 | 不得沿用旧路径或旧字段名；不得绕过 MDM/设备绑定/终端能力开关；正式联调前必须通过 `npm run verify:p0-contract` |
| `security-scans/**` | 旧 Node/Fastify/SQLite 原型安全扫描方法和历史风险追溯 | 不得作为当前 Java/Spring/PostgreSQL 安全基线；正式工程落地后必须重做威胁模型和安全扫描 |
| `docs/business-module-mature-system-gap-analysis.md` | 市场成熟能力、P1/P2 增强方向 | 不得作为当前 P0 开发任务清单或接口契约 |
| `docs/openapi/openapi-p0.yaml/json` | 正式 P0 接口、请求体、权限、终端、幂等、事件 | 不得在未更新 OpenAPI 的情况下直接新增接口实现 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | OpenAPI operation 到任务簇、权限、事件、终端和样例准入的追溯附录 | 不得在未通过 `npm run verify:openapi-closure` 的情况下生成 Controller、SDK/mock、接口自动化或供应商联调文档 |

## 4. 迁移规则

1. **先契约，后实现**：任何正式 Java Controller 必须先能在 OpenAPI 中找到 operationId、路径、请求体、响应、权限、认证上下文、终端、幂等和事件，并能在 `p0-openapi-contract-closure-appendix-v0.1.md` 追溯任务簇、权限、事件、终端和样例准入。
2. **旧路径不得兼容为正式路径**：`/api/admin/*`、`/api/mobile/*`、`/api/integration/*`、`/api/auth/*` 旧路径只可在迁移适配层临时识别，不得写入正式 PRD、OpenAPI、测试集或供应商文档。
3. **前端和 Android 必须换契约**：Web、工作手机、平板、一体机、小程序/App/Web 客户端必须基于 OpenAPI 生成或手写新 API client，写接口请求字段以 OpenAPI snake_case schema 为准，客户端内部可做 camelCase 适配但不得裸发旧字段。
4. **数据库以 Flyway 为准**：旧 SQLite/Prisma schema 不得直接转换为生产表；正式表结构必须来自 PostgreSQL/Flyway 迁移和领域设计。
5. **权限和留痕前置**：迁移任何旧功能时，必须同步实现权限拦截、数据范围、终端能力校验、幂等、事件留痕和证据附件引用。

## 5. 旧路径迁移映射原则

| 旧路径类型 | 正式替代 |
|---|---|
| `/api/auth/*` | `/api/v1/auth/*` 或 `/api/v1/customer/auth/*`，按员工、商户员工、客人主体拆分 |
| `/api/admin/ticket-*` | `/api/v1/ticketing/*` |
| `/api/admin/bar/*` | `/api/v1/bar/*`、`/api/v1/inventory/*`、`/api/v1/finance/*` |
| `/api/admin/amusement/*` | `/api/v1/amusement/*` |
| `/api/mobile/*` | 不作为正式后端路径；按业务域进入 `/api/v1/terminals`、`/api/v1/ticketing`、`/api/v1/bar`、`/api/v1/amusement`、`/api/v1/evidence` |
| `/api/integration/*` | 平台内部 Adapter 使用 `/api/v1/integration/*`；第三方开放调用使用 `/api/open/*` |

## 6. 首批编码迁移任务

| 优先级 | 任务 | 完成标准 |
|---|---|---|
| P0-0 | 正式后端工程准入 | Java 21 + Spring Boot 4.x + Maven + PostgreSQL + Flyway + springdoc-openapi 工程可启动、可连库、可执行 Flyway baseline、可输出运行时 OpenAPI；未完成前不得把任一业务域标记为正式并行开发完成 |
| P0-1 | 完成 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 工程验收 | `backend/` 初始骨架已出现；仍需在验收环境证明健康检查、springdoc 输出、PostgreSQL 连接和 Flyway baseline 成功 |
| P0-2 | 导入 OpenAPI 契约校验 | `npm run verify:openapi` 可检查静态契约；`npm run verify:openapi-examples` 可检查首批高风险 request/response 机器示例；`npm run verify:operation-matrix` 可检查 operation 验收矩阵与 `docs/openapi/openapi-p0.json` 同步；`npm run verify:openapi-closure` 可检查追溯附录与 OpenAPI 同步；运行时 OpenAPI 与 `docs/openapi/openapi-p0.yaml/json` 在路径、operationId、请求体、响应示例、权限、认证上下文、终端/调用通道、幂等和事件上可比对 |
| P0-3 | 后端按领域实现 Controller/DTO | 不引用旧 TypeScript DTO，不引入 SQLite/Prisma 类型 |
| P0-4 | Web API client 迁移 | 旧 `/api/admin/*`、`/api/integration/*`、`/api/auth/*` 生产调用清零；`npm run verify:p0-contract` 通过 |
| P0-5 | Android/平板/一体机 API client 迁移 | 售票型一体机和展示型一体机按能力开关分流；旧 fixture 不参与正式联调；`TerminalApiClient`、Android App 健康检查和 fixture 不再调用旧路径；`npm run verify:p0-contract` 通过 |
| P0-6 | 自动化接口测试 | 覆盖 `p0-openapi-operation-acceptance-matrix-v0.1.md` 中全部 P0 operation |
| P0-7 | 160 脚本验收 | `complex-deploy-api.sh`、`complex-rollback-api.sh`、`complex-health-check.sh`、`complex-verify-be000.sh`、`complex-verify-p0.sh` 均指向 Java/Spring Boot 后端，不触发旧 `apps/api` |

## 7. 验收标准

### AC-MIG-001 正式技术栈闸口

Given 开发团队开始 P0 后端编码，  
When 检查工程入口、构建脚本、数据库连接和 OpenAPI 输出，  
Then 必须看到 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven + springdoc-openapi，不得以 TypeScript/Fastify/SQLite 工程替代。

### AC-MIG-002 旧路径清零

Given Web、Android、平板、一体机、小程序/App/Web 任一正式联调包，  
When 搜索生产调用路径，  
Then 不得出现非 `/api/v1/*`、非 `/api/open/*` 的正式 `/api/*` 调用；`npm run verify:client-routes` 必须通过；如为注释、历史说明或迁移适配，必须标注 legacy 且不得进入生产调用链。

### AC-MIG-002B 客户端路径必须进入 OpenAPI

Given Web、Android、平板、一体机、小程序/App/Web 任一正式联调包，  
When 扫描其中的 `/api/v1/*` 和 `/api/open/*` 调用，  
Then 每个路径模板和可识别 HTTP 方法必须能在 `docs/openapi/openapi-p0.json` 中匹配；`npm run verify:client-openapi` 必须通过；未进入 OpenAPI 的 P1/P2 或预研页面路径必须从 P0 联调包移除或先补 operation。

### AC-MIG-002C 客户端请求字段必须对齐 OpenAPI

Given Web、Android、平板、一体机、小程序/App/Web 任一正式联调包，  
When 扫描写接口调用和 OpenAPI requestBody schema，  
Then 客户端必须具备出站 snake_case 请求字段适配和入站响应字段适配；正式 OpenAPI 写入请求字段不得出现 camelCase；`npm run verify:client-payload` 必须通过。

### AC-MIG-002D OpenAPI 首批示例必须存在

Given 后端、前端、Android、一体机、小程序/App/Web 或供应商 mock 准备进入 P0 联调，  
When 检查 `docs/openapi/openapi-p0.json`，  
Then 开放鉴权、Webhook、购票、核销、订台、酒吧点单、支付、游乐安全提交、供应商 mock 和通用响应必须具备机器可读 example；`npm run verify:openapi-examples` 必须通过。

### AC-MIG-002E OpenAPI 追溯附录必须同步

Given 后端、前端、Android、一体机、小程序/App/Web、接口自动化或供应商 mock 准备进入 P0 联调，  
When 检查 `docs/p0-openapi-contract-closure-appendix-v0.1.md`，  
Then 每个正式 operation 必须能追溯任务簇、权限、认证上下文、终端/设备能力/调用通道、成功/失败事件和样例准入；`npm run verify:openapi-closure` 必须通过。

### AC-MIG-003 OpenAPI 比对通过

Given Spring Boot 运行时生成 OpenAPI，  
When 与 `docs/openapi/openapi-p0.yaml/json` 和 `p0-openapi-contract-closure-appendix-v0.1.md` 比对，  
Then 路径、方法、operationId、requestBody schema、权限、认证上下文、终端、幂等、事件和追溯附录必须一致。

### AC-MIG-004 旧原型只读参考

Given 开发者参考 `apps/api`、`apps/web` 或 `apps/android-terminal`，  
When 发现旧字段或旧流程与正式文档冲突，  
Then 必须按当前有效入口、OpenAPI、追溯附录、权限矩阵和验收矩阵执行，不得倒逼正式文档迁就旧原型。

### AC-MIG-005 旧脚本阻断

Given 开发者在仓库根目录或 `apps/api` 子目录尝试运行旧 API、Prisma、seed 或 smoke 脚本，  
When 执行 `npm run dev:api`、`npm run prisma:migrate`、`npm run seed:p0` 或 `npm run dev --workspace apps/api` 等命令，  
Then 根目录 API 入口必须转向 `backend/`，旧 `apps/api` 子脚本必须输出 legacy 阻断提示，不得启动 Fastify/SQLite 服务。

## 8. 变更日志

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-06-11 | V0.1 | 新增代码契约差异与迁移审计，明确旧原型、旧路径、正式 Java 技术栈、OpenAPI 契约和首批迁移任务。 |
| 2026-06-11 | V0.1 安全扫描收口 | 将 `security-scans/**` 纳入旧原型可参考/禁止使用范围，明确旧 Node/Fastify/SQLite 安全扫描不得作为当前 Java/Spring/PostgreSQL 安全基线。 |
| 2026-06-12 | V0.1 可编码准入收口修复 | 新增 P0-0 正式后端工程准入；未建立 Java 21 + Spring Boot 4.x + Maven + PostgreSQL + Flyway + springdoc-openapi 工程前，不得把任一业务域标记为正式并行开发完成。 |
| 2026-06-12 | V0.1 旧脚本阻断与后端骨架同步 | 同步 `backend/` 初始骨架已出现；根目录 API 脚本和 `apps/api` 子脚本改为阻断旧原型；新增 AC-MIG-005。 |
| 2026-06-12 | V0.1 160 运维脚本收口 | 新增 D6/P0-7；160 API 部署、回滚、健康检查、BE-000/P0 验收和备份脚本切换为 Java/Spring Boot/PostgreSQL/Flyway 口径。 |
| 2026-06-12 | V0.1 OpenAPI 契约闸口 | P0-2 补 `npm run verify:openapi` 静态契约检查，覆盖旧路径、operationId、认证上下文、权限、终端/调用通道、审计事件和泛化请求体。 |
| 2026-06-12 | V0.1 operation 矩阵同步闸口 | P0-2 补 `npm run verify:operation-matrix`，防止 OpenAPI 已更新但逐接口验收矩阵、权限、事件和终端口径未同步。 |
| 2026-06-12 | V0.1 客户端旧路径闸口 | 新增 `tools/audit-client-api-routes.mjs` 宽松/严格模式；P0-4/P0-5 和 AC-MIG-002 以 `npm run verify:client-routes` 作为正式清零验收。 |
| 2026-06-12 | V0.1 客户端 OpenAPI 覆盖闸口 | 新增 `tools/audit-client-openapi-coverage.mjs`、AC-MIG-002B；已补齐客户端 77 个正式路径模板和 95 个方法级调用的 OpenAPI 覆盖，`verify:client-openapi` 通过。 |
| 2026-06-12 | V0.1 客户端请求字段闸口 | 新增 `tools/verify-client-payload-policy.mjs`、AC-MIG-002C；Web/Android 已具备 OpenAPI 字段名适配，`verify:client-payload` 通过。 |
| 2026-06-13 | V0.1 P0 契约总闸口 | 将 Web/Android 迁移完成判定从散列闸口收口为 `npm run verify:p0-contract`，其展开包含 OpenAPI、示例、operation 矩阵、追溯附录、客户端路径、客户端 OpenAPI 覆盖和客户端请求字段。 |
| 2026-06-13 | V0.1 OpenAPI 示例闸口 | 新增 AC-MIG-002D；把 `verify:openapi-examples` 纳入代码契约迁移审计，防止正式路径和字段都正确但测试、供应商 mock、SDK 仍缺机器样例。 |
| 2026-06-13 | V0.1 OpenAPI 追溯附录闸口 | 新增 AC-MIG-002E；把 `verify:openapi-closure` 纳入代码契约迁移审计，防止旧路径清零后仍缺任务簇、权限、事件、终端或样例准入追溯。 |
