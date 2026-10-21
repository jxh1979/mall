// 文件版本：V0.01
// ROOT-06 R6：统一发布包构建器与安装向导的沙箱演练（BSM「能自动识别环境的安装包 + 内附教程」条，BSM:1973、1974、1966、1967）。
// 只在临时目录里跑：COMPLEX_INSTALL_ROOT 指向沙箱根，systemctl/nginx/psql 等系统命令全部换成 PATH 前置的假命令；
// 不碰本机系统、不需数据库与 Maven。环境识别按第十三批操作员裁决第 6 条：插了授权 U 盘即生产，否则测试。
import { test, after } from "node:test";
import assert from "node:assert/strict";
import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath, pathToFileURL } from "node:url";

const repo = fileURLToPath(new URL("../../", import.meta.url));
const builder = path.join(repo, "tools/release/build-release-package.mjs");
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), "release-package-"));
after(() => fs.rmSync(tmp, { recursive: true, force: true }));

const put = (file, text, mode) => {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, text);
  if (mode) fs.chmodSync(file, mode);
};
const read = (file) => fs.readFileSync(file, "utf8");
const sha = (file) => crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
const walk = (dir) => fs.readdirSync(dir, { withFileTypes: true })
  .flatMap((d) => (d.isDirectory() ? walk(path.join(dir, d.name)) : [path.join(dir, d.name)]));

// 假产物：两个环境的员工 App 内容不同，便于核对装上的是哪一个。
const art = path.join(tmp, "art");
put(`${art}/server/complex-api.jar`, "fake-jar");
put(`${art}/server/web/index.html`, "<html>后台</html>");
put(`${art}/clients/staff-android/complex-terminal-test.apk`, "apk-for-test");
put(`${art}/clients/staff-android/complex-terminal-prod.apk`, "apk-for-prod");

const SECRET_NOTE = "do-not-ship-0001";
const entry = (env, host) => ({
  env,
  label: env === "prod" ? "生产环境" : "测试环境",
  public_host: host,
  api_base_url: `https://${host}`,
  allowed_hosts: host,
  tls_cert: `/etc/ssl/complex/${env}.crt`,
  tls_key: `/etc/ssl/complex/${env}.key`,
  guest_entries: { ios: null, wechat: null },
  note_password: SECRET_NOTE,
});

let builds = 0;
function build(mutate = (list) => list) {
  const name = `b${++builds}`;
  const reg = path.join(tmp, `${name}.json`);
  put(reg, JSON.stringify({ environments: mutate([entry("test", "test.example.internal"), entry("prod", "prod.example.internal")]) }));
  const out = path.join(tmp, `out-${name}`);
  const r = spawnSync(process.execPath, [builder, "--environments", reg, "--out", out, "--artifacts-from", art], { encoding: "utf8" });
  const dirs = fs.existsSync(out) ? fs.readdirSync(out).filter((d) => fs.statSync(path.join(out, d)).isDirectory()) : [];
  return { status: r.status, out: r.stdout + r.stderr, pkg: dirs.length === 1 ? path.join(out, dirs[0]) : null };
}

// 假系统命令：记录参数；curl 按 SHIM_HEALTH 返回健康与否；id 永远答 0（当作 sudo 运行）。
const shims = path.join(tmp, "shims");
const shim = (name, body = "") => put(path.join(shims, name), `#!/bin/sh\necho "${name} $*" >> "$SHIM_LOG"\n${body}\nexit 0\n`, 0o755);
shim("id", "echo 0");
for (const name of ["systemctl", "nginx", "useradd", "chown", "journalctl", "mount", "umount", "sleep", "apt-get"]) shim(name);
shim("lsblk", '[ -n "$SHIM_LSBLK" ] && printf "%s\\n" "$SHIM_LSBLK"');
shim("runuser", "cat >/dev/null");
shim("psql", "cat >/dev/null");
shim("java", "echo 'openjdk version \"21.0.4\" 2024-07-16' >&2");
shim("curl", '[ "$SHIM_HEALTH" = fail ] && exit 7');

