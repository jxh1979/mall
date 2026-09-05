CRITERIA-LOCK-SHA256: 73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439

# M002 B4-r2 Codex 原始报告

报告版本：V0.01；控制V0.17；模块M002/322；IN_PROGRESS；B3已执行2次。先派Grok，本方审完全部diff并落本raw前未读本次Grok报告或原始JSON。本节点只审完整修复diff，不修源码。

## 两树与完整范围

before=0f29c275246b1e86a5e3843f11ba413ace34052f
after=9f235f47e026d7268cbcfc18dbd9ce5c8014fb24
完整未过滤binary diff SHA256=bc6ba3135dad818ff0693ca4395df9a531b77abe2d5d25e173bcc1cac8cec6b7。
两树由尚不存在路径的临时索引git add -A/git write-tree取得，涵盖当时全部tracked/staged/untracked。before先打印后修改；after取得后ledger V0.63才记录B3=2。真实索引SHA256=040cc6a55775bb41014c00b6ebc2728983138f2bea8289e55263ff377684e9ad未变。

本次两路径全文已读、实际源码等于after blob。既有门禁262行已追完整调用流：package.json verify:entry-anchors→tools脚本→walk docs→EXCLUDE→REF；verify:predeploy强制调用该入口。本次只多一行自身版本和原EXCLUDE的一个精确raw分支；其余源码逐字等于before。新测试51行全部包含下方diff。

```diff
diff --git a/tools/verify-entry-anchors.mjs b/tools/verify-entry-anchors.mjs
index 8cbcac1e..d32629fa 100755
--- a/tools/verify-entry-anchors.mjs
+++ b/tools/verify-entry-anchors.mjs
@@ -1,4 +1,5 @@
 #!/usr/bin/env node
+// 文件版本：V0.01
 // 门禁：三份业务最终解释权 HTML 的引用一律用「条目名」，不得再用行号。
 // 依据：DEC-R8-17（用户 2026-08-06 裁 A：彻底废行号、只留条目名）。
 // 病史：R8 两批 6 轮返工里有 3 轮因行号锚点漂移返工（760 行漂移 → 287 处重建 → +2 位移 19 处漏改）。
@@ -36,7 +37,7 @@ const REGISTRY = "docs/prd-audit/entry-registry.csv";
 const LEDGER = "docs/prd-audit/authority-rule-ledger.csv";
 const COVERAGE = "docs/prd-audit/html-prd-coverage.csv";
 // 历史留痕文件：记录的是「当时」的事实，一律不改、不检
-const EXCLUDE = /(changelog|change-log|-report|\.report\.|diff-matrix|anchor-rebuild|html-map|reanchor|html-mirror-scan|history-index|[\\/]rounds[\\/])/i;
+const EXCLUDE = /^docs\/audit-2026-09\/modules\/M\d{3}-b(?:1|4-r\d+|5-r\d+)-(?:self|grok)-raw\.md$|(changelog|change-log|-report|\.report\.|diff-matrix|anchor-rebuild|html-map|reanchor|html-mirror-scan|history-index|[\\/]rounds[\\/])/i;
 
 const errors = [];
 const warns = [];
diff --git a/tools/verify-entry-anchors.test.mjs b/tools/verify-entry-anchors.test.mjs
new file mode 100644
index 00000000..f96d9d4c
--- /dev/null
+++ b/tools/verify-entry-anchors.test.mjs
@@ -0,0 +1,51 @@
+// 文件版本：V0.01
+// M002-004：实际运行引用门禁，历史 raw 不参与校验，现行文档仍须定位。
+import assert from "node:assert/strict";
+import fs from "node:fs";
+import os from "node:os";
+import path from "node:path";
+import { fileURLToPath } from "node:url";
+import { spawnSync } from "node:child_process";
+
+const root = fileURLToPath(new URL("../", import.meta.url));
+const temp = fs.mkdtempSync(path.join(os.tmpdir(), "entry-anchors-"));
+const run = () => spawnSync(process.execPath, [path.join(root, "tools/verify-entry-anchors.mjs")], { cwd: temp, encoding: "utf8" });
+const put = (file, text) => {
+  const target = path.join(temp, file);
+  fs.mkdirSync(path.dirname(target), { recursive: true });
+  fs.writeFileSync(target, text, "utf8");
+};
+try {
+  for (const file of [
+    "docs/business-system-map-for-operations-v0.1.html",
+    "docs/platform-governance-map-for-operations-v0.1.html",
+    "docs/business-enum-quick-reference-v0.1.html",
+    "docs/prd-audit/entry-registry.csv",
+    "docs/prd-audit/authority-rule-ledger.csv",
+    "docs/prd-audit/html-prd-coverage.csv",
+  ]) put(file, fs.readFileSync(path.join(root, file), "utf8"));
+  let result = run();
+  assert.equal(result.status, 0, result.stdout + result.stderr);
+  for (const file of [
+    "docs/audit-2026-09/modules/M002-b1-grok-raw.md",
+    "docs/audit-2026-09/modules/M002-b4-r1-self-raw.md",
+    "docs/audit-2026-09/modules/M002-b5-r0-grok-raw.md",
+  ]) put(file, "历史反例：BSM「M002回归不存在条目」。\n");
+  result = run();
+  assert.equal(result.status, 0, result.stdout + result.stderr);
+  for (const file of [
+    "docs/audit-2026-09/findings.md",
+    "docs/audit-2026-09/ledger.md",
+    "docs/audit-2026-09/modules/M002-00-authority-html-02.md",
+    "docs/domain/guide.md",
+  ]) {
+    put(file, "现行引用：BSM「M002回归不存在条目」。\n");
+    result = run();
+    assert.equal(result.status, 1, result.stdout + result.stderr);
+    assert.ok(result.stderr.includes(file + ":1"), result.stderr);
+    fs.unlinkSync(path.join(temp, file));
+  }
+  console.log("PASS：历史 raw 放行，现行 findings、ledger、模块收口和普通文档的失效引用仍拒绝。");
+} finally {
+  fs.rmSync(temp, { recursive: true, force: true });
+}
```

