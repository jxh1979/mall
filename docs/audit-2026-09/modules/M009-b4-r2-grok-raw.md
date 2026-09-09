CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B4-r2 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r2
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M009-b4-r2-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r2 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：任何 `M009-b4-r2-self-raw.md`（当前亦不存在）、席甲本次 B4 raw。定稿队列以 findings.md V1.22 顶部「当前 M009 第二批 B2-r1 双PASS定稿，3项待修（B3-r2）」为准，并与本席第二批 B2-r1（`M009-b5-r1-grok-raw.md` V0.02 追加段）最小修复边界对照。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、3 个定稿「是」ID 是否按最小边界修好、每一行能否归因到这 3 个 ID 之一、是否越界改 HTML/锁/sidecar/ledger 基准/权限种子/迁移/029–035/第一批已修 15 项与 013/017–025/其它模块业务逻辑。未新造判据/角色，未询问操作员。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger 顶行同值。criteria-lock 版本仍 V0.11。`LOCK-AMEND-COUNT: 1`。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M009 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段 |
| 阶段 | B4-r2 只读审计 B3 第2次整批修复的「修复内容」本身 |
| 正式队列「是」（3） | M009-026、M009-027、M009-028 |
| 正式队列「否」（7+已修+第一批否） | 029、030、031、032、033、034、035；第一批已修 15 项（001–012、014–016）与 013/017–025 保持否 |
| B3 | 已执行 2 次（r1 双 PASS 已在 before 树内），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `5bfe07d9f0b7e7ad5fbde58513a4e0d905961e8e` |
| after（对象类型 tree，可解析） | `9350cf873f9075220c76c30a4ec2c90178d54213` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 5bfe07d9f0b7e7ad5fbde58513a4e0d905961e8e 9350cf873f9075220c76c30a4ec2c90178d54213`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 12568 bytes；SHA256 `69229f6ba3bbd0792bf2237ca731cb602f8b2babee94469f3038763d3d2d7a8d`；UTF-8；6 文件；65 插入 / 13 删除；16 hunk；name-status 全为 M |
| 与 `/tmp/m009_b3_r2.diff` | `cmp` 全等（本席自算为准，副本仅对照） |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | 各 7237 条，差集空；无新增/删除路径 |
| HEAD | `242f487233d028173cb9198eecd079f5d877405c` = `origin/main` |
| 工作区 6 文件 blob | 均等于 after；HEAD 6 文件 blob 均不等于 before（before 已含 B3-r1 双 PASS 内容，尚未提交） |
| findings.md | V1.22；工作区 SHA256 `ec210f49fa1cea8345a30ed8c2df3a4bdff4f5774d0489922e31b3986a08395f`；两树 blob 均为 `6059ae11c38b1c2e8ff65bbc8b6eba9b3908875c`（本 diff 未改 findings） |
| ledger.md | 两树 blob 均为 `bfe12d184b3f26cb1d92aa9b048ba88b6febf535`（本 diff 未改）；工作区另有 B3-r2 完成后记事，不在两树 diff 内 |
| 三 HTML 两树 blob | 完全相同，diff 未改 HTML |
| 锁/sidecar 两树 blob | 相同（未改） |
| 迁移 / 权限种子 | 两树 diff 无名 |

`git diff --stat --binary`：

```
 .../platform/governance/JdbcPlatformRepository.java | 15 +++++++++++++--
 .../complex/platform/governance/MerchantRecord.java |  4 ++--
 .../platform/governance/PlatformRepository.java     |  4 ++--
 .../platform/governance/PlatformService.java        | 19 ++++++++++++++-----
 .../governance/PlatformJdbcIntegrationTests.java    | 15 ++++++++++++++-
 .../platform/governance/PlatformServiceTests.java   | 21 ++++++++++++++++++++-
 6 files changed, 65 insertions(+), 13 deletions(-)
```

工作区相对 HEAD 另有既有父链脏文件（B3-r1 修复、审计产物、工具等）。那些路径在 before/after 两树 blob 相同，不在本轮 diff 内，不按本轮修复内容审计。本席未改任何源文件，只新建本 raw。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M009 的集合，本轮只用定稿 3 项已引用的 ID：BSM-004，GEN-002/003/004/006/012/016。不 LOCK-AMEND。

B3-r2 边界（与本席第二批 B2-r1 票及 findings V1.22 顶部一致）：只修 026/027/028 及归因到各 ID 的最小行为测试（TDD：先落可失败测试再修）；不改三份 HTML、判据、权限种子、迁移、其它 ID 内容、其它模块业务逻辑；改动文件自身版本递增（V0.01→V0.02）。M011 两测试文件的新增最小测试归因各 ID。034 保持否、不并 026。028 不并 020。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary 5bfe07d9f0b7e7ad5fbde58513a4e0d905961e8e 9350cf873f9075220c76c30a4ec2c90178d54213`

