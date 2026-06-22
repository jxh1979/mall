# Java 后端核心与平台地基开发设计

> 版本：V0.x ｜ 最后更新：2026-06-20 ｜ 覆盖迁移基线：见各小节迁移号（最新 V124）｜ 状态：现行
> 新功能承接见 docs/v099-v124-feature-implementation-and-acceptance-spec-v0.1.md
> <!-- 文档goalP3(2026-06-20)：补版本/日期/覆盖迁移头，未改正文 -->

本文档用于指导正式 Java 后端第一阶段编码。范围聚焦“平台地基”：多商户组织、账号权限、开放应用、安全鉴权、统一事件中心，以及这些能力和票务通票系统的基础衔接。

参考文档：

- `docs/system-development-spec.md`
- `docs/project-progress-dashboard.md`
- `docs/p0-coding-entry-pack-v0.1.md`
- `docs/foundation-systems-breakdown.md`
- `docs/foundation-database-design.md`
- `docs/system-development-spec.md`（正式技术栈入口）
- `docs/server-160-runbook-v0.2.md`
- `docs/ticketing-pass-entitlement-development-design.md`
- `backend/README.md`

## 1. 设计目标

第一阶段后端不是先把所有业务做完，而是先建立可持续扩展的平台底座：

- 所有商户、门店、区域、点位、设备、开放应用都能归属到清晰的组织结构。
- 平台员工、商户员工、第三方运营人员使用统一账号、角色、权限和数据范围模型。
- 第三方系统必须通过开放平台接入，具备动态鉴权、短时效 Token、防重放、连续三次失败锁定、人工解锁和调用审计能力。
- 所有关键动作统一写入事件中心，后续票务、酒吧、游乐、第三方、安卓端都复用同一套事件模型。
- 数据库迁移由 Flyway 版本化管理，禁止依赖 Hibernate 自动建表。
- 后端可以部署到 160 服务器的 `complex-api.service`。

## 2. 技术基线

| 层级 | 技术 | 第一阶段要求 |
|---|---|---|
| 运行时 | Java 21 | 160 服务器已安装 OpenJDK 21 |
| 框架 | Spring Boot 4.x | REST API、校验、配置、健康检查；P0 后端骨架锁定 Spring Boot 4.1.0，后续 4.x 补丁版本按安全和兼容性升级 |
| 安全 | Spring Security | 后台登录、接口鉴权、开放 API 网关校验 |
| 数据库 | PostgreSQL | 主业务库 |
| 迁移 | Flyway | 所有 DDL、种子权限、API 资源统一迁移 |
| ORM | Spring Data JPA | 第一阶段优先，复杂查询后续引入 QueryDSL 或 jOOQ |
| 缓存/锁 | Redis | Token、nonce、防重放、失败计数、限流、幂等 |
| 消息 | NATS | 第一阶段可先预留事件发布接口，P1 接入异步发布 |
| 对象存储 | MinIO | 证据附件、导出文件、视频证据索引引用 |
| API 文档 | springdoc-openapi | 自动生成管理端和开放 API 文档 |
| 构建 | Maven | P0 唯一后端构建工具，Gradle 不作为正式后端主线 |
| 部署 | systemd + Nginx | 服务名 `complex-api.service`，监听 `127.0.0.1:8080` |

> 已实现修复（finding F-CROSS-05，2026-06-22，已在 160 编译通过）：作为跨域通用能力的证据附件上传（`createAttachmentUploadIntent` 签发上传凭据、`completeAttachment` 确认上传），此前存在跨租户越权风险，现已加固为两点：
> 1. **object key 纳入商户 scope 前缀**：`createAttachmentUploadIntent` 生成的对象键统一以 `evidence/<merchantId 或 _platform>/...` 为前缀（平台级附件用 `_platform`），按租户隔离对象存储命名空间，防止跨商户路径覆盖。
> 2. **completeAttachment 使用签发态原 key**：确认上传时改用 upload-intent 签发并持久化的原始对象键（`existing.objectKey()`），不再信任客户端在 complete 请求中提交的 key，杜绝客户端指向/覆盖他商户存储对象的越权。
>
> 说明：本能力为平台地基对象存储（MinIO）之上的跨域附件原语，供风险处置（data-risk-ai §13.6 `risk_alert_actions.attachment_urls`）、财务凭证、移动端补录（android-device-terminal §14.4 `/api/mobile/attachments/presign`）等场景复用；上传凭据的接口契约以 P0 OpenAPI 契约为权威。

建议采用“模块化单体”作为第一阶段架构：一个 Spring Boot 应用内按业务域分包和分层，避免过早微服务化。后续票务、酒吧、游乐、第三方集成可以在同一应用内扩展模块，等业务边界稳定后再拆服务。

## 3. 工程结构

建议后端目录：

```text
backend
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── complex
│   │   │           └── platform
│   │   │               ├── ComplexApiApplication.java
│   │   │               ├── common
│   │   │               ├── config
│   │   │               ├── tenant
│   │   │               ├── identity
│   │   │               ├── access
│   │   │               ├── openplatform
│   │   │               ├── securityauth
│   │   │               ├── event
│   │   │               ├── device
│   │   │               └── audit
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-prod.yml
│   │       └── db
│   │           └── migration
│   └── test
│       └── java
│           └── com
│               └── complex
│                   └── platform
```

后端 P0 不保留 Gradle/Maven 双选项；所有 CI、部署脚本和 160 构建口径均以 Maven 为准。

## 4. 包结构与模块边界

### 4.1 `common`

通用基础能力，不包含具体业务规则。

职责：

- 统一响应结构。
- 错误码和异常处理。
- 分页请求和分页响应。
- 时间、ID、状态枚举基础类型。
- 请求上下文：当前用户、当前商户、数据范围、请求 ID。
- 通用审计字段基类。

建议对象：

- `ApiResponse<T>`
- `PageRequestDto`
- `PageResponse<T>`
- `BusinessException`
- `ErrorCode`
- `RequestContext`
- `BaseEntity`
- `AuditableEntity`

### 4.2 `tenant`

负责多组织、多商户和空间结构，是所有业务归属的根。

职责：

- 平台主体、组织、商户、服务商。
- 品牌、门店、场地。
- 区域、点位。
- 合同和合作关系基础信息。
- 商户状态管理：草稿、待审核、启用、暂停、终止。

不负责：

- 票务产品配置。
- 酒吧订单和库存。
- 游乐场次和排队。
- 第三方接口签名细节。

核心实体：

- `Organization`
- `Merchant`
- `ServiceProvider`
- `Brand`
- `Venue`
- `Area`
- `LocationPoint`
- `Contract`
- `MerchantCredential`
- `MerchantBankAccount`

### 4.3 `identity`

负责账号、员工档案、登录身份和会话。

职责：

- 系统账号。
- 员工档案。
- 平台员工、商户员工、第三方运营员工身份区分。
- 密码 Hash、登录失败、账号锁定。
- 企业微信身份绑定的基础映射。
- 登录会话。

核心实体：

- `UserAccount`
- `StaffProfile`
- `LoginSession`
- `WecomBinding`

### 4.4 `access`

负责角色权限、菜单权限、功能权限和数据范围。

职责：

- 角色管理。
- 权限点管理。
- 角色授权。
- 用户角色绑定。
- 数据范围规则。
- 临时授权。
- 权限模板、模板复制、模板应用和模板使用日志。
- 岗位、班次和当班权限。

核心实体：

- `Role`
- `Permission`
- `RolePermission`
- `UserRole`
- `DataScope`
- `Position`
- `Shift`
- `StaffShiftAssignment`
- `TemporaryGrant`
- `PermissionTemplate`
- `PermissionTemplateItem`
- `PermissionTemplateApplication`
- `PermissionTemplateUsageLog`
- `TemporaryGrant`

### 4.5 `openplatform`

负责第三方开放应用、API 目录、文档、SDK、Webhook。

职责：

- 开放应用管理。
- AppKey、凭证版本、密钥轮换。
- API 资源目录。
- API 版本。
- API 授权范围。
- 第三方调用日志。
- Webhook 订阅和投递日志。
- OpenAPI 文档分组。

核心实体：

- `OpenApp`
- `OpenAppCredential`
- `ApiResource`
- `ApiVersion`
- `ApiPermission`
- `OpenAppPermission`
- `ApiCallLog`
- `WebhookSubscription`
- `WebhookDeliveryLog`

### 4.6 `securityauth`

负责开放平台动态鉴权和调用校验。该模块只处理安全规则，不直接处理业务对象。

职责：

- 第三方账号密码校验。
- HMAC 签名校验。
- timestamp 校验。
- nonce 防重放。
- 短时效 access token 签发。
- 动态调用地址签发和校验。
- 连续三次失败锁定。
- 人工解锁。
- 鉴权事件写入。

