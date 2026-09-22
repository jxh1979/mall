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


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B4 第2轮 Codex 完整复核
版本：V0.02
模块状态：IN_PROGRESS；B3已执行2/3。本报告是Codex自审票，不替代Grok票。Codex在Grok调用12876运行期间独立审完整5文件22块，尚未读取本轮Grok raw/JSON。

## 事实与授权
用户先指出054-056未批准，Codex停止；随后用户明确“如果不是修改html，那么修复没有问题”，批准继续这三项代码/测试。历史先改后确认不倒填成事前批准。业务HTML逐字未动，早前013/038等批准保留。这里只审冻结本轮全部diff及直接依赖，整M009全量B5另行进行。
before=e60778ec7fd700bcfa671887ffa62ba85897d21b
after=e4c10154a405e61e1bfad9fedb6fde11068f653a
HEAD=e456ba96c84185c8d39f18f548011daac48a6466
diff字节=20857
diff-SHA256=269c28c32ab927579fb8d5a40f0fe16bc29b9c623546c085c1792f87fa1bbc51
真实暂存区SHA256=1657ef91568fc6c00e7fd66f58f4c00461260651323d2ecbf616238602b973a2，B3前后不变。
B3前后四值全部b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a，第四来源modules/M009-b4-r1-grok-raw.md；LOCK-AMEND-COUNT=1。
findings V2.10当前23行17是6否，本轮有变动仅054/055/056。原14项已在r1实现；B4-r1归因PASS但内容FAIL，因此未提前清队列。

## 复核结论
修复内容：PASS
逐行归因：PASS
当前发现新问题：0。
所有22块均可归到已定稿YES；没有归因到NO、没有新增资金/员工生产权限豁免。全文diff附后，未按路径过滤。git diff --check before after exit0。不能将本票写成双PASS，须等待完整Grok票。

## 三条审计线及直接代码证据
第一性原理：最高权限必须能够到达已有审批入口，否则HTML BSM第570-577行的“具备最高权限的人审批”无法执行。AccessService.java:485-548只在该方法的前置角色检查接纳既有isMasterAuthority；没有发明身份、权限或状态。AuthorizationService.java:139-144仍要求active和SessionIssuer固定类型/ID/用户名，普通platform_admin不被误认。共享requirePlatformAdmin未变，非本方法入口未扩权。其它两项是原业务测试资料补全，不发明新业务需求。
顺从式：AccessController.java:192-201仍在审批动作要求access.role.assign与governance.supervisor_review.approve；入口不移除该检查。AccessService.java:499-545保留存档、待审、占用、软删、恢复及成功审计。SupervisorReviewService.java:201-214先要求受益人、拦受益人和申请人自审；只补M006的明确permission-target和schedule，并通过精确MASTER_AUTHORITY_REQUIRED断言证明普通最高职级到达身份守卫。真USB审批仍有完整决策结果。
对抗式：普通staff生命周期拒绝PLATFORM_ADMIN_REQUIRED，普通admin审批拒绝MASTER_AUTHORITY_REQUIRED；真USB驳回/重申请/批准/恢复/存档/本人直接申请均完成且保留数据。占用仍拒绝，列表读取仍切回普通管理员，未给USB全局列表放行。Bar五个用例使用对应商户操作者，既有customer.stored_value.refund授权只在测试仓储建立；退款金额、渠道限制、键空间及幂等断言保留。通知消费以商户卡验证2300扣款/5700余额/card_type=merchant，同时新增原无merchant范围项目卡consumeInternal拒绝MERCHANT_SCOPE_REQUIRED、余额5000不动、消费通知0；项目卡充值通知保持。不能把商户卡消费测试冒称项目卡消费已可用。M010同一商户管理和本人读取仍不能出现手机号身份证原文。
冻结依据：054 BSM-004/GEN-005/GEN-008；055 GEN-003/GEN-005/GEN-008/PGM-041；056 GEN-003/GEN-005/GEN-008，业务原文治理HTML529与653。采用现有映射，不新增判据。

