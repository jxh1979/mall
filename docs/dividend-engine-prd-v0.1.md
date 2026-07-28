# 持股分红引擎 PRD v0.1（V299，2026-07-18 拍板落地）

> 状态：现行。收编自 2026-07-18 用户拍板终稿设计（分红基数=净利润制 / 主理人=真实股东股息红利 20% 代扣 /
> 周期=季度正式+月度预览 / 凡涉数据皆可配），并补充本轮实现细节。
> 上游数据基建：V296 库存进货/出库成本台账（`inventory-*`，成本侧）、V297 商户费用归集台账
> （`finance-settlement-product-prd-v0.1.md` §14，费用侧）。恒等式与算式留痕见 §5。

## 1. 业务背景与目标

综合体内部分业态（典型：酒吧）采用「平台持股 + 主理人经营」模式：平台出场地/牌照/资金占 80%，
主理人实际经营占 20%。此前钱都收进来了（结算流水、收入台账都有），但没有「定期把经营成果按持股比例
分给主理人」的机制——分红靠线下手工算，算错了没底账，主理人也看不到依据。

目标：分红变成一个**周期性、可审计、可追溯**的系统作业：期末算出该分多少 → 财务审阅、最高权限审批 →
生成应付款项走现有付款通道 → 主理人在商户专区看到自己那份的每一步计算依据。

## 2. 已拍板口径（2026-07-18 终稿）

1. **基数 = 净利润制（方案 B）**：期间收入 − 期间销货成本（V296）− 期间费用（V297）。
2. **主理人分个人 / 公司两类（V308，2026-07-19 拍板 #28）**：**个人股东**（自然人真实持股）→ 税目 =
   股息红利，按方案税率代扣（默认 20%，税率可配）；**公司股东**（法人持股）→ **不代扣个税、按约定全额发放**
   （发放单据代扣额=0）。类型落方案配置 `principal_type`，历史分红单按单内类型快照追溯。
3. **周期 = 季度正式发放 + 月度预览**（默认口径；周期类型可配为月度正式）。
4. **凡涉数据皆可配**：比例（80/20 仅默认值）、基数构成（计入哪些收入域/是否扣成本/计入哪些费用类别）、
   周期类型、税率、主理人类型、自定义扣除项，全部为后台可调配置，禁止硬编码；配置改动走审计事件留痕。

## 3. 分红方案（配置层，`dividend_plans`）

- 挂在**商户**上（一个商户=一个经营标的），不挂合同：提成规则挂合同是「租户交平台」方向，
  持股分红是「平台分主理人」方向，主体是经营实体本身、不随租约换签中断。
- 字段：主理人账户（`principal_user_id`，发放走应付台账挂该账户）、主理人类型 `principal_type`
  （individual 默认 / company，见 §2.2）、平台比例 `platform_share_bps`（默认 8000）、代扣税率 `tax_rate_bps`
  （默认 2000，公司股东单强制生效 0）、周期类型 `period_type`（quarterly 默认 / monthly）、
  基数构成开关（`revenue_domains` 默认 bar+ticket、`include_inventory_cost` 默认 true、
  `expense_types` 默认全部六类）、自定义扣除项 `dividend_deduction_items`（子表，见 §5A）、
  生效起始日、状态（active/ended）、备注。
- **一个商户同一时期只允许一个生效中方案**（部分唯一索引硬保证）；调整=直接改当前方案行
  （审计事件留痕），历史分红单按单内配置快照追溯、不受影响（见 §5 快照冻结）。停用走 status=ended。

## 4. 期间作业（`POST /finance/dividends/statements/compute`）

- **手动触发端点**（与 V297 费用自动挂接同理由：上游费用挂接本身是手动出账，定时空跑会把不完整的
  费用基数算进正式单；预览也需要按需出）。kind=official 正式单 / preview 月度预览。
- 期间标签：季度 `2026-Q3` / 月份 `2026-07`。official 必须匹配方案周期类型；preview 仅对季度方案出
  （月度方案的正式单本身就是月度）。
