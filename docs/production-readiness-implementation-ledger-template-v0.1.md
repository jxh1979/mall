# 生产前置资料实施台账模板 V0.1

## 1. 文档定位

本文档是生产前置资料的实施台账模板，用于把 MDM、支付、NAS、密钥轮换、人脸年龄核验、企业微信、第三方供应商、160 环境、编码主机保护、安全基线等上线前置事项统一管理。

它不是新的拍板清单。除总控已经明确暂缓的“域名 HTTPS 绑定”外，本文档中的事项按默认推荐方案执行；缺资料时记录为 `pending_info` 或 `blocked`，不得反向打开业务范围决策。

## 2. 状态枚举

| 状态 | 含义 |
|---|---|
| not_started | 未开始 |
| pending_info | 等待资料、账号、供应商或配置 |
| in_progress | 正在实施 |
| blocked | 被外部条件阻塞，必须写明阻塞范围 |
| ready_for_test | 配置完成，等待验收 |
| passed | 已验收通过 |
| failed | 验收失败，需整改 |
| waived | 已正式豁免，必须有审批证据 |
| not_applicable | 当前环境不适用，必须说明原因 |

## 3. 台账字段

| 字段 | 必填 | 说明 |
|---|---|---|
| item_id | 是 | 前置项编号，如 PRD-MDM-001 |
| category | 是 | MDM、payment、face_age、nas、credential、wecom、third_party、ops160、security、domain |
| item_name | 是 | 前置项名称 |
| default_solution | 是 | 默认推荐方案 |
| owner_department | 是 | 责任部门，如 IT、采购、财务、运营、法务、安全 |
| owner_person | 否 | 具体责任人，实施前补齐 |
| supplier_name | 否 | 供应商名称，未定填 TBD |
| status | 是 | 使用本文状态枚举 |
| target_date | 否 | 目标完成日期 |
| evidence_link | 否 | 合同、截图、测试报告、验收记录、恢复演练记录等证据链接 |
| last_test_time | 否 | 最近一次测试或复验时间 |
| blocking_scope | 否 | 阻塞范围，如“仅阻塞公开生产，不阻塞开发联调” |
| fallback_mode | 否 | 临时方案或降级方案 |
| audit_event | 否 | 关联审计事件或操作记录 |
| credential_ref | 否 | 密钥引用编号，不得写明文密钥 |
| runbook_ref | 是 | 对应 Runbook 或设计文档 |
| next_action | 是 | 下一步动作 |

## 4. 默认台账模板

