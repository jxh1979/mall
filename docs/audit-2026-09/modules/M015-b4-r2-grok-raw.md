CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M015 B4-r2 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r2
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M015-b4-r2-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r2 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。

未读取：任何 `M015-b4-r2-self-raw.md`（当前亦不存在）、席甲本次 B4-r2 raw。已读本席 B4-r1 票（`modules/M015-b4-r1-grok-raw.md` V0.01）与派发点名的席甲 B4-r1 V0.02 追加段（撤回 001/003 内容票）。定稿队列以 findings.md V1.93 顶部「当前 M015 B2-r2 双PASS定稿」为准（007 处置与 001/010/012/014 事实栏以 r2 节为准，其余以 r1 原表为准）。本轮只审 B3-r2 对 B4-r1 有 ID 项 **001、003** 的修复内容与 before/after 全文 diff；002/004/005/006/007/008/009 已由本席 B4-r1 PASS，本轮不重审其内容，只确认本 diff 未改它们且 GREEN 未回退。010–014 仍为队列「否」。修复只在本模块 23 文件与其直接测试类内；迁移/OpenAPI/三份 HTML/锁/其它模块不改。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、001/003 是否按最小边界修好、是否最懒可行、是否引入新缺陷或新业务发明（冲突→细化→技术性→添加）、每一变更行能否归因到 001 或 003、是否越界改三份业务 HTML / 锁 / sidecar / ledger 基准 / 迁移 / OpenAPI / 权限种子 / 其它模块源码，以及是否新增字段、接口或业务规则。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例（生产 AuthorizationService 与测试桩不一致、VARCHAR(64) 仍溢出、改草稿语义、改三 HTML/锁/迁移、无 ID 行、商户路径误调会抛的项目 scope），未冒充调用 grilling 子代理。B3-r1 diff 本轮不重审；判断 001/003 是否修好时对照 r1+r2 叠加后的当前源码。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 正文一行同值。criteria-lock 版本仍 V0.11。本批 B3-r2 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M015 空间点位、楼层平面图与营业时段／后台实现／第1段（23 文件） |
| 阶段 | B4-r2 只读审计 B3 第2次整批修复的「修复内容」本身 |
| 本轮修复队列 | 仅 001、003（B4-r1 有 ID FAIL 项）；002/004–009 本轮不重审内容 |
| 正式队列「否」（5） | 010（冲突 XM）、011（细化 XM）、012（全局遗漏）、013（XM CHECK）、014（观察） |
| B3 | 已执行 2 次（本轮为第 2 次），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `7337f4c1bd2093e7e4d3ad0208e09aa38893a6e5` |
| after（对象类型 tree，可解析） | `e7d8355fc16574f780cf51b134f06c72550cb666` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 7337f4c1bd2093e7e4d3ad0208e09aa38893a6e5 e7d8355fc16574f780cf51b134f06c72550cb666`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 11158 bytes；SHA256 `9a3d497dca2e1231db764e5c594934f33c3fbe50603bdf93cbb4d31ae9817b91`；UTF-8；2 文件；60 插入 / 11 删除；`git diff` 195 行 / 16 hunk |
| 与 `/tmp/m015_b3_r2.diff` | 字节与 SHA256 全等（本席自算为准，副本仅对照）；`cmp` 一致 |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3729 / after 3729；`comm -3` 空（无增删路径） |
| HEAD | `a041458d06051fed3811d562764cd5900bba6f72` = `origin/main` |
| findings.md / ledger.md | 两树 blob 相同（本 diff 未改）：findings `8d4aba11c6c530eb30183ddbfcd6afe312b44982`，ledger `04f88f8d52d630d94c1db88ae1eebbff14cb027c` |
| 三 HTML 两树 blob | BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3` / BEQR `86a717930f9303d4f131ee2864478baa14450e6d`，完全相同 |
| 锁/sidecar 两树 blob | 相同（未改）：lock `8db3fc02ce8a6dc998863a9784e7e88af511b960` / sidecar `41f575026b693af74d2a36e6faf86d260ef6f6ca` |
| OpenAPI | 两树 blob 相同：`docs/openapi/openapi-p0.yaml` `1f056b360dc60c37b6c026c761f0fa57b3688e63` / `docs/openapi/openapi-p0.json` `32231790707d8a93a303ca47033b7819ee0e3b71` |
| 源码/迁移/权限种子 | `git diff --name-only` 仅下表 2 路径；`backend/src/main/resources/db/migration` 与权限种子空 |

