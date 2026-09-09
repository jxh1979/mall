CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M008 B4-r2 Codex 本方完整修复差异审计
文件自身版本：V0.01

修复内容：PASS。范围归因：PASS。本方B4-r2总票：PASS。此时未读取Grok本次报告，须另一席完整报告也PASS才可进入B5；这不是模块PASS。

## 1. 范围、独立性及定稿输入
正本/home/james/complex-management，main HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c。controller V0.17，模块M008，B3累计2，ledger V1.97，findings V1.14（418170bytes SHA25ab53318c38ddc44e7ea670b8e1c3f31f821a8a05ce64fe7cc0cdfc2e2469a9）是唯一正式队列。本轮修复按B2-r3双PASS定稿：001–004/006–007是，005/008–016否。007已明确包括当前126与160，008只登记正常有效生成与正本资料漂移。
本方先派Grok B4-r2，等待真实exit0与指定路径回执后才开始本轮独立全diff审计，fresh重读3文件全文共737行（278+333+126），3文件工作区均等于after；重取完整diff、复核快照纠错、实跑3语法检查及77回归、独立before/after原生shell边界与真实资料生成器探针。本报告完整落盘前未打开Grok B4-r2 raw、未读取其原始JSON或片段。B3验证与B4新鲜验证分开陈述。
沿用已实际读过的requesting-code-review、receiving-code-review、verification-before-completion、systematic-debugging/diagnosing-bugs、TDD、grilling、writing-for-agents及ponytail的适用意图；本轮请审实际只调用固定Grok。没有声称重读或调用本轮未用工具，没有第三席/MCP/新判据。B4只审完整diff，不冒称B5重审17文件/19条。
冻结依据为GEN-001–018与PGM-036；本轮001依据GEN-002/004/008/009/013，007依据GEN-002/009/016，验证依据GEN-003/018。两项为命令检查/工程计数生成，未增业务实体、价格、枚举、权限审批流程，无业务发明；三HTML未改。

## 2. 可复核完整差异与快照纠错
正式before：bc3c7e12d74903e55f4e8063960d2de312fc6d19
正式after：07741199b927955fefef9a03510ddcc32f4608c8
命令：git diff --binary bc3c7e12d74903e55f4e8063960d2de312fc6d19 07741199b927955fefef9a03510ddcc32f4608c8
完整diff：7174bytes，SHA256 420c0652369c26e3dd05e8971765936cb0d5f3252bccae1dc5ef3d27c455342d；3文件18插入/10删除，7个hunk。没有路径过滤，含tracked/staged/untracked全量快照。
修复封存时发生两次调度错误：四值提取正则曾双转义导致IndexError；纠正后发现.scratch临时索引自身index.lock入树，拒收该次快照。旧before178b5b99f91f6a83c36e29cbaefcfa88b2865392不作为本轮B4基线。之后在同一B3批次内，将这3份已测字节保存在进程内，只暂时恢复这3文件的原before字节，以系统临时目录尚不存在index路径实跑git add-A/write-tree；finally恢复已测字节，再实跑取after。两次正式快照间没有报告/台账写入。此前B3次数尚未加1，取得正式after后才记2。
本方B4新鲜复核命令git diff --name-status 178b5b99f91f6a83c36e29cbaefcfa88b2865392 bc3c7e12d74903e55f4e8063960d2de312fc6d19输出唯一：
D\t.scratch/m008-r2-index-nmttkkcs/index.lock
所以重取before仅移除调度器自己错误纳入的临时锁，所有真实文件字节与初次before完全相同。没有人工过滤实际修复diff、没有删除用户资料/主暂存区操作，已测3份源码最终字节逐一相同。主索引eeb0e20f0c85989b6b902fba051c6b7b2f3051b870cacabd0c2ab6e2211b42aa未变，cached diff空。B3四值前后对应精确M008-b4-r1-grok-raw均同冻结SHA，不使用新B1追加替代。

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

