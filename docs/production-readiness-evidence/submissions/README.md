# 生产证据提交目录

## 1. 机器标记

- PRODUCTION_EVIDENCE_SUBMISSIONS_GUARD_READY
- MANIFEST_ONLY_IN_REPO
- RAW_EVIDENCE_STAYS_EXTERNAL
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET
- NO_FAKE_PRODUCTION_EVIDENCE_CLOSURE

## 2. 使用规则

本目录只允许提交 `manifest-<item_id>.json`、本 `README.md` 和 `.gitignore`。真实截图、Excel、供应商 SDK 包、支付后台导出、企业微信配置导出、MDM 控制台导出、数据库导出、对象存储密钥、JWT 或任何原始附件都不得进入仓库。

真实证据必须存放在受控外部位置，例如 IT 工单、财务审批系统、供应商交付目录、NAS 受控证据目录或权限受控文档库，并在 manifest 的 `evidence_links` 中记录链接或编号。

## 3. 回填步骤

1. 从 `docs/production-readiness-evidence/submission-manifest-templates/` 复制对应模板。
2. 只把填写后的 `manifest-<item_id>.json` 放入对应 `submissions/<owner>/<item_id>/` 目录。
3. `credential_refs` 只能填写凭证引用，例如 `cred://...`，不得填写密码、token、secret 或私钥正文。
4. 运行 `npm run verify:production-evidence-submissions-guard`、`npm run docs:production-evidence-submission-status` 和 `npm run verify:delivery-gates`。

## 4. 禁止事项

1. 不得把模板、mock、口头确认或占位截图当作生产证据。
2. 不得提交除 manifest 以外的原始证据文件。
3. 不得用本目录绕过生产前置台账、阻断清单或 findings register 的关闭流程。
