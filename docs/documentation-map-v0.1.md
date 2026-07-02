# 文档地图 V0.1

> 文档goal(2026-06-20)：本文档按类别列出 `docs/` 下**当前有效**文档（文件名 + 一句话用途 + 现行/部分过期/归档标注），作为第一入口 `docs/00-current-effective-document-entry-v0.1.md` 的导航补充。仅列**真实存在**的文件（经 `ls docs/` 实证）。冲突时以 `00-current-effective-document-entry-v0.1.md` §2 与 `unified-standards-baseline-v0.1.md` 为最终口径。
>
> 标注含义：**现行** = 当前有效执行口径；**部分过期** = 仍有参考价值但含旧快照/旧口径，需对照入口文档；**归档** = 历史快照/已被替代，不得作为执行依据。

## 0. 顶层入口

| 文件 | 用途 | 状态 |
|---|---|---|
| `00-current-effective-document-entry-v0.1.md` | 唯一正式口径、废止内容、角色阅读顺序、P0 准入条件 | 现行 |
| `unified-standards-baseline-v0.1.md` | 跨文档唯一标准基线 | 现行 |
| `business-html-source-of-truth-acceptance-spec-v0.1.md` | 三份业务 HTML 到 PRD/开发/OpenAPI/数据库/UI/测试的最小验收映射索引 | 现行 |
| `../README.md` | 仓库第一入口（定位/技术栈/启动/基线/读者路径） | 现行 |
| `product-documentation-index-v0.1.md` | 产品文档索引 | 现行 |
| `system-capability-layers-product-index-v0.1.md` | 六大能力层产品索引 | 现行 |
| `master-roadmap.md` / `workstream-index.md` / `project-progress-dashboard.md` | 路线图 / 工作流索引 / 进度看板 | 现行 |

## 1. 产品 PRD

| 文件 | 用途 | 状态 |
|---|---|---|
| `product-requirements-v0.1.md` | 产品总纲与业务边界 | 现行 |
| `ticketing-entitlement-checkin-product-prd-v0.1.md` | 票务-权益-核销 PRD | 现行 |
| `member-loyalty-and-stored-value-product-prd-v0.1.md` | 会员忠诚度（两本账/签到/生日）与储值/付费会员 PRD（2026-06-26 审计回填需求源） | 现行 |
| `customer-app-miniapp-product-prd-v0.1.md` | 客人端（App/小程序）PRD：两端一套口径/订台定金/订卡邀请函/隐私同意（2026-06-26 审计回填） | 现行 |
| `business-dashboard-metrics-product-prd-v0.1.md` | 经营看板指标 PRD：8 类指标 + 下钻 + 角色可见范围（2026-06-26 审计回填，裁决#7） | 现行 |
| `bar-business-operations-product-prd-v0.1.md` | 酒吧业务运营 PRD | 现行 |
| `amusement-business-operations-product-prd-v0.1.md` | 游乐业务运营 PRD | 现行 |
| `supply-chain-inventory-product-prd-v0.1.md` | 供应链-库存 PRD | 现行 |
| `finance-settlement-product-prd-v0.1.md` | 财务结算 PRD | 现行 |
| `merchant-portal-zone-prd-v0.1.md` | 商户专区 PRD（3.18，2026-07-02 拍板方案 A：后台内商户专区） | 现行 |
| `third-party-business-access-product-prd-v0.1.md` | 第三方业务接入 PRD | 现行 |
| `platform-governance-prd-v0.1.md` / `platform-governance-product-spec-v0.2.md` | 平台治理 PRD / 产品规格 | 现行 |
| `evidence-risk-control-product-prd-v0.1.md` | 证据留痕与风控 PRD | 现行 |
| `terminal-page-product-prd-v0.1.md` / `web-mobile-app-terminal-extension-product-prd-v0.1.md` | 终端页面 / Web-App-终端扩展 PRD | 现行 |
| `operation-traceability-and-terminal-switch-prd-v0.1.md` | 操作留痕与终端开关 PRD | 现行 |
| `real-estate-full-chain-domain-prd-v0.1.md` | 地产全链域 PRD（招商/租赁全周期/物业工单/楼宇设备/能耗/楼宇巡检）授权基线（2026-06-24 业务 HTML 扩授权后补） | 现行 |
| `original-requirements-capture-v0.1.md` | 原始需求采集 | 部分过期（早期采集，以各域 PRD 为准） |

