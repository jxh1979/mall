CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M017 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M017-b4-r1-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：任何 `M017-b4-r1-self-raw.md`（当前亦不存在）、席甲本次 B4 raw。定稿队列以 findings.md V1.99 顶部「当前 M017 B2-r1 双PASS定稿」为准。本轮只审该定稿修复队列「是」8 项（M017-001…008）的修复内容与 before/after 全文 diff；009、010 为队列「否」。本模块为后台验证（测试）类：修复面只有 5 个测试文件，实现（M015/M016 已闭环）零改动。TDD 口径为「新例对真实实现全绿 + 一次性副本上实现变异体各被对应新例击红」。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、8 个定稿「是」ID 是否按最小边界修好、是否最懒可行、是否引入新缺陷或新业务发明（冲突→细化→技术性→添加）、每一变更行能否归因到上述 ID、是否越界改三份业务 HTML / 锁 / sidecar / ledger 基准 / OpenAPI / 迁移 / 权限种子 / 前端 / 任何 `backend/src/main` 实现、是否新增测试文件、是否把 corridor 进白名单 / 加分页参数 / 加二次确认字段。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例（失败任务发布用假候选 ID、201 假 ID 是否真撞上限、listShapes 桩缺 total、corridor 白名单、开始营业时间未单锁），未冒充调用 grilling 子代理。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 正文一行同值。criteria-lock 版本仍 V0.11。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。ledger 顶部同字段同值（本席只读，不把 ledger 当哈希来源）。

| 项 | 值 |
|---|---|
| 模块 | M017 空间点位、楼层平面图与营业时段／后台验证／第1段（5 测试文件） |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 正式队列「是」（8） | 细化 002；技术性 001/003/004/005/006/007/008 |
| 正式队列「否」（2） | 009（观察：requireText 英文键为 FieldLabels 设计口径）；010（观察） |
| B3 | 已执行 1 次（本轮为第 1 次），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `5dedaf48e6d9fdccdb3bf8ca8959b4caa8b1cfed` |
| after（对象类型 tree，可解析） | `03981e7c1bcfe2a5db58169d37475aede7c85c94` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 5dedaf48e6d9fdccdb3bf8ca8959b4caa8b1cfed 03981e7c1bcfe2a5db58169d37475aede7c85c94`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 34184 bytes；SHA256 `ebd46c56447d7bc79b3cf1cf07e549f78edc37db406e3dcd41d84eedefef5bbf`；UTF-8；5 文件；422 插入 / 18 删除；`git diff` 608 行 / 17 hunk；name-status M×5 |
| 与 `/tmp/m017_b3_r1.diff` | 字节与 SHA256 全等（`cmp` identical；本席自算为准，副本仅对照） |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3744 / after 3744；`comm -3` 空（无新增/删除路径） |
| HEAD | `e2abb037001ccd72650efa3cd86e9b1b4bfe3ea0` = `origin/main`（PR #437 M016 闭环检查点已合并） |
| findings.md | 工作区 V1.99（797370 字节，SHA256 `a705f0397179c0820ad275020aae1e43b3307c2ed37fdb8091ac2ac62ec5b9e3`）；两树 blob 均为 `5d9b34a5531a7b51a78c77f9f61de203388cbb46`（本 diff 未改 findings）；HEAD blob `2479902f` 较旧 |
| ledger.md | 两树 blob 均为 `13d7a629025f3d2339663d97f6e9122330297999`（本 diff 未改）；工作区 blob `ec13d5886cb50a2a8ad28c043da7b557f3133926` 为冻结 after 树之后写入的台账，不在本轮修复内容 diff 内 |
| 三 HTML 两树 blob | BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3` / BEQR `86a717930f9303d4f131ee2864478baa14450e6d`，完全相同 |
| 锁/sidecar 两树 blob | 相同（未改）：lock `8db3fc02ce8a6dc998863a9784e7e88af511b960` / sidecar `41f575026b693af74d2a36e6faf86d260ef6f6ca` |
| OpenAPI | 两树 blob 相同：`docs/openapi/openapi-p0.yaml` `1f056b360dc60c37b6c026c761f0fa57b3688e63` / `docs/openapi/openapi-p0.json` `32231790707d8a93a303ca47033b7819ee0e3b71` |
| `backend/src/main` / 迁移 / 前端 / 权限种子 | 两树 `git diff --name-only -- backend/src/main apps docs/openapi backend/src/main/resources/db` 空 |

