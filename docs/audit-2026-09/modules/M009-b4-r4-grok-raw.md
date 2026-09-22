CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B4-r4 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r4
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M009-b4-r4-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r4 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。第1项手机号明文豁免仍在 Controller，原冲突事实保留。不可用 B3 自报红→绿代替本轮完整审计。席甲自审票本席未读；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：`docs/audit-2026-09/modules/M009-b4-r4-self-raw.md`（路径存在：19894 字节、mtime 2026-09-21 22:48，按互盲禁读正文）。未读适配器缓冲、未读席甲本次 B4 意见。定稿队列以 findings.md V3.25 顶部「M009 本批B2双PASS定稿，进入一次授权B3」为准，并与本席 B1/B2 V0.14（`M009-b1-grok-raw.md` 最后一次完整报告）最小修复边界对照。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、2 个定稿「是」ID（M009-058 仅第2项、M009-059）是否按最小边界修好、每一行能否归因到该二 ID 之一、是否越界改 HTML/锁/sidecar/ledger 基准/权限种子/Controller/手机号/共享 PiiCipher/其它姓名列/CHECK/回填/第1项。未新造判据/角色，未询问操作员，未 LOCK-AMEND。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger 顶行同值。criteria-lock 版本仍 V0.11。`LOCK-AMEND-COUNT: 1`。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M009 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段 |
| 阶段 | B4-r4 只读审计 B3 累计第4次（操作员仅授权一次额外修复）的「修复内容」本身 |
| 正式队列「是」（2） | M009-058（仅第2项：merchant.viewed 对象/入口/项目/筛选分页）；M009-059（两姓名加解密 + 两列 TEXT + 既有测试/门禁） |
| 正式队列「否」 | 第1项手机号明文豁免（操作员「1不动」）；038–057 及历史已修/登记项保持否 |
| B3 | 历史 3 + 本次额外 1 = 累计第 4 次；模块仍 IN_PROGRESS（ledger V7.35） |
| findings.md | V3.25；工作区 SHA256 `f886e7b41bf45e179ee63395dc1b8448664d1871a7046624d41253975822279d`（本 diff 未改 findings） |
| before（对象类型 tree，可解析） | `d23497895e200310c4e9692323d5f4e431234981` |
| after（对象类型 tree，可解析） | `4a8843e8fd431fdd9c9639d2ba96013049986c7a` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary d23497895e200310c4e9692323d5f4e431234981 4a8843e8fd431fdd9c9639d2ba96013049986c7a`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 13576 bytes；SHA256 `6fa14a09544d45a41e6fc14bcc51d47b56f96525295e8869cd427a0bed41a4bb`；UTF-8；6 路径；91 插入 / 12 删除；14 hunk；name-status 5×M + 1×A |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3955 / after 3956；差集仅新增 `backend/src/main/resources/db/migration/V431__merchant_contact_names_ciphertext.sql` |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main`=`ebb1eb99c610df330c07d24278798c2d3611299a`（本席未 fetch） |
| 工作区六文件 blob | 全部等于 after（见 §1.1） |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3`；三者两树完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |
| PiiCipher.java 两树 blob | `ff8f5bdf49eb22845402f88caaccd634343a53d7` 相同（共享 cipher 未改） |
| PlatformController.java 两树 blob | `52d3f0d27191d6fd4cc923a1607a9263bc3eb7fa` 相同 |
| PlatformControllerTests.java 两树 blob | `a16333d9e0d89863c0620b30aec588eae84ebc8f` 相同 |
| 迁移号 | before 最大 V430；after 最大 V431；本地 origin/main 最大 V430 且无名 V431（未 fetch） |

`git diff --stat --binary`：

```
 .../governance/JdbcPlatformRepository.java         | 12 +++----
 .../platform/governance/PlatformService.java       | 12 +++++--
 .../V431__merchant_contact_names_ciphertext.sql    |  5 +++
 .../governance/PlatformJdbcIntegrationTests.java   | 28 ++++++++++++++-
 .../platform/governance/PlatformServiceTests.java  | 42 +++++++++++++++++++++-
 tools/verify-pii-encryption.mjs                    |  4 ++-
 6 files changed, 91 insertions(+), 12 deletions(-)
