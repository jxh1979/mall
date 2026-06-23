# 应收应付台账开发设计 V0.1

> 版本：V0.1 ｜ 最后更新：2026-06-20 ｜ 覆盖迁移基线：见各小节迁移号（最新 V124）｜ 状态：现行
> 新功能承接见 docs/v099-v124-feature-implementation-and-acceptance-spec-v0.1.md
> <!-- 文档goalP3(2026-06-20)：补版本/日期/覆盖迁移头，未改正文 -->

> **✅ R7 复审上修标注（2026-06-19，只读核验真实迁移/代码后回填）**
> R5 横幅原标注的"日结锁定补录审批零落地"已在本会话 P0 实现并落库（V069），以下上修；盘点/报损对账、聚合口径财务复核保持开放。
> - **日结锁定 + 补录审批 + 六源汇总已落地（V069）**：`ar_ap_items` 补 `locked_day_close_id`（指向 `finance_daily_closes`），新建 `ar_ap_adjustments`（`locked_day_close_id` + `approval_status` 三态 `pending/approved/rejected`）——锁定后直接改影响金额被硬拦截，须走补录审批通过后方可补录并留痕；新建日结六源汇总表 `finance_daily_close_sources`（`source_stream` 六源 CHECK + `uq_fdcs_close_source` 唯一键），日结金额来自真实多源流水汇总，不再是空壳。退款联动 `ar_ap` + `revenue.net`（V079/V078 口径）。
> - **聚合口径**：T7 六源与 `revenue.net`（= 自营售票实付 + 酒吧实收 − 退款，bar 仅取 `bar_bill_settlements` 现场消费实收）口径已定稿、日切可配；通票收入业态拆分另设展示口径 `revenue.ticket_bar_share/ticket_amusement_share`(V078)，不并入 net。
> - **仍开放**：盘点/报损对账（无 stocktake/variance 表或代码，§"库存差异→独立批次→应收应付"链条未落地）；聚合口径最终值仍待财务复核确认（草案见 `r5-aggregation-metrics-draft-20260618.md`）。
> - **验收口径（已正式）**：锁定日结后直接改影响金额被拒，走上级审批通过后方可补录并留痕；日结金额来自真实多源流水汇总。

文档类型：Implementation  
适用范围：P0 财务经营子域。  
上游口径：`finance-settlement-product-prd-v0.1.md`、`p0-implementation-task-list.md`、`pm-critic-and-acceptance-criteria-20260610.md`。  
目标：把“P0 完整应收应付台账”从产品口径落成可编码、可验收、可审计的数据模型和流程。

## 1. 定位

P0 不替代正式财务软件的总账、凭证、报税和复杂税票自动化。P0 必须把综合体经营事实整理成内部可追款、可付款、可复核、可追溯的台账。

应收应付台账覆盖：

| 类型 | 示例 | P0 处理 |
|---|---|---|
| 应收 | 租金、物业费、能源费、CAM、公摊、保证金、渠道应收、商户分成应收 | 生成台账、收款状态、账龄、附件、调整留痕 |
| 应付 | 供应商货款、服务商结算、渠道应付、商户分成应付、退款责任款项 | 生成台账、付款状态、账龄、附件、调整留痕 |
| 员工提成/内部薪酬依据 | 卡座销售提成、授权员工业务提成 | P0 生成可复核应付或内部薪酬台账；工资发放、个税和复杂绩效后置 |
| 差异 | 对账差异、库存差异影响金额、退款异常、第三方回流不一致 | 先进入差异复核，不得直接改锁定日结 |
| 内部核算 | 平台自营酒吧/游乐内部经营成本和收入归集 | 形成内部核算台账，不等同外部付款 |

## 2. P0 非范围

| 不做 | 原因 | 后续 |
|---|---|---|
| 商户账单门户、催缴和费用异议自助处理 | P0 先保证内部财务台账准确 | P1 |
| 总账凭证、税务申报、发票全自动开具 | 需要财务软件或税控系统边界 | P1/P2 或外部系统 |
| 复杂公式规则引擎 | 先支持模板化固定费、分成、服务费和手工调整 | P1 |
| 自动密码/密钥轮换系统 | P0 已定为人工台账 + 到期提醒 + Runbook | P1/P2 |

## 3. 核心对象

