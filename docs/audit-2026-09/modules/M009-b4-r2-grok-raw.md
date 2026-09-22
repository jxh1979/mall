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

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B4-r2 修复内容独立审计（Grok 席乙）

文件版本：V0.02
步骤：B4-r2（第四批 B3 第 2 次整批修复的修复内容复审）
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁，不提交推送部署）
本文件路径：docs/audit-2026-09/modules/M009-b4-r2-grok-raw.md
本文件自身版本：V0.02（路径原先存在 V0.01：历史第二批 B4-r2，正式队列当时为 026/027/028。本次不覆盖该前缀，只在文末追加第四批完整票）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。上方 30914 字节 V0.01 前缀原样保留，不得当作本票。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮第四批 B4-r2 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲票仍是作者自审票；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：任何 `M009-b4-r2-self-raw.md` 正文（目录可见同名历史/并行文件，本席不打开）；未读席甲本次未封存稿。定稿队列以 findings.md V2.10 顶部「当前M009第四批B2定稿（唯一当前表；双PASS）」为准：23 行 17 是 / 6 否。本轮只审 054/055/056 的最小落地；原 14 项已在第 1 轮落实，本 diff 未重复修改。

授权时序（如实，不倒填）：操作员先指出 054–056 未经批准实施，Codex 承认并暂停，没有回滚/提交/推送；随后明确答复「如果不是修改html，那么修复没有问题」，现获批准继续这三项代码与测试范围。此前「无待答复项」不代表当时已批准。三份业务 HTML 本批逐字未改且不得改。本席按已批准范围审实现质量，批准范围 ≠ 自动内容 PASS。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、3 个定稿「是」ID（054/055/056）是否按 V2.10 最小边界修好、每一行（含文件版本号）能否归因到这 3 个当前正式 YES ID 之一、是否越界改 HTML/锁/sidecar/ledger 基准/权限种子/迁移/原 14 是的已落实行为/6 否/其它模块生产业务逻辑。B4 只审完整 diff 及直接依赖，不全仓扫；B5 整模块另做。未新造判据/角色，未询问操作员。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger 顶行同值。criteria-lock 版本仍 V0.11。`LOCK-AMEND-COUNT: 1`。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）；sidecar blob 均为 `41f575026b693af74d2a36e6faf86d260ef6f6ca`。

| 项 | 值 |
|---|---|
| 模块 | M009 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段 |
| 阶段 | B4-r2 只读审计 B3 第2次整批修复的「修复内容」本身（第四批；B3=2/3） |
| 正式队列「是」（17） | 038、039、040、041、031、042、043、044、045、046、030、047、018、013、**054、055、056** |
| 本轮应对齐的 YES | 仅 054、055、056（原 14 项已在 r1 落实，本 diff 无名） |
| 正式队列「否」（6） | 048、049、050、051、052、053 |
| before（对象类型 tree，可解析） | `e60778ec7fd700bcfa671887ffa62ba85897d21b` |
| after（对象类型 tree，可解析） | `e4c10154a405e61e1bfad9fedb6fde11068f653a` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary e60778ec7fd700bcfa671887ffa62ba85897d21b e4c10154a405e61e1bfad9fedb6fde11068f653a`（无路径过滤） |
| 实际差异 | 20857 bytes；SHA256 `269c28c32ab927579fb8d5a40f0fe16bc29b9c623546c085c1792f87fa1bbc51`；UTF-8；5 路径；89 插入 / 25 删除；22 hunk；name-status 全为 M |
| 与派发期望 | 字节/SHA256/5 路径/22 块均独算确认一致 |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | 各 3761 条，差集空；无新增/删除路径 |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`（未提交本批修复） |
| 工作区 5 文件 blob | 均等于 after |
| findings.md | 工作区 V2.10；两树 blob 均为 `efa9a52e16ed7aa5bf13d00ac326e722c990debf`（本 diff 未改 findings） |
| ledger.md | 两树 blob 均为 `5365ce88e41c581b5e774be7ae4395f3afb51a39`（本 diff 未改） |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3`，完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |
| 迁移 / 权限种子 / Controller / StoredValueService / StaffService / SupervisorReviewService / AuthorizationService | 两树 diff 无名 |

`git diff --stat --binary`：

```
 .../com/complex/platform/access/AccessService.java |  7 +++-
 .../access/M006SecurityRegressionTests.java        | 26 +++++++++++--
 .../platform/access/staff/M010RegressionTests.java |  8 ++--
 ...arStoredValueFundChainJdbcIntegrationTests.java | 30 ++++++++++++---
 .../StoredValueNotificationIntegrationTest.java    | 43 ++++++++++++++++------
 5 files changed, 89 insertions(+), 25 deletions(-)
