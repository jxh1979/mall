// 文件版本：V0.01
// 商户手机应用工程断言（第 25 条 25-R5、25-R7；ENUM「终端十一端：……商户手机应用……」、PGM「受控的十一类终端：……商户手机应用……」「界面全中文（所有终端零英文）」）：
// - 原生壳只加载本地打包的页面资源（不设远程网页地址）、页面请求走原生通道、原生桥不打插件参数日志；页面资源取站点根（base /，整页链接重载不白屏）；
// - Android：调试包包名 com.complex.merchant.dev、中文名「综合体商户端（开发）」、版本号＝package.json 的 version；只申请联网、关系统自动备份
//   （第十八批裁决 19「卸载重装或清除应用数据算新设备」）；调试包只多信任 160 自签证书，发行包只认系统证书；网页弹窗按钮为「确定」「取消」；
// - iPhone：Bundle ID 与显示名为开发值、简体中文区域、版本号＝package.json 的 version、相机／相册／麦克风用途说明为中文；网页弹窗按钮为「确定」「取消」；
// - .github/workflows/ios-merchant-app.yml：只按商户 App 路径触发、不签名模拟器构建并启动冒烟、第三方动作固定完整提交号。
// 160 没有 Xcode：iPhone 端这里只是静态检查，构建结论只认该工作流的成功运行。用法：node --test tools/merchant-app.test.mjs
import assert from "node:assert/strict";
import fs from "node:fs";
import { createRequire } from "node:module";
import path from "node:path";
import { describe, test } from "node:test";
import { fileURLToPath } from "node:url";
import { parse as parseYaml } from "yaml";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const app = path.join(root, "apps/merchant-app");
const DEV_ID = "com.complex.merchant.dev";
const DEV_NAME = "综合体商户端（开发）";
// Capacitor 原生壳写死的英文弹窗按钮（Android BridgeWebChromeClient、iPhone WebViewDelegationHandler）。
const ENGLISH_BUTTONS = /"(OK|Ok|Cancel)"/;
const unq = (value) => String(value ?? "").replace(/^"(.*)"$/, "$1");

function read(rel) {
  const file = path.join(app, rel);
  assert.ok(fs.existsSync(file), `缺 apps/merchant-app/${rel}`);
  return fs.readFileSync(file, "utf8");
}

const appVersion = () => JSON.parse(read("package.json")).version;

describe("商户手机应用工程", () => {
  test("nativeShellLoadsLocalBundleOnly：不设远程网页地址，原生桥不打日志，页面请求走原生通道，资源取站点根", () => {
    const config = read("capacitor.config.ts");
    assert.match(config, /appId:\s*"com\.complex\.merchant\.dev"/);
    assert.match(config, /webDir:\s*"dist"/);
    assert.doesNotMatch(config, /\bserver\s*:|\burl\s*:/);
    assert.match(config, /loggingBehavior:\s*"none"/);
    assert.match(config, /CapacitorHttp:\s*\{\s*enabled:\s*true\s*\}/);
    assert.match(read("vite.config.ts"), /base:\s*"\/"/);
  });
});

