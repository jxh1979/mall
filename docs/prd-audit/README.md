# PRD 全量审计

状态：phase1–7 完成；B 线文档债清偿进行中
唯一正式版本：`192.168.88.160:/home/james/complex-management` 的 `main`

## 权威层级

1. `docs/business-system-map-for-operations-v0.1.html`
2. `docs/platform-governance-map-for-operations-v0.1.html`
3. `docs/business-enum-quick-reference-v0.1.html`
4. 用户已确认且未被现行 HTML 推翻的业务裁决
5. 现行 PRD、产品规格、术语、追溯矩阵和产品文档入口

三个 HTML 共同拥有最终业务解释权。PRD 和历史裁决不得反向削弱 HTML；HTML 之间无法唯一推出结论时进入单问题裁决队列。

## 正式范围

- 纳入范围见 `document-scope-manifest.csv`。
- 备份与当前版本关系见 `backup-current-diff.md`。
- 历史和本轮业务裁决见 `business-decision-ledger.md`。
- 原子规则账本和覆盖矩阵在阶段 2 建立。
- 最终结论写入 `prd-audit-report.md`；phase1–7 文档审计已完成；B 线清空高严重度文档债前，不得宣称「PRD 零缺口」。

## 阶段

| 阶段 | 内容 | 状态 |
|---:|---|---|
| 1 | 基线、范围和历史续接 | 完成 |
| 2 | 三个 HTML 原子规则账本 | 完成 |
| 3 | 平台治理与通用规则 | 完成 |
| 4 | 交易、会员、票务与酒吧 | 完成（文档）；残余 DEFERRED 见 B 线 |
| 5 | 地产、招商、合同、楼宇与供应链 | 完成（文档） |
| 6 | 游乐、终端、第三方与数据分析 | 完成（文档）；残余 DEFERRED 见 B 线 |
| 7 | 跨域一致性与收口 | 完成（文档）；XD-006 随 ADV-TX-008 |

## 审计状态

覆盖状态固定为 `FULL`、`PARTIAL`、`CONFLICT`、`MISSING`、`AMBIGUOUS`、`STALE`、`NOT_APPLICABLE`。

严重度固定为：

- `P0`：资金或权益不守恒、越权、自审自批、责任主体错误、不可恢复状态或相反实现。
- `P1`：核心流程或跨域规则缺失，导致业务无法闭环或验收错误。
- `P2`：局部不完整、追溯断裂或术语和状态不一致，但有明确替代路径。
- `P3`：不改变业务含义的引用、结构或表述问题。

## 设计与计划

- `docs/superpowers/specs/2026-07-27-prd-audit-design.md`
- `docs/superpowers/plans/2026-07-27-prd-audit.md`

## B+3 范围与附录轨（2026-07-27）

- `method-b3-scope-and-tracks.md`：方法与三轨定义
- `document-scope-manifest.csv`：现行三轨清单（旧版见 `document-scope-manifest.pre-b3.csv`）
- `phases/01b-scope-rework-b3.md`：范围返工
- `phases/03b-appendix-crosscheck.md`：phase3 附录交叉复核
- `appendix-crosscheck-findings.csv`：交叉复核机器表
- `rounds/2026-07-27-b3-scope-crosscheck-report.md`：本轮总报告

## 全量文档审计状态（2026-07-28）

- 状态：**phase1–7 文档审计完成**
- 最终报告：`prd-audit-report.md`
- 阶段 6/7：`phases/06-amusement-terminal-thirdparty-data.md`、`phases/07-cross-domain-closure.md`
- 覆盖：`html-prd-coverage.csv`（与 authority-rule-ledger 全量对齐）
- 约束：仅文档；不改代码；HTML 未改

