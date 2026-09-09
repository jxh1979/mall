CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M011 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M011-b4-r1-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：任何 `M011-b4-r1-self-raw.md`（当前亦不存在）、席甲本次 B4 raw。定稿队列以 findings.md V1.36 顶部「当前 M011 B2-r2 双PASS定稿，5项待修（B3-r1）」为准（派发写「B2-r1 双PASS」及 005「补缺幂等键」为 r1 已作废口径，见观察；本席按 V1.36 顶部 r2 正式定稿审）。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、5 个定稿「是」ID（M011-001/002/003/004/005）是否按最小边界修好、每一行能否归因到上述 ID、是否越界改 HTML/锁/sidecar/ledger 基准/权限种子/迁移/被测 M009 源码/006–007/其它模块。未新造判据/角色，未询问操作员。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值（65 字节）。ledger 顶行同值。criteria-lock 版本仍 V0.11。`LOCK-AMEND-COUNT: 1`。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M011 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台验证／第1段 |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 正式队列「是」（5） | M011-001（冲突）、002、003、004（细化）、005（技术性，仅改名） |
| 正式队列「否」（2） | M011-006（跨模块观察，按 M009-022 缺陷否）、007（登记） |
| B3 | 已执行 1 次（本轮为第 1 次），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `4932ca5273356458d9e11fe26cc0a284236b98b0` |
| after（对象类型 tree，可解析） | `dbe9e28f0b3c7aa3a8cbd044d240ee655247cd8a` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 4932ca5273356458d9e11fe26cc0a284236b98b0 dbe9e28f0b3c7aa3a8cbd044d240ee655247cd8a`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 19791 bytes；SHA256 `b64c84bc0b9559a62337f5c8e4c971eba0e156e8a83d85be5911a7beca486947`；UTF-8；3 文件；135 插入 / 22 删除；23 hunk；name-status 三处 M |
| 与 `/tmp/m011_b3_r1.diff` | `cmp` 全等（本席自算为准，副本仅对照） |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3620 / after 3620；`comm -3` 空（无增删路径） |
| 当前工作区 `git add -A && git write-tree`（独立临时目录索引） | `dbe9e28f0b3c7aa3a8cbd044d240ee655247cd8a`＝after |
| HEAD | `bd445728a6ec7bb36a9e7ff19fe56927410ac61e` = `origin/main`（派发写 242f4872；ledger V2.32 已把检查点合并后的基准改为 bd445728。三测试文件 HEAD blob＝before，工作区 blob＝after） |
| 三测试文件 blob | Controller HEAD/before `7730181444aee857acb6c360d81414196af7705c` after/work `02d9e49d672fbf867cb405b5d2d8576fd2bd2043`；Jdbc HEAD/before `844e6f75f3a3a322e51c3e61d362f76a9da5ea8c` after/work `62a9b7a9de4be34baeb304439eb87ee79bad478e`；Service HEAD/before `148e460e8139610cf38241194382b9d1a169e938` after/work `4f29f79b4ac503503e98f8bc54a9d3b1e78b1ca6` |
| findings.md | 工作区 V1.36（513731 字节，内容 SHA256 `b23e571bbea8a70a82be6b631ec36e3c5a3722fa6836a549b510704c8443f3bd`）；两树 blob 均为 `0f8be4dc5f4e7b83ea064b4e29fd4795ac55609c`（本 diff 未改 findings） |
| ledger.md | 两树 blob 均为 `a5067ee8a28c914b4dc844dcb965b9d2945effb3`（本 diff 未改） |
| 三 HTML 两树 blob | BSM `12334b13f3c27a815c9bbf9eea216403fb0d76d1` / PGM `c2588ca1c65a3f196b39a23f299bd56967b5652e` / BEQR `86a717930f9303d4f131ee2864478baa14450e6d`，完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |
| 全部 `backend/src/main/resources/db/migration` | 两树 ls-tree 全等；V003 权限种子 blob `a13ebe87a1a2e5a8fb160eac6980a4cd4cd84812`；V410 blob 相同 |
| 被测 M009 源码两树 blob | PlatformService / JdbcPlatformRepository / PlatformController / MerchantRecord / PlatformRepository 全部 SAME |

`git diff --stat --binary`：

```
 .../governance/PlatformControllerTests.java        | 69 ++++++++++++++++++++--
 .../governance/PlatformJdbcIntegrationTests.java   | 19 +++---
 .../platform/governance/PlatformServiceTests.java  | 69 +++++++++++++++++++---
 3 files changed, 135 insertions(+), 22 deletions(-)
