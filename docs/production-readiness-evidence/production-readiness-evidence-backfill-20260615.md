# 生产前置实施证据回填记录 2026-06-15

## 1. 回填结论

本次进入“生产前置实施证据回填”阶段后，已完成当前代码基线、P0 自动化闸口、160 运维脚本闸口和最新 NAS 备份恢复演练证据回填。

本记录不把外部采购、IT 纳管或真实生产凭证发放伪装为已完成。凡需要 MDM 控制台、供应商后台、生产密钥保管系统、告警系统或 IT 工单截图的事项，仍保持 `pending_info` 或 `open`，并列明最低证据。

## 2. 已回填证据

| item_id / finding | 当前状态 | 已回填证据 | 证据位置 | 备注 |
|---|---|---|---|---|
| P0 编码基线 | passed | 最新提交 `5463ee1 fix: harden credential storage and webhook signatures`，160 工作区干净 | 160 本地 git `master` | 160 未配置 remote，未 push |
| P0 自动化闸口 | passed | `npm run verify:p0` 通过 | 160 命令执行记录 | 覆盖 P0 契约、安全基线、后端 547 测试、Web build、Android env/build/smoke/APK |
| P0-A 运维闸口 | passed | `npm run verify:ops160-recovery` 通过 | 160 命令执行记录 | 覆盖执行策略、备份恢复脚本、生产凭证台账结构、P0-A ops hardening |
| PRD-SEC-001 | passed | 正式安全基线 V1 与 findings register 已生成并通过机器校验 | `security-scans/formal-security-baseline-v1.md`、`security-scans/findings-register-v1.md`、`docs/production-readiness-evidence/security-baseline-latest.json` | 不关闭 MDM、生产凭证、真实支付/企微/供应商等 open 阻断项 |
| 交付闸口脚本 Semgrep | passed | `pipx run semgrep --config p/javascript` 扫描 12 个新增/修改 Node 校验/生成脚本，0 findings | `security-scans/delivery-readiness-semgrep-20260615.json` | 不替代完整发布前安全扫描 |
| 生产阻断清单 | open_blockers_present | 已从生产前置台账和 findings register 生成机器可读阻断清单 | `docs/production-blocker-register-v0.1.md`、`docs/production-readiness-evidence/production-blocker-register-latest.json` | 当前仍有外部/IT/供应商阻断项，不得伪关闭 |
| 生产前置证据回填模板 | passed | 已按 MDM、NAS、凭证、支付、企微、人脸、第三方和 160 运行配置补齐证据字段模板 | `docs/production-readiness-evidence-intake-template-v0.1.md` | 模板不关闭阻断项，只规范回填材料 |
| 生产责任人动作包 | open_actions_present | 已按责任人把未关闭生产阻断项拆成动作、证据和复测命令 | `docs/production-owner-action-packs-v0.1.md`、`docs/production-readiness-evidence/production-owner-action-packs-latest.json` | 从阻断清单自动生成，不关闭任何 pending/open 项 |
| 生产证据提交包 | awaiting_real_evidence | 已把责任人动作包转成提交目录、manifest 字段、证据类型和复测命令 | `docs/production-evidence-submission-kit-v0.1.md`、`docs/production-readiness-evidence/production-evidence-submission-kit-latest.json` | 提交包只定义格式，不关闭任何 pending/open 项 |
| 生产证据 Manifest 模板 | templates_ready_not_real_evidence | 已为 40 个提交项生成可复制 JSON 模板，默认 blocked 且仅作填写样例 | `docs/production-evidence-manifest-templates-v0.1.md`、`docs/production-readiness-evidence/production-evidence-manifest-template-index-latest.json` | 模板不是生产证据，复制后必须替换占位内容 |
| 生产证据责任组回填说明包 | owner_guides_ready_not_real_evidence | 已按责任组生成 5 份回填说明，列出模板路径、提交路径和复测命令 | `docs/production-evidence-owner-guides-v0.1.md`、`docs/production-readiness-evidence/production-evidence-owner-guides-latest.json`、`docs/production-readiness-evidence/owner-guides/` | 说明包不是生产证据，不关闭阻断项 |
| 生产证据提交状态看板 | awaiting_real_evidence | 已扫描实际 manifest 提交状态；当前未提交 manifest 均保持 awaiting，不视为 passed | `docs/production-evidence-submission-status-v0.1.md`、`docs/production-readiness-evidence/production-evidence-submission-status-latest.json` | 看板不关闭阻断项；无效 manifest 会阻断交付闸口 |
| 生产证据提交目录护栏 | manifest_only_in_repo | 已创建 submissions README/.gitignore，并强制仓库只允许 manifest 入库；原始证据外部留链 | `docs/production-readiness-evidence/submissions/README.md`、`docs/production-readiness-evidence/submissions/.gitignore` | 护栏不关闭阻断项，只降低误传原始证据和密钥风险 |
| 生产证据链接与凭证引用校验 | traceable_links_required | 已禁止 TODO/占位值/本地路径/localhost/不可追溯证据链接；凭证引用必须为 `cred://...` 且不得含占位词；机器标记 `TRACEABLE_EVIDENCE_LINKS_REQUIRED`、`CREDENTIAL_REF_PLACEHOLDER_FORBIDDEN` | `tools/generate-production-evidence-submission-status.mjs`、`docs/production-evidence-submission-status-v0.1.md` | 该校验只判定提交材料是否可验收，不关闭任何生产阻断项 |
| 生产证据回填优先级看板 | awaiting_p0_evidence | 已按责任组、P0 缺口、状态和类别生成回填批次，机器标记 `PRODUCTION_EVIDENCE_BACKFILL_PRIORITY_READY`、`P0_EVIDENCE_GAPS_TRACKED` | `docs/production-evidence-backfill-priority-v0.1.md`、`docs/production-readiness-evidence/production-evidence-backfill-priority-latest.json` | 看板只排序和分派，不关闭任何生产阻断项 |
| PRD-NAS-001 | passed | NAS `//192.168.88.113/bn` 已挂载到 `/mnt/complex-nas-backup` 且可写；已补 live mount + write/read/delete 机器证据 | `docs/production-readiness-evidence/nas-smb-mount-latest.json`、`docs/nas-backup-and-restore-runbook-v0.1.md#63-最新真实-nas-验证证据2026-06-15` | 系统级自动挂载仍需 IT 固化；可用 `npm run verify:nas-smb-mount-evidence` 复核 |
| PRD-NAS-002 | passed | 当前版本备份 `/mnt/complex-nas-backup/manual/20260615-112721.tar.gz` 已从 NAS 恢复成功 | `docs/production-readiness-evidence/nas-restore-drill-latest.json` | 恢复 Flyway 版本 `039` |
| PRD-CRED-NAS-001 | structure_passed | NAS SMB 凭证引用已记录为 `cred://prod/nas/smb/complex-backup-rw` | `docs/production-readiness-implementation-ledger-template-v0.1.md` | 真实密码不得进入仓库；轮换证据仍需 IT 回填 |
| PRD-CRED-NAS-KEY-001 | not_applicable | 当前总控口径为备份归档不加密，保留 `cred://prod/nas/backup-encryption-key` 作为预留引用 | `docs/production-credential-rotation-runbook-v0.1.md` | 当前不阻塞；启用加密后再转阻断 |

