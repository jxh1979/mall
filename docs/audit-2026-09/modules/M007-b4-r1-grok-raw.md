CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M007 B4-r1 修复 diff 独立复审（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：Grok 独立审计席乙
计划：当前 471 / V0.17（固定 Codex/Grok 两席；本席不派子代理、不用 MCP、不修源/HTML/台账）
本文件路径：docs/audit-2026-09/modules/M007-b4-r1-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）

审计结论：PASS。
范围归因结论：PASS。
最终结论：PASS。
以上仅为席乙本轮票，不等于 B4 双 PASS，也不等于模块 PASS。不可用 B2 或 B3 自报 PASS 代替本轮完整审计；本轮已独立取树、取完整 diff、复跑红绿与边界。

未读取：本轮 self raw、任何其它历史模块 raw 正文、本席历史 B1/B2 raw 正文。定稿队列以 findings.md V1.08 顶部为准。

本轮实际应用技能（V0.17 收窄，不引入第三席）：requesting-code-review / receiving-code-review（本席自行审完整 diff，不派审查子代理）、verification-before-completion、grilling、diagnosing-bugs、ponytail-review。判据仅用冻结 criteria-lock 既有 GEN/HTML 分类，未发明新判据。

---

## 1. 输入

| 项 | 值 |
|---|---|
| 模块 | M007 治理门禁与 CI 基线／运行和发布脚本／第1段 |
| 阶段 | B4-r1 只读修复 diff 复审 |
| 正式队列 | findings.md V1.08 顶部：B2-r1 双 PASS 定稿 001–004 是，005–011 否 |
| B3 | 累计 1，仍 IN_PROGRESS |
| before（精确，对象类型 tree） | `ac86ec95b63a0816b050be4f90c38abccc1fdbb0` |
| after（精确，对象类型 tree） | `9f63094e21e4ed94a29b29071af5bd07cf99fd6a` |
| 本席自取 treeid | `git cat-file -t` 两者均为 `tree`；`git rev-parse <id>^{tree}` 与输入 id 相同 |
| 完整 diff 命令 | `git diff --binary ac86ec95b63a0816b050be4f90c38abccc1fdbb0 9f63094e21e4ed94a29b29071af5bd07cf99fd6a`（无路径过滤） |
| 实际差异 | 9770 bytes；SHA256 `0f6450cf482a1990d56ddde4ab004521ef52d98759a946c1c77b1521dd6c6265`；186 行 unified；5 文件；116 插入 / 7 删除 |
| `git diff --stat` | `ops/160/hooks/pre-push` 8±；`install-git-hooks.sh` 3±；`merge-pr.sh` 4±；`propose-change.sh` 6+；`m007-regression.mjs` 102+ |
| `git diff --numstat` | 4/4、2/1、2/2、6/0、102/0 |
| name-status | M 四脚本；A 测试 |
| 主索引 | 未用主暂存区做 B3。当前 `.git/index` SHA256 `ae1dfbf11bec123e6a662097d16de9bb7088fe8bc50d1e9ae4a78da24e90022c` |
| 主索引 write-tree | `2af03dee3a7d65c4e745c9aa3173da2050890a65`（= HEAD^{tree} = origin/main^{tree}，≠ after 树；after 为临时索引快照） |
| HEAD | `242f487233d028173cb9198eecd079f5d877405c` |
| 工作区五文件 blob | 均等于 after 树 blob（见 §6） |
| 定稿 001 | 第三字段 `remote_ref` 仅按目标 main；删误导注释；保留 ALLOW_MAIN_PUSH / 删除 main / 普通 feature / mainish / tag |
| 定稿 002 | `git config --path --get`；维持默认/相对/绝对/worktree-common 并修 tilde；不改英文消息 |
| 定稿 003 | propose 在 npm 之前拒脏工作区；不改最终 merge 准入 |
| 定稿 004 | 只去掉 run list 的 stderr 丢弃与 `\|\| true`；非 0 立即退出并保留原因；真实空列表仍 30 次；其它 run-view 容错 / skip 不动 |
| 禁止 | 改三 HTML、锁、CI 镜像、真实 `.git/hooks`；把未执行的 hook 重装/生产发布冒称完成；全仓父链/部署/真实远端写；新增依赖 |

本席独立重算锁（命令 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`）：

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 同值。锁文件 blob 两树均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`。criteria-lock 版本 V0.11，18 GEN + 383 HTML 锚点，未改。

---

## 2. 锁与分类（既有，不新造）

B2 定稿 001–004 均为技术性、工程缺陷是、修复队列是。本轮只核对修复是否落在该四项及允许的最小测试/版本行。

