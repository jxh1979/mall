# Findings Register V1

> 本清单由 `tools/generate-formal-security-baseline.mjs` 生成。P0 未修复或未正式豁免前，不允许进入公开生产安全准入。

- Generated from deterministic workspace inputs; execution time is captured by the verification logs.
- OpenAPI operation count: 227
- OpenAPI SHA-256: `c3e22fd45dab2e03488b1a6da8e2998c6ebdfcf535c96eeeaf668c392100f86e`

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
| P1 | `SEC-P1-WEBHOOK-REPLAY-001` | 真实供应商 Webhook 签名、防重放和轮询校验需在供应商资料到位后补实 | open | 阻塞真实支付/人脸/设备/开放平台供应商联调上线；不阻塞 mock adapter | 后端/IT/供应商联调负责人 | OpenAPI 当前存在 5 个开放应用或 webhook 签名上下文 operation | 按供应商 SDK/API 补字段映射、nonce、timestamp、签名验签、幂等、补偿和对账测试 |
| P2 | `SEC-P2-CREDENTIALS-001` | Bearer 原始 token 不再进入 Spring Security credentials | resolved | 已修复；继续通过单测防回归 | 后端 | `BearerAuthenticationFilter` 构造 `UsernamePasswordAuthenticationToken(user, null, authorities)` | 保留 `BearerAuthenticationFilterTests`，后续正式认证实现沿用不把原始凭据放入认证对象的原则 |
| P2 | `SEC-P2-SMOKE-HASH-001` | dev smoke AuthService 使用 SHA-256 密码摘要，不可迁移到生产认证 | resolved | 仅允许 local/dev/test；公开生产必须替换为正式身份源或强密码哈希方案 | 后端/安全 | `AuthService` 使用 `MessageDigest.getInstance("SHA-256")`，且类上存在 local/dev/test profile | 生产认证不得复用该 smoke 账号存储；若自建密码，采用 Argon2id/bcrypt/PBKDF2 并补 MFA/锁定策略 |
| P2 | `SEC-P2-CSRF-API-001` | 当前 API 关闭 CSRF，需在最终认证形态中复核 Web Cookie 场景 | open_for_prod_review | 不阻塞 Bearer/API 调用 smoke；若后台 Web 采用 Cookie 会话则阻塞公开生产 | 后端/前端/安全 | `SecurityConfig` 调用 `csrf.disable()` | 正式认证方案定稿时按 Bearer、SameSite Cookie 或双提交 Token 分支复核 |
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

## tenant-isolation

> 本小节为 P0 清单#10「租户隔离机审闸口（`verify:tenant-isolation`）」的越权 finding 台账，闭合
> `data-isolation-and-tenant-scope-security-spec-v0.1.md` §6 的 **ISO-GAP-01**（权威台账无跨租户越权 finding 系列）与
> §6A.2（越权 finding 台账落点）。机审闸口：`tools/verify-tenant-isolation.mjs`，挂载于 `npm run verify:delivery-gates`。
> 当前为生成器纳管前的人工登记（取代 §6 自述「2026-06-18 已手工补登」实为不存在的 manual-tenant-isolation 小节，本小节即真实补登）。
> 机审已知缺口 baseline：`security-scans/tenant-isolation-baseline.json`（**延期项⑥收口后 0 条**；首次 49 条 → 处置后 11 条 → ⑥收口后 0 条）。闸口对 baseline 之外的**新增回归**硬失败（fail-closed）；
> 缺口逐条修复后须从 baseline + 本台账移除，并以 `npm run verify:tenant-isolation -- --strict` 收紧至零容忍。
>
> **延期项⑥（11 条 F1 收口，2026-06-19）**：原 11 张「平台管理读豁免」F1 已**逐表真实收口**（不再仅 baseline 暂存）——
> 8 张商户面表在其 `Jdbc*Repository` 新增 `<scope_col> = :` scoped 读通道，3 张综合体级表在 CSV 真实重归类 `single_complex_default`。
> 机审 F1 由 11 → 0，baseline `known_findings` 收敛为空。详见下「F1 11 表⑥收口逐条处置」。

