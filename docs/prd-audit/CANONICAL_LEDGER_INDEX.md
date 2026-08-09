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

## ORPHANED / STALE 69 行 —— 是证据，不是待办

> 实测基线：`ab3019ee`（worktree `~/wt-r12d`）。本节为 docs-only 追加，**未改动任何 CSV**，
> 故上表计数不变（`authority-rule-ledger.csv` 1225 / `html-prd-coverage.csv` 1234）。

```
ORPHANED_ENTRIES_ARE_EVIDENCE_NOT_BACKLOG
账本 authority-rule-ledger.csv  entry_name=ORPHANED         69 行
矩阵 html-prd-coverage.csv      coverage_status=STALE       69 行
⚠ 二者 rule_id 集合恒等（交集 69，两侧差集皆空）——是同一批行的两个视图，不是两批
```

### 1. 定性：口径变更史的证据

R8 对这批行的处置意图见 `rounds/r8/ledger-rebuild-report.md:248`，逐字引用：

> **未删除任何行**：76 条 ORPHANED 全部原样保留（`anchor_or_line` 写 `ORPHANED(旧记 NNN)`），作为口径变更史的证据。

这批行的 `decision_status` 全部为 `SUPERSEDED`（实测 69/69）。**它们记录的是"曾经的口径"，
不是"尚未完成的承接"。不要把它们当待办去清零。**

### 2. 取代关系全库不存在 —— 不是漏回填

已裁定：**不再要求为这批行回填 `decision_id`。** 依据是下列实测，
即"取代关系"这条信息在本仓根本没有产生过，不存在可回填的来源：

- `authority-rule-ledger.csv` 全部 18 列里**没有任何一列**记录"被谁取代"——无 `superseded_by`、
  无 `replaced_by`、无 `decision_id`。`decision_status` 是纯枚举，只说被取代、不说被谁。
- 69 行中 55 行的 `html-prd-coverage.csv:decision_id` 为空。这 55 个 `rule_id` 在
  `phase4-issue-rows.csv` 里**零行**——不是"有行但没回填"，是根本没有登记入口。
- 全库检索这 55 个 `rule_id`，命中 11 个文件，其中**含 decision 语义列的为 0 个**
  （`appendix-crosscheck-findings.csv` / `appendix-deep-findings.csv` 均无 decision 列，
  55 个 id 只在 `sample_rule_ids` 这类采样列里被顺带提到）。
- `docs/prd-audit/` 下只有 `phase4-/phase5-/phase6-issue-rows.csv` 三份，
  **phase 03 根本没有 `issue-rows` 文件**，而 55 行里有 21 行属 `phase=03-governance-and-common`。

### 3. `reanchor-manual-review.csv` 不是取代关系表

`rounds/r8/reanchor-manual-review.csv` 含 `old_anchor` / `new_anchor` / `旧文` / `新文` 四列，
对这 69 行 100% 覆盖且 `new_anchor` 与「新文」全部非空。**它看上去像"旧文→新文"承接表，但不是。**

- R8 自己给 ORPHANED 的定义（`ledger-rebuild-report.md:35`）：
  「当前 HTML 已无该条原文，最长公共子串 **<12 字**，**不臆造行号**」。
- 实测这 69 行的 `lcs` 众数为 4~7 字（`6`×13、`4`×13、`5`×10、`7`×9），远低于成立门槛。
- `ledger-rebuild-report.md:151` 更点名 `BSM-0033` 的定位「落到『已拍板』问答块而非正文条目，**疑似误配**」。

```
⚠ 不得据 reanchor-manual-review.csv 的 new_anchor / 新文 推断承接关系。
  那两列记的是"该位置附近现在写着什么"，是工具输出且被 R8 自己判为不成立的候选，
  不是"这条被那条取代"的裁决。
```

### 4. 删这 69 行 = 永久丢失旧 HTML 原文

账本这 69 行的 `source_text` 是**旧 HTML 原文的唯一全文留存处**——按 ORPHANED 的定义，
整句在三份 HTML 现文里已不存在（个别措辞可能以改写形态散落在别处，但整条口径的原始表述只此一份）。
九个镜像列（`trigger` / `preconditions` / `state_transition` / `value_flow` / `permission` /
`approval` / `separation_of_duties` / `failure_recovery` / `evidence_required`）同样存着旧文切片。
**删除即不可恢复。**

### 5. `entry_name="ORPHANED"` 是门禁豁免钥匙

这个字面量被 `tools/verify-entry-anchors.mjs` 两处硬编码比对：

| 位置 | 覆盖的检查 | 对 ORPHANED |
|---|---|---|
| `:127` `if (r.entry_name === "ORPHANED") continue;` | ④ 条目名在注册表 + ⑤ `source_text` 对 HTML 现文 | 一句跳掉两检 |
| `:204` `if (r.entry_name === "ORPHANED") continue;` | ⑦ 九镜像列对 HTML 现文 | 跳过 |
| （无豁免） | ⑥ 账本↔矩阵一一对应 | **照检**：`rule_id` 双边必须都在，`entry_name` 必须逐字相等 |

