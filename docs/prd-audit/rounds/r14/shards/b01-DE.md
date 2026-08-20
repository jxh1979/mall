已完成 shard DE 的三关审计。以下是完整 shard 报告。

---

# R14-b01 Auditor Shard **DE** — `docs/dividend-engine-prd-v0.1.md` @ `ff2037a1`

## 1. 覆盖声明

**目标文件**：`docs/dividend-engine-prd-v0.1.md`（352 行 / 69704 字节 @ `ff2037a1`，`git show` 取证，全程未读工作区文件）
**取证仓**：`/home/james/complex-management`（HEAD=`e3770efa`，与 `AUDIT_BASE_SHA` 不同，故全部取证走 `git show ff2037a1:<path>`）

| 关 | skill | 核过的面 | 结果 |
|---|---|---|---|
| CONFORMANCE | `mattpocock-skills:domain-modeling` | coverage 20 承接行逐行过（FULL 19 全部抽验、STALE 1 必查）+ ARL 20 行 `source_text` 与 HTML 实文长度逐条比对 + BSM L1727–L1760 / ENUM L285–L294 定向取节 + PGM L301/L435 + 决策台账 DEC-R7-21/R7-41/R8-92/R8-97-08/R8-98/R4-63/R4-67 + §G-1~G-6 + canonical findings 238 行 + r11 gaps 73 行 | 9 条 finding |
| ADVERSARIAL | `mattpocock-skills:grilling` | 跨条算术代实数串链（净利→亏损结转→劈分→税→抵扣→自定义→留存→payout，附守恒复算）+ 不可证伪措辞 13 词全文扫描 + 完成 claim 逐条回代码树验证 + 跨文件镜像句（finance-settlement §4B.1、platform-governance-prd §12C、BSM L822） | 5 条 finding |
| FIRST_PRINCIPLES | `ponytail:ponytail-audit` | 每条规范约束追 business rationale 出处（HTML 条目名 / DEC 编号）；专查悬空参数、同一事三口径、只有实现理由的约束、越权项进正文恒等式 | 3 条 finding |

**R13/R13后 追溯**：`git log --oneline 2bc7cb0f..ff2037a1 -- docs/dividend-engine-prd-v0.1.md` 实测 **3 个 commit**（`76213531` / `3f3e30cd` / `ff2037a1`），与任务包列出的三个一致，逐 hunk 过完（见 §6）。

**未能取证的面**：`DEPLOYED_SHA` 不可知，本轮零 `RUNTIME_OBSERVED`；下述实现层结论一律标 `WIRING_ACTIVE` 或 `IMPLEMENTATION_INERT`。

### Counting contract（先定义再给数，两套口径不互换不相减）

**CC-1 · 本文件的 coverage 承接行数**
计数对象＝`html-prd-coverage.csv` 物理数据行；解析＝RFC4180 + `utf-8-sig`（BOM+CRLF），全表 1234 行；过滤＝`prd_path` 含 `dividend-engine`；不去重；时点＝`ff2037a1`。
→ **20 行**（BSM 14 / BEQ 6；`coverage_status` FULL 19、STALE 1）。
任务包预估的 18（BSM 持股分红 12 + ENUM 十三 6）按 section 归属估算，未含 `regext-expansion` 阶段追加的 BSM-0591/0786/0788/0789/0790/0791 与 BSM-0411。两数**不相减**，以 CC-1 为本 shard 执行口径。

**CC-2 · 分红域权威规则的承接登记落点**
计数对象＝`authority-rule-ledger.csv` 行且 `line_hint` 可解析为 `L<int>`；纳入＝`html_file`=BSM 且 1727≤L≤1760，或 =ENUM 且 285≤L≤294；按 `rule_id` 去重；时点＝`ff2037a1`。
→ 分母 **28**；承接登记指向 dividend-engine **18**、指向 finance-settlement **9**、指向 platform-governance-prd **1**、无 coverage 行 **0**。9 条中 BSM-0425（连接成本与费用台账）属正当归口，其余 **8 条错位**（见 R14-DE-03）。

---

## 2. Findings 总表

| finding_id | audit_lens | severity | disposition | 文件:条目名 | 一句话 | EV |
|---|---|---|---|---|---|---|
| R14-DE-01 | CONFORMANCE | **P0** | A | dividend-engine §9 发放保护线 / §5B 清算前置 | 「释放机制未实现＝该负债无出口」缺口块在基线上被实现证据整体证伪：端点、规则、UI、自动作业、迁移全部存在且早于该块写入日 15 天 | EV-DE-05~08 |
| R14-DE-02 | CONFORMANCE | **P0** | A | §5 完整守恒 / §6 分红前置 | 「当期可分配利润必须先偿还全部到期借款本息」在 §5 算式链与恒等式中零落点，§5「完整守恒」绝对陈述与该前置不能同时为真 | EV-DE-09~12 |
| R14-DE-03 | CONFORMANCE | P1 | A | html-prd-coverage.csv（8 行） | 分红域 8 条 AUTHORITATIVE 规则唯一承接登记指向 finance-settlement 中不含该规则的节 | EV-DE-13,14 |
| R14-DE-04 | ADVERSARIAL | P1 | A | §2#2 / §3 / §5 代扣个税 | 同一税额三处三口径；`eff_tax_rate`/`tax_rate_bps` 成为无算式引用的悬空参数 | EV-DE-15 |
| R14-DE-05 | ADVERSARIAL | P1 | A | §2#2 vs §4A#4 税率调整 | 「财务/税务/法务三审＋顶层平台公司最终确认」与「最高权限审批」两条互斥审批链并存 | EV-DE-16 |
| R14-DE-06 | CONFORMANCE | P1 | C | ENUM「可选扣除项」L292 | 欠款抵扣债权人 ENUM 写「对平台」、BSM 2026-08-01 已改「对相应法律主体」，ENUM 未镜像 | EV-DE-17,18 |
| R14-DE-07 | ADVERSARIAL | P1 | A | §5B 付账日 | 「每月 5 / 15 / 25 日」写成固定值，BSM L822 与 finance-settlement §4B.1 现文均为「业务参数，全平台统一可配，初始为…」 | EV-DE-19 |
| R14-DE-08 | CONFORMANCE | P1 | B | §9 发放保护线「由大股东决定」 | HTML 把留存比例决定权给大股东；PRD 把含 `payout_floor_bps` 的全部方案变更路由到最高权限审批，「大股东」在 PRD 无定义、无落点 | EV-DE-20 |
| R14-DE-09 | CONFORMANCE | P1 | A | §9 亏损结转·存量回填 | 「历史多期真累计不回溯——设计边界」与 BSM L1739「亏损一直累着」冲突，无 HTML/DEC 依据 | EV-DE-21 |
| R14-DE-10 | CODE-FOLLOWUP | P1 | CODE-FOLLOWUP | §5A 生产禁用建议第 1 条 | `customDeductions` 写入口在基线上仍可达，越权能力未按 PRD 处置下线 | EV-DE-22 |
| R14-DE-11 | ADVERSARIAL | P2 | A | §9 期间关账锁 | 命名的判定纯函数 `DividendPeriodLock` 在全树仅存于三份文档，`backend`/`apps` 零命中 | EV-DE-23 |
| R14-DE-12 | CONFORMANCE | P2 | A | §5 分红税务法定例外 | 引用的 PGM「金额一律精确到分」条三份 HTML 零命中，实际条目名为 PGM「全局原则 · 金额精度与税务例外」 | EV-DE-24 |
| R14-DE-13 | CONFORMANCE | P2 | A | §4 期间作业 | 「preview 仅对季度方案出」窄于 HTML 无条件「每月另出一张只读的预览单」；权威半年周期落地后无预览路径 | EV-DE-25 |
| R14-DE-14 | FIRST_PRINCIPLES | P2 | A | §9 发放额恒等式 | 越权实现项 `custom_deduction_cent` 进入正文规范恒等式，而非只留在 §5A 留档块 | EV-DE-26 |
| R14-DE-15 | ADVERSARIAL | P2 | A | 变更日志表 | 空行把表劈成两张，后表无表头且 10 行为 5 列（表头 4 列），`ff2037a1` 的 A-05 只修了 1 行 | EV-DE-27 |
| R14-DE-16 | FIRST_PRINCIPLES | P2 | A | 文件头 v0.2 / 文件名 v0.1 | `3f3e30cd` 把标题改成 v0.2 未改文件名，全仓引用仍为 `-v0.1.md` | EV-DE-28 |
| R14-DE-17 | ADVERSARIAL | P2 | D | DEC-R8-92 批准真身 | 该 DEC 的用户批准除台账自述外无独立产物，取证链未跑完 | EV-DE-29 |

**三关均有发现**，无「本关无发现」情形。

---

## 3. A 类明细（fix-ready）

### R14-DE-01 · P0 · §9 发放保护线条目 + §5B 清算前置

**现行原文逐字（§9 L280–L285）**：
> **释放机制（下期/年度末返还规则 + 手动释放端点 + UI）为后续独立构建 R**，本轮只落待释放负债。
> > **【缺口登记（2026-08-04 F5）：释放机制未实现 = 该负债无出口】**
> > …现状只完成了**挂负债**这一半：`dividend_reserve_withheld` 应付**只进不出**——**没有任何释放端点、没有释放规则、没有 UI 入口**，该负债**没有出口**，只能无限期挂账。
> > - **后果**：①「暂扣待释放」在事实上退化为「平台无限期占用」，与 HTML 口径背离；②该负债金额随每期留存单调递增，无人对账；③**酒吧清算无法完成**……
> > - **在释放机制上线之前**：开启 `payout_floor_enabled` 须逐个方案评估并知会主理人（其款项会被无限期暂扣），且**不得据现状认为「暂扣待释放」已满足 HTML 要求**。

