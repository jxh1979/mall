# 会员忠诚度 · 储值 · 付费会员 开发设计（V0.1）

> 生成日期：2026-06-24。性质：**据现有实现回填（source-driven）**——会员忠诚度/储值/付费会员后端已在 `customer` 域完整落地（`LoyaltyService`/`MembershipService`/`StoredValueService` + 迁移 V025/V060/V089/V090/V100/V151），但此前无 PRD/开发设计承载，本文按代码实证补齐设计口径。业务口径以业务系统地图 #ticketing 会员规则、业务枚举速查表 §二（会员与积分）为最终解释权；本文与之对齐。

> **[A2 / 业务口径覆盖 2026-07-28]** 储值**业务规则**以 MAIN `member-loyalty-and-stored-value-product-prd-v0.1.md` **§7** 与裁决 `DEC-SV-*` 为准，**覆盖**本文 2026-06-24 代码回填中的单一余额叙述。
> 本文 §6 仍描述**当时实现结构**，实现迁移前不得用旧单科目模型对抗 MAIN。积分主规则见 `member-points-tier-and-clearing-prd-v1.0.md`。

>
> 字段/方法以 `backend/src/main/java/com/complex/platform/customer/` 与迁移为准；本文不冻结 OpenAPI（端点见 `docs/openapi/openapi-p0.yaml` `customer/*`、`loyalty/*` 段）。

## 1. 积分两本账（核心模型）

业务速查表 §二："成长积分（算等级，只增不减、仅到期清减、不受兑换影响）；可用积分（兑换消耗会扣减、不影响等级）"。**实现实证**：

| 账 | 落点（`customer_points_accounts`） | 行为 |
|---|---|---|
| **可用积分** | `balance` (BIGINT) | 兑换/扣减消耗；获取时增加。`deductPoints` 扣此账。 |
| **成长积分（保级进度）** | `keep_threshold_progress` (BIGINT，V089) | 获取时累加、只增不减、仅周期到期清减；**等级由它算**，兑换扣 `balance` 不影响。 |
| 冻结额 | `frozen_points` (V089) | 账户冻结时挂起。 |
| 账户状态 | `account_status`（active/frozen） | `freeze`/`unfreeze`。 |
| 保级周期 | `level_period_start` / `grace_until`（V089） | 等级评估窗口与宽限。 |

**等级计算关键**：`LoyaltyService.evaluateAccount` 用 `highestQualified(levels, account.keepThresholdProgress())` 取等级——即等级看**成长积分（keep_threshold_progress）**，不看 `balance`。**故客人兑换礼物扣 `balance` 不会降低会员等级**，与速查表"可用积分扣减不影响等级"一致。

## 2. 会员等级（升级 / 保级 / 降级 / 宽限）

落 `customer_member_levels`（V089）：`level_code`/`level_rank`/`upgrade_threshold`（升级门槛）/`keep_threshold`（保级门槛）/`keep_period_days`（保级周期）/`grace_days`（宽限）/`point_multiplier_basis_points`（积分倍率，10000=1x）。

`LoyaltyService.evaluateLevels(now)`（`LoyaltyScheduledJob` 定时调）逐账户评估：
1. **升级**：保级进度 `keep_threshold_progress` 达更高等级 `upgrade_threshold` 即时升级（不等周期末）。
2. **保级/降级**：周期（`level_period_start`+`keep_period_days`）到期重算；未达 `keep_threshold` 进入宽限（`grace_until`），宽限内可补足；超期降级并留痕。
   **降级落点（2026-08-03 修正）**：超宽限时用 `highestQualified(levels, keep_threshold_progress)` 按当前进度**重新定级**，
   进度跌穿几级就一次落到对应等级；原实现取"比当前低一级的最高者"（已删的 `nextLower`），**恰好只降一级**，
   与速查表§二"到期直接降到对应等级"、系统地图"等级与成长值"以及 `member-and-tier-product-prd-v1.0.md` §8.3
   "会员可直接降至实际符合的等级，不要求逐级下降"相反。单测 `LoyaltyServiceTests.periodEndDowngradesDirectlyToQualifiedLevelAcrossTiers` 守这条。
3. 单账户手动评估 `evaluateLevel(customerId)` 过权限闸 `customer.loyalty.evaluate`。

### 2.1 待办：成长值口径尚未落地（HTML-05 未闭合，勿视为已完成）

