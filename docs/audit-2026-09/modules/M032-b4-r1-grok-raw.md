CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M032 B4-r1 Grok 席乙只读审计（修复 diff）
版本：V0.01
席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
步骤：B4-r1（160-audit-prompt.md V0.17 §4 B4 + §5 闸 3）
进度：模块 M032 / 共 471，B3 第 1 次
正本：`/home/james/complex-management` main 主工作区
对象：席甲 B3-r1 整批修复 diff（功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第2段）
findings 正本：`docs/audit-2026-09/findings.md` V3.01，已定稿且归属「是」：M032-001、M032-002（W-1…W-4 只登记到未发布工作流，不得出现在本 diff）
独立性：本席未读 `M032-b4-r1-self-raw.md`（`ls` 见该路径存在、23329 bytes、mtime Sep 20 23:21，本会话未打开正文）；未派 MCP / 子代理 / 第三席；未问人；未改业务 / HTML / 锁 / findings / ledger；未 fetch / commit / push / merge / deploy；未建脚本 / 持久日志 / 审计临时产物。一次性 `git worktree add --detach /home/james/wt-m032-b4-grok-ro origin/main` 拷入 after 树四文件与 Jdbc 发布变体及主工作区未发布 V427（仅过 Flyway 校验），实跑 GREEN 与 RED 后已 `git worktree remove --force`，目录已不存在。旧 B1/B2 可对照（`M032-b1-grok-raw.md` V0.01+V0.02）；本轮以 V3.01 定稿最小处置与本席 B2-r1 实施边界为准。不对任何数据库执行写语句（仅跑既有测试类）；未 source `/etc/complex/complex.env`。

---

## 0. 技能预检（§6，按本环境实核）

核到且按收窄口径使用：
1. `using-superpowers`：已读开工路由段。用户指令优先：本步只读、两席、不问人。
2. `using-arc`：已读开头；其默认派评审子代理与双席铁律冲突，本席不走 Arc `audit` 流程、不写 `docs/arc/audits/`。
3. `graft`：已读并调用。`graft grep "saveGrantPolicy"`；`graft grep "AccessGrantPolicy"`；`graft grep "new EffectivePermissions"`；`graft callers saveGrantPolicy --direction in --depth 2`；`graft callers AccessGrantPolicy --direction in --depth 1`；`graft callers AuthorizationService.effectivePermissions --direction out --depth 1`。tokens saved ≈ **52,509 + 34,932 + 9,218 + 81,076 + 16,768 + 5,569 = 200,072**。证明：生产 `saveGrantPolicy` 入边仍是已闭环 `AccessService.updateGrantPolicyAudited` + M006Jdbc 回归 + 本批新真库条；全后端 `new EffectivePermissions(` 生产点仍仅 `AuthorizationService.computeEffectivePermissions:206` 第四参恒 `false`。
4. `grilling`：已读开头；对抗线自问自答；本体「等用户 / 派子代理」作废。结论见 §6。
5. `ponytail-review`：已读；只找可删的多余抽象；不改码。结论见 §5.4。
6. `verification-before-completion`：已读。PASS 前当场：两树 `git diff --binary` 与冻结 diff `cmp`、blob SHA256、mode、独立 worktree GREEN / RED、worktree 删除。
7. `requesting-code-review`：已读。B4＝本席即外审；不用 `HEAD~1`、不用子代理复审。
8. `long-running-background-tasks`：已读；本步 sha256 / maven / worktree 均前台完成（maven 曾被宿主 15s 自动转后台，本席等到进程结束读完整输出），未挂 monitor。
9. `codebase-design`：已核。本 diff 不改 `AccessRepository.saveGrantPolicy` 接口签名，只在既有 Jdbc 适配器体内把整份覆盖改成 jsonb `||`；记录类第 5 组件是把库内已有键纳入既有缝，不是新开接口。
10. `diagnosing-bugs` / `systematic-debugging`：已核存在。收窄为「RED 命令 → 本席 GREEN / RED」；不加诊断桩、不修码。
11. `test-driven-development` / `tdd`：本步只核席甲是否先测后改。席甲首跑日志：只打测试补丁时真库回归 1 条 Unrecognized field + AccessServiceTests 11 条政策 409；本席独立 RED 同 12 条，GREEN 六类全绿。002 只改注释，不强制落测试。
12. `writing-for-agents`：本 raw 给下游席甲归并用；结论与证据共置。
13. `resolving-merge-conflicts`：`test -f .git/MERGE_HEAD` → NO_MERGE。

