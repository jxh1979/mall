CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M026 B5-r1 Grok 席乙完整报告

- 版本：V0.01
- 步骤：B5-r1（修复后整模块三线全量重审，不只看 diff；B3 累计 1）
- 席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区 HEAD `e456ba96c84185c8d39f18f548011daac48a6466`（本文件相对 HEAD 仍为未提交的 B3-r1 工作区改动，与 after tree 一致）
- 模块：`plan/modules.md` V0.04「租户范围与数据隔离（技术不变量）／后台实现／第1段」；标记 NORMAL；FILES 闭集 1 文件；计划有效内容 29 行；after tree `22d70caf574f878827e9d2afbd6bf9c62cb696e1`
- 判据锁：`plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（未抄 sidecar / ledger / 派发词 / 其它报告）
- 映射 21 项：`M026 → [BSM-001, BSM-003, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001]`
- findings.md V2.85 只读背景：M026-001 修复队列「否（已修，B3-r1）」；当前队列「是」= 0。本席不把「全部否」当作文件已全对，本轮按对象重审全文。
- 独立性：未读 `M026-b5-r1-self-raw.md`；未用 MCP / 子代理 / 第三席；未问人；未改业务代码 / 三 HTML / 锁 / findings / ledger；未 fetch/commit/push/merge/deploy；未建脚本、日志或临时审计产物。旧 B1/B2/B4 可读作背景，行为证据本轮当场重跑，不把 B2/B4 票当本步结论。
- 结论：**PASS**（1 文件全文覆盖、21 项逐项核对、三线完成；M026-001 完成口径已落地且无回归；无新的未定稿工程缺陷；无添加项；无 questions）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md，不是提一嘴。grilling 本体「等用户回答 / 派子代理」作废：只借追问结构，事实自读。

| 技能 | 存在性 | 本轮用法 |
|---|---|---|
| using-superpowers | 已读 | 开工技能路由；用户指令优先于 Arc 全量审计派席 |
| using-arc | 已读 | 用户禁止子代理/第三席，不走 Arc audit 派评审席 |
| graft | 已读并调用 | `graft grep JdbcTimestamps` / `nullableInstant` / `timestampWithTimeZone`；`graft callers` 因符号歧义改 grep。本轮有储蓄行的调用合计约 583,822 tokens |
| long-running-background-tasks | 已读 | Maven 单测前台完成（约 5s），未挂 background / monitor |
| grilling / grill-me | 已读 | grill-me 仅转发 grilling。对抗式自问自答，不问人、不派子代理 |
| batch-grill-me | **缺口** | 本席技能树无独立 `batch-grill-me`；已并入 grilling 自问自答，不冒充调用 |
| ubiquitous-language | **缺口** | 本席加载路径无此技能；§6 已划掉 domain-modeling，不冒充调用 |
| codebase-design | 已读 | 两方法浅实现、高杠杆；本步不改 Interface |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现；B5 禁止修复、不加诊断桩 |
| tdd / test-driven-development | 已读 | 用来判断测试能否因行为变坏而红；本席不写测试 |
| verification-before-completion | 已读 | 先跑 m026_check / javac / jshell+JDBC / BarJdbc 单测再下结论 |
| receiving-code-review | 已读 | 禁止表演性同意；本轮无对方本次 raw 可收 |
| requesting-code-review | 存在，B4 用 | B5 不适用 |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → `MERGE=NO` |
| git-guardrails-claude-code | 可选存在 | 只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B5 不修；review 口径扫可删项，见 §8 |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B5 不适用 |
| i-have-adhd | 存在 | 编号、无寒暄；本文件第一行仍必须是判据锁哈希 |

明确不用（提示词划掉或与双席冲突）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`executing-plans`、`research`、`ponytail-audit`、mattpocock `code-review`、Arc `audit`（会派子代理）。

---

## 1. 模块覆盖（全文实读，行数/SHA 本轮实算）

命令：`wc -l -c` + `sha256sum` + `python3` 计换行 + `git hash-object` + `git cat-file` after tree。

| 项 | 实算 |
|---|---|
| 路径 | `backend/src/main/java/com/complex/platform/common/jdbc/JdbcTimestamps.java` |
| 物理行 / 字节 | **29 行 / 1057 bytes**（29 个 `\n`，文件以换行结尾） |
| 内容 SHA256 | `3ba59fa2c9f69c48d4ed0a95ca39cf9d3f28419f030bf44fb310d293012bf634` |
| git blob（工作区 / after tree） | `722574f2b60df48f0abf4560975d1ca6009eac40`；`cmp` 工作区与 `git cat-file -p` after blob **相同** |
| after tree | `22d70caf574f878827e9d2afbd6bf9c62cb696e1` 内该路径 mode `100644` blob `722574f2…` |
| HEAD blob | `80d2c3689cdd97b9a86a87c661f6d31c9be29398`（B3 前）；`git status --porcelain` 本文件 ` M`；`git diff HEAD` 6 行 ±（与 B3-r1 注释/版本行一致）。B3 不提交，预期如此 |
| 方法体指纹 `sed -n '/^public final class/,$p' \| sha256sum` | `4b9880a6da40f02496b5b9f873bc94386a9f89dc2a5a5109460dad9a3d1c4699`（与 B4 记录的 before/after 方法体指纹相同：方法体未动） |
| 覆盖 | **全文逐行，不抽样、不只看 diff** |

逐行覆盖：

| 行 | 内容 | 审法 |
|---|---|---|
| 1 | `// 文件版本：V0.01` | 读；与定稿最小处置一致，可从当前文件复现 |
| 2–8 | `package` + `ResultSet` / `SQLException` / `Instant` / `OffsetDateTime` / `ZoneOffset` | 读 |
| 10–13 | 类 Javadoc（现句：供 **import 本类** 的 JDBC 仓储/参数源使用的 Instant↔timestamptz 映射） | 读；完成口径核调用面，见 §3.2 / §6 |
| 14–17 | `public final class` + 私有构造 | 读 + javap |
| 19–22 | `timestampWithTimeZone(Instant)`：`null → null`，否则 `instant.atOffset(ZoneOffset.UTC)` | 读 + javap + jshell |
| 24–28 | `nullableInstant(ResultSet, String)`：`getObject(..., OffsetDateTime.class)`，`null → null`，否则 `toInstant()` | 读 + javap + JDBC SELECT |
| 29 | 类结束 `}` | 读 |

