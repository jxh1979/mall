# 生产证据责任组回填说明包 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_OWNER_GUIDES_READY
- OWNER_GUIDES_FROM_MANIFEST_TEMPLATES
- OWNER_GUIDES_NOT_REAL_PRODUCTION_EVIDENCE
- NO_OWNER_GUIDE_CLOSES_BLOCKERS
- VERIFY_PRODUCTION_EVIDENCE_OWNER_GUIDES_READY

## 2. 定位

本文从 `docs/production-readiness-evidence/production-evidence-manifest-template-index-latest.json` 和 `docs/production-readiness-evidence/production-evidence-submission-status-latest.json` 自动生成，把 manifest 模板和实际提交状态按责任组拆成可交付说明包。本文不创建真实证据，不关闭任何生产阻断项。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| 责任组说明 | 5 |
| 回填项 | 40 |
| P0 回填项 | 31 |
| 无效提交 | 0 |

## 4. 责任组说明清单

| owner_group | guide_path | items | p0_items | awaiting_submission | valid_submission | invalid_submission |
| --- | --- | --- | --- | --- | --- | --- |
| 财务 | docs/production-readiness-evidence/owner-guides/finance.md | 4 | 4 | 4 | 0 | 0 |
| IT/运维/安全 | docs/production-readiness-evidence/owner-guides/it-ops-security.md | 23 | 19 | 23 | 0 | 0 |
| 运营/法务 | docs/production-readiness-evidence/owner-guides/operations-legal.md | 3 | 2 | 3 | 0 | 0 |
| 采购/供应商联调 | docs/production-readiness-evidence/owner-guides/procurement-supplier.md | 6 | 4 | 6 | 0 | 0 |
| 技术实现负责人 | docs/production-readiness-evidence/owner-guides/technical-owner.md | 4 | 2 | 4 | 0 | 0 |

## 5. 使用规则

1. 将对应 `guide_path` 发给责任组。
2. 责任组按说明复制 manifest 模板并填入真实证据链接。
3. 每次提交后运行 `npm run docs:production-evidence-submission-status` 和 `npm run verify:delivery-gates`。
4. 本说明包只做分派和指导，不得作为生产证据或关闭依据。
