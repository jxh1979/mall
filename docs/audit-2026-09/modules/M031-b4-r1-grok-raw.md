CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M031 B4-r1 Grok 席乙只读审计（修复 diff）
版本：V0.01
席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
步骤：B4-r1（160-audit-prompt.md V0.17 §4 B4 + §5 闸 3）
进度：模块 M031 / 共 471，B3 第 1 次
正本：`/home/james/complex-management` main 主工作区
对象：席甲 B3-r1 整批修复 diff（功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第1段；M031 FILES 闭集 3 文件 `AccessController.java` / `AccessRepository.java` / `AccessService.java`）
findings 正本：`docs/audit-2026-09/findings.md` V2.98，已定稿且归属「是」：M031-001、M031-002、M031-003（跨模块 XM-M031-A/B/C 只登记、不得出现在本 diff）
独立性：本席未读 `M031-b4-r1-self-raw.md`（`ls` 见该路径存在、44079 bytes、mtime Sep 20 22:01，本会话未打开正文）；未派 MCP / 子代理 / 第三席；未问人；未改业务 / HTML / 锁 / findings / ledger；未 fetch / commit / push / merge / deploy；未建脚本 / 持久日志 / 审计临时产物。一次性 `git worktree add --detach /home/james/wt-m031-b4-grok-ro origin/main` 拷入 after 树本模块文件与同批测试文件及 M006 发布变体，实跑 GREEN 与 002 变异后已 `git worktree remove --force`，目录已不存在。旧 B1/B2 可对照（`M031-b1-grok-raw.md` V0.01+V0.02）；本轮以 V2.98 定稿最小处置与本席 B2-r1 实施边界为准。不对任何数据库执行写语句（仅跑既有测试类）；未 source `/etc/complex/complex.env`。

---

## 0. 技能预检（§6，按本环境实核）

核到且按收窄口径使用：
1. `using-superpowers`：已读开工路由段。用户指令优先：本步只读、两席、不问人。
2. `using-arc`：已读开头；其默认派评审子代理与双席铁律冲突，本席不走 Arc `audit` 流程、不写 `docs/arc/audits/`。
3. `graft`：已读并调用。`graft grep "changePositionTemplateLifecycle"`；`graft callers AccessService.savePositionRoleTemplate --direction in --depth 2`；`graft callers AccessService.recomputeUsersOnPositionTemplate --direction in`；`graft callers AccessService.changePositionTemplateLifecycle --direction out`。tokens saved ≈ **34,831 + 78,970 + 30,795 + 31,907 = 176,503**。证明：私有 `savePositionRoleTemplate`（含重算）只被 create/update 调用；restore 走 `repository.savePositionRoleTemplate`；`recomputeUsersOnPositionTemplate` 唯一生产入口仍是私有 save。
4. `grilling`：已读开头；对抗线自问自答；本体「等用户 / 派子代理」作废。结论见 §7。
5. `ponytail-review`：已读；只找可删的多余抽象；不改码。结论见 §7。
6. `verification-before-completion`：已读。PASS 前当场：两树 `git diff --binary` 与冻结 diff `cmp`、blob SHA256、mode、独立 worktree GREEN、002 变异红、worktree 删除。
7. `requesting-code-review`：已读。B4＝本席即外审；不用 `HEAD~1`、不用子代理复审。
8. `long-running-background-tasks`：已读；本步 sha256 / maven / worktree 均前台完成，未挂 monitor。
9. `codebase-design`：已核。本 diff 不改 AccessRepository 接口，只在既有 seam（Service 写口 + 测试假仓适配器）上补行为；未新开接口。
10. `diagnosing-bugs` / `systematic-debugging`：已核存在。收窄为「RED 命令（席甲日志）→ 本席 GREEN / 变异」；不加诊断桩、不修码。
11. `test-driven-development` / `tdd`：本步只核席甲是否先测后改。席甲首跑日志：只打测试补丁时 001 两条 + 003 一条失败、002 新测试已绿；本席独立变异证明 002 断言能红。
12. `writing-for-agents`：本 raw 给下游席甲归并用；结论与证据共置。
13. `resolving-merge-conflicts`：`test -f .git/MERGE_HEAD` → NO_MERGE。

明确不用（提示词划掉或本步不适用；本席未另开这些 SKILL.md 正文当流程跑完）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`（一次性 detach 是 D-028 允许的只读核，不是该技能本体）、`executing-plans`、`research`、`ponytail-audit`、mattpocock `code-review`。`brainstorming` / `writing-plans` / `ponytail` 本体属 A/B3，本步只读不修。`receiving-code-review` 属席甲收票。`audit-context-building` 要求派 function-analyzer 子代理，与「只允许两席」冲突，不执行。

缺口（实找无独立可调用技能目录，不冒充已调用）：`batch-grill-me`、`ubiquitous-language`。`git-guardrails-claude-code` 有、本步无 git 写。`i-have-adhd` 在环境技能列表中；本文件第一行仍必须是判据锁哈希（A2 交叉报数优先于该技能「首行即下一步」），CLI 不回结论。

---

## 1. 输入身份（类型 4）

本席独立实算（未从 sidecar / ledger / 派发词抄写）：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
→ b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```
sidecar 全文一行与 ledger 顶行 `CRITERIA-LOCK-SHA256:` 与本席实算同值，仅作旁证，不以它们代替实算。`criteria-lock.md` 文首版本 V0.11；`LOCK-AMEND-COUNT: 1` 只读见到，未改。findings 文首「版本：V2.98」；`sha256sum docs/audit-2026-09/findings.md` → `9772a3fe289688b7af5ade5338861639dcefa00be0dda358c88ab3ad5fe82b1b`。本席 B2 票所在 `M031-b1-grok-raw.md` 全文 SHA256 `b557a5aba194ec0f4ea02de53d7780186e27df0386d6bef891f83ccf81335cba`。

