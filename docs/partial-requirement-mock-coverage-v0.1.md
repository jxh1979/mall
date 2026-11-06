# Partial 需求 Mock/沙箱验收覆盖报告 V0.1

## 1. 机器标记

- PARTIAL_REQUIREMENT_MOCK_COVERAGE_READY
- PARTIAL_REQUIREMENTS_HAVE_MOCK_ACCEPTANCE_PATHS
- PARTIAL_REQUIREMENT_MOCK_COVERAGE_FROM_REQUIREMENT_MATRIX
- PARTIAL_REQUIREMENT_MOCK_COVERAGE_FROM_EXTERNAL_TRACEABILITY
- MOCK_IS_NOT_PRODUCTION_EVIDENCE
- NO_FAKE_EXTERNAL_EVIDENCE_CLOSURE
- INTEGRATION_CAPABILITY_SWITCHES_APPLIED

## 2. 定位

本文由 `tools/generate-partial-requirement-mock-coverage.mjs` 从 `docs/requirements-code-implementation-matrix-v0.1.csv` 和 `docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.json` 生成，用于证明 8 条 partial 需求虽然不能宣称生产闭环，但已有 mock、adapter、页面、后端测试或 E2E 验收替身，可以支撑联调和业务验收。

本文不关闭任何生产 blocker；真实支付/POS、人脸、视频、机器人、设备、对象存储、开放平台凭证和 MDM/NAS 证据仍必须按 production evidence manifest 回填。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| partial 需求 | 8 |
| mock/沙箱验收可执行 | 8 |
| 仍阻塞生产声明 | 0 |
| 去重 mock/测试/页面文件 | 21 |
| 去重生产 manifest 路径 | 0 |

## 4. 需求到 mock 验收矩阵

| 需求 | 模块 | 矩阵状态 | mock 验收状态 | 生产证据状态 | mock/测试/页面证据 | 生产 manifest 数 |
| --- | --- | --- | --- | --- | --- | --- |
| REQ-17 | 酒吧 | 部分实现 | mock_or_sandbox_acceptance_ready | covered_by_disabled_integration_switches | apps/web/src/admin/pages/BarOrdersPage.tsx<br>apps/web/src/admin/pages/StoredWinePage.tsx<br>apps/web/src/admin/pages/BarCommissionsPage.tsx<br>tests/e2e/full-admin-remaining.spec.ts<br>backend/src/test/java/com/complex/platform/bar/BarServiceTests.java<br>docs/t3b-supplier-sdk-mock-kit-v0.1.md | 0 |
| REQ-21 | 酒吧 | 部分实现 | mock_or_sandbox_acceptance_ready | covered_by_disabled_integration_switches | apps/web/src/admin/pages/BarOrdersPage.tsx<br>apps/web/src/admin/pages/ReconciliationPage.tsx<br>backend/src/test/java/com/complex/platform/bar/BarServiceTests.java<br>docs/supplier-mock/t3b-webhook-envelope.example.json<br>docs/p0-openapi-operation-acceptance-matrix-v0.1.md | 0 |
| REQ-23b | 酒吧 | 部分实现 | mock_or_sandbox_acceptance_ready | covered_by_disabled_integration_switches | backend/src/main/java/com/complex/platform/entry/FaceVerificationRouter.java<br>backend/src/test/java/com/complex/platform/entry/FaceVerificationGateTests.java<br>backend/src/test/java/com/complex/platform/bar/BarServiceTests.java<br>docs/third-party-supplier-integration-kit-v0.1.md | 0 |
| REQ-25 | 酒吧 | 部分实现 | mock_or_sandbox_acceptance_ready | covered_by_disabled_integration_switches | apps/web/src/admin/pages/BarTableSessionsPage.tsx<br>backend/src/test/java/com/complex/platform/bar/BarServiceTests.java<br>tests/e2e/support/mock-api.ts<br>docs/p0-openapi-operation-acceptance-matrix-v0.1.md | 0 |
| REQ-32 | 酒吧 | 部分实现 | mock_or_sandbox_acceptance_ready | covered_by_disabled_integration_switches | apps/web/src/admin/pages/InventoryLedgerPage.tsx<br>apps/web/src/admin/pages/StoredWinePage.tsx<br>backend/src/test/java/com/complex/platform/bar/BarServiceTests.java<br>tests/e2e/full-admin-remaining.spec.ts<br>tests/e2e/support/mock-api.ts | 0 |
| REQ-40 | 第三方 | 部分实现 | mock_or_sandbox_acceptance_ready | covered_by_disabled_integration_switches | apps/web/src/admin/pages/IntegrationEventsPage.tsx<br>backend/src/test/java/com/complex/platform/integration/IntegrationServiceTests.java<br>docs/t3b-supplier-sdk-mock-kit-v0.1.md<br>docs/supplier-mock/t3b-provider-adapter-test.example.json<br>docs/p0-openapi-operation-acceptance-matrix-v0.1.md | 0 |
| REQ-41 | 第三方 | 部分实现 | mock_or_sandbox_acceptance_ready | covered_by_disabled_integration_switches | apps/web/src/admin/pages/AuditEventsPage.tsx<br>apps/web/src/admin/pages/IntegrationEventsPage.tsx<br>tests/e2e/support/mock-api.ts<br>backend/src/test/java/com/complex/platform/evidence/EvidenceEventJdbcIntegrationTests.java<br>docs/third-party-supplier-integration-kit-v0.1.md | 0 |
| REQ-44 | 第三方 | 部分实现 | mock_or_sandbox_acceptance_ready | covered_by_disabled_integration_switches | apps/web/src/admin/pages/InventoryLedgerPage.tsx<br>apps/web/src/admin/pages/BarOrdersPage.tsx<br>backend/src/test/java/com/complex/platform/bar/BarServiceTests.java<br>tests/e2e/support/mock-api.ts<br>docs/android-terminal-breakdown.md | 0 |