before tree = `5bfe07d9f0b7e7ad5fbde58513a4e0d905961e8e`
after tree = `9350cf873f9075220c76c30a4ec2c90178d54213`

字节 12568；SHA256 `69229f6ba3bbd0792bf2237ca731cb602f8b2babee94469f3038763d3d2d7a8d`；UTF-8 完整原文如下。

````diff
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
````

---

## 4. 逐 ID 修复内容（修好 / 最懒 / 无新缺陷或业务发明）

分类顺序冲突→细化→技术性→添加。本批 026 技术性、027 技术性、028 细化。无冲突、无添加。

### M009-026（技术性，注释，010 同根因残留）

定稿处置：三处残留「硬删」注释改软删除口径；编译级、无测试面。位置：JdbcPlatformRepository countMerchantReferences 行、MerchantRecord 字段注释、PlatformRepository countMerchantReferences javadoc。

after 实文：

- Jdbc：`>0 拒绝删除（软删除标记同样不允许，防连锁孤儿/资金孤儿）`。占用闸拦的就是这条软删标记路径，与 `deleteMerchant` 的 `UPDATE delete_status='approved'` 一致。
- MerchantRecord：`删除=审批后软删除标记(delete_status=approved,行保留可查可恢复,BSM-004/M009-026)`。
- PlatformRepository：`>0 则拒绝删除（含软删除标记，M009-026；防连锁孤儿）`。

before 三处均含「硬删」；after 这三处「硬删」字面量为 0。行为未改。无测试面，符合定稿。最懒：只改三行措辞 + 三文件版本，不扩 034 无引用 import，不改 M011 fake「拒硬删」注释（B2 明文「本 ID 不强制」）。

无业务发明：只把已作废的硬删口径改成与实现和 BSM-004「删除只隐藏不抹除」一致的当前事实（GEN-016）。

判定：修好。PASS。

### M009-027（技术性，Jdbc 覆写 findSpaceById 按主键）

定稿处置：Jdbc 覆写 `findSpaceById` 为 `WHERE id = :id`（同 `findMerchantById`）；不撤 `listSpaces` LIMIT 500；M011 JDBC 测试插入 501 条后断言最早一条可按 id 查到。

before：`JdbcPlatformRepository` 全文 `findSpaceById` 字面量 = 0；接口默认方法基于 `listSpaces()` 过滤；Jdbc `listSpaces` 为 `ORDER BY created_at DESC, id LIMIT 500`。after：新增覆写，SELECT 列与 `listSpaces`/`mapSpace` 同构，谓词仅 `id = :id`。默认方法保留给内存 fake（无 LIMIT）。`listSpaces` LIMIT 500 未动。

测试 `findSpaceByIdIsNotTruncatedByListLimit`：501 条递增 `created_at`，断言 `listSpaces().size()==500` 且 `findSpaceById("sp_limit_0")` 有值。类上 `@Transactional`，不污染库。一条覆写同时修 `createSpace` 上级空间闸与 `spaceExists`（Leasing 意向空间），不另改消费方。

最懒：不撤列表 LIMIT、不加 FK/迁移、不改接口默认。无业务发明（存在性按主键，GEN-006/002）。

判定：修好。PASS。

### M009-028（细化，requireActiveMerchant 仅三处创建）

定稿处置：`requireActiveMerchant`（存在且 `archived_at==null` 且 `delete_status≠approved`，否则 400 中文、内部码 `MERCHANT_ARCHIVED`）用于 `createProject` / `createSpace` / `createBusinessEntityBinding`；其余路径仍 `requireMerchant`；ServiceTests 覆盖三处创建。

after：私有方法谓词与定稿逐字相符。三处创建由 `requireMerchant` 改为 `requireActiveMerchant`。`archiveMerchant` / `unarchiveMerchant` / `requestDeleteMerchant` / `approveDeleteMerchant` / `updateMerchant` 仍 `requireMerchant`（须命中已存档行）。无 `updateProject`/`updateSpace`。错误文案中文；`MERCHANT_ARCHIVED` 属标识符例外。

测试 `archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding`：先存档，再对三个创建入口各一条 `assertThatThrownBy` 期望 `MERCHANT_ARCHIVED`，并断言项目/绑定未写入。合并为一个测试方法比「各一条独立 @Test」更懒；三处断言任一未抛都会红（红阶段停在 createProject 未抛）。BSM-004「要删除，得先存档」，已批删行通常已带 `archivedAt`；`deleteStatus==approved` 为同一 if 的皮带。不并 020、不改列表 API。

