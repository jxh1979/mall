// 文件版本：V0.01
// 统一发布包构建器（ROOT-06；BSM:1973「最终要提供一个能自动检测运行环境的安装包」、BSM:1974「安装包里附带一份简单的使用教程」）。
// 产出 <out>/complex-<版本>-<提交>/ 及同名 .tar.gz：安装向导、U 盘首次制钥、keyvault 脚本、服务端与网页产物、按环境的员工 App、
// 教程、manifest.json 与 SHA256SUMS。环境识别在安装时做（插了授权 U 盘即生产，第十三批操作员裁决第 6 条），出包只决定包支持哪些环境。
// 用法：node tools/release/build-release-package.mjs [--environments 登记表] [--out 目录] [--artifacts-from 已构建产物目录（仅测试用）]
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath, pathToFileURL } from "node:url";
import { parseArgs } from "node:util";

const root = fileURLToPath(new URL("../../", import.meta.url));
const LABEL = { test: "测试环境", prod: "生产环境" };
// 出包必需的登记字段（其余键一律不带进包）。
const REQUIRED = { public_host: "对外地址", api_base_url: "员工 App 服务地址", allowed_hosts: "员工 App 允许访问地址", tls_cert: "证书路径", tls_key: "证书私钥路径" };
// 与 ops/160/complex-deploy-api.sh REQUIRED_GATES 同源（照抄数组，不读 package.json 的 verify:predeploy）。
const REQUIRED_GATES = [
  "verify:html-ledger-sync", "verify:entry-anchors", "verify:html-param-tier",
  "verify:delivery-gates-docs", "verify:full-admin-matrix", "verify:domain-ownership-scope",
  "verify:matrix-vs-migration", "verify:data-isolation-spec", "verify:tenant-isolation",
  "verify:p0-contract", "verify:security-baseline", "verify:production-readiness-evidence",
  "verify:production-blockers", "verify:pii-encryption", "verify:test-db-isolation",
];
const SIGNING_VARS = ["COMPLEX_ANDROID_SIGNING_STORE_FILE", "COMPLEX_ANDROID_SIGNING_STORE_PASSWORD", "COMPLEX_ANDROID_SIGNING_KEY_ALIAS", "COMPLEX_ANDROID_SIGNING_KEY_PASSWORD"];

/** 某环境登记齐且员工 App 已产出才进包；否则 excluded 写中文原因。staffApks：{env: 文件名}。 */
export function decideSupportedEnvironments(registry, staffApks) {
  const supported = [];
  const excluded = {};
  for (const e of registry) {
    const missing = Object.entries(REQUIRED).filter(([k]) => !String(e[k] ?? "").trim()).map(([, name]) => name);
    if (missing.length) excluded[e.env] = `登记未齐：缺${missing.join("、")}`;
    else if (!staffApks[e.env]) excluded[e.env] = e.env === "prod" ? "缺员工 App 正式签名" : "缺员工 App 安装包";
    else supported.push(e.env);
  }
  return { supported, excluded };
}

const fail = (message) => {
  console.error(`出包停止：${message}`);
  process.exit(1);
};
const run = (cmd, args, env = {}) => {
  const r = spawnSync(cmd, args, { cwd: root, stdio: "inherit", env: { ...process.env, ...env } });
  if (r.status !== 0) fail(`${cmd} ${args.join(" ")} 失败`);
};
const git = (...args) => spawnSync("git", args, { cwd: root, encoding: "utf8" }).stdout.trim();
const sha256 = (file) => crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
const shellQuote = (v) => `'${String(v).replace(/'/g, "'\\''")}'`;
const html = (v) => String(v).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
const copy = (from, to) => fs.cpSync(from, to, { recursive: true });

