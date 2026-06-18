# 交付包说明 V0.1（delivery-package-manifest）

> **本文档为什么新增（定位）：** 两轮审计发现全目录无任何"打包发给其他机器智能体"的交付包说明（deliver-1）。原始需求 §20 明确要求"所有相关项目内容需要打包，方便发给其他机器智能体做进一步完善和评估"。本文给出交付包文件清单、目录结构、阅读起点、打包方式、版本日期，以及给外部智能体的引导，使任何不了解历史会话的机器智能体也能据此理解并继续完善本项目。
>
> **依据：** finding deliver-1；原始需求 `original-requirements-capture-v0.1.md` §20 REQ-89。

## 1. 版本与生成信息

| 项 | 值 |
|---|---|
| 交付包名 | 综合体管理系统文档交付包 |
| 版本 | V0.1 |
| 生成日期 | 2026-06-12 |
| 当前第一阅读入口 | `00-current-effective-document-entry-v0.1.md` |
| 单一权威口径 | `unified-standards-baseline-v0.1.md`（D1–D13，凡冲突以它为准） |
| P0 机器可读接口契约 | `openapi/openapi-p0.yaml`、`openapi/openapi-p0.json` |
| 唯一权威总索引 | `system-capability-layers-product-index-v0.1.md` |
| 唯一权威线程登记 | `thread-registry.md` |
| 正式技术栈 | Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven（D10/A1，2026-06-12 修订） |
| 可编码配套工具 | 根 `package.json` scripts、`tools/*.mjs`、`ops/160/*.sh`、`backend/` |

> 说明：交付包根目录为 `complex-management-system/docs/`。当前活跃交付包共 **81 个文件 = 77 个 Markdown + 2 个交付 HTML + 1 个 OpenAPI YAML + 1 个 OpenAPI JSON**；其中主目录活跃入口为 **79 个文件 = 77 个 Markdown + 2 个 HTML**。已丢弃不再使用的旧技术路线快照、旧进度快照和相关历史审计材料；保留的归档文件只作为历史证据，不作为当前产品、开发或验收入口。
>
> 可编码交付口径：上面的 81 个文件是“文档包”计数，不包含根脚本、OpenAPI 生成/校验工具、160 运维脚本、Android/后端运行器和 `backend/` 正式后端骨架。外部智能体或开发团队接手编码时，必须同时读取 `package.json`、`tools/run-backend-maven.mjs`、`tools/run-android-gradle.mjs`、`tools/apply-openapi-p0-examples.mjs`、`tools/verify-openapi-p0-examples.mjs`、`tools/generate-openapi-operation-matrix.mjs`、`tools/generate-openapi-contract-closure-appendix.mjs`、`tools/verify-openapi-contract.mjs`、`ops/160/*.sh`、`backend/README.md`、`apps/android-terminal/README.md`、`mdm-android-enterprise-implementation-plan-v0.1.md`、`production-credential-rotation-runbook-v0.1.md`、`nas-backup-and-restore-runbook-v0.1.md`、`nas-it-controlled-mount-handoff-v0.1.md`、`nas-diagnostic-report-v0.1.md`、`external-dependency-gates-v0.1.md`、`external-evidence-requirement-traceability-v0.1.md` 和 `delivery-readiness-gates-v0.1.md`；交付前必须运行 `npm run verify:delivery-gates`。否则会漏掉后端/Android 环境前置校验、契约闸口、示例闸口、OpenAPI 权限/事件/任务追溯闸口、外部依赖阻断、BE-000 环境准入、MDM、密钥轮换、NAS 备份恢复和 160 部署口径。
>
> 计数口径提示：下方多条“修订说明”保留的是当时阶段的历史文件数，用于追溯文档演进；当前交付数量一律以上一段“当前活跃交付包共 81 个文件”和最后一条 2026-06-13 生产前置薄 Runbook 收口说明为准。
>
> 修订说明（2026-06-08 复核修复）：本改进包实际为 100 个文件（94 Markdown + 2 HTML + 4 TXT）。上一版仍写 99 个文件/3 个审查明细 TXT，漏计 `审查发现明细-第三轮复核.txt`，现据实修正。
>
> 修订说明（2026-06-08 全量复核）：新增 `documentation-full-audit-and-fix-report-20260608.md`，文件计数更新为 101（95 Markdown + 2 HTML + 4 TXT），该报告作为当前复核结论入口。
>
> 修订说明（2026-06-08 Product Design 复核）：新增 `documentation-product-flow-audit-and-fix-report-20260608.md`，文件计数更新为 102（96 Markdown + 2 HTML + 4 TXT），用于说明产品流、角色旅程、页面字段验收与追溯矩阵旧状态收口。
>
> 修订说明（2026-06-09 对抗式市场基准复核）：当前本地 docs 目录已增至 104 个文件（98 Markdown + 2 HTML + 4 TXT）。新增 `market-benchmark-adversarial-audit-20260609.md`，并补齐 2026-06-08 后续增量在清单中的计数口径。
>
> 修订说明（2026-06-10 合同到期字段决策）：当前本地 docs 目录已增至 105 个文件（99 Markdown + 2 HTML + 4 TXT）。新增 `contract-date-compliance-field-granularity-options-20260610.md`，用于承接合同关键日期、证照/资质/保险到期字段 P0 颗粒度建议。
>
> 修订说明（2026-06-10 总控决策建议补充）：当时本地 docs 目录增至 106 个文件（100 Markdown + 2 HTML + 4 TXT）。新增 `adversarial-decision-options-20260610.md`，用于集中承接票务策略、MDM/Android Enterprise、企业微信、第三方接入、AI 质检、密钥轮换、备份、商户服务和商户账单等历史待判断建议；这些建议现已被最终执行口径、P0 任务清单和供应商接入预留包吸收，不再作为当前产品拍板入口。
>
> 修订说明（2026-06-10 全量对抗式审计收口）：当时本地 docs 目录增至 107 个文件（101 Markdown + 2 HTML + 4 TXT）。新增历史审计入口，用于集中呈现当轮全量文档审计、已修复冲突和建议默认值；相关事项现已被最终执行口径吸收。
>
> 修订说明（2026-06-10 二次对抗式审计补充）：当前本地 docs 目录已增至 108 个文件（102 Markdown + 2 HTML + 4 TXT）。新增 `adversarial-full-document-audit-followup-20260610.md`，用于收口 P0 完整应收应付台账、四类主体唯一绑定、B/C/D/E 执行口径和最新市场基准。
>
> 修订说明（2026-06-10 PM critic 与验收标准补充）：当前本地 docs 目录已增至 109 个文件（103 Markdown + 2 HTML + 4 TXT）。新增 `pm-critic-and-acceptance-criteria-20260610.md`，用于记录 PM 红队发现和关键功能 Given/When/Then 验收标准。
>
> 修订说明（2026-06-10 最终执行口径收口）：当时本地 docs 目录增至 110 个文件（104 Markdown + 2 HTML + 4 TXT）。新增 `final-open-decisions-and-owner-checklist-20260610.md`，最初用于集中列出总控、法务、采购、IT、财务或供应商事项；现已按总控最新口径改为"最终已确认执行口径与责任人清单"，不再作为产品拍板入口。
>
> 修订说明（2026-06-10 PM critic 二次补强）：当前本地 docs 目录已增至 112 个文件（106 Markdown + 2 HTML + 4 TXT）。新增 `business-entity-binding-development-design-v0.1.md` 与 `finance-ar-ap-ledger-development-design-v0.1.md`，把四类主体唯一绑定和 P0 完整应收应付台账补成可编码开发设计。
>
> 修订说明（2026-06-10 文档收口与归档审计）：当前本地 docs 目录已增至 113 个文件（107 Markdown + 2 HTML + 4 TXT）。新增 `document-retention-merge-audit-20260610.md`，用于逐一检查全部文档是否收口，并列出可合并、可归档或低价值文档。
>
> 修订说明（2026-06-10 正式技术栈收口）：按总控最新要求丢弃旧技术路线、旧进度快照和原始审查明细相关材料，并继续执行平台治理、业务经营和 160 运维物理合并；当前总量为 107 个文件，主目录保留 62 个活跃入口文件。
>
> 修订说明（2026-06-10 V0.4 对抗式复核）：继续按 documentation-specialist / doc-coauthoring / Product Design / PM Critic / acceptance-criteria 口径复核当前活跃文档；平台治理、业务经营、第三方接入、证据风控、数据分析、终端展示和 160 运维均以合并规格或 Runbook 为入口。当前总量仍为 107 个文件，主目录保留 62 个活跃入口文件。
>
> 修订说明（2026-06-10 P0 执行矩阵补齐）：当前本地 docs 目录为 111 个文件（109 Markdown + 2 HTML），主目录活跃入口为 66 个文件（64 Markdown + 2 HTML）。新增 `p0-operation-permission-matrix-v0.1.md`、`p0-action-event-evidence-matrix-v0.1.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md`、`third-party-supplier-integration-kit-v0.1.md`，用于补齐权限颗粒度、全链路留痕、端到端验收和第三方供应商未定前的接口占位。
>
> 修订说明（2026-06-10 当前有效文档入口补齐）：当前本地 docs 目录为 112 个文件（110 Markdown + 2 HTML），主目录活跃入口为 67 个文件（65 Markdown + 2 HTML）。新增 `00-current-effective-document-entry-v0.1.md`，作为产品、开发、测试、运营、供应商和外部机器智能体的第一阅读入口，并补齐正式技术栈、废止原型、阅读顺序和 P0 开发准入清单。
>
> 修订说明（2026-06-10 原型废止与技术栈基线补齐）：当前本地 docs 目录为 113 个文件（111 Markdown + 2 HTML），主目录活跃入口为 68 个文件（66 Markdown + 2 HTML）。新增 `prototype-deprecation-and-technology-baseline-v0.1.md`，明确旧 TypeScript/Fastify/Prisma/SQLite 后端仅为历史预研原型，正式路线固定 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven。
>
> 修订说明（2026-06-10 P0 API 契约补齐）：当前本地 docs 目录为 114 个文件（112 Markdown + 2 HTML），主目录活跃入口为 69 个文件（67 Markdown + 2 HTML）。新增 `p0-api-contract-pack-v0.1.md`，作为 P0 接口契约基线，覆盖统一路径、鉴权、响应、错误码、幂等、Webhook、证据上传、OpenAPI 交付和 P0 接口目录。
>
> 修订说明（2026-06-10 P0 用户故事地图补齐）：当前本地 docs 目录为 115 个文件（113 Markdown + 2 HTML），主目录活跃入口为 70 个文件（68 Markdown + 2 HTML）。新增 `p0-user-story-map-and-sprint-slices-v0.1.md`，作为 P0 用户故事地图和迭代切片入口，覆盖角色、业务旅程、Slice 0-4、用户故事和 Given/When/Then 验收标准。
>
> 修订说明（2026-06-10 P0 执行映射与游乐安全闭环补齐）：当前本地 docs 目录为 117 个文件（115 Markdown + 2 HTML），主目录活跃入口为 72 个文件（70 Markdown + 2 HTML）。新增 `p0-task-api-permission-event-acceptance-map-v0.1.md` 与 `amusement-p0-safety-inspection-spec-v0.1.md`，用于把 P0 任务、接口、权限、事件、验收逐行绑定，并固化游乐开放前阻断式安全巡检、整改复检和主管恢复闭环。
>
> 修订说明（2026-06-11 OpenAPI 与原型隔离执行收口）：新增 `docs/openapi/openapi-p0.yaml`、`docs/openapi/openapi-p0.json`，并在 `apps/api/LEGACY-PROTOTYPE.md` 增加旧 TypeScript/Fastify/SQLite 原型物理标识。P0 联调、自动化测试和供应商 mock 必须以 OpenAPI 与权限矩阵为准。

