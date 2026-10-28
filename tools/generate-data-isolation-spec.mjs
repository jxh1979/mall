// 文件版本：V0.02
// docs:data-isolation-spec / verify:data-isolation-spec
// 以 Flyway 迁移目录为事实源，生成数据隔离规格中的自动迁移隔离基线，避免 R1-FINDING-A 的静态计数漂移。

import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const migrationDir = path.join(root, "backend", "src", "main", "resources", "db", "migration");
const specPath = path.join(root, "docs", "data-isolation-and-tenant-scope-security-spec-v0.1.md");
const check = process.argv.includes("--check");

const START = "<!-- DATA_ISOLATION_SPEC_AUTO_START -->";
const END = "<!-- DATA_ISOLATION_SPEC_AUTO_END -->";
const SCOPE_COLUMNS = ["organization_id", "merchant_id", "project_id", "venue_id", "bar_venue_id", "space_id"];
const CONSTRAINT_KEYWORDS = new Set(["CONSTRAINT", "PRIMARY", "FOREIGN", "UNIQUE", "CHECK", "EXCLUDE", "LIKE", "INDEX"]);

function migrationFiles() {
  return fs.readdirSync(migrationDir)
    .filter((file) => /^V\d+__.*\.sql$/.test(file))
    .sort((a, b) => Number(a.match(/^V(\d+)/)[1]) - Number(b.match(/^V(\d+)/)[1]));
}

function addColumn(tables, table, column, version, source) {
  const key = table.toLowerCase();
  if (!tables.has(key)) tables.set(key, { table: key, columns: new Map(), versions: new Set() });
  const row = tables.get(key);
  row.versions.add(version);
  const col = column.toLowerCase();
  if (!row.columns.has(col)) row.columns.set(col, { column: col, versions: new Set(), sources: new Set() });
  row.columns.get(col).versions.add(version);
  row.columns.get(col).sources.add(source);
}

function parseSchema() {
  const tables = new Map();
  const files = migrationFiles();
  for (const file of files) {
    const version = file.match(/^(V\d+)__/)[1];
    const sql = fs.readFileSync(path.join(migrationDir, file), "utf8");

    const createRe = /CREATE TABLE\s+(?:IF NOT EXISTS\s+)?([a-zA-Z_][\w]*)\s*\(([\s\S]*?)\n\)\s*;/gi;
    let createMatch;
    while ((createMatch = createRe.exec(sql)) !== null) {
      const table = createMatch[1];
      for (const rawLine of createMatch[2].split("\n")) {
        const line = rawLine.trim();
        if (!line) continue;
        const tokenMatch = line.match(/^"?([a-zA-Z_][\w]*)"?/);
        if (!tokenMatch) continue;
        const token = tokenMatch[1];
        if (CONSTRAINT_KEYWORDS.has(token.toUpperCase())) continue;
        if (SCOPE_COLUMNS.includes(token.toLowerCase())) {
          addColumn(tables, table, token, version, file);
        }
      }
    }

    const alterRe = /ALTER TABLE\s+(?:ONLY\s+)?([a-zA-Z_][\w]*)([\s\S]*?);/gi;
    let alterMatch;
    while ((alterMatch = alterRe.exec(sql)) !== null) {
      const table = alterMatch[1];
      const stmt = alterMatch[2];
      const addColRe = /ADD COLUMN\s+(?:IF NOT EXISTS\s+)?"?([a-zA-Z_][\w]*)"?/gi;
      let colMatch;
      while ((colMatch = addColRe.exec(stmt)) !== null) {
        const column = colMatch[1];
        if (SCOPE_COLUMNS.includes(column.toLowerCase())) {
          addColumn(tables, table, column, version, file);
        }
      }
      // DROP COLUMN — 删掉的 scope 列移出基线，表上一个 scope 列都不剩即移出（第十八批 V558：会员等级与生日加倍规则改全平台一套）。
      const dropColRe = /DROP COLUMN\s+(?:IF EXISTS\s+)?"?([a-zA-Z_][\w]*)"?/gi;
      while ((colMatch = dropColRe.exec(stmt)) !== null) {
        const row = tables.get(table.toLowerCase());
        if (!row) continue;
        row.columns.delete(colMatch[1].toLowerCase());
        if (row.columns.size === 0) tables.delete(table.toLowerCase());
      }
    }

    // DROP TABLE — 已删除的表移出隔离基线（按版本序处理：本文件的 DROP 在其 CREATE/ALTER 之后应用；
    // 跨文件“建→删→重建”场景由后续迁移的 CREATE TABLE 自然重新加入，故不会误删重建表）。
    const dropRe = /DROP TABLE\s+(?:IF EXISTS\s+)?"?([a-zA-Z_][\w]*)"?/gi;
    let dropMatch;
    while ((dropMatch = dropRe.exec(sql)) !== null) {
      tables.delete(dropMatch[1].toLowerCase());
    }
  }
  return { tables, files };
}

