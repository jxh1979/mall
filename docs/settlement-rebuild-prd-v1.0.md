# 通票权益结算重构 PRD v1.0

> **状态**:**v1.0 定稿**(2026-07-04 批 0 评审通过;经赠酒/返佣纠偏 + 全 PRD 逻辑一致性审查 + 术语表对齐,进入批 1 实施。术语见 terminology-canonical-v1.0.md)
> **权威来源**:① 用户 2026-07-04 口述结算口径(13 条);② 财务底稿 `llm-wiki/raw/内部底稿_财务测算完整版_v6a.1_0702.xlsx`(以及 NEXT-PARK 财务预测报表 v2.2、财务测算.xlsx)——尤其 `02_公司结构图`/`03_收入流向图`/`08_通票与分账`/`20_公式与口径说明`。
> 2026-07-04 逻辑一致性修订:§5.1 SPV 分账新增 `by_venue_per_guest`(按入场客数×各酒吧协议单价)方式;§5.3 赠酒段细化为"赠酒成本 3 元/点位先留(补偿酒成本)+ 剩余按各酒吧每客协议价分账"两段口径。
> **口径纪律**:本 PRD 落地后须回写业务 HTML(最终解释权)+ 相关 PRD;一切业务策略参数**可后台配置、禁硬编码**。
> **重要认识**:财务底稿是"按平均、按年"的**测算模型**(算 IRR/回报用);本系统要实现的是"每张票、每次核销**逐笔实时结算**"的**引擎**。底稿里的比例(20/80)、单价(内部结算价/权益单价/赠酒 3 元)在系统里一律是**可配参数**,系统按实际核销逐笔跑,底稿数值作为默认种子与统计校验基准。

---

## 1. 背景与目标

### 1.1 问题
现状系统在**出票时**就把票款按定价一次性分摊入各商户账(`issued_entitlements.allocated_revenue_cent`,`TicketOrderService.allocateRevenue`),**与是否核销无关**。这导致:
- 客人买了含 5 个权益的票只用了 4 个,第 5 个权益的钱**仍已计入对应商户账**,项目公司无法回收;
- 无"按核销结算"、无"未用权益归项目"、无各业态公司(SPV)分账层、无手续费预扣、无单独门票服务费、无套票折扣归属、无场次入场规则——这些要么文档有代码无,要么全新。

### 1.2 目标
把结算从"**发放即分摊**"重构为"**按实际核销逐笔结算 + 未用权益归项目公司**",并建立一套**全可配置的多主体分账体系**,精确复现财务底稿的商业逻辑,支撑地产全链的财务与经营口径。

### 1.3 非目标(本期不做/另批)
- 财务预测/IRR/敏感性测算(那是底稿的事,不进系统);
- 各业态公司的完整会计核算/报税(系统只做**营收分账与结算单**,出具给财务,不替代财务软件);
- 资产公司折旧、CapEx 等纯财务项。

---

## 2. 组织架构与名词定义(定死,回写所有文档)

### 2.1 四层实体(自上而下)
| 层 | 名词 | 定义 | 现状 |
|---|---|---|---|
| L1 | **平台** | 最大管理实体,可辖多城市多项目 | ⚠️ 代码无干净独立实体,"平台"一词混用 |
| L2 | **项目公司** | 某城市的项目(如长沙 NEXT PARK);平台下辖多个 | = 代码 `projects` 表;文档中"项目/综合体/集团"混用需统一 |
| L3 | **管理公司**(业态主体/SPV) | 项目公司与商户间的分账主体,如娱乐运营平台、酒吧 SPV、中央餐饮、游乐、传媒、资产公司 | ✅ 层级已落库(V318):复用 `business_entities`(entity_role=management_company/spv…)+ 新增 `parent_entity_id` 挂 L3→L2 项目公司父链;实例数据后台录入 |
| L4 | **商户** | 具体经营单位 | = `merchants` 表,叫法统一 |

### 2.2 术语统一规则(§12 展开)
- "综合体""集团""项目"混用处 → 一律按上表:面向单个经营体用"**项目公司**",面向多项目汇总用"**平台/集团视图**"。
- "管理公司"= L3 业态主体的统称;具体实例用专名(娱乐运营平台 / 酒吧SPV / 中央餐饮公司 / 游乐公司 / 传媒公司 / 资产公司)。

### 2.3 管理公司层落地范围(用户裁定:先建分账用得到的)
- **本期建**:娱乐运营平台(通票收款+留存主体)、酒吧 SPV(分账池承接方,×N 家可配)。
- **后批建**:中央餐饮、游乐、传媒、资产公司(随对应收入类型上线时补)。
- 表设计一次到位(支持全部 6 类),实例数据先录必需的。

---

## 3. 核心设计原则

1. **按核销结算**:权益收入在**核销发生时**才确认结算给商户/SPV;未核销 = 不结算给商户。
2. **未用权益归项目公司**:票有效期结束(或作废)后仍未核销的权益,其对应收入**归项目公司**(平台留存),不结给商户。
3. **全可配置、禁硬编码**:分账方案、留存比例、内部结算价、权益单价、服务费率、手续费率、赠酒单价、访问点位数、折扣归属、中央餐饮开关、**各项税费(增值税/文建费/附加,有无+费率)**等,一律走配置表 + 后台页 + 权限。
4. **逐笔可追溯**:每笔结算能追溯到票→权益→核销记录→结算明细→结算单,支持对账。
5. **幂等与防混账**:核销/结算/退款全幂等;权益归属严格隔离(project_id / merchant_id / spv_id),不串户。
6. **向后兼容迁移**:提供从 `allocated_revenue_cent`(发放即分摊)到新引擎的迁移与并行校验路径(§11)。
7. **业务规则 ≠ 财务测算假设(2026-07-04 用户纠偏,关键)**:财务底稿里大量"公式"是**预测假设**——用来在财务模型里估算营收数字(如"50% 通票客人会买额外酒水、平均 1.5 杯、每杯 20 元""客流 7100 人/日""满台率 70%")。这类假设**一律不进系统**——系统按**真实交易/核销逐笔记账**(客人实际买几杯记几杯、实际访问几个点位算几个)。只有"每一笔钱怎么算、怎么分"的**结算规则**(留存比例、服务费率、内部结算价、佣金率、手续费率、折扣归属、税率)才进系统做配置。**成本同理:系统成本 = 真实费用归集,不用"收入×成本率"估算。** 附录 A 已据此把参数分为"进系统"与"仅财务测算"两栏。

---

## 4. 收入类型与主体归属(6 主体 + 税务属性 + 中央餐饮开关)

依据 `03_收入流向图`。**关键口径列只写业务结算规则,不写财务预测公式**(如"张数×50%×1.5杯"是财务测算假设,系统按实际交易记账,见 §3.7):

| 客人付款 | 收款主体 | 税务属性 | 关键口径(系统按实际交易入账) |
|---|---|---|---|
| **通票** | 娱乐运营平台 | 平台运营服务费(留存%) + SPV 分账(余额),计文化事业建设费 | 按核销分账,§5 |
| 订台低消 / 二次消费 | **中央餐饮**(开关关→归酒吧) | 餐饮服务 | 按实际消费入账;订台价运营后台设定 |
| 额外酒水 | **中央餐饮**(开关关→归酒吧) | 餐饮服务 | 按实际销售逐笔入账 |
| 非散台服务费(仅包厢/卡座) | 酒吧 SPV | 娱乐服务 | 服务费率(默认 10%,可配)× 实际低消+二次消费,散台不收 |
| 游乐票 | 游乐公司 | 游乐服务 | 计文化事业建设费 |
| 演出票 | 传媒公司 | 文化服务 | 计文化事业建设费 |
| 商业租金 | 资产公司 | 不动产租赁 | 外部商铺/市集 |

**中央餐饮开关(用户明确)**:`central_catering_enabled`,项目公司级,**默认=开**,切换影响收入归属与税务属性、审计留痕。
- **开**:酒水销售(额外酒水/订台酒水)收入归**中央餐饮公司**;中央餐饮借酒吧场地卖酒,按可配**场地返佣**返给对应酒吧 SPV(见下)。
- **关**:酒水销售收入**直接归该酒吧**(作独立商户账,不经中央餐饮、不进通票分账池);此时无场地返佣。
- **通票分账池不受开关影响**——通票收入始终按留存%+余额规则(§5)分。

