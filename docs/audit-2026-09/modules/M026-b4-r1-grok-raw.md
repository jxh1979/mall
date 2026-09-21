CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M026 B4-r1 Grok 席乙只读审计（修复 diff）
版本：V0.01
席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
步骤：B4-r1（160-audit-prompt.md V0.17 §4 B4 + §5 闸 3）
正本：`/home/james/complex-management` main 主工作区
对象：席甲 B3-r1 整批修复 diff（后台实现模块；M026 FILES 闭集 1 文件）
findings 正本：`docs/audit-2026-09/findings.md` V2.84，已定稿且归属「是」：仅 M026-001
独立性：本席未读 `M026-b4-r1-self-raw.md`（该路径存在，本会话未打开）；未派 MCP / 子代理 / 第三席；未问人；未改业务 / HTML / 锁 / findings / ledger；未 fetch / commit / push / merge / deploy；未建脚本 / 日志 / 临时产物。旧 B1/B2 可对照（`M026-b1-grok-raw.md` V0.02 B2-r1 段、`M026-b1-self-raw.md` V0.02、findings V2.84）；本轮以 V2.84 定稿最小处置为准。检查脚本 `/home/james/audit-tmp/m026_check.sh` 只读复跑，其 `m026_javac_out` 由脚本自建自删，复跑后目录不在。

---

## 0. 技能预检（§6，按本环境实核）

核到且按收窄口径使用：
1. `using-superpowers`：开工技能自检。用户指令优先：本步只读、两席、不问人。
2. `using-arc`：已读；其默认派评审子代理与双席铁律冲突，本席不走 Arc `audit` 流程。
3. `graft`：`graft grep "JdbcTimestamps" --in backend/src/main/java` → 49 文件 / 84 命中（含本类）。证明「that import this class」可从当前仓复现，且外模块调用面未被本 diff 改写。
4. `grilling` / `grill-me`：对抗式自问自答；本体「等用户 / 派子代理」作废。`grill-me` 只转发 grilling。
5. `ponytail-review`：只找可删的多余抽象；不改码。
6. `verification-before-completion`：PASS 前当场复跑 `m026_check.sh`、两树 `git diff --binary` 与冻结 diff `cmp`、方法体指纹、去 `{@code}` 后与 B2 定稿句比对。
7. `systematic-debugging` / `diagnosing-bugs`：只借读错误→复现→追数据流。红环＝对 before blob 数 `every JDBC repository|Centralizes`（2）且 copies>0；绿环＝同一检查脚本对 after。本体 instrumentation / 最小改码试假说作废。本步不修。
8. `tdd` / `test-driven-development`：本 diff 是类注释一句 + 文首版本行；160 §6 文档类不强制落测试文件。加测试类会改他文件，闸 3 越界。类型 4 用既有 `m026_check.sh` + 单文件 javac。
9. `requesting-code-review`：B4＝本席即外审；不用 `HEAD~1`、不用子代理复审。
10. `codebase-design`：本类接口仍是两个静态方法；本 diff 不改 Interface。迁外模块副本会跨 FILES 闭集，属加深 locality 的别模块事，定稿禁止。
11. `writing-for-agents`：本 raw 给两席落齐后的下游席读。
12. `long-running-background-tasks`：已读；本步无长任务、无 monitor。
13. `i-have-adhd`：存在（`disable-model-invocation: true`），未开持久模式。编号、无寒暄；本文件第一行仍必须是判据锁哈希（提示词覆盖输出模式）。

明确不用（提示词划掉或本步不适用）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`executing-plans`、`research`、`ponytail-audit`、mattpocock `code-review`。`brainstorming` / `writing-plans` / `ponytail` 本体属 A/B3，本步只读不修。`receiving-code-review` 属席甲收票。`resolving-merge-conflicts` 无合并。`git-guardrails-claude-code` 存在但本体要问人装 hook（第四闸），本席只读、无 git 写，不用。

