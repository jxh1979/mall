CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M009 B4-r5 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r5
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M009-b4-r5-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r5 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。B3 累计第 5 次（操作员追加一次已批准）只审本对 tree 的修复内容。席甲自审票本席未读；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：`docs/audit-2026-09/modules/M009-b4-r5-self-raw.md`（路径存在：20035 字节、mtime 2026-09-22 06:42，按互盲禁读正文）。未读适配器缓冲、未读席甲本次 B4 意见、未读原始 CLI 给对方。定稿队列以 findings.md V3.48 顶部「M009现锁B2双PASS定稿」为准：仅 M009-058 队列是。本席 B2 V0.16（`M009-b1-grok-raw.md` 最后一次完整 B2）同意的最小修法作为执行绑定对照，不把 B2 票本身当证据。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；仅 058 是否按 B2 同意边界修好；每一行能否归因到 M009-058；是否越界改 AuthorizationService / HTML / 锁 / sidecar / ledger 基准 / 新权限码 / 新端点 / DB / 其它模块。未新造判据/角色，未询问操作员，未 LOCK-AMEND。

---

## 0. 技能自核（§6，不冒充）

本席核到且按收窄口径使用：

| 技能 | 路径 | 本轮用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能优先；操作员口径覆盖通用派子代理 |
| requesting-code-review | 同 superpowers | B4 请审=只派本席；禁止 `HEAD~1` 基线、禁止子代理复审、禁止无 ID 即刻修 |
| receiving-code-review | 同 superpowers | 本席是外审，不 IMPLEMENT |
| verification-before-completion | 同 superpowers | PASS 前实跑 114 单测 + /tmp 探针 |
| systematic-debugging | 同 superpowers | 只借读错误→复现→追数据流；不改生产码 |
| test-driven-development / tdd | superpowers + mattpocock | 红阶段未改工作区回放 maven；用 before 算法隔离探针复现 5 条失败语义 |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗结构自问自答；事实自取，不等操作员、不派子代理 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 七入口共用 helper 缝；消费留在已有 `requirePermissions` |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 红环=先消费功能权再查布尔；同组第一次脱敏 |
| ponytail / ponytail-review | `.../ponytail/4.9.0/skills/` | 复用既有一次核验；不改全局布尔、不新权限码。Lean already. |
| writing-for-agents | 存在 | 本 raw 给下游 B5/调度读 |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 存在；可选。本席不安装 hook、不写 `.git` |

缺口（不冒充已调用）：`batch-grill-me` 不存在；`ubiquitous-language` 不存在。划掉的 worktree/子代理/research/Arc 全仓 audit 未调用。graft SKILL.md 已读；本轮权威证据是两 tree `git diff --binary`，未调 graft MCP（V0.17 禁 MCP/子代理）。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；891182 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger 顶行同值。criteria-lock 版本 V0.14。`LOCK-AMEND-COUNT: 1`。本批 B3 两树 lock blob 均为 `69dccbb728feb863971f2fd7aca76da47db11e64`（内容 SHA256 同上，未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M009 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段 |
| 阶段 | B4-r5 只读审计 B3 累计第5次（操作员追加一次已批准）的「修复内容」本身 |
| 正式队列「是」（1） | 仅 M009-058（单次权限消费：七入口共享 helper；有电话权则同次 `requirePermissions(功能, phone)`；无电话权只原功能权；请求内 boolean 传两 mask，禁止消费后再查） |
| 正式队列「否」 | 038–057、059 及历史已修/登记项；059 旧行未回填不在本批准范围 |
| B3 | 累计 4→5（ledger V7.91 当前段）；模块仍 IN_PROGRESS |
| findings.md | V3.48；工作区 SHA256 `1ac0eda3f6ad3d31d3ea40cb242b8b1cdd91d8e3d8941c866a3451326c51983c`（本 diff 未改 findings；两树 findings blob 均为 `f80111253bab3ea1ad8c8cdc2e77c26c1b97586b` 且等于工作区） |
| before（对象类型 tree，可解析） | `4ddfdc255c4bbf0da9d42837e9b1bd7c0124b6be` |
| after（对象类型 tree，可解析） | `a685f41c8cc2333c5bb937acce746d5258f9eade` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 4ddfdc255c4bbf0da9d42837e9b1bd7c0124b6be a685f41c8cc2333c5bb937acce746d5258f9eade`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 14402 bytes；SHA256 `190cf338324bd6be153d5afabe5ee87d948cb53bedfff8f881443f1269db3148`；UTF-8；无 NUL；214 行；2 路径；106 插入 / 22 删除；7 hunk；name-status 2×M |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3960 / after 3960；`comm -3` 空（无增删路径） |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main`=`ebb1eb99c610df330c07d24278798c2d3611299a`（本席未 fetch） |
| 工作区两文件 blob | 全部等于 after（见 §1.1） |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `6e9d813c985406aaf14c1645af2b494d342ae657`；三者两树完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |
| AuthorizationService.java 两树 blob | `4bfae6d2ddded7e2810666bac72bfce818ff9e22` 相同（全局授权服务未改） |
| PlatformService.java 两树 blob | `63e53b3177610f87a5b49e82173e375dbaa7d951` 相同 |
| ledger.md | 两树 blob `e3d1a5248da5f43746533ebd39ff7b505256ec0e` 相同；工作区 `eb1e62b180b2e24e3238c0b456e87699b6de9134` 较新（B3 完成后的台账段，不在本对 tree diff 内） |

