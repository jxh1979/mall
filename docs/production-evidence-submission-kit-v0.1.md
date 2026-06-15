# 生产证据提交包 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_SUBMISSION_KIT_READY
- SUBMISSION_KIT_FROM_OWNER_ACTION_PACKS
- AWAITING_REAL_PRODUCTION_EVIDENCE
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET
- VERIFY_PRODUCTION_EVIDENCE_SUBMISSION_KIT_READY

## 2. 定位

本文从 `docs/production-readiness-evidence/production-owner-action-packs-latest.json` 自动生成，把责任人动作包进一步转成可提交、可复核、可追溯的证据包结构。本文只定义提交格式，不改变生产前置台账、阻断清单或安全 findings 的状态。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| 责任组 | 5 |
| 证据提交模板 | 40 |
| P0 证据提交模板 | 31 |

## 4. 责任组提交根目录

| owner_group | status | submission_count | p0_count | packet_root |
| --- | --- | --- | --- | --- |
| IT/运维/安全 | awaiting_real_evidence | 23 | 19 | docs/production-readiness-evidence/submissions/it-ops-security/ |
| 财务 | awaiting_real_evidence | 4 | 4 | docs/production-readiness-evidence/submissions/finance/ |
| 采购/供应商联调 | awaiting_real_evidence | 6 | 4 | docs/production-readiness-evidence/submissions/procurement-supplier/ |
| 运营/法务 | awaiting_real_evidence | 3 | 2 | docs/production-readiness-evidence/submissions/operations-legal/ |
| 技术实现负责人 | awaiting_real_evidence | 4 | 2 | docs/production-readiness-evidence/submissions/technical-owner/ |

## 5. 提交模板清单