缺口（实找无目录，不冒充已调用）：`batch-grill-me`、`ubiquitous-language`。

---

## 1. 输入身份（类型 4）

本席独立实算（未从 sidecar / ledger / 派发词抄写）：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
→ b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```
sidecar 与 ledger 顶部同值，仅作旁证，不以它们代替实算。

冻结 diff：
```
wc -c /home/james/audit-tmp/m026_b3_r1.diff → 985
sha256sum → 9c8a415f67788af4b62f7de8e899cdf3a5f1bcfedc25ccc99576196425a92c47
```
与派发声明一致。

tree（`git cat-file -t` 均为 tree）：
- before `122926c48d76e39783614d9ca0ec33e3e4ffb893`
- after  `22d70caf574f878827e9d2afbd6bf9c62cb696e1`

```
git diff --binary <before> <after> | sha256sum
→ 9c8a415f67788af4b62f7de8e899cdf3a5f1bcfedc25ccc99576196425a92c47
git diff --binary <before> <after> | cmp - /home/james/audit-tmp/m026_b3_r1.diff → CMP_OK
git diff --stat：1 file changed, 3 insertions(+), 3 deletions(-)
git diff --name-status：仅 M backend/src/main/java/com/complex/platform/common/jdbc/JdbcTimestamps.java
hunks=2（冻结 diff 2 个 @@）
```

blob / mode：
- before `80d2c3689cdd97b9a86a87c661f6d31c9be29398` mode `100644` 内容 SHA256 `369ffc5be326f26334ee3291c67e1231e8fda509f455a17650119ef28189ac6a`（29 行 / 1064 bytes；与本席 B1 grok-raw 对象 SHA 一致）
- after  `722574f2b60df48f0abf4560975d1ca6009eac40` mode `100644` 内容 SHA256 `3ba59fa2c9f69c48d4ed0a95ca39cf9d3f28419f030bf44fb310d293012bf634`（29 行 / 1057 bytes）
- 工作区 `git hash-object` = after blob `722574f2…`；`cmp` 工作区与 after blob 相同。两树 mode 均为 `100644`。分支 `main`，`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`。

方法体指纹（`sed -n '/^public final class/,$p'`，含私有构造与两方法及其方法级 Javadoc）：
- before / after / 工作区均为 `4b9880a6da40f02496b5b9f873bc94386a9f89dc2a5a5109460dad9a3d1c4699`
- `diff -u` 两树方法体：空（`METHOD_BODIES_IDENTICAL`）

闸 3：diff＝两 tree 之间全部差异；每一 `+`/`-` 行须归到 V2.84 归属「是」的 ID（仅 M026-001）；归不到即越界。

模块闭集：`plan/modules.md` M026 FILES 仅 `backend/src/main/java/com/complex/platform/common/jdbc/JdbcTimestamps.java`。`git diff --name-only` 闭集＝该 1 文件。

---

## 2. 定稿最小处置（审计标尺，摘 V2.84）

| ID | 分类 | 修复队列 | 最小处置 |
|---|---|---|---|
| M026-001 | 技术性 | 是 | 只改本文件类注释这一句，去掉「every JDBC repository and parameter source」的完成口径，改为「Shared JDBC timestamp conversions: the Instant <-> SQL timestamptz mapping used by the JDBC repositories and parameter sources that import this class.」（不新增「还有哪些没收口」之类会再过时的声称）；文件首行加 `// 文件版本：V0.01`。两个方法体不动；不在本模块迁移别的模块里的副本 |

B2 双席已同意：本席 B1 grok-raw V0.02「拟改英文」即上表英文句；「B3 可保留 `{@code Instant}` / `{@code timestamptz}` 既有 Javadoc 标记，不改变句意」；「文件首行加」＝文首插入该标记、不得覆盖 `package com.complex.platform.common.jdbc;`。无第三 ID。不是无 ID 新缺陷。

