# P0 API Contract Pack V0.1

> **口径修订（2026-08-06 R10）：「人工放行」已废止**——ENUM「核销记录状态」条 明确「**系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过**」（DEC-CFG-2026-08-03-12 / DEC-R6-08）。本文原有的「人工放行」取值、端点、按钮、指标、权限点与复核类型**一律废止**，废止处就地标注「原含『人工放行』，已废止·见文首口径」；已实现的端点、权限点与状态取值待代码轮下线，期间不得新增调用、不得新写入。**注意**：合同或基础定义违规被卡住后「由更高级审核放行」的准入场景仍然存在，那是准入阻断的升级放行，与核销域无关，不得回读为「可以人工放行一笔核销」。

## 1. 文档定位

本文档是综合体管理系统 P0 阶段的接口契约基线，用于让后端、管理后台、Android 工作手机/平板、一体机、小程序/App/Web、第三方供应商和测试团队按同一套接口规则并行工作。

本文档不是运行时最终 OpenAPI YAML 文件；它是生成编码前 `openapi-p0.yaml/json` 契约草案和校验运行时 OpenAPI 的契约源。后续编码时，Spring Boot 接口、springdoc-openapi 输出、自动化测试、第三方联调文档必须与本文保持一致。

## 2. P0 接口强制原则

| 原则 | 要求 |
|---|---|
| 正式技术栈 | Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven |
| 契约优先 | P0 接口先按本文冻结路径、鉴权、响应格式、错误码、幂等和权限事件映射 |
| 业务主模型稳定 | 不把供应商私有字段写入票务、酒吧、财务、库存等业务主模型 |
| 全链路留痕 | 关键写操作必须产生标准事件，并可关联操作者、终端、设备、IP、业务对象 |
| 幂等优先 | 支付、退款、出票、核销、入场、库存扣减、回调、日结等写操作必须支持幂等 |
| 权限先行 | 每个接口必须标注权限点、数据范围和敏感等级 |
| 可验收 | 每个接口必须能映射到 Given/When/Then 验收标准或边界用例 |
| 字段可编码 | OpenAPI schema 和公共参数必须包含字段含义、必填项、枚举、长度、数值范围、数组上限和单位；不得保留“业务字段 xxx”式模板说明 |

## 3. 版本、环境与路径

### 3.1 API 版本

| 项 | 规则 |
|---|---|
| 内部业务 API 版本 | `/api/v1/...` |
| 开放平台鉴权 | `/api/open/auth/token` |
| 开放平台动态业务 API | `/api/open/d/{endpoint_key}/{domain}/...` |
| 开放平台 Webhook | `/api/open/d/{endpoint_key}/webhooks/{provider}/{event}` |
| OpenAPI 文档 | `/docs/openapi` 或 Springdoc 默认文档页，实际部署路径以 160 Runbook 为准 |

域名 HTTPS P0 先不绑定，本文只固定路径和契约；生产域名、证书、回调域名由上线部署配置决定。

### 3.2 API 分组

| 分组 | 路径前缀 | 说明 |
|---|---|---|
| 系统健康 | `/api/v1/system` | 健康检查、版本、配置只读摘要 |
| 后台鉴权 | `/api/v1/auth` | 员工、商户、后台登录和会话 |
| 平台治理 | `/api/v1/platform` | 综合体、商户、空间、合同、主体绑定 |
| 权限 | `/api/v1/access` | 权限点、角色、模板、临时授权 |
| 终端 | `/api/v1/terminals` | 设备绑定、能力开关、一体机模式 |
| 票务权益 | `/api/v1/ticketing` | 票种、票价、订单、出票、权益、核销、退款 |
| 客人端 | `/api/v1/customer` | App/小程序/Web 客人登录、购票、二维码、订单 |
| 酒吧 | `/api/v1/bar` | 卡座、订台、开台、点单、账单、低消、年龄核验、提成 |
| 库存供应链 | `/api/v1/inventory` | 供应商、采购、库存、追踪码、存酒、报损 |
| 游乐 | `/api/v1/amusement` | 项目、场次、容量、核销、安全巡检 |
| 财务 | `/api/v1/finance` | 应收应付、日结、对账、支付抽象、结算 |
| 留痕风控 | `/api/v1/evidence` | 事件、证据、复核、风险预警 |
| 数据分析 | `/api/v1/analytics` | P0 看板、指标、规则命中 |
| 第三方集成 | `/api/v1/integration` | 供应商 Adapter、mock、字段映射、补偿 |

## 4. 通用请求规范

### 4.1 Header

| Header | 必填 | 用途 |
|---|---:|---|
| `Authorization: Bearer <token>` | 条件必填 | 后台、员工端、商户端和客人端访问令牌；开放平台、Webhook、健康检查和设备凭证类接口按 4.1A 认证矩阵执行 |
| `X-Project-Id` | 是 | 当前综合体项目 |
| `X-Terminal-Type` | 是 | 终端类型，取 `unified-standards-baseline-v0.1.md` D5 |
| `X-Device-Id` | 条件必填 | 工作手机、平板、一体机等现场设备 |
| `X-Idempotency-Key` | 写操作必填 | 幂等键 |
| `X-Request-Id` | 建议 | 请求追踪 |
| `X-Operator-Id` | 服务端识别 | 操作者，通常由 token 解析 |

