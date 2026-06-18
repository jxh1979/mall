import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check");
const kitPath = "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json";
const jsonPath = "docs/production-readiness-evidence/production-evidence-manifest-template-index-latest.json";
const mdPath = "docs/production-evidence-manifest-templates-v0.1.md";
const templateRoot = "docs/production-readiness-evidence/submission-manifest-templates";

function readText(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), "utf8");
}

function readJson(relativePath) {
  return JSON.parse(readText(relativePath));
}

function normalize(content) {
  return content.replace(/\r\n/g, "\n").trimEnd() + "\n";
}

function unique(values) {
  return [...new Set(values.filter(Boolean))];
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

function ownerSlugFromPacketFolder(packetFolder) {
  const match = String(packetFolder).match(/submissions\/([^/]+)\//);
  return match?.[1] ?? "owner";
}

function placeholderCredentialRefs(template) {
  if (!template.required_fields?.includes("credential_refs")) return undefined;
  const category = String(template.category ?? "general");
  return [`cred://prod/${category}/${template.item_id}/replace-with-real-reference`];
}

function templateManifest(template) {
  const manifest = {
    template_notice: "TEMPLATE_ONLY_NOT_PRODUCTION_EVIDENCE",
    submission_id: template.submission_id,
    item_id: template.item_id,
    owner_group: template.owner_group,
    owner_person: "待填写责任人姓名",
    submitted_at: "YYYY-MM-DDTHH:mm:ssZ",
    evidence_ids: [`EVD-${template.item_id}-YYYYMMDD-001`],
    evidence_links: ["待填写工单/脱敏导出/供应商回执/验收报告链接"],
    evidence_types: template.accepted_evidence_types?.slice(0, 2) ?? ["test_report"],
    last_test_time: "YYYY-MM-DDTHH:mm:ssZ",
    result: "blocked",
    reviewer: "待填写复核人",
    redaction_confirmed: false,
    credential_refs: placeholderCredentialRefs(template),
    rollback_step: template.required_fields?.includes("rollback_step") ? "待填写回滚步骤；不得写密钥正文" : undefined,
    next_retest_due: template.required_fields?.includes("next_retest_due") ? "YYYY-MM-DD" : undefined,
    failure_case_evidence: template.required_fields?.includes("failure_case_evidence") ? ["待填写失败场景证据链接"] : undefined,
    approval_record: template.required_fields?.includes("approval_record") ? "待填写审批/验收记录链接" : undefined,
    notes: "复制到真实 submissions 目录前，必须替换占位字段；真实密钥只允许写 credential_ref。",
  };
  return Object.fromEntries(Object.entries(manifest).filter(([, value]) => value !== undefined));
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

const kit = readJson(kitPath);
const templates = (kit.submissions ?? []).map((submission) => {
  const ownerSlug = ownerSlugFromPacketFolder(submission.packet_folder);
  const relativePath = `${templateRoot}/${ownerSlug}/template-${submission.item_id}.json`;
  const manifest = templateManifest(submission);
  return {
    template_id: `TPL-${submission.submission_id}`,
    submission_id: submission.submission_id,
    owner_group: submission.owner_group,
    item_id: submission.item_id,
    priority: submission.priority,
    category: submission.category,
    template_path: relativePath,
    target_manifest_path: `${submission.packet_folder}${submission.manifest_file_name}`,
    result: manifest.result,
    template_notice: manifest.template_notice,
    manifest,
  };
});

const duplicateTemplatePaths = duplicateValues(templates.map((template) => template.template_path));
const duplicateTemplateIds = duplicateValues(templates.map((template) => template.template_id));
if (duplicateTemplatePaths.length > 0 || duplicateTemplateIds.length > 0) {
  throw new Error(`duplicate manifest templates: ids=${duplicateTemplateIds.join(", ")}; paths=${duplicateTemplatePaths.join(", ")}`);
}

const output = {
  template_index_version: "v0.1",
  status: "templates_ready_not_real_evidence",
  source_submission_kit_path: kitPath,
  generated_from: [kitPath],
  policy: {
    template_only_not_production_evidence: true,
    do_not_place_templates_under_real_submissions_without_replacing_placeholders: true,
    credential_ref_only_no_plaintext_secret: true,
    default_result_is_blocked: true,
  },
  counts: {
    templates: templates.length,
    p0_templates: templates.filter((template) => template.priority === "P0").length,
    owner_groups: unique(templates.map((template) => template.owner_group)).length,
  },
  templates: templates.map(({ manifest, ...summary }) => summary),
};

const rows = output.templates.map((template) => ({
  template_id: template.template_id,
  owner_group: template.owner_group,
  item_id: template.item_id,
  priority: template.priority,
  category: template.category,
  result: template.result,
  template_path: template.template_path,
  target_manifest_path: template.target_manifest_path,
}));

const md = normalize(`# 生产证据 Manifest 模板 V0.1\n\n## 1. 机器标记\n\n- PRODUCTION_EVIDENCE_MANIFEST_TEMPLATES_READY\n- TEMPLATE_ONLY_NOT_PRODUCTION_EVIDENCE\n- DEFAULT_RESULT_BLOCKED_UNTIL_REAL_EVIDENCE\n- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET\n- VERIFY_PRODUCTION_EVIDENCE_MANIFEST_TEMPLATES_READY\n\n## 2. 定位\n\n本文和配套 JSON 模板从 \`${kitPath}\` 自动生成，供 IT、财务、采购、运营、法务和技术负责人复制填写。模板不是生产证据，默认 \`result=blocked\`，\`redaction_confirmed=false\`；复制到真实 submissions 目录前必须替换责任人、证据链接、测试时间、复核人、credential_ref 和回滚/复测字段。\n\n## 3. 总览\n\n| 指标 | 数量 |\n|---|---:|\n| manifest 模板 | ${output.counts.templates} |\n| P0 manifest 模板 | ${output.counts.p0_templates} |\n| 责任组 | ${output.counts.owner_groups} |\n\n## 4. 模板清单\n\n${mdTable(rows, [
  { key: "template_id", label: "template_id" },
  { key: "owner_group", label: "owner_group" },
  { key: "item_id", label: "item_id" },
  { key: "priority", label: "priority" },
  { key: "category", label: "category" },
  { key: "result", label: "result" },
  { key: "template_path", label: "template_path" },
  { key: "target_manifest_path", label: "target_manifest_path" },
])}\n## 5. 使用规则\n\n1. 从 \`submission-manifest-templates/<owner>/template-<item_id>.json\` 复制模板。\n2. 放到 \`production-evidence-submission-kit-v0.1.md\` 指定的真实 \`packet_folder\`，文件名保持 \`manifest-<item_id>.json\`。\n3. 替换所有占位内容，并将 \`template_notice\` 删除或保留为非证据说明。\n4. 真实证据只允许写 \`credential_ref\`，不得写明文密码、token、API secret 或私钥。\n5. 填完后运行 \`npm run docs:production-evidence-submission-status\` 和 \`npm run verify:delivery-gates\`。\n`);

const json = normalize(JSON.stringify(output, null, 2));
forbidPlaintextSecrets(mdPath, md);
forbidPlaintextSecrets(jsonPath, json);
for (const template of templates) forbidPlaintextSecrets(template.template_path, normalize(JSON.stringify(template.manifest, null, 2)));

function failWith(failures) {
  console.error("PRODUCTION_EVIDENCE_MANIFEST_TEMPLATES_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

function writeOutputs() {
  fs.mkdirSync(path.dirname(path.join(repoRoot, jsonPath)), { recursive: true });
  fs.writeFileSync(path.join(repoRoot, jsonPath), json, "utf8");
  fs.writeFileSync(path.join(repoRoot, mdPath), md, "utf8");
  for (const template of templates) {
    const absolutePath = path.join(repoRoot, template.template_path);
    fs.mkdirSync(path.dirname(absolutePath), { recursive: true });
    fs.writeFileSync(absolutePath, normalize(JSON.stringify(template.manifest, null, 2)), "utf8");
  }
}

if (checkMode) {
  const failures = [];
  const outputPaths = [jsonPath, mdPath, ...templates.map((template) => template.template_path)];
  const missingOutputs = outputPaths.filter((relativePath) => !fs.existsSync(path.join(repoRoot, relativePath)));
  if (missingOutputs.length > 0) failWith(missingOutputs.map((relativePath) => `${relativePath} is missing; run npm run docs:production-evidence-manifest-templates`));
  if (normalize(readText(jsonPath)) !== json) failures.push(`${jsonPath} is not in sync`);
  if (normalize(readText(mdPath)) !== md) failures.push(`${mdPath} is not in sync`);
  for (const template of templates) {
    const expected = normalize(JSON.stringify(template.manifest, null, 2));
    const actual = normalize(readText(template.template_path));
    if (actual !== expected) failures.push(`${template.template_path} is not in sync`);
    if (template.manifest.result !== "blocked") failures.push(`${template.template_path} must default result to blocked`);
    if (template.manifest.redaction_confirmed !== false) failures.push(`${template.template_path} must default redaction_confirmed to false`);
  }
  if (failures.length > 0) failWith(failures);
  console.log("PRODUCTION_EVIDENCE_MANIFEST_TEMPLATES_PASS");
} else {
  writeOutputs();
  console.log(`generated ${jsonPath}`);
  console.log(`generated ${mdPath}`);
  console.log(`generated ${templates.length} manifest template(s)`);
}
