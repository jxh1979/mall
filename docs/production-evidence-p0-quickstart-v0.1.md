# P0 生产证据快速回填快照 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_P0_QUICKSTART_READY
- P0_ONLY_EVIDENCE_BACKFILL_VIEW
- OWNER_FIRST_ACTION_READY
- QUICKSTART_DOES_NOT_CLOSE_BLOCKERS
- RAW_EVIDENCE_STAYS_EXTERNAL
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET

## 2. 定位

本文从 `docs/production-readiness-evidence/production-evidence-submission-status-latest.json`、`docs/production-readiness-evidence/production-evidence-backfill-priority-latest.json` 和 `docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.json` 自动生成，只给责任人看 P0 证据缺口。它是快速派发表，不是生产证据本身；不能关闭任何生产阻断项。

## 3. P0 总览

| 指标 | 数量 |
|---|---:|
| P0 预期提交 | 13 |
| P0 等待提交 | 13 |
| P0 无效提交 | 0 |
| P0 有效提交 | 0 |
| 责任组 | 3 |
| P0 批次 | 5 |

## 4. 责任组第一动作

| owner_group | p0_total | awaiting | invalid | categories | first_action | owner_guide_path |
| --- | --- | --- | --- | --- | --- | --- |
| IT/运维/安全 | 11 | 11 | 0 | credential, nas, ops160 | 先补生产账号、数据库、对象存储、JWT、第三方和 NAS credential_ref、到期提醒和轮换证据。 | docs/production-readiness-evidence/owner-guides/it-ops-security.md |
| 财务 | 1 | 1 | 0 | credential | 按 owner guide 复制模板为目标 manifest，补外部证据链接、复核人、测试时间和 cred:// 引用。 | docs/production-readiness-evidence/owner-guides/finance.md |
| 技术实现负责人 | 1 | 1 | 0 | credential | 按 owner guide 复制模板为目标 manifest，补外部证据链接、复核人、测试时间和 cred:// 引用。 | docs/production-readiness-evidence/owner-guides/technical-owner.md |

## 5. P0 批次顺序

| batch_id | rank | owner_group | category | awaiting | item_ids | recommended_action |
| --- | --- | --- | --- | --- | --- | --- |
| BATCH-01 | 1 | IT/运维/安全 | credential | 5 | PRD-CRED-001,PRD-CRED-DB-001,PRD-CRED-JWT-001,PRD-CRED-NAS-001,PRD-CRED-OPS-001 | 先补生产账号、数据库、对象存储、JWT/第三方密钥的 credential_ref、到期提醒和轮换证据。 |
| BATCH-02 | 2 | 财务 | credential | 1 | PRD-CRED-001 | 先补生产账号、数据库、对象存储、JWT/第三方密钥的 credential_ref、到期提醒和轮换证据。 |
| BATCH-03 | 3 | 技术实现负责人 | credential | 1 | PRD-CRED-JWT-001 | 先补生产账号、数据库、对象存储、JWT/第三方密钥的 credential_ref、到期提醒和轮换证据。 |
| BATCH-04 | 4 | IT/运维/安全 | nas | 4 | PRD-NAS-ALERT-001,PRD-NAS-AUTOMOUNT-001,PRD-NAS-DRILL-CADENCE-001,PRD-NAS-SCHEDULE-001 | 先补 NAS 自动挂载、定时备份、容量/失败告警和周期恢复演练证据。 |
| BATCH-05 | 5 | IT/运维/安全 | ops160 | 2 | PRD-160-001,SEC-P0-OPS-RECOVERY-001 | 先补 160 生产运行配置、systemd、健康检查、日志和回滚证据。 |

## 6. P0 明细

| owner_group | category | item_id | status | template_path | target_manifest_path | operator_next_step |
| --- | --- | --- | --- | --- | --- | --- |
| IT/运维/安全 | credential | PRD-CRED-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-001/manifest-PRD-CRED-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| IT/运维/安全 | credential | PRD-CRED-DB-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-DB-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-DB-001/manifest-PRD-CRED-DB-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| IT/运维/安全 | credential | PRD-CRED-JWT-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-JWT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| IT/运维/安全 | credential | PRD-CRED-NAS-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-NAS-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-NAS-001/manifest-PRD-CRED-NAS-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| IT/运维/安全 | credential | PRD-CRED-OPS-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-OPS-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPS-001/manifest-PRD-CRED-OPS-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| IT/运维/安全 | nas | PRD-NAS-ALERT-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-ALERT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-ALERT-001/manifest-PRD-NAS-ALERT-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| IT/运维/安全 | nas | PRD-NAS-AUTOMOUNT-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-AUTOMOUNT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-AUTOMOUNT-001/manifest-PRD-NAS-AUTOMOUNT-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| IT/运维/安全 | nas | PRD-NAS-DRILL-CADENCE-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-DRILL-CADENCE-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-DRILL-CADENCE-001/manifest-PRD-NAS-DRILL-CADENCE-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| IT/运维/安全 | nas | PRD-NAS-SCHEDULE-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-SCHEDULE-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-SCHEDULE-001/manifest-PRD-NAS-SCHEDULE-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| IT/运维/安全 | ops160 | PRD-160-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-160-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-160-001/manifest-PRD-160-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| IT/运维/安全 | ops160 | SEC-P0-OPS-RECOVERY-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P0-OPS-RECOVERY-001.json | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P0-OPS-RECOVERY-001/manifest-SEC-P0-OPS-RECOVERY-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| 财务 | credential | PRD-CRED-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/finance/template-PRD-CRED-001.json | docs/production-readiness-evidence/submissions/finance/PRD-CRED-001/manifest-PRD-CRED-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| 技术实现负责人 | credential | PRD-CRED-JWT-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/technical-owner/template-PRD-CRED-JWT-001.json | docs/production-readiness-evidence/submissions/technical-owner/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |

## 7. 回填后复跑

```bash
npm run verify:production-evidence-submissions-guard
npm run docs:production-evidence-submission-status
npm run verify:production-evidence-submission-status
npm run docs:production-evidence-backfill-priority
npm run verify:production-evidence-backfill-priority
npm run docs:production-evidence-backfill-execution-ledger
npm run verify:production-evidence-backfill-execution-ledger
npm run docs:production-evidence-p0-quickstart
npm run verify:production-evidence-p0-quickstart
npm run verify:delivery-gates
```