| 对象 | 说明 |
|---|---|
| `ar_ap_items` | 应收应付主台账，一条记录对应一笔待收/待付/内部核算事项 |
| `ar_ap_item_sources` | 来源明细，可关联合同、订单、核销、退款、采购、库存差异、人工调整 |
| `ar_ap_payments` | 收款/付款登记，可支持部分收付款 |
| `ar_ap_adjustments` | 调整、冲正、减免、坏账、补录申请 |
| `ar_ap_attachments` | 合同、发票、付款凭证、对账单、差异说明等附件 |
| `ar_ap_audit_logs` | 金额、状态、主体、附件、调整等审计记录 |

## 4. 表设计建议

### 4.1 `ar_ap_items`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | uuid | 是 | 主键 |
| `project_id` | uuid | 是 | 综合体项目 |
| `item_no` | varchar | 是 | 台账编号，唯一 |
| `direction` | enum | 是 | `receivable` 应收、`payable` 应付、`internal_accounting` 内部核算 |
| `business_entity_binding_id` | uuid | 是 | 四类主体绑定关系，见 `business-entity-binding-development-design-v0.1.md` |
| `responsible_subject_id` | uuid | 是 | 责任主体 |
| `settlement_subject_id` | uuid | 是 | 收付款主体 |
| `counterparty_subject_id` | uuid | 否 | 对方主体 |
| `amount_total` | bigint | 是 | 总金额，单位分 |
| `amount_paid` | bigint | 是 | 已收/已付金额，单位分 |
| `amount_unpaid` | bigint | 是 | 未收/未付金额，单位分 |
| `currency` | varchar | 是 | 默认 `CNY` |
| `due_date` | date | 是 | 到期日 |
| `aging_days` | int | 是 | 按未收/未付余额计算 |
| `status` | enum | 是 | 见 §5 |
| `source_type` | enum | 是 | `contract_fee`、`order`、`verification`、`refund`、`purchase`、`inventory_difference`、`employee_commission`、`manual_adjustment`、`third_party_reconciliation`、`pass_drink_settlement`（通票免费酒水平台↔酒吧结算，F-TXN-03 piece③b；权威集合见 `FinanceService.SOURCE_TYPES`） |
| `source_id` | uuid/varchar | 否 | 来源对象 |
| `contract_id` | uuid | 否 | 关联合同 |
| `space_id` | uuid | 否 | 关联铺位/点位/区域 |
| `merchant_id` | uuid | 否 | 关联商户 |
| `description` | text | 否 | 摘要 |
| `created_by` | uuid | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `locked_day_close_id` | uuid | 否 | 已纳入锁定日结时写入 |
| `deleted_at` | timestamp | 否 | 软删除 |

### 4.2 `ar_ap_payments`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | uuid | 是 | 主键 |
| `ar_ap_item_id` | uuid | 是 | 台账 |
| `payment_direction` | enum | 是 | `inbound`、`outbound` |
| `amount` | bigint | 是 | 金额，单位分 |
| `payment_channel` | enum | 是 | 聚合支付、现金、POS、银行转账、第三方渠道、其他 |
| `paid_at` | timestamp | 是 | 实际收付款时间 |
| `operator_id` | uuid | 是 | 登记人 |
| `attachment_id` | uuid | 否 | 凭证 |
| `note` | text | 否 | 说明 |

### 4.3 `ar_ap_adjustments`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | uuid | 是 | 主键 |
| `ar_ap_item_id` | uuid | 是 | 台账 |
| `adjustment_type` | enum | 是 | `increase`、`decrease`、`write_off`、`reversal`、`bad_debt`、`manual_correction` |
| `amount_delta` | bigint | 是 | 调整金额，可正可负 |
| `reason` | text | 是 | 调整原因 |
| `approval_status` | enum | 是 | `draft`、`pending_review`、`approved`、`rejected`、`cancelled` |
| `reviewer_id` | uuid | 否 | 复核人 |
| `reviewed_at` | timestamp | 否 | 复核时间 |
| `created_by` | uuid | 是 | 创建人 |

> **✅ 已实现修复（F-OPS-07，2026-06-22，已在 .160 编译通过）**：补录/调整审批 `decideAdjustment`（finance 域唯一涉资金审批链）落地**职责分离（SoD）**——审批人 `approver`（写入 `reviewer_id`）**不得等于**发起人 `requested_by`（`created_by`），**自审自批抛 `SELF_APPROVAL_FORBIDDEN`**。与 §7「发起调整 / 审批调整」分属不同权限、§8 反模式「日结锁定后直接改金额→走调整/冲正审批」一致：影响金额的调整必须经第二人复核，发起人不能给自己的调整单放行。

