# R11 · Auditor shard-04 · REGX-11 清账纪律零承接

基线：`1c5c0c7d812349497240bbf57655b7d34d1e4442`（worktree `~/wt-r11`）
角色：Auditor。**本轮未修改任何 PRD / 台账 / HTML / 代码**，只产出发现与拟改文本。
disposition 图例（任务书未给，沿用 shard-03 已声明口径以保持本轮一致）：
`A` = 拟改可直接落（口径已由 HTML 定死，无待裁项）；`B` = 需用户裁决后方可落；
`C` = 判为无需改（已承接／有意省略）；`D` = 移交其它轮次或其它文档处理。

---

## 纠正任务书前提

### 前提① · 条目名不在 `entry-registry.csv` 里按 `BSM-0741` 查

任务书原文：**「从 BSM 取该条原文**逐字摘录**（条目名见 `entry-registry.csv` 中 BSM-0741 对应项；注意全角标点，零命中先定位实际文本）」**

实测不符：`entry-registry.csv` 的 `entry_id` 形如 `BSM-E0543`，**全表无 `BSM-0741`**。

- 检索式：`grep -n "BSM-0741" docs/prd-audit/entry-registry.csv` → 退出码 1，**零命中**
- `BSM-0741` 是 `authority-rule-ledger.csv`（L1108）与 `html-prd-coverage.csv`（L1117）的 `rule_id`

经条目名回查得到对应关系：

- `BSM-0741`（规则账本 `rule_id`）→ 条目名「影响结算的规则由各自责任主体在系统中配置、审批和版本化」
  → `entry-registry.csv:544` = `BSM-E0543` → BSM HTML **L1501**

不影响任务实质，按条目名定位后继续。
（旁证：同轮 shard-03 处理的 `BSM-0742` 亦指向同一条目 `BSM-E0543` / 同一段 L1501 ——
这一整段 HTML 被账本拆成多条 `rule_id`，`BSM-0741` 与 `BSM-0742` 是该段中相邻的两句。）

### 前提② · r8 记录的「命中行数=0」实跑为 1，结论仍成立但证据数字不准

`html-prd-coverage.csv:1117` 的 `conflict_summary` 自述：

```
两种姿势零命中：grep -rniE '当月内完全清理|平台无法控制' docs --exclude-dir=_archive --exclude-dir=prd-audit → 命中行数=0
```

逐字复跑该命令（`@1c5c0c7d`，`~/wt-r11`）：

```
$ grep -rniE "当月内完全清理|平台无法控制" docs --exclude-dir=_archive --exclude-dir=prd-audit | wc -l
1
$ grep -rniE "结算完成时限|完成时限|结算时限" docs --exclude-dir=_archive --exclude-dir=prd-audit | wc -l
0
```

第一式**实跑=1，不是 0**：命中的正是 `docs/business-system-map-for-operations-v0.1.html:1501`
（HTML 本体也在 `docs/` 下、未被两个 `--exclude-dir` 排除）。
r8 的**结论（无 PRD 承接）经本 shard 独立复核成立**，但其记录的检索式与命中数不自洽，
不能直接当「全库唯一」的证据引用。本 shard 另出完整检索面，见 EV-04-04。

### 前提③ · 落点判断（任务书已声明为建议，本 shard 判为**合适**）

任务书原文：**「建议落点（**建议，非已裁**）：`docs/finance-settlement-product-prd-v0.1.md §4B.4`」**
本 shard 判定：**建议落点成立，且插入位置可由 HTML 原句序唯一确定**。理由见 A-1「落点判断」。
任务书提示的先例（`§4A 只讲营业日与退款税会归属，硬插会造错位`）**在本条不适用**，理由同处说明。

---

## A-1 · REGX-11 · 清账纪律（当月内清完／第三方相关顺延下月）全库零承接