```

`git diff --numstat`：`63	6	PlatformControllerTests.java`；`10	9	PlatformJdbcIntegrationTests.java`；`62	7	PlatformServiceTests.java`。

工作区相对 HEAD 另有 findings/ledger/M011-b1 双席 raw（B2-r2 台账，已在 before/after 两树且 blob 相同）。那些路径不在本轮 diff 内，不按本轮修复内容审计。本席未改任何源文件，只新建本 raw。验证在 `/tmp/m011_b4r1_grok_backend` 工程副本上跑 mvn（JDBC 连 `complex_test`，类上 `@Transactional` 回滚）；事后 `flyway_schema_history.max(version)` 仍为 410。未改任一 `complex_test*` 的模式/数据，未在主仓跑写模式 mvn。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M011 的集合。本轮只用定稿 001–005 已引用的 ID：BSM-004、BSM-072、BSM-074、BEQR-148、BEQR-155、PGM-026、PGM-027、GEN-002、GEN-003、GEN-005、GEN-008、GEN-011、GEN-016。不 LOCK-AMEND。

B3-r1 边界（与 findings V1.36 顶部一致）：

- **只改本模块 3 个测试文件**。版本：PlatformControllerTests V0.01→V0.02、PlatformJdbcIntegrationTests V0.03→V0.04、PlatformServiceTests V0.03→V0.04。
- **001**：先改 `approveDelete…` 断言（默认列表不含、`listMerchants(true)` 含且 approved）见红，再改内存 fake `deleteMerchant` 为 `withDelete("approved",…)` 留行见绿；注释改软删。
- **002**：JDBC :69-72/:142-143/:164/:201/:393/:415 与 Controller :133/:157/:169 的退役码改 `lease`/`joint_operation`/`active`。保留服务测试把退役码当非法值。不新增库 CHECK。
- **003**：服务层 unarchive 可见性、rejected→none 且仍在已存档列表 + deleteReason（走 `listMerchants(true)`）、控制器一条 archive 权限/幂等用例。不为 archive_reason 造 fake、不给 MerchantRecord 加字段。
- **004**：服务层一条 `asMerchantUser` 负例、控制器一条循环覆盖其余五个治理 GET。
- **005**：`…OutsideFiveValues`→`…OutsideSixValues`、两条绑定用例去掉 Idempotency 字样。**禁止补缺键请求**。
- 不改被测 M009 源码、三份 HTML、锁、迁移、权限种子。

四分类本轮不重开：001 冲突、002/003/004 细化、005 技术性（仅改名，无业务发明）。落地仍停在各格。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary 4932ca5273356458d9e11fe26cc0a284236b98b0 dbe9e28f0b3c7aa3a8cbd044d240ee655247cd8a`

before tree = `4932ca5273356458d9e11fe26cc0a284236b98b0`
after tree = `dbe9e28f0b3c7aa3a8cbd044d240ee655247cd8a`

字节 19791；SHA256 `b64c84bc0b9559a62337f5c8e4c971eba0e156e8a83d85be5911a7beca486947`；UTF-8 完整原文如下。

````diff
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
index 77301814..02d9e49d 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.mockito.ArgumentMatchers.any;
@@ -21,6 +21,7 @@ import com.complex.platform.common.api.GlobalExceptionHandler;
 import com.complex.platform.common.api.PageResponse;
 import com.complex.platform.config.SecurityConfig;
 import com.complex.platform.governance.dto.BusinessEntityBindingRequest;
+import com.complex.platform.governance.dto.MerchantArchiveRequest;
 import com.complex.platform.governance.dto.SpaceRequest;
 import com.complex.platform.governance.dto.StatusChangeRequest;
 import java.math.BigDecimal;
@@ -111,6 +112,7 @@ class PlatformControllerTests {
                 .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
     }
 
