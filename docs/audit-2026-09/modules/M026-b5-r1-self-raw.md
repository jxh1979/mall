CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M026 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；席甲 B1 的 PASS 已被 Grok 推翻过一次，以 Grok 票为准）。2026-09-20T23:50:34Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；B3-r1 after tree `22d70caf574f878827e9d2afbd6bf9c62cb696e1`。
- 模块 M026 1 文件（§1 全读），21 判据；B3 累计 1。
- 顺序：Grok 已先派（modules/M026-b5-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件；数据库只对 complex_test SELECT。
- 本轮补的方法（B1 把现在时的收口声称读成历史说明）：注释里每个动词按其时态当作当前声称去仓内复现。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/common/jdbc/JdbcTimestamps.java | 29 | 1057 | 3ba59fa2c9f69c48d4ed0a95ca39cf9d3f28419f030bf44fb310d293012bf634 | 全读（L1-29） |

## 2. 可执行证据原文（/home/james/audit-tmp/m026_b5r1_run.log；B1 同一取证脚本修后重跑）

```
START=2026-09-20T23:50:34Z HEAD=e456ba96 origin/main=28cfc46f
== 0. file (lines/bytes/sha256)
backend/src/main/java/com/complex/platform/common/jdbc/JdbcTimestamps.java 29 1057 3ba59fa2c9f69c48d4ed0a95ca39cf9d3f28419f030bf44fb310d293012bf634
== 1. vs HEAD / vs origin/main / history
 1 file changed, 3 insertions(+), 3 deletions(-)
 1 file changed, 3 insertions(+), 3 deletions(-)
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
END=2026-09-20T23:50:34Z
```

说明同 B1：探针 4 第一条汇总 SQL 写错、输出为报错行，不作证据；探针 5 的 glob 计数为 0 系路径层级所致，实数 77（B1 §2）；探针 4 的 7 条同名异型列已在 B1 §2 逐个追过调用方，本轮复核无变化。

## 2b. 类注释完成口径/同构副本/单文件 javac/方法体指纹（/home/james/audit-tmp/m026_b5r1_check.log）

```
completion-claim lines in javadoc: 0 ; equivalent private helpers elsewhere in main: 9
JAVAC_EXIT=0
method bodies sha256 (from 'public final class' to EOF): 4b9880a6da40f02496b5b9f873bc94386a9f89dc2a5a5109460dad9a3d1c4699
first line: // 文件版本：V0.01
problems=0
CHECK_EXIT=0
```

## 3. 三线审计

- 第一性原理线：用途/实际行为同 B1。类注释现只陈述「本类是什么、由 import 本类的仓储与参数源使用」，可复现（48 个文件 import）；两条方法注释「Converts an Instant to a UTC OffsetDateTime for binding to a timestamptz column」「Reads a nullable timestamptz column as an Instant」与方法体一致。
- 顺从式线：21 判据逐项——B1 经 Grok 指出不通过的 GEN-016 由 001 闭合；GEN-001/002/003（间接）/006/008/009/012/013/017/018 同 B1 通过；BSM-001、BSM-003、PGM-001 与 GEN-004/005/007/010/011/014/015 无本面（理由同 B1 §4）。
- 对抗式线（grilling）：注释里还有没有现在时的范围声称→「used by … that import this class」按定义成立；「Shared」→48 个调用文件，成立；传 null、非 UTC 会话时区、date/varchar 列→同 B1，无此调用；方法体是否被顺手改动→指纹与修前一致；别的模块 9 处同构副本→B2 双席定为只登记，本类注释不再对它们作任何声称。无新缺陷、无添加项、无 HTML 问题。

## 4. 已定稿 001 现态

已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。

## 5. 本次 Findings

无新增。

## 6. 观察（登记）

- 同 B1/B2 登记项（别的模块里的同构副本；无直测、77 个真库测试类间接守住；英文类注释非界面文字；pgjdbc 对无时区列两种读法的差异仅涉 flyway 列）。

## 7. 结论

席甲 B5-r1 票：**PASS**。1 文件全读、21 判据逐项、取证脚本与检查复核；未改任何文件；落盘前未读 Grok 本次正文。
