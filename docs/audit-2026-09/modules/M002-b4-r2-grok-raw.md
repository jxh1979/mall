CRITERIA-LOCK-SHA256: 73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439

# M002 B4-r2 Grok 独立复审 V0.01

- 步骤：B4-r2（只审本轮全部 diff，期间不修）
- 模块：M002 `00-authority-html-02` / 共 322；ledger 状态 `IN_PROGRESS`；B3 已执行次数 = 2
- 控制：V0.17。M001 PR #410 已发布完成，不重开。
- 席位：Grok（乙）。未派子代理、未修复、未写其它文件、未提交推送、不进 B5、不宣称模块 PASS。
- 互盲：未读取 `modules/M002-b4-r2-self-raw.md` 正文，未采信 Codex 本次 B4-r2 结论。已定稿 B2（本席 `M002-b1-grok-raw.md` V0.03 / findings V0.27）与以往 B4-r1 本席报告可读且已读。
- 本席独立实算 `sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439`，与 sidecar 内容行、ledger 顶部基准相同。未改 lock / sidecar / ledger。`LOCK-AMEND-COUNT` 仍 0。PGM-066 未做完整 LOCK-AMEND。

## 0. 技能预检（收窄口径，按意图执行）

本环境核到：

| 技能 | 处置 |
|---|---|
| `using-superpowers` | 开工前自检 |
| `requesting-code-review` | 本席即外审；**不**派子代理、**不用** `HEAD~1`，基线为下列两 tree id |
| `verification-before-completion` | PASS 前实跑命令 |
| `systematic-debugging` | 只借读错误→复现→追数据流；不加诊断插桩、不改生产代码 |
| `diagnosing-bugs` | 用 CLI 做红绿环，不写 instrumentation |
| `grilling` | 对抗/第一性原理自问自答；不等操作员、不派子代理 |
| `tdd` | 核测试是否打在公开 CLI 缝上；测缝确认已由定稿 004 写死，不另问人 |
| `codebase-design` | 核测试缝=spawn 真实门禁进程，而非读私有 `EXCLUDE` |
| `ponytail-review` | 只找可删；本 diff 无额外可删 |
| `writing-for-agents` | 本 raw 给下游席位读 |
| `git-guardrails-claude-code` | 可选存在于 `skills/misc/`；本体要装 hook，本席不装 |
| `batch-grill-me` | **缺口**：已并入 `grilling`，无独立技能文件 |
| `ubiquitous-language` | **缺口**：已并入划掉的 `domain-modeling` |
| ~~`dispatching-parallel-agents` / `subagent-driven-development` / `using-git-worktrees` / `ponytail-audit` / `ponytail-debt` / `ponytail-gain` / `research`~~ | 按 V0.17 不用 |

未假造已调用不存在的技能。

## 1. 两 tree（未用 HEAD 或路径子集替代）

```text
before = 0f29c275246b1e86a5e3843f11ba413ace34052f   # git cat-file -t → tree
after  = 9f235f47e026d7268cbcfc18dbd9ce5c8014fb24   # git cat-file -t → tree
```

工作区两目标文件 blob 与 after 逐字相同（`git hash-object` = `git rev-parse after:path`）：

| 路径 | before blob | after/工作区 blob |
|---|---|---|
| `tools/verify-entry-anchors.mjs` | `8cbcac1e396bbf0641a39674b7f2a7be685d5696` | `d32629fa53b882a4409cca24511a2092e9d25ffc` |
| `tools/verify-entry-anchors.test.mjs` | 树中不存在 | `f96d9d4c90c50134bbb57d7b0b03d83e5010542d` |

```text
git diff --binary --name-status <before> <after>
M       tools/verify-entry-anchors.mjs
A       tools/verify-entry-anchors.test.mjs
```

`comm` 两树路径：before 不独有任何路径；after 只多 `tools/verify-entry-anchors.test.mjs`。共 2 文件，无 HTML、无 CSV、无 `package.json`、无 lock、无 M315 路径。

完整未过滤 binary diff SHA256 = `bc6ba3135dad818ff0693ca4395df9a531b77abe2d5d25e173bcc1cac8cec6b7`（`wc -c` = 3814）。

## 2. 完整 binary diff 全文

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

## 3. 逐块归因（闸 3）

定稿来源：`docs/audit-2026-09/findings.md` V0.27。当前队列：`M002-001` = 是（已修字节，本批不得重改 HTML）；`M002-004` = 是；`M002-002`/`M002-003` = 否。本轮 diff 只允许落到队列为「是」的已定稿 ID。

