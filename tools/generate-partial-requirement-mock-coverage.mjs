import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");
const matrixPath = "docs/requirements-code-implementation-matrix-v0.1.csv";
const externalTracePath = "docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.json";
const jsonPath = "docs/partial-requirement-mock-coverage-latest.json";
const mdPath = "docs/partial-requirement-mock-coverage-v0.1.md";

const map = {
  "REQ-17": ["酒吧桌台/卡座/最低消费/点酒/赠饮/账单/异常/存酒 mock 验收闭环；真实支付、人脸、对象存储和供应商凭证仍按生产证据回填。", [
    ["admin_ui", "apps/web/src/admin/pages/BarOrdersPage.tsx", ["bar-orders-page", "deliveryPhotoAttachmentIds"]],
    ["admin_ui", "apps/web/src/admin/pages/StoredWinePage.tsx", ["stored-wine-page", "claim_items"]],
    ["admin_ui", "apps/web/src/admin/pages/BarCommissionsPage.tsx", ["bar-commissions-page", "15%", "10%"]],
    ["ui_e2e", "tests/e2e/full-admin-remaining.spec.ts", ["ADM-BAR-002", "ADM-BAR-003", "ADM-BAR-004"]],
    ["backend_unit", "backend/src/test/java/com/complex/platform/bar/BarServiceTests.java", ["billUsesOrderCentsAndReservationDepositWithoutTicketOrMinimumSpendOffset", "deliveryPersistsEvidenceAndPhotoAttachmentsOnTicketEventAndStockMovement"]],
    ["supplier_mock", "docs/t3b-supplier-sdk-mock-kit-v0.1.md", ["T3B_SUPPLIER_MOCK_KIT_READY", "NO_PRODUCTION_VENDOR_CONTROL"]],
  ]],
  "REQ-21": ["吧台买酒支付意图、现金/POS 账单口径和对账差异可 mock 验收；真实聚合支付商户、回调、退款和日结文件仍需生产证据。", [
    ["admin_ui", "apps/web/src/admin/pages/BarOrdersPage.tsx", ["paymentId", "posBoundary", "settle"]],
    ["admin_ui", "apps/web/src/admin/pages/ReconciliationPage.tsx", ["reconciliation-page", "manual_review_logged"]],
    ["backend_unit", "backend/src/test/java/com/complex/platform/bar/BarServiceTests.java", ["barBillPayIntentReusesFinancePaymentIntentSemantics", "partialSettlementKeepsOriginalPayableAndMarksPartialStatuses"]],
    ["supplier_mock", "docs/supplier-mock/t3b-webhook-envelope.example.json", ["payment.paid", "idempotency_key"]],
    ["openapi_contract", "docs/p0-openapi-operation-acceptance-matrix-v0.1.md", ["/api/v1/bar/bills/{bill_id}/pay-intents", "/api/v1/bar/bills/{bill_id}/settle"]],
  ]],
  "REQ-23b": ["年龄/人脸核验(2026 售酒豁免后,commit 3ed5f07 移除酒吧售酒验龄)由综合体入场的年龄核验环节统一把关(全场 18+);能力开关默认关、供应商未配置时 fail-safe 非阻断,生产须等真实人脸 SDK/API、合规材料和 credential_ref。", [
    ["service", "backend/src/main/java/com/complex/platform/entry/FaceVerificationRouter.java", ["FaceVerificationRouter", "face.verification.enabled"]],
    ["backend_unit", "backend/src/test/java/com/complex/platform/entry/FaceVerificationGateTests.java", ["alwaysDisabledRouterReturnsReservedNonBlocking", "switchReadFailureFailsSafeToDisabled"]],
    ["backend_unit", "backend/src/test/java/com/complex/platform/bar/BarServiceTests.java", ["scanOrderBindsOpenSessionInheritsEntryGateAndSourcesApp"]],
    ["supplier_boundary", "docs/third-party-supplier-integration-kit-v0.1.md", ["人脸/年龄核验", "供应商未配置不得生产通过"]],
  ]],
  "REQ-25": ["订台定金在账单生成、定金抵扣、支付意图和卡座会话中可 mock 验收；真实押金收款、退款和财务日结仍由支付证据关闭。", [
    ["admin_ui", "apps/web/src/admin/pages/BarTableSessionsPage.tsx", ["BarTableSessionsPage", "reservation"]],
    ["backend_unit", "backend/src/test/java/com/complex/platform/bar/BarServiceTests.java", ["billUsesOrderCentsAndReservationDepositWithoutTicketOrMinimumSpendOffset", "customer pays deposit first"]],
    ["mock_api", "tests/e2e/support/mock-api.ts", ["bar/reservations", "depositAmount"]],
    ["openapi_contract", "docs/p0-openapi-operation-acceptance-matrix-v0.1.md", ["/api/v1/bar/reservations", "/api/v1/customer/bar-reservations"]],
  ]],
  "REQ-32": ["高价值报损、盘点差异、存酒作废和照片/视频留证用附件 ID、库存流水、主管复核和 mock 事件验收；真实对象存储、视频平台和设备证据仍待回填。", [
    ["admin_ui", "apps/web/src/admin/pages/InventoryLedgerPage.tsx", ["inventory-ledger-page", "evidence_attachment_ids"]],
    ["admin_ui", "apps/web/src/admin/pages/StoredWinePage.tsx", ["evidence_attachment_ids", "在线校验"]],
    ["backend_unit", "backend/src/test/java/com/complex/platform/bar/BarServiceTests.java", ["deliveryPersistsEvidenceAndPhotoAttachmentsOnTicketEventAndStockMovement", "att_delivery_photo"]],
    ["ui_e2e", "tests/e2e/full-admin-remaining.spec.ts", ["ADM-INV-001", "ADM-GOV-006", "evidence"]],
    ["supplier_mock", "tests/e2e/support/mock-api.ts", ["video", "robot", "webhook"]],
  ]],
  "REQ-40": ["第三方自动留痕通过开放平台 token、Webhook、provider adapter mock、重试/补偿/轮询页面和服务测试验收；真实 SDK/API 字段映射和签名材料未到位前不能生产关闭。", [
    ["admin_ui", "apps/web/src/admin/pages/IntegrationEventsPage.tsx", ["integration-events-page", "Webhook + 定时轮询校验"]],
    ["backend_unit", "backend/src/test/java/com/complex/platform/integration/IntegrationServiceTests.java", ["webhookPersistsEventAndDuplicateIdempotencyReturnsOriginalResult", "mockAdapterTestScenariosAreRecorded", "orderSyncRetryCompensationAndReconciliationKeepProviderPayloadOutOfCoreModels"]],
    ["supplier_mock", "docs/t3b-supplier-sdk-mock-kit-v0.1.md", ["Webhook", "provider adapter", "NO_REAL_SECRET"]],
    ["supplier_mock", "docs/supplier-mock/t3b-provider-adapter-test.example.json", ["test-mock", "mock-provider"]],
    ["openapi_contract", "docs/p0-openapi-operation-acceptance-matrix-v0.1.md", ["/api/open/d/{endpoint_key}/webhooks/{provider}/{event}", "/api/v1/integration/provider-adapters/{id}/test-mock"]],
  ]],
  "REQ-41": ["留痕与监控视频对应关系在 mock 事件、附件引用和集成事件页面中可验收；真实视频平台 API、设备回调和证据链仍需供应商资料。", [
    ["admin_ui", "apps/web/src/admin/pages/AuditEventsPage.tsx", ["audit-events-page", "eventTrace"]],
    ["admin_ui", "apps/web/src/admin/pages/IntegrationEventsPage.tsx", ["补偿处理", "轮询校验"]],
    ["mock_api", "tests/e2e/support/mock-api.ts", ["video", "evidence_attachment_ids"]],
    ["backend_integration", "backend/src/test/java/com/complex/platform/evidence/EvidenceEventJdbcIntegrationTests.java", ["EvidenceEventRecord", "evidence.review.create"]],
    ["supplier_boundary", "docs/third-party-supplier-integration-kit-v0.1.md", ["设备/机器人/视频", "超时告警并补偿"]],
  ]],
  "REQ-44": ["机器人或人工配送交接扫码留痕用库存移动、出品/送达、设备事件和集成 mock 验收；真实机器人/设备 API 与 MDM 纳管证据仍阻塞生产声明。", [
    ["admin_ui", "apps/web/src/admin/pages/InventoryLedgerPage.tsx", ["stock-movements", "出库"]],
    ["admin_ui", "apps/web/src/admin/pages/BarOrdersPage.tsx", ["delivery", "deliveryPhotoAttachmentIds"]],
    ["backend_unit", "backend/src/test/java/com/complex/platform/bar/BarServiceTests.java", ["deliveryRejectsOrderItemFromAnotherOrderAndKeepsBothOrdersPending", "deliveryMarksProductionTicketDeliveredWithoutOverwritingCancelledTicket"]],
    ["mock_api", "tests/e2e/support/mock-api.ts", ["robot/task-events", "mock-video"]],
    ["supplier_boundary", "docs/android-terminal-breakdown.md", ["机器人交接", "inventory.handover_to_robot"]],
  ]],
};

