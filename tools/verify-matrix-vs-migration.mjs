// verify:matrix-vs-migration
// 护栏：以 Flyway 迁移目录为唯一事实源，校验 requirement-traceability-matrix / dev-design 中
// "声称已补 X 表/字段" 的断言是否与真实 schema 一致。根治 R1/R3 反复发现的
// "设计稿写了字段、迁移从未落地" 漂移。
//
// 工作方式：
//   1. 解析 backend 全部 V*.sql，构建 table -> Set<column> 的真实 schema。
//   2. 读取 docs/matrix-migration-claims.json 声明清单（每条 = {req, table, column?, expected:'present'|'absent'}）。
//   3. present 不存在 / absent 已出现 => 失败并明确指示动作。
//      其中 absent 变 present 表示"修复已落地"——必须同步更新本清单与 requirement-traceability-matrix
//      的覆盖状态（把误导性的旧 '已覆盖' 改为真实状态），否则闸口阻断。

import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const migrationDir = path.join(root, "backend", "src", "main", "resources", "db", "migration");
const claimsPath = path.join(root, "docs", "matrix-migration-claims.json");

const CONSTRAINT_KEYWORDS = new Set([
  "CONSTRAINT", "PRIMARY", "FOREIGN", "UNIQUE", "CHECK", "EXCLUDE", "LIKE", "INDEX",
]);

function buildSchema() {
  const tables = new Map(); // tableName -> Set<column>
  const files = fs.readdirSync(migrationDir).filter((f) => /^V\d+__.*\.sql$/.test(f));
  for (const file of files) {
    const sql = fs.readFileSync(path.join(migrationDir, file), "utf8");

    // CREATE TABLE [IF NOT EXISTS] <name> ( ... \n);   (table-close ")" at line start)
    const createRe = /CREATE TABLE\s+(?:IF NOT EXISTS\s+)?([a-zA-Z_][\w]*)\s*\(([\s\S]*?)\n\)\s*;/gi;
    let m;
    while ((m = createRe.exec(sql)) !== null) {
      const table = m[1].toLowerCase();
      const body = m[2];
      const columns = tables.get(table) ?? new Set();
      for (const rawLine of body.split("\n")) {
        const line = rawLine.trim();
        if (!line) continue;
        const tokenMatch = line.match(/^"?([a-zA-Z_][\w]*)"?/);
        if (!tokenMatch) continue;
        const token = tokenMatch[1];
        if (CONSTRAINT_KEYWORDS.has(token.toUpperCase())) continue;
        columns.add(token.toLowerCase());
      }
      tables.set(table, columns);
    }

    // ALTER TABLE <name> ... ADD COLUMN [IF NOT EXISTS] <col> ...  (one statement may add several)
    const alterRe = /ALTER TABLE\s+(?:ONLY\s+)?([a-zA-Z_][\w]*)([\s\S]*?);/gi;
    let a;
    while ((a = alterRe.exec(sql)) !== null) {
      const table = a[1].toLowerCase();
      const stmt = a[2];
      const addColRe = /ADD COLUMN\s+(?:IF NOT EXISTS\s+)?"?([a-zA-Z_][\w]*)"?/gi;
      let c;
      const columns = tables.get(table) ?? new Set();
      while ((c = addColRe.exec(stmt)) !== null) {
        columns.add(c[1].toLowerCase());
      }
      if (columns.size > 0) tables.set(table, columns);
    }
  }
  return tables;
}

function exists(schema, table, column) {
  const t = table.toLowerCase();
  if (!schema.has(t)) return false;
  if (!column) return true;
  return schema.get(t).has(column.toLowerCase());
}

function main() {
  if (!fs.existsSync(claimsPath)) {
    console.error(`matrix-migration claims registry not found: ${claimsPath}`);
    process.exit(1);
  }
  const schema = buildSchema();
  const registry = JSON.parse(fs.readFileSync(claimsPath, "utf8"));
  const claims = registry.claims ?? [];
  const errors = [];
  let presentChecked = 0;
  let absentChecked = 0;

  for (const claim of claims) {
    const { req, table, column, expected } = claim;
    const target = column ? `${table}.${column}` : `${table}`;
    const isThere = exists(schema, table, column);
    if (expected === "present") {
      presentChecked++;
      if (!isThere) {
        errors.push(`[${req}] claims PRESENT but missing in migrations: ${target} — matrix/dev-design over-claims; either implement it or correct the matrix coverage status.`);
      }
    } else if (expected === "absent") {
      absentChecked++;
      if (isThere) {
        errors.push(`[${req}] registered ABSENT but now EXISTS in migrations: ${target} — a fix landed; update docs/matrix-migration-claims.json to 'present' AND fix requirement-traceability-matrix coverage status.`);
      }
    } else {
      errors.push(`[${req}] invalid expected value '${expected}' for ${target} (use present|absent)`);
    }
  }

  if (errors.length > 0) {
    console.error("matrix-vs-migration verification FAILED:");
    for (const e of errors) console.error(`- ${e}`);
    console.error(`\nParsed ${schema.size} tables from migrations; ${presentChecked} present-claims, ${absentChecked} absent-claims checked.`);
    process.exit(1);
  }

  console.log(`matrix-vs-migration verification passed: ${schema.size} tables parsed; ${presentChecked} present-claims confirmed, ${absentChecked} absent-claims still-absent (tracked gaps).`);
}

main();