核心实体：

- `AuthSession`
- `AccessToken`
- `DynamicEndpoint`
- `NonceRecord`
- `AuthFailureRecord`
- `AppLockRecord`
- `ManualUnlockRecord`

Redis 关键键建议：

```text
openauth:token:{token_hash}
openauth:nonce:{app_id}:{nonce}
openauth:fail-count:{app_id}
openauth:dynamic-endpoint:{endpoint_key}
ratelimit:openapi:{app_id}:{api_code}
idempotency:{scope}:{key}
```

### 4.7 `event`

负责统一事件中心，是所有业务留痕的公共入口。

职责：

- 标准事件写入。
- 原始 payload 保存。
- 事件关联。
- 时间线查询。
- 视频证据索引。
- 风险标记。
- 人工补录。
- 物品追踪码和操作交接事件。
- 业务过程实例和步骤留痕，用于订单获取、销售归属、卡座服务、异常首响和财务调整。
- 服务责任记录，用于追溯销售、服务员、异常第一处理人和责任变更。
- 后续向 NATS 发布事件。

核心实体：

- `AuditEvent`
- `EventRawPayload`
- `EventRelation`
- `VideoEvidence`
- `EventRiskMark`
- `ManualRecord`
- `InventoryTraceCode`
- `OperationHandoverRecord`
- `BusinessProcessInstance`
- `BusinessProcessStepRecord`
- `ServiceResponsibilityRecord`

### 4.8 `device`

负责设备基础档案和绑定关系。第一阶段只做最小底座，给安卓手机、平板、一体机、扫码/NFC/闸机设备预留。

职责：

- 设备注册。
- 设备归属商户、门店、区域、点位。
- 设备类型和状态。
- 员工领用、归还、绑定记录。
- 终端能力开关，控制《统一口径基准》D5 的 10 端是否开放：客人 App（`app`）、小程序（`miniapp`）、Web 前端（`web`）、工作人员平板（`tablet`）、员工工作手机（`staff_work_phone`）、员工私人手机 App（`staff_private_phone`）、商户侧 Web 门户（`merchant_portal`）、商户侧 App（`merchant_app`）、触摸屏一体机（`kiosk`）、移动管理端（`mobile_admin`）。
  > 修订说明（2026-06-07 收口）：原文用"员工端、商户侧"笼统词，既合并了工作手机/私人手机、又漏移动管理端、未拆商户 portal/app。按 D5"不得用员工端笼统合并工作手机/私人手机；不得漏移动管理端"改为对齐 D5 十端权威码（与 `operation-traceability §6.3`、`foundation-database §6.9`、`platform-governance 字段字典 §3.1` 一字一致）。依据 finding baseline-conf-6、D5。

核心实体：

- `Device`
- `DeviceBinding`
- `StaffDeviceLink`
- `TerminalCapabilitySwitch`

#### 4.8.1 员工设备凭证现状与目标（D11，`staff_device_credentials` 后端承接缺失）

> 现状（2026-06-18 审计）：私人手机 App 自助注册→审批、`staff_device_credentials`（Keystore 设备公钥 / 不可逆设备指纹 / 换机解绑 / Root 失效）的**后端实现设计缺失**——数据库侧已有/规划字段，但本 Java 设计无对应实体、Service、Controller 承接（DB 有字段、Java 设计无承接）。需说明：现存 `terminal_device_credentials`(V011) 是 **kiosk 一体机随机 token 的 SHA-256**，与员工私人手机设备指纹是**不同实体**，不能当作"员工设备凭证已覆盖"（详见 `android-device-terminal-development-design` R5 审计标注与 §3.1/§13.1）。两套凭证互不串用。

**现状**

- `staff_device_credentials`（员工私人手机设备凭证：Keystore 公钥指纹/不可逆指纹、换机解绑重绑、Root/调试失效）在 Java 后端无实体、无 Repository、无 Service、无 Controller。
- 私人手机"自助注册 → 平台审批 → 凭证绑定生效"流程在后端无设计承接。
- 换机的"解绑旧 → 绑新（待审）→ 生效"中间态（含 supersedes / 换机待审）无状态机定义。

**目标（P0）**

- 在 `device` 模块新增员工设备凭证后端设计，至少包含：
  - 实体：`StaffDeviceCredential`（字段：`staff_id`、`user_id`、`device_key_fingerprint`(不可逆公钥指纹)、`credential_status`、`registration_status`(审批态)、`root_debug_state`、`bound_at`、`unbound_at`、`superseded_by`(换机指向)、仅持久化不可逆哈希/公钥指纹，不存原始标识，PIPL 最小化，对齐合规 C4）。
  - 状态机：`pending_review`(自助注册待审) → `active`(绑定生效) → `superseded`(换机被取代) / `revoked`(解绑/离职注销) / `invalidated`(Root/调试/指纹 mismatch 失效待复核)。
  - 换机：先解绑旧凭证（置 `revoked`/`superseded`）再绑新凭证（新凭证 `pending_review` → 审批 → `active`），中间保留"换机待审"态，不允许新旧同时 `active`。
  - 失效：Root、调试、设备凭证/指纹变化或不匹配时凭证立即 `invalidated` 并进入后台复核。
  - Service/Controller：自助注册提交、审批通过/驳回、凭证绑定、换机解绑重绑、失效与复核；与 `identity`/`access` 准入校验衔接（私人手机仅用于注册、通知、状态查看、凭证绑定，不承担现场业务）。
  - 事件留痕：注册提交、审批、绑定、换机解绑重绑、失效复核全部写 `audit_events`。
- 明确 `staff_device_credentials`（员工）与 `terminal_device_credentials`（kiosk 一体机）两套凭证表/实体互不串用。

**候选验收**

- 私人手机可完成"自助注册 → 平台审批 → 凭证绑定生效"，并能在后台查到员工/私人手机/设备公钥指纹/工作手机一一对应关系。
- 换机走"解绑旧 → 绑新（待审）→ 生效"闭环，且任一时刻同一员工私人手机只有一条 `active` 凭证；旧凭证解绑即删除对应绑定凭据。
- Root/调试/指纹 mismatch 设备绑定立即失效并进入复核，复核确认后封禁，未通过准入不能进入现场业务。
- 仅持久化不可逆哈希/公钥指纹，不存原始唯一标识（PIPL 最小化校验通过）。
- 员工设备凭证与 kiosk 一体机凭证（`terminal_device_credentials`）两套互不串用，校验路径分离。

> 注：本节为后端承接设计补登；私人手机端的产品形态、PIPL 告知同意/最小化/解绑删除、Android 端注册/Keystore 指纹/Root 检测细节以 `android-device-terminal-development-design §3.1/§13.1/§13.1.1` 为准，本节负责其后端实体、状态机、Service/Controller 与事件落点。

### 4.9 `audit`

负责后台操作审计的公共切面和查询入口。事件中心记录业务事件，`audit` 模块负责把后台敏感操作统一拦截并写入事件中心。

职责：

- 创建、修改、删除操作留痕。
- 敏感数据查看留痕。
- 导出操作留痕。
- 人工解锁、授权变更留痕。

## 5. 分层规范

每个业务模块内部建议保持一致结构：

```text
tenant
├── controller
├── dto
├── entity
├── enums
├── repository
├── service
├── policy
└── mapper
```

分层职责：

| 层 | 职责 |
|---|---|
| Controller | 接收 HTTP 请求、参数校验、返回 DTO，不写业务规则 |
| DTO | 请求和响应对象，不直接暴露 Entity |
| Service | 编排业务流程、事务、事件写入 |
| Policy | 放权限判断、状态流转判断、数据范围判断 |
| Repository | 数据访问，不拼业务流程 |
| Entity | 数据库实体和基础领域行为 |
| Mapper | Entity 与 DTO 转换 |

强约束：

- Controller 不直接调用 Repository。
- Service 不返回 JPA Entity 给前端。
- 所有写操作必须经过 Service。
- 所有跨商户查询必须显式携带数据范围条件。
- 所有关键写操作必须写 `AuditEvent`。

## 6. PostgreSQL 与 Flyway 迁移分层

第一阶段迁移建议：

```text
V001__platform_foundation_core.sql
V002__identity_access_control.sql
V003__open_platform_auth.sql
V004__audit_event_center.sql
V005__seed_permissions_and_api_resources.sql
V006__device_foundation.sql
V010__ticketing_foundation_reference.sql
```

### 6.1 `V001__platform_foundation_core.sql`

建表范围：

- `organizations`
- `merchants`
- `service_providers`
- `brands`
- `venues`
- `areas`
- `location_points`
- `contracts`
- `merchant_credentials`
- `merchant_bank_accounts`