| 状态 | 含义 | 可进入条件 |
|---|---|---|
| `draft` | 草稿 | 人工创建未提交 |
| `pending_confirm` | 待确认 | 来源数据齐全，等待财务确认 |
| `pending_collection` | 待收款 | 应收已确认且未全额收款 |
| `pending_payment` | 待付款 | 应付已确认且未全额付款 |
| `partial_paid` | 部分收/付款 | 已登记部分收付款 |
| `settled` | 已结清 | 未收/未付余额为 0 |
| `in_dispute` | 有差异/争议 | 对账、合同、主体或金额异常 |
| `pending_adjustment_review` | 调整待复核 | 影响金额的调整已提交 |
| `written_off` | 已核销/坏账 | 经审批核销 |
| `cancelled` | 已取消 | 误建或来源撤销 |

硬规则：

1. `amount_unpaid = amount_total - amount_paid + approved_adjustments`，不得由前端手工输入。
2. 已锁定日结关联的台账不得直接改金额，只能走 `ar_ap_adjustments`。
3. 缺少 `business_entity_binding_id`、`settlement_subject_id`、`due_date`、`amount_total` 时不得进入确认态。
4. 同一 `source_type + source_id + direction` 默认不得重复生成，重复必须走调整或冲正。

> **✅ 已实现修复（F-OPS-01 / F-OPS-09，2026-06-22，已在 .160 编译通过）**：
> - **F-OPS-01 运行期权威台账 append-only（`delete_only` 口径）**：`ar_ap_items` 纳入 append-only 保护，但口径为 **`delete_only`——仅禁物理 `DELETE`，保留合法 `UPDATE`** 供 `amount_paid`/`amount_unpaid`/`status` 等收付款回写（与硬规则 1 的余额公式、§5 状态机流转一致）。**金额修正一律走冲正分录**（`ar_ap_adjustments`，§4.3 `reversal`/`manual_correction`），不得物理删除原行重建。即：台账行只增不删、状态/已收付金额可合法 UPDATE 回写、金额修正走调整冲正。
> - **F-OPS-09 日结判锁加 merchant scope 过滤**：日结判锁 `lockedDailyCloseExistsForDate` 加 **merchant scope 过滤**——`finance_daily_closes` 以 `scope_type`/`scope_id` 表达封账归属，**仅 `scope_type='merchant'` 才约束该商户**，与锁定落账 `lockArApItemsForDailyClose` 口径一致：仅商户级封账冻结该商户台账，**非 merchant scope（如平台/门店级）封账不跨冻**该商户。消除"任意 scope 的当日封账都冻结全部台账"的过冻。

## 6. 生成流程

### 6.1 合同固定费用

1. 合同生效并配置固定费用模板。
2. 定时任务按周期生成应收/应付。
3. 系统写入合同、空间、经营对象、结算主体和到期日快照。
4. 财务确认后进入待收款/待付款。

### 6.2 订单、核销和第三方回流

1. 票务、酒吧、游乐或第三方回流形成结算依据。
2. 系统检查合同规则、主体绑定、金额、来源对象是否完整。
3. 完整则生成台账；不完整进入差异复核。
4. 差异复核通过后再生成台账或调整单。

### 6.3 库存差异和退款异常

1. 库存差异先生成独立差异批次。
2. 财务日结只显示潜在影响金额，不自动改锁定日结。
3. 经审批后进入应收应付或财务调整台账。

### 6.4 内部核算入账（已实现修复）

> **✅ 已实现修复（F-OPS-02，2026-06-22，已在 .160 编译通过）**：内部核算入账 `recordInternalAccountingRevenue`（平台自营酒吧/游乐内部经营收入归集，对应 §1「内部核算」行、§4.1 `direction=internal_accounting`）**写台账前校验日结锁**——若目标经营日已封账（存在该经营日的锁定日结，判锁口径见 §5 硬规则 F-OPS-09 的 merchant scope 过滤），**禁止向已封账经营日补写内部核算收入**，与硬规则 2「已锁定日结关联的台账不得直接改金额」同源：已封账日的内部核算收入修正同样须走 `ar_ap_adjustments` 补录审批（§4.3），不得直接补写绕过封账。

## 6D. 库存差异→应收应付资金回写（stocktake/variance→AP）

