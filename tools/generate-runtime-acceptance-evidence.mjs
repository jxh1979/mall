import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

const repoRoot = process.cwd();
const markdownPath = "docs/runtime-acceptance-evidence-v0.1.md";
const jsonPath = "docs/runtime-acceptance-evidence-latest.json";
const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");

// AUD-021: 证据必须绑定到"生成它时的源代码内容"和"生成时间"，否则两周前或另一提交
// 生成的陈旧证据会一直 PASS，为当前代码背书。下列 source paths 覆盖运行验收真正依赖的输入
// （对齐 required_refresh_rule：后端、Web、Playwright、运行脚本改动）。证据文档本身不在其中，
// 因此把再生成的证据一起提交不会自我失配。
const SOURCE_FINGERPRINT_PATHS = [
  "backend/src/main",
  "backend/src/test",
  "backend/pom.xml",
  "apps/web/src",
  "apps/web/package.json",
  "apps/web/vite.config.ts",
  "apps/web/tsconfig*.json",
  "tsconfig.base.json",
  "tests/e2e",
  "docs/openapi/openapi-p0.yaml",
  "package.json",
  "playwright.config.ts",
  "playwright.real-backend.config.ts",
  "tools/run-real-backend-ui-e2e.mjs",
  "tools/run-backend-maven.mjs",
];
// 证据超过此天数即视为陈旧失败（即使源码未变，也应周期性重跑运行验收）。可用环境变量覆盖。
const MAX_AGE_DAYS = Number(process.env.RUNTIME_EVIDENCE_MAX_AGE_DAYS ?? 30);

function absolute(relativePath) {
  return path.join(repoRoot, relativePath);
}

function exists(relativePath) {
  return fs.existsSync(absolute(relativePath));
}

function readText(relativePath) {
  return fs.readFileSync(absolute(relativePath), "utf8").replace(/^﻿/, "");
}

function readJson(relativePath) {
  return JSON.parse(readText(relativePath));
}

function runGit(args) {
  try {
    return execFileSync("git", args, { cwd: repoRoot, encoding: "utf8", maxBuffer: 64 * 1024 * 1024 }).trimEnd();
  } catch (error) {
    const stdout = error.stdout ? String(error.stdout) : "";
    const stderr = error.stderr ? String(error.stderr) : "";
    throw new Error(`git ${args.join(" ")} failed: ${stdout}${stderr}`);
  }
}

// 对当前工作树中所有被跟踪的源文件内容做 sha256（含新增/删除/改名/内容改动）。
// 用工作树内容（而非仅 index blob），以捕获尚未 staged 的源码改动。
function computeSourceFingerprint() {
  const listing = runGit(["ls-files", "-z", "--", ...SOURCE_FINGERPRINT_PATHS]);
  const files = listing.split("\0").filter(Boolean).sort();
  const hash = crypto.createHash("sha256");
  for (const file of files) {
    hash.update(file);
    hash.update("\0");
    const full = absolute(file);
    if (fs.existsSync(full) && fs.statSync(full).isFile()) {
      hash.update(fs.readFileSync(full));
    } else {
      hash.update("<missing>");
    }
    hash.update("\0");
  }
  return { fingerprint: hash.digest("hex"), file_count: files.length };
}

function gitBinding() {
  const source = computeSourceFingerprint();
  return {
    head: runGit(["rev-parse", "HEAD"]),
    head_short: runGit(["rev-parse", "--short", "HEAD"]),
    branch: runGit(["rev-parse", "--abbrev-ref", "HEAD"]),
    worktree_dirty: runGit(["status", "--porcelain"]).length > 0,
    source_fingerprint: source.fingerprint,
    source_fingerprint_file_count: source.file_count,
    source_fingerprint_paths: SOURCE_FINGERPRINT_PATHS,
    max_age_days: MAX_AGE_DAYS,
  };
}

