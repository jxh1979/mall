#!/usr/bin/env node
// 门禁：三份业务最终解释权 HTML 的引用一律用「条目名」，不得再用行号。
// 依据：DEC-R8-17（用户 2026-08-06 裁 A：彻底废行号、只留条目名）。
// 病史：R8 两批 6 轮返工里有 3 轮因行号锚点漂移返工（760 行漂移 → 287 处重建 → +2 位移 19 处漏改）。
// 本门禁不依赖 git diff 基线，任何时候跑都是全量实检，不会空转。
//
// 检三件事：
//   ① 注册表 docs/prd-audit/entry-registry.csv 内，条目名在同一份 HTML 内唯一；
//      且每条的**整条正文**（entry_body 列，与本文件 flatEntry() 同源、去全部空白）逐字仍在
//      对应 HTML 现文里（注册表没跟上 HTML 改动即红）。
//      R12 之前该列存的是首句前 24 字指纹，条目中段被改动检不出，
//      R8-U-02「HTML 内容无保护落点」的缺口即由此而来。换成整条正文后，注册表全部行
//      （entry ＋ section）都进入逐字保护面，不再依赖该条目有没有账本行。
//   ② 全库 Markdown（历史留痕文件除外）里的 文件代号「名」 引用，
//      必须能在该 HTML 内唯一定位——要么等于注册表条目名，要么是恰好一条条目正文的子串。
//   ③ 正文里不得再出现「实测 LNNN」式行号锚点；
//   ④ 账本与承接矩阵两份机读台账的 entry_name 必须同名同源（矩阵是从账本抄过来的，抄错即红）；
//   ⑤ 账本每行 source_text 必须仍逐字存在于对应 HTML 现文里——这是**账本侧**的逐字实检；
//      **条目侧**的整条正文保护由检查① 承担，两者互补，不再有「未登记进账本就没人管」的条目。
//   ⑥ 账本每行必须在承接矩阵 docs/prd-audit/html-prd-coverage.csv 里有对应行
//      （反向：④ 只检「矩阵→账本」，缺了「账本→矩阵」这一向，账本新增条目可以长期不进矩阵而门禁不响）；
//      矩阵里 9 条 *-MPS-000N 占位伪 ID 按白名单放行（member-points 重建 Phase4 待映射，账本无对应行是设计如此）。
//   ⑦ 账本另外九个句子类镜像列（trigger / preconditions / state_transition / value_flow / permission /
//      approval / separation_of_duties / failure_recovery / evidence_required）同样逐字实检：
//      检查⑤ 只覆盖 source_text 一列，这九列在此之前命中数一直是 0——HTML 改了、这九列不跟，门禁不会响。
//      trigger 列是抽取时合成的触发场景概括，定性豁免；哨兵值 NOT_* 豁免；短于 12 字的片段不检。
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
  const entryBody = (r.entry_body || "").replace(/\s+/g, "");
  if (entryBody && !flat[code].includes(entryBody)) {
    errors.push(`注册表条目正文在 HTML 现文中找不到：${r.entry_id} ${code}「${r.entry_name}」——HTML 已改而注册表未重建`);
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
  // 逐行比 source_text 与 HTML 现文，挡住「HTML 改了台账没跟上」
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

// ---- ⑥ 账本每行必须在承接矩阵里有对应行（R10 新增）----
// 矩阵里的占位伪 ID：member-points 重建 Phase4 待映射，账本无对应行，按设计保留。
const FAKE_COVERAGE_IDS = new Set([
  "BSM-MPS-0001", "BSM-MPS-0002", "BSM-MPS-0003",
  "PGM-MPS-0001", "PGM-MPS-0002", "PGM-MPS-0003",
  "BEQ-MPS-0001", "BEQ-MPS-0002", "BEQ-MPS-0003",
]);
const covIdSet = new Set(covRows.map(r => r.rule_id));
for (const r of ledRows) {
  if (!covIdSet.has(r.rule_id)) {
    errors.push(`账本行未登记进承接矩阵：${r.rule_id}「${r.entry_name}」——账本与承接矩阵须一一对应（检查⑥）`);
  }
}
for (const r of covRows) {
  if (ledName[r.rule_id] === undefined && !FAKE_COVERAGE_IDS.has(r.rule_id)) {
    errors.push(`承接矩阵出现账本没有、又不在伪 ID 白名单里的 rule_id：${r.rule_id}（检查⑥）`);
  }
}

// ---- ⑦ 账本九个句子类镜像列逐字实检（R12 新增）----
// 检查⑤ 只管 source_text 一列。另外九列（trigger / preconditions / state_transition / value_flow /
// permission / approval / separation_of_duties / failure_recovery / evidence_required）从来没被机检过，
// 命中数一直是 0——HTML 改了、这九列不跟，门禁不会响。本检把它们纳入保护范围。
//
// 【片段分隔符口径：宽口径 /[｜；;\s]+/】
// 这九列的值是「从条目正文里挑若干句拼起来」，抽取时用过三种拼接符——全角竖线 ｜、分号 ；/;，
// 以及**一个普通空格**（被挑的句子在原文里不相邻时用它）。
// 例：BSM-0001 preconditions = source_text 的第 1 句 + " " + 第 3 句，中间第 2 句跳过。
// norm() 会把空格抹掉，于是两段不相邻的原文被焊成一条现文里根本不存在的串，纯属误报。
// 实测对比（本轮基线 a4f31f56，阈值 12）：
//   只切 ｜        ：checked=1663，非 trigger 失配 26
//   切 ｜；;        ：checked=1901，非 trigger 失配 17
//   切 ｜；; + 空白 ：checked=1965，非 trigger 失配 12   ← 取此口径
// 宽口径检的片段更多、误报更少，两头都占优，故取宽口径。
//
// 【trigger 列整列豁免】该列存的是抽取时**合成**的触发场景概括，设计上就不逐字复制原文
// （实测 301 处失配里 289 处落在这一列，占 96%）。对一个合成列做逐字检，等于要求它不再是概括，
// 属口径冲突而非数据错误，故定性豁免；其余八列逐字检。
//
// 【阈值 12 字】低于 12 字的片段多是「不用审批」「自动留痕」这类标签式短语，全库大量重复出现，
// 逐字命中与否不构成「账本跟丢了 HTML」的证据，检了只增噪声。
//
// 【尾部标点容差】单元格习惯以 。 收尾，而原文同一处常是 ；或 、。终止标点不承载规则语义，
// 比对前一律剥掉，免掉一整类纯标点误报。
const MIRROR_COLS = ["trigger", "preconditions", "state_transition", "value_flow",
  "permission", "approval", "separation_of_duties", "failure_recovery", "evidence_required"];
const SENTINEL = /^NOT_[A-Z0-9_-]*$/;   // NOT_STATED / NOT_EXTRACTED_R8 / _R8-H2 / _R9：原文本就没说，不是失配
const MIRROR_MIN = 12;
const flatNorm = Object.fromEntries(Object.entries(flat).map(([k, v]) => [k, norm(v)]));
let mirrorChecked = 0;
for (const r of ledRows) {
  if (r.entry_name === "ORPHANED") continue;   // 同检查⑤：现行 HTML 已无原文
  const code = fileOf[r.html_file];
  if (!code) continue;                          // 未知 HTML 文件已由检查④ 报错
  for (const col of MIRROR_COLS) {
    if (col === "trigger") continue;            // 见上：合成列，定性豁免
    const v = (r[col] || "").trim();
    if (!v || SENTINEL.test(v)) continue;
    for (const frag of v.split(/[｜；;\s]+/)) {
      const n = norm(frag).replace(/[。；;，、．.]+$/, "");
      if (n.length < MIRROR_MIN) continue;
      mirrorChecked++;
      if (!flatNorm[code].includes(n)) {
        errors.push(`账本镜像列与 HTML 现文对不上：${r.rule_id}「${r.entry_name}」${col} 片段「${frag.trim()}」——HTML 已改而账本未重取（检查⑦）`);
      }
    }
  }
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
console.log(`[entry-anchors] 注册表 ${rows.length} 行；账本 ${ledRows.length} 行；承接矩阵 ${covRows.length} 行（同名 ${covMapped}、账本中无此 rule_id ${covNotInLedger}）；账本镜像列片段 ${mirrorChecked} 条；被检 Markdown ${mdFiles.length} 份；条目名引用 ${refTotal} 处（注册表原名 ${refByName} 处，正文唯一定位 ${refByText} 处）。`);
if (warns.length) warns.forEach(w => console.log("  提示：" + w));
if (errors.length) {
  console.error(`[entry-anchors] FAIL：${errors.length} 处不合格。`);
  errors.slice(0, 60).forEach(e => console.error("  - " + e));
  if (errors.length > 60) console.error(`  ……另有 ${errors.length - 60} 处未列出。`);
  console.error("  处置：条目改名或 HTML 改动后，重建 docs/prd-audit/entry-registry.csv 并把引用改为现行条目名；一律不得写回行号。");
  process.exit(1);
}
console.log("[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。");
