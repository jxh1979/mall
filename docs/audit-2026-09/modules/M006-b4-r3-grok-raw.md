CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M006 B4-r3 Grok 只读复审（审计席乙）

版本：V0.01
控制：V0.17；模块 M006 `10-backend-main-access-01`（6/322）；B3 已执行次数=3；ledger=`IN_PROGRESS`。
本席角色：Grok。只读复审本轮完整 diff，不修复、不改方案、不执行 B3/B5、不重开其他模块、不启用第三席。
本次完整报告范围：本文件自本行 `CRITERIA-LOCK-SHA256:` 起至文末。

## 0. 技能预检（实际核读，非声称）

已实际读取并按合同收窄口径使用：

| 技能 | 路径 | 本轮用法 |
|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检；本席为派发的审计席乙，不按其“先澄清再动手”扩闸 |
| requesting-code-review | 同套件 `requesting-code-review/SKILL.md` | B4 只由本席审；**不用**本体 `HEAD~1`、同会话子代理、无 ID 即刻修 |
| verification-before-completion | 同套件 `verification-before-completion/SKILL.md` | PASS 前本席在 `.scratch` 隔离实跑红绿对照，不以操作员自审 98/0 当本席证据 |
| grilling | `/home/james/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只借追问结构；**不**等操作员、**不**派子代理 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 读错误→复现→追数据流；不改生产代码、不加诊断桩 |
| diagnosing-bugs | mattpocock `engineering/diagnosing-bugs/SKILL.md` | 031/032 红绿对照环 |
| codebase-design | mattpocock `engineering/codebase-design/SKILL.md` | 幂等范围接缝：`CallerScopeResolver` / `IdempotencyService.execute` |
| tdd | mattpocock `engineering/tdd/SKILL.md` | 测缝须用户确认作废；本席只核已落测试是否能红、是否打在真实接缝 |
| ponytail-review | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删；结论见 §7 |
| writing-for-agents | mattpocock `productivity/writing-for-agents/SKILL.md` | 本 raw 给下游席阅读 |
| i-have-adhd | `/home/james/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 编号、先结论、无寒暄；**第一行被控制锁哈希格式覆盖** |
| differential-review | trailofbits `differential-review/SKILL.md` + methodology/adversarial/patterns | 6 文件 SMALL+HIGH（授权/幂等）；**不**另写报告文件、**不**派 adversarial-modeler |
| knowledge-work code-review | `/home/james/.claude/plugins/cache/knowledge-work-plugins/engineering/1.2.0/skills/code-review/SKILL.md` | 正确性/安全/错误路径；CONNECTORS.md 不存在，记缺口 |
| sentry code-review | sentry-skills `code-review/SKILL.md` | 访问控制与测试接缝 |
| audit-context-building | trailofbits 该技能 | 手工追 `currentActor`/`resolve`/`execute`/`FieldLabels.translate`；**不**派 function-analyzer、不写 `audit-context/` |

缺口（不得冒充已调用）：

- `batch-grill-me`：未找到技能目录
- `ubiquitous-language`：未找到技能目录
- `git-guardrails-claude-code`：存在于 mattpocock `skills/misc/`，可选；本轮无 git 写操作，未调用
- knowledge-work `code-review/CONNECTORS.md`：文件不存在

明确不用（合同划掉）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`ponytail-audit`、`ponytail-debt`、`research`、`code-review`（mattpocock）。未派第三席。

## 1. 冻结输入

