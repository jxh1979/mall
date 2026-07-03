# 生产责任人动作包 V0.1

## 1. 机器标记

- PRODUCTION_OWNER_ACTION_PACKS_READY
- OWNER_ACTIONS_FROM_PRODUCTION_BLOCKERS
- OWNER_ACTIONS_LINKED_TO_EVIDENCE_MANIFESTS
- NO_OWNER_ACTION_CAN_CLOSE_WITHOUT_SOURCE_LEDGER
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET
- VERIFY_PRODUCTION_OWNER_ACTIONS_READY

## 2. 定位

本文从 `docs/production-readiness-evidence/production-blocker-register-latest.json` 自动生成，把仍未关闭的生产阻断项按责任人拆成可执行动作包。本文不改变来源台账状态，不允许用本文手工删项来关闭阻断。

Evidence manifest source: `docs/production-readiness-evidence/production-evidence-submission-kit-latest.json`. Every owner action item must link to at least one manifest path; verification fails when items_without_manifest is not 0.

## 3. 总览

| 指标 | 数量 |
|---|---:|
| 未关闭唯一阻断项 | 12 |
| 责任人动作包 | 3 |
| 按责任人展开后的动作行 | 15 |
| 已关联证据提交行 | 15 |
| 未匹配 manifest 的动作行 | 0 |

## 4. 责任人动作包总表

| owner_group | status | blocker_count | priorities | first_actions |
| --- | --- | --- | --- | --- |
| IT/运维/安全 | open_actions_present | 12 | P0, P2 | 固化系统级挂载；配置定时备份和校验；配置容量/失败告警；完成周期性恢复演练；建立人工台账 |
| 财务 | open_actions_present | 1 | P0 | 建立人工台账；设置到期提醒；执行首次发放或轮换演练；记录 credential_ref 和回滚步骤 |
| 技术实现负责人 | open_actions_present | 2 | P0, P2 | 建立人工台账；设置到期提醒；执行首次发放或轮换演练；记录 credential_ref 和回滚步骤；补齐来源证据 |

## 5. 分责任人明细

### IT/运维/安全

| id | priority | category | status | blocking_scope | next_action | required_evidence | manifest_paths | accepted_evidence_types | verification_commands |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| PRD-NAS-AUTOMOUNT-001 | P0 | nas | pending_info | 阻塞长期预生产和生产自动恢复；不阻塞开发验证 | IT 安装后回填重启后 findmnt、读写删除测试和截图 | 重启后自动挂载证据；读写删除测试证据；定时备份归档和 sha256；从定时归档恢复演练报告；容量告警和失败告警送达记录 | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-AUTOMOUNT-001/manifest-PRD-NAS-AUTOMOUNT-001.json | automount_export；scheduled_backup_log；sha256_file；restore_drill_report；alert_delivery_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |
| PRD-NAS-SCHEDULE-001 | P0 | nas | pending_info | 阻塞长期预生产和生产自动备份；不阻塞手动备份演练 | IT 安装后回填调度配置、最近一次 scheduled 备份和 .sha256 | 重启后自动挂载证据；读写删除测试证据；定时备份归档和 sha256；从定时归档恢复演练报告；容量告警和失败告警送达记录 | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-SCHEDULE-001/manifest-PRD-NAS-SCHEDULE-001.json | automount_export；scheduled_backup_log；sha256_file；restore_drill_report；alert_delivery_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |
| PRD-NAS-ALERT-001 | P0 | nas | pending_info | 阻塞长期预生产和生产运维准入；不阻塞开发验证 | 回填容量告警、失败告警和通知送达证据 | 重启后自动挂载证据；读写删除测试证据；定时备份归档和 sha256；从定时归档恢复演练报告；容量告警和失败告警送达记录 | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-ALERT-001/manifest-PRD-NAS-ALERT-001.json | automount_export；scheduled_backup_log；sha256_file；restore_drill_report；alert_delivery_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |
| PRD-NAS-DRILL-CADENCE-001 | P0 | nas | pending_info | 阻塞长期预生产和生产可恢复结论；不阻塞当前编码 | 回填最近一次恢复演练报告、临时库清理和验证表 | 重启后自动挂载证据；读写删除测试证据；定时备份归档和 sha256；从定时归档恢复演练报告；容量告警和失败告警送达记录 | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-DRILL-CADENCE-001/manifest-PRD-NAS-DRILL-CADENCE-001.json | automount_export；scheduled_backup_log；sha256_file；restore_drill_report；alert_delivery_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |
| PRD-CRED-001 | P0 | credential | pending_info | 阻塞长期预生产和生产 | 建立密钥引用、轮换周期、责任人和应急回滚 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-001/manifest-PRD-CRED-001.json | credential_ref_record；rotation_runbook；expiry_reminder_record；rotation_or_issuance_evidence；rollback_test_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |
| PRD-160-001 | P0 | ops160 | pending_info | 阻塞长期预生产和生产；不阻塞用户级覆盖开发验证 | 迁移用户级覆盖到系统配置，接入 systemd 和告警 | /etc/complex/complex.env 或受控配置等效证据；systemd 服务、日志路径和健康检查；备份、恢复、告警和容量监控证据；运维 Runbook 和回滚步骤；不得由 Codex 写入系统级生产 secret | docs/production-readiness-evidence/submissions/it-ops-security/PRD-160-001/manifest-PRD-160-001.json | system_config_redacted_export；systemd_status_export；health_check_log；rollback_drill_report；alert_delivery_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |
| PRD-CRED-OPS-001 | P0 | credential | pending_info | 阻塞长期预生产和生产 | 建立账号负责人、授权范围、到期日和停用步骤 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPS-001/manifest-PRD-CRED-OPS-001.json | credential_ref_record；rotation_runbook；expiry_reminder_record；rotation_or_issuance_evidence；rollback_test_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |
| PRD-CRED-DB-001 | P0 | credential | pending_info | 阻塞生产数据库接入 | 生成强密码、写入安全保管位置、补到期日和维护窗口 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-DB-001/manifest-PRD-CRED-DB-001.json | credential_ref_record；rotation_runbook；expiry_reminder_record；rotation_or_issuance_evidence；rollback_test_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |
| PRD-CRED-JWT-001 | P0 | credential | pending_info | 阻塞生产登录和开放接口签发 | 设计签名版本、双验窗口、旧 token 失效计划 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json | credential_ref_record；rotation_runbook；expiry_reminder_record；rotation_or_issuance_evidence；rollback_test_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |
| PRD-CRED-NAS-001 | P0 | credential | pending_info | 阻塞生产可恢复结论 | IT 将真实密码写入安全保管位置或 160 受控凭证文件，不得写入文档、聊天或仓库 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-NAS-001/manifest-PRD-CRED-NAS-001.json | credential_ref_record；rotation_runbook；expiry_reminder_record；rotation_or_issuance_evidence；rollback_test_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |
| SEC-P0-OPS-RECOVERY-001 | P0 | ops160 | open | 阻塞公开生产安全准入；不阻塞开发联调 | P0-A no-system-write boundary：Codex 不安装系统级配置；后续由 IT 固化 160 开机自动挂载、定时备份、容量告警、失败告警和周期性恢复演练；生产凭证实际发放前按明细台账补责任人、到期日、credential_ref、提醒和轮换证据 | /etc/complex/complex.env 或受控配置等效证据；systemd 服务、日志路径和健康检查；备份、恢复、告警和容量监控证据；运维 Runbook 和回滚步骤；不得由 Codex 写入系统级生产 secret | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P0-OPS-RECOVERY-001/manifest-SEC-P0-OPS-RECOVERY-001.json | system_config_redacted_export；systemd_status_export；health_check_log；rollback_drill_report；alert_delivery_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |
| SEC-P2-CSRF-API-001 | P2 | security | open_for_prod_review | 不阻塞 Bearer/API 调用 smoke；若后台 Web 采用 Cookie 会话则阻塞公开生产 | 员工后台仍为 Bearer 头 token（无 cookie 环境凭证）；若后台改用 Cookie 会话再按 SameSite/双提交复核 | 认证方案分支说明；CSRF 或 Bearer Token 安全复核记录；安全负责人签收；生产前复测命令输出 | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P2-CSRF-API-001/manifest-SEC-P2-CSRF-API-001.json | security_review_record；auth_scheme_decision；test_output；approval_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |

