# CANONICAL LEDGER INDEX

> 本文件解决一个反复发生的认知污染：`docs/prd-audit/rounds/r8/findings.csv` 位于历史轮次目录
> `rounds/` 之下，任何 agent 看到路径都会当成历史归档而跳过——**它是当前活动数据**。
>
> 实测基线：`1c5c0c7d`（worktree `~/wt-r11`，分支 `docs/prd-audit-r11`）。
> 下列计数全部为**本轮实测**（按 CSV 解析器计数据行，非 `wc -l`；四份文件末尾均有换行，无末行截断）。
> **凡本轮有变动的数字一律双列给出「基线值 → 本轮落地后值」**——基线＝提交 `1c5c0c7d`，
> 落地后＝本轮 worktree 工作区（尚未 commit）。写死单值会让本文件出厂即过期，那正是它要防的事。

```
CURRENT_CANONICAL_FINDINGS: docs/prd-audit/rounds/r8/findings.csv
⚠ DO NOT TREAT AS HISTORICAL ARCHIVE —— 该文件虽在 rounds/ 下，是当前活动数据
```

## 四张 canonical 台账（唯一真源）

| 路径 | 数据行（基线 → 落地后） | 表头列 | 作用 |
|---|---:|---|---|
| `docs/prd-audit/entry-registry.csv` | **1210 → 1210**（本轮未改） | `entry_id,html_file,entry_kind,entry_name,line_now,first_sentence_fingerprint,section_h2,section_h3` | 三份 HTML 的条目注册表（`entry` 1059 + `section` 151，实测） |
| `docs/prd-audit/authority-rule-ledger.csv` | **1225 → 1225**（本轮未改） | `rule_id,html_file,entry_name,line_hint,source_text,domain,…,decision_status` | 权威规则账本（HTML 原文 + 18 列结构化抽取） |
| `docs/prd-audit/html-prd-coverage.csv` | **1234 → 1234**（本轮不增删行） | `rule_id,entry_name,prd_path,prd_anchor_or_line,coverage_status,severity,finding_id,…,phase` | HTML→PRD 承接矩阵（四态实测 FULL **1078 → 1077** / PARTIAL **74 → 85** / STALE **69 → 69** / MISSING **13 → 3**；PARTIAL 差额对账见 `rounds/r11/worklog.md` B-10 计数冲销说明） |
| `docs/prd-audit/rounds/r8/findings.csv` | **233 → 236** | `finding_id,type,severity,file,line,html_evidence,disposition,status` | **当前活动**缺口台账（实测 `status=open` **75 → 64** 行） |

## 非 canonical：其余 16 张已跟踪 CSV

实测 `docs/prd-audit` 树下 `*.csv` 共 **20 → 23** 个，其中 git 已跟踪 **20 → 20** 个、
未跟踪 **0 → 3** 个（未跟踪的 3 个全是 R11 本轮产物：`rounds/r11/consolidated/findings-candidates.csv`、
`rounds/r11/implementation-gaps.csv`、`rounds/r11/invariant-registry.csv`）。
已跟踪的 20 个减去上表 4 张 canonical ＝ **16 张非 canonical**（本轮无增减）：

```
docs/prd-audit/appendix-crosscheck-findings.csv
docs/prd-audit/appendix-deep-action-queue.csv
docs/prd-audit/appendix-deep-findings.csv
docs/prd-audit/document-scope-manifest.csv
docs/prd-audit/document-scope-manifest.pre-b3.csv
docs/prd-audit/phase4-adversarial-findings.csv
docs/prd-audit/phase4-issue-rows.csv
docs/prd-audit/phase5-adversarial-findings.csv
docs/prd-audit/phase5-issue-rows.csv
docs/prd-audit/phase6-adversarial-findings.csv
docs/prd-audit/phase6-issue-rows.csv
docs/prd-audit/phase7-cross-domain-findings.csv
docs/prd-audit/rounds/r8/reanchor-manual-review.csv
docs/prd-audit/rounds/r8/reanchor-text-drift.csv
docs/prd-audit/rounds/r9/ledger-source-refresh.csv
docs/prd-audit/rounds/regext/non-rule-dispositions.csv
```

```
⚠ 禁止用 docs/prd-audit/*.csv 通配读取或改写台账 —— 通配会同时命中 16 张非 canonical 快照，
  其中 rounds/r9/ledger-source-refresh.csv 是 R9 时点的旧文快照（见 R11 D-02），
  误当基线回填会把已订正内容静默回滚。
```

## 迁移

```
MIGRATION_DEFERRED: R12+
目标路径: docs/prd-audit/findings.csv
迁移面: 见 docs/prd-audit/rounds/r11/worklog.md §「MIGRATION_DEFERRED 迁移面」
```
