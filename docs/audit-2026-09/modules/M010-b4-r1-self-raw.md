CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M010 B4-r1 修复内容复审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B4-r1（B3 第 1 次整批修复的完整 diff 复审）。2026-09-09T14:04:02Z
- before tree：`c9f0eddc65514b33494905cabdb02cf16572f6eb`；after tree：`165925c18c1c31438fa3c54c6d194af7d8dabeeb`；`git diff --binary c9f0eddc6551 165925c18c1c` 全文 15205 bytes SHA256 d472de189809ceb125b72e09fa649228387c32eccceec25fa5610a09552af888（全文附于本报告末尾）。
- 定稿队列（findings V1.29 定稿）：M010-001、002、003、004 共 4 项；本轮只修这 4 项、归因各 ID 的最小 JDBC 断言与下游最小伴生（001：settlement 两测试；003：M011 六处夹具）。
- 锁四值：实算/sidecar/ledger/Grok 均 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。

## 1. diffstat

```
...business_entity_topology_and_role_isolation.sql | 26 +++++++++++
 .../governance/PlatformJdbcIntegrationTests.java   | 52 +++++++++++++++++++---
 ...venueStreamAttributionJdbcIntegrationTests.java | 21 +--------
 .../SettlementEntityAttributionS1Tests.java        | 11 ++---
 4 files changed, 76 insertions(+), 34 deletions(-)
```

## 2. 逐块归因（每一行改动都归到定稿 ID；「版本铁律」行随其所在文件的 ID）

| 文件 | 块 | 位置 | 块内标记 ID | 归因 |
|---|---|---|---|---|
| backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql | 1 | `@@ -0,0 +1,26 @@` | M010-001, M010-002, M010-002/003, M010-003, M010-004, 版本铁律（随所属ID） | M010-001/002/003/004（按段注释） |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | M010-001/002/003/004（新测试）+ 003（六处夹具伴生） |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 2 | `@@ -63,7 +63,7 @@ class PlatformJdbcIntegrationTests {` | M010-003 | M010-001/002/003/004（新测试）+ 003（六处夹具伴生） |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 3 | `@@ -91,7 +91,7 @@ class PlatformJdbcIntegrationTests {` | — | M010-001/002/003/004（新测试）+ 003（六处夹具伴生） |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 4 | `@@ -113,7 +113,7 @@ class PlatformJdbcIntegrationTests {` | — | M010-001/002/003/004（新测试）+ 003（六处夹具伴生） |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 5 | `@@ -387,7 +387,7 @@ class PlatformJdbcIntegrationTests {` | — | M010-001/002/003/004（新测试）+ 003（六处夹具伴生） |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 6 | `@@ -409,7 +409,7 @@ class PlatformJdbcIntegrationTests {` | — | M010-001/002/003/004（新测试）+ 003（六处夹具伴生） |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 7 | `@@ -423,7 +423,7 @@ class PlatformJdbcIntegrationTests {` | — | M010-001/002/003/004（新测试）+ 003（六处夹具伴生） |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 8 | `@@ -436,6 +436,44 @@ class PlatformJdbcIntegrationTests {` | M010-001, M010-002/003, M010-004 | M010-001/002/003/004（新测试）+ 003（六处夹具伴生） |
| backend/src/test/java/com/complex/platform/settlement/RevenueStreamAttributionJdbcIntegrationTests.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | M010-001（伴生：删父链方法与类头第三条） |
| backend/src/test/java/com/complex/platform/settlement/RevenueStreamAttributionJdbcIntegrationTests.java | 2 | `@@ -19,7 +20,7 @@ import org.springframework.jdbc.core.JdbcT` | M010-001 | M010-001（伴生：删父链方法与类头第三条） |
| backend/src/test/java/com/complex/platform/settlement/RevenueStreamAttributionJdbcIntegrationTests.java | 3 | `@@ -115,22 +116,4 @@ class RevenueStreamAttributionJdbcInteg` | — | M010-001（伴生：删父链方法与类头第三条） |
| backend/src/test/java/com/complex/platform/settlement/SettlementEntityAttributionS1Tests.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | M010-001（伴生：去父链断言） |
| backend/src/test/java/com/complex/platform/settlement/SettlementEntityAttributionS1Tests.java | 2 | `@@ -221,19 +222,13 @@ class SettlementEntityAttributionS1Tes` | M010-001 | M010-001（伴生：去父链断言） |