> 修正(2026-06-19 R5业务HTML缺口审计 F5)：本文 R7 横幅已自认"盘点/报损对账无 stocktake/variance 表或代码，库存差异→独立批次→应收应付链条未落地"。代码核验：migration 中 `stocktake` **零命中**；`variance` 仅作为子类型枚举 `stock_variance`（V085 `inventory_high_value_reviews.subject_type` CHECK、V094 注释）和指标字段 `variance_rate`（V037），**无 `CREATE TABLE inventory_variance/stocktake`**；`source_type=inventory_difference` 虽列于 §4.1 枚举，但无生成代码。本节补"库存差异→AR/AP 资金回写"完整链条。

### 6D.1 业务问题

- 报损、盘点差异、高价值酒水异常当前**不进财务台账**：缺 stocktake（盘点单）/inventory_variance（差异明细）承载表，`source_type=inventory_difference` 无生成代码，库存差异影响金额无法回写应收应付。
- 结果：业务HTML #finance 要求的"核对采购入库、库存消耗、报损、盘点差异"在财务侧只能停留在差异提示，无法形成可追款/可付款/可入账的台账项。

### 6D.2 表设计建议（待落地）

| 表 | 用途 | 关键字段（建议） |
|---|---|---|
| `stocktake` 盘点单 | 一次盘点动作的主单 | `id`、`project_id`、`scope`（仓/区/点位）、`business_date`、`status`、`reviewer_id`、`created_by`、审计字段 |
| `inventory_variance` 差异明细 | 盘点/报损差异逐项 | `id`、`stocktake_id`、`item_id`、`batch_id`、`qty_diff`（数量差）、`amount_diff_cent`（金额差，分）、`reason`（报损/盘亏/盘盈/异常）、`review_status`（差异生成/复核/入账/驳回）、`high_value_flag`、审计字段 |

> 与既有结构衔接：高价值判定复用 `inventory_high_value_reviews`（V085，`subject_type IN ('damage_report','stock_variance','saved_bottle_void')`）与 V094 阈值字段，报损/盘点差异经域内复核后再桥接财务。

### 6D.3 桥接：库存差异→ar_ap_items

- 高价值报损 / 盘点差异经**主管复核**（`inventory_variance.review_status=入账`）后 →生成 `ar_ap_items`，`source_type=inventory_difference`，`source_id` 指向 `inventory_variance.id`。
- 方向：损失类（盘亏/报损）按应付/调整分录登记；盘盈或回收类按应收/冲减登记（具体科目方向见待确认项）。
- 来源明细写入 `ar_ap_item_sources`（关联库存差异），金额来自 `amount_diff_cent`，不得前端手填。

### 6D.4 状态流转

```
差异生成（inventory_variance 创建）
  → 复核（主管/高价值复核，inventory_high_value_reviews）
    → 入账（生成 ar_ap_items，source_type=inventory_difference）
    或 驳回（不入账，留痕原因）
```

- 入账前仅在财务日结显示潜在影响金额，不自动改锁定日结（承接 §6.3 现状口径）。

### 6D.5 事件

- `inventory.variance.posted_to_ap`：库存差异复核通过并生成应收应付/调整分录后发出，供日结、对账、台账下游订阅。

### 6D.6 与供应链高价值复核 / V094 阈值衔接

- 报损 / 盘点差异是否走高价值复核由 V094 阈值字段判定；超阈值进入 `inventory_high_value_reviews`（V085）域内复核。
- 仅域内复核通过的高价值差异方可桥接财务入账，避免低于阈值的小额差异淹没台账。

### 6D.7 验收

- 高价值报损审批通过后，可在 AR/AP 台账查到对应**应付/调整分录**（`source_type=inventory_difference`，金额=差异金额）。
- 存酒"已售未取"余额作为**预收/负债口径**登记（承接 F 审计"存酒余额→AR/AP 项"），标 **P1**：将 `saved_bottle` 已售未取余额作为平台对客负债，按预收/待履约口径登记入 AR/AP，履约（取酒）或核销后冲减。

> 待业务确认：库存差异入账的会计科目方向——应付（对供应商/责任方的赔付义务）vs 损益调整（直接计入经营损益、不形成对外应付）。

---

## 7. 权限与审计