`X-Terminal-Type` 与 OpenAPI `x-terminal-allowed` 只能使用 `unified-standards-baseline-v0.1.md` D5 的 10 个终端码。门禁核销设备、人脸/年龄核验设备、监控探针和服务端调用不是终端类型，必须在 OpenAPI 中分别通过 `x-device-capability` 或 `x-invocation-channel` 表达；不得把 `gate-device`、`authorized-age-device`、`server-to-server` 等历史别名写入终端枚举。

### 4.1A 接口认证矩阵

| 接口类型 | 认证方式 | 是否要求 `Authorization` | 说明 |
|---|---|---:|---|
| 后台 Web、员工端、商户端 | 员工/商户员工 JWT | 是 | 登录后按权限矩阵和数据范围校验 |
| 客人端小程序/App/Web | 客人 token | 是 | 手机号/开放身份登录后访问购票、会员、订台和二维码 |
| 工作手机、平板、一体机设备启动和配置 | 设备凭证 + 设备绑定 + 端能力开关 | 否 | 设备取短期凭证后，业务接口再叠加员工或设备上下文 |
| 开放平台取 token | open app 凭证 + 签名 | 否 | `POST /api/open/auth/token` 不要求员工 JWT |
| 入站 Webhook | 供应商签名 + nonce + 时间戳 | 否 | 按 9.1 Header 验签和防重放 |
| 健康检查和版本 | 内网或运维白名单 | 否 | 不返回敏感配置和密钥 |

所有 OpenAPI `security` 声明必须按本矩阵拆分，不得把 `Authorization` 设成全局强制项。

### 4.2 幂等规则

| 场景 | 幂等键规则 |
|---|---|
| 购票下单 | 同一终端一次提交生成一个 `X-Idempotency-Key` |
| 支付回调 | 以供应商交易号 + 平台订单号 + 回调事件号归一 |
| 出票 | 订单号 + 出票批次 |
| 核销 | 票码凭证哈希 + 权益实例 + 终端设备 + 幂等键 |
| 卡座入场 | 卡座预约号 + 入场人标识 + 幂等键 |
| 库存扣减 | 来源单据 + 物品/批次 + 幂等键 |
| 日结锁定 | 营业日 + 主体/门店 + 幂等键 |
| Webhook | `provider_event_id` 或 `raw_payload_hash` + 供应商 + 事件类型 |

重复请求必须返回第一次处理结果，不得重复扣库存、重复出票、重复核销、重复生成应收应付或重复写提成（同一提交范围内；其他提交范围再用同一幂等键提交同一操作的，拒绝且不返回第一次处理结果，见平台治理 PRD §2A.3）。

### 4.3 分页、排序与过滤

列表接口统一使用：

| 参数 | 说明 |
|---|---|
| `page` | 从 1 开始 |
| `page_size` | 默认 20，最大 100 |
| `sort` | `field:asc` 或 `field:desc` |
| `keyword` | 模糊搜索 |
| `status` | 状态过滤 |
| `date_from` / `date_to` | 时间范围 |

## 5. 通用响应格式

### 5.1 成功响应

```json
{
  "success": true,
  "trace_id": "req_20260610_000001",
  "data": {},
  "meta": {
    "server_time": "2026-06-10T12:00:00Z"
  }
}
```

重复提交返回第一次处理结果时（见 4.2），成功响应在顶层另带 `replayed: true`，`message` 为「该操作已完成（重复提交，返回首次结果）」，`data` 仍是第一次的结果；第一次执行的响应、失败响应和查看类请求的响应不带该字段（OpenAPI `SuccessResponse.replayed`，可选布尔字段）。

### 5.2 分页响应

```json
{
  "success": true,
  "trace_id": "req_20260610_000002",
  "data": {
    "items": [],
    "page": 1,
    "page_size": 20,
    "total": 0
  }
}
```

### 5.3 错误响应

```json
{
  "success": false,
  "trace_id": "req_20260610_000003",
  "error": {
    "code": "TICKET_NOT_VALID",
    "message": "票码不可核销",
    "user_message": "该票当前不可使用",
    "details": {}
  }
}
```

错误响应不得泄露密钥、签名、原始人脸图像、未脱敏手机号、供应商敏感原文和数据库异常堆栈。

`message` 是展示文字，一律中文业务话术，禁止出现英文字段键或英文句子（后台、客人端、设备端、安卓端共用同一条文案，安卓端没有任何本地翻译层，后端吐什么就上屏什么）。抛点仍按 `order_id is required` 写英文字段键——那是给日志和排障看的机读信息，写响应体时由 `GlobalExceptionHandler` 经 `FieldLabels` 统一换成中文字段名（如「订单未填写」）；认不出的字段键退到中文兜底文案，绝不把英文键名当展示文字外抛。

## 6. 通用错误码

| 错误码 | HTTP | 含义 |
|---|---:|---|
| `UNAUTHORIZED` | 401 | 未登录或 token 无效 |
| `FORBIDDEN` | 403 | 无权限或数据范围不允许 |
| `VALIDATION_FAILED` | 400 | 参数校验失败 |
| `IDEMPOTENCY_CONFLICT` | 409 | 幂等键冲突 |
| `IDEMPOTENCY_KEY_USED` | 409 | 该提交编号已被使用，本次操作未执行 |
| `BUSINESS_REJECTED` | 409 | 业务规则拒绝 |
| `NOT_FOUND` | 404 | 对象不存在 |
| `STATE_CONFLICT` | 409 | 状态不允许 |
| `RATE_LIMITED` | 429 | 触发限流 |
| `PROVIDER_UNAVAILABLE` | 503 | 第三方供应商不可用 |
| `PROVIDER_SIGNATURE_INVALID` | 400 | 第三方签名无效 |
| `PAYMENT_STATE_UNKNOWN` | 202 | 支付状态待确认 |
| `RISK_REVIEW_REQUIRED` | 202 | 进入风险复核 |
| `INTERNAL_ERROR` | 500 | 系统内部错误 |