```

`git diff --numstat`：6/6 JdbcPlatformRepository.java；9/3 PlatformService.java；5/0 V431.sql；27/1 PlatformJdbcIntegrationTests.java；41/1 PlatformServiceTests.java；3/1 verify-pii-encryption.mjs。

### 1.1 工作区六文件仍等于 after 树

| 路径 | before blob | after=worktree blob | 判定 |
|---|---|---|---|
| `backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java` | `aa34c220de5a8aa45499d11e860a6797891a1dbf` | `f4c086abb98e715325399f4069596809269ad259` | MATCH-AFTER |
| `backend/src/main/java/com/complex/platform/governance/PlatformService.java` | `b325adc09bd66d73505130138889b7a87f9bd211` | `63e53b3177610f87a5b49e82173e375dbaa7d951` | MATCH-AFTER |
| `backend/src/main/resources/db/migration/V431__merchant_contact_names_ciphertext.sql` | 不存在 | `5fe129ae2425ede84c80998e29ecd14d27930d51` | MATCH-AFTER |
| `backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java` | `563eca720adda9e9c05ac09aa7143b9e82daa203` | `40f0068afcc68951ac68e9c234dc9d3239d8e3ff` | MATCH-AFTER |
| `backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java` | `394ae9d1f13d5d2229fe39093785d42186683a98` | `c9315fa5f2b6bdb30d3ec2551586b3cb15c5e85f` | MATCH-AFTER |
| `tools/verify-pii-encryption.mjs` | `bb5a6785e2d28c1882b2e1ad51862b4ec47960ca` | `d5864320e53f2d2ed926635eb4012158a75a685f` | MATCH-AFTER |

工作区 `git hash-object` 与 after 全等。真实 index 仍停在 before 各 blob（`MM`/`??`），符合 B3 用临时 `GIT_INDEX_FILE` 写 after tree、不碰真正暂存区的规程；B4 审的是两 tree，不是真实 index。本席未改任何源文件，只新建本 raw。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M009 的集合（本席点算 `M009 → […]` 含 PGM-026/041/043、GEN-008/010/011；**不含** PGM-057/059）。本轮只用定稿已引用的 ID。不 LOCK-AMEND。

B3-r4 边界（与本席 V0.14 票及 findings V3.25 顶部一致）：
- **058 仅第2项**：只改 `PlatformService.listMerchants` 既有 `merchant.viewed` metadata：本页 `merchantIds`、入口 `/api/v1/platform/merchants`、`projectId`（null→空串）、`includeArchived`、`businessFormat`（无筛选用空串）、实际 `page`/`pageSize` 及既有 `count`/`total`。禁止写姓名/手机号；不改 Controller 及手机号行为。必要既有 Service 测试覆盖不同页、筛选、空页和默认页归一化，自身版本递增。
- **059**：只在 `JdbcPlatformRepository` 共用 `merchantParameters`/`mapMerchant` 调用既有 `PiiCipher` 加/解密两列；新增最小 SQL 将两列改 TEXT（不删数据、不改旧迁移、不扩其他姓名列）；既有 `PlatformJdbcIntegrationTests` 验证新增/更新/两列原始密文和读取一致、120 字中文姓名、null 及空串。现有 `tools/verify-pii-encryption.mjs` 仅登记两列并升版。不新增历史回填/兼容组件；仅新建及后续保存走加密。不得改手机号、`owner_person`、`legal_representative`、其它模块姓名/生日、HTML 及共享 `PiiCipher`。
- 操作员「1不动」：手机号明文豁免保持前样；本轮 B4 不得写成模块 PASS。
- 四分类本轮不重开：058 仍冲突（第1项残留）；059 仍细化（PGM-026 档案下位字段）。无添加项。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary d23497895e200310c4e9692323d5f4e431234981 4a8843e8fd431fdd9c9639d2ba96013049986c7a`
字节：13576
SHA256：6fa14a09544d45a41e6fc14bcc51d47b56f96525295e8869cd427a0bed41a4bb

