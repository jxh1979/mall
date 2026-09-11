CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M016 B4-r1 修复内容复审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B4-r1（B3 第 1 次整批修复的完整 diff 复审）。2026-09-11T09:43:19Z
- before tree：`ced99bc97a1ccceaf075d804ed39e5f6ad74b3e0`；after tree：`71f1468b8bf3bc19d5624d92bcebd5a3edd99cde`；`git diff --binary ced99bc97a1c 71f1468b8bf3` 全文 12377 bytes SHA256 0351ea65a591a33def9327c2f8d6d3bfbbdbd3a75a825a2dd4d1bee2ee982ea2（全文附于本报告末尾）。
- 定稿依据 findings V1.96「当前 M016 B2-r1 双PASS定稿」：是＝M016-001（project_id 列级 NOT VALID CHECK）、002（shape_type CHECK 八值 + M015 白名单同批）、003（字典 status 闭集）；否＝004。B3 累计 1/3。TDD：RED＝4 例红（FloorPlanJdbcIntegrationTests 5 例 1 失败 1 错误、PlatformJdbcIntegrationTests 17 例 1 失败、FloorPlanServiceTests 28 例 1 错误）；GREEN＝6 类 75 例全绿（/tmp/m016_b3r1_red.log、_green.log）；干跑先在一次性 m016_scratch 库验证 RED 4 → GREEN 50/50。
- 锁四值：实算/sidecar/ledger/Grok 均 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。

## 1. diffstat

```
.../platform/floorplan/FloorPlanService.java       |  5 +--
 ...V423__m016_floor_plan_and_space_type_checks.sql | 22 +++++++++++++
 .../floorplan/FloorPlanJdbcIntegrationTests.java   | 36 +++++++++++++++++++++-
 .../platform/floorplan/FloorPlanServiceTests.java  | 29 ++++++++++++++++-
 .../governance/PlatformJdbcIntegrationTests.java   | 12 +++++++-
 ...solation-and-tenant-scope-security-spec-v0.1.md |  2 +-
 6 files changed, 100 insertions(+), 6 deletions(-)
```

## 2. 逐块归因（每一行改动都归到定稿 ID；「版本铁律」行随其所在文件的 ID）

| 文件 | 块 | 位置 | 块内标记 ID | 归因 |
|---|---|---|---|---|
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | 002（SHAPE_TYPES 八值 + 注释；M015-013 授权同批）；版本头 V0.03 随本文件 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 2 | `@@ -31,7 +31,8 @@ import static com.complex.platform.common.` | M016-002 | 002（SHAPE_TYPES 八值 + 注释；M015-013 授权同批）；版本头 V0.03 随本文件 |
| backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql | 1 | `@@ -0,0 +1,22 @@` | — | 001（chk_mall_floor_plans_project_id_required NOT VALID）、002（两表 shape_type CHECK 八值）、003（字典 status 归一 + CHECK）；新文件，可重跑 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java | 1 | `@@ -1,7 +1,8 @@` | 版本铁律（随所属ID） | 001（floorPlanWithoutProjectIsRejectedByDatabase）、002（shapeTypeBoothAndPublicAreaAreAcceptedByDatabase）+ floorPlan/shape 辅助与两处导入；版本头 V0.02 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java | 2 | `@@ -9,6 +10,7 @@ import java.util.List;` | — | 001（floorPlanWithoutProjectIsRejectedByDatabase）、002（shapeTypeBoothAndPublicAreaAreAcceptedByDatabase）+ floorPlan/shape 辅助与两处导入；版本头 V0.02 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java | 3 | `@@ -177,6 +179,38 @@ class FloorPlanJdbcIntegrationTests {` | M016-001, M016-002 | 001（floorPlanWithoutProjectIsRejectedByDatabase）、002（shapeTypeBoothAndPublicAreaAreAcceptedByDatabase）+ floorPlan/shape 辅助与两处导入；版本头 V0.02 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | 002（saveShapeAcceptsBoothAndPublicArea + shapeRequestOfType 辅助）；版本头 V0.03 随本文件 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 2 | `@@ -546,6 +546,33 @@ class FloorPlanServiceTests {` | M016-002 | 002（saveShapeAcceptsBoothAndPublicArea + shapeRequestOfType 辅助）；版本头 V0.03 随本文件 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | 003（spaceTypeDictionaryRejectsUnknownStatus + 导入）；版本头 V0.05 随本文件 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 2 | `@@ -13,6 +13,7 @@ import java.util.Map;` | — | 003（spaceTypeDictionaryRejectsUnknownStatus + 导入）；版本头 V0.05 随本文件 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 3 | `@@ -371,6 +372,15 @@ class PlatformJdbcIntegrationTests {` | M016-003 | 003（spaceTypeDictionaryRejectsUnknownStatus + 导入）；版本头 V0.05 随本文件 |
| docs/data-isolation-and-tenant-scope-security-spec-v0.1.md | 1 | `@@ -56,7 +56,7 @@ scope 类型（与盘点 CSV 的 `scope_type` 对齐）：` | — | 001–003 门禁产物（generate-data-isolation-spec 对 V423 的自动再生成，最新迁移 V422→V423） |

