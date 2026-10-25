// 文件版本：V0.01
// 客人 Android App 工程断言（ROOT-03-R12；PGM「受控的十一类终端：客人手机应用……」「界面全中文（所有终端零英文）」）：
// - 原生壳只加载本地打包的页面资源、原生桥不打插件参数日志；
// - 调试包包名 com.complex.customer.dev、中文名「综合体客人端（开发）」；深链 https://<客人链接主机>/c/…；不申请定位与存储读写；
// - 投诉语音在 App 内现场录（BSM「支持文字留言、照片、视频与语音」）：申请录音（网页录音由原生壳转成系统麦克风权限框）；
// - 手机上显示的版本号与请求头 X-Client-Version（按版本关停用，PGM「版本/设备」开关）同一个值：package.json 的 version；
// - 调试包只多信任 160 自签证书，发行包只认系统证书、不许 http；
// - 发行包：外部资料（正式包名、中文名、签名、正式接口地址与深链主机）缺失或仍为开发值时，Gradle 在执行任何任务前失败并给中文原因。
// 发行包检查用 gradle -m（只排任务图不执行）预演；需要 160 上的 gradle 与 ANDROID_HOME。用法：node --test tools/customer-android.test.mjs
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { after, describe, test } from "node:test";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const app = path.join(root, "apps/customer-app");
const android = path.join(app, "android");

function read(rel) {
  const file = path.join(android, rel);
  assert.ok(fs.existsSync(file), `缺 apps/customer-app/android/${rel}`);
  return fs.readFileSync(file, "utf8");
}

describe("客人 Android App 工程", () => {
  test("nativeShellLoadsLocalBundleOnly：不设远程网页地址，原生桥不打日志，页面请求走原生通道", () => {
    const config = fs.readFileSync(path.join(app, "capacitor.config.ts"), "utf8");
    assert.match(config, /webDir:\s*"dist"/);
    assert.doesNotMatch(config, /\bserver\s*:|\burl\s*:/);
    assert.match(config, /loggingBehavior:\s*"none"/);
    assert.match(config, /CapacitorHttp:\s*\{\s*enabled:\s*true\s*\}/);
  });

  test("debugIdentityIsDevPackageWithChineseName：调试包独立包名与中文名，不复用员工端包名", () => {
    const gradle = read("app/build.gradle");
    assert.match(gradle, /applicationId "com\.complex\.customer\.dev"/);
    assert.match(gradle, /resValue "string", "app_name", "综合体客人端（开发）"/);
    assert.doesNotMatch(gradle, /com\.complex\.terminal/);
  });

  test("manifestPermissionsAndDeepLink：只申请联网、相机（扫码、拍照录像）与录音（投诉语音），深链只认 https 与 /c/，主机由构建注入", () => {
    const manifest = read("app/src/main/AndroidManifest.xml");
    assert.match(manifest, /android\.permission\.INTERNET/);
    assert.match(manifest, /android\.permission\.CAMERA/);
    // 网页录音的权限请求由原生壳转成这两项系统权限（Capacitor BridgeWebChromeClient），少一项即拒绝录音。
    assert.match(manifest, /android\.permission\.RECORD_AUDIO/);
    assert.match(manifest, /android\.permission\.MODIFY_AUDIO_SETTINGS/);
    assert.doesNotMatch(manifest, /ACCESS_(FINE|COARSE)_LOCATION|READ_EXTERNAL_STORAGE|READ_MEDIA_/);
    assert.match(manifest, /android:networkSecurityConfig="@xml\/network_security_config"/);
    assert.match(manifest, /<intent-filter android:autoVerify="true">/);
    assert.match(manifest, /<data android:scheme="https" android:host="\$\{customerLinkHost\}" android:pathPrefix="\/c\/" \/>/);
  });

  test("versionNameIsClientVersion：手机上显示的版本号＝请求头 X-Client-Version＝apps/customer-app/package.json 的 version", () => {
    const { version } = JSON.parse(fs.readFileSync(path.join(app, "package.json"), "utf8"));
    assert.ok(read("app/build.gradle").includes(`versionName "${version}"`), `versionName 应为 ${version}`);
  });

  test("debugTrusts160CertReleaseSystemOnly：160 自签证书只在调试源集，发行包只认系统证书、不许 http", () => {
    const release = read("app/src/main/res/xml/network_security_config.xml");
    assert.match(release, /<base-config cleartextTrafficPermitted="false">/);
    assert.doesNotMatch(release, /complex160|192\.168\.88\.160|src="user"/);
    assert.ok(!fs.existsSync(path.join(android, "app/src/main/res/raw/complex160.pem")), "160 证书不得进发行包");
    const debug = read("app/src/debug/res/xml/network_security_config.xml");
    assert.match(debug, /<domain includeSubdomains="false">192\.168\.88\.160<\/domain>/);
    assert.match(debug, /<certificates src="@raw\/complex160" \/>/);
    const staffCert = fs.readFileSync(path.join(root, "apps/android-terminal/android-app/app/src/main/res/raw/complex160.pem"), "utf8");
    assert.equal(read("app/src/debug/res/raw/complex160.pem"), staffCert);
    assert.doesNotMatch(staffCert, /PRIVATE KEY/);
  });
});