五文件 blob（工作区 `git hash-object` = after；B3 修复已在工作区，未入主索引）：

| 文件 | before | after / work | 版本头 |
|---|---|---|---|
| `backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java` | `090f8b9c3e93b95b8ef2c220eb6a0de29e6db4e1` | `574cfff99191b38689c0ae372d51c36678b32525` | （无）→V0.01 |
| `backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java` | `d10a197470cf480883d6c987d36da4acd1c4f0bd` | `fcad00ed3e4a0931e863c3a0508bd6804c89682c` | （无）→V0.01 |
| `backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java` | `9bbcb755277f8260f126b03796bb3272ed2df785` | `30b3866d5a0158ac2f91ebebdf4898d817b7d8a8` | V0.02→V0.03 |
| `backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java` | `ef4b7afbb98f63f4ea36efb9d935e84a854fa3cb` | `9f3fb0ab42d5eb82824e6d8d04b5a90f36fb7e8f` | V0.03→V0.04 |
| `backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java` | `2c6d2ce6cd58ebca3e06862a49947bc22fdeab70` | `302a5cdc1730c61f12c3c2ad644e0411c406610a` | V0.01→V0.02 |

内容 SHA256（与操作员 B3 指纹前 16 位一致，本席自算全文）：Controller `acd1f485522c9c31…aaa7aac`；Parser `a6e0f52b13a11a0d…1b758c`；Jdbc `5a5932ce4bf3dc12…dedaa59`；Service `9fa9ecad75631864…075a09`；Venue `d8b3df0e5d5ad082…9dfba9`。

`git diff --stat --binary`：`5 files changed, 422 insertions(+), 18 deletions(-)`。`git diff --name-status`：`M`×5。`git diff --numstat`：71/6 Controller；81/0 Parser；34/1 Jdbc；152/3 Service；84/8 Venue。

工作区相对 HEAD 另有 findings/ledger 已改、以及本模块 B1 双席 raw（未跟踪）。那些路径不在本轮 5 文件 diff 内（findings 两树 blob 全等），不按本轮修复内容审计。本席只审派发的 before/after 对。本席未改任何源文件，只新建本 raw。验证：主仓只读 mvn 测试（测试类写死 `complex_test`；`@Transactional` 回滚）+ `git show`/`git diff` + 一次性 `/tmp/m017_b4_grok_mut` 实现变异体（事后删除）。未 commit/push/PR/merge/部署。未对主索引 `git add`。未对任一 `complex_test*` 库做手工 INSERT/UPDATE/DDL。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M017 的集合。本轮只用定稿 001–008 已引用的 ID：GEN-003/009/PGM-023（001）、PGM-023/GEN-003/009/012（002）、GEN-003/008（003）、GEN-005/007/003（004）、GEN-003/008/PGM-023（005）、GEN-015/003/PGM-001（006）、GEN-003/BEQR-152/PGM-017（007）、GEN-011/003/BSM-003/PGM-002（008）。不 LOCK-AMEND。四分类本轮不重开。

B3-r1 边界（与 findings V1.99 顶部一致）：

