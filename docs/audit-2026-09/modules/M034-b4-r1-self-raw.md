CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M034 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T08:28:16Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M034 B3-r1 修复 diff：before tree `0b52ef063c92fced6cb33ac28c30c67b314d06bb` → after tree `6a961e93046d113f530b5492170ff0c91398d79f`；冻结 diff `/home/james/audit-tmp/m034_b3r1.diff`（3630 bytes，SHA256 076be04f4c948bc7c8d630ed0066237e66ca210b838463d89f64d93e8710c3ca），全文见 §2。定稿 ID：M034-001（队列「是」）。
- 顺序：Grok 已先派（modules/M034-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐行归因（§5 闸 3）

| 文件 | ± | 归因 ID | 行首 64 字 |
|---|---|---|---|
| V429__retire_verification_manual_override.sql | `+` | 001 | -- V429：M034 审计（2026-09-21）001。 |
| V429__retire_verification_manual_override.sql | `+` | 001 | -- 枚举速查「核销记录状态」：「系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不 |
| V429__retire_verification_manual_override.sql | `+` | 001 | -- 权限矩阵文档自 2026-08-06 R10 口径修订起已把 verification.manual_override  |
| V429__retire_verification_manual_override.sql | `+` | 001 | -- （V270 种入并授给平台管理员）。历史迁移不回改，向前修：该码停用，并清掉它的全部授予来源。 |
| V429__retire_verification_manual_override.sql | `+` | 001 | --   权限判定先看字典里该码是否 active，不是即一律拒绝（含最高权限）；而有效权限的汇集不联字典状态，所以四处授予都 |
| V429__retire_verification_manual_override.sql | `+` | 001 | --   端点与服务方法的下线属票务模块，另行登记。 |
| V429__retire_verification_manual_override.sql | `+` | 001 |  |
| V429__retire_verification_manual_override.sql | `+` | 001 | UPDATE permissions SET status = 'retired' WHERE permission_code |
| V429__retire_verification_manual_override.sql | `+` | 001 |  |
| V429__retire_verification_manual_override.sql | `+` | 001 | DELETE FROM role_permissions WHERE permission_code = 'verificat |
| V429__retire_verification_manual_override.sql | `+` | 001 | DELETE FROM permission_template_items WHERE permission_code = ' |
| V429__retire_verification_manual_override.sql | `+` | 001 | DELETE FROM user_permission_overrides WHERE permission_code = ' |
| V429__retire_verification_manual_override.sql | `+` | 001 |  |
| V429__retire_verification_manual_override.sql | `+` | 001 | UPDATE temporary_permission_grants |
| V429__retire_verification_manual_override.sql | `+` | 001 |    SET status = 'revoked', revoked_by = 'V429', revoked_at = no |
| V429__retire_verification_manual_override.sql | `+` | 001 |  WHERE permission_code = 'verification.manual_override' |
| V429__retire_verification_manual_override.sql | `+` | 001 |    AND status = 'active'; |
| AccessJdbcIntegrationTests.java | `+` | 001 |     // M034-001（枚举速查「系统不提供人工放行…任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过」） |
| AccessJdbcIntegrationTests.java | `+` | 001 |     // 人工放行权限码已停用，且不留任何授予来源。 |
| AccessJdbcIntegrationTests.java | `+` | 001 |     @Test |
| AccessJdbcIntegrationTests.java | `+` | 001 |     void manualOverridePermissionIsRetiredAndGrantedToNobody()  |
| AccessJdbcIntegrationTests.java | `+` | 001 |         assertThat(repository.permissionExists("verification.ma |
| AccessJdbcIntegrationTests.java | `+` | 001 |         assertThat(jdbcTemplate.queryForObject(""" |
| AccessJdbcIntegrationTests.java | `+` | 001 |                 SELECT (SELECT count(*) FROM role_permissions W |
| AccessJdbcIntegrationTests.java | `+` | 001 |                      + (SELECT count(*) FROM permission_templat |
| AccessJdbcIntegrationTests.java | `+` | 001 |                      + (SELECT count(*) FROM user_permission_ov |
| AccessJdbcIntegrationTests.java | `+` | 001 |                      + (SELECT count(*) FROM temporary_permissi |
| AccessJdbcIntegrationTests.java | `+` | 001 |                          WHERE permission_code = 'verification. |
| AccessJdbcIntegrationTests.java | `+` | 001 |                 """, Long.class)).isZero(); |
| AccessJdbcIntegrationTests.java | `+` | 001 |     } |
| AccessJdbcIntegrationTests.java | `+` | 001 |  |

- 2 文件、31 条 ± 行，全部为新增：前向迁移 V429（注释 6 行＋5 条语句）与一条真库断言，均归 001。无归因不到的行；无 mode 变更；已应用的 13 个迁移、票务端点与服务、三份 HTML、锁未动。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/backend/src/main/resources/db/migration/V429__retire_verification_manual_override.sql b/backend/src/main/resources/db/migration/V429__retire_verification_manual_override.sql
new file mode 100644
index 00000000..ac3c4bb6
--- /dev/null
+++ b/backend/src/main/resources/db/migration/V429__retire_verification_manual_override.sql
@@ -0,0 +1,17 @@
+-- V429：M034 审计（2026-09-21）001。
+-- 枚举速查「核销记录状态」：「系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过」；
+-- 权限矩阵文档自 2026-08-06 R10 口径修订起已把 verification.manual_override 标为「停用，待代码轮下线」，字典里却一直是 active
+-- （V270 种入并授给平台管理员）。历史迁移不回改，向前修：该码停用，并清掉它的全部授予来源。
+--   权限判定先看字典里该码是否 active，不是即一律拒绝（含最高权限）；而有效权限的汇集不联字典状态，所以四处授予都要清。
+--   端点与服务方法的下线属票务模块，另行登记。
+
+UPDATE permissions SET status = 'retired' WHERE permission_code = 'verification.manual_override';
+
+DELETE FROM role_permissions WHERE permission_code = 'verification.manual_override';
+DELETE FROM permission_template_items WHERE permission_code = 'verification.manual_override';
+DELETE FROM user_permission_overrides WHERE permission_code = 'verification.manual_override';
+
+UPDATE temporary_permission_grants
+   SET status = 'revoked', revoked_by = 'V429', revoked_at = now()
+ WHERE permission_code = 'verification.manual_override'
+   AND status = 'active';
diff --git a/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
index 0dbc4fec..9716ded5 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
@@ -644,6 +644,20 @@ class AccessJdbcIntegrationTests {
                 .isEqualTo(current.windowDays().value());
     }
 
+    // M034-001（枚举速查「系统不提供人工放行…任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过」）：
+    // 人工放行权限码已停用，且不留任何授予来源。
+    @Test
+    void manualOverridePermissionIsRetiredAndGrantedToNobody() {
+        assertThat(repository.permissionExists("verification.manual_override")).isFalse();
+        assertThat(jdbcTemplate.queryForObject("""
+                SELECT (SELECT count(*) FROM role_permissions WHERE permission_code = 'verification.manual_override')
+                     + (SELECT count(*) FROM permission_template_items WHERE permission_code = 'verification.manual_override')
+                     + (SELECT count(*) FROM user_permission_overrides WHERE permission_code = 'verification.manual_override')
+                     + (SELECT count(*) FROM temporary_permission_grants
+                         WHERE permission_code = 'verification.manual_override' AND status = 'active')
+                """, Long.class)).isZero();
+    }
+
     // M006-G-07（PGM-1.10.1「旧派生同步失效」）：已定岗改职级复用定岗的四步写入（职级、定岗、套模板、替换派生），
     // 入口必须与 assignUserPosition 一样由 Spring 事务整体包住，否则套模板已提交、替换派生失败时新旧派生并存。
     @Test
```

## 3. 内容审计

- 迁移五条语句都以 `permission_code = 'verification.manual_override'` 为唯一谓词，不波及别的码；在未跑过的库上首次执行：UPDATE/DELETE 命中 0 行也成功，不依赖该码存在。`status = 'retired'` 库内已有先例、无 CHECK；`revoked_by` 为 VARCHAR(128)，写 'V429' 合法。
- 生效路径：`JdbcAccessRepository.permissionExists` 只认 active → `AuthorizationService.permissionGrant` 先判 `knownPermission` 再判最高权限与日常桥 → 该码对任何主体一律 PERMISSION_DENIED；`findPermissionEffects` 不联字典状态，故四处授予同时清。GREEN 后库内终态与此一致（探针）。
- 注释与 SQL 一致；未回改 V270。真库断言在修前红、修后绿。
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m034_b3r1_run.log）

```
START=2026-09-21T08:25:20Z HEAD=e456ba96 origin/main=4f6ef48b
pre: flyway_top=428 status=active role_permissions=1 template_items=0 overrides=0 active_temp_grants=0 platform_admin_missing_active=0 
BEFORE_TREE=0b52ef063c92fced6cb33ac28c30c67b314d06bb
== RED：只落真库断言（尚无 V429）
patched AccessJdbcIntegrationTests.java
red MVN_EXIT=1
   AccessJdbcIntegrationTests tests=26 errors=0 skipped=0 failures=1
       failure manualOverridePermissionIsRetiredAndGrantedToNobody | &#10;Expecting value to be false but was true
red: flyway_top=428 status=active role_permissions=1 template_items=0 overrides=0 active_temp_grants=0 platform_admin_missing_active=0 
== GREEN：新增前向迁移 V429，重跑（Flyway 把 complex_test 升到 429）
green MVN_EXIT=0
   AccessJdbcIntegrationTests tests=26 errors=0 skipped=0 failures=0
green: flyway_top=429 status=retired role_permissions=0 template_items=0 overrides=0 active_temp_grants=0 platform_admin_missing_active=0 
== 同批回归（票务控制器测试用的是授权桩，不读字典；主工作区 AccessServiceTests/M006JdbcRegressionTests 的既有报错与本模块无关，不纳入）
regress MVN_EXIT=0
   AccessJdbcIntegrationTests tests=26 errors=0 skipped=0 failures=0
   AuthorizationServiceTests tests=24 errors=0 skipped=0 failures=0
   TicketingControllerTests tests=20 errors=0 skipped=0 failures=0
untouched applied migrations: 1 changed
AFTER_TREE=6a961e93046d113f530b5492170ff0c91398d79f
DIFF bytes=3630 sha=076be04f4c948bc7c8d630ed0066237e66ca210b838463d89f64d93e8710c3ca
 .../V429__retire_verification_manual_override.sql       | 17 +++++++++++++++++
 .../platform/access/AccessJdbcIntegrationTests.java     | 14 ++++++++++++++
 2 files changed, 31 insertions(+)
after AccessJdbcIntegrationTests.java 873 291e657bc4363c3b202b08c3b02b88de032911d46f762fdb19828596d898ff96
after V429__retire_verification_manual_override.sql 17 903331b27bd536cdce11c3abb3547f4ec77b1c4a0f07408cf2a706c6b3d02078
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T08:25:42Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
