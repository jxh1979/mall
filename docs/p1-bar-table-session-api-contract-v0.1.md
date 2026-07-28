# P1 酒吧卡座会话调整 API 契约 V0.1

> **[A2 修订 2026-07-28]** 与 MAIN `bar-business-operations-product-prd-v0.1.md` §7.3A / `DEC-SEAT-NO-RENEW-001` 对齐：
> **产品不做「续卡/续时」**。下文原 `renew` 接口与字段整节废止，不得再作为实现/验收依据。
> 转台/并台业务规则以 MAIN §7.3A 为准（转台低消进度结转多不退少补；并台低消加总；销售提成与服务员提成规则见 PRD）。

## 1. 文档定位

本文档是 P1-BAR-01「卡座**转台**、卡座**并台**」的接口契约补充，不替代 P0 API Contract Pack。P0 仍保持最小营业闭环；本 P1 契约只覆盖卡座资源在营业中发生调整时的后端接口、权限、幂等、留痕和验收边界。


运行时 OpenAPI 由 Spring Boot 4.x + springdoc 从控制器自动生成；本文档作为开发、测试、前端和平板/员工端调用的人工契约说明。

## 2. 业务边界

| 能力 | P1 本次包含 | 不包含 |
|---|---|---|
| 开卡座会话 | 支持基于卡座、酒吧、商户、营业日创建开放会话 | 不做完整预订资源日历 |
| 转卡 | 同一酒吧内从原卡座转到目标卡座，保留调整记录 | 不允许跨酒吧转卡 |
| 合并 | 同一酒吧、同一商户、同一营业日内将副会话并入主会话 | 不做复杂账单合并、拆单、挂账 |
| ~~续卡~~ | **不做**（DEC-SEAT-NO-RENEW-001） | 需要继续消费：当前会话加单或关台后重开 |
| 留痕 | 每次调整写入 `bar_table_session_adjustments` 并记录审计事件 | 不接第三方 POS 高级账单能力 |

## 3. 接口清单

| 方法 | 路径 | 权限点 | 幂等 | 说明 |
|---|---|---|---|---|
| GET | `/api/v1/bar/table-sessions` | `bar.table_session.view` | 否 | 查询卡座会话列表 |
| POST | `/api/v1/bar/table-sessions` | `bar.table_session.create` | 是 | 创建卡座会话 |
| POST | `/api/v1/bar/table-sessions/{session_id}/transfer` | `bar.table_session.transfer` | 是 | 转卡 |
| POST | `/api/v1/bar/table-sessions/{session_id}/merge` | `bar.table_session.merge` | 是 | 合并卡座会话 |
| ~~POST~~ | ~~`/api/v1/bar/table-sessions/{session_id}/renew`~~ | ~~`bar.table_session.renew`~~ | — | **废止：续卡** |

所有写接口必须提供请求体内 `idempotencyKey`，接口层会拒绝空幂等键。

## 4. 核心字段

### 4.1 创建卡座会话

`BarTableSessionRequest`：

| 字段 | 必填 | 说明 |
|---|---:|---|
| `idempotencyKey` | 是 | 幂等键 |
| `barId` | 是 | 酒吧 ID，必须与卡座归属一致 |
| `merchantId` | 是 | 商户/运营主体 ID |
| `businessDate` | 是 | 营业日 |
| `tableId` | 是 | 卡座 ID |
| `reservationId` | 否 | 关联预订；如果提供，必须与酒吧和卡座一致 |
| `customerId` | 否 | 客户 ID |
| `guestCount` | 否 | 入座人数，必须非负 |
| `salesStaffId` | 否 | 销售人员 ID |
| `serviceStaffId` | 否 | 服务人员 ID |
| `openedByUserId` | 否 | 开卡操作人，后续可并入操作者上下文 |
| `evidenceAttachmentIds` | 否 | 证据附件 |

### 4.2 转卡

`BarTableTransferRequest`：