## 5. 逐项验收说明

### REQ-17 酒吧

- 验收口径：桌台/卡座/吧台位/最低消费/点酒/支付/赠饮/账单/异常/存酒
- mock/沙箱覆盖：酒吧桌台/卡座/最低消费/点酒/赠饮/账单/异常/存酒 mock 验收闭环；真实支付、人脸、对象存储和供应商凭证仍按生产证据回填。
- 生产边界：covered_by_disabled_integration_switches；仍阻塞生产声明=否
- 生产证据 item：
- mock/测试/页面文件：
  - admin_ui: `apps/web/src/admin/pages/BarOrdersPage.tsx`，markers=`bar-orders-page`, `deliveryPhotoAttachmentIds`
  - admin_ui: `apps/web/src/admin/pages/StoredWinePage.tsx`，markers=`stored-wine-page`, `claim_items`
  - admin_ui: `apps/web/src/admin/pages/BarCommissionsPage.tsx`，markers=`bar-commissions-page`, `bar-commissions-kpi-sales`, `bar-commissions-kpi-non-sales`
  - ui_e2e: `tests/e2e/full-admin-remaining.spec.ts`，markers=`ADM-BAR-002`, `ADM-BAR-003`, `ADM-BAR-004`
  - backend_unit: `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`，markers=`billUsesOrderCentsAndReservationDepositWithoutTicketOrMinimumSpendOffset`, `deliveryPersistsEvidenceAndPhotoAttachmentsOnTicketEventAndStockMovement`
  - supplier_mock: `docs/t3b-supplier-sdk-mock-kit-v0.1.md`，markers=`T3B_SUPPLIER_MOCK_KIT_READY`, `NO_PRODUCTION_VENDOR_CONTROL`

### REQ-21 酒吧

- 验收口径：**吧台买酒优先线上支付，但必须支持现金和 POS（中国法律）**
- mock/沙箱覆盖：吧台买酒支付意图、现金/POS 账单口径和对账差异可 mock 验收；真实聚合支付商户、回调、退款和日结文件仍需生产证据。
- 生产边界：covered_by_disabled_integration_switches；仍阻塞生产声明=否
- 生产证据 item：
- mock/测试/页面文件：
  - admin_ui: `apps/web/src/admin/pages/BarOrdersPage.tsx`，markers=`paymentId`, `posBoundary`, `settle`
  - admin_ui: `apps/web/src/admin/pages/ReconciliationPage.tsx`，markers=`reconciliation-page`, `finance.reconciliation.difference_review_submitted`
  - backend_unit: `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`，markers=`barBillPayIntentReusesFinancePaymentIntentSemantics`, `partialSettlementKeepsOriginalPayableAndMarksPartialStatuses`
  - supplier_mock: `docs/supplier-mock/t3b-webhook-envelope.example.json`，markers=`payment.paid`, `idempotency_key`
  - openapi_contract: `docs/p0-openapi-operation-acceptance-matrix-v0.1.md`，markers=`/api/v1/bar/bills/{bill_id}/pay-intents`, `/api/v1/bar/bills/{bill_id}/settle`

