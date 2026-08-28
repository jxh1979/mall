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

> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §B-10，本文不再重复记载。
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

> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §B-11，本文不再重复记载。
