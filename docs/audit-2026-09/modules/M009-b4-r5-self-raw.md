CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M009 B4-r5 Codex 完整修复差异审计
版本：V0.01

M009 / 471，生命周期IN_PROGRESS，B3累计5。先派Grok，封存前未读取其本次B4报告。仅审本批完整tree差异，不把本票当B5。
依据findingsV3.48：两席B2 selfV0.17/GrokV0.16，26项仅M009-058队列是。操作员批准单次消费追加1批，原累计4→5不清零。HTML获批查看例外及禁止导出打印保持；第2项merchant.viewed、第3项两联系人姓名保存加密保持。

## 完整批次与边界
before 4ddfdc255c4bbf0da9d42837e9b1bd7c0124b6be
after a685f41c8cc2333c5bb937acce746d5258f9eade
git diff --binary完整SHA256 190cf338324bd6be153d5afabe5ee87d948cb53bedfff8f881443f1269db3148
真实主索引SHA256 d9a65108a1e87bd300c299ca7640f832bd5c2feb771f35a7a2413d32a1748331（前后相同）。HEAD e456ba96c84185c8d39f18f548011daac48a6466，无提交推送。before/after以不同临时索引git add -A/write-tree构造，包含tracked/staged/untracked，不过滤。实际差异仅Controller与既有ControllerTests两文件。

## 逐块归因
全部下列块归M009-058：
1. Controller文件版本V0.02→V0.03，随本批行为修复递增。
2. list/create两个入口：保留原view/manage功能码；取得同次授权结果再传掩码函数，原平台范围/幂等键检查及服务调用保留。
3. update/assign：仍manage，原幂等键/目标ID/请求不变；仅同次许可传递。
4. archive/unarchive/delete-request：仍manage；原理由/幂等/服务流程不变；许可同次消费，不改delete-approve。
5. 既有canViewPhonePlaintext改为requireMerchantPermission接收入口原功能权；有效电话权才requirePermissions合并两权，无电话权只requirePermission原功能；局部boolean非用户参数/新开关。不修改全局boolean查询。
6. 两个mask重载只使用该请求内已经成功核验的boolean，不能消费后重查；原withMaskedPhones及分页形状保持；每页一次消费不按行。
7. 测试版本V0.03→V0.04；新增4参数组合（列表/写回×单独电话单次/同组单次）、1消费失败HTTP断言及必要排列空行注释。无无关测试放宽。
无未归因行、无未授权功能/HTML/数据库/回填/角色/权限码/其它模块生产变动。

## 正确性与对抗性复核
全diff逐行已读。原7调用均改至同一个helper，列表view/其余manage不混用；无电话权的用户不被强制要求PII权限。消费成功后boolean是本次动作的许可，重复请求重新鉴权，不能复用前次boolean。已有requirePermissions先核全部后按source+sourceId合并消费；同组两码只消耗一次，永久权及真实master不产生单次消费。显式DENY/失效/无电话权仍false→掩码；消费竞争/权限撤销发生在检查后时requirePermissions失败不会执行服务或返回号码。没有把boolean全局查询变为有副作用，原菜单/其它Controller不受改动。
原角色身份、项目隔离、功能闸、幂等键、service.viewed审计路径保持。生命周期删除审批仍原功能码及真实最高权限，不在7响应范围中。Java局部boolean不进入接口/DB/配置。

## 测试质量与实际验证
先改测试再改生产。RED：mvn -q -Dtest=PlatformControllerTests test，exit1；19个测试，5fail/0error/0skip；2单独单次组合失败于consumed仍false，2同组组合失败于首次号码被mask，消费失败例为预期403实际200；旧14仍通过。
GREEN：mvn -q -Dtest=PlatformControllerTests,PlatformServiceTests,AuthorizationServiceTests test，exit0，19+70+25=114，全部0fail/0error/0skip。
4参数组合使用真实Controller及AuthorizationService，仓储mock模拟真实来源合并消费和consumed过滤；不是mock出授权结果。列表两条同页均必须明文且消费恰一次；第二请求在电话独立单次时必须掩码，同组时功能权限本身也耗尽而拒绝；写回走真实公开archive入口，验证同样语义。service mock仅隔离业务持久化；不据此声称数据库并发已实测。消费拒绝HTTP用例核Controller不会继续调用service；真实AuthorizationService的原CAS失败用例仍通过。没有移除旧隔离、幂等、无权限和持权返回断言。
B4独立复跑原B1隔离Java探针（只/tmp）：javac0/java0，first=18800000011 second=188****0011 consumed=true。原同字节probe B1会java1，现通过，证明症状消除。
git diff --check before after：exit0。
现Controller302行 SHA53d38fcb4910ba0de5acc7418936d3388673b662a0b0a427dced54b5baff0bc6；Tests532行 SHA21e657c73b07a6e974e55f90156c2bbdd1338838729c097dcc67b373b7d57a8e。