**中央餐饮场地返佣(2026-07-04 定:真实商业约定,进系统可配)**:开关开时,中央餐饮用酒吧场地/客流卖酒,按**实际酒水销售额**的可配比例(或可配固定额)返佣给提供场地的酒吧 SPV;返佣在销售结算时逐笔/周期计算,非财务平均分利。配置 `catering_venue_rebate_rules`(比例或金额,按门店可配)。

---

## 5. 通票分账引擎(本次重构的核心)

### 5.0 通票分账全流程(2026-07-04 用户锁定,批 2 权威)
一张通票收入按下列**固定顺序**逐笔分账(以 99 元为例):

| 步 | 算法 | 例(99 元) |
|---|---|---|
| ① 通票收入 | — | 99 |
| ② 扣渠道费 | 第三方抽成(美团/抖音等)**最先扣,在所有费用之前** | − 渠道费 |
| ③ 赠酒不从池扣 | 赠酒成本**不再从通票收入/可分配池扣**——移 §5.3 由 bar 域每次核销按该商品**内部结算价**给出品酒吧挂应付(D5 后口径) | —(不影响池) |
| ④ 可分配池 | 收入 − 渠道费 | 99 − 渠道费 |
| ⑤ 项目公司留存 | 池 × 留存%(默认 **20%**,可配)→ 归娱乐运营平台 | 池 × 20% |
| ⑥ 各酒吧总池 | 池 ×(1 − 留存%) | 池 × 80% |
| ⑦ 各酒吧分配 | 各酒吧总池按 §5.1 第二层方式分,**各家不同、总额 ≤ 总池** | ≤ 各酒吧总池 |

- **超总池硬约束**:各酒吧分配总额**不得超过**各酒吧总池;确需超出**仅限平台负责人特批的短期激励(≤3 个月)**,到期收回。系统对分配总额做校验,超出非特批则拒绝。**特批出口 + 超额 C 口径已落地(D7·甲标签法):① 特批出口——端点 `POST /settlement/bills/{id}/special-approval`(复用 `settlement.bill.post` 权限 + 平台域·`SettlementPostingService.specialApprove`),仅**超池 draft** 单可填 `special_approval_ref` 并置 confirmed(非超池/已推进态拒 `BILL_NOT_OVERPOOL_DRAFT`),confirmed 后走既有 `postBill` 过账;② 超额 C 口径——`postBill` 对超池单的池明细两段拆:池内额 `pool_i = a_i×spvPool/poolDistributed`(整除截断)过 `ticket_distribution`(项目公司承担)、超额 `excess_i = a_i − pool_i` 过 `platform_incentive_subsidy`(L3 娱乐运营平台承担·独立幂等前缀 `settle-pis-`),不变式 `pool_i+excess_i=a_i`、`Σpool≤spvPool`、`Σexcess=poolDistributed−Σpool`;专属直付(`exclusive_full`)不拆、全额 `ticket_distribution`,未超池单零行为变更;③ 防覆盖——`upsertBill` 的 ON CONFLICT 对已特批单 `COALESCE` 保 `special_approval_ref`、`CASE` 锁 `status` 不被月结重跑打回 draft;④ **冻结**——`settleMonth` 对「已特批 confirmed」单直接返回原单 id、**不重算明细**(特批锁定的是「该次超池特定金额」,此后新增核销普通重跑会把明细抬高与已批总额脱节);⑤ **溢出护栏**——两段拆 `pool_i` 用 `BigInteger` 中间乘(`a_i×spvPool` 大场馆高营业月可越 `long` 上限回绕、致 `pool_i` 偏小补贴虚高);⑥ **红线 b 隔离**——补贴应付 `project_id=NULL`(平台主体域,不进任一项目公司聚合),特批出口 `recordSuccess` 留痕(billId/ref/超池额)。**零迁移**(`ar_ap_items.source_type` 自由 VARCHAR 无 CHECK,复用现有权限)。**
- 赠酒补偿(§5.3)、⑦ 各酒吧分配均**按实际核销/入场逐笔发生**;通票未核销部分的余额归项目公司(原则 2)。
- "项目公司留存"= 项目公司**拿走**留存%,各酒吧分**剩余**(1−留存%);二者之和 = 可分配池。
- **过账**:结算单算出后,由月结作业 / 网关端点 `POST /settlement/bills/{id}/post` 过账到应收应付台账。🔴 **过账独立闸**:月结作业内的 `postBill` 受**独立开关** `settlement.monthly.posting.enabled` 门控(默认 false·**不进 application-prod.yml**),与月度作业运行开关 `settlement.monthly.scheduled.enabled`(prod=true·本就开着算账单)**解耦**——settleMonth 照常算 confirmed 单(内部计算不落账本),**生产过账保持关**,直到用户拍板翻 `posting.enabled=true` 才首次真落账本。网关端点是人工特批路径(不受此闸,本就要人点)。

### 5.1 分账两问,均可配置
用户明确:分账方案本身可配,20/80 只是财务平均视角。引擎设计为**可插拔的分账方案**:

**第一层——平台留存**(=综合服务费,所有门票都过一道,用户裁定):
- `platform_retention_rule`:平台从通票收入留存的方式,支持 `percentage`(默认 20%,可配)/ `fixed` / `tiered`;
- 留存部分 = 平台运营服务费,归娱乐运营平台;
- 单独门票(§7.2)的服务费复用同一条规则,费率可按票种/商户各自洽谈。

**第二层——余额在 SPV/商户间分配**(按核销):
- `spv_distribution_method`,可选:
  - `by_entitlement_price`(**推荐默认**):按各权益**实际核销**× 单价精分——实物权益按**内部结算价**、虚拟权益按**签约权益单价**(§6);
  - `by_visited_point_average`:**仅简化/过渡口径**——余额 ÷ 实际访问点位数平均分;各酒吧酒水价值不同,平均分不精确,仅供迁移期兼容,新业务默认用上一条按权益单价精分;
  - `by_component_price_ratio`:按权益定价占比(近似现状 allocated 逻辑,过渡用);
  - `by_venue_per_guest`(**2026-07-04 用户明确落地口径**):按**入场客数 × 各酒吧协议单价**分账——"每进一个客人给几元",**各酒吧协议不同、后台按门店可配**(配置项 `spv_per_guest_rate`);按各酒吧协议单价×客数分给对应酒吧 SPV。**赠酒补偿走 §5.3 第一段(bar 域按内部结算价挂应付),D5(2026-07-21)后不再从池预留赠酒成本(每杯 3 元池扣已删)**。此为用户拍板的现实结算方式,与上面"按权益单价精分/按点位平均"并列,后台可配选择。
  - `by_rank_tier`(**排名档位比例**,2026-07-04 用户口径):各酒吧总池按**通票核销排名**分档比例分——例:第 1 名 16%、2–4 名各 12%、5–8 名各 9%、最后 2 名各 6%(1+3+4+2=10 家,合计 100%);**档位区间与比例后台可配**,总额 = 各酒吧总池。
  - `by_fixed_amount`(**固定金额**):每家酒吧后台直接填分账固定额,**总额 ≤ 各酒吧总池**(超出走 §5.0 特批)。
- 上述 `by_venue_per_guest`/`by_rank_tier`/`by_fixed_amount` 三种是"各酒吧总池怎么分给各家"的并列可配方案(每家不同);`by_entitlement_price` 等为权益级精分口径。项目公司级选用哪种,后台可配。
- 未核销权益不参与二层分配 → 其对应余额**留在平台**(=未用归项目,原则 2)。

### 5.2 结算触发(按核销,非发放)
```
出票    → 记录票款、各权益定价与归属(不入商户收入账,仅"待核销")
核销    → 对该权益:①判定结算价(实物内部价/虚拟权益单价)②计入对应主体"已核销结算"
票失效  → 定时作业扫描未核销权益 → 其余额确认归平台(未用归项目)
```

### 5.3 赠酒(通票免费酒水权益)结算 — 按核销由项目方补偿(2026-07-04 纠偏)
赠酒 = 通票内含的**待核销实物权益**(每访问一个点位最多领 1 杯)。通票收入分账到酒吧 SPV 时,赠酒相关按**两段**处理(2026-07-04 用户明确):
- **第一段 — 赠酒补偿(不从池扣)**:赠酒补偿 = **bar 域每次核销按该商品内部结算价(真实进货成本)给出品酒吧挂应付**(`pass_drink_settlement`·幂等 + 可冲正)= **唯一补偿口径**;**未核销的杯数不发生**该补偿(按核销结算),酒吧按内部结算价**不赚不亏**。**不再从通票收入/可分配池中先留赠酒成本**(每杯 3 元池扣口径已删,见下 D5)。
- **第二段 — 各酒吧总池按各酒吧协议每客分账**:各酒吧总池(§5.0 ④,**D5 后不预留赠酒成本**)按各酒吧协议"每进一个客人给几元"分给对应酒吧 SPV(`spv_per_guest_rate`,**各酒吧协议不同、后台按门店可配**),即 §5.1 的 `by_venue_per_guest` 分账方式。
- **触发**:两段均**按实际核销/入场发生**;客人未核销该点位赠酒则第一段成本不发生,通票整体未核销的余额归项目方(未用归项目,原则 2)。
- **实装**:赠酒补偿由 bar 域每次核销按内部结算价给出品酒吧挂应付(`pass_drink_settlement`,第一段·D5,非"项目方→SPV 结算单一项");每客分账作为月度结算单明细。