**现行原文逐字（§5B L168）**：
> 因此**清算目前无法真正完成**：该负债只能一直挂着。**释放机制是清算的硬前置**，未实现前不得宣称酒吧清算可闭环。

**证伪证据（@ff2037a1，逐项）**：

| PRD 的否定断言 | 基线上的实际产物 | 证据层级 |
|---|---|---|
| 没有释放规则 | `V304__dividend_reserve_release.sql`：`dividend_plans.reserve_release_mode ∈ {manual, next_period, fixed_delay, annual}` + `reserve_release_delay_periods` + `chk_dp_reserve_release_mode` CHECK + `dividend_reserve_releases` 释放台账 + `uq_dividend_reserve_releases_statement` 幂等键 | `MIGRATION_DECLARED` |
| 没有手动释放端点 | `DividendController` L111 `@PostMapping("/statements/{id}/release-reserve")`；`openapi-p0.yaml` L24699，`x-audit-event-success: dividend.reserve_released`；另有 L101 `@GetMapping("/reserve-releases")` 台账端点（openapi L24641） | `WIRING_ACTIVE` |
| 没有 UI 入口 | `apps/web/src/admin/pages/FinanceDividendsPage.tsx`、`FinanceDividendsPage.reserve.test.tsx`、`MerchantZoneDividendsPage.tsx` | `IMPLEMENTATION_INERT` |
| 「为后续独立构建 R」 | `78057acc`（2026-07-20，PR #153）标题即「建设R: 分红发放保护线留存·释放机制(V304)」——**该构建早于本缺口块写入日（2026-08-04）15 天** | `BASELINE_STATE` |
| 只进不出 | V304 头注释：re-tag `dividend_reserve_withheld → dividend_reserve_released`，金额不变全额一次释放；未释放应付被 `settleArApItem` 支付守卫直接拒付 | `MIGRATION_DECLARED` |

同时 PRD **自身 §5 L155–156** 已在引用释放侧算法：「风险准备金「释放」额（`DividendReserveReleaseMath.releasableCent`）保持向下取整……**当前恒以全额比例 10000 调用**」——与同文件 §9「没有任何释放端点、没有释放规则」直接互斥。

**应改成什么**：删除 §9 的整个【缺口登记（2026-08-04 F5）：释放机制未实现 = 该负债无出口】块与 §5B「清算目前无法真正完成 / 释放机制是清算的硬前置」交叉标注；按基线实现现状如实改写为：留存释放已落地（四种释放时点 `manual`（出厂默认，不自动释放）/ `next_period` / `fixed_delay` N 期 / `annual`；手动端点 `POST /finance/dividends/statements/{id}/release-reserve`；释放台账 `GET /finance/dividends/reserve-releases` 与 `dividend_reserve_releases` 表；自动作业 `DividendReserveReleaseJob`，`dividend.reserve-release.scheduled.enabled` 默认关；未释放应付被支付守卫拦截）；同步删除对主理人的「款项会被无限期暂扣」告知要求，并删除 §5B 的清算不可闭环结论。

**依据**：BSM「发放保护线留下的这部分是「暂扣、待释放」」条（L1745）与 ENUM「发放保护线」条（L293「留存待释放 / 已释放（后来按规则手动、下期或年度末发还给主理人）」）—— 业务口径未变，改的是 PRD 对**实现状态**的错误陈述，不构成以代码授权业务口径。

**风险说明（定 P0 的理由）**：该错误陈述已在文内存续 3.5 个月并跨 R7/R10/R13/DEC-R8-98 四轮未被推翻；按现文运营会（a）压制一项已上线的资金返还能力、（b）向主理人作出「款项无限期暂扣」的错误告知、（c）把酒吧清算判为不可闭环。属资金 + 可致大范围错误实施。

---

### R14-DE-02 · P0 · §5 完整守恒 / §6 分红前置

**权威原文逐字**
BSM「分红前置」条 L1744：
> 分红前置：存在逾期未还借款的，分红一律优先偿还借款——**当期可分配利润必须先偿还全部到期借款本息，剩余部分才可用于分红**，分红方案提交时须附借款清偿情况（见平台治理地图）；该前置不属于本处三项扣除开关。

PGM L435（同句，并为 `platform-governance-prd-v0.1.md` §12C L736 逐字镜像，该 PRD 另加一句「**该口径与 `dividend-engine` 分红基数计算必须一致**」）。DEC-R7-21（2026-08-05）同文。

**PRD 现行原文逐字（§6 L234）**：
> **存在逾期未还借款的一律不得提交分红方案**——`draft → pending_review` 提交时校验该酒吧主体名下的**逾期未还借款**，命中即拒绝提交并回显理由；当期可分配利润**必须先偿还全部到期借款本息，剩余部分才可用于分红**；分红方案提交时须随单附**借款清偿情况**。

**PRD 现行原文逐字（§5 L152）**：
> **完整守恒**：链上每一步的另一半均取补数定义（…），因此 `平台分成 + 依法确定的正式税额 + 欠款抵扣 + 自定义扣除 + 风险准备金 + 主理人实得 ≡ 可分配额` 逐分严格相等；非税取整只改变对应项大小，不改变守恒结构。

**问题**：HTML 的两个分句管两种情形——**已逾期**的走「一律不得分红」（PRD 已承接为提交闸），**本期到期但尚未逾期**的走「先偿还全部到期借款本息，剩余才可分红」。第二个分句是一个**算式步骤**，而 PRD 的算式链（§5 L136–143）与 §9 恒等式（`chk_ds_distributable` / `chk_ds_payout_identity` 等五条）**完全没有该项**；§9 三扣除开关明文排除它，§5A 自定义扣除也明文排除它。因此该权威算式步骤在本 PRD 内零落点，而 §5「完整守恒」是一条**绝对陈述**，与之不能同时为真。

**代实数串链（本关跨条算术，全部单位＝分）**
方案：平台 8000 bps / 主理人 2000 bps；期初累计亏损 100,000；本期净利润 1,000,000；本期**到期但未逾期**借款本息 300,000；主理人本人对相应法律主体欠款 0；`payout_floor_bps` = 0。

- 按 PRD §5 现文：`applied` = min(100,000, 1,000,000) = 100,000 → `distributable` = 900,000 → 平台 = HALF_UP(900,000×0.8) = 720,000 → `principal_gross` = 180,000 → 税(20%) = 36,000 → `principal_net` = 144,000 → `payout` = 144,000。守恒 720,000+36,000+0+0+0+144,000 = 900,000 ✓。**借款 300,000 未被偿还，全额分光。**
- 按 BSM L1744 / PGM L435 / DEC-R7-21：可分配利润 900,000 先偿还到期本息 300,000 → 可用于分红 600,000 → 平台 480,000 / 主理人税前 120,000。
- 差额：平台多分 240,000、主理人多分 60,000，出借主体少收 300,000。**分红一旦 `paid` 即为对外付款，追回困难。**

§6 的提交闸拦不住这个场景——借款「到期」当日尚未构成「逾期未还」，提交闸按定义不命中。

**应改成什么**（本条的唯一目标状态部分）：
1. §5「完整守恒」句必须停止作为绝对陈述——改为在恒等式左侧加入到期借款清偿项，或明确写出该恒等式仅在「当期无到期借款清偿」时成立并援引 §6 前置条款；二者取其一，不得保留现行无条件表述。
2. §5 算式链与 §9 恒等式清单必须出现「到期借款本息清偿」这一步，并在 §6 明确它作用于 `distributable`（HTML 明文「当期可分配利润」）而非主理人单侧。

**依据**：BSM「分红前置」条 L1744 逐字；PGM L435；DEC-R7-21；`platform-governance-prd-v0.1.md` §12C L736「该口径与 `dividend-engine` 分红基数计算必须一致」。

**与 GAP-41 的边界**：`GAP-41`（canonical findings，`open`，`分红前置借款清偿实现无落点｜挂起-已登记`）是**实现侧**零落点，DEC-R7-41 当时断言「已落 HTML 与 PRD」。本条证明**PRD 侧的落地只覆盖了提交闸，算式侧同样零落点**，DEC-R7-41 的该项断言不成立。不重列 GAP-41，按引用补充。

**尾差与不足清偿的处理边界属推断，另见 §5 推断补写清单第 1 条。**

---

### R14-DE-03 · P1 · `docs/prd-audit/html-prd-coverage.csv`（8 行）

**现行登记（`rule_id | line_hint | 现登记 prd_path @ anchor`）**：

| rule_id | HTML 行 | 条目名 | 现登记落点 |
|---|---|---|---|
| BSM-0412 | L1733 | 分红基数按「净利润」算 | finance-settlement `@14. 商户费用归集台账` |
| BSM-0415 | L1736 | 分红原则上每半年分配一次 | finance-settlement `@§4A DEC-BIZDAY-001 + T-INVOICE-LINK` |
| BSM-0421 | L1746 | 酒吧关闭的须进行清算，清算时必须完成分红 | finance-settlement `@14. 商户费用归集台账` |
| BSM-0422 | L1752 | 平台财务 | finance-settlement `@15. 持股分红引擎（V299，指针）` |
| BSM-0423 | L1753 | 有最高权限的管理者 | finance-settlement `@14. 商户费用归集台账` |
| BEQ-0118 | L290 | 分红周期 | finance-settlement `@§4A DEC-BIZDAY-001 + T-INVOICE-LINK` |
| BEQ-0121 | L293 | 发放保护线留存 | finance-settlement `@14. 商户费用归集台账` |
| BEQ-0122 | L294 | 期间关账 | finance-settlement `@14. 商户费用归集台账` |

