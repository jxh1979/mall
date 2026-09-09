CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B4-r2 修复内容复审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B4-r2（B3 第 2 次整批修复的完整 diff 复审）。2026-09-09T09:05:13Z
- before tree：`5bfe07d9f0b7e7ad5fbde58513a4e0d905961e8e`；after tree：`9350cf873f9075220c76c30a4ec2c90178d54213`；`git diff --binary 5bfe07d9f0b7 9350cf873f90` 全文 12568 bytes SHA256 69229f6ba3bbd0792bf2237ca731cb602f8b2babee94469f3038763d3d2d7a8d（全文附于本报告末尾）。
- 定稿队列（findings 第二批定稿）：026、027、028 共 3 项；本轮只修这 3 项及归因到各 ID 的最小行为测试。
- 锁四值：实算/sidecar/ledger/Grok 均 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。

## 1. diffstat

```
.../platform/governance/JdbcPlatformRepository.java | 15 +++++++++++++--
 .../complex/platform/governance/MerchantRecord.java |  4 ++--
 .../platform/governance/PlatformRepository.java     |  4 ++--
 .../platform/governance/PlatformService.java        | 19 ++++++++++++++-----
 .../governance/PlatformJdbcIntegrationTests.java    | 15 ++++++++++++++-
 .../platform/governance/PlatformServiceTests.java   | 21 ++++++++++++++++++++-
 6 files changed, 65 insertions(+), 13 deletions(-)
```

## 2. 逐块归因（每一行改动都归到定稿 ID；「版本铁律」行随其所在文件的 ID）

| 文件 | 块 | 位置 | 块内标记 ID | 归因 |
|---|---|---|---|---|
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | M009-026（注释）/027（findSpaceById 覆写） |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 2 | `@@ -164,6 +164,17 @@ class JdbcPlatformRepository implements` | M009-027 | M009-026（注释）/027（findSpaceById 覆写） |
| backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java | 3 | `@@ -184,7 +195,7 @@ class JdbcPlatformRepository implements ` | M009-026 | M009-026（注释）/027（findSpaceById 覆写） |
| backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | M009-026 |
| backend/src/main/java/com/complex/platform/governance/MerchantRecord.java | 2 | `@@ -24,7 +24,7 @@ public record MerchantRecord(` | M009-026 | M009-026 |
| backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | M009-026 |
| backend/src/main/java/com/complex/platform/governance/PlatformRepository.java | 2 | `@@ -51,7 +51,7 @@ public interface PlatformRepository {` | M009-026 | M009-026 |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | M009-028 |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 2 | `@@ -339,6 +339,15 @@ public class PlatformService {` | M009-028 | M009-028 |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 3 | `@@ -373,7 +382,7 @@ public class PlatformService {` | — | M009-028 |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 4 | `@@ -471,7 +480,7 @@ public class PlatformService {` | — | M009-028 |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 5 | `@@ -542,8 +551,8 @@ public class PlatformService {` | M009-006, M009-028 | M009-028 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | M009-027 |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 2 | `@@ -357,6 +357,19 @@ class PlatformJdbcIntegrationTests {` | M009-027 | M009-027 |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | M009-028 |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 2 | `@@ -237,6 +237,25 @@ class PlatformServiceTests {` | M009-028 | M009-028 |

## 3. 红绿证据（B3-r2 运行记录 /tmp/m009_b3r2_run.log 原文）