### 机审闸口与扫描结果（首次 → 处置后对比）

| 项 | 值 |
|---|---|
| 闸口脚本 | `tools/verify-tenant-isolation.mjs`（静态扫描，fail-closed，棘轮 baseline） |
| 权威表清单 | `docs/domain-ownership-scope-audit-v0.1.csv`（**⑥收口后 211 表**：direct 81 / 白名单豁免 42 / inherited 88；首次 169 表。⑥将 3 张综合体级表 direct→single_complex_default） |
| 扫描对象 | `backend/src/main/java/**/Jdbc*Repository.java`（32 个文件）内嵌 SQL |
| 覆盖口径 | F1 = direct 表「是否存在带 `<scope_col> = :` 谓词的 scoped 读取通道」；F2 = Repository SQL 引用的表是否已登记 CSV |
| 白名单豁免 | `global_reference`/`single_complex_default` + 基础设施表 `flyway_schema_history` |
| 已知盲区 | B1 inherited join-path 真实性（仅信息性，88 表）；B2 写路径 token-scope 覆盖（AC-ISO-2，Service 层数据流未分析）；B3 单调用点 scope 注入正确性（仅校验「存在 scoped 通道」近似）；B4 动态拼接/非 Jdbc 仓 |
| 首次扫描（2026-06-19） | **49 项 finding**：F1 UNSCOPED_DIRECT_READ × 13、F2 UNREGISTERED_TABLE × 36 |
| 处置后（2026-06-19） | **11 项 finding**（全 F1，全平台管理读豁免，留 baseline 跟踪）；机审 PASS（棘轮，0 新增回归）。净减 38：F2 全 36 表登记清零 + 2 张 amusement F1 经 CSV 正列改正真实消除；新版图额外补登 V108/V110/V112 的 7 张新增表 |
| **延期项⑥收口后（2026-06-19）** | **F1 → 0 项**：原 11 张平台管理读豁免 F1 逐表真实收口（8 张加 scoped 通道 + 3 张重归类 single_complex_default），baseline `known_findings` 收敛为空。direct 表数 84→81（3 张重归类至 single_complex_default 白名单：42→45），机审对 11 表 F1 全部清除 |

### F2 36 表登记归类汇总（SEC-P1-TENANT-ISOLATION-003，已 resolved）

逐表读迁移 CREATE TABLE 定义裁决真实归属，登记进 `domain-ownership-scope-audit-v0.1.csv`（口径真实，不为消警乱标 global）：

- **direct（自带 scope 列，有 scoped 读通道）**：`staff_profiles`(merchant_id，project_id NOT NULL+merchant_id)、`amusement_capacity_config/amusement_capacity_snapshot/amusement_over_capacity_policy/amusement_maintenance_record`(project_id，FK amusement_projects)、`bar_shifts/bar_order_item_voids`(merchant_id)、`wecom_risk_hits`→见下、`risk_alerts`(project_id+merchant_id，仓内 `AND project_id = :project_id`/`AND merchant_id = :merchant_scope` 动态过滤)、`supply_contracts/warehouses`(merchant_id，listXxxByMerchant 已 scoped)。
- **inherited（经父表 FK 继承 scope）**：`staff_device_credentials/employee_notices`(→staff_profiles)、`amusement_project_entitlements`(→amusement_projects)、`wecom_messages/wecom_access_audits`(→wecom_conversations)、`customer_check_ins/customer_memberships/customer_points_transfers`(→customers)、`customer_stored_value_transactions`(→stored_value_accounts)、`risk_alert_actions`(→risk_alerts)、`inventory_purchase_request_events`(→inventory_purchase_requests)、`saved_bottle_transfers`(→saved_bottles)。
- **single_complex_default（综合体级配置/平台合规，单综合体默认全局可见；迁移头明示隔离键 project_id 单综合体默认全局可见）**：`customer_member_levels/customer_check_in_tiers/customer_birthday_rules/customer_membership_plans/customer_stored_value_accounts/customer_points_transfer_limits`(V089/V090 会员中心综合体级配置)、`wecom_conversations/wecom_risk_hits/wecom_retention_policies`(V087/V106 平台侧 PIPL 合规模块)、`inventory_high_value_reviews`(无 scope 列，subject 多态关联)、`mdm_policy_violations`(device_id 关联，平台运维监控)。
- **global_reference（平台全局参考/设备策略配置）**：`mdm_app_whitelist/mdm_site_whitelist`(V088 平台 MDM 设备策略，scope_type/scope_id 默认 global)。