八行 `coverage_status` 全为 `FULL`。

**证伪（`NEGATIVE_SEARCH`，检索面＝`git show ff2037a1:docs/finance-settlement-product-prd-v0.1.md` 全文 1035 行，检索式＝子串宽匹配）**：`半年` 0 命中；`待释放` 0 命中；`最高权限的管理者` 0 命中；`清算` 1 命中且在 L900（退租押金清算，与酒吧关闭分红清算无关）。被指的两个 anchor 分别是 §4A「营业日与中国税法属期（双轨）」与 §14「商户费用归集台账」，§15 是纯指针节（正文「分红引擎本体……另立独立 PRD」）。

更强的一条：BSM-0412 被登记为 FULL 的 finance-settlement §14 首句是「供持股分红净利润基数扣减（**净利 = 毛利 − 费用**）」，与 BSM L1733 的权威构成（收入 − 进货成本 − 人工成本 − 归集费用 − 各期税费）**实质冲突**——机检把一条口径相反的句子判成了 FULL 承接。

这八条规则的**实际承接在本文件**：BSM-0412→§2#1；BSM-0415/BEQ-0118→§2#3；BSM-0421→§5B；BSM-0422/0423→§6 职责分离 + §7；BEQ-0121→§9 发放保护线；BEQ-0122→§9 期间关账锁。

**应改成什么**：八行 `prd_path` 改为 `docs/dividend-engine-prd-v0.1.md`，`prd_anchor_or_line` 按上表对应节改写；BSM-0412 若需保留 finance-settlement 侧落点，应另立一行并把该处 anchor 指到 §14 的费用侧口径，同时对「净利 = 毛利 − 费用」单独登记冲突（该句归 FS shard 处置，见 §7）。

**依据**：BSM L1733/L1736/L1746/L1752/L1753、ENUM L290/L293/L294 现文 + 本 PRD 对应节现文。
**本轮不落地**：O-1 已把 `html-prd-coverage.csv` 列 PROTECTED、本轮零写入。

---

### R14-DE-04 · P1 · §2#2 / §3 / §5 代扣个税三处三口径

**现行原文逐字**
§2#2 L48–49：「**个人股东**（自然人真实持股）→ 税目 = 股息红利，**按方案税率代扣（默认 20%，税率可配）**」
§3 L66–67：「代扣税率 `tax_rate_bps`（默认 2000，公司股东单强制生效 0）」
§5 L140–141：「生效税率 eff_tax_rate = 主理人类型=company ? 0 : tax_rate_bps」／「代扣个税 tax = 按发放时有效的国家法律、纳税主体身份与交易实质，依据正式税务申报规则、专业财税软件回传、主管税务机关可核验结果，或经授权人员手工填写并通过审批的可核验数据计算……**本 PRD 不硬编码截断或无条件 HALF_UP**。」

**问题**：`76213531` 把 §5 的原算式 `tax = principal_gross × eff_tax_rate / 10000` 整条删除，但保留了紧邻上一行的 `eff_tax_rate` 定义，也没有同步 §2#2 与 §3。结果：（a）`eff_tax_rate` 被计算并快照，却不再被任何算式引用——只剩 `chk_ds_company_no_tax` 用到「公司股东税额为 0」这一分支；（b）读者面对三种互斥说法（按方案税率代扣默认 20% / `tax_rate_bps`=2000 / 不得由参数推定、按法定确定），无法判定一张单的税额到底怎么来。这是资金链上的口径不确定。

**应改成什么**：三处收敛为一套表述——保留 `tax_rate_bps` 作为 HTML 明文要求的可配参数（含上下限，见 R14-DE-05），并明确它的作用域是「估算展示与公司股东 0 值硬保证」，最终税额一律按 §5 法定输入确定；§2#2 的「按方案税率代扣（默认 20%，税率可配）」改为与 §5 一致的表述，§3 字段说明同步标注该参数不直接产出最终税额。

**依据**：BSM「分红税务按发放时有效的中国税法、股东身份和交易实质处理」条 L1735（「税率以参数形式配置，不在业务页面硬编码」+「出厂默认 20%」）、ENUM「分红税务」条 L289 同文、`DEC-R8-92`（其射程仅覆盖「统一向下截断／保持截断」税务子句，**未废除税率参数化与 20% 出厂默认**）。

---

### R14-DE-05 · P1 · §2#2 与 §4A#4 的税率调整审批链互斥

**现行原文逐字**
§2#2 L51 末：「落地仍须走三份 HTML 明定的**财务／税务／法务三审 ＋ 顶层平台公司最终确认**。」
§4A#4 L110：「**比例/税率及其他钱类方案调整**：必须新建草案版本，**由最高权限审批且提交人不得自审**，并指定生效起始日……」

同一动作（改税率）两条不同的审批链，PRD 未说明二者是并列必经还是择一，也未在 §3 / §6 / §8 的任何流程中出现「三审」环节。HTML 侧（BSM L1735 / ENUM L289）明文只有三审 + 顶层平台公司最终确认这一条，且把税率层级标为**安全阈值**（`DEC-R8-82` ①）而非普通业务参数——安全阈值的审批要求高于普通方案版本审批。

**应改成什么**：§4A#4 对**税率**这一项补例外指向 §2#2 的三审 + 顶层平台公司最终确认链，并在 §3 字段行、§6 审批流、§8 页面同步补该链；`tax_rate_bps` 的**上下限配置**同批补齐（§2#2 现文已自标为「待补项」，但 §3 字段清单里既无 `tax_rate_min_bps` 也无 `tax_rate_max_bps`）。

**依据**：BSM L1735 与 ENUM L289 逐字「安全阈值，全平台统一可配，调整须经财务、税务、法务三审并由顶层平台公司最终确认，出厂默认 20%，**须同时配置上下限**」；`DEC-R8-82` ①。

---

### R14-DE-07 · P1 · §5B 付账日镜像句漂移

**现行原文逐字（§5B L171）**：
> **发放走应付款项，落在三个付账日之一**（**每月 5 / 15 / 25 日**，遇**法定节假日或非银行工作日顺延至下一个银行工作日**（矛盾16，2026-08-06 统一口径）；见 `finance-settlement-product-prd-v0.1.md` §4B），**具体哪一个由该公司在分红周期配置中指定**。

**权威与镜像现文**
BSM L822：「付账日为**业务参数，全平台统一可配，初始为**每月 5 日、15 日、25 日」。
`finance-settlement-product-prd-v0.1.md` §4B.1：「每月有**三个付账日**（**业务参数，全平台统一可配，初始为 5 日、15 日、25 日**）」——该文件 2026-08-11 变更日志明载：「§4B.3（GOV-MISS-004）：**删除三个付账日固定为 5／15／25 的旧传播**，改为当前生效的三个可配置付账日」。

dividend-engine 未随该轮同步，仍把三个日期写成固定值。逐字比对差异点：缺「业务参数 / 全平台统一可配 / 初始为」三个限定。

**应改成什么**：括注改为「三个付账日为业务参数、全平台统一可配，初始为每月 5 日、15 日、25 日，遇法定节假日或非银行工作日顺延至下一个银行工作日」。

**依据**：BSM「积分结算账期」条 L822 现文；镜像目标 `finance-settlement-product-prd-v0.1.md` §4B.1；GOV-MISS-004 已落地的同口径修订。

---

### R14-DE-09 · P1 · §9 亏损结转存量回填「不回溯」

**现行原文逐字（§9 L272）**：
> 存量单以 `after = GREATEST(−净利,0)` 回填满足无条件 CHECK（V303 前无结转链，首个开启期从最近一张已结单 after 起接续，**历史多期真累计不回溯——设计边界**）。

**权威原文**：BSM「亏损结转」条 L1739：「亏损结转：以前亏的钱**先累计着**，等以后哪期盈利了，**先把往期亏损补平、再分红**。这个开关只控制「本期要不要扣往期亏损」，关掉它不会清空已经累计的往期亏损。」ENUM L292 同义。

HTML 对累计范围无任何时间起点限制。「历史多期真累计不回溯」把 V303 之前的往期亏损一次性归零，直接抬高其后每期的 `distributable`，多分出去的部分不可逆（分红 `paid` 即对外付款）。全文与决策台账检索该「设计边界」无 HTML 条目名或 DEC 编号支撑（宽正则检索 `ledger.md` 2239 行：`亏损`/`结转`/`回溯` 均未命中相关裁决）。

**应改成什么**：删除「历史多期真累计不回溯——设计边界」这一表述，改为「往期亏损累计按 BSM「亏损结转」条无时间起点限制；V303 前存量单缺结转链属实现缺口，回填口径与追溯范围另行裁定，回填前不得据现状认为累计已完整」。

**依据**：BSM「亏损结转」条 L1739 逐字。
**回填范围与数据来源属推断，见 §5 推断补写清单第 3 条。**

---

### R14-DE-11 · P2 · §9 期间关账锁引用不存在的判定函数

**现行原文逐字（§9 L304–305）**：「判定纯函数见 `DividendPeriodLock`，SQL 索引 V302 `ix_dividend_statements_lock`。」

