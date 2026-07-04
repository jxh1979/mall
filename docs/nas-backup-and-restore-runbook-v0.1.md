# NAS 备份与恢复 Runbook V0.1

## 1. 文档定位

本文用于收口生产环境 NAS 备份和恢复演练的最低可执行方案。P0 默认使用 NAS 作为备份目标，覆盖 PostgreSQL、对象存储、配置文件和关键运行脚本。未完成恢复演练前，不得判定真实生产可恢复。

已确认 NAS 服务器地址为 `192.168.88.113`，挂载协议采用 SMB，共享路径为 `//192.168.88.113/bn`。160 机器执行备份脚本时，不得把该 IP 直接当成本地目录使用；IT/运维需先把 `//192.168.88.113/<share>` 挂载到 160 的本地目录，再把 `NAS_BACKUP_TARGET_DIR` 指向该挂载目录。当前正式挂载目录为 `/mnt/complex-nas-backup`。SMB 账号、共享凭证和可选加密口令只进入生产凭证台账或安全保管位置，不得写入本文档；本文只记录账号名、凭证引用和验收证据。

## 2. 备份范围

| 资产 | P0 备份要求 |
|---|---|
| PostgreSQL | 每日全量备份，保留可恢复校验记录 |
| 对象存储 | 上传附件、证据文件、合同附件、图片等按日同步 |
| 配置文件 | `.env` 模板、Nginx 配置、systemd 配置、部署配置脱敏归档 |
| 运行脚本 | `ops/160/*.sh`、发布脚本、回滚脚本、健康检查脚本 |
| 文档包 | 当前活跃交付包按版本归档 |

## 3. 备份策略

| 项目 | 默认口径 |
|---|---|
| 目标 | NAS，服务器地址 `192.168.88.113`，共享 `bn`，UNC `//192.168.88.113/bn` |
| 160 挂载口径 | 先将 `//192.168.88.113/<share>` 通过 SMB 挂载为 160 本地目录；当前生产路径为 `/mnt/complex-nas-backup`，再配置 `NAS_BACKUP_TARGET_DIR=/mnt/complex-nas-backup` |
| 强制同步开关 | 生产恢复验收时设置 `REQUIRE_NAS_BACKUP_SYNC=true`，缺少 `NAS_BACKUP_TARGET_DIR` 必须失败 |
| 频率 | 数据库每日一次；对象存储和配置每日同步；发布前手动备份 |
| 保留 | `RETENTION_DAYS=30`；容量上限 `CAPACITY_LIMIT=1TB`，由 NAS 侧或运维任务执行容量告警 |
| 权限 | 当前 SMB 用户名为 `jxh1`，凭证引用 `cred://prod/nas/smb/complex-backup-rw`；密码不得进入文档、聊天、脚本仓库或日志 |
| 加密 | 当前总控口径为备份归档不加密；160 必须显式设置 `ALLOW_UNENCRYPTED_BACKUPS=true`，并依赖 SMB 最小权限、NAS ACL、`.sha256` 校验和恢复演练补偿风险 |
| 加密预留 | `NAS_BACKUP_KEY_REF=cred://prod/nas/backup-encryption-key` 仅作为后续启用备份加密的预留引用；当前不作为 P0 阻断项 |
| 告警 | 备份失败、空间不足、校验失败必须告警 |
| 恢复演练 | 上线前至少完成一次恢复到临时环境 |

生产前置总控台账通过 `GET /api/v1/ops/production-readiness/items`、`PUT /api/v1/ops/production-readiness/items/{id}` 和 `POST /api/v1/ops/production-readiness/checks` 承载；本文负责 NAS 备份和恢复演练步骤，系统台账负责记录备份恢复是否阻断真实生产、责任人、计划日期、证据链接和降级方式。

## 4. 恢复验收标准

Given 已存在最近一次数据库和对象存储备份，  
When IT 在临时环境执行恢复演练，  
Then 数据库能启动、核心表可查询、对象文件可读取、配置可加载，并记录 `restore.drill_completed`。

Given NAS 备份任务失败，  
When 到达检查时间，  
Then 系统或运维台账必须记录失败原因、责任人和补救动作，不得把生产标记为可恢复。

Given 生产发布前需要回滚保障，  
When 执行发布 Runbook，  
Then 必须先完成发布前备份或确认最近备份可用。