适用冻结 GEN（工程）：GEN-001 最小性、GEN-002 可执行正确性、GEN-003 测试有效性、GEN-004 信任边界、GEN-008 接口/协议一致、GEN-009 错误处理、GEN-011 可观测、GEN-013 构建/依赖/配置、GEN-016 文档可验证、GEN-017 分类、GEN-018 第一性原理证据。

GEN-017：脚本与回归测试无票务/财务/审批/枚举发明，保持技术性。三 HTML 两树 blob 不变，无 HTML 冲突或细化改写。

PGM-066 / GEN-015：M007-008 已定稿不改英文 CLI；本 diff 未改 `installed pre-push hook -> $dest/pre-push`。

---

## 3. 完整未过滤 binary diff（全文，非链接）

命令：`git diff --binary ac86ec95b63a0816b050be4f90c38abccc1fdbb0 9f63094e21e4ed94a29b29071af5bd07cf99fd6a`

字节 9770；SHA256 `0f6450cf482a1990d56ddde4ab004521ef52d98759a946c1c77b1521dd6c6265`；`git diff --check` exit 0。

````diff
diff --git a/ops/160/hooks/pre-push b/ops/160/hooks/pre-push
index 45966c01..3ae3d237 100755
--- a/ops/160/hooks/pre-push
+++ b/ops/160/hooks/pre-push
@@ -1,11 +1,11 @@
 #!/usr/bin/env bash
+# 文件版本：V0.01
 # 2026-07-05:禁止直推 main(补 GitHub 私有库免费版无分支保护的缺口)。开发者提示,非安全边界。
 # 正道:推 feature 分支 → 开 PR → 独立复核 + predeploy 门禁绿 → gh pr merge。
 # 紧急直推需 ALLOW_MAIN_PUSH=1 覆盖(留痕:请在 PR/提交说明记录原因)。
-while read -r line; do
-  # 稳健取 remote ref:git 传入「<localref> <localsha> <remoteref> <remotesha>」,
-  # remoteref 目标 refs/heads/main 本身无空格;直接整行匹配该目标,避免源 ref 含空格致字段错位漏拦。
-  if [[ " $line " == *" refs/heads/main "* ]]; then
+while read -r local_ref local_sha remote_ref remote_sha; do
+  # Git 标准输入四字段,只按第三字段 remote_ref 判断目标分支。
+  if [[ "$remote_ref" == "refs/heads/main" ]]; then
     if [[ "${ALLOW_MAIN_PUSH:-0}" != "1" ]]; then
       echo "[pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。" >&2
       echo "[pre-push] 确需直推:ALLOW_MAIN_PUSH=1 git push ...(并在 PR/说明记录原因)。" >&2
diff --git a/ops/160/install-git-hooks.sh b/ops/160/install-git-hooks.sh
index 2362788d..c3dc157d 100755
--- a/ops/160/install-git-hooks.sh
+++ b/ops/160/install-git-hooks.sh
@@ -1,9 +1,10 @@
 #!/usr/bin/env bash
+# 文件版本：V0.01
 # 安装仓库 git 钩子到生效的 hooks 目录(worktree 共用)。新克隆/新机首次须跑一次。
 # 注意:若仓库/全局设了 core.hooksPath,Git 只认该路径——装到别处会静默失效,故在此显式处理。
 set -euo pipefail
 cd "$(dirname "$0")/../.."
-custom="$(git config --get core.hooksPath || true)"
+custom="$(git config --path --get core.hooksPath || true)"
 if [[ -n "$custom" ]]; then
   dest="$custom"
   echo "[install-hooks] 检测到 core.hooksPath=$custom,安装到该路径(否则钩子不生效)。"
diff --git a/ops/160/merge-pr.sh b/ops/160/merge-pr.sh
index e2952ca8..ef2a97b4 100755
--- a/ops/160/merge-pr.sh
+++ b/ops/160/merge-pr.sh
@@ -1,5 +1,5 @@
 #!/usr/bin/env bash
-# 文件版本：V0.01
+# 文件版本：V0.02
 # 合并前置检查(2026-08-07 门禁升级批 1,用户弹窗批立项):CI 绿灯才允许合并。
 # 病史:本仓是私有库免费版,设不了 required status check,.github/workflows/predeploy-gates.yml
 #      自己在注释里写明只是「可见信号」。也就是说 gh pr merge 对红灯毫无抵抗力,全靠人记得去看一眼。
@@ -41,7 +41,7 @@ else
   echo "[merge] 等 predeploy-gates 对 ${head_sha:0:8} 的 CI 结果..."
   run_ids=""
   for i in $(seq 1 30); do
-    run_ids="$(gh run list --commit "$head_sha" --workflow predeploy-gates.yml --limit 10 --json databaseId -q '.[].databaseId' 2>/dev/null || true)"
+    run_ids="$(gh run list --commit "$head_sha" --workflow predeploy-gates.yml --limit 10 --json databaseId -q '.[].databaseId' )"
     [[ -n "$run_ids" ]] && break
     echo "[merge]   还没有 run(第 $i/30 次),10s 后重试..."
     sleep 10
