#!/usr/bin/env node
// 测试库隔离静态门禁(2026-07-05,codex 复核建议):测试源码/资源里不得出现"生产库 URL",
// 防止未来新测试重新硬编码 complex 生产库、或引入 ${COMPLEX_DATABASE_URL:...complex} 生产回退。
// 允许 complex_test;禁止裸 complex 库 URL。这是"边界"而非"逐点",挡的是漂移。
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";

const ROOTS = ["backend/src/test"];
// 匹配 jdbc:postgresql://.../complex 但排除 complex_test（负向前瞻）
const PROD_URL = /jdbc:postgresql:\/\/[^"'\s]*\/complex(?!_test)\b/;

const hits = [];
function walk(dir) {
  for (const entry of readdirSync(dir)) {
    const p = join(dir, entry);
    const s = statSync(p);
    if (s.isDirectory()) walk(p);
    else if (/\.(java|yml|yaml|properties)$/.test(entry)) {
      const lines = readFileSync(p, "utf8").split(/\r?\n/);
      lines.forEach((ln, i) => {
        if (PROD_URL.test(ln)) hits.push(`${p}:${i + 1}: ${ln.trim().slice(0, 120)}`);
      });
    }
  }
}
for (const r of ROOTS) { try { walk(r); } catch { /* 目录缺失忽略 */ } }

if (hits.length) {
  console.error("TEST_DB_ISOLATION_FAIL: 测试源引用了生产库 URL（应用 complex_test）:");
  for (const h of hits) console.error("- " + h);
  process.exit(1);
}
console.log("TEST_DB_ISOLATION_PASS: 测试源未引用生产库 complex（均用 complex_test）");
