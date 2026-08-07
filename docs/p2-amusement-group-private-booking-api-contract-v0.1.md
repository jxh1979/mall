# P2-B 游乐团体/包场预订与容量锁定 API 契约 V0.1

> **口径修订（2026-08-06 R10）：「场次容量」已废止**——BSM「容量控制」条 明确「**只按单个游乐项目的实时在玩人数控制容量，不再按场次/时段或点位/设备另设容量；排队人数不占用容量**」（DEC-R5-2026-08-04-04；R8 用户裁决 D-02）。本文原「场次容量」一律改称「**项目容量（实时在玩人数）**」；双阈值不变（运营容量软上限告警放行、安全容量硬上限阻断并自动停运）。仍按场次/时段或点位/设备维度罗列的页面、接口、组件与字段只作历史结构参考，容量口径一律以本条为准，相关字段待代码轮下线，期间不得据以新增受控维度。

## 1. 定位

P2-B 为游乐场次增加团体/包场预订和容量锁定能力。它原解决“团体或包场需要提前占用场次容量，避免普通核销继续放人导致超容”的问题；该口径已被 amusement PRD §11A.3 / ENUM「团体/包场」条（DEC-R6-17）更正——**包场不是「占用整场次容量」，而是包场期间该游乐项目整体停止对外核销、仅该团体客人可入**；团体（批量预订）仍按项目容量（实时在玩人数）双阈值判定。本文按现行口径读，场次维容量锁定待代码轮下线。

本期只做后台创建、人工审批、容量锁定和释放，不处理付款、退款、改期和销售归属。

## 2. 权限与接口

| 接口 | 权限 |
| --- | --- |
| `GET /api/v1/amusement/group-bookings` | `amusement.group_booking.view` |
| `POST /api/v1/amusement/group-bookings` | `amusement.group_booking.manage` |
| `POST /api/v1/amusement/group-bookings/{id}/approve` | `amusement.group_booking.approve` |
| `POST /api/v1/amusement/group-bookings/{id}/release` | `amusement.group_booking.manage` |

所有写接口必须提供 `idempotency_key`。

## 3. 状态机

| 状态 | 含义 | 是否锁定容量 |
| --- | --- | --- |
| `pending_approval` | 已创建，等待审批 | 否 |
| `approved` | 已审批，已锁定容量 | 是 |
| `released` | 已释放，容量已归还 | 否 |
| `cancelled` | 预留状态，本期不提供独立取消接口 | 否 |

只有 `pending_approval` 可以审批；只有 `approved` 可以释放。

## 4. 创建团体/包场预订

`POST /api/v1/amusement/group-bookings`

请求字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `idempotency_key` | 是 | 幂等键 |
| `operator_note` | 否 | 操作说明 |
| `evidence_attachment_ids` | 否 | 证据附件 ID 列表 |
| `project_id` | 是 | 游乐项目 ID |
| `session_id` | 是 | 场次 ID |
| `booking_type` | 否 | `group` 或 `private`，默认 `group` |
| `customer_name` | 是 | 客户/团体名称 |
| `contact_phone` | 是 | 联系电话 |
| `party_size` | 是 | 实际人数，必须大于 0 |
| `reserved_capacity` | 是 | 需要锁定的容量，必须大于 0 |

创建成功后状态为 `pending_approval`，不立即锁定容量（原写「不立即占用场次容量」（原称「场次容量」，已废止·见文首口径））。

## 5. 审批并锁定容量

`POST /api/v1/amusement/group-bookings/{id}/approve`

请求字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `idempotency_key` | 是 | 幂等键 |
| `approval_note` | 是 | 审批说明 |
| `evidence_attachment_ids` | 否 | 审批证据 |

审批规则：

- 预订必须是 `pending_approval`。
- 场次必须存在且状态允许运营。
- 必须满足 `used_capacity + locked_capacity + reserved_capacity <= capacity`。
- 容量不足时返回 `SESSION_CAPACITY_LOCK_FAILED`，不得部分锁定。

## 6. 释放并归还容量

`POST /api/v1/amusement/group-bookings/{id}/release`

请求字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `idempotency_key` | 是 | 幂等键 |
| `reason` | 是 | 释放原因 |
| `evidence_attachment_ids` | 否 | 证据附件 |

释放规则：

- 预订必须是 `approved`。
- 释放后状态为 `released`。
- 场次 `locked_capacity` 扣回对应 `reserved_capacity`。

## 7. 对普通核销的影响

普通游乐核销继续走原接口，但容量判断改为：

`used_capacity + locked_capacity < capacity`

因此已经被团体/包场锁定的容量不会再被普通客人占用。

## 8. 审计事件

| 事件 | 触发时机 |
| --- | --- |
| `amusement.group_booking.created` | 创建预订 |
| `amusement.group_booking.approved` | 审批并锁定容量 |
| `amusement.group_booking.released` | 释放并归还容量 |
| `amusement.group_booking.viewed` | 查看列表 |

## 9. 明确不进入本期

- 团体/包场收费。
- 退款和改期。
- 小程序/App 自助预订。
- 销售提成归属。