关键约束：

- `merchants.organization_id` 关联 `organizations.id`。
- `venues.merchant_id` 关联 `merchants.id`。
- `areas.venue_id` 关联 `venues.id`。
- `location_points.area_id` 关联 `areas.id`。
- 商户编码、门店编码、点位编码在租户范围内唯一。

### 6.2 `V002__identity_access_control.sql`

建表范围：

- `user_accounts`
- `staff_profiles`
- `roles`
- `permissions`
- `role_permissions`
- `user_roles`
- `data_scopes`
- `positions`
- `shifts`
- `staff_shift_assignments`
- `wecom_bindings`
- `temporary_grants`
- `permission_templates`
- `permission_template_items`
- `permission_template_applications`
- `permission_template_usage_logs`
- `login_sessions`

关键约束：

- 平台用户可无 `merchant_id`，商户用户必须有 `merchant_id`。
- `permissions.code` 全局唯一。
- `roles.code` 在 `merchant_id` 范围内唯一。
- `temporary_grants` 必须有有效期。
- `permission_templates.template_code` 在主体范围内唯一。
- 临时权限模板必须有失效时间或单次有效规则；模板复制只复制权限快照，不随源模板后续变更自动变化。

### 6.3 `V003__open_platform_auth.sql`

建表范围：

- `open_apps`
- `open_app_credentials`
- `api_resources`
- `api_versions`
- `api_permissions`
- `open_app_permissions`
- `auth_sessions`
- `access_tokens`
- `dynamic_endpoints`
- `nonce_records`
- `auth_failure_records`
- `app_lock_records`
- `manual_unlock_records`
- `api_call_logs`

> D8 Webhook 表优先级收口：`webhook_subscriptions`、`webhook_delivery_logs` 归 **P1**（与 `open-platform-security-auth §12.2` P1 表清单及 §18.2 "Webhook 订阅和投递"P1 任务一致）。本文 §4.5 实体清单、§8.5 后台接口、§19 P1 顺序中的 Webhook 能力均按 P1 落地，不进 V003（P0）迁移；P1 实施时新增独立迁移版本建表。原 V003 误列这两张表已据此移除。

关键约束：

- `open_apps.app_code` 全局唯一。
- 凭证明文永不入库，只保存 `credential_hash`、`secret_hash`、`secret_last4`。
- `access_tokens.token_hash` 唯一。
- `dynamic_endpoints.endpoint_key` 唯一。
- `nonce_records` 对 `app_id + nonce` 唯一。
- `api_call_logs` 需要建立 `app_id + called_at`、`request_id` 索引。

### 6.4 `V004__audit_event_center.sql`

建表范围：

- `audit_events`
- `event_raw_payloads`
- `event_relations`
- `video_evidences`
- `event_risk_marks`
- `manual_records`
- `inventory_trace_codes`
- `operation_handover_records`
- `terminal_capability_switches`
- `business_process_instances`
- `business_process_step_records`
- `service_responsibility_records`

关键约束：

- `audit_events.event_id` 全局唯一。
- `audit_events.event_type + occurred_at` 建索引。
- `audit_events.merchant_id + occurred_at` 建索引。
- `audit_events.subject_type + subject_id` 建索引。
- `inventory_trace_codes.trace_code` 全局唯一。
- `operation_handover_records.trace_code_id + occurred_at` 建索引。
- `terminal_capability_switches.scope_type + scope_id + terminal_type + business_system + module_code` 建索引。
- `business_process_instances.process_type + business_object_type + business_object_id` 建索引。
- `business_process_step_records.process_instance_id + occurred_at` 建索引。
- `service_responsibility_records.order_id + table_id + status` 建索引。
- 原始 payload 使用 `jsonb`。

### 6.5 `V005__seed_permissions_and_api_resources.sql`

种子数据范围：

- 平台超级管理员角色。
- 平台运营角色。
- 商户管理员角色。
- 第三方应用管理员角色。
- 第一批权限点。
- 第一批开放 API 资源。

权限编码建议：

> 修订说明（2026-06-07）：依据《统一口径基准》D3（devtech-2），开放平台权限编码采用点号风格 `domain.resource.action`，与 `open-platform-security-auth` 一致。**开放平台权限点以 `open-platform-security-auth-development-design` 为准**：该文档已把 `openplatform.app.manage` 细分为 `openplatform.app.create / update / disable / view / rotate_secret`，并将解锁定为 `openplatform.auth.unlock`、发布 API 定为 `openplatform.api.publish`。下方种子保留 `openplatform.app.manage` 作为"开放应用管理"粗粒度角色权限聚合（可由角色映射到上述细分点），细分点的最终清单以 open-platform 文档 §3 为权威。

```text
tenant.organization.manage
tenant.merchant.manage
tenant.venue.manage
identity.user.manage
access.role.manage
openplatform.app.manage   # 粗粒度聚合，细分点（app.create/update/disable/view/rotate_secret）以 open-platform-security-auth §3 为准
openplatform.auth.unlock
openplatform.calllog.view
event.audit.view
event.manual_record.create
```

API 资源编码建议：

```text
open.auth.issue_token
open.auth.refresh_endpoint
open.ticket.query
open.ticket.verify
open.event.write
open.webhook.subscribe
```

### 6.6 `V006__device_foundation.sql`

建表范围：

- `devices`
- `device_bindings`
- `staff_device_credentials`（员工私人手机设备凭证：Keystore 公钥指纹/不可逆指纹、注册审批态、换机解绑重绑、Root/调试失效；D11，详见 §4.8.1）

> D11 现状说明：`staff_device_credentials` 后端实体/状态机/Service/Controller 设计原缺失（DB 有字段、Java 设计无承接），§4.8.1 已补"现状→目标(P0)→候选验收"。本表与 kiosk 一体机的 `terminal_device_credentials`(V011) 是不同实体，互不串用。

用于后续安卓员工手机、平板、一体机、扫码/NFC设备接入。

### 6.7 `V010__ticketing_foundation_reference.sql`

不在本文档内实现票务全部表，只放平台地基和票务的引用准备：

- 票务表必须引用 `merchant_id`、`venue_id`、`area_id`、`location_point_id`。
- 票务核销事件必须写入 `audit_events`。
- 票务开放 API 必须注册到 `api_resources`。

## 7. 通用数据库字段规范

所有主表建议包含：

```text
id uuid primary key
tenant_id uuid null
merchant_id uuid null
status varchar(32) not null
created_at timestamptz not null
created_by uuid null
updated_at timestamptz not null
updated_by uuid null
deleted_at timestamptz null
version bigint not null default 0
metadata jsonb null
```

规则：

> 修订说明（2026-06-07）：依据《统一口径基准》D1（devtech-3、fielddb-7），全局金额一律改为**整数分（bigint）**，禁止 `numeric(18,2)` 元；所有金额字段须标注"单位：分(整数)"。例如原价 12.50 元存为 `1250`。本规则覆盖本文所有主表及后续票务、酒吧、财务等模块金额字段。

- 主键使用 UUID。
- 金额统一使用 **`bigint`，单位：分(整数)**（全局金额=分，禁止 `numeric(18,2)` 元）。
- 数量可用 `numeric(18,4)`。
- 时间统一使用 `timestamptz`。
- 状态统一使用字符串枚举，便于读库和审计。
- 软删除只用于管理类对象；事件、日志、流水不允许软删除。
- JSONB 只能存扩展字段，不能代替核心字段。

## 8. 早期 REST 能力草案（非 P0 正式路径）

> 2026-06-11 执行口径，2026-06-13 追溯闸口补强：本章 API 表为早期 Java 后端分层草案，用于理解后台、开放平台、事件、终端开关等能力边界，不再作为 P0 正式路径清单。P0 编码、联调和验收必须以 `docs/openapi/openapi-p0.yaml`、`docs/openapi/openapi-p0.json`、`p0-openapi-operation-acceptance-matrix-v0.1.md`、`p0-openapi-contract-closure-appendix-v0.1.md`、`p0-operation-permission-matrix-v0.1.md` 和 `code-contract-difference-and-migration-audit-v0.1.md` 为准；历史 `/api/admin/*`、`/api/terminal/*`、旧权限码和旧 DTO 命名不得直接进入 Controller。若确需保留某项后台/开放平台/事件/终端能力，必须先补正式 OpenAPI operation、operation 验收矩阵和追溯附录，并通过 `npm run verify:openapi-closure`。

早期后台接口草案曾使用默认前缀（仅用于识别旧路径，不得继续作为正式 Controller 前缀）：

```text
/api/admin
```

早期开放平台第三方接口草案曾使用默认前缀（正式开放平台路径以 `docs/openapi/openapi-p0.yaml/json` 为准）：

