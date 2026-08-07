# R11 shard-01 审计发现（Auditor 只读产出）

范围：A-1 承接矩阵 10 行 `MISSING` vs finding `resolved` 账实不符；A-2 GAP-21 状态账实不符
基线：`1c5c0c7d812349497240bbf57655b7d34d1e4442`（worktree `~/wt-r11`，分支 `docs/prd-audit-r11`）
本 shard **不修改任何文件**，只产出发现与拟改方案。

涉及文件：
- 承接矩阵：`docs/prd-audit/html-prd-coverage.csv`（1235 行 = 1 表头 + 1234 数据行，实测 `wc -l` = 1235，`csv.DictReader` 计数 = 1234，无跨行字段）
- 缺口台账：`docs/prd-audit/rounds/r8/findings.csv`（234 行 = 1 表头 + 233 数据行）
- 规则原文台账：`docs/prd-audit/authority-rule-ledger.csv`（列 `rule_id/html_file/entry_name/line_hint/source_text/...`）
- 落地批工作日志：`docs/prd-audit/rounds/docfix/worklog.md`

**disposition 图例（本 shard 自定义，因 r11 目录下无既有图例文件，`find docs/prd-audit/rounds/r11 -type f` 仅得 `prd-scope-manifest.txt` 与 `shard-05.md`，后者亦无图例）**：
- `A` = 证据充分，可直接照拟改文字落笔
- `B` = 待用户/规划裁定（涉新值域、涉口径选择）
- `C` = 账实一致，不改
- `D` = 并入其他条目/其他轮次处置

---

## 0. 纠正任务书前提

**无需纠正。** 任务书所述 10 行行号、rule_id、finding_id 与「其余 3 行 MISSING」清单经实测全部成立，逐条对照见 EV-01-01 与 EV-01-12。

唯一需要**补充**（非纠正）的一点：任务书只要求判 `coverage_status`，但实测这 10 行的 `prd_path` 与 `prd_anchor_or_line` 两列**同样是缺口期的旧值**（写着「待补齐承接章节」「无归口 PRD」「（无承接）」，或指向与实际落点不同的文件），只改 `coverage_status` 会留下自相矛盾的行（状态 FULL 但落点写「无归口 PRD」）。故每行拟改均给出这三列（必要时四列）的完整改法。详见 EV-01-02 起各条「拟改」。

---

## A-1 · 承接矩阵 10 行 MISSING vs finding resolved

### EV-01-01 | HISTORICAL | docs/prd-audit/html-prd-coverage.csv | @1c5c0c7d

全库 `coverage_status` 值域与分布（`csv.DictReader` 实测，非引用他处合计）：

```
Counter({'FULL': 1078, 'PARTIAL': 74, 'STALE': 69, 'MISSING': 13})
1078 + 74 + 69 + 13 = 1234 ✓ 与数据行数吻合
```

13 行 `MISSING` 的物理行号实测为：
`918, 919, 921, 931, 940, 941, 1023, 1080, 1084, 1110, 1117, 1118, 1171`
（`grep -n MISSING` 另命中 920、929，逐行回读为 `PARTIAL` 行的 `conflict_summary` 正文里提到 MISSING 一词，非状态列，已排除。）

逐行 `(行号, rule_id, finding_id, coverage_status, finding.status)` 实测：

| 行 | rule_id | finding_id | coverage_status | findings.csv status |
|---|---|---|---|---|
| 918 | PGM-0194 | R10-U-01 | MISSING | **resolved** |
| 919 | PGM-0195 | R10-U-02 | MISSING | **resolved** |
| 921 | BSM-0547 | R10-U-04 | MISSING | **resolved** |
| 931 | BSM-0555 | REGX-01 | MISSING | **resolved** |
| 940 | BSM-0564 | REGX-02 | MISSING | **resolved** |
| 941 | BSM-0565 | REGX-03 | MISSING | **resolved** |
| 1023 | BSM-0647 | REGX-04 | MISSING | **resolved** |
| 1080 | BSM-0704 | REGX-08 | MISSING | **resolved** |
| 1084 | BSM-0708 | REGX-09 | MISSING | **resolved** |
| 1110 | BSM-0734 | REGX-10 | MISSING | **resolved** |
| 1117 | BSM-0741 | REGX-11 | MISSING | open |
| 1118 | BSM-0742 | REGX-12 | MISSING | open |
| 1171 | PGM-0199 | REGX-15 | MISSING | open |

---

问题：10 行承接矩阵停在 `MISSING`，对应 finding 已 `resolved`（销案批注均写「PRD 承接已随 PR #345（DOCFIX 轮，commit 21256abd）落地」），账实不符。
拟改：见 EV-01-02 ~ EV-01-11 逐行拟改。
依据：EV-01-01（本条）
severity: P1   disposition: A

### EV-01-01b | HISTORICAL | docs/prd-audit/rounds/docfix/worklog.md:74 | @1c5c0c7d

关键节选：

```
## 待销案清单（本批不碰任何 CSV，交后续批处理）

`docs/prd-audit/rounds/r8/findings.csv` 中下列行的 `status` 仍为 `open`，承接已在本批落地，可由后续登记批销案：
```

同文件「与任务书的偏差 / 有意留白」第 6 项：`findings CSV 销案未做（硬边界），见上「待销案清单」`。

---

问题：任务书所引根因**部分成立、需要修正一处**：docfix 批自陈不碰 CSV，其中 `findings.csv` 的销案**后续已由他批补做**（10 条现为 `resolved`，销案批注日期 2026-08-07），**唯 `html-prd-coverage.csv` 一直没人补**。所以缺口不是「后续批都没做」，而是「后续批只补了 findings.csv 一张表，漏了承接矩阵」。
拟改：无（本条只用于定性根因）。
依据：EV-01-01、EV-01-01b
severity: P2   disposition: C

