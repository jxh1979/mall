// 文件版本：V0.01
// M002-004：实际运行引用门禁，历史 raw 不参与校验，现行文档仍须定位。
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const root = fileURLToPath(new URL("../", import.meta.url));
const temp = fs.mkdtempSync(path.join(os.tmpdir(), "entry-anchors-"));
const run = () => spawnSync(process.execPath, [path.join(root, "tools/verify-entry-anchors.mjs")], { cwd: temp, encoding: "utf8" });
const put = (file, text) => {
  const target = path.join(temp, file);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, text, "utf8");
};
try {
  for (const file of [
    "docs/business-system-map-for-operations-v0.1.html",
    "docs/platform-governance-map-for-operations-v0.1.html",
    "docs/business-enum-quick-reference-v0.1.html",
    "docs/prd-audit/entry-registry.csv",
    "docs/prd-audit/authority-rule-ledger.csv",
    "docs/prd-audit/html-prd-coverage.csv",
  ]) put(file, fs.readFileSync(path.join(root, file), "utf8"));
  let result = run();
  assert.equal(result.status, 0, result.stdout + result.stderr);
  for (const file of [
    "docs/audit-2026-09/modules/M002-b1-grok-raw.md",
    "docs/audit-2026-09/modules/M002-b4-r1-self-raw.md",
    "docs/audit-2026-09/modules/M002-b5-r0-grok-raw.md",
  ]) put(file, "历史反例：BSM「M002回归不存在条目」。\n");
  result = run();
  assert.equal(result.status, 0, result.stdout + result.stderr);
  for (const file of [
    "docs/audit-2026-09/findings.md",
    "docs/audit-2026-09/ledger.md",
    "docs/audit-2026-09/modules/M002-00-authority-html-02.md",
    "docs/domain/guide.md",
  ]) {
    put(file, "现行引用：BSM「M002回归不存在条目」。\n");
    result = run();
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.ok(result.stderr.includes(file + ":1"), result.stderr);
    fs.unlinkSync(path.join(temp, file));
  }
  console.log("PASS：历史 raw 放行，现行 findings、ledger、模块收口和普通文档的失效引用仍拒绝。");
} finally {
  fs.rmSync(temp, { recursive: true, force: true });
}
