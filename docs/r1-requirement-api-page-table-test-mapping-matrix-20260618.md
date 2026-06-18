# 第一轮 需求-接口-页面-数据表-测试 映射矩阵 R1（2026-06-18）

> 定位：把"需求 → 接口 → 页面 → 数据表 → 测试"五段链路**贯通成单一视图**，并叠加本轮新发现的链路断点。本矩阵是**矩阵之矩阵**：完整逐条数据已由既有矩阵分段承接，本文确认其覆盖关系、补"数据表段"、并标注断链。
> 机器标记：R1_REQ_API_PAGE_TABLE_TEST_MATRIX_READY。

## 1. 链路分段承接源（确认覆盖）

| 链路段 | 承接文档 | 当前态 |
|---|---|---|
| 需求 → 文档 | `requirement-traceability-matrix-v0.1.md`（REQ-01~92）| 覆盖 |
| 需求 → 代码/测试 | `requirements-code-implementation-matrix-v0.1.csv`（96 项：88 完整 / 8 部分）| 覆盖（含逐 REQ 的页面/API/表/代码/测试列）|
| 接口 → 权限/事件/终端/测试 | `p0-openapi-operation-acceptance-matrix-v0.1.md`（207 op）| 覆盖（唯一未漂移 op 数源）|
| 任务 → 接口 → 权限 → 事件 → 验收 | `p0-task-api-permission-event-acceptance-map-v0.1.md` | 覆盖 |
| 页面 → 权限/字段/UI 验收 | `full-admin-page-permission-field-matrix-v0.1.csv`（30 页）+ `full-admin-ui-acceptance-matrix-v0.1.md` | 覆盖 |
| 数据表 → scope/隔离 | `data-isolation-and-tenant-scope-security-spec-v0.1.md` + `domain-ownership-scope-audit-v0.1.csv` | 覆盖（但只到 V052，漏 V053/V054）|
| 需求 → 数据表 | 散落于 `foundation-database-design.md` + 各域 design | 部分（无单一 REQ→table 矩阵）|

## 2. 按业务域贯通视图（关键 REQ 抽样 + 断点标注）

> 状态来源：requirements-code CSV。断点列为本轮 R1 新增（对应审计报告 §3 / §6）。

