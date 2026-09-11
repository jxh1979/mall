CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M017 B4-r1 修复内容复审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B4-r1（B3 第 1 次整批修复的完整 diff 复审）。2026-09-11T10:57:01Z
- before tree：`5dedaf48e6d9fdccdb3bf8ca8959b4caa8b1cfed`；after tree：`03981e7c1bcfe2a5db58169d37475aede7c85c94`；`git diff --binary 5dedaf48e6d9 03981e7c1bcf` 全文 34184 bytes SHA256 ebd46c56447d7bc79b3cf1cf07e549f78edc37db406e3dcd41d84eedefef5bbf（全文附于本报告末尾）。
- 定稿依据 findings V1.99「当前 M017 B2-r1 双PASS定稿」：是＝M017-001…008（全部只改 5 个测试文件）；否＝009、010。B3 累计 1/3。TDD 口径（测试模块）：新例对真实实现全绿（主工作区 5 类 83 例，TEST-*.xml 计数）+ 一次性副本 /tmp/m017_dry 上 6 个实现变异体 6/6 被对应新例击红（/tmp/m017_b3_dry.log）；实现零改动。
- 锁四值：实算/sidecar/ledger/Grok 均 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。

## 1. diffstat

```
.../floorplan/FloorPlanControllerTests.java        |  77 +++++++++-
 .../floorplan/FloorPlanImportParserTests.java      |  81 +++++++++++
 .../floorplan/FloorPlanJdbcIntegrationTests.java   |  35 ++++-
 .../platform/floorplan/FloorPlanServiceTests.java  | 155 ++++++++++++++++++++-
 .../governance/VenueBusinessHoursServiceTests.java |  92 ++++++++++--
 5 files changed, 422 insertions(+), 18 deletions(-)
```

## 2. 逐块归因（每一行改动都归到定稿 ID；「版本铁律」行随其所在文件的 ID）

| 文件 | 块 | 位置 | 块内标记 ID | 归因 |
|---|---|---|---|---|
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | 003（三处桩 (…,1,1,1) + page_size 断言）、004（8 端点 403 + saveShape/createImport 缺幂等键 400 + 四个请求体常量）；版本头 V0.01 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java | 2 | `@@ -148,25 +149,28 @@ class FloorPlanControllerTests {` | M017-003 | 003（三处桩 (…,1,1,1) + page_size 断言）、004（8 端点 403 + saveShape/createImport 缺幂等键 400 + 四个请求体常量）；版本头 V0.01 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java | 3 | `@@ -228,13 +232,14 @@ class FloorPlanControllerTests {` | — | 003（三处桩 (…,1,1,1) + page_size 断言）、004（8 端点 403 + saveShape/createImport 缺幂等键 400 + 四个请求体常量）；版本头 V0.01 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java | 4 | `@@ -290,6 +295,66 @@ class FloorPlanControllerTests {` | M017-004 | 003（三处桩 (…,1,1,1) + page_size 断言）、004（8 端点 403 + saveShape/createImport 缺幂等键 400 + 四个请求体常量）；版本头 V0.01 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | 001（10 个错误码负例 + SVG 越界 + 默认值）、006（POINTS_TOO_FEW 中文 errorMessage）；版本头 V0.01 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java | 2 | `@@ -109,4 +110,84 @@ class FloorPlanImportParserTests {` | M017-001, M017-006 | 001（10 个错误码负例 + SVG 越界 + 默认值）、006（POINTS_TOO_FEW 中文 errorMessage）；版本头 V0.01 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | 005（import_candidates booth/public_area 接受、corridor 被 CHECK 拒 + candidate 辅助）、007（projectExists 走真实 projects 表）；版本头 V0.03 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java | 2 | `@@ -200,6 +200,39 @@ class FloorPlanJdbcIntegrationTests {` | M017-005, M017-007 | 005（import_candidates booth/public_area 接受、corridor 被 CHECK 拒 + candidate 辅助）、007（projectExists 走真实 projects 表）；版本头 V0.03 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | 002（不覆盖既有图形、失败任务、候选归属/不存在、上限 201 与空列表、列表详情审计）、003（listFloorPlans/listImportTasks 诚实分页）、005（corridor 负例）、006（四处中文整句断言，含 1,000,001 字符「导入内容超过 100 万字符上限」）+ importRequest(key,payload) 辅助；版本头 V0.04 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 2 | `@@ -98,8 +98,11 @@ class FloorPlanServiceTests {` | M017-006 | 002（不覆盖既有图形、失败任务、候选归属/不存在、上限 201 与空列表、列表详情审计）、003（listFloorPlans/listImportTasks 诚实分页）、005（corridor 负例）、006（四处中文整句断言，含 1,000,001 字符「导入内容超过 100 万字符上限」）+ importRequest(key,payload) 辅助；版本头 V0.04 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 3 | `@@ -546,6 +549,148 @@ class FloorPlanServiceTests {` | M017-002, M017-003, M017-005, M017-006 | 002（不覆盖既有图形、失败任务、候选归属/不存在、上限 201 与空列表、列表详情审计）、003（listFloorPlans/listImportTasks 诚实分页）、005（corridor 负例）、006（四处中文整句断言，含 1,000,001 字符「导入内容超过 100 万字符上限」）+ importRequest(key,payload) 辅助；版本头 V0.04 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 4 | `@@ -573,6 +718,10 @@ class FloorPlanServiceTests {` | — | 002（不覆盖既有图形、失败任务、候选归属/不存在、上限 201 与空列表、列表详情审计）、003（listFloorPlans/listImportTasks 诚实分页）、005（corridor 负例）、006（四处中文整句断言，含 1,000,001 字符「导入内容超过 100 万字符上限」）+ importRequest(key,payload) 辅助；版本头 V0.04 随本文件 |
| backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java | 1 | `@@ -1,14 +1,15 @@` | 版本铁律（随所属ID） | 006（两处中文整句断言）、007（@Nested 真实仓库 spaces 用例 + 导入）、008（真实 AuditEventService + 内存审计库、5 处弱断言加严、审计事件例、admin 全量 + effectiveFrom 例）；版本头 V0.02 随本文件 |
| backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java | 2 | `@@ -16,7 +17,12 @@ import java.util.ArrayList;` | — | 006（两处中文整句断言）、007（@Nested 真实仓库 spaces 用例 + 导入）、008（真实 AuditEventService + 内存审计库、5 处弱断言加严、审计事件例、admin 全量 + effectiveFrom 例）；版本头 V0.02 随本文件 |
| backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java | 3 | `@@ -65,6 +71,7 @@ class VenueBusinessHoursServiceTests {` | — | 006（两处中文整句断言）、007（@Nested 真实仓库 spaces 用例 + 导入）、008（真实 AuditEventService + 内存审计库、5 处弱断言加严、审计事件例、admin 全量 + effectiveFrom 例）；版本头 V0.02 随本文件 |
| backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java | 4 | `@@ -73,7 +80,9 @@ class VenueBusinessHoursServiceTests {` | M017-008 | 006（两处中文整句断言）、007（@Nested 真实仓库 spaces 用例 + 导入）、008（真实 AuditEventService + 内存审计库、5 处弱断言加严、审计事件例、admin 全量 + effectiveFrom 例）；版本头 V0.02 随本文件 |
| backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java | 5 | `@@ -147,23 +156,90 @@ class VenueBusinessHoursServiceTests {` | M017-006, M017-007, M017-008 | 006（两处中文整句断言）、007（@Nested 真实仓库 spaces 用例 + 导入）、008（真实 AuditEventService + 内存审计库、5 处弱断言加严、审计事件例、admin 全量 + effectiveFrom 例）；版本头 V0.02 随本文件 |

