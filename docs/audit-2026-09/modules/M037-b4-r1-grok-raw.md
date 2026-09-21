CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M037 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 独立只读审计席乙（grok-4.6 / xhigh）
计划：160 审计 V0.17 B4 与 §5 闸 3
本文件路径：`docs/audit-2026-09/modules/M037-b4-r1-grok-raw.md`
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

模块：M037 / 共 471，B3 第 1 次之后的 B4。计划名：功能权限点、职级三十档、权限模板与最终权限计算／后台验证／第3段。
修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。席甲为作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。

未读取：任何 `M037-b4-r1-self-raw.md`（落盘时该路径已存在，本席按独立性禁令整文件不打开；ledger 席甲 B4 封存行不构成本席证据）。已读：`160-audit-prompt.md` V0.17 B4 与 §5 闸 3／§6、`findings.md` V3.15 顶部「当前 M037 B2-r1 双PASS定稿」、本席旧 `M037-b1-grok-raw.md` V0.01/V0.02（B1+B2，允许）、两份冻结 diff、发布变体全文、origin/main 与两 tree 的测试类／AccessService 闸、B3 冻结 `m037_b3r1_run.log` 与 `m037_mutate.py`。ledger 只读取模块状态与 B3 次数。本票以两 tree 全文 diff、发布变体 diff、定稿最小处置、本席 B2-r1 边界、既有 RED/GREEN/变异日志＋本席独立重跑 GREEN 为准。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff，加上 D-029 发布变体相对 origin/main 的第二份 diff；M037-001／002 是否按 findings V3.15 定稿最小处置与本席 B2-r1 边界实现；是否最懒可行（ponytail ultra）；是否引入新缺陷或新业务发明；每一变更行能否归因到归属「是」的 ID（仅 001、002）；新测试断言能否失败；可执行位与文件模式是否未变。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例，未派子代理。

---

## 0. 技能预检与节点应用（本环境实核）

本席按 `160-audit-prompt.md` V0.17 §6 在 **本机路径** 核验，不抄 Codex 预检表。grilling 只借追问树自问自答（事实自己读；技能本体「问人 / 派子代理查事实」作废）。Arc `audit` 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，**不执行**。`requesting-code-review` 本体的 `HEAD~1` / 同会话子代理复审 / 无 ID 即刻修，一律不用——本席自身即 B4 外审。`receiving-code-review` 本节点不适用（本席产出意见，不收意见去修）。`test-driven-development` / mattpocock `tdd`：本轮只核 B3 已落的红绿／变异证据并本席重跑 GREEN，不新写测试。`resolving-merge-conflicts`：工作区无进行中 merge/rebase，节点不适用。`writing-for-agents`：本 raw 按完成准则写给下游合取席读。`git-guardrails-claude-code`：存在；本席未 push/reset/clean。`i-have-adhd`：存在；与「raw 第一行必须是独占锁行」冲突时，160 提示词优先。`brainstorming` / `writing-plans`：阶段 A 技能，B4 不适用。`ponytail`：B3 默认强度，B4 只审不修。`long-running-background-tasks`：本席 Maven 跑完即收，无后台作业遗留。`graft`：本环境存在，已用于定位 `requireActorCanGrant`／`requireGrantApprovalIfNeeded`／`savePositionTemplateLifecycle`／`PLATFORM_ADMIN_REQUIRED`／`MASTER_AUTHORITY_REQUIRED`／`reject_delete`。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由识别；**不执行** Arc 全量审计工作流 | 本体要派评审子代理，记缺口 |
| Arc `audit` | howells `skills/audit/SKILL.md` | 同上 | **不用**（第三席+§7 外产物） |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `graft grep` 定位两闸与生命周期写入点 | 无 |
| `grilling` / `grill-me` | mattpocock `skills/productivity/grilling/SKILL.md`；`grill-me` 仅转发 | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `batch-grill-me` | — | — | **本机技能树不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | 测试 adapter 对齐 SUT 闸序；本 diff 只改测试断言与本条夹具，不改被测模块 | 不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环=只读命令（三工具 sha256、`cmp` 冻结 diff、读 RED/变异违例行、本席重跑 GREEN） | 五个变异未本席重放（会改被测代码）；以既有 `m037_b3r1_run.log` 行号与 `m037_mutate.py`＋origin SUT 对齐 |
| `systematic-debugging` | superpowers | 只借读错误→追数据流（定稿声称→AccessService 两闸／生命周期 switch→测试断言）；作废插桩 | 无 |
| `verification-before-completion` | superpowers | PASS 前本席实算哈希/`cmp`/D-029 正文比对/两处 Maven GREEN；不以「应能过」代替 | 变异 5 红以既有日志为证 |
| `requesting-code-review` | superpowers | B4=本席即外审；基线=两 tree id 而非 `HEAD~1`；不派子代理 | 本体子代理路径作废 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删的越界添加 | 不落地 |
| `tdd` / `test-driven-development` | 存在 | 核 001／002 RED（五变异）→ GREEN（发布口径 39；主工作区 39）；不改被测即绿是定稿要求 | 无 |
| `long-running-background-tasks` | grok bundled | Maven 同步跑完 | 节点不适用遗留 |
| `git-guardrails-claude-code` | mattpocock misc | 只读；未装 hook | **存在但未安装** |
| Trail of Bits `audit-context-building` / `differential-review` | 存在 | 本体强制子代理 | **不用** |
| `resolving-merge-conflicts` | 存在 | 工作区无 merge/rebase | 节点不适用 |
| `writing-for-agents` | 存在 | 本 raw 给下游合取席 | 无 |
| `ponytail` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail/SKILL.md` | B4 只审：本条测试内第三码夹具 + 发布口径三处断言，ultra 已满足 | 不修 |
| `i-have-adhd` | `~/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 结论先行、编号；与独占锁行冲突时 160 优先 | 无 |

