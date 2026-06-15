# P1 酒吧账单调整 API 契约 V0.1

## 1. 文档定位

本文档是 P1-BAR-02「酒吧账单调整」的接口契约补充，用于覆盖服务费、低消差额补齐、挂账登记三个最小可编码能力。本文档不替代 P0 API Contract Pack；P0 仍保持点酒、出品、支付、核销、留痕的最小营业闭环。

运行时 OpenAPI 由 Spring Boot 4.x + springdoc 从控制器自动生成；本文档作为产品、开发、测试、运营和后续第三方 POS/财务系统对齐的人读契约。

## 2. 业务边界

| 能力 | P1 本次包含 | 不包含 |
|---|---|---|
| 服务费 | 对未结清酒吧账单追加服务费，并记录调整流水 | 不做复杂服务费规则引擎 |
| 低消差额补齐 | 对未结清酒吧账单追加低消差额，并记录调整流水 | 不允许用通票、卡座入场权益、赠酒权益抵扣低消 |
| 挂账登记 | 将未结清酒吧账单标记为 `payment_status=on_account`、`settlement_status=receivable`，并生成一条财务 AR/AP 应收台账 | 不做催收、坏账、账龄报表和第三方财务系统自动同步 |
| 留痕 | 每次调整写入 `bar_bill_adjustments`，并记录审计事件 | 不接入第三方 POS 高级拆单、合单、转台能力 |
| 账单拆合 | 不包含 | 拆单、合单、转台、配方成本、员工提成结算留在后续 P1 酒吧 SaaS 增强 |

关键规则：票务、卡座入场名额、通票赠酒和酒吧最低消费是不同业务对象。低消补差只根据酒吧账单应付金额调整，不读取、不抵扣、不消耗票务或卡座入场权益。

## 3. 接口清单

| 方法 | 路径 | 权限点 | 幂等 | 说明 |
|---|---|---|---|---|
| POST | `/api/v1/bar/bills/{bill_id}/adjustments` | `bar.bill.adjust` | 是 | 对酒吧账单追加服务费、低消补差或挂账登记 |

所有写接口必须提供请求体内 `idempotencyKey`。接口层会拒绝空幂等键。

## 4. 请求字段

`BarBillAdjustmentRequest`：

| 字段 | 必填 | 说明 |
|---|---:|---|
| `idempotencyKey` | 是 | 幂等键，同一账单同一幂等键只允许形成一次调整结果 |
| `adjustmentType` | 是 | 支持 `service_fee`、`min_spend_top_up`、`on_account` |
| `amountCent` | 是 | 金额，单位分；服务费和低消补差必须大于 0，挂账必须为 0 |
| `reason` | 是 | 调整原因，用于运营复盘和审计 |
| `requestedByUserId` | 是 | 操作人或发起人用户 ID |
| `operatorNote` | 否 | 操作备注，当前切片保留字段 |
| `evidenceAttachmentIds` | 否 | 凭证、截图、审批附件 ID 列表 |

## 5. 响应字段

`BarBillAdjustmentRecord`：

| 字段 | 说明 |
|---|---|
| `id` | 调整记录 ID |
| `billId` | 被调整账单 ID |
| `adjustmentType` | 调整类型 |
| `amountCent` | 调整金额 |
| `beforePayableAmountCent` | 调整前账单应付金额 |
| `afterPayableAmountCent` | 调整后账单应付金额 |
| `reason` | 调整原因 |
| `requestedByUserId` | 发起人 |
| `approvalRequired` | 是否需要审批，当前默认 `true` |
| `status` | 当前默认 `applied` |
| `idempotencyKey` | 幂等键 |
| `evidenceAttachmentIds` | 证据附件 |
| `arApItemId` | 挂账登记生成的财务应收台账 ID；服务费、低消补差为空 |
| `snapshotJson` | 调整前后账单状态快照 |
| `createdAt` | 创建时间 |

## 6. 状态变化

