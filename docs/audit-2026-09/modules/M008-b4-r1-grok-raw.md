CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M008 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M008-b4-r1-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。本轮已独立取树、取无过滤 binary diff、核对六文件当前等于 after、逐块逐行归因，并在主仓只读入口与 `.scratch` 隔离输入上复跑正反例。

未读取：任何 `M008-b4-r1-self-raw.md`（当前亦不存在）、Codex 本次 B4 raw、其它模块旧票正文。定稿队列以 findings.md V1.12 顶部正式表为准：001–004、006–007 是；005、008–016 否。

本轮实际审查口径：完整两树 diff、冻结 GEN-001–018 及 PGM-036 共 19 条、正式六项修复边界与最小回归。未新造判据/角色，未询问操作员。

---

## 1. 输入与独立锁实算

| 项 | 值 |
|---|---|
| 模块 | M008 治理门禁与 CI 基线／检查及生成工具／第1段 |
| 阶段 | B4-r1 只读「修复内容」完整 diff 独立审计 |
| 正式队列 | findings.md V1.12：001–004、006–007 是；005/008–016 否 |
| B3 | 已执行 1 次，模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `18894a9ee67577c6c8c3d19d99281f338a82b976` |
| after（对象类型 tree，可解析） | `97b93d8f8e7ef71e8706dd428c4315499436b015` |
| 本席自取 | `git cat-file -t` 两者均为 `tree`；`git rev-parse <id>^{tree}` 与输入 id 相同 |
| 完整 diff 命令 | `git diff --binary 18894a9ee67577c6c8c3d19d99281f338a82b976 97b93d8f8e7ef71e8706dd428c4315499436b015`（无路径过滤，不只看 HEAD 已提交） |
| 实际差异 | 21637 bytes；SHA256 `996b9954f7b4782aafca8ef5904a556dcee90067c0919311651b85139c8089d2`；UTF-8；352 行 unified；6 文件；98 插入 / 31 删除；25 hunk |
| `git diff --stat` | 见下表 |
| `git diff --numstat` | 58/1、3/2、12/4、2/2、8/8、15/14 |
| name-status | 六路径均为 M |
| `git diff --check` | exit 0 |
| 两树路径清单 | 各 7224 条，差集空；`--raw` 仅 6 条 blob 变化 |
| HEAD | `242f487233d028173cb9198eecd079f5d877405c` |
| HEAD^{tree} = origin/main^{tree} | `2af03dee3a7d65c4e745c9aa3173da2050890a65`（≠ after；after 为临时 write-tree） |
| 主索引 SHA256 | `eeb0e20f0c85989b6b902fba051c6b7b2f3051b870cacabd0c2ab6e2211b42aa` |
| `git diff --cached` | 空 |
| 工作区六文件 blob | 均等于 after（见 §6） |
| HEAD 六文件 blob | 均等于 before（修复尚未提交） |
| findings.md | V1.12；395917 bytes；SHA256 `17fc5fc891de4b65ca0bcce821eba9bc4a864c2b0cde26bb95900b244acf06b8` |
| ledger.md | V1.94；顶部锁同行；`LOCK-AMEND-COUNT: 1` |
| 三 HTML 两树 blob | 完全相同，diff 未改 HTML |
| 锁文件两树 blob | `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改） |

`git diff --stat --binary`：

```
 tools/gate-hardening-tests/m001-regression.mjs | 59 +++++++++++++++++++++++++-
 tools/refresh-entry-doc-baseline.mjs           |  5 ++-
 tools/verify-delivery-gates.mjs                | 16 +++++--
 tools/verify-entry-anchors.mjs                 |  4 +-
 tools/verify-html-ledger-sync.mjs              | 16 +++----
 tools/verify-html-param-tier.mjs               | 29 +++++++------
 6 files changed, 98 insertions(+), 31 deletions(-)
```

本席独立重算锁（命令 `sha256sum docs/audit-2026-09/plan/criteria-lock.md`）：

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 同行。criteria-lock 版本 V0.11。映射 `M008 → [GEN-001…GEN-018, PGM-036]`，共 19 条，本轮只用这 19 条。

---

## 2. 锁、分类与正式队列（既有，不新造）

B2-r2 定稿：001–004、006–007 技术性、工程缺陷是、修复队列是；005、008–016 否。本轮只核这六项是否按正式表修好，以及每行是否归到允许的 ID（含最小测试与自身版本）。

适用冻结判据：GEN-001 最小性、GEN-002 可执行正确性、GEN-003 测试有效性、GEN-004 信任边界、GEN-006 完整性、GEN-008 接口一致、GEN-009 错误处理、GEN-010 安全、GEN-011 可观测、GEN-013 构建/依赖、GEN-015 界面语言、GEN-016 文档可验证、GEN-017 分类、GEN-018 第一性原理；PGM-036 设备管理（本 diff 无物理设备实现面，沿用 016 仅登记）。

GEN-017：六文件改动均为治理检查/生成器/回归，无票务/财务/审批/枚举/设备发明，保持技术性。三 HTML 两树 blob 不变。

005：`requiredDeliveryCommands` 数组 before/after 全文相等（50 项、44 unique）。本 diff 未删重复常量。

009 输入资料：真实 `verify-entry-anchors` 仍 6 红、`verify-html-ledger-sync` 仍台账未同步红。本轮不修父链、不改 HTML/CSV/台账正本。

B/C 原意不冲突，不扩为 CLI 批准（014 仍否）。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary 18894a9ee67577c6c8c3d19d99281f338a82b976 97b93d8f8e7ef71e8706dd428c4315499436b015`

字节 21637；SHA256 `996b9954f7b4782aafca8ef5904a556dcee90067c0919311651b85139c8089d2`；UTF-8 完整原文如下。