```diff
diff --git a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
index aa34c220..f4c086ab 100644
--- a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.08
+// 文件版本：V0.09
 import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
@@ -31,7 +31,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
 
     private final NamedParameterJdbcTemplate jdbcTemplate;
     private final ObjectMapper objectMapper;
-    // P0-A/GAP-07：商户联系人手机号 PII 落库加密（V339）。范式同 customers.phone(V188)/ticket_orders.buyer_phone(V247)：
+    // 商户主次联系人姓名及手机号 PII 落库加密。范式同 customers.phone(V188)/ticket_orders.buyer_phone(V247)：
     // 仓库层写时 encrypt、读时 decrypt，业务层始终拿明文；展示脱敏在 controller 按权限做。
     private final PiiCipher piiCipher;
 
@@ -622,13 +622,13 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 .addValue("business_entity_id", merchant.businessEntityId())
                 .addValue("contract_entity_id", merchant.contractEntityId())
                 .addValue("settlement_entity_id", merchant.settlementEntityId())
-                .addValue("contact_name", merchant.contactName())
+                .addValue("contact_name", piiCipher.encrypt(merchant.contactName()))
                 .addValue("contact_phone", piiCipher.encrypt(merchant.contactPhone()))
                 .addValue("owner_person", merchant.ownerPerson())
                 .addValue("mall_block_code", merchant.mallBlockCode())
                 .addValue("area_square_meter", merchant.areaSquareMeter())
                 .addValue("leased_area_square_meter", merchant.leasedAreaSquareMeter())
-                .addValue("secondary_contact_name", merchant.secondaryContactName())
+                .addValue("secondary_contact_name", piiCipher.encrypt(merchant.secondaryContactName()))
                 .addValue("secondary_contact_phone", piiCipher.encrypt(merchant.secondaryContactPhone()))
                 .addValue("rent_per_square_meter", merchant.rentPerSquareMeter())
                 .addValue("cooperation_form", merchant.cooperationForm())
@@ -715,13 +715,13 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 resultSet.getString("business_entity_id"),
                 resultSet.getString("contract_entity_id"),
                 resultSet.getString("settlement_entity_id"),
-                resultSet.getString("contact_name"),
+                piiCipher.decrypt(resultSet.getString("contact_name")),
                 piiCipher.decrypt(resultSet.getString("contact_phone")),
                 resultSet.getString("owner_person"),
                 resultSet.getString("mall_block_code"),
                 resultSet.getBigDecimal("area_square_meter"),
                 resultSet.getBigDecimal("leased_area_square_meter"),
-                resultSet.getString("secondary_contact_name"),
+                piiCipher.decrypt(resultSet.getString("secondary_contact_name")),
                 piiCipher.decrypt(resultSet.getString("secondary_contact_phone")),
                 resultSet.getBigDecimal("rent_per_square_meter"),
                 resultSet.getString("cooperation_form"),
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformService.java b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
index b325adc0..63e53b31 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformService.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.11
+// 文件版本：V0.12
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -147,7 +147,8 @@ public class PlatformService {
             throw ApiException.badRequest("MERCHANT_BUSINESS_FORMAT_INVALID",
                     "商户业态必须为 酒吧/餐饮/零售/游乐/传媒/其他 之一");
         }
-        PageResponse<MerchantRecord> result = repository.listMerchantsPaged(currentProject(), includeArchived, format, page, size);
+        String projectId = currentProject();
+        PageResponse<MerchantRecord> result = repository.listMerchantsPaged(projectId, includeArchived, format, page, size);
         auditEventService.recordSuccess(
                 "merchant.viewed",
                 "merchant.profile.view",
@@ -155,7 +156,12 @@ public class PlatformService {
                 "all",
                 null,
                 null,
-                Map.of("count", result.items().size(), "total", result.total())
+                Map.of("count", result.items().size(), "total", result.total(),
+                        "merchantIds", result.items().stream().map(MerchantRecord::id).toList(),
+                        "entry", "/api/v1/platform/merchants",
+                        "projectId", projectId == null ? "" : projectId,
+                        "includeArchived", includeArchived, "businessFormat", format == null ? "" : format,
+                        "page", result.page(), "pageSize", result.pageSize())
         );
         return result;
     }
diff --git a/backend/src/main/resources/db/migration/V431__merchant_contact_names_ciphertext.sql b/backend/src/main/resources/db/migration/V431__merchant_contact_names_ciphertext.sql
new file mode 100644
index 00000000..5fe129ae
--- /dev/null
+++ b/backend/src/main/resources/db/migration/V431__merchant_contact_names_ciphertext.sql
@@ -0,0 +1,5 @@
+-- 文件版本：V0.01
+-- M009-059：主次联系人姓名的加密信封需要容纳原姓名长度及加密开销。
+ALTER TABLE merchants
+    ALTER COLUMN contact_name TYPE TEXT,
+    ALTER COLUMN secondary_contact_name TYPE TEXT;
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
index 563eca72..40f0068a 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.08
+// 文件版本：V0.09
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -33,6 +33,32 @@ class PlatformJdbcIntegrationTests {
     @Autowired private PlatformService platformService;
     @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
 
+
+    @Test
+    void merchantContactNamesAreEncryptedAtRestAndRoundTrip() {
+        String[][] names = {{"张".repeat(120), "李".repeat(120)}, {"王敏", "赵宁"}, {null, ""}};
+        for (String[] pair : names) {
+            repository.saveMerchant(new MerchantRecord(
+                    "mch_m009_name_cipher", "姓名加密测试商户", "other", null, null, null,
+                    pair[0], null, null, null, null, null, pair[1], null, null, null,
+                    "active", Instant.EPOCH, Instant.EPOCH));
+            Map<String, Object> stored = jdbcTemplate.queryForMap(
+                    "SELECT contact_name, secondary_contact_name FROM merchants WHERE id = ?",
+                    "mch_m009_name_cipher");
+            for (int i = 0; i < pair.length; i++) {
+                String ciphertext = (String) stored.get(i == 0 ? "contact_name" : "secondary_contact_name");
+                if (pair[i] == null || pair[i].isEmpty()) {
+                    assertThat(ciphertext).isEqualTo(pair[i]);
+                } else {
+                    assertThat(ciphertext).startsWith("enc:v1:").doesNotContain(pair[i]);
+                }
+            }
+            MerchantRecord loaded = repository.findMerchantById("mch_m009_name_cipher").orElseThrow();
+            assertThat(loaded.contactName()).isEqualTo(pair[0]);
+            assertThat(loaded.secondaryContactName()).isEqualTo(pair[1]);
+        }
+    }
+
     @Test
     void migrationCreatesPlatformMasterDataTablesAndSeedsPermissions() {
         assertThat(regclass("business_entities")).isEqualTo("business_entities");
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
index 394ae9d1..c9315fa5 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.16
+// 文件版本：V0.17
 
 import com.complex.platform.access.AccessActor;
 import com.complex.platform.auth.SessionIssuer;
@@ -74,6 +74,46 @@ class PlatformServiceTests {
         TestSecurityActors.clear();
     }
 
+
+    @Test
+    void merchantViewAuditRecordsOnlyReturnedPageAndQueryContext() {
+        MerchantRecord first = seedMerchant("audit-first");
+        MerchantRecord second = seedMerchant("audit-second");
+        service.createMerchant(new MerchantProfileRequest(
+                "audit-bar", "seed", List.of(), "筛选外商户", "bar", "be_1",
+                null, null, "Jane", "18800000011",
+                null, null, null, null, null, null, null, null, "active"));
+
+        assertThat(service.listMerchants(false, " other ", 2, 1).items())
+                .extracting(MerchantRecord::id).containsExactly(second.id());
+        service.listMerchants(true, null, 0, 0);
+        service.listMerchants(false, "bar", 99, 1);
+
+        List<AuditEvent> views = auditEventRepository.events.stream()
+                .filter(event -> event.eventName().equals("merchant.viewed")).toList();
+        assertThat(views).hasSize(3);
+        assertThat(views.get(0).metadata())
+                .containsEntry("merchantIds", List.of(second.id()))
+                .containsEntry("entry", "/api/v1/platform/merchants")
+                .containsEntry("projectId", "org_1")
+                .containsEntry("includeArchived", false)
+                .containsEntry("businessFormat", "other")
+                .containsEntry("page", 2).containsEntry("pageSize", 1)
+                .containsEntry("count", 1).containsEntry("total", 2L);
+        assertThat(views.get(0).metadata().toString())
+                .doesNotContain(first.id(), "Jane", "18800000011");
+        assertThat(views.get(1).metadata())
+                .containsEntry("includeArchived", true)
+                .containsEntry("businessFormat", "")
+                .containsEntry("page", 1).containsEntry("pageSize", 500)
+                .containsEntry("count", 3).containsEntry("total", 3L);
+        assertThat(views.get(2).metadata())
+                .containsEntry("merchantIds", List.of())
+                .containsEntry("businessFormat", "bar")
+                .containsEntry("page", 99).containsEntry("pageSize", 1)
+                .containsEntry("count", 0).containsEntry("total", 1L);
+    }
+
     @Test
     void createMerchantRequiresFormalMerchantName() {
         assertThatThrownBy(() -> service.createMerchant(new MerchantProfileRequest(
diff --git a/tools/verify-pii-encryption.mjs b/tools/verify-pii-encryption.mjs
index bb5a6785..d5864320 100755
--- a/tools/verify-pii-encryption.mjs
+++ b/tools/verify-pii-encryption.mjs
@@ -1,5 +1,5 @@
 #!/usr/bin/env node
-// 文件版本：V0.01
+// 文件版本：V0.02
 // PII 落库加密门禁(2026-07-05 立,防"buyer_phone 明文"类回归复发;2026-07-27 扩到全部主体)。
 // 口径:登记表列出所有"自然人手机号/PII 必须密文落库"的列 + 其写入文件;
 // 断言每处 addValue("<col>", ...) 都被 piiCipher.encrypt(...) 包裹。明文即 FAIL。
@@ -19,6 +19,8 @@ const REGISTRY = [
   { table: "ticket_orders", column: "buyer_phone", writeFile: "backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingParameterSource.java" },
   { table: "amusement_group_bookings", column: "contact_phone", writeFile: "backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRepository.java" },
   // ── 多主体联系人侧(V339,P0-A/GAP-07)──
+  { table: "merchants", column: "contact_name", writeFile: "backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java" },
+  { table: "merchants", column: "secondary_contact_name", writeFile: "backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java" },
   { table: "merchants", column: "contact_phone", writeFile: "backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java" },
   { table: "merchants", column: "secondary_contact_phone", writeFile: "backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java" },
   { table: "inventory_suppliers", column: "contact_phone", writeFile: "backend/src/main/java/com/complex/platform/inventory/JdbcInventoryRepository.java" },
```

