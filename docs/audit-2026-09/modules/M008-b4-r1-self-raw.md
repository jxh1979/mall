CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M008 B4-r1 Codex完整独立修复复审
版本：V0.01
当前步骤B4-r1；M008 IN_PROGRESS，B3累计1；不是B5或模块收口。修复内容结论FAIL；每行范围归因PASS；本席总FAIL。
Grok通过固定无头命令先真实结束exit0，只收到指定M008-b4-r1-grok-raw.md路径与退出码。本席随后重新读取完整两树diff、逐文件验证当前bytes等于after、6个node --check、完整71条回归，再做独立反例与当前资料检查；写完本报告之前未读Grok本B4正文或原始JSON。
固定调用：/home/james/.local/bin/grok -p <本轮M008 B4完整差异复审要求> -m grok-4.6 --reasoning-effort xhigh --output-format json --permission-mode bypassPermissions --no-subagents --disable-web-search --cwd /home/james/complex-management；通过既定路径适配器，指定出处modules/M008-b4-r1-grok-raw.md。未用MCP/第三席。技能按本会话已实读的requesting/receiving-code-review、verification-before-completion、systematic-debugging、TDD及ponytail收窄使用；不派子代理、不在复审中修源。
before 18894a9ee67577c6c8c3d19d99281f338a82b976 → after 97b93d8f8e7ef71e8706dd428c4315499436b015；完整binary diff 21637bytes SHA996b9954f7b4782aafca8ef5904a556dcee90067c0919311651b85139c8089d2。6文件98插入/31删除。真实主索引B3前后均eeb0e20f0c85989b6b902fba051c6b7b2f3051b870cacabd0c2ab6e2211b42aa；本B4 cached diff仍空。全部源改动依findings V1.12正式001–004/006–007。005否，原50项44唯一常量区全文与before相同。
当前B4只审本轮6文件完整差异与最窄上下文；冻结判据GEN001–018/PGM036共19不扩。三HTML、参数基线、README、入口资料及历史日志无本轮diff，保护文件bytes当前与before相同。

## 修复内容逐项结论（完整审完，不在首个问题处停止）
| ID | 结论及验证 |
|---|---|
| M008-001 | FAIL：原echo/相似名/整串打印/注释8反例已被修复且当前通过回归；但新helper把LF当命令内空格，也吞掉&&之前非法换行，仍声称必跑命令存在。具体独立反例见下。原ID仍在队列，不分配新ID。 |
| M008-002 | PASS：空正文明确失败，有效CSV、BOM/CRLF、跨行字段通过，非法列数/引号被拒；真实原6处资料不匹配仍拒绝，未用豁免换绿。 |
| M008-003 | PASS：工作区删台账拒绝，已提交删除也拒绝，即使工作区有未跟踪同名副本也不能覆盖提交区事实；正常更新通过，原比较范围保持。 |
| M008-004 | PASS：段内换行通过、同物理行两个p未标第二段拒绝，段后数值豁免保留且不扩大到另一段，空/部分豁免仍拒绝；当前三HTML精确副本新扫描器全量0项。按本仓块结构收窄解析，未引第三方依赖或HTML改动。 |
| M008-006 | PASS：两个脚本所有Git入口及show均以argv调用；干净树非法基线argv/env均拒绝，合法PR/push/多提交/部署基线和棘轮路径回归通过。 |
| M008-007 | PASS：实际126当前句式经隔离复制，check指出入口与README、写后126更新V399/55tests且check通过；最小回归同时断言实际2ops/V12/3tests、历史句保留和无报告时保留旧测试数。未回填正本M435/M436。 |

B4范围归因PASS：下方完整diff所有增删行逐块能归上述正式ID；无无ID代码/测试/依赖修改，不要求因内容FAIL回滚已归因的合规修复。本席仍保留六项修复队列，必须读完Grok本批完整意见后整批按§4路由。

