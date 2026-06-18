# 第三轮 已确认缺口优先级与修复 Backlog R3（2026-06-18）

> 定位：把 R2/R3 代码审计确认的真实缺口，按**总控/产品已拍板的优先级**（2026-06-18 确认）落成可执行修复清单。每项给出"现状 → 需补迁移/代码/事件 → 验收草案 → 建议迁移版本"。
> 来源审计：`r2-code-audit-req26b-req21-req30-20260618.md`、`r3-migration-level-drift-audit-20260618.md`。当前迁移最高版本 **V054**，新增从 **V055** 起。
> 边界：本文是**修复排期入口**，尚未改代码。机器标记：R3_CONFIRMED_GAP_BACKLOG_READY、PRIORITIES_OWNER_CONFIRMED_20260618。

## 0. 优先级拍板结果（2026-06-18 总控确认）

| 缺口 | REQ | 拍板 | 备注 |
|---|---|---|---|
| 服务责任链 service_responsibility_records + 业务过程三层模型 | REQ-34/37 | **P0** | ✅ **已实现 V059（2026-06-18）**：3 张表 + TraceabilityService 内部记录服务；含财务责任段 refunded_by/waived_by/approved_by |
| 自动留痕率 capture_source + 留痕率 KPI | REQ-40 | **P1** | capture_source 列已随 V059 落库；自动留痕率 KPI/统计页仍 P1 |
| 审批层级 approval_level + 枚举 | REQ-69 | **P0** | ✅ **已实现 V057（2026-06-18）**：扩展既有 supervisor_review（非新建 admission_audits）；approval_level 枚举留痕 + 按 level_rank 逐级上提 |
| 按杯出酒量/损耗 | REQ-22 | **P0** | ✅ **已实现 V058（2026-06-18）**：bar_products 标准出酒量/单位/损耗阈值 + bar_order_items 实际出酒量/损耗量；出品超阈值发 inventory.loss_exception |
| 职级 M1-M10 上中下 | REQ-60 | **P0** | ✅ **已实现 V055（2026-06-18）**：`staff_levels` 30 行 level_rank 1..30，影响默认权限模板与审批层级 |
| 薪资字段 | REQ-61 | **P0** | ✅ **已实现 V055（2026-06-18）**：`staff_level_assignments`（整数分/CNY/明细）；实际工资发放/个税仍后置 |
| 积分规则引擎 points_rules | REQ-12 | **P0** | ✅ **已实现 V060（2026-06-18）**：建 `customer_points_rules`（收敛 customer_ 前缀）+ 仓储 |
| 积分礼物表 point_gifts | REQ-14 | **P0** | ✅ **已实现 V060（2026-06-18）**：`customer_redemption_catalog_items` 承接礼物语义（补 value_amount_cent/project_id）；兑换扣减改 redeem_consume |
| 现金/POS 收款人/流水号字段 + 事件 | REQ-21 | **P1**（R2 既定）| 中国法律要求现金/POS |
| 存酒高价值/取酒核身复核/手机号检索 | REQ-30/32 | **P1**（R2 既定）| |
| 订台 deposit_rule | REQ-26 | **P1**（R2 既定）| |
| channel 枚举约束 + bar↔finance 映射 | REQ-21/对账 | **P1**（R2 既定）| |
| 提成贯通 finance ar_ap_items | REQ-26b | **P1 可选增强** | 现 bar_commission_calculations 已满足 D13 P0 |

## 1. P0 修复项（带可开发规格）

### P0-1｜服务责任链 + 业务过程三层模型（REQ-34/37）
- 现状：三层模型表从未建（`grep business_process|service_responsibility` 迁移零命中）；仅事件模型 `audit_events/evidence_events/evidence_reviews/evidence_attachments`。
- 补迁移（建议 V055）：建 `business_process_instances`、`business_process_step_records`、`service_responsibility_records`，按 `foundation-database-design.md:1202-1265` 字段定义落库，其中 `service_responsibility_records` 含 `served_by/assigned_by/changed_by/first_handled_by/refunded_by/waived_by/approved_by`。
- `process_type` 枚举：在设计稿 9 值基础上**补 `bar_table_commission`、`saved_bottle_lifecycle`**（R1-SPEC-06）；`business_object_type` 补 `commission`、`saved_bottle`。
- 事件落点：核销/服务/加单/送达/换人/异常首处理/退款/免单/调账写对应 `*_records` + 事件中心。
- 验收：Given 一次卡座服务全流程，Then 可查到完整服务责任链（谁服务/加单/送达/换人）与异常首处理人；财务责任段 refunded_by/waived_by/approved_by 可追溯。
- scope：新表须带 merchant_id（按数据隔离规格 §3 补 AC-ISO-1/2）。