| 块 | 位置 | 变更 | 归属 ID | 闸 3 |
|---|---|---|---|---|
| A | `tools/verify-entry-anchors.mjs` `@@ -1,4 +1,5 @@` | 增加 `// 文件版本：V0.01` | M002-004（定稿明文要求自身 V0.01 版本注释） | 合法 |
| B | 同文件 `@@ -36,7 +37,7 @@` | 在既有历史留痕正则前用 `\|` 接一条锚定路径：`^docs/audit-2026-09/modules/M\d{3}-b(?:1\|4-r\d+\|5-r\d+)-(?:self\|grok)-raw\.md$`；旧分支一字未改 | M002-004（「只扩展现有 EXCLUDE，匹配 M 三位编号-b1\|b4-rN\|b5-rN-self\|grok-raw.md 精确历史路径」） | 合法 |
| C | `tools/verify-entry-anchors.test.mjs` 新文件 51 行 | Node 标准库、spawn 真实 CLI、临时目录副本 | M002-004（「另留一项可运行回归……不检查私有正则源码」） | 合法 |

无第三文件、无「顺手优化」、无换测试框架、无新依赖、无改 `walk`/`REF`/①④⑤⑥⑦、无改三份需求 HTML、无改 CSV 列、无改 `package.json`（既有 `:32` `verify:entry-anchors` 与 `:96` `verify:predeploy` 第二步仍指向同一 CLI）。

`M002-001` 本批 diff 中零行。与定稿「核实 001 已修字节仍正确，不再改 HTML 或重复递增版本」一致，不是漏修、也不是越界。

## 4. 根因是否覆盖、范围是否够窄

### 4.1 根因（类型 4）

检查②对 `walk("docs")` 得到的 md 做 `EXCLUDE.test(相对路径)`。旧正则没有 `-raw`，规定审计 raw 被当成现行 `BSM「…」` 引用。本席 B1 L213 原文仍在、未改写：

```text
4. **Q BSM「容量属于点位」是否打脸本页容量条？** 否。那是点位归属，不是第二容量维度。
```

把 **before 树脚本** 写到 `/tmp`（不改仓库）对**同一工作区**跑，与 after 脚本对照：

before（`git show 0f29c275…:tools/verify-entry-anchors.mjs` → `node /tmp/m002-b4-r2-before-verify-entry-anchors.mjs`）：

```text
[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1563 条；被检 Markdown 363 份；条目名引用 1527 处（注册表原名 1357 处，正文唯一定位 158 处）。
[entry-anchors] FAIL：12 处不合格。
  - docs/audit-2026-09/modules/M002-b1-grok-raw.md:213：BSM「容量属于点位」在该 HTML 中定位不到任何条目
  - docs/audit-2026-09/modules/M002-b1-grok-raw.md:524：BSM「容量属于点位」在该 HTML 中定位不到任何条目
  - docs/audit-2026-09/modules/M002-b1-grok-raw.md:532：BSM「容量属于点位」在该 HTML 中定位不到任何条目
  - docs/audit-2026-09/modules/M002-b4-r1-grok-raw.md:305：BSM「容量属于点位」在该 HTML 中定位不到任何条目
  - docs/audit-2026-09/modules/M002-b4-r1-grok-raw.md:306：BSM「容量属于点位」在该 HTML 中定位不到任何条目
  - docs/audit-2026-09/modules/M002-b4-r1-grok-raw.md:316：BSM「容量属于点位」在该 HTML 中定位不到任何条目
  - docs/audit-2026-09/modules/M002-b4-r1-self-raw.md:123：BSM「容量属于点位」在该 HTML 中定位不到任何条目
  - docs/audit-2026-09/modules/M002-b4-r2-self-raw.md:74：BSM「M002回归不存在条目」在该 HTML 中定位不到任何条目
  - docs/audit-2026-09/modules/M002-b4-r2-self-raw.md:83：BSM「M002回归不存在条目」在该 HTML 中定位不到任何条目
  - docs/audit-2026-09/modules/M002-b4-r2-self-raw.md:179：BSM「M002回归不存在条目」在该 HTML 中定位不到任何条目
  - docs/audit-2026-09/modules/M002-b4-r2-self-raw.md:179：BSM「M002回归不存在条目」在该 HTML 中定位不到任何条目
  - docs/audit-2026-09/modules/M002-b4-r2-self-raw.md:179：BSM「M002回归不存在条目」在该 HTML 中定位不到任何条目
BEFORE_SCRIPT_EXIT=1
```

（stderr 出现 `M002-b4-r2-self-raw.md` 是旧门禁扫文件系统的输出。本席**未打开该文件、不采信其中 B4 结论**。）

