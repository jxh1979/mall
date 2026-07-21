# 对抗式审计（第三轮 2026-07-19）Findings 登记册 V0.1

> 来源审计：`~/audit-20260719/complex-management-adversarial-audit-round3-20260719.md`（`head=b80d306`）。
> 本登记册跟踪身份/授权域三项（AUD-010/011/020）的修复状态；资金/状态机域（AUD-012/013/018/019）与可靠性/证据域
> （AUD-014~017/021~023）由其他任务分别处置，不在本册结论内。修复分支：`fix/aud-010-011-020-identity-authz`。

## 状态总表

| ID | 级别 | 结论 | 状态 | 修复位置 |
|---|---|---|---|---|
| AUD-010 | P1 | 终端/设备身份由请求头自报、可伪造或缺省绕过 | **FIXED（服务端完整；enforce 待客户端签名就绪；app enforce 属业务口径）** | `gate/DeviceCredentialVerifier`+`DeviceCredentialInterceptor`+`DeviceCredentialBodyCachingFilter`+`NonceStore`+`terminal/DeviceSigningKeyStore`+`TerminalIdentityHolder`+`GateRequestContext`+`ServiceOrderController`+V325 |
| AUD-011 | P1 | 权限模板应用绕过高风险授权审批 | **CLOSED** | `access/AccessService.applyPermissionTemplate` |
| AUD-020 | P1 | 高值库存复核子服务手工 new，绕过 AOP 守卫 | **CLOSED** | `inventory/InventoryService.reviewHighValue`（守卫上移被代理入口）+ `GuardedActionProxyBypassArchTest` |

## AUD-010 详情（FIXED · 服务端完整，enforce 待客户端就绪）

**服务端前置全套已落地（2026-07-20 用户拍板本批做完）**：
- **设备绑定型验签**（staff_work_phone/tablet/kiosk 写请求）：签名量 = `deviceId|ts|nonce|METHOD|path|sha256(body)`；
  staff_work_phone→员工 Keystore 公钥且 header deviceId 归属该员工（⑤）；kiosk/tablet→设备级签名公钥
  `terminal_device_signing_keys`（②，登记端点 `POST /api/v1/terminals/devices/{id}/signing-key`）。
- **① web/app 资金旁路已闭（服务端）**：`web` enforce 下拒「`Authorization: Bearer` 自报 web」（合法后台走 Cookie 会话，
  **无客户端依赖、可即刻翻 enforce**）；`app` enforce 下拒 app 写。
- **③ 重放**：nonce 落库 `terminal_device_nonces`（V325），验签通过后消费，重复即 `DEVICE_CREDENTIAL_REPLAY`。
- **④ 防改体**：body SHA-256 纳入签名量（`DeviceCredentialBodyCachingFilter` 仅包裹「设备绑定型+写+带签名」请求）。
- **⑤ deviceId 归属**：员工只能用自己密钥配自己设备的 deviceId，否则 `device_owner_mismatch` 拒。
- kiosk 自助端点（config/heartbeat，有 X-Device-Token 认证）豁免本闸，不被 enforce 误伤。

**为何标 FIXED 而非 CLOSED**：分级强制默认 `grace`（生产安全态，不瘫痪现网）。**实际翻 enforce 依赖客户端 App
（安卓/kiosk/平板）实现请求签名 + 登记签名密钥——客户端侧工作，本 PR 不含**。故服务端 ready-to-enforce，真正 flip
是客户端就绪后的部署决策（web 例外：无客户端依赖，可先翻）。

### ⚠️ 需用户定：app 资金动作业务口径
`app`（客人 App 自点，`CREATE_AND_CODE_TERMINALS` 保留项）的 createOrder 现被 RBAC（`service.order.create`）挡住客人，
故无活跃客人自点流程；残留=内部持权者可自报 app 从私人设备动钱。本 PR 处理：**app enforce 下拒写**（把决定推到 enforce
翻转时）。翻 app enforce 会阻断「客人 App 自点」——若业务要保留客人自点，须先建**客人侧证明**（客人身份/订单归属），
属业务口径，**请用户拍板**：①翻 app enforce（客人自点作废，走别的客人认证流）② 建客人侧证明后再翻 ③ app 移出资金白名单。

**验证**：`DeviceCredentialVerifierTests`（真实 EC 签/验，18 例，含 web-Bearer/app/deviceId-mismatch/kiosk-device-key/
body-tamper·valid）；`NonceAndSigningKeyJdbcIntegrationTests`（nonce 重放 + 设备密钥库，真实 JDBC）；
`DeviceCredentialEnforcementJdbcIntegrationTests`（配置驱动分级强制）。全套连同 AccessJdbc/ServiceOrderJdbc
在隔离库 complex_test_aud010 上 24 JDBC 全绿（上下文 boot + 无回归）。

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
