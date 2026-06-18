# T16 订台定金闭环 可编码开发规格 V0.1（2026-06-18）

> **任务**：R5 backlog **T16**（W1.5），酒吧域。
> **裁决依据**：二轮裁决——订台定金**后台可配：固定额与按最低消费比例两种都支持**，退/没收时限后台可配。
> **现状基准**（已核）：`bar_reservations` 有 `deposit_amount_cent`(默认0)/`deposit_status`/`order_source`/`sales_staff_id`/`status`，V063 已加 `deposit_rule`；`bar_table_guest_checkins` 已在；但 `BarEntryService.createReservation` **硬编码 `deposit_amount_cent=0,deposit_status='not_required'`**，§13.1 到店抵扣/取消退没收逻辑因定金恒 0 空转。
> **迁移**：取下一连续版本（示意 **V073**）；测试用 `MigrationVersions.latest()`。

---

## 1. 数据模型（迁移草案）
```sql
-- 定金规则（后台可配：固定额 / 按低消比例；退没收时限）
CREATE TABLE IF NOT EXISTS bar_deposit_rules (
    id VARCHAR(64) PRIMARY KEY,
    scope_type VARCHAR(32) NOT NULL DEFAULT 'bar',   -- bar/table_type/time_slot
    scope_id VARCHAR(64) NOT NULL,
    mode VARCHAR(16) NOT NULL,                        -- fixed / min_consumption_ratio
    fixed_amount_cent BIGINT,                         -- mode=fixed
    ratio_bps INTEGER,                                -- mode=ratio：基点(如3000=30%)
    cancel_free_before_minutes INTEGER NOT NULL DEFAULT 0,  -- 取消免没收时限
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_bar_deposit_mode CHECK (mode IN ('fixed','min_consumption_ratio'))
);

-- 订台补定金流转字段
ALTER TABLE bar_reservations
  ADD COLUMN IF NOT EXISTS deposit_intent_id VARCHAR(64),     -- 定金支付意图
  ADD COLUMN IF NOT EXISTS deposit_paid_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS deposit_settlement VARCHAR(16);    -- deducted/refunded/forfeited
ALTER TABLE bar_reservations
  ADD CONSTRAINT chk_bar_reservations_deposit_settle CHECK
    (deposit_settlement IS NULL OR deposit_settlement IN ('deducted','refunded','forfeited'));
-- deposit_status 取值收口
ALTER TABLE bar_reservations
  ADD CONSTRAINT chk_bar_reservations_deposit_status CHECK
    (deposit_status IN ('not_required','pending','paid','settled'));
```

## 2. 逻辑（`BarEntryService` / `BarBillingService`）
- **创建订台**：按 `bar_deposit_rules`（scope 优先级 table_type/time_slot > bar）算定金——`fixed` 取 fixed_amount_cent，`min_consumption_ratio` 取 `桌台 min_consumption_cent × ratio_bps/10000`；写 `deposit_amount_cent` + `deposit_status='pending'`（**替换硬编码 0**），生成 `deposit_intent_id`（聚合/现金/POS 支付）。
- **支付定金**：支付成功 → `deposit_status='paid'`/`deposit_paid_at`。
- **到店核销**：`bar_table_guest_checkins` 到店 → 定金转**抵扣**进账单（`BarBillingService.depositDeduct`），`deposit_status='settled'`/`deposit_settlement='deducted'`。
- **取消/爽约**：取消时点早于 `cancel_free_before_minutes` → 退定金(`refunded`)；晚于/爽约 → 没收(`forfeited`)，进账单/收入并留痕。

## 3. API / 事件 / 权限
- `POST /api/v1/bar/reservations`（含定金计算）、`/{id}/pay-deposit`、`/{id}/checkin`、`/{id}/cancel`；`POST /api/v1/bar/deposit-rules`。
- 事件：`bar.reservation_deposit_required`、`bar.reservation_deposit_paid`、`bar.reservation_deposit_deducted`、`bar.reservation_deposit_refunded/forfeited`。
- 权限：`bar.reservation.manage`、`bar.deposit_rule.manage`。

## 4. 验收（GWT）
- Given 规则 mode=min_consumption_ratio、ratio=30%、桌台低消 1000 元，When 订台，Then 定金=300 元、deposit_status=pending（非 0）。
- Given 已付定金，When 到店 checkin，Then 定金抵扣进账单、settled/deducted。
- Given 取消早于免没收时限，Then 退定金 refunded；晚于/爽约，Then 没收 forfeited 进账单。

## 5. 测试 / 依赖 / claims
- 测试：固定额/比例两种计算、抵扣、退/没收时限分支、deposit_status CHECK；`MigrationVersions.latest()`。
- 依赖：`bar_reservations`(V013/V063)、`bar_tables.min_consumption_cent`、`BarBillingService`(depositDeduct 已存在但空转)。
- **claims**：bar_deposit_rules present、bar_reservations.deposit_settlement present；矩阵 REQ-25/26 行下修转"定金闭环已落地"。

## 6. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T16 规格（bar_deposit_rules 固定额/比例+定金支付/到店抵扣/取消退没收时限闭环） | R5 backlog T16；二轮裁决；现状取自 V013/V063 |