> 修订说明（2026-06-11 可编码准入补强）：当前本地 docs 目录为 121 个文件（117 Markdown + 2 HTML + 1 YAML + 1 JSON），主目录活跃入口为 74 个文件（72 Markdown + 2 HTML）。新增 `p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `code-contract-difference-and-migration-audit-v0.1.md`，并将 OpenAPI 请求体补齐到字段级 schema；P0 编码、测试和供应商 mock 不得再引用 `GenericWrite` 或旧 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*` 路径。

> 修订说明（2026-06-12 可编码收口复核）：当前活跃交付包为 76 个文件（72 Markdown + 2 HTML + 1 YAML + 1 JSON），主目录活跃入口为 74 个文件（72 Markdown + 2 HTML）。本轮补强 BE-000 正式后端准入、P0 用户故事依赖、任务-接口精确路径、OpenAPI 字段长度/边界/说明和旧库存口径噪音；历史归档数量不再计入当前交付包。
>
> 修订说明（2026-06-12 可编码入口包补强）：当前活跃交付包为 77 个文件（73 Markdown + 2 HTML + 1 YAML + 1 JSON），主目录活跃入口为 75 个文件（73 Markdown + 2 HTML）。新增 `p0-coding-entry-pack-v0.1.md`，并将根目录 API 脚本和 `apps/api` 子脚本改为阻断旧 TypeScript/Fastify/SQLite 原型，正式后端统一指向 `backend/`。
>
> 修订说明（2026-06-12 160 运维脚本收口）：交付包文件计数不变。将 `ops/160/complex-deploy-api.sh`、`complex-rollback-api.sh`、`complex-health-check.sh`、`complex-verify-p0.sh` 改为 Java/Spring Boot/PostgreSQL/Flyway 口径，新增 `complex-verify-be000.sh`，并从备份脚本移除旧 SQLite 原型库。
>
> 修订说明（2026-06-12 npm 工作区收口）：交付包文件计数不变。根 `package.json` workspace 改为 `apps/web` + `packages/*`，根 `package-lock.json` 移除旧 `apps/api`、Fastify、Prisma 依赖，旧 API 子目录仅保留阻断提示和历史参考。
>
> 修订说明（2026-06-13 OpenAPI 权限/事件/任务追溯附录补强）：当前活跃交付包为 78 个文件（74 Markdown + 2 HTML + 1 YAML + 1 JSON），主目录活跃入口为 76 个文件（74 Markdown + 2 HTML）。新增 `p0-openapi-contract-closure-appendix-v0.1.md`，用于由 OpenAPI 反向生成权限、事件、任务簇、终端和样例准入追溯；新增 `docs:openapi-closure` 与 `verify:openapi-closure`。

