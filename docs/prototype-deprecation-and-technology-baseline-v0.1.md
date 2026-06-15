# 原型废止与正式技术栈基线说明 V0.1

## 1. 文档定位

本文档用于把综合体管理系统的历史预研原型和正式开发路线切开，防止产品、开发、测试、安全审计、供应商或外部机器智能体继续把旧 TypeScript/SQLite 后端当成正式系统。

本文档不删除历史文件；它只规定哪些内容可参考、哪些内容必须废止、正式开发应从哪里开始，以及迁移或归档时必须满足的准入条件。旧 `apps/api` 目录已通过 `LEGACY-PROTOTYPE.md` 和阻断型 `package.json` 脚本做物理隔离，正式后端入口统一为 `backend/`。

## 2. 强制结论

| 项 | 当前结论 |
|---|---|
| 正式后端技术栈 | Java 21 + Spring Boot 4.x |
| 正式构建工具 | Maven |
| 正式数据库 | PostgreSQL |
| 正式数据库迁移 | Flyway |
| 正式运行环境 | 160 服务器或后续生产环境按 Runbook 部署 |
| P0 规则处理 | Java 内置规则处理器 |
| P2 扩展 | 独立 Python AI 服务可后置引入 |
| 废止后端路线 | TypeScript / Fastify / Prisma / SQLite |

任何与本表冲突的旧代码、旧脚本、旧安全扫描、旧说明和旧任务口径，均不得作为正式开发、测试、验收、部署和安全审计依据。

## 3. 当前识别到的历史原型痕迹

| 路径或内容 | 当前定性 | 允许用途 | 禁止用途 |
|---|---|---|---|
| 根目录 `package.json` / `package-lock.json` | 历史 Node 工作区脚本 | 追溯旧原型启动方式 | 不得作为正式后端构建入口 |
| 根目录 `tsconfig.base.json` | 历史 TypeScript 工作区配置 | 追溯旧前后端预研 | 不得作为正式后端编译入口 |
| `apps/api/package.json` | 历史 Fastify API 原型的阻断脚本入口，且已从根 npm workspace 摘除 | 提醒开发、测试、安全审计和供应商旧原型不得运行 | 不得恢复为正式 API 启动、构建、seed、smoke 或 Prisma 脚本；不得重新写回根 workspace 或 lockfile |
| `apps/api/LEGACY-PROTOTYPE.md` | 旧后端目录物理标识 | 提醒开发、测试、安全审计和供应商此目录为历史预研 | 不得删除该提示后继续按旧后端开发 |
| `apps/api/src/**` | 历史 TypeScript 后端代码 | 参考接口命名、流程草稿和测试样例 | 不得直接迁入生产 |
| `apps/api/prisma/schema.prisma` | 历史 Prisma/SQLite 数据模型 | 只可作为数据对象对照材料 | 不得作为正式数据库设计 |
| `apps/api/dev.db`、`apps/api/prisma/dev.db` | 历史 SQLite 原型数据 | 只可作为样例数据来源 | 不得作为正式数据库、测试库或迁移源 |
| `security-scans/**`、`security-scans/LEGACY-SECURITY-SCANS.md` | 基于旧原型的安全扫描报告仅作历史追溯；`security-scans/threat_model.md` 已替换为正式安全基线重建入口 | 追溯旧问题和安全审计方法 | 不得作为当前 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 安全结论；正式 Java 后端落地后必须重做安全基线 |
| `ops/160/*.sh` | 160 运维脚本 | 正式 Java 后端部署、回滚、健康检查、BE-000/P0 验收和生产备份 | 不得启动旧 `apps/api`，不得把 SQLite 原型库作为正式备份资产 |
| `apps/web/**` | Web 前端原型或页面实现 | 可按当前 PRD/接口契约复核后复用 UI 经验 | 不得以旧 mock 接口反向约束正式后端 |
| `apps/android-terminal/**` | Android 工作手机/平板/一体机端侧预研代码 | 可参考端侧扫码、fixture、设备交互和页面经验 | 不得沿用旧 `/api/auth/*`、`/api/admin/*`、`/api/mobile/*` 路径或绕过设备绑定/能力开关 |

说明：本文废止的是旧后端路线，不等于废止所有 TypeScript。Web 前端仍可使用 TypeScript/React/Vite 等前端技术，但必须以当前 PRD、API Contract Pack、权限矩阵和验收标准为准。

## 4. 正式后端基线

### 4.1 工程结构

正式后端工程应使用 Maven 多模块或单模块 Spring Boot 工程。P0 推荐先用单模块 Spring Boot 起步，按领域包组织，避免在业务边界未稳定前过早拆微服务。

建议结构：

```text
backend/
  pom.xml
  src/main/java/
    .../platform/
    .../ticketing/
    .../bar/
    .../amusement/
    .../inventory/
    .../finance/
    .../openapi/
    .../evidence/
  src/main/resources/
    application.yml
    db/migration/
```

### 4.2 数据库

