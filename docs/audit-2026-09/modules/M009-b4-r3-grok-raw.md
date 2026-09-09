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