## 逐块归因与授权

findings V0.26双席B2 PASS，V0.27记录定稿；001/004是，002/003否。Grok B2 V0.03接受004入当前模块修复队列，保留其B4-r1四文件diff正确的原PASS，没有改写对方。

| 变更 | 归因 | 核实 |
|---|---|---|
| tools/verify-entry-anchors.mjs:2 自身V0.01 | M002-004 | 原无自身版本；用户版本铁律要求本次写入有自身版本 |
| 同文件:40 EXCLUDE窄路径分支 | M002-004 | 历史raw漏过滤的共享入口；只匹配本轮规定M三位编号、B1/B4-rN/B5-rN、self/grok、raw后缀 |
| tools/verify-entry-anchors.test.mjs:1–51 | M002-004 | 一项真实CLI回归，捕获历史raw误报及整目录跳检两种错误；标准库，无框架或新依赖 |

所有新增/删除行均归已定稿004，无第五文件或无ID改动。001上批4文件已在本轮before拍照前与edc03163560e2373adb503a97cd22d76928d5ce0逐字比较相等，仍为已批准两处HTML加必要镜像/台账；本批不重复递增HTML版本。原B4-r1的唯一阻断因004修复已消失；不存在越界回滚条件。

## 三线核查

第一性原理：
1. 为何必须存在：删掉原门禁，失效现行引用/镜像不同步不再被拦；所以保留。只删掉本次raw分支，会再次把原始审计中的反例当现行引用、阻断用户要求的每模块发布。冻结GEN-013/016，依据类型4。
2. 声称行为已由真实CLI运行验证；新测试不读取私有EXCLUDE实现，不重写生产源码或原HTML。
3. 未经核实前提已落成负例：历史raw必须排除，现行台账及普通文档必须继续检查。两个错误实现的临时副本均被同一测试拒绝。

顺从式：旧EXCLUDE所有分支保留，新增路径首尾锚定、仅本轮规范raw命名。REF、CSV比对、镜像列、覆盖矩阵及walk保持原逻辑。四类现行文件不被新增分支覆盖。版本统一Vx.xx。HTML及历史报告未改。

对抗式：测试临时根由mkdtemp创建；只复制当前三份HTML及三张CSV，唯一写入/删除发生在自己拥有的临时根内，finally清理。复制完成先验证无Markdown基线exit0，防坏镜像掩盖行为。三个raw同放应exit0；四类现行文件依次加入同一失效引用，各应exit1且错误指向该文件:1，之后移除以隔离下一例。子进程启动失败或signal导致status=null会被严格断言拒绝；测试不会靠别的文件失败来冒充现行文件拒绝。

## 真实验证输出

本轮B3先落测试，原门禁实际RED（ERR_ASSERTION actual1/expected0，历史三raw失效引用），再改共享EXCLUDE，正式测试GREEN。B4独立再次执行以下完整命令，均exit0：