冻结 diff：
```
wc -c /home/james/audit-tmp/m031_b3r1.diff → 18200
sha256sum → e1d337122226fd5bb839ae20ab0893b6c3a8ae3dd9f171bea15199e4b43b856d
```
与派发声明一致。

tree（`git cat-file -t` 均为 tree）：
- before `9d8468b35cb4617044df7c61fd72a26f9083b337`
- after  `f2b1cd10fc7a0fa71faec2e4f78a5f9ed95a4826`

```
git diff --binary <before> <after> | sha256sum
→ e1d337122226fd5bb839ae20ab0893b6c3a8ae3dd9f171bea15199e4b43b856d
git diff --binary <before> <after> | cmp - /home/james/audit-tmp/m031_b3r1.diff → CMP_OK
git diff --stat：3 files changed, 177 insertions(+), 5 deletions(-)
git diff --name-status：
  M backend/src/main/java/com/complex/platform/access/AccessService.java
  M backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
  M backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
git diff --numstat：
  33  1  AccessService.java
  141 2  AccessServiceTests.java
  3   2  M006SecurityRegressionTests.java
git diff --summary（mode/create/delete）：空
hunks=11（冻结 diff 11 个 @@）
+/- 实数：+177 / -5（内容行；文件头 / index / --- / +++ / @@ 不计）
禁止路径（HTML / findings / ledger / 锁 / M032 的 JdbcAccessRepository.java 与 PositionRoleTemplate.java / AuditEventService / OpenAPI）：name-only 无匹配
```

分支 `main`，`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`，`origin/main=508b2e87ca43a0e40f1fa81aaf1561aa9ba7def6`。`test -f .git/MERGE_HEAD` → **NO_MERGE**。

blob / mode（`git ls-tree`；两树比较）：

| 路径 | before mode/blob | after mode/blob | 工作区 hash-object vs after |
|---|---|---|---|
| `AccessService.java` | `100644` `3720af8cf3a6a5d1b114789b8342fda2b790be1d` | `100644` `de7e6c4fc0bdc813b4bb59867bc7b5016603d32f` | 相同 |
| `AccessServiceTests.java` | `100644` `e24ea166b1af528f0592cbae52ee359dfe6fc60b` | `100644` `1c34f772232f8787b702fce9afea55213ca6d185` | 相同 |
| `M006SecurityRegressionTests.java` | `100644` `3629dd5d350f700c9a0df3e3f4cabf223278f482` | `100644` `2d815f0e662115e30099fffb25ce2e0b6b88e29d` | 相同（主工作区另含别的模块未发布改动，已在 before 树；两树之差仅本批 3 行） |
| `AccessController.java`（M031 FILES，禁止本批无 ID 改） | `100644` `5524ed405227e0778aebc89257ed9995eff2b9d9` | 同左 | 同左 |
| `AccessRepository.java`（M031 FILES） | `100644` `a5978929f641d488aba54475873511261648ef85` | 同左 | 同左 |
| `JdbcAccessRepository.java`（M032，禁止改） | `100644` `680fda305730eae25b21359fab0c43feb1f45c14` | 同左 | — |
| `PositionRoleTemplate.java`（M032，禁止改） | `100644` `342040efb348cd33acd32a59edcf7406c77a564f` | 同左 | — |

工作区磁盘 mode（`stat -c %a`）：三份改动文件均为 `664`。git 树内 mode 修前修后 `100644`，未变；`git diff --summary` 无 mode 行。可执行位未变。

内容 SHA256（本席对 blob 实算）：

| 路径 | before | after |
|---|---|---|
| AccessService.java | `f38bff0b90f42703756a4069fafe560a16b834e8c30b34b9bb1b6a57d3e580bb`（128036 B） | `4556c02cf35206e8e5032855a0e9c6d8915beb3c43f2ae6f77fb1ce615dfeb0d`（130307 B） |
| AccessServiceTests.java | `cdf6adc034499ed46411db909f320afd58584c1530f22b0afecbad0fcae36749`（117456 B） | `077ce9e6b00430c7cc7d83abf95601b8bff665a57a95fb53f8458403e1d265f6`（126863 B） |
| M006SecurityRegressionTests.java | `f5ae66e2c12c632c5f5d4c403eb9992ee1d23d175df422a22da10e2057e5fa7e`（59359 B） | `167733bc7c2b9fb07271779bb17d9447ccbefe6c5f7c56da3a6329b12c0b40b6`（59529 B） |

三份 after 均以 `\n` 结尾、无 CRLF。工作区三文件 SHA256 与 after 相同。

M006 发布变体（不在两树之内、只作检查点带出核）：`/home/james/audit-tmp/m031_M006SecurityRegressionTests.publish.java` SHA256 `c2d8b8a69779b7c5555cc49c7776f233b0e16bc875c22bfc96149799f48698c3`。相对 `origin/main`（V0.24，SHA256 `9222ef61267af1358937fce257bb570bb491fc8dd0a928b536b1146e001c7f64`）为 **+3 / -2**：版本行、`doAnswer` upsert 桩、断言 `inactive`→`active`。与冻结两树之差语义相同（冻结基线是主工作区未发布 V0.29→V0.30）。闸 3 只审两 tree 之差。

模块闭集：`plan/modules.md` M031 FILES = 3（Controller / Repository / Service）。测试文件属 B2 写入集合（AccessServiceTests＝M036 PENDING；M006SecurityRegressionTests＝M037 PENDING），闸 3 归因到 finding ID 即合法。origin/main 同路径 Service/Tests 与 before/after 均不同：属主工作区既有未发布改动（D-028），**不是本批两树之差**。

