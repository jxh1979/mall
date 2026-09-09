CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M010 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M010-b4-r1-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：任何 `M010-b4-r1-self-raw.md`（当前亦不存在）、席甲本次 B4 raw。定稿队列以 findings.md V1.29 顶部「当前 M010 B2-r1 双PASS定稿，4项待修（B3-r1；新迁移须待测试库处置）」为准，并与本席 B2-r1（`M010-b1-grok-raw.md` V0.02 追加段「允许的最小修复边界」）对照。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、4 个定稿「是」ID（M010-001/002/003/004）是否按最小边界修好、每一行能否归因到上述 ID、是否越界改 HTML/锁/sidecar/ledger 基准/权限种子/历史迁移/005–011/其它模块业务逻辑。未新造判据/角色，未询问操作员。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger 顶行同值。criteria-lock 版本仍 V0.11。`LOCK-AMEND-COUNT: 1`。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M010 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段 |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 正式队列「是」（4） | M010-001、M010-002、M010-003、M010-004 |
| 正式队列「否」 | M010-005、006、007、008、009、010、011（登记；009 版本号已由操作员裁定 V410） |
| B3 | 已执行 1 次（本轮），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `c9f0eddc65514b33494905cabdb02cf16572f6eb` |
| after（对象类型 tree，可解析） | `165925c18c1c31438fa3c54c6d194af7d8dabeeb` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary c9f0eddc65514b33494905cabdb02cf16572f6eb 165925c18c1c31438fa3c54c6d194af7d8dabeeb`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 15205 bytes；SHA256 `d472de189809ceb125b72e09fa649228387c32eccceec25fa5610a09552af888`；UTF-8；4 文件；76 插入 / 34 删除；14 hunk；name-status A+3M |
| 与 `/tmp/m010_b3_r1.diff` | `cmp` 全等（本席自算为准，副本仅对照） |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 7250 / after 7251；差集仅新增 `V410__m010_business_entity_topology_and_role_isolation.sql` |
| HEAD | `242f487233d028173cb9198eecd079f5d877405c` = `origin/main` |
| 工作区 4 文件 blob | 均等于 after（`647844c1` / `844e6f75` / `50b804bf` / `9f6858fc`） |
| findings.md | 工作区 V1.29（493314 字节，SHA256 `85edca2a7ac0e10a8af01376f8bc38a1207a105689ffdfb930b9cc048c16867d`）；两树 blob 均为 `8ef376e131dacab05df74ffced5d286918c9f640`（本 diff 未改 findings） |
| ledger.md | 两树 blob 均为 `f3726aabf34aeecf53868e899f319f9f51c9520d`（本 diff 未改） |
| 三 HTML 两树 blob | BSM `12334b13f3c27a815c9bbf9eea216403fb0d76d1` / PGM `c2588ca1c65a3f196b39a23f299bd56967b5652e` / BEQR `86a717930f9303d4f131ee2864478baa14450e6d`，完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |
| V005 / V048 / V318 / V354 | 两树 blob 相同（`94692f5a` / `61018fff` / `9f8eeaf9` / `b5181122`） |
| PlatformService / 权限种子 / Flyway 配置 | 两树 blob 相同 |
| IdempotencyServiceTests | 两树 blob 相同（未把 `operationId` 当角色码） |

`git diff --stat --binary`：

```
 ...business_entity_topology_and_role_isolation.sql | 26 +++++++++++
 .../governance/PlatformJdbcIntegrationTests.java   | 52 +++++++++++++++++++---
 ...venueStreamAttributionJdbcIntegrationTests.java | 21 +--------
 .../SettlementEntityAttributionS1Tests.java        | 11 ++---
 4 files changed, 76 insertions(+), 34 deletions(-)
