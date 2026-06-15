import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(scriptDir, "..");
const originalEnvKeys = new Set(Object.keys(process.env));

function stripEnvValueQuotes(value) {
  if (value.length >= 2 && value.startsWith('"') && value.endsWith('"')) {
    return value.slice(1, -1).replace(/\\"/g, '"');
  }
  if (value.length >= 2 && value.startsWith("'") && value.endsWith("'")) {
    return value.slice(1, -1);
  }
  return value;
}

function loadEnvFile(envFilePath) {
  if (!envFilePath || !fs.existsSync(envFilePath)) return;

  for (const rawLine of fs.readFileSync(envFilePath, "utf8").split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) continue;

    const withoutExport = line.startsWith("export ") ? line.slice("export ".length).trim() : line;
    const separator = withoutExport.indexOf("=");
    if (separator <= 0) continue;

    const key = withoutExport.slice(0, separator).trim();
    if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(key)) continue;
    if (originalEnvKeys.has(key)) continue;

    process.env[key] = stripEnvValueQuotes(withoutExport.slice(separator + 1).trim());
  }
}

loadEnvFile(process.env.COMPLEX_ENV_FILE || "/etc/complex/complex.env");
loadEnvFile(path.join(os.homedir(), ".complex", "complex.env"));

const args = process.argv.slice(2);
const checkOnly = args.includes("--check");
const requireAndroidSdk = args.includes("--require-android-sdk");

let projectDir = path.join("apps", "android-terminal");
const gradleArgs = [];
for (let index = 0; index < args.length; index += 1) {
  const arg = args[index];
  if (arg === "--check" || arg === "--require-android-sdk") continue;
  if (arg === "--project") {
    projectDir = args[index + 1];
    index += 1;
    continue;
  }
  gradleArgs.push(arg);
}

const projectPath = path.resolve(root, projectDir);

function run(command, commandArgs, options = {}) {
  return spawnSync(command, commandArgs, {
    cwd: root,
    encoding: "utf8",
    windowsHide: true,
    ...options,
  });
}

function printBlock(title, lines) {
  console.error(`\n[android-env] ${title}`);
  for (const line of lines.filter(Boolean)) {
    console.error(`- ${line}`);
  }
}

function fail(title, lines = []) {
  printBlock(title, lines);
  process.exit(1);
}

function textOf(result) {
  return `${result.stdout || ""}\n${result.stderr || ""}`.trim();
}

