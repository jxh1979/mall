CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M037 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T12:12:03Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象（D-029，两份）：①主工作区冻结 diff：before tree `f0671f2cc9f80c735f0bb152d55e10894af3bd27` → after tree `4ad8f54ce984346dc01eb680498030753c92620f`，`/home/james/audit-tmp/m037_b3r1.diff`（2584 bytes，SHA256 e18982ea461b932c0c0f89d3a14a274ed7c78d084921ce108ff7a2566658d070），只含 001；②发布变体与 origin/main 的差异 `/home/james/audit-tmp/m037_b3r1_publish.diff`（SHA256 37dac1c3dec8704e06a5b4d35a011546adee030ad2d107487c03a276ecd28d23），含 001＋002，检查点发布的是它。定稿 ID：M037-001、002（队列「是」）。
- 顺序：Grok 已先派（modules/M037-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐行归因（§5 闸 3）

### 1.1 主工作区冻结 diff（14 条 ± 行，全部归 001）

| ± | 归因 ID | 行首 70 字 |
|---|---|---|
| `-` | 001 | // 文件版本：V0.30 |
| `+` | 001 | // 文件版本：V0.31 |
| `+` | 001 |     // M037-001：两道闸各自锁住——不持有的码（无论是否须审批）被「不得授出自己不持有的权限」拦下；持有的高危码被「须审批」 |
| `+` | 001 |         String daily = "ops.production_readiness.check"; |
| `+` | 001 |         var definitions = new ArrayList<>(repository.listPermissions( |
| `+` | 001 |         definitions.add(new PermissionDefinition(daily, "检查", "ops",  |
| `+` | 001 |         when(repository.listPermissions()).thenReturn(definitions); |
| `-` | 001 |                 .isInstanceOf(ApiException.class); |
| `+` | 001 |                 .isInstanceOfSatisfying(ApiException.class, e -> asse |
| `+` | 001 |         assertThatThrownBy(() -> service.updatePermissionTemplate(t.i |
| `+` | 001 |                 .isInstanceOfSatisfying(ApiException.class, e -> asse |
| `+` | 001 |         effect("manager", HIGH, AccessRepository.Effect.GRANT, "own-h |
| `+` | 001 |         assertThatThrownBy(() -> service.updatePermissionTemplate(t.i |
| `+` | 001 |                 .isInstanceOfSatisfying(ApiException.class, e -> asse |

### 1.2 发布变体 diff（32 条 ± 行）

| ± | 归因 ID | 行首 70 字 |
|---|---|---|
| `-` | 001/002 | // 文件版本：V0.30 |
| `+` | 001/002 | // 文件版本：V0.31 |
| `+` | 001 |     // M037-001：两道闸各自锁住——不持有的码（无论是否须审批）被「不得授出自己不持有的权限」拦下；持有的高危码被「须审批」 |
| `+` | 001 |         String daily = "ops.production_readiness.check"; |
| `+` | 001 |         var definitions = new ArrayList<>(repository.listPermissions( |
| `+` | 001 |         definitions.add(new PermissionDefinition(daily, "检查", "ops",  |
| `+` | 001 |         when(repository.listPermissions()).thenReturn(definitions); |
| `-` | 001 |                 .isInstanceOf(ApiException.class); |
| `+` | 001 |                 .isInstanceOfSatisfying(ApiException.class, e -> asse |
| `+` | 001 |         assertThatThrownBy(() -> service.updatePermissionTemplate(t.i |
| `+` | 001 |                 .isInstanceOfSatisfying(ApiException.class, e -> asse |
| `+` | 001 |         effect("manager", HIGH, AccessRepository.Effect.GRANT, "own-h |
| `+` | 001 |         assertThatThrownBy(() -> service.updatePermissionTemplate(t.i |
| `+` | 001 |                 .isInstanceOfSatisfying(ApiException.class, e -> asse |
| `+` | 002 |         // M037-002：非平台管理员不得操作岗位模板生命周期，且不落任何写入。 |
| `+` | 002 |         actor("ordinary-staff", "staff", "org", null); |
| `+` | 002 |         assertThatThrownBy(() -> service.changePositionTemplateLifecy |
| `+` | 002 |                 new StatusChangeRequest("staff-archive", "archive", " |
| `+` | 002 |                 .isInstanceOfSatisfying(ApiException.class, e -> asse |
| `+` | 002 |         verify(repository, never()).savePositionTemplateLifecycle(any |
| `+` | 002 |         actor("admin", "platform_admin", "org", null); |
| `-` | 002 |                 new StatusChangeRequest("ordinary-admin", "approve_de |
| `+` | 002 |                 new StatusChangeRequest("ordinary-admin", "approve_de |
| `+` | 002 |                 .isInstanceOfSatisfying(ApiException.class, e -> asse |
| `+` | 002 |         // M037-002：最高权限驳回删除——不软删、清掉删除复核单号，之后可再次申请并批准。 |
| `+` | 002 |         var masterAuthentication = SecurityContextHolder.getContext() |
| `+` | 002 |         service.changePositionTemplateLifecycle("lifecycle", new Stat |
| `+` | 002 |         assertThat(mapping.get().deletedAt()).isNull(); |
| `+` | 002 |         assertThat(mapping.get().deleteReviewId()).isNull(); |
| `+` | 002 |         actor("admin", "platform_admin", "org", null); |
| `+` | 002 |         service.changePositionTemplateLifecycle("lifecycle", new Stat |
| `+` | 002 |         SecurityContextHolder.getContext().setAuthentication(masterAu |

- 两份 diff 都只触及 M006SecurityRegressionTests.java 一个文件。模板编辑那条测试的改动归 001，生命周期那条测试的改动归 002，版本号行两个 ID 共用。程序比对：两份 diff 里 001 的新增行逐字相同＝True。无归因不到的行；无 mode 变更；被测代码、迁移、三份 HTML、锁未动。归因票：**PASS**。

## 2. diff 全文

### 2.1 主工作区冻结 diff

```diff
diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
index 2d815f0e..1202d4a3 100644
--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.30
+// 文件版本：V0.31
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.ArgumentMatchers.*;
@@ -173,12 +173,22 @@ class M006SecurityRegressionTests {
     }
 
     // M006-005：已应用模板的内容编辑不能把不持有的高危权限发出去。
+    // M037-001：两道闸各自锁住——不持有的码（无论是否须审批）被「不得授出自己不持有的权限」拦下；持有的高危码被「须审批」拦下。
     @Test void templateEditCannotGrantAnUnownedHighPermission() {
+        String daily = "ops.production_readiness.check";
+        var definitions = new ArrayList<>(repository.listPermissions());
+        definitions.add(new PermissionDefinition(daily, "检查", "ops", "medium", false, "active"));
+        when(repository.listPermissions()).thenReturn(definitions);
         actor("manager", "staff", "org", null);
         effect("manager", LOW, AccessRepository.Effect.GRANT, "own", false);
         PermissionTemplate t = service.createPermissionTemplate(template("create", LOW));
         assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), template("edit", HIGH)))
-                .isInstanceOf(ApiException.class);
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS"));
+        assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), template("edit-daily", LOW, daily)))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS"));
+        effect("manager", HIGH, AccessRepository.Effect.GRANT, "own-high", false);
+        assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), template("edit-owned-high", LOW, HIGH)))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL"));
         assertThat(items.get(t.id())).extracting(PermissionTemplateItem::permissionCode).containsExactly(LOW);
     }
```

### 2.2 发布变体 diff

```diff
diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
index c83e3c77..09c86cb6 100644
--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.30
+// 文件版本：V0.31
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.ArgumentMatchers.*;
@@ -173,12 +173,22 @@ class M006SecurityRegressionTests {
     }
 
     // M006-005：已应用模板的内容编辑不能把不持有的高危权限发出去。
+    // M037-001：两道闸各自锁住——不持有的码（无论是否须审批）被「不得授出自己不持有的权限」拦下；持有的高危码被「须审批」拦下。
     @Test void templateEditCannotGrantAnUnownedHighPermission() {
+        String daily = "ops.production_readiness.check";
+        var definitions = new ArrayList<>(repository.listPermissions());
+        definitions.add(new PermissionDefinition(daily, "检查", "ops", "medium", false, "active"));
+        when(repository.listPermissions()).thenReturn(definitions);
         actor("manager", "staff", "org", null);
         effect("manager", LOW, AccessRepository.Effect.GRANT, "own", false);
         PermissionTemplate t = service.createPermissionTemplate(template("create", LOW));
         assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), template("edit", HIGH)))
-                .isInstanceOf(ApiException.class);
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS"));
+        assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), template("edit-daily", LOW, daily)))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS"));
+        effect("manager", HIGH, AccessRepository.Effect.GRANT, "own-high", false);
+        assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), template("edit-owned-high", LOW, HIGH)))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL"));
         assertThat(items.get(t.id())).extracting(PermissionTemplateItem::permissionCode).containsExactly(LOW);
     }
 
@@ -582,6 +592,13 @@ class M006SecurityRegressionTests {
                 reviewRepository, new AuditEventService(new InMemoryAuditEventRepository()), event -> { });
         service = new AccessService(repository, new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                 new AuditEventService(new InMemoryAuditEventRepository()), authorization, credentials, reviews, true, 3, 3);
+        // M037-002：非平台管理员不得操作岗位模板生命周期，且不落任何写入。
+        actor("ordinary-staff", "staff", "org", null);
+        assertThatThrownBy(() -> service.changePositionTemplateLifecycle("lifecycle",
+                new StatusChangeRequest("staff-archive", "archive", "越权操作", List.of())))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PLATFORM_ADMIN_REQUIRED"));
+        verify(repository, never()).savePositionTemplateLifecycle(any(), anyString());
+        actor("admin", "platform_admin", "org", null);
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("archive", "archive", "停止使用", List.of()));
         assertThat(mapping.get().archivedAt()).isNotNull();
         assertThat(service.listPositionRoleTemplates().items()).isEmpty();
@@ -590,10 +607,19 @@ class M006SecurityRegressionTests {
         doReturn(0L).when(repository).countPositionTemplateUsage("lifecycle");
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("request", "request_delete", "删除申请", List.of()));
         assertThatThrownBy(() -> service.changePositionTemplateLifecycle("lifecycle",
-                new StatusChangeRequest("ordinary-admin", "approve_delete", "同意", List.of()))).isInstanceOf(ApiException.class);
+                new StatusChangeRequest("ordinary-admin", "approve_delete", "同意", List.of())))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_REQUIRED"));
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                 new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                         com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"), "unused", List.of()));
+        // M037-002：最高权限驳回删除——不软删、清掉删除复核单号，之后可再次申请并批准。
+        var masterAuthentication = SecurityContextHolder.getContext().getAuthentication();
+        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("reject", "reject_delete", "保留配置", List.of()));
+        assertThat(mapping.get().deletedAt()).isNull();
+        assertThat(mapping.get().deleteReviewId()).isNull();
+        actor("admin", "platform_admin", "org", null);
+        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("request-again", "request_delete", "重新申请", List.of()));
+        SecurityContextHolder.getContext().setAuthentication(masterAuthentication);
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("approve", "approve_delete", "同意", List.of()));
         assertThat(mapping.get().deletedAt()).isNotNull();
         assertThat(service.listPositionRoleTemplates(true).items()).hasSize(1);
```

## 3. 内容审计

- 001：测试内临时往字典里加一条不须审批、操作人未持有的第三码；原场景（不持有且高危）核 `GRANT_EXCEEDS_OWN_PERMISSIONS`（第一道闸先抛）；「LOW＋第三码」核同码；给操作人补上 HIGH 的明确授予后「LOW＋HIGH」核 `GRANT_REQUIRES_APPROVAL`；末尾模板项仍只有 LOW。三次用不同的幂等键。变异下：拿掉第一道闸→原场景得到 GRANT_REQUIRES_APPROVAL 而红；拿掉第二道闸→第三个断言不抛而红。
- 002（只在发布变体）：先以 staff 主体调 archive，核 `PLATFORM_ADMIN_REQUIRED` 且 `savePositionTemplateLifecycle` 未被调用，再切回平台管理员走原流程；「普通管理员批准删除」核 `MASTER_AUTHORITY_REQUIRED`；主会话 `reject_delete` 后断言未软删、删除复核单号清空，切回平台管理员再次申请、主会话批准，后续原断言不变。主会话夹具沿用 origin/main 的 MASTER_USER_ID＋platform_admin，未引入未发布常量。三个对应变异下各自红。
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m037_b3r1_run.log）

```
START=2026-09-21T12:07:58Z HEAD=e456ba96 origin/main=40fcc8f1
before tree=f0671f2cc9f80c735f0bb152d55e10894af3bd27
scratch at 40fcc8f1
== BASE（origin/main 原样）
MVN_EXIT(base)=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
== 打补丁（发布变体：origin/main 版＋001＋002）
patched origin ['001a', '001b', '002a', '002b', '002c'] V0.30 -> V0.31
== RED：补丁＋变异 template_edit_no_own_gate
mutated template_edit_no_own_gate access/AccessService.java
MVN_EXIT(red_template_edit_no_own_gate)=1
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=1
       failure templateEditCannotGrantAnUnownedHighPermission | &#10;expected: &quot;GRANT_EXCEEDS_OWN_PERMISSIONS&quot;&#10; but was: &quot;GRANT_REQUIRES_APPROVAL&quot;
== RED：补丁＋变异 template_edit_no_approval_gate
mutated template_edit_no_approval_gate access/AccessService.java
MVN_EXIT(red_template_edit_no_approval_gate)=1
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=1
       failure templateEditCannotGrantAnUnownedHighPermission | &#10;Expecting code to raise a throwable.
== RED：补丁＋变异 lifecycle_no_platform_admin
mutated lifecycle_no_platform_admin access/AccessService.java
MVN_EXIT(red_lifecycle_no_platform_admin)=1
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=1
       failure positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval | &#10;Expecting code to raise a throwable.
== RED：补丁＋变异 lifecycle_no_master
mutated lifecycle_no_master access/AccessService.java
MVN_EXIT(red_lifecycle_no_master)=1
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=1
       failure positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval | &#10;expected: &quot;MASTER_AUTHORITY_REQUIRED&quot;&#10; but was: &quot;SUPERVISOR_SELF_REVIEW_FORBIDDEN&quot;
== RED：补丁＋变异 reject_delete_as_approve
mutated reject_delete_as_approve access/AccessService.java
MVN_EXIT(red_reject_delete_as_approve)=1
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=1
       failure positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval | &#10;expected: null&#10; but was: 2026-09-21T12:08:58.003601156Z
== GREEN：补丁、无变异
MVN_EXIT(green)=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
发布变体 sha256: c459a5d2246f05f40ea0a454fc6103e0bdf5ed07ac9cc89fb4f70e85ccd28273 835 行
发布变体与 origin/main 的差异: 6014 bytes sha256 37dac1c3dec8704e06a5b4d35a011546adee030ad2d107487c03a276ecd28d23 [ 1 file changed, 29 insertions(+), 3 deletions(-)]
scratch dirty after cleanup: 0
scratch removed
== 主工作区落补丁（只落 001；002 的断言该版已有）
patched main ['001a', '001b'] V0.30 -> V0.31
after tree=4ad8f54ce984346dc01eb680498030753c92620f
frozen diff: 2584 bytes sha256 e18982ea461b932c0c0f89d3a14a274ed7c78d084921ce108ff7a2566658d070
 .../platform/access/M006SecurityRegressionTests.java       | 14 ++++++++++++--
 1 file changed, 12 insertions(+), 2 deletions(-)
主工作区版 sha256: ce3a9006cb546eb6cb0e743cbe6a47f4c7c2b979f60d41e5bd2b009b8de78f7d 833 行
== 主工作区现状对照（含未发布改动，非发布口径）
MVN_EXIT(main worktree)=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T12:09:15Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
