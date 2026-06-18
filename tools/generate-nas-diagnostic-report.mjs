import { execFileSync } from "node:child_process";
import fs from "node:fs";
import net from "node:net";
import path from "node:path";
import process from "node:process";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check");
const mdPath = "docs/nas-diagnostic-report-v0.1.md";
const jsonPath = "docs/production-readiness-evidence/nas-diagnostic-latest.json";
const mountPath = process.env.NAS_MOUNT_PATH ?? "/mnt/complex-nas-backup";
const nasServer = process.env.NAS_SERVER ?? "192.168.88.113";
const nasShare = process.env.NAS_SHARE ?? "bn";
const nasUnc = process.env.NAS_UNC ?? `//${nasServer}/${nasShare}`;
const credentialFile = process.env.NAS_SMB_CREDENTIALS_FILE ?? "/etc/complex/secrets/nas-smb.credentials";
const credentialRef = "cred://prod/nas/smb/complex-backup-rw";
const preflightScript = "ops/160/complex-nas-preflight.sh";
const failures = [];

function normalize(text) {
  return text.replace(/\r\n/g, "\n").trimEnd() + "\n";
}

function readText(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath);
  if (!fs.existsSync(absolutePath)) return "";
  return fs.readFileSync(absolutePath, "utf8").replace(/^\uFEFF/, "");
}

function runCommand(command, args, options = {}) {
  try {
    const stdout = execFileSync(command, args, {
      cwd: repoRoot,
      encoding: "utf8",
      timeout: options.timeout ?? 5000,
      env: { ...process.env, ...options.env },
      stdio: ["ignore", "pipe", "pipe"],
    });
    return { exit_code: 0, stdout: stdout.trim(), stderr: "" };
  } catch (error) {
    return {
      exit_code: error.status ?? 1,
      stdout: String(error.stdout ?? "").trim(),
      stderr: String(error.stderr ?? error.message ?? "").trim(),
    };
  }
}

function statPath(filePath) {
  try {
    const stat = fs.statSync(filePath);
    return {
      exists: true,
      mode: (stat.mode & 0o777).toString(8),
      uid: stat.uid,
      gid: stat.gid,
      is_directory: stat.isDirectory(),
      is_file: stat.isFile(),
    };
  } catch (error) {
    return { exists: false, error: error.message };
  }
}

function credentialMetadata() {
  const stat = statPath(credentialFile);
  let readableByCurrentUser = false;
  try {
    fs.accessSync(credentialFile, fs.constants.R_OK);
    readableByCurrentUser = true;
  } catch {
    readableByCurrentUser = false;
  }
  return {
    path: credentialFile,
    credential_ref: credentialRef,
    ...stat,
    readable_by_current_user: readableByCurrentUser,
    content_collected: false,
  };
}

function parseFindmnt(stdout) {
  if (!stdout) return { active: false };
  const [target, source, fstype, ...optionParts] = stdout.split(/\s+/);
  const options = optionParts.join(" ");
  return {
    active: Boolean(target),
    target,
    source,
    fstype,
    options,
    exact_expected_cifs: target === mountPath && source === nasUnc && fstype === "cifs",
  };
}

function tcpProbe(host, port, timeoutMs = 2500) {
  return new Promise((resolve) => {
    const started = Date.now();
    const socket = new net.Socket();
    let settled = false;
    function finish(status, error = "") {
      if (settled) return;
      settled = true;
      socket.destroy();
      resolve({ status, host, port, elapsed_ms: Date.now() - started, error });
    }
    socket.setTimeout(timeoutMs);
    socket.once("connect", () => finish("passed"));
    socket.once("timeout", () => finish("failed", "timeout"));
    socket.once("error", (error) => finish("failed", error.message));
    socket.connect(port, host);
  });
}

function commandStatus(command) {
  const result = runCommand("bash", ["-lc", `command -v ${command}`], { timeout: 3000 });
  if (result.exit_code === 0) return { command, available: true, path: result.stdout || null, detected_by: "path" };
  const fallbackPaths = {
    "mount.cifs": ["/sbin/mount.cifs", "/usr/sbin/mount.cifs", "/bin/mount.cifs", "/usr/bin/mount.cifs"],
    mount: ["/bin/mount", "/usr/bin/mount"],
  }[command] ?? [];
  for (const candidate of fallbackPaths) {
    try {
      fs.accessSync(candidate, fs.constants.X_OK);
      return { command, available: true, path: candidate, detected_by: "well_known_path" };
    } catch {
      // keep searching
    }
  }
  return { command, available: false, path: null, detected_by: "not_found" };
}

