# IT/运维/安全 生产证据执行包 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_OWNER_EXECUTION_PACKET_READY
- PRODUCTION_EVIDENCE_DRAFT_PREP_READY
- OWNER_PACKET_DOES_NOT_CLOSE_BLOCKERS
- RAW_EVIDENCE_STAYS_EXTERNAL
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET

## 2. 当前任务

| 项目 | 值 |
|---|---:|
| 状态 | awaiting_real_evidence |
| 回填项 | 12 |
| P0 回填项 | 11 |
| 等待提交 | 12 |
| 无效提交 | 0 |

第一动作：先补生产账号、数据库、对象存储、JWT、第三方和 NAS credential_ref、到期提醒和轮换证据。

配套责任组说明：`docs/production-readiness-evidence/owner-guides/it-ops-security.md`。

## 3. 批量草稿准备

默认只预览，不写入文件：

```bash
npm run prepare:production-evidence-drafts -- --owner it-ops-security --p0
```

实际复制模板时必须显式加 `--execute`，且已有 manifest 不会被覆盖：

```bash
npm run prepare:production-evidence-drafts -- --owner it-ops-security --p0 --execute
```

复制出来的仍是待填写草稿，不是生产证据；必须替换占位字段、补外部证据链接和复核人后再跑状态校验。

## 4. P0 优先项

| item_id | category | status | blocking_scope | template_path | target_manifest_path |
| --- | --- | --- | --- | --- | --- |
| PRD-CRED-001 | credential | awaiting_submission | 阻塞长期预生产和生产 | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-001/manifest-PRD-CRED-001.json |
| PRD-CRED-DB-001 | credential | awaiting_submission | 阻塞生产数据库接入 | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-DB-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-DB-001/manifest-PRD-CRED-DB-001.json |
| PRD-CRED-JWT-001 | credential | awaiting_submission | 阻塞生产登录和开放接口签发 | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-JWT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json |
| PRD-CRED-NAS-001 | credential | awaiting_submission | 阻塞生产可恢复结论 | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-NAS-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-NAS-001/manifest-PRD-CRED-NAS-001.json |
| PRD-CRED-OPS-001 | credential | awaiting_submission | 阻塞长期预生产和生产 | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-OPS-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPS-001/manifest-PRD-CRED-OPS-001.json |
| PRD-NAS-ALERT-001 | nas | awaiting_submission | 阻塞长期预生产和生产运维准入；不阻塞开发验证 | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-ALERT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-ALERT-001/manifest-PRD-NAS-ALERT-001.json |
| PRD-NAS-AUTOMOUNT-001 | nas | awaiting_submission | 阻塞长期预生产和生产自动恢复；不阻塞开发验证 | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-AUTOMOUNT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-AUTOMOUNT-001/manifest-PRD-NAS-AUTOMOUNT-001.json |
| PRD-NAS-DRILL-CADENCE-001 | nas | awaiting_submission | 阻塞长期预生产和生产可恢复结论；不阻塞当前编码 | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-DRILL-CADENCE-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-DRILL-CADENCE-001/manifest-PRD-NAS-DRILL-CADENCE-001.json |
| PRD-NAS-SCHEDULE-001 | nas | awaiting_submission | 阻塞长期预生产和生产自动备份；不阻塞手动备份演练 | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-SCHEDULE-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-SCHEDULE-001/manifest-PRD-NAS-SCHEDULE-001.json |
| PRD-160-001 | ops160 | awaiting_submission | 阻塞长期预生产和生产；不阻塞用户级覆盖开发验证 | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-160-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-160-001/manifest-PRD-160-001.json |
| SEC-P0-OPS-RECOVERY-001 | ops160 | awaiting_submission | 阻塞公开生产安全准入；不阻塞开发联调 | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P0-OPS-RECOVERY-001.json | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P0-OPS-RECOVERY-001/manifest-SEC-P0-OPS-RECOVERY-001.json |

## 5. 可复制命令示例

这些命令只复制模板到目标 manifest 路径，不填真实证据、不关闭阻断；如果目标 manifest 已存在，命令会停止，避免覆盖人工提交。

