# 应收应付台账开发设计 V0.1

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
| `source_type` | enum | 是 | `contract_fee`、`order`、`verification`、`refund`、`purchase`、`inventory_difference`、`employee_commission`、`manual_adjustment`、`third_party_reconciliation` |
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

## 5. 状态机

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

## 12. 引用

| 主题 | 文档 |
|---|---|
| 财务产品口径 | `finance-settlement-product-prd-v0.1.md §10-§11` |
| P0 任务清单 | `p0-implementation-task-list.md §18` |
| 主体绑定 | `business-entity-binding-development-design-v0.1.md` |
| PM critic 验收 | `pm-critic-and-acceptance-criteria-20260610.md §3.1` |
| 运维备份与密钥 | `server-160-runbook-v0.2.md` |