- 本席实算 `docs/audit-2026-09/plan/criteria-lock.md` SHA256 = `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`
- sidecar `plan/criteria-lock.sha256` 同行；`ledger.md` 顶部同行；`LOCK-AMEND-COUNT: 0`
- `findings.md`：V0.62；241974 字节；SHA256=`abdf0b89301bd74b50b1f25c6218085e033114fc2f25e28da1b50044b62d6f2f`
- 当前队列表唯一值：031/032 修复队列=是；001—030 与 033=否。本轮只允许归因 031/032。
- 拟修边界（现值，不扩）：031 沿用既有主体/组织字段收窄共享 `p` 范围，缓存表不改结构；新范围键用已有 `hashJson` 适配 `operation_id` VARCHAR(128)；旧共享记录缺可靠主体归属时拒绝再次执行且不返回其内容；同主体新记录重试仍返回原结果。032 只改本模块账号类型错误分支与真实 handler 响应断言，不改 FieldLabels、不改错误码与注册审批规则。
- before tree=`11319ccfc6c0a3b609bc1cfdd4579766a7118705`（type=tree）
- after tree=`c77d4453db7eb9b92388de630fdd5121cdef7959`（type=tree）
- 未读取本轮 M006-b4-r3 self raw（文件不存在）、任何 B5 raw、双方 B1/B2 论证正文；不以其他席结果作证据。findings.md 仅作闸 3 ID 归因来源。
- 未按路径过滤 diff、未改用 HEAD、未建第三 tree。工作区另有 r1/r2 未跟踪修复源，均同时存在于 before/after 两树，故本轮 diff 无名变化。

适用冻结判据（模块映射内）：GEN-001、GEN-002、GEN-003、GEN-004、GEN-005、GEN-007、GEN-009、GEN-010、GEN-015、BSM-028、BSM-114、PGM-001、PGM-066。不扩映射、不启用锁外判据、不要求改冻结 scope。

## 2. 两树完整 diff 逐字原文

命令：`git diff --binary 11319ccfc6c0a3b609bc1cfdd4579766a7118705 c77d4453db7eb9b92388de630fdd5121cdef7959`

实算：14687 字节；SHA256=`a04dfea8324fd7a11d5b5cfebf3585fd93c9cb32ec3cd05fde2bb8b610956717`；6 路径；`grep -c '^@@'` = 12。与派发声明一致。

`git ls-tree -r --name-only` 两树路径 `comm -3` 为空（无新增/删除路径，仅 6 个已有路径改内容）。6 个工作区文件 SHA256 均与 after blob 逐字相同。

路径与 numstat（+ / −）：

1. `backend/src/main/java/com/complex/platform/access/AccessService.java` +2/−2
2. `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` +21/−23
3. `backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java` +8/−6
4. `backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java` +13/−3
5. `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` +51/−1
6. `backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java` +34/−0

下文为该命令全部未过滤输出（逐字）：

````
diff --git a/backend/src/main/java/com/complex/platform/access/AccessService.java b/backend/src/main/java/com/complex/platform/access/AccessService.java
index 37ddec45..aded7caf 100644
--- a/backend/src/main/java/com/complex/platform/access/AccessService.java
+++ b/backend/src/main/java/com/complex/platform/access/AccessService.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.12
+// 文件版本：V0.13
 
 import com.complex.platform.access.dto.AccessGrantApplyRequest;
 import com.complex.platform.access.dto.AccessGrantReviewRequest;
