# IT/运维/安全 生产证据回填说明 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_OWNER_GUIDE_READY
- OWNER_GUIDE_FROM_MANIFEST_TEMPLATES
- TEMPLATE_ONLY_NOT_PRODUCTION_EVIDENCE
- NO_OWNER_GUIDE_CLOSES_BLOCKERS
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET

## 2. 定位

本文只给 `IT/运维/安全` 使用，列出该责任组需要回填的生产证据模板、真实提交位置和复测命令。本文不关闭任何生产阻断项，不允许把模板、口头确认或 mock 当成生产证据。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| 待回填项 | 23 |
| P0 待回填项 | 19 |
| 当前 awaiting | 23 |
| 当前 valid | 0 |
| 当前 invalid | 0 |

## 4. 回填清单

| item_id | priority | category | current_status | template_path | target_manifest_path |
| --- | --- | --- | --- | --- | --- |
| PRD-160-001 | P0 | ops160 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-160-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-160-001/manifest-PRD-160-001.json |
| PRD-CRED-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-001/manifest-PRD-CRED-001.json |
| PRD-CRED-3P-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-3P-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-3P-001/manifest-PRD-CRED-3P-001.json |
| PRD-CRED-DB-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-DB-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-DB-001/manifest-PRD-CRED-DB-001.json |
| PRD-CRED-JWT-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-JWT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json |
| PRD-CRED-NAS-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-NAS-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-NAS-001/manifest-PRD-CRED-NAS-001.json |
| PRD-CRED-OBJ-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-OBJ-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OBJ-001/manifest-PRD-CRED-OBJ-001.json |
| PRD-CRED-OPEN-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-OPEN-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPEN-001/manifest-PRD-CRED-OPEN-001.json |
| PRD-CRED-OPS-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-OPS-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPS-001/manifest-PRD-CRED-OPS-001.json |
| PRD-FACE-001 | P0 | face_age | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-FACE-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-FACE-001/manifest-PRD-FACE-001.json |
| PRD-MDM-001 | P0 | MDM | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-MDM-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-MDM-001/manifest-PRD-MDM-001.json |
| PRD-NAS-ALERT-001 | P0 | nas | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-ALERT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-ALERT-001/manifest-PRD-NAS-ALERT-001.json |
| PRD-NAS-AUTOMOUNT-001 | P0 | nas | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-AUTOMOUNT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-AUTOMOUNT-001/manifest-PRD-NAS-AUTOMOUNT-001.json |
| PRD-NAS-DRILL-CADENCE-001 | P0 | nas | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-DRILL-CADENCE-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-DRILL-CADENCE-001/manifest-PRD-NAS-DRILL-CADENCE-001.json |
| PRD-NAS-SCHEDULE-001 | P0 | nas | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-SCHEDULE-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-SCHEDULE-001/manifest-PRD-NAS-SCHEDULE-001.json |
| PRD-PAY-001 | P0 | payment | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-PAY-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-PAY-001/manifest-PRD-PAY-001.json |
| PRD-PAY-002 | P0 | payment | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-PAY-002.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-PAY-002/manifest-PRD-PAY-002.json |
| SEC-P0-MDM-DEVICE-001 | P0 | MDM | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P0-MDM-DEVICE-001.json | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P0-MDM-DEVICE-001/manifest-SEC-P0-MDM-DEVICE-001.json |
| SEC-P0-OPS-RECOVERY-001 | P0 | ops160 | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P0-OPS-RECOVERY-001.json | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P0-OPS-RECOVERY-001/manifest-SEC-P0-OPS-RECOVERY-001.json |
| PRD-3P-001 | P1 | third_party | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-3P-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-3P-001/manifest-PRD-3P-001.json |
| PRD-WECOM-001 | P1 | wecom | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-WECOM-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-WECOM-001/manifest-PRD-WECOM-001.json |
| SEC-P1-WEBHOOK-REPLAY-001 | P1 | third_party | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P1-WEBHOOK-REPLAY-001.json | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P1-WEBHOOK-REPLAY-001/manifest-SEC-P1-WEBHOOK-REPLAY-001.json |
| SEC-P2-CSRF-API-001 | P2 | security | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P2-CSRF-API-001.json | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P2-CSRF-API-001/manifest-SEC-P2-CSRF-API-001.json |

## 5. 操作步骤

1. 从 `template_path` 复制 JSON 模板。
2. 放到对应 `target_manifest_path`，文件名保持 `manifest-<item_id>.json`。
3. 替换责任人、提交时间、证据链接、测试时间、复核人、credential_ref、回滚步骤和复测日期。
4. 删除或保留 `template_notice` 均可，但不得把模板默认值当成真实证据。
5. 运行以下命令：

```bash
npm run docs:production-evidence-submission-status
npm run verify:production-evidence-submission-status
npm run verify:delivery-gates
```

## 6. 禁止事项

1. 不得提交明文密码、token、API secret、私钥正文或供应商密钥。
2. 不得用 mock、口头确认、占位截图或未脱敏导出关闭生产阻断项。
3. 看板显示 `valid_submission` 后，仍需更新来源台账和 findings register，不能只靠本文关闭阻断。
