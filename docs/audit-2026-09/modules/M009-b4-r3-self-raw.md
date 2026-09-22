CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B4-r3 修复内容复审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B4-r3（B3 第 3 次整批修复的完整 diff 复审）。2026-09-09T09:46:45Z
- before tree：`dcd5101ccb38e15a85b7e7382558611044cecdba`；after tree：`8c092c00b92c87997de2f22dfe56a09c7e6e17cd`；`git diff --binary dcd5101ccb38 8c092c00b92c` 全文 4472 bytes SHA256 eecbbb6c51d31cc37ead7c2d30e1692cf5108226ad21f3187d5ea088f80001f7（全文附于本报告末尾）。
- 定稿队列（findings 第三批定稿）：036 共 1 项；本轮只修这 1 项及归因到各 ID 的最小行为测试。
- 锁四值：实算/sidecar/ledger/Grok 均 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。

## 1. diffstat

```
.../complex/platform/governance/PlatformService.java    | 17 +++++++++++++++--
 .../platform/governance/PlatformServiceTests.java       | 14 +++++++++++++-
 2 files changed, 28 insertions(+), 3 deletions(-)
```

## 2. 逐块归因（每一行改动都归到定稿 ID；「版本铁律」行随其所在文件的 ID）

| 文件 | 块 | 位置 | 块内标记 ID | 归因 |
|---|---|---|---|---|
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | M009-036 |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 2 | `@@ -60,6 +60,9 @@ public class PlatformService {` | M009-036 | M009-036 |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 3 | `@@ -497,7 +500,7 @@ public class PlatformService {` | — | M009-036 |
| backend/src/main/java/com/complex/platform/governance/PlatformService.java | 4 | `@@ -681,6 +684,16 @@ public class PlatformService {` | M009-036 | M009-036 |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 1 | `@@ -1,5 +1,5 @@` | 版本铁律（随所属ID） | M009-036 |
| backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java | 2 | `@@ -256,6 +256,18 @@ class PlatformServiceTests {` | M009-036 | M009-036 |

## 3. 红绿证据（B3-r2 运行记录 /tmp/m009_b3r3_run.log 原文）

```
START=2026-09-09T09:35:02Z
BEFORE_TREE=dcd5101ccb38e15a85b7e7382558611044cecdba
=== PHASE RED: tests for 036 ===
PATCHED test/java/com/complex/platform/governance/PlatformServiceTests.java
BUMPED test/java/com/complex/platform/governance/PlatformServiceTests.java V0.03
RED_DONE ['036']
RED_EXIT=1
[ERROR] Tests run: 13, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 3.116 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.residualMigrationCreatesBindingContractTablesAndSeedsPermissions -- Time elapsed: 0.017 s <<< FAILURE!
[ERROR] Tests run: 31, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.097 s <<< FAILURE! -- in com.complex.platform.governance.PlatformServiceTests
[ERROR] com.complex.platform.governance.PlatformServiceTests.createSpaceRejectsStatusOutsideCheckSet -- Time elapsed: 0.002 s <<< FAILURE!
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.708 s -- in com.complex.platform.governance.PlatformControllerTests
[ERROR] Tests run: 54, Failures: 2, Errors: 0, Skipped: 0
=== PHASE GREEN: fixes for 036 ===
PATCHED main/java/com/complex/platform/governance/PlatformService.java
BUMPED main/java/com/complex/platform/governance/PlatformService.java V0.03
GREEN_DONE ['036']
GREEN_EXIT=1
[ERROR] Tests run: 13, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 3.101 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.residualMigrationCreatesBindingContractTablesAndSeedsPermissions -- Time elapsed: 0.008 s <<< FAILURE!
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.120 s -- in com.complex.platform.governance.PlatformServiceTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.088 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.811 s -- in com.complex.platform.governance.PlatformControllerTests
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.086 s -- in com.complex.platform.leasing.LeasingServiceTests
[ERROR] Tests run: 4, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.039 s <<< FAILURE! -- in com.complex.platform.leasing.LeasingJdbcIntegrationTests
[ERROR] com.complex.platform.leasing.LeasingJdbcIntegrationTests.migrationCreatesLeasingTablesAndSeedsPermissions -- Time elapsed: 0.005 s <<< FAILURE!
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.098 s -- in com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests
[ERROR] Tests run: 77, Failures: 2, Errors: 0, Skipped: 0
AFTER_TREE=8c092c00b92c87997de2f22dfe56a09c7e6e17cd
END=2026-09-09T09:35:29Z
```

## 4. 本席复审结论

见文末追加的判定段。

## 附：完整 diff（before→after，--binary）

```diff
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

```

## 4. 本席复审结论（席甲，作者自审票）

本席逐块读完上述 diff 全文（2 文件，28+/3-，6 hunk），并以 §2 归因表核对每一行。

### 4.1 修复内容（逐 ID）

| ID | 修复内容 | 证据 | 本席票 |
|---|---|---|---|
| 036 | `SPACE_STATUSES = Set.of("active","unavailable")`（≡ 已发布 V203 `chk_spaces_status` 二值）；`requireSpaceStatus`（缺省 active，否则 400 中文 `VALIDATION_FAILED`「可用/不可用」，未引入未映射 BEQR-170 六态用词）只替换 `createSpace` 一处 `trimToDefault`；`createSpaceType`/029 字典部分、V203、`GlobalExceptionHandler`、`saveSpace` 仓储均未动 | `createSpaceRejectsStatusOutsideCheckSet` 红（"Expecting code to raise a throwable"）→ 绿；既有 30 条 ServiceTests 仍绿；governance+leasing 77 条 = 75 通过 + 2 条 017 环境红 | PASS |

