CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M022 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T18:50:16Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M022 B3-r1 整批修复 diff：before tree `1393447c3f19e6293ede77e0714499e83af25f18` → after tree `ffca9860fcfe31222a407f692190fb9a887c9d81`；冻结 diff `/home/james/audit-tmp/m022_b3_r1.diff`（11579 bytes，SHA256 cb80ab95be12ecf881aa31da30a53d9246136adc054b0b25cb93cc7a1d5ab754），全文见 §2。定稿 ID：M022-001…007（findings V2.69，队列「是」）。
- 顺序：Grok 已先派（modules/M022-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐块归因（§5 闸 3）

| 文件 | hunk | ±行数 | 归因 ID |
|---|---|---:|---|
| MerchantPortalJdbcIntegrationTests.java | `@@ -1,4 +1,5 @@` | 1 | 001/002 |
| MerchantPortalJdbcIntegrationTests.java | `@@ -14,10 +15,11 @@ import org.springfra` | 9 | 001/002 |
| MerchantPortalJdbcIntegrationTests.java | `@@ -27,7 +29,7 @@ import org.springframe` | 2 | 001 |
| MerchantPortalJdbcIntegrationTests.java | `@@ -43,23 +45,18 @@ class MerchantPortal` | 23 | 001/002 |
| MerchantPortalJdbcIntegrationTests.java | `@@ -70,7 +67,7 @@ class MerchantPortalJd` | 2 | 001 |
| MerchantPortalServiceTests.java | `@@ -1,5 +1,5 @@` | 2 | 001/002/003/004/005/006/007 |
| MerchantPortalServiceTests.java | `@@ -15,6 +15,7 @@ import static org.mock` | 1 | 007 |
| MerchantPortalServiceTests.java | `@@ -119,6 +120,18 @@ class MerchantPorta` | 12 | 004 |
| MerchantPortalServiceTests.java | `@@ -130,7 +143,11 @@ class MerchantPorta` | 4 | 005 |
| MerchantPortalServiceTests.java | `@@ -141,6 +158,8 @@ class MerchantPortal` | 2 | 006 |
| MerchantPortalServiceTests.java | `@@ -170,7 +189,7 @@ class MerchantPortal` | 2 | 003 |
| MerchantPortalServiceTests.java | `@@ -183,6 +202,20 @@ class MerchantPorta` | 14 | 003 |
| MerchantPortalServiceTests.java | `@@ -247,8 +280,11 @@ class MerchantPorta` | 7 | 007 |

