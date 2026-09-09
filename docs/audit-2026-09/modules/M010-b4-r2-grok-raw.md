CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M010 B4-r2 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r2
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M010-b4-r2-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r2 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：任何 `M010-b4-r2-self-raw.md`（当前亦不存在）、席甲本次 B4 raw。定稿队列以 findings.md V1.32 顶部「当前 M010 第二批 B2-r1 双PASS定稿，1项待修（B3-r2）」为准，并与本席第二批 B2-r1（`M010-b5-r1-grok-raw.md` V0.02 追加段 012 最小处置）对照。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、1 个定稿「是」ID（M010-012）是否按最小边界修好、每一行能否归因到该 ID、是否越界改 HTML/锁/sidecar/ledger 基准/权限种子/迁移/001–011/013/014/其它模块业务逻辑。未新造判据/角色，未询问操作员。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a\n`（65 字节）同值。ledger 顶行同值。criteria-lock 版本仍 V0.11。`LOCK-AMEND-COUNT: 1`。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M010 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段 |
| 阶段 | B4-r2 只读审计 B3 第2次整批修复的「修复内容」本身 |
| 正式队列「是」（1） | M010-012 |
| 正式队列「否」 | 001–004（已修）、005–011、013、014（登记） |
| B3 | 已执行 2 次（本轮为第 2 次），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `35806985b5e4753f1095a358b2e70c48c0777995` |
| after（对象类型 tree，可解析） | `3bd4d6670eb5553ebc6c60e5655470b8b8000cd8` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 35806985b5e4753f1095a358b2e70c48c0777995 3bd4d6670eb5553ebc6c60e5655470b8b8000cd8`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 1387 bytes；SHA256 `cf8e5d67189309f447efabb1a5ad73c12279910d03c4264715f18d8f9b4af7ef`；UTF-8；1 文件；3 插入 / 1 删除；2 hunk；name-status M |
| 与 `/tmp/m010_b3_r2.diff` | `cmp` 全等（本席自算为准，副本仅对照） |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 7255 / after 7255；`comm -3` 空（无增删路径） |
| HEAD | `242f487233d028173cb9198eecd079f5d877405c` = `origin/main` |
| 工作区该测试文件 blob | 等于 after：`d8e195c3521d84c9e165ca4d269d11c20e837eea` |
| HEAD 该测试文件 blob | 等于 before：`8616b586822408afe37a59ea62fee7bcb92d832e` |
| findings.md | 工作区 V1.32（499656 字节，SHA256 `9a1f63441edf489485d71fed9e3d151543fb00686610274f40072fb058cc7e8e`）；两树 blob 均为 `1cbe6f81c2162309d86ed30051408b54553afadb`（本 diff 未改 findings） |
| ledger.md | 两树 blob 均为 `ceb658b76c81f4285645090144e86b668671d378`（本 diff 未改） |
| 三 HTML 两树 blob | BSM `12334b13f3c27a815c9bbf9eea216403fb0d76d1` / PGM `c2588ca1c65a3f196b39a23f299bd56967b5652e` / BEQR `86a717930f9303d4f131ee2864478baa14450e6d`，完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |
| V005 / V048 / V318 / V354 | 两树 blob 相同（`94692f5a` / `61018fff` / `9f8eeaf9` / `b5181122`） |
| V410 | 两树 blob 相同（`647844c1a40bde9f0f8c1a0835dd4d8b3f8f6a81`） |
| V003 权限种子 | 两树 blob 相同（`a13ebe87`） |
| JdbcAnalyticsRepository / PlatformService / S1 / PlatformJdbc / RevenueStream / 前端两 mock / IdempotencyServiceTests | 两树 blob 全部相同 |

`git diff --stat --binary`：

```
 .../com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java | 4 +++-
 1 file changed, 3 insertions(+), 1 deletion(-)
```

`git diff --numstat`：`3	1	backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java`。

工作区相对 HEAD 另有既有父链脏文件（M009 已修未提交、审计产物、工具、V410 等）。那些路径在 before/after 两树 blob 相同，不在本轮 diff 内，不按本轮修复内容审计。本席未改任何源文件，只新建本 raw。验证只在临时库 `m010_b4r2_grok_probe`（`TEMPLATE complex_test`，事后 DROP）与 `/tmp/m010_b4r2_backend` 工程副本上的 mvn；未改任一 `complex_test*` 的模式/数据，未在主仓跑写模式 mvn。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M010 的集合（GEN-001…018 共 18）。本轮只用定稿 012 已引用的 ID：GEN-003、GEN-008（测试有效性；接口与模式一致性）。不 LOCK-AMEND。

B3-r2 边界（与本席第二批 B2-r1 票及 findings V1.32 顶部一致）：