## 3. 红绿证据（B3-r2 运行记录 /tmp/m010_b3_run.log 原文）

```
START=2026-09-09T13:47:13Z
BEFORE_TREE=c9f0eddc65514b33494905cabdb02cf16572f6eb
=== PHASE RED: JDBC tests for 001/002/003/004 (M011 file) ===
PATCHED test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
BUMPED test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java V0.03
RED_DONE
RED_EXIT=1
[ERROR] Tests run: 16, Failures: 2, Errors: 1, Skipped: 0, Time elapsed: 3.230 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.entityRoleCheckRejectsSpvAndOperation -- Time elapsed: 0.005 s <<< FAILURE!
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.organizationScopedRolesMayShareNameAcrossOrganizations -- Time elapsed: 0.007 s <<< ERROR!
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.parentEntityColumnIsRemovedFromBusinessEntities -- Time elapsed: 0.007 s <<< FAILURE!
[ERROR] Tests run: 16, Failures: 2, Errors: 1, Skipped: 0
=== PHASE GREEN: migration V410 + companions ===
WROTE V410__m010_business_entity_topology_and_role_isolation.sql
PATCHED_ALL test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
PATCHED test/java/com/complex/platform/settlement/SettlementEntityAttributionS1Tests.java
BUMPED test/java/com/complex/platform/settlement/SettlementEntityAttributionS1Tests.java V0.01
PATCHED test/java/com/complex/platform/settlement/RevenueStreamAttributionJdbcIntegrationTests.java
BUMPED test/java/com/complex/platform/settlement/RevenueStreamAttributionJdbcIntegrationTests.java V0.01
GREEN_DONE 410
GREEN_EXIT=1
2026-09-09T06:47:33.423-07:00  INFO 2642151 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "410 - m010 business entity topology and role isolation"
2026-09-09T06:47:34.672-07:00  INFO 2642151 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Successfully applied 1 migration to schema "public", now at version v410 (execution time 00:01.222s)
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.275 s -- in com.complex.platform.settlement.SettlementEntityAttributionS1Tests
[ERROR] Tests run: 16, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.960 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.entityRoleCheckRejectsSpvAndOperation -- Time elapsed: 0.007 s <<< FAILURE!
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.095 s -- in com.complex.platform.governance.PlatformServiceTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.882 s -- in com.complex.platform.governance.PlatformControllerTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.066 s -- in com.complex.platform.leasing.LeasingJdbcIntegrationTests
[ERROR] Tests run: 69, Failures: 1, Errors: 0, Skipped: 0
AFTER_TREE=44bea876c39eaaa73600c85b3c89cfb20e27b3c5
END=2026-09-09T13:47:39Z
=== GREEN2 (after test fix, 2026-09-09T13:48:46Z) ===
GREEN2_EXIT=see above
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.046 s -- in com.complex.platform.settlement.RevenueStreamAttributionJdbcIntegrationTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.108 s -- in com.complex.platform.settlement.SettlementEntityAttributionS1Tests
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.764 s -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.097 s -- in com.complex.platform.governance.PlatformServiceTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.831 s -- in com.complex.platform.governance.PlatformControllerTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.062 s -- in com.complex.platform.leasing.LeasingJdbcIntegrationTests
[INFO] Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
AFTER_TREE2=165925c18c1c31438fa3c54c6d194af7d8dabeeb
```

## 4. 本席复审结论

见文末追加的判定段。

## 附：完整 diff（before→after，--binary）

