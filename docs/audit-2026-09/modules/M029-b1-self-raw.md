CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M029 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；四个文件非席甲所写）。2026-09-21T02:16:43Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；4 个文件与 HEAD、origin/main（6f3d25d7）均无差异。
- 模块 M029「租户范围与数据隔离（技术不变量）／检查及生成工具／第1段」4 文件，18 判据（GEN-001…018，无 HTML 条目）。B3 累计 0。
- 环境前提（D-028）：主工作区含未发布的 V427 与别的模块暂存改动，工具实跑证据取自基于 origin/main 的一次性 worktree（已删除）。
- 顺序：本席取证脚本先跑完（避免与 Grok 同时建一次性 worktree），随后派 Grok，再落本报告；落盘前未读 Grok 正文（modules/M029-b1-grok-raw.md 存在=False）。只读承诺：未改任何已跟踪文件；未带 --write-baseline；数据库只对 complex_test SELECT。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| tools/generate-data-isolation-spec.mjs | 140 | 6664 | 14d5ca6b2083890691b4ac188a597395f9f2bbe7726fc4c596e83266e3076abf | 全读 |
| tools/generate-domain-ownership-scope-audit.mjs | 232 | 14174 | ae8568fa8b1091f694a3462785088ee28023b757a41585298e6fa1af0ec14bfa | 全读 |
| tools/verify-tenant-isolation.mjs | 336 | 16578 | dfec43c3fcc65759297af89d805826fff9b7082e82615b905a00e73068d959e0 | 全读 |
| tools/verify-test-db-isolation.mjs | 33 | 1445 | 6c36a678982b21189ff3a14961be56ff7dd945d10709cdccb9c44d1d4007ffea | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m029_b1_run.log）

```
START=2026-09-21T02:14:15Z HEAD=e456ba96 origin/main=6f3d25d7
== 0. files (lines/bytes/sha256) + three states
tools/generate-data-isolation-spec.mjs 140 6664 14d5ca6b2083890691b4ac188a597395f9f2bbe7726fc4c596e83266e3076abf | vsHEAD[] vsOM[] | // docs:data-isolation-spec / verify:dat
tools/generate-domain-ownership-scope-audit.mjs 232 14174 ae8568fa8b1091f694a3462785088ee28023b757a41585298e6fa1af0ec14bfa | vsHEAD[] vsOM[] | // 文件版本：V0.03
tools/verify-tenant-isolation.mjs 336 16578 dfec43c3fcc65759297af89d805826fff9b7082e82615b905a00e73068d959e0 | vsHEAD[] vsOM[] | // verify:tenant-isolation
tools/verify-test-db-isolation.mjs 33 1445 6c36a678982b21189ff3a14961be56ff7dd945d10709cdccb9c44d1d4007ffea | vsHEAD[] vsOM[] | #!/usr/bin/env node
node: v24.21.0 ; syntax: ok ok ok ok 
== 1. run all four tools in a disposable worktree at origin/main
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
  - 权威表清单（CSV）：335 表（direct=173，白名单豁免=83，inherited 为信息性盲区）
  - Jdbc*Repository 源：53 个文件
  - 本次扫出 finding：4（已登记 baseline：4，新增回归：0）
[tenant-isolation] KNOWN-BLINDSPOT B1（inherited，仅信息性，不硬失败）：72 表
    · amusement_incident_actions (scope_source: inspection/incident id)
    · amusement_safety_actions (scope_source: inspection/incident id)
    · analytics_ai_thresholds (scope_source: dimension/project/merchant)
    · analytics_customer_intents (scope_source: dimension/project/merchant)
    · analytics_metric_anomalies (scope_source: dimension/project/merchant)
    · analytics_metric_definitions (scope_source: dimension/project/merchant)
    · …其余 66 表略
[tenant-isolation] 注意：baseline 中 1 条已不再复现（疑似已修复），应从 baseline 与台账移除以收紧闸口：
    · F1:UNSCOPED_DIRECT_READ:access_grant_requests

[tenant-isolation] PASS（棘轮）：无新增回归；4 条已知缺口在台账跟踪闭环中（见 findings-register-v1.md tenant-isolation 小节）。隔离面未劣化。
[m029_t3s tail]
  - [F1 UNSCOPED_DIRECT_READ] 表 'integration_channel_store_map'（direct, scope=merchant_id）在 backend/src/main/java/com/complex/platform/integration/JdbcIntegrationRepository.java 中有 SELECT 读，但找不到任何带 'merchant_id = :' 的 scoped 查询 —— 服务端无法按 token scope 过滤该表，存在跨租户读越权风险（AC-ISO-1，§4）。
  - [F1 UNSCOPED_DIRECT_READ] 表 'revenue_stream_attribution'（direct, scope=project_id）在 backend/src/main/java/com/complex/platform/settlement/JdbcSettlementConfigWriteRepository.java 中有 SELECT 读，但找不到任何带 'project_id = :' 的 scoped 查询 —— 服务端无法按 token scope 过滤该表，存在跨租户读越权风险（AC-ISO-1，§4）。

[tenant-isolation] 处置：修复未隔离查询 / 在 CSV 登记新表 / 显式标白名单后重跑；已知缺口须在 security-scans/findings-register-v1.md tenant-isolation 小节登记证据（§6A.2），并将其 key 纳入 security-scans/tenant-isolation-baseline.json 后方可暂存。
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
END=2026-09-21T02:14:16Z
```

