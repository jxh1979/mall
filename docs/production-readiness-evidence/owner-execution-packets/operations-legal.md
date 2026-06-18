# 运营/法务 生产证据执行包 V0.1

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
| 回填项 | 3 |
| P0 回填项 | 2 |
| 等待提交 | 3 |
| 无效提交 | 0 |

第一动作：先补人脸/年龄核验供应商 SDK/API、测试账号、合规材料和成功/失败样例。

配套责任组说明：`docs/production-readiness-evidence/owner-guides/operations-legal.md`。

## 3. 批量草稿准备

默认只预览，不写入文件：

```bash
npm run prepare:production-evidence-drafts -- --owner operations-legal --p0
```

实际复制模板时必须显式加 `--execute`，且已有 manifest 不会被覆盖：

```bash
npm run prepare:production-evidence-drafts -- --owner operations-legal --p0 --execute
```

复制出来的仍是待填写草稿，不是生产证据；必须替换占位字段、补外部证据链接和复核人后再跑状态校验。

## 4. P0 优先项

| item_id | category | status | blocking_scope | template_path | target_manifest_path |
| --- | --- | --- | --- | --- | --- |
| PRD-CRED-3P-001 | credential | awaiting_submission | 阻塞真实供应商联调或生产通过 | docs/production-readiness-evidence/submission-manifest-templates/operations-legal/template-PRD-CRED-3P-001.json | docs/production-readiness-evidence/submissions/operations-legal/PRD-CRED-3P-001/manifest-PRD-CRED-3P-001.json |
| PRD-FACE-001 | face_age | awaiting_submission | 阻塞酒吧生产年龄核验通过 | docs/production-readiness-evidence/submission-manifest-templates/operations-legal/template-PRD-FACE-001.json | docs/production-readiness-evidence/submissions/operations-legal/PRD-FACE-001/manifest-PRD-FACE-001.json |

## 5. 可复制命令示例

这些命令只复制模板到目标 manifest 路径，不填真实证据、不关闭阻断；如果目标 manifest 已存在，命令会停止，避免覆盖人工提交。

| item_id | copy_command |
| --- | --- |
| PRD-CRED-3P-001 | mkdir -p 'docs/production-readiness-evidence/submissions/operations-legal/PRD-CRED-3P-001/' && test ! -e 'docs/production-readiness-evidence/submissions/operations-legal/PRD-CRED-3P-001/manifest-PRD-CRED-3P-001.json' && cp 'docs/production-readiness-evidence/submission-manifest-templates/operations-legal/template-PRD-CRED-3P-001.json' 'docs/production-readiness-evidence/submissions/operations-legal/PRD-CRED-3P-001/manifest-PRD-CRED-3P-001.json' |
| PRD-FACE-001 | mkdir -p 'docs/production-readiness-evidence/submissions/operations-legal/PRD-FACE-001/' && test ! -e 'docs/production-readiness-evidence/submissions/operations-legal/PRD-FACE-001/manifest-PRD-FACE-001.json' && cp 'docs/production-readiness-evidence/submission-manifest-templates/operations-legal/template-PRD-FACE-001.json' 'docs/production-readiness-evidence/submissions/operations-legal/PRD-FACE-001/manifest-PRD-FACE-001.json' |

## 6. 全部回填项

| item_id | priority | category | status | operator_next_step | accepted_evidence_types |
| --- | --- | --- | --- | --- | --- |
| PRD-CRED-3P-001 | P0 | credential | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | credential_ref_record,rotation_runbook,expiry_reminder_record,rotation_or_issuance_evidence,rollback_test_record |
| PRD-FACE-001 | P0 | face_age | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | supplier_sdk_or_api_doc,success_failure_case_report,compliance_material,procurement_approval_record,operation_acceptance_record |
| PRD-WECOM-001 | P1 | wecom | awaiting_submission | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 | wecom_app_config_export,directory_sync_report,callback_config_export,failure_alert_test,credential_ref_record |

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