## 3. 红绿证据（B3-r1 运行记录 /tmp/m017_b3r1_run.log 原文）

```
START=2026-09-11T10:42:22Z HEAD=e2abb037
== apply test additions (five test classes; implementation untouched)
FloorPlanImportParserTests.java a6e0f52b13a11a0d
FloorPlanServiceTests.java 9fa9ecad75631864
FloorPlanControllerTests.java acd1f485522c9c31
FloorPlanJdbcIntegrationTests.java 5a5932ce4bf3dc12
VenueBusinessHoursServiceTests.java d8b3df0e5d5ad082
TESTS_PATCH_OK
GREEN_EXIT=0
floorplan.FloorPlanControllerTests testcases=14 failed=0
floorplan.FloorPlanImportParserTests testcases=14 failed=0
floorplan.FloorPlanJdbcIntegrationTests testcases=7 failed=0
floorplan.FloorPlanServiceTests testcases=36 failed=0
governance.VenueBusinessHoursServiceTests testcases=12 failed=0
XML_TOTAL=83 XML_FAILED=0
== mutation evidence (disposable copy /tmp/m017_dry, six implementation mutants; main worktree implementation untouched)
10:GREEN_EXIT=0
17:mutant_lines=src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:1 src/main/java/com/complex/platform/floorplan/FloorPlanService.java:3 src/main/ja
18:MUTANT_EXIT=1
20:floorplan.FloorPlanImportParserTests.txt:Tests run: 14, Failures: 1, Errors: 0, Skipped: 0,
22:floorplan.FloorPlanServiceTests.txt:Tests run: 36, Failures: 3, Errors: 0, Skipped: 0,
== post-B3 fingerprints
backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java 472 acd1f485522c9c31 tests=15
backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java 193 a6e0f52b13a11a0d tests=14
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java 277 5a5932ce4bf3dc12 tests=7
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java 864 9fa9ecad75631864 tests=36
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java 251 d8b3df0e5d5ad082 tests=12
implementation changed: 0
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
 M backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
 M docs/audit-2026-09/findings.md
 M docs/audit-2026-09/ledger.md
?? docs/audit-2026-09/modules/M017-b1-grok-raw.md
?? docs/audit-2026-09/modules/M017-b1-self-raw.md
END=2026-09-11T10:42:35Z
```

## 4. 本席复审结论

见文末追加的判定段。

## 附：完整 diff（before→after，--binary）

