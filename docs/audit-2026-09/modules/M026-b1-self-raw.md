CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M026 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；本文件非席甲所写）。2026-09-20T23:17:05Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；本文件与 HEAD、origin/main（28cfc46f）均无差异。
- 模块 M026「租户范围与数据隔离（技术不变量）／后台实现／第1段」1 文件，21 判据（BSM-001、BSM-003、PGM-001、GEN-001…018）。B3 累计 0。
- 顺序：Grok 已先派（modules/M026-b1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件；数据库只对 complex_test SELECT。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/common/jdbc/JdbcTimestamps.java | 29 | 1064 | 369ffc5be326f26334ee3291c67e1231e8fda509f455a17650119ef28189ac6a | 全读（L1-29） |

## 2. 可执行证据原文（/home/james/audit-tmp/m026_b1_run.log）

```
START=2026-09-20T23:14:27Z HEAD=e456ba96 origin/main=28cfc46f
== 0. file (lines/bytes/sha256)
backend/src/main/java/com/complex/platform/common/jdbc/JdbcTimestamps.java 29 1064 369ffc5be326f26334ee3291c67e1231e8fda509f455a17650119ef28189ac6a
== 1. vs HEAD / vs origin/main / history
b1fd6534 feat: 后端域服务重构+动态定价/工单/督导, 前端admin+legacy-console边界与二次确认, 文档R6收口
== 2. call surface
call sites: timestampWithTimeZone=654 nullableInstant=337 files=48
== 3. leftover private copies the javadoc says were centralized
private/static redefinitions of timestampWithTimeZone: 0
private/static redefinitions of nullableInstant: 2
backend/src/main/java/com/complex/platform/merchantservice/JdbcMerchantServiceRepository.java:316:    private Instant nullableInstant(ResultSet resultSet, String columnName) throws SQLException {
backend/src/main/java/com/complex/platform/ops/productionreadiness/JdbcProductionReadinessRepository.java:197:    private Instant nullableInstant(ResultSet resultSet, String columnName) throws SQLExce
inline atOffset(ZoneOffset.UTC) outside the helper (main): 6
== 4. columns read through nullableInstant(rs, "col") vs complex_test column types
distinct literal column names: 82
schema-wide timestamp column types (public): ERROR:  aggregate functions are not allowed in GROUP BY LINE 1: select data_type||'='||count(*) from information_schema.colu...                                ^ 
of the literal column names, any column with that name NOT timestamptz: 7
compliance_expiry_items.valid_from date
compliance_expiry_items.valid_to date
supplier_qualifications.valid_from date
supplier_qualifications.valid_to date
supply_contracts.valid_from date
supply_contracts.valid_to date
terminal_sync_tasks.occurred_at character varying
timestamp WITHOUT time zone columns (all): 
flyway_schema_history.installed_on
== 5. tests
tests naming JdbcTimestamps: 0
Jdbc*Tests touching a real DB (files): 0
== 6. driver
84:            <artifactId>postgresql</artifactId>
85-            <scope>runtime</scope>
86-        </dependency>
42.7.11 
== 7. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-20T23:14:27Z
```

如实说明与补充只读核对：
- 探针 4 第一条汇总 SQL 写错（GROUP BY 里用了聚合），输出为报错行，不作证据；其余各条有效。探针 5「Jdbc*Tests … (files): 0」是 glob 只匹配了一层目录所致，实数见下。探针执行时在 160 的 /tmp 下建过一个列名清单临时文件，脚本末已删除。
- 探针 4 列出的 7 条「同名但非 timestamptz」逐个追调用方：读 `valid_from`/`valid_to` 的是 `JdbcTicketingRowMapper:122-123`、`JdbcAccessRepository:1184-1185/1317/1354/1430`；读 `occurred_at` 的是 `JdbcTraceabilityRepository:236`、`JdbcEvidenceEventRepository:277/309`、`JdbcMdmRepository:303`（表 mdm_policy_violations）。complex_test 内 `occurred_at` 为 timestamptz 的 6 张表含 evidence_events、audit_events、mdm_policy_violations；varchar 的只有 terminal_sync_tasks（仅 `JdbcTicketingRepository` 引用该表，不经本工具读该列）。date 型 valid_from/valid_to 所在的 compliance_expiry_items、supplier_qualifications、supply_contracts 无调用方经本工具读取。
- 间接测试面：`find backend/src/test/java -name "*Jdbc*Tests.java"` = 77；非空与空两条分支的断言原文见 §4 GEN-003。
- 遗留私有拷贝原文：`JdbcMerchantServiceRepository.java:316-319` 与 `JdbcProductionReadinessRepository.java:197-200` 各有私有 `nullableInstant`，实现为 `resultSet.getTimestamp(col)` → `toInstant()`（与本类的 OffsetDateTime 路径不同写法、同结果）。主代码另有 6 处内联 `atOffset(ZoneOffset.UTC)`（inventory、ticketing、governance），均在别的模块。