### 4.2 范围归因（§5 闸 3）

- 6 hunk 全部落到定稿 036（常量、调用替换、私有校验方法、一条最小测试）；2 个「文件版本：V0.02→V0.03」hunk 按版本铁律随所属 ID。
- 未改：三份业务 HTML、`plan/*`、锁、sidecar、ledger 基准、findings、权限种子、迁移、全局异常处理、037/029–035 与已修 18 项对应内容、其它模块。after tree 再次实取相同。
- 未提交/推送/合并/部署。

### 4.3 环境红（不由本 diff 引入）

`PlatformJdbcIntegrationTests.residualMigration…` 与 `LeasingJdbcIntegrationTests.migrationCreates…`（399≠409）：M009-017 环境项。

### 4.4 结论

内容修复：**PASS**。范围归因：**PASS**。总：**PASS**（作者自审票；须 Grok B4-r3 亦 PASS 才进 B5-r3；B3 已达上限 3）。本席在本报告落盘前未读取 `M009-b4-r3-grok-raw.md`。


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B4-r3 Codex 完整修复差异复核
版本：V0.02
内容审计：PASS
归因审计：PASS
本席总体：PASS
模块生命周期IN_PROGRESS；B3已执行3/3。本票仅B4，不能代替B5或Grok票。Grok83937已先派，Codex独立读取本轮完整1文件1块和直接行为依据，封存本票前未读取本次Grok raw或原始JSON。

## 输入和冻结
B2第五批对findings V2.12双PASS，定稿V2.13当前24行仅057是，23否保持。用户非HTML修复授权收到；旧054-056先改后确认历史保留，不改其时间线。本轮唯一修复057。
before=aed0f2d9c35c0ffdecf7bbd8e2341d27d5cd1f3d
after=39b7324829f44339c8c2afb4d2800dfce674ed2c
完整binary diff=859bytes，SHA256=2c7445ea8b5b36d3c7bb868379f7986ef5b61c850771203906d7698b5880fb3a
真实index SHA256 1657ef91568fc6c00e7fd66f58f4c00461260651323d2ecbf616238602b973a2。
HEAD e456ba96c84185c8d39f18f548011daac48a6466；当前16行SpaceTypeRequest与after同字节，其余代码未改。三HTML前后同字节。未提交/推送/部署。
B3前后四值均b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a；第四来源B5-r2-grok，LOCK-AMEND-COUNT=1。B3计数在取得after后才从2增3。

## 全部差异逐块归因
|块|文件|范围|正式ID|归因|
|---|---|---|---|---|
|1|backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java|@@ -1,8 +1,9 @@|M009-057|新增//文件版本V0.01满足用户版本铁律；删除旧“录入/修改、存在则更新”注释，替为新建、编码不得重复、固定启用。没有其它改动。|
归因覆盖整个diff，不按路径过滤。没有归到NO或无ID；没有测试、字段、调用者、业务权限、HTML改动。无需回滚。
精确改后注释：/** 空间类型字典新建请求（后台管理员维护）。typeCode 不得重复，新建状态固定为启用。 */

## 三线与验证
第一性原理：旧注释指导调用者把POST当更新；第一次提交已有编码即拒绝。删错误承诺、对齐已有入口即可，不新增更新能力。空间字典有BSM-004/PGM-005上位，归细化+工程缺陷；冻结GEN-002/008/016，不增判据。
顺从式：PlatformService551-558仍拒绝已有typeCode并把新建status设active；改后注释逐项一致。SpaceTypeRequest完整16行的字段和record签名原样；旧自身无版本，新添V0.01符合已定稿边界。
对抗式：尝试将修改理解为放宽重复编码或允许请求disabled——去注释后旧/新非空token序列逐一全等，无法产生新运行分支。已有两个测试分别验证重复编码抛SPACE_TYPE_CODE_EXISTS、请求disabled仍返回active，正文与现实现匹配；它们已在本席283/Grok239套件实际通过，本轮不冒称重新运行、不为注释加重复测试。
可复现验证：Python从git show before:path与当前源移除块/行注释后，re.findall非空token序列相等；assert True。git diff --check before after exit0。当前文件SHA256 7c3a5fa28ab5a45b49d3fc325e39d1f25ddcbc38b86de3872ca86ce338b05f34。
初次执行全仓相对HEAD的git diff --check exit2，原因是旧审计raw中的literal diff尾空白；它不等于本批两树diff存在空白错误。没有修改/覆盖历史raw或借机格式化。已收窄到本次完整两tree检查，exit0。
沿用已读receiving-code-review、verification-before-completion与最小修复要求，核真实源码和token等价；未使用未核实工具，不派第三席。无新的技术/业务内容需登记，无待答问题/HTML申请。

## 本次全部问题
0项。内容PASS且逐块归因PASS；须等Grok同样PASS才可将057队列改否并进入B5。第3次本轮若最终FAIL按原三次上限处理，不自行加轮。

## 完整未过滤binary diff
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

