# 当前有效文档入口与开发准入清单 V0.1

## 1. 文档定位

本文档是综合体管理系统当前交付包的第一阅读入口，用于回答四个问题：

1. 哪些文档是当前有效口径。
2. 哪些内容已经归档或废止，不能再作为开发依据。
3. 产品、开发、测试、运营、第三方供应商应按什么顺序阅读。
4. 哪些交付物完成前，不允许进入正式并行开发、联调或生产上线。

本文档不替代 PRD、技术方案、接口文档和验收文档；它只负责做入口、裁决优先级和准入规则。凡本文档与旧文档冲突，先按本文档和 `unified-standards-baseline-v0.1.md` 执行，再回补修正文档；业务口径除外——三份业务 HTML 为最终解释权（见 §2 首行）。

## 2. 当前唯一正式口径

| 事项 | 当前口径 |
|---|---|
| 正式技术栈 | Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven + springdoc-openapi |
| 业务人员最高口径 HTML | `docs/business-system-map-for-operations-v0.1.html`、`docs/platform-governance-map-for-operations-v0.1.html`、`docs/business-enum-quick-reference-v0.1.html`；三份 HTML 为业务口径最终解释权，PRD、开发、OpenAPI、数据库、UI、测试、实现计划需向其对齐 |
| 业务 HTML 事实源验收规格 | `docs/business-html-source-of-truth-acceptance-spec-v0.1.md`；只做三份业务 HTML 到 PRD/开发/OpenAPI/数据库/UI/测试的最小映射索引，不替代三份 HTML |
| 地产业务全链路 PRD | `docs/real-estate-full-chain-domain-prd-v0.1.md` |
| 招商获客业务域 PRD | `docs/leasing-acquisition-prd-v1.0.md` |
| 楼宇运营业务域 PRD | `docs/building-operations-prd-v1.0.md` |
| 集团治理重建 PRD | `docs/group-governance-rebuild-prd-v1.0.md` |
| P0 机器可读接口契约 | 当前可执行契约以 `docs/openapi/openapi-p0.json` 为准；`docs/openapi/openapi-p0.yaml` 为同名 YAML 镜像。operation 数以 `npm run verify:operation-matrix` / `verify:openapi-closure` 脚本输出为准；本轮（2026-08-03）核验为 720 operations。YAML 镜像若落后于 JSON，不得用落后的 YAML 反向覆盖 JSON，需先同步差异 |
| OpenAPI 与业务 HTML 覆盖核对（2026-06-30 复核更新，分辨四类，勿再笼统列缺口） | ① 商户档案/入驻=已覆盖（`/api/v1/platform/merchants` + `merchant.profile.view/manage`）；② 应收=已覆盖（AR/AP 域）；③ 楼栋/楼层/区块/铺位/公共区域=由扁平 `spaces` 的 `space_type`+`parent_space_id` 自套承载（非缺口，映射见 foundation-database-design）；④ 发票=税务/电子发票平台第三方对接、故意延后（预留接口+后台开关+不阻塞自研，真实资料到位再补契约，见 finance-settlement-product-prd §6A）；⑤ 操作审计日志查询/导出=**现状已覆盖 + 明确增强项**：审计事件页 ADM-EVD-001（AUDITOR，搜索/筛选/导出留痕）已实现、由 `/api/v1/evidence/events` 承载，按对象时间线已 UNION `audit_events`；**待增强（P1/P2 留档、非阻塞）**=跨对象批量搜索/导出全量 `audit_events` 操作日志（如“导出某月全部退款放行”），需另开 audit_events 专属查询/导出端点+页+测试，按审计实际需要再排。 |
| P0 operation 级验收矩阵 | `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` |
| P0 OpenAPI 权限、事件、任务追溯附录 | `docs/p0-openapi-contract-closure-appendix-v0.1.md` |
| P0 可编码入口包 | `docs/p0-coding-entry-pack-v0.1.md` |
| P0 首批编码包 | `docs/p0-first-coding-package-v0.1.md` |
| P0 编码完成与交接报告 | `docs/p0-coding-completion-and-handoff-report-v0.1.md` |
| P1 编码完成审计 | `docs/p1-coding-completion-audit-v0.1.md` |
| P2 编码候选审计 | `docs/p2-coding-candidate-audit-v0.1.md` |
| P2-A 平面图导入开发计划 | `docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md` |
| P2-A 平面图导入接口契约 | `docs/p2-floor-plan-import-api-contract-v0.1.md` |
| P2-A 平面图导入完成审计 | `docs/p2-floor-plan-import-completion-audit-v0.1.md` |
| P2-B 游乐团体/包场开发计划 | `docs/superpowers/plans/2026-06-15-p2-amusement-group-private-booking.md` |
| P2-B 游乐团体/包场接口契约 | `docs/p2-amusement-group-private-booking-api-contract-v0.1.md` |
| P2-B 游乐团体/包场完成审计 | `docs/p2-amusement-group-private-booking-completion-audit-v0.1.md` |
| P2-C 票务动态定价开发计划 | `docs/superpowers/plans/2026-06-15-p2-ticket-dynamic-pricing.md` |
| P2-C 票务动态定价接口契约 | `docs/p2-ticket-dynamic-pricing-api-contract-v0.1.md` |
| P2-C 票务动态定价完成审计 | `docs/p2-ticket-dynamic-pricing-completion-audit-v0.1.md` |
| P2-D 经营指标快照开发计划 | `docs/superpowers/plans/2026-06-15-p2-business-metric-snapshots.md` |
| P2-D 经营指标快照接口契约 | `docs/p2-business-metric-snapshots-api-contract-v0.1.md` |
| P2-D 经营指标快照完成审计 | `docs/p2-business-metric-snapshots-completion-audit-v0.1.md` |
| P2-E 第三方深度集成边界契约 | `docs/p2-third-party-deep-integration-boundary-contract-v0.1.md` |
| P2-E 第三方深度集成边界完成审计 | `docs/p2-third-party-deep-integration-boundary-completion-audit-v0.1.md` |
| P2-F 独立 Python AI 服务边界契约 | `docs/p2-python-ai-service-boundary-contract-v0.1.md` |
| P2-F 独立 Python AI 服务边界完成审计 | `docs/p2-python-ai-service-boundary-completion-audit-v0.1.md` |
| P2 全量完成审计 | `docs/p2-full-completion-audit-v0.1.md` |
| P2 当前差异对抗式审计 | `docs/p2-current-diff-adversarial-review-v0.1.md` |
| 开发文档完整性对抗式审计 | `docs/development-documentation-adversarial-audit-v0.1.md` |
| 开发文档全量地图 CSV | `docs/development-documentation-audit-map-v0.1.csv` |
| 可开发规格草案 | `docs/development-ready-spec-draft-v0.1.md` |
| 完整后台管理端 SRS | `docs/full-admin-srs-v0.1.md` |
| 完整后台页面权限字段矩阵 | `docs/full-admin-page-permission-field-matrix-v0.1.csv` |
| 完整后台 UI 自动化验收矩阵 | `docs/full-admin-ui-acceptance-matrix-v0.1.md` |
| 完整后台首批 MVP 页面开发包 | `docs/full-admin-mvp-coding-package-v0.1.md` |
| 完整后台首批 MVP 任务拆解 | `docs/full-admin-mvp-coding-task-breakdown-v0.1.csv` |
| UI 自动化验收报告 | `docs/ui-automation-acceptance-report-v0.1.md` |
| gstack 需求验收与交付审计 | `docs/gstack-acceptance-ship-audit-v0.1.md` |
| 代码实现对抗式验收审计 | `docs/code-implementation-acceptance-audit-v0.1.md` |
| 最终闭环实现验收审计 | `docs/final-closed-loop-acceptance-audit-v0.1.md` |
| 闭环运行态总验收 | `npm run verify:closed-loop-runtime`，覆盖后端测试、Web typecheck/build、常规 UI 和真实后端 UI 验收 |
| 闭环运行态验收证据 | `docs/runtime-acceptance-evidence-v0.1.md`、`docs/runtime-acceptance-evidence-latest.json`、`npm run verify:runtime-acceptance-evidence` |
| 前端体验验收报告 | `docs/closed-loop-implementation-ui-acceptance-report-v0.1.md`、`npm run verify:ui-experience-acceptance` |
| 闭环当前态快照 | `docs/closed-loop-current-state-snapshot-v0.1.md`、`docs/closed-loop-current-state-snapshot-latest.json`、`npm run verify:closed-loop-current-state` |
| 需求到代码实现矩阵 CSV | `docs/requirements-code-implementation-matrix-v0.1.csv` |
| T3-0 编码入口包 | `docs/t3-coding-entry-pack-v0.1.md` |
| T3 用户故事地图 | `docs/t3-user-story-map-and-sprint-slices-v0.1.md` |
| T3 安全与供应商就绪闸口 | `docs/t3-security-and-supplier-readiness-gate-v0.1.md` |
| T3 入口评估与编码包审计 | `docs/t3-entry-assessment-and-coding-package-audit-v0.1.md` |
| T3 编码包机器校验 | `npm run verify:t3-coding-package` |
| T3-D1 本地大模型 AI 质检建议 | `docs/t3d-local-llm-ai-quality-v0.1.md` |
| T3-D1 本地大模型 AI 质检机器校验 | `npm run verify:t3d-local-llm-ai-quality` |
| 外部依赖准入闸口 | `docs/external-dependency-gates-v0.1.md` |
| 外部证据需求追踪报告 | `docs/external-evidence-requirement-traceability-v0.1.md`、`docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.json`、`npm run verify:external-evidence-requirement-traceability` |
| 交付总闸口文档 | `docs/delivery-readiness-gates-v0.1.md` |
| 交付总闸口机器校验 | `npm run verify:delivery-gates` |
| 正式安全基线生成与维护任务 | `docs/formal-security-baseline-generation-task-v0.1.md` |
| 生产前置资料实施台账模板 | `docs/production-readiness-implementation-ledger-template-v0.1.md` |
| 生产阻断清单 | `docs/production-blocker-register-v0.1.md`、`docs/production-readiness-evidence/production-blocker-register-latest.json` |
| 生产责任人动作包 | `docs/production-owner-action-packs-v0.1.md`、`docs/production-readiness-evidence/production-owner-action-packs-latest.json` |
| 生产证据提交包 | `docs/production-evidence-submission-kit-v0.1.md`、`docs/production-readiness-evidence/production-evidence-submission-kit-latest.json` |
| 生产证据 Manifest 模板 | `docs/production-evidence-manifest-templates-v0.1.md`、`docs/production-readiness-evidence/production-evidence-manifest-template-index-latest.json` |
| 生产证据责任组回填说明包 | `docs/production-evidence-owner-guides-v0.1.md`、`docs/production-readiness-evidence/production-evidence-owner-guides-latest.json` |
| 生产证据提交状态看板 | `docs/production-evidence-submission-status-v0.1.md`、`docs/production-readiness-evidence/production-evidence-submission-status-latest.json` |
| 生产证据提交目录护栏 | `docs/production-readiness-evidence/submissions/README.md`、`docs/production-readiness-evidence/submissions/.gitignore` |
| 生产证据正向验收样例 | `tools/verify-production-evidence-positive-fixtures.mjs`、`npm run verify:production-evidence-positive-fixtures` |
| 生产证据正向传播验收样例 | `tools/verify-production-evidence-positive-propagation-fixtures.mjs`、`npm run verify:production-evidence-positive-propagation-fixtures` |
| 生产 ready no-go 防误判验收 | `tools/verify-production-ready-no-go-fixtures.mjs`、`npm run verify:production-ready-no-go-fixtures` |
| 生产 blocker register no-go 防误判验收 | `tools/verify-production-blocker-register-no-go-fixtures.mjs`、`npm run verify:production-blocker-register-no-go-fixtures` |
| 生产 ready 正向可关闭验收 | `tools/verify-production-ready-positive-fixtures.mjs`、`npm run verify:production-ready-positive-fixtures` |
| 生产证据回填优先级看板 | `docs/production-evidence-backfill-priority-v0.1.md`、`docs/production-readiness-evidence/production-evidence-backfill-priority-latest.json` |
| 生产证据回填执行台账 | `docs/production-evidence-backfill-execution-ledger-v0.1.md`、`docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.json`、`docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.csv` |
| 生产证据 P0 快速回填快照 | `docs/production-evidence-p0-quickstart-v0.1.md`、`docs/production-readiness-evidence/production-evidence-p0-quickstart-latest.json`、`docs/production-readiness-evidence/production-evidence-p0-quickstart-latest.csv`、`npm run verify:production-evidence-p0-quickstart` |
| 生产证据责任组执行包 | `docs/production-evidence-owner-execution-packets-v0.1.md`、`docs/production-readiness-evidence/production-evidence-owner-execution-packets-latest.json`、`docs/production-readiness-evidence/owner-execution-packets/*.md`、`npm run verify:production-evidence-owner-execution-packets
npm run verify:production-evidence-crosslinks` |
| 生产证据 manifest 草稿准备工具 | `tools/prepare-production-evidence-manifest-drafts.mjs`、`tools/verify-production-evidence-draft-prep-fixtures.mjs`、`npm run prepare:production-evidence-drafts`、`npm run verify:production-evidence-draft-prep`、`npm run verify:production-evidence-draft-prep-fixtures` |
| 生产闭环缺口总报告 | `docs/production-evidence-closure-gap-report-v0.1.md`、`npm run docs:production-closure-gap-reports`、`npm run verify:production-closure-gap-reports` |
| 当前暂存交接清单 | `docs/current-staged-handoff-manifest-v0.1.md`、`docs/current-staged-handoff-manifest-latest.json`、`npm run verify:staged-handoff-manifest` |
| NAS IT 受控挂载交接包 | `docs/nas-it-controlled-mount-handoff-v0.1.md`、`ops/160/complex-nas-system-mount.example.sh`、`npm run verify:nas-it-handoff` |
| 已有生产证据到 Manifest 差距审计 | `docs/production-existing-evidence-to-manifest-gap-audit-v0.1.md` |
| NAS 当前状态漂移报告 | `docs/nas-current-state-drift-report-v0.1.md`、`docs/production-readiness-evidence/nas-current-state-drift-latest.json`、`npm run verify:nas-current-state-drift` |
| NAS 只读诊断报告 | `docs/nas-diagnostic-report-v0.1.md`、`docs/production-readiness-evidence/nas-diagnostic-latest.json`、`npm run verify:nas-diagnostics` |
| 生产前置证据回填模板 | `docs/production-readiness-evidence-intake-template-v0.1.md` |
| 160 编码保护基线 | `docs/coding-host-protection-baseline-v0.1.md` |
| 代码契约迁移闸口 | `docs/code-contract-difference-and-migration-audit-v0.1.md` |
| 废止技术路线 | TypeScript / SQLite / Prisma / Fastify 后端仅可作为历史预研原型，不得作为正式开发、测试、部署、安全审计依据；`apps/api` 已于 2026-06-15 从 160 正式仓库物理移除，`verify:no-legacy-api-prototype` 已纳入 `verify:p0-contract`，用于阻断旧目录、旧脚本、旧依赖和旧 lockfile 标记回流 |
| 金额单位 | 全局金额使用整数分 `bigint` |
| 币种 | P0 仅启用 CNY，保留 `currency=CNY` 字段，不启用多币种和汇率换算 |
| 能力层 | 平台治理、业务经营、第三方接入、证据留痕与风控、数据分析、终端展示六层 |
| 财务定位 | 财务经营是数据分析层下的财务/对账子域，P0 必须支持完整应收应付台账 |
| 第三方接入 | 双向开放：平台提供统一 OpenAPI/Webhook，同时通过供应商 SDK/API 适配器接入外部成熟服务 |
| Webhook/轮询 | P0 使用 Webhook + 定时轮询校验 |
| 设备管控 | Android Enterprise / Android Management API + 商业 MDM 作为 P0 上线前置 |
| 备份 | NAS 备份方案，NAS 服务器 `192.168.88.113`，共享 `bn`，UNC `//192.168.88.113/bn`，160 挂载 `/mnt/complex-nas-backup`，当前不启用备份归档加密；历史上已完成一次真实 NAS 同步和从 NAS 恢复演练；当前状态以 `docs/nas-current-state-drift-report-v0.1.md` 和 `docs/nas-diagnostic-report-v0.1.md` 为准，生产前仍需固化开机自动挂载、定时备份、容量告警和周期性恢复演练 |
| 生产凭证轮换 | 生产账号、PostgreSQL、对象存储、JWT/Token、第三方/支付/企微/开放平台密钥采用人工台账 + 到期提醒 + Runbook；`npm run verify:ops160-recovery` 必须通过 |
| AI/规则处理 | P0 使用 Java 内置规则处理器；T3-D1 可使用 160 本地大模型提供脱敏后的只读质检建议，必须人工复核且不得自动决策；独立 Python AI 服务仍后置，未进入当前生产路径 |
| 构建工具 | Maven 为唯一 P0 构建工具 |
| 游乐安全 | P0 强制开放前阻断式安全巡检、严重项暂停开放、主管恢复、全程留痕 |
| 一体机 | 统一终端码 `kiosk`，通过 `kiosk_mode=ticketing/service` 区分售票型和非售票型 |
| 售票型一体机 | 承担授权服务人员辅助到店购票、取票、订单查询职责；绑定酒吧门口/前台时只能售卖本酒吧单店门票 |
| 卡座提成 | 新增销售人员岗位；销售人员售卖卡座按可计提消费额 15%，非销售人员按 10%；不含通票和票务权益 |
| 卡座转卡/合并/续卡 | 已纳入 P1 酒吧 SaaS/POS 增强：同酒吧、同商户、同营业日受控操作；保留账单、低消、入场名额、销售归属、服务责任链和事件留痕；不作为当前 P0 阻断 |

