# 第二轮 代码审计报告 R2：REQ-26b / REQ-21 / REQ-30 链路断点核实（2026-06-18）

> 范围：把第一轮（`r1-requirements-documentation-audit-report-20260618.md`）发现的 3 处文档矛盾，拿到真实 Java 代码 + Flyway 迁移 + OpenAPI + 测试里核实，判定"文档矛盾是否被代码规避"还是"代码层真实缺口"。
> 方法：3 路并行只读代码审计（documentation-specialist 审计工作流 + gstack-review 对抗式复核），全部带 file:line 证据。**只读、未改代码。**
> 机器标记：R2_CODE_AUDIT_READY、CODE_FACTS_AS_OF_20260618、READ_ONLY_NO_CODE_CHANGE。

## 0. 总裁决（一句话）

3 条断点**全部在代码层成立**，**没有一条被代码规避**。其中 REQ-30/32 更严重：`requirement-traceability-matrix` 与 bar dev-design 做了**与真实迁移不符的字段级"已覆盖"声明**。同时本轮对第一轮自身的 REQ-26b 判定做**诚实下调**（详见 §1）。

| 断点 | R1 文档结论 | R2 代码裁决 | 修正后等级 |
|---|---|---|---|
| REQ-26b 提成入财务台账 | R1-LOOP-01 P1（提成无 ar_ap_items 生成流程）| 计算台账已落+测试通过；ar_ap_items 贯通**确未实现** | **P2（按 D13"或"已合规，详见下）** |
| REQ-21 扫码/现金/POS 支付 | R1-LOOP-02/13 P1（无聚合支付入口、枚举无映射）| 聚合支付**已复用同表但旁路 FinanceService**；现金/POS 字段与事件**确不存在**；channel 无枚举无映射 | **P1** |
| REQ-30 存酒高价值/核身/手机号 | R1-LOOP-04/05 P1（字段断链、无复核状态）| 三子点**全部未实现**，且矩阵字段级声明与迁移**不符** | **P1（升级：含虚假覆盖声明）** |

## 1. REQ-26b 卡座提成入财务台账 —— 裁决：计算台账已落，财务贯通未实现（按 D13"或"判 P2）

**代码事实（file:line）**：
- 提成计算：`BarController.java:39-47` → `BarService.calculateCommission`（`BarService.java:348-359`，含 `executeWrite` 幂等）→ `BarBillingService.calculateCommission`（`BarBillingService.java:321-345`）。
- 费率正确：`BarBillingService.java:330-333`——`salesStaffId==null ? non_sales(1000bp/10%) : sales(1500bp/15%)`，base=`subtotal-discount`，与 D13 一致。测试 `BarServiceTests.java:287-311` 断言 15%/10% 通过。
- **唯一落库**：`repository.saveCommissionCalculation(...)`（`BarBillingService.java:334`）写入 `bar_commission_calculations` 表，**不调用任何 finance / createArApItem**。
- 全库 `grep "employee_commission"` **零命中**；`ar_ap_items` 唯一约束在 `V008:29-31`（非 V044），`source_type` 为自由 `VARCHAR(64)` 无枚举。
- bar 域确有 `FinanceBarReceivableLedgerService`（`:29-41`），但只服务挂账客单（`source_type="bar_bill"`/`direction="receivable"`，调用点 `BarBillingService.java:161`），与提成无关。

**诚实校正第一轮**：统一基线 **D13 原文是"提成在 P0 生成可复核台账**或**应付/内部薪酬依据"**。`bar_commission_calculations`（含费率/金额/归属/可复核）**已满足"可复核台账"分支**。因此 R1-LOOP-01 当初按"必须贯通 ar_ap_items"属**过严读法**，应下调为 **P2**：代码已满足 P0 D13 口径；ar_ap_items 贯通是 R1-SPEC-01 的**可选增强**（用于直接进薪酬应付），非 P0 阻断。
**仍需文档处理**：finance §6 与 traceability 矩阵措辞应明确"提成在 P0 以 `bar_commission_calculations` 为可复核台账承接；进入 finance 应付为 P1 增强"，避免后续开发误以为 P0 必须贯通。