业务域可以扩展错误码，但必须继承统一响应格式，并在 OpenAPI 中列出。

## 7. 鉴权、权限与数据范围

### 7.1 端侧鉴权

| 端 | 鉴权方式 |
|---|---|
| 管理后台 Web | 员工账号 + JWT + Refresh Token |
| 商户门户 | 商户员工账号 + JWT + 数据范围 |
| Android 工作手机/平板 | 员工账号 + 设备绑定 + MDM/设备白名单 |
| 一体机 | 设备凭证 + 点位绑定 + 模块开关 |
| 小程序/App/Web 客人端 | 手机号/平台授权登录 + 客人 token |
| 第三方系统 | 开放应用鉴权 + 动态 endpoint + 签名 + nonce |

### 7.2 权限映射要求

每个接口必须在 OpenAPI 扩展字段或接口说明中声明权限、认证上下文、终端、数据范围、幂等和事件。权限码和接口级事件的机器事实源为 `docs/openapi/openapi-p0.yaml/json` 与 `p0-openapi-contract-closure-appendix-v0.1.md`，权限模板、临时授权、单次有效和高风险审批规则以 `p0-operation-permission-matrix-v0.1.md` 为准。接口文档不得自行创造同义权限码；如发现冲突，先改 OpenAPI、追溯附录或权限矩阵，再进入联调。

`x-permission` 只允许填写员工/后台权限矩阵中的权限码，或 `public`、`authenticated`、`not_applicable` 三个保留标记。客人 Token、设备凭证、开放平台应用凭证、Webhook 签名等非员工主体必须写入 `x-auth-context`，不得伪装成权限码。

执行收口（2026-06-12）：所有 P0 operation 都必须显式声明 `x-auth-context`。即使接口是公开匿名访问或员工 Bearer Token 访问，也必须分别写为 `public` 或 `staff-token`，不得由 `security` 或 `x-permission` 口头推断。

| 字段 | 示例 |
|---|---|
| `x-permission` | `verification.execute` |
| `x-auth-context` | `public / staff-token / customer-token / device-credential / open-app-credential / webhook-signature` |
| `x-data-scope` | `project / merchant / store / bar / device` |
| `x-terminal-allowed` | `staff_work_phone, tablet, kiosk` |
| `x-audit-event-success` | `verification.succeeded` |
| `x-audit-event-failure` | `verification.failed` |
| `x-idempotency-required` | `true` |
| `requestBody.$ref` | `#/components/requestBodies/TicketOrderRequest` |

## 8. P0 接口目录

本节是 P0 接口目录基线。字段细节以对应 PRD、开发设计、数据库设计和正式 OpenAPI 为准；路径、职责、权限、事件和验收方向以本文为准。

> 2026-06-13 执行收口：首版机器可读契约已落地为 `docs/openapi/openapi-p0.yaml` 和 `docs/openapi/openapi-p0.json`。本 Markdown 中的 `GET/POST` 合并行仅作阅读摘要；编码、联调、自动化测试、供应商 mock 和 springdoc 比对必须使用 OpenAPI 每个 operation 的字段级 `requestBody`、`x-permission`、`x-auth-context`、`x-data-scope`、`x-terminal-allowed`、幂等和事件声明。operation 级验收以 `p0-openapi-operation-acceptance-matrix-v0.1.md` 为准，operation 到权限、事件、任务簇、终端和样例准入的完整追溯以 `p0-openapi-contract-closure-appendix-v0.1.md` 为准。

### 8.1 系统、登录和会话

| 方法 | 路径 | 用途 | 权限/事件 |
|---|---|---|---|
| `GET` | `/api/v1/system/health` | 健康检查 | 无 |
| `GET` | `/api/v1/system/version` | 版本和构建信息 | `system.version.view` |
| `POST` | `/api/v1/auth/login` | 员工/商户员工登录 | `auth.login.succeeded/failed` |
| `POST` | `/api/v1/auth/refresh` | 刷新会话 | `auth.token.refreshed` |
| `POST` | `/api/v1/auth/logout` | 退出登录 | `auth.logout` |

### 8.2 平台治理

| 方法 | 路径 | 用途 | 权限/事件 |
|---|---|---|---|
| `GET` | `/api/v1/platform/projects` | 查询综合体项目 | `governance.project.view` |
| `GET/POST` | `/api/v1/platform/merchants` | 商户查询/创建 | `GET=merchant.profile.view`；`POST=merchant.profile.manage` |
| `PUT` | `/api/v1/platform/merchants/{merchant_id}` | 修改商户 | `merchant.profile.manage` |
| `GET/POST` | `/api/v1/platform/spaces` | 场馆、区域、点位 | `GET=governance.space.view`；`POST=governance.space.manage` |
| `GET/POST` | `/api/v1/platform/contracts` | 合同摘要和附件归档 | `GET=contract.view`；`POST=contract.draft.manage` |
| `POST` | `/api/v1/platform/contracts/{contract_id}/approve` | 合同审批流 | `contract.lifecycle.approve` |
| `GET/POST` | `/api/v1/platform/business-entities` | 经营/运营/合同/结算主体档案 | `GET=entity.binding.view`；`POST=entity.binding.manage` |
| `GET/POST` | `/api/v1/platform/business-entity-bindings` | 四类主体唯一绑定 | `GET=entity.binding.view`；`POST=entity.binding.manage` |
| `POST` | `/api/v1/platform/business-entity-bindings/{id}/change` | 主体绑定变更 | `entity.binding.manage` |

