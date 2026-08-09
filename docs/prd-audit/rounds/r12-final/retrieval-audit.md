# R12 检索陈述抽查

基线 `566df807` | 含检索陈述 32 条（脚本命中）| 其中真检索断言 30 条 | 一致 17 / 不一致 4 / 无法复核 9 | 正则误命中（非检索断言）2

4 条不实断言全部已处置：2 条就地订正（`BEQ-0155`、`PGM-0220`），2 条经逐句核验后改判（`BSM-0741`、`BSM-0742`：`MISSING` → `FULL`）。

## 0. 检查面圈定

取数命令（`~/wt-final`，基线 `566df807`）：

```
python3 -c "
import csv,re
rows=list(csv.DictReader(open('docs/prd-audit/html-prd-coverage.csv',encoding='utf-8-sig')))
pat=re.compile(r'命中|零命中|检索|grep|TOTAL_HITS|全 ?docs|全库')
hit=[r for r in rows if pat.search(r['conflict_summary'])]
print('含检索陈述的条目', len(hit))
"
```

输出：`总行数 1234` / `含检索陈述的条目 32`。

按 `e6ab52d9`（R11 收官）与 `566df807` 两版 CSV 的 `conflict_summary` 逐行比对（`git show <ref>:docs/prd-audit/html-prd-coverage.csv`）：

| 分类 | 条数 | rule_id |
|---|---|---|
| R12 本轮新写／改写 | 21 | BEQ-0123 0124 0128 0129 0131 0132 0139 0141 0147 0152 0155 0157 0159 0161 0163 0165 0167、BSM-0696 0741、PGM-0214 0220 |
| R12 前既有·本轮未改 | 11 | PGM-0194 0199、BSM-0555 0564 0565 0566 0647 0704 0708 0734 0742 |

同一比对给出：R12 改写的非空 `conflict_summary` 共 **77 条**，含检索断言者 21 条（27.3%）。

## 1. 逐条核对表

命中数一律以 `git grep` 在基线 `566df807` 上实跑为准；`EXIT=1` 表示零命中。

### 1.1 R12 本轮新写（21 条）