| 动作 | 权限 | 审计要求 |
|---|---|---|
| 创建台账 | 财务经营编辑 | 记录创建人、来源、主体、金额 |
| 确认台账 | 财务确认 | 记录确认人、时间、确认意见 |
| 登记收付款 | 财务收付款 | 记录渠道、凭证、金额、操作人 |
| 发起调整 | 财务调整申请 | 记录原因、差额、附件 |
| 审批调整 | 财务主管/管理层 | 记录审批结果和意见 |
| 导出 | 财务导出 | 二次确认并写导出审计 |

## 8. 反模式

| 不要这样做 | 应该这样做 | 原因 |
|---|---|---|
| 只做结算单，不做应收应付余额 | 建 `ar_ap_items` 并记录余额、账龄、状态 | P0 已明确要完整台账 |
| 用商户名文本做收付款主体 | 引用主体绑定和结算主体 ID | 名称会变，结算责任不能漂移 |
| 日结锁定后直接改金额 | 走调整/冲正审批 | 防止财务口径被悄悄改写 |
| 第三方字段不完整也自动入账 | 进入差异复核 | 避免错账 |
| 附件上传失败仍标记完成 | 保持待补充或保存失败 | 凭证缺失会影响审计 |

## 9. 测试用例规格

| 测试 ID | 场景 | 输入 | 期望 |
|---|---|---|---|
| FIN-ARAP-UT-001 | 固定租金生成应收 | 生效合同 + 月租金模板 | 生成一条应收，绑定合同/空间/结算主体 |
| FIN-ARAP-UT-002 | 部分收款 | 应收 10000 分，收款 4000 分 | 状态为 `partial_paid`，未收 6000 分 |
| FIN-ARAP-UT-003 | 重复来源防重 | 同一核销记录重复生成 | 阻止重复或提示走调整 |
| FIN-ARAP-UT-004 | 锁定日结调整 | 已锁定台账尝试改金额 | 拒绝直接修改，要求调整审批 |
| FIN-ARAP-UT-005 | 主体缺失 | 无结算主体保存 | 拒绝保存并提示字段 |
| FIN-ARAP-IT-001 | 合同费用端到端 | 合同生效到收款登记 | 台账、收款、审计完整 |
| FIN-ARAP-IT-002 | 第三方差异 | 回流金额不一致 | 进入差异复核，不自动入账 |
| FIN-ARAP-IT-003 | 库存差异入账 | 高价值报损审批通过 | 生成调整或应收应付记录 |

## 10. 错误处理矩阵

| 错误 | 检测 | 响应 | 留痕 |
|---|---|---|---|
| 主体绑定缺失 | 保存/生成前校验 | 阻断并提示补主体绑定 | 操作审计 |
| 金额为负或单位错误 | 后端校验金额分 | 阻断 | 错误日志 |
| 来源重复 | 唯一键/幂等键 | 返回重复提示 | 审计 |
| 附件上传失败 | 对象存储错误 | 提示重试，不进入完成态 | 错误日志 |
| 日结已锁定 | `locked_day_close_id` 不为空 | 要求调整审批 | 调整申请 |
| 第三方字段不完整 | 字段完整性校验 | 进入差异复核 | 差异记录 |

## 11. 验收标准

采用 `pm-critic-and-acceptance-criteria-20260610.md §3.1` 的 Given/When/Then 作为 P0 验收门槛。补充两条：

1. Given 财务人员导出台账，When 导出字段包含主体、金额、账龄和状态，Then 系统必须记录导出人、时间、筛选条件和文件摘要。
2. Given 台账进入争议状态，When 争议关闭，Then 系统必须记录关闭结论、责任方、处理金额和附件。

## 14. 综合体计费 AR/AP 开发设计（P1）

> 新增说明（R6 全闭环(2026-06-19)，市场标杆审计后回填，只读核验真实迁移/代码后追加，未改代码）：相对成熟商场/综合体系统，"营业额租金/扣点（turnover rent）、保证金专项台账、CAM 公摊分摊"在全库迁移/字段层**零命中**（`turnover_rent`/`deposit_ledger`/`cam_allocations`/`true-up` 在 migration 与 docs 表设计层均无 `CREATE TABLE`，仅 `business-module-mature-system-gap-analysis.md` 以市场标杆口径提及）；§4.1 `source_type` 枚举亦无 `turnover_rent`/`cam` 取值。存酒"已售未取"负债当前仅 §6D.7 标 P1，未入 AR/AP 实链。本节承接产品口径 `finance-settlement-product-prd-v0.1.md §13`，补三类综合体计费的 AR/AP 落库与计费引擎开发设计。全部为 P1（CAM true-up 为 P2）。科目方向（entry 方向）引用决议 `business-decision-log` R6-D08（科目方向）。