无业务发明：落到本模块写路径的「不再被新业务选用」（BSM-004 存档条），不把「下单、配价、组套餐」扩成新禁则。注释偶用邻近小标题「下线就不能再用」作口语句，谓词仍是定稿两条件——见第 9 节观察，不构成新规则。

判定：修好。PASS。

### 029–035 与其它 ID

diff 无名触及 029 状态闭集、030 建档三列、031 停用清理、032 PUT 空 status、033 双写、034 无引用 import、035 主体 status×绑定。第一批 001–016 行为行未再改（仅随文件版本铁律 + 028 在既有 M009-006 注释尾追加「且商户未下线」）。013/017–025 无名。

---

## 5. TDD 红绿（本席独立，不只信席甲日志）

`/tmp/m009_b3r2_red.log`、`/tmp/m009_b3r2_green.log` 只作对照。本席不得在主仓回滚源码重放红阶段（B4 禁止写源），故红用 before 树源码对照新测试断言（类型 4），绿用当前 after 工作区实跑。

**红（类型 4，before 树 vs 新测试）：**

- 026 无测试面，编译级注释，不要求红。
- 027：before Jdbc `findSpaceById` 覆写次数 = 0，走接口默认 `listSpaces().stream()…`；Jdbc `listSpaces` LIMIT 500 且 `ORDER BY created_at DESC`。新测试插入 `sp_limit_0`…`sp_limit_500`（时间递增）后查最早一条，必然 Optional 空。席甲红日志对照：`findSpaceByIdIsNotTruncatedByListLimit:370 Expecting Optional to contain a value but it was empty`，与此断言集合一致。
- 028：before 三处创建均 `requireMerchant`（`findMerchantById` 不过滤 `archived_at`/`delete_status`）。新测试先 `archiveMerchant` 再 `createProject`，必然不抛。席甲红日志对照：`archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding:246 Expecting code to raise a throwable`，与此一致。

**绿（本席 2026-09-09T08:58:49Z 实跑；只 export `COMPLEX_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/complex_test` 及用户名口令三变量，未 source 整份 env；库名 `complex_test`）：**

```
PlatformJdbcIntegrationTests 13 条中 12 PASS；唯一 FAIL = residualMigration… expected 399 but was 409（M009-017 环境，非本 diff）
  其中 findSpaceByIdIsNotTruncatedByListLimit = PASS（本席 surefire 该文件唯一失败项为 residualMigration）
PlatformServiceTests 30/30（含 archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding）
VenueBusinessHoursServiceTests 6/6
PlatformControllerTests 10/10
LeasingServiceTests 11/11
LeasingJdbcIntegrationTests 4 条中 3 PASS；唯一 FAIL = migrationCreates… 399≠409（同 017）
LeasingSignAtomicityJdbcIntegrationTests 2/2
```

合计本席套件 76 后端测试：74 PASS + 2 环境红（017）。Flyway 日志：`Current version of schema "public": 409`，`newer than the latest available migration (399)`。与 green 日志形态相同。

026 无单测：以 after 三处「硬删」字面量为 0 且 `deleteMerchant` 仍为 UPDATE 软标记为证。不因此判内容 FAIL。

---

## 6. §5 闸 3 逐行归因（16 hunk）

判定：每一行都能归因到队列「是」的 3 个 ID 之一。验证性测试归因到它所证明的 ID；`// 文件版本：V0.01`→`V0.02` 按操作员版本铁律随该文件所属 ID。混合 hunk 按下表拆到行。归因不到的行 = 0。

文件版本行（6 处，V0.01→V0.02）所属：

| 文件 | 版本行归因 |
|---|---|
| JdbcPlatformRepository.java | 026+027（该文件本轮承载这两 ID） |
| MerchantRecord.java | 026 |
| PlatformRepository.java | 026 |
| PlatformService.java | 028 |
| PlatformJdbcIntegrationTests.java | 027（M011 最小测试） |
| PlatformServiceTests.java | 028（M011 最小测试） |

