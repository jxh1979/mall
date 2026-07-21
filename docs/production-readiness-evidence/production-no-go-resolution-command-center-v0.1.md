# 生产 No-Go 关闭指挥板 V0.1

- PRODUCTION_NO_GO_RESOLUTION_COMMAND_CENTER_READY
- NO_FAKE_PRODUCTION_EVIDENCE_CLOSURE
- CLOSURE_REQUIRES_VALID_SUBMISSION_MANIFESTS
- NAS_REQUIRES_IT_SUDO_MAINTENANCE_WINDOW
- OWNER_ACTIONS_LINKED_TO_EVIDENCE_MANIFESTS
- EXTERNAL_PARTIALS_MAPPED_TO_MANIFESTS
- PRODUCTION_READY_REMAINS_NO_GO_UNTIL_EXTERNAL_EVIDENCE_VALID

> 本文是生产上线 no-go 的执行指挥板。它只把阻塞项、责任人、manifest 路径和验证命令合并成一张可执行清单；不得用它替代真实生产 evidence。raw evidence 必须留在工单、审批、供应商平台、NAS/对象存储或安全库中，仓库只提交 manifest，密钥只写 `cred://prod/...` 引用。

## 1. 当前结论

| 项目 | 当前值 |
| --- | --- |
| 可进入联调 | 是 |
| 可进入业务验收 | 是 |
| 可宣称生产上线就绪 | 否 |
| 待提交 production evidence | 15/15 |
| P0 待提交 production evidence | 13 |
| open blocker | 12（readiness=6，credential=4，security=2） |
| partial 外部/供应商需求 | 8 |
| owner action 无 manifest 链接 | 0 |
| NAS live status | failed |

生产上线仍为 no-go，直到以下条件同时满足：40 份 manifest 均为 valid_submission、NAS 当前态通过、23 个 blocker 归零、8 条 partial 需求的真实外部证据完成回填。

## 2. NAS 先决动作

口径说明：`//192.168.88.113/bn` 是 NAS SMB 真实地址；`/mnt/complex-nas-backup` 只是 160 上用于访问该 SMB 共享的本地挂载点，不是 NAS 地址。

当前 NAS 根因：NAS host and SMB port are reachable, mount.cifs exists, but the expected CIFS mount is inactive and the current user has no passwordless sudo; IT must execute the controlled mount handoff

| 字段 | 值 |
| --- | --- |
| NAS SMB 地址（真实地址） | `//192.168.88.113/bn` |
| 160 本地挂载点（不是 NAS 地址） | `/mnt/complex-nas-backup` |
| credential file | `/etc/complex/secrets/nas-smb.credentials` |
| credential_ref | `cred://prod/nas/smb/complex-backup-rw` |
| 当前用户免密 sudo | 否 |
| SEC-P0-OPS-RECOVERY-001 可关闭 | 否 |

IT/运维在 160 的受控维护窗口执行以下命令；Codex 不应在无审批、无 sudo、无 IT 责任人的情况下修改系统挂载：

```bash
cd /home/james/complex-management
sudo env EXECUTE_MOUNT=true   NAS_UNC='//192.168.88.113/bn'   MOUNT_PATH='/mnt/complex-nas-backup'   NAS_SMB_CREDENTIALS_FILE='/etc/complex/secrets/nas-smb.credentials'   MOUNT_USER='james'   MOUNT_GROUP='james'   SMB_VERSION='3.0'   EVIDENCE_DIR='/tmp/complex-nas-mount-evidence'   bash ops/160/complex-nas-system-mount.example.sh

bash ops/160/complex-nas-preflight.sh
node tools/generate-nas-diagnostic-report.mjs
node tools/generate-nas-current-state-drift-report.mjs
npm run verify:nas-diagnostics
npm run verify:nas-current-state-drift
```

IT 执行后，把 `/tmp/complex-nas-mount-evidence/*.json` 中的脱敏证据链接写入对应 NAS manifest，不要把 SMB 密码写入仓库。

## 3. P0 优先 manifest 清单

无。


## 4. P1/P2 后续 manifest 清单

无。


## 5. 23 个 open blocker