两文件 blob（工作区 `git hash-object` = after；B3-r2 修复已在工作区，未入主索引）：

| 文件 | before | after / work |
|---|---|---|
| `backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java` | `7684e2a3fa5677d3c481f0b1fc21f35e2a626a6b` | `75663eebb0b0bf40ef89967877b832c961eb21f2` |
| `backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java` | `ec3685660dbe69751bc1f1f94488c8c1a433f78b` | `515b2dff446b13a12b0c38fc6378295a1301f548` |

`git diff --stat --binary`：`2 files changed, 60 insertions(+), 11 deletions(-)`。`git diff --name-status`：两处 `M`。工作区相对 HEAD 另有 r1 已改的另外 7 个源/测文件、findings/ledger、以及本模块 B1/B4-r1 双席 raw（未跟踪）。那些路径不在本轮 2 文件 diff 内（findings/ledger 两树 blob 全等），不按本轮修复内容审计。本席只审派发的 before/after 对。本席未改任何源文件，只新建本 raw。验证：主仓只读 mvn 测试 + `git show`/`git diff`；RED 在 `/tmp/m015_b4_r2_red_backend_446421` 副本复现，未改主仓。未 commit/push/PR/merge/部署。未对主索引 `git add`。

越界对照（两树 blob 全等，不在 diff 内）：`AuthorizationService.java` `6907271481fd49d94b07d39c49b019b7e2088423`；`FieldLabels.java` `51d75c79e8f1bfb40e3c67d5ac47e5c094b42f78`；`governance/PlatformController.java` `567dc33c2ae2ef76448619cc2eb00c7961653a32`；`FloorPlanImportPage.tsx` `2a5239ab6ed0f9e642177cd4a8b34db64164a967`；`V003__p0_access_permission_authz_slice.sql` `a13ebe87a1a2e5a8fb160eac6980a4cd4cd84812`；`V261__platform_admin_full_permissions.sql` `8d8c308da688090f0fac1efd47a5dc33826d7e20`。

---

## 2. 锁、分类与本轮队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M015 的集合。本轮只用定稿 001/003 已引用的 ID。不 LOCK-AMEND。四分类本轮不重开。

B4-r1 本席票：范围归因 PASS、内容 FAIL——001 派生主键超 VARCHAR(64)（现网键候选 id 68、UUID 键图形/任务 id 80）；003 生产路径先调会抛的 `currentMerchantScope()` 致项目员工到不了项目 scope，且测试桩伪造生产不存在的组合。002/004/005/006/007/008/009 已 PASS。席甲核源后接受并撤回 001/003 内容票（self-raw V0.02）。整批回 B3-r2 只再修 001/003。

B3-r2 派发边界（与席甲撤回段最小处置一致）：

