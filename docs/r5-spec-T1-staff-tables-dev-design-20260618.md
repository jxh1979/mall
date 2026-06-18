# T1 员工三表 可编码开发规格 V0.1（2026-06-18）

> **任务**：R5 backlog **T1（员工档案/设备凭证/通知三表）**，W0 底座，**解锁 T9（Android 设备凭证）**。
> **裁决依据**：R5 裁决⑨——员工三表 + Android 端凭证本期 P0；MDM 自有侧字段暂缓。
> **关键边界**：本规格的 `staff_device_credentials`（员工私人手机 Keystore 公钥/不可逆指纹）与既有 `terminal_device_credentials`（V011，kiosk 一体机随机 token）是**两套完全不同实体**，互不复用、互不串值。
> **工程约定**（对齐既有迁移）：`id VARCHAR(64)` 主键；`TIMESTAMPTZ ... DEFAULT now()`；CHECK 命名 `chk_<表>_<字段>`、唯一 `uq_<表>_<列>`；隔离列 `project_id VARCHAR(64) NOT NULL`、`merchant_id VARCHAR(64)`（NULL=平台员工）；个人信息只存不可逆哈希（PIPL，compliance §4）。
> **迁移**：当前最新 V065，本任务取 **V066**（连续取号）；测试用 `MigrationVersions.latest()` 动态读版本（R3 已去硬钉，无需改 12 处 JDBC 测试）。落地后把 `matrix-migration-claims.json` 中 `staff_profiles`/`staff_device_credentials`/`employee_notices` 由 absent 改 present。

---

## 1. 数据模型（Flyway V066 草案 DDL）

```sql
-- V066__p0_staff_profiles_and_device_credentials.sql

-- 1) 员工档案
CREATE TABLE IF NOT EXISTS staff_profiles (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,                 -- 综合体归属（隔离）
    merchant_id VARCHAR(64),                         -- NULL=平台员工；非空=商户员工
    user_id VARCHAR(64),                             -- 审批通过后关联 access_users.id（生成角色）
    name VARCHAR(128) NOT NULL,
    phone_hash VARCHAR(64),                          -- 手机号不可逆哈希（PIPL）
    id_doc_hash VARCHAR(64),                         -- 证件不可逆哈希（可选）
    staff_level_id VARCHAR(64) REFERENCES staff_levels(id),  -- 职级（V055）
    registration_status VARCHAR(32) NOT NULL DEFAULT 'submitted',
    approved_by VARCHAR(64),
    approved_at TIMESTAMPTZ,
    reject_reason VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_staff_profiles_reg CHECK (registration_status IN ('submitted','approved','rejected','suspended')),
    CONSTRAINT chk_staff_profiles_status CHECK (status IN ('active','inactive'))
);
CREATE INDEX IF NOT EXISTS idx_staff_profiles_scope ON staff_profiles(project_id, merchant_id);

-- 2) 员工私人手机设备凭证（Keystore 公钥 / 不可逆指纹）—— 区别于 kiosk 的 terminal_device_credentials
CREATE TABLE IF NOT EXISTS staff_device_credentials (
    id VARCHAR(64) PRIMARY KEY,
    staff_profile_id VARCHAR(64) NOT NULL REFERENCES staff_profiles(id) ON DELETE CASCADE,
    device_public_key TEXT NOT NULL,                 -- Android Keystore 设备公钥
    device_fingerprint_hash VARCHAR(64) NOT NULL,    -- 不可逆设备指纹哈希（不读手机本机 NFC 唯一标识，D8）
    bind_status VARCHAR(32) NOT NULL DEFAULT 'pending_review',
    supersedes_credential_id VARCHAR(64) REFERENCES staff_device_credentials(id),  -- 换机链：指向被替换旧凭证
    integrity_status VARCHAR(32) NOT NULL DEFAULT 'trusted',
    revoked_reason VARCHAR(255),
    bound_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_staff_dev_cred_bind CHECK (bind_status IN ('pending_review','active','superseded','revoked')),
    CONSTRAINT chk_staff_dev_cred_integrity CHECK (integrity_status IN ('trusted','root_detected','debug_detected','abnormal'))
);
-- 每个员工至多一条 active 凭证（换机先 superseded 旧的再 active 新的）
CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_dev_cred_one_active
    ON staff_device_credentials(staff_profile_id) WHERE bind_status = 'active';
CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_dev_cred_fingerprint
    ON staff_device_credentials(device_fingerprint_hash) WHERE bind_status IN ('pending_review','active');

-- 3) 员工通知 / 合规同意凭据（必须有写入方，避免"建而无写入方"）
CREATE TABLE IF NOT EXISTS employee_notices (
    id VARCHAR(64) PRIMARY KEY,
    staff_profile_id VARCHAR(64) NOT NULL REFERENCES staff_profiles(id) ON DELETE CASCADE,
    notice_type VARCHAR(48) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    consent_required BOOLEAN NOT NULL DEFAULT false,
    consent_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    consented_at TIMESTAMPTZ,
    created_by VARCHAR(64) NOT NULL,                 -- 写入方（系统/审批人/合规模块）
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_employee_notices_type CHECK (notice_type IN ('approval_result','status_change','compliance_consent','system_notice')),
    CONSTRAINT chk_employee_notices_consent CHECK (consent_status IN ('pending','acknowledged','consented','declined'))
);
CREATE INDEX IF NOT EXISTS idx_employee_notices_staff ON employee_notices(staff_profile_id);
```