## 3. 红绿证据（B3-r1 运行记录 /tmp/m016_b3r1_run.log 原文）

```
START=2026-09-11T09:27:52Z HEAD=fe50e3f3
complex_test head before: 422
== RED: real-DB assertions, migration absent
FloorPlanJdbcIntegrationTests.java 018480bf503be157
PlatformJdbcIntegrationTests.java 16e57fdb9d033c2d
FloorPlanServiceTests.java 03a59adf8c527f0d
RED_PATCH_OK
RED_EXIT=1
Tests run: 5, Failures: 1, Errors: 1, Skipped: 0, Time elapsed: 3.030 s <<< FAILURE! -- in com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
Tests run: 17, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.313 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
Tests run: 28, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.084 s <<< FAILURE! -- in com.complex.platform.floorplan.FloorPlanServiceTests
31:[ERROR] Tests run: 5, Failures: 1, Errors: 1, Skipped: 0,
32:[ERROR] com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests.floorPlanWithoutProjectIsRejectedByDatabase --
38:[ERROR] com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests.shapeTypeBoothAndPublicAreaAreAcceptedByDatabase --
94:[ERROR] Tests run: 28, Failures: 0, Errors: 1, Skipped: 0,
95:[ERROR] com.complex.platform.floorplan.FloorPlanServiceTests.saveShapeAcceptsBoothAndPublicArea --
109:[ERROR] Tests run: 17, Failures: 1, Errors: 0, Skipped: 0,
110:[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.spaceTypeDictionaryRejectsUnknownStatus --
35:Expecting code to raise a throwable.
58:]; ERROR: new row for relation "floor_plan_shapes" violates check constraint "chk_floor_plan_shapes_shape_type"
79:Caused by: org.postgresql.util.PSQLException: ERROR: new row for relation "floor_plan_shapes" violates check constraint "chk_floor_plan_shapes_shape_type"
113:Expecting code to raise a throwable.
118:Expecting code to raise a throwable.
120:Expecting code to raise a throwable.
140:]; ERROR: new row for relation "floor_plan_shapes" violates check constraint "chk_floor_plan_shapes_shape_type"
== GREEN: add V423
V423__m016_floor_plan_and_space_type_checks.sql 2099 b9912e89df22d03b
FloorPlanService.java 21e7ea67b986b73d
GREEN_PATCH_OK
GREEN_EXIT=0
floorplan.FloorPlanControllerTests.txt:Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.766 s -- in com.complex.platform.floorplan.FloorPlanControllerTests
floorplan.FloorPlanImportParserTests.txt:Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.139 s -- in com.complex.platform.floorplan.FloorPlanImportParserTests
floorplan.FloorPlanJdbcIntegrationTests.txt:Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.041 s -- in com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
floorplan.FloorPlanServiceTests.txt:Tests run: 28, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.082 s -- in com.complex.platform.floorplan.FloorPlanServiceTests
governance.PlatformJdbcIntegrationTests.txt:Tests run: 17, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.307 s -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
governance.VenueBusinessHoursServiceTests.txt:Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.049 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
Migrating schema "public" to version "423 - m016 floor plan and space type checks"
Successfully applied 1 migration to schema 
complex_test head after: 423
== gate docs regeneration
generated docs/data-isolation-and-tenant-scope-security-spec-v0.1.md automatic migration scope baseline
generated docs/domain-ownership-scope-audit-v0.1.csv
data isolation spec is in sync with Flyway migration scope baseline.
DOMAIN_OWNERSHIP_SCOPE_PASS: 334 tables classified
== post-B3 state
 M backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
 M backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
 M docs/audit-2026-09/findings.md
 M docs/audit-2026-09/ledger.md
 M docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
?? backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql
?? docs/audit-2026-09/modules/M016-b1-grok-raw.md
?? docs/audit-2026-09/modules/M016-b1-self-raw.md
backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql 22 b9912e89df22d03b
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java 678 21e7ea67b986b73d
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java 715 03a59adf8c527f0d
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java 244 018480bf503be157
backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java 506 16e57fdb9d033c2d
docs/data-isolation-and-tenant-scope-security-spec-v0.1.md 371 33cd57b26ee44008
docs/domain-ownership-scope-audit-v0.1.csv 335 b33ca86505e1327e
END=2026-09-11T09:28:18Z
```

