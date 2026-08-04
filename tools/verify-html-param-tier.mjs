#!/usr/bin/env node
// 门禁（仅警告，不阻断）：三份业务最终解释权 HTML 中新增的业务参数，应当标出所属层级。
// 依据：用户 2026-08-03 拍板——参数分三层（法定值不可配／安全阈值可配但须最高权限审批并留痕／
//       其余业务参数一律可配），且三层一律在参数原句里字面标出，不设临场判定。
// 为什么只警告不阻断：机械判断「这个数字是不是业务参数」做不准，硬拦会误伤引用法条编号、
//       「三种情形」这类计数表述。做成警告让人看一眼，比做成拦截然后被加一堆例外要实用。
import { execSync } from "node:child_process";

const HTML = [
  "docs/business-system-map-for-operations-v0.1.html",
  "docs/platform-governance-map-for-operations-v0.1.html",
  "docs/business-enum-quick-reference-v0.1.html",
];

// 具体数值 + 单位：形如 30 天、10%、3 个工作日、2 次、100 分、0.7
const VALUE = /(\d+(?:\.\d+)?)\s*(%|个工作日|工作日|个月|分钟|小时|天|月|年|次|杯|元|分)/;
// 层级标注词：出现任一即视为已标注
const TIER = /(法定值|安全阈值|业务参数|可配|出厂默认)/;

const base = process.argv[2] || "origin/main";
let diff;
try {
  diff = execSync(`git diff --unified=0 ${base}...HEAD -- ${HTML.join(" ")}`, {
    encoding: "utf8",
    maxBuffer: 32 * 1024 * 1024,
  });
} catch {
  console.error(`[html-param-tier] 无法比对基线 ${base}，跳过（本地未 fetch 时属正常）`);
  process.exit(0);
}

if (!diff.trim()) {
  console.log("[html-param-tier] PASS: 本次未改动业务最终解释权 HTML。");
  process.exit(0);
}

let file = null;
const hits = [];
for (const line of diff.split("\n")) {
  if (line.startsWith("+++ b/")) {
    file = line.slice(6).trim();
    continue;
  }
  if (!line.startsWith("+") || line.startsWith("+++")) continue;
  const text = line.slice(1).replace(/<[^>]*>/g, "");
  if (!VALUE.test(text)) continue;
  if (TIER.test(text)) continue;
  hits.push({ file, text: text.trim() });
}

if (hits.length === 0) {
  console.log("[html-param-tier] PASS: 新增内容中带具体数值的语句都已标出层级。");
  process.exit(0);
}

console.warn(`[html-param-tier] 警告 ${hits.length} 处：新增语句含具体数值，但同句未标出层级。`);
console.warn("  三层写法：（法定值，不可配）／（安全阈值，按X可配，调整须最高权限审批并留痕）／（业务参数，按X可配，出厂默认 N）");
for (const h of hits) {
  console.warn(`  - ${h.file}`);
  console.warn(`      ${h.text.slice(0, 160)}${h.text.length > 160 ? "…" : ""}`);
}
console.warn("  若确为法条编号、分类计数等非参数表述，忽略即可——本门禁不阻断。");
process.exit(0);