### REQ-23b 酒吧

- 验收口径：酒吧入场/售酒年龄核验强制人脸或等效设备；供应商未定时只能 mock，生产不得正式通过
- mock/沙箱覆盖：年龄/人脸核验(2026 售酒豁免后,commit 3ed5f07 移除酒吧售酒验龄)由综合体入场的年龄核验环节统一把关(全场 18+);能力开关默认关、供应商未配置时 fail-safe 非阻断,生产须等真实人脸 SDK/API、合规材料和 credential_ref。
- 生产边界：covered_by_disabled_integration_switches；仍阻塞生产声明=否
- 生产证据 item：
- mock/测试/页面文件：
  - service: `backend/src/main/java/com/complex/platform/entry/FaceVerificationRouter.java`，markers=`FaceVerificationRouter`, `face.verification.enabled`
  - backend_unit: `backend/src/test/java/com/complex/platform/entry/FaceVerificationGateTests.java`，markers=`alwaysDisabledRouterReturnsReservedNonBlocking`, `switchReadFailureFailsSafeToDisabled`
  - backend_unit: `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`，markers=`scanOrderBindsOpenSessionInheritsEntryGateAndSourcesApp`
  - supplier_boundary: `docs/third-party-supplier-integration-kit-v0.1.md`，markers=`人脸/年龄核验`, `供应商未配置不得生产通过`

### REQ-25 酒吧

- 验收口径：**订台需支付定金**
- mock/沙箱覆盖：订台定金在账单生成、定金抵扣、支付意图和卡座会话中可 mock 验收；真实押金收款、退款和财务日结仍由支付证据关闭。
- 生产边界：covered_by_disabled_integration_switches；仍阻塞生产声明=否
- 生产证据 item：
- mock/测试/页面文件：
  - admin_ui: `apps/web/src/admin/pages/BarTableSessionsPage.tsx`，markers=`BarTableSessionsPage`, `reservation`
  - backend_unit: `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`，markers=`billUsesOrderCentsAndReservationDepositWithoutTicketOrMinimumSpendOffset`, `customer pays deposit first`
  - mock_api: `tests/e2e/support/mock-api.ts`，markers=`bar/reservations`, `depositAmount`
  - openapi_contract: `docs/p0-openapi-operation-acceptance-matrix-v0.1.md`，markers=`/api/v1/bar/reservations`, `/api/v1/customer/bar-reservations`

### REQ-32 酒吧

- 验收口径：**高价值报损/盘点差异/存酒作废必须拍照和视频留证 + 主管复核**
- mock/沙箱覆盖：高价值报损、盘点差异、存酒作废和照片/视频留证用附件 ID、库存流水、主管复核和 mock 事件验收；真实对象存储、视频平台和设备证据仍待回填。
- 生产边界：covered_by_disabled_integration_switches；仍阻塞生产声明=否
- 生产证据 item：
- mock/测试/页面文件：
  - admin_ui: `apps/web/src/admin/pages/InventoryLedgerPage.tsx`，markers=`inventory-ledger-page`, `evidence_attachment_ids`
  - admin_ui: `apps/web/src/admin/pages/StoredWinePage.tsx`，markers=`evidence_attachment_ids`, `在线校验`
  - backend_unit: `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`，markers=`deliveryPersistsEvidenceAndPhotoAttachmentsOnTicketEventAndStockMovement`, `att_delivery_photo`
  - ui_e2e: `tests/e2e/full-admin-remaining.spec.ts`，markers=`ADM-INV-001`, `ADM-GOV-006`, `evidence`
  - supplier_mock: `tests/e2e/support/mock-api.ts`，markers=`video`, `robot`, `webhook`

### REQ-40 第三方

