# 生产责任人动作包 V0.1

## 1. 机器标记

- PRODUCTION_OWNER_ACTION_PACKS_READY
- OWNER_ACTIONS_FROM_PRODUCTION_BLOCKERS
- NO_OWNER_ACTION_CAN_CLOSE_WITHOUT_SOURCE_LEDGER
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET
- VERIFY_PRODUCTION_OWNER_ACTIONS_READY

## 2. 定位

本文从 `docs/production-readiness-evidence/production-blocker-register-latest.json` 自动生成，把仍未关闭的生产阻断项按责任人拆成可执行动作包。本文不改变来源台账状态，不允许用本文手工删项来关闭阻断。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| 未关闭唯一阻断项 | 23 |
| 责任人动作包 | 5 |
| 按责任人展开后的动作行 | 40 |

## 4. 责任人动作包总表

| owner_group | status | blocker_count | priorities | first_actions |
| --- | --- | --- | --- | --- |
| IT/运维/安全 | open_actions_present | 23 | P0, P1, P2 | 完成选型/采购/控制台配置；纳管最小设备矩阵；回填 enroll、策略和丢失处置证据；复跑生产阻断清单和交付闸口；开通聚合支付资料 |
| 财务 | open_actions_present | 4 | P0 | 开通聚合支付资料；配置回调和验签；完成退款与对账样例；财务确认日结和异常处理口径；建立人工台账 |
| 采购/供应商联调 | open_actions_present | 6 | P0, P1 | 完成选型/采购/控制台配置；纳管最小设备矩阵；回填 enroll、策略和丢失处置证据；复跑生产阻断清单和交付闸口；完成供应商资料收集 |
| 运营/法务 | open_actions_present | 3 | P0, P1 | 完成供应商资料收集；确认年龄核验合规材料；联调成功/失败样例；运营验收后再关闭阻断；配置企业微信应用和权限 |
| 技术实现负责人 | open_actions_present | 4 | P0, P1, P2 | 建立人工台账；设置到期提醒；执行首次发放或轮换演练；记录 credential_ref 和回滚步骤；收集 SDK/API 和沙箱 |

## 5. 分责任人明细

### IT/运维/安全