/** 从源码构建：门禁 → 后端 → 网页 → 每个登记齐的环境各一份员工 App。返回与 --artifacts-from 相同布局的目录。 */
function buildFromSource(registry, outDir) {
  if (git("status", "--porcelain")) fail("工作区有未提交改动，安装包必须对应一个提交");
  for (const gate of REQUIRED_GATES) run("npm", ["run", gate], { HTML_GATES_BASE: process.env.HTML_GATES_BASE ?? "HEAD~1" });
  run("node", ["tools/run-backend-maven.mjs", "clean", "package"]);
  run("npm", ["run", "build", "--workspace", "apps/web"]);
  const art = path.join(outDir, ".artifacts");
  fs.rmSync(art, { recursive: true, force: true });
  const jar = fs.readdirSync(path.join(root, "backend/target")).find((f) => f.endsWith(".jar") && !f.endsWith(".original"));
  if (!jar) fail("backend/target 下找不到后端 jar");
  copy(path.join(root, "backend/target", jar), path.join(art, "server/complex-api.jar"));
  copy(path.join(root, "apps/web/dist"), path.join(art, "server/web"));
  const signed = SIGNING_VARS.every((k) => process.env[k]);
  for (const e of registry) {
    if (Object.keys(REQUIRED).some((k) => !String(e[k] ?? "").trim())) continue;
    if (e.env === "prod" && !signed) continue; // 没有正式签名不出生产员工 App，decideSupportedEnvironments 记原因
    const variant = e.env === "prod" ? "release" : "debug";
    run("node", ["tools/run-android-gradle.mjs", "--project", "apps/android-terminal/android-app", "--require-android-sdk",
      `:app:assemble${variant === "release" ? "Release" : "Debug"}`],
    { COMPLEX_ANDROID_API_BASE_URL: e.api_base_url, COMPLEX_ANDROID_API_ALLOWED_HOSTS: e.allowed_hosts });
    copy(path.join(root, `apps/android-terminal/android-app/app/build/outputs/apk/${variant}/app-${variant}.apk`),
      path.join(art, `clients/staff-android/complex-terminal-${e.env}.apk`));
  }
  return art;
}

function guideEntries(registry, supported) {
  const rows = registry.filter((e) => supported.includes(e.env)).map((e) => {
    const entryOf = (v) => (v ? html(v) : "尚未发布");
    return `<tr><td>${LABEL[e.env]}</td><td><code>https://${html(e.public_host)}/</code></td>`
      + `<td><code>https://${html(e.public_host)}/complex-terminal.apk</code></td><td>尚未发布</td>`
      + `<td>${entryOf(e.guest_entries?.ios)}</td><td>${entryOf(e.guest_entries?.wechat)}</td></tr>`;
  });
  return `<table><tr><th>环境</th><th>后台网址</th><th>员工手机 App 下载</th><th>客人安卓 App</th><th>客人 iPhone App</th><th>微信小程序</th></tr>${rows.join("")}</table>`;
}

