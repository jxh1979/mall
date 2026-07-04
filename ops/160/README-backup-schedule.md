# Complex 备份 / 恢复演练 / 告警调度（160 生产就绪）

面向运维的可复现安装说明。所有调度用 **systemd 用户级 timer**（linger 已开，无需 sudo，
与 `complex-preview` / `complex-portal` 一致）。安装脚本幂等，可回滚。

## 组成

| 文件 | 作用 |
|---|---|
| `install-backup-schedule.sh` | 一键安装/卸载（`--uninstall`）。部署 guard 脚本 + 安装并启用 timer。 |
| `complex-backup-guard.sh` | 备份包装器：跑 `complex-backup.sh daily`（强制 NAS 同步）+ 备份前后 NAS 容量检查 + 失败/超阈值告警。 |
| `complex-restore-drill-guard.sh` | 恢复演练包装器：跑 `complex-restore-drill.sh latest`（kind=daily）+ 失败告警。 |
| `complex-alert-dispatch.sh` | 告警分发框架：事件写本地告警日志；若存在可执行的投递钩子则转发（真实投递端待运维填）。 |
| `complex-backup.{service,timer}` | 每日 03:30 触发备份。 |
| `complex-restore-drill.{service,timer}` | 每周一 04:30 触发恢复演练。 |

## 安装

```bash
cd /home/james/complex-management/ops/160   # 或本 worktree 的 ops/160
./install-backup-schedule.sh
```

## 卸载 / 回滚

```bash
./install-backup-schedule.sh --uninstall
# 或手动：
systemctl --user disable --now complex-backup.timer complex-restore-drill.timer
rm -f ~/.config/systemd/user/complex-backup.* ~/.config/systemd/user/complex-restore-drill.*
systemctl --user daemon-reload
```

## 调度周期取值与依据

- **备份 = 每日 03:30**：`complex-backup-schedule.example` 的 `CADENCE=daily`；03:30 为低峰。
  `Persistent=true` → 关机错过则开机补跑。
- **恢复演练 = 每周一 04:30**：任务要求"低频（如每周）"；排在每日备份窗口之后，验证最新 daily 备份可还原。
- 两个 service 均带 `ConditionPathIsMountPoint=/mnt/complex-nas-backup`：NAS（`nofail` 挂载）掉线时
  干净跳过而非报错刷屏。

## 备份口径（实读脚本确认）

- **kind=`daily`**：`complex-backup.sh` 只接受 `manual|daily|pre-deploy|pre-rollback|p0-final-clean`。
  （注：`complex-backup-schedule.example` 里的 `scheduled` 是无效 kind，本调度改用 `daily`。）
- **加密开启**：`~/.complex/backup.passphrase`(600) 存在，脚本默认 AES-256 加密，产出 `*.tar.gz.enc + .sha256`。
  恢复演练脚本会自动解密校验。未设置 `ALLOW_UNENCRYPTED_BACKUPS`。
  （`complex.env.example` 记录的"生产不加密"决策与此相反 —— **待用户/IT 拍板**：若确定不加密，
  安装前 export `ALLOW_UNENCRYPTED_BACKUPS=true` 并去掉 passphrase 文件。当前取更安全的加密默认。）
- **NAS 同步**：`NAS_BACKUP_TARGET_DIR=/mnt/complex-nas-backup`、`REQUIRE_NAS_BACKUP_SYNC=true`。
- **保留**：`complex-backup.sh` 对 `daily` 硬编码删除 14 天前归档（非模板里的 30 天）。
  # ponytail: 保留 = 脚本内 14 天硬编码。若要 30 天，改脚本里的 `-mtime +14`，非本调度层职责。

## 告警框架（未接真实投递）

`complex-alert-dispatch.sh <event> <message>`：
- 事件写 `~/.complex/logs/backup-alerts.log`（时间戳 + 事件 + 消息）。
- 若 `/opt/complex/scripts/complex-alert-delivery.sh` 可执行 → 转发给它；否则记 `alert_delivery_pending`。

事件：`backup_failed` / `capacity_threshold_exceeded` / `restore_drill_failed`
（对应 `complex-backup-alert.example`）。容量阈值默认 **warn 80% / critical 90%**
（取模板显式值，非自造）。可用 `CAPACITY_WARNING_PERCENT` / `CAPACITY_CRITICAL_PERCENT` 覆盖。

### 待运维填（本代理无法产出）
1. **真实告警投递端**：在 `/opt/complex/scripts/complex-alert-delivery.sh` 放一个可执行脚本，
   读同样 `$1=event $2=message`，用凭证台账里的 `cred://prod/alert/wecom/...` 或
   `cred://prod/alert/email/...` 推到企微/邮件。**不要把 webhook/邮箱硬编码进仓库。**
2. **生产凭证发放证据**（责任人/到期/credential_ref/轮换）—— 见 findings `SEC-P0-OPS-RECOVERY-001` 仍 open。

## 验证

```bash
systemctl --user list-timers 'complex-*' --all      # 看下次触发时间
systemctl --user start complex-backup.service        # 手动触发一次
journalctl --user -u complex-backup.service -n 50    # 看日志
cat ~/.complex/logs/backup-alerts.log                # 看告警日志
```