```

`git diff --numstat`：AccessService 4/1；M006SecurityRegressionTests 22/4；M010RegressionTests 4/4；BarStoredValueFundChainJdbcIntegrationTests 26/4；StoredValueNotificationIntegrationTest 33/12。

三份 HTML 工作区独立 SHA256（与锁引用对照，非 diff 内容）：BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`；BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`；PGM `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1`。

本席未改任何源文件，只向本 raw 追加本次完整报告。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M009 的集合。本轮只用定稿 054/055/056 已引用的 ID：BSM-004、GEN-003/005/008、PGM-041。不 LOCK-AMEND。

V2.10 本轮最小边界（摘录，不扩写）：

- **M009-054**（冲突 / 工程缺陷是 / 修复队列是）：仅 `AccessService.changePositionTemplateLifecycle` 对真实 `isMasterAuthority` 身份兼容该方法已有动作；不改共享 `requirePlatformAdmin` 或其它入口。保留 Controller 功能权限、对象状态、占用检查、独立审批和留痕；补真实最高权限及普通管理员边界验证。
- **M009-055**（细化 / 是 / 是）：只调整 `BarStoredValueFundChainJdbcIntegrationTests`、`StoredValueNotificationIntegrationTest`、`M010RegressionTests` 对应失败用例的合法操作人/商户资料/现有调用前提；保留核心金额、余额、退款、幂等、通知、隔离与隐私断言。通知与通用幂等用既有合法商户卡；原无商户范围项目卡 `consumeInternal` 仍明确拒绝，不能宣称项目卡消费已补齐；项目卡充值通知保留。不得放宽资金/员工生产代码权限或伪装最高权限。
- **M009-056**（细化 / 是 / 是）：只在 M006 该权限审批身份用例补测试授权的真实受益人及既有 schedule 数据；仍证明普通最高职级管理员不能代替 USB、真实 USB 合法审批。保留生产受益人及利益冲突检查，不新增豁免。

操作员批准的是上述代码与测试范围，且「不是修改 html」。本席核：三 HTML 未改。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary e60778ec7fd700bcfa671887ffa62ba85897d21b e4c10154a405e61e1bfad9fedb6fde11068f653a`

before tree = `e60778ec7fd700bcfa671887ffa62ba85897d21b`
after tree = `e4c10154a405e61e1bfad9fedb6fde11068f653a`

字节 20857；SHA256 `269c28c32ab927579fb8d5a40f0fe16bc29b9c623546c085c1792f87fa1bbc51`；UTF-8 完整原文如下。

````diff
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
````

---

## 4. 22 块逐行归因（含版本号）

当前正式 YES 仅 054/055/056 可承接本 diff。原 14 是不在本 diff，6 否不在本 diff。上下文行（未改）不需要 ID；下列每条 `+`/`-` 均绑定。

### H01 · AccessService.java · `@@ -1,6 +1,6 @@`
| 行 | 归属 |
|---|---|
| `-// 文件版本：V0.19` | M009-054（本文件唯一生产改动的版本递增） |
| `+// 文件版本：V0.20` | M009-054 |

### H02 · AccessService.java · `@@ -485,7 +485,10 @@`（after L485–494）
| 行 | 归属 |
|---|---|
| `-        requirePlatformAdmin(authorizationService.currentActor());` | M009-054 |
| `+        AccessActor actor = authorizationService.currentActor();` | M009-054 |
| `+        if (!authorizationService.isMasterAuthority(actor)) {` | M009-054 |
| `+            requirePlatformAdmin(actor);` | M009-054 |
| `+        }` | M009-054 |

同方法其余分支（archive/restore/request_delete/approve_delete/reject_delete、占用、独立审批、审计）在本 hunk 上下文中未改。共享方法 `requirePlatformAdmin`（after L2184–2189）两树全文相同。另一入口 `savePositionRoleTemplate` 仍直接 `requirePlatformAdmin(actor)`，本 diff 未改。