## 3. 全部增删行逐块归因
| 文件/行块 | 所有增删行的用途 | 正式ID |
|---|---|---|
| regression头1，V0.06→V0.07 | 承载本批行为验证，自身版本递增 | 001、007 |
| regression 184–201，内层数组解构增加expected默认1、2个非法LF与1个合法续行变体、断言读取expected | 两真实入口各核命令内LF/&&前LF应失败、&&后LF应通过；旧8组仍默认1，没有改变旧反例含义 | 001 |
| regression 250–253，现有入口fixture增加取证为当前快照 | 对应真实160，不新增测试文件/框架 | 007 |
| regression 260–270，新增取证为回填断言、replace→replaceAll、无报告再次断言 | 同时扰动两句ops，要求都恢复；3 tests旧值应保留；原126与历史断言仍在 | 007 |
| refresh头1，V0.01→V0.02 | 自身版本递增 | 007 |
| refresh69–71，新增现行取证为句式匹配与回填 | 只匹配当前句的migration/ops/报告陈述，无报告时oldStatus保留，其它既有正则不改 | 007 |
| delivery头1，V0.03→V0.04 | 自身版本递增 | 001 |
| delivery8–10三行替换 | 首尾只去shell允许的空格/tab/LF；&&前仅空格/tab，后允许LF；词间仅行内空白；拒绝残留LF以避开JS美元终锚匹配终端换行；按同一行内分隔抽第三字段 | 001 |

全部18插入/10删除均有正式ID。002–004/006已正确的3文件与r1 after完全相同，不因仍在整批队列就无意义重改；其对应现有回归本轮再次通过。005常量数组50项/44唯一与r1 before全文相同。没有源码以外路径diff，三HTML/README/入口/基线/历史log/锁未改。范围归因PASS。

## 4. 修复正确性与边界
001：两个调用点delivery161与predeploy321共用helper，修根因，不在各入口重复防护。旧宽泛空白会把LF当词分隔，也会吞掉&&前不合法换行；新helper限制行内空白并显式拒绝命令片段残留LF。字符串整体首尾LF和&&后LF属于原生shell可执行排版，保留。B4独立12组native探针（两入口×6）比较before/after：正常、&&后LF、tab、首尾LF都两版gate0且原生shell目标npm到达；命令内LF/&&前LF仅before假绿，after为1且原生shell不执行目标npm。不引通用shell解析器，不新增必跑命令，不宣称支持本仓未用的其它shell语法。127是隔离run桩显式返回，2是原生shell语法错误；不冒称真实npm全链退出码。
007：新69–71匹配实际160取证为句，同原126的无报告/有报告陈述，闭合后再次运行仍可匹配。B4独立复制真实资料，先跑before默认写+check，160仍V385/无报告但check0；在同一副本上跑after --check立即exit1且只指入口（README已由before回填）；after写后126/160均V399/55tests，check0。现有最小回归也验证两句无新报告仍更新ops且保留3tests、历史句不改。未回填M435/M436正本。
002/003/004/006：完整B4-r1已经逐块内容验证；本轮相关源无变更，fresh77回归中的空正文、删除台账、逻辑块/豁免/棘轮、argv基线等全部通过，保持既有修复。不得把本轮审计缩成只看001一项。
其它观察均为既有ID：005重复常量保持；008真实正本快照还旧；009真实anchors六处输入不一致及ledger未同步红正确拒绝；010历史log不改；01177PASS不冒称覆盖全仓；012既有全3HTML副本扫描证据保留，本轮仅实际变更BSM的只读门禁重新通过；014/015/016 CLI/UTC/PGM边界不变。没有本次新增无ID finding/观察、没有待询添加或HTML本身问题。
修复内容PASS。上述结论在当前全部diff与所需输入上成立，不是全链绿，不替代后续B5。