- 对每个生效中方案（或指定商户）各算一张计算单；时窗=平台全局经营日切分
  （`BusinessDayResolver.startInstant`，与 V296 成本汇总、营业额租金取数同口径——三来源同窗才能恒等）。
- **幂等可重跑**：同期同商户同类别只允许一张未作废单（部分唯一索引）；重跑时 preview/draft/rejected
  作废（voided）重算=红字更正范式；pending_review/approved/paid 已进入审批流，跳过并回报原因。
  写路径统一幂等键+载荷指纹（同键不同载荷拒绝）。
- **周期可配 + 跨季度处理不算错（2026-07-19 核查 #34：现状已支持，无需改码）**：分红「会计期间」由方案
  `period_type`（季度 / 月度）+ 期间标签共同决定，是一个**固定的自然日窗口**；三来源金额按**各交易自身
  时间戳**（结账 `settled_at` / 退款 `refunded_at` / 售票 `created_at` / 费用月份桶）归入其所属期间，**不按作业
  运行日归集**。因此：财务对账按固定日子（一个月/一周的某几天）而非每天触发时，某笔结算即便**跨季度才被
  处理到**，也只是落进其时间戳所属的那个期间——`compute` 端点**接受任意历史期间标签、任意时点触发**、幂等可
  重跑，**不存在「处理太晚 / 跨季度」的报错**。对账触发节奏是运维触发面（何时 POST compute），与会计期间窗口
  完全解耦。


## 4A. 跨周期维度补充（P2，2026-07-28）

闭合 BSM-0411 `cross_period`（与 §4 期间作业一致，明示验收）：

1. **正式周期 vs 预览**：默认季度正式 + 月度预览；切换为月度正式时，预览规则按方案配置，不得两套正式单重叠同一官方期间。  
2. **跨季/跨月重算**：已 `approved/paid` 的正式单不因后期费用调整自动重算；需冲销/作废后按权限重开期间作业。  
3. **关账锁**：正式非 preview 作业锁定费用侧期间后，跨周期补录费用不得无声进入已锁定分红基数；须走解锁/冲销策略（见既有 period-lock）。  
4. **比例/税改配置**：改方案只影响新期间；历史单按单内快照。  
5. **权限/证据**：跨期冲销、重算、解锁须最高权限或配置权限 + 全链路审计。


## 5. 算式与三来源对账（恒等式 DB CHECK 硬保证）

```
期间收入  = Σ计入收入域（bar：bar_bill_settlements 结账实收(reversed=false) − bar_bill_adjustments 退款；
            ticket：issued_entitlements.allocated_revenue_cent − ticket_refunds）    ← revenue.net 商户行级同谓词区间版
− 销货成本 = V296 InventoryRepository.sumOutboundCost（仅源库位出库流水 total_cost_cent；
            无成本基准出库笔数 no_cost_basis_count 随单留痕，>0 提示成本或有低估）
− 期间费用 = V297 merchant_expense_entries 按月份桶 × 计入类别净额（红字冲销行负值自然抵减；
            含租金 rent_allocation——2026-07-18 拍板租金计入费用台账并默认扣减，auto_rent 自动挂接
            覆盖 contract_rent 合同租金 + turnover_rent 营业额抽成两条出账路径）
= 净利润 net_profit_cent
可分配额 distributable = max(净利润, 0)          （亏损期不倒扣主理人现金）
平台分成 platform_share = 四舍五入(distributable × platform_share_bps / 10000)  （HALF_UP，2026-07-28 拍板）
主理人应得 principal_gross = distributable − platform_share            （取补数，两侧和恒 = 可分配额，不丢分）
生效税率 eff_tax_rate = 主理人类型=company ? 0 : tax_rate_bps        （V308 #28：公司股东不代扣，快照 eff 于 tax_rate_bps 列）
代扣个税 tax = principal_gross × eff_tax_rate / 10000                  （**整除截断·刻意保留**，对齐税局申报口径，与薪资税表 V217 同口径）
净发放额 principal_net = principal_gross − tax
```

