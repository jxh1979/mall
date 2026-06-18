# 生产证据责任组执行包索引 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_OWNER_EXECUTION_PACKETS_READY
- OWNER_PACKET_ONE_STOP_BACKFILL_READY
- PRODUCTION_EVIDENCE_DRAFT_PREP_READY
- P0_FIRST_EXECUTION_ORDER_READY
- OWNER_PACKET_DOES_NOT_CLOSE_BLOCKERS
- RAW_EVIDENCE_STAYS_EXTERNAL
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET

## 2. 定位

本文从生产证据提交状态、P0 快速表、回填执行台账和责任组说明包生成。它把 40 个待提交生产证据拆成责任组执行包，方便 IT、财务、采购、运营/法务和技术负责人按同一口径回填。

它不创建真实证据，不关闭生产阻断，不允许原始证据入库。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| 责任组执行包 | 3 |
| 回填项 | 15 |
| P0 回填项 | 13 |
| 等待提交 | 15 |
| 无效提交 | 0 |

## 4. 责任组执行包

| owner_group | packet_path | owner_guide_path | status | p0/total | first_action |
| --- | --- | --- | --- | --- | --- |
| IT/运维/安全 | docs/production-readiness-evidence/owner-execution-packets/it-ops-security.md | docs/production-readiness-evidence/owner-guides/it-ops-security.md | awaiting_real_evidence | 11/12 | 先补生产账号、数据库、对象存储、JWT、第三方和 NAS credential_ref、到期提醒和轮换证据。 |
| 技术实现负责人 | docs/production-readiness-evidence/owner-execution-packets/technical-owner.md | docs/production-readiness-evidence/owner-guides/technical-owner.md | awaiting_real_evidence | 1/2 | 按 owner guide 复制模板为目标 manifest，补外部证据链接、复核人、测试时间和 cred:// 引用。 |
| 财务 | docs/production-readiness-evidence/owner-execution-packets/finance.md | docs/production-readiness-evidence/owner-guides/finance.md | awaiting_real_evidence | 1/1 | 按 owner guide 复制模板为目标 manifest，补外部证据链接、复核人、测试时间和 cred:// 引用。 |

## 5. 使用顺序

1. 先派发 P0 数量最多、阻断范围最大的责任组执行包。
2. 责任组可先运行 `npm run prepare:production-evidence-drafts -- --owner <owner_slug> --p0` 预览待复制草稿。
3. 需要批量复制模板时显式追加 `--execute`，已有 manifest 不会被覆盖。
4. 责任组只复制模板到目标 manifest，不提交原始证据文件。
5. 填写外部证据链接、审批记录、测试时间、复核人和 `cred://prod/...` 引用。
6. 提交后复跑本文第 6 节命令。
7. 若出现 invalid manifest，先修 invalid，再继续补 awaiting。

## 6. 总复跑命令

```bash
npm run verify:production-evidence-submissions-guard
npm run docs:production-evidence-submission-status
npm run verify:production-evidence-submission-status
npm run docs:production-evidence-backfill-priority
npm run docs:production-evidence-backfill-execution-ledger
npm run docs:production-evidence-owner-execution-packets
npm run verify:production-evidence-owner-execution-packets
npm run verify:production-evidence-draft-prep
npm run verify:closed-loop-current-state
npm run verify:delivery-gates
```
