CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M015 B4-r2 修复内容复审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B4-r2（B3 第 2 次整批修复的完整 diff 复审）。2026-09-11T08:04:47Z
- before tree：`7337f4c1bd2093e7e4d3ad0208e09aa38893a6e5`；after tree：`e7d8355fc16574f780cf51b134f06c72550cb666`；`git diff --binary 7337f4c1bd20 e7d8355fc165` 全文 11158 bytes SHA256 9a3d497dca2e1231db764e5c594934f33c3fbe50603bdf93cbb4d31ae9817b91（全文附于本报告末尾）。
- 定稿依据 findings V1.93「当前 M015 B2-r2 双PASS定稿」；本轮（B3-r2）只再修 Grok B4-r1 有 ID 项：M015-001（派生主键落入 VARCHAR(64)）、M015-003（项目员工经 currentMerchantIdQuietly 到达项目 scope）。B3 累计 2/3。TDD：RED＝测试桩改为生产语义 + 新增 derivedIdsFitVarchar64Columns，在未改服务上 FloorPlanServiceTests 27 例 4 红（3 例 MERCHANT_SCOPE_REQUIRED、1 例候选 id 68>64）；GREEN＝5 个测试类 55 例全绿（/tmp/m015_b3r2_red.log、_green.log）。B3-r1 diff 已经 B4-r1 复审（范围归因双 PASS）。
- 锁四值：实算/sidecar/ledger/Grok 均 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。

## 1. diffstat

```
.../platform/floorplan/FloorPlanService.java       | 26 ++++++++-----
 .../platform/floorplan/FloorPlanServiceTests.java  | 45 +++++++++++++++++++++-
 2 files changed, 60 insertions(+), 11 deletions(-)
```

## 2. 逐块归因（每一行改动都归到定稿 ID；「版本铁律」行随其所在文件的 ID）

| 文件 | 块 | 位置 | 块内标记 ID | 归因 |
|---|---|---|---|---|
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | 001（derivedId 前缀 + SHA-256 前 32 位：图形/任务/发布图形三处）、003（五处 currentMerchantScope → currentMerchantIdQuietly、注释）；版本头 V0.02 随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 2 | `@@ -69,8 +69,8 @@ public class FloorPlanService {` | M015-003 | 001（derivedId 前缀 + SHA-256 前 32 位：图形/任务/发布图形三处）、003（五处 currentMerchantScope → currentMerchantIdQuietly、注释）；版本头 V0.02 随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 3 | `@@ -227,7 +227,7 @@ public class FloorPlanService {` | — | 001（derivedId 前缀 + SHA-256 前 32 位：图形/任务/发布图形三处）、003（五处 currentMerchantScope → currentMerchantIdQuietly、注释）；版本头 V0.02 随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 4 | `@@ -265,7 +265,7 @@ public class FloorPlanService {` | — | 001（derivedId 前缀 + SHA-256 前 32 位：图形/任务/发布图形三处）、003（五处 currentMerchantScope → currentMerchantIdQuietly、注释）；版本头 V0.02 随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 5 | `@@ -293,7 +293,7 @@ public class FloorPlanService {` | — | 001（derivedId 前缀 + SHA-256 前 32 位：图形/任务/发布图形三处）、003（五处 currentMerchantScope → currentMerchantIdQuietly、注释）；版本头 V0.02 随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 6 | `@@ -390,7 +390,7 @@ public class FloorPlanService {` | — | 001（derivedId 前缀 + SHA-256 前 32 位：图形/任务/发布图形三处）、003（五处 currentMerchantScope → currentMerchantIdQuietly、注释）；版本头 V0.02 随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 7 | `@@ -508,7 +508,7 @@ public class FloorPlanService {` | — | 001（derivedId 前缀 + SHA-256 前 32 位：图形/任务/发布图形三处）、003（五处 currentMerchantScope → currentMerchantIdQuietly、注释）；版本头 V0.02 随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 8 | `@@ -599,7 +599,7 @@ public class FloorPlanService {` | — | 001（derivedId 前缀 + SHA-256 前 32 位：图形/任务/发布图形三处）、003（五处 currentMerchantScope → currentMerchantIdQuietly、注释）；版本头 V0.02 随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 9 | `@@ -609,7 +609,7 @@ public class FloorPlanService {` | — | 001（derivedId 前缀 + SHA-256 前 32 位：图形/任务/发布图形三处）、003（五处 currentMerchantScope → currentMerchantIdQuietly、注释）；版本头 V0.02 随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 10 | `@@ -647,6 +647,12 @@ public class FloorPlanService {` | M015-001 | 001（derivedId 前缀 + SHA-256 前 32 位：图形/任务/发布图形三处）、003（五处 currentMerchantScope → currentMerchantIdQuietly、注释）；版本头 V0.02 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | 001（derivedIdsFitVarchar64Columns +1 例）、003（noScopeAuth/serviceWithMerchantScope/serviceWithProjectScope 桩改为生产语义）；版本头 V0.02 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 2 | `@@ -359,6 +359,26 @@ class FloorPlanServiceTests {` | M015-001 | 001（derivedIdsFitVarchar64Columns +1 例）、003（noScopeAuth/serviceWithMerchantScope/serviceWithProjectScope 桩改为生产语义）；版本头 V0.02 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 3 | `@@ -419,6 +439,7 @@ class FloorPlanServiceTests {` | — | 001（derivedIdsFitVarchar64Columns +1 例）、003（noScopeAuth/serviceWithMerchantScope/serviceWithProjectScope 桩改为生产语义）；版本头 V0.02 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 4 | `@@ -426,6 +447,11 @@ class FloorPlanServiceTests {` | — | 001（derivedIdsFitVarchar64Columns +1 例）、003（noScopeAuth/serviceWithMerchantScope/serviceWithProjectScope 桩改为生产语义）；版本头 V0.02 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 5 | `@@ -439,9 +465,15 @@ class FloorPlanServiceTests {` | — | 001（derivedIdsFitVarchar64Columns +1 例）、003（noScopeAuth/serviceWithMerchantScope/serviceWithProjectScope 桩改为生产语义）；版本头 V0.02 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 6 | `@@ -459,11 +491,22 @@ class FloorPlanServiceTests {` | — | 001（derivedIdsFitVarchar64Columns +1 例）、003（noScopeAuth/serviceWithMerchantScope/serviceWithProjectScope 桩改为生产语义）；版本头 V0.02 随本文件 |