明确不用（提示词已划掉，本席亦不调用）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`（B3/B4/B5 主工作区就地；本席为发布口径独立 GREEN 建的 origin/main 一次性 worktree 已 `git worktree remove --force` 删除）、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`。未使用 MCP。未建脚本/持久日志/临时审计产物（`/tmp/m037_b4_r1_verify.diff` 在 `cmp` 后已删；一次性 worktree `/tmp/m037_b4_r1_pubwt` 已删除）。

graft 本席调用节省合计约 205621 tokens（`requireActorCanGrant` 30230 + `requireGrantApprovalIfNeeded` 30532 + `savePositionTemplateLifecycle` 52477 + `PLATFORM_ADMIN_REQUIRED` 30797 + `MASTER_AUTHORITY_REQUIRED` 30786 + `reject_delete` 30799）。

---

## 1. 输入与独立锁实算

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值，与本文件第一行独占锁行一致。文件 889020 字节。sidecar 一行同值（对照，非抄写来源）。ledger 顶部基准同值（对照）。criteria-lock 版本仍 V0.11。LOCK-AMEND-COUNT 仍为 1。未 LOCK-AMEND。

| 项 | 值 |
|---|---|
| 模块 | M037 后台验证／第3段（计划 1 文件；本轮 B3 写入集合＝`M006SecurityRegressionTests.java`，不改被测、不加迁移） |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 本轮修复队列 | findings.md V3.15：M037-001 技术性／是／是；002 细化／是／是；修复队列「否」0 |
| findings 全文 SHA256 | `65de8b84c14d55aca7a329b7610c1c3dde48752c3706666c0314e37478426443`（本席实算） |
| B3 | 已执行 1 次；模块仍 IN_PROGRESS |
| before（对象类型 tree） | `f0671f2cc9f80c735f0bb152d55e10894af3bd27` |
| after（对象类型 tree） | `4ad8f54ce984346dc01eb680498030753c92620f` |
| `git cat-file -t` | 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary f0671f2cc9f80c735f0bb152d55e10894af3bd27 4ad8f54ce984346dc01eb680498030753c92620f`（无路径过滤） |
| HEAD / 分支 | `e456ba96c84185c8d39f18f548011daac48a6466` / `main` |
| origin/main | `40fcc8f1b3429d2874ccf856a404b214364decc7` |

### 1.1 两份冻结产物独立复核

| 产物 | 声称 | 本席实算 | 对照 |
|---|---|---|---|
| 主工作区冻结 diff `/home/james/audit-tmp/m037_b3r1.diff` | 2584 bytes SHA256 `e18982ea461b932c0c0f89d3a14a274ed7c78d084921ce108ff7a2566658d070`；1 file, 12+/2- | 2584 bytes，同 SHA；`git diff --binary` 两 tree 写入 `/tmp/m037_b4_r1_verify.diff` 后 `cmp` **identical**，同 SHA | 通过 |
| 发布变体 diff `/home/james/audit-tmp/m037_b3r1_publish.diff` | 6014 bytes SHA256 `37dac1c3dec8704e06a5b4d35a011546adee030ad2d107487c03a276ecd28d23`；1 file, 29+/3- | 6014 bytes，同 SHA | 通过 |
| 发布变体全文 `/home/james/audit-tmp/m037_M006SecurityRegressionTests.publish.java` | SHA256 `c459a5d2246f05f40ea0a454fc6103e0bdf5ed07ac9cc89fb4f70e85ccd28273` | 60946 bytes，同 SHA；835 行 | 通过 |
| origin/main 测试类 + 发布 diff | 应得到发布变体 | `patch -p1` 应用于 `git show origin/main:…M006SecurityRegressionTests.java`，产物 SHA **等于** 发布变体 | 通过 |
| after blob vs 工作区文件 | after `1202d4a3…` | `git cat-file -p 1202d4a3…` 与工作区文件同 SHA `ce3a9006cb546eb6cb0e743cbe6a47f4c7c2b979f60d41e5bd2b009b8de78f7d` | 通过 |

文件模式：before `100644 blob 2d815f0e…`；after `100644 blob 1202d4a3…`；origin/main `100644 blob c83e3c77…`。两份 diff 头均为 `100644`，模式未变。

路径集合：主工作区 tree diff **仅** `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java`。`git diff --stat … -- backend/src/main/` 空。发布 diff 同样只此测试文件。被测 Java 不在写入集合内。

---

## 2. 两份 diff 全文

### 2.1 主工作区冻结 diff（before → after；只落 001）

```
diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
index 2d815f0e..1202d4a3 100644
--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.30
+// 文件版本：V0.31
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.ArgumentMatchers.*;
@@ -173,12 +173,22 @@ class M006SecurityRegressionTests {
     }
 
     // M006-005：已应用模板的内容编辑不能把不持有的高危权限发出去。
