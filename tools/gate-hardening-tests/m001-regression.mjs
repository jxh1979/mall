// 文件版本：V0.08
// 实际运行治理脚本；输入和模拟合并均在临时目录，绝不操作真实 PR。
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const root = fileURLToPath(new URL("../../", import.meta.url));
const tempParent = fs.realpathSync(os.tmpdir());
const temp = fs.mkdtempSync(path.join(tempParent, "m001-gates-"));
const failures = [];
const put = (file, text) => {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, text, "utf8");
};
const exec = (command, args, cwd, env = {}) => spawnSync(command, args, {
  cwd, encoding: "utf8", env: { ...process.env, HTML_GATES_BASE: "", ...env },
});
const git = (cwd, ...args) => {
  const result = exec("git", args, cwd);
  assert.equal(result.status, 0, result.error?.message || result.stderr);
  return result.stdout.trim();
};
const expectExit = (name, result, expected) => {
  try {
    assert.equal(result.status, expected, result.error?.message || result.stdout + result.stderr);
    console.log(`PASS ${name}`);
  } catch (error) {
    failures.push(name);
    console.error(`FAIL ${name}: ${error.message}`);
  }
};
const run = (file, cwd, args = [], env = {}) => exec(process.execPath, [path.join(root, file), ...args], cwd, env);
const bashPath = (file) => process.platform === "win32"
  ? "/" + file.replaceAll("\\", "/").replace(":", "") : file;
const html = [
  "docs/business-system-map-for-operations-v0.1.html",
  "docs/platform-governance-map-for-operations-v0.1.html",
  "docs/business-enum-quick-reference-v0.1.html",
];
const ledger = "docs/prd-audit/business-decision-ledger.md";
const baseline = "tools/html-param-tier-baseline.json";