### 8.3 权限、模板和临时授权

| 方法 | 路径 | 用途 | 权限/事件 |
|---|---|---|---|
| `GET` | `/api/v1/access/permissions` | 权限点列表 | `access.permission.view` |
| `GET/POST` | `/api/v1/access/roles` | 角色列表/创建 | `access.role.view` / `access.role.manage` |
| `GET/POST` | `/api/v1/access/permission-templates` | 权限模板列表/创建 | `GET=access.permission_template.view`；`POST=access.permission_template.manage` |
| `POST` | `/api/v1/access/permission-templates/{id}/copy` | 复制模板 | `access.permission_template.manage` |
| `PUT` | `/api/v1/access/permission-templates/{id}` | 修改模板 | `access.permission_template.manage` |
| `POST` | `/api/v1/access/permission-templates/{id}/apply` | 应用到员工/角色 | `access.permission_template.apply` |
| `POST` | `/api/v1/access/temporary-grants` | 临时授权 | `access.temporary_grant.create` |
| `POST` | `/api/v1/access/temporary-grants/{id}/revoke` | 撤销临时授权 | `access.temporary_grant.revoke` |

### 8.4 终端、设备和一体机

| 方法 | 路径 | 用途 | 权限/事件 |
|---|---|---|---|
| `POST` | `/api/v1/terminals/devices/bind` | 设备绑定 | `terminal.device.bind` |
| `POST` | `/api/v1/terminals/devices/{id}/unbind` | 设备解绑 | `terminal.device.unbind` |
| `GET` | `/api/v1/terminals/capability-switches` | 查询端能力开关 | `terminal.switch.view` |
| `PUT` | `/api/v1/terminals/capability-switches/{id}` | 修改端能力开关 | `terminal.switch.update` |
| `GET` | `/api/v1/terminals/kiosks/{device_id}/config` | 一体机配置，含 `kiosk_mode` | 设备凭证 |
| `POST` | `/api/v1/terminals/kiosks/{device_id}/heartbeat` | 一体机心跳 | `terminal.kiosk.heartbeat` |

### 8.5 票务、票价、订单、出票和核销

| 方法 | 路径 | 用途 | 权限/事件 |
|---|---|---|---|
| `GET/POST` | `/api/v1/ticketing/ticket-types` | 票种查询/创建 | `ticket.product.view` / `ticket.product.manage` |
| `POST` | `/api/v1/ticketing/ticket-types/{id}/publish` | 发布票种 | `ticket.product.manage` |
| `GET/POST` | `/api/v1/ticketing/price-rules` | 平日/周末/节假日及派生价规则 | `ticket.price_variant.view` / `ticket.price_variant.manage` |
| `POST` | `/api/v1/ticketing/orders` | 创建票务订单 | `ticket.order.create` |
| `POST` | `/api/v1/ticketing/orders/{order_id}/pay-intents` | 创建支付意图 | `finance.payment_intent.create` |
| `POST` | `/api/v1/ticketing/orders/{order_id}/issue` | 出票 | `ticket.ticket.issue` |
| `GET` | `/api/v1/ticketing/tickets/{ticket_id}/credential` | 获取动态二维码凭证 | `ticket.credential.view` |
| `POST` | `/api/v1/ticketing/verifications/preview` | 核销预览 | `verification.execute` |
| `POST` | `/api/v1/ticketing/verifications` | 正式核销 | `verification.execute` |
| `POST` | `/api/v1/ticketing/refunds` | 退款/撤销申请 | `ticket.refund.request` |
| `POST` | `/api/v1/ticketing/refunds/{refund_id}/approve` | 退款审批 | `ticket.refund.approve` |

### 8.6 客人端、小程序/App/Web

| 方法 | 路径 | 用途 | 权限/事件 |
|---|---|---|---|
| `POST` | `/api/v1/customer/auth/phone-login` | 手机号登录 | `customer.auth.login` |
| `POST` | `/api/v1/customer/consents` | 隐私同意记录 | `customer.consent.created` |
| `GET` | `/api/v1/customer/ticket-products` | 可购票种/票价 | 客人 token |
| `POST` | `/api/v1/customer/ticket-orders` | 客人购票下单 | `customer.ticket_order.created` |
| `GET` | `/api/v1/customer/orders` | 我的订单 | 客人 token |
| `GET` | `/api/v1/customer/tickets/{ticket_id}/qr` | 我的动态二维码 | 客人 token |
| `GET` | `/api/v1/customer/bar-reservation-options` | 可订酒吧、卡座、时段、人数额度和定金规则 | `customer.bar_reservation_options_viewed` |
| `POST` | `/api/v1/customer/bar-reservations` | 卡座预订 | `customer.bar_reservation.created` |
| `GET` | `/api/v1/customer/points` | 积分余额 | 客人 token |

### 8.7 酒吧、卡座、点单、支付和提成

