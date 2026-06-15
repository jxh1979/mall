# 项目进度总控看板

更新时间：2026-06-15（T3-A 只读经营分析完成复核）

本文档记录“综合体管理系统”的正式实施状态。所有后续编码、验收、运维和外部交接均以 **Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven** 为唯一技术栈口径。

## 1. 当前总目标

目标已经从“生成可编码系统文档”推进到“按正式技术栈执行 P0/P1/P2/P3 实施与验收”。

当前总控判定：

- 文档：主线产品、开发设计、字段、流程、验收、运维和 PM Critic 审查文档已收口；“已收口”仅代表产品和开发口径已固定，不代表正式代码已经完成。
- P0 编码：2026-06-14 已在 192.168.88.160 的 `/home/james/complex-management` 完成首批 P0 正式编码验收。OpenAPI 181 个 operation 与 Java Controller 181 个映射一致，缺失 0，多余 0；后端 360 个测试通过；Web、Android、APK、API/Web 健康检查和 PostgreSQL/Redis/NATS/MinIO 状态均通过；`/opt/complex/scripts/complex-verify-p0.sh` 最终输出 `P0 verification PASS`；根目录 `npm run verify:p0` 也已可直接加载 160 用户级运行配置并通过完整验证。交接入口见 `p0-coding-completion-and-handoff-report-v0.1.md`。
- P0-0：正式 Java 21 + Spring Boot 4.x + Maven + PostgreSQL + Flyway + springdoc-openapi 后端工程骨架已创建在 `backend/`。160 机器已同步最新工程和 `ops/160` 脚本，并已通过 `verify:p0-env`、`verify:p0-contract`、`verify:p0-web`、`complex-runtime-config-check.sh`、`complex-verify-be000.sh` 运行态验收、`complex-health-check.sh` 和完整 `complex-verify-p0.sh`。当前 API release `20260613-193142` 与 Web release `20260613-180329` 已启动。注意：完整 P0 通过的是 dev 环境自动化 smoke 切片，证明正式路径、统一响应、鉴权、Android 流程、APK 和 160 环境可跑通；真实业务域 Controller/Repository/Service 仍不得标记为“正式并行开发完成”。
- 入口：P0 编码、联调、测试和供应商 mock 统一从 `p0-coding-entry-pack-v0.1.md` 进入；旧 `apps/api` TypeScript/Fastify/SQLite 原型已从 160 正式仓库物理移除，根目录 API 脚本统一指向 `backend/`，`verify:no-legacy-api-prototype` 已纳入 `verify:p0-contract` 防回退。
- 编码主机保护：新增 `coding-host-protection-baseline-v0.1.md` 和 `verify:coding-host-protection`；正式编码、完整 P0 验证、安全扫描和提交只认 `192.168.88.160` 的 `/home/james/complex-management`，本机只负责进度检查、任务拆解和质量检查，不得在本机仓库提交正式代码。
- T3-0：已建立 T3 编码入口包、用户故事地图、安全与供应商就绪闸口、入口审计和 `verify:t3-coding-package`；当前不直接进入 T3-C 机器人/视频/设备真实控制，也不进入 T3-D 独立 Python AI 服务。下一步若继续编码，只建议先做 T3-A 只读经营分析深化。
- T3-A：已在 160 完成只读经营分析最小切片，Web 管理端 `analytics` 入口改为 `T3-A 只读经营分析`，修正 analytics GET 返回字段映射，新增 `verify:t3a-readonly-analytics`；不新增后端写接口、数据库表、真实供应商控制或独立 Python AI 服务。
- 客户端：`npm run verify:client-routes` 已通过，Web/Android 非正式 `/api/*` 路径已清零；`npm run verify:client-openapi` 已通过，客户端 77 个正式路径模板和 95 个方法级调用均已被 P0 OpenAPI 覆盖；`npm run verify:client-payload` 已通过，Web/Android 已具备 OpenAPI 字段名适配；`ClientPlaceholderRequest` 已删除，写接口均改为字段级 requestBody schema。
- OpenAPI 文档质量：`docs:repair-openapi-text` 已修复残留连续问号摘要、描述和参数说明；`docs:operation-matrix` 已按当前 181 个 operation 重新生成 operation 验收矩阵；`docs:openapi-examples` 已补首批高风险写接口和外部接入接口机器示例；`docs:openapi-closure` 已生成 `p0-openapi-contract-closure-appendix-v0.1.md`，覆盖 operation 到任务簇、权限、事件、终端和样例准入的追溯附录；`verify:operation-matrix`、`verify:openapi-examples` 与 `verify:openapi-closure` 已纳入 P0 总验证链。
- 安全基线：`security-scans/threat_model.md` 已从旧 TypeScript/SQLite 原型威胁模型替换为正式安全基线重建入口；旧安全扫描仅通过 `security-scans/LEGACY-SECURITY-SCANS.md` 作历史追溯，不得参与当前 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 上线判断；`verify:prod-docs-access` 已纳入 `verify:security-baseline`，用于保证 prod profile 下 OpenAPI/Swagger 文档不匿名公开。
- 首批编码包：新增 `p0-first-coding-package-v0.1.md`，首批正式编码按 CP-BE、CP-DB、CP-AUTH、CP-EVT、CP-GOV、CP-FIN、CP-OPS、CP-QA 执行，不再继续在 dev smoke 切片上堆生产逻辑。
- P0 编码交接：新增 `p0-coding-completion-and-handoff-report-v0.1.md`，用于承接 P0 编码完成后的验收证据、已实现边界、生产前置约束和下一步交接动作。
- 生产前置资料：新增 `production-readiness-implementation-ledger-template-v0.1.md`，MDM、支付、NAS、密钥、人脸年龄核验、企业微信、第三方供应商、160、HTTPS 和安全基线均进入台账化管理；NAS 服务器已确认为 `192.168.88.113`，挂载协议为 SMB，共享为 `bn`，160 挂载目录为 `/mnt/complex-nas-backup`，当前不启用备份归档加密。MDM 采购/enroll、真实支付商户、企业微信、人脸年龄核验供应商、第三方厂商资料和 NAS 密码交付不纳入 P0 编码完成范围，只作为生产前置管理。
- 正式安全基线：新增 `formal-security-baseline-generation-task-v0.1.md`，旧安全扫描不得替代正式基线；首批正式编码包落地且运行时 OpenAPI 可导出后生成 V1 安全基线和 findings register。
- 文档入口复核：`00-current-effective-document-entry-v0.1.md` 的角色最小阅读包已修复失效文件名，`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` 已补高风险 AC 到 operationId 映射，`p0-user-story-map-and-sprint-slices-v0.1.md` 已将积分摘要和生产前置责任状态归入切片表。
- 本机验证边界：本机 Codex 只负责进度检查、任务拆解、远程调度和质量检查；正式代码修改、Java/Android/安全扫描、完整 `npm run verify:p0` 和提交必须在 `192.168.88.160` 的 `/home/james/complex-management` 执行。`verify:p0` 已接入 `verify:coding-host-protection`，非 160 主机默认不得给出完整 P0 通过结论。
- Web 构建边界：`npm run build` 已在本机通过，包含 typecheck 与 Vite 生产构建；当前仅有 Vite chunk size 警告，不作为 P0 阻断。
- P0：以 `p0-implementation-task-list.md` 为唯一边界清单，先完成核心组织/商户/票务/权益/核销/结算/酒吧最小闭环、完整应收应付台账、主体绑定、事件留痕和基础运维。
- P1：酒吧 SaaS 增强、商户服务工单、商户账单门厅、游乐安全告知/电子免责/事故工单、商场平面图图片+多边形坐标、合同审批流和附件归档。
- P2：动态定价、集团视图、预约排队、团体/包场、真实机器人/视频/设备厂商深度联动、独立 Python AI 服务。
- P3：AI 深度分析和智能推荐类能力，在合规和数据质量稳定后推进。
- 第三方：采用双向开放方案，先建设开放 API、Webhook、签名、幂等、轮询校验、对账和异常补偿框架；真实供应商字段映射待 SDK/API 到位后接入。
- 160：作为开发/预生产承载环境，旧项目已备份，当前项目已按 Java/Spring Boot/PostgreSQL/Flyway 最新口径同步；已通过环境、契约、Web 构建、运行配置预检、API/Web 启动、BE-000 运行态和整体健康检查。由于 `/etc/complex/complex.env` 当前需 sudo 才能修改，160 开发/预生产采用 `~/.complex/complex.env` 用户级覆盖完成实跑；生产或长期预生产仍需 IT/运维把同口径写入 `/etc/complex/complex.env`、接入 systemd、完成账号密钥轮换和告警。
- 160 复核：2026-06-14 已重新执行 `/opt/complex/scripts/complex-verify-p0.sh`，结果 PASS；契约、后端测试、Spring Boot jar 打包、Web 构建、Android 环境、Android 核心构建、Android smoke、APK、API/Web 健康检查和 PostgreSQL/Redis/NATS/MinIO 容器状态均通过。本轮补齐根目录 `npm run verify:p0` 的 160 配置加载后，直接执行总验证也已通过。P0 编码可收口；公开生产仍需完成生产前置台账、正式安全基线、真实供应商资料和运维配置生产化。