## 2. 需求矩阵 / 追溯

| 文件 | 用途 | 状态 |
|---|---|---|
| `requirement-traceability-matrix-v0.1.md` | 需求到设计/任务/验收追踪 | 现行 |
| `requirements-code-implementation-matrix-v0.1.csv` | 需求到代码实现矩阵 | 现行 |
| `r1-requirement-api-page-table-test-mapping-matrix-20260618.md` | 需求-API-页面-表-测试 映射矩阵 | 现行 |
| `full-admin-mvp-requirements-code-matrix-v0.1.csv` / `full-admin-remaining-requirements-code-matrix-v0.1.csv` | 后台 MVP / 剩余需求-代码矩阵 | 现行 |
| `matrix-migration-claims.json` | 迁移落地声明权威源（2026-06-19） | 现行 |
| `business-functional-topology-v0.1.md` / `business-systems-breakdown.md` / `system-breakdown.md` | 业务功能拓扑 / 系统拆解 | 部分过期（早期拆解，对照 PRD） |

## 3. 开发设计

| 文件 | 用途 | 状态 |
|---|---|---|
| `system-development-spec.md` | 总体技术方案 | 现行 |
| `java-backend-platform-foundation-development-design.md` | Java 后端地基设计 | 现行 |
| `frontend-admin-development-design.md` | 前端管理端开发设计 | 现行 |
| `android-device-terminal-development-design.md` | Android 终端开发设计 | 现行 |
| `customer-app-miniapp-development-design-v0.1.md` | 客户 App/小程序开发设计 | 现行 |
| `member-loyalty-and-stored-value-development-design-v0.1.md` | 会员忠诚度/积分两本账/储值/付费会员订阅/签到/生日 开发设计（2026-06-24 据现有实现 V025/V060/V089/V090/V100 回填） | 现行 |
| `ticketing-pass-entitlement-development-design.md` | 票务-通票-权益开发设计 | 现行 |
| `bar-operations-development-design.md` | 酒吧运营开发设计 | 现行 |
| `amusement-operations-development-design.md` | 游乐运营开发设计 | 现行 |
| `supply-chain-inventory-development-design-v0.1.md` | 供应链-库存开发设计 | 现行 |
| `finance-ar-ap-ledger-development-design-v0.1.md` | 财务应收应付台账开发设计 | 现行 |
| `third-party-integration-development-design.md` | 第三方集成开发设计 | 现行 |
| `open-platform-security-auth-development-design.md` | 开放平台鉴权/安全/签名设计 | 现行 |
| `data-risk-ai-development-design.md` | 数据风控/AI 开发设计 | 现行 |
| `business-entity-binding-development-design-v0.1.md` | 业务实体绑定开发设计 | 现行 |
| `r5-spec-T1..T17-*-dev-design-20260618.md` | R5 各切片（T1–T17）可 TDD 开发设计 | 现行 |
| `r6-spec-C3a-*` / `r6-spec-C4-*-20260618.md` | R6 切片开发设计（有效权限/分阶段质量修复） | 现行 |
| `development-ready-spec-draft-v0.1.md` | 可开发规格草案 | 现行 |
| `business-operations-layer-spec-v0.2.md` / `data-analysis-layer-spec-v0.2.md` / `evidence-risk-layer-spec-v0.2.md` / `third-party-access-layer-spec-v0.2.md` / `terminal-display-layer-spec-v0.2.md` | 六大能力层规格 V0.2 | 现行 |
| `foundation-systems-breakdown.md` / `android-terminal-breakdown.md` / `ticketing-entitlement-settlement-breakdown.md` | 地基/终端/票务结算拆解 | 部分过期（早期拆解） |

## 4. API 契约