function sudoNoPasswordStatus() {
  const result = runCommand("sudo", ["-n", "true"], { timeout: 3000 });
  return {
    command: "sudo -n true",
    available: result.exit_code === 0,
    exit_code: result.exit_code,
    stdout: result.stdout,
    stderr: result.stderr,
  };
}

function statusFromExit(result) {
  return result.exit_code === 0 ? "passed" : "failed";
}

async function assess() {
  const ping = runCommand("ping", ["-c", "2", "-W", "1", nasServer], { timeout: 5000 });
  const tcp445 = await tcpProbe(nasServer, 445);
  const findmnt = runCommand("findmnt", ["-no", "TARGET,SOURCE,FSTYPE,OPTIONS", mountPath], { timeout: 5000 });
  const findmntStatus = commandStatus("findmnt");
  const mountStatus = commandStatus("mount");
  const mountCifsStatus = commandStatus("mount.cifs");
  const pingStatus = commandStatus("ping");
  const sudoNoPassword = sudoNoPasswordStatus();
  const parsedMount = parseFindmnt(findmnt.stdout);
  const preflight = fs.existsSync(path.join(repoRoot, preflightScript))
    ? runCommand("bash", [preflightScript], {
        timeout: 10000,
        env: { NAS_UNC: nasUnc, MOUNT_PATH: mountPath, NAS_BACKUP_TARGET_DIR: mountPath },
      })
    : { exit_code: 127, stdout: "", stderr: `${preflightScript} missing` };
  const mountActive = parsedMount.exact_expected_cifs === true;
  const preflightPassed = preflight.exit_code === 0;
  const tcpPassed = tcp445.status === "passed";
  const pingPassed = ping.exit_code === 0;
  const rootCause = mountActive && preflightPassed
    ? "expected CIFS mount is active and preflight passed; production manifest still needs owner/reviewer evidence"
    : tcpPassed && mountCifsStatus.available && !sudoNoPassword.available
      ? "NAS host and SMB port are reachable, mount.cifs exists, but the expected CIFS mount is inactive and the current user has no passwordless sudo; IT must execute the controlled mount handoff"
      : tcpPassed && !mountCifsStatus.available
        ? "NAS host and SMB port are reachable, but mount.cifs is not available to this environment; IT must install/repair cifs-utils before the controlled mount"
        : tcpPassed
          ? "NAS host and SMB port are reachable, but the expected CIFS mount is not active or preflight failed on 160"
          : pingPassed
            ? "NAS host responds to ping, but SMB port 445 is not reachable from 160"
            : "NAS host is not reachable from 160 by ping/SMB probe";

  return {
    diagnostic_version: "v0.1",
    generated_at: new Date().toISOString(),
    policy: {
      diagnostic_only: true,
      does_not_close_manifest: true,
      no_system_write: true,
      no_mount_attempt: true,
      no_plaintext_secret_collection: true,
      credential_ref_only: true,
    },
    expected: {
      nas_server: nasServer,
      nas_share: nasShare,
      nas_unc: nasUnc,
      mount_path: mountPath,
      credential_file: credentialFile,
      credential_ref: credentialRef,
    },
    environment: {
      findmnt: findmntStatus,
      mount: mountStatus,
      mount_cifs: mountCifsStatus,
      ping: pingStatus,
      sudo_no_password: sudoNoPassword,
    },
    checks: {
      ping: { status: statusFromExit(ping), ...ping },
      tcp_445: tcp445,
      findmnt: { status: statusFromExit(findmnt), ...findmnt, parsed: parsedMount },
      mount_path_state: statPath(mountPath),
      credential_file_metadata: credentialMetadata(),
      preflight: { status: statusFromExit(preflight), ...preflight },
    },
    diagnostic_summary: {
      network_ping_reachable: pingPassed,
      smb_445_reachable: tcpPassed,
      expected_cifs_mount_active: mountActive,
      preflight_passed: preflightPassed,
      live_status: mountActive && preflightPassed ? "passed" : "failed",
      root_cause: rootCause,
      production_manifest_can_close: false,
      next_action: mountActive && preflightPassed
        ? "IT must backfill approved production manifests for automount, schedule, alert and drill evidence"
        : !mountCifsStatus.available
          ? "IT must install/repair cifs-utils or mount.cifs first, then run the controlled mount handoff during an approved maintenance window and regenerate NAS drift and diagnostic reports"
          : !sudoNoPassword.available
            ? "IT must use an approved sudo/root maintenance window to run the controlled mount handoff, then regenerate NAS drift and diagnostic reports"
            : "IT must run the controlled mount handoff during an approved maintenance window, then regenerate NAS drift and diagnostic reports",
    },
  };
}

