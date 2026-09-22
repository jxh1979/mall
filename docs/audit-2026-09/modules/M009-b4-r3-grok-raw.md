CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B4-r3 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r3
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M009-b4-r3-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r3 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：任何 `M009-b4-r3-self-raw.md`（当前亦不存在）、席甲本次 B4 raw。定稿队列以 findings.md V1.25 顶部「当前 M009 第三批 B2-r1 双PASS定稿，1项待修（B3-r3，最后一次）」为准，并与本席第三批 B2-r1（`M009-b5-r2-grok-raw.md` V0.02 追加段）最小修复边界对照。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、1 个定稿「是」ID（M009-036）是否按最小边界修好、每一行能否归因到该 ID、是否越界改 HTML/锁/sidecar/ledger 基准/权限种子/迁移/Handler/037/029–035/第一、二批已修 18 项与 013/017–025/其它模块业务逻辑。未新造判据/角色，未询问操作员。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger 顶行同值。criteria-lock 版本仍 V0.11。`LOCK-AMEND-COUNT: 1`。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M009 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段 |
| 阶段 | B4-r3 只读审计 B3 第3次整批修复的「修复内容」本身（最后一次） |
| 正式队列「是」（1） | M009-036 |
| 正式队列「否」 | 037（登记，外审维持否）；029–035（登记）；第一、二批已修 18 项（001–012、014–016、026–028）与 013/017–025 保持否 |
| B3 | 已执行 3 次（r1/r2 双 PASS 已在 before 树内），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `dcd5101ccb38e15a85b7e7382558611044cecdba` |
| after（对象类型 tree，可解析） | `8c092c00b92c87997de2f22dfe56a09c7e6e17cd` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary dcd5101ccb38e15a85b7e7382558611044cecdba 8c092c00b92c87997de2f22dfe56a09c7e6e17cd`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 4472 bytes；SHA256 `eecbbb6c51d31cc37ead7c2d30e1692cf5108226ad21f3187d5ea088f80001f7`；UTF-8；2 文件；28 插入 / 3 删除；6 hunk；name-status 全为 M |
| 与 `/tmp/m009_b3_r3.diff` | `cmp` 全等（本席自算为准，副本仅对照） |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | 各 7241 条，差集空；无新增/删除路径 |
| HEAD | `242f487233d028173cb9198eecd079f5d877405c` = `origin/main` |
| 工作区 2 文件 blob | 均等于 after（`fe67a293…` / `148e460e…`）；HEAD 两文件 blob 均不等于 before（before 已含 B3-r1/r2 双 PASS 内容，尚未提交） |
| findings.md | V1.25；工作区内容 SHA256 `c1b82f9141ec54636a8f9e32f13d45cc5a530fa4d9dfdbb7318e6c77248de9c5`（480680 字节）；两树 blob 均为 `600fc308ad627e4966a44155fb82187663565734`（本 diff 未改 findings） |
| ledger.md | 两树 blob 均为 `66b93a248b9c7c151e6ee0efa004b5f30f2be3c4`（本 diff 未改）；工作区另有记事 blob `205fda6f3098d5b370a61212c9460c3942dd0f85`，不在两树 diff 内 |
| 三 HTML 两树 blob | BSM `12334b13f3c27a815c9bbf9eea216403fb0d76d1` / PGM `c2588ca1c65a3f196b39a23f299bd56967b5652e` / BEQR `86a717930f9303d4f131ee2864478baa14450e6d`，完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |
| V203 / GlobalExceptionHandler | 两树 blob 相同（`a0a893b77a67effda8bc33c2e074563ad46bd8c5` / `07a7429a25df2761bc76777b65f9f11c578b1e90`） |
| 迁移 / 权限种子 | 两树 diff 无名 |

`git diff --stat --binary`：

```
 .../complex/platform/governance/PlatformService.java    | 17 +++++++++++++++--
 .../platform/governance/PlatformServiceTests.java       | 14 +++++++++++++-
 2 files changed, 28 insertions(+), 3 deletions(-)
```

`git diff --numstat`：15/2 PlatformService.java；13/1 PlatformServiceTests.java。

工作区相对 HEAD 另有既有父链脏文件（B3-r1/r2 修复、审计产物、工具等）。那些路径在 before/after 两树 blob 相同，不在本轮 diff 内，不按本轮修复内容审计。本席未改任何源文件，只新建本 raw。TDD 红复现仅短暂替换 `backend/target/classes/.../PlatformService.class`（随后立即还原，源 blob 仍等于 after）。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M009 的集合（本席点算 `M009 → […]` COUNT=87 unique=87）。本轮只用定稿 036 已引用的 ID：GEN-004、GEN-008、GEN-009。BEQR-170 不在 M009 映射 87 条，禁止用作入队或发明依据。不 LOCK-AMEND。

B3-r3 边界（与本席第三批 B2-r1 票及 findings V1.25 顶部一致）：只修 036 及归因的最小行为测试（TDD：先落可失败测试再修）；`SPACE_STATUSES`≡已发布 V203 二值 `active/unavailable`；`requireSpaceStatus` 中文 400 只译此二值（可用/不可用），只用于 `createSpace`；`createSpaceType` 与 029 字典部分不动；不得引入未映射 BEQR-170 六态；不改三份 HTML、判据、权限种子、迁移、全局异常处理、其它 ID 内容、其它模块业务逻辑；改动文件自身版本递增（V0.02→V0.03）。M011 `PlatformServiceTests` 的新增最小测试归因 036。037 维持否。

四分类（冲突→细化→技术性→添加）本轮不重开 036：B2 已定为技术性。本席核对落地是否仍停在该格：闭集来自已发布 CHECK 而非 HTML 新造；中文只译二值；无状态机、无租赁看板六态。仍是技术性，无业务发明。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary dcd5101ccb38e15a85b7e7382558611044cecdba 8c092c00b92c87997de2f22dfe56a09c7e6e17cd`