| 文件 | 用途 | 状态 |
|---|---|---|
| `openapi/openapi-p0.yaml` / `openapi/openapi-p0.json` | P0 机器可读 OpenAPI 契约；operation 数以 `verify:operation-matrix` / `verify:openapi-closure` 为准，本轮核验 516 operations | 现行 |
| `p0-api-contract-pack-v0.1.md` | P0 接口契约基线 | 现行 |
| `p0-openapi-operation-acceptance-matrix-v0.1.md` | operation 级开发完成定义/验收矩阵 | 现行 |
| `p0-openapi-contract-closure-appendix-v0.1.md` | OpenAPI 反向生成权限/事件/任务追溯附录 | 现行 |
| `p1-*-api-contract-v0.1.md`（bill/table-session/refund/work-order/billing-portal/inventory/contract/points/floor-plan/integration 等） | P1 各能力接口契约 | 现行 |
| `p2-*-api-contract-v0.1.md`（floor-plan-import/group-booking/dynamic-pricing/metric-snapshots/deep-integration/python-ai-boundary） | P2 各能力接口/边界契约 | 现行 |

## 5. 数据表 / 数据库

| 文件 | 用途 | 状态 |
|---|---|---|
| `foundation-database-design.md` | PostgreSQL/Flyway 数据库总设计 | 现行 |
| `ticketing-database-implementation-notes.md` | 票务数据库实现笔记 | 现行 |
| （权威）`../backend/src/main/resources/db/migration/` | 实际 Flyway 迁移；版本与数量以迁移目录为准，本轮核验最新 V182、179 个迁移 | 现行 |

## 6. 页面权限矩阵 / 后台 SRS

| 文件 | 用途 | 状态 |
|---|---|---|
| `full-admin-srs-v0.1.md` | 完整后台管理端 SRS | 现行 |
| `full-admin-page-permission-field-matrix-v0.1.csv` | 后台页面-权限-字段矩阵 | 现行 |
| `p0-operation-permission-matrix-v0.1.md` | P0 operation 权限矩阵 | 现行 |
| `full-admin-mvp-coding-package-v0.1.md` / `full-admin-mvp-coding-task-breakdown-v0.1.csv` | 后台 MVP 开发包/任务拆解 | 现行 |

## 7. 验收 / 端到端

| 文件 | 用途 | 状态 |
|---|---|---|
| `p0-end-to-end-acceptance-and-edge-cases-v0.1.md` | P0 端到端验收与边界用例 | 现行 |
| `full-admin-ui-acceptance-matrix-v0.1.md` | 后台 UI 自动化验收矩阵 | 现行 |
| `full-admin-ui-automation-acceptance-report-20260618.md` / `ui-automation-acceptance-report-v0.1.md` | 后台 UI 自动化验收报告 | 现行 |
| `closed-loop-implementation-ui-acceptance-report-v0.1.md` | 闭环前端体验验收报告 | 现行 |
| `runtime-acceptance-evidence-v0.1.md` | 闭环运行态验收证据 | 现行 |
| `closed-loop-current-state-snapshot-v0.1.md` | 闭环当前态快照 | 现行（数字以 latest.json/verify 实时为准） |
| `final-closed-loop-acceptance-audit-v0.1.md` / `closed-loop-live-acceptance-audit-20260618.md` | 最终/在线闭环验收审计 | 现行 |
| `code-implementation-acceptance-audit-v0.1.md` / `code-implementation-goal-audit-fix-20260618.md` | 代码实现验收/目标审计 | 现行 |
| `gstack-acceptance-ship-audit-v0.1.md` | gstack 需求验收与交付审计 | 现行 |
| `p1-coding-completion-audit-v0.1.md` / `p2-full-completion-audit-v0.1.md` / 各 `p2-*-completion-audit-v0.1.md` | P1/P2 编码完成审计 | 现行 |
| `p0-coding-completion-and-handoff-report-v0.1.md` | P0 编码完成与交接报告 | 现行 |

## 8. 权限 / 事件矩阵

| 文件 | 用途 | 状态 |
|---|---|---|
| `p0-action-event-evidence-matrix-v0.1.md` | P0 动作-事件-证据矩阵 | 现行 |
| `p0-task-api-permission-event-acceptance-map-v0.1.md` | 任务-API-权限-事件验收映射 | 现行 |
| `p0-user-story-map-and-sprint-slices-v0.1.md` | P0 用户故事地图与迭代切片 | 现行 |

## 9. 第三方供应商

