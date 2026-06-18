import { spawn } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { setTimeout as delay } from "node:timers/promises";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(scriptDir, "..");
const npmCommand = process.platform === "win32" ? "npm.cmd" : "npm";
const nodeCommand = process.execPath;
const javaCommand = process.env.JAVA_BIN ?? "java";
const port = String(process.env.REAL_BACKEND_PORT ?? "18080");
const backendBaseUrl = process.env.REAL_BACKEND_BASE_URL ?? `http://127.0.0.1:${port}`;
const username = process.env.REAL_BACKEND_USERNAME ?? process.env.P0_ADMIN_USERNAME ?? "admin";
const password = process.env.REAL_BACKEND_PASSWORD ?? process.env.P0_ADMIN_PASSWORD ?? "admin-pass";
const jarPath = path.resolve(root, process.env.REAL_BACKEND_JAR ?? "backend/target/complex-api-0.1.0-SNAPSHOT.jar");
const logDir = path.join(root, "test-results", "real-backend");
const logPath = path.join(logDir, "backend.log");
const healthUrl = `${backendBaseUrl}/api/v1/system/health`;

// Spring Boot / Hikari require a `jdbc:` URL with separate credentials. Some
// environments export a 12-factor style `postgresql://user:pass@host/db` URL
// (used by psql and node `pg`), which Hikari rejects with
// `'url' must start with "jdbc"`. Normalize that shape so the seeded backend
// boots against the local database without requiring a manual env override.
function normalizeDatabaseEnv(env) {
  const raw = env.COMPLEX_DATABASE_URL;
  if (!raw || raw.startsWith("jdbc:")) {
    return {};
  }
  const match = raw.match(
    /^(?:postgres|postgresql):\/\/(?:([^:@/]+)(?::([^@/]*))?@)?(.+)$/,
  );
  if (!match) {
    return {};
  }
  const [, user, pass, rest] = match;
  const overrides = { COMPLEX_DATABASE_URL: `jdbc:postgresql://${rest}` };
  if (user && !env.COMPLEX_DATABASE_USERNAME) {
    overrides.COMPLEX_DATABASE_USERNAME = decodeURIComponent(user);
  }
  if (pass !== undefined && !env.COMPLEX_DATABASE_PASSWORD) {
    overrides.COMPLEX_DATABASE_PASSWORD = decodeURIComponent(pass);
  }
  return overrides;
}

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
  const deadline = Date.now() + Number(process.env.REAL_BACKEND_STARTUP_TIMEOUT_MS ?? 120_000);
  let lastError = "not started";
  while (Date.now() < deadline) {
    if (backendProcess.exitCode !== null) {
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
  throw new Error(`backend health check timed out at ${healthUrl}: ${lastError}. See ${logPath}`);
}

function stopBackend(backendProcess) {
  return new Promise((resolve) => {
    if (backendProcess.exitCode !== null || backendProcess.killed) {
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

async function main() {
  console.log("[real-backend-ui] packaging backend without tests...");
  await run(nodeCommand, ["tools/run-backend-maven.mjs", "-DskipTests", "package"]);

  if (!fs.existsSync(jarPath)) {
    throw new Error(`backend jar not found: ${jarPath}`);
  }

  fs.mkdirSync(logDir, { recursive: true });
  const logFd = fs.openSync(logPath, "w");
  const databaseEnv = normalizeDatabaseEnv(process.env);
  if (databaseEnv.COMPLEX_DATABASE_URL) {
    console.log(
      `[real-backend-ui] normalized COMPLEX_DATABASE_URL to JDBC form ${databaseEnv.COMPLEX_DATABASE_URL}`,
    );
  }
  console.log(`[real-backend-ui] starting backend at ${backendBaseUrl} with test admin ${username}`);
  const backendProcess = spawn(javaCommand, ["-jar", jarPath, `--server.port=${port}`], {
    cwd: root,
    stdio: ["ignore", logFd, logFd],
    env: {
      ...process.env,
      ...databaseEnv,
      SERVER_PORT: port,
      P0_ADMIN_USERNAME: username,
      P0_ADMIN_PASSWORD: password,
    },
    windowsHide: true,
  });

  try {
    await waitForHealth(backendProcess);
    console.log("[real-backend-ui] backend health check passed; running Playwright...");
    await run(npmCommand, ["run", "verify:ui:real-backend"], {
      env: {
        REAL_BACKEND_E2E: "1",
        REAL_BACKEND_BASE_URL: backendBaseUrl,
        REAL_BACKEND_USERNAME: username,
        REAL_BACKEND_PASSWORD: password,
      },
    });
    console.log("[real-backend-ui] real backend UI acceptance passed.");
  } finally {
    await stopBackend(backendProcess);
    fs.closeSync(logFd);
  }
}

main().catch((error) => {
  console.error(`[real-backend-ui] ${error instanceof Error ? error.message : String(error)}`);
  process.exit(1);
});