- 只改本模块文件与其测试类；「// 文件版本：V0.02」随该文件所属 ID。
- **001**：派生主键改 `derivedId(prefix, seed)=前缀+SHA-256 前 32 位十六进制`（图形 `fps_` / 任务 `fpi_` / 发布图形 `fps_` 三处，恒 36 字符；候选/错误 id 仍 `fpic_`/`fpie_`+`slug(taskId+"_"+n)` ≤46），不改迁移。
- **003**：五处 `currentMerchantScope()` 改永不抛的 `currentMerchantIdQuietly()`（`AuthorizationService:440-455`，平台管理员桥为空）；测试桩改为生产语义（项目员工 `currentMerchantScope` 抛 `MERCHANT_SCOPE_REQUIRED`、`currentMerchantIdQuietly` 空、项目 scope 有值；商户员工 `currentProjectScopeQuietly` 抛 `PROJECT_SCOPE_REQUIRED`；平台管理员桥三者皆空）。
- 禁止：改迁移/OpenAPI/三份 HTML/锁/权限种子/PlatformController/前端/FieldLabels；新增字段或接口；把 010/011/012/013 当本批可修；重写 002/004–009。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary 7337f4c1bd2093e7e4d3ad0208e09aa38893a6e5 e7d8355fc16574f780cf51b134f06c72550cb666`

before tree = `7337f4c1bd2093e7e4d3ad0208e09aa38893a6e5`

after tree = `e7d8355fc16574f780cf51b134f06c72550cb666`

字节 11158；SHA256 `9a3d497dca2e1231db764e5c594934f33c3fbe50603bdf93cbb4d31ae9817b91`；UTF-8；无 NUL、无 CR。完整原文如下。

````diff
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
````

---

## 4. 逐 ID 修复内容

### M015-001（技术性 / 是）— PASS

B4-r1 FAIL：`"fps_"/"fpi_"+slug(floorPlanId+"_"+key)` 把平面图域拼进主键后，现网 `fpi-${Date.now()}`（17 字符）下候选/错误 id 68＞`VARCHAR(64)`；UUID 键下图形/任务 id 80；发布图形 id 按 `fps_+slug(taskId+"_"+candidate.id())` 更长。内存仓锁不住列宽。

本轮实现（after `FloorPlanService.java` 677 行，blob `75663eeb…`）：

- 新增私有 `derivedId(prefix, seed) = prefix + sha256(seed).substring(0, 32)`，复用既有 `sha256`（SHA-256 十六进制 64 位，`HexFormat` 小写）。
- 三处改为 `derivedId`：保存图形 `fps_`（`:268`）、导入任务 `fpi_`（`:296`）、发布图形 `fps_`（`:393`）。种子仍含平面图域（`floorPlanId+"_"+key` 或 `importTaskId+"_"+candidate.id()`，而 taskId 自身由含 `floorPlanId` 的种子派生）。
- 候选/错误仍 `fpic_`/`fpie_`+`slug(taskId+"_"+n)`，未改迁移、未改 OpenAPI、未改列类型。

本席独立长度实算（Python `hashlib.sha256`，与 Java SHA-256 UTF-8 同算法；`fp_`+UUID=39）：

| 键 | r1 公式长度 | r2 公式长度 |
|---|---|---|
| 任务 id（现网 `fpi-`+13 位时间戳） | 61 | **36**（`fpi_`+32 hex） |
| 候选/错误 n=1 | **68** | **43** |
| 候选 n=2000（`MAX_IMPORT_CANDIDATES`） | — | **46** |
| UUID 键图形 id | **80** | **36** |
| 发布图形 id | 134 | **36** |

`information_schema`（本席只读查 `complex_test`）四表加 `mall_floor_plans` 的 `id` 均为 `character varying 64`：`floor_plan_shapes` / `floor_plan_import_tasks` / `floor_plan_import_candidates` / `floor_plan_import_errors` / `mall_floor_plans`。与 V023/V034 源码一致。r2 全部派生 id ≤46≤64。

确定性与平面图域：同一 `(floorPlanId, key)` → 同一 SHA-256 → 同一主键（幂等 upsert 仍落同一行）；不同平面图 → 不同种子 → 不同 128-bit 前缀（生日碰撞可忽略）。r1 两例 `saveShapeWithSameKeyAcrossFloorPlansDoesNotOverwrite` / `createImportTaskWithSameKeyAcrossFloorPlansDoesNotOverwrite` 仍在，GREEN 绿。发布路径仍只 `saveShape` 追加、不删既有图形、已发布任务拒绝再发（r1 语义未动）。

最懒可行：不改迁移、不改列宽、不新依赖，只把会随输入变长的 slug 换成既有哈希截断。符合 GEN-006/GEN-007/GEN-002/BSM-001「主键加入平面图域」且补上 B4-r1 指出的列宽约束。无新业务字段/接口/规则。

新测 `derivedIdsFitVarchar64Columns`：现网键 `fpi-1757570000000` 走导入+发布，UUID 键走 `saveShape`；断言 task/候选/全部图形/新图形 id 均 `hasSizeLessThanOrEqualTo(64)`。本席 RED 副本（after 测试 + before 服务 blob `7684e2a3…`）该例红在候选 id **68**（与 B4-r1 实算一致），能失败。GREEN 绿。未覆盖 `fpie_` 错误 id，公式与候选对称、n=1 同为 43，不升缺陷。

### M015-003（细化 / 是）— PASS

B4-r1 FAIL：五处仍先调会抛的 `currentMerchantScope()`（`:422-431`：非平台管理员且无 merchantId → `MERCHANT_SCOPE_REQUIRED`）。项目员工（有 organizationId、无 merchant）永远到不了 `currentProjectScopeQuietly` / `findAllFloorPlansByProject` / `CROSS_PROJECT_FORBIDDEN`。测试把「商户空 ∩ 项目有」桩成 `currentMerchantScope()` 返回 empty——生产不存在该组合。

本轮实现：`FloorPlanService` 已 **零** `currentMerchantScope` 调用。五处全部改为 `currentMerchantIdQuietly()`（生产 `:440-455` 永不抛，平台管理员桥 empty，异常 empty）：

1. `listFloorPlans:72` — 商户 quietly 有值 → `findAllFloorPlansByMerchant`（**不**再调项目 scope）；否则 `currentProjectScopeQuietly().map(findAllFloorPlansByProject).orElseGet(findAllFloorPlans)`。
2. `createFloorPlanOnce:230` — 写入 `merchant_id`（empty→null）。
3. `validateCreateRequest:511` — quietly 空才比项目 scope，不等 → `CROSS_PROJECT_FORBIDDEN`。
4. `checkProjectOwnership:602` — quietly 有值直接 return（商户走商户闸）；否则项目 scope 不等 → `CROSS_PROJECT_FORBIDDEN`。
5. `checkMerchantOwnership:612` — quietly 有值且不等 → `CROSS_MERCHANT_FORBIDDEN`。

生产语义（本席核 `AuthorizationService`，本 diff 未改该文件 blob `69072714…`）：

| 角色 | `currentMerchantScope` | `currentMerchantIdQuietly` | `currentProjectScopeQuietly` |
|---|---|---|---|
| 平台管理员桥 | empty（不抛） | empty（不抛） | empty（`isPlatformAdminBridge` 短路，不抛） |
| 项目员工（无 merchant、有 org） | 抛 `MERCHANT_SCOPE_REQUIRED` | empty | `Optional.of(organizationId)` |
| 商户员工（有 merchant、无 org） | `of(merchantId)` | `of(merchantId)` | `requireProjectScope` → 抛 `PROJECT_SCOPE_REQUIRED` |

测试桩已改为上表（`noScopeAuth` / `serviceWithProjectScope` / `serviceWithMerchantScope`）。项目员工三例在生产语义下才能到达目标分支：

- `listFloorPlansFiltersToProjectScope`：quietly 空 → `currentProjectScopeQuietly` 有值 → `findAllFloorPlansByProject`；若仍调 `currentMerchantScope` 则抛（本席 RED：`listFloorPlans:72` 栈顶即桩的 `MERCHANT_SCOPE_REQUIRED`）。断言只含 `prj_1`。同例 `service.listFloorPlans()`（平台管理员桥）size=2，全量仍在。
- `accessingFloorPlanBelongingToDifferentProjectIsForbidden`：`listShapes` → `ensureFloorPlanExists` → `checkProjectOwnership`；RED 为 `MERCHANT_SCOPE_REQUIRED`，GREEN 为 `CROSS_PROJECT_FORBIDDEN`。
- `createFloorPlanOutsideProjectScopeIsForbidden`：`validateCreateRequest` 项目不等；RED 同为 `MERCHANT_SCOPE_REQUIRED`，GREEN 为 `CROSS_PROJECT_FORBIDDEN`，且 `floorPlanSaveCount` 仍 0。

商户员工绝不触发项目 scope：`listFloorPlansFiltersToMerchantScope` 走 `serviceWithMerchantScope`（`currentProjectScopeQuietly` 抛）。若列表在商户有值后仍调用项目 API，此例会变 `PROJECT_SCOPE_REQUIRED`。GREEN 该例绿，且只见本商户。`createFloorPlanWithSameKeyAcrossMerchantsDoesNotOverwrite` / `accessingFloorPlanBelongingToDifferentMerchantIsForbidden` 同桩，GREEN 绿。短路由：`merchantScope.isPresent() ? findByMerchant : project…` 与 `checkProjectOwnership` 的 early return 保证商户路径不调会抛的项目 API。

平台管理员仍全量：`noScopeAuth` 三者皆空；`listFloorPlansFiltersToMerchantScope` 的 `resultAdmin.hasSize(2)` 与项目列表例的 `service.listFloorPlans().items().hasSize(2)` 锁住 `orElseGet(findAllFloorPlans)`。

B2-r2 非阻断（「不把无组织全局员从现网全量改成 403」）：现网本端点 `currentMerchantScope()` 对非平台管理员无商户已抛，全量只落在平台管理员桥。r2 平台管理员桥 `currentMerchantIdQuietly` 空且 `currentProjectScopeQuietly` 因 `isPlatformAdminBridge` 空，仍全量。无组织非平台管理员的错误码由 `MERCHANT_SCOPE_REQUIRED` 变为可能的 `PROJECT_SCOPE_REQUIRED`（`currentProjectScopeQuietly:510-515` 经 `requireProjectScope` 抛，属既有 AuthorizationService 行为，本 diff 未改）。不把全量观众改成 403，不新开 ID。

最懒可行：调用仓内已有、TicketingService/AdmissionGuard 已用的 quietly API，不改 AuthorizationService、不改权限种子、不新增接口。无新业务发明。

---

## 5. §5 闸 3 逐行归因

每一变更行（含两文件首行后 `// 文件版本：V0.01`→`V0.02`，按操作员版本铁律随该文件所属 ID）均可归因到 001 或 003。无无法归因行。

