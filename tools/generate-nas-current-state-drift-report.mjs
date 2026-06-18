import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check");
const jsonPath = "docs/production-readiness-evidence/nas-current-state-drift-latest.json";
const mdPath = "docs/nas-current-state-drift-report-v0.1.md";
const historicalEvidencePath = "docs/production-readiness-evidence/nas-smb-mount-latest.json";
const restoreEvidencePath = "docs/production-readiness-evidence/nas-restore-drill-latest.json";
const mountPath = process.env.NAS_MOUNT_PATH ?? "/mnt/complex-nas-backup";
const expectedUnc = process.env.NAS_UNC ?? "//192.168.88.113/bn";
const expectedUser = process.env.NAS_SMB_USERNAME ?? "jxh1";
const credentialFile = process.env.NAS_SMB_CREDENTIALS_FILE ?? "/etc/complex/secrets/nas-smb.credentials";
const failures = [];

function readText(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath);
  if (!fs.existsSync(absolutePath)) return "";
  return fs.readFileSync(absolutePath, "utf8").replace(/^\uFEFF/, "");
}

function readJson(relativePath) {
  const text = readText(relativePath);
  if (!text) return null;
  return JSON.parse(text);
}

function normalize(text) {
  return text.replace(/\r\n/g, "\n").trimEnd() + "\n";
}

function runFindmnt() {
  try {
    return {
      exit_code: 0,
      stdout: execFileSync("findmnt", ["-no", "TARGET,SOURCE,FSTYPE,OPTIONS", mountPath], { encoding: "utf8" }).trim(),
      stderr: "",
    };
  } catch (error) {
    return {
      exit_code: error.status ?? 1,
      stdout: String(error.stdout ?? "").trim(),
      stderr: String(error.stderr ?? error.message ?? "").trim(),
    };
  }
}

function currentUserCanReadCredentialFile() {
  try {
    fs.accessSync(credentialFile, fs.constants.R_OK);
    return true;
  } catch {
    return false;
  }
}

function mountPathState() {
  try {
    const stat = fs.statSync(mountPath);
    return {
      exists: true,
      mode: (stat.mode & 0o777).toString(8),
      uid: stat.uid,
      gid: stat.gid,
      is_directory: stat.isDirectory(),
    };
  } catch (error) {
    return { exists: false, error: error.message };
  }
}

function writeReadDeleteCheck() {
  if (!fs.existsSync(mountPath)) return { status: "skipped", error: "mount path does not exist" };
  const filePath = path.join(mountPath, `.complex-nas-current-state-${process.pid}`);
  try {
    fs.writeFileSync(filePath, "complex nas current state check\n", { mode: 0o600 });
    const readBack = fs.readFileSync(filePath, "utf8");
    if (readBack !== "complex nas current state check\n") return { status: "failed", error: "read back mismatch" };
    fs.rmSync(filePath);
    return { status: "passed" };
  } catch (error) {
    return { status: "failed", error: error.message };
  } finally {
    try {
      if (fs.existsSync(filePath)) fs.rmSync(filePath);
    } catch {
      // best effort cleanup only
    }
  }
}

function assess() {
  const historical = readJson(historicalEvidencePath);
  const restore = readJson(restoreEvidencePath);
  const findmnt = runFindmnt();
  const pathState = mountPathState();
  const mounted = findmnt.exit_code === 0 && findmnt.stdout.includes(expectedUnc) && findmnt.stdout.includes("cifs");
  const writeCheck = mounted
    ? writeReadDeleteCheck()
    : { status: "skipped", error: "expected CIFS NAS mount is not active; write/read/delete check intentionally skipped" };
  const writable = writeCheck.status === "passed";
  const currentLiveStatus = mounted && writable ? "passed" : "failed";
  return {
    status_version: "v0.1",
    generated_at: new Date().toISOString(),
    report_policy: {
      does_not_close_manifest: true,
      historical_passed_evidence_is_not_current_production_closure: true,
      current_live_failure_keeps_nas_manifest_awaiting: true,
      no_plaintext_secret_collection: true,
    },
    expected: {
      mount_path: mountPath,
      nas_unc: expectedUnc,
      smb_username: expectedUser,
      credential_file: credentialFile,
      credential_ref: "cred://prod/nas/smb/complex-backup-rw",
    },
    historical_evidence: {
      mount_evidence_path: historicalEvidencePath,
      restore_evidence_path: restoreEvidencePath,
      mount_status: historical?.status ?? "missing",
      mount_verified_at: historical?.verified_at ?? null,
      restore_status: restore?.verification_result ?? "missing",
      restore_finished_at: restore?.finished_at ?? null,
    },
    current_state: {
      live_status: currentLiveStatus,
      findmnt,
      mount_path_state: pathState,
      write_read_delete_check: writeCheck,
      credential_file_readable_by_current_user: currentUserCanReadCredentialFile(),
    },
    production_closure_assessment: {
      can_close_prd_nas_automount: false,
      can_close_prd_nas_schedule: false,
      can_close_prd_nas_alert: false,
      can_close_prd_nas_drill_cadence: false,
      can_close_sec_p0_ops_recovery: false,
      reason: currentLiveStatus === "passed"
        ? "current live mount is usable, but production manifest still needs IT owner, reviewer, approval, scheduled backup, alert and cadence evidence"
        : "current live mount check failed, and historical passed evidence cannot prove current automount or production recovery readiness",
    },
  };
}

function statusMarker(report) {
  return report.current_state.live_status === "passed" ? "CURRENT_LIVE_NAS_CHECK_PASSED" : "CURRENT_LIVE_NAS_CHECK_FAILED";
}