EV-04-01 | HTML_SOURCE | BSM「影响结算的规则由各自责任主体在系统中配置、审批和版本化」条（`docs/business-system-map-for-operations-v0.1.html` L1501；注册表 `BSM-E0543`；账本 `BSM-0741`） | @1c5c0c7d

关键节选（逐字，含全角标点；为示句序，摘该 `<p>` 内相邻三句，**目标句居中**）：

```
凡属电子数据、且计算比例固定的，一律在月结定格时即出结果，不等待任何人工动作；只有存在不确定性的（如积分、退款）与商户已提出异议的，才留出较长处理时间。大部分账目应在当月内完全清理清楚，只有平台无法控制的（第三方相关）才顺延至下个月。积分与商户的结算目标为账期结束后一个月以内完成，具体天数由后台配置。
```

**目标句（逐字，`BSM-0741` 的 `source_text`）**：

```
大部分账目应在当月内完全清理清楚，只有平台无法控制的（第三方相关）才顺延至下个月。
```

标点实测：逗号为全角「，」、括号为全角「（）」、句号为全角「。」；
半角变体 `(第三方相关)` / `,只有` **零命中**（`grep -n "(第三方相关)" docs/business-system-map-for-operations-v0.1.html` → 退出码 1）。

---

EV-04-02 | PRD_CURRENT | `docs/finance-settlement-product-prd-v0.1.md` L327–L330（§4B.4 出结果的时点纪律与账期沿用） | @1c5c0c7d

关键节选（逐字，整节现文——该节现仅两条 bullet）：

```
### 4B.4 出结果的时点纪律与账期沿用

- **凡属电子数据、且计算比例固定的，一律在月结定格时即出结果，不等待任何人工动作**；只有存在不确定性的（如积分、退款）与商户已提出异议的，才留出较长处理时间。
- **通票分账、单店门票结付、游乐域内结算、餐饮与零售的券结算，一律沿用与积分结算相同的账期**：**自然月计费、次月出账**；**付款落在三个付账日上**。
```

补充：该节所属 §4B 的标题（L297）已把本条 HTML 段落列为依据：

```
## 4B. 三个付账日与内部收付纪律（2026-08-04 新增，依据 BSM「平台内部各主体之间的应收应付，每月有三个付账日」条、BSM「影响结算的规则由各自责任主体在系统中配置、审批和版本化」条「付款时点一律后台配置 + 月结定格→账单出账→异议期届满→付账日顺序约束」、ENUM「积分结算账期」条（不得轧差））
```

---

EV-04-03 | LEDGER_ROW | `docs/prd-audit/authority-rule-ledger.csv` L1108（`BSM-0741`） | @1c5c0c7d

用 Python `csv` 模块解析（非 grep 目测），18 列全列取值：

```
rule_id              = BSM-0741
html_file            = business-system-map-for-operations-v0.1.html
entry_name           = 影响结算的规则由各自责任主体在系统中配置、审批和版本化
line_hint            = L1501
source_text          = 大部分账目应在当月内完全清理清楚，只有平台无法控制的（第三方相关）才顺延至下个月。
domain               = finance
subject              = 平台|第三方
object               = 账目清理|当月内|顺延下月
trigger              = 判断某笔账目能否在当月内清理完毕时
preconditions        = NOT_STATED
state_transition     = NOT_STATED
value_flow           = 大部分账目应在当月内完全清理清楚，只有平台无法控制的（第三方相关）才顺延至下个月。
permission           = NOT_STATED
approval             = NOT_STATED
separation_of_duties = NOT_STATED
failure_recovery     = NOT_STATED
evidence_required    = NOT_STATED
decision_status      = AUTHORITATIVE
```

**回答任务书第 5 问**：

- 该行在 `authority-rule-ledger.csv` 中**存在**（L1108，18 列完整，`decision_status = AUTHORITATIVE`）。
- `value_flow` 取值 = **`source_text` 的逐字副本**，不是资金流描述。
- **判定：本条实质不涉钱。** 它规定的是「账目何时清完」的**时限纪律**，不改变任何一笔金额、
  不改变收付方向、不改变分账比例；顺延的只有**时点**。非空的 `value_flow` 是**填表副作用造成的
  假「涉钱」信号**，不应据此把本条当资金规则处理。