after（工作区 = after blob）：`AFTER_SCRIPT_EXIT=0`，被检 Markdown **350** 份。363−350 = **13**，与磁盘上命中新分支的 raw 文件数相等（见 §5.5）。12 处不合格全部在 `*-raw.md`；错误列表没有注册表/账本/矩阵/镜像失配，也没有 `findings.md` / `ledger.md` / 模块收口 / 普通文档。

根因覆盖：历史 raw 仍含原反例，排除规则生效后现行检查①④⑤⑥⑦计数不变（1228 / 1235 / 1244 / 1563）。不是把引用改掉、不是跳过整门禁、不是整目录排除。

### 4.2 窄到只排除规定历史 raw

定稿路径模板：`docs/audit-2026-09/modules/M<三位>-b1|b4-rN|b5-rN-(self|grok)-raw.md`。源码用 `M\d{3}` 实现「三位编号」，不是只写 M002，也不是排除整个 `docs/audit-2026-09/`。

精确 JS 正则（从 after 源抄出，`node --input-type=module`）探测：

```text
OK EXCL docs/audit-2026-09/modules/M002-b1-grok-raw.md
OK EXCL docs/audit-2026-09/modules/M002-b4-r2-grok-raw.md
OK EXCL docs/audit-2026-09/modules/M315-b5-r0-self-raw.md
OK SCAN docs/audit-2026-09/findings.md
OK SCAN docs/audit-2026-09/ledger.md
OK SCAN docs/audit-2026-09/modules/M002-00-authority-html-02.md
OK SCAN docs/domain/guide.md
OK SCAN docs/audit-2026-09/plan/runbook.md
OK SCAN docs/prd-audit/business-decision-ledger.md
OK SCAN docs/audit-2026-09/modules/M002-b2-grok-raw.md
```

`M315-b5-r0-self-raw.md` 命中 EXCL 只说明**规定命名**的历史 raw 一律不检；磁盘上无 M315 文件，ledger 中 M315 仍 `PENDING`，本 diff 不含 M315 路径。这不是把未来 M315 标成已审。

非规定名 `b2-*-raw` / 缺 `rN` 的 `b4-self-raw` 仍 SCAN。回归测试对 findings/ledger/收口/`docs/domain/guide.md` 的同一失效引用要求 exit 1，证明现行文档仍检。

## 5. 全部检查：命令与输出

### 5.1 测试前哈希（三份 HTML + 三份 CSV + 两目标文件）

```text
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
3edff56a7640565d08983621e34d726d149261e70fd747ecb88b8e9cc27774ea  docs/business-system-map-for-operations-v0.1.html
9864216e609df2f319fa2cf7e51b61f06003ce45dff64a1a4098e71435865a68  docs/platform-governance-map-for-operations-v0.1.html
9bf932a414ec71a3eee813d7c4bb37c30a6086887a698215f5a6e6a0574463d8  docs/prd-audit/entry-registry.csv
36c53884cd4a972b3421c767280a2ee8f4fa49749c2a0706a0bce8e0afbb461f  docs/prd-audit/authority-rule-ledger.csv
b4a5f1828ce9324219dece8f8b52f348a9ca3cd26f83955f5ac42ff79386d5e2  docs/prd-audit/html-prd-coverage.csv
4f00ea005a724e7f0d768070b3c250a42a68b1a341d3ec14c5acecbb78a6c46e  tools/verify-entry-anchors.mjs
f60e5421146fa5d809346d4d917c97cdfd056353982c4df63420d8353b6c7842  tools/verify-entry-anchors.test.mjs
```

`ls -d /tmp/entry-anchors-*` → `no entry-anchors temp dirs`

### 5.2 `node tools/verify-entry-anchors.test.mjs`

```text
(node:…) Warning: The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.
PASS：历史 raw 放行，现行 findings、ledger、模块收口和普通文档的失效引用仍拒绝。
CMD1_EXIT=0
```

测试只 `spawnSync(process.execPath, [root/tools/verify-entry-anchors.mjs], { cwd: temp })`，不 `import`、不读 `EXCLUDE` 源码。`writeFileSync`/`mkdirSync`/`unlinkSync`/`rmSync` 的目标均在 `os.tmpdir()` 下 `entry-anchors-` 前缀目录。对仓库根只 `readFileSync` 复制 HTML/CSV。

### 5.3 `npm run verify:entry-anchors`

```text
> entertainment-complex-management@0.1.0 verify:entry-anchors
> node tools/verify-entry-anchors.mjs

[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1563 条；被检 Markdown 350 份；条目名引用 1514 处（注册表原名 1357 处，正文唯一定位 157 处）。
[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。
CMD2_EXIT=0
```