```diff
diff --git a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java
index 090f8b9c..574cfff9 100644
--- a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java
+++ b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.mockito.ArgumentMatchers.any;
@@ -148,25 +149,28 @@ class FloorPlanControllerTests {
 
     @Test
     void listUsesViewPermissionAndPageShape() throws Exception {
-        when(service.listFloorPlans()).thenReturn(new PageResponse<>(List.of(floorPlan()), 1, 50, 1));
+        // M017-003：桩取诚实不分页形状（M015-007），并断言 page_size 出网
+        when(service.listFloorPlans()).thenReturn(new PageResponse<>(List.of(floorPlan()), 1, 1, 1));
 
         mockMvc.perform(get("/api/v1/floor-plans")
                         .with(user("ops").roles("P0_SMOKE")))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.data.items[0].id").value("fp_1"))
-                .andExpect(jsonPath("$.data.total").value(1));
+                .andExpect(jsonPath("$.data.total").value(1))
+                .andExpect(jsonPath("$.data.page_size").value(1));
 
         verify(authorizationService).requirePermission("floor_plan.view");
     }
 
     @Test
     void listShapesUsesViewPermission() throws Exception {
-        when(service.listShapes("fp_1")).thenReturn(new PageResponse<>(List.of(shape()), 1, 50, 1));
+        when(service.listShapes("fp_1")).thenReturn(new PageResponse<>(List.of(shape()), 1, 1, 1));
 
         mockMvc.perform(get("/api/v1/floor-plans/fp_1/shapes")
                         .with(user("ops").roles("P0_SMOKE")))
                 .andExpect(status().isOk())
-                .andExpect(jsonPath("$.data.items[0].id").value("fps_1"));
+                .andExpect(jsonPath("$.data.items[0].id").value("fps_1"))
+                .andExpect(jsonPath("$.data.page_size").value(1));
 
         verify(authorizationService).requirePermission("floor_plan.view");
     }
@@ -228,13 +232,14 @@ class FloorPlanControllerTests {
 
     @Test
     void listImportsUsesImportViewPermission() throws Exception {
-        when(service.listImportTasks("fp_1")).thenReturn(new PageResponse<>(List.of(importTask()), 1, 50, 1));
+        when(service.listImportTasks("fp_1")).thenReturn(new PageResponse<>(List.of(importTask()), 1, 1, 1));
 
         mockMvc.perform(get("/api/v1/floor-plans/fp_1/imports")
                         .with(user("ops").roles("P0_SMOKE")))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.data.items[0].id").value("fpi_1"))
-                .andExpect(jsonPath("$.data.total").value(1));
+                .andExpect(jsonPath("$.data.total").value(1))
+                .andExpect(jsonPath("$.data.page_size").value(1));
 
         verify(authorizationService).requirePermission("floor_plan.import.view");
     }
@@ -290,6 +295,66 @@ class FloorPlanControllerTests {
         verify(authorizationService).requirePermission("floor_plan.import.manage");
     }
 
+    // M017-004：8 个端点权限被拒时统一 403 PERMISSION_DENIED（修复前只有 create 一例）
+    @Test
+    void everyEndpointReturnsForbiddenWhenPermissionDenied() throws Exception {
+        record Call(String permission, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) {
+        }
+        List<Call> calls = List.of(
+                new Call("floor_plan.view", get("/api/v1/floor-plans")),
+                new Call("floor_plan.manage", post("/api/v1/floor-plans").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY)),
+                new Call("floor_plan.view", get("/api/v1/floor-plans/fp_1/shapes")),
+                new Call("floor_plan.manage", post("/api/v1/floor-plans/fp_1/shapes").contentType(MediaType.APPLICATION_JSON).content(SHAPE_BODY)),
+                new Call("floor_plan.import.view", get("/api/v1/floor-plans/fp_1/imports")),
+                new Call("floor_plan.import.manage", post("/api/v1/floor-plans/fp_1/imports").contentType(MediaType.APPLICATION_JSON).content(IMPORT_BODY)),
+                new Call("floor_plan.import.view", get("/api/v1/floor-plans/fp_1/imports/fpi_1/candidates")),
+                new Call("floor_plan.import.manage", post("/api/v1/floor-plans/fp_1/imports/fpi_1/publish").contentType(MediaType.APPLICATION_JSON).content(PUBLISH_BODY))
+        );
+        for (Call call : calls) {
+            Mockito.reset(authorizationService);
+            doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
+                    .when(authorizationService)
+                    .requirePermission(call.permission());
+
+            mockMvc.perform(call.request().with(user("ops").roles("P0_SMOKE")))
+                    .andExpect(status().isForbidden())
+                    .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
+        }
+    }
+
+    // M017-004：saveShape / createImport 缺幂等键 → 400 VALIDATION_FAILED（修复前只有 create/publish 两例）
+    @Test
+    void saveShapeAndCreateImportWithoutIdempotencyKeyReturnValidationFailed() throws Exception {
+        mockMvc.perform(post("/api/v1/floor-plans/fp_1/shapes")
+                        .with(user("ops").roles("P0_SMOKE"))
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content(SHAPE_BODY.replace("\"idempotency_key\": \"shape-save\",", "")))
+                .andExpect(status().isBadRequest())
+                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
+        mockMvc.perform(post("/api/v1/floor-plans/fp_1/imports")
+                        .with(user("ops").roles("P0_SMOKE"))
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content(IMPORT_BODY.replace("\"idempotency_key\": \"floor-import-create\",", "")))
+                .andExpect(status().isBadRequest())
+                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
+    }
+
+    private static final String CREATE_BODY = """
+            {"idempotency_key": "floor-plan-create", "project_id": "prj_1", "floor_no": "1F", "plan_name": "1F plan",
+             "image_url": "https://assets.example.com/floor-1.png", "image_width": 1600, "image_height": 1200,
+             "version_no": "v1", "status": "active"}
+            """;
+    private static final String SHAPE_BODY = """
+            {"idempotency_key": "shape-save", "shape_type": "shop", "ref_type": "space", "ref_id": "sp_1", "label": "Shop 101",
+             "polygon_points": [{"x": 0.1, "y": 0.1}, {"x": 0.4, "y": 0.1}, {"x": 0.4, "y": 0.3}], "z_index": 10, "status": "active"}
+            """;
+    private static final String IMPORT_BODY = """
+            {"idempotency_key": "floor-import-create", "source_format": "svg", "source_name": "1F.svg", "source_payload": "<svg viewBox='0 0 10 10'><polygon points='1,1 2,1 2,2' /></svg>"}
+            """;
+    private static final String PUBLISH_BODY = """
+            {"idempotency_key": "floor-import-publish", "candidate_ids": ["fpic_1"]}
+            """;
+
     private FloorPlanRecord floorPlan() {
         return new FloorPlanRecord(
                 "fp_1",
diff --git a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java
index d10a1974..fcad00ed 100644
--- a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java
+++ b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 
@@ -109,4 +110,84 @@ class FloorPlanImportParserTests {
                 .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
                 .containsExactly("POINT_OUT_OF_RANGE");
     }
+
+    // M017-001：解析器错误码闭集 10 个逐一有可失败用例（修复前只锁 POLYLINE_NOT_CLOSED 与 JSON 侧 POINT_OUT_OF_RANGE）
+    @Test
+    void reportsPayloadRequiredForBlankPayload() {
+        assertThat(parser.parse("svg", "   ").errors())
+                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
+                .containsExactly("PAYLOAD_REQUIRED");
+    }
+
+    @Test
+    void reportsUnsupportedSourceFormat() {
+        assertThat(parser.parse("geojson", "{}").errors())
+                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
+                .containsExactly("UNSUPPORTED_SOURCE_FORMAT");
+    }
+
+    @Test
+    void reportsViewBoxRequiredForSvgWithoutViewBox() {
+        assertThat(parser.parse("svg", "<svg><polygon points=\"1,1 2,1 2,2\" /></svg>").errors())
+                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
+                .containsExactly("VIEWBOX_REQUIRED");
+    }
+
+    @Test
+    void reportsPointsRequiredForShapeWithoutPoints() {
+        assertThat(parser.parse("svg", "<svg viewBox=\"0 0 10 10\"><polygon id=\"a\" /></svg>").errors())
+                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
+                .containsExactly("POINTS_REQUIRED");
+    }
+
+    // M017-001 / M017-006：落库并展示给复核人的 error_message 为中文整句（M015-004）
+    @Test
+    void reportsPointsTooFewWithChineseMessage() {
+        FloorPlanImportParseResult result = parser.parse("svg", "<svg viewBox=\"0 0 10 10\"><polygon points=\"1,1 2,2\" /></svg>");
+
+        assertThat(result.candidates()).isEmpty();
+        assertThat(result.errors())
+                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode, FloorPlanImportParseResult.ErrorDraft::errorMessage)
+                .containsExactly(org.assertj.core.groups.Tuple.tuple("POINTS_TOO_FEW", "多边形至少需要 3 个顶点"));
+    }
+
+    @Test
+    void reportsNoShapesFoundForSvgWithoutPolygons() {
+        assertThat(parser.parse("svg", "<svg viewBox=\"0 0 10 10\"><rect width=\"1\" height=\"1\" /></svg>").errors())
+                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
+                .containsExactly("NO_SHAPES_FOUND");
+    }
+
+    @Test
+    void reportsJsonParseFailedForMalformedJson() {
+        assertThat(parser.parse("standard_json", "{not json").errors())
+                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
+                .containsExactly("JSON_PARSE_FAILED");
+    }
+
+    @Test
+    void reportsShapesRequiredForJsonWithoutShapesArray() {
+        assertThat(parser.parse("standard_json", "{\"foo\": 1}").errors())
+                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
+                .containsExactly("SHAPES_REQUIRED");
+    }
+
+    @Test
+    void reportsSvgPointOutOfRangeAgainstViewBox() {
+        assertThat(parser.parse("svg", "<svg viewBox=\"0 0 100 100\"><polygon points=\"0,0 150,0 150,50\" /></svg>").errors())
+                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
+                .containsExactly("POINT_OUT_OF_RANGE");
+    }
+
+    @Test
+    void appliesSvgDefaultsForLabelShapeTypeAndRefType() {
+        FloorPlanImportParseResult result = parser.parse("svg", "<svg viewBox=\"0 0 100 100\"><polygon points=\"10,10 20,10 20,20\" /></svg>");
+
+        assertThat(result.errors()).isEmpty();
+        FloorPlanImportParseResult.CandidateDraft candidate = result.candidates().getFirst();
+        assertThat(candidate.label()).isEqualTo("shape-1");
+        assertThat(candidate.shapeType()).isEqualTo("shop");
+        assertThat(candidate.refType()).isEqualTo("custom");
+        assertThat(candidate.refId()).isNull();
+    }
 }
diff --git a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
index 9bbcb755..30b3866d 100644
--- a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.floorplan;
-// 文件版本：V0.02
+// 文件版本：V0.03
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -200,6 +200,39 @@ class FloorPlanJdbcIntegrationTests {
                 .isInstanceOf(DataIntegrityViolationException.class);
     }
 
+    // M017-007：projectExists 走真实 projects 表（内存仓夹具掩盖不了 SQL）
+    @Test
+    void projectExistsQueriesRealProjectsTable() {
+        assertThat(repository.projectExists("prj_jdbc_missing")).isFalse();
+        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
+                + "VALUES ('prj_jdbc_real', 'mall', '测试综合体', 'active', now(), now())");
+
+        assertThat(repository.projectExists("prj_jdbc_real")).isTrue();
+    }
+
+    // M017-005：导入候选表的 shape_type 八值 CHECK（V423）——booth/public_area 可落库、非闭集值被拒（M016-004 观察）
+    @Test
+    void importCandidateShapeTypeFollowsDatabaseCheck() {
+        Instant now = Instant.parse("2026-06-14T00:00:00Z");
+        repository.saveFloorPlan(floorPlan("fp_cand_types", "prj_jdbc", now));
+        repository.saveImportTask(new FloorPlanImportTaskRecord("fpi_cand_types", "fp_cand_types", "svg", "1F.svg", "hash",
+                "<svg />", "parsed", 2, 0, null, now, now));
+        repository.saveImportCandidate(candidate("fpic_cand_booth", "fpi_cand_types", 1, "booth", now));
+        repository.saveImportCandidate(candidate("fpic_cand_public", "fpi_cand_types", 2, "public_area", now));
+
+        assertThat(repository.findImportCandidatesByTaskId("fpi_cand_types"))
+                .extracting(FloorPlanImportCandidateRecord::shapeType)
+                .containsExactlyInAnyOrder("booth", "public_area");
+        assertThatThrownBy(() -> repository.saveImportCandidate(candidate("fpic_cand_corridor", "fpi_cand_types", 3, "corridor", now)))
+                .isInstanceOf(DataIntegrityViolationException.class);
+    }
+
+    private static FloorPlanImportCandidateRecord candidate(String id, String taskId, int no, String shapeType, Instant now) {
+        return new FloorPlanImportCandidateRecord(id, taskId, no, shapeType, "space", null, shapeType + " candidate",
+                List.of(new FloorPlanPoint(0.1, 0.1), new FloorPlanPoint(0.4, 0.1), new FloorPlanPoint(0.4, 0.3)),
+                0, "pending", now, now);
+    }
+
     private static FloorPlanRecord floorPlan(String id, String projectId, Instant now) {
         return new FloorPlanRecord(id, projectId, "A", "1F", "1F plan", "https://assets.example.com/floor-1.png",
                 1600, 1200, "v1", "active", now, now, null);
diff --git a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
index ef4b7afb..9f3fb0ab 100644
--- a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.floorplan;
-// 文件版本：V0.03
+// 文件版本：V0.04
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
@@ -98,8 +98,11 @@ class FloorPlanServiceTests {
                 "shape-two-points",
                 null,
                 List.of(new FloorPlanPoint(0.1, 0.1), new FloorPlanPoint(0.2, 0.2))
-        ))).isInstanceOfSatisfying(ApiException.class, exception ->
-                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
+        ))).isInstanceOfSatisfying(ApiException.class, exception -> {
+            assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
+            // M017-006：M015-006 已改的中文整句
+            assertThat(exception.getMessage()).isEqualTo("多边形至少需要 3 个顶点");
+        });
     }
 
     @Test
@@ -546,6 +549,148 @@ class FloorPlanServiceTests {
         );
     }
 
+    // M017-002：发布「不可静默覆盖既有布局证据」（治理地图 L472）——发布前已有图形须并存且原样
+    @Test
+    void publishImportTaskKeepsExistingShapes() {
+        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-publish-keep"));
+        FloorPlanShapeRecord existing = service.saveShape(floorPlan.id(), shapeRequest("shape-existing", null, polygon()));
+        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(), importRequest("fp-import-keep"));
+        String candidateId = repository.findImportCandidatesByTaskId(task.id()).getFirst().id();
+
+        service.publishImportTask(floorPlan.id(), task.id(),
+                new FloorPlanImportPublishRequest("publish-keep", "publish import", List.of(candidateId)));
+
+        assertThat(repository.findShapesByFloorPlanId(floorPlan.id()))
+                .extracting(FloorPlanShapeRecord::label)
+                .containsExactlyInAnyOrder("Shop 101", "A101");
+        assertThat(repository.findShapeById(existing.id())).contains(existing);
+    }
+
+    // M017-002：解析失败的导入任务落 failed、错误落库，且不可发布
+    @Test
+    void importTaskWithOnlyErrorsIsFailedAndCannotBePublished() {
+        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-import-failed"));
+        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(),
+                importRequest("fp-import-failed-key", "<svg viewBox=\"0 0 1000 800\"><polygon id=\"bad\" points=\"1,1 2,2\" /></svg>"));
+
+        assertThat(task.status()).isEqualTo("failed");
+        assertThat(task.candidateCount()).isZero();
+        assertThat(task.errorCount()).isEqualTo(1);
+        assertThat(repository.findImportErrorsByTaskId(task.id()))
+                .extracting(FloorPlanImportErrorRecord::errorCode)
+                .containsExactly("POINTS_TOO_FEW");
+        assertThatThrownBy(() -> service.publishImportTask(floorPlan.id(), task.id(),
+                new FloorPlanImportPublishRequest("publish-failed", "publish import", List.of("fpic_x"))))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
+    }
+
+    // M017-002 / M017-006：候选项必须属于该导入任务（中文整句）；不存在的候选项 → IMPORT_CANDIDATE_NOT_FOUND；两次拒绝都不落图形
+    @Test
+    void publishRejectsCandidateOfAnotherTaskOrUnknownCandidate() {
+        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-publish-owner"));
+        FloorPlanImportTaskRecord taskA = service.createImportTask(floorPlan.id(), importRequest("fp-import-owner-a"));
+        FloorPlanImportTaskRecord taskB = service.createImportTask(floorPlan.id(), importRequest("fp-import-owner-b"));
+        String candidateOfB = repository.findImportCandidatesByTaskId(taskB.id()).getFirst().id();
+
+        assertThatThrownBy(() -> service.publishImportTask(floorPlan.id(), taskA.id(),
+                new FloorPlanImportPublishRequest("publish-owner-1", "publish import", List.of(candidateOfB))))
+                .isInstanceOfSatisfying(ApiException.class, exception -> {
+                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
+                    assertThat(exception.getMessage()).isEqualTo("候选项不属于该导入任务");
+                });
+        assertThatThrownBy(() -> service.publishImportTask(floorPlan.id(), taskA.id(),
+                new FloorPlanImportPublishRequest("publish-owner-2", "publish import", List.of("fpic_missing"))))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("IMPORT_CANDIDATE_NOT_FOUND"));
+        assertThat(repository.findShapesByFloorPlanId(floorPlan.id())).isEmpty();
+    }
+
+    // M015-008 / M017-002 / M017-006：一次发布最多 200 个候选项；空列表拒绝（中文整句）
+    @Test
+    void publishRejectsMoreThanTwoHundredCandidatesAndEmptyList() {
+        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-publish-cap"));
+        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(), importRequest("fp-import-cap"));
+        List<String> tooMany = java.util.stream.IntStream.rangeClosed(1, 201).mapToObj(i -> "fpic_" + i).toList();
+
+        assertThatThrownBy(() -> service.publishImportTask(floorPlan.id(), task.id(),
+                new FloorPlanImportPublishRequest("publish-cap", "publish import", tooMany)))
+                .isInstanceOfSatisfying(ApiException.class, exception -> {
+                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
+                    assertThat(exception.getMessage()).isEqualTo("一次发布的候选项不能超过 200 个");
+                });
+        assertThatThrownBy(() -> service.publishImportTask(floorPlan.id(), task.id(),
+                new FloorPlanImportPublishRequest("publish-empty", "publish import", List.of())))
+                .isInstanceOfSatisfying(ApiException.class, exception -> {
+                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
+                    assertThat(exception.getMessage()).isEqualTo("候选项列表不能为空");
+                });
+    }
+
+    // M017-002：导入任务列表与详情（含错误清单）可读，并写审计事件
+    @Test
+    void listImportTasksAndDetailsReturnRecordsAndWriteAuditEvents() {
+        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-import-details"));
+        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(), importRequest("fp-import-details-key"));
+
+        assertThat(service.listImportTasks(floorPlan.id()).items())
+                .extracting(FloorPlanImportTaskRecord::id)
+                .containsExactly(task.id());
+        FloorPlanImportDetailsResponse details = service.getImportDetails(floorPlan.id(), task.id());
+
+        assertThat(details.task().id()).isEqualTo(task.id());
+        assertThat(details.candidates()).hasSize(1);
+        assertThat(details.errors()).isEmpty();
+        assertThat(auditEventRepository.events)
+                .extracting(AuditEvent::eventName)
+                .contains("floor_plan.import_viewed", "floor_plan.import_candidate_viewed");
+    }
+
+    // M017-006：导入内容上限文案（M015-006 已改中文整句）——1,000,001 字符被拒
+    @Test
+    void createImportTaskRejectsPayloadOverOneMillionCharsWithChineseMessage() {
+        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-import-huge"));
+        String huge = "<svg viewBox=\"0 0 10 10\">" + "x".repeat(1_000_001) + "</svg>";
+
+        assertThatThrownBy(() -> service.createImportTask(floorPlan.id(), importRequest("fp-import-huge-key", huge)))
+                .isInstanceOfSatisfying(ApiException.class, exception -> {
+                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
+                    assertThat(exception.getMessage()).isEqualTo("导入内容超过 100 万字符上限");
+                });
+    }
+
+    // M017-003：平面图列表与导入任务列表同样诚实不分页（M015-007 三处；修复前只锁 listShapes）
+    @Test
+    void listFloorPlansAndImportTasksReportHonestPageSizeAndTotal() {
+        FloorPlanRecord planA = service.createFloorPlan(createRequest("fp-honest-a"));
+        service.createFloorPlan(createRequest("fp-honest-b"));
+        service.createImportTask(planA.id(), importRequest("fp-honest-import-1"));
+        service.createImportTask(planA.id(), importRequest("fp-honest-import-2"));
+
+        PageResponse<FloorPlanRecord> plans = service.listFloorPlans();
+        PageResponse<FloorPlanImportTaskRecord> tasks = service.listImportTasks(planA.id());
+
+        assertThat(plans.items()).hasSize(2);
+        assertThat(plans.total()).isEqualTo(2);
+        assertThat(plans.pageSize()).isEqualTo(2);
+        assertThat(tasks.items()).hasSize(2);
+        assertThat(tasks.total()).isEqualTo(2);
+        assertThat(tasks.pageSize()).isEqualTo(2);
+    }
+
+    // M017-005 / M017-006：非闭集的候选区域类型（如 corridor）须被服务白名单拒绝（中文整句）；corridor 不进白名单
+    @Test
+    void saveShapeRejectsShapeTypeOutsideClosedSet() {
+        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-corridor"));
+
+        assertThatThrownBy(() -> service.saveShape(floorPlan.id(), shapeRequestOfType("shape-corridor", "corridor")))
+                .isInstanceOfSatisfying(ApiException.class, exception -> {
+                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
+                    assertThat(exception.getMessage()).isEqualTo("图形类型取值无效");
+                });
+        assertThat(repository.findShapesByFloorPlanId(floorPlan.id())).isEmpty();
+    }
+
     // M016-002（M015-013 同批）：候选区域类型「摊位」「公共区」须被服务白名单接受（修复前白名单六值 → 此例红）
     @Test
     void saveShapeAcceptsBoothAndPublicArea() {
@@ -573,6 +718,10 @@ class FloorPlanServiceTests {
         );
     }
 
+    private FloorPlanImportCreateRequest importRequest(String idempotencyKey, String svgPayload) {
+        return new FloorPlanImportCreateRequest(idempotencyKey, "import svg", "svg", "1F.svg", svgPayload);
+    }
+
     private FloorPlanImportCreateRequest importRequest(String idempotencyKey) {
         return new FloorPlanImportCreateRequest(
                 idempotencyKey,
diff --git a/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java b/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
index 2c6d2ce6..302a5cdc 100644
--- a/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
@@ -1,14 +1,15 @@
 package com.complex.platform.governance;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
-import static org.mockito.Mockito.mock;
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
+import com.complex.platform.evidence.AuditEvent;
 import com.complex.platform.evidence.AuditEventService;
 import com.complex.platform.governance.dto.VenueBusinessHoursRequest;
+import com.complex.platform.support.InMemoryAuditEventRepository;
 import java.time.Instant;
 import java.time.LocalDate;
 import java.time.LocalTime;
@@ -16,7 +17,12 @@ import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
+import org.junit.jupiter.api.Nested;
 import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.context.SpringBootTest;
+import org.springframework.jdbc.core.JdbcTemplate;
+import org.springframework.transaction.annotation.Transactional;
 
 /**
  * D1 场地营业时间配置的入参闸（V226 建表后一直无界面，本页是唯一配置入口）。
@@ -65,6 +71,7 @@ class VenueBusinessHoursServiceTests {
     }
 
     private RecordingRepository repository;
+    private InMemoryAuditEventRepository auditEventRepository;
     private VenueBusinessHoursService service;
 
     private VenueBusinessHoursService service() {
@@ -73,7 +80,9 @@ class VenueBusinessHoursServiceTests {
 
     private VenueBusinessHoursService service(String projectScope) {
         repository = new RecordingRepository();
-        service = new VenueBusinessHoursService(repository, mock(AuditEventService.class), auth(projectScope));
+        // M017-008：真实 AuditEventService + 内存审计库（与 FloorPlanServiceTests 同构），审计事件可断言
+        auditEventRepository = new InMemoryAuditEventRepository();
+        service = new VenueBusinessHoursService(repository, new AuditEventService(auditEventRepository), auth(projectScope));
         return service;
     }
 
@@ -147,23 +156,90 @@ class VenueBusinessHoursServiceTests {
     @Test
     void rejectsDayOfWeekOutOfRange() {
         assertThatThrownBy(() -> service().save(request(7, "18:00", "23:00")))
-                .isInstanceOf(ApiException.class);
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
         assertThatThrownBy(() -> service().save(request(null, "18:00", "23:00")))
-                .isInstanceOf(ApiException.class);
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
     }
 
     @Test
     void rejectsMalformedTimeAndMissingVenue() {
         assertThatThrownBy(() -> service().save(request(1, "晚上八点", "23:00")))
-                .isInstanceOf(ApiException.class);
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
         assertThatThrownBy(() -> service().save(request(1, "18:00", null)))
-                .isInstanceOf(ApiException.class);
+                .isInstanceOfSatisfying(ApiException.class, ex -> {
+                    assertThat(ex.code()).isEqualTo("VALIDATION_FAILED");
+                    // M017-006：M015-006 已改的中文整句
+                    assertThat(ex.getMessage()).isEqualTo("结束营业时间必须填写，格式 HH:mm");
+                });
         assertThatThrownBy(() -> service().save(
                 new VenueBusinessHoursRequest("idem-1", "  ", 1, "18:00", "23:00", false, null)))
-                .isInstanceOf(ApiException.class);
+                .isInstanceOfSatisfying(ApiException.class, ex -> {
+                    assertThat(ex.code()).isEqualTo("VALIDATION_FAILED");
+                    assertThat(ex.getMessage()).isEqualTo("场地不能为空");
+                });
         assertThat(repository.saved).isEmpty();
     }
 
+    // M017-008：保存/列表须写审计事件（GEN-011；修复前 mock 从未 verify）
+    @Test
+    void saveAndListWriteAuditEvents() {
+        VenueBusinessHoursService svc = service();
+        svc.save(request(3, "18:00", "23:00"));
+        svc.list();
+
+        assertThat(auditEventRepository.events)
+                .extracting(AuditEvent::eventName)
+                .containsExactly("venue_business_hours.updated", "venue_business_hours.viewed");
+    }
+
+    // M017-008：平台管理员（项目 scope 为空）列表取全量，且 effective_from 原样落库
+    @Test
+    void adminListsAllVenuesAndEffectiveFromIsPersisted() {
+        VenueBusinessHoursService svc = service();
+        VenueBusinessHoursRecord saved = svc.save(
+                new VenueBusinessHoursRequest("idem-1", "sp_bar_main", 4, "18:00", "23:00", false, LocalDate.of(2026, 10, 1)));
+        svc.save(new VenueBusinessHoursRequest("idem-2", "sp_bar_side", 4, "18:00", "23:00", false, null));
+
+        assertThat(saved.effectiveFrom()).isEqualTo(LocalDate.of(2026, 10, 1));
+        assertThat(svc.list()).extracting(VenueBusinessHoursRecord::venueId).containsExactlyInAnyOrder("sp_bar_main", "sp_bar_side");
+        assertThat(repository.projectQueries).isEmpty();
+    }
+
+    // M017-007：真实仓库 SQL（spaces / venue_business_hours 表）——不存在场地 empty、存在场地返回项目、按项目过滤；事务回滚
+    @Nested
+    @SpringBootTest(properties = {
+            "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
+            "spring.datasource.username=complex",
+            "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
+    })
+    @Transactional
+    class RealRepositoryCases {
+
+        @Autowired
+        private VenueBusinessHoursRepository realRepository;
+
+        @Autowired
+        private JdbcTemplate jdbcTemplate;
+
+        @Test
+        void findVenueProjectIdAndListByProjectHitRealSpacesTable() {
+            jdbcTemplate.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at) VALUES "
+                    + "('sp_vbh_a', 'prj_vbh_1', '吧台 A', 'shop', 'active', now(), now()), "
+                    + "('sp_vbh_b', 'prj_vbh_2', '吧台 B', 'shop', 'active', now(), now())");
+
+            assertThat(realRepository.findVenueProjectId("sp_vbh_missing")).isEmpty();
+            assertThat(realRepository.findVenueProjectId("sp_vbh_a")).contains("prj_vbh_1");
+
+            Instant now = Instant.parse("2026-06-14T00:00:00Z");
+            realRepository.upsert("vbh_real_a", "sp_vbh_a", 1, LocalTime.of(18, 0), LocalTime.of(23, 0), false, null, now);
+            realRepository.upsert("vbh_real_b", "sp_vbh_b", 1, LocalTime.of(18, 0), LocalTime.of(23, 0), false, null, now);
+
+            assertThat(realRepository.listByProject("prj_vbh_1"))
+                    .extracting(VenueBusinessHoursRecord::venueId)
+                    .containsExactly("sp_vbh_a");
+        }
+    }
+
     @Test
     void savesDisabledDayWithoutDeletingConfiguration() {
         // 「本日不营业」= 停用，配置行保留可随时恢复（不物理删配置）。

```


