# Findings Register V1

> 本清单由 `tools/generate-formal-security-baseline.mjs` 生成。P0 未修复或未正式豁免前，不允许进入公开生产安全准入。

- Generated from deterministic workspace inputs; execution time is captured by the verification logs.
- OpenAPI operation count: 207
- OpenAPI SHA-256: `4d510fb82cab592aca9b96994db74a67139563708065904585e3a281d9da3012`

## status-policy

| 状态 | 含义 |
|---|---|
| open | 未修复，仍需进入责任人计划 |
| resolved | 已修复并有验证依据 |
| accepted_for_dev_smoke | 仅允许当前开发 smoke，不能迁移到生产 |
| open_for_prod_review | 生产方案定稿时必须复核 |
| needs_review | 静态证据不足，需要人工复核 |

## findings

| severity | id | title | status | blocking_scope | owner | evidence | next_action |
|---|---|---|---|---|---|---|---|
| P0 | `SEC-P0-AUTH-PROD-001` | 正式生产认证 Provider 与会话治理未落地 | resolved | 阻塞公开生产和长期预生产；不阻塞当前 dev smoke 编码验证 | 技术负责人/安全 | `ProductionAuthService`/`ProductionAuthController`/`ProductionBearerAuthenticationFilter`、`V018__p0_production_auth_provider.sql` 和 `ProductionAuthJdbcIntegrationTests` 已落地 | 继续保留 MDM、NAS、密钥轮换、真实供应商接入等非代码生产前置项的阻断状态 |
| P0 | `SEC-P0-MDM-DEVICE-001` | MDM/Android Enterprise 生产纳管仍处 pending_info | open | 阻塞生产工作手机、平板、一体机执行高风险核销/出酒/支付相关动作 | IT/采购/运维 | `production-readiness-implementation-ledger-template-v0.1.md` 中 PRD-MDM-001 为 pending_info | 完成 MDM 选型、enroll、策略下发、设备绑定截图和生产准入记录 |
| P0 | `SEC-P0-OPS-RECOVERY-001` | NAS 挂载、真实同步和从 NAS 恢复证据已完成，生产凭证发放证据仍需补齐 | open | 阻塞公开生产安全准入；不阻塞开发联调 | IT/安全/运维 | `verify:ops160-execution-policy` 已固化 NAS 服务器 `192.168.88.113` 和 160 后端测试口径；`verify:production-credentials` 已覆盖生产账号、PostgreSQL、对象存储、JWT/Token、第三方/支付/企微/开放平台密钥、NAS SMB 凭证和备份加密预留台账；NAS 服务器 `192.168.88.113` 的真实挂载、备份同步和从 NAS 恢复演练 `restore.drill_completed` 已通过 | P0-A no-system-write boundary：Codex 不安装系统级配置；后续由 IT 固化 160 开机自动挂载、定时备份、容量告警、失败告警和周期性恢复演练；生产凭证实际发放前按明细台账补责任人、到期日、credential_ref、提醒和轮换证据 |
| P1 | `SEC-P1-DOCS-EXPOSURE-001` | OpenAPI/Swagger 文档在当前 SecurityConfig 中公开放行 | resolved | 公开生产匿名访问已保护；dev/test 仍可公开用于契约导出和联调验证 | 后端/运维 | `SecurityConfig` 仅在非 prod profile 对 `apiDocumentationPaths()` permitAll；`verify:prod-docs-access` 已覆盖 prod 匿名访问保护 | 后续若变更 SecurityConfig，必须继续执行 `npm run verify:prod-docs-access` 和 `npm run verify:security-baseline` |
| P1 | `SEC-P1-WEBHOOK-REPLAY-001` | 真实供应商 Webhook 签名、防重放和轮询校验需在供应商资料到位后补实 | open | 阻塞真实支付/人脸/设备/开放平台供应商联调上线；不阻塞 mock adapter | 后端/IT/供应商联调负责人 | OpenAPI 当前存在 3 个开放应用或 webhook 签名上下文 operation | 按供应商 SDK/API 补字段映射、nonce、timestamp、签名验签、幂等、补偿和对账测试 |
| P2 | `SEC-P2-CREDENTIALS-001` | Bearer 原始 token 不再进入 Spring Security credentials | resolved | 已修复；继续通过单测防回归 | 后端 | `BearerAuthenticationFilter` 构造 `UsernamePasswordAuthenticationToken(user, null, authorities)` | 保留 `BearerAuthenticationFilterTests`，后续正式认证实现沿用不把原始凭据放入认证对象的原则 |
| P2 | `SEC-P2-SMOKE-HASH-001` | dev smoke AuthService 使用 SHA-256 密码摘要，不可迁移到生产认证 | accepted_for_dev_smoke | 仅允许 local/dev/test；公开生产必须替换为正式身份源或强密码哈希方案 | 后端/安全 | `AuthService` 使用 `MessageDigest.getInstance("SHA-256")`，且类上存在 local/dev/test profile | 生产认证不得复用该 smoke 账号存储；若自建密码，采用 Argon2id/bcrypt/PBKDF2 并补 MFA/锁定策略 |
| P2 | `SEC-P2-CSRF-API-001` | 当前 API 关闭 CSRF，需在最终认证形态中复核 Web Cookie 场景 | open_for_prod_review | 不阻塞 Bearer/API 调用 smoke；若后台 Web 采用 Cookie 会话则阻塞公开生产 | 后端/前端/安全 | `SecurityConfig` 调用 `csrf.disable()` | 正式认证方案定稿时按 Bearer、SameSite Cookie 或双提交 Token 分支复核 |
| P3 | `SEC-P3-BASELINE-AUTOMATION-001` | 安全基线已脚本化，后续需接入发布闸口 | resolved | 已纳入完整 P0 验证链 | 技术负责人 | `verify:p0` 已包含 `verify:security-baseline`，安全基线验证会随完整 P0 验证执行 | 后续发布前继续执行 `npm run verify:p0`，不要绕过安全基线闸口 |