```

`git diff --numstat`：26/0 新迁移；45/7 PlatformJdbcIntegrationTests；2/19 RevenueStreamAttributionJdbcIntegrationTests；3/8 SettlementEntityAttributionS1Tests。

工作区相对 HEAD 另有既有父链脏文件（M009 已修未提交、审计产物、工具等）。那些路径在 before/after 两树 blob 相同，不在本轮 diff 内，不按本轮修复内容审计。本席未改任何源文件，只新建本 raw。验证只在临时库 `m010_b4_grok_probe`（`TEMPLATE complex_test`，事后 DROP）与只读 SELECT；未改任一 `complex_test*` 的模式/数据，未在主仓跑写模式 mvn。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M010 的集合（GEN-001…018 共 18）。本轮只用定稿 001–004 已引用的 ID：GEN-001/003/005/006/008/014/016/017，以及 B2 已引用的 HTML 原文 BEQR-144 / PGM-018 / PGM-019 / BSM-028（映射在 M009/M011，本批 SQL 注释引用不等于把 HTML 锚点扩进 M010 映射）。不 LOCK-AMEND。

B3-r1 边界（与本席 B2-r1 票及 findings V1.29 顶部一致）：

- **一条** 新迁移 `V410__m010_business_entity_topology_and_role_isolation.sql`（按 ID 分段；不要拆成 V410–V413，也不要启用 outOfOrder）：001 删索引/列；002+003 存量 `operation`/`spv` 行 UPDATE 坍缩为 `management_company` 后 CHECK 收三值并改 COMMENT；004 删 `roles_role_name_key`、建表达式唯一索引 `UNIQUE (COALESCE(organization_id, ''), role_name)`。
- 不改 V005/V048/V318/V354 正文；不改三份业务 HTML、锁、权限种子、M009 服务层白名单。
- M011 `PlatformJdbcIntegrationTests`：列不存在、CHECK 拒 `spv`/`operation` 且收三值、两组织同名可共存/同组织仍拒；六处 `operation` 夹具改为 `management_company`（归因 003）。
- 001 伴生：结算 S1 去父链断言；删除 `businessEntityParentLayerPersists` 整方法（含类头第三条）。
- 全部改动文件自身版本递增（无版本者加 V0.01）。

禁止：方案 A 留列；`NOT VALID` 留存量角色码；新建多对多协作表；把 010 改成修复队列是；为 009 改 Flyway 配置或把测试库处置写进本 diff；无 ID 改 PRD/术语表/008 状态闭集；把幂等测试里的 `operationId` 当角色码修改。

四分类本轮不重开 001–004：B2 已定为冲突/添加/添加/细化。落地仍停在该格：001 删除 HTML 禁止的隶属链列，不是新建协作表；002/003 收回无 HTML 来源的角色码（操作员 2.2 答删除）；004 把 V048 已声称的组织隔离落到唯一约束。无业务发明。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary c9f0eddc65514b33494905cabdb02cf16572f6eb 165925c18c1c31438fa3c54c6d194af7d8dabeeb`

before tree = `c9f0eddc65514b33494905cabdb02cf16572f6eb`
after tree = `165925c18c1c31438fa3c54c6d194af7d8dabeeb`

字节 15205；SHA256 `d472de189809ceb125b72e09fa649228387c32eccceec25fa5610a09552af888`；UTF-8 完整原文如下。

````diff
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
````

---

## 4. §5 闸3 逐行归因（14 hunk / 全部命中）

任一行归因不到即整轮 FAIL。本席按 hunk 全覆盖；「文件版本」行按操作员版本铁律随该文件所属 ID。

### 4.1 新文件 `V410__m010_business_entity_topology_and_role_isolation.sql`（hunk 1，+26）

| 行块 | 内容 | 归因 |
|---|---|---|
| `-- 文件版本：V0.01` | 新文件首版 | 版本铁律；该文件承载 001/002/003/004，随本文件 |
| 头注 V410 编号、GEN-014 授权、2.2 答删除、不改 V005/V048/V318/V354、引用 BEQR-144/PGM-018/BSM-028/PGM-019 | 说明性注释，非新业务规则 | 同文件分段头；009 否（只使用已裁定版本号，无配置改动） |
| `DROP INDEX IF EXISTS idx_business_entities_parent;` + `DROP COLUMN IF EXISTS parent_entity_id;` | 方案 B 删列+索引 | **M010-001** |
| `UPDATE … entity_role IN ('operation','spv')` 坍缩为 `management_company` | 先 UPDATE 再约束 | **M010-003 / M010-002**（B2 裁定同一句，不用 NOT VALID） |
| `DROP/ADD chk_be_entity_role` 三值 | CHECK 收 HTML 三角色 | **M010-002 / M010-003** |
| `COMMENT ON COLUMN … entity_role` | 去掉 spv/operation/四层伪称 | **M010-002 / M010-003** |
| `DROP CONSTRAINT roles_role_name_key` + `CREATE UNIQUE INDEX uq_roles_org_role_name ON roles ((COALESCE(organization_id, '')), role_name)` | 与 B2 已写形态逐字一致 | **M010-004** |