## 未修好的原finding M008-001
位置tools/verify-delivery-gates.mjs:7–10，npmCommands内split和词间匹配都使用\\s，包含换行。
冻结依据GEN-002/004/008/009/013，2.3类型2/4；分类技术性，工程缺陷是，无业务发明。不是新finding/新判据。
第一性原理：既有门禁要求实际npm run调用存在；一个行分隔符不是npm与run之间的普通空格。对同一完整当前链，只把目标“npm run”拆成两行，检查退出0并打印DELIVERY_GATES_DOCS_PASS，而原生/bin/sh将其分成npm与run两个命令，要求的npm调用未发生。另把&&放到下一行，同样检查0而shell语法退出2。
对照：原链、&&后合法换行，检查均0、shell也0、目标npm调用发生。两个真实受检入口都复现；不是仅测试helper或依靠字符串断言。
隔离shell只把npm/run外部进程替换为无害参数记录程序；run替身主动返回127，表示它是单独命令，并非执行真实npm门禁。LF在&&之前的exit2为原生shell语法拒绝。未运行完整生产门禁、无发布。关键证据是实际命令边界及目标npm调用缺失，不能把替身exit127冒称真实完整npm链实跑。
这是原001“实际执行位置识别”尚未修全，格式化命令时一次插入换行即触发，不需并发/网络组合。最小方向：区分单条命令内空格与换行，仅保留&&后合法续行，不引入通用shell解析器/新门。B4不动源。

## 其余观察与边界
001所用简化器支持本仓已有npm run &&链；不在本轮新增其它shell语法支持。005常量未删，原真诊断重复仍仅登记，不重新开启已裁定队列。当前anchors六处资料红、ledger不同步红、refresh入口/README快照红均保留在原009/008边界；技术脚本正确拒绝不等于所有父链已闭环。PGM036物理设备无本diff实现面，B/C业务参数原意不冲突，不作CLI特批。不改锁、不新增业务/添加/冲突/questions/HTML修改申请。未运行全仓predeploy/真push/PR/merge/部署。

## 逐块归因
| diff块 | ID归因 | 说明 |
|---|---|---|
| m001-regression版本行 | M008-001/002/003/004/006/007 | 同文件六项验证的自身版本递增 |
| m001-regression旧60行后基线args/env块 | M008-006 | 两脚本干净树非法参数/环境基线须拒绝 |
| m001-regression旧81行后4个段落样例 | M008-004 | 换行、同物理行两段、段后精确豁免与作用边界 |
| m001-regression旧89行后台账删除块 | M008-003 | 工作区删除，以及已提交删除不能被未跟踪副本遮盖 |
| m001-regression旧116行后空正文块 | M008-002 | 原有效CSV只清空正文必须失败，测试后还原输入 |
| m001-regression旧159行后两个命令链反例块 | M008-001 | 打印、相似名、整串打印、注释不能代表实际执行；保存/恢复测试输入归同ID |
| m001-regression旧206行后生成器资料与断言块 | M008-007 | 当前活跃句式/实际输出计数、历史句及无报告保持；所有测试资料均隔离 |
| refresh新增版本头及旧63行块 | M008-007 | 实际当前快照匹配替换旧取证句式，保留无报告陈述和其它活跃/历史路径 |
| delivery版本/helper及旧151/310行块 | M008-001 | 原两个调用点共同识别已有npm run &&链；声明当前支持语法边界；无新门禁项目 |
| anchors版本及旧111行块 | M008-002 | 空正文和不匹配都拒绝，一行条件变化 |
| ledger版本 | M008-003/006 | 两项同文件修复的自身版本递增 |
| ledger import/git helper/rev-parse/merge-base/status/diff | M008-006 | 所有基线和路径作为参数传递，不经过shell |
| ledger旧55行包含LEDGER条件 | M008-003 | 同一range排除删除，保留文件级同步 |
| param版本 | M008-004/006 | 两项同文件修复的自身版本递增 |
| param import/helper/rev-parse/merge-base/show/status/diff | M008-006 | 全部Git调用为argv，show保留原stdio行为 |
| param scan旧53行块及旧193行提示 | M008-004 | 等长mask索引匹配原文，按本仓块标签分段，段后ignore随前段；提示对应作用位置 |
所有+/-行属于上表，未新增无ID测试文件/依赖/业务内容；005常量数组全文与before相同，10份历史log及三HTML/资料/锁无diff。此表是B3解释，B4两席仍须独立核验，不提前当作PASS。
## 本B4完整无过滤diff
命令：git diff --binary 18894a9ee67577c6c8c3d19d99281f338a82b976 97b93d8f8e7ef71e8706dd428c4315499436b015
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