## 3. 仍需 IT/采购/安全回填的生产阻断证据

| item_id / finding | 阻断范围 | 最低回填材料 | 建议责任方 | 完成判定 |
|---|---|---|---|---|
| `SEC-P0-MDM-DEVICE-001` / PRD-MDM-001 | 阻塞生产工作手机、平板、一体机执行高风险核销、出酒、支付相关动作 | MDM/Android Enterprise 供应商或 Android Management API 项目资料；组织/策略模板截图；至少 1 台工作手机、1 台平板、1 台售票型一体机、1 台非售票型一体机 enroll 截图；策略下发成功截图；丢失/锁定/擦除演练记录；平台设备绑定记录 | IT/采购/运维 | 台账状态从 `pending_info` 改为 `passed`，并有 evidence_link、last_test_time、owner_person |
| `SEC-P0-OPS-RECOVERY-001` / PRD-NAS-AUTOMOUNT-001 | 阻塞长期预生产和生产自动恢复 | 重启后 `findmnt /mnt/complex-nas-backup` 证据；读写删除测试；自动挂载配置截图或导出 | IT/运维 | 台账状态 `passed`，证据不含明文密码 |
| PRD-NAS-SCHEDULE-001 | 阻塞长期预生产和生产自动备份 | 已安装调度截图或导出；最近一次 scheduled 备份归档；`.sha256`；从 scheduled 归档恢复演练报告 | IT/运维 | 台账状态 `passed`，且恢复报告能追溯到 scheduled 归档 |
| PRD-NAS-ALERT-001 | 阻塞长期预生产和生产运维准入 | 容量阈值配置；备份失败告警演练；通知送达截图；处理工单或记录 | IT/运维/安全 | 台账状态 `passed`，告警 secret 只保存在安全位置 |
| PRD-NAS-DRILL-CADENCE-001 | 阻塞长期预生产和生产可恢复结论 | 月度或上线前恢复演练计划；最近一次演练报告；临时库清理记录 | IT/运维 | 台账状态 `passed`，evidence_link 指向最新报告 |
| PRD-CRED-001 / PRD-CRED-* | 阻塞长期预生产和生产 | 生产服务器账号、PostgreSQL、对象存储、JWT/Token、聚合支付、企业微信、第三方供应商、开放平台、NAS SMB 凭证的 `credential_ref`、owner_person、到期日、提醒策略、轮换步骤、回滚步骤、验证步骤、rotation_evidence_link | IT/安全/财务/采购/供应商负责人 | `verify:production-credentials` 结构已通过；真实发放和轮换证据补齐后才可关闭生产阻断 |
| PRD-PAY-001 / PRD-PAY-002 | 阻塞真实收款、退款、对账和日结 | 聚合支付测试/生产商户号；回调地址；验签资料；退款权限；对账文件样例；财务责任人确认 | 财务/IT | 支付 mock 不再作为生产证据 |
| PRD-WECOM-001 | 阻塞企业微信 P0 同步能力 | 企业微信应用配置、通讯录权限、会话存档权限、回调配置、同步任务验收、失败告警证据 | IT/运营 | 完成一次增量同步和失败告警演练 |
| PRD-FACE-001 | 阻塞酒吧年龄/人脸核验生产通过 | 供应商 SDK/API、测试账号、错误码、合规材料、年龄核验成功/失败样例、采购确认 | 采购/IT/运营/法务 | 供应商未定前只能 mock，不得标记生产 passed |
| PRD-3P-001 | 阻塞真实机器人/视频/设备厂商联调 | SDK/API、签名验签样例、Webhook 样例、轮询策略、字段映射、错误码、沙箱账号、生产 credential_ref | 采购/IT/供应商联调负责人 | mock adapter 不等于真实生产接入 |