## 5. 恢复演练记录字段

| 字段 | 说明 |
|---|---|
| `restore_drill_id` | 演练编号 |
| `backup_snapshot_id` | 备份快照编号 |
| `restore_target` | 临时恢复环境 |
| `started_at` | 开始时间 |
| `finished_at` | 完成时间 |
| `operator` | 执行人 |
| `verification_result` | 通过、失败或部分通过 |
| `failed_reason` | 失败原因 |
| `evidence_link` | 截图、日志或报告链接 |
| `audit_event` | 恢复演练事件 |

## 6. 实施 Runbook

1. IT 在 `192.168.88.113` 创建或确认 SMB 共享 `bn`、账号 `jxh1`、最小权限、容量告警和访问审计。
2. 运维把 `//192.168.88.113/bn` 挂载到 160 本地目录 `/mnt/complex-nas-backup`，并在 160 运行配置中填写 `NAS_BACKUP_TARGET_DIR=/mnt/complex-nas-backup`。
3. 160 环境配置数据库备份、对象存储同步和配置归档任务。
4. 当前不启用备份文件加密，运行配置必须显式写入 `ALLOW_UNENCRYPTED_BACKUPS=true`；如后续改启用加密，则删除该开关并配置 `BACKUP_ENCRYPTION_PASSPHRASE_FILE`。
5. 每次备份生成快照编号、`.tar.gz` 归档、`.sha256` 校验文件、日志和 NAS 同步结果。
6. 上线前恢复到临时环境，验证数据库、对象文件和配置。
7. 失败时记录原因、补救动作和复测时间。

## 6.1 160 配置模板

```bash
NAS_BACKUP_HOST=192.168.88.113
NAS_BACKUP_SHARE=bn
NAS_BACKUP_UNC=//192.168.88.113/bn
NAS_BACKUP_TARGET_DIR=/mnt/complex-nas-backup
NAS_BACKUP_RETENTION_DAYS=30
NAS_BACKUP_CAPACITY_LIMIT=1TB
NAS_BACKUP_CREDENTIAL_REF=cred://prod/nas/smb/complex-backup-rw
REQUIRE_NAS_BACKUP_SYNC=true
ALLOW_UNENCRYPTED_BACKUPS=true
NAS_BACKUP_KEY_REF=cred://prod/nas/backup-encryption-key
```

SMB 密码不得写入本文。推荐由 IT 在 160 写入仅当前运维用户可读的凭证文件，例如 `/home/james/.complex/secrets/nas-smb.credentials`，文件权限必须为 `600`。如果需要系统级自动挂载，因 160 上 `sudo` 需要密码，应由 IT 通过受控维护窗口完成 `/mnt/complex-nas-backup` 创建、挂载和权限配置。

## 6.2 验收证据

| 证据项 | 最低要求 |
|---|---|
| 挂载证据 | `//192.168.88.113/bn` 已挂载到 `/mnt/complex-nas-backup`，输出中不得包含密码 |
| 读写证据 | 在挂载目录完成创建、读取、校验、删除测试文件 |
| 备份证据 | `complex-backup.sh` 在 `REQUIRE_NAS_BACKUP_SYNC=true` 下生成 `.tar.gz` 与 `.sha256` 并同步到 NAS |
| 恢复证据 | `complex-restore-drill.sh latest` 输出 `restore.drill_completed`，临时库恢复后被删除 |
| 台账证据 | 生产前置台账记录责任人、credential_ref、到期提醒、演练报告链接和失败补偿动作 |
| 自动化验证 | `npm run verify:nas-smb-mount-evidence` 校验实时挂载(findmnt)、读写测试与 `docs/production-readiness-evidence/nas-smb-mount-latest.json` 证据一致,全绿方可交付 |

## 6.3 历史 NAS 验证证据与当前漂移状态

### 6.3.1 历史一次性验证证据（2026-06-15）

