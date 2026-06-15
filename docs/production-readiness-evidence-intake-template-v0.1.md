# 生产前置证据回填模板 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_INTAKE_TEMPLATES_READY
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET
- MOCK_IS_NOT_PRODUCTION_EVIDENCE
- NO_SCREENSHOT_ONLY_FOR_HIGH_RISK_ITEMS
- VERIFY_PRODUCTION_EVIDENCE_INTAKE_READY

## 2. 定位

本文定义 IT、采购、财务、运营、安全和供应商回填生产前置证据时必须提供的字段和材料。本文不是新的业务决策，也不把任何 `pending_info` 项改为 `passed`；只有对应台账、evidence、Runbook 和机器校验均通过后，才允许关闭阻断项。

所有真实密码、token、API secret、私钥正文、支付密钥、人脸供应商密钥、企业微信密钥、NAS SMB 密码都不得写入本文、聊天、日志、仓库或截图文件名。系统只接受 `credential_ref`。

配套提交包：`production-evidence-submission-kit-v0.1.md` 从责任人动作包自动生成，定义每个责任组、每个阻断项的提交目录、manifest 文件名、必填字段、可接受证据类型和回填后复测命令。字段口径以本文为准，提交结构以提交包为准。

配套 manifest 模板：`production-evidence-manifest-templates-v0.1.md` 提供可复制 JSON 样例；模板默认 `result=blocked`、`redaction_confirmed=false`，不是真实证据。

配套责任组说明包：`production-evidence-owner-guides-v0.1.md` 将模板路径、真实提交路径和复测命令按责任组拆分，便于 IT、财务、采购、运营、法务和技术负责人执行。

配套状态看板：`production-evidence-submission-status-v0.1.md` 扫描实际 manifest 提交状态；未提交保持 awaiting，格式错误、缺字段或明文密钥会阻断交付闸口。

## 3. 通用证据字段

| 字段 | 必填 | 说明 |
|---|---|---|
| evidence_id | 是 | 证据编号，建议格式 `EVD-<item_id>-<YYYYMMDD>-<seq>` |
| item_id | 是 | 对应生产前置项，如 `PRD-MDM-001` |
| owner_department | 是 | IT、采购、财务、运营、安全、供应商等 |
| owner_person | 是 | 具体责任人 |
| evidence_link | 是 | 证据文件、工单、供应商回执或验收报告链接 |
| evidence_type | 是 | screenshot、export、test_report、contract、runbook_log、supplier_reply、credential_ref_record |
| last_test_time | 是 | 最近一次真实测试或验收时间 |
| result | 是 | passed、failed、partial、blocked |
| credential_ref | 条件必填 | 涉及密钥时只填引用，不填明文 |
| rollback_step | 条件必填 | 涉及账号、支付、供应商、运行配置时必须填写 |
| next_retest_due | 条件必填 | 周期演练、凭证轮换、告警验证必须填写 |
| approver | 条件必填 | 豁免、生产切换、退款权限、密钥发放必须填写 |
| notes | 否 | 仅写非敏感说明 |

## 4. 分类模板

### 4.1 MDM / Android Enterprise：`PRD-MDM-001`

最低材料：

- MDM 或 Android Management API 项目资料、供应商或控制台名称。
- 组织、策略模板、设备组和限制策略截图或导出。
- 至少 1 台工作手机、1 台平板、1 台售票型一体机、1 台非售票型一体机 enroll 成功证据。
- 策略下发成功证据。
- 丢失、锁定、擦除或等效远程管控演练记录。
- 平台设备绑定记录。

通过条件：`SEC-P0-MDM-DEVICE-001` 可从 `open` 改为 `resolved`，并且台账 `PRD-MDM-001` 可从 `pending_info` 改为 `passed`。

### 4.2 NAS 自动挂载、定时备份、告警、周期恢复：`PRD-NAS-AUTOMOUNT-001` / `PRD-NAS-SCHEDULE-001` / `PRD-NAS-ALERT-001` / `PRD-NAS-DRILL-CADENCE-001`

最低材料：