### 5.4 `npm run verify:html-ledger-sync`

```text
> entertainment-complex-management@0.1.0 verify:html-ledger-sync
> node tools/verify-html-ledger-sync.mjs

[html-ledger-sync] PASS: 改动了 1 份业务 HTML，决策台账已同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@001878a1。
CMD3_EXIT=0
```

该「1 份业务 HTML」是相对 `origin/main` 的 **M002-001** 已修字节（V0.08 / 页脚去日期），**不是本轮 after-before diff**。本轮两树中该 HTML blob 相同。

### 5.5 测试后哈希 / 临时目录 / porcelain

测试后八个 sha256 **与测试前逐字相同**。`ls -d /tmp/entry-anchors-*` 仍无目录。

`git status --porcelain` 对三份需求 HTML + 三份 CSV：工作区相对 **HEAD** 仍显示

```text
 M docs/platform-governance-map-for-operations-v0.1.html
 M docs/prd-audit/authority-rule-ledger.csv
 M docs/prd-audit/entry-registry.csv
```

这是 B3-r1 的 001 相对已发布 `origin/main` 未提交差，**不是本测试写入**：上述哈希未变；该三文件 before/after **blob 全等**（见 §6）。

磁盘上命中新 EXCLUDE 分支的现有 raw（13）：

```text
docs/audit-2026-09/modules/M001-b1-grok-raw.md
docs/audit-2026-09/modules/M001-b1-self-raw.md
docs/audit-2026-09/modules/M001-b4-r1-grok-raw.md
docs/audit-2026-09/modules/M001-b4-r1-self-raw.md
docs/audit-2026-09/modules/M001-b4-r2-grok-raw.md
docs/audit-2026-09/modules/M001-b4-r2-self-raw.md
docs/audit-2026-09/modules/M001-b5-r2-grok-raw.md
docs/audit-2026-09/modules/M001-b5-r2-self-raw.md
docs/audit-2026-09/modules/M002-b1-grok-raw.md
docs/audit-2026-09/modules/M002-b1-self-raw.md
docs/audit-2026-09/modules/M002-b4-r1-grok-raw.md
docs/audit-2026-09/modules/M002-b4-r1-self-raw.md
docs/audit-2026-09/modules/M002-b4-r2-self-raw.md
```

before 扫 363、after 扫 350，差 13，与上表一致。M001 八份 raw 在 before 的 12 处 FAIL 中未出现（当时引用可定位）；按定稿「M 三位编号」一并排除，属规定历史 raw 命名，不是扩大模块审计范围。

## 6. M002-001 四文件本批未重改（仍正确）

两树 blob 全等：

| 路径 | before = after blob |
|---|---|
| `docs/platform-governance-map-for-operations-v0.1.html` | `8be84143b8aa74c9388a0e7865f35bad505218e0` |
| `docs/prd-audit/entry-registry.csv` | `634dd2c5bc66261c1ece901371fe20c7b50e20df` |
| `docs/prd-audit/authority-rule-ledger.csv` | `38d470b6c520bf92ddfd0c9810aae23e37aa8738` |
| `docs/prd-audit/business-decision-ledger.md` | `c1b085f55a252c2e0ccf3dd5659aac3d2c25b690` |

另两份需求 HTML 同样全等（本批未改）：ENUM `86a71793…` / BSM `24839e81…`。

现行治理 HTML：L2 `<!-- 文件版本：V0.08 -->`；L974 页脚以「用途：」起，无「更新日期」前缀；`grep -c 更新日期` = 0。文件 sha256 `9864216e609df2f319fa2cf7e51b61f06003ce45dff64a1a4098e71435865a68` 与本席 B4-r1 记录相同。申请20 两处仍在、未再递增版本。本席 B4-r1 对日期四文件的 PASS **维持**，不因 004 入队而改写。

## 7. 三线（只针对本轮 diff）

### 7.1 第一性原理

1. **为什么必须存在？** 删掉块 B，before 脚本对现行工作区仍 exit 1，`verify:predeploy` 第二步红，与用户「每模块审计 PASS 后提交/CI/合并/推送」冲突。冻结 GEN-013（脚本/CI 入口须可复现且失败显式；对不可改 raw 报「改引用/重建注册表」是错处置）与 GEN-016（不得把旧证据写成当前事实）。两条已在 M002 映射内。删掉块 C，没有公开 CLI 回归证明「raw 放行 / 现行文档仍拒绝」。删掉块 A 只少版本注释，定稿要求留下。
2. **声称行为是否被执行验证？** 是。§5 三条命令 + before/after 同工作区对照。测试断言的是 CLI exit 与 stderr 中的 `path:1`，不是正则字符串。
3. **未验证就当真的前提？** 无。未把「B3 记录 RED→GREEN」当本席证据；本席独立复现 RED（before 脚本 exit 1）与 GREEN（after exit 0）。未把 M315 当已审。未把 html-ledger-sync 的「1 份 HTML」当成 004 改了 HTML。