diff --git a/ops/160/propose-change.sh b/ops/160/propose-change.sh
index eb7198de..35a5139e 100755
--- a/ops/160/propose-change.sh
+++ b/ops/160/propose-change.sh
@@ -1,4 +1,5 @@
 #!/usr/bin/env bash
+# 文件版本：V0.01
 # 变更提议流程(2026-07-05 起替代直推 main):
 #   推 feature 分支 → 开/更新 PR → (调用方另跑独立复核) → predeploy 门禁绿 → ops/160/merge-pr.sh。
 #   合并动作一律走 merge-pr.sh(它会等 CI 绿),不要手敲 gh pr merge;CI 故障需 SKIP_CI_WAIT=1 时须在 PR 说明原因。
@@ -8,6 +9,11 @@ title="${1:?需要 PR 标题}"; body="${2:-}"
 branch="$(git rev-parse --abbrev-ref HEAD)"
 [[ "$branch" == "main" ]] && { echo "不要在 main 上提议;先切 feature 分支。" >&2; exit 1; }
 
+if [[ -n "$(git status --porcelain)" ]]; then
+  echo "[propose] 工作区有未提交改动;请先提交或 stash,再运行本地检查并推送。" >&2
+  exit 1
+fi
+
 echo "[propose] 本地跑 predeploy 门禁(红则不推)..."
 npm run verify:predeploy
 