由此得到可安全操作面：

```
改 coverage_status / decision_id / severity → 门禁零影响，不会红
  依据：verify-entry-anchors.mjs 对这三列零引用；且它是全仓唯一读 html-prd-coverage.csv 的程序
        （git grep -ln 'html-prd-coverage' 命中 47 文件，其余全是散文提及文件名）
        该门禁在 verify:predeploy 链上，CI 真跑

改 entry_name（任一侧）或删行 → 必红
  · 改矩阵侧 entry_name → ④ 块 `ledName[rule_id] !== entry_name` 报"承接矩阵与账本条目名不一致"
  · 改账本侧 entry_name → ④⑤⑦ 三检同时解除豁免，⑤ 逐行 includes(source_text) 对现文必然落空
  · 删除任一侧 → ⑥ 报"账本行未登记进承接矩阵" / "承接矩阵出现账本没有的 rule_id"
  ⚠ 要给这批行换留存形态，必须同步改门禁 :127 / :204 两处判据
```

### 6. 待查：R8 记 76 条，当前 69 条

`ledger-rebuild-report.md:35` 与 `:48` 均记 ORPHANED **76** 条，当前账本实测 **69** 条。
**差 7 条的处置去向未追**，留作待查。引用这批行的规模时不要沿用 76。

### 7. 两条 `kind=FUZZY` 的特例（BSM-0033 / BSM-0063）

69 行里 67 行 `kind=ORPHANED`，另 2 行 `kind=FUZZY`，是 R8 人工降级进来的
（`rounds/r8/changelog-rework.md:104` 小节标题：`## R-7 账本两条错锚改 ORPHANED`）。
两条现状实测如下，**均无需改动**：

- **`BSM-0033`**（P0）：`lcs=14` 确实超过 R8 的 12 字门槛，实算最长公共子串为
  `积分只兑虚拟券，实物券发券时`。但 R8 的「疑似误配」判断成立，且真实原因比"误配"更具体：
  **该条旧规则被跨文件拆成了两半，两半都已各自有 AUTHORITATIVE 账本行**——
  - 前半（兑券/预占/实扣/过期释放）→ `business-system-map-for-operations-v0.1.html:1815`，
    已由 `BSM-0497` 承接（`decision_status=AUTHORITATIVE`，`line_hint=L1815`）；
  - 后半（**按内部结算价与承接商户结算**，即钱路）→ 迁到了**另一份 HTML**
    `business-enum-quick-reference-v0.1.html:75`「发券与核销结算」，
    已由 `BEQ-0027` 与 `BEQ-0143` 承接（均 `AUTHORITATIVE`、`L75`）。

  R8 的重锚工具按单文件比对，跨文件拆分它看不见，这才是 `lcs` 只剩 14 字的原因。
  把 `BSM-0033` 重锚到任一半都会与已有 AUTHORITATIVE 行撞行重复。
  另实测旧文三处措辞「不减可售」「消费金额只是可配置条件之一」「会员等级成长和积分兑换关系」
  在三份 HTML 全域 0 命中——旧文并非被逐字搬走，是被改写后分置。
- **`BSM-0063`**：R8 当时提出的新文（`客人可在自己手机上的综合体官方小程序或App自助购买该酒吧单店门票`）
  在**当前** HTML 里也已 0 命中。旧文四个特征分片同样全部 0 命中。R8 之后的多轮 HTML 修订
  已使该候选一并失效，现为双向孤儿。

### 8. `html-prd-coverage.csv:decision_id` 的真实语义

**该列记的是"闭合该 `finding_id` 的裁决"，不是"取代该 `rule_id` 的裁决"。**
实测 64 个带 `decision_id` 的行，每个 `decision_id` 恰好对应**唯一一个** `finding_id`
（如 `BAR-VOID-A1`→`ADV-TX-010`、`POINTS-VOUCHER-ACCRUAL-002`→`ADV-TX-004`、
`DEC-SEAT-TRANSFER-001`→`ADV-TX-007`），即 `decision_id` 由 `finding_id` 函数决定。

因此这 69 行里 14 行带的 `decision_id` **不能读作"取代它的裁决"**——它是该行当年所属 finding
的闭合裁决号，在该行被改判 STALE 时作为历史字段留下。典型例：`BSM-0071` 挂
`POINTS-VOUCHER-ACCRUAL-002`，但其 `source_text` 讲的是"积分两本账（成长积分/可用积分）"账户结构，
而 `ACCRUAL-002` 裁的是"积分兑商户虚拟券 — 到店核销才记商户承接"，两者主题不同；
二者的联系是同属 finding `ADV-TX-004`（与 `BSM-0039`、`BSM-0069` 并列）。
