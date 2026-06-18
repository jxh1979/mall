# 第一轮 文档地图 R1（2026-06-18）

> 定位：本图是第一轮审计对 `docs/` 全量文档（357 个文件，md≈190 / csv≈8 / json≈40 / html≈3 / 其余归档）的**当前态盘点与裁决标记**。它**不取代**既有 `development-documentation-audit-map-v0.1.csv`（297 行，已滞后），而是：①给出按桶的当前态视图；②逐项标注"是否过期/是否重复/是否冲突"；③把本轮新发现的滞后点显式登记。
> 列含义：文档路径｜类型｜当前用途｜是否过期｜是否重复｜是否与他文档冲突｜关联模块｜关联接口/页面/数据表/测试。
> 机器标记：R1_DOC_MAP_READY、AUDIT_MAP_CSV_STALE_NEEDS_REGEN。

## 0. 总览与桶统计

| 桶 | 数量(约) | 当前态判断 |
|---|---:|---|
| index-governance 入口/基线/追溯/治理 | 33 | S0/S1 当前权威；`development-documentation-audit-map-v0.1.csv` 已滞后需重生成 |
| prd 产品总纲+模块 PRD | 14 | S2 产品事实源；治理 PRD 有 R1-LOOP-09 入职入口冲突 |
| design-spec 开发设计/层规格 | 22 | S2；多处 `merchant_id` 写 uuid 与迁移 VARCHAR 不符（ISO-GAP-05）；6 大业务设计有 R1-LOOP 矛盾 |
| machine-contract OpenAPI | 2 | S1 机器契约；207 ops，准确 |
| delivery-contract 验收/operation 矩阵/closure | 52 | S1 核心验收依据 |
| security 安全基线/隔离 | 3+ | 数据隔离规格已建但只到 V052（漏 V053/V054）|
| third-party 供应商接入 | 4 | SMS 占位未并入主开关矩阵 |
| ops-readiness 生产前置 | 16 | Runbook 齐全，真实证据待回填 |
| plans-reviews / production-evidence | 113 | 阶段性证据，高度重复，建议归档收敛 |
| archive 历史 | 45 | 仅追溯，不作开发入口 |

## 1. index-governance（入口/基线/追溯/治理）

| 路径 | 类型 | 当前用途 | 过期 | 重复 | 冲突 | 模块 | 关联 |
|---|---|---|---|---|---|---|---|
| `00-current-effective-document-entry-v0.1.md` | S0 入口/裁决 | 第一阅读入口、废止口径、准入清单 | 否（181→207 已修）| 否 | 否 | 全局 | 全部 |
| `unified-standards-baseline-v0.1.md` | S0 统一基线 | D1–D13 唯一权威口径 | 否 | 否 | 否（裁决源）| 全局 | 全部接口/表 |
| `business-decision-log-v0.1.md` | S0 决策日志 | 历史业务决策证据 | 否 | 否 | 否 | 全局 | — |
| `requirement-traceability-matrix-v0.1.md` | S1 追溯 | REQ-01~92 → 文档章节 | 否 | 否 | 否 | 全局 | 全部 |
| `requirements-code-implementation-matrix-v0.1.csv` | S1 追溯 | 96 项需求→代码/测试（88 完整/8 部分）| 否 | 与上行互补 | 否 | 全局 | 全部 |
| `product-documentation-index-v0.1.md` | 索引 | 产品侧文档总索引 | 部分（未含 0618 新文档）| 否 | 否 | 全局 | — |
| `development-documentation-audit-map-v0.1.csv` | S1 文档地图 | 297 文档盘点 | **是·已滞后**（漏 0618/full-admin-srs/zoom-out 等）| 与本图互补 | 否 | 全局 | — |
| `development-documentation-adversarial-audit-v0.1.md` | 审计 | 历史文档审计 | **是·已被 v0.2 取代** | 是 | 否 | 全局 | — |
| `development-documentation-audit-v0.2.md` | 审计 | 当前态文档审计（0618）| **部分·迁移数已漂移**（写 V052，实际 V054）| 与本轮 R1 互补 | 否 | 全局 | — |
| `master-roadmap.md` / `project-progress-dashboard.md` | 治理 | 路线/进度 | **是**（dashboard 停在 360 测试/181 op/P0-only）| 否 | 与实测冲突 | 全局 | — |

## 2. prd / design-spec（产品与开发设计）

