# NAS IT 受控挂载交接包 V0.1

## 1. 机器标记

- NAS_IT_CONTROLLED_MOUNT_HANDOFF_READY
- VERIFY_NAS_IT_HANDOFF_READY
- DEFAULT_DRY_RUN_NO_SYSTEM_WRITE
- NO_PLAINTEXT_SECRET_COLLECTION
- MANIFEST_EVIDENCE_MAPPING_READY
- SEC_P0_OPS_RECOVERY_REMAINS_BLOCKED_UNTIL_LIVE_PASS

## 2. 定位

本文把 NAS 当前阻塞从“知道要做”收口为 IT 可以执行、测试可以复核、生产证据可以回填的交接包。它不替代真实生产证据，也不在仓库内保存原始截图、密码、token 或 SMB 密码。

当前权威事实仍以 `docs/nas-current-state-drift-report-v0.1.md` 和 `docs/production-readiness-evidence/nas-current-state-drift-latest.json` 为准：历史上已经完成过一次 NAS 同步和恢复演练，但当前 live check 失败，`/mnt/complex-nas-backup` 不是有效 CIFS 挂载点，因此 `SEC-P0-OPS-RECOVERY-001` 不能关闭。

## 3. 交付文件

| 文件 | 用途 |
|---|---|
| `ops/160/complex-nas-system-mount.example.sh` | IT 维护窗口使用的受控挂载模板；默认 dry-run，不写系统状态 |
| `ops/160/complex-nas-preflight.sh` | 用户级挂载可用性检查：findmnt、容量、读写删除 |
| `docs/nas-current-state-drift-report-v0.1.md` | 当前 live 状态和历史证据漂移报告 |
| `docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-AUTOMOUNT-001.json` | 自动挂载生产证据 manifest 模板 |
| `docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P0-OPS-RECOVERY-001.json` | P0 运维恢复闭环 manifest 模板 |

## 4. IT 执行步骤

### 4.1 Dry-run 预览

```bash
cd /home/james/complex-management
bash ops/160/complex-nas-system-mount.example.sh
```

预期输出包含 `NAS_IT_CONTROLLED_MOUNT_DRY_RUN`，并打印受控维护窗口命令。Dry-run 不创建目录、不挂载、不写凭证、不改 systemd、不写 `/etc/fstab`。

### 4.2 受控维护窗口执行

由 IT/运维在 160 上确认 `/etc/complex/secrets/nas-smb.credentials` 已存在、权限为 `600` 或 `400`、内容由安全渠道写入，并确认 `mount.cifs`/`cifs-utils` 可用后，再执行：

```bash
cd /home/james/complex-management
sudo env EXECUTE_MOUNT=true   NAS_UNC='//192.168.88.113/bn'   MOUNT_PATH='/mnt/complex-nas-backup'   NAS_SMB_CREDENTIALS_FILE='/etc/complex/secrets/nas-smb.credentials'   MOUNT_USER='james'   MOUNT_GROUP='james'   SMB_VERSION='3.0'   EVIDENCE_DIR='/tmp/complex-nas-mount-evidence'   bash ops/160/complex-nas-system-mount.example.sh
```

通过标准：输出 `NAS_IT_CONTROLLED_MOUNT_PASS`，并生成一个脱敏 evidence JSON。该 JSON 只包含 `credential_ref=cred://prod/nas/smb/complex-backup-rw`，不得包含 SMB 密码。

### 4.3 刷新仓库当前态

```bash
npm run docs:nas-current-state-drift
npm run verify:nas-current-state-drift
npm run docs:closed-loop-current-state
npm run verify:closed-loop-current-state
npm run verify:delivery-gates
```

如果 `docs/nas-current-state-drift-report-v0.1.md` 仍显示 `CURRENT_LIVE_NAS_CHECK_FAILED`，不得提交 NAS 自动挂载通过的生产 manifest。

## 5. 生产证据回填映射

| item_id | 必须回填的外部证据 | 最低通过口径 |
|---|---|---|
| `PRD-NAS-AUTOMOUNT-001` | 受控挂载命令输出、重启后 `findmnt`、读写删除测试、审批记录 | 当前 live check passed，且 evidence 链接可追溯 |
| `PRD-NAS-SCHEDULE-001` | 定时备份配置导出、最近一次 scheduled 备份日志、sha256 校验 | scheduled 备份归档和校验都 passed |
| `PRD-NAS-ALERT-001` | 容量告警阈值、备份失败告警、通知送达记录 | 至少一次容量或失败告警演练通过 |
| `PRD-NAS-DRILL-CADENCE-001` | 周期恢复演练计划、最近一次恢复演练报告、下次复测日期 | 恢复演练 passed，且 next_retest_due 合法 |
| `SEC-P0-OPS-RECOVERY-001` | NAS、160 systemd/配置、凭证轮换、告警、恢复演练全链路证据 | 所有关联 P0 证据均 valid 后才可关闭 |

## 6. Manifest 填写规则

1. 只把 `manifest-<item_id>.json` 放入 `docs/production-readiness-evidence/submissions/...` 对应目录。
2. 原始截图、日志、审批单、导出文件继续放在外部受控系统，通过 `evidence_links` 引用。
3. 密钥只写 `credential_refs`，格式必须是 `cred://prod/...`。
4. `owner_person` 与 `reviewer` 不能相同。
5. `result` 只有真实通过后才能写 `passed`；未完成或失败必须保持 `blocked`。
6. 提交后必须运行：

```bash
npm run verify:production-evidence-submissions-guard
npm run docs:production-evidence-submission-status
npm run verify:production-evidence-submission-status
npm run docs:production-evidence-backfill-priority
npm run docs:production-evidence-backfill-execution-ledger
npm run verify:delivery-gates
```

## 7. 不允许事项

- 不允许把历史 `nas-smb-mount-latest.json` 当作当前生产自动挂载证据。
- 不允许把 `/tmp/complex-nas-mount-evidence/*.json` 直接当作生产审批完成证据；它只是 IT 回填 manifest 的一个输入。
- 不允许在仓库、聊天、日志、manifest 或文档里写 SMB 密码。
- 不允许由 Codex 在无维护窗口、无 sudo 审批、无 IT 责任人的情况下改 `/etc/fstab`、systemd mount/timer 或生产凭证。
- 不允许忽略 `mount.cifs`/`cifs-utils` 缺失或当前用户无免密 sudo 的事实；这些必须由 IT 在维护窗口处理。