- 2 文件 13 个 hunk 全部落到 001–007（版本标记/import 行归其文件所含 ID）；生产代码零改动（变异在执行器内逐条还原、SHA 核对 ok）；归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java
index 29f2cfac..f69a6734 100644
--- a/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.merchantportal;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 
@@ -14,10 +15,11 @@ import org.springframework.boot.test.context.SpringBootTest;
 import org.springframework.transaction.annotation.Transactional;
 
 /**
- * AUD-005 收尾(#187 复核残留,四域之外):商户专区平台管理员「全量合同/结算」真分页。
- * 坐实平台全量结算 listAllSettlementsPaged = SQL COUNT 取真 total(跨所有商户)+ LIMIT/OFFSET 第二页可达;
- * 商户自 scope 的 listSettlementsByMerchant 仍谓词隔离(只见本商户、无 LIMIT),不受影响。
- * 合同侧平台全量走既有 listContractsPaged(ContractJdbcIntegrationTests 已覆盖 501/多页)。
+ * 商户专区(3.18)读路径的真实库验证:合同仓储的商户维度读取必须按商户谓词隔离——
+ * 「我的合同」listContractsByMerchant(+ listContractSpaceIds)与「我的对账」listSettlementsByMerchant
+ * 都只回本商户的数据(业务地图「商户侧页面…不看平台全局」)。
+ * 商户专区服务层不提供平台全量口径(M020-001:无商户归属账号 fail-closed),故本类不承载平台全量分页用例(M022-001);
+ * 仓储 listAllSettlementsPaged 现无生产调用方,去留归合同域。
  */
 @SpringBootTest(properties = {
         "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
@@ -27,7 +29,7 @@ import org.springframework.transaction.annotation.Transactional;
 @Transactional
 class MerchantPortalJdbcIntegrationTests {
 
-    // 注入公有接口: 运行时是 JdbcContractRepository bean, listAllSettlementsPaged 走 SQL 覆写。
+    // 注入公有接口: 运行时是 JdbcContractRepository bean, 商户维度读取走单 SQL 覆写。
     @Autowired
     private com.complex.platform.contract.ContractRepository repository;
 
@@ -43,23 +45,18 @@ class MerchantPortalJdbcIntegrationTests {
                 null, null, null, "proj_1", at, at);
     }
 
+    // M022-002:「我的合同只见本商户」——真实 SQL 的 merchant_id 谓词(服务层 listMyContracts 的两条读路径)。
     @Test
-    void platformAllSettlementsPagedRealTotalAndSecondPageReachable() {
+    void merchantScopeContractsStayIsolated() {
         Instant now = Instant.now();
-        repository.saveContract(contract("ctr_mp_a", "mch_mp_a", now));
-        repository.saveContract(contract("ctr_mp_b", "mch_mp_b", now.plusSeconds(1)));
-        repository.saveSettlement(settlement("stl_mp_1", "ctr_mp_a", now));
-        repository.saveSettlement(settlement("stl_mp_2", "ctr_mp_b", now.plusSeconds(1)));
+        repository.saveContract(contract("ctr_mp_e", "mch_mp_e", now));
+        repository.saveContract(contract("ctr_mp_f", "mch_mp_f", now));
 
-        var page1 = repository.listAllSettlementsPaged(1, 1);
-        var page2 = repository.listAllSettlementsPaged(2, 1);
-        // 真 total: 跨所有商户 COUNT(*),不是当前页条数。
-        assertThat(page1.total()).isGreaterThanOrEqualTo(2L);
-        assertThat(page1.items()).hasSize(1);
-        assertThat(page2.items()).hasSize(1);
-        // 第二页可达且与首页不重(稳定排序 created_at DESC, id)。
-        assertThat(page2.items()).extracting(ContractSettlementRecord::id)
-                .doesNotContainAnyElementsOf(page1.items().stream().map(ContractSettlementRecord::id).toList());
+        assertThat(repository.listContractsByMerchant("mch_mp_e"))
+                .extracting(ContractRecord::id).containsExactly("ctr_mp_e");
+        assertThat(repository.listContractsByMerchant("mch_mp_f"))
+                .extracting(ContractRecord::id).containsExactly("ctr_mp_f");
+        assertThat(repository.listContractSpaceIds("ctr_mp_e")).as("未绑定铺位的合同回空列表").isEmpty();
     }
 
     @Test
@@ -70,7 +67,7 @@ class MerchantPortalJdbcIntegrationTests {
         repository.saveSettlement(settlement("stl_mp_c", "ctr_mp_c", now));
         repository.saveSettlement(settlement("stl_mp_d", "ctr_mp_d", now));
 
-        // 商户自 scope 仍只见本商户(平台全量真分页不污染隔离口径)。
+        // 「我的对账」只见本商户:结算单经合同 merchant_id 谓词隔离。
         assertThat(repository.listSettlementsByMerchant("mch_mp_c"))
                 .extracting(ContractSettlementRecord::id).containsExactly("stl_mp_c");
     }
diff --git a/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java b/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java
index f6154809..48db84ab 100644
--- a/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.merchantportal;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -15,6 +15,7 @@ import static org.mockito.Mockito.when;
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
+import com.complex.platform.common.api.FieldLabels;
 import com.complex.platform.common.api.PageResponse;
 import com.complex.platform.common.idempotency.IdempotencyService;
 import com.complex.platform.contract.ContractRecord;
@@ -119,6 +120,18 @@ class MerchantPortalServiceTests {
                 "merchant_portal_contract", null, null, null, "MERCHANT_SCOPE_REQUIRED");
         verify(audit).recordFailure("merchant_portal.settlement.view_failed", "merchant.portal.settlement.view",
                 "merchant_portal_settlement", null, null, null, "MERCHANT_SCOPE_REQUIRED");
+
+        // M022-004:异议入口同样 fail-closed(disputeOnce 内的商户 scope 闸),不写库并落 dispute_failed。
+        when(contractRepository.findSettlementById("s1"))
+                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
+        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
+        assertThatThrownBy(() -> service.submitSettlementDispute("s1",
+                new MerchantSettlementDisputeRequest("k0", null, "无归属账号留言")))
+                .isInstanceOfSatisfying(ApiException.class,
+                        e -> assertThat(e.code()).isEqualTo("MERCHANT_SCOPE_REQUIRED"));
+        verify(contractRepository, never()).saveSettlement(any());
+        verify(audit).recordFailure("merchant_portal.settlement.dispute_failed", "merchant.portal.settlement.dispute",
+                "merchant_portal_settlement", "s1", null, "k0", "MERCHANT_SCOPE_REQUIRED");
     }
 
     @Test
@@ -130,7 +143,11 @@ class MerchantPortalServiceTests {
         PageResponse<ContractSettlementRecord> page = service.listMySettlements(1, 500);
 
         assertThat(page.items()).extracting(ContractSettlementRecord::id).containsExactly("s1");
+        assertThat(page.total()).isEqualTo(1L);
         verify(authz).requirePermission("merchant.portal.settlement.view");
+        // M022-005:成功路径落 merchant_portal.settlement.viewed 审计。
+        verify(audit).recordSuccess(eq("merchant_portal.settlement.viewed"), eq("merchant.portal.settlement.view"),
+                eq("merchant_portal_settlement"), eq("mch_a"), isNull(), isNull(), eq(Map.of("count", 1)));
         verify(contractRepository, never()).listAllSettlementsPaged(anyInt(), anyInt());
     }
 
@@ -141,6 +158,8 @@ class MerchantPortalServiceTests {
         ContractSettlementRecord disputed = service.submitSettlementDispute("s1",
                 new MerchantSettlementDisputeRequest("k1", null, "金额与合同约定不符"));
 
+        // M022-006:异议入口先过 merchant.portal.settlement.dispute 权限码。
+        verify(authz).requirePermission("merchant.portal.settlement.dispute");
         assertThat(disputed.status()).isEqualTo("pending_settlement");
         assertThat(disputed.disputeReason()).isEqualTo("金额与合同约定不符");
         ArgumentCaptor<ContractSettlementRecord> captor = ArgumentCaptor.forClass(ContractSettlementRecord.class);
@@ -170,7 +189,7 @@ class MerchantPortalServiceTests {
         assertThat(disputed.status()).isEqualTo("pending_settlement");
     }
 
-    // M020-003:合并后超过 dispute_reason 列宽(500)→ 400 且不写库;恰为 500 允许。
+    // M020-003 / M022-003:新留言单独超列宽、或「既有理由 + 新留言」合并后超列宽(500)→ 400 且不写库;恰为 500 允许。
     @Test
     void disputeReasonBeyondColumnWidthIsRejected() {
         stubOwnOpenSettlement();
@@ -183,6 +202,20 @@ class MerchantPortalServiceTests {
                 });
         verify(contractRepository, never()).saveSettlement(any());
 
+        // M022-003:既有理由 300 字 + 换行 + 新留言 250 字 = 551 > 500;新留言单独并不超限,量的是合并后长度。
+        when(contractRepository.findSettlementById("s1")).thenReturn(Optional.of(
+                settlement("s1", "c1", "pending_settlement", "平".repeat(300), "usr_manager",
+                        Instant.parse("2026-09-01T08:00:00Z"))));
+        assertThatThrownBy(() -> service.submitSettlementDispute("s1",
+                new MerchantSettlementDisputeRequest("k3b", null, "漏".repeat(250))))
+                .isInstanceOfSatisfying(ApiException.class, e -> {
+                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
+                    assertThat(e.getMessage()).contains("500");
+                });
+        verify(contractRepository, never()).saveSettlement(any());
+
+        when(contractRepository.findSettlementById("s1"))
+                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
         assertThat(service.submitSettlementDispute("s1",
                 new MerchantSettlementDisputeRequest("k4", null, "漏算".repeat(250))).disputeReason())
                 .hasSize(500);
@@ -247,8 +280,11 @@ class MerchantPortalServiceTests {
 
         assertThatThrownBy(() -> service.submitSettlementDispute("s1",
                 new MerchantSettlementDisputeRequest("k8", null, "  ")))
-                .isInstanceOfSatisfying(ApiException.class,
-                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
+                .isInstanceOfSatisfying(ApiException.class, e -> {
+                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
+                    // M022-007:服务层机读英文经 GlobalExceptionHandler → FieldLabels.translate 出网,用户看到的是中文。
+                    assertThat(FieldLabels.translate(e.getMessage())).isEqualTo("原因未填写");
+                });
         verify(contractRepository, never()).saveSettlement(any());
         verify(audit).recordFailure(eq("merchant_portal.settlement.dispute_failed"), any(), any(), eq("s1"),
                 isNull(), eq("k8"), eq("VALIDATION_FAILED"));
```

## 3. 内容审计（对照定稿最小处置与被测真实现）

- 001：删 `platformAllSettlementsPagedRealTotalAndSecondPageReachable`（守护的仓储方法无生产调用方，存在理由为假）；类注释改为现行事实（商户维度读取的谓词隔离；商户专区不提供平台全量口径，M020-001；仓储方法去留归合同域）；加版本标记。
- 002：`merchantScopeContractsStayIsolated`——真实库两商户各一份合同，`listContractsByMerchant` 只回本商户、`listContractSpaceIds` 对未绑定合同回空；@Transactional 回滚。变异（谓词加 OR 1=1）→ 红。
- 003：既有理由 300 字 + 新留言 250 字合并 551 > 500 → 400 且 never save；恰 500 允许保留。变异（改量新留言）→ 红。
- 004：无 scope 调异议入口 → MERCHANT_SCOPE_REQUIRED + dispute_failed + never save。变异（scope 闸改 quietly）→ 红。
- 005：结算列表 `recordSuccess("merchant_portal.settlement.viewed", …, "mch_a", …, count=1)` 与 total=1。变异（事件名改坏）→ 红。
- 006：`verify(authz).requirePermission("merchant.portal.settlement.dispute")`。变异（权限码改坏）→ 红。
- 007：`FieldLabels.translate(e.getMessage())` = 「原因未填写」——锁全局处理器出网文案为中文，不改已闭环的 M020 服务文件。变异（文案改为不可译英文）→ 红。
- 证据摘要（/home/james/audit-tmp/m022_b3r1_run.log）：
```
GREEN1_MVN_EXIT=0
com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.txt: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.880 s -- in com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests
com.complex.platform.merchantportal.MerchantPortalServiceTests.txt: Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.485 s -- in com.complex.platform.merchantportal.MerchantPortalServiceTests
MUT_002 target=MerchantPortalJdbcIntegrationTests#merchantScopeContractsStayIsolated mvn_exit=1 result=[Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 2.909 s <<< FAIL] restored_sv=ok restored_jr=ok
MUT_003 target=MerchantPortalServiceTests#disputeReasonBeyondColumnWidthIsRejected mvn_exit=1 result=[Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.430 s <<< FAIL] restored_sv=ok restored_jr=ok
MUT_004 target=MerchantPortalServiceTests#callerWithoutMerchantScopeIsRejectedAndAudited mvn_exit=1 result=[Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.433 s <<< FAIL] restored_sv=ok restored_jr=ok
MUT_005 target=MerchantPortalServiceTests#merchantSeesOnlyOwnSettlements mvn_exit=1 result=[Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.425 s <<< FAIL] restored_sv=ok restored_jr=ok
MUT_006 target=MerchantPortalServiceTests#merchantCanDisputeOwnOpenSettlementWithoutChangingStatus mvn_exit=1 result=[Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.423 s <<< FAIL] restored_sv=ok restored_jr=ok
MUT_007 target=MerchantPortalServiceTests#disputeReasonIsRequired mvn_exit=1 result=[Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.433 s <<< FAIL] restored_sv=ok restored_jr=ok
== GREEN-2: final state, implementation restored
GREEN_MVN_EXIT=0
com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.txt: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.803 s -- in com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests
com.complex.platform.merchantportal.MerchantPortalServiceTests.txt: Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.479 s -- in com.complex.platform.merchantportal.MerchantPortalServiceTests
TEST-com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.xml: tests="2" failures="0" errors="0"
TEST-com.complex.platform.merchantportal.MerchantPortalServiceTests.xml: tests="10" failures="0" errors="0"
platform-all test removed: 0; stale claim removed: 0; contracts isolation case: 1; markers: // 文件版本：V0.01 / // 文件版本：V0.02
production files restored: SV=ok JR=ok
```
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m022_b3r1_run.log）

```
START=2026-09-20T18:48:06Z HEAD=e456ba96 URL=jdbc:postgresql://127.0.0.1:5432/complex_test
check OK
BEFORE_TREE=1393447c3f19e6293ede77e0714499e83af25f18
== apply test changes (2 files of this module)
check OK
wrote MerchantPortalServiceTests.java 091633e8bc500b4676a009ba6048445de2b1a560eb8dae8be99ad49fe7bfc09a
wrote MerchantPortalJdbcIntegrationTests.java 12a961e73af6ede1087e1a8f67609327377f8b15b67046143e6a067746ccddef
== GREEN-1: both classes against the real implementation
GREEN1_MVN_EXIT=0
com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.txt: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.880 s -- in com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests
com.complex.platform.merchantportal.MerchantPortalServiceTests.txt: Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.485 s -- in com.complex.platform.merchantportal.MerchantPortalServiceTests
== RED by mutation: break the implementation, the new assertion must fail, then restore
mutated 002 JdbcContractRepository.java
MUT_002 target=MerchantPortalJdbcIntegrationTests#merchantScopeContractsStayIsolated mvn_exit=1 result=[Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 2.909 s <<< FAIL] restored_sv=ok restored_jr=ok
mutated 003 MerchantPortalService.java
MUT_003 target=MerchantPortalServiceTests#disputeReasonBeyondColumnWidthIsRejected mvn_exit=1 result=[Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.430 s <<< FAIL] restored_sv=ok restored_jr=ok
mutated 004 MerchantPortalService.java
MUT_004 target=MerchantPortalServiceTests#callerWithoutMerchantScopeIsRejectedAndAudited mvn_exit=1 result=[Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.433 s <<< FAIL] restored_sv=ok restored_jr=ok
mutated 005 MerchantPortalService.java
MUT_005 target=MerchantPortalServiceTests#merchantSeesOnlyOwnSettlements mvn_exit=1 result=[Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.425 s <<< FAIL] restored_sv=ok restored_jr=ok
mutated 006 MerchantPortalService.java
MUT_006 target=MerchantPortalServiceTests#merchantCanDisputeOwnOpenSettlementWithoutChangingStatus mvn_exit=1 result=[Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.423 s <<< FAIL] restored_sv=ok restored_jr=ok
mutated 007 MerchantPortalService.java
MUT_007 target=MerchantPortalServiceTests#disputeReasonIsRequired mvn_exit=1 result=[Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.433 s <<< FAIL] restored_sv=ok restored_jr=ok
== GREEN-2: final state, implementation restored
GREEN_MVN_EXIT=0
com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.txt: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.803 s -- in com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests
com.complex.platform.merchantportal.MerchantPortalServiceTests.txt: Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.479 s -- in com.complex.platform.merchantportal.MerchantPortalServiceTests
TEST-com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.xml: tests="2" failures="0" errors="0"
TEST-com.complex.platform.merchantportal.MerchantPortalServiceTests.xml: tests="10" failures="0" errors="0"
== post-fix probes
platform-all test removed: 0; stale claim removed: 0; contracts isolation case: 1; markers: // 文件版本：V0.01 / // 文件版本：V0.02
production files restored: SV=ok JR=ok
AFTER_TREE=ffca9860fcfe31222a407f692190fb9a887c9d81
DIFF bytes=11579 sha256=cb80ab95be12ecf881aa31da30a53d9246136adc054b0b25cb93cc7a1d5ab754
 .../MerchantPortalJdbcIntegrationTests.java        | 37 +++++++++---------
 .../merchantportal/MerchantPortalServiceTests.java | 44 ++++++++++++++++++++--
 2 files changed, 57 insertions(+), 24 deletions(-)
hunks=13 files=2
backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java 74 12a961e73af6ede1087e1a8f67609327377f8b15b67046143e6a067746ccddef
backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java 292 091633e8bc500b4676a009ba6048445de2b1a560eb8dae8be99ad49fe7bfc09a
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-20T18:49:32Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