```diff
diff --git a/backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql b/backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql
new file mode 100644
index 00000000..647844c1
--- /dev/null
+++ b/backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql
@@ -0,0 +1,26 @@
+-- 文件版本：V0.01
+-- V410 审计 M010（2026-09-09）：主体拓扑与角色隔离修复。操作员裁定：新迁移自 V410 起编号（跳过未发布分支的 V400–V409）；
+-- M010-001 删列已按 GEN-014 明确授权；M010-002/003 添加项按 2.2 答「删除」。历史迁移 V005/V048/V318/V354 正文不改。
+-- 业务依据（三份 HTML 最终解释权）：BEQR-144 / PGM-018 / BSM-028「物理项目与运营公司是多对多关系，不是固定的“平台→项目公司→域公司→商户”行政隶属链」；
+-- PGM-018/019 主体角色只有顶层平台、城市项目公司、域主管公司（业态主体）三类。
+
+-- M010-001：删除 V318 引入的固定父链列与索引（后台 M009-012 已不再读写；V354 六行主体骨架的稳定 id/名称/角色保留）。
+DROP INDEX IF EXISTS idx_business_entities_parent;
+ALTER TABLE business_entities DROP COLUMN IF EXISTS parent_entity_id;
+
+-- M010-003 / M010-002：存量「operation」「spv」角色行（V354 已按软删铁律归档为 inactive）迁为域主管公司角色后，
+-- 把 entity_role 白名单收为 HTML 三角色；不删除任何行。
+UPDATE business_entities
+   SET entity_role = 'management_company',
+       updated_at  = now()
+ WHERE entity_role IN ('operation', 'spv');
+ALTER TABLE business_entities DROP CONSTRAINT IF EXISTS chk_be_entity_role;
+ALTER TABLE business_entities ADD CONSTRAINT chk_be_entity_role
+    CHECK (entity_role IN ('platform', 'project_company', 'management_company'));
+COMMENT ON COLUMN business_entities.entity_role IS
+    '主体角色（PGM-018/019）:platform=顶层平台 / project_company=城市项目公司 / management_company=域主管公司（业态主体）';
+
+-- M010-004：V048 组织隔离配套唯一性——全局角色（organization_id 为空）之间同名唯一、同一组织内同名唯一、不同组织可同名。
+ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_role_name_key;
+CREATE UNIQUE INDEX IF NOT EXISTS uq_roles_org_role_name
+    ON roles ((COALESCE(organization_id, '')), role_name);
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
index dec3176b..844e6f75 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.02
+// 文件版本：V0.03
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -63,7 +63,7 @@ class PlatformJdbcIntegrationTests {
         // AUD-005 真分页: SQL COUNT + LIMIT/OFFSET(含 include_archived + delete_status 谓词), 第二页可达首页之后的商户。
         Instant t = Instant.now();
         repository.saveBusinessEntity(new BusinessEntityRecord(
-                "be_pg", "PG Entity", "operation", null, null, null, "active", t, t)); // M009-012：主体记录不再带 parent 链
+                "be_pg", "PG Entity", "management_company", null, null, null, "active", t, t)); // M009-012：主体记录不再带 parent 链；M010-003：operation 已出白名单
         repository.saveMerchant(new MerchantRecord("mch_pg_1", "PG A", "other", "be_pg", "be_pg", "be_pg",
                 "C1", "18800000001", "Owner", "1F-A", new java.math.BigDecimal("10.00"), new java.math.BigDecimal("9.00"),
                 null, null, new java.math.BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
@@ -91,7 +91,7 @@ class PlatformJdbcIntegrationTests {
         BusinessEntityRecord saved = repository.saveBusinessEntity(new BusinessEntityRecord(
                 "be_jdbc_formal",
                 "  P0 Platform Entity  ",
-                "operation",
+                "management_company",
                 "91310000MA1P0",
                 "Jane Ops",
                 "vault://bank/entity",
@@ -113,7 +113,7 @@ class PlatformJdbcIntegrationTests {
                 .contains(tuple(
                         "be_jdbc_formal",
                         "  P0 Platform Entity  ",
-                        "operation",
+                        "management_company",
                         "91310000MA1P0",
                         "vault://bank/entity",
                         "active"
@@ -387,7 +387,7 @@ class PlatformJdbcIntegrationTests {
     void merchantContactPhonesArePersistedAsCiphertextAndReadBackAsPlaintext() {
         Instant t = Instant.now();
         repository.saveBusinessEntity(new BusinessEntityRecord(
-                "be_pii", "PII Entity", "operation", null, null, null, "active", t, t));
+                "be_pii", "PII Entity", "management_company", null, null, null, "active", t, t));
         repository.saveMerchant(new MerchantRecord("mch_pii_1", "PII 商户", "other", "be_pii", "be_pii", "be_pii",
                 "第一联系人", "18800000021", "负责人", "1F-P", new BigDecimal("10.00"), new BigDecimal("9.00"),
                 "第二联系人", "18800000022", new BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
@@ -409,7 +409,7 @@ class PlatformJdbcIntegrationTests {
         // 防二次加密：把读回来的（明文）记录原样再存一次，落库仍是可解密的单层密文。
         Instant t = Instant.now();
         repository.saveBusinessEntity(new BusinessEntityRecord(
-                "be_pii2", "PII Entity 2", "operation", null, null, null, "active", t, t));
+                "be_pii2", "PII Entity 2", "management_company", null, null, null, "active", t, t));
         repository.saveMerchant(new MerchantRecord("mch_pii_2", "PII 商户2", "other", "be_pii2", "be_pii2", "be_pii2",
                 "联系人", "18800000023", "负责人", "1F-Q", new BigDecimal("10.00"), new BigDecimal("9.00"),
                 null, null, new BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
@@ -423,7 +423,7 @@ class PlatformJdbcIntegrationTests {
         // DB 级边界（V249 思路）：任何绕开仓库的手写 SQL / 批量导入写明文，数据库直接拒。
         Instant t = Instant.now();
         repository.saveBusinessEntity(new BusinessEntityRecord(
-                "be_pii3", "PII Entity 3", "operation", null, null, null, "active", t, t));
+                "be_pii3", "PII Entity 3", "management_company", null, null, null, "active", t, t));
         assertThatThrownBy(() -> jdbcTemplate.update(
                 "INSERT INTO merchants (id, merchant_name, merchant_type, contact_phone, status, created_at, updated_at) "
                         + "VALUES ('mch_pii_plain', 'X', 'other', '18800000024', 'draft', now(), now())"))
@@ -436,6 +436,44 @@ class PlatformJdbcIntegrationTests {
         assertThat(platformAdminPermissionCodes()).contains("pii.contact_phone.view");
     }
 
+    // ---- M010-001：V318 固定父链列已删除（BEQR-144/PGM-018 多对多协作，不是隶属链）。
+    @Test
+    void parentEntityColumnIsRemovedFromBusinessEntities() {
+        assertThat(jdbcTemplate.queryForObject(
+                "SELECT count(*) FROM information_schema.columns WHERE table_name = 'business_entities' AND column_name = 'parent_entity_id'",
+                Integer.class)).isZero();
+        assertThat(jdbcTemplate.queryForObject(
+                "SELECT count(*) FROM pg_indexes WHERE tablename = 'business_entities' AND indexname = 'idx_business_entities_parent'",
+                Integer.class)).isZero();
+    }
+
+    // ---- M010-002/003：entity_role 白名单只剩 HTML 三角色，spv/operation 被库层拒绝（操作员 2.2 答「删除」）。
+    @Test
+    void entityRoleCheckRejectsSpvAndOperation() {
+        // 约束定义本身只剩 HTML 三角色（同一事务内只能放一条违例语句，PostgreSQL 违例后事务即中止）。
+        String definition = jdbcTemplate.queryForObject(
+                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'business_entities'::regclass AND conname = 'chk_be_entity_role'",
+                String.class);
+        assertThat(definition).contains("'platform'", "'project_company'", "'management_company'")
+                .doesNotContain("'spv'", "'operation'");
+        assertThat(jdbcTemplate.update(
+                "INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) VALUES ('be_role_ok', '角色探针', 'management_company', 'active', now(), now())"))
+                .isEqualTo(1);
+        assertThatThrownBy(() -> jdbcTemplate.update(
+                "INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) VALUES ('be_role_spv', '角色探针', 'spv', 'active', now(), now())"))
+                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
+    }
+
+    // ---- M010-004：V048 组织隔离配套——不同组织可同名角色，同组织/全局同名仍唯一。
+    @Test
+    void organizationScopedRolesMayShareNameAcrossOrganizations() {
+        String insert = "INSERT INTO roles (id, role_name, description, data_scope, status, created_at, organization_id) VALUES (?, ?, NULL, 'project', 'active', now(), ?)";
+        assertThat(jdbcTemplate.update(insert, "role_m010_a", "店长（M010探针）", "org_m010_a")).isEqualTo(1);
+        assertThat(jdbcTemplate.update(insert, "role_m010_b", "店长（M010探针）", "org_m010_b")).isEqualTo(1);
+        assertThatThrownBy(() -> jdbcTemplate.update(insert, "role_m010_a2", "店长（M010探针）", "org_m010_a"))
+                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
+    }
+
     private List<String> platformAdminPermissionCodes() {
         return jdbcTemplate.queryForList("""
                 SELECT permission_code
diff --git a/backend/src/test/java/com/complex/platform/settlement/RevenueStreamAttributionJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/settlement/RevenueStreamAttributionJdbcIntegrationTests.java
index 418751ba..50b804bf 100644
--- a/backend/src/test/java/com/complex/platform/settlement/RevenueStreamAttributionJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/settlement/RevenueStreamAttributionJdbcIntegrationTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.settlement;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 
@@ -19,7 +20,7 @@ import org.springframework.jdbc.core.JdbcTemplate;
  * <ol>
  *   <li>解析优先级:项目特例 > 平台默认 > 未配置(null);</li>
  *   <li>通票 settleMonth 把归属主体标注进 settlement_bills.attributed_entity_id;</li>
- *   <li>business_entities.parent_entity_id 层级落库可读回(L3→L2 父链)。</li>
+ *   <li>（M010-001：原第三条「parent_entity_id 层级落库可读回」随固定父链列删除而移除；BEQR-144/PGM-018 多对多协作。）</li>
  * </ol>
  * 走真实 Flyway 迁移库(complex_test);须主会话串行跑(勿与其它 Flyway 集成测试并发)。
  */
@@ -115,22 +116,4 @@ class RevenueStreamAttributionJdbcIntegrationTests {
                 "SELECT attributed_entity_id FROM settlement_bills WHERE project_id = ?", String.class, PROJECT);
         assertThat(attributed).isEqualTo(ENT_PROJECT);
     }
-
-    @Test
-    void businessEntityParentLayerPersists() {
-        Instant now = Instant.now();
-        // L2 项目公司(父=某平台),L3 管理公司(父=该项目公司)
-        jdbcTemplate.update("""
-                INSERT INTO business_entities (id, entity_name, entity_role, parent_entity_id, status, created_at, updated_at)
-                VALUES (?, ?, 'project_company', NULL, 'active', ?, ?)
-                """, ENT_DEFAULT, "项目公司X", java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
-        jdbcTemplate.update("""
-                INSERT INTO business_entities (id, entity_name, entity_role, parent_entity_id, status, created_at, updated_at)
-                VALUES (?, ?, 'management_company', ?, 'active', ?, ?)
-                """, ENT_PROJECT, "娱乐平台公司X", ENT_DEFAULT, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
-
-        String parent = jdbcTemplate.queryForObject(
-                "SELECT parent_entity_id FROM business_entities WHERE id = ?", String.class, ENT_PROJECT);
-        assertThat(parent).isEqualTo(ENT_DEFAULT);
-    }
 }
diff --git a/backend/src/test/java/com/complex/platform/settlement/SettlementEntityAttributionS1Tests.java b/backend/src/test/java/com/complex/platform/settlement/SettlementEntityAttributionS1Tests.java
index 57091e1d..9f6858fc 100644
--- a/backend/src/test/java/com/complex/platform/settlement/SettlementEntityAttributionS1Tests.java
+++ b/backend/src/test/java/com/complex/platform/settlement/SettlementEntityAttributionS1Tests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.settlement;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -221,19 +222,13 @@ class SettlementEntityAttributionS1Tests {
                 "be_central_catering", "management_company",
                 "be_amusement", "management_company",
                 "be_media", "management_company");
-        Map<String, String> expectedParent = Map.of(
-                "be_proj_changsha", "be_platform",
-                "be_ent_platform", "be_proj_changsha",
-                "be_central_catering", "be_proj_changsha",
-                "be_amusement", "be_proj_changsha",
-                "be_media", "be_proj_changsha");
-
         for (Map.Entry<String, String> e : expectedRole.entrySet()) {
             Map<String, Object> row = jdbcTemplate.queryForMap(
                     "SELECT * FROM business_entities WHERE id = ?", e.getKey());
             assertThat(row.get("entity_role")).as("%s 的主体角色", e.getKey()).isEqualTo(e.getValue());
             assertThat(row.get("status")).isEqualTo("active");
-            assertThat(row.get("parent_entity_id")).isEqualTo(expectedParent.get(e.getKey()));
+            // M010-001：固定父链列已删除（BEQR-144/PGM-018 多对多协作），骨架只保留稳定 id/名称/角色。
+            assertThat(row).doesNotContainKey("parent_entity_id");
             // 工商信息不得由代码编造,一律留空由运营在后台补。
             assertThat(row.get("unified_social_credit_code")).as("%s 不得预置统一社会信用代码", e.getKey()).isNull();
             assertThat(row.get("legal_representative")).isNull();

```

