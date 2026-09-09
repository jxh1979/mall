CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M007 B4-r1 Codex 修复diff独立复审
版本：V0.01

内容审计：PASS。范围归因：PASS。最终本席结论：PASS。
阶段只为B4，不是模块PASS。当前471计划M007，B3累计1；findings V1.08定稿队列001–004是、005–011否。Grok本次真实exit0后，本方才重新读取完整diff、执行独立验证、写本报告；未读本轮Grok raw。

before：ac86ec95b63a0816b050be4f90c38abccc1fdbb0
after：9f63094e21e4ed94a29b29071af5bd07cf99fd6a
完整git diff --binary两树：9770bytes SHA0f6450cf482a1990d56ddde4ab004521ef52d98759a946c1c77b1521dd6c6265；5文件116插入7删除。两树包含tracked/staged/untracked全路径，不用HEAD提交diff或路径过滤。主索引前后ae1dfbf11bec123e6a662097d16de9bb7088fe8bc50d1e9ae4a78da24e90022c。

## 全部差异归因
| 差异块 | 定稿ID | 归因与核验 |
|---|---|---|
| hooks/pre-push唯一hunk，版本头/四字段读取/第三字段比较/删除空格假设注释 | M007-001 | 只纠正localref被误当目标；所有增删行均服务于该ID。原ALLOW_MAIN_PUSH和目标main拒绝不动。 |
| install-git-hooks唯一hunk，版本头及--path | M007-002 | Git原生路径展开；默认else和相对/绝对职责不改。新增版本是本文件修改的版本要求。 |
| merge-pr版本hunk与query hunk | M007-004 | V0.01→V0.02；只删除丢stderr/成功兜底，使set-e传播gh失败。保留空列表循环、末段SHA、skip、run-view容错。括号前余空格不改语义，不为格式另修。 |
| propose-change版本hunk与clean guard hunk | M007-003 | 在npm前拒绝工作树不同于提交，中文提示说明原流程前提；push/PR创建逻辑未改。 |
| 新m007-regression.mjs L1–28及L97–102 | M007-001/002/003/004 | 元数据、stdlib导入、隔离目录、执行/写fixture/断言收集及安全清理，为四个定稿行为提供必要公共验证，无依赖/业务抽象。 |
| 新测试L29–40 | M007-001 | 真实hook四字段stdin，合法main源→feature目的放行，main目标拦/覆盖/近似名保留。 |
| 新测试L42–59 | M007-002 | 真实Git隔离HOME与安装产物；不是对源码字符串作镜像断言。 |
| 新测试L61–81 | M007-003/004 | 只替换外部git/gh/npm/sleep进程边界，防真实push/PR；原propose/merge脚本仍实际执行。 |
| 新测试L82–88 | M007-003 | dirty时必须非0且没有push副作用。 |
| 新测试L89–96 | M007-004 | 查询exit2原错误仍可见、只查一次、无merge副作用。 |

逐文件/逐hunk/每一新增删除行均有定稿ID归因；102行测试全覆盖归因。无业务HTML、锁、CI、仅登记项或其它业务路径改动。新增测试属提示词B3明确允许的归因验证；不是新增审计产物类型。

## 修复内容复审
001：逐字段read合法Git输入，remote_ref=第三字段。fresh测试main源→feature为0、feature→main为1、ALLOW=1为0、mainish为0；额外删除main为1、tag main为0、ALLOW=true仍1，保留要求成立。无新增业务权限。
002：--path解析Git pathname，在fresh真实Git default/relative/absolute/tilde四模式全部安装到Git期望位置。worktree-common的原else代码未改；本方B4未另建worktree，不能冒称本方在本轮执行过该模式。当前真实.git/hooks未重新安装，源码修复与运行时复制不是同一步，不冒称生效副本已更新。
003：guard处于npm前；fresh dirty exit1、无push/PR。clean-create/existing保持0，npm红不推、push失败不开PR，main仍拒绝。此前实际Git/npm错配证据对应的入口现在在检查前就拒绝脏树。
004：assignment位于set-e作用域普通for体，无||true；fresh专门测试gh查询失败exit2，原因可见、一次查询、未merge。成功空列表仍原30次重试后1，红CI与两种SHA前移仍拒绝；正常两run及显式skip保持成功。
测试有效性：本轮重新用after测试对before四脚本运行，四个原问题全部重现FAIL；同一after测试对当前源码全部PASS。不是只拿B3记录声称通过。当前五个变更文件与after树逐字节相同。

