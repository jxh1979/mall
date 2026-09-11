CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M016 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M016-b4-r1-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：任何 `M016-b4-r1-self-raw.md`（当前亦不存在）、席甲本次 B4 raw。定稿队列以 findings.md V1.96 顶部「当前 M016 B2-r1 双PASS定稿」为准。本轮只审该定稿修复队列「是」3 项（M016-001、002、003）的修复内容与 before/after 全文 diff；004 为队列「否」。本模块为数据库迁移类：修复只能新 V 号（V423），已应用 V023/V034/V184/V203 不改写。定稿明示同批改 M015 `FloorPlanService.SHAPE_TYPES`（M015-013 授权）、M017 `FloorPlanJdbcIntegrationTests` / `FloorPlanServiceTests` 先红用例、M011 `PlatformJdbcIntegrationTests`（候选曾误写 M017，Grok B2 非阻断勘误，V1.96 顶部已改）以及 `npm run docs:data-isolation-spec` / `docs:domain-ownership-scope` 重生成的门禁产物。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、3 个定稿「是」ID 是否按最小边界修好、是否最懒可行、是否引入新缺陷或新业务发明（冲突→细化→技术性→添加）、每一变更行能否归因到上述 ID、是否越界改三份业务 HTML / 锁 / sidecar / ledger 基准 / OpenAPI / 权限种子 / 前端 / 其它模块源码（M015/M017/M011 仅定稿点名的文件与行）、是否改写已应用迁移、是否新增字段/接口/业务规则（CHECK 收窄与门禁产物再生成不是新业务能力）。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例（corridor 入库、SET NOT NULL、加 FK、geojson/前端、改 V023 正文、空 project_id 走 Java 层而非库层），未冒充调用 grilling 子代理。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 正文一行同值。criteria-lock 版本仍 V0.11。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M016 空间点位、楼层平面图与营业时段／数据库迁移／第1段（4 文件：V023/V034/V184/V203） |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 正式队列「是」（3） | 细化 001/002；技术性 003 |
| 正式队列「否」（1） | 004（观察；含 M018 登记：前端 corridor / sourceFormat 默认 geojson） |
| B3 | 已执行 1 次（本轮为第 1 次），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `ced99bc97a1ccceaf075d804ed39e5f6ad74b3e0` |
| after（对象类型 tree，可解析） | `71f1468b8bf3bc19d5624d92bcebd5a3edd99cde` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary ced99bc97a1ccceaf075d804ed39e5f6ad74b3e0 71f1468b8bf3bc19d5624d92bcebd5a3edd99cde`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 12377 bytes；SHA256 `0351ea65a591a33def9327c2f8d6d3bfbbdbd3a75a825a2dd4d1bee2ee982ea2`；UTF-8；6 文件；100 插入 / 6 删除；`git diff` 203 行 / 12 hunk；name-status A+5M |
| 与 `/tmp/m016_b3_r1.diff` | 字节与 SHA256 全等（`cmp` identical；本席自算为准，副本仅对照） |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3736 / after 3737；`comm -3` 仅新增 `backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql` |
| HEAD | `fe50e3f309c04f81820f8414bd8860325d694098` = `origin/main`（PR #436 M015 闭环检查点已合并） |
| findings.md | 工作区 V1.96（784447 字节，SHA256 `f8f55b6cf6ab8f0ac1a4126473d4ed6d079444ea5219b9d3dcc482fd91a54e08`）；两树 blob 均为 `724f71d6d018fb558adaff2c5c3ae4c402ec7878`（本 diff 未改 findings）；HEAD blob `ea150d00` 较旧 |
| ledger.md | 两树 blob 均为 `401a999d492f80a23314a72c6edae3833882deb2`（本 diff 未改）；工作区 blob `5e9d73ef` 为冻结 after 树之后写入的 B3-r1 完成节，不在本轮修复内容 diff 内 |
| 三 HTML 两树 blob | BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3` / BEQR `86a717930f9303d4f131ee2864478baa14450e6d`，完全相同 |
| 锁/sidecar 两树 blob | 相同（未改）：lock `8db3fc02ce8a6dc998863a9784e7e88af511b960` / sidecar `41f575026b693af74d2a36e6faf86d260ef6f6ca` |
| OpenAPI | 两树 blob 相同：`docs/openapi/openapi-p0.yaml` `1f056b360dc60c37b6c026c761f0fa57b3688e63` / `docs/openapi/openapi-p0.json` `32231790707d8a93a303ca47033b7819ee0e3b71` |
| 已应用四迁移 | 两树 blob 相同：V023 `60f2b52a20f52a1b255b5d0c2058b58cc1c5ccf7` / V034 `52f4768935fec73b702c4a390e73688b26681173` / V184 `617d4b20eb8e4f70588e789ae27088f381f53548` / V203 `a0a893b77a67effda8bc33c2e074563ad46bd8c5` |
| `docs/domain-ownership-scope-audit-v0.1.csv` | 两树 blob 相同 `ced51949f9704d6b9c8db616a942bce03f18290e`（V423 未建表，再生成零 diff） |

