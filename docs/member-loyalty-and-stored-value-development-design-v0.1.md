# 会员忠诚度 · 储值开发设计 V0.05（付费会员已废止）

> 生成日期：2026-06-24。性质：**据当前实现回填（source-driven）**——`LoyaltyService` 与 `StoredValueService` 已有部分后端能力，但与现行 HTML 仍有明确差距；`MembershipService` 已不存在，付费会员表已由 V290 删除。本文只记录当前代码事实和待改造差距，业务口径以三份 HTML 和现行会员、积分、储值 PRD 为准。

> **[A2 / 业务口径覆盖 2026-07-28]** 储值**业务规则**以 MAIN `member-loyalty-and-stored-value-product-prd-v0.1.md` **§7** 与裁决 `DEC-SV-*` 为准，**覆盖**本文 2026-06-24 代码回填中的单一余额叙述。
> 本文 §6 仍描述**当前实现结构**，实现迁移前不得用旧单科目模型对抗 MAIN。积分主规则见 `points-and-voucher-product-prd-v1.0.md`，会员与等级主规则见 `member-and-tier-product-prd-v1.0.md`。

>
> 字段/方法以 `backend/src/main/java/com/complex/platform/customer/` 与迁移为准；本文不冻结 OpenAPI（端点见 `docs/openapi/openapi-p0.yaml` `customer/*`、`loyalty/*` 段）。

## 1. 积分两本账（核心模型）

ENUM「等级与成长值」条现文：「成长值与可用积分是两套独立账，积分兑换不扣成长值」；等级按后台配置的滚动统计周期内的成长值计算，达到门槛立即升级、升级后享有保护期（同条）。**实现实证**（2026-08-07 引文订正：原引「成长积分（算等级，只增不减、仅到期清减、不受兑换影响）」为旧口径，现行 HTML 已改称「成长值」并改为滚动统计周期 + 保护期；代码侧尚无独立成长值账，沿用保级进度列，差距见下文待办表）：

| 账 | 落点（`customer_points_accounts`） | 行为 |
|---|---|---|
| **可用积分** | `balance` (BIGINT) | 兑换/扣减消耗；获取时增加。`deductPoints` 扣此账。 |
| **保级进度（代码列，不等同业务成长值）** | `keep_threshold_progress` (BIGINT，V089) | 积分确认值等仍累加到该列，但自第十四批 OBS-13-01（V543）起等级不再读它。该列不是 HTML 要求的独立成长值账。 |
| 冻结额 | `frozen_points` (V089) | 账户冻结时挂起。 |
| 账户状态 | `account_status`（active/frozen） | `freeze`/`unfreeze`。 |
| 本轮保护期 | `level_period_start` / `protection_until`（V543 由 `grace_until` 改名） | 最近一次等级变化时点（升级时即本轮保护期起点；期末保级不更新，作三项等级参数取版本的时点）与本轮保护期截止。 |

**等级计算关键（2026-10-25 第十四批 OBS-13-01，V543）**：`LoyaltyService.evaluateAccount` 按 `customer_growth_ledger` 在滚动统计周期内的成长值净额取等级，不看 `balance` 与 `keep_threshold_progress`，兑换、签到、消费积分都不影响等级。

## 2. 会员等级（升级 / 保护期 / 降级预警 / 降级）

> **实现现状（2026-10-25 第十四批 OBS-13-01，V543）**：本节以下 2026-08 前的「保级门槛＋考核周期＋宽限期」描述已不再是代码事实。现行实现见 `member-and-tier-product-prd-v1.0.md` §8 实现现状：三项等级参数（滚动统计周期、保护期、提前通知天数）落 `ticket_reward_config_versions` 版本化审批；成长值入账即时升级、升级起保护期；每日作业发降级预警、「已保级」并在保护期结束当日定级、补发降级通知；成长值冲回即时重定；通知落 `customer_level_notices` 由客人端弹出。`customer_member_levels` 的 `keep_threshold`／`keep_period_days`／`grace_days` 三列保留默认值、不再读写；对应单测已删，判级由 `LoyaltyLevelProtectionJdbcIntegrationTests` 守。