## 2. 文档成果入口

| 模块 | 状态 | 入口 |
|---|---|---|
| 当前有效入口 | 已收口 | `docs/00-current-effective-document-entry-v0.1.md` |
| P0 可编码入口包 | 已收口 | `docs/p0-coding-entry-pack-v0.1.md` |
| P0 首批编码包 | 已收口 | `docs/p0-first-coding-package-v0.1.md` |
| P0 编码完成与交接报告 | 已新增 | `docs/p0-coding-completion-and-handoff-report-v0.1.md` |
| 原型废止与技术栈基线 | 已收口 | `docs/prototype-deprecation-and-technology-baseline-v0.1.md` |
| P0 API 契约 | 已收口 | `docs/p0-api-contract-pack-v0.1.md` |
| P0 用户故事与迭代切片 | 已收口 | `docs/p0-user-story-map-and-sprint-slices-v0.1.md` |
| P0 任务-接口-权限-事件-验收映射 | 已收口 | `docs/p0-task-api-permission-event-acceptance-map-v0.1.md` |
| 总体架构 | 已收口 | `docs/system-development-spec.md`, `docs/master-roadmap.md`, `docs/system-breakdown.md` |
| 权威口径 | 已收口 | `docs/unified-standards-baseline-v0.1.md`, `docs/business-decision-log-v0.1.md` |
| 需求追踪 | 已收口 | `docs/requirement-traceability-matrix-v0.1.md` |
| P0 实施 | 已收口 | `docs/p0-implementation-task-list.md` |
| PM Critic 与验收 | 已收口 | `docs/pm-critic-and-acceptance-criteria-20260610.md` |
| Java 后端 | 已收口 | `docs/java-backend-platform-foundation-development-design.md` |
| 数据库 | 已收口 | `docs/foundation-database-design.md` |
| 前端后台 | 已收口 | `docs/frontend-admin-development-design.md` |
| Android 终端 | 已收口 | `docs/android-device-terminal-development-design.md`, `docs/android-terminal-breakdown.md` |
| 票务通票 | 已收口 | `docs/ticketing-pass-entitlement-development-design.md`, `docs/ticketing-database-implementation-notes.md` |
| 酒吧经营 | 已收口 | `docs/bar-operations-development-design.md` |
| 游乐运营 | 已收口 | `docs/amusement-operations-development-design.md`, `docs/amusement-p0-safety-inspection-spec-v0.1.md` |
| 供应链库存 | 已收口 | `docs/supply-chain-inventory-product-prd-v0.1.md`, `docs/supply-chain-inventory-development-design-v0.1.md` |
| 第三方集成 | 已收口 | `docs/third-party-integration-development-design.md`, `docs/third-party-access-layer-spec-v0.2.md` |
| 数据风控 AI | 已收口 | `docs/data-risk-ai-development-design.md`, `docs/data-analysis-layer-spec-v0.2.md` |
| 证据风控 | 已收口 | `docs/evidence-risk-layer-spec-v0.2.md` |
| 终端展示 | 已收口 | `docs/terminal-display-layer-spec-v0.2.md` |
| 160 运维 | 已收口 | `docs/server-160-runbook-v0.2.md` |
| MDM/Android Enterprise 生产前置 | 已收口 | `docs/mdm-android-enterprise-implementation-plan-v0.1.md` |
| 生产账号密钥轮换 | 台账结构已可验证；真实凭证证据待实施 | `docs/production-credential-rotation-runbook-v0.1.md`、`npm run verify:production-credentials` |
| NAS 备份恢复 | 已收口 | `docs/nas-backup-and-restore-runbook-v0.1.md` |
| 生产前置资料实施台账 | 已收口 | `docs/production-readiness-implementation-ledger-template-v0.1.md` |
| 正式安全基线待生成任务 | 已收口 | `docs/formal-security-baseline-generation-task-v0.1.md` |
| 文档归档审计 | 已收口 | `docs/document-retention-merge-audit-20260610.md` |

