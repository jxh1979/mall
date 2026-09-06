CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M006 B4-r2 Grok 只读复审（审计席乙）

版本：V0.01
控制：V0.17；模块 M006 `10-backend-main-access-01`（6/322）；B3 已执行次数=2；ledger=`IN_PROGRESS`。
本席角色：Grok。只读复审本轮完整 diff，不修复、不改方案、不启动 B5、不启用第三角色。
本次完整报告范围：本文件自本行 `CRITERIA-LOCK-SHA256:` 起至文末。

## 0. 技能预检（实际核读，非声称）

已实际读取并按合同收窄口径使用：

| 技能 | 路径 | 本轮用法 |
|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 |
| requesting-code-review | 同套件 `requesting-code-review/SKILL.md` | B4 只派本席；**不用**本体 `HEAD~1`、同会话子代理、无 ID 即刻修 |
| verification-before-completion | 同套件 `verification-before-completion/SKILL.md` | PASS 前实跑命令，不以未运行当通过 |
| grilling | `/home/james/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只借追问结构；**不**等操作员、**不**派子代理 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 读错误→复现→追数据流；不改生产代码 |
| diagnosing-bugs | mattpocock `engineering/diagnosing-bugs/SKILL.md` | 027 红绿对照环 |
| codebase-design | mattpocock `engineering/codebase-design/SKILL.md` | 占用计数与来源接缝 |
| tdd | mattpocock `engineering/tdd/SKILL.md` | 测缝须用户确认作废；文档/SQL 不强制新测 |
| ponytail-review | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删；结论见 §7 |
| writing-for-agents | mattpocock `productivity/writing-for-agents/SKILL.md` | 本 raw 给下游席阅读 |
| i-have-adhd | `/home/james/.claude/plugins/marketplaces/i-have-adhd/skills/i-have-adhd/SKILL.md` | 输出形状 |
| receiving-code-review | superpowers `receiving-code-review/SKILL.md` | 本席是外审，不实施修复 |

缺口（不得冒充已调用）：

- `batch-grill-me`：未找到技能目录
- `ubiquitous-language`：未找到技能目录
- `git-guardrails-claude-code`：存在于 mattpocock `skills/misc/`，可选；本轮无 git 写操作，未调用

明确不用（合同划掉）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`ponytail-audit`、`ponytail-debt`、`research`、`code-review`（mattpocock）。未派第三席。

## 1. 冻结输入

- 本席实算 `docs/audit-2026-09/plan/criteria-lock.md` SHA256 = `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`
- sidecar `plan/criteria-lock.sha256` 同行；`ledger.md` 顶部同行；`LOCK-AMEND-COUNT: 0`
- `findings.md`：V0.59；225477 字节；SHA256=`30902ffb71d71f6f3c42e7f944da490cff5eecc95f589ac2f4556d33db815f89`
- 当前队列表唯一值：001—018/024—030 修复队列=是；019—023=否。本轮只允许归因是项。
- 头部统一路由：r1 整批审计 FAIL、越界双 PASS；3 条失败均仍在队列 009/026/027，整批直接回 B3；r2 已完成。仍沿用全部 25 项定稿授权。
- 本轮直接修复的 3 项遗留原因：009 正常旧岗位授权接续/独立来源保留；026 旧配置占用；027 旧卡充值模式与确认模式一致。
- before tree=`7fecb8e739b6b04475755fffbb8e106fa0759713`（type=tree）
- after tree=`2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9`（type=tree）
- 未读取本轮 M006-b4-r2 双方 raw、任何 B5 raw、双方 B1/B2 论证；不以其他席结果作证据。
- 未按路径过滤 diff、未改用 HEAD、未建第三 tree、未读当前整个工作区 diff。

适用冻结判据（模块映射内）：GEN-002、GEN-003、GEN-005、GEN-006、GEN-011、GEN-014、GEN-017、BSM-003、BSM-035、BSM-120。上位 HTML 原文（不扩映射）：BSM-004 删前占用（业务地图 570–575 段，锁内 BSM-003/004 定义区）、PGM-041 换岗重算（锁内 PGM-041，M006 映射未列 PGM-041，本候选以已映射 GEN-005/006/017 与 BSM-120 举证）、BSM-006/枚举 210/业务地图 1480 预留接口默认关。

027 边界（当前拟修正文，不把历史二维码/只读方案当现行规则）：RESERVED/OFF 与未配置失败关闭；不要求启用真实网关、生产固定码或改 HTML；报本人手机号加短信验证扣真实订单/账单；门票已有二维码不删。本轮 diff 不触及短信/账单/门票码路径。

## 2. 两树完整 diff 逐字原文

命令：`git diff --binary 7fecb8e739b6b04475755fffbb8e106fa0759713 2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9`

实算：17644 字节；SHA256=`cc2fc0f92106bd49386038da79b4b1e1b862cf447bc6bc753322417efd413a41`；5 路径；`grep -c '^@@'` = 12。

路径：

1. `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java`
2. `backend/src/main/java/com/complex/platform/customer/StoredValueService.java`
3. `backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql`
4. `backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java`
5. `backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java`

下文为该命令全部未过滤输出（逐字）：

````
diff --git a/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java b/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
index 8ea5d417..5344c1f3 100644
--- a/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
+++ b/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
@@ -1,5 +1,5 @@
 package com.complex.platform.access;
-// 文件版本：V0.10
+// 文件版本：V0.11
 import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
@@ -511,7 +511,14 @@ class JdbcAccessRepository implements AccessRepository {
     @Override
     public long countPositionTemplateUsage(String id) {
         return jdbcTemplate.queryForObject("""
-                SELECT count(*) FROM user_position_assignments WHERE assigned_position_template_id = :id
+                SELECT count(*) FROM user_position_assignments assignment
+                 WHERE assigned_position_template_id = :id
+                    OR (assigned_position_template_id IS NULL AND EXISTS (
+                        SELECT 1 FROM position_role_templates mapping
+                         WHERE mapping.id = :id AND mapping.position_code = assignment.position_code
+                           AND mapping.staff_level_id = assignment.staff_level_id
+                           AND (mapping.organization_id IS NULL
+                                OR mapping.organization_id = assignment.organization_id)))
                 """, Map.of("id", id), Long.class);
     }
 
diff --git a/backend/src/main/java/com/complex/platform/customer/StoredValueService.java b/backend/src/main/java/com/complex/platform/customer/StoredValueService.java
index eed489a5..cc0af2e3 100644
--- a/backend/src/main/java/com/complex/platform/customer/StoredValueService.java
+++ b/backend/src/main/java/com/complex/platform/customer/StoredValueService.java
@@ -1,6 +1,6 @@
 package com.complex.platform.customer;
 
-// 文件版本：V0.05
+// 文件版本：V0.07
 
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.common.money.MoneyRounding;
@@ -130,7 +130,7 @@ public class StoredValueService {
         // 发卡资格门控（AUD-061）：商户卡充值前须平台已给该商户开通「发卡资格」，默认关→拒。通用卡不受限。
         requireCardIssuanceEnabled(merchantId);
         // 提前建账户（active），充值入账时复用（开卡即从商户配置定格资金模式）。
-        loadOrCreateAccountForUpdate(customerId, projectId, merchantId);
+        var card = loadOrCreateAccountForUpdate(customerId, projectId, merchantId);
         String key = idempotencyKey == null || idempotencyKey.isBlank()
                 ? "sv_topup_" + UUID.randomUUID() : idempotencyKey;
         // B1d⑤ 资金归属：商户卡走 stored_value_topup_merchant（finance 据此把预收记入发卡商户，按开关结算）；
@@ -146,7 +146,7 @@ public class StoredValueService {
         PaymentIntentRecord intent = financeService.createPaymentIntent(new PaymentIntentRequest(
                 scopedTopupKey, null, null, businessOrderType, customerId, amountCent, "CNY",
                 channel == null || channel.isBlank() ? "wechat" : channel, null,
-                merchantId, resolveMerchantFundOwnership(merchantId)));
+                merchantId, effectiveFundOwnership(card)));
         // AUD-058 + AUD 复核 P1-1②：把本次充值的卡范围（customer/project/merchant）绑定到支付意图，settle 只认此绑定范围入账。
         // bind「冲突即校验一致」：同一支付意图已绑定的卡范围与本次不同 → 抛 409（不再 DO UPDATE 静默改绑）；完全一致才幂等放过。
         if (repository.bindStoredValueTopupIntent(intent.id(), customerId, projectId, merchantId, now()) == 0) {
diff --git a/backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql b/backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql
index f90c67ee..20db4b23 100644
--- a/backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql
+++ b/backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql
@@ -1,4 +1,4 @@
--- 文件版本：V0.04
+-- 文件版本：V0.07
 -- M006-006：授权申请的审批事实快照；不回填既有申请，缺快照须重新申请。
 ALTER TABLE access_grant_requests ADD COLUMN authorization_fingerprint VARCHAR(64);
 
@@ -27,3 +27,45 @@ ALTER TABLE user_position_assignments
     ADD COLUMN assigned_role_id VARCHAR(64) REFERENCES roles(id),
     ADD COLUMN assigned_position_template_id VARCHAR(64) REFERENCES position_role_templates(id),
     ADD COLUMN position_application_ids VARCHAR(64)[] NOT NULL DEFAULT '{}';
+
+-- M006-009：旧入口仅岗位应用使用实际操作员ID；独立套用固定写access-api。
+-- 只接续成功岗位事件能证明的来源，不按时间接近或相同权限猜测来源。
+UPDATE user_position_assignments AS position
+   SET assigned_role_id = (
+           SELECT CASE WHEN EXISTS (
+                      SELECT 1 FROM user_roles legacy WHERE legacy.user_id = position.user_id
+                        AND legacy.role_id = event.metadata->>'role_id')
+                  THEN event.metadata->>'role_id' END
+             FROM audit_events event
+            WHERE event.event_name = 'access.user_position.assigned' AND event.event_result = 'success'
+              AND event.business_type = 'user_position_assignment' AND event.business_id = position.user_id
+            ORDER BY event.occurred_at DESC, event.id DESC LIMIT 1),
+       position_application_ids = ARRAY(
+           SELECT application.id FROM permission_template_applications application
+            WHERE application.target_type = 'user' AND application.target_id = position.user_id
+              AND application.applied_by IS NOT NULL AND application.applied_by <> 'access-api'
+              AND application.status = 'active'
+              AND EXISTS (SELECT 1 FROM audit_events event
+                           WHERE event.event_name = 'access.user_position.assigned'
+                             AND event.event_result = 'success' AND event.business_type = 'user_position_assignment'
+                             AND event.business_id = position.user_id
+                             AND event.metadata->>'template_id' = application.template_id));
+
+-- 角色表没有来源列；成功岗位事件识别派生角色，独立显式授予及既有注册/引导角色保留。
+DELETE FROM user_roles AS legacy
+ WHERE EXISTS (SELECT 1 FROM user_position_assignments position WHERE position.user_id = legacy.user_id)
+   AND EXISTS (SELECT 1 FROM audit_events event
+                WHERE event.event_name = 'access.user_position.assigned' AND event.event_result = 'success'
+                  AND event.business_type = 'user_position_assignment' AND event.business_id = legacy.user_id
+                  AND event.metadata->>'role_id' = legacy.role_id)
+   AND COALESCE((SELECT event.event_name FROM audit_events event
+                  WHERE event.event_name IN ('access.role.assigned', 'access.role.unassigned')
+                    AND event.event_result = 'success' AND event.business_type = 'user_role'
+                    AND event.business_id = legacy.user_id || ':' || legacy.role_id
+                  ORDER BY event.occurred_at DESC, event.id DESC LIMIT 1), '') <> 'access.role.assigned'
+   AND NOT EXISTS (
+       SELECT 1 FROM access_users account WHERE account.id = legacy.user_id
+         AND ((account.account_type = 'platform_admin' AND legacy.role_id = 'role_platform_admin')
+           OR (account.partner_org_id IS NOT NULL AND legacy.role_id IN
+               ('role_outsourced_security_cleaning', 'role_outsourced_construction', 'role_outsourced_facility'))
+           OR (account.account_type = 'staff' AND legacy.role_id = 'role_staff_self')));
diff --git a/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
index f66f8d4c..0b636c0c 100644
--- a/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.10
+// 文件版本：V0.13
 import static org.assertj.core.api.Assertions.*;
 import static org.junit.jupiter.api.Assumptions.assumeTrue;
 
@@ -136,10 +136,7 @@ class M006JdbcRegressionTests {
 
     // M006-006/009/016/026：执行完整迁移 SQL，仅作用于本连接临时表；验证政策可持久化修改。
     @Test void migrationAppliesAndGrantPolicyRoundTripsInPostgres() throws Exception {
-        jdbc.execute("ALTER TABLE pg_temp.access_grant_requests DROP COLUMN authorization_fingerprint");
-        for (String table : java.util.List.of("supervisor_review_policies", "position_role_templates", "user_position_assignments", "roles")) {
-            jdbc.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL)");
-        }
+        preparePositionMigrationTables();
         jdbc.execute(java.nio.file.Files.readString(java.nio.file.Path.of(
                 "backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql")));
         var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
@@ -257,4 +254,53 @@ class M006JdbcRegressionTests {
         jdbc.update("INSERT INTO temporary_permission_grants (id, subject_id, permission_code, single_use) VALUES (?, 'user', 'permission', true)", id);
         return id;
     }
+
+    // M006-009/026：旧版本真实写入形态经迁移后仍区分岗位来源与独立来源。
+    @Test void legacyPositionMigrationPreservesIndependentGrantsAndCountsOccupiedDefinitions() throws Exception {
+        preparePositionMigrationTables();
+        jdbc.update("INSERT INTO roles (id, role_name, data_scope, status, created_at) VALUES ('old-role','旧岗位','project','active',now()), ('prior-role','更早岗位','project','active',now()), ('new-role','新岗位','project','active',now())");
+        jdbc.update("INSERT INTO access_users (id,username,account_type,status,organization_id,created_at) VALUES ('legacy','legacy','staff','active','org',now()), ('explicit','explicit','staff','active','org',now()), ('no-audit','no-audit','staff','active','org',now())");
+        jdbc.update("INSERT INTO position_role_templates (id,position_code,staff_level_id,role_id,template_id,organization_id) VALUES ('old-mapping','old-position','level','old-role','old-template','org'), ('outside','old-position','level','old-role','old-template','other-org'), ('new-mapping','new-position','level','new-role','new-template','org')");
+        jdbc.update("INSERT INTO user_position_assignments (user_id,position_code,staff_level_id,organization_id,assigned_by) VALUES ('legacy','old-position','level','org','operator'), ('explicit','old-position','level','org','operator'), ('no-audit','old-position','level','org','operator')");
+        jdbc.update("INSERT INTO user_roles (user_id,role_id) VALUES ('legacy','old-role'), ('legacy','prior-role'), ('explicit','old-role')");
+        jdbc.update("""
+                INSERT INTO permission_template_applications (id,template_id,target_id,target_type,single_use,applied_by,applied_at)
+                VALUES ('legacy-app','old-template','legacy','user',false,'operator',now()),
+                       ('independent-app','old-template','legacy','user',false,'access-api',now()),
+                       ('new-app','new-template','legacy','user',false,'operator',now())
+                """);
+        for (String user : java.util.List.of("legacy", "explicit")) {
+            jdbc.update("""
+                    INSERT INTO audit_events (id,occurred_at,event_name,event_result,business_type,business_id,metadata)
+                    VALUES (?,now(),'access.user_position.assigned','success','user_position_assignment',?,
+                            '{"position_code":"old-position","staff_level_id":"level","role_id":"old-role","template_id":"old-template"}')
+                    """, UUID.randomUUID(), user);
+        }
+        jdbc.update("""
+                INSERT INTO audit_events (id,occurred_at,event_name,event_result,business_type,business_id,metadata)
+                VALUES (?,now() - interval '1 day','access.user_position.assigned','success','user_position_assignment','legacy',
+                        '{"position_code":"prior-position","staff_level_id":"level","role_id":"prior-role","template_id":"prior-template"}'),
+                       (?,now(),'access.role.assigned','success','user_role','explicit:old-role','{"role_id":"old-role"}')
+                """, UUID.randomUUID(), UUID.randomUUID());
+        jdbc.execute(java.nio.file.Files.readString(java.nio.file.Path.of(
+                "backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql")));
+        assertThat(repository.countPositionTemplateUsage("old-mapping")).isEqualTo(3);
+        assertThat(repository.countPositionTemplateUsage("outside")).isZero();
+        assertThat(jdbc.queryForObject("SELECT assigned_role_id FROM user_position_assignments WHERE user_id='legacy'", String.class)).isEqualTo("old-role");
+        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_roles WHERE user_id='legacy'", Integer.class)).isZero();
+        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_roles WHERE user_id='explicit' AND role_id='old-role'", Integer.class)).isEqualTo(1);
+        repository.replacePositionGrants("legacy", "new-role", "new-mapping", java.util.List.of("new-app"), "operator", Instant.now());
+        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id='legacy-app'", String.class)).isEqualTo("revoked");
+        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id='independent-app'", String.class)).isEqualTo("active");
+        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id='new-app'", String.class)).isEqualTo("active");
+    }
+
+    private void preparePositionMigrationTables() {
+        jdbc.execute("ALTER TABLE pg_temp.access_grant_requests DROP COLUMN authorization_fingerprint");
+        for (String table : java.util.List.of("supervisor_review_policies", "position_role_templates",
+                "user_position_assignments", "roles", "user_roles", "access_users", "audit_events")) {
+            jdbc.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL)");
+        }
+        jdbc.execute("ALTER TABLE pg_temp.permission_template_applications ADD COLUMN target_type varchar, ADD COLUMN applied_by varchar, ADD COLUMN applied_at timestamptz, ADD COLUMN revoked_by varchar, ADD COLUMN revoked_at timestamptz");
+    }
 }
diff --git a/backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java b/backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java
index 51e84045..c1bb8b39 100644
--- a/backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java
+++ b/backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.customer;
 
-// 文件版本：V0.11
+// 文件版本：V0.12
 // M006-027：现场储值扣款必须先核验报出的手机号和短信验证码。
 import static org.assertj.core.api.Assertions.*;
 import static org.mockito.Mockito.*;
@@ -220,4 +220,22 @@ class M006StoredValuePaymentTests {
                 .isInstanceOf(ApiException.class).hasMessageContaining("短信服务未配置");
         assertUnchanged();
     }
+
+    // M006-027：修改商户发卡配置后，已开卡续充仍使用卡本身定格的资金模式。
+    @Test void rechargeAfterMerchantConfigChangeUsesTheCardsFrozenFundMode() {
+        var finance=mock(FinanceService.class);
+        var gateway=mock(com.complex.platform.finance.gateway.PaymentGatewayRouter.class);
+        when(gateway.isGatewayEnabled()).thenReturn(true);
+        var service=new StoredValueService(customers,finance,mock(AuditEventService.class),gateway,auth,null);
+        when(customers.findStoredValueMerchantRefundRule("merchant_1")).thenReturn(Optional.of(
+                new StoredValueMerchantRefundRuleRecord("merchant_1",true,10000,true,"platform_collect",now,now)));
+        when(customers.findStoredValueAccount("customer_1",null,"merchant_1")).thenAnswer(i->Optional.of(balance.get()));
+        var intent=new com.complex.platform.finance.PaymentIntentRecord("pay","stored_value_topup_merchant","customer_1",5000L,"CNY","wechat","created",now,now);
+        when(finance.createPaymentIntent(any())).thenReturn(intent);
+        when(customers.bindStoredValueTopupIntent(eq("pay"),eq("customer_1"),isNull(),eq("merchant_1"),any())).thenReturn(1);
+        service.topup("customer_1",null,"merchant_1",5000L,"wechat","regular-recharge");
+        var request=org.mockito.ArgumentCaptor.forClass(com.complex.platform.finance.dto.PaymentIntentRequest.class);
+        verify(finance).createPaymentIntent(request.capture());
+        assertThat(request.getValue().fundMode()).isEqualTo(balance.get().fundOwnership());
+    }
 }
````

## 3. 12 hunk 逐块 ID 归因（闸 3）

归因来源：`findings.md` 当前队列表。是=001–018/024–030；否=019–023。否项未使用。

| hunk | 文件 | @@ | 增删要点 | 归因 ID | 队列 |
|---|---|---|---|---|---|
| H1 | JdbcAccessRepository.java | `-1,5 +1,5` | 文件版本 V0.10→V0.11 | M006-026 | 是 |
| H2 | JdbcAccessRepository.java | `-511,7 +511,14` | 占用计数补 NULL 旧岗按岗位码/职级/组织匹配 | M006-026 | 是 |
| H3 | StoredValueService.java | `-1,6 +1,6` | 文件版本 V0.05→V0.07 | M006-027 | 是 |
| H4 | StoredValueService.java | `-130,7 +130,7` | 开卡/锁行结果赋给 `card` | M006-027 | 是 |
| H5 | StoredValueService.java | `-146,7 +146,7` | 支付意图资金模式改 `effectiveFundOwnership(card)` | M006-027 | 是 |
| H6 | V386 SQL | `-1,4 +1,4` | 文件版本 V0.04→V0.07 | M006-009 | 是 |
| H7 | V386 SQL | `-27,3 +27,45` | 旧岗来源接续 UPDATE + 派生角色 DELETE | M006-009 | 是 |
| H8 | M006JdbcRegressionTests.java | `-1,6 +1,6` | 文件版本 V0.10→V0.13 | M006-009、M006-026 | 是 |
| H9 | M006JdbcRegressionTests.java | `-136,10 +136,7` | 既有迁移测试改走共享 TEMP 准备 | M006-009、M006-026 | 是 |
| H10 | M006JdbcRegressionTests.java | `-257,4 +254,53` | 旧写入形态迁移+占用断言与 `preparePositionMigrationTables` | M006-009、M006-026 | 是 |
| H11 | M006StoredValuePaymentTests.java | `-1,6 +1,6` | 文件版本 V0.11→V0.12 | M006-027 | 是 |
| H12 | M006StoredValuePaymentTests.java | `-220,4 +220,22` | 旧卡续充资金模式断言 | M006-027 | 是 |

越界判定：12 hunk 每一增删行都能指到当前队列为「是」的定稿 ID。无顺手抽象、无换测试框架、无 019–023、无 HTML 三份需求文件、无 lock/ledger/findings 改动。验证性测试归因到对应 ID，合法。

**越界：PASS**

## 4. 直接调用者与旧版本真实写路径

只按已列文件名再读直接相关源码路径（排除 `docs/audit-2026-09`、`.scratch`、raw）。

009 旧写入（`origin/main` `AccessService.assignUserPositionDirect`，现仍可在 after tree 对照）：

- `saveUserPositionAssignment` + `repository.assignUserRole(user, role)`：**不**走 `assignUserRoleDirect`，故**不**写 `access.role.assigned`
- `applyTemplate(..., context.actor().id())`：岗位应用 `applied_by`=真实操作员
- `recordSuccess("access.user_position.assigned", "access.role.assign", "user_position_assignment", userId, ..., metadata role_id/template_id)`
- 独立套用：`applyTemplate(..., "access-api")` + `permission_template.applied`
- 独立授角色：`assignUserRoleDirect` → `access.role.assigned` / `business_type=user_role` / `business_id=userId:roleId`
- 解角色：`access.role.unassigned`，同一 business_id 形态

现码换岗（after tree，本轮未改 `AccessService`）：`assignUserPositionDirect` 1160–1161 调 `replacePositionGrants`；模板应用仍用 `actor.id()`。`replacePositionGrants`（JdbcAccessRepository 820–841）只撤销 `position_application_ids` 内应用并改写三列来源，不碰独立 `user_roles`。

026 调用：`AccessService.requirePositionTemplateUnused` 534–537 → `countPositionTemplateUsage`；`request_delete`/`approve_delete` 501、518。`deletePositionRoleTemplate` 仍抛禁止物理删除。

027 调用：`topup` 创建意图；`settleTopup` 182 行已用 `effectiveFundOwnership(card)`；`createIntent`（FinanceService 1323–1326）把 `request.fundMode()` 交给网关。`loadOrCreateAccountForUpdate` 852–856 仅开卡时 `resolveMerchantFundOwnership`。

## 5. 三线

### 第一性原理

1. **为何必须存在？** 删掉 H2，HTML「删前自动查占用」对未回填 `assigned_position_template_id` 的旧岗会计 0，能删仍在用的岗位配置（BSM-003/004）。删掉 H7，换岗只改新列、旧 `user_roles`/操作员应用仍在，PGM-041/拟修「旧岗位派生权限同步失效、独立来源保留」做不到。删掉 H4/H5，续充按商户当前配置而非卡上定格，与 settle/confirm 及「开卡定格」不一致，资金路由会在配置变更后错配。
2. **声称行为是否实跑？** 是。见 §6：TEMP 表执行完整 V386；占用 3/0；换岗后独立应用仍 active；027 单测 after PASS、before tree 同测 FAIL（`merchant_prepaid` vs `platform_collect`）。触及测试类 23/23 通过。共享库 public 行数与列未变。
3. **未验证就当真的前提？** 无真实微信/支付宝回执（027 现行边界不要求启用网关）。未跑会写共享库的 Spring 集成用例。无审计事件的旧岗（`no-audit`）不接续 `assigned_role_id`——旧入口同方法内写库与 `recordSuccess`，第一次正常跑会有事件；无事件需审计失败且事务仍提交，属多条件偶发，不单独立项。

### 顺从式

- 009 拟修：同一事务撤旧岗派生、保留独立显式授权。H7 用成功岗位事件+操作员 `applied_by` 接续，独立 `access-api` 与 `access.role.assigned` 保留；`replacePositionGrants` 只撤已接续 ID。与拟修一致。
- 026 拟修：删前占用。H2 把 NULL 旧岗按定义键计入；跨组织 `outside` 不计。未把 DELETE 改成仅 `status=inactive`。
- 027 拟修：充值经既有 intent/网关；确认模式用卡定格。H5 与 settle 同源 `effectiveFundOwnership`。未打开 `payment.gateway.enabled`，未改 Reserved 网关，未改三 HTML。

### 对抗式

- 用当前商户配置 `platform_collect` 打已定格 `merchant_prepaid` 卡：before 失败、after 通过。
- 同岗位码不同组织映射：占用 0。
- 独立 `access.role.assigned`：`explicit` 的 `user_roles` 保留。
- 引导角色 `role_platform_admin`：TEMP 上 DELETE 后仍在。
- 换岗只撤 `legacy-app`，`independent-app`/`new-app` 仍 active。
- 无岗位事件用户：不删其角色（本夹具无角色），但仍计入占用 3。
- 门票码/短信账单：本 diff 无删码、无改 HTML 585。
- 未发现可归因到无 ID 或队列「否」的行。

## 6. 实际命令与输出（凭据已脱敏）

### 6.1 锁与 findings、diff 规格

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
wc -c / sha256sum docs/audit-2026-09/findings.md
225477
30902ffb71d71f6f3c42e7f944da490cff5eecc95f589ac2f4556d33db815f89
git cat-file -t <before> / <after>
tree / tree
git diff --binary <before> <after> | wc -c
17644
git diff --binary <before> <after> | sha256sum
cc2fc0f92106bd49386038da79b4b1e1b862cf447bc6bc753322417efd413a41
grep -c '^@@'
12
```