- **只修 012**：`AnalyticsJdbcIntegrationTests` 一行夹具 `operation`→`management_company` + 文件版本头（无版本者加 `// 文件版本：V0.01`）。
- TDD：红＝该用例现状实跑 ERROR；绿＝改后该类通过。不新开测试文件。
- 不改被测生产代码、三份 HTML、判据、权限种子、迁移（含 V410 与 V005/V048/V318/V354）。
- 不并前端 vitest mock（`OrganizationMerchantPage.test.tsx` / `GovernanceTopologyPage.test.tsx` 的 `entity_role:"operation"` 不命中 V410 CHECK，两席 B5 均未列为 finding，不另开 ID）。
- 不改 `SettlementEntityAttributionS1Tests`（014 登记且 B2 写死禁止顺带）。
- 已修 001–004 与 005–011、013、014 不在本轮。

四分类本轮不重开 012：B2 已定为技术性（003 的跨模块机械后果）。落地仍停在该格：夹具角色坍缩为已有 `management_company`，与 003 存量 UPDATE / M011 六处同法。无业务发明。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary 35806985b5e4753f1095a358b2e70c48c0777995 3bd4d6670eb5553ebc6c60e5655470b8b8000cd8`

before tree = `35806985b5e4753f1095a358b2e70c48c0777995`
after tree = `3bd4d6670eb5553ebc6c60e5655470b8b8000cd8`

字节 1387；SHA256 `cf8e5d67189309f447efabb1a5ad73c12279910d03c4264715f18d8f9b4af7ef`；UTF-8 完整原文如下。

````diff
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
````

---

## 4. §5 闸 3 逐行归因

| hunk | 符号 | 内容（压缩） | 归因 ID | 理由 |
|---|---|---|---|---|
| L1 包声明后 | `+` | `// 文件版本：V0.01` | M010-012 | 操作员版本铁律：该文件原无版本头，随本文件所属 ID 加 V0.01；B2 写死「加版本头」 |
| L477 VALUES | `-` | `'org_m4' … 'operation' …` | M010-012 | 定稿处置：夹具退役码 |
| L477 VALUES | `+` | `'org_m4' … 'management_company' …` | M010-012 | 定稿处置：坍缩为 HTML 三角色已有码，与 003/M011 同法 |
| VALUES 与 ON CONFLICT 之间 | `+` | `-- M010-012：entity_role 白名单已收为 HTML 三角色（V410），夹具不再直写退役码 operation` | M010-012 | 同一夹具 hunk 内的归因注释（PostgreSQL `--` 行注释，不改变 SQL 语义）；形态同 B3-r1 已接受的 M011 夹具同行注释；复述已定稿 003/V410 事实，非新业务发明 |

未改动的上下文行（`package`、`import`、`INSERT INTO …`、`ON CONFLICT (id) DO NOTHING`、后续商户夹具）无独立变更，不另归因。`ON CONFLICT` 按 B2「不改 ON CONFLICT」保持原句。

**无任何一行归因不到定稿「是」ID。** 013/014/001–011 在本 diff 中零行。闸 3 归因：**PASS**。

---

## 5. 修复内容（是否修好 / 最懒 / 新缺陷 / 新发明）

### 5.1 M010-012 — 修好

缺陷：`merchantSummariesRollupAggregatesRealSources` 夹具直写 `entity_role='operation'`，V410 `chk_be_entity_role` 只收 `platform` / `project_company` / `management_company`，INSERT 在目标断言前 ERROR（GEN-003）；夹具枚举与库 CHECK 不一致（GEN-008）。

after 现文 L478 已为 `'management_company'`。该类测的是商户汇总滚动，`entity_role` 只为满足 `merchants.business_entity_id` 存在性；断言读 `analytics_merchant_summaries` 的商户名/金额/核销/风险/账单状态，不读角色码。把角色换成已有三角色不改变被测行为。

### 5.2 最懒可行

与 B2 写死边界一致：只动这一个测试文件、一行夹具 + 版本头。未改生产代码、未新开测试、未改方法名、未改商户夹具、未改 `ON CONFLICT`、未改 S1、未并前端 mock、未改 V410。

夹具 hunk 内多出的 SQL 归因注释仍归 012，不引入可执行行为，不构成单独 finding。相对「只改字面量」不是更短，但与本模块上一轮已接受的夹具注释同形态，不升格为越界或无 ID。

### 5.3 未引入新缺陷、未发明业务

- 角色值是 003 已落地的 `management_company`，不是新码。
- 注释复述 V410/HTML 三角色已有事实。
- after 文件中不再有存活 JDBC 夹具直写 `operation`/`spv`（仅注释提到退役码；`PlatformJdbcIntegrationTests:463` 的 `'spv'` INSERT 是 003 能红拒绝路径，两树未改，保持）。
- 前端 mock 仍含 `entity_role:"operation"`，两树 blob 相同，遵守「不并入 012」。