### H03 · M006SecurityRegressionTests.java · `@@ -1,6 +1,6 @@`
| 行 | 归属 |
|---|---|
| `-// 文件版本：V0.28` | M009-054 + M009-056（本文件同时承载两 ID 的最小验证，版本递增一次） |
| `+// 文件版本：V0.29` | M009-054 + M009-056 |

### H04 · M006SecurityRegressionTests.java · `@@ -389,6 +389,10 @@`（`topAuthorityReviewRequiresMasterSessionRatherThanRankAlone`）
| 行 | 归属 |
|---|---|
| `+        users.put("permission-target", user("permission-target", "org", null));` | M009-056 |
| `+        when(reviewRepository.findSupervisorReviewSchedule("top")).thenReturn(Optional.of(` | M009-056 |
| `+                new ...ReviewSchedule(` | M009-056 |
| `+                        "top", "permission-target", Instant.now().plusSeconds(3600), 0, null, null)));` | M009-056 |

受益人 `permission-target` ≠ 审批人 `admin` ≠ 发起人 `requester` ≠ USB `MASTER_USER_ID`，未把审批人设成受益人以绕过自审闸。

### H05 · M006SecurityRegressionTests.java · `@@ -405,7 +409,8 @@`
| 行 | 归属 |
|---|---|
| `-        assertThatThrownBy(() -> reviews.approve("top", request)).isInstanceOf(ApiException.class);` | M009-056 |
| `+        assertThatThrownBy(() -> reviews.approve("top", request)).isInstanceOfSatisfying(ApiException.class,` | M009-056 |
| `+                e -> assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_REQUIRED"));` | M009-056 |

仍先证普通最高职级被拒（明确 USB 码，而不是受益人缺失码），随后上下文未改的 USB `approve` 成功断言仍在。`actionType` 仍为 `permission_grant_l3`，未偷换。

### H06 · M006SecurityRegressionTests.java · `@@ -577,6 +582,11 @@`（`positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval`）
| 行 | 归属 |
|---|---|
| `+        actor("ordinary-staff", "staff", "org", null);` | M009-054 |
| `+        assertThatThrownBy(() -> service.changePositionTemplateLifecycle("lifecycle",` | M009-054 |
| `+                new StatusChangeRequest("staff-archive", "archive", "越权操作", List.of())))` | M009-054 |
| `+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PLATFORM_ADMIN_REQUIRED"));` | M009-054 |
| `+        actor("admin", "platform_admin", "org", null);` | M009-054 |

证明非 master 仍走 `requirePlatformAdmin`；随后原 platform_admin 存档路径继续。

### H07 · M006SecurityRegressionTests.java · `@@ -585,13 +595,23 @@`
| 行 | 归属 |
|---|---|
| `-                ... approve_delete ... )).isInstanceOf(ApiException.class);` | M009-054 |
| `+                ... approve_delete ... ))` | M009-054 |
| `+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_REQUIRED"));` | M009-054 |
| `+        var masterAuthentication = SecurityContextHolder.getContext().getAuthentication();` | M009-054 |
| `+        service.changePositionTemplateLifecycle(..., "reject_delete", ...);` | M009-054 |
| `+        assertThat(mapping.get().deletedAt()).isNull();` | M009-054 |
| `+        assertThat(mapping.get().deleteReviewId()).isNull();` | M009-054 |
| `+        actor("admin", "platform_admin", "org", null);` | M009-054 |
| `+        service.changePositionTemplateLifecycle(..., "request_delete", "重新申请", ...);` | M009-054 |
| `+        SecurityContextHolder.getContext().setAuthentication(masterAuthentication);` | M009-054 |
| `+        actor("admin", ...) / setAuthentication(masterAuthentication)` 围绕 list/restore | M009-054 |

覆盖：普通管理员不可最高审批；真实 USB 驳回/再申请后批准/恢复。占用拒绝（`countPositionTemplateUsage=1`）与独立审批仍在未改上下文。

### H08 · M010RegressionTests.java · `@@ -1,6 +1,6 @@`
| 行 | 归属 |
|---|---|
| `-// 文件版本：V0.26` | M009-055 |
| `+// 文件版本：V0.27` | M009-055 |