- 该填法**非本行独有**：全表 1225 条数据行中，`value_flow != NOT_STATED` 者 441 条，
  其中 **176 条（39.9%）的 `value_flow` 与 `source_text` 逐字相同**
  （样例 `BSM-0006 / BSM-0009 / BSM-0016 / BSM-0025 / BSM-0060 / BSM-0069 / BSM-0076 / BSM-0079`）。
  属**账本级系统性填表问题**，超出本 shard 授权范围，另列 A-2 报备、不在本轮处置。

---

EV-04-04 | SEARCH_EVIDENCE | 全库检索（`~/wt-r11` @1c5c0c7d） | @1c5c0c7d

**检索面 1 —— 工作树全量**（`.` 递归，仅排除 `.git`；**含 backend 代码、apps/web 前端、SQL 迁移、测试、`docs/_archive`、`docs/prd-audit`**）：

```
$ grep -rlniE "当月内完全清理|完全清理清楚|顺延至下个月|平台无法控制" . --exclude-dir=.git | sort
./docs/business-system-map-for-operations-v0.1.html      ← HTML 本体（唯一口径来源）
./docs/prd-audit/authority-rule-ledger.csv               ← 审计记账
./docs/prd-audit/html-prd-coverage.csv                   ← 审计记账
./docs/prd-audit/rounds/final/worklog.md                 ← 审计记账
./docs/prd-audit/rounds/r7/html-map-bsm-2.md             ← 审计记账
./docs/prd-audit/rounds/r8/findings.csv                  ← 审计记账
./docs/prd-audit/rounds/regext/draft-05.jsonl            ← 审计记账
./docs/prd-audit/rounds/regext/shard-05.jsonl            ← 审计记账
./docs/prd-audit/rounds/regext/worklog.md                ← 审计记账
```

命中文件 **9 个 = 1 份 HTML 本体 + 8 份审计记账**。
**零 PRD、零代码、零 SQL、零测试、零前端。**

**检索面 2 —— 全角／半角与近义式双跑**（同检索面 1）：

| 检索式 | 命中行数 |
|---|---|
| `grep -rn "当月内完全清理" . --exclude-dir=.git` | 9 |
| `grep -rn "完全清理清楚" . --exclude-dir=.git` | 8 |
| `grep -rn "平台无法控制" . --exclude-dir=.git` | 9 |
| `grep -rn "顺延至下个月" . --exclude-dir=.git` | 8 |
| `grep -rn "顺延到下个月" . --exclude-dir=.git` | **0** |
| `grep -rn "顺延下月" . --exclude-dir=.git` | 4 |
| `grep -rn "当月内清理" . --exclude-dir=.git` | 2 |
| `grep -rn "当月清理" . --exclude-dir=.git` | **0** |
| `grep -n "(第三方相关)" docs/business-system-map-for-operations-v0.1.html`（半角括号） | **0**（退出码 1） |

以上非零命中均落在检索面 1 列出的那 9 个文件内，无新增文件。

**检索面 3 —— 语义近义扫**（`.` 递归，排除 `.git` 与 `docs/prd-audit`，即**排除审计自身记账后仍看有无实体承接**）：

```
$ for p in 当月结清 月内结清 本月内清 跨月挂账 不得跨月 挂账跨月 清账 结清时限 清理时限 当月了结 月度清理; do
    echo "[$p]=$(grep -rn "$p" . --exclude-dir=.git --exclude-dir=prd-audit | wc -l)"; done
[当月结清]=0  [月内结清]=0  [本月内清]=0  [跨月挂账]=0  [不得跨月]=0  [挂账跨月]=0
[清账]=4      [结清时限]=0  [清理时限]=0  [当月了结]=0  [月度清理]=0
```