六文件 blob（工作区 `git hash-object` = after；B3 修复已在工作区，未入主索引）：

| 文件 | before | after / work |
|---|---|---|
| `backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java` | `75663eebb0b0bf40ef89967877b832c961eb21f2` | `1a08caed13819272c42a9eb1c6d90edd6733c12f` |
| `backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql` | （无） | `93543319367f4a0ba7277761ba3c3ce3893b0cc3` |
| `backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java` | `c2cfeb972ad480201de8cce246813bfe2ed933aa` | `9bbcb755277f8260f126b03796bb3272ed2df785` |
| `backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java` | `515b2dff446b13a12b0c38fc6378295a1301f548` | `ef4b7afbb98f63f4ea36efb9d935e84a854fa3cb` |
| `backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java` | `62a9b7a9de4be34baeb304439eb87ee79bad478e` | `b155b4bfbd5ac63ac6691d34a9bef629489f833a` |
| `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` | `42e2435c492fbbe085d6455109f3eaaf4cb618f6` | `7bd106b7bdd93aed8caba90fdbd18d7f21fc8bdb` |

`git diff --stat --binary`：`6 files changed, 100 insertions(+), 6 deletions(-)`。`git diff --name-status`：`M`×5 + `A` V423。`git diff --numstat`：3/2 FloorPlanService；22/0 V423；35/1 FloorPlanJdbcIntegrationTests；28/1 FloorPlanServiceTests；11/1 PlatformJdbcIntegrationTests；1/1 data-isolation spec。

工作区相对 HEAD 另有 findings/ledger 已改、以及本模块 B1 双席 raw（未跟踪）。那些路径不在本轮 6 文件 diff 内（findings 两树 blob 全等），不按本轮修复内容审计。本席只审派发的 before/after 对。本席未改任何源文件，只新建本 raw。验证：主仓只读 mvn 测试（测试类写死 `complex_test`；`@Transactional` 回滚）+ `git show`/`git diff` + `complex_test` 只读 SELECT + 一次性库 `m016_b4_grok_probe`（`TEMPLATE complex_test`，事后 DROP）。未 commit/push/PR/merge/部署。未对主索引 `git add`。未对任一 `complex_test*` 库做手工 INSERT/UPDATE/DDL。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M016 的集合（GEN-001…018 共 18）。本轮只用定稿 001–003 已引用的 ID：GEN-006/008/014（001、002）与 GEN-006/008/004（003）。不 LOCK-AMEND。四分类本轮不重开。

B3-r1 边界（与 findings V1.96 顶部一致）：