## 3. 红绿证据（B3-r2 运行记录 /tmp/m015_b3r2_run.log 原文）

```
START=2026-09-11T07:45:46Z HEAD=a041458d
== RED: stubs with production semantics + VARCHAR(64) test (service unchanged)
FloorPlanServiceTests.java 4a0f962e9ec981ba
RED2_PATCH_OK
RED_EXIT=1
Tests run: 27, Failures: 3, Errors: 1, Skipped: 0, Time elapsed: 0.275 s <<< FAILURE! -- in com.complex.platform.floorplan.FloorPlanServiceTests
1:[ERROR] Tests run: 27, Failures: 3, Errors: 1, Skipped: 0,
2:[ERROR] com.complex.platform.floorplan.FloorPlanServiceTests.createFloorPlanOutsideProjectScopeIsForbidden --
10:[ERROR] com.complex.platform.floorplan.FloorPlanServiceTests.accessingFloorPlanBelongingToDifferentProjectIsForbidden --
18:[ERROR] com.complex.platform.floorplan.FloorPlanServiceTests.listFloorPlansFiltersToProjectScope --
25:[ERROR] com.complex.platform.floorplan.FloorPlanServiceTests.derivedIdsFitVarchar64Columns --
5:expected: "CROSS_PROJECT_FORBIDDEN"
6: but was: "MERCHANT_SCOPE_REQUIRED"
13:expected: "CROSS_PROJECT_FORBIDDEN"
14: but was: "MERCHANT_SCOPE_REQUIRED"
28:Expecting all elements of:
34:Expecting size of:
36:to be less than or equal to 64 but was 68
45:expected: "CROSS_PROJECT_FORBIDDEN"
== GREEN: derivedId + currentMerchantIdQuietly
FloorPlanService.java d912fb381895537f
GREEN2_PATCH_OK
GREEN_EXIT=0
floorplan.FloorPlanControllerTests.txt:Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.836 s -- in com.complex.platform.floorplan.FloorPlanControllerTests
floorplan.FloorPlanImportParserTests.txt:Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.141 s -- in com.complex.platform.floorplan.FloorPlanImportParserTests
floorplan.FloorPlanJdbcIntegrationTests.txt:Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.648 s -- in com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
floorplan.FloorPlanServiceTests.txt:Tests run: 27, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.076 s -- in com.complex.platform.floorplan.FloorPlanServiceTests
governance.VenueBusinessHoursServiceTests.txt:Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.045 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
== post-B3-r2 fingerprints
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java 677 d912fb381895537f
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java 688 4a0f962e9ec981ba
 M backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java
 M backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java
 M backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
 M backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java
 M backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java
 M backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
 M backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
 M docs/audit-2026-09/findings.md
 M docs/audit-2026-09/ledger.md
END=2026-09-11T07:46:07Z
```