只登记不入队（不得混进本 diff）：外模块同构/旁路副本本体；`JdbcIdempotencyRepository` 读路径旁路；pgjdbc `timestamp without time zone` 读法差；列名碰撞；无直接单测；英文类注释非界面文字；JDBC 与 psql 会话时区差。本 diff 未碰这些文件。

---

## 3. diff 全文（冻结件 985 bytes，与 `git diff --binary` 字节级相同）

```
diff --git a/backend/src/main/java/com/complex/platform/common/jdbc/JdbcTimestamps.java b/backend/src/main/java/com/complex/platform/common/jdbc/JdbcTimestamps.java
index 80d2c368..722574f2 100644
--- a/backend/src/main/java/com/complex/platform/common/jdbc/JdbcTimestamps.java
+++ b/backend/src/main/java/com/complex/platform/common/jdbc/JdbcTimestamps.java
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 package com.complex.platform.common.jdbc;
 
 import java.sql.ResultSet;
@@ -7,9 +8,8 @@ import java.time.OffsetDateTime;
 import java.time.ZoneOffset;
 
 /**
- * Shared JDBC timestamp conversions. Centralizes the {@code Instant} <-> SQL
- * {@code timestamptz} mapping that was previously copy-pasted verbatim across
- * every JDBC repository and parameter source.
+ * Shared JDBC timestamp conversions: the {@code Instant} <-> SQL {@code timestamptz}
+ * mapping used by the JDBC repositories and parameter sources that import this class.
  */
 public final class JdbcTimestamps {
```

index 行两侧均为 `100644`：普通源文件 mode 保留，无 mode hunk。

---

## 4. diff 逐块归因（闸 3）

`+`/`-` 行闭集＝3 插入 + 3 删除。上下文行无 `+`/`-`，不入归因。

### 4.1 H01 `@@ -1,3 +1,4`

| 行 | 内容 | ID |
|---|---|---|
| `+// 文件版本：V0.01` | 文件第一行插入版本标记；未覆盖原 L1 `package`（原 L1 现为 after L2，逐字 `package com.complex.platform.common.jdbc;`） | M026-001 |

上下文 `package` / 空行 / `import java.sql.ResultSet;` 无 `+`/`-`。

### 4.2 H02 `@@ -7,9 +8,8`

| 行 | 内容 | ID |
|---|---|---|
| `- * Shared JDBC timestamp conversions. Centralizes the {@code Instant} <-> SQL` | 删现在时 Centralizes 完成口径 | M026-001 |
| `- * {@code timestamptz} mapping that was previously copy-pasted verbatim across` | 删「previously copy-pasted verbatim across」 | M026-001 |
| `- * every JDBC repository and parameter source.` | 删「every JDBC repository and parameter source」 | M026-001 |
| `+ * Shared JDBC timestamp conversions: the {@code Instant} <-> SQL {@code timestamptz}` | 定稿英文句上半；保留既有 `{@code}` | M026-001 |
| `+ * mapping used by the JDBC repositories and parameter sources that import this class.` | 定稿英文句下半；限制从句 `that import this class` | M026-001 |

上下文 `import java.time.ZoneOffset;`、` */`、`public final class JdbcTimestamps {` 无 `+`/`-`。H02 截断于类声明行，两方法体不在变更集。

### 4.3 归因结论

3 插入、3 删除均可落到唯一归属「是」的 ID M026-001（版本行归 001，V2.84 最小处置明文「文件首行加」；B2-r1 本席已同意插入、不得覆盖 `package`）。  
未见「顺手优化 / 迁外模块副本 / 改方法体 / 改绑定 offset / 加测试类 / 写尚未收口清单 / 改其他文件 / 改 mode」。  
`git diff --name-only` 闭集＝1 文件＝M026 FILES。  
before→after：除插入 L1 与类 Javadoc 两行替换外，其余行映射后逐字相同（方法体 `diff` 空）。  
**归因票：PASS。**

---

## 5. 内容审计