+    // M037-001：两道闸各自锁住——不持有的码（无论是否须审批）被「不得授出自己不持有的权限」拦下；持有的高危码被「须审批」拦下。
     @Test void templateEditCannotGrantAnUnownedHighPermission() {
+        String daily = "ops.production_readiness.check";
+        var definitions = new ArrayList<>(repository.listPermissions());
+        definitions.add(new PermissionDefinition(daily, "检查", "ops", "medium", false, "active"));
+        when(repository.listPermissions()).thenReturn(definitions);
         actor("manager", "staff", "org", null);
         effect("manager", LOW, AccessRepository.Effect.GRANT, "own", false);
         PermissionTemplate t = service.createPermissionTemplate(template("create", LOW));
         assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), template("edit", HIGH)))
-                .isInstanceOf(ApiException.class);
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS"));
+        assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), template("edit-daily", LOW, daily)))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS"));
+        effect("manager", HIGH, AccessRepository.Effect.GRANT, "own-high", false);
+        assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), template("edit-owned-high", LOW, HIGH)))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL"));
         assertThat(items.get(t.id())).extracting(PermissionTemplateItem::permissionCode).containsExactly(LOW);
     }
```

stat：1 file changed, 12 insertions(+), 2 deletions(-)。± 行合计 14。

### 2.2 发布变体 diff（origin/main → 发布全文；001＋002）

全文见 `/home/james/audit-tmp/m037_b3r1_publish.diff`（本席已通读）。结构：

- hunk 1：`@@ -1,6 +1,6 @@` 版本 V0.30→V0.31（与主工作区 hunk 1 逐字相同）
- hunk 2：`@@ -173,12 +173,22 @@` 即 001（与主工作区 hunk 2 **hunk 正文逐字节相同**）
- hunk 3：`@@ -582,6 +592,13 @@` 即 002① staff archive
- hunk 4：`@@ -590,10 +607,19 @@` 即 002③ 错误码 + 002② reject_delete 后再申请

stat：1 file changed, 29 insertions(+), 3 deletions(-)。± 行合计 32。

### 2.3 D-029：001 新增行两份 diff 逐字相同

本席把两份 diff 按 `@@` 切 hunk，取含 `templateEditCannotGrantAnUnownedHighPermission` 的那一块：

- plus 行 11 条、minus 行 1 条，两侧列表 `==`
- hunk 正文 `MAIN_H==PUB_H` 为 True（含上下文）
- 版本 hunk 两侧均为 `-// 文件版本：V0.30` / `+// 文件版本：V0.31`

