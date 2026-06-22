# 集成能力开关矩阵 V0.1

## 1. 机器标记

- INTEGRATION_CAPABILITY_SWITCHES_READY
- DEFAULT_EXTERNAL_INTEGRATIONS_OFF
- DISABLED_CAPABILITY_DOES_NOT_REQUIRE_INTERFACE
- ENABLED_CAPABILITY_REQUIRES_REAL_INTERFACE_EVIDENCE
- LOCAL_AI_LLM_ON_160_REPLACES_EXTERNAL_AI_FOR_NOW
- NAS_BACKUP_REMAINS_REQUIRED_UNTIL_EQUIVALENT_RECOVERY_TARGET_EXISTS

## 2. 结论

本矩阵把第三方接口、设备、对象存储、开放平台、企业微信、工作手机纳管和 AI 能力统一改成“开关化”生产准入口径。默认除 160 本地 AI 与 NAS 备份恢复外，外部集成全部关闭。关闭表示不读取外部接口、不要求供应商密钥和生产 evidence；但对应业务能力也必须降级、隐藏或走人工替代流程，不能一边关闭接口一边宣称自动能力已经上线。

## 3. 开关矩阵

| capability_id | 能力 | 默认 | 需真实接口 | 关联证据项 | 关联安全项 | 关闭后的业务口径 | 开启后的要求 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| aggregate_payment | 聚合支付 | off | no | PRD-PAY-001, PRD-PAY-002, PRD-CRED-3P-001 | SEC-P1-WEBHOOK-REPLAY-001 | 关闭后不读取聚合支付接口，不要求商户号、回调、退款或对账文件；支付仅允许现金、线下 POS 刷卡或人工收款确认，并写入审计留痕。 | 开启后必须补齐商户号、回调验签、退款权限、对账文件、credential_ref、失败补偿和生产证据。 |
| groupbuy_ticket_channel | 团购/渠道售票 | off | no | PRD-3P-001, PRD-CRED-3P-001 | SEC-P1-WEBHOOK-REPLAY-001 | 关闭后不接美团/大众点评/抖音等渠道订单、票码、退款和对账接口；仅支持平台自出票和后台手动补录渠道信息。 | 开启后必须补齐渠道 API/SDK、券码核销、订单回流、退款同步、对账口径和生产证据。 |
| wecom | 企业微信 | off | no | PRD-WECOM-001, PRD-CRED-3P-001 | SEC-P1-WEBHOOK-REPLAY-001 | 关闭后不读取企业微信通讯录、会话存档、客户联系或回调；员工账号由后台手动录入，质检仅使用平台内数据。 | 开启后必须补齐企业微信应用、通讯录、会话存档、回调、同步任务、告警和 credential_ref。 |
| bar_face_age | 酒吧人脸/年龄核验 | off | no | PRD-FACE-001, PRD-CRED-3P-001 | SEC-P1-WEBHOOK-REPLAY-001 | 关闭后不调用第三方人脸/年龄接口；酒吧入场必须走人工证件核验、人工复核和审计留痕，不能宣称已完成自动人脸核验。 | 开启后必须补齐供应商 SDK/API、成功/失败样例、错误码、合规材料、测试账号和 credential_ref。 |
| managed_work_devices | 工作手机/平板/一体机 MDM 纳管 | off | no | PRD-MDM-001 | SEC-P0-MDM-DEVICE-001 | 关闭后不允许工作手机、平板、一体机执行高风险核销、出酒、支付相关动作；相关操作只能在受控后台或人工流程内完成。 | 开启后必须补齐 Android Enterprise/商业 MDM 选型、enroll、策略下发、远程锁定/擦除测试和设备绑定证据。 |
| object_storage | 对象存储/附件证据桶 | off | no | PRD-CRED-OBJ-001 |  | 关闭后不要求 MinIO/S3 等对象存储接口；附件和证据只允许记录外部受控证据编号或手工 evidence_link，不提供平台内文件上传/签名 URL 能力。 | 开启后必须补齐桶、权限、Access Key 引用、上传下载、签名 URL、备份和轮换证据。 |
| open_platform | 开放平台/第三方应用接入 | off | no | PRD-CRED-OPEN-001, PRD-3P-001, PRD-CRED-3P-001 | SEC-P1-WEBHOOK-REPLAY-001 | 关闭后不开放第三方应用 app_key/app_secret、动态 endpoint、Webhook 入站或供应商主动调用平台；仅保留内部 Adapter/mock。 | 开启后必须补齐开放应用、allowed scopes、签名密钥、nonce、防重放、Webhook 验签和轮换证据。 |
| catering_pos | 餐饮 POS | off | no | PRD-3P-001, PRD-CRED-3P-001 | SEC-P1-WEBHOOK-REPLAY-001 | 关闭后不接餐饮 POS 点单、收银、券核销、退款、订单回流或对账接口；餐饮相关数据只支持后台手工登记或不启用。 | 开启后必须补齐餐饮 POS API/SDK、订单/退款/券核销字段、Webhook/轮询、对账文件和 credential_ref。 |
| retail_pos | 零售 POS | off | no | PRD-3P-001, PRD-CRED-3P-001 | SEC-P1-WEBHOOK-REPLAY-001 | 关闭后不接零售 POS 商品/SKU、收银、券核销、退款、订单回流或对账接口；零售相关数据只支持后台手工登记或不启用。 | 开启后必须补齐零售 POS API/SDK、SKU/订单/退款/券核销字段、Webhook/轮询、对账文件和 credential_ref。 |
| robot_platform | 机器人平台 | off | no | PRD-3P-001, PRD-CRED-3P-001 | SEC-P1-WEBHOOK-REPLAY-001 | 关闭后不下发机器人任务、不接机器人状态、不要求机器人 API；配送和交接按人工流程留痕。 | 开启后必须补齐机器人任务 API、状态回调、签名、异常补偿、测试账号和生产证据。 |
| video_platform | 视频监控平台 | off | no | PRD-3P-001, PRD-CRED-3P-001 | SEC-P1-WEBHOOK-REPLAY-001 | 关闭后不读取摄像头、录像、截图、片段链接或播放 token；系统只记录人工 evidence_link，不做视频内嵌播放或自动关联。 | 开启后必须补齐摄像头点位、播放鉴权、截图/片段接口、事件关联、审计回调和 credential_ref。 |
| gate_nfc_scan_device | 闸机/NFC/扫码设备 | off | no | PRD-3P-001, PRD-CRED-3P-001 | SEC-P1-WEBHOOK-REPLAY-001 | 关闭后不接闸机、NFC 读卡器、扫码枪或设备网关接口；票务/权益核销只能走后台或人工扫码录入。 | 开启后必须补齐设备协议/API、核销校验、开闸结果、离线队列、设备事件、签名和补偿证据。 |
| local_ai_llm | 160 本地 AI 大模型 | on | no |  |  | 关闭后只使用 Java 内置规则处理器。 | 开启后使用 160 本地大模型作为辅助分析，不调用外部云 AI，不允许 AI 直接做付款、核销、处罚、退款或员工绩效最终决策。 |
| nas_smb_backup | NAS SMB 备份恢复 | on | yes | PRD-CRED-NAS-001, PRD-NAS-AUTOMOUNT-001, PRD-NAS-SCHEDULE-001, PRD-NAS-ALERT-001, PRD-NAS-DRILL-CADENCE-001 | SEC-P0-OPS-RECOVERY-001 | 不建议关闭。若关闭，必须先指定等效备份恢复目标；否则不能宣称生产可恢复。 | 开启后必须固化 //192.168.88.113/bn 到 160 本地挂载点、定时备份、容量/失败告警和周期性恢复演练。 |
| sms_notification | 短信通知 | off | no | PRD-CRED-3P-001 |  | 关闭后不调用真实短信供应商；登录验证码走 mock OTP loopback（非生产固定码 000000），生产无供应商时 fail-closed 拒绝登录；催缴、告警等短信能力降级为人工或站内留痕。 | 开启后必须补齐短信供应商签名/模板报备、API/SDK、credential_ref、发送回执、限频与失败补偿和生产证据。 |