> ⚠️ **删除旧口径**:原写"SPV 承担赠酒成本""中央餐饮按成本供应 SPV""不进 75% 毛利规则"——① 前两句是财务测算的成本归集,与"未核销不结算"**自相矛盾**(成本若绑 SPV,则"未核销归项目"不成立,用户 2026-07-04 指出);② "75% 毛利规则"**底稿全文查无此项**,系虚构表述,删除。系统内赠酒成本**只在核销时按实际访问点位发生**,不存在"SPV 先垫再核销"。

> **D5(2026-07-21 用户拍板删)** —— 删除引擎「每杯 3 元」池扣:3 元是财务测算假设(附录 B·不进系统),且与第一段赠酒补偿的**内部结算价应付对同一杯赠酒双重计**;删后**可分配池不再被赠酒缩小**(§5.0 ④ 池 = 收入 − 渠道费)。第二段「剩余按每客分账」不变。

### 5.4 访问点位
- `visitable_points_count`:**可配参数**,默认 10(当前 9 酒吧 + 1 Live Show);随门店增减而变,**非固定事实**;
- 用于赠酒补偿计算与 `by_visited_point_average` 分账法(后者仅为简化/过渡口径,见 §5.1)。

---

## 6. 权益结算规则

### 6.1 实物 vs 虚拟权益(用户口径 3)
| 类型 | 结算价来源 | 现状 |
|---|---|---|
| **实物权益**(酒水等) | **内部结算价** `internal_settlement_price_cent` | ✅ 酒吧商品已有(V152),复用/扩展 |
| **虚拟权益**(入场资格/服务) | **签约权益单价** `agreement_price_cent` | ❌ 新增字段 + 配置 |
- 权益模板增标记 `entitlement_kind ∈ {physical, virtual}` 决定取哪个价。

> ✅ **已落地(批3-1,V225,main=eb5cba5)**:`entitlement_templates`/`issued_entitlements` 加 `entitlement_kind`+`agreement_price_cent`,`ticket_verifications` 加 `settlement_price_cent`;核销时 `TicketVerificationService.resolveSettlementPrice` 判定——实物经 `bar_item_ref`→`bar_products.internal_settlement_price_cent`(V152)回退 value,虚拟取 `agreement_price_cent` 回退 value,NULL 回退 value;落该次核销记录。

### 6.2 酒吧入场资格——场次规则(用户口径 4,全新)
- 入场资格权益的核销单位是**场次**(一个"开业→歇业"周期):
  - 客人当场次**入过场**后,该场次内可**持续进出**(不重复消耗);
  - 即使门票有效期未到,一旦某场次已入场,**过了该场次(下一个开歇业周期)即作废**,不能再用;
- 新建 `venue_sessions`(场次/营业周期)+ `entitlement_entry_records`(入场记录);核销校验按场次状态机。
- **场次自动触发(2026-07-04 定)**:系统按各酒吧**营业时间表**(新增 `venue_business_hours` 配置)自动划场次——开业开场、歇业关场;入场资格权益随场次结束自动作废,无需人工开关。

> ✅ **已落地(批3-2,V226,main=98a7c50)**:新引入 `consumption_window`(per_use/per_session/per_day/unlimited_in_day)判别位,`per_session`=入场资格;建 `venue_business_hours`(营业时间表)+`venue_sessions`(场次实例)+`entitlement_entry_records`(入场记录);核销惰性开场(按营业时间表判当前是否营业·跨零点延续归同场次·非营业时段拒 `VENUE_NOT_IN_SESSION`);同场次二次核销放行不消耗、跨场次已入过场 `ENTRY_PASS_PAST_SESSION_VOID` 作废、唯一键防并发重扣。

### 6.3 游乐——当天无限 + 票种规则(用户口径 5,2026-07-04 扩)
- **当天无限核心机制**:游乐权益按**自然日**核销——每次核销激活「一个整天无限」:当天首刷即消耗一次、当天内再刷卡/扫码/多人再进=放行**不重复消耗不重复计费**;跨自然日=新的一天从剩余次数再消耗一次。
- 权益模板增 `consumption_window ∈ {per_use, per_session, per_day, unlimited_in_day}`(`unlimited_in_day`=当天无限)。
- **票种规则(2026-07-04 用户拍板,游乐票同酒吧通票:有有效期、期内可用)**:
  - **单人票**:有效期内任选一天首刷→当天无限玩→**跨日作废**(即使有效期未到,次日票即废)。靠 `total_times=1`(次日核销即 `ENTITLEMENT_USED_UP`)。
  - **多人票(N 人)**:规则同单人票,唯一差异=**当天可核销 N 次**(N 人当天各入场随便玩);同天用不完的额度**跨日作废剩余**。(单日型+跨日废剩余标记 → **批 4 票种**。)
  - **多次票(N 次)**:有效期内 N 次、**每次核销=一整天无限**、**跨日保留剩余**(买 3 次,第 1 次核销完,有效期内第 2/3 天还能各核销一次)。靠 `total_times=N`(天天各扣一次)。
  - **新人票/学生票**:规则+作废规则**同酒吧通票**。
  - 票种细分(单人/多人/新人/学生)、多人票「同天 N 次+跨日废剩余」、`num_people` 语义 → **批 4 票种**集中实现。

> ✅ **已落地(批3-3,V227,main=d36d0a5)**:`unlimited_in_day` 的「按自然日激活」——建 `entitlement_day_activations`(唯一键 entitlement_id+usage_date);
> 当天已激活放行不消耗、当天首刷且剩余>0 才扣次(唯一键防并发重扣)、剩余=0 不插激活让其走 `ENTITLEMENT_USED_UP`;
> **复用现有 `total_times`**——单人票 total=1 天然跨日作废、多次票 total=N 天然跨日保留,不重造次数模型。

### 6.4 专属某商户不分摊(用户口径 9)
- 权益标记 `exclusive_merchant_id` 非空时:该权益核销收入**全额归该商户**,不进 SPV 分账池、不平均分;
- 适用"仅限某酒吧的酒水/服务"单点消费(酒吧内或第三方购买)。

> ✅ **已落地(批3-4,V228,main=d0e0e60)**:`entitlement_templates`/`issued_entitlements`/`settlement_transactions` 加 `exclusive_merchant_id`(核销双写定格结算源真相);月度引擎 split——池基/排名/客数聚合加 `exclusive_merchant_id IS NULL` 过滤(专属不进池不稀释),专属核销按 `exclusive_merchant_id` 分组**全额直付**明细(`method=exclusive_full`·不扣留存·不进 spvPool·明细 id 加 `_excl` 后缀防撞);越池校验只算池分账。

### 6.5 游乐票购买人脸拍照存档(用户口径,2026-07-04 新增)
- **游乐票购买时做人脸校验、拍一张照片加密存档;核销时后台显示该照片**,核销员据此确认核销的是本人(防转票/人票一致)。区别于 `entry/` 人脸入场闸(判 18+ 年龄闸·不存照)。
- **适用范围**:全票种**可配**·游乐票(`entitlement_type='amusement'`)**默认开**——权益模板级开关 `require_face_photo`(禁硬编码,游乐默认开是种子回填)。
- **保留期**:票核销完/失效后**短期自清(valid_to+30 天)**——生物识别敏感信息(PIPL),越短越安全。
- **存储/访问**:复用 `PiiCipher`(AES-GCM)+`KeyVault` **加密存库**(密文,禁明文);取照限核销/运营角色(`verification.view`,无权 403)+**每次查看留审计**(`ticket_face_photo.viewed`)。