---

### 判定口径与改法模板（EV-01-02 ~ EV-01-11 共用）

**FULL / PARTIAL 判据**：本矩阵一行 = `authority-rule-ledger.csv` 的**一条 rule**（不是一个 HTML 条目；同一 `entry_name` 可对应多个 `rule_id`，如「销售提成」对应 BSM-0703/0704/0705/0708）。故只需判**该条 rule 的 `source_text` 是否被 PRD 逐句承接**，不需承接整个 HTML 条目。
- `source_text` 全部要件均有 PRD 正文承接 → `FULL`
- 承接了本体但**明确遗留要件**（如 PGM-0196 行的「效力范围声明」、BSM-0566 行的「仅限单一场景」）→ `PARTIAL`

**改法模板**（照 1137 行 BSM-0761／REGX-13 的既有销案惯例，实测该行 `coverage_status=FULL`、`severity` 保留 `important`、`finding_id` 保留 `REGX-13`）：
1. `coverage_status`：`MISSING` → `FULL`
2. `prd_path` / `prd_anchor_or_line`：改为实际落点（**不得再写行号**，R9 已废行号锚点制度；用 `§编号 + 小节标题`）
3. `conflict_summary`：**前置**一句 `已闭环（2026-08-07 DOCFIX 批，PR #345 / commit 21256abd）：<落点>已逐句承接，原判 MISSING 不再成立。以下为原缺口叙述，仅留历史：` ，原文保留在后
4. `recommended_change`：**前置** `【2026-08-07 DOCFIX 批销案：本建议已执行、不再有效，以下仅留历史】`
5. `severity` / `finding_id` / `decision_id` / `phase`：**不动**

> 说明：第 2 项不是任务书列的项，但不改会造出「状态 FULL、落点写『无归口 PRD』」的自相矛盾行（940/941/1084 三行原值即 `无归口 PRD`），故与状态同批改。

---

### EV-01-02 | IMPLEMENTATION_ACTIVE | docs/platform-governance-prd-v0.1.md:376 | @1c5c0c7d

规则原文（`authority-rule-ledger.csv` PGM-0194 `source_text`）：
`开关配置的每一次变更——包括模块级开关——都必须记录变更人、变更时间、变更前后的取值与影响范围。`

关键节选（PRD 承接原文，`platform-governance-prd-v0.1.md:370` 为小节头 `### 6.11 关键配置变更留痕`）：

```
5. **开关配置变更同样受本节约束（2026-08-07 补，依据 PGM「留痕与审计记录不属于可被开关关闭的模块」条；R10-U-01）**：**开关配置的每一次变更——包括模块级开关——都必须记录变更人、变更时间、变更前后的取值与影响范围**；**留痕与审计记录不属于可被开关关闭的模块**，任何层级的开关关闭都不影响留痕的写入，**被开关拒绝的访问本身同样留痕**。
```

变更日志同步已落（同文件 L871 行记 `§6.11` + `R10-U-01`）。

---

问题：承接已逐字落地，矩阵 918 行仍写 `MISSING` + 落点「§ 开关与留痕（待补齐承接章节）」。
拟改：
- `coverage_status`：`MISSING` → `FULL`
- `prd_anchor_or_line`：`§ 开关与留痕（待补齐承接章节）` → `§6.11 关键配置变更留痕·第 5 条（开关配置变更同样受本节约束）`
- `prd_path`：不变（`docs/platform-governance-prd-v0.1.md`）
- `conflict_summary` / `recommended_change`：按模板前置销案句
依据：EV-01-01、EV-01-02
severity: P1   disposition: A

### EV-01-03 | IMPLEMENTATION_ACTIVE | docs/platform-governance-prd-v0.1.md:476 | @1c5c0c7d

规则原文（PGM-0195 `source_text`）：
`数据的项目与商户隔离不提供关闭开关，与导出脱敏同例。`

关键节选（小节头在 `:466` `### 7.8 角色权限与数据权限`）：

```
> **数据隔离不提供关闭开关（2026-08-07 补，依据 PGM「所有业务数据默认按项目隔离，普通岗位没有跨项目查看权」条；R10-U-02）**：**数据的项目与商户隔离不提供关闭开关，与导出脱敏同例**。终端开关、模块开关等任何层级的开关都不得用于关闭项目与商户隔离；**只有项目之上的获授权岗位才可以跨项目汇总**，一个岗位能查看本项目普通业务数据，不代表能查看其他项目或敏感经营数据。
```

「与导出脱敏同例」的被指向物实存：同文件 `:770` `导出文件是否脱敏，不由导出人判断，也不提供关闭开关`。

---

问题：承接已逐字落地，矩阵 919 行仍写 `MISSING` + 落点「§ 数据隔离（待补齐承接章节）」。
拟改：
- `coverage_status`：`MISSING` → `FULL`
- `prd_anchor_or_line`：`§ 数据隔离（待补齐承接章节）` → `§7.8 角色权限与数据权限·「数据隔离不提供关闭开关」引用块`
依据：EV-01-01、EV-01-03
severity: P1   disposition: A

### EV-01-04 | IMPLEMENTATION_ACTIVE | docs/platform-governance-prd-v0.1.md:120-123 | @1c5c0c7d

规则原文（BSM-0547 `source_text`，**两个要件**）：
`后台配置只能承载规则，不能改变三份最高解释权 HTML 的业务口径；票务规则和商户消费业态规则不得混用。`