## 3. P0 实施状态口径

| 模块 | P0 状态 | 验收关注 |
|---|---|---|
| BE-000 正式后端工程准入 | 160 运行态和完整 P0 自动化 smoke 已通过 | `backend/` 已建立 Java 21 + Spring Boot 4.x + Maven + PostgreSQL + Flyway + springdoc-openapi 工程、健康检查、版本接口、OpenAPI 配置和 `V001__baseline.sql`；根目录 API 脚本已改向 `backend/`，旧 `apps/api` 已物理移除并由 `verify:no-legacy-api-prototype` 防回退；160 已同步最新工程和脚本并通过 `verify:p0-env`、`verify:p0-contract`、`verify:p0-web`、`complex-runtime-config-check.sh`、`complex-verify-be000.sh`、`complex-health-check.sh` 和 `complex-verify-p0.sh`；当前 API 使用 dev profile、非 systemd 回退启动和 `local/dev/test` 内存态 P0 smoke 切片，生产或长期预生产前仍需正式写入 `/etc/complex/complex.env`、systemd、密钥轮换、告警，并将业务接口迁移为 PostgreSQL + Flyway 正式实现 |
| Web/Android API client 迁移 | 旧路径清零，OpenAPI 示例、OpenAPI 覆盖、operation 矩阵同步和请求字段闸口已闭合 | `npm run verify:p0-contract` 已通过；`ClientPlaceholderRequest` 已删除，后续不得恢复通用占位请求体；客户端写接口请求字段必须与 OpenAPI requestBody schema 一致 |
| 平台组织、商户、场馆、区域、点位 | 160 P0 编码验收通过 | 多综合体、多主体、合同主体、运营主体、结算主体唯一绑定和留痕已进入自动化验收；生产配置和真实资料仍按生产前置台账执行 |
| 员工账号与权限 | 160 P0 编码验收通过 | 认证、权限、权限模板、临时授权、审计日志和 auth refresh/logout 已补齐；refresh/logout 保持 Bearer 认证边界 |
| 票种、权益、出票、核销、结算 | 160 P0 编码验收通过 | 平日/周末/节假日基准价，多人票/新人票/学生票/渠道价和核销闭环已进入 P0 验收；真实支付资料仍待生产前置 |
| 卡座权益 | 160 P0 编码验收通过 | 卡座预定者可带规定人数免通票进入预定酒吧；不同卡座人数不同；不得进入其他酒吧 |
| 酒吧点单与支付 | 160 P0 编码验收通过 | 聚合支付抽象、直连接口预留、高价值酒水在线核销边界已进入 P0 验收；真实商户号、回调和退款权限待财务/IT |
| 库存扣减 | 160 P0 编码验收通过 | 核心酒水和可追责物品库存流水、高价值酒水强在线核销和主管复核已进入 P0 验收 |
| 游乐安全巡检 | 160 P0 编码验收通过 | 开放前阻断式安全巡检、严重项暂停、整改复检、主管恢复和核销安全状态校验已进入 P0 验收 |
| 完整应收应付台账 | 160 P0 编码验收通过 | 租金、物业费、能源费、公摊/CAM、结算、退款、采购、供应商付款台账已进入 P0 验收 |
| 企业微信 | P0 接口/任务框架验收通过，生产资料待配置 | 最低管理成本方案进入实现边界；真实企业微信应用、通讯录、会话存档、回调和同步任务资料按台账补齐 |
| MDM/Android Enterprise | 待采购/IT 配置 | Android Enterprise / Android Management API + 商业 MDM，平台公司 IT 选型和运维；执行入口为 `docs/mdm-android-enterprise-implementation-plan-v0.1.md` |
| Android P0 构建、smoke 与 APK 验收 | 160 已通过 | 160 已具备 Java 21、Gradle/Android SDK 前置能力，并已通过 `verify:android-env`、`android:p0:build`、`android:p0:smoke` 和 `android:p0:apk`。Android smoke 使用专用种子测试账号，不使用真实生产管理员密码；后续真实业务域落地后仍需复跑同一脚本 |
| 备份与密钥轮换 | 真实 NAS 演练已通过；密钥轮换生产证据待实施 | NAS 服务器为 `192.168.88.113`，SMB 共享 `bn`，UNC `//192.168.88.113/bn`，160 挂载 `/mnt/complex-nas-backup`，账号 `jxh1`，保留 30 天，容量上限 1TB；当前不启用备份归档加密，已完成读写删除测试、`.tar.gz` + `.sha256` 同步和从 NAS 恢复演练；生产账号、PostgreSQL、对象存储、JWT/Token、聚合支付、企业微信、第三方供应商、开放平台和 NAS SMB 凭证采用人工台账 + 到期提醒 + Runbook，执行入口为 `docs/production-credential-rotation-runbook-v0.1.md`；合并闸口为 `npm run verify:ops160-recovery` |

