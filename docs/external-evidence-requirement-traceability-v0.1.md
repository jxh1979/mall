# 外部证据需求追踪报告 V0.1

## 1. 机器标记

- EXTERNAL_EVIDENCE_REQUIREMENT_TRACEABILITY_READY
- GENERATED_FROM_REQUIREMENTS_CODE_MATRIX
- GENERATED_FROM_PRODUCTION_EVIDENCE_LEDGER
- GENERATED_FROM_PRODUCTION_EVIDENCE_STATUS
- PARTIAL_REQUIREMENTS_HAVE_OWNER_MANIFESTS
- NO_FAKE_EXTERNAL_EVIDENCE_CLOSURE
- MOCK_IS_NOT_PRODUCTION_EVIDENCE

## 2. 定位

本文由 `tools/generate-external-evidence-requirement-traceability.mjs` 从 `docs/requirements-code-implementation-matrix-v0.1.csv`、`docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.json` 和 `docs/production-readiness-evidence/production-evidence-submission-status-latest.json` 自动生成，用于解决 8 条“部分实现”需求的验收追踪问题：代码和 mock/沙箱可支持联调与业务验收，但真实支付/POS/人脸/视频/机器人/设备/对象存储/开放平台凭证等生产证据未回填前，不得宣称生产闭环完成。

本文只证明“每条 partial 需求已经挂到责任组、证据 item_id、模板和目标 manifest 路径”，不关闭任何 blocker，不接收原始证据入库，也不允许把 mock 截图当生产证据。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| partial 需求 | 9 |
| 已挂证据台账的 partial 需求 | 8 |
| 去重证据 item_id | 0 |
| 证据台账行 | 0 |
| P0 证据台账行 | 0 |
| awaiting evidence 行 | 0 |
| invalid evidence 行 | 0 |
| 仍阻塞生产声明的需求 | 0 |

## 4. 需求到生产证据摘要

| 需求 | 模块 | 矩阵状态 | 生产证据追踪状态 | 证据 item_id | 责任组 | P0 行 | 待提交行 | 目标 manifest |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| # 总体标注(2026-06-22) | 全部模块 | 以文档/迁移/代码为准(总体标注) | covered_by_disabled_integration_switches |  |  | 0 | 0 |  |
| REQ-17 | 酒吧 | 部分实现 | covered_by_disabled_integration_switches |  |  | 0 | 0 |  |
| REQ-21 | 酒吧 | 部分实现 | covered_by_disabled_integration_switches |  |  | 0 | 0 |  |
| REQ-23b | 酒吧 | 部分实现 | covered_by_disabled_integration_switches |  |  | 0 | 0 |  |
| REQ-25 | 酒吧 | 部分实现 | covered_by_disabled_integration_switches |  |  | 0 | 0 |  |
| REQ-32 | 酒吧 | 部分实现 | covered_by_disabled_integration_switches |  |  | 0 | 0 |  |
| REQ-40 | 第三方 | 部分实现 | covered_by_disabled_integration_switches |  |  | 0 | 0 |  |
| REQ-41 | 第三方 | 部分实现 | covered_by_disabled_integration_switches |  |  | 0 | 0 |  |
| REQ-44 | 第三方 | 部分实现 | covered_by_disabled_integration_switches |  |  | 0 | 0 |  |

## 5. 执行台账明细

无。

## 6. 验收口径

1. 进入联调/业务验收：允许使用 mock、adapter、沙箱和本地证据链验证业务路径。
2. 进入生产上线声明：必须由责任组按目标 manifest 回填真实外部证据，并通过生产证据提交目录护栏、状态看板、回填优先级、执行台账、交付总闸口和安全扫描。
3. 对支付、人脸、视频、机器人、设备和开放平台凭证，任何明文密钥、token、密码、私钥正文都不得写入仓库；只能写 `cred://...` 引用。
4. 若供应商 SDK/API 尚未确定，需求保持 partial，不得在需求矩阵或最终验收审计中改写为生产完成。

## 7. 复跑命令

- `npm run docs:external-evidence-requirement-traceability`
- `npm run verify:external-evidence-requirement-traceability`
- `npm run verify:delivery-gates`