| REQ | 域 | 接口（节选）| 页面 | 关键数据表 | 测试 | 实现态 | 链路断点（R1）|
|---|---|---|---|---|---|---|---|
| REQ-01/05/06/08/09 | 票务核销 | `POST /ticketing/orders`、`/orders/{id}/issue`、核销 verify | ADM-TKT-003 | `ticket_orders`/`ticket_credentials`/`ticket_verifications` | TicketingService/Controller/Jdbc Tests | 完整 | 核销无 `manual_overridden` 状态、刷新周期无字段（R1-LOOP-10/11）|
| REQ-10/11 | 通票/权益 | `/ticketing/entitlement-templates`、`/issued-entitlements` | ADM-TKT-001/003 | `entitlement_templates`/`issued_entitlements` | 同上 | 完整 | 餐饮券无容量承接表（P3）|
| REQ-12/13/14 | 会员积分/兑换 | `/inventory/saved-bottles`、积分相关 | — | `points_*`/`point_gifts`/`point_redemptions` | InventoryTests | 完整 | V054 积分余额非负约束未入隔离规格（R1-FINDING-A）|
| REQ-17/18/19 | 酒吧卡座/低消 | `/bar/orders`、`/bar/bills/{id}/settle` | ADM-BAR-001/002 | `bar_tables`/`bar_orders`/`bar_bills` | BarService/Jdbc Tests | **REQ-17 部分** | 定金事件缺失、聚合支付入口缺（R1-LOOP-02/03）|
| REQ-21 | 扫码支付 | `/bar/bills/{id}/pay-intents`、`/settle` | ADM-BAR-002 | `bar_bill_payments` | BarServiceTests | **部分** | 酒吧无 finance payment-intents 入口、枚举不一致（R1-LOOP-02/13）|
| REQ-23/REQ-23b | 酒水领取/年龄核验 | `/bar/age-verifications`、兑换点单 | ADM-BAR-002 | `bar_age_verifications`/`issued_entitlements` | BarServiceTests | **REQ-23b 部分** | 兑换 verify 接口未进 OpenAPI（R1-LOOP-08）；库存事件双轨（R1-LOOP-06）|
| REQ-25/26/26b | 订台定金/提成 | `/bar/reservations`、`/bar/bills/{id}/commissions/calculate` | ADM-BAR-001/004 | `bar_reservations`/`bar_table_commissions` | BarServiceTests | 完整 | 提成无 `ar_ap_items` 生成流程（R1-LOOP-01）；定金支付事件缺（R1-LOOP-03）|
| REQ-30/31/32 | 客人存酒 | `/inventory/saved-bottles`、`/{id}/claim` | ADM-BAR-003 | `saved_bottles`/`saved_bottle_claims` | Inventory/BarTests | **REQ-32 部分** | 高价值字段断链、核身复核状态缺、手机号脱敏 vs 查询（R1-LOOP-04/05）|
| REQ-34/40 | 全过程留痕 | 事件中心 | ADM-EVD-001 | `business_process_instances`/`step_records`/`service_responsibility_records` | Evidence Tests | 完整 | 留痕率无 KPI、process_type 缺提成/存酒（R1-LOOP-07）|
| REQ-40/41/44 | 第三方留痕/配送 | `/integration/*`、webhook | ADM-INT-001/002 | integration 事件表 | IntegrationServiceTests | **部分** | 真实供应商待资料（开关关闭）|
| REQ-46/47/48 | 游乐容量/安全 | `/amusement/*` | ADM-AMU-001/003 | `amusement_capacity_*`/`amusement_maintenance_record` | AmusementTests | 完整 | 多人票×容量交互未定义（R1-LOOP-12）|
| REQ-58/59/63 | 权限/手动录入 | `/access/users`、`/permission-templates/{id}/apply` | ADM-ACC-001/002 | `permissions`/`user_permission_overrides`/权限模板表组 | AccessTests | 完整 | 后台手动录入入口未被治理 PRD 承认、字段/同意凭据/事件缺（R1-LOOP-09/14）|
| REQ-64~69 | 治理/合同/审批 | `/governance/*`、`/contracts/*` | ADM-GOV-001~006 | `merchants`/`contracts`/`admission_audits`/`supervisor_review_decisions` | GovernanceTests | 完整 | `supervisor_review_decisions` 无 scope 列（ISO-GAP）；V053 发起人列未入隔离规格 |

## 3. 数据表 → scope 隔离链（补"数据表段"，叠加 R1 漂移）

| 迁移 | 表 | scope 列 | 隔离规格已记 | R1 核对 |
|---|---|---|---|---|
| V044 | `ar_ap_items` 等 | merchant_id | 是 | ✅ |
| V046 | inventory 10 表 + saved_bottles | merchant_id | 是 | ✅（`chk_stock_adjustment_type` NOT VALID 待 VALIDATE）|
| V047 | bar 6 表 | merchant_id | 是 | ✅ |
| V048 | roles | organization_id | 是 | ✅ |
| V049 | amusement 4 表 | merchant_id | 是 | ⚠️ 无索引（ISO-GAP-03）|
| V050 | supervisor_review_decisions | 无 scope 列 | 是（§7）| ⚠️ ISO-GAP 待裁决 |
| V051 | compliance_reminders | merchant_id | 是 | ✅ |
| V052 | mall_floor_plans | merchant_id | 是 | ✅ |
| **V053** | supervisor_review 发起人 | 待核 | **否** | ❌ **隔离规格未登记**（本轮新发现）|
| **V054** | customer points 余额非负约束 | 约束类 | **否** | ❌ **隔离规格未登记**（本轮新发现）|

## 4. 矩阵维护纪律（建议固化）
- 每次 OpenAPI 变更：改 OpenAPI → 生成 operation matrix/closure → `verify:p0-contract` → `verify:delivery-gates`。
- 每次新增带租户数据的表：补 `domain-ownership-scope-audit` CSV + 隔离规格 §3 + AC-ISO-1/2 用例（隔离规格 §8 双向绑定）。
- 计数类断言一律引用实时校验输出，不写静态快照（R1-FINDING-A）。
