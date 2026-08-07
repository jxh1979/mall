# 第三方供应商接入预留包 V0.1

> 本文档用于在供应商尚未确定前，先把平台的稳定边界、接口占位、字段映射方式、Webhook/轮询策略、对账和异常补偿规则固定下来。供应商 SDK/API 到位后，只补具体字段、签名细节和适配器实现，不改变业务主模型。

## 1. 接入原则

1. 第三方能力采用“双向开放”：
   - 平台对外开放统一 OpenAPI / Webhook / SDK 示例，供第三方调用平台能力。
   - 平台对内通过供应商适配器接入聚合支付、企业微信、人脸核验、机器人、视频、设备、票务渠道等成熟服务。
2. 业务主模型保持平台稳定字段，不直接依赖供应商私有字段。
3. 供应商私有字段进入 `provider_payload`、字段映射表或适配器配置，不进入核心业务表的判断逻辑。
4. P0 默认 Webhook 实时处理 + 定时轮询校验；只支持轮询的供应商必须提高轮询频率和异常告警。
5. 所有供应商接入必须具备鉴权、签名、幂等、重试、对账、补偿、审计和密钥轮换。
6. 原始供应商报文 P0 默认保存摘要和必要业务键；确需留存必要原文时，必须字段级脱敏后加密保存，密钥引用走人工台账、到期提醒和 Runbook，不允许把未脱敏原文写入业务事件或核心业务表。
7. P0 正式接口、请求体、权限、认证上下文、终端、幂等和事件以 `docs/openapi/openapi-p0.yaml/json` 与 `p0-openapi-operation-acceptance-matrix-v0.1.md` 为准；开放应用凭证和 Webhook 签名写入 `x-auth-context`，不得作为员工权限码配置。
8. P0 供应商 mock、SDK 示例、接口自动化和联调文档必须能在 `p0-openapi-contract-closure-appendix-v0.1.md` 追溯 operation、任务簇、权限、事件、终端/调用方和样例准入；`verify:openapi-closure` 未通过时不得推进真实供应商联调。

## 2. 平台对外开放接口

| 能力 | 统一路径 | 说明 |
|---|---|---|
| 获取令牌 | `POST /api/open/auth/token` | 统一鉴权入口 |
| 动态业务 API | `/api/open/d/{endpoint_key}/{domain}/...` | endpoint_key 由平台分配 |
| 第三方回调入口 | `POST /api/open/d/{endpoint_key}/webhooks/{provider}/{event}` | 统一签名、幂等、重试 |
| 回调结果查询 | `GET /api/open/d/{endpoint_key}/events/{event_id}` | 供第三方排查 |
| 业务状态查询 | `GET /api/open/d/{endpoint_key}/{domain}/{object_id}` | 按授权范围开放 |

执行口径：P0 已进入 OpenAPI 的对外接口为 `openAuthToken`、`receiveOpenWebhook`、`getOpenEventResult`。其他动态业务 API 和业务状态查询属于供应商资料到位后的开放能力扩展，必须先补 OpenAPI、字段映射和验收矩阵后开发。

## 3. 平台接入供应商适配器

### 3.1 适配器统一能力

| 适配器能力 | 必须 | 说明 |
|---|---:|---|
| `capability_code` | 是 | payment / wecom / face / robot / video / device / ticket_channel |
| `provider_code` | 是 | 供应商确定后配置 |
| `sign_request` | 是 | 出站请求签名 |
| `verify_callback` | 是 | 入站回调验签 |
| `normalize_payload` | 是 | 私有报文转平台标准模型 |
| `map_status` | 是 | 私有状态转平台统一状态 |
| `query_remote_status` | 是 | 定时轮询校验 |
| `reconcile` | 是 | 对账或状态比对 |
| `compensate` | 是 | 异常补偿 |
| `rotate_credential` | 是 | 密钥轮换 Runbook |

### 3.2 标准字段映射占位