环境（D-028）：主工作区 AccessServiceTests 因别的模块未发布改动有 6 个报错，不得在主工作区当 GREEN 证据。本席实跑取自 origin/main 一次性 worktree。本席宿主默认 `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` 无 `javac`（`release version 21 not supported`）；改用 `/usr/lib/jvm/temurin-21-jdk-amd64`（javac 21.0.12.1）后编过。只导出三个库变量，库名改为 `complex_test`；未 source `/etc/complex/complex.env`。

闸 3：diff＝两 tree 之间全部差异；每一 `+`/`-` 行须归到 V2.98 归属「是」的 ID（M031-001 / 002 / 003）；归不到即越界。只登记不入队项（XM-A/B/C 与观察）不得出现在变更集。

---

## 2. 定稿最小处置（审计标尺，摘 V2.98 + 本席 B2-r1）

| ID | 分类 | 修复队列 | 最小处置（定稿） | 本席 B2 实施边界（采纳进 V2.98 文首） |
|---|---|---|---|---|
| M031-001 | 细化 | 是 | restore 先查同范围启用中替代映射（`findPositionRoleTemplate(org, position, level)` 命中且 `organizationId` 与 current 相同）→ 409 中文「同岗位同职级已有启用中的岗位模板，须先停用或存档后再恢复」，状态不变；否则先 `savePositionTemplateLifecycle` 再 `repository.savePositionRoleTemplate` 把同一条写回 `active` 并返回启用态。存档期间派生未收回，恢复不触发重算。测试：M006 L617→`active`；AccessServiceTests「存档→恢复→可定岗」与「已有替代启用映射时恢复被拒且状态不变」。只改 AccessService，不动 M032。 | 409 零写；顺序不可反；不走私有 `savePositionRoleTemplate`；不摘 `@Transactional`；M006 同一测试须给 upsert 补桩（或改断言返回值）；内存仓按 Jdbc：lifecycle 恒写 inactive，upsert 不动生命周期列 |
| M031-002 | 细化 | 是 | AccessServiceTests 覆盖重算两分支：改映射后在岗人员派生换新且薪资不变；停用后派生收回。RED 若暴露实现缺陷则最小修 AccessService。 | 不借机改 N+1；不扩组织维度岗位映射 |
| M031-003 | 技术性 | 是 | 两方法体包 try/catch RuntimeException → `recordFailure` 再抛；事件名 `access.position_role_template.lifecycle_failed`、`access.grant_policy.change_failed`；权限码与业务类型沿用各自成功事件；测试覆盖两处失败落事件。真库留得住依赖 XM-M031-B，本模块不改事务传播。 | 不摘 11 个 `@Transactional`；不改 M006-G-07；不给 AuditEventService 加 `REQUIRES_NEW`；不把 `grantSummary` 读口塞进本条 |

只登记不入队（不得混进本 diff）：XM-M031-A（→M032，`requireProjectScopeId` 未发布 Jdbc）、XM-M031-B（→M092，失败事件随 `@Transactional` 回滚丢失）、XM-M031-C（→M392，OpenAPI 六路径）；以及 B2 观察（N+1、organizationId 恒 null、读口无查看事件、wildcard 死分支等）。本 diff 未碰这些。

B2 写入集合＝`AccessService.java` ＋ `AccessServiceTests.java` ＋ `M006SecurityRegressionTests.java`。一次 B3 禁止改 `JdbcAccessRepository.java`、`PositionRoleTemplate.java`、`AuditEventService`、OpenAPI、三 HTML、锁。

---

## 3. diff 全文（冻结件 18200 bytes，与 `git diff --binary` 字节级相同）