## 4. 本席复审结论

见文末追加的判定段。

## 附：完整 diff（before→after，--binary）

```diff
diff --git a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
index 75663eeb..1a08caed 100644
--- a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
+++ b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
@@ -1,5 +1,5 @@
 package com.complex.platform.floorplan;
-// 文件版本：V0.02
+// 文件版本：V0.03
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -31,7 +31,8 @@ import static com.complex.platform.common.Strings.requireText;
 public class FloorPlanService {
 
     private static final Set<String> STATUSES = Set.of("active", "inactive", "archived");
-    private static final Set<String> SHAPE_TYPES = Set.of("area", "shop", "entrance", "facility", "risk_zone", "route");
+    // M016-002（M015-013 同批）：与 V423 CHECK 同闭集——治理地图候选区域「铺位、摊位、公共区」= shop / booth / public_area。
+    private static final Set<String> SHAPE_TYPES = Set.of("area", "shop", "entrance", "facility", "risk_zone", "route", "booth", "public_area");
     private static final Set<String> REF_TYPES = Set.of("space", "merchant", "project", "amusement_project", "device", "custom");
     private static final Set<String> IMPORT_SOURCE_FORMATS = Set.of("svg", "standard_json");
     private static final Set<String> IMPORT_TASK_STATUSES = Set.of("parsed", "failed", "published", "cancelled");
diff --git a/backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql b/backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql
new file mode 100644
index 00000000..93543319
--- /dev/null
+++ b/backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql
@@ -0,0 +1,22 @@
+-- M016 审计 B3（2026-09-11）：三处库层闭集/契约对齐，不改数据语义、可重跑（DROP IF EXISTS 后重建）。
+-- 1) 候选区域类型对齐治理地图「铺位、摊位、公共区」与 V184 字典：shape_type CHECK 增加 booth / public_area（M015-013 转入）。
+--    既有六值保留；应用白名单（FloorPlanService.SHAPE_TYPES）与前端闭集另批放宽。
+ALTER TABLE floor_plan_shapes DROP CONSTRAINT IF EXISTS chk_floor_plan_shapes_shape_type;
+ALTER TABLE floor_plan_shapes ADD CONSTRAINT chk_floor_plan_shapes_shape_type
+    CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route', 'booth', 'public_area'));
+ALTER TABLE floor_plan_import_candidates DROP CONSTRAINT IF EXISTS chk_floor_plan_import_candidates_shape_type;
+ALTER TABLE floor_plan_import_candidates ADD CONSTRAINT chk_floor_plan_import_candidates_shape_type
+    CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route', 'booth', 'public_area'));
+
+-- 2) 平面图必须带综合体项目归属（M015-002 转入；服务层已必填并校验 projects 存在）：
+--    新写/更新行强制非空，存量 NULL 行容忍（NOT VALID），不发明归属值；软引用不加 FK（与 spaces.project_id 范式一致）。
+ALTER TABLE mall_floor_plans DROP CONSTRAINT IF EXISTS chk_mall_floor_plans_project_id_required;
+ALTER TABLE mall_floor_plans ADD CONSTRAINT chk_mall_floor_plans_project_id_required
+    CHECK (project_id IS NOT NULL) NOT VALID;
+
+-- 3) 空间类型字典 status 闭集（V184 注释 active/disabled；listActiveSpaceTypeCodes 只认 active）：
+--    按 V203 范式先归一存量脏值再上 CHECK；脏值归为 disabled（此前它们已不在下拉/硬校验中，保持行为不变）。
+UPDATE space_type_dictionary SET status = 'disabled' WHERE status NOT IN ('active', 'disabled');
+ALTER TABLE space_type_dictionary DROP CONSTRAINT IF EXISTS chk_space_type_dictionary_status;
+ALTER TABLE space_type_dictionary ADD CONSTRAINT chk_space_type_dictionary_status
+    CHECK (status IN ('active', 'disabled'));
diff --git a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
index c2cfeb97..9bbcb755 100644
--- a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
@@ -1,7 +1,8 @@
 package com.complex.platform.floorplan;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import static org.assertj.core.api.Assertions.assertThat;
+import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.assertj.core.groups.Tuple.tuple;
 
 import java.time.Instant;
@@ -9,6 +10,7 @@ import java.util.List;
 import org.junit.jupiter.api.Test;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.boot.test.context.SpringBootTest;
+import org.springframework.dao.DataIntegrityViolationException;
 import org.springframework.jdbc.core.JdbcTemplate;
 import org.springframework.transaction.annotation.Transactional;
 
@@ -177,6 +179,38 @@ class FloorPlanJdbcIntegrationTests {
                 .containsExactly("POINTS_TOO_FEW");
     }
 
+    // M016-002：库层 shape_type 闭集须含治理地图「摊位」「公共区」（V423 前 CHECK 六值 → 此例红）
+    @Test
+    void shapeTypeBoothAndPublicAreaAreAcceptedByDatabase() {
+        Instant now = Instant.parse("2026-06-14T00:00:00Z");
+        repository.saveFloorPlan(floorPlan("fp_shape_types", "prj_jdbc", now));
+        repository.saveShape(shape("fps_booth", "fp_shape_types", "booth", now));
+        repository.saveShape(shape("fps_public_area", "fp_shape_types", "public_area", now));
+
+        assertThat(repository.findShapesByFloorPlanId("fp_shape_types"))
+                .extracting(FloorPlanShapeRecord::shapeType)
+                .containsExactlyInAnyOrder("booth", "public_area");
+    }
+
+    // M016-001：mall_floor_plans.project_id 列级不得为空（V423 前可空 → 此例红）
+    @Test
+    void floorPlanWithoutProjectIsRejectedByDatabase() {
+        assertThatThrownBy(() -> repository.saveFloorPlan(
+                floorPlan("fp_no_project", null, Instant.parse("2026-06-14T00:00:00Z"))))
+                .isInstanceOf(DataIntegrityViolationException.class);
+    }
+
+    private static FloorPlanRecord floorPlan(String id, String projectId, Instant now) {
+        return new FloorPlanRecord(id, projectId, "A", "1F", "1F plan", "https://assets.example.com/floor-1.png",
+                1600, 1200, "v1", "active", now, now, null);
+    }
+
+    private static FloorPlanShapeRecord shape(String id, String floorPlanId, String shapeType, Instant now) {
+        return new FloorPlanShapeRecord(id, floorPlanId, shapeType, "space", null, shapeType + " shape",
+                List.of(new FloorPlanPoint(0.1, 0.1), new FloorPlanPoint(0.4, 0.1), new FloorPlanPoint(0.4, 0.3)),
+                0, "active", now, now);
+    }
+
     private String regclass(String tableName) {
         return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
     }
diff --git a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
index 515b2dff..ef4b7afb 100644
--- a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.floorplan;
-// 文件版本：V0.02
+// 文件版本：V0.03
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
@@ -546,6 +546,33 @@ class FloorPlanServiceTests {
         );
     }
 
+    // M016-002（M015-013 同批）：候选区域类型「摊位」「公共区」须被服务白名单接受（修复前白名单六值 → 此例红）
+    @Test
+    void saveShapeAcceptsBoothAndPublicArea() {
+        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-booth-types"));
+
+        FloorPlanShapeRecord booth = service.saveShape(floorPlan.id(), shapeRequestOfType("shape-booth", "booth"));
+        FloorPlanShapeRecord publicArea = service.saveShape(floorPlan.id(), shapeRequestOfType("shape-public-area", "public_area"));
+
+        assertThat(booth.shapeType()).isEqualTo("booth");
+        assertThat(publicArea.shapeType()).isEqualTo("public_area");
+    }
+
+    private FloorPlanShapeSaveRequest shapeRequestOfType(String idempotencyKey, String shapeType) {
+        return new FloorPlanShapeSaveRequest(
+                idempotencyKey,
+                "save polygon",
+                null,
+                shapeType,
+                "space",
+                "sp_1",
+                shapeType + " shape",
+                polygon(),
+                10,
+                "active"
+        );
+    }
+
     private FloorPlanImportCreateRequest importRequest(String idempotencyKey) {
         return new FloorPlanImportCreateRequest(
                 idempotencyKey,
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
index 62a9b7a9..b155b4bf 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.04
+// 文件版本：V0.05
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -13,6 +13,7 @@ import java.util.Map;
 import org.junit.jupiter.api.Test;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.boot.test.context.SpringBootTest;
+import org.springframework.dao.DataIntegrityViolationException;
 import org.springframework.jdbc.core.JdbcTemplate;
 import org.springframework.transaction.annotation.Transactional;
 
@@ -371,6 +372,15 @@ class PlatformJdbcIntegrationTests {
         assertThat(repository.findSpaceById("sp_limit_0")).isPresent();
     }
 
+    // M016-003：空间类型字典 status 只允许 active/disabled（V423 前无 CHECK → 此例红）
+    @Test
+    void spaceTypeDictionaryRejectsUnknownStatus() {
+        Instant now = Instant.parse("2026-06-14T00:00:00Z");
+        assertThatThrownBy(() -> repository.saveSpaceType(
+                new SpaceTypeRecord("sptype_jdbc_bad", "jdbc_bad", "字典状态测试", 100, "Active", now, now)))
+                .isInstanceOf(DataIntegrityViolationException.class);
+    }
+
     private String regclass(String tableName) {
         return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
     }
diff --git a/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md b/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
index 42e2435c..7bd106b7 100644
--- a/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
+++ b/docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
@@ -56,7 +56,7 @@ scope 类型（与盘点 CSV 的 `scope_type` 对齐）：
 
 <!-- DATA_ISOLATION_SPEC_AUTO_START -->
 ### 3A. 自动迁移隔离基线（由 `npm run docs:data-isolation-spec` 生成）
-> 事实源：`backend/src/main/resources/db/migration`。当前解析到最新迁移 **V422**，发现 **170** 张含 scope 字段的表。不要手工修改本区块；新增迁移后运行 `npm run docs:data-isolation-spec`，提交前运行 `npm run verify:data-isolation-spec`。
+> 事实源：`backend/src/main/resources/db/migration`。当前解析到最新迁移 **V423**，发现 **170** 张含 scope 字段的表。不要手工修改本区块；新增迁移后运行 `npm run docs:data-isolation-spec`，提交前运行 `npm run verify:data-isolation-spec`。
 | 表 | scope 字段 | scope 类型 | 首次出现迁移 | 证据文件 |
 |---|---|---|---|---|
 | `access_grant_requests` | `merchant_id` + `project_id` | direct | V170 | `V170__access_grant_requests.sql` |

```