## 限制与判据
只用冻结GEN-002/003/004/007/008/009/011/013/016/017/018和已核三HTML分类边界；全部改动技术性，不发明业务。按先前实读requesting/receiving-code-review、grilling、diagnosing-bugs、ponytail-review、verification-before-completion收窄规则复审。B4不修复、不新增finding判据、无第三主体。
未执行真实GitHub写入、业务仓push/merge/commit、生产部署、真实hook覆盖、数据库或全仓父链；这些不是本次diff行为验证。无新finding。下一步仅在本方完整raw落盘后读取Grok，同意票须双PASS才出队。


## 完整无过滤binary diff
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

## 本轮独立before红/当前绿及after一致性
````python
from pathlib import Path
import subprocess,hashlib,tempfile,re,os
r=Path('/home/james/complex-management');d=r/'docs/audit-2026-09'
before='ac86ec95b63a0816b050be4f90c38abccc1fdbb0';after='9f63094e21e4ed94a29b29071af5bd07cf99fd6a'
print('CHECK_ACTUAL_LOCK_SHA',hashlib.sha256((d/'plan/criteria-lock.md').read_bytes()).hexdigest())
delta=subprocess.check_output(['git','diff','--binary',before,after],cwd=r)
print('DIFF_SHA',len(delta),hashlib.sha256(delta).hexdigest())
print('FULL_DIFF_BEGIN');print(delta.decode());print('FULL_DIFF_END')
fs=['ops/160/hooks/pre-push','ops/160/install-git-hooks.sh','ops/160/propose-change.sh','ops/160/merge-pr.sh','tools/gate-hardening-tests/m007-regression.mjs']
for rel in fs:
 obj=subprocess.check_output(['git','show',after+':'+rel],cwd=r)
 actual=(r/rel).read_bytes();assert obj==actual,rel
 print('AFTER_SOURCE',rel,len(actual),hashlib.sha256(actual).hexdigest())
p=subprocess.run(['node','tools/gate-hardening-tests/m007-regression.mjs'],cwd=r,text=True,capture_output=True)
print('FRESH_GREEN_EXIT',p.returncode);print(p.stdout,p.stderr);assert p.returncode==0
with tempfile.TemporaryDirectory(prefix='m007-b4-before-',dir=r/'.scratch') as td:
 t=Path(td)
 for rel in fs:
  dst=t/rel;dst.parent.mkdir(parents=True,exist_ok=True)
  rev=after if rel.endswith('m007-regression.mjs') else before
  dst.write_bytes(subprocess.check_output(['git','show',rev+':'+rel],cwd=r))
 q=subprocess.run(['node',str(t/'tools/gate-hardening-tests/m007-regression.mjs')],cwd=t,text=True,capture_output=True)
 print('FRESH_BEFORE_RED_EXIT',q.returncode);print(q.stdout,q.stderr)
 assert q.returncode!=0
 for token in ['M007-001 main -> feature/a','M007-002 Git','M007-003 脏','M007-004 查询']:assert 'FAIL '+token in q.stderr,token
print('INDEPENDENT_RED_GREEN_PROVED')
print('MAIN_INDEX_SHA',hashlib.sha256((r/'.git/index').read_bytes()).hexdigest())

