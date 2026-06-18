# 已有生产证据到 Manifest 差距审计 V0.1

## 1. 机器标记

- EXISTING_EVIDENCE_TO_MANIFEST_GAP_AUDIT_READY
- NO_FAKE_PRODUCTION_MANIFEST_CLOSURE
- EXISTING_NAS_EVIDENCE_PARTIAL_ONLY
- IT_OWNER_REVIEWER_APPROVAL_REQUIRED
- SYSTEM_LEVEL_HARDENING_EVIDENCE_REQUIRED

## 2. 结论

截至 `2026-06-16 17:32 UTC`，仓库中已有 NAS 挂载读写、手动备份恢复、P0-A 运维预检和 160 编码主机边界证据，但这些证据 **不能直接生成 passed 生产 manifest**。原因是生产 manifest 校验要求的不只是技术命令输出，还包括真实责任人、独立复核人、外部可追溯证据链接、审批记录、失败场景证据、下一次复测日期，以及由 IT/运维完成的系统级固化证据。

因此，本报告只做差距审计和预填建议，不改变 `production-evidence-submission-status`，也不关闭任何 blocker。

## 3. 差距矩阵

| item_id | 当前提交状态 | 模板接受的 evidence_types | 已有可引用证据 | 仍缺字段/证据 | 审计结论 |
| --- | --- | --- | --- | --- | --- |
| PRD-NAS-AUTOMOUNT-001 | awaiting_submission | automount_export, scheduled_backup_log, sha256_file, restore_drill_report, alert_delivery_record | nas-smb-mount-latest.json: findmnt + write/read/delete live check passed<br>p0a-ops-hardening-latest.json: runtime preflight passed on 160 | IT 系统级自动挂载配置导出或截图<br>重启后 findmnt 证据<br>真实 owner_person<br>独立 reviewer<br>approval_record<br>failure_case_evidence<br>next_retest_due<br>外部 evidence_links | 不能直接提交 passed manifest；当前证据证明手动/当前会话挂载可用，但不证明开机自动挂载已由 IT 固化。 |
| PRD-NAS-SCHEDULE-001 | awaiting_submission | automount_export, scheduled_backup_log, sha256_file, restore_drill_report, alert_delivery_record | nas-restore-drill-latest.json: manual backup archive restore passed<br>nas-smb-mount-latest.json: NAS target writable | 定时备份调度配置<br>最近一次 scheduled 备份归档<br>scheduled 归档 .sha256<br>从 scheduled 归档恢复演练<br>真实 owner/reviewer/approval/failure case/external link | 不能直接提交 passed manifest；当前证据是 manual 备份与恢复，不是 scheduled 备份闭环。 |
| PRD-NAS-DRILL-CADENCE-001 | awaiting_submission | automount_export, scheduled_backup_log, sha256_file, restore_drill_report, alert_delivery_record | nas-restore-drill-latest.json: 一次恢复演练 passed，临时库已删除 | 月度或上线前恢复演练计划<br>复测周期 next_retest_due<br>IT/运维负责人和复核人<br>approval_record<br>failure_case_evidence<br>外部证据编号或工单链接 | 不能直接提交 passed manifest；已有一次演练，但生产 cadence 与外部审批链未闭合。 |
| PRD-160-001 | awaiting_submission | system_config_redacted_export, systemd_status_export, health_check_log, rollback_drill_report, alert_delivery_record | coding-host-protection-latest.json: 160 为 canonical coding host<br>p0a-ops-hardening-latest.json: P0-A no-system-write boundary 与 runtime preflight passed | /etc/complex/complex.env 或等效受控配置导出<br>systemd 服务状态导出<br>日志路径和健康检查外部证据<br>回滚演练报告<br>告警送达记录<br>真实 owner/reviewer/approval | 不能直接提交 passed manifest；当前证明开发主机边界，不证明生产运行配置已经 IT 系统级固化。 |
| SEC-P0-OPS-RECOVERY-001 | awaiting_submission | system_config_redacted_export, systemd_status_export, health_check_log, rollback_drill_report, alert_delivery_record | nas-smb-mount-latest.json: NAS live mount + write test passed<br>nas-restore-drill-latest.json: manual restore drill passed<br>p0a-ops-hardening-latest.json: no-system-write boundary documented | 开机自动挂载<br>定时备份<br>容量告警<br>失败告警<br>周期恢复演练计划<br>生产凭证责任人/到期/提醒/轮换证据<br>系统级配置由 IT 执行的证据<br>外部审批记录 | 不能关闭 P0 finding；已有开发/预生产验证，但生产恢复能力还缺 IT 固化和凭证轮换证据。 |