diff --git a/tools/gate-hardening-tests/m007-regression.mjs b/tools/gate-hardening-tests/m007-regression.mjs
new file mode 100644
index 00000000..6fc4720e
--- /dev/null
+++ b/tools/gate-hardening-tests/m007-regression.mjs
@@ -0,0 +1,102 @@
+// 文件版本：V0.01
+// M007-001～004：执行真实脚本；只替换 GitHub/推送边界，禁止真实发布。
+import assert from "node:assert/strict";
+import fs from "node:fs";
+import path from "node:path";
+import { fileURLToPath } from "node:url";
+import { spawnSync } from "node:child_process";
+
+const root = fileURLToPath(new URL("../../", import.meta.url));
+const scratch = path.join(root, ".scratch");
+fs.mkdirSync(scratch, { recursive: true });
+const temp = fs.mkdtempSync(path.join(scratch, "m007-regression-"));
+const failures = [];
+const exec = (cmd, args, cwd, env = {}, input) => {
+  const result = spawnSync(cmd, args, { cwd, input, encoding: "utf8", env: { ...process.env, ...env } });
+  assert.equal(result.error, undefined);
+  return result;
+};
+const put = (file, text, mode) => {
+  fs.mkdirSync(path.dirname(file), { recursive: true });
+  fs.writeFileSync(file, text, "utf8");
+  if (mode) fs.chmodSync(file, mode);
+};
+const check = (name, fn) => {
+  try { fn(); console.log("PASS " + name); }
+  catch (error) { failures.push(name); console.error("FAIL " + name + ": " + error.message); }
+};
+try {
+  // M007-001：目的分支才决定是否拦截；源 main、目标 feature 应放行。
+  for (const [local, remote, allow, want] of [
+    ["main", "feature/a", "0", 0], ["feature/a", "main", "0", 1],
+    ["feature/a", "main", "1", 0], ["feature/a", "mainish", "0", 0],
+  ]) check("M007-001 " + local + " -> " + remote + " allow=" + allow, () => {
+    const input = "refs/heads/" + local + " " + "a".repeat(40) +
+      " refs/heads/" + remote + " " + "b".repeat(40) + "\n";
+    const p = exec("bash", [path.join(root, "ops/160/hooks/pre-push")],
+      root, { ALLOW_MAIN_PUSH: allow }, input);
+    assert.equal(p.status, want, p.stderr);
+  });
+
+  // M007-002：真实 Git 解析 ~/ 路径；验证安装产物落到 Git 使用的位置。
+  const installRepo = path.join(temp, "install");
+  const home = path.join(temp, "home");
+  fs.mkdirSync(home);
+  for (const rel of ["ops/160/install-git-hooks.sh", "ops/160/hooks/pre-push"])
+    put(path.join(installRepo, rel), fs.readFileSync(path.join(root, rel), "utf8"));
+  const installEnv = { HOME: home, GIT_CONFIG_GLOBAL: "/dev/null", GIT_CONFIG_NOSYSTEM: "1" };
+  check("M007-002 Git 生效路径", () => {
+    for (const args of [["init", "-q"], ["config", "core.hooksPath", "~/custom-hooks"]]) {
+      const p = exec("git", args, installRepo, installEnv);
+      assert.equal(p.status, 0, p.stderr);
+    }
+    const p = exec("bash", ["ops/160/install-git-hooks.sh"], installRepo, installEnv);
+    assert.equal(p.status, 0, p.stderr);
+    const installed = path.join(home, "custom-hooks/pre-push");
+    assert.ok(fs.existsSync(installed), "安装成功但 Git 的 HOME/custom-hooks/pre-push 不存在");
+    assert.deepEqual(fs.readFileSync(installed), fs.readFileSync(path.join(root, "ops/160/hooks/pre-push")));
+    assert.ok(fs.statSync(installed).mode & 0o111);
+  });
+
+  // M007-003/004：复用一个窄的进程边界；未知命令失败，真实源码不替换。
+  const bin = path.join(temp, "bin");
+  put(path.join(bin, "git"), [
+    "#!/usr/bin/env bash", "set -eu",
+    'case "$*" in',
+    " 'rev-parse --abbrev-ref HEAD') echo feature/m007;;",
+    " 'status --porcelain') if [[ $M007_CASE == dirty ]]; then echo ' M pending.txt'; fi;;",
+    " 'rev-parse HEAD') echo aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;;",
+    " 'ls-remote origin refs/heads/feature/m007') printf 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\\trefs/heads/feature/m007\\n';;",
+    " 'push -u origin feature/m007') touch pushed;;",
+    ' *) echo "unexpected git: $*" >&2; exit 97;;', "esac", "",
+  ].join("\n"), 0o755);
+  put(path.join(bin, "npm"), "#!/usr/bin/env bash\nexit 0\n", 0o755);
+  put(path.join(bin, "sleep"), "#!/usr/bin/env bash\nexit 0\n", 0o755);
+  put(path.join(bin, "gh"), [
+    "#!/usr/bin/env bash", "set -eu", 'case "$1 $2" in',
+    " 'pr view') :;;",
+    " 'run list') echo AUDIT_QUERY_AUTH_FAILURE >&2; echo query >> queries; exit 2;;",
+    " 'pr merge') touch merged;;",
+    ' *) echo "unexpected gh: $*" >&2; exit 98;;', "esac", "",
+  ].join("\n"), 0o755);
+  const env = { PATH: bin + ":/usr/bin:/bin", SKIP_CI_WAIT: "0" };
+  check("M007-003 脏工作区不得推送", () => {
+    const p = exec("bash", [path.join(root, "ops/160/propose-change.sh"), "验证标题"],
+      temp, { ...env, M007_CASE: "dirty" });
+    assert.notEqual(p.status, 0, p.stdout);
+    assert.equal(fs.existsSync(path.join(temp, "pushed")), false);
+  });
+  check("M007-004 查询失败须保留原因并立即退出", () => {
+    const p = exec("bash", [path.join(root, "ops/160/merge-pr.sh")],
+      temp, { ...env, M007_CASE: "query-error" });
+    assert.notEqual(p.status, 0, p.stdout);
+    assert.ok(p.stderr.includes("AUDIT_QUERY_AUTH_FAILURE"), p.stderr);
+    assert.equal(fs.readFileSync(path.join(temp, "queries"), "utf8").trim().split("\n").length, 1);
+    assert.equal(fs.existsSync(path.join(temp, "merged")), false);
+  });
+  assert.deepEqual(failures, [], "M007 行为回归失败：" + failures.join("、"));
+} finally {
+  assert.equal(path.dirname(temp), scratch);
+  assert.ok(path.basename(temp).startsWith("m007-regression-"));
+  fs.rmSync(temp, { recursive: true });
+}
````

---

## 4. 逐块 ID 归因（每个新增/删除行）

Hunk 编号按上述完整 diff 出现顺序。无未归因行。

### 4.1 H1 `ops/160/hooks/pre-push`  `@@ -1,11 +1,11 @@`  index 45966c01..3ae3d237  mode 100755 保留

| 符号 | 行内容（截断以可扫描） | ID |
|---|---|---|
| + | `# 文件版本：V0.01` | M007-001 文件自身版本 |
| - | `while read -r line; do` | M007-001 |
| - | `# 稳健取 remote ref:git 传入「<localref> <localsha> <remoteref> <remotesha>」,` | M007-001 删除误导注释 |
| - | `# remoteref 目标 refs/heads/main 本身无空格;直接整行匹配该目标,避免源 ref 含空格致字段错位漏拦。` | M007-001 删除误导注释 |
| - | `if [[ " $line " == *" refs/heads/main "* ]]; then` | M007-001 去掉整行匹配 |
| + | `while read -r local_ref local_sha remote_ref remote_sha; do` | M007-001 标准四字段 |
| + | `# Git 标准输入四字段,只按第三字段 remote_ref 判断目标分支。` | M007-001 |
| + | `if [[ "$remote_ref" == "refs/heads/main" ]]; then` | M007-001 仅目标 main |

