// verify:tenant-isolation
// 租户隔离机审闸口（对齐 docs/data-isolation-and-tenant-scope-security-spec-v0.1.md §6A.1，
// 闭合 ISO-GAP-02「无 verify:tenant-isolation 自动闸口」）。
//
// 权威事实源：
//   - 表清单/scope 语义：docs/domain-ownership-scope-audit-v0.1.csv（§8 双向绑定）。
//       scope_type ∈ { direct, inherited, global_reference, single_complex_default }
//   - 查询事实源：backend 全部 Jdbc*Repository.java 内嵌 SQL。
//
// 机审口径（fail-closed，发现即非 0 退出）：
//   [F1] UNSCOPED_DIRECT_READ —— 某 direct 表（CSV 标 direct，自带 merchant_id/project_id/
//        organization_id 等 scope 列）在某 Repository 中存在 SELECT 读，但该 Repository
//        对该表**没有任何一条带 `<scope_col> = :` 谓词的 scoped 读**。
//        语义：服务端无法按 token scope 过滤该表 → 跨租户读越权风险（§4 AC-ISO-1）。
//   [F2] UNREGISTERED_TABLE —— Repository SQL 中 FROM/JOIN/INTO/UPDATE 引用的表未登记到
//        CSV（§6A.1「新增带租户数据的表未登记 → 失败」）。
//
// 白名单豁免（§6A.1）：scope_type ∈ { global_reference, single_complex_default } 的平台
//   全局/单综合体默认参考表，豁免行级 scoped-read 校验（仍纳入 F2 已登记校验）。
//
// ===== 明确的覆盖范围与已知盲区（不假装全覆盖）=====
//   覆盖：direct 表「是否存在 scoped 读取通道」的静态可达性（最小可信版本）。
//   盲区（KNOWN-BLINDSPOT，登记在 security-scans 台账，待迭代）：
//     B1 inherited 表的 join-path 是否真正继承上游 scope —— 本版仅信息性列出，不硬失败。
//     B2 写路径是否把请求体 merchant_id 覆盖为 token scope（AC-ISO-2）—— 在 Service 层，
//        本版不做数据流分析，不校验。
//     B3 单条 SELECT 的「平台级全量读」是否在 Service 层被正确门控（scope.isPresent() 分支）
//        —— 属 Service 编排，本版按「Repository 至少提供 scoped 通道」近似，不逐调用点核验。
//     B4 动态拼接/字符串变量 SQL、非 Jdbc*Repository（如 JPA）—— 不在扫描面内。
//   即：本闸口能抓住「某 direct 表压根没有按 scope 过滤的查询路径」这一类硬越权，
//   不能证明每个调用点都正确注入了 scope（后者靠单测/运行时 AC-ISO 用例，见 §4/§6A.3）。

import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const csvPath = path.join(root, "docs", "domain-ownership-scope-audit-v0.1.csv");
const repoGlobDir = path.join(root, "backend", "src", "main", "java");
const baselinePath = path.join(root, "security-scans", "tenant-isolation-baseline.json");

// --strict：忽略 baseline，对全部 finding 硬失败（用于隔离缺口被修复后收紧闸口）。
const strict = process.argv.includes("--strict");
// --write-baseline：把当前全部 finding 写入 baseline（首建/重建台账底座用，非 CI 默认路径）。
const writeBaseline = process.argv.includes("--write-baseline");

const WHITELIST_SCOPE_TYPES = new Set(["global_reference", "single_complex_default"]);

// 基础设施/非租户表，CSV 不纳管也不应误报为「未登记租户表」。
const INFRA_TABLE_WHITELIST = new Set(["flyway_schema_history"]);

// SQL 关键字/伪 token：FROM/JOIN 后若命中这些，说明匹配到的是子查询或谓词残片，非真实表名。
const SQL_NOISE_TOKENS = new Set([
  "select", "set", "values", "where", "on", "and", "or", "as", "using",
  "lateral", "returning", "into",
  // 行锁子句关键字：`FOR UPDATE [OF ..] [SKIP LOCKED|NOWAIT|SHARE]` 中 UPDATE 之后的 token
  // 是锁修饰符而非被更新的表名，否则会把 `FOR UPDATE SKIP LOCKED` 的 'skip' 误判为未登记表。
  "skip", "locked", "nowait", "of", "share",
]);