| 方法 | 路径 | 用途 | 权限/事件 |
|---|---|---|---|
| `GET/POST` | `/api/v1/bar/tables` | 桌台/卡座管理 | `bar.table.view` / `bar.table.manage` |
| `POST` | `/api/v1/bar/reservations` | 订台/卡座预订 | `bar.reservation.create` |
| `POST` | `/api/v1/bar/reservations/{id}/invitation/entry-verify` | 卡座邀请函门口入场核验（本人动态入场码） | `bar.table.guest_checkin` |
| `POST` | `/api/v1/bar/age-verifications` | 年龄/人脸核验 | `bar.entry.age_verify` |
| `POST` | `/api/v1/bar/tables/{table_id}/open` | 开台 | `bar.table.open_close` |
| `POST` | `/api/v1/bar/orders` | 点单 | `bar.order.create` |
| `POST` | `/api/v1/bar/orders/{order_id}/deliver` | 出酒/送达确认 | `bar.order.create` |
| `POST` | `/api/v1/bar/bills/{bill_id}/pay-intents` | 账单支付意图 | `finance.payment_intent.create` |
| `POST` | `/api/v1/bar/bills/{bill_id}/settle` | 账单结清 | `finance.payment.register` |
| `POST` | `/api/v1/bar/bills/{bill_id}/stored-value/sms` | 账单储值付款发验证码 | `finance.payment.register` |
| `POST` | `/api/v1/bar/bills/{bill_id}/commissions/calculate` | 卡座销售提成计算 | `bar.table_commission.calculate` |

### 8.8 供应链、库存、追踪码和存酒

| 方法 | 路径 | 用途 | 权限/事件 |
|---|---|---|---|
| `GET/POST` | `/api/v1/inventory/suppliers` | 供应商管理 | `inventory.supplier.view` / `inventory.supplier.manage` |
| `GET/POST` | `/api/v1/inventory/purchase-requests` | 采购申请 | `inventory.purchase_request.view` / `inventory.purchase_request.manage` |
| `POST` | `/api/v1/inventory/stock-in` | 入库 | `inventory.stock.move` |
| `POST` | `/api/v1/inventory/stock-movements` | 库存移动/扣减 | `inventory.stock.move` |
| `POST` | `/api/v1/inventory/trace-codes/scan-handover` | 物品追踪码扫码交接 | `inventory.stock.move` |
| `GET/POST` | `/api/v1/inventory/saved-bottles` | 存酒查询/登记 | `bar.saved_bottle.manage` |
| `POST` | `/api/v1/inventory/saved-bottles/{id}/claim` | 存酒领取 | `bar.saved_bottle.manage` |
| `POST` | `/api/v1/inventory/stock-adjustments` | 盘点差异/报损/报溢 | `inventory.stock.adjust` |

### 8.9 游乐、场次、容量和安全巡检

| 方法 | 路径 | 用途 | 权限/事件 |
|---|---|---|---|
| `GET/POST` | `/api/v1/amusement/projects` | 游乐项目管理 | `GET=amusement.session.view`；`POST=amusement.session.manage` |
| `GET/POST` | `/api/v1/amusement/sessions` | 场次和容量 | `GET=amusement.session.view`；`POST=amusement.session.manage` |
| `POST` | `/api/v1/amusement/verifications` | 游乐权益核销 | `amusement.verification.execute` |
| `POST` | `/api/v1/amusement/manual-overrides` | 人工放行（**已废止·见文首口径**，接口已下线） | `amusement.verification.override`（停用） |
| `GET` | `/api/v1/amusement/projects/{id}/open-eligibility` | 查询项目/场次安全与容量准入 | `amusement.safety_inspection.view` |
| `GET/POST` | `/api/v1/amusement/safety-templates` | 巡检模板查询/维护 | `GET=amusement.safety_template.view`；`POST=amusement.safety_template.manage` |
| `GET/POST` | `/api/v1/amusement/safety-inspections` | 开放前安全巡检 | `GET=amusement.safety_inspection.view`；`POST=amusement.safety_inspection.create` |
| `POST` | `/api/v1/amusement/safety-inspections/{id}/submit` | 提交巡检结果 | `amusement.safety_inspection.submit` |
| `POST` | `/api/v1/amusement/safety-inspections/{id}/rectify` | 提交整改说明和证据 | `amusement.safety_inspection.rectify` |
| `POST` | `/api/v1/amusement/safety-inspections/{id}/recheck` | 提交复检结果 | `amusement.safety_inspection.recheck` |
| `POST` | `/api/v1/amusement/safety-inspections/{id}/approve-reopen` | 严重项恢复开放 | `amusement.safety_inspection.approve_reopen` |

### 8.10 财务、支付、应收应付和日结

| 方法 | 路径 | 用途 | 权限/事件 |
|---|---|---|---|
| `POST` | `/api/v1/finance/payment-intents` | 创建聚合支付意图 | `finance.payment_intent.create` |
| `POST` | `/api/v1/finance/payments/{payment_id}/confirm-offline` | 现金/POS 线下收款确认 | `finance.offline_payment.confirm` |
| `POST` | `/api/v1/finance/refunds` | 退款请求 | `finance.refund.create` |
| `GET` | `/api/v1/finance/ar-ap-items` | 应收应付台账查询 | `finance.ar_ap.view` |
| `POST` | `/api/v1/finance/ar-ap-items` | 手工新增台账项 | `finance.ar_ap.create` |
| `POST` | `/api/v1/finance/ar-ap-items/{id}/payments` | 收付款登记 | `finance.ar_ap.payment.create` |
| `POST` | `/api/v1/finance/daily-closes` | 日结锁定 | `finance.daily_close.lock` |
| `GET` | `/api/v1/finance/daily-closes` | 日结批次列表 | `finance.daily_close.manage` |
| `GET` | `/api/v1/finance/reconciliation/differences` | 对账差异 | `finance.reconciliation.view` |

### 8.11 留痕、证据、复核和风险

