import fs from "node:fs";
import path from "node:path";
import { defaultSwitchState, disabledItemIds, disabledFindingIds, switchReasonsByItemId, switchReasonsByFindingId } from "./integration-capability-switches.mjs";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check");
const jsonPath = "docs/production-readiness-evidence/production-blocker-register-latest.json";
const mdPath = "docs/production-blocker-register-v0.1.md";

function readText(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), "utf8");
}

function normalize(content) {
  return content.replace(/\r\n/g, "\n").trimEnd() + "\n";
}

function stripMarkdown(value) {
  return String(value ?? "").replace(/`/g, "").trim();
}

function parseTableAfterHeading(markdown, heading) {
  const lines = markdown.split(/\r?\n/);
  const headingIndex = lines.findIndex((line) => line.trim() === heading);
  if (headingIndex < 0) throw new Error(`Heading not found: ${heading}`);
  const headerIndex = lines.findIndex((line, index) => index > headingIndex && line.trim().startsWith("|"));
  if (headerIndex < 0) throw new Error(`Table header not found after ${heading}`);
  const separatorIndex = headerIndex + 1;
  const header = splitTableRow(lines[headerIndex]);
  if (!/^\|\s*[-:]/.test(lines[separatorIndex] ?? "")) {
    throw new Error(`Table separator not found after ${heading}`);
  }
  const rows = [];
  for (let index = separatorIndex + 1; index < lines.length; index += 1) {
    const line = lines[index];
    if (!line.trim().startsWith("|")) break;
    const cells = splitTableRow(line);
    if (cells.length !== header.length) continue;
    rows.push(Object.fromEntries(header.map((key, cellIndex) => [key, cells[cellIndex]])));
  }
  return rows;
}

function splitTableRow(line) {
  return line
    .trim()
    .replace(/^\|/, "")
    .replace(/\|$/, "")
    .split("|")
    .map((cell) => cell.trim());
}

function toLedgerBlocker(row, sourceTable) {
  const id = row.item_id ?? row.credential_item_id;
  const status = row.status ?? row.rotation_status;
  const name = row.item_name ?? row.credential_type;
  return {
    id: stripMarkdown(id),
    category: stripMarkdown(row.category),
    name: stripMarkdown(name),
    status: stripMarkdown(status),
    owner_department: stripMarkdown(row.owner_department),
    supplier_name: stripMarkdown(row.supplier_name),
    blocking_scope: stripMarkdown(row.blocking_scope),
    runbook_ref: stripMarkdown(row.runbook_ref),
    next_action: stripMarkdown(row.next_action),
    source: "docs/production-readiness-implementation-ledger-template-v0.1.md",
    source_table: sourceTable,
  };
}

function toFindingBlocker(row) {
  return {
    severity: stripMarkdown(row.severity),
    id: stripMarkdown(row.id),
    title: stripMarkdown(row.title),
    status: stripMarkdown(row.status),
    owner: stripMarkdown(row.owner),
    blocking_scope: stripMarkdown(row.blocking_scope),
    next_action: stripMarkdown(row.next_action),
    source: "security-scans/findings-register-v1.md",
  };
}

const ledger = readText("docs/production-readiness-implementation-ledger-template-v0.1.md");
const findings = readText("security-scans/findings-register-v1.md");
const mainRows = parseTableAfterHeading(ledger, "## 4. 默认台账模板");
const credentialRows = parseTableAfterHeading(ledger, "## 4.1 生产凭证明细台账模板");
const findingRows = parseTableAfterHeading(findings, "## findings");

const switchState = defaultSwitchState();
const disabledEvidenceItemIds = disabledItemIds(switchState);
const disabledSecurityFindingIds = disabledFindingIds(switchState);
const disabledItemReasons = switchReasonsByItemId(switchState);
const disabledFindingReasons = switchReasonsByFindingId(switchState);

function isDisabledBySwitch(row) {
  return disabledEvidenceItemIds.has(row.id);
}

function isFindingDisabledBySwitch(row) {
  return disabledSecurityFindingIds.has(row.id);
}

function withSwitchReason(row) {
  const reason = (disabledItemReasons.get(row.id) ?? []).join("；");
  return {
    ...row,
    status: "switch_disabled",
    blocking_scope: "对应集成能力开关关闭；不要求真实外部接口或生产证据，但对应自动能力不得启用",
    next_action: reason || row.next_action,
  };
}

function withFindingSwitchReason(row) {
  const reason = (disabledFindingReasons.get(row.id) ?? []).join("；");
  return {
    ...row,
    status: "switch_disabled",
    blocking_scope: "对应集成能力开关关闭；不阻塞当前生产闭口，但开启能力前必须复核",
    next_action: reason || row.next_action,
  };
}

const ledgerBlockerStatuses = new Set(["pending_info", "blocked", "failed"]);
const ledgerDeferredStatuses = new Set(["not_started", "ready_for_test", "in_progress"]);
const allLedgerBlockers = mainRows
  .filter((row) => ledgerBlockerStatuses.has(stripMarkdown(row.status)))
  .map((row) => toLedgerBlocker(row, "default_readiness"));
const allCredentialBlockers = credentialRows
  .filter((row) => ledgerBlockerStatuses.has(stripMarkdown(row.rotation_status)))
  .map((row) => toLedgerBlocker(row, "credential_rotation"));
const allFindingBlockers = findingRows
  .filter((row) => ["open", "open_for_prod_review"].includes(stripMarkdown(row.status)))
  .map(toFindingBlocker);
const switchDisabledLedgerItems = allLedgerBlockers.filter(isDisabledBySwitch).map(withSwitchReason);
const switchDisabledCredentialItems = allCredentialBlockers.filter(isDisabledBySwitch).map(withSwitchReason);
const switchDisabledFindingItems = allFindingBlockers.filter(isFindingDisabledBySwitch).map(withFindingSwitchReason);
const ledgerBlockers = allLedgerBlockers.filter((row) => !isDisabledBySwitch(row));
const credentialBlockers = allCredentialBlockers.filter((row) => !isDisabledBySwitch(row));
const deferredItems = [
  ...mainRows
    .filter((row) => ledgerDeferredStatuses.has(stripMarkdown(row.status)))
    .map((row) => toLedgerBlocker(row, "default_readiness")),
  ...switchDisabledLedgerItems,
  ...switchDisabledCredentialItems,
];
const findingBlockers = allFindingBlockers.filter((row) => !isFindingDisabledBySwitch(row));
const acceptedDevSmokeFindings = findingRows
  .filter((row) => stripMarkdown(row.status) === "accepted_for_dev_smoke")
  .map(toFindingBlocker);

const register = {
  register_version: "v0.1",
  status: "open_blockers_present",
  generated_from: [
    "docs/production-readiness-implementation-ledger-template-v0.1.md",
    "security-scans/findings-register-v1.md",
  ],
  policy: {
    no_fake_closure: true,
    no_plaintext_secrets: true,
    no_supplier_mock_as_production_evidence: true,
    current_domain_https_binding_deferred: true,
    integration_capability_switches_applied: true,
  },
  counts: {
    readiness_blockers: ledgerBlockers.length,
    credential_blockers: credentialBlockers.length,
    finding_blockers: findingBlockers.length,
    deferred_items: deferredItems.length,
    accepted_dev_smoke_findings: acceptedDevSmokeFindings.length,
    switch_disabled_items: switchDisabledLedgerItems.length + switchDisabledCredentialItems.length + switchDisabledFindingItems.length,
  },
  readiness_blockers: ledgerBlockers,
  credential_blockers: credentialBlockers,
  finding_blockers: findingBlockers,
  deferred_items: deferredItems,
  accepted_dev_smoke_findings: acceptedDevSmokeFindings,
  switch_disabled_items: [...switchDisabledLedgerItems, ...switchDisabledCredentialItems, ...switchDisabledFindingItems],
};

function mdTable(rows, columns) {
  if (rows.length === 0) return "无。\n";
  const header = `| ${columns.map((column) => column.label).join(" | ")} |`;
  const separator = `| ${columns.map(() => "---").join(" | ")} |`;
  const body = rows.map((row) => `| ${columns.map((column) => String(row[column.key] ?? "").replace(/\|/g, "/")).join(" | ")} |`);
  return [header, separator, ...body].join("\n") + "\n";
}

const md = normalize(`# 生产阻断清单 V0.1

## 1. 机器标记

- PRODUCTION_BLOCKER_REGISTER_READY
- PRODUCTION_BLOCKERS_OPEN
- NO_FAKE_PRODUCTION_EVIDENCE_CLOSURE
- NO_SUPPLIER_MOCK_AS_PRODUCTION_EVIDENCE
- VERIFY_PRODUCTION_BLOCKERS_READY
- INTEGRATION_CAPABILITY_SWITCHES_APPLIED

## 2. 结论

当前仍存在必须由 IT、采购、财务、运营、安全或供应商回填的生产阻断项。本文已应用 docs/integration-capability-switches-latest.json：开关关闭的外部集成不再要求真实接口或生产证据，但对应自动能力不得启用。本文不重新打开业务决策，不允许把 mock、口头确认、截图占位或未纳管设备当作生产证据。

| 指标 | 数量 |
|---|---:|
| 生产前置台账阻断项 | ${register.counts.readiness_blockers} |
| 生产凭证轮换阻断项 | ${register.counts.credential_blockers} |
| 安全 findings open / open_for_prod_review | ${register.counts.finding_blockers} |
| 已暂缓或后续 review 项 | ${register.counts.deferred_items + register.counts.accepted_dev_smoke_findings} |

## 3. 生产前置台账阻断项

${mdTable(register.readiness_blockers, [
  { key: "id", label: "item_id" },
  { key: "category", label: "category" },
  { key: "status", label: "status" },
  { key: "blocking_scope", label: "blocking_scope" },
  { key: "owner_department", label: "owner" },
  { key: "next_action", label: "next_action" },
])}
## 4. 生产凭证轮换阻断项

${mdTable(register.credential_blockers, [
  { key: "id", label: "credential_item_id" },
  { key: "category", label: "category" },
  { key: "status", label: "status" },
  { key: "blocking_scope", label: "blocking_scope" },
  { key: "owner_department", label: "owner" },
  { key: "next_action", label: "next_action" },
])}
## 5. 安全 findings 阻断项

${mdTable(register.finding_blockers, [
  { key: "severity", label: "severity" },
  { key: "id", label: "id" },
  { key: "status", label: "status" },
  { key: "blocking_scope", label: "blocking_scope" },
  { key: "owner", label: "owner" },
  { key: "next_action", label: "next_action" },
])}
## 6. 暂缓、开关关闭或生产方案定稿复核项

${mdTable([...register.deferred_items, ...register.accepted_dev_smoke_findings], [
  { key: "id", label: "id" },
  { key: "status", label: "status" },
  { key: "blocking_scope", label: "blocking_scope" },
  { key: "next_action", label: "next_action" },
])}
## 7. 使用规则

1. 每次 IT、采购、财务、运营、安全或供应商补齐证据后，先更新生产前置台账或 findings register。
2. 执行 \`npm run docs:production-blockers\` 重新生成本文和 JSON。
3. 执行 \`npm run verify:production-blockers\`、\`npm run verify:production-readiness-evidence\`、\`npm run verify:delivery-gates\`。
4. 不得手工把本文中的阻断项删掉；必须从来源台账或 findings 关闭。
`);

const json = normalize(JSON.stringify(register, null, 2));

if (checkMode) {
  const currentJson = normalize(readText(jsonPath));
  const currentMd = normalize(readText(mdPath));
  const failures = [];
  if (currentJson !== json) failures.push(`${jsonPath} is not in sync`);
  if (currentMd !== md) failures.push(`${mdPath} is not in sync`);
  if (failures.length > 0) {
    console.error("PRODUCTION_BLOCKER_REGISTER_FAIL");
    for (const failure of failures) console.error(`- ${failure}`);
    process.exit(1);
  }
  console.log("PRODUCTION_BLOCKER_REGISTER_PASS");
} else {
  fs.mkdirSync(path.dirname(path.join(repoRoot, jsonPath)), { recursive: true });
  fs.writeFileSync(path.join(repoRoot, jsonPath), json, "utf8");
  fs.writeFileSync(path.join(repoRoot, mdPath), md, "utf8");
  console.log(`generated ${jsonPath}`);
  console.log(`generated ${mdPath}`);
}