import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const args = process.argv.slice(2);
const checkMode = args.includes("--check") || args.includes("--verify");
const mdPath = "docs/production-readiness-evidence/production-no-go-resolution-command-center-v0.1.md";
const jsonPath = "docs/production-readiness-evidence/production-no-go-resolution-command-center-latest.json";
const csvPath = "docs/production-readiness-evidence/production-no-go-resolution-command-center-latest.csv";

function absolute(relativePath) {
  return path.join(repoRoot, relativePath);
}

function stripBom(text) {
  return text.replace(/^\uFEFF/, "");
}

function readJson(relativePath) {
  return JSON.parse(stripBom(fs.readFileSync(absolute(relativePath), "utf8")));
}

function normalize(text) {
  return text.replace(/\r\n/g, "\n").trimEnd() + "\n";
}

function csvEscape(value) {
  const text = String(value ?? "");
  if (!/[",\n]/.test(text)) return text;
  return `"${text.replace(/"/g, '""')}"`;
}

function mdEscape(value) {
  return String(value ?? "").replace(/\|/g, "/").replace(/\r?\n/g, "<br>");
}

function mdTable(rows, columns) {
  if (!rows.length) return "无。\n";
  const header = `| ${columns.map((column) => column.label).join(" | ")} |`;
  const separator = `| ${columns.map(() => "---").join(" | ")} |`;
  const body = rows.map((row) => `| ${columns.map((column) => mdEscape(row[column.key])).join(" | ")} |`);
  return [header, separator, ...body].join("\n") + "\n";
}

function writeIfChanged(relativePath, content) {
  const fullPath = absolute(relativePath);
  fs.mkdirSync(path.dirname(fullPath), { recursive: true });
  const normalized = normalize(content);
  if (checkMode) {
    if (!fs.existsSync(fullPath)) throw new Error(`${relativePath} is missing`);
    const current = normalize(fs.readFileSync(fullPath, "utf8"));
    if (current !== normalized) throw new Error(`${relativePath} is not in sync with production no-go sources`);
    return;
  }
  fs.writeFileSync(fullPath, normalized, "utf8");
  console.log(`wrote ${relativePath}`);
}

function blockerRows(blockers) {
  const readiness = (blockers.readiness_blockers ?? []).map((item) => ({
    type: "readiness",
    severity: "P0/P1 by item priority",
    id: item.id,
    owner: item.owner_department,
    category: item.category,
    status: item.status,
    blocking_scope: item.blocking_scope,
    next_action: item.next_action,
    runbook_ref: item.runbook_ref,
  }));
  const credential = (blockers.credential_blockers ?? []).map((item) => ({
    type: "credential",
    severity: "P0",
    id: item.id,
    owner: item.owner_department,
    category: item.category,
    status: item.status,
    blocking_scope: item.blocking_scope,
    next_action: item.next_action,
    runbook_ref: item.runbook_ref,
  }));
  const findings = (blockers.finding_blockers ?? []).map((item) => ({
    type: "security_finding",
    severity: item.severity,
    id: item.id,
    owner: item.owner,
    category: "security",
    status: item.status,
    blocking_scope: item.blocking_scope,
    next_action: item.next_action,
    runbook_ref: item.source,
  }));
  return [...readiness, ...credential, ...findings];
}

function flattenOwnerRows(ownerActions) {
  const rows = [];
  for (const packet of ownerActions.owner_packets ?? []) {
    for (const row of packet.rows ?? []) {
      rows.push({
        owner_group: packet.owner_group,
        owner_slug: packet.owner_slug,
        priority: row.priority,
        item_id: row.item_id,
        category: row.category,
        status: row.current_status ?? row.status ?? "awaiting_submission",
        blocking_scope: row.blocking_scope,
        manifest_path: row.target_manifest_path,
        template_path: row.template_path,
        accepted_evidence_types: (row.accepted_evidence_types ?? []).join("; "),
        required_evidence_summary: (row.required_evidence_summary ?? []).join("; "),
        next_step: row.operator_next_step ?? "Copy template, replace placeholders, provide external evidence links, reviewer, test time and cred:// refs.",
        verification_commands: (row.verification_commands ?? []).join("; "),
      });
    }
  }
  const priorityRank = { P0: 0, P1: 1, P2: 2, P3: 3 };
  const categoryRank = new Map(["nas", "ops160", "credential", "MDM", "payment", "face_age", "third_party", "wecom", "security"].map((item, index) => [item, index]));
  return rows.sort((a, b) =>
    (priorityRank[a.priority] ?? 99) - (priorityRank[b.priority] ?? 99)
    || (categoryRank.get(a.category) ?? 99) - (categoryRank.get(b.category) ?? 99)
    || String(a.owner_slug).localeCompare(String(b.owner_slug))
    || String(a.item_id).localeCompare(String(b.item_id)),
  );
}

function partialRows(externalTraceability) {
  return (externalTraceability.requirement_traceability ?? []).map((item) => ({
    requirement_id: item.requirement_id,
    module: item.module,
    current_status: item.current_status,
    mapped_item_ids: (item.mapped_item_ids ?? []).join("; "),
    owner_groups: (item.owner_groups ?? []).join("; "),
    awaiting_evidence_rows: item.awaiting_evidence_rows,
    production_blocking: item.production_blocking ? "yes" : "no",
    target_manifest_count: (item.target_manifest_paths ?? []).length,
    next_step: "Submit valid production manifests for the mapped item_ids; mock/sandbox evidence cannot close this requirement.",
  }));
}

function buildState() {
  const submissionStatus = readJson("docs/production-readiness-evidence/production-evidence-submission-status-latest.json");
  const blockers = readJson("docs/production-readiness-evidence/production-blocker-register-latest.json");
  const ownerActions = readJson("docs/production-readiness-evidence/production-owner-action-packs-latest.json");
  const externalTraceability = readJson("docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.json");
  const nasDrift = readJson("docs/production-readiness-evidence/nas-current-state-drift-latest.json");
  const nasDiagnostic = readJson("docs/production-readiness-evidence/nas-diagnostic-latest.json");
  const closedLoop = readJson("docs/closed-loop-current-state-snapshot-latest.json");
  const actionRows = flattenOwnerRows(ownerActions);
  const blockersFlat = blockerRows(blockers);
  const partials = partialRows(externalTraceability);
  return {
    markers: [
      "PRODUCTION_NO_GO_RESOLUTION_COMMAND_CENTER_READY",
      "NO_FAKE_PRODUCTION_EVIDENCE_CLOSURE",
      "CLOSURE_REQUIRES_VALID_SUBMISSION_MANIFESTS",
      "NAS_REQUIRES_IT_SUDO_MAINTENANCE_WINDOW",
      "OWNER_ACTIONS_LINKED_TO_EVIDENCE_MANIFESTS",
      "EXTERNAL_PARTIALS_MAPPED_TO_MANIFESTS",
      "PRODUCTION_READY_REMAINS_NO_GO_UNTIL_EXTERNAL_EVIDENCE_VALID",
    ],
    generated_from: [
      "production-evidence-submission-status-latest.json",
      "production-blocker-register-latest.json",
      "production-owner-action-packs-latest.json",
      "external-evidence-requirement-traceability-latest.json",
      "nas-current-state-drift-latest.json",
      "nas-diagnostic-latest.json",
      "closed-loop-current-state-snapshot-latest.json",
    ],
    counts: {
      expected_submissions: submissionStatus.counts?.expected_submissions ?? 0,
      awaiting_submission: submissionStatus.counts?.awaiting_submission ?? 0,
      valid_submission: submissionStatus.counts?.valid_submission ?? 0,
      p0_awaiting_submission: submissionStatus.counts?.p0_awaiting_submission ?? 0,
      readiness_blockers: blockers.counts?.readiness_blockers ?? 0,
      credential_blockers: blockers.counts?.credential_blockers ?? 0,
      finding_blockers: blockers.counts?.finding_blockers ?? 0,
      blocker_total: blockersFlat.length,
      partial_requirements: externalTraceability.counts?.partial_requirements ?? partials.length,
      owner_actions_without_manifest: ownerActions.counts?.items_without_manifest ?? 0,
    },
    go_no_go: closedLoop.go_no_go,
    nas: {
      live_status: nasDrift.current_state?.live_status ?? nasDiagnostic.diagnostic_summary?.live_status ?? "unknown",
      mount_path: nasDrift.expected?.mount_path ?? nasDiagnostic.expected?.mount_path,
      nas_unc: nasDrift.expected?.nas_unc ?? nasDiagnostic.expected?.nas_unc,
      credential_file: nasDrift.expected?.credential_file ?? nasDiagnostic.expected?.credential_file,
      credential_ref: nasDrift.expected?.credential_ref ?? nasDiagnostic.expected?.credential_ref,
      sudo_no_password: nasDiagnostic.environment?.sudo_no_password?.available === true,
      root_cause: nasDiagnostic.diagnostic_summary?.root_cause,
      next_action: nasDiagnostic.diagnostic_summary?.next_action,
      can_close_sec_p0_ops_recovery: nasDrift.production_closure_assessment?.can_close_sec_p0_ops_recovery === true,
    },
    p0_first_owner_actions: actionRows,
    blockers: blockersFlat,
    partial_requirements: partials,
    verification_after_submission: [
      "npm run verify:production-evidence-submissions-guard",
      "npm run docs:production-evidence-submission-status",
      "npm run verify:production-evidence-submission-status",
      "npm run docs:production-evidence-backfill-priority",
      "npm run verify:production-evidence-backfill-priority",
      "npm run docs:production-closure-gap-reports",
      "npm run verify:production-closure-gap-reports",
      "npm run verify:closed-loop-current-state",
      "npm run verify:delivery-gates",
    ],
  };
}

function buildCsv(rows) {
  const columns = [
    "owner_group", "priority", "item_id", "category", "status", "manifest_path", "template_path", "accepted_evidence_types", "required_evidence_summary", "next_step", "verification_commands",
  ];
  return `${columns.join(",")}\n${rows.map((row) => columns.map((column) => csvEscape(row[column])).join(",")).join("\n")}\n`;
}

function buildMarkdown(state) {
  const p0Rows = state.p0_first_owner_actions.filter((row) => row.priority === "P0");
  const nonP0Rows = state.p0_first_owner_actions.filter((row) => row.priority !== "P0");
  const markersMarkdown = state.markers.map((marker) => `- ${marker}`).join("\n");
  const verificationMarkdown = state.verification_after_submission.map((command) => `- \`${command}\``).join("\n");
  return `# 生产 No-Go 关闭指挥板 V0.1

${markersMarkdown}

> 本文是生产上线 no-go 的执行指挥板。它只把阻塞项、责任人、manifest 路径和验证命令合并成一张可执行清单；不得用它替代真实生产 evidence。raw evidence 必须留在工单、审批、供应商平台、NAS/对象存储或安全库中，仓库只提交 manifest，密钥只写 \`cred://prod/...\` 引用。

## 1. 当前结论

| 项目 | 当前值 |
| --- | --- |
| 可进入联调 | ${state.go_no_go?.can_enter_integration ? "是" : "否"} |
| 可进入业务验收 | ${state.go_no_go?.can_enter_business_acceptance ? "是" : "否"} |
| 可宣称生产上线就绪 | ${state.go_no_go?.can_claim_production_ready ? "是" : "否"} |
| 待提交 production evidence | ${state.counts.awaiting_submission}/${state.counts.expected_submissions} |
| P0 待提交 production evidence | ${state.counts.p0_awaiting_submission} |
| open blocker | ${state.counts.blocker_total}（readiness=${state.counts.readiness_blockers}，credential=${state.counts.credential_blockers}，security=${state.counts.finding_blockers}） |
| partial 外部/供应商需求 | ${state.counts.partial_requirements} |
| owner action 无 manifest 链接 | ${state.counts.owner_actions_without_manifest} |
| NAS live status | ${state.nas.live_status} |

生产上线仍为 no-go，直到以下条件同时满足：40 份 manifest 均为 valid_submission、NAS 当前态通过、23 个 blocker 归零、8 条 partial 需求的真实外部证据完成回填。

## 2. NAS 先决动作

口径说明：\`//192.168.88.113/bn\` 是 NAS SMB 真实地址；\`/mnt/complex-nas-backup\` 只是 160 上用于访问该 SMB 共享的本地挂载点，不是 NAS 地址。

当前 NAS 根因：${state.nas.root_cause ?? "未记录"}

| 字段 | 值 |
| --- | --- |
| NAS SMB 地址（真实地址） | \`${state.nas.nas_unc}\` |
| 160 本地挂载点（不是 NAS 地址） | \`${state.nas.mount_path}\` |
| credential file | \`${state.nas.credential_file}\` |
| credential_ref | \`${state.nas.credential_ref}\` |
| 当前用户免密 sudo | ${state.nas.sudo_no_password ? "是" : "否"} |
| SEC-P0-OPS-RECOVERY-001 可关闭 | ${state.nas.can_close_sec_p0_ops_recovery ? "是" : "否"} |

IT/运维在 160 的受控维护窗口执行以下命令；Codex 不应在无审批、无 sudo、无 IT 责任人的情况下修改系统挂载：

\`\`\`bash
cd /home/james/complex-management
sudo env EXECUTE_MOUNT=true \
  NAS_UNC='${state.nas.nas_unc}' \
  MOUNT_PATH='${state.nas.mount_path}' \
  NAS_SMB_CREDENTIALS_FILE='${state.nas.credential_file}' \
  MOUNT_USER='james' \
  MOUNT_GROUP='james' \
  SMB_VERSION='3.0' \
  EVIDENCE_DIR='/tmp/complex-nas-mount-evidence' \
  bash ops/160/complex-nas-system-mount.example.sh

bash ops/160/complex-nas-preflight.sh
node tools/generate-nas-diagnostic-report.mjs
node tools/generate-nas-current-state-drift-report.mjs
npm run verify:nas-diagnostics
npm run verify:nas-current-state-drift
\`\`\`

IT 执行后，把 \`/tmp/complex-nas-mount-evidence/*.json\` 中的脱敏证据链接写入对应 NAS manifest，不要把 SMB 密码写入仓库。

## 3. P0 优先 manifest 清单

${mdTable(p0Rows, [
    { key: "owner_group", label: "责任组" },
    { key: "item_id", label: "item_id" },
    { key: "category", label: "类别" },
    { key: "status", label: "状态" },
    { key: "manifest_path", label: "目标 manifest" },
    { key: "accepted_evidence_types", label: "接受证据类型" },
  ])}

## 4. P1/P2 后续 manifest 清单

${mdTable(nonP0Rows, [
    { key: "owner_group", label: "责任组" },
    { key: "priority", label: "优先级" },
    { key: "item_id", label: "item_id" },
    { key: "category", label: "类别" },
    { key: "status", label: "状态" },
    { key: "manifest_path", label: "目标 manifest" },
  ])}

## 5. 23 个 open blocker

${mdTable(state.blockers, [
    { key: "type", label: "类型" },
    { key: "severity", label: "级别" },
    { key: "id", label: "id" },
    { key: "owner", label: "责任方" },
    { key: "status", label: "状态" },
    { key: "next_action", label: "下一步" },
  ])}

## 6. 8 条 partial 外部/供应商需求

${mdTable(state.partial_requirements, [
    { key: "requirement_id", label: "需求" },
    { key: "module", label: "模块" },
    { key: "mapped_item_ids", label: "证据 item" },
    { key: "owner_groups", label: "责任组" },
    { key: "awaiting_evidence_rows", label: "待证据行" },
    { key: "target_manifest_count", label: "manifest 数" },
    { key: "next_step", label: "关闭动作" },
  ])}

## 7. 提交后统一验证

${verificationMarkdown}

## 8. 禁止事项

- 不得把 mock、沙箱截图、口头确认、占位文件、明文密钥、未归属截图当作生产 evidence。
- 不得把历史 NAS passed evidence 当作当前自动挂载或生产恢复证据。
- 不得在 manifest 中写入密码、token、secret、private key；只能写 \`cred://prod/...\`。
- 不得跳过失败场景、复核人、审批记录、下次复测日期。
`;
}

const state = buildState();
const csv = buildCsv(state.p0_first_owner_actions);
const markdown = buildMarkdown(state);
const json = `${JSON.stringify(state, null, 2)}\n`;

try {
  writeIfChanged(mdPath, markdown);
  writeIfChanged(jsonPath, json);
  writeIfChanged(csvPath, csv);
  if (checkMode) console.log("PRODUCTION_NO_GO_RESOLUTION_COMMAND_CENTER_PASS");
} catch (error) {
  console.error("PRODUCTION_NO_GO_RESOLUTION_COMMAND_CENTER_FAIL");
  console.error(`- ${error.message}`);
  process.exit(1);
}