## 5. 本轮新鲜命令与完整输出
### 5.1 语法及现有回归
在主仓运行node --check三个修改文件及node tools/gate-hardening-tests/m001-regression.mjs。回归自身写隔离临时目录，未执行真实PR/merge/部署。已完整读测试278行，断言真实调用目标脚本，不是只镜像实现。Node实际v24.20.0，未冒称Node22。
```text
SYNTAX tools/gate-hardening-tests/m001-regression.mjs 0 
SYNTAX tools/verify-delivery-gates.mjs 0 
SYNTAX tools/refresh-entry-doc-baseline.mjs 0 
FRESH_B4_R2_REGRESSION 0 PASS_COUNT 77
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
PASS M008-001 verify:predeploy 命令内换行
PASS M008-001 verify:predeploy && 前换行
PASS M008-001 verify:predeploy && 后续行
PASS M008-001 verify:delivery-gates 打印
PASS M008-001 verify:delivery-gates 相似名
PASS M008-001 verify:delivery-gates 整串打印
PASS M008-001 verify:delivery-gates 注释
PASS M008-001 verify:delivery-gates 命令内换行
PASS M008-001 verify:delivery-gates && 前换行
PASS M008-001 verify:delivery-gates && 后续行
PASS 检查入口缺少 HTML 门禁
PASS 合并提交 stable
PASS 合并提交 watch
PASS 合并提交 late
PASS M008-007 回填快照
PASS M008-007 回填后校验
PASS M008-007 无报告仍回填计数
M001_GOVERNANCE_REGRESSION_PASS


```
B3的先红后绿历史：只更新测试时001四个非法LF组exit0造成失败、007取证为断言失败；修源后同测试77PASS。该历史不替代下列B4重新取before字节构造的独立对照。

### 5.2 两入口原生shell边界，对比before与after
```python
from pathlib import Path
import subprocess,tempfile,re,json,os
r=Path('/home/james/complex-management');rel='tools/verify-delivery-gates.mjs'
source=(r/rel).read_text()
deps=set(re.findall(r'(?:readText|readJson|requireIncludes)\("([^"]+)"',source))
deps.update(['.github/workflows/predeploy-gates.yml','ops/ci/predeploy-gates.yml','ops/160/complex-deploy-api.sh','ops/160/complex-deploy-web.sh'])
with tempfile.TemporaryDirectory(prefix='m008-b4-r2-native-',dir=r/'.scratch') as td:
 t=Path(td);old=t/'before.mjs';old.write_bytes(subprocess.check_output(['git','show','bc3c7e12d74903e55f4e8063960d2de312fc6d19:'+rel],cwd=r))
 for f in deps:
  p=t/f;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((r/f).read_bytes())
 pkg=json.loads((t/'package.json').read_text());original=dict(pkg['scripts']);bindir=t/'bin';bindir.mkdir()
 for name,body in [('npm','#!/bin/sh\nprintf \'NPM:%s\\n\' "$*"\n'),('run','#!/bin/sh\nprintf \'RUN_NOT_NPM:%s\\n\' "$*"\nexit 127\n')]:
  p=bindir/name;p.write_text(body);p.chmod(0o755)
 env={**os.environ,'PATH':str(bindir)+':/usr/bin:/bin'}
 for key,gate in [('verify:delivery-gates','verify:unit'),('verify:predeploy','verify:html-param-tier')]:
  for label,chain in [('canonical',original[key]),('valid-after-and',original[key].replace(' && ',' &&\n')),('LF-in-command',original[key].replace('npm run '+gate,'npm\nrun '+gate)),('LF-before-and',original[key].replace(' && ','\n&& ',1)),('tabs',original[key].replace('npm run ','npm\trun\t')),('outer-LF','\n'+original[key]+'\n')]:
   pkg['scripts']={**original,key:chain};(t/'package.json').write_text(json.dumps(pkg))
   codes=[]
   for f in [old,r/rel]:
    checked=subprocess.run(['node',str(f)],cwd=t,text=True,capture_output=True);codes.append(checked.returncode)
   actual=subprocess.run(['/bin/sh','-c',chain],cwd=t,env=env,text=True,capture_output=True);ran=('NPM:run '+gate) in actual.stdout.splitlines()
   valid=label not in ('LF-in-command','LF-before-and')
   print('FRESH_NATIVE',key,label,'BEFORE',codes[0],'AFTER',codes[1],'SHELL',actual.returncode,'NPM_TARGET',ran)
   assert codes==[0,0 if valid else 1] and ran==valid
print('Only boundary stubs ran; run stub explicitly exits127; no real production npm chain executed.')

```
```text
FRESH_NATIVE verify:delivery-gates canonical BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_NATIVE verify:delivery-gates valid-after-and BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_NATIVE verify:delivery-gates LF-in-command BEFORE 0 AFTER 1 SHELL 127 NPM_TARGET False
FRESH_NATIVE verify:delivery-gates LF-before-and BEFORE 0 AFTER 1 SHELL 2 NPM_TARGET False
FRESH_NATIVE verify:delivery-gates tabs BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_NATIVE verify:delivery-gates outer-LF BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_NATIVE verify:predeploy canonical BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_NATIVE verify:predeploy valid-after-and BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_NATIVE verify:predeploy LF-in-command BEFORE 0 AFTER 1 SHELL 127 NPM_TARGET False
FRESH_NATIVE verify:predeploy LF-before-and BEFORE 0 AFTER 1 SHELL 2 NPM_TARGET False
FRESH_NATIVE verify:predeploy tabs BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_NATIVE verify:predeploy outer-LF BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
Only boundary stubs ran; run stub explicitly exits127; no real production npm chain executed.

```