@@ -920,7 +920,7 @@ public class AccessService {
                         String accountType = normalizeAccountType(trimToDefault(request.accountType(), "staff"));
                         if (!ALLOWED_ACCOUNT_TYPES.contains(accountType)) {
                             throw ApiException.badRequest("INVALID_ACCOUNT_TYPE",
-                                    "账号类型「" + accountType + "」不允许，可选值：" + ALLOWED_ACCOUNT_TYPES);
+                                    "账号类型不允许，请重新选择");
                         }
                         String organizationId = requireText(request.organizationId(), "organization_id");
                         if (!authorizationService.isPlatformAdmin(actor)
diff --git a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
index 9ed82813..7221c16f 100644
--- a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
+++ b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.03
+// 文件版本：V0.04
 
 import com.complex.platform.auth.AuthService;
 import com.complex.platform.common.api.ApiException;
@@ -190,32 +190,30 @@ public class AuthorizationService implements CallerScopeResolver {
         throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
     }
 
-    /**
-     * Returns the caller scope token for idempotency key isolation.
-     * "m:{merchantId}" for merchant users, "p" for platform admins/staff.
-     * Returns "" on any error so IdempotencyService falls back to unscoped (safe degradation).
-     */
+    /** 非商户请求按已有主体及组织隔离；商户与系统请求沿用原范围。 */
     @Override
     public String resolve() {
-        try {
-            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
-            if (auth == null || !auth.isAuthenticated()) {
-                return "";
-            }
-            Object principal = auth.getPrincipal();
-            String merchantId = null;
-            if (principal instanceof AccessActor actor) {
-                merchantId = actor.merchantId();
-            } else if (principal instanceof AuthService.UserRecord user) {
-                merchantId = user.merchantId();
-            }
-            if (merchantId != null && !merchantId.isBlank()) {
-                return "m:" + merchantId;
-            }
-            return "p";
-        } catch (Exception e) {
+        String legacy = legacyScope();
+        if (!"p".equals(legacy)) {
+            return legacy;
+        }
+        AccessActor actor = currentActor();
+        if (actor.id() == null || actor.id().isBlank()) {
+            throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
+        }
+        return "u:" + actor.id().length() + ":" + actor.id() + ":"
+                + java.util.Objects.toString(actor.organizationId(), "");
+    }
+
+    @Override
+    public String legacyScope() {
+        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
+        if (auth == null || !auth.isAuthenticated()) {
             return "";
         }
+        AccessActor actor = currentActor();
+        String merchantId = actor.merchantId();
+        return merchantId == null || merchantId.isBlank() ? "p" : "m:" + merchantId;
     }
 
     private boolean knownPermission(String permissionCode) {
diff --git a/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java b/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java
index 8d4c9c08..a98a5ede 100644
--- a/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java
+++ b/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java
@@ -1,12 +1,14 @@
 package com.complex.platform.common.idempotency;
 
-/**
- * Resolves the current caller's scope token for idempotency key isolation.
- * Returns "m:{merchantId}" for merchant callers, "p" for platform admins,
- * or "" (empty) when no scope is available (e.g., in tests).
- * An empty scope leaves the operationId unmodified (backward-compatible).
- */
+// 文件版本：V0.01
+
+/** Resolves the trusted caller scope used to isolate idempotent operations. */
 @FunctionalInterface
 public interface CallerScopeResolver {
     String resolve();
+
+    /** Existing scopes keep their keys; changed scopes must also check their old records. */
+    default String legacyScope() {
+        return resolve();
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java b/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java
index 65260597..05f286a3 100644
--- a/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java
+++ b/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java
@@ -1,4 +1,7 @@
 package com.complex.platform.common.idempotency;
+
+// 文件版本：V0.01
+
 import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;
 
 import com.complex.platform.common.api.ApiException;
@@ -70,7 +73,15 @@ public class IdempotencyService {
             Function<T, T> storedResponseTransformer
     ) {
         requireIdempotencyKey(idempotencyKey);
-        String scopedId = scopedOperationId(operationId);
+        String scope = callerScopeResolver.resolve();
+        String legacyScope = callerScopeResolver.legacyScope();
+        String legacyId = scopedOperationId(operationId, legacyScope);
+        boolean changedScope = !java.util.Objects.equals(scope, legacyScope);
+        String scopedId = changedScope ? "caller:" + hashJson(java.util.List.of(scope, operationId)) : legacyId;
+        // 旧共享记录没有可靠主体归属：保留结果，阻止串用或再次执行。
+        if (changedScope && repository.findByOperationIdAndIdempotencyKey(legacyId, idempotencyKey).isPresent()) {
+            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "该请求编号存在旧处理记录，请先核实原操作结果");
+        }
         String requestHash = hashJson(request);
         Instant now = Instant.now();
         IdempotencyRecord reservation = new IdempotencyRecord(
@@ -111,8 +122,7 @@ public class IdempotencyService {
                 "请求正在处理中，请稍后刷新查看结果");
     }
 
-    private String scopedOperationId(String operationId) {
-        String scope = callerScopeResolver.resolve();
+    private String scopedOperationId(String operationId, String scope) {
         if (scope == null || scope.isEmpty()) {
             return operationId;
         }
diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
index cbf28cd3..7b63bf74 100644
--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.19
+// 文件版本：V0.20
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.ArgumentMatchers.*;
@@ -592,4 +592,54 @@ class M006SecurityRegressionTests {
         assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
                 e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
     }
+
+    // M006-031：实际控制器须按组织和操作人区分首次请求。
+
+    @Test void companyRequestsHaveIndependentResultsAndRetryOnlyOnce() {
+        var idem = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
+        idem.setCallerScopeResolver(authorization);
+        service = new AccessService(repository, idem, new AuditEventService(new InMemoryAuditEventRepository()),
+                authorization, credentials, null, true, 3, 3);
+        when(repository.listPermissions()).thenReturn(List.of(
+                new PermissionDefinition(LOW, "查看", "ops", "medium", false, "active"),
+                new PermissionDefinition("access.role.manage", "管理角色", "access", "medium", false, "active")));
+        for(String id: List.of("manager-a", "manager-b")) {
+            effect(id, LOW, AccessRepository.Effect.GRANT, id+"-read", false);
+            effect(id, "access.role.manage", AccessRepository.Effect.GRANT, id+"-manage", false);
+        }
+        var controller = new AccessController(service, authorization);
+        var request = new RoleRequest("company-first-role", "正常首次创建", List.of(), "巡检员", "只读", List.of(LOW), "project");
+        actor("manager-a", "staff", "org-a", null);
+        var first = controller.createRole(request).data();
+        actor("manager-b", "staff", "org-b", null);
+        var result = controller.createRole(request);
+        assertThat(roles.values()).extracting(RoleRecord::organizationId).containsExactlyInAnyOrder("org-a", "org-b");
+        assertThat(result.data().id()).isNotEqualTo(first.id());
+        assertThat(controller.createRole(request).data().id()).isEqualTo(result.data().id());
+        actor("manager-a", "staff", "org-a", null);
+        assertThat(controller.createRole(request).data().id()).isEqualTo(first.id());
+        assertThat(roles).hasSize(2);
+    }
+
+    // M006-032：实际错误响应须保持中文。
+
+    @Test void invalidAccountTypeResponseIsChinese() throws Exception {
+        when(repository.listPermissions()).thenReturn(List.of(
+            new PermissionDefinition("access.user.manage", "管理账号", "access", "medium", false, "active")));
+        var controller = new AccessController(service, authorization);
+        ApiException failure = null;
+        try {
+            controller.createAccessUser(new AccessUserRequest("b5-invalid", "检查错误提示", List.of(),
+                "example", "password", null, "platform_admin", "org", null, "active"));
+        } catch(ApiException e) { failure = e; }
+        assertThat(failure).isNotNull();
+        assertThat(failure.code()).isEqualTo("INVALID_ACCOUNT_TYPE");
+        var handler = new com.complex.platform.common.api.GlobalExceptionHandler();
+        var method = handler.getClass().getDeclaredMethod("handleApiException", ApiException.class);
+        method.setAccessible(true);
+        var response = (org.springframework.http.ResponseEntity<?>) method.invoke(handler, failure);
+        var body = (com.complex.platform.common.api.ApiResponse<?>) response.getBody();
+        assertThat(body.message()).contains("账号类型").doesNotContainPattern("[A-Za-z]");
+    }
+
 }
diff --git a/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java b/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java
index 02bda0c1..f604c646 100644
--- a/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java
@@ -1,5 +1,7 @@
 package com.complex.platform.common.idempotency;
 
+// 文件版本：V0.01
+
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
@@ -300,6 +302,38 @@ class IdempotencyServiceTests {
         assertThat(responseB).isNotEqualTo(new TestResponse("secret-A"));
     }
 
+
+    // M006-031：范围变更不能重放归属不明的旧结果，也不能再次执行旧请求。
+    @Test
+    void changedCallerScopePreservesOldOperationsAndBoundsNewKeys() {
+        service.setCallerScopeResolver(new CallerScopeResolver() {
+            public String resolve() { return "u:" + "user".repeat(16) + ":" + "organization".repeat(6); }
+            public String legacyScope() { return "p"; }
+        });
+        TestRequest request = new TestRequest("same-payload");
+        AtomicInteger actions = new AtomicInteger();
+        for (String code : java.util.List.of("OK", "IN_PROGRESS", "FAILED_RETRYABLE")) {
+            IdempotencyRecord old = new IdempotencyRecord(UUID.randomUUID(), "p:operation", code,
+                    hashJson(request), code, code.equals("OK") ? "{\"value\":\"old-result\"}" : null,
+                    code.equals("OK") ? 200 : 202, Instant.now(), Instant.now().plusSeconds(86400));
+            repository.put(old);
+            assertThatThrownBy(() -> service.execute("operation", code, request,
+                    () -> { actions.incrementAndGet(); return new TestResponse("duplicate"); }, TestResponse.class))
+                    .isInstanceOfSatisfying(ApiException.class, failure ->
+                            assertThat(failure.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
+            assertThat(repository.findByOperationIdAndIdempotencyKey("p:operation", code)).contains(old);
+        }
+        assertThat(actions).hasValue(0);
+        String operation = "createRole:" + "r".repeat(64);
+        TestResponse first = service.execute(operation, "fresh", request,
+                () -> { actions.incrementAndGet(); return new TestResponse("created"); }, TestResponse.class);
+        TestResponse retry = service.execute(operation, "fresh", request,
+                () -> { actions.incrementAndGet(); return new TestResponse("duplicate"); }, TestResponse.class);
+        assertThat(retry).isEqualTo(first);
+        assertThat(actions).hasValue(1);
+        assertThat(repository.records.values()).allMatch(record -> record.operationId().length() <= 128);
+    }
+
     private String hashJson(Object value) {
         try {
             MessageDigest digest = MessageDigest.getInstance("SHA-256");
````

## 3. 逐块 ID 归因（闸 3）

| hunk | 文件 | 内容 | 归属 |
|---|---|---|---|
| 1 | AccessService.java `@@ -1,6` | 版本头 V0.12→V0.13 | M006-032（本文件本轮仅 032 行为） |
| 2 | AccessService.java `@@ -920,7` | 去掉英文输入值与 `ALLOWED_ACCOUNT_TYPES` 展示 | M006-032 |
| 3 | AuthorizationService.java `@@ -1,6` | 版本头 V0.03→V0.04 | M006-031 |
| 4 | AuthorizationService.java `@@ -190,32` | `legacyScope` 保留 `m:`/`p`/`""`；非商户 `p` 改为主体+组织 | M006-031 |
| 5 | CallerScopeResolver.java `@@ -1,12` | 版本头；`default legacyScope()` 让既有实现仍只实现 `resolve()` | M006-031 |
| 6 | IdempotencyService.java `@@ -1,4` | 版本头 V0.01 | M006-031 |
| 7 | IdempotencyService.java `@@ -70,7` | 新范围 `caller:`+`hashJson`；旧共享键存在则 `IDEMPOTENCY_CONFLICT` | M006-031 |
| 8 | IdempotencyService.java `@@ -111,8` | `scopedOperationId` 改接收已解析 scope，避免二次 `resolve()` | M006-031 |
| 9 | M006SecurityRegressionTests.java `@@ -1,6` | 版本头 V0.19→V0.20 | M006-031 与 M006-032（同文件两测） |
| 10 | M006SecurityRegressionTests.java `@@ -592,4` | `companyRequestsHaveIndependentResultsAndRetryOnlyOnce` | M006-031 |
| 10 续 | 同上 | `invalidAccountTypeResponseIsChinese` | M006-032 |
| 11 | IdempotencyServiceTests.java `@@ -1,5` | 版本头 V0.01 | M006-031 |
| 12 | IdempotencyServiceTests.java `@@ -300,6` | 旧 `p:` 记录冲突、不执行、新键 ≤128、同主体重试一次 | M006-031 |

无归因不到 031/032 的行。无顺手重构、无新策略/审批/HTML、无锁/scope 改动、无 033 业务修补。

必要直接调用边界：生产唯一 `CallerScopeResolver` 实现是 `AuthorizationService`；`IdempotencyService.setCallerScopeResolver` 是既有注入点；接口 default 方法保持 `@FunctionalInterface` 与既有 `() -> ""` / `currentScope::get` 实现。`createRole` 本轮无方法体改动，因它已走 `idempotencyService.execute("createRole", …)`，隔离落在解析器与幂等服务接缝，符合 031「必要既有幂等直接实现」。

先前 B3 合法未跟踪源（`AccessGrantPolicy.java`、`V386__…sql`、若干 M006*Tests）在 before/after 两树均存在且本轮无名差异，不构成漏审 untracked，也不构成扩 scope 理由。

## 4. 三线审核

### 4.1 第一性原理

1. **为什么必须存在？** 删掉 031：两个公司组织员工用同一非空请求编号走 `AccessController.createRole`，会因共享 `p` 键把 org-b 做成 org-a 的角色回放，违背 BSM-028 原文「看不到别的商户和别的项目的数据」与「同一笔操作重复提交不重复生效」、BSM-114「只做自己该做的」、GEN-005 服务端按主体/对象授权。删掉 032：`GlobalExceptionHandler` 经 `FieldLabels.translate` 原样透传含 CJK 的混合文案，用户看到 `platform_admin`/`staff` 等英文枚举，违背 PGM-066「提示、报错必须全部为中文」与 PGM-001/GEN-015。
2. **声称行为有没有被实际执行验证？** 有。本席未采信操作员「98 PASS/0 FAIL / 全后端 test-compile」自审数。本席在仓库 `.scratch` 临时目录复制 `backend/target/classes`（不编译共享 target），删除被改类后对 after 源 `javac --release 21`，再跑测试；对照结束删除 scratch。共享 `backend/target/classes` mtime 两次核对均未变。未写业务库。
3. **哪条前提没验证就当真？** 本席未跑全后端 `test-compile -DskipTests`。已跑本 diff 直接覆盖的两类测试全集：`M006SecurityRegressionTests` 33 PASS/0 FAIL；`IdempotencyServiceTests` 9 PASS/0 FAIL。未把未跑的全仓编译写成通过。

### 4.2 顺从式（对照拟修与冻结判据）

031：

- 非商户且原范围为 `p` 时，`resolve()` 使用既有 `AccessActor.id()` 与 `organizationId()`，不发明新身份字段。
- 商户仍 `m:{merchantId}`；未认证 `legacyScope()` 先返回 `""`，`resolve()` 因不是 `p` 而保持空范围。
- `UserRecord` 仍经既有 `currentActor()` 转成 `AccessActor`，未丢掉该主体路径。
- 范围变化时键为 `caller:` + 既有 `hashJson`（SHA-256 hex 64 位）= 71 字符，低于 V002 `operation_id VARCHAR(128)`。
- 旧 `legacyId` 命中时抛既有码 `IDEMPOTENCY_CONFLICT`，固定中文说明，不 `replay` 旧 `response_body`，`action` 计数保持 0；OK/IN_PROGRESS/FAILED_RETRYABLE 三类旧记录同一处理。
- 同主体新键首次执行、重试返回同一结果且 action 只跑一次。

032：

- 仅改 `INVALID_ACCOUNT_TYPE` 抛点文案；错误码、白名单集合、审批规则、FieldLabels 未改。
- 真实 `AccessController.createAccessUser` → 真实 `GlobalExceptionHandler.handleApiException` → `FieldLabels.translate`。含 CJK 则原样透传，故根因是去掉英文片段，而不是改翻译器。

版本头均随所在文件改动更新。

### 4.3 对抗式

| 攻击/反例 | 结果 |
|---|---|
| 两公司员工同编号 `company-first-role` | after：两角色、org-a/org-b、id 不同；before：只剩 org-a。根因即共享 `p` |
| 重放旧 `p:` 成功体 `{"value":"old-result"}` | after：冲突且 `actions=0`，仓库仍是原记录；before：期望抛错却无 throwable |
| 伪造请求体 `merchantId` 挤进 `m:` 桶 | 范围取自认证 `AccessActor`，不取 RoleRequest 商户字段 |
| 未认证调用幂等 | `legacyScope()` 在 `currentActor()` 之前返回 `""`，不把匿名流量打进 `p` |
| 无法识别的 principal | 现抛 `UNAUTHENTICATED`，不再落入共享 `p`（比旧 catch 后返回 `p`/`""` 更窄） |
| 键长：`user*16`+`organization*6`+`createRole:`+64 个 `r` | 新键走 hash，测试断言全部 `operationId.length()<=128` |
| 032 只断言异常对象、不经 handler | 测试调用真实 handler；before 响应仍含 `platform_admin`/`staff` 等拉丁字母 |
| `FieldLabels.translate` 遇 CJK 不剥英文 | 已读 L528–529；故必须改抛点，不能靠全局翻译器。本 diff 未改 FieldLabels，符合 032 边界 |
| 商户隔离回归 | after 上 `IdempotencyServiceTests` 既有 `differentCallerScopes…` / `merchantBCannotReplay…` 仍 PASS |

未发现可成立的越权回放、旧结果泄漏、英文枚举回响或无 ID 改动。

## 5. 本席独立验证（类型 4）

隔离约定：`.scratch/m006-b4-r3-grok-*`；`mvn -o dependency:build-classpath -DincludeScope=test` 只写 scratch 内 classpath 文件；`javac -d` 指向 scratch；跑完 `rmtree`；`LEFTOVER []`。

| 对照 | 命令要点 | 可观察结果 |
|---|---|---|
| after 绿（031 公司+032） | 8 源 javac + 跑 `M006SecurityRegressionTests` 全类 | `GREEN_JAVAC_EXIT 0`；`RESULT 33 PASS / 0 FAIL`，含 `companyRequestsHaveIndependentResultsAndRetryOnlyOnce` 与 `invalidAccountTypeResponseIsChinese` |
| after 绿（031 幂等） | 同包 runner 跑 `IdempotencyServiceTests` | 新测 1 PASS；全类 `RESULT 9 PASS / 0 FAIL` |
| before 红 031 公司 | before 的 Auth/Caller/Idempotency + after 测试 | `Expecting actual: ["org-a"] to contain exactly … ["org-a","org-b"]` |
| before 红 031 幂等 | before 的 Caller/Idempotency + after 新测 | `Expecting code to raise a throwable` |
| before 红 032 | before 的 AccessService + after 测试与 handler | 响应 `账号类型「platform_admin」不允许，可选值：[sales, supervisor, outsourced_vendor, merchant_admin, merchant, staff]`，命中 `[A-Za-z]` |
| 共享产物 | 两次核 `backend/target/classes` mtime | 未变（`1785725458.3775005`） |

测试能红：031 公司测打在真实 `AccessController.createRole` + `AuthorizationService` 解析器；032 打在真实 controller + `GlobalExceptionHandler`；031 幂等测打在 `execute` 旧键查找。不是tautology。

## 6. 2.1 分类（本轮 diff，只记录不发问）

- 031 修复：冲突（跨组织串用角色结果）。工程缺陷已对根因下药。
- 032 修复：冲突（展示报错夹带英文枚举）。
- 无添加项、无 HTML 修改、无锁外判据。033 仍否，本轮未改政策源码。

## 7. ponytail-review

`AuthorizationService.java:L204`: shrink: 范围随后被 hash，长度前缀对解析不是负载。`u:{id}:{org}` 更短。不构成缺陷。

其余：复用 `hashJson`、接口 default、不改表结构。Lean already for the authorized IDs.

`net: -2 lines possible.` 不挡 PASS。

## 8. 结论

- **审计结论：PASS**
- **范围结论：PASS**（12 hunk / 6 路径每一行归因 031 或 032；无越界行）
- 新 finding：无
- 031 根因（无 merchant 主体共享 `p`）已消除；032 根因（CJK 透传混合英文抛点）已消除
- 不进入 B5（本席职责止于 B4 raw）；不改源码/HTML/锁/台账

本席未把操作员自审「60 源 javac / 95→98 / 全后端 test-compile exit0」记为本席实跑。本席实跑范围见 §5。