- **取整口径（2026-07-28 用户拍板 · R-7/D15.1 收尾 · 两种口径刻意混用）**：
  - **改四舍五入（HALF_UP，走公共入口 `MoneyRounding.bpsOf`）**：**平台分成**、**风险准备金留存**（发放保护线比例额）。
    旧整除截断使平台恒少收/少留、差额白送主理人且不留痕，与「金额一律四舍五入到分」的全局口径不符。
  - **保持整除截断（不得改）**：**代扣股息红利个税**，以及薪资域个税税表整除链（`JdbcStaffRepository` V217）。
    理由：**与税局申报口径对齐**，改成四舍五入会使代扣申报对不上。
  - **混用不破坏守恒**：链上每一步的另一半均取**补数**定义（`principal_gross := distributable − platform_share`、
    `principal_net := principal_gross − tax`、`payout := after_custom − reserve`），故
    `平台分成 + 代扣个税 + 欠款抵扣 + 自定义扣除 + 风险准备金 + 主理人实得 ≡ 可分配额` 逐分严格相等。
  - **分红链尾差恒为 0**，故**不设**尾差单列列（与通票分账不同——那边各家份额各自独立按比例算才产生尾差，
    落 `settlement_bills.rounding_residual_cent`/V343）。已按 R-7 既有尾差范式用 `MoneyRounding.residual` 实算断言为 0。
  - **风险准备金「释放」额**（`DividendReserveReleaseMath.releasableCent`）**保持向下取整**：
    使「Σ各次释放 ≤ 原留存额」恒成立（分期释放不超额返还）；当前恒以全额比例 10000 调用，无舍入发生。
- 五条恒等式落 `dividend_statements` DB CHECK（净利恒等/可分配额/劈分守恒/净发放/税额边界），
  不恒等的单**无法落库**。**公司股东单** `principal_type='company'` 时 `chk_ds_company_no_tax` 硬保证 `tax_cent=0`
  （V308 #28）。
- **算式明细行**（`dividend_statement_lines`，append-only：V140 触发器禁 UPDATE/DELETE + REVOKE）：
  每张单逐项留痕（各收入域收入与退款/成本/分类型费用/净利/劈分/税/净发放），detail 注明取数谓词与窗口，
  可追溯到三个来源台账；来源行合计 = 净利润（单测+集成测试断言）。
- 计算单头快照冻结当期配置（比例/税率/基数构成/主理人/主理人类型/自定义扣除合计），历史单永远按当期口径追溯；
  头部金额列冻结由仓储保证（UPDATE 只触状态/审批留痕列）。

## 5A. 自定义扣除项（V309，2026-07-19 拍板 #35）

在 V303 默认三项扣除（亏损结转 / 欠款抵扣 / 发放保护线）之外，方案可**按需新增任意条自定义扣除项**，
配置模型可扩展（新增来源=加枚举值 + 纯算式分支，非硬编码固定项数）：

- **配置**（`dividend_deduction_items` 子表，挂方案；随方案配置整表替换，历史单靠算式明细快照留痕不受影响）：
  - `name` 名称；`source_type` 金额来源（`fixed` 固定额 → `amount_cent`；`ratio` 比例 → `ratio_bps` ×
    `ratio_basis` 基数，基数 ∈ {`net_profit` 净利润 / `revenue` 营收}）；`enabled` 开关；`sort_order` 扣减顺序。
  - 后台经方案新建/修改请求携带 `customDeductions` 列表整表落地（缺省=不改现值，空列表=清空）。
- **落账口径**（不破坏 V299 净利/劈分/税恒等，也不破坏 V303「各扣除」语义）：自定义扣除与欠款抵扣/留存
  同属「税后应得的各扣除」，在欠款抵扣之后、留存之前，对 (税后应得 − 欠款抵扣) 余额**逐项按 `sort_order`
  封顶扣减**（不透支），合计冻结在 `custom_deduction_cent`，逐项落 `custom_deduction` 算式明细行留痕。
- **发放额恒等改写**：`payout_cent = principal_net_cent − arrears_offset_cent − custom_deduction_cent −
  reserve_withheld_cent`（由 `chk_ds_payout_identity` 重写硬保证；`chk_ds_custom_deduction_range` 保证
  `0 ≤ custom_deduction_cent ≤ principal_net − arrears`，`chk_ds_payout_nonneg` 保证实发非负）。留存保护线
  的比例/固定额作用基数随之变为「自定义扣除后余额」（无自定义项时逐分等于 V303 现状）。
