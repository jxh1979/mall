CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M015 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M015-b4-r1-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：FAIL。
范围归因结论：PASS。
总结论：FAIL。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席对修复内容行使否决。

未读取：任何 `M015-b4-r1-self-raw.md`（当前亦不存在）、席甲本次 B4 raw。定稿队列以 findings.md V1.93 顶部「当前 M015 B2-r2 双PASS定稿」为准（该节写明：007 处置与 001/010/012/014 事实栏以 r2 节为准，其余以 r1 原表为准）。本轮只审该定稿修复队列「是」9 项（M015-001、002、003、004、005、006、007、008、009）的修复内容与 before/after 全文 diff；010–014 为队列「否」。修复只在本模块 23 文件与其直接测试类内；迁移/OpenAPI/三份 HTML/锁/其它模块不改。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、9 个定稿「是」ID 是否按最小边界修好、是否最懒可行、是否引入新缺陷或新业务发明（冲突→细化→技术性→添加）、每一变更行能否归因到上述 ID、是否越界改三份业务 HTML / 锁 / sidecar / ledger 基准 / 迁移 / OpenAPI / 权限种子 / 其它模块源码，以及是否新增字段、接口或业务规则（`projectExists` 为仓储读查询，不是新业务能力）。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例（生产 AuthorizationService 与测试桩不一致、VARCHAR(64) 溢出、改草稿语义、改三 HTML/锁/迁移、无 ID 行），未冒充调用 grilling 子代理。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 正文一行同值。criteria-lock 版本仍 V0.11。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M015 空间点位、楼层平面图与营业时段／后台实现／第1段（23 文件） |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 正式队列「是」（9） | 技术性 001/007/008；细化 002/003/004/005/006/009 |
| 正式队列「否」（5） | 010（冲突 XM）、011（细化 XM）、012（全局遗漏）、013（XM CHECK）、014（观察） |
| B3 | 已执行 1 次（本轮为第 1 次），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `e0e9fe4451cc5255e0554ebef94bf7f521737a6e` |
| after（对象类型 tree，可解析） | `1bb326da9a30686decf2274d9f3543376ff6addd` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary e0e9fe4451cc5255e0554ebef94bf7f521737a6e 1bb326da9a30686decf2274d9f3543376ff6addd`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 41713 bytes；SHA256 `2218456f105d17a121e8f0a4aab7ebde272e31c69c9dba3b163410d1ed2ddcf7`；UTF-8；9 文件；341 插入 / 40 删除；`git diff` 797 行 / 48 hunk |
| 与 `/tmp/m015_b3_r1.diff` | 字节与 SHA256 全等（本席自算为准，副本仅对照） |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3727 / after 3727；`comm -3` 空（无增删路径） |
| HEAD | `a041458d06051fed3811d562764cd5900bba6f72` = `origin/main`（PR #435 M014 闭环检查点已合并） |
| findings.md / ledger.md | 两树 blob 相同（本 diff 未改）：findings `8d4aba11c6c530eb30183ddbfcd6afe312b44982`，ledger `9f12b0ef0460b2b13079e2136bcd6cacc64336be` |
| 三 HTML 两树 blob | BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3` / BEQR `86a717930f9303d4f131ee2864478baa14450e6d`，完全相同 |
| 锁/sidecar 两树 blob | 相同（未改）：lock `8db3fc02ce8a6dc998863a9784e7e88af511b960` / sidecar `41f575026b693af74d2a36e6faf86d260ef6f6ca` |
| OpenAPI | 两树 blob 相同：`docs/openapi/openapi-p0.yaml` `1f056b360dc60c37b6c026c761f0fa57b3688e63` / `docs/openapi/openapi-p0.json` `32231790707d8a93a303ca47033b7819ee0e3b71` |
| 源码/迁移/权限种子 | `git diff --name-only` 仅下表 9 路径；`backend/src/main/resources/db/migration` 与权限种子空 |

九文件 blob（工作区 `git hash-object` = after；B3 修复已在工作区，未入主索引）：

| 文件 | before | after / work |
|---|---|---|
| `backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java` | `8e7beebfc09ddc27082a94b03112f287fbfb2742` | `53a26bc86c6293bd83e9c178b1b2c3aabd16def2` |
| `backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java` | `3084aa32f49bcbbd62b7d591008e3c21cfafcfb1` | `97d918beb8753be0c77e1246045d3645e1d61f57` |
| `backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java` | `dc6672f1662227d1cadfa645d22e17e4891d85d1` | `7684e2a3fa5677d3c481f0b1fc21f35e2a626a6b` |
| `backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java` | `333f52bea173afff961668f55860d5b536471b94` | `8f27454787c3371f84ca944400f9ec6e2498a0e4` |
| `backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java` | `d7e0c02181f4133eb56d494c01453c7981edeea7` | `30946ec517ca7a25d1b14ca1f7d91cf6ef349601` |
| `backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java` | `291cb91b1082e99a4196def311d0c979f442a6e9` | `2192b92b9ecd7fed3e918a226b6c9277e1a398a4` |
| `backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java` | `530cec7233d3a9fca1f7513c760288b71220b2ec` | `c2cfeb972ad480201de8cce246813bfe2ed933aa` |
| `backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java` | `149dfffe7a8ced240d61911cbc14622a3289085a` | `ec3685660dbe69751bc1f1f94488c8c1a433f78b` |
| `backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java` | `e7d429b473eeb9422871a1fe5d164322f08c2982` | `2c6d2ce6cd58ebca3e06862a49947bc22fdeab70` |

`git diff --stat --binary`：`9 files changed, 341 insertions(+), 40 deletions(-)`。`git diff --name-status`：九处 `M`。M015 其余 17 个模块文件与 M017 未改的 `FloorPlanControllerTests` / `FloorPlanImportParserTests` 两树 blob 全等。

工作区相对 HEAD 另有 findings/ledger 已改、以及本模块 B1 双席 raw（未跟踪）。那些路径不在本轮 9 文件 diff 内（findings/ledger 两树 blob 全等），不按本轮修复内容审计。本席只审派发的 before/after 对。本席未改任何源文件，只新建本 raw。验证：主仓只读 mvn 测试 + `git show`/`git diff`；RED 在 `/tmp/m015_b4_red_backend` 副本复现，未改主仓。未 commit/push/PR/merge/部署。未对主索引 `git add`。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M015 的集合。本轮只用定稿 001–009 已引用的 ID。不 LOCK-AMEND。四分类本轮不重开。

B3-r1 边界（与 findings V1.93 顶部一致；007 以 r2 为准）：