> 修订说明（2026-06-13 角色阅读包与旧路径追溯闸口补强）：交付包文件计数不变。`00-current-effective-document-entry-v0.1.md` 新增角色最小阅读包；统一口径基准、P0 任务清单、Android、票务、第三方集成、开放平台、留痕终端开关和市场参考文档把历史 `/api/mobile/*`、`/api/admin/*`、`/api/integration/*` 能力路径统一绑定到 OpenAPI、operation 验收矩阵和 `p0-openapi-contract-closure-appendix-v0.1.md`，未通过 `verify:openapi-closure` 不得进入 Controller、SDK/mock、接口自动化或供应商联调。
>
> 修订说明（2026-06-13 后端环境前置校验补强）：交付包文件计数不变。新增 `tools/run-backend-maven.mjs` 与 `npm run verify:backend-env`，`test:backend`、`dev:backend`、`verify:p0` 和 160 API 发布/BE-000 验收统一先检查 Java 21+、Maven Wrapper 或 Maven 3.9+，避免把本机环境缺失误判为后端实现或文档口径缺陷。
>
> 修订说明（2026-06-13 Android 环境前置校验补强）：交付包文件计数不变。新增 `tools/run-android-gradle.mjs` 与 `npm run verify:android-env`，Android 核心构建、smoke、APK 和 160 P0 验收统一检查 Java 21+、Gradle Wrapper 或系统 Gradle、Android SDK；P0 不允许因 Gradle/SDK 缺失静默跳过 Android 验收。
>
> 修订说明（2026-06-13 P0 验证链分层）：交付包文件计数不变。新增 `verify:p0-contract`、`verify:p0-web` 和 `verify:p0-env`；外部团队可先验文档契约、客户端联调包和 Web 构建，IT/运维单独验后端/Android 环境，完整上线仍以 `verify:p0` 为准。

> 修订说明（2026-06-13 生产前置薄 Runbook 收口）：当前活跃交付包为 81 个文件（77 Markdown + 2 HTML + 1 YAML + 1 JSON），主目录活跃入口为 79 个文件（77 Markdown + 2 HTML）。新增 `mdm-android-enterprise-implementation-plan-v0.1.md`、`production-credential-rotation-runbook-v0.1.md` 和 `nas-backup-and-restore-runbook-v0.1.md`，用于把 MDM/Android Enterprise、生产凭证轮换和 NAS 备份恢复从大文档说明收口为可执行运维入口。

> 修订说明（2026-06-16 开发文档完整性审计）：新增 `development-documentation-adversarial-audit-v0.1.md`、`development-documentation-audit-map-v0.1.csv` 与 `development-ready-spec-draft-v0.1.md`。当前完整开发文档审计以 160 仓库全量地图的 297 份开发相关文档为准；本清单中的早期文件数量保留为历史阶段计数，不再反向限定当前仓库文档总量。

> 修订说明（2026-06-16 代码实现验收审计）：新增 `code-implementation-acceptance-audit-v0.1.md` 与 `requirements-code-implementation-matrix-v0.1.csv`，用于把需求、页面/API/数据表/代码/测试/证据逐项映射，并记录当前核心业务可运行验收结论。

> 修订说明（2026-06-16 架构 Zoom-out 与完整后台闸口）：新增并修复 `codebase-architecture-zoom-out-audit-v0.1.md`，同步 `tools/verify-full-admin-page-matrix.mjs`，完整后台页面矩阵校验已纳入 `verify:delivery-gates`。
> 修订说明（2026-06-16 闭环当前态快照）：新增 `closed-loop-current-state-snapshot-v0.1.md`、`closed-loop-current-state-snapshot-latest.json` 与 `tools/generate-closed-loop-current-state.mjs`，并将 `verify:closed-loop-current-state` 接入 `verify:delivery-gates`，用于机器校验需求矩阵、代码行数、安全扫描、NAS 漂移和生产证据缺口的当前态。
>
> 修订说明（2026-06-16 NAS IT 受控挂载交接）：新增 `nas-it-controlled-mount-handoff-v0.1.md`、`ops/160/complex-nas-system-mount.example.sh` 与 `tools/verify-nas-it-handoff.mjs`，并将 `verify:nas-it-handoff` 接入 `verify:delivery-gates`；该脚本默认 dry-run，不写系统状态，真实挂载只能由 IT 在维护窗口用 `EXECUTE_MOUNT=true` 执行。
> 修订说明（2026-06-17 NAS 只读诊断）：新增 `docs/nas-diagnostic-report-v0.1.md`、`docs/production-readiness-evidence/nas-diagnostic-latest.json` 与 `tools/generate-nas-diagnostic-report.mjs`，并将 `verify:nas-diagnostics` 接入 `verify:delivery-gates`；该报告只做网络、SMB 端口、CIFS 挂载和 preflight 根因诊断，不读取密钥、不尝试挂载、不关闭生产 manifest。
> 修订说明（2026-06-17 运行时验收证据）：新增 `docs/runtime-acceptance-evidence-v0.1.md`、`docs/runtime-acceptance-evidence-latest.json` 与 `tools/generate-runtime-acceptance-evidence.mjs`，并将 `verify:runtime-acceptance-evidence` 接入 `verify:delivery-gates`；该快照只记录后端测试、Web 构建、UI、真实后端 UI 和生产边界证据，不关闭生产阻断。
> 修订说明（2026-06-17 前端体验验收）：`docs/closed-loop-implementation-ui-acceptance-report-v0.1.md` 改为由 `tools/generate-ui-experience-acceptance-report.mjs` 生成，并通过 `verify:ui-experience-acceptance` 防止 UI、Semgrep 和运行态数字漂移。
> 修订说明（2026-06-17 P0 生产证据快速回填）：新增 `docs/production-evidence-p0-quickstart-v0.1.md`、`docs/production-readiness-evidence/production-evidence-p0-quickstart-latest.json`、`docs/production-readiness-evidence/production-evidence-p0-quickstart-latest.csv` 与 `tools/generate-production-evidence-p0-quickstart.mjs`，并将 `verify:production-evidence-p0-quickstart` 接入 `verify:delivery-gates`；该快照只做 P0 责任组派发表，不关闭生产阻断。
> 修订说明（2026-06-17 生产证据责任组执行包）：新增 `tools/generate-production-evidence-owner-execution-packets.mjs`、`docs/production-evidence-owner-execution-packets-v0.1.md`、`docs/production-readiness-evidence/production-evidence-owner-execution-packets-latest.json` 与 `docs/production-readiness-evidence/owner-execution-packets/*.md`，并将 `verify:production-evidence-owner-execution-packets` 接入 `verify:delivery-gates`；该执行包只做责任组派发和模板复制提示，不关闭生产阻断。
> 修订说明（2026-06-17 生产证据草稿准备器）：新增 `tools/prepare-production-evidence-manifest-drafts.mjs`、`prepare:production-evidence-drafts` 与 `verify:production-evidence-draft-prep`；该工具默认 dry-run，只有显式 `--execute` 才复制模板到 submissions manifest 目录，且拒绝覆盖已有 manifest，不关闭生产阻断。
> 修订说明（2026-06-17 生产证据草稿准备器执行态验证）：新增 `tools/verify-production-evidence-draft-prep-fixtures.mjs` 与 `verify:production-evidence-draft-prep-fixtures`；该验证在临时目录实测 dry-run、execute、no-overwrite 和 broad execute 拒绝，不写真实 submissions。
> 修订说明（2026-06-17 生产证据正向验收样例）：新增 `tools/verify-production-evidence-positive-fixtures.mjs` 与 `verify:production-evidence-positive-fixtures`；该验证在临时目录写入一个合规 manifest，证明状态看板能接受 valid_submission，同时不污染真实 submissions。
> 修订说明（2026-06-17 生产证据正向传播验收）：新增 `tools/verify-production-evidence-positive-propagation-fixtures.mjs` 与 `verify:production-evidence-positive-propagation-fixtures`；该验证在临时目录确认一个合规 manifest 能从提交状态传播到生产闭环缺口报告和闭环当前态，同时不会误宣称生产上线就绪。
> 修订说明（2026-06-17 生产 ready no-go 防误判验收）：新增 `tools/verify-production-ready-no-go-fixtures.mjs` 与 `verify:production-ready-no-go-fixtures`；该验证在 40 份生产 manifest 全有效且 NAS 模拟 ready 时，确认 8 条外部证据 partial 需求仍会阻断生产上线 ready 声明。
> 修订说明（2026-06-17 生产 blocker register no-go 防误判验收）：新增 `tools/verify-production-blocker-register-no-go-fixtures.mjs` 与 `verify:production-blocker-register-no-go-fixtures`；该验证在 manifest、NAS 和外部证据都模拟满足时，确认生产 blocker register 未清零仍会阻断生产上线 ready 声明。
> 修订说明（2026-06-17 生产 ready 正向验收）：新增 `tools/verify-production-ready-positive-fixtures.mjs` 与 `verify:production-ready-positive-fixtures`；临时目录模拟全部硬门满足时，校验 production ready=true。