| 文件 | 用途 | 状态 |
|---|---|---|
| `third-party-supplier-integration-kit-v0.1.md` | 第三方供应商接入套件（联调入口） | 现行 |
| `t3b-supplier-sdk-mock-kit-v0.1.md` | 供应商 SDK/mock 套件 | 现行 |
| `integration-capability-switches-v0.1.md` | 集成能力开关清单（off/mock/on） | 现行 |
| `r1-third-party-capability-switch-and-mock-strategy-20260618.md` | 第三方能力开关与 mock 策略 | 现行 |
| `r1-pending-supplier-information-backfill-checklist-20260618.md` | 待回填供应商信息清单 | 现行 |
| `third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md` | 短信通知与供应商字段映射占位 | 现行 |
| `external-dependency-gates-v0.1.md` / `external-evidence-requirement-traceability-v0.1.md` | 外部依赖准入闸口 / 外部证据需求追踪 | 现行 |
| `mdm-android-enterprise-implementation-plan-v0.1.md` | 商业 MDM / Android Enterprise 实施计划 | 现行 |

## 10. 运营 / 运维 Runbook

| 文件 | 用途 | 状态 |
|---|---|---|
| `server-160-runbook-v0.2.md` | 160 主机部署/回滚/健康检查 Runbook | 现行 |
| `nas-backup-and-restore-runbook-v0.1.md` | NAS 备份与恢复 Runbook | 现行 |
| `nas-it-controlled-mount-handoff-v0.1.md` | NAS IT 受控挂载交接包 | 现行 |
| `nas-current-state-drift-report-v0.1.md` / `nas-diagnostic-report-v0.1.md` | NAS 当前态漂移 / 只读诊断报告 | 现行 |
| `production-credential-rotation-runbook-v0.1.md` | 生产凭证轮换 Runbook | 现行 |
| `coding-host-protection-baseline-v0.1.md` | 160 编码保护基线 | 现行 |

## 11. 合规

| 文件 | 用途 | 状态 |
|---|---|---|
| `compliance-and-personal-information-protection-requirements-v0.1.md` | 合规与个人信息保护要求 | 现行 |
| `data-isolation-and-tenant-scope-security-spec-v0.1.md` | 数据隔离与租户范围安全规格 | 现行 |
| `contract-date-compliance-field-granularity-options-20260610.md` | 合同日期合规字段粒度选项 | 部分过期（早期选项，以最终裁定为准） |

## 12. 生产就绪证据

| 文件 | 用途 | 状态 |
|---|---|---|
| `delivery-readiness-gates-v0.1.md` | 交付总闸口 | 现行 |
| `production-blocker-register-v0.1.md` | 生产阻断清单（上线把关） | 现行 |
| `production-owner-action-packs-v0.1.md` / `production-evidence-owner-execution-packets-v0.1.md` / `production-evidence-owner-guides-v0.1.md` | 生产责任人动作包/执行包/回填说明 | 现行 |
| `production-evidence-submission-kit-v0.1.md` / `production-evidence-submission-status-v0.1.md` / `production-evidence-manifest-templates-v0.1.md` | 生产证据提交包/状态看板/Manifest 模板 | 现行 |
| `production-evidence-backfill-priority-v0.1.md` / `production-evidence-backfill-execution-ledger-v0.1.md` / `production-evidence-p0-quickstart-v0.1.md` | 生产证据回填优先级/执行台账/P0 快速回填 | 现行 |
| `production-evidence-closure-gap-report-v0.1.md` / `production-existing-evidence-to-manifest-gap-audit-v0.1.md` | 生产闭环缺口/已有证据到 Manifest 差距 | 现行 |
| `production-readiness-implementation-ledger-template-v0.1.md` / `production-readiness-evidence-intake-template-v0.1.md` | 生产前置台账/证据回填模板 | 现行 |
| `formal-security-baseline-generation-task-v0.1.md` | 正式安全基线生成任务 | 现行 |
| `partial-requirement-mock-coverage-v0.1.md` | partial 需求 mock/沙箱验收覆盖（MOCK_IS_NOT_PRODUCTION_EVIDENCE） | 现行 |
| `current-staged-handoff-manifest-v0.1.md` / `delivery-package-manifest-v0.1.md` | 当前暂存交接清单 / 交付包清单 | 现行 |

## 13. 业务决策

| 文件 | 用途 | 状态 |
|---|---|---|
| `business-decision-log-v0.1.md` | 业务决策日志（含 §32–§35 总控拍板、§36 R6 14 项已确认） | 现行 |
| `final-open-decisions-and-owner-checklist-20260610.md` | 待裁决决策与责任人清单 | 部分过期（早期开放项，多数已在 §36 收口） |
| `pm-critic-and-acceptance-criteria-20260610.md` | PM 批判与验收标准 | 现行 |