```
diff --git a/backend/src/main/java/com/complex/platform/access/AccessService.java b/backend/src/main/java/com/complex/platform/access/AccessService.java
index 3720af8c..de7e6c4f 100644
--- a/backend/src/main/java/com/complex/platform/access/AccessService.java
+++ b/backend/src/main/java/com/complex/platform/access/AccessService.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.20
+// 文件版本：V0.21
 
 import com.complex.platform.access.dto.AccessGrantApplyRequest;
 import com.complex.platform.access.dto.AccessGrantReviewRequest;
@@ -484,6 +484,17 @@ public class AccessService {
 
     @Transactional
     public PositionRoleTemplate changePositionTemplateLifecycle(String id, StatusChangeRequest request) {
+        // M031-003（GEN-011）：生命周期操作失败也留痕，与同文件其余写口同一范式。
+        try {
+            return changePositionTemplateLifecycleAudited(id, request);
+        } catch (RuntimeException exception) {
+            auditEventService.recordFailure("access.position_role_template.lifecycle_failed", "access.role.assign",
+                    "position_role_template", id, null, request == null ? null : request.idempotencyKey(), codeOf(exception));
+            throw exception;
+        }
+    }
+
+    private PositionRoleTemplate changePositionTemplateLifecycleAudited(String id, StatusChangeRequest request) {
         requireRequest(request);
         AccessActor actor = authorizationService.currentActor();
         if (!authorizationService.isMasterAuthority(actor)) {
@@ -504,6 +515,11 @@ public class AccessService {
                 case "restore" -> {
                     if (current.archivedAt() == null) throw ApiException.conflict("INVALID_STATE", "模板未存档");
                     if (current.deleteReviewId() != null && current.deletedAt() == null) throw ApiException.conflict("DELETE_REVIEW_PENDING", "须先处理删除复核");
+                    // M031-001（BSM-004「想恢复点"取消存档"即可」）：恢复即回到可定岗的启用态；同范围已有启用中的替代映射时拒绝，不静默留成停用。
+                    if (repository.findPositionRoleTemplate(current.organizationId(), current.positionCode(), current.staffLevelId())
+                            .filter(active -> java.util.Objects.equals(active.organizationId(), current.organizationId())).isPresent()) {
+                        throw ApiException.conflict("POSITION_TEMPLATE_ACTIVE_EXISTS", "同岗位同职级已有启用中的岗位模板，须先停用或存档后再恢复");
+                    }
                     updated = current.withLifecycle(null, null, null, null, now);
                 }
                 case "request_delete" -> {
@@ -541,6 +557,11 @@ public class AccessService {
                 default -> throw ApiException.badRequest("INVALID_ACTION", "不支持的岗位模板生命周期操作");
             }
             repository.savePositionTemplateLifecycle(updated, authorizationService.currentActor().id());
+            if ("restore".equals(action)) {
+                updated = new PositionRoleTemplate(updated.id(), updated.positionCode(), updated.staffLevelId(), updated.roleId(),
+                        updated.templateId(), updated.organizationId(), "active", updated.createdBy(), updated.createdAt(), now);
+                repository.savePositionRoleTemplate(updated);
+            }
             auditEventService.recordSuccess("access.position_role_template." + action, "access.role.assign",
                     "position_role_template", id, null, request.idempotencyKey(), Map.of("reason", reason));
             return updated;
@@ -1921,6 +1942,17 @@ public class AccessService {
 
     @Transactional
     public AccessGrantPolicy updateGrantPolicy(AccessGrantPolicy.Update request) {
+        // M031-003（GEN-011）：授权政策保存失败也留痕。
+        try {
+            return updateGrantPolicyAudited(request);
+        } catch (RuntimeException exception) {
+            auditEventService.recordFailure("access.grant_policy.change_failed", "admission.policy.manage",
+                    "supervisor_review_policy", "access_grant", null, request == null ? null : request.idempotencyKey(), codeOf(exception));
+            throw exception;
+        }
+    }
+
+    private AccessGrantPolicy updateGrantPolicyAudited(AccessGrantPolicy.Update request) {
         if (request == null || request.policy() == null) throw ApiException.badRequest("VALIDATION_FAILED", "授权政策不能为空");
         requireText(request.operatorNote(), "operator_note");
         return idempotencyService.execute("updateAccessGrantPolicy", request.idempotencyKey(), request, () -> {
diff --git a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
index e24ea166..1c34f772 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.06
+// 文件版本：V0.07
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
@@ -1503,6 +1503,134 @@ class AccessServiceTests {
                 "pending", null, "scope test", "usr_requester", null, null, projectId, null, now, now);
     }
 
+    // M031-001（BSM-003；业务地图原文「想恢复点"取消存档"即可」）：存档后恢复即回到可定岗的启用态。
+    @Test
+    void restoringAnArchivedPositionMappingMakesItAssignableAgain() {
+        authenticateAsPlatformAdmin("seed-org");
+        PositionRoleTemplate mapping = seedRestoreFixture("usr_restore", "ops_restore", "map-restore");
+        service.changePositionTemplateLifecycle(mapping.id(), new StatusChangeRequest("restore-archive", "archive", "停止使用", List.of()));
+        assertThatThrownBy(() -> service.assignUserPosition(new UserPositionAssignmentRequest(
+                "assign-while-archived", null, "usr_restore", "ops_restore", "lvl_m1_lower", null, null, null)))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("POSITION_ROLE_TEMPLATE_NOT_FOUND"));
+
+        PositionRoleTemplate restored = service.changePositionTemplateLifecycle(mapping.id(),
+                new StatusChangeRequest("restore-restore", "restore", "恢复使用", List.of()));
+
+        assertThat(restored.status()).isEqualTo("active");
+        PositionRoleTemplate stored = accessRepository.findPositionRoleTemplateById(mapping.id()).orElseThrow();
+        assertThat(stored.status()).isEqualTo("active");
+        assertThat(stored.archivedAt()).isNull();
+        service.assignUserPosition(new UserPositionAssignmentRequest(
+                "assign-after-restore", null, "usr_restore", "ops_restore", "lvl_m1_lower", null, null, null));
+        assertThat(accessRepository.positionRoles.get("usr_restore")).isEqualTo("role_restore");
+    }
+
+    // M031-001：同岗位同职级已有启用中的替代映射时，恢复被拒且原映射状态不变。
+    @Test
+    void restoringIsRejectedWhileAnActiveReplacementMappingExists() {
+        authenticateAsPlatformAdmin("seed-org");
+        PositionRoleTemplate mapping = seedRestoreFixture("usr_restore_blocked", "ops_restore_blocked", "map-restore-blocked");
+        service.changePositionTemplateLifecycle(mapping.id(), new StatusChangeRequest("blocked-archive", "archive", "停止使用", List.of()));
+        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
+                "map-restore-replacement", null, "ops_restore_blocked", "lvl_m1_lower", "role_restore", "tpl_restore", "active"));
+
+        assertThatThrownBy(() -> service.changePositionTemplateLifecycle(mapping.id(),
+                new StatusChangeRequest("blocked-restore", "restore", "恢复使用", List.of())))
+                .isInstanceOfSatisfying(ApiException.class, e -> {
+                    assertThat(e.status().value()).isEqualTo(409);
+                    assertThat(e.code()).isEqualTo("POSITION_TEMPLATE_ACTIVE_EXISTS");
+                });
+
+        PositionRoleTemplate unchanged = accessRepository.findPositionRoleTemplateById(mapping.id()).orElseThrow();
+        assertThat(unchanged.status()).isEqualTo("inactive");
+        assertThat(unchanged.archivedAt()).isNotNull();
+    }
+
+    private PositionRoleTemplate seedRestoreFixture(String userId, String positionCode, String idempotencyKey) {
+        accessRepository.users.put(userId, new AccessUser(
+                userId, userId, "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
+        accessRepository.roles.put("role_restore", new RoleRecord(
+                "role_restore", "role_restore", "Ops checker", "project", "active",
+                Instant.parse("2026-06-14T00:00:00Z"), List.of("ops.production_readiness.check"), null));
+        accessRepository.saveTemplate(new PermissionTemplate(
+                "tpl_restore", "tpl_restore", "Ops Check", null, "base",
+                null, "platform", null, "merge", null, null, false,
+                "active", null, null, null,
+                Instant.parse("2026-06-14T00:00:00Z"),
+                Instant.parse("2026-06-14T00:00:00Z")),
+                List.of(new PermissionTemplateItem(
+                        "tpl_restore", "ops.production_readiness.check", AccessRepository.Effect.GRANT, "project", false)));
+        return service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
+                idempotencyKey, null, positionCode, "lvl_m1_lower", "role_restore", "tpl_restore", "active"));
+    }
+
+    // M031-002（治理地图「系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效」）：
+    // 保存岗位映射后在岗人员按新映射重算、薪资不变；映射停用则收回派生。
+    @Test
+    void savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants() {
+        authenticateAsPlatformAdmin("seed-org");
+        accessRepository.users.put("usr_recompute", new AccessUser(
+                "usr_recompute", "recompute", "staff", "seed-org", null, "active",
+                Instant.parse("2026-06-14T00:00:00Z")));
+        for (String roleId : List.of("role_recompute_old", "role_recompute_new")) {
+            accessRepository.roles.put(roleId, new RoleRecord(
+                    roleId, roleId, "Ops checker", "project", "active",
+                    Instant.parse("2026-06-14T00:00:00Z"),
+                    List.of("ops.production_readiness.check"),
+                    null));
+        }
+        accessRepository.saveTemplate(new PermissionTemplate(
+                "tpl_recompute", "tpl_recompute", "Ops Check", null, "base",
+                null, "platform", null, "merge", null, null, false,
+                "active", null, null, null,
+                Instant.parse("2026-06-14T00:00:00Z"),
+                Instant.parse("2026-06-14T00:00:00Z")),
+                List.of(new PermissionTemplateItem(
+                        "tpl_recompute",
+                        "ops.production_readiness.check",
+                        AccessRepository.Effect.GRANT,
+                        "project",
+                        false)));
+        PositionRoleTemplate mapping = service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
+                "map-recompute", null, "ops_recompute", "lvl_m1_lower",
+                "role_recompute_old", "tpl_recompute", "active"));
+        service.assignUserPosition(new UserPositionAssignmentRequest(
+                "assign-recompute", null, "usr_recompute", "ops_recompute", "lvl_m1_lower",
+                500000L, "CNY", null));
+        assertThat(accessRepository.positionRoles.get("usr_recompute")).isEqualTo("role_recompute_old");
+
+        service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
+                "map-recompute-change", null, "ops_recompute", "lvl_m1_lower",
+                "role_recompute_new", "tpl_recompute", "active"));
+
+        assertThat(accessRepository.positionRoles.get("usr_recompute")).isEqualTo("role_recompute_new");
+        assertThat(accessRepository.findStaffLevelAssignment("usr_recompute")).get()
+                .extracting(StaffLevelAssignment::baseSalaryAmountCent)
+                .isEqualTo(500000L);
+
+        service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
+                "map-recompute-off", null, "ops_recompute", "lvl_m1_lower",
+                "role_recompute_new", "tpl_recompute", "inactive"));
+
+        assertThat(accessRepository.positionRoles).containsKey("usr_recompute");
+        assertThat(accessRepository.positionRoles.get("usr_recompute")).isNull();
+    }
+
+    // M031-003（GEN-011）：岗位模板生命周期与授权政策两个写口失败也留痕。
+    @Test
+    void positionTemplateLifecycleAndGrantPolicyFailuresAreAudited() {
+        authenticateAsPlatformAdmin("seed-org");
+        assertThatThrownBy(() -> service.changePositionTemplateLifecycle("prt_missing",
+                new StatusChangeRequest("lifecycle-missing", "archive", "停止使用", List.of())))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("POSITION_ROLE_TEMPLATE_NOT_FOUND"));
+        assertThatThrownBy(() -> service.updateGrantPolicy(null))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
+        assertThat(auditEventRepository.events)
+                .filteredOn(event -> "failure".equals(event.eventResult()))
+                .extracting(AuditEvent::eventName)
+                .contains("access.position_role_template.lifecycle_failed", "access.grant_policy.change_failed");
+    }
+
     private void authenticateAsPlatformAdmin(String organizationId) {
         AuthService.UserRecord user = new AuthService.UserRecord(
                 "usr_admin", "seed-admin", "hash", "platform_admin", organizationId, null, "active",
@@ -1753,12 +1881,23 @@ class AccessServiceTests {
                     .findFirst());
         }
 
+        // 与 JdbcAccessRepository 同语义：upsert 不动存档/删除列；生命周期保存恒写 inactive。
         @Override
         public PositionRoleTemplate savePositionRoleTemplate(PositionRoleTemplate mapping) {
-            positionRoleTemplates.put(mapping.id(), mapping);
+            PositionRoleTemplate existing = positionRoleTemplates.get(mapping.id());
+            positionRoleTemplates.put(mapping.id(), existing == null ? mapping : new PositionRoleTemplate(
+                    mapping.id(), mapping.positionCode(), mapping.staffLevelId(), mapping.roleId(), mapping.templateId(),
+                    mapping.organizationId(), mapping.status(), mapping.createdBy(), mapping.createdAt(), mapping.updatedAt(),
+                    existing.archivedAt(), existing.deletedAt(), existing.deleteReviewId(), existing.deleteReason()));
             return mapping;
         }
 
+        @Override
+        public void savePositionTemplateLifecycle(PositionRoleTemplate mapping, String actorId) {
+            positionRoleTemplates.put(mapping.id(), mapping.withLifecycle(mapping.archivedAt(), mapping.deletedAt(),
+                    mapping.deleteReviewId(), mapping.deleteReason(), mapping.updatedAt()));
+        }
+
         @Override
         public void deletePositionRoleTemplate(String id) {
             positionRoleTemplates.remove(id);
diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
index 3629dd5d..2d815f0e 100644
--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.29
+// 文件版本：V0.30
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.ArgumentMatchers.*;
@@ -557,6 +557,7 @@ class M006SecurityRegressionTests {
         when(repository.findPositionRoleTemplateById("lifecycle")).thenAnswer(a -> Optional.of(mapping.get()));
         when(repository.listPositionRoleTemplates(null)).thenAnswer(a -> List.of(mapping.get()));
         doAnswer(a -> { mapping.set(a.getArgument(0)); return null; }).when(repository).savePositionTemplateLifecycle(any(), anyString());
+        doAnswer(a -> { mapping.set(a.getArgument(0)); return a.getArgument(0); }).when(repository).savePositionRoleTemplate(any());
         doReturn(1L).when(repository).countPositionTemplateUsage("lifecycle");
         var reviewRepository = mock(com.complex.platform.governance.review.SupervisorReviewRepository.class);
         Map<String, com.complex.platform.governance.review.SupervisorReviewDecisionRecord> decisions = new HashMap<>();
@@ -614,7 +615,7 @@ class M006SecurityRegressionTests {
         SecurityContextHolder.getContext().setAuthentication(masterAuthentication);
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("restore", "restore", "恢复留存配置", List.of()));
         assertThat(mapping.get().deletedAt()).isNull();
-        assertThat(mapping.get().status()).isEqualTo("inactive");
+        assertThat(mapping.get().status()).isEqualTo("active"); // M031-001：恢复即回到启用态
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("self-archive", "archive", "停止使用", List.of()));
         int reviewCount = decisions.size();
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("self-request", "request_delete", "最高权限本人删除", List.of()));
```