| rule_id | 声称 | 实测（取数命令见 §4） | 判定 | 处置 |
|---|---|---|---|---|
| BEQ-0123 | 「一体机」「小程序」「购票渠道」在结算 PRD 全文 0 命中 | 三词在 `docs/settlement-rebuild-prd-v1.0.md` 均 EXIT=1（0 命中） | 一致 | 不动 |
| BEQ-0124 | 分句 5「学生票不等于成年…」`docs/*.md` 全仓 0 命中 | `git grep -c 学生票不等于成年 -- :(glob)docs/*.md` EXIT=1；全 docs 仅命中 ENUM HTML:31 与审计产物 | 一致 | 不动 |
| BEQ-0128 | 分句 10 后半与分句 14 在原 finance PRD 零命中；两句在 SRB :195 与 :122／:129／:192／:200 有正文 | finance PRD 三个特征串（售票时刻／事后不得变更／实际提供的餐饮）均 EXIT=1；SRB 实测 `售票时刻`=:195、`实际提供的餐饮`=:122 :129 :191 :200 :831 :843（:191 餐饮域、:192 零售域，均落在所指区段） | 一致 | 不动 |
| BEQ-0129 | SRB 全文「上下限」三处且均属高价值判定阈值；「项目管理费」「管理费」0 命中 | `上下限` 实测 3 行（:712 :715 :726，三行均为高价值判定阈值段）；`项目管理费` `管理费` 均 EXIT=1 | 一致 | 不动 |
| BEQ-0131 | MPS 全文零命中：零售商户／联营／租赁／商户档案／不得为其开通／获批内部结算价 | 六词在 `docs/merchant-points-settlement-rules-v1.0.md` 全部 EXIT=1 | 一致 | 不动 |
| BEQ-0132 | —— | 正则命中的是引文「全部命中资格」，非检索断言 | 非检索断言 | 不动 |
| BEQ-0139 | 「顶层平台公司」在 points-and-voucher 与 MPS 两份 PRD 零命中 | 两文件合并检索 EXIT=1 | 一致 | 不动 |
| BEQ-0141 | 「最小销售单元」全部 PRD 零命中；terminology-canonical 检索「商品规格」零命中；MPS 全文 SKU 零命中 | `最小销售单元` 全 docs 仅命中 ENUM HTML:73、决策台账、entry-registry 与审计产物，无任一 PRD 正文；`商品规格` 在 `docs/terminology-canonical-v1.0.md` EXIT=1；`SKU` 在 MPS EXIT=1 | 一致 | 不动 |
| BEQ-0147 | MPS「顶层平台公司」0 命中；「同比例退回」0 命中 | 两词均 EXIT=1 | 一致 | 不动 |
| BEQ-0152 | —— | 正则命中的是行文「检索只能用手机号」，非检索断言 | 非检索断言 | 不动 |
| BEQ-0155 | 两份 PRD grep「自发放／发放之日」零命中 | **实测 4 命中**：amusement :394 :454 :461、ticketing :627 | **不一致** | **已订正** |
| BEQ-0157 | 「跨年」在 finance PRD 与 settlement PRD 全文 0 命中 | 两文件合并检索 EXIT=1 | 一致 | 不动 |
| BEQ-0159 | 「归娱乐平台／归游乐公司／归传媒公司／归城市项目公司」全文零命中 | 四词在 `docs/platform-governance-prd-v0.1.md` 全部 EXIT=1 | 一致 | 不动 |
| BEQ-0161 | 全文「已忽略」与 `ignored` 命中 0 | 两词在 `docs/evidence-risk-control-product-prd-v0.1.md` 均 EXIT=1 | 一致 | 不动 |
| BEQ-0163 | 「资产管理公司」「同一主体」「两个叫法」「资产业态」「兼任」全文零命中 | 五词在 `docs/platform-governance-prd-v0.1.md`（本行 prd_path）全部 EXIT=1。注：本条「全文」指该 PRD 而非全 docs，与 PGM-0220 的「全 docs」口径不同，故不构成同类错误 | 一致 | 不动 |
| BEQ-0165 | 「代收代付」「扣留」「当班」「服务提供人」在结算 PRD 全文 0 命中 | 四词在 SRB 全部 EXIT=1 | 一致 | 不动 |
| BEQ-0167 | 「中国税法」「交易实质」「发放时有效」仅出现在 :49／:322；「凭证」零命中 | 三词各 2 行且行号恰为 49 与 322；`凭证` EXIT=1 | 一致 | 不动 |
| BSM-0696 | 原判 PARTIAL 系检索式「承担＋至多八字＋核销成本｜酒吧域权益由娱乐平台公司」零命中所致 | 该正则在 docs/ 的命中全部落在 BSM HTML:984（权威源本身）、台账 CSV 与审计轮产物，无任一 PRD `.md` 正文命中；且所引承接位 BAR:156 :157、PGV:208、SRB:392 :402 :1133 实读均在文 | 一致 | 不动 |
| BSM-0741 | `grep -rniE '当月内完全清理\|平台无法控制'` → 命中行数=0；`'结算完成时限\|完成时限\|结算时限'` → 命中行数=0 | **各 2 命中**。第一式命中 BSM HTML:1501 与 **`docs/finance-settlement-product-prd-v0.1.md:330`**（逐字承接）；第二式命中 `points-and-voucher-product-prd-v1.0.md:379` 与 `merchant-points-settlement-rules-v1.0.md:332` | **不一致·推翻原判定** | **逐句核验后改判 MISSING → FULL** |
| PGM-0214 | 「先做好自有」全 docs 仅 1 命中（`original-requirements-capture-v0.1.md` L30） | 全 docs 实测 6 行；其中 `.md` 非审计正文命中确为 `original-requirements-capture-v0.1.md:30` 唯一 1 行，其余为 PGM HTML:388（权威源）、两张台账 CSV 与审计产物。所引承接位 OPS:1118 :25 :28 实读在文 | 一致 | 不动 |
| PGM-0220 | 「本轮实跑 git grep -n 资产管理公司 -- docs/ 实得 18 行……14 行在 business-decision-ledger.md、而 prd-sync-change-log-20260804.md 零命中」 | **`ac6f3035^` 实得 43 行、基线 `566df807` 实得 49 行**；business-decision-ledger.md **16 行**；`docs/prd-sync-change-log-20260804.md` **3 行**（:1321-1323）。HTML 现行正文 2 行与 authority-rule-ledger.csv 2 行两项属实 | **不一致（且本身即一次订正）** | **二次订正** |