| 路径 | 类型 | 用途 | 过期 | 重复 | 冲突 | 模块 | 关联 |
|---|---|---|---|---|---|---|---|
| `product-requirements-v0.1.md` | S2 PRD | 产品总纲 | 否 | 否 | 否 | 全局 | — |
| `platform-governance-prd-v0.1.md` / `-product-spec-v0.2.md` | S2 | 治理/主体/合同/权限 | 否 | spec 已合并多源 | **是·R1-LOOP-09**（未承认后台手动录入入口）| 治理 | governance API/30 页 |
| `ticketing-*prd/dev-design` | S2 | 票务/通票/权益/核销 | 否 | 否 | **R1-LOOP-10/11/12**（放行状态/刷新周期/多人容量）| 票务 | ticketing API/`ticket_*` 表 |
| `bar-business-operations-product-prd` / `bar-operations-development-design.md` | S2 | 酒吧/卡座/低消/存酒/提成 | 否 | 否 | **R1-LOOP-01~06**（提成/支付/定金/存酒/库存事件）| 酒吧 | bar API/`bar_*` 表 |
| `supply-chain-inventory-*` | S2 | 供应链/库存/存酒 | 否 | 否 | R1-LOOP-06（库存事件名）| 库存 | inventory API/`inventory_*` |
| `amusement-*` | S2 | 游乐/容量/安全巡检 | 否 | 否 | R1-LOOP-12（多人票容量）| 游乐 | amusement API |
| `operation-traceability-and-terminal-switch-prd-v0.1.md` | S2（事件字典权威）| 留痕三层模型 + 终端开关 | 否 | 否 | **R1-LOOP-06/07**（事件双轨/留痕率无 KPI/过程枚举缺）| 全局 | 事件中心 |
| `finance-ar-ap-ledger-development-design-v0.1.md` | S2 | 应收应付台账 | 否 | 否 | **R1-LOOP-01/13**（提成无生成流程/支付枚举）| 财务 | finance API/`ar_ap_items` |
| `foundation-database-design.md` | S2 | 数据库设计 | 部分 | 否 | ISO-GAP-05（merchant_id uuid vs VARCHAR）| 全局 | 全表 |
| `full-admin-srs-v0.1.md` | S1 SRS | 完整后台需求基线 | 否 | 否 | R1-LOOP-09/14（入职入口/字段/事件）| 后台 | 30 页矩阵 |
| `full-admin-page-permission-field-matrix-v0.1.csv` | S1 页面矩阵 | 30 页×权限×字段×事件 | 否 | 否 | R1-LOOP-14 | 后台 | 全后台页面 |

## 3. machine-contract / delivery-contract / security

| 路径 | 类型 | 用途 | 过期 | 冲突 | 关联 |
|---|---|---|---|---|---|
| `openapi/openapi-p0.{yaml,json}` | S1 机器契约 | 207 ops 真实契约 | 否（实测 207）| R1-LOOP-08（缺兑换/入场 verify 接口）| 全 API |
| `p0-openapi-operation-acceptance-matrix-v0.1.md` | S1 | 唯一未漂移的 op 数源（207）| 否 | 否 | 全 API |
| `p0-openapi-contract-closure-appendix-v0.1.md` | S1 | op→权限/事件/任务/终端追溯 | 否 | 否 | 全 API |
| `p0-end-to-end-acceptance-and-edge-cases-v0.1.md` | S1 | 端到端验收+边界 | 否 | 否 | 核心闭环 |
| `data-isolation-and-tenant-scope-security-spec-v0.1.md` | S1 安全规格 | 多租户隔离 + 越权验收 | **部分·只到 V052**（漏 V053/V054）| ISO-GAP-01~05 | V044–V052 表 |
| `domain-ownership-scope-audit-v0.1.csv` | S1 | 逐表 scope 盘点 | 部分 | 否 | 全表 |
| `security-scans/formal-security-baseline-v1.md` / `findings-register-v1.md` | S1 安全 | 安全基线/findings | 部分·缺租户越权条目（ISO-GAP-01）| 否 | 全局 |

## 4. third-party / ops-readiness（节选，全量见 CSV）

| 路径 | 类型 | 用途 | 过期 | 冲突 | 关联 |
|---|---|---|---|---|---|
| `integration-capability-switches-v0.1.md` | S1 | 14 能力开关矩阵 | **部分·缺 sms_notification 行** | 否 | 全外部集成 |
| `third-party-supplier-integration-kit-v0.1.md` | S2 | adapter/字段映射/补偿 | 部分·对象存储/MDM/视频字段未下沉（已由占位文档补）| 否 | 第三方 |
| `third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md` | S1 占位 | SMS+对象存储/MDM/视频字段映射+占位错误码 | 否（0618 新建）| 落地动作未执行（未并入主矩阵）| 第三方/登录 |
| `external-dependency-gates-v0.1.md` | S1 闸口 | 外部依赖准入门 | 否 | 否 | 第三方/AI |
| `production-blocker-register-v0.1.md` | S1 台账 | 6 前置+4 凭证+2 安全+11 暂缓 | 否（生成式）| 否 | 生产 |
| `t3d-local-llm-ai-quality-v0.1.md` | S1 | 160 本地 AI 只读质检 | 否 | 否 | 分析/AI |
| `server-160-runbook-v0.2.md` / `nas-*` / `mdm-*` / `production-credential-*` | S1 Runbook | 部署/备份/MDM/凭证 | 否 | 否 | 运维 |

## 5. plans-reviews / production-evidence / archive（收敛建议）

- `superpowers/plans/*`（20+）、各 `*-completion-audit-v0.1.md`、`*-acceptance-audit*`、`production-readiness-evidence/**`（61）：阶段性证据，**高度重复**（v0.2 §6 R1/R2 已指出 5 份验收审计 + P0/P1/P2 完成审计重叠）。建议：保留 `closed-loop-live-acceptance-audit-20260618.md`（当前最准实测）+ 本轮 R1 + v0.2 为当前态，其余降级归档，不删除。
- `_archive/**`（45）：仅历史追溯，含旧计数，**不作开发入口**，已正确标历史。

## 6. 本图登记的"必须重生成/补登"清单（移交 owner）
1. 重生成 `development-documentation-audit-map-v0.1.csv`（补 0618 新文档、full-admin-srs、zoom-out、本轮 R1 5 份）。
2. `data-isolation-and-tenant-scope-security-spec` §3 补 V053/V054（并改为脚本自动生成）。
3. `integration-capability-switches-v0.1.md` 并入 `sms_notification` 行（占位文档 §5）。
4. `project-progress-dashboard.md` 重写为当前态（207 op/673 测试/V054/P0+P1+P2+T3）。
5. 把本轮 R1 5 份交付物 + 0618 两份 + 数据隔离规格 + SMS 占位挂入 S0 入口。