---

## 4. 逐块／逐行归因（闸 3）

归属「是」的已定稿 ID 闭集：`{M031-001, M031-002, M031-003}`。本 diff `+` 行 = 177，`-` 行 = 5。文件头 / index / `---` / `+++` / `@@` 是 diff 元数据，不是被改文件内容。下列覆盖全部 182 条内容变更行。

| 块 | 路径 | 行类 | 条数 | 原文（截） | 归属 ID | 理由 |
|---|---|---|---|---|---|---|
| H1 | `AccessService.java` | `-`/`+` | 1/1 | `// 文件版本：V0.20` → `V0.21` | 001+003 | 本文件本批生产改动来自 001 与 003；仓库文件版本随改随 bump，不是第三行为 |
| H2 | 同上 | `+` | 11 | public 方法改 try/catch `recordFailure("access.position_role_template.lifecycle_failed", "access.role.assign", "position_role_template", …)` 再抛；抽出 `changePositionTemplateLifecycleAudited` | 003 | 与同文件 `executeTemplateWrite` 同一范式；权限码/业务类型沿用成功事件 `access.role.assign` / `position_role_template`；`@Transactional` 仍在 public 方法上 |
| H3 | 同上 | `+` | 5 | restore 内 `findPositionRoleTemplate` + `Objects.equals(organizationId)` → `ApiException.conflict("POSITION_TEMPLATE_ACTIVE_EXISTS", "同岗位同职级已有启用中的岗位模板，须先停用或存档后再恢复")` | 001 | 写在 `updated = withLifecycle` 与任何 save **之前**；409 中文与定稿逐字相同 |
| H4 | 同上 | `+` | 5 | `savePositionTemplateLifecycle` 之后若 restore：10 参构造 `status="active"`（生命周期字段默认 null）再 `repository.savePositionRoleTemplate(updated)` | 001 | 先 lifecycle 再 upsert；走仓储不是私有 save；返回值被赋成启用态对象 |
| H5 | 同上 | `+` | 11 | `updateGrantPolicy` 同样 try/catch `recordFailure("access.grant_policy.change_failed", "admission.policy.manage", "supervisor_review_policy", "access_grant", …)`；抽出 `updateGrantPolicyAudited` | 003 | 权限码/业务类型沿用成功事件；`request == null` 短路避免 NPE |
| H6 | `AccessServiceTests.java` | `-`/`+` | 1/1 | `V0.06` → `V0.07` | 001+002+003 | 本测试文件本批三条 ID 都加了用例 |
| H7a | 同上 | `+` | 22 | `restoringAnArchivedPositionMappingMakesItAssignableAgain`：存档后定岗 `POSITION_ROLE_TEMPLATE_NOT_FOUND`；恢复后 status/archivedAt/可定岗 | 001 | 定稿「存档→恢复→可定岗」；`archivedAt()==null` 能抓住「只 upsert 不清存档列」 |
| H7b | 同上 | `+` | 21 | `restoringIsRejectedWhileAnActiveReplacementMappingExists`：409 + `POSITION_TEMPLATE_ACTIVE_EXISTS`；原映射仍 inactive 且 archivedAt 非空 | 001 | 定稿「已有替代启用映射时恢复被拒且状态不变」 |
| H7c | 同上 | `+` | 18 | `seedRestoreFixture` | 001 | 上两条共用夹具，不是新行为 |
| H7d | 同上 | `+` | 52 | `savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants`：改角色后 `positionRoles==role_recompute_new` 且薪资 500000；停用后 get==null | 002 | 定稿两分支都执行；派发词允许「一条覆盖两分支」。RED 阶段该测试已绿（实现无缺陷，002 只补测试面） |
| H7e | 同上 | `+` | 15 | `positionTemplateLifecycleAndGrantPolicyFailuresAreAudited`：缺模板 / `updateGrantPolicy(null)` 后失败事件名包含两处 | 003 | 定稿「测试覆盖两处」；派发词允许一条覆盖两写口 |
| H8a | 同上 | `+` | 1 | 注释「与 JdbcAccessRepository 同语义：upsert 不动存档/删除列；生命周期保存恒写 inactive」 | 001 | B2 点名的内存仓语义 |
| H8b | 同上 | `-`/`+` | 1/5 | `savePositionRoleTemplate` 由整对象 put 改为保留 existing 的 archived/deleted/review/reason | 001 | 对齐 Jdbc upsert 不写生命周期列；否则测不到「必须先 lifecycle」 |
| H8c | 同上 | `+` | 5 | 新增 `savePositionTemplateLifecycle`：`put(id, mapping.withLifecycle(…))` | 001 | `withLifecycle` 恒写 `"inactive"`，对齐 Jdbc `SET status='inactive'` |
| H9 | `M006SecurityRegressionTests.java` | `-`/`+` | 1/1 | `V0.29` → `V0.30` | 001 | 该文件本批只服务 001 |
| H10 | 同上 | `+` | 1 | `doAnswer(a -> { mapping.set(a.getArgument(0)); return a.getArgument(0); }).when(repository).savePositionRoleTemplate(any());` | 001 | B2：「同一测试须给 upsert 补桩」。`doAnswer` 不先调默认方法（默认抛 UnsupportedOperationException） |
| H11 | 同上 | `-`/`+` | 1/1 | 断言 `inactive` → `active` | 001 | 定稿点名改 M006 恢复后断言 |