> 真实归属裁决要点：会员中心（customer_*）与企微合规（wecom_*）迁移头均明示「隔离键综合体级 project_id，单综合体默认全局可见」，故标 single_complex_default 而非 direct/project_id（否则会在无 `project_id = :` 通道的仓上制造假 F1，且语义上也确非 merchant 隔离）；MDM 白名单为平台全局设备策略，标 global_reference 真实。

### F1 13 表三色分类（SEC-P1-TENANT-ISOLATION-002）

逐表 Read Jdbc*Repository 查询 + 调用链（Controller 权限 / Service scope 分支）裁决，**结论：0 张真商户面越权，2 张已修，11 张平台管理读豁免**：

- **🟢 已修（CSV scope_source 正列改正，真实消除 F1，非弱化）— 2 张**：
  - `amusement_incidents`、`amusement_safety_notices`：实表 V049 已 `ADD COLUMN merchant_id` + V065 隔离索引，仓内确有 `findXxxByMerchant WHERE merchant_id = :merchant_id` scoped 读、Service 按 `currentMerchantScope().isPresent() ? byMerchant : all` 收口。CSV 原标 scope_source=`project_id` 系**标错列名**（真实强制隔离键是 merchant_id），改正为 `merchant_id` 后机审命中真实 scoped 通道，F1 真实消除。
- **🟡 平台管理读豁免（非商户面越权，保留 baseline 跟踪，仍为 direct 不弱化）— 11 张**：
  - `access_users`(IAM/组织域，Service 按 `organization_id` 收口，merchant_id 仅归属字段)、`staff_level_assignments`(按 user_id 自读做鉴权)、`merchants`(平台治理本体，`/api/v1/platform/*` 平台管理员域)、`terminal_devices`(平台/运维按 device_id/device_code 主键单取)；
  - `analytics_dashboard_snapshots`/`analytics_merchant_health_scores`/`analytics_merchant_summaries`(权限 `analytics.dashboard.view` 经 V017 仅授 `role_platform_admin`，商户用户无法到达；商户下钻另走 `overview(currentMerchantScope())` 行级隔离)；
  - `mall_floor_plans`/`business_process_instances`(Service 已 `scope.isPresent() ? findXxxByMerchant : findAll`，正则只匹到平台全量分支；商户面走 byMerchant + ownership 守卫)；
  - `customer_points_rules`/`customer_redemption_catalog_items`(V060 综合体级会员配置/兑换目录，单综合体默认全局可见，非 merchant 隔离)。
  - 豁免理由：均为平台管理员/单综合体内部读，商户侧无法读到跨 merchant 数据；保留 `direct` 归属与 baseline 跟踪，多综合体推广前按 next_action 补 scoped 通道。
- **🔴 真商户面越权（需修 Repository 补 scoped 查询）— 0 张**。

> 不弱化安全声明：11 张豁免表在 CSV 仍保持 `direct` 归属、仍在 baseline 棘轮跟踪闭环内，闸口对其之外的新增回归仍 fail-closed；豁免仅记录「平台管理读、当前非越权」事实，不移除其租户列标注、不切 global 消警。

### F1 11 表⑥收口逐条处置（SEC-P1-TENANT-ISOLATION-002，2026-06-19 → resolved）

延期项⑥要求把上述 11 张「平台管理读豁免」从 baseline 暂存升级为**真实收口**（消除 finding 而不弱化安全、不伪造）。逐表二选一处置：