上面的降级落点修正只是**止血**，等级体系与业务 HTML 的口径差距仍在，本批**未做**，如实登记为待办：

| 待办 | HTML 口径 | 代码现状 | 影响 |
|---|---|---|---|
| **成长值这本账在代码里根本不存在** | 速查表§二"成长值与可用积分是两套独立账，积分兑换不扣成长值" | 全仓 `成长值`/`growth_value`/`growthValue` 零命中；`LoyaltyService` 把**本次积分**直接累加进 `keep_threshold_progress`（`earnPoints`/`earnPointsPending`/确认路径三处），且该数值**已经吃过等级倍率和生日倍率** | 等级由"加了倍率的积分"决定，高等级会自我加速升级；PRD §9"等级倍率不得放大成长值"被违反 |
| **保级窗口不是滚动十二个月** | 速查表§二/PRD §7"评估时点向前滚动最近 12 个月的有效成长值" | `keep_period_days` 固定周期 + 到期把 `keep_threshold_progress` **清零重攒**（`applyLevelChange(resetPeriod=true)`） | 同样的消费节奏，落在周期边界两侧结果不同；不是滚动窗口 |
| **票务积分未按票种规则算** | 速查表§二"票务订单不读取商户业态规则，而按票种或票务产品已审批生效的积分规则、成长值规则分别计算" | `TicketOrderService` 走项目×受理商户的通用积分累计率 | 票种差异化积分/成长值无法配置 |
| **降级提前三十天通知未实现** | 速查表§二"降级前提前三十天通知"、PRD §8.3 | 只有 `grace_until` 宽限期，无通知触发点 | 会员被静默降级 |

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
- **过期清零** `expirePoints(now)` / `expirePointsForCustomer`：`customer_points_ledger` 的 `expire_at`/`remaining_points`/`expired`，按自然年或后台配置天数清减**成长积分**。
- 事件 `points.earned`/`points.deducted`（V101 `audit_events` 扩 category/subject）。

## 4. 连续签到

落 `customer_check_ins`（V089，`customer_id`+`check_in_date` 唯一）+ 账户 `last_check_in_date`/`check_in_streak`：
- `LoyaltyService.checkIn(customerId, sourceTerminal)`：每日签到记连续天数，断签重置。
- 阶梯奖励 `tierAwardForStreak(streak)`：按 `customer_check_in_tiers`（streak_days→award_points 升序，取命中最高阶梯）发积分。

## 5. 生日加倍

落 `customers.birth_date`（V089）+ `customer_birthday_rules`（窗口 `birthday_day`/`birthday_month` + `multiplier_basis_points`）：
- 获取积分时 `birthdayMultiplier`：命中生日窗口取最高倍率，`applyMultiplier(points, basisPoints)` 加倍。

## 6. 储值预付卡

落 `customer_stored_value_accounts` / `customer_stored_value_transactions`（V090）。`StoredValueService`：
- `topup`：返回 finance 支付意图（`PaymentIntentRecord`）；**`settleTopup` 支付成功后才入账**（不凭空加余额，速查表"充值必须支付成功才入账"）。
- `consume`：消费扣余额（幂等）。
- `refund`：退卡/退款。
- `freeze`/`unfreeze`：账户冻结/解冻（速查表"账户可冻结/解冻"）。
- 客人端端点 `customer/stored-value/{topup,settle,consume,account}`（MemberWalletController）。

## 7. 付费会员订阅（已废止 2026-07-18）

> **[已废止 2026-07-18]** 付费会员（客人付费购买年卡/权益包）已按权威业务地图（三份业务 HTML 对「付费会员」零命中）彻底废除：客人端/后台端点、`customer_memberships` / `customer_membership_plans` 两表、`MembershipService`、OpenAPI 六条 operation 全部下线（迁移 V290 DROP 两表并退役 `customer.membership.*` 三条权限）。本节仅存历史记录，非现行能力；免费会员忠诚度（成长积分/等级自动升级）与储值预付卡不受影响。

落 `customer_membership_plans` / `customer_memberships`（V090）。`MembershipService`：
- `subscribe`：返回支付意图，绑支付（速查表"开通绑定支付"）。
- `activate`（支付成功后生效）/ `renew` / `autoRenewCharge`（自动续费，V290 已废止）/ `cancel` / `expireMemberships`（到期）。
- `activeBenefits(customerId)`：当前生效会员权益。
- `savePlan`：后台档位维护（权限 `customer.membership.manage`）。
- 客人端 `customer/memberships{,/subscribe}`、`customer/membership-plans`、`customer/membership-benefits`。