| 平台字段 | 供应商字段 | 状态 |
|---|---|---|
| `provider_order_id` | 待供应商 SDK/API 确认 | 占位 |
| `provider_transaction_id` | 待供应商 SDK/API 确认 | 占位 |
| `provider_user_id` | 待供应商 SDK/API 确认 | 占位 |
| `provider_status` | 待供应商 SDK/API 确认 | 占位 |
| `callback_event_id` | 待供应商 SDK/API 确认 | 占位 |
| `signature` | 待供应商 SDK/API 确认 | 占位 |
| `raw_payload_hash` | 平台生成 | 已定 |
| `raw_payload_encrypted_ref` | 平台生成 | 必要原文加密留存引用；默认可空 |
| `normalized_status` | 平台统一状态 | 已定 |
| `idempotency_key` | 平台或供应商唯一键 | 已定 |

供应商确定后必须新增字段映射表，不得在 PRD 文本中散写临时映射。

### 3.2B 短信/通知、对象存储、MDM、视频字段映射占位（并入主表，原散在占位文档）

> 新增小节（`文档goalP1(2026-06-20)`）：原 `third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md` §1.2/§2 把短信/通知、对象存储、MDM、视频四类字段映射占位单独成文，导致占位散在独立文档、与本 kit §3.2 主表脱节。现按该占位文档 §5"落地动作"把四类逐项字段映射并入本 kit 字段映射主表（§3.2 区）；占位文档保留为来源与失败场景/错误码命名空间说明，字段映射以本节为权威。供应商私有字段（`<vendor.*>`）经适配器 `request_mapping/response_mapping` 占位，真实值到位后替换，不改业务主模型。

#### 3.2B.1 短信/通知（开关 `sms_notification`，默认 off）

| 平台字段 | 供应商字段（占位）| 说明 | 状态 |
|---|---|---|---|
| `phone` | `<vendor.mobile>` | 接收号码 | 占位 |
| `template_code` | `<vendor.templateId>` | 模板编号 | 占位 |
| `template_params` | `<vendor.params>` | 模板变量（含 verification_code） | 占位 |
| `biz_type` | `<vendor.signName/scene>` | 业务类型（login/dunning/risk_alert） | 占位 |
| `request_id` | `<vendor.outId>` | 我方幂等键 | 占位 |
| `send_status` | `<vendor.code → sent/failed/throttled>` | 发送状态映射 | 占位 |

#### 3.2B.2 对象存储（开关 `object_storage`，默认 off）

| 平台字段 | 供应商字段（占位）| 平台字段 | 供应商字段（占位）|
|---|---|---|---|
| `bucket` | `<vendor.bucket>` | `presigned_url` | `<vendor.signedUrl>` |
| `object_key` | `<vendor.key>` | `content_hash` | `<vendor.etag/md5>` |
| `content_type` | `<vendor.contentType>` | `expires_at` | `<vendor.expiry>` |

#### 3.2B.3 MDM 工作设备纳管（开关 `managed_work_devices`，默认 off）

| 平台字段 | 供应商字段（占位）|
|---|---|
| `device_serial` | `<vendor.serialNumber>` |
| `enroll_token` | `<vendor.enrollmentToken>` |
| `policy_id` | `<vendor.policyName>` |
| `lock_result` / `wipe_result` | `<vendor.commandResult>` |

#### 3.2B.4 视频/监控（开关 `video_platform`，默认 off；补 §3.2A 样例包缺的视频行）

| 平台字段 | 供应商字段（占位）|
|---|---|
| `camera_id` | `<vendor.deviceId>` |
| `play_token` | `<vendor.streamToken>` |
| `clip_url` | `<vendor.playbackUrl>` |
| `retention_until` | `<vendor.retentionExpiry>` |

