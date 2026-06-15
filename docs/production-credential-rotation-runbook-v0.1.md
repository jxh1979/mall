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

## 3. 台账字段

| 字段 | 说明 |
|---|---|
| `credential_id` | 凭证编号 |
| `credential_ref` | 凭证引用编号或保管位置编号，只写引用，不得写真实密钥 |
| `credential_type` | 数据库、对象存储、支付、企业微信、供应商 API、服务器账号等 |
| `environment` | `dev`、`test`、`staging`、`prod` |
| `owner_department` | 责任部门 |
| `owner_person` | 责任人 |
| `system_scope` | 使用系统或模块 |
| `supplier_name` | 第三方供应商名称，若无则为空 |
| `created_at` | 创建时间 |
| `expires_at` | 到期时间 |
| `rotation_cycle_days` | 轮换周期 |
| `reminder_days` | 到期提醒策略，P0 默认 `30,7,1` |
| `last_rotated_at` | 最近轮换时间 |
| `next_rotation_at` | 下次轮换时间 |
| `rotation_status` | `not_started`、`pending_info`、`scheduled`、`rotated`、`failed`、`emergency_disabled` |
| `secret_location` | 密钥保存位置描述，不记录明文 |
| `runbook_link` | 轮换步骤链接 |
| `verification_step` | 轮换后验证步骤 |
| `rollback_step` | 轮换失败后的回滚或停用步骤 |
| `rotation_evidence_link` | 轮换记录、截图、工单、供应商回执或测试报告 |
| `emergency_disable_step` | 应急停用步骤 |
| `audit_event` | 最近一次创建、查看、轮换或停用事件 |

## 4. 轮换规则

| 场景 | P0 规则 |
|---|---|
| 新增生产凭证 | 必须先登记台账，再发放给责任人 |
| 到期前提醒 | 默认提前 30 天、7 天、1 天提醒 |
| 责任人离职或调岗 | 必须立即轮换或停用相关凭证 |
| 供应商变更 | 旧凭证停用，新凭证登记后再联调 |
| 疑似泄露 | 立即停用、轮换、记录影响范围和复盘结论 |
| 临时凭证 | 必须设置到期日，不允许长期有效 |

## 4.1 P0 必登凭证类型

| 凭证类型 | P0 登记要求 | 阻塞口径 |
|---|---|---|
| 生产服务器/运维账号 | 记录账号用途、负责人、授权范围、登录方式、停用步骤和审计证据 | 缺失时阻塞长期预生产和生产 |
| PostgreSQL 生产库 | 记录应用账号、管理账号、备份账号的 `credential_ref`、到期日、轮换窗口和验证步骤 | 缺失时阻塞生产数据库接入 |
| 对象存储 | 记录 MinIO 或等效对象存储 Access Key/Secret Key 的 `credential_ref`、桶范围、权限和回滚方式 | 缺失时阻塞生产附件、证据包和备份接入 |
| JWT/Token 签名密钥 | 记录签名密钥引用、签名版本、过渡双签/双验窗口、旧 token 失效计划和回滚步骤 | 缺失时阻塞生产登录和开放接口签发 |
| 聚合支付凭证 | 记录商户号、密钥引用、证书引用、回调验签资料、退款权限和财务责任人 | 缺失时阻塞生产收款、退款和日结 |
| 企业微信凭证 | 记录 corp id、应用 secret 引用、会话存档密钥引用、回调 token/AES key 引用和同步负责人 | 缺失时阻塞企业微信 P0 同步能力 |
| 第三方供应商 API 密钥 | 记录供应商、API/SDK、测试/生产环境、签名算法、回调密钥引用和异常停用方式 | 缺失时阻塞真实供应商联调或生产通过 |
| 开放平台应用/回调签名密钥 | 记录外部应用、app key、secret ref、webhook secret ref、允许 endpoint 和轮换窗口 | 缺失时阻塞第三方双向开放生产接入 |
| NAS SMB 凭证 / 备份加密口令预留 | 记录 SMB 用户、共享路径、挂载路径、`credential_ref`、保管责任、恢复演练使用方式和紧急替换步骤；当前备份归档不加密，备份加密口令仅为预留项 | 缺少 SMB 凭证和挂载证据时阻塞生产可恢复结论；当前不因未发放备份加密口令阻塞 |

## 5. 验收标准

Given 运维准备发放生产数据库密码，  
When 台账中没有责任人、到期日或 Runbook，  
Then 不得发放该凭证。

Given 某聚合支付密钥距离到期不足 7 天，  
When 系统执行到期检查，  
Then 必须生成提醒任务并记录到期提醒事件。

Given 某供应商 API 密钥疑似泄露，  
When IT 执行应急停用，  
Then 必须记录停用时间、影响系统、替换凭证、责任人和复盘链接。

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