function markerFor(report) {
  if (report.diagnostic_summary.expected_cifs_mount_active) return "NAS_DIAGNOSTIC_EXPECTED_CIFS_MOUNT_ACTIVE";
  if (report.diagnostic_summary.smb_445_reachable) return "NAS_DIAGNOSTIC_NETWORK_REACHABLE_MOUNT_INACTIVE";
  return "NAS_DIAGNOSTIC_NETWORK_OR_SMB_UNREACHABLE";
}

function renderMd(report) {
  const summary = report.diagnostic_summary;
  const checks = report.checks;
  return normalize(`# NAS 只读诊断报告 V0.1

## 1. 机器标记

- NAS_DIAGNOSTIC_REPORT_READY
- ${markerFor(report)}
- NAS_DIAGNOSTIC_NO_SYSTEM_WRITE
- NAS_DIAGNOSTIC_NO_MOUNT_ATTEMPT
- NAS_DIAGNOSTIC_NO_PLAINTEXT_SECRET_COLLECTION
- NAS_DIAGNOSTIC_DOES_NOT_CLOSE_PRODUCTION_MANIFEST

## 2. 结论

当前 NAS 诊断状态为 \`${summary.live_status}\`。本报告只证明 160 到 NAS 的网络、端口、160 本地挂载点和预检状态；它不关闭 \`PRD-NAS-AUTOMOUNT-001\`、\`PRD-NAS-SCHEDULE-001\`、\`PRD-NAS-ALERT-001\`、\`PRD-NAS-DRILL-CADENCE-001\` 或 \`SEC-P0-OPS-RECOVERY-001\`。

根因判断：${summary.root_cause}。

## 3. 期望配置

| 字段 | 值 |
|---|---|
| NAS server | \`${report.expected.nas_server}\` |
| NAS share | \`${report.expected.nas_share}\` |
| NAS SMB 地址（真实地址） | \`${report.expected.nas_unc}\` |
| 160 本地挂载点（不是 NAS 地址） | \`${report.expected.mount_path}\` |
| credential ref | \`${report.expected.credential_ref}\` |
| credential file | \`${report.expected.credential_file}\` |

## 4. 只读检查结果

| 检查项 | 状态 | 关键输出 |
|---|---|---|
| ping NAS | \`${checks.ping.status}\` | \`${(checks.ping.stdout || checks.ping.stderr || "empty").split("\n")[0]}\` |
| TCP 445 | \`${checks.tcp_445.status}\` | \`${checks.tcp_445.error || `${checks.tcp_445.elapsed_ms}ms`}\` |
| findmnt exact path | \`${checks.findmnt.status}\` | \`${checks.findmnt.stdout || checks.findmnt.stderr || "empty"}\` |
| expected CIFS mount active | \`${summary.expected_cifs_mount_active}\` | source=\`${checks.findmnt.parsed?.source ?? "missing"}\`, fstype=\`${checks.findmnt.parsed?.fstype ?? "missing"}\` |
| 160 本地挂载点存在 | \`${checks.mount_path_state.exists}\` | mode=\`${checks.mount_path_state.mode ?? "missing"}\`, uid=\`${checks.mount_path_state.uid ?? "missing"}\`, gid=\`${checks.mount_path_state.gid ?? "missing"}\` |
| credential file metadata | \`${checks.credential_file_metadata.exists}\` | mode=\`${checks.credential_file_metadata.mode ?? "missing"}\`, readable_by_current_user=\`${checks.credential_file_metadata.readable_by_current_user}\`, content_collected=\`${checks.credential_file_metadata.content_collected}\` |
| mount.cifs helper | \`${report.environment.mount_cifs.available}\` | path=\`${report.environment.mount_cifs.path ?? "missing"}\`, detected_by=\`${report.environment.mount_cifs.detected_by}\` |
| sudo no-password | \`${report.environment.sudo_no_password.available}\` | exit_code=\`${report.environment.sudo_no_password.exit_code}\`, stderr=\`${report.environment.sudo_no_password.stderr || "empty"}\` |
| preflight | \`${checks.preflight.status}\` | \`${checks.preflight.stderr || checks.preflight.stdout || "empty"}\` |

## 5. IT 下一步

1. 若本报告显示 \`NAS_DIAGNOSTIC_NETWORK_REACHABLE_MOUNT_INACTIVE\`，说明 NAS 主机和 SMB 端口可达，优先处理 160 的 CIFS 挂载或开机自动挂载。
2. IT 在批准维护窗口执行 \`docs/nas-it-controlled-mount-handoff-v0.1.md\` 中的 dry-run 和 \`EXECUTE_MOUNT=true\` 流程。
3. 执行后依次运行：

\`\`\`bash
npm run docs:nas-diagnostics
npm run verify:nas-diagnostics
npm run docs:nas-current-state-drift
npm run verify:nas-current-state-drift
npm run docs:closed-loop-current-state
npm run verify:delivery-gates
\`\`\`

4. 真实生产通过后仍需按 manifest 模板回填外部审批、截图、日志、告警和恢复演练链接；本诊断 JSON 只能作为 IT 排障证据之一。
`);
}