### 14.1 表设计建议（待落地）

#### 14.1.1 `turnover_rent_rules` 营业额租金规则

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | uuid | 是 | 主键 |
| `project_id` | uuid | 是 | 综合体项目 |
| `contract_id` | uuid | 是 | 关联合同 |
| `merchant_id` | uuid | 是 | 关联商户 |
| `base_rent_cent` | bigint | 是 | 固定租金，单位分 |
| `turnover_rate_bp` | int | 是 | 扣率（基点 bp，万分之一，避免浮点） |
| `floor_rent_cent` | bigint | 否 | 保底租金，单位分（空=无保底） |
| `cap_rent_cent` | bigint | 否 | 封顶抽成租金，单位分（空=无封顶） |
| `calc_mode` | enum | 是 | `base_plus_turnover` 累加 / `greater_of` 取大 / `turnover_only` 纯抽成，默认 `base_plus_turnover`（R6-D08 配套，默认按合同摘要） |
| `settlement_cycle` | enum | 是 | `monthly`/`quarterly`，默认 `monthly` |
| `enabled` | boolean | 是 | 营业额回流开关，默认 `false`（第三方未接入前置灰，依决议日志 §2） |
| 审计字段 | — | 是 | created_by/created_at/updated_by/updated_at/deleted_at |

#### 14.1.2 `deposit_ledger` 保证金专项台账

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | uuid | 是 | 主键 |
| `project_id` | uuid | 是 | 综合体项目 |
| `contract_id` | uuid | 是 | 关联合同 |
| `merchant_id` | uuid | 是 | 关联商户 |
| `action_type` | enum | 是 | `charge` 收取 / `refund` 退还 / `offset` 抵扣 / `forfeit` 没收 |
| `amount_cent` | bigint | 是 | 动作金额，单位分（恒正，方向由 action_type 决定） |
| `balance_after_cent` | bigint | 是 | 动作后保证金余额，单位分（系统算，不前端填） |
| `linked_ar_ap_item_id` | uuid | 否 | offset 时指向被抵扣的租金应收 `ar_ap_items.id` |
| `reason` | text | 否 | 退还/没收/抵扣原因 |
| `approval_status` | enum | 是 | `pending`/`approved`/`rejected`（没收/抵扣须审批，承接 §13.2 默认值） |
| 审计字段 | — | 是 | created_by/created_at/updated_by/updated_at/deleted_at |

> 余额硬规则：`balance_after = Σcharge − Σrefund − Σoffset − Σforfeit`，由系统算；退还/抵扣不得使余额为负。

#### 14.1.3 `cam_allocations` CAM 公摊分摊

> 修订说明（2026-06-22，F-OPS-08）：CAM 权重分摊已**整体废止**，改为物业费每平米固定计费（见 §14.4）。本表 `cam_allocations` 与 `cam_trueup_*`（V110/V121）仅作**历史只读**保留，应用层不再新写。现行物业费用新表 `property_fee_rates`/`property_fee_bills`（V150）。

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | uuid | 是 | 主键 |
| `project_id` | uuid | 是 | 综合体项目 |
| `cost_pool_period` | varchar | 是 | 费用池周期标识（如 `2026-06`） |
| `cost_pool_amount_cent` | bigint | 是 | 本周期物业/能源/公共费用池总额，单位分 |
| `merchant_id` | uuid | 是 | 被分摊商户 |
| `contract_id` | uuid | 否 | 关联合同 |
| `alloc_basis` | enum | 是 | `lease_area` 建筑面积 / `agreed_ratio` 约定比例，默认 `lease_area` |
| `alloc_weight_bp` | int | 是 | 该商户分摊权重（基点 bp） |
| `allocated_amount_cent` | bigint | 是 | 分摊额=费用池×权重（系统算，尾差挂最大分摊方） |
| `is_prepaid` | boolean | 是 | 是否周期预收（P1=true） |
| `trueup_year` | int | 否 | 年度 true-up 归属年（P2 使用） |
| `trueup_diff_cent` | bigint | 否 | true-up 差额（多退为负/少补为正，P2） |
| 审计字段 | — | 是 | created_by/created_at/updated_by/updated_at/deleted_at |

> §4.1 `ar_ap_items.source_type` 枚举须扩 `turnover_rent`、`cam`、`deposit`（落地时由 DB 任务细化 CHECK 约束）。

