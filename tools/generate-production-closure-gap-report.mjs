import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const outputPath = "docs/production-evidence-closure-gap-report-v0.1.md";
const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");

function absolute(relativePath) {
  return path.join(repoRoot, relativePath);
}

function readText(relativePath) {
  return fs.readFileSync(absolute(relativePath), "utf8").replace(/^\uFEFF/, "");
}

function readJson(relativePath) {
  return JSON.parse(readText(relativePath));
}

function readOptionalJson(relativePath) {
  const fullPath = absolute(relativePath);
  if (!fs.existsSync(fullPath)) return null;
  return JSON.parse(fs.readFileSync(fullPath, "utf8").replace(/^\uFEFF/, ""));
}

function normalize(text) {
  return `${text.trimEnd()}\n`;
}

function cell(value) {
  if (Array.isArray(value)) return value.join(", ").replace(/\|/g, "\\|");
  return String(value ?? "").replace(/\|/g, "\\|").replace(/\n/g, "<br>");
}

function mdTable(rows, columns) {
  const header = `| ${columns.map((column) => column.title).join(" | ")} |`;
  const divider = `| ${columns.map((column) => column.align === "right" ? "---:" : "---").join(" | ")} |`;
  const body = rows.map((row) => `| ${columns.map((column) => cell(column.value(row))).join(" | ")} |`);
  return [header, divider, ...body].join("\n");
}

function countBy(rows, keyFn) {
  const map = new Map();
  for (const row of rows) {
    const key = keyFn(row) || "未分类";
    const current = map.get(key) ?? { key, total: 0, p0: 0, later: 0 };
    current.total += 1;
    if (row.priority === "P0") current.p0 += 1;
    else current.later += 1;
    map.set(key, current);
  }
  return [...map.values()].sort((a, b) => b.total - a.total || a.key.localeCompare(b.key, "zh-Hans-CN"));
}

function summarizeItems(items, limit = 6) {
  const list = items ?? [];
  if (list.length <= limit) return list.join(", ");
  return `${list.slice(0, limit).join(", ")} ...`;
}

function uniqueSorted(values) {
  return [...new Set(values.filter(Boolean))].sort((a, b) => a.localeCompare(b, "zh-Hans-CN"));
}