### 7.2 顺从式（假定修复正确，核冻结判据）

| 判据 | 本 diff | 结论 |
|---|---|---|
| GEN-013 | 无新依赖；测试用 `node:assert/fs/os/path/url/child_process`；既有 npm 脚本未改；失败仍 `process.exit(1)` | 通过 |
| GEN-016 | 历史 raw 不再被当成现行条目承诺；findings/ledger/收口/普通文档仍检；版本注释 V0.01 与文件头一致 | 通过 |
| 铁律 4 | 三份需求 HTML 不在本 diff；测试只读复制到 /tmp | 通过 |
| 铁律 6 / 闸 2 | 未新增判据、未改 lock、未改模块映射、未改 scope | 通过 |
| 闸 3 | §3 每行有 ID 且队列为是 | 通过 |

分类维持：004 技术性、有工程缺陷、队列是。无业务发明。不是添加，无 `questions/`。

### 7.3 对抗式（grilling 自答）

1. **是否整目录排除 audit？** 否。findings/ledger/plan/收口仍 SCAN；测试对它们的失效引用要求 exit 1。
2. **是否只检查私有源码？** 否。测试不读 `EXCLUDE`；spawn 真实 `verify-entry-anchors.mjs`。
3. **是否改了原 HTML/CSV？** 否。测试前后哈希全等；写路径仅 `os.tmpdir()`，测后目录不在。
4. **是否用 tautological 断言？** 否。期望值是独立字面「M002回归不存在条目」+ CLI 退出码，不是把正则再算一遍。
5. **是否把未来 M315 标已审？** 否。仅命名模板命中；无 M315 文件、无模块状态改动。
6. **001 是否被本批弄坏？** 否。四文件 blob 全等，日期检查仍过，html-ledger-sync PASS。
7. **旧 `-report` 分支会不会误伤 raw？** raw 名是 `-raw` 不是 `-report`；新分支用 `\|` 前接，旧分支未改。
8. **工作区 `git status` 的 HTML M 是否测试副作用？** 否；相对 HEAD 的 001 未提交差，哈希未变。

ponytail-review：`Lean already. Ship.` `net: -0`。版本注释与 51 行 CLI 回归均在定稿 004 范围内，不是额外抽象。

## 8. 发现分类与冻结依据

本轮 **无新 GR**。不启用锁外判据。

| ID | 分类 | 队列 | 本轮 diff | 冻结 |
|---|---|---|---|---|
| M002-001 | 冲突 | 是 | 未出现（已修字节仍正确） | GEN-016；申请20 |
| M002-002 | 细化 | 否 | 未出现 | 维持 B2 |
| M002-003 | 技术性无缺陷 | 否 | 未出现 | 维持 B2 |
| M002-004 | 技术性有缺陷 | 是 | 两文件全部归因于此 | GEN-013、GEN-016 |

## 9. 明确不做的声明

- **不是** B5，**不是**模块 PASS，**不是**双席合取结果（本文件只是 Grok 一票）。
- **不是** PGM-066 的 V0.17 完整 LOCK-AMEND；lock 引文仍待后续五步，COUNT 保持 0。
- **不是** M315 已审。工具文件虽落在未来 M315 切分里，本条只是本模块发布门禁的必要辅助，不改 `scope.txt` / `modules.md`。
- M001 保持审计及发布 PASS，不重开。

## 10. B4 结论

**PASS**

不是有条件通过。合取项本席已核：两 tree 真实且 diff 恰 2 文件；未过滤 binary diff 全文已收录；每一变更行归因已定稿且队列为是的 M002-004；排除规则覆盖规定历史 raw、现行 findings/ledger/收口/普通文档仍拒绝失效引用；真实 CLI 测试有效且只写临时副本、不改原 HTML/CSV；`node tools/verify-entry-anchors.test.mjs`、`npm run verify:entry-anchors`、`npm run verify:html-ledger-sync` 本席实跑均为 0；before 脚本对同一工作区仍 RED（exit 1，12 处全在 raw），after GREEN；001 四文件本批未改且仍正确；锁未改。无待补项、无未答复添加项、无新 GR、无越界行。

本票只覆盖 M002/B4-r2 本轮 diff。不进入 B5，不改其它文件，不提交推送。