### H09 · M010RegressionTests.java · `@@ -473,13 +473,15 @@`（`OwnAndManagementProfileResponsesDoNotReturnPersonalValues`）
| 行 | 归属 |
|---|---|
| `-        var profile = profile(null);` | M009-055 |
| `+        access.updateUserScope(manager, "m010_p", registrationMerchant, manager, Instant.now());` | M009-055 |
| `+        actor(manager, "platform_admin", registrationMerchant);` | M009-055 |
| `+        var profile = profile(registrationMerchant);` | M009-055 |
| `-        actor(profile.userId(), "staff", null);` | M009-055 |
| `+        actor(profile.userId(), "staff", registrationMerchant);` | M009-055 |

未改生产员工服务。管理端仍用真实 `platform_admin`（非伪装 master）；本人读取仍用 `profile.userId()`。脱敏断言未改：`contains("138****8000").doesNotContain("13800138000", "110101199001010000")`。

### H10 · BarStoredValueFundChainJdbcIntegrationTests.java · `@@ -1,6 +1,6 @@`
| 行 | 归属 |
|---|---|
| `-// 文件版本：V0.05` | M009-055 |
| `+// 文件版本：V0.06` | M009-055 |

### H11 · 同文件 · `@@ -58,6 +58,9 @@`
| 行 | 归属 |
|---|---|
| `+    @Autowired` | M009-055 |
| `+    private com.complex.platform.access.AccessRepository accessRepository;` | M009-055 |
| `+` | M009-055（空行随夹具注入） |

仅为合法商户操作人落库，不改生产资金代码。

### H12 · 同文件 · `@@ -76,6 +79,7 @@`（settleBill 储值渠道）
| 行 | 归属 |
|---|---|
| `+        asMerchantOperator("merchant_svchain1");` | M009-055 |

金额/渠道/余额断言未改（5000 支付、channel=`stored_value`、余额 3000）。

### H13 · 同文件 · `@@ -109,6 +113,7 @@`（外键碰撞拒绝）
| 行 | 归属 |
|---|---|
| `+        asMerchantOperator("merchant_svchain3");` | M009-055 |

「已被其他操作使用」断言未改。

### H14 · 同文件 · `@@ -142,18 +147,19 @@`（通用幂等）
| 行 | 归属 |
|---|---|
| `-        seedAccount(..., PROJECT_ID, null, ...)` / project `consumeInternal` / `projectCardBalance` | M009-055 |
| `+        asMerchantOperator("merchant_svchain5");` | M009-055 |
| `+        seedAccount(..., null, "merchant_svchain5", ...)` | M009-055 |
| `+        consumeInternal(customerId, null, "merchant_svchain5", ...)`（两次同指纹 + 一次改金额） | M009-055 |
| `+        assertThat(accountBalance(..., "merchant_svchain5")).isEqualTo(3800L);` | M009-055 |

按边界：通用幂等改走既有合法商户卡；同键同指纹重放、改金额拒绝的金额/余额断言保留。不宣称项目卡消费已实现。

### H15 · 同文件 · `@@ -190,6 +196,7 @@`（柜台退款）
| 行 | 归属 |
|---|---|
| `+        asMerchantOperator("merchant_svchain7");` | M009-055 |

线上退拒绝、凭证必填、柜台退余额 0、closed、同键重放断言均未改。

### H16 · 同文件 · `@@ -222,6 +229,7 @@`（线上/柜台键命名空间）
| 行 | 归属 |
|---|---|
| `+        asMerchantOperator("merchant_svchain8");` | M009-055 |

3000/6000/3000 余额与不同流水 ID 断言未改。

### H17 · 同文件 · `@@ -239,6 +247,16 @@`
| 行 | 归属 |
|---|---|
| `+    private void asMerchantOperator(String merchantId) { ... }` 共 10 行 | M009-055 |

`TestSecurityActors.asMerchantUser(merchantId)` 与 `usr_`+merchantId 建档一致；只 GRANT `customer.stored_value.refund`（柜台退款需要）。`consumeInternal` 生产注释写明不重复过 consume 权限闸。未授予资金生产豁免，未伪装 master。

### H18 · StoredValueNotificationIntegrationTest.java · `@@ -1,8 +1,9 @@`
| 行 | 归属 |
|---|---|
| `-// 文件版本：V0.05` | M009-055 |
| `+// 文件版本：V0.06` | M009-055 |
| `+import static org.assertj.core.api.Assertions.assertThatThrownBy;` | M009-055（新拒绝用例） |