关键节选：

```
### 2A.6 后台配置只能承载规则，不能改变 HTML 业务口径（2026-08-07 补，依据 BSM「会员身份、成长值、等级和积分分别按已批准规则计算」条；R10-U-04）

- **会员身份、成长值、等级和积分分别按已批准规则计算，后台配置只能承载规则，不能改变三份最高解释权 HTML 的业务口径。**
- **票务规则和商户消费业态规则不得混用。**
```

两个要件均在，第二个要件（票务/业态不得混用）亦逐字。

---

问题：承接已逐字落地（含常被漏掉的第二要件），矩阵 921 行仍写 `MISSING`。
拟改：
- `coverage_status`：`MISSING` → `FULL`
- `prd_anchor_or_line`：`§ 配置与口径边界（待补齐承接章节）` → `§2A.6 后台配置只能承载规则，不能改变 HTML 业务口径`
依据：EV-01-01、EV-01-04
severity: P1   disposition: A

### EV-01-05 | IMPLEMENTATION_ACTIVE | docs/ticketing-entitlement-checkin-product-prd-v0.1.md:26 + docs/customer-app-miniapp-product-prd-v0.1.md:29 | @1c5c0c7d

规则原文（BSM-0555 `source_text`）：
`单个酒吧的单店门票允许客人在自己手机上的综合体官方小程序或App自助购买`

关键节选（ticketing PRD，`## 2. P0 目标` 在 `:13`）：

```
- **单店门票的自助购买渠道（2026-08-07 补，依据 BSM「门票主路径是客人通过官方 App/小程序或第三方平台自助购买」条；REGX-01）**：**单个酒吧的单店门票允许客人在自己手机上的综合体官方小程序或 App 自助购买**。本条只承接售卖渠道口径，不改变单店门票的收款主体与收入归属（见 `settlement-rebuild-prd-v1.0.md` §4），也不放宽下一条「工作人员平板 App 不允许代客购买门票」的红线。
```

关键节选（customer-app PRD，`## 3. 主要功能` 在 `:22`）：

```
- **单店门票自助购买**：**单个酒吧的单店门票允许客人在自己手机上的综合体官方小程序或 App 自助购买**（2026-08-07 补，依据 BSM「门票主路径是客人通过官方 App/小程序或第三方平台自助购买」条；REGX-01）。
```

原 finding 点名的两条零命中依据已被推翻：`单店门票` 在 ticketing PRD 现有 `:26` 承接，`综合体官方小程序` 在两份 PRD 均命中。

---

问题：承接已在两份 PRD 双落，矩阵 931 行仍写 `MISSING`。
拟改：
- `coverage_status`：`MISSING` → `FULL`
- `prd_anchor_or_line`：`§2 P0 目标·业务边界（待补齐单店门票客人自助购买渠道）` → `§2 P0 目标·业务边界「单店门票的自助购买渠道」；另 docs/customer-app-miniapp-product-prd-v0.1.md §3 主要功能「单店门票自助购买」`
- `prd_path`：不变（保留 ticketing 为主落点；本表 `prd_path` 单值、副落点写进 anchor，与 1118 行既有写法一致）
依据：EV-01-01、EV-01-05
severity: P1   disposition: A

### EV-01-06 | IMPLEMENTATION_ACTIVE | docs/platform-governance-prd-v0.1.md:851-859 | @1c5c0c7d

规则原文（BSM-0564 `source_text`，**四个要件**）：
`内部工单：系统提供内部部门间的工单流程。工单由内部部门发起，任何一个员工都可以发起工单；发起时填写描述、证据、时间等，并可指定受理部门；`

关键节选：

```
## 15E. 内部部门间工单（2026-08-07 补，依据 BSM「内部工单」条；REGX-02 / REGX-03）

系统提供**内部部门间**的工单流程。本节照抄 HTML 现文承接，**不推断扩写**状态机、时限、权限与考核口径。

### 15E.1 发起（REGX-02）

- **发起主体**：**工单由内部部门发起，任何一个员工都可以发起工单**。
- **发起必填项**：**发起时填写描述、证据、时间等**。
- **受理部门**：发起时**可指定受理部门**。
```

四个要件（流程存在 / 发起主体 / 必填项 / 可指定受理部门）逐条对上。

---

问题：承接已落地并新建了归口章节，矩阵 940 行仍写 `MISSING` + `prd_path=无归口 PRD`。
拟改：
- `coverage_status`：`MISSING` → `FULL`
- `prd_path`：`无归口 PRD` → `docs/platform-governance-prd-v0.1.md`
- `prd_anchor_or_line`：`（无承接）` → `§15E.1 发起（内部部门间工单）`
依据：EV-01-01、EV-01-06
severity: P1   disposition: A

### EV-01-07 | IMPLEMENTATION_ACTIVE | docs/platform-governance-prd-v0.1.md:861-865 | @1c5c0c7d

规则原文（BSM-0565 `source_text`，**两个要件**）：
`被指定受理部门必须填写本部门的处理结果并给予反馈，也可以将其中的部分事项转指定其他部门协助；`

关键节选：

```
### 15E.2 受理、转派与闭环（REGX-03）

- **强制反馈义务**：**被指定受理部门必须填写本部门的处理结果并给予反馈**。
- **转派协助**：受理部门**也可以将其中的部分事项转指定其他部门协助**。
- **闭环要求**：**每张工单最后必须形成完整闭环，未闭环的不得关闭**。
```

