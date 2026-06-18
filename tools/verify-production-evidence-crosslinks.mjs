import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const failures = [];

function absolute(relativePath) {
  return path.join(repoRoot, relativePath);
}

function readText(relativePath) {
  const file = absolute(relativePath);
  if (!fs.existsSync(file)) {
    failures.push(`${relativePath} is missing`);
    return "";
  }
  return fs.readFileSync(file, "utf8").replace(/^\uFEFF/, "");
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

function requireFile(relativePath) {
  if (!fs.existsSync(absolute(relativePath))) failures.push(`${relativePath} must exist`);
}

function keyOf(row) {
  return `${row.owner_group}::${row.submission_id ?? row.item_id}::${row.item_id}`;
}

function manifestKey(row) {
  return row.target_manifest_path ?? row.manifest_path ?? `${row.packet_folder ?? ""}${row.manifest_file_name ?? ""}`;
}

function mapBy(rows, getKey, label) {
  const map = new Map();
  for (const row of rows) {
    const key = getKey(row);
    if (map.has(key)) failures.push(`${label} contains duplicate key ${key}`);
    map.set(key, row);
  }
  return map;
}

function assertEqual(label, actual, expected) {
  if (actual !== expected) failures.push(`${label} expected ${expected}, got ${actual}`);
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
  for (const [index, pattern] of patterns.entries()) {
    if (pattern.test(text)) failures.push(`${label} contains apparent plaintext secret pattern #${index + 1}`);
  }
}

const kitPath = "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json";
const templateIndexPath = "docs/production-readiness-evidence/production-evidence-manifest-template-index-latest.json";
const statusPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
const ledgerPath = "docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.json";
const quickstartPath = "docs/production-readiness-evidence/production-evidence-p0-quickstart-latest.json";
const ownerPacketsPath = "docs/production-readiness-evidence/production-evidence-owner-execution-packets-latest.json";

const kit = readJson(kitPath);
const templateIndex = readJson(templateIndexPath);
const status = readJson(statusPath);
const ledger = readJson(ledgerPath);
const quickstart = readJson(quickstartPath);
const ownerPackets = readJson(ownerPacketsPath);

if (kit && templateIndex && status && ledger && quickstart && ownerPackets) {
  const kitRows = kit.submissions ?? [];
  const templateRows = templateIndex.templates ?? [];
  const statusRows = status.submissions ?? [];
  const ledgerRows = ledger.rows ?? [];
  const quickstartRows = quickstart.p0_rows ?? [];
  const ownerPacketRows = (ownerPackets.owner_packets ?? []).flatMap((packet) => (packet.rows ?? []).map((row) => ({ ...row, owner_packet: packet.packet_path, owner_guide: packet.owner_guide_path })));

  assertEqual("kit submission count", kitRows.length, kit.counts?.submission_templates ?? kitRows.length);
  assertEqual("template index count", templateRows.length, templateIndex.counts?.templates ?? templateRows.length);
  assertEqual("status submission count", statusRows.length, status.counts?.expected_submissions ?? statusRows.length);
  assertEqual("ledger row count", ledgerRows.length, ledger.counts?.ledger_rows ?? ledgerRows.length);
  assertEqual("owner packet row count", ownerPacketRows.length, ownerPackets.counts?.rows ?? ownerPacketRows.length);

  const kitP0 = kitRows.filter((row) => row.priority === "P0");
  const templateP0 = templateRows.filter((row) => row.priority === "P0");
  const ledgerP0 = ledgerRows.filter((row) => row.priority === "P0");
  const ownerPacketP0 = ownerPacketRows.filter((row) => row.priority === "P0");
  assertEqual("kit P0 count", kitP0.length, kit.counts?.p0_submission_templates ?? kitP0.length);
  assertEqual("template P0 count", templateP0.length, templateIndex.counts?.p0_templates ?? templateP0.length);
  assertEqual("ledger P0 count", ledgerP0.length, ledger.counts?.p0_rows ?? ledgerP0.length);
  assertEqual("quickstart P0 count", quickstartRows.length, quickstart.counts?.p0_expected ?? quickstartRows.length);
  assertEqual("owner packet P0 count", ownerPacketP0.length, ownerPackets.counts?.p0_rows ?? ownerPacketP0.length);

  const templateBySubmission = mapBy(templateRows, (row) => row.submission_id, "template index");
  const statusBySubmission = mapBy(statusRows, (row) => row.submission_id, "submission status");
  const ledgerBySubmission = mapBy(ledgerRows, (row) => row.submission_id, "backfill ledger");
  const quickstartByTarget = mapBy(quickstartRows, manifestKey, "P0 quickstart");
  const ownerPacketByTarget = mapBy(ownerPacketRows, manifestKey, "owner execution packets");

  const kitByKey = mapBy(kitRows, keyOf, "submission kit");
  mapBy(templateRows, keyOf, "template index normalized key");
  mapBy(statusRows, keyOf, "submission status normalized key");
  mapBy(ledgerRows, keyOf, "backfill ledger normalized key");

  for (const submission of kitRows) {
    const targetManifestPath = `${submission.packet_folder}${submission.manifest_file_name}`;
    const template = templateBySubmission.get(submission.submission_id);
    const statusRow = statusBySubmission.get(submission.submission_id);
    const ledgerRow = ledgerBySubmission.get(submission.submission_id);
    if (!template) failures.push(`${submission.submission_id} missing from template index`);
    if (!statusRow) failures.push(`${submission.submission_id} missing from submission status`);
    if (!ledgerRow) failures.push(`${submission.submission_id} missing from backfill ledger`);
    if (submission.priority === "P0" && !quickstartByTarget.has(targetManifestPath)) failures.push(`${submission.submission_id} missing from P0 quickstart`);

    if (template) {
      assertEqual(`${submission.submission_id} template target_manifest_path`, template.target_manifest_path, targetManifestPath);
      requireFile(template.template_path);
      const templateManifest = readJson(template.template_path);
      if (templateManifest) {
        assertEqual(`${template.template_path} submission_id`, templateManifest.submission_id, submission.submission_id);
        assertEqual(`${template.template_path} item_id`, templateManifest.item_id, submission.item_id);
        assertEqual(`${template.template_path} owner_group`, templateManifest.owner_group, submission.owner_group);
        assertEqual(`${template.template_path} default result`, templateManifest.result, "blocked");
        assertEqual(`${template.template_path} template_notice`, templateManifest.template_notice, "TEMPLATE_ONLY_NOT_PRODUCTION_EVIDENCE");
        if (templateManifest.redaction_confirmed !== false) failures.push(`${template.template_path} redaction_confirmed must default to false`);
        forbidPlaintextSecrets(template.template_path, templateManifest);
      }
    }

    if (statusRow) {
      assertEqual(`${submission.submission_id} status manifest_path`, statusRow.manifest_path, targetManifestPath);
      assertEqual(`${submission.submission_id} status item_id`, statusRow.item_id, submission.item_id);
      assertEqual(`${submission.submission_id} status owner_group`, statusRow.owner_group, submission.owner_group);
    }

    if (ledgerRow) {
      assertEqual(`${submission.submission_id} ledger target_manifest_path`, ledgerRow.target_manifest_path, targetManifestPath);
      assertEqual(`${submission.submission_id} ledger template_path`, ledgerRow.template_path, template?.template_path);
      assertEqual(`${submission.submission_id} ledger item_id`, ledgerRow.item_id, submission.item_id);
      if (!Array.isArray(ledgerRow.verification_commands) || ledgerRow.verification_commands.length === 0) failures.push(`${submission.submission_id} ledger must include verification_commands`);
      if (!String(ledgerRow.operator_next_step ?? "").includes("manifest")) failures.push(`${submission.submission_id} ledger operator_next_step must mention manifest`);
    }

    const ownerPacketRow = ownerPacketByTarget.get(targetManifestPath);
    if (!ownerPacketRow) {
      failures.push(`${submission.submission_id} missing from owner execution packet by target manifest path`);
    } else {
      assertEqual(`${submission.submission_id} owner packet item_id`, ownerPacketRow.item_id, submission.item_id);
      requireFile(ownerPacketRow.owner_packet);
      requireFile(ownerPacketRow.owner_guide);
      if (submission.priority === "P0") {
        const packetText = readText(ownerPacketRow.owner_packet);
        if (!packetText.includes(submission.item_id)) failures.push(`${ownerPacketRow.owner_packet} must include ${submission.item_id}`);
        if (!packetText.includes(targetManifestPath)) failures.push(`${ownerPacketRow.owner_packet} must include ${targetManifestPath}`);
      }
    }
  }

  for (const row of [...templateRows, ...statusRows, ...ledgerRows]) {
    if (!kitByKey.has(keyOf(row))) failures.push(`${row.submission_id ?? row.item_id} exists outside submission kit canonical set`);
  }

  for (const row of quickstartRows) {
    const targetManifestPath = manifestKey(row);
    if (!targetManifestPath) failures.push(`${row.item_id ?? "unknown"} in P0 quickstart must include target_manifest_path`);
    if (!kitRows.some((submission) => `${submission.packet_folder}${submission.manifest_file_name}` === targetManifestPath)) failures.push(`${row.item_id ?? targetManifestPath} in P0 quickstart missing from submission kit by target_manifest_path`);
    if (!row.template_path) failures.push(`${row.item_id ?? targetManifestPath} in P0 quickstart must include template_path`);
    if (row.template_path) requireFile(row.template_path);
    if (!String(row.operator_next_step ?? "").includes("manifest")) failures.push(`${row.item_id ?? targetManifestPath} quickstart operator_next_step must mention manifest`);
  }

  for (const packet of ownerPackets.owner_packets ?? []) {
    requireFile(packet.packet_path);
    requireFile(packet.owner_guide_path);
    const packetText = readText(packet.packet_path);
    for (const marker of [
      "PRODUCTION_EVIDENCE_OWNER_EXECUTION_PACKET_READY",
      "OWNER_PACKET_DOES_NOT_CLOSE_BLOCKERS",
      "CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET",
      "prepare:production-evidence-drafts",
    ]) {
      if (!packetText.includes(marker)) failures.push(`${packet.packet_path} must include marker ${marker}`);
    }
    forbidPlaintextSecrets(packet.packet_path, packetText);
  }
}

const packageJson = readJson("package.json");
if (packageJson) {
  const scripts = packageJson.scripts ?? {};
  if (scripts["verify:production-evidence-crosslinks"] !== "node tools/verify-production-evidence-crosslinks.mjs") failures.push("package.json must expose verify:production-evidence-crosslinks");
  if (!String(scripts["verify:delivery-gates"] ?? "").includes("verify:production-evidence-crosslinks")) failures.push("verify:delivery-gates must include verify:production-evidence-crosslinks");
}

for (const [relativePath, markers] of [
  ["docs/delivery-readiness-gates-v0.1.md", ["PRODUCTION_EVIDENCE_CROSSLINKS_READY", "verify:production-evidence-crosslinks"]],
  ["docs/00-current-effective-document-entry-v0.1.md", ["verify:production-evidence-crosslinks"]],
  ["docs/delivery-package-manifest-v0.1.md", ["verify:production-evidence-crosslinks"]],
]) {
  const text = readText(relativePath);
  for (const marker of markers) if (!text.includes(marker)) failures.push(`${relativePath} must include ${marker}`);
}

if (failures.length > 0) {
  console.error("PRODUCTION_EVIDENCE_CROSSLINKS_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("PRODUCTION_EVIDENCE_CROSSLINKS_PASS");