| submission_id | owner_group | item_id | priority | category | submission_status | packet_folder | manifest_file_name |
| --- | --- | --- | --- | --- | --- | --- | --- |
| SUB-it-ops-security-PRD-MDM-001 | IT/运维/安全 | PRD-MDM-001 | P0 | MDM | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-MDM-001/ | manifest-PRD-MDM-001.json |
| SUB-it-ops-security-PRD-PAY-001 | IT/运维/安全 | PRD-PAY-001 | P0 | payment | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-PAY-001/ | manifest-PRD-PAY-001.json |
| SUB-it-ops-security-PRD-PAY-002 | IT/运维/安全 | PRD-PAY-002 | P0 | payment | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-PAY-002/ | manifest-PRD-PAY-002.json |
| SUB-it-ops-security-PRD-FACE-001 | IT/运维/安全 | PRD-FACE-001 | P0 | face_age | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-FACE-001/ | manifest-PRD-FACE-001.json |
| SUB-it-ops-security-PRD-NAS-AUTOMOUNT-001 | IT/运维/安全 | PRD-NAS-AUTOMOUNT-001 | P0 | nas | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-AUTOMOUNT-001/ | manifest-PRD-NAS-AUTOMOUNT-001.json |
| SUB-it-ops-security-PRD-NAS-SCHEDULE-001 | IT/运维/安全 | PRD-NAS-SCHEDULE-001 | P0 | nas | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-SCHEDULE-001/ | manifest-PRD-NAS-SCHEDULE-001.json |
| SUB-it-ops-security-PRD-NAS-ALERT-001 | IT/运维/安全 | PRD-NAS-ALERT-001 | P0 | nas | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-ALERT-001/ | manifest-PRD-NAS-ALERT-001.json |
| SUB-it-ops-security-PRD-NAS-DRILL-CADENCE-001 | IT/运维/安全 | PRD-NAS-DRILL-CADENCE-001 | P0 | nas | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-DRILL-CADENCE-001/ | manifest-PRD-NAS-DRILL-CADENCE-001.json |
| SUB-it-ops-security-PRD-CRED-001 | IT/运维/安全 | PRD-CRED-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-001/ | manifest-PRD-CRED-001.json |
| SUB-it-ops-security-PRD-WECOM-001 | IT/运维/安全 | PRD-WECOM-001 | P1 | wecom | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-WECOM-001/ | manifest-PRD-WECOM-001.json |
| SUB-it-ops-security-PRD-3P-001 | IT/运维/安全 | PRD-3P-001 | P1 | third_party | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-3P-001/ | manifest-PRD-3P-001.json |
| SUB-it-ops-security-PRD-160-001 | IT/运维/安全 | PRD-160-001 | P0 | ops160 | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-160-001/ | manifest-PRD-160-001.json |
| SUB-it-ops-security-PRD-CRED-OPS-001 | IT/运维/安全 | PRD-CRED-OPS-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPS-001/ | manifest-PRD-CRED-OPS-001.json |
| SUB-it-ops-security-PRD-CRED-DB-001 | IT/运维/安全 | PRD-CRED-DB-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-DB-001/ | manifest-PRD-CRED-DB-001.json |
| SUB-it-ops-security-PRD-CRED-OBJ-001 | IT/运维/安全 | PRD-CRED-OBJ-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OBJ-001/ | manifest-PRD-CRED-OBJ-001.json |
| SUB-it-ops-security-PRD-CRED-JWT-001 | IT/运维/安全 | PRD-CRED-JWT-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-JWT-001/ | manifest-PRD-CRED-JWT-001.json |
| SUB-it-ops-security-PRD-CRED-3P-001 | IT/运维/安全 | PRD-CRED-3P-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-3P-001/ | manifest-PRD-CRED-3P-001.json |
| SUB-it-ops-security-PRD-CRED-OPEN-001 | IT/运维/安全 | PRD-CRED-OPEN-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-OPEN-001/ | manifest-PRD-CRED-OPEN-001.json |
| SUB-it-ops-security-PRD-CRED-NAS-001 | IT/运维/安全 | PRD-CRED-NAS-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/PRD-CRED-NAS-001/ | manifest-PRD-CRED-NAS-001.json |
| SUB-it-ops-security-SEC-P0-MDM-DEVICE-001 | IT/运维/安全 | SEC-P0-MDM-DEVICE-001 | P0 | MDM | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P0-MDM-DEVICE-001/ | manifest-SEC-P0-MDM-DEVICE-001.json |
| SUB-it-ops-security-SEC-P0-OPS-RECOVERY-001 | IT/运维/安全 | SEC-P0-OPS-RECOVERY-001 | P0 | ops160 | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P0-OPS-RECOVERY-001/ | manifest-SEC-P0-OPS-RECOVERY-001.json |
| SUB-it-ops-security-SEC-P1-WEBHOOK-REPLAY-001 | IT/运维/安全 | SEC-P1-WEBHOOK-REPLAY-001 | P1 | third_party | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P1-WEBHOOK-REPLAY-001/ | manifest-SEC-P1-WEBHOOK-REPLAY-001.json |
| SUB-it-ops-security-SEC-P2-CSRF-API-001 | IT/运维/安全 | SEC-P2-CSRF-API-001 | P2 | security | awaiting_real_evidence | docs/production-readiness-evidence/submissions/it-ops-security/SEC-P2-CSRF-API-001/ | manifest-SEC-P2-CSRF-API-001.json |
| SUB-finance-PRD-PAY-001 | 财务 | PRD-PAY-001 | P0 | payment | awaiting_real_evidence | docs/production-readiness-evidence/submissions/finance/PRD-PAY-001/ | manifest-PRD-PAY-001.json |
| SUB-finance-PRD-PAY-002 | 财务 | PRD-PAY-002 | P0 | payment | awaiting_real_evidence | docs/production-readiness-evidence/submissions/finance/PRD-PAY-002/ | manifest-PRD-PAY-002.json |
| SUB-finance-PRD-CRED-001 | 财务 | PRD-CRED-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/finance/PRD-CRED-001/ | manifest-PRD-CRED-001.json |
| SUB-finance-PRD-CRED-3P-001 | 财务 | PRD-CRED-3P-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/finance/PRD-CRED-3P-001/ | manifest-PRD-CRED-3P-001.json |
| SUB-procurement-supplier-PRD-MDM-001 | 采购/供应商联调 | PRD-MDM-001 | P0 | MDM | awaiting_real_evidence | docs/production-readiness-evidence/submissions/procurement-supplier/PRD-MDM-001/ | manifest-PRD-MDM-001.json |
| SUB-procurement-supplier-PRD-FACE-001 | 采购/供应商联调 | PRD-FACE-001 | P0 | face_age | awaiting_real_evidence | docs/production-readiness-evidence/submissions/procurement-supplier/PRD-FACE-001/ | manifest-PRD-FACE-001.json |
| SUB-procurement-supplier-PRD-3P-001 | 采购/供应商联调 | PRD-3P-001 | P1 | third_party | awaiting_real_evidence | docs/production-readiness-evidence/submissions/procurement-supplier/PRD-3P-001/ | manifest-PRD-3P-001.json |
| SUB-procurement-supplier-PRD-CRED-3P-001 | 采购/供应商联调 | PRD-CRED-3P-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/procurement-supplier/PRD-CRED-3P-001/ | manifest-PRD-CRED-3P-001.json |
| SUB-procurement-supplier-SEC-P0-MDM-DEVICE-001 | 采购/供应商联调 | SEC-P0-MDM-DEVICE-001 | P0 | MDM | awaiting_real_evidence | docs/production-readiness-evidence/submissions/procurement-supplier/SEC-P0-MDM-DEVICE-001/ | manifest-SEC-P0-MDM-DEVICE-001.json |
| SUB-procurement-supplier-SEC-P1-WEBHOOK-REPLAY-001 | 采购/供应商联调 | SEC-P1-WEBHOOK-REPLAY-001 | P1 | third_party | awaiting_real_evidence | docs/production-readiness-evidence/submissions/procurement-supplier/SEC-P1-WEBHOOK-REPLAY-001/ | manifest-SEC-P1-WEBHOOK-REPLAY-001.json |
| SUB-operations-legal-PRD-FACE-001 | 运营/法务 | PRD-FACE-001 | P0 | face_age | awaiting_real_evidence | docs/production-readiness-evidence/submissions/operations-legal/PRD-FACE-001/ | manifest-PRD-FACE-001.json |
| SUB-operations-legal-PRD-WECOM-001 | 运营/法务 | PRD-WECOM-001 | P1 | wecom | awaiting_real_evidence | docs/production-readiness-evidence/submissions/operations-legal/PRD-WECOM-001/ | manifest-PRD-WECOM-001.json |
| SUB-operations-legal-PRD-CRED-3P-001 | 运营/法务 | PRD-CRED-3P-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/operations-legal/PRD-CRED-3P-001/ | manifest-PRD-CRED-3P-001.json |
| SUB-technical-owner-PRD-CRED-JWT-001 | 技术实现负责人 | PRD-CRED-JWT-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/technical-owner/PRD-CRED-JWT-001/ | manifest-PRD-CRED-JWT-001.json |
| SUB-technical-owner-PRD-CRED-OPEN-001 | 技术实现负责人 | PRD-CRED-OPEN-001 | P0 | credential | awaiting_real_evidence | docs/production-readiness-evidence/submissions/technical-owner/PRD-CRED-OPEN-001/ | manifest-PRD-CRED-OPEN-001.json |
| SUB-technical-owner-SEC-P1-WEBHOOK-REPLAY-001 | 技术实现负责人 | SEC-P1-WEBHOOK-REPLAY-001 | P1 | third_party | awaiting_real_evidence | docs/production-readiness-evidence/submissions/technical-owner/SEC-P1-WEBHOOK-REPLAY-001/ | manifest-SEC-P1-WEBHOOK-REPLAY-001.json |
| SUB-technical-owner-SEC-P2-CSRF-API-001 | 技术实现负责人 | SEC-P2-CSRF-API-001 | P2 | security | awaiting_real_evidence | docs/production-readiness-evidence/submissions/technical-owner/SEC-P2-CSRF-API-001/ | manifest-SEC-P2-CSRF-API-001.json |

