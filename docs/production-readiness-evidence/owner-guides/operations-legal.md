# 运营/法务 生产证据回填说明 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_OWNER_GUIDE_READY
- OWNER_GUIDE_FROM_MANIFEST_TEMPLATES
- TEMPLATE_ONLY_NOT_PRODUCTION_EVIDENCE
- NO_OWNER_GUIDE_CLOSES_BLOCKERS
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET
- RAW_EVIDENCE_STAYS_EXTERNAL

## 2. 定位

本文只给 `运营/法务` 使用，列出该责任组需要回填的生产证据模板、真实提交位置和复测命令。本文不关闭任何生产阻断项，不允许把模板、口头确认或 mock 当成生产证据。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| 待回填项 | 3 |
| P0 待回填项 | 2 |
| 当前 awaiting | 3 |
| 当前 valid | 0 |
| 当前 invalid | 0 |

## 4. 回填清单

| item_id | priority | category | current_status | template_path | target_manifest_path |
| --- | --- | --- | --- | --- | --- |
| PRD-CRED-3P-001 | P0 | credential | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/operations-legal/template-PRD-CRED-3P-001.json | docs/production-readiness-evidence/submissions/operations-legal/PRD-CRED-3P-001/manifest-PRD-CRED-3P-001.json |
| PRD-FACE-001 | P0 | face_age | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/operations-legal/template-PRD-FACE-001.json | docs/production-readiness-evidence/submissions/operations-legal/PRD-FACE-001/manifest-PRD-FACE-001.json |
| PRD-WECOM-001 | P1 | wecom | awaiting_submission | docs/production-readiness-evidence/submission-manifest-templates/operations-legal/template-PRD-WECOM-001.json | docs/production-readiness-evidence/submissions/operations-legal/PRD-WECOM-001/manifest-PRD-WECOM-001.json |

## 5. 操作步骤

1. 从 `template_path` 复制 JSON 模板。
2. 放到对应 `target_manifest_path`，文件名保持 `manifest-<item_id>.json`。
3. 替换责任人、提交时间、证据链接、测试时间、复核人、credential_ref、回滚步骤和复测日期；证据链接必须指向外部受控位置，不提交原始证据文件。
4. 删除或保留 `template_notice` 均可，但不得把模板默认值当成真实证据。
5. 运行以下命令：

```bash
npm run verify:production-evidence-submissions-guard
npm run docs:production-evidence-submission-status
npm run verify:production-evidence-submission-status
npm run verify:delivery-gates
```

## 6. 禁止事项

1. 不得提交明文密码、token、API secret、私钥正文或供应商密钥。
2. 不得用 mock、口头确认、占位截图或未脱敏导出关闭生产阻断项。
3. 看板显示 `valid_submission` 后，仍需更新来源台账和 findings register，不能只靠本文关闭阻断。