## 3. 三线审计

- 第一性原理线。用途：让所有 JDBC 仓储用同一种方式在 `Instant` 与 PostgreSQL timestamptz 之间互转。实际行为：写入按 UTC 偏移绑定，读出取绝对时刻。未经验证的前提有两条，均已验证：①被读的列都是 timestamptz（§2 逐个追过）；②驱动支持 `OffsetDateTime`（pgjdbc 42.7.11）。
- 顺从式线：21 判据逐项见 §4；29 行逐行读过。
- 对抗式线（grilling）：传 null→两方法均返回 null，不抛；数据库会话时区不是 UTC→读出的是绝对时刻，`toInstant()` 不受影响；列是 date/varchar→驱动会抛转换异常，但现无此调用（§2）；有人新增对 date 列的调用→由其所在仓储的真库测试第一次运行即暴露，不为此在工具里加防御；方法改坏有没有测试会红→§4 GEN-003 列的真库断言会红；类注释是否夸大→说的是「此前各处复制」，本类行为与注释一致，遗留拷贝在别的模块（OBS-1）。

## 4. 21 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BSM-001 整体说明：这套系统到底管什么 | 无本面｜本文件只做 Instant 与 timestamptz 的互转，不承载、不展示、不声称「整体说明：这套系统到底管什么」相关对象、状态或界面文字 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 无本面｜本文件只做 Instant 与 timestamptz 的互转，不承载、不展示、不声称「系统通用能力：数据可改可删、操作有引导、权限分岗」相关对象、状态或界面文字 |
| GEN-001 必要性与最小性 | 适用｜29 行、两个静态方法、私有构造；全后端 48 个文件约 991 处调用（探针 2），有存在理由，无多余分支｜通过 |
| GEN-002 可执行正确性 | 适用｜`timestampWithTimeZone`：null→null，否则 `instant.atOffset(UTC)`，绑定到 timestamptz 参数；`nullableInstant`：`getObject(col, OffsetDateTime.class)`，null→null，否则 `toInstant()`。驱动 pgjdbc 42.7.11（探针 6）支持 timestamptz↔OffsetDateTime；经本工具读取的 82 个字面列名，对应被读的表中该列均为 timestamptz（探针 4 的 7 条同名异型列逐个核过：compliance/supplier/supply_contracts 的 date 型 valid_from/valid_to 与 terminal_sync_tasks 的 varchar 型 occurred_at 均不经本工具读取，读 valid_from/valid_to/occurred_at 的是 ticketing、access、evidence、mdm 仓储，其表内该列为 timestamptz）｜通过 |
| GEN-003 测试有效性 | 适用｜无直接命名 JdbcTimestamps 的测试；间接由 77 个 *Jdbc*Tests 真库往返守住两条分支——非空：`AmusementJdbcIntegrationTests:289` `createdAt()).isEqualTo(now)`、`StaffJdbcIntegrationTests:655`、`ServiceOrderJdbcIntegrationTests:210`；空：`M006JdbcRegressionTests:244` `deletedAt()).isNull()`、`BarPosCatalogJdbcIntegrationTests:437`。把任一方法改坏，这些断言即红｜通过（间接） |
| GEN-004 信任边界校验 | 无本面｜纯转换工具，不接触外部输入 |
| GEN-005 授权与最小权限 | 无本面｜不涉授权 |
| GEN-006 数据完整性 | 适用｜统一按 UTC 偏移写入、按绝对时刻读出，与会话时区无关，不丢时刻信息；全库业务表无 timestamp without time zone 列（仅 flyway_schema_history.installed_on，不经本工具）｜通过 |
| GEN-007 并发与幂等 | 无本面｜无状态、无共享可变量 |
| GEN-008 接口与模式一致性 | 适用｜方法签名稳定；调用方统一 `import static`；与 JDBC 4.2 `getObject(…, OffsetDateTime.class)` 约定一致｜通过 |
| GEN-009 错误处理与恢复 | 适用｜`SQLException` 原样上抛由调用方/Spring 转译，不吞异常｜通过 |
| GEN-010 安全与敏感信息 | 无本面｜不处理敏感信息 |
| GEN-011 可观测与审计证据 | 无本面｜不产生审计事件 |
| GEN-012 资源与性能边界 | 适用｜每次调用常数开销、无分配外资源｜通过 |
| GEN-013 构建配置与依赖 | 适用｜只依赖 JDK `java.sql`/`java.time`；驱动为 runtime 依赖（pom.xml:84）｜通过 |
| GEN-014 数据库迁移正确性 | 无本面｜不是迁移 |
| GEN-015 界面可用性与可访问性 | 无本面｜无界面；类注释为英文属代码注释，不是界面文字 |
| GEN-016 文档可验证性 | 适用｜类注释称该映射「previously copy-pasted verbatim across every JDBC repository」并由本类集中——`timestampWithTimeZone` 的私有重定义现为 0（属实）；`nullableInstant` 仍有 2 处私有同名方法留在别的模块（见 OBS-1），注释说的是「此前」的状态，本类自身行为与注释一致｜通过（登记 OBS-1） |
| GEN-017 业务来源分类 | 适用｜技术性内容，无业务规则，无业务发明｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到只读命令输出（m026_b1_run.log、两条补充 SELECT/grep）与源码原文｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 无本面｜本文件只做 Instant 与 timestamptz 的互转，不承载、不展示、不声称「整体定位：平台治理层不是一个业务部门，而是所有业务的底座」相关对象、状态或界面文字 |