| 调整类型 | 金额规则 | 账单应付变化 | 支付状态变化 | 结算状态变化 |
|---|---|---:|---|---|
| `service_fee` | `amountCent > 0` | `payable += amountCent` | 不变 | 不变 |
| `min_spend_top_up` | `amountCent > 0` | `payable += amountCent` | 不变 | 不变 |
| `on_account` | `amountCent = 0` | 不变 | `on_account` | `receivable`，并创建 `direction=receivable`、`source_type=bar_bill` 的 AR/AP 台账 |

已 `settled` 的账单不允许调整。部分支付、未支付、挂账前状态允许按上述规则调整。账单已经处于 `payment_status=on_account` 或 `settlement_status=receivable` 后，不允许使用新的幂等键再次挂账，避免重复生成应收台账；同一账单同一幂等键重复提交仍返回同一结果。

## 7. 错误码

| 错误码 | 场景 |
|---|---|
| `VALIDATION_FAILED` | 请求体为空、幂等键缺失、必填字段缺失、金额不合法、调整类型不支持 |
| `BAR_BILL_NOT_FOUND` | 账单不存在 |
| `BAR_BILL_NOT_ADJUSTABLE` | 账单已结清，不能再调整 |
| `BAR_BILL_ALREADY_ON_ACCOUNT` | 账单已挂账或已进入应收状态，不能再次挂账 |

## 8. 权限、幂等与审计

| 项目 | 设计 |
|---|---|
| 权限点 | `bar.bill.adjust` |
| 风险等级 | high |
| 审批标记 | 权限种子 `requires_approval=true`；调整记录 `approvalRequired=true` |
| 成功审计事件 | `bar.bill.adjusted` |
| 失败审计事件 | `bar.bill.adjust_failed` |
| 业务类型 | `bar_bill_adjustment` |
| 幂等约束 | 服务层幂等执行；数据库层用 `(bill_id, idempotency_key)` 唯一索引兜底 |

## 9. 数据库对象

Flyway 迁移：`V020__p1_bar_bill_adjustments.sql`、`V021__p1_bar_on_account_receivable_link.sql`

新增表：`bar_bill_adjustments`

新增字段：`bar_bill_adjustments.ar_ap_item_id`，用于保存挂账登记生成的财务应收台账 ID，历史流水允许为空。

关键约束：

| 约束 | 说明 |
|---|---|
| `chk_bar_bill_adjustment_type` | 调整类型只允许 `service_fee`、`min_spend_top_up`、`on_account` |
| `chk_bar_bill_adjustment_amounts` | 金额和调整前后应付金额不可为负 |
| `uq_bar_bill_adjustments_idempotency` | 同一账单同一幂等键唯一 |
| `idx_bar_bill_adjustments_bill` | 支持按账单查询调整流水 |
| `idx_bar_bill_adjustments_ar_ap_item` | 支持通过应收台账反查挂账流水 |

## 10. 验收标准

自动化验收以 160 机器为准：

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarServiceTests,BarControllerTests,BarJdbcIntegrationTests,FinanceServiceTests,FinanceJdbcIntegrationTests test
```

当前验收覆盖：

| 测试 | 覆盖点 |
|---|---|
| `BarServiceTests` | 服务费追加、低消补差、挂账登记、应收台账桥接、重复挂账拒绝、非法类型拒绝、审计事件 |
| `BarControllerTests` | 路由、权限点、幂等键校验 |
| `BarJdbcIntegrationTests` | Flyway 021、表结构、权限种子、`ar_ap_item_id` JDBC 保存和查询 |
| `FinanceServiceTests` | `source_type=bar_bill` 的应收台账创建 |
| `FinanceJdbcIntegrationTests` | 财务 AR/AP 表结构和台账持久化 |

完整回归仍需执行：

```bash
cd /home/james/complex-management
npm run verify:p0-contract

cd /home/james/complex-management/backend
mvn test
```