---

## 2. 状态机

**注册** `staff_profiles.registration_status`
`submitted → approved`（生成 user_id + 默认角色，写 staff.approved 事件 + approval_result 通知）｜`submitted → rejected`（reject_reason 必填）｜`approved → suspended`（停用，撤销关联角色）。

**设备凭证** `staff_device_credentials.bind_status`
- 首次绑定：`(无) → pending_review →（审批）active`，写 `bound_at`。
- 换机：新凭证 `pending_review` 且 `supersedes_credential_id=旧 active.id`；审批通过 → 新凭证 `active`、旧凭证 `superseded`（保证 `uq_staff_dev_cred_one_active`）。
- 失效：`active|pending_review → revoked`（解绑、或 integrity 检测异常），写 `revoked_at`/`revoked_reason`。
- 完整性：`integrity_status` 由 Android 端 report；`root_detected/debug_detected/abnormal` → 自动 `bind_status=revoked` + 写 staff_device.integrity_failed + 进库存域外的复核（沿用 supervisor_review 或仅通知，见下）。

> **换机口径**：先解绑旧、再绑新——通过"新凭证 pending_review + supersedes 链 + 审批后旧 superseded"实现"换机待审中间态"，符合 HTML"换手机必须先解绑旧再绑新"。

---

## 3. Service 方法（建议 `staff` 域）

`StaffProfileService`
- `submitRegistration(scope, req)` → 建 staff_profiles(submitted)；权限 `staff.profile.submit`。
- `reviewRegistration(scope, id, decision, reason)` → approve 生成 user_id+角色 / reject 写 reason；权限 `staff.profile.review`（DENY 优先 + currentMerchantScope）。
- `suspend(scope, id)` / `listProfiles(scope)`（强制 currentMerchantScope 过滤）。

`StaffDeviceCredentialService`
- `bindCredential(scope, staffId, publicKey, fingerprintHash)` → pending_review（若已有 active 且 fingerprint 不同→视为换机，置 supersedes）。
- `approveRebind(scope, credId)` → 新 active、旧 superseded；权限 `staff.device.review`。
- `reportIntegrity(staffId, credId, status)` → 非 trusted 即 revoke + 通知 + 事件；端侧上报。
- `revokeCredential(scope, credId, reason)` / `unbind(...)`；权限 `staff.device.revoke`。

`EmployeeNoticeService`
- `createNotice(staffId, type, title, body, consentRequired, createdBy)`（审批/状态变更/合规模块调用，保证有写入方）。
- `acknowledge(scope, noticeId, consent)` → consent_status 流转。

