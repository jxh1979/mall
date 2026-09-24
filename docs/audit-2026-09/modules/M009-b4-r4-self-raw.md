CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B4 累计第4次修复完整diff独立复审
版本：V0.01
结论：PASS（本次diff及授权范围；不是模块B5 PASS）。
本席在Grok运行期间独立完成，未读本次Grok raw。授权为第2/3项一次B3，第1项不动。
输入：findings V3.25（B2两席V0.14定稿）；before d23497895e200310c4e9692323d5f4e431234981；after 4a8843e8fd431fdd9c9639d2ba96013049986c7a。
完整git diff --binary两树，无路径筛选；6路径，diff SHA256 6fa14a09544d45a41e6fc14bcc51d47b56f96525295e8869cd427a0bed41a4bb。真实暂存区SHA d9a65108a1e87bd300c299ca7640f832bd5c2feb771f35a7a2413d32a1748331未变。
B3前后实算/sidecar/ledger/精确M009-b1-grok-raw最后哈希行四值全等，见首行实算值，LOCK-AMEND-COUNT=1未改。
## 逐块归因及第一性原理/顺从/对抗检查
1. JdbcPlatformRepository所有4块→M009-059：自身V0.08→V0.09；PII注释准确加入姓名；merchantParameters两姓名encrypt，mapMerchant两姓名decrypt。所有新建/更新共用参数，所有查询共用映射，未改变手机/owner_person/legal_representative。已有cipher具null/empty和信封处理，不新增helper/组件/加密算法。功能必需性来自已批准两姓名加密，否则直写明文仍在；已有API姓名明文语义保持，数据库实际存密文。
2. PlatformService所有3块→M009-058第2项：自身V0.11→V0.12；currentProject只算一次复用查询和metadata；保留事件名/权限码/businessType/businessId=all/count/total，仅补merchantIds/entry/projectId/includeArchived/businessFormat/page/pageSize。IDs只取实际result.items，排除其它页及筛选外商户；空页[]。从result取归一化页，不信原请求参数。projectId为空转空串，避免Map.of/Map.copyOf拒null破坏不存在项目的空列表；format已trim与白名单且空串可写。metadata不含姓名和手机号；无新审计通道。
3. V431全部5行→M009-059：新文件V0.01，仅两列TYPE TEXT，容纳120汉字UTF8加信封开销（7+base64(360+28)=527字符）。不改原历史迁移，无DROP/删除/回填/CHECK/其它列。写前工作区最大430且本地origin/main迁移最大<431；未fetch、未部署。测试库迁移实际成功，两列text。
4. PlatformJdbcIntegrationTests两块→M009-059：自身V0.08→V0.09；一个真实JDBC用例以同id依次新建120字主次姓名、修改主次姓名、修改null/empty。分别检查两列持久化enc:v1密文且不含原文、findById读回准确。@Transactional自动回滚，实际查询测试id残留0。红阶段在真实裸库姓名值不以enc:v1开头失败，绿色修复后通过；不是仅mock断言，也不是读回密文假装姓名。已有测试覆盖全部读取mapper及手机行为，本次不重复造私有测试。
5. PlatformServiceTests两块→M009-058第2项：自身V0.16→V0.17；既有真实service/审计service与内存repo边界验证三次查询：第二页one other，归一化0/0默认页，bar筛选越界空页。逐项检查objectIDs/context/count/total/无明文。红阶段只有count/total缺merchantIds，确认先失败后修复。原有项目隔离/不存在项目测试在70例中仍通过。
6. verify-pii-encryption两块→M009-059：自身V0.01→V0.02；既有登记数组加两姓名列，不新建门禁、不改正则/框架。真实运行18列PASS。摘要里旧记17列并不作为本次事实；实际修复前该脚本16项，本次加2成18，以实跑为准。
所有空行/版本/注释均紧邻本批必要实现/测试，无无ID变更；六路径逐字等于after树，无额外业务变更。
## 真实验证
修复前命令（backend）：mvn -o -Dtest=PlatformServiceTests#merchantViewAuditRecordsOnlyReturnedPageAndQueryContext,PlatformJdbcIntegrationTests#merchantContactNamesAreEncryptedAtRestAndRoundTrip test -q，EXIT=1；2 tests，2 failures，0 errors，0 skipped。
失败要点：merchantIds缺失；数据库姓名实际120个“张”为明文。
修复后命令：mvn -o -Dtest=PlatformServiceTests,PlatformControllerTests,PlatformJdbcIntegrationTests test -q，EXIT=0。
Surefire：PlatformServiceTests 70 / PlatformControllerTests14 / PlatformJdbcIntegrationTests24；总108，failure/error/skip全0。测试只注入COMPLEX_DATABASE_URL/USERNAME/PASSWORD，complex_test。
node tools/verify-pii-encryption.mjs EXIT0：PII_ENCRYPTION_PASS: 18 个 PII 列均经 piiCipher.encrypt 落库。
git diff --check d23497895e200310c4e9692323d5f4e431234981 4a8843e8fd431fdd9c9639d2ba96013049986c7a EXIT0。
全工作区git diff --check曾EXIT2，均为已有审计raw尾空格，未越界清理旧证据，未把全工作区说成干净。
独立DB核验：current_database=complex_test；V431 success=t；两列text；本次测试商户id COUNT=0。
## 边界及待B5确认
PlatformController与三HTML逐字等于before树。第1项手机号明文豁免仍在、原冲突未修；不得将这次B4 PASS当整模块PASS。
姓名加密覆盖以后新建/保存路径；既有未重存明文不声称已全部加密，遵守不新增历史回填设计。没有新CHECK，避免旧数据被归档/项目更新时拒绝。
复核范围6文件已经完整审完，无新增finding、无归因失败。B3累计4已用尽本次一次授权。下一步仅在Grok也PASS后B5整模块双审；任一FAIL按既有回退/上限，不自行再修。

## 本轮完整binary diff
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

封存：2026-09-22T05:47:41.428257+00:00

## V0.02 B4验证补充：既有迁移顺序门禁
node tools/verify-migration-version-order.mjs EXIT=1，只报V427__m009_project_profiles_and_associations.sql（当前origin/main最大430，V427低于430）。本次V431没有报错，禁止称全工作区发布门禁已全绿。
按门禁相同算法以完整before/after树文件列表对比本地origin/main，bad集合两次均只有V427；该文件两树blob均e485a5d29fea55fa714091485aa26facc8c45160，旧问题，不是本次六文件diff引入。全工作区检查失败如实保留，不对V427改号/改旧迁移，也不据此扩大已获准一次B3。
本次B4 diff归因与正确性结论仍PASS；新增验证不会覆盖原raw。整模块B5及全局发布状态仍未通过。
时间：2026-09-22T05:48:50.207100+00:00