## 2. 给外部机器智能体的引导（如何在不了解历史会话的情况下理解本项目）

如果你是接手本项目的外部智能体，请按以下顺序建立理解，不要从随机文件开始：

1. **先读当前入口、口径与背景**（避免被局部旧表述误导）：
   - `00-current-effective-document-entry-v0.1.md` —— 当前第一阅读入口，说明哪些文档有效、哪些内容废止、正式技术栈和 P0 开发准入条件。
   - `prototype-deprecation-and-technology-baseline-v0.1.md` —— 原型废止与正式技术栈基线，说明旧 TypeScript/Fastify/Prisma/SQLite 后端不得作为正式开发依据。
   - `p0-coding-entry-pack-v0.1.md` —— P0 编码、联调、测试和供应商 mock 的统一入口，说明正式工程、契约、权限、留痕、验收和旧原型阻断规则。
   - `external-dependency-gates-v0.1.md` —— 外部依赖准入闸口，阻断未确定供应商前的真实机器人、视频、设备控制和独立 Python AI 服务伪实现。
   - `delivery-readiness-gates-v0.1.md` —— 交付总闸口，要求交付前运行 `npm run verify:delivery-gates`。
   - `production-owner-action-packs-v0.1.md`、`production-evidence-submission-kit-v0.1.md` —— 生产阻断项按责任人分派和证据提交包格式，要求回填后复跑交付闸口。
   - `p0-api-contract-pack-v0.1.md` —— P0 接口契约基线，供前端、后端、Android、一体机、小程序/App/Web、第三方和测试共同对齐。
   - `openapi/openapi-p0.yaml`、`openapi/openapi-p0.json` —— P0 机器可读接口契约，供 springdoc 比对、SDK/mock 生成、接口自动化和供应商 Adapter 契约测试使用。
   - `p0-openapi-operation-acceptance-matrix-v0.1.md` —— P0 operation 级验收矩阵，覆盖请求体、权限、认证上下文、终端、幂等、事件、边界情况和开发用户故事。
   - `code-contract-difference-and-migration-audit-v0.1.md` —— 旧原型、旧 Web/Android 路径和正式 Java/OpenAPI 契约的迁移闸口。
   - `p0-user-story-map-and-sprint-slices-v0.1.md` —— P0 用户故事地图和迭代切片，供产品、开发、测试和运营按角色闭环排期。
   - `original-requirements-capture-v0.1.md` —— 用户最初及持续补充的真实业务意图（需求源头）。
   - `unified-standards-baseline-v0.1.md` —— 跨文档单一权威口径 D1–D13（金额=分、6 层能力、事件命名权威、10 端枚举、NFC=设备凭证、门票资格、技术栈=Java/Maven、权限模板、币种、销售人员卡座提成等），**凡冲突以它为准**。
   - `business-decision-log-v0.1.md` —— 已拍板业务规则（含 §32 的 2026-06-07 A1/A5/A6/A9）。
   - `compliance-and-personal-information-protection-requirements-v0.1.md` —— 合规与个人信息保护要求（已采纳为 P0）。
2. **再读总索引与追溯**：
   - `product-documentation-index-v0.1.md` —— 交付物清单视图。
   - `system-capability-layers-product-index-v0.1.md` —— **唯一权威总索引**，按 6 层组织全部产品文档/PRD。
   - `requirement-traceability-matrix-v0.1.md` —— 需求→文档逐条追溯矩阵（REQ-01~REQ-92）。
   - `pm-critic-and-acceptance-criteria-20260610.md` —— PM critic 产品红队审查与关键功能验收标准。
   - `final-open-decisions-and-owner-checklist-20260610.md` —— 最终已确认执行口径与责任人清单，区分总控、法务/HR/财务、采购/IT、供应商资料和实施配置。
   - `business-entity-binding-development-design-v0.1.md` —— 四类主体唯一绑定的 P0 开发设计。
   - `finance-ar-ap-ledger-development-design-v0.1.md` —— P0 完整应收应付台账开发设计。
   - `p0-operation-permission-matrix-v0.1.md` —— P0 操作权限矩阵、权限模板和高风险权限审批留痕。
   - `p0-action-event-evidence-matrix-v0.1.md` —— P0 操作事件、证据、复核和敏感访问留痕矩阵。
   - `p0-end-to-end-acceptance-and-edge-cases-v0.1.md` —— P0 端到端验收标准和边界用例。
   - `p0-task-api-permission-event-acceptance-map-v0.1.md` —— P0 任务、接口、权限、事件和验收逐行映射表。
   - `amusement-p0-safety-inspection-spec-v0.1.md` —— 游乐 P0 安全巡检闭环规格。
   - `third-party-supplier-integration-kit-v0.1.md` —— 第三方供应商 SDK/API 未定前的双向开放、适配器、映射占位和补偿策略。
   - `document-retention-merge-audit-20260610.md` —— 全量文档收口、合并、归档和低价值文档审计。
   - `server-160-runbook-v0.2.md` —— 160 服务器部署、回滚、备份、监控和安全 Runbook。
   - `mdm-android-enterprise-implementation-plan-v0.1.md` —— MDM / Android Enterprise 设备纳管和生产前置验收 Runbook。
   - `production-credential-rotation-runbook-v0.1.md` —— 生产账号、密钥和密码轮换 Runbook。
   - `nas-backup-and-restore-runbook-v0.1.md` —— NAS 备份、恢复演练和可恢复验收 Runbook。
   - `production-evidence-closure-gap-report-v0.1.md` —— 生产闭环缺口总报告，由 `npm run docs:production-closure-gap-reports` 从当前运行态、Semgrep、NAS 当前状态和生产证据 JSON 生成，汇总待补 manifest、P0 优先级、责任组、阻断项和复测命令。
   - `production-existing-evidence-to-manifest-gap-audit-v0.1.md` —— 已有 NAS/160 机器证据到生产 manifest 的差距审计，防止误把部分证据伪关闭为 passed。
   - `current-staged-handoff-manifest-v0.1.md`、`current-staged-handoff-manifest-latest.json` —— 当前暂存交接清单，由 `npm run docs:staged-handoff-manifest` 从 160 Git 索引生成，防止提交或补丁包漏掉新增核心文件。
   - `docs/nas-current-state-drift-report-v0.1.md`、`npm run verify:nas-current-state-drift` —— NAS 历史通过证据与当前 live check 漂移报告，防止把旧 passed evidence 当作当前生产闭环。
   - `nas-diagnostic-report-v0.1.md` —— NAS 只读诊断报告，汇总网络、SMB 端口、CIFS 挂载和 preflight 根因；不读取密钥、不尝试挂载、不关闭生产 manifest。
3. **再读各层 PRD、合并规格、字段章节和验收章节**（按需进入对应能力层）。
4. **最后看两个业务人员 HTML 地图**：
   - `business-system-map-for-operations-v0.1.html`
   - `platform-governance-map-for-operations-v0.1.html`
5. **判断进度与边界时**：进度看 `project-progress-dashboard.md`；P0/P1/P2 边界以 `p0-implementation-task-list.md` 为单一权威；编码总入口看 `system-development-spec.md`。

