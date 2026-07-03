# NAS 只读诊断报告 V0.1

## 1. 机器标记

- NAS_DIAGNOSTIC_REPORT_READY
- NAS_DIAGNOSTIC_NETWORK_REACHABLE_MOUNT_INACTIVE
- NAS_DIAGNOSTIC_NO_SYSTEM_WRITE
- NAS_DIAGNOSTIC_NO_MOUNT_ATTEMPT
- NAS_DIAGNOSTIC_NO_PLAINTEXT_SECRET_COLLECTION
- NAS_DIAGNOSTIC_DOES_NOT_CLOSE_PRODUCTION_MANIFEST

## 2. 结论

当前 NAS 诊断状态为 `failed`。本报告只证明 160 到 NAS 的网络、端口、160 本地挂载点和预检状态；它不关闭 `PRD-NAS-AUTOMOUNT-001`、`PRD-NAS-SCHEDULE-001`、`PRD-NAS-ALERT-001`、`PRD-NAS-DRILL-CADENCE-001` 或 `SEC-P0-OPS-RECOVERY-001`。

根因判断：NAS host and SMB port are reachable, mount.cifs exists, but the expected CIFS mount is inactive and the current user has no passwordless sudo; IT must execute the controlled mount handoff。

## 3. 期望配置

| 字段 | 值 |
|---|---|
| NAS server | `192.168.88.113` |
| NAS share | `bn` |
| NAS SMB 地址（真实地址） | `//192.168.88.113/bn` |
| 160 本地挂载点（不是 NAS 地址） | `/mnt/complex-nas-backup` |
| credential ref | `cred://prod/nas/smb/complex-backup-rw` |
| credential file | `/etc/complex/secrets/nas-smb.credentials` |

## 4. 只读检查结果

| 检查项 | 状态 | 关键输出 |
|---|---|---|
| ping NAS | `passed` | `PING 192.168.88.113 (192.168.88.113) 56(84) bytes of data.` |
| TCP 445 | `passed` | `17ms` |
| findmnt exact path | `failed` | `empty` |
| expected CIFS mount active | `false` | source=`missing`, fstype=`missing` |
| 160 本地挂载点存在 | `true` | mode=`755`, uid=`0`, gid=`0` |
| credential file metadata | `false` | mode=`missing`, readable_by_current_user=`false`, content_collected=`false` |
| mount.cifs helper | `true` | path=`/sbin/mount.cifs`, detected_by=`well_known_path` |
| sudo no-password | `false` | exit_code=`1`, stderr=`sudo: a password is required` |
| preflight | `failed` | `P0A_NAS_PREFLIGHT FAIL: mount path is not an active mount point: /mnt/complex-nas-backup` |

## 5. IT 下一步

1. 若本报告显示 `NAS_DIAGNOSTIC_NETWORK_REACHABLE_MOUNT_INACTIVE`，说明 NAS 主机和 SMB 端口可达，优先处理 160 的 CIFS 挂载或开机自动挂载。
2. IT 在批准维护窗口执行 `docs/nas-it-controlled-mount-handoff-v0.1.md` 中的 dry-run 和 `EXECUTE_MOUNT=true` 流程。
3. 执行后依次运行：

```bash
npm run docs:nas-diagnostics
npm run verify:nas-diagnostics
npm run docs:nas-current-state-drift
npm run verify:nas-current-state-drift
npm run docs:closed-loop-current-state
npm run verify:delivery-gates
```

4. 真实生产通过后仍需按 manifest 模板回填外部审批、截图、日志、告警和恢复演练链接；本诊断 JSON 只能作为 IT 排障证据之一。