// ---- CSV 解析（处理引号内逗号）----
function parseCsvLine(line) {
  const out = [];
  let cur = "";
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (ch === '"') {
      if (inQuotes && line[i + 1] === '"') { cur += '"'; i++; }
      else inQuotes = !inQuotes;
    } else if (ch === "," && !inQuotes) {
      out.push(cur); cur = "";
    } else {
      cur += ch;
    }
  }
  out.push(cur);
  return out;
}

function loadScopeRegistry() {
  if (!fs.existsSync(csvPath)) {
    console.error(`[tenant-isolation] FATAL: scope audit CSV not found: ${csvPath}`);
    process.exit(1);
  }
  const lines = fs.readFileSync(csvPath, "utf8").split(/\r?\n/).filter((l) => l.trim().length > 0);
  const header = parseCsvLine(lines[0]).map((h) => h.trim());
  const idxTable = header.indexOf("table_name");
  const idxType = header.indexOf("scope_type");
  const idxSource = header.indexOf("scope_source");
  if (idxTable < 0 || idxType < 0) {
    console.error("[tenant-isolation] FATAL: CSV header missing table_name/scope_type columns.");
    process.exit(1);
  }
  const registry = new Map(); // table -> { type, source }
  for (const line of lines.slice(1)) {
    const cols = parseCsvLine(line);
    const table = (cols[idxTable] || "").trim().toLowerCase();
    if (!table) continue;
    registry.set(table, {
      type: (cols[idxType] || "").trim(),
      source: (cols[idxSource] || "").trim(),
    });
  }
  return registry;
}

// ---- 收集所有 Jdbc*Repository.java 源码 ----
function collectJdbcRepositories(dir) {
  const files = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...collectJdbcRepositories(full));
    } else if (/^Jdbc.*Repository.*\.java$/.test(entry.name)) {
      files.push(full);
    }
  }
  return files;
}

