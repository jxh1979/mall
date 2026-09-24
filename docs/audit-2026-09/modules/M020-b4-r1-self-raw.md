CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M020 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T11:13:58Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M020 B3-r1 整批修复 diff：before tree `8baac60a74362e8b8279b861e162d0b0f54088cc` → after tree `d063773a67499e1b19fb8d2a46b1ee0324a9acbf`；冻结 diff `/home/james/audit-tmp/m020_b3_r1.diff`（32036 bytes，SHA256 2739f6165931e99a97cbb64488c927bf412bd58c276ff6f0e93f9526325d41c6），全文见 §2。定稿 ID：M020-001…006（findings V2.63，队列「是」）；写入集合：本模块 2 文件 + M022 测试类（跨单元 TDD）。
- 顺序：Grok 已先派（modules/M020-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐块归因（§5 闸 3）

| 文件 | hunk | ±行数 | 归因 ID |
|---|---|---:|---|
| MerchantPortalController.java | `@@ -1,6 +1,7 @@` | 3 | 001/002/003/004/005/006 |
| MerchantPortalController.java | `@@ -47,9 +48,8 @@ public class MerchantP` | 5 | 004 |
| MerchantPortalService.java | `@@ -1,8 +1,10 @@` | 2 | 001/002/003/004/005/006 |
| MerchantPortalService.java | `@@ -11,18 +13,17 @@ import com.complex.p` | 9 | 001/004/005 |
| MerchantPortalService.java | `@@ -30,19 +31,24 @@ public class Merchan` | 7 | 002/003/004 |
| MerchantPortalService.java | `@@ -50,25 +56,30 @@ public class Merchan` | 43 | 001/005 |
| MerchantPortalService.java | `@@ -76,53 +87,74 @@ public class Merchan` | 77 | 001/002/003/004/005/006 |
| MerchantPortalService.java | `@@ -135,4 +167,14 @@ public class Mercha` | 10 | 001/005 |
| MerchantPortalServiceTests.java | `@@ -1,20 +1,29 @@` | 9 | 001/002/003/004/005/006 |
| MerchantPortalServiceTests.java | `@@ -26,18 +35,24 @@ import org.mockito.A` | 12 | 001/002/003/004 |
| MerchantPortalServiceTests.java | `@@ -48,8 +63,21 @@ class MerchantPortalS` | 15 | 001/002 |
| MerchantPortalServiceTests.java | `@@ -58,24 +86,39 @@ class MerchantPortal` | 35 | 001/003/005 |
| MerchantPortalServiceTests.java | `@@ -84,67 +127,130 @@ class MerchantPort` | 101 | 001/002/003/004/005/006 |

- 3 文件 13 个 hunk 全部落到 001–006（版本标记/import 行归其所在文件所含 ID）；未改 MerchantContractView/dto/AuthorizationService/迁移/OpenAPI/三份 HTML/锁；归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java b/backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java
index 38fae082..e2c6a269 100644
--- a/backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java
+++ b/backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java
@@ -1,6 +1,7 @@
 package com.complex.platform.merchantportal;
+// 文件版本：V0.01
 
-import com.complex.platform.common.api.ApiException;
+import com.complex.platform.common.api.ApiRequests;
 import com.complex.platform.common.api.ApiResponse;
 import com.complex.platform.common.api.PageResponse;
 import com.complex.platform.contract.ContractSettlementRecord;
@@ -47,9 +48,8 @@ public class MerchantPortalController {
             @PathVariable("settlement_id") String settlementId,
             @RequestBody @jakarta.validation.Valid MerchantSettlementDisputeRequest request
     ) {
-        if (request == null || request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
-            throw ApiException.badRequest("IDEMPOTENCY_KEY_REQUIRED", "idempotency_key is required");
-        }
+        // M020-004:幂等键校验走共享助手(VALIDATION_FAILED),与全仓其它控制器一致。
+        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
         return ApiResponse.ok(service.submitSettlementDispute(settlementId, request));
     }
 }
diff --git a/backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java b/backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java
index ead8b422..74d2e5ed 100644
--- a/backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java
+++ b/backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java
@@ -1,8 +1,10 @@
 package com.complex.platform.merchantportal;
+// 文件版本：V0.01
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.common.api.PageResponse;
+import com.complex.platform.common.idempotency.IdempotencyService;
 import com.complex.platform.contract.ContractRecord;
 import com.complex.platform.contract.ContractRepository;
 import com.complex.platform.contract.ContractSettlementRecord;
@@ -11,18 +13,17 @@ import com.complex.platform.merchantportal.dto.MerchantSettlementDisputeRequest;
 import java.time.Instant;
 import java.util.List;
 import java.util.Map;
-import java.util.Optional;
 import java.util.Set;
 import org.springframework.stereotype.Service;