- **诚实边界**：自定义扣除额由平台留存（隐式=少发即留），本轮**不单独挂应付/应收台账**；如需指定去向
  （如计入某科目/挂某方应付）另立项。

## 6. 审阅审批与发放（状态机）

```
compute → draft（正式单）/ preview（月预览，只可看，不可提交/审批/发放）
draft → pending_review（提交审阅）→ approved（审批通过）→ paid（标记发放）
pending_review → rejected（驳回，理由必填）→ 修正数据后重算（旧单 voided + 新 draft）
```

- 审批要求 `finance.dividend.manage`（V299 仅授 `role_platform_admin`）= 最高权限审批现状口径
  （与 V271 存档删除审批同范式）。
- **approved 时挂应付**：净发放额 > 0 → `ar_ap_items`（direction=payable，counterparty=主理人账户，
  `source_type='dividend_payout'`，source_id=计算单 id；(direction,source_type,source_id) 部分唯一索引
  防一单重挂）。实际打款走应收应付台账既有付款确认流程；`mark-paid` 登记发放完成。
  净发放额=0（无利润期）审批通过即归档，不挂应付。
- 审计事件全程留痕（10 个 operation 各配 success/failure 事件对，见 OpenAPI x-audit-event）。

## 7. 权限与可见性

- `finance.dividend.view`（medium）：方案/计算单只读；商户 scope 账号被后端 `currentMerchantScope`
  硬闸强制只看本商户（列表覆盖请求参数、详情跨商户 CROSS_MERCHANT_FORBIDDEN、明细行叠加商户谓词）。
- `finance.dividend.manage`（high）：方案配置/算单/审批/发放，全部要求平台 scope（requirePlatformScope）。
- V299 两码直授 `role_platform_admin`；商户专区主理人按需授 view（走既有角色装配）。

## 8. 页面

- **ADM-FIN-007 持股分红**（/admin/finance/dividends，平台侧）：方案列表+引导式新建/修改
  （GuidedFlowModal 三分节：商户主理人/比例税率周期/基数构成）、期间作业触发（含跳过原因回显）、
  计算单筛选分页、行内状态机动作（提交审阅/审批通过/驳回带理由/标记发放）、明细抽屉逐行算式。
- **ADM-MER-008 我的分红**（/admin/merchant-zone/dividends，商户专区，accountForms=merchant）：
  主理人只读视图——当前方案摘要 + 每期基数/比例/扣税/实发/状态 + 算式明细抽屉；月度预览可见（只看不发）；
  无任何写动作入口，异议走平台侧驳回重算。

## 9. 边界与后续迭代（如实留档）