| id | priority | category | status | blocking_scope | next_action | required_evidence |
| --- | --- | --- | --- | --- | --- | --- |
| PRD-MDM-001 | P0 | MDM | pending_info | 阻塞生产工作手机、平板、一体机使用；不阻塞开发 mock | IT 选型、设备 enroll、策略下发和截图归档 | MDM/Android Enterprise 选型或 Android Management API 项目资料；策略模板截图或导出；工作手机、平板、售票型一体机、非售票型一体机 enroll 证据；远程锁定、擦除、丢失模式演练记录；平台设备绑定记录和责任人 |
| PRD-PAY-001 | P0 | payment | pending_info | 阻塞真实收款测试；不阻塞支付 mock | 获取测试商户号、回调地址、对账文件样例 | 聚合支付测试或生产商户资料；回调地址、验签资料和对账文件样例；退款权限、退款审批和日结口径；财务责任人确认；密钥仅填写 credential_ref，不写明文 |
| PRD-PAY-002 | P0 | payment | pending_info | 阻塞生产收款、退款和日结 | 建立商户资料、密钥引用、退款审批和对账口径 | 聚合支付测试或生产商户资料；回调地址、验签资料和对账文件样例；退款权限、退款审批和日结口径；财务责任人确认；密钥仅填写 credential_ref，不写明文 |
| PRD-FACE-001 | P0 | face_age | pending_info | 阻塞酒吧生产年龄核验通过 | 获取 SDK/API、错误码、测试账号、合规材料 | 年龄/人脸供应商 SDK 或 API 文档；测试账号、错误码、成功和失败样例；合规材料与采购确认；生产 credential_ref；运营验收记录 |
| PRD-NAS-AUTOMOUNT-001 | P0 | nas | pending_info | 阻塞长期预生产和生产自动恢复；不阻塞开发验证 | IT 安装后回填重启后 findmnt、读写删除测试和截图 | 重启后自动挂载证据；读写删除测试证据；定时备份归档和 sha256；从定时归档恢复演练报告；容量告警和失败告警送达记录 |
| PRD-NAS-SCHEDULE-001 | P0 | nas | pending_info | 阻塞长期预生产和生产自动备份；不阻塞手动备份演练 | IT 安装后回填调度配置、最近一次 scheduled 备份和 .sha256 | 重启后自动挂载证据；读写删除测试证据；定时备份归档和 sha256；从定时归档恢复演练报告；容量告警和失败告警送达记录 |
| PRD-NAS-ALERT-001 | P0 | nas | pending_info | 阻塞长期预生产和生产运维准入；不阻塞开发验证 | 回填容量告警、失败告警和通知送达证据 | 重启后自动挂载证据；读写删除测试证据；定时备份归档和 sha256；从定时归档恢复演练报告；容量告警和失败告警送达记录 |
| PRD-NAS-DRILL-CADENCE-001 | P0 | nas | pending_info | 阻塞长期预生产和生产可恢复结论；不阻塞当前编码 | 回填最近一次恢复演练报告、临时库清理和验证表 | 重启后自动挂载证据；读写删除测试证据；定时备份归档和 sha256；从定时归档恢复演练报告；容量告警和失败告警送达记录 |
| PRD-CRED-001 | P0 | credential | pending_info | 阻塞长期预生产和生产 | 建立密钥引用、轮换周期、责任人和应急回滚 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |
| PRD-WECOM-001 | P1 | wecom | pending_info | 阻塞企业微信 P0 同步能力 | 配置应用、通讯录、会话存档、回调和同步任务 | 企业微信应用和通讯录权限配置；回调配置、同步任务配置和失败告警；一次增量同步验收记录；会话存档或适用范围确认；生产 credential_ref |
| PRD-3P-001 | P1 | third_party | pending_info | 不阻塞 P0 主闭环；阻塞真实厂商联调 | 收集 SDK/API、签名、回调、字段映射和测试账号 | 供应商 SDK/API、签名算法和回调样例；字段映射、错误码、补偿和对账口径；沙箱账号和生产 credential_ref；Webhook nonce/timestamp/幂等验证；轮询校验策略和失败重试策略 |
| PRD-160-001 | P0 | ops160 | pending_info | 阻塞长期预生产和生产；不阻塞用户级覆盖开发验证 | 迁移用户级覆盖到系统配置，接入 systemd 和告警 | /etc/complex/complex.env 或受控配置等效证据；systemd 服务、日志路径和健康检查；备份、恢复、告警和容量监控证据；运维 Runbook 和回滚步骤；不得由 Codex 写入系统级生产 secret |
| PRD-CRED-OPS-001 | P0 | credential | pending_info | 阻塞长期预生产和生产 | 建立账号负责人、授权范围、到期日和停用步骤 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |
| PRD-CRED-DB-001 | P0 | credential | pending_info | 阻塞生产数据库接入 | 生成强密码、写入安全保管位置、补到期日和维护窗口 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |
| PRD-CRED-OBJ-001 | P0 | credential | pending_info | 阻塞生产附件、证据包、对象备份接入 | 确认桶范围、权限、Access Key 引用和停用方式 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |
| PRD-CRED-JWT-001 | P0 | credential | pending_info | 阻塞生产登录和开放接口签发 | 设计签名版本、双验窗口、旧 token 失效计划 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |
| PRD-CRED-3P-001 | P0 | credential | pending_info | 阻塞真实供应商联调或生产通过 | 收集 SDK/API、签名算法、回调地址、测试账号和证据 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |
| PRD-CRED-OPEN-001 | P0 | credential | pending_info | 阻塞第三方双向开放生产接入 | 建立应用、endpoint、allowed scopes、密钥引用和轮换窗口 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |
| PRD-CRED-NAS-001 | P0 | credential | pending_info | 阻塞生产可恢复结论 | IT 将真实密码写入安全保管位置或 160 受控凭证文件，不得写入文档、聊天或仓库 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |
| SEC-P0-MDM-DEVICE-001 | P0 | MDM | open | 阻塞生产工作手机、平板、一体机执行高风险核销/出酒/支付相关动作 | 完成 MDM 选型、enroll、策略下发、设备绑定截图和生产准入记录 | MDM/Android Enterprise 选型或 Android Management API 项目资料；策略模板截图或导出；工作手机、平板、售票型一体机、非售票型一体机 enroll 证据；远程锁定、擦除、丢失模式演练记录；平台设备绑定记录和责任人 |
| SEC-P0-OPS-RECOVERY-001 | P0 | ops160 | open | 阻塞公开生产安全准入；不阻塞开发联调 | P0-A no-system-write boundary：Codex 不安装系统级配置；后续由 IT 固化 160 开机自动挂载、定时备份、容量告警、失败告警和周期性恢复演练；生产凭证实际发放前按明细台账补责任人、到期日、credential_ref、提醒和轮换证据 | /etc/complex/complex.env 或受控配置等效证据；systemd 服务、日志路径和健康检查；备份、恢复、告警和容量监控证据；运维 Runbook 和回滚步骤；不得由 Codex 写入系统级生产 secret |
| SEC-P1-WEBHOOK-REPLAY-001 | P1 | third_party | open | 阻塞真实支付/人脸/设备/开放平台供应商联调上线；不阻塞 mock adapter | 按供应商 SDK/API 补字段映射、nonce、timestamp、签名验签、幂等、补偿和对账测试 | 供应商 SDK/API、签名算法和回调样例；字段映射、错误码、补偿和对账口径；沙箱账号和生产 credential_ref；Webhook nonce/timestamp/幂等验证；轮询校验策略和失败重试策略 |
| SEC-P2-CSRF-API-001 | P2 | security | open_for_prod_review | 不阻塞 Bearer/API 调用 smoke；若后台 Web 采用 Cookie 会话则阻塞公开生产 | 正式认证方案定稿时按 Bearer、SameSite Cookie 或双提交 Token 分支复核 | 认证方案分支说明；CSRF 或 Bearer Token 安全复核记录；安全负责人签收；生产前复测命令输出 |