未归因行：**0**。越界行：**0**。

H7 细分核：hunk7 共 128 条 `+`：22+21+18+52+15=128。H8 共 13 条变更（+12/-1）。H1–H6+H9–H11 共 41。182=177+5。

---

## 5. 内容审计（是否按定稿最小处置与 B2 边界）

### 5.1 M031-001 restore

核 after 源 `AccessService.java:515-564`（本席 Read，非抽样）：

1. **替代映射检查在任何写之前。** restore case 在 `withLifecycle` 之前调用 `repository.findPositionRoleTemplate(current.organizationId(), current.positionCode(), current.staffLevelId()).filter(active -> java.util.Objects.equals(active.organizationId(), current.organizationId()))`，命中即 `ApiException.conflict`。Jdbc `findPositionRoleTemplate` 只认 `status='active' AND archived_at IS NULL AND deleted_at IS NULL`，当前行仍是 archived/inactive，**不会命中自己**。`Objects.equals` 含双方 null。与 B2 一致。
2. **409 中文逐字。** `"同岗位同职级已有启用中的岗位模板，须先停用或存档后再恢复"`。错误码 `POSITION_TEMPLATE_ACTIVE_EXISTS` 是 409 的内部码（PGM-001 允许），不是新业务规则；B2 已写「409 是把 UNIQUE 冲突说成人话」。
3. **零写。** throw 在 switch 内、`savePositionTemplateLifecycle` 在 switch 后，409 不进两步写。
4. **两步顺序。** 先 `repository.savePositionTemplateLifecycle(updated, actorId)`（`updated` 此时是 `withLifecycle(null,null,null,null,now)` → status 被写死 inactive、存档/删除列清空），再 `new PositionRoleTemplate(…, "active", …)`（10 参构造把 archived/deleted/review/reason 置 null），再 `repository.savePositionRoleTemplate(updated)`。Jdbc upsert 不写生命周期列，故必须先 lifecycle；顺序不可反。返回的 `updated` 已是 active，不会把 inactive 对象交出去。
5. **不走私有 save。** after 仍有 `private PositionRoleTemplate savePositionRoleTemplate(String, Request)`（L2092，末尾 `recomputeUsersOnPositionTemplate`）。graft：该私有方法只被 create/update 调用。restore 调用的是 `repository.savePositionRoleTemplate`。`@Transactional` before/after 均为 **11**，lifecycle public 方法注解仍在。
6. **不改 M032。** Jdbc / PositionRoleTemplate 两树 blob 相同。
7. **测试。** AccessServiceTests 两条行为断言 + 内存仓语义；M006 断言改 active 且 `doAnswer` 桩。席甲首跑曾用 `when/thenAnswer` 触发默认方法 `UnsupportedOperationException`（测试桩写法错误）；**冻结 after 已是 `doAnswer`**，本席不把已修正的桩错误算进本 diff。