> **人脸/年龄能力（`bar_face_age`）的两个具体开关**（2026-06-22 P0-4/P0-5 落地）：`face.verification.enabled`（V146，entry 域入场闸实时年龄/人脸核验裁决，bar 售酒继承其结论）与 `integration.face_age.sync`（V071，T14 第三方供应商数据对账/同步）。二者职责不同、独立开关（与 payment 的 `payment.gateway.enabled` vs `integration.payment.sync` 同范式），本期均默认关。入场闸关 → bar 售酒年龄核验**继承「入场闸豁免」**（`provider_code=entry_gate_exempt`）非阻塞放行 + 制度兜底，**不展示「自动人脸已通过」**（对齐本文 §5 验收 #6，修复 X-03 客户端自报缺省 passed）；接入供应商 + 开启后入场闸 fail-closed 判龄，bar 自动继承 PASSED/FAILED（`face_inherited`）。

## 4. 后台手动工作人员录入需求

新增后台用户录入渠道：平台管理员、IT 管理员或 HR 可在后台手动创建工作人员账号，录入员工账号、手机号、组织、商户、岗位/角色、权限模板、临时权限时效或单次有效设置，并写入审计事件。企业微信关闭时，后台手动录入是默认员工入口；企业微信开启后，企业微信同步结果只能作为辅助来源，不能绕过后台权限审批和留痕。

## 5. 验收标准

1. Given 某能力开关为关闭，When 运行生产阻断和外部证据追踪，Then 该能力对应的供应商接口、密钥、Webhook、轮询和生产 evidence 不再构成上线阻断。
2. Given 某能力开关为关闭，When 用户访问对应后台入口，Then 页面必须显示关闭/未启用状态，写接口不得伪调用外部供应商。
3. Given 某能力开关为开启，When 进入生产准入检查，Then 必须要求真实 SDK/API、credential_ref、Webhook/轮询、对账/补偿、失败样例和生产证据。
4. Given 企业微信关闭，When 创建工作人员，Then 后台手动录入、角色或权限模板分配、审计留痕必须可完成。
5. Given 工作手机/平板/一体机 MDM 关闭，When 执行高风险核销、出酒或支付动作，Then 移动终端不得放行，必须转后台或人工流程。
6. Given 酒吧人脸/年龄核验关闭，When 酒吧入场，Then 系统只能记录人工证件核验结果，不能展示自动人脸核验已通过。

## 6. 餐饮 POS 和零售 POS 解释

餐饮 POS/零售 POS 不是“只刷卡”。刷卡或扫码收款属于支付通道；餐饮 POS/零售 POS 是商户自己的点单、收银、退款、券核销、SKU/商品、订单流水和对账系统。本项目默认不接餐饮/零售 POS。只有对应开关打开后，才需要供应商 API/SDK、字段映射、Webhook、轮询和对账文件。