### 1.2 R12 前既有·本轮未改（11 条）

| rule_id | 声称 | 实测 | 判定 | 处置 |
|---|---|---|---|---|
| PGM-0199 | 「见名可点」0 行；「名称、编号、金额、状态、人员」0 行；「来源单据」23 行且全为数据库字段／接口清单／变更日志 | 前两式在 `docs/*.md`（剔除 `docs/prd-audit/rounds/`）均 EXIT=1；`来源单据` 同范围实测 **恰 23 行**；其中唯一 PRD 命中 `finance-settlement-product-prd-v0.1.md:411` 为幂等键举例，非「见名可点」承接，定性成立 | 一致 | 不动 |
| BSM-0742 | `'账期结束后一个月\|一个月以内完成'` → 0；`'结算目标\|账期结束后.{0,6}月'` → 0 | **各 3 命中**：BSM HTML:1501、**`points-and-voucher-product-prd-v1.0.md:379`**、**`merchant-points-settlement-rules-v1.0.md:332`**，后两者逐字承接 | **不一致·推翻原判定** | **逐句核验后改判 MISSING → FULL** |
| PGM-0194 / BSM-0555 / BSM-0564 / BSM-0565 / BSM-0566 / BSM-0647 / BSM-0704 / BSM-0708 / BSM-0734 | 各自的历史检索陈述 | 见 §3 | 无法复核 | 不动 |

## 2. 不实陈述清单（含订正前后）

### 2.1 就地订正（2 条，`coverage_status` 未动）

**BEQ-0155**（`coverage_status=PARTIAL`，未变）

- 订正前：`两份 PRD grep「自发放／发放之日」零命中`
- 订正后：`两份 PRD grep「自发放／发放之日」实测 4 命中（R12-final 复核订正：原写「零命中」不实。基线 566df807 实跑 git grep -n -e 自发放 -e 发放之日 得 4 行——amusement :394／:454／:461 与 ticketing :627；该 4 行全部是「旧版硬编码已删除」的括注或变更日志行、PRD 正文确无起算锚点、故本条缺口与 PARTIAL 判定不变）`
- 说明：该陈述自相矛盾——同一句先逐字引用了 `amusement:394` 的括注「旧版『自发放之日起一年内有效』的硬编码已删除」，随后又断言同一串 grep 零命中。实质结论（正文无起算锚点）不受影响，判定不变。

**PGM-0220**（`coverage_status=FULL`，未变）—— **第二次订正**

- 第一版（R12-batch3）：`命中 3 行且 3 行全在 docs/prd-sync-change-log-20260804.md §CL-49 变更日志、无一在现行正文`
- 第二版（R12-batch4 订正）：`实得 18 行……14 行在 business-decision-ledger.md、而 docs/prd-sync-change-log-20260804.md 零命中`
- 第三版（R12-final 二次订正，本次写入，全文）：