before tree = `dcd5101ccb38e15a85b7e7382558611044cecdba`
after tree = `8c092c00b92c87997de2f22dfe56a09c7e6e17cd`

字节 4472；SHA256 `eecbbb6c51d31cc37ead7c2d30e1692cf5108226ad21f3187d5ea088f80001f7`；UTF-8 完整原文如下。

````diff
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformService.java b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
index 453be3c5..fe67a293 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformService.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.02
+// 文件版本：V0.03
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -60,6 +60,9 @@ public class PlatformService {
     /** M009-005：PGM-018/019 主体角色——顶层平台 / 城市项目公司 / 域主管公司（业态主体）。 */
     public static final Set<String> BUSINESS_ENTITY_ROLES = Set.of("platform", "project_company", "management_company");
 
+    /** M009-036：空间状态闭集来自已发布迁移 V203 的 chk_spaces_status（active/unavailable），服务层前置校验以中文 400 安全失败。 */
+    public static final Set<String> SPACE_STATUSES = Set.of("active", "unavailable");
+
     private final PlatformRepository repository;
     private final AuditedOperationExecutor operations;
     private final AuditEventService auditEventService;
@@ -497,7 +500,7 @@ public class PlatformService {
                             trimToNull(request.floorNo()),
                             request.areaSquareMeter(),
                             request.polygonPoints() == null ? List.of() : request.polygonPoints(),
-                            trimToDefault(request.status(), "active"),
+                            requireSpaceStatus(request.status()),
                             now,
                             now
                     ));
@@ -681,6 +684,16 @@ public class PlatformService {
         return value;
     }
 