从 `/etc/complex/complex.env` 只读入 `COMPLEX_DATABASE_URL/USERNAME/PASSWORD` 到进程，URL 库名改为 `complex_test`。打印仅为 `URL_DB_BEFORE complex` → `URL_DB_AFTER complex_test`，`USER_SET yes`，`PASS_SET yes`。未打印凭据。

### 6.2 只读 public / Flyway

`complex_test`：`flyway_schema_history` 无 version=386；`user_position_assignments` 无 `assigned_role_id`（仍 7 列旧形态）；`user_position_assignments` 行数 0；`user_roles` 行数 3。故 r2 是改尚未落地的 V386 文件，不是改已应用迁移的校验和。

### 6.3 连接级 TEMP 执行 V386（回滚，不改共享表）

复现 Java 夹具后 `EXECUTE V386` → `V386_OK`。

- `legacy`：`assigned_role_id=old-role`，`position_application_ids=['legacy-app']`，`user_roles` 清空
- `explicit`：`assigned_role_id=old-role`，应用数组空，`user_roles` 仍 `(explicit, old-role)`
- `no-audit`：`assigned_role_id=NULL`，数组空
- 占用 SQL（与 H2 相同）：`old-mapping=3`，`outside=0`，`new-mapping=0`
- `replacePositionGrants` 语义：`legacy-app=revoked`，`independent-app=active`，`new-app=active`；之后占用 `old-mapping=2`、`new-mapping=1`
- public：`user_roles` 仍 3，`assignments` 仍 0
- 额外：`platform_admin`+`role_platform_admin` 经岗位事件后 DELETE 仍保留
- 事务 `rollback`

