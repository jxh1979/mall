#!/usr/bin/env node
// 门禁：三份业务最终解释权 HTML 与决策台账必须同一批改动内同步。
// 依据：用户 2026-08-03 拍板「台账保持完整，但改 HTML 必须在同一个 PR 内同步台账」。
// 病史：2026-07-22~08-01 三份 HTML 被 12 笔提交改过、台账停在改之前，挖出 36 条「已拍板未落 HTML」；
//       PR #326 合并后方向反转，台账 4 处反而落后。靠人记性两次都没守住，故加机械闸。
import { execSync } from "node:child_process";

const HTML = [
  "docs/business-system-map-for-operations-v0.1.html",
  "docs/platform-governance-map-for-operations-v0.1.html",
  "docs/business-enum-quick-reference-v0.1.html",
];
const LEDGER = "docs/prd-audit/business-decision-ledger.md";

const base = process.argv[2] || "origin/main";
let changed;
try {
  changed = execSync(`git diff --name-only ${base}...HEAD`, { encoding: "utf8" })
    .split("\n").map(s => s.trim()).filter(Boolean);
} catch {
  console.error(`[html-ledger-sync] 无法比对基线 ${base}，跳过（本地未 fetch 时属正常）`);
  process.exit(0);
}

const touchedHtml = HTML.filter(f => changed.includes(f));
if (touchedHtml.length === 0) {
  console.log("[html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。");
  process.exit(0);
}
if (changed.includes(LEDGER)) {
  console.log(`[html-ledger-sync] PASS: 改动了 ${touchedHtml.length} 份业务 HTML，决策台账已同步。`);
  process.exit(0);
}
console.error("[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。");
console.error("  已改动的 HTML：");
touchedHtml.forEach(f => console.error(`    - ${f}`));
console.error(`  必须在同一批改动内一并更新：${LEDGER}`);
console.error("  依据：用户 2026-08-03 拍板——台账保持完整，改 HTML 必须同 PR 同步台账。");
console.error("  若本次确为纯排版/错别字、不涉及口径，请在台账内追加一行说明后再提交。");
process.exit(1);