未改：shebang、原三行说明、ALLOW_MAIN_PUSH 分支、两行中文拒绝/覆盖提示、`done`、`exit 0`。numstat 4/4，全部归 001。

### 4.2 H2 `ops/160/install-git-hooks.sh`  `@@ -1,9 +1,10 @@`  index 2362788d..c3dc157d  mode 100755 保留

| 符号 | 行内容 | ID |
|---|---|---|
| + | `# 文件版本：V0.01` | M007-002 文件自身版本 |
| - | `custom="$(git config --get core.hooksPath \|\| true)"` | M007-002 |
| + | `custom="$(git config --path --get core.hooksPath \|\| true)"` | M007-002 Git 原生路径解析 |

未改：职责、`set -euo pipefail`、默认 `git rev-parse --git-common-dir)/hooks`、`install -m 0755`、中文检测句、英文 `installed pre-push hook -> $dest/pre-push`。numstat 2/1，全部归 002。

### 4.3 H3 `ops/160/merge-pr.sh`  `@@ -1,5 +1,5 @@`  index e2952ca8..ef2a97b4  mode 100755 保留

| 符号 | 行内容 | ID |
|---|---|---|
| - | `# 文件版本：V0.01` | M007-004 文件自身版本递增 |
| + | `# 文件版本：V0.02` | M007-004 文件自身版本递增 |

### 4.4 H4 `ops/160/merge-pr.sh`  `@@ -41,7 +41,7 @@`

| 符号 | 行内容 | ID |
|---|---|---|
| - | `run_ids="$(gh run list --commit "$head_sha" --workflow predeploy-gates.yml --limit 10 --json databaseId -q '.[].databaseId' 2>/dev/null \|\| true)"` | M007-004 去掉 stderr 丢弃与成功兜底 |
| + | `run_ids="$(gh run list --commit "$head_sha" --workflow predeploy-gates.yml --limit 10 --json databaseId -q '.[].databaseId' )"` | M007-004 |

未改：`set -euo pipefail`、脏工作区检查、HEAD/远端一致性、`SKIP_CI_WAIT` 整块、30 次空列表重试、`gh run watch`、`gh run view ... >&2 \|\| true`、等待期间 SHA 前移保护、`gh pr merge ... --match-head-commit`。numstat 2/2，全部归 004。

### 4.5 H5 `ops/160/propose-change.sh`  `@@ -1,4 +1,5 @@`  index eb7198de..35a5139e  mode 100755 保留

| 符号 | 行内容 | ID |
|---|---|---|
| + | `# 文件版本：V0.01` | M007-003 文件自身版本 |

### 4.6 H6 `ops/160/propose-change.sh`  `@@ -8,6 +9,11 @@`

| 符号 | 行内容 | ID |
|---|---|---|
| + | `if [[ -n "$(git status --porcelain)" ]]; then` | M007-003 |
| + | `  echo "[propose] 工作区有未提交改动;请先提交或 stash,再运行本地检查并推送。" >&2` | M007-003 |
| + | `  exit 1` | M007-003 |
| + | `fi` | M007-003 |
| + | （空行） | M007-003 与 npm 调用的分隔 |

插入位置在 `[[ "$branch" == "main" ]]` 之后、`npm run verify:predeploy` 之前。未改：推送、`gh pr view/create --base main`、结尾说明。numstat 6/0，全部归 003。

### 4.7 H7 新文件 `tools/gate-hardening-tests/m007-regression.mjs`  `@@ -0,0 +1,102 @@`  mode 100644  blob 6fc4720e

B2 允许「分别归因最小可失败测试」。无新依赖（仅 `node:assert/strict`、`node:fs`、`node:path`、`node:url`、`node:child_process`）。`package.json` / `package-lock.json` 两树 blob 不变。

