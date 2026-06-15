# 生产证据提交状态看板 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_SUBMISSION_STATUS_READY
- STATUS_FROM_PRODUCTION_EVIDENCE_SUBMISSION_KIT
- MISSING_MANIFEST_IS_AWAITING_NOT_PASSED
- INVALID_MANIFEST_BLOCKS_DELIVERY_GATE
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET
- RAW_EVIDENCE_STAYS_EXTERNAL
- TRACEABLE_EVIDENCE_LINKS_REQUIRED
- CREDENTIAL_REF_PLACEHOLDER_FORBIDDEN

## 2. 定位

本文从 `docs/production-readiness-evidence/production-evidence-submission-kit-latest.json` 自动生成，并扫描各责任组提交目录下的 `manifest-<item_id>.json`。未提交 manifest 只显示为 `awaiting_submission`，不会被误判为 passed；一旦提交 manifest 出现缺字段、格式错误、明文密钥、占位值、模板专用字段、本地路径、localhost、不可追溯证据链接或关键字段不匹配，则进入 `invalid_submission` 并阻断交付闸口。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| 预期提交项 | 40 |
| 等待提交 | 40 |
| 有效提交 | 0 |
| 无效提交 | 0 |
| P0 等待提交 | 31 |
| P0 有效提交 | 0 |
| P0 无效提交 | 0 |

## 4. 责任组状态

| owner_group | expected_submissions | awaiting_submission | valid_submission | invalid_submission |
| --- | --- | --- | --- | --- |
| IT/运维/安全 | 23 | 23 | 0 | 0 |
| 技术实现负责人 | 4 | 4 | 0 | 0 |
| 财务 | 4 | 4 | 0 | 0 |
| 运营/法务 | 3 | 3 | 0 | 0 |
| 采购/供应商联调 | 6 | 6 | 0 | 0 |

## 5. 提交项状态