## 2. REQ-21 扫码支付 / 现金 / POS —— 裁决：聚合支付部分对接，现金/POS 真实缺口（P1）

**代码事实（file:line）**：
- 聚合支付入口**真实存在且未被开关关闭**：`BarController.java:49-57`（权限复用 `finance.payment_intent.create`，入出参用 `finance.PaymentIntentRecord/Request`）→ `BarService.java:275-286` → `BarBillingService.java:72-98`。
- **复用"类型+物理表"，但旁路 FinanceService**：bar import `finance.PaymentIntentRecord`（`BarBillingService.java:8-9`），最终 `INSERT INTO finance_payment_intents`（`JdbcBarInventoryPaymentRepository.java:84-104`），与 finance 写同一张表；但 finance 自己的 `FinanceService.createIntent`（`FinanceService.java:321-335`）**未被 bar 调用**——bar 自建了一套 JDBC 直写。无独立 `bar_bill_payments` 表。
- **现金/POS 真实缺口**：全库搜 `cash_received`/`pos_received`/`cashier_staff_id`/`pos_transaction_no` **零命中**。文档（REQ-21 矩阵）声称"记录收款人、POS 流水号、事件 `payment.cash_received`/`payment.pos_received`"——**代码无字段、无事件**。注：finance 侧有线下支付路径（`OFFLINE_PAYMENT_METHODS={cash,bank_transfer,pos,other}`，`FinanceService.java:36`，走 `confirm-offline`），但与 bar bill 支付路径不关联。
- **channel 无枚举无映射**：`finance_payment_intents.channel VARCHAR(64) NOT NULL` 无 CHECK（`V008:60-70`）；bar/finance 两端均只 `requireText`，不校验取值、不转换。测试传入值是 `"aggregate_pay"`（`BarServiceTests.java:644`），既非文档英文枚举也非中文枚举——**两套枚举在代码里都不存在**。对账只能按 `business_order_type+business_order_id` 归并，**不能按 channel 归并**（R1-LOOP-13 成立）。
- **pay-intent 与 settle 解耦**：`settleBill`（`BarBillingService.java:100-127`）的 `paymentId` 是不透明字符串，不校验 pay-intent 是否存在/已支付；`BarBillSettleRequest.paymentChannel`（`:11`）字段**被服务完全忽略、不持久化**。bar_bills 列是 `payment_status`/`settlement_status`/`paid_amount_cent`（`V013:172-175`），无 `business_status`。
- 测试 `barBillPayIntentReusesFinancePaymentIntentSemantics`（`BarServiceTests.java:632-656`）只断言复用了 record 形状与初始状态 `created`，**未断言走 FinanceService、未断言 channel 校验/映射、未覆盖 cash/pos**——测试名偏乐观。

**结论**：聚合支付链路对接（写同表），推翻了 R1"似乎没对接"的部分担忧；但①现金/POS 收款人/流水号字段与事件确实缺失（中国法律要求现金/POS，且 REQ-21 矩阵声称已记录——**矩阵声明与代码不符**），②channel 无映射对账归并残缺。判 **P1**。

## 3. REQ-30/31/32 客人存酒 —— 裁决：三子点全部未实现 + 矩阵字段级虚假声明（P1，升级）

**真实表结构**（`V012__p0_inventory_saved_bottle_trace.sql:113-130`，唯一后续 ALTER `V046:51-54` 仅加 `merchant_id`）：
实际列 = `id, customer_id, bar_project_id, item_id(→inventory_items FK), bottle_name, initial_quantity, remaining_quantity, unit, expire_at, status, evidence_attachment_ids(JSONB), created_at, updated_at, merchant_id`。`SavedBottleRecord.java:7-23` 一一对应。