function statInfo(relativePath) {
  if (!exists(relativePath)) return null;
  const stat = fs.statSync(absolute(relativePath));
  return {
    path: relativePath,
    size_bytes: stat.size,
    mtime: stat.mtime.toISOString(),
  };
}

function parseAttributes(source) {
  const attrs = {};
  const attrPattern = /([A-Za-z_:][-A-Za-z0-9_:.]*)="([^"]*)"/g;
  let match;
  while ((match = attrPattern.exec(source)) !== null) {
    attrs[match[1]] = match[2];
  }
  return attrs;
}

function sumSurefireReports() {
  const dir = absolute("backend/target/surefire-reports");
  const summary = {
    path: "backend/target/surefire-reports",
    suites: 0,
    tests: 0,
    failures: 0,
    errors: 0,
    skipped: 0,
    latest_mtime: null,
    passed: false,
  };
  if (!fs.existsSync(dir)) return summary;
  for (const file of fs.readdirSync(dir).filter((name) => name.startsWith("TEST-") && name.endsWith(".xml"))) {
    const filePath = path.join(dir, file);
    const text = fs.readFileSync(filePath, "utf8");
    const match = text.match(/<testsuite\b([^>]*)>/);
    if (!match) continue;
    const attrs = parseAttributes(match[1]);
    summary.suites += 1;
    summary.tests += Number(attrs.tests ?? 0);
    summary.failures += Number(attrs.failures ?? 0);
    summary.errors += Number(attrs.errors ?? 0);
    summary.skipped += Number(attrs.skipped ?? 0);
    const mtime = fs.statSync(filePath).mtime.toISOString();
    if (!summary.latest_mtime || mtime > summary.latest_mtime) summary.latest_mtime = mtime;
  }
  summary.passed = summary.suites > 0 && summary.tests > 0 && summary.failures === 0 && summary.errors === 0;
  return summary;
}

function listFilesRecursive(relativeDir, predicate) {
  const root = absolute(relativeDir);
  if (!fs.existsSync(root)) return [];
  const results = [];
  const visit = (dir) => {
    for (const name of fs.readdirSync(dir)) {
      const full = path.join(dir, name);
      const stat = fs.statSync(full);
      if (stat.isDirectory()) visit(full);
      else if (!predicate || predicate(full)) results.push(path.relative(repoRoot, full).replace(/\\/g, "/"));
    }
  };
  visit(root);
  return results.sort();
}