| submission_id | owner_group | item_id | priority | category | status | manifest_path | failures | warnings |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SUB-it-ops-security-PRD-160-001 | IT/运维/安全 | PRD-160-001 | P0 | ops160 | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-160-001/manifest-PRD-160-001.json |  |  |
| SUB-it-ops-security-PRD-3P-001 | IT/运维/安全 | PRD-3P-001 | P1 | third_party | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-3P-001/manifest-PRD-3P-001.json |  |  |
| SUB-it-ops-security-PRD-CRED-001 | IT/运维/安全 | PRD-CRED-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-001/manifest-PRD-CRED-001.json |  |  |
| SUB-it-ops-security-PRD-CRED-3P-001 | IT/运维/安全 | PRD-CRED-3P-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-3P-001/manifest-PRD-CRED-3P-001.json |  |  |
| SUB-it-ops-security-PRD-CRED-DB-001 | IT/运维/安全 | PRD-CRED-DB-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-DB-001/manifest-PRD-CRED-DB-001.json |  |  |
| SUB-it-ops-security-PRD-CRED-JWT-001 | IT/运维/安全 | PRD-CRED-JWT-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json |  |  |
| SUB-it-ops-security-PRD-CRED-NAS-001 | IT/运维/安全 | PRD-CRED-NAS-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-NAS-001/manifest-PRD-CRED-NAS-001.json |  |  |
| SUB-it-ops-security-PRD-CRED-OBJ-001 | IT/运维/安全 | PRD-CRED-OBJ-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OBJ-001/manifest-PRD-CRED-OBJ-001.json |  |  |
| SUB-it-ops-security-PRD-CRED-OPEN-001 | IT/运维/安全 | PRD-CRED-OPEN-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPEN-001/manifest-PRD-CRED-OPEN-001.json |  |  |
| SUB-it-ops-security-PRD-CRED-OPS-001 | IT/运维/安全 | PRD-CRED-OPS-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPS-001/manifest-PRD-CRED-OPS-001.json |  |  |
| SUB-it-ops-security-PRD-FACE-001 | IT/运维/安全 | PRD-FACE-001 | P0 | face_age | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-FACE-001/manifest-PRD-FACE-001.json |  |  |
| SUB-it-ops-security-PRD-MDM-001 | IT/运维/安全 | PRD-MDM-001 | P0 | MDM | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-MDM-001/manifest-PRD-MDM-001.json |  |  |
| SUB-it-ops-security-PRD-NAS-ALERT-001 | IT/运维/安全 | PRD-NAS-ALERT-001 | P0 | nas | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-ALERT-001/manifest-PRD-NAS-ALERT-001.json |  |  |
| SUB-it-ops-security-PRD-NAS-AUTOMOUNT-001 | IT/运维/安全 | PRD-NAS-AUTOMOUNT-001 | P0 | nas | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-AUTOMOUNT-001/manifest-PRD-NAS-AUTOMOUNT-001.json |  |  |
| SUB-it-ops-security-PRD-NAS-DRILL-CADENCE-001 | IT/运维/安全 | PRD-NAS-DRILL-CADENCE-001 | P0 | nas | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-DRILL-CADENCE-001/manifest-PRD-NAS-DRILL-CADENCE-001.json |  |  |
| SUB-it-ops-security-PRD-NAS-SCHEDULE-001 | IT/运维/安全 | PRD-NAS-SCHEDULE-001 | P0 | nas | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-SCHEDULE-001/manifest-PRD-NAS-SCHEDULE-001.json |  |  |
| SUB-it-ops-security-PRD-PAY-001 | IT/运维/安全 | PRD-PAY-001 | P0 | payment | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-PAY-001/manifest-PRD-PAY-001.json |  |  |
| SUB-it-ops-security-PRD-PAY-002 | IT/运维/安全 | PRD-PAY-002 | P0 | payment | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-PAY-002/manifest-PRD-PAY-002.json |  |  |
| SUB-it-ops-security-PRD-WECOM-001 | IT/运维/安全 | PRD-WECOM-001 | P1 | wecom | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/PRD-WECOM-001/manifest-PRD-WECOM-001.json |  |  |
| SUB-it-ops-security-SEC-P0-MDM-DEVICE-001 | IT/运维/安全 | SEC-P0-MDM-DEVICE-001 | P0 | MDM | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P0-MDM-DEVICE-001/manifest-SEC-P0-MDM-DEVICE-001.json |  |  |
| SUB-it-ops-security-SEC-P0-OPS-RECOVERY-001 | IT/运维/安全 | SEC-P0-OPS-RECOVERY-001 | P0 | ops160 | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P0-OPS-RECOVERY-001/manifest-SEC-P0-OPS-RECOVERY-001.json |  |  |
| SUB-it-ops-security-SEC-P1-WEBHOOK-REPLAY-001 | IT/运维/安全 | SEC-P1-WEBHOOK-REPLAY-001 | P1 | third_party | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P1-WEBHOOK-REPLAY-001/manifest-SEC-P1-WEBHOOK-REPLAY-001.json |  |  |
| SUB-it-ops-security-SEC-P2-CSRF-API-001 | IT/运维/安全 | SEC-P2-CSRF-API-001 | P2 | security | awaiting_submission | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P2-CSRF-API-001/manifest-SEC-P2-CSRF-API-001.json |  |  |
| SUB-technical-owner-PRD-CRED-JWT-001 | 技术实现负责人 | PRD-CRED-JWT-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/technical-owner/PRD-CRED-JWT-001/manifest-PRD-CRED-JWT-001.json |  |  |
| SUB-technical-owner-PRD-CRED-OPEN-001 | 技术实现负责人 | PRD-CRED-OPEN-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/technical-owner/PRD-CRED-OPEN-001/manifest-PRD-CRED-OPEN-001.json |  |  |
| SUB-technical-owner-SEC-P1-WEBHOOK-REPLAY-001 | 技术实现负责人 | SEC-P1-WEBHOOK-REPLAY-001 | P1 | third_party | awaiting_submission | docs/production-readiness-evidence/submissions/technical-owner/SEC-P1-WEBHOOK-REPLAY-001/manifest-SEC-P1-WEBHOOK-REPLAY-001.json |  |  |
| SUB-technical-owner-SEC-P2-CSRF-API-001 | 技术实现负责人 | SEC-P2-CSRF-API-001 | P2 | security | awaiting_submission | docs/production-readiness-evidence/submissions/technical-owner/SEC-P2-CSRF-API-001/manifest-SEC-P2-CSRF-API-001.json |  |  |
| SUB-finance-PRD-CRED-001 | 财务 | PRD-CRED-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/finance/PRD-CRED-001/manifest-PRD-CRED-001.json |  |  |
| SUB-finance-PRD-CRED-3P-001 | 财务 | PRD-CRED-3P-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/finance/PRD-CRED-3P-001/manifest-PRD-CRED-3P-001.json |  |  |
| SUB-finance-PRD-PAY-001 | 财务 | PRD-PAY-001 | P0 | payment | awaiting_submission | docs/production-readiness-evidence/submissions/finance/PRD-PAY-001/manifest-PRD-PAY-001.json |  |  |
| SUB-finance-PRD-PAY-002 | 财务 | PRD-PAY-002 | P0 | payment | awaiting_submission | docs/production-readiness-evidence/submissions/finance/PRD-PAY-002/manifest-PRD-PAY-002.json |  |  |
| SUB-operations-legal-PRD-CRED-3P-001 | 运营/法务 | PRD-CRED-3P-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/operations-legal/PRD-CRED-3P-001/manifest-PRD-CRED-3P-001.json |  |  |
| SUB-operations-legal-PRD-FACE-001 | 运营/法务 | PRD-FACE-001 | P0 | face_age | awaiting_submission | docs/production-readiness-evidence/submissions/operations-legal/PRD-FACE-001/manifest-PRD-FACE-001.json |  |  |
| SUB-operations-legal-PRD-WECOM-001 | 运营/法务 | PRD-WECOM-001 | P1 | wecom | awaiting_submission | docs/production-readiness-evidence/submissions/operations-legal/PRD-WECOM-001/manifest-PRD-WECOM-001.json |  |  |
| SUB-procurement-supplier-PRD-3P-001 | 采购/供应商联调 | PRD-3P-001 | P1 | third_party | awaiting_submission | docs/production-readiness-evidence/submissions/procurement-supplier/PRD-3P-001/manifest-PRD-3P-001.json |  |  |
| SUB-procurement-supplier-PRD-CRED-3P-001 | 采购/供应商联调 | PRD-CRED-3P-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submissions/procurement-supplier/PRD-CRED-3P-001/manifest-PRD-CRED-3P-001.json |  |  |
| SUB-procurement-supplier-PRD-FACE-001 | 采购/供应商联调 | PRD-FACE-001 | P0 | face_age | awaiting_submission | docs/production-readiness-evidence/submissions/procurement-supplier/PRD-FACE-001/manifest-PRD-FACE-001.json |  |  |
| SUB-procurement-supplier-PRD-MDM-001 | 采购/供应商联调 | PRD-MDM-001 | P0 | MDM | awaiting_submission | docs/production-readiness-evidence/submissions/procurement-supplier/PRD-MDM-001/manifest-PRD-MDM-001.json |  |  |
| SUB-procurement-supplier-SEC-P0-MDM-DEVICE-001 | 采购/供应商联调 | SEC-P0-MDM-DEVICE-001 | P0 | MDM | awaiting_submission | docs/production-readiness-evidence/submissions/procurement-supplier/SEC-P0-MDM-DEVICE-001/manifest-SEC-P0-MDM-DEVICE-001.json |  |  |
| SUB-procurement-supplier-SEC-P1-WEBHOOK-REPLAY-001 | 采购/供应商联调 | SEC-P1-WEBHOOK-REPLAY-001 | P1 | third_party | awaiting_submission | docs/production-readiness-evidence/submissions/procurement-supplier/SEC-P1-WEBHOOK-REPLAY-001/manifest-SEC-P1-WEBHOOK-REPLAY-001.json |  |  |

## 6. 使用规则

1. 责任人提交真实材料时，只在对应 `packet_folder` 下放置 `manifest-<item_id>.json`；原始证据文件必须存放在外部受控位置，并通过 `evidence_links` 引用。
2. manifest 必须使用 `production-evidence-submission-kit-v0.1.md` 定义的字段，真实密钥只能写 `credential_ref`；`evidence_links`、`failure_case_evidence` 和 `approval_record` 必须是外部可追溯链接或批准的证据编号，不能是 TODO、示例值、本地路径或 localhost。
3. 提交后执行 `npm run verify:production-evidence-submissions-guard`、`npm run docs:production-evidence-submission-status` 和 `npm run verify:production-evidence-submission-status`。
4. 看板为 `valid_submission` 也不自动关闭阻断项；仍需更新来源台账、findings register 和复跑交付闸口。
