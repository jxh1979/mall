#!/usr/bin/env node
// 文件版本：V0.02
// 门禁：三份业务最终解释权 HTML 里带具体数值的业务句，必须在原句里标出所属层级。
// 依据：用户 2026-08-03 拍板——参数分三层（法定值不可配／安全阈值可配但须最高权限审批并留痕／
//       其余业务参数一律可配），且三层一律在参数原句里字面标出，不设临场判定。
// 2026-08-07 门禁升级批 1（用户弹窗批立项）：由「只警告永不阻断」改为「棘轮阻断」。
//   ① 先剥掉 <style> 块与 HTML 注释再取正文——原版把 CSS 里的 width: 100%、border-radius: 50% 之类
//      当成业务参数：三份 HTML 全量实测 18 条命中里 9 条是 CSS，真业务句只有 9 条。
//      正是这一半噪声让人相信「这门禁只会瞎报」，于是它响了也没人看，等于没有。
//   ② 存量未标注句登记进 tools/html-param-tier-baseline.json：命中基线内 → 警告放行；基线外 → 阻断。
//      基线只许缩不许增，且该约束由本脚本机检（拿基线 commit 里的同一份文件比 key 集合，多出来即红）——
//      否则 --write-baseline 跑一遍就能给新违规发通行证，棘轮等于没上。
//   ③ 行内豁免：<!-- param-tier:ignore 0 元;1 月 --> 只豁免标记里列出的数值，同行其余数值照检；
//      不列数值的裸标记不再整行豁免（DEC-R8-81 豁免须带数值；2026-09-03 M001 审计改为阻断）——
//      整行豁免等于给这行以后新增的数值也预先发了通行证。
//   ④ 取不到基线一律红——无法判定不等于通过。
//   ⑤ 改动了 HTML 就全量重扫这几份文件（不只看 diff 增行）：句子被就地改数值、被挪位置都逃不掉。
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import crypto from "node:crypto";

const git = (...args) => execFileSync("git", args, { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 });

// 一律切到仓根再干活：在子目录里跑时相对路径读不到文件、git 也只看当前目录，会静默变绿。
try {
  process.chdir(git("rev-parse", "--show-toplevel").trim());
} catch {
  console.error("[html-param-tier] FAIL: 当前目录不在 git 仓库内，无法定位仓根。");
  process.exit(1);
}

const HTML = [
  "docs/business-system-map-for-operations-v0.1.html",
  "docs/platform-governance-map-for-operations-v0.1.html",
  "docs/business-enum-quick-reference-v0.1.html",
];
const BASELINE = "tools/html-param-tier-baseline.json";

// 具体数值 + 单位：形如 30 天、10%、3 个工作日、2 次、100 分、0.7
const VALUE = /(\d+(?:\.\d+)?)\s*(%|个工作日|工作日|个月|分钟|小时|天|月|年|次|杯|元|分)/g;
// 层级标注词：出现任一即视为已标注
const TIER = /(法定值|安全阈值|业务参数|(?<!不)可配|出厂默认)/;
// 行内豁免标记，可带数值清单：<!-- param-tier:ignore 0 元;1 月 -->
const IGNORE = /param-tier:ignore([^>]*)/;

// 取出一段文字里所有「数值+单位」，去掉空格后比较（「0 元」与「0元」视为同一个）
const valuesIn = (s) => (s.match(VALUE) || []).map((v) => v.replace(/\s+/g, ""));
const norm = (s) => s.replace(/<[^>]*>/g, " ").replace(/\s+/g, " ").trim();
const keyOf = (file, text) =>
  crypto.createHash("sha1").update(`${file}\n${text}`).digest("hex").slice(0, 16);