内存仓 after `AccessServiceTests.java:1884-1899`：upsert 保留 existing 生命周期四列；lifecycle 走 `withLifecycle`（恒 inactive）。与 B2 / Jdbc 一致。

### 5.2 M031-002 重算测试面

生产代码 **零行** 服务 002。席甲首跑：只打测试补丁时该新测试已绿。本席变异：worktree 内把 `recomputeUsersOnPositionTemplate(saved, request);` 注释掉后，`savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants` **红**：`expected: "role_recompute_new" but was: "role_recompute_old"`（L1606）。断言能失败，不是同义反复。

一条 `@Test` 连续走 update-to-new-role 与 update-to-inactive，覆盖定稿两分支（派生换新 + 薪资不变 + 停用收回）。定稿表写「补两条」、派发词写「一条覆盖重算两分支」；合取看行为覆盖，不是 `@Test` 方法计数。未改 N+1、未扩 organizationId。无实现缺陷可修，完成节口径与「RED 未暴露缺陷则只补测试」一致。

### 5.3 M031-003 失败留痕

- 事件名与定稿逐字相同。
- 权限码/实体沿用成功路径：lifecycle 成功是 `"access.role.assign"` + `"position_role_template"`；grant policy 成功是 `"admission.policy.manage"` + `"supervisor_review_policy"` + `"access_grant"`。
- 只捕 `RuntimeException` 再抛，与 `executeTemplateWrite` 同构。抽出 `*Audited` 私有方法是为了让 `@Transactional` 仍包住 try/catch（同类自调用不会再开代理）；不是新写口、不是新事务传播。
- 未摘注解、未改 AuditEventService、未改 M006-G-07。真库生效依赖 XM-M031-B 的限定仍成立，本 diff 没有假装已修好回滚丢失。
- 测试一条打两处：缺 id → `POSITION_ROLE_TEMPLATE_NOT_FOUND`；`updateGrantPolicy(null)` → `VALIDATION_FAILED`（定稿列举的首次正常失败之一）。