| item_id | category | item_name | default_solution | owner_department | supplier_name | status | blocking_scope | runbook_ref | next_action |
|---|---|---|---|---|---|---|---|---|---|
| PRD-MDM-001 | MDM | Android 设备纳管 | Android Enterprise / Android Management API + 商业 MDM | IT/采购 | TBD | pending_info | 阻塞生产工作手机、平板、一体机使用；不阻塞开发 mock | `mdm-android-enterprise-implementation-plan-v0.1.md` | IT 选型、设备 enroll、策略下发和截图归档 |
| PRD-PAY-001 | payment | 聚合支付测试商户 | 聚合支付优先，保留直连接口抽象 | 财务/IT | TBD | pending_info | 阻塞真实收款测试；不阻塞支付 mock | `p0-api-contract-pack-v0.1.md` | 获取测试商户号、回调地址、对账文件样例 |
| PRD-PAY-002 | payment | 生产商户与退款权限 | 聚合支付生产商户 + 回调 + 对账 + 退款权限 | 财务/IT | TBD | pending_info | 阻塞生产收款、退款和日结 | `finance-ar-ap-ledger-development-design-v0.1.md` | 建立商户资料、密钥引用、退款审批和对账口径 |
| PRD-FACE-001 | face_age | 酒吧年龄/人脸核验 | 第三方接口或等效设备；供应商未定先 mock | 采购/IT/运营 | TBD | pending_info | 阻塞酒吧生产年龄核验通过 | `third-party-supplier-integration-kit-v0.1.md` | 获取 SDK/API、错误码、测试账号、合规材料 |
| PRD-NAS-001 | nas | NAS 备份目标 | NAS `192.168.88.113`，SMB 共享 `bn`，UNC `//192.168.88.113/bn`，160 挂载 `/mnt/complex-nas-backup`，保留 30 天，容量上限 1TB，不启用备份归档加密 | IT | `192.168.88.113` | passed | 真实 NAS 备份目标和一次性写入验证已通过；生产前仍需持久化挂载、定时备份和容量告警 | `nas-backup-and-restore-runbook-v0.1.md` | 将当前手工挂载固化为受控开机自动挂载方案，补定时备份、容量告警和失败告警证据 |
| PRD-NAS-002 | nas | NAS 恢复演练 | 备份后必须恢复演练，当前验收归档为 `.tar.gz` + `.sha256` + `restore.drill_completed` | IT/运维 | `192.168.88.113` | passed | 真实 NAS 备份同步和从 NAS 恢复演练已通过；生产前仍需周期化恢复演练计划 | `nas-backup-and-restore-runbook-v0.1.md` | 按月或上线前固定执行恢复演练，并将最新报告持续回填 evidence |
| PRD-NAS-AUTOMOUNT-001 | nas | NAS 开机自动挂载 | P0-A no-system-write boundary：Codex 只提供预检脚本，IT 负责安装自动挂载 | IT/运维 | `192.168.88.113` | pending_info | 阻塞长期预生产和生产自动恢复；不阻塞开发验证 | `nas-backup-and-restore-runbook-v0.1.md#64-p0-a-no-system-write-boundary` | IT 安装后回填重启后 `findmnt`、读写删除测试和截图 |
| PRD-NAS-SCHEDULE-001 | nas | NAS 定时备份 | 使用 `ops/160/complex-backup-schedule.example` 作为非安装模板，由 IT 转为真实调度 | IT/运维 | 自管 | pending_info | 阻塞长期预生产和生产自动备份；不阻塞手动备份演练 | `nas-backup-and-restore-runbook-v0.1.md#64-p0-a-no-system-write-boundary` | IT 安装后回填调度配置、最近一次 scheduled 备份和 `.sha256` |
| PRD-NAS-ALERT-001 | nas | NAS 容量告警和失败告警 | 使用 `ops/160/complex-backup-alert.example`，真实 secret 只进凭证保管位置 | IT/运维/安全 | 企业微信/邮件 | pending_info | 阻塞长期预生产和生产运维准入；不阻塞开发验证 | `server-160-runbook-v0.2.md#51-p0-a-no-system-write-boundary` | 回填容量告警、失败告警和通知送达证据 |
| PRD-NAS-DRILL-CADENCE-001 | nas | 周期恢复演练 | 至少月度或上线前执行恢复演练并回填 evidence | IT/运维 | `192.168.88.113` | pending_info | 阻塞长期预生产和生产可恢复结论；不阻塞当前编码 | `nas-backup-and-restore-runbook-v0.1.md#64-p0-a-no-system-write-boundary` | 回填最近一次恢复演练报告、临时库清理和验证表 |
| PRD-CRED-001 | credential | 生产账号和密钥轮换 | 人工台账 + 到期提醒 + Runbook | IT/安全/财务 | TBD | pending_info | 阻塞长期预生产和生产 | `production-credential-rotation-runbook-v0.1.md` | 建立密钥引用、轮换周期、责任人和应急回滚 |
| PRD-WECOM-001 | wecom | 企业微信配置 | 一次授权、自动账号映射、每日同步、失败告警、基础质检 | IT/运营 | 企业微信 | pending_info | 阻塞企业微信 P0 同步能力 | `third-party-integration-development-design.md` | 配置应用、通讯录、会话存档、回调和同步任务 |
| PRD-3P-001 | third_party | 机器人/视频/设备厂商接入资料 | 双向开放接口 + 供应商 adapter；Webhook + 定时轮询校验 | 采购/IT | TBD | pending_info | 不阻塞 P0 主闭环；阻塞真实厂商联调 | `third-party-supplier-integration-kit-v0.1.md` | 收集 SDK/API、签名、回调、字段映射和测试账号 |
| PRD-160-001 | ops160 | 160 运行配置生产化 | `/etc/complex/complex.env`、systemd、日志、健康检查、告警 | IT/运维 | 自管 | pending_info | 阻塞长期预生产和生产；不阻塞用户级覆盖开发验证 | `server-160-runbook-v0.2.md` | 迁移用户级覆盖到系统配置，接入 systemd 和告警 |
| PRD-160-CODE-001 | ops160 | 160 编码保护基线 | 正式编码、完整 P0 验证、安全扫描和提交只认 `192.168.88.160` 的 `/home/james/complex-management`；本机只负责进度检查、任务拆解和质量检查 | 技术负责人/运维 | 自管 | passed | 阻塞非 160 主机形成正式编码和完整验收结论；不阻塞本机协调和文档审查 | `coding-host-protection-baseline-v0.1.md` | 已建立 `verify:coding-host-protection`、证据 JSON 和 package 闸口；后续迁移主机时必须更新基线和证据 |
| PRD-HTTPS-001 | domain | 域名 HTTPS | 总控已确认先不绑定 | IT/总控 | TBD | not_started | 仅阻塞公开生产域名访问，不阻塞内网预生产和开发验收 | `server-160-runbook-v0.2.md` | 等总控启动公开访问后再配置 |
| PRD-SEC-001 | security | 正式安全基线 | V1 已生成；后续 OpenAPI、认证、权限、第三方、支付、MDM、生产凭证或 160 运维证据变化后必须重生成 | 安全/技术负责人 | 自管/第三方 | passed | 安全基线已生成并可验证；仍需关闭 findings 中 P0/P1 open 项后才可公开生产 | `formal-security-baseline-generation-task-v0.1.md`、`security-scans/formal-security-baseline-v1.md`、`security-scans/findings-register-v1.md`、`docs/production-readiness-evidence/security-baseline-latest.json` | `npm run verify:security-baseline` 与 `npm run verify:production-readiness-evidence` 通过 |