> 所有列表/查询注入 `currentMerchantScope`：平台角色见全量，商户角色仅见本 merchant_id 行。

---

## 4. API / OpenAPI 端点（P0）
| 方法 | 路径 | 权限点 | 说明 |
|---|---|---|---|
| POST | /api/v1/staff/registrations | staff.profile.submit | 私人手机 App 提交注册 |
| POST | /api/v1/staff/registrations/{id}/review | staff.profile.review | 后台审批通过/驳回 |
| GET | /api/v1/staff/profiles | staff.profile.view | 隔离列表 |
| POST | /api/v1/staff/device-credentials | staff.device.bind | 绑定/换机申请（pending_review）|
| POST | /api/v1/staff/device-credentials/{id}/approve-rebind | staff.device.review | 审批换机生效 |
| POST | /api/v1/staff/device-credentials/{id}/report-integrity | （端侧鉴权）| Root/调试上报 |
| POST | /api/v1/staff/device-credentials/{id}/revoke | staff.device.revoke | 解绑/失效 |
| GET | /api/v1/staff/notices | staff.notice.view | 通知/同意列表 |
| POST | /api/v1/staff/notices/{id}/acknowledge | staff.notice.view | 确认/同意 |

> OpenAPI json + yaml 同步（注意工具 `tools/apply-openapi-p0-examples.mjs` 需 `yaml` 包；若缺，手工镜像 yaml 并 pyyaml 校验）。

## 5. 事件（D4，点+下划线）
`staff.registered`、`staff.approved`、`staff.rejected`、`staff.suspended`；`staff_device.bound`、`staff_device.rebind_requested`、`staff_device.rebound`、`staff_device.revoked`、`staff_device.integrity_failed`；`staff.notice_created`、`staff.notice_acknowledged`。

## 6. 权限点（D3，dot 风格，纳入 T13 全业务域最小颗粒度预置）
`staff.profile.submit/review/view`、`staff.device.bind/review/revoke`、`staff.notice.view`。

---

## 7. 验收口径（GWT）
- **注册**：Given 员工提交注册，When 有 `staff.profile.review` 的上级审批通过，Then 生成 user_id+默认角色并发 approval_result 通知；驳回必须带 reason。
- **绑定/换机**：Given 员工已有 active 凭证，When 用新设备绑定，Then 新凭证 pending_review 且 supersedes 指向旧 active；审批通过后新 active、旧 superseded，`uq_staff_dev_cred_one_active` 始终成立。
- **完整性**：Given 端侧上报 root_detected，When reportIntegrity，Then 凭证 revoked + 发 integrity_failed + 通知。
- **隔离**：Given 商户角色，When 查 profiles/notices，Then 仅返回本 merchant_id 行（跨商户不可见）。
- **实体区分**：staff_device_credentials 与 terminal_device_credentials 无外键/无串值。

## 8. 测试计划
- 迁移应用：`MigrationVersions.latest()` 动态断言到 V066；CHECK 约束拒绝非法枚举。
- 状态机：注册 approve/reject、换机 supersede 链、一员工仅一 active（唯一索引冲突用例）、root→revoke。
- 隔离：商户 scope 只见自身；平台 scope 见全量；越权读他户被拒。
- `verify:matrix-vs-migration`：实现后三表 absent→present 触发闸口 → 更新 claims.json。
- `verify:p0-contract`、`verify:openapi` 绿；后端测试全绿。

## 9. 依赖与下游
- 依赖：`access_users`（生成角色）、`staff_levels`（V055 职级）。
- **解锁 T9**：Android App 注册/Keystore 指纹/Root 检测/换机解绑直接对接本三表与上述端点；MDM 自有侧字段本期暂缓（仅接口+开关占位）。

## 10. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T1 员工三表可编码规格（V066 DDL+状态机+Service+API+事件+权限+验收+测试） | R5 backlog T1；裁决⑨；DDL 约定取自 V011/V055 |