-import org.springframework.transaction.annotation.Transactional;
 import static com.complex.platform.common.Strings.trimToNull;
 import static com.complex.platform.common.Strings.requireText;
 
 /**
  * 商户专区一期(3.18 方案A)商户自助层:我的合同 / 我的对账只读 + 结算异议留言。
- * 数据边界铁律:currentMerchantScope——商户账号只见本商户(员工无 merchant 抛 MERCHANT_SCOPE_REQUIRED
- * fail-closed),平台管理员 scope 为空走全量监督口径;资金/审批的平台侧权威流程不变
- * (异议只留言、不改结算状态、不暂停付款义务)。
+ * 数据边界铁律:currentMerchantScope——商户账号只见本商户;无商户归属的账号(含平台管理员)一律
+ * fail-closed 抛 MERCHANT_SCOPE_REQUIRED(红线④:不因平台管理员身份整类关掉商户隔离;平台监督走各域
+ * 平台端点,本前缀不提供全量口径,M020-001);资金/审批的平台侧权威流程不变(异议只留言、不改结算状态、
+ * 不暂停付款义务)。留言同一幂等键只认一次(M020-004);三个入口成功/失败均落审计事件(M020-005)。
  */
 @Service
 public class MerchantPortalService {
@@ -30,19 +31,24 @@ public class MerchantPortalService {
     /** 结算单可提异议的驻留态:开账后未结清且未挂起争议。 */
     private static final Set<String> DISPUTABLE_STATUSES =
             Set.of("pending_settlement", "deposit_offsetting", "arrears_collecting");
+    /** contract_settlements.dispute_reason 列宽(V162);既有理由与商户留言合并后不得超过(M020-003)。 */
+    private static final int DISPUTE_REASON_MAX_LENGTH = 500;
 
     private final ContractRepository contractRepository;
     private final AuthorizationService authorizationService;
     private final AuditEventService auditEventService;
+    private final IdempotencyService idempotencyService;
 
     public MerchantPortalService(
             ContractRepository contractRepository,
             AuthorizationService authorizationService,
-            AuditEventService auditEventService
+            AuditEventService auditEventService,
+            IdempotencyService idempotencyService
     ) {
         this.contractRepository = contractRepository;
         this.authorizationService = authorizationService;
         this.auditEventService = auditEventService;
+        this.idempotencyService = idempotencyService;
     }
 
     public PageResponse<MerchantContractView> listMyContracts() {
@@ -50,25 +56,30 @@ public class MerchantPortalService {
     }
 
     public PageResponse<MerchantContractView> listMyContracts(int page, int size) {
-        authorizationService.requirePermission("contract.view");
-        Optional<String> scope = authorizationService.currentMerchantScope();
-        // 商户自 scope: 本商户查询(无 LIMIT、天然有界)内存切片; 平台全量: 仓储 SQL COUNT + LIMIT/OFFSET 真分页(501+ 可达、total 取真值)。
-        PageResponse<ContractRecord> contracts = scope
-                .map(merchantId -> PageResponse.of(contractRepository.listContractsByMerchant(merchantId), page, size))
-                .orElseGet(() -> contractRepository.listContractsPaged(page, size));
-        List<MerchantContractView> views = contracts.items().stream()
-                .map(contract -> MerchantContractView.of(contract, contractRepository.listContractSpaceIds(contract.id())))
-                .toList();
-        auditEventService.recordSuccess(
-                "merchant_portal.contract.viewed",
-                "contract.view",
-                "merchant_portal_contract",
-                scope.orElse("platform-all"),
-                null,
-                null,
-                Map.of("count", views.size())
-        );
-        return new PageResponse<>(views, contracts.page(), contracts.pageSize(), contracts.total());
+        try {
+            authorizationService.requirePermission("contract.view");
+            String merchantId = requireMerchantScope();
+            // 商户自 scope: 本商户查询(无 LIMIT、天然有界)内存切片。
+            PageResponse<ContractRecord> contracts =
+                    PageResponse.of(contractRepository.listContractsByMerchant(merchantId), page, size);
+            List<MerchantContractView> views = contracts.items().stream()
+                    .map(contract -> MerchantContractView.of(contract, contractRepository.listContractSpaceIds(contract.id())))
+                    .toList();
+            auditEventService.recordSuccess(
+                    "merchant_portal.contract.viewed",
+                    "contract.view",
+                    "merchant_portal_contract",
+                    merchantId,
+                    null,
+                    null,
+                    Map.of("count", views.size())
+            );
+            return new PageResponse<>(views, contracts.page(), contracts.pageSize(), contracts.total());
+        } catch (RuntimeException exception) {
+            auditEventService.recordFailure("merchant_portal.contract.view_failed", "contract.view",
+                    "merchant_portal_contract", null, null, null, codeOf(exception));
+            throw exception;
+        }
     }
 
     public PageResponse<ContractSettlementRecord> listMySettlements() {
@@ -76,53 +87,74 @@ public class MerchantPortalService {
     }
 
     public PageResponse<ContractSettlementRecord> listMySettlements(int page, int size) {
-        authorizationService.requirePermission("merchant.portal.settlement.view");
-        Optional<String> scope = authorizationService.currentMerchantScope();
-        // 商户自 scope: listSettlementsByMerchant(谓词隔离、无 LIMIT)内存切片; 平台全量: SQL COUNT + LIMIT/OFFSET 真分页。
-        PageResponse<ContractSettlementRecord> items = scope
-                .map(merchantId -> PageResponse.of(contractRepository.listSettlementsByMerchant(merchantId), page, size))
-                .orElseGet(() -> contractRepository.listAllSettlementsPaged(page, size));
-        auditEventService.recordSuccess(
-                "merchant_portal.settlement.viewed",
-                "merchant.portal.settlement.view",
-                "merchant_portal_settlement",
-                scope.orElse("platform-all"),
-                null,
-                null,
-                Map.of("count", items.items().size())
-        );
-        return items;
+        try {
+            authorizationService.requirePermission("merchant.portal.settlement.view");
+            String merchantId = requireMerchantScope();
+            // 商户自 scope: listSettlementsByMerchant(谓词隔离、无 LIMIT)内存切片。
+            PageResponse<ContractSettlementRecord> items =
+                    PageResponse.of(contractRepository.listSettlementsByMerchant(merchantId), page, size);
+            auditEventService.recordSuccess(
+                    "merchant_portal.settlement.viewed",
+                    "merchant.portal.settlement.view",
+                    "merchant_portal_settlement",
+                    merchantId,
+                    null,
+                    null,
+                    Map.of("count", items.items().size())
+            );
+            return items;
+        } catch (RuntimeException exception) {
+            auditEventService.recordFailure("merchant_portal.settlement.view_failed", "merchant.portal.settlement.view",
+                    "merchant_portal_settlement", null, null, null, codeOf(exception));
+            throw exception;
+        }
     }
 
-    /** 商户对结算单提异议留言：只记理由，不改状态、不暂停付款。 */
-    @Transactional
+    /** 商户对结算单提异议留言：只记理由，不改状态、不暂停付款；同一幂等键只认一次。 */
     public ContractSettlementRecord submitSettlementDispute(String settlementId, MerchantSettlementDisputeRequest request) {
-        authorizationService.requirePermission("merchant.portal.settlement.dispute");
-        if (request == null) {
-            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
+        try {
+            authorizationService.requirePermission("merchant.portal.settlement.dispute");
+            if (request == null) {
+                throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
+            }
+            String id = requireText(settlementId, "settlement_id");
+            return idempotencyService.execute("merchantPortalSettlementDispute:" + id, request.idempotencyKey(), request,
+                    () -> disputeOnce(id, request), ContractSettlementRecord.class);
+        } catch (RuntimeException exception) {
+            auditEventService.recordFailure("merchant_portal.settlement.dispute_failed", "merchant.portal.settlement.dispute",
+                    "merchant_portal_settlement", trimToNull(settlementId), null,
+                    request == null ? null : request.idempotencyKey(), codeOf(exception));
+            throw exception;
         }
+    }
+
+    private ContractSettlementRecord disputeOnce(String settlementId, MerchantSettlementDisputeRequest request) {
         String reason = trimToNull(request.reason());
         if (reason == null) {
             throw ApiException.badRequest("VALIDATION_FAILED", "reason is required");
         }
-        ContractSettlementRecord settlement = contractRepository
-                .findSettlementById(requireText(settlementId, "settlement_id"))
+        ContractSettlementRecord settlement = contractRepository.findSettlementById(settlementId)
                 .orElseThrow(() -> ApiException.notFound("CONTRACT_SETTLEMENT_NOT_FOUND", "结算单不存在或已被删除"));
         ContractRecord contract = contractRepository.findContractById(settlement.contractId())
                 .orElseThrow(() -> ApiException.notFound("CONTRACT_NOT_FOUND", "合同不存在或已被删除"));
-        Optional<String> scope = authorizationService.currentMerchantScope();
-        if (scope.isPresent() && !scope.get().equals(contract.merchantId())) {
+        if (!requireMerchantScope().equals(contract.merchantId())) {
             throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                     "无权查看其他商户的结算单");
         }
         if (!DISPUTABLE_STATUSES.contains(settlement.status())) {
-            throw ApiException.conflict("SETTLEMENT_NOT_DISPUTABLE",
-                    "结算单当前状态不可发起异议：" + settlement.status());
+            // M020-006:报错不回显英文内部状态码。
+            throw ApiException.conflict("SETTLEMENT_NOT_DISPUTABLE", "结算单当前状态不可发起异议");
+        }
+        // M020-002:不覆盖平台侧既有争议理由,不清空裁决人/裁决时间;M020-003:合并后受列宽约束。
+        String existing = trimToNull(settlement.disputeReason());
+        String merged = existing == null ? reason : existing + "\n" + reason;
+        if (merged.length() > DISPUTE_REASON_MAX_LENGTH) {
+            throw ApiException.badRequest("VALIDATION_FAILED", "异议留言不能超过 500 字");
         }
         Instant now = Instant.now();
         ContractSettlementRecord disputed = contractRepository.saveSettlement(new ContractSettlementRecord(
                 settlement.id(), settlement.contractId(), settlement.triggerType(), settlement.status(),
-                settlement.settlementItems(), reason, null, null,
+                settlement.settlementItems(), merged, settlement.resolvedBy(), settlement.resolvedAt(),
                 settlement.projectId(), settlement.createdAt(), now));
         auditEventService.recordSuccess(
                 "merchant_portal.settlement.disputed",
@@ -135,4 +167,14 @@ public class MerchantPortalService {
         );
         return disputed;
     }
+
+    /** 红线④:无商户归属即拒绝——与 AuthorizationService.currentMerchantScope 真实现同义,不存在平台全量口径。 */
+    private String requireMerchantScope() {
+        return authorizationService.currentMerchantScope()
+                .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行"));
+    }
+
+    private static String codeOf(RuntimeException exception) {
+        return exception instanceof ApiException apiException ? apiException.code() : exception.getClass().getSimpleName();
+    }
 }
diff --git a/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java b/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java
index f967d5c6..f6154809 100644
--- a/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java
@@ -1,20 +1,29 @@
 package com.complex.platform.merchantportal;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.ArgumentMatchers.any;
+import static org.mockito.ArgumentMatchers.anyInt;
+import static org.mockito.ArgumentMatchers.eq;
+import static org.mockito.ArgumentMatchers.isNull;
 import static org.mockito.Mockito.mock;
+import static org.mockito.Mockito.never;
+import static org.mockito.Mockito.times;
 import static org.mockito.Mockito.verify;
 import static org.mockito.Mockito.when;
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.common.api.PageResponse;
+import com.complex.platform.common.idempotency.IdempotencyService;
 import com.complex.platform.contract.ContractRecord;
 import com.complex.platform.contract.ContractRepository;
 import com.complex.platform.contract.ContractSettlementRecord;
 import com.complex.platform.evidence.AuditEventService;
 import com.complex.platform.merchantportal.dto.MerchantSettlementDisputeRequest;
+import com.complex.platform.support.InMemoryIdempotencyRepository;
+import com.fasterxml.jackson.databind.ObjectMapper;
 import java.time.Instant;
 import java.time.LocalDate;
 import java.util.List;
@@ -26,18 +35,24 @@ import org.mockito.ArgumentCaptor;
 
 /**
  * 商户专区一期(3.18 方案A)商户自助层:我的合同/我的对账只读 + 结算异议留言。
- * 铁律:只见本商户(currentMerchantScope 硬闸),跨商户异议 CROSS_MERCHANT_FORBIDDEN,
- * 已结清/已有争议的结算单不可再提异议。
+ * 铁律:只见本商户(currentMerchantScope 硬闸;无商户归属一律 MERCHANT_SCOPE_REQUIRED,不存在平台全量口径),
+ * 跨商户异议 CROSS_MERCHANT_FORBIDDEN,已结清/已挂起争议的结算单不可再提异议;留言不覆盖平台裁决痕迹、
+ * 受列宽约束、同幂等键只认一次;成功/失败均落审计(M020-001…006)。
  */
 class MerchantPortalServiceTests {
 
+    private static final ApiException SCOPE_REQUIRED =
+            ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");
+
     private final ContractRepository contractRepository = mock(ContractRepository.class);
     private final AuthorizationService authz = mock(AuthorizationService.class);
+    private final AuditEventService audit = mock(AuditEventService.class);
     private MerchantPortalService service;
 
     @BeforeEach
     void setUp() {
-        service = new MerchantPortalService(contractRepository, authz, mock(AuditEventService.class));
+        service = new MerchantPortalService(contractRepository, authz, audit,
+                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()));
     }
 
     private static ContractRecord contract(String id, String merchantId) {
@@ -48,8 +63,21 @@ class MerchantPortalServiceTests {
     }
 
     private static ContractSettlementRecord settlement(String id, String contractId, String status) {
+        return settlement(id, contractId, status, null, null, null);
+    }
+
+    private static ContractSettlementRecord settlement(String id, String contractId, String status,
+            String disputeReason, String resolvedBy, Instant resolvedAt) {
         return new ContractSettlementRecord(id, contractId, "move_out", status, List.of(),
-                null, null, null, "project_1", Instant.now(), Instant.now());
+                disputeReason, resolvedBy, resolvedAt, "project_1", Instant.now(), Instant.now());
+    }
+
+    private void stubOwnOpenSettlement() {
+        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
+        when(contractRepository.findSettlementById("s1"))
+                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
+        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
+        when(contractRepository.saveSettlement(any())).thenAnswer(invocation -> invocation.getArgument(0));
     }
 
     @Test
@@ -58,24 +86,39 @@ class MerchantPortalServiceTests {
         when(contractRepository.listContractsByMerchant("mch_a")).thenReturn(List.of(contract("c1", "mch_a")));
         when(contractRepository.listContractSpaceIds("c1")).thenReturn(List.of("space_1"));
 
-        PageResponse<MerchantContractView> page = service.listMyContracts();
+        PageResponse<MerchantContractView> page = service.listMyContracts(1, 500);
 
         assertThat(page.items()).hasSize(1);
         assertThat(page.items().get(0).merchantId()).isEqualTo("mch_a");
         assertThat(page.items().get(0).spaceIds()).containsExactly("space_1");
+        assertThat(page.total()).isEqualTo(1L);
         verify(authz).requirePermission("contract.view");
+        verify(contractRepository, never()).listContractsPaged(anyInt(), anyInt());
+        verify(audit).recordSuccess(eq("merchant_portal.contract.viewed"), eq("contract.view"),
+                eq("merchant_portal_contract"), eq("mch_a"), isNull(), isNull(), eq(Map.of("count", 1)));
     }
 
+    // M020-001/005:无商户归属的账号(含平台管理员)按真实现 fail-closed,不存在平台全量口径,并落失败审计。
     @Test
-    void platformAdminSeesAllContracts() {
-        when(authz.currentMerchantScope()).thenReturn(Optional.empty());
-        when(contractRepository.listContractsPaged(1, 500)).thenReturn(
-                new PageResponse<>(List.of(contract("c1", "mch_a"), contract("c2", "mch_b")), 1, 200, 2L));
-        when(contractRepository.listContractSpaceIds(any())).thenReturn(List.of());
-
-        PageResponse<MerchantContractView> page = service.listMyContracts();
-        assertThat(page.items()).hasSize(2);
-        assertThat(page.total()).isEqualTo(2L);
+    void callerWithoutMerchantScopeIsRejectedAndAudited() {
+        when(authz.currentMerchantScope()).thenThrow(SCOPE_REQUIRED);
+
+        assertThatThrownBy(() -> service.listMyContracts(1, 500))
+                .isInstanceOfSatisfying(ApiException.class,
+                        e -> assertThat(e.code()).isEqualTo("MERCHANT_SCOPE_REQUIRED"));
+        assertThatThrownBy(() -> service.listMySettlements(1, 500))
+                .isInstanceOfSatisfying(ApiException.class,
+                        e -> assertThat(e.code()).isEqualTo("MERCHANT_SCOPE_REQUIRED"));
+
+        verify(contractRepository, never()).listContractsPaged(anyInt(), anyInt());
+        verify(contractRepository, never()).listAllSettlementsPaged(anyInt(), anyInt());
+        verify(contractRepository, never()).listContractsByMerchant(any());
+        verify(contractRepository, never()).listSettlementsByMerchant(any());
+        verify(audit, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
+        verify(audit).recordFailure("merchant_portal.contract.view_failed", "contract.view",
+                "merchant_portal_contract", null, null, null, "MERCHANT_SCOPE_REQUIRED");
+        verify(audit).recordFailure("merchant_portal.settlement.view_failed", "merchant.portal.settlement.view",
+                "merchant_portal_settlement", null, null, null, "MERCHANT_SCOPE_REQUIRED");
     }
 
     @Test
@@ -84,67 +127,130 @@ class MerchantPortalServiceTests {
         when(contractRepository.listSettlementsByMerchant("mch_a"))
                 .thenReturn(List.of(settlement("s1", "c1", "pending_settlement")));
 
-        PageResponse<ContractSettlementRecord> page = service.listMySettlements();
+        PageResponse<ContractSettlementRecord> page = service.listMySettlements(1, 500);
 
         assertThat(page.items()).extracting(ContractSettlementRecord::id).containsExactly("s1");
         verify(authz).requirePermission("merchant.portal.settlement.view");
+        verify(contractRepository, never()).listAllSettlementsPaged(anyInt(), anyInt());
     }
 
     @Test
-    void merchantCanDisputeOwnOpenSettlement() {
-        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
-        when(contractRepository.findSettlementById("s1"))
-                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
-        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
-        when(contractRepository.saveSettlement(any())).thenAnswer(invocation -> invocation.getArgument(0));
+    void merchantCanDisputeOwnOpenSettlementWithoutChangingStatus() {
+        stubOwnOpenSettlement();
 
         ContractSettlementRecord disputed = service.submitSettlementDispute("s1",
                 new MerchantSettlementDisputeRequest("k1", null, "金额与合同约定不符"));
 
         assertThat(disputed.status()).isEqualTo("pending_settlement");
-        assertThat(disputed.disputeReason()).contains("金额与合同约定不符");
+        assertThat(disputed.disputeReason()).isEqualTo("金额与合同约定不符");
         ArgumentCaptor<ContractSettlementRecord> captor = ArgumentCaptor.forClass(ContractSettlementRecord.class);
         verify(contractRepository).saveSettlement(captor.capture());
         assertThat(captor.getValue().status()).isEqualTo("pending_settlement");
+        verify(audit).recordSuccess(eq("merchant_portal.settlement.disputed"), eq("merchant.portal.settlement.dispute"),
+                eq("merchant_portal_settlement"), eq("s1"), isNull(), eq("k1"),
+                eq(Map.of("merchant_id", "mch_a", "reason", "金额与合同约定不符")));
+    }
+
+    // M020-002:平台开账(有争议)→主管裁决→商户留言,不清空裁决人/裁决时间,不覆盖平台理由。
+    @Test
+    void disputeKeepsResolutionTrailAndAppendsToExistingReason() {
+        Instant resolvedAt = Instant.parse("2026-09-01T08:00:00Z");
+        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
+        when(contractRepository.findSettlementById("s1")).thenReturn(Optional.of(
+                settlement("s1", "c1", "pending_settlement", "平台：押金核算存疑", "usr_manager", resolvedAt)));
+        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
+        when(contractRepository.saveSettlement(any())).thenAnswer(invocation -> invocation.getArgument(0));
+
+        ContractSettlementRecord disputed = service.submitSettlementDispute("s1",
+                new MerchantSettlementDisputeRequest("k2", null, "商户：违约金多算"));
+
+        assertThat(disputed.resolvedBy()).isEqualTo("usr_manager");
+        assertThat(disputed.resolvedAt()).isEqualTo(resolvedAt);
+        assertThat(disputed.disputeReason()).isEqualTo("平台：押金核算存疑\n商户：违约金多算");
+        assertThat(disputed.status()).isEqualTo("pending_settlement");
+    }
+
+    // M020-003:合并后超过 dispute_reason 列宽(500)→ 400 且不写库;恰为 500 允许。
+    @Test
+    void disputeReasonBeyondColumnWidthIsRejected() {
+        stubOwnOpenSettlement();
+
+        assertThatThrownBy(() -> service.submitSettlementDispute("s1",
+                new MerchantSettlementDisputeRequest("k3", null, "漏算".repeat(251))))
+                .isInstanceOfSatisfying(ApiException.class, e -> {
+                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
+                    assertThat(e.getMessage()).contains("500");
+                });
+        verify(contractRepository, never()).saveSettlement(any());
+
+        assertThat(service.submitSettlementDispute("s1",
+                new MerchantSettlementDisputeRequest("k4", null, "漏算".repeat(250))).disputeReason())
+                .hasSize(500);
     }
 
+    // M020-004:同一幂等键重复提交只认一次(真 IdempotencyService + 内存仓储)。
     @Test
-    void disputeOnForeignSettlementIsForbidden() {
+    void disputeWithSameIdempotencyKeyIsProcessedOnce() {
+        stubOwnOpenSettlement();
+        MerchantSettlementDisputeRequest request = new MerchantSettlementDisputeRequest("k5", null, "少算一期租金");
+
+        ContractSettlementRecord first = service.submitSettlementDispute("s1", request);
+        ContractSettlementRecord replay = service.submitSettlementDispute("s1", request);
+
+        assertThat(replay.id()).isEqualTo(first.id());
+        assertThat(replay.disputeReason()).isEqualTo("少算一期租金");
+        verify(contractRepository, times(1)).saveSettlement(any());
+        verify(audit, times(1)).recordSuccess(eq("merchant_portal.settlement.disputed"), any(), any(), any(), any(), any(), any());
+    }
+
+    // M020-005:跨商户异议被拒、不写库,并落 dispute_failed 失败审计。
+    @Test
+    void disputeOnForeignSettlementIsForbiddenAndAudited() {
         when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_b"));
         when(contractRepository.findSettlementById("s1"))
                 .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
         when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
 
         assertThatThrownBy(() -> service.submitSettlementDispute("s1",
-                new MerchantSettlementDisputeRequest("k1", null, "越权尝试")))
+                new MerchantSettlementDisputeRequest("k6", null, "越权尝试")))
                 .isInstanceOfSatisfying(ApiException.class,
                         e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
+        verify(contractRepository, never()).saveSettlement(any());
+        verify(audit).recordFailure("merchant_portal.settlement.dispute_failed", "merchant.portal.settlement.dispute",
+                "merchant_portal_settlement", "s1", null, "k6", "CROSS_MERCHANT_FORBIDDEN");
     }
 
+    // M020-006:不可异议的报错固定中文,不回显英文内部状态码。
     @Test
     void settledOrAlreadyDisputedSettlementCannotBeDisputed() {
         when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
         when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
-        for (String status : List.of("settled", "disputed")) {
+        int n = 0;
+        for (String status : List.of("settled", "disputed", "voided")) {
+            n++;
+            String key = "k7-" + n;
             when(contractRepository.findSettlementById("s1"))
                     .thenReturn(Optional.of(settlement("s1", "c1", status)));
             assertThatThrownBy(() -> service.submitSettlementDispute("s1",
-                    new MerchantSettlementDisputeRequest("k1", null, "重复异议")))
-                    .isInstanceOfSatisfying(ApiException.class,
-                            e -> assertThat(e.code()).isEqualTo("SETTLEMENT_NOT_DISPUTABLE"));
+                    new MerchantSettlementDisputeRequest(key, null, "重复异议")))
+                    .isInstanceOfSatisfying(ApiException.class, e -> {
+                        assertThat(e.code()).isEqualTo("SETTLEMENT_NOT_DISPUTABLE");
+                        assertThat(e.getMessage()).isEqualTo("结算单当前状态不可发起异议").doesNotContain(status);
+                    });
         }
+        verify(contractRepository, never()).saveSettlement(any());
     }
 
     @Test
     void disputeReasonIsRequired() {
-        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
-        when(contractRepository.findSettlementById("s1"))
-                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
-        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
+        stubOwnOpenSettlement();
 
         assertThatThrownBy(() -> service.submitSettlementDispute("s1",
-                new MerchantSettlementDisputeRequest("k1", null, "  ")))
+                new MerchantSettlementDisputeRequest("k8", null, "  ")))
                 .isInstanceOfSatisfying(ApiException.class,
                         e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
+        verify(contractRepository, never()).saveSettlement(any());
+        verify(audit).recordFailure(eq("merchant_portal.settlement.dispute_failed"), any(), any(), eq("s1"),
+                isNull(), eq("k8"), eq("VALIDATION_FAILED"));
     }
 }
```

## 3. 内容审计（对照定稿最小处置与现行依赖）

- 001：类注释改为无商户账号 fail-closed（红线④）；两处 `orElseGet` 全量分支与 `"platform-all"` 占位删除；`requireMerchantScope()` 以 `orElseThrow(MERCHANT_SCOPE_REQUIRED)` 取商户号——与 `AuthorizationService.currentMerchantScope:425-432` 真实现同义（从不 empty），生产行为不变；测试 `platformAdminSeesAllContracts` 删除，改 `callerWithoutMerchantScopeIsRejectedAndAudited`（桩按真实现抛，断言不触库、不落成功审计）。
- 002：`saveSettlement(...)` 传 `settlement.resolvedBy()/resolvedAt()`；既有 `disputeReason` 非空时 `existing + "\n" + reason` 合并（不覆盖平台理由）；测试 `disputeKeepsResolutionTrailAndAppendsToExistingReason`。
- 003：`DISPUTE_REASON_MAX_LENGTH = 500`（V162 列宽）；合并后超限 → 400 VALIDATION_FAILED 中文文案；测试 251×2 字拒、250×2 字恰 500 允许。
- 004：注入 `IdempotencyService`；`submitSettlementDispute` 主体经 `idempotencyService.execute("merchantPortalSettlementDispute:" + id, key, request, () -> disputeOnce(id, request), ContractSettlementRecord.class)`，`@Transactional` 由 `runAndComplete` 事务模板承接（action+complete 同事务）；控制器改 `ApiRequests.requireIdempotencyKey`（VALIDATION_FAILED，全仓一致）；测试真 `IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper())` 同键两次只写一次。
- 005：三方法 try/catch RuntimeException → `recordFailure(<x-audit-event-failure 事件名>, 权限码, businessType, businessId 或 null, null, 幂等键或 null, codeOf)` 再抛；成功审计 metadata 不变（merchant_id/reason；读路径 count）；测试跨商户落 `dispute_failed`、无 scope 落两条 `view_failed`。
- 006：`SETTLEMENT_NOT_DISPUTABLE` 文案固定「结算单当前状态不可发起异议」；测试对 settled/disputed/voided 断言不含英文状态。
- 未改：`MerchantContractView`、`dto`、无参重载（GEN-001 登记项保留）、审计成功事件名/权限码/businessType。
- TDD 与探针证据（/home/james/audit-tmp/m020_b3r1_run.log 摘要）：
```
RED_MVN_EXIT=1
[ERROR] COMPILATION ERROR : 
GREEN_MVN_EXIT=0
com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.txt: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.002 s -- in com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests
com.complex.platform.merchantportal.MerchantPortalServiceTests.txt: Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.484 s -- in com.complex.platform.merchantportal.MerchantPortalServiceTests
TEST-com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.xml: tests=tests="2" failures=failures="0" errors=errors="0"
TEST-com.complex.platform.merchantportal.MerchantPortalServiceTests.xml: tests=tests="10" failures=failures="0" errors=errors="0"
POST_PROBE_EXIT=0
orElseGet/platform-all remaining: 0; recordFailure sites: 3; idempotencyService.execute: 1; english status suffix: 0; requireIdempotencyKey: 1; IDEMPOTENCY_KEY_REQUIRED: 0; resolvedBy carried: 1; stub Optional.empty: 0
== english operator text residue in module main (expect only handler-translated is required)
```
- 改后只读探针摘要（/home/james/audit-tmp/m020_b3r1_post.log）：
```
service scope call lines: 173 ; orElseGet/platform-all lines: 
class doc L23-24:  * 数据边界铁律:currentMerchantScope——商户账号只见本商户;无商户归属的账号(含平台管理员)一律  * fail-closed 抛 MERCHANT_SCOPE_REQUIRED(红线④:不因平台管理员身份整类关掉商户隔离;平台监督走各域 
test stub: 
service length() checks: 1; handler DataIntegrity: 151:     * 不拦父类 DataIntegrityViolationException——NOT NULL/CHECK/FK 违例多为服务端 bug,仍应落 500 +
services calling idempotencyService.execute: 32; portal: 5
controllers using ApiRequests.requireIdempotencyKey: 33; portal inline: 
services with recordFailure/withFailureAudit: 48; ContractService: 0; portal: 3; openapi x-audit-event-failure ops: 732
```
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m020_b3r1_run.log）

```
START=2026-09-20T11:12:49Z HEAD=e456ba96 URL=jdbc:postgresql://127.0.0.1:5432/complex_test
check OK 879e424bcec16771c44b1c4da01f3b9c6343312fdf75814d05c1e6b078dfa25d
BEFORE_TREE=8baac60a74362e8b8279b861e162d0b0f54088cc
== RED: new tests (M022 file) against old implementation
wrote backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java 14178 ab967aa91746a29445f6928f648bc3cacfdef9a683f294c2890c7b812c52c159
RED_MVN_EXIT=1
[ERROR] COMPILATION ERROR : 
[ERROR] /home/james/complex-management/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java:[54,19] constructor MerchantPortalService in class com.complex.platform.merchantportal.Merc
[ERROR] /home/james/complex-management/backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java:[54,19] constructor MerchantPortalService in class com.complex.platform.merchantportal.Merc
== GREEN: implementation + tests (unit + JDBC integration on complex_test)
wrote backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java 9761 64209fc525268e0b5e1cc8c7c1d977f76b4cd1a94df5f6d215ecdf9eafcdbac0
wrote backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java 2587 536105598b9c5f4c6c16e002d791a05196f2209fec8010a37d18504834ca2dda
GREEN_MVN_EXIT=0
com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.txt: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.002 s -- in com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests
com.complex.platform.merchantportal.MerchantPortalServiceTests.txt: Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.484 s -- in com.complex.platform.merchantportal.MerchantPortalServiceTests
TEST-com.complex.platform.merchantportal.MerchantPortalJdbcIntegrationTests.xml: tests=tests="2" failures=failures="0" errors=errors="0"
TEST-com.complex.platform.merchantportal.MerchantPortalServiceTests.xml: tests=tests="10" failures=failures="0" errors=errors="0"
== post-fix probes
POST_PROBE_EXIT=0
service scope call lines: 173 ; orElseGet/platform-all lines: 
class doc L23-24:  * 数据边界铁律:currentMerchantScope——商户账号只见本商户;无商户归属的账号(含平台管理员)一律  * fail-closed 抛 MERCHANT_SCOPE_REQUIRED(红线④:不因平台管理员�
test stub: 
service length() checks: 1; handler DataIntegrity: 151:     * 不拦父类 DataIntegrityViolationException——NOT NULL/CHECK/FK 违例多为服务端 bug,仍应落 500 +
services calling idempotencyService.execute: 32; portal: 5
controllers using ApiRequests.requireIdempotencyKey: 33; portal inline: 
services with recordFailure/withFailureAudit: 48; ContractService: 0; portal: 3; openapi x-audit-event-failure ops: 732
orElseGet/platform-all remaining: 0; recordFailure sites: 3; idempotencyService.execute: 1; english status suffix: 0; requireIdempotencyKey: 1; IDEMPOTENCY_KEY_REQUIRED: 0; resolvedBy carried: 1; stub Optional.empty: 0
== english operator text residue in module main (expect only handler-translated is required)
backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java:134:            throw ApiException.badRequest("VALIDATION_FAILED", "reason is required");
AFTER_TREE=d063773a67499e1b19fb8d2a46b1ee0324a9acbf
DIFF bytes=32036 sha256=2739f6165931e99a97cbb64488c927bf412bd58c276ff6f0e93f9526325d41c6
 .../merchantportal/MerchantPortalController.java   |   8 +-
 .../merchantportal/MerchantPortalService.java      | 148 +++++++++++-------
 .../merchantportal/MerchantPortalServiceTests.java | 172 +++++++++++++++++----
 3 files changed, 238 insertions(+), 90 deletions(-)
hunks=13 files=3
backend/src/main/java/com/complex/platform/merchantportal/MerchantContractView.java 40 3ffb0b7d30655784c7a2bf34ed72887c024b4bf4b992ba9c9dc81b9487aad1d8 
backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java 55 536105598b9c5f4c6c16e002d791a05196f2209fec8010a37d18504834ca2dda // 文件版本：V0.01
backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java 180 64209fc525268e0b5e1cc8c7c1d977f76b4cd1a94df5f6d215ecdf9eafcdbac0 // 文件版本：V0.01
backend/src/main/java/com/complex/platform/merchantportal/dto/MerchantSettlementDisputeRequest.java 9 c55d2d3ca6d102b71d5e46e0a7ba48f0b7516aaac639448b8cb17d685a9d2052 
backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java 256 ab967aa91746a29445f6928f648bc3cacfdef9a683f294c2890c7b812c52c159 // 文件版本：V0.01
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
A  backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql
END=2026-09-20T11:13:12Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