## 4. 本席复审结论（席甲，作者自审票）

本席逐块读完上述 diff 全文（5 个测试文件，实现零改动），以 §2 归因表核对每一行；§3 为本席 B3-r1 运行记录原文（主工作区 5 类全绿、计数取 TEST-*.xml；一次性副本 6 个实现变异体 6/6 被对应新例击红）。

### 4.1 修复内容（逐 ID）

| ID | 修复内容 | 证据（diff 块 / 变异） | 本席票 |
|---|---|---|---|
| 001 | `FloorPlanImportParserTests` +10 例：8 个未锁错误码各一负例、SVG 越界、默认 label/shop/custom；POINTS_TOO_FEW 例断言中文 errorMessage | 变异 m1（文案改英文）→ `reportsPointsTooFewWithChineseMessage` 红 | PASS |
| 002 | `FloorPlanServiceTests` +5 例：先存图形再发布并存且原样、全错 SVG → failed/错误落库/不可发布、他任务候选 VALIDATION_FAILED + 不存在候选 IMPORT_CANDIDATE_NOT_FOUND、201 个与空列表、列表/详情 + 两条审计事件 | 变异 m2（去归属校验）、m3（放开上限）各击红对应例 | PASS |
| 003 | `listFloorPlansAndImportTasksReportHonestPageSizeAndTotal`；Controller 三处桩改 `(…,1,1,1)` 并断言 `$.data.page_size` | 变异 m6（listFloorPlans 谎报 50）→ 红 | PASS |
| 004 | `everyEndpointReturnsForbiddenWhenPermissionDenied`（8 端点循环 doThrow→403）、`saveShapeAndCreateImportWithoutIdempotencyKeyReturnValidationFailed`；四个请求体常量 | Controller 14/14 绿；权限码与 `requireIdempotencyKey` 顺序由实现决定 | PASS |
| 005 | 服务侧 corridor → VALIDATION_FAILED「图形类型取值无效」；Jdbc `import_candidates` booth/public_area 接受、corridor 被八值 CHECK 拒 | Jdbc 7/7 绿（DataIntegrityViolation 在事务末） | PASS |
| 006 | 中文整句 `getMessage()` 断言：多边形至少需要 3 个顶点、候选项不属于该导入任务、一次发布的候选项不能超过 200 个、候选项列表不能为空、图形类型取值无效、导入内容超过 100 万字符上限（`createImportTaskRejectsPayloadOverOneMillionCharsWithChineseMessage`，1,000,001 字符，Grok B2-r1 勘误建议）、结束营业时间必须填写，格式 HH:mm、场地不能为空；解析器 errorMessage 归 001 | 变异 m1 红；服务 36/36 绿 | PASS |
| 007 | Jdbc `projectExistsQueriesRealProjectsTable`（插入最小 projects 行）；Venue 同文件 `@Nested @SpringBootTest @Transactional RealRepositoryCases`（插入最小 spaces 行 → findVenueProjectId/listByProject） | 真实 SQL 在 complex_test 内回滚执行（XML 计数含 @Nested 例） | PASS |
| 008 | 真实 `AuditEventService(new InMemoryAuditEventRepository())`，`saveAndListWriteAuditEvents` 断言 updated/viewed；5 处 `isInstanceOf` 改 code==VALIDATION_FAILED；`adminListsAllVenuesAndEffectiveFromIsPersisted` | 变异 m4（改事件名）、m5（星期错误码）各击红 | PASS |

版本铁律：ParserTests/ControllerTests 新加 V0.01，ServiceTests V0.03→V0.04，JdbcIntegrationTests V0.02→V0.03，VenueTests V0.01→V0.02；随所属 ID 归因。

### 4.2 范围归因（§5 闸 3）

- 全部 hunk 落到定稿 001–008；版本头行随所属文件；辅助方法/常量随其服务的用例；归因不到的行 0。
- 未改：backend/src/main 任何实现、迁移、OpenAPI、三份业务 HTML、锁/sidecar、权限种子、前端；未新增测试文件（007 以同文件 @Nested 承载）。
- 009/010（观察）未落地：未改 Strings/FieldLabels、未加分页参数、未加二次确认字段、corridor 只作负例。
- 未提交/推送/合并/部署。

### 4.3 结论

内容修复：**PASS**。范围归因：**PASS**。总：**PASS**（作者自审票；须 Grok B4-r1 亦 PASS 才进 B5-r1；B3 累计 1/3）。本席在本报告落盘前未读取 `M017-b4-r1-grok-raw.md`。
