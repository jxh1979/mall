# 日结多源汇总 + 看板七类指标 聚合口径（已定稿，2026-06-19 财务确认）

> **状态：已定稿（2026-06-19 财务确认）。** 原 V0.1 草案中 §四 4 项口径已由财务确认并落代码（见 §四与 §六变更日志）。本文是 R5 backlog 任务 **T7（日结多源汇总）** 与 **T12（看板真实聚合）** 的口径附件，解除"聚合口径由谁定义"的阻塞。
>
> **口径来源**：业务说明地图 #finance（六源汇总/四类差异/结算依据）与 #analytics（七类指标）；数据源表名取自真实 Flyway 迁移（V001–V076，已 `grep CREATE TABLE` 核对）。
>
> **基础约定**：金额=整数分（D1）；所有看板/汇总查询统一经 `currentMerchantScope` 行级隔离（商户只见自身范围，D11/数据隔离规格）；日切以经营日为准——**经营日切时点可后台配置，默认 04:00**（配置项 `finance.business-day-cutoff-hour`，凌晨该点前的流水归前一经营日；助手 `BusinessDayResolver`，T7 与 T12 ETL 统一分桶）；状态以各表 CHECK 枚举为准。
>
> **⚠️ 实现前必须先收口的两处表分裂**（影响 T7）：迁移同时存在 `ar_ap_items`/`ar_ap_payments` 与 `finance_ar_ap_items`/`finance_ar_ap_payments` 两套，以及 `finance_reconciliation_*` 与 `integration_reconciliation_*` 两套——T7 落地前须由 owner 裁定权威表，本草案默认以 `finance_*` 为财务权威、`integration_*` 为第三方侧来源。

---

## 一、日结多源汇总口径（T7）

### 1.1 汇总目标
按经营日把六源经营流水汇总进 `finance_daily_closes`（需补汇总金额/数量字段），形成可锁定的日结批次；锁定后改影响金额走补录审批（见 backlog T7）。

### 1.2 六个源流 → 日结
| 源流 | 权威源表 | 纳入口径（计入状态） | 金额/数量字段 | 维度 |
|---|---|---|---|---|
| 官方 App/小程序售票 | `ticket_orders` + `ticket_payment_intents` | 已支付 paid；退款单独入退款列 | 订单实付分 / 单数 | 项目·渠道·票种·商户 |
| 第三方渠道售票 | `integration_order_syncs`（provider=美团/大众点评/抖音） | 回流 status=accepted 且对账匹配 | 回流订单分 / 单数 | 渠道·项目 |
| 酒吧 | `bar_bills` + `bar_bill_settlements` | 已结算 settled（含现金/POS/聚合，settle_channel 全计） | 账单实收分 / 账单数 | 商户·酒吧·支付渠道 |
| 游乐 | `amusement_verifications` | 成功核销 success（游乐多为通票核销，**直接收入归票务**，此处计核销量/客流，不重复计收入） | 核销人次 | 项目·场次 |
| 供应链库存（成本侧） | `inventory_stock_movements` + `inventory_purchase_requests` | 出库消耗/采购入库（**成本与库存影响，不计经营收入**） | 消耗成本分·采购成本分 | 商户·库房·物料 |
| 第三方回流（餐饮/零售） | `integration_order_syncs`（provider=餐饮/零售 POS） | 回流 accepted（按第三方原则，开关开启后才有数据） | 回流分 / 单数 / 券核销数 | 商户·业态 |

> **净收入（已定稿）** = 自营售票实付 + 酒吧实收 − 当日退款，**不含第三方回流**。第三方回流（美团/点评/抖音/餐饮零售 POS）**仅作商户结算/对账口径，不计平台净收入**，T7 六源汇总中仍单列展示（`thirdparty_reflow` 行），但 `revenue.net` 派生时剔除该源。退款（`ticket_refunds` 当日发生额）**冲减当日净收入**，按经营日（可配日切）归集，落 `official_ticket` 源的 `refund_cent` 列。（酒吧侧无独立退款表，`bar_bill_adjustments` 为加项/挂账，暂不计退款；待业务进一步确认酒吧退款来源后补。）

