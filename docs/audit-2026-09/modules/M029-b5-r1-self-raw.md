CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M029 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；3 个工具的现行改动为席甲 B3 所写，且席甲 B1 的 PASS 已被 Grok 推翻过一次，以 Grok 票为准）。2026-09-21T03:00:31Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；B3-r1 after tree `d639978e4c5b02b405365eba6f87dcfd73bcb8dd`。
- 模块 M029 4 文件（§1 全读），18 判据；B3 累计 1。环境前提同 B1（D-028）：工具实跑在基于 origin/main 的一次性 worktree 里、拷入修后的 3 个工具与 2 份重生成产物后进行（已删除）。
- 顺序：本席取证脚本先跑完，随后派 Grok，再落本报告；落盘前未读 Grok 正文（modules/M029-b5-r1-grok-raw.md 存在=False）。只读承诺：未改任何已跟踪文件；未带 --write-baseline；数据库只对 complex_test SELECT。
- 本轮补的方法（B1 漏了 GR-02）：工具输出一律看全文，不截尾；对隔离机审的表名抽取，另找了「含 FROM 字样但不是表引用」的其他 SQL 语法做反查（§2b）。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| tools/generate-data-isolation-spec.mjs | 140 | 6650 | c9311231ec932fb65a3776f882078ed4d7ba8d4cea0c6c79032861c8cea944ba | 全读 |
| tools/generate-domain-ownership-scope-audit.mjs | 233 | 14340 | 47087d2b8330828419f79397ba85be7dd8aaca0b281900cdf6180d26d0898e19 | 全读 |
| tools/verify-tenant-isolation.mjs | 339 | 16841 | 56dbdbd745afa7fa8c2326262082d9ba10509c855f8dfad7d9355fd2f966395f | 全读 |
| tools/verify-test-db-isolation.mjs | 33 | 1445 | 6c36a678982b21189ff3a14961be56ff7dd945d10709cdccb9c44d1d4007ffea | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m029_b5r1_run.log；B1 同一取证脚本，worktree 内为 B3-r1 修后状态）

