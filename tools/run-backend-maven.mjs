import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(scriptDir, "..");
const backendPom = path.join(root, "backend", "pom.xml");
const args = process.argv.slice(2);
const checkOnly = args.includes("--check");
const mavenGoals = args.filter((arg) => arg !== "--check");

function run(command, commandArgs, options = {}) {
  return spawnSync(command, commandArgs, {
    cwd: root,
    encoding: "utf8",
    windowsHide: true,
    ...options,
  });
}

function printBlock(title, lines) {
  console.error(`\n[backend-env] ${title}`);
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

function parseMavenVersion(text) {
  const match = text.match(/Apache Maven\s+(\d+)\.(\d+)(?:\.(\d+))?/i);
  if (!match) return null;
  return {
    major: Number(match[1]),
    minor: Number(match[2]),
    patch: Number(match[3] || "0"),
  };
}

function assertJava21() {
  const result = run("java", ["-version"]);
  if (result.error?.code === "ENOENT") {
    fail("缺少 Java 21 运行环境。", [
      "请安装 JDK 21 或更高版本，并确保 `java -version` 在当前终端可用。",
      "正式后端口径固定为 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway。",
      "未通过该前置项时，不得把 BE-000 或 P0 后端验证标记为完成。",
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
      "请切换到 JDK 21 或更高版本后再执行后端验证。",
    ]);
  }
  return text.split(/\r?\n/)[0] || "Java detected";
}

function resolveMavenCommand() {
  const systemMaven = process.platform === "win32" ? "mvn.cmd" : "mvn";
  const candidates = process.platform === "win32"
    ? ["mvnw.cmd", "mvnw", systemMaven]
    : ["mvnw", systemMaven];

  for (const candidate of candidates) {
    if (candidate === systemMaven) {
      return { command: systemMaven, label: "system Maven" };
    }

    const fullPath = path.join(root, candidate);
    if (fs.existsSync(fullPath)) {
      return { command: fullPath, label: candidate };
    }
  }

  return { command: systemMaven, label: "system Maven" };
}

function assertMaven39(maven) {
  const result = run(maven.command, ["-version"]);
  if (result.error?.code === "ENOENT") {
    fail("缺少 Maven 3.9+，且项目根目录没有可用 Maven Wrapper。", [
      "推荐方案：在项目根目录加入 Maven Wrapper（`mvnw.cmd`、`mvnw`、`.mvn/`），降低接手机器安装成本。",
      "临时方案：安装 Maven 3.9+，并确保 `mvn -version` 在当前终端可用。",
      "当前 `npm run test:backend`、`npm run dev:backend` 和 `verify:p0` 都会先经过该前置校验。",
    ]);
  }
  if (result.error) {
    fail("无法检查 Maven 运行环境。", [result.error.message]);
  }
  if (result.status !== 0) {
    fail("Maven 版本检查执行失败。", [textOf(result)]);
  }

  const text = textOf(result);
  const version = parseMavenVersion(text);
  if (version && (version.major < 3 || (version.major === 3 && version.minor < 9))) {
    fail("Maven 版本低于 3.9。", [
      `当前检测结果：${text.split(/\r?\n/)[0]}`,
      "请升级到 Maven 3.9+，或在项目根目录加入符合要求的 Maven Wrapper。",
    ]);
  }
  return `${maven.label}: ${text.split(/\r?\n/)[0] || "Maven detected"}`;
}

if (!fs.existsSync(backendPom)) {
  fail("找不到正式后端工程。", [
    `缺失文件：${path.relative(root, backendPom)}`,
    "正式后端必须位于 `backend/`，不得回退到旧 TypeScript/Fastify/SQLite 原型。",
  ]);
}

const javaLine = assertJava21();
const maven = resolveMavenCommand();
const mavenLine = assertMaven39(maven);

if (checkOnly) {
  console.log("[backend-env] 后端环境前置校验通过。");
  console.log(`- ${javaLine}`);
  console.log(`- ${mavenLine}`);
  process.exit(0);
}

if (mavenGoals.length === 0) {
  fail("缺少 Maven 执行目标。", [
    "示例：`npm run test:backend` 或 `npm run dev:backend`。",
    "直接调用示例：`node tools/run-backend-maven.mjs test`。",
  ]);
}

const result = spawnSync(maven.command, ["-f", path.join("backend", "pom.xml"), ...mavenGoals], {
  cwd: root,
  stdio: "inherit",
  windowsHide: true,
});

if (result.error?.code === "ENOENT") {
  fail("Maven 命令在执行阶段不可用。", [
    "请重新执行 `npm run verify:backend-env` 定位环境问题。",
    result.error.message,
  ]);
}
if (result.error) {
  fail("Maven 执行失败。", [result.error.message]);
}

process.exit(result.status ?? 1);