**`NEGATIVE_SEARCH`**：检索面＝`ff2037a1` 全树（`git grep -l 'DividendPeriodLock' ff2037a1`），命中 3 个文件，**全部是文档**：`docs/dividend-engine-prd-v0.1.md`、`docs/finance-settlement-product-prd-v0.1.md`、`docs/pending-development-backlog-v0.1.md`。`backend/**` 与 `apps/**` 零命中；`backend/src/main/java/com/complex/platform/dividend/` 全部 13 个文件已列出，无该类。

关账能力本身存在（`DIVIDEND_PERIOD_LOCKED` 在 `MerchantExpenseService.java`；`ix_dividend_statements_lock` 在 `V302__dividend_period_lock_index.sql`；端点在 `MerchantExpenseController`），只是不以该名称承载。

**应改成什么**：删除对 `DividendPeriodLock` 的引用，改指实际承载点（`MerchantExpenseService` 的 `DIVIDEND_PERIOD_LOCKED` 判定 + `V302 ix_dividend_statements_lock`）。

**依据**：本 PRD 头部锚点纪律（L3「以条目名为主锚点」同理适用于实现引用须可解析）；无 HTML 依据，属文档准确性。
（旁证，不属本 shard 分母：`pending-development-backlog-v0.1.md` L85 把 `DividendPeriodLock` 列在「✅ 已完成」的 20 文件清单里，同为失效声明。）

---

### R14-DE-12 · P2 · §5 引用了不存在的 PGM 条目名

**现行原文逐字（§5 L146）**：「PGM「**金额一律精确到分**」是一般金额规则；股息红利个税依 PGM「原则之间的仲裁顺序」和 BSM「税务处理」优先按……」（同一条目名亦出现在变更日志 L328 的依据列）

**`NEGATIVE_SEARCH`**：检索面＝三份 HTML 全文（BSM 2018 行 / ENUM 299 行 / PGM 975 行，标签剥离后按行子串匹配），检索式＝`金额一律精确到分` → **零命中**。`精确到分` 仅命中 BSM L1060 与 ENUM L104（均为「应收金额精确到分收取」，讲现金收款不抹零）。PGM 实际条目为 L301「**全局原则 · 金额精度与税务例外**」；BSM 侧对应条为 L1716「**金额精度与尾差**」。另两处引用「PGM「原则之间的仲裁顺序」」（PGM L303）与「BSM「税务处理」」（BSM L1719）核对**存在且正确**。

**应改成什么**：「PGM「金额一律精确到分」」改为「PGM「全局原则 · 金额精度与税务例外」条」（或 BSM「金额精度与尾差」条），两处（§5 L146、变更日志 L328）同改。

**依据**：PGM L301 现文条目名。

---

### R14-DE-13 · P2 · §4 预览单适用范围窄于 HTML

**现行原文逐字（§4 L87–88）**：「official 必须匹配方案周期类型；**preview 仅对季度方案出**（月度方案的正式单本身就是月度）。」（§9 L293 同：「对每个生效中**季度方案**自动出上月 preview 单」）

**权威原文**：BSM L1736 与 ENUM L290/L291：「**每月另出一张只读的预览单**，让主理人提前看到当月大致情况（预览只能看，不参与审批、不发放）」——无方案周期类型的前置条件；而同条首句是「**分红原则上每半年分配一次**」，本 PRD §2#3 已承接并把半年周期登记为待改造缺口。

**结果**：权威周期（半年）落地之后，按现文既非季度方案也非月度方案，**不会产生任何预览单**，与 HTML 无条件要求相悖。

**应改成什么**：「preview 仅对季度方案出」改为「除正式单本身即为月度的方案外，一律按月出只读预览单（含半年、季度及其他月整数倍周期方案）」；§9 定时/事件驱动预览的候选集同步由「生效中季度方案」改为「正式单周期粒度大于月的全部生效中方案」。

**依据**：BSM L1736、ENUM L290/L291 现文。

---

### R14-DE-14 · P2 · §9 发放额恒等式内含越权实现项

**现行原文逐字（§9 L286）**：「发放额恒等（V309 后含自定义扣除）`payout_cent = principal_net_cent − arrears_offset_cent − custom_deduction_cent − reserve_withheld_cent`（均非负、抵扣不超税后应得）由 `chk_ds_payout_identity`（V309 重写）……硬保证」

§5A 抬头已把整节定性为**越权实现、生产禁用建议**，理由是 BSM L1737「扣除项的种类以本文件列明的三项为准……不得在后台自行增删」。但 §9 与 §5 L152 的**正文规范恒等式**仍把 `custom_deduction_cent` 作为常规项列出，读者按 §9 实施即恢复越权能力。第一性原理问「这一项为什么存在于规范链」——business rationale 出处为**无**（HTML 明确禁止其存在）。

**应改成什么**：§9 与 §5 L152 的主链恒等式回到 V303 形态（`payout_cent = principal_net_cent − arrears_offset_cent − reserve_withheld_cent`），`custom_deduction_cent` 及 `chk_ds_custom_deduction_range` 只在 §5A 留档块内出现并标注「越权实现、生产禁用、恒为 0」。

**依据**：BSM「三个可选的扣除开关」条 L1737 逐字；ENUM「可选扣除项」条 L292 同文。

---

### R14-DE-15 · P2 · 变更日志表结构破损

**现状（`ff2037a1` 实测）**：`## 变更日志` 表头在 L325（4 列）、分隔行 L326；L327–L338 为 4 列数据行；**L339 为空行**，把表劈成两张——L340–L352 的第二张表**没有表头行**，GFM 下不渲染为表格；其中 **L342–L350、L352 共 10 行为 5 列**（日期 / 类型 / 落点 / 内容 / 依据），与表头的 4 列不一致；L340/L341/L351 为 4 列。

`ff2037a1` 的提交说明写「A-05 finance / dividend 变更日志行改为 4 列格式并移到日期序表首」——实际只把 2026-08-19 那一行改为 4 列并上移，空行与 10 行 5 列结构未处理。另：表整体也非日期序（第一段降序到 2026-07-18，第二段又从 2026-07-28 起）。

**应改成什么**：删除 L339 空行合为一张表；L342–L350、L352 十行由 5 列并为 4 列（把「落点」列并入「内容」列首）；全表按日期降序重排。

**依据**：本文件变更日志表头（4 列）＋ `ff2037a1` A-05 已确立的同一目标状态。无 HTML 依据，属文档结构完整性。

---

### R14-DE-16 · P2 · 标题版本号与文件名不一致

`3f3e30cd` 把首行由 `# 持股分红引擎 PRD v0.1（V299，2026-07-18 拍板落地）` 改为 `v0.2`，文件名仍为 `dividend-engine-prd-v0.1.md`，且全仓交叉引用（finance-settlement §15、platform-governance-prd §12C、coverage.csv 20 行的 `prd_path`、`document-scope-manifest.csv`）一律写 `-v0.1.md`。该 hunk 不在 `3f3e30cd` 提交说明列出的任何一项里（说明只提「钱路五份：……分红净利润基数」）。

**应改成什么**：标题版本号改回 `v0.1`（与文件名及全部引用一致），或按项目既有改版流程另立 `-v0.2.md` 并同步全部引用；不得二者并存。

**依据**：与 O-7 已登记的 acceptance-spec 版本漂移（文内 V0.3 / 文件名 v0.1）同一模式。

---

## 4. B / C / D 明细

### R14-DE-06 · C 类（需改 HTML）· P1 · ENUM「可选扣除项」L292

**现行 HTML 原文逐字（ENUM L292，节选相关句）**：
> 可选扣除项亏损结转（往期亏损先补平再分，开关只控本期是否扣、关闭不清累计）、**欠款抵扣（先抵主理人对平台未清欠款）**、发放保护线（按比例/固定额先留存不发）；……

**拟改逐句**：
> 欠款抵扣（**按有效协议先抵主理人本人对相应法律主体尚未结清的欠款**）

**依据与胜出证据链**：
- BSM「欠款抵扣」条 L1740 现文：「欠款抵扣：主理人这期该得的钱，**按有效协议先抵其对相应法律主体尚未结清的欠款**，抵完剩下的才发。」
- `git log -S'欠款抵扣：主理人这期该得的钱' -- <BSM>` → `5d8b5232`（2026-07-20）；`git show f4f323bb^:<BSM>` 的旧文为「先拿去抵掉他对**平台**还没结清的欠款」；`git log -S'先抵其对相应法律主体尚未结清的欠款' -- <BSM>` → `f4f323bb`（2026-08-01，`align authoritative business html rules`）。即 BSM 于 2026-08-01 **定向修改**了债权人口径。
- `git log -S'先抵主理人对平台未清欠款' -- <ENUM>` → 仅 `5d8b5232`（2026-07-20），此后未变；该行虽在 `87534751`（2026-08-05）被触碰（追加「分红前置」句），债权人子句未同步。
- 决策台账宽正则检索 `欠款抵扣` → **0 命中**（2239 行全文），无 DEC 对该分歧作过裁定。
- PRD 侧已按 BSM 承接（§9 L273–274 + §6 L229 + 验收 #5），`76213531` 的 MNY-MISS-002 即此。

**影响**：口径差为实钱——主理人欠**城市项目公司**（非平台）的款项，按 BSM 可抵、按 ENUM 不可抵；抵与不抵直接决定该期实发额。