> ✅ **后端已落地(批3-6,V229,main=a545261)**:建 `ticket_face_photos`(entitlement_id 唯一·密文 BYTEA·purge_after);
> 发放时 `require_face_photo=TRUE` 缺照拒 `FACE_PHOTO_REQUIRED`(建权益行前拒·两发放入口 `@Transactional` 无孤儿)、有照 PiiCipher 加密存;
> `GET /ticketing/issued-entitlements/{id}/face-photo` 授权+审计取照;`TicketFacePhotoPurgeJob` 按 purge_after 硬删(默认关)。
> **前端待办**:①H5 购票拍照 ②后台核销页显示脸照 ③模板编辑 `require_face_photo` 勾选。

---

## 7. 各票种结算规则

### 7.1 通票(仅项目公司可售 —— 用户口径 1)
- 售卖主体限制:通票类票种**只有项目公司(平台)角色**能创建/售卖,商户不能;后端加销售主体校验(权限点 + 主体身份双重)。
- 结算走 §5 分账引擎。

### 7.2 单独门票——综合服务费(用户口径 8 + "所有门票都抽",用户裁定)
> ✅ **已落地(V268)**:建 `comprehensive_service_fee_configs`(项目×商户×票种范围可配·`fee_bps` 默认 0=不抽·禁硬编码)。落点=月度分账引擎专属商户直付线(单独门票走此线):直付前项目公司按配置费率抽综合服务费,商户拿净额(收入−服务费),服务费归项目记入 `settlement_bills.comp_service_fee_cent`。**默认费率 0 → 商户拿全额 → 零行为变更**(现有 batch34 专属直付测试断言全额不变、仍绿)。通票的"留存 20%"仍是同族另一条规则(`platform_retention_rules` §5.1 第一层),本表专管单独/专属门票那道。
- 客人购买某商户的单独门票(任意渠道):项目公司先抽**综合服务费/项目管理费**(= 平台留存规则 §5.1,费率项目↔商户可洽谈、可配),余额结给该商户;
- **所有门票**(含通票)都过平台留存这一道——通票的"留存 20%"与单独门票的"综合服务费"是**同一条留存规则**的不同费率实例。
> ✅ **已落地(V328)**:综合服务费现支持**比例或固定额二选一**——配固定额(`fixed_amount_cent`)则费率(`fee_bps`)须为 0(DB 约束 `chk_csfc_fee_mode` 保证二者不并存);默认仍费率 0 → 零行为变更。
> **固定额口径(用户 2026-07-21 拍板 A)**:固定额=**每张票固定额 × 核销张数**(对齐「所有门票都过一道」=每张都过),**非**「每票种/每月整包一口价」;引擎实扣 `min(固定额 × 核销张数, 本笔毛额)`(上限本笔收入防净额为负)。比例口径不受影响(比例天然随每张收入缩放)。

### 7.3 单点消费(用户口径 9)
- 仅某商户的酒水/服务消费(非票务),全额归该商户,不分摊(见 §6.4)。

### 7.4 套票(用户口径 10,全新)
- 套票 = 跨域权益组合(如酒吧+游乐)且整体打折;
- 折扣归属可配 `discount_bearer ∈ {platform_absorb, merchant_absorb, split_by_ratio}`:
  - 平台承担:各权益按原单价结算,折扣差额由平台留存吸收;
  - 商户分摊:折扣按各权益单价占比分摊到各商户结算价;
  - 按比例:可配比例拆分。
> ✅ **已落地(V268 配置 → D6/V331 折扣额落库应用)**:建 `ticket_combo_discount_configs`(项目级可配 `discount_bearer ∈ {platform_absorb, merchant_absorb, split_by_ratio}` + `split_platform_bps`)。默认 `platform_absorb`(项目承担折扣)=商户结算不受折扣影响,与现状一致,**零行为变更**。
> **D6(V331)套票折扣额已走真实售卖/核销路径落库**:售卖时快照满额价 `issued_entitlements.component_full_price_cent` → 核销时算折扣增量 `settlement_transactions.discount_amount_cent` 下传 #179 append-only 账本;结算按 `discount_bearer` 应用——`platform_absorb`(商户拿满额、折扣记 `settlement_bills.combo_discount_cent` 归项目)/ `merchant_absorb`(商户拿净额)/ `split_by_ratio`(按 `split_platform_bps` 拆)。**满额价空 → 折扣 0 → 零行为变更。**
> **跨月冲正(V335·套票真上线前必修)**:项目承担折扣<b>带符号</b>——核销为正(平台吸收),该核销在<b>后续已过账月</b>被冲正时为负(平台<b>追回</b>先前吸收),与收入侧 `verified_amount_cent` 带符号跨月净掉一致。原 `platformBorneDiscount` 的 `discountCent≤0` 钳0 会丢弃跨月负向追回 → 收入 -R 追回了、折扣 -D 没追回,项目多付;V335 放开 `combo_discount_cent` 的 ≥0 约束、去钳。⚠️ 已知天花板:冲正当月须有抵充收入(否则 `ticket_revenue<0` 撞 `chk_sb_amount`,与收入侧同限,非本修引入)。

### 7.5 第三方通票(用户口径 7)
- 美团/抖音等第三方售出的通票**适用整套规则**(权益拆分、按核销结算、未用归项目、手续费预扣);
- 依赖第三方回传核销 → 新建第三方核销回传对接 + 对账;
- 收入进 `integration_order_syncs`(§13 需补 merchant_id/spv 映射)。

---

## 8. 退款与手续费预扣(用户口径 6)

- 退票按**同一套核销规则**逆向冲正:仅冲正**已核销**部分对应的结算,未核销部分本就未结算给商户;
- **手续费由平台公司预扣**:退款金额先扣手续费(费率可配,按支付渠道/票种),再退客人;手续费归平台;
- 新建 `refund_fee_rules`(渠道/票种 → 费率 + 预扣时机);退款单增手续费字段与冲正明细。

---

## 9. 结算配置中心(可配置载体,全新)

现状 `settlement_rules` 表**文档有、代码无建表 SQL**。**配置中心已建成(批0+批1)**——**配置表族 + 后台配置页 + 权限点**已落地,承载 §3 原则 3 的一切可配项:

| 配置表 | 承载 | 后台页(建议编号) |
|---|---|---|
| `platform_retention_rules` | 平台留存/综合服务费(通票+单独门票) | ADM-STL-001 |
| `spv_distribution_configs` | 分账方案选择 + 参数(方法/点位数/中央餐饮开关) | ADM-STL-002 |
| `entitlement_pricing_rules` | 实物内部价 / 虚拟权益单价 | ADM-STL-003 |
| `refund_fee_rules` | 退款手续费预扣 | ADM-STL-004 |
| `ticket_discount_rules` | 套票折扣归属 | ADM-STL-005 |
| `giveaway_cost_configs` | 赠酒单价/点位/上限 | ADM-STL-006 |
| `revenue_stream_attribution`(V319,已落库) | 6 业态收入流 → 对应 L3 管理公司主体(all_pass_ticket/table_dining/bar_service_fee/amusement_ticket/performance_ticket/venue_rent);项目特例 > 平台默认 > 未配置 null,后台可配 | ADM-STL-007 |
| ↳ 通票结算标注 | `settlement_bills.attributed_entity_id`(V319):settleMonth 按 (项目, all_pass_ticket) 解析归属主体并标注,不改任何金额 | — |
| ~~`revenue_attribution_rules`(扩 V221)~~ | ⚠️ V221 `merchant_revenue_attribution_rules` 是商户级 net 口径(ticket/bar/rent 计入方式),粒度不同,非 6 主体归属;后者由上表 `revenue_stream_attribution` 承载 | — |
| `tax_config`(税务全可配,2026-07-04) | 每主体×收入类型的 增值税/文化事业建设费/附加税(**有无 + 费率**均后台填) | ADM-STL-010 |
- 均带 `is_draft`(草案态,复核后转正)、`effective_from/to`、`updated_by`、审计;
- 权限点 `settlement.config.view/manage`,限项目公司财务/运营角色。

> **2026-07-20 新立项批0 实现修订(配置中心骨架落地对齐)**:
> - 配置中心落地为**统一总控页 ADM-STL-001**(「看全局」概览 + 草案复核队列);各配置的可视化维护作为其内栏目/标签随新立项批1-5 接入,不再是本节原设想的「每表一页」(ADM-STL-002~010 编号保留作栏目锚点)。
> - 配置表族**实际建表名**与本节 proposed 名对应:分账方案=`ticket_distribution_configs`+`ticket_distribution_rank_tiers`+`ticket_distribution_merchant_rates`(非 `spv_distribution_configs`);门票综合服务费=`comprehensive_service_fee_configs`;套票折扣归属=`ticket_combo_discount_configs`(非 `ticket_discount_rules`);税费=`settlement_tax_configs`(非 `tax_config`,V223 已建、待批2 复活消费)。
> - **ADM-STL-004 `refund_fee_rules`(退款手续费预扣)作废**——退款手续费预扣 2026-07-04 已裁定不建。
> - 权限点已落 **V326**:`settlement.config.view/manage` + `settlement.distribution.manage`(维护/分账高敏 requires_approval);复核闸批0 为**轻量式**(高敏权限+前态守卫+审计),资金级配置(分红批4)再上 supervisor_review 重审批。
> - 读取侧统一强制 `is_draft=FALSE`(草案不生效);现存种子经 V326 一次性转正(视同已批准的当前生效值,零行为变化)。

