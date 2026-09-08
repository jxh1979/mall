#!/usr/bin/env node
// 文件版本：V0.01
// 门禁：三份业务最终解释权 HTML 与决策台账必须同一批改动内同步。
// 依据：用户 2026-08-03 拍板「台账保持完整，但改 HTML 必须在同一个 PR 内同步台账」。
// 病史：2026-07-22~08-01 三份 HTML 被 12 笔提交改过、台账停在改之前，挖出 36 条「已拍板未落 HTML」；
//       PR #326 合并后方向反转，台账 4 处反而落后。靠人记性两次都没守住，故加机械闸。
// 2026-08-07 门禁升级批 1（用户弹窗批立项）修两处空转假绿：
//   ① 只比对提交区 → 改 HTML 但没 commit 时门禁看不见，一路绿到 push 前。现按工作区脏净自动切范围。
//   ② 取不到基线时「跳过 exit 0」→ 无法判定不等于通过，改为 exit 1 并提示先 fetch。
import { execSync } from "node:child_process";

const git = (cmd) => execSync(`git ${cmd}`, { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 });

// 一律切到仓根再干活：在子目录里跑时 git 只看当前目录，会静默变绿。
try {
  process.chdir(git("rev-parse --show-toplevel").trim());
} catch {
  console.error("[html-ledger-sync] FAIL: 当前目录不在 git 仓库内，无法定位仓根。");
  process.exit(1);
}

const HTML = [
  "docs/business-system-map-for-operations-v0.1.html",
  "docs/platform-governance-map-for-operations-v0.1.html",
  "docs/business-enum-quick-reference-v0.1.html",
];
const LEDGER = "docs/prd-audit/business-decision-ledger.md";

// CI 提供事件发生前的基线；push main 后 origin/main 已指向 HEAD。
const base = process.argv[2] || process.env.HTML_GATES_BASE || "origin/main";
// 基线取不到就必须红：本门禁唯一的作用就是判定「改没改 HTML」，判不了不等于没改。
let mergeBase;
try {
  mergeBase = git(`merge-base ${base} HEAD`).trim();
} catch {
  console.error(`[html-ledger-sync] FAIL: 取不到基线 ${base}，无法判定本次改动范围。`);
  console.error("  无法判定不等于通过。请先 git fetch origin（CI 侧 actions/checkout 需 fetch-depth: 0）。");
  console.error("  如需换基线：node tools/verify-html-ledger-sync.mjs <基线ref>");
  process.exit(1);
}

// 三份 HTML 有未提交改动 → 连工作区一起比（基线↔工作区，含已提交的那部分）；
// 干净 → 维持原提交区口径 base...HEAD。
const dirty = git(`status --porcelain -- ${HTML.join(" ")}`).trim() !== "";
const range = dirty ? mergeBase : `${base}...HEAD`;
const scope = dirty
  ? `工作区（含未提交改动）↔ 基线 ${base}@${mergeBase.slice(0, 8)}`
  : `提交区 ${base}...HEAD（工作区三份 HTML 无未提交改动）`;

const changed = git(`diff --name-only ${range}`)
  .split("\n").map(s => s.trim()).filter(Boolean);

const touchedHtml = HTML.filter(f => changed.includes(f));
if (touchedHtml.length === 0) {
  console.log(`[html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。比对范围：${scope}。`);
  process.exit(0);
}
if (changed.includes(LEDGER)) {
  console.log(`[html-ledger-sync] PASS: 改动了 ${touchedHtml.length} 份业务 HTML，决策台账已同步。比对范围：${scope}。`);
  process.exit(0);
}
console.error(`[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：${scope}。`);
console.error("  已改动的 HTML：");
touchedHtml.forEach(f => console.error(`    - ${f}`));
console.error(`  必须在同一批改动内一并更新：${LEDGER}`);
console.error("  依据：用户 2026-08-03 拍板——台账保持完整，改 HTML 必须同 PR 同步台账。");
console.error("  若本次确为纯排版/错别字、不涉及口径，请在台账内追加一行说明后再提交。");
process.exit(1);