````
````text
AFTER_SOURCE ops/160/hooks/pre-push 922 e38ccd33e40065045c24fa8d4bb08d35f519a753ce2520884fee7103e4e77f85
AFTER_SOURCE ops/160/install-git-hooks.sh 707 8609b7e20a6fc49cec6ffc90781310e73fb943c00ca7791e564e956166f9a37b
AFTER_SOURCE ops/160/propose-change.sh 1626 88d3abe9a1c1beaecb7ce6645df537be088768620b72827ad293ba74f7000a8b
AFTER_SOURCE ops/160/merge-pr.sh 4180 3e222657e568b852608eec39bc64efd2d618dce71b93f22864f3a076d68bcf06
AFTER_SOURCE tools/gate-hardening-tests/m007-regression.mjs 5239 10e7b07bf603eb2a47d07fa7a9a8f9f022ba4ff8c3687a1f99cd2889af5781d8
FRESH_GREEN_EXIT 0
PASS M007-001 main -> feature/a allow=0
PASS M007-001 feature/a -> main allow=0
PASS M007-001 feature/a -> main allow=1
PASS M007-001 feature/a -> mainish allow=0
PASS M007-002 Git 生效路径
PASS M007-003 脏工作区不得推送
PASS M007-004 查询失败须保留原因并立即退出
 