| hunk | 文件 | 内容 | ID |
|---|---|---|---|
| 1 | JdbcPlatformRepository.java | 版本 V0.01→V0.02 | 026+027 |
| 2 | JdbcPlatformRepository.java | 新增 `findSpaceById` 按主键覆写（11 行） | 027 |
| 3 | JdbcPlatformRepository.java | countMerchantReferences 注释去「硬删」 | 026 |
| 4 | MerchantRecord.java | 版本 V0.01→V0.02 | 026 |
| 5 | MerchantRecord.java | 字段注释「硬删走审批」→软删除标记 | 026 |
| 6 | PlatformRepository.java | 版本 V0.01→V0.02 | 026 |
| 7 | PlatformRepository.java | countMerchantReferences javadoc 去「硬删」 | 026 |
| 8 | PlatformService.java | 版本 V0.01→V0.02 | 028 |
| 9 | PlatformService.java | 新增 `requireActiveMerchant` | 028 |
| 10 | PlatformService.java | `createProject` 改调用 | 028 |
| 11 | PlatformService.java | `createSpace` 改调用 | 028 |
| 12 | PlatformService.java | 绑定创建注释追加 + 改调用 | 028 |
| 13 | PlatformJdbcIntegrationTests.java | 版本 V0.01→V0.02 | 027 |
| 14 | PlatformJdbcIntegrationTests.java | `findSpaceByIdIsNotTruncatedByListLimit` | 027 |
| 15 | PlatformServiceTests.java | 版本 V0.01→V0.02 | 028 |
| 16 | PlatformServiceTests.java | `archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding` | 028 |

hunk 12 注释仍保留 M009-006 前半（存在性，第一批已修），本轮只追加「M009-028：且商户未下线」并改调用，整行归 028。无「顺手优化」「换测试框架」「补抽象」行。无归因到 029–035、013/017–025 或第一批行为修复的行。

---

## 7. 越界检查

| 禁止项 | 本轮两树 diff |
|---|---|
| 三份业务 HTML | blob 不变（BSM `12334b13…` / PGM `c2588ca1…` / BEQR `86a71793…`） |
| 锁 / sidecar / ledger 基准 | 锁/sidecar blob 不变；ledger 两树 blob 相同；工作区 ledger 另有 B3 完成后记事，不在 diff 内 |
| 权限种子 | 无名 |
| 迁移文件 | 无名；无 V410 |
| 029–035 对应内容 | 无（034 无引用 import 未清，符合队列否） |
| 第一批 001–016 行为 / 013/017–025 | 无行为再改 |
| 其它模块业务逻辑 | 无。M011 两测试文件仅各 ID 最小测试；未改 LeasingService 业务、未改页面、未改 ContractService |

after 六文件中仍见 M011 `PlatformServiceTests.java:694` fake 注释「拒硬删」——该行不在本 diff，B2 明文不强制 026 扩扫，不判越界。`VenueBusinessHoursRecord.java:16`「不做物理删除」不在本 diff、非定稿三处。

---

## 8. 复跑命令（本席）

后端（只 export 三个 DB 变量，库名 `complex_test`）：

```
mvn -Dtest=PlatformServiceTests,PlatformControllerTests,PlatformJdbcIntegrationTests,VenueBusinessHoursServiceTests,LeasingServiceTests,LeasingSignAtomicityJdbcIntegrationTests,LeasingJdbcIntegrationTests test
```

未跑前端：本 diff 无页面文件。未 commit / push / PR / merge / 部署。未在主仓写源。未跑写模式 SQL。

---

## 9. 问题二分（回退统一规则）

有 ID 且仍在修复队列、本轮未修好：无。
无 ID 或不在修复队列的新缺陷：无。

残留观察（不构成 FAIL、不新开 ID）：

- 026 不强制的 M011 fake 注释 `PlatformServiceTests.java:694` 仍写「拒硬删」（B2 归 010→M011 扫尾）。
- 028 方法/测试注释偶用 BSM-004 邻近小标题「下线就不能再用」；定稿 HTML 依据是「存档（先下线）…不再被新业务选用」。谓词与三处调用面仍按定稿，不发明新禁则。
- 028 测试以存档路径覆盖三处创建，未单列 `delete_status=approved` 孤立用例；删除须先存档，不扩测。
- 017 环境红 399≠409 仍在，非本 diff。

---

## 10. 技能与缺口

已按收窄口径使用：`verification-before-completion`（绿必须本席实跑）。B4 按提示词：不派第三席、不 `HEAD~1`、不修。`grilling` 本轮按自问自答取证，未打开该 SKILL.md 全文——记缺口，不冒称已核实调用。未派 Arc audit 子代理。ponytail 只作「最懒可行」尺度，不落地代码。无 MCP。

---

## 11. 结论

修复内容：PASS（026/027/028 均按定稿最小边界落地，TDD 红有 before 树对照、绿有本席复跑；017 环境红不是本轮引入）。
范围归因：PASS（6 文件 16 hunk 65+/13- 逐行可归到 026/027/028；无越界）。
总结论：PASS。

此票不是模块 PASS，也不是 B4 双 PASS。须席甲独立 B4 票且双 PASS 后才进 B5。本席未改 findings 队列归属。