> 四类失败场景与错误码命名空间（`SMS_*`/`STORAGE_*`/`MDM_*`/`VIDEO_*`）见占位文档 §1.3/§2/§3；开放平台鉴权层错误码仍以 `open-platform-security-auth-development-design.md` §15 为准。

### 3.2C 短信类供应商最小回流/发送契约锚点

> 新增小节（`文档goalP1(2026-06-20)`）：短信与支付/票务等"业务回流"型供应商不同——短信是**出站发送 + 回执回流**模型。本节固化短信类最小发送/回流契约锚点，使 mock adapter 与真实供应商共用同一标准模型；登录链路已硬依赖 `sms_code`，关闭时走 mock OTP（非生产固定码），生产无供应商时 fail-closed 拒绝登录（与开关矩阵 `sms_notification` 一致）。

| 契约环节 | 锚点 | 口径 |
|---|---|---|
| 出站发送 | `sendSms(phone, template_code, template_params, biz_type, request_id)` → `{ send_status, provider_msg_id }` | `request_id` 为我方幂等键（`<vendor.outId>`），同 `request_id` 重发不重复扣量；模板变量含验证码时按敏感字段处理 |
| 状态映射 | `<vendor.code> → sent / failed / throttled` | 供应商发送码映射到平台标准 `send_status`；`throttled` 触发退避重试 + 告警 |
| 回执回流 | 回调入站或轮询兜底落标准回执 `{ request_id, provider_msg_id, delivery_status, delivered_at }` | 回执缺失时按轮询兜底（占位文档 §1.3）；回执验签复用开放平台 Webhook 验签 |
| 失败场景 | 超时（不视为已送达，可重发）、签名/鉴权失败、限流/节流、模板不存在、验证码有效期失效（重新获取不放行）、回执缺失 | 失败码走 `SMS_*` 命名空间（`SMS_THROTTLED`/`SMS_TEMPLATE_NOT_FOUND`/`SMS_CODE_EXPIRED`） |
| mock 桩 | `docs/supplier-mock/` 短信发送 mock | 固定返回 `sent`，可注入 `throttled`/`failed` 触发失败路径；验证码 mock 固定值用于联调 |

> 短信回流不进 §6 经营对账（短信非经营流水），但发送量/失败率进运营监控；催缴/风险告警短信关闭时降级为 manual/站内记录，不静默丢弃。

### 3.2A P0 最小供应商样例包

P0 供应商未确定时，不等待真实 SDK/API。以下样例包用于 mock adapter、接口自动化和供应商联调说明；真实供应商到位后，只把“供应商字段”列替换为真实字段映射，平台标准字段和业务主模型不变。

| 类别 | 平台标准字段最小集 | 供应商字段占位 | 成功判定 | 异常补偿 |
|---|---|---|---|---|
| 聚合支付 | `provider_order_id`、`provider_transaction_id`、`amount_cent`、`currency`、`normalized_status`、`callback_event_id`、`raw_payload_hash` | `provider_order_id`、`transaction_id`、`pay_status`、`paid_at` | Webhook 入账 + 定时轮询状态一致 | 状态不一致进入支付差异队列，财务复核后补偿 |
| 企业微信 | `provider_user_id`、`external_user_id`、`message_id`、`occurred_at`、`raw_payload_hash`、`normalized_status` | `userid`、`external_userid`、`msgid`、`msgtime` | 每日同步批次完成，上传状态可查 | 漏传或失败批次进入补跑任务 |
| 人脸/年龄核验 | `provider_user_id`、`verification_id`、`age_result`、`face_result`、`normalized_status`、`raw_payload_hash` | `person_id`、`verify_id`、`age`、`liveness_score`、`result_code` | 年龄/人脸结果写入综合体入场年龄核验环节判断；供应商未配置不得生产通过 | 供应商不可用时进入人工抽查放行、客人继续进场（不要求逐人查验证件，但应当对明显疑似未成年的人员进行查验） |
| 设备厂商 | `device_id`、`provider_status`、`callback_event_id`、`normalized_status`、`raw_payload_hash` | `sn`、`device_status`、`event_id`、`reported_at` | 设备状态、心跳或事件可进入终端开关和设备台账 | 心跳超时进入设备异常告警 |
| 票务渠道 | `provider_order_id`、`provider_transaction_id`、`provider_status`、`voucher_code_hash`、`normalized_status`、`raw_payload_hash` | `order_id`、`voucher_code`、`deal_status`、`verify_status` | 渠道订单/券码回流后由平台核销，不绕过平台 | 渠道漏发通过轮询补齐，券码冲突进入异常复核 |

