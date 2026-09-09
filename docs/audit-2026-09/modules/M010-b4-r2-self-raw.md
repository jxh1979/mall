CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M010 B4-r2 修复内容复审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B4-r2（B3 第 2 次整批修复的完整 diff 复审）。2026-09-09T14:49:45Z
- before tree：`35806985b5e4753f1095a358b2e70c48c0777995`；after tree：`3bd4d6670eb5553ebc6c60e5655470b8b8000cd8`；`git diff --binary 35806985b5e4 3bd4d6670eb5` 全文 1387 bytes SHA256 cf8e5d67189309f447efabb1a5ad73c12279910d03c4264715f18d8f9b4af7ef（全文附于本报告末尾）。
- 定稿队列（findings V1.32 第二批定稿）：M010-012 共 1 项；本轮只改 AnalyticsJdbcIntegrationTests 一行夹具与文件版本头。
- 锁四值：实算/sidecar/ledger/Grok 均 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。

## 1. diffstat

```
.../com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java | 4 +++-
 1 file changed, 3 insertions(+), 1 deletion(-)
```

## 2. 逐块归因（每一行改动都归到定稿 ID；「版本铁律」行随其所在文件的 ID）

| 文件 | 块 | 位置 | 块内标记 ID | 归因 |
|---|---|---|---|---|
| backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java | 1 | `@@ -1,4 +1,5 @@` | 版本铁律（随所属ID） | M010-012（伴生：夹具角色码 + 版本头） |
| backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java | 2 | `@@ -474,7 +475,8 @@ class AnalyticsJdbcIntegrationTests {` | M010-012 | M010-012（伴生：夹具角色码 + 版本头） |

## 3. 红绿证据（B3-r2 运行记录 /tmp/m010_b3r2_run.log 原文）

```
START=2026-09-09T14:36:05Z
BEFORE_TREE=35806985b5e4753f1095a358b2e70c48c0777995
=== PHASE RED: existing analytics test on V410 DB (no code change) ===
RED_EXIT=1
[ERROR] Tests run: 29, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 3.342 s <<< FAILURE! -- in com.complex.platform.analytics.AnalyticsJdbcIntegrationTests
[ERROR] com.complex.platform.analytics.AnalyticsJdbcIntegrationTests.merchantSummariesRollupAggregatesRealSources -- Time elapsed: 0.017 s <<< ERROR!
]; ERROR: new row for relation "business_entities" violates check constraint "chk_be_entity_role"
Caused by: org.postgresql.util.PSQLException: ERROR: new row for relation "business_entities" violates check constraint "chk_be_entity_role"
]; ERROR: new row for relation "business_entities" violates check constraint "chk_be_entity_role"
[ERROR] Tests run: 29, Failures: 0, Errors: 1, Skipped: 0
=== PHASE GREEN: M010-012 fixture ===
PATCHED AnalyticsJdbcIntegrationTests (M010-012)
GREEN_EXIT=0
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.196 s -- in com.complex.platform.analytics.AnalyticsJdbcIntegrationTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.481 s -- in com.complex.platform.settlement.SettlementEntityAttributionS1Tests
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.791 s -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[INFO] Tests run: 53, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
AFTER_TREE=3bd4d6670eb5553ebc6c60e5655470b8b8000cd8
END=2026-09-09T14:36:25Z
```

## 4. 本席复审结论

见文末追加的判定段。

## 附：完整 diff（before→after，--binary）

```diff
diff --git a/backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java
index 8616b586..d8e195c3 100644
--- a/backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.analytics;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.groups.Tuple.tuple;
@@ -474,7 +475,8 @@ class AnalyticsJdbcIntegrationTests {
         jdbcTemplate.update("""
                 INSERT INTO business_entities (id, entity_name, entity_role, unified_social_credit_code,
                         legal_representative, bank_account_ref, status, created_at, updated_at)
-                VALUES ('org_m4', 'M4主体', 'operation', 'm4-code', 'M4', 'vault://m4', 'active', now(), now())
+                VALUES ('org_m4', 'M4主体', 'management_company', 'm4-code', 'M4', 'vault://m4', 'active', now(), now())
+                -- M010-012：entity_role 白名单已收为 HTML 三角色（V410），夹具不再直写退役码 operation
                 ON CONFLICT (id) DO NOTHING
                 """, java.util.Map.of());
         jdbcTemplate.update("""

```

## 4. 本席复审结论（席甲，作者自审票）

本席逐块读完上述 diff 全文（1 文件，2 hunk），并以 §2 归因表核对每一行。

### 4.1 修复内容（逐 ID）

| ID | 修复内容 | 证据 | 本席票 |
|---|---|---|---|
| 012 | `AnalyticsJdbcIntegrationTests` 夹具 `INSERT INTO business_entities … 'operation'` → `'management_company'`（附一行 SQL 注释说明 V410 白名单），文件加「// 文件版本：V0.01」；不改被测生产代码、不改 SettlementEntityAttributionS1Tests（014 登记，Grok B2 禁止顺带）、不并前端 mock | 红：未改代码时 `merchantSummariesRollupAggregatesRealSources` 在 V410 库上 ERROR（`chk_be_entity_role` 违例）；绿：改后 Analytics 29/29、PlatformJdbc 16/16、Settlement S1 8/8，BUILD SUCCESS | PASS |

### 4.2 范围归因（§5 闸 3）

- 2 hunk（版本头、夹具行+注释）全部落到定稿 012；归因不到的行 0。
- 未改：三份业务 HTML、`plan/*`、锁、sidecar、ledger 基准、findings、V005/V048/V318/V354/V410、权限种子、M009/M010 生产代码、014 所指文件、前端测试。after tree 再次实取相同。
- 未提交/推送/合并/部署。

### 4.3 结论

内容修复：**PASS**。范围归因：**PASS**。总：**PASS**（作者自审票；须 Grok B4-r2 亦 PASS 才进 B5-r2；B3 累计 2/3）。本席在本报告落盘前未读取 `M010-b4-r2-grok-raw.md`。