| item_id | copy_command |
| --- | --- |
| PRD-CRED-001 | mkdir -p 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-001/' && test ! -e 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-001/manifest-PRD-CRED-001.json' && cp 'docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-001.json' 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-001/manifest-PRD-CRED-001.json' |
| PRD-CRED-DB-001 | mkdir -p 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-DB-001/' && test ! -e 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-DB-001/manifest-PRD-CRED-DB-001.json' && cp 'docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-DB-001.json' 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-DB-001/manifest-PRD-CRED-DB-001.json' |
| PRD-CRED-JWT-001 | mkdir -p 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-JWT-001/' && test ! -e 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json' && cp 'docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-JWT-001.json' 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json' |
| PRD-CRED-NAS-001 | mkdir -p 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-NAS-001/' && test ! -e 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-NAS-001/manifest-PRD-CRED-NAS-001.json' && cp 'docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-NAS-001.json' 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-NAS-001/manifest-PRD-CRED-NAS-001.json' |
| PRD-CRED-OPS-001 | mkdir -p 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPS-001/' && test ! -e 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPS-001/manifest-PRD-CRED-OPS-001.json' && cp 'docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-OPS-001.json' 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPS-001/manifest-PRD-CRED-OPS-001.json' |
| PRD-NAS-ALERT-001 | mkdir -p 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-ALERT-001/' && test ! -e 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-ALERT-001/manifest-PRD-NAS-ALERT-001.json' && cp 'docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-ALERT-001.json' 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-ALERT-001/manifest-PRD-NAS-ALERT-001.json' |
| PRD-NAS-AUTOMOUNT-001 | mkdir -p 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-AUTOMOUNT-001/' && test ! -e 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-AUTOMOUNT-001/manifest-PRD-NAS-AUTOMOUNT-001.json' && cp 'docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-AUTOMOUNT-001.json' 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-AUTOMOUNT-001/manifest-PRD-NAS-AUTOMOUNT-001.json' |
| PRD-NAS-DRILL-CADENCE-001 | mkdir -p 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-DRILL-CADENCE-001/' && test ! -e 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-DRILL-CADENCE-001/manifest-PRD-NAS-DRILL-CADENCE-001.json' && cp 'docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-DRILL-CADENCE-001.json' 'docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-DRILL-CADENCE-001/manifest-PRD-NAS-DRILL-CADENCE-001.json' |

## 6. 全部回填项

| item_id | priority | category | status | operator_next_step | accepted_evidence_types |
| --- | --- | --- | --- | --- | --- |
| PRD-CRED-001 | P0 | credential | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | credential_ref_record,rotation_runbook,expiry_reminder_record,rotation_or_issuance_evidence,rollback_test_record |
| PRD-CRED-DB-001 | P0 | credential | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | credential_ref_record,rotation_runbook,expiry_reminder_record,rotation_or_issuance_evidence,rollback_test_record |
| PRD-CRED-JWT-001 | P0 | credential | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | credential_ref_record,rotation_runbook,expiry_reminder_record,rotation_or_issuance_evidence,rollback_test_record |
| PRD-CRED-NAS-001 | P0 | credential | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | credential_ref_record,rotation_runbook,expiry_reminder_record,rotation_or_issuance_evidence,rollback_test_record |
| PRD-CRED-OPS-001 | P0 | credential | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | credential_ref_record,rotation_runbook,expiry_reminder_record,rotation_or_issuance_evidence,rollback_test_record |
| PRD-NAS-ALERT-001 | P0 | nas | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | automount_export,scheduled_backup_log,sha256_file,restore_drill_report,alert_delivery_record |
| PRD-NAS-AUTOMOUNT-001 | P0 | nas | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | automount_export,scheduled_backup_log,sha256_file,restore_drill_report,alert_delivery_record |
| PRD-NAS-DRILL-CADENCE-001 | P0 | nas | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | automount_export,scheduled_backup_log,sha256_file,restore_drill_report,alert_delivery_record |
| PRD-NAS-SCHEDULE-001 | P0 | nas | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | automount_export,scheduled_backup_log,sha256_file,restore_drill_report,alert_delivery_record |
| PRD-160-001 | P0 | ops160 | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | system_config_redacted_export,systemd_status_export,health_check_log,rollback_drill_report,alert_delivery_record |
| SEC-P0-OPS-RECOVERY-001 | P0 | ops160 | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | system_config_redacted_export,systemd_status_export,health_check_log,rollback_drill_report,alert_delivery_record |
| SEC-P2-CSRF-API-001 | P2 | security | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | security_review_record,auth_scheme_decision,test_output,approval_record |

## 7. 提交后复跑

```bash
npm run verify:production-evidence-submissions-guard
npm run docs:production-evidence-submission-status
npm run verify:production-evidence-submission-status
npm run docs:production-evidence-backfill-priority
npm run docs:production-evidence-backfill-execution-ledger
npm run docs:production-evidence-owner-execution-packets
npm run verify:production-evidence-owner-execution-packets
npm run verify:delivery-gates
```

## 8. 禁止事项

- 不提交原始截图、日志、导出文件、私钥、token 或 SMB 密码到仓库。
- 不把 mock、沙箱、口头确认、占位链接当作生产证据。
- 不让同一人同时作为 owner_person 和 reviewer。
- 不用本执行包关闭生产阻断；只有 manifest 通过状态校验且外部证据可追溯时，才进入验收判断。
