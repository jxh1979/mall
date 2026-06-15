# 全量文档收口与合并归档审计 V0.1

## 1. 审计结论

本轮逐一检查并按本报告执行归档、合并和旧路线材料丢弃后，`docs` 当前全量为 115 个 Markdown、2 个 HTML，共 117 个文件；其中主目录活跃入口为 70 个 Markdown、2 个 HTML，共 72 个文件，历史文件集中在 `_archive/20260610-retention-audit/`。

结论：

1. 当前产品方向、P0/P1/P2/P3 边界、实名口径、MDM、企业微信、支付、备份、主体绑定、应收应付、PM Critic findings 均已收口。
2. 后续开发、业务审计和决策过程应以当前权威入口为准；历史审计报告、旧线程索引、原型技术路线和原始发现明细不再作为当前决策依据。
3. 可合并、归档或丢弃的文档主要集中在四类：历史审计证据、旧线程索引、旧进度快照、早期交付说明。
4. 已按总控最新要求丢弃不再使用的旧技术路线和旧进度快照材料；保留 `server-160-runbook-v0.2.md` 作为 160 运维合并入口。
5. 已补入 P0 权限矩阵、操作事件证据矩阵、端到端验收边界用例、任务-接口-权限-事件-验收映射表、游乐 P0 安全巡检闭环规格和第三方供应商接入预留包；这些文档是当前 P0 编码、测试和供应商联调的新增执行入口。

## 2. 当前权威入口

后续开发、业务审计和决策优先阅读以下文档：