| 类型 | 级别 | id | 责任方 | 状态 | 下一步 |
| --- | --- | --- | --- | --- | --- |
| readiness | P0/P1 by item priority | PRD-NAS-AUTOMOUNT-001 | IT/运维 | pending_info | IT 安装后回填重启后 findmnt、读写删除测试和截图 |
| readiness | P0/P1 by item priority | PRD-NAS-SCHEDULE-001 | IT/运维 | pending_info | IT 安装后回填调度配置、最近一次 scheduled 备份和 .sha256 |
| readiness | P0/P1 by item priority | PRD-NAS-ALERT-001 | IT/运维/安全 | pending_info | 回填容量告警、失败告警和通知送达证据 |
| readiness | P0/P1 by item priority | PRD-NAS-DRILL-CADENCE-001 | IT/运维 | pending_info | 回填最近一次恢复演练报告、临时库清理和验证表 |
| readiness | P0/P1 by item priority | PRD-CRED-001 | IT/安全/财务 | pending_info | 建立密钥引用、轮换周期、责任人和应急回滚 |
| readiness | P0/P1 by item priority | PRD-160-001 | IT/运维 | pending_info | 迁移用户级覆盖到系统配置，接入 systemd 和告警 |
| credential | P0 | PRD-CRED-OPS-001 | IT/运维/安全 | pending_info | 建立账号负责人、授权范围、到期日和停用步骤 |
| credential | P0 | PRD-CRED-DB-001 | IT/运维/安全 | pending_info | 既成事实（2026-07-06 prod 切换后）：运行时 app 角色 complex_app 已轮换 55 位强密码且被 REVOKE 最小权限；owner/Flyway 超级角色 complex 密码仍为 dev 默认 complex_dev_pwd，运行时以 COMPLEX_DATABASE_CREDENTIAL_SEPARATION_ENFORCED=false 关闭凭证守卫掩盖此状态。待 IT 为 owner ALTER ROLE 生成强密码、同步 owner env 并重启，随后移除 enforced=false 让守卫 fail-closed 生效；补到期日和维护窗口 |
| credential | P0 | PRD-CRED-JWT-001 | IT/安全/技术负责人 | pending_info | 设计签名版本、双验窗口、旧 token 失效计划 |
| credential | P0 | PRD-CRED-NAS-001 | IT/运维/安全 | pending_info | IT 将真实密码写入安全保管位置或 160 受控凭证文件，不得写入文档、聊天或仓库 |
| security_finding | P0 | SEC-P0-OPS-RECOVERY-001 | IT/安全/运维 | open | P0-A no-system-write boundary：Codex 不安装系统级配置；后续由 IT 固化 160 开机自动挂载、定时备份、容量告警、失败告警和周期性恢复演练；生产凭证实际发放前按明细台账补责任人、到期日、credential_ref、提醒和轮换证据 |
| security_finding | P2 | SEC-P2-CSRF-API-001 | 后端/前端/安全 | open_for_prod_review | 员工后台仍为 Bearer 头 token（无 cookie 环境凭证）；若后台改用 Cookie 会话再按 SameSite/双提交复核 |


## 6. 8 条 partial 外部/供应商需求

| 需求 | 模块 | 证据 item | 责任组 | 待证据行 | manifest 数 | 关闭动作 |
| --- | --- | --- | --- | --- | --- | --- |
| REQ-17 | 酒吧 |  |  | 0 | 0 | Submit valid production manifests for the mapped item_ids; mock/sandbox evidence cannot close this requirement. |
| REQ-21 | 酒吧 |  |  | 0 | 0 | Submit valid production manifests for the mapped item_ids; mock/sandbox evidence cannot close this requirement. |
| REQ-23b | 酒吧 |  |  | 0 | 0 | Submit valid production manifests for the mapped item_ids; mock/sandbox evidence cannot close this requirement. |
| REQ-25 | 酒吧 |  |  | 0 | 0 | Submit valid production manifests for the mapped item_ids; mock/sandbox evidence cannot close this requirement. |
| REQ-32 | 酒吧 |  |  | 0 | 0 | Submit valid production manifests for the mapped item_ids; mock/sandbox evidence cannot close this requirement. |
| REQ-40 | 第三方 |  |  | 0 | 0 | Submit valid production manifests for the mapped item_ids; mock/sandbox evidence cannot close this requirement. |
| REQ-41 | 第三方 |  |  | 0 | 0 | Submit valid production manifests for the mapped item_ids; mock/sandbox evidence cannot close this requirement. |
| REQ-44 | 第三方 |  |  | 0 | 0 | Submit valid production manifests for the mapped item_ids; mock/sandbox evidence cannot close this requirement. |


## 7. 提交后统一验证

- `npm run verify:production-evidence-submissions-guard`
- `npm run docs:production-evidence-submission-status`
- `npm run verify:production-evidence-submission-status`
- `npm run docs:production-evidence-backfill-priority`
- `npm run verify:production-evidence-backfill-priority`
- `npm run docs:production-closure-gap-reports`
- `npm run verify:production-closure-gap-reports`
- `npm run verify:closed-loop-current-state`
- `npm run verify:delivery-gates`

## 8. 禁止事项

- 不得把 mock、沙箱截图、口头确认、占位文件、明文密钥、未归属截图当作生产 evidence。
- 不得把历史 NAS passed evidence 当作当前自动挂载或生产恢复证据。
- 不得在 manifest 中写入密码、token、secret、private key；只能写 `cred://prod/...`。
- 不得跳过失败场景、复核人、审批记录、下次复测日期。