### 5.3 真实入口资料的隔离before/after生成
```python
from pathlib import Path
import subprocess,tempfile,json,re,hashlib
r=Path('/home/james/complex-management');entry='docs/00-current-effective-document-entry-v0.1.md'
lines=(r/entry).read_text(encoding='utf-8').splitlines()
print('ACTUAL_ENTRY_CONTEXT\n'+'\n'.join(str(i)+': '+l for i,l in enumerate(lines,1) if i in (126,160)))
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
 old=t/'before-refresh.mjs';old.write_bytes(subprocess.check_output(['git','show','bc3c7e12d74903e55f4e8063960d2de312fc6d19:tools/refresh-entry-doc-baseline.mjs'],cwd=r))
 for args in [[],['--check']]:
  q=subprocess.run(['node',str(old),*args],cwd=t,text=True,capture_output=True)
  print('FRESH_BEFORE_REFRESH',args,'EXIT',q.returncode,q.stdout+q.stderr)
 assert '最新 V385' in (t/entry).read_text().splitlines()[159]
 print('BEFORE_STILL_LINE160',(t/entry).read_text().splitlines()[159])
 for args in [['--check'],[],['--check']]:
  p=subprocess.run(['node',str(r/'tools/refresh-entry-doc-baseline.mjs'),*args],cwd=t,text=True,capture_output=True)
  print('ISOLATED_CURRENT_REFRESH',args,'EXIT',p.returncode);print(p.stdout);print(p.stderr)
 print('AFTER_LINE126',(t/entry).read_text(encoding='utf-8').splitlines()[125])
 print('AFTER_LINE160',(t/entry).read_text(encoding='utf-8').splitlines()[159])
 assert '最新 V399' in (t/entry).read_text().splitlines()[159] and '55 tests' in (t/entry).read_text().splitlines()[159]
 print('AFTER_README_TEST',next(l for l in (t/'README.md').read_text().splitlines() if '| 后端测试 |' in l))
print('BASELINE_AFTER',hashlib.sha256((r/'tools/html-param-tier-baseline.json').read_bytes()).hexdigest())

```
```text
ACTUAL_ENTRY_CONTEXT
126: 本节附加式登记 R1~R4 阶段审计与回填交付物，以及配套的数据隔离规格与 SMS 占位规格，便于审计入口统一追溯。本节为附加内容，不删除上文任一原有登记；当前事实基线以 `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `npm run verify:*` 实时输出为准。本轮 2026-09-01 实测 OpenAPI 720 operations、Flyway 最新 V385；当前工作树无 `backend/target/surefire-reports`，故不声明当前测试数量。
160: 本节附加式登记业务决策批次与本会话已落地新功能，不删除上文任一原有内容。本轮 2026-09-01 取证为 Flyway 最新 V385、OpenAPI 720 operations；当前工作树无后端 surefire 报告，不声明当前测试数量。各项以 `docs/openapi/openapi-p0.yaml`、`backend/src/main/resources/db/migration`、`backend/target/surefire-reports`、`npx playwright test --list` 与 `npm run verify:*` 实时输出为最终权威。
LIVE_SUREFIRE_TEST_SUM 55
FRESH_BEFORE_REFRESH [] EXIT 0 [entry-doc-baseline] 已回填 docs/00-current-effective-document-entry-v0.1.md + README.md：720 ops / V399（377 迁移） / 55 tests（2026-09-09）。