function absolute(relativePath) { return path.join(repoRoot, relativePath); }
function readText(relativePath) { return fs.readFileSync(absolute(relativePath), "utf8").replace(/^\uFEFF/, ""); }
function readJson(relativePath) { return JSON.parse(readText(relativePath)); }
function normalize(content) { return content.replace(/\r\n/g, "\n").trimEnd() + "\n"; }
function parseCsv(text) {
  const rows = []; let row = []; let value = ""; let quoted = false;
  for (let i = 0; i < text.length; i += 1) {
    const c = text[i], n = text[i + 1];
    if (quoted) { if (c === '"' && n === '"') { value += '"'; i += 1; } else if (c === '"') quoted = false; else value += c; continue; }
    if (c === '"') { quoted = true; continue; }
    if (c === ",") { row.push(value); value = ""; continue; }
    if (c === "\n") { row.push(value.replace(/\r$/, "")); rows.push(row); row = []; value = ""; continue; }
    value += c;
  }
  if (value.length || row.length) { row.push(value); rows.push(row); }
  if (!rows.length) return [];
  const [headers, ...body] = rows;
  return body.filter((cells) => cells.some((cell) => String(cell).trim() !== "")).map((cells) => Object.fromEntries(headers.map((h, i) => [h.replace(/^\uFEFF/, ""), cells[i] ?? ""])));
}
function escapeCell(value) { return String(value ?? "").replace(/\|/g, "/").replace(/\r?\n/g, "<br>"); }
function mdTable(rows, columns) {
  if (!rows.length) return "无。\n";
  return [`| ${columns.map((c) => c.label).join(" | ")} |`, `| ${columns.map(() => "---").join(" | ")} |`, ...rows.map((r) => `| ${columns.map((c) => escapeCell(r[c.key])).join(" | ")} |`)].join("\n") + "\n";
}