## 4.1 生产凭证明细台账模板

本节把 `PRD-CRED-001` 拆成可验收的生产凭证明细。P0 仍采用“人工台账 + 到期提醒 + Runbook”，不引入自动轮换系统；但每一类生产账号、数据库、对象存储、JWT/第三方密钥都必须有独立行、责任人、到期日、提醒策略、轮换步骤、回滚步骤、验证步骤和证据链接。系统只接受 credential_ref，不接受任何明文密码、明文 token、明文 API secret 或私钥正文。

| credential_item_id | category | credential_type | system_scope | environment | owner_department | supplier_name | credential_ref | reminder_days | rotation_status | blocking_scope | runbook_ref | verification_step | rollback_step | rotation_evidence_link | next_action |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| PRD-CRED-OPS-001 | credential | 生产服务器/运维账号 | 160 或等效预生产/生产服务器 | prod | IT/运维/安全 | 自管 | 待录入账号引用 | 30,7,1 | pending_info | 阻塞长期预生产和生产 | `production-credential-rotation-runbook-v0.1.md#71-生产服务器运维账号轮换` | 登录、最小权限、健康检查和审计事件验证 | 恢复旧账号最小权限或停用新账号 | 待归档 | 建立账号负责人、授权范围、到期日和停用步骤 |
| PRD-CRED-DB-001 | credential | PostgreSQL 生产库 | 应用账号、管理账号、备份账号 | prod | IT/运维/安全 | PostgreSQL | 待录入数据库密码引用 | 30,7,1 | pending_info | 阻塞生产数据库接入 | `production-credential-rotation-runbook-v0.1.md#72-postgresql-生产库密码轮换` | 数据库连接、Flyway 状态、健康检查、备份和恢复演练抽检 | 恢复旧密码引用并暂停切换 | 待归档 | 既成事实（2026-07-06 prod 切换后）：运行时 app 角色 complex_app 已轮换 55 位强密码且被 REVOKE 最小权限；owner/Flyway 超级角色 complex 密码仍为 dev 默认 complex_dev_pwd，运行时以 COMPLEX_DATABASE_CREDENTIAL_SEPARATION_ENFORCED=false 关闭凭证守卫掩盖此状态。待 IT 为 owner ALTER ROLE 生成强密码、同步 owner env 并重启，随后移除 enforced=false 让守卫 fail-closed 生效；补到期日和维护窗口 |
| PRD-CRED-OBJ-001 | credential | 对象存储 | MinIO/附件/证据包/备份对象桶 | prod | IT/运维/安全 | MinIO 或等效厂商 | 待录入对象存储 key 引用 | 30,7,1 | pending_info | 阻塞生产附件、证据包、对象备份接入 | `production-credential-rotation-runbook-v0.1.md#73-对象存储-access-keysecret-key-轮换` | 上传、下载、签名 URL、桶权限和备份同步验证 | 恢复旧 key 引用并停用新 key | 待归档 | 确认桶范围、权限、Access Key 引用和停用方式 |
| PRD-CRED-JWT-001 | credential | JWT/Token 签名密钥 | 登录、刷新、登出、开放平台签名、终端会话 | prod | IT/安全/技术负责人 | 自管 | 待录入 JWT 签名密钥引用 | 30,7,1 | pending_info | 阻塞生产登录和开放接口签发 | `production-credential-rotation-runbook-v0.1.md#74-jwttoken-签名密钥轮换` | 登录、刷新、登出、旧验签窗口和终端会话验证 | 恢复旧签名版本并记录影响范围 | 待归档 | 设计签名版本、双验窗口、旧 token 失效计划 |
| PRD-CRED-3P-001 | credential | 第三方供应商 API 密钥 / 聚合支付凭证 / 企业微信凭证 | 支付、企业微信、人脸年龄核验、机器人/视频/设备供应商 | prod | IT/采购/财务/运营/安全 | TBD | 待录入供应商凭证引用 | 30,7,1 | pending_info | 阻塞真实供应商联调或生产通过 | `production-credential-rotation-runbook-v0.1.md#75-第三方支付企业微信和开放平台密钥轮换` | Webhook 验签、轮询校验、退款/对账、会话同步或供应商 API 抽检 | 回滚旧供应商凭证引用并暂停生产切换 | 待供应商回执 | 收集 SDK/API、签名算法、回调地址、测试账号和证据 |
| PRD-CRED-OPEN-001 | credential | 开放平台应用/回调签名密钥 | open-app-credential、webhook-signature、外部调用 | prod | IT/安全/集成开发 | 外部应用方/TBD | 待录入开放应用 secret 引用 | 30,7,1 | pending_info | 阻塞第三方双向开放生产接入 | `production-credential-rotation-runbook-v0.1.md#75-第三方支付企业微信和开放平台密钥轮换` | app key、签名、timestamp、nonce、防重放、幂等和回调验证 | 回滚旧 app/webhook secret 引用并禁用新配置 | 待归档 | 建立应用、endpoint、allowed scopes、密钥引用和轮换窗口 |
| PRD-CRED-NAS-001 | credential | NAS SMB 凭证 | NAS SMB 账号 `jxh1`、共享 `bn`、挂载 `/mnt/complex-nas-backup`、备份同步和恢复演练 | prod | IT/运维/安全 | NAS | `cred://prod/nas/smb/complex-backup-rw` | 30,7,1 | pending_info | 阻塞生产可恢复结论 | `production-credential-rotation-runbook-v0.1.md#41-p0-必登凭证类型` | SMB 挂载、读写删除测试、备份 `.tar.gz`/`.sha256` 同步和恢复演练报告归档 | 恢复旧 SMB 凭证或停用新凭证并暂停 NAS 同步 | 待归档 | IT 将真实密码写入安全保管位置或 160 受控凭证文件，不得写入文档、聊天或仓库 |
| PRD-CRED-NAS-KEY-001 | credential | NAS 备份加密口令预留 | 当前总控口径为备份归档不加密；该引用仅在后续启用加密时使用 | prod | IT/运维/安全 | NAS | `cred://prod/nas/backup-encryption-key` | 30,7,1 | not_applicable | 当前不阻塞；启用加密后阻塞生产可恢复结论 | `production-credential-rotation-runbook-v0.1.md#41-p0-必登凭证类型` | 当前验证 `ALLOW_UNENCRYPTED_BACKUPS=true`；启用加密后验证 `.tar.gz.enc`、`.sha256` 和恢复演练 | 启用加密失败时回滚到已正式接受风险的无加密备份口径 | 当前不适用 | 保留引用，不发放明文口令 |