## 4. 本席复审结论（席甲，作者自审票）

本席逐块读完上述 diff 全文（4 文件，76+/34-），并以 §2 归因表核对每一行。

### 4.1 修复内容（逐 ID）

| ID | 修复内容 | 证据 | 本席票 |
|---|---|---|---|
| 001 | V410 §1：`DROP INDEX IF EXISTS idx_business_entities_parent; ALTER TABLE business_entities DROP COLUMN IF EXISTS parent_entity_id`；六行骨架的 id/名称/角色保留；伴生：SettlementEntityAttributionS1Tests 删 `expectedParent` 与父链断言、改为断言列不存在；RevenueStreamAttributionJdbcIntegrationTests 删 `businessEntityParentLayerPersists` 整方法与类头第三条 | `parentEntityColumnIsRemovedFromBusinessEntities` 红（列存在）→ 绿；psql 实态列 0、索引 0；结算两套件 8/8、2/2 | PASS |
| 002/003 | V410 §2：存量 `operation`/`spv` 行 UPDATE 为 `management_company` 后 CHECK 收为三值、COMMENT 改为三角色口径（Grok B2 裁定 UPDATE 而非 NOT VALID）；伴生：M011 六处夹具 `operation`→`management_company`（含断言字面量） | `entityRoleCheckRejectsSpvAndOperation` 红（约束定义含 spv/operation）→ 绿（首轮因同一事务两条违例 INSERT 中止而未过，改为约束定义断言 + 末尾一条违例 INSERT 后通过）；psql 实态 CHECK 三值、六行角色未变 | PASS |
| 004 | V410 §3：`DROP CONSTRAINT roles_role_name_key`，`CREATE UNIQUE INDEX uq_roles_org_role_name ON roles ((COALESCE(organization_id,'')), role_name)` | `organizationScopedRolesMayShareNameAcrossOrganizations` 红（第二组织插入 23505 错误）→ 绿；psql 实态索引存在 | PASS |

### 4.2 范围归因（§5 闸 3）

- 新迁移文件全部行按段注释归 001 / 003+002 / 004；「-- 文件版本：V0.01」随新文件；M011 文件的版本行 V0.02→V0.03 与三条新测试、六处夹具随各 ID；两份 settlement 测试的「// 文件版本：V0.01」行与删改随 001（两席 B2 裁定的下游最小伴生）。归因不到的行 0。
- 未改：三份业务 HTML、`plan/*`、锁、sidecar、ledger 基准、findings、V005/V048/V318/V354 正文、权限种子、M009 服务层、Flyway 配置、其它模块生产代码。after tree 再次实取相同。
- 环境：complex_test 已改名保留 + 新建（席甲自定并记 ledger V2.21），V410 在新库应用后 410=410；未提交/推送/合并/部署。

### 4.3 结论

内容修复：**PASS**。范围归因：**PASS**。总：**PASS**（作者自审票；须 Grok B4-r1 亦 PASS 才进 B5）。本席在本报告落盘前未读取 `M010-b4-r1-grok-raw.md`。
