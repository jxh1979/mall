# T5 多人票核销 + 核销冲正 可编码开发规格 V0.1（2026-06-18）

> **任务**：R5 backlog **T5**，W1 票务域，紧随 T4（建议**合并同一迁移**，同表 ALTER 避免版本冲突）。
> **裁决依据**：裁决②——多人票核销 P0、核销冲正(reverse) P0。
> **现状基准**（已核）：`issued_entitlements` 有 `total_times/used_times`，**无 `num_people`**；`ticket_verifications`(V006) 无 `verified_people`、无 `reversed` 态、无冲正关联；多人票 D9 约束（verifiedPeople=num_people）`grep` 全 Java 零命中。
> **迁移**：与 T4 同迁移（示意 **V068**）；测试用 `MigrationVersions.latest()`。

---

## 1. 数据模型（与 T4 合并迁移）
```sql
-- 多人票：发放时记录人数
ALTER TABLE issued_entitlements ADD COLUMN IF NOT EXISTS num_people INTEGER NOT NULL DEFAULT 1;

-- 核销：记录本次核销人数 + 冲正关联（接 T4 的 ticket_verifications ALTER）
ALTER TABLE ticket_verifications
  ADD COLUMN IF NOT EXISTS verified_people INTEGER NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS reverses_verification_id VARCHAR(64) REFERENCES ticket_verifications(id),
  ADD COLUMN IF NOT EXISTS reversed_by VARCHAR(64),
  ADD COLUMN IF NOT EXISTS reversed_at TIMESTAMPTZ;
-- status 取值扩展（现为自由 VARCHAR(32)，本期补 CHECK 收口）
ALTER TABLE ticket_verifications
  ADD CONSTRAINT chk_ticket_verifications_status CHECK (status IN ('success','failed','rejected','reversed'));
```

## 2. 逻辑（`TicketVerificationService` / `TicketOrderService`）
**多人票核销**：
- 发放时按票种写 `issued_entitlements.num_people`（联合/多人票 >1）。
- 核销口径（D9，**待业务最终确认一次核多人 vs 拆次**，本规格默认"同场次一次集中核销 num_people"）：`verify` 时 `verified_people = num_people`，对 `used_times` 做一次性 +1（或按 times 模型扣减），不可超 `total_times`。
- 校验：`verified_people ≤ num_people` 且未超 total_times。

**核销冲正 reverse**：
- `POST /verifications/{id}/reverse`：对 status=success 的核销建一条新 verification（status=reversed，`reverses_verification_id=原id`），回滚 `issued_entitlements.used_times`，原记录标 reversed_by/reversed_at；若已联动结算/赠饮扣库存，触发对应冲正（与 T7 结算冲正、T15 库存回补对齐）。
- 仅允许有 `ticket.verify.reverse` 权限者在可冲正窗口内操作；冲正必须填 reason。

## 3. API / 事件 / 权限
- `POST /api/v1/ticketing/verifications/{id}/reverse`（reason 必填）。
- 事件：`ticket.verification_reversed`。
- 权限：`ticket.verify.reverse`（敏感，可挂准入/审批）。

## 4. 验收（GWT）
- 多人票：Given num_people=4 的票，When 核销，Then verified_people=4，used_times 不超 total_times；超额核销被拒。
- 冲正：Given 误核销，When reverse，Then 原记录 reversed、used_times 回滚、生成 reversed 记录可追溯；联动结算/库存同步冲正。
- 权限：无 ticket.verify.reverse 不能冲正。

## 5. 测试 / 依赖 / claims
- 测试：多人票核销人数校验、超额拒绝、冲正回滚 used_times、冲正幂等（重复 reverse 拒绝）、status CHECK；`MigrationVersions.latest()`。
- 依赖：T4（同表 ALTER）、`issued_entitlements`、（冲正联动）T7 结算 / T15 赠饮库存。
- **claims**：issued_entitlements.num_people present、ticket_verifications.verified_people/reverses_verification_id present；矩阵多人票/冲正行下修转"已落地"。

## 6. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T5 规格（num_people 多人票核销 + reverse 冲正回滚+联动） | R5 backlog T5；裁决②；现状取自 V006/V009 |