- 只新增 `V423__m016_floor_plan_and_space_type_checks.sql`（已应用 V023/V034/V184/V203 不改写；可重跑）。
- **001**：`DROP CONSTRAINT IF EXISTS` + `CHECK (project_id IS NOT NULL) NOT VALID`；不加 FK、不 `SET NOT NULL`。TDD：`FloorPlanJdbcIntegrationTests` 空 project_id 落库被拒一例先红。
- **002**：两表 `DROP CONSTRAINT IF EXISTS` 后重建 shape_type CHECK 八值（既有六值 + booth、public_area；不含 corridor）；同批 `FloorPlanService.SHAPE_TYPES` 加 booth/public_area（版本头 V0.02→V0.03）+ `FloorPlanServiceTests` 一例先红 + `FloorPlanJdbcIntegrationTests` booth/public_area 落库一例先红。解析器仍「非白名单整单失败」；前端 corridor/默认值属 M018，本批不改。
- **003**：`UPDATE … SET status='disabled' WHERE status NOT IN ('active','disabled')` 后 `CHECK (status IN ('active','disabled'))`；TDD：`PlatformJdbcIntegrationTests` status='Active' 被拒一例先红。
- 同批 `npm run docs:data-isolation-spec` / `docs:domain-ownership-scope` 重生成门禁产物（改动归因 001–003）。各改动文件版本头递增。
- 禁止：加 FK、`SET NOT NULL`、改前端/OpenAPI/三份 HTML/锁/权限种子/解析器闭集、把 corridor 或 geojson 入库；把 004 当本批可修。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary ced99bc97a1ccceaf075d804ed39e5f6ad74b3e0 71f1468b8bf3bc19d5624d92bcebd5a3edd99cde`

before tree = `ced99bc97a1ccceaf075d804ed39e5f6ad74b3e0`

after tree = `71f1468b8bf3bc19d5624d92bcebd5a3edd99cde`

字节 12377；SHA256 `0351ea65a591a33def9327c2f8d6d3bfbbdbd3a75a825a2dd4d1bee2ee982ea2`；UTF-8 完整原文如下。

````diff
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
````

---

## 4. 逐 ID 修复内容

四分类本轮不重开。判定顺序仍是冲突→细化→技术性→添加。三条均为既有定稿 ID。无新业务发明：001 是列级非空收窄（V159 NOT VALID 范式）；002 是把治理地图已写、V184 已有、前端已标的 booth/public_area 补进 CHECK 与服务白名单同闭集；003 闭集取自 V184 自身注释 `active/disabled`，脏值归一按 V203 范式。

### 4.1 M016-001（细化 / 是）— 修好，最懒可行

定稿最小处置：新迁移 `CHECK (project_id IS NOT NULL) NOT VALID`（前置 DROP IF EXISTS）；新写强制、存量 NULL 容忍、不发明归属值；不加 FK；不 `SET NOT NULL`。

实态（`complex_test` 只读 SELECT，current_database=`complex_test`）：

- `pg_get_constraintdef`：`CHECK ((project_id IS NOT NULL)) NOT VALID`
- `pg_constraint.convalidated = f`（NOT VALID 实锤）
- `information_schema.columns.project_id`：`is_nullable=YES`（未 SET NOT NULL）
- `pg_constraint` 该表 `contype='f'`：0 行（未加 FK）
- flyway_schema_history `version=423 success=t checksum=1707295493`（2026-09-11 02:28:14）

Jdbc 路径：`JdbcFloorPlanRepository.floorPlanParameters` `addValue("project_id", record.projectId())`，Java `null` 落 SQL NULL。测试 `floorPlanWithoutProjectIsRejectedByDatabase` 走仓储而非服务层，断言 `DataIntegrityViolationException`，指向真实 CHECK 而非 Java 校验。

一次性库 `m016_b4_grok_probe`（TEMPLATE complex_test，事后 DROP）：

- 新写 `project_id NULL` → 违 `chk_mall_floor_plans_project_id_required`
- 先 DROP 约束插入存量 NULL 行，再 `ADD … NOT VALID`：该行仍在（`is_null=t`），`convalidated=f`；再新写 NULL 仍拒
- 对照：`ALTER COLUMN project_id SET NOT NULL` 在有存量 NULL 时失败（`column "project_id" … contains null values`）——印证定稿不选 SET NOT NULL

可重跑：scratch 上 `\i V423` 再次 DROP/ADD 成功（UPDATE 0）。V023 正文两树 blob 全等。

最懒：一条 CHECK NOT VALID，无回填、无 FK、无服务层再改。PASS。

### 4.2 M016-002（细化 / 是）— 修好，最懒可行

定稿八值：`area, shop, entrance, facility, risk_zone, route, booth, public_area`（不含 corridor）。两表同闭集。同批白名单。

`pg_get_constraintdef` 两表均为上述 ARRAY 八值。Java `SHAPE_TYPES` 同八值。解析器未改。前端未改。corridor 未入库。

测试：

- `shapeTypeBoothAndPublicAreaAreAcceptedByDatabase`：Jdbc `saveShape` 绕过服务白名单，专打库层 CHECK；断言读回 `booth`/`public_area`。无 V423（仍六值）时 INSERT booth 即违旧 CHECK → JUnit error（与 B3 `/tmp/m016_b3r1_red.log` 一致）。
- `saveShapeAcceptsBoothAndPublicArea`：走 `normalizeAllowed(…, SHAPE_TYPES)`。白名单未改时抛 `ApiException` → JUnit error（红日志 28 例 1 错误）。`createRequest` 使用 `prj_1`，内存仓 `projectExists` 认该值，断言打在真实 `shapeType()`。

scratch 探针：shapes/candidates 均接受 booth、拒绝 corridor。

最懒：DROP+重建 CHECK（放宽二值）+ 白名单一行。未改解析器、未改前端、未把 corridor 放进闭集。PASS。

### 4.3 M016-003（技术性 / 是）— 修好，最懒可行，无业务发明

V184 注释 `-- active/disabled` 无 CHECK。V423：先 UPDATE 脏值→`disabled`，再 CHECK `(active, disabled)`。complex_test 八行均为 `active`（脏值 0，UPDATE 为 no-op）。`convalidated=t`。

测试 `spaceTypeDictionaryRejectsUnknownStatus` 直接 `repository.saveSpaceType(… status="Active")`，绕过服务层 `trimToDefault`。无 CHECK 时 INSERT 成功，`assertThatThrownBy` 失败（红日志 17 例 1 失败）。有 CHECK 时违约束。scratch：`Active` 拒、`disabled` 收。

无业务发明：闭集原文来自 V184 注释；脏值归一不改变「非 active 不进下拉」的既有行为。服务层 status 校验按定稿不改。PASS。

### 4.4 M016-004（否）— 本批未修

diff 无前端、无 corridor、无 geojson、无解析器闭集、无权限种子。符合「否」。

### 4.5 新增用例能否失败

| 用例 | 无修复时 | 有修复时 |
|---|---|---|
| `floorPlanWithoutProjectIsRejectedByDatabase` | 不抛（红日志「Expecting code to raise a throwable」） | `DataIntegrityViolationException` |
| `shapeTypeBoothAndPublicAreaAreAcceptedByDatabase` | 旧六值 CHECK 拒 booth（error） | 读回 booth/public_area |
| `saveShapeAcceptsBoothAndPublicArea` | 白名单拒 booth（error） | 两类型均接受 |
| `spaceTypeDictionaryRejectsUnknownStatus` | `Active` 落库成功（failure） | CHECK 拒 |

断言均指向真实库约束或服务白名单，不是 tautology。

---

## 5. §5 闸 3 逐行归因

提示词：每一行都必须能归因到 findings 已定稿且归属为「是」的 ID。版本头行按操作员版本铁律随该文件所属 ID。docs 门禁产物再生成行归因 001–003。任一行归因不到即整轮 FAIL。

| 块 | 路径 | hunk | 行义 | 归属 |
|---|---|---|---|---|
| 1 | FloorPlanService.java | @@ -1,5 | 版本头 V0.02→V0.03 | 002（本文件本批只为 002 白名单） |
| 2 | FloorPlanService.java | @@ -31,7 | 注释 + SHAPE_TYPES 加 booth/public_area | 002 |
| 3 | V423 新文件 L1 | 文件头「三处库层闭集」 | 001–003（整文件载体） |
| 4 | V423 L2–9 | shape_type 两表 DROP/ADD 八值 | 002 |
| 5 | V423 L3 注释「白名单/前端另批放宽」 | 002 段落内注释（见 §9 非阻断；实际 Java 白名单已同批改，前端按 004 不改） | 002 |
| 6 | V423 L11–15 | project_id CHECK NOT VALID | 001 |
| 7 | V423 L17–22 | 字典 status 归一 + CHECK | 003 |
| 8 | FloorPlanJdbcIntegrationTests | @@ -1,7 | 版本头 V0.01→V0.02；新增 assertThatThrownBy | 001（该 import 仅 001 用例使用） |
| 9 | FloorPlanJdbcIntegrationTests | @@ -9,6 | DataIntegrityViolationException import | 001 |
| 10 | FloorPlanJdbcIntegrationTests | @@ -177 | `shapeTypeBoothAndPublicArea…` + `shape()` | 002 |
| 11 | 同上 | `floorPlanWithoutProject…` | 001 |
| 12 | 同上 | `floorPlan()` 夹具 | 001–002（两例共用） |
| 13 | FloorPlanServiceTests | @@ -1,5 | 版本头 V0.02→V0.03 | 002 |
| 14 | FloorPlanServiceTests | @@ -546 | `saveShapeAcceptsBoothAndPublicArea` + `shapeRequestOfType` | 002 |
| 15 | PlatformJdbcIntegrationTests | @@ -1,5 | 版本头 V0.04→V0.05 | 003 |
| 16 | PlatformJdbcIntegrationTests | @@ -13,6 | DataIntegrityViolationException import | 003 |
| 17 | PlatformJdbcIntegrationTests | @@ -371 | `spaceTypeDictionaryRejectsUnknownStatus` | 003 |
| 18 | data-isolation spec | @@ -56,7 | 自动区块最新迁移 V422→V423（170 表未变） | 001–003（定稿：门禁产物再生成） |

`docs/domain-ownership-scope-audit-v0.1.csv` 两树 blob 全等：V423 只 ALTER 已有表、不 CREATE，生成器零 diff。`npm run verify:domain-ownership-scope` exit 0（`DOMAIN_OWNERSHIP_SCOPE_PASS: 334 tables classified`）。无未归因行。

无「顺手优化 / 换测试框架 / 补抽象」。无 004 行。范围归因 PASS。

---

## 6. 越界检查

| 禁区 | 两树 |
|---|---|
| 三份业务 HTML | blob 全等 |
| 锁 / sidecar | blob 全等 |
| ledger 基准（本 diff） | blob 全等（工作区后续 B3 完成节不在本 diff） |
| OpenAPI yaml/json | blob 全等 |
| 权限种子 / V023·V034 的 INSERT permissions | 四旧迁移 blob 全等；V423 不碰 permissions |
| 前端 `apps/web` | 不在 name-only |
| FloorPlanImportParser | 不在 name-only |
| 已应用 V023/V034/V184/V203 | blob 全等；flyway checksum 仍为 1247380439 / 1294355959 / -1668892912 / 83253196 |
| 其它模块源码 | 仅定稿点名：M015 `FloorPlanService.SHAPE_TYPES` 一行+版本头+注释；M017 两测试类新增用例；M011 `PlatformJdbcIntegrationTests` 一例。无 PlatformService / Controller / FieldLabels |
| 新增字段 / 接口 / 业务规则 | 无。CHECK 收窄与门禁产物再生成按定稿不是新业务能力 |
| corridor / geojson 入库 | CHECK 八值不含 corridor；scratch 插入 corridor 被拒 |
| origin/main 最大 V | V422；本分支新增 V423。`npm run verify:migration-version-order`：`本分支新增 1 个迁移,均排在 origin/main 最大号 V422 之后` |

越界检查 PASS（本席闸 3 票；双 PASS 仍待席甲）。

---

## 7. 复跑验证

只导出三个 `COMPLEX_DATABASE_*` 变量，不 source 整份 env。环境文件里的 URL 指向库名 `complex`；Jdbc 测试类写死 `jdbc:postgresql://127.0.0.1:5432/complex_test`。本席 mvn 实际连的是 `complex_test`（Flyway 日志：`Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)`，`Current version of schema "public": 423`，`Schema "public" is up to date`）。

