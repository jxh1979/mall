# 第三轮 迁移级漂移抽查报告 R3（2026-06-18）

> 范围：把 `requirement-traceability-matrix-v0.1.md` 中其余标"已覆盖/已补 X 字段"的需求行，逐项拿到真实 Flyway 迁移（V001–V054）+ Java record + 服务代码核验，检测第二轮发现的"声称已补字段、迁移未落"漂移模式的覆盖面。
> 方法：4 路并行只读代码审计 + 审计员亲自 grep 复核，全部带 file:line。**只读、未改代码。**
> 机器标记：R3_MIGRATION_DRIFT_AUDIT_READY、SYSTEMIC_DESIGNED_NOT_MIGRATED_DRIFT_CONFIRMED。

## 0. 总裁决

第二轮的漂移**不是孤例，而是系统性的**。抽查的 11 条需求中，**仅 1 条字段全部坐实，其余全部存在不同程度漂移**。根因：**设计文档（dev-design / foundation-database-design）超前于实现编写，而追溯矩阵把"设计稿写了"当成了"已覆盖/已实现"**，其中相当一部分设计的表/字段从未进入 Flyway 迁移。

按性质分三类：

| 类别 | 含义 | 涉及需求 |
|---|---|---|
| **A 换了实现模型** | 需求功能大方向有承载，但矩阵声称的表/字段名不实、部分子能力未做 | REQ-12/13/14（积分）、REQ-34/37/40（留痕）|
| **B 真实功能缺失** | 任何常见命名下迁移+Java 均零命中，功能未实现 | REQ-22（按杯损耗）、REQ-60/61（职级/薪资）、REQ-69（审批层级）|
| **C 命名/单字段漂移** | 功能在，个别声称字段缺 | REQ-26（缺 deposit_rule）|

## 1. A 类：换了实现模型（需求大方向有载体，矩阵字段声明不实）

### REQ-34/37/40 全过程留痕 —— **最重要发现**
- 矩阵声称留痕三层模型 `business_process_instances` / `business_process_step_records` / `service_responsibility_records` 已建，且已补 `capture_source`（自动留痕率）、`refunded_by/waived_by/approved_by`（财务责任段）。
- **真相**：这三张表在 V001–V054 全部迁移中**从未建表**，仅存在于设计稿 `foundation-database-design.md:1202/1233/1255`。审计员复核 `grep business_process|service_responsibility|step_records|capture_source` 在迁移目录**零命中**。
- **实际留痕载体**：事件模型 `audit_events`（`V002:1`）、`evidence_events`（`V007:45`）、`evidence_attachments`（`V010:125`）、`evidence_reviews`（`V010:144`）。
- **结论**：留痕"大方向"有承载（事件 + 证据 + 复核），但**服务责任链（service_responsibility_records）、自动留痕率（capture_source）、业务过程时间线（process_type/business_object_type）三项核心子能力均未实现**。第一轮 R1-LOOP-07（留痕率无 KPI、process_type 缺提成/存酒）在迁移层是"空对空"——连承载列都不存在。用户本轮特别点名的"留痕闭环"在三层模型口径下**未落地**。

### REQ-12/13/14 会员积分 / 兑换
- 矩阵声称五件套 `points_accounts/points_ledger/points_rules/point_gifts/point_redemptions`、`member_level`、兑换写 `stock_movements(movement_type=redeem_consume)` 扣 `point_gifts.stock_quantity`。
- **真相**：实际表带 `customer_` 前缀——`customer_points_accounts`（`V009:162`）、`customer_points_ledger`（`V025:1`）、`customer_points_redemptions`（`V025:35`）、`customer_redemption_catalog_items`（`V025:15`）；**`points_rules`（规则引擎）与 `point_gifts`（礼物表）不存在**；等级列是 `level`（`V009:166`，默认 `'base'`）**非 `member_level`**；兑换扣库存走 `createStockAdjustment(... "loss" ...)`（`CustomerService.java:331-342`），`redeem_consume` 在迁移+代码零命中，typed-movement 白名单 `{outbound,damage,waste,loss}` 不含它（`InventoryStockMovementService.java:30-31`）。
- **结论**：积分账户/明细/兑换功能存在；**积分规则后台（points_rules）与礼物库存（point_gifts）未建**，兑换扣减机制与声称不符。

## 2. B 类：真实功能缺失（审计员亲自复核零命中）