```text
/api/open
```

### 8.1 健康检查

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/health` | 服务健康检查 | 无 |
| GET | `/api/admin/health/dependencies` | 数据库、Redis、NATS、MinIO 检查 | `system.health.view` |

### 8.2 多商户与组织

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/admin/organizations` | 创建组织 | `tenant.organization.manage` |
| GET | `/api/admin/organizations` | 查询组织列表 | `tenant.organization.view` |
| GET | `/api/admin/organizations/{id}` | 查询组织详情 | `tenant.organization.view` |
| PUT | `/api/admin/organizations/{id}` | 更新组织 | `tenant.organization.manage` |
| POST | `/api/admin/merchants` | 创建商户 | `tenant.merchant.manage` |
| GET | `/api/admin/merchants` | 查询商户列表 | `tenant.merchant.view` |
| GET | `/api/admin/merchants/{id}` | 查询商户详情 | `tenant.merchant.view` |
| PUT | `/api/admin/merchants/{id}` | 更新商户 | `tenant.merchant.manage` |
| POST | `/api/admin/merchants/{id}/activate` | 启用商户 | `tenant.merchant.manage` |
| POST | `/api/admin/merchants/{id}/suspend` | 暂停商户 | `tenant.merchant.manage` |
| POST | `/api/admin/merchants/{id}/terminate` | 终止商户 | `tenant.merchant.manage` |
| POST | `/api/admin/service-providers` | 创建服务商 | `tenant.service_provider.manage` |
| GET | `/api/admin/service-providers` | 查询服务商 | `tenant.service_provider.view` |
| POST | `/api/admin/brands` | 创建品牌 | `tenant.brand.manage` |
| GET | `/api/admin/brands` | 查询品牌 | `tenant.brand.view` |
| POST | `/api/admin/venues` | 创建门店/场地 | `tenant.venue.manage` |
| GET | `/api/admin/venues` | 查询门店/场地 | `tenant.venue.view` |
| GET | `/api/admin/venues/{id}` | 查询门店/场地详情 | `tenant.venue.view` |
| PUT | `/api/admin/venues/{id}` | 更新门店/场地 | `tenant.venue.manage` |
| POST | `/api/admin/areas` | 创建区域 | `tenant.area.manage` |
| GET | `/api/admin/areas` | 查询区域 | `tenant.area.view` |
| POST | `/api/admin/location-points` | 创建点位 | `tenant.location_point.manage` |
| GET | `/api/admin/location-points` | 查询点位 | `tenant.location_point.view` |

### 8.3 账号、员工与登录

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/admin/auth/login` | 后台登录 | 无 |
| POST | `/api/admin/auth/refresh` | 刷新登录令牌 | 登录态 |
| POST | `/api/admin/auth/logout` | 退出登录 | 登录态 |
| GET | `/api/admin/auth/me` | 当前用户信息 | 登录态 |
| POST | `/api/admin/users` | 创建账号 | `identity.user.manage` |
| GET | `/api/admin/users` | 查询账号 | `identity.user.view` |
| GET | `/api/admin/users/{id}` | 查询账号详情 | `identity.user.view` |
| PUT | `/api/admin/users/{id}` | 更新账号 | `identity.user.manage` |
| POST | `/api/admin/users/{id}/disable` | 禁用账号 | `identity.user.manage` |
| POST | `/api/admin/users/{id}/enable` | 启用账号 | `identity.user.manage` |
| POST | `/api/admin/users/{id}/reset-password` | 重置密码 | `identity.user.manage` |
| POST | `/api/admin/staff` | 创建员工档案 | `identity.staff.manage` |
| GET | `/api/admin/staff` | 查询员工档案 | `identity.staff.view` |
| POST | `/api/admin/wecom-bindings` | 绑定企业微信身份 | `identity.wecom.manage` |
| DELETE | `/api/admin/wecom-bindings/{id}` | 解除企业微信绑定 | `identity.wecom.manage` |

### 8.4 角色、权限与数据范围

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/admin/roles` | 创建角色 | `access.role.manage` |
| GET | `/api/admin/roles` | 查询角色 | `access.role.view` |
| GET | `/api/admin/roles/{id}` | 查询角色详情 | `access.role.view` |
| PUT | `/api/admin/roles/{id}` | 更新角色 | `access.role.manage` |
| POST | `/api/admin/roles/{id}/permissions` | 设置角色权限 | `access.role.manage` |
| GET | `/api/admin/permissions` | 查询权限点 | `access.permission.view` |
| GET | `/api/admin/permission-templates` | 查询权限模板 | `access.permission_template.view` |
| POST | `/api/admin/permission-templates` | 创建基础/临时权限模板 | `access.permission_template.manage` |
| PUT | `/api/admin/permission-templates/{id}` | 更新权限模板 | `access.permission_template.manage` |
| POST | `/api/admin/permission-templates/{id}/copy` | 复制模板为新模板 | `access.permission_template.manage` |
| POST | `/api/admin/permission-templates/{id}/permissions` | 设置模板权限项、数据范围和显式拒绝 | `access.permission_template.manage` |
| POST | `/api/admin/permission-templates/{id}/apply` | 应用模板到员工/岗位/职级/角色 | `access.permission_template.apply` |
| POST | `/api/admin/permission-template-applications/{id}/revoke` | 撤销模板应用 | `access.permission_template.revoke` |
| POST | `/api/admin/users/{id}/roles` | 设置用户角色 | `access.user_role.manage` |
| POST | `/api/admin/data-scopes` | 创建数据范围 | `access.data_scope.manage` |
| GET | `/api/admin/data-scopes` | 查询数据范围 | `access.data_scope.view` |
| POST | `/api/admin/temporary-grants` | 创建临时授权 | `access.temporary_grant.manage` |
| POST | `/api/admin/temporary-grants/{id}/revoke` | 撤销临时授权 | `access.temporary_grant.manage` |
| POST | `/api/admin/positions` | 创建岗位 | `access.position.manage` |
| POST | `/api/admin/shifts` | 创建班次 | `access.shift.manage` |
| POST | `/api/admin/staff-shift-assignments` | 员工排班/上班授权 | `access.shift.manage` |
| POST | `/api/admin/staff-shift-assignments/{id}/close` | 下班回收权限 | `access.shift.manage` |

### 8.5 开放应用

> 修订说明（2026-06-07）：依据《统一口径基准》D3（devtech-2），本表权限点沿用点号风格。其中 `openplatform.app.manage` 为粗粒度聚合写法，**实际细分权限点（创建/更新/停用/查看/轮换密钥 → `openplatform.app.create/update/disable/view/rotate_secret`）以 `open-platform-security-auth-development-design` §3 为准**；编码落地时按 open-platform 文档的细分点校验。

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/admin/open-apps` | 创建开放应用 | `openplatform.app.manage` |
| GET | `/api/admin/open-apps` | 查询开放应用 | `openplatform.app.view` |
| GET | `/api/admin/open-apps/{id}` | 查询开放应用详情 | `openplatform.app.view` |
| PUT | `/api/admin/open-apps/{id}` | 更新开放应用 | `openplatform.app.manage` |
| POST | `/api/admin/open-apps/{id}/enable` | 启用开放应用 | `openplatform.app.manage` |
| POST | `/api/admin/open-apps/{id}/disable` | 停用开放应用 | `openplatform.app.manage` |
| POST | `/api/admin/open-apps/{id}/credentials` | 创建或轮换凭证 | `openplatform.credential.manage` |
| GET | `/api/admin/open-apps/{id}/credentials` | 查询凭证版本 | `openplatform.credential.view` |
| POST | `/api/admin/open-apps/{id}/permissions` | 设置开放 API 权限 | `openplatform.permission.manage` |
| POST | `/api/admin/api-resources` | 创建 API 资源 | `openplatform.api_resource.manage` |
| GET | `/api/admin/api-resources` | 查询 API 资源 | `openplatform.api_resource.view` |
| POST | `/api/admin/webhook-subscriptions` | 创建 Webhook 订阅 | `openplatform.webhook.manage` |
| GET | `/api/admin/webhook-subscriptions` | 查询 Webhook 订阅 | `openplatform.webhook.view` |

### 8.6 开放平台动态鉴权

> 修订说明（2026-06-07）：依据《统一口径基准》D3（devtech-1），本节对外路径 `/api/open/auth/token`、`/api/open/auth/refresh-endpoint` 及 §13.3 动态网关 `/api/open/d/{endpoint_key}/...` 已与 D3 规范一致，予以保留；`open-platform-security-auth` 文档的对外路径已据此对齐到同一前缀。

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/open/auth/token` | 第三方获取短时效 Token 和动态调用地址 | 开放应用凭证 |
| POST | `/api/open/auth/refresh-endpoint` | 刷新动态调用地址 | 开放应用 Token |
| POST | `/api/open/auth/revoke` | 主动吊销当前 Token | 开放应用 Token |
| GET | `/api/admin/open-auth/sessions` | 查询鉴权会话 | `openplatform.auth.view` |
| GET | `/api/admin/open-auth/failures` | 查询失败记录 | `openplatform.auth.view` |
| GET | `/api/admin/open-auth/locks` | 查询锁定记录 | `openplatform.auth.view` |
| POST | `/api/admin/open-apps/{id}/unlock` | 人工解锁开放应用 | `openplatform.auth.unlock` |
| GET | `/api/admin/open-api/call-logs` | 查询 API 调用日志 | `openplatform.calllog.view` |

