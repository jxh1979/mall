# 正式安全基线 V1

> 本报告由 `tools/generate-formal-security-baseline.mjs` 生成，覆盖当前 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 首批 P0 编码状态。

- Generated from deterministic workspace inputs; execution time is captured by the verification logs.
- OpenAPI operation count: 736
- OpenAPI SHA-256: `8335eb3e2e19e3cf425a72300ba905eff528deed1acd4ad04e8508d172070de0`
- Java version: `21`
- Spring Boot version: `4.1.0`
- springdoc-openapi version: `3.0.3`
- Maven dependency count: 16
- Verification command: `npm run verify:security-baseline`

## scope-and-inputs

| 输入 | 状态 |
|---|---|
| OpenAPI 静态契约 | 已读取 `docs/openapi/openapi-p0.json` |
| 安全基线生成任务 | 已覆盖 10 类输出物 |
| 生产前置台账 | 已读取 MDM、NAS、密钥轮换、安全基线等状态 |
| Spring Security 配置 | 已读取 `SecurityConfig` |
| dev smoke 鉴权实现 | 已读取 `AuthService` 与 `BearerAuthenticationFilter` |
| 数据隔离与租户 scope | 已读取 `data-isolation-and-tenant-scope-security-spec-v0.1.md`、scope CSV、`verify-tenant-isolation.mjs` 和 tenant-isolation baseline |

## threat-model-v1

| 资产/边界 | 当前结论 | 主要风险 | 当前缓解 | 后续动作 |
|---|---|---|---|---|
| 客户、员工、商户、会员身份 | P0 契约已区分 staff-token、customer-token、device-credential、open-app-credential、webhook-signature | 生产认证 Provider 未落地 | dev smoke 限定 local/dev/test profile | 正式身份源、Token 生命周期、设备凭证、开放应用签名补实 |
| 票务、权益、核销、酒水、存酒、卡座 | OpenAPI、权限矩阵和事件矩阵已声明闭环 | 重复核销、越权核销、卡座人数并发超额 | operation 级幂等和审计事件已进入契约 | 后续业务实现中必须用数据库约束和事务固化 |
| 支付、退款、应收应付、对账 | 聚合支付优先、保留直连抽象 | 回调重放、退款冲正缺审计、日结后改账 | OpenAPI 声明幂等和审计事件 | 供应商资料到位后补验签、对账文件、补偿策略 |
| Android 工作手机、平板、一体机 | 端侧边界已在文档和 OpenAPI 表达 | 未纳管设备生产使用 | 生产前置台账阻断 MDM pending_info | Android Enterprise/商业 MDM enroll 后才可生产使用 |
| 开放平台与第三方 | 双向开放与 adapter 边界已定义 | 签名、防重放、字段映射供应商差异 | mock adapter 和 OpenAPI 先行 | 真实供应商 SDK/API 到位后补字段和异常码 |
| 多租户/商户数据隔离 | scope 注入只来自安全 token；scope CSV、数据隔离规格和 `verify:tenant-isolation` 已形成机审链路 | Repository 漏 scope、请求体伪造 merchant_id、跨项目读写越权 | `verify:tenant-isolation` + 自动数据隔离基线 + SEC-P0-TENANT-ISOLATION-001 finding | 新增带租户数据的表必须同步 CSV、迁移、Repository scoped 通道和越权测试 |
| 160 运维、NAS、密钥 | 已有 runbook、台账模板和恢复演练脚本 | NAS 服务器已明确为 `192.168.88.113`，真实 NAS 挂载、同步备份和从 NAS 恢复演练 `restore.drill_completed` 已通过；生产凭证台账结构已可验证，真实凭证发放/轮换证据仍需补齐 | 当前不在报告中记录明文密钥 | 固化自动挂载、定时备份、容量告警、到期提醒和轮换证据归档 |

## authz-review-v1

| auth_context | operation_count |
|---|---:|
| `customer-token` | 39 |
| `device-credential` | 4 |
| `open-app-credential` | 1 |
| `public` | 7 |
| `staff-token` | 681 |
| `webhook-signature` | 4 |

| 检查项 | 结论 |
|---|---|
| 认证上下文 | 已按 OpenAPI 统计；员工、客户、设备、开放应用和 webhook 不混用员工权限 |
| 权限码 | `verify:p0-contract` 已要求每个 operation 具备 `x-permission` |
| 数据范围 | `verify:p0-contract` 已要求 operation 具备数据范围和终端/调用通道 |
| 高风险审批 | 权限矩阵声明高风险权限审批、显式拒绝、临时授权和模板机制 |
| 当前缺口 | 正式生产认证 Provider、Token 过期/刷新/吊销、失败锁定和限速仍为生产前阻断 |

## openapi-exposure-review-v1

| 指标 | 数量 |
|---|---:|
| 全部 operation | 736 |
| 写 operation | 442 |
| public operation | 7 |
| open-app/webhook operation | 5 |

| terminal_or_channel | operation_count |
|---|---:|
| `app` | 50 |
| `kiosk` | 44 |
| `merchant_app` | 2 |
| `merchant_portal` | 22 |
| `miniapp` | 50 |
| `mobile_admin` | 64 |
| `provider-webhook` | 3 |
| `server-to-server` | 2 |
| `staff_private_phone` | 39 |
| `staff_work_phone` | 80 |
| `tablet` | 102 |
| `web` | 699 |