```
START=2026-09-21T03:00:18Z HEAD=e456ba96 origin/main=6f3d25d7
== 0. files (lines/bytes/sha256) + three states
tools/generate-data-isolation-spec.mjs 140 6650 c9311231ec932fb65a3776f882078ed4d7ba8d4cea0c6c79032861c8cea944ba | vsHEAD[ 1 file changed, 5 insertions(+), 5 deletions(-)] vsOM[ 1 file changed, 5 insertions(+), 5 deletions(-)] | // 文件版本：V0.01
tools/generate-domain-ownership-scope-audit.mjs 233 14340 47087d2b8330828419f79397ba85be7dd8aaca0b281900cdf6180d26d0898e19 | vsHEAD[ 1 file changed, 2 insertions(+), 1 deletion(-)] vsOM[ 1 file changed, 2 insertions(+), 1 deletion(-)] | // 文件版本：V0.04
tools/verify-tenant-isolation.mjs 339 16841 56dbdbd745afa7fa8c2326262082d9ba10509c855f8dfad7d9355fd2f966395f | vsHEAD[ 1 file changed, 3 insertions(+)] vsOM[ 1 file changed, 3 insertions(+)] | // 文件版本：V0.01
tools/verify-test-db-isolation.mjs 33 1445 6c36a678982b21189ff3a14961be56ff7dd945d10709cdccb9c44d1d4007ffea | vsHEAD[] vsOM[] | #!/usr/bin/env node
node: v24.21.0 ; syntax: ok ok ok ok 
== 1. run all four tools in a disposable worktree at origin/main
patched tools + regenerated outputs copied into scratch (B3-r1 state)
SPEC_CHECK_EXIT=0
CSV_CHECK_EXIT=0
TENANT_GATE_EXIT=0
TENANT_GATE_STRICT_EXIT=1
TESTDB_GATE_EXIT=0
TESTDB_GATE_WRONG_CWD_EXIT=0
baseline known_findings: 4
java files with inline SQL outside the Jdbc*Repository scan surface: 17
backend/src/main/java/com/complex/platform/compliance/JdbcWecomWebhookNonceStore.java
backend/src/main/java/com/complex/platform/ticketing/TicketFacePhotoRepository.java
backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingSql.java
backend/src/main/java/com/complex/platform/settlement/RevenueStreamAttributionRepository.java
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java
backend/src/main/java/com/complex/platform/auth/ProductionAuthService.java
scratch clean
scratch removed
[m029_t1] data isolation spec is in sync with Flyway migration scope baseline.
[m029_t2] DOMAIN_OWNERSHIP_SCOPE_PASS: 335 tables classified
[m029_t4] TEST_DB_ISOLATION_PASS: 测试源未引用生产库 complex（均用 complex_test）
[m029_t4_wrongcwd] TEST_DB_ISOLATION_PASS: 测试源未引用生产库 complex（均用 complex_test）
[m029_t3]
[tenant-isolation] 扫描范围：
  - 权威表清单（CSV）：335 表（direct=173，白名单豁免=82，inherited 为信息性盲区）
  - Jdbc*Repository 源：53 个文件
  - 本次扫出 finding：2（已登记 baseline：2，新增回归：0）
[tenant-isolation] KNOWN-BLINDSPOT B1（inherited，仅信息性，不硬失败）：73 表
    · amusement_incident_actions (scope_source: inspection/incident id)
    · amusement_safety_actions (scope_source: inspection/incident id)
    · analytics_ai_thresholds (scope_source: dimension/project/merchant)
    · analytics_customer_intents (scope_source: dimension/project/merchant)
    · analytics_metric_anomalies (scope_source: dimension/project/merchant)
    · analytics_metric_definitions (scope_source: dimension/project/merchant)
    · …其余 67 表略
[tenant-isolation] 注意：baseline 中 2 条已不再复现（疑似已修复），应从 baseline 与台账移除以收紧闸口：
    · F1:UNSCOPED_DIRECT_READ:access_grant_requests
    · F2:UNREGISTERED_TABLE:d

[m029_t3s FULL --strict output]
[tenant-isolation] 扫描范围：
  - 权威表清单（CSV）：335 表（direct=173，白名单豁免=82，inherited 为信息性盲区）
  - Jdbc*Repository 源：53 个文件
  - 本次扫出 finding：2（已登记 baseline：2，新增回归：0）
[tenant-isolation] KNOWN-BLINDSPOT B1（inherited，仅信息性，不硬失败）：73 表
    · amusement_incident_actions (scope_source: inspection/incident id)
    · amusement_safety_actions (scope_source: inspection/incident id)
    · analytics_ai_thresholds (scope_source: dimension/project/merchant)
    · analytics_customer_intents (scope_source: dimension/project/merchant)
    · analytics_metric_anomalies (scope_source: dimension/project/merchant)
    · analytics_metric_definitions (scope_source: dimension/project/merchant)
    · …其余 67 表略
[tenant-isolation] 注意：baseline 中 2 条已不再复现（疑似已修复），应从 baseline 与台账移除以收紧闸口：
    · F1:UNSCOPED_DIRECT_READ:access_grant_requests
    · F2:UNREGISTERED_TABLE:d

[tenant-isolation] FAIL: strict 模式， 2 项租户隔离问题（fail-closed，阻断合入）：
  - [F1 UNSCOPED_DIRECT_READ] 表 'integration_channel_store_map'（direct, scope=merchant_id）在 backend/src/main/java/com/complex/platform/integration/JdbcIntegrationRepository.java 中有 SELECT 读，但找不到任何带 'merchant_id = :' 的 scoped 查询 —— 服务端无法按 token scope 过滤该表，存在跨租户读越权风险（AC-ISO-1，§4）。
  - [F1 UNSCOPED_DIRECT_READ] 表 'revenue_stream_attribution'（direct, scope=project_id）在 backend/src/main/java/com/complex/platform/settlement/JdbcSettlementConfigWriteRepository.java 中有 SELECT 读，但找不到任何带 'project_id = :' 的 scoped 查询 —— 服务端无法按 token scope 过滤该表，存在跨租户读越权风险（AC-ISO-1，§4）。

[tenant-isolation] 处置：修复未隔离查询 / 在 CSV 登记新表 / 显式标白名单后重跑；已知缺口须在 security-scans/findings-register-v1.md tenant-isolation 小节登记证据（§6A.2），并将其 key 纳入 security-scans/tenant-isolation-baseline.json 后方可暂存。
other FROM-bearing non-table syntaxes in Jdbc*Repository sources: SUBSTRING..FROM=0 TRIM..FROM=0 OVERLAY..FROM=0 DISTINCT-FROM=14
== 2. generator outputs vs real schema (complex_test has V427 applied, origin/main does not: business_entity_projects / merchants.project_id are expected extras)
CSV tables 335 DB tables 336 | DB-only: ['business_entity_projects'] | CSV-only: []
CSV direct rows whose scope_source column does NOT exist in DB: []
CSV non-direct rows that DO have a direct-scope column in DB: []
CSV direct rows where chosen column != first-priority existing column: [('merchants', 'business_entity_id', ['project_id'])]
auto-block tables 170 DB tables with a scope column 172
DB has scope column but auto-block misses table: ['business_entity_projects', 'merchants']
auto-block lists table that DB lacks: []
auto-block lists column that DB table lacks: []
DB scope column missing from auto-block row: []
== 3. test-db gate regex: positive/negative samples
FLAG  jdbc:postgresql://127.0.0.1:5432/complex
FLAG  jdbc:postgresql://127.0.0.1:5432/complex?sslmode=disable
pass  jdbc:postgresql://h/complex_test
pass  jdbc:postgresql://h/complex_test_m09
pass  jdbc:postgresql://h/complex_dev
pass  jdbc:postgresql://h/complexity
FLAG  ${COMPLEX_DATABASE_URL:jdbc:postgresql://h/complex}
files scanned by test-db gate: 270 ; other resource types under src/test: 
== 4. package.json wiring
docs:data-isolation-spec -> node tools/generate-data-isolation-spec.mjs | in predeploy: 0 in delivery-gates: 0
verify:data-isolation-spec -> node tools/generate-data-isolation-spec.mjs --check | in predeploy: 1 in delivery-gates: 1
docs:domain-ownership-scope -> node tools/generate-domain-ownership-scope-audit.mjs | in predeploy: 0 in delivery-gates: 0
verify:domain-ownership-scope -> node tools/generate-domain-ownership-scope-audit.mjs --check | in predeploy: 1 in delivery-gates: 1
verify:tenant-isolation -> node tools/verify-tenant-isolation.mjs | in predeploy: 1 in delivery-gates: 1
verify:test-db-isolation -> node tools/verify-test-db-isolation.mjs | in predeploy: 1 in delivery-gates: 0
== 5. tests for these tools
tools/verify-entry-anchors.test.mjs
tools/generate-formal-security-baseline.mjs
tools/gate-hardening-tests/99-predeploy-final.log
== 6. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T03:00:20Z
```

