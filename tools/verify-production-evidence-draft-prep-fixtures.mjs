import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const repoRoot = process.cwd();
const toolPath = "tools/prepare-production-evidence-manifest-drafts.mjs";
const packetPath = "docs/production-readiness-evidence/production-evidence-owner-execution-packets-latest.json";
const marker = "PRODUCTION_EVIDENCE_DRAFT_PREP_FIXTURES_READY";

function absolute(relativePath) {
  return path.join(repoRoot, relativePath);
}

function readJson(relativePath) {
  return JSON.parse(fs.readFileSync(absolute(relativePath), "utf8").replace(/^\uFEFF/, ""));
}

function mkdirFor(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function copyFileToTemp(tempRoot, relativePath) {
  const target = path.join(tempRoot, relativePath);
  mkdirFor(target);
  fs.copyFileSync(absolute(relativePath), target);
}

function runTool(tempRoot, args) {
  const result = spawnSync(process.execPath, [toolPath, ...args, "--json"], {
    cwd: tempRoot,
    encoding: "utf8",
  });
  if (result.status !== 0) {
    throw new Error(`draft prep command failed: ${result.stderr || result.stdout}`);
  }
  try {
    return JSON.parse(result.stdout);
  } catch (error) {
    throw new Error(`draft prep command did not return JSON: ${error.message}\n${result.stdout}`);
  }
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

const packets = readJson(packetPath);
const firstPacket = packets.owner_packets?.find((packet) => packet.owner_slug === "it-ops-security") ?? packets.owner_packets?.[0];
const firstRow = firstPacket?.rows?.find((row) => row.priority === "P0") ?? firstPacket?.rows?.[0];
if (!firstPacket || !firstRow) throw new Error("owner execution packet fixture source is empty");

const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "complex-evidence-draft-prep-"));
try {
  copyFileToTemp(tempRoot, toolPath);
  copyFileToTemp(tempRoot, packetPath);
  for (const packet of packets.owner_packets ?? []) {
    for (const row of packet.rows ?? []) {
      copyFileToTemp(tempRoot, row.template_path);
    }
  }

  const dryRun = runTool(tempRoot, ["--owner", firstPacket.owner_slug, "--item", firstRow.item_id]);
  assert(dryRun.mode === "dry-run", "default invocation must be dry-run");
  assert(dryRun.selected_count === 1, "dry-run should select exactly one fixture row");
  assert(dryRun.copy_template_count === 1, "dry-run should plan one template copy");
  assert(dryRun.copied_count === undefined, "dry-run must not copy files");
  assert(!fs.existsSync(path.join(tempRoot, firstRow.target_manifest_path)), "dry-run must not create target manifest");

  const execute = runTool(tempRoot, ["--owner", firstPacket.owner_slug, "--item", firstRow.item_id, "--execute"]);
  assert(execute.mode === "execute", "execute invocation must report execute mode");
  assert(execute.copied_count === 1, "first execute should copy exactly one manifest");
  const targetPath = path.join(tempRoot, firstRow.target_manifest_path);
  assert(fs.existsSync(targetPath), "execute must create target manifest");
  const original = fs.readFileSync(targetPath, "utf8");
  assert(original === fs.readFileSync(path.join(tempRoot, firstRow.template_path), "utf8"), "created manifest must equal template content");

  fs.writeFileSync(targetPath, `${original}\n// SENTINEL_DO_NOT_OVERWRITE\n`, "utf8");
  const secondExecute = runTool(tempRoot, ["--owner", firstPacket.owner_slug, "--item", firstRow.item_id, "--execute"]);
  assert(secondExecute.mode === "execute", "second execute invocation must report execute mode");
  assert(secondExecute.copied_count === 0, "second execute must not overwrite existing manifest");
  assert(secondExecute.skipped_count === 1, "second execute must skip existing manifest");
  const afterSecond = fs.readFileSync(targetPath, "utf8");
  assert(afterSecond.endsWith("// SENTINEL_DO_NOT_OVERWRITE\n"), "existing manifest content must not be overwritten");

  const broadExecute = spawnSync(process.execPath, [toolPath, "--execute", "--json"], {
    cwd: tempRoot,
    encoding: "utf8",
  });
  assert(broadExecute.status !== 0, "--execute without selector must be rejected");
  assert((broadExecute.stderr + broadExecute.stdout).includes("--execute requires"), "broad execute rejection must explain selector requirement");

  console.log(`${marker}: item=${firstRow.item_id}, owner=${firstPacket.owner_slug}`);
} finally {
  fs.rmSync(tempRoot, { recursive: true, force: true });
}