### H19 · 同文件 · `@@ -65,7 +66,7 @@`（项目卡充值通知）
| 行 | 归属 |
|---|---|
| `-        settleTopup(customerId, 5_000L, prefix);` | M009-055 |
| `+        settleTopup(customerId, 5_000L, prefix, null);` | M009-055 |

`merchantId=null` 仍走项目卡绑定；`card_type=project`、金额 5000 等断言未改。项目卡充值通知保留。

### H20 · 同文件 · `@@ -91,12 +92,14 @@`（消费通知）
| 行 | 归属 |
|---|---|
| `-        settleTopup(...); consumeInternal(..., PROJECT_ID, null, ...)` | M009-055 |
| `+        String merchantId = prefix + "_merchant";` | M009-055 |
| `+        settleTopup(..., merchantId);` | M009-055 |
| `+        TestSecurityActors.asMerchantUser(merchantId);` | M009-055 |
| `+        consumeInternal(customerId, null, merchantId, 2300L, ...)` | M009-055 |

通知/金额 2300/余额 5700 断言保留，只把卡范围改到合法商户卡。

### H21 · 同文件 · `@@ -107,7 +110,7 @@`
| 行 | 归属 |
|---|---|
| `-        assertThat(logParam(..., "card_type")).isEqualTo("project");` | M009-055 |
| `+        assertThat(logParam(..., "card_type")).isEqualTo("merchant");` | M009-055 |

与合法商户卡路径一致，不是把项目卡消费伪装成成功。

### H22 · 同文件 · `@@ -118,37 +121,55 @@`
| 行 | 归属 |
|---|---|
| 开关关闭充值通知的 `settleTopup(..., null)` 改签名 | M009-055 |
| `+    void projectCardInternalConsumeWithoutMerchantScopeRemainsRejected()` 整方法 | M009-055 |
| `+        consumeInternal(customerId, PROJECT_ID, null, 1000L, ...)` 期望 `MERCHANT_SCOPE_REQUIRED` | M009-055 |
| `+        余额仍 5000；consume 通知日志为 0` | M009-055 |
| `settleTopup`/`insertTopupIntentBinding` 增加 `merchantId`；null→项目卡 PROJECT_ID，非空→merchant 卡 project_id null | M009-055 |

原无商户范围项目卡内部扣款仍明确拒绝；不能宣称项目卡消费已补齐。生产 `StoredValueService` 不在 diff 内。

**归因结论：** 22 块全部 `+`/`-` 行均可归到 V2.10 当前正式 YES 的 M009-054 / 055 / 056。无无 ID 行，无把 048–053 否项或原 14 是拿来扩修。范围归因：**PASS**。

---

## 5. 修复内容审查（第一性原理 / 顺从 / 对抗；grilling 收窄自问自答）

### 5.1 M009-054 — 岗位模板生命周期入口兼容真实 USB

**事实（before）：** `changePositionTemplateLifecycle` 入口无条件 `requirePlatformAdmin(currentActor())`。生产 `isMasterAuthority` 要求 `SessionIssuer` 三件套（`usr_master_authority` / `master-authority` / `master_authority`），真实 USB 不是 `platform_admin`，故在已有 `approve_delete`/`reject_delete` 的 `MASTER_AUTHORITY_REQUIRED` 分支之前被挡。BSM-004 原文：删除须由具备最高权限的人审批；「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」。GEN-005/GEN-008：受保护操作须按主体授权，接口与生产身份一致。

**落地：** 仅该入口：非 master 仍 `requirePlatformAdmin`；master 进入后走原 switch。graft `callers changePositionTemplateLifecycle --depth 2`：生产调用仅 `AccessController` L192–202。Controller 本 diff 0 字节：`access.role.assign`；approve/reject 另加 `governance.supervisor_review.approve`。占用 `requirePositionTemplateUnused`、状态机、`SupervisorReviewService.approve`、`auditEventService.recordSuccess` 未改。共享 `requirePlatformAdmin` 方法体两树逐字相同。`savePositionRoleTemplate` 仍只认 platform_admin。