> 检索陈述二次订正（R12-final）：本条 conflict_summary 的检索陈述已两次不实。原陈述（R12-batch3）写「命中 3 行、全在 docs/prd-sync-change-log-20260804.md §CL-49、无一在现行正文」；R12-batch4 订正为「18 行、14 行在 business-decision-ledger.md、该变更日志零命中」——该订正同样不实。R12-final 实测（基线 566df807）：git grep -n 资产管理公司 566df807 -- docs/ | wc -l ＝ 49 行（同式在 ac6f3035^ 即 batch4 补写前为 43 行）；business-decision-ledger.md 16 行；docs/prd-sync-change-log-20260804.md 3 行（:1321-1323·非零命中）；三份 HTML 现行正文 2 行（business-enum-quick-reference-v0.1.html:179 与 platform-governance-map-for-operations-v0.1.html:419）；authority-rule-ledger.csv 2 行；docs/terminology-canonical-v1.0.md 3 行（:39／:55／:87·本批补写）；html-prd-coverage.csv 2 行；其余 21 行在审计轮产物。原陈述中属实的只有「HTML 现行正文 2 行」与「authority-rule-ledger.csv 2 行」两项。缺口真正所在是 docs/terminology-canonical-v1.0.md 该词在本批补写前零命中（现 :39／:55／:87 三处在文）·而非全库零命中。FULL 判定依据（terminology-canonical-v1.0.md :39／:55／:87 已登记别名）实测在文·判定不变。

- 说明：同一条上数字连错三次（原始 3 行、第一次订正 18/14/0、实测 49（43）/16/3）。**订正动作本身不自带正确性豁免**，见 §6。

### 2.2 逐句核验后改判（2 条，`MISSING` → `FULL`）

两条的共同事实：承接段均由 **`e6ab52d9`（R11 收官 PR#352）** 写入（`git log --oneline -S<原句> -- <PRD>` 各返回 `e6ab52d9`），早于 R12 全部批次，即 R12 写下「零命中」时落点早已在文。

**BSM-0741** · entry「影响结算的规则由各自责任主体在系统中配置、审批和版本化」

- 台账 `source_text`（2 分句）：`大部分账目应在当月内完全清理清楚，只有平台无法控制的（第三方相关）才顺延至下个月。`
- 逐句比对（落点 `docs/finance-settlement-product-prd-v0.1.md` §4B.4 出结果的时点纪律与账期沿用 · `:330`）：

| 分句 | HTML 原文 | PRD 落点 | 结论 |
|---|---|---|---|
| 1 | 大部分账目应在当月内完全清理清楚 | `:330`「**大部分账目应在当月内完全清理清楚**」 | 逐字 |
| 2 | 只有平台无法控制的（第三方相关）才顺延至下个月 | `:330` 同行「只有**平台无法控制的（第三方相关）**才顺延至下个月。」 | 逐字 |

- 无数值项、方向一致、无缺句 → **FULL**（非 PARTIAL）。
- 改动字段：`prd_path` `无归口 PRD` → `docs/finance-settlement-product-prd-v0.1.md`；`prd_anchor_or_line` `（无）` → `§4B.4 出结果的时点纪律与账期沿用（docs/finance-settlement-product-prd-v0.1.md:330）`；`coverage_status` `MISSING` → `FULL`；`conflict_summary` 重写。
- 原判「本条无归口 PRD，不硬造归口（与 PGM-0196 / R10-U-03 同口径）」随之作废。**注意该口径本身没错，错的是「零命中」这个前提**——PGM-0196 / R10-U-03 不受牵连。

**BSM-0742** · 同一 entry

- 台账 `source_text`（2 分句）：`积分与商户的结算目标为账期结束后一个月以内完成，具体天数由后台配置。`
- 逐句比对（落点 `points-and-voucher-product-prd-v1.0.md` §11A.1 账期参数 · `:379`，与 `merchant-points-settlement-rules-v1.0.md` §16.1 账期 · `:332`，两处同文）：