第三条 bullet 承接的是**另一条 rule**（`BSM-0566`，`source_text` = `每张工单最后必须形成完整闭环，未闭环的不得关闭。`），非本行范围内的扩写；本行两个要件均逐字在。

---

问题：承接已落地，矩阵 941 行仍写 `MISSING` + `prd_path=无归口 PRD`。
拟改：
- `coverage_status`：`MISSING` → `FULL`
- `prd_path`：`无归口 PRD` → `docs/platform-governance-prd-v0.1.md`
- `prd_anchor_or_line`：`（无承接）` → `§15E.2 受理、转派与闭环·强制反馈义务 / 转派协助`
依据：EV-01-01、EV-01-07
severity: P1   disposition: A

### EV-01-08 | IMPLEMENTATION_ACTIVE | docs/ticketing-entitlement-checkin-product-prd-v0.1.md:230 | @1c5c0c7d

规则原文（BSM-0647 `source_text`）：
`下单与支付时间每一笔购票订单都分别记录两个时间：下单时间是客人提交这笔订单的时刻，支付时间是这笔钱支付成功的时刻。两个时间各记各的，不能拿下单时间当支付时间用；还没付钱的，支付时间就空着。这两个时间用于对账、时效统计和全链追溯。`

关键节选（小节头 `:228` `### 7.2 订单状态`）：

```
> **下单与支付双时点（2026-08-07 补，依据 BSM「核销、退款与有效期规则·下单与支付时间」条；REGX-04）**：**每一笔购票订单都分别记录两个时间**——**下单时间**是客人提交这笔订单的时刻，**支付时间**是这笔钱支付成功的时刻。**两个时间各记各的，不能拿下单时间当支付时间用**；**还没付钱的，支付时间就空着**。这两个时间用于**对账、时效统计和全链追溯**。
```

四个要件（双时点定义 / 各记各的 / 未付款留空 / 三种用途）逐字全在。

---

问题：承接已落地在票务域 PRD，矩阵 1023 行仍写 `MISSING`，且 `prd_path` 停在 `docs/bar-business-operations-product-prd-v0.1.md`（缺口期用来指「同口径别处落点」的旧值，不是本条的承接文件）。
拟改：
- `coverage_status`：`MISSING` → `FULL`
- `prd_path`：`docs/bar-business-operations-product-prd-v0.1.md` → `docs/ticketing-entitlement-checkin-product-prd-v0.1.md`
- `prd_anchor_or_line`：`（票务域文档无对应小节；同口径落点仅见于 docs/bar-business-operations-product-prd-v0.1.md §7.1A 订台定金、docs/member-loyalty-and-stored-value-product-prd-v0.1.md 充值双时点、docs/service-order-product-prd-v0.1.md §4.2）` → `§7.2 订单状态·「下单与支付双时点」引用块`
依据：EV-01-01、EV-01-08
severity: P1   disposition: A

### EV-01-09 | IMPLEMENTATION_ACTIVE | docs/bar-business-operations-product-prd-v0.1.md:635 | @1c5c0c7d

规则原文（BSM-0704 `source_text`）：
`销售提成随薪资发放（此处「次月」按自然月计）；薪资发放日由后台配置（业务参数，按项目可配，须同时配置上下限），不得迟于次月15日`

关键节选（小节头 `:619` `## 11. 已确认规则和后续细化事项`）：

```
5e. **销售提成的发放时点（2026-08-07 补，依据 BSM「销售提成」条；REGX-08）**：**销售提成随薪资发放**（此处「次月」按自然月计）；**薪资发放日由后台配置**（业务参数，按项目可配，须同时配置上下限），**不得迟于次月 15 日**。该上限与 `finance-settlement-product-prd-v0.1.md` §4B.3「全部付款时点……一律后台可配，本 PRD 不写死具体日期」并列成硬上限：可配，但不得配到次月 15 日之后。
```

三个要件（随薪资发放且次月按自然月 / 可配且须配上下限 / 不得迟于次月 15 日）逐字全在。

---

问题：承接落在 bar PRD，矩阵 1080 行仍写 `MISSING`，且 `prd_path` 指 finance PRD、anchor 含**行号 `L318`**（R9 已废行号锚点制度）。
拟改：
- `coverage_status`：`MISSING` → `FULL`
- `prd_path`：`docs/finance-settlement-product-prd-v0.1.md` → `docs/bar-business-operations-product-prd-v0.1.md`
- `prd_anchor_or_line`：`§4B.3 付款时点的顺序校验（L318，仅泛化承接）` → `§11 已确认规则和后续细化事项·5e 销售提成的发放时点（与 finance-settlement-product-prd-v0.1.md §4B.3 并列成硬上限）`
依据：EV-01-01、EV-01-09
severity: P1   disposition: A

### EV-01-10 | IMPLEMENTATION_ACTIVE | docs/bar-business-operations-product-prd-v0.1.md:636-639 | @1c5c0c7d

规则原文（BSM-0708 `source_text`，**三个要件**）：
`退款时的提成与服务费冲回：凡属于付给酒吧的收入部分，酒吧按实退金额全额退回，对应的销售提成按订单最终保留收入重新计算并冲回差额；凡属于服务费、小费与表演报酬的部分，因退款均发生在发放之前，一律按订单最终保留收入重新计算并冲回差额，不由酒吧承担。所有冲回都生成一笔反向结算记录，不覆盖原归属、原计算与原结算记录。`

关键节选：