开放鉴权请求必须包含：

```text
app_code
login_account
timestamp
nonce
signature
password 或 password_digest
```

> D8 字段名收口（鉴权请求标识字段）：本文原用 `credential_id`，`open-platform-security-auth` 用 `loginAccount`/`login_account`（且 `open_app_credentials.login_account` 已是数据库列名）。两文件统一为 **`login_account`**（请求体 JSON 字段名 `loginAccount`，DB 列名 `login_account`），以与 open 文档 §6.2/§12.4 和数据库列名对齐；`credential_id` 不再作为鉴权请求标识字段。

签名建议：

```text
HMAC-SHA256(secret, method + path + timestamp + nonce + body_sha256)
```

返回：

```text
access_token
expires_at
dynamic_endpoint
endpoint_expires_at
allowed_api_codes
server_time
```

### 8.7 事件中心

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/admin/events` | 管理端写入事件 | `event.audit.write` |
| POST | `/api/open/events` | 第三方写入标准事件 | 开放应用 Token + `open.event.write` |
| GET | `/api/admin/events` | 查询事件列表 | `event.audit.view` |
| GET | `/api/admin/events/{id}` | 查询事件详情 | `event.audit.view` |
| GET | `/api/admin/events/timeline` | 查询对象时间线 | `event.audit.view` |
| POST | `/api/admin/events/{id}/relations` | 关联事件 | `event.audit.manage` |
| POST | `/api/admin/events/{id}/risk-marks` | 标记风险 | `event.risk.manage` |
| POST | `/api/admin/manual-records` | 人工补录 | `event.manual_record.create` |
| GET | `/api/admin/manual-records` | 查询人工补录 | `event.manual_record.view` |
| POST | `/api/work/inventory-trace-codes/{code}/scan` | 现场扫码记录物品状态或交接 | `inventory.trace.scan` |
| GET | `/api/admin/inventory-trace-codes/{code}/timeline` | 查询单瓶酒或物品全过程 | `event.audit.view` |
| GET | `/api/admin/operation-handover-records` | 查询操作交接记录 | `event.audit.view` |
| POST | `/api/events/process-step` | 各业务模块写入业务过程步骤留痕 | 登录态或服务内部凭证 |
| GET | `/api/admin/business-processes/{id}/timeline` | 查询订单、服务、异常等业务过程时间线 | `event.audit.view` |
| GET | `/api/admin/service-responsibilities` | 查询订单、卡座、客户、异常的责任链 | `event.audit.view` |
| POST | `/api/admin/exceptions/{id}/first-handle` | 异常第一处理人接单并留痕 | `event.exception.handle` |
| POST | `/api/admin/events/{id}/video-candidates` | 为事件生成或查询候选视频片段索引 | `event.video.view` |

### 8.8 终端能力开关

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/terminal/bootstrap-capabilities` | 终端启动时查询可用能力 | 登录态或设备凭证 |
| GET | `/api/admin/terminal-switches` | 查询终端开关 | `terminal.switch.view` |
| POST | `/api/admin/terminal-switches` | 新建终端开关 | `terminal.switch.manage` |
| PUT | `/api/admin/terminal-switches/{id}` | 修改终端开关 | `terminal.switch.manage` |
| POST | `/api/admin/terminal-switches/{id}/enable` | 启用终端或模块 | `terminal.switch.manage` |
| POST | `/api/admin/terminal-switches/{id}/disable` | 停用终端或模块 | `terminal.switch.manage` |

## 9. DTO 建议

### 9.1 通用 DTO

- `IdResponse`
- `PageQueryRequest`
- `PageResultResponse<T>`
- `StatusChangeRequest`
- `AuditTrailResponse`
- `OptionItemResponse`

### 9.2 多商户 DTO

- `CreateOrganizationRequest`
- `UpdateOrganizationRequest`
- `OrganizationListItemResponse`
- `OrganizationDetailResponse`
- `CreateMerchantRequest`
- `UpdateMerchantRequest`
- `MerchantListQueryRequest`
- `MerchantListItemResponse`
- `MerchantDetailResponse`
- `MerchantStatusChangeRequest`
- `CreateVenueRequest`
- `VenueDetailResponse`
- `CreateAreaRequest`
- `CreateLocationPointRequest`

### 9.3 账号权限 DTO

- `AdminLoginRequest`
- `AdminLoginResponse`
- `RefreshTokenRequest`
- `CurrentUserResponse`
- `CreateUserAccountRequest`
- `UpdateUserAccountRequest`
- `UserAccountDetailResponse`
- `CreateStaffProfileRequest`
- `CreateRoleRequest`
- `UpdateRoleRequest`
- `RoleDetailResponse`
- `SetRolePermissionsRequest`
- `SetUserRolesRequest`
- `CreateDataScopeRequest`
- `CreateTemporaryGrantRequest`
- `CreateStaffShiftAssignmentRequest`

### 9.4 开放平台 DTO

- `CreateOpenAppRequest`
- `UpdateOpenAppRequest`
- `OpenAppDetailResponse`
- `CreateOpenAppCredentialRequest`
- `OpenAppCredentialCreatedResponse`
- `SetOpenAppPermissionsRequest`
- `CreateApiResourceRequest`
- `CreateWebhookSubscriptionRequest`
- `OpenAuthRequest`
- `OpenAuthResponse`
- `RefreshDynamicEndpointRequest`
- `RefreshDynamicEndpointResponse`
- `ManualUnlockOpenAppRequest`
- `ApiCallLogQueryRequest`
- `ApiCallLogResponse`

### 9.5 事件中心 DTO

- `CreateAuditEventRequest`
- `AuditEventListQueryRequest`
- `AuditEventListItemResponse`
- `AuditEventDetailResponse`
- `CreateEventRelationRequest`
- `CreateEventRiskMarkRequest`
- `CreateManualRecordRequest`
- `EventTimelineQueryRequest`
- `EventTimelineResponse`
- `CreateBusinessProcessRequest`
- `AppendProcessStepRequest`
- `BusinessProcessTimelineResponse`
- `ServiceResponsibilityQueryRequest`
- `ServiceResponsibilityResponse`
- `FirstHandleExceptionRequest`
- `EventVideoCandidateResponse`

## 10. Service 建议

### 10.1 多商户

- `OrganizationService`
- `MerchantService`
- `ServiceProviderService`
- `BrandService`
- `VenueService`
- `AreaService`
- `LocationPointService`
- `ContractService`

关键方法：

```text
createMerchant
updateMerchant
activateMerchant
suspendMerchant
terminateMerchant
ensureMerchantAccessible
resolveMerchantTree
```

### 10.2 账号权限

- `AdminAuthService`
- `UserAccountService`
- `StaffProfileService`
- `RoleService`
- `PermissionService`
- `DataScopeService`
- `ShiftPermissionService`
- `TemporaryGrantService`

关键方法：

```text
login
refreshToken
assignRoles
setRolePermissions
resolveEffectivePermissions
resolveEffectiveDataScope
openShiftAccess
closeShiftAccess
```

### 10.3 开放平台与安全鉴权

- `OpenAppService`
- `OpenAppCredentialService`
- `ApiResourceService`
- `OpenAppPermissionService`
- `OpenAuthService`
- `OpenTokenService`
- `DynamicEndpointService`
- `NonceService`
- `AuthFailureLockService`
- `ApiCallLogService`
- `WebhookSubscriptionService`

关键方法：

```text
issueToken
verifySignature
checkNonce
recordAuthFailure
lockAppIfNecessary
manualUnlock
validateOpenApiCall
recordApiCall
refreshDynamicEndpoint
```

### 10.4 事件中心

- `AuditEventService`
- `EventRawPayloadService`
- `EventRelationService`
- `EventTimelineService`
- `EventRiskMarkService`
- `ManualRecordService`
- `BusinessProcessService`
- `BusinessProcessStepService`
- `ServiceResponsibilityService`
- `EventVideoCandidateService`
- `EventPublisher`