- 只改本模块 5 个测试文件；不改任何实现/迁移/OpenAPI/三份 HTML/锁。
- 版本头：ParserTests/ControllerTests 新加 V0.01，ServiceTests V0.03→V0.04，JdbcIntegrationTests V0.02→V0.03，VenueTests V0.01→V0.02。
- TDD：新例对真实实现全绿 + 一次性副本上实现变异体各被对应新例击红。007 真实仓库用例以同文件 `@Nested @SpringBootTest @Transactional` 承载（surefire 文本报告对 @Nested 计 0 属报告器怪癖，以 TEST-*.xml testcase 为准）。
- 禁止：改 Strings/FieldLabels/FloorPlanService/Parser/Controller/VenueBusinessHoursService、加分页参数、把 corridor 进白名单、加二次确认字段、新增测试文件；把 009/010 当本批可修。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary 5dedaf48e6d9fdccdb3bf8ca8959b4caa8b1cfed 03981e7c1bcfe2a5db58169d37475aede7c85c94`

before tree = `5dedaf48e6d9fdccdb3bf8ca8959b4caa8b1cfed`

after tree = `03981e7c1bcfe2a5db58169d37475aede7c85c94`

字节 34184；SHA256 `ebd46c56447d7bc79b3cf1cf07e549f78edc37db406e3dcd41d84eedefef5bbf`；UTF-8 完整原文如下。

````diff
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

````

---

## 4. 技能自核（V0.17 §6，按 B4 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证（10 码闭集、失败任务发布顺序、201 假 ID、listShapes 缺 total、corridor 白名单、审计 mock），不向操作员发问、不派子代理 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步 mvn 为前台命令（主仓 ~6s / 副本 ~12s），未起 monitor |
| using-arc / Arc `audit` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/audit/SKILL.md` | 已读；会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |

本步未重读全文、按 B1 已核路径与 controller 收窄使用其原则（不冒称「本步已读 SKILL.md」）：

- `using-superpowers`：用户指令与 160 controller 优先于技能本体
- `verification-before-completion`：先跑 mvn / 变异再声称绿/红
- `diagnosing-bugs` / `systematic-debugging`：先建可红反馈环再下结论
- `ponytail-review`：只列可删复杂度，不落地——本批无额外抽象可删；403 用循环收拢是最懒
- `requesting-code-review`：B4 请审=只派本席；本席即外审，不再派第三席
- `git-guardrails-claude-code`：存在；本席未改 git、未直推

B4 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）、executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、MCP、第三主体。

缺口（不冒称调用）：`batch-grill-me`、`ubiquitous-language` 本机未作为独立技能核到。

---

## 5. 逐 ID 修复内容（001–008）

### M017-001（技术性 / 是）— PASS

定稿：每个未锁错误码各补一例最小负例、SVG 越界一例、默认值一例，至少一例断言 errorMessage 中文整句（POINTS_TOO_FEW「多边形至少需要 3 个顶点」）；不发明新错误码；只改 ParserTests，加版本头 V0.01。B2 非阻断：闭集 10、未锁 8 码各一例。

实现闭集 10 码（`FloorPlanImportParser.java`）均有可失败例：既有 POLYLINE_NOT_CLOSED、JSON 侧 POINT_OUT_OF_RANGE；新例 PAYLOAD_REQUIRED / UNSUPPORTED_SOURCE_FORMAT（`geojson`，与 M016-004 观察对齐且不把 geojson 放行）/ VIEWBOX_REQUIRED / POINTS_REQUIRED / POINTS_TOO_FEW（中文整句）/ NO_SHAPES_FOUND / JSON_PARSE_FAILED / SHAPES_REQUIRED / SVG 侧 POINT_OUT_OF_RANGE / 默认 label=`shape-1`、shape_type=`shop`、ref_type=`custom`。断言指向 `parser.parse` 真实返回，改码或改文案即红。未发明新错误码。最懒：只加负例，不改解析器。

### M017-002（细化 / 是）— PASS

定稿五例均落地于 `FloorPlanServiceTests`：

