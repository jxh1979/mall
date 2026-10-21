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

## 6. 【需求·2026-06-29 用户新增】业务通知事件目录与两级可配开关（待落地）

> 需求：把用户存酒/取酒/存酒过期、储值充值/消费（**不含积分**）、商户房租/水电/物业/续约等重要事件，经第三方短信平台通知对方；后台可配哪些事件发短信。**复用 §1 `sms_notification` 适配层与失败/幂等/回执口径，不另起通道。** 状态：仅需求口径，代码未落地。

### 6.1 通知事件目录

| 域 | 事件 | 接收方 | 备注 |
|---|---|---|---|
| 存酒 | 存酒成功 | 用户 | |
| 存酒 | 取酒成功 | 用户 | |
| 存酒 | 存酒即将到期（提前 N 天，N 可配） | 用户 | |
| 存酒 | 存酒已过期 | 用户 | |
| 储值 | 充值到账 | 用户 | 不含积分 |
| 储值 | 储值消费扣款 | 用户 | **每笔消费都发**（用户 2026-06-29 决定） |
| 储值 | 余额异常 / 退款（可选，后续定） | 用户 | |
| 商户费用 | 房租账单生成 / 到期催缴 | 商户主联系人（后台可改） | |
| 商户费用 | 水电费账单 / 催缴 | 同上 | |
| 商户费用 | 物业费账单 / 催缴 | 同上 | |
| 商户费用 | 合同续约提醒（到期前 N 天） | 同上 | |

### 6.2 两级可配开关（用户决定：域总开关 + 每事件开关）

- **第 0 级**：`sms_notification` 主开关（§1.1，默认关）——关则一律不发。
- **第 1 级**：域总开关 `sms.notify.<domain>`（`stored_wine` / `stored_value` / `merchant_fee`）——关则该域全部事件不发。
- **第 2 级**：每事件开关 `sms.notify.<domain>.<event>`——精确到单个通知。
- **生效规则**：三级 AND——主开关 ON 且 域开关 ON 且 事件开关 ON 才发。
- 开关并入 `integration-capability-switches-v0.1.md` 矩阵（落地时）；储值消费类按用户决定默认开。

### 6.3 接收方口径

- 用户类（存酒 / 储值）：发用户注册手机（需手机有效 + 已同意短信）。
- 商户费用类：默认发商户档案**主联系人**手机，**后台每类账单可改接收人**（用户决定）。

### 6.4 复用与边界

- 发送复用 §1：幂等 `request_id` 防重复扣量、失败场景（超时/限流/模板缺失/回执兜底）、`SMS_*` 错误码命名空间（§3）。
- 模板：每事件一个 `template_code`，变量含金额 / 到期日 / 铺位 / 账单类型等。
- 合规：均为**交易类**通知（非营销）；关键催缴/到期类不可退订，其余可预留退订位（后续定）。
- 域数据触点：存酒 = `saved_bottles`；储值 = 储值账户/流水（不含积分）；商户费用 = `merchant_bills` / 合同续约（`contract_key_dates`）。
- ⚠️ 业务 HTML 为最终解释权，此为新增业务能力；是否写进业务 HTML 待用户定，勿擅改 HTML。

## 7. 【2026-06-30】各事件短信模板文案草稿(供运营商报备拿模板ID)

> 用户拍板:暂不接真供应商(适配器留占位、按第三方接口规则),先由本文起草各事件中文文案。
> 国内短信为"模板报备制":下表文案拿到阿里云/腾讯云短信后台报备审核,通过后得到模板ID,
> 回填到对应 templateCode 的映射(真供应商接入时用,替换 UnconfiguredSmsSender)。
> 签名 `【综合体】` 为占位,报备时替换为实际签名;`${变量}` 为模板变量,由通知发送参数填充。

### 7.1 存酒(收件人=客人)

| templateCode | 事件 | 文案草稿 | 变量 |
|---|---|---|---|
| sms.stored_wine.deposit | 存酒成功 | 【综合体】您的${wine}已成功存入,到期日${expireDate},请在到期前取用。 | wine、expireDate |
| sms.stored_wine.claim | 取酒成功 | 【综合体】您已成功取出${wine},如非本人操作请联系门店。 | wine |
| sms.stored_wine.expiring | 即将到期 | 【综合体】您寄存的${wine}将于${expireDate}到期,请尽快取用,逾期视为放弃。 | wine、expireDate |
| sms.stored_wine.expired | 已过期 | 【综合体】您寄存的${wine}已于${expireDate}到期,如有疑问请联系门店。 | wine、expireDate |

### 7.2 储值(收件人=客人,不含积分)

| templateCode | 事件 | 文案草稿 | 变量 |
|---|---|---|---|
| sms.stored_value.topup | 充值到账 | 【综合体】您已成功充值${amount}元,当前余额${balance}元。 | amount、balance |
| sms.stored_value.consume | 消费扣款 | 【综合体】您本次消费${amount}元,储值余额${balance}元。 | amount、balance |

### 7.3 商户费用(收件人=商户主联系人或后台配置接收人)

| templateCode | 事件 | 文案草稿 | 变量 |
|---|---|---|---|
| sms.merchant_fee.rent | 房租账单/催缴 | 【综合体】您${period}房租账单${amount}元,请于${dueDate}前缴纳。 | period、amount、dueDate |
| sms.merchant_fee.utilities | 水电费账单/催缴 | 【综合体】您${period}水电费账单${amount}元,请于${dueDate}前缴纳。 | period、amount、dueDate |
| sms.merchant_fee.property | 物业费账单/催缴 | 【综合体】您${period}物业费账单${amount}元,请于${dueDate}前缴纳。 | period、amount、dueDate |
| sms.merchant_fee.renewal | 合同续约提醒 | 【综合体】您的租赁合同将于${expireDate}到期,请及时办理续约。 | expireDate |

### 7.4 备注

- 催缴(dunning)与账单生成(bill)当前共用同一费用事件模板;若运营希望催缴语气更强(如"逾期将影响经营"),可单独报备一套催缴模板并在映射区分,本期先共用。
- 变量名以业务可读为准,真供应商接入时按其模板变量命名(${vendor.params})做一次映射,见 §1.2 字段映射占位。
- 凭证(AccessKey/Secret/签名)按第三方接口规则注入(环境变量/配置占位),不得硬编码,见 supplier kit。
- 模板ID回填位置:真供应商适配器替换 UnconfiguredSmsSender 时,templateCode → 运营商模板ID 的映射表(待建)。