## 6. 单项 manifest 最低字段

每个 `manifest-<item_id>.json` 至少包含：

- submission_id
- item_id
- owner_group
- owner_person
- submitted_at
- evidence_ids
- evidence_links
- last_test_time
- result：只允许 passed、failed、partial、blocked
- redaction_confirmed
- reviewer
- credential_refs：涉及账号、支付、企微、供应商、NAS、JWT、对象存储、数据库时必填；只写引用，不写密钥正文
- rollback_step：涉及支付、凭证、供应商、160 配置、NAS 时必填
- next_retest_due：涉及周期演练、凭证轮换、MDM、NAS、160 运行配置时必填

## 7. 回填后必跑命令

1. `npm run docs:production-blockers`
2. `npm run docs:production-owner-actions`
3. `npm run docs:production-evidence-submission-kit`
4. `npm run verify:production-evidence-submission-kit`
5. `npm run verify:production-readiness-evidence`
6. `npm run verify:delivery-gates`

## 8. 禁止事项

1. 本提交包不能替代真实证据，不能把 `awaiting_real_evidence` 当作 passed。
2. 不得提交明文密码、明文 token、API secret、私钥正文、支付密钥、人脸供应商密钥、企业微信密钥或 NAS SMB 密码。
3. 只能提交 credential_ref、脱敏导出、工单、供应商回执、验收报告和机器命令输出。
4. 只有 mock、口头确认、占位截图、无责任人截图、无测试时间截图，均不得关闭生产阻断。