+    /** M009-036：空间状态白名单＝已发布 V203 chk_spaces_status 二值（active/unavailable），缺省 active；不引入其它状态。 */
+    private String requireSpaceStatus(String raw) {
+        String value = trimToDefault(raw, "active");
+        if (!SPACE_STATUSES.contains(value)) {
+            throw ApiException.badRequest("VALIDATION_FAILED",
+                    "空间状态必须为 可用/不可用 之一(active/unavailable)");
+        }
+        return value;
+    }
+
     /** M009-005：主体角色白名单（PGM-018/019）；非法值在服务层以中文 400 拒绝，不落到数据库 CHECK。 */
     private static String requireBusinessEntityRole(String raw) {
         String value = requireText(raw, "entity_role");
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
index 308166f8..148e460e 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.02
+// 文件版本：V0.03
 
 import com.complex.platform.access.AccessActor;
 import com.complex.platform.auth.SessionIssuer;
@@ -256,6 +256,18 @@ class PlatformServiceTests {
         assertThat(platformRepository.listBusinessEntityBindings()).isEmpty();
     }
 
+    // ---- M009-036：空间状态须在已发布 CHECK（V203：active/unavailable）内，非法值 400 而非落库 500 ----
+    @Test
+    void createSpaceRejectsStatusOutsideCheckSet() {
+        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                "sp-bad-status", "create", List.of(), null, null, null, "A", "shop", null, null, null, "zzz")))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
+        assertThat(platformRepository.listSpaces()).noneMatch(s -> "zzz".equals(s.status()));
+        assertThat(service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                "sp-unavailable", "create", List.of(), null, null, null, "B", "shop", null, null, null, "unavailable")).status())
+                .isEqualTo("unavailable");
+    }
+
     // ---- M009-008：生效绑定同步商户三个主体列（PGM-026「引用同一组绑定」） ----
     @Test
     void createBusinessEntityBindingSyncsMerchantEntityColumns() {
````

---

## 4. 修复内容审计（逐 ID）

### M009-036（队列「是」）— 修好；最懒可行；无新缺陷、无新业务发明

定稿处置：`SPACE_STATUSES`=V203 集合、`requireSpaceStatus`（缺省 `active`，否则 400 中文 `VALIDATION_FAILED`，与 004 的 `requireProjectStatus` 同法）用于 `createSpace`；ServiceTests 一条最小用例（TDD 红→绿）；不改迁移、不改全局异常处理；029 字典部分维持登记。

落地核对：

| 定稿项 | 本席核源 |
|---|---|
| 闭集 ≡ V203 `active/unavailable` | `SPACE_STATUSES = Set.of("active", "unavailable")`。本席 `psql -h 127.0.0.1 -d complex_test`：`chk_spaces_status` 实态 `CHECK (status = ANY (ARRAY['active','unavailable']))`；`INSERT … 'zzz'` → `violates check constraint "chk_spaces_status"`；ROLLBACK 后 `id=sp_b4r3_probe` 行数 0。V203 文件两树 blob 未改。 |
| 只替换 `createSpace` 的 `trimToDefault` | before L500：`trimToDefault(request.status(), "active")`；after L503：`requireSpaceStatus(request.status())`。`git grep requireSpaceStatus` 于 after 树仅两处：定义 + `createSpace` 调用。 |
| `createSpaceType` / 029 不动 | after 仍 L457 `trimToDefault(request.status(), "active")`；本 diff 无名 `createSpaceType`。 |
| 缺省仍 `active` | `requireSpaceStatus` 内继续 `trimToDefault(raw, "active")`（L813 空/空白回默认）。旧行为保留。 |
| 中文 400 只译二值 | `"空间状态必须为 可用/不可用 之一(active/unavailable)"`。`ApiException.badRequest` = HTTP 400 + `VALIDATION_FAILED`。括号内代码为内部标识符例外。 |
| 禁止 BEQR-170 六态 | 本 diff `grep` 无「空置/洽谈中/已租/即将到期/已退租/不可租/BEQR-170」。用「不可用」而非「不可租」，未把租赁看板六态写入常量或文案。 |
| 不改 Handler / 迁移 / 仓储 | 两树 blob 相同；无 `saveSpace` 改动；无 `updateSpace`。 |
| 文件版本 V0.02→V0.03 | 两文件均递增，符合操作员版本铁律。 |

无业务发明：集合等于已发布 CHECK；缺省仍 `active`；`unavailable` 仍合法。中文只陈述该二值。不是从 HTML 新造状态机，也不是把 BEQR-154/155 项目/商户六态套到 `spaces.status`。

最懒可行：常量 + 与 `requireProjectStatus` 同形的私有校验 + 一处调用替换 + M011 一条 ServiceTests。未加 JDBC、未改 Handler、未给字典发明闭集、未扩 037。B2 写「不必测 `unavailable` 仍接受」——本测例在同一方法内多了一条 `unavailable` 正路径断言，仍归 036、未扩文件/ID；不构成越界，记残留观察。

TDD（本席重跑，不只信 `/tmp/m009_b3r3_*.log`）：

- 红：before 源无 `requireSpaceStatus`。本席把 before `PlatformService.java` javac 到 `/tmp/m009_b4r3_red`，短暂覆盖 `target/classes/.../PlatformService.class`（源文件未改），`mvn -Dtest=PlatformServiceTests#createSpaceRejectsStatusOutsideCheckSet surefire:test` → Tests run: 1, Failures: 1，`Expecting code to raise a throwable.` 于 L262。随即还原 class，工作区源 blob 仍 `fe67a293…` / `148e460e…`。与派发红日志同一失败句。夹具 `shop` 为启用类型（`seedSpaceTypes` 含 shop），故红不是铺位类型校验误伤。
- 绿：`mvn -Dtest=PlatformServiceTests test` → Tests run: 31, Failures: 0, Errors: 0，BUILD SUCCESS。含 `createSpaceRejectsStatusOutsideCheckSet`。

工程缺陷已按定稿最小面关闭。票：修好。

### 队列「否」未顺手改

037 / 029–035 / 已修 18 项 / 013/017–025：本 diff 无对应行为行。`createSpaceType` 仍任意 status。`changeBusinessEntityBinding` 未入 diff。

---

## 5. §5 闸 3 逐行归因

6 hunk，每一行归 M009-036。

| # | 文件 | 行性质 | ID |
|---|---|---|---|
| 1 | PlatformService.java | `// 文件版本：V0.02`→`V0.03` | 036（该文件所属 ID；操作员版本铁律） |
| 2 | PlatformService.java | `SPACE_STATUSES` 注释 + `Set.of("active","unavailable")` + 空行 | 036 |
| 3 | PlatformService.java | `createSpace` 构造 `trimToDefault`→`requireSpaceStatus` | 036 |
| 4 | PlatformService.java | `requireSpaceStatus` 方法（含中文 400） | 036 |
| 5 | PlatformServiceTests.java | `// 文件版本：V0.02`→`V0.03` | 036（该文件所属 ID） |
| 6 | PlatformServiceTests.java | `createSpaceRejectsStatusOutsideCheckSet` 及其注释（验证性测试） | 036 |

无「顺手优化」「换测试框架」「补抽象」行。无归因到 037、029–035、013/017–025 或已修 18 项行为的行。任一行均可归到定稿「是」ID，闸 3 命中。

---

## 6. 越界检查

| 禁止项 | 本轮两树 diff |
|---|---|
| 三份业务 HTML | blob 不变 |
| 锁 / sidecar / ledger 基准 | 锁/sidecar/ledger 两树 blob 相同；工作区 ledger 另有记事，不在 diff 内 |
| 权限种子 | 无名 |
| 迁移文件 | 无名；V203 blob 不变；无 V410 |
| GlobalExceptionHandler | blob 不变 |
| 037 / 029 字典 / createSpaceType | 无；`createSpaceType` 仍 `trimToDefault` |
| 已修 18 项行为 / 013/017–025 | 无行为再改 |
| 其它模块业务逻辑 | 无。M011 `PlatformServiceTests` 仅 036 最小测试；未改 LeasingService、页面、Controller、Jdbc、Handler |
| 未映射 BEQR-170 六态 | 未写入常量或文案 |

文件数/增减与派发声明一致：2 文件、28+/3-、版本 V0.02→V0.03。

---

## 7. 复跑命令（本席）

只 export 三个 `COMPLEX_DATABASE_*`（从 `~/.complex/complex.env` 抽出这三键，库名改 `complex_test`），未 `source` 整个 env。

```
mvn -Dtest=PlatformServiceTests test
# Tests run: 31, Failures: 0, BUILD SUCCESS

mvn -Dtest=PlatformServiceTests,PlatformControllerTests,PlatformJdbcIntegrationTests,VenueBusinessHoursServiceTests,LeasingServiceTests,LeasingSignAtomicityJdbcIntegrationTests,LeasingJdbcIntegrationTests test
# Tests run: 77, Failures: 2, Errors: 0
# 仅 017 环境红：PlatformJdbcIntegrationTests.residualMigration… 与 LeasingJdbcIntegrationTests.migrationCreates…（expected 399 but was 409）
# PlatformServiceTests 31/0；Controller 10/0；VenueBusinessHours 6/0；LeasingService 11/0；SignAtomicity 2/0
```

只读 psql（BEGIN 探针 + ROLLBACK）：`current_database=complex_test`；`chk_spaces_status` 二值；`zzz` 违例；探针行 0。

未跑前端：本 diff 无页面文件。未 commit / push / PR / merge / 部署。未在主仓写源。未跑写模式 SQL。

---

## 8. 问题二分（回退统一规则）

有 ID 且仍在修复队列、本轮未修好：无。
无 ID 或不在修复队列的新缺陷：无。

残留观察（不构成 FAIL、不新开 ID）：

- 同一最小测例附带 `unavailable` 正路径断言。B2 写「不必测」；仍归 036，未扩范围。
- 测例断言 `VALIDATION_FAILED` 与未写入，未再断言中文「可用/不可用」正文。实现已含定稿中文；`badRequest` 即 400。不扩测。
- 测例未覆盖空/缺省→`active`。`trimToDefault` 旧行为未改。
- 017 环境红 399≠409 仍在，非本 diff。

对抗自问（grilling 收窄为自问自答，不问操作员）：非法值是否仍可能落到 500？服务层已拒，内存测红绿已证；JDBC CHECK 仍在作兜底，Handler 未改，与 005 同法。字典 `createSpaceType` 是否被顺手加上闭集？否。是否把「不可租」或六态写进文案？否。

---

## 9. 技能与缺口

已按收窄口径使用：`verification-before-completion`（绿/红必须本席实跑，不得只信 B3 日志）。B4 按提示词：不派第三席、不 `HEAD~1`、不修。`grilling` 已读 SKILL.md 全文，但该技能原文要求向用户发问并派子代理查事实——本轮按提示词收窄为自问自答、不派子代理、不问操作员，记口径差，不冒称完整 grilling 会话。未派 Arc audit 子代理。ponytail 只作「最懒可行」尺度，不落地代码。无 MCP。

---

## 10. 结论

修复内容：PASS（036 按定稿最小边界落地；TDD 红有 before 树对照且本席 overlay 复现、绿有本席复跑；017 环境红不是本轮引入）。
范围归因：PASS（2 文件 6 hunk 28+/3- 逐行可归到 036；无越界）。
总结论：PASS。

此票不是模块 PASS，也不是 B4 双 PASS。须席甲独立 B4 票且双 PASS 后才把 036 队列改否并进 B5。本席未改 findings 队列归属。B3 累计 3/3 为最后一次；本票 PASS 不等于第 3 轮后的模块收口。

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B4-r3 修复内容独立审计（Grok 席乙）

文件版本：V0.02
步骤：B4-r3（第五批 B3 第 3 次整批修复的「修复内容」本身）
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁，不提交推送部署）
本文件路径：docs/audit-2026-09/modules/M009-b4-r3-grok-raw.md
本文件自身版本：V0.02（路径原先存在 V0.01：历史第三批 B4-r3，正式队列当时为 M009-036。本次不覆盖该前缀，只在文末追加第五批完整票）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。上方 20299 字节 V0.01 前缀原样保留，不得当作本票。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮第五批 B4-r3 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报代替本轮完整审计。席甲票仍是作者自审票；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：任何 `M009-b4-r3-self-raw.md` 正文（目录可见同名文件，本席不打开）；未读席甲本次未封存稿。定稿队列以 findings.md V2.13 顶部「当前M009第五批B2定稿（唯一当前表；双PASS）」为准：24 行仅 M009-057 修复队列「是」；原 17 已修 + 6 登记共 23 否。本轮只审 057 的最小落地。054–056 先改后确认历史保留，不倒填事先批准。用户非 HTML 修复授权已收到；本 diff 未改三份业务 HTML。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、1 个定稿「是」ID（M009-057）是否按 V2.13 最小边界修好、每一行（含文件版本号）能否归因到该当前正式 YES ID、是否越界改 HTML/锁/sidecar/ledger 基准/权限种子/迁移/测试/DTO 字段与类/运行分支/其它 19 个计划文件/已修 17 项与 6 登记否。B4 只审完整 diff 及直接依赖，不全仓扫、不重跑全套；B5 整模块另做。未新造判据/角色，未询问操作员。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger 顶行同值。criteria-lock 版本仍 V0.11。`LOCK-AMEND-COUNT: 1`。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）；sidecar blob 均为 `41f575026b693af74d2a36e6faf86d260ef6f6ca`。

| 项 | 值 |
|---|---|
| 模块 | M009 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段 |
| 阶段 | B4-r3 只读审计 B3 第3次整批修复的「修复内容」本身（第五批；B3=3/3 已在 after 取得后计入，冻结树内 ledger 仍为 V3.68 / B3=2/3，见下） |
| 正式队列「是」（1） | **M009-057** |
| 正式队列「否」（23） | 038、039、040、041、031、042、043、044、045、046、030、047、018、013、054、055、056（17 已修）；048、049、050、051、052、053（6 登记） |
| before（对象类型 tree，可解析） | `aed0f2d9c35c0ffdecf7bbd8e2341d27d5cd1f3d` |
| after（对象类型 tree，可解析） | `39b7324829f44339c8c2afb4d2800dfce674ed2c` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary aed0f2d9c35c0ffdecf7bbd8e2341d27d5cd1f3d 39b7324829f44339c8c2afb4d2800dfce674ed2c`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 859 bytes；SHA256 `2c7445ea8b5b36d3c7bb868379f7986ef5b61c850771203906d7698b5880fb3a`；UTF-8；1 文件；2 插入 / 1 删除；1 hunk；name-status 为 M |
| 与派发期望 | 字节/SHA256/1 文件/1 hunk 均独算确认一致 |
| `git diff --check` | exit 0（无空白错误） |
| `git diff-tree -r --raw` | 仅 1 条：`f38cc5b2d3fb165a6937136f7969215d7d3b4a5c` → `61bde624e9493df104d561d767438286972c2947` `SpaceTypeRequest.java` |
| 两树路径清单 | 各 3761 条，差集空；无新增/删除路径 |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`（本批修复未提交；该路径 HEAD blob = before） |
| 工作区 SpaceTypeRequest | 内容 SHA256 `7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34`，与 after 全文逐字节相等 |
| 真实 index | `.git/index` SHA256 `1657ef91568fc6c00e7fd66f58f4c00461260651323d2ecbf616238602b973a2`（与台账所记 B3 前后真实 index 同值；本席只核当前值，B3 用临时 `GIT_INDEX_FILE`，主 index 不在本 diff 内） |
| findings.md | 工作区 = 两树内容 SHA256 `ad589ab194cbce5d4d926dc8adc36e1ac564101c0d9eb56ae72bdfc5bdacd2a8`（866628 字节，V2.13）；两树 blob 均为 `4663d8798298da8d019d7d258c2088985cac05da`（本 diff 未改 findings） |
| ledger.md | 两树 blob 均为 `4528e54b2311b91b034761e494ca722d29cfef8e`，内容 SHA256 `29c6142755d0c4c14128c7a12272179c2b8441e473e660eaa1f029fceff26499`，版本 **V3.68**（本 diff 未改）。工作区另有 V3.70 记事（B3 计次与 B4 派发），不在两树 diff 内，不按本轮修复内容审计 |
| 候选 V2.12 | 派发给出 SHA256 `c37083760956300222e0ca223e29cffc4523a37408677560c04643491efc5539`；现行磁盘已是 V2.13，本席不回放旧字节。B2 两票文件本席独算：Codex `M009-b1-self-raw.md` 116315 bytes / `db41437ea9bcd86bf9d29ed11e896ff7f6064c4a080330d7b8aa08eec057b69c`；本席 B2 `M009-b1-grok-raw.md` 239474 bytes / `f53b5784697bcb945d98d2681de9ca751c5ab9e3448b601217a39c3a12056a64` |
| 三 HTML 两树 git blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3`，完全相同 |
| 三 HTML 内容 SHA256（before=after=工作区） | BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`（133961）；BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`（304482）；PGM `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1`（103015） |
| 锁/sidecar 两树 blob | 相同（未改） |
| PlatformService / PlatformServiceTests / PlatformController | 两树 blob 相同（`b325adc09bd66d73505130138889b7a87f9bd211` / `394ae9d1f13d5d2229fe39093785d42186683a98` / `52d3f0d27191d6fd4cc923a1607a9263bc3eb7fa`） |
| 迁移 / 权限种子 | 两树 diff 无名 |

`git diff --stat --binary`：

```
 .../java/com/complex/platform/governance/dto/SpaceTypeRequest.java     | 3 ++-
 1 file changed, 2 insertions(+), 1 deletion(-)
```

`git diff --numstat`：`2	1	backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java`。

计划 `modules.md` V0.04 M009 FILES 20 条 / 冻结有效内容行 2476（切分时不改）。当前同 20 文件 `wc -l` 合计：**before 3368 / after=工作区 3369**（本 diff +1 行，计划行数不凑）。

本席未改任何源文件，只向本 raw 追加本次完整报告。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M009 的集合。本轮只用定稿 057 已引用的 ID：GEN-002、GEN-008、GEN-016、BSM-004、PGM-005（findings 冻结列另有 PGM-020，作相关治理上下文；本席不把本 diff 扩到项目档案字段）。不 LOCK-AMEND。

本席自读锁定义（节录，不新造）：

- GEN-002：声明的入口、主路径和错误路径须能构建或执行，并与其接口、断言和文档声称的行为一致。
- GEN-008：前端、后端、数据库、OpenAPI、迁移、DTO 和测试夹具对名称、类型、可空性、枚举及错误语义的约定须一致。
- GEN-016：文档中的路径、命令、链接、版本、状态和完成声明须可从当前纳入范围复现，不得把计划或旧证据写成当前事实。
- BSM-004：来源 `business-system-map-for-operations-v0.1.html:566`。现行 HTML 第 566 行仍是 `<h3>配置数据可以存档、也可以删除（删除要审批）</h3>`，与锁摘录命中。
- PGM-005：来源 `platform-governance-map-for-operations-v0.1.html:347`。现行 HTML 第 347 行仍是 `<h3>空间和点位</h3>`，与锁摘录命中。

B3-r3 / 057 边界（与本席第五批 B2 V0.10 及 findings V2.13 顶部一致）：只删除 Java 注释中「录入/修改」「存在则更新名称/排序/状态」的错误承诺，改为新建、编码不可重复、新建启用；原文件无自身版本则加 `V0.01`。不新增更新功能；不改 DTO 字段、类签名、接口行为、测试或三份 HTML；不强制改旧测试名或事件名。注释修改用当前实现与既有两条测试证据核对；不为注释增加重复测试。

四分类本轮不重开 057：B2 已定为细化。本席核对落地是否仍停在该格：改的是 DTO 文档声称，使之与已有 create-only 行为一致；无新状态机、无新更新入口、无 HTML 发明。仍是细化，无业务发明。

授权：057 改的是 Java 注释，不是三份 HTML。适用已收到的「如果不是修改html，那么修复没有问题」。无 2.2 添加项，无 HTML 申请。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary aed0f2d9c35c0ffdecf7bbd8e2341d27d5cd1f3d 39b7324829f44339c8c2afb4d2800dfce674ed2c`

before tree = `aed0f2d9c35c0ffdecf7bbd8e2341d27d5cd1f3d`
after tree = `39b7324829f44339c8c2afb4d2800dfce674ed2c`

字节 859；SHA256 `2c7445ea8b5b36d3c7bb868379f7986ef5b61c850771203906d7698b5880fb3a`；UTF-8；15 行（含末尾换行）；完整原文如下。

````diff
diff --git a/backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java b/backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java
index f38cc5b2..61bde624 100644
--- a/backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java
+++ b/backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java
@@ -1,8 +1,9 @@
 package com.complex.platform.governance.dto;
+// 文件版本：V0.01
 
 import java.util.List;
 
-/** 条目3 空间类型字典 录入/修改请求（后台管理员维护）。typeCode 为唯一键，存在则更新名称/排序/状态。 */
+/** 空间类型字典新建请求（后台管理员维护）。typeCode 不得重复，新建状态固定为启用。 */
 public record SpaceTypeRequest(
         String idempotencyKey,
         String operatorNote,
````

以上即为两树完整未过滤 binary diff 的全部字节，无省略、无第二文件、无后续 hunk。

---

## 4. 逐块 / 逐行归因（闸 3）

仅 1 文件 1 hunk。git 元数据行与上下文行随该文件出现，全部归 M009-057。

| 块 | 路径 | 行（hunk 内） | 变化 | 归属 |
|---|---|---|---|---|
| H1 | `.../dto/SpaceTypeRequest.java` | `package …dto;` | 上下文，未改 | M009-057（所在文件） |
| H1 | 同上 | `+// 文件版本：V0.01` | 新增自身版本 | M009-057（V2.13：原文件无版本，按版本铁律加 V0.01；格式与同目录 DTO `package` 后 `// 文件版本：V0.01` 惯例一致，抽检 `MerchantArchiveRequest` / `ProjectRequest`） |
| H1 | 同上 | `import java.util.List;` 及空行 | 上下文，未改 | M009-057（所在文件） |
| H1 | 同上 | `-/** 条目3 空间类型字典 录入/修改请求（后台管理员维护）。typeCode 为唯一键，存在则更新名称/排序/状态。 */` | 删除误导 javadoc | M009-057（与 findings 原句逐字相同） |
| H1 | 同上 | `+/** 空间类型字典新建请求（后台管理员维护）。typeCode 不得重复，新建状态固定为启用。 */` | 写入拟改 javadoc | M009-057（与 findings 拟改逐字相同） |
| H1 | 同上 | `public record SpaceTypeRequest(` / `idempotencyKey` / `operatorNote` | 上下文，未改；hunk 在默认 3 行下文结束 | M009-057（所在文件） |

字段 `typeCode` / `typeName` / `sortOrder` / `status`、record 签名、类名均未出现在 `+`/`-` 行。无第二块可归因。无归因落空行。

---

## 5. 修复内容核对（057 是否按最小边界落地）

### 5.1 注释与版本

after 全文（16 行 / 457 字节）：

```
package com.complex.platform.governance.dto;
// 文件版本：V0.01

import java.util.List;

/** 空间类型字典新建请求（后台管理员维护）。typeCode 不得重复，新建状态固定为启用。 */
public record SpaceTypeRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String typeCode,
        String typeName,
        Integer sortOrder,
        String status
) {
}
```

- 旧 javadoc 已不在 after 中。
- 新 javadoc 与 findings V2.13「拟改」逐字相同。
- before 无 `文件版本` 行；after 为 `V0.01`，不是从已有版本递增，符合「无自身版本→V0.01」。
- `String status` 字段保留（边界：不改 DTO 字段）。

### 5.2 去注释 token 一致性

剥离 `//…` 与 `/**…*/` 后，before/after 非空白 token 序列均为 24 项，逐项相等：

`package` `com.complex.platform.governance.dto;` `import` `java.util.List;` `public` `record` `SpaceTypeRequest(` `String` `idempotencyKey,` `String` `operatorNote,` `List<String>` `evidenceAttachmentIds,` `String` `typeCode,` `String` `typeName,` `Integer` `sortOrder,` `String` `status` `)` `{` `}`

无字段/类/方法 token 增减。

### 5.3 对应实现（两树 blob 相同，行号 551–558 仍成立）

`PlatformService.createSpaceType`（工作区 = after = before）：

```
551:                    if (existing.isPresent()) {
552:                        throw ApiException.conflict("SPACE_TYPE_CODE_EXISTS",
553:                                "该种类编码已存在，不能用新增改已有种类");
554:                    }
555:                    return repository.saveSpaceType(new SpaceTypeRecord(
556:                            "sptype_" + typeCode, typeCode, typeName,
557:                            request.sortOrder() == null ? 100 : request.sortOrder(),
558:                            "active", now, now));
```

拒绝重复编码；新建状态字面量 `"active"`，忽略 `request.status()`。无按编码更新名称/排序/状态的分支。与新注释一致，与旧注释相反。`executeWrite` 仍用事件名 `space_type.updated` / `space_type.update_failed`——057 明确不强制改事件名，本 diff 未改。

### 5.4 既有两条测试源（两树 blob 相同）

`PlatformServiceTests.java:374-387`：

- `createSpaceTypeRejectsExistingCodeEvenWhenActive`：对已有 `shop` 再 POST，断言 `SPACE_TYPE_CODE_EXISTS`。
- `createSpaceTypeIgnoresRequestedDisabledAndStaysActive`：请求 `status=disabled`，断言结果仍 `active`。

这两条即「拒重复 / 固定 active」的可失败断言。本 diff 未改测试文件。`:827` 方法名仍为 `createSpaceTypeUpsertsDictionaryAndEnablesItForSpaceValidation`，方法体只新建 `kiosk` 并用于 `createSpace`，不断言 upsert；不把该方法名扩进 057。

### 5.5 封存 283 / 239（本轮按派发不重跑全套）

类型 4 封存，不把历史套件写成「本席本轮新跑」：

- 席甲 B4-r2 / B5-r2 封存：11 类 283 项，0 failure / 0 error / 0 skip，`MAVEN_EXIT=0`；命令含 `PlatformServiceTests`（见 `M009-b4-r2-self-raw.md` 与 `M009-b5-r2-self-raw.md` 历史段、ledger 第四批 B4 记事）。本席读的是历史封存，**不是**本次 `M009-b4-r3-self-raw.md`。
- 本席 B5-r2 封存：10 类 239 项，0/0/0，`MAVEN_EXIT=0`；其中 `PlatformServiceTests` 69/0/0/0（`M009-b5-r2-grok-raw.md` V0.02 本次完整报告 §6）。该 69 例包含上述两条。
- 本 B3 未改 `PlatformService` / `PlatformServiceTests` blob，故封存时通过的这两条断言在 after 树上仍然是同一源。注释修复不要求新绿跑。

### 5.6 20 文件：仅 SpaceTypeRequest 在两树不同

| # | 路径 | before 行 | after/工作区 行 | 两树 blob |
|---|---|---:|---:|---|
| 1 | BusinessEntityRecord.java | 17 | 17 | 同 |
| 2 | BusinessEntityBindingRecord.java | 19 | 19 | 同 |
| 3 | MerchantRecord.java | 116 | 116 | 同 |
| 4 | MerchantDeleteDecision.java | 9 | 9 | 同 |
| 5 | JdbcPlatformRepository.java | 1338 | 1338 | 同 |
| 6 | PlatformController.java | 296 | 296 | 同 |
| 7 | PlatformRepository.java | 204 | 204 | 同 |
| 8 | PlatformService.java | 1173 | 1173 | 同 |
| 9 | package-info.java | 1 | 1 | 同 |
| 10 | BusinessEntityBindingRequest.java | 17 | 17 | 同 |
| 11 | BusinessEntityRequest.java | 26 | 26 | 同 |
| 12 | MerchantArchiveRequest.java | 6 | 6 | 同 |
| 13 | MerchantDeleteApprovalRequest.java | 5 | 5 | 同 |
| 14 | MerchantDeleteRequest.java | 5 | 5 | 同 |
| 15 | MerchantProfileRequest.java | 41 | 41 | 同 |
| 16 | ProjectRequest.java | 26 | 26 | 同 |
| 17 | SpaceRequest.java | 21 | 21 | 同 |
| 18 | **SpaceTypeRequest.java** | **15** | **16** | **不同（本 diff 唯一）** |
| 19 | StatusChangeRequest.java | 11 | 11 | 同 |
| 20 | VenueBusinessHoursRequest.java | 22 | 22 | 同 |
| | 合计 | 3368 | 3369 | |

工作区 19 个未改文件内容等于 after；SpaceTypeRequest 工作区等于 after。计划冻结 2476 未改。

### 5.7 未做（边界内禁止项，本席确认 diff 无名）

未改：DTO 字段/类签名；`createSpaceType` 分支/错误码/事件名；测试（含旧名）；三份业务 HTML；锁/sidecar；findings 队列列（057 在 B4 双 PASS 前仍为「是」，正确）；权限种子；迁移；其它模块生产代码。未实现「存在则更新」。未新增测试。

---

## 6. 对抗自问（grilling 收窄为自问自答，不问操作员、不派子代理）

1. 是否借注释顺手实现了 upsert？否。token 去注释后全等；`PlatformService` blob 未变。
2. 是否删掉 `status` 字段或改类签名？否。字段仍在；057 禁止改字段。
3. 新注释是否仍声称可修改/可更新？否。现句只有新建、不得重复、新建启用。
4. 是否把「条目3」删掉算越界？否。拟改原文即无「条目3」，落地与拟改逐字相同。
5. 版本写成 V0.02 是否错？否。before 无版本行，铁律是加入 V0.01。
6. 事件名 `space_type.updated` 未改是否 FAIL？否。B2/V2.13 明确不强制改事件名。
7. 旧测试名仍含 Upserts 是否 FAIL？否。同样明确不强制改。
8. 工作区 ledger V3.70 ≠ 冻结树 V3.68 是否越界？否。那是 after 树取得之后的 B3 计次/B4 派发记事，两树 ledger blob 相同，不在本 diff。
9. 是否改了三份 HTML？否。before=after=工作区内容 SHA 全等。
10. 是否需要本轮新跑 283/239？否。派发允许用封存证据；本 diff 无行为/测试变化。

---

## 7. 技能与缺口

已按 V0.17 收窄实际使用（只读，无第三席）：

| 技能 | 用法 | 缺口 |
|---|---|---|
| using-superpowers | 开工自检；用户/160 提示词优先于技能本体 | 本体「技能未读不得动手」与本派发「只写指定 raw」并存；已读适用节 |
| requesting-code-review | B4 请审 = 本席本票 | 本体 `HEAD~1`、派子代理 **作废** |
| receiving-code-review | 先核源与 diff 再接受或反驳，禁止表演性同意 | 本体「逐条 IMPLEMENT」**作废**（B4 禁止修） |
| verification-before-completion | PASS 前独算两树 binary diff、token、实现/测试源、HTML/20 文件 blob | 按派发 **不** 新跑全套 Maven；不以历史 283/239 冒充本轮新跑 |
| grilling | 对抗自问见 §6 | 本体「向用户发问 / 派子代理查事实」**作废** |
| ponytail | 最懒可行尺度：只改误导注释+补 V0.01 | 不落地代码 |
| test-driven-development | 纯注释，不强制新测 | 未新写（禁止） |
| graft | 派发已给精确路径；B4 对象是冻结 tree-id diff | **未调用** graft CLI（记缺口，不冒称已检索） |
| 子代理 / MCP / worktree / mattpocock 并行 code-review | 禁用 | 未用 |

无 MCP。无第三席。未询问操作员。

---

## 8. 问题二分（回退统一规则）

有 ID 且仍在修复队列、本轮未修好：无。
无 ID 或不在修复队列的新缺陷：无。

残留观察（不构成 FAIL、不新开 ID、不扩大 057）：

- `createSpaceType` 审计事件名仍为 `space_type.updated` / `space_type.update_failed`。
- `PlatformServiceTests:827` 方法名仍含 Upserts，方法体不是 upsert。
- DTO 仍有未被服务端读取的 `status` 入参；新注释已写明新建固定启用，未把该字段改成可写状态机。
- 本席 B5-r2「87/87 邻域命中 ≠ 69 业务片段全文同字节」与 BSM-114/BSM-120 观察仍不入队、不改 HTML/锁。

---

## 9. 结论

修复内容：PASS（057 按定稿最小边界落地：误导 javadoc 已换成拟改原文；无版本→V0.01；去注释 token 全等；`PlatformService` 551–558 拒重复/固定 active 未改且与新注释一致；既有两条测试源与封存 283/239 中的 `PlatformServiceTests` 通过记录仍覆盖该行为；未改字段/类/测试/HTML）。
范围归因：PASS（1 文件 1 hunk 2+/1- 逐行可归到 M009-057；无越界）。
总结论：PASS。

此票不是模块 PASS，也不是 B4 双 PASS。须席甲独立本次 B4 票且双 PASS 后才把 057 队列改否并进 B5。本席未改 findings 队列归属。B3 累计 3/3 为最后一次；本票 PASS 不等于第 3 轮后的模块收口。若本轮双 PASS 后 B5 仍 FAIL，按既有 `BLOCKED-3ROUNDS` 交操作员，本席不自行加轮。