## 4. 本席复审结论

见文末追加的判定段。

## 附：完整 diff（before→after，--binary）

```diff
diff --git a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
index 7684e2a3..75663eeb 100644
--- a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
+++ b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
@@ -1,5 +1,5 @@
 package com.complex.platform.floorplan;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -69,8 +69,8 @@ public class FloorPlanService {
     }
 
     public PageResponse<FloorPlanRecord> listFloorPlans() {
-        Optional<String> merchantScope = authorizationService.currentMerchantScope();
-        // M015-003：商户 scope 优先；否则按项目 scope（BEQR-152 数据默认按项目隔离）；平台管理员全量。
+        Optional<String> merchantScope = authorizationService.currentMerchantIdQuietly();
+        // M015-003：商户 scope 优先（用永不抛的 currentMerchantIdQuietly，项目员工才能走到项目 scope）；否则按项目 scope（BEQR-152）；平台管理员全量。
         List<FloorPlanRecord> records = merchantScope.isPresent()
                 ? repository.findAllFloorPlansByMerchant(merchantScope.get())
                 : authorizationService.currentProjectScopeQuietly()
@@ -227,7 +227,7 @@ public class FloorPlanService {
 
     private FloorPlanRecord createFloorPlanOnce(FloorPlanCreateRequest request) {
         Instant now = Instant.now();
-        Optional<String> scope = authorizationService.currentMerchantScope();
+        Optional<String> scope = authorizationService.currentMerchantIdQuietly();
         FloorPlanRecord record = repository.saveFloorPlan(new FloorPlanRecord(
                 "fp_" + UUID.randomUUID(),
                 requireText(request.projectId(), "project_id"),
@@ -265,7 +265,7 @@ public class FloorPlanService {
         }
         Instant now = Instant.now();
         FloorPlanShapeRecord record = repository.saveShape(new FloorPlanShapeRecord(
-                requestedShapeId == null ? "fps_" + slug(floorPlanId + "_" + request.idempotencyKey()) : requestedShapeId,
+                requestedShapeId == null ? derivedId("fps_", floorPlanId + "_" + request.idempotencyKey()) : requestedShapeId,
                 floorPlanId,
                 normalizeAllowed(request.shapeType(), "shape_type", SHAPE_TYPES),
                 normalizeAllowed(request.refType(), "ref_type", REF_TYPES),
@@ -293,7 +293,7 @@ public class FloorPlanService {
         Instant now = Instant.now();
         FloorPlanImportParseResult parseResult = importParser.parse(request.sourceFormat(), request.sourcePayload());
         enforceImportLimits(parseResult);
-        String taskId = "fpi_" + slug(floorPlanId + "_" + request.idempotencyKey());
+        String taskId = derivedId("fpi_", floorPlanId + "_" + request.idempotencyKey());
         String status = parseResult.candidates().isEmpty() ? "failed" : "parsed";
         FloorPlanImportTaskRecord task = repository.saveImportTask(new FloorPlanImportTaskRecord(
                 taskId,
@@ -390,7 +390,7 @@ public class FloorPlanService {
         Instant now = Instant.now();
         for (FloorPlanImportCandidateRecord candidate : candidates) {
             repository.saveShape(new FloorPlanShapeRecord(
-                    "fps_" + slug(importTaskId + "_" + candidate.id()),
+                    derivedId("fps_", importTaskId + "_" + candidate.id()),
                     floorPlanId,
                     candidate.shapeType(),
                     candidate.refType(),
@@ -508,7 +508,7 @@ public class FloorPlanService {
         if (!repository.projectExists(projectId)) {
             throw ApiException.notFound("PROJECT_NOT_FOUND", "综合体项目不存在或已被删除");
         }
-        if (authorizationService.currentMerchantScope().isEmpty()) {
+        if (authorizationService.currentMerchantIdQuietly().isEmpty()) {
             authorizationService.currentProjectScopeQuietly().ifPresent(scope -> {
                 if (!scope.equals(projectId)) {
                     throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只能在当前综合体项目下创建平面图");
@@ -599,7 +599,7 @@ public class FloorPlanService {
 
     // M015-003：项目员工只能访问本项目平面图（BEQR-152）；商户账号走商户闸、平台管理员 scope 为空放行。
     private void checkProjectOwnership(String resourceProjectId) {
-        if (authorizationService.currentMerchantScope().isPresent()) {
+        if (authorizationService.currentMerchantIdQuietly().isPresent()) {
             return;
         }
         Optional<String> scope = authorizationService.currentProjectScopeQuietly();
@@ -609,7 +609,7 @@ public class FloorPlanService {
     }
 
     private void checkMerchantOwnership(String resourceMerchantId) {
-        Optional<String> scope = authorizationService.currentMerchantScope();
+        Optional<String> scope = authorizationService.currentMerchantIdQuietly();
         if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
             throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "平面图属于其他商户");
         }
@@ -647,6 +647,12 @@ public class FloorPlanService {
         return trimmed;
     }
 
+    // M015-001（r2）：派生主键＝前缀 + SHA-256 前 32 位十六进制（恒 36 字符），确定性、含平面图域且恒落入 id VARCHAR(64)；
+    // slug 直接拼接会随平面图 UUID 与客户端键长度溢出（候选 id 68、UUID 键图形 id 80）。
+    private String derivedId(String prefix, String seed) {
+        return prefix + sha256(seed).substring(0, 32);
+    }
+
     private String slug(String value) {
         return requireText(value, "idempotency_key").toLowerCase(Locale.ROOT)
                 .replaceAll("[^a-z0-9_-]", "_");
diff --git a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
index ec368566..515b2dff 100644
--- a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.floorplan;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
@@ -359,6 +359,26 @@ class FloorPlanServiceTests {
         assertThat(repository.findImportCandidatesByTaskId(taskOnB.id())).hasSize(1);
     }
 
+    // M015-001（r2）：派生主键必须落入 V023/V034 的 id VARCHAR(64)——现网前端键 fpi-<13 位时间戳> 与 UUID 键都要能落库（修复前候选 id 68、UUID 图形 id 80 → 此例红）
+    @Test
+    void derivedIdsFitVarchar64Columns() {
+        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-varchar64"));
+        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(), importRequest("fpi-1757570000000"));
+        List<String> candidateIds = repository.findImportCandidatesByTaskId(task.id()).stream()
+                .map(FloorPlanImportCandidateRecord::id).toList();
+        service.publishImportTask(floorPlan.id(), task.id(),
+                new FloorPlanImportPublishRequest("publish-varchar64", "publish import", candidateIds));
+        FloorPlanShapeRecord shape = service.saveShape(floorPlan.id(),
+                shapeRequest("6f1d2c3b-4a5e-4f60-9b7c-8d9e0f1a2b3c", null, polygon()));
+
+        assertThat(task.id()).hasSizeLessThanOrEqualTo(64);
+        assertThat(candidateIds).allSatisfy(id -> assertThat(id).hasSizeLessThanOrEqualTo(64));
+        assertThat(repository.findShapesByFloorPlanId(floorPlan.id()))
+                .extracting(FloorPlanShapeRecord::id)
+                .allSatisfy(id -> assertThat(id).hasSizeLessThanOrEqualTo(64));
+        assertThat(shape.id()).hasSizeLessThanOrEqualTo(64);
+    }
+
     // M015-007：列表不分页时 pageSize/total 必须等于真实条数（修复前 pageSize 谎报 50 → 此例红）
     @Test
     void listShapesReportsHonestPageSizeAndTotal() {
@@ -419,6 +439,7 @@ class FloorPlanServiceTests {
         assertThat(repository.floorPlanSaveCount).isZero();
     }
 
+    // 平台管理员桥（生产语义：isPlatformAdminBridge → 商户/项目 scope 皆空、永不抛）
     private AuthorizationService noScopeAuth() {
         return new AuthorizationService(null) {
             @Override
@@ -426,6 +447,11 @@ class FloorPlanServiceTests {
                 return Optional.empty();
             }
 
+            @Override
+            public Optional<String> currentMerchantIdQuietly() {
+                return Optional.empty();
+            }
+
             @Override
             public Optional<String> currentProjectScopeQuietly() {
                 return Optional.empty();
@@ -439,9 +465,15 @@ class FloorPlanServiceTests {
                 new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                 new AuditEventService(auditEventRepository),
                 new FloorPlanImportParser(),
+                // 项目员工（生产语义：无 merchantId → currentMerchantScope 抛 MERCHANT_SCOPE_REQUIRED；currentMerchantIdQuietly 为空；项目 scope 有值）
                 new AuthorizationService(null) {
                     @Override
                     public Optional<String> currentMerchantScope() {
+                        throw ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");
+                    }
+
+                    @Override
+                    public Optional<String> currentMerchantIdQuietly() {
                         return Optional.empty();
                     }
 
@@ -459,11 +491,22 @@ class FloorPlanServiceTests {
                 new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                 new AuditEventService(auditEventRepository),
                 new FloorPlanImportParser(),
+                // 商户员工（生产语义：有 merchantId、无 organizationId → currentProjectScopeQuietly 抛 PROJECT_SCOPE_REQUIRED）
                 new AuthorizationService(null) {
                     @Override
                     public Optional<String> currentMerchantScope() {
                         return Optional.of(merchantId);
                     }
+
+                    @Override
+                    public Optional<String> currentMerchantIdQuietly() {
+                        return Optional.of(merchantId);
+                    }
+
+                    @Override
+                    public Optional<String> currentProjectScopeQuietly() {
+                        throw ApiException.forbidden("PROJECT_SCOPE_REQUIRED", "此操作必须在项目范围内进行");
+                    }
                 }
         );
     }

```