````diff
diff --git a/tools/gate-hardening-tests/m001-regression.mjs b/tools/gate-hardening-tests/m001-regression.mjs
index a6683195..208cb941 100644
--- a/tools/gate-hardening-tests/m001-regression.mjs
+++ b/tools/gate-hardening-tests/m001-regression.mjs
@@ -1,4 +1,4 @@
-// 文件版本：V0.05
+// 文件版本：V0.06
 // 实际运行治理脚本；输入和模拟合并均在临时目录，绝不操作真实 PR。
 import assert from "node:assert/strict";
 import fs from "node:fs";
@@ -60,6 +60,11 @@ try {
   git(gates, "update-ref", "refs/remotes/origin/main", before);
   const ledgerGate = "tools/verify-html-ledger-sync.mjs";
   const paramGate = "tools/verify-html-param-tier.mjs";
+  // M008-006：基线是一个参数，干净树上的非法值也必须拒绝。
+  for (const file of [ledgerGate, paramGate]) {
+    expectExit(`M008-006 参数基线 ${file}`, run(file, gates, ["HEAD;echo M008_BASE_ARG"]), 1);
+    expectExit(`M008-006 环境基线 ${file}`, run(file, gates, [], { HTML_GATES_BASE: "HEAD;echo M008_BASE_ARG" }), 1);
+  }
   put(path.join(gates, html[0]), "<p>临时期限 30 天。</p>\n");
   for (const file of [ledgerGate, paramGate]) expectExit(`未提交错误 ${file}`, run(file, gates), 1);
   git(gates, "add", ".");
@@ -81,12 +86,24 @@ try {
     ["否定词", "临时期限 30 天，该天数不可配。", 1],
     ["法定层级", "临时期限 30 天（法定值，不可配）。", 0],
     ["业务层级", "临时期限 30 天（业务参数，按项目可配）。", 0],
+    // M008-004：段落内换行与独立段落不得改变层级判断。
+    ["M008-004 换行标注", "临时期限 30 天\n（业务参数，按项目可配）。", 0],
+    ["M008-004 同行两段", "临时期限 30 天（业务参数，按项目可配）。</p><p>另一事项每期 3 次。", 1],
+    ["M008-004 段后豁免", "临时期限 30 天。</p><!-- param-tier:ignore 30 天 --><p>其它说明", 0],
+    ["M008-004 豁免不扩到后段", "临时期限 30 天。</p><!-- param-tier:ignore 30 天 --><p>另一事项每期 3 次。", 1],
   ]) {
     put(path.join(gates, html[0]), `<p>${body}</p>\n`);
     expectExit(name, run(paramGate, gates, [before]), expected);
   }
   put(path.join(gates, ledger), "# 临时验证台账\n已记录本次变更。\n");
   expectExit("台账已同步", run(ledgerGate, gates, [before]), 0);
+  // M008-003：工作区删除和已提交删除都不是台账同步。
+  fs.unlinkSync(path.join(gates, ledger));
+  expectExit("M008-003 删除台账", run(ledgerGate, gates, [before]), 1);
+  git(gates, "add", ".");
+  git(gates, "commit", "-qm", "temporary deleted ledger");
+  put(path.join(gates, ledger), "# 临时验证台账\n已记录本次变更。\n");
+  expectExit("M008-003 已提交删除不被未跟踪副本掩盖", run(ledgerGate, gates, [before]), 1);
   put(path.join(gates, html[0]), "<p>临时验证资料</p>\n");
   put(path.join(gates, baseline), '{"entries":[{"key":"new-key","file":"temporary","text":"temporary"}]}\n');
   expectExit("基线新增 key", run(paramGate, gates, [before]), 1);