落 `customer_member_levels`（V089）：`level_code`/`level_rank`/`upgrade_threshold`（升级门槛）/`keep_threshold`（保级门槛）/`keep_period_days`（保级周期）/`grace_days`（宽限）/`point_multiplier_basis_points`（积分倍率，10000=1x）。

> **实现现状（2026-10-27 第十八批第 12 条，V558）**：`customer_member_levels` 去掉 `project_id`、`level_code` 全局唯一，等级档位全平台一套（BSM L820/L821，DEC-R3-2026-08-04-35）；后台 `POST /api/v1/loyalty/config/member-levels` 仅全国范围账号（平台管理员或勾了「可以查看全部项目」的角色）可写，录入即用于全国定级与倍率。

`LoyaltyService.evaluateLevels(now)`（`LoyaltyScheduledJob` 定时调）逐账户评估：
1. **升级**：保级进度 `keep_threshold_progress` 达更高等级 `upgrade_threshold` 即时升级（不等周期末）。
2. **保级/降级**：周期（`level_period_start`+`keep_period_days`）到期重算；未达 `keep_threshold` 进入宽限（`grace_until`），宽限内可补足；超期降级并留痕。
   **降级落点（2026-08-03 修正）**：超宽限时用 `highestQualified(levels, keep_threshold_progress)` 按当前进度**重新定级**，
   进度跌穿几级就一次落到对应等级；原实现取"比当前低一级的最高者"（已删的 `nextLower`），**恰好只降一级**，
   与速查表§二"到期直接降到对应等级"、系统地图"等级与成长值"以及 `member-and-tier-product-prd-v1.0.md` §8.3
   "会员可直接降至实际符合的等级，不要求逐级下降"相反。单测 `LoyaltyServiceTests.periodEndDowngradesDirectlyToQualifiedLevelAcrossTiers` 守这条。
3. 单账户手动评估 `evaluateLevel(customerId)` 过权限闸 `customer.loyalty.evaluate`。

### 2.1 待办：成长值口径尚未全部落地（HTML-05 未闭合；表中标「已闭合」的三行除外，其余勿视为已完成）

上面的降级落点修正只是**止血**，等级体系与业务 HTML 的口径差距仍在，本批**未做**，如实登记为待办：

