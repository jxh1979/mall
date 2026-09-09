CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M008 B4-r2 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r2
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（两席；本席 grok-4.6 xhigh；不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁；不问操作员）
本文件路径：docs/audit-2026-09/modules/M008-b4-r2-grok-raw.md
本文件自身版本：V0.01（指定路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r2 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。本轮已独立取树、取无过滤 binary diff、核对三文件当前等于 after、逐块逐行归因，并在隔离 overlay / 真实入口副本 / 原生 shell 桩上复跑正反例。

未读取：任何 `M008-b4-r2-self-raw.md`（当前亦不存在）、Codex 本次 B4 raw。原 B4-r1 / B2 报告只作已定范围与历史事实，不替代本轮完整 diff 审计。定稿队列以 findings.md V1.14 顶部正式表为准：001–004、006–007 是；005、008–016 否。本批实际改动只针对 001 与 007；002–004/006 保持、005 重复常量未删。

本轮实际应用技能（V0.17 收窄，不引入第三席、不派子代理）：requesting-code-review / receiving-code-review（本席自行审完整 diff）；verification-before-completion；test-driven-development（只核已落测试的红绿，不写新测试）；diagnosing-bugs（helper 矩阵与 overlay 红环）；ponytail-review（本 diff 无额外可删抽象）；grilling（自问自答，未询问操作员）。判据仅用冻结 criteria-lock 既有 GEN-001–018 及 PGM-036，未发明新判据。

---

## 1. 输入与独立锁实算

| 项 | 值 |
|---|---|
| 模块 | M008 治理门禁与 CI 基线／检查及生成工具／第1段 |
| 阶段 | B4-r2 只读「修复内容」完整 diff 独立审计 |
| 正式队列 | findings.md V1.14：001–004、006–007 是；005/008–016 否 |
| B3 | 已执行 2 次（本轮为 r2），模块仍 IN_PROGRESS |
| LOCK-AMEND-COUNT | 1 |
| before（对象类型 tree，可解析） | `bc3c7e12d74903e55f4e8063960d2de312fc6d19` |
| after（对象类型 tree，可解析） | `07741199b927955fefef9a03510ddcc32f4608c8` |
| 本席自取 | `git cat-file -t` 两者均为 `tree`；`git rev-parse <id>^{tree}` 与输入 id 相同 |
| 完整 diff 命令 | `git diff --binary bc3c7e12d74903e55f4e8063960d2de312fc6d19 07741199b927955fefef9a03510ddcc32f4608c8`（无路径过滤，不只看 HEAD 已提交） |
| 实际差异 | 7174 bytes；SHA256 `420c0652369c26e3dd05e8971765936cb0d5f3252bccae1dc5ef3d27c455342d`；UTF-8；98 行 unified（含末 NL）；3 文件；18 插入 / 10 删除；7 hunk |
| `git diff --check` | exit 0 |
| 两树路径清单 | 各 7226 条，差集空；`--raw` 仅 3 条 blob 变化，均为 M |
| HEAD | `242f487233d028173cb9198eecd079f5d877405c` = `origin/main` |
| HEAD^{tree} = origin/main^{tree} | `2af03dee3a7d65c4e745c9aa3173da2050890a65`（≠ after；after 为临时 write-tree） |
| 主索引 SHA256 | `eeb0e20f0c85989b6b902fba051c6b7b2f3051b870cacabd0c2ab6e2211b42aa` |
| `git diff --cached` | 空 |
| 主索引 write-tree | `2af03dee3a7d65c4e745c9aa3173da2050890a65`（= HEAD^{tree}） |
| 工作区三文件 blob | 均等于 after（见 §6） |
| findings.md | V1.14；418170 bytes；SHA256 `25ab53318c38ddc44e7ea670b8e1c3f31f821a8a05ce64fe7cc0cdfc2e2469a9` |
| 本席 B2-r3 票 | modules/M008-b1-grok-raw.md V0.04；83685 bytes；SHA256 `6e3b0f7cbdce80bd5f0af1aeda641bfcf692b6dee9d5c14f3e877f46d556586d` |
| 三 HTML / README / 00-entry / 基线 / 锁 两树 blob | 完全相同，diff 未改这些路径 |
| findings / ledger / B1 grok raw 两树 blob | 完全相同（报告/台账未介入两树） |

`git diff --stat --binary`：