---

## 4. 逐块 ID 归因（闸 3；每一行）

14 个 hunk，全部归因到 findings.md 当前值为「是」的 ID。无第三 ID，无无法归因行。

### 4.1 `JdbcPlatformRepository.java` → 全部 M009-059

**Hunk 1** `@@ -1,5 +1,5 @@` 文件版本 V0.08→V0.09。本文件本 diff 只有 059 的加解密与注释，版本递增归因 059。

| 行 | 归因 |
|---|---|
| `-// 文件版本：V0.08` | 059 |
| `+// 文件版本：V0.09` | 059 |

**Hunk 2** `@@ -31,7 +31,7 @@` 注释从「仅手机号」改为「主次联系人姓名及手机号」。与 encrypt/decrypt 行为同文件同 ID，不是新抽象。

| 行 | 归因 |
|---|---|
| `-    // P0-A/GAP-07：商户联系人手机号 PII 落库加密（V339）。范式同 customers.phone(V188)/ticket_orders.buyer_phone(V247)：` | 059 |
| `+    // 商户主次联系人姓名及手机号 PII 落库加密。范式同 customers.phone(V188)/ticket_orders.buyer_phone(V247)：` | 059 |
| 上下文「仓库层写时 encrypt、读时 decrypt…」未改 | — |