```json
[
  {
    "command": [
      "node",
      "tools/verify-entry-anchors.test.mjs"
    ],
    "exit": 0,
    "stdout": "PASS：历史 raw 放行，现行 findings、ledger、模块收口和普通文档的失效引用仍拒绝。\n",
    "stderr": ""
  },
  {
    "command": [
      "npm",
      "run",
      "verify:entry-anchors"
    ],
    "exit": 0,
    "stdout": "\n> entertainment-complex-management@0.1.0 verify:entry-anchors\n> node tools/verify-entry-anchors.mjs\n\n[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1563 条；被检 Markdown 350 份；条目名引用 1514 处（注册表原名 1357 处，正文唯一定位 157 处）。\n[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。\n",
    "stderr": ""
  },
  {
    "command": [
      "npm",
      "run",
      "verify:html-ledger-sync"
    ],
    "exit": 0,
    "stdout": "\n> entertainment-complex-management@0.1.0 verify:html-ledger-sync\n> node tools/verify-html-ledger-sync.mjs\n\n[html-ledger-sync] PASS: 改动了 1 份业务 HTML，决策台账已同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@001878a1。\n",
    "stderr": ""
  },
  {
    "command": [
      "git",
      "diff",
      "--check",
      "0f29c275246b1e86a5e3843f11ba413ace34052f",
      "9f235f47e026d7268cbcfc18dbd9ce5c8014fb24"
    ],
    "exit": 0,
    "stdout": "",
    "stderr": ""
  }
]
```

## 测试有效性：两种错误实现都能被捕获

只在临时副本写入两种故意错误实现，仓库源码没有改动：
- missing_raw_exclusion：使用before树原工具，测试因历史raw被扫而失败。
- overbroad_audit_exclusion：故意跳过整个本轮audit目录，测试因现行findings错误引用被放行而失败。

```json
[
  {
    "mutation": "missing_raw_exclusion",
    "exit": 1,
    "stderr": "node:internal/modules/run_main:107\n    triggerUncaughtException(\n    ^\n\nAssertionError [ERR_ASSERTION]: [entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1563 条；被检 Markdown 3 份；条目名引用 3 处（注册表原名 0 处，正文唯一定位 0 处）。\n[entry-anchors] FAIL：3 处不合格。\n  - docs/audit-2026-09/modules/M002-b1-grok-raw.md:1：BSM「M002回归不存在条目」在该 HTML 中定位不到任何条目\n  - docs/audit-2026-09/modules/M002-b4-r1-self-raw.md:1：BSM「M002回归不存在条目」在该 HTML 中定位不到任何条目\n  - docs/audit-2026-09/modules/M002-b5-r0-grok-raw.md:1：BSM「M002回归不存在条目」在该 HTML 中定位不到任何条目\n  处置：条目改名或 HTML 改动后，重建 docs/prd-audit/entry-registry.csv 并把引用改为现行条目名；一律不得写回行号。\n\n\n1 !== 0\n\n    at file:///tmp/m002-b4-mutation-kxu0h7wp/tools/verify-entry-anchors.test.mjs:35:10\n    at ModuleJob.run (node:internal/modules/esm/module_job:561:25)\n    at async node:internal/modules/esm/loader:647:26\n    at async asyncRunEntryPointWithESMLoader (node:internal/modules/run_main:101:5) {\n  generatedMessage: false,\n  code: 'ERR_ASSERTION',\n  actual: 1,\n  expected: 0,\n  operator: 'strictEqual',\n  diff: 'simple'\n}\n\nNode.js v24.20.0\n"
  },
  {
    "mutation": "overbroad_audit_exclusion",
    "exit": 1,
    "stderr": "node:internal/modules/run_main:107\n    triggerUncaughtException(\n    ^\n\nAssertionError [ERR_ASSERTION]: [entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1563 条；被检 Markdown 0 份；条目名引用 0 处（注册表原名 0 处，正文唯一定位 0 处）。\n[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。\n\n\n0 !== 1\n\n    at file:///tmp/m002-b4-mutation-kxu0h7wp/tools/verify-entry-anchors.test.mjs:44:12\n    at ModuleJob.run (node:internal/modules/esm/module_job:561:25)\n    at async node:internal/modules/esm/loader:647:26\n    at async asyncRunEntryPointWithESMLoader (node:internal/modules/run_main:101:5) {\n  generatedMessage: false,\n  code: 'ERR_ASSERTION',\n  actual: 0,\n  expected: 1,\n  operator: 'strictEqual',\n  diff: 'simple'\n}\n\nNode.js v24.20.0\n"
  }
]
```

可复现命令（本席实际执行的只读主仓诊断脚本；临时副本自行清理）：

