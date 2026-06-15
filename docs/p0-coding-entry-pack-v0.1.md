# P0 可编码入口包 V0.1

## 1. 文档定位

本文档是 P0 编码、联调、测试和供应商 mock 的统一入口包。它不新增业务需求，不替代 PRD，也不重新定义接口；它只回答一个问题：接手项目的人应该从哪些文件开始，按什么顺序开发，哪些旧内容绝对不能作为正式依据。

凡本文档与旧说明、旧原型、历史接口表冲突，按 `00-current-effective-document-entry-v0.1.md`、`unified-standards-baseline-v0.1.md`、`docs/openapi/openapi-p0.yaml`、`p0-openapi-operation-acceptance-matrix-v0.1.md`、`p0-openapi-contract-closure-appendix-v0.1.md` 执行。

## 2. 正式编码唯一入口

| 用途 | 唯一入口 | 不得替代为 |
|---|---|---|
| 正式后端工程 | `backend/` | `apps/api`、Fastify、Prisma、SQLite |
| 正式接口契约 | `docs/openapi/openapi-p0.yaml`、`docs/openapi/openapi-p0.json` | `/api/admin/*`、`/api/mobile/*`、`/api/integration/*` 历史路径 |
| 接口解释 | `p0-api-contract-pack-v0.1.md` | 旧路由代码、旧 smoke 脚本 |
| operation 验收 | `p0-openapi-operation-acceptance-matrix-v0.1.md` | 只看接口能返回 200 |
| operation 追溯 | `p0-openapi-contract-closure-appendix-v0.1.md` | 只在人工矩阵里找权限/事件，或只按模块名排任务 |
| 权限 | `p0-operation-permission-matrix-v0.1.md` | 页面菜单、旧角色名、临时口头授权 |
| 留痕和证据 | `p0-action-event-evidence-matrix-v0.1.md` | 只写业务表、不写事件和证据 |
| 客户端接口迁移闸口 | `tools/audit-client-api-routes.mjs`、`npm run verify:client-routes` | Web/Android 继续调用旧 `/api/*` 非正式路径 |
| 客户端 OpenAPI 覆盖闸口 | `tools/audit-client-openapi-coverage.mjs`、`npm run verify:client-openapi` | 客户端调用虽在 `/api/v1/*` 或 `/api/open/*`，但路径模板或 HTTP 方法未进入 P0 OpenAPI |
| 客户端请求字段闸口 | `tools/verify-client-payload-policy.mjs`、`npm run verify:client-payload` | 路径正确但请求字段仍用旧 camelCase 或绕过 OpenAPI 字段规范 |
| 任务拆分 | `p0-implementation-task-list.md`、`p0-task-api-permission-event-acceptance-map-v0.1.md` | 只按模块名或数据库表排期 |
| 用户故事 | `p0-user-story-map-and-sprint-slices-v0.1.md` | 只按页面清单开发 |
| 端到端验收 | `p0-end-to-end-acceptance-and-edge-cases-v0.1.md` | 只做 happy path |
| 160 运维 | `server-160-runbook-v0.2.md` | 本地临时脚本 |

## 3. 角色阅读顺序

| 角色 | 必读顺序 |
|---|---|
| 后端 | 本文档 -> `backend/README.md` -> `openapi/openapi-p0.yaml` -> `p0-openapi-operation-acceptance-matrix-v0.1.md` -> `p0-implementation-task-list.md` |
| 数据库 | 本文档 -> `foundation-database-design.md` -> `backend/src/main/resources/db/migration/V001__baseline.sql` -> `finance-ar-ap-ledger-development-design-v0.1.md` -> 各业务开发设计 |
| Web/小程序/App | 本文档 -> `p0-api-contract-pack-v0.1.md` -> `openapi/openapi-p0.json` -> `frontend-admin-development-design.md` / `customer-app-miniapp-development-design-v0.1.md` |
| Android/平板/一体机 | 本文档 -> `android-device-terminal-development-design.md` -> `terminal-page-product-prd-v0.1.md` -> `openapi/openapi-p0.json` |
| 测试 | 本文档 -> `p0-openapi-operation-acceptance-matrix-v0.1.md` -> `p0-end-to-end-acceptance-and-edge-cases-v0.1.md` -> `p0-action-event-evidence-matrix-v0.1.md` |
| 第三方供应商 | 本文档 -> `third-party-supplier-integration-kit-v0.1.md` -> `openapi/openapi-p0.yaml` -> `third-party-integration-development-design.md` |
| 产品/运营 | 本文档 -> `00-current-effective-document-entry-v0.1.md` -> `p0-user-story-map-and-sprint-slices-v0.1.md` -> 对应业务 PRD |