function renderMd(report) {
  const current = report.current_state;
  return normalize(`# NAS 当前状态漂移报告 V0.1

## 1. 机器标记

- NAS_CURRENT_STATE_DRIFT_REPORT_READY
- HISTORICAL_NAS_EVIDENCE_NOT_CURRENT_CLOSURE
- ${statusMarker(report)}
- NO_FAKE_NAS_PRODUCTION_MANIFEST
- NAS_MANIFEST_REMAINS_AWAITING_IT_EVIDENCE

## 2. 结论

当前 NAS live check 状态为 \`${current.live_status}\`。历史文件 \`${historicalEvidencePath}\` 记录的是一次性挂载读写验证，不能替代当前生产自动挂载、定时备份、容量/失败告警、周期恢复演练和真实 manifest 审批证据。

因此，本报告不关闭任何 NAS 或 SEC-P0-OPS-RECOVERY manifest；它只用于防止把历史 \`passed\` evidence 误读为当前生产闭环完成。

## 3. 历史证据

| 字段 | 值 |
|---|---|
| 挂载 evidence | \`${historicalEvidencePath}\` |
| 挂载历史状态 | \`${report.historical_evidence.mount_status}\` |
| 挂载历史验证时间 | \`${report.historical_evidence.mount_verified_at ?? "missing"}\` |
| 恢复 evidence | \`${restoreEvidencePath}\` |
| 恢复历史状态 | \`${report.historical_evidence.restore_status}\` |
| 恢复历史完成时间 | \`${report.historical_evidence.restore_finished_at ?? "missing"}\` |

## 4. 当前 live check

| 检查项 | 当前结果 |
|---|---|
| findmnt exit_code | \`${current.findmnt.exit_code}\` |
| findmnt stdout | \`${current.findmnt.stdout || "empty"}\` |
| mount_path | \`${report.expected.mount_path}\` |
| mount_path exists | \`${current.mount_path_state.exists}\` |
| write/read/delete | \`${current.write_read_delete_check.status}\` |
| write/read/delete error | \`${current.write_read_delete_check.error ?? ""}\` |
| credential_file_readable_by_current_user | \`${current.credential_file_readable_by_current_user}\` |

## 5. 生产闭环影响

| item_id | 当前结论 |
|---|---|
| PRD-NAS-AUTOMOUNT-001 | 不可关闭：需要 IT 系统级自动挂载、重启后 findmnt、读写删除测试和审批证据 |
| PRD-NAS-SCHEDULE-001 | 不可关闭：需要 scheduled 备份归档、sha256、日志和恢复证据 |
| PRD-NAS-ALERT-001 | 不可关闭：需要容量告警、失败告警和通知送达证据 |
| PRD-NAS-DRILL-CADENCE-001 | 不可关闭：需要周期恢复演练计划、下次复测日期和复核记录 |
| SEC-P0-OPS-RECOVERY-001 | 不可关闭：需要 NAS、160 systemd/配置、凭证轮换、告警和恢复演练全链路证据 |

## 6. 下一步

1. IT/运维重新完成受控 SMB 挂载或开机自动挂载配置。
2. 运行 \`npm run verify:nas-smb-mount-evidence\` 复核 live mount。
3. 运行 \`npm run docs:nas-current-state-drift\` 刷新本报告。
4. 真实生产 manifest 仍按 \`docs/production-evidence-backfill-execution-ledger-v0.1.md\` 的 target path 回填，只写外部证据链接和 \`cred://...\` 引用。
`);
}

const report = assess();
const json = normalize(JSON.stringify(report, null, 2));
const md = renderMd(report);

if (checkMode) {
  const currentJson = readText(jsonPath);
  const currentMd = readText(mdPath);
  if (!currentJson) failures.push(`${jsonPath} is missing; run npm run docs:nas-current-state-drift`);
  if (!currentMd) failures.push(`${mdPath} is missing; run npm run docs:nas-current-state-drift`);
  if (currentJson) {
    const existing = JSON.parse(currentJson);
    if (existing.current_state?.live_status !== report.current_state.live_status) {
      failures.push(`${jsonPath} live_status is ${existing.current_state?.live_status}, current live_status is ${report.current_state.live_status}; rerun npm run docs:nas-current-state-drift`);
    }
  }
  for (const marker of [
    "NAS_CURRENT_STATE_DRIFT_REPORT_READY",
    "HISTORICAL_NAS_EVIDENCE_NOT_CURRENT_CLOSURE",
    statusMarker(report),
    "NO_FAKE_NAS_PRODUCTION_MANIFEST",
    "NAS_MANIFEST_REMAINS_AWAITING_IT_EVIDENCE",
  ]) {
    if (!currentMd.includes(marker)) failures.push(`${mdPath} must include marker ${marker}`);
  }
  if (failures.length > 0) {
    console.error("NAS_CURRENT_STATE_DRIFT_REPORT_FAIL");
    for (const failure of failures) console.error(`- ${failure}`);
    process.exit(1);
  }
  console.log(`NAS_CURRENT_STATE_DRIFT_REPORT_PASS: current live status ${report.current_state.live_status}`);
} else {
  fs.mkdirSync(path.dirname(path.join(repoRoot, jsonPath)), { recursive: true });
  fs.writeFileSync(path.join(repoRoot, jsonPath), json, "utf8");
  fs.writeFileSync(path.join(repoRoot, mdPath), md, "utf8");
  console.log(`generated ${jsonPath}`);
  console.log(`generated ${mdPath}`);
  console.log(`current live status: ${report.current_state.live_status}`);
}