@@ -116,6 +133,11 @@ try {
   for (const [file, text] of Object.entries(csvInputs)) put(path.join(csv, file), text);
   const anchors = "tools/verify-entry-anchors.mjs";
   expectExit("CSV 引号逗号及跨行字段", run(anchors, csv), 0);
+  // M008-002：整条正文必填，不能以空字符串跳过验证。
+  const registryFile = "docs/prd-audit/entry-registry.csv";
+  put(path.join(csv, registryFile), csvInputs[registryFile].replace('"临时条目正文,\n后续内容"', '""'));
+  expectExit("M008-002 空条目正文", run(anchors, csv), 1);
+  put(path.join(csv, registryFile), csvInputs[registryFile]);
   for (const [file, text] of Object.entries(csvInputs)) {
     put(path.join(csv, file), text + "损坏记录,多一列,多一列,多一列,多一列,多一列\n");
     expectExit(`CSV 列数错误 ${file}`, run(anchors, csv), 1);
@@ -159,6 +181,21 @@ try {
     put(path.join(delivery, file), validScript);
   }
   const pkg = JSON.parse(fs.readFileSync(path.join(delivery, "package.json"), "utf8"));
+  // M008-001：实际运行检查不能被打印文字、注释或相似脚本名替代。
+  const originalScripts = { ...pkg.scripts };
+  for (const [script, gate] of [["verify:predeploy", "verify:html-param-tier"], ["verify:delivery-gates", "verify:unit"]]) {
+    for (const [name, value] of [
+      ["打印", originalScripts[script].replace(`npm run ${gate}`, `echo ${gate}`)],
+      ["相似名", originalScripts[script].replace(`npm run ${gate}`, `npm run ${gate}-extra`)],
+      ["整串打印", `echo ${JSON.stringify(originalScripts[script])}`],
+      ["注释", `echo skipped # ${originalScripts[script]}`],
+    ]) {
+      pkg.scripts = { ...originalScripts, [script]: value };
+      put(path.join(delivery, "package.json"), JSON.stringify(pkg));
+      expectExit(`M008-001 ${script} ${name}`, run(deliveryGate, delivery), 1);
+    }
+  }
+  pkg.scripts = originalScripts;
   pkg.scripts["verify:predeploy"] = "true";
   put(path.join(delivery, "package.json"), JSON.stringify(pkg));
   expectExit("检查入口缺少 HTML 门禁", run(deliveryGate, delivery), 1);
@@ -206,6 +243,26 @@ exit 0
     expectExit(`合并提交 ${mode}`, result, expected);
     if (fs.existsSync(path.join(merge, "merged")) !== (expected === 0)) failures.push(`合并副作用 ${mode}`);
   }
+  // M008-007：使用当前活跃句式，检查实际回填内容并保留历史句。
+  const refresh = path.join(temp, "refresh");
+  const entryFile = "docs/00-current-effective-document-entry-v0.1.md";
+  const history = "历史记录：V385 / 1606 tests，保留当时事实。";
+  put(path.join(refresh, entryFile), "# 临时入口\n本轮 2026-09-01 实测 OpenAPI 720 operations、Flyway 最新 V385；当前工作树无 `backend/target/surefire-reports`，故不声明当前测试数量。\n" + history + "\n");
+  put(path.join(refresh, "README.md"), "<!-- baseline:auto-begin -->\n<!-- baseline:auto-end -->\n");
+  put(path.join(refresh, "docs/openapi/openapi-p0.json"), JSON.stringify({ paths: { "/sample": { get: {}, post: {} } } }));
+  put(path.join(refresh, "backend/src/main/resources/db/migration/V12__sample.sql"), "select 1;\n");
+  const reportFile = path.join(refresh, "backend/target/surefire-reports/sample.xml");
+  put(reportFile, '<testsuite tests="3"></testsuite>\n');
+  const generator = "tools/refresh-entry-doc-baseline.mjs";
+  expectExit("M008-007 回填快照", run(generator, refresh), 0);
+  const refreshed = fs.readFileSync(path.join(refresh, entryFile), "utf8");
+  assert.match(refreshed, /实测 OpenAPI 2 operations、Flyway 最新 V12；后端 surefire 现有报告 3 tests。/);
+  assert.ok(refreshed.includes(history), "M008-007 历史记录应保留");
+  expectExit("M008-007 回填后校验", run(generator, refresh, ["--check"]), 0);
+  fs.unlinkSync(reportFile);
+  put(path.join(refresh, entryFile), refreshed.replace("2 operations", "99 operations"));
+  expectExit("M008-007 无报告仍回填计数", run(generator, refresh), 0);
+  assert.match(fs.readFileSync(path.join(refresh, entryFile), "utf8"), /实测 OpenAPI 2 operations、Flyway 最新 V12；后端 surefire 现有报告 3 tests。/);
   assert.deepEqual(failures, [], `治理回归失败：${failures.join("、")}`);
   console.log("M001_GOVERNANCE_REGRESSION_PASS");
 } finally {
diff --git a/tools/refresh-entry-doc-baseline.mjs b/tools/refresh-entry-doc-baseline.mjs
index f2d970fa..1faf6e74 100644
--- a/tools/refresh-entry-doc-baseline.mjs
+++ b/tools/refresh-entry-doc-baseline.mjs
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 // 00-entry + README 事实基线快照自动回填（审计 P2-4 建议"计数由工具自动回填"的落地，根治手工刷反复过期）。
 // 权威来源：openapi-p0.json（operation 数）/ db/migration（最新迁移+迁移数）/ surefire-reports（测试数）。
 // 用法：node tools/refresh-entry-doc-baseline.mjs        —— 回填快照（写文件）
@@ -63,8 +64,8 @@ entryText = entryText.replace(
     /(以 `npm run verify:\*` 输出为准，)\d{4}-\d{2}-\d{2}( 刷新）)/g,
     `$1${today}$2`);
 entryText = entryText.replace(
-    /本轮 \d{4}-\d{2}-\d{2} 取证 Flyway 最新 V\d+、OpenAPI \d+ operations、后端 surefire 现有报告 (\d+) tests/g,
-    (m, oldTests) => `本轮 ${today} 取证 Flyway 最新 ${latestMigrationTag}、OpenAPI ${operationCount} operations、后端 surefire 现有报告 ${testCount ?? oldTests} tests`);
+    /本轮 \d{4}-\d{2}-\d{2} 实测 OpenAPI \d+ operations、Flyway 最新 V\d+；(当前工作树无 `backend\/target\/surefire-reports`，故不声明当前测试数量|后端 surefire 现有报告 \d+ tests)。/g,
+    (m, oldStatus) => `本轮 ${today} 实测 OpenAPI ${operationCount} operations、Flyway 最新 ${latestMigrationTag}；${testCount === null ? oldStatus : `后端 surefire 现有报告 ${testCount} tests`}。`);
 entryText = entryText.replace(/查看本轮核验 \d+ 个 operation/g,
     `查看本轮核验 ${operationCount} 个 operation`);
 entryText = entryText.replace(/本轮核验 \d+ 个 OpenAPI operation/g,
diff --git a/tools/verify-delivery-gates.mjs b/tools/verify-delivery-gates.mjs
index 28b5853b..5fdfb898 100644
--- a/tools/verify-delivery-gates.mjs
+++ b/tools/verify-delivery-gates.mjs
@@ -1,8 +1,15 @@
-// 文件版本：V0.02
+// 文件版本：V0.03
 import fs from "node:fs";
 import path from "node:path";
 import { parse } from "yaml";
 
+// ponytail: 识别本仓现有的 npm run && 链；引入其它 shell 语法时再使用专门解析器。
+function npmCommands(script = "") {
+  const parts = script.trim().split(/\s*&&\s*/);
+  if (!parts.every(command => /^npm\s+run\s+[\w:-]+$/.test(command))) return new Set();
+  return new Set(parts.map(command => command.split(/\s+/)[2]));
+}
+
 const repoRoot = process.cwd();
 const failures = [];
 
@@ -151,8 +158,9 @@ if (packageJson) {
   if (!deliveryScript) {
     failures.push("package.json must expose verify:delivery-gates");
   } else {
+    const deliveryCommands = npmCommands(deliveryScript);
     for (const command of requiredDeliveryCommands) {
-      if (!deliveryScript.includes(command)) failures.push(`verify:delivery-gates must include ${command}`);
+      if (!deliveryCommands.has(command)) failures.push(`verify:delivery-gates must include ${command}`);
     }
     for (const forbidden of ["test:backend", "android:p0", "dev:backend", "dev:web"]) {
       if (deliveryScript.includes(forbidden)) failures.push(`verify:delivery-gates must stay lightweight and not include ${forbidden}`);
@@ -310,9 +318,9 @@ for (const script of ["ops/160/complex-deploy-api.sh", "ops/160/complex-deploy-w
   }
 }
 if (packageJson) {
-  const predeploy = packageJson.scripts?.["verify:predeploy"] ?? "";
+  const predeployCommands = npmCommands(packageJson.scripts?.["verify:predeploy"]);
   for (const gate of htmlGates) {
-    if (!predeploy.includes(gate)) failures.push(`verify:predeploy must include ${gate}`);
+    if (!predeployCommands.has(gate)) failures.push(`verify:predeploy must include ${gate}`);
   }
 }
 
diff --git a/tools/verify-entry-anchors.mjs b/tools/verify-entry-anchors.mjs
index 0c1361db..817fa249 100755
--- a/tools/verify-entry-anchors.mjs
+++ b/tools/verify-entry-anchors.mjs
@@ -1,5 +1,5 @@
 #!/usr/bin/env node
-// 文件版本：V0.02
+// 文件版本：V0.03
 // 门禁：三份业务最终解释权 HTML 的引用一律用「条目名」，不得再用行号。
 // 依据：DEC-R8-17（用户 2026-08-06 裁 A：彻底废行号、只留条目名）。
 // 病史：R8 两批 6 轮返工里有 3 轮因行号锚点漂移返工（760 行漂移 → 287 处重建 → +2 位移 19 处漏改）。
@@ -111,7 +111,7 @@ for (const r of rows) {
   if (nameSeen[key]) errors.push(`条目名在同一份 HTML 内重复：${code}「${r.entry_name}」（${nameSeen[key]} 与 ${r.entry_id}）`);
   nameSeen[key] = r.entry_id;
   const entryBody = (r.entry_body || "").replace(/\s+/g, "");
-  if (entryBody && !flat[code].includes(entryBody)) {
+  if (!entryBody || !flat[code].includes(entryBody)) {
     errors.push(`注册表条目正文在 HTML 现文中找不到：${r.entry_id} ${code}「${r.entry_name}」——HTML 已改而注册表未重建`);
   }
   byCode[code].push(r);
diff --git a/tools/verify-html-ledger-sync.mjs b/tools/verify-html-ledger-sync.mjs
index b4768f3b..d2ef1258 100644
--- a/tools/verify-html-ledger-sync.mjs
+++ b/tools/verify-html-ledger-sync.mjs
@@ -1,5 +1,5 @@
 #!/usr/bin/env node
-// 文件版本：V0.01
+// 文件版本：V0.02
 // 门禁：三份业务最终解释权 HTML 与决策台账必须同一批改动内同步。
 // 依据：用户 2026-08-03 拍板「台账保持完整，但改 HTML 必须在同一个 PR 内同步台账」。
 // 病史：2026-07-22~08-01 三份 HTML 被 12 笔提交改过、台账停在改之前，挖出 36 条「已拍板未落 HTML」；
@@ -7,13 +7,13 @@
 // 2026-08-07 门禁升级批 1（用户弹窗批立项）修两处空转假绿：
 //   ① 只比对提交区 → 改 HTML 但没 commit 时门禁看不见，一路绿到 push 前。现按工作区脏净自动切范围。
 //   ② 取不到基线时「跳过 exit 0」→ 无法判定不等于通过，改为 exit 1 并提示先 fetch。
-import { execSync } from "node:child_process";
+import { execFileSync } from "node:child_process";
 
-const git = (cmd) => execSync(`git ${cmd}`, { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 });
+const git = (...args) => execFileSync("git", args, { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 });
 
 // 一律切到仓根再干活：在子目录里跑时 git 只看当前目录，会静默变绿。
 try {
-  process.chdir(git("rev-parse --show-toplevel").trim());
+  process.chdir(git("rev-parse", "--show-toplevel").trim());
 } catch {
   console.error("[html-ledger-sync] FAIL: 当前目录不在 git 仓库内，无法定位仓根。");
   process.exit(1);
@@ -31,7 +31,7 @@ const base = process.argv[2] || process.env.HTML_GATES_BASE || "origin/main";
 // 基线取不到就必须红：本门禁唯一的作用就是判定「改没改 HTML」，判不了不等于没改。
 let mergeBase;
 try {
-  mergeBase = git(`merge-base ${base} HEAD`).trim();
+  mergeBase = git("merge-base", base, "HEAD").trim();
 } catch {
   console.error(`[html-ledger-sync] FAIL: 取不到基线 ${base}，无法判定本次改动范围。`);
   console.error("  无法判定不等于通过。请先 git fetch origin（CI 侧 actions/checkout 需 fetch-depth: 0）。");
@@ -41,13 +41,13 @@ try {
 
 // 三份 HTML 有未提交改动 → 连工作区一起比（基线↔工作区，含已提交的那部分）；
 // 干净 → 维持原提交区口径 base...HEAD。
-const dirty = git(`status --porcelain -- ${HTML.join(" ")}`).trim() !== "";
+const dirty = git("status", "--porcelain", "--", ...HTML).trim() !== "";
 const range = dirty ? mergeBase : `${base}...HEAD`;
 const scope = dirty
   ? `工作区（含未提交改动）↔ 基线 ${base}@${mergeBase.slice(0, 8)}`
   : `提交区 ${base}...HEAD（工作区三份 HTML 无未提交改动）`;
 
-const changed = git(`diff --name-only ${range}`)
+const changed = git("diff", "--name-only", range)
   .split("\n").map(s => s.trim()).filter(Boolean);
 
 const touchedHtml = HTML.filter(f => changed.includes(f));
@@ -55,7 +55,7 @@ if (touchedHtml.length === 0) {
   console.log(`[html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。比对范围：${scope}。`);
   process.exit(0);
 }
-if (changed.includes(LEDGER)) {
+if (changed.includes(LEDGER) && git("diff", "--name-only", "--diff-filter=d", range, "--", LEDGER).trim()) {
   console.log(`[html-ledger-sync] PASS: 改动了 ${touchedHtml.length} 份业务 HTML，决策台账已同步。比对范围：${scope}。`);
   process.exit(0);
 }
diff --git a/tools/verify-html-param-tier.mjs b/tools/verify-html-param-tier.mjs
index 182c300d..75e85a96 100755
--- a/tools/verify-html-param-tier.mjs
+++ b/tools/verify-html-param-tier.mjs
@@ -1,5 +1,5 @@
 #!/usr/bin/env node
-// 文件版本：V0.01
+// 文件版本：V0.02
 // 门禁：三份业务最终解释权 HTML 里带具体数值的业务句，必须在原句里标出所属层级。
 // 依据：用户 2026-08-03 拍板——参数分三层（法定值不可配／安全阈值可配但须最高权限审批并留痕／
 //       其余业务参数一律可配），且三层一律在参数原句里字面标出，不设临场判定。
@@ -15,15 +15,15 @@
 //      整行豁免等于给这行以后新增的数值也预先发了通行证。
 //   ④ 取不到基线一律红——无法判定不等于通过。
 //   ⑤ 改动了 HTML 就全量重扫这几份文件（不只看 diff 增行）：句子被就地改数值、被挪位置都逃不掉。
-import { execSync } from "node:child_process";
+import { execFileSync } from "node:child_process";
 import fs from "node:fs";
 import crypto from "node:crypto";
 
-const git = (cmd) => execSync(`git ${cmd}`, { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 });
+const git = (...args) => execFileSync("git", args, { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 });
 
 // 一律切到仓根再干活：在子目录里跑时相对路径读不到文件、git 也只看当前目录，会静默变绿。
 try {
-  process.chdir(git("rev-parse --show-toplevel").trim());
+  process.chdir(git("rev-parse", "--show-toplevel").trim());
 } catch {
   console.error("[html-param-tier] FAIL: 当前目录不在 git 仓库内，无法定位仓根。");
   process.exit(1);
@@ -53,19 +53,20 @@ const keyOf = (file, text) =>
 // （DEC-R8-17 已废行号锚点，基线用行号必然随改动漂移）。
 function scan(file) {
   const raw = fs.readFileSync(file, "utf8");
-  // <style> 块与 HTML 注释整块屏蔽成等量空白：行号、行数都不变，行内豁免标记仍从原文取。
+  // 屏蔽成等量空白保留偏移，豁免标记从同一逻辑块的原文取。
   const masked = raw
     .replace(/<style[\s\S]*?<\/style>/gi, (m) => m.replace(/[^\n]/g, " "))
     .replace(/<!--[\s\S]*?-->/g, (m) => m.replace(/[^\n]/g, " "));
-  const rawLines = raw.split("\n");
+  // ponytail: 按本仓段落/列表项/表格行/容器/标题分块；复杂嵌套需求再采用 HTML 解析器。
+  const starts = [0, ...Array.from(masked.matchAll(/<(?:p|li|tr|div|h[1-6])(?:\s|>)/gi), m => m.index), masked.length];
   const seen = new Set();
   const hits = [];
-  masked.split("\n").forEach((line, i) => {
-    const text = norm(line);
+  starts.slice(0, -1).forEach((start, i) => {
+    const text = norm(masked.slice(start, starts[i + 1]));
     if (!text || TIER.test(text)) return;
     let vals = valuesIn(text);
     if (!vals.length) return;
-    const mark = IGNORE.exec(rawLines[i] || "");
+    const mark = IGNORE.exec(raw.slice(start, starts[i + 1]));
     if (mark) {
       const listed = valuesIn(mark[1]);
       if (!listed.length) {
@@ -123,7 +124,7 @@ const base = process.argv[2] || process.env.HTML_GATES_BASE || "origin/main";
 
 let mergeBase;
 try {
-  mergeBase = git(`merge-base ${base} HEAD`).trim();
+  mergeBase = git("merge-base", base, "HEAD").trim();
 } catch {
   console.error(`[html-param-tier] FAIL: 取不到基线 ${base}，无法判定本次改动范围。`);
   console.error("  无法判定不等于通过。请先 git fetch origin（CI 侧 actions/checkout 需 fetch-depth: 0）。");
@@ -138,7 +139,7 @@ const local = readBaseline(fs.readFileSync(BASELINE, "utf8"), "本地");
 let prevJson = null;
 try {
   // 用 stdio 选项而不是 shell 重定向：Windows 上 cmd 会把 2>/dev/null 当成路径而报错，导致棘轮比较被静默跳过。
-  prevJson = execSync(`git show ${mergeBase}:${BASELINE}`, { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] });
+  prevJson = execFileSync("git", ["show", `${mergeBase}:${BASELINE}`], { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] });
 } catch {
   prevJson = null;
 }
@@ -154,13 +155,13 @@ if (prevJson !== null) {
   }
 }
 
-const dirty = git(`status --porcelain -- ${HTML.join(" ")}`).trim() !== "";
+const dirty = git("status", "--porcelain", "--", ...HTML).trim() !== "";
 const range = dirty ? mergeBase : `${base}...HEAD`;
 const scope = dirty
   ? `工作区（含未提交改动）↔ 基线 ${base}@${mergeBase.slice(0, 8)}`
   : `提交区 ${base}...HEAD（工作区三份 HTML 无未提交改动）`;
 
-const changed = git(`diff --name-only ${range}`)
+const changed = git("diff", "--name-only", range)
   .split("\n").map((s) => s.trim()).filter(Boolean);
 const touched = HTML.filter((f) => changed.includes(f));
 
@@ -193,6 +194,6 @@ console.error(`[html-param-tier] FAIL: ${fresh.length} 处带具体数值的语
 console.error(`  比对范围：${scope}；扫描口径：改动文件全文，已剥除 <style> 与注释。`);
 console.error("  三层写法：（法定值，不可配）／（安全阈值，按X可配，调整须最高权限审批并留痕）／（业务参数，按X可配，出厂默认 N）");
 fresh.forEach((h) => console.error(show(h)));
-console.error("  确为法条编号、举例日期等非参数表述：在该行内加 <!-- param-tier:ignore 数值1;数值2 --> 只豁免这些数值。");
+console.error("  确为法条编号、举例日期等非参数表述：在该段内或紧随段尾加 <!-- param-tier:ignore 数值1;数值2 --> 只豁免这些数值。");
 console.error(`  存量待补标的老句子在 ${BASELINE}——那份只许缩不许增，新句子不得往里补登记。`);
 process.exit(1);
````

---

## 4. 逐块逐行归因

hunk 分布：m001-regression 6；refresh 2；delivery 3；entry-anchors 2；ledger-sync 5；param-tier 7。合计 25。无范围外路径。

### 4.1 `tools/gate-hardening-tests/m001-regression.mjs` V0.05→V0.06

| 行块 | 内容 | ID |
|---|---|---|
| 文件头 `// 文件版本：V0.06` | 自身版本递增 | 六项（测试文件承载全部新增块） |
| 新增 4 行：`HEAD;echo M008_BASE_ARG` 参数/环境，ledger+param | 非法基线必须拒绝 | M008-006 |
| 新增 4 组 param 用例：换行标注 / 同行两段 / 段后豁免 / 豁免不扩到后段 | 逻辑块扫描与豁免范围 | M008-004 |
| 新增删除台账 + 已提交删除+未跟踪副本 | 删除不是同步 | M008-003 |
| 新增空 `entry_body` | 空正文必须失败 | M008-002 |
| 新增 8 组 echo/相似名/整串打印/注释 × predeploy/delivery-gates | 实际命令识别 | M008-001 |
| 新增 refresh 回填/校验/无报告保留测试数 + 历史句 | 当前活跃句式 | M008-007 |

原有用例（脏工作区、PR、push、豁免、层级、CSV 损坏、CI、部署门、merge 夹具）未改语义。测试写 `os.tmpdir()`，不写主仓。

### 4.2 `tools/refresh-entry-doc-baseline.mjs` 新增 V0.01

| 行块 | 内容 | ID |
|---|---|---|
| 新增 `// 文件版本：V0.01` | 自身版本 | M008-007 |
| 替换「取证 Flyway…surefire 现有报告 (\d+) tests」为「实测 OpenAPI…；（无 surefire 句\|现有报告 \d+ tests）。」 | 匹配 00-entry:126 当前句；`testCount===null` 时保留捕获的原陈述 | M008-007 |

未改 README 自动块逻辑、未改其它历史句正则、未改正本入口/README。

### 4.3 `tools/verify-delivery-gates.mjs` V0.02→V0.03

| 行块 | 内容 | ID |
|---|---|---|
| 版本 V0.03 | 自身版本 | M008-001 |
| 新增 `npmCommands()`：只认 `npm run &&` 链，否则空 Set（失败关闭） | 实际命令识别入口/最小 helper | M008-001 |
| `verify:delivery-gates`：`includes` → `deliveryCommands.has` | 001 | M008-001 |
| `verify:predeploy`：`includes` → `predeployCommands.has` | 001 | M008-001 |
| `const requiredDeliveryCommands = […]` | **全文未出现在 diff 中** | 005 未动（50/44 保持） |

forbidden 脚本名仍用 `includes`，不在本批队列。

### 4.4 `tools/verify-entry-anchors.mjs` V0.02→V0.03

| 行块 | 内容 | ID |
|---|---|---|
| 版本 V0.03 | 自身版本 | M008-002 |
| `if (entryBody && !flat[code].includes(entryBody))` → `if (!entryBody \|\| !flat[code].includes(entryBody))` | 空/空白正文不再跳过 | M008-002 |

CSV 框架、排除规则、镜像列未改。

### 4.5 `tools/verify-html-ledger-sync.mjs` V0.01→V0.02

| 行块 | 内容 | ID |
|---|---|---|
| 版本 V0.02 | 自身版本 | M008-003 与 M008-006（同文件两处根因） |
| `execSync`→`execFileSync`；`git(cmd)`→`git(...args)` | 全部 Git 改参数数组 | M008-006 |
| `rev-parse` / `merge-base` / `status --porcelain -- …HTML` / `diff --name-only` 全部拆成 argv | 006 | M008-006 |
| `changed.includes(LEDGER)` 增加同一 `range` 上 `--diff-filter=d`（小写，排除删除）且 `-- LEDGER` | 删除不视为已同步 | M008-003（判定）+ M008-006（该次 Git 亦为 argv） |

文件级同批规则未改成自然语言审查。隔离探针：修改台账时 `--diff-filter=d` 仍列出该路径；删除后 `--diff-filter=d` 为空、`--diff-filter=D` 列出。

### 4.6 `tools/verify-html-param-tier.mjs` V0.01→V0.02

| 行块 | 内容 | ID |
|---|---|---|
| 版本 V0.02 | 自身版本 | M008-004 与 M008-006 |
| `execSync`→`execFileSync`；helper argv；`merge-base`/`status`/`diff`；`git show ${mergeBase}:${BASELINE}` 改为 `execFileSync("git", ["show", \`${mergeBase}:${BASELINE}\`])` | 全部 Git 与 show | M008-006 |
| `scan()`：等量 mask 保留偏移；按 `p/li/tr/div/h1-6` 开标签分块；豁免从同一块 `raw.slice` 取 | 逻辑块扫描/偏移/豁免 | M008-004 |
| 失败提示「该行内」→「该段内或紧随段尾」 | 与新块模型一致 | M008-004 |
| 基线棘轮、`VALUE`/`TIER`/`IGNORE`、只缩不增 | 未改业务含义 | 保持（004 要求保留） |

`--write-baseline` 分支仍在，本席未在主仓调用。

### 4.7 归因总览

- 范围外文件：无。
- 005 数组：无 diff。
- 三 HTML / 基线 JSON / README / 00-entry 正本 / 历史 log / 锁：无 diff。
- 测试新增块均带对应 `M008-00x` 注释。
- 自身版本：delivery→001；anchors→002；ledger→003+006；param→004+006；refresh→007；regression→六项。

**范围归因：PASS。**

---

## 5. 修复正确性（对照正式表，不扩队列）

### M008-001 — PASS

正式事实：`includes` 把 `echo verify:html-param-tier` / 相似名 / 整串打印 / 注释当成功。现用 `npmCommands` 抽 `npm run &&` 链的第三字段做集合判断。真实 `package.json` 的 `verify:predeploy`（15）与 `verify:delivery-gates`（49）均为纯 `npm run &&`，helper 能解析。当前 `node tools/verify-delivery-gates.mjs` exit 0。

反例（before 源 + 现测试）：8 组全部 `DELIVERY_GATES_DOCS_PASS`（exit 0≠1）。after 同源 8 组 exit 1。未新增门禁项目。

### M008-002 — PASS

`!entryBody` 现与找不到正文同一失败。before：空 `""` 仍打印 PASS。after 回归 `M008-002 空条目正文` exit 1。未重造 CSV。

### M008-003 — PASS

同一比较范围上 `--diff-filter=d` 排除删除。before：工作区删除与「已提交删除 + 未跟踪副本」都假绿「台账已同步」。after 两案 exit 1。修改台账仍可通过（回归「台账已同步」）。未做语义审查。与 012（任意追加）未混同。

### M008-004 — PASS

物理行改为逻辑块。before：「换行标注」把「30 天」与「业务参数」拆成两行而误红；「同行两段」第二段「3 次」无标注却假绿。after：前者 0、后者 1。段后豁免/不扩后段在 before 因同物理行已碰巧符合，after 仍保持。精确数值豁免与棘轮用例仍过。未改 HTML、未改层级词。

### M008-005 — 未修，符合定稿否

数组 50/44 全文相等。不得顺便删。本轮无该项 diff。

### M008-006 — PASS

ledger/param 已无 `execSync` 字符串拼 Git。before：`HEAD;echo M008_BASE_ARG` 参数与环境在干净树上假绿，范围字面变成 `HEAD;echo M008_BASE_ARG...HEAD`。after 四组 exit 1。`git show` 现为 argv 单参 `mergeBase:path`。合法 `absent-ref` 仍 1。

### M008-007 — PASS

新正则匹配 00-entry:126「实测 OpenAPI…；无 surefire / 现有报告 N tests」。隔离：before `--check` **只报 README**；after `--check` **同时报入口 + README**（720 ops / V399 / 55 tests）。隔离写模式（仅 `.scratch` 副本）把 :126 写成 V399 / 55 tests，历史句与 :160 未改；回填后 `--check` 0。回归合成句保留「历史记录：V385 / 1606 tests」，无报告时保留原测试陈述、仍回填 operations/Flyway。

正本 `docs/00-current-effective-document-entry-v0.1.md` SHA256 仍 `34e9c9f76578b61eb659dc4e369b274f62adb110b5f4dd3895bc0bb08f83e314`；README 仍 `28a46aea13227b0e3e101f9dc0a2970b6e3e581ca5995d291288aa59cb2d23b5`。未回填 M435/M436 正本。

:160「取证为 Flyway…」仍不匹配——见 §7 观察，不构成本项 FAIL（正式表钉的是 :126）。

### 005/008–016

本 diff 未改仅登记项。009 现行 6 红与 ledger 红仍在（§8）。010 历史 log 未改。011 最小回归已补进现有 `m001-regression.mjs`，未另立测试工程。014 未翻译 CLI。016/PGM-036 无设备实现。

**修复内容：PASS。**

---

## 6. 当前源 hash（工作区 = after）

| 文件 | 版本 | git blob | SHA256 |
|---|---|---|---|
| tools/gate-hardening-tests/m001-regression.mjs | V0.06 | `208cb94106c0380d2aaa7053831a735a6bc0aca4` | `d7479d0da66e8d599de40a1cc697fc581f90b9c605813bdfb3a87931f2f63b5c` |
| tools/refresh-entry-doc-baseline.mjs | V0.01 | `1faf6e74c6b9ebc4bc6c72537b62d7af186f6091` | `2ba460a532e9571cf18cc270e77dfc7c0385c0b132e0c2351431f3a710b6f1d7` |
| tools/verify-delivery-gates.mjs | V0.03 | `5fdfb898f62f0f36e418fa30cb825a300bf37ca6` | `73bcf494770156441945c0ea2ff6afec30ae4489a48d7cc1c0a3225b9221a78f` |
| tools/verify-entry-anchors.mjs | V0.03 | `817fa24977937248eed875014adef892b9f1f92a` | `7e62c6a19e36be5c13b166fd40aa4cb75e6ad057dfd6e97090323cae17853b82` |
| tools/verify-html-ledger-sync.mjs | V0.02 | `d2ef1258fd526b2dfbfa77c4f9ec2cbe79f8208e` | `1c9f974f4c49c3886469e6b86236675393317b7b0379bbbd1d354599f911214d` |
| tools/verify-html-param-tier.mjs | V0.02 | `75e85a9607352087fa68bdb488e593fd248d8b19` | `c79cdb39969b123264e917aa2921e4442e2ac1679f5f78ae6fbc0c70a5e71be9` |

before blob：`a6683195` / `f2d970fa` / `28b5853b` / `0c1361db` / `b4768f3b` / `182c300d`。工作区六文件 `git hash-object` 均等于 after。`node --check` 六文件均 exit 0。

基线 JSON 仍空 `entries: []`，SHA256 `9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8`（B1 误写同字节事实保留；本席未再主仓写 baseline）。

---

## 7. 全部审计发现 / 观察

无新工程缺陷进入修复队列。原正式 ID 001–004、006–007 已按表修好。005/008–016 保持否。

### 观察 GR-M008-B4-OBS-01（临时 ID，不修）

- 位置：`tools/refresh-entry-doc-baseline.mjs:66–68` 对照正本 `docs/00-current-effective-document-entry-v0.1.md:160`
- 事实：隔离回填后 :126 已是 V399/55 tests，:160 仍为「本轮 2026-09-01 取证为 Flyway 最新 V385…」。`--check` 在 :126 已对齐后可以对入口报绿，同时 :160 仍旧。
- 分类：技术性。工程缺陷：否（本批）。修复队列：否。
- 依据类型：2/4。判据：GEN-001/016。
- 理由：正式 007 钉的是 :126 活跃「实测」句；:160 是另一套「取证为」句，B1/B2 放在 008/M436 入口资料，本轮明确不回填正本。扩正则会超出定稿六项。
- 无业务发明。

未把 009 父链红、010 历史 log、014 CLI 语言写成新票。

四分类顺序：无 HTML 冲突、无细化改写、全部技术性、无添加项。

---

## 8. 真实运行命令、输出、退出码

### 8.1 当前 after 回归（主仓 cwd；测试自己写临时目录）

命令：`node tools/gate-hardening-tests/m001-regression.mjs`

退出码：0。71 条 `PASS ` + `M001_GOVERNANCE_REGRESSION_PASS`。含全部 M008-001/002/003/004/006/007 新块。

### 8.2 before 源 + 同一当前测试（`.scratch` clone overlay）

`git clone --local` HEAD（六工具 = before）后只覆盖 after 版 `m001-regression.mjs`。退出码 1。

`PASS ` 52 条（旧用例仍过）。`FAIL ` 17 条，对应该修的旧缺陷：

- 006×4：假绿，范围 `HEAD;echo M008_BASE_ARG...HEAD`
- 004 换行：误红「30天」未标层级
- 004 同行两段：假绿 PASS（0≠1）
- 003×2：删除仍「台账已同步」
- 002：空 body 仍 PASS
- 001×8：echo/相似名/整串打印/注释仍 `DELIVERY_GATES_DOCS_PASS`

007：`PASS M008-007 回填快照`（生成器 no-op exit 0）随后 `assert.match` 抛错，输入仍为 720/V385/无 surefire 原句。与正式缺陷一致。

### 8.3 主仓只读父链（不写、不装 hook）

| 命令 | exit | 关键输出 |
|---|---|---|
| `node --check` 六文件 | 0×6 | （无输出） |
| `node tools/verify-delivery-gates.mjs` | 0 | `DELIVERY_GATES_DOCS_PASS` |
| `node tools/verify-html-param-tier.mjs` | 0 | PASS：改动了 1 份业务 HTML，无基线外未标层级句。范围工作区↔`origin/main@242f4872` |
| `node tools/verify-html-ledger-sync.mjs` | 1 | FAIL：改了 BSM，决策台账未同步（009 已登记） |
| `node tools/verify-entry-anchors.mjs` | 1 | FAIL 6 处：BSM-E0433/E0736 正文；BSM-0283 source/preconditions/state_transition；BSM-0530 source（009 已登记） |
| `node tools/refresh-entry-doc-baseline.mjs --check` | 1 | `DRIFT: docs/00-current-effective-document-entry-v0.1.md + README.md`（应为 720 ops / V399（377 迁移） / 55 tests）。**如实记录双文件漂移，不为全链绿修父链。** |

工作区 `M docs/business-system-map-for-operations-v0.1.html`（blob `12334b13…`，HEAD 为 `16783edc…`）。该脏 HTML **已同在 before 与 after 两树**，不在本轮 6 文件 diff 内，属 009 输入资料。

### 8.4 007 隔离写（仅 `.scratch` 副本）

- after `--check`（写前）：exit 1，入口+README 双漂移。
- before `--check`（写前）：exit 1，**仅 README**。
- after 默认写：exit 0，`已回填 … 720 ops / V399（377 迁移） / 55 tests（2026-09-09）`。
- after `--check`（写后）：exit 0。
- 副本 :126 → V399 / 55 tests；:160 仍 V385；正本 hash 不变。

未在主仓跑 refresh 默认写、`--write-baseline`、真实 push/PR/merge/部署、安装 hook。

### 8.5 Git `--diff-filter` 探针（隔离仓库）

小写 `d` 排除删除、保留修改；大写 `D` 只选删除。与 003 实现一致。

---

## 9. 验证限制

- 本轮不是 B5，未按 17 文件/1530 行/19 判据做全模块三线复审，不冒称模块 PASS。
- 未读 Codex B4 raw；未引入第三席。
- 未验证 GitHub 远端、真实 hook 安装副本、生产部署。
- `npmCommands` 只认当前仓纯 `npm run &&`；其它 shell 语法会失败关闭。真实两入口符合该假设，未扩解析器（GEN-001）。
- 007 隔离写使用副本+对 OpenAPI/迁移/surefire 的符号链接；surefire 4 个 xml、迁移 377、最新 V399，与 `--check` 摘要一致。日期为 `toISOString` UTC（本机为 2026-09-09），015 已登记，不新开时区项。
- B3 自报 71 PASS 经本席复跑确认，不代替独立取证。
- 临时目录 `.scratch/m008-b4-r1-grok` 在落盘后删除，不新增产物类型。

---

## 10. 结论

| 项 | 结果 |
|---|---|
| 修复内容 | **PASS** |
| 范围归因 | **PASS** |
| 总 | **PASS** |

六项正式缺陷在 after 上可复现为绿，在 before+同测试上可复现为红。无范围外改动，005 未删。009 父链红与 007 `--check` 双漂移据实保留。无新修复 ID。本票不是 B5，不是模块收口。