| 路径（hunk） | 位置 | 归因 |
|---|---|---|
| FloorPlanService.java 1 | `@@ -1,5 +1,5 @@` 版本 V0.02 | 版本铁律，随本文件 001+003 |
| FloorPlanService.java 2 | `@@ -69,8 +69,8 @@` `listFloorPlans` quietly + 注释 | **003** |
| FloorPlanService.java 3 | `@@ -227,7 +227,7 @@` `createFloorPlanOnce` | **003** |
| FloorPlanService.java 4 | `@@ -265,7 +265,7 @@` `saveShape` `derivedId("fps_",…)` | **001** |
| FloorPlanService.java 5 | `@@ -293,7 +293,7 @@` 任务 `derivedId("fpi_",…)` | **001** |
| FloorPlanService.java 6 | `@@ -390,7 +390,7 @@` 发布图形 `derivedId("fps_",…)` | **001** |
| FloorPlanService.java 7 | `@@ -508,7 +508,7 @@` 创建项目 scope 闸 | **003** |
| FloorPlanService.java 8 | `@@ -599,7 +599,7 @@` `checkProjectOwnership` | **003** |
| FloorPlanService.java 9 | `@@ -609,7 +609,7 @@` `checkMerchantOwnership` | **003** |
| FloorPlanService.java 10 | `@@ -647,6 +647,12 @@` `derivedId` 方法 | **001** |
| FloorPlanServiceTests.java 1 | `@@ -1,5 +1,5 @@` 版本 V0.02 | 版本铁律，随本文件 001+003 |
| FloorPlanServiceTests.java 2 | `@@ -359,6 +359,26 @@` `derivedIdsFitVarchar64Columns` | **001** |
| FloorPlanServiceTests.java 3 | `@@ -419,6 +439,7 @@` 平台管理员桥注释 | **003** |
| FloorPlanServiceTests.java 4 | `@@ -426,6 +447,11 @@` `noScopeAuth.currentMerchantIdQuietly` 空 | **003** |
| FloorPlanServiceTests.java 5 | `@@ -439,9 +465,15 @@` 项目员工生产语义桩 | **003** |
| FloorPlanServiceTests.java 6 | `@@ -459,11 +491,22 @@` 商户员工生产语义桩 | **003** |