| 行号 | 内容角色 | ID |
|---|---|---|
| 1 | `// 文件版本：V0.01` | 001–004 测试文件自身版本 |
| 2 | 范围注释 M007-001～004，禁止真实发布 | 001–004 |
| 3–7 | 标准库 import | 001–004 公共测试辅助 |
| 8 | 空行 | 001–004 公共测试辅助 |
| 9–13 | root / scratch / temp / failures | 001–004 公共测试辅助 |
| 14–18 | `exec` | 001–004 公共测试辅助 |
| 19–23 | `put` | 001–004 公共测试辅助 |
| 24–27 | `check` | 001–004 公共测试辅助 |
| 28 | `try {` | 001–004 公共测试辅助 |
| 29–39 | 001 四组 stdin：main→feature 放行、feature→main 拦、ALLOW_MAIN_PUSH、mainish | M007-001 |
| 40 | 空行 | M007-001/002 分隔，公共辅助 |
| 41–59 | 真实 Git + HOME，断言 `~/custom-hooks` 装到 Git 使用的 HOME 路径 | M007-002 |
| 60 | 空行 | 002/003 分隔，公共辅助 |
| 61 | 003/004 共用窄进程边界注释 | M007-003 与 M007-004 |
| 62–72 | git stub（含 dirty porcelain / 禁止真 push） | M007-003 与 M007-004 公共测试辅助 |
| 73 | npm stub `exit 0` | M007-003（验证前边界，避免真门禁） |
| 74 | sleep stub | M007-004（避免真 10s×30） |
| 75–81 | gh stub：run list 失败留 `AUDIT_QUERY_AUTH_FAILURE`，禁止真 merge | M007-003 与 M007-004 公共测试辅助 |
| 82 | PATH / SKIP_CI_WAIT=0 | M007-003 与 M007-004 公共测试辅助 |
| 83–87 | 脏工作区不得推送 | M007-003 |
| 88–95 | 查询失败保留原因、queries 恰 1 次、未 merge | M007-004 |
| 96 | 汇总 failures 必须空 | 001–004 公共测试辅助 |
| 97–102 | finally 校验 scratch 前缀并删除临时目录 | 001–004 公共测试辅助 |

102 行无剩余。未把测试工程化为新框架，未改 M008 其它文件。

### 4.8 范围归因合计

| 文件 | 插入 | 删除 | ID |
|---|---:|---:|---|
| ops/160/hooks/pre-push | 4 | 4 | 001 |
| ops/160/install-git-hooks.sh | 2 | 1 | 002 |
| ops/160/merge-pr.sh | 2 | 2 | 004 |
| ops/160/propose-change.sh | 6 | 0 | 003 |
| tools/gate-hardening-tests/m007-regression.mjs | 102 | 0 | 001–004 |
| **合计** | **116** | **7** | 全部落入定稿 001–004 |

两树 name-only 仅上述 5 路径。以下路径 before=after blob：三业务 HTML、criteria-lock 及其 sidecar、`.github/workflows/predeploy-gates.yml`、`ops/ci/predeploy-gates.yml`（内容 SHA256 均为 `4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d`）、`package.json`、`package-lock.json`。真实 `.git/hooks/pre-push` blob 仍为 before `45966c01610c92bb624b921b8333314f08238fed`（997 bytes），不等于 after `3ae3d237...`。未重装生效 hook，未声称生产发布完成。

范围归因：PASS。

---

## 5. 逐项复验

证据类型：可复现命令输出 + 当前纳入范围源码。本席当场执行，不以 ledger B3 段或历史输出代替。

### 5.1 测试可执行且断言有效（GEN-003）

当前工作区五文件 blob = after 树。`node tools/gate-hardening-tests/m007-regression.mjs`：

- exit 0
- `PASS M007-001 main -> feature/a allow=0`
- `PASS M007-001 feature/a -> main allow=0`
- `PASS M007-001 feature/a -> main allow=1`
- `PASS M007-001 feature/a -> mainish allow=0`
- `PASS M007-002 Git 生效路径`
- `PASS M007-003 脏工作区不得推送`
- `PASS M007-004 查询失败须保留原因并立即退出`

隔离目录仅放置 after 四脚本 + after 测试，再跑同一文件：exit 0，同上 7 条 PASS。

隔离目录放置 **before 四脚本 + after 测试**（非交付物，仅复证红）：exit 1。四条目标断言全部红，且 001 其余三组仍绿：

- FAIL `M007-001 main -> feature/a allow=0`：旧整行匹配误拦，stderr 为拒绝直推 main，`1 !== 0`
- PASS feature→main 拦截 / ALLOW_MAIN_PUSH / mainish
- FAIL `M007-002`：`安装成功但 Git 的 HOME/custom-hooks/pre-push 不存在`（装进了仓内字面 `~/`）
- FAIL `M007-003`：旧入口跑完 npm 与 stub push，status 0，stdout 含「PR 已存在」
- FAIL `M007-004`：stderr 为「等了 5 分钟仍没有 … run」，无 `AUDIT_QUERY_AUTH_FAILURE`（错误被丢弃后空列表重试）
- 最终 `AssertionError`：failures 恰为上述四名

同一测试文件先红后绿，断言能失败。`bash -n` 对 after 四脚本均为 0。

### 5.2 M007-001 内容与保留边界

after hook：`while read -r local_ref local_sha remote_ref remote_sha` + `[[ "$remote_ref" == "refs/heads/main" ]]`。独立 stdin（非测试文件矩阵）：