## 本B4新实跑：当前源一致性、语法与71回归
命令：逐路径 git show <after>:<path> 对当前bytes；node --check 六文件；node tools/gate-hardening-tests/m001-regression.mjs；git diff --cached --raw。
````text
CURRENT_EQUALS_AFTER tools/gate-hardening-tests/m001-regression.mjs 16438 d7479d0da66e8d599de40a1cc697fc581f90b9c605813bdfb3a87931f2f63b5c
SYNTAX tools/gate-hardening-tests/m001-regression.mjs 0 
CURRENT_EQUALS_AFTER tools/refresh-entry-doc-baseline.mjs 7354 2ba460a532e9571cf18cc270e77dfc7c0385c0b132e0c2351431f3a710b6f1d7
SYNTAX tools/refresh-entry-doc-baseline.mjs 0 
CURRENT_EQUALS_AFTER tools/verify-delivery-gates.mjs 16371 73bcf494770156441945c0ea2ff6afec30ae4489a48d7cc1c0a3225b9221a78f
SYNTAX tools/verify-delivery-gates.mjs 0 
CURRENT_EQUALS_AFTER tools/verify-entry-anchors.mjs 16186 7e62c6a19e36be5c13b166fd40aa4cb75e6ad057dfd6e97090323cae17853b82
SYNTAX tools/verify-entry-anchors.mjs 0 
CURRENT_EQUALS_AFTER tools/verify-html-ledger-sync.mjs 3894 1c9f974f4c49c3886469e6b86236675393317b7b0379bbbd1d354599f911214d
SYNTAX tools/verify-html-ledger-sync.mjs 0 
CURRENT_EQUALS_AFTER tools/verify-html-param-tier.mjs 11193 c79cdb39969b123264e917aa2921e4442e2ac1679f5f78ae6fbc0c70a5e71be9
SYNTAX tools/verify-html-param-tier.mjs 0 
B4_FRESH_REGRESSION_EXIT 0 PASS_LINES 71
PASS M008-006 参数基线 tools/verify-html-ledger-sync.mjs
PASS M008-006 环境基线 tools/verify-html-ledger-sync.mjs
PASS M008-006 参数基线 tools/verify-html-param-tier.mjs
PASS M008-006 环境基线 tools/verify-html-param-tier.mjs
PASS 未提交错误 tools/verify-html-ledger-sync.mjs
PASS 未提交错误 tools/verify-html-param-tier.mjs
PASS PR 错误 tools/verify-html-ledger-sync.mjs
PASS PR 错误 tools/verify-html-param-tier.mjs
PASS push 多提交 tools/verify-html-ledger-sync.mjs
PASS 明确基线 tools/verify-html-ledger-sync.mjs
PASS 缺失基线 tools/verify-html-ledger-sync.mjs
PASS push 多提交 tools/verify-html-param-tier.mjs
PASS 明确基线 tools/verify-html-param-tier.mjs
PASS 缺失基线 tools/verify-html-param-tier.mjs
PASS 空豁免
PASS 部分豁免
PASS 明确豁免
PASS 否定词
PASS 法定层级
PASS 业务层级
PASS M008-004 换行标注
PASS M008-004 同行两段
PASS M008-004 段后豁免
PASS M008-004 豁免不扩到后段
PASS 台账已同步
PASS M008-003 删除台账
PASS M008-003 已提交删除不被未跟踪副本掩盖
PASS 基线新增 key
PASS 基线保持不变
PASS 部署主线比较 ops/160/complex-deploy-api.sh tools/verify-html-ledger-sync.mjs
PASS 部署主线比较 ops/160/complex-deploy-api.sh tools/verify-html-param-tier.mjs
PASS 部署主线比较 ops/160/complex-deploy-web.sh tools/verify-html-ledger-sync.mjs
PASS 部署主线比较 ops/160/complex-deploy-web.sh tools/verify-html-param-tier.mjs
PASS CSV 引号逗号及跨行字段
PASS M008-002 空条目正文
PASS CSV 列数错误 docs/prd-audit/entry-registry.csv
PASS CSV 未闭合引号 docs/prd-audit/entry-registry.csv
PASS CSV BOM/CRLF docs/prd-audit/entry-registry.csv
PASS CSV 列数错误 docs/prd-audit/authority-rule-ledger.csv
PASS CSV 未闭合引号 docs/prd-audit/authority-rule-ledger.csv
PASS CSV BOM/CRLF docs/prd-audit/authority-rule-ledger.csv
PASS CSV 列数错误 docs/prd-audit/html-prd-coverage.csv
PASS CSV 未闭合引号 docs/prd-audit/html-prd-coverage.csv
PASS CSV BOM/CRLF docs/prd-audit/html-prd-coverage.csv
PASS 一致的 CI 定义
PASS CI 镜像分叉
PASS 两份 CI 都缺完整历史
PASS CI 删除事件基线
PASS CI 注释不能代替完整历史
PASS CI 未执行预部署检查
PASS 部署缺少 verify:html-ledger-sync ops/160/complex-deploy-api.sh
PASS 部署缺少 verify:entry-anchors ops/160/complex-deploy-api.sh
PASS 部署缺少 verify:html-param-tier ops/160/complex-deploy-api.sh
PASS 部署缺少 verify:html-ledger-sync ops/160/complex-deploy-web.sh
PASS 部署缺少 verify:entry-anchors ops/160/complex-deploy-web.sh
PASS 部署缺少 verify:html-param-tier ops/160/complex-deploy-web.sh
PASS M008-001 verify:predeploy 打印
PASS M008-001 verify:predeploy 相似名
PASS M008-001 verify:predeploy 整串打印
PASS M008-001 verify:predeploy 注释
PASS M008-001 verify:delivery-gates 打印
PASS M008-001 verify:delivery-gates 相似名
PASS M008-001 verify:delivery-gates 整串打印
PASS M008-001 verify:delivery-gates 注释
PASS 检查入口缺少 HTML 门禁
PASS 合并提交 stable
PASS 合并提交 watch
PASS 合并提交 late
PASS M008-007 回填快照
PASS M008-007 回填后校验
PASS M008-007 无报告仍回填计数
M001_GOVERNANCE_REGRESSION_PASS