**Hunk 3** `@@ -622,13 +622,13 @@` `merchantParameters` 写路径。graft callers：该方法仅被 `saveMerchant`（INSERT + ON CONFLICT UPDATE）调用，故新建与更新共用加密。

| 行 | 归因 |
|---|---|
| `-                .addValue("contact_name", merchant.contactName())` | 059 |
| `+                .addValue("contact_name", piiCipher.encrypt(merchant.contactName()))` | 059 |
| 上下文 `contact_phone` 仍 `piiCipher.encrypt` | 未改手机号 |
| 上下文 `owner_person` 仍明文 | 059 禁止扩大 |
| `-                .addValue("secondary_contact_name", merchant.secondaryContactName())` | 059 |
| `+                .addValue("secondary_contact_name", piiCipher.encrypt(merchant.secondaryContactName()))` | 059 |
| 上下文 `secondary_contact_phone` 仍 encrypt | 未改手机号 |

**Hunk 4** `@@ -715,13 +715,13 @@` `mapMerchant` 读路径。本模块全部商户 SELECT（含 `listMerchantsPaged` 五参/四参、`findMerchantById`、按主体列表）走此 mapper。

| 行 | 归因 |
|---|---|
| `-                resultSet.getString("contact_name"),` | 059 |
| `+                piiCipher.decrypt(resultSet.getString("contact_name")),` | 059 |
| 上下文 `contact_phone` 仍 decrypt | 未改 |
| 上下文 `owner_person` 仍明文 | 未扩大 |
| `-                resultSet.getString("secondary_contact_name"),` | 059 |
| `+                piiCipher.decrypt(resultSet.getString("secondary_contact_name")),` | 059 |

其它 `UPDATE merchants`（`:193` 软删、`:1210` 存档理由、`:1243` 项目归属）不碰姓名列，本 diff 未改它们。

### 4.2 `PlatformService.java` → 全部 M009-058（仅第2项）

**Hunk 5** 文件版本 V0.11→V0.12。本文件本 diff 只有 058 metadata，版本归因 058。

**Hunk 6** `currentProject()` 只算一次并传入仓储。为 metadata 复用同一 `projectId`，避免二次调用，且给 null→空串提供局部变量。功能必需，不是顺手重构。

| 行 | 归因 |
|---|---|
| `-        PageResponse<MerchantRecord> result = repository.listMerchantsPaged(currentProject(), includeArchived, format, page, size);` | 058 |
| `+        String projectId = currentProject();` | 058 |
| `+        PageResponse<MerchantRecord> result = repository.listMerchantsPaged(projectId, includeArchived, format, page, size);` | 058 |

**Hunk 7** metadata 扩充。事件名/权限码/businessType/`businessId="all"`/`count`/`total` 保留。`Map.of` 9 对（上限 10）。

| 行 | 归因 |
|---|---|
| `-                Map.of("count", result.items().size(), "total", result.total())` | 058 |
| `+                Map.of("count", result.items().size(), "total", result.total(),` | 058 |
| `+                        "merchantIds", result.items().stream().map(MerchantRecord::id).toList(),` | 058 本页 IDs |
| `+                        "entry", "/api/v1/platform/merchants",` | 058 稳定入口 |
| `+                        "projectId", projectId == null ? "" : projectId,` | 058 空串防 NPE |
| `+                        "includeArchived", includeArchived, "businessFormat", format == null ? "" : format,` | 058 |
| `+                        "page", result.page(), "pageSize", result.pageSize())` | 058 实际分页 |

### 4.3 `V431__merchant_contact_names_ciphertext.sql` → 全部 M009-059（新文件 A）

| 行 | 归因 |
|---|---|
| `+-- 文件版本：V0.01` | 059 |
| `+-- M009-059：主次联系人姓名的加密信封需要容纳原姓名长度及加密开销。` | 059 |
| `+ALTER TABLE merchants` | 059 |
| `+    ALTER COLUMN contact_name TYPE TEXT,` | 059 |
| `+    ALTER COLUMN secondary_contact_name TYPE TEXT;` | 059 |