### 1.3 四类差异识别（对账）
平台流水 vs 支付渠道（`finance_payment_intents`）vs 商户 vs 第三方（`integration_order_syncs`），写入 `finance_reconciliation_differences`（`difference_type` 需补 CHECK 枚举，替换自由文本）：
| 差异类型 | 定义 | 判定 |
|---|---|---|
| 金额差异 amount | 同一笔两侧金额不等 | platform.amount ≠ channel.amount |
| 数量差异 count | 笔数对不上 | platform 单数 ≠ 渠道单数 |
| 状态差异 status | 状态不一致 | 如平台已支付、渠道已退款 |
| 缺失差异 missing | 一侧有另一侧无 | 单号仅在一侧出现 |

### 1.4 结算依据
按 `merchant_id` + 合同（`contracts`）+ 周期分组 → `merchant_bills` / `merchant_bill_lines`，结合合作方式（自营/租赁/联营/代运营）口径生成可结算结果。

---

## 二、看板七类指标口径（T12）

> 格式：指标 | metric_code（建议）| 分子/分母或聚合 | 权威源表 | 维度。全部经 `currentMerchantScope` 隔离。

### 2.1 收入
| 指标 | metric_code | 聚合 | 源表 |
|---|---|---|---|
| 售票收入 | revenue.ticket | Σ 已支付订单实付分 | ticket_orders |
| 酒吧收入 | revenue.bar | Σ 已结算账单实收分 | bar_bills/bar_bill_settlements |
| 第三方回流收入 | revenue.thirdparty | Σ 回流 accepted 分 | integration_order_syncs |
| 退款 | revenue.refund | Σ 退款分 | ticket_refunds/finance_refunds/bar_bill_adjustments |
| 净收入 | revenue.net | 售票 + 酒吧 − 退款（**不含第三方回流**） | 派生 |

### 2.2 客流
| 指标 | metric_code | 聚合 | 源表 |
|---|---|---|---|
| 入场人数 | traffic.entry | count(成功入场核销) | ticket_verifications |
| 酒吧入场 | traffic.bar | Σ guest_count（开台/到店） | bar_table_sessions/bar_table_guest_checkins |
| 游乐核销人数 | traffic.amusement | count(success) | amusement_verifications |
| 渠道客流 | traffic.by_channel | 按 channel 分组客流 | ticket_orders+ticket_verifications |

### 2.3 核销
| 指标 | metric_code | 聚合 | 源表 |
|---|---|---|---|
| 权益发放数 | verify.issued | count | issued_entitlements |
| 核销数 | verify.done | count(status=success) | ticket_verifications+amusement_verifications |
| 核销率 | verify.rate | 核销数 / 发放数 | 派生 |
| 拒绝数 | verify.rejected | count(status=rejected/failed) | ticket_verifications |
| 人工放行数 | verify.manual_override | count | amusement_manual_overrides +（票务 manual_overridden 状态，见 backlog C1）|

### 2.4 会员
| 指标 | metric_code | 聚合 | 源表 |
|---|---|---|---|
| 新增会员 | member.new | count(period 内新建) | customers |
| 积分发放 | member.points_earned | Σ earn 方向积分 | customer_points_ledger |
| 积分兑换 | member.points_redeemed | count/Σ redeem | customer_points_redemptions/customer_points_ledger |
| 会员等级分布 | member.level_dist | 按 member_level 分组 | customer_points_accounts |

### 2.5 供应链
| 指标 | metric_code | 聚合 | 源表 | 备注 |
|---|---|---|---|---|
| 采购到货 | supply.received | Σ 入库确认 | inventory_purchase_requests | **当前仅"申请"，依赖 T6 采购周期落地** |
| 库存余额 | supply.balance | Σ on_hand | inventory_stock_balances | — |
| 吧台消耗 | supply.consumed | Σ(sale/pour/redeem_consume) | inventory_stock_movements | — |
| 缺货预警 | supply.low_stock | count(SKU 可用量 < 该 SKU 安全库存阈值) | inventory_items.min_quantity + inventory_stock_balances | **已定稿**：绝对值阈值/SKU 可配（复用 `inventory_items.min_quantity`，绝对值/SKU）；阈值 ≤ 0 视为不预警；已接入 ETL（平台+商户口径） |
| 盘点差异 | supply.variance | Σ count_correction | inventory_stock_adjustments | 盘点流程依赖 T6 |
| 存酒余额 | supply.saved_bottle | count/Σ(active) | saved_bottles | — |