const requirementRows = parseCsv(readText(matrixPath)).filter((row) => !String(row["需求编号"] ?? "").trim().startsWith("#"));
const externalTrace = readJson(externalTracePath);
const externalByRequirement = new Map((externalTrace.requirement_traceability ?? []).map((row) => [row.requirement_id, row]));
const partialRequirements = requirementRows.filter((row) => row["当前实现状态"] !== "已完整实现");
const failures = [];

for (const row of partialRequirements) {
  const id = row["需求编号"];
  if (!map[id]) failures.push(`${id} is partial but has no mock coverage mapping`);
  if (!externalByRequirement.has(id)) failures.push(`${id} is partial but missing from external traceability`);
}
for (const id of Object.keys(map)) {
  if (!partialRequirements.some((row) => row["需求编号"] === id)) failures.push(`${id} has mock coverage mapping but is not partial in requirement matrix`);
}

const coverageRows = partialRequirements.map((req) => {
  const id = req["需求编号"];
  const [scope = "", rawCoverage = []] = map[id] ?? [];
  const external = externalByRequirement.get(id) ?? {};
  const coverage = rawCoverage.map(([kind, file, markers]) => ({ kind, path: file, markers }));
  const row = {
    requirement_id: id,
    module: req["业务模块"],
    acceptance: req["验收标准"],
    current_status: req["当前实现状态"],
    matrix_gap: req["缺口"],
    mock_acceptance_scope: scope,
    mock_acceptance_status: coverage.length ? "mock_or_sandbox_acceptance_ready" : "missing_mock_coverage_mapping",
    production_trace_status: external.production_trace_status ?? "missing_external_traceability",
    production_blocking: external.production_blocking ?? true,
    mapped_item_ids: external.mapped_item_ids ?? [],
    target_manifest_paths: external.target_manifest_paths ?? [],
    mock_coverage: coverage,
    mock_coverage_files: [...new Set(coverage.map((item) => item.path))],
  };
  for (const item of coverage) {
    const file = absolute(item.path);
    if (!fs.existsSync(file)) { failures.push(`${id} mock coverage file missing: ${item.path}`); continue; }
    const text = fs.readFileSync(file, "utf8");
    for (const marker of item.markers) if (!text.includes(marker)) failures.push(`${id} ${item.path} must include marker ${marker}`);
  }
  const disabledBySwitch = row.production_trace_status === "covered_by_disabled_integration_switches";
  if (row.production_blocking !== true && !disabledBySwitch) {
    failures.push(`${id} must either remain production-blocking or be covered by disabled integration switches`);
  }
  if (!row.target_manifest_paths.length && !disabledBySwitch) {
    failures.push(`${id} must keep target production manifest paths unless disabled by integration switches`);
  }
  return row;
});