> **2026-07-21 批1 实现修订(配置中心已建成·增量落地对齐)**:
> - **已落地(批0+批1)**:统一总控页 **ADM-STL-001**(全局概览 + **通票分账 8 配置块** + 草案复核队列)+ **8 类配置写入面** + **转正泛化端点**(一个端点转正各类草案)。
> - **双人复核**:差人转正——执行人 ≠ 起草人,否则 `SELF_ACTIVATION_FORBIDDEN`。
> - **Path 2 保值**:草案**不影响正在进行的结算**(对齐已批准业务 HTML)。
> - **回退收敛**:缺配置 **fail-loud**,不再落第二份写死默认。
> - **表映射校正(补批0)**:**新增 `channel_fee_configs`**(渠道费率·消灭写死 0·ADM-STL 通票分账栏);`giveaway_cost_configs` → **D5 后已废**(赠酒不占池,§5.3);`refund_fee_rules` → **作废**(退款手续费 2026-07-04 取消·§8/附录 E);`tax_config` = `settlement_tax_configs`(三维税表·零消费待接引擎)。分账方案 `spv_distribution_configs` → `ticket_distribution_configs` + `ticket_distribution_rank_tiers` + `ticket_distribution_merchant_rates`(重申 2026-07-20 note)。
> - **权限点补**:`settlement.bill.post`(过账·高敏 requires_approval)、`settlement.distribution.manage`(分账/超池特批·高敏)。

---

## 10. 数据模型变更清单

### 10.1 新建表
- **管理公司/业态主体层**(L3:娱乐平台/酒吧SPV/中央餐饮/游乐/传媒/资产;持股/分红配置)——⚠️ 2026-07-04 对齐代码:**DB 层复用 `business_entities` + `entity_role` 承载(现状无 organizations 表),不新建 management_companies 表**;四主体绑定见 `business_entity_bindings`(active 唯一);`projects` 表是业态经营单元/场馆(非"项目公司")。详见 `merchant-space-contract-model-v0.1.md`
  - **2026-07-19 落库(V318/V319)**:①`business_entities.parent_entity_id`(V318)显式挂 L3→L2 项目公司父链(「一个项目公司下多个管理公司主体」= 多行 parent 指向同一项目公司);②`revenue_stream_attribution`(V319)承载 6 业态收入流→管理公司主体映射(可配,项目特例 > 平台默认);③`settlement_bills.attributed_entity_id`(V319)通票分账已接:settleMonth 解析并标注归属主体。其余 5 类收入流(餐饮/服务费/游乐/演出/租金)复用同一 `RevenueStreamAttributionRepository.resolveManagementEntity` 解析,随各自结算路径上线时接入。
- `venue_sessions`(场次/开歇业周期)+ `entitlement_entry_records`(入场记录)
- `settlement_transactions`(按核销的结算流水:票→权益→核销→主体→金额→税务属性)
- `settlement_bills` / `settlement_items`(结算单 + 明细,对齐底稿口径)
- `unused_entitlement_reversals`(未用权益归平台的回收记录)
- §9 配置表族
- `refund_fee_rules`

### 10.2 改表
- `merchants`:加 `management_company_id`(挂靠 L3)+ `business_category`(业态:酒吧/餐饮/零售/游乐…,供租售比业态化 §13)
- `entitlement_templates`:加 `entitlement_kind`(实物/虚拟)、`agreement_price_cent`、`consumption_window`、`exclusive_merchant_id`
- `issued_entitlements`:结算状态从"发放即分摊"迁移为"待核销/已核销结算/已回收"(§11)
- `ticket_types`:加 `sales_subject_restriction`(仅项目公司可售通票)、`is_standalone` 标记
- `refund_orders`:加手续费预扣字段
- `integration_order_syncs`:加 `merchant_id`/`management_company_id` 映射(§13)
- `settlement_transactions`(审计 AUD-012/013/017):转不可变账务事件账本——加 `business_at`(业务时间,account 按此落月窗)、`chk_st_type` 增 `verification_reversed`、`chk_st_amount` 令冲正行恒 ≤0(资金守恒 DB 守卫)、删「每权益一行」`uq_st_entitlement_verified`(改按 `verification_id`/全局 `transaction_no` 幂等 append)
- `ticket_refunds`(审计 AUD-018):加 `succeeded_at`(有效退款生效时刻)+ 权威枚举 `chk_ticket_refunds_status`(已申请/审批中/已通过/已驳回/处理中/成功/失败/已取消);财务/分析/分红仅统计 `status='success'` 按 `succeeded_at` 落窗

### 10.3 批1(配置中心)迁移清单(2026-07-21)
- **V327** `channel_fee_configs`(渠道费率配置,消灭写死 0)
- **V328** `comprehensive_service_fee_configs.fixed_amount_cent`(综合服务费比例/固定额二选一·`chk_csfc_fee_mode` 约束)
- **V329** 六配置表复合唯一 `(scope, is_draft) WHERE status='active'`(草案与生效值并存)+ `revenue_stream_attribution` 加 `is_draft` + `merchant_revenue_attribution_rules` 升 Path 2(草案不影响进行中结算)
- **V330** `settlement.bill.post` 权限点(过账·高敏 requires_approval)
- **V331** `issued_entitlements.component_full_price_cent`(售卖快照满额价)+ `settlement_transactions.discount_amount_cent`(核销折扣增量·扩 `chk_st_amount` 允许 ± 守恒)+ `settlement_bills.combo_discount_cent`(平台吸收折扣归项目)
- **V335** 放开 `settlement_bills.combo_discount_cent` 的 ≥0 约束(`chk_sb_combo_discount` drop·改带符号)——套票折扣<b>跨月冲正</b>时项目承担折扣可为负=追回,与收入侧 `verified_amount_cent` 带符号跨月净掉一致(修 `platformBorneDiscount` 的 `discountCent≤0` 钳0 致跨月不对称、项目多付;套票真上线前必修)

---

## 11. 从现状迁移(发放即分摊 → 按核销结算)

风险最高的一步,分三阶段并行校验:
1. **双写观察**:新引擎按核销结算写 `settlement_transactions`,旧 `allocated_revenue_cent` 保留;analytics 同时出两套数,比对差异(未核销部分即差异来源)。
2. **切换看板口径**:确认新引擎正确后,商户净收入看板从"发放分摊"切到"按核销结算";未用部分归平台单列。
3. **停用旧路径**:`allocateRevenue` 发放即分摊逻辑降级为"仅登记定价与归属,不入收入账"。
- 历史已出票据:按票有效期,新引擎从切换日起对未来核销生效;存量已分摊数据保留为历史口径,不追溯重算(除非财务要求)。

---

## 12. 名词统一与文档同步范围(结算重构完一起做 —— 用户裁定)

回写(业务语言,业务 HTML 为最终解释权):
- **业务 HTML 三份**:`business-system-map`(1632 行,逐处"项目/综合体/平台"按四层校正)、`business-enum-quick-reference`(四类主体表补管理公司层)、`platform-governance-map`;
- **PRD/设计文档**:`product-requirements`、`foundation-database-design`、`platform-governance-product-spec`、`group-governance-rebuild-prd`、`ticketing-entitlement-settlement-breakdown`(§7 分账整段重写对齐本 PRD)、`finance-settlement-product-prd`;
- **追溯矩阵**:结算相关 REQ 回写;
- 统一术语表落一份 `docs/terminology-canonical-v1.0.md`,四层定义 + 各业态主体专名。

---

## 13. 租售比业态化(用户口径:业态级+商户级都要)

- `merchants.business_category` 落地 → 支持**业态级租售比**(酒吧/餐饮/零售各自 租金÷营业额);
- **商户级/店级租售比**依赖营业额分母能拆到店:给 `integration_order_syncs` 补 `merchant_id`/门店映射(从 `external_channel_order_id`+`channel`+payload 反查,或要求第三方回传门店号);第三方给不到店级数据的部分,明确标注"仅平台级"不静默。
- 分子(租金)`ar_ap_items` 已带 merchant_id,现成。