无拆成 V410–V413，无 outOfOrder，无方案 A 留列。

### 4.2 `PlatformJdbcIntegrationTests.java`（hunk 2–9）

| hunk | 行 | 内容 | 归因 |
|---|---|---|---|
| 2 | 文件版本 V0.02→V0.03 | 版本铁律 | 随本文件（001–004 测试 + 003 夹具） |
| 3 | L66 `operation`→`management_company`（含注释补 M010-003） | 夹具 1/6 | **M010-003** |
| 4 | L94 构造器角色字面量 | 夹具 2/6 | **M010-003** |
| 5 | L116 断言 tuple 角色字面量 | 夹具 3/6 | **M010-003** |
| 6 | L390 `be_pii` | 夹具 4/6 | **M010-003** |
| 7 | L412 `be_pii2` | 夹具 5/6 | **M010-003** |
| 8 | L426 `be_pii3` | 夹具 6/6 | **M010-003** |
| 9 | `parentEntityColumnIsRemovedFromBusinessEntities` | 列/索引不存在 | **M010-001**（验证性测试） |
| 9 | `entityRoleCheckRejectsSpvAndOperation` | 约束定义收三值且不含 spv/operation；合法 INSERT；spv INSERT 拒 | **M010-002 / M010-003**（验证性测试） |
| 9 | `organizationScopedRolesMayShareNameAcrossOrganizations` | 两组织同名可插入、同组织仍 DuplicateKey | **M010-004**（验证性测试） |

六处位点与本席 B2 点名的 66/94/116/390/412/426 全等。`be_operation_1/2/3` 是商户 id，不是 `entity_role`，未动，符合「不要把幂等/其它 operation 字面量当角色码」。

### 4.3 `RevenueStreamAttributionJdbcIntegrationTests.java`（hunk 10–12）

| hunk | 内容 | 归因 |
|---|---|---|
| 10 | 新增 `// 文件版本：V0.01` | 版本铁律；该文件本批只为 001 伴生 |
| 11 | 类头第三条由「parent_entity_id 层级落库可读回」改为删除说明 | **M010-001** |
| 12 | 整方法 `businessEntityParentLayerPersists` 删除（含 INSERT/SELECT 父链） | **M010-001** |

未改写成新协作语义；该类其余收入归属用例未动。

### 4.4 `SettlementEntityAttributionS1Tests.java`（hunk 13–14）

| hunk | 内容 | 归因 |
|---|---|---|
| 13 | 新增 `// 文件版本：V0.01` | 版本铁律；该文件本批只为 001 伴生 |
| 14 | 删除 `expectedParent` 与 `parent_entity_id` 相等断言；改为 `doesNotContainKey("parent_entity_id")` | **M010-001** |

保留六 id/角色/status/工商空值/`archive_reason` 键/「资产公司」0 行/非法角色 CHECK 拒绝（方法其余部分不在 diff 内）。未发明多对多协作表。方法名仍含 `ParentChain`（见观察，非越界行）。

### 4.5 闸3 结论

diff 中每一行均可归因到定稿「是」ID 001–004 之一（验证性测试归因到它所锁的 ID；版本行随所属文件）。无 HTML、锁、sidecar、ledger 基准、权限种子、V005/V048/V318/V354、Flyway 配置、M009 服务层、005–011 对应内容。范围归因 **PASS**。

---

## 5. 逐 ID 修复内容（是否修好 / 最懒 / 新缺陷或发明）

### M010-001（冲突；方案 B 删列，GEN-014 已授权）

活库（只读 `complex_test`）：`parent_entity_id` 列 count=0；`idx_business_entities_parent` count=0；`business_entities` 现 10 列（V005 九列 + `archive_reason`，父链列消失）。六骨架稳定 id/名称/角色仍在且全 `active`，工商三列空，`archive_reason` 键仍在，「资产公司」0 行。

SQL 形态与本席 B2 原文一致（显式 `DROP INDEX IF EXISTS` 可保留）。未改 V318/V354。伴生两测试按清单执行。未新建协作表。