// —— 发行包检查（gradle -m 预演） ——
const storeFile = path.join(fs.mkdtempSync(path.join(os.tmpdir(), "customer-android-")), "placeholder.jks");
fs.writeFileSync(storeFile, "");
after(() => fs.rmSync(path.dirname(storeFile), { recursive: true, force: true }));

const FORMAL = {
  CUSTOMER_ANDROID_APPLICATION_ID: "com.example.customer",
  CUSTOMER_ANDROID_APP_NAME: "综合体客人端",
  COMPLEX_ANDROID_SIGNING_STORE_FILE: storeFile,
  COMPLEX_ANDROID_SIGNING_STORE_PASSWORD: "placeholder",
  COMPLEX_ANDROID_SIGNING_KEY_ALIAS: "placeholder",
  COMPLEX_ANDROID_SIGNING_KEY_PASSWORD: "placeholder",
  VITE_CUSTOMER_API_BASE_URL: "https://api.example.com",
  VITE_CUSTOMER_LINK_HOST: "c.example.com",
};

function dryRun(task, env) {
  const base = Object.fromEntries(Object.entries(process.env).filter(([key]) => !/^(CUSTOMER_ANDROID_|COMPLEX_ANDROID_SIGNING_|VITE_CUSTOMER_)/.test(key)));
  const result = spawnSync("gradle", ["-p", android, "-m", "--console=plain", "-q", task], { env: { ...base, ...env }, encoding: "utf8" });
  return { status: result.status, out: `${result.stdout ?? ""}\n${result.stderr ?? ""}${result.error ? `\n${result.error.message}` : ""}` };
}

describe("客人 Android App 发行包检查", () => {
  test("releaseRefusedWithoutExternalInputs：外部资料都没有时发行包不能构建，逐项给中文原因", () => {
    const { status, out } = dryRun(":app:assembleRelease", {});
    assert.notEqual(status, 0, out);
    for (const reason of ["缺正式包名", "缺正式中文应用名", "缺发行签名", "缺正式接口地址", "缺客人深链主机"]) assert.match(out, new RegExp(reason), out);
  });

  test("releaseRefusesDevValues：发行包拒绝开发包名、开发名称、160 私网地址与 http", () => {
    const { status, out } = dryRun(":app:assembleRelease", {
      ...FORMAL,
      CUSTOMER_ANDROID_APPLICATION_ID: "com.complex.customer.dev",
      CUSTOMER_ANDROID_APP_NAME: "综合体客人端（开发）",
      VITE_CUSTOMER_API_BASE_URL: "http://192.168.88.160",
      VITE_CUSTOMER_LINK_HOST: "192.168.88.160",
    });
    assert.notEqual(status, 0, out);
    for (const reason of ["正式包名不能用开发包名", "正式中文应用名不能带「开发」", "正式接口地址须为 https", "客人深链主机不能是私网或本机地址"]) {
      assert.match(out, new RegExp(reason), out);
    }
    const https160 = dryRun(":app:assembleRelease", { ...FORMAL, VITE_CUSTOMER_API_BASE_URL: "https://192.168.88.160" });
    assert.notEqual(https160.status, 0, https160.out);
    assert.match(https160.out, /正式接口地址不能是私网或本机地址/, https160.out);
  });

  test("releaseRefusesBundleBuiltWithDevValues：外部资料都齐但安装包里的页面资源不是按正式接口地址打包的，也拒绝", () => {
    const { status, out } = dryRun(":app:assembleRelease", FORMAL);
    assert.notEqual(status, 0, out);
    assert.match(out, /页面资源不是按正式接口地址打包的/, out);
    assert.doesNotMatch(out, /缺正式|缺发行签名|缺客人深链主机|不能用开发|须为 https|私网或本机/, out);
  });

  test("debugBuildNeedsNoExternalInputs：调试包不受发行包检查影响", () => {
    const { status, out } = dryRun(":app:assembleDebug", {});
    assert.equal(status, 0, out);
  });
});