CHECK_ACTUAL_LOCK_SHA b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
CACHED_DIFF ''
````
## 独立命令边界反例完整命令
````python
from pathlib import Path
import subprocess,tempfile,re,json,os
r=Path('/home/james/complex-management')
source=(r/'tools/verify-delivery-gates.mjs').read_text()
deps=set(re.findall(r'(?:readText|readJson|requireIncludes)\("([^"]+)"',source))
deps.update(['.github/workflows/predeploy-gates.yml','ops/ci/predeploy-gates.yml','ops/160/complex-deploy-api.sh','ops/160/complex-deploy-web.sh'])
with tempfile.TemporaryDirectory(prefix='m008-b4-shell-',dir=r/'.scratch') as td:
 t=Path(td)
 for rel in deps:
  p=t/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((r/rel).read_bytes())
 pkg=json.loads((t/'package.json').read_text());original=dict(pkg['scripts'])
 bindir=t/'bin';bindir.mkdir()
 (bindir/'npm').write_text('#!/bin/sh\nprintf \'NPM:%s\\n\' "$*"\n',encoding='utf-8');(bindir/'npm').chmod(0o755)
 (bindir/'run').write_text('#!/bin/sh\nprintf \'RUN_NOT_NPM:%s\\n\' "$*"\nexit 127\n',encoding='utf-8');(bindir/'run').chmod(0o755)
 env={**os.environ,'PATH':str(bindir)+':/usr/bin:/bin'}
 for key,gate in [('verify:delivery-gates','verify:unit'),('verify:predeploy','verify:html-param-tier')]:
  for label,chain in [('canonical',original[key]),('valid-wrap-after-and',original[key].replace(' && ',' &&\n')),('LF-between-npm-and-run',original[key].replace('npm run '+gate,'npm\nrun '+gate)),('LF-before-and',original[key].replace(' && ','\n&& ',1))]:
   pkg['scripts']={**original,key:chain};(t/'package.json').write_text(json.dumps(pkg),encoding='utf-8')
   checked=subprocess.run(['node',str(r/'tools/verify-delivery-gates.mjs')],cwd=t,text=True,capture_output=True)
   actual=subprocess.run(['/bin/sh','-c',chain],cwd=t,env=env,text=True,capture_output=True)
   ran=('NPM:run '+gate) in actual.stdout.splitlines()
   print('B4_SHELL_CASE',key,label,'GATE_EXIT',checked.returncode,'SHELL_EXIT',actual.returncode,'REQUIRED_NPM_INVOKED',ran)
   print('GATE_OUTPUT',repr(checked.stdout+checked.stderr))
   if label=='LF-between-npm-and-run':print('SHELL_BOUNDARY_OUTPUT',[l for l in actual.stdout.splitlines() if l=='NPM:' or l.startswith('RUN_NOT_NPM:')])
print('All writes isolated; canonical sources unchanged.')