001 的全部新增行（含注释、第三码夹具、三处错误码断言、持有高危的 `effect`）在两份 diff 里逐字相同。主工作区没有 002 hunk，符合「未发布版已有 002 三处断言、主工作区只落 001」。

发布全文 **无** `MASTER_ACCOUNT_TYPE`。origin/main `SessionIssuer.java` 仅 `MASTER_USER_ID` / `MASTER_USERNAME`，无该常量。发布变体主会话夹具为 `MASTER_USER_ID` + `MASTER_USERNAME` + `"platform_admin"`，与 origin/main 原夹具同形。

---

## 3. 逐块／逐行归因（闸 3）

归属「是」且已定稿的 ID 仅 M037-001、M037-002。归因不到即越界。

### 3.1 主工作区冻结 diff（14 条 ± 行，全部 → 001）

after 树行号。

| after 行 | 符号 | 内容（缩） | ID | 理由 |
|---|---|---|---|---|
| L3 | −/+ | `文件版本：V0.30` → `V0.31` | M037-001 | 本文件本轮只落 001；版本头随该 ID 加一 |
| L176 | + | `// M037-001：两道闸各自锁住…` | M037-001 | 本条测试的 ID 注释 |
| L178 | + | `String daily = "ops.production_readiness.check"` | M037-001 | B2 边界：本条测试内加 `requiresApproval=false` 且 manager 未持有的第三码 |
| L179 | + | `new ArrayList<>(repository.listPermissions())` | M037-001 | 让 `listPermissions`／`permissionExists` 认得第三码；不写进被测生产字典 |
| L180 | + | `PermissionDefinition(daily,…,false,"active")` | M037-001 | 第五参 `requiresApproval=false` |
| L181 | + | `when(repository.listPermissions()).thenReturn(definitions)` | M037-001 | 同上 |
| L186 | −/+ | `isInstanceOf(ApiException)` → `isInstanceOfSatisfying` 核 `GRANT_EXCEEDS_OWN_PERMISSIONS` | M037-001 | 原场景（不持有且高危 HIGH）核第一道闸 |
| L187–188 | + | `template("edit-daily", LOW, daily)` → `GRANT_EXCEEDS_OWN_PERMISSIONS` | M037-001 | 不持有、不须审批 → 同码 |
| L189 | + | `effect("manager", HIGH, GRANT, "own-high", false)` | M037-001 | B2：持有的高危须先 GRANT 再改模板 |
| L190–191 | + | `template("edit-owned-high", LOW, HIGH)` → `GRANT_REQUIRES_APPROVAL` | M037-001 | 持有的高危码核第二道闸 |

未改行 `containsExactly(LOW)` 覆盖三种情形后模板项均不变，本身不是 ± 行，不另占 ID。无空白空改、无第二文件、无 002 行。

**主工作区归因票：PASS。** 14/14 归因到 M037-001。

### 3.2 发布变体 diff（32 条 ± 行）

hunk 1+2（14 条）与 §3.1 同文，全部 → M037-001（发布侧该文件同时落 002，版本头由两 ID 共用；001 hunk 与主工作区相同，版本行仍是本文件随本轮写入加一，合法）。hunk 3+4 如下（发布全文行号）。