- 只改本模块文件与其测试类，各加 `// 文件版本：V0.01`。
- **001**：图形/导入任务主键加入平面图域（+2 例）。
- **002**：`project_id` 必填并经 `FloorPlanRepository.projectExists` 校验存在（+2 例）。
- **003**：商户 scope 优先；仅当商户 scope 为空时按 `currentProjectScopeQuietly` 走 `findAllFloorPlansByProject`（平台管理员桥接为空则全量）；`ensureFloorPlanExists` 加项目归属校验；创建时项目 scope 存在则须相等（+3 例）。r2 非阻断：不把无组织全局员从「现网全量」改成 403。
- **004**：`FloorPlanImportParser` 13 处 error_message 改中文；Jdbc 夹具随改。
- **005**：`plan_name` / `candidate_ids` / `<field> is invalid` 改本模块中文整句，不改 FieldLabels。
- **006**：去掉中文句内英文键。
- **007**（r2）：三处改 `new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size())` 诚实不分页（+1 非空 listShapes 例）。
- **008**：`candidate_ids.size() > 200` 拒绝（中文）。
- **009**：`findVenueProjectId` + 保存校验场地存在与项目 scope、列表 JOIN 按项目过滤、服务注入 `AuthorizationService`（+3 例）。
- 禁止：改迁移/OpenAPI/三份 HTML/锁/权限种子/PlatformController/前端/FieldLabels；新增字段或接口；把 010/011/012/013 当本批可修。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary e0e9fe4451cc5255e0554ebef94bf7f521737a6e 1bb326da9a30686decf2274d9f3543376ff6addd`

before tree = `e0e9fe4451cc5255e0554ebef94bf7f521737a6e`

after tree = `1bb326da9a30686decf2274d9f3543376ff6addd`

字节 41713；SHA256 `2218456f105d17a121e8f0a4aab7ebde272e31c69c9dba3b163410d1ed2ddcf7`；UTF-8 完整原文如下。

````diff
diff --git a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java
index 8e7beebf..53a26bc8 100644
--- a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java
+++ b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 
 import com.fasterxml.jackson.databind.JsonNode;
 import com.fasterxml.jackson.databind.ObjectMapper;
@@ -33,7 +34,7 @@ public class FloorPlanImportParser {
         if (sourcePayload == null || sourcePayload.isBlank()) {
             return new FloorPlanImportParseResult(
                     List.of(),
-                    List.of(error(null, "source_payload", "PAYLOAD_REQUIRED", "source_payload is required"))
+                    List.of(error(null, "source_payload", "PAYLOAD_REQUIRED", "导入内容不能为空"))
             );
         }
         return switch (format) {
@@ -41,7 +42,7 @@ public class FloorPlanImportParser {
             case "standard_json" -> parseStandardJson(sourcePayload);
             default -> new FloorPlanImportParseResult(
                     List.of(),
-                    List.of(error(null, "source_format", "UNSUPPORTED_SOURCE_FORMAT", "source_format is not supported"))
+                    List.of(error(null, "source_format", "UNSUPPORTED_SOURCE_FORMAT", "不支持的导入格式（仅支持 svg / standard_json）"))
             );
         };
     }
@@ -53,7 +54,7 @@ public class FloorPlanImportParser {
         if (viewBox == null || viewBox[2] <= 0 || viewBox[3] <= 0) {
             return new FloorPlanImportParseResult(
                     List.of(),
-                    List.of(error(null, "viewBox", "VIEWBOX_REQUIRED", "svg viewBox with positive width and height is required"))
+                    List.of(error(null, "viewBox", "VIEWBOX_REQUIRED", "SVG 必须包含宽高为正数的 viewBox"))
             );
         }
 
@@ -64,19 +65,19 @@ public class FloorPlanImportParser {
             Map<String, String> attributes = readAttributes(matcher.group(2));
             String points = attributes.get("points");
             if (points == null || points.isBlank()) {
-                errors.add(error(shapeNo, "points", "POINTS_REQUIRED", "points attribute is required"));
+                errors.add(error(shapeNo, "points", "POINTS_REQUIRED", "图形缺少 points 属性"));
                 shapeNo++;
                 continue;
             }
 
             List<FloorPlanPoint> rawPoints = parseSvgPoints(points);
             if (rawPoints.size() < 3) {
-                errors.add(error(shapeNo, "points", "POINTS_TOO_FEW", "polygon requires at least 3 points"));
+                errors.add(error(shapeNo, "points", "POINTS_TOO_FEW", "多边形至少需要 3 个顶点"));
                 shapeNo++;
                 continue;
             }
             if ("polyline".equals(tag) && !samePoint(rawPoints.getFirst(), rawPoints.getLast())) {
-                errors.add(error(shapeNo, "points", "POLYLINE_NOT_CLOSED", "polyline must be closed before import"));
+                errors.add(error(shapeNo, "points", "POLYLINE_NOT_CLOSED", "折线（polyline）必须首尾闭合后才能导入"));
                 shapeNo++;
                 continue;
             }
@@ -86,7 +87,7 @@ public class FloorPlanImportParser {
 
             List<FloorPlanPoint> normalized = normalizePoints(rawPoints, viewBox);
             if (normalized.stream().anyMatch(point -> outOfRange(point.x()) || outOfRange(point.y()))) {
-                errors.add(error(shapeNo, "points", "POINT_OUT_OF_RANGE", "normalized point x/y must be between 0 and 1"));
+                errors.add(error(shapeNo, "points", "POINT_OUT_OF_RANGE", "归一化后的顶点坐标 x/y 必须在 0 到 1 之间"));
                 shapeNo++;
                 continue;
             }
@@ -103,7 +104,7 @@ public class FloorPlanImportParser {
         }
 
         if (candidates.isEmpty() && errors.isEmpty()) {
-            errors.add(error(null, "source_payload", "NO_SHAPES_FOUND", "svg does not contain polygon or polyline shapes"));
+            errors.add(error(null, "source_payload", "NO_SHAPES_FOUND", "SVG 中没有 polygon 或 polyline 图形"));
         }
         return new FloorPlanImportParseResult(candidates, errors);
     }
@@ -118,13 +119,13 @@ public class FloorPlanImportParser {
         } catch (Exception exception) {
             return new FloorPlanImportParseResult(
                     List.of(),
-                    List.of(error(null, "source_payload", "JSON_PARSE_FAILED", "source_payload is not valid JSON"))
+                    List.of(error(null, "source_payload", "JSON_PARSE_FAILED", "导入内容不是合法的 JSON"))
             );
         }
         if (!shapesNode.isArray()) {
             return new FloorPlanImportParseResult(
                     List.of(),
-                    List.of(error(null, "shapes", "SHAPES_REQUIRED", "standard_json requires a shapes array"))
+                    List.of(error(null, "shapes", "SHAPES_REQUIRED", "standard_json 必须包含 shapes 数组"))
             );
         }
 
@@ -238,9 +239,9 @@ public class FloorPlanImportParser {
 
     private String messageFor(String errorCode) {
         return switch (errorCode) {
-            case "POINT_OUT_OF_RANGE" -> "polygon point x/y must be between 0 and 1";
-            case "POINTS_TOO_FEW" -> "polygon requires at least 3 points";
-            default -> "import candidate is invalid";
+            case "POINT_OUT_OF_RANGE" -> "多边形顶点坐标 x/y 必须在 0 到 1 之间";
+            case "POINTS_TOO_FEW" -> "多边形至少需要 3 个顶点";
+            default -> "导入候选项无效";
         };
     }
 
diff --git a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java
index 3084aa32..97d918be 100644
--- a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java
+++ b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 
 import java.util.List;
 import java.util.Optional;
@@ -19,6 +20,9 @@ public interface FloorPlanRepository {
 
     List<FloorPlanRecord> findAllFloorPlansByMerchant(String merchantId);
 
+    /** M015-002：平面图必须归属已存在的综合体项目（projects.id）。 */
+    boolean projectExists(String projectId);
+
     Optional<FloorPlanRecord> findFloorPlanById(String id);
 
     FloorPlanRecord saveFloorPlan(FloorPlanRecord record);
diff --git a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
index dc6672f1..7684e2a3 100644
--- a/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
+++ b/backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -41,6 +42,11 @@ public class FloorPlanService {
     // 一个 <1MB 的 payload 仍可塞入海量小多边形或单个超多顶点多边形撑爆解析/落库，故 parse 后强制上限。
     private static final int MAX_IMPORT_CANDIDATES = 2_000;
     private static final int MAX_IMPORT_POLYGON_POINTS = 1_000;
+    // M015-008：发布一次最多 200 个候选（与 OpenAPI FloorPlanImportPublishRequest.candidate_ids maxItems 一致）。
+    private static final int MAX_PUBLISH_CANDIDATES = 200;
+    // M015-005：本模块校验文案中文整句，不经 FieldLabels 通用键翻译（candidate_ids/plan_name 在通用表中另有含义）。
+    private static final Map<String, String> FIELD_LABELS = Map.of(
+            "status", "状态", "shape_type", "图形类型", "ref_type", "关联类型", "source_format", "导入格式");
 
     private final FloorPlanRepository repository;
     private final IdempotencyService idempotencyService;
@@ -63,10 +69,13 @@ public class FloorPlanService {
     }
 
     public PageResponse<FloorPlanRecord> listFloorPlans() {
-        Optional<String> scope = authorizationService.currentMerchantScope();
-        List<FloorPlanRecord> records = scope.isPresent()
-                ? repository.findAllFloorPlansByMerchant(scope.get())
-                : repository.findAllFloorPlans();
+        Optional<String> merchantScope = authorizationService.currentMerchantScope();
+        // M015-003：商户 scope 优先；否则按项目 scope（BEQR-152 数据默认按项目隔离）；平台管理员全量。
+        List<FloorPlanRecord> records = merchantScope.isPresent()
+                ? repository.findAllFloorPlansByMerchant(merchantScope.get())
+                : authorizationService.currentProjectScopeQuietly()
+                        .map(repository::findAllFloorPlansByProject)
+                        .orElseGet(repository::findAllFloorPlans);
         auditEventService.recordSuccess(
                 "floor_plan.viewed",
                 "floor_plan.view",
@@ -76,7 +85,7 @@ public class FloorPlanService {
                 null,
                 Map.of("count", records.size())
         );
-        return new PageResponse<>(records, 1, 50, records.size());
+        return new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size());
     }
 
     public PageResponse<FloorPlanShapeRecord> listShapes(String floorPlanId) {
@@ -92,7 +101,7 @@ public class FloorPlanService {
                 null,
                 Map.of("count", records.size())
         );
-        return new PageResponse<>(records, 1, 50, records.size());
+        return new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size());
     }
 
     public FloorPlanRecord createFloorPlan(FloorPlanCreateRequest request) {
@@ -138,7 +147,7 @@ public class FloorPlanService {
                 null,
                 Map.of("count", records.size())
         );
-        return new PageResponse<>(records, 1, 50, records.size());
+        return new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size());
     }
 
     public FloorPlanImportDetailsResponse getImportDetails(String floorPlanId, String importTaskId) {
@@ -221,10 +230,10 @@ public class FloorPlanService {
         Optional<String> scope = authorizationService.currentMerchantScope();
         FloorPlanRecord record = repository.saveFloorPlan(new FloorPlanRecord(
                 "fp_" + UUID.randomUUID(),
-                trimToNull(request.projectId()),
+                requireText(request.projectId(), "project_id"),
                 trimToNull(request.buildingCode()),
                 requireText(request.floorNo(), "floor_no"),
-                requireText(request.planName(), "plan_name"),
+                requireField(request.planName(), "平面图名称"),
                 requireText(request.imageUrl(), "image_url"),
                 request.imageWidth(),
                 request.imageHeight(),
@@ -256,7 +265,7 @@ public class FloorPlanService {
         }
         Instant now = Instant.now();
         FloorPlanShapeRecord record = repository.saveShape(new FloorPlanShapeRecord(
-                requestedShapeId == null ? "fps_" + slug(request.idempotencyKey()) : requestedShapeId,
+                requestedShapeId == null ? "fps_" + slug(floorPlanId + "_" + request.idempotencyKey()) : requestedShapeId,
                 floorPlanId,
                 normalizeAllowed(request.shapeType(), "shape_type", SHAPE_TYPES),
                 normalizeAllowed(request.refType(), "ref_type", REF_TYPES),
@@ -284,7 +293,7 @@ public class FloorPlanService {
         Instant now = Instant.now();
         FloorPlanImportParseResult parseResult = importParser.parse(request.sourceFormat(), request.sourcePayload());
         enforceImportLimits(parseResult);
-        String taskId = "fpi_" + slug(request.idempotencyKey());
+        String taskId = "fpi_" + slug(floorPlanId + "_" + request.idempotencyKey());
         String status = parseResult.candidates().isEmpty() ? "failed" : "parsed";
         FloorPlanImportTaskRecord task = repository.saveImportTask(new FloorPlanImportTaskRecord(
                 taskId,
@@ -362,7 +371,7 @@ public class FloorPlanService {
                 .distinct()
                 .toList();
         if (candidateIds.isEmpty()) {
-            throw ApiException.badRequest("VALIDATION_FAILED", "candidate_ids is required");
+            throw ApiException.badRequest("VALIDATION_FAILED", "候选项列表不能为空");
         }
 
         List<FloorPlanImportCandidateRecord> candidates = new ArrayList<>();
@@ -494,8 +503,20 @@ public class FloorPlanService {
             throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
         }
         requireText(request.idempotencyKey(), "idempotency_key");
+        // M015-002/003：平面图必须归属已存在的综合体项目（PGM-017），且项目员工只能在本项目下创建（BEQR-152）。
+        String projectId = requireText(request.projectId(), "project_id");
+        if (!repository.projectExists(projectId)) {
+            throw ApiException.notFound("PROJECT_NOT_FOUND", "综合体项目不存在或已被删除");
+        }
+        if (authorizationService.currentMerchantScope().isEmpty()) {
+            authorizationService.currentProjectScopeQuietly().ifPresent(scope -> {
+                if (!scope.equals(projectId)) {
+                    throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只能在当前综合体项目下创建平面图");
+                }
+            });
+        }
         requireText(request.floorNo(), "floor_no");
-        requireText(request.planName(), "plan_name");
+        requireField(request.planName(), "平面图名称");
         requireText(request.imageUrl(), "image_url");
         requireText(request.versionNo(), "version_no");
         requirePositive(request.imageWidth(), "image_width");
@@ -514,7 +535,7 @@ public class FloorPlanService {
         normalizeAllowed(request.status(), "status", STATUSES);
         List<FloorPlanPoint> points = request.polygonPoints();
         if (points == null || points.size() < 3) {
-            throw ApiException.badRequest("VALIDATION_FAILED", "多边形顶点（polygon_points）至少需要 3 个点");
+            throw ApiException.badRequest("VALIDATION_FAILED", "多边形至少需要 3 个顶点");
         }
         for (FloorPlanPoint point : points) {
             if (point == null || point.x() < 0 || point.x() > 1 || point.y() < 0 || point.y() > 1) {
@@ -532,7 +553,7 @@ public class FloorPlanService {
         requireText(request.sourceName(), "source_name");
         String payload = requireText(request.sourcePayload(), "source_payload");
         if (payload.length() > 1_000_000) {
-            throw ApiException.badRequest("VALIDATION_FAILED", "导入内容（source_payload）超过 1000000 字符上限");
+            throw ApiException.badRequest("VALIDATION_FAILED", "导入内容超过 100 万字符上限");
         }
     }
 
@@ -561,7 +582,10 @@ public class FloorPlanService {
         }
         requireText(request.idempotencyKey(), "idempotency_key");
         if (request.candidateIds() == null || request.candidateIds().isEmpty()) {
-            throw ApiException.badRequest("VALIDATION_FAILED", "candidate_ids is required");
+            throw ApiException.badRequest("VALIDATION_FAILED", "候选项列表不能为空");
+        }
+        if (request.candidateIds().size() > MAX_PUBLISH_CANDIDATES) {
+            throw ApiException.badRequest("VALIDATION_FAILED", "一次发布的候选项不能超过 " + MAX_PUBLISH_CANDIDATES + " 个");
         }
     }
 
@@ -569,9 +593,21 @@ public class FloorPlanService {
         FloorPlanRecord fp = repository.findFloorPlanById(id)
                 .orElseThrow(() -> ApiException.notFound("FLOOR_PLAN_NOT_FOUND", "平面图不存在或已被删除"));
         checkMerchantOwnership(fp.merchantId());
+        checkProjectOwnership(fp.projectId());
         return fp;
     }
 
+    // M015-003：项目员工只能访问本项目平面图（BEQR-152）；商户账号走商户闸、平台管理员 scope 为空放行。
+    private void checkProjectOwnership(String resourceProjectId) {
+        if (authorizationService.currentMerchantScope().isPresent()) {
+            return;
+        }
+        Optional<String> scope = authorizationService.currentProjectScopeQuietly();
+        if (scope.isPresent() && !scope.get().equals(resourceProjectId)) {
+            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "平面图属于其他综合体项目");
+        }
+    }
+
     private void checkMerchantOwnership(String resourceMerchantId) {
         Optional<String> scope = authorizationService.currentMerchantScope();
         if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
@@ -598,11 +634,19 @@ public class FloorPlanService {
     private String normalizeAllowed(String value, String field, Set<String> allowed) {
         String normalized = requireText(value, field).toLowerCase(Locale.ROOT);
         if (!allowed.contains(normalized)) {
-            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
+            throw ApiException.badRequest("VALIDATION_FAILED", FIELD_LABELS.getOrDefault(field, field) + "取值无效");
         }
         return normalized;
     }
 
+    private String requireField(String value, String label) {
+        String trimmed = trimToNull(value);
+        if (trimmed == null) {
+            throw ApiException.badRequest("VALIDATION_FAILED", label + "不能为空");
+        }
+        return trimmed;
+    }
+
     private String slug(String value) {
         return requireText(value, "idempotency_key").toLowerCase(Locale.ROOT)
                 .replaceAll("[^a-z0-9_-]", "_");
diff --git a/backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java b/backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java
index 333f52be..8f274547 100644
--- a/backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java
+++ b/backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
 import com.fasterxml.jackson.core.JsonProcessingException;
@@ -60,6 +61,13 @@ class JdbcFloorPlanRepository implements FloorPlanRepository {
                 """, Map.of("project_id", projectId), this::mapFloorPlan);
     }
 
+    @Override
+    public boolean projectExists(String projectId) {
+        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM projects WHERE id = :id",
+                Map.of("id", projectId), Long.class);
+        return count != null && count > 0;
+    }
+
     @Override
     public Optional<FloorPlanRecord> findFloorPlanById(String id) {
         return jdbcTemplate.query("""
diff --git a/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java b/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java
index d7e0c021..30946ec5 100644
--- a/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import java.time.Instant;
 import java.time.LocalDate;
@@ -6,6 +7,7 @@ import java.time.LocalTime;
 import java.time.ZoneOffset;
 import java.time.format.DateTimeFormatter;
 import java.util.List;
+import java.util.Optional;
 import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
 import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
 import org.springframework.stereotype.Repository;
@@ -45,6 +47,33 @@ class VenueBusinessHoursRepository {
                 rs.getObject("updated_at", java.time.OffsetDateTime.class).toInstant()));
     }
 
+    /** M015-009：场地＝spaces.id；返回其综合体项目归属（无项目归属返回空串），场地不存在返回 empty。 */
+    Optional<String> findVenueProjectId(String venueId) {
+        return jdbcTemplate.query("SELECT COALESCE(project_id, '') AS project_id FROM spaces WHERE id = :id",
+                new MapSqlParameterSource("id", venueId), (rs, n) -> rs.getString("project_id")).stream().findFirst();
+    }
+
+    /** M015-009：按综合体项目过滤的营业时间行（BEQR-152 项目隔离；JOIN spaces 取 project_id）。 */
+    List<VenueBusinessHoursRecord> listByProject(String projectId) {
+        return jdbcTemplate.query("""
+                SELECT h.id, h.venue_id, h.day_of_week, h.open_time, h.close_time, h.is_draft, h.effective_from,
+                       h.created_at, h.updated_at
+                  FROM venue_business_hours h
+                  JOIN spaces s ON s.id = h.venue_id
+                 WHERE s.project_id = :project_id
+                 ORDER BY h.venue_id, h.day_of_week
+                """, new MapSqlParameterSource("project_id", projectId), (rs, n) -> new VenueBusinessHoursRecord(
+                rs.getString("id"),
+                rs.getString("venue_id"),
+                rs.getInt("day_of_week"),
+                rs.getObject("open_time", LocalTime.class).format(HH_MM),
+                rs.getObject("close_time", LocalTime.class).format(HH_MM),
+                rs.getBoolean("is_draft"),
+                rs.getObject("effective_from", LocalDate.class),
+                rs.getObject("created_at", java.time.OffsetDateTime.class).toInstant(),
+                rs.getObject("updated_at", java.time.OffsetDateTime.class).toInstant()));
+    }
+
     /** 按 (venue_id, day_of_week) 幂等覆盖（V226 唯一键）。返回落库后的行。 */
     VenueBusinessHoursRecord upsert(String id, String venueId, int dayOfWeek, LocalTime openTime,
                                     LocalTime closeTime, boolean disabled, LocalDate effectiveFrom, Instant now) {
diff --git a/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java b/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
index 291cb91b..2192b92b 100644
--- a/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
+++ b/backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
@@ -1,5 +1,7 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
+import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.evidence.AuditEventService;
 import com.complex.platform.governance.dto.VenueBusinessHoursRequest;
@@ -8,6 +10,7 @@ import java.time.LocalTime;
 import java.time.format.DateTimeParseException;
 import java.util.List;
 import java.util.Map;
+import java.util.Optional;
 import java.util.UUID;
 import org.springframework.stereotype.Service;
 
@@ -27,14 +30,21 @@ public class VenueBusinessHoursService {
 
     private final VenueBusinessHoursRepository repository;
     private final AuditEventService auditEventService;
+    private final AuthorizationService authorizationService;
 
-    VenueBusinessHoursService(VenueBusinessHoursRepository repository, AuditEventService auditEventService) {
+    VenueBusinessHoursService(VenueBusinessHoursRepository repository, AuditEventService auditEventService,
+                              AuthorizationService authorizationService) {
         this.repository = repository;
         this.auditEventService = auditEventService;
+        this.authorizationService = authorizationService;
     }
 
     public List<VenueBusinessHoursRecord> list() {
-        List<VenueBusinessHoursRecord> rows = repository.listAll();
+        // M015-009：项目员工只看本项目场地（BEQR-152）；平台管理员 scope 为空取全量。
+        Optional<String> projectScope = authorizationService.currentProjectScopeQuietly();
+        List<VenueBusinessHoursRecord> rows = projectScope.isPresent()
+                ? repository.listByProject(projectScope.get())
+                : repository.listAll();
         auditEventService.recordSuccess(
                 "venue_business_hours.viewed", "governance.space.view", "venue_business_hours", "all",
                 null, null, Map.of("count", rows.size()));
@@ -47,14 +57,22 @@ public class VenueBusinessHoursService {
         }
         String venueId = trimToNull(request.venueId());
         if (venueId == null) {
-            throw ApiException.badRequest("VALIDATION_FAILED", "venue_id is required");
+            throw ApiException.badRequest("VALIDATION_FAILED", "场地不能为空");
         }
+        // M015-009：场地必须是现存空间（spaces.id），且项目员工只能配置本项目场地。
+        String venueProjectId = repository.findVenueProjectId(venueId)
+                .orElseThrow(() -> ApiException.notFound("VENUE_NOT_FOUND", "场地不存在或已被删除"));
+        authorizationService.currentProjectScopeQuietly().ifPresent(scope -> {
+            if (!scope.equals(venueProjectId)) {
+                throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "场地属于其他综合体项目");
+            }
+        });
         Integer dayOfWeek = request.dayOfWeek();
         if (dayOfWeek == null || dayOfWeek < 0 || dayOfWeek > 6) {
             throw ApiException.badRequest("VALIDATION_FAILED", "星期取值必须为 0（周日）到 6（周六）");
         }
-        LocalTime openTime = parseTime(request.openTime(), "open_time");
-        LocalTime closeTime = parseTime(request.closeTime(), "close_time");
+        LocalTime openTime = parseTime(request.openTime(), "开始营业时间");
+        LocalTime closeTime = parseTime(request.closeTime(), "结束营业时间");
         // 开歇同刻 = 零长营业窗口 = 这一天谁都进不来，但页面上看着像配好了。宁可拒，不留这种静默坑。
         if (openTime.equals(closeTime)) {
             throw ApiException.badRequest("VALIDATION_FAILED", "开始营业时间与结束营业时间不能相同");
@@ -78,12 +96,12 @@ public class VenueBusinessHoursService {
     private LocalTime parseTime(String raw, String field) {
         String value = trimToNull(raw);
         if (value == null) {
-            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须填写，格式 HH:mm");
+            throw ApiException.badRequest("VALIDATION_FAILED", field + "必须填写，格式 HH:mm");
         }
         try {
             return LocalTime.parse(value.length() == 5 ? value : value.substring(0, 5));
         } catch (DateTimeParseException | IndexOutOfBoundsException exception) {
-            throw ApiException.badRequest("VALIDATION_FAILED", field + " 格式不正确，应为 HH:mm");
+            throw ApiException.badRequest("VALIDATION_FAILED", field + "格式不正确，应为 HH:mm");
         }
     }
 
diff --git a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
index 530cec72..c2cfeb97 100644
--- a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.groups.Tuple.tuple;
@@ -161,7 +162,7 @@ class FloorPlanJdbcIntegrationTests {
                 1,
                 "points",
                 "POINTS_TOO_FEW",
-                "polygon requires at least 3 points",
+                "多边形至少需要 3 个顶点",
                 now
         ));
 
diff --git a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
index 149dfffe..ec368566 100644
--- a/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.floorplan;
+// 文件版本：V0.01
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
@@ -24,6 +25,7 @@ import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
+import java.util.Set;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
 
@@ -326,15 +328,131 @@ class FloorPlanServiceTests {
                 .get().extracting(FloorPlanRecord::merchantId).isEqualTo("m_2");
     }
 
+    // M015-001: 图形/导入任务主键须含平面图域——跨平面图同一客户端幂等键不得覆盖他图数据（修复前此两例红）
+    @Test
+    void saveShapeWithSameKeyAcrossFloorPlansDoesNotOverwrite() {
+        FloorPlanRecord planA = service.createFloorPlan(createRequest("fp-shape-key-a"));
+        FloorPlanRecord planB = service.createFloorPlan(createRequest("fp-shape-key-b"));
+        FloorPlanShapeRecord shapeOnA = service.saveShape(planA.id(), shapeRequest("shared-shape-key", null, polygon()));
+
+        FloorPlanShapeRecord shapeOnB = service.saveShape(planB.id(), shapeRequest("shared-shape-key", null, polygon()));
+
+        assertThat(shapeOnB.id()).isNotEqualTo(shapeOnA.id());
+        assertThat(repository.findShapeById(shapeOnA.id())).get()
+                .extracting(FloorPlanShapeRecord::floorPlanId).isEqualTo(planA.id());
+        assertThat(repository.findShapesByFloorPlanId(planA.id())).hasSize(1);
+        assertThat(repository.findShapesByFloorPlanId(planB.id())).hasSize(1);
+    }
+
+    @Test
+    void createImportTaskWithSameKeyAcrossFloorPlansDoesNotOverwrite() {
+        FloorPlanRecord planA = service.createFloorPlan(createRequest("fp-import-key-a"));
+        FloorPlanRecord planB = service.createFloorPlan(createRequest("fp-import-key-b"));
+        FloorPlanImportTaskRecord taskOnA = service.createImportTask(planA.id(), importRequest("shared-import-key"));
+
+        FloorPlanImportTaskRecord taskOnB = service.createImportTask(planB.id(), importRequest("shared-import-key"));
+
+        assertThat(taskOnB.id()).isNotEqualTo(taskOnA.id());
+        assertThat(repository.findImportTaskById(taskOnA.id())).get()
+                .extracting(FloorPlanImportTaskRecord::floorPlanId).isEqualTo(planA.id());
+        assertThat(repository.findImportCandidatesByTaskId(taskOnA.id())).hasSize(1);
+        assertThat(repository.findImportCandidatesByTaskId(taskOnB.id())).hasSize(1);
+    }
+
+    // M015-007：列表不分页时 pageSize/total 必须等于真实条数（修复前 pageSize 谎报 50 → 此例红）
+    @Test
+    void listShapesReportsHonestPageSizeAndTotal() {
+        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-honest-page"));
+        service.saveShape(floorPlan.id(), shapeRequest("shape-page-1", null, polygon()));
+        service.saveShape(floorPlan.id(), shapeRequest("shape-page-2", null, polygon()));
+
+        PageResponse<FloorPlanShapeRecord> page = service.listShapes(floorPlan.id());
+
+        assertThat(page.items()).hasSize(2);
+        assertThat(page.total()).isEqualTo(2);
+        assertThat(page.pageSize()).isEqualTo(2);
+    }
+
+    // M015-002: 平面图必须带综合体项目归属且项目须存在（PGM-017/PGM-022；修复前此两例红）
+    @Test
+    void createFloorPlanRequiresProjectId() {
+        assertThatThrownBy(() -> service.createFloorPlan(createRequest("floor-plan-no-project", " ")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
+        assertThat(repository.floorPlanSaveCount).isZero();
+    }
+
+    @Test
+    void createFloorPlanRejectsUnknownProject() {
+        assertThatThrownBy(() -> service.createFloorPlan(createRequest("floor-plan-unknown-project", "prj_missing")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("PROJECT_NOT_FOUND"));
+        assertThat(repository.floorPlanSaveCount).isZero();
+    }
+
+    // M015-003: 项目员工按项目 scope 隔离（BEQR-152）；修复前此三例红
+    @Test
+    void listFloorPlansFiltersToProjectScope() {
+        service.createFloorPlan(createRequest("fp-project-scope-1", "prj_1"));
+        service.createFloorPlan(createRequest("fp-project-scope-2", "prj_2"));
+
+        PageResponse<FloorPlanRecord> scoped = serviceWithProjectScope("prj_1").listFloorPlans();
+
+        assertThat(scoped.items()).extracting(FloorPlanRecord::projectId).containsOnly("prj_1");
+        assertThat(service.listFloorPlans().items()).hasSize(2);
+    }
+
+    @Test
+    void accessingFloorPlanBelongingToDifferentProjectIsForbidden() {
+        FloorPlanRecord planOnProject2 = service.createFloorPlan(createRequest("fp-project-access-2", "prj_2"));
+
+        assertThatThrownBy(() -> serviceWithProjectScope("prj_1").listShapes(planOnProject2.id()))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
+    }
+
+    @Test
+    void createFloorPlanOutsideProjectScopeIsForbidden() {
+        assertThatThrownBy(() -> serviceWithProjectScope("prj_1").createFloorPlan(createRequest("fp-project-create-2", "prj_2")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
+        assertThat(repository.floorPlanSaveCount).isZero();
+    }
+
     private AuthorizationService noScopeAuth() {
         return new AuthorizationService(null) {
             @Override
             public Optional<String> currentMerchantScope() {
                 return Optional.empty();
             }
+
+            @Override
+            public Optional<String> currentProjectScopeQuietly() {
+                return Optional.empty();
+            }
         };
     }
 
+    private FloorPlanService serviceWithProjectScope(String projectId) {
+        return new FloorPlanService(
+                repository,
+                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
+                new AuditEventService(auditEventRepository),
+                new FloorPlanImportParser(),
+                new AuthorizationService(null) {
+                    @Override
+                    public Optional<String> currentMerchantScope() {
+                        return Optional.empty();
+                    }
+
+                    @Override
+                    public Optional<String> currentProjectScopeQuietly() {
+                        return Optional.of(projectId);
+                    }
+                }
+        );
+    }
+
     private FloorPlanService serviceWithMerchantScope(String merchantId) {
         return new FloorPlanService(
                 repository,
@@ -351,10 +469,14 @@ class FloorPlanServiceTests {
     }
 
     private FloorPlanCreateRequest createRequest(String idempotencyKey) {
+        return createRequest(idempotencyKey, "prj_1");
+    }
+
+    private FloorPlanCreateRequest createRequest(String idempotencyKey, String projectId) {
         return new FloorPlanCreateRequest(
                 idempotencyKey,
                 "create floor plan",
-                "prj_1",
+                projectId,
                 "A",
                 "1F",
                 "1F plan",
@@ -415,6 +537,18 @@ class FloorPlanServiceTests {
         private int shapeSaveCount;
         private int importTaskSaveCount;
 
+        @Override
+        public boolean projectExists(String projectId) {
+            return Set.of("prj_1", "prj_2").contains(projectId);
+        }
+
+        @Override
+        public List<FloorPlanRecord> findAllFloorPlansByProject(String projectId) {
+            return floorPlans.values().stream()
+                    .filter(fp -> projectId.equals(fp.projectId()))
+                    .toList();
+        }
+
         @Override
         public List<FloorPlanRecord> findAllFloorPlans() {
             return new ArrayList<>(floorPlans.values());
diff --git a/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java b/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
index e7d429b4..2c6d2ce6 100644
--- a/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
@@ -1,9 +1,11 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.Mockito.mock;
 
+import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.evidence.AuditEventService;
 import com.complex.platform.governance.dto.VenueBusinessHoursRequest;
@@ -12,6 +14,8 @@ import java.time.LocalDate;
 import java.time.LocalTime;
 import java.util.ArrayList;
 import java.util.List;
+import java.util.Map;
+import java.util.Optional;
 import org.junit.jupiter.api.Test;
 
 /**
@@ -31,6 +35,24 @@ class VenueBusinessHoursServiceTests {
             super(null);
         }
 
+        private final List<String> projectQueries = new ArrayList<>();
+
+        @Override
+        Optional<String> findVenueProjectId(String venueId) {
+            return Optional.ofNullable(Map.of("sp_bar_main", "prj_1", "sp_bar_side", "prj_2").get(venueId));
+        }
+
+        @Override
+        List<VenueBusinessHoursRecord> listAll() {
+            return new ArrayList<>(saved);
+        }
+
+        @Override
+        List<VenueBusinessHoursRecord> listByProject(String projectId) {
+            projectQueries.add(projectId);
+            return saved.stream().filter(r -> projectId.equals(findVenueProjectId(r.venueId()).orElse(null))).toList();
+        }
+
         @Override
         VenueBusinessHoursRecord upsert(String id, String venueId, int dayOfWeek, LocalTime openTime,
                                         LocalTime closeTime, boolean disabled, LocalDate effectiveFrom, Instant now) {
@@ -46,11 +68,51 @@ class VenueBusinessHoursServiceTests {
     private VenueBusinessHoursService service;
 
     private VenueBusinessHoursService service() {
+        return service(null);
+    }
+
+    private VenueBusinessHoursService service(String projectScope) {
         repository = new RecordingRepository();
-        service = new VenueBusinessHoursService(repository, mock(AuditEventService.class));
+        service = new VenueBusinessHoursService(repository, mock(AuditEventService.class), auth(projectScope));
         return service;
     }
 
+    private static AuthorizationService auth(String projectScope) {
+        return new AuthorizationService(null) {
+            @Override
+            public Optional<String> currentProjectScopeQuietly() {
+                return Optional.ofNullable(projectScope);
+            }
+        };
+    }
+
+    // M015-009：场地必须是现存空间（spaces.id），且项目员工只能读写本项目场地（BEQR-152）；修复前此三例红
+    @Test
+    void rejectsUnknownVenue() {
+        assertThatThrownBy(() -> service().save(
+                new VenueBusinessHoursRequest("idem-1", "sp_missing", 1, "18:00", "23:00", false, null)))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VENUE_NOT_FOUND"));
+        assertThat(repository.saved).isEmpty();
+    }
+
+    @Test
+    void rejectsVenueOutsideProjectScope() {
+        assertThatThrownBy(() -> service("prj_2").save(request(1, "18:00", "23:00")))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
+        assertThat(repository.saved).isEmpty();
+    }
+
+    @Test
+    void listFiltersByProjectScope() {
+        VenueBusinessHoursService scoped = service("prj_1");
+        scoped.save(request(1, "18:00", "23:00"));
+        repository.saved.add(new VenueBusinessHoursRecord(
+                "vbh_side", "sp_bar_side", 1, "18:00", "23:00", false, null, Instant.EPOCH, Instant.EPOCH));
+
+        assertThat(scoped.list()).extracting(VenueBusinessHoursRecord::venueId).containsOnly("sp_bar_main");
+        assertThat(repository.projectQueries).containsExactly("prj_1");
+    }
+
     private VenueBusinessHoursRequest request(Integer dayOfWeek, String open, String close) {
         return new VenueBusinessHoursRequest("idem-1", "sp_bar_main", dayOfWeek, open, close, false, null);
     }
````


---

## 4. 逐 ID 修复内容（是否修好 / 最懒 / 新缺陷 / 新发明）

分类顺序：冲突 → 细化 → 技术性 → 添加。本批无添加、无 HTML 申请。`projectExists` / `findVenueProjectId` / `listByProject` 为仓储读查询，按派发口径不是新业务能力。

### M015-001（技术性 / 是）— FAIL（引入新缺陷）

- 最小处置：`"fps_"+slug(floorPlanId+"_"+key)`、`"fpi_"+slug(floorPlanId+"_"+key)`（候选/错误 ID 随任务 ID）；TDD 跨图同键两例。
- 服务层已按公式改；内存测试 `saveShapeWithSameKeyAcrossFloorPlansDoesNotOverwrite` / `createImportTaskWithSameKeyAcrossFloorPlansDoesNotOverwrite` 在桩上能红能绿（本席独立 RED 两例均红，GREEN 全绿）。跨图同键覆盖在内存仓储上已堵住。
- **新缺陷（GEN-006/GEN-002）**：V023/V034 与 `information_schema`（本席只读查 `complex_test`）四张表 `id VARCHAR(64)`。现网前端 `FloorPlanImportPage.tsx` 使用 `idempotency_key: fpi-${Date.now()}`（17 字符）。`createFloorPlanOnce` 的平面图 id 为 `fp_`+UUID（39）。任务 id = `fpi_`+slug(floorPlanId+`_`+key) = 61，尚能落入 64；候选/错误 id 仍为既有 `fpic_`/`fpie_`+slug(taskId+`_`+序号) = **68 > 64**。本席用现行 slug 规则实算：`fpic_fpi_fp_<uuid>_fpi-<13位时间戳>_1` 长度 68。成功解析的导入在落候选行时会被 PostgreSQL 拒（value too long）。OpenAPI `idempotency_key` maxLength 128；UUID 键下图形/任务 id 长度 80，同样溢出。Jdbc 夹具用短硬编码 id，单测走内存 Map，**现测锁不住此回归**。
- 最懒可行本应在 64 字节主键约束内实现「主键加入平面图域」（例如截断哈希），不得把可工作的前端导入键打爆。本批禁止改迁移，因此更不能靠加长列。001 内容修复不成立。

### M015-002（细化 / 是）— PASS

- `validateCreateRequest` 对 `project_id` `requireText`（缺=VALIDATION_FAILED）+ `repository.projectExists`（无=PROJECT_NOT_FOUND 中文「综合体项目不存在或已被删除」）。接口新增 `boolean projectExists`；Jdbc `SELECT COUNT(1) FROM projects WHERE id=:id` 参数化。TDD 两例本席 RED 均红（空白键未抛 / 未知项目未抛），GREEN 绿。无列级 NOT NULL（归 M016）。不是新业务能力。

### M015-003（细化 / 是）— FAIL（未修好；测试假绿）

- 最小处置：商户优先，其次 `currentProjectScopeQuietly`→`findAllFloorPlansByProject`，平台管理员全量；`ensureFloorPlanExists` 项目归属；创建时项目 scope 须相等。
- 实现仍**先调用会抛的** `AuthorizationService.currentMerchantScope()`（`:422-431`：非平台管理员且无 merchantId → `MERCHANT_SCOPE_REQUIRED`）。项目员工（有 `organizationId`、无 merchant）在列表 / `checkMerchantOwnership` / 创建校验三处**到达不了** `currentProjectScopeQuietly`。`checkProjectOwnership` 写在 `checkMerchantOwnership` 之后，同样被商户闸截死。
- 真实 `currentMerchantScope()` 返回 empty **仅**平台管理员桥；真实 `currentProjectScopeQuietly()` 对平台管理员也是 empty。测试 `serviceWithProjectScope` 把前者桩成 empty、后者桩成项目 ID——**生产 AuthorizationService 不存在这一组合**。本席对照 Grok B1 已核源句（`currentMerchantScope` 对非管理员无商户已抛；现网「空则全量」只落在平台管理员桥）与 B2-r2 非阻断（商户空才查项目、不把无组织全局员从现网全量改成 403）：正确最懒写法是列表/所有权/创建改走 `currentMerchantIdQuietly`（永不抛）再查项目 scope，而不是继续用会抛的 `currentMerchantScope`。
- 故 003 在生产路径未修好。8 例 FloorPlan 新测试里属于 003 的 3 例只锁桩，不锁目标行为（GEN-003）。

### M015-004（细化 / 是）— PASS（观察不升队）

- 所列 13 处 error_message 均已改中文，error_code 未改；Jdbc 夹具 `POINTS_TOO_FEW` 文案随改。Parser 既有 4 例只断言 error_code，故未改测试类仍绿。残留 `svg` / `standard_json` / `polyline` / `polygon` / `shapes` / `x/y` 为格式或标签名，004 最小处置是「改中文」不是 006 的「去掉括号英文」。不据此否 004。

### M015-005（细化 / 是）— PASS

- `candidate_ids is required`→「候选项列表不能为空」（发布校验与发布一次两处）；`requireField(planName,"平面图名称")`→「平面图名称不能为空」；`field+" is invalid"` 改为本模块 `FIELD_LABELS`（status/shape_type/ref_type/source_format）+「取值无效」。未改 `FieldLabels.java`（两树 blob 未变）。

### M015-006（细化 / 是）— PASS

- 「多边形至少需要 3 个顶点」「导入内容超过 100 万字符上限」「场地不能为空」；`parseTime` 标签改为「开始营业时间」「结束营业时间」，去掉键与中文之间的空格，与「开始/结束营业时间必须填写，格式 HH:mm」一致。未改 010 草稿语义。

### M015-007（技术性 / 是，以 r2 为准）— PASS

- 三处 `new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size())`，未用会丢数据的 `PageResponse.of(...,500)`，未新增分页参数。TDD 非空 `listShapes`：2 条时 pageSize=total=items=2；本席 RED 为 expected 2 but was 50。空列表 pageSize=1 与 `of()` safeSize 约定一致，r2 要求打非空例，已打。

### M015-008（技术性 / 是）— PASS（观察：无新例）

- `MAX_PUBLISH_CANDIDATES=200`，`candidateIds.size()>200`→VALIDATION_FAILED「一次发布的候选项不能超过 200 个」，与 OpenAPI maxItems 一致。r1 最小处置未写 TDD 例数。无新测试锁该分支，记观察，不单独否 008。

### M015-009（细化 / 是）— PASS

- `findVenueProjectId`：`SELECT COALESCE(project_id,'') FROM spaces WHERE id=:id`，无行 empty→`VENUE_NOT_FOUND`；有行后按 `currentProjectScopeQuietly` 比项目，不等则 `CROSS_PROJECT_FORBIDDEN`。`listByProject` JOIN spaces。服务注入 `AuthorizationService`。本方法不经 `currentMerchantScope`，项目员工可到达（与 003 不同）。TDD 三例本席 RED 全红、GREEN 绿。未改 is_draft/保存即生效（010 仍否）。`listByProject`/`findVenueProjectId` 不是新 HTTP 接口。

### 010–014

diff 未改草稿语义、未加确认字段、未加迁移/CHECK/白名单、未改观察项。符合「否」。

---

## 5. §5 闸 3 逐行归因（48 hunk / 797 行）

每一变更行（含各文件首行后新增的 `// 文件版本：V0.01`，按操作员版本铁律随该文件所属 ID）均可归因到定稿「是」ID。无无法归因行。未改 010–014、未改模块外文件。