---

## 14. 分批实施计划(建议)

| 批 | 内容 | 依赖 | 产出 |
|---|---|---|---|
| **批 0** | 本 PRD 评审定稿 + 术语表 + 配置表族 schema 设计 | — | 定稿 PRD、DDL 草案 |
| **批 1(地基)** | 按核销结算引擎 + 未用归项目 + settlement_transactions + 双写观察 | 批0 | 核心引擎 + 校验 |
| **批 2** | 管理公司层(平台+酒吧SPV)+ 两层分账方案(可配)+ 中央餐饮开关 | 批1 | 分账落地 |
| **批 3** | 权益结算细则:实物内部价/虚拟权益单价/场次入场/游乐当天无限/专属不分摊 | 批1 | 权益精算 |
| **批 4** | 票种规则:综合服务费(所有门票)/单独门票/套票折扣 + 退款手续费预扣 | 批2/3 | 票种闭环 |
| **批 5** | 配置中心配置表 + 后台页(ADM-STL-001~007、010 税务)+ 权限 | 批2-4 | 全可配 |
| **批 6** | 第三方通票对接 + 核销回传对账 | 批1-4 | 第三方 |
| **批 7** | 名词统一 + 全文档同步 + 业务 HTML 回写 + 追溯矩阵 | 批1-5 | 文档追平 |
| **批 8** | 租售比业态化 + integration_order_syncs 补 merchant 映射 | 批7 | 租售比 |

---

### 14.1 实施进度(2026-07-04 起,代码/迁移落地持续回写)

| 批 | 状态 | 落地内容 |
|---|---|---|
| 批 0 | ✅ 完成 | 术语表 `terminology-canonical-v1.0` + 本 PRD v1.0 定稿 |
| 批 1(按核销结算地基) | ✅ 完成 | **V222**(`settlement_transactions` + 对账表)、核销双写(`SettlementSettledEvent`/Consumer,AFTER_COMMIT 不影响核销主流程、金额三段 verified+unverified=allocated 幂等)、`SettlementReconciliationJob` 新旧对账、`SettlementExpiryJob` 未核销归项目(`allocated×未核销次数/总次数`);旧 `allocated_revenue_cent` 保留双写观察、不切口径;测试全绿 |
| 批 2(分账引擎) | ✅ 完成 | **V223**(6 配置表)、**V224**(`settlement_bills`+`settlement_items`)、月度分账引擎 `settleMonth`(算池/留存/三方式分配/超额待特批)、批 2-4 过账 `ar_ap_items`(`SettlementPostingService`·幂等键 SHA-256 前 20 hex 防明细撞·批2-5 逐家真断言证伪)、批 2-5 端到端全绿;**丢账 bug 已修 main=c6f3623** |
| 批 3(权益细则 §6.1-6.4+人脸照) | ✅ 完成 | **3-1** 结算价判定(V225·实物内部价/虚拟签约价/回退)、**3-2** 场次入场(V226·consumption_window+营业时间表+场次+入场记录·惰性开场·过场作废)、**3-3** 当天无限(V227·按自然日激活·单人跨日作废/多次跨日保留靠 total_times)、**3-4** 专属不分摊(V228·exclusive_merchant_id 全额直付不进池)、**3-6** 游乐票人脸拍照存档(V229·PiiCipher 加密·授权+审计取照·30 天自清);每片自带证伪测试+回归全绿;main=a545261。**票种细分(单人/多人/多次/新人/学生)→ 批 4** |
| 批 4(票种分类·收缩后小尾巴) | ✅ 完成 | **V230**(`ticket_types` 加 `ticket_category` 资格/定价档标签:standard/new_person/student,仅供后台分类/报表,不碰定价/消耗/作废逻辑;新人票/学生票=完全同酒吧通票靠独立 ticket_type+定价;结构型单人/多人/多次由 num_people+total_times 派生不入本字段);仅加列+回填可独立回滚;4f6eb96。**批 4 原计划的综合服务费/单独门票/套票折扣/退款手续费预扣仍未做,归入批 5 配置中心 + 各票种规则后续实现** |
| 前端配套(可点击/行明细/中文化/时间戳) | ✅ 完成 | 全链可点击下钻(引用字段 merchant/contract/order/project/space/处理人/审计对象全站可点·统一 `<TraceLink>`)、KPI 数字可点、全站 ~60 表页整行→右侧明细抽屉(`RowDetailDrawer`)、全站零英文(枚举/状态/时间 cnDate/CSV 导出)、原生日期控件→自研 `CnDatePicker`;**下单业务记支付时间 `paid_at`(V231)**、**物流配送到位时间 `bar_order_items.delivered_at`+游乐核销场次展示(V232)**;全部已 push origin/main(tip=7502a08) |
| 迁移号 | — | V222–V232(结算重构)已用;**V268**(综合服务费 + 套票折扣归属配置);**V323/V324**(状态机加固:结算账本不可变 + 退款终态);全仓下一空闲号见 migration 目录 |
| 收尾(§7.2 综合服务费 + §7.4 套票折扣归属) | ✅ 完成 | **V268**:`comprehensive_service_fee_configs`(所有门票综合服务费,专属直付线抽,默认 0 零变更) + `ticket_combo_discount_configs`(套票折扣归属,默认 platform_absorb) + `settlement_bills.comp_service_fee_cent`;引擎按票种拆专属聚合命中费率、商户拿净额、费归项目;5 测试(默认 0/配置费率/多商户独立/按票种命中/折扣配置)+ 证伪(破扣费 3 红)全绿;退款手续费预扣按用户 2026-07-04 取消**不建** |
| **新立项·批0(结算配置中心骨架,2026-07-20 用户拍板)** | ✅ 开发完成·待主计划集成 | **V326**:结算配置权限点(`settlement.config.view/manage`+`settlement.distribution.manage`)+ 9 张配置表现存种子一次性转正 `is_draft→FALSE`;读取侧统一强制 `is_draft=FALSE`(堵「草案即生效」漏洞:`JdbcSettlementBillRepository` 7 个 finder + 归因规则应用路径)+ 归因规则编辑一律落草案 + 转正端点(`SettlementConfigController`/`Service`·高敏 `settlement.config.manage`+前态守卫+审计)+ 只读概览端点 `GET /api/v1/settlement/config/overview`;前端新增顶级导航「结算配置中心」+ 总控页 **ADM-STL-001**(看全局+草案复核队列)。3 例集成测试全绿(草案不生效/种子转正零变化/转正+前态守卫)。⚠️ 新立项批号(批0-5)≠ 本 PRD §14 原批号(批0-8)。分支 `feat/settlement-config-center-batch0`,rebase 到集成后 main、待统一集成 |
| **新立项·批1(结算配置中心增量,2026-07-21)** | ✅ 开发完成·待主计划集成(worktree) | **D1 渠道费率**(**V327** `channel_fee_configs`·消灭写死 0)/ **B1-2 综合服务费固定额**(**V328** `fixed_amount_cent`·比例或固定额二选一)/ **B1-3 配置写入面全套** + 转正泛化端点 + 双人复核(执行人≠起草人 `SELF_ACTIVATION_FORBIDDEN`)+ Path 2 保值(草案不影响进行中结算)+ 回退收敛(缺配置 fail-loud)(**V329/V330**)/ **D5 削赠酒池扣**(赠酒不占池·补偿改 bar 域内部结算价挂应付)/ **过账 pipeline ①②**(`POST /settlement/bills/{id}/post`·**默认关·生产首次激活另行拍板**)/ **D6 套票折扣落库 + 应用**(**V331** `component_full_price_cent`→`discount_amount_cent`→`combo_discount_cent`)/ **B1-4 前端**/ **✅ D7 超池特批出口 + 超额 C 口径(甲标签法)**——特批端点 `POST /settlement/bills/{id}/special-approval`(复用 `settlement.bill.post`·仅超池 draft 可批·拒 `BILL_NOT_OVERPOOL_DRAFT`)+ `postBill` 两段拆(池内 `pool_i` 过 `ticket_distribution`/超额 `excess_i` 过 `platform_incentive_subsidy`·独立幂等前缀 `settle-pis-`·`pool_i+excess_i=a_i`/`Σpool≤spvPool`)+ 专属直付不拆/未超池零变更 + `upsertBill` 防覆盖(COALESCE ref/CASE 锁 status 不被重跑打回)·**零迁移**(`source_type` 自由 VARCHAR·复用权限)·`SettlementOverPoolSpecialApprovalTests` **9 绿**(6 验收 + 3 对抗复核回归锁:溢出安全/特批冻结/补贴 project_id NULL 出项目域)。**迁移 **V327–V331** 已落 worktree(D7 无迁移,故无 V332)** |
| **报表·放弃权益额外收入(结算B2,2026-07-21)** | ✅ 开发完成·待主计划集成(worktree) | 只读财务报表 **ADM-ANA-013**「放弃权益额外收入」:客人买票但已过期未核销的权益,其未核销部分归项目公司的额外收入,按 **票种(通票 `ie.bar_id IS NULL` / 单独门票 `bar_id` 非空)× 权益类型(酒水/入场/游乐/演出/其他,沿用 analytics 分桶)** 交叉呈现,含行合计/列合计/总计。数据源直读 `SettlementExpiryJob` 每日 02:30 写好的 `settlement_transactions`(`transaction_type='unverified_expired'` + `beneficiary_type='project'`)归项目流水,金额取权威 `unverified_amount_cent`(不重算口径)、笔数 `COUNT(*)`,窗口 `[start(含), end(不含))` 经营日切(`BusinessDayResolver`)。端点 `GET /api/v1/analytics/abandoned-entitlement-revenue`(复用 `analytics.dashboard.view` 只读权限 + `requirePlatformScope`);**零迁移(纯读)**。项目维度过滤暂不做(归项目流水 `project_id` 恒 NULL,诚实标注全局+日期)。证伪测试 `AbandonedEntitlementRevenueJdbcIntegrationTests` 逐格断言(通票×酒水多行求和/单独门票×入场/游乐/空类型=0/窗外不计/非 unverified_expired 不计)1 绿;门禁 `verify:full-admin-matrix`+`verify:p0-contract` 全绿。分支 `feat/settlement-b2-abandon-report`,待统一集成 |