```
 tools/gate-hardening-tests/m001-regression.mjs | 15 ++++++++++-----
 tools/refresh-entry-doc-baseline.mjs           |  5 ++++-
 tools/verify-delivery-gates.mjs                |  8 ++++----
 3 files changed, 18 insertions(+), 10 deletions(-)
```

`git diff --raw --binary`：

```
:100644 100644 208cb941 772ad6dc M	tools/gate-hardening-tests/m001-regression.mjs
:100644 100644 1faf6e74 34355d1e M	tools/refresh-entry-doc-baseline.mjs
:100644 100644 5fdfb898 6e19c620 M	tools/verify-delivery-gates.mjs
```

本席独立重算锁（命令 `sha256sum docs/audit-2026-09/plan/criteria-lock.md`）：

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 同行。criteria-lock 版本 V0.11。映射 `M008 → [GEN-001…GEN-018, PGM-036]`，共 19 条，本轮只用这 19 条。ledger 顶部锁同行，`LOCK-AMEND-COUNT: 1`。

---

## 2. 锁、分类与正式队列（既有，不新造）

B2-r3 定稿：001–004、006–007 技术性、工程缺陷是、修复队列是；005、008–016 否。本轮只核 001/007 是否按正式表修好、002–004/006 是否保持、005 六个重复常量是否未删，以及每行是否归到允许的 ID（含最小测试与自身版本）。不为当前全链绿编辑正本。

适用冻结判据：GEN-001 最小性、GEN-002 可执行正确性、GEN-003 测试有效性、GEN-004 信任边界、GEN-006 完整性、GEN-008 接口一致、GEN-009 错误处理、GEN-010 安全、GEN-011 可观测、GEN-013 构建/依赖、GEN-015 界面语言、GEN-016 文档可验证、GEN-017 分类、GEN-018 第一性原理；PGM-036 设备管理（本 diff 无物理设备实现面，沿用 016 仅登记）。GEN-005/007/012/014 对本 diff 无对应实现面。

GEN-017：三文件改动均为治理检查/生成器/回归，无票务/财务/审批/枚举/设备发明，保持技术性。三 HTML 两树 blob 不变。

005：`requiredDeliveryCommands` 数组 before/after 全文相等（50 项、44 unique、6 次额外重复；数组块 SHA256 `d14c159fcefbb0692fe942bd4c93daebc5c9c39129af7c716984c53079789f80`）。本 diff 未删重复常量。

---

## 3. 完整未过滤 binary diff（全文，非链接）

命令：`git diff --binary bc3c7e12d74903e55f4e8063960d2de312fc6d19 07741199b927955fefef9a03510ddcc32f4608c8`

字节 7174；SHA256 `420c0652369c26e3dd05e8971765936cb0d5f3252bccae1dc5ef3d27c455342d`；`git diff --check` exit 0。本席自取全文如下，与磁盘 `/tmp/m008-b4-r2-full.diff` 及二次 `git diff --binary` 管道 SHA 相同。

````diff
diff --git a/tools/gate-hardening-tests/m001-regression.mjs b/tools/gate-hardening-tests/m001-regression.mjs
index 208cb941..772ad6dc 100644
--- a/tools/gate-hardening-tests/m001-regression.mjs
+++ b/tools/gate-hardening-tests/m001-regression.mjs
@@ -1,4 +1,4 @@
-// 文件版本：V0.06
+// 文件版本：V0.07
 // 实际运行治理脚本；输入和模拟合并均在临时目录，绝不操作真实 PR。
 import assert from "node:assert/strict";
 import fs from "node:fs";