## 4. 可安全复用的字段

| 字段 | 可复用来源 | 注意事项 |
|---|---|---|
| `credential_refs` | `cred://prod/nas/smb/complex-backup-rw`、必要时保留 `cred://prod/nas/backup-encryption-key` | 只能写引用，不得写 SMB 密码或 token 正文 |
| `last_test_time` | `nas-smb-mount-latest.json.verified_at` 或 `nas-restore-drill-latest.json.finished_at` | 不能晚于 `submitted_at` |
| `evidence_ids` | 可按 `EVD-<item_id>-YYYYMMDD-001` 生成 | 不得跨 manifest 重复 |
| `evidence_links` | 需由 IT 转成 `NAS-EVIDENCE-*`、`ITSM-*`、`APPROVAL-*`、`ticket://...`、`workorder://...` 或受控 HTTPS 链接 | 仓库本地路径、`/mnt/...`、`localhost`、明文 HTTP 不合格 |
| `failure_case_evidence` | 需由 IT 做失败告警/恢复失败或权限失败演练后提供 | 不能用“无失败”代替 |
| `approval_record` | IT/运维/安全或财务审批记录 | 不能由 Codex 代填 |

## 5. 禁止直接生成 passed manifest 的原因

1. `owner_person` 和 `reviewer` 必须是真实责任人与不同复核人，不能由 Codex 或同一人代替。
2. `evidence_links`、`failure_case_evidence`、`approval_record` 必须是外部可追溯链接或批准的证据编号，不能引用本地文件路径。
3. `result=passed` 才会被看板认定为有效提交；如果缺少真实材料而强行填 passed，会违反 `NO_FAKE_PRODUCTION_EVIDENCE_CLOSURE`。
4. PRD-NAS-SCHEDULE-001 需要 scheduled 备份证据；现有恢复演练来自 manual 归档，不能替代。
5. PRD-NAS-AUTOMOUNT-001 需要重启后自动挂载证据；现有 live mount 不能证明开机自动挂载。
6. SEC-P0-OPS-RECOVERY-001 还要求容量告警、失败告警、周期恢复演练和生产凭证轮换证据，不能只凭一次恢复演练关闭。

## 6. IT/运维最小可执行动作

1. 在受控工单系统中为每个 item 建立工单，生成 `ITSM-*` 或 `ticket://...` 链接。
2. 将现有 NAS JSON 证据作为附件上传到工单，生成 `NAS-EVIDENCE-*` 编号。
3. 补做重启后自动挂载、scheduled 备份、容量告警、失败告警、周期恢复演练计划和 systemd/配置导出。
4. 由责任人填写 manifest，由不同复核人复核。
5. 把 manifest 放入对应 `docs/production-readiness-evidence/submissions/.../manifest-<item_id>.json`，不要提交原始证据文件。
6. 运行：

```bash
npm run verify:production-evidence-submissions-guard
npm run docs:production-evidence-submission-status
npm run verify:production-evidence-submission-status
npm run docs:production-evidence-backfill-priority
npm run verify:production-evidence-backfill-priority
npm run verify:delivery-gates
```

## 7. 关联证据

| 证据 | 路径 | 当前作用 |
|---|---|---|
| NAS live mount 历史证据 | `docs/production-readiness-evidence/nas-smb-mount-latest.json` | 证明历史某次 160 到 NAS 可读写，不证明当前仍挂载，不证明自动挂载/定时备份/告警 |
| NAS 当前状态漂移 | `docs/nas-current-state-drift-report-v0.1.md`、`docs/production-readiness-evidence/nas-current-state-drift-latest.json` | 记录当前 live check 与历史 evidence 的差异，不关闭任何 manifest |
| NAS restore drill | `docs/production-readiness-evidence/nas-restore-drill-latest.json` | 证明一次 manual 归档恢复通过，不证明 scheduled 备份和周期机制 |
| P0-A ops hardening | `docs/production-readiness-evidence/p0a-ops-hardening-latest.json` | 证明 Codex no-system-write 边界和运行预检，不证明 IT 已系统级固化 |
| 编码主机保护 | `docs/production-readiness-evidence/coding-host-protection-latest.json` | 证明 160 是正式编码主机，不证明生产运行配置完成 |
