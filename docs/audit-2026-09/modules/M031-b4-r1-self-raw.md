CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M031 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T05:01:45Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M031 B3-r1 修复 diff：before tree `9d8468b35cb4617044df7c61fd72a26f9083b337` → after tree `f2b1cd10fc7a0fa71faec2e4f78a5f9ed95a4826`；冻结 diff `/home/james/audit-tmp/m031_b3r1.diff`（18200 bytes，SHA256 e1d337122226fd5bb839ae20ab0893b6c3a8ae3dd9f171bea15199e4b43b856d），全文见 §2。定稿 ID：M031-001、002、003（队列「是」）。
- 顺序：Grok 已先派（modules/M031-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐行归因（§5 闸 3）

| 文件 | ± | 归因 ID | 行首 64 字 |
|---|---|---|---|
| AccessService.java | `-` | 版本标记（001/003 共用） | // 文件版本：V0.20 |
| AccessService.java | `+` | 版本标记（001/003 共用） | // 文件版本：V0.21 |
| AccessService.java | `+` | 003 |         // M031-003（GEN-011）：生命周期操作失败也留痕，与同文件其余写口同一范式。 |
| AccessService.java | `+` | 003 |         try { |
| AccessService.java | `+` | 003 |             return changePositionTemplateLifecycleAudited(id, r |
| AccessService.java | `+` | 003 |         } catch (RuntimeException exception) { |
| AccessService.java | `+` | 003 |             auditEventService.recordFailure("access.position_ro |
| AccessService.java | `+` | 003 |                     "position_role_template", id, null, request |
| AccessService.java | `+` | 003 |             throw exception; |
| AccessService.java | `+` | 003 |         } |
| AccessService.java | `+` | 003 |     } |
| AccessService.java | `+` | 003 |  |
| AccessService.java | `+` | 003 |     private PositionRoleTemplate changePositionTemplateLifecycl |
| AccessService.java | `+` | 001 |                     // M031-001（BSM-004「想恢复点"取消存档"即可」）：恢复即回到可定岗 |
| AccessService.java | `+` | 001 |                     if (repository.findPositionRoleTemplate(cur |
| AccessService.java | `+` | 001 |                             .filter(active -> java.util.Objects |
| AccessService.java | `+` | 001 |                         throw ApiException.conflict("POSITION_T |
| AccessService.java | `+` | 001 |                     } |
| AccessService.java | `+` | 001 |             if ("restore".equals(action)) { |
| AccessService.java | `+` | 001 |                 updated = new PositionRoleTemplate(updated.id() |
| AccessService.java | `+` | 001 |                         updated.templateId(), updated.organizat |
| AccessService.java | `+` | 001 |                 repository.savePositionRoleTemplate(updated); |
| AccessService.java | `+` | 001 |             } |
| AccessService.java | `+` | 003 |         // M031-003（GEN-011）：授权政策保存失败也留痕。 |
| AccessService.java | `+` | 003 |         try { |
| AccessService.java | `+` | 003 |             return updateGrantPolicyAudited(request); |
| AccessService.java | `+` | 003 |         } catch (RuntimeException exception) { |
| AccessService.java | `+` | 003 |             auditEventService.recordFailure("access.grant_polic |
| AccessService.java | `+` | 003 |                     "supervisor_review_policy", "access_grant", |
| AccessService.java | `+` | 003 |             throw exception; |
| AccessService.java | `+` | 003 |         } |
| AccessService.java | `+` | 003 |     } |
| AccessService.java | `+` | 003 |  |
| AccessService.java | `+` | 003 |     private AccessGrantPolicy updateGrantPolicyAudited(AccessGr |
| AccessServiceTests.java | `-` | 版本标记（001/002/003 共用） | // 文件版本：V0.06 |
| AccessServiceTests.java | `+` | 版本标记（001/002/003 共用） | // 文件版本：V0.07 |
| AccessServiceTests.java | `+` | 001 |     // M031-001（BSM-003；业务地图原文「想恢复点"取消存档"即可」）：存档后恢复即回到可定岗的启用态。 |
| AccessServiceTests.java | `+` | 001 |     @Test |
| AccessServiceTests.java | `+` | 001 |     void restoringAnArchivedPositionMappingMakesItAssignableAga |
| AccessServiceTests.java | `+` | 001 |         authenticateAsPlatformAdmin("seed-org"); |
| AccessServiceTests.java | `+` | 001 |         PositionRoleTemplate mapping = seedRestoreFixture("usr_ |
| AccessServiceTests.java | `+` | 001 |         service.changePositionTemplateLifecycle(mapping.id(), n |
| AccessServiceTests.java | `+` | 001 |         assertThatThrownBy(() -> service.assignUserPosition(new |
| AccessServiceTests.java | `+` | 001 |                 "assign-while-archived", null, "usr_restore", " |
| AccessServiceTests.java | `+` | 001 |                 .isInstanceOfSatisfying(ApiException.class, e - |
| AccessServiceTests.java | `+` | 001 |  |
| AccessServiceTests.java | `+` | 001 |         PositionRoleTemplate restored = service.changePositionT |
| AccessServiceTests.java | `+` | 001 |                 new StatusChangeRequest("restore-restore", "res |
| AccessServiceTests.java | `+` | 001 |  |
| AccessServiceTests.java | `+` | 001 |         assertThat(restored.status()).isEqualTo("active"); |
| AccessServiceTests.java | `+` | 001 |         PositionRoleTemplate stored = accessRepository.findPosi |
| AccessServiceTests.java | `+` | 001 |         assertThat(stored.status()).isEqualTo("active"); |
| AccessServiceTests.java | `+` | 001 |         assertThat(stored.archivedAt()).isNull(); |
| AccessServiceTests.java | `+` | 001 |         service.assignUserPosition(new UserPositionAssignmentRe |
| AccessServiceTests.java | `+` | 001 |                 "assign-after-restore", null, "usr_restore", "o |
| AccessServiceTests.java | `+` | 001 |         assertThat(accessRepository.positionRoles.get("usr_rest |
| AccessServiceTests.java | `+` | 001 |     } |
| AccessServiceTests.java | `+` | 001 |  |
| AccessServiceTests.java | `+` | 001 |     // M031-001：同岗位同职级已有启用中的替代映射时，恢复被拒且原映射状态不变。 |
| AccessServiceTests.java | `+` | 001 |     @Test |
| AccessServiceTests.java | `+` | 001 |     void restoringIsRejectedWhileAnActiveReplacementMappingExis |
| AccessServiceTests.java | `+` | 001 |         authenticateAsPlatformAdmin("seed-org"); |
| AccessServiceTests.java | `+` | 001 |         PositionRoleTemplate mapping = seedRestoreFixture("usr_ |
| AccessServiceTests.java | `+` | 001 |         service.changePositionTemplateLifecycle(mapping.id(), n |
| AccessServiceTests.java | `+` | 001 |         service.createPositionRoleTemplate(new PositionRoleTemp |
| AccessServiceTests.java | `+` | 001 |                 "map-restore-replacement", null, "ops_restore_b |
| AccessServiceTests.java | `+` | 001 |  |
| AccessServiceTests.java | `+` | 001 |         assertThatThrownBy(() -> service.changePositionTemplate |
| AccessServiceTests.java | `+` | 001 |                 new StatusChangeRequest("blocked-restore", "res |
| AccessServiceTests.java | `+` | 001 |                 .isInstanceOfSatisfying(ApiException.class, e - |
| AccessServiceTests.java | `+` | 001 |                     assertThat(e.status().value()).isEqualTo(40 |
| AccessServiceTests.java | `+` | 001 |                     assertThat(e.code()).isEqualTo("POSITION_TE |
| AccessServiceTests.java | `+` | 001 |                 }); |
| AccessServiceTests.java | `+` | 001 |  |
| AccessServiceTests.java | `+` | 001 |         PositionRoleTemplate unchanged = accessRepository.findP |
| AccessServiceTests.java | `+` | 001 |         assertThat(unchanged.status()).isEqualTo("inactive"); |
| AccessServiceTests.java | `+` | 001 |         assertThat(unchanged.archivedAt()).isNotNull(); |
| AccessServiceTests.java | `+` | 001 |     } |
| AccessServiceTests.java | `+` | 001 |  |
| AccessServiceTests.java | `+` | 001 |     private PositionRoleTemplate seedRestoreFixture(String user |
| AccessServiceTests.java | `+` | 001 |         accessRepository.users.put(userId, new AccessUser( |
| AccessServiceTests.java | `+` | 001 |                 userId, userId, "staff", "seed-org", null, "act |
| AccessServiceTests.java | `+` | 001 |         accessRepository.roles.put("role_restore", new RoleReco |
| AccessServiceTests.java | `+` | 001 |                 "role_restore", "role_restore", "Ops checker",  |
| AccessServiceTests.java | `+` | 001 |                 Instant.parse("2026-06-14T00:00:00Z"), List.of( |
| AccessServiceTests.java | `+` | 001 |         accessRepository.saveTemplate(new PermissionTemplate( |
| AccessServiceTests.java | `+` | 001 |                 "tpl_restore", "tpl_restore", "Ops Check", null |
| AccessServiceTests.java | `+` | 001 |                 null, "platform", null, "merge", null, null, fa |
| AccessServiceTests.java | `+` | 001 |                 "active", null, null, null, |
| AccessServiceTests.java | `+` | 001 |                 Instant.parse("2026-06-14T00:00:00Z"), |
| AccessServiceTests.java | `+` | 001 |                 Instant.parse("2026-06-14T00:00:00Z")), |
| AccessServiceTests.java | `+` | 001 |                 List.of(new PermissionTemplateItem( |
| AccessServiceTests.java | `+` | 001 |                         "tpl_restore", "ops.production_readines |
| AccessServiceTests.java | `+` | 001 |         return service.createPositionRoleTemplate(new PositionR |
| AccessServiceTests.java | `+` | 001 |                 idempotencyKey, null, positionCode, "lvl_m1_low |
| AccessServiceTests.java | `+` | 001 |     } |
| AccessServiceTests.java | `+` | 001 |  |
| AccessServiceTests.java | `+` | 002 |     // M031-002（治理地图「系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效」）： |
| AccessServiceTests.java | `+` | 002 |     // 保存岗位映射后在岗人员按新映射重算、薪资不变；映射停用则收回派生。 |
| AccessServiceTests.java | `+` | 002 |     @Test |
| AccessServiceTests.java | `+` | 002 |     void savingAPositionMappingRecomputesAssignedUsersAndDeacti |
| AccessServiceTests.java | `+` | 002 |         authenticateAsPlatformAdmin("seed-org"); |
| AccessServiceTests.java | `+` | 002 |         accessRepository.users.put("usr_recompute", new AccessU |
| AccessServiceTests.java | `+` | 002 |                 "usr_recompute", "recompute", "staff", "seed-or |
| AccessServiceTests.java | `+` | 002 |                 Instant.parse("2026-06-14T00:00:00Z"))); |
| AccessServiceTests.java | `+` | 002 |         for (String roleId : List.of("role_recompute_old", "rol |
| AccessServiceTests.java | `+` | 002 |             accessRepository.roles.put(roleId, new RoleRecord( |
| AccessServiceTests.java | `+` | 002 |                     roleId, roleId, "Ops checker", "project", " |
| AccessServiceTests.java | `+` | 002 |                     Instant.parse("2026-06-14T00:00:00Z"), |
| AccessServiceTests.java | `+` | 002 |                     List.of("ops.production_readiness.check"), |
| AccessServiceTests.java | `+` | 002 |                     null)); |
| AccessServiceTests.java | `+` | 002 |         } |
| AccessServiceTests.java | `+` | 002 |         accessRepository.saveTemplate(new PermissionTemplate( |
| AccessServiceTests.java | `+` | 002 |                 "tpl_recompute", "tpl_recompute", "Ops Check",  |
| AccessServiceTests.java | `+` | 002 |                 null, "platform", null, "merge", null, null, fa |
| AccessServiceTests.java | `+` | 002 |                 "active", null, null, null, |
| AccessServiceTests.java | `+` | 002 |                 Instant.parse("2026-06-14T00:00:00Z"), |
| AccessServiceTests.java | `+` | 002 |                 Instant.parse("2026-06-14T00:00:00Z")), |
| AccessServiceTests.java | `+` | 002 |                 List.of(new PermissionTemplateItem( |
| AccessServiceTests.java | `+` | 002 |                         "tpl_recompute", |
| AccessServiceTests.java | `+` | 002 |                         "ops.production_readiness.check", |
| AccessServiceTests.java | `+` | 002 |                         AccessRepository.Effect.GRANT, |
| AccessServiceTests.java | `+` | 002 |                         "project", |
| AccessServiceTests.java | `+` | 002 |                         false))); |
| AccessServiceTests.java | `+` | 002 |         PositionRoleTemplate mapping = service.createPositionRo |
| AccessServiceTests.java | `+` | 002 |                 "map-recompute", null, "ops_recompute", "lvl_m1 |
| AccessServiceTests.java | `+` | 002 |                 "role_recompute_old", "tpl_recompute", "active" |
| AccessServiceTests.java | `+` | 002 |         service.assignUserPosition(new UserPositionAssignmentRe |
| AccessServiceTests.java | `+` | 002 |                 "assign-recompute", null, "usr_recompute", "ops |
| AccessServiceTests.java | `+` | 002 |                 500000L, "CNY", null)); |
| AccessServiceTests.java | `+` | 002 |         assertThat(accessRepository.positionRoles.get("usr_reco |
| AccessServiceTests.java | `+` | 002 |  |
| AccessServiceTests.java | `+` | 002 |         service.updatePositionRoleTemplate(mapping.id(), new Po |
| AccessServiceTests.java | `+` | 002 |                 "map-recompute-change", null, "ops_recompute",  |
| AccessServiceTests.java | `+` | 002 |                 "role_recompute_new", "tpl_recompute", "active" |
| AccessServiceTests.java | `+` | 002 |  |
| AccessServiceTests.java | `+` | 002 |         assertThat(accessRepository.positionRoles.get("usr_reco |
| AccessServiceTests.java | `+` | 002 |         assertThat(accessRepository.findStaffLevelAssignment("u |
| AccessServiceTests.java | `+` | 002 |                 .extracting(StaffLevelAssignment::baseSalaryAmo |
| AccessServiceTests.java | `+` | 002 |                 .isEqualTo(500000L); |
| AccessServiceTests.java | `+` | 002 |  |
| AccessServiceTests.java | `+` | 002 |         service.updatePositionRoleTemplate(mapping.id(), new Po |
| AccessServiceTests.java | `+` | 002 |                 "map-recompute-off", null, "ops_recompute", "lv |
| AccessServiceTests.java | `+` | 002 |                 "role_recompute_new", "tpl_recompute", "inactiv |
| AccessServiceTests.java | `+` | 002 |  |
| AccessServiceTests.java | `+` | 002 |         assertThat(accessRepository.positionRoles).containsKey( |
| AccessServiceTests.java | `+` | 002 |         assertThat(accessRepository.positionRoles.get("usr_reco |
| AccessServiceTests.java | `+` | 002 |     } |
| AccessServiceTests.java | `+` | 002 |  |
| AccessServiceTests.java | `+` | 003 |     // M031-003（GEN-011）：岗位模板生命周期与授权政策两个写口失败也留痕。 |
| AccessServiceTests.java | `+` | 003 |     @Test |
| AccessServiceTests.java | `+` | 003 |     void positionTemplateLifecycleAndGrantPolicyFailuresAreAudi |
| AccessServiceTests.java | `+` | 003 |         authenticateAsPlatformAdmin("seed-org"); |
| AccessServiceTests.java | `+` | 003 |         assertThatThrownBy(() -> service.changePositionTemplate |
| AccessServiceTests.java | `+` | 003 |                 new StatusChangeRequest("lifecycle-missing", "a |
| AccessServiceTests.java | `+` | 003 |                 .isInstanceOfSatisfying(ApiException.class, e - |
| AccessServiceTests.java | `+` | 003 |         assertThatThrownBy(() -> service.updateGrantPolicy(null |
| AccessServiceTests.java | `+` | 003 |                 .isInstanceOfSatisfying(ApiException.class, e - |
| AccessServiceTests.java | `+` | 003 |         assertThat(auditEventRepository.events) |
| AccessServiceTests.java | `+` | 003 |                 .filteredOn(event -> "failure".equals(event.eve |
| AccessServiceTests.java | `+` | 003 |                 .extracting(AuditEvent::eventName) |
| AccessServiceTests.java | `+` | 003 |                 .contains("access.position_role_template.lifecy |
| AccessServiceTests.java | `+` | 003 |     } |
| AccessServiceTests.java | `+` | 003 |  |
| AccessServiceTests.java | `+` | 001（内存仓按 Jdbc 语义） |         // 与 JdbcAccessRepository 同语义：upsert 不动存档/删除列；生命周期保存恒写  |
| AccessServiceTests.java | `-` | 001（内存仓按 Jdbc 语义） |             positionRoleTemplates.put(mapping.id(), mapping); |
| AccessServiceTests.java | `+` | 001（内存仓按 Jdbc 语义） |             PositionRoleTemplate existing = positionRoleTemplat |
| AccessServiceTests.java | `+` | 001（内存仓按 Jdbc 语义） |             positionRoleTemplates.put(mapping.id(), existing == |
| AccessServiceTests.java | `+` | 001（内存仓按 Jdbc 语义） |                     mapping.id(), mapping.positionCode(), mappi |
| AccessServiceTests.java | `+` | 001（内存仓按 Jdbc 语义） |                     mapping.organizationId(), mapping.status(), |
| AccessServiceTests.java | `+` | 001（内存仓按 Jdbc 语义） |                     existing.archivedAt(), existing.deletedAt() |
| AccessServiceTests.java | `+` | 001（内存仓按 Jdbc 语义） |         @Override |
| AccessServiceTests.java | `+` | 001（内存仓按 Jdbc 语义） |         public void savePositionTemplateLifecycle(PositionRoleT |
| AccessServiceTests.java | `+` | 001（内存仓按 Jdbc 语义） |             positionRoleTemplates.put(mapping.id(), mapping.wit |
| AccessServiceTests.java | `+` | 001（内存仓按 Jdbc 语义） |                     mapping.deleteReviewId(), mapping.deleteRea |
| AccessServiceTests.java | `+` | 001（内存仓按 Jdbc 语义） |         } |
| AccessServiceTests.java | `+` | 001（内存仓按 Jdbc 语义） |  |
| M006SecurityRegressionTests.java | `-` | 版本标记（001） | // 文件版本：V0.29 |
| M006SecurityRegressionTests.java | `+` | 版本标记（001） | // 文件版本：V0.30 |
| M006SecurityRegressionTests.java | `+` | 001 |         doAnswer(a -> { mapping.set(a.getArgument(0)); return a |
| M006SecurityRegressionTests.java | `-` | 001 |         assertThat(mapping.get().status()).isEqualTo("inactive" |
| M006SecurityRegressionTests.java | `+` | 001 |         assertThat(mapping.get().status()).isEqualTo("active"); |

- 3 文件 11 个 hunk、182 条 ± 行。AccessService.java：版本标记；生命周期与授权政策两处 try/catch 外壳归 003；restore 分支的替代映射检查与生命周期保存后的 upsert 归 001。AccessServiceTests.java：版本标记；001 两条测试与夹具助手、内存仓按 Jdbc 语义的两个方法归 001；重算测试归 002；失败留痕测试归 003。M006SecurityRegressionTests.java：版本标记、upsert 桩、恢复后断言改 active 归 001。无归因不到的行；无 mode 变更；M032 两个文件、AuditEventService、OpenAPI、三份 HTML、锁未动。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/backend/src/main/java/com/complex/platform/access/AccessService.java b/backend/src/main/java/com/complex/platform/access/AccessService.java
index 3720af8c..de7e6c4f 100644
--- a/backend/src/main/java/com/complex/platform/access/AccessService.java
+++ b/backend/src/main/java/com/complex/platform/access/AccessService.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.20
+// 文件版本：V0.21
 
 import com.complex.platform.access.dto.AccessGrantApplyRequest;
 import com.complex.platform.access.dto.AccessGrantReviewRequest;
@@ -484,6 +484,17 @@ public class AccessService {
 
     @Transactional
     public PositionRoleTemplate changePositionTemplateLifecycle(String id, StatusChangeRequest request) {
+        // M031-003（GEN-011）：生命周期操作失败也留痕，与同文件其余写口同一范式。
+        try {
+            return changePositionTemplateLifecycleAudited(id, request);
+        } catch (RuntimeException exception) {
+            auditEventService.recordFailure("access.position_role_template.lifecycle_failed", "access.role.assign",
+                    "position_role_template", id, null, request == null ? null : request.idempotencyKey(), codeOf(exception));
+            throw exception;
+        }
+    }
+
+    private PositionRoleTemplate changePositionTemplateLifecycleAudited(String id, StatusChangeRequest request) {
         requireRequest(request);
         AccessActor actor = authorizationService.currentActor();
         if (!authorizationService.isMasterAuthority(actor)) {
@@ -504,6 +515,11 @@ public class AccessService {
                 case "restore" -> {
                     if (current.archivedAt() == null) throw ApiException.conflict("INVALID_STATE", "模板未存档");
                     if (current.deleteReviewId() != null && current.deletedAt() == null) throw ApiException.conflict("DELETE_REVIEW_PENDING", "须先处理删除复核");
+                    // M031-001（BSM-004「想恢复点"取消存档"即可」）：恢复即回到可定岗的启用态；同范围已有启用中的替代映射时拒绝，不静默留成停用。
+                    if (repository.findPositionRoleTemplate(current.organizationId(), current.positionCode(), current.staffLevelId())
+                            .filter(active -> java.util.Objects.equals(active.organizationId(), current.organizationId())).isPresent()) {
+                        throw ApiException.conflict("POSITION_TEMPLATE_ACTIVE_EXISTS", "同岗位同职级已有启用中的岗位模板，须先停用或存档后再恢复");
+                    }
                     updated = current.withLifecycle(null, null, null, null, now);
                 }
                 case "request_delete" -> {
@@ -541,6 +557,11 @@ public class AccessService {
                 default -> throw ApiException.badRequest("INVALID_ACTION", "不支持的岗位模板生命周期操作");
             }
             repository.savePositionTemplateLifecycle(updated, authorizationService.currentActor().id());
+            if ("restore".equals(action)) {
+                updated = new PositionRoleTemplate(updated.id(), updated.positionCode(), updated.staffLevelId(), updated.roleId(),
+                        updated.templateId(), updated.organizationId(), "active", updated.createdBy(), updated.createdAt(), now);
+                repository.savePositionRoleTemplate(updated);
+            }
             auditEventService.recordSuccess("access.position_role_template." + action, "access.role.assign",
                     "position_role_template", id, null, request.idempotencyKey(), Map.of("reason", reason));
             return updated;
@@ -1921,6 +1942,17 @@ public class AccessService {
 
     @Transactional
     public AccessGrantPolicy updateGrantPolicy(AccessGrantPolicy.Update request) {
+        // M031-003（GEN-011）：授权政策保存失败也留痕。
+        try {
+            return updateGrantPolicyAudited(request);
+        } catch (RuntimeException exception) {
+            auditEventService.recordFailure("access.grant_policy.change_failed", "admission.policy.manage",
+                    "supervisor_review_policy", "access_grant", null, request == null ? null : request.idempotencyKey(), codeOf(exception));
+            throw exception;
+        }
+    }
+
+    private AccessGrantPolicy updateGrantPolicyAudited(AccessGrantPolicy.Update request) {
         if (request == null || request.policy() == null) throw ApiException.badRequest("VALIDATION_FAILED", "授权政策不能为空");
         requireText(request.operatorNote(), "operator_note");
         return idempotencyService.execute("updateAccessGrantPolicy", request.idempotencyKey(), request, () -> {
diff --git a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
index e24ea166..1c34f772 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.06
+// 文件版本：V0.07
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
@@ -1503,6 +1503,134 @@ class AccessServiceTests {
                 "pending", null, "scope test", "usr_requester", null, null, projectId, null, now, now);
     }
 
+    // M031-001（BSM-003；业务地图原文「想恢复点"取消存档"即可」）：存档后恢复即回到可定岗的启用态。
+    @Test
+    void restoringAnArchivedPositionMappingMakesItAssignableAgain() {
+        authenticateAsPlatformAdmin("seed-org");
+        PositionRoleTemplate mapping = seedRestoreFixture("usr_restore", "ops_restore", "map-restore");
+        service.changePositionTemplateLifecycle(mapping.id(), new StatusChangeRequest("restore-archive", "archive", "停止使用", List.of()));
+        assertThatThrownBy(() -> service.assignUserPosition(new UserPositionAssignmentRequest(
+                "assign-while-archived", null, "usr_restore", "ops_restore", "lvl_m1_lower", null, null, null)))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("POSITION_ROLE_TEMPLATE_NOT_FOUND"));
+
+        PositionRoleTemplate restored = service.changePositionTemplateLifecycle(mapping.id(),
+                new StatusChangeRequest("restore-restore", "restore", "恢复使用", List.of()));
+
+        assertThat(restored.status()).isEqualTo("active");
+        PositionRoleTemplate stored = accessRepository.findPositionRoleTemplateById(mapping.id()).orElseThrow();
+        assertThat(stored.status()).isEqualTo("active");
+        assertThat(stored.archivedAt()).isNull();
+        service.assignUserPosition(new UserPositionAssignmentRequest(
+                "assign-after-restore", null, "usr_restore", "ops_restore", "lvl_m1_lower", null, null, null));
+        assertThat(accessRepository.positionRoles.get("usr_restore")).isEqualTo("role_restore");
+    }
+
+    // M031-001：同岗位同职级已有启用中的替代映射时，恢复被拒且原映射状态不变。
+    @Test
+    void restoringIsRejectedWhileAnActiveReplacementMappingExists() {
+        authenticateAsPlatformAdmin("seed-org");
+        PositionRoleTemplate mapping = seedRestoreFixture("usr_restore_blocked", "ops_restore_blocked", "map-restore-blocked");
+        service.changePositionTemplateLifecycle(mapping.id(), new StatusChangeRequest("blocked-archive", "archive", "停止使用", List.of()));
+        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
+                "map-restore-replacement", null, "ops_restore_blocked", "lvl_m1_lower", "role_restore", "tpl_restore", "active"));
+
+        assertThatThrownBy(() -> service.changePositionTemplateLifecycle(mapping.id(),
+                new StatusChangeRequest("blocked-restore", "restore", "恢复使用", List.of())))
+                .isInstanceOfSatisfying(ApiException.class, e -> {
+                    assertThat(e.status().value()).isEqualTo(409);
+                    assertThat(e.code()).isEqualTo("POSITION_TEMPLATE_ACTIVE_EXISTS");
+                });
+
+        PositionRoleTemplate unchanged = accessRepository.findPositionRoleTemplateById(mapping.id()).orElseThrow();
+        assertThat(unchanged.status()).isEqualTo("inactive");
+        assertThat(unchanged.archivedAt()).isNotNull();
+    }
+
+    private PositionRoleTemplate seedRestoreFixture(String userId, String positionCode, String idempotencyKey) {
+        accessRepository.users.put(userId, new AccessUser(
+                userId, userId, "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
+        accessRepository.roles.put("role_restore", new RoleRecord(
+                "role_restore", "role_restore", "Ops checker", "project", "active",
+                Instant.parse("2026-06-14T00:00:00Z"), List.of("ops.production_readiness.check"), null));
+        accessRepository.saveTemplate(new PermissionTemplate(
+                "tpl_restore", "tpl_restore", "Ops Check", null, "base",
+                null, "platform", null, "merge", null, null, false,
+                "active", null, null, null,
+                Instant.parse("2026-06-14T00:00:00Z"),
+                Instant.parse("2026-06-14T00:00:00Z")),
+                List.of(new PermissionTemplateItem(
+                        "tpl_restore", "ops.production_readiness.check", AccessRepository.Effect.GRANT, "project", false)));
+        return service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
+                idempotencyKey, null, positionCode, "lvl_m1_lower", "role_restore", "tpl_restore", "active"));
+    }
+
+    // M031-002（治理地图「系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效」）：
+    // 保存岗位映射后在岗人员按新映射重算、薪资不变；映射停用则收回派生。
+    @Test
+    void savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants() {
+        authenticateAsPlatformAdmin("seed-org");
+        accessRepository.users.put("usr_recompute", new AccessUser(
+                "usr_recompute", "recompute", "staff", "seed-org", null, "active",
+                Instant.parse("2026-06-14T00:00:00Z")));
+        for (String roleId : List.of("role_recompute_old", "role_recompute_new")) {
+            accessRepository.roles.put(roleId, new RoleRecord(
+                    roleId, roleId, "Ops checker", "project", "active",
+                    Instant.parse("2026-06-14T00:00:00Z"),
+                    List.of("ops.production_readiness.check"),
+                    null));
+        }
+        accessRepository.saveTemplate(new PermissionTemplate(
+                "tpl_recompute", "tpl_recompute", "Ops Check", null, "base",
+                null, "platform", null, "merge", null, null, false,
+                "active", null, null, null,
+                Instant.parse("2026-06-14T00:00:00Z"),
+                Instant.parse("2026-06-14T00:00:00Z")),
+                List.of(new PermissionTemplateItem(
+                        "tpl_recompute",
+                        "ops.production_readiness.check",
+                        AccessRepository.Effect.GRANT,
+                        "project",
+                        false)));
+        PositionRoleTemplate mapping = service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
+                "map-recompute", null, "ops_recompute", "lvl_m1_lower",
+                "role_recompute_old", "tpl_recompute", "active"));
+        service.assignUserPosition(new UserPositionAssignmentRequest(
+                "assign-recompute", null, "usr_recompute", "ops_recompute", "lvl_m1_lower",
+                500000L, "CNY", null));
+        assertThat(accessRepository.positionRoles.get("usr_recompute")).isEqualTo("role_recompute_old");
+
+        service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
+                "map-recompute-change", null, "ops_recompute", "lvl_m1_lower",
+                "role_recompute_new", "tpl_recompute", "active"));
+
+        assertThat(accessRepository.positionRoles.get("usr_recompute")).isEqualTo("role_recompute_new");
+        assertThat(accessRepository.findStaffLevelAssignment("usr_recompute")).get()
+                .extracting(StaffLevelAssignment::baseSalaryAmountCent)
+                .isEqualTo(500000L);
+
+        service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
+                "map-recompute-off", null, "ops_recompute", "lvl_m1_lower",
+                "role_recompute_new", "tpl_recompute", "inactive"));
+
+        assertThat(accessRepository.positionRoles).containsKey("usr_recompute");
+        assertThat(accessRepository.positionRoles.get("usr_recompute")).isNull();
+    }
+
+    // M031-003（GEN-011）：岗位模板生命周期与授权政策两个写口失败也留痕。
+    @Test
+    void positionTemplateLifecycleAndGrantPolicyFailuresAreAudited() {
+        authenticateAsPlatformAdmin("seed-org");
+        assertThatThrownBy(() -> service.changePositionTemplateLifecycle("prt_missing",
+                new StatusChangeRequest("lifecycle-missing", "archive", "停止使用", List.of())))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("POSITION_ROLE_TEMPLATE_NOT_FOUND"));
+        assertThatThrownBy(() -> service.updateGrantPolicy(null))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
+        assertThat(auditEventRepository.events)
+                .filteredOn(event -> "failure".equals(event.eventResult()))
+                .extracting(AuditEvent::eventName)
+                .contains("access.position_role_template.lifecycle_failed", "access.grant_policy.change_failed");
+    }
+
     private void authenticateAsPlatformAdmin(String organizationId) {
         AuthService.UserRecord user = new AuthService.UserRecord(
                 "usr_admin", "seed-admin", "hash", "platform_admin", organizationId, null, "active",
@@ -1753,12 +1881,23 @@ class AccessServiceTests {
                     .findFirst());
         }
 
+        // 与 JdbcAccessRepository 同语义：upsert 不动存档/删除列；生命周期保存恒写 inactive。
         @Override
         public PositionRoleTemplate savePositionRoleTemplate(PositionRoleTemplate mapping) {
-            positionRoleTemplates.put(mapping.id(), mapping);
+            PositionRoleTemplate existing = positionRoleTemplates.get(mapping.id());
+            positionRoleTemplates.put(mapping.id(), existing == null ? mapping : new PositionRoleTemplate(
+                    mapping.id(), mapping.positionCode(), mapping.staffLevelId(), mapping.roleId(), mapping.templateId(),
+                    mapping.organizationId(), mapping.status(), mapping.createdBy(), mapping.createdAt(), mapping.updatedAt(),
+                    existing.archivedAt(), existing.deletedAt(), existing.deleteReviewId(), existing.deleteReason()));
             return mapping;
         }
 
+        @Override
+        public void savePositionTemplateLifecycle(PositionRoleTemplate mapping, String actorId) {
+            positionRoleTemplates.put(mapping.id(), mapping.withLifecycle(mapping.archivedAt(), mapping.deletedAt(),
+                    mapping.deleteReviewId(), mapping.deleteReason(), mapping.updatedAt()));
+        }
+
         @Override
         public void deletePositionRoleTemplate(String id) {
             positionRoleTemplates.remove(id);
diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
index 3629dd5d..2d815f0e 100644
--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.29
+// 文件版本：V0.30
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.ArgumentMatchers.*;
@@ -557,6 +557,7 @@ class M006SecurityRegressionTests {
         when(repository.findPositionRoleTemplateById("lifecycle")).thenAnswer(a -> Optional.of(mapping.get()));
         when(repository.listPositionRoleTemplates(null)).thenAnswer(a -> List.of(mapping.get()));
         doAnswer(a -> { mapping.set(a.getArgument(0)); return null; }).when(repository).savePositionTemplateLifecycle(any(), anyString());
+        doAnswer(a -> { mapping.set(a.getArgument(0)); return a.getArgument(0); }).when(repository).savePositionRoleTemplate(any());
         doReturn(1L).when(repository).countPositionTemplateUsage("lifecycle");
         var reviewRepository = mock(com.complex.platform.governance.review.SupervisorReviewRepository.class);
         Map<String, com.complex.platform.governance.review.SupervisorReviewDecisionRecord> decisions = new HashMap<>();
@@ -614,7 +615,7 @@ class M006SecurityRegressionTests {
         SecurityContextHolder.getContext().setAuthentication(masterAuthentication);
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("restore", "restore", "恢复留存配置", List.of()));
         assertThat(mapping.get().deletedAt()).isNull();
-        assertThat(mapping.get().status()).isEqualTo("inactive");
+        assertThat(mapping.get().status()).isEqualTo("active"); // M031-001：恢复即回到启用态
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("self-archive", "archive", "停止使用", List.of()));
         int reviewCount = decisions.size();
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("self-request", "request_delete", "最高权限本人删除", List.of()));
```

## 3. 内容审计

- 001：替代映射检查发生在任何写之前，`findPositionRoleTemplate` 只认启用且未存档的行，不会命中正在恢复的自己；`Objects.equals` 处理双方组织皆空。顺序为先 `savePositionTemplateLifecycle`（清存档/删除标记，Jdbc SQL 恒写 inactive）再 `repository.savePositionRoleTemplate`（upsert 只改 status 等七列，不碰生命周期列），反过来会被盖回 inactive；走的是仓储方法，不是会触发重算的私有 `savePositionRoleTemplate`。两步都在幂等动作闭包内，同一事务；`@Transactional` 未动。返回值为启用态对象。内存仓改成与 Jdbc 同语义后，「只 upsert 不先清存档」或「顺序写反」都会让新测试变红。
- 002：只加测试；RED 阶段即绿说明重算实现无缺陷；变异核对证明断言能失败。
- 003：两处外壳与同文件 `executeTemplateWrite` 同范式，原方法体原样下沉为私有方法，成功路径行为不变；事件名、权限码、业务类型按定稿。真库上失败事件仍随事务回滚丢失，直到 XM-M031-B 在 M092 处置——已在定稿与 B3 完成节如实写明。
- 如实记录：首次落盘的 upsert 桩写法有误（`when(...).thenAnswer(...)`），续跑改为 `doAnswer(...).when(...)`；冻结 diff 以续跑后的 after tree 为准，不含错误写法。
- 内容票：**PASS**。

## 4. 可执行证据原文

首跑（/home/james/audit-tmp/m031_b3r1_run.log）：
```
START=2026-09-21T04:54:50Z HEAD=e456ba96 origin/main=508b2e87
BEFORE_TREE=9d8468b35cb4617044df7c61fd72a26f9083b337
before AccessController.java fbdb4667cf1a0811adae1e668dc299b42a07a82ba5abda879cc098bca0c60146
before AccessRepository.java 6fc11453fee5aefbfe97bb270d4f6f88a294101daaab88c62abcf768338b6cca
before AccessService.java f38bff0b90f42703756a4069fafe560a16b834e8c30b34b9bb1b6a57d3e580bb
before AccessServiceTests.java cdf6adc034499ed46411db909f320afd58584c1530f22b0afecbad0fcae36749
before AccessControllerTests.java 466b780026d8bab9c814f5a6269fb76bfd0755c520cba5f1797778bc7a30ea0e
before M006SecurityRegressionTests.java f5ae66e2c12c632c5f5d4c403eb9992ee1d23d175df422a22da10e2057e5fa7e
scratch M006SecurityRegressionTests.java = origin/main 版 9222ef61267af1358937fce257bb570bb491fc8dd0a928b536b1146e001c7f64
== BASE（未打补丁）
base MVN_EXIT=0
   AccessControllerTests tests=13 errors=0 skipped=0 failures=0
   AccessServiceTests tests=50 errors=0 skipped=0 failures=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
== RED（只打测试补丁）
patched AccessServiceTests.java 2 V0.07
patched M006SecurityRegressionTests.java 2 V0.30
red MVN_EXIT=1
   AccessControllerTests tests=13 errors=0 skipped=0 failures=0
   AccessServiceTests tests=54 errors=0 skipped=0 failures=3
       failure positionTemplateLifecycleAndGrantPolicyFailuresAreAudited | &#10;Expecting ArrayList:&#10;  []&#10;to contain:&#10;  [&quot;access.position_role_template.lifecycle_failed&quot;,&#10;    &quot;access.grant_polic
       failure restoringIsRejectedWhileAnActiveReplacementMappingExists | &#10;Expecting code to raise a throwable.
       failure restoringAnArchivedPositionMappingMakesItAssignableAgain | &#10;expected: &quot;active&quot;&#10; but was: &quot;inactive&quot;
   M006SecurityRegressionTests tests=39 errors=1 skipped=0 failures=0
       error positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval | savePositionRoleTemplate not supported by this repository
== GREEN（再打主代码补丁）
patched AccessService.java 4 V0.21
green MVN_EXIT=1
   AccessControllerTests tests=13 errors=0 skipped=0 failures=0
   AccessServiceTests tests=54 errors=0 skipped=0 failures=0
   M006SecurityRegressionTests tests=39 errors=1 skipped=0 failures=0
       error positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval | savePositionRoleTemplate not supported by this repository
== 主工作区打同一补丁
patched AccessServiceTests.java 2 V0.07
patched M006SecurityRegressionTests.java 2 V0.30
patched AccessService.java 4 V0.21
same as scratch: AccessService.java
same as scratch: AccessServiceTests.java
publish variant saved ba99d9b28016c8567330156c91cff315ecf7edfde9c188c0ad3193f1369405bd
AFTER_TREE=04fc6620c0e2fa166cc955c7eda595094a089628
DIFF bytes=18202 sha=c5ac2bb7923b8b3e0dc67ee7d0b331ae2f79516354598788c9304ebfeec8b0a9
 .../com/complex/platform/access/AccessService.java |  34 ++++-
 .../platform/access/AccessServiceTests.java        | 143 ++++++++++++++++++++-
 .../access/M006SecurityRegressionTests.java        |   5 +-
 3 files changed, 177 insertions(+), 5 deletions(-)
scratch removed
END=2026-09-21T04:55:23Z
```
续跑（/home/james/audit-tmp/m031_b3r1_rerun.log）：
```
START=2026-09-21T04:57:13Z BEFORE_TREE=9d8468b35cb4617044df7c61fd72a26f9083b337
patched M006SecurityRegressionTests.java 1 None
patched M006SecurityRegressionTests.java 2 V0.30
== GREEN-2（origin/main＋主工作区现行 5 个文件＋M006SecurityRegressionTests 发布变体）
green2 MVN_EXIT=0
   AccessControllerTests tests=13 errors=0 skipped=0 failures=0
   AccessServiceTests tests=54 errors=0 skipped=0 failures=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
publish variant saved c2d8b8a69779b7c5555cc49c7776f233b0e16bc875c22bfc96149799f48698c3
publish variant vs origin/main:
 1 file changed, 3 insertions(+), 2 deletions(-)
scratch removed
== 主工作区 M006SecurityRegressionTests（含别的模块未发布改动，仅供参考）
main-worktree MVN_EXIT=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
AFTER_TREE=f2b1cd10fc7a0fa71faec2e4f78a5f9ed95a4826
DIFF bytes=18200 sha=e1d337122226fd5bb839ae20ab0893b6c3a8ae3dd9f171bea15199e4b43b856d
 .../com/complex/platform/access/AccessService.java |  34 ++++-
 .../platform/access/AccessServiceTests.java        | 143 ++++++++++++++++++++-
 .../access/M006SecurityRegressionTests.java        |   5 +-
 3 files changed, 177 insertions(+), 5 deletions(-)
after AccessService.java 4556c02cf35206e8e5032855a0e9c6d8915beb3c43f2ae6f77fb1ce615dfeb0d
after AccessServiceTests.java 077ce9e6b00430c7cc7d83abf95601b8bff665a57a95fb53f8458403e1d265f6
after M006SecurityRegressionTests.java 167733bc7c2b9fb07271779bb17d9447ccbefe6c5f7c56da3a6329b12c0b40b6
END=2026-09-21T04:57:37Z
```
变异核对（/home/james/audit-tmp/m031_b3r1_mutation.log）：
```
START=2026-09-21T04:58:14Z
recompute call sites before mutation: 1
mutation MVN_EXIT=1
   AccessServiceTests tests=54 errors=0 skipped=0 failures=1
       failure savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants | &#10;expected: &quot;role_recompute_new&quot;&#10; but was: &quot;role_recompute_old&quot;
scratch removed
END=2026-09-21T04:58:25Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