### 5.1 三线（本步范围＝该 diff，不是整模块重审）

**第一性原理**
1. 删掉这些改写，V2.84 唯一「是」仍红：类注释主句 Centralizes + every 完成声称不能从当前仓复现为已收口。必须存在。
2. 声称的现行事实：下列命令/源码已核（类型 4）。
3. 未验证前提：无。本席未改生产代码、未迁副本、未跑 Maven 全量测试；可执行正确性用单文件 javac；完成口径用 grep；方法体用指纹。

**顺从式**  
假定 B3 意图正确，逐 ID 对照最小处置原文与 after 树对应句，见 5.3。

**对抗式（grilling 自答）**
- Q：版本行无独立 ID，是否越界？→ 否。V2.84 001 最小处置明文「文件首行加 `// 文件版本：V0.01`」。after L1 恰为该行；after L2＝before L1 `package`，逐字未覆盖。
- Q：M020 Java 是 `package` 下一行才写版本，本 diff 写在 package **之前**，是否未按 B2 叙述？→ 定稿表与派发词均为「文件首行」。本席 B2 把「文件首行加」解为文首插入、不得覆盖 package——当前 L1 版本、L2 package，满足该解。B2 另提 M020「package 下一行」是既有样例，不是与「文件首行」相反的第二硬条件。Java 允许 package 前注释；当场 javac 0。不构成越界或未落地。
- Q：`{@code}` 是否改了定稿措辞？→ 去标记后 after 类 Javadoc 与 B2 定稿句逐字相同。B2 明文「可保留 `{@code}`」。
- Q：新句是否仍含不可复现完成口径？→ after 对 `every JDBC` / `Centralizes` / `previously copy-pasted` / `尚未` / `没收口` / `全部收口` 均为 0。留下的是「used by … that import this class」，外延＝import 本类者；graft 49 文件可复现。
- Q：是否新增会再过时的声称（剩余副本清单、「尚未迁完」）？→ after 类 Javadoc 无文件名、无「尚未」、无计数。
- Q：方法体是否被 hunk 上下文牵动？→ `public final class` 至 EOF 两树 SHA 相同；`timestampWithTimeZone` / `nullableInstant` / 私有构造及方法级 Javadoc 均在指纹内。
- Q：是否迁移了别的模块副本？→ 同构私有助手仍在：`nullableInstant`/`instant` 7 处 + `offset`/`off` 2 处＝9（与 red/green 日志 copies=9 一致）。`git diff --name-only` 无那些路径。
- Q：是否改了三 HTML / 锁 / findings / ledger / 他文件？→ 两树 diff 无名。
- Q：是否业务发明？→ 只改工具类注释范围描述与版本行；转换语义、调用方、绑定 offset、`getObject` 路径均未动。V0.17 技术性分类仍成立。
- Q：加测试文件或改 `modules.md` 行数？→ 未改。加他文件即越界。

### 5.2 改后注释 vs GEN-016

去 `{@code}` 折叠空白后：

- before：`Shared JDBC timestamp conversions. Centralizes the Instant <-> SQL timestamptz mapping that was previously copy-pasted verbatim across every JDBC repository and parameter source.`
- after：`Shared JDBC timestamp conversions: the Instant <-> SQL timestamptz mapping used by the JDBC repositories and parameter sources that import this class.`
- after == B2 定稿拟句：True

「that import this class」可用 `graft grep JdbcTimestamps` 从当前纳入范围复现（49 生产 Java 文件），不是「every JDBC repository」式全称收口。未把计划或缺口清单写成当前事实。GEN-016 就本句成立。

### 5.3 按 ID 核最小处置

**001** after L1 `// 文件版本：V0.01`；after L11–12 类注释为定稿英文句（保留 `{@code}`）；去掉 Centralizes / every / previously copy-pasted；两方法体指纹不变；外模块 9 处同构私有助手仍在。通过。

### 5.4 禁区