16 hunk 全部命中 001 或 003。无「无 ID」行。闸 3 越界判定：本 diff 未改三份业务 HTML、锁、sidecar、ledger 基准、迁移、OpenAPI、权限种子、其它模块源码、前端、FieldLabels、PlatformController。未新增业务字段/HTTP 接口。范围归因 **PASS**。

---

## 6. 越界检查

- 三 HTML / 锁 / sidecar / 两树 findings·ledger blob：未改。
- 迁移目录、OpenAPI、权限种子、`apps/`、M015 其余 21 文件、M017 未列入本 diff 的 Controller/Parser/Jdbc/Venue 测试：未改（Venue 等 r1 文件两树 blob 仍为 r1 after，本轮 diff 无名）。
- 010 草稿注释与 `is_draft` 复用停用：未改。
- `derivedId` 为私有技术函数；`currentMerchantIdQuietly` 为既有授权 API，不是新业务能力。
- 本席未在主仓写模式；RED 副本在 `/tmp/m015_b4_r2_red_backend_446421`。主仓 `git status --porcelain` 仍为 r1+r2 已改的 9 源/测文件 + findings/ledger + 未跟踪 B1/B4-r1 raw；本席只新增本 raw。

---

## 7. TDD 与独立复跑（不得只信 B3 日志）

