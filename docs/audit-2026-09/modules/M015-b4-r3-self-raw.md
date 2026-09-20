CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M015 B4-r3 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；本席修、本席自审，不得以本票覆盖 Grok FAIL）。2026-09-20T05:04:14Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M015 B3-r3（第 3 次，上限）整批修复 diff：before tree `991f4df7a79773fd7a179cb0a0cb374ba345ea57` → after tree `3036d0c089905520aaa86fddb7a75eb75409b019`；冻结 diff `/home/james/audit-tmp/m015_b3_r3.diff`（20661 bytes，SHA256 865c853787ab36e4734305ba68523ea6ce924200bfe1e2a28296b828c7324fa5），全文见 §2。定稿 ID：M015-015、M015-016（findings V2.51，队列「是」）。
- 顺序：Grok 已先派（modules/M015-b4-r3-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：本报告不再改任何文件。

## 1. 逐块归因（§5 闸 3：每一行归因到已定稿且归属「是」的 ID）

| 文件 | hunk | ±行数 | 归因 ID |
|---|---|---:|---|
| FloorPlanService.java | `@@ -1,5 +1,5 @@` | 2 | 015/016 |
| FloorPlanService.java | `@@ -72,9 +72,10 @@ public class FloorPlanService {` | 3 | 015 |
| FloorPlanService.java | `@@ -362,8 +363,9 @@ public class FloorPlanService {` | 5 | 016 |
| FloorPlanService.java | `@@ -510,7 +512,7 @@ public class FloorPlanService {` | 2 | 015 |
| FloorPlanService.java | `@@ -603,12 +605,21 @@ public class FloorPlanService {` | 11 | 015 |
| VenueBusinessHoursService.java | `@@ -1,5 +1,5 @@` | 2 | 015 |
| VenueBusinessHoursService.java | `@@ -41,7 +41,8 @@ public class VenueBusinessHoursService {` | 3 | 015 |
| VenueBusinessHoursService.java | `@@ -51,6 +52,15 @@ public class VenueBusinessHoursService {` | 9 | 015 |
| VenueBusinessHoursService.java | `@@ -62,7 +72,7 @@ public class VenueBusinessHoursService {` | 2 | 015 |
| FloorPlanServiceTests.java | `@@ -1,11 +1,12 @@` | 3 | 015/016 |
| FloorPlanServiceTests.java | `@@ -274,6 +275,37 @@ class FloorPlanServiceTests {` | 31 | 016 |
| FloorPlanServiceTests.java | `@@ -442,22 +474,53 @@ class FloorPlanServiceTests {` | 55 | 015 |
| FloorPlanServiceTests.java | `@@ -468,23 +531,8 @@ class FloorPlanServiceTests {` | 19 | 015 |
| FloorPlanServiceTests.java | `@@ -494,23 +542,8 @@ class FloorPlanServiceTests {` | 19 | 015 |
| VenueBusinessHoursServiceTests.java | `@@ -1,9 +1,10 @@` | 3 | 015 |
| VenueBusinessHoursServiceTests.java | `@@ -86,11 +87,18 @@ class VenueBusinessHoursServiceTests {` | 11 | 015 |
| VenueBusinessHoursServiceTests.java | `@@ -122,6 +130,32 @@ class VenueBusinessHoursServiceTests {` | 26 | 015 |

- 4 文件 17 个 hunk 全部落到 015/016（版本头与 AccessActor import 归两条共用/015）；未改 `AuthorizationService`、Controller、DTO、OpenAPI、迁移、三份 HTML、锁；无「顺手」改动。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
index 1a08caed..fc17314f 100644
--- a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
+++ b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
@@ -1,5 +1,5 @@
 package com.complex.platform.floorplan;
