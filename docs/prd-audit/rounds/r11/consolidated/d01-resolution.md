# D-01 结案 · 两份代码轮输入报告的钱路/非钱路分流判据

> 只读调查产物。基线 `1c5c0c7d`。不改 shard／台账／PRD／HTML／代码，不 commit。

```
D-01 结论:  未用过
判据实际是:  populations 取自 findings.csv 的 open 行，非取自 authority-rule-ledger.csv
             逐字原文（两份报告全文仅此一句涉及分流）：
             「findings open 66 条中 16 条归钱路组，本报告覆盖 50 条 + 2 项调查中新发现」
             — docs/prd-audit/rounds/final/code-round-input-nonmoney.md:5
             佐证（同批入库的台账登记，写明逐条依据）：
             「逐条判定与证据见 `docs/prd-audit/rounds/r8/findings.csv`（finding_id 见各行「出处」）」
             — docs/pending-development-backlog-v0.1.md:199
```

## 一、认定「未用过」的五条实证

| # | 证据 | 实测 |
|---|---|---|
| 1 | 分流母体是 findings.csv，该文件**根本没有 `value_flow` 列** | `git show 1c5c0c7d:docs/prd-audit/rounds/r8/findings.csv` 表头 = `finding_id,type,severity,file,line,html_evidence,disposition,status`（8 列，无 `value_flow`） |
| 2 | 分流基数**逐数字对得上 findings.csv**，对不上账本 | 非钱路报告自述基线 `7da3aaa1`；`git show 7da3aaa1:.../findings.csv` 实测 total 204 / **open 恰为 66**。16＋50＝66 完全吻合。账本 `value_flow` 非空（非 ORPHANED、排除 `NOT_STATED`）实测 **441 行**，与 16 无任何对应关系 |
| 3 | 钱路报告**通篇零个账本 rule_id** | 全文扫 `BSM-/PGM-/BEQ-` 前缀：**0 命中**。全文仅出现 `DEC-R8-54`（HTML 裁决号）与 `R-3`（旧审计项号）。以 `value_flow` 列筛出的清单不可能一个 rule_id 都不带 |
| 4 | 非钱路报告出现的 ID **全是 finding_id** | `GAP-02/19/20/21/26/30/33/40`、`REGX-15` 九个逐一在 `7da3aaa1:findings.csv` 的 `finding_id` 列命中（`REQ-110` 为需求号、被 finding 正文引用） |
| 5 | 钱路报告**实际收录了一条 `value_flow=NOT_STATED` 的钱路**（反证） | `BEQ-0117`（分红税务：个税代扣安全阈值、三审、出厂默认 20%）`value_flow` 实测 `NOT_STATED`；而钱路报告第 6 项「个税安全阈值三审」正是该条主题（`tax_rate_bps` 仅 0~10000 校验／单权限一步改／无审批流）。若曾以 `value_flow` 非空为闸，该项进不了钱路清单 |

结论：两份报告的分流母体是 **findings.csv 的 open 集合**，与 `authority-rule-ledger.csv` 的 `value_flow` 列**无取数关系**。
D-01 所担心的「钱路清单被 204 格 `source_text` 逐字副本虚高约一半」**在这两份报告上不成立**——
它们从未读过那一列。**D-01 可关闭。**

## 二、必须同时记下的限制（不能写成「判据已查明」）

两份报告**都没有方法／口径／取数章节**。检索 `value_flow`／`非空`／`涉钱`／`筛选`／`取数`／`判据`／
`口径`／`方法`／`分流`：钱路报告 0 命中，非钱路报告仅命中上引第 5 行那一句。

因此本次能确定的只有两件事：

- **确定**：分流母体 = findings.csv open 66（数字吻合＋逐条依据见台账 line 199）；**没用** `value_flow` 列。
- **未写明**：这 66 条里「哪 16 条归钱路组」所依据的**具体判定规则**，两份报告一字未写，
  `docs/prd-audit/rounds/final/worklog.md`（627 行，全文检索 `钱路`／`value_flow`）也未记该拆分过程——
  worklog 只在 §T5b:423 追认钱路报告的来源文件路径。**此项按事实记为「报告未写方法」，不作推测。**

## 三、顺带核实、但不属 D-01 结论的两个数

- 账本 `value_flow` 非空口径下 `norm(value_flow) == norm(source_text)` 的**逐字副本行实测 204 行**，
  与 D-01 背景给出的 204 一致；分母本次实测为 441（非 ORPHANED 且排除 `NOT_STATED`），
  与背景所述 406 有差（占比 46.3% vs 50.2%）。**分母口径差异不影响本结论**，未追查。
- `decision_status` 列实际取值为 `AUTHORITATIVE` 1156 / `SUPERSEDED` 69（worklog 所称「ORPHANED 69 行」对应 `SUPERSEDED`）。

## 四、实际执行的读取命令（全部只读）

```
git show 1c5c0c7d:docs/prd-audit/rounds/final/code-round-input-money.md
git show 1c5c0c7d:docs/prd-audit/rounds/final/code-round-input-nonmoney.md
git show 1c5c0c7d:docs/prd-audit/rounds/final/worklog.md
git show 1c5c0c7d:docs/prd-audit/rounds/r8/findings.csv
git show 7da3aaa1:docs/prd-audit/rounds/r8/findings.csv
git show 1c5c0c7d:docs/prd-audit/authority-rule-ledger.csv
git show 1c5c0c7d:docs/pending-development-backlog-v0.1.md
git log -1 --format=%B 7c54ded9        # 两份报告的入库提交
git grep -n "钱路组" 1c5c0c7d -- docs   # 全仓仅 2 命中，均在非钱路报告内
```