### 5.4 业务发明 / 越界 / 可执行位

- **业务发明：无。** 恢复到可定岗启用态是 BSM-004 对照原文下的细化，不是新状态机；409 是部分唯一索引的人话。未新开权限码、未改 HTML、未 LOCK-AMEND。
- **越界：无。** 三文件均在 B2 写入集合；M032 / HTML / 锁 / findings / ledger / OpenAPI / AuditEventService 两树未动。Controller / Repository 本模块文件 blob 未变。
- **可执行位与文件模式：未变。** 两树 `100644`，`git diff --summary` 空，磁盘 `664`。
- **多余抽象（ponytail-review）：** `Lean already. Ship.` 002/003 把「两条/各一条」收成能失败的一条，是更短不是更长。`*Audited` 私有方法若改成 70 行方法体内直接 try/catch 可少两个符号，但不减少行为、也不构成越界；不作为 FAIL。

### 5.5 本席独立实跑（D-028 worktree，已删除）

```
git worktree add --detach /home/james/wt-m031-b4-grok-ro origin/main   # HEAD=508b2e87
# 从 after 树拷入 AccessController/Repository/Service + AccessServiceTests + AccessControllerTests
# M006 ← publish 变体（origin/main + 本批 3 行）
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
# 只导出 COMPLEX_DATABASE_URL（dbname=complex_test）/ USERNAME / PASSWORD
# 未 source /etc/complex/complex.env
mvn -o -Dtest=AccessControllerTests,AccessServiceTests,M006SecurityRegressionTests test
```

GREEN（本席 2026-09-20T22:11:11–22:11:24 -07:00）：

| 类 | tests | failures | errors | skipped |
|---|---|---|---|---|
| AccessControllerTests | 13 | 0 | 0 | 0 |
| AccessServiceTests | 54 | 0 | 0 | 0 |
| M006SecurityRegressionTests | 39 | 0 | 0 | 0 |
| 合计 | 106 | 0 | 0 | 0 |

`BUILD SUCCESS`，GREEN_EXIT=0。worktree 内 AccessService / AccessServiceTests SHA256 与 after 树相同；M006 为发布变体 SHA256 `c2d8b8a6…`。

变异（同 worktree，随后删除）：注释唯一 `recomputeUsersOnPositionTemplate(saved, request);` → `MUTATION_EXIT=1`，002 新测试 1 failure：`expected: "role_recompute_new" but was: "role_recompute_old"`。与席甲 `m031_b3r1_mutation.log` 同因。`git worktree remove --force` 后 `test -e /home/james/wt-m031-b4-grok-ro` → GONE。主工作区 AccessService 未被变异（仍是 after 的 +33/-1）。

席甲首跑 / 续跑日志本席只读核对，不替代本席实跑：BASE 13+50+39；RED 001 两条 + 003 一条失败、M006 一条 error（桩）、002 已绿；续跑 doAnswer 后 13+54+39。与本席 GREEN 合。

---

## 6. grilling 自问自答（不派子代理、不等操作员）

1. **restore 会不会误走重算？** graft：私有 save 的入边不含 lifecycle。源码 H4 是 `repository.savePositionRoleTemplate`。否。
2. **只 upsert 不清 archived_at，Jdbc 下能定岗吗？** 否。本 diff 先 lifecycle。H7a 断言 `archivedAt()==null` 能抓住只 upsert。
3. **先 upsert 后 lifecycle？** lifecycle SQL 恒 inactive，会盖掉 active。源码顺序是 lifecycle 再 upsert。
4. **409 是否误伤自己？** find 只认 active 未存档；检查在 withLifecycle 之前。否。
5. **内存仓若不实现 lifecycle，001 测试会不会假绿？** 会（旧 put 整对象替换能藏 Jdbc 缺口）。本 diff 已按 Jdbc 实现。H7a RED 曾是 `expected active but was inactive`，证明走了 lifecycle 恒 inactive 路径。
6. **002 是否测到实现而非口号？** 变异把重算去掉即红。是。
7. **003 是否在本模块「修好」真库留痕？** 否；仍依赖 XM-M031-B。本 diff 未摘注解、未改传播。符合定稿限定。
8. **M006 主工作区未发布改动是否混进闸 3？** 在 before 与 after 两树都有，diff 只有版本行 + 桩 + 断言。否。
9. **有无第三文件或可执行位？** name-status 3 个 M；summary 空；mode 100644。无。

---

## 7. 票

- **归因票：PASS。** 182 条 `+`/`-` 全部归到 V2.98 归属「是」的 M031-001 / 002 / 003；0 越界。
- **内容票：PASS。** 001 两步写、409 零写、不走私有 save、不摘事务、不动 M032、测试与内存仓语义按 B2；002 只补能失败的测试面；003 补同文件失败留痕范式且不改事务传播。无业务发明。可执行位与文件模式未变。
- **合取：PASS。** 内容与归因均为 PASS。本票不是模块 PASS，不是 B5。

本席 B4-r1 **PASS**。

（CLI 不附机器标记。完整报告到此为止。）