| 路径（hunk 数） | 变更摘要 | 归因 |
|---|---|---|
| FloorPlanImportParser.java（9） | 版本行；13 处 error_message 中文 | **004**（版本行随 004） |
| FloorPlanRepository.java（2） | 版本行；`projectExists` 契约 | **002** |
| FloorPlanService.java（16） | 版本行；FIELD_LABELS/MAX_PUBLISH_CANDIDATES；列表/图形/导入三处诚实分页；create 的 projectId/planName；图形/任务主键加平面图域；candidate_ids 中文与 >200；validateCreate 的 projectExists+项目 scope；polygon/payload 去英文键；ensure+checkProjectOwnership；normalizeAllowed/requireField | 版本行随本文件 001–008；分页 **007**；主键 **001**；projectExists/必填 **002**；列表与 checkProjectOwnership 与创建 scope **003**；plan_name/candidate_ids/取值无效 **005**；polygon/payload **006**；>200 **008** |
| JdbcFloorPlanRepository.java（2） | 版本行；`projectExists` SQL | **002** |
| VenueBusinessHoursRepository.java（3） | 版本行；Optional 导入；`findVenueProjectId`；`listByProject` | **009** |
| VenueBusinessHoursService.java（5） | 版本行；注入 AuthorizationService；list 按项目；venue 存在/项目闸；场地不能为空；parseTime 中文标签 | 版本行随 006/009；隔离与注入 **009**；中文 **006** |
| FloorPlanJdbcIntegrationTests.java（2） | 版本行；夹具中文 | **004** |
| FloorPlanServiceTests.java（5） | 版本行；001 两例；007 一例；002 两例；003 三例；noScope 桩补 quietly；serviceWithProjectScope；createRequest 重载；内存仓 `projectExists`/`findAllFloorPlansByProject` | 版本行随 001/002/003/007；脚手架 002/003 |
| VenueBusinessHoursServiceTests.java（4） | 版本行；假仓三方法；三参构造；auth 桩；009 三例 | **009** |

