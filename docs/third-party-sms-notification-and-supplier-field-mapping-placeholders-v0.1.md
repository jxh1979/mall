# 第三方占位补齐：短信/通知 + 对象存储/MDM/视频 字段映射 + 占位错误码 V0.1

> 创建日期：2026-06-18
> 状态：占位规格（供应商真实资料未到位时，使开发能把接口边界实现到可联调/可验收）。补齐第三方审计发现的缺口。
> 关联：`integration-capability-switches-v0.1.md`（开关矩阵）、`third-party-supplier-integration-kit-v0.1.md`（adapter/字段映射/对账/补偿）、`partial-requirement-mock-coverage-v0.1.md`、`docs/supplier-mock/`。

机器识别标记：SMS_NOTIFICATION_PLACEHOLDER_READY、SUPPLIER_FIELD_MAPPING_PLACEHOLDERS_READY。

## 1. 【P0】短信/通知接入占位（此前全维度缺失，而登录链路已硬依赖 sms_code）

业务已依赖短信：新人注册/客户登录 `sms_code`（OpenAPI 已落）、商户催缴 `sms` 渠道、严重风险短信告警。但此前**无开关、无 mock、无失败场景、无字段映射**。补齐如下。

### 1.1 能力开关（应并入 `integration-capability-switches-v0.1.md` 矩阵）
| 开关键 | 默认 | 关闭时行为 |
|---|---|---|
| `sms_notification` | off | 登录验证码走 mock OTP（固定/loopback，禁止生产）；催缴与风险告警降级为 manual/站内记录，不静默丢弃 |

### 1.2 供应商字段映射占位（our ↔ supplier，supplier 值待定）
| our field | 说明 | supplier field（占位）|
|---|---|---|
| phone | 接收号码 | `<vendor.mobile>` |
| template_code | 模板编号 | `<vendor.templateId>` |
| template_params | 模板变量（含 verification_code）| `<vendor.params>` |
| biz_type | 业务类型（login/dunning/risk_alert）| `<vendor.signName/scene>` |
| request_id | 我方幂等键 | `<vendor.outId>` |
| send_status | 发送状态映射 | `<vendor.code → sent/failed/throttled>` |

### 1.3 失败场景（必须实现并可测）
超时（不视为已送达，可重发，幂等 request_id 防重复扣量）、签名/鉴权失败、限流/节流（退避重试 + 告警）、模板不存在、验证码有效期失效（重新获取，不放行）、回执回调缺失（轮询兜底）。

### 1.4 mock 桩（应放 `docs/supplier-mock/`）
短信发送 mock：固定返回 `sent`，可注入 `throttled`/`failed` 触发失败路径；验证码 mock 固定值用于联调。

## 2. 【P1】对象存储 / MDM / 视频 字段映射占位（通用 adapter 已有，但这三类未下沉到逐项）

### 2.1 对象存储（启用 `object_storage` 后）
| our field | supplier field（占位）| our field | supplier field（占位）|
|---|---|---|---|
| bucket | `<vendor.bucket>` | presigned_url | `<vendor.signedUrl>` |
| object_key | `<vendor.key>` | content_hash | `<vendor.etag/md5>` |
| content_type | `<vendor.contentType>` | expires_at | `<vendor.expiry>` |

失败场景：上传超时、签名 URL 过期、桶权限不足、hash 校验不一致。

### 2.2 MDM（启用 `managed_work_devices` 后）
| our field | supplier field（占位）|
|---|---|
| device_serial | `<vendor.serialNumber>` |
| enroll_token | `<vendor.enrollmentToken>` |
| policy_id | `<vendor.policyName>` |
| lock_result / wipe_result | `<vendor.commandResult>` |

失败场景：enroll 超时、锁定/擦除指令失败重试、设备离线指令排队。

### 2.3 视频/监控（启用 `video_platform` 后，补 P0 样例包缺的视频行）
| our field | supplier field（占位）|
|---|---|
| camera_id | `<vendor.deviceId>` |
| play_token | `<vendor.streamToken>` |
| clip_url | `<vendor.playbackUrl>` |
| retention_until | `<vendor.retentionExpiry>` |

失败场景：取流超时（告警+补偿）、播放鉴权失败、回放保留期过期。

## 3. 【P2】占位错误码命名空间（避免开发各自硬编码）

供应商真实错误码未定前，先约定我方标准化命名空间，映射时填供应商码：

| 命名空间 | 用途 | 示例 |
|---|---|---|
| `PAY_*` | 支付/聚合支付 | `PAY_TIMEOUT`、`PAY_SIGN_INVALID`、`PAY_RECONCILE_DIFF` |
| `SMS_*` | 短信/通知 | `SMS_THROTTLED`、`SMS_TEMPLATE_NOT_FOUND`、`SMS_CODE_EXPIRED` |
| `FACE_*` | 人脸/年龄 | `FACE_TIMEOUT`、`FACE_LOW_LIVENESS`、`FACE_MANUAL_REVIEW` |
| `ROBOT_*` / `DEVICE_*` | 机器人/设备 | `DEVICE_OFFLINE`、`ROBOT_TASK_FAILED` |
| `VIDEO_*` | 视频 | `VIDEO_STREAM_TIMEOUT`、`VIDEO_AUTH_FAILED` |
| `STORAGE_*` | 对象存储 | `STORAGE_PRESIGN_EXPIRED`、`STORAGE_HASH_MISMATCH` |
| `MDM_*` | 设备管控 | `MDM_ENROLL_TIMEOUT`、`MDM_CMD_FAILED` |

开放平台鉴权层错误码仍以 `open-platform-security-auth-development-design.md` 的 16 项为准；本表为各业务能力的占位补充。

## 4. 地图/定位

当前全项目无地图/LBS 业务面（仅商场平面图多边形，已有 `p1-floor-plan-polygon-api-contract-v0.1.md`），**判定为不适用**，不补占位；若后续引入定位/电子围栏需求再补开关与契约。

## 5. 落地动作（供维护者）

> 进度标注（`文档goalP1(2026-06-20)`）：§1.2 / §2 的短信/通知、对象存储、MDM、视频字段映射占位**已并入** `third-party-supplier-integration-kit-v0.1.md` §3.2B（字段映射主表区）；短信类最小回流/发送契约锚点已落 supplier kit §3.2C。本文这些字段表保留为来源与失败场景/错误码命名空间说明，**字段映射以 supplier kit §3.2B/§3.2C 为权威**，避免占位散在独立文档。

- 把 §1.1 `sms_notification` 行并入 `integration-capability-switches-v0.1.md` 与 `integration-capability-switches-latest.json`。（注：开关矩阵 `sms_notification` 行已在 `integration-capability-switches-v0.1.md` §3 落地。）
- ~~把 §1.2 / §2 的字段映射行并入 `third-party-supplier-integration-kit-v0.1.md` 第 3 节 adapter 字段映射表。~~ **已完成**：并入 supplier kit §3.2B；短信最小回流/发送契约锚点见 §3.2C。
- 在 `docs/supplier-mock/` 增短信发送 mock 样例。
- 供应商资料到位后，用真实值替换 `<vendor.*>` 占位与错误码映射。