function countTestsInFiles(files) {
  let tests = 0;
  for (const file of files) {
    const text = readText(file);
    const matches = text.match(/^\s*(?:test|it)\s*\(/gm);
    tests += matches ? matches.length : 0;
  }
  return tests;
}

function webUnitTests() {
  const files = listFilesRecursive("apps/web/src", (full) => /\.test\.tsx?$/.test(full));
  return {
    files,
    file_count: files.length,
    test_count_from_source: countTestsInFiles(files),
  };
}

function playwrightEvidence() {
  const allSpecFiles = listFilesRecursive("tests/e2e", (full) => /\.spec\.ts$/.test(full));
  const normalSpecFiles = allSpecFiles.filter((file) => !file.endsWith("web-ui-real-backend.spec.ts"));
  const realBackendSpecFiles = allSpecFiles.filter((file) => file.endsWith("web-ui-real-backend.spec.ts"));
  const lastRun = exists("test-results/.last-run.json") ? readJson("test-results/.last-run.json") : null;
  return {
    normal: {
      report_path: "playwright-report/index.html",
      report_exists: exists("playwright-report/index.html"),
      spec_files: normalSpecFiles,
      test_count_from_source: countTestsInFiles(normalSpecFiles),
    },
    real_backend: {
      report_path: "playwright-report-real-backend/index.html",
      report_exists: exists("playwright-report-real-backend/index.html"),
      spec_files: realBackendSpecFiles,
      test_count_from_source: countTestsInFiles(realBackendSpecFiles),
    },
    last_run: lastRun,
    last_run_passed: lastRun?.status === "passed" && Array.isArray(lastRun.failedTests) && lastRun.failedTests.length === 0,
  };
}

function webBuildEvidence() {
  const assets = listFilesRecursive("apps/web/dist/assets", (full) => /\.(js|css)$/.test(full)).map((file) => statInfo(file));
  const mainJs = assets.filter((item) => item.path.includes("/main-") && item.path.endsWith(".js"));
  const largest = [...assets].sort((a, b) => b.size_bytes - a.size_bytes)[0] ?? null;
  return {
    dist_index_exists: exists("apps/web/dist/index.html"),
    route_indexes: ["apps/web/dist/ticketing/index.html", "apps/web/dist/bar/index.html", "apps/web/dist/amusement/index.html"].map((file) => ({ path: file, exists: exists(file) })),
    asset_count: assets.length,
    main_js_assets: mainJs,
    largest_asset: largest,
    no_main_bundle_warning_expected: mainJs.every((item) => item.size_bytes < 250 * 1024),
  };
}

function scriptsEvidence() {
  const scripts = readJson("package.json").scripts ?? {};
  const closedLoopRuntime = scripts["verify:closed-loop-runtime"] ?? "";
  return {
    test_backend: scripts["test:backend"] ?? "",
    web_typecheck: scripts["typecheck"] ?? "",
    web_build: scripts["build"] ?? "",
    verify_ui: scripts["verify:ui"] ?? "",
    verify_ui_real_backend_seeded: scripts["verify:ui:real-backend:seeded"] ?? "",
    verify_closed_loop_runtime: closedLoopRuntime,
    verify_runtime_acceptance_evidence: scripts["verify:runtime-acceptance-evidence"] ?? "",
    docs_runtime_acceptance_evidence: scripts["docs:runtime-acceptance-evidence"] ?? "",
    closed_loop_runtime_contains_expected_steps: ["test:backend", "typecheck --workspace apps/web", "build --workspace apps/web", "verify:ui", "verify:ui:real-backend:seeded"].every((token) => closedLoopRuntime.includes(token)),
  };
}

function currentStateEvidence() {
  return exists("docs/closed-loop-current-state-snapshot-latest.json") ? readJson("docs/closed-loop-current-state-snapshot-latest.json") : null;
}

function numeric(value, fallback = 0) {
  const number = Number(value ?? fallback);
  return Number.isFinite(number) ? number : fallback;
}

function countSemgrepErrors(value) {
  return Array.isArray(value) ? value.length : numeric(value, 0);
}

function semgrepSummary(semgrep) {
  return {
    scan_id: semgrep.scan_id ?? semgrep.scan_dir ?? semgrep.raw_result_file ?? semgrep.latest_file ?? "",
    exit_code: numeric(semgrep.semgrep_exit_code ?? semgrep.exit_code ?? semgrep.json_exitcode ?? semgrep.json_exit_code, 0),
    results: numeric(semgrep.result_count ?? semgrep.results ?? semgrep.results_count ?? semgrep.findings, 0),
    errors: countSemgrepErrors(semgrep.error_count ?? semgrep.errors ?? semgrep.errors_count),
    paths_scanned: numeric(semgrep.scanned_path_count ?? semgrep.paths_scanned ?? semgrep.paths_scanned_count ?? semgrep.scanned_count, 0),
  };
}

function buildState() {
  const semgrep = readJson("security-scans/semgrep-latest-summary.json");
  const semgrepState = semgrepSummary(semgrep);
  const backend = sumSurefireReports();
  const unit = webUnitTests();
  const playwright = playwrightEvidence();
  const webBuild = webBuildEvidence();
  const scripts = scriptsEvidence();
  const currentState = currentStateEvidence();
  const git = gitBinding();
  const semgrepClean = semgrepState.results === 0 && semgrepState.errors === 0 && semgrepState.exit_code === 0;
  const runtimeReady =
    backend.passed &&
    webBuild.dist_index_exists &&
    webBuild.no_main_bundle_warning_expected &&
    playwright.normal.report_exists &&
    playwright.real_backend.report_exists &&
    playwright.last_run_passed &&
    scripts.closed_loop_runtime_contains_expected_steps &&
    semgrepClean;
  const productionEvidenceGate = currentState?.gates?.production_evidence ?? null;
  const nasCurrentState = currentState?.gates?.nas_current_state ?? null;
  return {
    markers: [
      "RUNTIME_ACCEPTANCE_EVIDENCE_READY",
      "RUNTIME_EVIDENCE_FROM_SUREFIRE_REPORTS",
      "RUNTIME_EVIDENCE_FROM_PLAYWRIGHT_REPORTS",
      "RUNTIME_EVIDENCE_FROM_WEB_DIST",
      "RUNTIME_EVIDENCE_FROM_SEMGREP_LATEST_SUMMARY",
      "RUNTIME_EVIDENCE_BOUND_TO_SOURCE_FINGERPRINT",
      "RUNTIME_EVIDENCE_DOES_NOT_CLOSE_PRODUCTION_MANIFEST",
    ],
    generated_at: new Date().toISOString(),
    git,
    captured_command: "npm run verify:closed-loop-runtime",
    required_refresh_rule: "After backend, web, Playwright, OpenAPI, or runtime script changes, rerun npm run verify:closed-loop-runtime, then npm run docs:runtime-acceptance-evidence.",
    scripts,
    backend_surefire: backend,
    web_unit_source: unit,
    web_build: webBuild,
    playwright,
    semgrep: {
      ...semgrepState,
      clean: semgrepClean,
    },
    production_boundaries: {
      production_evidence_complete: productionEvidenceGate?.complete === true,
      production_evidence_awaiting: productionEvidenceGate?.awaiting_submission ?? null,
      p0_production_evidence_awaiting: productionEvidenceGate?.p0_awaiting_submission ?? null,
      nas_live_status: nasCurrentState?.live_status ?? "unknown",
      can_claim_production_ready: false,
    },
    conclusion: {
      runtime_ready_for_integration_and_business_acceptance: runtimeReady,
      can_claim_production_ready: false,
      reason: runtimeReady
        ? "Runtime evidence supports integration and business acceptance; production readiness remains blocked by external production evidence and NAS current mount state."
        : "Runtime evidence is incomplete or stale; rerun the closed-loop runtime command and regenerate this evidence.",
    },
  };
}

function renderMarkdown(state) {
  return `# 运行时验收证据快照 V0.1

> 本文由 \`tools/generate-runtime-acceptance-evidence.mjs\` 生成，用于把后端测试、Web 构建、Playwright UI、真实后端 UI、Semgrep 和生产边界汇总成可追溯证据。它不替代 \`npm run verify:closed-loop-runtime\`，也不关闭任何生产 manifest。

机器识别标记：

${state.markers.map((marker) => `- ${marker}`).join("\n")}

## 0. 证据绑定（防陈旧）

| 绑定项 | 当前值 |
|---|---|
| 生成时的 Git HEAD | \`${state.git.head}\`（${state.git.branch}） |
| 生成时工作树 | ${state.git.worktree_dirty ? "有未提交改动" : "干净"} |
| 源码指纹 sha256 | \`${state.git.source_fingerprint}\` |
| 指纹覆盖文件数 | ${state.git.source_fingerprint_file_count} |
| 生成时间 | ${state.generated_at} |
| 最大有效天数 | ${state.git.max_age_days} |

> 校验规则（\`--check\`）：若当前源码指纹与本文记录不一致（源码已变更），或本证据超过最大有效天数，则立即 FAIL；陈旧证据不再自欺 PASS。

## 1. 结论

| 判断项 | 当前值 |
|---|---|
| 运行时证据支持进入联调/业务验收 | ${state.conclusion.runtime_ready_for_integration_and_business_acceptance ? "是" : "否"} |
| 可以宣称生产上线就绪 | ${state.conclusion.can_claim_production_ready ? "是" : "否"} |
| 结论说明 | ${state.conclusion.reason} |

刷新规则：${state.required_refresh_rule}

## 2. 必须能复跑的命令

| 命令 | 当前脚本 |
|---|---|
| 闭环运行态总验收 | \`${state.scripts.verify_closed_loop_runtime}\` |
| 后端测试 | \`${state.scripts.test_backend}\` |
| Web 构建 | \`${state.scripts.web_build}\` |
| 常规 UI 验收 | \`${state.scripts.verify_ui}\` |
| 真实后端 UI 验收 | \`${state.scripts.verify_ui_real_backend_seeded}\` |
| 本证据校验 | \`${state.scripts.verify_runtime_acceptance_evidence}\` |

## 3. 后端测试证据

| 指标 | 当前值 |
|---|---:|
| Surefire suites | ${state.backend_surefire.suites} |
| tests | ${state.backend_surefire.tests} |
| failures | ${state.backend_surefire.failures} |
| errors | ${state.backend_surefire.errors} |
| skipped | ${state.backend_surefire.skipped} |
| latest_mtime | ${state.backend_surefire.latest_mtime ?? "-"} |

## 4. Web 与 UI 验收证据

| 证据 | 当前值 |
|---|---|
| Web dist index | ${state.web_build.dist_index_exists ? "存在" : "缺失"} |
| Web main JS | ${state.web_build.main_js_assets.map((item) => `\`${item.path}\` ${item.size_bytes} bytes`).join("; ") || "-"} |
| 最大静态资源 | ${state.web_build.largest_asset ? `\`${state.web_build.largest_asset.path}\` ${state.web_build.largest_asset.size_bytes} bytes` : "-"} |
| 主包过大 warning 预期 | ${state.web_build.no_main_bundle_warning_expected ? "无" : "需复查"} |
| 前端单元测试源文件 | ${state.web_unit_source.file_count} 个文件，${state.web_unit_source.test_count_from_source} 条测试定义 |
| Playwright 常规 UI | ${state.playwright.normal.report_exists ? "报告存在" : "报告缺失"}，${state.playwright.normal.test_count_from_source} 条测试定义 |
| Playwright 真实后端 UI | ${state.playwright.real_backend.report_exists ? "报告存在" : "报告缺失"}，${state.playwright.real_backend.test_count_from_source} 条测试定义 |
| Playwright last-run | ${state.playwright.last_run_passed ? "passed" : "not passed / missing"} |

