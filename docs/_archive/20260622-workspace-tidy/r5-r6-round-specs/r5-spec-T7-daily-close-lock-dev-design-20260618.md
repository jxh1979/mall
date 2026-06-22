# T7 日结锁定 + 补录审批 + 多源汇总 可编码开发规格 V0.1（2026-06-18）

> **任务**：R5 backlog **T7**，W1 财务域。
> **裁决依据**：裁决⑤——锁定后硬拦截改影响金额 + 补录需上级审批闭环 + 日结多源汇总，本期 P0。聚合口径见 `r5-aggregation-metrics-draft-20260618.md`（待财务复核）。
> **现状基准**（已核）：`finance_daily_closes`(id/business_date/scope/force_close/status/locked_at，**无汇总金额字段**)，`lockDailyClose` 仅插一条 locked 记录、不汇总；`finance_ar_ap_items`(UUID PK，**无 locked_day_close_id**)，`registerArApPayment/createArApItem` 不查锁定状态——**锁定后仍可自由改金额**；`finance_reconciliation_differences.difference_type` 为无 CHECK 自由文本。
> **⚠️ 实现前置（owner 收口）**：迁移并存 `ar_ap_items` 与 `finance_ar_ap_items`、`finance_reconciliation_*` 与 `integration_reconciliation_*` 两套。本规格默认 **`finance_ar_ap_items` 为财务权威**、`integration_*` 为第三方来源；落地前须 owner 确认收口。
> **迁移**：取下一连续版本（示意 **V070**，实际以合并顺序为准，与 T4/T6 错开）。

---

## 1. 数据模型（迁移草案）

```sql
-- 1) 日结多源汇总（六源）
CREATE TABLE IF NOT EXISTS finance_daily_close_summaries (
    id VARCHAR(64) PRIMARY KEY,
    daily_close_id VARCHAR(64) NOT NULL REFERENCES finance_daily_closes(id) ON DELETE CASCADE,
    source_stream VARCHAR(40) NOT NULL,               -- 六源
    amount_cent BIGINT NOT NULL DEFAULT 0,
    refund_cent BIGINT NOT NULL DEFAULT 0,
    order_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_fdcs_source CHECK (source_stream IN
      ('official_ticket','channel_ticket','bar','amusement','supply_cost','thirdparty_reflow')),
    CONSTRAINT uq_fdcs_close_source UNIQUE (daily_close_id, source_stream)
);

-- 2) 锁定后补录审批
ALTER TABLE finance_ar_ap_items ADD COLUMN IF NOT EXISTS locked_day_close_id VARCHAR(64) REFERENCES finance_daily_closes(id);

CREATE TABLE IF NOT EXISTS ar_ap_adjustments (
    id VARCHAR(64) PRIMARY KEY,
    ar_ap_item_id UUID NOT NULL REFERENCES finance_ar_ap_items(id),
    locked_day_close_id VARCHAR(64) NOT NULL REFERENCES finance_daily_closes(id),
    adjustment_amount_cent BIGINT NOT NULL,           -- 补录变动额（带符号）
    approval_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    requested_by VARCHAR(64) NOT NULL,
    approved_by VARCHAR(64),
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at TIMESTAMPTZ,
    CONSTRAINT chk_ar_ap_adj_approval CHECK (approval_status IN ('pending','approved','rejected'))
);

-- 3) 对账差异类型收口（四类 CHECK，替换自由文本）
ALTER TABLE finance_reconciliation_differences
  ADD CONSTRAINT chk_frd_difference_type CHECK (difference_type IN ('amount','count','status','missing'));
```

## 2. 逻辑变更（`FinanceService`）

**日结汇总 + 锁定** `lockDailyClose(businessDate, scope)`：
- 聚合六源（按 `r5-aggregation-metrics-draft` §1.2 口径）写入 `finance_daily_close_summaries`；官方售票=`ticket_orders`、第三方渠道/回流=`integration_order_syncs`、酒吧=`bar_bills/bar_bill_settlements`、游乐=`amusement_verifications`(客流计数)、供应链成本=`inventory_stock_movements/inventory_purchase_requests`。
- 将该 business_date 范围内的 `finance_ar_ap_items` 打上 `locked_day_close_id`。
- `finance_daily_closes.status=locked`。

**锁定后硬拦截**：`createArApItem`/`registerArApPayment`/退款 等改影响金额的操作，先查目标 item 的 `locked_day_close_id`：
- 已锁定 → `throw ApiException.conflict("DAY_CLOSE_LOCKED", ...)`，**禁止直改**；引导走补录审批。

**补录审批闭环**：
- `requestAdjustment(itemId, amountCent, reason)` → 建 `ar_ap_adjustments(pending)`；权限 `finance.locked_backfill.request`。
- `approveAdjustment(id)` → 上级审批（复用 supervisor_review 或 finance 内审），通过后才真正写入金额变动并标 approved；reject 不改账。权限 `finance.locked_backfill.approve`。

## 3. API / 事件 / 权限
- `POST /api/v1/finance/daily-closes/{id}/lock`（含汇总）、`GET .../summaries`；`POST /api/v1/finance/ar-ap-adjustments`、`/{id}/approve`。
- 事件：`finance.daily_close_locked`、`finance.locked_backfill_requested`、`finance.locked_backfill_approved/rejected`、`finance.daily_close_summarized`。
- 权限：`finance.daily_close.manage`、`finance.locked_backfill.request`、`finance.locked_backfill.approve`。
- 隔离：日结/汇总/对账查询经 `currentMerchantScope`（商户只见自身 scope）。

## 4. 验收（GWT）
- Given 某经营日已锁定，When 直接改该日 AR/AP 影响金额或发起退款，Then 被拒 DAY_CLOSE_LOCKED。
- Given 已锁定，When 走 requestAdjustment→approveAdjustment，Then 审批通过后金额变动入账并留痕；reject 不改账。
- Given 锁定执行，Then finance_daily_close_summaries 六源金额=真实汇总（非种子），日结金额来源可追溯。
- Given 对账差异，Then difference_type 仅取 amount/count/status/missing。

## 5. 测试 / 依赖 / claims
- 测试：锁定后直改被拒、补录审批闭环、六源汇总数值正确、difference_type CHECK、隔离；`MigrationVersions.latest()`。
- 依赖：`finance_daily_closes`、`finance_ar_ap_items`、`ticket_orders`/`bar_bills`/`integration_order_syncs`/`amusement_verifications`/`inventory_stock_movements`（汇总源）。
- **claims 同步**：ar_ap_adjustments present、finance_ar_ap_items.locked_day_close_id present、finance_daily_close_summaries present；矩阵财务行（日结锁定）下修转"已落地"。
- **前置**：两套 ar_ap / reconciliation 表收口（见顶部），聚合口径草案经财务复核后再定稿汇总公式。

## 6. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T7 规格（六源汇总表+locked_day_close_id+ar_ap_adjustments 审批闭环+差异四类 CHECK） | R5 backlog T7；裁决⑤；现状取自 V002/V008；口径见 r5-aggregation-metrics-draft |