说明：探针 2 里「CSV direct rows where chosen column != first-priority existing column」命中的 merchants 一行，是因为测试库已应用未发布的 V427（merchants.project_id），origin/main 迁移里没有该列，生成器选 business_entity_id 无误。

## 3. 三线审计

- 第一性原理线。用途：让「迁移里有哪些带范围的表、每张表怎么分类、仓储读路径有没有按范围过滤、测试源有没有指向生产库」四件事由机器守住。实际行为：探针 1 四个工具全部按设计运行；探针 2 证实两个生成器的解析对现有全部迁移零漏零错。未经验证的前提：①正则解析能覆盖迁移里出现过的全部建表/加列写法——以库对照验证，成立；②机审的扫描面足够——头注自认盲区 B1–B4，与代码一致；③门禁「能红」——三个能红，测试库门禁在根目录缺失时不能（OBS-2）。
- 顺从式线：18 判据逐项见 §4；四个文件 741 行逐行读过。
- 对抗式线（grilling）：新增一张带 merchant_id 的表却不重生成→两个 --check 红；新增一张带租户数据但没有任何 scope 列的表→CSV 自动归 single_complex_default 并进白名单，机审不拦——这是 CSV 备注里写明的现行姿态（「多综合体上线前补列或写明父表 join」），76 张表同此，不是解析错误；仓储里新写一条对 direct 表的无范围 SELECT→只要同文件别处有 `col = :` 就不报（头注 B3 已登记的近似）；SQL 写在非 Jdbc*Repository 文件里→不扫（头注 B4）；有人把生产库 URL 写进测试配置→红；从仓库根以外目录直接 node 运行测试库门禁→打印 PASS（OBS-2，npm 路径不触发）；baseline 里已修复的条目→只提示不失败（现有 1 条 access_grant_requests 陈旧项，属 security-scans 基线文件所在模块）。