function parseJavaMajor(text) {
  const match = text.match(/(?:openjdk\s+)?(?:version\s+)?"(\d+)(?:\.\d+)?/i);
  return match ? Number(match[1]) : null;
}

function assertJava21() {
  const result = run("java", ["-version"]);
  if (result.error?.code === "ENOENT") {
    fail("缺少 Java 21 运行环境。", [
      "Android P0 Kotlin 核心工程使用 Java 21 toolchain。",
      "请安装 JDK 21 或更高版本，并确保 `java -version` 在当前终端可用。",
      "未通过该前置项时，不得把 Android P0 构建、smoke 或 APK 验证标记为完成。",
    ]);
  }
  if (result.error) {
    fail("无法检查 Java 运行环境。", [result.error.message]);
  }
  if (result.status !== 0) {
    fail("`java -version` 执行失败。", [textOf(result)]);
  }

  const text = textOf(result);
  const major = parseJavaMajor(text);
  if (major !== null && major < 21) {
    fail("Java 版本低于 21。", [
      `当前检测结果：${text.split(/\r?\n/)[0]}`,
      "请切换到 JDK 21 或更高版本后再执行 Android P0 验证。",
    ]);
  }
  return text.split(/\r?\n/)[0] || "Java detected";
}

function resolveGradleCommand() {
  const systemGradle = process.platform === "win32" ? "gradle.cmd" : "gradle";
  const wrapperNames = process.platform === "win32" ? ["gradlew.bat", "gradlew"] : ["gradlew"];
  const wrapperDirs = [projectPath, root];

  for (const dir of wrapperDirs) {
    for (const wrapper of wrapperNames) {
      const fullPath = path.join(dir, wrapper);
      if (fs.existsSync(fullPath)) {
        return { command: fullPath, label: path.relative(root, fullPath) };
      }
    }
  }

  return { command: systemGradle, label: "system Gradle" };
}

function assertGradle(gradle) {
  const result = run(gradle.command, ["-version"]);
  if (result.error?.code === "ENOENT") {
    fail("缺少 Gradle，且项目目录没有可用 Gradle Wrapper。", [
      "推荐方案：在 Android 工程目录加入 Gradle Wrapper，降低接手机器安装成本。",
      "临时方案：安装 Gradle，并确保 `gradle -version` 在当前终端可用。",
      "当前 `android:p0:*` 和 `verify:p0` 都会先经过该前置校验。",
    ]);
  }
  if (result.error) {
    fail("无法检查 Gradle 运行环境。", [result.error.message]);
  }
  if (result.status !== 0) {
    fail("Gradle 版本检查执行失败。", [textOf(result)]);
  }
  return `${gradle.label}: ${textOf(result).split(/\r?\n/)[0] || "Gradle detected"}`;
}

function readLocalSdkDir() {
  const localProperties = path.join(projectPath, "local.properties");
  if (!fs.existsSync(localProperties)) return null;

  const content = fs.readFileSync(localProperties, "utf8");
  const match = content.match(/^sdk\.dir=(.+)$/m);
  return match ? match[1].trim() : null;
}

function assertAndroidSdk() {
  const candidates = [
    process.env.ANDROID_HOME,
    process.env.ANDROID_SDK_ROOT,
    readLocalSdkDir(),
  ].filter(Boolean);

  if (candidates.length === 0) {
    fail("APK 构建缺少 Android SDK 路径。", [
      "请设置 ANDROID_HOME 或 ANDROID_SDK_ROOT，或在 APK 工程 local.properties 中配置 sdk.dir。",
      "APK 验证未通过前，不得把 Android APK 壳工程标记为 P0 验收完成。",
    ]);
  }

  const existing = candidates.find((candidate) => fs.existsSync(path.resolve(candidate)));
  if (!existing) {
    fail("Android SDK 路径不可访问。", [
      `已检测到路径：${candidates.join(", ")}`,
      "请确认 SDK 已安装，且当前执行账号可访问该目录。",
    ]);
  }

  return existing;
}

if (!fs.existsSync(projectPath)) {
  fail("找不到 Android 工程目录。", [
    `缺失目录：${projectDir}`,
    "Android P0 正式入口必须位于 `apps/android-terminal/`。",
  ]);
}

if (!fs.existsSync(path.join(projectPath, "build.gradle.kts")) && !fs.existsSync(path.join(projectPath, "settings.gradle.kts"))) {
  fail("Android 工程缺少 Gradle 配置。", [
    `当前目录：${projectDir}`,
    "至少需要 `build.gradle.kts` 或 `settings.gradle.kts`。",
  ]);
}

const javaLine = assertJava21();
const gradle = resolveGradleCommand();
const gradleLine = assertGradle(gradle);
const sdkLine = requireAndroidSdk ? `Android SDK: ${assertAndroidSdk()}` : null;

if (checkOnly) {
  console.log("[android-env] Android P0 环境前置校验通过。");
  console.log(`- ${javaLine}`);
  console.log(`- ${gradleLine}`);
  if (sdkLine) console.log(`- ${sdkLine}`);
  process.exit(0);
}

if (gradleArgs.length === 0) {
  fail("缺少 Gradle 执行任务。", [
    "示例：`npm run android:p0:build`、`npm run android:p0:smoke` 或 `npm run android:p0:apk`。",
  ]);
}

const result = spawnSync(gradle.command, ["-p", projectDir, ...gradleArgs], {
  cwd: root,
  stdio: "inherit",
  windowsHide: true,
});

if (result.error?.code === "ENOENT") {
  fail("Gradle 命令在执行阶段不可用。", [
    "请重新执行 `npm run verify:android-env` 定位环境问题。",
    result.error.message,
  ]);
}
if (result.error) {
  fail("Gradle 执行失败。", [result.error.message]);
}

process.exit(result.status ?? 1);