## 5. 安全扫描与生产边界

| 证据 | 当前值 |
|---|---|
| Semgrep | scan=${state.semgrep.scan_id}，results=${state.semgrep.results}，errors=${state.semgrep.errors}，paths_scanned=${state.semgrep.paths_scanned} |
| 生产证据 | awaiting=${state.production_boundaries.production_evidence_awaiting ?? "unknown"}，P0 awaiting=${state.production_boundaries.p0_production_evidence_awaiting ?? "unknown"} |
| NAS 当前状态 | ${state.production_boundaries.nas_live_status} |
| 本报告是否关闭生产 manifest | 否 |

## 6. 机器可读证据

对应 JSON：\`docs/runtime-acceptance-evidence-latest.json\`。
`;
}

function validateExisting() {
  const failures = [];
  if (!exists(markdownPath)) failures.push(`${markdownPath} is missing`);
  if (!exists(jsonPath)) failures.push(`${jsonPath} is missing`);
  if (failures.length === 0) {
    const markdown = readText(markdownPath);
    const data = readJson(jsonPath);
    for (const marker of [
      "RUNTIME_ACCEPTANCE_EVIDENCE_READY",
      "RUNTIME_EVIDENCE_FROM_SUREFIRE_REPORTS",
      "RUNTIME_EVIDENCE_FROM_PLAYWRIGHT_REPORTS",
      "RUNTIME_EVIDENCE_FROM_WEB_DIST",
      "RUNTIME_EVIDENCE_BOUND_TO_SOURCE_FINGERPRINT",
      "RUNTIME_EVIDENCE_DOES_NOT_CLOSE_PRODUCTION_MANIFEST",
    ]) {
      if (!markdown.includes(marker)) failures.push(`${markdownPath} must include marker: ${marker}`);
      if (!data.markers?.includes(marker)) failures.push(`${jsonPath} must include marker: ${marker}`);
    }
    if (data.captured_command !== "npm run verify:closed-loop-runtime") failures.push(`${jsonPath} captured_command must be npm run verify:closed-loop-runtime`);
    if (data.backend_surefire?.tests < 1) failures.push(`${jsonPath} must record backend test count`);
    if (data.backend_surefire?.failures !== 0 || data.backend_surefire?.errors !== 0) failures.push(`${jsonPath} must record zero backend failures/errors`);
    if (data.playwright?.normal?.test_count_from_source < 1) failures.push(`${jsonPath} must record normal Playwright test definitions`);
    if (data.playwright?.real_backend?.test_count_from_source < 1) failures.push(`${jsonPath} must record real backend Playwright test definitions`);
    if (data.web_build?.no_main_bundle_warning_expected !== true) failures.push(`${jsonPath} must record no expected main bundle warning`);
    if (data.semgrep?.clean !== true) failures.push(`${jsonPath} must record clean Semgrep state`);
    if (data.conclusion?.can_claim_production_ready !== false) failures.push(`${jsonPath} must not claim production readiness`);
    if (!data.scripts?.verify_closed_loop_runtime?.includes("verify:ui:real-backend:seeded")) failures.push(`${jsonPath} must keep real backend UI in closed-loop runtime script`);

    // AUD-021: 陈旧证据不得通过。把证据绑定到当前源码指纹与生成时间。
    const git = data.git;
    if (!git || typeof git !== "object") {
      failures.push(`${jsonPath} must record git binding (regenerate with npm run docs:runtime-acceptance-evidence)`);
    } else {
      if (!git.head) failures.push(`${jsonPath} must record git.head`);
      if (!git.source_fingerprint) {
        failures.push(`${jsonPath} must record git.source_fingerprint`);
      } else {
        const current = computeSourceFingerprint().fingerprint;
        if (git.source_fingerprint !== current) {
          failures.push(
            `runtime acceptance evidence is stale: source changed since it was generated ` +
              `(evidence fingerprint ${git.source_fingerprint.slice(0, 12)}… != current ${current.slice(0, 12)}…). ` +
              `Rerun npm run verify:closed-loop-runtime then npm run docs:runtime-acceptance-evidence.`,
          );
        }
      }
    }
    const generatedAtMs = Date.parse(data.generated_at);
    if (!Number.isFinite(generatedAtMs)) {
      failures.push(`${jsonPath} generated_at is not a valid timestamp`);
    } else {
      const ageDays = (Date.now() - generatedAtMs) / 86_400_000;
      if (ageDays > MAX_AGE_DAYS) {
        failures.push(
          `runtime acceptance evidence is stale: generated ${ageDays.toFixed(1)} days ago exceeds ` +
            `RUNTIME_EVIDENCE_MAX_AGE_DAYS=${MAX_AGE_DAYS}. Rerun the closed-loop runtime command and regenerate.`,
        );
      }
    }
  }
  if (failures.length > 0) {
    console.error("RUNTIME_ACCEPTANCE_EVIDENCE_FAIL");
    for (const failure of failures) console.error(`- ${failure}`);
    process.exit(1);
  }
  console.log("RUNTIME_ACCEPTANCE_EVIDENCE_PASS");
}

if (checkMode) {
  validateExisting();
} else {
  const state = buildState();
  fs.writeFileSync(absolute(jsonPath), JSON.stringify(state, null, 2) + "\n");
  fs.writeFileSync(absolute(markdownPath), renderMarkdown(state));
  console.log(`wrote ${markdownPath}`);
  console.log(`wrote ${jsonPath}`);
}