### 5.4 TDD 红绿（本席独立重跑，不信 B3 日志）

B3 记录 `/tmp/m010_b3r2_red.log`、`/tmp/m010_b3r2_green.log` 只作对照。本席在临时副本重跑：

1. 只抽出三变量（`COMPLEX_DATABASE_URL` / `_USERNAME` / `_PASSWORD`），不 `source` 整份 env。
2. `CREATE DATABASE m010_b4r2_grok_probe TEMPLATE complex_test`（complex_test 当时 0 连接）。
3. 探针只读：Flyway 354/399/410 `success=t`；`chk_be_entity_role` = `IN ('platform','project_company','management_company')`。
4. SQL 红：`BEGIN; INSERT … entity_role='operation' id=org_m4_b4r2_red;` → `ERROR: … violates check constraint "chk_be_entity_role"`，psql exit 3。
5. SQL 绿：同句改 `'management_company'`（id=`org_m4_b4r2_ok`）→ `INSERT 0 1`，`ROLLBACK`，exit 0；残留 0。
6. 工程副本 `/tmp/m010_b4r2_backend`（非主仓）：`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`；`SETTLE_TEST_DB_URL`/`COMPLEX_DATABASE_URL` 指向探针。
   - 红：写入 before blob（SHA256 `4ad44cc9b59508da00dd981cd5599398eabd8dacc6eef72ef947b60de88b670b` = blob `8616b586`）。`mvn -Dtest=com.complex.platform.analytics.AnalyticsJdbcIntegrationTests test` → exit 1；29 跑、0 失败、**1 ERROR**（`merchantSummariesRollupAggregatesRealSources`）；SQL 仍为 `'operation'`；`chk_be_entity_role`。日志 `/tmp/m010_b4r2_grok_red.log`（12505 字节，SHA256 `b414b97c55421cfacf9f4aa60d4374e2268375e2882962b36990a59134d4c893`）。Flyway：`jdbc:postgresql://127.0.0.1:5432/m010_b4r2_grok_probe`，version 410，未再迁。
   - 绿：写入 after blob（SHA256 `44d69324473a371be1520c6c12ab864e76f6fb5eb4215bc8e4f39f7bf7421a43` = blob `d8e195c3`）。同类命令 → exit 0；**29/29、Failures 0、Errors 0、BUILD SUCCESS**。日志 `/tmp/m010_b4r2_grok_green.log`（7428 字节，SHA256 `dfc2db2f5451407409e107e5b7d24263cc8fe263cef1a357814c124017f070a9`）。同样打探针库 version 410。
7. 探针 `id='org_m4'` 计数 0（`@Transactional` 回滚）。`DROP DATABASE m010_b4r2_grok_probe WITH (FORCE)`。complex_test 仍 410、`org_m4` 计数 0。事后删除三变量临时 env 与工程副本。

红在绿之前、用同一用例自身。可执行行为变更先有可失败测试。TDD：**成立**。

修复内容：**PASS**。

---

## 6. 越界检查

| 禁止项 | 两树结果 |
|---|---|
| 三份业务 HTML | blob 相同 |
| 锁 / sidecar / ledger 基准 | blob 相同 |
| findings（本 diff） | blob 相同 |
| 权限种子 V003 | blob 相同 |
| 历史迁移 V005/V048/V318/V354 | blob 相同 |
| 本轮不得改的 V410 | blob 相同 |
| 其它 ID 内容（001–011、013、014） | 对应文件 blob 相同；S1 未改 |
| 其它模块业务逻辑 / 被测生产代码 | `JdbcAnalyticsRepository`、`PlatformService` 相同 |
| 前端 mock | 两测试 tsx blob 相同（未并入） |
| 幂等 `operationId` | `IdempotencyServiceTests` 相同 |

越界：**无**。闸 3 越界判定本席 **PASS**。

---

## 7. 问题二分（回退统一规则）

- **有定稿 ID 的问题：** 无。012 已按最小边界修好。
- **无定稿 ID 的问题：** 无。

不触发「无 ID → 整批回 B2」或「有 ID 仍属队列 → 整批回 B3」。本席不声明模块 PASS，不改 findings/ledger。

观察（不构成 FAIL、不另开 ID）：夹具 hunk 内 SQL 归因注释比 B2「只改 L477 + 版本头」多一行，已归因 012，可执行语义为零。

---

## 8. 结论

| 项 | 本席票 |
|---|---|
| 内容修复是否成立 | **PASS** |
| 范围归因是否全部命中 | **PASS** |
| 总结论 | **PASS** |

本席 FAIL 具否决权：本轮不行使。§5 闸 3 越界判定本席 PASS，仍须席甲独立票合取。不可用本票代替 B5 或模块 PASS。
