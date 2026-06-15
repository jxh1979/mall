# 生产阻断清单 V0.1

## 1. 机器标记

- PRODUCTION_BLOCKER_REGISTER_READY
- PRODUCTION_BLOCKERS_OPEN
- NO_FAKE_PRODUCTION_EVIDENCE_CLOSURE
- NO_SUPPLIER_MOCK_AS_PRODUCTION_EVIDENCE
- VERIFY_PRODUCTION_BLOCKERS_READY

## 2. 结论

当前仍存在必须由 IT、采购、财务、运营、安全或供应商回填的生产阻断项。本文不重新打开业务决策，不允许把 mock、口头确认、截图占位或未纳管设备当作生产证据。

| 指标 | 数量 |
|---|---:|
| 生产前置台账阻断项 | 12 |
| 生产凭证轮换阻断项 | 7 |
| 安全 findings open / open_for_prod_review | 4 |
| 已暂缓或后续 review 项 | 2 |

## 3. 生产前置台账阻断项

| item_id | category | status | blocking_scope | owner | next_action |
| --- | --- | --- | --- | --- | --- |
| PRD-MDM-001 | MDM | pending_info | 阻塞生产工作手机、平板、一体机使用；不阻塞开发 mock | IT/采购 | IT 选型、设备 enroll、策略下发和截图归档 |
| PRD-PAY-001 | payment | pending_info | 阻塞真实收款测试；不阻塞支付 mock | 财务/IT | 获取测试商户号、回调地址、对账文件样例 |
| PRD-PAY-002 | payment | pending_info | 阻塞生产收款、退款和日结 | 财务/IT | 建立商户资料、密钥引用、退款审批和对账口径 |
| PRD-FACE-001 | face_age | pending_info | 阻塞酒吧生产年龄核验通过 | 采购/IT/运营 | 获取 SDK/API、错误码、测试账号、合规材料 |
| PRD-NAS-AUTOMOUNT-001 | nas | pending_info | 阻塞长期预生产和生产自动恢复；不阻塞开发验证 | IT/运维 | IT 安装后回填重启后 findmnt、读写删除测试和截图 |
| PRD-NAS-SCHEDULE-001 | nas | pending_info | 阻塞长期预生产和生产自动备份；不阻塞手动备份演练 | IT/运维 | IT 安装后回填调度配置、最近一次 scheduled 备份和 .sha256 |
| PRD-NAS-ALERT-001 | nas | pending_info | 阻塞长期预生产和生产运维准入；不阻塞开发验证 | IT/运维/安全 | 回填容量告警、失败告警和通知送达证据 |
| PRD-NAS-DRILL-CADENCE-001 | nas | pending_info | 阻塞长期预生产和生产可恢复结论；不阻塞当前编码 | IT/运维 | 回填最近一次恢复演练报告、临时库清理和验证表 |
| PRD-CRED-001 | credential | pending_info | 阻塞长期预生产和生产 | IT/安全/财务 | 建立密钥引用、轮换周期、责任人和应急回滚 |
| PRD-WECOM-001 | wecom | pending_info | 阻塞企业微信 P0 同步能力 | IT/运营 | 配置应用、通讯录、会话存档、回调和同步任务 |
| PRD-3P-001 | third_party | pending_info | 不阻塞 P0 主闭环；阻塞真实厂商联调 | 采购/IT | 收集 SDK/API、签名、回调、字段映射和测试账号 |
| PRD-160-001 | ops160 | pending_info | 阻塞长期预生产和生产；不阻塞用户级覆盖开发验证 | IT/运维 | 迁移用户级覆盖到系统配置，接入 systemd 和告警 |

## 4. 生产凭证轮换阻断项