**对抗：**
1. 是否把 USB 做成全局 platform_admin？否，仅此方法。
2. 普通 staff 能否进？测试期望 `PLATFORM_ADMIN_REQUIRED`，本席复跑通过。
3. 普通 platform_admin 能否最高审批？`MASTER_AUTHORITY_REQUIRED`，通过。
4. 是否删独立审批/占用？否。
5. Controller 功能权限是否被服务层短路？HTTP 仍先 `requirePermission`；本 diff 未改 Controller。服务层单测直接调 service，054 要求的正是服务入口兼容。

内容：**PASS**。

### 5.2 M009-055 — 合法商户夹具，不放宽生产守卫

**事实：** `currentMerchantScope()` 在无 `merchantId` 时抛 `MERCHANT_SCOPE_REQUIRED`（含 master）。资金/员工回归在 before 树即被该守卫挡住，不是本批引入（V2.10 已纠正「本批引入」说法）。GEN-003/005/008、PGM-041：测试须执行目标行为；商户隔离不关开关。

**落地：** 只改三个指定测试类。生产 `StoredValueService`/`StaffService`/`AuthorizationService` 不在 diff。通知消费与通用幂等改为商户卡；项目卡充值通知 `settleTopup(..., null)` + `card_type=project` 保留；新增 `projectCardInternalConsumeWithoutMerchantScopeRemainsRejected` 锁住项目卡 `consumeInternal` 拒绝且余额/通知不变。M010 管理端 `platform_admin`+商户范围，本人仍真实 staff，脱敏断言保留。

**对抗：**
1. 是否为绿删除隔离或恢复假 master？否。Bar `@BeforeEach` 仍是真实 master 三件套；失败路径改为 `asMerchantOperator`。
2. 是否伪造项目卡扣款入口？否，拒绝用例锁 `MERCHANT_SCOPE_REQUIRED`。
3. `asMerchantOperator` 只授 refund，结账会不会因缺 consume 权限假绿？`consumeInternal` 注释写明不重复过 consume 闸；`settleBill` 无 `@GuardedAction`，只 `checkMerchantOwnership`。本席实跑 8/8 通过。
4. 是否扩大到未点名的旧失败类？diff 无名其它测试生产文件。

内容：**PASS**。不得把本 ID 写成「项目卡消费已实现」。

### 5.3 M009-056 — 受益人夹具，不豁免生产检查

**事实：** `permission_grant_l3` 命中 `requiresBeneficiary`（`grant`）。`approve` 在 USB 节点闸之前检查 schedule 受益人；缺则 `BENEFICIARY_REQUIRED`，到不了「普通最高职级不能代替 USB」。

**落地：** 只补 `permission-target` 用户 + `findSupervisorReviewSchedule`。生产 `SupervisorReviewService` 不在 diff。`requiresBeneficiary` / 受益人自审 / 发起人自审仍执行。普通 admin（最高职级 M10 + `governance.supervisor_review.approve`）现在得到 `MASTER_AUTHORITY_REQUIRED`；随后 USB 三件套批准同一事项。未改 `actionType`。

**对抗：**
1. 是否把受益人设成审批人以免自审？否。
2. 是否测试里绕过 `requiresBeneficiary`？否，schedule 有受益人，生产函数仍跑。
3. 是否新豁免？生产文件未改。

内容：**PASS**。

### 5.4 越界与旧问题

- 三 HTML / lock / sidecar / 迁移 / 权限种子 / 原 14 是生产行为 / 6 否：两树无名。
- 未把 GR-02/03 写成「本批引入」。V2.10 已定性为既有夹具与真实规则不一致，本 diff 只合法化指定夹具。
- 未发现需新临时编号的本批引入缺陷。AccessServiceTests 未改类上的历史观察不在本 diff，不误写为本批引入，B4 也不把未改类扩入。

修复内容：**PASS**。

---

## 6. 技能预检与按收窄口径实际使用

已读再用（控制提示词 §6 收窄；本席为乙席，B4 禁止修复）：

