#!/usr/bin/env node
// 迁移 V 号并发门禁(AGENTS.md §9「数据库迁移 V 号串行生成,绝不并发」)。
// 规则:本分支新增的迁移 V 号必须大于 origin/main 当前的最大 V 号。
// 为什么:并发分支各自取号 → 高号先合、低号后合 → 共享测试库 complex_test 版本乱序漂移,已翻车两次。
// 这条铁律此前只有文字没有机器检查,本脚本补上闸门。
import { execFileSync } from "node:child_process";
import { readdirSync } from "node:fs";

const DIR = "backend/src/main/resources/db/migration";
const vOf = (f) => Number(/^V(\d+)__/.exec(f)?.[1] ?? NaN);

let mainFiles;
try {
  mainFiles = execFileSync("git", ["ls-tree", "-r", "--name-only", "origin/main", "--", DIR], { encoding: "utf8" });
} catch {
  console.error("MIGRATION_VERSION_ORDER_FAIL: 读不到 origin/main 的迁移目录,请先执行 git fetch origin main");
  process.exit(1);
}

const onMain = new Set(mainFiles.split("\n").filter(Boolean).map((f) => f.split("/").pop()));
const mainMax = Math.max(0, ...[...onMain].map(vOf).filter(Number.isFinite));
const added = readdirSync(DIR).filter((f) => f.endsWith(".sql") && !onMain.has(f));
const bad = added.filter((f) => vOf(f) <= mainMax);

if (bad.length) {
  console.error(`MIGRATION_VERSION_ORDER_FAIL: origin/main 当前最大迁移号是 V${mainMax},本分支新增的迁移必须排在它之后:`);
  for (const f of bad.sort()) console.error(`- ${f}(V${vOf(f)} 不大于 V${mainMax}) → 请重命名为 V${mainMax + 1} 或更后`);
  console.error("原因:并发取号会让共享测试库 complex_test 版本乱序漂移(AGENTS.md §9,已翻车两次)。");
  process.exit(1);
}

console.log(added.length
  ? `MIGRATION_VERSION_ORDER_PASS: 本分支新增 ${added.length} 个迁移,均排在 origin/main 最大号 V${mainMax} 之后`
  : `MIGRATION_VERSION_ORDER_PASS: 本分支未新增迁移(origin/main 最大号 V${mainMax})`);