**推断单列**：BSM「相应法律主体」的具体范围（是否含关联方、同一集团内其他主体）HTML 未定义，本条草案**不作扩展**，只做镜像句补齐。

**⚠ 给 Consolidator 的分歧提示（本 shard 不选边）**：我按「更晚的定向修改 + PRD/DEC 已按 BSM 落地」判为 C（唯一胜出证据＝BSM 2026-08-01 的定向修订）。反向读法是：ENUM 该行整行的最后触碰时间（2026-08-05）晚于 BSM 的 2026-08-01，若 Consolidator 认为整行触碰时间即生效时间，则不存在唯一胜出证据，本条应改判 **B 类，`change_target=HTML`**，不得择一写进结论。两侧证据已并列于上。

---

### R14-DE-08 · B 类 · P1 · §9 发放保护线「由大股东决定」在 PRD 无落点

**五条件自证（request.md §5.2）**
① 属业务政策与权责：留存比例（可至 100%）的决定权归属，直接决定主理人当期能否拿到钱。②§4.3 取证已完成：见下。③ 现有权威来源无唯一答案：HTML 只给了「由大股东决定」六个字，未定义大股东的判定方法、平票处理、与平台审批链的关系。④ 存在多个合理方案：见最小决策面。⑤ 用户回答会创造新业务事实（谁有权按下这个开关），不是帮代理找文件。

**取证**
- BSM「发放保护线」条 L1741 逐字：「按比例或固定额先留一部分不发（留存），**留存比例最高可至百分之百，由大股东决定**，保护该酒吧公司的资金安全线。」
- `NEGATIVE_SEARCH`：`大股东` 在 ENUM（299 行）与 PGM（975 行）**零命中**，仅 BSM L1741 一处。
- PRD §9 L277 逐字承接了该句，但 §3 L81 与 §4A#4 L110 把**包含 `payout_floor_bps` 在内的全部方案调整**统一路由到「由最高权限审批且提交人不得自审」；§7 权限清单无「大股东」角色；全文检索 `大股东` 仅 1 处（§9 L277 的引句本身）。
- 冲突的现实性：BSM L1728/L1734 明定「实际以每家酒吧的工商登记比例为准」，80/20 只是新酒吧默认建议——因此**大股东可以是主理人本人**，而 PRD 现行链条把该决定权完全交给平台侧最高权限。

**两处查证结果**：决策台账宽正则 `大股东` / `保护线` → 相关裁决仅 `DEC-R4-2026-08-04-63`（「发放保护线留存比例最高可至百分之百、由大股东决定」，即 HTML 同文，未定义大股东）；canonical findings 238 行无对应条目；`docs/prd-audit/rounds/r11/implementation-gaps.csv` 73 行零分红条目。

**最小决策面**
- 现有证据：HTML 只写「由大股东决定」；PRD 现行把决定权给平台最高权限。
- HTML 明确到哪：留存比例上限 100%、决定主体名为「大股东」、目的是「保护该酒吧公司的资金安全线」。
- 未明确什么：大股东的判定口径（按工商登记持股比例的最大者？须过 50%？并列时怎么办）；大股东意思表示的形式与留痕；与平台方案审批链是并列必经、还是大股东决定后平台仅作登记。
- 候选方案与影响：
  **甲** 大股东＝工商登记比例最大股东，其书面同意为设置/调整 `payout_floor_bps` 的前置，平台最高权限仅审版本合规 —— 最贴 HTML 字面；主理人为大股东的酒吧，平台将失去单方设定保护线的能力。
  **乙** 维持平台最高权限决定，「由大股东决定」解释为「须经大股东知会/无异议」 —— 改动最小；实质弱化 HTML 明文的「决定」二字。
  **丙** 双签：大股东同意 + 平台最高权限审批，任一缺失不生效 —— 最保守；流程成本最高，且未解决并列持股的平票问题。
- 影响范围：§3 字段与审批流、§4A#4、§6、§7 权限清单、§8 页面。

---

### D 类

**R14-DE-17 · P2 · `DEC-R8-92` 的用户批准无独立真身**

已跑完的取证链：
- `git show ff2037a1:docs/prd-audit/business-decision-ledger.md` L1951–L1965 全块读毕。该 DEC 的「依据」列写「用户 2026-08-10 明确裁决"税务属于例外，一律按国家法律进行"」，**未引用任何批准记录文件**；块首「来源」段同样只自述。
- 宽正则检索台账 2239 行的 `DEC-R8-92` → 1 处（即本块），无后继补充块、无更正块。
- `76213531` 的 commit message 与 PRD 变更日志 L328 依据列引用的是 `batch-02-spec-review-v1.00 I-01～I-06`、`batch-02-spec-review-v1.01 N-I-01／N-M-01／N-M-02`。

**还差什么**：这两份 `batch-02-spec-review-v1.0x` 是否在仓内存在、是否含用户逐条批准痕迹，未检索。
**下一步查哪里**：`git log --all --diff-filter=A -- '*batch-02-spec-review*'`、全仓 `find -iname '*batch-02-spec-review*'`、`.scratch/` 与 `docs/prd-audit/rounds/**`。

**边界说明**：`DEC-R8-92` 明文「本裁决**不要求修改 HTML**」，且 `76213531` 实测未触碰三份 HTML（该 commit 只改 3 个文件，见 §6），故任务书 §1 第 2 问（HTML 改动是否有逐句批准）**对本条不适用**。本条与任务包点名的 `DEC-R8-87~91`「最终审批记录 v1.01」/ `DEC-R8-93`「final-report-v1.02.md」零命中问题属同一模式，但那两组的定性归 b05 相关 Auditor，本 shard 不代为结论、不重复登记。

---

## 5. 推断补写清单（HTML 未写明、PRD 需补的判定方法/边界/兜底/时限/上限/留痕 —— 均属 B，不入 A）

**1. 到期借款本息清偿的算式化边界（配套 R14-DE-02）**
HTML 只写「当期可分配利润必须先偿还全部到期借款本息，剩余部分才可用于分红」。未写明：
- 清偿项在链条中的确切位置——是在 `net_profit → distributable` 之间（即先于亏损结转），还是在 `distributable` 之后、劈分之前？两种放法对「亏损结转 applied 的基数」给出不同结果。代实数：净利 1,000,000、期初累计亏损 100,000、到期本息 300,000 —— 先清偿再结转得 `applied`=min(100,000, 700,000)=100,000、可分配 600,000；先结转再清偿得 `applied`=100,000、可分配 600,000（本例同值），但当到期本息 > 净利时（如本息 1,200,000）两法分歧：前者 `distributable`=0 且累计亏损不动，后者 `applied`=100,000 被消耗而实际一分未分——**开关语义被静默改变**。
- 到期本息 > 当期可分配利润时的兜底：欠额如何结转下期？是否阻断本期出单？
- 清偿动作与分红单的关联留痕：是否落 `dividend_statement_lines`、是否进 `ar_ap_items`、是否纳入 `chk_ds_*` 恒等 CHECK。
- 「到期」判定时点（提交时 / 期末 / 审批时）与「借款本息」的取数口径（登记于哪张表、按哪个主体维度归集）——后半段与 `GAP-41` 的实现侧取数缺口重叠，引用不重列。
- 与 §6 提交闸的关系：清偿完成后本期是否即可提交（提交闸解除条件）。

**2. 欠款抵扣的「既定先后顺序」（配套 §9 L274）**
PRD 现文：「足额时按有效协议和**既定先后顺序**抵扣，剩余发放。」BSM L1740 只写「按有效协议先抵……抵完剩下的才发」，**未定义多笔欠款之间的抵扣顺序**。未写明：多笔合格欠款并存时按先欠先抵 / 按到期日 / 按利率 / 按协议指定；协议之间冲突时的仲裁；部分抵扣时的分摊与留痕。原实现口径（`76213531` 删掉的旧文）是「对应收先欠先抵逐项结算」，删除后新文未给替代定义。

**3. 亏损结转历史累计的回填范围（配套 R14-DE-09）**
HTML 未写明累计的时间起点、V303 之前无结转链期间的累计如何重建、重建所依据的历史单口径版本、以及重建是否追溯已 `approved/paid` 的单。

**4. 税务法定输入闸的状态门禁与界面文案（配套 DE-S1，见 §6）**
BSM「税务处理」条 L1719 明文支持的部分：涉税金额精度/舍入按法定、税率税目不写死、**三阶段金额分别保存互不覆盖**（系统计算/实际申报/实际缴纳）。**HTML 未写明、PRD 自行补入**的部分：
- 状态门禁：「不得生成最终税额、不得审批通过、不得形成应付、不得标记已支付」「税务闸本身不得阻断 `draft → pending_review`」「`pending_review → approved` 才硬校验」
- 界面与单据文案：「待税务确认」「非正式估算，不可申报、不可发放」两处硬性文案
- 「实际申报与实际缴税结果只在对应事实发生后追加关联，禁止预填未来结果」的写入时序约束
- 「经授权人员手工填写并通过审批的可核验数据」这一**兜底输入通道**及其授权与审批要求
以上五项均为判定方法/门禁/文案/兜底/留痕，HTML 无对应明文，`DEC-R8-92` 文本亦未涉及。PRD 验收 §3 末句已自认「是……PRD 产品收口，**不表示用户逐项裁决了实现流程**」——按 request.md §5.1 硬边界，应逐条经用户裁决，不得以 A 类落地。

**5. 大股东的判定与授权形式**（内容见 R14-DE-08 最小决策面，不重复）

---

## 6. R13 追溯表（commit × hunk × 追溯结论）