`清账` 的 4 处**均非本规则**，逐条核过：

- `docs/business-system-map-for-operations-v0.1.html:1218` —— BSM「签约与合同」条**商户欠费处置**
- `docs/real-estate-full-chain-domain-prd-v0.1.md:32` —— **押金治理**
- `docs/prd-audit-fix-change-log-20260804.md:2025` —— GAP-35 **台账 rule_text 陈旧**
- `docs/superpowers/reviews/2026-06-14-p1-bar-bill-adjustments-review.md:42` —— 「已**结清**账单拒绝调整」（分词误命中）

**检索面 4 —— 英文标识符侧**（同检索面 1）：

```
clearWithinMonth=0  settleWithinMonth=0  carryOverToNextMonth=0  clearingDeadline=0  settlementDeadline=0
```

**检索面 5 —— 决策台账**：

```
$ grep -n "当月内完全清理\|清理清楚\|顺延至下个月" docs/prd-audit/business-decision-ledger.md
（退出码 1，零命中）
```

无任何 DEC 就本条做过裁决或有意豁免的记录 —— 即**不是「有意留白」，是漏承接**。

**结论**：`REGX-11` 全库零承接**成立**，且系「无归口 PRD」而非「归错口」。

---

问题：BSM L1501 段中的清账时限纪律「大部分账目应在当月内完全清理清楚，只有平台无法控制的（第三方相关）才顺延至下个月」在**全库 1 份 PRD 都未承接**（检索面见 EV-04-04），HTML 定了「当月清完」这条总时限、下游无一处可执行口径。

落点判断：**任务书建议的 `finance-settlement-product-prd-v0.1.md §4B.4` 合适，且插入位置可唯一确定，建议采纳。** 四条理由：

1. **节题即是本条的属性**。§4B.4 标题为「**出结果的时点纪律**与账期沿用」；本条恰是一条「时点纪律」（规定账目清完的时限），语义与节题逐字对应，不需要改节题即可容纳。
2. **同源同段，不产生跨文档交叉引用**。§4B 标题（L297）已把 BSM「影响结算的规则由各自责任主体在系统中配置、审批和版本化」条列为依据来源，本条正出自该条同一 `<p>`；落进来是**把已引依据补全**，不是**新引一个来源**，因此不产生新的交叉引用，也不需要动 §4B 标题的依据串。
3. **插入位置由 HTML 句序唯一确定，不会错位**。§4B.4 现有两条 bullet 在 HTML 原段中分别是**第 2 句**（凡属电子数据…）与**第 8 句**（通票分账…账期沿用）；目标句是**第 3 句**，紧接第 2 句之后。故新 bullet 应插在**现 L329 与 L330 之间**（插入后为新 L330），HTML 原句序即得完整保留。
4. **任务书提示的「§4A 先例」在本条不适用**。该先例之所以造错位，是因为 §4A 的节题限定为「营业日与退款税会归属」——是**两个具体主题**的枚举，硬插第三主题即破坏节题外延；而 §4B.4 节题「出结果的**时点纪律**」是**类目名**，本条落在类目之内，属同类并列而非扩题。**性质相反，不构成重蹈。**

替代落点（**不推荐，仅记录已比对过**）：

- `§4B.1 三个付账日` —— 讲的是**单笔应付落哪一天**及三种顺延（节假日／异议期／资金不足），
  粒度是「笔」；本条粒度是「一个月的全部账目」，且其「顺延下月」的触发因是**第三方不可控**、
  与 §4B.1 三种顺延**不同因**，混入会被误读为**第四种顺延**并污染 §4B.5 已定的
  「三种顺延（节假日顺延、异议期未届满顺延、资金不足顺延）」验收口径。**明确不选。**
- `§4A.3 结算期` —— 只讲结算周期可配与守恒，属营业日／税会双轨语境，即上述先例所在处。**明确不选。**

