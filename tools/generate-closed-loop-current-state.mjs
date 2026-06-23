import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const markdownPath = "docs/closed-loop-current-state-snapshot-v0.1.md";
const jsonPath = "docs/closed-loop-current-state-snapshot-latest.json";
const checkMode = process.argv.includes("--check") || process.argv.includes("--verify") || process.env.CLOSED_LOOP_CURRENT_STATE_CHECK === "1";

function readText(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), "utf8").replace(/^\uFEFF/, "");
}

function readJson(relativePath) {
  return JSON.parse(readText(relativePath));
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
    if (char === ",") {
      row.push(value);
      value = "";
      continue;
    }
    if (char === "\n") {
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
  return body
    .filter((cells) => cells.some((cell) => String(cell).trim() !== ""))
    .map((cells) => Object.fromEntries(headers.map((header, index) => [header.replace(/^\uFEFF/, ""), cells[index] ?? ""])));
}

function csvRows(relativePath) {
  return parseCsv(readText(relativePath));
}

function lineCount(relativePath) {
  return readText(relativePath)
    .split(/\r?\n/)
    .filter((_, index, lines) => index < lines.length - 1 || lines[index] !== "")
    .length;
}

function countBy(rows, predicate) {
  return rows.filter(predicate).length;
}

function escapeMarkdownCell(value) {
  return String(value ?? "").replace(/\|/g, "/").replace(/\r?\n/g, "<br>");
}

function mdTable(rows, columns) {
  if (!Array.isArray(rows) || rows.length === 0) return "\u65e0\u3002\n";
  const header = `| ${columns.map((column) => column.label).join(" | ")} |`;
  const separator = `| ${columns.map(() => "---").join(" | ")} |`;
  const body = rows.map((row) => `| ${columns.map((column) => escapeMarkdownCell(row[column.key])).join(" | ")} |`);
  return [header, separator, ...body].join("\n") + "\n";
}


function matrixState() {
  const requirementRows = csvRows("docs/requirements-code-implementation-matrix-v0.1.csv");
  const mvpRows = csvRows("docs/full-admin-mvp-requirements-code-matrix-v0.1.csv");
  const remainingRows = csvRows("docs/full-admin-remaining-requirements-code-matrix-v0.1.csv");
  const externalGaps = requirementRows
    .filter((row) => row["当前实现状态"] !== "已完整实现")
    .map((row) => ({
      requirement_id: row["需求编号"] || row.requirement_id || "",
      module: row["业务模块"] || row.module || "",
      status: row["当前实现状态"] || "",
      gap: row["缺口"] || row["修复优先级"] || "",
    }));
  return {
    requirements: {
      path: "docs/requirements-code-implementation-matrix-v0.1.csv",
      total: requirementRows.length,
      complete: countBy(requirementRows, (row) => row["当前实现状态"] === "已完整实现"),
      gap: externalGaps.length,
      external_gaps: externalGaps,
    },
    full_admin_mvp: {
      path: "docs/full-admin-mvp-requirements-code-matrix-v0.1.csv",
      total: mvpRows.length,
      complete: countBy(mvpRows, (row) => row["当前实现状态"] === "已完整实现"),
      gap: countBy(mvpRows, (row) => row["当前实现状态"] !== "已完整实现"),
    },
    full_admin_remaining: {
      path: "docs/full-admin-remaining-requirements-code-matrix-v0.1.csv",
      total: remainingRows.length,
      complete: countBy(remainingRows, (row) => row.status === "已实现"),
      gap: countBy(remainingRows, (row) => row.status !== "已实现"),
    },
  };
}

function currentState() {
  const semgrep = readJson("security-scans/semgrep-latest-summary.json");
  const submissionStatus = readJson("docs/production-readiness-evidence/production-evidence-submission-status-latest.json");
  const productionBlockerRegister = readJson("docs/production-readiness-evidence/production-blocker-register-latest.json");
  const nasDrift = readJson("docs/production-readiness-evidence/nas-current-state-drift-latest.json");
  const nasDiagnostic = readJson("docs/production-readiness-evidence/nas-diagnostic-latest.json");
  const runtimeEvidence = readJson("docs/runtime-acceptance-evidence-latest.json");
  const partialMockCoverage = readJson("docs/partial-requirement-mock-coverage-latest.json");
  const productionOwnerActions = readJson("docs/production-readiness-evidence/production-owner-action-packs-latest.json");
  const matrices = matrixState();
  const productionCounts = submissionStatus.counts ?? {};
  const blockerCounts = productionBlockerRegister.counts ?? {};
  const productionBlockingTotal =
    Number(blockerCounts.readiness_blockers ?? 0) +
    Number(blockerCounts.credential_blockers ?? 0) +
    Number(blockerCounts.finding_blockers ?? 0);
  const p0Expected =
    (productionCounts.p0_awaiting_submission ?? 0) +
    (productionCounts.p0_valid_submission ?? 0) +
    (productionCounts.p0_invalid_submission ?? 0);
  const semgrepResults = Number(semgrep.result_count ?? semgrep.results ?? semgrep.results_count ?? semgrep.findings ?? 0);
  const semgrepErrors = Array.isArray(semgrep.errors) ? semgrep.errors.length : Number(semgrep.error_count ?? semgrep.errors ?? semgrep.errors_count ?? 0);
  const semgrepJsonExitCode = Number(semgrep.json_exitcode ?? semgrep.json_exit_code ?? semgrep.semgrep_exit_code ?? semgrep.exit_code ?? 0);
  const semgrepSarifExitCode = Number(semgrep.sarif_exitcode ?? semgrep.sarif_exit_code ?? 0);
  const semgrepPathsScanned = Array.isArray(semgrep.paths_scanned) ? semgrep.paths_scanned.length : Number(semgrep.scanned_path_count ?? semgrep.paths_scanned ?? semgrep.paths_scanned_count ?? semgrep.scanned_count ?? 0);
  const semgrepScanRef = semgrep.scan_ref ?? semgrep.scan_dir ?? semgrep.scan_id ?? semgrep.scanDir ?? semgrep.raw_result_file ?? semgrep.latest_file ?? "unknown";
  const semgrepClean = semgrepResults === 0 && semgrepErrors === 0 && semgrepJsonExitCode === 0 && semgrepSarifExitCode === 0;
  const adminComplete = matrices.full_admin_mvp.gap === 0 && matrices.full_admin_remaining.gap === 0;
  const productionEvidenceComplete = (productionCounts.awaiting_submission ?? 0) === 0 && (productionCounts.invalid_submission ?? 0) === 0;
  const nasProductionReady = nasDrift.production_closure_assessment?.can_close_sec_p0_ops_recovery === true;
  const externalEvidenceComplete = matrices.requirements.external_gaps.length === 0;
  const productionBlockersClear = productionBlockingTotal === 0;

  const p0AwaitingSubmissions = (submissionStatus.submissions ?? [])
    .filter((item) => item.priority === "P0" && item.status === "awaiting_submission")
    .map((item) => ({
      item_id: item.item_id,
      owner_group: item.owner_group,
      category: item.category,
      manifest_path: item.manifest_path,
    }));
  const readinessBlockers = (productionBlockerRegister.readiness_blockers ?? []).map((item) => ({
    id: item.id,
    owner: item.owner_department,
    status: item.status,
    blocking_scope: item.blocking_scope,
    next_action: item.next_action,
  }));
  const credentialBlockers = (productionBlockerRegister.credential_blockers ?? []).map((item) => ({
    id: item.id,
    owner: item.owner_department,
    status: item.status,
    blocking_scope: item.blocking_scope,
    next_action: item.next_action,
  }));
  const securityFindingBlockers = (productionBlockerRegister.finding_blockers ?? []).map((item) => ({
    id: item.id,
    severity: item.severity,
    owner: item.owner,
    status: item.status,
    blocking_scope: item.blocking_scope,
    next_action: item.next_action,
  }));
  return {
    markers: [
      "CLOSED_LOOP_CURRENT_STATE_SNAPSHOT_READY",
      "SNAPSHOT_FROM_REQUIREMENTS_CODE_MATRIX",
      "SNAPSHOT_FROM_SEMGREP_LATEST_SUMMARY",
      "SNAPSHOT_FROM_PRODUCTION_EVIDENCE_STATUS",
      "SNAPSHOT_FROM_NAS_CURRENT_STATE_DRIFT",
      "SNAPSHOT_FROM_NAS_DIAGNOSTIC_REPORT",
      "SNAPSHOT_FROM_RUNTIME_ACCEPTANCE_EVIDENCE",
      "SNAPSHOT_FROM_PARTIAL_REQUIREMENT_MOCK_COVERAGE",
      "SNAPSHOT_FROM_PRODUCTION_OWNER_ACTION_MANIFEST_LINKS",
      "OWNER_ACTIONS_LINKED_TO_EVIDENCE_MANIFESTS",
      "NO_PRODUCTION_READY_CLAIM_WITH_OPEN_MANIFESTS",
    ],
    matrices,
    architecture_line_counts: {
      "apps/web/src/main.tsx": lineCount("apps/web/src/main.tsx"),
      "backend/src/main/java/com/complex/platform/ticketing/TicketingService.java": lineCount("backend/src/main/java/com/complex/platform/ticketing/TicketingService.java"),
      "backend/src/main/java/com/complex/platform/bar/BarService.java": lineCount("backend/src/main/java/com/complex/platform/bar/BarService.java"),
      "backend/src/main/java/com/complex/platform/bar/BarOrderService.java": lineCount("backend/src/main/java/com/complex/platform/bar/BarOrderService.java"),
      "backend/src/main/java/com/complex/platform/bar/BarBillingService.java": lineCount("backend/src/main/java/com/complex/platform/bar/BarBillingService.java"),
      "backend/src/main/java/com/complex/platform/bar/BarTableSessionService.java": lineCount("backend/src/main/java/com/complex/platform/bar/BarTableSessionService.java"),
    },
    gates: {
      semgrep: {
        scan_dir: semgrepScanRef,
        results: semgrepResults,
        errors: semgrepErrors,
        paths_scanned: semgrepPathsScanned,
        clean: semgrepClean,
      },
      production_evidence: {
        expected_submissions: productionCounts.expected_submissions ?? 0,
        awaiting_submission: productionCounts.awaiting_submission ?? 0,
        valid_submission: productionCounts.valid_submission ?? 0,
        invalid_submission: productionCounts.invalid_submission ?? 0,
        p0_expected: p0Expected,
        p0_awaiting_submission: productionCounts.p0_awaiting_submission ?? 0,
        p0_valid_submission: productionCounts.p0_valid_submission ?? 0,
        p0_invalid_submission: productionCounts.p0_invalid_submission ?? 0,
        complete: productionEvidenceComplete,
      },
      production_blockers: {
        readiness_blockers: Number(blockerCounts.readiness_blockers ?? 0),
        credential_blockers: Number(blockerCounts.credential_blockers ?? 0),
        finding_blockers: Number(blockerCounts.finding_blockers ?? 0),
        blocking_total: productionBlockingTotal,
        complete: productionBlockersClear,
      },
      nas_current_state: {
        live_status: nasDrift.current_state?.live_status ?? "unknown",
        can_close_sec_p0_ops_recovery: nasDrift.production_closure_assessment?.can_close_sec_p0_ops_recovery === true,
        reason: nasDrift.production_closure_assessment?.reason ?? "",
      },
      runtime_acceptance: {
        backend_tests: runtimeEvidence.backend_surefire?.tests ?? 0,
        backend_failures: runtimeEvidence.backend_surefire?.failures ?? 0,
        backend_errors: runtimeEvidence.backend_surefire?.errors ?? 0,
        web_unit_tests: runtimeEvidence.web_unit_source?.test_count_from_source ?? 0,
        ui_tests: runtimeEvidence.playwright?.normal?.test_count_from_source ?? 0,
        real_backend_ui_tests: runtimeEvidence.playwright?.real_backend?.test_count_from_source ?? 0,
        last_run_passed: runtimeEvidence.playwright?.last_run_passed === true,
        web_main_bundle_ok: runtimeEvidence.web_build?.no_main_bundle_warning_expected === true,
        ready_for_integration_and_business_acceptance: runtimeEvidence.conclusion?.runtime_ready_for_integration_and_business_acceptance === true,
      },
      nas_diagnostic: {
        live_status: nasDiagnostic.diagnostic_summary?.live_status ?? "unknown",
        network_ping_reachable: nasDiagnostic.diagnostic_summary?.network_ping_reachable === true,
        smb_445_reachable: nasDiagnostic.diagnostic_summary?.smb_445_reachable === true,
        expected_cifs_mount_active: nasDiagnostic.diagnostic_summary?.expected_cifs_mount_active === true,
        preflight_passed: nasDiagnostic.diagnostic_summary?.preflight_passed === true,
        root_cause: nasDiagnostic.diagnostic_summary?.root_cause ?? "",
      },
      external_evidence: {
        partial_requirements: matrices.requirements.external_gaps.length,
        complete: externalEvidenceComplete,
      },
      partial_mock_coverage: {
        partial_requirements: partialMockCoverage.counts?.partial_requirements ?? 0,
        mock_acceptance_ready: partialMockCoverage.counts?.mock_acceptance_ready ?? 0,
        production_blocking_requirements: partialMockCoverage.counts?.production_blocking_requirements ?? 0,
        unique_mock_files: partialMockCoverage.counts?.unique_mock_files ?? 0,
        status: partialMockCoverage.status ?? "unknown",
      },
      owner_action_manifest_links: {
        unique_unresolved_items: productionOwnerActions.counts?.unique_unresolved_items ?? 0,
        owner_action_packs: productionOwnerActions.counts?.owner_action_packs ?? 0,
        duplicated_owner_action_rows: productionOwnerActions.counts?.duplicated_owner_action_rows ?? 0,
        related_submission_rows: productionOwnerActions.counts?.related_submission_rows ?? 0,
        items_without_manifest: productionOwnerActions.counts?.items_without_manifest ?? 0,
        complete: (productionOwnerActions.counts?.items_without_manifest ?? 0) === 0,
        status: productionOwnerActions.status ?? "unknown",
      },
    },
    production_ready_next_actions: {
      p0_awaiting_submission_total: p0AwaitingSubmissions.length,
      p0_awaiting_submission_sample: p0AwaitingSubmissions.slice(0, 12),
      readiness_blockers: readinessBlockers,
      credential_blockers: credentialBlockers,
      security_finding_blockers: securityFindingBlockers,
      external_partial_requirements: matrices.requirements.external_gaps,
      nas_next_action: nasDiagnostic.diagnostic_summary?.next_action ?? "",
      nas_root_cause: nasDiagnostic.diagnostic_summary?.root_cause ?? "",
      source_documents: [
        "docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.json",
        "docs/production-readiness-evidence/production-blocker-register-latest.json",
        "docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.json",
        "docs/partial-requirement-mock-coverage-latest.json",
        "docs/production-readiness-evidence/production-owner-action-packs-latest.json",
        "docs/production-readiness-evidence/nas-current-state-drift-latest.json",
      ],
    },
    go_no_go: {
      // 联调/业务验收看代码、UI、契约和安全扫描是否自洽；生产外部证据只阻断“上线就绪”声明。
      can_enter_integration: semgrepClean && adminComplete && runtimeEvidence.conclusion?.runtime_ready_for_integration_and_business_acceptance === true,
      can_enter_business_acceptance: semgrepClean && adminComplete && matrices.requirements.complete > 0 && runtimeEvidence.conclusion?.runtime_ready_for_integration_and_business_acceptance === true,
      can_claim_production_ready: semgrepClean && adminComplete && productionEvidenceComplete && nasProductionReady && externalEvidenceComplete && productionBlockersClear,
      production_ready_blockers: [
        ...((productionCounts.awaiting_submission ?? 0) > 0
          ? [`${productionCounts.awaiting_submission} production evidence submissions awaiting, including ${productionCounts.p0_awaiting_submission ?? 0} P0`]
          : []),
        ...(nasProductionReady ? [] : [`NAS current state is ${nasDrift.current_state?.live_status ?? "unknown"}; SEC-P0-OPS-RECOVERY-001 cannot close`]),
        ...(productionBlockersClear ? [] : [`${productionBlockingTotal} production blocker register items remain open (${Number(blockerCounts.readiness_blockers ?? 0)} readiness, ${Number(blockerCounts.credential_blockers ?? 0)} credential, ${Number(blockerCounts.finding_blockers ?? 0)} security findings)`]),
        ...(matrices.requirements.external_gaps.length > 0
          ? [`${matrices.requirements.external_gaps.length} external evidence or supplier-backed requirements remain partial`]
          : []),
      ],
    },
  };
}

function renderMarkdown(state) {
  const matrixRows = [
    [state.matrices.requirements.path, state.matrices.requirements.total, state.matrices.requirements.complete, state.matrices.requirements.gap],
    [state.matrices.full_admin_mvp.path, state.matrices.full_admin_mvp.total, state.matrices.full_admin_mvp.complete, state.matrices.full_admin_mvp.gap],
    [state.matrices.full_admin_remaining.path, state.matrices.full_admin_remaining.total, state.matrices.full_admin_remaining.complete, state.matrices.full_admin_remaining.gap],
  ];
  const lineRows = Object.entries(state.architecture_line_counts);
  const gapRows = state.matrices.requirements.external_gaps;
  return `# 闭环当前态快照 V0.1

> 本文由 \`tools/generate-closed-loop-current-state.mjs\` 从需求矩阵、代码行数、安全扫描、生产证据提交状态和 NAS 当前漂移证据生成。它用于证明“当前可联调/可业务验收，但不可宣称生产上线就绪”的边界，不替代真实测试命令输出。

机器识别标记：

${state.markers.map((marker) => `- ${marker}`).join("\n")}

## 1. Go / No-Go

| 判断项 | 当前值 |
|---|---|
| 可以进入联调 | ${state.go_no_go.can_enter_integration ? "是" : "否"} |
| 可以进入业务验收 | ${state.go_no_go.can_enter_business_acceptance ? "是" : "否"} |
| 可以宣称生产上线就绪 | ${state.go_no_go.can_claim_production_ready ? "是" : "否"} |

## 2. 需求矩阵当前态

| 矩阵 | 总数 | 完整/已实现 | 缺口 |
|---|---:|---:|---:|
${matrixRows.map(([file, total, complete, gap]) => `| \`${file}\` | ${total} | ${complete} | ${gap} |`).join("\n")}