-// 文件版本：V0.03
+// 文件版本：V0.04
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -72,9 +72,10 @@ public class FloorPlanService {
     public PageResponse<FloorPlanRecord> listFloorPlans() {
         Optional<String> merchantScope = authorizationService.currentMerchantIdQuietly();
         // M015-003：商户 scope 优先（用永不抛的 currentMerchantIdQuietly，项目员工才能走到项目 scope）；否则按项目 scope（BEQR-152）；平台管理员全量。
+        // M015-015：项目 scope 经 projectScopeOrAll()（平台管理员为空＝全量；其余走会抛的 currentProjectScopeQuietly）。
         List<FloorPlanRecord> records = merchantScope.isPresent()
                 ? repository.findAllFloorPlansByMerchant(merchantScope.get())
-                : authorizationService.currentProjectScopeQuietly()
+                : projectScopeOrAll()
                         .map(repository::findAllFloorPlansByProject)
                         .orElseGet(repository::findAllFloorPlans);
         auditEventService.recordSuccess(
@@ -362,8 +363,9 @@ public class FloorPlanService {
             FloorPlanImportPublishRequest request
     ) {
         FloorPlanImportTaskRecord task = ensureImportTaskBelongsToFloorPlan(floorPlanId, importTaskId);
-        if (!"parsed".equals(task.status())) {
-            throw ApiException.badRequest("VALIDATION_FAILED", "只有解析完成的导入任务才能发布");
+        // M015-016：人工复核可分批发布——已发布任务的剩余待处理候选仍可继续发布（PGM-023）；failed/cancelled 仍拒。
+        if (!"parsed".equals(task.status()) && !"published".equals(task.status())) {
+            throw ApiException.badRequest("VALIDATION_FAILED", "只有解析完成或已发布的导入任务才能继续发布");
         }
 
         List<String> candidateIds = request.candidateIds().stream()
@@ -510,7 +512,7 @@ public class FloorPlanService {
             throw ApiException.notFound("PROJECT_NOT_FOUND", "综合体项目不存在或已被删除");
         }
         if (authorizationService.currentMerchantIdQuietly().isEmpty()) {
-            authorizationService.currentProjectScopeQuietly().ifPresent(scope -> {
+            projectScopeOrAll().ifPresent(scope -> {
                 if (!scope.equals(projectId)) {
                     throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只能在当前综合体项目下创建平面图");
                 }
@@ -603,12 +605,21 @@ public class FloorPlanService {
         if (authorizationService.currentMerchantIdQuietly().isPresent()) {
             return;
         }
-        Optional<String> scope = authorizationService.currentProjectScopeQuietly();
+        Optional<String> scope = projectScopeOrAll();
         if (scope.isPresent() && !scope.get().equals(resourceProjectId)) {
             throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "平面图属于其他综合体项目");
         }
     }
 
+    // M015-015：项目 scope 只读取值——平台管理员（isPlatformAdmin）为空＝全量，与 requirePlatformScope 的平台管理员桥同口径；
+    // 其余主体走 currentProjectScopeQuietly（无项目归属的员工 fail-closed 抛 PROJECT_SCOPE_REQUIRED，红线④保持）。
+    private Optional<String> projectScopeOrAll() {
+        if (authorizationService.isPlatformAdmin(authorizationService.currentActor())) {
+            return Optional.empty();
+        }
+        return authorizationService.currentProjectScopeQuietly();
+    }
+
     private void checkMerchantOwnership(String resourceMerchantId) {
         Optional<String> scope = authorizationService.currentMerchantIdQuietly();
         if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
diff --git a/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java b/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
index 2192b92b..d61b8496 100644
--- a/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
+++ b/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -41,7 +41,8 @@ public class VenueBusinessHoursService {
 
     public List<VenueBusinessHoursRecord> list() {
         // M015-009：项目员工只看本项目场地（BEQR-152）；平台管理员 scope 为空取全量。
-        Optional<String> projectScope = authorizationService.currentProjectScopeQuietly();
+        // M015-015：平台管理员经 projectScopeOrAll() 取全量；其余走会抛的 currentProjectScopeQuietly。
+        Optional<String> projectScope = projectScopeOrAll();
         List<VenueBusinessHoursRecord> rows = projectScope.isPresent()
                 ? repository.listByProject(projectScope.get())
                 : repository.listAll();
@@ -51,6 +52,15 @@ public class VenueBusinessHoursService {
         return rows;
     }
 
+    // M015-015：项目 scope 只读取值——平台管理员（isPlatformAdmin）为空＝全量，与 requirePlatformScope 的平台管理员桥同口径；
+    // 其余主体走 currentProjectScopeQuietly（无项目归属的员工 fail-closed 抛 PROJECT_SCOPE_REQUIRED，红线④保持）。
+    private Optional<String> projectScopeOrAll() {
+        if (authorizationService.isPlatformAdmin(authorizationService.currentActor())) {
+            return Optional.empty();
+        }
+        return authorizationService.currentProjectScopeQuietly();
+    }
+
     public VenueBusinessHoursRecord save(VenueBusinessHoursRequest request) {
         if (request == null) {
             throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
@@ -62,7 +72,7 @@ public class VenueBusinessHoursService {
         // M015-009：场地必须是现存空间（spaces.id），且项目员工只能配置本项目场地。
         String venueProjectId = repository.findVenueProjectId(venueId)
                 .orElseThrow(() -> ApiException.notFound("VENUE_NOT_FOUND", "场地不存在或已被删除"));
-        authorizationService.currentProjectScopeQuietly().ifPresent(scope -> {
+        projectScopeOrAll().ifPresent(scope -> {
             if (!scope.equals(venueProjectId)) {
                 throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "场地属于其他综合体项目");
             }
diff --git a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
index 9f3fb0ab..a96b9f65 100644
--- a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
@@ -1,11 +1,12 @@
 package com.complex.platform.floorplan;
-// 文件版本：V0.04
+// 文件版本：V0.05
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
+import com.complex.platform.access.AccessActor;
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.common.api.PageResponse;
@@ -274,6 +275,37 @@ class FloorPlanServiceTests {
                 assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
     }
 
+    // M015-016：人工复核只发部分候选后，剩余待处理候选仍可继续发布（修复前第二次发布被「只有解析完成的导入任务才能发布」拒绝）
+    @Test
+    void publishRemainingPendingCandidatesAfterPartialPublish() {
+        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-publish-partial"));
+        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(), importRequest("fp-import-partial", """
+                <svg viewBox="0 0 1000 800">
+                  <polygon id="A101" data-shape-type="shop" data-ref-type="space" data-ref-id="sp_101" points="100,80 300,80 300,240 100,240" />
+                  <polygon id="A102" data-shape-type="booth" data-ref-type="space" data-ref-id="sp_102" points="400,80 600,80 600,240 400,240" />
+                </svg>
+                """));
+        List<String> candidateIds = repository.findImportCandidatesByTaskId(task.id()).stream()
+                .map(FloorPlanImportCandidateRecord::id)
+                .toList();
+        assertThat(candidateIds).hasSize(2);
+
+        FloorPlanImportTaskRecord first = service.publishImportTask(floorPlan.id(), task.id(),
+                new FloorPlanImportPublishRequest("publish-partial-1", "publish first", List.of(candidateIds.get(0))));
+        assertThat(first.status()).isEqualTo("published");
+
+        FloorPlanImportTaskRecord second = service.publishImportTask(floorPlan.id(), task.id(),
+                new FloorPlanImportPublishRequest("publish-partial-2", "publish rest", List.of(candidateIds.get(1))));
+
+        assertThat(second.status()).isEqualTo("published");
+        assertThat(repository.findShapesByFloorPlanId(floorPlan.id()))
+                .extracting(FloorPlanShapeRecord::label)
+                .containsExactlyInAnyOrder("A101", "A102");
+        assertThat(repository.findImportCandidatesByTaskId(task.id()))
+                .extracting(FloorPlanImportCandidateRecord::status)
+                .containsOnly("published");
+    }
+
     // P1-2: saveShape 未校验 shapeId 归属 — 修复前此测试会红（无归属校验时覆写成功）
     @Test
     void saveShapeRejectsShapeIdBelongingToDifferentFloorPlan() {
@@ -442,22 +474,53 @@ class FloorPlanServiceTests {
         assertThat(repository.floorPlanSaveCount).isZero();
     }
 
-    // 平台管理员桥（生产语义：isPlatformAdminBridge → 商户/项目 scope 皆空、永不抛）
+    // M015-015：无 organizationId 的平台管理员全量可达（修复前真 currentProjectScopeQuietly 抛 PROJECT_SCOPE_REQUIRED，本例与 setUp 用 service 的各例一起红）
+    @Test
+    void platformAdminWithoutOrganizationSeesAllFloorPlans() {
+        service.createFloorPlan(createRequest("fp-admin-all-1", "prj_1"));
+        service.createFloorPlan(createRequest("fp-admin-all-2", "prj_2"));
+
+        assertThat(service.listFloorPlans().items())
+                .extracting(FloorPlanRecord::projectId)
+                .containsExactlyInAnyOrder("prj_1", "prj_2");
+    }
+
+    // M015-015：无项目归属的普通员工 fail-closed（红线④：不因缺归属放行全量）
+    @Test
+    void staffWithoutProjectScopeIsRejected() {
+        FloorPlanRecord plan = service.createFloorPlan(createRequest("fp-orphan-1", "prj_1"));
+        FloorPlanService orphan = new FloorPlanService(
+                repository,
+                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
+                new AuditEventService(auditEventRepository),
+                new FloorPlanImportParser(),
+                orphanStaffAuth()
+        );
+
+        assertThatThrownBy(orphan::listFloorPlans)
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("PROJECT_SCOPE_REQUIRED"));
+        assertThatThrownBy(() -> orphan.listShapes(plan.id()))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("PROJECT_SCOPE_REQUIRED"));
+    }
+
+    // M015-015：平台管理员（生产语义：只替换 currentActor，currentMerchantIdQuietly / currentProjectScopeQuietly / isPlatformAdmin 走真实现——
+    // 无 organizationId 的平台管理员调 currentProjectScopeQuietly 会抛 PROJECT_SCOPE_REQUIRED，服务须经 isPlatformAdmin 判全量）
     private AuthorizationService noScopeAuth() {
-        return new AuthorizationService(null) {
-            @Override
-            public Optional<String> currentMerchantScope() {
-                return Optional.empty();
-            }
+        return actorAuth(new AccessActor("usr_platform_admin", "platform-admin", "platform_admin", null, null, "active"));
+    }
 
-            @Override
-            public Optional<String> currentMerchantIdQuietly() {
-                return Optional.empty();
-            }
+    // M015-015：无项目归属的普通员工（生产语义：currentProjectScopeQuietly 抛 PROJECT_SCOPE_REQUIRED，fail-closed）
+    private AuthorizationService orphanStaffAuth() {
+        return actorAuth(new AccessActor("usr_orphan", "orphan", "staff", null, null, "active"));
+    }
 
+    private static AuthorizationService actorAuth(AccessActor actor) {
+        return new AuthorizationService(null) {
             @Override
-            public Optional<String> currentProjectScopeQuietly() {
-                return Optional.empty();
+            public AccessActor currentActor() {
+                return actor;
             }
         };
     }
@@ -468,23 +531,8 @@ class FloorPlanServiceTests {
                 new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                 new AuditEventService(auditEventRepository),
                 new FloorPlanImportParser(),
-                // 项目员工（生产语义：无 merchantId → currentMerchantScope 抛 MERCHANT_SCOPE_REQUIRED；currentMerchantIdQuietly 为空；项目 scope 有值）
-                new AuthorizationService(null) {
-                    @Override
-                    public Optional<String> currentMerchantScope() {
-                        throw ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");
-                    }
-
-                    @Override
-                    public Optional<String> currentMerchantIdQuietly() {
-                        return Optional.empty();
-                    }
-
-                    @Override
-                    public Optional<String> currentProjectScopeQuietly() {
-                        return Optional.of(projectId);
-                    }
-                }
+                // 项目员工（生产语义：仅替换 currentActor；无 merchantId → currentMerchantScope 抛、currentMerchantIdQuietly 空；organizationId=项目 → 项目 scope 有值）
+                actorAuth(new AccessActor("usr_" + projectId, projectId + "-staff", "staff", projectId, null, "active"))
         );
     }
 
@@ -494,23 +542,8 @@ class FloorPlanServiceTests {
                 new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                 new AuditEventService(auditEventRepository),
                 new FloorPlanImportParser(),
-                // 商户员工（生产语义：有 merchantId、无 organizationId → currentProjectScopeQuietly 抛 PROJECT_SCOPE_REQUIRED）
-                new AuthorizationService(null) {
-                    @Override
-                    public Optional<String> currentMerchantScope() {
-                        return Optional.of(merchantId);
-                    }
-
-                    @Override
-                    public Optional<String> currentMerchantIdQuietly() {
-                        return Optional.of(merchantId);
-                    }
-
-                    @Override
-                    public Optional<String> currentProjectScopeQuietly() {
-                        throw ApiException.forbidden("PROJECT_SCOPE_REQUIRED", "此操作必须在项目范围内进行");
-                    }
-                }
+                // 商户员工（生产语义：仅替换 currentActor；有 merchantId、无 organizationId → currentProjectScopeQuietly 抛 PROJECT_SCOPE_REQUIRED）
+                actorAuth(new AccessActor("usr_" + merchantId, merchantId + "-staff", "merchant_staff", null, merchantId, "active"))
         );
     }
 
diff --git a/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java b/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
index 302a5cdc..bc51adb9 100644
--- a/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
@@ -1,9 +1,10 @@
 package com.complex.platform.governance;
-// 文件版本：V0.02
+// 文件版本：V0.03
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
+import com.complex.platform.access.AccessActor;
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.evidence.AuditEvent;
@@ -86,11 +87,18 @@ class VenueBusinessHoursServiceTests {
         return service;
     }
 
+    // M015-015：只替换 currentActor，scope 方法走真实现——projectScope 为空＝无 organizationId 的平台管理员（真 currentProjectScopeQuietly 会抛，服务须经 isPlatformAdmin 判全量）
     private static AuthorizationService auth(String projectScope) {
+        return actorAuth(projectScope == null
+                ? new AccessActor("usr_platform_admin", "platform-admin", "platform_admin", null, null, "active")
+                : new AccessActor("usr_" + projectScope, projectScope + "-staff", "staff", projectScope, null, "active"));
+    }
+
+    private static AuthorizationService actorAuth(AccessActor actor) {
         return new AuthorizationService(null) {
             @Override
-            public Optional<String> currentProjectScopeQuietly() {
-                return Optional.ofNullable(projectScope);
+            public AccessActor currentActor() {
+                return actor;
             }
         };
     }
@@ -122,6 +130,32 @@ class VenueBusinessHoursServiceTests {
         assertThat(repository.projectQueries).containsExactly("prj_1");
     }
 
+    // M015-015：无 organizationId 的平台管理员列表取全量（修复前真 currentProjectScopeQuietly 抛 PROJECT_SCOPE_REQUIRED，本例与 service() 各例一起红）
+    @Test
+    void platformAdminWithoutOrganizationListsAllVenues() {
+        VenueBusinessHoursService admin = service();
+        admin.save(request(1, "18:00", "23:00"));
+        repository.saved.add(new VenueBusinessHoursRecord(
+                "vbh_side", "sp_bar_side", 1, "18:00", "23:00", false, null, Instant.EPOCH, Instant.EPOCH));
+
+        assertThat(admin.list()).extracting(VenueBusinessHoursRecord::venueId).containsExactlyInAnyOrder("sp_bar_main", "sp_bar_side");
+        assertThat(repository.projectQueries).isEmpty();
+    }
+
+    // M015-015：无项目归属的普通员工 fail-closed（红线④：不因缺归属放行全量）
+    @Test
+    void staffWithoutProjectScopeIsRejected() {
+        repository = new RecordingRepository();
+        VenueBusinessHoursService orphan = new VenueBusinessHoursService(repository, new AuditEventService(new InMemoryAuditEventRepository()),
+                actorAuth(new AccessActor("usr_orphan", "orphan", "staff", null, null, "active")));
+
+        assertThatThrownBy(orphan::list)
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PROJECT_SCOPE_REQUIRED"));
+        assertThatThrownBy(() -> orphan.save(request(1, "18:00", "23:00")))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PROJECT_SCOPE_REQUIRED"));
+        assertThat(repository.saved).isEmpty();
+    }
+
     private VenueBusinessHoursRequest request(Integer dayOfWeek, String open, String close) {
         return new VenueBusinessHoursRequest("idem-1", "sp_bar_main", dayOfWeek, open, close, false, null);
     }
```

## 3. 内容审计（对照定稿最小处置）

- 015：`FloorPlanService.projectScopeOrAll()` / `VenueBusinessHoursService.projectScopeOrAll()`——`authorizationService.isPlatformAdmin(authorizationService.currentActor())`（`AuthorizationService:279-281` 公有方法，等价 `isPlatformAdminBridge`＝`requirePlatformScope:482-486` 的平台管理员桥同口径）为真 → `Optional.empty()`（全量）；否则 `currentProjectScopeQuietly()`（`:504-507`，无 organizationId 抛 `PROJECT_SCOPE_REQUIRED`，红线④与 `M006SecurityRegressionTests:150` 保持）。五处调用（列表/创建校验/`checkProjectOwnership`/营业时段 list/save）全部改用；商户员工分支（`currentMerchantIdQuietly` 优先）与项目员工分支（organizationId=项目）未动。测试桩改为只替换 `currentActor()`：`currentMerchantScope/currentMerchantIdQuietly/currentProjectScopeQuietly/isPlatformAdmin` 走真实现——修复前 RED（RED_MVN_EXIT=1），修复后 GREEN。
- 016：发布前置 `!"parsed" && !"published"` → 已发布任务的剩余 pending 候选可继续发布；`failed`/`cancelled` 仍拒；候选状态闸「只有待处理的候选项才能发布」未动，故重复发布同一候选仍被拒（既有用例 `publishImportTaskRejectsAlreadyPublishedTask` 仍绿）。新用例 `publishRemainingPendingCandidatesAfterPartialPublish` 修复前红、修复后绿。
- 回归：5 类 88 例 0 失败（`m015_b3r3_green.log`，TEST-*.xml 口径）。
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m015_b3r3_run.log）

```
START=2026-09-20T05:03:40Z HEAD=e456ba96 URL=jdbc:postgresql://127.0.0.1:5432/complex_test SPRING_PROFILES_ACTIVE_set=0
CHECK tests OK
CHECK impl OK
BEFORE_TREE=991f4df7a79773fd7a179cb0a0cb374ba345ea57
== RED: tests only
CHECK tests OK
APPLIED tests 10 edits
RED_MVN_EXIT=1
RED TEST-com.complex.platform.floorplan.FloorPlanControllerTests: tests="14" errors="0" failures="0" 
RED TEST-com.complex.platform.floorplan.FloorPlanImportParserTests: tests="14" errors="0" failures="0" 
RED TEST-com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests: tests="7" errors="0" failures="0" 
RED TEST-com.complex.platform.floorplan.FloorPlanServiceTests: tests="39" errors="30" failures="2" 
RED TEST-com.complex.platform.governance.VenueBusinessHoursServiceTests: tests="14" errors="6" failures="3" 
     10  but was: "PROJECT_SCOPE_REQUIRED"
== GREEN: impl
CHECK impl OK
APPLIED impl 10 edits
GREEN_MVN_EXIT=0
GREEN TEST-com.complex.platform.floorplan.FloorPlanControllerTests: tests=14 failures+errors=0
GREEN TEST-com.complex.platform.floorplan.FloorPlanImportParserTests: tests=14 failures+errors=0
GREEN TEST-com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests: tests=7 failures+errors=0
GREEN TEST-com.complex.platform.floorplan.FloorPlanServiceTests: tests=39 failures+errors=0
GREEN TEST-com.complex.platform.governance.VenueBusinessHoursServiceTests: tests=14 failures+errors=0
GREEN_XML_TOTAL=88 GREEN_XML_FAILED=0
AFTER_TREE=3036d0c089905520aaa86fddb7a75eb75409b019
DIFF bytes=20661 sha256=865c853787ab36e4734305ba68523ea6ce924200bfe1e2a28296b828c7324fa5
 .../platform/floorplan/FloorPlanService.java       |  23 +++-
 .../governance/VenueBusinessHoursService.java      |  16 ++-
 .../platform/floorplan/FloorPlanServiceTests.java  | 127 +++++++++++++--------
 .../governance/VenueBusinessHoursServiceTests.java |  40 ++++++-
 4 files changed, 147 insertions(+), 59 deletions(-)
hunks=17 files=4
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java // 文件版本：V0.04 689 5cad799d091fd43b58d99ff05d12e10afbc16e63fb7e59857dcd94c042ccdd80
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java // 文件版本：V0.02 125 149c1eec1650ada8cf9a447bf3fd7117683e71c1a882dea84680d2134082fd5e
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java // 文件版本：V0.05 897 ea7aa63ecdb36e9cb8e0b921c62f012e5931d1a157a99ae202015a6b4ceda31d
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java // 文件版本：V0.03 285 3585ca694bf16fdd809689b819d258641792c6a8c964448374f295dff85c9c76
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-20T05:04:09Z
```

## 5. 结论

席甲 B4-r3 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