## 4. 回填操作记录

| 时间 | 操作 | 结果 |
|---|---|---|
| 2026-06-15 11:27 UTC | 执行当前版本 NAS 备份 | 生成 `/mnt/complex-nas-backup/manual/20260615-112721.tar.gz` 并同步到 NAS |
| 2026-06-15 11:27 UTC | 从 NAS 归档执行恢复演练 | `restore.drill_completed`，Flyway `039`，临时库已删除 |
| 2026-06-15 19:56 UTC | 执行 NAS SMB 挂载/读写机器校验 | `npm run verify:nas-smb-mount-evidence` 通过，不读取或记录 SMB 密码 |
| 2026-06-15 20:05 UTC | 执行正式安全基线与生产前置证据校验 | `npm run verify:security-baseline` 和 `npm run verify:production-readiness-evidence` 通过 |
| 2026-06-15 20:06 UTC | 执行交付闸口脚本 Semgrep 增量扫描 | `pipx run semgrep --config p/javascript` 扫描 12 个脚本，0 findings |
| 2026-06-15 20:12 UTC | 生成生产阻断清单 | `npm run docs:production-blockers` 与 `npm run verify:production-blockers` 通过 |
| 2026-06-15 20:18 UTC | 校验生产前置证据回填模板 | `npm run verify:production-evidence-intake` 通过 |
| 2026-06-15 20:25 UTC | 生成生产责任人动作包 | `npm run docs:production-owner-actions` 与 `npm run verify:production-owner-actions` 通过 |
| 2026-06-15 20:34 UTC | 生成生产证据提交包 | `npm run docs:production-evidence-submission-kit` 与 `npm run verify:production-evidence-submission-kit` 通过 |
| 2026-06-15 20:42 UTC | 生成生产证据提交状态看板 | `npm run docs:production-evidence-submission-status` 与 `npm run verify:production-evidence-submission-status` 通过 |
| 2026-06-15 20:50 UTC | 生成生产证据 Manifest 模板 | `npm run docs:production-evidence-manifest-templates` 与 `npm run verify:production-evidence-manifest-templates` 通过 |
| 2026-06-15 20:58 UTC | 生成生产证据责任组回填说明包 | `npm run docs:production-evidence-owner-guides` 与 `npm run verify:production-evidence-owner-guides` 通过 |
| 2026-06-15 21:24 UTC | 固化生产证据提交目录护栏 | `npm run verify:production-evidence-submissions-guard` 纳入交付闸口；submissions 目录只允许 manifest 入库 |
| 2026-06-15 21:45 UTC | 固化证据链接与凭证引用校验 | 已用 TDD 验证 `TODO` 证据链接会被拦截，`ticket://...`/`APPROVAL-...`/`cred://...` 可通过 |
| 2026-06-15 21:52 UTC | 生成生产证据回填优先级看板 | `npm run docs:production-evidence-backfill-priority` 与 `npm run verify:production-evidence-backfill-priority` 纳入交付闸口 |
| 2026-06-15 | 执行 `npm run verify:p0` | 通过 |
| 2026-06-15 | 执行 `npm run verify:ops160-recovery` | 通过 |

