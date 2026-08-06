#!/usr/bin/env node
// 门禁：三份业务最终解释权 HTML 的引用一律用「条目名」，不得再用行号。
// 依据：DEC-R8-17（用户 2026-08-06 裁 A：彻底废行号、只留条目名）。
// 病史：R8 两批 6 轮返工里有 3 轮因行号锚点漂移返工（760 行漂移 → 287 处重建 → +2 位移 19 处漏改）。
// 本门禁不依赖 git diff 基线，任何时候跑都是全量实检，不会空转。
//
// 检三件事：
//   ① 注册表 docs/prd-audit/entry-registry.csv 内，条目名在同一份 HTML 内唯一；
//      且每条的首句指纹能在对应 HTML 现文中命中（注册表没跟上 HTML 改动即红）。
//   ② 全库 Markdown（历史留痕文件除外）里的 文件代号「名」 引用，
//      必须能在该 HTML 内唯一定位——要么等于注册表条目名，要么是恰好一条条目正文的子串。
//   ③ 正文里不得再出现「实测 LNNN」式行号锚点；
//   ④ 账本与承接矩阵两份机读台账的 entry_name 必须同名同源（矩阵是从账本抄过来的，抄错即红）；
//   ⑤ **对已登记进账本的条目**逐字实检：账本每行 source_text 必须仍逐字存在于对应 HTML 现文里
//      （指纹只看前 24 字，挡不住行中间改动，故另设此检）。**未登记进账本的 HTML 条目不在本检保护范围内**——
//      改了这类条目本门禁不会红；账本扩登记是 R8-U-02 的待办。
import fs from "node:fs";
import path from "node:path";

const HTML = {
  BSM: "docs/business-system-map-for-operations-v0.1.html",
  PGM: "docs/platform-governance-map-for-operations-v0.1.html",
  ENUM: "docs/business-enum-quick-reference-v0.1.html",
};
const REGISTRY = "docs/prd-audit/entry-registry.csv";
const LEDGER = "docs/prd-audit/authority-rule-ledger.csv";
const COVERAGE = "docs/prd-audit/html-prd-coverage.csv";
// 历史留痕文件：记录的是「当时」的事实，一律不改、不检
const EXCLUDE = /(changelog|change-log|-report|\.report\.|diff-matrix|anchor-rebuild|html-map|reanchor|html-mirror-scan|history-index|[\\/]rounds[\\/])/i;

const errors = [];
const warns = [];

function readCsv(file) {
  let t = fs.readFileSync(file, "utf8");
  if (t.charCodeAt(0) === 0xfeff) t = t.slice(1);
  const rows = [];
  let row = [], cell = "", q = false;
  for (let i = 0; i < t.length; i++) {
    const c = t[i];
    if (q) {
      if (c === '"') { if (t[i + 1] === '"') { cell += '"'; i++; } else q = false; }
      else cell += c;
    } else if (c === '"') q = true;
    else if (c === ",") { row.push(cell); cell = ""; }
    else if (c === "\n") { row.push(cell); rows.push(row); row = []; cell = ""; }
    else if (c !== "\r") cell += c;
  }
  if (cell !== "" || row.length) { row.push(cell); rows.push(row); }
  const head = rows.shift();
  return rows.filter(r => r.length === head.length).map(r => Object.fromEntries(head.map((h, i) => [h, r[i]])));
}

function plainText(file) {
  return fs.readFileSync(file, "utf8")
    .replace(/<!--[\s\S]*?-->/g, " ")
    .replace(/<style[\s\S]*?<\/style>/gi, " ")
    .replace(/<script[\s\S]*?<\/script>/gi, " ")
    .replace(/<[^>]*>/g, " ")
    .replace(/&nbsp;/g, " ").replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"')
    .replace(/\s+/g, "");
}

// 引号写法在 PRD 与 HTML 之间历来混用（「」『』“”""），比对前一律剥掉，只比文字
function norm(s) {
  return s.replace(/[「」『』“”"'‘’]/g, "").replace(/\s+/g, "");
}

function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) { if (e.name === "node_modules" || e.name === "_archive") continue; walk(p, out); }
    else if (e.name.endsWith(".md")) out.push(p);
  }
  return out;
}

// ---- ① 注册表自身 ----
const rows = readCsv(REGISTRY);
const fileOf = Object.fromEntries(Object.entries(HTML).map(([k, v]) => [path.basename(v), k]));
const flat = Object.fromEntries(Object.entries(HTML).map(([k, v]) => [k, plainText(v)]));
const byCode = { BSM: [], PGM: [], ENUM: [] };
const nameSeen = {};
for (const r of rows) {
  const code = fileOf[r.html_file];
  if (!code) { errors.push(`注册表出现未知 HTML 文件：${r.html_file}（${r.entry_id}）`); continue; }
  const key = code + "|" + r.entry_name;
  if (nameSeen[key]) errors.push(`条目名在同一份 HTML 内重复：${code}「${r.entry_name}」（${nameSeen[key]} 与 ${r.entry_id}）`);
  nameSeen[key] = r.entry_id;
  const fp = (r.first_sentence_fingerprint || "").replace(/\s+/g, "");
  if (fp && !flat[code].includes(fp)) {
    errors.push(`注册表指纹在 HTML 现文中找不到：${r.entry_id} ${code}「${r.entry_name}」——HTML 已改而注册表未重建`);
  }
  byCode[code].push(r);
}