FRESH_BEFORE_RED_EXIT 1
PASS M007-001 feature/a -> main allow=0
PASS M007-001 feature/a -> main allow=1
PASS M007-001 feature/a -> mainish allow=0
 FAIL M007-001 main -> feature/a allow=0: [pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。
[pre-push] 确需直推:ALLOW_MAIN_PUSH=1 git push ...(并在 PR/说明记录原因)。


1 !== 0

FAIL M007-002 Git 生效路径: 安装成功但 Git 的 HOME/custom-hooks/pre-push 不存在
FAIL M007-003 脏工作区不得推送: [propose] 本地跑 predeploy 门禁(红则不推)...
[propose] 推 feature 分支 feature/m007 ...
[propose] PR 已存在,已更新:
[propose] 下一步:①独立复核(codex/评审)②合并一律走 ops/160/merge-pr.sh
[propose]   merge-pr.sh 会先等 GitHub Actions predeploy-gates 对本 commit 变绿再 gh pr merge;
[propose]   本仓是私有库免费版设不了 required status check,别再手敲 gh pr merge 绕过这道检查。

FAIL M007-004 查询失败须保留原因并立即退出: [merge] 等了 5 分钟仍没有 aaaaaaaa 的 predeploy-gates run,中止合并。
        可能原因:workflow 被禁用、push 的不是这个 commit、或 Actions 排队异常。
        CI 确实故障时可用 SKIP_CI_WAIT=1 强行合并(会打印警告并需在 PR 说明原因)。

node:internal/modules/run_main:107
    triggerUncaughtException(
    ^

AssertionError [ERR_ASSERTION]: M007 行为回归失败：M007-001 main -> feature/a allow=0、M007-002 Git 生效路径、M007-003 脏工作区不得推送、M007-004 查询失败须保留原因并立即退出
+ actual - expected

+ [
+   'M007-001 main -> feature/a allow=0',
+   'M007-002 Git 生效路径',
+   'M007-003 脏工作区不得推送',
+   'M007-004 查询失败须保留原因并立即退出'
+ ]
- []

    at file:///home/james/complex-management/.scratch/m007-b4-before-jbdjjrc1/tools/gate-hardening-tests/m007-regression.mjs:97:10
    at ModuleJob.run (node:internal/modules/esm/module_job:561:25)
    at async node:internal/modules/esm/loader:647:26
    at async asyncRunEntryPointWithESMLoader (node:internal/modules/run_main:101:5) {
  generatedMessage: false,
  code: 'ERR_ASSERTION',
  actual: [
    'M007-001 main -> feature/a allow=0',
    'M007-002 Git 生效路径',
    'M007-003 脏工作区不得推送',
    'M007-004 查询失败须保留原因并立即退出'
  ],
  expected: [],
  operator: 'deepStrictEqual',
  diff: 'simple'
}

Node.js v24.20.0

INDEPENDENT_RED_GREEN_PROVED
MAIN_INDEX_SHA ae1dfbf11bec123e6a662097d16de9bb7088fe8bc50d1e9ae4a78da24e90022c

````

## 本轮相关既有行为保持
````python
from pathlib import Path
import os,subprocess,tempfile,shutil,json
r=Path('/home/james/complex-management');scratch=r/'.scratch';scratch.mkdir(exist_ok=True)
fake='''#!/usr/bin/python3
import sys,os,json
from pathlib import Path
name=Path(sys.argv[0]).name;a=sys.argv[1:];mode=os.environ["CASE"];root=Path(os.environ["CASE_ROOT"]);sha="a"*40
with (root/"calls").open("a") as f:f.write(json.dumps([name]+a)+"\\n")
def stop(n=0,s=""):
 if s: print(s)
 sys.exit(n)
if name=="sleep":stop()
if name=="npm":stop(1 if mode=="red-local" else 0)
if name=="git":
 if a==["rev-parse","--abbrev-ref","HEAD"]:stop(s="main" if mode=="main" else "feature/audit")
 if a==["status","--porcelain"]:stop(s=" M pending.txt" if mode=="dirty" else "")
 if a==["rev-parse","HEAD"]:stop(s=sha)
 if a[:2]==["ls-remote","origin"]:
  if mode=="no-remote":stop()
  stop(s=("b"*40 if mode=="mismatch" or (root/"advanced").exists() else sha)+"\\trefs/heads/feature/audit")
 if a and a[0]=="push":stop(1 if mode=="push-fail" else 0)
if name=="gh":
 if a[:2]==["pr","view"]:
  if mode=="no-pr" or mode in ["clean-create","red-local","push-fail","dirty"]:stop(1)
  stop(s=("b"*40 if (root/"advanced").exists() else sha) if "headRefOid" in a else ("https://example.invalid/pr" if "url" in a else ""))
 if a[:2]==["run","list"]:stop(s="" if mode=="no-runs" else "101\\n102")
 if a[:2]==["run","watch"]:
  if mode=="advance":(root/"advanced").touch()
  stop(1 if mode=="red-ci" else 0)
 if a[:2]==["run","view"]:stop(s="https://example.invalid/run")
 if a[:2]==["pr","merge"]:
  assert "--match-head-commit" in a and a[a.index("--match-head-commit")+1]==sha
  if mode=="late":stop(1)
  (root/"merged").touch();stop()
 if a[:2]==["pr","create"]:(root/"created").touch();stop()
stop(97,"UNEXPECTED "+repr([name]+a))
'''
with tempfile.TemporaryDirectory(prefix='m007-b1-',dir=scratch) as td:
 t=Path(td);bin=t/'bin';bin.mkdir()
 for n in ['git','gh','npm','sleep']:
  p=bin/n;p.write_text(fake);p.chmod(0o755)
 for mode,expected in [('stable',0),('invalid',1),('main',1),('dirty',1),('no-remote',1),('mismatch',1),('no-pr',1),('no-runs',1),('red-ci',1),('advance',1),('late',1),('skip',0)]:
  c=t/('merge-'+mode);c.mkdir()
  env={**os.environ,'PATH':str(bin)+':/usr/bin:/bin','CASE':mode,'CASE_ROOT':str(c),'SKIP_CI_WAIT':'1' if mode=='skip' else '0'}
  p=subprocess.run(['bash',str(r/'ops/160/merge-pr.sh')]+(['--bad'] if mode=='invalid' else []),cwd=c,env=env,text=True,capture_output=True)
  calls=[json.loads(x) for x in (c/'calls').read_text().splitlines()] if (c/'calls').exists() else []
  merged=(c/'merged').exists();assert p.returncode==expected and merged==(expected==0),(mode,p.returncode,p.stderr)
  print('MERGE',mode,'exit',p.returncode,'merged',merged,'calls',len(calls),'watch',sum(x[:3]==['gh','run','watch'] for x in calls))
  if mode in ['red-ci','advance','late','skip']:print('MESSAGE',p.stderr.strip())
 for mode,expected in [('clean-create',0),('existing',0),('red-local',1),('push-fail',1),('main',1),('dirty',1)]:
  c=t/('propose-'+mode);c.mkdir()
  env={**os.environ,'PATH':str(bin)+':/usr/bin:/bin','CASE':mode,'CASE_ROOT':str(c)}
  p=subprocess.run(['bash',str(r/'ops/160/propose-change.sh'),'审计标题','审计正文'],cwd=c,env=env,text=True,capture_output=True)
  calls=[json.loads(x) for x in (c/'calls').read_text().splitlines()]
  assert p.returncode==expected,(mode,p.returncode,p.stderr)
  push=any(x[:2]==['git','push'] for x in calls);create=(c/'created').exists()
  if mode in ['red-local','main']:assert not push and not create
  if mode=='push-fail':assert not create
  print('PROPOSE',mode,'exit',p.returncode,'push',push,'create',create,'CALLS',json.dumps(calls,ensure_ascii=False))
 for label,local,remote,allow,expected in [('feature','feature/a','feature/a',False,0),('to-main','feature/a','main',False,1),('main-to-feature','main','feature/a',False,0),('allow','feature/a','main',True,0)]:
  line=f'refs/heads/{local} {"a"*40} refs/heads/{remote} {"b"*40}\n'
  p=subprocess.run(['bash',str(r/'ops/160/hooks/pre-push')],cwd=r,input=line,env={**os.environ,'ALLOW_MAIN_PUSH':'1' if allow else '0'},text=True,capture_output=True)
  assert p.returncode==expected
  print('HOOK',label,'exit',p.returncode,'INPUT',line.strip(),'MESSAGE',p.stderr.strip())
 for mode in ['default','relative','absolute','tilde']:
  repo=t/('install-'+mode);(repo/'ops/160/hooks').mkdir(parents=True)
  for src in ['ops/160/install-git-hooks.sh','ops/160/hooks/pre-push']:shutil.copyfile(r/src,repo/src)
  home=t/('home-'+mode);home.mkdir()
  env={**os.environ,'HOME':str(home),'GIT_CONFIG_GLOBAL':'/dev/null','GIT_CONFIG_NOSYSTEM':'1'}
  def g(*args):return subprocess.run(['/usr/bin/git',*args],cwd=repo,env=env,text=True,capture_output=True,check=True).stdout.strip()
  g('-c','init.defaultBranch=main','init','-q')
  custom={'relative':'.custom-hooks','absolute':str(repo/'absolute-hooks'),'tilde':'~/custom-hooks'}.get(mode)
  if custom:g('config','core.hooksPath',custom)
  target=Path(g('rev-parse','--git-path','hooks/pre-push'))
  if not target.is_absolute():target=repo/target
  p=subprocess.run(['bash','ops/160/install-git-hooks.sh'],cwd=repo,env=env,text=True,capture_output=True)
  assert p.returncode==0
  active=target.exists();assert active,(mode,target)
  print('INSTALL',mode,'exit',p.returncode,'git_expected',str(target.relative_to(t)),'effective_exists',active,'stdout',p.stdout.strip())
print('HARNESS_FINISHED: source copies unchanged; all expected observed outcomes asserted; all current expected behaviors verified after the four fixes.')

for label,line,allow,want in [
 ('delete-main','(delete) '+'0'*40+' refs/heads/main '+'a'*40,'0',1),
 ('tag-main','refs/tags/main '+'a'*40+' refs/tags/main '+'b'*40,'0',0),
 ('nonliteral-allow','refs/heads/a '+'a'*40+' refs/heads/main '+'b'*40,'true',1)]:
 p=subprocess.run(['bash',str(r/'ops/160/hooks/pre-push')],cwd=r,input=line+'\n',env={**os.environ,'ALLOW_MAIN_PUSH':allow},text=True,capture_output=True)
 print('HOOK_PRESERVED',label,p.returncode);assert p.returncode==want

````
````text
MERGE stable exit 0 merged True calls 11 watch 2
MERGE invalid exit 1 merged False calls 0 watch 0
MERGE main exit 1 merged False calls 1 watch 0
MERGE dirty exit 1 merged False calls 3 watch 0
MERGE no-remote exit 1 merged False calls 4 watch 0
MERGE mismatch exit 1 merged False calls 4 watch 0
MERGE no-pr exit 1 merged False calls 5 watch 0
MERGE no-runs exit 1 merged False calls 65 watch 0
MERGE red-ci exit 1 merged False calls 8 watch 1
MESSAGE [merge] CI predeploy-gates 未通过(run 101),中止合并。
https://example.invalid/run
MERGE advance exit 1 merged False calls 10 watch 2
MESSAGE [merge] 分支在等待 CI 期间被更新:已验 aaaaaaaa,远端 bbbbbbbb,PR head bbbbbbbb。中止,请对新 commit 重跑本脚本。
MERGE late exit 1 merged False calls 11 watch 2
MESSAGE 
MERGE skip exit 0 merged True calls 8 watch 0
MESSAGE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
!! SKIP_CI_WAIT=1:跳过 CI 绿灯检查,直接合并 aaaaaaaa 到 main !!
!! 治理门禁(predeploy-gates)本次未被确认通过。请在 PR 里写明原因。   !!
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
PROPOSE clean-create exit 0 push True create True CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["git", "status", "--porcelain"], ["npm", "run", "verify:predeploy"], ["git", "push", "-u", "origin", "feature/audit"], ["gh", "pr", "view", "feature/audit"], ["gh", "pr", "create", "--base", "main", "--head", "feature/audit", "--title", "审计标题", "--body", "审计正文"]]
PROPOSE existing exit 0 push True create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["git", "status", "--porcelain"], ["npm", "run", "verify:predeploy"], ["git", "push", "-u", "origin", "feature/audit"], ["gh", "pr", "view", "feature/audit"], ["gh", "pr", "view", "feature/audit", "--json", "url", "-q", ".url"]]
PROPOSE red-local exit 1 push False create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["git", "status", "--porcelain"], ["npm", "run", "verify:predeploy"]]
PROPOSE push-fail exit 1 push True create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["git", "status", "--porcelain"], ["npm", "run", "verify:predeploy"], ["git", "push", "-u", "origin", "feature/audit"]]
PROPOSE main exit 1 push False create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"]]
PROPOSE dirty exit 1 push False create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["git", "status", "--porcelain"]]
HOOK feature exit 0 INPUT refs/heads/feature/a aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa refs/heads/feature/a bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb MESSAGE 
HOOK to-main exit 1 INPUT refs/heads/feature/a aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa refs/heads/main bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb MESSAGE [pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。
[pre-push] 确需直推:ALLOW_MAIN_PUSH=1 git push ...(并在 PR/说明记录原因)。
HOOK main-to-feature exit 0 INPUT refs/heads/main aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa refs/heads/feature/a bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb MESSAGE 
HOOK allow exit 0 INPUT refs/heads/feature/a aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa refs/heads/main bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb MESSAGE [pre-push] ALLOW_MAIN_PUSH=1 覆盖:放行直推 main(已留痕提醒)。
INSTALL default exit 0 git_expected install-default/.git/hooks/pre-push effective_exists True stdout installed pre-push hook -> .git/hooks/pre-push
INSTALL relative exit 0 git_expected install-relative/.custom-hooks/pre-push effective_exists True stdout [install-hooks] 检测到 core.hooksPath=.custom-hooks,安装到该路径(否则钩子不生效)。
installed pre-push hook -> .custom-hooks/pre-push
INSTALL absolute exit 0 git_expected install-absolute/absolute-hooks/pre-push effective_exists True stdout [install-hooks] 检测到 core.hooksPath=/home/james/complex-management/.scratch/m007-b1-pufdyqx2/install-absolute/absolute-hooks,安装到该路径(否则钩子不生效)。
installed pre-push hook -> /home/james/complex-management/.scratch/m007-b1-pufdyqx2/install-absolute/absolute-hooks/pre-push
INSTALL tilde exit 0 git_expected home-tilde/custom-hooks/pre-push effective_exists True stdout [install-hooks] 检测到 core.hooksPath=/home/james/complex-management/.scratch/m007-b1-pufdyqx2/home-tilde/custom-hooks,安装到该路径(否则钩子不生效)。
installed pre-push hook -> /home/james/complex-management/.scratch/m007-b1-pufdyqx2/home-tilde/custom-hooks/pre-push
HARNESS_FINISHED: source copies unchanged; all expected observed outcomes asserted; all current expected behaviors verified after the four fixes.
HOOK_PRESERVED delete-main 1
HOOK_PRESERVED tag-main 0
HOOK_PRESERVED nonliteral-allow 1

````

最终本席：内容PASS、范围归因PASS、B4-r1 PASS。