## 2b. 补充反查：其他含 FROM 字样的非表引用语法

- 在 53 个 Jdbc*Repository 源里检索 `SUBSTRING(… FROM …)`、`TRIM(… FROM …)`、`OVERLAY(… FROM …)`、`POSITION(… IN …)`：见 §2 日志后附的 --strict 全文——修后 strict finding 只剩既有已登记的 F1 缺口，无任何单字母或列名形态的「未登记表」，说明现仓储里没有第三种会被误当表名的 FROM 语法。

## 3. 三线审计

- 第一性原理线：用途/实际行为同 B1。修后：规格自动区块类型只在规格 §1 闭集内；两份产物对同一张表的分类不再矛盾（`business_process_step_records` 两边均为 inherited 语义、venue 两表两边均为 direct）；隔离机审不再把比较运算符当表引用。
- 顺从式线：18 判据逐项——B1 经 Grok 指出不通过的 GEN-002/008/016 由 001、002 闭合；GEN-001（登记）/003（登记）/004/005/006/009/012/013/017/018 同 B1；GEN-007/010/011/014/015 无本面。
- 对抗式线（grilling）：两个生成器输出再对一遍库（§2 探针 2）→仍只差未发布 V427 的两项，无新增漏表/错列；`scopeType` 恒 direct 会不会把不该列的表标错→该区块只收「迁移里出现过六个 scope 列之一」的表，按规格 §1 定义即 direct，CSV 对这 170 张也全是 direct；新增的 inherited 规则会不会误伤别的表→模式是全名锚定；`IS_DISTINCT` 中和会不会吃掉真实的 `FROM <表>`→只替换运算符三/四个词本身，其后的标识符原样保留；--strict 还红不红→红（既有 F1 缺口），默认路径绿；baseline 的 `d` 键→工具提示「已不再复现」，属 M030。无新缺陷、无添加项、无 HTML 问题。

## 4. 已定稿 001、002 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。

## 5. 本次 Findings

无新增。

## 6. 观察（登记）

- 同 B2-r1 定稿的登记项（写死 covered 致 uncovered 检查不可达；测试库门禁根目录缺失静默 PASS；扫描面外 17 个含 SQL 的 Java 文件；baseline 陈旧键 access_grant_requests 与修后新增的陈旧键 d（M030）；DROP COLUMN／RENAME TABLE 未解析；V1000 排序分叉；无自带单测等）。

## 7. 结论

席甲 B5-r1 票：**PASS**。4 文件全读、18 判据逐项、取证脚本实跑复核；未改任何已跟踪文件；落盘前未读 Grok 本次正文。