`git log --oneline 2bc7cb0f..ff2037a1 -- docs/dividend-engine-prd-v0.1.md` 实测 3 个 commit。

### 6.1 `76213531`（2026-08-10，PR #372，「修复 R13 第二批五项 P0」，改 3 文件：本 PRD +100/−51、决策台账 +15、coverage.csv ±6）

10 个 hunk，逐个：

| # | hunk | 改了什么 | 能否追溯 | 结论 |
|---|---|---|---|---|
| 1 | `@@ -5,7` | 状态行「凡涉数据皆可配」→「可配参数依三份 HTML 所定边界配置；成本/费用/税费扣减固定全量执行」 | ✅ BSM L1733「全部扣完、税后剩下的才是可分配的净利润」+ MNY-008 | 追溯成立 |
| 2 | `@@ -53,8`（**删除类**） | **删** §2#4 原文「**凡涉数据皆可配**：比例…、**基数构成（计入哪些收入域/是否扣成本/计入哪些费用类别）**、周期类型、税率、主理人类型…全部为后台可调配置」；改为「可配置边界…**不得配置关闭任何一类成本、费用或税费**」 | ✅ BSM L1733 要求全额扣除，原文允许「是否扣成本/计入哪些费用类别」可配 = 允许少扣，与 HTML 直接冲突 | **删对了**。被删内容为越权可配面 |
| 3 | `@@ -64,9` | §3 字段行同步固定化 + 新增「方案版本状态与实现缺口」块（`draft/pending_approval/approved/active/ended/rejected`） | ✅ MNY-009；BSM「业务策略一律可配置」条的版本化与生效日要求 | 追溯成立 |
| 4 | `@@ -76,8`（**删除类**） | **删**「调整=**直接改当前方案行**（审计事件留痕），历史分红单按单内配置快照追溯；停用走 status=ended」；改为强制新版本、最高权限审批、提交人不得自审、不追溯已确认或已发放分红 | ✅ BSM L1734 / ENUM L288「必须以获批登记版本生效，**修改不能追溯已经确认或发放的分红**」 | **删对了**。原地更新与「不追溯」不能共存 |
| 5 | `@@ -106,12`（**删除类**） | **删** §4A#4「比例/税改配置：改方案只影响新期间；历史单按单内快照」与 #5「须最高权限**或配置权限**」；改为新建草案版本 + 最高权限审批 + 「配置权限不自动取得该权力」；新增 §5「业务口径优先」块 | ✅ PGM「高危授权审批」；BSM L1753「有最高权限的管理者：审批分红单（提交单据的人不能审批自己提交的单）」。原文「或配置权限」是权限旁路 | **删对了** |
| 6 | `@@ -135,33`（**删除类，最大**） | **删** 33 行取整口径块，含「代扣个税 tax = `principal_gross × eff_tax_rate / 10000`（**整除截断·刻意保留**，对齐税局申报口径）」及「保持整除截断（不得改）」整段；换入「分红税务法定例外」五条 | ⚠ 部分。见下方 **DE-S1 专项** | **多改**（见 DE-S1） |
| 7 | `@@ -211,26` | §6 补「职责分离」与「税务法定输入闸」两块；`voided` 表格「谁可以作废」→「经办发起与审批」，把「最高权限审批人（`finance.dividend.manage` + 平台 scope）」改为「逐项授予经办权限的人发起 + 最高权限审批 + 发起人不得自审」 | ✅ 权限部分追溯 BSM L1753 + PGM「高危授权审批」；**税务输入闸的状态门禁与文案无 HTML 明文**（见 §5 推断补写第 4 条） | 权限成立；税闸多改 |
| 8 | `@@ -241,15`（**删除类**） | **删** §7「`finance.dividend.manage`（high）：方案配置/算单/审批/发放，全部要求平台 scope」与「V299 两码直授 `role_platform_admin`」；改为「必须逐项单独授予并留痕；拥有该权限不自动取得最终审批权」「不得因 role_platform_admin 默认授予分红审批」 | ✅ BSM L1753；PGM「高危授权审批」。原文把功能权限等同审批权 | **删对了** |
| 9 | `@@ -268,11`（**删除类**） | **删** §9 欠款抵扣旧口径「税后应得先抵扣**该商户对平台**未清应收（ar_ap direction=receivable）」整块（含 `applyDividendOffsetToReceivables` 预检与 `DIFFERENT` 原子性描述）；改为「主理人**本人**对**相应法律主体**」+「酒吧商户公司、其他股东或任何第三人的欠款不得并入」 | ✅ BSM L1740 现文（2026-08-01 起）；MNY-MISS-002；验收 #5 反例（商户欠 20,000 / 主理人无欠款不得抵） | **删对了**。⚠ 顺带删掉了「先欠先抵逐项结算」的顺序定义未给替代，见 §5 推断补写第 2 条 |
| 10 | `@@ -307,18`（**删除类**） | **删** §9「费用侧租金已默认计入…**可按方案关闭**；turnover_rent 历史行…需手工录入补齐」；改为「必须计入净利润扣减，**不得按方案关闭**」；新增「本批业务验收」5 条 + 变更日志行 | ✅ BSM L1733 归集费用含租金且须全额扣除 | **删对了** |

**该 commit 追溯结论**：10 个 hunk 中 8 个可完整追溯到 HTML 条目名或有效 DEC；hunk 6、7 存在超出裁决射程的补写（见 DE-S1）。7 个删除类 hunk 逐个复原比对，**6 个删对了**（被删内容均与 HTML 明文冲突），第 9 个删对但留下顺序定义空白。

### 6.2 专项子任务 **DE-S1** · `DEC-R8-92` 承接忠实度

**DEC 说了什么**（`business-decision-ledger.md` L1959 逐字要点）：
1. 税务事项属于法定例外，分红税务按发放时有效的国家法律、纳税主体身份和交易实质处理；
2. **PRD 不得把本场景硬编码为统一向下截断，也不得以无条件 `HALF_UP` 替代**；
3. 射程：**仅覆盖** `D-20260728-1` 与 `unified-standards-baseline-v0.1.md` D15.1「例外一」中"代扣个税统一向下截断／保持截断"的**税务子句**；旧行保留、不回写；**非税业务规则不受影响**；
4. 落点：本 PRD `§2／§5／§6／验收／变更日志` + coverage 现有 `BEQ-0167`、`BSM-0790` 行；**不改三份 HTML、代码、OpenAPI、schema 或 migration**；
5. 依据列自设边界：BSM「税务处理」「分红税务」与 PGM「原则之间的仲裁顺序」提供**下游数据、凭证及流程设计依据，但不冒充本条新增用户裁决**；
6. 射程与执行边界第 2 条：本轮只同步 canonical ledger、分红 PRD 与必要 coverage 现有行。

**PRD 改成了什么**（`76213531` hunk 6/7/10 + coverage 的 6 处改动）。

**逐项对表**：

| 维度 | 判定 |
|---|---|
| 删除「整除截断·刻意保留」及「保持整除截断（不得改）」 | ✅ **正好** —— 正是 DEC 第 2 条要求 |
| 不引入无条件 `HALF_UP`（§5 明写「也不硬编码无条件 HALF_UP」「不得从其他模块实现、一般金额原则或申报表字段格式反向推定」） | ✅ **正好** |
| 非税取整（平台分成 / 留存比例额 / 自定义扣除比例原始额继续 `MoneyRounding.bpsOf`／`HALF_UP`；准备金释放向下取整）原样保留 | ✅ **正好** —— DEC 第 3 条「非税业务规则不受影响」 |
| 未改三份 HTML / 代码 / OpenAPI / schema / migration | ✅ **正好** —— `76213531` 只改 3 个文件（本 PRD、决策台账、coverage.csv），HTML 与 `backend/**` 零触碰 |
| 薪资域个税税表整除链（`JdbcStaffRepository` V217） | ✅ 未被本 commit 触碰，符合 DEC 射程 |
| **连同税额算式主体一并删除**：原式 `tax = principal_gross × eff_tax_rate / 10000` 被整条删掉，只留 `eff_tax_rate` 定义悬空 | ❌ **多改**。DEC 覆盖的是**舍入方式子句**（"统一向下截断／保持截断"），不是税率参数化算式本身。BSM L1735 与 ENUM L289 现文仍明定「税率以参数形式配置」「出厂默认 20%，须同时配置上下限」，`DEC-R8-92` 未废除之 —— 见 R14-DE-04 |
| **新增税务法定输入闸的状态门禁**（不得生成最终税额 / 不得审批通过 / 不得形成应付 / 不得标记已支付；`draft → pending_review` 不阻断；`pending_review → approved` 才硬校验；未形成合规应付不得 `paid`） | ❌ **多改**。DEC 文本零涉状态机；BSM L1719「税务处理」条只写精度/舍入/税率不写死/三阶段保存/差异机制，无任何审批或状态门禁语句 —— 属推断补写，见 §5 第 4 条 |
| **新增界面与单据硬性文案**（「待税务确认」「非正式估算，不可申报、不可发放」） | ❌ **多改**，同上 |
| **新增「经授权人员手工填写并通过审批的可核验数据」兜底输入通道** | ❌ **多改**。BSM L1719 只承认「专业财税或开票系统依法形成的结果」作为依据，未授权人工填写通道 |
| 三阶段金额分别保存互不覆盖 / 禁止预填未来结果 | ⚠ **有独立 HTML 依据**（BSM L1719 逐字「税费金额分三个阶段分别保存、互不覆盖：系统计算金额、实际申报金额、实际缴纳金额」），**不是**由 `DEC-R8-92` 授权 —— 承接成立但依据标注应指向 BSM「税务处理」条而非本 DEC |
| 新增「本批业务验收」5 条 | ⚠ 其中第 3 条覆盖税务，末句已自认「是 PRD 产品收口，**不表示用户逐项裁决了实现流程**」—— 诚实标注，但按 request.md §5.1 仍属需用户裁决的推断补写 |
| 落点范围 | ⚠ DEC 第 4 条只授权 `§2／§5／§6／验收／变更日志`。实测 `76213531` 另改了 **§3、§4、§4A、§7、§8、§9**——但这些改动分别归属 MNY-008/009/011/MISS-002，**各有独立依据**，不算 `DEC-R8-92` 的越界 |