```python
from pathlib import Path
import subprocess,hashlib,json,tempfile,shutil,re
before='0f29c275246b1e86a5e3843f11ba413ace34052f';after='9f235f47e026d7268cbcfc18dbd9ce5c8014fb24'
paths=subprocess.check_output(['git','diff','--name-only',before,after],text=True).splitlines()
assert paths==['tools/verify-entry-anchors.mjs','tools/verify-entry-anchors.test.mjs']
diff=subprocess.check_output(['git','diff','--binary',before,after]);assert hashlib.sha256(diff).hexdigest()=='bc6ba3135dad818ff0693ca4395df9a531b77abe2d5d25e173bcc1cac8cec6b7'
for p in paths:assert Path(p).read_bytes()==subprocess.check_output(['git','show',after+':'+p])
source=Path(paths[0]).read_text(encoding='utf-8');base=subprocess.check_output(['git','show',before+':'+paths[0]]).decode('utf-8')
old=[x for x in base.splitlines() if x.startswith('const EXCLUDE = ')][0];new=[x for x in source.splitlines() if x.startswith('const EXCLUDE = ')][0]
assert source==base.replace('#!/usr/bin/env node\n','#!/usr/bin/env node\n// 文件版本：V0.01\n',1).replace(old,new,1)
checks=[]
for args in [['node',paths[1]],['npm','run','verify:entry-anchors'],['npm','run','verify:html-ledger-sync'],['git','diff','--check',before,after]]:
 r=subprocess.run(args,text=True,capture_output=True);checks.append({'command':args,'exit':r.returncode,'stdout':r.stdout,'stderr':r.stderr});assert r.returncode==0
fixtures=['docs/business-system-map-for-operations-v0.1.html','docs/platform-governance-map-for-operations-v0.1.html','docs/business-enum-quick-reference-v0.1.html','docs/prd-audit/entry-registry.csv','docs/prd-audit/authority-rule-ledger.csv','docs/prd-audit/html-prd-coverage.csv']
hashes={p:hashlib.sha256(Path(p).read_bytes()).hexdigest() for p in fixtures}
mutations=[]
with tempfile.TemporaryDirectory(prefix='m002-b4-mutation-') as tmp:
 root=Path(tmp)
 for p in fixtures+[paths[1]]:
  dst=root/p;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,dst)
 for label,mutant in [('missing_raw_exclusion',base),('overbroad_audit_exclusion',source.replace(new,r'const EXCLUDE = /^docs\/audit-2026-09\//i;'))]:
  (root/paths[0]).write_text(mutant,encoding='utf-8')
  r=subprocess.run(['node',str(root/paths[1])],text=True,capture_output=True)
  assert r.returncode==1 and 'ERR_ASSERTION' in r.stderr
  mutations.append({'mutation':label,'exit':r.returncode,'stderr':r.stderr})
assert hashes=={p:hashlib.sha256(Path(p).read_bytes()).hexdigest() for p in fixtures}
lock=Path('docs/audit-2026-09/plan/criteria-lock.md');currenthash=hashlib.sha256(lock.read_bytes()).hexdigest()
print(json.dumps({'before':before,'after':after,'diff_sha256':hashlib.sha256(diff).hexdigest(),'diff':diff.decode('utf-8'),'paths':paths,'checks':checks,'mutations':mutations,'original_authority_and_mirrors_untouched':hashes,'lock_actual':currenthash,'source_two_changes_only':True,'test_lines':len(Path(paths[1]).read_text().splitlines())},ensure_ascii=False))

```

原始业务HTML与机器镜像在验证前后SHA相同：
```json
{
  "docs/business-system-map-for-operations-v0.1.html": "3edff56a7640565d08983621e34d726d149261e70fd747ecb88b8e9cc27774ea",
  "docs/platform-governance-map-for-operations-v0.1.html": "9864216e609df2f319fa2cf7e51b61f06003ce45dff64a1a4098e71435865a68",
  "docs/business-enum-quick-reference-v0.1.html": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66",
  "docs/prd-audit/entry-registry.csv": "9bf932a414ec71a3eee813d7c4bb37c30a6086887a698215f5a6e6a0574463d8",
  "docs/prd-audit/authority-rule-ledger.csv": "36c53884cd4a972b3421c767280a2ee8f4fa49749c2a0706a0bce8e0afbb461f",
  "docs/prd-audit/html-prd-coverage.csv": "b4a5f1828ce9324219dece8f8b52f348a9ca3cd26f83955f5ac42ff79386d5e2"
}
```

## 技能及结论

实际沿用已读using-superpowers、requesting-code-review（只派Grok、两树基线）、receiving-code-review、systematic-debugging/diagnosing-bugs、verification-before-completion；TDD/tdd实际落失败测试后最小修复，ponytail复用现有共享规则及Node标准库。未用第三席、未新增审计产物类型、未提前全量审M315。

无新CX工程缺陷；004仍为既定技术性/有工程缺陷的修复验证，无新增业务发明。判据锁实算仍为首行值、COUNT0，PGM-066后续完整LOCK-AMEND尚未开始。本报告不替代B5整模块审计。

本席B4-r2结论：**PASS**。两路径完整diff正确、全行归因004合法，实际原门禁和回归均通过。须Grok本次也PASS后才能出队和进入后续流程；此时M002尚未PASS。