| 证据项 | 历史结果 |
|---|---|
| 160 挂载 | `//192.168.88.113/bn` 曾挂载到 `/mnt/complex-nas-backup`，当时 `findmnt` 显示 `cifs`、`rw`、`vers=3.0`、`username=jxh1` |
| 读写状态 | 当时 `/mnt/complex-nas-backup` 对 160 用户可写，恢复演练前置检查通过 |
| 历史 NAS 备份 | `complex-backup.sh` 在 `NAS_BACKUP_TARGET_DIR=/mnt/complex-nas-backup`、`REQUIRE_NAS_BACKUP_SYNC=true`、`ALLOW_UNENCRYPTED_BACKUPS=true` 下生成 `/mnt/complex-nas-backup/manual/20260615-112721.tar.gz` |
| 校验文件 | `/mnt/complex-nas-backup/manual/20260615-112721.tar.gz.sha256` 当时校验通过，恢复演练输出 `checksum=verified` |
| 从 NAS 恢复 | `complex-restore-drill.sh /mnt/complex-nas-backup/manual/20260615-112721.tar.gz` 输出 `restore.drill_completed` |
| 恢复结果 | 临时库 `complex_restore_drill_20260615_112734` 恢复成功并自动删除；Flyway 版本 `039`；验证表 `auth_credentials,auth_sessions,flyway_schema_history,platform_migration_baseline` |
| 历史挂载 evidence | `docs/production-readiness-evidence/nas-smb-mount-latest.json` |
| 历史恢复 evidence | `docs/production-readiness-evidence/nas-restore-drill-latest.json` |

以上只证明 2026-06-15 做过一次真实 NAS 同步和恢复演练，不能证明当前仍挂载、开机自动挂载已固化、定时备份已安装、容量/失败告警已接通或生产 manifest 已审批。

### 6.3.2 当前 live 状态

当前 live 状态以 `docs/nas-current-state-drift-report-v0.1.md` 和 `docs/production-readiness-evidence/nas-current-state-drift-latest.json` 为准；生成/校验命令：

```bash
npm run docs:nas-current-state-drift
npm run verify:nas-current-state-drift
```

截至最新漂移报告，当前 NAS live check 为 `failed`：`findmnt /mnt/complex-nas-backup` 未返回有效 CIFS 挂载，当前用户对 `/mnt/complex-nas-backup` 的写入测试失败，且当前用户不能读取 `/etc/complex/secrets/nas-smb.credentials`。因此不得把历史 `passed` evidence 作为当前生产闭环完成证明。

生产前仍需把挂载写入受控的开机自动挂载方案，补定时备份、容量告警、失败告警和周期性恢复演练计划；这些属于 `PRD-NAS-AUTOMOUNT-001`、`PRD-NAS-SCHEDULE-001`、`PRD-NAS-ALERT-001`、`PRD-NAS-DRILL-CADENCE-001` 和 `SEC-P0-OPS-RECOVERY-001` 的运维生产化工作，不再反向阻塞编码完成，但继续阻塞真实生产可恢复结论。

## 6.4 P0-A no-system-write boundary

本轮 P0-A 收口确认：Codex 不写系统级挂载和调度配置，只交付可验证的用户级脚本与 IT 模板。生产挂载、定时任务和真实告警通道必须由 IT/运维执行并回填证据。

P0-A 最小验收：

| 项目 | Codex 交付 | IT/运维回填证据 |
|---|---|---|
| 开机自动挂载 | `ops/160/complex-nas-preflight.sh` 和挂载检查口径 | 自动挂载配置截图、重启后 `findmnt`、读写删除测试 |
| 定时备份 | `ops/160/complex-backup-schedule.example` | 已安装的调度截图或导出、最近一次 scheduled 备份归档 |
| 容量告警 | `ops/160/complex-backup-alert.example` 中 `capacity_threshold_exceeded` | 容量阈值、告警触发或演练截图 |
| 失败告警 | `ops/160/complex-backup-alert.example` 中 `backup_failed` | 故障演练、通知送达和处理记录 |
| 周期恢复演练 | `complex-restore-drill.sh` 与 `nas-restore-drill-latest.json` | 月度或上线前恢复演练报告 |

推荐执行顺序：

1. 160 上执行 `ops/160/complex-nas-preflight.sh`。
2. IT 按 `complex-backup-schedule.example` 安装真实调度。
3. IT 按 `complex-backup-alert.example` 配置真实告警 secret。
4. 执行一次 scheduled 备份和一次恢复演练。
5. 回填生产前置台账和证据链接。

## 7. 关联文档

- `server-160-runbook-v0.2.md`
- `p0-end-to-end-acceptance-and-edge-cases-v0.1.md`
- `p0-implementation-task-list.md`
- `delivery-package-manifest-v0.1.md`