## 4. P0 编码流水线

每个 P0 功能必须按以下顺序推进：

1. 在 `p0-user-story-map-and-sprint-slices-v0.1.md` 找到用户故事和业务旅程。
2. 在 `p0-implementation-task-list.md` 找到任务编号、依赖和里程碑。
3. 在 `openapi/openapi-p0.yaml/json` 找到 operationId、路径、请求体、响应、认证上下文、权限、终端、幂等和事件。
4. 在 `p0-openapi-operation-acceptance-matrix-v0.1.md` 找到验收标准和边界情况。
5. 在 `p0-openapi-contract-closure-appendix-v0.1.md` 找到该 operation 的权限、认证上下文、事件、任务簇、终端和样例准入要求。
6. 在 `p0-operation-permission-matrix-v0.1.md` 找到权限点、模板规则和高风险审批要求；若人工矩阵未列出某接口级权限码，以追溯附录为补充事实源。
7. 在 `p0-action-event-evidence-matrix-v0.1.md` 找到高风险证据和复核路径；接口级完整成功/失败事件名以追溯附录为准。
8. 在 `backend/` 实现 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 代码。
9. 用运行时 springdoc 输出与 `docs/openapi/openapi-p0.yaml/json` 做差异校验。

## 5. 强制反模式

以下行为一律视为 P0 不合格：