// 本区块只收自带 scope 列的表：按规格 §1 定义即 direct（与盘点 CSV 同口径），不发射 §1 类型闭集之外的值（M029-001）。
function scopeType() {
  return "direct";
}

function renderBlock() {
  const { tables, files } = parseSchema();
  const latest = files.at(-1)?.match(/^(V\d+)__/)[1] ?? "unknown";
  const rows = [...tables.values()].sort((a, b) => a.table.localeCompare(b.table));
  const lines = [];
  lines.push(START);
  lines.push("\n### 3A. 自动迁移隔离基线（由 `npm run docs:data-isolation-spec` 生成）\n");
  lines.push(`> 事实源：\`backend/src/main/resources/db/migration\`。当前解析到最新迁移 **${latest}**，发现 **${rows.length}** 张含 scope 字段的表。不要手工修改本区块；新增迁移后运行 \`npm run docs:data-isolation-spec\`，提交前运行 \`npm run verify:data-isolation-spec\`。\n`);
  lines.push("| 表 | scope 字段 | scope 类型 | 首次出现迁移 | 证据文件 |\n");
  lines.push("|---|---|---|---|---|\n");
  for (const row of rows) {
    const cols = [...row.columns.values()].sort((a, b) => a.column.localeCompare(b.column));
    const colText = cols.map((c) => `\`${c.column}\``).join(" + ");
    const versions = [...new Set(cols.flatMap((c) => [...c.versions]))].sort((a, b) => Number(a.slice(1)) - Number(b.slice(1)));
    const sources = [...new Set(cols.flatMap((c) => [...c.sources]))].sort();
    lines.push(`| \`${row.table}\` | ${colText} | ${scopeType()} | ${versions.join(", ")} | ${sources.map((s) => `\`${s}\``).join("<br>")} |\n`);
  }
  lines.push(`\n> 自动基线说明：本区块只证明“迁移中出现了 scope 字段”，不替代 \`verify:tenant-isolation\` 对 Repository scope 过滤的机审，也不替代越权 E2E/单测。\n`);
  lines.push(END);
  return lines.join("");
}

function insertOrReplace(content, block) {
  if (content.includes(START) && content.includes(END)) {
    const before = content.slice(0, content.indexOf(START));
    const after = content.slice(content.indexOf(END) + END.length);
    return `${before}${block}${after}`;
  }
  const marker = "## 4. 运行时越权拦截与验收标准";
  const idx = content.indexOf(marker);
  if (idx < 0) throw new Error(`cannot find insertion marker: ${marker}`);
  return `${content.slice(0, idx).trimEnd()}\n\n${block}\n\n${content.slice(idx)}`;
}

function main() {
  if (!fs.existsSync(specPath)) throw new Error(`data isolation spec not found: ${specPath}`);
  const content = fs.readFileSync(specPath, "utf8");
  const next = insertOrReplace(content, renderBlock());
  if (check) {
    if (content !== next) {
      console.error("data isolation spec is out of sync with Flyway migrations. Run: npm run docs:data-isolation-spec");
      process.exit(1);
    }
    console.log("data isolation spec is in sync with Flyway migration scope baseline.");
    return;
  }
  fs.writeFileSync(specPath, next);
  console.log("generated docs/data-isolation-and-tenant-scope-security-spec-v0.1.md automatic migration scope baseline");
}

main();