```
START=2026-09-09T08:47:01Z
BEFORE_TREE=5bfe07d9f0b7e7ad5fbde58513a4e0d905961e8e
=== PHASE RED: tests for 027 028 ===
PATCHED test/java/com/complex/platform/governance/PlatformServiceTests.java
BUMPED test/java/com/complex/platform/governance/PlatformServiceTests.java
PATCHED test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
BUMPED test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
RED_DONE ['027', '028']
RED_EXIT=1
[ERROR] Tests run: 13, Failures: 2, Errors: 0, Skipped: 0, Time elapsed: 4.044 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.findSpaceByIdIsNotTruncatedByListLimit -- Time elapsed: 0.105 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.residualMigrationCreatesBindingContractTablesAndSeedsPermissions -- Time elapsed: 0.006 s <<< FAILURE!
[ERROR] Tests run: 30, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.109 s <<< FAILURE! -- in com.complex.platform.governance.PlatformServiceTests
[ERROR] com.complex.platform.governance.PlatformServiceTests.archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding -- Time elapsed: 0.002 s <<< FAILURE!
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.671 s -- in com.complex.platform.governance.PlatformControllerTests
[ERROR] Tests run: 53, Failures: 3, Errors: 0, Skipped: 0
=== PHASE GREEN: fixes for 026 027 028 ===
PATCHED main/java/com/complex/platform/governance/JdbcPlatformRepository.java
PATCHED main/java/com/complex/platform/governance/MerchantRecord.java
PATCHED main/java/com/complex/platform/governance/PlatformRepository.java
BUMPED main/java/com/complex/platform/governance/JdbcPlatformRepository.java
BUMPED main/java/com/complex/platform/governance/MerchantRecord.java
BUMPED main/java/com/complex/platform/governance/PlatformRepository.java
PATCHED main/java/com/complex/platform/governance/JdbcPlatformRepository.java
PATCHED main/java/com/complex/platform/governance/PlatformService.java
BUMPED main/java/com/complex/platform/governance/PlatformService.java
GREEN_DONE ['026', '027', '028']
GREEN_EXIT=1
[ERROR] Tests run: 13, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 3.226 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.residualMigrationCreatesBindingContractTablesAndSeedsPermissions -- Time elapsed: 0.007 s <<< FAILURE!
[INFO] Tests run: 30, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.110 s -- in com.complex.platform.governance.PlatformServiceTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.091 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.851 s -- in com.complex.platform.governance.PlatformControllerTests
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.067 s -- in com.complex.platform.leasing.LeasingServiceTests
[ERROR] Tests run: 4, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.050 s <<< FAILURE! -- in com.complex.platform.leasing.LeasingJdbcIntegrationTests
[ERROR] com.complex.platform.leasing.LeasingJdbcIntegrationTests.migrationCreatesLeasingTablesAndSeedsPermissions -- Time elapsed: 0.007 s <<< FAILURE!
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.085 s -- in com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests
[ERROR] Tests run: 76, Failures: 2, Errors: 0, Skipped: 0
AFTER_TREE=9350cf873f9075220c76c30a4ec2c90178d54213
END=2026-09-09T08:47:27Z
```

## 4. 本席复审结论

见文末追加的判定段。

## 附：完整 diff（before→after，--binary）