| 发布行 | 符号 | 内容（缩） | ID | 理由 |
|---|---|---|---|---|
| L595 | + | `// M037-002：非平台管理员不得操作…且不落任何写入` | M037-002① | ID 注释 |
| L596–599 | + | staff `archive` → `PLATFORM_ADMIN_REQUIRED` | M037-002① | 定稿① |
| L600 | + | `verify(repository, never()).savePositionTemplateLifecycle(any(), anyString())` | M037-002① | 定稿「未被调用」 |
| L601 | + | `actor("admin", "platform_admin", …)` | M037-002① | 恢复后续合法 archive 的主体；load-bearing |
| L610 | −/+ | `isInstanceOf(ApiException)` → 核 `MASTER_AUTHORITY_REQUIRED` | M037-002③ | 普通管理员批准删除 |
| L614 | + | `// M037-002：最高权限驳回删除…` | M037-002② | ID 注释 |
| L615 | + | `var masterAuthentication = …getAuthentication()` | M037-002② | 保存 origin 主会话，供 `actor()` 后再批准 |
| L616 | + | `reject_delete` | M037-002② | 定稿② 调用该分支 |
| L617 | + | `deletedAt()` isNull | M037-002② | 未软删 |
| L618 | + | `deleteReviewId()` isNull | M037-002② | 复核单号清空（SUT `request_delete` 要求其为空才能再申请） |
| L619 | + | `actor("admin", "platform_admin", …)` | M037-002② | 非主会话再申请，避免主会话 `request_delete` 走直接软删 |
| L620 | + | `request-again` / `request_delete` | M037-002② | 随后可再申请 |
| L621 | + | 恢复 `masterAuthentication` | M037-002② | 再批准仍用 origin 主会话夹具 |

发布变体主会话仍是 origin 已有的三行 `MASTER_USER_ID` + `MASTER_USERNAME` + `"platform_admin"`（上下文，非 ±）。未引入 `MASTER_ACCOUNT_TYPE`。未改 restore／self-archive／主会话直接删除等后续既有步骤。

**发布归因票：PASS。** 32/32 归因到 001 或 002。无越界行。

**闸 3 归因总结论：PASS。**

---

## 4. 内容审计（定稿最小处置 × 本席 B2-r1 边界）

### 4.1 被测代码与写入集合

- 写入集合＝计划 FILES 唯一文件 `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java`。
- 两 tree 之间 `backend/src/main/` 无 diff。发布 diff 不含被测源。
- origin/main `AccessService.updatePermissionTemplate` L174 `requireActorCanGrant(added)`、L175 `requireGrantApprovalIfNeeded(added)`；`changePositionTemplateLifecycleAudited` L500–502 `requirePlatformAdmin`（非主会话）、L538–555 `approve_delete`/`reject_delete` 同分支、L542–543 `MASTER_AUTHORITY_REQUIRED`、L553–554 驳回时 `deletedAt=null` 且 `deleteReviewId=null`、L559 才 `savePositionTemplateLifecycle`。本 diff 未改这些行。
- 主工作区未发布 AccessService 的 `updatePermissionTemplate` 两闸与 origin 同文同行；001 在两口径下打同一处测试补丁。

### 4.2 M037-001

B2 允许：只改 `templateEditCannotGrantAnUnownedHighPermission`；原场景核 `GRANT_EXCEEDS_OWN_PERMISSIONS`；补不持有、不须审批的码 → 同码且模板项不变；补持有的高危码 → `GRANT_REQUIRES_APPROVAL` 且模板项不变。两版同一处。禁止改 AccessService／只收紧原场景／把套用／建角色／员工授予／岗位路径写进本文件。

对照：