无「无 ID」行。闸 3 越界判定：本 diff 未改三份业务 HTML、锁、sidecar、ledger 基准、迁移、OpenAPI、权限种子、其它模块源码、前端、FieldLabels、PlatformController。未新增业务字段/HTTP 接口。范围归因 **PASS**。

---

## 6. 越界检查

- 三 HTML / 锁 / sidecar / 两树 findings·ledger blob：未改。
- 迁移目录、OpenAPI、权限种子、`apps/`、M015 其余 17 文件、M017 未列入 diff 的 Controller/Parser 测试：未改。
- 010 草稿注释与 `is_draft` 复用停用：未改。
- `projectExists` 等为只读查询，不是新业务能力。
- 本席未在主仓写模式；RED 副本在 `/tmp/m015_b4_red_backend`。

---

## 7. TDD 与独立复跑（不得只信 B3 日志）

B3 自报：RED 先加新测试与仓储契约脚手架，未改服务逻辑 → FloorPlanServiceTests 8 红 + VenueBusinessHoursServiceTests 3 红；GREEN 改服务/解析器后 5 类 54 例全绿。

本席独立：

- **RED**（`/tmp/m015_b4_red_backend`，JAVA_HOME=Temurin 21.0.12.1）：after 测试 + after 仓储契约 + before `FloorPlanService`/`FloorPlanImportParser` + before `VenueBusinessHoursService` 仅补三参构造。`mvn -Dtest=FloorPlanServiceTests,VenueBusinessHoursServiceTests test`。**Tests run: 35, Failures: 11, Errors: 0**（8+3），失败方法名与 `/tmp/m015_b3r1_red.log` 同集。断言指向真实旧行为：跨图同键 id 相等、pageSize=50、缺 project 未抛、未知项目未抛、项目列表含 prj_2、跨项目未 403、未知场地未抛。**能失败。**
- **GREEN**（主仓只读，仅 export 三个 `COMPLEX_DATABASE_*`，URL 改写为 `jdbc:postgresql://127.0.0.1:5432/complex_test`，未 source env）：`mvn -Dtest=FloorPlanServiceTests,FloorPlanJdbcIntegrationTests,VenueBusinessHoursServiceTests,FloorPlanImportParserTests,FloorPlanControllerTests test`，exit 0。Surefire：Controller 12 / Parser 4 / Jdbc 3 / FloorPlanService 26 / Venue 9，合计 **54 例 Failures 0**。Flyway 日志：`jdbc:postgresql://127.0.0.1:5432/complex_test`，schema v422，无新迁移。