+    // M011-002：mock 的合作方式取 HTML 六类之一（BEQR-148 lease），不再把已退役收费模板码当 API 形状。
     @Test
     void listingMerchantsReturnsApiResponsePageShape() throws Exception {
         when(service.listMerchants(eq(false), any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(
@@ -130,7 +132,7 @@ class PlatformControllerTests {
                         "Jack",
                         "18800000013",
                         BigDecimal.valueOf(12.30),
-                        "fixed_rent_plus_commission",
+                        "lease",
                         "active",
                         Instant.parse("2026-06-14T00:00:00Z"),
                         Instant.parse("2026-06-14T00:00:00Z")
@@ -154,7 +156,7 @@ class PlatformControllerTests {
                 .andExpect(jsonPath("$.data.items[0].contact_phone").value("188****0011"))
                 .andExpect(jsonPath("$.data.items[0].secondary_contact_phone").value("188****0013"))
                 .andExpect(jsonPath("$.data.items[0].rent_per_square_meter").value(12.30))
-                .andExpect(jsonPath("$.data.items[0].cooperation_form").value("fixed_rent_plus_commission"))
+                .andExpect(jsonPath("$.data.items[0].cooperation_form").value("lease"))
                 .andExpect(jsonPath("$.data.total").value(1));
     }
 
@@ -166,7 +168,7 @@ class PlatformControllerTests {
                 List.of(new MerchantRecord(
                         "mch_1", "P0 Merchant", "other", "be_1", null, null, "Jane", "18800000011",
                         "Owner Wang", "1F-A", BigDecimal.valueOf(128.50), BigDecimal.valueOf(120.00),
-                        "Jack", "18800000013", BigDecimal.valueOf(12.30), "fixed_rent_plus_commission", "active",
+                        "Jack", "18800000013", BigDecimal.valueOf(12.30), "lease", "active",
                         Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")
                 )), 1, 50, 1));
 
@@ -210,6 +212,44 @@ class PlatformControllerTests {
         verify(authorizationService).requirePermission("governance.space.manage");
     }
 
+    // M011-003：存档/删除 HTTP 面（BSM-004）：存档须 merchant.profile.manage + 幂等键；缺键 400 且不进服务层。
+    @Test
+    void archivingMerchantRequiresManagePermissionAndIdempotencyKey() throws Exception {
+        when(service.archiveMerchant(eq("mch_1"), any(MerchantArchiveRequest.class)))
+                .thenReturn(new MerchantRecord(
+                        "mch_1", "P0 Merchant", "other", "be_1", null, null, "Jane", "18800000011",
+                        "Owner Wang", "1F-A", BigDecimal.valueOf(128.50), BigDecimal.valueOf(120.00),
+                        "Jack", "18800000013", BigDecimal.valueOf(12.30), "lease", "active",
+                        Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")
+                ).withArchive(Instant.parse("2026-06-15T00:00:00Z"), "ops", Instant.parse("2026-06-15T00:00:00Z")));
+
+        mockMvc.perform(post("/api/v1/platform/merchants/mch_1/archive")
+                        .with(user("ops").roles("P0_SMOKE"))
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content("""
+                                {
+                                  "idempotency_key": "archive-1",
+                                  "reason": "退场清理"
+                                }
+                                """))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$.data.id").value("mch_1"))
+                .andExpect(jsonPath("$.data.archived_at").isNotEmpty());
+        verify(authorizationService).requirePermission("merchant.profile.manage");
+
+        mockMvc.perform(post("/api/v1/platform/merchants/mch_1/archive")
+                        .with(user("ops").roles("P0_SMOKE"))
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content("""
+                                {
+                                  "reason": "缺幂等键"
+                                }
+                                """))
+                .andExpect(status().isBadRequest())
+                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
+        verify(service, Mockito.times(1)).archiveMerchant(eq("mch_1"), any(MerchantArchiveRequest.class));
+    }
+
     @Test
     void listBusinessEntityBindingsUsesEntityBindingViewPermission() throws Exception {
         when(service.listBusinessEntityBindings(anyInt(), anyInt())).thenReturn(new PageResponse<>(
@@ -254,8 +294,25 @@ class PlatformControllerTests {
                 .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));
     }
 
+    // M011-004：其余五个治理读端同法 fail-closed（BSM-072/BSM-074/PGM-027）；任一读端退回只 requirePermission 即红。
+    @Test
+    void otherGovernanceReadEndpointsRequirePlatformScope() throws Exception {
+        doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "商户账号无权查看平台级经营分析"))
+                .when(authorizationService)
+                .requirePlatformScope();
+
+        for (String path : List.of("/api/v1/platform/business-entities", "/api/v1/platform/projects",
+                "/api/v1/platform/spaces", "/api/v1/platform/space-types", "/api/v1/platform/venue-business-hours")) {
+            mockMvc.perform(get(path)
+                            .with(user("ops").roles("P0_SMOKE")))
+                    .andExpect(status().isForbidden())
+                    .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));
+        }
+    }
+
     @Test
-    void createBusinessEntityBindingRequiresIdempotencyAndManagePermission() throws Exception {
+    // M011-005：方法名与断言一致——本用例只验证 entity.binding.manage 权限码；缺幂等键的共享闸见 creatingMerchantWithoutIdempotencyKeyReturnsValidationFailed。
+    void createBusinessEntityBindingRequiresManagePermission() throws Exception {
         when(service.createBusinessEntityBinding(any(BusinessEntityBindingRequest.class)))
                 .thenReturn(new BusinessEntityBindingRecord(
                         "beb_created",
@@ -292,7 +349,7 @@ class PlatformControllerTests {
     }
 
     @Test
-    void changeBusinessEntityBindingRequiresIdempotencyAndManagePermission() throws Exception {
+    void changeBusinessEntityBindingRequiresManagePermission() throws Exception {
         when(service.changeBusinessEntityBinding(eq("beb_1"), any(StatusChangeRequest.class)))
                 .thenReturn(new BusinessEntityBindingRecord(
                         "beb_1",
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
index 844e6f75..62a9b7a9 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.03
+// 文件版本：V0.04
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -66,10 +66,10 @@ class PlatformJdbcIntegrationTests {
                 "be_pg", "PG Entity", "management_company", null, null, null, "active", t, t)); // M009-012：主体记录不再带 parent 链；M010-003：operation 已出白名单
         repository.saveMerchant(new MerchantRecord("mch_pg_1", "PG A", "other", "be_pg", "be_pg", "be_pg",
                 "C1", "18800000001", "Owner", "1F-A", new java.math.BigDecimal("10.00"), new java.math.BigDecimal("9.00"),
-                null, null, new java.math.BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
+                null, null, new java.math.BigDecimal("1.00"), "lease", "active", t, t));
         repository.saveMerchant(new MerchantRecord("mch_pg_2", "PG B", "other", "be_pg", "be_pg", "be_pg",
                 "C2", "18800000002", "Owner", "1F-B", new java.math.BigDecimal("10.00"), new java.math.BigDecimal("9.00"),
-                null, null, new java.math.BigDecimal("1.00"), "fixed_rent_plus_commission", "draft",
+                null, null, new java.math.BigDecimal("1.00"), "lease", "active",
                 t.plusSeconds(1), t.plusSeconds(1)));
         var page1 = repository.listMerchantsPaged(true, null, 1, 1);
         var page2 = repository.listMerchantsPaged(true, null, 2, 1);
@@ -120,6 +120,7 @@ class PlatformJdbcIntegrationTests {
                 ));
     }
 
+    // M011-002：夹具只用服务层白名单内的合作方式/商户状态（BEQR-148 六类、BEQR-155），更新路径 lease→joint_operation 证明列可改。
     @Test
     void createUpdateAndListMerchantPersistsFormalFields() {
         Instant createdAt = Instant.parse("2026-06-14T02:00:00Z");
@@ -139,8 +140,8 @@ class PlatformJdbcIntegrationTests {
                 "Jack Contact",
                 "18800000013",
                 new BigDecimal("12.30"),
-                "fixed_rent_plus_commission",
-                "draft",
+                "lease",
+                "active",
                 createdAt,
                 createdAt
         ));
@@ -161,7 +162,7 @@ class PlatformJdbcIntegrationTests {
                 "Jill Contact",
                 "18800000014",
                 new BigDecimal("15.80"),
-                "revenue_share",
+                "joint_operation",
                 "active",
                 createdAt,
                 Instant.parse("2026-06-14T03:00:00Z")
@@ -198,7 +199,7 @@ class PlatformJdbcIntegrationTests {
                         "Jill Contact",
                         "18800000014",
                         new BigDecimal("15.80"),
-                        "revenue_share",
+                        "joint_operation",
                         "active"
                 ));
     }
@@ -390,7 +391,7 @@ class PlatformJdbcIntegrationTests {
                 "be_pii", "PII Entity", "management_company", null, null, null, "active", t, t));
         repository.saveMerchant(new MerchantRecord("mch_pii_1", "PII 商户", "other", "be_pii", "be_pii", "be_pii",
                 "第一联系人", "18800000021", "负责人", "1F-P", new BigDecimal("10.00"), new BigDecimal("9.00"),
-                "第二联系人", "18800000022", new BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
+                "第二联系人", "18800000022", new BigDecimal("1.00"), "lease", "active", t, t));
 
         Map<String, Object> raw = jdbcTemplate.queryForMap(
                 "SELECT contact_phone, secondary_contact_phone FROM merchants WHERE id = 'mch_pii_1'");
@@ -412,7 +413,7 @@ class PlatformJdbcIntegrationTests {
                 "be_pii2", "PII Entity 2", "management_company", null, null, null, "active", t, t));
         repository.saveMerchant(new MerchantRecord("mch_pii_2", "PII 商户2", "other", "be_pii2", "be_pii2", "be_pii2",
                 "联系人", "18800000023", "负责人", "1F-Q", new BigDecimal("10.00"), new BigDecimal("9.00"),
-                null, null, new BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
+                null, null, new BigDecimal("1.00"), "lease", "active", t, t));
         repository.saveMerchant(repository.findMerchantById("mch_pii_2").orElseThrow());
 
         assertThat(repository.findMerchantById("mch_pii_2").orElseThrow().contactPhone()).isEqualTo("18800000023");
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
index 148e460e..4f29f79b 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.03
+// 文件版本：V0.04
 
 import com.complex.platform.access.AccessActor;
 import com.complex.platform.auth.SessionIssuer;
@@ -318,9 +318,9 @@ class PlatformServiceTests {
         assertThat(page.pageSize()).isEqualTo(500);
     }
 
-    // V348 商户业态标准字段：五值之外的业态一律拒收（业态错＝通票分账份额拆给错的业态）。
+    // V348/V384 商户业态标准字段：六值（酒吧/餐饮/零售/游乐/传媒/其他）之外的业态一律拒收（业态错＝通票分账份额拆给错的业态）。M011-005
     @Test
-    void createMerchantRejectsBusinessFormatOutsideFiveValues() {
+    void createMerchantRejectsBusinessFormatOutsideSixValues() {
         assertThatThrownBy(() -> service.createMerchant(new MerchantProfileRequest(
                 "bad-format", "seed", List.of(), "业态错的商户", "tenant", "be_1",
                 null, null, "Jane", "18800000011",
@@ -359,7 +359,7 @@ class PlatformServiceTests {
     }
 
     @Test
-    void approveDeleteRemovesArchivedMerchantWhenNoReferences() {
+    void approveDeleteSoftDeletesArchivedMerchantWhenNoReferences() {
         MerchantRecord merchant = seedMerchant("seed-delete");
         service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-2", "测试存档理由"));
         service.requestDeleteMerchant(merchant.id(),
@@ -371,8 +371,58 @@ class PlatformServiceTests {
                 new MerchantDeleteApprovalRequest("delete-approve", "approved", null));
 
         assertThat(decision.deleted()).isTrue();
-        assertThat(service.listMerchants(true).items())
+        // M011-001：审批删除＝软删除标记（BSM-004「不是真从数据库里抹掉…可查、可审计、可恢复」）：
+        // 默认列表不见；已存档列表仍可查且 delete_status=approved——与 JdbcPlatformRepository 同口径。
+        assertThat(service.listMerchants(false).items())
                 .noneMatch(m -> m.id().equals(merchant.id()));
+        assertThat(service.listMerchants(true).items())
+                .anyMatch(m -> m.id().equals(merchant.id()) && "approved".equals(m.deleteStatus()));
+    }
+
+    // M011-003：BSM-004「想恢复点“取消存档”即可」——存档后默认列表不可见，取消存档后默认列表重新可见、待审批删除回退 none。
+    @Test
+    void unarchiveMerchantRestoresDefaultListVisibility() {
+        MerchantRecord merchant = seedMerchant("seed-unarchive");
+        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-u1", "测试存档理由"));
+        assertThat(service.listMerchants(false).items()).noneMatch(m -> m.id().equals(merchant.id()));
+
+        service.unarchiveMerchant(merchant.id(), new MerchantArchiveRequest("unarchive-u1", null));
+
+        assertThat(service.listMerchants(false).items())
+                .anyMatch(m -> m.id().equals(merchant.id()) && m.archivedAt() == null && "none".equals(m.deleteStatus()));
+    }
+
+    // M011-003：驳回删除申请——商户回到 none 态、仍在已存档列表可查（BSM-004「可恢复」）；删除理由随申请落到记录（BSM-004「存档及删除都需要填写理由」）。
+    @Test
+    void rejectingDeleteRequestKeepsMerchantAndResetsDeleteStatus() {
+        MerchantRecord merchant = seedMerchant("seed-reject");
+        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-r1", "测试存档理由"));
+        service.requestDeleteMerchant(merchant.id(), new MerchantDeleteRequest("delete-req-r1", "录入错误，申请删除"));
+        assertThat(service.listMerchants(true).items())
+                .anyMatch(m -> m.id().equals(merchant.id()) && "pending".equals(m.deleteStatus())
+                        && "录入错误，申请删除".equals(m.deleteReason()));
+
+        asMasterAuthority();
+        MerchantDeleteDecision decision = service.approveDeleteMerchant(merchant.id(),
+                new MerchantDeleteApprovalRequest("delete-reject-r1", "rejected", null));
+
+        assertThat(decision.deleted()).isFalse();
+        assertThat(decision.decision()).isEqualTo("rejected");
+        assertThat(service.listMerchants(true).items())
+                .anyMatch(m -> m.id().equals(merchant.id()) && "none".equals(m.deleteStatus()) && m.archivedAt() != null);
+    }
+
+    // M011-004：BSM-072/074「商户不能看平台全局和其他商户数据」——商户账号打治理写路径在服务层首行即被
+    // requirePlatformScope 拒绝（F-AUTHZ-02），不依赖控制器。
+    @Test
+    void merchantActorIsRejectedByPlatformScopeOnGovernanceWrite() {
+        TestSecurityActors.asMerchantUser("mch_scope_1");
+        assertThatThrownBy(() -> seedMerchant("scope-write"))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
+        assertThatThrownBy(() -> service.archiveMerchant("mch_any", new MerchantArchiveRequest("archive-scope", "理由")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
     }
 
     @Test
@@ -703,7 +753,7 @@ class PlatformServiceTests {
             return new ArrayList<>(businessEntities.values());
         }
 
-        // V260 测试用：模拟商户被核心运营表引用的活条数（>0 拒硬删）。
+        // V260 测试用：模拟商户被核心运营表引用的活条数（>0 拒绝审批删除；删除本身为软删除标记，M011-001）。
         private long merchantReferenceCount;
 
         @Override
@@ -719,7 +769,12 @@ class PlatformServiceTests {
 
         @Override
         public void deleteMerchant(String merchantId) {
-            merchants.remove(merchantId);
+            // M011-001：与 JdbcPlatformRepository.deleteMerchant 同语义——软删除标记，行保留可查可恢复（BSM-004）。
+            MerchantRecord merchant = merchants.get(merchantId);
+            if (merchant != null) {
+                merchants.put(merchantId, merchant.withDelete("approved", merchant.deleteRequestedBy(),
+                        merchant.deleteRequestedAt(), merchant.deleteReason(), Instant.now()));
+            }
         }
 
         @Override

````

---

## 4. §5 闸3 逐行归因（23 hunk / 全部命中）

每一变更行都能指到队列为「是」的已定稿 ID。版本头按操作员版本铁律随该文件所属 ID。无无 ID 行。

### 4.1 PlatformControllerTests.java（V0.01→V0.02；9 hunk）

| hunk | 变更 | 归因 |
|---|---|---|
| `@@ -1,5` | 文件版本 V0.01→V0.02 | 002/003/004/005（本文件承载这四项；版本铁律） |
| `@@ -21,6` | `import MerchantArchiveRequest` | 003（archive HTTP 用例需要该 DTO） |
| `@@ -111,6` | 注释「mock 的合作方式取 HTML 六类之一」 | 002 |
| `@@ -130,7` | 夹具 `fixed_rent_plus_commission`→`lease` | 002（BEQR-148；位置 :133） |
| `@@ -154,7` | JSON 断言值随改为 `lease` | 002（:157） |
| `@@ -166,7` | PII 明文用例 mock 同改 `lease` | 002（:169） |
| `@@ -210,6 +212,44` | 新增 `archivingMerchantRequiresManagePermissionAndIdempotencyKey` | 003（控制器一条 archive 权限/幂等；缺键 400 且 `verify(service, times(1))`）。用例内 `cooperation_form=lease` 取值受 002 白名单约束，不是无 ID |
| `@@ -254,8 +294,25` | 新增 `otherGovernanceReadEndpointsRequirePlatformScope`（五路径循环）；同 hunk 把 `createBusinessEntityBindingRequiresIdempotencyAndManagePermission` 改名并加注释 | **004**＝新增循环负例（`/business-entities` `/projects` `/spaces` `/space-types` `/venue-business-hours`）；**005**＝方法名去掉 Idempotency、注释指向共享闸 `creatingMerchantWithoutIdempotencyKeyReturnsValidationFailed` |
| `@@ -292,7 +349,7` | `changeBusinessEntityBindingRequiresIdempotencyAndManagePermission`→`RequiresManagePermission` | 005 |

### 4.2 PlatformJdbcIntegrationTests.java（V0.03→V0.04；8 hunk）

全部 002。本文件本批无 001/003/004/005 行。

| hunk | 变更 | 归因 |
|---|---|---|
| `@@ -1,5` | 文件版本 V0.03→V0.04 | 002 版本铁律 |
| `@@ -66,10` | 分页夹具两处 `fixed_rent_plus_commission`/`draft`→`lease`/`active` | 002（:69-72） |
| `@@ -120,6` | 注释「夹具只用服务层白名单…lease→joint_operation」 | 002 |
| `@@ -139,8` | 创建夹具 `fixed_rent_plus_commission`/`draft`→`lease`/`active` | 002（:142-143） |
| `@@ -161,7` | 更新夹具 `revenue_share`→`joint_operation` | 002（:164） |
| `@@ -198,7` | 断言元组 `revenue_share`→`joint_operation` | 002（:201） |
| `@@ -390,7` | PII 密文用例夹具改 `lease`/`active` | 002（:393） |
| `@@ -412,7` | 二次加密用例夹具改 `lease`/`active` | 002（:415） |

`:430` 手写 SQL `status='draft'` 不在本 diff、不在 002 位置清单（该 INSERT 预期被 `chk_merchants_contact_phone_pii` 拒绝，永不落库）。见观察。

### 4.3 PlatformServiceTests.java（V0.03→V0.04；6 hunk）

| hunk | 变更 | 归因 |
|---|---|---|
| `@@ -1,5` | 文件版本 V0.03→V0.04 | 001/003/004/005 版本铁律 |
| `@@ -318,9` | 注释/方法名五值→六值 `createMerchantRejectsBusinessFormatOutsideSixValues` | 005（仅改名；非法值断言未改，仍拒 `tenant`） |
| `@@ -359,7` | `approveDeleteRemovesArchivedMerchantWhenNoReferences`→`approveDeleteSoftDeletesArchivedMerchantWhenNoReferences` | 001（GEN-016 名称与软删口径一致） |
| `@@ -371,8 +371,58` | 001 断言改为 `listMerchants(false)` noneMatch + `listMerchants(true)` anyMatch approved；其后新增 unarchive、rejected+deleteReason、`asMerchantUser` 写路径负例 | **001**＝原用例断言；**003**＝`unarchiveMerchantRestoresDefaultListVisibility` 与 `rejectingDeleteRequestKeepsMerchantAndResetsDeleteStatus`（deleteReason 走 `listMerchants(true)`，换人 `asMasterAuthority()`）；**004**＝`merchantActorIsRejectedByPlatformScopeOnGovernanceWrite`（`seedMerchant`＝`createMerchant`，另打 `archiveMerchant` 同属治理写路径 `requirePlatformScope`） |
| `@@ -703,7 +753,7` | 注释「>0 拒硬删」→「>0 拒绝审批删除；删除本身为软删除标记」 | 001 |
| `@@ -719,7 +769,12` | fake `merchants.remove`→`withDelete("approved", …)` 留行 | 001（与 `JdbcPlatformRepository.deleteMerchant` 的 `UPDATE delete_status='approved'` 同语义；`findMerchantById` 默认走 `listMerchants()` 全量 map，仍命中） |

### 4.4 闸3 结论

23 hunk、135+/22- 全部命中队列「是」的 001–005。无 006/007 行，无锁外新判据，无无 ID 行。越界判定见第 7 节。

---

## 5. 逐 ID 修复内容（是否修好 / 是否最懒 / 有无新缺陷或业务发明）

分类顺序冲突→细化→技术性→添加。本批无添加项。

### M011-001（冲突／是）— PASS

- **修好**：fake 不再 `remove`；审批通过后默认列表不见、已存档列表含且 `deleteStatus=approved`。对照生产 `JdbcPlatformRepository.deleteMerchant`（`UPDATE merchants SET delete_status='approved'`）与默认 `listMerchants(boolean)`（`includeArchived=true` 全量含 approved）。HTML BSM-004「不是真从数据库里抹掉…可查、可审计、可恢复」。
- **最懒**：只改内存 fake + 一条既有用例的断言/方法名/两处注释。不改 M009 源码，不新增恢复 API。
- **无新发明**：未做真 DELETE，未改 `MerchantRecord`。
- **TDD**：V1.36 要求先改断言见红再改 fake 见绿。本席在 `/tmp` 副本把 after 的 fake 拨回 `remove`，单跑 `approveDeleteSoftDeletesArchivedMerchantWhenNoReferences`：**1 failure**，`PlatformServiceTests.java:379`「Expecting any elements of: [] to match given predicate」（与 B3 `/tmp/m011_b3r1_red.log` 同句）。改回软删 fake 后该用例随 GREEN 62/62 通过。

### M011-002（细化／是）— PASS

- **修好**：定稿列出的 JDBC/Controller 夹具位置全部改为 `lease`/`joint_operation`/`active`。Controller JSON 断言随改。服务层 `createMerchantRejectsCooperationFormOutsideSixModes` / `createMerchantRejectsStatusOutsideEnumeratedStates` 仍把 `fixed_rent_plus_commission` / `draft` 当非法值（定稿「保留」）。
- **最懒**：只改夹具字符串与对应断言，不新增 CHECK、不改生产白名单。
- **无新发明**：`lease`/`joint_operation` 是 BEQR-148 六类既有码；`active` 是 BEQR-155 既有态。

### M011-003（细化／是）— PASS

- **修好**：
  - `unarchiveMerchant` 后默认列表重新可见且 `archivedAt==null`、`deleteStatus=none`（命中生产 `unarchiveMerchant`：`withArchive(null).withDelete("none",…)`）。
  - `approveDelete(decision=rejected)` 后 `deleted()==false`、`decision==rejected`、仍在 `listMerchants(true)` 且 `deleteStatus=none`、`archivedAt!=null`；换人 `asMasterAuthority()`。
  - `requestDeleteMerchant` 后 `listMerchants(true)` 上 `deleteReason` 精确等于申请理由（走既有 `withDelete`，未实现 `setMerchantArchiveReason` fake——符合 r2「不为 archive_reason 造 fake」）。
  - 控制器一条 archive：`merchant.profile.manage` + 有键 200 + 缺键 400 `VALIDATION_FAILED` + 服务只进一次（`requireIdempotencyKey` 共享闸）。
- **最懒**：三条服务断言收在两个新用例里（驳回用例兼 deleteReason）；HTTP 只写 archive 一条，不为四条 HTTP 各写一条。
- **无新发明**：未给 `MerchantRecord` 加 `archiveReason` 字段。取消存档 `reason=null` 与 DTO 注释「取消存档可空」及生产 `setMerchantArchiveReason(id, null)` 一致。

### M011-004（细化／是）— PASS

- **修好**：服务层 `TestSecurityActors.asMerchantUser("mch_scope_1")` 打 `createMerchant`（经 `seedMerchant`）与 `archiveMerchant`，均 `PLATFORM_SCOPE_REQUIRED`。生产 `createMerchant`/`archiveMerchant` 首行 `requirePlatformScope()`；`AuthorizationService.requirePlatformScope` 对非空 `merchantId` 抛该码。控制器循环覆盖其余五个治理 GET；生产五入口均先 `requirePlatformScope()`（`PlatformController.java:46/129/145/152/175`）。既有 `/merchants` 负例与 bindings 的 verify 未拆掉。
- **最懒**：一条服务负例、一条参数化 GET 循环，不为每个 verb 各写一条。服务层多打了 `archiveMerchant`（定稿例示 `createMerchant`/`listMerchants`），仍属同一 `requirePlatformScope` 写路径，未新造规则。
- **无新发明**：未改授权模型。

### M011-005（技术性／是／仅改名）— PASS

- **修好**：方法名/注释五值→六值；两条绑定用例去掉 Idempotency 字样，注释写明缺键共享闸已有。**未**补缺键请求——这是 V1.36 r2 的禁止项，不是漏修。
- **最懒**：零行为变更。TDD 以该用例自身已能失败的非法业态断言为准，不新写测试。
- **无业务发明**：不增第七业态。六值集合（bar/catering/retail/amusement/media/other）原已在服务与库 CHECK。

### 未修且不应修

- **006**：跨模块（`createSpaceType` 无显式 `requireIdempotencyKey`），队列否；diff 未改 PlatformController 源码。
- **007**：登记项；diff 未改。

---

## 6. TDD 与独立复跑（不得只信 B3 日志）

B3 自报（`/tmp/m011_b3r1_run.log`）：RED＝patch a 后 Service 31 tests / 1 failure（`approveDeleteSoftDeletes…`）；GREEN＝patch b 后 Jdbc 16 + Service 34 + Controller 12 = 62/62。本席独立核对如下。

### 6.1 本席 GREEN（after，`/tmp/m011_b4r1_grok_backend`，Temurin 21.0.12.1）

命令：`mvn -Dtest=PlatformServiceTests,PlatformControllerTests,PlatformJdbcIntegrationTests test`

只 export 三个 `COMPLEX_DATABASE_*`（从 `~/.complex/complex.env` 抽出这三键，不 source 整文件）；测试类硬编码 `jdbc:postgresql://127.0.0.1:5432/complex_test`。

结果：`Tests run: 62, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；Jdbc 16 / Service 34 / Controller 12。日志路径 `/tmp/m011_b4r1_grok_green.log`。Controller 日志出现 6 次 `PLATFORM_SCOPE_REQUIRED`（既有 `/merchants` 负例 1 + 004 五路径 5）及 `VALIDATION_FAILED` / `idempotency_key is required`（003 HTTP 缺键）。事后只读 `select max(version) from flyway_schema_history` 仍 **410**。

### 6.2 本席 RED 001（after 断言 + 旧 fake `remove`）

单跑 `approveDeleteSoftDeletesArchivedMerchantWhenNoReferences`：**Failures: 1**，行 379，空列表 anyMatch。与 B3 红日志同失败句。证明新断言在旧 fake 上能红，不是永绿。

### 6.3 首跑即绿覆盖类（003 驳回、003 unarchive、004 服务、003 HTTP）

V1.36 未要求 003/004 进红阶段。派发写「RED＝001 断言与 003 取消存档在旧 fake 上 2 条失败」与 B3 日志（1 条）及本席复现均不符：在**旧 fake**上跑 `unarchiveMerchantRestoresDefaultListVisibility` + `rejectingDeleteRequestKeepsMerchantAndResetsDeleteStatus` + `merchantActorIsRejectedByPlatformScopeOnGovernanceWrite`＝**3/3 绿**（取消存档/驳回走 `saveMerchant` 不走 `deleteMerchant`；004 不碰 fake）。故 003 unarchive 不可能成为第二条红。

可失败性（`/tmp` 副本突变生产代码，事后已从主仓还原，主仓源码 hash 未变）：

| 用例 | 突变 | 结果 |
|---|---|---|
| 004 控制器五 GET | 去掉 `listSpaceTypes` 的 `requirePlatformScope` | Status expected 403 but was 200（`otherGovernanceReadEndpointsRequirePlatformScope:308`） |
| 003 HTTP 缺键 | 去掉 `archiveMerchant` 的 `requireIdempotencyKey` | Status expected 400 but was 200（`:248`） |
| 003 unarchive | `unarchiveMerchant` 改为直接 `return merchant` | 默认列表 anyMatch 失败（商户仍隐藏） |

断言指向真实行为：004 打的是控制器里真实的 `requirePlatformScope()`；003 HTTP 打的是 `requirePermission("merchant.profile.manage")` + `ApiRequests.requireIdempotencyKey`；003 服务打的是 `PlatformService.unarchiveMerchant` / `approveDeleteMerchant` 的 rejected 分支 / `requestDeleteMerchant` 写入的 `deleteReason`。200 成功路径的 `archived_at` 来自 mock 返回值，只证明反序列化，权限/缺键才是该 HTTP 用例的可失败核（与定稿「权限/幂等」一致）。

005 无新断言；`createMerchantRejectsBusinessFormatOutsideSixValues` 仍对 `tenant` 期望 `MERCHANT_BUSINESS_FORMAT_INVALID`，自身可失败。

---

## 7. 越界检查

| 禁止项 | 两树 |
|---|---|
| 三份业务 HTML | blob 相同 |
| 判据锁 / sidecar | blob 相同 |
| ledger 基准 | blob 相同（本 diff 未改） |
| 权限种子 V003 | blob `a13ebe87…` 相同 |
| 全部迁移含 V410 | ls-tree 全等 |
| 被测 M009 源码 | PlatformService / Jdbc / Controller / MerchantRecord / PlatformRepository SAME |
| 其它模块测试/前端 | 不在 3 文件 diff 内 |
| 006/007 | 未改 |

本 diff 3 文件恰好等于派发/B2 允许面（`plan/modules.md` M011 FILES 三测试文件）。闸3 越界 **未发生**。

---

## 8. 对抗式最小反例（本 diff 内）

假定修复错误，下列反例均未成立：

- fake 仍 `remove` → 本席 RED 会红，after 已绿。
- 夹具仍写退役码 → after 三文件对 `fixed_rent_plus_commission`/`revenue_share` 为 0；Controller/Jdbc 夹具已无 `draft` 作为成功落库值。
- 005 偷偷补了两条缺键请求 → 两条绑定用例请求体仍带 `idempotency_key`，未增加缺键 POST。
- 为实现 003 给 `MerchantRecord` 加字段或实现 `setMerchantArchiveReason` fake → fake 的 `setMerchantArchiveReason` 仍为空实现。
- 改生产源码「顺便修好」006 → PlatformController 两树相同。
- 版本头未递增 → 三文件均 +1，与 V1.36 指定版本对得上。

ponytail-review（只评不改）：004 服务负例多打 `archiveMerchant` 可再删一行，仍属同一 ID，不构成越界或新缺陷。`Lean already. Ship.` 对 001/002/005/003 HTTP 收口成立。

---

## 9. 发现二分与回退

### 有定稿 ID

无。001–005 均按 V1.36 最小处置落地，无回归、无扩范围。

### 无定稿 ID（不入修复队列；观察）

1. 派发摘要写「B2-r1 双PASS」且 005「两条绑定用例补缺幂等键→400」——这是 V1.35/r1 表已作废单元格。正式定稿是 V1.36 B2-r2：005 仅改名、禁止补缺键。本席按正式定稿审，不把「未补缺键」判成漏修。
2. 派发写 RED 两条失败（含 003 取消存档）；B3 `run.log` 与本席复现均为 **001 一条**。003 unarchive 在旧 fake 上首跑即绿。不构成内容缺陷（覆盖类测试可失败性已用突变证明）。
3. 派发 HEAD=242f4872；检出 `bd445728`（M001–M010 检查点 PR #428）。ledger V2.32 已记。三测试文件指纹仍 before=HEAD、after=工作区，不影响本 diff。
4. JDBC `:430` 明文电话负例 SQL 仍写 `status='draft'`。不在 002 位置清单、不在本 diff、INSERT 预期被 PII CHECK 拒绝。不升格。
5. `PlatformService.listMerchants` 自身无 `requirePlatformScope`（读端守卫在控制器，M009-015）。004 用 `createMerchant` 符合定稿斜杠「或」，避免打一条不会红的服务读路径。不另开 ID。

回退统一规则：无「没有定稿 ID」的缺陷，无「不在修复队列」的改动行 → **不回 B2**。内容与闸3 均 PASS → 待席甲本批 B4 PASS 后合取；未经双 PASS 不进 B5，更不称模块 PASS。

---

## 10. 技能自检（§6 收窄；不派子代理）

| 技能 | 路径 | 本节点用法 |
|---|---|---|
| `verification-before-completion` | `…/superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | 声称前先跑锁哈希、两树、GREEN 62/62、RED 001、三处突变可失败、flyway 410 |
| `receiving-code-review` | `…/superpowers/6.3.0/skills/receiving-code-review/SKILL.md` | 先核源/命令再接受 B3 日志；发现「2 条红」与日志不符后以复现为准 |
| `grilling` | `…/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只借追问结构：软删是否真留行、夹具是否仍退役码、005 是否偷补缺键、是否改了 M009 源码 |
| `tdd` | `…/mattpocock-skills/1.2.3/skills/engineering/tdd/SKILL.md` | 只核红绿与可失败，不落测试 |
| `ponytail-review` | `…/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只评最小，不落地 |
| `systematic-debugging` | `…/superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 「2 条红」根因＝003 unarchive 不走 fake `deleteMerchant` |
| `writing-for-agents` | `…/mattpocock-skills/1.2.3/skills/productivity/writing-for-agents/SKILL.md` | 报告结构 |

`requesting-code-review` 要派同会话子代理 → 第四席，**未用**。`batch-grill-me` 本环境未调用。无 MCP、无子代理、无第三席、未问操作员。

---

## 11. 本席结论

内容修复：**PASS**（001–005 按 V1.36 B2-r2 最小边界落地；TDD 001 红绿独立复现；003/004/HTTP 断言指向真实行为且能失败；无新缺陷、无业务发明）。

范围归因 / §5 闸3：**PASS**（3 文件 23 hunk 135+/22- 逐行命中 001–005；无越界）。

总结论：**PASS**。须席甲本批 B4 PASS 后合取；未经双 PASS 不进 B5，更不称模块 PASS。步骤仍 B4-r1，M011 IN_PROGRESS，B3=1。
