// 文件版本：V0.01
// M007-001～004：执行真实脚本；只替换 GitHub/推送边界，禁止真实发布。
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const root = fileURLToPath(new URL("../../", import.meta.url));
const scratch = path.join(root, ".scratch");
fs.mkdirSync(scratch, { recursive: true });
const temp = fs.mkdtempSync(path.join(scratch, "m007-regression-"));
const failures = [];
const exec = (cmd, args, cwd, env = {}, input) => {
  const result = spawnSync(cmd, args, { cwd, input, encoding: "utf8", env: { ...process.env, ...env } });
  assert.equal(result.error, undefined);
  return result;
};
const put = (file, text, mode) => {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, text, "utf8");
  if (mode) fs.chmodSync(file, mode);
};
const check = (name, fn) => {
  try { fn(); console.log("PASS " + name); }
  catch (error) { failures.push(name); console.error("FAIL " + name + ": " + error.message); }
};
try {
  // M007-001：目的分支才决定是否拦截；源 main、目标 feature 应放行。
  for (const [local, remote, allow, want] of [
    ["main", "feature/a", "0", 0], ["feature/a", "main", "0", 1],
    ["feature/a", "main", "1", 0], ["feature/a", "mainish", "0", 0],
  ]) check("M007-001 " + local + " -> " + remote + " allow=" + allow, () => {
    const input = "refs/heads/" + local + " " + "a".repeat(40) +
      " refs/heads/" + remote + " " + "b".repeat(40) + "\n";
    const p = exec("bash", [path.join(root, "ops/160/hooks/pre-push")],
      root, { ALLOW_MAIN_PUSH: allow }, input);
    assert.equal(p.status, want, p.stderr);
  });

  // M007-002：真实 Git 解析 ~/ 路径；验证安装产物落到 Git 使用的位置。
  const installRepo = path.join(temp, "install");
  const home = path.join(temp, "home");
  fs.mkdirSync(home);
  for (const rel of ["ops/160/install-git-hooks.sh", "ops/160/hooks/pre-push"])
    put(path.join(installRepo, rel), fs.readFileSync(path.join(root, rel), "utf8"));
  const installEnv = { HOME: home, GIT_CONFIG_GLOBAL: "/dev/null", GIT_CONFIG_NOSYSTEM: "1" };
  check("M007-002 Git 生效路径", () => {
    for (const args of [["init", "-q"], ["config", "core.hooksPath", "~/custom-hooks"]]) {
      const p = exec("git", args, installRepo, installEnv);
      assert.equal(p.status, 0, p.stderr);
    }
    const p = exec("bash", ["ops/160/install-git-hooks.sh"], installRepo, installEnv);
    assert.equal(p.status, 0, p.stderr);
    const installed = path.join(home, "custom-hooks/pre-push");
    assert.ok(fs.existsSync(installed), "安装成功但 Git 的 HOME/custom-hooks/pre-push 不存在");
    assert.deepEqual(fs.readFileSync(installed), fs.readFileSync(path.join(root, "ops/160/hooks/pre-push")));
    assert.ok(fs.statSync(installed).mode & 0o111);
  });

  // M007-003/004：复用一个窄的进程边界；未知命令失败，真实源码不替换。
  const bin = path.join(temp, "bin");
  put(path.join(bin, "git"), [
    "#!/usr/bin/env bash", "set -eu",
    'case "$*" in',
    " 'rev-parse --abbrev-ref HEAD') echo feature/m007;;",
    " 'status --porcelain') if [[ $M007_CASE == dirty ]]; then echo ' M pending.txt'; fi;;",
    " 'rev-parse HEAD') echo aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;;",
    " 'ls-remote origin refs/heads/feature/m007') printf 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\\trefs/heads/feature/m007\\n';;",
    " 'push -u origin feature/m007') touch pushed;;",
    ' *) echo "unexpected git: $*" >&2; exit 97;;', "esac", "",
  ].join("\n"), 0o755);
  put(path.join(bin, "npm"), "#!/usr/bin/env bash\nexit 0\n", 0o755);
  put(path.join(bin, "sleep"), "#!/usr/bin/env bash\nexit 0\n", 0o755);
  put(path.join(bin, "gh"), [
    "#!/usr/bin/env bash", "set -eu", 'case "$1 $2" in',
    " 'pr view') :;;",
    " 'run list') echo AUDIT_QUERY_AUTH_FAILURE >&2; echo query >> queries; exit 2;;",
    " 'pr merge') touch merged;;",
    ' *) echo "unexpected gh: $*" >&2; exit 98;;', "esac", "",
  ].join("\n"), 0o755);
  const env = { PATH: bin + ":/usr/bin:/bin", SKIP_CI_WAIT: "0" };
  check("M007-003 脏工作区不得推送", () => {
    const p = exec("bash", [path.join(root, "ops/160/propose-change.sh"), "验证标题"],
      temp, { ...env, M007_CASE: "dirty" });
    assert.notEqual(p.status, 0, p.stdout);
    assert.equal(fs.existsSync(path.join(temp, "pushed")), false);
  });
  check("M007-004 查询失败须保留原因并立即退出", () => {
    const p = exec("bash", [path.join(root, "ops/160/merge-pr.sh")],
      temp, { ...env, M007_CASE: "query-error" });
    assert.notEqual(p.status, 0, p.stdout);
    assert.ok(p.stderr.includes("AUDIT_QUERY_AUTH_FAILURE"), p.stderr);
    assert.equal(fs.readFileSync(path.join(temp, "queries"), "utf8").trim().split("\n").length, 1);
    assert.equal(fs.existsSync(path.join(temp, "merged")), false);
  });
  assert.deepEqual(failures, [], "M007 行为回归失败：" + failures.join("、"));
} finally {
  assert.equal(path.dirname(temp), scratch);
  assert.ok(path.basename(temp).startsWith("m007-regression-"));
  fs.rmSync(temp, { recursive: true });
}