## 4. 已确认规则

- 域名 HTTPS 先不绑定。
- 合同日期颗粒度采用通用事项台账 + 本地 AI 辅助识别，AI 结果必须人工确认。
- 低消不允许用票务或卡座权益抵扣；票务和卡座独立。
- 通票赠酒成本由平台承担。
- 酒吧合同中的“股东比例”只用于利润报表。
- 商场平面图 P1 先做图片 + 多边形坐标，可扩展 CAD/SVG 标准化导入。
- 合同审批 P0 只做审批流和附件归档。
- Android P1 可支持策略摘要离线查看；高价值酒水核销保持在线。
- 支付通道采用聚合支付优先，并保留直连接口抽象。
- Webhook 与轮询采用“Webhook + 定时轮询校验”组合。
- 告警渠道、160 脚本、财务结算主口径、B/C/D/E 类建议均按已收口默认方案执行。

## 5. 下一阶段建议

1. 按 `p0-coding-completion-and-handoff-report-v0.1.md` 做 P0 编码交接：固定验证证据、已实现边界、生产前置约束和下一步责任人。
2. 按 `formal-security-baseline-generation-task-v0.1.md` 生成正式安全基线 V1 和 findings register；P0 阻断缺陷未修复或未正式接受风险前不得上线。
3. 按 `production-readiness-implementation-ledger-template-v0.1.md` 建立真实生产前置资料台账；MDM、支付、NAS、密钥、人脸年龄核验、企业微信、第三方供应商、160、HTTPS 和安全基线必须有状态、责任、证据、阻塞范围和下一步动作。
4. 将 160 用户级运行配置迁移为长期预生产或生产配置：`/etc/complex/complex.env`、systemd、日志、告警、备份恢复演练和密钥轮换台账。
5. 在保持 `verify:p0-contract` 和 `complex-verify-p0.sh` 通过的前提下，启动 P1 排期：酒吧 SaaS 增强、商户服务工单、游乐电子免责/事故工单、商场平面图、合同审批附件归档。

