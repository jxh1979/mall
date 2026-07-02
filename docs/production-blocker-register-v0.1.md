# 生产阻断清单 V0.1

## 1. 机器标记

- PRODUCTION_BLOCKER_REGISTER_READY
- PRODUCTION_BLOCKERS_OPEN
- NO_FAKE_PRODUCTION_EVIDENCE_CLOSURE
- NO_SUPPLIER_MOCK_AS_PRODUCTION_EVIDENCE
- VERIFY_PRODUCTION_BLOCKERS_READY
- INTEGRATION_CAPABILITY_SWITCHES_APPLIED

## 2. 结论

当前仍存在必须由 IT、采购、财务、运营、安全或供应商回填的生产阻断项。本文已应用 docs/integration-capability-switches-latest.json：开关关闭的外部集成不再要求真实接口或生产证据，但对应自动能力不得启用。本文不重新打开业务决策，不允许把 mock、口头确认、截图占位或未纳管设备当作生产证据。

| 指标 | 数量 |
|---|---:|
| 生产前置台账阻断项 | 6 |
| 生产凭证轮换阻断项 | 4 |
| 安全 findings open / open_for_prod_review | 2 |
| 已暂缓或后续 review 项 | 10 |

## 3. 生产前置台账阻断项

| item_id | category | status | blocking_scope | owner | next_action |
| --- | --- | --- | --- | --- | --- |
| PRD-NAS-AUTOMOUNT-001 | nas | pending_info | 阻塞长期预生产和生产自动恢复；不阻塞开发验证 | IT/运维 | IT 安装后回填重启后 findmnt、读写删除测试和截图 |
| PRD-NAS-SCHEDULE-001 | nas | pending_info | 阻塞长期预生产和生产自动备份；不阻塞手动备份演练 | IT/运维 | IT 安装后回填调度配置、最近一次 scheduled 备份和 .sha256 |
| PRD-NAS-ALERT-001 | nas | pending_info | 阻塞长期预生产和生产运维准入；不阻塞开发验证 | IT/运维/安全 | 回填容量告警、失败告警和通知送达证据 |
| PRD-NAS-DRILL-CADENCE-001 | nas | pending_info | 阻塞长期预生产和生产可恢复结论；不阻塞当前编码 | IT/运维 | 回填最近一次恢复演练报告、临时库清理和验证表 |
| PRD-CRED-001 | credential | pending_info | 阻塞长期预生产和生产 | IT/安全/财务 | 建立密钥引用、轮换周期、责任人和应急回滚 |
| PRD-160-001 | ops160 | pending_info | 阻塞长期预生产和生产；不阻塞用户级覆盖开发验证 | IT/运维 | 迁移用户级覆盖到系统配置，接入 systemd 和告警 |

## 4. 生产凭证轮换阻断项

| credential_item_id | category | status | blocking_scope | owner | next_action |
| --- | --- | --- | --- | --- | --- |
| PRD-CRED-OPS-001 | credential | pending_info | 阻塞长期预生产和生产 | IT/运维/安全 | 建立账号负责人、授权范围、到期日和停用步骤 |
| PRD-CRED-DB-001 | credential | pending_info | 阻塞生产数据库接入 | IT/运维/安全 | 生成强密码、写入安全保管位置、补到期日和维护窗口 |
| PRD-CRED-JWT-001 | credential | pending_info | 阻塞生产登录和开放接口签发 | IT/安全/技术负责人 | 设计签名版本、双验窗口、旧 token 失效计划 |
| PRD-CRED-NAS-001 | credential | pending_info | 阻塞生产可恢复结论 | IT/运维/安全 | IT 将真实密码写入安全保管位置或 160 受控凭证文件，不得写入文档、聊天或仓库 |

## 5. 安全 findings 阻断项

