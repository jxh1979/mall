// 00-entry + README 事实基线快照自动回填（审计 P2-4 建议"计数由工具自动回填"的落地，根治手工刷反复过期）。
// 权威来源：openapi-p0.json（operation 数）/ db/migration（最新迁移+迁移数）/ surefire-reports（测试数）。
// 用法：node tools/refresh-entry-doc-baseline.mjs        —— 回填快照（写文件）
//       node tools/refresh-entry-doc-baseline.mjs --check —— 只校验漂移，漂移则退出码 1（可挂进可选链，
//       不入 delivery-gates：文档快照漂移闸属"自指证据闸"，按 2026-07-02 B2 裁决留在可选链）。
// 只改带"本轮/当前事实基线/取证"快照语义的行与 README baseline:auto 标记块；
// 历史变更记录（§changelog 旧快照数字）不动。无 surefire 报告时保留原测试数（不臆造），其余数字照常回填。
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const entryPath = path.join(root, "docs", "00-current-effective-document-entry-v0.1.md");
const readmePath = path.join(root, "README.md");
const openapiPath = path.join(root, "docs", "openapi", "openapi-p0.json");
const migrationDir = path.join(root, "backend", "src", "main", "resources", "db", "migration");
const surefireDir = path.join(root, "backend", "target", "surefire-reports");
const checkOnly = process.argv.includes("--check");

// ---- 权威值 ----
const methods = ["get", "post", "put", "patch", "delete", "head", "options"];
const openapi = JSON.parse(fs.readFileSync(openapiPath, "utf8"));
let operationCount = 0;
for (const item of Object.values(openapi.paths ?? {})) {
  for (const method of methods) {
    if (item?.[method]) operationCount += 1;
  }
}

const migrations = fs.readdirSync(migrationDir).filter((name) => /^V\d+__.*\.sql$/.test(name));
const migrationCount = migrations.length;
const latestMigration = migrations
  .map((name) => ({ name, version: Number(/^V(\d+)__/.exec(name)[1]) }))
  .sort((a, b) => a.version - b.version)
  .at(-1);
const latestMigrationTag = `V${latestMigration.version}`;

// surefire：汇总 xml testsuite tests 属性；无报告目录则保留原快照数字（不臆造）。
let testCount = null;
if (fs.existsSync(surefireDir)) {
  testCount = 0;
  for (const file of fs.readdirSync(surefireDir)) {
    if (!file.endsWith(".xml")) continue;
    const head = fs.readFileSync(path.join(surefireDir, file), "utf8").slice(0, 500);
    const match = /<testsuite[^>]*\btests="(\d+)"/.exec(head);
    if (match) testCount += Number(match[1]);
  }
  if (testCount === 0) testCount = null;
}

const today = new Date().toISOString().slice(0, 10);

// ---- 00-entry 快照行替换（正则均限定"快照语义"上下文，不碰历史 changelog 行）----
// tests 数无新报告时用捕获组保留原数字，ops/迁移照常回填（修 AUD-008：此前无报告会整行跳过导致 ops 也不刷）。
let entryText = fs.readFileSync(entryPath, "utf8");
const entryBefore = entryText;
entryText = entryText.replace(/本轮（\d{4}-\d{2}-\d{2}）核验为 \d+ operations/g,
    `本轮（${today}）核验为 ${operationCount} operations`);
entryText = entryText.replace(
    /当前事实基线（后端 surefire 现有报告 (\d+) tests \/ OpenAPI \d+ operations \/ Flyway 最新 V\d+/g,
    (m, oldTests) => `当前事实基线（后端 surefire 现有报告 ${testCount ?? oldTests} tests / OpenAPI ${operationCount} operations / Flyway 最新 ${latestMigrationTag}`);
entryText = entryText.replace(
    /本轮 \d{4}-\d{2}-\d{2} 取证 Flyway 最新 V\d+、OpenAPI \d+ operations、后端 surefire 现有报告 (\d+) tests/g,
    (m, oldTests) => `本轮 ${today} 取证 Flyway 最新 ${latestMigrationTag}、OpenAPI ${operationCount} operations、后端 surefire 现有报告 ${testCount ?? oldTests} tests`);
entryText = entryText.replace(/查看本轮核验 \d+ 个 operation/g,
    `查看本轮核验 ${operationCount} 个 operation`);
entryText = entryText.replace(/本轮核验 \d+ 个 OpenAPI operation/g,
    `本轮核验 ${operationCount} 个 OpenAPI operation`);
entryText = entryText.replace(/示例、\d+ 个 operation 矩阵/g,
    `示例、${operationCount} 个 operation 矩阵`);

// ---- README baseline:auto 标记块整块重生成 ----
let readmeText = fs.readFileSync(readmePath, "utf8");
const readmeBefore = readmeText;
const blockRe = /(<!-- baseline:auto-begin[^>]*-->)([\s\S]*?)(<!-- baseline:auto-end -->)/;
const blockMatch = blockRe.exec(readmeText);
if (blockMatch) {
  const oldTests = /\*\*(\d+)\*\* tests/.exec(blockMatch[2]);
  const effectiveTests = testCount ?? (oldTests ? Number(oldTests[1]) : null);
  const testsCell = effectiveTests === null
      ? "以 `mvn test` 后 surefire 报告为准（无现存快照）"
      : `**${effectiveTests}** tests（现有 surefire 报告）`;
  const block = [
    "",
    "| 指标 | 当前值 | 取证方式 |",
    "|---|---|---|",
    `| Flyway 最新迁移 | **${latestMigrationTag}**（${migrationCount} 个迁移） | \`backend/src/main/resources/db/migration\` 实时统计 |`,
    `| OpenAPI operation 数 | **${operationCount}** | \`docs/openapi/openapi-p0.json\` 方法统计（与 \`npm run verify:operation-matrix\` 同口径） |`,
    `| 后端测试 | ${testsCell} | \`backend/target/surefire-reports\` 合计；无报告时保留上次快照，不声明刚完成全量测试 |`,
    "| 前端 E2E | 以 `npx playwright test --list` 实时输出为准 | 不写死快照数 |",
    "",
  ].join("\n");
  readmeText = readmeText.replace(blockRe, `$1${block}$3`);
}

const summary = `${operationCount} ops / ${latestMigrationTag}（${migrationCount} 迁移）${testCount ? ` / ${testCount} tests` : ""}`;
const drifted = [];
if (entryText !== entryBefore) drifted.push("docs/00-current-effective-document-entry-v0.1.md");
if (readmeText !== readmeBefore) drifted.push("README.md");

if (checkOnly) {
  if (drifted.length > 0) {
    console.error(`[entry-doc-baseline] DRIFT: ${drifted.join(" + ")} 快照落后于实际（应为 ${summary}）。运行 node tools/refresh-entry-doc-baseline.mjs 回填。`);
    process.exit(1);
  }
  console.log(`[entry-doc-baseline] PASS: 快照与实际一致（${summary}）。`);
  process.exit(0);
}

if (drifted.length === 0) {
  console.log(`[entry-doc-baseline] 无漂移，未改动（${summary}）。`);
} else {
  if (entryText !== entryBefore) fs.writeFileSync(entryPath, entryText);
  if (readmeText !== readmeBefore) fs.writeFileSync(readmePath, readmeText);
  console.log(`[entry-doc-baseline] 已回填 ${drifted.join(" + ")}：${summary}（${today}）。`);
}