- 重启后 `findmnt /mnt/complex-nas-backup` 证据。
- 重启后读写删除测试证据。
- 自动挂载配置截图或导出，不能包含 SMB 明文密码。
- 定时备份配置截图或导出。
- 最近一次 scheduled 备份归档、`.sha256`、恢复演练报告。
- 容量告警阈值、备份失败告警演练和通知送达证据。
- 月度或上线前恢复演练计划。

通过条件：不要求 Codex 写系统配置；必须由 IT/运维在受控窗口安装并回填证据。

### 4.3 生产账号和密钥轮换：`PRD-CRED-001` / `PRD-CRED-*`

最低材料：

- 生产服务器、PostgreSQL、对象存储、JWT/Token、第三方/支付/企业微信/开放平台、NAS SMB 的 `credential_ref`。
- owner_person、授权范围、到期日、提醒策略、轮换步骤、回滚步骤、应急停用步骤。
- 轮换或发放证据链接。
- 验证步骤：登录、数据库连接、Flyway 状态、对象上传下载、Webhook 验签、旧 token 双验窗口等。

禁止材料：明文密码、明文 token、明文 API secret、私钥正文、可直接使用的支付或企微密钥。

### 4.4 聚合支付：`PRD-PAY-001` / `PRD-PAY-002`

最低材料：

- 测试商户号、生产商户号、产品开通范围、退款权限、对账文件样例。
- 回调地址、验签算法、timestamp/nonce、防重放规则。
- 支付成功、支付失败、退款成功、退款失败、对账差异、重复回调、漏回调补偿测试报告。
- 财务负责人确认的结算口径。
- 生产密钥只记录 `credential_ref`。

通过条件：支付 mock 不得作为生产证据；必须有真实渠道或聚合支付沙箱/生产回执。

### 4.5 企业微信：`PRD-WECOM-001`

最低材料：

- 企业微信应用配置、通讯录权限、会话存档权限、回调配置。
- 账号绑定、每日同步、失败告警和重试记录。
- 至少一次全量同步、一次增量同步、一次失败告警演练。
- 企业微信凭证只记录 `credential_ref`。

### 4.6 酒吧年龄/人脸核验：`PRD-FACE-001`

最低材料：

- 供应商 SDK/API、测试账号、错误码、合规材料。
- 年龄核验成功、失败、无法识别、网络失败、人工复核样例。
- 采购确认和法务/运营确认的使用边界。
- 供应商密钥只记录 `credential_ref`。

通过条件：供应商未定时只能 mock，不得标记生产 `passed`。

### 4.7 机器人、视频、设备厂商：`PRD-3P-001`

最低材料：

- SDK/API、签名算法、Webhook 样例、轮询策略、字段映射、错误码、沙箱账号。
- 真实设备或供应商沙箱测试报告。
- 超时、重复回调、漏回调、签名错误、幂等冲突、补偿和对账策略。
- 生产凭证只记录 `credential_ref`。

通过条件：mock adapter 不等于真实生产接入；真实控制能力必须等供应商材料到位后再启用。

### 4.8 160 运行配置生产化：`PRD-160-001`

最低材料：

- `/etc/complex/complex.env` 或等效受控配置已由 IT 安装。
- systemd、日志、健康检查、告警配置和回滚路径。
- 配置脱敏导出或截图，不包含明文密钥。
- 最近一次启动、健康检查、回滚演练或发布演练记录。

## 5. 验收与复核命令

每补齐一类证据后执行：

```bash
npm run docs:production-blockers
npm run verify:production-blockers
npm run verify:production-readiness-evidence
npm run verify:delivery-gates
```

如涉及 OpenAPI、认证、权限、供应商、支付、MDM、生产凭证或 160 运维证据变化，还必须执行：

```bash
npm run docs:security-baseline
npm run verify:security-baseline
```

## 6. 禁止关闭条件

| 情况 | 结论 |
|---|---|
| 只有 mock adapter | 不得关闭生产阻断 |
| 只有口头确认 | 不得关闭生产阻断 |
| 只有截图但无测试时间、责任人、证据链接 | 不得关闭高风险阻断 |
| evidence 中出现明文密码、token、API secret 或私钥 | 阻断，必须删除并轮换泄露凭证 |
| 未做失败场景或回滚验证 | 不得关闭支付、供应商、凭证、160 运行配置类阻断 |
| 供应商未确定 | 只能保留接口占位和 mock，不得启用真实生产控制 |