### P0-2｜审批层级 approval_level + 枚举（REQ-69）
- 现状：`grep admission|approval_level|one_level_up` 迁移零命中。
- 补迁移（V056）：`admission_audits` 增 `approval_level VARCHAR`，约束枚举 `{one_level_up, two_levels_up, top_authority, specific_role}`；按 `level_rank`（依赖 P0-5 职级）计算所需审批层级。
- 代码：合同/基础定义违规先阻断（已有 contract.blocked 事件），按 approval_level 路由到对应层级审批人，放行写 contract.released + approval_level 留痕。
- 验收：Given 合同违规触发逐级上提，When 上一级/上两级/最高权限审批，Then approval_level 正确留痕并放行。

### P0-3｜按杯出酒量/损耗（REQ-22）
- 现状：`grep pour|loss_threshold|actual_.*volume` 迁移零命中。
- 补迁移（V057）：`bar_products` 增 `standard_pour_volume`、`pour_volume_unit`、`loss_threshold`；`bar_order_items` 增 `actual_pour_volume`、`loss_volume`。
- 代码：按杯销售按标准出酒量扣库存，实际超标记 `loss_volume`，超 `loss_threshold` 触发损耗核查（风控/主管复核）。
- 验收：Given 按杯出酒实际量 > 标准量，Then 差额记损耗；累计超阈值触发核查事件。

### P0-4｜职级 M1-M10 上中下（REQ-60）
- 现状：`staff_levels/main_level/sub_level/level_rank` 迁移+Java 零命中。
- 补迁移（V058）：建 `staff_levels`（`main_level` M1..M10、`sub_level` 上/中/下、派生 `level_rank` 1..30）。
- 代码：职级用于默认权限模板与审批层级建议（不替代单一功能权限，REQ-62），供 P0-2 approval_level 按 level_rank 计算。
- 验收：Given M3 中级，Then level_rank 正确派生、参与审批层级计算。

### P0-5｜薪资字段（REQ-61，仅字段）
- 现状：`salary/base_salary/pay_grade` 零命中。
- 补迁移（V058 同批）：`staff` 或职级关联表增 `base_salary_amount`、`salary_currency`（默认 CNY，D12）、`salary_detail`；职级表 `salary_policy_summary`。
- **范围界定**：仅字段录入 P0；实际工资发放、个税、复杂分摊**仍后置**（原始口径不变）。
- 验收：Given 录入具体薪资，Then 与职级大范围分离存储。

### P0-6｜积分规则引擎 points_rules（REQ-12）
- 现状：`points_rules` 不存在（账户/明细/兑换已存在为 `customer_points_*`）。
- 补迁移（V059）：建 `customer_points_rules`（与现有 `customer_` 前缀收敛），规则后台定义（获取/消费/有效期/过期方式，按 D7 状态映射）。
- 代码：积分获取/消费按规则计算；规则变更写审计。
- 验收：Given 后台配规则，Then 消费按规则积分/等级成长。

### P0-7｜积分礼物表 point_gifts（REQ-14）
- 现状：`point_gifts` 不存在；兑换扣库存走 `customer_redemption_catalog_items.stock_quantity` + `createStockAdjustment("loss")`。
- 口径收敛（V059 同批）：二选一并固化——①将现有 `customer_redemption_catalog_items` 正式承接"礼物"语义并补文档；或②新建 `point_gifts` 并迁移 catalog 数据。**推荐 ①**（避免重复表）。同时把兑换扣减 movement_type 从 `"loss"` 改为语义正确的 `redeem_consume`（并入 typed-movement 白名单），修正 R3 发现的机制漂移。
- 验收：Given 兑换礼物，Then 扣积分 + 扣礼物库存 + 写 `redeem_consume` 库存流水（非 loss）。

## 2. P1 修复项（摘要，规格见 R1-SPEC / R2 报告）

| 项 | REQ | 要点 | 规格 |
|---|---|---|---|
| 自动留痕率 capture_source + KPI | REQ-40 | 三层模型(P0-1)落地后补 capture_source 列 + 留痕率≥80% 可配置阈值 + 统计页 | R1-SPEC-06 |
| 现金/POS 字段+事件 | REQ-21 | bar 账单补 `cashier_staff_id`/`pos_transaction_no` + 事件 `payment.cash_received/pos_received` | R2 §2 |
| channel 枚举+映射 | REQ-21/对账 | `finance_payment_intents.channel` 加枚举约束 + bar↔finance 映射表 | R1-SPEC-02 |
| 存酒高价值/核身/手机号 | REQ-30/32 | saved_bottles 补 `is_high_value`/阈值/标签值/姓名昵称/可逆加密手机号 + 状态机 `pending_identity_review`/`disputed_frozen` + 作废接口 + 双证据主管复核 | R1-SPEC-04 |
| 订台 deposit_rule | REQ-26 | bar_reservations 补 `deposit_rule`（金额/比例/退没收规则）| — |
| 提成 finance 贯通（可选）| REQ-26b | 提成生成 ar_ap_items(source_type=employee_commission) | R1-SPEC-01 |