- **扣除项三开关已实现**（V303，设计稿 §5：亏损结转/欠款抵扣/发放保护线）：方案级三个可选开关，
  **默认全关时逐分等于 V299 现状**（回归由 DividendMathTests `computeAllTogglesOffEqualsLegacySplit` 抽样组合断言）。
  - **亏损结转 `loss_carryforward_enabled`**（2026-07-19 用户拍板：**累计恒结转，开关只控本期扣不扣**）：
    亏损一旦产生就一直累着直到被后续盈利填平；开关只控本期是否扣减可分配额，**关闭期不清零累计**。
    `applied` = 开启? min(期初累计, max(净利,0)) : 0（关闭期不扣不还）；可分配额 = GREATEST(净利 − `applied`, 0)；
    `after` = **恒** 期初累计 − applied + max(−净利,0)（关闭期把老累计+本期新亏损带下去）。累计**不另建可变台账**——
    以「已审批/已发放（approved/paid）的正式计算单」自身快照的 `loss_carryforward_after_cent` 逐期结转（`priorLossCarryforwardCent`
    **恒**取 period_start 之前最近一张已结正式单，与本期开关无关），approved/paid 单即账本：draft/preview 不计（幂等，重算稳定）、voided 天然回滚。
    本期 in/applied/after 三值冻结在单上，结转链恒等由 `chk_ds_lcf_applied`（随开关）/ `chk_ds_lcf_after`（无条件累计式）DB CHECK 硬保证。
    **累计更新时点**=审批通过（approve）——同 compute 幂等（draft 重算读的是上一张已结单，不受本期 draft 影响）。
    存量单以 `after = GREATEST(−净利,0)` 回填满足无条件 CHECK（V303 前无结转链，首个开启期从最近一张已结单 after 起接续，历史多期真累计不回溯——设计边界）。
  - **欠款抵扣 `arrears_offset_enabled`**：税后应得先抵扣该商户对平台未清应收（ar_ap direction=receivable, unpaid>0），
    计算时快照抵扣额 `arrears_offset_cent`=min(税后应得, 未清应收合计) 并逐项留痕；**审批通过时** `applyDividendOffsetToReceivables`
    先预检该商户当前未清应收总额——不足冻结抵扣额（抵扣期间被并发清偿）则**一分不结算**直接返回实况、由服务层抛 `DIVIDEND_ARREARS_CHANGED`
    409（**原子 by construction**：不留半结、不依赖事务回滚，因 `executeWrite` 非独立事务），要求作废重算（快照冻结口径）；
    足额则按冻结额对应收先欠先抵逐项结算（dividend_offset 收款，复用 `settleArApItem` 行锁），抵扣后余额（`payout_cent`）才挂应付。
  - **发放保护线 `payout_floor_enabled`**（2026-07-19 用户拍板：**留存=暂扣待释放、欠主理人、非平台白拿**）：留存
    `reserve_withheld_cent` = min(抵扣后余额, max(余额×`payout_floor_bps`/10000, `payout_floor_fixed_cent`)) 本期不发放；
    审批时把留存额记为**欠主理人的待释放应付**（`FinanceService.createDividendReserveWithholdingPayable`：payable, counterparty=主理人,
    source_type=`dividend_reserve_withheld`, source_id=计算单 id 一单一挂；状态沿用 pending_payment，source_type 即待释放标识）。
    **释放机制（下期/年度末返还规则 + 手动释放端点 + UI）为后续独立构建 R**，本轮只落待释放负债。
  - 发放额恒等（V309 后含自定义扣除）`payout_cent = principal_net_cent − arrears_offset_cent − custom_deduction_cent − reserve_withheld_cent`
    （均非负、抵扣不超税后应得）由 `chk_ds_payout_identity`（V309 重写）/ `chk_ds_arrears_range` / `chk_ds_custom_deduction_range` /
    `chk_ds_payout_nonneg` DB CHECK 硬保证；净利/劈分/税恒等（V299）不变，
    可分配额只扣本期结转弥补额（`chk_ds_distributable` 重写为 GREATEST(净利 − `loss_carryforward_applied_cent`, 0)，关闭期 applied=0 退化为原式）。
  - 三扣除（+ V309 自定义扣除，见 §5A）只削减「发放额」，**审批挂应付口径由 `principal_net_cent` 改为 `payout_cent`**（默认全关/无自定义项时二者相等，行为不变）。
- **正式单仍手动触发**：正式单（进入审批发放）维持手动触发端点（费用侧关账节奏由人把关，防不完整基数入正式单）。
  **月度预览定时作业已上**（V302，`DividendMonthlyPreviewJob`）：默认关闭（`dividend.preview.scheduled.enabled=true`
  才注册，cron 可配，默认每月 1 日 02:00 跑上月），对每个生效中季度方案自动出上月 preview 单（只读不可审批），
  刷新式幂等（同期未作废 preview 先作废再重出）；业务逻辑在可直调的 `DividendService.runScheduledMonthlyPreview`。