## 5. 禁止事项

1. 不得把真实密码、token、API secret、私钥正文写入本文、台账、聊天、日志或仓库。
2. 不得用 mock、截图占位或口头确认关闭 MDM、真实支付、真实供应商、真实凭证发放类生产阻断项。
3. 不得把 `verify:p0` 通过解释为公开生产准入通过；公开生产仍需所有 P0 open findings 关闭或正式豁免。
4. 不得由 Codex 写系统级自动挂载、systemd、crontab 或真实告警 secret；这些必须由 IT/运维在受控窗口执行并回填证据。

## 6. 下一步执行顺序

1. IT/采购先完成 PRD-MDM-001 的 MDM 选型和测试设备 enroll。
2. IT/运维固化 NAS 自动挂载、定时备份、容量告警和失败告警。
3. IT/安全/财务按 PRD-CRED-* 明细补齐真实生产凭证引用、到期提醒和轮换证据。
4. 财务/IT 回填聚合支付商户、退款、回调和对账证据。
5. 运营/IT 回填企业微信、人脸年龄核验和第三方供应商真实资料。
6. 每补齐一批证据后先执行：`npm run docs:production-blockers && npm run docs:production-owner-actions && npm run docs:production-evidence-submission-kit && npm run docs:production-evidence-manifest-templates && npm run docs:production-evidence-owner-guides && npm run docs:production-evidence-submission-status && npm run docs:production-evidence-backfill-priority && npm run verify:production-owner-actions && npm run verify:production-evidence-submission-kit && npm run verify:production-evidence-manifest-templates && npm run verify:production-evidence-owner-guides && npm run verify:production-evidence-submissions-guard && npm run verify:production-evidence-submission-status && npm run verify:production-evidence-backfill-priority`。
7. 再执行：`npm run docs:security-baseline && npm run verify:security-baseline && npm run verify:production-readiness-evidence && npm run verify:p0 && npm run verify:ops160-recovery`。