关键方法：

```text
appendEvent
appendRawPayload
linkEvents
queryTimeline
markRisk
createManualRecord
startProcess
appendProcessStep
assignServiceResponsibility
recordFirstHandler
matchVideoCandidates
publishAfterCommit
```

## 11. Repository 建议

第一阶段使用 Spring Data JPA Repository。命名规则：

- `MerchantRepository`
- `UserAccountRepository`
- `RoleRepository`
- `OpenAppRepository`
- `AccessTokenRepository`
- `AuditEventRepository`

复杂查询处理建议：

- 简单列表：Spring Data JPA 方法或 `Specification`。
- 多条件审计查询：优先 `Specification`。
- 大型统计、报表、结算：后续引入 jOOQ 或 QueryDSL。

禁止：

- Controller 拼 SQL。
- 使用 EntityManager 在 Controller 内直接查询。
- 为了省事把所有查询写成一个“大 Repository”。

## 12. 领域对象与状态建议

### 12.1 商户状态

```text
DRAFT
PENDING_REVIEW
ACTIVE
SUSPENDED
TERMINATED
```

状态流转：

```text
DRAFT -> PENDING_REVIEW -> ACTIVE -> SUSPENDED -> ACTIVE
ACTIVE -> TERMINATED
SUSPENDED -> TERMINATED
```

### 12.2 用户状态

```text
ACTIVE
DISABLED
LOCKED
LEFT
```

### 12.3 开放应用状态

> 修订说明（2026-06-07）：依据《统一口径基准》D3（devtech-1/7），**开放应用状态机以 `open-platform-security-auth §5.1` 的 7 态为唯一权威**：`draft / testing / active / suspended / locked / disabled / revoked`。本文原 5 态（缺 `testing` 联调中、`suspended` 平台暂停、`revoked` 吊销，且多出无对应流转的 `EXPIRED`）须对齐为下方 7 态；`foundation-database open_apps.status` 同样与之对齐。大小写以数据库小写枚举为准。

```text
draft       # 草稿
testing     # 联调中
active      # 正常可用
suspended   # 平台暂停
locked      # 鉴权失败锁定
disabled    # 停用
revoked     # 吊销，不可恢复
```

规则：

- 7 态与流转的权威定义见 `open-platform-security-auth §5.1`，本文不得自行增删。
- `locked` 只能由连续失败策略触发或管理员手动设置。
- 从 `locked` 恢复必须走人工解锁接口。
- 应用停用后所有 Token 和动态地址立即失效。

### 12.4 动态地址状态

```text
ACTIVE
EXPIRED
REVOKED
```

### 12.5 事件风险状态

```text
NORMAL
SUSPICIOUS
VIOLATION
RESOLVED
FALSE_POSITIVE
```

## 13. 安全鉴权流程

### 13.1 后台登录

建议第一阶段使用：

- Access Token：短时效 JWT，15 到 30 分钟。
- Refresh Token：服务端可撤销，保存 Hash，7 到 30 天。
- 后台请求必须带 `Authorization: Bearer ...`。
- 用户禁用、角色变更、密码重置后，相关会话可批量失效。

已确认执行口径：P0 采用 JWT + Refresh Token，Redis 保存刷新令牌和黑名单。若未来企业内网强管控场景需要服务端 Session，可作为 P1/P2 安全增强评估，但不再影响 P0 登录方案。

### 13.2 第三方动态鉴权

流程：

1. 第三方使用 `app_code + login_account + password/password_digest + timestamp + nonce + signature` 请求 `/api/open/auth/token`。
2. 系统校验应用状态、凭证状态、密码 Hash、签名、timestamp、nonce。
3. 校验失败写入 `auth_failure_records` 和 `audit_events`。
4. 同一 `open_app_id + credential_id`（应用+凭证维度）连续三次失败，写入 `app_lock_records`，锁定凭证和开放应用，应用状态变为 `locked`。

> D8 锁定维度收口：登录失败锁定维度以 **应用+凭证（`open_app_id + credential_id`）** 为准，与 `open-platform-security-auth §6.5` 一致（取更严口径，避免同一应用下任一凭证失败被其它凭证稀释计数）。注意此处 `credential_id` 仅作为**失败计数与锁定的内部凭证维度键**，与上文鉴权请求标识字段统一为 `login_account` 不冲突：请求方提交 `login_account`，系统据此解析出对应 `credential_id` 后再按应用+凭证维度计数。
5. 校验成功后清理连续失败计数，签发短时效 `access_token` 和 `dynamic_endpoint`。
6. Token 明文只返回一次，数据库只保存 Hash。
7. 后续第三方通过动态地址调用业务 API。
8. API 网关校验 Token、动态地址、签名、nonce、权限、限流。
9. 每次调用写入 `api_call_logs` 和 `audit_events`。

### 13.3 动态调用地址建议

第一阶段建议采用“动态路由前缀 + 短时效 Token”的组合：

```text
/api/open/d/{endpoint_key}/tickets/verify
/api/open/d/{endpoint_key}/events
```

规则：

- `endpoint_key` 随鉴权生成。
- 有效期建议 5 到 30 分钟。
- endpoint 和 token 双重校验。
- endpoint 过期后必须刷新。
- endpoint 不承载业务权限，只作为动态入口因子。

不建议第一阶段做真实动态域名或动态 IP，因为部署、Nginx、证书和第三方联调复杂度较高。

## 14. 事件留痕规范

第一阶段必须写事件的操作：

- 商户创建、启用、暂停、终止。
- 门店、区域、点位创建和修改。
- 用户登录、登出、失败登录、禁用、启用、重置密码。
- 角色授权、用户角色变更、临时授权。
- 开放应用创建、凭证轮换、授权变更、启用、停用。
- 第三方鉴权成功、鉴权失败、应用锁定、人工解锁。
- 第三方 API 调用成功、失败、拒绝、限流。
- 人工补录、风险标记。
- 终端开关创建、启用、停用、灰度和访问被拒绝。
- 订单获取、销售归属、订台定金、卡座服务员分配、服务责任变更。
- 酒吧点单、加单、送达、现金收款、POS 收款、退款、免单和调账。
- 物品追踪码生成、出库核验、机器人交接、后厨接收、服务员取走、送达客人、异常上报。
- 异常发现、异常第一处理人接单、转派、升级、处理完成、复核关闭。
- 事件候选视频片段匹配和人工确认绑定。
- 后续票务出票、核销、退款、结算必须沿用同一事件模型。

事件命名建议：

> 修订说明（2026-06-07）：依据《统一口径基准》D4（tracewire-1、trace-2、devtech-6），**唯一权威事件字典为 `operation-traceability-and-terminal-switch-prd §4.1`**。本节事件清单须与其一字一致；如出现差异，以 operation-traceability §4.1 为权威，本文据其修订，不得另起命名（例如支付统一用 `payment.cash_received`/`payment.pos_received`，销售归属用 `order.sales_attributed`）。

```text
merchant.created
merchant.activated
merchant.suspended
venue.created
user.login_succeeded
user.login_failed
role.permissions_changed
open_app.created
open_app.credential_rotated
open_app.auth_succeeded
open_app.auth_failed
open_app.locked
open_app.unlocked
open_api.called
open_api.rejected
terminal.switch_changed
terminal.access_rejected
order.acquired
order.sales_attributed
reservation.deposit_paid
table.service_assigned
table.service_changed
bar.order_created
bar.order_delivered
payment.cash_received
payment.pos_received
inventory.item_labeled
inventory.outbound_verified
inventory.handover_to_robot
bar_kitchen.goods_received
bar_service.goods_picked_up
bar_service.goods_delivered
inventory.exception_reported
exception.reported
exception.first_handled
exception.escalated
exception.resolved
exception.reviewed
video.candidate_matched
audit.manual_record_created
event.risk_marked
contract.blocked
contract.released
supplier.blacklisted
supply_contract.approved
purchase.ordered
inventory.inbound_confirmed
inventory.inspection_recorded
inventory.transferred
inventory.redeem_reserved
inventory.gift_claimed
inventory.damaged
inventory.surplus_recorded
inventory.counted
inventory.reversed
inventory.loss_exception
bar.table.opened
bar.table.transferred
bar.table.cleared
reservation.created
reservation.deposit_settled
bar.bill_settled
bar.saved_bottle_picked_up
bar.gift_redeemed
ticket.issued
ticket.refunded
ticket.changed
ticket.inventory_locked
ticket.inventory_released
verification.succeeded
verification.failed
points.earned
points.deducted
points.redeemed
points.expired
consent.recorded
consent.withdrawn
venue.over_capacity
personal_data.purged
personal_data.anonymized
```