GREEN（本席独立重跑，主仓、未改文件）：

```
cd backend
mvn -Dtest=com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests,com.complex.platform.floorplan.FloorPlanServiceTests,com.complex.platform.floorplan.FloorPlanControllerTests,com.complex.platform.floorplan.FloorPlanImportParserTests,com.complex.platform.governance.PlatformJdbcIntegrationTests,com.complex.platform.governance.VenueBusinessHoursServiceTests test
```

结果：Tests run: 75, Failures: 0, Errors: 0, Skipped: 0；BUILD SUCCESS。拆分：FloorPlanJdbc 5、FloorPlanServiceTests 28、FloorPlanControllerTests 12、FloorPlanImportParserTests 4、PlatformJdbc 17、VenueBusinessHoursServiceTests 9。Flyway validated 391 migrations（含 V423）。

RED：complex_test 已在 V423，测试类写死该库，本席不能改文件把 mvn 指到别的库。故未在主仓复跑 RED。独立替代证据：

1. `/tmp/m016_b3r1_red.log`：无 V423、白名单未改时 5 例 1 失败 1 错误 / 17 例 1 失败 / 28 例 1 错误，失败用例名与本 diff 四例一一对应。
2. 一次性库 `m016_b4_grok_probe` 对 V423 约束做正反插入（见 §4）；结束 `DROP DATABASE`。`complex_test` 事后仍 `version=423 success=t`，`mall_floor_plans` 行数 0。