## 4. 本席复审结论（席甲，作者自审票）

本席逐块读完上述 diff 全文（新迁移 V423 + M015 `FloorPlanService` + M017 三个测试类 + 门禁产物），以 §2 归因表核对每一行；§3 为本席 B3-r1 运行记录原文（RED 4 例红 → GREEN 6 类 75 例全绿，主工作区实跑；Flyway 把 complex_test 从 V422 升到 V423；干跑先在一次性 m016_scratch 库验证）。

### 4.1 修复内容（逐 ID）

| ID | 修复内容 | 证据（diff 块） | 本席票 |
|---|---|---|---|
| 001 | V423：`ALTER TABLE mall_floor_plans DROP CONSTRAINT IF EXISTS … ; ADD CONSTRAINT chk_mall_floor_plans_project_id_required CHECK (project_id IS NOT NULL) NOT VALID`（V159 范式：存量容忍、新写/更新强制；不加 FK、不 SET NOT NULL、不发明归属值） | V423 第 2 段；`floorPlanWithoutProjectIsRejectedByDatabase`（RED 时插入成功而红，GREEN DataIntegrityViolationException） | PASS |
| 002 | V423：`floor_plan_shapes` / `floor_plan_import_candidates` 两处 `DROP CONSTRAINT IF EXISTS` + 重建 shape_type CHECK 八值（既有六值 + booth、public_area；不含 corridor）；同批 `FloorPlanService.SHAPE_TYPES` 同闭集 | V423 第 1 段；FloorPlanService 一行 + 注释；`shapeTypeBoothAndPublicAreaAreAcceptedByDatabase`（RED 时 CHECK 违例）、`saveShapeAcceptsBoothAndPublicArea`（RED 时 VALIDATION_FAILED） | PASS |
| 003 | V423：`UPDATE space_type_dictionary SET status='disabled' WHERE status NOT IN ('active','disabled')` 后 `ADD CONSTRAINT chk_space_type_dictionary_status CHECK (status IN ('active','disabled'))`（V203 范式；脏值归 disabled 保持既有可见行为） | V423 第 3 段；`spaceTypeDictionaryRejectsUnknownStatus`（RED 时 upsert 成功而红） | PASS |

版本铁律：改动源/测试文件版本头递增（FloorPlanService V0.03、FloorPlanServiceTests V0.03、FloorPlanJdbcIntegrationTests V0.02、PlatformJdbcIntegrationTests V0.05）；V423 为新文件。门禁产物（data-isolation spec / domain-ownership CSV）为生成器再生成，归因 001–003。

### 4.2 范围归因（§5 闸 3）

- 全部 hunk 落到定稿 001–003；版本头行随所属文件；门禁产物行归因 001–003；归因不到的行 0。
- 未改：V023/V034/V184/V203（已应用迁移零改写）、OpenAPI、三份业务 HTML、锁/sidecar、权限种子、前端、解析器闭集、其它模块源码（M015/M017 仅定稿点名的文件与行）。
- 004（观察）未在本批落地：corridor/geojson 未入库，未加 FK。
- 未提交/推送/合并/部署；complex_test 已随测试启动升到 V423（与 main 迁移目录一致）。

### 4.3 结论

内容修复：**PASS**。范围归因：**PASS**。总：**PASS**（作者自审票；须 Grok B4-r1 亦 PASS 才进 B5-r1；B3 累计 1/3）。本席在本报告落盘前未读取 `M016-b4-r1-grok-raw.md`。