| 状态机加固(审计 AUD-012/013/017/018/019·资金正确性核心) | ✅ 代码落地·PR 待并 | **V323**(`settlement_transactions` 转不可变账务事件账本)+**V324**(`ticket_refunds` 终态)。①核销按 `verification_id` 写不可变**增量**结算行(增量=f(k)−f(k−1),`business_at` 落月→跨月各归其月,取代累计值覆盖首插行)②核销冲正追加**负向**结算 `ST-VR`(严格取原行反;原月未过账则该月净重算、已过账则落当月红冲不改历史)③退款仅 `success` 且按 `succeeded_at` 聚合(finance/analytics/dividend 四处),修 `FinanceJdbcIntegrationTests`「`requested` 计入退款」错误绿灯④到期归项目「可没收终态」白名单=`status='active'`+父实例非冻结(排除已成功退款/退款申请中)⑤消费者幂等 append-only(重放 DO NOTHING)+ `SettlementReconciliationJob.rebuildMissingSettlements` 按核销权威事实重建缺失结算(AUD-017 outbox 之替代;全量事务性 outbox+死信为后续上限)。资金恒等自检 `SettlementAmountsTests`(非DB) + 门禁 `SettlementStateMachineJdbcIntegrationTests`/`FinanceJdbcIntegrationTests`(DB,主会话串行跑) |

> 🔒部署前核对(状态机加固·复核提示):①`ticket_refunds` 加 status CHECK 前先 `SELECT DISTINCT status FROM ticket_refunds` 确认无八值外脏值;②重建 `rebuildMissingSettlements` 为本 PR 全新、生产从未跑过(基线 d577fc4b 无此方法),库内不应有「同权益既有遗留累计行 verification_id IS NULL、又有 per-verification 行」的双计残留——部署前核对下句应返回 0 行(非 0 先排查、勿开 `settlement.reconciliation.scheduled` 重建):`SELECT s.entitlement_id FROM settlement_transactions s WHERE s.transaction_type='verification_settled' AND s.verification_id IS NOT NULL AND EXISTS(SELECT 1 FROM settlement_transactions leg WHERE leg.entitlement_id=s.entitlement_id AND leg.transaction_type='verification_settled' AND leg.verification_id IS NULL) GROUP BY s.entitlement_id;`。注:`settlement.reconciliation.scheduled.enabled` 生产=true(application-prod.yml:31),重建首夜即跑,故上述前置校验为部署闸。

> 渠道费明细来源(§5.0 步骤②)现无数据源,引擎暂置 0,待第三方渠道费率接入补(批 6)。

---

## 15. 待决清单

### 已定(2026-07-04 用户评审)
1. ✅ **SPV 建模**:**真实建 N 家**(9 酒吧+1 LiveShow),SPV 数量**可配**(支持增减店),商户挂到具体 SPV、结算精到单店。→ 影响 §2.3、§10。
2. ✅ **历史数据**:**仅新票据走新引擎,不追溯重算**;切换日起新出票据按核销结算,存量保留旧口径。→ 确认 §11。
3. ✅ **持股分红进系统**:系统一直算到**主理人/平台分红到手**(平台 80% 持股 SPV、主理人 20% 分红)。→ 新增持股分红层,见附录 B。
4. ✅ **结算做到开票清单级**:系统出到底稿 `23_开票清单模板`粒度(开票项/开票主体/税率/凭证要件),不止结算单+税务标注。→ 新增开票模块,见附录 B。

### 已定(2026-07-04 续)
5. ✅ **中央餐饮开关默认 = 开**:酒水销售(额外酒水/订台酒水)默认归中央餐饮公司;可后台切关(改归酒吧)。
6. ✅ **场次按营业时间自动触发**:系统按各酒吧营业时间表自动划场次(开业开场、歇业关场),入场资格过场次自动作废;无需人工开关。→ 需先配"营业时间表",见 §6.2。

---

## 附录 A. 财务底稿参数清单(各配置项默认种子值)
> 从底稿 05_基础参数/08_通票与分账/09_订台/10_酒水/14_中央餐饮/15_游乐传媒/23_开票/26_会员积分/04_税务结构 提炼。以下"✓可配"项 = 系统配置表种子值,后台可改;税务属性/开票映射为规则性。

### A. 进系统 · 真业务规则(= 配置表种子值,后台可改)
按"何时触发 / 怎么结算"组织,分五小类(2026-07-04 重组,避免成本参数被误当分账规则):

**A1 · 结算与分账规则(核销时逐笔触发)**
| 参数 | 默认值 | 配置表 | 触发/口径 |
|---|---|---|---|
| 平台留存比例(=综合服务费) | 20% | platform_retention_rules | 售票时从票款先扣;所有门票都过 |
| SPV 分账方式 | 按核销权益单价精分(默认) / 按各酒吧每客协议价(`by_venue_per_guest`) | spv_distribution_configs、spv_per_guest_rate | 核销时逐笔;余额=票款−留存;`by_venue_per_guest`=按客数×协议单价分(赠酒补偿走 bar 域内部结算价,D5 后不预留赠酒成本) |
| 非散台服务费率(仅包厢/卡座) | 10% | — | **(实际低消+对应二次消费)× 费率**,合计后乘,散台不收 |
| 退款手续费率 | 按渠道/票种配 | refund_fee_rules | 退款时平台预扣 |
| 套票折扣归属 | 平台/商户/按比例 | ticket_discount_rules | 核销结算时 |

**A2 · 成本补偿与佣金(核销/销售时逐笔发生)**
| 参数 | 默认值 | 配置表 | 口径 |
|---|---|---|---|
| 赠酒补偿(D5 后=内部结算价) | 按商品内部结算价(真实进货成本) | ~~giveaway_cost_configs(D5 后废)~~ | **核销时 bar 域按内部结算价给出品酒吧挂应付**(`pass_drink_settlement`);未核销不补偿。原「每杯 3 元」是财务测算假设(附录 B),D5(2026-07-21)已删、不再进系统 |
| 中央餐饮场地返佣 | 可配比例或固定额 | catering_venue_rebate_rules | 开关开时,按**实际酒水销售额**返给提供场地的酒吧 SPV(真实场地费,非财务分利) |
| 订台渠道佣金率(通票无) | 15% | — | **订台权益核销时**平台预扣;非平均分 |

**A3 · 运营定价(运营后台设定,非公式反推)**
| 项 | 默认/来源 | 归属 |
|---|---|---|
| 通票价 平日/周末 | 99 / 149 元(可改) | 娱乐平台 |
| 游乐票价 | 30 元(可改) | 游乐公司 |
| 订台低消(散台/卡座/包厢) | 运营后台设定 | 中央餐饮 |
| 实物权益内部结算价 | 按商品录入 | — |
| 虚拟权益签约单价 | 按与商户签约 | — |