```
5f. **退款时的提成与服务费冲回（2026-08-07 补，依据 BSM「销售提成」条；REGX-09）**：
   - **凡属于付给酒吧的收入部分，酒吧按实退金额全额退回**，对应的**销售提成按订单最终保留收入重新计算并冲回差额**。
   - **凡属于服务费、小费与表演报酬的部分，因退款均发生在发放之前，一律按订单最终保留收入重新计算并冲回差额，不由酒吧承担**（其中**服务费与小费**部分与 §10.3 的归属判定并列，不改变其口径；**表演报酬** §10.3 未涉及，本 PRD 不对其归属作任何推断）。
   - **所有冲回都生成一笔反向结算记录，不覆盖原归属、原计算与原结算记录。**
```

三个要件逐字全在。括注只声明「不对表演报酬的**归属**作推断」，**未削弱本条的冲回义务**（表演报酬的冲回句照抄在正文里），故不构成 PARTIAL。「表演报酬」的定义/归属/计提/发放全仓零承接是**另一条待登记缺口**（docfix worklog「建议新登记」节已提，**至今未见对应 finding 行**——见 EV-01-13）。

---

问题：承接已落地，矩阵 1084 行仍写 `MISSING` + `prd_path=无归口 PRD`。
拟改：
- `coverage_status`：`MISSING` → `FULL`
- `prd_path`：`无归口 PRD` → `docs/bar-business-operations-product-prd-v0.1.md`
- `prd_anchor_or_line`：`（无）` → `§11 已确认规则和后续细化事项·5f 退款时的提成与服务费冲回`
依据：EV-01-01、EV-01-10
severity: P1   disposition: A

### EV-01-11 | IMPLEMENTATION_ACTIVE | docs/settlement-rebuild-prd-v1.0.md:46 | @1c5c0c7d

规则原文（BSM-0734 `source_text`，**两档**）：
`餐饮与零售商户（含独立、联营、租赁各种合作模式）统一由城市项目公司审批，业态为“其他”的商户由其商户档案上指定的主管公司审批`

关键节选（小节头 `:31` `## 2. 主体协作关系与职责`）：

```
**内部结算价的审批主体两档（2026-08-07 补，依据 BSM「每个项目分别建立“内部结算价管理”页面和独立价格目录」条；REGX-10）**：①**餐饮与零售商户（含独立、联营、租赁各种合作模式）统一由城市项目公司审批**——BSM 同条另有「独立餐饮商户由城市项目公司审批，但中央餐饮供货部分仍由中央餐饮公司审批」，两句并存不冲突（后一句已由 `merchant-points-settlement-rules-v1.0.md` §9 项目级内部结算价承接）；②**业态为「其他」的商户，由其商户档案上指定的主管公司审批**。**任何商户在没有获批内部结算价之前，不得为其开通需要该价格结算的权益或券。**
```

两档逐字全在。**落点与 finding 的建议不同**（finding 建议 `merchant-points-settlement-rules-v1.0.md` §9，实际落 `settlement-rebuild-prd-v1.0.md` §2），这是 docfix worklog「与任务书的偏差」第 1 项**已声明的有意偏差**，非漏做；`merchant-points-settlement-rules-v1.0.md` 未动，其 §9 现文仍为原六档。

---

问题：承接已落地，矩阵 1110 行仍写 `MISSING`，`prd_path` 指向 MPS（未承接本条的文件）、anchor 含**行号 `L172`**。
拟改：
- `coverage_status`：`MISSING` → `FULL`
- `prd_path`：`docs/merchant-points-settlement-rules-v1.0.md` → `docs/settlement-rebuild-prd-v1.0.md`
- `prd_anchor_or_line`：`§9（L172，审批主体清单缺这两档）` → `§2 主体协作关系与职责·「内部结算价的审批主体两档」`
依据：EV-01-01、EV-01-11
severity: P1   disposition: A

### EV-01-12 | NEGATIVE_SEARCH | docs/prd-audit/rounds/r8/findings.csv | @1c5c0c7d

其余 3 行 `MISSING` 的 finding 状态实测（`csv.DictReader` 逐行取 `status` 列）：

```
REGX-11 (BSM-0741, 矩阵 1117 行) | status=open | sev=advisory | 建议落点 finance-settlement-product-prd §4B.4，未落
REGX-12 (BSM-0742, 矩阵 1118 行) | status=open | sev=advisory | 建议落点 points-and-voucher-prd §11A + MPS 账期节，未落
REGX-15 (PGM-0199, 矩阵 1171 行) | status=open | sev=important | 与 R10-U-03 同因（全链可点击原则至今无归口 PRD），按用户裁决不得硬造归口
```

三者均**不在** docfix worklog「待销案清单」15 条内（该清单为 REGX-01/02/03/04/05/07/08/09/10/16/17/18 + R10-U-01/02/04），与「未落地」自洽。

---

问题：无。三行 `MISSING` × finding `open` 账实一致。
拟改：不改。
依据：EV-01-01、EV-01-12
severity: P2   disposition: C

### EV-01-13 | HISTORICAL | docs/prd-audit/rounds/docfix/worklog.md（「建议新登记」节） | @1c5c0c7d

顺带核实项（**不在任务书范围内，仅登记，不建议本轮动手**）：

关键节选：

```
- **「表演报酬」全仓 PRD 零承接**：BSM「销售提成」条的退款冲回句点名「服务费、小费与表演报酬」三类，但「表演报酬」在 `docs/**/*.md`（剔除 `_archive`、`prd-audit`）除本批新写的 `bar-…-prd` §11 5f 外**零命中**……本批**不作任何推断**，仅登记缺口，建议后续批按 HTML 现文单独立项。
```