## 技能与验证
本会话已实际读取并沿用控制提示词收窄的using-superpowers、requesting/receiving-code-review、grilling、diagnosing-bugs、systematic-debugging、TDD与verification-before-completion：追入口和权限依赖、检查对抗断言、验证精确错误与不变量，未派第三席。本轮graft ask changePositionTemplateLifecycle --source成功；截断处已回源读取完整方法。未用任何未核实技能。
054先红：新增真实身份测试在旧入口失败PLATFORM_ADMIN_REQUIRED，Maven exit1；修后054/056两项0失败0错误；055三类目标13项全过。最终不是只采定向测试，运行完整11类283项，0失败0错误0跳过，JDK21与complex_test。现有失败8项并非身份类型修复制造，本次合法前提修复让测试达到原目的；没有靠恢复假master掩盖。执行命令由安全Python只读三个数据库变量，mvn -o -q -Dtest=PlatformServiceTests,PlatformJdbcIntegrationTests,PlatformControllerTests,LeasingServiceTests,LeasingSignAtomicityJdbcIntegrationTests,AuthorizationServiceTests,SupervisorReviewServiceTests,M006SecurityRegressionTests,M010RegressionTests,BarStoredValueFundChainJdbcIntegrationTests,StoredValueNotificationIntegrationTest -Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test test，exit0。fresh XML结果：
MAVEN_EXIT=0
TEST-com.complex.platform.governance.PlatformServiceTests.xml {'tests': '69', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.172'}
TEST-com.complex.platform.governance.PlatformJdbcIntegrationTests.xml {'tests': '23', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.455'}
TEST-com.complex.platform.governance.PlatformControllerTests.xml {'tests': '14', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.676'}
TEST-com.complex.platform.leasing.LeasingServiceTests.xml {'tests': '11', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.092'}
TEST-com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests.xml {'tests': '3', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.107'}
TEST-com.complex.platform.access.AuthorizationServiceTests.xml {'tests': '24', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.012'}
TEST-com.complex.platform.governance.review.SupervisorReviewServiceTests.xml {'tests': '44', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.218'}
TEST-com.complex.platform.access.M006SecurityRegressionTests.xml {'tests': '39', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.656'}
TEST-com.complex.platform.access.staff.M010RegressionTests.xml {'tests': '44', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.979'}
TEST-com.complex.platform.bar.BarStoredValueFundChainJdbcIntegrationTests.xml {'tests': '8', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '0.103'}
TEST-com.complex.platform.customer.StoredValueNotificationIntegrationTest.xml {'tests': '4', 'failures': '0', 'errors': '0', 'skipped': '0', 'time': '3.612'}


## 全部diff逐块归因
|块|文件|hunk|正式ID|原因|
|---|---|---|---|---|
|1|backend/src/main/java/com/complex/platform/access/AccessService.java|@@ -1,6 +1,6 @@|M009-054|本修复文件版本递增|
|2|backend/src/main/java/com/complex/platform/access/AccessService.java|@@ -485,7 +485,10 @@ public class AccessService {|M009-054|仅生命周期入口兼容真实最高权限|
|3|backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java|@@ -1,6 +1,6 @@|M009-054 / M009-056|本文件两项修复共同版本递增|
|4|backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java|@@ -389,6 +389,10 @@ class M006SecurityRegressionTests {|M009-056|补直接受益人并锁定USB审批身份断言|
|5|backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java|@@ -405,7 +409,8 @@ class M006SecurityRegressionTests {|M009-056|补直接受益人并锁定USB审批身份断言|
|6|backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java|@@ -577,6 +582,11 @@ class M006SecurityRegressionTests {|M009-054|真实USB生命周期和普通账号边界验证|
|7|backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java|@@ -585,13 +595,23 @@ class M006SecurityRegressionTests {|M009-054|真实USB生命周期和普通账号边界验证|
|8|backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java|@@ -1,6 +1,6 @@|M009-055|合法商户测试前提及既有断言|
|9|backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java|@@ -473,13 +473,15 @@ class M010RegressionTests {|M009-055|合法商户测试前提及既有断言|
|10|backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java|@@ -1,6 +1,6 @@|M009-055|合法商户测试前提及既有断言|
|11|backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java|@@ -58,6 +58,9 @@ class BarStoredValueFundChainJdbcIntegrationTests {|M009-055|合法商户测试前提及既有断言|
|12|backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java|@@ -76,6 +79,7 @@ class BarStoredValueFundChainJdbcIntegrationTests {|M009-055|合法商户测试前提及既有断言|
|13|backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java|@@ -109,6 +113,7 @@ class BarStoredValueFundChainJdbcIntegrationTests {|M009-055|合法商户测试前提及既有断言|
|14|backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java|@@ -142,18 +147,19 @@ class BarStoredValueFundChainJdbcIntegrationTests {|M009-055|合法商户测试前提及既有断言|
|15|backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java|@@ -190,6 +196,7 @@ class BarStoredValueFundChainJdbcIntegrationTests {|M009-055|合法商户测试前提及既有断言|
|16|backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java|@@ -222,6 +229,7 @@ class BarStoredValueFundChainJdbcIntegrationTests {|M009-055|合法商户测试前提及既有断言|
|17|backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java|@@ -239,6 +247,16 @@ class BarStoredValueFundChainJdbcIntegrationTests {|M009-055|合法商户测试前提及既有断言|
|18|backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java|@@ -1,8 +1,9 @@|M009-055|合法商户测试前提及既有断言|
|19|backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java|@@ -65,7 +66,7 @@ class StoredValueNotificationIntegrationTest {|M009-055|合法商户测试前提及既有断言|
|20|backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java|@@ -91,12 +92,14 @@ class StoredValueNotificationIntegrationTest {|M009-055|合法商户测试前提及既有断言|
|21|backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java|@@ -107,7 +110,7 @@ class StoredValueNotificationIntegrationTest {|M009-055|合法商户测试前提及既有断言|
|22|backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java|@@ -118,37 +121,55 @@ class StoredValueNotificationIntegrationTest {|M009-055|合法商户测试前提及既有断言|

## 完整git diff --binary before after

历史同名文件在本轮before/after树均已存在，本次保留原23037bytes/SHA256 6e456aabee69512605b40b4f8a9cff8723a8899ed6e2790a0311b03a192cc546并追加独立完整报告；本轮只采当前追加票。

```diff
diff --git a/backend/src/main/java/com/complex/platform/access/AccessService.java b/backend/src/main/java/com/complex/platform/access/AccessService.java
index 46cafc7e..3720af8c 100644
--- a/backend/src/main/java/com/complex/platform/access/AccessService.java
+++ b/backend/src/main/java/com/complex/platform/access/AccessService.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.19
+// 文件版本：V0.20
 
 import com.complex.platform.access.dto.AccessGrantApplyRequest;
 import com.complex.platform.access.dto.AccessGrantReviewRequest;
@@ -485,7 +485,10 @@ public class AccessService {
     @Transactional
     public PositionRoleTemplate changePositionTemplateLifecycle(String id, StatusChangeRequest request) {
         requireRequest(request);
-        requirePlatformAdmin(authorizationService.currentActor());
+        AccessActor actor = authorizationService.currentActor();
+        if (!authorizationService.isMasterAuthority(actor)) {
+            requirePlatformAdmin(actor);
+        }
         String action = requireText(request.action(), "action");
         String reason = requireText(request.reason(), "reason");
         return idempotencyService.execute("positionTemplateLifecycle:" + id, request.idempotencyKey(), request, () -> {
diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
index 7fdca215..3629dd5d 100644
--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.28
+// 文件版本：V0.29
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.ArgumentMatchers.*;
@@ -389,6 +389,10 @@ class M006SecurityRegressionTests {
                 "top", "permission_grant_l3", "permission_grant_l3", "access_grant_request", "request",
                 true, "pending", 3, 0, null, "理由", List.of(), Instant.now(), "requester", "top_authority", 30);
         when(reviewRepository.findPendingSupervisorReviewById("top")).thenReturn(Optional.of(pending));
+        users.put("permission-target", user("permission-target", "org", null));
+        when(reviewRepository.findSupervisorReviewSchedule("top")).thenReturn(Optional.of(
+                new com.complex.platform.governance.review.SupervisorReviewRepository.ReviewSchedule(
+                        "top", "permission-target", Instant.now().plusSeconds(3600), 0, null, null)));
         // M087：最高权限事项的链末节点为主会话节点（可变持有，裁决后 listChainNodes 反映新状态）。
         List<com.complex.platform.governance.chain.ApprovalChainNode> topChain = new ArrayList<>(List.of(
                 new com.complex.platform.governance.chain.ApprovalChainNode("n_top", "top", "main", 1, "master", null, "pending", null, null, null, null, 0)));
@@ -405,7 +409,8 @@ class M006SecurityRegressionTests {
         effect("admin", "governance.supervisor_review.approve", AccessRepository.Effect.GRANT, "approval-role", false);
         var request = new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(
                 "top-admin", "同意", List.of(), null, "approved", "同意");
-        assertThatThrownBy(() -> reviews.approve("top", request)).isInstanceOf(ApiException.class);
+        assertThatThrownBy(() -> reviews.approve("top", request)).isInstanceOfSatisfying(ApiException.class,
+                e -> assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_REQUIRED"));
         verify(reviewRepository, never()).saveSupervisorReview(any());
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                 new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
@@ -577,6 +582,11 @@ class M006SecurityRegressionTests {
                 reviewRepository, new AuditEventService(new InMemoryAuditEventRepository()), event -> { });
         service = new AccessService(repository, new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                 new AuditEventService(new InMemoryAuditEventRepository()), authorization, credentials, reviews, true, 3, 3);
+        actor("ordinary-staff", "staff", "org", null);
+        assertThatThrownBy(() -> service.changePositionTemplateLifecycle("lifecycle",
+                new StatusChangeRequest("staff-archive", "archive", "越权操作", List.of())))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PLATFORM_ADMIN_REQUIRED"));
+        actor("admin", "platform_admin", "org", null);
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("archive", "archive", "停止使用", List.of()));
         assertThat(mapping.get().archivedAt()).isNotNull();
         assertThat(service.listPositionRoleTemplates().items()).isEmpty();
@@ -585,13 +595,23 @@ class M006SecurityRegressionTests {
         doReturn(0L).when(repository).countPositionTemplateUsage("lifecycle");
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("request", "request_delete", "删除申请", List.of()));
         assertThatThrownBy(() -> service.changePositionTemplateLifecycle("lifecycle",
-                new StatusChangeRequest("ordinary-admin", "approve_delete", "同意", List.of()))).isInstanceOf(ApiException.class);
+                new StatusChangeRequest("ordinary-admin", "approve_delete", "同意", List.of())))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_REQUIRED"));
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                 new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                         com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, null, null, "active"), "unused", List.of()));
+        var masterAuthentication = SecurityContextHolder.getContext().getAuthentication();
+        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("reject", "reject_delete", "保留配置", List.of()));
+        assertThat(mapping.get().deletedAt()).isNull();
+        assertThat(mapping.get().deleteReviewId()).isNull();
+        actor("admin", "platform_admin", "org", null);
+        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("request-again", "request_delete", "重新申请", List.of()));
+        SecurityContextHolder.getContext().setAuthentication(masterAuthentication);
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("approve", "approve_delete", "同意", List.of()));
         assertThat(mapping.get().deletedAt()).isNotNull();
+        actor("admin", "platform_admin", "org", null);
         assertThat(service.listPositionRoleTemplates(true).items()).hasSize(1);
+        SecurityContextHolder.getContext().setAuthentication(masterAuthentication);
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("restore", "restore", "恢复留存配置", List.of()));
         assertThat(mapping.get().deletedAt()).isNull();
         assertThat(mapping.get().status()).isEqualTo("inactive");
diff --git a/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java b/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java
index f703bd6b..5a3d264c 100644
--- a/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access.staff;
 
-// 文件版本：V0.26
+// 文件版本：V0.27
 
 import static org.assertj.core.api.Assertions.*;
 
@@ -473,13 +473,15 @@ class M010RegressionTests {
     }
 
     @Test void OwnAndManagementProfileResponsesDoNotReturnPersonalValues() throws Exception {
-        var profile = profile(null);
+        access.updateUserScope(manager, "m010_p", registrationMerchant, manager, Instant.now());
+        actor(manager, "platform_admin", registrationMerchant);
+        var profile = profile(registrationMerchant);
         repository.saveProfile(new StaffProfileRecord(profile.id(), profile.projectId(), profile.merchantId(),
                 profile.userId(), profile.name(), "13800138000", "110101199001010000", null, "approved", manager,
                 profile.approvedAt(), null, "active", profile.createdAt(), Instant.now()));
         assertThat(json.writeValueAsString(controller.listProfiles(false))).contains("138****8000")
                 .doesNotContain("13800138000", "110101199001010000");
-        actor(profile.userId(), "staff", null);
+        actor(profile.userId(), "staff", registrationMerchant);
         assertThat(json.writeValueAsString(controller.myProfile())).contains("138****8000")
                 .doesNotContain("13800138000", "110101199001010000");
     }
diff --git a/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java
index 9b5c2e7e..93d2a14f 100644
--- a/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.bar;
 
-// 文件版本：V0.05
+// 文件版本：V0.06
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -58,6 +58,9 @@ class BarStoredValueFundChainJdbcIntegrationTests {
     @Autowired
     private JdbcTemplate jdbcTemplate;
 
+    @Autowired
+    private com.complex.platform.access.AccessRepository accessRepository;
+
     @BeforeEach
     void setUpActor() {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
@@ -76,6 +79,7 @@ class BarStoredValueFundChainJdbcIntegrationTests {
     @Test
     void settleBillWithStoredValueChannelPersistsSettlementAndDeductsBalance() {
         String customerId = seedCustomer("svchain1");
+        asMerchantOperator("merchant_svchain1");
         String billId = seedUnpaidBill("svchain1", customerId, "merchant_svchain1", 5000L);
         seedAccount(customerId, null, "merchant_svchain1", 8000L, null);
 
@@ -109,6 +113,7 @@ class BarStoredValueFundChainJdbcIntegrationTests {
     @Test
     void settleBillRejectsReplayOfForeignConsumeTransaction() {
         String customerId = seedCustomer("svchain3");
+        asMerchantOperator("merchant_svchain3");
         String billId = seedUnpaidBill("svchain3", customerId, "merchant_svchain3", 5000L);
         seedAccount(customerId, null, "merchant_svchain3", 8000L, null);
         // 预占键：同客户先以 barsv_k3attack 做了一笔小额消费（历史数据/内部键碰撞）。
@@ -142,18 +147,19 @@ class BarStoredValueFundChainJdbcIntegrationTests {
     @Test
     void idempotentReplayWithSameFingerprintReturnsOriginalTransaction() {
         String customerId = seedCustomer("svchain5");
-        seedAccount(customerId, PROJECT_ID, null, 5000L, null);
+        asMerchantOperator("merchant_svchain5");
+        seedAccount(customerId, null, "merchant_svchain5", 5000L, null);
 
-        CustomerStoredValueTransactionRecord first = storedValueService.consumeInternal(customerId, PROJECT_ID, null,
+        CustomerStoredValueTransactionRecord first = storedValueService.consumeInternal(customerId, null, "merchant_svchain5",
                 1200L, "same request retry", "retrykey_svchain5");
-        CustomerStoredValueTransactionRecord replay = storedValueService.consumeInternal(customerId, PROJECT_ID, null,
+        CustomerStoredValueTransactionRecord replay = storedValueService.consumeInternal(customerId, null, "merchant_svchain5",
                 1200L, "same request retry", "retrykey_svchain5");
 
         assertThat(replay.id()).isEqualTo(first.id());
-        assertThat(projectCardBalance(customerId, PROJECT_ID)).isEqualTo(3800L);
+        assertThat(accountBalance(customerId, "merchant_svchain5")).isEqualTo(3800L);
 
         // 同键改金额=指纹不符，拒绝而非错误重放。
-        assertThatThrownBy(() -> storedValueService.consumeInternal(customerId, PROJECT_ID, null,
+        assertThatThrownBy(() -> storedValueService.consumeInternal(customerId, null, "merchant_svchain5",
                 999L, "same request retry", "retrykey_svchain5"))
                 .isInstanceOf(ApiException.class)
                 .hasMessageContaining("已被其他操作使用");
@@ -190,6 +196,7 @@ class BarStoredValueFundChainJdbcIntegrationTests {
     @Test
     void merchantPrepaidCardRefundsAtCounterWhileOnlineRefundStaysRejected() {
         String customerId = seedCustomer("svchain7");
+        asMerchantOperator("merchant_svchain7");
         seedAccount(customerId, null, "merchant_svchain7", 10000L, "merchant_prepaid");
 
         // 线上退款仍无条件拒（AUD-060 口径不变）。
@@ -222,6 +229,7 @@ class BarStoredValueFundChainJdbcIntegrationTests {
     @Test
     void onlineAndCounterRefundKeysLiveInSeparateNamespaces() {
         String customerId = seedCustomer("svchain8");
+        asMerchantOperator("merchant_svchain8");
         seedAccount(customerId, null, "merchant_svchain8", 9000L, "platform_collect");
 
         // 线上退款(platform_collect 卡允许)用键 X。
@@ -239,6 +247,16 @@ class BarStoredValueFundChainJdbcIntegrationTests {
 
     // ---------------- 造数 ----------------
 
+    private void asMerchantOperator(String merchantId) {
+        String userId = "usr_" + merchantId;
+        accessRepository.createUser(new com.complex.platform.access.AccessUser(
+                userId, "merchant_user", "staff", "org_1", merchantId, "active", Instant.now()));
+        accessRepository.saveUserPermissionOverrides(userId,
+                java.util.List.of("customer.stored_value.refund"),
+                com.complex.platform.access.AccessRepository.Effect.GRANT, "本商户柜台退款测试授权", userId);
+        TestSecurityActors.asMerchantUser(merchantId);
+    }
+
     private String seedCustomer(String prefix) {
         String id = prefix + "_customer";
         String phone = "139" + String.format("%08d", Math.abs(prefix.hashCode()) % 100_000_000);
diff --git a/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java b/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java
index 69252fb5..3b1b2c09 100644
--- a/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java
+++ b/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java
@@ -1,8 +1,9 @@
 package com.complex.platform.customer;
 
-// 文件版本：V0.05
+// 文件版本：V0.06
 
 import static org.assertj.core.api.Assertions.assertThat;
+import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
 import com.complex.platform.support.TestSecurityActors;
 import java.util.UUID;
@@ -65,7 +66,7 @@ class StoredValueNotificationIntegrationTest {
         String phone = uniquePhone();
         insertCustomer(customerId, phone);
 
-        CustomerStoredValueTransactionRecord txn = settleTopup(customerId, 5_000L, prefix);
+        CustomerStoredValueTransactionRecord txn = settleTopup(customerId, 5_000L, prefix, null);
 
         assertThat(logCount("topup", phone)).isEqualTo(1);
         assertThat(logValue("topup", phone, "request_id")).isEqualTo("svtopup:" + txn.id());
@@ -91,12 +92,14 @@ class StoredValueNotificationIntegrationTest {
         String customerId = prefix + "_customer";
         String phone = uniquePhone();
         insertCustomer(customerId, phone);
-        settleTopup(customerId, 8_000L, prefix);
+        String merchantId = prefix + "_merchant";
+        settleTopup(customerId, 8_000L, prefix, merchantId);
+        TestSecurityActors.asMerchantUser(merchantId);
 
         CustomerStoredValueTransactionRecord txn = storedValueService.consumeInternal(
                 customerId,
-                PROJECT_ID,
                 null,
+                merchantId,
                 2_300L,
                 "notification test consume",
                 prefix + "_consume");
@@ -107,7 +110,7 @@ class StoredValueNotificationIntegrationTest {
         assertThat(logValue("consume", phone, "status")).isEqualTo("sent");
         assertThat(logParam("consume", phone, "amount_cent")).isEqualTo("2300");
         assertThat(logParam("consume", phone, "balance_after_cent")).isEqualTo("5700");
-        assertThat(logParam("consume", phone, "card_type")).isEqualTo("project");
+        assertThat(logParam("consume", phone, "card_type")).isEqualTo("merchant");
     }
 
     @Test
@@ -118,37 +121,55 @@ class StoredValueNotificationIntegrationTest {
         String phone = uniquePhone();
         insertCustomer(customerId, phone);
 
-        settleTopup(customerId, 3_000L, prefix);
+        settleTopup(customerId, 3_000L, prefix, null);
 
         assertThat(logCount("topup", phone)).isEqualTo(1);
         assertThat(logValue("topup", phone, "status")).isEqualTo("skipped_switch_off");
         assertThat(logValue("topup", phone, "reason")).isEqualTo("switch_off:sms_notification");
     }
 
+    @Test
+    void projectCardInternalConsumeWithoutMerchantScopeRemainsRejected() {
+        String prefix = unique("scope");
+        String customerId = prefix + "_customer";
+        String phone = uniquePhone();
+        insertCustomer(customerId, phone);
+        settleTopup(customerId, 5_000L, prefix, null);
+        assertThatThrownBy(() -> storedValueService.consumeInternal(customerId, PROJECT_ID, null,
+                1_000L, "无商户范围项目卡扣款", prefix + "_consume"))
+                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
+                        e -> assertThat(e.code()).isEqualTo("MERCHANT_SCOPE_REQUIRED"));
+        assertThat(jdbcTemplate.queryForObject(
+                "SELECT balance_cent FROM customer_stored_value_accounts WHERE customer_id = ? AND project_id = ? AND merchant_id IS NULL",
+                Long.class, customerId, PROJECT_ID)).isEqualTo(5_000L);
+        assertThat(logCount("consume", phone)).isZero();
+    }
+
     private CustomerStoredValueTransactionRecord settleTopup(
             String customerId,
             long amountCent,
-            String prefix
+            String prefix,
+            String merchantId
     ) {
         String paymentIntentId = prefix + "_payment_intent";
         insertCreatedPaymentIntent(paymentIntentId, customerId, amountCent);
         // AUD-058：settleTopup 只认充值时绑定的卡范围，夹具需同步落绑定行（等价 topup 落库动作）。
-        insertTopupIntentBinding(paymentIntentId, customerId);
+        insertTopupIntentBinding(paymentIntentId, customerId, merchantId);
         storedValueService.confirmOfflineTopupAndSettle(paymentIntentId,
                 new com.complex.platform.finance.dto.OfflinePaymentConfirmRequest(
                         prefix + "_pos_confirm", null, null, paymentIntentId, amountCent, "pos", java.time.Instant.now()));
         return customerRepository.findStoredValueTransactionByIdempotencyKey("sv_settle_" + paymentIntentId).orElseThrow();
     }
 
-    private void insertTopupIntentBinding(String paymentIntentId, String customerId) {
+    private void insertTopupIntentBinding(String paymentIntentId, String customerId, String merchantId) {
         jdbcTemplate.update("""
                 INSERT INTO stored_value_topup_intents (payment_intent_id, customer_id, project_id, merchant_id, created_at)
-                VALUES (?, ?, ?, NULL, now())
+                VALUES (?, ?, ?, ?, now())
                 ON CONFLICT (payment_intent_id) DO UPDATE SET
                     customer_id = EXCLUDED.customer_id,
                     project_id = EXCLUDED.project_id,
                     merchant_id = EXCLUDED.merchant_id
-                """, paymentIntentId, customerId, PROJECT_ID);
+                """, paymentIntentId, customerId, merchantId == null ? PROJECT_ID : null, merchantId);
     }
 
     private void insertCreatedPaymentIntent(String id, String customerId, long amountCent) {

```