1. `publishImportTaskKeepsExistingShapes`：先存「Shop 101」再发布 SVG「A101」，`containsExactlyInAnyOrder` 两标签且 `findShapeById(existing.id()).contains(existing)`——覆盖即红。
2. `importTaskWithOnlyErrorsIsFailedAndCannotBePublished`：两点多边形 SVG → `failed` / candidateCount=0 / errorCount=1 / 错误落库 POINTS_TOO_FEW / 发布 VALIDATION_FAILED。实现先判 `status!="parsed"`（「只有解析完成的导入任务才能发布」）再查候选；例用假 `fpic_x` 仍锁失败任务不可发布（若删状态闸会变成 IMPORT_CANDIDATE_NOT_FOUND，本例红）。payload 稳定产 1 条错误，符合 B2 非阻断。
3. `publishRejectsCandidateOfAnotherTaskOrUnknownCandidate`：他任务候选 → VALIDATION_FAILED「候选项不属于该导入任务」；不存在 → IMPORT_CANDIDATE_NOT_FOUND；两次均无图形落库。变异体去归属校验后本例「Expecting code to raise a throwable」红。
4. `publishRejectsMoreThanTwoHundredCandidatesAndEmptyList`：201 个假 ID（上限校验在 `validateImportPublishRequest`、查找之前，不必造 201 行，最懒）→「一次发布的候选项不能超过 200 个」；空列表 →「候选项列表不能为空」。放上限后本例变成 IMPORT_CANDIDATE_NOT_FOUND 而红。
5. `listImportTasksAndDetailsReturnRecordsAndWriteAuditEvents`：列表含该任务、详情含 1 候选 0 错误，审计 `contains` `floor_plan.import_viewed` 与 `floor_plan.import_candidate_viewed`（create 另写事件，用 contains 而非 containsExactly，能失败）。

辅助 `importRequest(key, svg)` 专为 002 失败导入例与 006 百万字符例，归因 002/006。未改 2000/1000 上限（010 观察）。未加二次确认字段。

### M017-003（技术性 / 是）— PASS

`listFloorPlansAndImportTasksReportHonestPageSizeAndTotal`：两条列表 `pageSize()==total()==items.size()==2`。Controller 三处桩均改为 `(…,1,1,1)`；listFloorPlans / listImports 断言 `$.data.total` 与 `$.data.page_size`；listShapes 断言 `$.data.page_size`（原谎报字段）。未新增分页参数。变异体 listFloorPlans `pageSize=50` 被该服务例击红（expected 2 but was 50）。

非阻断余量（不构成没修好、不进回退）：listShapes 桩未加 `$.data.total`。旧桩 total 本已是 1，缺陷是 pageSize 谎报 50；page_size 已锁，且 M015-007 服务层 `listShapesReportsHonestPageSizeAndTotal` 已锁 pageSize==total。最小处置字面「各断言 total」未在第三处补上，属余量不是新缺陷。

### M017-004（技术性 / 是）— PASS

`FloorPlanController` 恰 8 个映射。`everyEndpointReturnsForbiddenWhenPermissionDenied` 循环 8 端点（含已有 create，收拢「其余 7」）`doThrow(forbidden)`→403 `PERMISSION_DENIED`。`saveShapeAndCreateImportWithoutIdempotencyKeyReturnValidationFailed` 去掉 JSON 幂等键 →400 `VALIDATION_FAILED`（日志 raw_message=`idempotency_key is required`，出网经 FieldLabels，夹具英文按 GEN-015 豁免）。常量 CREATE/SHAPE/IMPORT/PUBLISH_BODY 与 record Call 专为这两例，归因 004。未改 Controller 实现。

### M017-005（技术性 / 是）— PASS

`saveShapeRejectsShapeTypeOutsideClosedSet`：corridor → VALIDATION_FAILED「图形类型取值无效」，无图形落库；corridor 只作负例，白名单未改（本 diff 零 `src/main`）。`importCandidateShapeTypeFollowsDatabaseCheck`：`saveImportCandidate` booth/public_area 接受，corridor 被 CHECK 拒（`DataIntegrityViolationException`）。辅助 `candidate()` 归因 005。未把 corridor 进白名单。

