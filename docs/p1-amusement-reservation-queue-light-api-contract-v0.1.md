# P1 游乐预约/排队轻闭环接口契约 V0.12

## 1. 范围

本切片补齐游乐预约与排队的最小可编码闭环：创建预约、预约状态变更、加入排队、排队状态变更、查询列表、权限和审计留痕。

不包含动态容量策略、团体/包场、跨项目集团视图、第三方排队系统 SDK 对接、短信/企微自动通知和硬件闸机控制。

## 2. 接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/amusement/reservations` | `amusement.reservation.view` | 查询预约记录 |
| POST | `/api/v1/amusement/reservations` | `amusement.reservation.manage` | 创建预约 |
| POST | `/api/v1/amusement/reservations/{id}/status` | `amusement.reservation.manage` | 变更预约状态 |
| GET | `/api/v1/amusement/queue-entries` | `amusement.queue.view` | 查询排队记录 |
| POST | `/api/v1/amusement/queue-entries` | `amusement.queue.manage` | 加入排队 |
| POST | `/api/v1/amusement/queue-entries/{id}/status` | `amusement.queue.manage` | 变更排队状态 |

## 3. 请求

创建预约：

```json
{
  "idempotency_key": "reservation-once",
  "project_id": "project_1",
  "session_id": "session_1",
  "customer_id": "customer_1",
  "reservation_status": "reserved"
}
```

加入排队：

```json
{
  "idempotency_key": "queue-once",
  "project_id": "project_1",
  "session_id": "session_1",
  "customer_id": "customer_1",
  "queue_no": "A001",
  "status": "waiting"
}
```

状态变更：

```json
{
  "idempotency_key": "reservation-complete-once",
  "action": "completed",
  "reason": "customer arrived"
}
```

## 4. 状态

预约状态：`reserved`、`cancelled`、`completed`、`no_show`。

排队状态：`waiting`、`called`、`checked_in`、`cancelled`、`no_show`。

创建预约和排队时，场次必须属于指定项目，且场次状态必须为 `scheduled` 或 `open`。`closed`、`cancelled` 场次不允许新增预约或排队。

## 5. 审计事件

| 动作 | 成功事件 | 失败事件 |
| --- | --- | --- |
| 创建预约 | `amusement.reservation.create` | `amusement.reservation.create_failed` |
| 预约状态变更 | `amusement.reservation.status_change` | `amusement.reservation.status_change_failed` |
| 加入排队 | `amusement.queue_entry.create` | `amusement.queue_entry.create_failed` |
| 排队状态变更 | `amusement.queue_entry.status_change` | `amusement.queue_entry.status_change_failed` |

## 6. 验收

- 使用相同 `idempotency_key` 重试创建预约或排队，返回同一条业务记录，不重复写入。
- 项目与场次不匹配时返回 `SESSION_PROJECT_MISMATCH`。
- 关闭或取消场次不允许新增预约或排队。
- 状态变更必须携带幂等键、目标状态和原因。
- V032 必须写入 `amusement.reservation.manage` 与 `amusement.queue.manage`，并授予 `role_platform_admin`。