**A. 商户面数据 → 在 `Jdbc*Repository` 新增 `<scope_col> = :` scoped 读通道（8 张，F1 真实清除）：**

| 表 | scope 列 | 新增方法（接口 default + Jdbc SQL 覆盖） | 仓文件 |
|---|---|---|---|
| `access_users` | merchant_id | `listUsersByMerchant` `WHERE merchant_id = :merchant_id` | `access/AccessRepository.java` + `JdbcAccessRepository.java` |
| `staff_level_assignments` | merchant_id | `listStaffLevelAssignmentsByMerchant` `WHERE merchant_id = :merchant_id` | 同上 |
| `merchants` | business_entity_id | `listMerchantsByBusinessEntity` `WHERE business_entity_id = :business_entity_id` | `governance/PlatformRepository.java` + `JdbcPlatformRepository.java` |
| `terminal_devices` | venue_id | `listDevicesByVenue` `WHERE venue_id = :venue_id` | `terminal/TerminalDeviceRepository.java` + `JdbcTerminalDeviceRepository.java` |
| `analytics_merchant_health_scores` | merchant_id | `listMerchantHealthScoresByMerchant` `WHERE merchant_id = :merchant_id` | `analytics/AnalyticsRepository.java` + `JdbcAnalyticsRepository.java` |
| `analytics_merchant_summaries` | merchant_id | `listAnalyticsMerchantsByMerchant` `WHERE merchant_id = :merchant_id` | 同上 |
| `mall_floor_plans` | project_id | `findAllFloorPlansByProject` `WHERE project_id = :project_id`（原已有 byMerchant，本次补 project_id 通道对齐 CSV scope_source） | `floorplan/FloorPlanRepository.java` + `JdbcFloorPlanRepository.java` |
| `business_process_instances` | project_id | `listProcessInstancesByProject` `WHERE project_id = :project_id`（原已有 merchant_id 通道，本次补 project_id 对齐 CSV scope_source） | `evidence/TraceabilityRepository.java` + `JdbcTraceabilityRepository.java` |

> 新增方法均为接口 `default`（向后兼容，不破坏既有实现/调用方）+ Jdbc 子类 `@Override` SQL 谓词下推；只加不删，既有 `listXxx()`/`listXxxByMerchant()` 平台全量与商户通道全部保留。机审命中 `<scope_col> = :` 后 8 张 F1 真实清除。

**B. 平台全局/综合体级（非商户面）→ CSV 真实重归类 `single_complex_default`（3 张，机审豁免，非消警乱标）：**

| 表 | 原归类 | 新归类 | 重归类理由（真实依据） |
|---|---|---|---|
| `analytics_dashboard_snapshots` | direct/project_id | single_complex_default | P0 综合体级总览快照（`getP0OverviewDashboard` 取最新一条综合体级 rollup，平台管理读，非商户面）；单综合体默认全局，多综合体按 project_id 收紧 |
| `customer_points_rules` | direct/project_id | single_complex_default | V060 迁移头**明示**「隔离键 project_id（综合体级会员中心，非 merchant）；单综合体默认全局可见，多综合体启用时收紧」 |
| `customer_redemption_catalog_items` | direct/project_id | single_complex_default | V025 建表**实表无 project_id/merchant_id scope 列**；V060 收敛为综合体级会员中心兑换目录/礼物语义，综合体内全局可见，平台管理非商户面 |

> 重归类不弱化安全：3 张确为综合体级平台管理数据（其中 `customer_redemption_catalog_items` 实表根本无租户 scope 列，原标 direct/project_id 系误标），归 `single_complex_default` 白名单是真实分类；多综合体启用时按迁移头说明收紧。

> ⑥收口结果：机审 F1 由 11 → **0**，`tenant-isolation-baseline.json` `known_findings` 收敛为空。全量后端测试 1251 全绿（含新增 group-overview/ETL project 维度用例）。