**DE-S1 结论**：`DEC-R8-92` 的核心裁决（不得硬编码统一截断、不得无条件 HALF_UP、非税规则不动、不改 HTML/代码）**全部忠实承接，无少改**；但承接同时**多改了三处**——（i）删掉了 DEC 射程之外的税率参数化算式，使 `eff_tax_rate`/`tax_rate_bps` 悬空且与 §2#2/§3 三处口径打架（R14-DE-04）；（ii）新增了 HTML 与 DEC 均无明文的税务状态门禁与界面文案；（iii）新增了未获授权的人工填写兜底输入通道。三处均登记，(i) 入 A，(ii)(iii) 入推断补写清单第 4 条（属 B）。

### 6.3 `3f3e30cd`（2026-08-19，DEC-R8-98 下游同步 16 份文档）· 3 个 hunk

| # | hunk | 内容 | 追溯 |
|---|---|---|---|
| 1 | `@@ -1,4` | 标题 `v0.1` → `v0.2` | ❌ **追不到**。该 commit 说明中「钱路五份」只提「分红净利润基数」，DEC-R8-98-01~10 无版本号裁决；文件名与全仓引用未同步 → R14-DE-16 |
| 2 | `@@ -40,6` | §2#1 增「挂起与坏账不进净利润基数」段 | ✅ `DEC-R8-97-08`（台账 L2111）逐字可对；BSM「商户核销未收售价」条 |
| 3 | `@@ -346,4` | 变更日志追加对应行（表尾） | ✅ 同上 |

### 6.4 `ff2037a1`（2026-08-20，DEC-R8-98 技术审查 I-01~I-06 修复）· 2 个 hunk

| # | hunk | 内容 | 追溯 |
|---|---|---|---|
| 1 | `@@ -324,6` | 变更日志表首插入 2026-08-19 行（4 列格式） | ✅ 提交说明 A-05；但只修 1 行，未处理空行断表与 10 行 5 列 → R14-DE-15 |
| 2 | `@@ -348,5`（**删除类**） | **删**表尾原 `2026-08-19 DEC-R8-98` 行（5 列格式） | ✅ 与 hunk 1 为同一行的移动，内容未丢失，逐字比对两行仅列数与日期标签写法不同 → **删对了** |

**全批 R13/R13后 追溯小结**：15 个 hunk（10 + 3 + 2），13 个可追溯到 HTML 条目名或有效 DEC；1 个追不到（标题改版本号）；1 个（hunk 6/7 合计）为射程内正确修改 + 射程外多改。9 个删除类 hunk 全部复原被删内容并逐条判定，**8 个删对了**，1 个（标题）为无依据改写。**未因「已经在 main 上」默认任何一处为对。**

---

## 7. CODE-FOLLOWUP 候选

先查 `docs/prd-audit/rounds/r11/implementation-gaps.csv`（73 行基线）：全表按 `分红`/`dividend`/`留存`/`释放` 宽匹配 → **零命中**，故下列两条均非重复登记。

**R14-DE-10 · P1 · §5A 越权实现的「生产禁用」处置未落地**
- PRD 证据：§5A L182「**生产环境禁用**——**写入口下线**（方案新建/修改请求不再接受 `customDeductions`）**或强制空表**（配置层恒为 0 条），使 `custom_deduction_cent` 恒为 0」；BSM L1737 明定扣除项三项冻结、后台不得增删。
- 实施证据（`IMPLEMENTATION_INERT` + `WIRING_ACTIVE`）：`DividendService.java` L194 与 L270–272 —— `if (request.customDeductions() != null) { repository.replaceCustomDeductions(...) }`，可经 `POST /plans` 与 `PUT /plans/{id}` 两条已注册路由到达；`dividend_deduction_items` 子表（V309）在库；`chk_ds_custom_deduction_range` CHECK 在库。写入口既未下线也未强制空表。
- 归类：HTML = PRD ≠ Code 的 implementation gap。

**R14-DE-11 关联 · P2 · `DividendPeriodLock` 命名落点缺失**
- PRD 证据：§9 L304「判定纯函数见 `DividendPeriodLock`」。
- 实施证据（`NEGATIVE_SEARCH`）：`git grep -l 'DividendPeriodLock' ff2037a1` 仅命中 3 个 `docs/` 文件；`backend/src/main/java/com/complex/platform/dividend/` 13 文件清单中无该类。
- 归类：文档引用 ≠ 代码事实。本条主体已按 A 类登记（改 PRD 引用），此处仅作 CODE-FOLLOWUP 备选——若确认关账判定应当被抽为纯函数，则属代码轮事项；若否，仅改 PRD 即闭合。**倾向后者，不建议为此立代码项。**

**跨文件、归 FS shard 处置（本 shard 只提示，不代为登记）**：`finance-settlement-product-prd-v0.1.md` §14 首句「供持股分红净利润基数扣减（**净利 = 毛利 − 费用**）」与 BSM L1733 权威净利润构成（须扣人工成本与该期间各项税费）实质冲突，且该句正是 BSM-0412 被登记为 `FULL` 的落点（见 R14-DE-03）。请 Consolidator 与 FS shard 合并去重。

---

## 8. Evidence 附录

格式：`EV-DE-NN | 类型 | 命令 | exit @ SHA | ≤10 行节选`。所有 `git show`/`git grep`/`git log` 均以 `ff2037a1c1e40a97e5979a5517451c7bfe409d16` 为基线；管道处退出码取 `${PIPESTATUS[0]}`。