无 DROP、无 DELETE、无 USING 丢数据、无 CHECK、无回填、无其它列。PostgreSQL `varchar(n)`→`text` 为赋值转换，不删行。旧迁移 V005/V042 未改。

### 4.4 `PlatformJdbcIntegrationTests.java` → 全部 M009-059

**Hunk 9** 版本 V0.08→V0.09。

**Hunk 10** 新增 `merchantContactNamesAreEncryptedAtRestAndRoundTrip`（含测试前空行）。同一 id 三次 `saveMerchant` = 1 次 INSERT + 2 次 ON CONFLICT 更新；裸 SQL 断言两列 `enc:v1:` 且不含原文；`findMerchantById` 读回明文；覆盖 120 汉字 / 短姓名 / null / 空串。`@Transactional` 类级回滚。`JdbcTemplate.queryForMap(?, id)` 字段类型为 `JdbcTemplate`，占位符合法。19 参构造把 `pair[0]`/`pair[1]` 绑到 `contactName`/`secondaryContactName`（`MerchantRecord.java:64-74`）。

### 4.5 `PlatformServiceTests.java` → 全部 M009-058

**Hunk 11** 版本 V0.16→V0.17。

**Hunk 12** 新增 `merchantViewAuditRecordsOnlyReturnedPageAndQueryContext`。三次真实 `service.listMerchants`：第 2 页 size=1 的 other 筛选；`0,0` 归一化；bar 第 99 页空页。断言对象 IDs/入口/projectId/筛选/实际 page-pageSize/count-total；`metadata.toString()` 不含其它页 id、姓名 Jane、手机号。内存仓储走接口 default `listMerchantsPaged(String projectId,…)` + `PageResponse.of`（页 `<1`→1，size `<1`→500）。

### 4.6 `tools/verify-pii-encryption.mjs` → 全部 M009-059

**Hunk 13** 版本 V0.01→V0.02。

**Hunk 14** 既有 REGISTRY 仅加两姓名列，不改正则/框架、不新建门禁。mode 仍 100755。

| 行 | 归因 |
|---|---|
| `-// 文件版本：V0.01` / `+// 文件版本：V0.02` | 059 |
| `+  { table: "merchants", column: "contact_name", writeFile: "…JdbcPlatformRepository.java" },` | 059 |
| `+  { table: "merchants", column: "secondary_contact_name", writeFile: "…JdbcPlatformRepository.java" },` | 059 |

### 4.7 归因结论

六路径、14 hunk、全部 +/- 行均可归到 058 或 059。无无法归因行。无顺手优化、无换测试框架、无改 Controller/HTML/锁/PiiCipher/owner_person/legal_representative/其它姓名列。闸 3 **PASS**。

---

## 5. 行为核对（第一性原理 + 顺从 + 对抗；全 diff 审完）

### 5.1 M009-058 第2项

1. **运行路径**：`PlatformController` `@RequestMapping("/api/v1/platform")` + `@GetMapping("/merchants")` → `service.listMerchants` → 唯一一处 `merchant.viewed`。入口字面 `/api/v1/platform/merchants` 与映射一致。Controller 本 diff 未改。
2. **空 projectId 不会 NPE**：`currentProject()` 在 `projectExists(scope)` 为假时返回 **null**（`PlatformService.java:411-415`）。仓储 `addValue("project_id", projectId, Types.VARCHAR)` + `WHERE project_id = :project_id` 对 SQL NULL 得到空页，不抛 Java NPE。`AuditEventService.record` 第 123 行 `Map.copyOf(new LinkedHashMap<>(metadata))` **拒绝 null 值**。本 diff 写 `projectId == null ? "" : projectId`，`businessFormat` 同样空串。无 NPE。
3. **实际 page/pageSize**：metadata 取 `result.page()`/`result.pageSize()`，不是请求原值。Jdbc 五参：`page<1→1`，`size<1→500` 且 `min(size,500)`。内存 default：`PageResponse.of` 同样页/尺寸归一化（内存不加 500 上限，本测试用 0,0 与 99,1，不触及该差异）。测试断言 `0,0`→`page=1,pageSize=500`；`99,1` 空页仍记 99。
4. **只返回本页 IDs、不泄露姓名/手机号**：`merchantIds` 来自 `result.items().stream().map(MerchantRecord::id)`。空页 `[]`。无姓名/电话字段。测试：第 2 页只含 `second.id()`，`doesNotContain(first.id(), "Jane", "18800000011")`。
5. **筛选**：`trimToNull(" other ")`→`"other"` 后写入 metadata；无筛选 `format==null`→`""`。bar 筛选 total=1、本页 0。
6. **未改第1项**：`canViewPhonePlaintext` / `maskPhonesForUnauthorized` 仍在；持 `pii.contact_phone.view` 仍返回原文。Controller 与 ControllerTests 两树 blob 相同。Surefire 仍含 `listingMerchantsReturnsPlaintextPhoneWithPiiPermission`，断言 `18800000011` 明文。

