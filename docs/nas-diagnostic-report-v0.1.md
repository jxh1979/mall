# NAS 只读诊断报告 V0.1

## 1. 机器标记

- NAS_DIAGNOSTIC_REPORT_READY
- NAS_DIAGNOSTIC_EXPECTED_CIFS_MOUNT_ACTIVE
- NAS_DIAGNOSTIC_NO_SYSTEM_WRITE
- NAS_DIAGNOSTIC_NO_MOUNT_ATTEMPT
- NAS_DIAGNOSTIC_NO_PLAINTEXT_SECRET_COLLECTION
- NAS_DIAGNOSTIC_DOES_NOT_CLOSE_PRODUCTION_MANIFEST

## 2. 结论

当前 NAS 诊断状态为 `passed`。本报告只证明 160 到 NAS 的网络、端口、160 本地挂载点和预检状态；它不关闭 `PRD-NAS-AUTOMOUNT-001`、`PRD-NAS-SCHEDULE-001`、`PRD-NAS-ALERT-001`、`PRD-NAS-DRILL-CADENCE-001` 或 `SEC-P0-OPS-RECOVERY-001`。

根因判断：expected CIFS mount is active and preflight passed; production manifest still needs owner/reviewer evidence。

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
| TCP 445 | `passed` | `10ms` |
| findmnt exact path | `passed` | `/mnt/complex-nas-backup //192.168.88.113/bn cifs   rw,relatime,vers=3.0,sec=ntlmssp,cache=strict,upcall_target=app,username=jxh1,uid=1000,forceuid,gid=1000,forcegid,addr=192.168.88.113,file_mode=0600,dir_mode=0700,iocharset=utf8,soft,nounix,serverino,mapposix,noperm,rsize=4194304,wsize=4194304,bsize=1048576,retrans=1,echo_interval=60,actimeo=1,closetimeo=1` |
| expected CIFS mount active | `true` | source=`//192.168.88.113/bn`, fstype=`cifs` |
| 160 本地挂载点存在 | `true` | mode=`700`, uid=`1000`, gid=`1000` |
| credential file metadata | `false` | mode=`missing`, readable_by_current_user=`false`, content_collected=`false` |
| mount.cifs helper | `true` | path=`/sbin/mount.cifs`, detected_by=`well_known_path` |
| sudo no-password | `false` | exit_code=`1`, stderr=`sudo: a password is required` |
| preflight | `passed` | `P0A_NAS_PREFLIGHT PASS
mount_path=/mnt/complex-nas-backup
nas_backup_target_dir=/mnt/complex-nas-backup
retention_days=30
capacity_limit_bytes=1099511627776
free_bytes=31509759680512
nas_unc=//192.168.88.113/bn
mount_info=/mnt/complex-nas-backup //192.168.88.113/bn cifs   rw,relatime,vers=3.0,sec=ntlmssp,cache=strict,upcall_target=app,username=jxh1,uid=1000,forceuid,gid=1000,forcegid,addr=192.168.88.113,file_mode=0600,dir_mode=0700,iocharset=utf8,soft,nounix,serverino,mapposix,noperm,rsize=4194304,wsize=4194304,bsize=1048576,retrans=1,echo_interval=60,actimeo=1,closetimeo=1` |

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