| severity | id | status | blocking_scope | owner | next_action |
| --- | --- | --- | --- | --- | --- |
| P0 | SEC-P0-OPS-RECOVERY-001 | open | 阻塞公开生产安全准入；不阻塞开发联调 | IT/安全/运维 | P0-A no-system-write boundary：Codex 不安装系统级配置；后续由 IT 固化 160 开机自动挂载、定时备份、容量告警、失败告警和周期性恢复演练；生产凭证实际发放前按明细台账补责任人、到期日、credential_ref、提醒和轮换证据 |
| P2 | SEC-P2-CSRF-API-001 | open_for_prod_review | 不阻塞 Bearer/API 调用 smoke；若后台 Web 采用 Cookie 会话则阻塞公开生产 | 后端/前端/安全 | 员工后台仍为 Bearer 头 token（无 cookie 环境凭证）；若后台改用 Cookie 会话再按 SameSite/双提交复核 |

## 6. 暂缓、开关关闭或生产方案定稿复核项

| id | status | blocking_scope | next_action |
| --- | --- | --- | --- |
| PRD-HTTPS-001 | not_started | 仅阻塞公开生产域名访问，不阻塞内网预生产和开发验收 | 等总控启动公开访问后再配置 |
| PRD-MDM-001 | switch_disabled | 对应集成能力开关关闭；不要求真实外部接口或生产证据，但对应自动能力不得启用 | 工作手机/平板/一体机 MDM 纳管 关闭：关闭后不允许工作手机、平板、一体机执行高风险核销、出酒、支付相关动作；相关操作只能在受控后台或人工流程内完成。 |
| PRD-PAY-001 | switch_disabled | 对应集成能力开关关闭；不要求真实外部接口或生产证据，但对应自动能力不得启用 | 聚合支付 关闭：关闭后不读取聚合支付接口，不要求商户号、回调、退款或对账文件；支付仅允许现金、线下 POS 刷卡或人工收款确认，并写入审计留痕。 |
| PRD-PAY-002 | switch_disabled | 对应集成能力开关关闭；不要求真实外部接口或生产证据，但对应自动能力不得启用 | 聚合支付 关闭：关闭后不读取聚合支付接口，不要求商户号、回调、退款或对账文件；支付仅允许现金、线下 POS 刷卡或人工收款确认，并写入审计留痕。 |
| PRD-FACE-001 | switch_disabled | 对应集成能力开关关闭；不要求真实外部接口或生产证据，但对应自动能力不得启用 | 全场入场刷脸闸（满 18 岁核验） 关闭：关闭后不调用第三方人脸/年龄接口；全场入场按制度兜底（人工证件核验/人工复核/审计留痕），售酒环节继承入场核验、不逐单验年龄，不能宣称已完成自动人脸核验。 |
| PRD-WECOM-001 | switch_disabled | 对应集成能力开关关闭；不要求真实外部接口或生产证据，但对应自动能力不得启用 | 企业微信 关闭：关闭后不读取企业微信通讯录、会话存档、客户联系或回调；员工账号由后台手动录入，质检仅使用平台内数据。 |
| PRD-3P-001 | switch_disabled | 对应集成能力开关关闭；不要求真实外部接口或生产证据，但对应自动能力不得启用 | 团购/渠道售票 关闭：关闭后不接美团/大众点评/抖音等渠道订单、票码、退款和对账接口；仅支持平台自出票和后台手动补录渠道信息。；开放平台/第三方应用接入 关闭：关闭后不开放第三方应用 app_key/app_secret、动态 endpoint、Webhook 入站或供应商主动调用平台；仅保留内部 Adapter/mock。；餐饮 POS 关闭：关闭后不接餐饮 POS 点单、收银、券核销、退款、订单回流或对账接口；餐饮相关数据只支持后台手工登记或不启用。；零售 POS 关闭：关闭后不接零售 POS 商品/SKU、收银、券核销、退款、订单回流或对账接口；零售相关数据只支持后台手工登记或不启用。；机器人平台 关闭：关闭后不下发机器人任务、不接机器人状态、不要求机器人 API；配送和交接按人工流程留痕。；视频监控平台 关闭：关闭后不读取摄像头、录像、截图、片段链接或播放 token；系统只记录人工 evidence_link，不做视频内嵌播放或自动关联。；闸机/NFC/扫码设备 关闭：关闭后不接闸机、NFC 读卡器、扫码枪或设备网关接口；票务/权益核销只能走后台或人工扫码录入。 |
| PRD-CRED-OBJ-001 | switch_disabled | 对应集成能力开关关闭；不要求真实外部接口或生产证据，但对应自动能力不得启用 | 对象存储/附件证据桶 关闭：关闭后不要求 MinIO/S3 等对象存储接口；附件和证据只允许记录外部受控证据编号或手工 evidence_link，不提供平台内文件上传/签名 URL 能力。；对象存储/附件证据桶 关闭：关闭后不要求 MinIO/S3 等对象存储接口；附件和证据只允许记录外部受控证据编号或手工 evidence_link，不提供平台内文件上传/签名 URL 能力。 |
| PRD-CRED-3P-001 | switch_disabled | 对应集成能力开关关闭；不要求真实外部接口或生产证据，但对应自动能力不得启用 | 聚合支付 关闭：关闭后不读取聚合支付接口，不要求商户号、回调、退款或对账文件；支付仅允许现金、线下 POS 刷卡或人工收款确认，并写入审计留痕。；团购/渠道售票 关闭：关闭后不接美团/大众点评/抖音等渠道订单、票码、退款和对账接口；仅支持平台自出票和后台手动补录渠道信息。；企业微信 关闭：关闭后不读取企业微信通讯录、会话存档、客户联系或回调；员工账号由后台手动录入，质检仅使用平台内数据。；全场入场刷脸闸（满 18 岁核验） 关闭：关闭后不调用第三方人脸/年龄接口；全场入场按制度兜底（人工证件核验/人工复核/审计留痕），售酒环节继承入场核验、不逐单验年龄，不能宣称已完成自动人脸核验。；开放平台/第三方应用接入 关闭：关闭后不开放第三方应用 app_key/app_secret、动态 endpoint、Webhook 入站或供应商主动调用平台；仅保留内部 Adapter/mock。；餐饮 POS 关闭：关闭后不接餐饮 POS 点单、收银、券核销、退款、订单回流或对账接口；餐饮相关数据只支持后台手工登记或不启用。；零售 POS 关闭：关闭后不接零售 POS 商品/SKU、收银、券核销、退款、订单回流或对账接口；零售相关数据只支持后台手工登记或不启用。；机器人平台 关闭：关闭后不下发机器人任务、不接机器人状态、不要求机器人 API；配送和交接按人工流程留痕。；视频监控平台 关闭：关闭后不读取摄像头、录像、截图、片段链接或播放 token；系统只记录人工 evidence_link，不做视频内嵌播放或自动关联。；闸机/NFC/扫码设备 关闭：关闭后不接闸机、NFC 读卡器、扫码枪或设备网关接口；票务/权益核销只能走后台或人工扫码录入。；短信通知 关闭：关闭后不调用真实短信供应商；登录验证码走 mock OTP loopback（非生产固定码 000000），生产无供应商时 fail-closed 拒绝登录；催缴、告警等短信能力降级为人工或站内留痕。；开票/电子发票（税务机关 / 电子发票平台对接） 关闭：关闭后不对接税务机关/电子发票平台、不自研开票引擎、不生成发票号；开票降级为人工或不启用，财务仅维护「可开票额=已收−已退−已开」口径、不伪造开票结果（finance-settlement PRD §6A）。 |
| PRD-CRED-OPEN-001 | switch_disabled | 对应集成能力开关关闭；不要求真实外部接口或生产证据，但对应自动能力不得启用 | 开放平台/第三方应用接入 关闭：关闭后不开放第三方应用 app_key/app_secret、动态 endpoint、Webhook 入站或供应商主动调用平台；仅保留内部 Adapter/mock。；开放平台/第三方应用接入 关闭：关闭后不开放第三方应用 app_key/app_secret、动态 endpoint、Webhook 入站或供应商主动调用平台；仅保留内部 Adapter/mock。 |

## 7. 使用规则

1. 每次 IT、采购、财务、运营、安全或供应商补齐证据后，先更新生产前置台账或 findings register。
2. 执行 `npm run docs:production-blockers` 重新生成本文和 JSON。
3. 执行 `npm run verify:production-blockers`、`npm run verify:production-readiness-evidence`、`npm run verify:delivery-gates`。
4. 不得手工把本文中的阻断项删掉；必须从来源台账或 findings 关闭。