### M017-006（技术性 / 是）— PASS

已改中文整句在 Service/Venue/Parser 有 `getMessage()`/`errorMessage` 断言（可并入 002/008 新例与既有例，符合最懒）：

| 定稿列出的已改文案 | 锁定位置 |
|---|---|
| 多边形至少需要 3 个顶点 | Parser POINTS_TOO_FEW；既有 `saveShapeRequiresAtLeastThreePolygonPoints` 补 getMessage |
| 候选项列表不能为空 | 002 空列表例 |
| 一次发布…200 | 002 201 例 |
| 候选项不属于… | 002 他任务例（「图形/候选项/导入任务不属于」家族代表） |
| 导入内容超过 100 万字符上限 | `createImportTaskRejectsPayloadOverOneMillionCharsWithChineseMessage`（1,000,001 字符，B2 非阻断口径） |
| 场地不能为空 / 结束营业时间必须填写，格式 HH:mm | Venue `rejectsMalformedTimeAndMissingVenue` |

解析器 errorMessage 归 001。未改 FieldLabels。未断言测试夹具/日志英文。

非阻断余量：「图形不属于该平面图」「导入任务不属于该平面图」「开始营业时间必须填写」未各写独立例；同家族/同 `parseTime` 模板已由代表句锁住，不是新业务发明。

### M017-007（技术性 / 是）— PASS

`projectExistsQueriesRealProjectsTable`：未知 id 假；插入最小 `projects(id,venue_type,name,status,created_at,updated_at)` 后真。Venue 同文件 `@Nested @SpringBootTest @Transactional RealRepositoryCases.findVenueProjectIdAndListByProjectHitRealSpacesTable`：插入最小 `spaces` 两行（`shop`/`active`/project_id），未知 empty、命中 `prj_vbh_1`、`listByProject` 只返本项目。不改生产 SQL。Jdbc/@Nested 均 `@Transactional` 回滚。主仓 XML 含该 nested 例且绿。

### M017-008（技术性 / 是）— PASS

`service(...)` 改为 `new AuditEventService(new InMemoryAuditEventRepository())`（与 FloorPlanServiceTests 同构）。`saveAndListWriteAuditEvents` 断言 `venue_business_hours.updated` 然后 `viewed`（改名即红）。五处弱断言改为 `code==VALIDATION_FAILED`（星期 7、星期 null、畸形时刻、缺结束时间、缺场地）。`adminListsAllVenuesAndEffectiveFromIsPersisted`：`service()` 项目 scope 空 → `listAll`（`projectQueries` 空）、两场地全量、`effectiveFrom=2026-10-01` 透传。只改本测试文件。

### 009 / 010

本 diff 零行。未把观察项当可修，未改 FieldLabels/requireText。合规。

---

## 6. 是否最懒、是否新缺陷 / 新业务发明

四分类顺序本轮不重开。本批全是测试加例/加严断言/版本头，无冲突、无添加、无 HTML 申请。未发明错误码、未放宽/收窄实现闭集、未加分页参数、未加二次确认、未把 corridor 入库、未改生产 SQL。403 用一循环、201 用假 ID、007 用同文件 @Nested，均是最懒可行。新例指向真实行为且能失败（主仓 83/83 绿；副本 6 个实现变异体 6/6 被对应新例击红）。未引入新缺陷。

---

## 7. §5 闸 3 逐行归因（17 hunk / 422+ / 18-）

归因是唯一判据。版本头随该文件所属 ID；辅助方法/常量随其服务的用例 ID。任一无法归因即整轮 FAIL。本席逐 hunk 核对：

