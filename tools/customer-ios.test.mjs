// 文件版本：V0.01
// 客人 iPhone App 工程断言（ROOT-03-R13；PGM「受控的十一类终端：客人手机应用……」「界面全中文（所有终端零英文）」）：
// - Xcode 工程有共享 scheme App；开发区域与唯一本地化为简体中文（系统弹窗与按钮随之显示中文）；显示名、Bundle ID 为开发值；
// - 只申请相机、麦克风（投诉录语音、录像的声音）、相册的中文用途说明，不申请定位；
// - 手机上显示的版本号（MARKETING_VERSION）与请求头 X-Client-Version（按版本关停用）同一个值：package.json 的 version；
// - 每次构建编译前先跑 release-check.sh：发行构建（Release）外部资料缺失或仍为开发值即失败并逐项给中文原因（这里在 Linux 上直接用 bash 跑该脚本）；
// - 通用链接（深链）只在发行构建带 applinks:<客人深链主机>；
// - .github/workflows/ios-customer-app.yml：只按客人端路径触发，macOS 固定 Xcode，不签名模拟器构建并启动冒烟，第三方动作固定完整提交号。
// 160 没有 Xcode：本文件只是静态检查与发行检查脚本的执行，不代表 iPhone 包已构建；构建结论只认该工作流的成功运行。
// 用法：node --test tools/customer-ios.test.mjs
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import { createRequire } from "node:module";
import os from "node:os";
import path from "node:path";
import { describe, test } from "node:test";
import { fileURLToPath } from "node:url";
import { parse as parseYaml } from "yaml";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const app = path.join(root, "apps/customer-app");
const ios = path.join(app, "ios/App");
// 解析 Xcode 工程与 plist 用 Capacitor CLI（apps/customer-app 的开发依赖）自带的解析库。
const requireFromApp = createRequire(path.join(app, "package.json"));
const xcode = requireFromApp("xcode");
const plist = requireFromApp("plist");

const DEV_ID = "com.complex.customer.dev";
const DEV_NAME = "综合体客人端（开发）";
const unq = (value) => String(value ?? "").replace(/^"(.*)"$/, "$1");

function read(rel) {
  const file = path.join(ios, rel);
  assert.ok(fs.existsSync(file), `缺 apps/customer-app/ios/App/${rel}`);
  return fs.readFileSync(file, "utf8");
}

function loadProject() {
  read("App.xcodeproj/project.pbxproj");
  const parsed = xcode.project(path.join(ios, "App.xcodeproj/project.pbxproj")).parseSync();
  const objects = parsed.hash.project.objects;
  const entries = (isa) => Object.entries(objects[isa] ?? {}).filter(([key]) => !key.endsWith("_comment"));
  const [targetId, target] = entries("PBXNativeTarget").find(([, t]) => unq(t.name) === "App");
  const configs = Object.fromEntries(objects.XCConfigurationList[target.buildConfigurationList].buildConfigurations
    .map(({ value }) => [unq(objects.XCBuildConfiguration[value].name), objects.XCBuildConfiguration[value].buildSettings]));
  const setting = (config, key) => (configs[config][key] === undefined ? undefined : unq(configs[config][key]));
  return { project: objects.PBXProject[parsed.hash.project.rootObject], objects, targetId, target, setting };
}