统一 Webhook 示例信封以 `docs/openapi/openapi-p0.yaml/json` 中 `Webhook` example 为准；供应商 mock adapter 测试以 `ProviderAdapterTestRequest` example 为准。文档正文不得再复制多份互相漂移的 payload 样例。

### 3.3 P0 管理接口占位

| 方法 | 路径 | 用途 |
|---|---|---|
| `GET` | `/api/v1/integration/provider-adapters` | 查询供应商适配器、能力、环境、mock 状态和生产就绪状态 |
| `POST` | `/api/v1/integration/provider-adapters` | 创建 mock 或真实供应商适配器占位 |
| `PUT` | `/api/v1/integration/provider-adapters/{id}` | 更新字段映射占位、Webhook/轮询开关、密钥引用和状态 |
| `POST` | `/api/v1/integration/provider-adapters/{id}/test-mock` | 运行 mock adapter 契约测试 |
| `POST` | `/api/v1/integration/provider-adapters/{id}/mark-production-ready` | 标记生产就绪，必须由 IT/采购/法务资料齐备后执行 |

生产密钥、证书、供应商账号不进入文档正文；只保存保管位置、负责人、到期日和轮换 Runbook 引用。

## 4. 各类供应商 P0 边界

| 类别 | P0 做什么 | P0 不做什么 | 供应商资料到位后补齐 |
|---|---|---|---|
| 聚合支付 | 支付、退款、回调、轮询、对账、异常补偿 | 不同时启用多套直连通道 | API 字段、签名、费率、结算单格式 |
| 企业微信 | 组织/账号同步、消息通知、低管理成本自动配置 | 不做复杂自研 IM | 通讯录字段、回调事件、同步频率 |
| 人脸/年龄核验 | 酒吧年龄强制核验结果接入 | 平台不自设原始人脸照片保留开关（原始人脸照片是否保留，取决于第三方人脸识别 SDK 允不允许——允许保留的按已满 18 岁／未满 18 岁两档分类保留，不允许保留的一律不保留；合规 §2.2） | 活体字段、年龄结果、失败码、合规材料 |
| 机器人 | 指令下发、任务状态、异常回调 | 不自研机器人调度算法 | 设备 API、签名、回调、状态码 |
| 视频平台 | 设备/通道、回放/取证链接、告警回调 | 不自研视频底层平台 | 取流/回放 API、告警字段、权限模型 |
| 设备厂商 | 闸机/一体机/外设状态、指令和回调 | 不写死单一厂商协议 | 设备编号、指令集、回调格式 |
| 票务渠道 | 团购订单回流、券码核销、对账 | 不让渠道绕过平台核销 | 订单字段、券码字段、对账口径 |

## 5. Webhook + 轮询策略

| 策略 | 优点 | 缺点 | P0 结论 |
|---|---|---|---|
| 只用 Webhook | 实时、成本低 | 供应商漏发/网络失败会丢状态 | 不单独采用 |
| 只用轮询 | 可控、容易补偿 | 延迟高、接口成本高 | 仅供应商不支持 Webhook 时兜底 |
| Webhook + 定时轮询 | 实时性和可靠性平衡 | 实现复杂度略高 | P0 默认采用 |

轮询频率按业务风险配置：