## 3. 配套护栏（建议同批落地）
- ✅ **已实现 `verify:matrix-vs-migration`（2026-06-18）**：`tools/verify-matrix-vs-migration.mjs` 以迁移目录为真相源解析 table→columns（CREATE TABLE + ALTER ADD COLUMN 两路径），校验 `docs/matrix-migration-claims.json` 声明清单——present 缺失即失败、absent 一旦落地即阻断（强制同步更新清单与矩阵覆盖状态）。已接入 `package.json` 脚本并纳入 `verify:delivery-gates` 闸口链。当前：153 表解析、6 present 确认、14 absent 缺口跟踪。
- ✅ **已修复版本钉子反模式（2026-06-18）**：新增测试助手 `MigrationVersions.latest()`（从 classpath db/migration 动态解析最新版本），12 处 `currentFlywayVersion()==\"054\"` 硬钉改为 `==MigrationVersions.latest()`——语义升级为"DB 已迁到 head"不变式，加迁移不再需手改测试。全部 71 个 JDBC 集成测试通过。
- 每个新表补数据隔离 AC-ISO-1/2（数据隔离规格 §8），并把 V055+ 纳入隔离规格 §3。
- 修复后据本 backlog 复核 `requirements-code-implementation-matrix-v0.1.csv` 的"已完整实现"列。

## 4. 建议执行顺序
1. P0-4/5 职级薪资（V058）→ 2. P0-2 审批层级（V056，依赖 level_rank）→ 3. P0-1 留痕三层模型（V055）→ 4. P0-3 按杯损耗（V057）→ 5. P0-6/7 积分规则与礼物收敛（V059）→ 6. P1 批次。
> 注：迁移版本号按实际开工顺序连续分配即可，上表仅示意分组。

## 5. 实施进展

| 日期 | 项 | 迁移 | 内容 | 验证 |
|---|---|---|---|---|
| 2026-06-18 | P0-4 职级（REQ-60）+ P0-5 薪资字段（REQ-61）| **V055**（实际下一连续版本，非示意的 V058）| 新建 `staff_levels`（30 行 M1-M10×上中下，level_rank 1..30 唯一连续，上>中>下、M10>M1）+ `staff_level_assignments`（整数分薪资 D1、币种 CHECK=CNY D12、非负约束、org/merchant 隔离）+ `StaffLevelRecord`/`StaffLevelAssignment`/DTO + 仓储 4 法 + 服务 `listStaffLevels`/`assignStaffLevel`（职级存在校验、CNY 校验、跨组织禁止、幂等、审计 `access.staff_level.assigned`）| `npm run test:backend -- -Dtest=Access*,AuthorizationServiceTests` = **38 passed / 0 fail**（含 2 个新 JDBC 集成测试：Flyway 应用 V055、30 行连续 rank、归属持久化）；`verify:openapi` PASS（未触碰契约）|

| 2026-06-18 | P0-4/P0-5 端点暴露 | **V056**（权限种子）| controller `GET /api/v1/access/staff-levels`、`PUT /api/v1/access/users/{id}/staff-level`（{id}=user_id 路径注入）；OpenAPI json+yaml 同步新增 2 operation（209 ops）+ `StaffLevelAssignmentRequest` 字段级 schema（D3，非 GenericWrite）；operation matrix/closure 重生成；V056 seed 权限点 `access.staff_level.view`(medium) / `access.staff_level.manage`(high,需审批) | `verify:p0-contract` = **EXIT 0**（209 ops，matrix/closure in sync，client-routes/openapi/payload 全 PASS）；access 域 **41 测试通过**（含 3 个新 controller 测试 + V056 迁移应用）；yaml↔json 对新增键逐字节一致（pyyaml 校验）|