| 分句 | HTML 原文 | PRD 落点 | 结论 |
|---|---|---|---|
| 1 | 积分与商户的结算目标为账期结束后一个月以内完成 | 两处「**积分与商户的结算目标为账期结束后一个月以内完成**」 | 逐字 |
| 2 | 具体天数由后台配置 | 两处「**具体天数为业务参数，由全国积分结算中心统一配置、审批和版本化，须同时配置上下限**」 | 语义完整承接，且更严 |

- 无缺句 → **FULL**。
- **PRD 严于 HTML 三点，不构成缺口，也不属超出 HTML 授权**（已写入 `conflict_summary`，下轮勿据此报冲突）：①指定配置主体为全国积分结算中心；②要求审批与版本化；③要求同时配置上下限。三点的授权来自本条 entry 自身的正文「影响结算的规则由各自责任主体在系统中**配置、审批和版本化**」与 `DEC-R8-01`，PRD 已在原句括注写明依据。
- 原 `conflict_summary` 另一条断言「承接的是另一套表述（异议期十个工作日 + 异议期届满后的下一个付账日），不含本条的完成时限目标」**同样不成立**：两份 PRD 里「完成时限目标」与「异议期届满后的下一个付账日」是并列两条，后者未取代前者，且 PRD 自身即写明「该时限是**完成目标**，不构成独立的账期口径」。
- 改动字段：`prd_path` 补第二份文件；`prd_anchor_or_line` 由旧行号（§11A L359 / L327-L328）改为实测 `:379` / `:332`；`coverage_status` `MISSING` → `FULL`；`conflict_summary` 重写。

**两条的 `recommended_change` 未动**（仍是承接前的「建议落点：……补写……」文字，现已实现）。改这一列不在本次授权的字段清单内，已在两条 `conflict_summary` 末尾写明「遗留：recommended_change 待另批清理」。

## 3. 无法复核的条目与原因（9 条）

`PGM-0194`、`BSM-0555`、`BSM-0564`、`BSM-0565`、`BSM-0566`、`BSM-0647`、`BSM-0704`、`BSM-0708`、`BSM-0734`。

共同形态：`conflict_summary` 开头已写「已闭环（2026-08-07 DOCFIX 批，PR #345 / commit 21256abd）……原判 MISSING 不再成立。**以下为原缺口叙述，仅留历史**」，`coverage_status` 已改为 `FULL`。其后的检索陈述描述的是 **PR#345 修复之前**的仓库状态。

两条不可复核的原因：

1. **检索面已不存在**。这些陈述的成立面是 `21256abd` 之前的树，按基线 `566df807` 复跑必然不同（这是修复生效的证据，不是陈述失真）。抽样对照 `21256abd^` 与 `566df807`：`综合体官方小程序` 0 → 4、`单店`（ticketing + customer-app 两 PRD）0 → 4、`下单时间`（ticketing PRD）0 → 1，方向与「已闭环」一致。
2. **原命令的检索面不可逐字复原**。这批陈述用的是工作树语义的 `grep -rn --include=*.md .`（仓根起算、含 `docs/` 之外目录）与 `grep -rn ... docs --exclude-dir=...` 混用，且部分只报「命中 N 文件」而非行数；`git grep` 的 pathspec 语义与之不等价。按近似面复跑，差异全部落在权威源 HTML 自身命中、`docs/superpowers/plans/` 等非 PRD 目录，属检索面口径差而非计数造假——但无法据此判「一致」。

判定依据：这 9 条已被同格内的「已闭环」声明覆盖，`coverage_status` 也已改判，不再作为下一轮的活判断依据，故标 `无法复核` 而非 `不一致`，不订正。

## 4. 取数命令与原始输出

全部检索在 `~/wt-final`、基线 `566df807` 上以 `git grep` 实跑；`git grep -c` 无命中时退出码为 1、无输出。核心几条的命令原文与实测：