## 限制与说明
未跑写库测试/生产库/全后端；没有数据库改变，不宣称全链绿。旧V427顺序/其它模块环境红保留；旧姓名未回填。消费时点沿用原功能授权之前，仍在服务执行前鉴权；未新增成功后补偿/新事务框架。测试未逐个重复七同构入口，全部调用已逐行归因，列表和单记录两公开响应类均验证。
V0.16 B1同组Java报告示例中打印行后出现字面反斜杠n，复制时应换为实际换行；实际执行命令与输出正确，Grok B2独立探针亦复现，不以排版字符串替代实测。
技能TDD/请求审查/完成前验证按V0.17收窄实际采用；只Grok对席，无其它审计代理。

## 完整binary diff
```diff
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformController.java b/backend/src/main/java/com/complex/platform/governance/PlatformController.java
index 52d3f0d2..ad4edc0d 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformController.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformController.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.02
+// 文件版本：V0.03
 import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;
 
 import com.complex.platform.access.AuthorizationService;
@@ -63,16 +63,16 @@ public class PlatformController {
             @org.springframework.web.bind.annotation.RequestParam(name = "merchantType", required = false) String merchantType,
             @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
             @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
-        authorizationService.requirePermission("merchant.profile.view");
+        boolean viewPhonePlaintext = requireMerchantPermission("merchant.profile.view");
         authorizationService.requirePlatformScope();
-        return ApiResponse.ok(maskPhonesForUnauthorized(service.listMerchants(includeArchived, merchantType, page, size)));
+        return ApiResponse.ok(maskPhonesForUnauthorized(service.listMerchants(includeArchived, merchantType, page, size), viewPhonePlaintext));
     }
 
     @PostMapping("/merchants")
     public ApiResponse<MerchantRecord> createMerchant(@RequestBody @jakarta.validation.Valid MerchantProfileRequest request) {
-        authorizationService.requirePermission("merchant.profile.manage");
+        boolean viewPhonePlaintext = requireMerchantPermission("merchant.profile.manage");
         requireIdempotencyKey(request == null ? null : request.idempotencyKey());
-        return ApiResponse.ok(maskPhonesForUnauthorized(service.createMerchant(request)));
+        return ApiResponse.ok(maskPhonesForUnauthorized(service.createMerchant(request), viewPhonePlaintext));
     }
 
     @PutMapping("/merchants/{merchant_id}")
@@ -80,18 +80,18 @@ public class PlatformController {
             @PathVariable("merchant_id") String merchantId,
             @RequestBody @jakarta.validation.Valid MerchantProfileRequest request
     ) {
-        authorizationService.requirePermission("merchant.profile.manage");
+        boolean viewPhonePlaintext = requireMerchantPermission("merchant.profile.manage");
         requireIdempotencyKey(request == null ? null : request.idempotencyKey());
-        return ApiResponse.ok(maskPhonesForUnauthorized(service.updateMerchant(merchantId, request)));
+        return ApiResponse.ok(maskPhonesForUnauthorized(service.updateMerchant(merchantId, request), viewPhonePlaintext));
     }
 
     @PutMapping("/merchants/{merchant_id}/project")
     public ApiResponse<MerchantRecord> assignMerchantProject(
             @PathVariable("merchant_id") String merchantId,
             @RequestBody @jakarta.validation.Valid ProjectAssociationRequest request) {
-        authorizationService.requirePermission("merchant.profile.manage");
+        boolean viewPhonePlaintext = requireMerchantPermission("merchant.profile.manage");
         requireIdempotencyKey(request == null ? null : request.idempotencyKey());
-        return ApiResponse.ok(maskPhonesForUnauthorized(service.assignMerchantProject(merchantId, request)));
+        return ApiResponse.ok(maskPhonesForUnauthorized(service.assignMerchantProject(merchantId, request), viewPhonePlaintext));
     }
 
     @PostMapping("/business-entities/{entity_id}/projects")
@@ -109,27 +109,27 @@ public class PlatformController {
     public ApiResponse<MerchantRecord> archiveMerchant(
             @PathVariable("merchant_id") String merchantId,
             @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.MerchantArchiveRequest request) {
-        authorizationService.requirePermission("merchant.profile.manage");
+        boolean viewPhonePlaintext = requireMerchantPermission("merchant.profile.manage");
         requireIdempotencyKey(request == null ? null : request.idempotencyKey());
-        return ApiResponse.ok(maskPhonesForUnauthorized(service.archiveMerchant(merchantId, request)));
+        return ApiResponse.ok(maskPhonesForUnauthorized(service.archiveMerchant(merchantId, request), viewPhonePlaintext));
     }
 
     @PostMapping("/merchants/{merchant_id}/unarchive")
     public ApiResponse<MerchantRecord> unarchiveMerchant(
             @PathVariable("merchant_id") String merchantId,
             @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.MerchantArchiveRequest request) {
-        authorizationService.requirePermission("merchant.profile.manage");
+        boolean viewPhonePlaintext = requireMerchantPermission("merchant.profile.manage");
         requireIdempotencyKey(request == null ? null : request.idempotencyKey());
-        return ApiResponse.ok(maskPhonesForUnauthorized(service.unarchiveMerchant(merchantId, request)));
+        return ApiResponse.ok(maskPhonesForUnauthorized(service.unarchiveMerchant(merchantId, request), viewPhonePlaintext));
     }
 
     @PostMapping("/merchants/{merchant_id}/delete-request")
     public ApiResponse<MerchantRecord> requestDeleteMerchant(
             @PathVariable("merchant_id") String merchantId,
             @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.MerchantDeleteRequest request) {
-        authorizationService.requirePermission("merchant.profile.manage");
+        boolean viewPhonePlaintext = requireMerchantPermission("merchant.profile.manage");
         requireIdempotencyKey(request == null ? null : request.idempotencyKey());
-        return ApiResponse.ok(maskPhonesForUnauthorized(service.requestDeleteMerchant(merchantId, request)));
+        return ApiResponse.ok(maskPhonesForUnauthorized(service.requestDeleteMerchant(merchantId, request), viewPhonePlaintext));
     }
 
     @PostMapping("/merchants/{merchant_id}/delete-approve")
@@ -278,16 +278,22 @@ public class PlatformController {
     // 脱敏放在 controller（service 外），与 V279 内部结算价同款范式——避免脱敏值污染幂等缓存后
     // 被有权限者重放读到掩码。
 
-    private boolean canViewPhonePlaintext() {
-        return authorizationService.currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION);
+    private boolean requireMerchantPermission(String permissionCode) {
+        boolean viewPhonePlaintext = authorizationService.currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION);
+        if (viewPhonePlaintext) {
+            authorizationService.requirePermissions(permissionCode, PhoneMask.VIEW_PLAINTEXT_PERMISSION);
+        } else {
+            authorizationService.requirePermission(permissionCode);
+        }
+        return viewPhonePlaintext;
     }
 
-    private MerchantRecord maskPhonesForUnauthorized(MerchantRecord merchant) {
-        return canViewPhonePlaintext() || merchant == null ? merchant : merchant.withMaskedPhones();
+    private MerchantRecord maskPhonesForUnauthorized(MerchantRecord merchant, boolean viewPhonePlaintext) {
+        return viewPhonePlaintext || merchant == null ? merchant : merchant.withMaskedPhones();
     }
 
-    private PageResponse<MerchantRecord> maskPhonesForUnauthorized(PageResponse<MerchantRecord> page) {
-        if (canViewPhonePlaintext()) {
+    private PageResponse<MerchantRecord> maskPhonesForUnauthorized(PageResponse<MerchantRecord> page, boolean viewPhonePlaintext) {
+        if (viewPhonePlaintext) {
             return page;
         }
         return new PageResponse<>(page.items().stream().map(MerchantRecord::withMaskedPhones).toList(),
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
index a16333d9..52bcae2b 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.03
+// 文件版本：V0.04
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.mockito.ArgumentMatchers.any;
@@ -428,6 +428,84 @@ class PlatformControllerTests {
         Mockito.verifyNoInteractions(service);
     }
 
+
+    // M009-058：真实授权服务；单次许可只供一次响应，同组功能权限不能先把电话权限耗掉。
+    @org.junit.jupiter.params.ParameterizedTest
+    @org.junit.jupiter.params.provider.CsvSource({"false,false", "false,true", "true,false", "true,true"})
+    void merchantPhoneSingleUseAppliesToOneResponse(boolean writeResponse, boolean sameGrant) {
+        var repository = Mockito.mock(com.complex.platform.access.AccessRepository.class);
+        var consumed = new java.util.concurrent.atomic.AtomicBoolean();
+        String permission = writeResponse ? "merchant.profile.manage" : "merchant.profile.view";
+        when(repository.permissionExists(any())).thenReturn(true);
+        when(repository.findPermissionEffects(eq("phone_once"), any())).thenAnswer(call -> List.of(
+                new com.complex.platform.access.AccessRepository.PermissionEffect("phone_once", permission,
+                        com.complex.platform.access.AccessRepository.Effect.GRANT,
+                        sameGrant ? com.complex.platform.access.AccessRepository.Source.TEMPORARY_GRANT
+                                : com.complex.platform.access.AccessRepository.Source.ROLE,
+                        sameGrant ? "phone_once_grant" : "profile_role", null, null,
+                        sameGrant, sameGrant && consumed.get()),
+                new com.complex.platform.access.AccessRepository.PermissionEffect("phone_once", "pii.contact_phone.view",
+                        com.complex.platform.access.AccessRepository.Effect.GRANT,
+                        com.complex.platform.access.AccessRepository.Source.TEMPORARY_GRANT,
+                        "phone_once_grant", null, null, true, consumed.get())));
+        when(repository.markSingleUsePermissionConsumed(eq("phone_once_grant"), any(), eq("phone_once"), any()))
+                .thenAnswer(call -> consumed.compareAndSet(false, true));
+        var authorization = new AuthorizationService(repository);
+        var controller = new PlatformController(service, authorization, Mockito.mock(VenueBusinessHoursService.class));
+        var merchant = new MerchantRecord("mch_once", "商户", "other", null, null, null,
+                "甲", "18800000011", null, null, null, null, "乙", "18800000013",
+                null, null, "active", Instant.EPOCH, Instant.EPOCH);
+        when(service.listMerchants(false, null, 1, 50))
+                .thenReturn(new PageResponse<>(List.of(merchant, merchant), 1, 50, 2));
+        when(service.archiveMerchant(eq(merchant.id()), any())).thenReturn(merchant);
+        var previous = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
+        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
+                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
+                        new com.complex.platform.access.AccessActor("phone_once", "phone_once", "staff",
+                                "project_a", null, "active"), "token", List.of()));
+        java.util.function.Supplier<List<MerchantRecord>> request = () -> writeResponse
+                ? List.of(controller.archiveMerchant(merchant.id(),
+                        new MerchantArchiveRequest("phone-once", "保存测试")).data())
+                : controller.listMerchants(false, null, 1, 50).data().items();
+        try {
+            assertThat(request.get()).allSatisfy(record -> {
+                assertThat(record.contactPhone()).isEqualTo("18800000011");
+                assertThat(record.secondaryContactPhone()).isEqualTo("18800000013");
+            });
+            assertThat(consumed.get()).isTrue();
+            if (sameGrant) {
+                org.assertj.core.api.Assertions.assertThatThrownBy(request::get)
+                        .isInstanceOfSatisfying(ApiException.class,
+                                error -> assertThat(error.code()).isEqualTo("PERMISSION_DENIED"));
+            } else {
+                assertThat(request.get()).allSatisfy(record -> {
+                    assertThat(record.contactPhone()).isEqualTo("188****0011");
+                    assertThat(record.secondaryContactPhone()).isEqualTo("188****0013");
+                });
+            }
+            verify(repository, Mockito.times(1)).markSingleUsePermissionConsumed(
+                    eq("phone_once_grant"), any(), eq("phone_once"), any());
+        } finally {
+            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(previous);
+        }
+    }
+
+    @Test
+    void failedPhonePermissionConsumptionDoesNotReturnPlaintext() throws Exception {
+        when(authorizationService.currentActorHasPermission("pii.contact_phone.view")).thenReturn(true);
+        when(service.listMerchants(eq(false), any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(
+                List.of(new MerchantRecord("mch_race", "商户", "other", null, null, null,
+                        "甲", "18800000011", null, null, null, null, "乙", "18800000013",
+                        null, null, "active", Instant.EPOCH, Instant.EPOCH)), 1, 50, 1));
+        doThrow(ApiException.forbidden("PERMISSION_DENIED", "单次授权已被使用"))
+                .when(authorizationService).requirePermissions("merchant.profile.view", "pii.contact_phone.view");
+
+        mockMvc.perform(get("/api/v1/platform/merchants").with(user("ops").roles("P0_SMOKE")))
+                .andExpect(status().isForbidden())
+                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
+        Mockito.verifyNoInteractions(service);
+    }
+
     @TestConfiguration
     static class TestBeans {
         @Bean
```

## 结论
修复正确性PASS；完整diff每行归因PASS；越界判定PASS。Codex B4 PASS，等Grok独立完整报告，未将M009标PASS。