### 2.6 风险
| 指标 | metric_code | 聚合 | 源表 | 备注 |
|---|---|---|---|---|
| 待复核 | risk.pending_review | count(pending) | supervisor_review_decisions+evidence_reviews+inventory_risk_alerts | — |
| 异常事件 | risk.incidents | count | amusement_incidents+analytics_risk_alerts | 通用异常链依赖 P1 risk_alerts |
| 补录数量 | risk.manual_backfill | count(补录) | — | **当前无补录字段，依赖 P1 留痕接线** |
| 第三方差异 | risk.thirdparty_diff | count | integration_reconciliation_differences | — |

### 2.7 财务
| 指标 | metric_code | 聚合 | 源表 |
|---|---|---|---|
| 日结状态 | finance.daily_close_status | 按 status 分组 | finance_daily_closes |
| 对账差异 | finance.recon_diff | count/Σ | finance_reconciliation_differences+integration_reconciliation_differences |
| 结算待确认 | finance.settle_pending | count(pending) | merchant_bills/finance_ar_ap_items |

---

## 三、隔离与权限
- 所有指标查询强制注入 `currentMerchantScope`：平台角色见全量，商户角色仅见本商户 merchant_id 行（修复 analytics 包 0 处隔离的缺口）。
- 前后端 DTO 字段名统一以本草案 metric_code 为准，修复总览 KPI 字段错位全 0（T12）。
- 下钻：总览 → 票务/酒吧/供应链/游乐/商户/异常/对账明细，按角色权限展示同一套经营事实。

---

## 四、财务/运营复核确认点（4 项已定稿，2026-06-19）
1. **【已定稿】经营日切时点 = 可后台配置，默认 04:00。** 配置项 `finance.business-day-cutoff-hour`（0–23，默认 4），凌晨该点前的流水归前一经营日。助手 `BusinessDayResolver.businessDate(Instant)`；T7 日结六源汇总与 T12 看板 ETL 的"经营日"分桶统一改用该助手（替换原自然日 UTC date）。
2. **【已定稿】第三方回流仅计商户结算口径，不计平台净收入。** 平台 `revenue.net` = 自营售票实付 + 酒吧实收 − 当日退款；第三方回流单列展示（T7 `thirdparty_reflow` 源、T12 `revenue.thirdparty`），net 派生时剔除。
3. 游乐通票收入是否在票务侧一次计入（避免与游乐核销重复计收入）——本文按"收入归票务、游乐计客流/核销"处理。**仍待确认。**
4. `ar_ap_items` vs `finance_ar_ap_items`、`finance_reconciliation_*` vs `integration_reconciliation_*` 两套表的权威收口（影响 T7）。**仍待确认。**
5. **【已定稿】退款冲减当日净收入。** net = 收入 − 当日退款；退款按经营日（可配日切）归集，T7 落 `official_ticket` 源 `refund_cent`（`ticket_refunds` 当日发生额）。
6. **【已定稿】缺货预警阈值 = 绝对值阈值/SKU 可配。** 复用 `inventory_items.min_quantity`（绝对值/SKU，阈值 ≤ 0 不预警），无需新增字段/迁移；T12 `supply.low_stock = count(SKU 可用量 < 该 SKU 阈值)`。

## 五、验收口径（草案）
- 日结金额=六源真实汇总（非种子），锁定后改影响金额被拒、补录走审批。
- 看板每个 metric_code 有真实聚合值（非种子、非全 0），且商户登录只见自身范围。
- 对账差异按金额/数量/状态/缺失四类自动识别（difference_type 有 CHECK）。

## 六、变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建聚合口径草案 V0.1，映射六源日结 + 七类指标到真实迁移表 + metric_code + 维度 + 隔离 | R5 backlog T7/T12；HTML #finance/#analytics；表名取自 V001–V065 |
| 2026-06-19 | **定稿**：财务确认 4 项口径并落代码——(1) 经营日切可配默认 04:00（配置项 `finance.business-day-cutoff-hour` + `BusinessDayResolver`，T7/T12 统一分桶）；(2) 第三方回流不计平台净收入（单列展示，`revenue.net` 剔除）；(3) 退款冲减当日净收入（按经营日归集，落 `official_ticket.refund_cent`）；(4) 缺货阈值绝对值/SKU 可配（复用 `inventory_items.min_quantity`，无新迁移）。新增 `supply.low_stock` 指标接入 ETL。§3（游乐通票收入归属）、§4（双表权威收口）仍待确认 | 2026-06-19 财务确认；代码：`BusinessDayResolver`、`FinanceService.summarizeDailyClose`、`JdbcFinanceRepository.aggregate*`、`DashboardAggregationJob`、`JdbcAnalyticsRepository.dayRange/countLowStockByMerchant` |