const report = await assess();
const json = normalize(JSON.stringify(report, null, 2));
const md = renderMd(report);

if (checkMode) {
  const existingJsonText = readText(jsonPath);
  const existingMd = readText(mdPath);
  if (!existingJsonText) failures.push(`${jsonPath} is missing; run npm run docs:nas-diagnostics`);
  if (!existingMd) failures.push(`${mdPath} is missing; run npm run docs:nas-diagnostics`);
  if (existingJsonText) {
    const existing = JSON.parse(existingJsonText);
    for (const field of ["network_ping_reachable", "smb_445_reachable", "expected_cifs_mount_active", "preflight_passed", "live_status"]) {
      if (existing.diagnostic_summary?.[field] !== report.diagnostic_summary[field]) {
        failures.push(`${jsonPath} ${field} is ${existing.diagnostic_summary?.[field]}, current is ${report.diagnostic_summary[field]}; rerun npm run docs:nas-diagnostics`);
      }
    }
  }
  for (const marker of [
    "NAS_DIAGNOSTIC_REPORT_READY",
    markerFor(report),
    "NAS_DIAGNOSTIC_NO_SYSTEM_WRITE",
    "NAS_DIAGNOSTIC_NO_MOUNT_ATTEMPT",
    "NAS_DIAGNOSTIC_NO_PLAINTEXT_SECRET_COLLECTION",
    "NAS_DIAGNOSTIC_DOES_NOT_CLOSE_PRODUCTION_MANIFEST",
  ]) {
    if (!existingMd.includes(marker)) failures.push(`${mdPath} must include marker ${marker}`);
  }
  if (failures.length > 0) {
    console.error("NAS_DIAGNOSTIC_REPORT_FAIL");
    for (const failure of failures) console.error(`- ${failure}`);
    process.exit(1);
  }
  console.log(`NAS_DIAGNOSTIC_REPORT_PASS: ${report.diagnostic_summary.root_cause}`);
} else {
  fs.mkdirSync(path.dirname(path.join(repoRoot, jsonPath)), { recursive: true });
  fs.writeFileSync(path.join(repoRoot, jsonPath), json, "utf8");
  fs.writeFileSync(path.join(repoRoot, mdPath), md, "utf8");
  console.log(`generated ${jsonPath}`);
  console.log(`generated ${mdPath}`);
  console.log(`diagnostic status: ${report.diagnostic_summary.live_status}`);
  console.log(`root cause: ${report.diagnostic_summary.root_cause}`);
}