| credential_item_id | category | status | blocking_scope | owner | next_action |
| --- | --- | --- | --- | --- | --- |
| PRD-CRED-OPS-001 | credential | pending_info | 阻塞长期预生产和生产 | IT/运维/安全 | 建立账号负责人、授权范围、到期日和停用步骤 |
| PRD-CRED-DB-001 | credential | pending_info | 阻塞生产数据库接入 | IT/运维/安全 | 生成强密码、写入安全保管位置、补到期日和维护窗口 |
| PRD-CRED-OBJ-001 | credential | pending_info | 阻塞生产附件、证据包、对象备份接入 | IT/运维/安全 | 确认桶范围、权限、Access Key 引用和停用方式 |
| PRD-CRED-JWT-001 | credential | pending_info | 阻塞生产登录和开放接口签发 | IT/安全/技术负责人 | 设计签名版本、双验窗口、旧 token 失效计划 |
| PRD-CRED-3P-001 | credential | pending_info | 阻塞真实供应商联调或生产通过 | IT/采购/财务/运营/安全 | 收集 SDK/API、签名算法、回调地址、测试账号和证据 |
| PRD-CRED-OPEN-001 | credential | pending_info | 阻塞第三方双向开放生产接入 | IT/安全/集成开发 | 建立应用、endpoint、allowed scopes、密钥引用和轮换窗口 |
| PRD-CRED-NAS-001 | credential | pending_info | 阻塞生产可恢复结论 | IT/运维/安全 | IT 将真实密码写入安全保管位置或 160 受控凭证文件，不得写入文档、聊天或仓库 |

## 5. 安全 findings 阻断项

| severity | id | status | blocking_scope | owner | next_action |
| --- | --- | --- | --- | --- | --- |
| P0 | SEC-P0-MDM-DEVICE-001 | open | 阻塞生产工作手机、平板、一体机执行高风险核销/出酒/支付相关动作 | IT/采购/运维 | 完成 MDM 选型、enroll、策略下发、设备绑定截图和生产准入记录 |
| P0 | SEC-P0-OPS-RECOVERY-001 | open | 阻塞公开生产安全准入；不阻塞开发联调 | IT/安全/运维 | P0-A no-system-write boundary：Codex 不安装系统级配置；后续由 IT 固化 160 开机自动挂载、定时备份、容量告警、失败告警和周期性恢复演练；生产凭证实际发放前按明细台账补责任人、到期日、credential_ref、提醒和轮换证据 |
| P1 | SEC-P1-WEBHOOK-REPLAY-001 | open | 阻塞真实支付/人脸/设备/开放平台供应商联调上线；不阻塞 mock adapter | 后端/IT/供应商联调负责人 | 按供应商 SDK/API 补字段映射、nonce、timestamp、签名验签、幂等、补偿和对账测试 |
| P2 | SEC-P2-CSRF-API-001 | open_for_prod_review | 不阻塞 Bearer/API 调用 smoke；若后台 Web 采用 Cookie 会话则阻塞公开生产 | 后端/前端/安全 | 正式认证方案定稿时按 Bearer、SameSite Cookie 或双提交 Token 分支复核 |

## 6. 暂缓或生产方案定稿复核项

| id | status | blocking_scope | next_action |
| --- | --- | --- | --- |
| PRD-HTTPS-001 | not_started | 仅阻塞公开生产域名访问，不阻塞内网预生产和开发验收 | 等总控启动公开访问后再配置 |
| SEC-P2-SMOKE-HASH-001 | accepted_for_dev_smoke | 仅允许 local/dev/test；公开生产必须替换为正式身份源或强密码哈希方案 | 生产认证不得复用该 smoke 账号存储；若自建密码，采用 Argon2id/bcrypt/PBKDF2 并补 MFA/锁定策略 |

## 7. 使用规则

1. 每次 IT、采购、财务、运营、安全或供应商补齐证据后，先更新生产前置台账或 findings register。
2. 执行 `npm run docs:production-blockers` 重新生成本文和 JSON。
3. 执行 `npm run verify:production-blockers`、`npm run verify:production-readiness-evidence`、`npm run verify:delivery-gates`。
4. 不得手工把本文中的阻断项删掉；必须从来源台账或 findings 关闭。