| 待办 | HTML 口径 | 代码现状 | 影响 |
|---|---|---|---|
| **成长值这本账在代码里根本不存在** | 速查表§二"成长值与可用积分是两套独立账，积分兑换不扣成长值" | 全仓 `成长值`/`growth_value`/`growthValue` 零命中；`LoyaltyService` 把**本次积分**直接累加进 `keep_threshold_progress`（`earnPoints`/`earnPointsPending`/确认路径三处），且该数值**已经吃过等级倍率和生日倍率** | 等级由"加了倍率的积分"决定，高等级会自我加速升级；PRD §9"等级倍率不得放大成长值"被违反 |
| **保级窗口不是滚动十二个月**（**已由第十四批 OBS-13-01（V543）闭合**：按滚动统计周期内的成长值定级） | 速查表§二/PRD §7"评估时点向前滚动后台配置的统计周期（全平台统一可配，出厂默认 12 个月）内的有效成长值" | `keep_period_days` 固定周期 + 到期把 `keep_threshold_progress` **清零重攒**（`applyLevelChange(resetPeriod=true)`） | 同样的消费节奏，落在周期边界两侧结果不同；不是滚动窗口 |
| **票务积分未按票种规则算** | 速查表§二"票务订单不读取商户业态规则，而按票种或票务产品已审批生效的积分规则、成长值规则分别计算" | `TicketOrderService` 走项目×受理商户的通用积分累计率 | 票种差异化积分/成长值无法配置 |
| **票务积分确认与成长值确认未拆开** | 票务积分在退款期结束且对应票款未退款时确认，未使用或到期未用仍有积分；票务成长值只按实际核销人次确认 | 支付成功只写 `pending`；`LoyaltyPointsConsumer` 仍在核销事件上按 `verifiedUnits/totalUnits` 确认积分并同步增加 `keep_threshold_progress`；到期作废任务不终结未核销订单的 `pending` 积分 | 已支付未退款但未核销的票无法按期获得可用积分，核销又把积分值混入成长进度 |
| **签到奖励错误进入保级进度**（**已由第十四批 OBS-13-01（V543）闭合**：等级只读成长值流水，签到不再推动等级） | 签到按连续天数阶梯奖励积分，不产生成长值 | `LoyaltyService.checkIn` 同时增加 `balance` 与 `keep_threshold_progress` | 签到会错误推动等级 |
| **生日月仍可配置** | 生日加倍只限生日当天 | `birthdayMultiplierBasisPoints` 和后台配置仍接受 `birthday_month` | 非生日当天也可能命中生日倍率 |
| **生日规则锁定缺失** | 生日倍率按支付成功时点锁定适用时点和规则版本 | 积分入账时读取当前生日规则，积分流水没有支付时点、适用时点和生日规则版本快照 | 后续配置变化可能改写订单应适用的倍率 |
| **降级提前三十天通知未实现**（**已由第十四批 OBS-13-01（V543）闭合**：降级预警、「已保级」与降级补发通知已有） | 速查表§二"降级前提前三十天通知"、PRD §8.3 | 只有 `grace_until` 宽限期，无通知触发点 | 会员被静默降级 |

修这几条要动成长值独立账本（新表/新列 + 明细带发生时间做滚动窗口）与票务规则读取，属独立批次，不在本次止血范围内。

## 3. 积分获取引擎（earn / confirm / deduct / 过期）

落 `customer_points_ledger`（V100/V025，幂等 `uq_customer_points_ledger_source_doc`）。
> ⚠️ 更正（2026-07-28，V352）：本节原写「+ `customer_points_rules`（V060 规则）」，与代码不符——引擎从未读过该表。
> 积分累计率读 `points_accrual_rate_configs`（V340），等级读 `customer_member_levels`，签到读 `customer_check_in_tiers`，
> 生日读 `customer_birthday_rules`。`customer_points_rules` 与后台页 ADM-CUS-003 已停用下线（表留库不物删）。
- `earnPoints` / `earnPointsPending`：获取/挂起（pending）；按消费金额或按核销张数等规则。
- `confirmPoints(sourceDoc, verifiedUnits, totalUnits)`：**按已核销张数比例确认**；退款按已确认份额冲减（reversed）。 **AUD-014（2026-07-20）：算 delta 前对 earn 行加 `FOR UPDATE` 行锁（锁序 earn→account），串行化同一订单的并发确认/冲正 → 后到事务读到已提交的已确认份额，delta=0 幂等，杜绝两事务基于同一旧值重复给账户加钱（余额>明细的账实分叉）。**
  - **✅ P0-B 积分台账改追加（2026-07-28，迁移 V344）**：确认份额**不再就地改写 earn 行的 `confirmed_amount`**（旧值被覆盖、变动无痕，属资金台账 append-only 破防）。
    新口径：每次确认/冲正**追加一条 `change_type='confirm'` 分录**，`points_delta` 带符号（正 = 本次新确认份额，负 = 本次冲正回滚份额），
    `source_id` 指向原 earn 行、`source_doc_type`/`source_doc_id` 指向业务单据；某单「已确认份额」= **该单全部 confirm 分录的 `points_delta` 之和**。
    `confirmPoints` 与 `deductPoints` 都改读这个派生值。earn 行只再同步**生命周期列**（`status`、以及 FIFO 批次的 `remaining_points`），
    与 `ar_ap_items.paid`/`unpaid` 可变口径同源。
  - DB 侧硬化：新增触发器 `trg_points_ledger_freeze_facts` **冻结事实列**（`customer_id`/`points_delta`/`balance_after`/`change_type`/
    `source_type`/`source_id`/`source_doc_type`/`source_doc_id`/`confirmed_amount`/`expire_at`/`created_at`），改这些列一律抛
    `COMPLEX_APPEND_ONLY_VIOLATION`；放行 `remaining_points`/`expired`/`status`/`description`。
    刻意用**冻结清单（deny-list）**而非允许清单——后续给该表加的新列默认可改，不会被写死的列名堵死迭代。
  - 唯一索引 `uq_customer_points_ledger_source_doc` 加 `change_type <> 'confirm'` 条件（同一单据要能反复追加 confirm 分录：部分核销 1/3 → 2/3 → 3/3 各落一行、冲正落负行）。
  - 存量迁移：每条 `confirmed_amount > 0` 的历史 earn 行**补建一条等额 confirm 分录**（`lcbf_` + md5(earn id)），令派生值与原就地值**逐单严格相等**——零行为改变，
    且避免存量单派生值突然归零导致再来一次核销事件把整份积分二次入账。