FRESH_BEFORE_REFRESH ['--check'] EXIT 0 [entry-doc-baseline] PASS: 快照与实际一致（720 ops / V399（377 迁移） / 55 tests）。

BEFORE_STILL_LINE160 本节附加式登记业务决策批次与本会话已落地新功能，不删除上文任一原有内容。本轮 2026-09-01 取证为 Flyway 最新 V385、OpenAPI 720 operations；当前工作树无后端 surefire 报告，不声明当前测试数量。各项以 `docs/openapi/openapi-p0.yaml`、`backend/src/main/resources/db/migration`、`backend/target/surefire-reports`、`npx playwright test --list` 与 `npm run verify:*` 实时输出为最终权威。
ISOLATED_CURRENT_REFRESH ['--check'] EXIT 1

[entry-doc-baseline] DRIFT: docs/00-current-effective-document-entry-v0.1.md 快照落后于实际（应为 720 ops / V399（377 迁移） / 55 tests）。运行 node tools/refresh-entry-doc-baseline.mjs 回填。

ISOLATED_CURRENT_REFRESH [] EXIT 0
[entry-doc-baseline] 已回填 docs/00-current-effective-document-entry-v0.1.md：720 ops / V399（377 迁移） / 55 tests（2026-09-09）。


ISOLATED_CURRENT_REFRESH ['--check'] EXIT 0
[entry-doc-baseline] PASS: 快照与实际一致（720 ops / V399（377 迁移） / 55 tests）。


AFTER_LINE126 本节附加式登记 R1~R4 阶段审计与回填交付物，以及配套的数据隔离规格与 SMS 占位规格，便于审计入口统一追溯。本节为附加内容，不删除上文任一原有登记；当前事实基线以 `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `npm run verify:*` 实时输出为准。本轮 2026-09-09 实测 OpenAPI 720 operations、Flyway 最新 V399；后端 surefire 现有报告 55 tests。
AFTER_LINE160 本节附加式登记业务决策批次与本会话已落地新功能，不删除上文任一原有内容。本轮 2026-09-09 取证为 Flyway 最新 V399、OpenAPI 720 operations；后端 surefire 现有报告 55 tests。各项以 `docs/openapi/openapi-p0.yaml`、`backend/src/main/resources/db/migration`、`backend/target/surefire-reports`、`npx playwright test --list` 与 `npm run verify:*` 实时输出为最终权威。
AFTER_README_TEST | 后端测试 | **55** tests（现有 surefire 报告） | `backend/target/surefire-reports` 合计；无报告时保留上次快照，不声明刚完成全量测试 |
BASELINE_AFTER 9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8

```