function main() {
  const { values } = parseArgs({
    options: {
      environments: { type: "string", default: path.join(root, "ops/release/environments.json") },
      out: { type: "string", default: path.join(root, "dist/release") },
      "artifacts-from": { type: "string" },
    },
  });
  values.out = path.resolve(values.out);
  const registry = JSON.parse(fs.readFileSync(values.environments, "utf8")).environments;
  fs.mkdirSync(values.out, { recursive: true });
  const art = values["artifacts-from"] ?? buildFromSource(registry, values.out);

  const staffApks = Object.fromEntries(registry
    .map((e) => [e.env, `complex-terminal-${e.env}.apk`])
    .filter(([, f]) => fs.existsSync(path.join(art, "clients/staff-android", f))));
  const { supported, excluded } = decideSupportedEnvironments(registry, staffApks);
  if (!supported.length) {
    for (const [env, reason] of Object.entries(excluded)) console.error(`${LABEL[env] ?? env}：${reason}`);
    fail("没有可安装的环境");
  }

  const version = JSON.parse(fs.readFileSync(path.join(root, "package.json"), "utf8")).version;
  const commit = git("rev-parse", "--short", "HEAD");
  const builtAt = new Date().toISOString();
  const name = `complex-${version}-${commit}`;
  const pkg = path.join(values.out, name);
  fs.rmSync(pkg, { recursive: true, force: true });
  fs.rmSync(`${pkg}.tar.gz`, { force: true });

  for (const f of ["安装.sh", "U盘首次制钥.sh"]) {
    copy(path.join(root, "ops/release", f), path.join(pkg, f));
    fs.chmodSync(path.join(pkg, f), 0o755);
  }
  copy(path.join(root, "ops/keyvault"), path.join(pkg, "keyvault"));
  for (const f of ["nginx-complex.conf.tmpl", "complex-api.service.tmpl"]) copy(path.join(root, "ops/release", f), path.join(pkg, "config", f));
  copy(path.join(art, "server/complex-api.jar"), path.join(pkg, "server/complex-api.jar"));
  copy(path.join(art, "server/web"), path.join(pkg, "server/web"));
  for (const env of supported) {
    copy(path.join(art, "clients/staff-android", staffApks[env]), path.join(pkg, "clients/staff-android", staffApks[env]));
  }

  // 安装器读的配置：只含白名单字段（对外地址、证书路径），不带登记表其他键。
  const lines = [
    `PACKAGE_VERSION=${shellQuote(`${version}-${commit}`)}`,
    `PACKAGE_BUILT_AT=${shellQuote(builtAt)}`,
    `SUPPORTED_ENVS=${shellQuote(supported.join(" "))}`,
    ...Object.entries(excluded).map(([env, reason]) => `EXCLUDED_${env}=${shellQuote(reason)}`),
    ...registry.filter((e) => supported.includes(e.env)).flatMap((e) => [
      `${e.env}_PUBLIC_HOST=${shellQuote(e.public_host)}`,
      `${e.env}_TLS_CERT=${shellQuote(e.tls_cert)}`,
      `${e.env}_TLS_KEY=${shellQuote(e.tls_key)}`,
    ]),
  ];
  fs.writeFileSync(path.join(pkg, "config/install.env"), `${lines.join("\n")}\n`);

  const guide = fs.readFileSync(path.join(root, "ops/release/guide/使用教程.html"), "utf8")
    .replaceAll("@版本@", html(`${version}-${commit}`))
    .replaceAll("@各端入口@", guideEntries(registry, supported));
  fs.mkdirSync(path.join(pkg, "教程"), { recursive: true });
  fs.writeFileSync(path.join(pkg, "教程/使用教程.html"), guide);

  const migrationDir = path.join(root, "backend/src/main/resources/db/migration");
  const migrations = fs.readdirSync(migrationDir).filter((f) => f.endsWith(".sql"))
    .sort((a, b) => Number(a.match(/^V(\d+)/)?.[1] ?? 0) - Number(b.match(/^V(\d+)/)?.[1] ?? 0) || a.localeCompare(b))
    .map((file) => ({ file, sha256: sha256(path.join(migrationDir, file)) }));

  const walk = (dir) => fs.readdirSync(dir, { withFileTypes: true })
    .flatMap((d) => (d.isDirectory() ? walk(path.join(dir, d.name)) : [path.join(dir, d.name)]));
  const files = walk(pkg).map((abs) => path.relative(pkg, abs)).sort()
    .map((rel) => ({ path: rel, sha256: sha256(path.join(pkg, rel)), bytes: fs.statSync(path.join(pkg, rel)).size }));
  const perEnv = (pick) => Object.fromEntries(supported.map((env) => [env, pick(registry.find((e) => e.env === env)) ?? null]));
  const manifest = {
    product: "综合体管理系统",
    version,
    git_commit: commit,
    built_at: builtAt,
    environments_supported: supported,
    excluded,
    files,
    migrations,
    clients: {
      staff_android: perEnv((e) => staffApks[e.env]),
      guest_android: perEnv(() => null), // 客人安卓 App 工程尚未产出（ROOT-03），产出后按员工 App 同法构建
      guest_ios: perEnv((e) => e.guest_entries?.ios),
      guest_wechat: perEnv((e) => e.guest_entries?.wechat),
    },
  };
  fs.writeFileSync(path.join(pkg, "manifest.json"), `${JSON.stringify(manifest, null, 2)}\n`);
  const sums = walk(pkg).map((abs) => path.relative(pkg, abs)).filter((rel) => rel !== "SHA256SUMS").sort()
    .map((rel) => `${sha256(path.join(pkg, rel))}  ${rel}`);
  fs.writeFileSync(path.join(pkg, "SHA256SUMS"), `${sums.join("\n")}\n`);
  run("tar", ["-czf", `${pkg}.tar.gz`, "-C", values.out, name]);

  for (const [env, reason] of Object.entries(excluded)) console.log(`本包不支持${LABEL[env] ?? env}：${reason}`);
  console.log(`安装包已生成：${pkg}.tar.gz（支持：${supported.map((env) => LABEL[env]).join("、")}）`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) main();
