import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");
const statusPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
const priorityPath = "docs/production-readiness-evidence/production-evidence-backfill-priority-latest.json";
const ledgerPath = "docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.json";
const quickstartPath = "docs/production-readiness-evidence/production-evidence-p0-quickstart-latest.json";
const ownerGuidesPath = "docs/production-readiness-evidence/production-evidence-owner-guides-latest.json";
const jsonPath = "docs/production-readiness-evidence/production-evidence-owner-execution-packets-latest.json";
const mdPath = "docs/production-evidence-owner-execution-packets-v0.1.md";
const ownerPacketRoot = "docs/production-readiness-evidence/owner-execution-packets";

const ownerSlugByGroup = new Map([
  ["IT/运维/安全", "it-ops-security"],
  ["采购/供应商联调", "procurement-supplier"],
  ["财务", "finance"],
  ["运营/法务", "operations-legal"],
  ["技术实现负责人", "technical-owner"],
]);

function absolute(relativePath) {
  return path.join(repoRoot, relativePath);
}

function exists(relativePath) {
  return fs.existsSync(absolute(relativePath));
}

function readText(relativePath) {
  return fs.readFileSync(absolute(relativePath), "utf8").replace(/^\uFEFF/, "");
}

function readJson(relativePath) {
  return JSON.parse(readText(relativePath));
}

function writeText(relativePath, text) {
  fs.mkdirSync(path.dirname(absolute(relativePath)), { recursive: true });
  fs.writeFileSync(absolute(relativePath), text, "utf8");
}

function normalize(text) {
  return text.replace(/\r\n/g, "\n").trimEnd() + "\n";
}

function escapeMd(value) {
  return String(value ?? "").replace(/\|/g, "/").replace(/\r?\n/g, "<br>");
}

function mdTable(rows, columns) {
  if (rows.length === 0) return "无。\n";
  return [
    `| ${columns.map((column) => column.label).join(" | ")} |`,
    `| ${columns.map((column) => column.align === "right" ? "---:" : "---").join(" | ")} |`,
    ...rows.map((row) => `| ${columns.map((column) => escapeMd(column.value ? column.value(row) : row[column.key])).join(" | ")} |`),
  ].join("\n") + "\n";
}

function collectText(value) {
  if (value === null || value === undefined) return [];
  if (Array.isArray(value)) return value.flatMap((item) => collectText(item));
  if (typeof value === "object") return Object.values(value).flatMap((item) => collectText(item));
  return [String(value)];
}