拟改：`docs/finance-settlement-product-prd-v0.1.md` | §4B.4 出结果的时点纪律与账期沿用 | **在现 L329 之后、现 L330 之前插入 1 行 bullet**（插入后该节共 3 条 bullet，句序与 HTML 原段一致）

插入后该节完整形态（**新增行以 `+` 标示，`+` 不写入文件**）：

```
### 4B.4 出结果的时点纪律与账期沿用

- **凡属电子数据、且计算比例固定的，一律在月结定格时即出结果，不等待任何人工动作**；只有存在不确定性的（如积分、退款）与商户已提出异议的，才留出较长处理时间。
+ - **大部分账目应在当月内完全清理清楚**，只有**平台无法控制的（第三方相关）**才顺延至下个月。
- **通票分账、单店门票结付、游乐域内结算、餐饮与零售的券结算，一律沿用与积分结算相同的账期**：**自然月计费、次月出账**；**付款落在三个付账日上**。
```

**待插入行（逐字，可直接取用）**：

```
- **大部分账目应在当月内完全清理清楚**，只有**平台无法控制的（第三方相关）**才顺延至下个月。
```

与 HTML 的逐字关系：去掉 `**` 加粗标记后，该行正文
`大部分账目应在当月内完全清理清楚，只有平台无法控制的（第三方相关）才顺延至下个月。`
与 EV-04-01 目标句**逐字一致**（含全角「，」「（）」「。」），**无一字自创、无一字增删**。

**不另补 §4B.5 验收 bullet（有意为之，非遗漏）**：§4B.5 现有 6 条验收，逐条核过后确认
**没有一条**是为 §4B.4 现有两条 bullet 写的（6 条分别对应 §4B.1 指定付账日、§4B.1 提前付、
§4B.1 三种顺延、§4B.1 异议期、§4B.3 顺序校验、§4B.2 不得轧差）。即 §4B.4 整节在本 PRD 中
**本就无验收覆盖**。本条只做「补齐 §4B.4 缺句」，若额外只给新句配验收，反而使新句的落地要求
**高于同节既有两句**，属超出 finding 范围的口径拔高。**如编排者要补验收，须连同 §4B.4 另两句
一并补，且应另开 finding；本 shard 不代裁。**

**不加日期／DEC 标注（有意为之）**：§4B.4 现有两条 bullet 均无标注（系原始转写）；
§4B.1 内带「（2026-08-06 按 BSM 同步，DEC-R8-01 / DEC-R8-06）」的 bullet 是**改写既有口径**才标的。
本条属**原始转写补漏**、非改写，依同节体例不标。若本轮统一要求补漏行留痕，
可改用变体 `- **大部分账目应在当月内完全清理清楚**，只有**平台无法控制的（第三方相关）**才顺延至下个月（2026-08-07 按 BSM 补录，REGX-11）。` —— **二选一由编排者定，本 shard 默认取不标注版。**

句式依据：抄同文件 **§4B.4 第 1 条 bullet**（`docs/finance-settlement-product-prd-v0.1.md:329`），原文：

```
- **凡属电子数据、且计算比例固定的，一律在月结定格时即出结果，不等待任何人工动作**；只有存在不确定性的（如积分、退款）与商户已提出异议的，才留出较长处理时间。
```

抄取的三个特征：①「`- ` + 主纪律整句加粗」起头；②主纪律之后接**例外／让步小句**，
例外小句**不整句加粗、只加粗其中的限定词**；③保留 HTML 原句的全角标点与原词序，**不改写不概括**。
拟改行同构：主纪律「大部分账目应在当月内完全清理清楚」加粗，例外小句只加粗限定词
「平台无法控制的（第三方相关）」。