实测 `findings.csv` 233 行内**未见**以「表演报酬」立项的 finding（`finding_id` 全集里无对应新条目；REGX 段最大编号为 REGX-18）。

---

问题：docfix 批点名的两条「建议新登记」缺口（表演报酬零承接；`data-archive-delete-pattern-design-v0.1.md` 与实现侧的批量存档闸）至今**未落成 finding 行**，与 A-1 同一根因（docfix 批的 CSV 欠账只补了一半）。
拟改：**待裁**——是否本轮补登记为新 finding，属规划决策，不在本 shard 授权内。
依据：EV-01-01b、EV-01-13
severity: P2   disposition: B

### EV-01-14 | IMPLEMENTATION_ACTIVE | docs/prd-audit/html-prd-coverage.csv:942 | @1c5c0c7d

顺带核实的**额外账实不符行**（**不在任务书点名的 13 行内**）：

矩阵 942 行现值：

```
BSM-0566 | docs/merchant-portal-zone-prd-v0.1.md | §商户异议受理人（L57） | PARTIAL | sev= | finding=
conflict_summary: merchant-portal-zone-prd L57 逐字承接「必须形成完整闭环（未闭环的工单不得关闭）」，但仅限商户异议经内部工单流转这一个场景；对全部内部工单普遍适用的闭环约束无承接落点。
```

该「残余缺口」已被 docfix 批消灭：`platform-governance-prd-v0.1.md:865` 在**面向全部内部工单**的 §15E.2 下逐字写有 `**每张工单最后必须形成完整闭环，未闭环的不得关闭**`（= BSM-0566 `source_text` 全文）。

---

问题：942 行 `PARTIAL` 所依据的残余缺口已不存在，矩阵未跟上（与 A-1 十行同批遗漏）。
拟改：
- `coverage_status`：`PARTIAL` → `FULL`
- `prd_path`：`docs/merchant-portal-zone-prd-v0.1.md` → `docs/platform-governance-prd-v0.1.md`
- `prd_anchor_or_line`：`§商户异议受理人（L57）` → `§15E.2 受理、转派与闭环·闭环要求（另 docs/merchant-portal-zone-prd-v0.1.md §商户异议受理人为场景化重述）`
- `conflict_summary`：按模板前置 `已闭环（2026-08-07 DOCFIX 批，PR #345 / commit 21256abd）：§15E.2 已对全部内部工单逐字承接闭环约束，原判残余缺口不再成立。以下仅留历史：`
依据：EV-01-07、EV-01-14
severity: P2   disposition: A

### EV-01-15 | NEGATIVE_SEARCH | docs/prd-audit/html-prd-coverage.csv | @1c5c0c7d

照抄逐字性抽查（三处**字形层**差异，不影响语义、不影响 FULL 判定，登记备查）：

| rule | HTML/台账原文 | PRD 现文 | 差异 |
|---|---|---|---|
| BSM-0555 | `或App自助购买` | `或 App 自助购买` | 中英间加半角空格 |
| BSM-0704 | `不得迟于次月15日` | `不得迟于次月 15 日` | 数字两侧加半角空格 |
| BSM-0734 | `业态为“其他”的商户` | `业态为「其他」的商户` | 直角引号替换弯引号 |

---

问题：docfix 批自陈「照抄 HTML 现文」，实际做了排版层规范化。三处均不改变语义与可判定性。
拟改：不改（若后续要上「HTML 逐字比对」机械门禁，需先明确空格/引号是否参与比对——该决策不在本 shard 授权内）。
依据：EV-01-05、EV-01-09、EV-01-11、EV-01-15
severity: P2   disposition: C

---

## A-2 · GAP-21 状态账实不符

### 纠正任务书前提（仅 A-2，一处小节号）

任务书原文：

> `prd-audit-fix-change-log-20260804.md` §7.4A：2011 与 §status 表：2253 已明写「终态：已并入 GAP-28，本条不再单独跟踪」

**两处引文的正文逐字属实**，但**小节归属有一处不符**：`:2011` 落在 `### 7.4 D 类：待改造缺口汇总`（标题在 `:1984`）内的缺口表，**不在** §7.4A；`§7.4A GAP 状态总表（2026-08-05 R8 补建）` 的标题在 `:2223`，`:2253` 才落在其中。不影响结论，仅更正引用地址。

### EV-01-16 | HISTORICAL | docs/prd-audit-fix-change-log-20260804.md:2011 | @1c5c0c7d

关键节选（§7.4 D 类待改造缺口汇总表内 GAP-21 行）：

```
| **GAP-21** | F2 待同步 | 页面权限字段矩阵未同步（工单） | `docs/full-admin-page-permission-field-matrix-v0.1.csv` | ADM-MER-001 / ADM-MER-004 的端点与状态机列仍为 V022 口径，待随工单主表收敛同步 **终态（2026-08-07 R10）：已并入 GAP-28，本条不再单独跟踪**——页面权限字段矩阵的同步随 GAP-28 的分表口径一并完成；本条只保留登记，不再单独统计为未闭合缺口。 |
```

同文件 `:2194`（§12.2 本批未改动与边界自证）：

```
- **GAP-21**（页面权限字段矩阵未同步）改按**分表**口径同步，已并入 GAP-28 描述；原条目文字保留不动，避免两处口径漂移。
```

---

问题：变更台账已宣告 GAP-21「不再单独统计为未闭合缺口」。
拟改：本条为证据，不改。
依据：EV-01-16
severity: P2   disposition: C

### EV-01-17 | HISTORICAL | docs/prd-audit-fix-change-log-20260804.md:2253 + :2260 | @1c5c0c7d