const counts = {
  partial_requirements: partialRequirements.length,
  mock_acceptance_ready: coverageRows.filter((row) => row.mock_acceptance_status === "mock_or_sandbox_acceptance_ready").length,
  production_blocking_requirements: coverageRows.filter((row) => row.production_blocking === true).length,
  unique_mock_files: new Set(coverageRows.flatMap((row) => row.mock_coverage_files)).size,
  unique_production_manifest_paths: new Set(coverageRows.flatMap((row) => row.target_manifest_paths)).size,
};
const output = {
  mock_coverage_version: "v0.1",
  status: failures.length ? "mock_coverage_gap" : "mock_or_sandbox_acceptance_ready_but_production_evidence_pending",
  generated_from: [matrixPath, externalTracePath],
  policy: {
    mock_is_not_production_evidence: true,
    integration_capability_switches_applied: true,
    no_fake_external_evidence_closure: true,
    partial_requirement_must_keep_production_manifest_paths: true,
    this_report_only_proves_business_acceptance_substitutes: true,
  },
  counts,
  requirement_mock_coverage: coverageRows,
};
const summaryRows = coverageRows.map((row) => ({
  requirement_id: row.requirement_id,
  module: row.module,
  current_status: row.current_status,
  mock_acceptance_status: row.mock_acceptance_status,
  production_trace_status: row.production_trace_status,
  mock_files: row.mock_coverage_files.join("<br>"),
  production_manifest_count: row.target_manifest_paths.length,
}));
const md = `# Partial 需求 Mock/沙箱验收覆盖报告 V0.1

## 1. 机器标记

- PARTIAL_REQUIREMENT_MOCK_COVERAGE_READY
- PARTIAL_REQUIREMENTS_HAVE_MOCK_ACCEPTANCE_PATHS
- PARTIAL_REQUIREMENT_MOCK_COVERAGE_FROM_REQUIREMENT_MATRIX
- PARTIAL_REQUIREMENT_MOCK_COVERAGE_FROM_EXTERNAL_TRACEABILITY
- MOCK_IS_NOT_PRODUCTION_EVIDENCE
- NO_FAKE_EXTERNAL_EVIDENCE_CLOSURE
- INTEGRATION_CAPABILITY_SWITCHES_APPLIED

## 2. 定位

本文由 \`tools/generate-partial-requirement-mock-coverage.mjs\` 从 \`${matrixPath}\` 和 \`${externalTracePath}\` 生成，用于证明 8 条 partial 需求虽然不能宣称生产闭环，但已有 mock、adapter、页面、后端测试或 E2E 验收替身，可以支撑联调和业务验收。

本文不关闭任何生产 blocker；真实支付/POS、人脸、视频、机器人、设备、对象存储、开放平台凭证和 MDM/NAS 证据仍必须按 production evidence manifest 回填。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| partial 需求 | ${counts.partial_requirements} |
| mock/沙箱验收可执行 | ${counts.mock_acceptance_ready} |
| 仍阻塞生产声明 | ${counts.production_blocking_requirements} |
| 去重 mock/测试/页面文件 | ${counts.unique_mock_files} |
| 去重生产 manifest 路径 | ${counts.unique_production_manifest_paths} |

## 4. 需求到 mock 验收矩阵

${mdTable(summaryRows, [
  { key: "requirement_id", label: "需求" },
  { key: "module", label: "模块" },
  { key: "current_status", label: "矩阵状态" },
  { key: "mock_acceptance_status", label: "mock 验收状态" },
  { key: "production_trace_status", label: "生产证据状态" },
  { key: "mock_files", label: "mock/测试/页面证据" },
  { key: "production_manifest_count", label: "生产 manifest 数" },
])}
## 5. 逐项验收说明

${coverageRows.map((row) => `### ${row.requirement_id} ${row.module}\n\n- 验收口径：${row.acceptance}\n- mock/沙箱覆盖：${row.mock_acceptance_scope}\n- 生产边界：${row.production_trace_status}；仍阻塞生产声明=${row.production_blocking ? "是" : "否"}\n- 生产证据 item：${row.mapped_item_ids.join(", ")}\n- mock/测试/页面文件：\n${row.mock_coverage.map((item) => `  - ${item.kind}: \`${item.path}\`，markers=${item.markers.map((marker) => `\`${marker}\``).join(", ")}`).join("\n")}\n`).join("\n")}
## 6. 复跑命令