## 5. 录入规则

P0-A no-system-write boundary：本台账允许记录由 Codex 生成的预检脚本、模板和校验结果，但不得把系统级挂载、定时任务、真实告警 secret 或 root-owned 凭证文件标记为 Codex 已安装。相关生产动作必须由 IT/运维执行并回填证据。

1. `credential_ref` 只能写密钥引用编号、保管位置或轮换台账编号，不得写真实密钥。
2. `evidence_link` 必须能指向可审计证据，如截图、验收报告、合同、恢复演练记录或供应商测试报告。
3. `blocked` 必须填写 `blocking_scope`，说明阻塞开发、联调、预生产、公开生产中的哪一类。
4. `waived` 必须有正式审批证据，且只能豁免非 P0 高风险项；安全、备份恢复、密钥轮换的 P0 阻断项不得口头豁免。
5. 供应商未确定时，`supplier_name=TBD`，但接口占位、adapter、Webhook、轮询校验和补偿策略仍按文档开发。
6. 域名 HTTPS 已确认先不绑定，只记录为公开生产前置，不作为当前编码阻断项。
7. P0 编码完成范围不包含 MDM 采购/enroll、真实聚合支付商户、企业微信、人脸年龄核验供应商、第三方厂商资料和 NAS 真实账号开通；这些事项只作为生产前置台账管理，不反向阻塞 P0 编码完成结论。

