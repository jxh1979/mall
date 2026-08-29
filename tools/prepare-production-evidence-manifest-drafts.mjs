import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const ownerPacketsPath = "docs/production-readiness-evidence/production-evidence-owner-execution-packets-latest.json";
const submissionsRoot = "docs/production-readiness-evidence/submissions/";
const templatesRoot = "docs/production-readiness-evidence/submission-manifest-templates/";

const args = process.argv.slice(2);
const execute = args.includes("--execute");
const checkMode = args.includes("--check") || args.includes("--verify");
const onlyP0 = args.includes("--p0");
const all = args.includes("--all");
const jsonOutput = args.includes("--json");

function valuesFor(flag) {
  const values = [];
  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index];
    if (arg === flag) {
      const value = args[index + 1];
      if (!value || value.startsWith("--")) throw new Error(`${flag} requires a value`);
      values.push(value);
      index += 1;
    } else if (arg.startsWith(`${flag}=`)) {
      values.push(arg.slice(flag.length + 1));
    }
  }
  return values;
}

const ownerFilters = new Set(valuesFor("--owner"));
const itemFilters = new Set(valuesFor("--item"));

function absolute(relativePath) {
  return path.join(repoRoot, relativePath);
}

function readJson(relativePath) {
  return JSON.parse(fs.readFileSync(absolute(relativePath), "utf8").replace(/^\uFEFF/, ""));
}

function normalizeRelative(relativePath) {
  const normalized = path.posix.normalize(String(relativePath).replace(/\\/g, "/"));
  if (normalized.startsWith("../") || normalized === ".." || path.posix.isAbsolute(normalized)) {
    throw new Error(`Unsafe relative path: ${relativePath}`);
  }
  return normalized;
}

function assertSafeTemplatePath(relativePath) {
  const normalized = normalizeRelative(relativePath);
  if (!normalized.startsWith(templatesRoot)) throw new Error(`Template path must stay under ${templatesRoot}: ${relativePath}`);
  if (!normalized.endsWith(".json")) throw new Error(`Template path must be JSON: ${relativePath}`);
  return normalized;
}

function assertSafeTargetPath(relativePath, itemId) {
  const normalized = normalizeRelative(relativePath);
  if (!normalized.startsWith(submissionsRoot)) throw new Error(`Target path must stay under ${submissionsRoot}: ${relativePath}`);
  if (path.posix.basename(normalized) !== `manifest-${itemId}.json`) {
    throw new Error(`Target manifest name must be manifest-${itemId}.json: ${relativePath}`);
  }
  return normalized;
}