@@ -184,15 +184,18 @@ try {
   // M008-001：实际运行检查不能被打印文字、注释或相似脚本名替代。
   const originalScripts = { ...pkg.scripts };
   for (const [script, gate] of [["verify:predeploy", "verify:html-param-tier"], ["verify:delivery-gates", "verify:unit"]]) {
-    for (const [name, value] of [
+    for (const [name, value, expected = 1] of [
       ["打印", originalScripts[script].replace(`npm run ${gate}`, `echo ${gate}`)],
       ["相似名", originalScripts[script].replace(`npm run ${gate}`, `npm run ${gate}-extra`)],
       ["整串打印", `echo ${JSON.stringify(originalScripts[script])}`],
       ["注释", `echo skipped # ${originalScripts[script]}`],
+      ["命令内换行", originalScripts[script].replace(`npm run ${gate}`, `npm\nrun ${gate}`)],
+      ["&& 前换行", originalScripts[script].replace(" && ", "\n&& ")],
+      ["&& 后续行", originalScripts[script].replaceAll(" && ", " &&\n"), 0],
     ]) {
       pkg.scripts = { ...originalScripts, [script]: value };
       put(path.join(delivery, "package.json"), JSON.stringify(pkg));
-      expectExit(`M008-001 ${script} ${name}`, run(deliveryGate, delivery), 1);
+      expectExit(`M008-001 ${script} ${name}`, run(deliveryGate, delivery), expected);
     }
   }
   pkg.scripts = originalScripts;
@@ -247,7 +250,7 @@ exit 0
   const refresh = path.join(temp, "refresh");
   const entryFile = "docs/00-current-effective-document-entry-v0.1.md";
   const history = "历史记录：V385 / 1606 tests，保留当时事实。";
-  put(path.join(refresh, entryFile), "# 临时入口\n本轮 2026-09-01 实测 OpenAPI 720 operations、Flyway 最新 V385；当前工作树无 `backend/target/surefire-reports`，故不声明当前测试数量。\n" + history + "\n");
+  put(path.join(refresh, entryFile), "# 临时入口\n本轮 2026-09-01 实测 OpenAPI 720 operations、Flyway 最新 V385；当前工作树无 `backend/target/surefire-reports`，故不声明当前测试数量。\n本轮 2026-09-01 取证为 Flyway 最新 V385、OpenAPI 720 operations；当前工作树无后端 surefire 报告，不声明当前测试数量。\n" + history + "\n");
   put(path.join(refresh, "README.md"), "<!-- baseline:auto-begin -->\n<!-- baseline:auto-end -->\n");
   put(path.join(refresh, "docs/openapi/openapi-p0.json"), JSON.stringify({ paths: { "/sample": { get: {}, post: {} } } }));
   put(path.join(refresh, "backend/src/main/resources/db/migration/V12__sample.sql"), "select 1;\n");
@@ -257,12 +260,14 @@ exit 0
   expectExit("M008-007 回填快照", run(generator, refresh), 0);
   const refreshed = fs.readFileSync(path.join(refresh, entryFile), "utf8");
   assert.match(refreshed, /实测 OpenAPI 2 operations、Flyway 最新 V12；后端 surefire 现有报告 3 tests。/);
+  assert.match(refreshed, /取证为 Flyway 最新 V12、OpenAPI 2 operations；后端 surefire 现有报告 3 tests。/);
   assert.ok(refreshed.includes(history), "M008-007 历史记录应保留");
   expectExit("M008-007 回填后校验", run(generator, refresh, ["--check"]), 0);
   fs.unlinkSync(reportFile);
-  put(path.join(refresh, entryFile), refreshed.replace("2 operations", "99 operations"));
+  put(path.join(refresh, entryFile), refreshed.replaceAll("2 operations", "99 operations"));
   expectExit("M008-007 无报告仍回填计数", run(generator, refresh), 0);
   assert.match(fs.readFileSync(path.join(refresh, entryFile), "utf8"), /实测 OpenAPI 2 operations、Flyway 最新 V12；后端 surefire 现有报告 3 tests。/);
+  assert.match(fs.readFileSync(path.join(refresh, entryFile), "utf8"), /取证为 Flyway 最新 V12、OpenAPI 2 operations；后端 surefire 现有报告 3 tests。/);
   assert.deepEqual(failures, [], `治理回归失败：${failures.join("、")}`);
   console.log("M001_GOVERNANCE_REGRESSION_PASS");
 } finally {
diff --git a/tools/refresh-entry-doc-baseline.mjs b/tools/refresh-entry-doc-baseline.mjs
index 1faf6e74..34355d1e 100644
--- a/tools/refresh-entry-doc-baseline.mjs
+++ b/tools/refresh-entry-doc-baseline.mjs
@@ -1,4 +1,4 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 // 00-entry + README 事实基线快照自动回填（审计 P2-4 建议"计数由工具自动回填"的落地，根治手工刷反复过期）。
 // 权威来源：openapi-p0.json（operation 数）/ db/migration（最新迁移+迁移数）/ surefire-reports（测试数）。
 // 用法：node tools/refresh-entry-doc-baseline.mjs        —— 回填快照（写文件）
@@ -66,6 +66,9 @@ entryText = entryText.replace(
 entryText = entryText.replace(
     /本轮 \d{4}-\d{2}-\d{2} 实测 OpenAPI \d+ operations、Flyway 最新 V\d+；(当前工作树无 `backend\/target\/surefire-reports`，故不声明当前测试数量|后端 surefire 现有报告 \d+ tests)。/g,
     (m, oldStatus) => `本轮 ${today} 实测 OpenAPI ${operationCount} operations、Flyway 最新 ${latestMigrationTag}；${testCount === null ? oldStatus : `后端 surefire 现有报告 ${testCount} tests`}。`);
+entryText = entryText.replace(
+    /本轮 \d{4}-\d{2}-\d{2} 取证为 Flyway 最新 V\d+、OpenAPI \d+ operations；(当前工作树无后端 surefire 报告，不声明当前测试数量|后端 surefire 现有报告 \d+ tests)。/g,
+    (m, oldStatus) => `本轮 ${today} 取证为 Flyway 最新 ${latestMigrationTag}、OpenAPI ${operationCount} operations；${testCount === null ? oldStatus : `后端 surefire 现有报告 ${testCount} tests`}。`);
 entryText = entryText.replace(/查看本轮核验 \d+ 个 operation/g,
     `查看本轮核验 ${operationCount} 个 operation`);
 entryText = entryText.replace(/本轮核验 \d+ 个 OpenAPI operation/g,
diff --git a/tools/verify-delivery-gates.mjs b/tools/verify-delivery-gates.mjs
index 5fdfb898..6e19c620 100644
--- a/tools/verify-delivery-gates.mjs
+++ b/tools/verify-delivery-gates.mjs
@@ -1,13 +1,13 @@
-// 文件版本：V0.03
+// 文件版本：V0.04
 import fs from "node:fs";
 import path from "node:path";
 import { parse } from "yaml";
 
 // ponytail: 识别本仓现有的 npm run && 链；引入其它 shell 语法时再使用专门解析器。
 function npmCommands(script = "") {
-  const parts = script.trim().split(/\s*&&\s*/);
-  if (!parts.every(command => /^npm\s+run\s+[\w:-]+$/.test(command))) return new Set();
-  return new Set(parts.map(command => command.split(/\s+/)[2]));
+  const parts = script.replace(/^[ \t\n]+|[ \t\n]+$/g, "").split(/[ \t]*&&[ \t\n]*/);
+  if (!parts.every(command => /^npm[ \t]+run[ \t]+[\w:-]+$/.test(command) && !command.includes("\n"))) return new Set();
+  return new Set(parts.map(command => command.split(/[ \t]+/)[2]));
 }
 
 const repoRoot = process.cwd();
````

---

## 4. 逐块逐行正式 ID 归因

全部 7 hunk、全部 `+`/`-` 行均落入正式队列 001 或 007。无无 ID 行，无 005/008–016 范围改动，无 002–004/006 无意义编辑。

### 4.1 `tools/gate-hardening-tests/m001-regression.mjs`（V0.06→V0.07；blob 208cb941→772ad6dc）

| hunk | 行（after 约） | 变更 | ID |
|---|---|---|---|
| H1 `@@ -1,4 +1,4` | L1 | 文件版本 V0.06→V0.07。本文件本轮同时含 001 与 007 测试编辑，版本行归这两项共享，不单列新 ID | M008-001、M008-007 |
| H2 `@@ -184,15 +184,18` | L187 | `for` 增加 `expected = 1` 默认，使合法 `&&` 后续行可期望 0 | M008-001 |
| H2 | L192–194 | 新增「命令内换行」「&& 前换行」期望 1；「&& 后续行」`replaceAll(" && ", " &&\n")` 期望 0。两真实入口 `verify:predeploy`/`verify:delivery-gates` 各跑一遍 | M008-001 |
| H2 | L197 | `expectExit(..., 1)` 改为 `expectExit(..., expected)` | M008-001 |
| H2 上下文 | L188–191 | 「打印/相似名/整串打印/注释」仍期望 1，无行为改写 | M008-001（保持） |
| H3 `@@ -247,7 +250,7` | L253 | 同一最小 fixture 在既有「实测」句后追加现行「取证为」句（V385 / 无报告句式） | M008-007 |
| H4 `@@ -257,12 +260,14` | L263 | 回填后断言「取证为 … V12 / 2 operations / 3 tests」 | M008-007 |
| H4 | L267 | `replace`→`replaceAll("2 operations", "99 operations")`，使无报告路径同时扰动实测/取证两句 | M008-007 |
| H4 | L270 | 无报告回填后再断言取证句恢复为 2 operations / 3 tests | M008-007 |
| H4 上下文 | L262/265/269 | 既有实测断言、历史 `includes`、无报告实测断言保留 | M008-007（保持） |

### 4.2 `tools/refresh-entry-doc-baseline.mjs`（V0.01→V0.02；blob 1faf6e74→34355d1e）

| hunk | 行 | 变更 | ID |
|---|---|---|---|
| H5 `@@ -1,4 +1,4` | L1 | 文件版本 V0.01→V0.02 | M008-007 |
| H6 `@@ -66,6 +66,9` | L69–71 | 新增当前快照句式：`本轮 YYYY-MM-DD 取证为 Flyway 最新 Vn、OpenAPI n operations；` + 无报告/有报告两种状态捕获。无报告保留 `oldStatus`，有报告回填 tests。不碰历史句、不改 README 自动块逻辑 | M008-007 |

既有 L66–68「实测 OpenAPI」正则保持，对应 00-entry:126。新正则对应 00-entry:160。本席实读正本仅这两处「实测 OpenAPI」「取证为」快照句。

### 4.3 `tools/verify-delivery-gates.mjs`（V0.03→V0.04；blob 5fdfb898→6e19c620）

| hunk | 行 | 变更 | ID |
|---|---|---|---|
| H7 `@@ -1,13 +1,13` | L1 | 文件版本 V0.03→V0.04 | M008-001 |
| H7 | L8–10 | `npmCommands`：首尾只剥空格/tab/LF；按 `[ \t]*&&[ \t\n]*` 切开（允许 && 后 LF，不允许 && 前 LF）；每段必须 `^npm[ \t]+run[ \t]+[\w:-]+$` 且 `!includes("\n")`；命令内分隔改为 `[ \t]+` 而非 `\s+` | M008-001 |

调用点仍为 `verify:delivery-gates`（约 L161）与 `verify:predeploy`（约 L321）。`requiredDeliveryCommands` 数组全文未出现在 diff 中。

---

## 5. 修复内容是否闭合

### 5.1 M008-001（是）— 闭合

正式事实：r1 helper 把 `npm` 与 `run` 间 LF、以及 `&&` 前 LF 当 `\s` 空白，两入口假绿；`&&` 后 LF 为合法续行须保持通过。

after helper 实读：

```
function npmCommands(script = "") {
  const parts = script.replace(/^[ \t\n]+|[ \t\n]+$/g, "").split(/[ \t]*&&[ \t\n]*/);
  if (!parts.every(command => /^npm[ \t]+run[ \t]+[\w:-]+$/.test(command) && !command.includes("\n"))) return new Set();
  return new Set(parts.map(command => command.split(/[ \t]+/)[2]));
}
```

独立把 before/after helper 抽到 `/tmp/m008-b4-r2-verify-Jm2xLh`，对真实 `package.json` 两入口做矩阵（before 假绿 / after 拒绝 已标）：

| 入口 | 用例 | before 空集 | after 空集 | after 仍含目标 gate |
|---|---|---|---|---|
| verify:predeploy（15 命令，目标 html-param-tier） | 合法原文 | 否 size=15 | 否 size=15 | 是 |
| 同上 | 命令内换行 `npm\nrun` | 否 size=15 仍有 gate | 是 size=0 | 否 |
| 同上 | `&&` 前换行 | 否 size=15 仍有 gate | 是 size=0 | 否 |
| 同上 | `&&` 后续行 | 否 size=15 | 否 size=15 | 是 |
| 同上 | TAB 命令 / `&&` 前仅 TAB / 首尾 LF | 否 | 否 | 是 |
| verify:delivery-gates（49 命令，目标 verify:unit） | 同上七类 | 同构：非法两类 before 假绿 after 空集；合法类两边都有 gate | | |

TAB 是正式允许的行内分隔，不是 `&&` 前 LF。

原生 shell（隔离 PATH：`npm` 桩 exit 0 记 `NPM_RUN`；裸 `run` 桩 exit 127 记 `BARE_RUN`；cwd 为可清理临时目录，未跑生产全链）：

| 入口 | 用例 | bash exit | 到达 npm run | 裸 run 桩 |
|---|---|---|---|---|
| verify:predeploy | 合法链 | 0 | 15 次，首个 html-ledger-sync | 无 |
| 同上 | `&&` 后 LF | 0 | 15 次 | 无 |
| 同上 | 命令内 LF | **127**（裸 run 桩） | 2 次后在 html-param-tier 处拆成 `run` | `verify:html-param-tier` |
| 同上 | `&&` 前 LF | **2** syntax error near `&&` | 仅 1 次（首段） | 无 |
| verify:delivery-gates | 合法链 | 0 | 49 次，首个 delivery-gates-docs | 无 |
| 同上 | `&&` 后 LF | 0 | 49 次 | 无 |
| 同上 | 命令内 LF | **127** | 7 次后在 unit 处拆开 | `verify:unit` |
| 同上 | `&&` 前 LF | **2** syntax error near `&&` | 仅 1 次 | 无 |

127 是隔离 `run` 桩显式返回，不是生产全链退出码。门禁层（`verify-delivery-gates.mjs` 解析、不执行 npm 链）的红绿见 §7：before overlay 上四例非法仍 `DELIVERY_GATES_DOCS_PASS`（期望 1）；after 工作区四例非法 PASS（期望 1）且两例 `&&` 后续行 PASS（期望 0）。

结论：001 根因已按正式句式收窄，两入口闭合，合法续行保留。

### 5.2 M008-007（是）— 闭合

正式事实：生成器须匹配 126「实测」与 160「取证为」两种当前快照；无报告保留旧测试陈述；不碰历史；不改 README/入口正本。

after 在既有「实测」替换后增加「取证为」替换。正本 00-entry 仅 L126、L160 两处该类快照句。隔离真实入口（复制 00-entry / README / OpenAPI / 377 个迁移 / surefire xml 到 `/tmp/m008-b4-r2-shell-VEohZV/real-entry`，**只在隔离目录写**）：

- 写前 L126/L160 仍为 2026-09-01、V385、无 surefire 声明
- `node tools/refresh-entry-doc-baseline.mjs` cwd=隔离目录 exit 0：`已回填 … 720 ops / V399（377 迁移） / 55 tests（2026-09-09）`
- 写后 L126：`本轮 2026-09-09 实测 OpenAPI 720 operations、Flyway 最新 V399；后端 surefire 现有报告 55 tests。`
- 写后 L160：`本轮 2026-09-09 取证为 Flyway 最新 V399、OpenAPI 720 operations；后端 surefire 现有报告 55 tests。`
- 隔离 `--check` exit 0
- 正本 00-entry SHA256 仍 `34e9c9f76578b61eb659dc4e369b274f62adb110b5f4dd3895bc0bb08f83e314`；正本 README SHA256 仍 `28a46aea13227b0e3e101f9dc0a2970b6e3e581ca5995d291288aa59cb2d23b5`

同一最小 fixture、before 生成器 vs after 生成器：

| | before 生成器 | after 生成器 |
|---|---|---|
| 写 exit | 0 | 0 |
| 实测→V12/2ops/3tests | 是 | 是 |
| 取证→V12/2ops/3tests | **否，仍 V385/720/无报告** | **是** |
| 历史句保留 | 是 | 是 |
| 写后 `--check` | 0（126 已刷、160 仍旧 → 假绿） | 0（两句都刷） |
| 删 surefire 后把 2ops 改 99 再写 | 实测恢复 2ops/3tests；取证仍不刷 | 实测与取证均恢复 2ops/3tests，不留 99 |

结论：007 对 126/160 当前句式闭合；无报告不扰动 tests；历史保留；正本未写。

### 5.3 M008-002/003/004/006（是，本批保持）— 未回退

冻结 17 文件里本轮只改 3 个；其余 14 个 before/after blob 全等（含 `verify-entry-anchors.mjs` `verify-html-ledger-sync.mjs` `verify-html-param-tier.mjs` 及 10 份历史 log、基线 json）。after 工作区回归 77 PASS 含 002 空正文、003 删台账、004 换行/同行两段、006 execFileSync 注入基线。before overlay 上这些用例仍 PASS（r1 已在 before 树）。本 diff 无这些文件。

### 5.4 M008-005（否）— 未误删

`requiredDeliveryCommands` 50/44 全文相等。6 次额外重复键仍在。

### 5.5 008–016（否）— 未为全链绿改正本

真实 cwd 只读：

- `verify-delivery-gates.mjs` exit 0 `DELIVERY_GATES_DOCS_PASS`
- `verify-html-param-tier.mjs` exit 0
- `verify-entry-anchors.mjs` exit 1，仍原 6 红（BSM-E0433/E0736、BSM-0283 source/preconditions/state_transition、BSM-0530 source）→ 009
- `verify-html-ledger-sync.mjs` exit 1 台账未同步 → 009
- `refresh-entry-doc-baseline.mjs --check` exit 1：`DRIFT: docs/00-current-effective-document-entry-v0.1.md + README.md … 应为 720 ops / V399（377 迁移） / 55 tests` → 008/M435/M436 资料，生成器在隔离目录已能刷 126/160，正本按定稿不回填

未对主仓跑默认写、`--write-baseline`、安装 hook。

---

## 6. 工作区三文件 vs after；其余 M008 文件

| 路径 | 字节 | SHA256 | 工作区 blob | after blob |
|---|---|---|---|---|
| tools/gate-hardening-tests/m001-regression.mjs | 17155 | c64e3048e7525c6158e102f9cd9eaf9cd10972c1255f9a2deb5528a8299383a6 | 772ad6dc… | 772ad6dc… |
| tools/refresh-entry-doc-baseline.mjs | 7807 | 8bd1ba5ac141ef47dbcd409616fcacc3303546b06cceba998852895fac4f8cbb | 34355d1e… | 34355d1e… |
| tools/verify-delivery-gates.mjs | 16444 | cbdfe7c023c3a5cc623b4f1742c56ca46238f039d701150d8d1b6abbe9102b8f | 6e19c620… | 6e19c620… |

三文件工作区逐字节等于 after。`node --check` 三文件均为 0。HEAD 三 blob ≠ before（before 已含 r1，main 未提交），符合临时 write-tree 而非已提交修复。

保护路径 before=after=工作区 blob：三 HTML、README、00-entry、html-param-tier-baseline.json、criteria-lock.md / .sha256。

---

## 7. 新增回归：before 红 / after 绿（独立 overlay）

**绿（工作区 = after 源 + after 测试）：** `node tools/gate-hardening-tests/m001-regression.mjs` exit 0；`^PASS ` 77 行；`^FAIL ` 0；末行 `M001_GOVERNANCE_REGRESSION_PASS`。其中 M008-001 十四例（两入口×打印/相似名/整串打印/注释/命令内换行/`&&` 前换行/`&&` 后续行）及 M008-007 三例 expectExit 全部 PASS。

**红（隔离 overlay `/tmp/m008-b4-r2-red-VrBxPI`：`git archive` after 后把 delivery/refresh 换成 before 字节，测试文件保持 after V0.07；`ln -s` 主仓 node_modules；overlay 内独立 `git init` 一提交，不碰主索引）：**

- overlay delivery SHA256 = before `73bcf494770156441945c0ea2ff6afec30ae4489a48d7cc1c0a3225b9221a78f`
- overlay refresh SHA256 = before `2ba460a532e9571cf18cc270e77dfc7c0385c0b132e0c2351431f3a710b6f1d7`
- overlay 测试 SHA256 = after `c64e3048e7525c6158e102f9cd9eaf9cd10972c1255f9a2deb5528a8299383a6`
- exit 1；`^PASS ` 71；`^FAIL ` 4：
  - `FAIL M008-001 verify:predeploy 命令内换行: DELIVERY_GATES_DOCS_PASS`
  - `FAIL M008-001 verify:predeploy && 前换行: DELIVERY_GATES_DOCS_PASS`
  - `FAIL M008-001 verify:delivery-gates 命令内换行: DELIVERY_GATES_DOCS_PASS`
  - `FAIL M008-001 verify:delivery-gates && 前换行: DELIVERY_GATES_DOCS_PASS`
- `PASS M008-001 … && 后续行` 两入口仍过（before helper 本就接受 `&&` 后 LF）
- `PASS M008-007 回填快照`（写 exit 0）后 `assert.match` 取证句未捕获失败：实测已刷 V12/3tests，取证仍 `2026-09-01 … V385 … 无后端 surefire 报告`；历史句仍在。进程 `AssertionError` exit 1。007 无报告第二断言因进程中止未在 overlay 跑到，已用 §5.2 独立 fixture 补证。

这是本席独立红绿，不是转述 B3。

---

## 8. 封存过程异常（据真实树核验，不凭说明接受）

对象 `178b5b99f91f6a83c36e29cbaefcfa88b2865392` 仍为 tree。`git diff --name-status --binary 178b5b99… bc3c7e12…` 仅：

```
D	.scratch/m008-r2-index-nmttkkcs/index.lock
```

stat：`1 file changed, 0 insertions(+), 0 deletions(-)`；该 blob 为 empty `e69de29b`。正式 before→after 完整 diff 只有上述 3 个源/测试文件。本席据此接受正式 before/after 作为本轮基线；未把错误 before 当正式基线。

主索引 SHA 在本席命令前后均为 `eeb0e20f0c85989b6b902fba051c6b7b2f3051b870cacabd0c2ab6e2211b42aa`，cached 空。本席未 `git add` 主仓、未真实 commit/push/PR/merge、未部署、未装 hook。

---

## 9. 冻结 19 条对本 diff

| ID | 本 diff |
|---|---|
| GEN-001 | 只收窄 helper 空白与补一条现行句式正则；无新抽象/新门禁项目。005 重复常量未借机删除 |
| GEN-002 | 两入口非法 LF 不再假绿；合法 `&&` 后 LF 仍解析且原生 shell 整链到达 npm。126/160 隔离写后与权威 V399/55tests 一致 |
| GEN-003 | 新增四例+取证断言能在 before 红、after 绿；002–004/006 旧回归仍绿 |
| GEN-004 | 脚本字符串按严格 npm-run&& 形状取命令名；含 LF 的非法形状空集失败，不以 includes 冒充 |
| GEN-005 | 无授权面 |
| GEN-006 | 007 无报告路径不把 99 写进 tests；历史句保留 |
| GEN-007 | 无并发面 |
| GEN-008 | 生成器句式与现行 00-entry:126/160 一致；helper 仍只认本仓 `npm run &&` 链 |
| GEN-009 | 非法形状空集→门禁缺命令→exit 1；不再伪成功 |
| GEN-010 | 未引入 shell 拼接执行；本轮 Git 调用不在 diff 内（006 已在 before 用 argv） |
| GEN-011 | 回归 FAIL 行带入口与用例名；隔离 `--check` 在正本漂移时仍红 |
| GEN-012 | 无资源面 |
| GEN-013 | 无新依赖；测试仍直接 `node` 跑既有 mjs |
| GEN-014 | 无迁移面 |
| GEN-015 | 无产品 UI；CLI 内部 PASS/FAIL/DRIFT 未顺手翻译（014） |
| GEN-016 | 不把隔离回填或 B3 自报写成正本已刷；正本 00-entry/README SHA 未变 |
| GEN-017 | 技术性，无业务发明 |
| GEN-018 | 结论落到两树 diff、helper 矩阵、原生 shell、overlay 红绿、隔离写读 |
| PGM-036 | 无设备绑定/点位/班次实现；沿用 016 |

---

## 10. 本轮观察（审完全部 diff 后一次性列出）

### 观察 GR-M008-B4-R2-OBS-01

- 分类：技术性
- 工程缺陷：否（不申请新 ID，不入修复队列）
- 位置：`tools/gate-hardening-tests/m001-regression.mjs` L262–270；overlay 红日志 AssertionError at L263
- 事实：007「取证为」内容断言是裸 `assert.match`，不经 `expectExit`。before overlay 上 001 四例已记入 `failures` 后，取证断言抛未捕获异常，进程 exit 1，后续「回填后校验 / 无报告」expectExit 未跑。after 绿路径该断言通过，三例 expectExit 均 PASS。
- GEN：GEN-003/011。断言能失败且失败时非 0，不是假绿；无报告路径已用独立 fixture 补证。不把测试汇总风格升级成必须修复项，也不扩 GEN。

无其它新缺陷。未发现无 ID 行、越界改 README/00-entry/三 HTML/基线/锁/台账/findings、或 005 被删。

ponytail-review：`Lean already. Ship.` 新「取证为」正则与既有「实测」正则并列，正是正式要求的最小句式匹配，不另抽解析器。

---

## 11. 结论

| 项 | 结论 |
|---|---|
| 修复内容 | **PASS**。001 helper 已禁止命令内 LF 与 `&&` 前 LF，保留 `&&` 后 LF；两入口测试与原生 shell 一致。007 生成器匹配 126 与 160，无报告不扰动 tests，历史保留，正本未写。002–004/006 保持。005 未删。 |
| 范围归因 | **PASS**。完整 7174 字节 / 7 hunk / 18+10 全部归 001 或 007；无无 ID 行；未改正式禁止的正本/HTML/基线/锁/台账。 |
| 总 | **PASS**。仅席乙本轮票。须待席甲独立 B4-r2 后合取；合取前不出队、不进 B5。 |

本席未修复、未派第三席、未问操作员、未读本轮 self-raw。