## 6. API 映射

| 能力 | OpenAPI operation | 说明 |
|---|---|---|
| 查询生产前置台账 | `listProductionReadinessItems` | 返回所有前置项状态、责任、证据和阻塞范围 |
| 更新生产前置台账 | `updateProductionReadinessItem` | 更新状态、责任人、证据、下一步动作和密钥引用 |
| 记录生产前置检查 | `createProductionReadinessCheck` | 执行准入检查并记录通过/失败/阻断项 |

## 7. 验收标准

| 场景 | Given | When | Then |
|---|---|---|---|
| 台账初始化 | P0 进入首批编码 | 初始化生产前置台账 | 默认 12 项均存在，状态不为空，责任部门和 Runbook 已填写 |
| 缺少关键资料 | MDM、NAS、密钥轮换或安全基线未完成 | 执行生产就绪检查 | 返回阻断项和阻塞范围，不得标记 `passed` |
| 更新证据 | IT 上传 MDM enroll 截图或 NAS 恢复报告 | 更新台账项 | 状态、证据链接、最近测试时间和审计事件可追溯 |
| 供应商未定 | 人脸年龄核验或设备厂商尚未采购 | 查询台账 | 状态为 `pending_info`，不阻塞 P0 接口占位和 mock，阻塞真实生产通过 |
| 域名 HTTPS 暂缓 | 总控未要求公开域名 | 执行检查 | 仅提示公开生产前置，不阻塞内网开发、预生产和编码验收 |
| 编码主机保护 | 开发或审计准备给出完整 P0 通过结论 | 执行 `npm run verify:coding-host-protection` 或完整 `npm run verify:p0` | 只有 160 的 `/home/james/complex-management` 仓库可通过；非 160 主机不得形成正式代码验收结论 |
| 密钥安全 | 运维填写密钥信息 | 保存台账 | 系统拒绝明文密钥，只接受 `credential_ref` |

## 8. 交付结论

生产前置资料从本模板开始纳入台账化管理。后续不再以聊天记录、口头说明或零散截图作为上线前置依据；所有前置项必须有状态、责任、证据、阻塞范围和下一步动作。
