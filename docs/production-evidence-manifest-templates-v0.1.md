# 生产证据 Manifest 模板 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_MANIFEST_TEMPLATES_READY
- TEMPLATE_ONLY_NOT_PRODUCTION_EVIDENCE
- DEFAULT_RESULT_BLOCKED_UNTIL_REAL_EVIDENCE
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET
- VERIFY_PRODUCTION_EVIDENCE_MANIFEST_TEMPLATES_READY

## 2. 定位

本文和配套 JSON 模板从 `docs/production-readiness-evidence/production-evidence-submission-kit-latest.json` 自动生成，供 IT、财务、采购、运营、法务和技术负责人复制填写。模板不是生产证据，默认 `result=blocked`，`redaction_confirmed=false`；复制到真实 submissions 目录前必须替换责任人、证据链接、测试时间、复核人、credential_ref 和回滚/复测字段。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| manifest 模板 | 15 |
| P0 manifest 模板 | 13 |
| 责任组 | 3 |

## 4. 模板清单

| template_id | owner_group | item_id | priority | category | result | template_path | target_manifest_path |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TPL-SUB-it-ops-security-PRD-NAS-AUTOMOUNT-001 | IT/运维/安全 | PRD-NAS-AUTOMOUNT-001 | P0 | nas | blocked | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-AUTOMOUNT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-AUTOMOUNT-001/manifest-PRD-NAS-AUTOMOUNT-001.json |
| TPL-SUB-it-ops-security-PRD-NAS-SCHEDULE-001 | IT/运维/安全 | PRD-NAS-SCHEDULE-001 | P0 | nas | blocked | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-SCHEDULE-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-SCHEDULE-001/manifest-PRD-NAS-SCHEDULE-001.json |
| TPL-SUB-it-ops-security-PRD-NAS-ALERT-001 | IT/运维/安全 | PRD-NAS-ALERT-001 | P0 | nas | blocked | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-ALERT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-ALERT-001/manifest-PRD-NAS-ALERT-001.json |
| TPL-SUB-it-ops-security-PRD-NAS-DRILL-CADENCE-001 | IT/运维/安全 | PRD-NAS-DRILL-CADENCE-001 | P0 | nas | blocked | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-DRILL-CADENCE-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-DRILL-CADENCE-001/manifest-PRD-NAS-DRILL-CADENCE-001.json |
| TPL-SUB-it-ops-security-PRD-CRED-001 | IT/运维/安全 | PRD-CRED-001 | P0 | credential | blocked | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-001/manifest-PRD-CRED-001.json |
| TPL-SUB-it-ops-security-PRD-160-001 | IT/运维/安全 | PRD-160-001 | P0 | ops160 | blocked | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-160-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-160-001/manifest-PRD-160-001.json |
| TPL-SUB-it-ops-security-PRD-CRED-OPS-001 | IT/运维/安全 | PRD-CRED-OPS-001 | P0 | credential | blocked | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-OPS-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPS-001/manifest-PRD-CRED-OPS-001.json |
| TPL-SUB-it-ops-security-PRD-CRED-DB-001 | IT/运维/安全 | PRD-CRED-DB-001 | P0 | credential | blocked | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-DB-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-DB-001/manifest-PRD-CRED-DB-001.json |
| TPL-SUB-it-ops-security-PRD-CRED-JWT-001 | IT/运维/安全 | PRD-CRED-JWT-001 | P0 | credential | blocked | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-JWT-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json |
| TPL-SUB-it-ops-security-PRD-CRED-NAS-001 | IT/运维/安全 | PRD-CRED-NAS-001 | P0 | credential | blocked | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-NAS-001.json | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-NAS-001/manifest-PRD-CRED-NAS-001.json |
| TPL-SUB-it-ops-security-SEC-P0-OPS-RECOVERY-001 | IT/运维/安全 | SEC-P0-OPS-RECOVERY-001 | P0 | ops160 | blocked | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P0-OPS-RECOVERY-001.json | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P0-OPS-RECOVERY-001/manifest-SEC-P0-OPS-RECOVERY-001.json |
| TPL-SUB-it-ops-security-SEC-P2-CSRF-API-001 | IT/运维/安全 | SEC-P2-CSRF-API-001 | P2 | security | blocked | docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P2-CSRF-API-001.json | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P2-CSRF-API-001/manifest-SEC-P2-CSRF-API-001.json |
| TPL-SUB-finance-PRD-CRED-001 | 财务 | PRD-CRED-001 | P0 | credential | blocked | docs/production-readiness-evidence/submission-manifest-templates/finance/template-PRD-CRED-001.json | docs/production-readiness-evidence/submissions/finance/PRD-CRED-001/manifest-PRD-CRED-001.json |
| TPL-SUB-technical-owner-PRD-CRED-JWT-001 | 技术实现负责人 | PRD-CRED-JWT-001 | P0 | credential | blocked | docs/production-readiness-evidence/submission-manifest-templates/technical-owner/template-PRD-CRED-JWT-001.json | docs/production-readiness-evidence/submissions/technical-owner/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json |
| TPL-SUB-technical-owner-SEC-P2-CSRF-API-001 | 技术实现负责人 | SEC-P2-CSRF-API-001 | P2 | security | blocked | docs/production-readiness-evidence/submission-manifest-templates/technical-owner/template-SEC-P2-CSRF-API-001.json | docs/production-readiness-evidence/submissions/technical-owner/SEC-P2-CSRF-API-001/manifest-SEC-P2-CSRF-API-001.json |

## 5. 使用规则

1. 从 `submission-manifest-templates/<owner>/template-<item_id>.json` 复制模板。
2. 放到 `production-evidence-submission-kit-v0.1.md` 指定的真实 `packet_folder`，文件名保持 `manifest-<item_id>.json`。
3. 替换所有占位内容，并将 `template_notice` 删除或保留为非证据说明。
4. 真实证据只允许写 `credential_ref`，不得写明文密码、token、API secret 或私钥。
5. 填完后运行 `npm run docs:production-evidence-submission-status` 和 `npm run verify:delivery-gates`。