## 6. 变更日志

| 日期 | 类型 | 内容 |
|---|---|---|
| 2026-06-10 | 改 | 按总控最新口径重写为正式 Java 栈实施看板，移除旧技术路线快照和旧验证命令。 |
| 2026-06-12 | 改 | 明确文档收口不等于代码完成；新增 BE-000 正式后端工程准入为下一步唯一先行动作。 |
| 2026-06-12 | 增 | 创建 `backend/` 正式后端骨架、`.env.example` Java/PostgreSQL 变量和 Flyway baseline；标记本地 Java/Maven 缺失导致 BE-000 尚待实跑验收。 |
| 2026-06-12 | 改 | 总控拍板 Spring Boot 使用 4.x；`backend/pom.xml` 父版本同步为 Spring Boot 4.1.0，Web 依赖切换为 Boot 4 的 WebMVC starter。 |
| 2026-06-12 | 增/改 | 新增 `p0-coding-entry-pack-v0.1.md` 为 P0 可编码入口包；根目录 API 脚本和 `apps/api` 子脚本改为阻断旧原型并指向正式 `backend/`。 |
| 2026-06-15 | 改 | 旧 `apps/api` TypeScript/Fastify/Prisma/SQLite 原型已从 160 正式仓库物理移除；新增 `verify:no-legacy-api-prototype` 并纳入 `verify:p0-contract`。 |
| 2026-06-15 | 增 | 新增 160 编码保护基线和 `verify:coding-host-protection`，完整 P0 验证、正式提交和安全扫描默认只能在 160 仓库形成证据。 |
| 2026-06-15 | 增 | 新增 T3-0 编码包和 `verify:t3-coding-package`，裁决下一步只允许优先进入 T3-A 只读经营分析；T3-C/T3-D 继续阻断。 |
| 2026-06-12 | 改 | 160 运维脚本切换为 Java/Spring Boot/PostgreSQL/Flyway 口径，新增 BE-000 验收脚本，备份脚本移除旧 SQLite 原型库。 |
| 2026-06-12 | 增/改 | Web/Android 非正式接口路径已清零，`verify:client-routes` 通过；补齐客户端路径模板 + HTTP 方法 OpenAPI 覆盖，`verify:client-openapi` 通过；`ClientPlaceholderRequest` 已删除并替换为字段级 DTO。 |
| 2026-06-12 | 增/改 | 新增客户端请求字段闸口 `verify:client-payload`；Web 出站请求统一转 OpenAPI snake_case、入站响应转前端 camelCase；Android 启用 snake_case 序列化并规范化手写 JsonObject。 |
| 2026-06-12 | 增/改 | 修复 OpenAPI 残留连续问号文本占位并重建当时 178 个 operation 的验收矩阵；本机 `verify:p0` 目前仅因缺少 Java/Maven/Gradle/Android SDK 无法继续后端和 Android 验证。 |
| 2026-06-12 | 增/改 | 新增 `verify:operation-matrix`，并纳入 `verify:p0` 与 160 验证脚本；OpenAPI 与 operation 验收矩阵不同步时阻断。 |
| 2026-06-13 | 增/改 | 新增 `docs:openapi-examples` 与 `verify:openapi-examples`，首批高风险写接口和外部接入接口已具备机器示例；`verify:p0` 与 160 P0 验证脚本已纳入示例闸口。 |
| 2026-06-13 | 增/改 | 新增 `p0-openapi-contract-closure-appendix-v0.1.md` 与 `verify:openapi-closure`，当前 181 个 operation 均已生成任务簇、权限、事件、终端和样例准入追溯；P0 总验证链已纳入追溯附录闸口。 |
| 2026-06-13 | 增/改 | 新增 `verify:backend-env` 和 `tools/run-backend-maven.mjs`，后端启动、后端测试和 P0 总验证统一先检查 Java 21+、Maven Wrapper 或 Maven 3.9+，当前本机阻塞被收口为明确环境前置项。 |
| 2026-06-13 | 增/改 | 新增 `verify:android-env` 和 `tools/run-android-gradle.mjs`，Android 构建、smoke、APK 和 160 P0 验收统一检查 Java 21+、Gradle/Wrapper 与 Android SDK，P0 不再允许静默跳过 Android。 |
| 2026-06-13 | 验证 | 本机 `npm run build` 通过，Web typecheck 与 Vite 生产构建可执行；Vite chunk size 警告暂不作为 P0 阻断。 |
| 2026-06-13 | 增/改 | 新增 `verify:p0-contract`、`verify:p0-web` 和 `verify:p0-env`；本机 `verify:p0-contract` 与 `verify:p0-web` 已实测通过，`verify:p0-env` 和完整 `verify:p0` 均阻断在 Java 21 缺失前置项。 |
| 2026-06-13 | 增/改 | 修复当前入口文档失效引用；补高风险 AC 到 operationId 映射；新增 MDM/Android Enterprise、生产凭证轮换、NAS 备份恢复三份薄 Runbook；交付包计数更新为 81。 |
| 2026-06-13 | 增/改 | 新增生产前置责任状态 P0 轻量台账/API，补齐查询、更新和准入检查三个 operation；OpenAPI 总数更新为 181。 |
| 2026-06-13 | 增/改 | 160 机器已备份旧项目并同步最新正式工程和脚本；早期实跑前曾识别出数据库 URL、账号密码和 API 运行态缺口，后续已由运行配置预检、用户级覆盖配置、API/Web 发布和 BE-000 运行态复验继续收口。 |
| 2026-06-13 | 增/改 | 新增 160 运行配置预检脚本和无密钥配置模板，并纳入 API 发布与回滚前置，阻断缺数据库账号/密码、数据库 URL 非 JDBC 形态、PostgreSQL 不可达或基础容器缺失时继续部署。 |
| 2026-06-13 | 改 | 票务数据库落地说明将资格规则表、学生凭证留痕表和多人票人数/放行人数留痕收口为正式 Flyway 首批业务迁移必落项；160 API 回滚脚本同步纳入运行配置预检。 |
| 2026-06-13 | 改 | 160 已通过用户级运行配置覆盖、API/Web 发布、BE-000 运行态和整体健康检查；当时完整 P0 处于阶段性待收口状态，后续已由 release `20260613-183652` 和完整 `complex-verify-p0.sh` 收口。 |
| 2026-06-13 | 改 | 160 API release `20260613-183652` 发布后，完整 `complex-verify-p0.sh` 已通过；新增 dev smoke 鉴权、统一响应解包、Android smoke 专用账号和内存态 P0 smoke 切片。 |
| 2026-06-13 | 改 | 同步安全基线与 Spring Boot 4.x 描述后，160 API release `20260613-193142` 已发布；运行时 `/docs/openapi` 已确认包含 `Java 21 + Spring Boot 4.x`，完整 `complex-verify-p0.sh` 于 19:32 再次通过。 |
| 2026-06-13 | 改 | 复核 160 当前环境：JDK 21.0.11、Maven 3.9.11、Node 22.22.3、npm 10.9.8、Spring Boot 4.1.0；`complex-runtime-config-check.sh`、`verify:backend-env`、`verify:android-env`、`verify:p0-contract` 和完整 `complex-verify-p0.sh` 均通过。 |
| 2026-06-13 | 改 | `security-scans/threat_model.md` 替换为正式安全基线重建入口，防止旧 TypeScript/SQLite 原型威胁模型继续被误用为当前上线依据。 |
| 2026-06-13 | 增/改 | 新增 `p0-first-coding-package-v0.1.md`、`formal-security-baseline-generation-task-v0.1.md`、`production-readiness-implementation-ledger-template-v0.1.md`；首批编码、安全基线待生成任务和生产前置资料台账均已进入执行入口、索引和验收链。 |
| 2026-06-14 | 验证/收口 | 160 机器完成 P0 首批编码正式验收：OpenAPI 181 个 operation 与 Controller 181 个映射一致，后端 360 个测试通过，Web/Android/APK/运行态健康检查通过，`complex-verify-p0.sh` 输出 `P0 verification PASS`；新增 `p0-coding-completion-and-handoff-report-v0.1.md` 作为交接入口。 |
| 2026-06-14 | 改 | 明确 NAS 服务器为 `192.168.88.113`；后续 Java/后端集成测试、BE-000 和完整 P0 验收默认在 160 或等效 Java 21 环境执行，本机缺 Java 21 只记录为本机环境前置缺失。 |
| 2026-06-14 | 复核 | 已将 NAS/160 测试口径、安全基线和生产文档访问保护同步到 160；160 上 `verify:ops160-recovery`、`verify:security-baseline`、`npm run test:backend` 和 `AUTO_START_RUNTIME=true /opt/complex/scripts/complex-verify-p0.sh` 均通过，后端测试为 360 个通过，完整 P0 输出 `P0 verification PASS`。 |
| 2026-06-14 | 改/验证 | 清理权限仓储和审计仓储中的残留未实现抛错痕迹；补齐 `tools/run-android-gradle.mjs` 对 160 配置文件的加载能力；160 上 `npm run verify:p0` 已直接通过完整验证。 |
| 2026-06-14 | 改 | NAS 参数补齐为 `//192.168.88.113/bn`、账号 `jxh1`、160 挂载 `/mnt/complex-nas-backup`、30 天保留、1TB 容量上限；当前不启用备份归档加密，脚本和恢复演练改为支持 `.tar.gz` + `.sha256`。 |
| 2026-06-14 | 验证 | 160 已完成真实 NAS 挂载、读写删除测试、备份同步、NAS 归档校验和从 NAS 恢复演练；恢复报告已回填 `docs/production-readiness-evidence/nas-restore-drill-latest.json`。 |