| hunk | 文件 / 锚点 | 变更 | 归属 ID |
|---|---|---|---|
| 1 | Controller `@@ -1,4 +1,5` | 版本头 V0.01 | 003+004（该文件所属；操作员版本铁律） |
| 2 | Controller `@@ -148,25 +149,28` | 两处桩 50→(1,1,1)，listFloorPlans 加 page_size，listShapes 加 page_size | 003 |
| 3 | Controller `@@ -228,13 +232,14` | listImports 桩 50→(1,1,1) 加 page_size | 003 |
| 4 | Controller `@@ -290,6 +295,66` | 8 端点 403 循环 + 缺幂等键两 POST + 四常量 | 004 |
| 5 | Parser `@@ -1,4 +1,5` | 版本头 V0.01 | 001（+006 文案例落本文件） |
| 6 | Parser `@@ -109,4 +110,84` | 10 个新例（8 码+SVG 越界+默认+中文 POINTS_TOO_FEW） | 001；POINTS_TOO_FEW 文案兼 006 |
| 7 | Jdbc `@@ -1,5 +1,5` | V0.02→V0.03 | 005+007 |
| 8 | Jdbc `@@ -200,6 +200,39` | projectExists 例 + import_candidates CHECK 例 + `candidate()` | 007；005；`candidate()`→005 |
| 9 | Service `@@ -1,5 +1,5` | V0.03→V0.04 | 002+003+005+006 |
| 10 | Service `@@ -98,8 +98,11` | 既有三点校验补中文 getMessage | 006 |
| 11 | Service `@@ -546,6 +549,148` | 002 五例、006 百万字符例、003 诚实分页例、005 corridor 负例 | 002 / 006 / 003 / 005；`shapeRequestOfType`→005 |
| 12 | Service `@@ -573,6 +718,10` | `importRequest(key,svg)` 重载 | 002（兼 006 百万字符） |
| 13 | Venue `@@ -1,14 +1,15` | V0.01→V0.02；去 mock import；加 AuditEvent / InMemoryAuditEventRepository | 008（版本头随 006/007/008） |
| 14 | Venue `@@ -16,7 +17,12` | Nested/SpringBootTest/JdbcTemplate/Transactional/Autowired | 007（@Nested 承载）兼 008 不使用这些 import 的单元例 |
| 15 | Venue `@@ -65,6 +71,7` | `auditEventRepository` 字段 | 008 |
| 16 | Venue `@@ -73,7 +80,9` | 真实 AuditEventService 接线 | 008 |
| 17 | Venue `@@ -147,23 +156,90` | 五处 code 加严 + 006 两句中文 + 008 审计/admin/effectiveFrom + 007 @Nested SQL 例 | 008 / 006 / 007 |

无空行、无格式-only、无「顺手优化」、无换测试框架、无新测试文件、无 `src/main`。全部 +/− 行均可归到 001–008。范围归因 PASS。

---

## 8. 越界检查

| 禁止项 | 结果 |
|---|---|
| 三份业务 HTML | 两树 blob 相同 |
| 锁 / sidecar / ledger 基准 | 两树 blob 相同（工作区 ledger 后写，不在本 diff） |
| OpenAPI | 两树 blob 相同 |
| 迁移 / `backend/src/main` | name-only 空 |
| 权限种子 / 前端 | 未出现在 diff |
| 新增测试文件 | 无；仍是模块 FILES 5 条 |
| corridor 进白名单 | 无；仅负例 |
| 加分页参数 | 无 |
| 加二次确认字段 | 无 |
| 改 009/010 观察项 / FieldLabels | 无 |

越界检查 PASS（闸 3 与内容检查一致）。

---

## 9. 复跑验证

只导出三个 `COMPLEX_DATABASE_*` 变量，不 source 整份 env。环境文件 URL 指向 `complex`；测试类写死 `jdbc:postgresql://127.0.0.1:5432/complex_test`。Flyway：`Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)`，`Current version of schema "public": 423`，`Schema "public" is up to date`。

主仓命令（只读，不改源）：