## 4. 18 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| GEN-001 必要性与最小性 | 适用｜四个工具各一职：两个生成器、隔离机审、测试库门禁，均接入 predeploy。`generate-domain-ownership-scope-audit.mjs:203-204` 把 `file_version`／`status` 写死为 V0.01／covered，使 `:222-227` 的「uncovered rows」检查永远不可能命中（死代码）——无行为影响，登记 OBS-1｜通过（登记） |
| GEN-002 可执行正确性 | 适用｜在基于 origin/main 的一次性 worktree 里四个工具实跑全部 exit 0（探针 1）。两个生成器的输出逐表逐列对 complex_test 真实结构（探针 2）：CSV 335 表 = 库内表集合（只差未发布 V427 的 business_entity_projects）；CSV 标 direct 的行所指列在库内全部存在；非 direct 的行在库内都确实没有任何 direct 列；规格自动区块 170 表与库内「含 scope 列的表」只差 V427 的两项，列集合逐表一致。解析器对现有全部迁移零漏零错｜通过 |
| GEN-003 测试有效性 | 适用｜四个工具自身无单测（tools 下只有 `verify-entry-anchors.test.mjs` 属别的工具）；生成器的正确性由探针 2 的库对照旁证，两个 --check 能红（M028 D-028 前主工作区即红）；隔离机审 `--strict` 能红（探针 1：exit 1，列出 4 条已登记缺口）；测试库门禁正则正反例 7 条全对（探针 3）。测试库门禁在扫描根目录缺失时打印 PASS（探针 1 错误 cwd 实测 exit 0）——经 npm 从仓库根运行时不触发，登记 OBS-2｜通过（登记） |
| GEN-004 信任边界校验 | 适用｜隔离机审的正则输入为 CSV 表名/列名（硬编码来源），`hasScopedRead` 对列名做 `\w` 过滤；无外部输入｜通过 |
| GEN-005 授权与最小权限 | 适用｜F1 判定「direct 表有读但全仓储无 `<scope_col> = :` 或 `IN (`」；参数级与列级两种读谓词都含 `col = :`，均被识别；COALESCE 哨兵式不被识别（与 M028 规格 §6A.1 一致）。白名单＝global_reference + single_complex_default（83 表），inherited 72 表仅信息性——与文件头 B1–B4 自述一致｜通过 |
| GEN-006 数据完整性 | 适用｜生成器输出稳定排序（表名 localeCompare／sort）；`--check` 为全文比对；CSV 转义处理引号与逗号，机审侧解析同规则｜通过 |
| GEN-007 并发与幂等 | 无本面 |
| GEN-008 接口与模式一致性 | 适用｜两个生成器的 scope 列集合不同（规格侧 6 列含 space_id/bar_venue_id；CSV 侧 9 列含 venue_id 与 *_entity_id 系列），规格侧对只有场地类列的表标 `inherited-or-location-scope`（规格 §1 图例无此值），CSV 侧把 venue_id 当 direct——M028 转入项，属两份产物口径差，各自 --check 自洽；改生成器标签会牵动已闭环的 M028 产物，登记 OBS-3｜通过（登记） |
| GEN-009 错误处理与恢复 | 适用｜缺 CSV／缺规格／基线 JSON 解析失败／找不到仓储文件均 FATAL 非零退出；生成器找不到插入锚点抛错｜通过 |
| GEN-010 安全与敏感信息 | 无本面｜工具不接触凭据；测试库门禁正是防生产库 URL 进测试源 |
| GEN-011 可观测与审计证据 | 无本面 |
| GEN-012 资源与性能边界 | 适用｜全量读迁移与 53 个仓储源文件，秒级完成｜通过 |
| GEN-013 构建配置与依赖 | 适用｜仅依赖 node 内置模块；package.json 接线：四个 verify 均在 predeploy，前三个另在 delivery-gates（探针 4）｜通过 |
| GEN-014 数据库迁移正确性 | 无本面｜工具只读迁移 |
| GEN-015 界面可用性与可访问性 | 无本面 |
| GEN-016 文档可验证性 | 适用｜文件头自述逐条对代码：机审头注 F1/F2 定义、白名单、盲区 B1–B4、baseline 棘轮与 --strict／--write-baseline 均与实现一致；「查询事实源：全部 Jdbc*Repository.java」与 `collectJdbcRepositories` 的文件名正则一致，扫描面之外另有 17 个含内联 SQL 的 Java 文件（含 `JdbcTicketingSql.java`、`TicketFacePhotoRepository.java` 等），头注 B4 已把「字符串变量 SQL、非 Jdbc*Repository」列为盲区，登记 OBS-4。生成器头注「以迁移目录为事实源」属实｜通过（登记） |
| GEN-017 业务来源分类 | 适用｜全部为技术性工具；无业务规则｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到实跑输出（m029_b1_run.log）、库对照与源码原文｜通过 |

