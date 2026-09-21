CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M032 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T06:21:47Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M032 B3-r1 修复 diff：before tree `03a43dc2d1ca63b174779b5284ed195877df5329` → after tree `2418fd96bd71264a3e84bd137cd8d183e9d976d0`；冻结 diff `/home/james/audit-tmp/m032_b3r1.diff`（9201 bytes，SHA256 e5102bd28a6dad49cb599ce73706a4a99443d9ddc55d3a7559dbdc82d521d052），全文见 §2。定稿 ID：M032-001、002（队列「是」）。
- 顺序：Grok 已先派（modules/M032-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐行归因（§5 闸 3）

| 文件 | ± | 归因 ID | 行首 64 字 |
|---|---|---|---|
| AccessGrantPolicy.java | `-` | 001 | // 文件版本：V0.01 |
| AccessGrantPolicy.java | `+` | 001 | // 文件版本：V0.02 |
| AccessGrantPolicy.java | `-` | 001 | /** M006-016：沿用主管复核策略表保存授权政策；查询不能覆盖政策值。 */ |
| AccessGrantPolicy.java | `+` | 001 | /** |
| AccessGrantPolicy.java | `+` | 001 |  * M006-016：沿用主管复核策略表保存授权政策；查询不能覆盖政策值。 |
| AccessGrantPolicy.java | `+` | 001 |  * M032-001：库内该行自 V389 起带审批通用时限 {@code deadlineWorkingDays}；记录类 |
| AccessGrantPolicy.java | `+` | 001 |  * 未跑 V389 的库没有该键故可为空；为空时不输出，保存走合并写，库内原值保留。 |
| AccessGrantPolicy.java | `+` | 001 |  */ |
| AccessGrantPolicy.java | `-` | 001 |                                 BoundedInt userThreshold, Map<S |
| AccessGrantPolicy.java | `+` | 001 |                                 BoundedInt userThreshold, Map<S |
| AccessGrantPolicy.java | `+` | 001 |                                 @com.fasterxml.jackson.annotati |
| AccessGrantPolicy.java | `+` | 001 |                                 BoundedInt deadlineWorkingDays) |
| AccessGrantPolicy.java | `+` | 001 |     public AccessGrantPolicy(BoundedInt windowDays, BoundedInt  |
| AccessGrantPolicy.java | `+` | 001 |                              BoundedInt userThreshold, Map<Stri |
| AccessGrantPolicy.java | `+` | 001 |         this(windowDays, positionThreshold, userThreshold, risk |
| AccessGrantPolicy.java | `+` | 001 |     } |
| AccessGrantPolicy.java | `+` | 001 |  |
| EffectivePermissions.java | `-` | 002 |  * <p>{@code wildcard=true} 仅当 actor 命中平台管理员桥（status=active 且 a |
| EffectivePermissions.java | `-` | 002 |  * 但前端/后端仍须让 {@code denies} 优先于 wildcard。 |
| EffectivePermissions.java | `+` | 002 |  * <p>{@code wildcard} 现恒为 {@code false}：平台管理员不再是通配，其日常功能码已直接并入 |
| EffectivePermissions.java | `+` | 002 |  * （见 {@link AuthorizationService#effectivePermissions}）；{@code |
| JdbcAccessRepository.java | `-` | 001 | // 文件版本：V0.12 |
| JdbcAccessRepository.java | `+` | 001 | // 文件版本：V0.13 |
| JdbcAccessRepository.java | `-` | 001 |                 UPDATE supervisor_review_policies SET parameter |
| JdbcAccessRepository.java | `+` | 001 |                 UPDATE supervisor_review_policies SET parameter |
| AccessJdbcIntegrationTests.java | `+` | 001 |     // M032-001（治理地图「全平台审批通用时限」为后台业务参数）：库内现行授权政策行（V389 起带 deadl |
| AccessJdbcIntegrationTests.java | `+` | 001 |     // 必须能按服务层同一解析路径读出；保存一份不带该键的政策后，库内该键保持原值。 |
| AccessJdbcIntegrationTests.java | `+` | 001 |     @Test |
| AccessJdbcIntegrationTests.java | `+` | 001 |     void storedGrantPolicyParsesAndSavingKeepsDeadlineWorkingDa |
| AccessJdbcIntegrationTests.java | `+` | 001 |         var mapper = new com.fasterxml.jackson.databind.ObjectM |
| AccessJdbcIntegrationTests.java | `+` | 001 |         String stored = repository.findGrantPolicy().orElseThro |
| AccessJdbcIntegrationTests.java | `+` | 001 |         int days = mapper.readTree(stored).path("deadlineWorkin |
| AccessJdbcIntegrationTests.java | `+` | 001 |         assertThat(days).isPositive(); |
| AccessJdbcIntegrationTests.java | `+` | 001 |  |
| AccessJdbcIntegrationTests.java | `+` | 001 |         AccessGrantPolicy current = mapper.readValue(stored, Ac |
| AccessJdbcIntegrationTests.java | `+` | 001 |  |
| AccessJdbcIntegrationTests.java | `+` | 001 |         repository.saveGrantPolicy(mapper.writeValueAsString(ne |
| AccessJdbcIntegrationTests.java | `+` | 001 |                 current.windowDays(), current.positionThreshold |
| AccessJdbcIntegrationTests.java | `+` | 001 |         assertThat(jdbcTemplate.queryForObject( |
| AccessJdbcIntegrationTests.java | `+` | 001 |                 "SELECT (parameters -> 'deadlineWorkingDays' -> |
| AccessJdbcIntegrationTests.java | `+` | 001 |                         + " WHERE policy_code = 'access_grant'" |
| AccessJdbcIntegrationTests.java | `+` | 001 |         assertThat(mapper.readValue(repository.findGrantPolicy( |
| AccessJdbcIntegrationTests.java | `+` | 001 |                 .isEqualTo(current.windowDays().value()); |
| AccessJdbcIntegrationTests.java | `+` | 001 |     } |
| AccessJdbcIntegrationTests.java | `+` | 001 |  |
| AccessServiceTests.java | `-` | 001 | // 文件版本：V0.07 |
| AccessServiceTests.java | `+` | 001 | // 文件版本：V0.08 |
| AccessServiceTests.java | `-` | 001 |         String policy = "{\"windowDays\":{\"value\":30,\"min\": |
| AccessServiceTests.java | `+` | 001 |         String policy = "{\"windowDays\":{\"value\":30,\"min\": |

- 5 文件 8 个 hunk、49 条 ± 行。EffectivePermissions.java 的注释两行归 002；其余四个文件（记录类第 5 组件与便捷构造器及版本行、`saveGrantPolicy` 一条 SQL 及版本行、真库回归一条、内存政策 JSON 补键及版本行）归 001。无归因不到的行；无 mode 变更；AuthorizationService、Jdbc 的未发布改动、M031 三个文件、三份 HTML、锁未动。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java b/backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java
index 6adf0144..d9f4d3d2 100644
--- a/backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java
+++ b/backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java
@@ -1,13 +1,19 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.01
+// 文件版本：V0.02
 import com.complex.platform.common.api.ApiException;
 import java.util.Map;
 import java.util.Set;
 
-/** M006-016：沿用主管复核策略表保存授权政策；查询不能覆盖政策值。 */
+/**
+ * M006-016：沿用主管复核策略表保存授权政策；查询不能覆盖政策值。
+ * M032-001：库内该行自 V389 起带审批通用时限 {@code deadlineWorkingDays}；记录类须认得它（否则读不出整份政策），
+ * 未跑 V389 的库没有该键故可为空；为空时不输出，保存走合并写，库内原值保留。
+ */
 public record AccessGrantPolicy(BoundedInt windowDays, BoundedInt positionThreshold,
-                                BoundedInt userThreshold, Map<String, ReviewRule> riskReviews) {
+                                BoundedInt userThreshold, Map<String, ReviewRule> riskReviews,
+                                @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
+                                BoundedInt deadlineWorkingDays) {
     public AccessGrantPolicy {
         if (windowDays == null || positionThreshold == null || userThreshold == null
                 || riskReviews == null || !riskReviews.keySet().equals(Set.of("low", "medium", "high", "critical"))
@@ -17,6 +23,11 @@ public record AccessGrantPolicy(BoundedInt windowDays, BoundedInt positionThresh
         riskReviews = Map.copyOf(riskReviews);
     }
 
+    public AccessGrantPolicy(BoundedInt windowDays, BoundedInt positionThreshold,
+                             BoundedInt userThreshold, Map<String, ReviewRule> riskReviews) {
+        this(windowDays, positionThreshold, userThreshold, riskReviews, null);
+    }
+
     public record BoundedInt(int value, int min, int max) {
         public BoundedInt {
             if (min < 1 || max < min || value < min || value > max) {
diff --git a/backend/src/main/java/com/complex/platform/access/EffectivePermissions.java b/backend/src/main/java/com/complex/platform/access/EffectivePermissions.java
index 382629b9..a5e210a2 100644
--- a/backend/src/main/java/com/complex/platform/access/EffectivePermissions.java
+++ b/backend/src/main/java/com/complex/platform/access/EffectivePermissions.java
@@ -10,8 +10,8 @@ import java.util.List;
  * DENY 优先级最高。{@code grants} 已剔除被 DENY 覆盖的权限码（实现 A），同时仍单独返回 {@code denies}
  * 供前端展示冲突来源；前端 {@code hasPermissions} 对是否预扣两种形态判定结果一致。
  *
- * <p>{@code wildcard=true} 仅当 actor 命中平台管理员桥（status=active 且 accountType=platform_admin），
- * 但前端/后端仍须让 {@code denies} 优先于 wildcard。
+ * <p>{@code wildcard} 现恒为 {@code false}：平台管理员不再是通配，其日常功能码已直接并入 {@code grants}
+ * （见 {@link AuthorizationService#effectivePermissions}）；{@code denies} 仍优先。字段保留是为不改接口形状。
  */
 public record EffectivePermissions(
         String userId,
diff --git a/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java b/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
index 680fda30..837b04aa 100644
--- a/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
+++ b/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
@@ -1,5 +1,5 @@
 package com.complex.platform.access;
-// 文件版本：V0.12
+// 文件版本：V0.13
 import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
@@ -776,7 +776,7 @@ class JdbcAccessRepository implements AccessRepository {
     @Override
     public void saveGrantPolicy(String policyJson) {
         int changed = jdbcTemplate.update("""
-                UPDATE supervisor_review_policies SET parameters = CAST(:parameters AS jsonb), updated_at = now()
+                UPDATE supervisor_review_policies SET parameters = parameters || CAST(:parameters AS jsonb), updated_at = now()
                  WHERE policy_code = 'access_grant' AND enabled = true
                 """, Map.of("parameters", policyJson));
         if (changed != 1) throw new IllegalStateException("授权政策未配置");
diff --git a/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
index cb23d668..0dbc4fec 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
@@ -624,6 +624,26 @@ class AccessJdbcIntegrationTests {
                 .containsEntry("result", "expired");
     }
 
+    // M032-001（治理地图「全平台审批通用时限」为后台业务参数）：库内现行授权政策行（V389 起带 deadlineWorkingDays）
+    // 必须能按服务层同一解析路径读出；保存一份不带该键的政策后，库内该键保持原值。
+    @Test
+    void storedGrantPolicyParsesAndSavingKeepsDeadlineWorkingDays() throws Exception {
+        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
+        String stored = repository.findGrantPolicy().orElseThrow();
+        int days = mapper.readTree(stored).path("deadlineWorkingDays").path("value").asInt();
+        assertThat(days).isPositive();
+
+        AccessGrantPolicy current = mapper.readValue(stored, AccessGrantPolicy.class);
+
+        repository.saveGrantPolicy(mapper.writeValueAsString(new AccessGrantPolicy(
+                current.windowDays(), current.positionThreshold(), current.userThreshold(), current.riskReviews())));
+        assertThat(jdbcTemplate.queryForObject(
+                "SELECT (parameters -> 'deadlineWorkingDays' ->> 'value')::int FROM supervisor_review_policies"
+                        + " WHERE policy_code = 'access_grant'", Integer.class)).isEqualTo(days);
+        assertThat(mapper.readValue(repository.findGrantPolicy().orElseThrow(), AccessGrantPolicy.class).windowDays().value())
+                .isEqualTo(current.windowDays().value());
+    }
+
     // M006-G-07（PGM-1.10.1「旧派生同步失效」）：已定岗改职级复用定岗的四步写入（职级、定岗、套模板、替换派生），
     // 入口必须与 assignUserPosition 一样由 Spring 事务整体包住，否则套模板已提交、替换派生失败时新旧派生并存。
     @Test
diff --git a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
index 1c34f772..da9feeb5 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.07
+// 文件版本：V0.08
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
@@ -1675,7 +1675,7 @@ class AccessServiceTests {
 
     private static final class InMemoryAccessRepository implements AccessRepository {
         final Map<String, PermissionDefinition> permissions = new LinkedHashMap<>();
-        String policy = "{\"windowDays\":{\"value\":30,\"min\":1,\"max\":2147483647},\"positionThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"userThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"riskReviews\":{\"low\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"medium\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"high\":{\"approvalLevel\":\"two_levels_up\",\"requiredLevel\":2},\"critical\":{\"approvalLevel\":\"top_authority\",\"requiredLevel\":3}}}";
+        String policy = "{\"windowDays\":{\"value\":30,\"min\":1,\"max\":2147483647},\"positionThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"userThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"riskReviews\":{\"low\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"medium\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"high\":{\"approvalLevel\":\"two_levels_up\",\"requiredLevel\":2},\"critical\":{\"approvalLevel\":\"top_authority\",\"requiredLevel\":3}},\"deadlineWorkingDays\":{\"value\":3,\"min\":1,\"max\":30}}";
         final Map<String, String> fingerprints = new LinkedHashMap<>();
         final Map<String, String> positionRoles = new LinkedHashMap<>();
         @Override public void lockGrantAuthorizationFacts() {} // 单线程内存夹具；真实锁在 M006JdbcRegressionTests 验证。
```

## 3. 内容审计

- 001 读：记录类认得 `deadlineWorkingDays` 后，库内现行政策行可被 `new ObjectMapper()` 读出（GREEN 真库回归）；缺键（未跑 V389 的库）得 null，不进 `BoundedInt` 校验，紧凑构造器的非空闭集未把它列入。写：为空不输出（`@JsonInclude(NON_NULL)`），`saveGrantPolicy` 用 jsonb `||` 合并，客户端没带的键保持库内原值（GREEN 真库回归断言保存不带该键的政策后库内值不变）。4 参便捷构造器保住 `M006JdbcRegressionTests.java:144` 的调用。对外 JSON 多一个可选字段，不破坏既有消费者。
- 002：只改两行类注释为现状；字段与计算未动。
- 发布变体：JdbcAccessRepository.java 在 origin/main 版上打同一处补丁（2 增 2 删），主工作区版的未发布改动不随本模块带出。
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m032_b3r1_run.log）

```
START=2026-09-21T06:18:55Z HEAD=e456ba96 origin/main=2c178f7a
BEFORE_TREE=03a43dc2d1ca63b174779b5284ed195877df5329
before AccessGrantPolicy.java 6f0de0f7052efa45430347547746379da6ec376235db46fe9e19f5017e087fb7 sameAsOriginMain=yes
before JdbcAccessRepository.java ecc78c0b65ecf3cf4154947c6160622b39eb881404bbb3501788839a74c9d86b sameAsOriginMain=no
before EffectivePermissions.java 68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e sameAsOriginMain=yes
before AccessServiceTests.java 077ce9e6b00430c7cc7d83abf95601b8bff665a57a95fb53f8458403e1d265f6 sameAsOriginMain=yes
before AccessJdbcIntegrationTests.java e81ba90973fd97220b259c3d448a5de87ca844d8cfbc964309f9f35872790a98 sameAsOriginMain=yes
scratch = origin/main 原样；为过 Flyway 校验拷入主工作区未发布迁移: V427__m009_project_profiles_and_associations.sql
== BASE（未打补丁）
base MVN_EXIT=0
   AccessControllerTests tests=13 errors=0 skipped=0 failures=0
   AccessJdbcIntegrationTests tests=24 errors=0 skipped=0 failures=0
   AccessServiceTests tests=54 errors=0 skipped=0 failures=0
   AuthorizationServiceTests tests=21 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=12 errors=0 skipped=0 failures=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
== RED（只打测试补丁）
patched AccessServiceTests.java 1 V0.08
patched AccessJdbcIntegrationTests.java 1 None
red MVN_EXIT=1
   AccessControllerTests tests=13 errors=0 skipped=0 failures=0
   AccessJdbcIntegrationTests tests=25 errors=1 skipped=0 failures=0
       error storedGrantPolicyParsesAndSavingKeepsDeadlineWorkingDays | Unrecognized field &quot;deadlineWorkingDays&quot; (class com.complex.platform.access.AccessGrantPolicy), not marked as ignorable (4 known properties:
   AccessServiceTests tests=54 errors=11 skipped=0 failures=0
       error listGrantRequestsFiltersRowsStampedWithOtherProjects | 后台授权政策格式或上下限无效
       error positionMappingAndAssignmentDoNotRequireTheConfigurerToHoldTheHeldHighRiskCodes | 后台授权政策格式或上下限无效
       error scopeChangeCancelsOldScopeHighRiskHoldsAndTheirDenyOverrides | 后台授权政策格式或上下限无效
       error assignUserPositionLandsNormalPartAndHoldsHighRiskPartUntilChainApproves | 后台授权政策格式或上下限无效
       error preExistingExplicitDenyIsNotReusedByTheHighRiskHold | 后台授权政策格式或上下限无效
       error moneyPermissionApplicationsAreMarkedForTheShorterDeadline | 后台授权政策格式或上下限无效
       error grantSummaryReportsFrequentTemporaryRequestsByPositionAndUser | 后台授权政策格式或上下限无效
       error approvedTemporaryGrantExpiresByEffectivePermissionRead | 后台授权政策格式或上下限无效
       error auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse | 后台授权政策格式或上下限无效
       error approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment | 后台授权政策格式或上下限无效
       error removingTheHighRiskHoldDenyRequiresApproval | 后台授权政策格式或上下限无效
   AuthorizationServiceTests tests=21 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=12 errors=0 skipped=0 failures=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
== GREEN（再打主代码补丁）
patched AccessGrantPolicy.java 2 V0.02
patched JdbcAccessRepository.java 1 V0.13
patched EffectivePermissions.java 1 None
green MVN_EXIT=0
   AccessControllerTests tests=13 errors=0 skipped=0 failures=0
   AccessJdbcIntegrationTests tests=25 errors=0 skipped=0 failures=0
   AccessServiceTests tests=54 errors=0 skipped=0 failures=0
   AuthorizationServiceTests tests=21 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=12 errors=0 skipped=0 failures=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
== 主工作区打同一补丁
patched AccessServiceTests.java 1 V0.08
patched AccessJdbcIntegrationTests.java 1 None
patched AccessGrantPolicy.java 2 V0.02
patched JdbcAccessRepository.java 1 V0.13
patched EffectivePermissions.java 1 None
same as scratch: AccessGrantPolicy.java
same as scratch: EffectivePermissions.java
same as scratch: AccessServiceTests.java
same as scratch: AccessJdbcIntegrationTests.java
publish variant saved 30faa630325fe784785b29ca37fd5eb4d5429f9215ca03f17ef9c6d1aec1ee5f; vs origin/main:  1 file changed, 2 insertions(+), 2 deletions(-)
== 主工作区（含未发布改动，仅供参考）
mainwt MVN_EXIT=1
   AccessControllerTests tests=13 errors=0 skipped=0 failures=0
   AccessJdbcIntegrationTests tests=25 errors=0 skipped=0 failures=0
   AccessServiceTests tests=54 errors=6 skipped=0 failures=0
       error assignUserPositionLandsNormalPartAndHoldsHighRiskPartUntilChainApproves | 退款、免单、授权与资金事项必须登记直接受益人
       error dailyTemporaryPermissionApplicationEntersSupervisorChain | 退款、免单、授权与资金事项必须登记直接受益人
       error approvedTemporaryGrantExpiresByEffectivePermissionRead | 退款、免单、授权与资金事项必须登记直接受益人
       error auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse | 退款、免单、授权与资金事项必须登记直接受益人
       error approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment | 退款、免单、授权与资金事项必须登记直接受益人
       error removingTheHighRiskHoldDenyRequiresApproval | 退款、免单、授权与资金事项必须登记直接受益人
   AuthorizationServiceTests tests=24 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=12 errors=1 skipped=0 failures=0
       error temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce | PreparedStatementCallback; bad SQL grammar [UPDATE temporary_permission_grants&#10;   SET consumed = TRUE,&#10;       consumed_at = ?&#10; WHERE grant
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
AFTER_TREE=2418fd96bd71264a3e84bd137cd8d183e9d976d0
DIFF bytes=9201 sha=e5102bd28a6dad49cb599ce73706a4a99443d9ddc55d3a7559dbdc82d521d052
 .../complex/platform/access/AccessGrantPolicy.java   | 17 ++++++++++++++---
 .../platform/access/EffectivePermissions.java        |  4 ++--
 .../platform/access/JdbcAccessRepository.java        |  4 ++--
 .../platform/access/AccessJdbcIntegrationTests.java  | 20 ++++++++++++++++++++
 .../complex/platform/access/AccessServiceTests.java  |  4 ++--
 5 files changed, 40 insertions(+), 9 deletions(-)
scratch removed
END=2026-09-21T06:20:00Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