**矩阵/设计声明 vs 真实迁移（核心新发现）**：`requirement-traceability-matrix` REQ-30/31/32 声称 bar dev-design §6.11 saved_bottles **"已补 `bottle_label_value`、`customer_name_or_nickname`、`registration_photo_refs`、`customer_phone`"**，REQ-32 声称"已落 `is_high_value`/`high_value_threshold`"。**真实迁移中这些列全部不存在**——只有 `bottle_name`（非 bottle_label_value）、通用 `evidence_attachment_ids`（非 registration_photo_refs）、无手机号列、无姓名昵称列、无高价值列、无指向商品的 product_id（只有 item_id）。**dev-design ↔ migration 字段名/字段集不一致，矩阵继承了 dev-design 的滞后视图，对开发与验收构成误导。**

**三子点裁决**：
- **① 高价值作废双证据 → 真实缺陷**：无 `is_high_value`/阈值字段/阈值来源表；`'cancelled'` 状态在 CHECK 中声明（`V012:129`）但**代码无任何写入分支**——`cancelSavedBottle`/`voidSavedBottle` 方法不存在（InventoryService/BarService 全无）。整条作废链缺失，"高价值双证据+主管复核"无从触发。`SupervisorReviewService`（`:29-30`）只服务 `manual_release`/`bill_waiver`，未挂存酒。
- **② 取酒核身复核状态 → 真实缺陷**：状态机 CHECK 仅 `stored/claimed/cancelled/expired`（`V012:129`），无 `pending_review`/`disputed_frozen`。`claimSavedBottle`（`InventoryService.java:212-279`）只 `requireText(verificationCode)` 校验**非空**（`:226-227`），**从不与姓名/标签值比对**，无"不一致→人工复核"分支。测试 `InventoryServiceTests.java:406-442` 仅断言 verificationCode 为空抛错——印证"核身"=存在性校验。
- **③ 手机号可查 → 真实缺陷（当前无冲突，因双方均未实现）**：saved_bottles 无 `customer_phone` 列；取酒请求 `claimByPhone`（`SavedBottleClaimRequest.java:11`）被原样写入 `saved_bottle_claims.claim_staff_id`（`InventoryService.java:270`），未脱敏/加密/哈希；按手机号查询取酒的方法**不存在**（`InventoryRepository.java:45-80` 仅 by-id/list）。

**结论**：REQ-30/31/32 的高价值作废、核身复核、手机号检索三项**均未落地**，且矩阵存在字段级虚假覆盖声明。判 **P1**，并需立即修正矩阵/设计的失真声明（见 §5）。

## 4. 对第一轮 9 份草案的影响

- **R1-SPEC-01（提成入 ar_ap_items）**：降级为**可选 P1 增强**（D13"或"已被 `bar_commission_calculations` 满足）。
- **R1-SPEC-02（酒吧聚合支付统一 + 枚举映射）**：聚合支付已写同表，剩余=channel 枚举约束+映射、现金/POS 字段与事件落地 → 维持。
- **R1-SPEC-04（存酒高价值/核身/手机号）**：全部成立，且需先补字段/状态/作废接口 → 维持并升级。

## 5. 建议的文档修正（本轮已执行的最小修正见下）

本轮在 `requirement-traceability-matrix-v0.1.md` 的 REQ-21 / REQ-26b / REQ-30 / REQ-32 行追加**第二轮代码审计修订注记**（附加、不删除原文），把"已覆盖"的失真声明改注为"文档声明字段，代码/迁移尚未落地，详见本报告"。`requirements-code-implementation-matrix-v0.1.csv` 的实现状态建议由 owner 据本报告复核（REQ-26b/30 不应标"已完整实现"）。

## 6. 第三轮建议
- 若进入修复：按 R1-SPEC-02/04 先补迁移（saved_bottles 高价值/姓名/标签/手机号列 + 状态机扩展 + 作废接口）、现金/POS 字段与事件、channel 枚举映射；提成 finance 贯通作为 P1 增强排期。
- 横向扩样：本轮 3 例均出现"dev-design/矩阵声称已补字段、迁移未落"模式，建议第三轮对 REQ-22/23/40/46 等"已覆盖"行做同样的迁移级抽查。
