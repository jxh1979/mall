# Findings Register V1.05

> 本清单由 `tools/generate-formal-security-baseline.mjs` 生成。P0 未修复或未正式豁免前，不允许进入公开生产安全准入。

- Generated from deterministic workspace inputs; execution time is captured by the verification logs.
- OpenAPI operation count: 1210
- OpenAPI SHA-256: `8e603ae233f85c1b3ddbf1a0c765d0e06c262f7dea3dea6718b497a5faae0193`

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
| P0 | `SEC-P0-TENANT-ISOLATION-001` | 跨租户/跨商户越权 finding 系列必须由安全基线纳管 | resolved | 已纳入安全基线生成器和交付闸口；继续通过 verify:tenant-isolation 防回归 | 安全/后端/架构负责人 | `data-isolation-and-tenant-scope-security-spec-v0.1.md` §3A 自动基线、`domain-ownership-scope-audit-v0.1.csv`、`verify:tenant-isolation` 与 `tenant-isolation-baseline.json` 已纳入生成器 | 后续新增带 scope 数据表时同步 CSV/迁移/Repository，并执行 `npm run verify:tenant-isolation` 与 `npm run verify:security-baseline` |
| P2 | `SEC-P2-TENANT-ISOLATION-PROJECT-READ-001` | 招商/机构 5 表跨项目读残留（棘轮 F1）按 project_id 下沉仓储谓词 | resolved | 已修复：leasing_leads/leasing_intents/leasing_brands/leasing_follow_ups/external_partner_orgs 读接口按调用方项目 scope（organizationId）过滤，平台管理员/全局平台员工全量；access_grant_requests 保留 baseline（AccessService.canViewGrantRequest 服务层按 actor 隔离更细，静态扫描器不识别服务层闸，非缺口） | 安全/后端 | `AuthorizationService.currentProjectScopeQuietly`、`JdbcLeasingRepository`/`JdbcPartnerRepository` scoped 查询、`LeasingServiceTests.projectScopedStaffOnlySeesOwnProjectRows`、`PartnerServiceTests.listPartnerOrgsPassesProjectScopeToRepository`、`PartnerJdbcIntegrationTests`/`LeasingJdbcIntegrationTests` 负例、tenant-isolation baseline 收紧至 1 条 | 若扫描器将来识别服务层闸，可移除最后 1 条 baseline 条目 |
| P1 | `SEC-P1-DOCS-EXPOSURE-001` | OpenAPI/Swagger 文档在当前 SecurityConfig 中公开放行 | resolved | 公开生产匿名访问已保护；dev/test 仍可公开用于契约导出和联调验证 | 后端/运维 | `SecurityConfig` 仅在非 prod profile 对 `apiDocumentationPaths()` permitAll；`verify:prod-docs-access` 已覆盖 prod 匿名访问保护 | 后续若变更 SecurityConfig，必须继续执行 `npm run verify:prod-docs-access` 和 `npm run verify:security-baseline` |
| P1 | `SEC-P1-WEBHOOK-REPLAY-001` | 真实供应商 Webhook 签名、防重放和轮询校验需在供应商资料到位后补实 | open | 阻塞真实支付/人脸/设备/开放平台供应商联调上线；不阻塞 mock adapter | 后端/IT/供应商联调负责人 | OpenAPI 当前存在 5 个开放应用或 webhook 签名上下文 operation | 按供应商 SDK/API 补字段映射、nonce、timestamp、签名验签、幂等、补偿和对账测试 |
| P2 | `SEC-P2-CREDENTIALS-001` | Bearer 原始 token 不再进入 Spring Security credentials | resolved | 已修复；继续通过单测防回归 | 后端 | `BearerAuthenticationFilter` 构造 `UsernamePasswordAuthenticationToken(user, null, authorities)` | 保留 `BearerAuthenticationFilterTests`，后续正式认证实现沿用不把原始凭据放入认证对象的原则 |
| P2 | `SEC-P2-SMOKE-HASH-001` | dev smoke AuthService 使用 SHA-256 密码摘要，不可迁移到生产认证 | resolved | 仅允许 local/dev/test；公开生产必须替换为正式身份源或强密码哈希方案 | 后端/安全 | `AuthService` 使用 `MessageDigest.getInstance("SHA-256")`，且类上存在 local/dev/test profile | 生产认证不得复用该 smoke 账号存储；若自建密码，采用 Argon2id/bcrypt/PBKDF2 并补 MFA/锁定策略 |
| P2 | `SEC-P2-CSRF-API-001` | 当前 API 关闭 CSRF，需在最终认证形态中复核 Web Cookie 场景 | open_for_prod_review | 不阻塞 Bearer/API 调用 smoke；若后台 Web 采用 Cookie 会话则阻塞公开生产 | 后端/前端/安全 | `SecurityConfig` 调用 `csrf.disable()`；客人 web 端 httpOnly 会话 cookie（3.3 免登录）已自带防御——SameSite=Strict + `CustomerSessionCookieFilter` 对 cookie 鉴权写操作强制 X-Requested-With 自定义头（含单测） | 员工后台仍为 Bearer 头 token（无 cookie 环境凭证）；若后台改用 Cookie 会话再按 SameSite/双提交复核 |
| P3 | `SEC-P3-BASELINE-AUTOMATION-001` | 安全基线已脚本化，后续需接入发布闸口 | resolved | 已纳入完整 P0 验证链 | 技术负责人 | `verify:p0` 已包含 `verify:security-baseline`，安全基线验证会随完整 P0 验证执行 | 后续发布前继续执行 `npm run verify:p0`，不要绕过安全基线闸口 |

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