### 5.4 当前真实门禁与005数组
```python
from pathlib import Path
import subprocess,os,re,hashlib
r=Path('/home/james/complex-management');os.chdir(r)
for f,args in [('tools/verify-delivery-gates.mjs',[]),('tools/verify-html-param-tier.mjs',[]),('tools/verify-entry-anchors.mjs',[]),('tools/verify-html-ledger-sync.mjs',[]),('tools/refresh-entry-doc-baseline.mjs',['--check'])]:
 p=subprocess.run(['node',f,*args],text=True,capture_output=True);print('CURRENT',f,'EXIT',p.returncode);print(p.stdout+p.stderr)
s=(r/'tools/verify-delivery-gates.mjs').read_text()
before=subprocess.check_output(['git','show','18894a9ee67577c6c8c3d19d99281f338a82b976:tools/verify-delivery-gates.mjs'],text=True)
pat=r'const requiredDeliveryCommands = \[([\s\S]*?)\n  \];'
now=re.search(pat,s)[1];old=re.search(pat,before)[1];assert now==old
names=re.findall(r'"(verify:[^"]+)"',now);print('M008_005_UNTOUCHED',len(names),len(set(names)))

```
```text
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

```
009红属于其它已定资料模块输入，不是本轮工具回归。本方没有修改/豁免输入求全绿。当前真实refresh入口+README双漂移与5.3“只剩入口漂移”不同：5.3是before工具已经回填README后的隔离状态，不能混报。

### 5.5 冻结字节与受保护文件
本方新鲜读取全部3修改文件，均与after git blob逐字节相同。文件记录：
tools/gate-hardening-tests/m001-regression.mjs： 278行，17155bytes SHAc64e3048e7525c6158e102f9cd9eaf9cd10972c1255f9a2deb5528a8299383a6
tools/refresh-entry-doc-baseline.mjs： 126行，7807bytes SHA8bd1ba5ac141ef47dbcd409616fcacc3303546b06cceba998852895fac4f8cbb
tools/verify-delivery-gates.mjs： 333行，16444bytes SHAcbdfe7c023c3a5cc623b4f1742c56ca46238f039d701150d8d1b6abbe9102b8f
以下为本次独立受保护检查完整回执；锁及基准不变：
```text
PROTECTED_UNCHANGED docs/business-system-map-for-operations-v0.1.html a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa
PROTECTED_UNCHANGED docs/platform-governance-map-for-operations-v0.1.html c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c
PROTECTED_UNCHANGED docs/business-enum-quick-reference-v0.1.html 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66
PROTECTED_UNCHANGED tools/html-param-tier-baseline.json 9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8
PROTECTED_UNCHANGED README.md 28a46aea13227b0e3e101f9dc0a2970b6e3e581ca5995d291288aa59cb2d23b5
PROTECTED_UNCHANGED docs/00-current-effective-document-entry-v0.1.md 34e9c9f76578b61eb659dc4e369b274f62adb110b5f4dd3895bc0bb08f83e314
R1_CORRECT_BLOCK_UNCHANGED tools/verify-entry-anchors.mjs 7e62c6a19e36be5c13b166fd40aa4cb75e6ad057dfd6e97090323cae17853b82
R1_CORRECT_BLOCK_UNCHANGED tools/verify-html-ledger-sync.mjs 1c9f974f4c49c3886469e6b86236675393317b7b0379bbbd1d354599f911214d
R1_CORRECT_BLOCK_UNCHANGED tools/verify-html-param-tier.mjs c79cdb39969b123264e917aa2921e4442e2ac1679f5f78ae6fbc0c70a5e71be9
CHECK_ACTUAL_LOCK_SHA b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
SIDECAR 'b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a\n'
LEDGER_BASELINE CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
LOCK_AMEND_COUNT LOCK-AMEND-COUNT: 1
FINDINGS_SHA 25ab53318c38ddc44e7ea670b8e1c3f31f821a8a05ce64fe7cc0cdfc2e2469a9
BRANCH main
HEAD 242f487233d028173cb9198eecd079f5d877405c

```

## 6. 本方完整结论
本次全部diff审完，修复内容PASS、范围归因PASS、本方总PASS。无新增finding、无未回答业务问题。本次未读Grok raw，不能先写双PASS或出队；下一步先读取其本次完整报告，按合取规则处置，双PASS才改队列并进B5。