| 输入（第三字段=remote_ref） | ALLOW | after | before | 期望 |
|---|---|---|---|---|
| local main → remote feature | 0 | 0 | 1（缺陷） | 只按目标 |
| feature → main | 0 | 1 | 1 | 拦目标 main |
| feature → main | 1 | 0（覆盖提示） | 0 | 保留 ALLOW_MAIN_PUSH |
| feature → feature | 0 | 0 | — | 普通 feature |
| feature → mainish | 0 | 0 | 0 | 精确相等，非子串 |
| 删除 remote main（local sha 全 0，remote_ref=main） | 0 | 1 | 1 | 删除 main 仍拦 |
| 删除 remote feature | 0 | 0 | — | 非 main 删除放行 |
| tag v1.0 → tag v1.0 | 0 | 0 | 0 | tag 保留 |
| local main → remote tag | 0 | 0 | 1（旧整行误拦） | 目标不是 heads/main |
| main → main | 0 | 1 | 1 | 直推 main 仍拦 |

未加 `set -euo`（M007-011 队列否）。结论：001 修对，指定旧行为保留。

### 5.3 M007-002 内容与路径矩阵

Git 2.43.0。`--path` 为历史 `--type=path`，展开 `~`。独立真实 `git init`（HOME 隔离，`GIT_CONFIG_GLOBAL=/dev/null`，`GIT_CONFIG_NOSYSTEM=1`）：

| 场景 | 结果 |
|---|---|
| 默认无 hooksPath | 装到 `<repo>/.git/hooks/pre-push`，英文消息 `installed pre-push hook -> .git/hooks/pre-push` |
| 相对 `rel-hooks` | 装到 `<repo>/rel-hooks/pre-push`，打印 `core.hooksPath=rel-hooks`（相对未被迫转绝对） |
| 绝对路径 | 装到该绝对目录 |
| after `~/custom-hooks` | 装到 `$HOME/custom-hooks/pre-push`，仓内字面 `~/` 不存在 |
| before `~/custom-hooks` | HOME 目标不存在，仓内 `~/custom-hooks/pre-push` 存在（原缺陷） |
| worktree 共用 | 从 worktree 跑安装，dest = 主仓 `--git-common-dir`/hooks，主仓与 worktree common 路径相同 |

英文消息字面未改。中文检测句仍在，仅 `$custom` 在 tilde 场景变为展开路径（Git 原生解析的结果，不是改文案）。

### 5.4 M007-003 内容与 merge 准入

源码顺序：脏检查下标 488 < `npm run verify:predeploy` 下标 647。独立 stub：npm 会 `touch npm_ran`。

| 脚本 | 脏？ | returncode | npm_ran | pushed |
|---|---|---|---|---|
| after propose | dirty | 1 | 否 | 否 |
| before propose | dirty | 0 | 是 | 是 |
| after propose | clean | 0 | 是 | 是 |

脏拒绝 stderr：`[propose] 工作区有未提交改动;请先提交或 stash,再运行本地检查并推送。` 未调用 npm。干净路径仍跑 npm 再 stub push。

merge 准入未改：`gh pr merge "$branch" "$method" --match-head-commit "$head_sha"` 两树都在。propose 未改 `--base main` / 未改 merge-pr 调用关系。

### 5.5 M007-004 内容与空列表 / skip / run-view

`set -euo pipefail` 下，去掉 `2>/dev/null || true` 后，`gh run list` 非 0 使赋值失败并立即退出，stderr 不再丢弃。

独立 stub（sleep 记次数，非真 sleep）：

| 场景 | returncode | list/query 次数 | sleep | merged | 原因可见 |
|---|---|---|---|---|---|
| after 空列表 exit 0 | 1 | lists=30 | 30 | 否 | 「等了 5 分钟仍没有 … run」 |
| before 空列表 | 1 | 30 | 30 | 否 | 同左（空列表路径保留） |
| after 查询 exit 2 | 2 | queries=1 | 0 | 否 | stderr=`AUDIT_QUERY_AUTH_FAILURE` |
| before 查询 exit 2 | 1 | 30 | 30 | 否 | 无该原因（被丢弃） |
| after SKIP_CI_WAIT=1 | 0 | 无 run list | — | 是 | 原 skip 警告块 |
| after run watch 失败 | 1 | — | — | 否 | 仍调用 `gh run view`；`VIEWERR` 可见；`\|\| true` 未把失败改成成功 |

run list 行仅去掉丢弃/兜底，其余 merge 逻辑未动。

### 5.6 边界未越

- 三 HTML 两树 blob 相同。
- 锁文件两树相同；sidecar 未改。
- CI 双镜像两树相同且彼此相等。
- 真实 `.git/hooks/pre-push` = before，未重装。
- 无新 npm 依赖，无 package 脚本登记本测试（按允许的最小可失败文件，不另立工程）。
- 未跑 `verify:predeploy` 全仓父链、未部署、未对真实 origin 写。
- 本席未改业务源码；红证在 `/tmp/m007-red-repro`。

### 5.7 ponytail-review