- **分红期间关账锁已上**（V302，口径=「费用侧冻结」非全基数冻结）：某商户某月被非 preview 且
  status∈(approved,paid) 的计算单按 [period_start, period_end] 覆盖即「关账」（季度单覆盖 3 个月）——
  <b>锁的只是费用台账 merchant_expense_entries</b>（营收/成本源头按设计不锁）；已发放单的金额靠 V299 计算单快照
  冻结另行兜底。费用台账三写路径挂 app 层硬闸——手工录入/红字冲销对已关账
  (商户,月份) 拒（响亮 409 `DIVIDEND_PERIOD_LOCKED`，进 x-failure-codes），自动挂接（能耗/物业/合同物业/租金四路）
  对已关账 (商户,月份) NOT EXISTS 跳过并在响应列出跳过清单；判定纯函数见 `DividendPeriodLock`，SQL 索引 V302
  `ix_dividend_statements_lock`。解锁=该单作废（判定式不含 voided，天然解锁）。营收/成本源头不锁（经营流水不因分红
  停摆），其漂移靠 compute 前置的同期唯一部分索引兜（同商户同期不重复出正式单）。前端费用页对已关账月份禁用录入/
  冲销按钮 + 锁定提示（读 `GET /finance/merchant-expenses/period-locks`），分红页对 approved/paid 正式单标「期间已关账」。
- **收入域默认不含合同租金**：租金是商户对平台的支出（费用侧 rent_allocation 类别），计入收入会与
  费用侧重复；BI revenue.net 展示口径含租金分量，与分红基数口径有意不同。费用侧租金已默认计入
  （2026-07-18 拍板：auto_rent 自动挂接，分红方案基数构成默认勾选 rent_allocation，可按方案关闭）；
  turnover_rent 历史行（V299 前出账）无 accounting_period 期间标记，不自动挂接，需手工录入补齐。
- **成本诚实边界**：存量库存未录进货价的出库成本为 NULL（V296 口径不编造），计算单以
  no_cost_basis_count 留痕提示或有低估；财务可在审阅时驳回、补录进货价后重算。
- 计算单头金额列冻结靠服务层仓储纪律（无金额列 UPDATE 语句）+明细行 DB 级 append-only；
  如需头部列级 DB 冻结需自定义触发器，本期不做。

## 变更日志