### 5.2 M009-059

1. **加解密双列所有读写**：写只经 `merchantParameters`←`saveMerchant`（INSERT 与 ON CONFLICT UPDATE 的 `EXCLUDED.contact_name`/`secondary_contact_name` 即加密绑定值）。读只经 `mapMerchant`。graft：`merchantParameters` 入边仅 `saveMerchant`。其它 UPDATE 不碰姓名。
2. **既有 PiiCipher 未改**：null/空/无 `enc:v1:` 前缀原样透传（`PiiCipher.java:32-55`）。存量明文可读，**不得声称已全部加密**。未新增 helper/回填/CHECK。
3. **VARCHAR→TEXT 容量**：V005 `contact_name VARCHAR(120)`；V042 `secondary_contact_name VARCHAR(120)`。本席独立计算：120 汉字 UTF-8=360B；IV12+明文+GCM tag16=388B；Base64=520；加前缀 `enc:v1:`(7)=**527 字符** > 120。TEXT 必要。V431 只改这两列。
4. **无数据删除**：`ALTER COLUMN … TYPE TEXT` 无 DROP/DELETE/TRUNCATE。owner_person 仍 V147 `VARCHAR(120)` 明文。
5. **手机号/共享 cipher**：写/读手机号行在 hunk 上下文中未改；`PiiCipher.java` 两树 blob 相同。
6. **门禁**：本席实跑 `node tools/verify-pii-encryption.mjs`，**exit 0**，stdout `PII_ENCRYPTION_PASS: 18 个 PII 列均经 piiCipher.encrypt 落库`。REGISTRY 点算 18 列，含本次两姓名。脚本只 `readFileSync`，无写库/写构建。

### 5.3 对抗式（grilling 自问自答，不等人、不派子代理）

- Q1 是否把 businessId 从 `all` 改成单对象、或新建事件名？否，仍 `merchant`/`all`/`merchant.viewed`。
- Q2 `Map.of` 是否超过 10 对或放入 null？9 对；null 已转空串。
- Q3 是否给姓名加信封 CHECK，导致存量明文 + 不碰姓名的 UPDATE 失败？否，V431 无 CHECK。
- Q4 是否改 Controller 掩码以「顺便」消第1项？否，两树 blob 相同；明文测试仍在且绿。
- Q5 是否加密 owner_person/legal_representative/其它表 contact_name？diff 无名。
- Q6 内存页序与 JDBC `ORDER BY created_at DESC` 不同，是否让 058 测错生产排序？058 测的是「返回页的 metadata」，不测排序；生产 Jdbc 测试未改排序用例。不构成越界或漏修。
- Q7 真实 index 停在 before 是否表示 after 未落地？否：工作区 blob=after，B3 临时索引写树；B4 以两 tree 为准。
- Q8 是否需要 LOCK-AMEND 才能修 059？本席 V0.14 已裁定用已映射 PGM-026/GEN-010/GEN-008 + 操作员「3.应该加密」，本批不扩锁。落地未引用未映射 PGM-059。

未发现越界或未修完的本批队列项。

---

## 6. 测试有效性与 Surefire（自己读 XML，不照抄；本席未跑 mvn）

禁令：不得跑会写代码/数据库/构建产物的命令。本席**未**执行 `mvn test`（会写 `backend/target`）。证据如下。

### 6.1 修复前两新测试会失败（不读 self 意见）

before 树生产：
- `PlatformService.listMerchants` metadata **只有** `count`/`total`（本席 `git show d23497895e200310c4e9692323d5f4e431234981:…/PlatformService.java` 第 151–159 行）。
- `merchantParameters` 两姓名 **直绑明文**；`mapMerchant` **直读明文**。

after 树测试：
- `merchantViewAuditRecordsOnlyReturnedPageAndQueryContext` 断言 `containsEntry("merchantIds", …)` → 对 before 生产必 `AssertionError`。
- `merchantContactNamesAreEncryptedAtRestAndRoundTrip` 断言 `startsWith("enc:v1:")` → 对 before 生产明文必失败。

文件系统时间（本席 `stat`）：两测试文件 mtime **2026-09-21 22:44:33**；生产/SQL/门禁 **22:45:18**；Surefire XML **22:45:31–22:45:32**。测试先于生产约 45 秒，符合 TDD 红→绿顺序。本席不读 self-raw 中的红日志正文。

### 6.2 修复后 Surefire（读 XML 属性与 testcase 名）

路径 `backend/target/surefire-reports/`，本席 Python `xml.etree` 解析：

