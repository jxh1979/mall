# 生产账号、密钥和密码轮换 Runbook V0.1

## 1. 文档定位

本文用于收口生产环境账号、API 密钥、数据库密码、对象存储密码、企业微信凭证、聚合支付凭证和第三方供应商凭证的最低管理方案。P0 不引入复杂自动化密钥平台，默认采用人工台账 + 到期提醒 + Runbook + 留痕。

## 2. 默认方案

| 项目 | 默认口径 |
|---|---|
| 管理方式 | 人工台账 + 到期提醒 + Runbook |
| 责任方 | IT/运维主责，财务/HR/供应商按凭证归属配合 |
| P0 目标 | 每个生产凭证都有责任人、用途、到期日、轮换步骤和应急停用方式 |
| 不做内容 | P0 不强制引入 Vault、KMS 或自动轮换；后续可在 P1/P2 增强 |

生产前置总控台账通过 `GET /api/v1/ops/production-readiness/items`、`PUT /api/v1/ops/production-readiness/items/{id}` 和 `POST /api/v1/ops/production-readiness/checks` 承载；本文负责凭证轮换步骤，系统台账负责记录凭证轮换是否阻断真实生产、责任人、到期计划、证据链接和降级方式。

P0-A no-system-write boundary：Codex 只校验生产凭证台账结构、`credential_ref`、提醒字段、轮换步骤、回滚步骤、验证步骤和证据链接，不创建、不轮换、不停用任何真实生产账号或密钥。真实凭证发放、到期提醒任务配置和轮换执行必须由 IT/安全/财务在受控流程中完成。

> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §B-12，本文不再重复记载。
## 6. 实施 Runbook

1. 建立生产凭证台账模板。
2. 将数据库、对象存储、聚合支付、企业微信、第三方供应商和服务器账号全部登记。
3. 为每类凭证补充轮换步骤和应急停用步骤。
4. 配置到期提醒任务。
5. 上线前抽样演练至少一类凭证的轮换流程。

## 7. 分类型轮换 Runbook

### 7.1 生产服务器/运维账号轮换

1. 在台账中确认账号用途、负责人、授权范围、登录方式、到期日和 `credential_ref`。
2. 新建或启用替代账号，授予最小权限，并完成一次登录/执行健康检查验证。
3. 将旧账号降权、禁用或删除，保留禁用截图、命令输出或工单链接。
4. 更新 `last_rotated_at`、`next_rotation_at`、`rotation_status`、`rotation_evidence_link` 和 `audit_event`。
5. 若替代账号验证失败，立即恢复旧账号最小必要权限并记录 `rollback_step`。

### 7.2 PostgreSQL 生产库密码轮换

1. 轮换前确认最近一次 NAS 备份和恢复演练证据可用。
2. 为应用账号、备份账号或管理账号生成新密码，并只把保管位置写入 `credential_ref`。
3. 在维护窗口更新数据库密码、应用配置和备份脚本引用，不在文档、代码或日志中输出明文。
4. 运行数据库连接、Flyway 状态、健康检查、备份任务和恢复演练抽检。
5. 验证通过后废止旧密码；失败时按 `rollback_step` 恢复旧引用并暂停继续轮换。

### 7.3 对象存储 Access Key/Secret Key 轮换

1. 明确对象存储桶范围、读写权限、证据包/附件/备份用途和责任人。
2. 创建新 Access Key/Secret Key，把密钥保管位置写入 `credential_ref`，不得在台账写明文。
3. 更新应用、备份脚本或供应商配置引用，并验证上传、下载、签名 URL、对象权限和备份同步。
4. 禁用旧 key，确认旧 key 无法访问对象存储。
5. 归档验证结果和回滚记录。

### 7.4 JWT/Token 签名密钥轮换

1. 生成新签名密钥并登记签名版本、`credential_ref`、生效时间和旧 token 过渡窗口。
2. 先发布“新签发 + 旧验签”的兼容配置，验证登录、刷新、登出、开放平台签名和 Android 终端会话。
3. 过渡窗口结束后禁用旧签名密钥，并强制旧 token 失效或等待自然过期。
4. 若登录或验签异常，恢复旧签名版本并记录影响范围。

### 7.5 第三方、支付、企业微信和开放平台密钥轮换

1. 对聚合支付凭证、企业微信凭证、第三方供应商 API 密钥、开放平台应用/回调签名密钥分别登记供应商、环境、回调地址、签名算法和负责人。
2. 与供应商确认新旧密钥并行窗口；无并行窗口时必须安排停机或低峰维护窗口。
3. 更新 `credential_ref` 后执行签名校验、Webhook 回调、定时轮询校验、退款/对账或同步任务抽检。
4. 禁用旧密钥并要求供应商回执或截图作为 `rotation_evidence_link`。
5. 任一验签失败、回调重复扣减、同步失败或对账异常，立即回滚到旧引用并暂停生产切换。

### 7.6 NAS SMB 凭证与备份加密预留轮换

1. 当前 NAS 备份归档不加密，必须登记 SMB 凭证引用 `cred://prod/nas/smb/complex-backup-rw`、账号名、共享 `//192.168.88.113/bn`、160 挂载路径 `/mnt/complex-nas-backup` 和最小权限范围。
2. SMB 密码只能进入安全保管位置或 160 受控凭证文件，不得写入文档、聊天、脚本仓库、日志或台账明文字段。
3. 轮换 SMB 凭证时，先创建新凭证并完成挂载、读写删除测试、`complex-backup.sh` 同步和 `complex-restore-drill.sh latest` 恢复演练，再停用旧凭证。
4. 当前 `NAS_BACKUP_KEY_REF=cred://prod/nas/backup-encryption-key` 只作为后续启用备份加密的预留引用；启用加密前不得要求运维发放明文口令。
5. 如果后续启用备份加密，必须删除 `ALLOW_UNENCRYPTED_BACKUPS=true`，配置 `BACKUP_ENCRYPTION_PASSPHRASE_FILE`，并重新完成 `.tar.gz.enc`、`.sha256` 和恢复演练证据。

## 8. 关联文档

- `server-160-runbook-v0.2.md`
- `p0-implementation-task-list.md`
- `p0-end-to-end-acceptance-and-edge-cases-v0.1.md`
- `third-party-supplier-integration-kit-v0.1.md`