最懒：成立（删列比留列改 COMMENT 更彻底且已授权；测试只去父链断言/删整方法）。

新缺陷/发明：未发现。`doesNotContainKey` 比「只删断言」略强，仍是证明列已删除，不是新协作语义。

票：**修好**。

### M010-002 / M010-003（添加；操作员均答删除）

活库 CHECK=`entity_role IN (platform, project_company, management_company)`，定义不含 `'spv'`/`'operation'`。COMMENT 已改为 PGM-018/019 三角色中文说明。`entity_role IN ('operation','spv')` 行 count=0。六骨架角色恰为 1 platform + 1 project_company + 4 management_company。

探针库（副本，事后 DROP）：三角色 INSERT 成功并 ROLLBACK；`spv` INSERT 与 `operation` INSERT 均 `violates check constraint chk_be_entity_role`。先 UPDATE 再 ADD 约束，无 `NOT VALID`。夹具六处已改。生产 Java 0 处角色字面量 `spv`/`operation`，服务层白名单未回改。

最懒：成立（同一迁移收三值；枚举坍缩到已有 `management_company`，不新增角色、不删行）。

新缺陷/发明：未把添加改写成技术性；未发明酒吧 SPV 商户行。观察见 §7（analytics 夹具，B2 未列入）。

票：**修好**。

### M010-004（细化；表达式唯一索引）

活库：`roles_role_name_key` 不存在；`uq_roles_org_role_name` = `UNIQUE (COALESCE(organization_id, ''), role_name)`，与 B2 已写形态一致。未改 V048，未发明两套部分索引/角色目录。

探针：两组织同名 INSERT 成功；同组织第二行 `duplicate key … uq_roles_org_role_name`；两个全局 NULL 同名亦拒（索引本身覆盖 B2「含两个全局 NULL」；测试方法未写 NULL 分支，见观察）。

最懒：成立。

票：**修好**。

---

## 6. TDD 红绿（本席独立复算，不只信席甲日志）

可执行行为变更：001 删列、002/003 收 CHECK、004 改唯一约束。纯 SQL 外，B2 要求 M011 三条最小 JDBC 断言先红后绿。

席甲日志（只作对照，本席不采信为唯一证据）：

| 日志 | 事实（本席实读） |
|---|---|
| `/tmp/m010_b3_red.log` | classpath 377 迁移、库 version 399、未应用 V410。`parentEntityColumnIsRemovedFromBusinessEntities` expected 0 but was 1；`entityRoleCheckRejectsSpvAndOperation` Expecting throwable（spv INSERT 未拒）；`organizationScopedRolesMayShareNameAcrossOrganizations` DuplicateKey `roles_role_name_key`。Tests run 16, Failures 2, Errors 1。BUILD FAILURE。 |
| `/tmp/m010_b3_green.log` | 378 迁移，Flyway 399→410。`entityRoleCheckRejectsSpvAndOperation` 因同一 `@Transactional` 内两条违例 INSERT 得 `25P02 current transaction is aborted`（测试结构，非修复）。 |
| `/tmp/m010_b3_green2.log` | 已改为「约束定义 + 一条违例 INSERT」。PlatformJdbc 16/16、Settlement S1 8/8、RevenueStream 2/2、LeasingJdbc 4/4、PlatformService 31/31、PlatformController 10/10；Tests run 71, Failures 0。BUILD SUCCESS。Flyway current 410，validate 378。 |

本席独立：

1. **不**在主仓对 `complex_test` 跑 mvn（测试类写死该库 URL，会写数据）。
2. 只读 `complex_test`：max version=410，V410 success，列/索引/CHECK/COMMENT/唯一索引与绿终态一致。
3. 副本 `m010_b4_grok_probe`：绿断言（列 0、CHECK 拒 spv **与** operation、两组织同名可插入、同组织/双 NULL 仍唯一、六骨架仍在）全部成立；事务回滚后探针无残留。
4. 同一副本单事务内反向应用 V410（加回列/五值 CHECK/`roles_role_name_key`）后：parent_col=1；spv INSERT 成功；两组织同名撞 `roles_role_name_key`。随后 ROLLBACK，探针回到 V410 终态。独立复现红。
5. 只读 `complex_test_pre_v410_20260909`：max=409，V400–V409 共 10 行（席甲改名保留属环境动作，**不在本 diff**）。
6. 副本已 DROP。`complex_test` 事后仍 max=410、parent_col=0。