````
实际输出：
````text
B4_SHELL_CASE verify:delivery-gates canonical GATE_EXIT 0 SHELL_EXIT 0 REQUIRED_NPM_INVOKED True
GATE_OUTPUT 'DELIVERY_GATES_DOCS_PASS\n'
B4_SHELL_CASE verify:delivery-gates valid-wrap-after-and GATE_EXIT 0 SHELL_EXIT 0 REQUIRED_NPM_INVOKED True
GATE_OUTPUT 'DELIVERY_GATES_DOCS_PASS\n'
B4_SHELL_CASE verify:delivery-gates LF-between-npm-and-run GATE_EXIT 0 SHELL_EXIT 127 REQUIRED_NPM_INVOKED False
GATE_OUTPUT 'DELIVERY_GATES_DOCS_PASS\n'
SHELL_BOUNDARY_OUTPUT ['NPM:', 'RUN_NOT_NPM:verify:unit']
B4_SHELL_CASE verify:delivery-gates LF-before-and GATE_EXIT 0 SHELL_EXIT 2 REQUIRED_NPM_INVOKED False
GATE_OUTPUT 'DELIVERY_GATES_DOCS_PASS\n'
B4_SHELL_CASE verify:predeploy canonical GATE_EXIT 0 SHELL_EXIT 0 REQUIRED_NPM_INVOKED True
GATE_OUTPUT 'DELIVERY_GATES_DOCS_PASS\n'
B4_SHELL_CASE verify:predeploy valid-wrap-after-and GATE_EXIT 0 SHELL_EXIT 0 REQUIRED_NPM_INVOKED True
GATE_OUTPUT 'DELIVERY_GATES_DOCS_PASS\n'
B4_SHELL_CASE verify:predeploy LF-between-npm-and-run GATE_EXIT 0 SHELL_EXIT 127 REQUIRED_NPM_INVOKED False
GATE_OUTPUT 'DELIVERY_GATES_DOCS_PASS\n'
SHELL_BOUNDARY_OUTPUT ['NPM:', 'RUN_NOT_NPM:verify:html-param-tier']
B4_SHELL_CASE verify:predeploy LF-before-and GATE_EXIT 0 SHELL_EXIT 2 REQUIRED_NPM_INVOKED False
GATE_OUTPUT 'DELIVERY_GATES_DOCS_PASS\n'
All writes isolated; canonical sources unchanged.
````
## 本B4当前实际入口及保护文件检查
命令：node tools/verify-delivery-gates.mjs；node tools/verify-html-param-tier.mjs；node tools/verify-entry-anchors.mjs；node tools/verify-html-ledger-sync.mjs；node tools/refresh-entry-doc-baseline.mjs --check；005数组与before逐字对照、保护文件与before字节对照。
````text
CURRENT tools/verify-delivery-gates.mjs EXIT 0
DELIVERY_GATES_DOCS_PASS

CURRENT tools/verify-html-param-tier.mjs EXIT 0
[html-param-tier] PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句（存量 0 处已登记放行）。比对范围：工作区（含未提交改动）↔ 基线 origin/main@242f4872；扫描口径：改动文件全文，已剥除 <style> 与注释。