明确不用（提示词划掉或本步不适用；本席未另开这些 SKILL.md 正文当流程跑完）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`（一次性 detach 是 D-029 允许的只读核，不是该技能本体）、`executing-plans`、`research`、`ponytail-audit`、mattpocock `code-review`。`brainstorming` / `writing-plans` / `ponytail` 本体属 A/B3，本步只读不修。`receiving-code-review` 属席甲收票。`audit-context-building` 要求派 function-analyzer 子代理，与「只允许两席」冲突，不执行。

缺口（实找无独立可调用技能目录，不冒充已调用）：`batch-grill-me`、`ubiquitous-language`。`git-guardrails-claude-code` 有、本步无 git 写。`i-have-adhd` 在环境技能列表中；本文件第一行仍必须是判据锁哈希（A2 交叉报数优先于该技能「首行即下一步」），CLI 不回结论。

---

## 1. 输入身份（类型 4）

本席独立实算（未从 sidecar / ledger / 派发词抄写）：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
→ b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```
sidecar 全文一行与 ledger 顶行 `CRITERIA-LOCK-SHA256:` 与本席实算同值，仅作旁证，不以它们代替实算。`criteria-lock.md` 文首版本 V0.11；`LOCK-AMEND-COUNT: 1` 只读见到，未改。findings 文首「版本：V3.01」；`sha256sum docs/audit-2026-09/findings.md` → `4c979742d8a1178514552a37779a470c40f189a4be48866fc9b52c3be41ac794`。本席 B2 票所在 `M032-b1-grok-raw.md` 全文 SHA256 `6f37ca0e6dcc71601f7f00dd3814b1a96b776c98122e16ea8050920e158e10dd`。

冻结 diff：
```
wc -c /home/james/audit-tmp/m032_b3r1.diff → 9201
sha256sum → e5102bd28a6dad49cb599ce73706a4a99443d9ddc55d3a7559dbdc82d521d052
```
与派发声明一致。

tree（`git cat-file -t` 均为 tree）：
- before `03a43dc2d1ca63b174779b5284ed195877df5329`
- after  `2418fd96bd71264a3e84bd137cd8d183e9d976d0`