### 2.1 R1~R4 审计交付物登记（2026-06-18 附加）

本节附加式登记 R1~R4 阶段审计与回填交付物，以及配套的数据隔离规格与 SMS 占位规格，便于审计入口统一追溯。本节为附加内容，不删除上文任一原有登记；当前事实基线（后端 surefire 现有报告 2583 tests / OpenAPI 720 operations / Flyway 最新 V382 / P0 契约与安全基线以 `npm run verify:*` 输出为准，2026-08-03 刷新）以 `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `npm run verify:*` 实时输出为准。<!-- 文档goal(2026-06-30)：事实基线刷新为现有 surefire 报告 1606 tests、OpenAPI 516 ops、Flyway V182（179 个迁移）；不声明刚完成全量验证 -->

| 交付物 | 入口 |
|---|---|
| R1 需求文档审计报告 | `docs/r1-requirements-documentation-audit-report-20260618.md` |
| R1 文档地图 | `docs/r1-documentation-map-20260618.md` |
| R1 需求-API-页面-表-测试 映射矩阵 | `docs/r1-requirement-api-page-table-test-mapping-matrix-20260618.md` |
| R1 第三方能力开关与 mock 策略 | `docs/r1-third-party-capability-switch-and-mock-strategy-20260618.md` |
| R1 待回填供应商信息清单 | `docs/r1-pending-supplier-information-backfill-checklist-20260618.md` |
| R2 代码审计（REQ26b/REQ21/REQ30） | `docs/r2-code-audit-req26b-req21-req30-20260618.md` |
| R3 迁移层漂移审计 | `docs/r3-migration-level-drift-audit-20260618.md` |
| R3 确认缺口优先级与修复 backlog | `docs/r3-confirmed-gap-priorities-and-fix-backlog-20260618.md` |
| R4 剩余工作 backlog | `docs/r4-remaining-work-backlog-20260618.md` |
| 数据隔离与租户范围安全规格 | `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` |
| 第三方短信通知与供应商字段映射占位 | `docs/third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md` |

<!-- 修正(2026-06-19 R5业务HTML缺口审计 F1/F3)：依据 business-system-map-for-operations-v0.1.html #supply 与 matrix-migration-claims.json -->
## 2.2 口径漂移收口登记（2026-06-19）

本节登记"同期两份文档结论相反/口径漂移"项并给出单一权威结论，附加式登记不删除上文任一原有内容。业务口径以三份业务 HTML 为最终解释权（见 §2 首行，与 `documentation-map-v0.1.md` 一致）；工程落地事实的权威源优先级：实际迁移（`backend/src/main/resources/db/migration`）+ `docs/matrix-migration-claims.json`（2026-06-19）> 代码 > PRD/开发设计散文。凡迁移已落地者，以代码/迁移为准；PRD 仍标更低优先级的口径需升级。

| 漂移项 | 两方说法 | 单一权威结论 | 后续动作 |
|---|---|---|---|
| 供应链落地状态 | dev-design 顶部横幅"零落地/保持开放" vs claims"V085（供应合同三模式/采购周期/六类库房/高价值复核）、V094（高价值阈值）已落" | **以 claims/迁移为准：V085/V094 已落地**；横幅为更早快照 | dev-design 已就地追加 R5 收口说明（指向 V085/V094）；验收以最新 claims 为准 |
| 班结/交班对账优先级 | bar PRD §10.1 标 P2（独立交班/班结对账 shift close / cash drawer） vs 代码 `V095__bar_shift_handover.sql` + `BarShiftService` 已落 | **以代码为准：已交付，PRD 口径需升级为 P0 已交付** | bar PRD §10.1 已就地追加收口说明（V095 落地 + 验收口径）；仍缺 G/W/T 验收 |
| 游乐 incident 优先级 | 游乐 incident PRD 标 P0 vs 开发设计降为 P1 | **暂不裁定，待 F11 对齐** | 留待 F11 专项对齐 incident 口径，本次不改游乐文档 |
| 商户级 revenue.net | 商户级 `revenue.net` REQ-81"保持开放" vs r5-aggregation"已落 V075/V078" | **待裁定，倾向以代码为准**（V075/V078 迁移已存在） | 留待裁定；倾向以 V075/V078 实际落地口径收口，需补对账硬约束确认 |
| 核销表名 | `verification_records` vs `ticket_verifications` | **统一以实际迁移表名为权威**（以迁移中真实建表名为准，文档引用名需对齐） | 各引用文档对齐到实际迁移表名 |
| 积分表名 | `points_*` vs `customer_points_*` | **统一以实际迁移表名为权威** | 各引用文档对齐到实际迁移表名 |
| REQ-79 引用不建之表 | REQ-79 引用 `point_gifts`/`point_redemptions`（迁移未建） | **以实际迁移为权威：引用表不存在，需改引用为真实迁移表** | 修正 REQ-79 引用，去除对未建表的依赖 |

<!-- 文档goal(2026-06-20)：新增 §2.3，登记 R6/R7 决策批次与本会话新功能落地，刷新事实基线 -->
## 2.3 R6/R7 决策批次与本会话新功能落地登记（2026-06-20）`文档goal(2026-06-20)`

本节附加式登记业务决策批次与本会话已落地新功能，不删除上文任一原有内容。事实基线刷新为：**本轮 2026-08-03 取证 Flyway 最新 V382、OpenAPI 720 operations、后端 surefire 现有报告 2583 tests**；各项以 `docs/openapi/openapi-p0.yaml`、`backend/src/main/resources/db/migration`、`backend/target/surefire-reports`、`npx playwright test --list` 与 `npm run verify:*` 实时输出为最终权威。

### 决策批次登记

| 批次 | 入口 | 状态 |
|---|---|---|
| R6 业务输入与确认决议（R6-D01..D14 共 14 项） | `docs/business-decision-log-v0.1.md` §36 | 14 项已于 2026-06-19 全部业务确认，"已确认决策"列为最终编码基线 |
| R7 决策批次 | —— | 截至 2026-06-20，`business-decision-log-v0.1.md` 最新业务决策批次为 §36（R6）；尚未设立独立 R7 章节。新批次产生时在 decision-log 追加 §37(R7) 并在此登记，避免悬挂引用 |

### 历史新功能已落地（迁移 V117/V120–V124；当前最新迁移以 `db/migration` 为准）

| 迁移 | 功能 | 关联文档 |
|---|---|---|
| `V117__bar_void_approval_writeback` | 酒吧退菜/退酒作废审批回写 | 酒吧 PRD/开发设计、§36 R6-D14（酒吧异常优先级升 P0） |
| `V120__merchant_service_work_order_sla_escalation` | 商户服务工单 SLA 升级 | `p1-merchant-service-work-order-api-contract-v0.1.md` |
| `V121__cam_trueup_and_refund_direction` | CAM 找差与退款方向（CAM 分摊已废止于 2026-06-22 物业费改制 V150，本迁移仅退款方向维度仍现行） | finance-settlement PRD、§36 R6-D08（科目方向） |
| `V123__amusement_maintenance_vendor_fk_validate` | 游乐维保供应商外键校验 | 游乐开发设计、`V116__amusement_maintenance_vendor_fk` 续 |
| `V124__bar_void_reject_reversal_adjustment` | 酒吧作废驳回/冲销调整 | 酒吧 PRD/开发设计、§36 R6-D14 |

## 3. 当前有效文档阅读顺序

### 3.1 总控、产品负责人、项目负责人

| 顺序 | 文档 | 用途 |
|---|---|---|
| 1 | `00-current-effective-document-entry-v0.1.md` | 确认当前有效入口、废止口径和准入条件 |
| 2 | `unified-standards-baseline-v0.1.md` | 查看跨文档唯一标准 |
| 3 | `product-requirements-v0.1.md` | 查看产品总纲和业务边界 |
| 4 | `business-decision-log-v0.1.md` | 查看历史业务决策来源 |
| 5 | `p0-coding-entry-pack-v0.1.md` | 查看正式编码、联调、测试和供应商 mock 的统一入口 |
| 6 | `p0-first-coding-package-v0.1.md` | 查看首批编码包、首批任务边界、反模式和验收标准 |
| 7 | `p0-coding-completion-and-handoff-report-v0.1.md` | 查看 160 P0 编码完成、验证证据、质量结论和生产前置边界 |
| 8 | `p1-coding-completion-audit-v0.1.md` | 查看 P1 编码完成范围、验证证据和剩余外部依赖 |
| 9 | `p2-coding-candidate-audit-v0.1.md` | 查看 P2 可编码候选、暂缓项和推荐顺序 |
| 10 | `superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md` | 查看 P2-A 平面图 SVG/标准 JSON 导入开发计划 |
| 11 | `p2-floor-plan-import-api-contract-v0.1.md` | 查看 P2-A 平面图导入接口、权限、事件和验收 |
| 12 | `p2-floor-plan-import-completion-audit-v0.1.md` | 查看 P2-A 当前完成范围、验证证据和后置边界 |
| 13 | `p2-amusement-group-private-booking-api-contract-v0.1.md`、`p2-ticket-dynamic-pricing-api-contract-v0.1.md`、`p2-business-metric-snapshots-api-contract-v0.1.md` | 查看 P2-B/C/D 当前已编码接口、权限、事件和验收 |
| 14 | `p2-third-party-deep-integration-boundary-contract-v0.1.md`、`p2-python-ai-service-boundary-contract-v0.1.md` | 查看 P2-E/F 仅边界收口、暂不做真实供应商/AI 服务编码的原因 |
| 15 | `p2-full-completion-audit-v0.1.md`、`p2-current-diff-adversarial-review-v0.1.md` | 查看 P2 当前完成范围、验证证据、差异审计和后续候选 B 入口 |
| 16 | `p0-implementation-task-list.md` | 查看 P0 开发任务和优先级 |
| 17 | `production-readiness-implementation-ledger-template-v0.1.md` | 查看生产前置资料实施台账、状态、责任、证据和阻塞范围 |
| 18 | `formal-security-baseline-generation-task-v0.1.md` | 查看正式安全基线生成条件、输入、输出和阻断项 |
| 19 | `p0-api-contract-pack-v0.1.md`、`openapi/openapi-p0.yaml`、`openapi/openapi-p0.json` | 查看 P0 接口契约基线和机器可读契约 |
| 19A | `external-dependency-gates-v0.1.md`、`delivery-readiness-gates-v0.1.md` | 查看外部依赖阻断、交付总闸口和 `npm run verify:delivery-gates` 准入口径 |
| 20 | `p0-openapi-operation-acceptance-matrix-v0.1.md` | 查看本轮核验 720 个 operation 的权限、认证上下文、终端、幂等、事件和验收口径（operation 数以本矩阵与 OpenAPI 实时为准；下文历史变更记录里的 181/191/198/207/386/419 为旧快照）<!-- 文档goal(2026-06-30)：419→516 --> |
| 21 | `p0-openapi-contract-closure-appendix-v0.1.md` | 查看 OpenAPI 反向生成的权限、事件、任务追溯和样例准入口径 |
| 22 | `p0-user-story-map-and-sprint-slices-v0.1.md` | 查看 P0 用户故事地图和迭代切片 |
| 23 | `requirement-traceability-matrix-v0.1.md` | 查看需求到设计、任务、验收的追踪关系 |

### 3.2 后端、数据库、架构和运维

| 顺序 | 文档 | 用途 |
|---|---|---|
| 1 | `system-development-spec.md` | 查看总体技术方案 |
| 2 | `p0-coding-entry-pack-v0.1.md` | 查看正式工程、契约、权限、留痕、验收和旧原型隔离的共同入口 |
| 3 | `p0-first-coding-package-v0.1.md` | 查看首批正式编码包和首批可验收边界 |
| 4 | `prototype-deprecation-and-technology-baseline-v0.1.md` | 查看历史原型废止和正式技术栈基线 |
| 5 | `code-contract-difference-and-migration-audit-v0.1.md` | 查看旧原型、旧路径与正式 Java/OpenAPI 契约的迁移闸口 |
| 6 | `java-backend-platform-foundation-development-design.md` | 查看 Java 后端地基设计 |
| 7 | `foundation-database-design.md` | 查看 PostgreSQL/Flyway 数据库设计 |
| 8 | `openapi/openapi-p0.yaml`、`openapi/openapi-p0.json` | 查看正式 P0 路径、认证、权限、幂等、事件和响应契约 |
| 9 | `p0-openapi-operation-acceptance-matrix-v0.1.md` | 查看 operation 级开发完成定义 |
| 10 | `p0-openapi-contract-closure-appendix-v0.1.md` | 查看 operation 到权限、事件、任务簇、终端和样例准入的完整追溯 |
| 11 | `open-platform-security-auth-development-design.md` | 查看开放平台鉴权、安全、签名和权限设计 |
| 12 | `production-readiness-implementation-ledger-template-v0.1.md` | 查看生产前置资料台账字段、状态和准入检查口径 |
| 13 | `formal-security-baseline-generation-task-v0.1.md` | 查看正式安全基线生成任务和安全准入输出 |
| 14 | `server-160-runbook-v0.2.md` | 查看 160 服务器部署、备份、恢复和运维脚本 |

### 3.3 业务模块开发

| 业务 | 先读文档 | 再读文档 |
|---|---|---|
| 票务、通票、权益、核销 | `ticketing-entitlement-checkin-product-prd-v0.1.md` | `ticketing-pass-entitlement-development-design.md` |
| 酒吧、卡座、低消、点酒、赠酒、存酒 | `bar-business-operations-product-prd-v0.1.md` | `bar-operations-development-design.md` |
| 供应链、库存、损耗、报损、存酒 | `supply-chain-inventory-product-prd-v0.1.md` | `supply-chain-inventory-development-design-v0.1.md` |
| 游乐项目、场次、容量、安全巡检 | `amusement-business-operations-product-prd-v0.1.md` | `amusement-operations-development-design.md` |
| 商户、主体、合同、权限、设备 | `platform-governance-prd-v0.1.md` | `platform-governance-product-spec-v0.2.md` |
| 应收应付、日结、对账、结算 | `finance-settlement-product-prd-v0.1.md` | `finance-ar-ap-ledger-development-design-v0.1.md` |
| 会员身份、成长值与等级 | `member-and-tier-product-prd-v1.0.md` | 本轮只发布 PRD，技术承接另行审计 |
| 积分获取、冻结、有效期、兑换与虚拟券 | `points-and-voucher-product-prd-v1.0.md` | 本轮只发布 PRD，技术承接另行审计 |
| 商户积分价格、费用、补偿、账单与税务边界 | `merchant-points-settlement-rules-v1.0.md` | 本轮只发布业务规则，技术承接另行审计 |
| 储值（付费会员已退役） | `member-loyalty-and-stored-value-product-prd-v0.1.md` | 现有开发设计只作实现追溯，不得反向解释三份新权威 PRD |
| 经营看板、8 类指标、下钻 | `business-dashboard-metrics-product-prd-v0.1.md` | `r5-spec-T12-dashboard-aggregation-dev-design-20260618.md` |
| 客人端 App/小程序、订台定金、订卡邀请函 | `customer-app-miniapp-product-prd-v0.1.md` | `customer-app-miniapp-development-design-v0.1.md` |

### 3.4 前端、Android、平板、一体机、小程序/App/Web

| 终端 | 文档 | 重点 |
|---|---|---|
| 后台管理 Web | `frontend-admin-development-design.md` | 管理端页面、表单、权限和操作入口 |
| Android 工作手机/平板 | `android-device-terminal-development-design.md` | 设备绑定、扫码、核销、MDM、在线策略 |
| 终端枚举与页面边界 | `terminal-page-product-prd-v0.1.md` | 端类型、页面职责、开关 |
| App/小程序/Web | `web-mobile-app-terminal-extension-product-prd-v0.1.md` | 客人购票、订单、权益、展示和自助入口 |
| 一体机 | `terminal-display-layer-spec-v0.2.md` | 售票型/非售票型一体机边界和能力开关 |

### 3.5 测试、审计、运营复核

| 文档 | 用途 |
|---|---|
| `p0-api-contract-pack-v0.1.md` | P0 接口契约基线、路径、鉴权、响应、错误码、幂等、Webhook 和接口目录 |
| `p0-first-coding-package-v0.1.md` | P0 首批编码包、首批可开发范围、强制反模式和首批验收标准 |
| `openapi/openapi-p0.yaml`、`openapi/openapi-p0.json` | P0 机器可读接口契约；后端 springdoc 输出、前端 SDK/mock、Android 联调和自动化测试均以此比对 |
| `p0-openapi-operation-acceptance-matrix-v0.1.md` | P0 operation 级验收矩阵；按接口逐条校验字段、权限、认证上下文、终端、幂等、事件和边界情况 |
| `p0-openapi-contract-closure-appendix-v0.1.md` | OpenAPI 反向生成的权限、事件、任务追溯和样例准入附录；补齐人工矩阵未逐 operation 展开的执行缺口 |
| `code-contract-difference-and-migration-audit-v0.1.md` | 旧原型、旧路径、正式技术栈和迁移任务的审计闸口 |
| `p0-user-story-map-and-sprint-slices-v0.1.md` | P0 用户故事地图、角色旅程和迭代切片 |
| `p0-end-to-end-acceptance-and-edge-cases-v0.1.md` | P0 端到端验收和边界场景 |
| `p0-operation-permission-matrix-v0.1.md` | 权限点、权限模板、临时授权、单次有效和拒绝规则 |
| `p0-action-event-evidence-matrix-v0.1.md` | 操作事件、证据、复核、敏感访问留痕 |
| `pm-critic-and-acceptance-criteria-20260610.md` | PM Critic 对抗式审查和验收补强 |
| `platform-governance-acceptance-pack-v0.2.md` | 平台治理验收包 |
| `formal-security-baseline-generation-task-v0.1.md` | 正式安全基线生成条件、输入、输出、阻断项和验收标准 |
| `production-readiness-implementation-ledger-template-v0.1.md` | 生产前置资料实施台账模板，覆盖 MDM、支付、NAS、密钥、供应商、160 和安全基线 |

### 3.6 第三方供应商和外部系统

| 文档 | 用途 |
|---|---|
| `third-party-business-access-product-prd-v0.1.md` | 第三方业务接入产品边界 |
| `third-party-access-layer-spec-v0.2.md` | 第三方接入层页面、字段、流程和验收 |
| `third-party-integration-development-design.md` | 第三方技术集成设计 |
| `third-party-supplier-integration-kit-v0.1.md` | 供应商未确定前的统一接口、适配器、字段映射占位、Webhook/轮询和补偿策略 |
| `openapi/openapi-p0.yaml`、`openapi/openapi-p0.json` | 供应商 Adapter、开放平台 mock、Webhook/轮询验收的接口契约 |
| `p0-openapi-operation-acceptance-matrix-v0.1.md` | 供应商 Adapter、Webhook、轮询、mock 和异常补偿的 operation 验收口径 |
| `p0-openapi-contract-closure-appendix-v0.1.md` | 供应商联调时按 operationId 查权限、事件、终端、任务簇和样例准入；不得引用旧 `/api/integration/*` 作为正式路径 |

### 3.7 文档类型、事实源等级和适用对象矩阵

事实源等级用于处理文档冲突：`S0` 为唯一裁决入口，`S1` 为可直接开发/验收的执行事实源，`S2` 为模块设计或产品拆解事实源，`S3` 为历史参考或市场增强材料。低等级文档不得反向覆盖高等级文档。

| 文档 | 类型 | 事实源等级 | 适用对象 | 可编码入口 | 冲突时上游 |
|---|---|---|---|---|---|
| `00-current-effective-document-entry-v0.1.md` | 当前入口/裁决规则 | S0 | 总控、产品、开发、测试、供应商 | 是，作为准入裁决 | 本文档自身 |
| `unified-standards-baseline-v0.1.md` | 统一口径基准 | S0 | 产品、开发、测试 | 是，作为跨域规则裁决 | 本文档 |
| `business-decision-log-v0.1.md` | 决策日志 | S0 | 总控、产品、实施 | 是，作为业务决策证据 | 本文档、统一口径基准 |
| `business-html-source-of-truth-acceptance-spec-v0.1.md` | 业务 HTML 事实源验收规格 | S0 | 产品、开发、测试、运营、供应商 | 是，作为三份业务 HTML 到下游文档的最小验收映射索引 | 三份业务 HTML、本文档 |
| `p0-coding-entry-pack-v0.1.md` | P0 可编码入口包 | S1 | 后端、前端、Android、一体机、测试、供应商 | 是，作为编码和联调第一执行入口 | 本文档、OpenAPI、验收矩阵 |
| `p0-first-coding-package-v0.1.md` | P0 首批编码包 | S1 | 后端、数据库、测试、项目管理 | 是，作为首批正式编码分派入口 | P0 编码入口包、任务清单、OpenAPI |
| `p0-coding-completion-and-handoff-report-v0.1.md` | P0 编码完成交接报告 | S1 | 总控、产品、开发、测试、运维、安全 | 是，作为 P0 编码完成后的交接和验收证据入口 | P0 首批编码包、160 验证结果、生产前置台账 |
| `p1-coding-completion-audit-v0.1.md` | P1 编码完成审计 | S1 | 总控、产品、开发、测试、运营、供应商 | 是，作为 P1 当前可编码范围完成结论和验收证据入口 | P1 API 契约、160 验证结果、Semgrep 扫描 |
| `p2-coding-candidate-audit-v0.1.md` | P2 编码候选审计 | S1 | 总控、产品、开发、测试、供应商 | 是，作为 P2 开工排序和外部依赖边界入口 | 本文档、P1 完成审计、各业务设计文档 |
| `superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md` | P2-A 平面图导入开发计划 | S1 | 总控、产品、开发、测试、运维 | 是，作为 P2-A 正式 TDD 编码执行清单 | P2 候选审计、P1 平面图能力、FloorPlan 后端模块 |
| `p2-floor-plan-import-api-contract-v0.1.md` | P2-A 平面图导入接口契约 | S1 | 产品、开发、测试、运营、实施 | 是，作为 P2-A 接口、权限、事件和验收依据 | P2-A 开发计划、后端实现、V034 迁移 |
| `p2-floor-plan-import-completion-audit-v0.1.md` | P2-A 平面图导入完成审计 | S1 | 总控、产品、开发、测试、实施 | 是，作为 P2-A 当前完成结论和验证证据入口 | 160 测试结果、P0 契约闸口、Semgrep 扫描 |
| `superpowers/plans/2026-06-15-p2-amusement-group-private-booking.md` | P2-B 游乐团体/包场开发计划 | S1 | 总控、产品、开发、测试、运营 | 是，作为 P2-B TDD 编码执行清单 | P2 候选审计、游乐业务设计、Amusement 后端模块 |
| `p2-amusement-group-private-booking-api-contract-v0.1.md` | P2-B 游乐团体/包场接口契约 | S1 | 产品、开发、测试、运营 | 是，作为团体/包场容量锁、审批、释放、权限和事件依据 | P2-B 开发计划、V035 迁移、后端实现 |
| `p2-amusement-group-private-booking-completion-audit-v0.1.md` | P2-B 游乐团体/包场完成审计 | S1 | 总控、产品、开发、测试、运营 | 是，作为 P2-B 完成结论和验证证据入口 | 160 测试结果、容量锁测试、权限测试 |
| `superpowers/plans/2026-06-15-p2-ticket-dynamic-pricing.md` | P2-C 票务动态定价开发计划 | S1 | 总控、产品、开发、测试、运营 | 是，作为 P2-C TDD 编码执行清单 | P2 候选审计、票务业务设计、Ticketing 后端模块 |
| `p2-ticket-dynamic-pricing-api-contract-v0.1.md` | P2-C 票务动态定价接口契约 | S1 | 产品、开发、测试、运营 | 是，作为动态定价规则、发布、模拟、权限和事件依据 | P2-C 开发计划、V036 迁移、后端实现 |
| `p2-ticket-dynamic-pricing-completion-audit-v0.1.md` | P2-C 票务动态定价完成审计 | S1 | 总控、产品、开发、测试、运营 | 是，作为 P2-C 完成结论和验证证据入口 | 160 测试结果、已发布动态定价影响正式订单测试、草稿规则仅模拟测试、权限测试 |
| `superpowers/plans/2026-06-15-p2-business-metric-snapshots.md` | P2-D 经营指标快照开发计划 | S1 | 总控、产品、开发、测试、运营 | 是，作为 P2-D TDD 编码执行清单 | P2 候选审计、数据分析设计、Analytics 后端模块 |
| `p2-business-metric-snapshots-api-contract-v0.1.md` | P2-D 经营指标快照接口契约 | S1 | 产品、开发、测试、运营 | 是，作为经营指标快照读取、权限和验收依据 | P2-D 开发计划、V037 迁移、后端实现 |
| `p2-business-metric-snapshots-completion-audit-v0.1.md` | P2-D 经营指标快照完成审计 | S1 | 总控、产品、开发、测试、运营 | 是，作为 P2-D 完成结论和验证证据入口 | 160 测试结果、种子数据和读取测试 |
| `p2-third-party-deep-integration-boundary-contract-v0.1.md` | P2-E 第三方深度集成边界契约 | S1 | 总控、产品、开发、供应商、测试 | 是，限边界、适配器契约和后续资料清单；不得替代真实供应商 API | P2 候选审计、第三方接入包、供应商资料 |
| `p2-third-party-deep-integration-boundary-completion-audit-v0.1.md` | P2-E 第三方深度集成边界完成审计 | S1 | 总控、产品、开发、供应商、测试 | 是，作为“边界已收口、真实接入待资料”的验收入口 | 边界契约、供应商资料到位情况 |
| `p2-python-ai-service-boundary-contract-v0.1.md` | P2-F 独立 Python AI 服务边界契约 | S1 | 总控、产品、开发、算法/AI、测试 | 是，限服务边界、接口占位和后续资料清单；不得替代正式 AI 服务实现 | P2 候选审计、AI/规则处理口径 |
| `p2-python-ai-service-boundary-completion-audit-v0.1.md` | P2-F 独立 Python AI 服务边界完成审计 | S1 | 总控、产品、开发、算法/AI、测试 | 是，作为“边界已收口、正式服务后置”的验收入口 | 边界契约、模型和部署资料到位情况 |
| `p2-full-completion-audit-v0.1.md` | P2 全量完成审计 | S1 | 总控、产品、开发、测试、运营、供应商 | 是，作为 P2-A 至 P2-F 当前完成和暂缓边界总入口 | 各 P2 子项完成审计、160 验证结果 |
| `p2-current-diff-adversarial-review-v0.1.md` | P2 当前差异对抗式审计 | S1 | 总控、产品、开发、测试、安全、运维 | 是，作为 160 未提交 P2 差异的质量、安全和回滚判断入口 | 当前 git diff、测试结果、Semgrep 扫描 |
| `development-documentation-adversarial-audit-v0.1.md` | 开发文档完整性、业务逻辑可用性和验收可执行性对抗式审计 | S1 | 总控、产品、开发、测试、运营、供应商、交付负责人 | 是，作为当前全量开发文档审计入口 | 文档地图、业务拆解、P0/P1/P2/P3 风险、可开发/不可上线结论 |
| `development-documentation-audit-map-v0.1.csv` | 全量开发相关文档地图 CSV | S1 | 产品、开发、测试、交付、外部智能体 | 是，作为 297 份开发相关文档的盘点索引 | 文档路径、标题、桶、状态、引用和风险标记 |
| `development-ready-spec-draft-v0.1.md` | 完整后台管理端和真实外部依赖联调的可开发规格草案 | S1 | 产品、开发、测试、供应商、交付负责人 | 是，作为文档不足以直接编码时的下一版 SRS 草案 | 后台页面矩阵、供应商接入模板、UI 验收矩阵、开发前置闸口 |
| `full-admin-srs-v0.1.md` | 完整后台管理端正式 SRS，定义范围、角色、功能、非功能、验收底线 | S1 | 产品、开发、测试、运营、财务、IT | 是，作为完整后台页面开发的需求基线 | 页面权限字段矩阵、UI 验收矩阵、OpenAPI、实现矩阵 |
| `full-admin-page-permission-field-matrix-v0.1.csv` | 完整后台逐页页面、权限、字段、接口、状态、审计事件矩阵 | S1 | 产品、开发、测试、权限/审计负责人 | 是，作为后台页面拆分和验收引用表 | 完整后台 SRS、UI 验收矩阵、RBAC、接口契约 |
| `full-admin-ui-acceptance-matrix-v0.1.md` | 完整后台 UI 自动化验收路径、异常流、权限流、窄屏标准 | S1 | 前端、测试、产品、验收负责人 | 是，作为 Playwright 补测和页面验收依据 | 页面权限字段矩阵、现有 UI 验收报告、E2E 测试 |
| `full-admin-mvp-coding-package-v0.1.md` | 完整后台首批 MVP 页面开发包，拆分总控、账号权限、票务、卡座、财务、供应商、生产证据 | S1 | 前端、后端、测试、产品、交付负责人 | 是，作为首批后台页面正式编码任务包 | 完整后台 SRS、页面权限字段矩阵、UI 验收矩阵、OpenAPI、E2E 测试 |
| `full-admin-mvp-coding-task-breakdown-v0.1.csv` | 完整后台首批 MVP 页面任务拆解表，列出切片、文件、接口、测试和风险 | S1 | 前端、测试、项目负责人 | 是，作为任务派发和回归检查清单 | 开发包、页面矩阵、UI 验收矩阵 |
| `t3d-local-llm-ai-quality-v0.1.md` | T3-D1 本地大模型 AI 质检建议 | S1 | 总控、产品、开发、测试、安全、运营 | 是，作为本地大模型辅助建议、loopback-only 适配、脱敏、人审兜底和机器验证入口 | T3 安全闸口、T3-D1 设计文档、后端实现和 `verify:t3d-local-llm-ai-quality` |
| `production-readiness-implementation-ledger-template-v0.1.md` | 生产前置实施台账模板 | S1 | 运维、IT、采购、财务、测试、项目管理 | 是，作为生产准入资料管理入口 | 本文档、各 Runbook、OpenAPI |
| `formal-security-baseline-generation-task-v0.1.md` | 正式安全基线生成任务 | S1 | 技术负责人、安全、测试、运维 | 是，作为正式安全基线生成和安全准入入口 | 本文档、OpenAPI、运行时安全报告 |
| `p0-api-contract-pack-v0.1.md` | 接口契约说明 | S1 | 后端、前端、Android、一体机、供应商、测试 | 是 | 本文档、OpenAPI |
| `openapi/openapi-p0.yaml`、`openapi/openapi-p0.json` | 机器可读接口契约 | S1 | 后端、前端、Android、一体机、小程序/App/Web、供应商、测试 | 是 | 本文档、接口契约包 |
| `p0-openapi-operation-acceptance-matrix-v0.1.md` | operation 验收矩阵 | S1 | 后端、前端、Android、测试、供应商 | 是 | OpenAPI、权限矩阵、事件矩阵 |
| `p0-openapi-contract-closure-appendix-v0.1.md` | OpenAPI 权限、事件、任务追溯收口附录 | S1 | 后端、前端、Android、一体机、测试、供应商、项目管理 | 是 | OpenAPI、operation 验收矩阵 |
| `p0-task-api-permission-event-acceptance-map-v0.1.md` | 任务-接口-权限-事件-验收映射 | S1 | 开发负责人、测试、项目管理 | 是 | OpenAPI、验收矩阵 |
| `p0-user-story-map-and-sprint-slices-v0.1.md` | 用户故事/迭代切片 | S1 | 产品、开发、测试、运营 | 是 | 本文档、任务映射表 |
| `p0-operation-permission-matrix-v0.1.md` | 权限矩阵 | S1 | 后端、前端、Android、测试、运营管理 | 是 | 本文档、统一口径基准 |
| `p0-action-event-evidence-matrix-v0.1.md` | 留痕/证据矩阵 | S1 | 后端、测试、审计、运营 | 是 | 本文档、统一口径基准 |
| `code-contract-difference-and-migration-audit-v0.1.md` | 代码契约迁移审计 | S1 | 技术负责人、后端、前端、Android | 是，作为旧原型迁移闸口 | 本文档、OpenAPI |
| 各业务 PRD | PRD/产品范围 | S2 | 产品、运营、测试 | 否，需映射到 S1 文档后开发 | 本文档、统一口径基准 |
| 各开发设计文档 | 模块设计/技术方案 | S2 | 开发、测试、供应商 | 是，但不得覆盖 S1 接口和验收 | S1 接口、权限、事件、任务映射 |
| `third-party-supplier-integration-kit-v0.1.md` | 供应商接入包 | S2 | 供应商、集成开发、测试 | 是，限接口占位和适配框架 | OpenAPI、第三方开发设计 |
| `business-module-mature-system-gap-analysis.md` | 市场成熟系统参考/增强建议 | S3 | 产品规划、后续路线图 | 否 | 本文档、P0 任务清单 |
| 历史审计报告、旧进度快照、归档源文件 | 历史证据 | S3 | 审计追溯 | 否 | 当前入口和 S0/S1 文档 |

### 3.8 角色最小阅读包

任何角色进入开发、验收、联调或供应商对接前，必须按下表读取最小包。若时间不足，不得跳过 S0/S1 文档去直接读历史设计稿。

| 角色 | 必读包 | 读完后必须能执行的动作 | 不得跳过 |
|---|---|---|---|
| 总控/产品负责人 | 本文档、`unified-standards-baseline-v0.1.md`、`business-decision-log-v0.1.md`、`p0-user-story-map-and-sprint-slices-v0.1.md`、`project-progress-dashboard.md` | 判断范围、优先级、版本边界、是否已收口和是否可进入编码 | 不得用历史审计报告或旧待决策清单反向打开已收口事项 |
| 后端负责人 | `p0-coding-entry-pack-v0.1.md`、`p0-first-coding-package-v0.1.md`、`p0-api-contract-pack-v0.1.md`、`openapi/openapi-p0.yaml/json`、`p0-openapi-operation-acceptance-matrix-v0.1.md`、`p0-openapi-contract-closure-appendix-v0.1.md`、`backend/README.md`、`java-backend-platform-foundation-development-design.md`、`foundation-database-design.md` | 按首批编码包和 operation 开发 Controller、Service、Flyway、权限、幂等、事件和接口测试 | 不得按旧 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*` 或 TypeScript/SQLite 原型开发 |
| Web/小程序/App 前端 | `p0-api-contract-pack-v0.1.md`、`openapi/openapi-p0.json`、`frontend-admin-development-design.md`、`customer-app-miniapp-development-design-v0.1.md`、`p0-openapi-operation-acceptance-matrix-v0.1.md`、`p0-openapi-contract-closure-appendix-v0.1.md`、`code-contract-difference-and-migration-audit-v0.1.md` | 对齐正式路径、请求字段、错误码、权限状态、支付/核销/权益闭环和页面验收 | 不得只把旧路径改前缀，也不得使用未进入 OpenAPI 的字段或状态 |
| Android 工作手机/平板/一体机 | `android-device-terminal-development-design.md`、`mdm-android-enterprise-implementation-plan-v0.1.md`、`apps/android-terminal/README.md`、`openapi/openapi-p0.json`、`p0-openapi-operation-acceptance-matrix-v0.1.md`、`p0-openapi-contract-closure-appendix-v0.1.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` | 区分工作手机、平板、售票型一体机、非售票型一体机能力；按设备绑定、终端限制、在线核销和留痕实现 | 不得把 `/api/mobile/*` 当作正式后端路径；未纳管设备不得生产使用 |
| 测试/审计 | `p0-first-coding-package-v0.1.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md`、`p0-openapi-operation-acceptance-matrix-v0.1.md`、`p0-openapi-contract-closure-appendix-v0.1.md`、`p0-operation-permission-matrix-v0.1.md`、`p0-action-event-evidence-matrix-v0.1.md`、`p0-task-api-permission-event-acceptance-map-v0.1.md`、`formal-security-baseline-generation-task-v0.1.md`、`t3d-local-llm-ai-quality-v0.1.md` | 按业务闭环、首批编码包、operation、权限、事件、证据、安全基线、AI 脱敏、人审兜底和异常场景编写验收用例 | 不得只按页面路径或模块标题验收；不得把 AI 建议当成自动处罚、退款、核销、调价、绩效或合同状态依据 |
| 运营/财务/门店管理 | `product-requirements-v0.1.md`、`bar-operations-development-design.md`、`ticketing-pass-entitlement-development-design.md`、`finance-ar-ap-ledger-development-design-v0.1.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` | 验证通票、权益、酒水领取、存酒、卡座、最低消费、扫码支付、核销、应收应付和日结对账闭环 | 不得用口头流程替代系统留痕和财务主口径 |
| 第三方供应商/集成开发 | `third-party-supplier-integration-kit-v0.1.md`、`open-platform-security-auth-development-design.md`、`third-party-integration-development-design.md`、`openapi/openapi-p0.yaml/json`、`p0-openapi-contract-closure-appendix-v0.1.md` | 明确平台调用供应商、供应商调用平台、签名、Webhook、轮询校验、字段映射和异常补偿边界 | 不得引用旧 `/api/integration/*` 作为正式联调路径 |
| 运维/IT/采购 | `p0-coding-entry-pack-v0.1.md`、`p0-first-coding-package-v0.1.md`、`production-readiness-implementation-ledger-template-v0.1.md`、`formal-security-baseline-generation-task-v0.1.md`、`server-160-runbook-v0.2.md`、`backend/README.md`、`mdm-android-enterprise-implementation-plan-v0.1.md`、`production-credential-rotation-runbook-v0.1.md`、`nas-backup-and-restore-runbook-v0.1.md`、`nas-it-controlled-mount-handoff-v0.1.md`、`nas-diagnostic-report-v0.1.md` | 完成首批编码环境配套、160 部署、回滚、健康检查、MDM/Android Enterprise、账号密钥轮换、NAS 备份恢复、NAS 受控挂载交接、NAS 当前诊断、安全基线输入和生产前置台账 | 不得把本地原型、未纳管设备、无安全基线或未演练备份视为生产就绪 |

## 4. 已归档、废止或不得作为执行入口的内容

| 内容 | 当前处理 | 规则 |
|---|---|---|
| TypeScript / SQLite / Prisma / Fastify 后端原型 | 已从正式仓库物理移除；只允许通过 Git 历史和历史审计文档追溯 | 不得恢复为正式 P0 开发、mock、seed、smoke 或供应商联调入口；恢复会被 `verify:no-legacy-api-prototype` 阻断 |
| `apps/web`、`apps/android-terminal` 现有旧路径调用 | 非正式路径已迁入 `/api/v1/*` 或 `/api/open/*`；迁移盘点使用 `npm run audit:client-routes` | 不得以 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*` 或旧请求字段名反向约束正式后端；正式联调前必须通过 `npm run verify:p0-contract`，其展开包含 OpenAPI、示例、operation 矩阵、追溯附录、客户端路径、客户端 OpenAPI 覆盖和客户端请求字段闸口 |
| Node/Fastify/SQLite 旧安全扫描或威胁模型 | 已通过 `security-scans/LEGACY-SECURITY-SCANS.md` 标注 legacy；`security-scans/threat_model.md` 已替换为 Java/Spring/PostgreSQL/Flyway 正式安全基线重建入口 | 不得作为当前 Java/PostgreSQL 安全设计依据；正式 Java 后端落地后重做安全基线 |
| 历史对抗式审计报告 | 保留追溯价值 | 不再作为当前执行入口，已吸收结论看当前文档 |
| 旧线程索引、旧进度快照 | 已归档或应归档 | 当前进度看 `project-progress-dashboard.md` |
| 写有历史未决措辞或 `pending_confirm` 类业务状态的旧文档 | 若事项已被本文档或 `unified-standards-baseline-v0.1.md` 收口，则不再作为开放产品问题 | 应回改为“已确认执行口径”或“实施前输入项” |

说明：`pending_confirm`、`待复核`、`待支付`、`待核销` 等作为业务状态枚举时，不代表产品仍有开放问题；是否需要总控拍板只看本文档、`business-decision-log-v0.1.md` 和 `final-open-decisions-and-owner-checklist-20260610.md` 的当前口径。

历史能力表统一规则：所有旧文档中保留的 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*`、`/api/public/*`、相对路径或“能力路径”表格，只能作为历史能力清单和迁移参考。每一行能力在进入编码前必须能映射到 `openapi/openapi-p0.yaml/json` 的 exact path 或 operationId，并进入 `p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `p0-openapi-contract-closure-appendix-v0.1.md`；无法映射的能力一律视为“未进入 OpenAPI，不得开发”，不得生成 Java Controller、SDK/mock、接口自动化或供应商联调文档。

业务状态措辞规则：当“待确认”“待复核”“待支付”“待核销”是枚举状态时，文档必须写成“业务状态：待确认”等上下文，不得裸写成开放决策问题。真正需要拍板的内容只能出现在当前有效决策清单或本文准入条件中。

## 5. P0 正式开发准入条件

以下事项完成前，不允许把对应模块标记为“可正式并行开发完成”。

### 5.1 P0 验证链分层

P0 验证不再只使用一个总命令表达。为避免 Java/Android 环境缺失掩盖已可先验的文档契约，也避免把契约通过误判为完整上线通过，统一按下表执行：

| 验证层 | 命令 | 通过后代表什么 | 不代表什么 |
|---|---|---|---|
| 契约层 | `npm run verify:p0-contract` | OpenAPI、示例、720 个 operation 矩阵、追溯附录、客户端路径、客户端 OpenAPI 覆盖和请求字段已闭合（operation 数以 OpenAPI/矩阵实时为准） | 不代表后端 Java、Web 构建、Android APK 或 160 环境已通过 |
| Web 层 | `npm run verify:p0-web` | 契约层通过，且 Web 可构建 | 不代表后端和 Android 环境已就绪 |
| 环境层 | `npm run verify:p0-env` | 后端 Java/Maven 与 Android Java/Gradle/SDK 前置环境满足验收条件 | 本机缺 Java 21 时不得判定代码失败；Java/后端测试和完整 P0 默认在 160 或等效 Java 21 环境执行 |
| 完整 P0 | `npm run verify:p0` | 契约、后端测试、Web 构建、Android 环境、Android 构建、smoke 和 APK 验证均通过 | 这是完整验收口径，不能用单项通过替代；当前协作中以后端/Java 测试在 `192.168.88.160` 执行为准 |

| 准入项 | 适用范围 | 未完成时允许做什么 | 未完成时禁止做什么 |
|---|---|---|---|
| P0-0 正式后端工程准入 | 后端、数据库、CI、接口自动化、SDK/mock、160 运维 | 可整理原型、迁移计划和文档契约 | 未建立 Java 21 + Spring Boot 4.x + Maven + PostgreSQL + Flyway + springdoc-openapi 工程，并通过 `ops/160/complex-verify-be000.sh` 或等效验收前，不得把任一业务域标记为正式并行开发完成 |
| P0 首批编码包 | 后端、数据库、权限、事件、应收应付、主体绑定、生产前置台账 | 可继续维护 OpenAPI、任务清单和 smoke | 未按 `p0-first-coding-package-v0.1.md` 完成首批包结构、统一响应、Flyway、权限模板、事件审计、主体绑定、应收应付和生产前置台账前，不得宣称 P0 正式业务实现已进入稳定开发态 |
| P0 API Contract Pack + OpenAPI YAML/JSON | 前端、后端、Android、一体机、小程序/App/Web、第三方 | 可做领域建模、页面草图、mock | OpenAPI 缺失或与权限矩阵不一致时不得进入真实联调和接口冻结 |
| P0 operation 级验收矩阵 | 后端、前端、Android、一体机、小程序/App/Web、测试、供应商 | 可按域拆开发故事 | 未覆盖字段、权限、认证上下文、终端、幂等和事件时不得标记接口完成 |
| OpenAPI 契约闸口 | 后端、前端、Android、一体机、小程序/App/Web、测试、供应商 | 可修改契约草案 | `npm run verify:openapi` 未通过时不得进入 P0 联调、自动化测试或供应商 mock |
| OpenAPI 示例闸口 | 前端、后端、Android、一体机、小程序/App/Web、测试、供应商 | 可补 example 和 mock payload | `npm run verify:openapi-examples` 未通过时不得进入 P0 联调、自动化测试或供应商 mock；高风险写接口和外部接入接口不得只有 schema 没有样例 |
| operation 矩阵同步闸口 | 后端、前端、Android、一体机、小程序/App/Web、测试、供应商 | 可重建验收矩阵 | `npm run verify:operation-matrix` 未通过时不得进入 P0 联调、自动化测试或供应商 mock |
| OpenAPI 权限/事件/任务追溯附录闸口 | 后端、前端、Android、一体机、小程序/App/Web、测试、供应商、项目管理 | 可重建追溯附录 | `npm run verify:openapi-closure` 未通过时不得进入 P0 联调、自动化测试、供应商 mock 或任务完成验收 |
| 代码契约迁移审计 | 旧 Web、旧 Android、旧 API 原型迁移 | 可参考旧交互和流程 | 未清理旧正式调用路径、未覆盖 OpenAPI、operation 矩阵不同步，或 `npm run verify:p0-contract` 未通过时不得进入真实联调 |
| 用户故事地图与迭代切片 | 全部 P0 业务模块 | 可维护任务清单 | 不得只按表结构排期 |
| 任务-接口-权限-事件-验收映射表 | 全部 P0 开发任务 | 可先补草表 | 不得宣称任务可验收 |
| Java/PostgreSQL/Flyway/Maven 脚手架 | 后端、数据库、CI | 可整理原型和迁移计划 | 不得沿用 Node/SQLite 作为正式后端 |
| 160 API 部署/回滚/健康检查脚本 | 运维、预生产、生产验收 | 可按 Runbook 执行 Java jar 发布、回滚、健康检查和 BE-000 验收 | 不得启动旧 `apps/api` 或备份旧 SQLite 原型库作为正式资产 |
| MDM 设备管控方案 | Android 工作手机、平板、一体机 | 可做模拟设备和测试环境 | 未纳管设备不得作为生产工作终端 |
| 聚合支付商户号、密钥、回调和对账文件 | 支付、退款、日结、对账 | 可做支付抽象和 mock | 不得生产收款/退款上线 |
| 酒吧年龄/人脸校验供应商或等效设备 | 酒吧强制年龄校验 | 可做接口占位和测试 mock | 不得标记生产正式通过 |
| NAS 备份与恢复演练 | 生产环境 | 真实 SMB 挂载、读写删除测试、NAS 同步备份和从 NAS 恢复演练已通过；最新证据为 `docs/production-readiness-evidence/nas-restore-drill-latest.json` | 生产前仍需固化开机自动挂载、定时备份、容量告警、失败告警和周期性恢复演练 |
| 生产前置资料实施台账 | 运维、IT、采购、财务、安全、供应商接入、生产准入 | 可按模板收集资料、账号、证据和阻塞范围 | 未建立 `production-readiness-implementation-ledger-template-v0.1.md` 对应台账，或 `npm run verify:ops160-recovery` 未通过前，不得用口头说明或零散截图替代上线前置证据 |
| 正式安全基线 | 后端、OpenAPI、开放平台、支付、设备、第三方、160 运维、生产上线 | V1 已生成并可通过 `npm run verify:security-baseline` 与 `npm run verify:production-readiness-evidence` 复核 | 仍有 P0/P1 open findings 或生产前置 pending_info 时，不得进入公开生产安全准入 |

## 6. 第一批必须新增或补强的文档

| 优先级 | 文档 | 目的 |
|---|---|---|
| P0 | `p0-api-contract-pack-v0.1.md` + `openapi/openapi-p0.yaml` + `openapi/openapi-p0.json` | 已新增并落地机器可读契约。固化正式 OpenAPI 基线、错误码、幂等、签名、分页、Webhook、回调和接口目录 |
| P0 | `p0-openapi-operation-acceptance-matrix-v0.1.md` | 已新增。把本轮核验 720 个 OpenAPI operation 的请求体、权限、认证上下文、数据范围、终端、幂等和事件转成可测试验收矩阵（operation 数以 OpenAPI/矩阵实时为准） |
| P0 | `p0-openapi-contract-closure-appendix-v0.1.md` | 已新增。由 OpenAPI 反向生成权限、事件、任务追溯和样例准入附录，补齐人工矩阵未逐 operation 展开的缺口 |
| P0 | `code-contract-difference-and-migration-audit-v0.1.md` | 已新增。明确旧 TypeScript/Fastify/SQLite API、旧 Web/Android 路径和正式 Java/OpenAPI 契约的迁移闸口 |
| P0 | `p0-user-story-map-and-sprint-slices-v0.1.md` | 已新增。按角色和业务闭环拆用户故事，避免只按模块开发 |
| P0 | `p0-task-api-permission-event-acceptance-map-v0.1.md` | 已新增。把任务、接口、权限、事件、验收标准绑定起来 |
| P0 | `prototype-deprecation-and-technology-baseline-v0.1.md` | 已新增。明确废止 TS/SQLite 后端原型，固定 Java/PostgreSQL/Maven 正式路线 |
| P0 | `p0-coding-entry-pack-v0.1.md` | 已新增。把正式后端、OpenAPI、operation 验收、权限、留痕、任务映射、供应商 mock 和旧原型阻断集中成可编码入口包 |
| P0 | `p0-first-coding-package-v0.1.md` | 已新增。把 BE-001/BE-002/DB-001/AUTH/事件/主体绑定/应收应付/生产前置台账压缩成首批可编码、可验收工作包 |
| P0 | `production-readiness-implementation-ledger-template-v0.1.md` | 已新增。把 MDM、支付、NAS、密钥、人脸年龄核验、企业微信、第三方供应商、160、安全基线等生产前置资料做成实施台账模板 |
| P0 | `formal-security-baseline-generation-task-v0.1.md` | 已新增。定义正式安全基线的触发条件、输入、输出、阻断规则和验收标准 |
| P0 | `amusement-p0-safety-inspection-spec-v0.1.md` | 已新增。固化游乐开放前阻断式巡检和恢复闭环 |
| P0 | `data-isolation-and-tenant-scope-security-spec-v0.1.md` | 2026-06-18 新增。多租户/商户数据隔离与越权防护权威规格：scope 注入只来自 token、逐表（V044–V052）隔离列、Given/When/Then 越权验收、V050 复核持久化表字段级文档；正式承接 `domain-ownership-scope-audit-v0.1.csv` |
| P0 | `p0-first-coding-package-v0.1.md` | 2026-06-18 补齐缺失文件（此前入口引用但文件不存在）。首批可编码/可验收工作包入口与索引 |
| P1 | `third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md` | 2026-06-18 新增。短信/通知能力开关+字段映射+失败场景+mock；对象存储/MDM/视频字段映射占位；占位错误码命名空间 |
| 审计 | `development-documentation-audit-v0.2.md` | 2026-06-18 新增。对当前代码态（207 ops/52 迁移/673 测试）刷新文档审计，取代 `development-documentation-adversarial-audit-v0.1.md`；含文档地图、需求-接口-页面-数据表-测试矩阵之矩阵、P0/P1/P2/P3 问题清单 |

## 7. AI-ready 实现文档强制规则

所有实现类文档（后端、前端、Android、票务、酒吧、供应链、游乐、第三方、风控、终端、160 运维）在进入编码前必须满足以下四件套。若原文没有单独章节，默认按本节和下列引用执行，不得让开发或测试自行猜测。

| 四件套 | 当前执行口径 | 主要引用 |
|---|---|---|
| 反模式 | 不得沿用旧 TypeScript/SQLite 后端、旧 `/api/admin/*`、旧 `/api/mobile/*`、旧 `/api/integration/*`、相对接口路径、旧请求字段名、无事件写表、无权限校验、无幂等写接口、无证据的高风险操作 | `prototype-deprecation-and-technology-baseline-v0.1.md`、`code-contract-difference-and-migration-audit-v0.1.md`、`p0-task-api-permission-event-acceptance-map-v0.1.md` |
| 测试用例 | 每个 P0 故事必须能映射到 Given/When/Then、operation 验收矩阵、OpenAPI 追溯附录、端到端验收和边界场景；没有验收映射的任务不得标记完成 | `p0-user-story-map-and-sprint-slices-v0.1.md`、`p0-openapi-operation-acceptance-matrix-v0.1.md`、`p0-openapi-contract-closure-appendix-v0.1.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| 错误处理 | 所有写接口必须覆盖参数错误、无权限、状态冲突、幂等冲突、第三方不可用、供应商回调重复/漏发、支付/库存/核销失败和人工复核路径 | `p0-api-contract-pack-v0.1.md`、`openapi/openapi-p0.yaml`、`p0-action-event-evidence-matrix-v0.1.md`、`third-party-supplier-integration-kit-v0.1.md` |
| 精确引用 | 文档引用必须使用当前有效文件名、OpenAPI exact path、operationId 或追溯附录条目；不得使用“见相关文档”“接口待定”“路径同上”等模糊写法 | `openapi/openapi-p0.json`、`p0-openapi-contract-closure-appendix-v0.1.md`、`p0-task-api-permission-event-acceptance-map-v0.1.md`、`delivery-package-manifest-v0.1.md` |

## 8. 进入下一章前的审计结论

当前第一批 P0 硬缺口已经收口：原型废止声明已由 `prototype-deprecation-and-technology-baseline-v0.1.md`、`verify:no-legacy-api-prototype` 和 `code-contract-difference-and-migration-audit-v0.1.md` 承接，P0 可编码入口已由 `p0-coding-entry-pack-v0.1.md` 承接，首批正式编码范围已由 `p0-first-coding-package-v0.1.md` 承接，160 API 部署/回滚/健康检查/BE-000 验收脚本已由 `ops/160/*.sh` 和 `server-160-runbook-v0.2.md` 承接，生产前置资料已由 `production-readiness-implementation-ledger-template-v0.1.md` 承接，正式安全基线生成与维护任务已由 `formal-security-baseline-generation-task-v0.1.md`、`security-scans/formal-security-baseline-v1.md`、`security-scans/findings-register-v1.md` 和 `verify:production-readiness-evidence` 承接，接口契约已由 `p0-api-contract-pack-v0.1.md`、`openapi/openapi-p0.yaml`、`openapi/openapi-p0.json`、`verify:p0-contract` 和 `p0-openapi-operation-acceptance-matrix-v0.1.md` 承接，OpenAPI 权限、事件、任务追溯缺口已由 `p0-openapi-contract-closure-appendix-v0.1.md` 和 `verify:openapi-closure` 承接，Web 可构建性已由 `verify:p0-web` 承接，后端/Android 环境前置已由 `verify:p0-env` 承接，用户故事和迭代切片已由 `p0-user-story-map-and-sprint-slices-v0.1.md` 承接，任务-接口-权限-事件-验收映射已由 `p0-task-api-permission-event-acceptance-map-v0.1.md` 承接，游乐 P0 安全巡检闭环已由 `amusement-p0-safety-inspection-spec-v0.1.md` 承接。票务资格 P0 的 `ticket_eligibility_rules`、`student_credential_audits`、多人票 `num_people` 和核销放行人数留痕已收口为正式 Flyway 首批业务迁移必落项，不再作为开放待决策项。160 机器已按最新项目和脚本完成同步，并已通过环境、契约、Web 构建、运行配置预检、API/Web 启动、BE-000 运行态、整体健康检查和完整 `complex-verify-p0.sh`；当前使用 `~/.complex/complex.env` 用户级覆盖解决 `/etc/complex/complex.env` 暂无 sudo 写入权限的问题，不在文档或脚本中落真实密钥。注意：当前完整 P0 通过的是 dev 环境自动化 smoke 切片，证明正式路径、统一响应、鉴权、Android 流程、APK 和 160 环境可跑通，不代表平台、票务、权益、核销、证据、设备等业务域已完成生产级实现。

下一步不再新增拍板问题；建议直接按 `p0-first-coding-package-v0.1.md` 进入首批编码包执行：先完成 BE-001/BE-002、Flyway 首批正式迁移、权限模板、事件审计、主体绑定、应收应付和生产前置台账，再把当前 `local/dev/test` 内存态 smoke 切片迁移为 PostgreSQL + Flyway + Service/Repository + 权限/事件/审计的正式业务实现，并要求每替换一个 smoke 接口都保持 `complex-verify-p0.sh` 继续通过。生产或长期预生产前，IT/运维仍需把用户级覆盖配置迁入 `/etc/complex/complex.env`，接入 systemd、账号密钥轮换、告警、生产前置台账，并关闭或正式豁免安全基线 findings 中的公开生产阻断项。

## 9. 变更日志

| 日期 | 版本 | 变更 |
|---|---|---|
| 2026-06-16 | V0.1 开发文档审计 | 新增开发文档完整性对抗式审计、全量文档地图 CSV、可开发规格草案，并补入口 UI 自动化和 gstack 交付审计；同步修正 P2 动态定价旧口径残留 |
| 2026-06-10 | V0.1 | 新增当前有效文档入口、正式口径、阅读顺序、废止内容和 P0 开发准入条件 |
| 2026-06-10 | V0.1 补充 | 标记 P0 任务映射表和游乐安全巡检规格已新增；审计结论改为第一批硬缺口已收口 |
| 2026-06-11 | V0.1 编码准入补强 | 新增 operation 级验收矩阵和代码契约迁移审计入口；明确旧 Web/Android 路径不得反向约束正式 Java/OpenAPI 后端 |
| 2026-06-12 | V0.1 可编码准入收口修复 | 新增 P0-0 正式后端工程准入；同步 OpenAPI 认证上下文、写接口请求体和旧路径降级规则，明确未建立正式 Java/Spring/PostgreSQL/Flyway 工程前不得标记业务域正式并行开发完成 |
| 2026-06-12 | V0.1 AI-ready 交付补强 | 新增实现文档四件套规则；明确 `pending_confirm` 类业务状态不等于产品开放问题；所有实现类文档必须按反模式、测试用例、错误处理和精确引用进入编码 |
| 2026-06-12 | V0.1 事实源矩阵收口 | 新增文档类型、事实源等级和适用对象矩阵；明确 S0/S1/S2/S3 冲突裁决关系，防止旧路径和市场参考文档反向覆盖正式 OpenAPI |
| 2026-06-13 | V0.1 入口引用与验证分层修复 | 修复角色最小阅读包中的失效文件名；新增 `verify:p0-contract`、`verify:p0-web`、`verify:p0-env`、`verify:p0` 四层验证口径；把 MDM、密钥轮换和 NAS 生产前置 Runbook 作为运维/IT/采购入口 |
| 2026-06-13 | V0.1 生产前置台账接口收口 | 新增生产前置责任状态 P0 轻量台账/API，OpenAPI operation 总数更新为 181 个 |
| 2026-06-12 | V0.1 可编码入口包补强 | 新增 `p0-coding-entry-pack-v0.1.md` 为编码、联调、测试和供应商 mock 统一入口；同步说明 `apps/api` 旧脚本已阻断 |
| 2026-06-12 | V0.1 160 脚本收口 | 160 API 部署、回滚、健康检查、BE-000/P0 验收和备份脚本切换为 Java/Spring Boot/PostgreSQL/Flyway 口径 |
| 2026-06-12 | V0.1 OpenAPI 契约闸口 | 新增 `npm run verify:openapi` 为 P0 联调、自动化测试和供应商 mock 前置检查 |
| 2026-06-12 | V0.1 npm 工作区收口 | 根 npm workspace 和 lockfile 移除旧 `apps/api`、Fastify、Prisma 依赖；旧 API 子目录仅保留阻断提示和历史参考 |
| 2026-06-15 | V0.1 旧原型物理清理 | `apps/api` 已从 160 正式仓库物理移除；`verify:no-legacy-api-prototype` 已纳入 `verify:p0-contract`，防止旧原型回流 |
| 2026-06-15 | V0.1 160 编码保护基线 | 新增 `coding-host-protection-baseline-v0.1.md`、`verify:coding-host-protection` 和实施证据；完整 P0 验证、正式提交和安全扫描只认 160 仓库证据 |
| 2026-06-15 | V0.1 T3-0 编码包 | 新增 T3 编码入口、用户故事地图、安全与供应商就绪闸口、入口审计和 `verify:t3-coding-package`；下一步只允许优先进入 T3-A 只读经营分析 |
| 2026-06-15 | V0.1 T3-A 只读经营分析 | Web 管理端经营分析入口按 T3-A 只读边界收口，修正 analytics 字段映射并新增 `verify:t3a-readonly-analytics` |
| 2026-06-15 | V0.1 T3-B SDK/mock 套件 | 新增开放平台签名工具、Webhook 信封样例、provider adapter mock 测试样例和 `verify:t3b-supplier-mock-kit` |
| 2026-06-15 | V0.1 T3-D1 本地大模型 AI 质检建议 | 新增 160 本地大模型 loopback-only 质检建议能力、脱敏、人审兜底和 `verify:t3d-local-llm-ai-quality`；不进入独立 Python AI 服务和 AI 自动决策 |
| 2026-06-12 | V0.1 客户端旧路径闸口 | 新增 `npm run audit:client-routes` 和 `npm run verify:client-routes` 作为 Web/Android/一体机/供应商 mock 正式联调前置检查 |
| 2026-06-12 | V0.1 客户端 OpenAPI 覆盖闸口 | 新增 `npm run audit:client-openapi` 和 `npm run verify:client-openapi`，覆盖检查升级为路径模板 + HTTP 方法一致性，防止客户端只改路径前缀但未补机器契约 |
| 2026-06-12 | V0.1 客户端请求字段闸口 | 新增 `npm run audit:client-payload` 和 `npm run verify:client-payload`，防止客户端路径正确但请求字段未对齐 OpenAPI requestBody |
| 2026-06-12 | V0.1 operation 矩阵同步闸口 | 新增 `npm run verify:operation-matrix`，防止 OpenAPI 已修改但逐接口验收矩阵未同步 |
| 2026-06-13 | V0.1 OpenAPI 示例闸口 | 新增 `npm run docs:openapi-examples` 和 `npm run verify:openapi-examples`；高风险写接口和外部接入接口必须具备机器可读 example，防止供应商 mock、接口测试和 SDK 生成继续猜 payload |
| 2026-06-13 | V0.1 OpenAPI 权限/事件/任务追溯附录闸口 | 新增 `p0-openapi-contract-closure-appendix-v0.1.md`、`npm run docs:openapi-closure` 和 `npm run verify:openapi-closure`；以 OpenAPI 反向生成权限、事件、任务簇、终端和样例准入追溯 |
| 2026-06-13 | V0.1 角色最小阅读包与旧路径隔离补强 | 新增总控、后端、前端、Android、测试、运营、供应商、运维的最小阅读包；旧路径联调闸口补入 `verify:openapi-closure` |
| 2026-06-13 | V0.1 160 实机更新与历史能力表规则收口 | 补充 160 最新项目同步后的验证边界；统一规定历史能力表必须映射 OpenAPI exact path 或 operationId，否则不得进入正式开发 |
| 2026-06-13 | V0.1 票务资格与160回滚收口 | 明确票务资格规则表、学生凭证留痕表、多人票人数和放行人数留痕为正式 Flyway 首批业务迁移必落项；同步声明 160 API 回滚不得绕过运行配置预检 |
| 2026-06-13 | V0.1 160 运行态与完整 P0 边界收口 | 160 已通过用户级运行配置覆盖、API/Web 启动、BE-000 运行态和整体健康检查；当时完整 P0 处于阶段性待收口状态，后续已由 release `20260613-183652` 和完整 `complex-verify-p0.sh` 收口 |
| 2026-06-13 | V0.1 完整 P0 smoke 通过 | 160 API release `20260613-183652` 发布后完整 `complex-verify-p0.sh` 已通过；同时明确 dev smoke 切片不等同于生产级业务实现 |
| 2026-06-13 | V0.1 安全基线入口收口 | `security-scans/threat_model.md` 不再保留旧 TypeScript/SQLite 原型威胁模型正文，改为 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 正式安全基线重建说明 |
| 2026-06-13 | V0.1 160 复核与运行态同步 | 本轮文档和 OpenAPI 描述同步到 160 后，API release `20260613-193142` 已发布；运行时 `/docs/openapi` 已确认包含 `Java 21 + Spring Boot 4.x`，完整 `complex-verify-p0.sh` 于 19:32 再次通过 |
| 2026-06-13 | V0.1 首批编码与生产前置收口 | 新增 `p0-first-coding-package-v0.1.md`、`formal-security-baseline-generation-task-v0.1.md`、`production-readiness-implementation-ledger-template-v0.1.md`，把首批编码范围、安全基线生成任务和生产前置资料台账纳入 S1 执行事实源 |
| 2026-06-14 | V0.1 NAS 与 160 验证口径收口 | 明确 NAS 服务器为 `192.168.88.113`；Java/后端集成测试、BE-000 和完整 P0 验收默认在 160 或等效 Java 21 环境执行，本机缺 Java 21 只记录为本机环境前置缺失 |
| 2026-06-14 | V0.1 NAS 参数收口 | 明确 NAS 共享 `bn`、UNC `//192.168.88.113/bn`、SMB 用户 `jxh1`、160 挂载 `/mnt/complex-nas-backup`、保留 30 天、容量上限 1TB；当前总控口径为备份归档不加密，需显式配置 `ALLOW_UNENCRYPTED_BACKUPS=true` |
| 2026-06-15 | V0.1 P1 编码收口 | 新增 `p1-coding-completion-audit-v0.1.md`，记录 P1 当前可编码能力、验证证据和供应商/硬件/BOM 等外部依赖边界 |
| 2026-06-15 | V0.1 P2 候选审计 | 新增 `p2-coding-candidate-audit-v0.1.md`，推荐 P2 优先从商场平面图 CAD/SVG 标准化导入开始，供应商深度联动和独立 Python AI 服务暂缓 |
| 2026-06-15 | V0.1 P2-A 开发计划 | 新增 `superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md`，把平面图 SVG/标准 JSON 导入拆为可 TDD 执行的开发计划 |
| 2026-06-15 | V0.1 P2-A 接口契约 | 新增 `p2-floor-plan-import-api-contract-v0.1.md`，固化平面图导入接口、权限、事件、解析规则和验收标准 |
| 2026-06-15 | V0.1 P2-A 完成审计 | 新增 `p2-floor-plan-import-completion-audit-v0.1.md`，记录 P2-A 平面图导入完成范围、验证证据和未纳入范围 |
| 2026-06-18 | V0.1 文档审计刷新与缺口补齐 | 刷新文档审计到当前代码态（207 ops/52 迁移/673 测试），新增 `development-documentation-audit-v0.2.md`（取代 adversarial-audit-v0.1）、`data-isolation-and-tenant-scope-security-spec-v0.1.md`、`third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md`；**补齐此前被记录为"已新增"但文件实际缺失的 `p0-first-coding-package-v0.1.md`**；修正本入口 operation 数 181→207（历史变更记录里的 181/191/198 为旧快照，operation 数以 `p0-openapi-operation-acceptance-matrix-v0.1.md` 与 OpenAPI 实时为准）|
| 2026-06-20 | V0.1 第一入口与事实基线刷新 `文档goal(2026-06-20)` | 刷新事实基线 729→1251 测试 / 209→386 op / V060→V124（实证：`openapi-p0.yaml` 方法统计、`backend/target/surefire-reports`、`backend/src/main/resources/db/migration`、`playwright --list` 42）；新增 §2.3 登记 R6（§36 已确认 14 项）/R7（暂未设立）与本会话新功能落地（V117/V120–V124）；新建仓库根 `README.md` 与分类文档地图 `docs/documentation-map-v0.1.md` |
| 2026-06-23 | V0.1 事实基线刷新 `文档goal(2026-06-23)` | 刷新当前事实基线 1251→1436 测试 / 386→419 op / V124→V155；同步 `security-scans/formal-security-baseline-v1.md` 与 `security-scans/findings-register-v1.md` 到当前 OpenAPI 419 operation；业务口径仍以业务人员 HTML 为最终解释权，除非该 HTML 后续被正式修改。 |
| 2026-06-30 | V0.1 业务 HTML 事实源验收规格与基线刷新 | 新增 `business-html-source-of-truth-acceptance-spec-v0.1.md`；刷新本轮取证口径为 Flyway V182（179 个迁移）、OpenAPI 516 operations、后端 surefire 现有报告 1606 tests、Playwright 默认列表 31 tests in 5 files；不声明刚完成全量验证。 |

| T3-B SDK/mock 套件 | 已新增 | `docs/t3b-supplier-sdk-mock-kit-v0.1.md` |
| T3-B SDK/mock 套件计划 | 已新增 | `docs/superpowers/plans/2026-06-15-t3b-supplier-mock-kit.md` |

| T3-B SDK/mock 套件验收 | 已新增 | `npm run verify:t3b-supplier-mock-kit` |
| T3-D1 本地大模型 AI 质检建议 | 已新增 | `docs/t3d-local-llm-ai-quality-v0.1.md` |
| T3-D1 本地大模型 AI 质检验收 | 已新增 | `npm run verify:t3d-local-llm-ai-quality` |

### Partial 需求 Mock/沙箱验收覆盖报告 V0.1

- `docs/partial-requirement-mock-coverage-v0.1.md`: 本文由 `tools/generate-partial-requirement-mock-coverage.mjs` 从 `docs/requirements-code-implementation-matrix-v0.1.csv` 和 `docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.json` 生成，用于证明 8 条 partial 需求虽然不能宣称生产闭环，但已有 mock、adapter、页面、后端测试或 E2E 验收替身，可以支撑联调和业务验收。
- `docs/partial-requirement-mock-coverage-latest.json`: machine-readable snapshot for partial requirement count, mock/sandbox acceptance readiness, production blockers and linked files.
- Verify: `npm run verify:partial-requirement-mock-coverage`.
- Boundary: MOCK_IS_NOT_PRODUCTION_EVIDENCE. Mock, adapter, page, backend test and E2E evidence can support integration/business acceptance, but cannot replace production evidence manifest.


## 会员、积分与商户积分结算重构入口（2026-08-01）

- 会员与等级唯一权威：`docs/member-and-tier-product-prd-v1.0.md`
- 积分与虚拟券唯一权威：`docs/points-and-voucher-product-prd-v1.0.md`
- 商户积分结算唯一权威：`docs/merchant-points-settlement-rules-v1.0.md`
- 储值现行职责：`docs/member-loyalty-and-stored-value-product-prd-v0.1.md`
- 签到与生日边界：`docs/stored-value-membership-checkin-birthday-prd-v0.1.md`
- 系统配置覆盖检查：`docs/member-points-p1-ops-parameter-tables-v0.1.md`，只检查系统是否承载，不是运行时配置真源。
- 旧混合 PRD：`docs/member-points-tier-and-clearing-prd-v1.0.md` 已被替代，只保留历史入口。
- 批准设计：`docs/superpowers/specs/2026-07-28-member-points-settlement-redesign-design.md`
- 审计范围：`docs/prd-audit/member-points-rebuild-scope-2026-07-28.md`
- 逐文件正式审计：`docs/prd-audit/member-points-related-doc-audit-2026-07-28.md`；以审计时 22 份现行 PRD 为基线，8 份精准同步、14 份无需修改。
- HTML 覆盖关系：`docs/prd-audit/html-prd-coverage.csv` 已登记三份新权威与三个业务 HTML 的 9 组待逐条映射关系；具体替代文本由 Phase 4 只读审计产生。
- HTML 边界：三个业务 HTML 中相关条款只读，须先完成逐条替代清单并获用户批准；未涉及业务仍由 HTML 最终解释。


## 通票分账与组织主体口径入口（2026-07-28）

- **业务真源**：业务 HTML 提交 `f19ea732`（PR #250，10 组 28 处，用户逐条批准）——`business-system-map-for-operations-v0.1.html`、`platform-governance-map-for-operations-v0.1.html`、`business-enum-quick-reference-v0.1.html`。
- **口径索引**：`docs/prd-audit/HTML-PASS-SETTLEMENT-WATERFALL-2026-07-28.md`（瀑布、赠送权益、收入归属、同步落点、实现缺口、HTML 哈希）。
- **术语与组织主数据权威表**：`docs/terminology-canonical-v1.0.md` §三/§五/**§六 组织主数据登记**；结算侧副本 `docs/settlement-rebuild-prd-v1.0.md` §2.3A。
- **业务裁决**：`docs/business-decision-log-v0.1.md` **DEC-ORG-2026-07-28-01~07**；审计侧同步 `docs/prd-audit/business-decision-ledger.md`「2026-07-28 新增业务裁决」节。
- **要点**：① 专名「娱乐运营平台」废止 → **娱乐平台公司**；② 分账瀑布 = 渠道抽成 → 赠送权益预估成本（娱乐平台公司）→ 门票综合服务费/留存（同一项只扣一次，**归娱乐平台公司**）→ 可分配池仅各酒吧分；③ 赠送权益每样独立、绑提供方、按内部结算价计成本，预扣按票种配「按件计／按票整额」；④ 赠酒每吧至少一杯、杯数写票面、**不得跨吧核销**；⑤ 非散台服务费与订台低消/二次消费/额外酒水归**各酒吧商户**（⚠️可重议）、场地租金归**长沙项目公司**（= 资产公司）、向商户供货货款归**中央餐饮公司**。
- **准入提示**：以上 ②③④ 与「综合服务费归属主体」**代码尚未实现**，属待开发缺口（见 `docs/dev-plan-v7-audit-20260722.md` R-1/R-3）；本轮为**文档同步批次，未改代码、未新增端点**。

### 第二轮（2026-07-28）综合／联合通票 + 免费权益 + 内部结算 SKU 目录

- **业务真源**：业务 HTML 提交 `e314c595`（PR #257，A/B/D）+ `88eba70c`（PR #260，C/E），用户逐条批准。
- **口径索引**：`docs/prd-audit/HTML-PASS-SETTLEMENT-WATERFALL-2026-07-28.md` **§七**。
- **业务裁决**：`docs/business-decision-log-v0.1.md` **DEC-ORG-2026-07-28-08~12**；审计侧同步 `docs/prd-audit/business-decision-ledger.md`「2026-07-28 新增业务裁决 · 第二轮」节。
- **承接权威**：A/B → `settlement-rebuild-prd-v1.0.md` **§5.0c**；D → 同 PRD **§5.1 两段分配**；C → 同 PRD **§6.6**；E → `finance-settlement-product-prd-v0.1.md` **§3B**（现状-目标差距权威）+ 结算侧指针 §9.7。
- **要点**：① **A** 综合／联合通票售价 =（游乐价 + 酒吧价 + 收费的餐饮/零售价）× 折扣，**免费的（票面价 0）不计入价值**；② **B** 整单先扣渠道抽成 → 按**各业态票面原价占比**拆份额 → 游乐/餐饮/零售各自归属 → **仅酒吧份额**再扣赠送权益预估成本与门票综合服务费 → 剩余进池；拆份额**只按业态大分类**，**依附加赠权益不作为业态层拆分对象**，**票面原价与内部结算价不可混用**；③ **D** 可分配池**两段分配**（「每客单价/固定额」先拿绝对额、余额由「排名档位」按比例分尽、**拿了第一段的不再参与第二段**），**替代原「三选一」**；④ **C** 免费权益成本承担分**商户让利型 / 平台出资型**两类，两类**均不参与份额拆分**，原「0 元也按内部结算价结算」一刀切**作废**；⑤ **E** 🆕 **新需求·完全未开发** 内部结算 SKU 目录（唯一真源 / 唯一可编辑处 / 草案→他人复核转正 / 核销结算固化当时价只读），现状**一价四存**（V152 酒吧商品、V257 酒吧套餐、V225 权益模板、V301 券台账）。
- **准入提示**：本轮 **A/B/C/D/E 五条代码全部未跟上**；其中 **C 的现状与新口径直接冲突**（核销侧对 0 元权益一律取价结算 = 全按平台出资型处理，平台会为商户让利型多付成本）。本轮为**文档同步批次，未改代码、未新增端点**。排期见 `docs/pending-development-backlog-v0.1.md` 批 10/11。