门禁：`npm run verify:data-isolation-spec` exit 0；`verify:domain-ownership-scope` exit 0；`verify:migration-version-order` exit 0。

V423 文件 SHA256 `b9912e89df22d03b54305a48af70052c53a58563df8212be0192b60e73ef4043`（22 行）。

---

## 8. 对抗式（grilling 结构，未派子代理）

| 反例 | 结果 |
|---|---|
| 把 corridor 放进 CHECK / 白名单 | 未放；scratch 插入 corridor 被两表 CHECK 拒 |
| `SET NOT NULL` 替代 NOT VALID | 未用；`is_nullable=YES`；scratch 上 SET NOT NULL 在存量 NULL 时失败 |
| 加 `REFERENCES projects` | 未加；mall_floor_plans FK 0 |
| 改写 V023/V034/V184/V203 | 两树 blob 全等 |
| 改三 HTML / 锁 / OpenAPI / 前端 / 解析器 | 不在 diff |
| 把 004 当可修（geojson 默认、corridor 前端） | 未改 |
| 测试只打服务层、库层仍可空 | 001 走 Jdbc `saveFloorPlan(null)`，RED 时无 CHECK 能插入成功 |
| 测试 tautology（assert true） | 四例均依赖约束/白名单，能红 |
| 新字段/新接口/新业务规则 | 无 |
| NOT VALID 不卡新写 | scratch 新写 NULL 被拒 |
| Flyway 未应用 | complex_test 423 success |
| 迁移不可重跑 | scratch `\i V423` 成功（DROP IF EXISTS） |
| 门禁产物未再生成 | data-isolation 自动区块 V422→V423；verify --check 双绿；ownership CSV 因无新表而零 diff |