- `deductPoints`：兑换消耗扣 `balance`。
- `change_type`：获取/扣减/兑换消耗/过期/调整（速查表 §二明细类型），每笔关联触发单据。
- **过期清零** `expirePoints(now)` / `expirePointsForCustomer`：当前代码按积分批次 `expire_at` 以 FIFO 清减可用 `balance`，保持 `keep_threshold_progress` 不变；等级周期到期清零保级进度发生在等级周期处理，不在积分过期方法。该批次过期实现仍不符合 HTML 的账户滚动有效规则，属于待改造差距。
- 事件 `points.earned`/`points.deducted`（V101 `audit_events` 扩 category/subject）。

## 4. 连续签到

落 `customer_check_ins`（V089，`customer_id`+`check_in_date` 唯一）+ 账户 `last_check_in_date`/`check_in_streak`：
- `LoyaltyService.checkIn(customerId, sourceTerminal)`：每日签到记连续天数，断签重置。
- 阶梯奖励 `tierAwardForStreak(streak)`：按 `customer_check_in_tiers`（streak_days→award_points 升序，取命中最高阶梯）发积分。

## 5. 生日加倍

落 `customers.birth_date`（V089）+ `customer_birthday_rules`（当前代码仍允许窗口 `birthday_day`/`birthday_month` + `multiplier_basis_points`）：

> **实现现状（2026-10-27 第十八批第 12 条同根，V558）**：`customer_birthday_rules` 去掉 `project_id`、`rule_name` 全局唯一，生日加倍规则全平台一套（BSM L831「不按项目分别配置」）；后台仅全国范围账号可写。

- 获取积分时 `birthdayMultiplier` 命中当前窗口后取最高倍率并乘算。`birthday_month` 与 HTML 只限生日当天冲突；支付时点、适用时点和规则版本快照也未落地，均见 §2.1 待办。

## 6. 储值预付卡

落 `customer_stored_value_accounts` / `customer_stored_value_transactions`（V090）。`StoredValueService`：
- `topup`：返回 finance 支付意图（`PaymentIntentRecord`）；**`settleTopup` 支付成功后才入账**（不凭空加余额，速查表"充值必须支付成功才入账"）。
- `consume`：消费扣余额（幂等）。
- `refund`：退卡/退款。
- `freeze`/`unfreeze`：账户冻结/解冻（速查表"账户可冻结/解冻"）。
- 客人端端点 `customer/stored-value/{topup,settle,consume,account}`（MemberWalletController）。
- 账户仍只有单一 `balance_cent`，没有本金/赠送金双科目；充值流水没有每笔支付成功时的支付通路费率与规则版本快照。
- 商户级卡仍允许 `fundOwnership=platform_collect`，消费时形成平台应付商户；这与 HTML“平台不代收、不介入商户级卡资金归属”冲突，必须作为实现差距移除，不能提升为业务规则。