CURRENT tools/verify-entry-anchors.mjs EXIT 1
[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1565 条；被检 Markdown 360 份；条目名引用 1517 处（注册表原名 1358 处，正文唯一定位 159 处）。
[entry-anchors] FAIL：6 处不合格。
  - 注册表条目正文在 HTML 现文中找不到：BSM-E0433 BSM「处理后回访客人或商户，验收合格才关闭」——HTML 已改而注册表未重建
  - 注册表条目正文在 HTML 现文中找不到：BSM-E0736 BSM「文档版本」——HTML 已改而注册表未重建
  - 账本正文与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」——HTML 已改而账本 source_text 未重取
  - 账本正文与 HTML 现文对不上：BSM-0530「文档版本」——HTML 已改而账本 source_text 未重取
  - 账本镜像列与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」preconditions 片段「处理后回访客人或商户，验收合格才关闭。」——HTML 已改而账本未重取（检查⑦）
  - 账本镜像列与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」state_transition 片段「处理后回访客人或商户，验收合格才关闭。」——HTML 已改而账本未重取（检查⑦）
  处置：条目改名或 HTML 改动后，重建 docs/prd-audit/entry-registry.csv 并把引用改为现行条目名；一律不得写回行号。

CURRENT tools/verify-html-ledger-sync.mjs EXIT 1
[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@242f4872。
  已改动的 HTML：
    - docs/business-system-map-for-operations-v0.1.html
  必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md
  依据：用户 2026-08-03 拍板——台账保持完整，改 HTML 必须同 PR 同步台账。
  若本次确为纯排版/错别字、不涉及口径，请在台账内追加一行说明后再提交。

CURRENT tools/refresh-entry-doc-baseline.mjs EXIT 1
[entry-doc-baseline] DRIFT: docs/00-current-effective-document-entry-v0.1.md + README.md 快照落后于实际（应为 720 ops / V399（377 迁移） / 55 tests）。运行 node tools/refresh-entry-doc-baseline.mjs 回填。

M008_005_UNTOUCHED 50 44
PROTECTED_UNCHANGED docs/business-system-map-for-operations-v0.1.html a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa
PROTECTED_UNCHANGED docs/platform-governance-map-for-operations-v0.1.html c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c
PROTECTED_UNCHANGED docs/business-enum-quick-reference-v0.1.html 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66
PROTECTED_UNCHANGED tools/html-param-tier-baseline.json 9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8
PROTECTED_UNCHANGED README.md 28a46aea13227b0e3e101f9dc0a2970b6e3e581ca5995d291288aa59cb2d23b5
PROTECTED_UNCHANGED docs/00-current-effective-document-entry-v0.1.md 34e9c9f76578b61eb659dc4e369b274f62adb110b5f4dd3895bc0bb08f83e314
````
## 本B4真实资料隔离生成器/全三HTML扫描复验
下面命令所有写模式只对.scratch临时副本，结束清理；不改正本。
````python
from pathlib import Path
import subprocess,tempfile,json,re,hashlib
r=Path('/home/james/complex-management');entry='docs/00-current-effective-document-entry-v0.1.md'
lines=(r/entry).read_text(encoding='utf-8').splitlines()
print('ACTUAL_ENTRY_CONTEXT\n'+'\n'.join(str(i)+': '+l for i,l in enumerate(lines,1) if 119<=i<=132))
with tempfile.TemporaryDirectory(prefix='m008-refresh-current-',dir=r/'.scratch') as td:
 t=Path(td)
 for rel in [entry,'README.md','docs/openapi/openapi-p0.json']:
  p=t/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((r/rel).read_bytes())
 md='backend/src/main/resources/db/migration';(t/md).mkdir(parents=True)
 for p in (r/md).glob('*.sql'):
  if re.match(r'^V\d+__.*\.sql$',p.name):(t/md/p.name).touch()
 sf='backend/target/surefire-reports';counts=[]
 if (r/sf).exists():
  (t/sf).mkdir(parents=True)
  for p in (r/sf).glob('*.xml'):
   m=re.search(r'<testsuite[^>]*\btests="(\d+)"',p.read_text(encoding='utf-8')[:500])
   if m:
    counts.append(int(m.group(1)));(t/sf/p.name).write_text('<testsuite tests="'+m.group(1)+'"/>',encoding='utf-8')
 print('LIVE_SUREFIRE_TEST_SUM',sum(counts))
 for args in [['--check'],[],['--check']]:
  p=subprocess.run(['node',str(r/'tools/refresh-entry-doc-baseline.mjs'),*args],cwd=t,text=True,capture_output=True)
  print('ISOLATED_CURRENT_REFRESH',args,'EXIT',p.returncode);print(p.stdout);print(p.stderr)
 print('AFTER_LINE126',(t/entry).read_text(encoding='utf-8').splitlines()[125])
 print('AFTER_README_TEST',next(l for l in (t/'README.md').read_text().splitlines() if '| 后端测试 |' in l))
print('BASELINE_AFTER',hashlib.sha256((r/'tools/html-param-tier-baseline.json').read_bytes()).hexdigest())

import tempfile,subprocess,json,os
with tempfile.TemporaryDirectory(prefix='m008-final-scan-',dir=r/'.scratch') as td:
 q=Path(td)
 for rel in ['docs/business-system-map-for-operations-v0.1.html','docs/platform-governance-map-for-operations-v0.1.html','docs/business-enum-quick-reference-v0.1.html','tools/html-param-tier-baseline.json']:
  p=q/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((r/rel).read_bytes())
 subprocess.run(['git','init','-q',str(q)],check=True)
 z=subprocess.run(['node',str(r/'tools/verify-html-param-tier.mjs'),'--write-baseline'],cwd=q,text=True,capture_output=True)
 entries=json.loads((q/'tools/html-param-tier-baseline.json').read_text())['entries']
 print('ISOLATED_REAL_THREE_HTML_SCAN',z.returncode,'entries',len(entries));print(z.stdout+z.stderr)
 assert z.returncode==0 and len(entries)==0

````
实际输出：
````text
ACTUAL_ENTRY_CONTEXT
119: | 一体机 | 统一终端码 `kiosk`；全体一体机统一具备权益展示、订台、吧台买酒、游乐状态和定期检修记录五项基础能力，通过叠加式售票能力开关 `ticketing_enabled` 区分是否允许授权服务人员辅助到店客人购票 |
120: | 启用售票能力的一体机 | 承担授权服务人员辅助到店购票、订单查询职责（**门票为动态二维码，不提供取票功能**）；绑定酒吧门口/前台时只能售卖本酒吧单店门票，不得售卖综合体通票（依据 BSM「启用售票能力的一体机」条；DEC-R7-05） |
121: | 卡座提成 | 一张账单的销售提成只计一次、只归帮该客人预订的人；自来客不产生销售提成；协助服务、加单、送达等其他经手人只留责任痕迹、不产生销售提成；服务员提成跟服务责任链和台位会话走：转台后记到新台会话，销售提成仍按原归属不变；并台时各服务员既有服务/提成记录分别保留，不把他人提成合并到一人，也不重算历史提成；存在预订归属时，按员工档案是否设有「销售岗」标识判定销售提成费率；费率可配（业务参数，按酒吧可配，出厂默认销售 15%、非销售 10%，须同时配置上下限），按可计提消费额计；不计销售提成：通票及票务权益整类（不论其中权益收费或免费、由谁出资）、卡座入场名额、赠送成本、退款、免单 |
122: | 卡座续卡/续时 | 平台不提供续卡、续时类产品。拆单、合单、转台、并台、拆台和撤销转台按业务 HTML 留痕，不作为续卡或续时产品 |
123: 
124: ### 2.1 R1~R4 审计交付物登记（2026-06-18 附加）
125: 
126: 本节附加式登记 R1~R4 阶段审计与回填交付物，以及配套的数据隔离规格与 SMS 占位规格，便于审计入口统一追溯。本节为附加内容，不删除上文任一原有登记；当前事实基线以 `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `npm run verify:*` 实时输出为准。本轮 2026-09-01 实测 OpenAPI 720 operations、Flyway 最新 V385；当前工作树无 `backend/target/surefire-reports`，故不声明当前测试数量。
127: 
128: | 交付物 | 入口 |
129: |---|---|
130: | R1 需求文档审计报告 | `docs/r1-requirements-documentation-audit-report-20260618.md` |
131: | R1 文档地图 | `docs/r1-documentation-map-20260618.md` |
132: | R1 需求-API-页面-表-测试 映射矩阵 | `docs/r1-requirement-api-page-table-test-mapping-matrix-20260618.md` |
LIVE_SUREFIRE_TEST_SUM 55
ISOLATED_CURRENT_REFRESH ['--check'] EXIT 1

[entry-doc-baseline] DRIFT: docs/00-current-effective-document-entry-v0.1.md + README.md 快照落后于实际（应为 720 ops / V399（377 迁移） / 55 tests）。运行 node tools/refresh-entry-doc-baseline.mjs 回填。

ISOLATED_CURRENT_REFRESH [] EXIT 0
[entry-doc-baseline] 已回填 docs/00-current-effective-document-entry-v0.1.md + README.md：720 ops / V399（377 迁移） / 55 tests（2026-09-09）。


ISOLATED_CURRENT_REFRESH ['--check'] EXIT 0
[entry-doc-baseline] PASS: 快照与实际一致（720 ops / V399（377 迁移） / 55 tests）。


AFTER_LINE126 本节附加式登记 R1~R4 阶段审计与回填交付物，以及配套的数据隔离规格与 SMS 占位规格，便于审计入口统一追溯。本节为附加内容，不删除上文任一原有登记；当前事实基线以 `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `npm run verify:*` 实时输出为准。本轮 2026-09-09 实测 OpenAPI 720 operations、Flyway 最新 V399；后端 surefire 现有报告 55 tests。
AFTER_README_TEST | 后端测试 | **55** tests（现有 surefire 报告） | `backend/target/surefire-reports` 合计；无报告时保留上次快照，不声明刚完成全量测试 |
BASELINE_AFTER 9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8
ISOLATED_REAL_THREE_HTML_SCAN 0 entries 0
[html-param-tier] 已重建基线 tools/html-param-tier-baseline.json：0 条。请人工核对条数只减不增。

````
## B3先红后绿证据（历史阶段，非冒称本B4新跑）
B3先只写m001-regression V0.06，在before五个源文件上执行该测试exit1，以下为当时失败摘要；完整测试命令可用before源和after同一测试在隔离仓复现。随后五源修完同一回归exit0/71PASS，本B4再次实跑已独立确认。
````text
TDD_BEFORE_FIX_EXIT 1
FAIL M008-006 参数基线 tools/verify-html-ledger-sync.mjs: [html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。比对范围：提交区 HEAD;echo M008_BASE_ARG...HEAD（工作区三份 HTML 无未提交改动）。
FAIL M008-006 环境基线 tools/verify-html-ledger-sync.mjs: [html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。比对范围：提交区 HEAD;echo M008_BASE_ARG...HEAD（工作区三份 HTML 无未提交改动）。
FAIL M008-006 参数基线 tools/verify-html-param-tier.mjs: [html-param-tier] PASS: 本次未改动业务最终解释权 HTML。比对范围：提交区 HEAD;echo M008_BASE_ARG...HEAD（工作区三份 HTML 无未提交改动）。
FAIL M008-006 环境基线 tools/verify-html-param-tier.mjs: [html-param-tier] PASS: 本次未改动业务最终解释权 HTML。比对范围：提交区 HEAD;echo M008_BASE_ARG...HEAD（工作区三份 HTML 无未提交改动）。
FAIL M008-004 换行标注: [html-param-tier] FAIL: 1 处带具体数值的语句未标出所属层级，且不在存量基线内。
FAIL M008-004 同行两段: [html-param-tier] PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句（存量 0 处已登记放行）。比对范围：工作区（含未提交改动）↔ 基线 966ca57a678e9fabab9f48619f9d40c7725193c6@966ca57a；扫描口径：改动文件全文，已剥除 <style> 与注释。
FAIL M008-003 删除台账: [html-ledger-sync] PASS: 改动了 1 份业务 HTML，决策台账已同步。比对范围：工作区（含未提交改动）↔ 基线 966ca57a678e9fabab9f48619f9d40c7725193c6@966ca57a。
FAIL M008-003 已提交删除不被未跟踪副本掩盖: [html-ledger-sync] PASS: 改动了 1 份业务 HTML，决策台账已同步。比对范围：提交区 966ca57a678e9fabab9f48619f9d40c7725193c6...HEAD（工作区三份 HTML 无未提交改动）。
FAIL M008-002 空条目正文: [entry-anchors] 注册表 1 行；账本 1 行；承接矩阵 1 行（同名 1、账本中无此 rule_id 0）；账本镜像列片段 0 条；被检 Markdown 0 份；条目名引用 0 处（注册表原名 0 处，正文唯一定位 0 处）。
FAIL M008-001 verify:predeploy 打印: DELIVERY_GATES_DOCS_PASS
FAIL M008-001 verify:predeploy 相似名: DELIVERY_GATES_DOCS_PASS
FAIL M008-001 verify:predeploy 整串打印: DELIVERY_GATES_DOCS_PASS
FAIL M008-001 verify:predeploy 注释: DELIVERY_GATES_DOCS_PASS
FAIL M008-001 verify:delivery-gates 打印: DELIVERY_GATES_DOCS_PASS
FAIL M008-001 verify:delivery-gates 相似名: DELIVERY_GATES_DOCS_PASS
FAIL M008-001 verify:delivery-gates 整串打印: DELIVERY_GATES_DOCS_PASS
FAIL M008-001 verify:delivery-gates 注释: DELIVERY_GATES_DOCS_PASS
AssertionError [ERR_ASSERTION]: The input did not match the regular expression /实测 OpenAPI 2 operations、Flyway 最新 V12；后端 surefire 现有报告 3 tests。/. Input:
````
最终本席B4-r1：修复内容FAIL（原M008-001仍在队列），范围归因PASS，总FAIL。至此完整diff已审完，本次全部问题已落盘；下一步才读Grok完整报告，按合取整批路由，不在此先修或宣称模块通过。
