# 对抗式审计（第三轮 2026-07-19）Findings 登记册 V0.1

> 来源审计：`~/audit-20260719/complex-management-adversarial-audit-round3-20260719.md`（`head=b80d306`）。
> 本登记册跟踪身份/授权域三项（AUD-010/011/020）的修复状态；资金/状态机域（AUD-012/013/018/019）与可靠性/证据域
> （AUD-014~017/021~023）由其他任务分别处置，不在本册结论内。修复分支：`fix/aud-010-011-020-identity-authz`。

## 状态总表

| ID | 级别 | 结论 | 状态 | 修复位置 |
|---|---|---|---|---|
| AUD-010 | P1 | 终端/设备身份由请求头自报、可伪造或缺省绕过 | **FIXED-partial**（设备绑定型名义向量闭合 + RBAC 兜底外部越权；**web/app 资金白名单旁路 STILL-OPEN**；enforce 分级生产滚动中） | `gate/DeviceCredentialVerifier`+`DeviceCredentialInterceptor`+`TerminalIdentityHolder`+`GateRequestContext`+`ServiceOrderController`+`application.yml` |
| AUD-011 | P1 | 权限模板应用绕过高风险授权审批 | **CLOSED** | `access/AccessService.applyPermissionTemplate` |
| AUD-020 | P1 | 高值库存复核子服务手工 new，绕过 AOP 守卫 | **CLOSED** | `inventory/InventoryService.reviewHighValue`（守卫上移被代理入口）+ `GuardedActionProxyBypassArchTest` |

## AUD-010 详情（FIXED-partial，勿读成已闭）

**机制（已落地）**：设备绑定型终端类型（staff_work_phone/tablet/kiosk）的写请求，要求对 `deviceId|timestamp|nonce|METHOD|path`
的密码学签名（复用员工 Android Keystore 设备凭证公钥）+ 核对 `terminal_devices` 登记；通过则以设备登记类型为服务端可信终端身份，
注入 `TerminalIdentityHolder`，终端开关/入场闸（`GateRequestContext`）与资金四路统一改读派生身份。分级强制
（`off|grace|enforce`，默认 `grace`=生产安全态：签名无效必拒、缺签名/未登记设备的写请求记审计后放行）。

**为何是 FIXED-partial（不是完整闭合）**：
- **名义向量闭合**：把某设备绑定型终端类型翻 `enforce` 后，冒充「该类型公司硬件」需有效签名——自报该类型的伪造被挡；
  外部人另被 RBAC 兜死（动钱需 `service.order.create` 权限，客人拿不到），故**非外部盗款通道**。
- **STILL-OPEN 残留（见「R1」）**：验签只对设备绑定型强制，但资金四路白名单**也接受 `web`/`app`**，自报 `web`/`app` 时**完全跳过验签**。
  故即便 `staff_work_phone` 翻 `enforce`，攻击者切自报 `web`/`app` 仍能动钱，绕过「资金动作仅限公司登记硬件」这一设备绑定意图。
  残留=**内部持权者可从任意私人设备经 web/app 动钱**；且**本 PR 前即存在**（非本次引入）。

### R1 web/app 资金白名单旁路（STILL-OPEN，独立挂号）
`ServiceOrderController` 资金四路（下单/确认收款/完成/出示收款码）的公司终端白名单含 `web`/`app`，这两类**非设备绑定型**、
不进设备凭证验签、也未与认证通道耦合，故「仅限公司登记硬件」保证对 web/app **不成立**。**翻任何终端类型 `enforce` 前必须同批处置**，三选一：
1. **web 与 Cookie 会话真相耦合**：`web` 只认后台 HttpOnly Cookie 会话来源，拒绝 `Authorization: Bearer` 自报 `web`（堵个人设备用 Bearer 冒充 web 后台）；
2. **app 资金动作要客人侧证明**：`app`（客人自点）走客人身份/订单归属证明，不等同「公司终端」放行；
3. **把 `web`/`app` 移出资金白名单**，或一并纳入验签（需各自登记/证明基建）。

### enforce 翻转前的加固项（复核提，翻 enforce 同批处置）
- **重放窗**：nonce 不落库，有 ±300s 时间窗；资金写由既有 idempotency_key 覆盖，但**非资金写**在窗口内可重放（未被幂等键覆盖）。
- **签名量未含 body 摘要**：签名绑定 `method+path`，未绑定请求体——截获有效签名后可改体（enforce 前用 `ContentCachingRequestWrapper` 追加 body 摘要）。
- **resolveSigningKey 未交叉校验归属**：签名公钥由「认证员工→档案→active 凭证」解析，**未**显式交叉校验 header `X-Device-Id` 与该登记密钥/员工归属一致（当前靠 `terminal_devices` 类型+bound 核对，宜再显式绑 deviceId↔凭证归属）。
- **kiosk/tablet 无每设备密钥基建**：翻 `enforce` 前须先落每设备签名密钥登记（否则该类型恒 403 自伤）；`kiosk` 可复用设备令牌走 HMAC 通道，本期未纳入。

**验证**：`DeviceCredentialVerifierTests`（真实 EC 签/验，11 例）；`DeviceCredentialEnforcementJdbcIntegrationTests`（真实 bean 装配 + 配置驱动分级强制端到端，4 例，随全量 22 JDBC 绿）。

## AUD-011 详情（CLOSED）

`applyPermissionTemplate` 复用唯一授权政策引擎 `requireGrantApprovalIfNeeded`——模板含任一 `requires_approval` 权限时批量应用
一律 `GRANT_REQUIRES_APPROVAL`(403) fail-closed，与角色/岗位/临时授权同口径；高风险须走 `applyGrant→reviewGrant` 主管复核流。
验证：`AccessServiceTests.applyPermissionTemplateRequiresApprovalWhenTemplateHasHighRiskPermission`。

## AUD-020 详情（CLOSED）

`InventoryHighValueReviewService` 由域内手工 `new`（非 Spring 代理），其 `review()` 上的 `@GuardedAction` 是死注解。能力闸上移到
被 Spring 代理的对外入口 `InventoryService.reviewHighValue()`，Controller 调用即命中 `ActionGateAspect`（能力开关关→
`TERMINAL_DISABLED`、无库存变化）。新增架构测试 `GuardedActionProxyBypassArchTest` 阻止「主源码 `new` 一个带 `@GuardedAction`
的类」再次绕过代理（负控已验证：重现 bug 即失败并点名两处 `new`）。验证：`FiveDomainGuardedActionCoverageTests`（+reviewHighValue）
+ `GuardedActionProxyBypassArchTest`。