```diff
diff --git a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
index 6536bda2..7c252208 100644
--- a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.01
+// 文件版本：V0.02
 import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
@@ -164,6 +164,17 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
         return count != null && count > 0;
     }
 
+    /** M009-027：空间存在性按主键查询，不受 listSpaces() 的 LIMIT 500 截断（上级空间 / 招商意向空间校验用）。 */
+    @Override
+    public Optional<SpaceRecord> findSpaceById(String spaceId) {
+        return jdbcTemplate.query("""
+                SELECT id, project_id, merchant_id, parent_space_id, space_name, space_type, floor_no,
+                       area_square_meter, polygon_points::text AS polygon_points, status, created_at, updated_at
+                  FROM spaces
+                 WHERE id = :id
+                """, Map.of("id", spaceId), this::mapSpace).stream().findFirst();
+    }
+
     @Override
     public java.util.Optional<MerchantRecord> findMerchantById(String merchantId) {
         return jdbcTemplate.query("""
@@ -184,7 +195,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
 
     @Override
     public long countMerchantReferences(String merchantId) {
-        // V259：商户被业务表以 merchant_id 引用的活条数，>0 拒硬删（防连锁孤儿/资金孤儿）。
+        // V259/M009-026：商户被业务表以 merchant_id 引用的活条数，>0 拒绝删除（软删除标记同样不允许，防连锁孤儿/资金孤儿）。
         // 元数据驱动：动态枚举所有带 merchant_id 列的基表并逐表 COUNT，避免硬编码清单随新表漂移
         // ——结算(settlement_transactions/items)、票单(ticket_orders/refunds)、储值(customer_stored_value_accounts)、
         //   公摊(cam_allocations)等资金承载表极易漏，漏则连锁孤儿/资金记录指向已删商户。
diff --git a/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java b/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
index 595fea23..c2deb1fe 100644
--- a/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
+++ b/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import java.math.BigDecimal;
 import java.time.Instant;
@@ -24,7 +24,7 @@ public record MerchantRecord(
         String status,
         Instant createdAt,
         Instant updatedAt,
-        // V260 数据存档与审批后删除范式(照 V255 权益模板试点)。存档=软标记可恢复;删除=硬删走审批。
+        // V260 数据存档与审批后删除范式(照 V255 权益模板试点)。存档=软标记可恢复;删除=审批后软删除标记(delete_status=approved,行保留可查可恢复,BSM-004/M009-026)。
         // archivedAt 非空=已存档;deleteStatus none|pending|approved。放末位便于既有构造点追加。
         Instant archivedAt,
         String archivedBy,
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java b/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
index 6648d901..57d157dd 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import java.util.List;
 import java.util.Optional;
@@ -51,7 +51,7 @@ public interface PlatformRepository {
     }
 
     /**
-     * V260：统计商户被【核心运营表】以 merchant_id 引用的活跃条数，>0 则拒绝硬删（防连锁孤儿）。
+     * V260：统计商户被【核心运营表】以 merchant_id 引用的活跃条数，>0 则拒绝删除（含软删除标记，M009-026；防连锁孤儿）。
      * 覆盖表见 JdbcPlatformRepository 实现。默认 0（内存 fake 无引用表）。
      */
     default long countMerchantReferences(String merchantId) {
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformService.java b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
index c29d5c8a..453be3c5 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformService.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -339,6 +339,15 @@ public class PlatformService {
                 .orElseThrow(() -> ApiException.notFound("MERCHANT_NOT_FOUND", "商户不存在或已被删除"));
     }
 
+    /** M009-028：BSM-004「下线就不能再用」——新项目/空间/绑定只能引用未存档且未审批删除的商户；档案更新、存档/恢复与删除流程仍经 requireMerchant 命中已存档行。 */
+    private MerchantRecord requireActiveMerchant(String id) {
+        MerchantRecord merchant = requireMerchant(id);
+        if (merchant.archivedAt() != null || "approved".equals(merchant.deleteStatus())) {
+            throw ApiException.badRequest("MERCHANT_ARCHIVED", "商户已存档或已删除，不能再用于新的项目、空间或主体绑定");
+        }
+        return merchant;
+    }
+
     public PageResponse<ProjectRecord> listProjects() {
         return listProjects(DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
     }
@@ -373,7 +382,7 @@ public class PlatformService {
                     String venueType = requireText(request == null ? null : request.venueType(), "venue_type");
                     String merchantId = trimToNull(request.merchantId());
                     if (merchantId != null) {
-                        requireMerchant(merchantId);
+                        requireActiveMerchant(merchantId);
                     }
                     String status = requireProjectStatus(request.status());
                     Instant now = Instant.now();
@@ -471,7 +480,7 @@ public class PlatformService {
                     }
                     String merchantId = trimToNull(request.merchantId());
                     if (merchantId != null) {
-                        requireMerchant(merchantId);
+                        requireActiveMerchant(merchantId);
                     }
                     String parentSpaceId = trimToNull(request.parentSpaceId());
                     if (parentSpaceId != null && repository.findSpaceById(parentSpaceId).isEmpty()) {
@@ -542,8 +551,8 @@ public class PlatformService {
                 () -> {
                     BusinessEntityBindingRequest actual = requireRequest(request);
                     String merchantId = requireText(actual.merchantId(), "merchant_id");
-                    // M009-006：四类主体绑定的经营对象与三个主体都必须真实存在（BEQR-143/PGM-026）。
-                    requireMerchant(merchantId);
+                    // M009-006：四类主体绑定的经营对象与三个主体都必须真实存在（BEQR-143/PGM-026）；M009-028：且商户未下线。
+                    requireActiveMerchant(merchantId);
                     String operationEntityId = requireBusinessEntity(actual.operationEntityId(), "operation_entity_id");
                     String contractEntityId = requireBusinessEntity(actual.contractEntityId(), "contract_entity_id");
                     String settlementEntityId = requireBusinessEntity(actual.settlementEntityId(), "settlement_entity_id");
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
index f81d5c6d..dec3176b 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -357,6 +357,19 @@ class PlatformJdbcIntegrationTests {
         assertThat(repository.businessEntityExists("be_definitely_missing")).isFalse();
     }
 
+    // M009-027：空间存在性按主键查询，不受 listSpaces() LIMIT 500 截断（上级空间/招商意向空间校验）。
+    @Test
+    void findSpaceByIdIsNotTruncatedByListLimit() {
+        Instant base = Instant.parse("2026-01-01T00:00:00Z");
+        for (int i = 0; i < 501; i++) {
+            Instant t = base.plusSeconds(i);
+            repository.saveSpace(new SpaceRecord("sp_limit_" + i, null, null, null, "空间" + i, "shop", "1F",
+                    null, List.of(), "active", t, t));
+        }
+        assertThat(repository.listSpaces()).hasSize(500);
+        assertThat(repository.findSpaceById("sp_limit_0")).isPresent();
+    }
+
     private String regclass(String tableName) {
         return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
     }
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
index 4678ae92..308166f8 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import com.complex.platform.access.AccessActor;
 import com.complex.platform.auth.SessionIssuer;
@@ -237,6 +237,25 @@ class PlatformServiceTests {
                 .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("SPACE_NOT_FOUND"));
     }
 
+    // ---- M009-028：已存档/已批删商户不能再被新项目、空间、绑定引用（BSM-004「下线就不能再用」） ----
+    @Test
+    void archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding() {
+        MerchantRecord merchant = seedMerchant("seed-archived-ref");
+        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-ref", "测试存档理由"));
+        seedEntity("be_ref_1");
+        assertThatThrownBy(() -> service.createProject(new ProjectRequest(
+                "project-archived-mch", "create", List.of(), merchant.id(), "bar", "P0 Bar", null, null, null, "draft")))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_ARCHIVED"));
+        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                "sp-archived-mch", "create", List.of(), null, merchant.id(), null, "A", "shop", null, null, null, "active")))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_ARCHIVED"));
+        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "binding-archived-mch", "ref", List.of(), merchant.id(), "be_ref_1", "be_ref_1", "be_ref_1", null, null)))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_ARCHIVED"));
+        assertThat(platformRepository.listProjects()).noneMatch(p -> merchant.id().equals(p.merchantId()));
+        assertThat(platformRepository.listBusinessEntityBindings()).isEmpty();
+    }
+
     // ---- M009-008：生效绑定同步商户三个主体列（PGM-026「引用同一组绑定」） ----
     @Test
     void createBusinessEntityBindingSyncsMerchantEntityColumns() {

```