### 财务

| id | priority | category | status | blocking_scope | next_action | required_evidence |
| --- | --- | --- | --- | --- | --- | --- |
| PRD-PAY-001 | P0 | payment | pending_info | 阻塞真实收款测试；不阻塞支付 mock | 获取测试商户号、回调地址、对账文件样例 | 聚合支付测试或生产商户资料；回调地址、验签资料和对账文件样例；退款权限、退款审批和日结口径；财务责任人确认；密钥仅填写 credential_ref，不写明文 |
| PRD-PAY-002 | P0 | payment | pending_info | 阻塞生产收款、退款和日结 | 建立商户资料、密钥引用、退款审批和对账口径 | 聚合支付测试或生产商户资料；回调地址、验签资料和对账文件样例；退款权限、退款审批和日结口径；财务责任人确认；密钥仅填写 credential_ref，不写明文 |
| PRD-CRED-001 | P0 | credential | pending_info | 阻塞长期预生产和生产 | 建立密钥引用、轮换周期、责任人和应急回滚 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |
| PRD-CRED-3P-001 | P0 | credential | pending_info | 阻塞真实供应商联调或生产通过 | 收集 SDK/API、签名算法、回调地址、测试账号和证据 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |

### 采购/供应商联调

| id | priority | category | status | blocking_scope | next_action | required_evidence |
| --- | --- | --- | --- | --- | --- | --- |
| PRD-MDM-001 | P0 | MDM | pending_info | 阻塞生产工作手机、平板、一体机使用；不阻塞开发 mock | IT 选型、设备 enroll、策略下发和截图归档 | MDM/Android Enterprise 选型或 Android Management API 项目资料；策略模板截图或导出；工作手机、平板、售票型一体机、非售票型一体机 enroll 证据；远程锁定、擦除、丢失模式演练记录；平台设备绑定记录和责任人 |
| PRD-FACE-001 | P0 | face_age | pending_info | 阻塞酒吧生产年龄核验通过 | 获取 SDK/API、错误码、测试账号、合规材料 | 年龄/人脸供应商 SDK 或 API 文档；测试账号、错误码、成功和失败样例；合规材料与采购确认；生产 credential_ref；运营验收记录 |
| PRD-3P-001 | P1 | third_party | pending_info | 不阻塞 P0 主闭环；阻塞真实厂商联调 | 收集 SDK/API、签名、回调、字段映射和测试账号 | 供应商 SDK/API、签名算法和回调样例；字段映射、错误码、补偿和对账口径；沙箱账号和生产 credential_ref；Webhook nonce/timestamp/幂等验证；轮询校验策略和失败重试策略 |
| PRD-CRED-3P-001 | P0 | credential | pending_info | 阻塞真实供应商联调或生产通过 | 收集 SDK/API、签名算法、回调地址、测试账号和证据 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |
| SEC-P0-MDM-DEVICE-001 | P0 | MDM | open | 阻塞生产工作手机、平板、一体机执行高风险核销/出酒/支付相关动作 | 完成 MDM 选型、enroll、策略下发、设备绑定截图和生产准入记录 | MDM/Android Enterprise 选型或 Android Management API 项目资料；策略模板截图或导出；工作手机、平板、售票型一体机、非售票型一体机 enroll 证据；远程锁定、擦除、丢失模式演练记录；平台设备绑定记录和责任人 |
| SEC-P1-WEBHOOK-REPLAY-001 | P1 | third_party | open | 阻塞真实支付/人脸/设备/开放平台供应商联调上线；不阻塞 mock adapter | 按供应商 SDK/API 补字段映射、nonce、timestamp、签名验签、幂等、补偿和对账测试 | 供应商 SDK/API、签名算法和回调样例；字段映射、错误码、补偿和对账口径；沙箱账号和生产 credential_ref；Webhook nonce/timestamp/幂等验证；轮询校验策略和失败重试策略 |