## 3. 架构瘦身行数快照

| 文件 | 当前行数 |
|---|---:|
${lineRows.map(([file, count]) => `| \`${file}\` | ${count} |`).join("\n")}

## 4. 安全与生产证据闸口

| 闸口 | 当前态 |
|---|---|
| Semgrep | results=${state.gates.semgrep.results}，errors=${state.gates.semgrep.errors}，paths_scanned=${state.gates.semgrep.paths_scanned}，scan_dir=\`${state.gates.semgrep.scan_dir}\` |
| 运行时验收证据 | backend=${state.gates.runtime_acceptance.backend_tests} tests / failures=${state.gates.runtime_acceptance.backend_failures} / errors=${state.gates.runtime_acceptance.backend_errors}，web_unit=${state.gates.runtime_acceptance.web_unit_tests}，ui=${state.gates.runtime_acceptance.ui_tests}，real_backend_ui=${state.gates.runtime_acceptance.real_backend_ui_tests}，ready=${state.gates.runtime_acceptance.ready_for_integration_and_business_acceptance ? "true" : "false"} |
| 生产证据提交 | expected=${state.gates.production_evidence.expected_submissions}，awaiting=${state.gates.production_evidence.awaiting_submission}，valid=${state.gates.production_evidence.valid_submission}，invalid=${state.gates.production_evidence.invalid_submission} |
| Production blocker register | readiness=${state.gates.production_blockers.readiness_blockers}, credential=${state.gates.production_blockers.credential_blockers}, findings=${state.gates.production_blockers.finding_blockers}, blocking_total=${state.gates.production_blockers.blocking_total}, complete=${state.gates.production_blockers.complete ? "true" : "false"} |
| P0 生产证据 | expected=${state.gates.production_evidence.p0_expected}，awaiting=${state.gates.production_evidence.p0_awaiting_submission}，valid=${state.gates.production_evidence.p0_valid_submission}，invalid=${state.gates.production_evidence.p0_invalid_submission} |
| NAS 当前状态 | live_status=${state.gates.nas_current_state.live_status}，can_close_sec_p0_ops_recovery=${state.gates.nas_current_state.can_close_sec_p0_ops_recovery ? "true" : "false"} |
| NAS 只读诊断 | ping=${state.gates.nas_diagnostic.network_ping_reachable ? "reachable" : "failed"}，tcp445=${state.gates.nas_diagnostic.smb_445_reachable ? "reachable" : "failed"}，cifs_mount=${state.gates.nas_diagnostic.expected_cifs_mount_active ? "active" : "inactive"}，preflight=${state.gates.nas_diagnostic.preflight_passed ? "passed" : "failed"} |
| NAS 诊断根因 | ${state.gates.nas_diagnostic.root_cause || "unknown"} |
| 外部证据 | partial=${state.gates.external_evidence.partial_requirements}，complete=${state.gates.external_evidence.complete ? "true" : "false"} |
| Owner action manifest links | blockers=${state.gates.owner_action_manifest_links.unique_unresolved_items}, actions=${state.gates.owner_action_manifest_links.duplicated_owner_action_rows}, related_submissions=${state.gates.owner_action_manifest_links.related_submission_rows}, items_without_manifest=${state.gates.owner_action_manifest_links.items_without_manifest}, complete=${state.gates.owner_action_manifest_links.complete ? "true" : "false"} |

## 5. 不可宣称生产上线就绪的原因

${state.go_no_go.production_ready_blockers.map((item) => `- ${item}`).join("\n") || "- 当前未发现生产上线阻断。"}


## 6. 下一步可执行阻断明细

这一节只汇总“现在该找谁、补什么”，不把外部证据伪装成已完成。完整清单以 \`production-evidence-backfill-execution-ledger-latest.json\`、\`production-blocker-register-latest.json\` 和 \`external-evidence-requirement-traceability-latest.json\` 为准。

### 6.1 P0 生产证据待回填（前 12 项）

待回填总数：${state.production_ready_next_actions.p0_awaiting_submission_total}。

${mdTable(state.production_ready_next_actions.p0_awaiting_submission_sample, [
  { key: "item_id", label: "item_id" },
  { key: "owner_group", label: "owner" },
  { key: "category", label: "category" },
  { key: "manifest_path", label: "manifest_path" },
])}
### 6.2 生产前置台账阻断项

${mdTable(state.production_ready_next_actions.readiness_blockers, [
  { key: "id", label: "item_id" },
  { key: "owner", label: "owner" },
  { key: "status", label: "status" },
  { key: "blocking_scope", label: "blocking_scope" },
  { key: "next_action", label: "next_action" },
])}
### 6.3 生产凭证轮换阻断项

${mdTable(state.production_ready_next_actions.credential_blockers, [
  { key: "id", label: "credential_item_id" },
  { key: "owner", label: "owner" },
  { key: "status", label: "status" },
  { key: "blocking_scope", label: "blocking_scope" },
  { key: "next_action", label: "next_action" },
])}
### 6.4 安全 findings 阻断项

${mdTable(state.production_ready_next_actions.security_finding_blockers, [
  { key: "id", label: "finding_id" },
  { key: "severity", label: "severity" },
  { key: "owner", label: "owner" },
  { key: "status", label: "status" },
  { key: "blocking_scope", label: "blocking_scope" },
  { key: "next_action", label: "next_action" },
])}
### 6.5 NAS 当前态下一步

- root_cause：${state.production_ready_next_actions.nas_root_cause || "unknown"}
- next_action：${state.production_ready_next_actions.nas_next_action || "unknown"}

## 7. 仍需外部证据或供应商资料的需求

| 需求编号 | 模块 | 状态 | 缺口 |
|---|---|---|---|
${gapRows.map((row) => `| ${row.requirement_id || "-"} | ${row.module || "-"} | ${row.status || "-"} | ${(row.gap || "-").replace(/\|/g, "/")} |`).join("\n")}
`;
}