| 方法 | 路径 | 用途 | 权限/事件 |
|---|---|---|---|
| `GET` | `/api/v1/evidence/events` | 事件查询 | `evidence.event.view` |
| `GET` | `/api/v1/evidence/timelines/{object_type}/{object_id}` | 业务时间线 | `evidence.timeline.view` |
| `POST` | `/api/v1/evidence/attachments/upload-intents` | 申请证据附件预签名上传 | `evidence.attachment.create` |
| `POST` | `/api/v1/evidence/attachments/{attachment_id}/complete` | 完成上传确认 | `evidence.attachment.create` |
| `POST` | `/api/v1/evidence/attachments/{attachment_id}/bind` | 关联业务对象 | `evidence.attachment.create` |
| `POST` | `/api/v1/evidence/reviews` | 风险/异常复核 | `evidence.review.create` |
| `GET` | `/api/v1/analytics/dashboards/p0-overview` | P0 经营总览 | `analytics.dashboard.view` |
| `GET` | `/api/v1/analytics/risk-alerts` | 风险预警 | `analytics.risk_alert.view` |

### 8.12 第三方、开放平台和供应商 Adapter

| 方法 | 路径 | 用途 | 权限/事件 |
|---|---|---|---|
| `POST` | `/api/open/auth/token` | 开放平台获取令牌 | 开放应用凭证 |
| `POST` | `/api/open/d/{endpoint_key}/webhooks/{provider}/{event}` | 第三方/供应商回调 | 签名 + nonce |
| `GET` | `/api/open/d/{endpoint_key}/events/{event_id}` | 回调处理结果查询 | 开放应用权限 |
| `GET/POST` | `/api/v1/integration/provider-adapters` | 供应商适配器查询/创建 | `GET=integration.provider_adapter.view`；`POST=integration.provider_adapter.create` |
| `PUT` | `/api/v1/integration/provider-adapters/{id}` | 更新适配器配置 | `integration.provider_adapter.update` |
| `POST` | `/api/v1/integration/provider-adapters/{id}/test-mock` | mock 契约测试 | `integration.provider_adapter.test` |
| `POST` | `/api/v1/integration/provider-adapters/{id}/mark-production-ready` | 标记生产就绪 | `integration.provider_adapter.production_ready` |

### 8.13 运维、生产前置责任状态

P0 将生产前置责任状态做成轻量系统台账/API，不做复杂运维平台。Runbook 仍是执行依据，系统只负责把阻断项、责任人、计划日期、证据链接、状态和降级方式纳入可查询、可更新、可审计的最小闭环。

| 方法 | 路径 | 用途 | 权限/事件 |
|---|---|---|---|
| `GET` | `/api/v1/ops/production-readiness/items` | 查询生产前置项状态，覆盖 MDM、支付、年龄核验、NAS、密钥轮换、企业微信、第三方联动、160 和 HTTPS/域名 | `ops.production_readiness.view`；`production_readiness.viewed` |
| `PUT` | `/api/v1/ops/production-readiness/items/{id}` | 更新单个前置项责任部门、责任人、状态、计划日期、证据、阻断范围和降级方式 | `ops.production_readiness.manage`；`production_readiness.updated` |
| `POST` | `/api/v1/ops/production-readiness/checks` | 触发生产准入检查；任一阻断项未通过时不得标记真实生产就绪 | `ops.production_readiness.check`；`production_readiness.checked` |

## 9. Webhook 契约

### 9.1 入站 Webhook Header

| Header | 必填 | 说明 |
|---|---:|---|
| `X-Provider-Code` | 是 | 供应商编码 |
| `X-Provider-Event-Id` | 是 | 供应商事件唯一键 |
| `X-Timestamp` | 是 | 请求时间 |
| `X-Nonce` | 是 | 防重放随机串 |
| `X-Signature` | 是 | 供应商签名 |
| `X-Idempotency-Key` | 建议 | 幂等键 |

### 9.2 Webhook 处理结果

| 状态 | 说明 |
|---|---|
| `accepted` | 已接收，异步处理 |
| `processed` | 已处理 |
| `duplicate` | 重复事件，已幂等忽略 |
| `rejected` | 签名、状态或业务校验失败 |
| `pending_review` | 进入人工复核 |

### 9.3 轮询校验

P0 默认 Webhook + 定时轮询校验。支付、退款、票务渠道券码、人脸年龄核验、企业微信同步、设备状态都必须保留轮询或状态查询接口占位。

## 10. 文件和证据上传

证据上传统一走对象存储，不把大文件直接写入业务表。

| 步骤 | 接口 |
|---|---|
| 获取上传授权 | `POST /api/v1/evidence/attachments/upload-intents` |
| 客户端上传文件 | 对象存储预签名地址 |
| 完成上传确认 | `POST /api/v1/evidence/attachments/{attachment_id}/complete` |
| 关联业务对象 | `POST /api/v1/evidence/attachments/{attachment_id}/bind` |

敏感证据必须记录访问审计，普通列表不返回未脱敏原始内容。

## 11. OpenAPI 交付要求

P0 首批编码前必须先输出 `openapi-p0.yaml/json` 契约草案；正式编码后必须由 Spring Boot/springdoc-openapi 输出运行时 OpenAPI，并与契约草案比对。任何路径、权限、错误码、幂等、Webhook 或认证方式差异，必须先回写本文和权限矩阵，再进入联调。

OpenAPI 必须满足：