function render() {
  const statusPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
  const blockerPath = "docs/production-readiness-evidence/production-blocker-register-latest.json";
  const priorityPath = "docs/production-readiness-evidence/production-evidence-backfill-priority-latest.json";
  const ledgerPath = "docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.json";
  const nasDriftPath = "docs/production-readiness-evidence/nas-current-state-drift-latest.json";
  const runtimePath = "docs/runtime-acceptance-evidence-latest.json";
  const currentStatePath = "docs/closed-loop-current-state-snapshot-latest.json";
  const externalTracePath = "docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.json";
  const semgrepPath = "security-scans/semgrep-latest-summary.json";

  const status = readJson(statusPath);
  const blockers = readJson(blockerPath);
  const priority = readJson(priorityPath);
  const ledger = readJson(ledgerPath);
  const nasDrift = readJson(nasDriftPath);
  const runtime = readOptionalJson(runtimePath);
  const currentState = readOptionalJson(currentStatePath);
  const externalTrace = readOptionalJson(externalTracePath);
  const semgrep = readOptionalJson(semgrepPath);

  const counts = status.counts ?? {};
  const blockerCounts = blockers.counts ?? {};
  const priorityCounts = priority.counts ?? {};
  const ledgerCounts = ledger.counts ?? {};
  const submissions = status.submissions ?? [];
  const p0ManifestCount = (counts.p0_awaiting_submission ?? 0) + (counts.p0_valid_submission ?? 0) + (counts.p0_invalid_submission ?? 0);
  const p1ManifestCount = submissions.filter((row) => row.priority === "P1").length;
  const p2ManifestCount = submissions.filter((row) => row.priority === "P2").length;
  const generatedAt = runtime?.generated_at ?? currentState?.generated_at ?? new Date().toISOString();
  const semgrepResults = semgrep?.result_count ?? semgrep?.results ?? semgrep?.results_count ?? 0;
  const semgrepErrors = Array.isArray(semgrep?.errors) ? semgrep.errors.length : (semgrep?.error_count ?? semgrep?.errors ?? semgrep?.errors_count ?? 0);
  const semgrepScanId = semgrep?.scan_id ?? semgrep?.scan_dir ?? semgrep?.raw_result_file ?? semgrep?.latest_file ?? "\u672a\u8bb0\u5f55";
  const semgrepPaths = semgrep?.scanned_path_count ?? semgrep?.paths_scanned ?? semgrep?.paths_scanned_count ?? semgrep?.scanned_count ?? 0;
  const runtimeReady = runtime?.conclusion?.runtime_ready_for_integration_and_business_acceptance === true;
  const productionReady = currentState?.go_no_go?.can_claim_production_ready === true;
  const goNoGoBlockers = currentState?.go_no_go?.production_ready_blockers ?? [];
  const externalCounts = externalTrace?.counts ?? {};
  const nasAssessment = nasDrift.production_closure_assessment ?? {};
  const nasCurrent = nasDrift.current_state ?? {};

  const overviewRows = [
    ["生产前置台账阻断项", blockerCounts.readiness_blockers],
    ["生产凭证轮换阻断项", blockerCounts.credential_blockers],
    ["安全 findings open / open_for_prod_review", blockerCounts.finding_blockers],
    ["预期生产证据 manifest", counts.expected_submissions],
    ["P0 manifest", p0ManifestCount],
    ["P1 manifest", p1ManifestCount],
    ["P2 manifest", p2ManifestCount],
    ["awaiting_submission", counts.awaiting_submission],
    ["valid_submission", counts.valid_submission],
    ["invalid_submission", counts.invalid_submission],
    ["回填执行台账行数", ledgerCounts.ledger_rows],
    ["回填批次数", (priority.batches ?? []).length],
  ];

  const runtimeRows = [
    { item: "运行态支持联调/业务验收", value: runtimeReady ? "是" : "否", evidence: runtimePath },
    { item: "可以宣称生产上线就绪", value: productionReady ? "是" : "否", evidence: currentStatePath },
    { item: "后端测试", value: runtime?.backend_surefire?.tests ?? "未记录", evidence: "backend/target/surefire-reports" },
    { item: "Web 单元测试", value: runtime?.web_unit_source?.test_count_from_source ?? "未记录", evidence: runtimePath },
    { item: "Playwright UI", value: runtime?.playwright?.normal?.test_count_from_source ?? "未记录", evidence: "playwright-report/index.html" },
    { item: "真实后端 UI", value: `${runtime?.playwright?.real_backend?.test_count_from_source ?? "未记录"}${runtime?.playwright?.real_backend?.report_exists ? "" : "（报告缺失，未跑）"}`, evidence: "playwright-report-real-backend/index.html" },
    { item: "Semgrep", value: `results=${semgrepResults}, errors=${semgrepErrors}, paths_scanned=${semgrepPaths}`, evidence: semgrepScanId },
  ];

  const nasRows = [
    { item: "当前 live mount 状态", value: nasCurrent.live_status ?? nasDrift.status ?? "unknown", evidence: nasDriftPath },
    { item: "findmnt exit_code", value: nasCurrent.findmnt?.exit_code ?? "未记录", evidence: `${nasDrift.expected?.nas_unc ?? "//192.168.88.113/bn"} -> local mount ${nasDrift.expected?.mount_path ?? "/mnt/complex-nas-backup"}` },
    { item: "读写删除检查", value: nasCurrent.write_read_delete_check?.status ?? "未记录", evidence: nasCurrent.write_read_delete_check?.error ?? "" },
    { item: "可关闭 SEC-P0-OPS-RECOVERY-001", value: nasAssessment.can_close_sec_p0_ops_recovery ? "是" : "否", evidence: nasAssessment.reason ?? "" },
  ];

  const ownerRows = (priority.owner_summaries ?? []).map((row) => ({
    owner_group: row.owner_group,
    expected_submissions: row.expected_submissions,
    p0_expected: row.p0_expected,
    later: (row.expected_submissions ?? 0) - (row.p0_expected ?? 0),
    awaiting_total: row.awaiting_total ?? row.awaiting_submission,
  }));

  const categoryRows = countBy(submissions, (row) => row.category);
  const topBatches = (priority.batches ?? []).slice(0, 14).map((batch) => ({
    ...batch,
    item_ids_text: summarizeItems(batch.item_ids),
  }));
  const partialRequirements = (externalTrace?.requirement_traceability ?? []).filter((row) => row.production_blocking);
  const allOwnerGroups = uniqueSorted(submissions.map((row) => row.owner_group));

  return normalize(`# 生产闭环缺口总报告 V0.1

## 1. 机器标记

- PRODUCTION_CLOSURE_GAP_REPORT_READY
- GENERATED_FROM_PRODUCTION_BLOCKER_REGISTER
- GENERATED_FROM_PRODUCTION_EVIDENCE_SUBMISSION_STATUS
- GENERATED_FROM_RUNTIME_ACCEPTANCE_EVIDENCE
- GENERATED_FROM_NAS_CURRENT_STATE_DRIFT
- NO_FAKE_PRODUCTION_EVIDENCE_CLOSURE
- MANIFEST_FIRST_RAW_EVIDENCE_EXTERNAL
- P0_EVIDENCE_FIRST_CLOSURE_ORDER

## 2. 结论

截至 \`${generatedAt}\`，当前代码实现、接口契约、UI 自动化、真实后端 UI 和 Semgrep 扫描的证据状态见第 3、4 节；完整生产闭环仍未达成。阻断原因不是业务代码继续扩写，而是外部生产实施证据尚未提交：生产证据提交看板共有 **${counts.expected_submissions}** 个预期提交项，其中 **${p0ManifestCount}** 个为 P0，当前 **${counts.awaiting_submission}** 个仍为 \`awaiting_submission\`，有效提交为 **${counts.valid_submission}**。

本文用于给总控、实施负责人和各责任组看“还差什么才能把生产闭环真正关上”。本文不关闭任何阻断项；关闭必须通过对应 manifest、来源台账、findings register、NAS 当前状态和交付闸口复核。

## 3. 当前 go/no-go

| 判断项 | 当前值 | 证据 |
|---|---|---|
| 可进入联调 | ${currentState?.go_no_go?.can_enter_integration ? "是" : "否"} | \`${currentStatePath}\` |
| 可进入业务验收 | ${currentState?.go_no_go?.can_enter_business_acceptance ? "是" : "否"} | \`${currentStatePath}\` |
| 可宣称生产上线就绪 | ${productionReady ? "是" : "否"} | \`${currentStatePath}\` |

生产上线阻断：

${goNoGoBlockers.map((item) => `- ${item}`).join("\n") || "- 当前未发现生产上线阻断。"}

## 4. 当前运行态证据

${mdTable(runtimeRows, [
  { title: "项目", value: (row) => row.item },
  { title: "当前值", value: (row) => row.value },
  { title: "证据", value: (row) => row.evidence },
])}

## 5. NAS 当前状态

${mdTable(nasRows, [
  { title: "项目", value: (row) => row.item },
  { title: "当前值", value: (row) => row.value },
  { title: "证据/说明", value: (row) => row.evidence },
])}

## 6. 当前状态总览

${mdTable(overviewRows.map(([label, value]) => ({ label, value })), [
  { title: "指标", value: (row) => row.label },
  { title: "数量", align: "right", value: (row) => row.value },
])}

## 7. 按责任组收口

${mdTable(ownerRows, [
  { title: "责任组", value: (row) => row.owner_group },
  { title: "需提交 manifest", align: "right", value: (row) => row.expected_submissions },
  { title: "其中 P0", align: "right", value: (row) => row.p0_expected },
  { title: "P1/P2", align: "right", value: (row) => row.later },
  { title: "等待提交", align: "right", value: (row) => row.awaiting_total },
])}

当前责任组：${allOwnerGroups.join("、")}

## 8. 按类别收口

${mdTable(categoryRows, [
  { title: "类别", value: (row) => row.key },
  { title: "需提交 manifest", align: "right", value: (row) => row.total },
  { title: "其中 P0", align: "right", value: (row) => row.p0 },
  { title: "P1/P2", align: "right", value: (row) => row.later },
])}

## 9. 最先处理的回填批次

${mdTable(topBatches, [
  { title: "batch_id", value: (row) => row.batch_id },
  { title: "排序", align: "right", value: (row) => row.rank },
  { title: "责任组", value: (row) => row.owner_group },
  { title: "优先级", value: (row) => row.priority },
  { title: "类别", value: (row) => row.category },
  { title: "关联项", value: (row) => row.item_ids_text },
  { title: "推荐动作", value: (row) => row.recommended_action },
])}

## 10. 仍阻断生产的安全 findings

${mdTable(blockers.finding_blockers ?? [], [
  { title: "级别", value: (row) => row.severity },
  { title: "id", value: (row) => row.id },
  { title: "状态", value: (row) => row.status },
  { title: "责任人", value: (row) => row.owner },
  { title: "阻断范围", value: (row) => row.blocking_scope },
  { title: "下一步", value: (row) => row.next_action },
])}

## 11. 生产前置台账阻断项

${mdTable(blockers.readiness_blockers ?? [], [
  { title: "item_id", value: (row) => row.id },
  { title: "类别", value: (row) => row.category },
  { title: "状态", value: (row) => row.status },
  { title: "责任人", value: (row) => row.owner_department },
  { title: "阻断范围", value: (row) => row.blocking_scope },
  { title: "下一步", value: (row) => row.next_action },
])}

## 12. 生产凭证轮换阻断项

${mdTable(blockers.credential_blockers ?? [], [
  { title: "credential_item_id", value: (row) => row.id },
  { title: "类别", value: (row) => row.category },
  { title: "状态", value: (row) => row.status },
  { title: "责任人", value: (row) => row.owner_department },
  { title: "阻断范围", value: (row) => row.blocking_scope },
  { title: "下一步", value: (row) => row.next_action },
])}

## 13. 外部证据 partial 需求

| 指标 | 数量 |
|---|---:|
| partial_requirements | ${externalCounts.partial_requirements ?? 0} |
| production_blocking_requirements | ${externalCounts.production_blocking_requirements ?? 0} |
| mapped_evidence_items | ${externalCounts.mapped_evidence_items ?? 0} |
| awaiting_evidence_rows | ${externalCounts.awaiting_evidence_rows ?? 0} |

${mdTable(partialRequirements, [
  { title: "需求编号", value: (row) => row.requirement_id },
  { title: "模块", value: (row) => row.module },
  { title: "当前状态", value: (row) => row.current_status },
  { title: "映射证据项", value: (row) => summarizeItems(row.mapped_item_ids, 5) },
  { title: "责任组", value: (row) => summarizeItems(row.owner_groups, 5) },
])}

## 14. 执行规则

1. P0 优先顺序：MDM/设备纳管 -> 生产凭证与密钥轮换 -> 支付/退款/对账 -> NAS 自动挂载/定时备份/告警/周期演练 -> 160 生产运行配置 -> 人脸年龄核验。
2. 真实供应商未确定前，机器人、视频、设备厂商、支付、人脸、企业微信只能保持 mock/stub 或 awaiting，不得标记生产 passed。
3. 原始证据文件不得进入仓库；仓库只接收 \`manifest-<item_id>.json\`，证据通过外部受控链接或批准的证据编号引用。
4. 所有密钥、密码、token、API secret 只允许写 \`cred://...\` 引用，不得写明文。
5. 看板状态为 \`valid_submission\` 不自动关闭阻断项；还必须更新来源台账或 findings register，并复跑交付闸口。
6. \`npm run verify:delivery-gates\` 通过只证明当前代码、文档和证据格式没有破坏；不代表外部生产实施证据已经补齐。

## 15. 每批证据回填后的复测命令

\`\`\`bash
npm run verify:production-evidence-submissions-guard
npm run docs:production-evidence-submission-status
npm run verify:production-evidence-submission-status
npm run docs:production-evidence-backfill-priority
npm run verify:production-evidence-backfill-priority
npm run docs:production-evidence-backfill-execution-ledger
npm run verify:production-evidence-backfill-execution-ledger
npm run docs:production-blockers
npm run verify:production-blockers
npm run docs:production-closure-gap-reports
npm run verify:production-closure-gap-reports
npm run verify:production-readiness-evidence
npm run verify:delivery-gates
\`\`\`

## 16. 关联事实源

| 事实源 | 路径 |
|---|---|
| 当前闭环状态快照 | \`${currentStatePath}\` |
| 运行态验收证据 | \`${runtimePath}\` |
| Semgrep 最新摘要 | \`${semgrepPath}\` |
| NAS 当前状态漂移 | \`${nasDriftPath}\` |
| 生产阻断清单 | \`docs/production-blocker-register-v0.1.md\`、\`${blockerPath}\` |
| 生产证据提交状态 | \`docs/production-evidence-submission-status-v0.1.md\`、\`${statusPath}\` |
| 生产证据回填优先级 | \`docs/production-evidence-backfill-priority-v0.1.md\`、\`${priorityPath}\` |
| 生产证据执行台账 | \`docs/production-evidence-backfill-execution-ledger-v0.1.md\`、\`${ledgerPath}\`、\`docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.csv\` |
| 外部证据需求追踪 | \`${externalTracePath}\` |
| 生产证据 manifest 模板 | \`docs/production-evidence-manifest-templates-v0.1.md\`、\`docs/production-readiness-evidence/submission-manifest-templates/\` |
| 责任组回填说明 | \`docs/production-evidence-owner-guides-v0.1.md\`、\`docs/production-readiness-evidence/owner-guides/\` |
`);
}

const nextMarkdown = render();
if (checkMode) {
  const current = readText(outputPath);
  if (current !== nextMarkdown) {
    console.error("PRODUCTION_CLOSURE_GAP_REPORT_FAIL");
    console.error(`- ${outputPath} is not in sync with current production/runtime evidence sources`);
    process.exit(1);
  }
  console.log("PRODUCTION_CLOSURE_GAP_REPORT_PASS");
} else {
  fs.writeFileSync(absolute(outputPath), nextMarkdown, "utf8");
  console.log(`wrote ${outputPath}`);
}
