import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check");
const templateIndexPath = "docs/production-readiness-evidence/production-evidence-manifest-template-index-latest.json";
const statusPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
const jsonPath = "docs/production-readiness-evidence/production-evidence-owner-guides-latest.json";
const mdPath = "docs/production-evidence-owner-guides-v0.1.md";
const guideRoot = "docs/production-readiness-evidence/owner-guides";

function readText(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), "utf8");
}

function readJson(relativePath) {
  return JSON.parse(readText(relativePath));
}

function normalize(content) {
  return content.replace(/\r\n/g, "\n").trimEnd() + "\n";
}

function escapeCell(value) {
  return String(value ?? "").replace(/\|/g, "/").replace(/\r?\n/g, "<br>");
}

function mdTable(rows, columns) {
  if (rows.length === 0) return "无。\n";
  const header = `| ${columns.map((column) => column.label).join(" | ")} |`;
  const separator = `| ${columns.map(() => "---").join(" | ")} |`;
  const body = rows.map((row) => `| ${columns.map((column) => escapeCell(row[column.key])).join(" | ")} |`);
  return [header, separator, ...body].join("\n") + "\n";
}

function ownerSlugFromTemplatePath(templatePath) {
  const match = String(templatePath).match(/submission-manifest-templates\/([^/]+)\//);
  return match?.[1] ?? "owner";
}

function forbidPlaintextSecrets(relativePath, text) {
  const patterns = [
    /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/i,
    /\b(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{16,}/,
    /\bAKIA[0-9A-Z]{16}\b/,
    /\b(?:password|token)\s*[:=]\s*["'][^"'<\n]{8,}["']/i,
    /\b(?:secret|secret_key)\s*[:=]\s*["'][^"'<\n]{8,}["']/i,
  ];
  for (const [index, pattern] of patterns.entries()) {
    if (pattern.test(text)) throw new Error(`${relativePath} contains apparent plaintext secret pattern #${index + 1}`);
  }
}

function duplicateValues(values) {
  const seen = new Set();
  const duplicates = new Set();
  for (const value of values) {
    if (seen.has(value)) duplicates.add(value);
    seen.add(value);
  }
  return [...duplicates];
}

function guideMd(guide) {
  const rows = guide.items.map((item) => ({
    item_id: item.item_id,
    priority: item.priority,
    category: item.category,
    current_status: item.current_status,
    template_path: item.template_path,
    target_manifest_path: item.target_manifest_path,
  }));
  return normalize(`# ${guide.owner_group} 生产证据回填说明 V0.1\n\n## 1. 机器标记\n\n- PRODUCTION_EVIDENCE_OWNER_GUIDE_READY\n- OWNER_GUIDE_FROM_MANIFEST_TEMPLATES\n- TEMPLATE_ONLY_NOT_PRODUCTION_EVIDENCE\n- NO_OWNER_GUIDE_CLOSES_BLOCKERS\n- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET\n\n## 2. 定位\n\n本文只给 \`${guide.owner_group}\` 使用，列出该责任组需要回填的生产证据模板、真实提交位置和复测命令。本文不关闭任何生产阻断项，不允许把模板、口头确认或 mock 当成生产证据。\n\n## 3. 总览\n\n| 指标 | 数量 |\n|---|---:|\n| 待回填项 | ${guide.counts.items} |\n| P0 待回填项 | ${guide.counts.p0_items} |\n| 当前 awaiting | ${guide.counts.awaiting_submission} |\n| 当前 valid | ${guide.counts.valid_submission} |\n| 当前 invalid | ${guide.counts.invalid_submission} |\n\n## 4. 回填清单\n\n${mdTable(rows, [
    { key: "item_id", label: "item_id" },
    { key: "priority", label: "priority" },
    { key: "category", label: "category" },
    { key: "current_status", label: "current_status" },
    { key: "template_path", label: "template_path" },
    { key: "target_manifest_path", label: "target_manifest_path" },
  ])}\n## 5. 操作步骤\n\n1. 从 \`template_path\` 复制 JSON 模板。\n2. 放到对应 \`target_manifest_path\`，文件名保持 \`manifest-<item_id>.json\`。\n3. 替换责任人、提交时间、证据链接、测试时间、复核人、credential_ref、回滚步骤和复测日期。\n4. 删除或保留 \`template_notice\` 均可，但不得把模板默认值当成真实证据。\n5. 运行以下命令：\n\n\`\`\`bash\nnpm run docs:production-evidence-submission-status\nnpm run verify:production-evidence-submission-status\nnpm run verify:delivery-gates\n\`\`\`\n\n## 6. 禁止事项\n\n1. 不得提交明文密码、token、API secret、私钥正文或供应商密钥。\n2. 不得用 mock、口头确认、占位截图或未脱敏导出关闭生产阻断项。\n3. 看板显示 \`valid_submission\` 后，仍需更新来源台账和 findings register，不能只靠本文关闭阻断。\n`);
}

const templateIndex = readJson(templateIndexPath);
const status = readJson(statusPath);
const statusBySubmissionId = new Map((status.submissions ?? []).map((row) => [row.submission_id, row]));

const guideMap = new Map();
for (const template of templateIndex.templates ?? []) {
  const ownerSlug = ownerSlugFromTemplatePath(template.template_path);
  if (!guideMap.has(ownerSlug)) {
    guideMap.set(ownerSlug, {
      owner_slug: ownerSlug,
      owner_group: template.owner_group,
      guide_path: `${guideRoot}/${ownerSlug}.md`,
      status: "awaiting_real_evidence",
      items: [],
    });
  }
  const statusRow = statusBySubmissionId.get(template.submission_id);
  guideMap.get(ownerSlug).items.push({
    submission_id: template.submission_id,
    item_id: template.item_id,
    priority: template.priority,
    category: template.category,
    template_path: template.template_path,
    target_manifest_path: template.target_manifest_path,
    current_status: statusRow?.status ?? "awaiting_submission",
  });
}

const ownerGuides = [...guideMap.values()].sort((a, b) => a.owner_slug.localeCompare(b.owner_slug));
for (const guide of ownerGuides) {
  guide.items.sort((a, b) => String(a.priority).localeCompare(String(b.priority)) || String(a.item_id).localeCompare(String(b.item_id)));
  guide.counts = {
    items: guide.items.length,
    p0_items: guide.items.filter((item) => item.priority === "P0").length,
    awaiting_submission: guide.items.filter((item) => item.current_status === "awaiting_submission").length,
    valid_submission: guide.items.filter((item) => item.current_status === "valid_submission").length,
    invalid_submission: guide.items.filter((item) => item.current_status === "invalid_submission").length,
  };
}

const duplicateGuidePaths = duplicateValues(ownerGuides.map((guide) => guide.guide_path));
if (duplicateGuidePaths.length > 0) throw new Error(`duplicate owner guide paths: ${duplicateGuidePaths.join(", ")}`);

const output = {
  owner_guides_version: "v0.1",
  status: "owner_guides_ready_not_real_evidence",
  source_manifest_template_index_path: templateIndexPath,
  source_submission_status_path: statusPath,
  generated_from: [templateIndexPath, statusPath],
  policy: {
    owner_guides_do_not_close_blockers: true,
    template_only_not_production_evidence: true,
    credential_ref_only_no_plaintext_secret: true,
    real_submission_must_go_to_submissions_directory: true,
  },
  counts: {
    owner_guides: ownerGuides.length,
    items: ownerGuides.reduce((sum, guide) => sum + guide.items.length, 0),
    p0_items: ownerGuides.reduce((sum, guide) => sum + guide.counts.p0_items, 0),
    invalid_submission: ownerGuides.reduce((sum, guide) => sum + guide.counts.invalid_submission, 0),
  },
  owner_guides: ownerGuides.map((guide) => ({
    owner_slug: guide.owner_slug,
    owner_group: guide.owner_group,
    guide_path: guide.guide_path,
    status: guide.status,
    counts: guide.counts,
    items: guide.items,
  })),
};

const rows = output.owner_guides.map((guide) => ({
  owner_group: guide.owner_group,
  guide_path: guide.guide_path,
  items: guide.counts.items,
  p0_items: guide.counts.p0_items,
  awaiting_submission: guide.counts.awaiting_submission,
  valid_submission: guide.counts.valid_submission,
  invalid_submission: guide.counts.invalid_submission,
}));

const md = normalize(`# 生产证据责任组回填说明包 V0.1\n\n## 1. 机器标记\n\n- PRODUCTION_EVIDENCE_OWNER_GUIDES_READY\n- OWNER_GUIDES_FROM_MANIFEST_TEMPLATES\n- OWNER_GUIDES_NOT_REAL_PRODUCTION_EVIDENCE\n- NO_OWNER_GUIDE_CLOSES_BLOCKERS\n- VERIFY_PRODUCTION_EVIDENCE_OWNER_GUIDES_READY\n\n## 2. 定位\n\n本文从 \`${templateIndexPath}\` 和 \`${statusPath}\` 自动生成，把 manifest 模板和实际提交状态按责任组拆成可交付说明包。本文不创建真实证据，不关闭任何生产阻断项。\n\n## 3. 总览\n\n| 指标 | 数量 |\n|---|---:|\n| 责任组说明 | ${output.counts.owner_guides} |\n| 回填项 | ${output.counts.items} |\n| P0 回填项 | ${output.counts.p0_items} |\n| 无效提交 | ${output.counts.invalid_submission} |\n\n## 4. 责任组说明清单\n\n${mdTable(rows, [
  { key: "owner_group", label: "owner_group" },
  { key: "guide_path", label: "guide_path" },
  { key: "items", label: "items" },
  { key: "p0_items", label: "p0_items" },
  { key: "awaiting_submission", label: "awaiting_submission" },
  { key: "valid_submission", label: "valid_submission" },
  { key: "invalid_submission", label: "invalid_submission" },
])}\n## 5. 使用规则\n\n1. 将对应 \`guide_path\` 发给责任组。\n2. 责任组按说明复制 manifest 模板并填入真实证据链接。\n3. 每次提交后运行 \`npm run docs:production-evidence-submission-status\` 和 \`npm run verify:delivery-gates\`。\n4. 本说明包只做分派和指导，不得作为生产证据或关闭依据。\n`);

const json = normalize(JSON.stringify(output, null, 2));
forbidPlaintextSecrets(mdPath, md);
forbidPlaintextSecrets(jsonPath, json);
for (const guide of ownerGuides) forbidPlaintextSecrets(guide.guide_path, guideMd(guide));

function failWith(failures) {
  console.error("PRODUCTION_EVIDENCE_OWNER_GUIDES_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

if (checkMode) {
  const failures = [];
  const outputPaths = [jsonPath, mdPath, ...ownerGuides.map((guide) => guide.guide_path)];
  const missingOutputs = outputPaths.filter((relativePath) => !fs.existsSync(path.join(repoRoot, relativePath)));
  if (missingOutputs.length > 0) failWith(missingOutputs.map((relativePath) => `${relativePath} is missing; run npm run docs:production-evidence-owner-guides`));
  if (normalize(readText(jsonPath)) !== json) failures.push(`${jsonPath} is not in sync`);
  if (normalize(readText(mdPath)) !== md) failures.push(`${mdPath} is not in sync`);
  for (const guide of ownerGuides) {
    const expected = guideMd(guide);
    const actual = normalize(readText(guide.guide_path));
    if (actual !== expected) failures.push(`${guide.guide_path} is not in sync`);
  }
  if (output.counts.invalid_submission !== 0) failures.push("owner guides must not include invalid submissions in delivery-ready state");
  if (failures.length > 0) failWith(failures);
  console.log("PRODUCTION_EVIDENCE_OWNER_GUIDES_PASS");
} else {
  fs.mkdirSync(path.dirname(path.join(repoRoot, jsonPath)), { recursive: true });
  fs.writeFileSync(path.join(repoRoot, jsonPath), json, "utf8");
  fs.writeFileSync(path.join(repoRoot, mdPath), md, "utf8");
  for (const guide of ownerGuides) {
    const absolutePath = path.join(repoRoot, guide.guide_path);
    fs.mkdirSync(path.dirname(absolutePath), { recursive: true });
    fs.writeFileSync(absolutePath, guideMd(guide), "utf8");
  }
  console.log(`generated ${jsonPath}`);
  console.log(`generated ${mdPath}`);
  console.log(`generated ${ownerGuides.length} owner guide(s)`);
}