describe("客人 iPhone App 工程", () => {
  test("sharedSchemeAppForXcodebuild：共享 scheme App 指向 App 目标，调试运行用 Debug", () => {
    const { targetId } = loadProject();
    const scheme = read("App.xcodeproj/xcshareddata/xcschemes/App.xcscheme");
    assert.match(scheme, new RegExp(`BlueprintIdentifier = "${targetId}"`));
    assert.match(scheme, /BuildableName = "App\.app"/);
    assert.match(scheme, /<LaunchAction\s+buildConfiguration = "Debug"/);
    assert.match(scheme, /<ArchiveAction\s+buildConfiguration = "Release"/);
  });

  test("chineseRegionAndDevIdentity：开发区域与唯一本地化为简体中文；显示名与 Bundle ID 为开发值", () => {
    const { project, setting } = loadProject();
    assert.equal(unq(project.developmentRegion), "zh-Hans");
    assert.deepEqual(project.knownRegions.map(unq), ["zh-Hans", "Base"]);
    const info = plist.parse(read("App/Info.plist"));
    assert.equal(info.CFBundleDevelopmentRegion, "zh-Hans");
    assert.deepEqual(info.CFBundleLocalizations, ["zh-Hans"]);
    assert.equal(info.CFBundleDisplayName, "$(CUSTOMER_IOS_DISPLAY_NAME)");
    for (const config of ["Debug", "Release"]) {
      assert.equal(setting(config, "CUSTOMER_IOS_DISPLAY_NAME"), DEV_NAME, config);
      assert.equal(setting(config, "PRODUCT_BUNDLE_IDENTIFIER"), DEV_ID, config);
    }
  });

  test("marketingVersionIsClientVersion：手机上显示的版本号＝请求头 X-Client-Version＝apps/customer-app/package.json 的 version", () => {
    const { setting } = loadProject();
    const { version } = JSON.parse(fs.readFileSync(path.join(app, "package.json"), "utf8"));
    for (const config of ["Debug", "Release"]) assert.equal(setting(config, "MARKETING_VERSION"), version, config);
  });

  test("usageDescriptionsChineseNoLocation：只申请相机、麦克风、相册，用途说明全中文；不申请定位", () => {
    const info = plist.parse(read("App/Info.plist"));
    const usage = Object.keys(info).filter((key) => key.endsWith("UsageDescription")).sort();
    assert.deepEqual(usage, ["NSCameraUsageDescription", "NSMicrophoneUsageDescription", "NSPhotoLibraryUsageDescription"]);
    for (const key of usage) {
      assert.match(info[key], /[一-鿿]/, key);
      assert.doesNotMatch(info[key], /[A-Za-z]/, key);
    }
    assert.match(info.NSMicrophoneUsageDescription, /录语音/);
  });

  test("releaseCheckRunsBeforeCompileAndLinksOnlyInRelease：编译前先跑发行包检查；通用链接只在发行构建带客人深链主机", () => {
    const { objects, target, setting } = loadProject();
    const [first] = target.buildPhases;
    const phase = objects.PBXShellScriptBuildPhase?.[first.value];
    assert.ok(phase, "第一个构建阶段应为发行包检查脚本");
    assert.equal(unq(phase.name), "发行包外部资料检查");
    assert.equal(phase.shellPath, "/bin/bash");
    assert.match(unq(phase.shellScript), /release-check\.sh/);
    assert.ok(fs.existsSync(path.join(ios, "release-check.sh")), "缺 apps/customer-app/ios/App/release-check.sh");
    for (const config of ["Debug", "Release"]) assert.equal(setting(config, "ENABLE_USER_SCRIPT_SANDBOXING"), "NO", config);
    assert.equal(setting("Release", "CODE_SIGN_ENTITLEMENTS"), "App/App.entitlements");
    assert.equal(setting("Debug", "CODE_SIGN_ENTITLEMENTS"), undefined);
    const entitlements = plist.parse(read("App/App.entitlements"));
    assert.deepEqual(entitlements, { "com.apple.developer.associated-domains": ["applinks:$(VITE_CUSTOMER_LINK_HOST)"] });
  });
});

// —— 发行包检查脚本（Xcode 构建时把构建设置作为环境变量交给脚本；这里直接用 bash 跑） ——
function releaseCheck(env, { configuration = "Release", publicJs } = {}) {
  const srcroot = fs.mkdtempSync(path.join(os.tmpdir(), "customer-ios-"));
  fs.mkdirSync(path.join(srcroot, "App/public/assets"), { recursive: true });
  if (publicJs) fs.writeFileSync(path.join(srcroot, "App/public/assets/index-abc.js"), publicJs);
  const base = Object.fromEntries(Object.entries(process.env)
    .filter(([key]) => !/^(VITE_CUSTOMER_|CUSTOMER_IOS_|PRODUCT_BUNDLE_IDENTIFIER$|DEVELOPMENT_TEAM$|CONFIGURATION$|SRCROOT$)/.test(key)));
  const result = spawnSync("bash", [path.join(ios, "release-check.sh")], {
    env: { ...base, CONFIGURATION: configuration, SRCROOT: srcroot, ...env },
    encoding: "utf8",
  });
  fs.rmSync(srcroot, { recursive: true, force: true });
  return { status: result.status, out: `${result.stdout ?? ""}\n${result.stderr ?? ""}` };
}

// 工程里的缺省构建设置（Xcode 每次构建都会带上）。
const PROJECT_DEFAULTS = { PRODUCT_BUNDLE_IDENTIFIER: DEV_ID, CUSTOMER_IOS_DISPLAY_NAME: DEV_NAME };
const FORMAL = {
  PRODUCT_BUNDLE_IDENTIFIER: "com.example.customer",
  CUSTOMER_IOS_DISPLAY_NAME: "综合体客人端",
  DEVELOPMENT_TEAM: "ABCDE12345",
  VITE_CUSTOMER_API_BASE_URL: "https://api.example.com",
  VITE_CUSTOMER_LINK_HOST: "c.example.com",
};
const FORMAL_JS = 'const base="https://api.example.com";';

