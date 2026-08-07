# P2-B 游乐团体/包场预订与容量锁定开发计划

## 1. 目标

在现有游乐项目、场次、预约/排队轻闭环基础上，新增团体/包场预订能力。P2-B 只覆盖后台创建、人工审批、容量锁定和释放，不处理价格、退款、改期和销售归属。

## 2. 本期边界

### 包含

- 后台创建团体/包场预订。
- 创建后状态为 `pending_approval`，不立即占用容量。
- 审批通过后原子锁定场次容量。（**口径已废止**：BSM「容量控制」条只按单个项目的实时在玩人数控制容量；本条只记录当时的计划）
- 释放已审批预订后归还容量。
- 普通核销必须同时检查已使用容量和已锁定容量。
- 全流程权限、幂等和审计留痕。

### 不包含

- 团体/包场价格、付款、退款。
- 改期、拆分、合并预订。
- 小程序/App 自助团体预订。
- 销售提成归属。

## 3. 文件变更

### 修改文件

- `backend/src/main/resources/db/migration/V035__p2_amusement_group_private_bookings.sql`
- `backend/src/main/java/com/complex/platform/amusement/AmusementSessionRecord.java`
- `backend/src/main/java/com/complex/platform/amusement/AmusementRepository.java`
- `backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRepository.java`
- `backend/src/main/java/com/complex/platform/amusement/AmusementService.java`
- `backend/src/main/java/com/complex/platform/amusement/AmusementController.java`
- `backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java`
- `backend/src/test/java/com/complex/platform/amusement/AmusementControllerTests.java`
- `backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java`
- `backend/src/test/java/**/*JdbcIntegrationTests.java` 中的 Flyway 版本断言。

### 新增文件

- `backend/src/main/java/com/complex/platform/amusement/AmusementGroupBookingRecord.java`
- `backend/src/main/java/com/complex/platform/amusement/AmusementGroupBookingRequest.java`
- `backend/src/main/java/com/complex/platform/amusement/AmusementGroupBookingApprovalRequest.java`
- `backend/src/main/java/com/complex/platform/amusement/AmusementGroupBookingReleaseRequest.java`
- `docs/p2-amusement-group-private-booking-api-contract-v0.1.md`
- `docs/p2-amusement-group-private-booking-completion-audit-v0.1.md`

## 4. 数据结构与接口变化

- `amusement_sessions` 新增 `locked_capacity integer not null default 0`。
- 新增 `amusement_group_bookings`，记录预订类型、客户信息、人数、锁定容量、状态、审批/释放信息和证据附件。
- 新增权限：
  - `amusement.group_booking.view`
  - `amusement.group_booking.manage`
  - `amusement.group_booking.approve`
- 新增接口：
  - `GET /api/v1/amusement/group-bookings`
  - `POST /api/v1/amusement/group-bookings`
  - `POST /api/v1/amusement/group-bookings/{id}/approve`
  - `POST /api/v1/amusement/group-bookings/{id}/release`

## 5. 测试点

- 创建团体/包场预订后状态为 `pending_approval`。
- 创建预订不占用 `locked_capacity`。
- 审批时使用数据库锁和 CTE 原子校验容量。
- 重复审批不会重复增加锁定容量。
- 释放时归还容量。
- 重复释放不会重复扣减锁定容量。
- 普通核销使用 `used_capacity + locked_capacity < capacity` 进行容量判断。
- 控制器接口使用精确权限并要求幂等键。
- JDBC 集成测试覆盖迁移、权限和容量锁定字段。

## 6. 验收标准

- `AmusementServiceTests`、`AmusementControllerTests`、`AmusementJdbcIntegrationTests` 通过。
- 后端全量测试通过。
- P0 契约闸口通过。
- Semgrep 对 P2-B 相关文件扫描无高危发现。

## 7. 风险与回滚

- 风险：容量并发超卖。处理：审批和释放使用数据库级原子更新。
- 风险：业务误以为已覆盖收费。处理：文档明确本期不做付款/退款/价格。
- 回滚：撤回 V035 和相关接口；未修改票务、支付、财务主流程。