> 重要提醒：正式实现以 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven 为准。线程 ID 一律以 `thread-registry.md` 为准，旧 thread-* 文件已归档到 `_archive/20260610-retention-audit/obsolete-thread-indexes/`。

## 3. 目录结构与文件清单（按用途分组）

### 3.0 顶层口径与总入口（最先读）

| 文件 | 作用 |
|---|---|
| `00-current-effective-document-entry-v0.1.md` | 当前第一阅读入口、正式口径、废止内容和 P0 开发准入清单 |
| `development-documentation-adversarial-audit-v0.1.md` | 当前开发文档完整性、业务逻辑可用性和验收可执行性对抗式审计 |
| `development-documentation-audit-map-v0.1.csv` | 全量开发相关文档地图，覆盖 297 份文档 |
| `development-ready-spec-draft-v0.1.md` | 完整后台管理端和真实外部依赖联调的可开发规格草案 |
| `full-admin-srs-v0.1.md` | 完整后台管理端正式 SRS，用于页面开发拆分和跨角色验收 |
| `full-admin-page-permission-field-matrix-v0.1.csv` | 完整后台页面、权限、字段、接口、状态、审计事件矩阵 |
| `full-admin-ui-acceptance-matrix-v0.1.md` | 完整后台 UI 自动化验收矩阵和边界场景 |
| `full-admin-mvp-coding-package-v0.1.md` | 完整后台首批 MVP 页面开发包，包含文件、接口、测试、验收、风险和回滚 |
| `full-admin-mvp-coding-task-breakdown-v0.1.csv` | 完整后台首批 MVP 页面任务拆解表 |
| `code-implementation-acceptance-audit-v0.1.md` | 当前代码实现对抗式验收审计、验证证据和联调/上线结论 |
| `final-closed-loop-acceptance-audit-v0.1.md` | 最终闭环实现验收审计，汇总需求、代码、页面、测试、安全和交付闸口证据 |
| `closed-loop-current-state-snapshot-v0.1.md` / `closed-loop-current-state-snapshot-latest.json` | 机器生成的闭环当前态快照，汇总需求矩阵、架构行数、安全扫描、生产证据和 NAS 当前漂移状态 |
| `runtime-acceptance-evidence-v0.1.md` / `runtime-acceptance-evidence-latest.json` | 机器生成的运行时验收证据，汇总后端测试、Web 构建、常规 UI、真实后端 UI、Semgrep 和生产边界；校验命令 `npm run verify:runtime-acceptance-evidence` |
| `closed-loop-implementation-ui-acceptance-report-v0.1.md` | 机器生成的前端体验验收报告，汇总 Playwright 路径、真实后端 UI、视觉基线、包体和 Semgrep 数字；校验命令 `npm run verify:ui-experience-acceptance` |
| `codebase-architecture-zoom-out-audit-v0.1.md` | 代码库架构 Zoom-out 审计、模块边界、剩余架构风险和分步改善计划 |
| `requirements-code-implementation-matrix-v0.1.csv` | 需求到代码实现矩阵，映射需求、页面/API、数据表、代码文件、测试和缺口 |
| `external-evidence-requirement-traceability-v0.1.md` / `production-readiness-evidence/external-evidence-requirement-traceability-latest.json` | 外部证据需求追踪报告，把 8 条 partial 需求绑定到责任组、证据 item_id、模板和目标 manifest 路径 |
| `prototype-deprecation-and-technology-baseline-v0.1.md` | 原型废止与正式技术栈基线，明确旧后端原型可参考但不得支配正式设计 |
| `p0-coding-entry-pack-v0.1.md` | P0 编码、联调、测试和供应商 mock 的统一入口包 |
| `p0-api-contract-pack-v0.1.md` | P0 API 契约基线，覆盖统一路径、鉴权、响应、错误码、幂等、Webhook 和接口目录 |
| `openapi/openapi-p0.yaml` / `openapi/openapi-p0.json` | P0 机器可读接口契约，后端、前端、Android、第三方和测试共同对齐 |
| `p0-openapi-operation-acceptance-matrix-v0.1.md` | P0 operation 级验收矩阵，后端、前端、Android、一体机、测试和供应商共同对齐 |
| `code-contract-difference-and-migration-audit-v0.1.md` | 旧 TypeScript/Fastify/SQLite 原型、旧 Web/Android 路径和正式 Java/OpenAPI 契约的迁移审计 |
| `p0-user-story-map-and-sprint-slices-v0.1.md` | P0 用户故事地图与迭代切片，按角色、业务旅程和验收标准拆分开发故事 |
| `p0-task-api-permission-event-acceptance-map-v0.1.md` | P0 任务、接口、权限、事件和验收逐行映射表 |
| `amusement-p0-safety-inspection-spec-v0.1.md` | 游乐 P0 开放前阻断式安全巡检、整改复检和主管恢复闭环 |
| `original-requirements-capture-v0.1.md` | 原始需求口径汇总（源头） |
| `unified-standards-baseline-v0.1.md` | 跨文档单一权威 D1–D13 |
| `business-decision-log-v0.1.md` | 业务拍板记录（含 2026-06-07 A1/A5/A6/A9） |
| `compliance-and-personal-information-protection-requirements-v0.1.md` | 合规与个人信息保护要求（P0） |
| `product-requirements-v0.1.md` | 产品需求总纲 |
| `product-documentation-index-v0.1.md` | 产品文档索引（交付物清单视图） |
| `system-capability-layers-product-index-v0.1.md` | **唯一权威总索引**（6 层） |
| `requirement-traceability-matrix-v0.1.md` | 需求→文档追溯矩阵 |
| `delivery-package-manifest-v0.1.md` | 本交付包说明 |
| `contract-date-compliance-field-granularity-options-20260610.md` | 合同关键日期、证照/资质/保险到期字段 P0 颗粒度建议 |
| `pm-critic-and-acceptance-criteria-20260610.md` | PM critic 产品红队审查与关键功能验收标准 |
| `final-open-decisions-and-owner-checklist-20260610.md` | 最终已确认执行口径与责任人清单 |
| `business-entity-binding-development-design-v0.1.md` | 四类主体唯一绑定 P0 开发设计 |
| `finance-ar-ap-ledger-development-design-v0.1.md` | P0 完整应收应付台账开发设计 |
| `p0-operation-permission-matrix-v0.1.md` | P0 操作权限矩阵、权限模板与高风险权限审批留痕 |
| `p0-action-event-evidence-matrix-v0.1.md` | P0 操作事件、证据、复核和敏感访问留痕矩阵 |
| `p0-end-to-end-acceptance-and-edge-cases-v0.1.md` | P0 端到端验收标准与边界用例 |
| `third-party-supplier-integration-kit-v0.1.md` | 第三方供应商未定前的接口预留、适配器、映射和补偿策略 |
| `document-retention-merge-audit-20260610.md` | 文档收口、合并、归档审计 |
| `server-160-runbook-v0.2.md` | 160 运维 Runbook |
| `mdm-android-enterprise-implementation-plan-v0.1.md` | MDM / Android Enterprise 设备纳管、策略状态、终端拦截和生产前置验收 Runbook |
| `production-credential-rotation-runbook-v0.1.md` | 生产账号、密钥、数据库密码、对象存储密码和第三方凭证轮换 Runbook |
| `nas-backup-and-restore-runbook-v0.1.md` | NAS 备份范围、策略、恢复演练和可恢复验收 Runbook |
| `nas-it-controlled-mount-handoff-v0.1.md` | NAS IT 受控挂载交接包，绑定 dry-run 脚本、维护窗口命令、manifest 回填和验收闸口 |
| `production-evidence-p0-quickstart-v0.1.md` | P0 生产证据快速回填快照，只列 31 个 P0 manifest、责任组、第一动作和目标路径 |
| `production-evidence-owner-execution-packets-v0.1.md` / `production-readiness-evidence/production-evidence-owner-execution-packets-latest.json` / `production-readiness-evidence/owner-execution-packets/*.md` | 按责任组生成的一站式生产证据执行包，绑定模板复制命令、第一动作和 P0 优先级；校验命令 `npm run verify:production-evidence-owner-execution-packets
npm run verify:production-evidence-crosslinks` |
| `tools/prepare-production-evidence-manifest-drafts.mjs` / `tools/verify-production-evidence-draft-prep-fixtures.mjs` | 生产证据 manifest 草稿准备器；`npm run prepare:production-evidence-drafts` 默认只预览，显式 `--execute` 才复制模板；校验命令 `npm run verify:production-evidence-draft-prep` 和 `npm run verify:production-evidence-draft-prep-fixtures` |
| `tools/verify-production-evidence-positive-fixtures.mjs` | 生产证据正向验收样例，在临时目录验证一个合规 manifest 会被识别为 `valid_submission`；校验命令 `npm run verify:production-evidence-positive-fixtures` |
| `tools/verify-production-evidence-positive-propagation-fixtures.mjs` | 生产证据正向传播验收样例，在临时目录验证 valid_submission 会同步反映到生产闭环缺口报告和闭环当前态，且不误判生产 ready；校验命令 `npm run verify:production-evidence-positive-propagation-fixtures` |
| `tools/verify-production-ready-no-go-fixtures.mjs` | 生产 ready no-go 防误判验收样例，在临时目录验证当生产 manifest 全有效且 NAS ready 时，外部证据 partial 需求仍阻断生产上线 ready；校验命令 `npm run verify:production-ready-no-go-fixtures` |
| `tools/verify-production-blocker-register-no-go-fixtures.mjs` | 生产 blocker register no-go 防误判验收样例，在临时目录验证当 manifest、NAS 和外部证据都 ready 时，生产 blocker register 未清零仍阻断生产上线 ready；校验命令 `npm run verify:production-blocker-register-no-go-fixtures` |
| `tools/verify-production-ready-positive-fixtures.mjs` | 生产 ready 正向验收样例，校验全部硬门满足时 production ready=true；命令 `npm run verify:production-ready-positive-fixtures` |
| `nas-diagnostic-report-v0.1.md` | NAS 只读诊断报告，汇总网络、SMB 端口、CIFS 挂载和 preflight 根因；不读取密钥、不尝试挂载、不关闭生产 manifest |
| `production-evidence-closure-gap-report-v0.1.md` | 自动生成的生产闭环缺口总报告，汇总当前运行态、Semgrep、NAS 状态、待补 manifest、P0 优先级、责任组、阻断项和复测命令 |
| `current-staged-handoff-manifest-v0.1.md` / `current-staged-handoff-manifest-latest.json` | 当前暂存交接清单，汇总 staged 文件数、状态、关键服务/迁移/测试是否进入索引和 name-status 指纹 |
| `production-existing-evidence-to-manifest-gap-audit-v0.1.md` | 已有 NAS/160 机器证据到生产 manifest 的差距审计，防止误把部分证据伪关闭为 passed |
| `system-development-spec.md` | 开发规格总入口（编码总入口） |
| `system-breakdown.md` / `business-systems-breakdown.md` / `foundation-systems-breakdown.md` | 系统/业务/地基总拆分 |
| `master-roadmap.md` | 项目总路线图 |
| `business-functional-topology-v0.1.md` | 业务功能拓扑图 |