B3 自报（`/tmp/m015_b3r2_run.log`）：RED＝改桩+加一例、服务未改 → FloorPlanServiceTests 27 例 4 红（3 例 `MERCHANT_SCOPE_REQUIRED`、1 例候选 id 68＞64）；GREEN＝改服务后 5 类 55 例全绿。本席不采信该日志为结论，只作对照。

本席独立：

- **RED**（`/tmp/m015_b4_r2_red_backend_446421`，JAVA_HOME=Temurin 21.0.12.1）：after 测试 blob `515b2dff…` + before `FloorPlanService` blob `7684e2a3…`（5 处仍 `currentMerchantScope`，0 处 `derivedId`）。`mvn -o -Dtest=com.complex.platform.floorplan.FloorPlanServiceTests test`。**Tests run: 27, Failures: 3, Errors: 1**。失败集与 `/tmp/m015_b3r2_red.log` 同四方法：`createFloorPlanOutsideProjectScopeIsForbidden` / `accessingFloorPlanBelongingToDifferentProjectIsForbidden` 期望 `CROSS_PROJECT_FORBIDDEN` 实为 `MERCHANT_SCOPE_REQUIRED`；`listFloorPlansFiltersToProjectScope` 在 `FloorPlanService.listFloorPlans:72` 调桩抛「此操作必须在商户范围内进行」；`derivedIdsFitVarchar64Columns` 候选 id 长度 **68**。断言指向真实旧行为，**能失败**。
- **GREEN**（主仓只读，仅从 `/etc/complex/complex.env` 逐行导出 `COMPLEX_DATABASE_USERNAME`/`PASSWORD` 两键，`COMPLEX_DATABASE_URL` 改写为 `jdbc:postgresql://127.0.0.1:5432/complex_test`，未 source 整个 env）：`mvn -o -Dtest=FloorPlanServiceTests,FloorPlanJdbcIntegrationTests,FloorPlanImportParserTests,FloorPlanControllerTests,VenueBusinessHoursServiceTests test`，exit 0。Surefire：Parser 4 / Jdbc 3 / FloorPlanService **27** / Controller 12 / Venue 9，合计 **55 例 Failures 0 Errors 0**。Flyway 日志：`jdbc:postgresql://127.0.0.1:5432/complex_test`，validated 390，schema v422，No migration necessary。主仓工作区两文件 blob 在测后仍等于 after tree。

GREEN 全绿与 RED 四红共同证明：001 列宽与 003 生产路径均被能失败的断言锁住，且 002/004–009 的既有例未回退。

---

## 8. 发现的问题（有 ID / 无 ID）

### 有 ID

无。001 列宽溢出与 003 生产路径/假桩均已按 B3-r2 最小处置闭合。

### 无 ID

无升队项。观察（不新开 ID、不回 B2、不否决本轮）：`derivedIdsFitVarchar64Columns` 未断言 `fpie_` 错误 id（与候选同公式）；`currentProjectScopeQuietly` 对无组织非平台管理员仍抛（既有 AuthorizationService，本 diff 未改），平台管理员全量未丢。

回退统一规则：若有仍属修复队列「是」的有 ID 项 → 整批回 B3（禁止拆批）。本轮无此类项。

---

## 9. 结论

| 项 | 票 |
|---|---|
| 内容修复是否成立 | **PASS**（001 派生主键恒落入 VARCHAR(64) 且保留平面图域/确定性/发布追加；003 项目员工可达项目列表/CROSS_PROJECT_FORBIDDEN/创建校验，商户不触发项目 scope，平台管理员仍全量；桩为生产语义；测试能失败） |
| 范围归因是否全部命中 | **PASS**（16 hunk 均可归到 001 或 003；无越界文件） |
| 总 | **PASS** |

本席 FAIL 具否决权，本轮不行使。不等于模块 PASS。下一步须席甲 B4-r2 亦内容+范围双 PASS 才进 B5。

本席未改源码、未改三份业务 HTML、未改锁、未 commit/push/PR/merge/部署。