### 14.2 营业额抽成计费引擎

- 输入：第三方回流营业额（按 `merchant_id` + 结算周期聚合的净营业额，口径默认不含税、扣退款，对齐 `revenue.net`）+ `turnover_rent_rules`。
- 计费：`抽成租金 = clamp(营业额×turnover_rate_bp/10000, floor_rent, cap_rent)`；最终应收按 `calc_mode` 取 `base+turnover` / `max(base, turnover)` / `turnover_only`。
- 输出：生成 `ar_ap_items`，`source_type=turnover_rent`，`source_id` 指向回流营业额聚合批次；来源明细写 `ar_ap_item_sources`，金额不前端手填。
- 门控：`turnover_rent_rules.enabled=false`（开关 off / 接口未接入）时引擎不产出应收，仅试算草稿不入账。
- 周期内回流增量重算走 `ar_ap_adjustments`（增减抽成），不直接改已确认/锁定台账。

### 14.3 保证金专项台账（收/退/抵扣/没收 → ar_ap 联动）

- `charge`（收取）：登记 `deposit_ledger`，余额↑；不直接进损益，作平台对客负债/暂收口径（科目方向引用 R6-D08）。
- `refund`（退还）：余额↓至 0；退还 ≤ 当前余额，存在欠费/未结抵扣时阻断或提示先抵扣。
- `offset`（抵扣租金欠费）：审批通过后，`deposit_ledger` 余额↓，同时联动被抵扣租金 `ar_ap_items` 的 `amount_paid`↑/`amount_unpaid`↓（经 `linked_ar_ap_item_id` 关联），二者金额一致可对账。
- `forfeit`（没收）：审批通过后余额↓，没收额按 R6-D08 科目方向转平台收入/应收冲销，生成对应 `ar_ap_items` 或调整分录并留痕。

### 14.4 物业费固定计费 → 应收（V150 替代 CAM 分摊 / 年度 true-up，F-OPS-08）

> 修订说明（2026-06-22，F-OPS-08）：用户 2026-06 裁决——CAM「按权重分摊 + 年度 true-up 多退少补」**整体废止**，改为物业费「每平米固定单价 × 商户面积」按期计费、**不退不补**。下为现行口径；旧 CAM 口径（§14.1.3 表）仅作历史保留，`cam_allocations`/`cam_trueup_*` 只读不再新写，应用层 `allocateCam`/`runCamTrueUp` 改抛 `CAM_ALLOCATION_ABOLISHED`/`CAM_TRUEUP_ABOLISHED`。

- **费率（`property_fee_rates`，项目级可按期）**：`rate_per_square_meter_cent`（分/㎡/期）；`effective_period` 精确期命中优先，NULL=该项目默认费率（未命中回退）。维护端点 `POST /finance/property-fee/rates`、列出 `GET /finance/property-fee/rates?project_id=`。
- **计费（`property_fee_bills` + 应收）**：端点 `POST /finance/property-fee/bills`，对运营提供的 `merchant_ids` 逐户 `amount_cent = round(单价 × merchants.area_square_meter)` 生成 `ar_ap_items`，`source_type=property_fee`、`direction=receivable`、`counterparty_id=merchant_id`；`UNIQUE(project_id,period,merchant_id)` 幂等，**一经计费即固定、不退不补**（费率事后调整不影响已计费行）。面积缺失/非正的商户跳过。
- **取数**：`merchants` 无 `project_id`，商户名单由运营显式提供（类比旧 CAM shares）；费率按请求 `project_id` 解析。无生效费率整批拒绝 `PROPERTY_FEE_RATE_NOT_FOUND`。
- **不退不补的含义**：取消「实际 vs 预收」差额对账（true-up），三层口径矛盾随之消解；后续费率变化只影响**未来期**计费，不回溯已计费期。

### 14.5 存酒"已售未取"→ 预收负债登记口径收口（承接 §6D.7 / F5）

> 承接 R7 横幅、§6D.7 P1 与 R5 业务HTML缺口审计 F5："存酒余额→AR/AP 项"。本节明确 entry 方向并收口登记口径，科目方向引用决议 `business-decision-log` R6-D08（科目方向）。