### 3.1 业务人员 HTML 地图（点击式）

| 文件 | 作用 |
|---|---|
| `business-system-map-for-operations-v0.1.html` | 业务系统点击式地图 |
| `platform-governance-map-for-operations-v0.1.html` | 平台治理层点击式地图 |

### 3.2 平台治理层

`platform-governance-prd-v0.1`、`platform-governance-product-spec-v0.2`、`platform-governance-acceptance-pack-v0.2`；历史完成审计已归档。

### 3.3 业务经营层

`business-operations-layer-spec-v0.2`；各业务域 PRD：`ticketing-entitlement-checkin-product-prd`、`bar-business-operations-product-prd`、`amusement-business-operations-product-prd`、`supply-chain-inventory-product-prd`、`operation-traceability-and-terminal-switch-prd`（跨层底座，含唯一权威事件字典）。

### 3.4 第三方接入层

`third-party-business-access-product-prd`、`third-party-access-layer-spec-v0.2`、`third-party-supplier-integration-kit-v0.1`。

### 3.5 证据留痕与风控层

`evidence-risk-control-product-prd`、`evidence-risk-layer-spec-v0.2`。

### 3.6 数据分析层（含财务/对账子域）

`data-analysis-layer-spec-v0.2`、`finance-settlement-product-prd`（财务/对账子域）。

### 3.7 终端展示层

`terminal-page-product-prd`、`web-mobile-app-terminal-extension-product-prd`、`customer-app-miniapp-development-design`、`terminal-display-layer-spec-v0.2`。

### 3.8 开发设计层（编码用）

`code-contract-difference-and-migration-audit-v0.1`、`java-backend-platform-foundation-development-design`、`foundation-database-design`、`open-platform-security-auth-development-design`、`ticketing-pass-entitlement-development-design`、`ticketing-entitlement-settlement-breakdown`、`ticketing-database-implementation-notes`、`bar-operations-development-design`、`amusement-operations-development-design`、`supply-chain-inventory-development-design`、`third-party-integration-development-design`、`data-risk-ai-development-design`、`frontend-admin-development-design`、`android-device-terminal-development-design`、`android-terminal-breakdown`、`business-module-mature-system-gap-analysis`。

### 3.9 实施与验收

`p0-implementation-task-list`（P0/P1/P2 边界单一权威）、`p0-openapi-operation-acceptance-matrix-v0.1`、`p0-operation-permission-matrix-v0.1`、`p0-action-event-evidence-matrix-v0.1`、`p0-end-to-end-acceptance-and-edge-cases-v0.1`、`project-progress-dashboard`。

### 3.10 运维部署（160）

`server-160-runbook-v0.2`、`mdm-android-enterprise-implementation-plan-v0.1`、`production-credential-rotation-runbook-v0.1`、`nas-backup-and-restore-runbook-v0.1`。

### 3.11 线程与工作流

`thread-registry`（**唯一权威**）、`workstream-index`、`project-progress-dashboard`。旧 `thread-index` / `thread-map` / `thread-maintenance-map` 已归档到 `_archive/20260610-retention-audit/obsolete-thread-indexes/`。

### 3.12 审查与完善记录（归档留痕，非当前入口）

当前入口只保留 `pm-critic-and-acceptance-criteria-20260610.md` 与 `document-retention-merge-audit-20260610.md`。以下历史审计和原始发现明细已归档：

- `_archive/20260610-retention-audit/historical-audits/`
- `_archive/20260610-retention-audit/raw-audit-findings/`

> 修订说明（2026-06-08 复核修复）：补列第四个审查明细 TXT `审查发现明细-第三轮复核.txt`，与 §1 总计数（含 4 个 TXT）口径一致。
>
> 修订说明（2026-06-08 全量复核）：补列本次全量文档复核报告 `documentation-full-audit-and-fix-report-20260608.md`，作为判断当前状态的最新入口。
>
> 修订说明（2026-06-08 Product Design 复核）：补列 `documentation-product-flow-audit-and-fix-report-20260608.md`，作为产品流和追溯矩阵状态判断入口。
>
> 修订说明（2026-06-09 对抗式市场基准复核）：补列 `market-benchmark-adversarial-audit-20260609.md`，作为市场同类系统能力差距、P0/P1/P2 建议和本轮对抗式审计入口。
>
> 修订说明（2026-06-10 全量对抗式审计收口）：曾补列全量对抗式审计入口；相关结论现已被最终执行口径和 V0.4 复核吸收。
>
> 修订说明（2026-06-10 二次对抗式审计补充）：补列 `adversarial-full-document-audit-followup-20260610.md`，作为最新拍板后的二次审计补充入口。
>
> 修订说明（2026-06-10 PM critic 与验收标准补充）：补列 `pm-critic-and-acceptance-criteria-20260610.md`，作为产品红队和验收标准入口。