- 三份需求 HTML：两树 diff 无名。
- 其他文件：`git diff --name-only` 仅本 Java。
- 方法体：指纹 EQ；unified diff 空。
- 外模块副本：未迁。
- 新完成/缺口声称：0。
- mode：`100644`→`100644`。
- 单文件 javac：当场 0。

### 5.5 类型 4 当场复跑（verification-before-completion）

修前（冻结 red 日志 + 本席对 before blob 独立计数；未改 before 树）：
```
/home/james/audit-tmp/m026_b3r1_red.log
completion-claim lines in javadoc: 2 ; equivalent private helpers elsewhere in main: 9
RED: 类注释仍称已在所有仓储收口，而仓内尚有 9 处同构私有副本
JAVAC_EXIT=0
method bodies sha256 … 4b9880a6da40f02496b5b9f873bc94386a9f89dc2a5a5109460dad9a3d1c4699
first line: package com.complex.platform.common.jdbc;
problems=1
```
本席独立：before blob `every JDBC repository|Centralizes` 命中 2；first line=`package …`。与 red 日志同因。

修后本席当场：
```
bash /home/james/audit-tmp/m026_check.sh
→ completion-claim lines in javadoc: 0 ; equivalent private helpers elsewhere in main: 9
JAVAC_EXIT=0
method bodies sha256 … 4b9880a6da40f02496b5b9f873bc94386a9f89dc2a5a5109460dad9a3d1c4699
first line: // 文件版本：V0.01
problems=0
CHECK_EXIT=0
```
与 `/home/james/audit-tmp/m026_b3r1_green.log` 同结论。当场 stdout 多一行 `Picked up JAVA_TOOL_OPTIONS`（环境），不改变 exit 0 / javac 0。脚本用完删除 `m026_javac_out`，复跑后该目录不在。

执行日志 `/home/james/audit-tmp/m026_b3r1_run.log`：BEFORE/AFTER tree、diff sha `9c8a415f…`、1 file 3+/3-、hunks=2、after 内容 SHA `3ba59fa2…`、方法体指纹、`other files touched: 0` 与本席实算一致。本席不把该日志当唯一证据，只作交叉。

证据文件 SHA256（只读核身份，非判据）：
- red `759d84c57b8b842a1daaee9f6bc6056e49716d6ef1d78d168c13b68a44d8ead9`
- green `f73c90a79b5c46752c94b71b3e70febf63ed3847edd73805b6d67dde17a211e7`
- run `74e0c5d14854639ab4683ffabc87af94508541fea945a7015ab7e0b89e8da748`
- check.sh `4f1cc288221cce8d12a9ec9d1a7fe780cf4fe09286b6139c2ab68506981182b1`

**内容票：PASS。**

---

## 6. ponytail-review（只读）

无额外可删抽象。版本行是 001 指定插入，不是膨胀。类 Javadoc 由 3 行收成 2 行，去掉完成口径。方法体零改。外模块副本按定稿不迁。  
`net: -0 lines possible.`（相对定稿最小集；注释本身已缩短）  
Lean already. Ship.

---

## 7. 最终票

| 轴 | 票 | 依据 |
|---|---|---|
| 归因（闸 3） | PASS | 3+/3- 均可归 M026-001；版本行归 001；无无 ID 行；闭集 1 文件 |
| 内容 | PASS | 定稿最小处置落地；去 `{@code}` 后与 B2 拟句逐字相同；无新完成口径/过时声称；方法体指纹修前修后一致；副本未迁；`100644` 保留；单文件 javac 0；检查脚本红→绿 |
| 本席 B4-r1 | PASS | 两轴均 PASS；任一 FAIL 即 FAIL，此处未触发 |

本票不是模块 PASS，不是授权改归属。B4 双 PASS 仍须席甲本次完整 raw 合取。本席未读 self-raw。

步骤：B4-r1。模块 M026。B3 次数＝本轮审的 r1。未写 findings/ledger。