### 财务

| id | priority | category | status | blocking_scope | next_action | required_evidence | manifest_paths | accepted_evidence_types | verification_commands |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| PRD-CRED-001 | P0 | credential | pending_info | 阻塞长期预生产和生产 | 建立密钥引用、轮换周期、责任人和应急回滚 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 | docs/production-readiness-evidence/submissions/finance/PRD-CRED-001/manifest-PRD-CRED-001.json | credential_ref_record；rotation_runbook；expiry_reminder_record；rotation_or_issuance_evidence；rollback_test_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |

### 技术实现负责人

| id | priority | category | status | blocking_scope | next_action | required_evidence | manifest_paths | accepted_evidence_types | verification_commands |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| PRD-CRED-JWT-001 | P0 | credential | pending_info | 阻塞生产登录和开放接口签发 | 设计签名版本、双验窗口、旧 token 失效计划 | credential_ref、owner_person、owner_department；到期日、提醒策略和轮换窗口；轮换步骤、回滚步骤和验证步骤；最近一次轮换或首次发放证据；不得出现明文密钥、token 或密码 | docs/production-readiness-evidence/submissions/technical-owner/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json | credential_ref_record；rotation_runbook；expiry_reminder_record；rotation_or_issuance_evidence；rollback_test_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |
| SEC-P2-CSRF-API-001 | P2 | security | open_for_prod_review | 不阻塞 Bearer/API 调用 smoke；若后台 Web 采用 Cookie 会话则阻塞公开生产 | 员工后台仍为 Bearer 头 token（无 cookie 环境凭证）；若后台改用 Cookie 会话再按 SameSite/双提交复核 | 认证方案分支说明；CSRF 或 Bearer Token 安全复核记录；安全负责人签收；生产前复测命令输出 | docs/production-readiness-evidence/submissions/technical-owner/SEC-P2-CSRF-API-001/manifest-SEC-P2-CSRF-API-001.json | security_review_record；auth_scheme_decision；test_output；approval_record | npm run docs:production-blockers；npm run docs:production-owner-actions；npm run docs:production-evidence-submission-kit；npm run verify:production-evidence-submission-kit；npm run verify:production-evidence-submissions-guard；npm run verify:production-readiness-evidence；npm run verify:delivery-gates |

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