| 需求 | 声称 | 复核结果（迁移 + Java 全零命中）|
|---|---|---|
| **REQ-22** 按杯销售损耗 | bar_products 补 `standard_pour_volume/pour_volume_unit/loss_threshold`；bar_order_items 补 `actual_pour_volume/loss_volume` | `grep pour\|loss_threshold\|actual_.*volume` 迁移零命中；bar_products(`V013:88-103`)/bar_order_items(`V013:130-144`) 建表无相关列，无后续 ALTER。**按杯出酒量扣减/损耗功能未实现**（REQ-22 仍是缺字段，矩阵第207行将其从"缺字段"改判"已覆盖"无代码依据）|
| **REQ-60** 职级 M1-M10 上中下 | `staff_levels` 拆 `main_level/sub_level`、派生 `level_rank` 1..30 | `grep staff_level\|job_level\|staff_grade\|level_rank\|main_level\|sub_level` 迁移+Java 零命中。**职级体系未实现** |
| **REQ-61** 薪资 | 补 `base_salary_amount/salary_currency/salary_detail/salary_policy_summary` | `grep salary\|pay_grade\|salary_band` 迁移+Java 零命中。**薪资字段未实现** |
| **REQ-69** 审批层级 | 枚举 `one_level_up/two_levels_up/top_authority/specific_role`；`admission_audits.approval_level` 留痕 | `grep admission\|approval_level\|one_level_up\|...` 迁移零命中。**审批层级枚举与 admission_audits.approval_level 未实现** |

> 注：B 类已用多种常见别名 grep 复核（含 java 端 camelCase），高置信判定为功能缺失；如 owner 确知存在完全不同概念模型的等价实现，请指出，否则按未实现处理。

## 3. C 类：命名/单字段漂移

| 需求 | 结果 |
|---|---|
| REQ-26 订台来源/定金 | 6 列中 5 列坐实（`order_source/created_terminal/sales_staff_id/device_id/deposit_status`，`V013:33-38`），**唯 `deposit_rule` 在迁移+代码零命中**（定金"规则"仅以 `deposit_amount_cent`+`deposit_status` 体现）|

## 4. REQ-46/48 游乐（A/B 混合，单列）
- REQ-46：声称的 `amusement_capacity_config`/`amusement_capacity_snapshot` 两表**不存在**（DRIFT）；容量仅场次级单维度（`amusement_sessions.capacity/used/locked`，`V014:55-56`），**非"项目+场次+点位/设备"三维度**；核销时安全状态+容量双校验**已实现**（`AmusementService.java:439-445`，PARTIAL）。
> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-10，本文不再重复记载。

## 5. 抽查覆盖面汇总

| 需求 | 裁决 | 类别 |
|---|---|---|
| REQ-12/13/14 积分 | PARTIAL / DRIFT | A |
| REQ-22 按杯损耗 | DRIFT | B |
| REQ-26 订台 | PARTIAL（缺 deposit_rule）| C |
| REQ-34/37/40 留痕 | DRIFT（三层模型未建，事件模型替代）| A |
| REQ-46 游乐容量 | DRIFT + PARTIAL | A/B |
| REQ-48 游乐检修 | DRIFT + PARTIAL | A/B |
| REQ-60 职级 | DRIFT | B |
| REQ-61 薪资 | DRIFT | B |
| REQ-69 审批层级 | DRIFT | B |

合并第二轮（REQ-21/26b/30/32），**已核 15 条需求，仅极少数字段全部坐实**——漂移是矩阵的系统性问题，不是个别条目。

## 6. 影响与建议

1. **追溯矩阵不可作为"已实现"凭据**：它反映的是设计意图，不是迁移现实。本轮已在矩阵顶部加全局修订横幅，并对 R2/R3 涉及行加注（见矩阵）。
2. **`requirements-code-implementation-matrix-v0.1.csv` 的"已完整实现"列需 owner 据 R2/R3 全面复核**——至少 REQ-22/30/32/34/37/40/60/61/69 不应标"已完整实现"。
3. **重生成机制**：建议新增 `verify:matrix-vs-migration`（占位）——以迁移目录为真相源，对矩阵声称的表/列做存在性校验，杜绝"设计=覆盖"的复发。
4. **优先级研判（供产品/总控）**：
   - 若留痕"服务责任链 + 自动留痕率"、职级/薪资、审批层级、按杯损耗属 P0 范围 → 它们是**真实开发缺口**，需排期实现（非文档问题）。
   - 若这些属 P1/P2 后置 → 需把矩阵从"已覆盖"改为"P1/P2 待实现"，并从 P0 完成声明中剔除。
5. **第四轮建议**：对剩余未抽查的"已覆盖"行（REQ-02/23/35/36/38/39/52/53/55/64/65 等）做同样的迁移级抽查，给出完整的"矩阵声称 vs 迁移现实"对照表。