```
git grep -c -e 一体机   566df807 -- docs/settlement-rebuild-prd-v1.0.md            # EXIT=1
git grep -c -e 学生票不等于成年 566df807 -- ":(glob)docs/*.md"                      # EXIT=1
git grep -n -e 上下限   566df807 -- docs/settlement-rebuild-prd-v1.0.md            # :712 :715 :726
git grep -n -e 自发放 -e 发放之日 566df807 -- docs/amusement-business-operations-product-prd-v0.1.md docs/ticketing-entitlement-checkin-product-prd-v0.1.md
                                                                                   # 4 行：amu:394 :454 :461 / tkt:627
grep -rniE "当月内完全清理|平台无法控制" docs --exclude-dir=_archive --exclude-dir=prd-audit
                                                                                   # 2 行：BSM HTML:1501 / finance PRD:330
grep -rniE "账期结束后一个月|一个月以内完成" docs --exclude-dir=_archive --exclude-dir=prd-audit
                                                                                   # 3 行：BSM HTML:1501 / pv:379 / mps:332
git grep -n -e 资产管理公司 566df807 -- docs/ | wc -l                               # 49
git grep -n -e 资产管理公司 "ac6f3035^" -- docs/ | wc -l                            # 43
git grep -n -e 来源单据 566df807 -- "docs/*.md" ":(exclude)docs/prd-audit/rounds/" | wc -l   # 23
git log --oneline -S"大部分账目应在当月内完全清理清楚" -- docs/finance-settlement-product-prd-v0.1.md   # e6ab52d9
git log --oneline -S"账期结束后一个月以内完成" -- docs/points-and-voucher-product-prd-v1.0.md          # e6ab52d9
```

## 5. 准确率

| 口径 | 分母 | 一致 | 不一致 | 准确率 |
|---|---|---|---|---|
| **R12 本轮新写的真检索断言** | 19（21 条命中减去 BEQ-0132、BEQ-0152 两条正则误命中） | 16 | 3 | **84.2%** |
| 全检查面真检索断言（剔除 9 条无法复核） | 21 | 17 | 4 | 81.0% |

## 6. 错误形态

**错误 100% 集中在「零命中」形态。** 4 条不实断言里 3 条把非零写成零（`BEQ-0155` 4→0、`BSM-0741` 2→0、`BSM-0742` 3→0）；剩下 1 条（`PGM-0220`）是计数与位置全错。反过来，声称「命中 N 行」的断言——`PGM-0199` 的 23 行、`PGM-0214` 的 1 行、`BEQ-0129` 的 3 处——实测**全部精确**。

**机理：报「找到了」需要拿出命中内容，报「没找到」不需要。** 后者没有自证负担。检索面写错、正则写错、时点取错、pathspec 语义搞混，任何一样都会得到一个「0」，而这个 0 看起来和「真的不存在」一模一样。`BEQ-0155` 是最干净的实例：同一句先逐字引用了目标串在 `:394` 的原文，紧接着断言同串 grep 零命中——自相矛盾却通过了整轮。

**规避（建议写入下一轮任务书）：** 凡在报告或台账里写「零命中」，必须同时记录三样：

1. **完整检索命令**，含 pathspec 与排除项（不是「全 docs 检索」这种叙述）；
2. **基线 commit**（`git grep <sha>` 或注明工作树 HEAD），杜绝时点漂移；
3. **一条已知应命中的对照串及其命中数** —— 对照串命中才证明检索本身有效。

第 3 条是关键：本轮 4 条错的，只要跑一次对照串（例如 `BSM-0741` 拿 entry 名或 source_text 里任一实词做对照）就会立刻暴露检索面选错。这与既有教训「测量失效 ≠ 被测对象通过」同源。

**订正动作本身也需要独立复核。** `PGM-0220` 同一条上数字连错三次：原始（3 行）、第一次订正（18/14/0）、本轮实测（49／43、16、3）。第一次订正正确地推翻了原始陈述，却把三个新数字全写错了。**「这是一次订正」不构成正确性豁免**——订正与原判适用同一套证据要求：附取数命令、附基线、附原始输出。
