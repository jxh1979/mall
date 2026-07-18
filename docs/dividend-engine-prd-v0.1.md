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
2. **主理人 = 真实股东（工商登记持股）→ 税目 = 股息红利，固定 20% 代扣**（税率本身可配）。
3. **周期 = 季度正式发放 + 月度预览**（默认口径；周期类型可配为月度正式）。
4. **凡涉数据皆可配**：比例（80/20 仅默认值）、基数构成（计入哪些收入域/是否扣成本/计入哪些费用类别）、
   周期类型、税率，全部为后台可调配置，禁止硬编码；配置改动走审计事件留痕。

## 3. 分红方案（配置层，`dividend_plans`）

- 挂在**商户**上（一个商户=一个经营标的），不挂合同：提成规则挂合同是「租户交平台」方向，
  持股分红是「平台分主理人」方向，主体是经营实体本身、不随租约换签中断。
- 字段：主理人账户（`principal_user_id`，发放走应付台账挂该账户）、平台比例 `platform_share_bps`
  （默认 8000）、代扣税率 `tax_rate_bps`（默认 2000）、周期类型 `period_type`（quarterly 默认 / monthly）、
  基数构成开关（`revenue_domains` 默认 bar+ticket、`include_inventory_cost` 默认 true、
  `expense_types` 默认全部六类）、生效起始日、状态（active/ended）、备注。
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
平台分成 platform_share = distributable × platform_share_bps / 10000   （整除链截断）
主理人应得 principal_gross = distributable − platform_share            （余数归主理人，不丢分）
代扣个税 tax = principal_gross × tax_rate_bps / 10000                  （整除链截断，与薪资税表 V217 同口径）
净发放额 principal_net = principal_gross − tax
```

- 五条恒等式落 `dividend_statements` DB CHECK（净利恒等/可分配额/劈分守恒/净发放/税额边界），
  不恒等的单**无法落库**。
- **算式明细行**（`dividend_statement_lines`，append-only：V140 触发器禁 UPDATE/DELETE + REVOKE）：
  每张单逐项留痕（各收入域收入与退款/成本/分类型费用/净利/劈分/税/净发放），detail 注明取数谓词与窗口，
  可追溯到三个来源台账；来源行合计 = 净利润（单测+集成测试断言）。
- 计算单头快照冻结当期配置（比例/税率/基数构成/主理人），历史单永远按当期口径追溯；
  头部金额列冻结由仓储保证（UPDATE 只触状态/审批留痕列）。

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

- **扣除项三开关未实现**（设计稿 §5：亏损结转/未结清款项抵扣/发放保护线）：本期口径为亏损期可分配额=0
  （不倒扣、不结转）；负净利跨期补亏、主理人欠款抵扣、小额并入下期留待后续迭代，快照结构已可扩展。
- **定时作业未上**：期末自动触发（定时器）依赖费用侧关账节奏，本期为手动触发端点；
  配套的**分红期间关账锁**（已核算月份禁止再动费用台账）在 V297 PRD §14 留档，一并后续实现。
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