```
export COMPLEX_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/complex_test
export COMPLEX_DATABASE_USERNAME=complex
export COMPLEX_DATABASE_PASSWORD=<from ~/.complex/complex.env 单行 grep，未 source>
cd backend
mvn -Dtest=com.complex.platform.floorplan.FloorPlanControllerTests,com.complex.platform.floorplan.FloorPlanImportParserTests,com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests,com.complex.platform.floorplan.FloorPlanServiceTests,com.complex.platform.governance.VenueBusinessHoursServiceTests test
```

GREEN：mvn exit 0。TEST-*.xml testcase：Controller 14、Parser 14、Jdbc 7、Service 36、Venue 12（含 `VenueBusinessHoursServiceTests$RealRepositoryCases::findVenueProjectIdAndListByProjectHitRealSpacesTable`）。XML_TOTAL=83 XML_FAILED=0。surefire 文本：Venue `Tests run: 0`（@Nested 报告器怪癖，与定稿说明一致，不以文本报告计数）。

一次性副本 `/tmp/m017_b4_grok_mut`（rsync backend，只改三份 `src/main` 实现；JAVA_HOME=Temurin 21 重编译；主仓实现未动）：

| 变异体 | 实现改动 | 击红新例 |
|---|---|---|
| m1 解析器文案英文 | POINTS_TOO_FEW errorMessage → English | `reportsPointsTooFewWithChineseMessage` |
| m2 去候选归属校验 | 删除「候选项不属于该导入任务」抛点 | `publishRejectsCandidateOfAnotherTaskOrUnknownCandidate`（未抛） |
| m3 放开发布上限 | `if (false && size > 200)` | `publishRejectsMoreThanTwoHundredCandidatesAndEmptyList`（VALIDATION_FAILED→IMPORT_CANDIDATE_NOT_FOUND） |
| m4 改审计事件名 | `venue_business_hours.updated`→`changed` | `saveAndListWriteAuditEvents` |
| m5 星期错误码 | VALIDATION_FAILED→VENUE_NOT_FOUND | `rejectsDayOfWeekOutOfRange` |
| m6 listFloorPlans 谎报 | pageSize 改回 50 | `listFloorPlansAndImportTasksReportHonestPageSizeAndTotal`（expected 2 but was 50） |

副本：Tests run 83，Failures 6，Errors 0。6/6 被对应新例击红。Controller 14 与 Jdbc 7 在实现变异下仍绿（桩/库层不受这六处服务文案影响）。副本目录已删除。未对 `complex_test*` 手工写库。

---

## 10. 对抗式（grilling 自构，不发问）

1. 失败任务发布用假候选 ID，会不会其实锁的是「候选不存在」？——不会：`publishImportTaskOnce` 先 `status!="parsed"` 抛 VALIDATION_FAILED。删该闸则本例变成 IMPORT_CANDIDATE_NOT_FOUND 而红。
2. 201 假 ID 会不会从未碰到上限？——不会：`publishImportTask` 先 `validateImportPublishRequest`。放开上限后本例红。
3. corridor 是否被测试夹带进白名单？——否，diff 无 `src/main`；负例要求 VALIDATION_FAILED。
4. 是否新增分页查询参数？——否。
5. @Nested 是否没跑？——XML 12 含 nested 例；文本 0 是报告器怪癖。
6. 缺幂等键是否被 Jackson 默认值绕过？——主仓日志 `idempotency_key is required`，400 VALIDATION_FAILED。
7. 007 INSERT 列是否不够？——主仓绿，事务回滚。
8. 008 admin 是否仍走项目过滤？——`projectQueries` 断言空，两 venue 全量。

---

## 11. 结论与回退二分

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。

发现的问题：无。

- 有 ID 且仍在修复队列：无（001–008 均按最小处置修好；§6 非阻断余量不是没修好、不触发回退）。
- 无 ID 或不在修复队列：无。

本席不行使否决。本票不等于 B4 双 PASS，不等于 B5，不等于模块 PASS。