let roots = 0;
function newRoot() {
  const root = path.join(tmp, `root${++roots}`);
  for (const env of ["test", "prod"]) {
    put(`${root}/etc/ssl/complex/${env}.crt`, "cert");
    put(`${root}/etc/ssl/complex/${env}.key`, "key");
  }
  put(`${root}/etc/nginx/sites-enabled/default`, "default site");
  return root;
}
const USB_PASS = "usb-pass-nonsecret-0001";
function plugUsb(root, at = "media/op/KEY") {
  const usb = `${root}/${at}`;
  fs.mkdirSync(usb, { recursive: true });
  const r = spawnSync("bash", [path.join(repo, "ops/keyvault/generate-keystore.sh"), usb], {
    env: { ...process.env, COMPLEX_KEYVAULT_PASS: USB_PASS }, encoding: "utf8",
  });
  assert.equal(r.status, 0, r.stdout + r.stderr);
  return usb;
}
function run(script, root, extra = {}) {
  const env = {
    PATH: `${shims}:${process.env.PATH}`, HOME: tmp, COMPLEX_INSTALL_ROOT: root,
    SHIM_LOG: path.join(tmp, `shim-${path.basename(root)}.log`), SUDO_USER: "zhangsan", SHIM_HEALTH: "ok", ...extra,
  };
  const r = spawnSync("bash", [script], { env, encoding: "utf8" });
  return { status: r.status, out: r.stdout + r.stderr };
}
const exists = (p) => fs.existsSync(p);
const currentOf = (root, part) => fs.readlinkSync(`${root}/opt/complex/${part}/current`);
// 先断言前提成立，未实现时以断言失败报红而不是路径错误。
const inPkg = (pkg, name) => { assert.ok(pkg, "构建器未产出安装包"); return path.join(pkg, name); };
const installedAt = (root) => assert.ok(exists(`${root}/etc/complex/install-state.env`), "前一用例未装好");

const full = build();

