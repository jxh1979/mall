# NAS 当前状态漂移报告 V0.1

## 1. 机器标记

- NAS_CURRENT_STATE_DRIFT_REPORT_READY
- HISTORICAL_NAS_EVIDENCE_NOT_CURRENT_CLOSURE
- CURRENT_LIVE_NAS_CHECK_PASSED
- NO_FAKE_NAS_PRODUCTION_MANIFEST
- NAS_MANIFEST_REMAINS_AWAITING_IT_EVIDENCE

## 2. 结论

当前 NAS live check 状态为 `passed`。历史文件 `docs/production-readiness-evidence/nas-smb-mount-latest.json` 记录的是一次性挂载读写验证，不能替代当前生产自动挂载、定时备份、容量/失败告警、周期恢复演练和真实 manifest 审批证据。

因此，本报告不关闭任何 NAS 或 SEC-P0-OPS-RECOVERY manifest；它只用于防止把历史 `passed` evidence 误读为当前生产闭环完成。

## 3. 历史证据

| 字段 | 值 |
|---|---|
| 挂载 evidence | `docs/production-readiness-evidence/nas-smb-mount-latest.json` |
| 挂载历史状态 | `passed` |
| 挂载历史验证时间 | `2026-06-15T19:56:41Z` |
| 恢复 evidence | `docs/production-readiness-evidence/nas-restore-drill-latest.json` |
| 恢复历史状态 | `passed` |
| 恢复历史完成时间 | `2026-06-15T11:27:37+00:00` |

## 4. 当前 live check

| 检查项 | 当前结果 |
|---|---|
| findmnt exit_code | `0` |
| findmnt stdout | `/mnt/complex-nas-backup //192.168.88.113/bn cifs   rw,relatime,vers=3.0,sec=ntlmssp,cache=strict,upcall_target=app,username=jxh1,uid=1000,forceuid,gid=1000,forcegid,addr=192.168.88.113,file_mode=0600,dir_mode=0700,iocharset=utf8,soft,nounix,serverino,mapposix,noperm,rsize=4194304,wsize=4194304,bsize=1048576,retrans=1,echo_interval=60,actimeo=1,closetimeo=1` |
| mount_path | `/mnt/complex-nas-backup` |
| mount_path exists | `true` |
| write/read/delete | `passed` |
| write/read/delete error | `` |
| credential_file_readable_by_current_user | `false` |

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
2. 运行 `npm run verify:nas-smb-mount-evidence` 复核 live mount。
3. 运行 `npm run docs:nas-current-state-drift` 刷新本报告。
4. 真实生产 manifest 仍按 `docs/production-evidence-backfill-execution-ledger-v0.1.md` 的 target path 回填，只写外部证据链接和 `cred://...` 引用。