## 8. 账户冻结 / 解冻

积分账户 `LoyaltyService.freeze/unfreeze`、储值账户 `StoredValueService.freeze/unfreeze`，均留痕（风控/异常时挂起）。

## 9. 数据表清单（实证）

| 表 | 迁移 | 用途 |
|---|---|---|
| `customer_points_accounts`（+V089 扩列） | V009/V089 | 两本账（balance 可用 / keep_threshold_progress 成长）+ 冻结 + 保级周期 + 签到 |
| `customer_member_levels` | V089 | 等级配置（升级/保级门槛/周期/宽限/倍率） |
| `customer_points_ledger` | V025/V100/V151/**V344** | 积分流水（获取/确认/扣减/过期，幂等）。**V344 起改追加**：已确认份额 = `change_type='confirm'` 分录的 `points_delta` 之和；事实列被触发器冻结，`confirmed_amount` 仅作历史列不再回写 |
| `customer_points_rules` | V060 / **V352** | ~~积分获取规则~~ **已停用（2026-07-28）**：零读取方，后台页 ADM-CUS-003 同批下线；表留库不物删（全表 `archived_at` 存档） |
| `customer_check_ins`（+ tiers） | V089 | 连续签到 + 阶梯 |
| `customer_birthday_rules` / `customers.birth_date` | V089 | 生日加倍 |
| `customer_stored_value_accounts` / `_transactions` | V090 | 储值预付卡 |
| `customer_membership_plans` / `customer_memberships` | V090 | 付费会员订阅 |
| `customer_points_redemptions` | V025/V060 | 积分兑换发放 |

> 注：`customer_points_transfers`/`customer_points_transfer_limits`（V090）为**已下线**的积分转赠遗留孤儿表（2026-06-24，业务"积分不支持会员间转赠"），待清理迁移统一删，不属本会员体系现行能力。

## 10. 待补（缺口）

- **UI 验收**：`full-admin-ui-acceptance-matrix` 与客人端 UI 验收矩阵均缺会员/储值/签到/订阅行（后端有、验收未单列）。
- **客人端页面**：`customer-app-miniapp-development-design` 缺签到页、储值充值/余额页、付费会员开通/权益页的页面→API 映射（能力已在 OpenAPI `customer/*`）。
- 积分获取规则（按金额/张数/签到/生日/活动）与等级升降的**产品规则文档**此前薄弱，本文已据实现回填核心口径；细则以各真身配置表为准——累计率 `points_accrual_rate_configs`（ADM-STL-001 结算配置中心）、等级 `customer_member_levels`、签到 `customer_check_in_tiers`、生日 `customer_birthday_rules`（均在 ADM-CUS-004）。原文此处写的 `customer_points_rules` 已于 2026-07-28 停用（V352）。


## 重构切片（2026-07-28）

- 实现切片与差距矩阵：`docs/superpowers/plans/2026-07-28-member-points-tier-clearing-implementation-slices.md`
- 产品主 PRD：`docs/member-points-tier-and-clearing-prd-v1.0.md`
- 注意：等级倍率乘算、batch 过期、账户级冻结等现网语义需按切片迁移，不得与新 PRD 长期双轨并行。


## 6A. 储值本金/赠送金（MAIN 覆盖，2026-07-28）

> 实现切片未完成前，**产品与验收以本节约定 + 会员忠诚度 PRD §7 为准**。

| 裁决 | 要求 |
|---|---|
| DEC-SV-LEDGER-001 | 充值拆 **本金 + 赠送金**；赠送额度可配 |
| DEC-SV-SPEND-ORDER-001 | 扣款顺序可配；**默认先本金后赠送金** |
| DEC-SV-REFUND-001 | 默认**仅本金可退现金**；赠送金默认不退现 |
| DEC-SV-CLOSE-001 | 注销：退剩余本金；赠送金默认清零 |
| DEC-SV-NO-MIGRATE-001 | **禁止**余额迁移 |

实现缺口（相对 MAIN，不在本文假装已落地）：
- 账户/流水需能拆本金与赠送金科目及活动来源；
- `topup`/`consume`/`refund` 需按双科目与默认扣款/退现顺序改造；
- 禁止账户间迁移 API/后台入口。