相关三 HTML（只读，铁律 4 未改；锁摘录与现行 HTML 逐字对照）：

- `docs/business-system-map-for-operations-v0.1.html` L505 `h2`「整体说明：这套系统到底管什么」（BSM-001）；L563 `h2`「系统通用能力：数据可改可删、操作有引导、权限分岗」（BSM-003，锁摘录仅 h2+lead，子 h3 是其它判据 ID）
- `docs/platform-governance-map-for-operations-v0.1.html` L256 `h2`「整体定位：平台治理层不是一个业务部门，而是所有业务的底座」（PGM-001）
- `docs/business-enum-quick-reference-v0.1.html`：本模块映射无 BEQR ID；本文件无枚举/业务清单面。本席只核其存在与未改（SHA 见下）

三 HTML / 计划文件本席实算 SHA256：

| 文件 | SHA256 |
|---|---|
| `docs/business-system-map-for-operations-v0.1.html` | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| `docs/business-enum-quick-reference-v0.1.html` | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |

调用面与列类型（只读证据，**不把外模块文件纳入本模块 findings 范围**）：见 §2 / §5。外模块同构副本按 B2 双席只登记，无新证据不改判。

`javap -c`（本轮 Temurin 21 重编译后的 class）与源码一致：`timestampWithTimeZone` 走 `ZoneOffset.UTC` + `Instant.atOffset`；`nullableInstant` 走 `ResultSet.getObject(String, OffsetDateTime.class)` + `OffsetDateTime.toInstant`。

---

## 2. 可执行证据（verification-before-completion；本轮当场跑）

### 2.1 驱动与库

