import { spawn } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { setTimeout as delay } from "node:timers/promises";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(scriptDir, "..");
const nodeCommand = process.execPath;
const javaCommand = process.env.JAVA_BIN ?? "java";
const port = String(process.env.ANDROID_P0_SMOKE_BACKEND_PORT ?? process.env.P0_SMOKE_BACKEND_PORT ?? "18081");
const backendBaseUrl = process.env.P0_API_BASE_URL ?? `http://127.0.0.1:${port}`;
const username = process.env.P0_ADMIN_USERNAME ?? "admin";
const password = process.env.P0_ADMIN_PASSWORD ?? "admin-pass";
const jarPath = path.resolve(root, process.env.ANDROID_P0_SMOKE_JAR ?? "backend/target/complex-api-0.1.0-SNAPSHOT.jar");
const logDir = path.join(root, "test-results", "android-p0-smoke");
const logPath = path.join(logDir, "backend.log");
const healthUrl = `${backendBaseUrl}/api/v1/system/health`;
const useExistingBackend = process.env.ANDROID_P0_SMOKE_USE_EXISTING_BACKEND === "1";

function run(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd: root,
      stdio: "inherit",
      env: { ...process.env, ...options.env },
      windowsHide: true,
    });
    child.on("error", reject);
    child.on("exit", (code, signal) => {
      if (code === 0) {
        resolve();
        return;
      }
      reject(new Error(`${command} ${args.join(" ")} failed with ${signal ?? code}`));
    });
  });
}

async function waitForHealth(backendProcess) {
  const deadline = Date.now() + Number(process.env.ANDROID_P0_SMOKE_STARTUP_TIMEOUT_MS ?? 120_000);
  let lastError = "not started";
  while (Date.now() < deadline) {
    if (backendProcess && backendProcess.exitCode !== null) {
      throw new Error(`backend exited early with code ${backendProcess.exitCode}. See ${logPath}`);
    }
    try {
      const response = await fetch(healthUrl, { signal: AbortSignal.timeout(2_000) });
      if (response.ok) {
        return;
      }
      lastError = `${response.status} ${await response.text()}`;
    } catch (error) {
      lastError = error instanceof Error ? error.message : String(error);
    }
    await delay(1_000);
  }
  const logHint = backendProcess ? ` See ${logPath}` : "";
  throw new Error(`backend health check timed out at ${healthUrl}: ${lastError}.${logHint}`);
}

function stopBackend(backendProcess) {
  return new Promise((resolve) => {
    if (!backendProcess || backendProcess.exitCode !== null || backendProcess.killed) {
      resolve();
      return;
    }
    const killer = setTimeout(() => {
      if (backendProcess.exitCode === null && !backendProcess.killed) {
        backendProcess.kill("SIGKILL");
      }
    }, 5_000);
    backendProcess.once("exit", () => {
      clearTimeout(killer);
      resolve();
    });
    backendProcess.kill("SIGTERM");
  });
}

async function startBackend() {
  if (useExistingBackend) {
    console.log(`[android-p0-smoke] using existing backend at ${backendBaseUrl}`);
    await waitForHealth(null);
    return null;
  }

  console.log("[android-p0-smoke] packaging backend without tests...");
  await run(nodeCommand, ["tools/run-backend-maven.mjs", "-DskipTests", "package"]);

  if (!fs.existsSync(jarPath)) {
    throw new Error(`backend jar not found: ${jarPath}`);
  }

  fs.mkdirSync(logDir, { recursive: true });
  const logFd = fs.openSync(logPath, "w");
  console.log(`[android-p0-smoke] starting temporary backend at ${backendBaseUrl} with seeded test admin ${username}`);
  const backendProcess = spawn(javaCommand, ["-jar", jarPath, `--server.port=${port}`], {
    cwd: root,
    stdio: ["ignore", logFd, logFd],
    env: {
      ...process.env,
      SERVER_PORT: port,
      P0_ADMIN_USERNAME: username,
      P0_ADMIN_PASSWORD: password,
    },
    windowsHide: true,
  });
  backendProcess.once("exit", () => fs.closeSync(logFd));
  await waitForHealth(backendProcess);
  return backendProcess;
}

async function main() {
  const backendProcess = await startBackend();
  try {
    console.log("[android-p0-smoke] backend health check passed; running Android terminal P0 smoke...");
    await run(nodeCommand, ["tools/run-android-gradle.mjs", "--project", "apps/android-terminal", "run"], {
      env: {
        P0_API_BASE_URL: backendBaseUrl,
        P0_ADMIN_USERNAME: username,
        P0_ADMIN_PASSWORD: password,
      },
    });
    console.log("ANDROID_P0_SMOKE_PASS");
  } finally {
    await stopBackend(backendProcess);
  }
}

main().catch((error) => {
  console.error(`[android-p0-smoke] ${error instanceof Error ? error.message : String(error)}`);
  process.exit(1);
});