正式数据库只使用 PostgreSQL。所有表结构、索引、约束、枚举、初始数据和变更脚本必须通过 Flyway 管理。

禁止行为：

1. 直接把 Prisma schema 当成正式表结构。
2. 直接把 SQLite 数据库转换成生产库。
3. 绕过 Flyway 手工改正式库。
4. 在 P0 表结构里写死第三方供应商私有字段。

### 4.3 API

正式 API 必须从 `p0-api-contract-pack-v0.1.md`、`docs/openapi/openapi-p0.yaml/json`、`p0-openapi-operation-acceptance-matrix-v0.1.md` 和 `p0-openapi-contract-closure-appendix-v0.1.md` 生成或对齐。旧 Fastify 路由只能作为业务流程参考，不能作为正式接口契约；未通过 `npm run verify:openapi-closure` 的能力不得生成 Controller、SDK/mock、接口自动化或供应商联调文档。

API 基线：

| 项 | 规则 |
|---|---|
| 内部 API | Spring Boot Controller / Service / Repository 分层 |
| 开放平台 API | 统一 `/api/open/...` 路径、签名、幂等、时间窗、nonce |
| 第三方接入 | 供应商 Adapter，不污染业务主模型 |
| 错误码 | 统一错误码、业务拒绝原因和审计事件 |
| 验收 | 每个接口必须映射字段级 requestBody、权限、认证上下文、终端、幂等、事件、追溯附录和 Given/When/Then |

### 4.4 规则处理与 AI

P0 只做 Java 内置规则处理器，覆盖票务资格、权益核销、权限判断、风险规则、基础质检、对账差异和告警触发。

独立 Python AI 服务只作为 P2 扩展，必须通过明确接口调用，不得在 P0 阶段把核心业务判断外包给未成型 AI 服务。

## 5. 旧原型迁移使用规则

旧原型可以被阅读，但必须按以下规则处理：