| 反模式 | 处理 |
|---|---|
| 启动 `apps/api` 作为正式后端 | 阻断；只能阅读历史原型 |
| 将 `apps/api` 重新加入根 npm workspace 或 lockfile | 阻断；正式后端只认 `backend/` |
| 从 `dev.db`、`schema.prisma` 或旧 TypeScript DTO 生成正式表结构 | 阻断；正式库只认 PostgreSQL + Flyway |
| 让后端兼容 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*` 作为正式路径 | 阻断；必须先进入 OpenAPI |
| 前端、Android、一体机或供应商 mock 在正式联调包中继续调用非 `/api/v1/*`、非 `/api/open/*` 路径 | 阻断；必须迁移 API client 并通过 `npm run verify:client-routes` |
| 前端、Android、一体机或供应商 mock 调用未进入 OpenAPI 的 `/api/v1/*` 路径 | 阻断；必须补 OpenAPI operation 或从 P0 客户端移除 |
| 新增或恢复 `ClientPlaceholderRequest`、通用占位请求体或无字段 DTO | 阻断；正式 P0 OpenAPI 写接口必须使用字段级 requestBody schema |
| 写接口没有字段级 requestBody schema | 阻断；不得用 `GenericWrite` 或通用对象 |
| 客户端写接口绕过字段名规范化，或正式 OpenAPI request schema 出现 camelCase 字段 | 阻断；P0 请求体字段以 OpenAPI snake_case 为准，Web/Android 可在客户端适配层转入/转出，但不得直接裸发旧字段 |
| 只做角色菜单控制，没有 operation 权限和数据范围 | 阻断；以权限矩阵为准 |
| 只改业务状态，不写事件、证据和审计 | 阻断；以事件证据矩阵为准 |
| 第三方供应商字段未确定时写死私有字段进业务主模型 | 阻断；只能进适配器或映射占位 |
| 高价值酒水、售酒年龄核验、支付退款、人工放行离线通过 | 阻断；P0 高风险动作保持在线或进入复核 |
| 把历史能力表中的旧路径、能力路径或相对路径直接做成 Controller、SDK/mock、接口自动化 | 阻断；必须先映射到 OpenAPI exact path 或 operationId，并通过 operation 矩阵和追溯附录校验 |
| 在 `local/dev/test` P0 smoke 切片上继续堆生产业务逻辑 | 阻断；该切片只用于 160 自动化 smoke 和 Android 流程保活，正式业务必须迁移到 PostgreSQL + Flyway + Service/Repository + 权限/事件/审计 |

## 6. 生产前输入项

以下事项不是新的产品拍板问题，但未完成时不得判定生产就绪：

| 输入项 | 可先做 | 生产前必须补齐 |
|---|---|---|
| 聚合支付 | 接口抽象、mock、对账框架 | 商户号、密钥、回调、对账文件、退款权限 |
| MDM/Android Enterprise | 设备绑定、能力开关、测试策略 | 真实 MDM 方案、设备 enroll、策略下发证据 |
| 酒吧年龄/人脸核验 | 适配器、mock、供应商状态字段 | 真实供应商或等效设备、失败/拒绝/复核证据 |
| NAS 备份 | 脚本、Runbook、台账 | NAS 账号、路径、容量、保留周期、恢复演练 |
| 第三方 SDK/API | 统一接口、adapter、webhook、轮询校验 | 字段映射、签名、错误码、测试账号、补偿策略 |
| Maven/Gradle Wrapper | P0 可先使用系统 Maven 3.9+ 与系统 Gradle 通过环境校验 | P1 工程化增强补齐 Maven Wrapper 与 Android Gradle Wrapper，降低换机、CI 和供应商交接成本 |
| Android P0 smoke 专用账号 | Android 核心构建、APK 壳工程和接口 mock | 种子 P0 测试管理员账号、`P0_ADMIN_USERNAME`、`P0_ADMIN_PASSWORD`、鉴权接口和核心 P0 业务接口；不得使用真实生产管理员密码 |
| P0 smoke 后端切片 | 可在 `local/dev/test` profile 下用内存态数据跑通 Android 自动化 | 生产级业务必须替换为正式数据库表、迁移、Service/Repository、权限、事件和审计；不得把 smoke 切片当生产实现 |

## 7. 完成定义

一个 P0 功能只有同时满足以下条件，才允许标记为“可验收”：

1. 代码落在正式 Java/Spring Boot/PostgreSQL/Flyway 工程。
2. operationId、权限、认证上下文、终端、幂等、事件与 OpenAPI 和验收矩阵一致。
3. 至少覆盖成功、失败、无权限、状态冲突、重复请求、第三方不可用或人工复核中的适用场景。
4. 写操作有审计事件；高风险操作有证据或复核记录。
5. 前端、Android、一体机或供应商 mock 不再调用旧正式路径。
6. 高风险写接口和外部接入接口已有机器可读示例，并通过 `npm run verify:openapi-examples`。
7. 前端、Android、一体机或供应商 mock 写接口请求字段与 OpenAPI requestBody schema 一致，并通过 `npm run verify:client-payload`。
8. 160 或等效验收环境可运行，API 服务健康检查通过，并能导出运行时 OpenAPI；仅项目同步、依赖安装、契约通过或 jar 打包通过不得替代运行态验收。
9. 若功能仍由 `local/dev/test` smoke 切片承接，只能标记为“自动化 smoke 已通过”，不得标记为“业务生产实现完成”。

## 8. 160 执行脚本

| 脚本 | 用途 | 强制口径 |
|---|---|---|
| `ops/160/complex-verify-be000.sh` | 检查 Java 21、Maven、Spring Boot 4.x、Flyway baseline、打包 jar、运行时健康检查和 OpenAPI 输出 | 不得跳过 BE-000 |
| `ops/160/complex-runtime-config-check.sh` | 检查 160 API 发布/回滚前运行配置、数据库 URL 形态、数据库凭证、PostgreSQL 可连接性和基础容器状态，且不打印密钥 | `COMPLEX_DATABASE_URL` 必须为 `jdbc:postgresql://host:port/database`，不得内嵌账号密码；不得依赖后端默认数据库密码隐式兜底；该脚本失败不得发布或回滚 API |
| `ops/160/complex-deploy-api.sh` | 构建并发布正式 Java API jar，优先使用 systemd 管理 `complex-api.service` | 不得启动 `apps/api` |
| `ops/160/complex-rollback-api.sh` | 先执行运行配置预检，再回滚到指定 Java API release | release 必须包含 `complex-api.jar`；回滚不得绕过数据库账号/密码、PostgreSQL 可达性和基础容器状态检查 |
| `ops/160/complex-health-check.sh` | 检查 `/actuator/health`、`/api/v1/system/health`、`/docs/openapi`、Web 和基础容器 | 不得使用旧 `/api/health` 作为默认入口 |
| `ops/160/complex-verify-p0.sh` | 执行 OpenAPI、OpenAPI 示例、operation 矩阵同步、OpenAPI 权限/事件/任务追溯附录同步、客户端旧路径、客户端 OpenAPI 覆盖、客户端请求字段、BE-000、Web 构建、Android 环境校验、Android 核心构建、smoke、APK 和整体健康检查；脚本读取 `/etc/complex/complex.env` 与 `~/.complex/complex.env`，并在 Android smoke 前检查 `P0_ADMIN_USERNAME`、`P0_ADMIN_PASSWORD` | 不得触发旧 Node API smoke；不得跳过 Android；不得绕过 `verify:openapi-examples`、`verify:operation-matrix`、`verify:openapi-closure`、`verify:client-routes`、`verify:client-openapi`、`verify:client-payload`、`verify:android-env` 或 Android smoke 专用种子账号；不得使用真实生产管理员密码 |
| `ops/160/complex-backup.sh` | 备份 PostgreSQL、MinIO、NATS、Redis、配置和文档 | 不得把旧 SQLite 原型库作为正式备份资产 |

## 9. 本机契约闸口

| 命令 | 用途 |
|---|---|
| `npm run verify:openapi` | 检查 `docs/openapi/openapi-p0.json` 是否存在旧路径、缺 operationId、缺认证上下文、缺权限、缺终端/调用通道、缺审计事件或 `GenericWrite` |
| `npm run docs:openapi-examples` | 将 P0 高风险写接口和外部接入接口首批 request/response example 写入 OpenAPI JSON/YAML |
| `npm run verify:openapi-examples` | 严格阻断 OpenAPI 缺少 P0 首批机器示例的联调包；覆盖开放鉴权、Webhook、购票、核销、订台、酒吧点单、支付、游乐安全提交、供应商 mock 和通用响应 |
| `npm run audit:client-routes` | 宽松扫描 Web/Android 客户端中仍存在的非正式 `/api/*` 路径，用于迁移盘点 |
| `npm run verify:client-routes` | 严格阻断 Web/Android 正式联调包中的旧接口路径；仅允许 `/api/v1/*` 和 `/api/open/*` |
| `npm run audit:client-openapi` | 宽松扫描 Web/Android 客户端正式命名空间路径是否已被 `openapi-p0.json` 覆盖 |
| `npm run verify:client-openapi` | 严格阻断未进入 OpenAPI 的客户端正式路径和 HTTP 方法 |
| `npm run audit:client-payload` | 宽松检查 Web/Android 客户端是否具备请求字段规范化策略，并检查 OpenAPI 写入请求字段是否保持 snake_case |
| `npm run verify:client-payload` | 严格阻断路径正确但请求字段与 OpenAPI 字段规范不一致的客户端联调包 |
| `npm run docs:repair-openapi-text` | 修复 OpenAPI 中不可执行的连续问号占位摘要、描述和参数说明，并同步生成 JSON/YAML |
| `npm run docs:operation-matrix` | 按当前 OpenAPI 重新生成 `p0-openapi-operation-acceptance-matrix-v0.1.md` |
| `npm run verify:operation-matrix` | 严格阻断 operation 验收矩阵与 `openapi-p0.json` 不一致的 P0 联调包 |
| `npm run docs:openapi-closure` | 按当前 OpenAPI 重新生成 `p0-openapi-contract-closure-appendix-v0.1.md` |
| `npm run verify:openapi-closure` | 严格阻断 OpenAPI 权限、事件、任务追溯附录与 `openapi-p0.json` 不一致的 P0 联调包 |
| `npm run verify:p0-contract` | 执行 P0 可在普通 Node 环境先验收的契约闸口：OpenAPI、示例、operation 矩阵、追溯附录、客户端旧路径、客户端 OpenAPI 覆盖和客户端请求字段 |
| `npm run verify:p0-web` | 执行 `verify:p0-contract` 后再构建 Web；适合前端、测试和供应商 mock 在后端/Android 环境未就绪时先确认联调包 |
| `npm run verify:backend-env` | 检查正式后端前置环境：Java 21+、Maven Wrapper 或 Maven 3.9+、`backend/pom.xml`；未通过时不得进入 BE-000 或 P0 后端验收 |
| `npm run verify:android-env` | 检查 Android P0 前置环境：Java 21+、Gradle Wrapper 或系统 Gradle、Android SDK；未通过时不得进入 Android 构建、smoke 或 APK 验收 |
| `npm run verify:p0-env` | 只执行后端和 Android 环境前置校验；适合 IT/运维先判断机器是否具备 BE-000 和 Android APK 实跑条件 |
| `npm run verify:p0` | 先执行 OpenAPI 契约闸口、OpenAPI 示例闸口、operation 验收矩阵同步闸口、OpenAPI 权限/事件/任务追溯附录闸口、客户端接口迁移闸口、客户端 OpenAPI 覆盖闸口和客户端请求字段闸口，再进入 BE-000、Web、Android 环境和 Android 构建/APK 验证 |

开放平台和 Webhook 接口不伪装成终端；它们必须声明 `x-invocation-channel`。员工、客人、设备和页面类接口必须声明 `x-terminal-allowed`，且只能使用统一口径中的 10 个终端码。

## 10. 变更日志

| 日期 | 版本 | 变更 |
|---|---|---|
| 2026-06-12 | V0.1 | 新增 P0 可编码入口包，集中承接正式后端入口、OpenAPI、operation 验收、权限、留痕、任务映射和旧原型隔离规则 |
| 2026-06-12 | V0.1 160 脚本收口 | 补充 160 BE-000、部署、回滚、健康检查、P0 验证和备份脚本的正式 Java 口径 |
| 2026-06-12 | V0.1 OpenAPI 闸口 | 新增 `npm run verify:openapi` 契约检查，并纳入 P0 验证链路 |
| 2026-06-12 | V0.1 npm 工作区收口 | 根 npm workspace 和 lockfile 移除旧 `apps/api`、Fastify、Prisma 依赖 |
| 2026-06-12 | V0.1 客户端迁移闸口 | 新增 `npm run audit:client-routes` 和 `npm run verify:client-routes`；P0 严格验收阻断 Web/Android 旧接口路径 |
| 2026-06-12 | V0.1 客户端 OpenAPI 覆盖闸口 | 新增 `npm run audit:client-openapi` 和 `npm run verify:client-openapi`；客户端路径模板和可识别 HTTP 方法必须进入 `openapi-p0.json` 才能通过 P0 严格验收 |
| 2026-06-12 | V0.1 客户端请求字段闸口 | 新增 `npm run audit:client-payload` 和 `npm run verify:client-payload`；Web/Android 必须具备 OpenAPI 字段名适配，正式 request schema 不得出现 camelCase 字段 |
| 2026-06-12 | V0.1 OpenAPI 可读文本修复 | 新增 `docs:repair-openapi-text` 和 `docs:operation-matrix`；OpenAPI 摘要/描述不得出现连续问号占位，operation 验收矩阵必须由当前 OpenAPI 生成 |
| 2026-06-12 | V0.1 operation 矩阵同步闸口 | 新增 `npm run verify:operation-matrix` 并纳入 `verify:p0` 与 `ops/160/complex-verify-p0.sh`；OpenAPI 变更后未重建验收矩阵将直接阻断 |
| 2026-06-13 | V0.1 OpenAPI 示例闸口 | 新增 `docs:openapi-examples` 和 `verify:openapi-examples` 并纳入 `verify:p0`；高风险写接口和外部接入接口必须具备机器可读示例，供应商 mock 和接口测试不得只靠口头样例 |
| 2026-06-13 | V0.1 OpenAPI 权限/事件/任务追溯附录闸口 | 新增 `docs:openapi-closure` 和 `verify:openapi-closure` 并纳入 `verify:p0`；OpenAPI 变更后权限、事件、任务追溯附录未重建将直接阻断 |
| 2026-06-13 | V0.1 Android 环境闸口 | 新增 `verify:android-env` 和统一 Android Gradle 运行器；P0 验收不得在 Gradle 或 Android SDK 缺失时跳过 Android 构建和 APK |
| 2026-06-13 | V0.1 P0 验证分层 | 新增 `verify:p0-contract`、`verify:p0-web`、`verify:p0-env`；将文档/契约、Web、环境和完整 P0 验收拆开，避免环境缺失掩盖已可先验的交付物 |
| 2026-06-13 | V0.1 160 与历史能力表收口 | 明确 160 运行态验收不得被同步/打包替代；历史能力表不得绕过 OpenAPI operationId、验收矩阵和追溯附录直接进入开发 |
| 2026-06-13 | V0.1 160 运行配置预检 | 新增 `complex-runtime-config-check.sh` 和 `ops/160/complex.env.example`，并纳入 API 发布前置，阻断缺数据库账号/密码、数据库 URL 非 JDBC 形态、PostgreSQL 不可达或基础容器缺失的运行态发布 |
| 2026-06-13 | V0.1 160 回滚预检 | `complex-rollback-api.sh` 纳入运行配置预检，阻断缺数据库账号/密码、PostgreSQL 不可达或基础容器缺失时把回滚误判为成功 |
| 2026-06-13 | V0.1 Android smoke 账号闸口 | `complex-verify-p0.sh` 读取 160 环境文件并要求 `P0_ADMIN_USERNAME`、`P0_ADMIN_PASSWORD`；缺专用种子测试账号时完整 P0 正确阻断，不得用生产管理员密码或跳过 smoke 标记通过 |
| 2026-06-13 | V0.1 完整 P0 smoke 通过 | 160 完整 `complex-verify-p0.sh` 已通过；新增 smoke 切片边界，明确其只保活自动化验收，不替代生产级业务实现 |
