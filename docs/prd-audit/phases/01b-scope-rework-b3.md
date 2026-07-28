# 范围返工报告（B+3 三轨清单）
## 身份与权威哈希

```
HOST=y9000p-ai
USER=james
PWD=/home/james/complex-management
BRANCH=main
HEAD=b4290638dc591ccc4683a30e07fd0550eb8ece39
STATUS=## main...origin/main
```

```
0492b9112fec7ac03366f8c69a7a948f7706e9483b70370396359197f2ca087e  docs/business-system-map-for-operations-v0.1.html
a18cfff21c9cfbaf68d48ec6a0d68eeaa9afd3ae462dff76093c0a56dc31572b  docs/platform-governance-map-for-operations-v0.1.html
3e2071d8b740aea43df8a4685871c307a4185591ced4f1ced279ffa202e39648  docs/business-enum-quick-reference-v0.1.html
```

## 计数

| 轨 | 文件数 | 说明 |
|---|---:|---|
| MAIN | 34 | 主规则覆盖对象 |
| APPENDIX | 265 | 登记+漂移/冲突附录对象 |
| EXCLUDED | 694 | 含 `_archive` 等被扫到的排除文件 |
| EXCLUDED（不含 archive/运行证据） | 6 | 更接近现行树中排除 |
| 合计（清单文件行） | 993 | |

相对旧 manifest（33 INCLUDED）：
- STILL_MAIN：33
- NEW_MAIN：1 → docs/system-capability-layers-product-index-v0.1.md
- 旧 INCLUDED 被降级：0

## MAIN 清单（34）

- `docs/00-current-effective-document-entry-v0.1.md` (index)
- `docs/amusement-business-operations-product-prd-v0.1.md` (prd)
- `docs/bar-business-operations-product-prd-v0.1.md` (prd)
- `docs/building-operations-prd-v1.0.md` (prd)
- `docs/business-dashboard-metrics-product-prd-v0.1.md` (prd)
- `docs/business-decision-log-v0.1.md` (decision)
- `docs/business-enum-quick-reference-v0.1.html` (html_authority)
- `docs/business-html-source-of-truth-acceptance-spec-v0.1.md` (spec_or_acceptance)
- `docs/business-system-map-for-operations-v0.1.html` (html_authority)
- `docs/customer-app-miniapp-product-prd-v0.1.md` (prd)
- `docs/dividend-engine-prd-v0.1.md` (prd)
- `docs/evidence-risk-control-product-prd-v0.1.md` (prd)
- `docs/finance-settlement-product-prd-v0.1.md` (prd)
- `docs/group-governance-rebuild-prd-v1.0.md` (prd)
- `docs/leasing-acquisition-prd-v1.0.md` (prd)
- `docs/member-loyalty-and-stored-value-product-prd-v0.1.md` (prd)
- `docs/merchant-portal-zone-prd-v0.1.md` (prd)
- `docs/operation-traceability-and-terminal-switch-prd-v0.1.md` (traceability)
- `docs/platform-governance-map-for-operations-v0.1.html` (html_authority)
- `docs/platform-governance-prd-v0.1.md` (prd)
- `docs/platform-governance-product-spec-v0.2.md` (product_spec)
- `docs/product-documentation-index-v0.1.md` (index)
- `docs/product-requirements-v0.1.md` (prd)
- `docs/real-estate-full-chain-domain-prd-v0.1.md` (prd)
- `docs/requirement-traceability-matrix-v0.1.md` (matrix)
- `docs/settlement-rebuild-prd-v1.0.md` (prd)
- `docs/stored-value-membership-checkin-birthday-prd-v0.1.md` (prd)
- `docs/supply-chain-inventory-product-prd-v0.1.md` (prd)
- `docs/system-capability-layers-product-index-v0.1.md` (index)
- `docs/terminal-page-product-prd-v0.1.md` (prd)
- `docs/terminology-canonical-v1.0.md` (terminology)
- `docs/third-party-business-access-product-prd-v0.1.md` (prd)
- `docs/ticketing-entitlement-checkin-product-prd-v0.1.md` (prd)
- `docs/web-mobile-app-terminal-extension-product-prd-v0.1.md` (prd)

## APPENDIX 按类型计数

- **other**: 122
- **audit_report**: 42
- **contract**: 26
- **spec_or_acceptance**: 16
- **development_design**: 14
- **delivery**: 13
- **design_other**: 13
- **matrix**: 8
- **ops**: 5
- **openapi**: 4
- **index**: 1
- **traceability**: 1

## APPENDIX 关键子类

### openapi
- `docs/openapi/openapi-p0.json`
- `docs/openapi/openapi-p0.yaml`
- `docs/p0-openapi-contract-closure-appendix-v0.1.md`
- `docs/p0-openapi-operation-acceptance-matrix-v0.1.md`

### ops
- `docs/nas-backup-and-restore-runbook-v0.1.md`
- `docs/operations-reconciliation-and-exception-handling-runbook-v0.1.md`
- `docs/ops/keyvault-usb-runbook.md`
- `docs/production-credential-rotation-runbook-v0.1.md`
- `docs/server-160-runbook-v0.2.md`

### development_design（最多列 20）
- `docs/amusement-operations-development-design.md`
- `docs/android-device-terminal-development-design.md`
- `docs/bar-operations-development-design.md`
- `docs/business-entity-binding-development-design-v0.1.md`
- `docs/customer-app-miniapp-development-design-v0.1.md`
- `docs/data-risk-ai-development-design.md`
- `docs/finance-ar-ap-ledger-development-design-v0.1.md`
- `docs/frontend-admin-development-design.md`
- `docs/java-backend-platform-foundation-development-design.md`
- `docs/member-loyalty-and-stored-value-development-design-v0.1.md`
- `docs/open-platform-security-auth-development-design.md`
- `docs/supply-chain-inventory-development-design-v0.1.md`
- `docs/third-party-integration-development-design.md`
- `docs/ticketing-pass-entitlement-development-design.md`

## 与旧范围的关键差异

1. 旧正式范围只有 33 个 INCLUDED（PRD 窄范围）。
2. B+3 将 265 个相关人类维护文档纳入 APPENDIX，不再假装不存在。
3. OpenAPI / ops / 设计 / 交付 / 大量审计报告进入 APPENDIX，不进入主规则账本。
4. `_archive` 与运行证据仍排除；排除物可在验证声明时按需读。

## 分类局限

- 分类器是路径/命名规则，不是全文语义审读。
- `APPENDIX|other` 体量较大，若需收紧可再批人工重标。
- EXCLUDED 总数因递归进 `_archive` 被放大；看现行有效文档请以 MAIN+APPENDIX 为主。

## 产物

- `document-scope-manifest-b3.csv`
- `manifest-summary.json`