## manual-tenant-isolation-findings (手工补登 2026-06-18)

> 以下跨租户越权 finding 系列为**手工补登**（本文件主表由 `tools/generate-formal-security-baseline.mjs` 生成，本小节待生成器纳管）。对应数据隔离规格 `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` 的 ISO-GAP-01。
> 隔离约定：隔离列（`merchant_id`/`organization_id`/`project_id`）的值在写入时由 `AuthorizationService` 的安全 token 注入，**绝不取请求体/查询参数**；读取按 token 的 scope 强制追加过滤。
> 状态语义：`mitigated_by_migration` = 对应迁移已落隔离列/索引，行级隔离风险由迁移 + Service scope 过滤缓解，仍待 `verify:tenant-isolation` 闸口（ISO-GAP-02）做契约级回归；`open_for_test` = 隔离列已落但缺自动化越权回归用例，待 TDD 回归 plan（ISO-GAP-04）。

| severity | id | title | status | migration | scope_key | next_action |
|---|---|---|---|---|---|---|
| P1 | `SEC-P1-TENANT-ISOLATION-FINANCE-001` | finance（ar_ap_items / finance_daily_closes）跨商户越权读写 | mitigated_by_migration | V044 | merchant_id (direct) + daily_close scope 唯一约束 | 待 `verify:tenant-isolation`（ISO-GAP-02）+ AC-ISO-1/2 回归 |
| P1 | `SEC-P1-TENANT-ISOLATION-INVENTORY-001` | inventory 10 表跨商户越权 | mitigated_by_migration | V046 | merchant_id (direct) | VALIDATE `chk_stock_adjustment_type`（NOT VALID）+ AC-ISO 回归 |
| P1 | `SEC-P1-TENANT-ISOLATION-BAR-001` | bar（tables/reservations/products/orders/production_tickets/bills）跨商户越权 | mitigated_by_migration | V047 | merchant_id (direct) | AC-ISO-1/2 回归 |
| P1 | `SEC-P1-TENANT-ISOLATION-GOVERNANCE-001` | governance（roles 组织级 + supervisor_review_decisions 无 scope 列）越权 | open_for_test | V048 / V050 / V057 | organization_id (direct) / supervisor_review_decisions 无 scope 列（V057 approval_level/required_approver_rank 仅留痕） | 裁决复核记录是否需 org/merchant 隔离 + AC-ISO 回归 |
| P1 | `SEC-P1-TENANT-ISOLATION-AMUSEMENT-001` | amusement 4 表跨商户越权（且 merchant_id 无索引） | open_for_test | V049 | merchant_id (direct，无索引 ISO-GAP-03) | 补部分索引 + AC-ISO-1/2 回归 |
| P1 | `SEC-P1-TENANT-ISOLATION-COMPLIANCE-001` | compliance_reminders 跨商户越权 | mitigated_by_migration | V051 | merchant_id (direct) | AC-ISO-1/2 回归 |
| P1 | `SEC-P1-TENANT-ISOLATION-FLOORPLAN-001` | mall_floor_plans 跨商户越权 | mitigated_by_migration | V052 | merchant_id (direct) | AC-ISO-1/2 回归 |
| P1 | `SEC-P1-TENANT-ISOLATION-CUSTOMER-POINTS-001` | customer_points_rules 跨综合体越权 | mitigated_by_migration | V060 | project_id (direct，综合体级) | AC-ISO-1/2 回归 |
| P1 | `SEC-P1-TENANT-ISOLATION-TRACEABILITY-001` | 业务流程/服务责任留痕（business_process_instances / business_process_step_records / service_responsibility_records）跨租户越权 | mitigated_by_migration | V059 | merchant_id+project_id (direct) / 经 process_instance_id inherited / merchant_id (direct) | AC-ISO-1/2 回归（含 inherited join 过滤） |

> 共性 next_action：`verify:tenant-isolation` 自动闸口待建（ISO-GAP-02）；V044–V061 TDD 风格越权回归 plan 待补（ISO-GAP-04）。`saved_bottles`（V046 merchant_id / V061 新增 customer_phone 等）敏感字段 at-rest 加密为 ISO-GAP-06。

## production-blocking-summary

| 阻断级别 | 规则 |
|---|---|
| P0 open | 阻塞公开生产或长期预生产安全准入，必须修复或正式豁免 |
| P1 open | 阻塞对应公开面或真实供应商联调，必须有责任人和计划 |
| P2/P3 open | 不阻塞当前编码，但必须进入后续排期或风险接受记录 |

## resolved-evidence

| finding | 证据 |
|---|---|
| `SEC-P2-CREDENTIALS-001` | Bearer 认证对象的 credentials 为 `null`，避免把原始 token 保存在 Spring Security authentication credentials |

## next-verification

1. 修复任一 finding 后，重新执行 `npm run docs:security-baseline`。
2. 再执行 `npm run verify:security-baseline`。
3. 生产前还必须执行 `npm run verify:p0-contract` 和完整 P0 验证脚本。