| 技能 | 本轮用法 | 缺口 |
|---|---|---|
| using-superpowers | 已读 SKILL.md。本体「任何回复前必须 invoke / 进 plan mode 先 brainstorm」与无头 B4、不问人、不派子代理冲突，按提示词作废；只保留技能自检。 | 未按其清单另开 plan/spec 文件 |
| receiving-code-review | 已读。只借「先核实再接受或反驳、禁止表演性同意」。操作员批准的是 054–056 范围，本席仍独立核实现。本体「逐条 IMPLEMENT」「不清就问人」作废。 | 无 |
| grilling | 已读。只借追问树：对 054/055/056 边界自问自答（§5）。不向操作员发问，不派 sub-agent 找事实。 | 未调用独立 `grill-me`/`batch-grill-me` 命令（结构已用） |
| verification-before-completion | 已读。在写 PASS 前实跑本节命令并读 exit code/失败数。 | 未跑 Codex 所报十一类 283 例；只跑本 diff 直接四类。不把未跑套件写成已通过 |
| requesting-code-review | B4=本席外审。不派第三席，不用 `HEAD~1`。 | 按提示词不用其派子代理部分 |
| graft | 只读：`callers changePositionTemplateLifecycle --depth 2`；`grep requirePlatformAdmin --in access/`；`callers consumeInternal --depth 1`。不 `graft build`、不另写审计产物 | 无 |
| long-running-background-tasks | 已读。本轮 Maven 前台 7.700s 结束，未后台化 | 无 |
| tdd / test-driven-development | B4 不写测试；只验证已有归因测试 | 未新写测试（禁止） |

明确不用：subagent-driven-development、dispatching-parallel-agents、using-git-worktrees、research、executing-plans、code-review（mattpocock 并行子代理）、open-code-review。未核到的技能未写成已调用。无第三席。

Graft 本轮约节省 116339 tokens（3 calls：≈48558+30200+37581）。

---

## 7. 验证（complex_test / JDK21；只读 3 个 COMPLEX_DATABASE 变量）

- 从 `/etc/complex/complex.env` 只解析 `COMPLEX_DATABASE_URL` / `USERNAME` / `PASSWORD` 三键；不 source 全文件；不导出其它 `COMPLEX_*`；不在本报告输出凭据。原 URL 库名=`complex`，进程内改写为 `complex_test` 后注入。username_len=7，password_len=48（只报长度）。
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`；运行时 `Temurin/OpenJDK 21.0.12.1`。测试日志：`Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)`；Flyway `Successfully validated 395 migrations`；`Current version of schema "public": 427`；`No migration necessary`。
- 命令：`mvn -f backend/pom.xml -Dtest=M006SecurityRegressionTests,M010RegressionTests,BarStoredValueFundChainJdbcIntegrationTests,StoredValueNotificationIntegrationTest -DfailIfNoTests=true test`
- **MAVEN_EXIT=0**。Surefire XML：

| 类 | tests | failures | errors | skipped |
|---|---|---|---|---|
| StoredValueNotificationIntegrationTest | 4 | 0 | 0 | 0 |
| M010RegressionTests | 44 | 0 | 0 | 0 |
| M006SecurityRegressionTests | 39 | 0 | 0 | 0 |
| BarStoredValueFundChainJdbcIntegrationTests | 8 | 0 | 0 | 0 |
| **合计** | **95** | **0** | **0** | **0** |

通知类 4 例含新增项目卡拒绝用例。未声称全仓或十一类 283 例（本席未跑）。`git diff --check` 两树 exit 0。

---

## 8. 新问题

无。完整 22 块审完后没有需要新临时编号、四分类和冻结 ID 的本批引入缺陷。旧夹具/旧观察不记为本批引入。

---

## 9. 两种结论

- **修复内容结论：PASS。** 054 仅生命周期入口兼容真实 USB，保留 Controller 功能权限、状态、占用、独立审批与审计，未改共享 `requirePlatformAdmin` 或其它入口。055 仅三个指定测试类合法商户夹具，金额/余额/退款/幂等/通知/隐私断言保留；通知与通用幂等走商户卡；项目卡 `consumeInternal` 无商户范围仍拒绝；项目卡充值通知保留；生产资金/员工服务未改。056 只补明确受益人 schedule，普通最高职级仍 `MASTER_AUTHORITY_REQUIRED`，真实 USB 可批，生产受益人检查未豁免。三份 HTML 未改。先改后确认的时序已如实记录，不倒填。
- **范围归因结论：PASS。** 5 路径 22 块 89+/25- 每一改动行（含五个文件版本号）均可归到 findings V2.10 当前正式 YES 的 M009-054 / 055 / 056。无越界。

总结论：PASS。

此票不是模块 PASS，也不是 B4 双 PASS。须席甲独立封存 B4 票且双 PASS 后才进 B5。本席未改 findings 队列归属。