## 4. 本席复审结论（席甲，作者自审票）

本席逐块读完上述 B3-r2 diff 全文（2 文件：`FloorPlanService.java`、`FloorPlanServiceTests.java`），以 §2 归因表核对每一行；§3 为本席 B3-r2 运行记录原文（RED 4 例红 → GREEN 5 类 55 例全绿，主工作区实跑，只导出三个 COMPLEX_DATABASE_* 变量、complex_test）。B3-r1 diff 已经 B4-r1 双席复审（范围归因双 PASS；内容 Grok FAIL 001/003，席甲核源后撤票），本轮只审 001/003 的再修。

### 4.1 修复内容（逐 ID）

| ID | 修复内容 | 证据（diff 块） | 本席票 |
|---|---|---|---|
| 001 | 新增 `derivedId(prefix, seed)` = 前缀 + `sha256(seed).substring(0, 32)`（恒 36 字符）；图形 `fps_`、导入任务 `fpi_`、发布图形 `fps_` 三处改用；seed 仍含平面图 ID（图形/任务）或任务 ID + 候选 ID（发布），确定性与平面图域保留；候选/错误 id `fpic_/fpie_ + slug(taskId+"_"+n)` 随任务 ≤46；不改迁移、不动发布追加语义 | FloorPlanService 三处调用 + 助手；`derivedIdsFitVarchar64Columns`（现网键 `fpi-1757570000000` 与 UUID 键；RED 时候选 id 68 而红） | PASS |
| 003 | 五处 `currentMerchantScope()` → `currentMerchantIdQuietly()`（`AuthorizationService:440-455` 永不抛、平台管理员桥为空）：列表、`createFloorPlanOnce` 的 merchant_id、`validateCreateRequest` 的 scope 校验、`checkProjectOwnership`、`checkMerchantOwnership`；商户 scope 存在则不触发项目 scope，平台管理员三者皆空仍全量 | FloorPlanService 五行 + 注释；测试桩改为生产语义（项目员工 `currentMerchantScope` 抛 MERCHANT_SCOPE_REQUIRED、商户员工 `currentProjectScopeQuietly` 抛 PROJECT_SCOPE_REQUIRED、平台管理员桥三者皆空）；原 003 三例 RED 时得 MERCHANT_SCOPE_REQUIRED 而红，商户三例在项目桩会抛的前提下仍绿 | PASS |

版本铁律：两个改动文件版本头 V0.01→V0.02，随所属 ID 归因。

### 4.2 范围归因（§5 闸 3）

- 全部 hunk 落到 001/003；版本头行随所属文件；归因不到的行 0。
- 未改：迁移、OpenAPI、三份业务 HTML、锁/sidecar、权限种子、FieldLabels、PlatformController、前端、ticketing、其它模块源码、其它 7 个 B3-r1 已改文件（diffstat 仅 2 文件）。
- 未提交/推送/合并/部署。

### 4.3 结论

内容修复：**PASS**。范围归因：**PASS**。总：**PASS**（作者自审票；须 Grok B4-r2 亦 PASS 才进 B5-r1；B3 累计 2/3）。本席在本报告落盘前未读取 `M015-b4-r2-grok-raw.md`。