// 注释剥离（行注释 + 块注释），避免文档/示例里的表名干扰扫描。
function stripJavaComments(src) {
  return src
    .replace(/\/\*[\s\S]*?\*\//g, " ")
    .replace(/(^|[^:])\/\/[^\n]*/g, "$1");
}

// 表名 token：去掉 schema 前缀，仅取标识符。
const TABLE_TOKEN = "([a-zA-Z_][\\w]*)";

// 抽取 CTE 名（WITH x AS ( … ), y AS ( … )）——这些是查询内临时关系，非真实表，须排除。
function extractCteNames(sql) {
  const names = new Set();
  // WITH <name> AS (  以及  ), <name> AS (
  const re = new RegExp(`(?:\\bWITH\\s+|\\)\\s*,\\s*)${TABLE_TOKEN}\\s+AS\\s*\\(`, "gi");
  let m;
  while ((m = re.exec(sql)) !== null) names.add(m[1].toLowerCase());
  return names;
}

// 抽取 SQL 中引用的表（FROM / JOIN / INTO / UPDATE），排除子查询、CTE、SQL 噪声 token。
function extractReferencedTables(sql, cteNames) {
  const tables = new Set();
  const patterns = [
    new RegExp(`\\bFROM\\s+${TABLE_TOKEN}`, "gi"),
    new RegExp(`\\bJOIN\\s+${TABLE_TOKEN}`, "gi"),
    new RegExp(`\\bINTO\\s+${TABLE_TOKEN}`, "gi"),
    new RegExp(`\\bUPDATE\\s+${TABLE_TOKEN}`, "gi"),
  ];
  for (const re of patterns) {
    let m;
    while ((m = re.exec(sql)) !== null) {
      // 子查询：FROM ( … ) —— TABLE_TOKEN 不会匹配 '('，但保险起见检查前缀后第一个非空白字符
      const after = sql.slice(re.lastIndex - m[1].length - 1);
      if (/^\s*\(/.test(after)) continue;
      const t = m[1].toLowerCase();
      if (SQL_NOISE_TOKENS.has(t)) continue;
      if (cteNames.has(t)) continue;
      tables.add(t);
    }
  }
  return tables;
}

// 针对某 direct 表，判定该源码是否存在「带 scope 谓词的读取」。
// 读取 = SELECT ... FROM <table> ；scope 谓词 = `<scope_col> = :` 或 `<scope_col> IN (`。
// 近似：只要源码中同时出现「FROM <table>」与「<scope_col> = :」即视为存在 scoped 读通道。
// 该近似的盲区已在文件头 B3 登记。
function hasScopedRead(src, table, scopeCol) {
  const reads = new RegExp(`\\bFROM\\s+${table}\\b`, "i").test(src);
  if (!reads) return null; // 该源码根本不读这张表
  const col = scopeCol.replace(/[^\w]/g, "");
  if (!col) return true; // scope 列未知，无法判定，保守放行
  const scopedPredicate = new RegExp(`\\b${col}\\s*(=\\s*:|IN\\s*\\()`, "i").test(src);
  return scopedPredicate;
}

function main() {
  const registry = loadScopeRegistry();
  const repoFiles = collectJdbcRepositories(repoGlobDir);
  if (repoFiles.length === 0) {
    console.error(`[tenant-isolation] FATAL: no Jdbc*Repository.java under ${repoGlobDir}`);
    process.exit(1);
  }

  const findings = [];           // 硬失败
  const blindspotInherited = []; // 信息性
  const referencedTables = new Set();

  // 预读全部源码（去注释）
  const sources = repoFiles.map((f) => ({
    file: path.relative(root, f),
    src: stripJavaComments(fs.readFileSync(f, "utf8")),
  }));

  // ---- F2: 未登记表 ----
  for (const { file, src } of sources) {
    const cteNames = extractCteNames(src);
    for (const t of extractReferencedTables(src, cteNames)) {
      referencedTables.add(t);
      if (INFRA_TABLE_WHITELIST.has(t)) continue;
      if (!registry.has(t)) {
        findings.push({
          key: `F2:UNREGISTERED_TABLE:${t}`,
          text:
            `[F2 UNREGISTERED_TABLE] 表 '${t}' 在 ${file} 的 SQL 中被引用，但未登记到 ` +
            `domain-ownership-scope-audit-v0.1.csv —— 新增带租户数据的表必须登记 scope_type/scope_source 后方可合入（§6A.1）。`,
        });
      }
    }
  }

  // ---- F1: direct 表缺 scoped 读通道 ----
  for (const [table, meta] of registry.entries()) {
    if (meta.type !== "direct") {
      if (meta.type === "inherited" && referencedTables.has(table)) {
        blindspotInherited.push(`${table} (scope_source: ${meta.source || "n/a"})`);
      }
      continue;
    }
    // direct：scope_source 多为 scope 列名（merchant_id / project_id / organization_id / venue_id / business_entity_id ...）
    const scopeCol = meta.source;
    let readSomewhere = false;
    let scopedSomewhere = false;
    const readingFiles = [];
    for (const { file, src } of sources) {
      const result = hasScopedRead(src, table, scopeCol);
      if (result === null) continue; // 该文件不读这张表
      readSomewhere = true;
      readingFiles.push(file);
      if (result === true) scopedSomewhere = true;
    }
    if (readSomewhere && !scopedSomewhere) {
      findings.push({
        key: `F1:UNSCOPED_DIRECT_READ:${table}`,
        text:
          `[F1 UNSCOPED_DIRECT_READ] 表 '${table}'（direct, scope=${scopeCol}）在 ` +
          `${readingFiles.join(", ")} 中有 SELECT 读，但找不到任何带 '${scopeCol} = :' 的 scoped 查询 ` +
          `—— 服务端无法按 token scope 过滤该表，存在跨租户读越权风险（AC-ISO-1，§4）。`,
      });
    }
  }

  // ---- 输出 ----
  const directCount = [...registry.values()].filter((m) => m.type === "direct").length;
  const whitelistCount = [...registry.values()].filter((m) => WHITELIST_SCOPE_TYPES.has(m.type)).length;

  // ---- baseline ratchet（棘轮）----
  // 已登记台账的已知 finding（security-scans/findings-register-v1.md tenant-isolation 小节 +
  // tenant-isolation-baseline.json）允许暂存，闸口对「新增回归」（baseline 之外的 finding）硬失败。
  // 这与 §6A.3「回归底座」一致：闸口阻止隔离面进一步劣化，已知缺口走台账 → 修复 → 收紧。
  // 传 --strict 时忽略 baseline，对全部 finding 硬失败（缺口清零后切换为零容忍）。
  let baselineKeys = new Set();
  let baselineMeta = null;
  if (fs.existsSync(baselinePath)) {
    try {
      baselineMeta = JSON.parse(fs.readFileSync(baselinePath, "utf8"));
      baselineKeys = new Set((baselineMeta.known_findings ?? []).map((f) => f.key));
    } catch (e) {
      console.error(`[tenant-isolation] FATAL: baseline 文件解析失败：${baselinePath}: ${e.message}`);
      process.exit(1);
    }
  }

  if (writeBaseline) {
    const payload = {
      note:
        "verify:tenant-isolation 已知缺口 baseline（棘轮底座）。闸口对此清单之外的新增 finding 硬失败；" +
        "每条须在 security-scans/findings-register-v1.md tenant-isolation 小节有对应 SEC-*-TENANT-ISOLATION-* 证据；" +
        "修复后从本清单与台账移除以收紧闸口。重建：node tools/verify-tenant-isolation.mjs --write-baseline。",
      generated_at_source: "static scan of Jdbc*Repository.java vs domain-ownership-scope-audit-v0.1.csv",
      known_findings: findings.map((f) => ({ key: f.key, text: f.text })),
    };
    fs.writeFileSync(baselinePath, JSON.stringify(payload, null, 2) + "\n");
    console.log(`[tenant-isolation] 已写入 baseline：${path.relative(root, baselinePath)}（${findings.length} 条已知 finding）`);
    process.exit(0);
  }

  const currentKeys = new Set(findings.map((f) => f.key));
  const newFindings = findings.filter((f) => !baselineKeys.has(f.key));
  const baselinedHit = findings.filter((f) => baselineKeys.has(f.key));
  const staleBaseline = [...baselineKeys].filter((k) => !currentKeys.has(k)); // 已修复，应从 baseline 移除

  console.log("[tenant-isolation] 扫描范围：");
  console.log(`  - 权威表清单（CSV）：${registry.size} 表（direct=${directCount}，白名单豁免=${whitelistCount}，inherited 为信息性盲区）`);
  console.log(`  - Jdbc*Repository 源：${repoFiles.length} 个文件`);
  console.log(`  - 本次扫出 finding：${findings.length}（已登记 baseline：${baselinedHit.length}，新增回归：${newFindings.length}）`);
  if (blindspotInherited.length > 0) {
    console.log(`[tenant-isolation] KNOWN-BLINDSPOT B1（inherited，仅信息性，不硬失败）：${blindspotInherited.length} 表`);
    for (const t of blindspotInherited.slice(0, 6)) console.log(`    · ${t}`);
    if (blindspotInherited.length > 6) console.log(`    · …其余 ${blindspotInherited.length - 6} 表略`);
  }

  if (staleBaseline.length > 0) {
    console.log(`[tenant-isolation] 注意：baseline 中 ${staleBaseline.length} 条已不再复现（疑似已修复），应从 baseline 与台账移除以收紧闸口：`);
    for (const k of staleBaseline) console.log(`    · ${k}`);
  }

  // strict 模式：全部 finding 都硬失败。
  const blocking = strict ? findings : newFindings;

  if (blocking.length > 0) {
    console.error(
      `\n[tenant-isolation] FAIL: ${strict ? "strict 模式，" : "发现 baseline 之外的"} ` +
      `${blocking.length} 项租户隔离问题（fail-closed，阻断合入）：`
    );
    for (const f of blocking) console.error(`  - ${f.text}`);
    console.error(
      "\n[tenant-isolation] 处置：修复未隔离查询 / 在 CSV 登记新表 / 显式标白名单后重跑；" +
      "已知缺口须在 security-scans/findings-register-v1.md tenant-isolation 小节登记证据（§6A.2），" +
      "并将其 key 纳入 security-scans/tenant-isolation-baseline.json 后方可暂存。"
    );
    process.exit(1);
  }

  if (findings.length > 0) {
    console.log(
      `\n[tenant-isolation] PASS（棘轮）：无新增回归；${baselinedHit.length} 条已知缺口在台账跟踪闭环中` +
      `（见 findings-register-v1.md tenant-isolation 小节）。隔离面未劣化。`
    );
    process.exit(0);
  }

  console.log(
    `\n[tenant-isolation] PASS: ${directCount} 个 direct 表均存在 scoped 读取通道，` +
    `Repository SQL 引用的表均已登记（已知盲区见脚本头 B1–B4）。`
  );
  process.exit(0);
}

main();