当前 SecurityConfig 允许 `/docs/**`、`/swagger-ui/**`、`/v3/api-docs/**` 公开访问。该配置可用于内网开发和联调，但公网公开生产必须关闭或加认证保护。

## webhook-signature-review-v1

| 检查项 | 当前状态 | 结论 |
|---|---|---|
| 签名上下文 | OpenAPI 使用 `webhook-signature` 与 `open-app-credential` 区分外部调用 | 边界正确 |
| 幂等 | 写接口由 `verify:p0-contract` 强制声明 `x-idempotency-required` | 契约已具备验收基础 |
| 防重放 | 供应商 nonce、timestamp、签名算法未到位 | 真实供应商联调前必须补实 |
| 轮询校验 | 文档采用 Webhook + 定时轮询校验 | 策略正确，待按供应商字段落地 |

## payment-risk-review-v1

| 检查项 | 当前状态 | 结论 |
|---|---|---|
| 支付通道 | 聚合支付优先，保留直连接口抽象 | 选型口径已收口 |
| 回调 | OpenAPI/事件矩阵已要求回调、轮询、幂等和补偿 | 待供应商资料 |
| 退款 | 权限矩阵要求退款发起/审批分离 | 后续实现必须不可覆盖原单，只做冲正 |
| 对账 | 财务与开放平台文档要求对账/补偿 | 真实对账文件样例到位后补测试 |

## pii-retention-review-v1

| 数据类型 | 当前策略 | 结论 |
|---|---|---|
| 法定实名/年龄核验 | 只在法定必要范围内强制实名；酒吧年龄/人脸核验走第三方接口 | 供应商合规材料和最小化字段待补 |
| 人脸数据 | 默认只保存最小必要结果和第三方流水，不默认保存原始人脸图像 | 符合最小化方向 |
| 敏感证据 | 查看和导出需最小授权、二次审批和用途留痕 | 事件矩阵已定义 |
| 日志 | 不得记录密码、原始 Token、生产密钥和完整敏感证据 | 本报告验证器会拦截明显明文密钥 |

## device-mdm-review-v1

| 检查项 | 当前状态 | 阻断结论 |
|---|---|---|
| Android Enterprise/商业 MDM | PRD-MDM-001 仍为 pending_info | 阻塞生产高风险终端动作 |
| 设备绑定 | 权限矩阵已声明设备绑定/解绑和 MDM 策略下发权限 | 开发可继续，生产需证据 |
| 离线边界 | 高价值酒水、支付、核销保持在线 | 正确 |
| 一体机 | 售票型和非售票型能力分离 | 正确，需设备能力开关测试 |

## ops-160-review-v1

| 检查项 | 当前状态 | 阻断结论 |
|---|---|---|
| 160 运维脚本 | P0 验证脚本已存在并可作为准入证据 | 需持续跑完整验证 |
| 160 后端测试执行位置 | Java/后端集成测试、BE-000 和完整 P0 验收默认在 `192.168.88.160` 或等效 Java 21 环境执行 | 本机缺 Java 21 只记录为本机环境前置缺失，不作为代码失败 |
| systemd/正式配置 | 文档要求迁移到系统配置和告警 | 公开生产前必须完成 |
| NAS 恢复演练 | PRD-NAS-002 仍需证据 | 阻塞生产可恢复结论 |
| 密钥轮换 | `verify:production-credentials` 已通过；明细台账覆盖生产账号、PostgreSQL、对象存储、JWT/Token、第三方/支付/企微/开放平台密钥、NAS SMB 凭证和备份加密预留 | 台账结构和 Runbook 不再阻塞；真实生产凭证发放仍需责任人、credential_ref、到期日和轮换证据 |
| 报告密钥安全 | 本报告不写真实密钥、数据库密码或对象存储密码 | 正确 |

## dependency-sbom-review-v1

| 依赖面 | 当前状态 | 后续动作 |
|---|---|---|
| Maven | `backend/pom.xml` 声明 16 个 dependency；Spring Boot `4.1.0` | 生成 CycloneDX 或等效 SBOM，并做漏洞处置/豁免 |
| npm/Web 工具链 | 根目录使用 lock 文件和 workspaces | 发布前执行 npm audit 或等效扫描 |
| Android/Gradle | 完整 P0 验证脚本覆盖 Android env/build/smoke/APK | 发布前补 Android 依赖 SBOM |

## findings-register-v1

完整发现清单见 `security-scans/findings-register-v1.md`。

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

## verification-and-gates

| Gate | 目的 | 命令 |
|---|---|---|
| 安全基线生成 | 生成 V1 报告和 findings register | `npm run docs:security-baseline` |
| 安全基线验证 | 检查章节、计数、发现状态、密钥泄漏 | `npm run verify:security-baseline` |
| P0 契约验证 | 检查 OpenAPI、追溯附录、客户端契约 | `npm run verify:p0-contract` |
| P0 全量验证 | 检查后端、Web、Android、APK | `/opt/complex/scripts/complex-verify-p0.sh` |