- 验收口径：**优先自动留痕、尽量避免人工填写**，人工只补原因/照片/视频/签名
- mock/沙箱覆盖：第三方自动留痕通过开放平台 token、Webhook、provider adapter mock、重试/补偿/轮询页面和服务测试验收；真实 SDK/API 字段映射和签名材料未到位前不能生产关闭。
- 生产边界：covered_by_disabled_integration_switches；仍阻塞生产声明=否
- 生产证据 item：
- mock/测试/页面文件：
  - admin_ui: `apps/web/src/admin/pages/IntegrationEventsPage.tsx`，markers=`integration-events-page`, `Webhook + 定时轮询校验`
  - backend_unit: `backend/src/test/java/com/complex/platform/integration/IntegrationServiceTests.java`，markers=`webhookPersistsEventAndDuplicateIdempotencyReturnsOriginalResult`, `mockAdapterTestScenariosAreRecorded`, `orderSyncRetryCompensationAndReconciliationKeepProviderPayloadOutOfCoreModels`
  - supplier_mock: `docs/t3b-supplier-sdk-mock-kit-v0.1.md`，markers=`Webhook`, `provider adapter`, `NO_REAL_SECRET`
  - supplier_mock: `docs/supplier-mock/t3b-provider-adapter-test.example.json`，markers=`test-mock`, `mock-provider`
  - openapi_contract: `docs/p0-openapi-operation-acceptance-matrix-v0.1.md`，markers=`/api/open/d/{endpoint_key}/webhooks/{provider}/{event}`, `/api/v1/integration/provider-adapters/{id}/test-mock`

### REQ-41 第三方

- 验收口径：留痕与监控视频后续一一对应，P0 不强依赖真实视频
- mock/沙箱覆盖：留痕与监控视频对应关系在 mock 事件、附件引用和集成事件页面中可验收；真实视频平台 API、设备回调和证据链仍需供应商资料。
- 生产边界：covered_by_disabled_integration_switches；仍阻塞生产声明=否
- 生产证据 item：
- mock/测试/页面文件：
  - admin_ui: `apps/web/src/admin/pages/AuditEventsPage.tsx`，markers=`audit-events-page`, `eventTrace`
  - admin_ui: `apps/web/src/admin/pages/IntegrationEventsPage.tsx`，markers=`补偿处理`, `轮询校验`
  - mock_api: `tests/e2e/support/mock-api.ts`，markers=`video`, `evidence_attachment_ids`
  - backend_integration: `backend/src/test/java/com/complex/platform/evidence/EvidenceEventJdbcIntegrationTests.java`，markers=`EvidenceEventRecord`, `evidence.review.create`
  - supplier_boundary: `docs/third-party-supplier-integration-kit-v0.1.md`，markers=`设备/机器人/视频`, `超时告警并补偿`

### REQ-44 第三方

- 验收口径：P0 必须记录机器人或人工配送交接留痕、各环节扫码
- mock/沙箱覆盖：机器人或人工配送交接扫码留痕用库存移动、出品/送达、设备事件和集成 mock 验收；真实机器人/设备 API 与 MDM 纳管证据仍阻塞生产声明。
- 生产边界：covered_by_disabled_integration_switches；仍阻塞生产声明=否
- 生产证据 item：
- mock/测试/页面文件：
  - admin_ui: `apps/web/src/admin/pages/InventoryLedgerPage.tsx`，markers=`stock-movements`, `出库`
  - admin_ui: `apps/web/src/admin/pages/BarOrdersPage.tsx`，markers=`delivery`, `deliveryPhotoAttachmentIds`
  - backend_unit: `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`，markers=`deliveryRejectsOrderItemFromAnotherOrderAndKeepsBothOrdersPending`, `deliveryMarksProductionTicketDeliveredWithoutOverwritingCancelledTicket`
  - mock_api: `tests/e2e/support/mock-api.ts`，markers=`robot/task-events`, `mock-video`
  - supplier_boundary: `docs/android-terminal-breakdown.md`，markers=`机器人交接`, `inventory.handover_to_robot`

## 6. 复跑命令

- `npm run docs:partial-requirement-mock-coverage`
- `npm run verify:partial-requirement-mock-coverage`
- `npm run verify:delivery-gates`