```
git diff --binary <before> <after> | sha256sum
→ e5102bd28a6dad49cb599ce73706a4a99443d9ddc55d3a7559dbdc82d521d052
git diff --binary <before> <after> | cmp - /home/james/audit-tmp/m032_b3r1.diff → CMP_OK
git diff --stat：5 files changed, 40 insertions(+), 9 deletions(-)
git diff --name-status：
  M backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java
  M backend/src/main/java/com/complex/platform/access/EffectivePermissions.java
  M backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
  M backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
  M backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
git diff --numstat：
  14  3  AccessGrantPolicy.java
   2  2  EffectivePermissions.java
   2  2  JdbcAccessRepository.java
  20  0  AccessJdbcIntegrationTests.java
   2  2  AccessServiceTests.java
git diff --summary（mode/create/delete）：空
hunks=8（冻结 diff 8 个 @@）
+/- 实数：+40 / -9（内容行；文件头 / index / --- / +++ / @@ 不计）→ 49 条 ± 行
禁止路径（HTML / findings / ledger / 锁 / AuthorizationService.java / AccessService.java / AccessController.java / AccessRepository.java / PositionRoleTemplate.java）：name-only 无匹配
B3 diff 正文 `rg requireProjectScopeId|grant_group_id` → 无（仅 002 注释里 `{@link AuthorizationService#effectivePermissions}`，不是改那个文件）
```

分支 `main`，`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`，`origin/main=2c178f7a47c5f744cf769e2db5f25cddccb19a27`。`test -f .git/MERGE_HEAD` → **NO_MERGE**。

blob / mode（`git ls-tree`；两树比较）：

| 路径 | before mode/blob | after mode/blob | 工作区 hash-object vs after |
|---|---|---|---|
| `AccessGrantPolicy.java` | `100644` `6adf014460a30672f50301a334c79003dfa000ec` | `100644` `d9f4d3d27b490280632d10f621b379ee9afb4257` | 相同 |
| `EffectivePermissions.java` | `100644` `382629b951fb78ef3a88d7aa97a4ec40874ede08` | `100644` `a5e210a2289e0445c2e7cf1075700973d6259399` | 相同 |
| `JdbcAccessRepository.java` | `100644` `680fda305730eae25b21359fab0c43feb1f45c14` | `100644` `837b04aa35440c9f02f3867650105242a78c15da` | 相同 |
| `AccessJdbcIntegrationTests.java` | `100644` `cb23d668e93397cb53b26f8529edf3085553ee57` | `100644` `0dbc4fecd64528b41df307668c304ad898b36bbd` | 相同 |
| `AccessServiceTests.java` | `100644` `1c34f772232f8787b702fce9afea55213ca6d185` | `100644` `da9feeb5da6c3dd62cf3e87559b7ae9a8e87c246` | 相同 |
| `AuthorizationService.java`（禁止本批改） | `100644` `4bfae6d2ddded7e2810666bac72bfce818ff9e22` | 同左 | — |
| `AccessController.java`（已闭环 M031） | `100644` `5524ed405227e0778aebc89257ed9995eff2b9d9` | 同左 | — |
| `AccessRepository.java`（已闭环 M031） | `100644` `a5978929f641d488aba54475873511261648ef85` | 同左 | — |
| `AccessService.java`（已闭环 M031） | `100644` `de7e6c4fc0bdc813b4bb59867bc7b5016603d32f` | 同左 | — |
| `PositionRoleTemplate.java` | `100644` `342040efb348cd33acd32a59edcf7406c77a564f` | 同左 | — |

工作区磁盘 mode（`stat -c %a`）：五份改动文件均为 `664`。git 树内 mode 修前修后 `100644`，未变；`git diff --summary` 无 mode 行。可执行位未变。

内容 SHA256（本席对 blob 实算）：

| 路径 | before | after |
|---|---|---|
| AccessGrantPolicy.java | `6f0de0f7052efa45430347547746379da6ec376235db46fe9e19f5017e087fb7`（2184 B） | `81873e72e8332532af9469de5e2182f2988ca2f5ab5fdf31550470930796e8e2`（2915 B） |
| EffectivePermissions.java | `68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e`（882 B） | `55bef627be66a02a45ddc4968a15c60b1e9d3357de7e3eaeb8a819f085e0247e`（960 B） |
| JdbcAccessRepository.java | `ecc78c0b65ecf3cf4154947c6160622b39eb881404bbb3501788839a74c9d86b`（75999 B） | `7639b374fd956a5d02a24bca0a7d6165873f2df9fe52a06fd421e7088ef995ba`（76013 B） |
| AccessJdbcIntegrationTests.java | `e81ba90973fd97220b259c3d448a5de87ca844d8cfbc964309f9f35872790a98`（42969 B） | `ba1d17c374af1bb0693937b20a78b776f4c2db70c52a66c0a1196d69f163e282`（44357 B） |
| AccessServiceTests.java | `077ce9e6b00430c7cc7d83abf95601b8bff665a57a95fb53f8458403e1d265f6`（126863 B） | `bf931a403f1217b4a23f184e4234e19b1f78cd8f1e1b5342b58ebf2805aebf1a`（126922 B） |

五份 after 均以 `\n` 结尾、无 CRLF。工作区五文件 SHA256 与 after 相同。before 树中 AccessGrantPolicy / EffectivePermissions / AccessJdbcIntegrationTests / AccessServiceTests 的 blob 与 `origin/main` 相同；Jdbc before 相对 origin/main 为 `19 insertions, 16 deletions`（未发布工作流，D-029），after 相对 origin/main 为 `21 insertions, 18 deletions`（未发布 hunk 仍在 + 本批 2/2）。

Jdbc 发布变体（不在两树之内、只作检查点带出核）：`/home/james/audit-tmp/m032_JdbcAccessRepository.publish.java` SHA256 `30faa630325fe784785b29ca37fd5eb4d5429f9215ca03f17ef9c6d1aec1ee5f`。相对 `origin/main`（blob `976a02abe85920b8983ce18f702c5093dc6fd27e`，内容 SHA256 `560d0e01028f5b474ca945949de67ca90578721d3984d03e94b93a6191a0dc95`）为 **+2 / -2**：版本行 V0.12→V0.13、该 SQL 行。`parameters = parameters || CAST(:parameters AS jsonb)` 与主工作区 after 同文逐字节相同。发布变体 `rg requireProjectScopeId` 空（origin/main 本就没有该闸）；`grant_group_id` 出现处与 origin/main 相同，不是本批新增。闸 3 只审两 tree 之差。

模块闭集：`plan/modules.md` M032 FILES = 25。测试文件属 B2 写入集合（AccessJdbcIntegrationTests＝M035 PENDING；AccessServiceTests＝M036 PENDING），闸 3 归因到 finding ID 即合法。

环境（D-029）：主工作区 AccessServiceTests 因别的模块未发布改动有 6 个报错、M006JdbcRegressionTests 1 条（夹具缺 `grant_group_id`），不得在主工作区当 GREEN 证据。本席实跑取自 origin/main 一次性 worktree。本席宿主默认 OpenJDK 无完整 javac 路径风险沿用 M031：改用 `/usr/lib/jvm/temurin-21-jdk-amd64`（javac 21.0.12.1）。只导出三个库变量，库名改为 `complex_test`；未 source `/etc/complex/complex.env`。进程环境里预先存在 `COMPLEX_HOME` / `COMPLEX_CONFIG_DIR` / `COMPLEX_LOG_DIR`（非本席 export），与三个库变量无关。

闸 3：diff＝两 tree 之间全部差异；每一 `+`/`-` 行须归到 V3.01 归属「是」的 ID（M032-001 / 002）；归不到即越界。只登记不入队项（W-1…W-4 与观察）不得出现在变更集。

---

## 2. 定稿最小处置（审计标尺，摘 V3.01 + 本席 B2-r1）

| ID | 分类 | 修复队列 | 最小处置（定稿） | 本席 B2 实施边界（采纳进 V3.01 文首） |
|---|---|---|---|---|
| M032-001 | 细化 | 是 | `AccessGrantPolicy` 增可空第 5 组件 `BoundedInt deadlineWorkingDays`；紧凑构造器不把它列入非空闭集；序列化空则不输出；保留 4 参便捷构造器。`JdbcAccessRepository.saveGrantPolicy` 改为 `parameters = parameters \|\| CAST(:parameters AS jsonb)`，主工作区与发布变体同一处同一文本。AccessJdbcIntegrationTests 补真库回归一条；AccessServiceTests 内存政策 JSON 补键。 | 第 5 组件可空、`@JsonInclude(NON_NULL)`（类或该组件）、4 参 `this(..., null)`；禁止改 AccessService 的 ObjectMapper、禁止 primitive int、禁止只 ignore-unknown、禁止发布变体夹带 `requireProjectScopeId`／`grant_group_id`、禁止动 M037 夹具。冻结支撑 GEN-002／GEN-006／GEN-008；PGM-026 不作本条支撑、不扩锁。 |
| M032-002 | 技术性 | 是 | 只改 `EffectivePermissions` 类注释为现状（`wildcard` 现恒 false；平台管理员日常码已并入 `grants`；`denies` 仍优先），不改字段与计算。 | 禁止改 `wildcard` 字段、构造第四参、`if (wildcard)` 计算、前端读方。 |

只登记不入队（不得混进本 diff）：W-1…W-4（未发布工作流：Jdbc 空范围 403、Quietly 会抛、`grant_group_id` 夹具、`MASTER_ACCOUNT_TYPE` 编译失败）；以及两版都有的观察（`withLifecycle` 恒 inactive 等）。本 diff 未碰这些。

B2 写入集合＝`AccessGrantPolicy.java` ＋ `JdbcAccessRepository.java`（只动 `saveGrantPolicy` 一条 SQL）＋ `EffectivePermissions.java` ＋ `AccessJdbcIntegrationTests.java` ＋ `AccessServiceTests.java`。一次 B3 禁止改 `AccessService.java`、`AuthorizationService.java`、`SessionIssuer`、三 HTML、锁、未发布隔离／整组消耗 hunk、已闭环 M031 三个文件。

---

## 3. diff 全文（冻结件 9201 bytes，与 `git diff --binary` 字节级相同）

```
diff --git a/backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java b/backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java
index 6adf0144..d9f4d3d2 100644
--- a/backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java
+++ b/backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java
@@ -1,13 +1,19 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.01
+// 文件版本：V0.02
 import com.complex.platform.common.api.ApiException;
 import java.util.Map;
 import java.util.Set;
 
-/** M006-016：沿用主管复核策略表保存授权政策；查询不能覆盖政策值。 */
+/**
+ * M006-016：沿用主管复核策略表保存授权政策；查询不能覆盖政策值。
+ * M032-001：库内该行自 V389 起带审批通用时限 {@code deadlineWorkingDays}；记录类须认得它（否则读不出整份政策），
+ * 未跑 V389 的库没有该键故可为空；为空时不输出，保存走合并写，库内原值保留。
+ */
 public record AccessGrantPolicy(BoundedInt windowDays, BoundedInt positionThreshold,
-                                BoundedInt userThreshold, Map<String, ReviewRule> riskReviews) {
+                                BoundedInt userThreshold, Map<String, ReviewRule> riskReviews,
+                                @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
+                                BoundedInt deadlineWorkingDays) {
     public AccessGrantPolicy {
         if (windowDays == null || positionThreshold == null || userThreshold == null
                 || riskReviews == null || !riskReviews.keySet().equals(Set.of("low", "medium", "high", "critical"))
@@ -17,6 +23,11 @@ public record AccessGrantPolicy(BoundedInt windowDays, BoundedInt positionThresh
         riskReviews = Map.copyOf(riskReviews);
     }
 
+    public AccessGrantPolicy(BoundedInt windowDays, BoundedInt positionThreshold,
+                             BoundedInt userThreshold, Map<String, ReviewRule> riskReviews) {
+        this(windowDays, positionThreshold, userThreshold, riskReviews, null);
+    }
+
     public record BoundedInt(int value, int min, int max) {
         public BoundedInt {
             if (min < 1 || max < min || value < min || value > max) {
diff --git a/backend/src/main/java/com/complex/platform/access/EffectivePermissions.java b/backend/src/main/java/com/complex/platform/access/EffectivePermissions.java
index 382629b9..a5e210a2 100644
--- a/backend/src/main/java/com/complex/platform/access/EffectivePermissions.java
+++ b/backend/src/main/java/com/complex/platform/access/EffectivePermissions.java
@@ -10,8 +10,8 @@ import java.util.List;
  * DENY 优先级最高。{@code grants} 已剔除被 DENY 覆盖的权限码（实现 A），同时仍单独返回 {@code denies}
  * 供前端展示冲突来源；前端 {@code hasPermissions} 对是否预扣两种形态判定结果一致。
  *
- * <p>{@code wildcard=true} 仅当 actor 命中平台管理员桥（status=active 且 accountType=platform_admin），
- * 但前端/后端仍须让 {@code denies} 优先于 wildcard。
+ * <p>{@code wildcard} 现恒为 {@code false}：平台管理员不再是通配，其日常功能码已直接并入 {@code grants}
+ * （见 {@link AuthorizationService#effectivePermissions}）；{@code denies} 仍优先。字段保留是为不改接口形状。
  */
 public record EffectivePermissions(
         String userId,
diff --git a/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java b/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
index 680fda30..837b04aa 100644
--- a/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
+++ b/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
@@ -1,5 +1,5 @@
 package com.complex.platform.access;
-// 文件版本：V0.12
+// 文件版本：V0.13
 import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
@@ -776,7 +776,7 @@ class JdbcAccessRepository implements AccessRepository {
     @Override
     public void saveGrantPolicy(String policyJson) {
         int changed = jdbcTemplate.update("""
-                UPDATE supervisor_review_policies SET parameters = CAST(:parameters AS jsonb), updated_at = now()
+                UPDATE supervisor_review_policies SET parameters = parameters || CAST(:parameters AS jsonb), updated_at = now()
                  WHERE policy_code = 'access_grant' AND enabled = true
                 """, Map.of("parameters", policyJson));
         if (changed != 1) throw new IllegalStateException("授权政策未配置");
diff --git a/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
index cb23d668..0dbc4fec 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
@@ -624,6 +624,26 @@ class AccessJdbcIntegrationTests {
                 .containsEntry("result", "expired");
     }
 
+    // M032-001（治理地图「全平台审批通用时限」为后台业务参数）：库内现行授权政策行（V389 起带 deadlineWorkingDays）
+    // 必须能按服务层同一解析路径读出；保存一份不带该键的政策后，库内该键保持原值。
+    @Test
+    void storedGrantPolicyParsesAndSavingKeepsDeadlineWorkingDays() throws Exception {
+        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
+        String stored = repository.findGrantPolicy().orElseThrow();
+        int days = mapper.readTree(stored).path("deadlineWorkingDays").path("value").asInt();
+        assertThat(days).isPositive();
+
+        AccessGrantPolicy current = mapper.readValue(stored, AccessGrantPolicy.class);
+
+        repository.saveGrantPolicy(mapper.writeValueAsString(new AccessGrantPolicy(
+                current.windowDays(), current.positionThreshold(), current.userThreshold(), current.riskReviews())));
+        assertThat(jdbcTemplate.queryForObject(
+                "SELECT (parameters -> 'deadlineWorkingDays' ->> 'value')::int FROM supervisor_review_policies"
+                        + " WHERE policy_code = 'access_grant'", Integer.class)).isEqualTo(days);
+        assertThat(mapper.readValue(repository.findGrantPolicy().orElseThrow(), AccessGrantPolicy.class).windowDays().value())
+                .isEqualTo(current.windowDays().value());
+    }
+
     // M006-G-07（PGM-1.10.1「旧派生同步失效」）：已定岗改职级复用定岗的四步写入（职级、定岗、套模板、替换派生），
     // 入口必须与 assignUserPosition 一样由 Spring 事务整体包住，否则套模板已提交、替换派生失败时新旧派生并存。
     @Test
diff --git a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
index 1c34f772..da9feeb5 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.07
+// 文件版本：V0.08
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
@@ -1675,7 +1675,7 @@ class AccessServiceTests {
 
     private static final class InMemoryAccessRepository implements AccessRepository {
         final Map<String, PermissionDefinition> permissions = new LinkedHashMap<>();
-        String policy = "{\"windowDays\":{\"value\":30,\"min\":1,\"max\":2147483647},\"positionThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"userThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"riskReviews\":{\"low\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"medium\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"high\":{\"approvalLevel\":\"two_levels_up\",\"requiredLevel\":2},\"critical\":{\"approvalLevel\":\"top_authority\",\"requiredLevel\":3}}}";
+        String policy = "{\"windowDays\":{\"value\":30,\"min\":1,\"max\":2147483647},\"positionThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"userThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"riskReviews\":{\"low\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"medium\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"high\":{\"approvalLevel\":\"two_levels_up\",\"requiredLevel\":2},\"critical\":{\"approvalLevel\":\"top_authority\",\"requiredLevel\":3}},\"deadlineWorkingDays\":{\"value\":3,\"min\":1,\"max\":30}}";
         final Map<String, String> fingerprints = new LinkedHashMap<>();
         final Map<String, String> positionRoles = new LinkedHashMap<>();
         @Override public void lockGrantAuthorizationFacts() {} // 单线程内存夹具；真实锁在 M006JdbcRegressionTests 验证。
```

---

## 4. diff 逐块／逐行归因（闸 3；49 条 ± 行）

归因规则：每一 `+`/`-` 行必须落到 V3.01 归属「是」的 ID。文件版本注释随该文件本批 finding。验证性测试归因到它所钉的 ID（§5 闸 3 / §6 TDD）。

| # | 文件 | hunk | ± | 内容（截） | ID |
|---|---|---|---|---|---|
| 01 | AccessGrantPolicy.java | H1 | − | `// 文件版本：V0.01` | M032-001 |
| 02 | AccessGrantPolicy.java | H1 | ＋ | `// 文件版本：V0.02` | M032-001 |
| 03 | AccessGrantPolicy.java | H1 | − | 旧单行 javadoc | M032-001 |
| 04–08 | AccessGrantPolicy.java | H1 | ＋ | 扩 javadoc（含可空／NON_NULL／合并写说明） | M032-001 |
| 09 | AccessGrantPolicy.java | H1 | − | 四组件记录收尾 | M032-001 |
| 10–12 | AccessGrantPolicy.java | H1 | ＋ | 第 5 组件 + 组件级 `@JsonInclude(NON_NULL)` | M032-001 |
| 13–17 | AccessGrantPolicy.java | H2 | ＋ | 4 参便捷构造器 `this(..., null)`（含空行） | M032-001 |
| 18–19 | EffectivePermissions.java | H1 | − | 旧「wildcard=true 仅当平台管理员桥」两行 | M032-002 |
| 20–21 | EffectivePermissions.java | H1 | ＋ | 现状：wildcard 恒 false、日常码并入 grants、denies 仍优先、字段保留 | M032-002 |
| 22 | JdbcAccessRepository.java | H1 | − | `// 文件版本：V0.12` | M032-001 |
| 23 | JdbcAccessRepository.java | H1 | ＋ | `// 文件版本：V0.13` | M032-001 |
| 24 | JdbcAccessRepository.java | H2 | − | `parameters = CAST(:parameters AS jsonb)` | M032-001 |
| 25 | JdbcAccessRepository.java | H2 | ＋ | `parameters = parameters \|\| CAST(:parameters AS jsonb)` | M032-001 |
| 26–45 | AccessJdbcIntegrationTests.java | H1 | ＋ | 真库回归 `storedGrantPolicyParsesAndSavingKeepsDeadlineWorkingDays`（20 行） | M032-001 |
| 46 | AccessServiceTests.java | H1 | − | `// 文件版本：V0.07` | M032-001 |
| 47 | AccessServiceTests.java | H1 | ＋ | `// 文件版本：V0.08` | M032-001 |
| 48 | AccessServiceTests.java | H2 | − | 内存政策 JSON 四键 | M032-001 |
| 49 | AccessServiceTests.java | H2 | ＋ | 内存政策 JSON 补 `deadlineWorkingDays:{value:3,min:1,max:30}` | M032-001 |

计数：M032-001 = 45 行（AccessGrantPolicy 17 + Jdbc 4 + 真库测试 20 + AccessServiceTests 4）；M032-002 = 4 行。合计 49 = 40＋ + 9−。**0 行未归因。**

未入队 ID 未出现在变更集：W-1…W-4 的 `requireProjectScopeId` / Quietly / `grant_group_id` 消耗改动 / `MASTER_ACCOUNT_TYPE` 均不在两树之差。before 与 after 的 Jdbc 仍都带未发布 hunk（`requireProjectScopeId` 行号 75/202/309/418 两树相同），本批只新增版本行与 `saveGrantPolicy` SQL。

---

## 5. 内容审计

### 5.1 M032-001 读 409 + 写抹键

**甲、记录类。** after `AccessGrantPolicy.java`：
- 第 5 组件 `BoundedInt deadlineWorkingDays`，类型可空，不是 primitive `int`。
- 紧凑构造器非空闭集仍只强制 `windowDays` / `positionThreshold` / `userThreshold` / `riskReviews`（及 riskReviews 四键闭集）；**未**把第 5 组件列入。未跑 V389 的库缺键 → Jackson 默认 `FAIL_ON_MISSING_CREATOR_PROPERTIES=false` → null，不进 `BoundedInt` 紧凑校验。
- 组件级 `@JsonInclude(Include.NON_NULL)`（B2 允许类或该组件）。4 参对象序列化不会写出 `"deadlineWorkingDays":null`，因此乙的 `||` 不会把库内对象盖成 JSON null。
- 4 参便捷构造器 `this(..., null)` 保留。`M006JdbcRegressionTests.java:144` 仍用四参；本席 GREEN 该类 12/12。新真库条也走四参保存。

**乙、合并写。** after `JdbcAccessRepository.saveGrantPolicy:779` 与发布变体 L786 同文：`parameters = parameters || CAST(:parameters AS jsonb)`。客户端没带的键保持库内原值。接口签名未改（graft：`AccessRepository.saveGrantPolicy` 仍 default；生产入边仍是已闭环 AccessService）。未改 WHERE、未改 `enabled = true`、未改「未配置」抛错。

**丙、测试。**
- 真库条用同款 `new ObjectMapper().readValue`（B2 允许 `grantPolicy()` 或同款 mapper）；`isPositive()` 在缺键时 `asInt()==0` 会红；四参保存后 SQL 断言库内 value 仍等于读出的 days；顺带 windowDays 不变。GREEN 该类 25/25（BASE 24 → 本批 +1）。
- 内存 JSON 补 `deadlineWorkingDays` 后，凡经 `grantPolicy()` 的 AccessServiceTests 不再 409。本席 RED：只留测试补丁、生产仍 origin 四字段 → 该类 54 测 11 error，文案「后台授权政策格式或上下限无效」，与席甲日志同 11 条方法名。

未改 AccessService 的 ObjectMapper（AccessService blob 两树均为 `de7e6c4f…`）。未只加 `@JsonIgnoreProperties`。未发明出厂默认日。

### 5.2 M032-002 注释收口

before 类注释现在时声称 `wildcard=true` 仅当平台管理员桥。after 改为：wildcard 现恒 false；日常功能码已并入 grants；denies 仍优先；字段保留以免改接口形状。记录组件仍是 `userId, grants, denies, boolean wildcard, computedAt`，一字未动。graft：生产唯一构造点仍 `AuthorizationService.java:206` 第四参恒 `false`；本 diff 未改 AuthorizationService（两树 blob 同）。符合 GEN-016（现在时声称须可复现）与 B2「只改注释、不改字段与计算」。`{@link AuthorizationService#effectivePermissions}` 是注释交叉引用，不是改那个文件。

### 5.3 业务发明 / 越界 / 可执行位

- **业务发明：无。** 001 把库内自 V389 已有的键纳入记录类并改成合并写，不新开时限规则、不改三 HTML、不 LOCK-AMEND。002 只把不可复现的现在时改成现状。
- **越界：无。** 五文件均在 B2 写入集合。未动 AuthorizationService、未动 Jdbc 未发布范围闸与 `grant_group_id` 消耗改动、未动已闭环 M031 三个文件、未动 HTML / 锁 / findings / ledger。测试文件归因到 001，合法。
- **可执行位与文件模式：未变。** 两树 `100644`，`git diff --summary` 空，磁盘 `664`。
- **多余抽象（ponytail-review）：** `Lean already. Ship.` 组件级 FQCN `@JsonInclude` 省一个 import；4 参转发是为不改 M037。真库一条同时钉读出与不抹键，比拆两条更短。

### 5.4 本席独立实跑（D-029 worktree，已删除）

```
git worktree add --detach /home/james/wt-m032-b4-grok-ro origin/main   # HEAD=2c178f7a
# 从 after 树拷入 AccessGrantPolicy / EffectivePermissions / AccessJdbcIntegrationTests / AccessServiceTests
# Jdbc ← publish 变体（origin/main + 本批 2/2）
# 临时拷入主工作区未发布 V427（Flyway 校验；用后随 worktree 删除）
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
# 只导出 COMPLEX_DATABASE_URL（dbname=complex_test）/ USERNAME / PASSWORD
# 未 source /etc/complex/complex.env
mvn -o -Dtest=AccessControllerTests,AccessJdbcIntegrationTests,AccessServiceTests,AuthorizationServiceTests,M006JdbcRegressionTests,M006SecurityRegressionTests test
```

GREEN（本席 2026-09-20T23:27:48–23:28:03 -07:00）：`BUILD SUCCESS`，GREEN_EXIT=0。Flyway `Successfully validated 396 migrations`；schema 当前 428，无新迁移。jdbc URL `jdbc:postgresql://127.0.0.1:5432/complex_test`。

| 类 | tests | failures | errors | skipped |
|---|---|---|---|---|
| AccessControllerTests | 13 | 0 | 0 | 0 |
| AccessJdbcIntegrationTests | 25 | 0 | 0 | 0 |
| AccessServiceTests | 54 | 0 | 0 | 0 |
| AuthorizationServiceTests | 21 | 0 | 0 | 0 |
| M006JdbcRegressionTests | 12 | 0 | 0 | 0 |
| M006SecurityRegressionTests | 39 | 0 | 0 | 0 |
| 合计 | 164 | 0 | 0 | 0 |

worktree 内四文件 hash-object 与 after 树相同；Jdbc 为发布变体 SHA256 `30faa630…`。

RED（同 worktree，把 AccessGrantPolicy / Jdbc / EffectivePermissions 还原 origin/main，保留 after 测试）：`BUILD FAILURE`，RED_EXIT=1。

| 类 | tests | errors | 信号 |
|---|---|---|---|
| AccessJdbcIntegrationTests | 25 | 1 | `storedGrantPolicyParsesAndSavingKeepsDeadlineWorkingDays`：`Unrecognized field "deadlineWorkingDays"`（4 known properties） |
| AccessServiceTests | 54 | 11 | 全部 `ApiException 后台授权政策格式或上下限无效`，方法名与席甲日志逐条相同 |

合计 79 测、12 error，与席甲「只打测试补丁 RED 共 12 条」同因。测试能红，不是同义反复。`git worktree remove --force` 后 `test -e /home/james/wt-m032-b4-grok-ro` → GONE。主工作区五文件仍与 after 树相同，未被 RED 还原污染。

席甲首跑日志本席只读核对，不替代本席实跑：BASE 13/24/54/21/12/39；RED 12 error；GREEN 13/25/54/21/12/39。与本席合。主工作区同批 6+1 报错文案为「退款、免单、授权与资金事项必须登记直接受益人」与 `grant_group_id` 缺列，属 B1 已查清的未发布工作流／别模块，与本 diff 无关。

---

## 6. grilling 自问自答（不派子代理、不等操作员）

1. **缺 `@JsonInclude(NON_NULL)` 时乙会不会把时限盖成 JSON null？** 会。4 参默认序列化 `"deadlineWorkingDays":null`，`||` 右侧 null 覆盖左侧对象。本 diff 有组件级 NON_NULL；GREEN 真库条保存四参后 value 仍为正。否。
2. **紧凑构造器若把第 5 组件列入非空闭集？** 未跑 V389 的库缺键即 409，比现状更重。源码未列入。否。
3. **只加 ignore-unknown 不建模该键？** GET 仍看不见时限，且须另配乙。本 diff 建模了该键。否。
4. **发布变体有没有夹带 requireProjectScopeId / grant_group_id？** 相对 origin/main 仅版本行 + SQL 行（+2/−2）；`rg requireProjectScopeId` 空。否。
5. **两树之差有没有把未发布 hunk 带进闸 3？** before Jdbc 已含那些 hunk；name-status 与 ± 行只有版本与 SQL。否。
6. **002 注释里的 AuthorizationService 链接算越界吗？** 文件未改（blob 同）；只是注释指向唯一构造点。否。
7. **真库条是否测到 mapper 而非 grantPolicy()？** B2 明确允许同款 `new ObjectMapper().readValue`。AccessService 禁止本批改。合法。
8. **内存假仓不实现 ||，抹键防护会不会假绿？** 会。抹键以真库条为准；RED 在 origin 生产上先炸在读侧 Unrecognized field，读 409 是 001 的更重缺陷。是。
9. **4 参构造器是否仍编译？** GREEN 含 M006JdbcRegressionTests 12/12 与真库条四参保存。是。
10. **有无第三文件或可执行位？** name-status 5 个 M；summary 空；mode 100644。无。

---

## 7. 票

- **归因票：PASS。** 49 条 `+`/`-` 全部归到 V3.01 归属「是」的 M032-001 / 002；0 越界。
- **内容票：PASS。** 001：可空第 5 组件、紧凑构造器非空闭集不含它、JsonInclude NON_NULL、4 参便捷构造器、jsonb `||` 合并写（主工作区与发布变体同一文本）、真库回归 + 内存 JSON 补键；未动 AccessService / 未发布闸 / M031 三文件。002：只改类注释为现状，字段与计算未动。无业务发明。可执行位与文件模式未变。
- **合取：PASS。** 内容与归因均为 PASS。本票不是模块 PASS，不是 B5。

本席 B4-r1 **PASS**。

（CLI 不附机器标记。完整报告到此为止。）