> 修订说明（2026-06-07 收口）：本清单与 `operation-traceability §4.1` 唯一权威字典同步——本轮 §4.1 补登的供应链/酒吧订台/票务核销退款/会员合规容量域事件，已在此一字一致补入（含 `contract.blocked`/`contract.released` 与 §4.1.1–§4.1.4 全部事件）。`event_category` 与载荷要点以 §4.1 为准，本节只列事件名不另起命名。依据 finding broken-ref-2/3/4、baseline-conf-5、annotation-1、D4。

> 修订说明（2026-06-08 三轮收口）：① 会员积分事件保持 `points.earned/deducted/redeemed/expired` 细粒度命名，**不引入聚合旧名 `member.points_changed`**（§4.1 主表已删除该聚合名，统一由 §4.1.4 points.* 承载）；本清单本就只列 points.*，与 §4.1、data-risk §7.1 三处一致（依据 R3、struct-annot-1、critic2-3、event-consumers-8）。② 同步补入个人信息处置事件 `personal_data.purged`/`personal_data.anonymized`（域=合规，对齐 §4.1.4），载荷要点 data_category/target_table/affected_count/disposal_action/job_log_id 以 §4.1.4 为准（依据 R6、compliance-wired-5、struct-annot-7）。

事件字段要求：

> D8 `event_category` 落列收口（2026-06-18 技术定稿）：**定稿口径：`audit_events` 表落独立数据库列 `event_category`**（值域以 `operation-traceability-and-terminal-switch-prd §4.1` 的事件域分类为准，便于按域查询与归档），下方字段清单据此补入 `event_category`。本口径以本节为权威，`open-platform-security-auth §16.2` 与之保持同一裁定。

- `event_id`
- `event_type`
- `event_category` 【定稿：`audit_events` 落独立数据库列；值域对齐 operation-traceability §4.1 事件域】
- `source_system`
- `actor_type`
- `actor_id`
- `merchant_id`
- `venue_id`
- `subject_type`
- `subject_id`
- `action`
- `occurred_at`
- `request_id`
- `trace_id`
- `risk_level`
- `payload_summary`
- `metadata`

敏感信息处理：

- 密码、Token、Secret 不进入事件 payload。
- 身份证、手机号、银行卡等敏感信息只保留脱敏值或摘要。
- 原始第三方 payload 如含敏感字段，P0 执行确定口径：默认只保存 `raw_payload_hash`、签名校验结果、状态码、必要业务键和脱敏摘要；确需留存必要原文时，必须先做字段级脱敏，再写入 `event_raw_payloads` 的加密字段，密钥引用走供应商密钥台账、到期提醒和 Runbook，不得把未脱敏原文直接写入事件 payload。

## 15. 与票务系统的衔接

票务通票系统开发时必须依赖本文档的地基能力：

- 票务产品归属 `merchant_id`、`venue_id`。
- 联合通票的权益实例必须能拆到不同 `merchant_id`。
- 核销点位引用 `location_point_id`。
- 第三方餐饮、零售券核销通过开放平台鉴权。
- 安卓核销端使用账号权限和设备绑定能力。
- 所有出票、核销、退款、结算写入 `audit_events`。
- 对外票务 API 必须在 `api_resources` 注册并授权给 `open_app`。

## 16. 测试策略

### 16.1 单元测试

覆盖：

- 商户状态流转。
- 角色权限合并。
- 数据范围计算。
- 开放平台签名校验。
- nonce 防重放。
- 连续失败锁定。
- 动态地址过期。
- 事件 payload 脱敏。

### 16.2 集成测试

建议使用 Testcontainers：

- PostgreSQL：验证 Flyway 迁移、Repository、事务。
- Redis：验证 Token、nonce、失败计数、限流。
- NATS：P1 验证事件发布。

必须覆盖：

- Flyway 从空库完整迁移。
- 创建商户到创建开放应用完整流程。
- 第三方三次鉴权失败后锁定。
- 人工解锁后可重新鉴权。
- nonce 重放被拒绝。
- 无权限访问被拒绝。
- 跨商户数据不可见。

### 16.3 API 测试

使用 MockMvc 或 SpringBootTest：

- 后台登录。
- 商户 CRUD。
- 用户角色授权。
- 开放应用创建。
- Token 签发。
- 动态地址调用。
- 事件查询。

### 16.4 迁移测试

规则：

- CI 或本地脚本执行 `flyway clean + migrate`。
- 禁止修改已发布迁移文件。
- 修改表结构必须新增迁移版本。
- 种子权限变更也必须版本化。

## 17. 部署到 160 服务器要求

160 服务器现有条件：

- Ubuntu 24.04.4 LTS
- Java 21
- Maven 3.9.11
- PostgreSQL、Redis、NATS、MinIO 已通过 Docker Compose 启动
- Nginx 已将 `/api/` 和 `/docs` 反代到 `127.0.0.1:8080`
- `complex-api.service` 已创建但未启动

建议部署产物：

```text
/opt/complex/api/complex-api.jar
/etc/complex/complex.env
/var/log/complex
```

应用监听：

```text
127.0.0.1:8080
```

环境变量建议：

```text
SPRING_PROFILES_ACTIVE=prod
COMPLEX_API_PORT=8080
COMPLEX_API_HOST=127.0.0.1
COMPLEX_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/complex
COMPLEX_DATABASE_NAME=complex
COMPLEX_DATABASE_USERNAME=<fill-from-credential-ledger>
COMPLEX_DATABASE_PASSWORD=<fill-from-credential-ledger>
COMPLEX_REDIS_URL=redis://localhost:6379
COMPLEX_NATS_URL=nats://localhost:4222
COMPLEX_MINIO_ENDPOINT=http://localhost:9000
COMPLEX_MINIO_ACCESS_KEY=<fill-from-credential-ledger>
COMPLEX_MINIO_SECRET_KEY=<fill-from-credential-ledger>
JWT_SECRET=<fill-at-least-32-random-characters>
```

> 运行配置收口（2026-06-13）：Spring Boot 正式数据源只接受 `jdbc:postgresql://host:port/database` 形态的 `COMPLEX_DATABASE_URL`；数据库账号和密码必须拆到 `COMPLEX_DATABASE_USERNAME`/`COMPLEX_DATABASE_PASSWORD`，不得继续使用 `postgresql://user:password@host:port/database` 旧配置形态。160 发布和回滚均由 `ops/160/complex-runtime-config-check.sh` 阻断错误配置。

部署步骤建议：

```text
构建 bootJar
上传或复制 jar 到 /opt/complex/api/complex-api.jar
确认 /etc/complex/complex.env
执行 systemctl daemon-reload
启动或重启 complex-api.service
检查 journalctl -u complex-api
访问 http://127.0.0.1:8080/api/health
访问 Nginx 对外 /api/health
```

上线前必须更换：

- PostgreSQL 密码。
- MinIO 密码。
- JWT Secret。
- 开放平台初始凭证。
- 管理员初始密码。

## 18. OpenAPI 文档要求

第一阶段需要生成三组文档：

- 管理后台 API：`/docs/admin`
- 开放平台 API：`/docs/open`
- Webhook API：`/docs/webhook`

要求：

- 每个接口有请求 DTO、响应 DTO、错误码。
- 开放 API 必须说明签名方式、timestamp、nonce、动态地址、Token 有效期。
- SDK 调用示例第一阶段（P0）只提供 **Java 与 JavaScript(Node.js)** 两类签名调用示例；**Python/PHP 示例为 P1**；完整多语言 SDK 包进入 P2。此口径与 `open-platform-security-auth-development-design §9.1/§21` 一字一致（D8 收口：原"JavaScript、Java、Python 三类"与 open 文档不一致，统一为 P0=Java+JavaScript(Node.js)、P1=Python/PHP、P2=完整 SDK）。
- 文档只展示可授权给第三方的接口，不暴露内部管理接口。
- 运行时 springdoc 输出必须能与 `docs/openapi/openapi-p0.yaml/json`、`p0-openapi-operation-acceptance-matrix-v0.1.md` 和 `p0-openapi-contract-closure-appendix-v0.1.md` 对齐；`verify:openapi-closure` 未通过时不得生成 SDK/mock、接口自动化或供应商联调包。

## 19. 第一阶段开发顺序

### P0：平台地基闭环

1. Spring Boot 后端项目骨架。
2. 配置 PostgreSQL、Flyway、Redis、OpenAPI。
3. `V001` 到 `V005` 迁移。
4. 健康检查接口。
5. 统一异常、响应、请求上下文。
6. 商户、门店、区域、点位基础 API。
7. 用户、角色、权限、数据范围基础 API。
8. 开放应用、凭证、API 授权 API。
9. 动态鉴权：Token、动态地址、nonce、三次失败锁定、人工解锁。
10. 事件中心写入和查询。
11. 部署到 160 的 `complex-api.service`。