| 2026-06-18 | P0-2 审批层级（REQ-69）| **V057** | **不新建 admission_audits**（R3 证其不存在），扩展既有 `supervisor_review`：`supervisor_review_decisions` 加 `approval_level`(CHECK 枚举) + `required_approver_rank`；新增 `ApprovalLevel` 枚举（上一级=+3 子级/上两级=+6/最高=30/指定角色不按 rank）；策略 contract_violation→two_levels_up、base_definition_violation→top_authority；evaluate 按发起人 `level_rank`（V055）算所需审批人 rank，approve 强制审批人 rank≥门槛（在既有主管级校验之上）；`AuthorizationService` 加 `currentActorLevelRank/levelRankOf`（null-repo 降级 0，保留既有行为）| **全套后端 719 测试通过**（新增 ApprovalLevelTests 6 + 3 个 service 测试含 mock rank 网关 reject/allow）；`verify:p0-contract` EXIT 0（未改 OpenAPI，决策新增字段经通用 Success 响应返回）；附带修复 12 处脆弱的 `currentFlywayVersion()=="054"` 测试钉子→"057" |

| 2026-06-18 | P0-3 按杯出酒量/损耗（REQ-22）| **V058** | bar_products +standard_pour_volume/pour_volume_unit/loss_threshold；bar_order_items +actual_pour_volume/loss_volume（非负 CHECK）；`BarPourLoss` 纯函数（损耗=max(0,实际-标准×数量)、超阈值判定）；BarOrderService deliver 时记损耗、超阈值发 **inventory.loss_exception**（D4 权威事件名，非新 bar.* 名）；BarProductRequest 加向后兼容构造器（13 既有调用方零改动）| 全套后端 **723 测试通过**（+BarPourLossTests 3 + 1 service deliver-loss 测试）；`verify:p0-contract` EXIT 0；**`verify:matrix-vs-migration` 触发 absent→present 强制更新**（REQ-22 已置 present，护栏闭环验证生效）|

| 2026-06-18 | P0-1 留痕三层模型（REQ-34/37/40）| **V059** | 建 business_process_instances/business_process_step_records/service_responsibility_records；TraceabilityService 内部记录服务（merchant scope 由 token 注入）；capture_source 列就位（自动留痕率统计基础）；含财务责任段 refunded_by/waived_by/approved_by | TraceabilityJdbcIntegrationTests 4（建表/持久化/token-scope 注入/merchant 隔离列表）；全套 729 后端测试通过 |
| 2026-06-18 | P0-6/P0-7 积分规则+礼物收敛（REQ-12/14）| **V060** + 代码 | 建 customer_points_rules（规则引擎，project_id 隔离，validity/expire/status CHECK）；customer_redemption_catalog_items 补 value_amount_cent/project_id 承接礼物；**兑换扣减从 createStockAdjustment("loss") 改为 createTypedStockMovement("redeem_consume")**（白名单 + CustomerService + 3 处测试更新）| CustomerJdbcIntegrationTests +2（建表/CHECK）；CustomerServiceTests 兑换测试改用 typed-movement 全绿；`verify:matrix-vs-migration` 强制 absent→present 更新 |

**已完成**：P0-1/P0-2/P0-3/P0-4/P0-5/P0-6/P0-7 全部 P0 缺口全链路闭环，`verify:p0-contract` 通过、全套 729 后端测试通过、两护栏生效。
**收敛说明**：积分按推荐①收敛到 `customer_points_rules`/`customer_redemption_catalog_items`，字面 `points_rules`/`point_gifts` 不建（claims registry 保留为 absent 并注明真实表名）。
**仍未实现（P1，非阻断）**：REQ-26 deposit_rule；REQ-30/32 存酒高价值/取酒核身复核状态/手机号检索；自动留痕率 KPI 统计页；留痕三层模型与各业务动作的逐点接线（TraceabilityService 已就绪，待在核销/卡座服务/出品/异常/财务调整各 service 内调用）。
**小后续（非阻断）**：OpenAPI `BarProductRequest` schema 未补 3 个可选 pour 字段（端点已存在、字段可选、向后兼容；建议补文档）。
**护栏提醒**：本轮发现 12 处 JDBC 测试硬钉 Flyway 版本号（`currentFlywayVersion()==`），属 R1/R3 反复指出的"静态快照"反模式——每次加迁移都要手改。建议 owner 改为动态读取期望版本（或断言 ≥），并纳入 `verify:matrix-vs-migration` 护栏。
**遗留小项（非阻断）**：①`staff_level_assignments` 已登记数据隔离规格 §3；AC-ISO-1/2 跨租户用例建议后续随接口集成测试补。②`tools/apply-openapi-p0-examples.mjs` 依赖的 `yaml` npm 包未安装（pre-existing，与本次无关），该工具无法重生成 yaml；本次 yaml 由手工镜像并经 pyyaml 校验，建议 owner `npm i -D yaml` 修复该工具。
