import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const submissionsRoot = "docs/production-readiness-evidence/submissions";
const kitPath = "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json";
const failures = [];

function readText(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath);
  if (!fs.existsSync(absolutePath)) {
    failures.push(`${relativePath} is missing`);
    return "";
  }
  return fs.readFileSync(absolutePath, "utf8");
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

function requireMarkers(relativePath, markers) {
  const text = readText(relativePath);
  for (const marker of markers) {
    if (!text.includes(marker)) failures.push(`${relativePath} must include marker: ${marker}`);
  }
  return text;
}

function walkFiles(absoluteDir) {
  if (!fs.existsSync(absoluteDir)) return [];
  const entries = fs.readdirSync(absoluteDir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const absolutePath = path.join(absoluteDir, entry.name);
    if (entry.isDirectory()) files.push(...walkFiles(absolutePath));
    if (entry.isFile()) files.push(absolutePath);
  }
  return files;
}

function repoRelative(absolutePath) {
  return path.relative(repoRoot, absolutePath).split(path.sep).join("/");
}

function relativeToSubmissions(absolutePath) {
  return path.relative(path.join(repoRoot, submissionsRoot), absolutePath).split(path.sep).join("/");
}

function forbidPlaintextSecrets(relativePath, text) {
  const patterns = [
    /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/i,
    /\b(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{16,}/,
    /\bAKIA[0-9A-Z]{16}\b/,
    /\b(?:password|token)\s*[:=]\s*(?:\\*["'][^"'<\n\\]{8,}\\*["']|[A-Za-z0-9_./+=-]{12,})/i,
    /\b(?:secret_key|secret)\s*[:=]\s*(?:\\*["'][^"'<\n\\]{8,}\\*["']|[A-Za-z0-9_./+=-]{12,})/i,
  ];
  for (const [index, pattern] of patterns.entries()) {
    if (pattern.test(text)) failures.push(`${relativePath} contains apparent plaintext secret pattern #${index + 1}`);
  }
}

const rootAbsolute = path.join(repoRoot, submissionsRoot);
if (!fs.existsSync(rootAbsolute)) failures.push(`${submissionsRoot} directory is missing`);

const readmeText = requireMarkers(`${submissionsRoot}/README.md`, [
  "PRODUCTION_EVIDENCE_SUBMISSIONS_GUARD_READY",
  "MANIFEST_ONLY_IN_REPO",
  "RAW_EVIDENCE_STAYS_EXTERNAL",
  "CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET",
  "NO_FAKE_PRODUCTION_EVIDENCE_CLOSURE",
]);
const gitignoreText = requireMarkers(`${submissionsRoot}/.gitignore`, [
  "PRODUCTION_EVIDENCE_SUBMISSIONS_GUARD_READY",
  "MANIFEST_ONLY_IN_REPO",
  "RAW_EVIDENCE_STAYS_EXTERNAL",
  "!**/manifest-*.json",
]);
forbidPlaintextSecrets(`${submissionsRoot}/README.md`, readmeText);
forbidPlaintextSecrets(`${submissionsRoot}/.gitignore`, gitignoreText);

const kit = readJson(kitPath);
const expectedManifestPaths = new Set((kit?.submissions ?? []).map((submission) => `${submission.packet_folder}${submission.manifest_file_name}`));

for (const absoluteFile of walkFiles(rootAbsolute)) {
  const relativeRepoPath = repoRelative(absoluteFile);
  const relativeSubmissionPath = relativeToSubmissions(absoluteFile);
  const text = fs.readFileSync(absoluteFile, "utf8");
  forbidPlaintextSecrets(relativeRepoPath, text);
  if (["README.md", ".gitignore"].includes(relativeSubmissionPath)) continue;
  if (/^(.+\/)?manifest-[A-Za-z0-9_.-]+\.json$/.test(relativeSubmissionPath)) {
    if (!expectedManifestPaths.has(relativeRepoPath)) failures.push(`${relativeRepoPath} is a manifest but not an expected submission manifest path`);
    continue;
  }
  failures.push(`${relativeRepoPath} is not allowed in git; store raw evidence externally and reference it from manifest evidence_links`);
}

if (failures.length > 0) {
  console.error("PRODUCTION_EVIDENCE_SUBMISSIONS_GUARD_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("PRODUCTION_EVIDENCE_SUBMISSIONS_GUARD_PASS");