| 用途 | 权威文档 |
|---|---|
| 当前第一阅读入口 | `00-current-effective-document-entry-v0.1.md` |
| 总入口 | `delivery-package-manifest-v0.1.md` |
| 能力层索引 | `system-capability-layers-product-index-v0.1.md` |
| 交付物索引 | `product-documentation-index-v0.1.md` |
| 统一口径 | `unified-standards-baseline-v0.1.md` |
| 原型废止与正式技术栈基线 | `prototype-deprecation-and-technology-baseline-v0.1.md` |
| P0 API 契约基线 | `p0-api-contract-pack-v0.1.md` |
| P0 用户故事地图与迭代切片 | `p0-user-story-map-and-sprint-slices-v0.1.md` |
| P0 任务-接口-权限-事件-验收映射 | `p0-task-api-permission-event-acceptance-map-v0.1.md` |
| 游乐 P0 安全巡检闭环 | `amusement-p0-safety-inspection-spec-v0.1.md` |
| 总纲 | `product-requirements-v0.1.md` |
| 决策来源 | `business-decision-log-v0.1.md` |
| 最终执行口径 | `final-open-decisions-and-owner-checklist-20260610.md` |
| P0 开发任务 | `p0-implementation-task-list.md` |
| P0 权限矩阵 | `p0-operation-permission-matrix-v0.1.md` |
| P0 操作留痕矩阵 | `p0-action-event-evidence-matrix-v0.1.md` |
| P0 端到端验收与边界用例 | `p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| 第三方供应商接入预留包 | `third-party-supplier-integration-kit-v0.1.md` |
| PM Critic 与验收 | `pm-critic-and-acceptance-criteria-20260610.md` |
| 项目进度总览 | `project-progress-dashboard.md` |
| 线程唯一登记 | `thread-registry.md` |

## 3. 可归档或低价值文档

这些文档在后续开发、业务审计、产品决策中已不应作为入口使用；价值主要是历史追溯。本轮已按下表移入归档目录。

| 文件 | 建议 | 原因 | 当前替代入口 |
|---|---|---|---|
| `thread-index.md` | 已归档 | 已标注废弃，线程 ID 与权威登记冲突 | `_archive/20260610-retention-audit/obsolete-thread-indexes/`；替代入口 `thread-registry.md` |
| `thread-map.md` | 已归档 | 已标注废弃，线程 ID 与权威登记冲突 | `_archive/20260610-retention-audit/obsolete-thread-indexes/`；替代入口 `thread-registry.md` |
| `thread-maintenance-map.md` | 已归档 | 已标注废弃，线程 ID 与权威登记冲突 | `_archive/20260610-retention-audit/obsolete-thread-indexes/`；替代入口 `thread-registry.md` |
| 早期技术架构材料 | 已丢弃 | 早期技术路线材料，已不再作为历史参考随包保留 | 替代入口 `system-development-spec.md` |
| `frontend-page-structure-optimization.md` | 已归档 | 内容较短，属于前端页面结构建议 | `_archive/20260610-retention-audit/merged-source-docs/`；替代入口 `frontend-admin-development-design.md` |
| `data-risk-ai-delivery-note.md` | 已归档 | 交付说明性质，内容可并入数据风控开发设计或交付包说明 | `_archive/20260610-retention-audit/merged-source-docs/`；替代入口 `data-risk-ai-development-design.md` |
| `server-160-ops-plan.md` | 已归档并合并 | 与 160 运维实施设计、环境记录重叠 | `_archive/20260610-retention-audit/merged-source-docs/`；替代入口 `server-160-runbook-v0.2.md` |
| 旧 P0 验证快照 | 已丢弃 | 旧阶段验证快照，当前应看任务清单和进度总览 | 替代入口 `project-progress-dashboard.md` |
| 旧 P0 进度快照 | 已丢弃 | 旧进度快照，当前应看正式实施看板 | 替代入口 `project-progress-dashboard.md` |
| 旧 P1 进度快照 | 已丢弃 | 旧进度快照，当前应看正式实施看板 | 替代入口 `project-progress-dashboard.md` |
| 旧 P2/P3 进度快照 | 已丢弃 | 旧进度快照，当前应看正式实施看板 | 替代入口 `project-progress-dashboard.md` |

## 4. 历史审计类文档

这些文档不算“无价值”，但后续不应作为当前产品口径或开发决策入口。本轮已统一移入历史审计包，并在主交付包中只保留归档链接。

| 文件 | 建议 | 原因 | 当前替代入口 |
|---|---|---|---|
| 原始审查明细 TXT | 已丢弃 | 原始发现明细体量大，已被 PM Critic、归档审计和后续修复吸收 | 当前入口 `pm-critic-and-acceptance-criteria-20260610.md` 与本文 |
| `文档审查与完善报告-v0.1.md` | 已丢弃 | 早期审查总报告，当前已被后续收口文档替代 | 当前入口 `document-retention-merge-audit-20260610.md` |
| `document-completeness-accuracy-audit.md` | 已丢弃 | 早期开发设计层完整性审计，范围和文件数已过期 | 当前入口 `document-retention-merge-audit-20260610.md` |
| `documentation-full-audit-and-fix-report-20260608.md` | 已归档 | 早期全量复核报告，结论已被后续收口吸收 | 同上 |
| `documentation-product-flow-audit-and-fix-report-20260608.md` | 已归档 | 产品流复核历史报告，后续五件套已收口 | 同上 |
| `improved-package-review-20260608.md` | 已归档 | 改进包复核历史报告，当前交付包已更新 | 同上 |
| `adversarial-decision-options-20260610.md` | 已归档 | 决策建议来源已被最终执行口径吸收 | 同上 |
| `adversarial-full-document-audit-and-decision-list-20260610.md` | 已归档 | 全量对抗式审计来源，当前可作为追溯，不作执行入口 | 同上 |
| `adversarial-full-document-audit-followup-20260610.md` | 已归档 | 二次对抗式审计补充，结论已落入 P0 任务和设计文档 | 同上 |
| `market-benchmark-adversarial-audit-20260609.md` | 已归档 | 市场基准建议已进入 P1/P2 路线和最终口径，可保留为市场依据 | 同上 |
| `platform-governance-completion-audit-v0.1.md` | 已归档 | 平台治理阶段性完成审计，当前口径以治理五件套为准 | 同上 |

## 5. 可合并但不建议归档的文档组

这些文档仍有价值，但为了减少后续阅读成本，可以在下一版交付包中合并。

| 文档组 | 建议合并方式 | 保留理由 |
|---|---|---|
| 数据分析层五件套 | 已合并为 `data-analysis-layer-spec-v0.2.md` | 文件较薄，合并后更利于开发和审计阅读 |
| 证据风控层五件套：`evidence-risk-*` | 合并为 `evidence-risk-layer-spec-v0.2.md`，PRD 可单独保留 | 页面、字段、流程、验收内容分散但强关联 |
| 第三方接入层五件套：`third-party-access-layer-*` | 合并为 `third-party-access-layer-spec-v0.2.md`，开发设计单独保留 | P0 主要是接口容器、字段、回流和对账，合并更清晰 |
| 终端展示层五件套：`terminal-display-layer-*` | 合并为 `terminal-display-layer-spec-v0.2.md`，终端 PRD 单独保留 | 页面、字段、流程、低保真可整合 |
| 平台治理五件套 | 已合并为 `platform-governance-product-spec-v0.2.md` 与 `platform-governance-acceptance-pack-v0.2.md` | 拆成产品规格和验收包，避免单文件过长 |
| 业务经营五件套 | 已合并为 `business-operations-layer-spec-v0.2.md` | 用户确认继续执行合并，源文件移入归档 |
| 160 运维三件套 | 已合并为 `server-160-runbook-v0.2.md` | 实施人员只需要一份可执行 Runbook |
| 线程/工作流：`thread-registry.md`、`workstream-index.md`、`master-roadmap.md` | 暂时保留三份；废弃旧线程图 | 三者分别承担线程 ID、工作流视图、路线图 |
| 系统拆解类：`system-breakdown.md`、`foundation-systems-breakdown.md`、`business-systems-breakdown.md`、`business-functional-topology-v0.1.md` | 后续可抽一份“系统总览 v0.2”，原文件作为分层详版 | 现在仍承担不同层级说明，暂不归档 |

## 6. 明确保留的开发/业务审计文档

以下文档仍直接服务开发、审计或业务执行，不建议归档：

- 总纲、统一口径、索引、交付包、决策日志、最终执行口径、PM Critic。
- P0 任务清单、权限矩阵、操作留痕矩阵、端到端验收/边界用例、任务-接口-权限-事件-验收映射表、游乐 P0 安全巡检闭环规格、第三方供应商接入预留包、路线图、进度总览、需求追溯矩阵。
- 平台治理、业务经营、第三方接入、证据风控、数据分析、终端展示各层 PRD 和合并规格。
- 各核心开发设计：Java 后端、数据库、前端后台、开放平台、票务、酒吧、供应链、游乐、Android、客人 App/小程序、第三方集成、数据风控 AI、160 运维、主体绑定、应收应付。
- 两个 HTML 业务地图：仍适合给非产品/非技术业务人员理解系统。

## 7. 执行结果

1. 已归档旧线程索引，并丢弃原始审查明细 TXT。
2. 已归档早期审计报告和历史复核报告，并在 `delivery-package-manifest-v0.1.md` 中保留归档说明。
3. 已将旧进度快照从交付包中丢弃，当前进度统一查看 `project-progress-dashboard.md`。
4. 已新增 `server-160-runbook-v0.2.md`，承接 160 运维三件套中的执行入口。
5. 已继续执行合并：平台治理、业务经营、第三方接入、证据风控、数据分析、终端展示和 160 运维均已有合并入口。
6. 已补齐 P0 权限模板、留痕矩阵、端到端验收/边界用例、任务-接口-权限-事件-验收映射表、游乐 P0 安全巡检闭环规格和供应商未定接入预留包，并同步到交付包、索引、任务清单和追溯矩阵。

## 8. 验证记录

本轮检查与归档执行已覆盖全部文件，并执行以下验证：

- 文件数量核对：全量 115 Markdown、2 HTML；主目录活跃入口 70 Markdown、2 HTML。
- V0.6 复核：PM Critic 与验收标准已更新到 `pm-critic-and-acceptance-criteria-20260610.md` V0.6；票务、酒吧、Android、第三方、P0 任务清单中的历史“待决/未决”误导性标题已改为已确认执行口径或后续排期，并新增 P0 权限、留痕、端到端验收和供应商接入占位四份执行入口；本轮进一步修复事件名漂移、第三方敏感原始报文策略和售票型一体机追溯锚点。
- 开放问题关键词扫描：未发现要求再次决策、实名旧待定、法务旧待定、最终开放判断等开放口径残留。
- 冲突标记扫描：未发现 Git 冲突标记。
- 权威入口扫描：`delivery-package-manifest-v0.1.md`、`system-capability-layers-product-index-v0.1.md`、`product-documentation-index-v0.1.md`、`thread-registry.md` 均仍保留当前入口地位。