---

## 9. 发现的问题（有 ID / 无 ID）

回退统一规则：无定稿 ID 或不在修复队列 → 整批回 B2；全部已有 ID 且仍属修复队列 → 整批回 B3。

**有 ID（定稿 001–003 范围内）：无阻断项。** 三条均已按最小边界落地。

**无 ID：无。** 不因无 ID 行整轮 FAIL。

非阻断勘误（不构成 FAIL、不新开 ID）：

1. V423 L3 注释写「应用白名单与前端闭集另批放宽」。前端按 004/M018 确属另批；Java `SHAPE_TYPES` 已由定稿授权本批修改，该句后半对白名单不准确。行为以 Java 与 CHECK 为准，二者闭集一致。
2. 无 corridor 负例、无 import_candidates 独立用例。定稿 TDD 只要求 shapes 落库一例 + 服务一例；两表 CHECK 已由 SQL 与 scratch 探针覆盖。
3. `CHECK (project_id IS NOT NULL)` 不拒空串。定稿字面即此；服务层已必填。不升格为缺陷。

---

## 10. 技能自检与缺口

按 runbook §2 / 提示词 §6：本席不派子代理、不用 MCP。grilling / grill-me 本体未作为子代理调用（缺口记此，不冒充）；只用其追问结构自构反例。verification-before-completion 按意图执行（独立重跑 GREEN 75/75，不以 B3 自报代替）。ponytail 按意图用于「最懒可行」核对。test-driven-development 只核对本批已落的先红用例，B4 不写测试。未使用 executing-plans / dispatching-parallel-agents / subagent-driven-development。

---

## 11. 结论

| 项 | 票 |
|---|---|
| 修复内容（001/002/003 是否修好、最懒、无新缺陷/新发明） | PASS |
| 范围归因（§5 闸 3 逐行 + 越界） | PASS |
| 总结论 | PASS |

本票不等于 B4 双 PASS、不等于 B5、不等于模块 PASS。席甲自审票仍须独立落盘；本席 FAIL 才有否决权，本轮不行使。