> 这些归档文件仍随包提供，但只用于追溯问题来源，不作为当前产品口径、开发计划或业务决策入口。4 个审查明细 TXT 已移入 `_archive/20260610-retention-audit/raw-audit-findings/`，仍计入 §1 全量文件总数。

## 4. 打包方式

在仓库根目录执行（任选其一）：

```bash
# 方式 A：zip（跨平台通用）
cd complex-management-system
zip -r complex-management-system-docs-v0.1-20260607.zip docs/

# 方式 B：tar + zstd（体积更小，适合 Linux/160）
cd complex-management-system
tar --use-compress-program=zstd -cf complex-management-system-docs-v0.1-20260607.tar.zst docs/
```

PowerShell（Windows）：

```powershell
Compress-Archive -Path .\complex-management-system\docs\* -DestinationPath .\complex-management-system-docs-v0.1-20260607.zip
```

打包后建议附校验值：

```bash
sha256sum complex-management-system-docs-v0.1-20260607.zip > complex-management-system-docs-v0.1-20260607.zip.sha256
```

## 5. 给外部智能体的评估建议

- 先以 `requirement-traceability-matrix-v0.1.md` 为需求覆盖总表；当前矩阵需求行已整体收口为已覆盖、已裁决并覆盖或后置预留，若后续发现新增业务口径，再新增 REQ 行并明确状态。
- 凡发现口径冲突，先比对 `unified-standards-baseline-v0.1.md`，再判断是文档未对齐还是基准需更新。
- 进度真实性以 `project-progress-dashboard.md` 和 `p0-implementation-task-list.md` 为准，后续验收均按正式 Java 栈执行。
- 合规项（PIPL/未成年人售酒/留存期限）已采纳为 P0，评估时按合规前置项对待。

## 变更日志

