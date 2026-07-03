import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const failures = [];

function readText(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath);
  if (!fs.existsSync(absolutePath)) {
    failures.push(`${relativePath} is missing`);
    return "";
  }
  return fs.readFileSync(absolutePath, "utf8").replace(/^\uFEFF/, "");
}

function readJson(relativePath) {
  const text = readText(relativePath);
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch (error) {
    failures.push(`${relativePath} is not valid JSON: ${error.message}`);
    return null;
  }
}

function requireIncludes(relativePath, markers) {
  const text = readText(relativePath);
  for (const marker of markers) {
    if (!text.includes(marker)) failures.push(`${relativePath} must include marker: ${marker}`);
  }
  return text;
}

function escapeRegex(text) {
  return String(text).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function requireRegex(relativePath, pattern, description) {
  const text = readText(relativePath);
  if (!pattern.test(text)) failures.push(`${relativePath} must include ${description}`);
}

function countLines(relativePath) {
  return readText(relativePath).split(/\r?\n/).filter((_, index, lines) => index < lines.length - 1 || lines[index] !== "").length;
}

function parseCsv(text) {
  const rows = [];
  let row = [];
  let value = "";
  let quoted = false;
  for (let index = 0; index < text.length; index += 1) {
    const char = text[index];
    const next = text[index + 1];
    if (quoted) {
      if (char === '"' && next === '"') {
        value += '"';
        index += 1;
      } else if (char === '"') {
        quoted = false;
      } else {
        value += char;
      }
      continue;
    }
    if (char === '"') {
      quoted = true;
      continue;
    }
    if (char === ',') {
      row.push(value);
      value = "";
      continue;
    }
    if (char === '\n') {
      row.push(value.replace(/\r$/, ""));
      rows.push(row);
      row = [];
      value = "";
      continue;
    }
    value += char;
  }
  if (value.length > 0 || row.length > 0) {
    row.push(value);
    rows.push(row);
  }
  if (rows.length === 0) return [];
  const [headers, ...body] = rows;
  return body.filter((cells) => cells.some((cell) => String(cell).trim() !== "")).map((cells) => Object.fromEntries(headers.map((header, index) => [header.replace(/^\uFEFF/, ""), cells[index] ?? ""])));
}

function csvRows(relativePath) {
  return parseCsv(readText(relativePath));
}

const finalAuditPath = "docs/final-closed-loop-acceptance-audit-v0.1.md";
const finalAudit = requireIncludes(finalAuditPath, [
  "FINAL_CLOSED_LOOP_AUDIT_READY",
  "FINAL_AUDIT_FROM_REQUIREMENTS_CODE_MATRIX",
  "FINAL_AUDIT_FROM_CURRENT_CODE_LINE_COUNTS",
  "FINAL_AUDIT_FROM_SEMGREP_LATEST_SUMMARY",
  "FINAL_AUDIT_FROM_PRODUCTION_EVIDENCE_STATUS",
  "FINAL_AUDIT_FROM_EXTERNAL_EVIDENCE_TRACEABILITY",
  "FINAL_AUDIT_FROM_PARTIAL_REQUIREMENT_MOCK_COVERAGE",
  "FINAL_AUDIT_FROM_PRODUCTION_OWNER_ACTION_MANIFEST_LINKS",
  "FINAL_AUDIT_FROM_NAS_CURRENT_STATE_DRIFT",
  "FINAL_AUDIT_FROM_CLOSED_LOOP_CURRENT_STATE",
  "FINAL_AUDIT_FROM_REAL_BACKEND_UI",
  "NO_PRODUCTION_READY_CLAIM_WITH_OPEN_MANIFESTS",
  "production-evidence-closure-gap-report-v0.1.md",
  "production-existing-evidence-to-manifest-gap-audit-v0.1.md",
  "nas-current-state-drift-report-v0.1.md",
  "external-evidence-requirement-traceability-v0.1.md",
  "partial-requirement-mock-coverage-v0.1.md",
  "production-owner-action-packs-v0.1.md",
  "OWNER_ACTIONS_LINKED_TO_EVIDENCE_MANIFESTS",
  "closed-loop-current-state-snapshot-v0.1.md",
  "verify:closed-loop-current-state",
  "verify:ui:real-backend:seeded",
  "verify:closed-loop-runtime",
  "verify:production-closure-gap-reports",
  "verify:external-evidence-requirement-traceability",
  "verify:partial-requirement-mock-coverage",
  "verify:production-owner-actions",
]);

const requirementRows = csvRows("docs/requirements-code-implementation-matrix-v0.1.csv");
const mvpRows = csvRows("docs/full-admin-mvp-requirements-code-matrix-v0.1.csv");
const remainingRows = csvRows("docs/full-admin-remaining-requirements-code-matrix-v0.1.csv");
const implementedRequirements = requirementRows.filter((row) => row["当前实现状态"] === "已完整实现").length;
const partialRequirements = requirementRows.length - implementedRequirements;
const mvpImplemented = mvpRows.filter((row) => row["当前实现状态"] === "已完整实现").length;
const remainingImplemented = remainingRows.filter((row) => row["status"] === "已实现").length;

for (const [matrixPath, total, done, gap] of [
  ["docs/requirements-code-implementation-matrix-v0.1.csv", requirementRows.length, implementedRequirements, partialRequirements],
  ["docs/full-admin-mvp-requirements-code-matrix-v0.1.csv", mvpRows.length, mvpImplemented, mvpRows.length - mvpImplemented],
  ["docs/full-admin-remaining-requirements-code-matrix-v0.1.csv", remainingRows.length, remainingImplemented, remainingRows.length - remainingImplemented],
]) {
  const matrixPattern = new RegExp(`\\|\\s*\`${escapeRegex(matrixPath)}\`\\s*\\|\\s*${total}\\s*\\|\\s*${done}\\s*\\|\\s*${gap}\\s*\\|`);
  requireRegex(finalAuditPath, matrixPattern, `${matrixPath} matrix counts ${total}/${done}/${gap}`);
}

const expectedLineCounts = {
  "apps/web/src/main.tsx": countLines("apps/web/src/main.tsx"),
  "backend/src/main/java/com/complex/platform/ticketing/TicketingService.java": countLines("backend/src/main/java/com/complex/platform/ticketing/TicketingService.java"),
  "backend/src/main/java/com/complex/platform/bar/BarService.java": countLines("backend/src/main/java/com/complex/platform/bar/BarService.java"),
  "backend/src/main/java/com/complex/platform/bar/BarOrderService.java": countLines("backend/src/main/java/com/complex/platform/bar/BarOrderService.java"),
  "backend/src/main/java/com/complex/platform/bar/BarBillingService.java": countLines("backend/src/main/java/com/complex/platform/bar/BarBillingService.java"),
  "backend/src/main/java/com/complex/platform/bar/BarTableSessionService.java": countLines("backend/src/main/java/com/complex/platform/bar/BarTableSessionService.java"),
};
for (const [file, lineCount] of Object.entries(expectedLineCounts)) {
  const basename = path.basename(file);
  requireRegex(finalAuditPath, new RegExp(`${escapeRegex(basename)}[^\n]*${lineCount}\\s*行`), `${basename} current line count ${lineCount}`);
}

requireRegex(finalAuditPath, new RegExp(`真实后端 UI 验收[^\n]*verify:ui:real-backend:seeded[^\n]*REAL-UI-02[^\n]*5174`), "real backend UI acceptance row");
requireRegex(finalAuditPath, new RegExp(`闭环运行态总验收[^\n]*verify:closed-loop-runtime`), "closed loop runtime verification row");

const semgrep = readJson("security-scans/semgrep-latest-summary.json");
if (semgrep) {
  const semgrepResults = semgrep.result_count ?? semgrep.results ?? semgrep.results_count ?? semgrep.findings;
  const semgrepErrors = Array.isArray(semgrep.errors) ? semgrep.errors.length : (semgrep.error_count ?? semgrep.errors ?? semgrep.errors_count);
  const semgrepPathsScanned = Array.isArray(semgrep.paths_scanned) ? semgrep.paths_scanned.length : (semgrep.scanned_path_count ?? semgrep.paths_scanned ?? semgrep.paths_scanned_count ?? semgrep.scanned_count);
  const semgrepScanRef = semgrep.scan_ref ?? semgrep.scan_id ?? semgrep.scan_dir ?? semgrep.scanDir ?? semgrep.raw_result_file ?? semgrep.latest_file;
  requireRegex(finalAuditPath, new RegExp(`results=${escapeRegex(semgrepResults)}`), `Semgrep results=${semgrepResults}`);
  requireRegex(finalAuditPath, new RegExp(`errors=${escapeRegex(semgrepErrors)}`), `Semgrep errors=${semgrepErrors}`);
  if (semgrepPathsScanned !== undefined) {
    requireRegex(finalAuditPath, new RegExp(`paths_scanned=${escapeRegex(semgrepPathsScanned)}`), `Semgrep paths_scanned=${semgrepPathsScanned}`);
  }
  if (semgrepScanRef) {
    requireRegex(finalAuditPath, new RegExp(escapeRegex(semgrepScanRef)), `Semgrep scan ref ${semgrepScanRef}`);
  }
}

const runtimeEvidence = readJson("docs/runtime-acceptance-evidence-latest.json");
if (runtimeEvidence) {
  const webUnitTests = runtimeEvidence.web_unit_source?.test_count_from_source ?? 0;
  const uiTests = runtimeEvidence.playwright?.normal?.test_count_from_source ?? 0;
  const realBackendUiTests = runtimeEvidence.playwright?.real_backend?.test_count_from_source ?? 0;
  requireRegex(finalAuditPath, new RegExp(`Web 单元测试[^\n]*${escapeRegex(webUnitTests)}|${escapeRegex(webUnitTests)} 条前端单元测试`), `web unit test count ${webUnitTests}`);
  requireRegex(finalAuditPath, new RegExp(`UI 自动化[^\n]*${escapeRegex(uiTests)} 条|${escapeRegex(uiTests)} 条 Playwright`), `Playwright UI count ${uiTests}`);
  requireRegex(finalAuditPath, new RegExp(`真实后端 UI 验收[^\n]*${escapeRegex(realBackendUiTests)} 条`), `real backend UI count ${realBackendUiTests}`);
}

const implementationAuditPath = "docs/code-implementation-acceptance-audit-v0.1.md";
const implementationAudit = readText(implementationAuditPath);
if (implementationAudit) {
  requireRegex(implementationAuditPath, new RegExp(`总需求数：${requirementRows.length}`), `implementation audit total requirements ${requirementRows.length}`);
  requireRegex(implementationAuditPath, new RegExp(`已完整实现：${implementedRequirements}`), `implementation audit implemented requirements ${implementedRequirements}`);
  requireRegex(implementationAuditPath, new RegExp(`部分实现：${partialRequirements}`), `implementation audit partial requirements ${partialRequirements}`);
  requireRegex(implementationAuditPath, new RegExp(`${escapeRegex(runtimeEvidence?.playwright?.normal?.test_count_from_source ?? "")} 条 Playwright|UI 验收通过：${escapeRegex(runtimeEvidence?.playwright?.normal?.test_count_from_source ?? "")}`), "implementation audit current UI count");
}

const submissionStatus = readJson("docs/production-readiness-evidence/production-evidence-submission-status-latest.json");
const nasDrift = readJson("docs/production-readiness-evidence/nas-current-state-drift-latest.json");
const closedLoopSnapshot = readJson("docs/closed-loop-current-state-snapshot-latest.json");
if (submissionStatus) {
  const counts = submissionStatus.counts ?? {};
  const p0Total = (counts.p0_awaiting_submission ?? 0) + (counts.p0_valid_submission ?? 0) + (counts.p0_invalid_submission ?? 0);
  requireRegex(finalAuditPath, new RegExp(`${escapeRegex(counts.expected_submissions)}\\s*份\\s*manifest`), `${counts.expected_submissions} manifest count`);
  requireRegex(finalAuditPath, new RegExp(`${escapeRegex(p0Total)}\\s*份.*P0|P0.*${escapeRegex(p0Total)}\\s*份`), `${p0Total} P0 manifest count`);
  if ((counts.valid_submission ?? 0) > 0 && /可以直接生产上线：是/.test(finalAudit)) {
    failures.push("final audit must not claim direct production launch while valid submissions are only partial evidence");
  }
  if ((counts.awaiting_submission ?? 0) > 0 && /可以直接生产上线：是/.test(finalAudit)) {
    failures.push("final audit must not claim direct production launch with awaiting submissions");
  }
}

const productionOwnerActions = readJson("docs/production-readiness-evidence/production-owner-action-packs-latest.json");
if (productionOwnerActions) {
  const counts = productionOwnerActions.counts ?? {};
  requireRegex(finalAuditPath, new RegExp(`unique_unresolved_items=${escapeRegex(counts.unique_unresolved_items)}`), `owner action unique unresolved items ${counts.unique_unresolved_items}`);
  requireRegex(finalAuditPath, new RegExp(`duplicated_owner_action_rows=${escapeRegex(counts.duplicated_owner_action_rows)}`), `owner action duplicated rows ${counts.duplicated_owner_action_rows}`);
  requireRegex(finalAuditPath, new RegExp(`related_submission_rows=${escapeRegex(counts.related_submission_rows)}`), `owner action related submission rows ${counts.related_submission_rows}`);
  requireRegex(finalAuditPath, new RegExp(`items_without_manifest=${escapeRegex(counts.items_without_manifest)}`), `owner action items_without_manifest ${counts.items_without_manifest}`);
  if (counts.items_without_manifest !== 0) failures.push("owner action packs must not have action rows without manifest links");
}



if (nasDrift) {
  const liveStatus = nasDrift.current_state?.live_status;
  requireRegex(finalAuditPath, new RegExp(`NAS[^\n]*live[^\n]*${escapeRegex(liveStatus)}|${escapeRegex(liveStatus)}[^\n]*NAS[^\n]*live`, "i"), `NAS current live status ${liveStatus}`);
  if (nasDrift.production_closure_assessment?.can_close_sec_p0_ops_recovery === false) {
    requireRegex(finalAuditPath, /SEC-P0-OPS-RECOVERY-001[^\n]*(不可|不能|仍|继续).*关闭|不可直接上线原因[^\n]*NAS/i, "SEC-P0-OPS-RECOVERY remains open due to NAS drift");
  }
}


if (closedLoopSnapshot) {
  const goNoGo = closedLoopSnapshot.go_no_go ?? {};
  requireRegex(finalAuditPath, /闭环当前态快照/, "closed-loop current state snapshot row");
  requireRegex(finalAuditPath, /可以进入联调：是/, "integration-ready conclusion from closed loop snapshot");
  requireRegex(finalAuditPath, /可以进入业务验收：是/, "business-acceptance-ready conclusion from closed loop snapshot");
  if (goNoGo.can_claim_production_ready === false) {
    requireRegex(finalAuditPath, /可以直接生产上线：否/, "production no-go conclusion from closed loop snapshot");
    if (/可以直接生产上线：是/.test(finalAudit)) {
      failures.push("final audit must not claim production launch while closed loop snapshot says production_ready=false");
    }
  }
  const productionEvidence = closedLoopSnapshot.gates?.production_evidence ?? {};
  requireRegex(finalAuditPath, new RegExp(`${escapeRegex(productionEvidence.awaiting_submission)}\\s*份\\s*manifest|${escapeRegex(productionEvidence.awaiting_submission)}\\s*production evidence`, "i"), `closed loop snapshot awaiting production evidence ${productionEvidence.awaiting_submission}`);
  if (closedLoopSnapshot.gates?.nas_current_state?.can_close_sec_p0_ops_recovery === false) {
    requireRegex(finalAuditPath, /SEC-P0-OPS-RECOVERY-001/, "closed loop snapshot SEC-P0-OPS-RECOVERY blocker");
  }
  const ownerLinks = closedLoopSnapshot.gates?.owner_action_manifest_links ?? {};
  if (ownerLinks.items_without_manifest !== undefined) {
    requireRegex(finalAuditPath, new RegExp(`items_without_manifest=${escapeRegex(ownerLinks.items_without_manifest)}`), `closed loop owner action manifest links ${ownerLinks.items_without_manifest}`);
    if (ownerLinks.items_without_manifest !== 0) failures.push("closed loop snapshot has owner actions without manifest links");
  }
}

requireIncludes("docs/00-current-effective-document-entry-v0.1.md", [finalAuditPath]);
requireIncludes("docs/delivery-package-manifest-v0.1.md", [finalAuditPath.replace("docs/", "")]);

if (failures.length > 0) {
  console.error("FINAL_CLOSED_LOOP_AUDIT_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("FINAL_CLOSED_LOOP_AUDIT_PASS");