// 全量扫一份 HTML，返回「带数值但没标层级」的句子。稳定键=文件+归一化句子哈希，不含行号
// （DEC-R8-17 已废行号锚点，基线用行号必然随改动漂移）。
function scan(file) {
  const raw = fs.readFileSync(file, "utf8");
  // 屏蔽成等量空白保留偏移，豁免标记从同一逻辑块的原文取。
  const masked = raw
    .replace(/<style[\s\S]*?<\/style>/gi, (m) => m.replace(/[^\n]/g, " "))
    .replace(/<!--[\s\S]*?-->/g, (m) => m.replace(/[^\n]/g, " "));
  // ponytail: 按本仓段落/列表项/表格行/容器/标题分块；复杂嵌套需求再采用 HTML 解析器。
  const starts = [0, ...Array.from(masked.matchAll(/<(?:p|li|tr|div|h[1-6])(?:\s|>)/gi), m => m.index), masked.length];
  const seen = new Set();
  const hits = [];
  starts.slice(0, -1).forEach((start, i) => {
    const text = norm(masked.slice(start, starts[i + 1]));
    if (!text || TIER.test(text)) return;
    let vals = valuesIn(text);
    if (!vals.length) return;
    const mark = IGNORE.exec(raw.slice(start, starts[i + 1]));
    if (mark) {
      const listed = valuesIn(mark[1]);
      if (!listed.length) {
        // DEC-R8-81：豁免只能带数值。裸标记不再整行放行，按未标层级句处理（M001 审计 2026-09-03）。
        console.error(
          `[html-param-tier] ${file} 有一处裸豁免（不列数值，含 ${vals.join("、")}）：` +
          `请改成带数值豁免 <!-- param-tier:ignore ${vals.join(";")} -->。`
        );
      } else {
        vals = vals.filter((v) => !listed.includes(v));
        if (!vals.length) return;
      }
    }
    const key = keyOf(file, text);
    if (seen.has(key)) return;
    seen.add(key);
    hits.push({ file, key, text, vals });
  });
  return hits;
}

function readBaseline(json, where) {
  try {
    const parsed = JSON.parse(json);
    if (!Array.isArray(parsed.entries)) throw new Error("entries 不是数组");
    return parsed;
  } catch (e) {
    console.error(`[html-param-tier] FAIL: ${where}的基线文件读不动（${e.message}）。`);
    console.error(`  期望格式 {"entries":[{"file":…,"key":…,"text":…}]}；重建：node tools/verify-html-param-tier.mjs --write-baseline`);
    process.exit(1);
  }
}

// 基线重建：node tools/verify-html-param-tier.mjs --write-baseline
// 只在「用户批准的补标草案落地后、存量条数变少」时用；想靠它给新违规发通行证会被下面的只缩不增机检拦住。
if (process.argv.includes("--write-baseline")) {
  const entries = HTML.flatMap(scan).map(({ file, key, text }) => ({ file, key, text }));
  const json = {
    _说明: [
      "存量待补层级标注的句子清单（门禁 tools/verify-html-param-tier.mjs 用）。",
      "草案批准一条删一条，只许缩不许增：新写的带数值业务句必须当场标层级，不得往这里补登记。",
      "只缩不增由门禁机检（与基线 commit 里的同名文件比 key 集合），手工加 key 会被拦下。",
      "稳定键 key = sha1(文件路径 + 换行 + 归一化句子).slice(0,16)，不含行号（DEC-R8-17 已废行号锚点）。",
      "命中基线内 → 警告放行；基线外 → 阻断。",
    ],
    entries,
  };
  fs.writeFileSync(BASELINE, JSON.stringify(json, null, 2) + "\n");
  console.log(`[html-param-tier] 已重建基线 ${BASELINE}：${entries.length} 条。请人工核对条数只减不增。`);
  process.exit(0);
}

// CI 提供事件发生前的基线；push main 后 origin/main 已指向 HEAD。
const base = process.argv[2] || process.env.HTML_GATES_BASE || "origin/main";

let mergeBase;
try {
  mergeBase = git("merge-base", base, "HEAD").trim();
} catch {
  console.error(`[html-param-tier] FAIL: 取不到基线 ${base}，无法判定本次改动范围。`);
  console.error("  无法判定不等于通过。请先 git fetch origin（CI 侧 actions/checkout 需 fetch-depth: 0）。");
  console.error("  如需换基线：node tools/verify-html-param-tier.mjs <基线ref>");
  process.exit(1);
}