| 字段 | 必填 | 说明 |
|---|---:|---|
| `idempotencyKey` | 是 | 幂等键 |
| `targetTableId` | 是 | 目标卡座，必须与当前会话同酒吧且状态为 `active` |
| `reason` | 是 | 调整原因 |
| `requestedByUserId` | 是 | 操作人 |
| `sessionVersion` | 是 | 乐观锁版本，防止并发覆盖 |
| `evidenceAttachmentIds` | 否 | 证据附件 |

转卡成功后：原卡座状态置为 `pending_clear`，目标卡座状态置为 `opened`，会话卡座 ID 更新为目标卡座，会话版本加 1。

### 4.3 合并

`BarTableMergeRequest`：

| 字段 | 必填 | 说明 |
|---|---:|---|
| `idempotencyKey` | 是 | 幂等键 |
| `mergedSessionId` | 是 | 被合并的副会话 |
| `reason` | 是 | 合并原因 |
| `requestedByUserId` | 是 | 操作人 |
| `primarySessionVersion` | 是 | 主会话版本 |
| `mergedSessionVersion` | 是 | 副会话版本 |
| `evidenceAttachmentIds` | 否 | 证据附件 |

合并只允许同一酒吧、同一商户、同一营业日内执行。成功后主会话版本加 1，副会话状态置为 `merged`，副会话 `sourceSessionId` 指向主会话，副卡座置为 `pending_clear`。

### 4.4 续卡（废止）

> **整节废止（2026-07-28 / DEC-SEAT-NO-RENEW-001）。**
>
> - 不得实现 `POST .../renew` 为成功路径；若路由仍残留，必须返回业务拒绝（建议 `FEATURE_NOT_AVAILABLE` / `VALIDATION_FAILED`）并留痕。
> - 不得在权限模板默认授予 `bar.table_session.renew` / `bar.table.renew`。
> - 历史已落库的 `adjustment_type=renew` 记录只读保留，不作新能力入口。
> - 业务替代：当前已开台会话内正常加单；或关台后重新开台（新开台不继承已关台低消进度，除非走转台/并台）。


## 5. 错误码

| 错误码 | 场景 |
|---|---|
| `VALIDATION_FAILED` | 必填字段缺失、金额为负、非法调整类型（含废止的续卡） |
| `BAR_TABLE_NOT_FOUND` | 卡座不存在 |
| `BAR_TABLE_SESSION_NOT_FOUND` | 卡座会话不存在 |
| `BAR_TABLE_SESSION_NOT_ACTIVE` | 会话不是开放状态 |
| `BAR_TABLE_SESSION_VERSION_CONFLICT` | 请求版本与当前会话版本不一致 |
| `BAR_TABLE_NOT_OPENABLE` | 创建会话时卡座不可开 |
| `BAR_TABLE_NOT_TRANSFERABLE` | 目标卡座不可转入 |
| `BAR_RESERVATION_NOT_FOUND` | 关联预订不存在 |
| `BAR_SCOPE_MISMATCH` | 跨酒吧、跨商户、跨营业日或预订不匹配 |

## 6. 审计与验收

每个写操作必须满足：

| 操作 | 调整记录 | 审计事件 |
|---|---|---|
| 创建会话 | 不写调整记录 | `bar.table_session.created` |
| 转卡 | `adjustment_type=transfer` | `bar.table.transferred` / `bar.table.transfer_failed` |
| 合并 | `adjustment_type=merge` | `bar.table.merged` / `bar.table.merge_failed` |
| ~~续卡~~ | ~~`adjustment_type=renew`~~ | **废止；禁止新发成功事件** |

自动化验收以 160 机器为准：

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarServiceTests,BarControllerTests,BarJdbcIntegrationTests test
```

当前验收覆盖：

| 测试 | 覆盖点 |
|---|---|
| `BarServiceTests` | 业务规则、跨酒吧拒绝、版本冲突、状态变化、审计事件 |
| `BarControllerTests` | 路径、权限点、幂等键校验 |
| `BarJdbcIntegrationTests` | Flyway 019、表结构、权限种子、JDBC 保存和查询 |

## 变更日志

| 2026-07-28 | A2 | 废止续卡整节与 renew 接口；对齐 MAIN §7.3A / DEC-SEAT-NO-RENEW-001 |