## 14. 用户故事 / 编码入口

| 文件 | 用途 | 状态 |
|---|---|---|
| `p0-coding-entry-pack-v0.1.md` / `p0-first-coding-package-v0.1.md` | P0 编码入口包 / 首批编码包 | 现行 |
| `p0-implementation-task-list.md` | P0 实现任务清单 | 现行 |
| `t3-coding-entry-pack-v0.1.md` / `t3-user-story-map-and-sprint-slices-v0.1.md` | T3 编码入口/用户故事地图 | 现行 |
| `t3-security-and-supplier-readiness-gate-v0.1.md` / `t3-entry-assessment-and-coding-package-audit-v0.1.md` | T3 安全/供应商就绪闸口/入口审计 | 现行 |
| `t3d-local-llm-ai-quality-v0.1.md` | T3-D1 本地大模型 AI 质检建议 | 现行 |
| `r4-remaining-work-backlog-20260618.md` / `r3-confirmed-gap-priorities-and-fix-backlog-20260618.md` / `r5-development-implementation-backlog-20260618.md` / `architecture-simplification-backlog-20260618.md` | 各阶段剩余工作/缺口/架构简化 backlog | 现行 |

## 15. 审计报告

| 文件 | 用途 | 状态 |
|---|---|---|
| `development-documentation-audit-v0.2.md` | 开发文档审计（取代 adversarial-audit-v0.1） | 现行 |
| `development-documentation-audit-map-v0.1.csv` | 开发文档全量审计地图 | 现行 |
| `development-documentation-adversarial-audit-v0.1.md` | 开发文档对抗式审计 | 归档（已被 v0.2 取代） |
| `codebase-architecture-zoom-out-audit-v0.1.md` | 代码库架构鸟瞰审计 | 现行 |
| `code-contract-difference-and-migration-audit-v0.1.md` | 代码契约差异与迁移审计 | 现行 |
| `business-module-mature-system-gap-analysis.md` | 业务模块与成熟系统差距分析 | 现行 |
| `domain-ownership-scope-audit-v0.1.csv` | 域归属范围审计 | 现行 |
| `r1-requirements-documentation-audit-report-20260618.md` / `r1-documentation-map-20260618.md` | R1 需求文档审计 / R1 文档地图 | 现行 |
| `r2-code-audit-req26b-req21-req30-20260618.md` / `r3-migration-level-drift-audit-20260618.md` | R2 代码审计 / R3 迁移层漂移审计 | 现行 |
| `r5-aggregation-metrics-draft-20260618.md` / `r5-spec-T12-dashboard-aggregation-dev-design-20260618.md` | R5 聚合指标草案/看板聚合设计 | 现行 |
| `full-admin-mvp-code-implementation-audit-v0.1.md` / `full-admin-remaining-code-implementation-audit-v0.1.md` | 后台 MVP/剩余代码实现审计 | 现行 |
| `document-retention-merge-audit-20260610.md` | 文档留存合并审计 | 部分过期（早期审计） |
| `prototype-deprecation-and-technology-baseline-v0.1.md` | 原型废止与技术基线 | 现行 |
| `thread-registry.md` | 会话/线程登记 | 现行 |

## 16. 业务系统地图（HTML）

| 文件 | 用途 | 状态 |
|---|---|---|
| `business-system-map-for-operations-v0.1.html` | 运营视角业务系统地图（最终解释权之一） | 现行 |
| `platform-governance-map-for-operations-v0.1.html` | 运营视角平台治理地图（最终解释权之一） | 现行 |
| `business-enum-quick-reference-v0.1.html` | 业务枚举速查表（最终解释权之一，配套两份地图使用） | 现行 |
| `business-html-source-of-truth-acceptance-spec-v0.1.md` | 三份业务 HTML 到下游文档和验收资料的最小映射索引；不替代 HTML | 现行 |
| `pending-development-backlog-v0.1.md` | 待开发/未完成项登记（带 P1/P2/P3 优先级 + 设计性gating + 已完成消项），全仓盘点结果 | 现行 |

## 17. 归档目录

| 路径 | 用途 | 状态 |
|---|---|---|
| `_archive/20260610-retention-audit/` | 历史留存审计归档 | 归档 |