### 运营/法务

| id | priority | category | status | blocking_scope | next_action | required_evidence |
| --- | --- | --- | --- | --- | --- | --- |
| PRD-FACE-001 | P0 | face_age | pending_info | 阻塞酒吧生产年龄核验通过 | 获取 SDK/API、错误码、测试账号、合规材料 | 年龄/人脸供应商 SDK 或 API 文档；测试账号、错误码、成功和失败样例；合规材料与采购确认；生产 credential_ref；运营验收记录 |
| PRD-WECOM-001 | P1 | wecom | pending_info | 阻塞企业微信 P0 同步能力 | 配置应用、通讯录、会话存档、回调和同步任务 | 企业微信应用和通讯录权限配置；回调配置、同步任务配置和失败告警；一次增量同步验收记录；会话存档或适用范围确认；生产 credential_ref |
| PRD-CRED-3P-001 | P0 | credential | pending_info | 阻塞真实供应商联调或生产通过 | 收集 SDK/API、签名算法、回调地址、测试账号和证据 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |

### 技术实现负责人

| id | priority | category | status | blocking_scope | next_action | required_evidence |
| --- | --- | --- | --- | --- | --- | --- |
| PRD-CRED-JWT-001 | P0 | credential | pending_info | 阻塞生产登录和开放接口签发 | 设计签名版本、双验窗口、旧 token 失效计划 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |
| PRD-CRED-OPEN-001 | P0 | credential | pending_info | 阻塞第三方双向开放生产接入 | 建立应用、endpoint、allowed scopes、密钥引用和轮换窗口 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 |
| SEC-P1-WEBHOOK-REPLAY-001 | P1 | third_party | open | 阻塞真实支付/人脸/设备/开放平台供应商联调上线；不阻塞 mock adapter | 按供应商 SDK/API 补字段映射、nonce、timestamp、签名验签、幂等、补偿和对账测试 | 供应商 SDK/API、签名算法和回调样例；字段映射、错误码、补偿和对账口径；沙箱账号和生产 credential_ref；Webhook nonce/timestamp/幂等验证；轮询校验策略和失败重试策略 |
| SEC-P2-CSRF-API-001 | P2 | security | open_for_prod_review | 不阻塞 Bearer/API 调用 smoke；若后台 Web 采用 Cookie 会话则阻塞公开生产 | 正式认证方案定稿时按 Bearer、SameSite Cookie 或双提交 Token 分支复核 | 认证方案分支说明；CSRF 或 Bearer Token 安全复核记录；安全负责人签收；生产前复测命令输出 |

## 6. 回填后必跑命令

1. `npm run docs:production-blockers`
2. `npm run docs:production-owner-actions`
3. `npm run verify:production-owner-actions`
4. `npm run verify:production-readiness-evidence`
5. `npm run verify:delivery-gates`

## 7. 禁止事项

1. 不得把口头确认、mock 截图、未纳管设备截图当作生产证据。
2. 不得把真实密码、token、API secret、私钥正文写入文档、仓库、聊天或日志。
3. 不得绕过来源台账或 findings register 直接手工删除本文阻断项。
4. 不得把供应商沙箱或模拟适配器当作生产接入完成证据。