## 7. 付费会员订阅（已废止 2026-07-18）

> **[已废止 2026-07-18]** 付费会员（客人付费购买年卡/权益包）已按权威业务地图（三份业务 HTML 对「付费会员」零命中）彻底废除：客人端/后台端点、`customer_memberships` / `customer_membership_plans` 两表、`MembershipService`、OpenAPI 六条 operation 全部下线（迁移 V290 DROP 两表并退役 `customer.membership.*` 三条权限）。本节仅存历史记录，非现行能力；免费会员忠诚度（成长值／当前保级进度列与等级自动升级）与储值预付卡不受影响。

当前仓库没有 `MembershipService`、会员订阅端点或付费会员 OpenAPI；不得按 V090 的历史结构恢复已废止能力。

## 8. 账户冻结 / 解冻

积分账户 `LoyaltyService.freeze/unfreeze`、储值账户 `StoredValueService.freeze/unfreeze`，均留痕（风控/异常时挂起）。

## 9. 数据表清单（实证）

| 表 | 迁移 | 用途 |
|---|---|---|
| `customer_points_accounts`（+V089 扩列） | V009/V089 | `balance` 可用积分、`keep_threshold_progress` 当前保级进度列（不等同成长值）+ 冻结 + 保级周期 + 签到 |
| `customer_member_levels` | V089 | 等级配置（升级/保级门槛/周期/宽限/倍率） |
| `customer_points_ledger` | V025/V100/V151/**V344** | 积分流水（获取/确认/扣减/过期，幂等）。**V344 起改追加**：已确认份额 = `change_type='confirm'` 分录的 `points_delta` 之和；事实列被触发器冻结，`confirmed_amount` 仅作历史列不再回写 |
| `customer_points_rules` | V060 / **V352** | ~~积分获取规则~~ **已停用（2026-07-28）**：零读取方，后台页 ADM-CUS-003 同批下线；表留库不物删（全表 `archived_at` 存档） |
| `customer_check_ins`（+ tiers） | V089 | 连续签到 + 阶梯 |
| `customer_birthday_rules` / `customers.birth_date` | V089 | 生日加倍 |
| `customer_stored_value_accounts` / `_transactions` | V090 | 储值预付卡 |
| `customer_membership_plans` / `customer_memberships` | V090 / V290 | 历史付费会员表；V290 已删除，非现行表 |
| `customer_points_redemptions` | V025/V060 | 积分兑换发放 |

> 注：`customer_points_transfers` / `customer_points_transfer_limits` 已由 V172 删除；积分余额不可转赠，不存在待清理的现行孤儿表。

## 10. 待补（缺口）

- **UI 验收**：`full-admin-ui-acceptance-matrix` 与客人端 UI 验收矩阵须核对会员、储值和签到能力；付费会员已废止，不得列为待补页面。
- **客人端页面**：须核对签到页、储值充值/余额页与现行 API 的映射；不得补回付费会员开通或权益页。
- 积分获取规则（按金额/张数/签到/生日/活动）与等级升降的**产品规则文档**此前薄弱，本文已据实现回填核心口径；细则以各真身配置表为准——累计率 `points_accrual_rate_configs`（ADM-STL-001 结算配置中心）、等级 `customer_member_levels`、签到 `customer_check_in_tiers`、生日 `customer_birthday_rules`（均在 ADM-CUS-004）。原文此处写的 `customer_points_rules` 已于 2026-07-28 停用（V352）。


## 重构切片（2026-07-28）

- 实现切片与差距矩阵：`docs/superpowers/plans/2026-07-28-member-points-tier-clearing-implementation-slices.md`
- 产品主 PRD：`docs/points-and-voucher-product-prd-v1.0.md`、`docs/member-and-tier-product-prd-v1.0.md`
- 注意：等级倍率乘算、batch 过期、账户级冻结等现网语义需按切片迁移，不得与新 PRD 长期双轨并行。


## 6A. 储值本金/赠送金（MAIN 覆盖，2026-07-28）

> 实现切片未完成前，**产品与验收以本节约定 + 会员忠诚度 PRD §7 为准**。

| 裁决 | 要求 |
|---|---|
| DEC-SV-LEDGER-001 | 充值拆 **本金 + 赠送金**；赠送额度可配 |
| DEC-SV-SPEND-ORDER-001 | 扣款顺序**写死为先扣本金、后扣赠送金，不可配置**（**仅商户级卡例外**：其扣款顺序由发卡商户自定、由平台运营代为录入后台）。依据 ENUM「储值卡类型与收款主体」条；DEC-R3-2026-08-04-21 / -24 |
| DEC-SV-REFUND-001 | **退卡只退未消费的本金**；**赠送金不退现**，未使用部分按活动规则作废或收回（非默认值，无「可退赠送金」的配置项）。依据 ENUM「储值卡类型与收款主体」条 |
| DEC-SV-CLOSE-001 | 销户：先解除全部占用与冻结，再退剩余本金；**赠送金清零**（非默认值，无可配置项）。依据 ENUM「储值卡类型与收款主体」条 |
| DEC-SV-NO-MIGRATE-001 | **禁止**余额迁移 |

**已落地（2026-10-17 CG-06a，V444 `stored_value_principal_bonus_split`）**：
- `customer_stored_value_accounts.principal_cent / bonus_cent`、`customer_stored_value_transactions.principal_cent / bonus_cent` 两段记账，`balance_cent`＝两段之和（服务层维护，不加等式约束，存量行两段为 0 不回填）；
- 充值赠送比例（第十八批第 11 条 V558 起取代原 `stored_value_bonus_policies` 存即生效表与 `bonus-policies` 端点）：通用卡 `stored_value_bonus`（按项目，项目可空＝无项目通用卡）与商户级卡 `merchant_card_bonus`（按发卡商户）并入 `ticket_reward_config_versions`，经 `POST /api/v1/loyalty/ticket-reward-configs` 提交审批（值与上下限同配，`ticket.reward.config.manage` 提交、`ticket.reward.rule.approve` 复核），审批通过到生效时间生效；充值流水 `bonus_config_version_id` 记下所用版本；
- `StoredValueService.settleTopupInternal` 拆两段入账；`consumeBalance` 通用卡先本金后赠送金、商户级卡按 `stored_value_merchant_refund_rules.consume_order`（`configureMerchantCard` 代录）；`doRefund` 只退本金（409 `STORED_VALUE_PRINCIPAL_INSUFFICIENT`），销户时赠送金作废（`adjust` 流水）；
- 客人端钱包账户与逐笔记录分列本金/赠送金；后台储值规则页配置扣款顺序，赠送比例在「门票积分、成长值与销售责任」页提交审批。

实现缺口（相对 MAIN，不在本文假装已落地）：
- 活动来源引用（赠送比例的版本化与审批已于第十八批第 11 条 V558 落地）；
- 消费退款回冲到卡的路径（先回冲赠送金、后回冲本金）：当前无任何路径把消费退回卡余额，仅流水留存两段扣减额；
- 每笔充值支付成功时须固化收款主体、发行主体、实际支付通路费率和退款规则版本；合同换期不得改写已充值余额的费率；
- 下游 `platform_collect` 分支（只对已定格旧卡）未删；平台代收入口已于第十八批第 11 条关闭，资金归属只能由发卡商户承担；
- 补齐客人端本金/赠送金和卡类型分列、充值/扣款/退款明细、不可关闭的储值金额短信、运营端三维储值台账与公共大屏隐私边界；
- 禁止账户间迁移 API/后台入口。