describe("商户手机应用 Android 端", () => {
  test("debugIdentityIsDevPackageWithChineseName：调试包独立包名与中文名，版本号＝package.json 的 version", () => {
    const gradle = read("android/app/build.gradle");
    assert.match(gradle, /namespace = "com\.complex\.merchant\.dev"/);
    assert.match(gradle, /applicationId "com\.complex\.merchant\.dev"/);
    assert.match(gradle, /resValue "string", "app_name", "综合体商户端（开发）"/);
    assert.ok(gradle.includes(`versionName "${appVersion()}"`), `versionName 应为 ${appVersion()}`);
    assert.doesNotMatch(gradle, /com\.complex\.(customer|terminal)/);
  });

  test("manifestInternetOnlyNoBackup：只申请联网，不许系统自动备份", () => {
    const manifest = read("android/app/src/main/AndroidManifest.xml");
    const permissions = [...manifest.matchAll(/<uses-permission android:name="([^"]+)"/g)].map((m) => m[1]);
    assert.deepEqual(permissions, ["android.permission.INTERNET"]);
    assert.match(manifest, /android:allowBackup="false"/);
    assert.match(manifest, /android:networkSecurityConfig="@xml\/network_security_config"/);
  });

  test("debugTrusts160CertReleaseSystemOnly：160 自签证书只在调试源集，发行包只认系统证书、不许 http", () => {
    const release = read("android/app/src/main/res/xml/network_security_config.xml");
    assert.match(release, /<base-config cleartextTrafficPermitted="false">/);
    assert.doesNotMatch(release, /complex160|192\.168\.88\.160|src="user"/);
    assert.ok(!fs.existsSync(path.join(app, "android/app/src/main/res/raw/complex160.pem")), "160 证书不得进发行包");
    const debug = read("android/app/src/debug/res/xml/network_security_config.xml");
    assert.match(debug, /<domain includeSubdomains="false">192\.168\.88\.160<\/domain>/);
    assert.match(debug, /<certificates src="@raw\/complex160" \/>/);
    const staffCert = fs.readFileSync(path.join(root, "apps/android-terminal/android-app/app/src/main/res/raw/complex160.pem"), "utf8");
    assert.equal(read("android/app/src/debug/res/raw/complex160.pem"), staffCert);
  });

  test("webDialogsChinese：网页弹窗（alert、confirm、prompt）按钮为「确定」「取消」", () => {
    const activity = read("android/app/src/main/java/com/complex/merchant/dev/MainActivity.java");
    assert.match(activity, /package com\.complex\.merchant\.dev;/);
    assert.match(activity, /setWebChromeClient\(new BridgeWebChromeClient\(bridge\)/);
    for (const method of ["onJsAlert", "onJsConfirm", "onJsPrompt"]) assert.match(activity, new RegExp(`public boolean ${method}\\(`), method);
    assert.match(activity, /"确定"/);
    assert.match(activity, /"取消"/);
    assert.doesNotMatch(activity, ENGLISH_BUTTONS);
  });
});

describe("商户手机应用 iPhone 端", () => {
  // 解析 Xcode 工程与 plist 用 Capacitor CLI（apps/merchant-app 的开发依赖）自带的解析库。
  function loadProject() {
    read("ios/App/App.xcodeproj/project.pbxproj");
    const requireFromApp = createRequire(path.join(app, "package.json"));
    const xcode = requireFromApp("xcode");
    const parsed = xcode.project(path.join(app, "ios/App/App.xcodeproj/project.pbxproj")).parseSync();
    const objects = parsed.hash.project.objects;
    const [, target] = Object.entries(objects.PBXNativeTarget).find(([key, t]) => !key.endsWith("_comment") && unq(t.name) === "App");
    const configs = Object.fromEntries(objects.XCConfigurationList[target.buildConfigurationList].buildConfigurations
      .map(({ value }) => [unq(objects.XCBuildConfiguration[value].name), objects.XCBuildConfiguration[value].buildSettings]));
    const setting = (config, key) => (configs[config][key] === undefined ? undefined : unq(configs[config][key]));
    return { project: objects.PBXProject[parsed.hash.project.rootObject], setting };
  }

  const info = () => createRequire(path.join(app, "package.json"))("plist").parse(read("ios/App/App/Info.plist"));

  test("devIdentityChineseRegionVersion：Bundle ID 与显示名为开发值、简体中文区域、版本号＝package.json 的 version", () => {
    const { project, setting } = loadProject();
    assert.equal(unq(project.developmentRegion), "zh-Hans");
    const plist = info();
    assert.equal(plist.CFBundleDevelopmentRegion, "zh-Hans");
    assert.equal(plist.CFBundleDisplayName, "$(MERCHANT_IOS_DISPLAY_NAME)");
    for (const config of ["Debug", "Release"]) {
      assert.equal(setting(config, "MERCHANT_IOS_DISPLAY_NAME"), DEV_NAME, config);
      assert.equal(setting(config, "PRODUCT_BUNDLE_IDENTIFIER"), DEV_ID, config);
      assert.equal(setting(config, "MARKETING_VERSION"), appVersion(), config);
    }
  });

  test("usageDescriptionsChinese：相机、相册、麦克风用途说明都在且全中文（上传凭证选拍照或录像时用，缺一项选到即闪退）", () => {
    const plist = info();
    const usage = Object.keys(plist).filter((key) => key.endsWith("UsageDescription")).sort();
    assert.deepEqual(usage, ["NSCameraUsageDescription", "NSMicrophoneUsageDescription", "NSPhotoLibraryUsageDescription"]);
    for (const key of usage) {
      assert.match(plist[key], /[一-鿿]/, key);
      assert.doesNotMatch(plist[key], /[A-Za-z]/, key);
    }
  });

  test("webDialogsChinese：网页弹窗（alert、confirm、prompt）按钮为「确定」「取消」，根视图控制器换成 MerchantBridgeViewController", () => {
    const delegate = read("ios/App/App/AppDelegate.swift");
    assert.match(delegate, /class MerchantBridgeViewController: CAPBridgeViewController/);
    for (const method of ["runJavaScriptAlertPanelWithMessage", "runJavaScriptConfirmPanelWithMessage", "runJavaScriptTextInputPanelWithPrompt"]) {
      assert.match(delegate, new RegExp(`func webView\\(_ webView: WKWebView, ${method}`), method);
    }
    assert.match(delegate, /"确定"/);
    assert.match(delegate, /"取消"/);
    assert.doesNotMatch(delegate, ENGLISH_BUTTONS);
    assert.match(read("ios/App/App/Base.lproj/Main.storyboard"), /customClass="MerchantBridgeViewController" customModule="App" customModuleProvider="target"/);
    const scene = read("ios/App/App/SceneDelegate.swift");
    assert.match(scene, /rootViewController = MerchantBridgeViewController\(\)/);
    assert.doesNotMatch(scene, /CAPBridgeViewController\(\)/);
  });
});

describe("商户手机应用 iPhone 模拟器构建工作流", () => {
  const file = path.join(root, ".github/workflows/ios-merchant-app.yml");
  const PATHS = ["apps/merchant-app/**", "packages/customer-core/**", ".github/workflows/ios-merchant-app.yml"];
  const load = () => {
    assert.ok(fs.existsSync(file), "缺 .github/workflows/ios-merchant-app.yml");
    return parseYaml(fs.readFileSync(file, "utf8"));
  };

  test("triggersOnlyOnMerchantAppPaths：只在商户 App、共用模块或本工作流变化时触发", () => {
    const workflow = load();
    assert.deepEqual(Object.keys(workflow.on).sort(), ["pull_request", "push"]);
    for (const event of ["pull_request", "push"]) assert.deepEqual(workflow.on[event].paths, PATHS, event);
  });

  test("unsignedSimulatorBuildThenLaunchSmoke：npm ci→构建商户页面→cap sync ios→不签名模拟器构建→启动冒烟；第三方动作固定完整提交号", () => {
    const jobs = Object.values(load().jobs);
    assert.equal(jobs.length, 1);
    const [job] = jobs;
    assert.equal(job.env.BUNDLE_ID, DEV_ID);
    const text = job.steps.map((step) => `${step.run ?? ""}\n`).join("");
    let at = -1;
    for (const marker of ["xcode-select -s", "npm ci", "npm run build --workspace apps/merchant-app", "cap sync ios", "CODE_SIGNING_ALLOWED=NO", DEV_NAME, "xcrun simctl launch"]) {
      const next = text.indexOf(marker, at + 1);
      assert.ok(next > at, `工作流缺少或顺序不对：${marker}`);
      at = next;
    }
    for (const step of job.steps.filter((s) => s.uses)) assert.match(step.uses, /^[\w.-]+\/[\w.-]+@[0-9a-f]{40}$/, step.uses);
  });
});