function forbiddenSecretHits(label, text) {
  const patterns = [
    /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/i,
    /\b(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{16,}/,
    /\bAKIA[0-9A-Z]{16}\b/,
    /\b(?:password|token)\s*[:=]\s*(?:\\*["'][^"'<\n\\]{8,}\\*["']|[A-Za-z0-9_./+=-]{12,})/i,
    /\b(?:secret_key|secret)\s*[:=]\s*(?:\\*["'][^"'<\n\\]{8,}\\*["']|[A-Za-z0-9_./+=-]{12,})/i,
  ];
  return patterns.flatMap((pattern, index) => pattern.test(text) ? [`${label} contains apparent plaintext secret pattern #${index + 1}`] : []);
}

function loadRows() {
  const packets = readJson(ownerPacketsPath);
  const rows = [];
  for (const packet of packets.owner_packets ?? []) {
    for (const row of packet.rows ?? []) {
      rows.push({
        owner_group: packet.owner_group,
        owner_slug: packet.owner_slug,
        item_id: row.item_id,
        priority: row.priority,
        category: row.category,
        template_path: assertSafeTemplatePath(row.template_path),
        target_manifest_path: assertSafeTargetPath(row.target_manifest_path, row.item_id),
        blocking_scope: row.blocking_scope,
      });
    }
  }
  return { packets, rows };
}

function selectedRows(rows) {
  return rows.filter((row) => {
    if (onlyP0 && row.priority !== "P0") return false;
    if (ownerFilters.size > 0 && !ownerFilters.has(row.owner_slug) && !ownerFilters.has(row.owner_group)) return false;
    if (itemFilters.size > 0 && !itemFilters.has(row.item_id)) return false;
    return true;
  });
}

function assertKnownFilters(rows) {
  const owners = new Set(rows.flatMap((row) => [row.owner_slug, row.owner_group]));
  const items = new Set(rows.map((row) => row.item_id));
  const failures = [];
  for (const owner of ownerFilters) if (!owners.has(owner)) failures.push(`Unknown owner filter: ${owner}`);
  for (const item of itemFilters) if (!items.has(item)) failures.push(`Unknown item filter: ${item}`);
  return failures;
}

function buildPlan(rows) {
  return rows.map((row) => {
    const templateExists = fs.existsSync(absolute(row.template_path));
    const targetExists = fs.existsSync(absolute(row.target_manifest_path));
    return {
      owner_group: row.owner_group,
      owner_slug: row.owner_slug,
      item_id: row.item_id,
      priority: row.priority,
      category: row.category,
      template_path: row.template_path,
      target_manifest_path: row.target_manifest_path,
      action: targetExists ? "skip_existing" : "copy_template",
      template_exists: templateExists,
      target_exists: targetExists,
      blocking_scope: row.blocking_scope,
    };
  });
}

function validatePlan(plan, packets) {
  const failures = [];
  const allRows = packets.owner_packets?.flatMap((packet) => packet.rows ?? []) ?? [];
  const p0Rows = allRows.filter((row) => row.priority === "P0");
  if (allRows.length !== packets.source_counts?.expected_submissions) {
    failures.push(`Expected ${packets.source_counts?.expected_submissions} rows, got ${allRows.length}`);
  }
  if (p0Rows.length !== packets.source_counts?.p0_expected) {
    failures.push(`Expected ${packets.source_counts?.p0_expected} P0 rows, got ${p0Rows.length}`);
  }
  for (const item of plan) {
    if (!item.template_exists) failures.push(`Template missing for ${item.item_id}: ${item.template_path}`);
    if (!item.target_manifest_path.startsWith(submissionsRoot)) failures.push(`Target outside submissions for ${item.item_id}`);
    if (item.target_exists && item.action !== "skip_existing") failures.push(`Existing target must be skipped for ${item.item_id}`);
    if (item.template_exists) {
      const templateText = fs.readFileSync(absolute(item.template_path), "utf8");
      failures.push(...forbiddenSecretHits(item.template_path, templateText));
    }
  }
  return failures;
}

function copyDrafts(plan) {
  const copied = [];
  const skipped = [];
  for (const item of plan) {
    if (item.target_exists) {
      skipped.push(item);
      continue;
    }
    fs.mkdirSync(path.dirname(absolute(item.target_manifest_path)), { recursive: true });
    fs.copyFileSync(absolute(item.template_path), absolute(item.target_manifest_path), fs.constants.COPYFILE_EXCL);
    copied.push(item);
  }
  return { copied, skipped };
}

function printHuman(result) {
  console.log("PRODUCTION_EVIDENCE_DRAFT_PREP_READY");
  console.log(`mode=${result.mode}`);
  console.log(`selected=${result.selected_count}`);
  console.log(`copy_template=${result.copy_template_count}`);
  console.log(`skip_existing=${result.skip_existing_count}`);
  console.log(`p0_selected=${result.p0_selected_count}`);
  if (result.copied_count !== undefined) console.log(`copied=${result.copied_count}`);
  if (result.skipped_count !== undefined) console.log(`skipped=${result.skipped_count}`);
  for (const item of result.plan.slice(0, 20)) {
    console.log(`${item.action}\t${item.owner_slug}\t${item.item_id}\t${item.target_manifest_path}`);
  }
  if (result.plan.length > 20) console.log(`... ${result.plan.length - 20} more row(s)`);
}

let packets;
let rows;
try {
  ({ packets, rows } = loadRows());
} catch (error) {
  console.error(`PRODUCTION_EVIDENCE_DRAFT_PREP_FAIL\n- ${error.message}`);
  process.exit(1);
}

const selectorProvided = ownerFilters.size > 0 || itemFilters.size > 0 || all || onlyP0;
const failures = assertKnownFilters(rows);
if (execute && !selectorProvided) failures.push("--execute requires --owner, --item, --p0, or --all to avoid accidental broad copy");

const plan = buildPlan(selectedRows(rows));
failures.push(...validatePlan(buildPlan(rows), packets));
if (plan.length === 0) failures.push("selected plan is empty");

if (failures.length > 0) {
  console.error("PRODUCTION_EVIDENCE_DRAFT_PREP_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

let copyResult;
if (execute && !checkMode) copyResult = copyDrafts(plan);

const result = {
  mode: checkMode ? "check" : execute ? "execute" : "dry-run",
  source: ownerPacketsPath,
  selected_count: plan.length,
  p0_selected_count: plan.filter((item) => item.priority === "P0").length,
  copy_template_count: plan.filter((item) => item.action === "copy_template").length,
  skip_existing_count: plan.filter((item) => item.action === "skip_existing").length,
  copied_count: copyResult?.copied.length,
  skipped_count: copyResult?.skipped.length,
  policy: {
    default_is_dry_run: true,
    execute_required_for_copy: true,
    no_overwrite: true,
    submissions_manifest_only: true,
    drafts_do_not_close_blockers: true,
  },
  plan,
};

if (jsonOutput) console.log(JSON.stringify(result, null, 2));
else if (checkMode) console.log("PRODUCTION_EVIDENCE_DRAFT_PREP_PASS");
else printHuman(result);