1. 每个接口有路径、方法、参数、字段级请求体、响应体、错误码。
2. 每个写接口声明 `X-Idempotency-Key`。
3. 每个接口声明权限点、认证上下文、数据范围、允许终端和审计事件。
4. `x-terminal-allowed` 只使用 D5 终端码；设备能力写入 `x-device-capability`，服务端/监控调用写入 `x-invocation-channel`。
5. 每个接口必须显式声明统一错误响应；第三方/开放平台接口额外声明供应商异常和超时响应。
6. 第三方开放接口包含签名示例和失败示例。
7. Webhook 包含重试、重复、签名失败和状态查询示例。
8. 业务拒绝必须可区分“参数错误、权限不足、状态冲突、风险复核、供应商异常”。
9. OpenAPI 输出必须能被测试团队转成接口测试用例。
10. P0 写接口不得引用 `GenericWrite`；如临时保留历史占位 schema，必须标记 deprecated，且不得进入正式编码任务。

## 12. 验收标准

### AC-API-001 契约入口唯一

Given 新开发者、测试或供应商接手 P0 接口，  
When 其阅读当前有效入口和交付包说明，  
Then 必须能找到本文档作为接口契约基线。

### AC-API-002 统一响应和错误码

Given 任一 P0 API 返回成功或失败，  
When 测试团队检查响应，  
Then 必须符合本文统一响应格式和错误码规则。

### AC-API-003 写操作幂等

Given 支付、出票、核销、卡座入场、库存扣减、日结或 Webhook 重复提交，  
When 使用相同幂等键或同一供应商事件号重复请求，  
Then 系统必须返回第一次处理结果，不得重复产生业务影响（同一提交范围内；其他提交范围再用同一幂等键提交同一操作的，拒绝且不返回第一次处理结果，见平台治理 PRD §2A.3）。

### AC-API-004 权限和事件映射

Given 任一 P0 写接口完成成功或失败，  
When 审计人员查询事件中心，  
Then 必须能看到对应成功/失败事件、操作者、终端、设备、业务对象和 trace_id。

### AC-API-005 第三方未确定不阻塞开发

Given 聚合支付、企业微信、人脸年龄核验、机器人、视频、设备或渠道供应商尚未最终确定，  
When 开发 P0 接口，  
Then 必须先完成平台标准接口、Adapter 占位、mock、Webhook 和轮询校验，不得写死供应商私有字段。

### AC-API-006 OpenAPI 可测试

Given P0 首批编码准备开始，  
When 后端、前端、Android、一体机、客人端和供应商联调人员领取接口任务，  
Then 必须已经存在 `openapi-p0.yaml/json` 契约草案，并能从中看到路径、认证方式、权限、请求体、响应体、错误码、幂等和事件字段。

Given Spring Boot 工程输出运行时 OpenAPI，  
When 测试团队按本文抽查任一 P0 模块，  
Then 运行时 OpenAPI 与契约草案、权限矩阵、任务映射表必须一致；如不一致，不得进入联调完成状态。

## 13. 后续补齐清单

| 优先级 | 补齐项 | 说明 |
|---|---|---|
| P0 | OpenAPI YAML/JSON | 已落地 `openapi-p0.yaml/json`，并补齐字段级请求体；编码后由 Spring Boot/springdoc-openapi 输出并比对，作为联调基准 |
| P0 | OpenAPI 首批示例 | 已通过 `npm run docs:openapi-examples` 写入高风险写接口与开放接口首批机器示例；`npm run verify:openapi-examples` 作为 P0 文档闸口，检查开放鉴权、Webhook、购票、核销、订台、酒吧点单、支付、游乐安全提交和供应商 mock 的请求示例，以及统一成功、分页、Webhook 接收响应示例 |
| P0 | operation 级验收矩阵 | 已落地 `p0-openapi-operation-acceptance-matrix-v0.1.md`，作为接口测试、权限测试、幂等测试和事件留痕测试入口 |
| P0 | OpenAPI 权限/事件/任务追溯附录 | 已落地 `p0-openapi-contract-closure-appendix-v0.1.md`，作为 operation 到权限、事件、任务簇、终端和样例准入的完整追溯入口 |
| P0 | 代码契约迁移审计 | 已落地 `code-contract-difference-and-migration-audit-v0.1.md`，旧 Web/Android/API 原型不得反向约束正式接口 |
| P0 | 供应商 mock payload | 支付、企业微信、人脸、设备、渠道至少各一组；首批 Webhook 和供应商适配器 mock 示例进入 OpenAPI 与 `third-party-supplier-integration-kit-v0.1.md`，真实供应商字段到位后只补映射，不改平台主模型 |
| P0 | 调用示例 | 首批以 OpenAPI example + Webhook 信封 + mock adapter payload 为准；curl、Java/JavaScript(Node.js) 签名调用示例用于第三方最小接入，不代表后端技术栈，完整多语言 SDK 包进入 P2 |
| P1 | Postman/接口测试集 | 供测试和供应商联调 |

### 13.1 P0 示例覆盖策略

P0 不要求在尚未进入编码的 operation 上一次性补满全部业务样例，否则会拖慢正式 Java 后端落地。P0 示例覆盖采用“两级准入”：

1. 第一层：保留“高风险写接口与外部接入优先”的首批机器示例，作为当前 OpenAPI 文档最小闸口。
2. 第二层：任一 P0 写接口、外部接入接口、支付/核销/订台/出票/退款/年龄核验/应收应付接口进入 Controller、SDK/mock、接口自动化或供应商联调前，必须补齐请求样例、成功响应样例和主要失败响应样例。