| 场景 | 允许 | 不允许 |
|---|---|---|
| 业务理解 | 阅读旧代码辅助理解早期流程 | 把旧代码行为当成最终需求 |
| 数据模型 | 对照字段名称和对象关系 | 复制 Prisma schema 到正式库 |
| 接口设计 | 参考旧路由覆盖了哪些场景 | 继续沿用旧路径、旧错误码、旧鉴权 |
| Web/Android 迁移 | 按 `code-contract-difference-and-migration-audit-v0.1.md` 做旧路径替换和 API client 重接 | 让后端兼容旧 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*` 正式路径 |
| 测试样例 | 参考旧 smoke 脚本里的业务链路 | 用旧脚本判定 P0 已通过 |
| 安全审计 | 参考旧发现类型 | 沿用旧威胁模型作当前结论 |
| 前端页面 | 参考界面布局和交互经验 | 用旧 mock 数据反推正式接口 |

## 6. 归档与标注要求

在正式 Java 工程创建后，应执行以下整理动作：

| 优先级 | 动作 | 目的 |
|---|---|---|
| P0 | 将 `apps/api` 标注为 legacy prototype 或迁入归档目录 | 防止误启动旧后端 |
| P0 | 根目录正式启动脚本改向 Java/Maven | 防止新开发者运行旧 Node API |
| P0 | 将旧安全扫描标注为 legacy scan | 防止安全结论错配 |
| P0 | 新增 Java/PostgreSQL/Flyway 安全审计基线 | 替换旧 Node/SQLite 威胁模型 |
| P1 | 盘点旧前端是否仍依赖旧 API mock | 避免前端接口反向污染后端契约 |
| P1 | 将可复用样例数据转为 PostgreSQL seed 设计 | 支持正式测试数据 |

当前执行状态：`apps/api` 已标注为 legacy prototype，且 `apps/api/package.json` 的运行、构建、Prisma、seed 和 smoke 脚本均已改为阻断提示；根 npm workspace 和根 lockfile 已移除旧 API、Fastify、Prisma 依赖；根目录 `dev`、`dev:api`、`smoke:p0`、`verify:p0` 已改向 `backend/` 或正式后端测试；160 API 发布、回滚、健康检查、BE-000/P0 验收和备份脚本已切换为 Java/Spring Boot/PostgreSQL/Flyway 口径。若后续决定物理迁入归档目录，仍须保留原型用途、废止原因、可参考范围、禁止用途和正式替代入口。

归档不是简单删除。所有历史原型在归档前应保留一份说明：原型用途、废止原因、可参考范围、禁止用途、正式替代入口。

## 7. 验收标准

### AC-TECH-001 正式技术栈唯一

Given 新开发者或外部供应商阅读交付包，  
When 其查看当前有效入口、交付包说明、统一口径基准和项目进度看板，  
Then 只能得出一个结论：正式后端为 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven。

### AC-TECH-002 旧原型不得误用

Given 仓库中仍存在 `apps/api`、SQLite 数据库、Prisma schema 或旧安全扫描，  
When 开发、测试、安全审计或供应商准备进入 P0 实施，  
Then 这些内容必须被识别为历史预研原型，不得作为正式实现、验收或安全结论。

### AC-TECH-003 旧代码可参考但不可支配正式设计

Given 旧 TypeScript 后端中存在某个接口、状态或字段，  
When 正式 Java 后端设计与旧实现不一致，  
Then 以当前 PRD、统一口径基准、API Contract Pack、数据库设计和验收标准为准。

### AC-TECH-004 前端 TypeScript 不被误伤

Given 文档废止 TypeScript/Fastify/Prisma/SQLite 后端路线，  
When 前端团队评估 `apps/web` 或后续 Web 前端实现，  
Then 不得把“废止后端 TypeScript 原型”误解为禁止前端使用 TypeScript。

### AC-TECH-005 安全审计切换

Given 旧安全扫描基于 Node/Fastify/SQLite 原型，  
When 正式 Java/PostgreSQL 工程建立后，  
Then 必须重新建立 Java 21 + Spring Boot 4.x/PostgreSQL/Flyway/MDM/开放平台/支付/NAS 备份相关的安全审计基线。

## 8. 与其他文档的关系

| 文档 | 关系 |
|---|---|
| `00-current-effective-document-entry-v0.1.md` | 当前第一阅读入口，引用本文作为技术路线收口文档 |
| `unified-standards-baseline-v0.1.md` | D10 是跨文档权威口径，本文展开 D10 的执行细则 |
| `system-development-spec.md` | 总体技术方案必须按本文修正旧口径 |
| `java-backend-platform-foundation-development-design.md` | Java 后端地基设计必须固定 Maven |
| `foundation-database-design.md` | PostgreSQL/Flyway 表结构和迁移是正式数据库依据 |
| `server-160-runbook-v0.2.md` | 160 部署和运维脚本必须按正式技术栈执行 |
| `p0-api-contract-pack-v0.1.md` | 后续新增后作为正式接口契约入口 |
| `p0-openapi-operation-acceptance-matrix-v0.1.md` | operation 级验收矩阵，支撑接口测试、权限测试、幂等测试和事件留痕测试 |
| `p0-openapi-contract-closure-appendix-v0.1.md` | operation 到任务簇、权限、事件、终端和样例准入的追溯附录 |
| `code-contract-difference-and-migration-audit-v0.1.md` | 旧原型、旧 Web/Android 路径和正式 Java/OpenAPI 契约的迁移闸口 |
| `p0-coding-entry-pack-v0.1.md` | P0 编码、联调、测试和供应商 mock 的统一入口，说明正式工程、契约、验收和旧原型阻断规则 |

## 9. 变更日志

| 日期 | 版本 | 变更 |
|---|---|---|
| 2026-06-11 | V0.1 执行收口 | 在 `apps/api/LEGACY-PROTOTYPE.md` 增加物理标识；正式后端只认 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven + springdoc-openapi；旧 `apps/api` 不得作为启动、部署、联调或安全审计入口 |
| 2026-06-12 | V0.1 技术栈修订 | 总控拍板 Spring Boot 使用 4.x；正式后端骨架同步为 Spring Boot 4.1.0 |
| 2026-06-11 | V0.1 可编码补强 | 将 `apps/android-terminal` 旧路径和 `apps/web` 旧接口调用纳入迁移闸口；正式接口以字段级 OpenAPI 和 operation 验收矩阵为准 |
| 2026-06-11 | V0.1 安全扫描标识 | 新增 `security-scans/LEGACY-SECURITY-SCANS.md`，明确旧扫描仅作历史追溯，正式 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 工程落地后重做安全基线 |
| 2026-06-13 | V0.1 安全基线入口替换 | `security-scans/threat_model.md` 已从旧原型威胁模型替换为正式安全基线重建入口，禁止旧 Node/SQLite 威胁模型正文参与当前上线判断 |
| 2026-06-10 | V0.1 | 新增原型废止与正式技术栈基线说明，明确旧 TypeScript/Fastify/Prisma/SQLite 后端仅为历史预研原型，正式路线固定 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven |
| 2026-06-12 | V0.1 旧脚本阻断 | 将根目录 API 脚本和 `apps/api` 子脚本改为阻断旧 TypeScript/Fastify/SQLite 原型，正式后端入口统一为 `backend/` |
| 2026-06-12 | V0.1 160 运维脚本收口 | 160 API 部署、回滚、健康检查、BE-000/P0 验收和备份脚本切换为正式 Java 后端口径 |
| 2026-06-12 | V0.1 npm 工作区收口 | 根 npm workspace 和 lockfile 移除旧 `apps/api`、Fastify、Prisma 依赖，旧 API 子目录仅保留阻断提示和历史参考 |
| 2026-06-13 | V0.1 OpenAPI 追溯附录闸口 | 正式 API 依据补入 `p0-openapi-contract-closure-appendix-v0.1.md` 和 `verify:openapi-closure`；旧原型能力未进入追溯附录不得生成 Controller、SDK/mock、接口自动化或供应商联调 |
