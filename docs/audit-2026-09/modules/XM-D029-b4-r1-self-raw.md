CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-D029 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Opus 5.5（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-23T23:32:27Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。对象：XM-D029 B3-r1 修复 diff：before tree `ce088c52fb3031d1ae65380a2dd10d261cc3c1ca` → after tree `74bb605e734ca7111c6a88873923e3e044f2deeb`；冻结 diff `/home/james/audit-tmp/xmd029_b3r1.diff`（65541 bytes，SHA256 1fa24e3a4dcd8e8dc2cc700d4c7801f1c18a92382e8b29e925c5e6cc19ed8eb4；`git diff --binary` 两 tree 与冻结文件逐字节相同）。定稿 ID：XM-D029-001…008（findings「XM-D029 B2-r1 双 PASS 定稿」段；003 按「XM-D029 B2-r2 双 PASS 定稿」段丁案）。
- 顺序：Grok 已先派（grok-4.7，modules/XM-D029-b4-r1-grok-raw.md），本报告落盘前未读其正文。

## 1. 逐块归因（§5 闸 3）

| 文件 | 块 | 块头 | ± | 归因 ID | 首条改动行（截断） |
|---|---:|---|---|---|---|
| AuthorizationService.java | 1 | `@@ -157,7 +157,7` | +1/−1 | 001 | if (!isMasterAuthority(actor)) { |
| AuthorizationService.java | 2 | `@@ -418,12 +418,15` | +5/−2 | 001 | * Platform admins get Optional.empty() (no filter, sees everything; D- |
| AuthorizationService.java | 3 | `@@ -433,13 +436,16` | +5/−2 | 001 | * Unlike {@link #currentMerchantScope()}, this never throws: callers w |
| AuthorizationService.java | 4 | `@@ -498,11 +504,13` | +4/−2 | 001 | * 项目维度只读 scope：返回当前主体的项目 ID。没有项目归属则拒绝。 |
| JdbcAccessRepository.java | 1 | `@@ -72,7 +72,7` | +1/−1 | 001 | requireProjectScopeId(organizationId); |
| JdbcAccessRepository.java | 2 | `@@ -199,7 +199,7` | +1/−1 | 001 | requireProjectScopeId(organizationId); |
| JdbcAccessRepository.java | 3 | `@@ -211,7 +211,8` | +2/−1 | 001 | requireMerchantScopeId(merchantId); |
| JdbcAccessRepository.java | 4 | `@@ -306,7 +307,7` | +1/−1 | 001 | requireProjectScopeId(organizationId); |
| JdbcAccessRepository.java | 5 | `@@ -415,7 +416,14` | +8/−1 | 001 | requireProjectScopeId(organizationId); |
| JdbcAccessRepository.java | 6 | `@@ -1034,7 +1042,7` | +1/−1 | 004 | int grantRows = templateRows == 0 ? jdbcTemplate.update(""" |
| JdbcAccessRepository.java | 7 | `@@ -1043,20 +1051,27` | +17/−10 | 004 | """, parameters) : 0; |
| JdbcAccessRepository.java | 8 | `@@ -1470,16 +1485,5` | +0/−11 | 001 | private static void requireProjectScopeId(String organizationId) { |
| AuthService.java | 1 | `@@ -174,8 +174,10` | +4/−2 | 006 | requireText(request.deviceId(), "device_id"); |
| ProductionAuthService.java | 1 | `@@ -199,8 +199,10` | +4/−2 | 006 | requireText(request.deviceId(), "device_id"); |
| PlatformService.java | 1 | `@@ -89,7 +89,9` | +3/−1 | 001 | PageResponse<BusinessEntityRecord> result = repository.listBusinessEnt |
| PlatformService.java | 2 | `@@ -147,8 +149,11` | +5/−2 | 001 | String projectId = currentProject(); |
| PlatformService.java | 3 | `@@ -404,17 +409,31` | +17/−3 | 001 | requireCurrentProject(merchant.projectId()); |
| PlatformService.java | 4 | `@@ -430,6 +449,10` | +4/−0 | 001 | if (platformWide()) { |
| PlatformService.java | 5 | `@@ -448,7 +471,9` | +3/−1 | 001 | PageResponse<ProjectRecord> result = repository.listProjectsPaged(curr |
| PlatformService.java | 6 | `@@ -505,6 +530,9` | +3/−0 | 001 | if (platformWide()) { |
| PlatformService.java | 7 | `@@ -515,7 +543,9` | +3/−1 | 001 | PageResponse<SpaceRecord> result = repository.listSpacesPaged(currentP |
| PlatformService.java | 8 | `@@ -705,7 +735,9` | +3/−1 | 001 | requireCurrentProject(parent.projectId()); |
| PlatformService.java | 9 | `@@ -734,7 +766,9` | +3/−1 | 001 | repository.listBusinessEntityBindingsPaged(currentProject(), page, siz |
| PlatformService.java | 10 | `@@ -942,7 +976,7` | +1/−1 | 001 | if (!repository.businessEntityInProject(id, currentProject())) { |
| PlatformService.java | 11 | `@@ -1010,7 +1044,7` | +1/−1 | 001 | && !projectId.equals(request.projectId().trim())) { |
| SupervisorReviewService.java | 1 | `@@ -202,7 +202,7` | +1/−1 | 003 | throw ApiException.forbidden("BENEFICIARY_REQUIRED", "退款、免单、授权与资金事项必须登 |
| SupervisorReviewService.java | 2 | `@@ -384,9 +384,10` | +2/−1 | 001 | // §24.3 同商户范围闸：发起人有商户隔离键时，审批人须同商户（仅 USB 最高权限可逐笔越权）。 |
| SupervisorReviewService.java | 3 | `@@ -618,22 +619,26` | +10/−6 | 005（requiresTopAuthorityGrant）、003（requiresBeneficiary 与文案） | /** XM-D029-005（PGM-041「持股分红……由最高权限审批」；其余高危授权按可配政策与指定最终审批人的审批链）：只有分红类升 |
| SupervisorReviewService.java | 4 | `@@ -788,7 +793,7` | +1/−1 | 003 | throw ApiException.forbidden("BENEFICIARY_REQUIRED", "退款、免单、授权与资金事项必须登 |
| SupervisorReviewService.java | 5 | `@@ -880,19 +885,19` | +4/−4 | 001（平台管理员整表）、002（最高权限不整表） | * 待复核清单按当前操作人范围过滤——非密钥主体只看到发起人属本项目（有商户隔离键时同商户）的单据，或自己是当前节点审批人 / 被转发人 |
| SupervisorReviewService.java | 6 | `@@ -921,18 +926,32` | +16/−2 | 001（平台管理员放行）、002（最高权限逐笔留痕、scopeProjectOf） | if (authorizationService.isMasterAuthority(actor)) { |
| AccessJdbcIntegrationTests.java | 1 | `@@ -872,4 +872,37` | +33/−0 | 004 | // XM-D029-004（PGM-041「临时授权可设有效期或单次使用；……单次消耗……全部留痕」）：一组两码的单次临时授权， |
| AccessServiceTests.java | 1 | `@@ -2111,6 +2111,18` | +12/−0 | 003 | // XM-D029-003：与 JDBC 同样存下排期里的直接受益人，审批时才读得到。 |
| AuthorizationServiceTests.java | 1 | `@@ -224,15 +224,19` | +8/−4 | 001 | void currentMerchantScopeReturnsBoundMerchantForPlatformAdmin() { |
| M006SecurityRegressionTests.java | 1 | `@@ -593,10 +593,12` | +2/−0 | 008 | // M037-002：非平台管理员不得操作岗位模板生命周期，且不落任何写入。 |
| M006SecurityRegressionTests.java | 2 | `@@ -611,6 +613,7` | +1/−0 | 008 | // M037-002：最高权限驳回删除——不软删、清掉删除复核单号，之后可再次申请并批准。 |
| AuthServiceTests.java | 1 | `@@ -252,4 +252,18` | +14/−0 | 006 | // XM-D029-006：网页后台登录与刷新都不带设备号——未绑定设备的会话照常续期；带设备号登录的会话，刷新缺或错设备号都拒。 |
| WecomRiskHitsEndpointReproTests.java | 1 | `@@ -42,6 +42,23` | +17/−0 | 007 | // XM-D029-007：生产配置下验签公钥不再能由库凭证开关豁免；以运行时生成的 Ed25519 测试公钥满足启动要求（同 Produ |
| PlatformServiceTests.java | 1 | `@@ -82,7 +82,7` | +1/−1 | 001 | null, null, null, null, null, null, null, null, "active")); |
| PlatformServiceTests.java | 2 | `@@ -95,7 +95,7` | +1/−1 | 001 | .containsEntry("projectId", "org_1") |
| PlatformServiceTests.java | 3 | `@@ -136,7 +136,7` | +1/−1 | 001 | ))).isInstanceOfSatisfying(ApiException.class, exception -> |
| PlatformServiceTests.java | 4 | `@@ -151,7 +151,7` | +1/−1 | 001 | null, null, null, null, null, null, null, null, "active")); |
| PlatformServiceTests.java | 5 | `@@ -207,12 +207,12` | +2/−2 | 001 | null, null, null, null, null, null, null, null, null, null, null, "fix |
| PlatformServiceTests.java | 6 | `@@ -222,12 +222,12` | +2/−2 | 001 | null, null, null, null, null, null, null, null, null, null, null, null |
| PlatformServiceTests.java | 7 | `@@ -248,7 +248,7` | +1/−1 | 001 | "entity-spv", "create", List.of(), "SPV 主体", "spv", null, null, null,  |
| PlatformServiceTests.java | 8 | `@@ -504,7 +504,7` | +1/−1 | 001 | null, null, null, null, null, null, null, null, "active"))) |
| PlatformServiceTests.java | 9 | `@@ -516,7 +516,7` | +1/−1 | 001 | null, null, null, null, null, null, null, null, "active")); |
| PlatformServiceTests.java | 10 | `@@ -639,7 +639,8` | +2/−1 | 001 | "active" |
| PlatformServiceTests.java | 11 | `@@ -844,7 +845,7` | +1/−1 | 001 | )); |
| PlatformServiceTests.java | 12 | `@@ -855,7 +856,7` | +1/−1 | 001 | )); |
| PlatformServiceTests.java | 13 | `@@ -894,7 +895,7` | +1/−1 | 001 | "Jane", "18800000011", null, null, null, null, null, null, null, null, |
| PlatformServiceTests.java | 14 | `@@ -1058,7 +1059,7` | +1/−1 | 001 | null, null, null, null, null, null, null, area, leasedArea, null, null |
| PlatformServiceTests.java | 15 | `@@ -1093,7 +1094,7` | +1/−1 | 001 | "invalid-role-message", null, List.of(), "公司", "unknown", null, null,  |
| PlatformServiceTests.java | 16 | `@@ -1141,6 +1142,9` | +3/−0 | 001 | // D-032：平台管理员不受当前项目限定；本用例守的是普通项目岗位仍只见本项目（M009-038）。 |
| PlatformServiceTests.java | 17 | `@@ -1172,6 +1176,9` | +3/−0 | 001 | // D-032：平台管理员不受当前项目限定；「须在当前项目」守的是普通项目岗位（M009-038）。 |
| PlatformServiceTests.java | 18 | `@@ -1261,9 +1268,6` | +0/−3 | 001 | assertThatThrownBy(() -> service.createBusinessEntity(new BusinessEnti |
| PlatformServiceTests.java | 19 | `@@ -1481,4 +1485,16` | +12/−0 | 001 | // D-032（红线④不约束平台管理员；BSM-007、PGM-041「只有项目之上的获授权岗位才可以跨项目汇总」）：平台管理员列表不按当 |
| SupervisorReviewServiceTests.java | 1 | `@@ -435,14 +435,14` | +4/−4 | 001、002 | TestSecurityActors.asPlatformAdmin("usr_admin_all"); |
| SupervisorReviewServiceTests.java | 2 | `@@ -759,8 +759,8` | +2/−2 | 005 | var pending = service.blockForAdmission("permission_grant_l2", "access |
| SupervisorReviewServiceTests.java | 3 | `@@ -875,11 +875,7` | +1/−5 | 002 | org.springframework.security.core.context.SecurityContextHolder.getCon |
| SupervisorReviewServiceTests.java | 4 | `@@ -1232,4 +1228,76` | +72/−0 | 002、003、005（新增 5 条回归） | // XM-D029-002（治理地图第 528 行第④类「持授权 U 盘密钥的最高权限可以逐笔开放，每次只对当前这一笔生效并留痕」）： |

共 61 块，+336/−100，逐块归因齐全。

- 14 个文件全部在 B2 定稿写入集合内（B2-r2 已去掉乙案的业务与切面文件，本 diff 未碰它们）；无 mode 变更；无归因不到的行。归因票：**PASS**。

## 2. 内容审计（逐 ID 对定稿边界）

- 001：AuthorizationService 三个范围方法各加一段平台管理员桥返回空（恢复发布前语义），查他人有效权限处平台管理员与最高权限都放行；isMasterAuthority（master_authority＋保留 id＋保留用户名）、职级档位封顶两级、日常通配与显式拒绝优先均未动。Jdbc 五个带参列表空范围恢复为全量（listPositionRoleTemplates 恢复发布前原查询），两个私有 require* 辅助方法删去。审批 :390 同商户闸在保留最高权限逐笔例外的同时加平台管理员；待审整表只给平台管理员；requireVisibleReview 平台管理员直接放行。PlatformService：新增 platformWide()（范围为空即平台管理员）；五类列表在平台管理员时走仓储不带项目参数的重载，没有把 null 传进带 projectId 的重载；requireMerchant、spaceExists、requireBusinessEntity 对平台管理员只校验对象存在；requireCurrentProject 对平台管理员只校验项目存在；新建（creationProject）对平台管理员 requireText(project_id) 后校验存在，不隐式填；建空间时父空间对平台管理员须与所建空间同项目（完整性，非范围）；商户档案更新的项目比较改为空安全（平台管理员更新无项目旧商户时原 equals 会空指针，属 001 连带）。普通项目岗位路径逐字不变（M009-038）。测试：AuthorizationServiceTests 恢复两条平台管理员全量旧用例、删去相反两条；PlatformServiceTests 新建调用按实参个数补显式项目 "org_1"（商户 19 参便捷构造与公司 9 参便捷构造、numericMerchant 辅助、一处变量形式请求），商户列表留痕期望 projectId 由 "org_1" 改为空，两条「普通岗位只在本项目」用例改以普通项目员工身份跑，删去「平台管理员不能在别的项目建公司」一句（与 D-032 冲突；同用例里「项目归属配置须由最高权限执行」的两句保留），新增平台管理员五类列表跨项目合计为 2 的回归。
- 002：待审清单最高权限不再整表、不再只记一条总数，按普通可见规则（同项目同商户、本人为当前节点/被转发人/发起人）过滤；scopeProjectOf 对最高权限取其组织号，无则空串，不抛 PROJECT_SCOPE_REQUIRED；requireVisibleReview 对最高权限：可见则放行，不可见时逐笔放行并记 governance.supervisor_review.viewed_cross_scope（业务号＝复核单号）。测试：待审清单用例改为平台管理员整表、最高权限只见本项目单；涉钱时限用例的整表断言改由平台管理员；新增「最高权限按编号读别项目复核单逐笔留痕、列表不含」回归。
- 003（丁案）：requiresBeneficiary 恰为「含 grant 或 authorization，或恰为 access.grant_request.money／access.scope_change.money」；建单、审批、转发三处仍调同一方法；:208、:800 等于比较未动；三处报错文案由「退款、免单、授权与资金事项必须登记直接受益人」改为「授权事项必须登记直接受益人（被授权人）」（否则文案对免单、退款失真，GEN-016）。AccessServiceTests 内存仓库补排期存取。新增三条回归：六参 bar.bill_waiver 受益人 null 能建单且审批不因缺受益人被拒；授权申请缺受益人仍拒；被授权人本人审批仍拒。未改任何业务入口、切面与 AccessService。
- 004：UPDATE…RETURNING permission_code 取回本次被标记消耗的码，逐码各写一条使用日志（模板应用路径仍只记触发码）；新增真库回归（一组两码用一个，两码各有 consumed 留痕）。
- 005：requiresTopAuthorityGrant 只看动作名含 dividend；拒自报 l1/l2 保留。「升级后转发失效」用例原依赖 .money 自动升档，改为显式传 permission_grant_l3／top_authority／3（即旧自动升档给出的同一组值），用例本意不变；新增「资金类授权保留策略档位、分红类升最高权限档」回归。
- 006：两处刷新改为「会话绑定设备（非空白且非哨兵 unknown）或请求带了设备号时，须与会话设备号一致」；主会话禁止续期未动；新增网页会话无设备号可续期、设备会话缺或错设备号仍拒的回归（AuthService）；ProductionAuthService 同一改动，走同一判断。
- 007：WecomRiskHitsEndpointReproTests 按 ProductionAuthJdbcIntegrationTests 方式以运行时 Ed25519 测试公钥补 complex.auth.master.public-key；未恢复豁免。
- 008：M006SecurityRegressionTests 恢复 `verify(repository, never()).savePositionTemplateLifecycle(any(), anyString())` 与两处 M037-002 注释。
- 能否失败：RED 阶段 11 条新增或改动的测试相对原样基线新红（§4），GREEN 全绿；无为刷绿改宽的断言。
- 发现：PlatformJdbcIntegrationTests.completingSpaceTypeRequestKeepsPriorRequestHistory 在试跑中时红时绿，根因 JdbcPlatformRepository.saveSpaceTypeDeleteRequest 以「stdr_＋类型号＋毫秒」作主键，同一毫秒两次申请撞键；M009 已发布代码，不在本范围，登记 XM-M036-A。
- 内容票：**PASS**。

## 3. diff 全文

```diff
diff --git a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
index 4bfae6d2..06747a66 100644
--- a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
+++ b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
@@ -157,7 +157,7 @@ public class AuthorizationService implements CallerScopeResolver {
         AccessActor actor = currentActor();
         AccessUser user = accessRepository.findUserById(userId)
                 .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "账号不存在"));
-        if (!isMasterAuthority(actor)) {
+        if (!isPlatformAdmin(actor) && !isMasterAuthority(actor)) {
             String organizationId = requireProjectScope(actor);
             if (!organizationId.equals(user.organizationId())) {
                 throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "无权查询其他组织的用户");
@@ -418,12 +418,15 @@ public class AuthorizationService implements CallerScopeResolver {
 
     /**
      * Returns the merchant scope of the current caller.
+     * Platform admins get Optional.empty() (no filter, sees everything; D-032：红线④不约束平台管理员).
      * Merchant users get Optional.of(merchantId).
-     * Callers without a merchantId throw MERCHANT_SCOPE_REQUIRED.
-     * 红线④：不因平台管理员身份整类关掉商户隔离；持密钥最高权限只在逐笔接口放行。
+     * Other callers without a merchantId throw MERCHANT_SCOPE_REQUIRED.
      */
     public Optional<String> currentMerchantScope() {
         AccessActor actor = currentActor();
+        if (isPlatformAdminBridge(actor)) {
+            return Optional.empty();
+        }
         String merchantId = actor.merchantId();
         if (merchantId != null && !merchantId.isBlank()) {
             return Optional.of(merchantId);
@@ -433,13 +436,16 @@ public class AuthorizationService implements CallerScopeResolver {
 
     /**
      * Returns the current caller's merchant id if present, otherwise empty.
-     * Unlike {@link #currentMerchantScope()}, this never throws: callers without a merchant
+     * Unlike {@link #currentMerchantScope()}, this never throws: platform admins and callers without a merchant
      * yield {@code Optional.empty()}. Degrades to empty on any error.
-     * 有商户号就按商户号过滤，不因平台管理员身份改成看全量。
+     * 平台管理员即便被种子误绑 merchant_id，也按平台视角看全量（D-032）。
      */
     public Optional<String> currentMerchantIdQuietly() {
         try {
             AccessActor actor = currentActor();
+            if (isPlatformAdminBridge(actor)) {
+                return Optional.empty();
+            }
             String merchantId = actor.merchantId();
             if (merchantId != null && !merchantId.isBlank()) {
                 return Optional.of(merchantId);
@@ -498,11 +504,13 @@ public class AuthorizationService implements CallerScopeResolver {
     }
 
     /**
-     * 项目维度只读 scope：返回当前主体的项目 ID。没有项目归属则拒绝。
-     * 红线④：不因平台管理员身份返回空（全量）。
+     * 项目维度只读 scope：平台管理员返回空＝全量（D-032）；其余主体返回其项目 ID，没有项目归属则拒绝。
      */
     public Optional<String> currentProjectScopeQuietly() {
         AccessActor actor = currentActor();
+        if (isPlatformAdminBridge(actor)) {
+            return Optional.empty();
+        }
         return Optional.of(requireProjectScope(actor));
     }
 
diff --git a/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java b/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
index 837b04aa..dfb369d0 100644
--- a/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
+++ b/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
@@ -72,7 +72,7 @@ class JdbcAccessRepository implements AccessRepository {
 
     @Override
     public List<PermissionTemplate> listTemplates(String organizationId) {
-        requireProjectScopeId(organizationId);
+        if (organizationId == null) return listTemplates();
         return jdbcTemplate.query("""
                 SELECT t.id, t.template_code, t.template_name, t.description, t.template_type, t.source_template_id,
                        t.owner_scope_type, t.owner_scope_id, t.usage_mode, t.valid_from, t.valid_to, t.single_use,
@@ -199,7 +199,7 @@ class JdbcAccessRepository implements AccessRepository {
 
     @Override
     public List<AccessUser> listUsers(String organizationId) {
-        requireProjectScopeId(organizationId);
+        if (organizationId == null) return listUsers();
         return jdbcTemplate.query("""
                 SELECT id, username, account_type, organization_id, merchant_id, status, created_at,
                        phone_hash, created_via, consent_reference, evidence_attachment_ids, operator_note
@@ -211,7 +211,8 @@ class JdbcAccessRepository implements AccessRepository {
 
     @Override
     public List<AccessUser> listUsersByMerchant(String merchantId) {
-        requireMerchantScopeId(merchantId);
+        // 商户面 scoped 读通道：按 access_users.merchant_id = token scope 过滤（AC-ISO-1，§4）。
+        if (merchantId == null) return listUsers();
         return jdbcTemplate.query("""
                 SELECT id, username, account_type, organization_id, merchant_id, status, created_at,
                        phone_hash, created_via, consent_reference, evidence_attachment_ids, operator_note
@@ -306,7 +307,7 @@ class JdbcAccessRepository implements AccessRepository {
 
     @Override
     public List<RoleRecord> listRoles(String organizationId) {
-        requireProjectScopeId(organizationId);
+        if (organizationId == null) return listRoles();
         return attachPermissionCodes(jdbcTemplate.query("""
                 SELECT id, role_name, description, data_scope, status, created_at, organization_id
                   FROM roles
@@ -415,7 +416,14 @@ class JdbcAccessRepository implements AccessRepository {
 
     @Override
     public List<PositionRoleTemplate> listPositionRoleTemplates(String organizationId) {
-        requireProjectScopeId(organizationId);
+        if (organizationId == null) {
+            return jdbcTemplate.query("""
+                    SELECT id, position_code, staff_level_id, role_id, template_id, organization_id,
+                           status, created_by, created_at, updated_at, archived_at, deleted_at, delete_review_id, delete_reason
+                      FROM position_role_templates
+                     ORDER BY position_code, staff_level_id, organization_id NULLS FIRST
+                    """, this::mapPositionRoleTemplate);
+        }
         return jdbcTemplate.query("""
                 SELECT id, position_code, staff_level_id, role_id, template_id, organization_id,
                        status, created_by, created_at, updated_at, archived_at, deleted_at, delete_review_id, delete_reason
@@ -1034,7 +1042,7 @@ class JdbcAccessRepository implements AccessRepository {
                    AND application.status = 'active'
                    AND application.consumed = FALSE
                 """, parameters);
-        int grantRows = templateRows == 0 ? jdbcTemplate.update("""
+        List<String> consumedGrantCodes = templateRows == 0 ? jdbcTemplate.queryForList("""
                 UPDATE temporary_permission_grants
                    SET consumed = TRUE,
                        consumed_at = :consumed_at
@@ -1043,20 +1051,27 @@ class JdbcAccessRepository implements AccessRepository {
                    AND status = 'active'
                    AND single_use = TRUE
                    AND consumed = FALSE
-                """, parameters) : 0;
-        boolean consumed = templateRows > 0 || grantRows > 0;
+                RETURNING permission_code
+                """, parameters, String.class) : List.of();
+        boolean consumed = templateRows > 0 || !consumedGrantCodes.isEmpty();
         if (consumed) {
             parameters.addValue("application_id", templateRows > 0 ? sourceId : null)
                     .addValue("reason", templateRows > 0 ? "single_use"
                             : "single_use:temporary_grant:" + sourceId);
-            jdbcTemplate.update("""
-                    INSERT INTO permission_template_usage_logs (
-                        id, application_id, user_id, permission_code, result, reason, occurred_at
-                    )
-                    VALUES (
-                        :usage_log_id, :application_id, :user_id, :permission_code, 'consumed', :reason, :consumed_at
-                    )
-                    """, parameters);
+            // XM-D029-004（PGM-041「单次消耗全部留痕」）：整组消耗时，本次被标成已消耗的每个权限码各写一条使用日志。
+            List<String> loggedCodes = templateRows > 0 ? List.of(permissionCode) : consumedGrantCodes;
+            for (String loggedCode : loggedCodes) {
+                jdbcTemplate.update("""
+                        INSERT INTO permission_template_usage_logs (
+                            id, application_id, user_id, permission_code, result, reason, occurred_at
+                        )
+                        VALUES (
+                            :usage_log_id, :application_id, :user_id, :permission_code, 'consumed', :reason, :consumed_at
+                        )
+                        """, new MapSqlParameterSource(parameters.getValues())
+                        .addValue("usage_log_id", UUID.randomUUID())
+                        .addValue("permission_code", loggedCode));
+            }
         }
         return consumed;
     }
@@ -1470,16 +1485,5 @@ class JdbcAccessRepository implements AccessRepository {
         return effect.name().toLowerCase(java.util.Locale.ROOT);
     }
 
-    private static void requireProjectScopeId(String organizationId) {
-        if (organizationId == null || organizationId.isBlank()) {
-            throw ApiException.forbidden("PROJECT_SCOPE_REQUIRED", "此操作必须在项目范围内进行");
-        }
-    }
-
-    private static void requireMerchantScopeId(String merchantId) {
-        if (merchantId == null || merchantId.isBlank()) {
-            throw ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");
-        }
-    }
 
 }
diff --git a/backend/src/main/java/com/complex/platform/auth/AuthService.java b/backend/src/main/java/com/complex/platform/auth/AuthService.java
index a6d45e92..8347b7ac 100644
--- a/backend/src/main/java/com/complex/platform/auth/AuthService.java
+++ b/backend/src/main/java/com/complex/platform/auth/AuthService.java
@@ -174,8 +174,10 @@ public class AuthService implements SessionIssuer {
             if (!session.id().equals(accessSessionId)) {
                 throw ApiException.unauthorized("SESSION_MISMATCH", "登录凭证与当前会话不匹配，请重新登录");
             }
-            requireText(request.deviceId(), "device_id");
-            if (!request.deviceId().equals(session.deviceId())) {
+            // XM-D029-006：登录时绑定了设备号的会话，刷新须带同一设备号（缺或错都拒）；未绑定设备（网页，空白或哨兵 unknown）照常续期；
+            // 提供了设备号就须与会话一致。
+            boolean deviceBound = !isBlank(session.deviceId()) && !"unknown".equals(session.deviceId());
+            if ((deviceBound || !isBlank(request.deviceId())) && !java.util.Objects.equals(request.deviceId(), session.deviceId())) {
                 throw ApiException.unauthorized("SESSION_MISMATCH", "设备与当前会话不匹配，请重新登录");
             }
             if (SessionIssuer.MASTER_USER_ID.equals(session.userId())) {
diff --git a/backend/src/main/java/com/complex/platform/auth/ProductionAuthService.java b/backend/src/main/java/com/complex/platform/auth/ProductionAuthService.java
index 7ecb5646..e28f1e39 100644
--- a/backend/src/main/java/com/complex/platform/auth/ProductionAuthService.java
+++ b/backend/src/main/java/com/complex/platform/auth/ProductionAuthService.java
@@ -199,8 +199,10 @@ public class ProductionAuthService implements SessionIssuer {
             if (!session.accessTokenHash().equals(sha256Hex(currentAccessToken))) {
                 throw ApiException.unauthorized("SESSION_MISMATCH", "登录凭证与当前会话不匹配，请重新登录");
             }
-            requireText(request.deviceId(), "device_id");
-            if (!request.deviceId().equals(session.deviceId())) {
+            // XM-D029-006：登录时绑定了设备号的会话，刷新须带同一设备号（缺或错都拒）；未绑定设备（网页，空白或哨兵 unknown）照常续期；
+            // 提供了设备号就须与会话一致。
+            boolean deviceBound = !isBlank(session.deviceId()) && !"unknown".equals(session.deviceId());
+            if ((deviceBound || !isBlank(request.deviceId())) && !java.util.Objects.equals(request.deviceId(), session.deviceId())) {
                 throw ApiException.unauthorized("SESSION_MISMATCH", "设备与当前会话不匹配，请重新登录");
             }
             if (SessionIssuer.MASTER_USER_ID.equals(session.userId())) {
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformService.java b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
index 63e53b31..2da0b8ee 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformService.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
@@ -89,7 +89,9 @@ public class PlatformService {
 
     // AUD-005 真分页: SQL COUNT + LIMIT/OFFSET, total 取真值; 默认 500 保留既有前端一次拉取(下拉源)契约。
     public PageResponse<BusinessEntityRecord> listBusinessEntities(int page, int size) {
-        PageResponse<BusinessEntityRecord> result = repository.listBusinessEntitiesPaged(currentProject(), page, size);
+        PageResponse<BusinessEntityRecord> result = platformWide()
+                ? repository.listBusinessEntitiesPaged(page, size)
+                : repository.listBusinessEntitiesPaged(currentProject(), page, size);
         auditEventService.recordSuccess(
                 "entity_binding.viewed",
                 "entity.binding.view",
@@ -147,8 +149,11 @@ public class PlatformService {
             throw ApiException.badRequest("MERCHANT_BUSINESS_FORMAT_INVALID",
                     "商户业态必须为 酒吧/餐饮/零售/游乐/传媒/其他 之一");
         }
-        String projectId = currentProject();
-        PageResponse<MerchantRecord> result = repository.listMerchantsPaged(projectId, includeArchived, format, page, size);
+        boolean allProjects = platformWide();
+        String projectId = allProjects ? null : currentProject();
+        PageResponse<MerchantRecord> result = allProjects
+                ? repository.listMerchantsPaged(includeArchived, format, page, size)
+                : repository.listMerchantsPaged(projectId, includeArchived, format, page, size);
         auditEventService.recordSuccess(
                 "merchant.viewed",
                 "merchant.profile.view",
@@ -404,17 +409,31 @@ public class PlatformService {
 
     private MerchantRecord requireMerchant(String id) {
         MerchantRecord merchant = findMerchant(id);
-        requireCurrentProject(merchant.projectId());
+        if (!platformWide()) {
+            requireCurrentProject(merchant.projectId());
+        }
         return merchant;
     }
 
+    /** D-032：平台管理员（项目范围为空＝全量）不受当前项目限定；普通项目岗位按真实项目校验（M009-038）。 */
+    private boolean platformWide() {
+        authorizationService.requirePlatformScope();
+        return authorizationService.currentProjectScopeQuietly().isEmpty();
+    }
+
     private String currentProject() {
         authorizationService.requirePlatformScope();
-        String scope = authorizationService.currentProjectScopeQuietly().orElseThrow();
-        return repository.projectExists(scope) ? scope : null;
+        String scope = authorizationService.currentProjectScopeQuietly().orElse(null);
+        return scope != null && repository.projectExists(scope) ? scope : null;
     }
 
     private String requireCurrentProject(String projectId) {
+        if (platformWide()) {
+            if (projectId == null || !repository.projectExists(projectId)) {
+                throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
+            }
+            return projectId;
+        }
         String current = currentProject();
         if (current == null || !current.equals(projectId)) {
             throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "无权访问该项目的数据");
@@ -430,6 +449,10 @@ public class PlatformService {
             }
             return explicit;
         }
+        if (platformWide()) {
+            // 平台管理员新建须显式给出已存在的项目，不按「当前项目」隐式填。
+            return requireCurrentProject(requireText(explicit, "project_id"));
+        }
         String current = currentProject();
         return requireCurrentProject(explicit == null ? current : explicit);
     }
@@ -448,7 +471,9 @@ public class PlatformService {
     }
 
     public PageResponse<ProjectRecord> listProjects(int page, int size) {
-        PageResponse<ProjectRecord> result = repository.listProjectsPaged(currentProject(), page, size);
+        PageResponse<ProjectRecord> result = platformWide()
+                ? repository.listProjectsPaged(page, size)
+                : repository.listProjectsPaged(currentProject(), page, size);
         auditEventService.recordSuccess(
                 "project.viewed",
                 "governance.project.view",
@@ -505,6 +530,9 @@ public class PlatformService {
 
     /** 3.13：空间存在性（招商线索/意向意向空间校验用）。 */
     public boolean spaceExists(String spaceId) {
+        if (platformWide()) {
+            return spaceId != null && repository.findSpaceById(spaceId).isPresent();
+        }
         String projectId = currentProject();
         return spaceId != null && projectId != null && repository.findSpaceById(spaceId)
                 .filter(space -> projectId.equals(space.projectId())).isPresent();
@@ -515,7 +543,9 @@ public class PlatformService {
     }
 
     public PageResponse<SpaceRecord> listSpaces(int page, int size) {
-        PageResponse<SpaceRecord> result = repository.listSpacesPaged(currentProject(), page, size);
+        PageResponse<SpaceRecord> result = platformWide()
+                ? repository.listSpacesPaged(page, size)
+                : repository.listSpacesPaged(currentProject(), page, size);
         auditEventService.recordSuccess(
                 "space.viewed",
                 "governance.space.view",
@@ -705,7 +735,9 @@ public class PlatformService {
                     if (parentSpaceId != null) {
                         SpaceRecord parent = repository.findSpaceById(parentSpaceId)
                                 .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "上级空间不存在"));
-                        requireCurrentProject(parent.projectId());
+                        if (platformWide() ? !projectId.equals(parent.projectId()) : !projectId.equals(requireCurrentProject(parent.projectId()))) {
+                            throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "上级空间须属于同一项目");
+                        }
                     }
                     requireNonNegative(request.areaSquareMeter(), "空间面积");
                     Instant now = Instant.now();
@@ -734,7 +766,9 @@ public class PlatformService {
     public PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindings(int page, int size) {
         try {
             PageResponse<BusinessEntityBindingRecord> result =
-                    repository.listBusinessEntityBindingsPaged(currentProject(), page, size);
+                    platformWide()
+                            ? repository.listBusinessEntityBindingsPaged(page, size)
+                            : repository.listBusinessEntityBindingsPaged(currentProject(), page, size);
             auditEventService.recordSuccess(
                     "entity_binding.viewed",
                     "entity.binding.view",
@@ -942,7 +976,7 @@ public class PlatformService {
         if (!repository.businessEntityExists(id)) {
             throw ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "主体不存在：" + id);
         }
-        if (!repository.businessEntityInProject(id, currentProject())) {
+        if (!platformWide() && !repository.businessEntityInProject(id, currentProject())) {
             throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "该主体未关联当前项目");
         }
         return id;
@@ -1010,7 +1044,7 @@ public class PlatformService {
                     MerchantRecord existing = update ? requireMerchant(merchantId) : null;
                     String projectId = existing == null ? creationProject(request.projectId(), false) : existing.projectId();
                     if (existing != null && trimToNull(request.projectId()) != null
-                            && !projectId.equals(request.projectId().trim())) {
+                            && !java.util.Objects.equals(projectId, request.projectId().trim())) {
                         throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "普通档案更新不能变更商户的项目归属");
                     }
                     if (existing == null) {
diff --git a/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java b/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java
index 90d22737..b7118551 100644
--- a/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java
+++ b/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java
@@ -202,7 +202,7 @@ public class SupervisorReviewService {
                     Optional<SupervisorReviewRepository.ReviewSchedule> schedule =
                             supervisorReviewRepository.findSupervisorReviewSchedule(id);
                     if (requiresBeneficiary(pending.actionType()) && schedule.map(s -> !textPresent(s.beneficiaryId())).orElse(true)) {
-                        throw ApiException.forbidden("BENEFICIARY_REQUIRED", "退款、免单、授权与资金事项必须登记直接受益人");
+                        throw ApiException.forbidden("BENEFICIARY_REQUIRED", "授权事项必须登记直接受益人（被授权人）");
                     }
                     // PGM-1.7.1「申请人与直接受益人不得审批与自己相关的申请，转发不构成例外」。
                     if (schedule.map(s -> reviewerId != null && reviewerId.equals(s.beneficiaryId())).orElse(false)) {
@@ -384,9 +384,10 @@ public class SupervisorReviewService {
                         "SUPERVISOR_ROLE_NOT_AUTHORIZED",
                         "复核人不具备该复核要求的审批角色");
             }
-            // §24.3 同商户范围闸：发起人有商户隔离键时，审批人须同商户（仅 USB 最高权限可逐笔越权）。
+            // §24.3 同商户范围闸：发起人有商户隔离键时，审批人须同商户（平台管理员越权，D-032；USB 最高权限逐笔越权）。
             Optional<String> initiatorMerchant = authorizationService.merchantIdOf(pending.initiatedBy());
             if (initiatorMerchant.isPresent()
+                    && !authorizationService.isPlatformAdmin(authorizationService.currentActor())
                     && !authorizationService.isMasterAuthority(authorizationService.currentActor())) {
                 Optional<String> reviewerMerchant = authorizationService.merchantIdOf(reviewerId);
                 if (reviewerMerchant.isEmpty() || !reviewerMerchant.get().equals(initiatorMerchant.get())) {
@@ -618,22 +619,26 @@ public class SupervisorReviewService {
         return findPolicy(actionType);
     }
 
+    /** XM-D029-005（PGM-041「持股分红……由最高权限审批」；其余高危授权按可配政策与指定最终审批人的审批链）：只有分红类升最高权限档。 */
     private boolean requiresTopAuthorityGrant(String actionType, Long amountCent) {
         String type = actionType == null ? "" : actionType;
-        return type.endsWith(".money") || type.contains("debit") || type.contains("deduction")
-                || type.contains("dividend") || type.contains("authorization");
+        return type.contains("dividend");
     }
 
+    /**
+     * XM-D029-003（治理地图第 529 行「申请人与直接受益人不得审批」）：只对授权类强制登记直接受益人——受益人是被授权的员工账号，
+     * 与审批人、转发目标同一编号体系，比较真实生效。免单、退款等准入路径的收款人是客人或往来方，与员工账号不同编号体系，
+     * 不在此强制（缺员工与客人／往来方的身份关联，另登记 XM）。
+     */
     private boolean requiresBeneficiary(String actionType) {
         String type = actionType == null ? "" : actionType;
-        return type.contains("grant") || type.contains("authorization") || type.contains("refund")
-                || type.contains("waiver") || type.contains("debit") || type.contains("deduction")
-                || type.endsWith(".money") || type.equals("ar_ap_adjustment");
+        return type.contains("grant") || type.contains("authorization")
+                || type.equals("access.grant_request.money") || type.equals("access.scope_change.money");
     }
 
     private void requireBeneficiaryIfNeeded(String actionType, String beneficiaryId) {
         if (requiresBeneficiary(actionType) && !textPresent(beneficiaryId)) {
-            throw ApiException.badRequest("BENEFICIARY_REQUIRED", "退款、免单、授权与资金事项必须登记直接受益人");
+            throw ApiException.badRequest("BENEFICIARY_REQUIRED", "授权事项必须登记直接受益人（被授权人）");
         }
     }
 
@@ -788,7 +793,7 @@ public class SupervisorReviewService {
                     String actorId = authorizationService.currentActor().id();
                     Optional<SupervisorReviewRepository.ReviewSchedule> schedule = supervisorReviewRepository.findSupervisorReviewSchedule(id);
                     if (requiresBeneficiary(pending.actionType()) && schedule.map(s -> !textPresent(s.beneficiaryId())).orElse(true)) {
-                        throw ApiException.forbidden("BENEFICIARY_REQUIRED", "退款、免单、授权与资金事项必须登记直接受益人");
+                        throw ApiException.forbidden("BENEFICIARY_REQUIRED", "授权事项必须登记直接受益人（被授权人）");
                     }
                     List<ApprovalChainNode> nodes = supervisorReviewRepository.listChainNodes(id);
                     if (nodes.isEmpty()) {
@@ -880,19 +885,19 @@ public class SupervisorReviewService {
 
     /**
      * M007-R6-02（PGM-1.10.1「所有业务数据默认按项目隔离……不提供关闭开关」、PGM-1.7.1「④看不到别的商户和别的项目的数据」）：
-     * 待复核清单按当前操作人范围过滤——非密钥主体只看到发起人属本项目（有商户隔离键时同商户）的单据，或自己是当前节点审批人 / 被转发人
-     * 的单据；持密钥最高权限跨范围查看留痕（红线④逐笔开放）。
+     * 待复核清单按当前操作人范围过滤——平台管理员跨范围查看留痕（D-032）；其余主体只看到发起人属本项目（有商户隔离键时同商户）的单据，
+     * 或自己是当前节点审批人 / 被转发人 / 发起人的单据。持密钥最高权限也按此过滤，不整表（红线④逐笔开放：按编号读取见 requireVisibleReview）。
      */
     public PageResponse<SupervisorReviewRepository.PendingReview> listPending(int limit) {
         AccessActor actor = authorizationService.currentActor();
         List<SupervisorReviewRepository.PendingReview> all = supervisorReviewRepository.listPendingSupervisorReviews(limit);
         List<SupervisorReviewRepository.PendingReview> items;
-        if (authorizationService.isMasterAuthority(actor)) {
+        if (authorizationService.isPlatformAdmin(actor)) {
             items = all;
             auditEventService.recordSuccess("governance.supervisor_review.pending_listed_cross_scope", VIEW_PERMISSION, "supervisor_review", "pending", null, null,
                     Map.of("actor_id", actor.id(), "count", items.size()));
         } else {
-            String projectId = authorizationService.requireProjectScope(actor);
+            String projectId = scopeProjectOf(actor);
             String merchantId = actor.merchantId() == null ? "" : actor.merchantId().trim();
             // ponytail: filters the <= limit rows already fetched; move the scope join into the query if pending volume grows
             items = all.stream().filter(item -> visibleTo(actor, projectId, merchantId, item)).toList();
@@ -921,18 +926,32 @@ public class SupervisorReviewService {
 
     private void requireVisibleReview(SupervisorReviewDecisionRecord record) {
         AccessActor actor = authorizationService.currentActor();
-        if (authorizationService.isMasterAuthority(actor)) {
+        if (authorizationService.isPlatformAdmin(actor)) {
             return;
         }
-        String projectId = authorizationService.requireProjectScope(actor);
+        String projectId = scopeProjectOf(actor);
         String merchantId = actor.merchantId() == null ? "" : actor.merchantId().trim();
         SupervisorReviewRepository.PendingReview item = new SupervisorReviewRepository.PendingReview(
                 record, supervisorReviewRepository.findSupervisorReviewSchedule(record.reviewId()).orElse(null));
         if (!visibleTo(actor, projectId, merchantId, item)) {
+            if (authorizationService.isMasterAuthority(actor)) {
+                // XM-D029-002（红线④「持授权 U 盘密钥的最高权限可以逐笔开放，每次只对当前这一笔生效并留痕」）：按编号逐笔放行并留痕。
+                auditEventService.recordSuccess("governance.supervisor_review.viewed_cross_scope", VIEW_PERMISSION, "supervisor_review",
+                        record.reviewId(), null, null, Map.of("actor_id", actor.id(), "review_id", record.reviewId()));
+                return;
+            }
             throw ApiException.forbidden("SUPERVISOR_REVIEW_SCOPE_FORBIDDEN", "看不到别的商户或别的项目的复核单");
         }
     }
 
+    /** 可见范围的项目：最高权限可无项目号（只见本人为当前节点、被转发人或发起人的单），其余主体无项目号即拒。 */
+    private String scopeProjectOf(AccessActor actor) {
+        if (authorizationService.isMasterAuthority(actor)) {
+            return actor.organizationId() == null ? "" : actor.organizationId().trim();
+        }
+        return authorizationService.requireProjectScope(actor);
+    }
+
     /**
      * M007-A-01（PGM-1.2 第 5 步「放行结果都要记录」、PGM-1.7.1「审批通过才生效」「逐笔放行」）：
      * 准入闸在拦截前调用——同动作、同对象、同发起人且已批准未放行的复核单被消费一次并放行本次动作，
diff --git a/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
index da5b59ed..4ad77d34 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
@@ -872,4 +872,37 @@ class AccessJdbcIntegrationTests {
     ) {
         return new PermissionTemplateItem(templateId, permissionCode, effect, "project", false);
     }
+
+    // XM-D029-004（PGM-041「临时授权可设有效期或单次使用；……单次消耗……全部留痕」）：一组两码的单次临时授权，
+    // 用其中一个即整组消耗，两个码都各有一条消耗留痕。
+    @Test
+    void consumingOneCodeOfASingleUseGrantGroupLogsEveryConsumedCode() {
+        TemporaryGrant grant = new TemporaryGrant(
+                "22222222-2222-2222-2222-222222222222",
+                "usr_jdbc_group_consume",
+                List.of("ops.production_readiness.view", "ops.production_readiness.check"),
+                Instant.parse("2026-06-14T00:00:00Z"),
+                Instant.parse("2099-06-15T00:00:00Z"),
+                true,
+                false,
+                "active",
+                "single use",
+                "seed-admin",
+                Instant.parse("2026-06-14T00:00:00Z"),
+                null,
+                null
+        );
+        repository.createTemporaryGrant(grant);
+
+        assertThat(repository.markSingleUsePermissionConsumed(grant.id(), "ops.production_readiness.view",
+                "usr_jdbc_group_consume", Instant.parse("2026-06-14T12:00:00Z"))).isTrue();
+
+        assertThat(jdbcTemplate.queryForList("""
+                SELECT permission_code
+                  FROM permission_template_usage_logs
+                 WHERE user_id = ? AND result = 'consumed'
+                 ORDER BY permission_code
+                """, String.class, "usr_jdbc_group_consume"))
+                .containsExactly("ops.production_readiness.check", "ops.production_readiness.view");
+    }
 }
diff --git a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
index ebdf8cae..a89c29bf 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
@@ -2111,6 +2111,18 @@ class AccessServiceTests {
         // M087：直接上级表与审批链节点（申请人 usr_requester 的直接上级为 usr_admin，链上只有这一个节点）。
         final Map<String, String> superiors = new LinkedHashMap<>(Map.of("usr_requester", "usr_admin"));
         final Map<String, List<com.complex.platform.governance.chain.ApprovalChainNode>> nodes = new LinkedHashMap<>();
+        // XM-D029-003：与 JDBC 同样存下排期里的直接受益人，审批时才读得到。
+        final Map<String, ReviewSchedule> schedules = new LinkedHashMap<>();
+
+        @Override
+        public void scheduleSupervisorReview(String reviewId, String beneficiaryId, Instant dueAt) {
+            schedules.put(reviewId, new ReviewSchedule(reviewId, beneficiaryId, dueAt, 0, null, null));
+        }
+
+        @Override
+        public Optional<ReviewSchedule> findSupervisorReviewSchedule(String reviewId) {
+            return Optional.ofNullable(schedules.get(reviewId));
+        }
 
         @Override
         public Optional<String> findDirectSuperior(String userId) {
diff --git a/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java b/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
index f3e040d0..bf1a118b 100644
--- a/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
@@ -224,15 +224,19 @@ class AuthorizationServiceTests {
     }
 
     @Test
-    void currentMerchantScopeReturnsBoundMerchantForPlatformAdmin() {
+    void currentMerchantScopeReturnsEmptyForPlatformAdmin() {
         authenticate("usr_admin", "seed-admin", "platform_admin");
-        assertThat(service.currentMerchantScope()).contains("merchant");
+        assertThat(service.currentMerchantScope()).isEmpty();
     }
 
     @Test
-    void currentMerchantIdQuietlyReturnsBoundMerchantForPlatformAdmin() {
+    void currentMerchantIdQuietlyReturnsEmptyForPlatformAdminEvenWhenSeedBoundMerchantId() {
+        // 审计主线2 根因回归：dev seed 曾给 platform_admin 绑 merchant_id（AuthService seed-merchant）。
+        // authenticate 助手正是这种形态（accountType=platform_admin 且 merchantId=merchant）。
+        // 数据层过滤（TicketingService#filterByMerchantScope / AdmissionGuard）走 currentMerchantIdQuietly，
+        // 平台管理员必须得到 empty=看全平台，绝不能被 seed 的 merchant_id 当成商户过滤成 0。
         authenticate("usr_admin", "seed-admin", "platform_admin");
-        assertThat(service.currentMerchantIdQuietly()).contains("merchant");
+        assertThat(service.currentMerchantIdQuietly()).isEmpty();
     }
 
     @Test
diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
index 1202d4a3..4c6e88ff 100644
--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
@@ -593,10 +593,12 @@ class M006SecurityRegressionTests {
                 reviewRepository, new AuditEventService(new InMemoryAuditEventRepository()), event -> { });
         service = new AccessService(repository, new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                 new AuditEventService(new InMemoryAuditEventRepository()), authorization, credentials, reviews, true, 3, 3);
+        // M037-002：非平台管理员不得操作岗位模板生命周期，且不落任何写入。
         actor("ordinary-staff", "staff", "org", null);
         assertThatThrownBy(() -> service.changePositionTemplateLifecycle("lifecycle",
                 new StatusChangeRequest("staff-archive", "archive", "越权操作", List.of())))
                 .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PLATFORM_ADMIN_REQUIRED"));
+        verify(repository, never()).savePositionTemplateLifecycle(any(), anyString());
         actor("admin", "platform_admin", "org", null);
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("archive", "archive", "停止使用", List.of()));
         assertThat(mapping.get().archivedAt()).isNotNull();
@@ -611,6 +613,7 @@ class M006SecurityRegressionTests {
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                 new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                         com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, null, null, "active"), "unused", List.of()));
+        // M037-002：最高权限驳回删除——不软删、清掉删除复核单号，之后可再次申请并批准。
         var masterAuthentication = SecurityContextHolder.getContext().getAuthentication();
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("reject", "reject_delete", "保留配置", List.of()));
         assertThat(mapping.get().deletedAt()).isNull();
diff --git a/backend/src/test/java/com/complex/platform/auth/AuthServiceTests.java b/backend/src/test/java/com/complex/platform/auth/AuthServiceTests.java
index fb126844..ff546b97 100644
--- a/backend/src/test/java/com/complex/platform/auth/AuthServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/auth/AuthServiceTests.java
@@ -252,4 +252,18 @@ class AuthServiceTests {
                 "staff_work_phone"
         ));
     }
+
+    // XM-D029-006：网页后台登录与刷新都不带设备号——未绑定设备的会话照常续期；带设备号登录的会话，刷新缺或错设备号都拒。
+    @Test
+    void webSessionRefreshesWithoutDeviceButDeviceBoundSessionNeedsTheSameDevice() {
+        AuthService.LoginResponse web = service.login(new AuthService.LoginRequest("p0_admin", "secret", null, null, null));
+        assertThat(service.refresh(new AuthService.RefreshTokenRequest(web.refreshToken(), null), web.accessToken()).sessionId())
+                .isEqualTo(web.sessionId());
+
+        AuthService.LoginResponse phone = login();
+        assertThatThrownBy(() -> service.refresh(new AuthService.RefreshTokenRequest(phone.refreshToken(), null), phone.accessToken()))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SESSION_MISMATCH"));
+        assertThatThrownBy(() -> service.refresh(new AuthService.RefreshTokenRequest(phone.refreshToken(), "other-phone"), phone.accessToken()))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SESSION_MISMATCH"));
+    }
 }
diff --git a/backend/src/test/java/com/complex/platform/compliance/WecomRiskHitsEndpointReproTests.java b/backend/src/test/java/com/complex/platform/compliance/WecomRiskHitsEndpointReproTests.java
index f088d557..921fafa4 100644
--- a/backend/src/test/java/com/complex/platform/compliance/WecomRiskHitsEndpointReproTests.java
+++ b/backend/src/test/java/com/complex/platform/compliance/WecomRiskHitsEndpointReproTests.java
@@ -42,6 +42,23 @@ import org.springframework.transaction.annotation.Transactional;
 @Transactional
 class WecomRiskHitsEndpointReproTests {
 
+    // XM-D029-007：生产配置下验签公钥不再能由库凭证开关豁免；以运行时生成的 Ed25519 测试公钥满足启动要求（同 ProductionAuthJdbcIntegrationTests）。
+    private static final java.security.KeyPair MASTER_KEY_PAIR = generateMasterKeyPair();
+
+    @org.springframework.test.context.DynamicPropertySource
+    static void masterPublicKey(org.springframework.test.context.DynamicPropertyRegistry registry) {
+        registry.add("complex.auth.master.public-key",
+                () -> java.util.Base64.getEncoder().encodeToString(MASTER_KEY_PAIR.getPublic().getEncoded()));
+    }
+
+    private static java.security.KeyPair generateMasterKeyPair() {
+        try {
+            return java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
+        } catch (java.security.GeneralSecurityException exception) {
+            throw new IllegalStateException(exception);
+        }
+    }
+
     @Autowired
     private MockMvc mockMvc;
 
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
index c9315fa5..16afe9e9 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
@@ -82,7 +82,7 @@ class PlatformServiceTests {
         service.createMerchant(new MerchantProfileRequest(
                 "audit-bar", "seed", List.of(), "筛选外商户", "bar", "be_1",
                 null, null, "Jane", "18800000011",
-                null, null, null, null, null, null, null, null, "active"));
+                null, null, null, null, null, null, null, null, "active", "org_1"));
 
         assertThat(service.listMerchants(false, " other ", 2, 1).items())
                 .extracting(MerchantRecord::id).containsExactly(second.id());
@@ -95,7 +95,7 @@ class PlatformServiceTests {
         assertThat(views.get(0).metadata())
                 .containsEntry("merchantIds", List.of(second.id()))
                 .containsEntry("entry", "/api/v1/platform/merchants")
-                .containsEntry("projectId", "org_1")
+                .containsEntry("projectId", "") // D-032：平台管理员查询不按项目限定
                 .containsEntry("includeArchived", false)
                 .containsEntry("businessFormat", "other")
                 .containsEntry("page", 2).containsEntry("pageSize", 1)
@@ -136,7 +136,7 @@ class PlatformServiceTests {
                 null,
                 null,
                 "active"
-        ))).isInstanceOfSatisfying(ApiException.class, exception ->
+        , "org_1"))).isInstanceOfSatisfying(ApiException.class, exception ->
                 assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
 
         assertThat(auditEventRepository.events)
@@ -151,7 +151,7 @@ class PlatformServiceTests {
         return service.createMerchant(new MerchantProfileRequest(
                 key, "seed", List.of(), "存档删除测试商户", "other", "be_1",
                 null, null, "Jane", "18800000011",
-                null, null, null, null, null, null, null, null, "active"));
+                null, null, null, null, null, null, null, null, "active", "org_1"));
     }
 
     /** M009-006：绑定引用的主体须真实存在——直接落一条主体行（不经服务，避免干扰幂等计数断言）。 */
@@ -207,12 +207,12 @@ class PlatformServiceTests {
     void createMerchantRejectsCooperationFormOutsideSixModes() {
         assertThatThrownBy(() -> service.createMerchant(new MerchantProfileRequest(
                 "coop-bad", "seed", List.of(), "合作方式错的商户", "other", null,
-                null, null, null, null, null, null, null, null, null, null, null, "fixed_rent_plus_commission", "active")))
+                null, null, null, null, null, null, null, null, null, null, null, "fixed_rent_plus_commission", "active", "org_1")))
                 .isInstanceOfSatisfying(ApiException.class, exception ->
                         assertThat(exception.code()).isEqualTo("MERCHANT_COOPERATION_FORM_INVALID"));
         MerchantRecord lease = service.createMerchant(new MerchantProfileRequest(
                 "coop-lease", "seed", List.of(), "租赁商户", "other", null,
-                null, null, null, null, null, null, null, null, null, null, null, "lease", "active"));
+                null, null, null, null, null, null, null, null, null, null, null, "lease", "active", "org_1"));
         assertThat(lease.cooperationForm()).isEqualTo("lease");
         assertThat(seedMerchant("coop-null").cooperationForm()).isNull();
     }
@@ -222,12 +222,12 @@ class PlatformServiceTests {
     void createMerchantRejectsStatusOutsideEnumeratedStates() {
         assertThatThrownBy(() -> service.createMerchant(new MerchantProfileRequest(
                 "status-bad", "seed", List.of(), "状态错的商户", "other", null,
-                null, null, null, null, null, null, null, null, null, null, null, null, "draft")))
+                null, null, null, null, null, null, null, null, null, null, null, null, "draft", "org_1")))
                 .isInstanceOfSatisfying(ApiException.class, exception ->
                         assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
         assertThat(service.createMerchant(new MerchantProfileRequest(
                 "status-pending", "seed", List.of(), "待准入商户", "other", null,
-                null, null, null, null, null, null, null, null, null, null, null, null, "pending_admission")).status())
+                null, null, null, null, null, null, null, null, null, null, null, null, "pending_admission", "org_1")).status())
                 .isEqualTo("pending_admission");
     }
 
@@ -248,7 +248,7 @@ class PlatformServiceTests {
     @Test
     void createBusinessEntityRejectsRoleOutsideWhitelist() {
         assertThatThrownBy(() -> service.createBusinessEntity(new BusinessEntityRequest(
-                "entity-spv", "create", List.of(), "SPV 主体", "spv", null, null, null, "active")))
+                "entity-spv", "create", List.of(), "SPV 主体", "spv", null, null, null, "active", "org_1")))
                 .isInstanceOfSatisfying(ApiException.class, exception ->
                         assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
     }
@@ -504,7 +504,7 @@ class PlatformServiceTests {
         assertThatThrownBy(() -> service.createMerchant(new MerchantProfileRequest(
                 "bad-format", "seed", List.of(), "业态错的商户", "tenant", "be_1",
                 null, null, "Jane", "18800000011",
-                null, null, null, null, null, null, null, null, "active")))
+                null, null, null, null, null, null, null, null, "active", "org_1")))
                 .isInstanceOfSatisfying(ApiException.class, exception ->
                         assertThat(exception.code()).isEqualTo("MERCHANT_BUSINESS_FORMAT_INVALID"));
     }
@@ -516,7 +516,7 @@ class PlatformServiceTests {
         service.createMerchant(new MerchantProfileRequest(
                 "seed-bar", "seed", List.of(), "话梅酒馆", "bar", "be_1",
                 null, null, "Jane", "18800000011",
-                null, null, null, null, null, null, null, null, "active"));
+                null, null, null, null, null, null, null, null, "active", "org_1"));
         MerchantRecord other = seedMerchant("seed-other");
 
         assertThat(service.listMerchants(true, "bar", 1, 50).items())
@@ -639,7 +639,8 @@ class PlatformServiceTests {
                 "91310000MA1P0",
                 "Jane Ops",
                 "vault://entity/bank",
-                "active"
+                "active",
+                "org_1"
         );
 
         BusinessEntityRecord first = service.createBusinessEntity(request);
@@ -844,7 +845,7 @@ class PlatformServiceTests {
                 null,
                 null,
                 "active"
-        ));
+        , "org_1"));
         BusinessEntityRecord e2 = service.createBusinessEntity(new BusinessEntityRequest(
                 "corp_A",
                 "entity corp_A",
@@ -855,7 +856,7 @@ class PlatformServiceTests {
                 null,
                 null,
                 "active"
-        ));
+        , "org_1"));
 
         assertThat(e1.id()).isNotEqualTo(e2.id());
         assertThat(e1.entityName()).isEqualTo("Corp A"); // 第一条未被覆盖
@@ -894,7 +895,7 @@ class PlatformServiceTests {
     private MerchantRecord unboundMerchant(String key) {
         return service.createMerchant(new MerchantProfileRequest(
                 key, "seed", List.of(), "待绑定商户", "other", null, null, null,
-                "Jane", "18800000011", null, null, null, null, null, null, null, null, "active"));
+                "Jane", "18800000011", null, null, null, null, null, null, null, null, "active", "org_1"));
     }
 
     @Test
@@ -1058,7 +1059,7 @@ class PlatformServiceTests {
     private MerchantProfileRequest numericMerchant(String key, String status,
             BigDecimal area, BigDecimal leasedArea, BigDecimal rent) {
         return new MerchantProfileRequest(key, null, List.of(), "数值验证商户", "other",
-                null, null, null, null, null, null, null, area, leasedArea, null, null, rent, null, status);
+                null, null, null, null, null, null, null, area, leasedArea, null, null, rent, null, status, "org_1");
     }
 
     @Test
@@ -1093,7 +1094,7 @@ class PlatformServiceTests {
     @Test
     void userFacingRoleValidationDoesNotIncludeEnglishEnumNames() {
         assertThatThrownBy(() -> service.createBusinessEntity(new BusinessEntityRequest(
-                "invalid-role-message", null, List.of(), "公司", "unknown", null, null, null, "active")))
+                "invalid-role-message", null, List.of(), "公司", "unknown", null, null, null, "active", "org_1")))
                 .isInstanceOfSatisfying(ApiException.class, e -> {
                     assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                     assertThat(e.getMessage()).doesNotContain("platform", "project_company", "management_company");
@@ -1141,6 +1142,9 @@ class PlatformServiceTests {
     @Test
     void projectListsAndTotalsStayWithinCurrentProjectForOrdinaryAdmin() {
         twoProjectFixtures();
+        // D-032：平台管理员不受当前项目限定；本用例守的是普通项目岗位仍只见本项目（M009-038）。
+        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
+                new AccessActor("employee-a", "employee-a", "employee", "org_1", null, "active"), "token", List.of()));
         org.junit.jupiter.api.Assertions.assertAll(
                 () -> { var p = service.listProjects(1, 1); assertThat(p.total()).isEqualTo(1);
                     assertThat(p.items()).extracting(ProjectRecord::id).containsExactly("org_1"); },
@@ -1172,6 +1176,9 @@ class PlatformServiceTests {
     @Test
     void spaceRequiresCurrentProjectAndNonnegativeArea() {
         twoProjectFixtures();
+        // D-032：平台管理员不受当前项目限定；「须在当前项目」守的是普通项目岗位（M009-038）。
+        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
+                new AccessActor("employee-a", "employee-a", "employee", "org_1", null, "active"), "token", List.of()));
         org.junit.jupiter.api.Assertions.assertAll(
                 () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                         "missing-project", null, List.of(), null, null, null, "空间", "shop",
@@ -1261,9 +1268,6 @@ class PlatformServiceTests {
                 new ProjectAssociationRequest("ordinary-link", "org_1", "普通认领"))).isInstanceOf(ApiException.class);
         assertThatThrownBy(() -> service.assignMerchantProject("scoped-merchant-b",
                 new ProjectAssociationRequest("ordinary-assign", "org_1", "普通认领"))).isInstanceOf(ApiException.class);
-        assertThatThrownBy(() -> service.createBusinessEntity(new BusinessEntityRequest(
-                "ordinary-company-foreign", null, List.of(), "其它项目", "management_company", null, null, null, "active", "other-project")))
-                .isInstanceOf(ApiException.class);
     }
 
     @Test
@@ -1481,4 +1485,16 @@ class PlatformServiceTests {
 
 
     }
+
+    // D-032（红线④不约束平台管理员；BSM-007、PGM-041「只有项目之上的获授权岗位才可以跨项目汇总」）：平台管理员列表不按当前项目截断。
+    @Test
+    void platformAdminListsAndTotalsCoverEveryProject() {
+        twoProjectFixtures();
+        org.junit.jupiter.api.Assertions.assertAll(
+                () -> assertThat(service.listProjects(1, 10).total()).isEqualTo(2),
+                () -> assertThat(service.listSpaces(1, 10).total()).isEqualTo(2),
+                () -> assertThat(service.listMerchants(true, null, 1, 10).total()).isEqualTo(2),
+                () -> assertThat(service.listBusinessEntities(1, 10).total()).isEqualTo(2),
+                () -> assertThat(service.listBusinessEntityBindings(1, 10).total()).isEqualTo(2));
+    }
 }
diff --git a/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java b/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
index 8c29f53a..43e24e9c 100644
--- a/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
@@ -435,14 +435,14 @@ class SupervisorReviewServiceTests {
         assertThat(svc.listPending(200).items()).extracting(p -> p.decision().reviewId()).containsExactly(a.reviewId());
         TestSecurityActors.asSupervisor("usr_sup_b", 1); // org_1 账号，但是 b 单当前节点审批人：可见
         assertThat(svc.listPending(200).items()).extracting(p -> p.decision().reviewId()).containsExactlyInAnyOrder(a.reviewId(), b.reviewId());
-        TestSecurityActors.asPlatformAdmin("usr_admin_all");
-        assertThat(svc.listPending(200).items()).extracting(p -> p.decision().reviewId()).containsExactly(a.reviewId());
+        TestSecurityActors.asPlatformAdmin("usr_admin_all"); // D-032：平台管理员跨范围整表可见
+        assertThat(svc.listPending(200).items()).extracting(p -> p.decision().reviewId()).containsExactlyInAnyOrder(a.reviewId(), b.reviewId());
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                                 com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"),
-                        "token", List.of()));
-        assertThat(svc.listPending(200).items()).extracting(p -> p.decision().reviewId()).containsExactlyInAnyOrder(a.reviewId(), b.reviewId());
+                        "token", List.of())); // XM-D029-002：最高权限不整表，按普通可见规则只见本项目 org_1 的单
+        assertThat(svc.listPending(200).items()).extracting(p -> p.decision().reviewId()).containsExactly(a.reviewId());
         assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("governance.supervisor_review.pending_listed_cross_scope");
     }
 
@@ -759,8 +759,8 @@ class SupervisorReviewServiceTests {
         reviewRepository.superiors.put("usr_esc_init", "usr_reviewer_a");
         reviewRepository.superiors.put("usr_reviewer_a", "usr_reviewer_c");
         TestSecurityActors.asSupervisor("usr_esc_init", 1);
-        var pending = service.blockForAdmission("permission_grant_l2", "access.grant_request.money", "access_grant_request",
-                "agr_esc", "two_levels_up", 2, "usr_esc_benef");
+        var pending = service.blockForAdmission("permission_grant_l3", "access.grant_request.money", "access_grant_request",
+                "agr_esc", "top_authority", 3, "usr_esc_benef"); // XM-D029-005：资金类不再自动升档，本例显式走最高权限档
         TestSecurityActors.asSupervisor("usr_reviewer_a", 3);
         assertThat(service.forward(pending.reviewId(), new SupervisorReviewForwardRequest(
                 "esc-forward-below-top", null, "usr_anyone", "handle")).forwardedTo()).isEqualTo("usr_anyone");
@@ -875,11 +875,7 @@ class SupervisorReviewServiceTests {
         var normalDue = reviewRepository.schedules.get(normal.reviewId()).dueAt();
         assertThat(moneyDue).isAfter(java.time.Instant.now());
         assertThat(normalDue).isAfter(moneyDue);
-        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
-                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
-                        new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"),
-                        "token", List.of())); // 红线④：只有密钥主体可整批看待审
+        TestSecurityActors.asPlatformAdmin(); // D-032：平台管理员整表看待审（最高权限不整表，XM-D029-002）
         assertThat(service.listPending(10).items()).extracting(item -> item.decision().reviewId())
                 .contains(money.reviewId(), normal.reviewId());
     }
@@ -1232,4 +1228,76 @@ class SupervisorReviewServiceTests {
             return false;
         }
     }
+
+    // XM-D029-002（治理地图第 528 行第④类「持授权 U 盘密钥的最高权限可以逐笔开放，每次只对当前这一笔生效并留痕」）：
+    // 最高权限的待审清单不整表；按编号读取别的项目的复核单时逐笔放行并留痕。
+    @Test
+    void masterOpensAnotherProjectsReviewOnlyOneByOneAndLeavesATrace() {
+        com.complex.platform.access.AccessRepository repo = levelAwareRepository(Map.of("usr_far_init", 3), null);
+        org.mockito.Mockito.when(repo.findUserById("usr_far_init")).thenReturn(Optional.of(new com.complex.platform.access.AccessUser(
+                "usr_far_init", "far", "staff", "org_2", null, "active", java.time.Instant.parse("2026-06-14T00:00:00Z"))));
+        SupervisorReviewService svc = new SupervisorReviewService(
+                new AuthorizationService(repo),
+                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
+                reviewRepository,
+                new AuditEventService(auditEventRepository), publishedEvents::add);
+        reviewRepository.superiors.put("usr_far_init", "usr_far_sup");
+        TestSecurityActors.asSupervisor("usr_far_init", 1);
+        var far = svc.evaluate(evaluate("scope-far", "v", List.of(), "contract_violation", "contract", "c_far", 0L, "violation"));
+        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
+                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
+                        new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"),
+                        "token", List.of()));
+        assertThat(svc.listPending(200).items()).extracting(p -> p.decision().reviewId()).doesNotContain(far.reviewId());
+        assertThat(svc.findReview(far.reviewId())).isPresent();
+        assertThat(auditEventRepository.events).filteredOn(e -> "governance.supervisor_review.viewed_cross_scope".equals(e.eventName()))
+                .extracting(AuditEvent::businessId).containsExactly(far.reviewId());
+    }
+
+    // XM-D029-003（丁案）：准入闸六参路径拿不到收款人，免单复核单照常建、审批不因缺受益人被拒；授权类仍强制受益人、被授权人本人仍不能审批。
+    @Test
+    void admissionWaiverWithoutBeneficiaryIsCreatedAndNotRejectedForMissingBeneficiary() {
+        chainL1();
+        TestSecurityActors.asSupervisorLevel(1);
+        var pending = service.blockForAdmission("bar_bill_waiver_block", "bar.bill_waiver", "bar_bill", "b_walk_in", "one_level_up", 2);
+        assertThat(pending.reviewStatus()).isEqualTo("pending_review");
+        TestSecurityActors.asSupervisor("usr_reviewer_a", 3);
+        org.assertj.core.api.Assertions.assertThatCode(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
+                "waiver-no-payee", "note", List.of("att"), 3, "approved", "同意"))).doesNotThrowAnyException();
+    }
+
+    @Test
+    void grantRequestWithoutBeneficiaryIsStillRejected() {
+        chainL1();
+        TestSecurityActors.asSupervisorLevel(1);
+        assertThatThrownBy(() -> service.blockForAdmission("permission_grant_l1", "access.grant_request", "access_grant_request",
+                "agr_no_benef", "one_level_up", 1))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("BENEFICIARY_REQUIRED"));
+    }
+
+    @Test
+    void granteeCannotApproveTheirOwnGrantRequest() {
+        chainL1();
+        TestSecurityActors.asSupervisorLevel(1);
+        var pending = service.blockForAdmission("permission_grant_l1", "access.grant_request", "access_grant_request",
+                "agr_self", "one_level_up", 1, "usr_grantee_self");
+        TestSecurityActors.asSupervisor("usr_grantee_self", 3);
+        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
+                "grant-self", "note", List.of("att"), 3, "approved", "self")))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
+    }
+
+    // XM-D029-005（PGM-041「持股分红……由最高权限审批」；其余高危授权按可配政策与指定最终审批人的链）：
+    // 资金类授权申请保留事项策略的档位，只有分红类升最高权限档。
+    @Test
+    void moneyGrantKeepsItsPolicyLevelWhileDividendGoesToTopAuthority() {
+        TestSecurityActors.asSupervisorLevel(1);
+        var money = service.blockForAdmission("permission_grant_l1", "access.grant_request.money", "access_grant_request",
+                "agr_money_level", "one_level_up", 1, "usr_money_grantee");
+        assertThat(money.policyCode()).isEqualTo("permission_grant_l1");
+        var dividend = service.blockForAdmission("permission_grant_l1", "finance.dividend.release", "dividend_statement",
+                "div_level", "one_level_up", 1, "usr_dividend_payee");
+        assertThat(dividend.policyCode()).isEqualTo("permission_grant_l3");
+    }
 }
```

## 4. 可执行证据原文（/home/james/audit-tmp/xmd029_b3_run.log）

```
START=2026-09-23T23:27:54Z HEAD=e456ba96 origin/main=4aa318a5
scratch at 4aa318a5
== RED（只落测试补丁）
MVN_EXIT(red)=1
  red: suites=263 tests=3532 red=343
  RED 相对原样基线（xm036a_base.red）新增红：
    NEW-RED: AccessJdbcIntegrationTests.consumingOneCodeOfASingleUseGrantGroupLogsEveryConsumedCode
    NEW-RED: AuthorizationServiceTests.currentMerchantIdQuietlyReturnsEmptyForPlatformAdminEvenWhenSeedBoundMerchantId
    NEW-RED: AuthorizationServiceTests.currentMerchantScopeReturnsEmptyForPlatformAdmin
    NEW-RED: AuthServiceTests.webSessionRefreshesWithoutDeviceButDeviceBoundSessionNeedsTheSameDevice
    NEW-RED: PlatformServiceTests.merchantViewAuditRecordsOnlyReturnedPageAndQueryContext
    NEW-RED: PlatformServiceTests.platformAdminListsAndTotalsCoverEveryProject
    NEW-RED: SupervisorReviewServiceTests.admissionWaiverWithoutBeneficiaryIsCreatedAndNotRejectedForMissingBeneficiary
    NEW-RED: SupervisorReviewServiceTests.deadlineIsShorterForMoneyActions
    NEW-RED: SupervisorReviewServiceTests.masterOpensAnotherProjectsReviewOnlyOneByOneAndLeavesATrace
    NEW-RED: SupervisorReviewServiceTests.moneyGrantKeepsItsPolicyLevelWhileDividendGoesToTopAuthority
    NEW-RED: SupervisorReviewServiceTests.pendingListIsScopedToTheActorsProjectUnlessCurrentApproverOrPlatformAdmin
== GREEN（测试＋生产补丁）
MVN_EXIT(green)=1
  green: suites=263 tests=3532 red=23
  GREEN 相对 XM-M036-A 预期残留（旧 20＋FieldLabels 1＋StaffProfile 2）：
  scratch dirty after cleanup: 0
  scratch removed
== 主工作区落补丁
before tree=ce088c52fb3031d1ae65380a2dd10d261cc3c1ca
after tree=74bb605e734ca7111c6a88873923e3e044f2deeb
frozen diff: 65541 bytes sha256 1fa24e3a4dcd8e8dc2cc700d4c7801f1c18a92382e8b29e925c5e6cc19ed8eb4
 .../platform/access/AuthorizationService.java      | 22 ++++--
 .../platform/access/JdbcAccessRepository.java      | 58 +++++++-------
 .../com/complex/platform/auth/AuthService.java     |  6 +-
 .../platform/auth/ProductionAuthService.java       |  6 +-
 .../platform/governance/PlatformService.java       | 58 +++++++++++---
 .../governance/review/SupervisorReviewService.java | 49 ++++++++----
 .../access/AccessJdbcIntegrationTests.java         | 33 ++++++++
 .../platform/access/AccessServiceTests.java        | 12 +++
 .../platform/access/AuthorizationServiceTests.java | 12 ++-
 .../access/M006SecurityRegressionTests.java        |  3 +
 .../complex/platform/auth/AuthServiceTests.java    | 14 ++++
 .../WecomRiskHitsEndpointReproTests.java           | 17 ++++
 .../platform/governance/PlatformServiceTests.java  | 56 +++++++++-----
 .../review/SupervisorReviewServiceTests.java       | 90 +++++++++++++++++++---
 14 files changed, 336 insertions(+), 100 deletions(-)
主工作区本轮 diff 与一次性 worktree 里验证过的 diff 是否逐字节相同：same
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-23T23:29:44Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