## 4. 本席复审结论（席甲，作者自审票）

本席逐块读完上述 diff 全文（6 文件，65+/13-，16 hunk），并以 §2 归因表核对每一行。

### 4.1 修复内容（逐 ID）

| ID | 修复内容 | 证据 | 本席票 |
|---|---|---|---|
| 026 | `JdbcPlatformRepository:187`、`MerchantRecord:27`、`PlatformRepository:54` 三处注释改为软删除口径（占用闸「拒绝删除（含软删除标记）」、记录注释「审批后软删除标记，行保留可查可恢复」） | 编译级；20 文件 `硬删|物理删除` 现为 0 处；行为未动 | PASS |
| 027 | Jdbc 覆写 `findSpaceById` 按主键 `WHERE id = :id`（与 `findMerchantById` 同法，复用 `mapSpace`），`listSpaces()` LIMIT 500 不变 | `findSpaceByIdIsNotTruncatedByListLimit` 红（Optional 为空）→ 绿；同一覆写同时修 `createSpace` 上级空间闸与 `spaceExists`（LeasingService:424） | PASS |
| 028 | 新增 `requireActiveMerchant`（存在且未存档且 delete_status≠approved，否则 400 中文 `MERCHANT_ARCHIVED`），只用于 `createProject`/`createSpace`/`createBusinessEntityBinding` 三处；`requireMerchant` 其余路径（更新/存档/恢复/删除申请与审批）不变 | `archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding` 红（三处均未抛）→ 绿；既有 29 条 ServiceTests 仍绿 | PASS |

### 4.2 范围归因（§5 闸 3）

- 16 hunk 全部落到定稿 026/027/028；6 个「文件版本：V0.01→V0.02」hunk 按版本铁律随所属 ID（Jdbc 承载 026+027，其余单一 ID）。
- 未改：三份业务 HTML、`plan/*`、锁、sidecar、ledger 基准、findings、权限种子、迁移、029–035 与已修 15 项对应内容、其它模块（`git diff --stat before after -- docs/*.html docs/audit-2026-09/plan/ ledger findings db/` 为空）。
- 未提交/推送/合并/部署；after tree 再次实取相同。

### 4.3 环境红（不由本 diff 引入）

`PlatformJdbcIntegrationTests.residualMigration…` 与 `LeasingJdbcIntegrationTests.migrationCreates…`（399≠409）：M009-017 环境项，红绿两阶段一致。

### 4.4 结论

内容修复：**PASS**。范围归因：**PASS**。总：**PASS**（作者自审票；须 Grok B4-r2 亦 PASS 才进 B5-r2）。本席在本报告落盘前未读取 `M009-b4-r2-grok-raw.md`。