H2 在回填前（`assigned_position_template_id` 全 NULL）若仍用旧 SQL `WHERE assigned_position_template_id = :id` 则占用为 0——即 r1 遗留的 026 原因。

### 6.4 javac + 触及测试（输出到 /tmp 后删除；未写 backend/target）

`mvn -o dependency:build-classpath` 到临时 `cp.txt`，exit 0。`backend/target` mtime 始终 `1788623733`。

`mvn compiler:compile` 因该进程 `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` 报 `release version 21 not supported` 失败，且日志写 `to target/classes`——**未采用 Maven 编译**。改用已有 `/usr/lib/jvm/temurin-21-jdk-amd64/bin/javac --release 21`，把已有 `backend/target/classes` **复制**到 `/tmp/m006-b4-r2-audit/classes` 后重编译 4 个本轮 Java 文件到该副本。

JUnit 5 本地 launcher（未下载 console-standalone）：

```
TESTS_FOUND=2 TESTS_SUCCEEDED=2 TESTS_FAILED=0
# M006JdbcRegressionTests#legacyPositionMigrationPreservesIndependentGrantsAndCountsOccupiedDefinitions
# M006StoredValuePaymentTests#rechargeAfterMerchantConfigChangeUsesTheCardsFrozenFundMode
JAVA_TEST_EXIT=0
RUN_DB complex_test
```

