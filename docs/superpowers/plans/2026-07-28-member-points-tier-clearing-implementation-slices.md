# 会员积分 / 等级 / 清算 — 实现切片与差距矩阵 v0.1

> 日期：2026-07-28  
> 基线：`main@40929204`  
> 产品依据：`docs/member-points-tier-and-clearing-prd-v1.0.md`  
> 代码基线：`LoyaltyService` / `CustomerPoints*` / OpenAPI loyalty&points  

## 1. 现状摘要

| 能力 | 现状 | PRD 差距 |
|---|---|---|
| 入账 | `earnPoints` 直接加可用；`earnPointsPending`/`confirmPoints` 履约后可用 | 缺「确认后冻结→退货周期解冻」；pending≠退货冻 |
| 等级加赠 | `pointMultiplierBasisPoints` 乘算 | 应改为加赠率分源；成本归商户；活动独立 |
| 成长值 | `keepThresholdProgress` | 缺冻结/有效成长值分离 |
| 过期 | FIFO batch expire | 应 V-12 整池续期（可配） |
| 冻结 | 账户级风控冻结 | 应分笔冻结；二者并存 |
| 退款 | 按 confirmed 比例 deduct | 要 R-SYM+成长值+重算等级 |
| 兑换 | 扣 balance 兑券 | 要 O-BAN、兑换即承接、双价 |
| 等级 | keep/grace/nextLower | 要 T-6-STRICT 可跨级降 |
| 清算 | 基本缺 | 双价、商户户、城市统计子账、不代收消费款 |
| 强制发分 | 弱 | 入驻/关分合规 |

## 2. 切片（S0–S12）

| 切片 | 名称 | 优先级 | 依赖 |
|---|---|---|---|
| S0 | 特性开关/配置/指标 | P0 | — |
| S1 | 账本：可用/冻结消费积分+成长值；来源 bucket | P0 | S0 |
| S2 | 入账引擎：基础分+等级加赠+活动；即消白名单 | P0 | S1 |
| S3 | 解冻 Job | P0 | S1-S2 |
| S4 | 退款 R-SYM + 等级重算 | P0 | S1-S3 |
| S5 | V-12 整池续期 | P0 | S1 |
| S6 | T-6-STRICT 等级 | P0 | S1 |
| S7 | 兑换目录 + O-BAN 禁抵现 + 兑换即承接 | P0 | S1 |
| S8 | 清算双价、商户户、城市统计子账 | P1 | S2/S7 |
| S9 | 强制发分合规 | P1 | S2/S8 |
| S10 | OpenAPI/Admin/C端/收银 | P1 | 并行 |
| S11 | 数据迁移与对账 | P0 | S1-S6 |
| S12 | 清理旧倍率/孤儿表/旧设计文 | P2 | 后 |

### 迭代建议
- I1: S0+S1+S11 方案  
- I2: S2+S3+S4  
- I3: S5+S6  
- I4: S7+S10  
- I5: S8+S9  
- I6: S12+全量对账  

## 3. 代码锚点（现网）

- `LoyaltyService.earnPoints` / `earnPointsPending` / `confirmPoints` / `deductPoints` / `expirePoints` / `freeze` / `unfreeze` / `evaluateAccount`
- `CustomerPointsRuleRecord`：earnRatio、validityDays、expireMode…
- `CustomerMemberLevelRecord`：upgrade/keep/grace/**pointMultiplierBasisPoints**
- `CustomerPointsAccountRecord`：balance、frozenPoints（账户冻结语义）、keepThresholdProgress
- OpenAPI：`/api/v1/loyalty/*`、`/api/v1/customer/points*`

## 4. 差距矩阵（摘要）

| PRD | 现状 | 切片 |
|---|---|---|
| 冻结解冻到账 | pending/confirm≠退货冻 | S2-S3 |
| 等级加赠分源 | 倍率乘算 | S2 |
| V-12 整池 | batch FIFO 过期 | S5 |
| T-6-STRICT | nextLower/grace | S6 |
| O-BAN/承接 | 兑换有、禁抵现/清算弱 | S7-S8 |
| R-SYM | 部分 deduct | S4 |
| 双价清算 | 缺 | S8 |

## 5. I1 开工清单（S0+S1）

1. Flags + 配置项  
2. DB migration：账户字段与流水字段  
3. 读写 API 返回 available/frozen  
4. 迁移草案：旧 balance→available  
5. 单测骨架  

## 6. 非目标
改 HTML、统一收银牌照、城市专属积分、整单抵现、无配置校验的假单价进生产。