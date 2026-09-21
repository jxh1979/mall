CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M035 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T09:41:29Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M035 B3-r1 修复 diff：before tree `8d98b7cf96b7f52240da486683ff1d69d441765c` → after tree `e6db773a3ba42ff53c5b0fcb59a4847d8e00c0cb`；冻结 diff `/home/james/audit-tmp/m035_b3r1.diff`（9908 bytes，SHA256 58691b86a3728dde9285c696d41d9f1f7f31b94165da81bea34b5bcb917f69f9），全文见 §2。定稿 ID：M035-001、002、003（队列「是」）。
- 顺序：Grok 已先派（modules/M035-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐行归因（§5 闸 3）

| 文件 | ± | 归因 ID | 行首 64 字 |
|---|---|---|---|
| V430__permission_names_no_latin_ai.sql | `+` | 001 | -- V430：M035 审计（2026-09-21）001。 |
| V430__permission_names_no_latin_ai.sql | `+` | 001 | -- 治理地图：展示给用户的一切文字「必须全部为中文，不得出现任何英文」，可以不用中文的只有四样（单据号、设备编号、二维码内容 |
| V430__permission_names_no_latin_ai.sql | `+` | 001 | -- 系统内部标识符），没有第五样。权限名由权限列表页、模板页直接上屏，仍有名称含拉丁字母「AI」；此前的展示名测试把「AI」 |
| V430__permission_names_no_latin_ai.sql | `+` | 001 | -- 历史迁移不回改，向前修：把权限名里的「AI」写成同义的「人工智能」。permission_code 是机读标识，不变。 |
| V430__permission_names_no_latin_ai.sql | `+` | 001 |  |
| V430__permission_names_no_latin_ai.sql | `+` | 001 | UPDATE permissions |
| V430__permission_names_no_latin_ai.sql | `+` | 001 |    SET permission_name = replace(permission_name, 'AI', '人工智能') |
| V430__permission_names_no_latin_ai.sql | `+` | 001 |  WHERE permission_name ~ 'AI'; |
| AccessControllerTests.java | `+` | 002 |         Mockito.verify(authorizationService).requirePermission( |
| AccessControllerTests.java | `+` | 002 |         Mockito.verify(authorizationService).requirePermission( |
| AccessControllerTests.java | `+` | 002 |         Mockito.verify(authorizationService).requirePermission( |
| AccessControllerTests.java | `+` | 002 |     // M035-002：岗位模板生命周期端点按动作分两种门控——批准/驳回删除须同时持两个权限码，其余动作只须角色装配 |
| AccessControllerTests.java | `+` | 002 |     @Test |
| AccessControllerTests.java | `+` | 002 |     void positionTemplateLifecycleGatesDeleteDecisionsOnBothPer |
| AccessControllerTests.java | `+` | 002 |         mockMvc.perform(post("/api/v1/access/position-role-temp |
| AccessControllerTests.java | `+` | 002 |                         .with(user("ops").roles("P0_SMOKE")) |
| AccessControllerTests.java | `+` | 002 |                         .contentType(MediaType.APPLICATION_JSON |
| AccessControllerTests.java | `+` | 002 |                         .content(""" |
| AccessControllerTests.java | `+` | 002 |                                 {"idempotency_key": "lifecycle- |
| AccessControllerTests.java | `+` | 002 |                                 """)) |
| AccessControllerTests.java | `+` | 002 |                 .andExpect(status().isOk()); |
| AccessControllerTests.java | `+` | 002 |         Mockito.verify(authorizationService).requirePermissions |
| AccessControllerTests.java | `+` | 002 |         Mockito.verify(authorizationService, Mockito.never()).r |
| AccessControllerTests.java | `+` | 002 |  |
| AccessControllerTests.java | `+` | 002 |         Mockito.reset(authorizationService); |
| AccessControllerTests.java | `+` | 002 |         mockMvc.perform(post("/api/v1/access/position-role-temp |
| AccessControllerTests.java | `+` | 002 |                         .with(user("ops").roles("P0_SMOKE")) |
| AccessControllerTests.java | `+` | 002 |                         .contentType(MediaType.APPLICATION_JSON |
| AccessControllerTests.java | `+` | 002 |                         .content(""" |
| AccessControllerTests.java | `+` | 002 |                                 {"idempotency_key": "lifecycle- |
| AccessControllerTests.java | `+` | 002 |                                 """)) |
| AccessControllerTests.java | `+` | 002 |                 .andExpect(status().isOk()); |
| AccessControllerTests.java | `+` | 002 |         Mockito.verify(authorizationService).requirePermission( |
| AccessControllerTests.java | `+` | 002 |         Mockito.verify(authorizationService, Mockito.never()) |
| AccessControllerTests.java | `+` | 002 |                 .requirePermissions("access.role.assign", "gove |
| AccessControllerTests.java | `+` | 002 |     } |
| AccessControllerTests.java | `+` | 002 |  |
| AccessControllerTests.java | `+` | 002 |     // M035-002：授权申请审批须同时持审批权限与主管复核权限；缺任一即 403，且不进服务层。 |
| AccessControllerTests.java | `+` | 002 |     @Test |
| AccessControllerTests.java | `+` | 002 |     void grantReviewRequiresBothReviewAndSupervisorApprovePermi |
| AccessControllerTests.java | `+` | 002 |         doThrow(ApiException.forbidden("PERMISSION_DENIED", "pe |
| AccessControllerTests.java | `+` | 002 |                 .when(authorizationService) |
| AccessControllerTests.java | `+` | 002 |                 .requirePermissions("access.grant_request.revie |
| AccessControllerTests.java | `+` | 002 |  |
| AccessControllerTests.java | `+` | 002 |         mockMvc.perform(post("/api/v1/access/grant-requests/agr |
| AccessControllerTests.java | `+` | 002 |                         .with(user("ops").roles("P0_SMOKE")) |
| AccessControllerTests.java | `+` | 002 |                         .contentType(MediaType.APPLICATION_JSON |
| AccessControllerTests.java | `+` | 002 |                         .content(""" |
| AccessControllerTests.java | `+` | 002 |                                 {"idempotency_key": "review-den |
| AccessControllerTests.java | `+` | 002 |                                 """)) |
| AccessControllerTests.java | `+` | 002 |                 .andExpect(status().isForbidden()) |
| AccessControllerTests.java | `+` | 002 |                 .andExpect(jsonPath("$.detail_code").value("PER |
| AccessControllerTests.java | `+` | 002 |         Mockito.verify(service, Mockito.never()).reviewGrant(an |
| AccessControllerTests.java | `+` | 002 |     } |
| AccessControllerTests.java | `+` | 002 |  |
| AccessControllerTests.java | `+` | 002 |     // M035-002：职级与定岗端点以路径里的账号为准，请求体里的 user_id 不生效。 |
| AccessControllerTests.java | `+` | 002 |     @Test |
| AccessControllerTests.java | `+` | 002 |     void staffLevelAndPositionEndpointsUseThePathUserIdNotTheBo |
| AccessControllerTests.java | `+` | 002 |         mockMvc.perform(put("/api/v1/access/users/usr_path/staf |
| AccessControllerTests.java | `+` | 002 |                         .with(user("ops").roles("P0_SMOKE")) |
| AccessControllerTests.java | `+` | 002 |                         .contentType(MediaType.APPLICATION_JSON |
| AccessControllerTests.java | `+` | 002 |                         .content(""" |
| AccessControllerTests.java | `+` | 002 |                                 {"idempotency_key": "level-path |
| AccessControllerTests.java | `+` | 002 |                                 """)) |
| AccessControllerTests.java | `+` | 002 |                 .andExpect(status().isOk()); |
| AccessControllerTests.java | `+` | 002 |         var level = org.mockito.ArgumentCaptor.forClass(com.com |
| AccessControllerTests.java | `+` | 002 |         Mockito.verify(service).assignStaffLevel(level.capture( |
| AccessControllerTests.java | `+` | 002 |         org.assertj.core.api.Assertions.assertThat(level.getVal |
| AccessControllerTests.java | `+` | 002 |  |
| AccessControllerTests.java | `+` | 002 |         mockMvc.perform(put("/api/v1/access/users/usr_path/posi |
| AccessControllerTests.java | `+` | 002 |                         .with(user("ops").roles("P0_SMOKE")) |
| AccessControllerTests.java | `+` | 002 |                         .contentType(MediaType.APPLICATION_JSON |
| AccessControllerTests.java | `+` | 002 |                         .content(""" |
| AccessControllerTests.java | `+` | 002 |                                 {"idempotency_key": "position-p |
| AccessControllerTests.java | `+` | 002 |                                 """)) |
| AccessControllerTests.java | `+` | 002 |                 .andExpect(status().isOk()); |
| AccessControllerTests.java | `+` | 002 |         var position = org.mockito.ArgumentCaptor.forClass(com. |
| AccessControllerTests.java | `+` | 002 |         Mockito.verify(service).assignUserPosition(position.cap |
| AccessControllerTests.java | `+` | 002 |         org.assertj.core.api.Assertions.assertThat(position.get |
| AccessControllerTests.java | `+` | 002 |     } |
| AccessControllerTests.java | `+` | 002 |  |
| AccessJdbcIntegrationTests.java | `-` | 003 |     // M006-G-07（PGM-1.10.1「旧派生同步失效」）：已定岗改职级复用定岗的四步写入（职级、定岗、套模板 |
| AccessJdbcIntegrationTests.java | `-` | 003 |     // 入口必须与 assignUserPosition 一样由 Spring 事务整体包住，否则套模板已提交、替换派生 |
| AccessJdbcIntegrationTests.java | `+` | 003 |     // M006-G-07（PGM-1.10.1「旧派生同步失效」）：已定岗改职级复用定岗的四步写入，两个入口都须声明事 |
| AccessJdbcIntegrationTests.java | `+` | 003 |     // M035-003：本测试只断言 assignUserPosition 与 assignStaffLevel 两个 |
| AccessJdbcIntegrationTests.java | `+` | 003 |     // （四步路径的行为测试在 AccessServiceTests）。 |
| AccessJdbcIntegrationTests.java | `-` | 003 |     void staffLevelChangeRunsInOneTransactionLikePositionAssign |
| AccessJdbcIntegrationTests.java | `+` | 003 |     void positionAndStaffLevelEntryPointsDeclareATransaction()  |
| AccessJdbcIntegrationTests.java | `-` | 001 |     // （HTML 正文自身使用的「AI」除外；permission_code / id 是机读标识，不在此列）。 |
| AccessJdbcIntegrationTests.java | `+` | 001 |     // （permission_code / id 是机读标识，不在此列）。M035-001：治理地图「不得出现任何英文 |
| AccessJdbcIntegrationTests.java | `+` | 001 |     // 「AI」不是豁免。 |
| AccessJdbcIntegrationTests.java | `-` | 001 |                  WHERE replace(permission_name, 'AI', '') ~ '[A |
| AccessJdbcIntegrationTests.java | `+` | 001 |                  WHERE permission_name ~ '[A-Za-z]' |

- 3 文件 7 个 hunk、93 条 ± 行。V430 全部归 001；AccessControllerTests.java 的三句 verify 与三条新测试归 002；AccessJdbcIntegrationTests.java 里测试名与 M006-G-07 注释的改写归 003，AI 豁免句与 SQL 的改写归 001。无归因不到的行；无 mode 变更；被测代码、已应用迁移、三份 HTML、锁未动。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/backend/src/main/resources/db/migration/V430__permission_names_no_latin_ai.sql b/backend/src/main/resources/db/migration/V430__permission_names_no_latin_ai.sql
new file mode 100644
index 00000000..385b7fb4
--- /dev/null
+++ b/backend/src/main/resources/db/migration/V430__permission_names_no_latin_ai.sql
@@ -0,0 +1,8 @@
+-- V430：M035 审计（2026-09-21）001。
+-- 治理地图：展示给用户的一切文字「必须全部为中文，不得出现任何英文」，可以不用中文的只有四样（单据号、设备编号、二维码内容、
+-- 系统内部标识符），没有第五样。权限名由权限列表页、模板页直接上屏，仍有名称含拉丁字母「AI」；此前的展示名测试把「AI」当豁免放过。
+-- 历史迁移不回改，向前修：把权限名里的「AI」写成同义的「人工智能」。permission_code 是机读标识，不变。
+
+UPDATE permissions
+   SET permission_name = replace(permission_name, 'AI', '人工智能')
+ WHERE permission_name ~ 'AI';
diff --git a/backend/src/test/java/com/complex/platform/access/AccessControllerTests.java b/backend/src/test/java/com/complex/platform/access/AccessControllerTests.java
index 1b4ea7d7..b183a061 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessControllerTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessControllerTests.java
@@ -106,6 +106,7 @@ class AccessControllerTests {
                 .andExpect(jsonPath("$.code").value("OK"))
                 .andExpect(jsonPath("$.data.items[0].permission_code").value("ops.production_readiness.view"))
                 .andExpect(jsonPath("$.data.total").value(1));
+        Mockito.verify(authorizationService).requirePermission("access.permission.view");
     }
 
     @Test
@@ -216,6 +217,7 @@ class AccessControllerTests {
                 .andExpect(jsonPath("$.code").value("OK"))
                 .andExpect(jsonPath("$.data.items[0].id").value("lvl_m1_lower"))
                 .andExpect(jsonPath("$.data.items[0].level_rank").value(1));
+        Mockito.verify(authorizationService).requirePermission("access.staff_level.view");
     }
 
     @Test
@@ -239,6 +241,7 @@ class AccessControllerTests {
                 .andExpect(jsonPath("$.code").value("OK"))
                 .andExpect(jsonPath("$.data.staff_level_id").value("lvl_m1_lower"))
                 .andExpect(jsonPath("$.data.salary_currency").value("CNY"));
+        Mockito.verify(authorizationService).requirePermission("access.staff_level.manage");
     }
 
     @Test
@@ -307,6 +310,76 @@ class AccessControllerTests {
                 .andExpect(jsonPath("$.data.wildcard").value(true));
     }
 
+    // M035-002：岗位模板生命周期端点按动作分两种门控——批准/驳回删除须同时持两个权限码，其余动作只须角色装配权限。
+    @Test
+    void positionTemplateLifecycleGatesDeleteDecisionsOnBothPermissions() throws Exception {
+        mockMvc.perform(post("/api/v1/access/position-role-templates/prt_1/lifecycle")
+                        .with(user("ops").roles("P0_SMOKE"))
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content("""
+                                {"idempotency_key": "lifecycle-approve", "action": "approve_delete", "reason": "同意删除"}
+                                """))
+                .andExpect(status().isOk());
+        Mockito.verify(authorizationService).requirePermissions("access.role.assign", "governance.supervisor_review.approve");
+        Mockito.verify(authorizationService, Mockito.never()).requirePermission("access.role.assign");
+
+        Mockito.reset(authorizationService);
+        mockMvc.perform(post("/api/v1/access/position-role-templates/prt_1/lifecycle")
+                        .with(user("ops").roles("P0_SMOKE"))
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content("""
+                                {"idempotency_key": "lifecycle-archive", "action": "archive", "reason": "停止使用"}
+                                """))
+                .andExpect(status().isOk());
+        Mockito.verify(authorizationService).requirePermission("access.role.assign");
+        Mockito.verify(authorizationService, Mockito.never())
+                .requirePermissions("access.role.assign", "governance.supervisor_review.approve");
+    }
+
+    // M035-002：授权申请审批须同时持审批权限与主管复核权限；缺任一即 403，且不进服务层。
+    @Test
+    void grantReviewRequiresBothReviewAndSupervisorApprovePermissions() throws Exception {
+        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
+                .when(authorizationService)
+                .requirePermissions("access.grant_request.review", "governance.supervisor_review.approve");
+
+        mockMvc.perform(post("/api/v1/access/grant-requests/agr_1/review")
+                        .with(user("ops").roles("P0_SMOKE"))
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content("""
+                                {"idempotency_key": "review-denied", "decision": "approve"}
+                                """))
+                .andExpect(status().isForbidden())
+                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
+        Mockito.verify(service, Mockito.never()).reviewGrant(any(), any());
+    }
+
+    // M035-002：职级与定岗端点以路径里的账号为准，请求体里的 user_id 不生效。
+    @Test
+    void staffLevelAndPositionEndpointsUseThePathUserIdNotTheBodyUserId() throws Exception {
+        mockMvc.perform(put("/api/v1/access/users/usr_path/staff-level")
+                        .with(user("ops").roles("P0_SMOKE"))
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content("""
+                                {"idempotency_key": "level-path", "user_id": "usr_body", "staff_level_id": "lvl_m1_lower"}
+                                """))
+                .andExpect(status().isOk());
+        var level = org.mockito.ArgumentCaptor.forClass(com.complex.platform.access.dto.StaffLevelAssignmentRequest.class);
+        Mockito.verify(service).assignStaffLevel(level.capture());
+        org.assertj.core.api.Assertions.assertThat(level.getValue().userId()).isEqualTo("usr_path");
+
+        mockMvc.perform(put("/api/v1/access/users/usr_path/position")
+                        .with(user("ops").roles("P0_SMOKE"))
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content("""
+                                {"idempotency_key": "position-path", "user_id": "usr_body", "position_code": "ops_clerk", "staff_level_id": "lvl_m1_lower"}
+                                """))
+                .andExpect(status().isOk());
+        var position = org.mockito.ArgumentCaptor.forClass(com.complex.platform.access.dto.UserPositionAssignmentRequest.class);
+        Mockito.verify(service).assignUserPosition(position.capture());
+        org.assertj.core.api.Assertions.assertThat(position.getValue().userId()).isEqualTo("usr_path");
+    }
+
     @TestConfiguration
     static class TestBeans {
         @Bean
diff --git a/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
index 9716ded5..da5b59ed 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
@@ -658,10 +658,11 @@ class AccessJdbcIntegrationTests {
                 """, Long.class)).isZero();
     }
 
-    // M006-G-07（PGM-1.10.1「旧派生同步失效」）：已定岗改职级复用定岗的四步写入（职级、定岗、套模板、替换派生），
-    // 入口必须与 assignUserPosition 一样由 Spring 事务整体包住，否则套模板已提交、替换派生失败时新旧派生并存。
+    // M006-G-07（PGM-1.10.1「旧派生同步失效」）：已定岗改职级复用定岗的四步写入，两个入口都须声明事务。
+    // M035-003：本测试只断言 assignUserPosition 与 assignStaffLevel 两个入口带事务声明，不执行四步写入本身
+    // （四步路径的行为测试在 AccessServiceTests）。
     @Test
-    void staffLevelChangeRunsInOneTransactionLikePositionAssignment() throws NoSuchMethodException {
+    void positionAndStaffLevelEntryPointsDeclareATransaction() throws NoSuchMethodException {
         assertThat(transactionAttributeSource.getTransactionAttribute(AccessService.class.getMethod(
                 "assignUserPosition", com.complex.platform.access.dto.UserPositionAssignmentRequest.class), AccessService.class))
                 .isNotNull();
@@ -671,12 +672,13 @@ class AccessJdbcIntegrationTests {
     }
 
     // M006-G-08（PGM-1.18「界面全中文」）：治理页 / 模板页直接上屏的权限名、角色名、模板名不含拉丁字母
-    // （HTML 正文自身使用的「AI」除外；permission_code / id 是机读标识，不在此列）。
+    // （permission_code / id 是机读标识，不在此列）。M035-001：治理地图「不得出现任何英文…可以不用中文的只有四样，没有第五样」，
+    // 「AI」不是豁免。
     @Test
     void seededDisplayNamesAreChinese() {
         assertThat(jdbcTemplate.queryForList("""
                 SELECT permission_code FROM permissions
-                 WHERE replace(permission_name, 'AI', '') ~ '[A-Za-z]'
+                 WHERE permission_name ~ '[A-Za-z]'
                  ORDER BY permission_code
                 """, String.class)).isEmpty();
         assertThat(jdbcTemplate.queryForList(
```

## 3. 内容审计

- 001：测试 SQL 由 `replace(permission_name, 'AI', '') ~ '[A-Za-z]'` 改为 `permission_name ~ '[A-Za-z]'`，对任何拉丁字母都红；注释删去豁免句并引治理地图原文。V430 单条 UPDATE，谓词 `permission_name ~ 'AI'`，只动名称含「AI」的行；在未跑过的库上命中 0 行也成功；permission_code 不动。GREEN 后库内含拉丁字母的权限名 0 条。
- 002：双权限两处对复数方法 `requirePermissions(双码)` 校验/打桩，archive 分支校验单数方法并断言复数方法未被调用；审批被拒时断言 403 且 `service.reviewGrant` 未被调用；职级与定岗用 ArgumentCaptor 断言传给服务层的是路径 id。三句 verify 补在既有成功路径测试末尾。变异核对三条全红，断言能失败。
- 003：只改测试名与注释，两条断言原样；新注释如实写明「只断言事务声明、不执行四步写入」。
- 内容票：**PASS**。

## 4. 可执行证据原文

/home/james/audit-tmp/m035_b3r1_run.log：
```
START=2026-09-21T09:38:11Z HEAD=e456ba96 origin/main=c3a48ccd
pre: flyway_top=429 latin_permission_names=2 latin_role_names=0 latin_template_names=0 analytics.risk_alert.view=查看经营分析风险预警和AI质量台账 wecom.ai_qc.manage=管理企微AI语义质检开关和适配器 
BEFORE_TREE=8d98b7cf96b7f52240da486683ff1d69d441765c
== RED：只落测试补丁（尚无 V430）
patched AccessControllerTests.java 4
patched AccessJdbcIntegrationTests.java 3
red MVN_EXIT=1
   AccessControllerTests tests=16 errors=0 skipped=0 failures=0
   AccessJdbcIntegrationTests tests=26 errors=0 skipped=0 failures=1
       failure seededDisplayNamesAreChinese | &#10;Expecting empty but was: [&quot;analytics.risk_alert.view&quot;, &quot;wecom.ai_qc.manage&quot;]
== GREEN：新增前向迁移 V430，重跑（Flyway 把 complex_test 升到 430）
green MVN_EXIT=0
   AccessControllerTests tests=16 errors=0 skipped=0 failures=0
   AccessJdbcIntegrationTests tests=26 errors=0 skipped=0 failures=0
green: flyway_top=430 latin_permission_names=0 latin_role_names=0 latin_template_names=0 analytics.risk_alert.view=查看经营分析风险预警和人工智能质量台账 wecom.ai_qc.manage=管理企微人工智能语义质检开关和适配器 
AFTER_TREE=e6db773a3ba42ff53c5b0fcb59a4847d8e00c0cb
DIFF bytes=9908 sha=58691b86a3728dde9285c696d41d9f1f7f31b94165da81bea34b5bcb917f69f9
 .../V430__permission_names_no_latin_ai.sql         |  8 +++
 .../platform/access/AccessControllerTests.java     | 73 ++++++++++++++++++++++
 .../access/AccessJdbcIntegrationTests.java         | 12 ++--
 3 files changed, 88 insertions(+), 5 deletions(-)
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T09:38:32Z
```
/home/james/audit-tmp/m035_b3r1_mutation.log：
```
START=2026-09-21T09:38:32Z scratch at c3a48ccd
mutated 4 sites
mutation MVN_EXIT=1
   AccessControllerTests tests=16 errors=0 skipped=0 failures=3
       failure staffLevelAndPositionEndpointsUseThePathUserIdNotTheBodyUserId | &#10;expected: &quot;usr_path&quot;&#10; but was: &quot;usr_body&quot;
       failure grantReviewRequiresBothReviewAndSupervisorApprovePermissions | Status expected:&lt;403&gt; but was:&lt;200&gt;
       failure positionTemplateLifecycleGatesDeleteDecisionsOnBothPermissions | &#10;Wanted but not invoked:&#10;authorizationService.requirePermissions(&#10;    &quot;access.role.assign&quot;,&#10;    &quot;governance.s
scratch removed
END=2026-09-21T09:38:43Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