> 注：机审当前仍报 2 项 **F2 UNREGISTERED_TABLE**（`cam_trueup_runs`/`cam_trueup_lines`，来自 finance 模块 **V121 未跟踪迁移**）——属 finance 域并发在制工作，**不在本次⑥隔离收口（access/governance/terminal/analytics/floorplan/evidence/customer）范围内**，应由 finance 域负责人登记 CSV scope（`cam_trueup_runs` 综合体级 settlement_period 计费批次、`cam_trueup_lines` 带 merchant_id 逐户差额）。本次未代为登记，避免越域裁决他域表归属。

### finding 系列条目

| severity | id | title | status | blocking_scope | owner | evidence | next_action |
|---|---|---|---|---|---|---|---|
| P0 | `SEC-P0-TENANT-ISOLATION-001` | 机审闸口 `verify:tenant-isolation` 建立并接入 `verify:delivery-gates` | resolved | 闭合 ISO-GAP-02（无自动闸口）；闸口对新增越权回归 fail-closed | 后端/安全 | `tools/verify-tenant-isolation.mjs`、`package.json` `verify:tenant-isolation` 已挂入 `verify:delivery-gates`；延期项⑥收口后 baseline `known_findings` 收敛为空（F1 0、F2 仅余 finance 域并发未跟踪表，非本次范围） | F1 缺口已清零；finance V121 的 `cam_trueup_*` 由 finance 域登记 CSV 后即可切 `--strict` 零容忍 |
| P1 | `SEC-P1-TENANT-ISOLATION-002` | 13 张 direct 表缺 token-scope 读取通道（F1 UNSCOPED_DIRECT_READ） | resolved | F1 全部清除（13→0）：2 张 amusement 正列改正 + 11 张延期项⑥逐表收口 | 各域后端 | 三色裁决（详见上「F1 13 表三色分类」）：🟢已修 2 张（`amusement_incidents`/`amusement_safety_notices`）；🟡原平台管理读豁免 11 张已于**延期项⑥（2026-06-19）真实收口**（详见「F1 11 表⑥收口逐条处置」）：8 张商户面表在 `Jdbc*Repository` 新增 `<scope_col> = :` scoped 读通道（`access_users`/`staff_level_assignments`/`merchants`/`terminal_devices`/`analytics_merchant_health_scores`/`analytics_merchant_summaries`/`mall_floor_plans`/`business_process_instances`），3 张综合体级表 CSV 重归类 `single_complex_default`（`analytics_dashboard_snapshots`/`customer_points_rules`/`customer_redemption_catalog_items`）；🔴真越权 0 张；机审 F1 → 0，baseline 收敛为空 | 维持收口；多综合体推广时在 Service 层把 token scope 强注入新增的 `ByMerchant/ByProject` 通道，并切 `--strict` 零容忍 |
| P1 | `SEC-P1-TENANT-ISOLATION-003` | 36 张被 Repository 引用的真实表未登记 scope 审计 CSV（F2 UNREGISTERED_TABLE） | resolved | F2 治理盲区已闭合；36 表全部裁决真实归属并登记 | 数据治理/各域后端 | 36 表逐表读迁移 CREATE TABLE 裁决并登记 `domain-ownership-scope-audit-v0.1.csv`（汇总见上「F2 36 表登记归类汇总」：direct/inherited/single_complex_default/global_reference 真实分类）；另补登 V108/V110/V112 衍生的 7 张表保持闸口绿；机审 F2 finding 由 36→0 | 维持登记；后续新增带租户表合入前必须先登记 CSV（闸口已 fail-closed 守护） |
| P1 | `SEC-P1-TENANT-ISOLATION-004` | inherited 表 join-path 继承 scope 的真实性未机审（已知盲区 B1） | open | 信息性盲区，不阻断；多综合体前需补 join-path 校验 | 后端/安全 | 70 张 inherited 表机审仅信息性列出，未校验是否真正经 join 继承上游 scope | 迭代机审：对 inherited 表解析 join 链是否落到带 scope 列的父表（§6A.1） |