describe("客人 iPhone App 发行包检查", () => {
  test("debugBuildNotBlocked：模拟器调试构建（Debug）不拦", () => {
    const { status, out } = releaseCheck(PROJECT_DEFAULTS, { configuration: "Debug" });
    assert.equal(status, 0, out);
  });

  test("releaseRefusedWithProjectDefaults：外部资料都没传（工程缺省为开发值）时发行构建失败，逐项给中文原因", () => {
    const { status, out } = releaseCheck(PROJECT_DEFAULTS);
    assert.notEqual(status, 0, out);
    for (const reason of ["正式 Bundle ID 不能用开发值", "正式中文应用名不能带「开发」", "缺 Apple 开发者团队", "缺正式接口地址", "缺客人深链主机"]) {
      assert.match(out, new RegExp(reason), out);
    }
    const empty = releaseCheck({});
    assert.notEqual(empty.status, 0, empty.out);
    for (const reason of ["缺正式 Bundle ID", "缺正式中文应用名"]) assert.match(empty.out, new RegExp(reason), empty.out);
  });

  test("releaseRefusesPrivateHttpAndUnbuiltAssets：私网与本机地址、http、页面资源未按正式地址打包一律拒绝", () => {
    const lan = releaseCheck({ ...FORMAL, VITE_CUSTOMER_API_BASE_URL: "https://192.168.88.160", VITE_CUSTOMER_LINK_HOST: "192.168.88.160" }, { publicJs: FORMAL_JS });
    assert.notEqual(lan.status, 0, lan.out);
    assert.match(lan.out, /正式接口地址不能是私网或本机地址/);
    assert.match(lan.out, /客人深链主机不能是私网或本机地址/);
    const local = releaseCheck({ ...FORMAL, VITE_CUSTOMER_LINK_HOST: "localhost" }, { publicJs: FORMAL_JS });
    assert.match(local.out, /客人深链主机不能是私网或本机地址/);
    const http = releaseCheck({ ...FORMAL, VITE_CUSTOMER_API_BASE_URL: "http://api.example.com" }, { publicJs: FORMAL_JS });
    assert.notEqual(http.status, 0, http.out);
    assert.match(http.out, /正式接口地址须为 https/);
    const unbuilt = releaseCheck(FORMAL, { publicJs: 'const base="https://192.168.88.160";' });
    assert.notEqual(unbuilt.status, 0, unbuilt.out);
    assert.match(unbuilt.out, /页面资源不是按正式接口地址打包的/);
  });

  test("releaseAllowedWithFormalInputs：外部资料齐全且页面资源按正式接口地址打包时放行", () => {
    const { status, out } = releaseCheck(FORMAL, { publicJs: FORMAL_JS });
    assert.equal(status, 0, out);
  });
});

describe("iPhone 模拟器构建工作流", () => {
  const file = path.join(root, ".github/workflows/ios-customer-app.yml");
  const PATHS = ["apps/customer-app/**", "packages/customer-core/**", ".github/workflows/ios-customer-app.yml"];

  test("triggersOnlyOnCustomerClientPaths：只在客人 App、三端共用模块或本工作流变化时触发", () => {
    assert.ok(fs.existsSync(file), "缺 .github/workflows/ios-customer-app.yml");
    const workflow = parseYaml(fs.readFileSync(file, "utf8"));
    assert.deepEqual(Object.keys(workflow.on).sort(), ["pull_request", "push"]);
    for (const event of ["pull_request", "push"]) assert.deepEqual(workflow.on[event].paths, PATHS, event);
  });

  test("unsignedSimulatorBuildThenLaunchSmoke：macOS 固定 Xcode，npm ci→构建客人页面→cap sync ios→不签名模拟器构建→启动冒烟；第三方动作固定完整提交号", () => {
    assert.ok(fs.existsSync(file), "缺 .github/workflows/ios-customer-app.yml");
    const workflow = parseYaml(fs.readFileSync(file, "utf8"));
    const jobs = Object.values(workflow.jobs);
    assert.equal(jobs.length, 1);
    const [job] = jobs;
    assert.match(String(job["runs-on"]), /^macos-(15|26)$/);
    const text = job.steps.map((step) => `${step.run ?? ""}\n`).join("") + JSON.stringify(job.env ?? {});
    assert.match(text, /\/Applications\/Xcode_\d+(\.\d+)+\.app/);
    let at = -1;
    for (const marker of ["xcode-select -s", "npm ci", "npm run build --workspace apps/customer-app", "cap sync ios", "CODE_SIGNING_ALLOWED=NO", "xcrun simctl launch"]) {
      const next = text.indexOf(marker, at + 1);
      assert.ok(next > at, `工作流缺少或顺序不对：${marker}`);
      at = next;
    }
    for (const step of job.steps.filter((s) => s.uses)) assert.match(step.uses, /^[\w.-]+\/[\w.-]+@[0-9a-f]{40}$/, step.uses);
  });
});