GREEN 全绿**不能**证明 003 生产行为已修（见 §4），也不能证明 001 派生 id 能落入 VARCHAR(64)。

008/004/005/006 无（或仅夹具）新失败例，不单独构成整轮 FAIL。

---

## 8. 发现的问题（有 ID / 无 ID）

### 有 ID（仍在修复队列「是」，整批回 B3）

1. **M015-003 未修好。** 生产路径继续用会抛的 `currentMerchantScope()`，项目员工到不了 `currentProjectScopeQuietly`/`findAllFloorPlansByProject`/`CROSS_PROJECT_FORBIDDEN`。测试桩伪造生产不存在的 merchant-empty ∩ project-present。最小处置「项目员工按项目隔离」未落地。GEN-005/GEN-003/BEQR-152。
2. **M015-001 引入新缺陷。** 主键加入平面图域后，候选/错误派生 id 对现网 `fpi-${Date.now()}` 键为 68 字符，超过 `VARCHAR(64)`；UUID 键下图形/任务 id 为 80。跨图覆盖在内存测已绿，Jdbc 真实插入会炸。GEN-006/GEN-002。

### 无 ID

无。008 缺测、004 文案残留格式名仅观察，不新开 ID、不回 B2。

回退统一规则：全部已有 ID 且仍属修复队列「是」→ **整批回 B3**（禁止拆批）。不要把 003 改成调用会抛的 API 再靠桩绿；001 须在不改迁移的前提下把派生主键保持 ≤64。

---

## 9. 结论

| 项 | 票 |
|---|---|
| 内容修复是否成立 | **FAIL**（003 未修好；001 引入 VARCHAR(64) 溢出） |
| 范围归因是否全部命中 | **PASS**（48 hunk 均可归到 001–009；无越界文件） |
| 总 | **FAIL** |

本席 FAIL 具否决权，本轮行使。不等于模块 PASS。下一步须 B3-r2 按上节有 ID 项整批再修，然后再 B4-r2。

本席未改源码、未改三份业务 HTML、未改锁、未 commit/push/PR/merge/部署。