| 文件 | tests | failures | errors | skipped | 新用例在 XML 中 |
|---|---|---|---|---|---|
| TEST-…PlatformServiceTests.xml | 70 | 0 | 0 | 0 | `merchantViewAuditRecordsOnlyReturnedPageAndQueryContext` time=0.002，无 failure/error/skipped 子元 |
| TEST-…PlatformControllerTests.xml | 14 | 0 | 0 | 0 | `listingMerchantsReturnsPlaintextPhoneWithPiiPermission` 仍在 |
| TEST-…PlatformJdbcIntegrationTests.xml | 24 | 0 | 0 | 0 | `merchantContactNamesAreEncryptedAtRestAndRoundTrip` time=0.004，无 failure/error/skipped 子元 |

合计 **70+14+24=108**，失败/错误/跳过均为 0。对应 `.txt` 头：`Tests run: 70/14/24, Failures: 0, Errors: 0, Skipped: 0`。这是盘上已有报告，不是本席本轮 mvn 复跑。

新测试不是 mock 加密：Service 测走真实 `PlatformService`+内存仓储+`AuditEventService`；JDBC 测走真实 `saveMerchant`/`findMerchantById`+裸 SQL。GEN-003 满足。

### 6.3 门禁与 diff-check

- `node tools/verify-pii-encryption.mjs` 本席实跑 **exit 0**，18 列。
- `git diff --check d23497895e200310c4e9692323d5f4e431234981 4a8843e8fd431fdd9c9639d2ba96013049986c7a` 本席实跑 **exit 0**。

---

## 7. 技能自核（V0.17 §6；B4 收窄）

本机实列目录，缺则记缺口，不冒称已调用。

| 技能 | 路径 | 本轮用法 |
|---|---|---|
| using-superpowers | superpowers 6.3.0 `using-superpowers/SKILL.md` | 开工先核技能；用户专项指令优先（禁第三席/MCP/子代理/fetch/commit/push/worktree/部署） |
| requesting-code-review | 同套件 | B4=本席即审方；**不用**本体 HEAD~1、子代理、无 ID 即刻修；基线改为两 tree id |
| receiving-code-review | 同套件 | 本席写意见不实施；先核实再下结论 |
| verification-before-completion | 同套件 | 锁哈希/两树 diff/工作区 blob/Surefire XML/node 门禁/源码；不把未跑的 mvn 写成已跑 |
| test-driven-development / tdd | superpowers + mattpocock | 只核红绿证据（before 生产 vs after 测试 + mtime）；测缝不问人 |
| systematic-debugging / diagnosing-bugs | 两套件 | 只读：null→Map.copyOf NPE、明文直绑、VARCHAR 容量 |
| grilling | mattpocock `productivity/grilling` | 只用追问树自取事实；作废等用户回答与 dispatch sub-agent |
| codebase-design | mattpocock engineering | 加解密停在已有 merchantParameters/mapMerchant 缝 |
| ponytail-review | ponytail 4.9.0 | 可删清单为空：无多余抽象/新通道/CHECK/回填 |
| writing-for-agents | mattpocock | 本 raw 给下游合取用 |
| i-have-adhd | marketplaces/i-have-adhd | 只借先结论、编号、无寒暄 |
| graft | `~/.claude/skills/graft/SKILL.md` | callers merchantParameters；ask listMerchants；grep 辅证 |
| resolving-merge-conflicts | 存在 | 无合并，未调用 |

B4 不适用：brainstorming/writing-plans（A）、ponytail 修复本体（B3）、executing-plans/worktrees/dispatching-parallel-agents/subagent-driven-development（禁用）。

缺口（不冒称调用）：
- `batch-grill-me`：mattpocock 技能树无此目录
- `ubiquitous-language`：未找到
- `git-guardrails-claude-code`：存在于 mattpocock `skills/misc/`（可选）；本席未改 git，未当强制调用

未调 MCP、未派子代理/第三席、未 fetch/commit/push/merge/worktree/部署，未写业务代码/三 HTML/lock/sidecar/ledger/findings/questions。唯一写入：本指定 grok-raw。

---

## 8. 总结论

| 检查 | 结果 |
|---|---|
| 两 tree 可解析且命令无路径过滤 | PASS |
| 工作区六文件 blob = after | PASS |
| 逐行归因 058 或 059 | PASS |
| 058 第2项：路径/空 projectId 空串/实际分页/本页 IDs/无姓名电话 | PASS |
| 059：双列读写加密、TEXT 容量 527、无删数据、无 CHECK/回填、PiiCipher 未改 | PASS |
| 第1项手机号 Controller 前样 | PASS（未改；冲突仍在） |
| 闸 3 无越界 | PASS |
| 测试先失败能力 + 盘上 Surefire 108=0/0/0 + node 18 列 exit0 + diff --check 0 | PASS |
| 本轮当作模块 PASS | **否**。本票只覆盖 B3-r4 的修复内容。第1项残留。须 B5 整模块双席。 |

**席乙 B4-r4：PASS。**