检索证据：见 EV-04-04（检索面 1 全工作树 / 检索面 2 全角半角双跑 9 式 / 检索面 3 语义近义 11 式 /
检索面 4 英文标识符 5 式 / 检索面 5 决策台账）。
**「全库零承接」断言的检索面 = `~/wt-r11` 工作树全量（仅排除 `.git`），含 backend Java、
SQL 迁移、apps/web 前端、测试、`docs/_archive`、`docs/prd-audit`。**
命中 9 文件 = HTML 本体 1 + 审计记账 8；PRD／代码／SQL／测试／前端**各 0**。

依据：EV-04-01（HTML 原文）、EV-04-02（PRD §4B.4 现文）、EV-04-03（账本行）、EV-04-04（检索证据）

severity: P1   disposition: A

> severity 定 P1 而非 finding 原记的 `advisory`（P2）的理由：本条是 HTML 声明的**总时限纪律**，
> 是「哪些账可以拖到下月」的**唯一判据**。缺它，§4B.1 的三种顺延（节假日／异议期／资金不足）
> 各自只管单笔，**没有任何一条约束「整月账目何时必须清空」**，第三方以外的账目实际可无限期滚存
> 而不违反现有任一条 PRD 口径。属口径缺口而非措辞问题。**不涉资金金额改动（见 EV-04-03），
> 故不提 P0。** 最终 severity 由编排者定。
>
> disposition 定 A（可直接落）的理由：拟改文本 100% 取自 HTML 本体、零自创，
> 落点与插入位置均由 HTML 句序与节题唯一确定，无待裁项。
> **唯一需编排者点头的是「加不加日期标注」这一个二选一**，不影响正文口径。

---

## A-2 · 报备（不在本轮处置）· 账本 `value_flow` 列 176 行为 `source_text` 逐字副本

EV-04-05 | LEDGER_STAT | `docs/prd-audit/authority-rule-ledger.csv` 全表 | @1c5c0c7d

Python `csv` 模块解析结果（脚本 `/tmp/r11s4b.py`，非 grep 目测）：

```
总数据行 1225 | value_flow 列序 11
value_flow 非 NOT_STATED 行数: 441
其中 value_flow == source_text 逐字相同: 176   （占非空的 39.9%）
样例: BSM-0006, BSM-0009, BSM-0016, BSM-0025, BSM-0060, BSM-0069, BSM-0076, BSM-0079
```

---

问题：`value_flow` 本应描述**资金如何流动**（谁付谁、金额基数、方向），实际有 176 行被填成
`source_text` 的整句副本。后果是**「`value_flow` 非空 ⇒ 该规则涉钱」这一筛选条件失真**：
`BSM-0741` 就是实例 —— 它只规定时限、不动任何金额（见 EV-04-03 判定），却因 `value_flow` 非空
会被任何按该列筛「钱路规则」的脚本或人工复核**误判为涉钱**。

落点判断：**不适用**（非 PRD 缺口，是台账数据质量问题）。

拟改：**本 shard 不提拟改。** 台账属本 shard 明令禁改范围（任务书：「禁止碰：任何 PRD 本体、
**四张台账**、三份 HTML、代码」），且这是 1225 行级的系统性填表问题，
不能借单条 finding 的口子夹带。建议编排者另开一条独立 finding／独立轮次处理，
并在处理前先定「`value_flow` 该填什么」的填表口径 —— 否则重填 176 行仍会各填各的。

句式依据：**不适用**（无拟改文本）。

检索证据：EV-04-05（全表 `csv` 模块解析，非抽样）。

依据：EV-04-03、EV-04-05

severity: P2   disposition: D

---

## 收工自检

- **本 shard 未修改任何 PRD / 台账 / HTML / 代码。**
- 唯一写入：`docs/prd-audit/rounds/r11/audit-shards/shard-04.md`（append-only）。
- 未执行 `git add` / `git commit` / `git push`。
- 临时脚本 `/tmp/r11s4.py`、`/tmp/r11s4b.py` 落在 `/tmp`，未进仓。
- 任务书三项前提中 **①、②与实跑不符已逐字引用并更正**；③（落点建议）经独立判断后**采纳**，理由见 A-1。