TDD 成立：先有可失败断言（红），再落 V410（绿）；中间 25P02 后把测试收到「定义断言 + 一条违例 INSERT」是最懒可执行形态，不是削弱锁（定义字符串同时锁 spv 与 operation；本席另用裸 SQL 证实 operation INSERT 亦拒）。

---

## 7. 越界检查

| 禁止项 | 两树 |
|---|---|
| 三份业务 HTML | blob 全等 |
| 锁 / sidecar / ledger 基准 | blob 全等（本 diff 未改） |
| findings | 两树 blob 全等 |
| 权限种子 / V005 | 全等 |
| 历史迁移 V048/V318/V354 | 全等 |
| M009 服务层白名单 PlatformService | 全等 |
| Flyway outOfOrder / application.yml | 全等 |
| IdempotencyServiceTests `operationId` | 全等 |
| 005–011 对应内容 | 未改 |
| 其它模块业务逻辑 | 结算生产代码未改；仅测试伴生按 B2 清单 |

本 diff 4 文件恰好等于派发/B2 允许面。闸3 越界 **未发生**。

---

## 8. 观察（不升格、不否决、不新开永久 ID）

1. **`AnalyticsJdbcIntegrationTests.merchantSummariesRollupAggregatesRealSources` L477 仍 INSERT `entity_role='operation'`。** 本席副本探针：该语句 `violates check constraint chk_be_entity_role`。B2 把 003 夹具伴生明文收窄为 M011 六处，010 维持否；本 diff 未改该文件，**遵守边界而非漏改**。不构成无 ID 新缺陷，不把 010 改「是」。交 B5 全量重审是否把该机械红升格回 003 伴生。
2. **004 测试未写「两个全局 NULL 同名」分支。** 索引 `COALESCE(organization_id,'')` 已覆盖；本席探针双 NULL 第二行撞 `uq_roles_org_role_name`。测试仍锁住 B2 主句「两组织同名可共存/同组织仍拒」。
3. **S1 方法名仍含 `ParentChain`。** 仅标识符，行为已去父链断言。
4. 未在主仓复跑 mvn；绿的 71/71 以席甲日志为对照、以本席 SQL 探针为独立证据。

分类（若日后升格）：观察 1 为 003 的跨模块机械后果（技术性），当前按 B2 清单不入本轮修复队列。

---

## 9. 问题二分与回退

| 类 | 内容 |
|---|---|
| 有 ID | 无。001/002/003/004 均按定稿最小边界修好，仍属本轮「是」队列（B4 双 PASS 后才由席甲改「否」；本席不改 findings/ledger）。 |
| 无 ID | 无。观察不升格为缺陷。 |

回退规则：无 ID 才整批回 B2；有 ID 且仍在修复队列才整批回 B3。本席两项结论均 PASS，不触发回退。

---

## 10. 技能与方法

本节点实读/自检：`verification-before-completion`（声称前先跑哈希与库探针）、`receiving-code-review`（先核源/命令再接受）、`grilling`（只借追问结构自查：删列是否真删、CHECK 是否真拒、唯一约束是否真按组织、是否越界、TDD 是否可失败）、`tdd`/`test-driven-development`（只核红绿，不落测试）、`ponytail-review`（只评最小删除/收窄，不落地）、`systematic-debugging`（25P02 根因=同事务两条违例 INSERT）、`writing-for-agents`（报告结构）。`requesting-code-review` 要派同会话子代理 → 第四席，**未用**。`batch-grill-me` 本环境未调用。无 MCP、无子代理、无第三席、未问操作员。

---

## 11. 结论

修复内容：**PASS**（001 删列+索引并完成两测试伴生；002/003 UPDATE 后 CHECK 收三值+COMMENT+六夹具；004 表达式唯一索引+最小可失败测试；TDD 红绿本席独立复现；无新业务发明）。

范围归因 / §5 闸3：**PASS**（4 文件 14 hunk 76+/34- 逐行命中 001–004；无越界）。

总结论：**PASS**。须席甲本批 B4 PASS 后合取；未经双 PASS 不进 B5，更不称模块 PASS。步骤仍 B4-r1，M010 IN_PROGRESS，B3=1。
