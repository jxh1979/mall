// 00-entry 事实基线快照自动回填（审计 P2-4 建议"计数由工具自动回填"的落地，根治手工刷反复过期）。
// 权威来源：openapi-p0.json（operation 数）/ db/migration（最新迁移）/ surefire-reports（测试数）。
// 用法：node tools/refresh-entry-doc-baseline.mjs        —— 回填快照（写文件）
//       node tools/refresh-entry-doc-baseline.mjs --check —— 只校验漂移，漂移则退出码 1（可挂进可选链，
//       不入 delivery-gates：文档快照漂移闸属"自指证据闸"，按 2026-07-02 B2 裁决留在可选链）。
// 只改带"本轮/当前事实基线/取证"快照语义的行；历史变更记录（§changelog 旧快照数字）不动。
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const entryPath = path.join(root, "docs", "00-current-effective-document-entry-v0.1.md");
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

// ---- 快照行替换（正则均限定"快照语义"上下文，不碰历史 changelog 行）----
let text = fs.readFileSync(entryPath, "utf8");
const before = text;
const replacements = [
  [/本轮（\d{4}-\d{2}-\d{2}）核验为 \d+ operations/g,
    `本轮（${today}）核验为 ${operationCount} operations`],
  [/当前事实基线（后端 surefire 现有报告 \d+ tests \/ OpenAPI \d+ operations \/ Flyway 最新 V\d+/g,
    `当前事实基线（后端 surefire 现有报告 ${testCount ?? "\\0"} tests / OpenAPI ${operationCount} operations / Flyway 最新 ${latestMigrationTag}`],
  [/本轮 \d{4}-\d{2}-\d{2} 取证 Flyway 最新 V\d+、OpenAPI \d+ operations、后端 surefire 现有报告 \d+ tests/g,
    `本轮 ${today} 取证 Flyway 最新 ${latestMigrationTag}、OpenAPI ${operationCount} operations、后端 surefire 现有报告 ${testCount ?? "\\0"} tests`],
  [/查看本轮核验 \d+ 个 operation/g,
    `查看本轮核验 ${operationCount} 个 operation`],
  [/示例、\d+ 个 operation 矩阵/g,
    `示例、${operationCount} 个 operation 矩阵`],
];
for (const [pattern, replacement] of replacements) {
  if (testCount === null && replacement.includes("\\0")) continue; // 无测试报告时跳过含 tests 的行
  text = text.replace(pattern, replacement);
}

if (checkOnly) {
  if (text !== before) {
    console.error(`[entry-doc-baseline] DRIFT: 00-entry 快照落后于实际（应为 ${operationCount} ops / ${latestMigrationTag}${testCount ? ` / ${testCount} tests` : ""}）。运行 node tools/refresh-entry-doc-baseline.mjs 回填。`);
    process.exit(1);
  }
  console.log(`[entry-doc-baseline] PASS: 快照与实际一致（${operationCount} ops / ${latestMigrationTag}${testCount ? ` / ${testCount} tests` : ""}）。`);
  process.exit(0);
}

if (text === before) {
  console.log(`[entry-doc-baseline] 无漂移，未改动（${operationCount} ops / ${latestMigrationTag}${testCount ? ` / ${testCount} tests` : ""}）。`);
} else {
  fs.writeFileSync(entryPath, text);
  console.log(`[entry-doc-baseline] 已回填：${operationCount} operations / ${latestMigrationTag}${testCount ? ` / ${testCount} tests` : ""}（${today}）。`);
}