function forbidPlaintextSecrets(label, value) {
  const text = collectText(value).join("\n");
  const patterns = [
    /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/i,
    /\b(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{16,}/,
    /\bAKIA[0-9A-Z]{16}\b/,
    /\b(?:password|token)\s*[:=]\s*(?:\\*["'][^"'<\n\\]{8,}\\*["']|[A-Za-z0-9_./+=-]{12,})/i,
    /\b(?:secret_key|secret)\s*[:=]\s*(?:\\*["'][^"'<\n\\]{8,}\\*["']|[A-Za-z0-9_./+=-]{12,})/i,
  ];
  const hits = [];
  for (const [index, pattern] of patterns.entries()) {
    if (pattern.test(text)) hits.push(`${label} contains apparent plaintext secret pattern #${index + 1}`);
  }
  return hits;
}

function commandQuote(text) {
  return `'${String(text).replace(/'/g, `'\\''`)}'`;
}

function safeCopyCommand(row) {
  const folder = row.packet_folder || path.dirname(row.target_manifest_path) + "/";
  return `mkdir -p ${commandQuote(folder)} && test ! -e ${commandQuote(row.target_manifest_path)} && cp ${commandQuote(row.template_path)} ${commandQuote(row.target_manifest_path)}`;
}

function ownerSlug(ownerGroup) {
  return ownerSlugByGroup.get(ownerGroup) ?? (ownerGroup.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "") || "unknown-owner");
}

function rowSort(a, b) {
  const priorityOrder = { P0: 0, P1: 1, P2: 2, P3: 3 };
  return (priorityOrder[a.priority] ?? 9) - (priorityOrder[b.priority] ?? 9)
    || Number(a.batch_rank ?? 999) - Number(b.batch_rank ?? 999)
    || String(a.category).localeCompare(String(b.category), "zh-Hans-CN")
    || String(a.item_id).localeCompare(String(b.item_id), "zh-Hans-CN");
}

function firstAction(ownerGroup, rows, quickstart) {
  const summary = (quickstart.owner_summaries ?? []).find((row) => row.owner_group === ownerGroup);
  if (summary?.first_action) return summary.first_action;
  const p0Rows = rows.filter((row) => row.priority === "P0");
  if (p0Rows.length > 0) return `先处理 ${p0Rows[0].item_id}，按模板补真实外部证据链接、复核人、测试时间和 cred:// 引用。`;
  return "按执行包顺序复制模板、补真实外部证据链接并复跑校验命令。";
}

function packetForOwner(ownerGroup, rows, quickstart, ownerGuides) {
  const slug = ownerSlug(ownerGroup);
  const sortedRows = [...rows].sort(rowSort);
  const guide = (ownerGuides.owner_guides ?? []).find((item) => item.owner_group === ownerGroup);
  const p0Rows = sortedRows.filter((row) => row.priority === "P0");
  const counts = {
    total: sortedRows.length,
    p0: p0Rows.length,
    awaiting_submission: sortedRows.filter((row) => row.current_status === "awaiting_submission").length,
    valid_submission: sortedRows.filter((row) => row.current_status === "valid_submission").length,
    invalid_submission: sortedRows.filter((row) => row.current_status === "invalid_submission").length,
  };
  const rowsForPacket = sortedRows.map((row) => ({
    ledger_id: row.ledger_id,
    batch_id: row.batch_id,
    batch_rank: row.batch_rank,
    owner_group: row.owner_group,
    priority: row.priority,
    category: row.category,
    item_id: row.item_id,
    current_status: row.current_status,
    blocking_scope: row.blocking_scope,
    template_path: row.template_path,
    target_manifest_path: row.target_manifest_path,
    packet_folder: row.packet_folder,
    required_evidence_summary: row.required_evidence_summary ?? [],
    accepted_evidence_types: row.accepted_evidence_types ?? [],
    not_accepted_evidence: row.not_accepted_evidence ?? [],
    operator_next_step: row.operator_next_step,
    safe_copy_command: safeCopyCommand(row),
    verification_commands: row.verification_commands ?? [],
  }));
  return {
    owner_group: ownerGroup,
    owner_slug: slug,
    packet_path: `${ownerPacketRoot}/${slug}.md`,
    owner_guide_path: guide?.guide_path ?? `docs/production-readiness-evidence/owner-guides/${slug}.md`,
    status: counts.invalid_submission > 0 ? "invalid_submission_needs_fix" : counts.awaiting_submission > 0 ? "awaiting_real_evidence" : "submitted",
    first_action: firstAction(ownerGroup, sortedRows, quickstart),
    counts,
    p0_item_ids: p0Rows.map((row) => row.item_id),
    rows: rowsForPacket,
  };
}

function renderOwnerPacket(packet) {
  const p0Rows = packet.rows.filter((row) => row.priority === "P0");
  const allRows = packet.rows;
  const commandRows = p0Rows.slice(0, 8).map((row) => ({ item_id: row.item_id, command: row.safe_copy_command }));
  return normalize(`# ${packet.owner_group} 生产证据执行包 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_OWNER_EXECUTION_PACKET_READY
- PRODUCTION_EVIDENCE_DRAFT_PREP_READY
- OWNER_PACKET_DOES_NOT_CLOSE_BLOCKERS
- RAW_EVIDENCE_STAYS_EXTERNAL
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET

## 2. 当前任务

| 项目 | 值 |
|---|---:|
| 状态 | ${packet.status} |
| 回填项 | ${packet.counts.total} |
| P0 回填项 | ${packet.counts.p0} |
| 等待提交 | ${packet.counts.awaiting_submission} |
| 无效提交 | ${packet.counts.invalid_submission} |

第一动作：${packet.first_action}

配套责任组说明：\`${packet.owner_guide_path}\`。

## 3. 批量草稿准备

默认只预览，不写入文件：

\`\`\`bash
npm run prepare:production-evidence-drafts -- --owner ${packet.owner_slug} --p0
\`\`\`

实际复制模板时必须显式加 \`--execute\`，且已有 manifest 不会被覆盖：

\`\`\`bash
npm run prepare:production-evidence-drafts -- --owner ${packet.owner_slug} --p0 --execute
\`\`\`

复制出来的仍是待填写草稿，不是生产证据；必须替换占位字段、补外部证据链接和复核人后再跑状态校验。

## 4. P0 优先项

${mdTable(p0Rows, [
  { key: "item_id", label: "item_id" },
  { key: "category", label: "category" },
  { key: "current_status", label: "status" },
  { key: "blocking_scope", label: "blocking_scope" },
  { key: "template_path", label: "template_path" },
  { key: "target_manifest_path", label: "target_manifest_path" },
])}
## 5. 可复制命令示例

这些命令只复制模板到目标 manifest 路径，不填真实证据、不关闭阻断；如果目标 manifest 已存在，命令会停止，避免覆盖人工提交。

${mdTable(commandRows, [
  { key: "item_id", label: "item_id" },
  { key: "command", label: "copy_command" },
])}
## 6. 全部回填项

${mdTable(allRows, [
  { key: "item_id", label: "item_id" },
  { key: "priority", label: "priority" },
  { key: "category", label: "category" },
  { key: "current_status", label: "status" },
  { key: "operator_next_step", label: "operator_next_step" },
  { key: "accepted_evidence_types", label: "accepted_evidence_types" },
])}
## 7. 提交后复跑

\`\`\`bash
npm run verify:production-evidence-submissions-guard
npm run docs:production-evidence-submission-status
npm run verify:production-evidence-submission-status
npm run docs:production-evidence-backfill-priority
npm run docs:production-evidence-backfill-execution-ledger
npm run docs:production-evidence-owner-execution-packets
npm run verify:production-evidence-owner-execution-packets
npm run verify:delivery-gates
\`\`\`

## 8. 禁止事项

- 不提交原始截图、日志、导出文件、私钥、token 或 SMB 密码到仓库。
- 不把 mock、沙箱、口头确认、占位链接当作生产证据。
- 不让同一人同时作为 owner_person 和 reviewer。
- 不用本执行包关闭生产阻断；只有 manifest 通过状态校验且外部证据可追溯时，才进入验收判断。
`);
}

function renderIndex(output) {
  return normalize(`# 生产证据责任组执行包索引 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_OWNER_EXECUTION_PACKETS_READY
- OWNER_PACKET_ONE_STOP_BACKFILL_READY
- PRODUCTION_EVIDENCE_DRAFT_PREP_READY
- P0_FIRST_EXECUTION_ORDER_READY
- OWNER_PACKET_DOES_NOT_CLOSE_BLOCKERS
- RAW_EVIDENCE_STAYS_EXTERNAL
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET

## 2. 定位

本文从生产证据提交状态、P0 快速表、回填执行台账和责任组说明包生成。它把 40 个待提交生产证据拆成责任组执行包，方便 IT、财务、采购、运营/法务和技术负责人按同一口径回填。

它不创建真实证据，不关闭生产阻断，不允许原始证据入库。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| 责任组执行包 | ${output.counts.owner_packets} |
| 回填项 | ${output.counts.rows} |
| P0 回填项 | ${output.counts.p0_rows} |
| 等待提交 | ${output.counts.awaiting_submission} |
| 无效提交 | ${output.counts.invalid_submission} |

## 4. 责任组执行包

${mdTable(output.owner_packets, [
  { key: "owner_group", label: "owner_group" },
  { key: "packet_path", label: "packet_path" },
  { key: "owner_guide_path", label: "owner_guide_path" },
  { key: "status", label: "status" },
  { key: "counts", label: "p0/total", value: (row) => `${row.counts.p0}/${row.counts.total}` },
  { key: "first_action", label: "first_action" },
])}
## 5. 使用顺序

1. 先派发 P0 数量最多、阻断范围最大的责任组执行包。
2. 责任组可先运行 \`npm run prepare:production-evidence-drafts -- --owner <owner_slug> --p0\` 预览待复制草稿。
3. 需要批量复制模板时显式追加 \`--execute\`，已有 manifest 不会被覆盖。
4. 责任组只复制模板到目标 manifest，不提交原始证据文件。
5. 填写外部证据链接、审批记录、测试时间、复核人和 \`cred://prod/...\` 引用。
6. 提交后复跑本文第 6 节命令。
7. 若出现 invalid manifest，先修 invalid，再继续补 awaiting。

## 6. 总复跑命令

\`\`\`bash
npm run verify:production-evidence-submissions-guard
npm run docs:production-evidence-submission-status
npm run verify:production-evidence-submission-status
npm run docs:production-evidence-backfill-priority
npm run docs:production-evidence-backfill-execution-ledger
npm run docs:production-evidence-owner-execution-packets
npm run verify:production-evidence-owner-execution-packets
npm run verify:production-evidence-draft-prep
npm run verify:closed-loop-current-state
npm run verify:delivery-gates
\`\`\`
`);
}

function validate(output, rendered) {
  const failures = [];
  if (output.counts.owner_packets < 1) failures.push("owner execution packets must not be empty");
  if (output.counts.rows !== output.source_counts.expected_submissions) failures.push(`packet rows ${output.counts.rows} must equal expected submissions ${output.source_counts.expected_submissions}`);
  if (output.counts.p0_rows !== output.source_counts.p0_expected) failures.push(`P0 rows ${output.counts.p0_rows} must equal P0 expected ${output.source_counts.p0_expected}`);
  if (output.counts.invalid_submission !== 0) failures.push("owner execution packets must not include invalid submissions in delivery-ready state");
  const seenPacketPaths = new Set();
  for (const packet of output.owner_packets) {
    if (seenPacketPaths.has(packet.packet_path)) failures.push(`duplicate packet path ${packet.packet_path}`);
    seenPacketPaths.add(packet.packet_path);
    if (!packet.owner_guide_path) failures.push(`${packet.owner_group} missing owner guide path`);
    if (packet.counts.total !== packet.rows.length) failures.push(`${packet.owner_group} count mismatch`);
    for (const row of packet.rows) {
      if (!row.template_path || !row.target_manifest_path) failures.push(`${packet.owner_group}/${row.item_id} missing template or target manifest path`);
      if (!row.target_manifest_path.startsWith("docs/production-readiness-evidence/submissions/")) failures.push(`${row.item_id} target manifest path must stay under submissions/`);
      if (!Array.isArray(row.accepted_evidence_types) || row.accepted_evidence_types.length === 0) failures.push(`${row.item_id} must have accepted evidence types`);
      if (!row.safe_copy_command.includes("test ! -e")) failures.push(`${row.item_id} copy command must not overwrite existing manifest`);
    }
  }
  for (const [label, text] of Object.entries(rendered)) failures.push(...forbidPlaintextSecrets(label, text));
  return failures;
}

const status = readJson(statusPath);
const priority = readJson(priorityPath);
const ledger = readJson(ledgerPath);
const quickstart = readJson(quickstartPath);
const ownerGuides = readJson(ownerGuidesPath);

const rows = ledger.rows ?? [];
const rowsByOwner = new Map();
for (const row of rows) {
  if (!rowsByOwner.has(row.owner_group)) rowsByOwner.set(row.owner_group, []);
  rowsByOwner.get(row.owner_group).push(row);
}

const ownerPackets = [...rowsByOwner.entries()]
  .map(([ownerGroup, ownerRows]) => packetForOwner(ownerGroup, ownerRows, quickstart, ownerGuides))
  .sort((a, b) => b.counts.p0 - a.counts.p0 || b.counts.total - a.counts.total || a.owner_group.localeCompare(b.owner_group, "zh-Hans-CN"));

const output = {
  owner_execution_packets_version: "v0.1",
  status: status.counts?.invalid_submission > 0 ? "invalid_submission_blocks_owner_execution" : status.counts?.awaiting_submission > 0 ? "awaiting_real_evidence" : "all_evidence_submitted",
  generated_from: [statusPath, priorityPath, ledgerPath, quickstartPath, ownerGuidesPath],
  policy: {
    owner_packets_do_not_close_blockers: true,
    one_stop_backfill_packet: true,
    p0_first_execution_order: true,
    raw_evidence_stays_external: true,
    credential_ref_only_no_plaintext_secret: true,
    safe_copy_command_does_not_overwrite_existing_manifest: true,
  },
  source_counts: {
    expected_submissions: status.counts?.expected_submissions ?? 0,
    p0_expected: status.counts?.p0_awaiting_submission + status.counts?.p0_valid_submission + status.counts?.p0_invalid_submission,
  },
  counts: {
    owner_packets: ownerPackets.length,
    rows: ownerPackets.reduce((sum, packet) => sum + packet.counts.total, 0),
    p0_rows: ownerPackets.reduce((sum, packet) => sum + packet.counts.p0, 0),
    awaiting_submission: ownerPackets.reduce((sum, packet) => sum + packet.counts.awaiting_submission, 0),
    valid_submission: ownerPackets.reduce((sum, packet) => sum + packet.counts.valid_submission, 0),
    invalid_submission: ownerPackets.reduce((sum, packet) => sum + packet.counts.invalid_submission, 0),
  },
  owner_packets: ownerPackets,
};

const indexMd = renderIndex(output);
const ownerMarkdown = Object.fromEntries(ownerPackets.map((packet) => [packet.packet_path, renderOwnerPacket(packet)]));
const json = normalize(JSON.stringify(output, null, 2));
const renderedForValidation = { [mdPath]: indexMd, [jsonPath]: json, ...ownerMarkdown };
const failures = validate(output, renderedForValidation);

if (checkMode) {
  if (!exists(jsonPath)) failures.push(`${jsonPath} is missing; run npm run docs:production-evidence-owner-execution-packets`);
  else if (normalize(readText(jsonPath)) !== json) failures.push(`${jsonPath} is not in sync`);
  if (!exists(mdPath)) failures.push(`${mdPath} is missing; run npm run docs:production-evidence-owner-execution-packets`);
  else if (normalize(readText(mdPath)) !== indexMd) failures.push(`${mdPath} is not in sync`);
  for (const [packetPath, packetMd] of Object.entries(ownerMarkdown)) {
    if (!exists(packetPath)) failures.push(`${packetPath} is missing; run npm run docs:production-evidence-owner-execution-packets`);
    else if (normalize(readText(packetPath)) !== packetMd) failures.push(`${packetPath} is not in sync`);
  }
  if (failures.length > 0) {
    console.error("PRODUCTION_EVIDENCE_OWNER_EXECUTION_PACKETS_FAIL");
    for (const failure of failures) console.error(`- ${failure}`);
    process.exit(1);
  }
  console.log("PRODUCTION_EVIDENCE_OWNER_EXECUTION_PACKETS_PASS");
} else {
  writeText(jsonPath, json);
  writeText(mdPath, indexMd);
  for (const [packetPath, packetMd] of Object.entries(ownerMarkdown)) writeText(packetPath, packetMd);
  if (failures.length > 0) {
    console.error("PRODUCTION_EVIDENCE_OWNER_EXECUTION_PACKETS_WARN");
    for (const failure of failures) console.error(`- ${failure}`);
  }
  console.log(`generated ${jsonPath}`);
  console.log(`generated ${mdPath}`);
  console.log(`generated ${ownerPackets.length} owner execution packet(s)`);
}