### P1：票务与设备衔接

1. 票务基础表迁移。
2. 票务开放 API 注册。
3. 安卓设备注册和绑定。
4. 核销事件写入。
5. NATS 事件异步发布。
6. Webhook 订阅和投递。

### P2：增强能力

1. QueryDSL 或 jOOQ。
2. 细粒度限流、熔断、配额。
3. API SDK 自动生成。
4. 事件归档。
5. 风控规则引擎。
6. 多环境配置和 CI/CD。

## 20. 开发验收标准

文档转编码后，第一阶段后端验收标准：

- 空库启动时 Flyway 可完整迁移。
- `GET /api/health` 可返回数据库、Redis 基础状态。
- 可创建平台组织、商户、门店、区域、点位。
- 可创建用户、角色、权限绑定和数据范围。
- 可创建开放应用并生成凭证。
- 第三方能成功获取短时效 Token 和动态调用地址。
- 连续三次鉴权失败后开放应用被锁定。
- 人工解锁后可重新鉴权。
- nonce 重放请求被拒绝。
- 每次关键操作都能在事件中心查询到。
- 服务可通过 `complex-api.service` 在 160 服务器启动。
- OpenAPI 文档可访问。

## 21. 已确认执行口径

1. 构建工具最终使用 Maven，方便团队上手和 160 构建；Gradle 不作为 P0 主线。
2. 后台登录采用 JWT + Refresh Token，Redis 存刷新令牌和黑名单。
3. ORM 第一阶段采用 Spring Data JPA 起步，复杂查询后续补 QueryDSL 或 jOOQ。
4. 动态调用地址 P0 采用动态路由前缀，不要求动态域名/动态网关地址。
5. 平台主体作为特殊 `organization` 建模，平台自营业务建特殊 `merchant` 主体，以统一页面、权限、结算和看板。
6. 事件中心 P0 同步写事件表，P1 增加 NATS 发布。
7. PostgreSQL、Redis、NATS、MinIO 在开发/预生产继续使用 160 Docker Compose；正式生产前再评估独立服务或云服务。
8. 第三方 SDK 第一阶段提供 OpenAPI 文档、curl 示例和 Java/JavaScript 示例；P2 再做自动生成 SDK 包。
9. 权限模板 P0 落在 access 模块，支持基础模板、临时模板、复制模板、有效期、单次有效、撤销和审计；最终权限计算以统一口径 D11 为准。
10. P0 币种仅启用 CNY，金额字段按整数分；保留 `currency=CNY` 字段用于未来扩展。

## 22. 变更日志

| 日期 | 类型 | 内容 | 原因/依据 |
|---|---|---|---|
| 2026-06-07 | 改 | §7 通用字段规范：金额由 `numeric(18,2)`（元）改为 `bigint` 整数分，全局金额=分，并加单位标注 | 统一口径基准 D1 / devtech-3、fielddb-7 |
| 2026-06-07 | 注 | §6.5、§8.5 开放平台权限编码保持点号风格，并注明"权限点以 open-platform-security-auth §3 为准"；`openplatform.app.manage` 为粗粒度聚合，细分点（app.create/update/disable/view/rotate_secret）以 open-platform 文档为权威 | 统一口径基准 D3 / devtech-2：两文件权限编码统一为同一套 |
| 2026-06-07 | 注 | §8.6/§13.3 对外路径 `/api/open/auth/token`、`/api/open/d/{endpoint_key}/...` 已与 D3 一致，予以保留并标注与 open-platform 对齐 | 统一口径基准 D3 / devtech-1 |
| 2026-06-07 | 改 | §12.3 开放应用状态由 5 态对齐为 open-platform §5.1 的 7 态（draft/testing/active/suspended/locked/disabled/revoked），删除无对应流转的 EXPIRED，注明权威来源 | 统一口径基准 D3 / devtech-1/7 |
| 2026-06-07 | 注 | §14 事件清单注明以 `operation-traceability-and-terminal-switch-prd §4.1` 为唯一权威事件字典，差异以其为准 | 统一口径基准 D4 / tracewire-1、trace-2、devtech-6 |
| 2026-06-07 | 增 | §14 事件清单与 §4.1 同步补登供应链/酒吧订台/票务核销退款/会员合规容量域事件（contract.*、supplier.blacklisted、inventory.*、reservation.created/deposit_settled、bar.bill_settled、ticket.refunded/changed/inventory_locked/released、verification.succeeded/failed、points.*、consent.*、venue.over_capacity），一字一致 | 统一口径基准 D4 / broken-ref-2/3/4、baseline-conf-5、annotation-1 |
| 2026-06-07 | 改 | §（设备/终端职责）终端能力开关描述正文由"App、小程序、Web、平板、员工端、商户侧和一体机"笼统词改为对齐 D5 十端权威码（拆工作/私人手机、拆商户 portal/app、补移动管理端） | 统一口径基准 D5 / baseline-conf-6 |
| 2026-06-08 | 注 | §14 事件清单 | 会员积分事件保持 `points.*` 细粒度命名、不引入聚合旧名 `member.points_changed`（§4.1 主表已删该名），本清单与 §4.1、data-risk §7.1 三处一致 | R3 / struct-annot-1、critic2-3、event-consumers-8 |
| 2026-06-08 | 增 | §14 事件清单 | 同步补入个人信息处置事件 `personal_data.purged`/`personal_data.anonymized`（域=合规，对齐 operation-traceability §4.1.4），载荷 data_category/target_table/affected_count/disposal_action/job_log_id | R6 / compliance-wired-5、struct-annot-7 |
| 2026-06-10 | 增/改 | §4.4 / §6.2 / §8.4 / §21：补权限模板实体、迁移表和管理 API；确认 P0 CNY 币种口径 | 用户确认权限模板机制和币类默认按建议执行 |
| 2026-06-12 可编码收口 | 改 | §8：将早期 REST 前缀明确标注为旧路径识别信息，不得继续作为正式 Controller 前缀；正式路径以 OpenAPI 为准 | 防止早期 `/api/admin/*`、`/api/open/*` 草案污染 Java/Spring 正式接口 |
| 2026-06-13 OpenAPI 追溯闸口 | 改 | §8/§18：早期 REST 草案若要进入正式实现，必须先补 OpenAPI、operation 验收矩阵和 `p0-openapi-contract-closure-appendix-v0.1.md`，并通过 `verify:openapi-closure` | 防止 Java 后端按历史草案直接生成 Controller、SDK/mock、接口自动化或供应商联调包 |
| 2026-06-18 D8 收口 | 改 | §18 SDK 首批语言统一为 P0=Java+JavaScript(Node.js)、P1=Python/PHP、P2=完整 SDK（原"JS/Java/Python 三类"与 open 不一致） | 与 open-platform §9.1/§21 收敛 |
| 2026-06-18 D8 收口 | 改 | §8.6/§13.2 鉴权请求标识字段 `credential_id` → `login_account`（JSON `loginAccount`，DB `login_account`）；`credential_id` 仅留作失败计数/锁定的内部凭证维度键 | 与 open-platform §6.2/§12.4 及 DB 列名收敛 |
| 2026-06-18 D8 收口 | 改 | §13.2 登录失败锁定维度由"应用"改为更严的"应用+凭证 `open_app_id + credential_id`" | 取更严口径，与 open-platform §6.5 收敛 |
| 2026-06-18 D8 收口 | 改 | §6.3 V003（P0）误列 `webhook_subscriptions`/`webhook_delivery_logs` 移除，Webhook 表统一归 P1 | 与 open-platform §12.2 收敛 |
| 2026-06-18 D8 收口 | 改 | §14 事件字段补入 `event_category`【待技术确认，推荐落列，值域对齐 operation-traceability §4.1】 | 与 open-platform §16.2 统一裁定，消除"落/不落"不一致 |
| 2026-06-18 D8 定稿 | 改 | §14 `event_category` 由【待技术确认/是否落列】定稿为"`audit_events` 落独立数据库列"，值域对齐 operation-traceability §4.1 事件域 | 技术已拍板，去除两可表述，与 open-platform §16.2 同一裁定 |
| 2026-06-18 D11 补登 | 增 | §4.8.1 / §6.6 | 补员工设备凭证 `staff_device_credentials` 后端承接设计（现状→目标P0→候选验收）：新增 `StaffDeviceCredential` 实体、注册审批/换机/失效状态机、Service/Controller 与事件落点；明确与 kiosk `terminal_device_credentials`(V011) 互不串用 | DB 有字段、Java 设计无承接；R5 审计与 android §3.1/§13.1 |