`git diff --stat --binary`：

```
 .../platform/governance/PlatformController.java    | 48 +++++++------
 .../governance/PlatformControllerTests.java        | 80 +++++++++++++++++++++-
 2 files changed, 106 insertions(+), 22 deletions(-)
```

`git diff --numstat`：26/22 PlatformController.java；80/0 PlatformControllerTests.java。

### 1.1 工作区两文件仍等于 after 树

| 路径 | before blob | after=worktree blob | 判定 |
|---|---|---|---|
| `backend/src/main/java/com/complex/platform/governance/PlatformController.java` | `52d3f0d27191d6fd4cc923a1607a9263bc3eb7fa` | `ad4edc0d417dbcd8311e34e6da5f24a68188bd54` | MATCH-AFTER；文件版本 V0.02→V0.03 |
| `backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java` | `a16333d9e0d89863c0620b30aec588eae84ebc8f` | `52bcae2bceabd0feb7d6c78125ba6ef1adaedb63` | MATCH-AFTER；文件版本 V0.03→V0.04 |

真实 index 仍停在 before 各 blob（`git ls-files -s` 上表 before 列），符合 B3 用临时 `GIT_INDEX_FILE` 写 after tree、不碰真正暂存区的规程；B4 审的是两 tree，不是真实 index。HEAD 两文件 blob（`ca47080c…` / `02d9e49d…`）更旧，属已提交基线，与本对 tree 无关。本席未改任何源文件；本 raw 原先不存在，本次新建。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.14 已映射到 M009 的集合（含 PGM-041/PGM-059、GEN-002/005/011）。本轮只用定稿已引用的 ID。不 LOCK-AMEND。

HTML 原文（本席自读现文，不是旧禁令）：

- PGM-041 / `docs/platform-governance-map-for-operations-v0.1.html:656`：「临时授权可设有效期或单次使用；权限的授予、收回、到期、单次消耗、失效全部留痕。」
- PGM-059 / 同文件 `:871`：「经单独授权且查看权限有效的人员，以及持授权 U 盘密钥的最高权限本人，可以在其获准访问的商户档案列表和档案操作结果中查看完整号码……平台管理员身份本身不自动取得该查看权限。」
- `:680` 页面例外不开放明文导出/打印：本 diff 未改导出/打印，未扩大例外。

B2 同意的 058 最小边界（本席 V0.16，与 findings V3.48 058 行一致）：

1. 只改 `PlatformController` 七个返回 `MerchantRecord` 的入口 + 两 mask 重载；文件版本递增。必要适配现有 `PlatformControllerTests`。
2. 共享 helper 接收该入口原功能权码（list=`merchant.profile.view`；create/update/assign/archive/unarchive/delete-request=`merchant.profile.manage`）。不得把 manage 入口改成还要 view，不得把 `pii.contact_phone.view` 做成端点必选。
3. 有电话权：`requirePermissions(原功能权, pii.contact_phone.view)` 一次；无电话权：只 `requirePermission(原功能权)` 并脱敏。USB/`isMasterAuthority` 仍走 grant=null、consume 集合空、本次明文=true。
4. 本次是否明文是 helper 的请求内局部 boolean，只传给私有 mask 重载。禁止消费后再查布尔。
5. 不改全局 `AuthorizationService`、不添加功能/HTML/DB、不改公共 `currentActorHasPermission` 只读语义。
6. 列表按请求消费一次，不按行。`approveDeleteMerchant` 返回 `MerchantDeleteDecision`，保持原 `merchant.profile.approve`。