测试 102 行含四缺陷最小可失败断言 + 系统边界 stub，属 ponytail 允许的最低自检，不记删除。四脚本改动为字段匹配、`--path`、脏检查、去掉吞错，无新抽象层。

Lean already. Ship.

net: -0 lines possible.

### 5.8 grilling / diagnosing-bugs

质疑 1：官方 003 只断言非 0 且无 pushed，若不在 npm 前拦截、而在 npm 后拦截，测试仍可能绿。源码顺序 + `npm_ran` 探针否决该质疑。

质疑 2：去掉 `|| true` 是否误伤空列表。空列表 stub 仍 30 次，否决。

质疑 3：`--path` 是否破坏相对/绝对/默认。矩阵否决。

质疑 4：四字段 `read` 是否改变删除 main / tag。独立 stdin 否决。

---

## 6. 源码指纹

对象类型：before/after 均为 tree。

| 路径 | before blob | after blob | after 内容 SHA256 |
|---|---|---|---|
| ops/160/hooks/pre-push | 45966c01610c92bb624b921b8333314f08238fed | 3ae3d237d26ba0f5bd937f36c7e8f856864f8b6f | e38ccd33e40065045c24fa8d4bb08d35f519a753ce2520884fee7103e4e77f85 |
| ops/160/install-git-hooks.sh | 2362788d8303e2404dfb159440f5d8801fb2b0ba | c3dc157dbfd23e77c25a071e5bf628ebe289cef9 | 8609b7e20a6fc49cec6ffc90781310e73fb943c00ca7791e564e956166f9a37b |
| ops/160/merge-pr.sh | e2952ca8d4848af74ac3c610eef98844421d062a | ef2a97b4f00bd13b26a01ecb2b74555dd0a38eaa | 3e222657e568b852608eec39bc64efd2d618dce71b93f22864f3a076d68bcf06 |
| ops/160/propose-change.sh | eb7198de8ae620705c0ad4b40e6f672728c547bb | 35a5139e2b176f88a5041cf4bbed4c0249898589 | 88d3abe9a1c1beaecb7ce6645df537be088768620b72827ad293ba74f7000a8b |
| tools/gate-hardening-tests/m007-regression.mjs | （无） | 6fc4720e3e704539e28a9d488b967a28a79548cf | 10e7b07bf603eb2a47d07fa7a9a8f9f022ba4ff8c3687a1f99cd2889af5781d8 |

工作区 `git hash-object` 与 after blob 全等。after 行数：pre-push 17、install 16、merge 73、propose 30、测试 102。

未改路径 blob：HTML `12334b13…` / `c2588ca1…` / `86a71793…`；workflow 与 `ops/ci` 均为 `3857c0f9…`；package.json `0281eaa9…`；package-lock `63ecc802…`。

---

## 7. 所有发现

无新 finding。无未归因行。无越界改 HTML/锁/CI/真实 hook。无把 hook 重装或生产发布写成已完成。

观察（不升 ID、不新判据、不挡本轮 PASS）：官方回归是 B2 允许的最小可失败集——001 未内建删除/tag 矩阵，002 未内建默认/相对/绝对/worktree，003 未断言 npm 未执行，004 未断言空列表 30 次。本席已用独立命令补证这些保留边界，不把「测试未覆盖保留路径」写成缺陷（与定稿 005–011「不另扩策略/不顺手改英文」一致）。

---

## 8. 结论

| 项 | 结论 |
|---|---|
| 内容审计 | PASS |
| 范围归因 | PASS |
| 本席最终 | PASS |
| B4 双 PASS / 模块 PASS | 否，仅席乙本轮 |

001–004 修复与指定边界成立；116/7 全部可归定稿 ID；测试能红能绿且断言打到原缺陷。

---

## 9. 验证限制

- 未跑全仓 `npm run verify:predeploy` 父链，未部署，未对真实 GitHub 做 push/PR/merge，未重装 `.git/hooks`。
- 001/003/004 的 push/PR/gh 边界用进程 stub；002 与 001 stdin 用真实 Git/bash。
- 空列表 30 次用 sleep stub，验证的是循环次数而非墙钟 5 分钟。
- 未核 GitHub Actions 历史 run，未核生产机 hook 生效状态（且本阶段不应把未重装写成完成）。
- 未下载/比较 action 镜像标签对应 commit（M007-006 队列否，且本 diff 未改 CI）。
- `.scratch/` 在运行官方测试前已存在历史目录；测试创建的 `m007-regression-*` 在 finally 删除。本席未把 `.scratch` 纳入修复范围。
- B4 只审 before/after 修复 diff，不是 M007 五文件/79 判据的 B5 全量重审。
- 工作区相对 HEAD 对五文件为未提交改动（B3 用临时索引，主索引未动）；本席按冻结两树审，不以 HEAD 冒充 after。

本报告正文未再次出现独占锁行。