**A4 · 基础事实(可配,相对稳定)**
| 项 | 默认值 | 备注 |
|---|---|---|
| 可访问点位数 `visitable_points_count` | 10(9 酒吧+1 LiveShow) | **可配参数**,随门店增减变,非固定事实 |
| 桌位容量 散台/卡座/包厢 | 4 / 8 / 20 人 | 排位/服务费分母用 |
| 营业时间表 | 各店配置 | 自动划场次 |

**A5 · 会员积分规则(→ 积分规则表;与独立会员系统边界见附录 B.4)**
| 项 | 规则 |
|---|---|
| 积分兑换 / 价值 | 消费 1 元=1 积分;1000 积分=10 元 |
| 可兑 / 不可兑 | 门票折扣券+餐饮 / 酒水·订台·直兑通票(护高毛利) |
| 计提基数 | 通票/订台/服务费/额外酒水/游乐/演出(租赁不计) |
> 积分"成本 1%"是财务测算的负债估算(B 栏),**不进系统**;系统按真实积分发放/兑换记账,结算侧只标注"该笔消费可计积分"。

### B. 不进系统 · 仅财务测算假设(留在底稿,系统按实际交易记账)
> 这些是财务预测里"估算营收/成本数字怎么来的"**假设**,不是业务规则。系统按真实核销/交易/费用逐笔记账,**不用这些比例**(2026-07-04 用户纠偏)。
| 财务假设 | 底稿值 | 为何不进系统 |
|---|---|---|
| 平均访问点位数 | 10 | 系统按每票**真实核销**的点位算 |
| 通票赠酒最大成本 | 30 元/人 | =10×3 推导值,按实际访问算 |
| 额外酒水购买比例/平均杯数/单价 | 50% / 1.5杯 / 20元 | 按**实际销售**逐笔记 |
| 订台二次消费发生比例/金额比例 | 30% / 50% | 按**实际消费**记 |
| 订台溢价系数 / 取整 | 30% / 百元 | 订台价**运营直接设** |
| 酒水成本率 / 损耗率 | 25% / 1% | 成本按**真实费用归集** |
| 游乐维护费率 / 艺人成本率 | 5% / 待定 | 成本按**真实费用归集** |
| 客流量 / 营业天数 / 满台率 / 翻台率 | 多值 | 纯预测,系统无此概念 |

### C. 税务配置(→ 税务配置表 `tax_config`,**全部后台可配** —— 2026-07-04 用户裁定)
> 所有税费(**增值税、文化事业建设费、附加税**)按**每主体 × 收入类型**后台可填"有没有这笔费用 + 费率";下表为**默认种子值(可后台改)**。税法依据:文建费只征广告+娱乐服务(费率 3%);演出=文化服务原则不交、游乐看是否游艺,最终认定权留财务。

| 收入 | 主体 | 增值税(默认) | 文化事业建设费(默认) |
|---|---|---|---|
| 通票 | 娱乐平台 | 6% | 3% |
| SPV 运营服务(分账) | 酒吧SPV | 6% | — |
| 非散台服务费 | 酒吧SPV | 6% | 3% |
| 订台/额外酒水 | 中央餐饮 | 6% | — |
| 商业租赁 | 资产公司 | 9% | — |
| 游乐票 | 游乐公司 | 6% | 游艺项目计 3% / 大型机械游乐可关 |
| 演出票 | 传媒公司 | 6% | 默认不计(文化服务;酒吧内按娱乐开票可开) |

> **文建费不重复计**:通票的文化事业建设费在**平台侧按通票整体计一次(3%)**;SPV 的 80% 分账是"平台向 SPV 采购运营服务",SPV 开**运营服务费**发票(6%,不重复计文建费);非散台服务费为**娱乐服务**另计 3%。

### D. 开票主体映射(→ 结算单开票项,底稿 23_开票清单模板)
通票→娱乐平台(综合体验服务费)/SPV分账→酒吧SPV(运营服务费)/非散台服务费→酒吧SPV(娱乐服务费)/订台+额外酒水→中央餐饮(餐饮服务费)/商业租赁→资产公司(不动产租赁9%)/游乐→游乐公司/演出→传媒公司;通票赠酒=内部成本转付单(不对外开票);积分/折扣券=营销成本(不开票)。

### E. 已取消项(勿再建)
酒吧场地装修使用费/设备使用费(设备以使用权入股SPV不收费)、目标税前利润率反推、设备维修基金(改周期更新CapEx)。

---

## 附录 B. 2026-07-04 评审决策增补(扩范围:开票模块 + 持股分红层)

用户评审拍板把系统边界向后延伸两块,相应增补:

### B.1 开票清单模块(决策 4:做到开票清单级)
- 结算流水(§5)确认后,进一步生成**开票清单条目**,粒度对齐底稿 `23_开票清单模板`:
  - 字段:开票主体(6 主体之一)、开票项目(综合体验服务费/运营服务费/娱乐服务费/餐饮服务费/不动产租赁服务费/游乐服务费/文化服务费)、税率(6%/9%)、文化事业建设费标记、凭证要件(购票截图/服务协议/对账单/消费小票/合同等)、金额、税额。
  - **内部转付不开票**:通票赠酒(中央餐饮→SPV 成本转付单)、积分/折扣券(营销成本)标记为内部单据,不进对外开票清单。
- 新增表 `settlement_invoice_items`(开票清单条目)+ `invoice_subject_rules`(开票主体×项目×税率映射,配置化,种子=附录 A 第七节)。
- 系统**只生成开票清单交财务**,不对接税务局开票通道(那是财务软件/税控盘的事)。
- 后台页 ADM-STL-008(开票清单 + 主体税率配置)。

### B.2 持股分红层(决策 3:分红进系统)
- 在 SPV 分账/结算之上,增**利润分红分配**:SPV 运营利润(分账池收入+非散台服务费−成本)→ 按持股结构分红(默认平台 80% / 主理人 20%,**可配**)。
- 新增表:
  - `company_equity_holdings`(主体持股:holder 类型[平台/主理人/其他]、持股比例、分红权比例,可配)
  - `profit_distribution_records`(分红计算记录:期间、主体、可分配利润、各持有人分红额)
- 分红为**周期性作业**(月/季结),非逐笔;依赖成本归集(部分成本参数见附录 A)。
- 配置页 ADM-STL-009(持股与分红比例);权限 `settlement.distribution.manage`。
- ⚠️ **成本 = 真实费用归集(2026-07-04 定),不用"收入×成本率"预测比例估算**。分红准确度依赖成本归集完整性——本期先做"营收侧分账+可分配收入",完整成本核算(人工/折旧/CapEx)若超出系统范围则标注交财务,分红按"营收−系统可归集真实成本"口径,并注明口径边界。

### B.3 对批次与数据模型的影响
- §10.1 新建表追加:`settlement_invoice_items`、`invoice_subject_rules`、`company_equity_holdings`、`profit_distribution_records`。
- §2.3 管理公司范围修正:**酒吧 SPV 按真实 N 家建、数量可配**(非"先 1 个统一模型")。
- §14 批次追加:
  - **批 4b 开票清单模块**(接批 4,ADM-STL-008)
  - **批 9 持股分红层**(接批 2/批 5,ADM-STL-009,依赖成本归集)
- 工作量提示:开票级 + 分红层使总范围较基础版增约 2 个批次;分红的成本口径边界需在批 9 启动前与财务对齐。

### B.4 系统集成与边界说明(2026-07-04 逻辑一致性审查补)
- **会员积分边界**:结算侧只标注"该笔消费可计积分"(依消费确认);积分发放/累计/兑换/负债由**独立会员系统**处理,本系统不算"积分成本 1%"(财务测算)。积分兑换的折扣券若抵票款,在售票结算时作一项抵扣入账。
- **赠酒补偿实装(D5,2026-07-21)**:核销时由 bar 域按该商品内部结算价给出品酒吧挂应付(`pass_drink_settlement`·幂等+可冲正)=唯一补偿口径;**不从分账池预留赠酒成本**(原「每杯 3 元」池扣是财务测算假设,已删,见 §5.3 D5);**不采用"SPV 先垫再核销"**的账务。
- **中央餐饮开关=关 的分账**:酒水收入直接进**该酒吧商户账**(不进分账池、无场地返佣);**通票分账池仍按 §5 留存%+余额规则**,不受开关影响。
- **中央餐饮返佣触发**:开关开时,按实际酒水销售逐笔/周期(按配置)计算返给对应 SPV;是结算规则(A2),非财务平均分利。