| 项 | 证据 |
|---|---|
| Spring Boot parent | `backend/pom.xml` L7–11：`4.1.0` |
| BOM `postgresql.version` | `~/.m2/.../spring-boot-dependencies-4.1.0.pom` L165：`42.7.11` |
| JDBC 实连 `DatabaseMetaData.getDriverVersion()` | **42.7.11** |
| 库产品 | PostgreSQL **16.14** / `complex_test` / 用户 `complex` |
| 口令 | 只从 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD=` 行读取，未 `source`、未回显；JDBC 用 `System.getenv` 直连，不把口令赋给会回显的 REPL 变量 |
| 主机 | `-h 127.0.0.1` |
| OS / JVM 默认区 | `America/Los_Angeles`（PDT, −07:00）；jshell `user.timezone_prop=null`，`ZoneId.systemDefault=America/Los_Angeles`，`offsetNow=-07:00` |
| 纯 `psql` 会话 `TimeZone` | **Asia/Shanghai**（服务器默认） |
| JDBC 连接 `SHOW TimeZone` | **America/Los_Angeles**（pgjdbc 按 JVM 区设置会话） |
| Flyway | 396 条迁移校验通过；schema `public` 版本 **428**；本轮测试未再迁 |

首次 `mvn test` 在 `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`（有 `java` 无 `javac`）下失败：`release version 21 not supported`。改 `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`（javac 21.0.12.1）后编译+测试成功。此为审计机 JDK 布局，不是本文件缺陷。

### 2.2 `/home/james/audit-tmp/m026_check.sh` 只读复跑（类型 4）

脚本自建自删 `m026_javac_out`，复跑后该目录不在。本席未改脚本。

```
completion-claim lines in javadoc: 0 ; equivalent private helpers elsewhere in main: 9
JAVAC_EXIT=0
method bodies sha256 (from 'public final class' to EOF): 4b9880a6da40f02496b5b9f873bc94386a9f89dc2a5a5109460dad9a3d1c4699
first line: // 文件版本：V0.01
problems=0
CHECK_EXIT=0
```

`grep -nE 'every JDBC repository|Centralizes' JdbcTimestamps.java` → 无命中。B1 GR-01 的完成口径本轮不在文件中。

单文件 `javac -encoding UTF-8` 退出 0。

### 2.3 jshell 加载已编译 `JdbcTimestamps`（类型 4）

非 UTC JVM（−07:00）下：

```
loaded=com.complex.platform.common.jdbc.JdbcTimestamps
twtz_nonnull=2026-09-20T12:00Z offset=Z instant=2026-09-20T12:00:00Z
twtz_null=true
plus8_toInstant=2026-09-20T12:00:00Z plus8_bound=2026-09-20T12:00Z plus8_offset=Z
```

要点：`Instant` 无时区；`+08:00` 的 `OffsetDateTime.toInstant()` 再经本方法，绑定值永远是 **offset `Z`（UTC）**，与 JVM 默认区无关。`null Instant` 返回 `null`。

### 2.4 JDBC / psql 只读 SELECT（类型 2+4；无 INSERT）

通过 `JdbcTimestamps.nullableInstant` 与同一 `ResultSet` 的 `getObject(OffsetDateTime.class)` / `getTimestamp()`：

| 输入 | 结果 |
|---|---|
| `SELECT NULL::timestamptz` | `nullableInstant` → `null`；`getObject` 亦 null |
| `SELECT TIMESTAMPTZ '2026-09-20 12:00:00+00'` | `nullableInstant` → `2026-09-20T12:00:00Z`；`getObject` = `2026-09-20T12:00Z`（offset **Z**，不是 −07）；`getTimestamp().toInstant()` 同为 `12:00:00Z` |
| 真列 `audit_events.occurred_at`（information_schema：`timestamp with time zone`） | psql（会话 +08）显示 `2026-09-20 14:08:35.541561+08`；JDBC helper / `getObject` / `getTimestamp().toInstant()` 三者均为 `2026-09-20T06:08:35.541561Z`（14:08+08 = 06:08Z） |
| 真列 `flyway_schema_history.installed_on`（全 `public` **唯一** `timestamp without time zone` 列，Flyway 自用，**无 JdbcTimestamps 调用方**） | helper/`getObject` → `2026-09-20T10:47:14.191735Z`（墙钟当 UTC）；`getTimestamp().toInstant()` → `2026-09-20T17:47:14.191735Z`（墙钟当 JVM 本地 −07，差 7 小时）；`helperEqGetTs=false` |
| `SELECT CAST('2026-09-20' AS date)` | `getObject(OffsetDateTime.class)` 抛 `org.postgresql.util.PSQLException: Cannot convert the column of type DATE to requested type java.time.OffsetDateTime.` |

`information_schema`（psql SELECT）：

- `timestamp without time zone`：**仅** `flyway_schema_history.installed_on` 1 列。
- `bar_order_items.delivered_at`：`timestamp with time zone`，可空。
- `audit_events.occurred_at` / `mdm_policy_violations.occurred_at` / `idempotency_records.created_at|expires_at` / `issued_entitlements.valid_from|valid_to`：均为 `timestamp with time zone`。
- 同名碰撞：`terminal_sync_tasks.occurred_at` 是 `character varying`；合规/供应 `valid_from`/`valid_to` 是 `date`。JdbcTimestamps 读路径抽查对应表为 timestamptz，或仓储根本不走本方法（合规走 `LocalDate`）。

### 2.5 间接仓储测试（类型 2；未改测试）

`bar_order_items.delivered_at` 在 `complex_test` 为 `timestamp with time zone`、可空。

写入：`JdbcBarParameterSource.java:226` `timestampWithTimeZone(item.deliveredAt())`  
读出：`JdbcBarRowMapper.java:237` `nullableInstant(resultSet, "delivered_at")`  
测试：`BarJdbcIntegrationTests.java:390-394` 注释写明改坏列 / mapper / param / INSERT 则本断言红。

```
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
# 仅 export 三个 COMPLEX_DATABASE_* ；口令同行读取，未 source
cd backend
mvn -q -Dtest=BarJdbcIntegrationTests#saveAndListFlowsPersistReservationOrderBillCommissionAndGuestCheckinRows test
```

- exit **0**
- surefire（本轮 mtime 2026-09-20 17:02:04 -0700）：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.345 s`
- 断言：`order_item_jdbc.deliveredAt` **isNull**（null 写入+null 读出）；`order_item_delivered_jdbc.deliveredAt` **isEqualTo(now)`，`now = Instant.parse("2026-06-14T00:00:00Z")`（非 null 往返）

本模块 **没有** 名为 `JdbcTimestamps` 的测试类（`backend/src/test` 0 命中）。关键四分支（写 null / 写非 null / 读 null / 读 timestamptz Instant）由上述仓储测试间接锁住。非 UTC offset 绑定形态由 §2.3/2.4 命令锁住，不靠该单测（timestamptz 瞬时对 +08 绑定也相等，改 offset 字母不会让 Instant 断言红——那不是业务失败，不补需求）。

### 2.6 调用面（类型 4；外模块只作证据）

- `graft grep "JdbcTimestamps"`：49 个 Java 文件 / 84 命中（含本类）→ **48 个生产调用方**。
- 本席 Python 扫描：48 个 import 文件中，**没有**「import 了却零次出现 `timestampWithTimeZone(` / `nullableInstant(`」的文件。合计约 twtz 654 次、ni 356 次（含个别文件本地同名方法的命中，不把那些算进本类调用）。
- 类注释「used by the JDBC repositories and parameter sources that import this class」可从当前仓复现。

---

## 3. 第一性原理三问（2.3）

### 3.1 这段东西为什么必须存在？删掉它，三份 HTML 哪一条做不到？

本文件是 JDBC 侧 `Instant` ↔ SQL `timestamptz` 的共享转换：两行空值语义 + 强制 UTC `OffsetDateTime` 绑定 + `OffsetDateTime.toInstant()` 读回。

三份 HTML **没有**「时间戳转换工具类」条文。BSM-001 要求把分散事情「按同一套规则记录下来」、PGM-001 要求证据可留痕——都**没有**规定 JDBC 必须用 `OffsetDateTime`。HTML 沉默处不反推业务规则。BSM-001 原文另写「技术性设定（与业务政策无关的内部运行参数）不在此列」——UTC 绑定属于技术性设定，不是可配策略参数。

答不上「删掉则某条 HTML 字面做不到」→ **不等于该删**。按 2.1 顺序：与 HTML 无冲突 → 不是某 HTML 条目的下位业务细节 → **技术性**（无业务发明：无实体/枚举/权限/价格/流程）。删除代价是 48 个调用文件回到逐处复制。该类以杠杆存在（codebase-design：浅实现、高杠杆）。

模块标题「租户范围与数据隔离」是 A1 聚合标签，**不是**本文件的行为。本文件不读写租户/项目/商户谓词。

### 3.2 声称的行为有没有被实际执行验证过？

方法体与方法注释（L19–28）**有**，类型 2+4，见 §2。

| 声称 | 原文位置 | 验证 |
|---|---|---|
| 文件版本 V0.01 | L1 | `head -1` = `// 文件版本：V0.01` |
| Instant → UTC OffsetDateTime，供 timestamptz 绑定 | L19–22 | jshell offset=`Z`；即使 JVM −07、源 `+08:00` 仍绑 `Z` |
| `timestampWithTimeZone(null)==null` | L21 | jshell `twtz_null=true`；BarJdbc 未送达明细 `deliveredAt` isNull |
| 读可空 timestamptz 为 Instant | L24–28 | JDBC 字面量与 `audit_events.occurred_at`；BarJdbc `isEqualTo(now)` |
| SQL NULL → null Instant | L27 | JDBC `NULL::timestamptz`；BarJdbc isNull |
| 类 Javadoc：供 import 本类的仓储/参数源使用的 Instant↔timestamptz 映射 | L10–13 | 48 个 import 文件均有调用名；抽查列均为 timestamptz。**不再**声称 every/Centralizes |

B1 类注释「Centralizes…every JDBC repository and parameter source」（当时 L9–12）**本轮文件中不存在**。现句去掉完成口径，GEN-016 可从当前仓复现。不把 B2/B4 的 PASS 当成本步证据——本步是对照现行文件+命令。

### 3.3 依赖的前提，哪一条没验证过就当真？

已验证：

- pgjdbc 42.7.11 对 **timestamptz** 的 `getObject(OffsetDateTime.class)` 在 JVM −07 下返回 offset **Z**，`toInstant()` 与 `getTimestamp().toInstant()` 一致。
- 本席抽查的 JdbcTimestamps **读路径**列在 `complex_test` 均为 `timestamp with time zone`（`bar_order_items.delivered_at`、`audit_events.occurred_at`、`mdm_policy_violations.occurred_at`、`idempotency_records.created_at/expires_at`、`issued_entitlements.valid_from/valid_to`）。
- `DATE` 列走本方法会抛 PSQLException（失败可见，不伪成功）。
- 方法体相对 B3 前未改（指纹 4b9880a6…）。

未当成必须修的前提（铁律 6：要两三件不巧才成立）：

- 调用方把**非 timestamptz** 列名传进 `nullableInstant`（同名 `terminal_sync_tasks.occurred_at` 是 `varchar`，且 **不是** `JdbcMdmRepository` 的 `mdm_policy_violations` 表；合规 `valid_from/valid_to` 是 `date`，`JdbcComplianceRepository` 走 `LocalDate` 而非本方法）。
- Flyway `timestamp without time zone` 若误用本方法：pgjdbc 把墙钟当 UTC，与 `getTimestamp()`（当 JVM 本地）差一个 JVM 偏移。全 `public` 模式仅此列，无本方法调用方。记观察，不立项。

---

## 4. 顺从式：21 项逐项

先假定实现意图正确。每条读锁定义全文（`criteria-lock.md` 定义区），再判本文件有无本面。不新增判据。

| ID | 锁定义要点 | 本面 | 证据 / 理由 |
|---|---|---|---|
| BSM-001 | HTML L505：管什么；先盘子/再动作/后结果；策略可配置；全链可点击；技术性设定不在可配列 | **无本面** | 本文件无综合体/商户/合同/策略参数/UI。模块名不构成 HTML 锚点。不把「按同一套规则记录」扩成必须有本转换类。UTC 绑定属技术性设定 |
| BSM-003 | HTML L563：通用规则 lead（可改可删、引导、分岗的总帽；子条是 BSM-004 起） | **无本面** | 无存档删除、无引导弹窗、无岗位权限 |
| PGM-001 | HTML L256：治理底座；归属/资格/责任；可点击；零英文展示；金额到分；先成功；仲裁序 | **无本面** | 无 UI。类名/`Instant`/`timestamptz` 属代码与系统内部标识，不是「展示给人看的文字」。零英文四例外不在本文件展示面触发。L1 中文版本标记不是界面文案 |
| GEN-001 | 必要性与最小性 | **适用** | 两方法、无私有状态；48 个调用文件。不是为假设场景加的第三方法。满足 |
| GEN-002 | 可执行正确性 | **适用** | 入口可加载（jshell `loaded=` + 单文件 javac 0 + Temurin mvn compile）；主路径与 null 路径见 §2。方法声称与实跑一致。满足 |
| GEN-003 | 测试有效性 | **适用** | 无直接测试类。`BarJdbcIntegrationTests#saveAndListFlows…` 经 `timestampWithTimeZone` 写 + `nullableInstant` 读，null/非 null 均有会失败断言；本席实跑 1/1 绿（4.345s）。满足「直接或经仓储测试间接」 |
| GEN-004 | 信任边界校验 | **适用（弱）** | `columnName` 由调用方字符串字面量传入，`getObject` 按列标签查找、不拼 SQL。DB 值经类型化 `getObject`；DATE 失败抛错。无用户/网络入口。满足此层信任模型 |
| GEN-005 | 授权与最小权限 | **无本面** | 无主体/对象/动作 |
| GEN-006 | 数据完整性 | **无本面** | 无事务/约束/金额 |
| GEN-007 | 并发与幂等 | **无本面** | 纯函数，无共享可变状态；重复调用幂等 |
| GEN-008 | 接口与模式一致性 | **适用** | Java `Instant` ↔ PG `timestamptz` ↔ pgjdbc `OffsetDateTime` 与驱动 42.7.11 实跑一致。抽查调用列均为 timestamptz。满足本文件契约 |
| GEN-009 | 错误处理与恢复 | **适用** | SQL NULL 不变成 `EPOCH`；DATE 抛 PSQLException，不吞错。满足 |
| GEN-010 | 安全与敏感信息 | **无本面** | 无秘密、无日志、无路径、无加密。列名不是字符串拼接 SQL |
| GEN-011 | 可观测与审计证据 | **无本面** | 不写日志/审计行；调用方仓储另论 |
| GEN-012 | 资源与性能边界 | **无本面** | 无循环/连接/缓存；不持有 ResultSet 生命周期 |
| GEN-013 | 构建配置与依赖 | **适用** | 无自身版本声明，跟随 Boot BOM `postgresql.version=42.7.11`；实连驱动号相同。单文件 javac 不依赖 Maven。满足 |
| GEN-014 | 数据库迁移正确性 | **无本面** | 无 SQL / V 号 |
| GEN-015 | 界面可用性 | **无本面** | 无 UI |
| GEN-016 | 文档可验证性 | **适用** | L1 版本标记可复现。L10–13 现句不再含 every/Centralizes 完成口径，且「import 本类的仓储/参数源」可从 48 个 import 文件复现。L19/L24 方法注释与 §2 实跑一致。B1 GR-01 已不成立。满足 |
| GEN-017 | 业务来源分类 | **适用** | 本类按冲突→细化→技术性→添加判为 **技术性**（无业务发明）。本轮无新条目需改分类 |
| GEN-018 | 第一性原理证据 | **适用** | 本报告结论落到 HTML 原文 / JDBC 与单测产物 / 命令输出；不以注释、B2/B4 票或席位意见当依据 |

---

## 5. 对抗式（grilling 结构，自问自答）

事实题全部本席自读，不等操作员、不派子代理。先假定实现错误。

❓ **Q1 — 非 UTC JVM / 非 UTC JDBC 会话会不会把 Instant 写成错的墙钟？**  
➡️ 不会（对本方法）。jshell 在 `offsetNow=-07:00`、JDBC `SHOW TimeZone=America/Los_Angeles` 下 `timestampWithTimeZone` 仍产出 `offset=Z`。psql 会话是 `Asia/Shanghai`，对 timestamptz 的 Instant 仍与 JDBC 一致（`audit_events` 14:08+08 = 06:08Z）。BarJdbc 用 `Instant.parse("2026-06-14T00:00:00Z")` 往返相等。

❓ **Q2 — 源时间带 `+08:00` 再经 Instant 会不会偏 8 小时？**  
➡️ `OffsetDateTime.parse("2026-09-20T20:00:00+08:00").toInstant()` = `12:00Z`，再 `timestampWithTimeZone` 仍是 `12:00Z`。方法入参是 `Instant`，没有「非 UTC Instant」。

❓ **Q3 — SQL NULL 会不会变成 epoch 或 NPE？**  
➡️ 两方法都有 `== null ? null`。JDBC `NULL::timestamptz` 与 BarJdbc 未送达明细 isNull。`javap` 确认 `ifnonnull` 分支。

❓ **Q4 — 不带时区列会怎样？**  
➡️ pgjdbc 42.7.11：`timestamp without time zone` 的 `getObject(OffsetDateTime.class)` / 本 helper 把墙钟当 UTC；`getTimestamp().toInstant()` 把墙钟当 JVM 本地，本机差 7 小时（10:47Z vs 17:47Z）。全库该类型仅 `flyway_schema_history.installed_on`，无本方法调用方。DATE 则直接 PSQLException。不为理论误用补校验（铁律 6）。

❓ **Q5 — 同名列不是 timestamptz 会不会悄悄读错？**  
➡️ `information_schema`：`terminal_sync_tasks.occurred_at` 是 `varchar`；`JdbcMdmRepository` 的 `occurred_at` 来自 `mdm_policy_violations`（timestamptz）+ `nullableInstant`。合规 `valid_from/valid_to` 是 `date`，仓储用 `LocalDate`。要「调错表 + 同名列」才撞上，不立项。

❓ **Q6 — 类注释是否仍把未完成收口写成当前事实？（GR-01 回归）**  
➡️ 否。现行 L10–13：`Shared JDBC timestamp conversions: the Instant <-> SQL timestamptz mapping used by the JDBC repositories and parameter sources that import this class.` 无 Centralizes / every。`m026_check.sh` completion-claim=0。48 个 import 文件均有调用名。外模块同构副本仍在（私有 helper 9 处等）——B2 已定为只登记，本轮无新证据不改判，也不把「仓内仍有副本」读成现句的完成声称（现句没有说已收口）。

❓ **Q7 — 有没有测试在本方法被改成恒返回 EPOCH / 去掉 null 分支时仍绿？**  
➡️ `nullableInstant` 恒 EPOCH：BarJdbc `deliveredAt isNull` 会红。去掉 `timestampWithTimeZone` 的 null 分支：未送达明细 `timestampWithTimeZone(null)` NPE，同测试会红。`isEqualTo(now)` 锁非 null 往返。本席本轮实跑该测试绿。GEN-003 不另立项。没有测试会因为「绑定 offset 从 Z 改成 +08」而红——不补需求。

❓ **Q8 — 并发改 ResultSet / 重复绑定？**  
➡️ 无内部状态。并发问题在调用方事务，不在本类。

❓ **Q9 — 能否整类删除（ponytail / GEN-001）？**  
➡️ 能编译删除，但 48 个调用文件要各自写回两行。HTML 不要求这个类；技术性保留。剩余副本在**外模块**，B2 只登记，本模块 B5 不替它们立项。

❓ **Q10 — 金额、中文 UI、越权、租户隔离有没有绕过？**  
➡️ 本文件无这些面。对抗式不在无本面发明漏洞。

❓ **Q11 — 方法体有没有在「只改注释」时被顺手改掉？**  
➡️ 方法体指纹 `4b9880a6…` 与 B4 记录的 before/after 相同。javap 仍是 UTC + getObject OffsetDateTime。无回归。

❓ **Q12 — 现句「that import this class」会不会变成新的不可复现完成声称？**  
➡️ 「used by … that import」是限制关系从句，不是「所有仓储已收口」。48 个 import 文件可列出；无 import 却零调用的文件。RowMapper / `ProductionAuthService` 也 import，现句并未写「仅仓储与参数源、别无调用方」——不把未写出的全称读成完成口径。

---

## 6. Findings

本轮 **无新的临时 finding**。

### M026-001 回归核（已定稿、队列否；本步只核是否仍成立或回潮）

- **原句（B1/B2，已不在现行文件）**：`Centralizes the Instant <-> SQL timestamptz mapping that was previously copy-pasted verbatim across every JDBC repository and parameter source.`
- **现行 L10–13**：`Shared JDBC timestamp conversions: the Instant <-> SQL timestamptz mapping used by the JDBC repositories and parameter sources that import this class.`
- **L1**：`// 文件版本：V0.01`
- **方法体**：未改（指纹 4b9880a6…）
- **正常触发对照**：`grep -nE 'every JDBC repository|Centralizes'` 空；`m026_check.sh` problems=0
- **结论**：B3-r1 最小处置已落地；完成口径未回潮；未新增「还有哪些没收口」类会再过时的声称。本步不把 001 重新入队。

无添加项。无 HTML 修改申请。无 questions。`docs/audit-2026-09/questions/M026*` 不存在。

四分类（对本文件整体，固定顺序）：冲突？否。细化？否（不能指到某一 HTML 锚点作为「必须有此类」的上位）。**技术性**（工具类，无业务发明）。不是添加。

---

## 7. 观察（不立项；无新证据，不改 B2 只登记）

- **OBS-1** 调用面：49 个 Java 文件出现 `JdbcTimestamps`（含自身），即 **48 个生产调用方**。测试树 0 命中类名。
- **OBS-2** 已 import 仍走旁路（外模块，B2 只登记）：例如 `JdbcIdempotencyRepository` 写用 `timestampWithTimeZone`、读用 `getTimestamp().toInstant()`（L118–119）；`JdbcProductionReadinessRepository` 写用本类、读用本地 `getTimestamp` 版 `nullableInstant`（L197–200）；`JdbcTicketingRepository` 读用本类、写用本地 `off`（L1720）；`JdbcAmusementSafetyRepository` / `JdbcAnalyticsRepository` / `JdbcMerchantServiceRepository` 写用本类、读用本地 `instant`/`nullableInstant`。本轮仍在，无新形态。
- **OBS-3** pgjdbc 42.7.11 对 `timestamp without time zone`：`getObject(OffsetDateTime)` 与 `getTimestamp().toInstant()` 在 JVM −07 下差 7 小时。仅 Flyway 列，无本方法调用方。本轮 JDBC 复测相同。
- **OBS-4** 列名碰撞：`occurred_at` / `valid_from` / `valid_to` 在库内既有 timestamptz 也有 varchar/date。本席核过 JdbcTimestamps 读路径对应表为 timestamptz 或根本不走本方法。
- **OBS-5** 工作区 HEAD 落后本文件 after blob（B3 未提交）。本文件与 after tree `22d70caf…` / blob `722574f2…` 一致。
- **OBS-6** 纯 `psql` 会话 `TimeZone=Asia/Shanghai`，JDBC 会话 `America/Los_Angeles`。对 timestamptz 的 Instant 仍一致。
- **OBS-7** 仓内同构私有 helper 仍约 9 处（m026_check 计数）；`atOffset(ZoneOffset.UTC)` 另 4 文件、`getObject(..., OffsetDateTime.class)` 另 6 文件（均不含本文件）。B2 定为只登记，本轮无新证据不改判。

---

## 8. ponytail-review（只列可删，不改码）

本文件：`Lean already. Ship.`  
两方法、私有构造、无第三抽象。净可删行在**外模块**的同构副本，不在 M026 FILES 闭集。

`net: -0`（本文件）。

---

## 9. 席位结论

| 门 | 状态 |
|---|---|
| 1 文件全文覆盖 | 是（29 行 / 1057 bytes / SHA `3ba59fa2…` / blob `722574f2…` = after tree） |
| 21 项逐项核对 | 是（适用或无本面均已记） |
| 三线 | 是（第一性三问 + 顺从 21 + grilling Q1–Q12） |
| 声称四类证据 | 方法体：是（类型 2+4）；类注释现句：是（类型 4，可复现）；L1 版本：是 |
| 未答 questions | 无 |
| 未定稿缺陷 | **无** |
| M026-001 | 已修且无回归；不重新入队 |
| 哈希行 | 本报告第一行独立实算 |

**PASS**
