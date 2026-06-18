# 生产证据回填优先级看板 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_BACKFILL_PRIORITY_READY
- P0_EVIDENCE_GAPS_TRACKED
- OWNER_BACKFILL_BATCHES_READY
- PRIORITY_BOARD_DOES_NOT_CLOSE_BLOCKERS
- RAW_EVIDENCE_STAYS_EXTERNAL

## 2. 定位

本文从 `docs/production-readiness-evidence/production-evidence-submission-status-latest.json` 和 `docs/production-readiness-evidence/production-evidence-submission-kit-latest.json` 自动生成，用于告诉 IT、财务、采购、运营、法务和技术负责人下一批应该先补哪些真实生产证据。本文只排序和分派，不关闭生产阻断项；只有真实 manifest 通过状态看板和来源台账/finding 同步更新后，才能进入关闭判断。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| 预期提交项 | 15 |
| P0 预期提交项 | 13 |
| P0 等待提交 | 13 |
| P0 有效提交 | 0 |
| P0 无效提交 | 0 |
| 全部等待提交 | 15 |
| 全部有效提交 | 0 |
| 全部无效提交 | 0 |
| 开放回填批次 | 7 |

## 4. 责任组 P0 缺口

| owner_group | p0_expected | p0_awaiting | p0_valid | p0_invalid | awaiting_total | invalid_total |
| --- | --- | --- | --- | --- | --- | --- |
| IT/运维/安全 | 11 | 11 | 0 | 0 | 12 | 0 |
| 技术实现负责人 | 1 | 1 | 0 | 0 | 2 | 0 |
| 财务 | 1 | 1 | 0 | 0 | 1 | 0 |

## 5. 建议回填批次

| batch_id | rank | owner_group | priority | category | awaiting_submission | invalid_submission | item_ids | recommended_action |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| BATCH-01 | 1 | IT/运维/安全 | P0 | credential | 5 | 0 | PRD-CRED-001, PRD-CRED-DB-001, PRD-CRED-JWT-001, PRD-CRED-NAS-001, PRD-CRED-OPS-001 | 先补生产账号、数据库、对象存储、JWT/第三方密钥的 credential_ref、到期提醒和轮换证据。 |
| BATCH-02 | 2 | 财务 | P0 | credential | 1 | 0 | PRD-CRED-001 | 先补生产账号、数据库、对象存储、JWT/第三方密钥的 credential_ref、到期提醒和轮换证据。 |
| BATCH-03 | 3 | 技术实现负责人 | P0 | credential | 1 | 0 | PRD-CRED-JWT-001 | 先补生产账号、数据库、对象存储、JWT/第三方密钥的 credential_ref、到期提醒和轮换证据。 |
| BATCH-04 | 4 | IT/运维/安全 | P0 | nas | 4 | 0 | PRD-NAS-ALERT-001, PRD-NAS-AUTOMOUNT-001, PRD-NAS-DRILL-CADENCE-001, PRD-NAS-SCHEDULE-001 | 先补 NAS 自动挂载、定时备份、容量/失败告警和周期恢复演练证据。 |
| BATCH-05 | 5 | IT/运维/安全 | P0 | ops160 | 2 | 0 | PRD-160-001, SEC-P0-OPS-RECOVERY-001 | 先补 160 生产运行配置、systemd、健康检查、日志和回滚证据。 |
| BATCH-06 | 6 | 技术实现负责人 | P2 | security | 1 | 0 | SEC-P2-CSRF-API-001 | 按 manifest 模板补齐外部可追溯证据链接、复核人、测试时间和 credential_ref。 |
| BATCH-07 | 7 | IT/运维/安全 | P2 | security | 1 | 0 | SEC-P2-CSRF-API-001 | 按 manifest 模板补齐外部可追溯证据链接、复核人、测试时间和 credential_ref。 |

## 6. 执行规则

1. 先处理 `invalid_submission` 批次，再处理 P0 批次。
2. 同为 P0 时，按 MDM、支付、凭证、NAS、160 运维、人脸年龄核验、企微、第三方、安全的顺序推进。
3. 每个批次只提交 manifest；原始证据继续留在外部受控系统，并在 `evidence_links` 留链。
4. 每批回填后执行 `npm run verify:production-evidence-submissions-guard`、`npm run docs:production-evidence-submission-status`、`npm run docs:production-evidence-backfill-priority` 和 `npm run verify:delivery-gates`。
