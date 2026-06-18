# 生产证据回填执行台账 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_BACKFILL_EXECUTION_LEDGER_READY
- ONE_ROW_PER_EVIDENCE_ITEM
- OPERATOR_BACKFILL_RUNBOOK_READY
- EXECUTION_LEDGER_DOES_NOT_CLOSE_BLOCKERS
- NO_RAW_EVIDENCE_IN_REPO

## 2. 定位

本文从 `docs/production-readiness-evidence/production-evidence-backfill-priority-latest.json`、`docs/production-readiness-evidence/production-evidence-submission-status-latest.json`、`docs/production-readiness-evidence/production-evidence-submission-kit-latest.json` 和 `docs/production-readiness-evidence/production-evidence-manifest-template-index-latest.json` 自动生成，用于把生产证据回填优先级拆成一行一个待补证据项的执行台账。本文只负责告诉责任组复制哪个模板、提交到哪个 manifest 路径、补哪些外部证据和复跑哪些命令；它不关闭任何生产阻断项，也不接收原始证据入库。

配套 CSV：`docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.csv`，用于给 IT、财务、采购、运营、法务和技术负责人分派执行。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| 台账行 | 15 |
| P0 台账行 | 13 |
| 等待提交 | 15 |
| 无效提交 | 0 |
| 责任组 | 3 |
| 批次 | 7 |

## 4. 执行台账

| ledger_id | batch_id | owner_group | priority | category | item_id | current_status | template_path | target_manifest_path | operator_next_step |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| BATCH-01-01 | BATCH-01 | IT/运维/安全 | P0 | credential | PRD-CRED-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-001/manifest-PRD-CRED-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-01-02 | BATCH-01 | IT/运维/安全 | P0 | credential | PRD-CRED-DB-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-DB-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-DB-001/manifest-PRD-CRED-DB-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-01-03 | BATCH-01 | IT/运维/安全 | P0 | credential | PRD-CRED-JWT-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-JWT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-01-04 | BATCH-01 | IT/运维/安全 | P0 | credential | PRD-CRED-NAS-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-NAS-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-NAS-001/manifest-PRD-CRED-NAS-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-01-05 | BATCH-01 | IT/运维/安全 | P0 | credential | PRD-CRED-OPS-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-OPS-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPS-001/manifest-PRD-CRED-OPS-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-02-01 | BATCH-02 | 财务 | P0 | credential | PRD-CRED-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/finance/template-PRD-CRED-001.json | docs/production-readiness-evidence/submissions/finance/PRD-CRED-001/manifest-PRD-CRED-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-03-01 | BATCH-03 | 技术实现负责人 | P0 | credential | PRD-CRED-JWT-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/technical-owner/template-PRD-CRED-JWT-001.json | docs/production-readiness-evidence/submissions/technical-owner/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-04-01 | BATCH-04 | IT/运维/安全 | P0 | nas | PRD-NAS-ALERT-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-ALERT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-ALERT-001/manifest-PRD-NAS-ALERT-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-04-02 | BATCH-04 | IT/运维/安全 | P0 | nas | PRD-NAS-AUTOMOUNT-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-AUTOMOUNT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-AUTOMOUNT-001/manifest-PRD-NAS-AUTOMOUNT-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-04-03 | BATCH-04 | IT/运维/安全 | P0 | nas | PRD-NAS-DRILL-CADENCE-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-DRILL-CADENCE-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-DRILL-CADENCE-001/manifest-PRD-NAS-DRILL-CADENCE-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-04-04 | BATCH-04 | IT/运维/安全 | P0 | nas | PRD-NAS-SCHEDULE-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-SCHEDULE-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-SCHEDULE-001/manifest-PRD-NAS-SCHEDULE-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-05-01 | BATCH-05 | IT/运维/安全 | P0 | ops160 | PRD-160-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-160-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-160-001/manifest-PRD-160-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-05-02 | BATCH-05 | IT/运维/安全 | P0 | ops160 | SEC-P0-OPS-RECOVERY-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P0-OPS-RECOVERY-001.json | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P0-OPS-RECOVERY-001/manifest-SEC-P0-OPS-RECOVERY-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-06-01 | BATCH-06 | 技术实现负责人 | P2 | security | SEC-P2-CSRF-API-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/technical-owner/template-SEC-P2-CSRF-API-001.json | docs/production-readiness-evidence/submissions/technical-owner/SEC-P2-CSRF-API-001/manifest-SEC-P2-CSRF-API-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |
| BATCH-07-01 | BATCH-07 | IT/运维/安全 | P2 | security | SEC-P2-CSRF-API-001 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P2-CSRF-API-001.json | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P2-CSRF-API-001/manifest-SEC-P2-CSRF-API-001.json | 复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。 |

## 5. 使用规则

1. 先按 `batch_rank` 从小到大处理，P0 优先。
2. 只允许把 manifest 放入 `target_manifest_path`；原始截图、导出、报告、合同、回执继续放在外部受控系统。
3. `credential_refs` 只能填写 `cred://...`，不得写明文密码、token、secret 或私钥正文。
4. 每个责任组回填后复跑 `npm run verify:production-evidence-submissions-guard`、`npm run docs:production-evidence-submission-status`、`npm run docs:production-evidence-backfill-priority`、`npm run docs:production-evidence-backfill-execution-ledger` 和 `npm run verify:delivery-gates`。