| 示例类别 | 已覆盖 requestBody / response | 覆盖目的 |
|---|---|---|
| 开放平台鉴权 | `OpenAuthTokenRequest` | 第三方能按 app_key、timestamp、nonce、signature 生成最小取 token 请求 |
| 第三方 Webhook | `Webhook`、`WebhookAccepted` | 供应商回调具备事件号、幂等键、原始摘要、平台接收回执样例 |
| 客人购票 | `CustomerTicketOrderRequest` | App/小程序/Web 用于客人自助购票；售票型一体机通过票务侧 staff-token 路径由授权服务人员辅助到店购票 |
| 核销类动作 | `Verification` | 票务核销、卡座入场、年龄核验、库存追踪扫码复用统一核销请求信封 |
| 客人订台 | `CustomerBarReservationRequest` | 订台资源、人数、定金和联系方式进入同一请求形态 |
| 酒吧点单 | `BarOrderRequest` | 卡座、商品、销售归属、服务责任和支付模式可生成 DTO/mock |
| 支付意图 | `PaymentIntent` | 聚合支付优先、保留直连接口抽象，金额统一按分 |
| 游乐安全提交 | `AmusementSafetyActionRequest` | 开放前安全巡检、整改、复检和恢复审批的动作提交可测试 |
| 供应商 mock | `ProviderAdapterTestRequest` | 供应商未定时仍可验证鉴权、Webhook、轮询、对账、字段映射和失败重试 |
| 通用响应 | `Success`、`Page`、`WebhookAccepted` | 自动化测试和供应商 mock 能看到成功、分页、Webhook 接收响应样例 |

后续补齐规则：当某个 P0 operation 准备进入编码或联调，或某个 P1/P2 能力准备进入正式开发时，必须先补对应 OpenAPI example、字段映射和验收矩阵，再进入 Controller、SDK/mock、接口自动化或供应商联调。

## 14. 变更日志

| 日期 | 版本 | 变更 |
|---|---|---|
| 2026-06-10 | V0.1 | 新增 P0 API 契约基线，覆盖统一路径、鉴权、响应、错误码、幂等、权限事件映射、P0 接口目录、Webhook、证据上传、OpenAPI 交付和验收标准 |
| 2026-06-10 | V0.1 补充 | 游乐安全巡检接口补齐开放准入查询、巡检提交、整改、复检和恢复审批；移除已完成的任务映射表待补项 |
| 2026-06-10 | V0.1 审计收口 | 将 `Authorization` 从全局必填改为按接口认证矩阵执行；权限码以权限矩阵为唯一事实源；证据附件上传统一为预签名三段式；OpenAPI YAML/JSON 改为 P0 首批编码前置契约 |
| 2026-06-11 | V0.1 可编码补强 | OpenAPI 写接口补齐字段级 requestBody；非员工主体从 `x-permission` 拆到 `x-auth-context`；新增 operation 级验收矩阵和旧原型迁移闸口 |
| 2026-06-11 | V0.1 终端枚举收口 | 明确 `X-Terminal-Type` 和 `x-terminal-allowed` 只能使用 D5 10 个终端码；设备能力使用 `x-device-capability`，服务端/监控调用使用 `x-invocation-channel`；所有接口需显式统一错误响应 |
| 2026-06-12 | V0.1 可编码准入收口修复 | OpenAPI 当时 178 个 operation 均显式声明 `x-auth-context`；补齐 `LogoutRequest` 与 `DeviceUnbindRequest`；一体机配置和心跳统一使用 `device-credential` 且 `x-permission=not_applicable`；字段说明从占位描述改为可指导 DTO、校验、测试和供应商 mock 的业务语义，并补齐字符串长度、数值边界、数组上限和公共路径参数说明；新增客人端订台资源读取接口 `GET /api/v1/customer/bar-reservation-options`；客户端覆盖闸口升级为路径模板 + HTTP 方法一致性 |
| 2026-06-12 | V0.1 OpenAPI 可读文本修复 | 修复 OpenAPI 中残留的连续问号摘要、描述和参数说明；新增 `docs:repair-openapi-text` 与 `docs:operation-matrix` 作为契约文档再生成入口，确保供应商 mock、测试用例和人工审阅不会依赖乱码占位 |
| 2026-06-12 | V0.1 operation 矩阵同步闸口 | 新增 `verify:operation-matrix`，要求 `p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `openapi-p0.json` 完全同步；OpenAPI 变更但矩阵未重建时不得进入 P0 联调 |
| 2026-06-13 | V0.1 示例闸口补强 | 新增 `docs:openapi-examples` 和 `verify:openapi-examples`；OpenAPI 首批覆盖 9 个高风险请求体示例与 3 个通用响应示例，供应商 mock、接口测试和前后端联调不得再只靠口头样例 |
| 2026-06-13 | V0.1 OpenAPI 权限/事件/任务追溯收口 | 新增 `p0-openapi-contract-closure-appendix-v0.1.md` 和 `verify:openapi-closure`；接口级权限、事件、任务簇、终端和样例准入以 OpenAPI 反向生成附录收口 |
| 2026-06-13 | V0.1 生产前置台账接口收口 | 新增 `ops` 接口组，补齐生产前置责任状态查询、更新和准入检查三个 operation |
| 2026-10-07 | V0.1 重复提交已完成标记 | 5.1 成功响应补记可选布尔字段 `replayed`：重复提交返回第一次处理结果时为 `true`，第一次执行、失败和查看类请求的响应不带；OpenAPI `SuccessResponse` 同步登记（M725-F03） |