关键节选（§7.4A GAP 状态总表，标题在 `:2223`）：

```
| GAP-21 | 页面权限字段矩阵未同步 | **终态：已并入 GAP-28，本条不再单独跟踪**（2026-08-07 R10） | 本台账 §10 R6T 说明：「GAP-21 改按分表口径同步，已并入 GAP-28 描述；原条目文字保留不动」——**本条不单独闭合，跟随 GAP-28** |
...
| GAP-28 | 商户服务工单分表独立七态 | 挂起-已登记 | PRD 侧已同步（含由 GAP-21 并入的矩阵同步）；实现层未核查 |
```

---

问题：GAP-28 自身状态为「挂起-已登记 / 实现层未核查」，**并非已完成**。
拟改：本条为证据，不改。
依据：EV-01-17
severity: P2   disposition: C

### EV-01-18 | HISTORICAL | docs/prd-audit/rounds/r8/findings.csv | @1c5c0c7d

两张表的状态用词对照（逐条实测，非引用他处结论）：

| finding | 变更台账 §7.4A 状态列 | findings.csv `status` | 是否对齐 |
|---|---|---|---|
| GAP-20 | 挂起-无人复核 | `open` | ✓ |
| GAP-22 | 挂起-无人复核 | `open` | ✓ |
| GAP-26 / 27 / 28 / 29 | 挂起-已登记 | `open` | ✓ |
| GAP-23 | **fixed-in-r9** | `fixed-in-r9` | ✓ |
| GAP-35 | **fixed-in-r9** | `fixed-in-r9` | ✓ |
| GAP-25 | 已闭合(文档层) | `closed-doc-layer` | ✓ |
| GAP-36 | 已闭合(文档层) | `closed-doc-layer` | ✓ |
| **GAP-21** | **终态：已并入 GAP-28，本条不再单独跟踪** | **`open`** | **✗** |

GAP-21 现行 `disposition` 实测为：`页面权限字段矩阵未同步｜挂起-已登记(并入 GAP-28)`——「并入」这一信息**已在 disposition 列**，缺的只是 `status` 列没有对应表达，故 GAP-21 仍落在 `status=open` 桶里，与 GAP-28 **重复计数**。

---

问题：两张表按上表惯例本应逐条对齐，唯 GAP-21 未对齐；R10 在变更台账写下「终态」时没有回写 findings.csv（与 A-1 同一类根因：台账双写、只改一张）。
拟改：见 EV-01-20。
依据：EV-01-16、EV-01-17、EV-01-18
severity: P1   disposition: B

### EV-01-19 | IMPLEMENTATION_INERT | docs/full-admin-page-permission-field-matrix-v0.1.csv:32,35 | @1c5c0c7d

**GAP-21 的实质工作是否已完成的实测**（这一步决定了绝不能把 GAP-21 标成完成类状态）：

ENUM 独立七态（`docs/building-operations-prd-v1.0.md:16` 逐字）：
```
采用 ENUM「商户服务工单状态」条的**独立七态状态链**：已提交 / 已派单 / 处理中 / 待商户确认 / 已处理待关闭 / 已关闭 / 已取消
```

矩阵 CSV `status_flow` 列现值（`csv.DictReader` 取列，非目测）：
```
ADM-MER-001 | status_flow = created->assigned->processing->closed/reopened          （4 态 + reopened，非七态）
ADM-MER-004 | status_flow = created->assigned->processing->pending_merchant->resolved->closed （6 态，缺「已取消」，且无「已处理待关闭」独立态）
```

`docs/building-operations-prd-v1.0.md:110` 明写该同步**未做**：
```
**改造列入排期**：补齐商户侧独立七态 DB CHECK 与状态机（现取值与 ENUM 七态未逐值核对）、`docs/full-admin-page-permission-field-matrix-v0.1.csv` 的 ADM-MER-001／ADM-MER-004 端点与状态机列按分表口径同步——本轮只改 PRD 口径，**不改 CSV、不改代码、不改迁移**
```

---

问题：GAP-21 的实质交付物（矩阵 CSV 两行的状态机列）**至今未同步**，两行现值均不是 ENUM 七态。
拟改：**不得**把 GAP-21 改为 `resolved` / `fixed` / `fixed-in-r10` / `implemented` / `closed-doc-layer` 中任何一个——那会把「未做的活」记成已完成，且会造出「被并入条已闭合、并入目标条 GAP-28 仍 open」的倒挂。
依据：EV-01-17、EV-01-19
severity: P0   disposition: A（此处的 A 指「不得标完成」这一约束成立，可直接据以否决错误改法）

### EV-01-20 | NEGATIVE_SEARCH | docs/prd-audit/rounds/r8/findings.csv | @1c5c0c7d

`status` 列现有值域全集（233 数据行实测计数，分项相加 = 233 ✓）：

```
fixed 78 | open 75 | resolved 31 | implemented 28 | fixed-in-rework 8 | fixed-in-r9 5
ruled-pending-implementation 3 | closed-doc-layer 2 | in-progress 1 | fixed-in-r10 1 | accepted-by-user 1
78+75+31+28+8+5+3+2+1+1+1 = 233 ✓
```

逐值适配性核验（每个值都对照其现有用例的语义，不凭字面猜）：

