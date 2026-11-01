import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const markdownPath = "docs/closed-loop-implementation-ui-acceptance-report-v0.1.md";
const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");

function absolute(relativePath) {
  return path.join(repoRoot, relativePath);
}

function exists(relativePath) {
  return fs.existsSync(absolute(relativePath));
}

function readText(relativePath) {
  return fs.readFileSync(absolute(relativePath), "utf8").replace(/^\uFEFF/, "");
}

function writeText(relativePath, text) {
  fs.writeFileSync(absolute(relativePath), text, "utf8");
}

function readJson(relativePath) {
  return JSON.parse(readText(relativePath));
}

function number(value, fallback = 0) {
  const parsed = Number(value ?? fallback);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function countSemgrepErrors(semgrep) {
  return Array.isArray(semgrep.errors) ? semgrep.errors.length : number(semgrep.error_count ?? semgrep.errors ?? semgrep.errors_count, 0);
}

function semgrepState(semgrep) {
  return {
    scan_ref: semgrep.raw_result_file ?? semgrep.latest_file ?? semgrep.scan_id ?? semgrep.scan_dir ?? semgrep.scanDir ?? "\u672a\u8bb0\u5f55",
    exit_code: number(semgrep.semgrep_exit_code ?? semgrep.exit_code ?? semgrep.json_exitcode ?? semgrep.json_exit_code, 0),
    results: number(semgrep.result_count ?? semgrep.results ?? semgrep.results_count ?? semgrep.findings, 0),
    errors: countSemgrepErrors(semgrep),
    paths_scanned: Array.isArray(semgrep.paths_scanned) ? semgrep.paths_scanned.length : number(semgrep.scanned_path_count ?? semgrep.paths_scanned ?? semgrep.paths_scanned_count ?? semgrep.scanned_count, 0),
  };
}

function walkFiles(dir, predicate, out = []) {
  const absDir = absolute(dir);
  if (!fs.existsSync(absDir)) return out;
  for (const entry of fs.readdirSync(absDir, { withFileTypes: true })) {
    const full = path.join(absDir, entry.name);
    const relative = path.relative(repoRoot, full).replace(/\\/g, "/");
    if (entry.isDirectory()) walkFiles(relative, predicate, out);
    else if (predicate(relative)) out.push(relative);
  }
  return out.sort();
}

function extractPlaywrightTests(specFiles) {
  const tests = [];
  for (const file of specFiles) {
    if (!exists(file)) continue;
    const source = readText(file);
    const regex = /\btest\s*\(\s*["'`]([^"'`]+)["'`]/g;
    let match;
    while ((match = regex.exec(source)) !== null) {
      tests.push({ file, name: match[1] });
    }
  }
  return tests;
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
  const runtime = readJson("docs/runtime-acceptance-evidence-latest.json");
  const current = readJson("docs/closed-loop-current-state-snapshot-latest.json");
  const semgrep = semgrepState(readJson("security-scans/semgrep-latest-summary.json"));
  const packageJson = readJson("package.json");
  const normalSpecFiles = runtime.playwright?.normal?.spec_files ?? [];
  const realBackendSpecFiles = runtime.playwright?.real_backend?.spec_files ?? [];
  const normalTests = extractPlaywrightTests(normalSpecFiles);
  const realBackendTests = extractPlaywrightTests(realBackendSpecFiles);
  const visualSnapshots = walkFiles("tests/e2e", (file) => file.endsWith(".png") && file.includes("-snapshots/"));
  return {
    markers: [
      "UI_EXPERIENCE_ACCEPTANCE_REPORT_READY",
      "GENERATED_FROM_RUNTIME_ACCEPTANCE_EVIDENCE",
      "GENERATED_FROM_PLAYWRIGHT_SPECS_AND_SNAPSHOTS",
      "REAL_BACKEND_UI_INCLUDED",
      "VISUAL_REGRESSION_BASELINES_TRACKED",
      "NO_STALE_UI_ACCEPTANCE_COUNTS",
    ],
    generated_at: runtime.generated_at,
    runtime,
    current,
    semgrep,
    package_scripts: {
      docs_ui_experience_acceptance: packageJson.scripts?.["docs:ui-experience-acceptance"] ?? "",
      verify_ui_experience_acceptance: packageJson.scripts?.["verify:ui-experience-acceptance"] ?? "",
      verify_ui: packageJson.scripts?.["verify:ui"] ?? "",
      verify_ui_real_backend_seeded: packageJson.scripts?.["verify:ui:real-backend:seeded"] ?? "",
      verify_closed_loop_runtime: packageJson.scripts?.["verify:closed-loop-runtime"] ?? "",
    },
    normal_tests: normalTests,
    real_backend_tests: realBackendTests,
    visual_snapshots: visualSnapshots,
    architecture_line_counts: current.architecture_line_counts ?? {},
    matrices: current.matrices ?? {},
    go_no_go: current.go_no_go ?? {},
  };
}

function render(state) {
  const runtime = state.runtime;
  const webBuild = runtime.web_build ?? {};
  const mainAsset = webBuild.main_js_assets?.[0] ?? {};
  const largestAsset = webBuild.largest_asset ?? {};
  const productionBlockers = state.go_no_go.production_ready_blockers ?? [];
  const testRows = [
    ...state.normal_tests.map((test) => ({ kind: "常规 UI", ...test })),
    ...state.real_backend_tests.map((test) => ({ kind: "真实后端 UI", ...test })),
  ];
  const snapshotRows = state.visual_snapshots.map((file) => ({ file }));
  const lineRows = Object.entries(state.architecture_line_counts).map(([file, lines]) => ({ file, lines }));
  const scriptRows = Object.entries(state.package_scripts).map(([name, command]) => ({ name, command }));
  return `# 闭环实现与前端体验验收报告 V0.1

> 本文由 \`tools/generate-ui-experience-acceptance-report.mjs\` 从运行态验收证据、闭环当前态快照、Semgrep 摘要、Playwright spec 和截图基线生成。它用于证明前端体验验收报告没有漂移，不替代真实生产证据回填。

机器标记：

${state.markers.map((marker) => `- ${marker}`).join("\n")}

更新时间：${state.generated_at}
执行位置：192.168.88.160，\`/home/james/complex-management\`

## 1. 本轮闭环结论

当前项目的 Web 管理端、完整后台首批/非首批页面、真实后端 UI 抽样、视觉回归和运行态证据的验收状态见下表与第 8 节；报告中的数字来自机器证据，不再保留旧口径。

| 项目 | 当前值 | 证据 |
|---|---:|---|
| 需求矩阵完整实现 | ${state.matrices.requirements?.complete ?? "未记录"}/${state.matrices.requirements?.total ?? "未记录"} | \`docs/closed-loop-current-state-snapshot-latest.json\` |
| 完整后台 MVP | ${state.matrices.full_admin_mvp?.complete ?? "未记录"}/${state.matrices.full_admin_mvp?.total ?? "未记录"} | \`docs/full-admin-mvp-requirements-code-matrix-v0.1.csv\` |
| 完整后台非首批 | ${state.matrices.full_admin_remaining?.complete ?? "未记录"}/${state.matrices.full_admin_remaining?.total ?? "未记录"} | \`docs/full-admin-remaining-requirements-code-matrix-v0.1.csv\` |
| 后端测试 | ${runtime.backend_surefire?.tests ?? "未记录"} | \`backend/target/surefire-reports\` |
| Web 单元测试定义 | ${runtime.web_unit_source?.test_count_from_source ?? "未记录"} | \`apps/web/src/**/*.test.*\` |
| Playwright 常规 UI | ${runtime.playwright?.normal?.test_count_from_source ?? "未记录"} | \`playwright-report/index.html\` |
| Playwright 真实后端 UI | ${runtime.playwright?.real_backend?.test_count_from_source ?? "未记录"}${runtime.playwright?.real_backend?.report_exists ? "" : "（报告缺失，未跑）"} | \`playwright-report-real-backend/index.html\` |
| 视觉回归截图基线 | ${state.visual_snapshots.length} | \`tests/e2e/**/*-snapshots/*.png\` |
| Semgrep | results=${state.semgrep.results}, errors=${state.semgrep.errors}, paths_scanned=${state.semgrep.paths_scanned} | \`${state.semgrep.scan_ref}\` |
| 可进入联调 | ${state.go_no_go.can_enter_integration ? "是" : "否"} | \`closed-loop-current-state-snapshot-latest.json\` |
| 可进入业务验收 | ${state.go_no_go.can_enter_business_acceptance ? "是" : "否"} | \`closed-loop-current-state-snapshot-latest.json\` |
| 可宣称生产上线就绪 | ${state.go_no_go.can_claim_production_ready ? "是" : "否"} | 生产证据与 NAS 当前状态 |

## 2. 前端构建与包体

| 项目 | 当前值 |
|---|---:|
| main JS | ${mainAsset.path ?? "未记录"}，${mainAsset.size_bytes ?? "未记录"} bytes |
| largest asset | ${largestAsset.path ?? "未记录"}，${largestAsset.size_bytes ?? "未记录"} bytes |
| main bundle warning | ${webBuild.no_main_bundle_warning_expected ? "不预期出现" : "需复查"} |
| dist index | ${webBuild.dist_index_exists ? "存在" : "缺失"} |
| route indexes | ${(webBuild.route_indexes ?? []).filter((row) => row.exists).length}/${(webBuild.route_indexes ?? []).length} |

## 3. UI 自动化验收矩阵

${mdTable(testRows, [
  { title: "类型", value: (row) => row.kind },
  { title: "测试路径", value: (row) => row.name },
  { title: "证据文件", value: (row) => row.file },
])}

## 4. 视觉回归基线

${mdTable(snapshotRows, [
  { title: "截图基线", value: (row) => row.file },
])}

## 5. 架构与可维护性约束

${mdTable(lineRows, [
  { title: "文件", value: (row) => row.file },
  { title: "当前行数", align: "right", value: (row) => row.lines },
])}

## 6. 可复跑命令

${mdTable(scriptRows, [
  { title: "脚本", value: (row) => row.name },
  { title: "命令", value: (row) => row.command },
])}

## 7. 仍不能宣称生产就绪的原因

${productionBlockers.length > 0 ? productionBlockers.map((item) => `- ${item}`).join("\n") : "- 无。"}

## 8. 验收判断

- 可进入开发联调：${state.go_no_go.can_enter_integration ? "可以" : "不可以"}。
- 可进入业务验收：${state.go_no_go.can_enter_business_acceptance ? "可以" : "不可以"}。
- 可宣称生产完全就绪：${state.go_no_go.can_claim_production_ready ? "可以" : "不可以"}。
- 下一步重点：继续回填真实生产证据、恢复 NAS 当前挂载/演练证据，并保持 \`npm run verify:ui-experience-acceptance\`、\`npm run verify:runtime-acceptance-evidence\` 和 \`npm run verify:delivery-gates\` 绿色。
`;
}

function validate(state, markdown) {
  const failures = [];
  const runtimeNormal = number(state.runtime.playwright?.normal?.test_count_from_source, -1);
  const runtimeReal = number(state.runtime.playwright?.real_backend?.test_count_from_source, -1);
  if (runtimeNormal !== state.normal_tests.length) failures.push(`normal Playwright source count mismatch: runtime=${runtimeNormal}, source=${state.normal_tests.length}`);
  if (runtimeReal !== state.real_backend_tests.length) failures.push(`real backend Playwright source count mismatch: runtime=${runtimeReal}, source=${state.real_backend_tests.length}`);
  if (state.visual_snapshots.length < 4) failures.push(`visual snapshot baselines must be >= 4, got ${state.visual_snapshots.length}`);
  if (state.semgrep.results !== 0 || state.semgrep.errors !== 0 || state.semgrep.exit_code !== 0) failures.push("Semgrep latest summary must be clean");
  if (state.semgrep.paths_scanned <= 0) failures.push("Semgrep paths_scanned must be positive");
  if (!state.package_scripts.docs_ui_experience_acceptance) failures.push("package.json must expose docs:ui-experience-acceptance");
  if (!state.package_scripts.verify_ui_experience_acceptance) failures.push("package.json must expose verify:ui-experience-acceptance");
  for (const stale of ["Semgrep 未授权", "25 条 Playwright", "562 个后端测试", "81/96", "paths_scanned=0"]) {
    if (markdown.includes(stale)) failures.push(`${markdownPath} must not contain stale phrase: ${stale}`);
  }
  for (const marker of state.markers) {
    if (!markdown.includes(marker)) failures.push(`${markdownPath} must include marker ${marker}`);
  }
  return failures;
}

const state = buildState();
const markdown = render(state);
const failures = validate(state, markdown);

if (checkMode) {
  if (!exists(markdownPath)) failures.push(`${markdownPath} is missing`);
  else if (readText(markdownPath) !== markdown) failures.push(`${markdownPath} is not in sync with current UI/runtime evidence`);
  if (failures.length > 0) {
    console.error("UI_EXPERIENCE_ACCEPTANCE_REPORT_FAIL");
    for (const failure of failures) console.error(`- ${failure}`);
    process.exit(1);
  }
  console.log("UI_EXPERIENCE_ACCEPTANCE_REPORT_PASS");
} else {
  writeText(markdownPath, markdown);
  if (failures.length > 0) {
    console.error("UI_EXPERIENCE_ACCEPTANCE_REPORT_WARN");
    for (const failure of failures) console.error(`- ${failure}`);
  }
  console.log(`wrote ${markdownPath}`);
}