| 业务 | 建议频率 | 说明 |
|---|---|---|
| 支付/退款 | 1-5 分钟内校验未终态订单 | 影响财务 |
| 票务渠道券码 | 5-15 分钟 | 影响核销与对账 |
| 企业微信组织同步 | 15-60 分钟 | 低实时性 |
| 视频/设备状态 | 1-10 分钟 | 现场设备可按需提高 |
| 人脸核验 | 以实时接口为主 | 不建议事后补过 |

## 6. 对账口径

| 对账对象 | 主口径 | 差异处理 |
|---|---|---|
| 支付 | 平台订单 + 聚合支付交易 + 财务台账 | 差异进入支付对账异常 |
| 退款 | 平台退款单 + 通道退款状态 + 应收应付冲正 | 未终态持续轮询 |
| 票务渠道 | 渠道订单/券码 + 平台核销 + 结算金额 | 渠道差异人工复核 |
| 酒水赠饮 | 平台权益核销 + 酒吧领取记录 + 平台成本承担 | 不计入酒吧成本 |
| 企业微信 | 平台员工/组织 + 企业微信账号 | 差异生成同步任务 |
| 设备/机器人/视频 | 平台指令 + 供应商状态 + 回调记录 | 超时告警并补偿 |

## 7. 异常补偿

| 异常 | 自动处理 | 人工介入 |
|---|---|---|
| Webhook 重复 | 幂等忽略重复业务结果 | 无 |
| Webhook 漏发 | 轮询发现差异后补事件 | 高风险需复核 |
| 签名失败 | 拒绝并告警 | IT 排查 |
| 供应商状态未知 | 持续轮询，超过阈值告警 | 财务/IT 决策 |
| 支付成功但平台失败 | 补建订单或退款，需审批 | 财务复核 |
| 核销成功但库存失败 | 标记业务异常，补偿库存 | 运营/仓库复核 |
| 人脸核验超时 | **进入人工抽查放行、客人继续进场**——不要求逐人查验证件，但应当对明显疑似未成年的人员进行查验，不得把疑似未成年人当作普通未抽查人员直接放行；落事件 + 异常记录 | 酒吧主管异常流程 |

## 8. 账号、密钥与轮换

- P0 采用人工台账 + 到期提醒 + Runbook。
- 每个供应商账号必须记录负责人、用途、环境、权限范围、密钥创建日、到期日、轮换记录。
- 生产密钥不得写入文档正文；文档只记录变量名、保管位置和轮换步骤。
- 密钥轮换必须先灰度验证，再切换生产，最后吊销旧密钥。

## 9. 供应商资料到位后的补齐清单

| 资料 | 用途 | 责任人 |
|---|---|---|
| SDK/API 文档 | 字段映射、签名、错误码 | IT/开发 |
| Webhook 文档 | 回调验签、重试、事件类型 | IT/开发 |
| 查询/对账接口 | 轮询校验、财务对账 | IT/财务 |
| 测试账号 | 联调和自动化验收 | IT/供应商 |
| 生产账号 | 上线配置 | IT/采购 |
| 合规材料 | 人脸、视频、个人信息处理依据 | 法务/IT |
| SLA/故障处理 | 告警、降级、补偿 | 采购/IT/运营 |

## 10. 验收标准

1. 未确定供应商前，系统可以通过 mock adapter 完成接口契约测试。
2. 确定供应商后，只新增字段映射和适配器实现，不修改票务、酒吧、财务等业务主模型。
3. 每个供应商接入必须通过签名失败、重复回调、漏回调、状态不一致、密钥轮换、对账差异六类测试。
4. 第三方调用平台 API 时必须经过统一鉴权和 endpoint 授权。
5. 平台调用供应商 SDK/API 时必须记录原始报文摘要、标准化结果和 trace_id。
6. 供应商 mock、SDK 示例、接口自动化和联调文档必须通过 OpenAPI、operation 验收矩阵和 OpenAPI 追溯附录三方一致性检查。
