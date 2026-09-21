CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M036 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T10:54:48Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M036 B3-r1 修复 diff：before tree `b637f432404d6f402eede21dc4af52a6265a87fa` → after tree `0b38ca49734ed5f50c724214285b7119ecf0c809`；冻结 diff `/home/james/audit-tmp/m036_b3r1.diff`（11784 bytes，SHA256 1e3ca09807990735c264be0b4c7ee5da9c41671ee4bb9da4a984637504de6427），全文见 §2。定稿 ID：M036-001、002（队列「是」）。
- 顺序：Grok 已先派（modules/M036-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐行归因（§5 闸 3）

| 文件 | ± | 归因 ID | 行首 64 字 |
|---|---|---|---|
| AccessServiceTests.java | `-` | 001 | // 文件版本：V0.08 |
| AccessServiceTests.java | `+` | 001 | // 文件版本：V0.09 |
| AccessServiceTests.java | `-` | 001 |         for (String roleId : List.of("role_recompute_old", "rol |
| AccessServiceTests.java | `-` | 001 |             accessRepository.roles.put(roleId, new RoleRecord( |
| AccessServiceTests.java | `-` | 001 |                     roleId, roleId, "Ops checker", "project", " |
| AccessServiceTests.java | `+` | 001 |         // M036-001：旧角色、新角色、模板各带一个互不相同的码，有效权限集才分得清派生来自哪里。 |
| AccessServiceTests.java | `+` | 001 |         for (Map.Entry<String, String> role : Map.of( |
| AccessServiceTests.java | `+` | 001 |                 "role_recompute_old", "ops.production_readiness |
| AccessServiceTests.java | `+` | 001 |                 "role_recompute_new", "governance.supervisor_re |
| AccessServiceTests.java | `+` | 001 |             accessRepository.roles.put(role.getKey(), new RoleR |
| AccessServiceTests.java | `+` | 001 |                     role.getKey(), role.getKey(), "Ops checker" |
| AccessServiceTests.java | `-` | 001 |                     List.of("ops.production_readiness.check"), |
| AccessServiceTests.java | `+` | 001 |                     List.of(role.getValue()), |
| AccessServiceTests.java | `+` | 001 |         assertThat(authorizationService.effectivePermissionsFor |
| AccessServiceTests.java | `+` | 001 |                 .contains("ops.production_readiness.view", "ops |
| AccessServiceTests.java | `+` | 001 |         assertThat(authorizationService.effectivePermissionsFor |
| AccessServiceTests.java | `+` | 001 |                 .contains("governance.supervisor_review.view",  |
| AccessServiceTests.java | `+` | 001 |                 .doesNotContain("ops.production_readiness.view" |
| AccessServiceTests.java | `+` | 001 |         assertThat(accessRepository.templateApplications).filte |
| AccessServiceTests.java | `+` | 001 |                 .extracting(PermissionTemplateApplication::stat |
| AccessServiceTests.java | `+` | 001 |         assertThat(authorizationService.effectivePermissionsFor |
| AccessServiceTests.java | `+` | 001 |                 .doesNotContain("ops.production_readiness.view" |
| AccessServiceTests.java | `+` | 001 |                         "ops.production_readiness.check"); |
| AccessServiceTests.java | `+` | 001 |         assertThat(accessRepository.templateApplications).filte |
| AccessServiceTests.java | `+` | 001 |                 .extracting(PermissionTemplateApplication::stat |
| AccessServiceTests.java | `+` | 001 |         final Map<String, List<String>> positionApplicationIds  |
| AccessServiceTests.java | `+` | 001 |         // M036-001：与 JdbcAccessRepository 同语义——先撤上一次岗位派生的模板应用， |
| AccessServiceTests.java | `-` | 001 |                 List<String> applicationIds, String actorId, In |
| AccessServiceTests.java | `+` | 001 |                 List<String> applicationIds, String actorId, In |
| AccessServiceTests.java | `+` | 001 |             revokeTemplateApplications(userId, positionApplicat |
| AccessServiceTests.java | `+` | 001 |             positionRoles.put(userId, roleId); |
| AccessServiceTests.java | `+` | 001 |             positionApplicationIds.put(userId, List.copyOf(appl |
| AccessServiceTests.java | `+` | 001 |         } |
| AccessServiceTests.java | `-` | 001 |                             "app_" + userId, |
| AccessServiceTests.java | `+` | 001 |                             "app_" + userId + "_" + (templateAp |
| AccessServiceTests.java | `-` | 001 |             for (String roleId : userRoles.getOrDefault(userId, |
| AccessServiceTests.java | `+` | 001 |             // M036-001：与 JdbcAccessRepository 同语义——岗位派生角色与直接装配 |
| AccessServiceTests.java | `+` | 001 |             Set<String> roleIds = new LinkedHashSet<>(userRoles |
| AccessServiceTests.java | `+` | 001 |             if (positionRoles.get(userId) != null) { |
| AccessServiceTests.java | `+` | 001 |                 roleIds.add(positionRoles.get(userId)); |
| AccessServiceTests.java | `+` | 001 |             } |
| AccessServiceTests.java | `+` | 001 |             for (String roleId : roleIds) { |
| AuthorizationServiceTests.java | `-` | 002 | // 文件版本：V0.03 |
| AuthorizationServiceTests.java | `+` | 002 | // 文件版本：V0.04 |
| AuthorizationServiceTests.java | `+` | 002 |     // M036-002（治理地图「临时授权可设有效期或单次使用」）：生效时间未到的授予不放行，也不进有效权限集。 |
| AuthorizationServiceTests.java | `+` | 002 |     @Test |
| AuthorizationServiceTests.java | `+` | 002 |     void grantWhoseValidityHasNotStartedDoesNotAuthorize() { |
| AuthorizationServiceTests.java | `+` | 002 |         authenticate("usr_staff", "staff", "staff"); |
| AuthorizationServiceTests.java | `+` | 002 |         repository.addEffect(new AccessRepository.PermissionEff |
| AuthorizationServiceTests.java | `+` | 002 |                 "usr_staff", |
| AuthorizationServiceTests.java | `+` | 002 |                 "ops.production_readiness.manage", |
| AuthorizationServiceTests.java | `+` | 002 |                 AccessRepository.Effect.GRANT, |
| AuthorizationServiceTests.java | `+` | 002 |                 AccessRepository.Source.TEMPORARY_GRANT, |
| AuthorizationServiceTests.java | `+` | 002 |                 "temp_not_started", |
| AuthorizationServiceTests.java | `+` | 002 |                 Instant.parse("2099-01-01T00:00:00Z"), |
| AuthorizationServiceTests.java | `+` | 002 |                 Instant.parse("2099-01-02T00:00:00Z"), |
| AuthorizationServiceTests.java | `+` | 002 |                 false, |
| AuthorizationServiceTests.java | `+` | 002 |                 false |
| AuthorizationServiceTests.java | `+` | 002 |         )); |
| AuthorizationServiceTests.java | `+` | 002 |  |
| AuthorizationServiceTests.java | `+` | 002 |         assertPermissionDenied("ops.production_readiness.manage |
| AuthorizationServiceTests.java | `+` | 002 |         assertThat(service.effectivePermissions().grants()).doe |
| AuthorizationServiceTests.java | `+` | 002 |     } |
| AuthorizationServiceTests.java | `+` | 002 |  |
| M006JdbcRegressionTests.java | `-` | 001 | // 文件版本：V0.13 |
| M006JdbcRegressionTests.java | `+` | 001 | // 文件版本：V0.14 |
| M006JdbcRegressionTests.java | `+` | 001 |     // M036-001：岗位派生角色（user_position_assignments.assigned_role_ |
| M006JdbcRegressionTests.java | `+` | 001 |     // 岗位派生置空后不再返回。 |
| M006JdbcRegressionTests.java | `+` | 001 |     @Test void positionDerivedRoleFeedsPermissionEffectsUntilTh |
| M006JdbcRegressionTests.java | `+` | 001 |         for (String table : java.util.List.of("user_roles", "ro |
| M006JdbcRegressionTests.java | `+` | 001 |                 "permission_template_items", "user_permission_o |
| M006JdbcRegressionTests.java | `+` | 001 |             jdbc.execute("CREATE TEMP TABLE " + table + " (LIKE |
| M006JdbcRegressionTests.java | `+` | 001 |         } |
| M006JdbcRegressionTests.java | `+` | 001 |         jdbc.execute("ALTER TABLE pg_temp.permission_templates  |
| M006JdbcRegressionTests.java | `+` | 001 |         jdbc.execute("ALTER TABLE pg_temp.permission_template_a |
| M006JdbcRegressionTests.java | `+` | 001 |         jdbc.execute("ALTER TABLE pg_temp.temporary_permission_ |
| M006JdbcRegressionTests.java | `+` | 001 |         jdbc.update("INSERT INTO roles (id, role_name, data_sco |
| M006JdbcRegressionTests.java | `+` | 001 |         jdbc.update("INSERT INTO role_permissions (role_id, per |
| M006JdbcRegressionTests.java | `+` | 001 |         jdbc.update("INSERT INTO user_position_assignments (use |
| M006JdbcRegressionTests.java | `+` | 001 |  |
| M006JdbcRegressionTests.java | `+` | 001 |         assertThat(repository.findPermissionEffects("user", Ins |
| M006JdbcRegressionTests.java | `+` | 001 |                 .extracting(AccessRepository.PermissionEffect:: |
| M006JdbcRegressionTests.java | `+` | 001 |                         AccessRepository.PermissionEffect::effe |
| M006JdbcRegressionTests.java | `+` | 001 |                 .containsExactly(tuple("position.only.code", Ac |
| M006JdbcRegressionTests.java | `+` | 001 |  |
| M006JdbcRegressionTests.java | `+` | 001 |         repository.replacePositionGrants("user", null, null, ja |
| M006JdbcRegressionTests.java | `+` | 001 |         assertThat(repository.findPermissionEffects("user", Ins |
| M006JdbcRegressionTests.java | `+` | 001 |     } |
| M006JdbcRegressionTests.java | `+` | 001 |  |

- 3 文件 11 个 hunk、89 条 ± 行。AccessServiceTests.java（内存仓三处对齐＋`savingAPositionMapping…` 的夹具与断言）与 M006JdbcRegressionTests.java（新真库测试）全部归 001；AuthorizationServiceTests.java（新测试）归 002；三个文件的版本号行随各自 ID。无归因不到的行；无 mode 变更；被测代码、迁移、三份 HTML、锁未动。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
index da9feeb5..ebdf8cae 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.08
+// 文件版本：V0.09
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
@@ -1572,11 +1572,14 @@ class AccessServiceTests {
         accessRepository.users.put("usr_recompute", new AccessUser(
                 "usr_recompute", "recompute", "staff", "seed-org", null, "active",
                 Instant.parse("2026-06-14T00:00:00Z")));
-        for (String roleId : List.of("role_recompute_old", "role_recompute_new")) {
-            accessRepository.roles.put(roleId, new RoleRecord(
-                    roleId, roleId, "Ops checker", "project", "active",
+        // M036-001：旧角色、新角色、模板各带一个互不相同的码，有效权限集才分得清派生来自哪里。
+        for (Map.Entry<String, String> role : Map.of(
+                "role_recompute_old", "ops.production_readiness.view",
+                "role_recompute_new", "governance.supervisor_review.view").entrySet()) {
+            accessRepository.roles.put(role.getKey(), new RoleRecord(
+                    role.getKey(), role.getKey(), "Ops checker", "project", "active",
                     Instant.parse("2026-06-14T00:00:00Z"),
-                    List.of("ops.production_readiness.check"),
+                    List.of(role.getValue()),
                     null));
         }
         accessRepository.saveTemplate(new PermissionTemplate(
@@ -1598,12 +1601,19 @@ class AccessServiceTests {
                 "assign-recompute", null, "usr_recompute", "ops_recompute", "lvl_m1_lower",
                 500000L, "CNY", null));
         assertThat(accessRepository.positionRoles.get("usr_recompute")).isEqualTo("role_recompute_old");
+        assertThat(authorizationService.effectivePermissionsForUser("usr_recompute").grants())
+                .contains("ops.production_readiness.view", "ops.production_readiness.check");
 
         service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
                 "map-recompute-change", null, "ops_recompute", "lvl_m1_lower",
                 "role_recompute_new", "tpl_recompute", "active"));
 
         assertThat(accessRepository.positionRoles.get("usr_recompute")).isEqualTo("role_recompute_new");
+        assertThat(authorizationService.effectivePermissionsForUser("usr_recompute").grants())
+                .contains("governance.supervisor_review.view", "ops.production_readiness.check")
+                .doesNotContain("ops.production_readiness.view");
+        assertThat(accessRepository.templateApplications).filteredOn(a -> "usr_recompute".equals(a.targetId()))
+                .extracting(PermissionTemplateApplication::status).containsExactly("revoked", "active");
         assertThat(accessRepository.findStaffLevelAssignment("usr_recompute")).get()
                 .extracting(StaffLevelAssignment::baseSalaryAmountCent)
                 .isEqualTo(500000L);
@@ -1614,6 +1624,11 @@ class AccessServiceTests {
 
         assertThat(accessRepository.positionRoles).containsKey("usr_recompute");
         assertThat(accessRepository.positionRoles.get("usr_recompute")).isNull();
+        assertThat(authorizationService.effectivePermissionsForUser("usr_recompute").grants())
+                .doesNotContain("ops.production_readiness.view", "governance.supervisor_review.view",
+                        "ops.production_readiness.check");
+        assertThat(accessRepository.templateApplications).filteredOn(a -> "usr_recompute".equals(a.targetId()))
+                .extracting(PermissionTemplateApplication::status).containsOnly("revoked");
     }
 
     // M031-003（GEN-011）：岗位模板生命周期与授权政策两个写口失败也留痕。
@@ -1684,8 +1699,14 @@ class AccessServiceTests {
             if (fingerprints.putIfAbsent(id, fingerprint) != null) throw new IllegalStateException("fingerprint already set");
         }
         @Override public Optional<String> findGrantFingerprint(String id) { return Optional.ofNullable(fingerprints.get(id)); }
+        final Map<String, List<String>> positionApplicationIds = new LinkedHashMap<>();
+        // M036-001：与 JdbcAccessRepository 同语义——先撤上一次岗位派生的模板应用，再记新角色与新应用 id；独立模板应用不受影响。
         @Override public void replacePositionGrants(String userId, String roleId, String mappingId,
-                List<String> applicationIds, String actorId, Instant at) { positionRoles.put(userId, roleId); }
+                List<String> applicationIds, String actorId, Instant at) {
+            revokeTemplateApplications(userId, positionApplicationIds.getOrDefault(userId, List.of()), actorId, at);
+            positionRoles.put(userId, roleId);
+            positionApplicationIds.put(userId, List.copyOf(applicationIds));
+        }
         final Map<String, PermissionTemplate> templates = new LinkedHashMap<>();
         final Map<String, List<PermissionTemplateItem>> items = new LinkedHashMap<>();
         final Map<String, TemporaryGrant> temporaryGrants = new LinkedHashMap<>();
@@ -1925,7 +1946,7 @@ class AccessServiceTests {
         ) {
             List<PermissionTemplateApplication> applications = targetUserIds.stream()
                     .map(userId -> new PermissionTemplateApplication(
-                            "app_" + userId,
+                            "app_" + userId + "_" + (templateApplications.size() + 1),
                             templateId,
                             "user",
                             userId,
@@ -2016,7 +2037,12 @@ class AccessServiceTests {
                     effects.add(override);
                 }
             }
-            for (String roleId : userRoles.getOrDefault(userId, Set.of())) {
+            // M036-001：与 JdbcAccessRepository 同语义——岗位派生角色与直接装配的角色一样以 ROLE 来源并入授予。
+            Set<String> roleIds = new LinkedHashSet<>(userRoles.getOrDefault(userId, Set.of()));
+            if (positionRoles.get(userId) != null) {
+                roleIds.add(positionRoles.get(userId));
+            }
+            for (String roleId : roleIds) {
                 RoleRecord role = roles.get(roleId);
                 if (role == null || !"active".equals(role.status())) {
                     continue;
diff --git a/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java b/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
index 714c10a2..f3e040d0 100644
--- a/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.03
+// 文件版本：V0.04
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -363,6 +363,26 @@ class AuthorizationServiceTests {
         assertThat(service.effectivePermissions().grants()).doesNotContain("ops.production_readiness.view");
     }
 
+    // M036-002（治理地图「临时授权可设有效期或单次使用」）：生效时间未到的授予不放行，也不进有效权限集。
+    @Test
+    void grantWhoseValidityHasNotStartedDoesNotAuthorize() {
+        authenticate("usr_staff", "staff", "staff");
+        repository.addEffect(new AccessRepository.PermissionEffect(
+                "usr_staff",
+                "ops.production_readiness.manage",
+                AccessRepository.Effect.GRANT,
+                AccessRepository.Source.TEMPORARY_GRANT,
+                "temp_not_started",
+                Instant.parse("2099-01-01T00:00:00Z"),
+                Instant.parse("2099-01-02T00:00:00Z"),
+                false,
+                false
+        ));
+
+        assertPermissionDenied("ops.production_readiness.manage");
+        assertThat(service.effectivePermissions().grants()).doesNotContain("ops.production_readiness.manage");
+    }
+
     private AccessRepository.PermissionEffect effect(
             String userId,
             String permissionCode,
diff --git a/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
index 37c1310c..ece6ff7e 100644
--- a/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.13
+// 文件版本：V0.14
 import static org.assertj.core.api.Assertions.*;
 import static org.junit.jupiter.api.Assumptions.assumeTrue;
 
@@ -248,6 +248,29 @@ class M006JdbcRegressionTests {
         assertThatThrownBy(() -> repository.deletePositionRoleTemplate("mapping")).isInstanceOf(IllegalStateException.class);
     }
 
+    // M036-001：岗位派生角色（user_position_assignments.assigned_role_id）的权限码以 ROLE 来源并入有效权限；
+    // 岗位派生置空后不再返回。
+    @Test void positionDerivedRoleFeedsPermissionEffectsUntilThePositionGrantsAreCleared() {
+        for (String table : java.util.List.of("user_roles", "roles", "role_permissions", "user_position_assignments",
+                "permission_template_items", "user_permission_overrides")) {
+            jdbc.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL)");
+        }
+        jdbc.execute("ALTER TABLE pg_temp.permission_templates ADD COLUMN status varchar, ADD COLUMN valid_from timestamptz, ADD COLUMN valid_to timestamptz");
+        jdbc.execute("ALTER TABLE pg_temp.permission_template_applications ADD COLUMN target_type varchar, ADD COLUMN revoked_by varchar, ADD COLUMN revoked_at timestamptz");
+        jdbc.execute("ALTER TABLE pg_temp.temporary_permission_grants ADD COLUMN subject_type varchar");
+        jdbc.update("INSERT INTO roles (id, role_name, data_scope, status, created_at) VALUES ('position-role','岗位角色','project','active',now())");
+        jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES ('position-role','position.only.code')");
+        jdbc.update("INSERT INTO user_position_assignments (user_id,position_code,staff_level_id,organization_id,assigned_by,assigned_role_id) VALUES ('user','position','level','org','operator','position-role')");
+
+        assertThat(repository.findPermissionEffects("user", Instant.now()))
+                .extracting(AccessRepository.PermissionEffect::permissionCode, AccessRepository.PermissionEffect::source,
+                        AccessRepository.PermissionEffect::effect)
+                .containsExactly(tuple("position.only.code", AccessRepository.Source.ROLE, AccessRepository.Effect.GRANT));
+
+        repository.replacePositionGrants("user", null, null, java.util.List.of(), "operator", Instant.now());
+        assertThat(repository.findPermissionEffects("user", Instant.now())).isEmpty();
+    }
+
     private UUID temporaryGrant() {
         UUID id = UUID.randomUUID();
         jdbc.update("INSERT INTO temporary_permission_grants (id, subject_id, permission_code, single_use) VALUES (?, 'user', 'permission', true)", id);
```

### 2.1 AuthorizationServiceTests 发布变体与 origin/main 的差异（/home/james/audit-tmp/m036_b3r1_publish_authz.diff）

```diff
--- a/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java (origin/main)
+++ b/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java (发布变体)
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -332,6 +332,26 @@
         assertThat(service.effectivePermissions().grants()).doesNotContain("ops.production_readiness.view");
     }
 
+    // M036-002（治理地图「临时授权可设有效期或单次使用」）：生效时间未到的授予不放行，也不进有效权限集。
+    @Test
+    void grantWhoseValidityHasNotStartedDoesNotAuthorize() {
+        authenticate("usr_staff", "staff", "staff");
+        repository.addEffect(new AccessRepository.PermissionEffect(
+                "usr_staff",
+                "ops.production_readiness.manage",
+                AccessRepository.Effect.GRANT,
+                AccessRepository.Source.TEMPORARY_GRANT,
+                "temp_not_started",
+                Instant.parse("2099-01-01T00:00:00Z"),
+                Instant.parse("2099-01-02T00:00:00Z"),
+                false,
+                false
+        ));
+
+        assertPermissionDenied("ops.production_readiness.manage");
+        assertThat(service.effectivePermissions().grants()).doesNotContain("ops.production_readiness.manage");
+    }
+
     private AccessRepository.PermissionEffect effect(
             String userId,
             String permissionCode,
```

程序比对：两份 diff 里该文件除版本号行外的新增行逐字相同＝True。

## 3. 内容审计

- 001 内存仓：`replacePositionGrants` 先按上一次记录的岗位应用 id 调 `revokeTemplateApplications`（只撤该用户、仍 active 的），再记新角色与新应用 id——与 Jdbc「先撤 position_application_ids 指向的应用、再写指针」同序；`applyTemplate` 的 id 加序号，避免同一用户第二次套模板与旧 id 相同而被一并撤掉；`findPermissionEffects` 把岗位派生角色与直接装配的角色合并后按同一段逻辑产出 ROLE 来源的授予（角色须 active）。独立模板应用、直接装配角色不受影响。
- 001 测试：旧角色、新角色、模板各带一个不同的码；定岗后有效集含旧角色码与模板码；改映射后含新角色码与模板码、不含旧角色码，模板应用状态为 [revoked, active]；停用后三码都不含，模板应用全为 revoked。变异 assign_passes_no_app_ids 下状态断言得到 [active, active] 而红，说明断言能失败，且抓的正是原断言（只看角色指针）抓不到的错。
- 001 真库测试：只建临时表（6 张 LIKE public、3 张手写表补列），不写 public 业务表；断言岗位派生角色的码以 (code, ROLE, GRANT) 返回，`replacePositionGrants(user, null, null, [], …)` 后为空。变异 no_position_role_join 下红。
- 002：授予的 validFrom=2099-01-01、validUntil=2099-01-02，未复用以 2099 为截止时间的 `temporaryGrant` 助手；断言 `requirePermission` 拒绝且有效集不含该码。变异 ignore_valid_from 下红。位置按 Grok B2：在 `effectivePermissionsExcludesExpiredGrant` 之后、私有助手之前。
- 如实披露：新真库测试在主工作区（含未发布的 Jdbc 改动）下与既有的 `temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce` 同因报错（grant_group_id 列，W-M036-2）；发布口径下绿。
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m036_b3r1_run.log）

```
START=2026-09-21T10:50:09Z HEAD=e456ba96 origin/main=3ca1b507
before tree=b637f432404d6f402eede21dc4af52a6265a87fa
scratch at 3ca1b507
temp migrations: V427__m009_project_profiles_and_associations.sql
== BASE（origin/main 原样）
MVN_EXIT(base)=0
   AccessServiceTests tests=54 errors=0 skipped=0 failures=0
   AuthorizationServiceTests tests=21 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=12 errors=0 skipped=0 failures=0
== 打补丁（发布变体：origin/main 版＋补丁）
patched AccessServiceTests.java 7 V0.08 -> V0.09
patched AuthorizationServiceTests.java 1 V0.01 -> V0.02
patched M006JdbcRegressionTests.java 1 V0.13 -> V0.14
== RED：补丁＋变异 assign_passes_no_app_ids
mutated assign_passes_no_app_ids AccessService.java
MVN_EXIT(red_assign_passes_no_app_ids)=1
   AccessServiceTests tests=54 errors=0 skipped=0 failures=1
       failure savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants | &#10;Expecting actual:&#10;  [&quot;active&quot;, &quot;active&quot;]&#10;to contain exactly (and in same order):&#10;  [&quot;revoked&quot;
   AuthorizationServiceTests tests=22 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=13 errors=0 skipped=0 failures=0
== RED：补丁＋变异 no_position_role_join
mutated no_position_role_join JdbcAccessRepository.java
MVN_EXIT(red_no_position_role_join)=1
   AccessServiceTests tests=54 errors=0 skipped=0 failures=0
   AuthorizationServiceTests tests=22 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=13 errors=0 skipped=0 failures=1
       failure positionDerivedRoleFeedsPermissionEffectsUntilThePositionGrantsAreCleared | &#10;Expecting actual:&#10;  []&#10;to contain exactly (and in same order):&#10;  [(&quot;position.only.code&quot;, ROLE, GRANT)]&#10;but co
== RED：补丁＋变异 ignore_valid_from
mutated ignore_valid_from AuthorizationService.java
MVN_EXIT(red_ignore_valid_from)=1
   AccessServiceTests tests=54 errors=0 skipped=0 failures=0
   AuthorizationServiceTests tests=22 errors=0 skipped=0 failures=1
       failure grantWhoseValidityHasNotStartedDoesNotAuthorize | &#10;Expecting code to raise a throwable.
   M006JdbcRegressionTests tests=13 errors=0 skipped=0 failures=0
== GREEN：补丁、无变异
MVN_EXIT(green)=0
   AccessServiceTests tests=54 errors=0 skipped=0 failures=0
   AuthorizationServiceTests tests=22 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=13 errors=0 skipped=0 failures=0
publish-variant sha256:
  AccessServiceTests.java be3e9be417a860350f57d091e401bf5c9cef772b308849335454035911600766 2160 行
  AuthorizationServiceTests.java 3b3b6c2b2b6c042d92bb0c8dd2721d80def1ace85417197fd907f5d570f6671a 584 行
  M006JdbcRegressionTests.java b16e34e2db9d05f6a78d8a08342cc0d3c97363705634e7aba0af53aba07e2cf9 342 行
scratch dirty after cleanup: 0
scratch removed
== 主工作区落补丁
patched AccessServiceTests.java 7 V0.08 -> V0.09
patched AuthorizationServiceTests.java 1 V0.03 -> V0.04
patched M006JdbcRegressionTests.java 1 V0.13 -> V0.14
after tree=0b38ca49734ed5f50c724214285b7119ecf0c809
frozen diff: 11784 bytes sha256 1e3ca09807990735c264be0b4c7ee5da9c41671ee4bb9da4a984637504de6427
 .../platform/access/AccessServiceTests.java        | 42 +++++++++++++++++-----
 .../platform/access/AuthorizationServiceTests.java | 22 +++++++++++-
 .../platform/access/M006JdbcRegressionTests.java   | 25 ++++++++++++-
 3 files changed, 79 insertions(+), 10 deletions(-)
主工作区版 sha256:
  AccessServiceTests.java be3e9be417a860350f57d091e401bf5c9cef772b308849335454035911600766 2160 行
  AuthorizationServiceTests.java 6d409fb7524492bc217c79c4afbe172787d1f530465a5b927734dc7b4f2a9c5e 615 行
  M006JdbcRegressionTests.java b16e34e2db9d05f6a78d8a08342cc0d3c97363705634e7aba0af53aba07e2cf9 342 行
AccessServiceTests/M006Jdbc 主工作区版 == 发布变体？（AuthorizationServiceTests 两版不同，D-029）
== 主工作区现状对照（含未发布改动，非发布口径）
MVN_EXIT(main worktree)=1
   AccessServiceTests tests=54 errors=6 skipped=0 failures=0
       error assignUserPositionLandsNormalPartAndHoldsHighRiskPartUntilChainApproves | 退款、免单、授权与资金事项必须登记直接受益人
       error dailyTemporaryPermissionApplicationEntersSupervisorChain | 退款、免单、授权与资金事项必须登记直接受益人
       error approvedTemporaryGrantExpiresByEffectivePermissionRead | 退款、免单、授权与资金事项必须登记直接受益人
       error auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse | 退款、免单、授权与资金事项必须登记直接受益人
       error approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment | 退款、免单、授权与资金事项必须登记直接受益人
       error removingTheHighRiskHoldDenyRequiresApproval | 退款、免单、授权与资金事项必须登记直接受益人
   AuthorizationServiceTests tests=25 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=13 errors=2 skipped=0 failures=0
       error positionDerivedRoleFeedsPermissionEffectsUntilThePositionGrantsAreCleared | PreparedStatementCallback; bad SQL grammar [SELECT subject_id AS user_id, permission_code, &apos;grant&apos; AS effect, &apos;TEMPORARY_GRAN
       error temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce | PreparedStatementCallback; bad SQL grammar [UPDATE temporary_permission_grants&#10;   SET consumed = TRUE,&#10;       consumed_at = ?&#10; W
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T10:51:12Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