before tree 的 `StoredValueService` 编进隔离目录后跑同一 027 方法：

```
expected: "merchant_prepaid" but was: "platform_collect"
BEFORE_SUCCEEDED=0 FAILED=1
```

两测试类全量（含 H9 改写的迁移政策测试）：

```
ALL_FOUND=23 SUCCEEDED=23 FAILED=0 SKIPPED=0 ABORTED=0
ALL_EXIT=0
```

跑后 public：`user_position_assignments=0`，`user_roles=3`，`flyway_386=0`，`assigned_role_id` 列仍不存在。

## 7. ponytail-review

`M006StoredValuePaymentTests` H12 多打了一条未使用的 `findStoredValueAccount` stub（topup 走 `findStoredValueAccountForUpdate`）。测试夹具冗余，不是产品缺陷，不单独立项。其余为版本行与必要 TEMP 表。`Lean already. Ship.` `net: -1 lines possible`（删那条 stub）。

## 8. 验证缺口（如实）

- 未跑会写共享库的 Spring 集成用例（合同禁止）。
- 未启用真实支付网关、未改 HTML、未使用生产固定短信码（027 现行边界）。
- Maven `--release 21` 在默认 JAVA_HOME 不可用；改用本机已有 Temurin 21 javac。不能把「Maven 编译 backend/target」写成已通过。
- 027 单测断言的是 `PaymentIntentRequest.fundMode()`；网关 `initiatePayment` 透传由读取 `FinanceService.createIntent` 1323–1326 支持，无真实渠道回执。
- 未做本模块 B5 全量（本任务禁止启动 B5）。

## 9. Findings

本席本轮 **无新 finding**。009/026/027 三条遗留原因在本 diff 内已对上拟修边界，且有类型 2/4 证据。

不把以下写成失败：无审计事件旧岗不接续角色（非第一次正常跑必撞）；未回填 `assigned_position_template_id`（H2 用 NULL 匹配覆盖占用，换岗后由 `replacePositionGrants` 写入）。

## 10. 结论

- **审计：PASS**
- **越界：PASS**
- 无待补项、无未答复 questions、无 HTML 改动、无 LOCK-AMEND。
- 不启动 B5。本席不修复。