const entryTextCache = new Map();
function flatEntry(code, r) {
  const k = r.entry_id;
  if (!entryTextCache.has(k)) {
    const lines = fs.readFileSync(HTML[code], "utf8").split("\n");
    const start = Number(r.line_now) - 1;
    let end = start;
    const raw = lines[start] || "";
    if (/<tr[\s>]/.test(raw) && !/<\/tr>/.test(raw)) { while (end < lines.length && !/<\/tr>/.test(lines[end])) end++; }
    const seg = lines.slice(start, end + 1).join(" ")
      .replace(/<!--[\s\S]*?-->/g, " ").replace(/<[^>]*>/g, " ")
      .replace(/&nbsp;/g, " ").replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"')
      .replace(/\s+/g, "");
    entryTextCache.set(k, seg);
  }
  return entryTextCache.get(k);
}

// ---- ④ 账本 / 承接矩阵的条目名同源 ----
const ledRows = readCsv(LEDGER);
const ledName = {};
for (const r of ledRows) {
  ledName[r.rule_id] = r.entry_name;
  if (r.entry_name === "ORPHANED") continue;          // R8 保留的口径变更史，现行 HTML 已无原文
  if (!nameSeen[fileOf[r.html_file] + "|" + r.entry_name]) {
    errors.push(`账本条目名不在注册表里：${r.rule_id} ${r.html_file}「${r.entry_name}」`);
    continue;
  }
  // 指纹只看前 24 字，挡不住行中间的改动；这里逐行比 source_text 与 HTML 现文，才真正挡住「HTML 改了台账没跟上」
  const st = norm(r.source_text);
  if (st && !norm(flat[fileOf[r.html_file]]).includes(st)) {
    errors.push(`账本正文与 HTML 现文对不上：${r.rule_id}「${r.entry_name}」——HTML 已改而账本 source_text 未重取`);
  }
}
const covRows = readCsv(COVERAGE);
let covMapped = 0, covNotInLedger = 0;
for (const r of covRows) {
  if (r.entry_name === "NOT_IN_LEDGER") {
    if (ledName[r.rule_id] !== undefined) errors.push(`承接矩阵 ${r.rule_id} 标 NOT_IN_LEDGER，但账本里有这条`);
    covNotInLedger++; continue;
  }
  if (ledName[r.rule_id] === undefined) { errors.push(`承接矩阵 ${r.rule_id} 在账本中不存在，却写了条目名「${r.entry_name}」`); continue; }
  if (ledName[r.rule_id] !== r.entry_name) {
    errors.push(`承接矩阵与账本条目名不一致：${r.rule_id} 矩阵「${r.entry_name}」≠ 账本「${ledName[r.rule_id]}」`);
    continue;
  }
  covMapped++;
}

// ---- ②③ 全库引用 ----
const mdFiles = walk("docs").filter(f => !EXCLUDE.test(f.replace(/\\/g, "/")));
const REF = /(BSM|PGM|ENUM|BEQ)\s*「([^」]{2,})」/g;
const LINE_ANCHOR = /实测\s*\*{0,2}L\d+/g;   // 加粗写法（实测 **L123**）同样算行号锚点
let refTotal = 0, refByName = 0, refByText = 0;
for (const f of mdFiles) {
  const txt = fs.readFileSync(f, "utf8");
  const rel = f.replace(/\\/g, "/");
  const la = txt.match(LINE_ANCHOR);
  if (la) errors.push(`${rel}：仍有行号锚点 ${[...new Set(la)].slice(0, 5).join("、")}（共 ${la.length} 处）`);
  let m;
  REF.lastIndex = 0;
  while ((m = REF.exec(txt))) {
    const code = m[1] === "BEQ" ? "ENUM" : m[1];
    const name = m[2];
    refTotal++;
    if (nameSeen[code + "|" + name]) { refByName++; continue; }
    // 省略号引用（头……尾）算作对同一条的引用：各段必须同时出现在该条正文里
    const parts = name.split(/……|…|\.\.\./).map(norm).filter(p => p.length >= 2);
    const hitAll = (t) => parts.every(p => t.includes(p));
    const hits = byCode[code].filter(r => hitAll(norm(r.entry_name)));
    const textHits = hits.length ? hits : byCode[code].filter(r => hitAll(norm(flatEntry(code, r))));
    if (textHits.length === 1) { refByText++; continue; }
    const at = txt.slice(0, m.index).split("\n").length;
    if (textHits.length === 0) errors.push(`${rel}:${at}：${code}「${name}」在该 HTML 中定位不到任何条目`);
    else errors.push(`${rel}:${at}：${code}「${name}」在该 HTML 中命中 ${textHits.length} 条，无法唯一定位`);
  }
}
console.log(`[entry-anchors] 注册表 ${rows.length} 行；账本 ${ledRows.length} 行；承接矩阵 ${covRows.length} 行（同名 ${covMapped}、账本中无此 rule_id ${covNotInLedger}）；被检 Markdown ${mdFiles.length} 份；条目名引用 ${refTotal} 处（注册表原名 ${refByName} 处，正文唯一定位 ${refByText} 处）。`);
if (warns.length) warns.forEach(w => console.log("  提示：" + w));
if (errors.length) {
  console.error(`[entry-anchors] FAIL：${errors.length} 处不合格。`);
  errors.slice(0, 60).forEach(e => console.error("  - " + e));
  if (errors.length > 60) console.error(`  ……另有 ${errors.length - 60} 处未列出。`);
  console.error("  处置：条目改名或 HTML 改动后，重建 docs/prd-audit/entry-registry.csv 并把引用改为现行条目名；一律不得写回行号。");
  process.exit(1);
}
console.log("[entry-anchors] PASS：条目名唯一、引用可唯一定位、正文无行号锚点、账本已登记条目正文与 HTML 现文一致（未登记条目不在保护范围，见 R8-U-02）。");