1. **原场景。** `manager`/`staff` 只 `effect` 了 `LOW`（`ops.production_readiness.view`，setUp 字典 `requiresApproval=false`），`update` 加 `HIGH`（`finance.refund.create`，`requiresApproval=true`）。`added`＝差集＝`HIGH`。第一道先抛 `GRANT_EXCEEDS_OWN_PERMISSIONS`（origin `requireActorCanGrant` L818；staff 非 wildcard）。现核该码。符合。
2. **第三码。** setUp 字典只有 LOW/HIGH。本条内加 `ops.production_readiness.check`，`PermissionDefinition` 第五参 `false`，经 `listPermissions` stub；`permissionExists` 的 setUp 答案读 `listPermissions()`，故 `requireKnownPermissions` 认得它。manager 未持有。`template("edit-daily", LOW, daily)` 的 `added`＝`daily`（LOW 已在模板里）。第一道同码。符合 B2「不得把第三码写进被测生产字典」。
3. **持有的高危。** `effect(manager, HIGH, GRANT)` 之后 `added`＝`HIGH`。第一道过（已持有）；`approvalRequiredPermissions` 过滤 `requiresApproval`，第二道 `GRANT_REQUIRES_APPROVAL`。符合。
4. **模板项。** 三处均在 `saveTemplate` 之前抛错；收尾 `containsExactly(LOW)` 锁三种情形。符合。
5. **未越界。** 无 `applyPermissionTemplate`／`createRole`／`changeUserPermissionOverrides`／岗位路径新测。符合 XM-M037-A 不在本文件补。

### 4.3 M037-002（只在发布口径）

B2 允许：只改发布口径 `positionTemplateLifecycle…`；① staff archive → `PLATFORM_ADMIN_REQUIRED` 且 `savePositionTemplateLifecycle` 未调用；② 申请后主会话 `reject_delete`，`deletedAt` 空、`deleteReviewId` 清空，随后可再申请并批准；③ 普通管理员批准删除核 `MASTER_AUTHORITY_REQUIRED`。夹具＝`MASTER_USER_ID`＋`"platform_admin"`。禁止引入 `MASTER_ACCOUNT_TYPE`、改 SUT、把未发布整段夹具打进 origin。

对照：

1. **①。** `actor("ordinary-staff","staff",…)`：origin `isPlatformAdminBridge` 只认 `platform_admin`；`isMasterAuthority` 还要 `MASTER_USER_ID`/`MASTER_USERNAME`。staff 两闸都不过，L500–502 抛 `PLATFORM_ADMIN_REQUIRED`，尚未进入 switch，L559 未到。`verify(never())` 在随后合法 archive 之前检查，Mockito 核的是该时刻之前的调用。然后 `actor("admin","platform_admin")` 才真正 archive。符合。
2. **③。** `StatusChangeRequest` 首参是幂等键不是主体；批准时 SecurityContext 仍是刚 `request_delete` 的 `admin`/`platform_admin`（非主会话）→ L542–543 `MASTER_AUTHORITY_REQUIRED`。符合「来批准的就是发起申请的同一个 admin」。
3. **②。** 先把 origin 已有主会话（`MASTER_USER_ID`+`MASTER_USERNAME`+`"platform_admin"`）存进 `masterAuthentication`，再 `reject_delete`。SUT L553–554 驳回：`deletedAt=null`、`deleteReviewId=null`。随后用 **admin** 再 `request_delete`（若用主会话会走 L530–531 直接软删，无法再「批准」）；再恢复主会话 `approve_delete`，`deletedAt` 非空。符合；`deleteReviewId` 清空不是加戏（L526 再申请前置）。
4. **未发布常量。** 发布 diff／发布全文 grep 无 `MASTER_ACCOUNT_TYPE`。before 树未发布版该测试 L602–603 已用该常量——本轮主工作区 diff 未改那些行，符合「主工作区不再改 002」。
5. **主工作区 002 已有等价断言。** before 树 L586–611 已有 staff→`PLATFORM_ADMIN_REQUIRED`、普通批准→`MASTER_AUTHORITY_REQUIRED`、`reject_delete`+清空+再申请+批准。缺 `verify(never())` 不构成本轮越界：定稿写明主工作区不再改 002；闸在 L500–502，抛错即未写入。

### 4.4 版本、模式、业务发明、能否失败

- 两份 diff 均 V0.30→V0.31（+1）。origin 与 before 均为 V0.30。
- 模式 100644 未变。
- 第三码是本条测试 stub，不是生产字典／新权限产品。注释只复述已有闸。无新业务规则、无 HTML、无锁、无迁移。
- **新断言能否失败**（B3 冻结日志，本席核 `m037_mutate.py` 替换片段与 origin SUT 唯一匹配，且失败信息与闸序一致；本席未重放变异以免改被测）：