- 登记口径：`saved_bottle` 已售未取余额作为**平台对客负债（预收/待履约）**登记入 `ar_ap_items`，`source_type` 取 `deposit`（或落地时新增 `saved_bottle_liability` 专用枚举，DB 任务细化），`direction` 取负债侧。
- **entry 方向（引用 R6-D08）**：售出（已售未取）时按"预收负债增加"登记（贷负债方向）；履约（取酒）或核销/作废（`saved_bottle_void`，对应 V085 `subject_type`）后按"负债减少/冲减"登记（借负债方向），余额随取酒进度冲减至 0。
- 金额来自存酒已售未取余额对应金额，不前端手填；与酒吧账单 / 库存口径可对账。

### 14.6 事件、状态流转、验收

**事件**

- `finance.turnover_rent.posted`：营业额抽成应收生成后发出（仅开关 on）。
- `finance.deposit.action_posted`：保证金收/退/抵扣/没收动作完成后发出（含 action_type）。
- ~~`finance.cam.allocation_posted` / `finance.cam.trueup_posted`~~（**已废止**，V150 起不再发出）；改发 `finance_property_fee.billed`：物业费按期计费完成后发出。
- `finance.saved_bottle_liability.posted` / `.released`：存酒负债登记 / 取酒核销冲减后发出。

**状态流转**

```
营业额回流(开关on) → 抽成试算(草稿) → 财务确认(应收, source_type=turnover_rent) → 周期锁定
保证金: charge → (offset 联动租金应收 / forfeit 转收入, 均审批) → refund(余额→0)
物业费(V150): 项目费率(property_fee_rates) → 按期计费 round(单价×面积) → 应收(source_type=property_fee, 不退不补)  [旧 CAM 分摊/true-up 已废止]
存酒负债: 售出→预收负债登记 → 取酒/核销→负债冲减(R6-D08 方向)
```

**验收**

- 高价值报损审批后入应付可查（承接 §6D.7）：`source_type=inventory_difference` 应付/调整分录可在台账查到，金额=差异金额。
- 营业额抽成可对账：开关 on 时按规则生成 `source_type=turnover_rent` 应收，可关联合同/商户/周期，与回流营业额×扣率封顶保底口径一致；开关 off 不入账。
- 保证金四动作余额自洽：余额=Σcharge−Σrefund−Σoffset−Σforfeit，offset 与被抵扣租金应收金额一致可对账。
- 物业费可对账（V150）：`source_type=property_fee` 应收金额 = round(项目费率 × 商户面积)，按 (项目,期,商户) 幂等不重复挂账；~~原 CAM 分摊额之和=费用池总额 / P2 true-up 多退少补~~ 已废止。
- 存酒已售未取负债登记后可查，取酒/核销后冲减至 0，entry 方向符合 R6-D08。

### 14.7 待业务确认项（已写明 R6 建议默认值）

| 待业务确认项 | R6 建议默认值 |
|---|---|
| 营业额扣率 / 保底 / 封顶取值 | 默认按合同摘要约定扣率；未约定则不启用抽成（只收固定租金） |
| 抽成模式 `calc_mode` | 默认 `base_plus_turnover`（固定+抽成累加） |
| 营业额口径 | 默认不含税净营业额、扣退款，对齐 `revenue.net` |
| 保证金抵扣 / 没收触发 | 默认人工发起 + 财务/上级审批，不自动扣减 |
| ~~CAM 分摊权重~~ | **已废止（V150，F-OPS-08）**：CAM 权重分摊取消，改物业费每平米固定单价×面积、不退不补 |
| ~~CAM true-up 周期~~ | **已废止（V150，F-OPS-08）**：年度 true-up 多退少补取消（物业费不退不补、无对账差额） |
| 库存差异 / 存酒负债 entry 科目方向 | 引用 `business-decision-log` R6-D08（科目方向）；库存差异默认损益调整 vs 应付按 R6-D08 定，存酒按预收负债方向登记 |

---

## 12. 引用

| 主题 | 文档 |
|---|---|
| 财务产品口径 | `finance-settlement-product-prd-v0.1.md §10-§11` |
| 综合体计费产品口径 | `finance-settlement-product-prd-v0.1.md §13`（R6 全闭环(2026-06-19)） |
| 科目方向决议 | `business-decision-log` R6-D08（科目方向） |
| P0 任务清单 | `p0-implementation-task-list.md §18` |
| 主体绑定 | `business-entity-binding-development-design-v0.1.md` |
| PM critic 验收 | `pm-critic-and-acceptance-criteria-20260610.md §3.1` |
| 运维备份与密钥 | `server-160-runbook-v0.2.md` |