const local = readBaseline(fs.readFileSync(BASELINE, "utf8"), "本地");

// 棘轮只缩不增机检：本地基线出现基线 commit 所没有的 key，一律红。
// 基线 commit 上没有这份文件 = 本批首次引入，放行。
let prevJson = null;
try {
  // 用 stdio 选项而不是 shell 重定向：Windows 上 cmd 会把 2>/dev/null 当成路径而报错，导致棘轮比较被静默跳过。
  prevJson = execFileSync("git", ["show", `${mergeBase}:${BASELINE}`], { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] });
} catch {
  prevJson = null;
}
if (prevJson !== null) {
  const prevKeys = new Set(readBaseline(prevJson, `基线 ${mergeBase.slice(0, 8)} `).entries.map((e) => e.key));
  const added = local.entries.filter((e) => !prevKeys.has(e.key));
  if (added.length) {
    console.error(`[html-param-tier] FAIL: 存量基线只许缩不许增，本次多出 ${added.length} 条 key。`);
    console.error(`  基线 ${base}@${mergeBase.slice(0, 8)} 有 ${prevKeys.size} 条，本地 ${BASELINE} 有 ${local.entries.length} 条。`);
    added.forEach((e) => console.error(`  + ${e.key} ${e.file}\n      ${String(e.text).slice(0, 100)}`));
    console.error("  新写的带数值业务句必须当场标层级或用 param-tier:ignore 豁免，不得补登记进基线。");
    process.exit(1);
  }
}

const dirty = git("status", "--porcelain", "--", ...HTML).trim() !== "";
const range = dirty ? mergeBase : `${base}...HEAD`;
const scope = dirty
  ? `工作区（含未提交改动）↔ 基线 ${base}@${mergeBase.slice(0, 8)}`
  : `提交区 ${base}...HEAD（工作区三份 HTML 无未提交改动）`;

const changed = git("diff", "--name-only", range)
  .split("\n").map((s) => s.trim()).filter(Boolean);
const touched = HTML.filter((f) => changed.includes(f));

if (touched.length === 0) {
  console.log(`[html-param-tier] PASS: 本次未改动业务最终解释权 HTML。比对范围：${scope}。`);
  process.exit(0);
}

const known = new Set(local.entries.map((e) => e.key));
const hits = touched.flatMap(scan);
const legacy = hits.filter((h) => known.has(h.key));
const fresh = hits.filter((h) => !known.has(h.key));
const show = (h) =>
  `  - ${h.file}（未标层级的数值：${h.vals.join("、")}）\n      ${h.text.slice(0, 160)}${h.text.length > 160 ? "…" : ""}`;

if (legacy.length) {
  console.warn(`[html-param-tier] 警告 ${legacy.length} 处：已登记在册的存量未标层级句（${BASELINE}），本次放行。`);
  legacy.forEach((h) => console.warn(show(h)));
}

if (fresh.length === 0) {
  console.log(
    `[html-param-tier] PASS: 改动了 ${touched.length} 份业务 HTML，全量重扫无基线外的未标层级句` +
    `（存量 ${legacy.length} 处已登记放行）。比对范围：${scope}；扫描口径：改动文件全文，已剥除 <style> 与注释。`
  );
  process.exit(0);
}

console.error(`[html-param-tier] FAIL: ${fresh.length} 处带具体数值的语句未标出所属层级，且不在存量基线内。`);
console.error(`  比对范围：${scope}；扫描口径：改动文件全文，已剥除 <style> 与注释。`);
console.error("  三层写法：（法定值，不可配）／（安全阈值，按X可配，调整须最高权限审批并留痕）／（业务参数，按X可配，出厂默认 N）");
fresh.forEach((h) => console.error(show(h)));
console.error("  确为法条编号、举例日期等非参数表述：在该段内或紧随段尾加 <!-- param-tier:ignore 数值1;数值2 --> 只豁免这些数值。");
console.error(`  存量待补标的老句子在 ${BASELINE}——那份只许缩不许增，新句子不得往里补登记。`);
process.exit(1);
