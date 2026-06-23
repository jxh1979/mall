import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

const repoRoot = process.cwd();
const markdownPath = "docs/current-staged-handoff-manifest-v0.1.md";
const jsonPath = "docs/current-staged-handoff-manifest-latest.json";
const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");

const criticalFiles = [
  "backend/src/main/java/com/complex/platform/bar/BarEntryService.java",
  "backend/src/main/java/com/complex/platform/bar/BarOrderService.java",
  "backend/src/main/java/com/complex/platform/bar/BarTableSessionService.java",
  "backend/src/main/java/com/complex/platform/bar/BarBillingService.java",
  "backend/src/main/java/com/complex/platform/ticketing/TicketPricingService.java",
  "backend/src/main/java/com/complex/platform/ticketing/TicketOrderService.java",
  "backend/src/main/java/com/complex/platform/ticketing/TicketVerificationService.java",
  "backend/src/main/java/com/complex/platform/ticketing/TicketCatalogService.java",
  "backend/src/main/resources/db/migration/V041__ticket_order_dynamic_pricing_trace.sql",
  "apps/web/src/admin/AdminApp.tsx",
  "apps/web/src/admin/security/PermissionGate.test.ts",
  "tests/e2e/full-admin-mvp.spec.ts",
  "tests/e2e/full-admin-remaining.spec.ts",
  "tests/e2e/web-ui-real-backend.spec.ts",
  "playwright.real-backend.config.ts",
  "tools/run-real-backend-ui-e2e.mjs",
  "tools/generate-production-closure-gap-report.mjs",
  "security-scans/semgrep-latest-summary.json",
  "docs/runtime-acceptance-evidence-latest.json",
  "docs/closed-loop-current-state-snapshot-latest.json",
  "docs/production-evidence-closure-gap-report-v0.1.md",
];

function runGit(args) {
  try {
    return execFileSync("git", args, { cwd: repoRoot, encoding: "utf8" }).trimEnd();
  } catch (error) {
    const stdout = error.stdout ? String(error.stdout) : "";
    const stderr = error.stderr ? String(error.stderr) : "";
    throw new Error(`git ${args.join(" ")} failed: ${stdout}${stderr}`);
  }
}

function absolute(relativePath) {
  return path.join(repoRoot, relativePath);
}

function readText(relativePath) {
  return fs.readFileSync(absolute(relativePath), "utf8").replace(/^\uFEFF/, "");
}

function writeText(relativePath, text) {
  fs.writeFileSync(absolute(relativePath), text, "utf8");
}

function parseNameStatus(text) {
  if (!text.trim()) return [];
  return text.split("\n").filter(Boolean).map((line) => {
    const parts = line.split("\t");
    return { status: parts[0], path: parts[parts.length - 1], raw: line };
  });
}

function parseStatus(text) {
  if (!text.trim()) return [];
  return text.split("\n").filter(Boolean).map((line) => ({
    index: line.slice(0, 1),
    worktree: line.slice(1, 2),
    path: line.slice(3),
    raw: line,
  }));
}

function countBy(items, keyFn) {
  const counts = new Map();
  for (const item of items) {
    const key = keyFn(item) || "unknown";
    counts.set(key, (counts.get(key) ?? 0) + 1);
  }
  return Object.fromEntries([...counts.entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0])));
}

function shortList(items, limit = 80) {
  if (items.length <= limit) return items;
  return [...items.slice(0, limit), `... ${items.length - limit} more`];
}

function mdTable(rows, columns) {
  const clean = (value) => String(value ?? "").replace(/\|/g, "\\|").replace(/\n/g, "<br>");
  return [
    `| ${columns.map((column) => column.title).join(" | ")} |`,
    `| ${columns.map((column) => column.align === "right" ? "---:" : "---").join(" | ")} |`,
    ...rows.map((row) => `| ${columns.map((column) => clean(column.value(row))).join(" | ")} |`),
  ].join("\n");
}

function buildState() {
  const statusText = runGit(["status", "--short"]);
  const nameStatusText = runGit(["diff", "--cached", "--name-status"]);
  const nameOnlyText = runGit(["diff", "--cached", "--name-only"]);
  const diffStatText = runGit([
    "diff",
    "--cached",
    "--stat",
    "--",
    ".",
    ":(exclude)docs/current-staged-handoff-manifest-v0.1.md",
    ":(exclude)docs/current-staged-handoff-manifest-latest.json",
  ]);
  const statusRows = parseStatus(statusText);
  const stagedRows = parseNameStatus(nameStatusText);
  const stagedFiles = nameOnlyText.trim() ? nameOnlyText.split("\n").filter(Boolean) : [];
  const stagedSet = new Set(stagedFiles);
  const untracked = statusRows.filter((row) => row.raw.startsWith("??")).map((row) => row.path);
  const unstagedOrMixed = statusRows.filter((row) => !row.raw.startsWith("??") && row.worktree !== " ").map((row) => row.raw);
  const missingCriticalFiles = criticalFiles.filter((file) => !stagedSet.has(file));
  const sha256 = crypto.createHash("sha256").update(nameStatusText).digest("hex");
  const statusCounts = countBy(stagedRows, (row) => row.status);
  const topLevelCounts = countBy(stagedFiles, (file) => file.split("/")[0] || file);
  const criticalStatus = criticalFiles.map((file) => ({ file, staged: stagedSet.has(file) }));
  const ready = stagedFiles.length > 0 && untracked.length === 0 && unstagedOrMixed.length === 0 && missingCriticalFiles.length === 0;
  return {
    manifest_version: "v0.1",
    generated_at: "generated-from-current-git-index",
    git_head: runGit(["rev-parse", "--short", "HEAD"]),
    branch: runGit(["branch", "--show-current"]),
    markers: [
      "STAGED_HANDOFF_MANIFEST_READY",
      "NO_UNTRACKED_CORE_FILES",
      "NO_MIXED_STAGED_WORKTREE",
      "CRITICAL_SPLIT_SERVICES_INCLUDED",
      "PATCH_PACKAGE_INPUTS_TRACEABLE",
    ],
    policy: {
      all_formal_code_changes_are_on_160: true,
      untracked_files_block_handoff: true,
      unstaged_worktree_changes_block_handoff: true,
      critical_split_services_must_be_in_index: true,
      this_manifest_does_not_claim_production_ready: true,
    },
    status: ready ? "ready_for_commit_or_patch_package" : "not_ready_for_commit_or_patch_package",
    counts: {
      staged_files: stagedFiles.length,
      staged_added: statusCounts.A ?? 0,
      staged_modified: statusCounts.M ?? 0,
      staged_deleted: statusCounts.D ?? 0,
      untracked_files: untracked.length,
      unstaged_or_mixed_entries: unstagedOrMixed.length,
      missing_critical_files: missingCriticalFiles.length,
    },
    staged_status_counts: statusCounts,
    top_level_counts: topLevelCounts,
    staged_name_status_sha256: sha256,
    critical_files: criticalStatus,
    missing_critical_files: missingCriticalFiles,
    untracked,
    unstaged_or_mixed: unstagedOrMixed,
    staged_files: stagedRows,
    diff_stat: diffStatText,
  };
}