- \`npm run docs:partial-requirement-mock-coverage\`
- \`npm run verify:partial-requirement-mock-coverage\`
- \`npm run verify:delivery-gates\`
`;
const expectedJson = JSON.stringify(output, null, 2) + "\n";
if (checkMode) {
  const currentJson = fs.existsSync(absolute(jsonPath)) ? normalize(fs.readFileSync(absolute(jsonPath), "utf8")) : "";
  const currentMd = fs.existsSync(absolute(mdPath)) ? normalize(fs.readFileSync(absolute(mdPath), "utf8")) : "";
  if (currentJson !== normalize(expectedJson)) failures.push(`${jsonPath} is not in sync with current partial requirement mock coverage`);
  if (currentMd !== normalize(md)) failures.push(`${mdPath} is not in sync with current partial requirement mock coverage`);
} else {
  fs.writeFileSync(absolute(jsonPath), normalize(expectedJson));
  fs.writeFileSync(absolute(mdPath), normalize(md));
  console.log(`wrote ${jsonPath}`);
  console.log(`wrote ${mdPath}`);
}
if (failures.length) {
  console.error("PARTIAL_REQUIREMENT_MOCK_COVERAGE_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}
if (checkMode) console.log("PARTIAL_REQUIREMENT_MOCK_COVERAGE_PASS");
