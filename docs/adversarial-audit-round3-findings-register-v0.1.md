# 对抗式审计（第三轮 2026-07-19）Findings 登记册 V0.1

> 来源审计：`~/audit-20260719/complex-management-adversarial-audit-round3-20260719.md`（`head=b80d306`）。
> 本登记册跟踪身份/授权域三项（AUD-010/011/020）的修复状态；资金/状态机域（AUD-012/013/018/019）与可靠性/证据域
> （AUD-014~017/021~023）由其他任务分别处置，不在本册结论内。修复分支：`fix/aud-010-011-020-identity-authz`。

## 状态总表

| ID | 级别 | 结论 | 状态 | 修复位置 |
|---|---|---|---|---|
| AUD-010 | P1 | 终端/设备身份由请求头自报、可伪造或缺省绕过 | **FIXED_MECHANISM_COMPLETE_ENFORCEMENT_GRADUATED（生产滚动中）** | `gate/DeviceCredentialVerifier`+`DeviceCredentialInterceptor`+`TerminalIdentityHolder`+`GateRequestContext`+`ServiceOrderController`+`application.yml` |
| AUD-011 | P1 | 权限模板应用绕过高风险授权审批 | **CLOSED** | `access/AccessService.applyPermissionTemplate` |
| AUD-020 | P1 | 高值库存复核子服务手工 new，绕过 AOP 守卫 | **CLOSED** | `inventory/InventoryService.reviewHighValue`（守卫上移被代理入口）+ `GuardedActionProxyBypassArchTest` |

## AUD-010 详情（不简单标 CLOSED）

- **机制已完整落地**：设备绑定型终端类型（staff_work_phone/tablet/kiosk）的写请求，要求对 `deviceId|timestamp|nonce|METHOD|path`
  的密码学签名，以设备注册公钥（复用员工 Android Keystore 设备凭证）验签 + 核对 `terminal_devices` 登记；通过则以设备登记类型
  为服务端可信终端身份，注入 `TerminalIdentityHolder`，终端开关/入场闸（`GateRequestContext`）与资金四路统一改读派生身份。
- **为何不标 CLOSED**：采**分级强制**（`off|grace|enforce`，默认 `grace`=生产安全态），以免硬强制瘫痪尚未登记签名密钥的现网终端。
  **`grace` 下缺签名的写请求仍放行**——伪造向量在「未翻转 `enforce` 的终端类型」上**仍处灰度**。仅当某终端类型翻转 `enforce`
  后，该类型的向量才真正关闭。翻转是**部署决策（需用户拍板）**，前置=现网设备登记签名公钥 + 客户端实现请求签名。
- **残留/后续**：`tablet`/`kiosk` 暂无每设备非对称密钥基建，其 `enforce` 需先落设备密钥登记（`kiosk` 可复用设备令牌走 HMAC 通道，
  本期未纳入）；签名量当前绑定 method+path，未绑定请求体（enforce 前加固项，见 PRD）。
- **验证**：`DeviceCredentialVerifierTests`（真实 EC 签/验，9 例覆盖 有效签名派生/enforce 缺证 403/grace 放行/无效签名拒/
  过期/设备未绑定/类型矛盾/读请求不拒/非设备绑定型放行）；`DeviceCredentialEnforcementJdbcIntegrationTests`（真实 bean 装配 +
  配置驱动分级强制端到端）。

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