四分类本轮不重开：058 仍冲突；无添加项。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary 4ddfdc255c4bbf0da9d42837e9b1bd7c0124b6be a685f41c8cc2333c5bb937acce746d5258f9eade`
字节：14402
SHA256：190cf338324bd6be153d5afabe5ee87d948cb53bedfff8f881443f1269db3148

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

（文件在 TestBeans 处截断是因为该 hunk 以新增测试插入结束、后续旧 TestBeans 无改。`git diff --stat` 已计 80 测试插入 / 0 删除于此文件的净变化；hunk 头 `@@ -428,6 +428,84` 与全文 14402 字节一致。）

---

## 4. 逐块 ID 归因（闸 3）

| hunk | 文件 | 行范围（after） | 归属 | 理由 |
|---|---|---|---|---|
| C1 | PlatformController.java | L2 版本 V0.02→V0.03 | M009-058 | 058 边界要求自身版本递增 |
| C2 | 同上 | L66–76 list view + create manage | M009-058 | 七入口之 1–2：原功能码进 helper，boolean 传 mask |
| C3 | 同上 | L83–94 update + assign manage | M009-058 | 七入口之 3–4 |
| C4 | 同上 | L112–132 archive/unarchive/delete-request | M009-058 | 七入口之 5–7。hunk 下文 `delete-approve` 未改 |
| C5 | 同上 | L281–299 helper + 两 mask 重载 | M009-058 | 删除只查询即明文；有电话权同次 `requirePermissions`；无电话权只原功能权；请求内 boolean、不重查 |
| T1 | PlatformControllerTests.java | L2 版本 V0.03→V0.04 | M009-058 | 验证性测试文件版本递增 |
| T2 | 同上 | L431–507 | M009-058 | 4 组单次/同组×列表/写回 + 消费失败不泄漏；闸 3 允许为该 ID 写验证测试 |

未出现的路径（两树相同，故无行可归因，亦无越界）：`AuthorizationService.java`、`PlatformService.java`、三 HTML、criteria-lock、sidecar、SQL 迁移、其它 M009 18 文件、其它模块 Controller 的 PhoneMask 布尔查询。

`approveDeleteMerchant`（L135–141）仍 `requirePermission("merchant.profile.approve")`，返回 `MerchantDeleteDecision`，符合「不在七入口」。`associateBusinessEntityProject` 仍 `entity.binding.manage`，返回 `BusinessEntityRecord`。

结论：diff 每一行都能归到已定稿且队列为「是」的 M009-058。闸 3 本席 PASS。

---

## 5. 三线（只审本 diff）

### 5.1 第一性原理

1. **为什么必须存在？** 删掉 helper 同次核验，PGM-041「单次消耗」与 PGM-059「查看权限有效」在七个商户响应入口做不到：before 是 `requirePermission(功能)` 后再 `currentActorHasPermission(phone)`，单次电话可重复明文，同组功能权会先烧掉整组使第一次合法持权也被脱敏。
2. **声称的行为是否执行？** 见 §6 本席实跑 114 单测 + /tmp 探针 GREEN-A…I 全部 PASS。
3. **未经验证就当真的前提？** 本席未把「maven 红阶段 19/5」当作自己跑过的命令；红语义用 before 算法探针 RED-A/B 独立复现。未跑写库 JDBC。未把 USB 主授权写成已改 `AuthorizationService`。

### 5.2 顺从式（假定修法意图正确）

逐条对照 B2 边界：

| 边界 | 现码 | 判定 |
|---|---|---|
| 七入口原 view/manage | after L66 view；L73/83/92/112/121/130 manage | 成立 |
| 有电话权同次 requirePermissions | L283–284 | 成立 |
| 无电话权只原 functional | L285–286 | 成立 |
| 请求内 boolean 传两 mask | L288 返回值 → L291/295 | 成立 |
| 不消费后再查 | mask 不再调用 `currentActorHasPermission` | 成立 |
| 不改 AuthorizationService | 两树 blob 相同 | 成立 |
| 不添加功能/HTML/DB | diff 仅 2 文件 | 成立 |
| 公共布尔保持只读 | `currentActorHasPermission` 未改；helper 仍先只读问 phone | 成立 |
| USB master grant=null 不消费 | `AuthorizationService:80-84` 短路返回 null；探针 GREEN-D 两次明文、mark 0 次 | 成立 |
| 平台管理员不自动取得 | V394 `requires_approval=TRUE` 使 phone 不进 daily；`isExplicitOrDailyGrant` 过滤 `role_platform_admin` 高危角色种；探针 GREEN-E 掩码 | 成立 |
| 列表一次消费不按行 | 参数化用例 2 条 items、mark 1 次；探针 GREEN-A | 成立 |

旧测试必要适配：无电话权路径仍走 `requirePermission`（`creatingMerchantWithoutManagePermission` / `archivingMerchant…` / `projectAssociationEndpoints…` 的 verify 仍成立）。有电话权的旧 mock 用例 `listingMerchantsReturnsPlaintextPhoneWithPiiPermission` 仍只 stub 布尔；consume 由新用例覆盖。

### 5.3 对抗式（假定实现错误）

自问自答（grilling 结构，不等操作员）：

1. **是否把 phone 做成端点必选？** 无电话权走 `requirePermission(functional)`。探针 GREEN-I：对无电话权主体硬调 `requirePermissions(view, phone)` 会 `没有操作权限：pii.contact_phone.view`。现码避开该路径。
2. **同组是否仍先烧掉功能权？** after 先只读问 phone，再同次双码。探针 GREEN-B 第一次明文。RED-B before 算法第一次脱敏。缺陷已删。
3. **单独电话单次是否仍可重复明文？** GREEN-A 第二次掩码且 mark 1 次。RED-A before 两次明文且 consumed=false。
4. **消费失败是否泄漏明文？** `failedPhonePermissionConsumptionDoesNotReturnPlaintext`：403、`verifyNoInteractions(service)`。探针 GREEN-F：`mark` 返回 false → `PERMISSION_DENIED`，service 0 次。异常 message 不含号码。
5. **真实 master / 普通 admin？** GREEN-D master 明文不消费。GREEN-E `platform_admin` + `role_platform_admin` 种子电话权 → 掩码。冒充 master 但 id/username 不全等会被 `isMasterAuthority` 拒绝（三字段合取，本席读源码 L139–144）。
6. **七入口是否漏接？** after 全文 302 行：返回 `MerchantRecord` 的恰好这七个；`javap` 七处 `requireMerchantPermission`。探针 GREEN-H 七入口第一次均为明文并消费。
7. **其它模块？** LeasingController / MerchantFeeNotificationRecipientController 仍用布尔查询，不在本 diff，本批不得扩大。
8. **列表消费后 scope/幂等失败会不会白耗电话权？** list 仍是 helper 之后 `requirePlatformScope`；写入口仍是 helper 之后 `requireIdempotencyKey`。功能权单次在 before 已是同一顺序。B2 明确保留 scope/幂等键。不新开 finding；记观察。

ponytail-review：无可删抽象。Lean already. `net: -0`。

---

## 6. 测试与可执行证据

### 6.1 本席绿阶段（工作区=after）

命令（backend 目录，纯单测，未导出写库变量、未改测试）：

```
mvn -Dtest=com.complex.platform.governance.PlatformControllerTests,com.complex.platform.governance.PlatformServiceTests,com.complex.platform.access.AuthorizationServiceTests -DfailIfNoTests=true test
```

结果：BUILD SUCCESS；Tests run: 114, Failures: 0, Errors: 0, Skipped: 0。

| 类 | Tests run | Failures | Errors |
|---|---:|---:|---:|
| PlatformServiceTests | 70 | 0 | 0 |
| PlatformControllerTests | 19 | 0 | 0 |
| AuthorizationServiceTests | 25 | 0 | 0 |

19 = 14 既有 `@Test` + `@ParameterizedTest` 4 组 + 1 消费失败。与声称 70+19+25=114 一致。`AuthorizationService` 不在本 diff，25 例作回归、证明全局授权服务未被本批改坏。

未跑 `PlatformJdbcIntegrationTests`（写库）。surefire 目录里有其旧 xml，不是本席本次命令产物。

### 6.2 红阶段

本席**没有**把工作区 Controller 退回 before 再跑 maven（禁止改主工作区代码测试）。隔离探针用 before 算法（先 `requirePermission(功能)` 再查布尔）对同一真实 `AuthorizationService`：

- RED-A 单独电话单次：`first=18800000011 second=18800000011 consumed=false`
- RED-B 同组单次：第一次 `188****0011` 且 consumed=true

对应 4 组参数化（列表/写回 × 分源/同组）在 before 下应失败、加上消费失败用例共 5 fail 0 error 的语义。本席不把操作员口头「红 19/5」写成自己跑过的 surefire。

### 6.3 /tmp 隔离探针

路径：`/tmp/m009-b4-r5-grok-probe/Probe.java`（绝对 /tmp，先 mkdir）。javac 0 / java 0 / `PROBE_PASS`。未向主 `.git` 或业务树写盘。

| 探针 | 结果 |
|---|---|
| RED-A before 单独电话可重复明文 | PASS |
| RED-B before 同组第一次脱敏 | PASS |
| GREEN-A after 分源：先明文后掩码、mark×1、列表 2 条一次消费 | PASS |
| GREEN-B after 同组：先明文、第二次 PERMISSION_DENIED、message 无号码 | PASS |
| GREEN-C 无电话权：掩码、mark×0 | PASS |
| GREEN-D 真实 master 三字段：两次明文、mark×0 | PASS |
| GREEN-E platform_admin + role 种子电话权：掩码 | PASS |
| GREEN-F mark 失败：拒绝且 service 0 次 | PASS |
| GREEN-G 消费后 `currentActorHasPermission(phone)=false`，但当次响应仍用请求内 boolean 明文 | PASS |
| GREEN-H 七入口第一次明文并消费 | PASS |
| GREEN-I 若把电话权写成必选会拒绝无电话权主体 | PASS |

### 6.4 测试质量

能失败：参数化断言明文/掩码/PERMISSION_DENIED；消费失败断言 403 且不进 service。真实 `AuthorizationService` 而非只 mock 布尔。列表多条一次消费。写回走 archive。无电话权既有列表用例仍断言掩码。

观察（不升 finding）：单测夹具未覆盖 USB master / platform_admin；本席探针已补证。旧 `listingMerchantsReturnsPlaintextPhoneWithPiiPermission` 仍只 stub 布尔、不证明消费。create/update/assign/unarchive/delete-request 与 archive 共用 helper，单测只直接打了 list+archive；GREEN-H 补了其余入口。

---

## 7. 本轮 findings

临时编号空集。无闸 3 越界行。无把 059 回填、第 1 项导出/打印、全局授权服务或其它模块改动混进本 diff。

观察（并列，不覆盖、不入队）：

- OBS-B4R5-01：list 在 helper 之后才 `requirePlatformScope`；写入口在 helper 之后才校验幂等键。功能权单次在 before 已是此序。B2 要求保留这两闸。
- OBS-B4R5-02：V339 曾把 `pii.contact_phone.view` 种给 `role_platform_admin`；运行时 daily 桥 + `isExplicitOrDailyGrant` + V394 审批标记使其不自动生效。属既有授权服务，本 diff 未改。

---

## 8. 审计结论

本席 B4-r5：**PASS**。

修复内容与 B2 同意边界一致；完整未过滤 binary diff 仅 2 文件；逐块均归 M009-058；授权范围、单次、同组、真实 master、普通 admin、无电话权、失败不泄漏均有本席命令/探针证据；版本 V0.03/V0.04；测试 114 绿。未扩大到其它模块。

本票不是 B4 双 PASS，不是 B5，不是模块 PASS。第 1 项导出/打印例外与 059 旧行未回填保持原登记，本 diff 未宣称消除。B3 累计 5。停止，不推进 B5。