## 5. 本次 Findings

无。

## 6. 观察（登记，不立项）

- OBS-1 `generate-domain-ownership-scope-audit.mjs:203-204` 写死 status=covered，`:222-227` 的 uncovered 检查不可达。无行为影响。
- OBS-2 `verify-test-db-isolation.mjs:26` 根目录缺失时吞异常并最终打印 PASS（错误 cwd 实测 exit 0）；经 `npm run`（cwd 恒为仓库根）不触发。
- OBS-3 两个生成器的 scope 列集合与类型词表不同：规格侧出现图例未定义的 `inherited-or-location-scope`；CSV 侧 venue_id 算 direct、space_id/bar_venue_id 不算；`business_process_step_records` 无匹配的 inherited 规则而落入 single_complex_default（M028 转入项）。两种分类在机审里都不硬失败，属产物口径差；改生成器标签会牵动已闭环 M028 的规格自动区块与 CSV。
- OBS-4 机审扫描面之外有 17 个含内联 SQL 的 Java 文件（`JdbcTicketingSql.java`、`JdbcWecomWebhookNonceStore.java`、`TicketFacePhotoRepository.java`、`RevenueStreamAttributionRepository.java`、`VenueBusinessHoursRepository.java`、`ProductionAuthService.java` 等），头注 B4 已登记为盲区。
- OBS-5 `security-scans/tenant-isolation-baseline.json` 有 1 条已不再复现的陈旧项（F1 access_grant_requests），工具只提示；属基线文件所在模块。
- OBS-6 CSV 生成器按文件名字典序读迁移（现行三位零填充，V999 之前与数值序一致）；`findColumn` 以整段定义的词元匹配列名（含注释文字），现库对照无误判。
- OBS-7 四个工具无自带单测；正确性由门禁自身的红绿与本次库对照旁证。

## 7. 结论

席甲 B1 票：**PASS**（无工程缺陷；OBS-1…7 登记）。4 文件全读、18 判据逐项、三线完成；未改任何已跟踪文件；落盘前未读 Grok 本次正文。按 V0.17：无发现也须经 B2 登记定稿后才进入后续步骤。


## M029 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-21T02:36:50Z。输入：modules/M029-b1-grok-raw.md V0.01（32315 bytes，SHA256 8f4c619b9cb3f2a14695a8914eac5b03eef2752bfe264ac8063975b06d0ea3df）、modules/M029-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M029 B2-r1 整批候选」段（全文件 SHA256 671da1321fb4250a7ddd77acc4ebe5954520793941fd0c35ad19ab8ca5f5e2ca）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-01（＝席甲 OBS-3）→001；GR-02→002；两席其余观察全部登记；无漏并、无改写。
- 自审纠正：①OBS-3 席甲 B1 以「改生成器会牵动已闭环 M028 的产物」为由只登记——发射规格闭集外的类型值、两份产物对同一张表分类矛盾，是现文件即可复现的工具缺陷；牵动别的模块是处置时要处理的流程问题，不是不立项的理由。撤回，采 Grok。②GR-02 席甲漏：探针只 `tail -n 4` 看了 --strict 输出，两条 `表 'd'` 在更上面；复核 --strict 全文、baseline 键与两处 SQL 原文，成立。B1 票 PASS 撤回为 FAIL。
- 001 处置：自动区块只收自带 scope 列的表，按规格 §1 定义恒为 direct，且与 CSV 对现 170 张表的分类完全一致——恒返回 direct 是最小且不引入新词的改法；CSV 侧补一条 inherited 规则与既有 40 余条同形。机械重生成的两份产物随批带入，B4 时这些行归 001。
- 002 处置：一行正则中和，与既有 EXTRACT 中和同形；baseline 不动（M030）。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