try {
  const gates = path.join(temp, "html");
  for (const file of html) put(path.join(gates, file), "<p>临时验证资料</p>\n");
  put(path.join(gates, ledger), "# 临时验证台账\n");
  put(path.join(gates, baseline), '{"entries":[]}\n');
  git(gates, "init", "-q", "-b", "main");
  git(gates, "config", "user.name", "M001 regression");
  git(gates, "config", "user.email", "m001@example.invalid");
  git(gates, "config", "core.autocrlf", "false");
  git(gates, "config", "core.hooksPath", path.join(temp, "empty-hooks"));
  git(gates, "config", "commit.gpgSign", "false");
  git(gates, "add", ".");
  git(gates, "commit", "-qm", "temporary baseline");
  const before = git(gates, "rev-parse", "HEAD");
  git(gates, "update-ref", "refs/remotes/origin/main", before);
  const ledgerGate = "tools/verify-html-ledger-sync.mjs";
  const paramGate = "tools/verify-html-param-tier.mjs";
  // M008-006：基线是一个参数，干净树上的非法值也必须拒绝。
  for (const file of [ledgerGate, paramGate]) {
    expectExit(`M008-006 参数基线 ${file}`, run(file, gates, ["HEAD;echo M008_BASE_ARG"]), 1);
    expectExit(`M008-006 环境基线 ${file}`, run(file, gates, [], { HTML_GATES_BASE: "HEAD;echo M008_BASE_ARG" }), 1);
  }
  put(path.join(gates, html[0]), "<p>临时期限 30 天。</p>\n");
  for (const file of [ledgerGate, paramGate]) expectExit(`未提交错误 ${file}`, run(file, gates), 1);
  git(gates, "add", ".");
  git(gates, "commit", "-qm", "temporary first change");
  for (const file of [ledgerGate, paramGate]) expectExit(`PR 错误 ${file}`, run(file, gates), 1);
  put(path.join(gates, "unrelated.txt"), "第二个临时提交\n");
  git(gates, "add", ".");
  git(gates, "commit", "-qm", "temporary second change");
  git(gates, "update-ref", "refs/remotes/origin/main", git(gates, "rev-parse", "HEAD"));
  for (const file of [ledgerGate, paramGate]) {
    expectExit(`push 多提交 ${file}`, run(file, gates, [], { HTML_GATES_BASE: before }), 1);
    expectExit(`明确基线 ${file}`, run(file, gates, [before]), 1);
    expectExit(`缺失基线 ${file}`, run(file, gates, [], { HTML_GATES_BASE: "absent-ref" }), 1);
  }
  for (const [name, body, expected] of [
    ["空豁免", "临时期限 30 天 <!-- param-tier:ignore -->", 1],
    ["部分豁免", "临时期限 30 天，每期 3 次 <!-- param-tier:ignore 30 天 -->", 1],
    ["明确豁免", "临时期限 30 天，每期 3 次 <!-- param-tier:ignore 30 天;3 次 -->", 0],
    ["否定词", "临时期限 30 天，该天数不可配。", 1],
    ["法定层级", "临时期限 30 天（法定值，不可配）。", 0],
    ["业务层级", "临时期限 30 天（业务参数，按项目可配）。", 0],
    // M008-004：段落内换行与独立段落不得改变层级判断。
    ["M008-004 换行标注", "临时期限 30 天\n（业务参数，按项目可配）。", 0],
    ["M008-004 同行两段", "临时期限 30 天（业务参数，按项目可配）。</p><p>另一事项每期 3 次。", 1],
    ["M008-004 段后豁免", "临时期限 30 天。</p><!-- param-tier:ignore 30 天 --><p>其它说明", 0],
    ["M008-004 豁免不扩到后段", "临时期限 30 天。</p><!-- param-tier:ignore 30 天 --><p>另一事项每期 3 次。", 1],
  ]) {
    put(path.join(gates, html[0]), `<p>${body}</p>\n`);
    expectExit(name, run(paramGate, gates, [before]), expected);
  }
  put(path.join(gates, ledger), "# 临时验证台账\n已记录本次变更。\n");
  expectExit("台账已同步", run(ledgerGate, gates, [before]), 0);
  // M008-003：工作区删除和已提交删除都不是台账同步。
  fs.unlinkSync(path.join(gates, ledger));
  expectExit("M008-003 删除台账", run(ledgerGate, gates, [before]), 1);
  git(gates, "add", ".");
  git(gates, "commit", "-qm", "temporary deleted ledger");
  put(path.join(gates, ledger), "# 临时验证台账\n已记录本次变更。\n");
  expectExit("M008-003 已提交删除不被未跟踪副本掩盖", run(ledgerGate, gates, [before]), 1);
  put(path.join(gates, html[0]), "<p>临时验证资料</p>\n");
  put(path.join(gates, baseline), '{"entries":[{"key":"new-key","file":"temporary","text":"temporary"}]}\n');
  expectExit("基线新增 key", run(paramGate, gates, [before]), 1);
  put(path.join(gates, baseline), '{"entries":[]}\n');
  expectExit("基线保持不变", run(paramGate, gates, [before]), 0);
  git(gates, "add", ".");
  git(gates, "commit", "-qm", "temporary deployment baseline");
  put(path.join(gates, html[0]), "<p>临时期限 60 天。</p>\n");
  git(gates, "add", ".");
  git(gates, "commit", "-qm", "temporary deployment change");
  git(gates, "update-ref", "refs/remotes/origin/main", git(gates, "rev-parse", "HEAD"));
  for (const deploy of ["ops/160/complex-deploy-api.sh", "ops/160/complex-deploy-web.sh"]) {
    const assignment = fs.readFileSync(path.join(root, deploy), "utf8").match(/^export HTML_GATES_BASE=.*$/m)?.[0] ?? ":";
    for (const file of [ledgerGate, paramGate]) {
      expectExit(`部署主线比较 ${deploy} ${file}`, exec("bash", ["--noprofile", "--norc", "-c",
        assignment + '\nexec "$1" "$2"', "m001", bashPath(process.execPath), bashPath(path.join(root, file))], gates), 1);
    }
  }

  const csv = path.join(temp, "csv");
  for (const file of html) put(path.join(csv, file), "<p>临时条目正文,后续内容</p>\n");
  const csvInputs = {
    "docs/prd-audit/entry-registry.csv": 'html_file,entry_id,entry_name,entry_body,line_now\nbusiness-system-map-for-operations-v0.1.html,T001,临时条目,"临时条目正文,\n后续内容",1\n',
    "docs/prd-audit/authority-rule-ledger.csv": "rule_id,html_file,entry_name,source_text\nR001,business-system-map-for-operations-v0.1.html,临时条目,临时条目正文\n",
    "docs/prd-audit/html-prd-coverage.csv": "rule_id,entry_name\nR001,临时条目\n",
  };
  for (const [file, text] of Object.entries(csvInputs)) put(path.join(csv, file), text);
  const anchors = "tools/verify-entry-anchors.mjs";
  expectExit("CSV 引号逗号及跨行字段", run(anchors, csv), 0);
  // M008-002：整条正文必填，不能以空字符串跳过验证。
  const registryFile = "docs/prd-audit/entry-registry.csv";
  put(path.join(csv, registryFile), csvInputs[registryFile].replace('"临时条目正文,\n后续内容"', '""'));
  expectExit("M008-002 空条目正文", run(anchors, csv), 1);
  put(path.join(csv, registryFile), csvInputs[registryFile]);
  for (const [file, text] of Object.entries(csvInputs)) {
    put(path.join(csv, file), text + "损坏记录,多一列,多一列,多一列,多一列,多一列\n");
    expectExit(`CSV 列数错误 ${file}`, run(anchors, csv), 1);
    put(path.join(csv, file), text + '损坏记录,"未闭合引号');
    expectExit(`CSV 未闭合引号 ${file}`, run(anchors, csv), 1);
    put(path.join(csv, file), "\ufeff" + text.replaceAll("\n", "\r\n"));
    expectExit(`CSV BOM/CRLF ${file}`, run(anchors, csv), 0);
    put(path.join(csv, file), text);
  }

  const delivery = path.join(temp, "delivery");
  git(temp, "clone", "--quiet", "--no-hardlinks", "-c", "core.autocrlf=false", "-c", "core.longpaths=true", root, delivery);
  const workflow = ".github/workflows/predeploy-gates.yml";
  const mirror = "ops/ci/predeploy-gates.yml";
  const validWorkflow = fs.readFileSync(path.join(root, workflow), "utf8");
  for (const file of [workflow, mirror]) put(path.join(delivery, file), validWorkflow);
  const deployScripts = ["ops/160/complex-deploy-api.sh", "ops/160/complex-deploy-web.sh"];
  for (const file of deployScripts) put(path.join(delivery, file), fs.readFileSync(path.join(root, file), "utf8"));
  const deliveryGate = "tools/verify-delivery-gates.mjs";
  expectExit("一致的 CI 定义", run(deliveryGate, delivery), 0);
  put(path.join(delivery, mirror), validWorkflow.replace("fetch-depth: 0", "fetch-depth: 1"));
  expectExit("CI 镜像分叉", run(deliveryGate, delivery), 1);
  put(path.join(delivery, workflow), validWorkflow.replace("fetch-depth: 0", "fetch-depth: 1"));
  expectExit("两份 CI 都缺完整历史", run(deliveryGate, delivery), 1);
  for (const file of [workflow, mirror]) put(path.join(delivery, file), validWorkflow);
  for (const [name, broken] of [
    ["CI 删除事件基线", validWorkflow.replace(/^.*HTML_GATES_BASE:.*\n/gm, "")],
    ["CI 注释不能代替完整历史", validWorkflow.replace("fetch-depth: 0", "fetch-depth: 1 # was fetch-depth: 0")],
    ["CI 未执行预部署检查", validWorkflow.replace("run: npm run verify:predeploy", "run: echo skipped")],
  ]) {
    for (const file of [workflow, mirror]) put(path.join(delivery, file), broken);
    expectExit(name, run(deliveryGate, delivery), 1);
  }
  for (const file of [workflow, mirror]) put(path.join(delivery, file), validWorkflow);
  for (const file of deployScripts) {
    const validScript = fs.readFileSync(path.join(root, file), "utf8");
    for (const gate of ["verify:html-ledger-sync", "verify:entry-anchors", "verify:html-param-tier"]) {
      put(path.join(delivery, file), validScript.replace(gate, ""));
      expectExit(`部署缺少 ${gate} ${file}`, run(deliveryGate, delivery), 1);
    }
    put(path.join(delivery, file), validScript);
  }
  const pkg = JSON.parse(fs.readFileSync(path.join(delivery, "package.json"), "utf8"));
  // M008-001：实际运行检查不能被打印文字、注释或相似脚本名替代。
  const originalScripts = { ...pkg.scripts };
  for (const [script, gate] of [["verify:predeploy", "verify:html-param-tier"], ["verify:delivery-gates", "verify:unit"]]) {
    for (const [name, value, expected = 1] of [
      ["打印", originalScripts[script].replace(`npm run ${gate}`, `echo ${gate}`)],
      ["相似名", originalScripts[script].replace(`npm run ${gate}`, `npm run ${gate}-extra`)],
      ["整串打印", `echo ${JSON.stringify(originalScripts[script])}`],
      ["注释", `echo skipped # ${originalScripts[script]}`],
      ["命令内换行", originalScripts[script].replace(`npm run ${gate}`, `npm\nrun ${gate}`)],
      ["&& 前换行", originalScripts[script].replace(" && ", "\n&& ")],
      ["&& 后续行", originalScripts[script].replaceAll(" && ", " &&\n"), 0],
    ]) {
      pkg.scripts = { ...originalScripts, [script]: value };
      put(path.join(delivery, "package.json"), JSON.stringify(pkg));
      expectExit(`M008-001 ${script} ${name}`, run(deliveryGate, delivery), expected);
    }
  }
  pkg.scripts = originalScripts;
  pkg.scripts["verify:predeploy"] = "true";
  put(path.join(delivery, "package.json"), JSON.stringify(pkg));
  expectExit("检查入口缺少 HTML 门禁", run(deliveryGate, delivery), 1);

  const merge = path.join(temp, "merge");
  const bin = path.join(merge, "bin");
  put(path.join(bin, "git"), `#!/usr/bin/env bash
set -eu
case "$*" in
 'rev-parse --abbrev-ref HEAD') echo feature/m001;;
 'rev-parse HEAD') echo aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;;
 'status --porcelain') :;;
 'ls-remote origin refs/heads/feature/m001')
  if [[ -f advanced ]]; then value=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb; else value=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa; fi
  printf '%s\\trefs/heads/feature/m001\\n' "$value";;
 *) echo "unexpected fake git: $*" >&2; exit 97;;
esac
`);
  put(path.join(bin, "gh"), `#!/usr/bin/env bash
set -eu
case "$1 $2" in
 'pr view')
  if [[ "$*" == *headRefOid* ]]; then if [[ -f advanced ]]; then echo bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb; else echo aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa; fi; fi;;
 'run list')
  if [[ "$*" == *workflowName* ]]; then printf 'predeploy-gates 101\\nbackend-tests 101\\nweb-tests 101\\n'; else echo 101; fi;;
 'run watch') if [[ "$M001_MODE" == watch ]]; then touch advanced; fi;;
 'pr merge')
  if [[ "$M001_MODE" == late ]]; then touch advanced; fi
  expected=''
  while [[ $# -gt 0 ]]; do if [[ "$1" == --match-head-commit ]]; then shift; expected="$1"; fi; shift; done
  if [[ -f advanced && "$expected" != bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb && -n "$expected" ]]; then exit 1; fi
  touch merged;;
 *) echo "unexpected fake gh: $*" >&2; exit 98;;
esac
exit 0
`);
  for (const file of ["git", "gh"]) fs.chmodSync(path.join(bin, file), 0o755);
  for (const [mode, expected] of [["stable", 0], ["watch", 1], ["late", 1]]) {
    for (const file of ["advanced", "merged"]) {
      const target = path.join(merge, file);
      if (fs.existsSync(target)) fs.unlinkSync(target);
    }
    const result = exec("bash", ["--noprofile", "--norc", "-c",
      'export PATH="$1:/usr/bin:/bin"; exec bash "$2"', "m001",
      bashPath(bin), bashPath(path.join(root, "ops/160/merge-pr.sh"))], merge, { M001_MODE: mode });
    expectExit(`合并提交 ${mode}`, result, expected);
    if (fs.existsSync(path.join(merge, "merged")) !== (expected === 0)) failures.push(`合并副作用 ${mode}`);
  }
  // M008-007：使用当前活跃句式，检查实际回填内容并保留历史句。
  const refresh = path.join(temp, "refresh");
  const entryFile = "docs/00-current-effective-document-entry-v0.1.md";
  const history = "历史记录：V385 / 1606 tests，保留当时事实。";
  put(path.join(refresh, entryFile), "# 临时入口\n本轮 2026-09-01 实测 OpenAPI 720 operations、Flyway 最新 V385；当前工作树无 `backend/target/surefire-reports`，故不声明当前测试数量。\n本轮 2026-09-01 取证为 Flyway 最新 V385、OpenAPI 720 operations；当前工作树无后端 surefire 报告，不声明当前测试数量。\n" + history + "\n");
  put(path.join(refresh, "README.md"), "<!-- baseline:auto-begin -->\n<!-- baseline:auto-end -->\n");
  put(path.join(refresh, "docs/openapi/openapi-p0.json"), JSON.stringify({ paths: { "/sample": { get: {}, post: {} } } }));
  put(path.join(refresh, "backend/src/main/resources/db/migration/V12__sample.sql"), "select 1;\n");
  const reportFile = path.join(refresh, "backend/target/surefire-reports/sample.xml");
  put(reportFile, '<testsuite tests="3"></testsuite>\n');
  const generator = "tools/refresh-entry-doc-baseline.mjs";
  expectExit("M008-007 回填快照", run(generator, refresh), 0);
  const refreshed = fs.readFileSync(path.join(refresh, entryFile), "utf8");
  assert.match(refreshed, /实测 OpenAPI 2 operations、Flyway 最新 V12；后端 surefire 现有报告 3 tests。/);
  assert.match(refreshed, /取证为 Flyway 最新 V12、OpenAPI 2 operations；后端 surefire 现有报告 3 tests。/);
  assert.ok(refreshed.includes(history), "M008-007 历史记录应保留");
  expectExit("M008-007 回填后校验", run(generator, refresh, ["--check"]), 0);
  fs.unlinkSync(reportFile);
  put(path.join(refresh, entryFile), refreshed.replaceAll("2 operations", "99 operations"));
  expectExit("M008-007 无报告仍回填计数", run(generator, refresh), 0);
  assert.match(fs.readFileSync(path.join(refresh, entryFile), "utf8"), /实测 OpenAPI 2 operations、Flyway 最新 V12；后端 surefire 现有报告 3 tests。/);
  assert.match(fs.readFileSync(path.join(refresh, entryFile), "utf8"), /取证为 Flyway 最新 V12、OpenAPI 2 operations；后端 surefire 现有报告 3 tests。/);
  assert.deepEqual(failures, [], `治理回归失败：${failures.join("、")}`);
  console.log("M001_GOVERNANCE_REGRESSION_PASS");
} finally {
  const resolved = fs.realpathSync(temp);
  assert.equal(path.dirname(resolved), tempParent);
  assert.ok(path.basename(resolved).startsWith("m001-gates-"));
  fs.rmSync(resolved, { recursive: true });
}