const state = currentState();
const nextJson = `${JSON.stringify(state, null, 2)}\n`;
const nextMarkdown = renderMarkdown(state);

if (!state.go_no_go.can_claim_production_ready && nextMarkdown.includes("| 可以宣称生产上线就绪 | 是 |")) {
  throw new Error("snapshot must not claim production ready while blockers remain");
}

if (state.gates.owner_action_manifest_links.items_without_manifest !== 0) {
  throw new Error("owner action manifest links must cover every action item");
}

if (checkMode) {
  const currentJson = fs.existsSync(jsonPath) ? readText(jsonPath) : "";
  const currentMarkdown = fs.existsSync(markdownPath) ? readText(markdownPath) : "";
  const failures = [];
  if (currentJson !== nextJson) failures.push(`${jsonPath} is out of date`);
  if (currentMarkdown !== nextMarkdown) failures.push(`${markdownPath} is out of date`);
  if (failures.length > 0) {
    console.error("CLOSED_LOOP_CURRENT_STATE_FAIL");
    for (const failure of failures) console.error(`- ${failure}`);
    process.exit(1);
  }
  console.log("CLOSED_LOOP_CURRENT_STATE_PASS");
  process.exit(0);
}

fs.writeFileSync(path.join(repoRoot, jsonPath), nextJson, "utf8");
fs.writeFileSync(path.join(repoRoot, markdownPath), nextMarkdown, "utf8");
console.log(`wrote ${markdownPath}`);
console.log(`wrote ${jsonPath}`);