| EV | 类型 | 命令 | exit | 节选 |
|---|---|---|---|---|
| EV-DE-01 | `BASELINE_STATE` | `git rev-parse HEAD; git cat-file -t ff2037a1…` | 0 | `e3770efab6b2…` / `commit` —— 工作区 HEAD ≠ 基线，故全程 `git show` |
| EV-DE-02 | `BASELINE_STATE` | `git show ff2037a1:docs/dividend-engine-prd-v0.1.md \| wc -lc` | 0 | `352 69704` —— 与任务包一致 |
| EV-DE-03 | `BUSINESS_AUTHORITY` | `git show ff2037a1:docs/prd-audit/html-prd-coverage.csv` → `csv.DictReader(utf-8-sig)` 过滤 `dividend-engine` | 0 | `total 1234 / DE rows 20 / Counter({'FULL':19,'STALE':1}) / Counter({'BSM':14,'BEQ':6})` |
| EV-DE-04 | `BUSINESS_AUTHORITY` | ARL 20 行 `source_text` 与 HTML 实文逐行长度比对（BSM-0790/0424/0791/0419、BEQ-0120/0116/0167 抽验） | 0 | `BSM-0790 ledger 239 html_line 239 substr` … 7/7 全为完整逐字子串，**未发现 source_text 不全** |
| EV-DE-05 | `MIGRATION_DECLARED` | `git show ff2037a1:backend/src/main/resources/db/migration/V304__dividend_reserve_release.sql \| head -40` | 0 | `-- V304 分红发放保护线留存·释放机制（建设 R…）` / `reserve_release_mode: manual/next_period/fixed_delay/annual` / `CREATE TABLE dividend_reserve_releases` / `未释放（withheld）应付被支付守卫拦截` |
| EV-DE-06 | `WIRING_ACTIVE` | `git show ff2037a1:…/DividendController.java \| grep -nE 'Mapping\|release'` | 0 | `101: @GetMapping("/reserve-releases")` / `111: @PostMapping("/statements/{id}/release-reserve")` |
| EV-DE-07 | `WIRING_ACTIVE` | `git show ff2037a1:docs/openapi/openapi-p0.yaml \| grep -nE '/dividends/'` | 0 | `24641: /api/v1/finance/dividends/reserve-releases` / `24699: …/{id}/release-reserve` / `24711: x-audit-event-success: dividend.reserve_released` |
| EV-DE-08 | `BASELINE_STATE` | `git log --diff-filter=A -- …/V304__…sql …/DividendReserveReleaseJob.java` | 0 | `78057acc\|2026-07-20\|建设R: 分红发放保护线留存·释放机制(V304) (#153)` —— **早于 §9 缺口块写入日 2026-08-04 十五天** |
| EV-DE-09 | `BUSINESS_AUTHORITY` | BSM 标签剥离后取 L1722–L1762 | 0 | `1744 \| 分红前置：存在逾期未还借款的，分红一律优先偿还借款——当期可分配利润必须先偿还全部到期借款本息，剩余部分才可用于分红…` |
| EV-DE-10 | `BUSINESS_AUTHORITY` | PGM 标签剥离后子串 `清偿` | 0 | `435 \| …酒吧当期可分配利润必须先偿还全部到期借款本息，剩余部分才可用于分红，存在逾期未还借款的一律不得分红…` |
| EV-DE-11 | `BUSINESS_AUTHORITY` | `git show ff2037a1:docs/prd-audit/business-decision-ledger.md` 子串 `分红前置` | 0 | `1348 \| DEC-R7-21 \| 2026-08-05 \| **分红前置：借款清偿优先于分红**…` / `1390 \| DEC-R7-41 … 新增 GAP-41…实现侧零落点` |
| EV-DE-12 | `BUSINESS_AUTHORITY` | `git show ff2037a1:docs/platform-governance-prd-v0.1.md \| grep -nE '借款\|清偿'` | 0 | `736: - **借款清偿优先于分红**：…该口径与 \`dividend-engine\` 分红基数计算必须一致。` |
| EV-DE-13 | `BUSINESS_AUTHORITY` | ARL×coverage 联查，纳入 BSM L1727–1760 + ENUM L285–294（CC-2） | 0 | `分红域规则总数 28 / 指向 dividend-engine 18 / 指向其他 PRD 10 / 无 coverage 行 0`，10 条明细见 §3 R14-DE-03 表 |
| EV-DE-14 | `NEGATIVE_SEARCH` | 检索面＝`git show ff2037a1:docs/finance-settlement-product-prd-v0.1.md`（1035 行）；检索式＝子串 `半年` / `待释放` / `最高权限的管理者` / `清算` | 0 | `半年 hits 0 []` / `待释放 hits 0 []` / `最高权限的管理者 hits 0 []` / `清算 hits 1 [900]`（退租押金，非酒吧关闭分红清算）；另 `972 \| …供持股分红净利润基数扣减（净利 = 毛利 − 费用）` |
| EV-DE-15 | `BASELINE_STATE` | `git show 76213531 -- docs/dividend-engine-prd-v0.1.md`，hunk `@@ -135,33 +136,29 @@` | 0 | `-代扣个税 tax = principal_gross × eff_tax_rate / 10000 （**整除截断·刻意保留**…）` / `+代扣个税 tax = 按发放时有效的国家法律…本 PRD 不硬编码截断或无条件 HALF_UP。`；上一行 `生效税率 eff_tax_rate = …` 保留 |
| EV-DE-16 | `BUSINESS_AUTHORITY` | ARL `BSM-0414` / `BEQ-0117` `source_text`；PRD §2#2 L51 与 §4A#4 L110 | 0 | HTML：`…调整须经财务、税务、法务三审并由顶层平台公司最终确认，出厂默认 20%，须同时配置上下限…`；PRD §4A#4：`必须新建草案版本，由最高权限审批且提交人不得自审` |
| EV-DE-17 | `EVIDENCE_CONFLICT` | BSM L1740 与 ENUM L292 标签剥离取文 | 0 | BSM：`欠款抵扣：主理人这期该得的钱，按有效协议先抵其对相应法律主体尚未结清的欠款…`；ENUM：`…欠款抵扣（先抵主理人对平台未清欠款）…` |
| EV-DE-18 | `HISTORICAL` | `git log -S'先抵主理人对平台未清欠款' -- <ENUM>`；`git log -S'先抵其对相应法律主体尚未结清的欠款' -- <BSM>`；`git show f4f323bb^:<BSM> \| grep -oE '欠款抵扣[^<]{0,90}'` | 0 | ENUM：`5d8b5232\|2026-07-20`（此后未改该子句）；BSM：`f4f323bb\|2026-08-01\|align authoritative business html rules`；BSM 旧文：`欠款抵扣：主理人这期该得的钱，先拿去抵掉他对平台还没结清的欠款，抵完剩下的才发。` |
| EV-DE-19 | `BUSINESS_AUTHORITY` | BSM 子串 `付账日` → L822；`git show ff2037a1:docs/finance-settlement-product-prd-v0.1.md` §4B.1（L308–326） | 0 | BSM L822：`…付账日为业务参数，全平台统一可配，初始为每月 5 日、15 日、25 日…`；FS §4B.1：`每月有**三个付账日**（**业务参数，全平台统一可配，初始为 5 日、15 日、25 日**）`；FS 变更日志 2026-08-11：`删除三个付账日固定为 5／15／25的旧传播` |
| EV-DE-20 | `NEGATIVE_SEARCH` | 检索面＝三份 HTML 全文标签剥离；检索式＝子串 `大股东` | 0 | `bsm.html 大股东 -> 1`（L1741）；`enum.html 大股东 -> 0`；`pgm.html 大股东 -> 0`。PRD 全文 `大股东` 亦仅 1 处（§9 L277 引句） |
| EV-DE-21 | `BUSINESS_AUTHORITY` | ARL `BSM-0416` `source_text` vs PRD §9 L272 | 0 | HTML：`亏损结转：以前亏的钱先累计着…关掉它不会清空已经累计的往期亏损。`；PRD：`…历史多期真累计不回溯——设计边界` |
| EV-DE-22 | `WIRING_ACTIVE` | `git grep -n 'customDeductions' ff2037a1 -- backend/src/main/java` | 0 | `DividendService.java:194: repository.replaceCustomDeductions(plan.id(), parseCustomDeductions(...))` / `:271: if (request.customDeductions() != null) {` |
| EV-DE-23 | `NEGATIVE_SEARCH` | 检索面＝`ff2037a1` 全树；检索式＝`git grep -l 'DividendPeriodLock' ff2037a1` | 0 | 3 命中，全为 `docs/`：dividend-engine PRD、finance-settlement PRD、pending-development-backlog。`git ls-tree -r ff2037a1 backend/…/dividend/` 13 文件无该类 |
| EV-DE-24 | `NEGATIVE_SEARCH` | 检索面＝三份 HTML 全文标签剥离；检索式＝子串 `金额一律精确到分`（0）、`到分`（BSM 3 / ENUM 2 / PGM 1） | 0 | `pgm.html 到分 -> 1` → `301 \| 全局原则 · 金额精度与税务例外：…` ；`金额一律精确到分` 全零 |
| EV-DE-25 | `BUSINESS_AUTHORITY` | BSM L1736；ENUM L290/L291；PRD §4 L87 / §9 L293 | 0 | BSM：`…每月另出一张只读的预览单…`（无周期类型条件）；PRD：`preview 仅对季度方案出` |
| EV-DE-26 | `BUSINESS_AUTHORITY` | ARL `BSM-0419` `source_text` vs PRD §9 L286 / §5 L152 | 0 | HTML：`…扣除项的种类以本文件列明的三项为准…不得在后台自行增删：`；PRD 主链恒等式含 `custom_deduction_cent` |
| EV-DE-27 | `BASELINE_STATE` | 逐行解析 `de.md` L323–L353 的表格列数 | 0 | `339 <BLANK>` / `342–350、352 cols=5` / 表头 `325 cols=4` |
| EV-DE-28 | `BASELINE_STATE` | `git show 3f3e30cd -- docs/dividend-engine-prd-v0.1.md`，hunk `@@ -1,4 +1,4 @@` | 0 | `-# 持股分红引擎 PRD v0.1（V299…）` / `+# 持股分红引擎 PRD v0.2（V299…）`；文件路径仍为 `docs/dividend-engine-prd-v0.1.md` |
| EV-DE-29 | `HISTORICAL` | `git show ff2037a1:docs/prd-audit/business-decision-ledger.md` L1951–L1965 | 0 | `本块只追加…三份业务 HTML 已有法定优先与税务处理规则，本裁决不要求修改 HTML。` / 依据列：`用户 2026-08-10 明确裁决"税务属于例外，一律按国家法律进行"`（无文件引用） |
| EV-DE-30 | `BASELINE_STATE` | `git show 76213531 --stat` | 0 | 3 文件：`docs/dividend-engine-prd-v0.1.md 100 ++/--`、`business-decision-ledger.md 15+`、`html-prd-coverage.csv 6 ±` —— **HTML 与 backend 零触碰**，符合 DEC-R8-92 第 4 条 |
| EV-DE-31 | `BASELINE_STATE` | `git log --oneline 2bc7cb0f..ff2037a1 -- docs/dividend-engine-prd-v0.1.md` | 0 | `ff2037a1` / `3f3e30cd` / `76213531` —— 3 个，与任务包一致 |
| EV-DE-32 | `NEGATIVE_SEARCH` | 检索面＝`rounds/r11/implementation-gaps.csv`（73 行）与 `rounds/r8/findings.csv`（238 行）；检索式＝`分红\|dividend\|留存\|释放` | 0 | gaps：0 命中；canonical findings：7 命中，其中与本 shard 相关的 `GAP-41`（open）与 `GAP-10`（open）已存在，**引用不重列**；无一条覆盖 R14-DE-01/02/03 |

---

### 给编排者的两点提示

1. **R14-DE-01 与 R14-DE-02 均为 P0**，按 request.md §7.6「任何 P0 未定性」不得 PASS —— 两条本 shard 均已定性（非 D 类），不阻塞收敛。
2. **R14-DE-06 存在 `EVIDENCE_CONFLICT` 读法分歧**（C 或 B），已并列两侧证据、未选边，请 Consolidator 裁；**R14-DE-03 与 finance-settlement §14「净利 = 毛利 − 费用」**需与 FS shard 合并去重。

（按任务包要求，本报告不输出任何 VERDICT 行。）

---

*编排者落盘备注：本文件为 R14-b01 Auditor DE 子代理返回报告的原样落盘（任务通知通道转义的 HTML 实体已还原）。落盘时间 2026-08-20。*