| 日期 | 类型 | 位置 | 内容 | 原因/依据 |
|---|---|---|---|---|
| 2026-06-07 | 增 | 新建本文档 | 交付包文件清单+目录结构+阅读起点+打包方式+版本日期+给外部智能体引导 | finding deliver-1；原始需求 §20 REQ-89 |
| 2026-06-07 收口 | 改 | §1 计数、§3.12 | 文件计数由"约 95（93 Markdown+2 HTML）"改为"99（94 Markdown+2 HTML+3 审查明细 TXT）"，§3.12 补列第三个审查明细 TXT，两处口径统一 | 计数与实际目录及自身随包口径自相矛盾。依据 finding newdocs-3 |
| 2026-06-08 复核修复 | 改 | §1 计数、§3.12 | 文件计数改为"100（94 Markdown+2 HTML+4 审查明细 TXT）"，补列 `审查发现明细-第三轮复核.txt` | 改进包实际随包有 4 个审查明细 TXT，上一版交付说明漏计 |
| 2026-06-08 全量复核 | 增/改 | §1、§3.12 | 新增 `documentation-full-audit-and-fix-report-20260608.md`，文件计数改为"101（95 Markdown+2 HTML+4 TXT）" | 本轮 documentation-specialist/doc-coauthoring 复核结果需随包提供 |
| 2026-06-08 Product Design 复核 | 增/改 | §1、§2、§3.12 | 新增 `documentation-product-flow-audit-and-fix-report-20260608.md`，文件计数改为"102（96 Markdown+2 HTML+4 TXT）" | 本轮 Product Design 产品流复核结果与追溯矩阵旧状态修复需随包提供 |
| 2026-06-09 对抗式市场基准复核 | 增/改 | §1、§2、§3.0、§3.12 | 新增 `market-benchmark-adversarial-audit-20260609.md`，文件计数更新为"104（98 Markdown+2 HTML+4 TXT）" | 用户要求以对抗式审计方式检查全部文档，并参考市场类似系统功能和服务 |
| 2026-06-10 总控决策建议补充 | 增/改 | §2、§3.12 | 新增历史决策建议入口，用于集中承接票务策略、MDM、企业微信、第三方接入、AI 质检、密钥轮换、备份和商户账单建议 | 用户要求给出优劣对比和可执行建议 |
| 2026-06-10 全量对抗式审计收口 | 增/改 | §1、§2、§3.0、§3.12 | 新增历史全量对抗式审计入口，文件计数当时更新为"107（101 Markdown+2 HTML+4 TXT）" | 用户要求同步检查全部文档、避免疏漏，并列出当时需要收口的事项 |
| 2026-06-10 二次对抗式审计补充 | 增/改 | §1、§2、§3.0、§3.12 | 新增 `adversarial-full-document-audit-followup-20260610.md`，文件计数更新为"108（102 Markdown+2 HTML+4 TXT）" | 用户要求再次检查完善全部文档，并参考市场类似系统功能和服务 |
| 2026-06-10 PM critic 与验收标准补充 | 增/改 | §1、§2、§3.0、§3.12 | 新增 `pm-critic-and-acceptance-criteria-20260610.md`，文件计数更新为"109（103 Markdown+2 HTML+4 TXT）" | 用户要求使用 pm-utility-pm-critic 检查产品方案问题，并用 pm-deliver-acceptance-criteria 补充验收标准 |
| 2026-06-10 P0 执行矩阵补齐 | 增/改 | §1、§2、§3.0、§3.4、§3.9 | 新增四份执行型文档，文件计数更新为"111（109 Markdown+2 HTML）"，主目录活跃入口 66 个 | 用户要求修复和补齐文档；第三方供应商未确定时留好接口并后续补字段 |
| 2026-06-10 当前有效文档入口补齐 | 增/改 | §1、§2、§3.0 | 新增 `00-current-effective-document-entry-v0.1.md`，文件计数更新为"112（110 Markdown+2 HTML）"，主目录活跃入口 67 个 | 用户确认全部按推荐方案执行；需先建立唯一阅读入口、正式口径和 P0 开发准入清单 |
| 2026-06-10 原型废止与技术栈基线补齐 | 增/改 | §1、§2、§3.0 | 新增 `prototype-deprecation-and-technology-baseline-v0.1.md`，文件计数更新为"113（111 Markdown+2 HTML）"，主目录活跃入口 68 个 | 防止旧 TypeScript/Fastify/Prisma/SQLite 后端原型继续误导正式 Java/PostgreSQL 开发 |
| 2026-06-10 P0 API 契约补齐 | 增/改 | §1、§2、§3.0 | 新增 `p0-api-contract-pack-v0.1.md`，文件计数更新为"114（112 Markdown+2 HTML）"，主目录活跃入口 69 个 | 固化 P0 接口契约，支持前端、后端、Android、一体机、小程序/App/Web、第三方和测试并行协作 |
| 2026-06-10 P0 用户故事地图补齐 | 增/改 | §1、§2、§3.0 | 新增 `p0-user-story-map-and-sprint-slices-v0.1.md`，文件计数更新为"115（113 Markdown+2 HTML）"，主目录活跃入口 70 个 | 将 P0 模块任务转换成角色目标、业务旅程、迭代切片和可测试用户故事 |
| 2026-06-10 P0 执行映射与游乐安全闭环补齐 | 增/改 | §1、§2、§3.0 | 新增 `p0-task-api-permission-event-acceptance-map-v0.1.md` 与 `amusement-p0-safety-inspection-spec-v0.1.md`，文件计数更新为"117（115 Markdown+2 HTML）"，主目录活跃入口 72 个 | 收口“任务完成但不可验收”和“游乐安全巡检索引有承诺但正文未交付”的硬缺口 |
| 2026-06-11 可编码准入补强 | 增/改 | §1、§2、§3.0、§3.8、§3.9 | 新增 `p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `code-contract-difference-and-migration-audit-v0.1.md`，文件计数更新为 121（117 Markdown + 2 HTML + 1 YAML + 1 JSON），主目录活跃入口 74 个 | 把 OpenAPI 字段级请求体、operation 验收和旧原型迁移闸口纳入交付包 |
| 2026-06-12 可编码准入收口修复 | 改 | OpenAPI、P0 任务清单、当前入口、代码迁移闸口、Android/前端/Java 旧路径说明 | 不新增文件；补齐 118 个 operation 的 `x-auth-context`，补 `LogoutRequest` 与 `DeviceUnbindRequest`，将一体机心跳改为设备凭证上下文，新增 P0-0 正式后端工程准入，并强化旧 `/api/admin/*`、`/api/mobile/*` 仅为能力参考；新增客人端订台资源读取接口 | 用户确认全部按推荐执行；防止旧 TypeScript/Fastify/SQLite 原型、旧路径和缺失认证上下文继续阻塞正式 Java/Spring Boot 可编码 |
| 2026-06-12 可编码入口包补强 | 增/改 | §1、§2、§3.0、根脚本和旧 API 原型脚本 | 新增 `p0-coding-entry-pack-v0.1.md`，交付包计数更新为 77（73 Markdown + 2 HTML + 1 YAML + 1 JSON），根目录 API 脚本和 `apps/api` 子脚本改为阻断旧原型，正式 API 入口统一指向 `backend/` | 防止文档已经废弃旧原型但命令仍启动旧 TypeScript/Fastify/SQLite 后端 |
| 2026-06-12 160 运维脚本收口 | 改 | `ops/160/*.sh`、§1、§2 | 160 API 发布、回滚、健康检查、BE-000/P0 验收和备份脚本切换为 Java/Spring Boot/PostgreSQL/Flyway 口径；不改变交付包文档计数 | 防止 160 服务器继续按旧 Node/Fastify/SQLite 路线部署或备份 |
| 2026-06-12 npm 工作区收口 | 改 | 根 `package.json`、`package-lock.json`、§1 | 根 npm workspace 和 lockfile 移除旧 `apps/api`、Fastify、Prisma 依赖；不改变交付包文档计数 | 防止旧 API 继续被根安装、构建或依赖审计识别为正式项目 |
| 2026-06-13 OpenAPI 示例闸口补强 | 增/改 | §1、OpenAPI、根 `package.json`、`tools/*.mjs` | 文档包计数不变；新增 OpenAPI P0 示例生成和校验工具，首批覆盖开放鉴权、Webhook、购票、核销、订台、酒吧点单、支付、游乐安全提交、供应商 mock 以及通用响应示例 | 防止接口契约只有 schema 没有机器样例，导致测试、供应商 mock 和 SDK 生成继续猜 payload |
| 2026-06-13 OpenAPI 权限/事件/任务追溯附录补强 | 增/改 | §1、§2、§3.0、根 `package.json`、`tools/*.mjs` | 新增 `p0-openapi-contract-closure-appendix-v0.1.md`，交付包计数更新为 78（74 Markdown + 2 HTML + 1 YAML + 1 JSON）；新增 `docs:openapi-closure` 和 `verify:openapi-closure` | 防止 OpenAPI 已变更但权限矩阵、事件矩阵、任务映射和样例准入口径未同步 |
| 2026-06-13 角色阅读包与旧路径追溯闸口补强 | 改 | §1、§2、当前入口、统一口径基准、P0 任务清单、Android、票务、第三方集成、开放平台、留痕终端开关、市场参考文档 | 文件计数不变；新增角色最小阅读包，并要求历史能力路径必须能在 OpenAPI、operation 验收矩阵和追溯附录定位，否则不得进入 Controller、SDK/mock、接口自动化或供应商联调 | 防止不同团队从旧路径表、历史能力清单或局部模块文档直接开工 |
| 2026-06-13 后端环境前置校验补强 | 增/改 | 根 `package.json`、`tools/*.mjs`、`backend/README.md`、`p0-coding-entry-pack-v0.1.md`、`ops/160/*.sh` | 文件计数不变；新增 `tools/run-backend-maven.mjs` 和 `verify:backend-env`，后端启动/测试/P0 总验证及 160 API 发布/BE-000 验收优先检查 Java 21+、Maven Wrapper 或 Maven 3.9+ | 防止本机或 160 缺少 Java/Maven 时被误判为 BE-000 实现失败，并降低后续加入 Maven Wrapper 的接手成本 |
| 2026-06-13 Android 环境前置校验补强 | 增/改 | 根 `package.json`、`tools/*.mjs`、`apps/android-terminal/README.md`、`android-device-terminal-development-design.md`、`p0-coding-entry-pack-v0.1.md`、`ops/160/*.sh` | 文件计数不变；新增 `tools/run-android-gradle.mjs` 和 `verify:android-env`，Android 构建、smoke、APK 和 160 P0 验收优先检查 Java 21+、Gradle/Wrapper 与 Android SDK | 防止 P0 验收在 Android 构建环境缺失时静默跳过，避免终端交付风险被文档收口掩盖 |
| 2026-06-13 P0 验证链分层 | 增/改 | 根 `package.json`、`p0-coding-entry-pack-v0.1.md`、`server-160-runbook-v0.2.md`、`project-progress-dashboard.md`、`ops/160/complex-verify-p0.sh` | 文件计数不变；新增 `verify:p0-contract`、`verify:p0-web`、`verify:p0-env`，160 P0 验收复用 `verify:p0-contract` | 防止缺 Java/Android 环境时掩盖文档契约已可先验，也防止把契约通过误判为完整 P0 上线通过 |
| 2026-06-13 生产前置薄 Runbook 收口 | 增/改 | §1、§2、§3.0、§3.10、当前入口 | 交付包计数更新为 81（77 Markdown + 2 HTML + 1 YAML + 1 JSON）；新增 MDM、生产凭证轮换和 NAS 备份恢复三份薄 Runbook | 防止入口文档引用生产前置项但没有独立执行入口，降低 IT/采购/运维交接成本 |
- `docs/full-admin-mvp-code-implementation-audit-v0.1.md`：完整后台 MVP 需求到代码审计。
- `docs/full-admin-mvp-requirements-code-matrix-v0.1.csv`：完整后台 MVP 需求-页面-接口-代码-测试矩阵。

- `docs/full-admin-remaining-code-implementation-audit-v0.1.md`：完整后台非首批页面实现审计与剩余缺口。
- `docs/full-admin-remaining-requirements-code-matrix-v0.1.csv`：完整后台非首批页面需求到代码实现矩阵。

### Partial 需求 Mock/沙箱验收覆盖报告 V0.1

- `docs/partial-requirement-mock-coverage-v0.1.md`
- `docs/partial-requirement-mock-coverage-latest.json`
- `tools/generate-partial-requirement-mock-coverage.mjs`
- Verify: `npm run verify:partial-requirement-mock-coverage`
- Boundary: MOCK_IS_NOT_PRODUCTION_EVIDENCE. The 8 partial requirements have mock/sandbox acceptance paths, but still require real external/vendor/production evidence before production blockers can close.