| 变异 | 替换 | 红的测试 | 失败信息（冻结日志） | 本席对齐 |
|---|---|---|---|---|
| `template_edit_no_own_gate` | 删 `requireActorCanGrant(added);` | 仅 `templateEditCannotGrantAnUnownedHighPermission` | expected `GRANT_EXCEEDS_OWN_PERMISSIONS` but was `GRANT_REQUIRES_APPROVAL` | 原场景 HIGH 改撞第二道 |
| `template_edit_no_approval_gate` | 删 `requireGrantApprovalIfNeeded(added);` | 同上 | Expecting code to raise a throwable | 持有高危写入，第三景不抛 |
| `lifecycle_no_platform_admin` | 去掉 `requirePlatformAdmin(actor)` | 仅 `positionTemplateLifecycle…` | Expecting code to raise a throwable | staff archive 不被拦 |
| `lifecycle_no_master` | `isMasterAuthority` 判断改 `if (false)` | 同上 | expected `MASTER_AUTHORITY_REQUIRED` but was `SUPERVISOR_SELF_REVIEW_FORBIDDEN` | ③ 被不得自审顶替 |
| `reject_delete_as_approve` | `boolean approve = true;` | 同上 | expected null but was 时间戳 | 驳回被当成批准，`deletedAt` 非空 |

各变异 `tests=39 failures=1`，只红对应测试。BASE 与去变异 GREEN 均为 39/0。主工作区落 001 后冻结日志 39/0。

### 4.5 本席独立 GREEN

本类不连库。命令：`mvn -q -o test -Dtest='M006SecurityRegressionTests'`（backend 目录）。

| 口径 | 环境 | 结果 |
|---|---|---|
| 主工作区 after（含未发布改动 + 001） | 正本 `backend/` | `MVN_EXIT=0`；surefire `tests=39 errors=0 skipped=0 failures=0` |
| 发布变体 | origin/main 一次性 worktree `/tmp/m037_b4_r1_pubwt` @ `40fcc8f1`，拷入发布全文（SHA `c459a5d2…`），`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64` | `PUB_MVN_EXIT=0`；surefire `tests=39 errors=0 skipped=0 failures=0`；随后 `git worktree remove --force`，目录已不存在 |

首次发布 GREEN 在默认 `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`（仅 JRE、无 javac）下 `release version 21 not supported`；改 Temurin JDK 后从零编译并通过。不是测试红。worktree 已删除。未 fetch／commit／push／merge／deploy。未对数据库执行写语句。

### 4.6 ponytail-review / grilling

ponytail-review：第三码夹具、`masterAuthentication` 暂存、staff 前后的 `actor()` 恢复，均为定稿 load-bearing。无可删抽象／新依赖／顺手重构。`Lean already. Ship.` `net: -0`。

grilling 自问（不等人）：

1. `verify(never())` 会不会被后续合法 archive 打假绿？不会：verify 发生在 archive 之前，只看当时调用史。
2. `ordinary-admin` 是不是换了主体？不是：那是幂等键；主体仍是申请删除的 `admin`。
3. 再申请若用主会话？会直接软删（L530–531），「再批准」无法表达——故切回 admin 是必要，不是发明。
4. 日常码会不会其实须审批？setUp 副本上该条 `requiresApproval=false`；HIGH 仍在列表里保持 `true`。
5. 主工作区漏 002 是否越界？否：before 树已有三处等价断言；定稿禁止主工作区再改 002。

**内容票：PASS。**

---

## 5. 最终票

| 轴 | 票 |
|---|---|
| 内容（最小处置、B2 边界、能失败、无发明、不改被测、版本+1、模式未变、D-029 两份 001 同文） | **PASS** |
| 归因（闸 3：每一 ± 行 → 已定稿且归属「是」的 001 或 002） | **PASS** |
| 总结论（任一 FAIL 即 FAIL） | **PASS** |

本票不是模块 PASS，不是授权跳过 B5。M037 仍 IN_PROGRESS；B3 累计 1。冻结判据未改。无 HTML 申请、无 questions、无添加项、不 BLOCKED-ASK。

未读取 `M037-b4-r1-self-raw.md`。本 raw 落盘后才允许互读。