| 候选值 | 现有用例语义（实测） | 用于 GAP-21 是否成立 |
|---|---|---|
| `open` | GAP-20/22/26/27/28/29 = 挂起，仍单独跟踪 | ✗ 与「不再单独跟踪、不再单独统计为未闭合缺口」直接冲突（现状即此，正是账实不符本身） |
| `fixed` / `fixed-in-rework` / `fixed-in-r9` / `fixed-in-r10` / `implemented` / `resolved` | 均为「活已干完」类（如 R10-U-01 销案批注「PRD 承接已随 PR #345 落地」） | ✗ EV-01-19 实测矩阵 CSV 未同步，标之即虚报 |
| `closed-doc-layer` | GAP-25 / GAP-36 = 文档层已清理完毕、实现层另计 | ✗ GAP-21 的交付物本身就是一份台账 CSV，文档层也没做完 |
| `ruled-pending-implementation` | R8-B-01/03/10 = 已有明确裁决、方向锁定、待实施 | △ 语义最近（GAP-21 确由 UD-06 / DEC-R6-26 裁定改按分表口径、待实施），但**不表达「并入 GAP-28、不再单独统计」** |
| `in-progress` | R8-B-02 = 正在做 | ✗ 无人在做 |
| `accepted-by-user` | R10-U-06 = 用户知情后裁定不修 | ✗ 不是不修，是随 GAP-28 修 |

**结论：现有 11 个取值中没有一个能精确表达「已并入他条、不再单独统计、实施未完成」**。按任务书「不得自创新状态值 —— 若现有取值都不合适，标为待裁」，**本条标为待裁**，并提交两个候选：

- **候选甲（推荐，不动值域）**：`status` `open` → `ruled-pending-implementation`；同时 `disposition` 由
  `页面权限字段矩阵未同步｜挂起-已登记(并入 GAP-28)`
  改为
  `页面权限字段矩阵未同步｜终态：已并入 GAP-28，本条不再单独跟踪、不再单独统计为未闭合缺口（2026-08-07 R10，见 prd-audit-fix-change-log-20260804.md §7.4 GAP-21 行与 §7.4A GAP 状态总表）；实质交付物＝full-admin-page-permission-field-matrix-v0.1.csv 的 ADM-MER-001／ADM-MER-004 status_flow 按 ENUM 独立七态同步，随 GAP-28 一并完成`
  - 好处：把 GAP-21 移出 `open` 桶、消除与 GAP-28 的重复计数；不虚报完成；不新增值域。
  - 代价：`status` 列本身不携带「并入」语义，靠 `disposition` 承载——**与本表既有做法一致**（GAP-21 现行 disposition 已在用括注承载「(并入 GAP-28)」）。

- **候选乙（须用户批准扩值域）**：新增一个表示并案的取值（如 `merged-into`），并在 `disposition` 写明目标条 `GAP-28`。
  - 好处：语义精确、可机械去重统计。
  - 代价：值域 +1，且需回头统一处理表内其他并案情形（实测另有并案表述散在 disposition 文字里，如 REGX-15「建议并入 R10-U-03 一并处置」、GAP-29「同源、不重复立项」、GAP-11「与 GAP-38 同源」——若开此值域，这几条是否一并改需一并裁）。

**不推荐**：维持 `open` 不动。理由是变更台账已白纸黑字写「不再单独统计为未闭合缺口」，维持 `open` 等于让两张 canonical 台账继续互相打架，且任何按 `status=open` 出的未闭合缺口数都会把 GAP-21 与 GAP-28 算成两条。

---

问题：GAP-21 在 findings.csv 仍为 `open`，与变更台账两处「终态：已并入 GAP-28，本条不再单独跟踪」冲突；而现有 status 值域无「已并入」语义。
拟改：**待裁**——推荐候选甲（`open` → `ruled-pending-implementation` + disposition 改写，逐字见上）；候选乙须先批准扩值域并统一处理其余并案条目。
依据：EV-01-16、EV-01-17、EV-01-18、EV-01-19、EV-01-20
severity: P1   disposition: B

---

## 收工自检

- 本 shard 唯一写入：`docs/prd-audit/rounds/r11/audit-shards/shard-01.md`（新建，append-only 三次追加）
- 未修改：四张 canonical 台账（`html-prd-coverage.csv` / `authority-rule-ledger.csv` / `entry-registry.csv` / `rounds/r8/findings.csv`）、任何 PRD、三份 HTML、其他 shard、代码/配置/迁移
- 未执行 `git add` / `git commit` / `git push`
- `git -C ~/wt-r11 status --porcelain` → `?? docs/prd-audit/rounds/r11/`（该目录本已存在未跟踪的 `prd-scope-manifest.txt` 与他人 shard，故整目录以单条 `??` 呈现）

## 待办移交（供 Fixer 批直接执行）

| # | 文件 | 改法 | 依据 EV | disposition |
|---|---|---|---|---|
| 1 | `html-prd-coverage.csv` 918/919/921/931/940/941/1023/1080/1084 行 | `MISSING`→`FULL` + 落点三列按逐条拟改改写 | EV-01-02~10 | A |
| 2 | `html-prd-coverage.csv` 1110 行 | 同上（注意 `prd_path` 改指 `settlement-rebuild-prd-v1.0.md`，非 finding 里建议的 MPS） | EV-01-11 | A |
| 3 | `html-prd-coverage.csv` 942 行（BSM-0566） | `PARTIAL`→`FULL` + 落点改指 gov §15E.2 | EV-01-14 | A |
| 4 | `html-prd-coverage.csv` 1117/1118/1171 行 | **不改** | EV-01-12 | C |
| 5 | `rounds/r8/findings.csv` GAP-21 行 | **待裁后再改**（候选甲逐字见 EV-01-20） | EV-01-20 | B |
| 6 | 「表演报酬」零承接 / 批量存档闸 两条建议新登记 | **待裁**是否本轮补立 finding | EV-01-13 | B |