## 5. 本次 Findings

无。

## 6. 观察（登记，不立项）

- OBS-1（跨模块）：`nullableInstant` 仍有 2 处私有同名拷贝（merchantservice、ops/productionreadiness 两个仓储），另有 6 处内联 `atOffset(ZoneOffset.UTC)`；均不在本模块文件内，属各自模块的收口项。
- OBS-2：无直接单测；由 77 个真库测试类间接守住。对 29 行无分支逻辑的转换工具，不为补一个直测立项。
- OBS-3：类注释为英文；属代码注释，不是任何终端的界面文字，PGM-001 无本面。

## 7. 结论

席甲 B1 票：**PASS**（无工程缺陷；OBS-1…3 登记）。1 文件全读、21 判据逐项、三线完成；未改任何文件；落盘前未读 Grok 本次正文。按 V0.17：即使两席 B1 均无发现，也须经 B2 登记定稿、跳过 B3/B4、B5 全量确认后才收口。


## M026 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-20T23:33:47Z。输入：modules/M026-b1-grok-raw.md V0.01（23976 bytes，SHA256 c658018655d80d12941959900bb6b6e1a2231d005e525b43db8dbdd9d28d7b21）、modules/M026-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M026 B2-r1 整批候选」段（全文件 SHA256 e3660cfc207102cd43e71ca71a68411dd67eba9d8f81b9e935d69e579db12357）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-01 与席甲 OBS-1 同一事实 → 001；两席其余观察全部登记；无漏并、无改写。
- 自审纠正：席甲 B1 §4 GEN-016 行判「通过（登记 OBS-1）」，理由是注释讲的是「此前」状态——主句 Centralizes 是现在时的收口声称，而仓内同构转换仍有多处不经本类（席甲自己的探针 3 就查到 2 处私有 nullableInstant 与 6 处内联转换，Grok 另查到 offset/instant 同构方法）。该读法过宽，B1 票 PASS 撤回为 FAIL，采 Grok。
- 001 分类技术性：无业务发明——只涉时间戳转换代码的收口范围描述；三 HTML 无上位句可指，故非细化；非添加。处置只改一句注释 + 文件版本标记，方法体不动，不迁移别的模块副本。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
