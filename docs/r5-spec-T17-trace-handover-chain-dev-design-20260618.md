# T17 扫码交接全链状态机 可编码开发规格 V0.1（2026-06-18）

> **任务**：R5 backlog **T17**（W1.5，REQ-38/44 P0），供应链×留痕域。
> **裁决依据**：二轮裁决——**员工扫码主动留痕 + 机器人上报被动留痕**；机器人上报与人工扫码出现缺口/重复/差异进复核。
> **现状基准**（已核）：`inventory_trace_handovers`(V012) 为**单条平面记录**（trace_code/from_staff/to_staff/device/`handover_result`），`InventoryService.scanTraceCodeHandover` 把 `handover_result` 写死 'accepted'，无分段状态机、无机器人段、无缺口异常。
> **迁移**：取下一连续版本（示意 **V074**）；测试用 `MigrationVersions.latest()`。

---

## 1. 数据模型（迁移草案：表头 + 分段步骤）
```sql
-- 保留 inventory_trace_handovers 作"一次交接任务表头"，补整体状态
ALTER TABLE inventory_trace_handovers
  ADD COLUMN IF NOT EXISTS overall_status VARCHAR(32) NOT NULL DEFAULT 'in_progress',
  ADD COLUMN IF NOT EXISTS current_segment VARCHAR(40);
ALTER TABLE inventory_trace_handovers
  ADD CONSTRAINT chk_trace_handover_overall CHECK
    (overall_status IN ('in_progress','completed','exception'));

-- 分段步骤（五段链）
CREATE TABLE IF NOT EXISTS inventory_trace_handover_steps (
    id VARCHAR(64) PRIMARY KEY,
    handover_id VARCHAR(64) NOT NULL REFERENCES inventory_trace_handovers(id) ON DELETE CASCADE,
    segment VARCHAR(40) NOT NULL,            -- 五段
    seq SMALLINT NOT NULL,                   -- 段序 1..5
    actor_type VARCHAR(16) NOT NULL,         -- staff(主动扫码) / robot(被动上报)
    operator_user_id VARCHAR(64),            -- staff 段
    robot_task_ref VARCHAR(128),             -- robot 段
    scan_at TIMESTAMPTZ,
    step_status VARCHAR(24) NOT NULL DEFAULT 'pending', -- pending/done/gap/duplicate/diff
    note VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_trace_step_segment CHECK (segment IN
      ('out_verify','dispatch','kitchen_receive','staff_pickup','deliver_customer')),
    CONSTRAINT chk_trace_step_actor CHECK (actor_type IN ('staff','robot')),
    CONSTRAINT chk_trace_step_status CHECK (step_status IN ('pending','done','gap','duplicate','diff')),
    CONSTRAINT uq_trace_step UNIQUE (handover_id, segment)
);

-- 交接异常 → 复核（缺口/重复/差异）
CREATE TABLE IF NOT EXISTS inventory_trace_handover_exceptions (
    id VARCHAR(64) PRIMARY KEY,
    handover_id VARCHAR(64) NOT NULL REFERENCES inventory_trace_handovers(id),
    segment VARCHAR(40) NOT NULL,
    exception_type VARCHAR(16) NOT NULL,     -- gap/duplicate/diff
    review_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    reviewer_user_id VARCHAR(64),
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT chk_trace_exc_type CHECK (exception_type IN ('gap','duplicate','diff')),
    CONSTRAINT chk_trace_exc_status CHECK (review_status IN ('pending','approved','rejected'))
);
```

## 2. 状态机（五段链）
段序：`out_verify(库管出库核验) → dispatch(机器人/人工配送) → kitchen_receive(后厨接收) → staff_pickup(服务员取走) → deliver_customer(送达客人/卡座)`。
- **staff 段**：工作手机扫平台唯一二维码 `scanTraceSegment(handoverId, segment)` → step done + scan_at；**主动留痕**。
- **robot 段（dispatch）**：机器人上报 `reportRobotSegment(handoverId, robotTaskRef, status)` → step done（actor_type=robot）；**被动留痕**。
- 全部 done → `overall_status=completed`。

## 3. 缺口/重复/差异 → 复核
- **gap**：上一段 done 后超时下一段未 done / 机器人上报送达但无人工接收扫码 → step_status=gap。
- **duplicate**：同段重复扫码 → duplicate。
- **diff**：机器人上报与人工扫码不一致（如送达点位/物料不符）→ diff。
- 任一异常 → 建 `inventory_trace_handover_exceptions(pending)` + `overall_status=exception`，进复核；主管 approve/reject 后闭合。

## 4. API / 事件 / 权限
- `POST /api/v1/inventory/trace-handovers`（建任务）、`/{id}/scan-segment`（staff）、`/{id}/robot-report`（机器人回调，受第三方开关门控）、`/exceptions/{id}/review`。
- 事件：`inventory.handover_segment_scanned`、`inventory.handover_completed`、`inventory.handover_exception_raised`、`inventory.handover_exception_resolved`。
- 权限：`inventory.handover.scan`、`inventory.handover.review`。

## 5. 验收（GWT）
- Given 一次交接，When 五段依次扫码/上报，Then 各 step done、overall completed、全程可追责（替换单条 accepted）。
- Given 机器人上报送达但服务员未扫接收，Then 产 gap 异常 + overall exception + 进复核。
- Given 同段重复扫码，Then duplicate；Given 机器人/人工不符，Then diff。
- Given 异常复核 approve，Then 闭合留结论。

## 6. 测试 / 依赖 / claims
- 测试：五段流转、staff 主动/robot 被动、gap/duplicate/diff 判定、复核闭合、段唯一约束；`MigrationVersions.latest()`。
- 依赖：`inventory_trace_handovers`(V012)、机器人回调（受 T2/T14 第三方开关门控）、工作手机扫码。
- **claims**：inventory_trace_handover_steps/exceptions present、overall_status present；矩阵 REQ-38/39/44 行下修转"全链已落地"。

## 7. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T17 规格（五段交接状态机+staff 主动/robot 被动+gap/duplicate/diff 复核） | R5 backlog T17；二轮裁决；现状取自 V012 |