| 日期 | 类型 | 内容 | 原因/依据 |
|---|---|---|---|
| 2026-07-18 | 增 | v0.1 初版：收编拍板终稿（净利润制/股息红利 20%/季度+月预览/全可配）+ V299 实现细节（三表/恒等式 CHECK/append-only 明细/状态机/应付挂账/两页面/10 operations） | 2026-07-18 用户拍板；V296/V297 数据基建就绪后的分红引擎本体交付 |
| 2026-07-18 复核 | 改 | C-1 状态机 UPDATE 全带期望前态守卫（0 行=业务 409，杜绝并发 approve×reject 双付窗口）；I-1 审批职责分离（提交人不得审批自己提交的单 SELF_APPROVAL_FORBIDDEN）；rejected_by/rejected_at 留痕；商户侧 DTO 脱敏（内部操作员/应付 id 不外发）；租金计入费用台账并默认扣减（auto_rent 挂接 contract_rent+turnover_rent，turnover_rent 出账起落 accounting_period） | 一轮复核 1C+1I+4S + 用户拍板租金计入分红基数 |
| 2026-07-18 二轮复核 I-2 | 增 | V302 分红期间关账锁 + 月度预览定时作业：关账判定纯函数 `DividendPeriodLock` + 部分索引 `ix_dividend_statements_lock`；费用台账三写路径挂锁（手工录入/冲销拒 `DIVIDEND_PERIOD_LOCKED` 409，自动挂接四路 NOT EXISTS 跳过 + lockedMerchants 清单）；`DividendMonthlyPreviewJob`（默认关，@ConditionalOnProperty，刷新式出上月季度方案预览单）；新增只读端点 `GET /finance/merchant-expenses/period-locks`（前端禁用已关账月份录入/冲销）；分红页 approved/paid 单标「期间已关账」。§9 边界更新 | 二轮复核 I-2 敞口（已发放单来源台账静默漂移）；封死费用侧追改口子 |
| 2026-07-19 | 增 | V303 扣除项三开关（设计稿 §5 落地，§9 留档项）：方案级 `loss_carryforward_enabled`/`arrears_offset_enabled`/`payout_floor_enabled` + 保护线参数 `payout_floor_bps`/`payout_floor_fixed_cent`，**默认全关时逐分等于 V299 现状**。计算单快照增 `loss_carryforward_in/applied/after_cent`、`arrears_offset_cent`、`reserve_withheld_cent`、`payout_cent` 七列（存量单回填 payout=principal_net）；`chk_ds_distributable` 随亏损结转开关重写，新增结转链/发放额恒等 CHECK；明细行增 `loss_carryforward`/`arrears_offset`/`reserve_withheld`/`payout` 四类留痕；`DividendMath.compute` 纯函数统一三扣除算式；亏损累计以已结正式单快照 projection 结转（不另建可变台账）；欠款抵扣审批时逐项结算应收（`DividendPayoutPort.applyDividendOffsetToReceivables` 复用 `settleArApItem`，实结≠冻结额抛 `DIVIDEND_ARREARS_CHANGED` 回滚重算）；审批挂应付口径由 principal_net 改为 payout。DividendMathTests +12 / DividendServiceTests +6 / JDBC +2（CHECK 拦截+结转端到端）/ vitest +3；前端方案引导式增「扣除项」分节、计算单实发额与扣除项逐行留痕。 | 设计稿 §5 已 deferred 项落地；快照结构可扩展兑现 |
| 2026-07-19 用户裁决 | 改 | #2 亏损结转=**累计恒结转**（开关只控本期扣不扣可分配额，关闭期不清零累计）：`DividendMath.compute` applied 随开关、after 恒累计式；service 恒读往期累计（不再按开关门控）；`chk_ds_lcf_after` 去 CASE 改无条件、`chk_ds_distributable` 改扣 `loss_carryforward_applied_cent`，存量单 after 回填 GREATEST(−净利,0)；反例测试 开→亏→关一期→再开（Q3 carryIn=保留累计非 0）。#3 发放保护线留存=**暂扣待释放**（欠主理人、非白拿）：审批时 `createDividendReserveWithholdingPayable` 挂 `dividend_reserve_withheld` 待释放应付（counterparty=主理人，进 SOURCE_TYPES+中文标签），释放规则/端点/UI 为后续独立构建 R。欠款抵扣改**原子 by construction**（预检不足一分不结算）。DividendServiceTests +1（累计切换）+留存待释放断言、JDBC +3（欠款真实结算×2 / 留存待释放）、entry 基线刷 650 ops、rebase 到 main 46a1ba8e。 | 2026-07-19 用户对 §9 两项待裁口径拍板 |
| 2026-07-19 | 增 | **#28 按主理人类型区分代扣个税**（V308）：方案增 `principal_type`（individual/company），公司股东生效税率强制 0、全额发放；计算单快照 `principal_type` + `chk_ds_company_no_tax`（公司单 tax_cent=0 硬保证）；税额行 detail 按类型分流。见 §2.2 / §5。 | 2026-07-19 用户拍板 #28 |
| 2026-07-19 | 增 | **#35 自定义扣除项**（V309）：新 `dividend_deduction_items` 子表（name/source_type[fixed/ratio]/amount_cent/ratio_bps/ratio_basis[net_profit/revenue]/enabled/sort_order），经方案请求 `customDeductions` 整表替换；`DividendMath.compute` 增 `clampDeductions` 逐项封顶（欠款抵扣后、留存前，对 (税后−抵扣) 余额扣减不透支）+ `customDeductionCent`；计算单增 `custom_deduction_cent` 列，`chk_ds_payout_identity` 重写含自定义扣除、新增 `chk_ds_custom_deduction_range`；明细行增 `custom_deduction` 留痕。DividendMathTests +4 / DividendServiceTests +5 / JDBC +5。见 §5A。 | 2026-07-19 用户拍板 #35 |
| 2026-07-19 核查 | — | **#34 分红周期真可配置**：核查结论**现状已支持、无需改码**——会计期间由 `period_type`+期间标签定义为固定自然日窗口，三来源按交易时间戳归期（非运行日），`compute` 接受任意历史期间、任意时点、幂等重跑，**无跨季度/迟处理报错**；对账触发节奏与会计窗口解耦。见 §4。 | 2026-07-19 用户 #34 先核查 |

| 2026-07-28 | 增 | §4A | 跨周期维度明示（P2 BSM-0411） |