test("构建器：包内容、哈希、迁移清单、登记表白名单与教程", () => {
  assert.equal(full.status, 0, full.out);
  const pkg = full.pkg;
  for (const f of ["manifest.json", "SHA256SUMS", "安装.sh", "U盘首次制钥.sh", "keyvault/lib.sh", "keyvault/generate-keystore.sh",
    "keyvault/provision-from-usb.sh", "keyvault/verify-provisioned.sh", "keyvault/keys.list", "config/install.env",
    "config/nginx-complex.conf.tmpl", "config/complex-api.service.tmpl", "server/complex-api.jar", "server/web/index.html",
    "clients/staff-android/complex-terminal-test.apk", "clients/staff-android/complex-terminal-prod.apk", "教程/使用教程.html"]) {
    assert.ok(exists(path.join(pkg, f)), `包内缺 ${f}`);
  }
  assert.ok(exists(`${pkg}.tar.gz`), "缺同名压缩包");
  const manifest = JSON.parse(read(path.join(pkg, "manifest.json")));
  assert.deepEqual(manifest.environments_supported, ["test", "prod"]);
  for (const f of manifest.files) assert.equal(sha(path.join(pkg, f.path)), f.sha256, f.path);
  const sqlCount = fs.readdirSync(path.join(repo, "backend/src/main/resources/db/migration")).filter((f) => f.endsWith(".sql")).length;
  assert.equal(manifest.migrations.length, sqlCount);
  assert.deepEqual(manifest.clients.staff_android, { test: "complex-terminal-test.apk", prod: "complex-terminal-prod.apk" });
  assert.deepEqual(manifest.clients.guest_ios, { test: null, prod: null });
  assert.equal(spawnSync("sha256sum", ["-c", "SHA256SUMS", "--quiet"], { cwd: pkg }).status, 0);
  for (const file of walk(pkg)) assert.ok(!read(file).includes(SECRET_NOTE), `登记表多余键被带进包：${file}`);

  const guide = read(path.join(pkg, "教程/使用教程.html"));
  assert.doesNotMatch(guide, /(src|href)\s*=\s*["']?https?:|url\(\s*["']?https?:|@import/i, "教程不得引用外链资源");
  assert.doesNotMatch(guide, /@[^@\s<>"']+@/, "教程还有未替换的占位");
  assert.ok(guide.includes("test.example.internal") && guide.includes("prod.example.internal"), "教程未渲染各环境地址");
  assert.ok(guide.includes("尚未发布"), "客人端入口缺失时应写尚未发布");
  for (const script of ["安装.sh", "U盘首次制钥.sh"]) {
    const messages = [...read(path.join(pkg, script)).matchAll(/\bstop "([^"]+)"/g)].map((m) => m[1]);
    assert.ok(messages.length >= 3, `${script} 停止提示过少`);
    for (const message of messages) {
      assert.ok(!message.includes("$"), `停止提示须为固定文案：${message}`);
      assert.ok(guide.includes(message), `教程第六章缺停止提示：${message}`);
    }
  }
});

test("构建器：登记未齐的环境不进包，两项都未齐即停止", () => {
  const one = build((list) => { list[1].tls_cert = ""; return list; });
  assert.equal(one.status, 0, one.out);
  const manifest = JSON.parse(read(inPkg(one.pkg, "manifest.json")));
  assert.deepEqual(manifest.environments_supported, ["test"]);
  assert.match(manifest.excluded.prod, /登记未齐：缺证书路径/);
  assert.ok(!exists(path.join(one.pkg, "clients/staff-android/complex-terminal-prod.apk")));

  const none = build((list) => { list[0].public_host = ""; list[1].tls_key = ""; return list; });
  assert.notEqual(none.status, 0);
  assert.match(none.out, /没有可安装的环境/);
});

test("出包规则：生产登记齐但没有正式签名的员工 App 即不支持生产", async () => {
  const mod = await import(pathToFileURL(builder).href).catch(() => ({}));
  assert.equal(typeof mod.decideSupportedEnvironments, "function", "构建器须导出 decideSupportedEnvironments");
  const registry = [entry("test", "t.example"), entry("prod", "p.example")];
  const noProdApk = mod.decideSupportedEnvironments(registry, { test: "complex-terminal-test.apk" });
  assert.deepEqual(noProdApk.supported, ["test"]);
  assert.equal(noProdApk.excluded.prod, "缺员工 App 正式签名");
  const both = mod.decideSupportedEnvironments(registry, { test: "a.apk", prod: "b.apk" });
  assert.deepEqual(both.supported, ["test", "prod"]);
});

test("安装：包被改动即停止，什么都不写", () => {
  const copy = path.join(tmp, "tampered");
  inPkg(full.pkg, "安装.sh");
  assert.equal(spawnSync("cp", ["-a", full.pkg, copy]).status, 0);
  fs.appendFileSync(path.join(copy, "server/complex-api.jar"), "x");
  const root = newRoot();
  const r = run(path.join(copy, "安装.sh"), root);
  assert.notEqual(r.status, 0, r.out);
  assert.match(r.out, /安装包已损坏或被改动/);
  assert.ok(!exists(`${root}/opt`) && !exists(`${root}/etc/complex`));
});

test("安装：插了授权 U 盘但本包不支持生产即停止，什么都不写", () => {
  const testOnly = build((list) => { list[1].tls_cert = ""; return list; });
  const root = newRoot();
  plugUsb(root);
  const r = run(inPkg(testOnly.pkg, "安装.sh"), root, { COMPLEX_KEYVAULT_PASS: USB_PASS });
  assert.notEqual(r.status, 0, r.out);
  assert.match(r.out, /本安装包不支持本机环境/);
  assert.match(r.out, /登记未齐/);
  assert.ok(!exists(`${root}/opt`) && !exists(`${root}/etc/complex`));
});

test("安装：本机已有人工部署的系统，安装器不接管", () => {
  const root = newRoot();
  put(`${root}/etc/complex/secrets.env`, "EXISTING_KEY=keep-me\n");
  const before = sha(`${root}/etc/complex/secrets.env`);
  const r = run(inPkg(full.pkg, "安装.sh"), root);
  assert.notEqual(r.status, 0, r.out);
  assert.match(r.out, /安装器不接管/);
  assert.equal(sha(`${root}/etc/complex/secrets.env`), before);
  assert.ok(!exists(`${root}/opt`));
});

const testRoot = newRoot();
test("安装：没插授权 U 盘即装测试环境（首装）", () => {
  const r = run(inPkg(full.pkg, "安装.sh"), testRoot);
  assert.equal(r.status, 0, r.out);
  assert.match(r.out, /本机识别为：测试环境/);
  assert.match(read(`${testRoot}/etc/complex/complex.env`), /^SPRING_PROFILES_ACTIVE=prod$/m);
  const verify = spawnSync("bash", [path.join(repo, "ops/keyvault/verify-provisioned.sh"), `${testRoot}/etc/complex/secrets.env`], { encoding: "utf8" });
  assert.equal(verify.status, 0, verify.stdout + verify.stderr);
  assert.equal(fs.statSync(`${testRoot}/etc/complex/test-keys/passphrase`).mode & 0o777, 0o600);
  assert.ok(!exists(`${testRoot}/etc/complex/initial-admin.env`), "初始管理员口令文件应在装好后删除");
  assert.match(r.out, /初始管理员账号：admin/);
  assert.equal(read(`${testRoot}/opt/complex/clients/current/complex-terminal.apk`), "apk-for-test");
  assert.equal(read(`${testRoot}/opt/complex/api/current/complex-api.jar`), "fake-jar");
  assert.ok(exists(`${testRoot}/opt/complex/web/current/dist/index.html`));
  const site = read(`${testRoot}/etc/nginx/sites-enabled/complex`);
  assert.match(site, /client_max_body_size 60m;/);
  assert.match(site, /server_name test\.example\.internal;/);
  assert.match(site, /ssl_certificate\s+\/etc\/ssl\/complex\/test\.crt;/);
  assert.ok(!exists(`${testRoot}/etc/nginx/sites-enabled/default`));
  const unit = read(`${testRoot}/etc/systemd/system/complex-api.service`);
  assert.match(unit, /EnvironmentFile=\/etc\/complex\/db\.env/);
  assert.match(unit, /User=complex/);
  const state = read(`${testRoot}/etc/complex/install-state.env`);
  assert.match(state, /^INSTALLED_ENV=test$/m);
  assert.match(state, /^INSTALLED_SECRETS_SOURCE=test-local$/m);
  assert.match(read(`${testRoot}/opt/complex/docs/使用教程.html`), /使用教程/);
});

test("安装：测试机插上授权 U 盘（识别为生产）即停止，不能直接改装", () => {
  installedAt(testRoot);
  plugUsb(testRoot);
  const before = sha(`${testRoot}/etc/complex/secrets.env`);
  const r = run(inPkg(full.pkg, "安装.sh"), testRoot, { COMPLEX_KEYVAULT_PASS: USB_PASS });
  assert.notEqual(r.status, 0, r.out);
  assert.match(r.out, /不能直接改装/);
  assert.equal(sha(`${testRoot}/etc/complex/secrets.env`), before);
  fs.rmSync(`${testRoot}/media`, { recursive: true, force: true });
});

test("安装：新版本起不来且有上一版本即自动恢复", () => {
  installedAt(testRoot);
  for (const part of ["api", "web", "clients"]) {
    const base = `${testRoot}/opt/complex/${part}`;
    fs.renameSync(path.join(base, currentOf(testRoot, part)), `${base}/releases/old-1`);
    fs.rmSync(`${base}/current`);
    fs.symlinkSync("releases/old-1", `${base}/current`);
  }
  const r = run(inPkg(full.pkg, "安装.sh"), testRoot, { SHIM_HEALTH: "fail" });
  assert.notEqual(r.status, 0, r.out);
  assert.match(r.out, /新版本启动失败，已自动恢复到上一版本/);
  for (const part of ["api", "web", "clients"]) assert.equal(currentOf(testRoot, part), "releases/old-1");
});

test("安装：首装启动失败即停止并保留初始管理员，重跑装好且口令不变", () => {
  const root = newRoot();
  const failed = run(inPkg(full.pkg, "安装.sh"), root, { SHIM_HEALTH: "fail" });
  assert.notEqual(failed.status, 0, failed.out);
  assert.match(failed.out, /系统启动失败，请把屏幕上的内容拍照发给 IT/);
  const kept = read(`${root}/etc/complex/initial-admin.env`).match(/^COMPLEX_AUTH_PRODUCTION_SEED_ADMIN_PASSWORD=(.+)$/m)?.[1];
  assert.ok(kept && kept.length >= 12, "初始管理员口令应保留且不少于 12 位");
  const rerun = run(inPkg(full.pkg, "安装.sh"), root);
  assert.equal(rerun.status, 0, rerun.out);
  assert.ok(rerun.out.includes(`初始管理员口令：${kept}`), rerun.out);
  assert.match(read(`${root}/var/log/complex/install.log`), /动作=首装/);
});

test("安装：生产首装 U 盘口令不对即停止，不生成运行密钥", () => {
  const root = newRoot();
  plugUsb(root);
  const r = run(inPkg(full.pkg, "安装.sh"), root, { COMPLEX_KEYVAULT_PASS: "wrong-pass-nonsecret" });
  assert.notEqual(r.status, 0, r.out);
  assert.match(r.out, /U 盘钥匙口令不对或钥匙已损坏/);
  assert.ok(!exists(`${root}/etc/complex/secrets.env`));
  assert.ok(!exists(`${root}/opt/complex/api/releases`));
});

test("安装：授权 U 盘已被手工挂在 /mnt 等别处，也识别为生产环境", () => {
  const root = newRoot();
  plugUsb(root, "mnt/usb");
  const r = run(inPkg(full.pkg, "安装.sh"), root, { COMPLEX_KEYVAULT_PASS: USB_PASS, SHIM_LSBLK: "/dev/sdb1 1 part /mnt/usb" });
  assert.equal(r.status, 0, r.out);
  assert.match(r.out, /本机识别为：生产环境/);
});

const prodRoot = newRoot();
test("安装：插了授权 U 盘即装生产环境（首装），换上 U 盘里的强密钥并留痕", () => {
  const usb = plugUsb(prodRoot);
  const r = run(inPkg(full.pkg, "安装.sh"), prodRoot, { COMPLEX_KEYVAULT_PASS: USB_PASS });
  assert.equal(r.status, 0, r.out);
  assert.match(r.out, /本机识别为：生产环境/);
  const pub = read(`${usb}/complex-keys/master_authority.pub.b64`).trim();
  assert.match(read(`${prodRoot}/etc/complex/secrets.env`), new RegExp(`^COMPLEX_MASTER_AUTHORITY_PUBKEY=${pub.replace(/[+/=]/g, "\\$&")}$`, "m"));
  assert.ok(!exists(`${prodRoot}/etc/complex/test-keys`));
  assert.equal(read(`${prodRoot}/opt/complex/clients/current/complex-terminal.apk`), "apk-for-prod");
  assert.match(read(`${prodRoot}/etc/nginx/sites-enabled/complex`), /server_name prod\.example\.internal;/);
  const fingerprint = crypto.createHash("sha256").update(Buffer.from(pub, "base64")).digest("hex").slice(0, 16);
  const lastLog = read(`${prodRoot}/var/log/complex/install.log`).trim().split("\n").at(-1);
  assert.ok(lastLog.includes("zhangsan") && lastLog.includes(fingerprint) && lastLog.includes("生产环境"), lastLog);
  assert.match(read(`${prodRoot}/etc/complex/install-state.env`), /^INSTALLED_SECRETS_SOURCE=usb$/m);
  assert.match(r.out, /请拔出 U 盘交保管人/);
});

test("安装：生产升级插着 U 盘即沿用现有密钥；不插 U 盘被识别为测试即停止", () => {
  installedAt(prodRoot);
  const before = sha(`${prodRoot}/etc/complex/secrets.env`);
  const upgraded = run(inPkg(full.pkg, "安装.sh"), prodRoot); // 不给口令：若去解 U 盘会读终端失败
  assert.equal(upgraded.status, 0, upgraded.out);
  assert.equal(sha(`${prodRoot}/etc/complex/secrets.env`), before);
  fs.rmSync(`${prodRoot}/media`, { recursive: true, force: true });
  const unplugged = run(inPkg(full.pkg, "安装.sh"), prodRoot);
  assert.notEqual(unplugged.status, 0, unplugged.out);
  assert.match(unplugged.out, /不能直接改装/);
  assert.equal(sha(`${prodRoot}/etc/complex/secrets.env`), before);
});

test("U盘首次制钥：口令过短即停止；已有钥匙的 U 盘不能重做；空 U 盘做成钥匙", () => {
  const script = inPkg(full.pkg, "U盘首次制钥.sh");
  const root = newRoot();
  fs.mkdirSync(`${root}/media/op/BLANK`, { recursive: true });
  const short = run(script, root, { COMPLEX_KEYVAULT_PASS: "short-0001" });
  assert.notEqual(short.status, 0, short.out);
  assert.match(short.out, /口令至少 12 位/);
  assert.ok(!exists(`${root}/media/op/BLANK/complex-keys`));

  const made = run(script, root, { COMPLEX_KEYVAULT_PASS: USB_PASS });
  assert.equal(made.status, 0, made.out);
  assert.ok(exists(`${root}/media/op/BLANK/complex-keys/keystore.env.enc`));
  assert.match(made.out, /安全基线台账/);

  const before = sha(`${root}/media/op/BLANK/complex-keys/keystore.env.enc`);
  const again = run(script, root, { COMPLEX_KEYVAULT_PASS: USB_PASS });
  assert.notEqual(again.status, 0, again.out);
  assert.match(again.out, /此 U 盘已有钥匙/);
  assert.equal(sha(`${root}/media/op/BLANK/complex-keys/keystore.env.enc`), before);
});