function renderMarkdown(state) {
  const criticalRows = state.critical_files.map((row) => ({ ...row, status: row.staged ? "已纳入索引" : "缺失" }));
  const topRows = Object.entries(state.top_level_counts).map(([name, count]) => ({ name, count }));
  const statusRows = Object.entries(state.staged_status_counts).map(([status, count]) => ({ status, count }));
  const fileRows = shortList(state.staged_files, 160).map((row) => typeof row === "string" ? { status: "", path: row } : row);
  return `# 当前暂存交接清单 V0.1

> 本文由 \`tools/generate-staged-handoff-manifest.mjs\` 从 160 机器当前 Git 索引生成，用于防止提交或补丁包交接时漏掉新增核心文件。它不宣称生产上线就绪。

机器标记：

${state.markers.map((marker) => `- ${marker}`).join("\n")}

## 1. 结论

| 项目 | 当前值 |
|---|---|
| 状态 | ${state.status} |
| 分支 | ${state.branch} |
| HEAD | ${state.git_head} |
| 暂存文件数 | ${state.counts.staged_files} |
| 未跟踪文件 | ${state.counts.untracked_files} |
| 混合/未暂存变更 | ${state.counts.unstaged_or_mixed_entries} |
| 缺失关键文件 | ${state.counts.missing_critical_files} |
| name-status 指纹 | \`${state.staged_name_status_sha256}\` |

## 2. 状态计数

${mdTable(statusRows, [
  { title: "Git 状态", value: (row) => row.status },
  { title: "数量", align: "right", value: (row) => row.count },
])}

## 3. 顶层目录计数

${mdTable(topRows, [
  { title: "目录", value: (row) => row.name },
  { title: "暂存文件数", align: "right", value: (row) => row.count },
])}

## 4. 关键文件纳入检查

${mdTable(criticalRows, [
  { title: "关键文件", value: (row) => row.file },
  { title: "状态", value: (row) => row.status },
])}

## 5. 暂存文件样例

${mdTable(fileRows, [
  { title: "状态", value: (row) => row.status },
  { title: "路径", value: (row) => row.path },
])}

## 6. 交接规则

1. 交接前必须运行 \`npm run verify:staged-handoff-manifest\`。
2. 不允许存在 \`??\` 未跟踪文件；新增服务类、迁移、Playwright 测试、运行证据和安全扫描必须进入索引。
3. 不允许存在混合状态；同一文件如果还有未暂存修改，必须重新生成清单并暂存。
4. 本清单只证明补丁包输入完整，不替代 \`npm run verify:delivery-gates\`、\`npm run verify:closed-loop-runtime\`、Semgrep 或生产证据回填。
`;
}

function validate(state) {
  const failures = [];
  if (state.counts.staged_files === 0) failures.push("no staged files are available for handoff");
  if (state.counts.untracked_files > 0) failures.push(`untracked files must be staged or removed: ${state.untracked.join(", ")}`);
  if (state.counts.unstaged_or_mixed_entries > 0) failures.push(`unstaged or mixed entries must be resolved: ${state.unstaged_or_mixed.join(", ")}`);
  if (state.counts.missing_critical_files > 0) failures.push(`critical files missing from index: ${state.missing_critical_files.join(", ")}`);
  return failures;
}

const state = buildState();
const markdown = renderMarkdown(state);
const json = `${JSON.stringify(state, null, 2)}\n`;
const failures = validate(state);

if (checkMode) {
  if (readText(markdownPath) !== markdown) failures.push(`${markdownPath} is not in sync with current staged index`);
  if (readText(jsonPath) !== json) failures.push(`${jsonPath} is not in sync with current staged index`);
  if (failures.length > 0) {
    console.error("STAGED_HANDOFF_MANIFEST_FAIL");
    for (const failure of failures) console.error(`- ${failure}`);
    process.exit(1);
  }
  console.log("STAGED_HANDOFF_MANIFEST_PASS");
} else {
  writeText(markdownPath, markdown);
  writeText(jsonPath, json);
  if (failures.length > 0) {
    console.error("STAGED_HANDOFF_MANIFEST_WARN");
    for (const failure of failures) console.error(`- ${failure}`);
  }
  console.log(`wrote ${markdownPath}`);
  console.log(`wrote ${jsonPath}`);
}
