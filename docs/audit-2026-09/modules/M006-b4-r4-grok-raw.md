CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

版本：V0.01
# M006 B4-r4 Grok 修复内容审计原始报告

控制：V0.17；模块 M006 `10-backend-main-access-01`（6/322）；B3 已执行次数=4（操作员例外）；ledger=`IN_PROGRESS`。
本席角色：Grok（席乙）。只读复审本轮完整 tree-id diff，不修复、不改方案、不执行 B3/B5、不重开其他模块、不启用第三席。
席位：本轮「你（Codex）」职责由 Claude Fable 5.1 承担；本席禁令不变。
本次完整报告范围：本文件自本行 `CRITERIA-LOCK-SHA256:` 起至文末。
互盲：未读取 `docs/audit-2026-09/modules/M006-b4-r4-self-raw.md`（调用时不存在）及任何本轮席甲报告。

## 0. 技能预检（实际核读，非声称）

已实际读取并按合同收窄口径使用：

| 技能 | 路径 | 本轮用法 |
|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检；本席为派发的审计席乙 |
| requesting-code-review | 同套件 `requesting-code-review/SKILL.md` | B4 只由本席审；**不用**本体 `HEAD~1`、同会话子代理、无 ID 即刻修 |
| verification-before-completion | 同套件 `verification-before-completion/SKILL.md` | PASS 前本席在 `.scratch` 隔离实跑红绿对照；不以 findings 自述 34 PASS 当本席证据 |
| grilling | `/home/james/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只借追问结构；事实自读；**不**等操作员、**不**派子代理 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 读错误→复现→追数据流；不改生产代码、不加诊断桩 |
| diagnosing-bugs | mattpocock `engineering/diagnosing-bugs/SKILL.md` | 034 文案/测试红绿环 |
| tdd | mattpocock `engineering/tdd/SKILL.md` | 测缝须用户确认作废；本席只核已落测试是否能红、是否打在真实接缝 |
| test-driven-development | superpowers `test-driven-development/SKILL.md` | 对已落测试做红绿；B4 不写新测试 |
| ponytail-review | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删；结论见 §7 |
| writing-for-agents | mattpocock `productivity/writing-for-agents/SKILL.md` | 本 raw 给下游席阅读 |
| i-have-adhd | `/home/james/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 编号、先结论、无寒暄；**第一行被控制锁哈希格式覆盖** |
| knowledge-work code-review | `/home/james/.claude/plugins/cache/knowledge-work-plugins/engineering/1.2.0/skills/code-review/SKILL.md` | 正确性/错误路径；CONNECTORS.md 在插件根存在，技能旁路径不存在 |

缺口（不得冒充已调用）：

- `batch-grill-me`：未找到技能目录
- `ubiquitous-language`：未找到技能目录
- `git-guardrails-claude-code`：存在于 mattpocock `skills/misc/`，可选；本轮无 git 写操作，未调用
- knowledge-work `skills/code-review/CONNECTORS.md`：该相对路径不存在；插件根 `engineering/1.2.0/CONNECTORS.md` 存在

明确不用（合同划掉）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`ponytail-audit`、`ponytail-debt`、`research`、`code-review`（mattpocock）。未派第三席。
本轮未读取 trailofbits differential-review / audit-context-building / sentry code-review，不写「已调用」。

## 1. 冻结输入

- 本席实算 `docs/audit-2026-09/plan/criteria-lock.md` SHA256 = `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`
- sidecar `plan/criteria-lock.sha256` 同行；`ledger.md` 顶部同行；`LOCK-AMEND-COUNT: 0`
- 四值①②③本轮相等；④为本报告第一行，落盘后由席甲在两席 raw 齐后取。
- `findings.md` 工作区现值：V0.66；252320 字节；SHA256=`b105f7340ab329cffd63949c73abfdd4ee84a4f4a868b3df3688d4b2f38ad08a`
- 当前队列表唯一「是」：M006-034。001—033=否。本轮只允许归因 034（含操作员指令②对 StaffService 247/1244 的点名扩展）。
- after tree 内 `findings.md` 为 V0.65（034 候选段）；工作区 V0.66 为 B3 执行后台账，**不在**两树 diff 的 after blob 中。闸 3 认工作区 `findings.md` 现值。
- 拟修边界（现值）：AccessService 515/1061/1491/1694 只改人读报错；StaffService 247/1244 同类文案（操作员②）；既有 `M006SecurityRegressionTests` 补 `invalidDecisionMessageIsChinese`。不改内部枚举、错误码、权限判断、币种允许值、通用翻译器。
- before tree=`c77d4453db7eb9b92388de630fdd5121cdef7959`（type=tree）
- after tree=`54999203b0587456b8d2f473b5ee0f5e3d67908b`（type=tree）
- 未按路径过滤 diff、未改用 HEAD、未建第三 tree。
- 工作区另有大量相对 HEAD 的未提交修复（含 M006 前三轮源）。三份业务文件的工作区 SHA256 **等于 after blob**；B4 只审两树 diff，不以工作区其它脏文件当本轮改动。

适用冻结判据（模块映射内，本轮实际引用）：GEN-002、GEN-003、GEN-015、PGM-001、PGM-066。不扩映射、不启用锁外判据。

## 2. 两树完整 diff 逐字原文

命令：`git diff --binary c77d4453db7eb9b92388de630fdd5121cdef7959 54999203b0587456b8d2f473b5ee0f5e3d67908b`

实算：179689 字节；SHA256=`c390d6d126c6bd46abddd9635cca596c49d0da3b98f6e89701c03743ba009d38`；2075 行；`grep -c '^@@'` = 18。与派发声明一致。

`git ls-tree -r --name-only` 两树路径 `comm -3` 仅 4 条，全部为 `docs/audit-2026-09/modules/` 下本审计产物新增（b4-r3 / b5-r3 两席 raw）。业务路径无新增/删除，仅改内容。

路径与 numstat（+ / −）：

| # | 路径 | + | − | 归类 |
|---|---|---:|---:|---|
| 1 | `backend/src/main/java/com/complex/platform/access/AccessService.java` | 4 | 4 | 业务修复 |
| 2 | `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | 2 | 2 | 业务修复（操作员②） |
| 3 | `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` | 15 | 0 | 业务修复验证测试 |
| 4 | `docs/audit-2026-09/findings.md` | 61 | 4 | §1.2 排除 |
| 5 | `docs/audit-2026-09/ledger.md` | 69 | 2 | §1.2 排除 |
| 6 | `docs/audit-2026-09/modules/M006-b1-grok-raw.md` | 215 | 0 | §1.2 排除 |
| 7 | `docs/audit-2026-09/modules/M006-b1-self-raw.md` | 8 | 0 | §1.2 排除 |
| 8 | `docs/audit-2026-09/modules/M006-b4-r3-grok-raw.md` | 448 | 0 | §1.2 排除（新增） |
| 9 | `docs/audit-2026-09/modules/M006-b4-r3-self-raw.md` | 476 | 0 | §1.2 排除（新增） |
| 10 | `docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md` | 317 | 0 | §1.2 排除（新增） |
| 11 | `docs/audit-2026-09/modules/M006-b5-r3-self-raw.md` | 309 | 0 | §1.2 排除（新增） |

合计 11 路径，1924 insertions / 12 deletions。业务 3 文件；审计产物 8 文件。

下文为该命令全部未过滤输出（逐字）：

````
diff --git a/backend/src/main/java/com/complex/platform/access/AccessService.java b/backend/src/main/java/com/complex/platform/access/AccessService.java
index aded7caf..bc83e832 100644
--- a/backend/src/main/java/com/complex/platform/access/AccessService.java
+++ b/backend/src/main/java/com/complex/platform/access/AccessService.java
@@ -512,7 +512,7 @@ public class AccessService {
                         throw ApiException.conflict("INVALID_STATE", "模板没有待处理删除申请");
                     }
                     if (!authorizationService.isMasterAuthority(authorizationService.currentActor())) {
-                        throw ApiException.forbidden("MASTER_AUTHORITY_REQUIRED", "删除配置须由 USB 最高权限审批");
+                        throw ApiException.forbidden("MASTER_AUTHORITY_REQUIRED", "删除配置须由最高权限（U 盘密钥）审批");
                     }
                     boolean approve = "approve_delete".equals(action);
                     if (approve) requirePositionTemplateUnused(id);
@@ -1058,7 +1058,7 @@ public class AccessService {
                         String currency = trimToDefault(request.salaryCurrency(), "CNY");
                         if (!"CNY".equals(currency)) {
                             throw ApiException.badRequest("UNSUPPORTED_CURRENCY",
-                                    "薪资币种当前仅支持人民币（CNY），实际为：" + currency);
+                                    "薪资币种当前仅支持人民币，不支持所填币种");
                         }
                         if (request.baseSalaryAmountCent() != null && request.baseSalaryAmountCent() < 0) {
                             throw ApiException.badRequest("VALIDATION_FAILED", "基本工资不能为负数");
@@ -1488,7 +1488,7 @@ public class AccessService {
         String currency = trimToDefault(request.salaryCurrency(), "CNY");
         if (!"CNY".equals(currency)) {
             throw ApiException.badRequest("UNSUPPORTED_CURRENCY",
-                    "薪资币种当前仅支持人民币（CNY），实际为：" + currency);
+                    "薪资币种当前仅支持人民币，不支持所填币种");
         }
         if (request.baseSalaryAmountCent() != null && request.baseSalaryAmountCent() < 0) {
             throw ApiException.badRequest("VALIDATION_FAILED", "基本工资不能为负数");
@@ -1691,7 +1691,7 @@ public class AccessService {
         if (List.of("approve", "reject").contains(normalized)) {
             return normalized;
         }
-        throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approve）或驳回（reject）");
+        throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过或驳回");
     }
 
     private PositionRoleTemplate savePositionRoleTemplate(String id, PositionRoleTemplateRequest request) {
diff --git a/backend/src/main/java/com/complex/platform/access/staff/StaffService.java b/backend/src/main/java/com/complex/platform/access/staff/StaffService.java
index a5c9612c..4b601fb3 100644
--- a/backend/src/main/java/com/complex/platform/access/staff/StaffService.java
+++ b/backend/src/main/java/com/complex/platform/access/staff/StaffService.java
@@ -244,7 +244,7 @@ public class StaffService {
                             return saved;
                         }
                         throw ApiException.badRequest("VALIDATION_FAILED",
-                                "审批结论必须为通过（approve）或驳回（reject）");
+                                "审批结论必须为通过或驳回");
                     },
                     StaffProfileRecord.class
             );
@@ -1241,7 +1241,7 @@ public class StaffService {
             authorizationService.requirePermission("staff.shift.manage");
             if (request == null || request.decision() == null
                     || !java.util.Set.of("approve", "reject").contains(request.decision())) {
-                throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approve）或驳回（reject）");
+                throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过或驳回");
             }
             StaffLeaveRequestRecord current = repository.findLeaveById(id)
                     .orElseThrow(() -> ApiException.notFound("LEAVE_NOT_FOUND", "请假申请不存在或已被删除"));
diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
index 7b63bf74..b2570c54 100644
--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
@@ -642,4 +642,19 @@ class M006SecurityRegressionTests {
         assertThat(body.message()).contains("账号类型").doesNotContainPattern("[A-Za-z]");
     }
 
+
+    // M006-034：审批结论报错须为纯中文，不回显 approve/reject。
+
+    @Test void invalidDecisionMessageIsChinese() throws Exception {
+        var method = AccessService.class.getDeclaredMethod("normalizeDecision", String.class);
+        method.setAccessible(true);
+        ApiException failure = null;
+        try {
+            method.invoke(service, "maybe");
+        } catch(java.lang.reflect.InvocationTargetException e) { failure = (ApiException) e.getCause(); }
+        assertThat(failure).isNotNull();
+        assertThat(failure.code()).isEqualTo("VALIDATION_FAILED");
+        assertThat(failure.getMessage()).contains("审批结论").doesNotContainPattern("[A-Za-z]");
+    }
+
 }
diff --git a/docs/audit-2026-09/findings.md b/docs/audit-2026-09/findings.md
index 20ee0c2b..f692a759 100644
--- a/docs/audit-2026-09/findings.md
+++ b/docs/audit-2026-09/findings.md
@@ -1,7 +1,63 @@
 # 全量审计 Findings 台账
-版本：V0.62
+版本：V0.65
 
-## 当前状态：M001—M005已发布PASS；M006 本批B2已双PASS定稿，33ID/2待修，准备整批B3-r3
+## 当前状态：M001—M005已发布PASS；M006依一次性授权恢复B2，候选034待双PASS后交Claude CLI
+
+## M006 本次例外恢复及B2候选（V0.65）
+
+操作员本次明确指令：“仅限本次，你把经过3次修复后仍未通过的问题交给claude cli,调用fable 5.1 模型 high 模式看看能否修复”。
+仅本次授权解除M006的BLOCKED-3ROUNDS、把下一批修复执行者改为Claude CLI Fable 5.1/high；不改变控制提示词V0.17原文和后续模块默认角色/上限。保留真实B3累计3，实际新整批完成后记4，不抹掉旧三轮。新一批仍按B2整批定稿→委托修复→B4完整差异双审→B5全模块双审；未经后续验证不称模块PASS。没有重开M001—M005或全仓。
+已实测 /home/james/.local/bin/claude 2.1.261，内置模型ID claude-fable-5-1；以--model claude-fable-5-1 --effort high无工具连通检查真实exit0/is_error=false，返回modelUsage canonicalModel=claude-fable-5-1、provider=firstParty。没有使用默认opus/xhigh或fallback模型。先一次解析把JSON事件数组误当对象，CLI自身仍exit0；修正解析后完整读取以上实际结果，无文件改动。
+git fetch origin后HEAD=origin/main=395e6fbab027020da1c38237fad17e8edcd7a132。
+
+### M006-034 — 用户可见报错残留英文枚举与缩写
+- 整批输入：modules/M006-b5-r3-self-raw.md原V0.01独立票及接收追加V0.02；modules/M006-b5-r3-grok-raw.md完整V0.01。原票各1项，完整并集同类4处，无遗漏。
+- 候选：冲突；工程缺陷是；修复队列是。无添加业务、无HTML申请；旧001—033保持否。
+- 冻结判据PGM-001/PGM-066/GEN-002/GEN-015；依据为冻结全中文规则、真实当前代码和已落盘可运行错误响应探针。Grok4处完整范围保留，不缩成仅本方1694。
+- 来源位置：backend/src/main/java/com/complex/platform/access/AccessService.java:515、1061、1491、1694；错误最终通过GlobalExceptionHandler:49—61、FieldLabels:504—529传到message。
+- 拟修边界：该源文件4处人读报错文本、源文件版本V0.13→V0.14；在既有backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java补最小真实行为回归（V0.20→V0.21），覆盖4处错误出口及允许业务行为仍保持。需要的夹具可复用现有测试，不另加测试框架。
+- 参考最小文字：审批结论“审批结论必须为通过或驳回”；最高权限“删除配置须由持授权密钥的最高权限人员审批”；两处币种“薪资币种当前仅支持人民币”。最终只改显示文案，不改内部枚举、错误码、权限判断、币种允许值、账号/支付/存储逻辑或通用翻译器。
+- Claude仅为本次修复执行者，不能给自己代行Codex/Grok审计票；本次范围外内容发现后只报告。修复前B2仍待双PASS，不先行写源。
+
+
+## M006 B5-r3完整双席意见与三轮交付（V0.64）
+
+两席本次整个模块均审完后统一判定：**B5 FAIL；模块 BLOCKED-3ROUNDS；B3已执行3次**。
+Grok于2026-09-06 04:49:42 UTC检查取得真实EXIT_CODE=0，报告317行/21532字节，SHA06506fb252baac014c9ed4d1f89c546b92479e54aa84289255d6b78c3498b0bb，全文已读；交付成功不等于审计通过。
+本方独立原始完整报告34586字节，SHA72da471a14607a68d3c363a0a65c7ec426dfe7b2649da01da7f9b8f767f681a3，先于读取Grok封存。原票FAIL，原1条意见保留；后续接收记录仅追加，不覆盖原票。
+
+### 本批全部意见（终态待恢复；未做B2定稿）
+| 原始来源 | 问题及真实影响 | 位置 | 原分类/工程缺陷 | 当前处理 |
+|---|---|---|---|---|
+| self CX-B5-01 | 无效审批结论报错给用户显示approve/reject，实际Controller→Service→错误响应复现；不改变审批结果 | AccessService.java:1694，入口147—155/703—724 | 冲突/是，PGM-001/066、GEN-015，类型4 | 原始意见保留，待操作员恢复M006后整批B2 |
+| Grok GR-01（与上条同类，完整保留其更宽范围） | 同上；另有岗位模板删除审核拒绝提示USB，以及两处薪资币种错误提示CNY/输入币种 | AccessService.java:515、1061、1491、1694 | 冲突/是，PGM-001/066、GEN-002/015，类型1+4 | 原始意见保留，四处合为一类文案问题的归并建议，尚未正式ID |
+
+原始源：[本方完整报告](modules/M006-b5-r3-self-raw.md)、[Grok完整报告](modules/M006-b5-r3-grok-raw.md)。
+本方接收后核对四处当前源码及GlobalExceptionHandler:49—61/FieldLabels:504—529；中文中夹英文原样透传。Grok额外三处的运行探针证据归属Grok，本方不冒称重跑了其99项测试。
+旧M006-031与032在本轮两席各自测试中通过，保持旧33ID全部队列否；不得把这批新问题直接塞入已出队旧ID或无ID修复。没有遗漏另一类新意见、没有新增业务规则、没有HTML申请。
+潜在最小变更仅四处提示文本；保留内部代码、允许的decision/币种值、权限与业务判断。恢复后仍须先整批B2双PASS再修，不在本轮突破三次上限。
+验证：本方当前源98项回归全通过；独立真实审批响应反例1失败；Grok报告99项所选单元/JDBC测试通过及报错探针确认英文，明确省略2个StaffService测试（本方98项中已经实际执行），不把两组数字相加称197个不同测试。未跑真实支付厂商、全端浏览器/安卓、全库迁移或完整mvn test。
+
+### B5后四值与锁
+四值均为 `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`：①当前实算；②sidecar；③ledger顶部；④本次Grok raw最后一条独占哈希行。LOCK-AMEND-COUNT=0，校验实际命令/输出在本次操作员回执与本方raw接收追加段。
+没有因失败修改锁、scope、模块映射或当前源码，三HTML仍与HEAD逐字相同。
+
+### 三轮完整diff交付
+以下所有六份raw都含对应两树**完整未过滤**git diff --binary；本次逐字包含校验均true，不仅是stat或节选。源码仍等于第三轮after，未回滚已通过修复。
+| B3轮次 | before tree | after tree | 完整diff字节及SHA256 | 双席原始diff报告 |
+|---|---|---|---|---|
+| 1 | f49ef25cdadc0d4572f169d8c33b5e971c21f024 | e845764656fb7f9c166026371371b720f10fde2a | 384478 / f8caaf8d47ea2993a9ea1d4b8a43e0c4f47246618d327e1a5410428234f6f5cc | [self](modules/M006-b4-r1-self-raw.md) / [Grok](modules/M006-b4-r1-grok-raw.md) |
+| 2 | 7fecb8e739b6b04475755fffbb8e106fa0759713 | 2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9 | 17644 / cc2fc0f92106bd49386038da79b4b1e1b862cf447bc6bc753322417efd413a41 | [self](modules/M006-b4-r2-self-raw.md) / [Grok](modules/M006-b4-r2-grok-raw.md) |
+| 3 | 11319ccfc6c0a3b609bc1cfdd4579766a7118705 | c77d4453db7eb9b92388de630fdd5121cdef7959 | 14687 / a04dfea8324fd7a11d5b5cfebf3585fd93c9cb32ec3cd05fde2bb8b610956717 | [self](modules/M006-b4-r3-self-raw.md) / [Grok](modules/M006-b4-r3-grok-raw.md) |
+
+停止原因：控制提示词V0.17第4节“第3次B3后……该次B4或B5审完仍FAIL→……状态记BLOCKED-3ROUNDS……是否重启由操作员另行下指令”。当前B5完整结束、两席均FAIL，故不自动回B2/B3、不加第四轮、不开始M007、不提交合并推送M006。
+需要操作员：新指令恢复M006；恢复后保留现有修复/原始报告，先处理本批全部意见，而非重开M001或全仓。
+
+
+## M006 B4-r3整批出队（V0.63）
+2026-09-06 04:32:19 UTC取得Grok真实exit0，完整448行/30050字节/SHA945fa354e3a1c02ae01a553284187b30ec917460764cebe31e0b4217348604dd已读完。其内容PASS、范围PASS、无新finding；独立绿测33+9通过，并分别从before源码复现031公司/旧记录及032错误文案的3个失败。未把本方98测试和完整编译算作自己实跑。
+本方原B4 V0.01内容/范围PASS，原36492字节/SHAf65b1ae24f09b0b6238f9ae248fb0c759e9c87d4b82a076f400093d2e7e03553，现仅追加V0.02算术勘误（新键实际71字符而非报告误写67，代码/测试/原票未改）。双方raw所含完整diff均与两树实际14687字节逐字一致，6现源仍等于after。
+按B4双PASS规则统一将031/032当前队列改否；全33ID当前均否。不是模块收口，须继续B5-r3整模块三线双审。Grok关于长度前缀的可选缩短不属finding，不作无ID改动。
 
 ## M006 B2定稿（V0.62）
 2026-09-06 04:11:34 UTC取得Grok真实exit0；完整读取新增V0.06的218行/18285字节，SHA a8fce223cd34d693da0b4689d7190b6128118b628fe74af9f6fb89e26ab33c64。旧147529字节前缀未改；整份165814字节/SHA4e0d7aa205a52b2f1b84cf040b4080033e5a621fb4330b88e09b51f8e44025a4。本方既有本批B2 V0.15 PASS已先落盘；Grok本批B2 PASS，全部3条差集为空，两席共同确认候选V0.61的当前33ID表、分类及范围。
@@ -270,9 +326,10 @@ B1后四值复核：actual、sidecar、ledger基准、指定Grok B1最后哈希
 | M006-028 | GR-08 | 技术性 | 是 | 否 | 未知或UserDetails认证主体被填成staff/active |
 | M006-029 | GR-10 | 技术性 | 是 | 否 | 手工建号空密码成功返回但无登录凭证 |
 | M006-030 | GR-11 | 细化 | 是 | 否 | 列表声称每页50却返回全量；授权申请缺默认时间过滤 |
-| M006-031 | B5-r2 CX-B5-01 | 冲突 | 是 | 是 | 不同组织使用相同请求编号时创建角色结果串用 |
-| M006-032 | B5-r2 GR-01 | 冲突 | 是 | 是 | 非法账号类型错误提示夹带英文枚举 |
+| M006-031 | B5-r2 CX-B5-01 | 冲突 | 是 | 否 | 不同组织使用相同请求编号时创建角色结果串用 |
+| M006-032 | B5-r2 GR-01 | 冲突 | 是 | 否 | 非法账号类型错误提示夹带英文枚举 |
 | M006-033 | B5-r2 GR-02（原票保留，B2已一致） | 细化 | 否 | 否 | B3新增政策源码未跟踪的构建意见；当前必要全源可编译，Grok确认只登记 |
+| M006-034 | B5-r3 CX-B5-01 + GR-01完整并集 | 冲突 | 是 | 是 | 授权复核、最高权限和两处薪资币种报错残留英文；仅本次Claude修复候选 |
 
 ### 全部候选证据与拟修边界
 
diff --git a/docs/audit-2026-09/ledger.md b/docs/audit-2026-09/ledger.md
index 44e6ae0c..c2f5526d 100644
--- a/docs/audit-2026-09/ledger.md
+++ b/docs/audit-2026-09/ledger.md
@@ -1,7 +1,7 @@
 CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
 LOCK-AMEND-COUNT: 0
 
-版本：V1.21
+版本：V1.26
 
 | 模块 | 状态 | B3执行次数 | 方案复核已修次数 | PASS证据 | 子代理失败重跑次数 | 备注 |
 |---|---|---:|---:|---|---:|---|
@@ -10,7 +10,7 @@ LOCK-AMEND-COUNT: 0
 | M003 `01-root-config-and-guidance-01` | PASS | 2 | 2 | modules/M003-01-root-config-and-guidance-01.md；B5-r2双PASS/四值一致 | 0 | V0.17；8文件621行/18GEN；审计及发布PASS；PR #412已合并，160=origin/main=9f7955eb |
 | M004 `01-root-config-and-guidance-02` | PASS | 1 | 2 | modules/M004-01-root-config-and-guidance-02.md；B5-r1双PASS/四值一致 | 0 | V0.17；1文件3321行/220记录/18GEN；审计及发布PASS；PR #413已合并，160=origin/main=134d75d1 |
 | M005 `01-root-config-and-guidance-03` | PASS | 1 | 2 | modules/M005-01-root-config-and-guidance-03.md；B5-r1双PASS/四值一致 | 0 | V0.17；4文件232行/127脚本/18GEN；审计及发布PASS；PR #414已合并，160=origin/main=395e6fba |
-| M006 `10-backend-main-access-01` | IN_PROGRESS | 2 | 2 | - | 0 | V0.17；B2整批双PASS；33ID/2待修；准备B3-r3 |
+| M006 `10-backend-main-access-01` | IN_PROGRESS | 3 | 2 | - | 0 | 操作员仅本次授权Claude Fable5.1/high修复；恢复B2，034四处候选待双PASS |
 | M007 `10-backend-main-access-02` | PENDING | 0 | 2 | - | 0 | - |
 | M008 `10-backend-main-access-03` | PENDING | 0 | 2 | - | 0 | - |
 | M009 `10-backend-main-access-04` | PENDING | 0 | 2 | - | 0 | - |
@@ -1349,3 +1349,70 @@ B3完成次数仍2；生命周期IN_PROGRESS，尚未模块PASS/发布。B5先
 
 ### V1.21 M006 B5-r2回退整批B2定稿
 本批两席B2均PASS：self V0.15与grok V0.06；Grok真实exit0于04:11:34 UTC取得并读完本次218行，旧前缀校验通过。031/032是、033否，旧30项否。findings V0.62记录定稿及最小范围，B3仍2，第三次整批修复尚未结束，故不加次数。无待问、无HTML申请、锁不变。
+
+
+### V1.22 M006 B3-r3完成，进入完整diff复审
+本批已定稿031/032整批修复完成，before 11319ccfc6c0a3b609bc1cfdd4579766a7118705，after c77d4453db7eb9b92388de630fdd5121cdef7959。完整未过滤diff 6路径/12块/14687字节，SHA a04dfea8324fd7a11d5b5cfebf3585fd93c9cb32ec3cd05fde2bb8b610956717。此刻才将B3次数2→3；队列仍031/032是，须本轮B4双PASS后出队。真实失败测试95通过/3失败，修复后98通过/0失败，60当前源码fresh javac退出0；全后端mvn -o -q -f backend/pom.xml test-compile -DskipTests实际退出0。未迁移共享数据库。B3前后四值都等于全局基准，真实索引SHA824f33c20ee9b3c04744aadfede5f7bdbb2303a35b0035b1f8496ad729cd0322未改。6文件严格UTF8/无替换符检查通过，业务HTML等于HEAD，控制提示词仍V0.17相同SHA。B4仍未得双票，不宣称模块通过。
+
+
+### V1.23 M006 B4-r3本方报告已封存
+本方完整B4 raw为modules/M006-b4-r3-self-raw.md，V0.01/36492字节/SHA f65b1ae24f09b0b6238f9ae248fb0c759e9c87d4b82a076f400093d2e7e03553；内容PASS、范围PASS，含完整6路径12块14687字节diff及逐块031/032归因、红绿测试和完整编译证据。另有两种主体表示/商户/无认证系统的4个实际断言通过。外席04:16:26 UTC派发，按5分钟检查；截至04:26检查未取得真实退出码，不作任何Grok结论。未读其当前B4正文。B3仍3，队列仍031/032是，后续仍须本批B4双PASS及B5全模块双PASS。
+
+
+### V1.24 M006 B4-r3双PASS，进入全模块B5
+04:32:19 UTC取得Grok exit0，当前448行完整报告已读。self原V0.01内容/范围PASS，Grok V0.01内容/范围PASS；完整diff逐字一致。findings V0.63将031/032整批出队，全33ID当前否；无新增意见。self仅追加V0.02纠正说明中键长为71的算术错误，原票不改。B3保持3，模块保持IN_PROGRESS，尚未B5通过、未发布。
+
+
+### V1.25 M006 B5-r3完整双FAIL，轮次上限终态
+## M006 B5-r3完整双席意见与三轮交付（V0.64）
+
+两席本次整个模块均审完后统一判定：**B5 FAIL；模块 BLOCKED-3ROUNDS；B3已执行3次**。
+Grok于2026-09-06 04:49:42 UTC检查取得真实EXIT_CODE=0，报告317行/21532字节，SHA06506fb252baac014c9ed4d1f89c546b92479e54aa84289255d6b78c3498b0bb，全文已读；交付成功不等于审计通过。
+本方独立原始完整报告34586字节，SHA72da471a14607a68d3c363a0a65c7ec426dfe7b2649da01da7f9b8f767f681a3，先于读取Grok封存。原票FAIL，原1条意见保留；后续接收记录仅追加，不覆盖原票。
+
+### 本批全部意见（终态待恢复；未做B2定稿）
+| 原始来源 | 问题及真实影响 | 位置 | 原分类/工程缺陷 | 当前处理 |
+|---|---|---|---|---|
+| self CX-B5-01 | 无效审批结论报错给用户显示approve/reject，实际Controller→Service→错误响应复现；不改变审批结果 | AccessService.java:1694，入口147—155/703—724 | 冲突/是，PGM-001/066、GEN-015，类型4 | 原始意见保留，待操作员恢复M006后整批B2 |
+| Grok GR-01（与上条同类，完整保留其更宽范围） | 同上；另有岗位模板删除审核拒绝提示USB，以及两处薪资币种错误提示CNY/输入币种 | AccessService.java:515、1061、1491、1694 | 冲突/是，PGM-001/066、GEN-002/015，类型1+4 | 原始意见保留，四处合为一类文案问题的归并建议，尚未正式ID |
+
+原始源：[本方完整报告](modules/M006-b5-r3-self-raw.md)、[Grok完整报告](modules/M006-b5-r3-grok-raw.md)。
+本方接收后核对四处当前源码及GlobalExceptionHandler:49—61/FieldLabels:504—529；中文中夹英文原样透传。Grok额外三处的运行探针证据归属Grok，本方不冒称重跑了其99项测试。
+旧M006-031与032在本轮两席各自测试中通过，保持旧33ID全部队列否；不得把这批新问题直接塞入已出队旧ID或无ID修复。没有遗漏另一类新意见、没有新增业务规则、没有HTML申请。
+潜在最小变更仅四处提示文本；保留内部代码、允许的decision/币种值、权限与业务判断。恢复后仍须先整批B2双PASS再修，不在本轮突破三次上限。
+验证：本方当前源98项回归全通过；独立真实审批响应反例1失败；Grok报告99项所选单元/JDBC测试通过及报错探针确认英文，明确省略2个StaffService测试（本方98项中已经实际执行），不把两组数字相加称197个不同测试。未跑真实支付厂商、全端浏览器/安卓、全库迁移或完整mvn test。
+
+### B5后四值与锁
+四值均为 `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`：①当前实算；②sidecar；③ledger顶部；④本次Grok raw最后一条独占哈希行。LOCK-AMEND-COUNT=0，校验实际命令/输出在本次操作员回执与本方raw接收追加段。
+没有因失败修改锁、scope、模块映射或当前源码，三HTML仍与HEAD逐字相同。
+
+### 三轮完整diff交付
+以下所有六份raw都含对应两树**完整未过滤**git diff --binary；本次逐字包含校验均true，不仅是stat或节选。源码仍等于第三轮after，未回滚已通过修复。
+| B3轮次 | before tree | after tree | 完整diff字节及SHA256 | 双席原始diff报告 |
+|---|---|---|---|---|
+| 1 | f49ef25cdadc0d4572f169d8c33b5e971c21f024 | e845764656fb7f9c166026371371b720f10fde2a | 384478 / f8caaf8d47ea2993a9ea1d4b8a43e0c4f47246618d327e1a5410428234f6f5cc | [self](modules/M006-b4-r1-self-raw.md) / [Grok](modules/M006-b4-r1-grok-raw.md) |
+| 2 | 7fecb8e739b6b04475755fffbb8e106fa0759713 | 2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9 | 17644 / cc2fc0f92106bd49386038da79b4b1e1b862cf447bc6bc753322417efd413a41 | [self](modules/M006-b4-r2-self-raw.md) / [Grok](modules/M006-b4-r2-grok-raw.md) |
+| 3 | 11319ccfc6c0a3b609bc1cfdd4579766a7118705 | c77d4453db7eb9b92388de630fdd5121cdef7959 | 14687 / a04dfea8324fd7a11d5b5cfebf3585fd93c9cb32ec3cd05fde2bb8b610956717 | [self](modules/M006-b4-r3-self-raw.md) / [Grok](modules/M006-b4-r3-grok-raw.md) |
+
+停止原因：控制提示词V0.17第4节“第3次B3后……该次B4或B5审完仍FAIL→……状态记BLOCKED-3ROUNDS……是否重启由操作员另行下指令”。当前B5完整结束、两席均FAIL，故不自动回B2/B3、不加第四轮、不开始M007、不提交合并推送M006。
+需要操作员：新指令恢复M006；恢复后保留现有修复/原始报告，先处理本批全部意见，而非重开M001或全仓。
+
+
+
+### V1.26 操作员一次性例外恢复M006
+## M006 本次例外恢复及B2候选（V0.65）
+
+操作员本次明确指令：“仅限本次，你把经过3次修复后仍未通过的问题交给claude cli,调用fable 5.1 模型 high 模式看看能否修复”。
+仅本次授权解除M006的BLOCKED-3ROUNDS、把下一批修复执行者改为Claude CLI Fable 5.1/high；不改变控制提示词V0.17原文和后续模块默认角色/上限。保留真实B3累计3，实际新整批完成后记4，不抹掉旧三轮。新一批仍按B2整批定稿→委托修复→B4完整差异双审→B5全模块双审；未经后续验证不称模块PASS。没有重开M001—M005或全仓。
+已实测 /home/james/.local/bin/claude 2.1.261，内置模型ID claude-fable-5-1；以--model claude-fable-5-1 --effort high无工具连通检查真实exit0/is_error=false，返回modelUsage canonicalModel=claude-fable-5-1、provider=firstParty。没有使用默认opus/xhigh或fallback模型。先一次解析把JSON事件数组误当对象，CLI自身仍exit0；修正解析后完整读取以上实际结果，无文件改动。
+git fetch origin后HEAD=origin/main=395e6fbab027020da1c38237fad17e8edcd7a132。
+
+### M006-034 — 用户可见报错残留英文枚举与缩写
+- 整批输入：modules/M006-b5-r3-self-raw.md原V0.01独立票及接收追加V0.02；modules/M006-b5-r3-grok-raw.md完整V0.01。原票各1项，完整并集同类4处，无遗漏。
+- 候选：冲突；工程缺陷是；修复队列是。无添加业务、无HTML申请；旧001—033保持否。
+- 冻结判据PGM-001/PGM-066/GEN-002/GEN-015；依据为冻结全中文规则、真实当前代码和已落盘可运行错误响应探针。Grok4处完整范围保留，不缩成仅本方1694。
+- 来源位置：backend/src/main/java/com/complex/platform/access/AccessService.java:515、1061、1491、1694；错误最终通过GlobalExceptionHandler:49—61、FieldLabels:504—529传到message。
+- 拟修边界：该源文件4处人读报错文本、源文件版本V0.13→V0.14；在既有backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java补最小真实行为回归（V0.20→V0.21），覆盖4处错误出口及允许业务行为仍保持。需要的夹具可复用现有测试，不另加测试框架。
+- 参考最小文字：审批结论“审批结论必须为通过或驳回”；最高权限“删除配置须由持授权密钥的最高权限人员审批”；两处币种“薪资币种当前仅支持人民币”。最终只改显示文案，不改内部枚举、错误码、权限判断、币种允许值、账号/支付/存储逻辑或通用翻译器。
+- Claude仅为本次修复执行者，不能给自己代行Codex/Grok审计票；本次范围外内容发现后只报告。修复前B2仍待双PASS，不先行写源。
+
diff --git a/docs/audit-2026-09/modules/M006-b1-grok-raw.md b/docs/audit-2026-09/modules/M006-b1-grok-raw.md
index d17fa39a..e26d7de7 100644
--- a/docs/audit-2026-09/modules/M006-b1-grok-raw.md
+++ b/docs/audit-2026-09/modules/M006-b1-grok-raw.md
@@ -2082,3 +2082,218 @@ sha256sum findings / 两份 B5 raw / 本文件追加前
 - 全批 3 条意见归并正确：031 冲突/是/是，032 冲突/是/是，033 细化/否/否。旧 30 项队列否不变。
 - 033 独立裁定：接受候选。BSM-001 上位故细化；B3 合法新源在 after tree、当前 45 源去缓存可编过；不扩冻结 scope；只登记、发布时跟踪提交。
 - 0 添加、待答 0。不存在有条件通过。本票只定本批 B2 候选复核；不进入 B3。
+
+# M006 B2 整批候选复核（Grok 席）
+- 版本：V0.07
+- 角色：审计席乙（独立；第一性原理 / 对抗式 + 顺从式）
+- 模块：M006 `10-backend-main-access-01`（6/322）
+- 步骤：B2（由 B5-r3 完整双 FAIL 回退后的整批归并复核）；不执行 B3/B4/B5，不重开其他模块，不修复源代码
+- 控制：V0.17；ledger `IN_PROGRESS`；B3 累计=3；planrepair=2；runtimefailure=0
+- 操作员一次性例外（原话）：「仅限本次，你把经过3次修复后仍未通过的问题交给claude cli,调用fable 5.1 模型 high 模式看看能否修复」。本席只把该句理解为：解除本次 M006 `BLOCKED-3ROUNDS`、允许 Codex 在本批 B2 双 PASS 后把修复执行交给 Claude CLI Fable 5.1 / high。不改控制文件，不授权本席修复，不把后续模块默认角色/上限扩成 Claude。
+- 本席只读追加本文件；不修代码、不改方案、不改 HTML/锁/台账/问题、不创建其他产物或测试文件、不共享 Maven 编译、不派第三席
+- 不以 Codex 已落盘的 B2 V0.16 为据；本票只由 B5-r3 两席本次完整报告 + 候选必要全文 + 四处源码/HTML 原文独立给出
+- 全批结论：**PASS**
+
+本次 B2 按派发不写 `CRITERIA-LOCK-SHA256:` 独占行。正文叙述本席实算锁哈希（见 §1），不得被闸 1 误取为本步④。
+
+## 0. 技能预检（本节点实核）
+
+按意图执行：B2 只用结构，不修代码、不派子代理、不等操作员。grilling 的「等用户回答 / 派子代理查事实」作废，自问自答并自己读源码。receiving-code-review 的 IMPLEMENT 作废。verification-before-completion：PASS 前实算指纹/源码/HTML。ponytail-review 只列可删不修。systematic-debugging 只借读错误→复现→追数据流，不加诊断桩。
+
+| 技能 | 本节点 | 缺口 |
+|---|---|---|
+| using-superpowers | 开工前自检 | 无 |
+| grilling | 只用追问结构；034 四处自问自答 | 不等操作员、不派子代理 |
+| verification-before-completion | PASS 前实算指纹、队列表、四处抛点、HTML 287 | 无 |
+| receiving-code-review | 先核实再接受或驳回候选；禁止表演性同意 | IMPLEMENT 作废 |
+| ponytail-review | §8 | 不修 |
+| diagnosing-bugs / systematic-debugging | 只借读源码→FieldLabels 透传 | 不插桩、不改码 |
+| codebase-design | 缺陷在人读 message 接缝，不在鉴权/币种判定 | 无 |
+| tdd / test-driven-development | B2 不落测试 | 不适用 |
+| resolving-merge-conflicts | 无合并 | 不适用 |
+| writing-for-agents | 本报告给调度席读 | 无 |
+| batch-grill-me / ubiquitous-language / i-have-adhd | 本席技能树不存在独立 SKILL.md | 记缺口，未冒充已调用 |
+| git-guardrails-claude-code | 未装 hook | 记缺口 |
+
+未核到的不写已核实。未派子代理。未启动其他模块全审。未扩大为 B5 重审。
+
+## 1. 输入指纹（本席实算，与派发声明对照）
+
+| 路径 | 字节 | SHA256 | 对照 |
+|---|---:|---|---|
+| `docs/audit-2026-09/modules/M006-b5-r3-self-raw.md` 全文 | 40760 | `2640e139a0c4ce55f1c67a0a37cd574b855294fc363e14c139249b4469c9c8d1` | 含 V0.01 独立票 + V0.02 接收追加 |
+| 同上前 34586 字节（Codex B5-r3 独立票 V0.01） | 34586 | `72da471a14607a68d3c363a0a65c7ec426dfe7b2649da01da7f9b8f767f681a3` | 与 self 自述一致；本票差集用该独立票的 CX-B5-01，V0.02 只作接收记录 |
+| `docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md` 全文（本席 B5 V0.01） | 21532 | `06506fb252baac014c9ed4d1f89c546b92479e54aa84289255d6b78c3498b0bb` | 本次完整报告，无后续追加审计票 |
+| `docs/audit-2026-09/findings.md` V0.65 | 250382 | `0974cdae8f4cfc954a302f68773d08853e35fb63a3dc78b5bc55d866889fec7a` | 与派发一致 |
+| 本文件追加前 | 165814 | `4e0d7aa205a52b2f1b84cf040b4080033e5a621fb4330b88e09b51f8e44025a4` | 与派发「原 165814 字节前缀保留」一致 |
+
+本席独立实算 `docs/audit-2026-09/plan/criteria-lock.md` 全文哈希 = `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（sidecar `plan/criteria-lock.sha256` 与 `ledger.md` 顶部同值；`LOCK-AMEND-COUNT: 0`）。**本 B2 报告不新增该机读哈希独占行。** `git rev-parse HEAD` = `395e6fbab027020da1c38237fad17e8edcd7a132`，分支 `main`。
+
+本次输入 B5 raw 路径（固定）：
+- `docs/audit-2026-09/modules/M006-b5-r3-self-raw.md`
+- `docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md`
+
+候选版本：findings.md **V0.65** / SHA256 `0974cdae8f4cfc954a302f68773d08853e35fb63a3dc78b5bc55d866889fec7a`。
+
+`questions/M006-027.md` 的 `## 操作员答复` 段首个非空行精确为 `保留`。本批无新添加项、无新 2.2 文件。
+
+## 2. 差集方法
+
+B2 无互盲。全集：
+
+- Codex B5-r3 本次完整独立票（self-raw 前 34586 字节 / V0.01）临时编号 **CX-B5-01**（1 条）
+- 本席 B5-r3 本次完整报告（grok-raw 全文 V0.01）临时编号 **GR-01**（1 条）
+- 当前候选 `findings.md` V0.65：34 个连续唯一 ID；001—033 队列否；**仅 034 队列是**
+- 只审本批 2 条新意见的归并、分类、工程缺陷、队列与拟修边界；旧 33 项保留原票，不拼接历史票、不重开已出队项、不把台账写成「34 项新缺陷」
+
+映射（与候选表一致，本席逐项对照，不另编号）：**CX-B5-01 ∪ GR-01 → M006-034**。机检 34 个连续唯一 ID 各一次，无重复。无第三条 B5-r3 意见被漏并。
+
+## 3. 原始 2 意见到 034 的完整映射
+
+原票不覆写。下列是 B2 对候选归并的本席核实。
+
+| 原始意见 | 原分类 / 工程缺陷 | 原位置 | 冻结判据 / 依据类型 | 映射 | 并集处置 |
+|---|---|---|---|---|---|
+| Codex CX-B5-01 | 冲突 / 是 | `AccessService.java:1689—1694`（根因抛点）；同文件 703—724（`reviewGrant` 执行路径）；`AccessController.java:147—155`（HTTP 入口）；`GlobalExceptionHandler.java:49—61` 与 `FieldLabels.java:504—529`（出口透传） | PGM-001、PGM-066、GEN-015；类型 4 | **M006-034** | 1694 为人读报错源。入口与 `reviewGrant` 不是另三处缺陷，只说明 1694 如何到达 HTTP。候选来源位置未再抄 147—155/703—724，不构成漏并。 |
+| 本席 GR-01 | 冲突 / 是 | `AccessService.java:1694`（主证）；同文件 `:515`、`:1061`、`:1491`（同类出口） | PGM-001、PGM-066、GEN-002、GEN-015；类型 1+4 | **M006-034** | 四处全部进入 034。候选未把本席意见缩成仅 1694。 |
+
+并集四抛点（当前源码逐字，文件头仍为 V0.13；测试文件头仍为 V0.20；**源代码修复尚未开始**）：
+
+| 行 | 当前人读 message | 本席 B5 探针（已落盘，本票未重跑） |
+|---:|---|---|
+| 515 | `删除配置须由 USB 最高权限审批` | `HTTP_CODE=MASTER_AUTHORITY_REQUIRED … LATIN=true` |
+| 1061 | `薪资币种当前仅支持人民币（CNY），实际为：` + `currency` | 与 1491 同文案；探针 `薪资币种当前仅支持人民币（CNY），实际为：USD` / `LATIN=true` |
+| 1491 | 与 1061 同一字符串模板 | 同上 |
+| 1694 | `审批结论必须为通过（approve）或驳回（reject）` | `HTTP_CODE=VALIDATION_FAILED MSG=审批结论必须为通过（approve）或驳回（reject） LATIN=true`；Codex 真实 Controller→Handler 同文案，断言 `[A-Za-z]` 失败 |
+
+**不是 032 未修好、也不是重开 032。** 本席 B5 已实测 `INVALID_ACCOUNT_TYPE` → `账号类型不允许，请重新选择` / `LATIN=false`。032 定稿边界只针对账号类型分支，B4-r3 已出队。034 是全模块复审发现的同类残留，必须新 ID。
+
+**1686 `request_type is invalid` 不进 034。** 本席 B5 探针：`FieldLabels` 将其译为「申请类型格式不正确」，`HAS_LATIN=false`。两席本次意见均未把它列为缺陷；B2 不另发明。
+
+无漏并：2 条原意见全部进入 034。无改写本席 GR-01 四处范围。无把 CX-B5-01 的真实 HTTP 复现从证据链抹掉。无把旧 001—033 改回队列是。
+
+## 4. 四处精确修复边界
+
+候选拟修与本席核实后的 **当前可接受边界**（B3/Claude 归因只能落在这些行；越出即闸 3 越界）：
+
+1. **`AccessService.java:515`**
+   只改 `ApiException.forbidden("MASTER_AUTHORITY_REQUIRED", …)` 的**人读第二参数**。保留错误码、`isMasterAuthority` 判定、`approve_delete`/`reject_delete` 动作字面量、主管复核调用。参考最小文字：`删除配置须由持授权密钥的最高权限人员审批`。治理地图正文用语是「持授权 U 盘密钥的最高权限」；展示句不得再出现拉丁 `USB`。不得把最高权限改成职级 3 或日常管理员。
+
+2. **`AccessService.java:1061`**（`assignStaffLevel` 币种拒绝）
+   只改 `UNSUPPORTED_CURRENCY` 的人读第二参数。保留 `trimToDefault(..., "CNY")`、`if (!"CNY".equals(currency))`、金额分存储、错误码。参考最小文字：`薪资币种当前仅支持人民币`。不得把允许币种扩成 USD，不得在 message 里回显 `CNY`/`USD`/输入原值。
+
+3. **`AccessService.java:1491`**（岗位授予上下文币种拒绝）
+   与 1061 同一显示缺陷的第二抛点，必须**同样只改人读第二参数**，不得只修一处。内部允许值与错误码同上。
+
+4. **`AccessService.java:1694`**（`normalizeDecision`）
+   只改非法 `decision` 的人读第二参数。保留 `List.of("approve", "reject")` 内部取值、`VALIDATION_FAILED`、`reviewGrant` 权限与自审拒绝。参考最小文字：`审批结论必须为通过或驳回`。请求体字段名 `approve`/`reject` 可作为内部标识保留，**展示给人的 message 不能再列出英文取值**。
+
+**版本：** `AccessService.java` 文件头 V0.13→V0.14；既有 `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` V0.20→V0.21。测试须覆盖上述 4 个错误出口的真实 Handler 响应无拉丁字母，并断言允许的审批/最高权限/人民币路径仍保持。夹具复用现有测试，不另加测试框架。
+
+**明确不修：** 内部枚举与动作字面量；币种允许值 `CNY`；权限码与 `isMasterAuthority`；三份业务 HTML；`FieldLabels` / `GlobalExceptionHandler` 通用翻译器；账号/支付/存储逻辑；032 已修分支；001—033 已出队项。
+
+候选「来源位置」把 Handler:49—61 与 FieldLabels:504—529 写成透传路径，本席接受为**解释**而非修复文件。`FieldLabels.translate` 对已含 CJK 的句子原样返回（源码 528—529），故夹英文的中文句子必须改抛点，不能靠翻译器。
+
+## 5. grilling 自问（不等操作员）
+
+1. 能否把 034 缩成只修 1694？
+   不能。那是 CX-B5-01 的子集，会改写本席 GR-01。候选已取并集。
+2. 能否把 USB/CNY 辩成「系统内部标识符」留下？
+   不能。治理地图 287：「这四样之外……不得以“这是标识”为由保留英文。」USB/CNY/approve/reject 出现在 HTTP `message`，不是单据号/设备编号/二维码/内部标识符。
+3. 参考句「持授权密钥的最高权限人员」是否发明新权限？
+   否。判定仍是现有 `isMasterAuthority`。HTML 已有「持授权 U 盘密钥的最高权限」。这是显示映射，不是新角色。
+4. 034 是添加还是冲突？
+   冲突。判定顺序冲突→细化→技术性→添加。能指到 PGM-001/066 明文「提示、报错、枚举取值必须全部为中文」。
+5. 旧 33 项会不会被这 1 个新 ID 说成 34 项新缺陷？
+   不会。001—033 队列仍否；仅 034 为新待修。台账行数 34 ≠ 34 项新缺陷。
+
+## 6. 四分类
+
+本批新意见只有 034：
+
+| 分类 | 本批新条 | 处置 |
+|---|---|---|
+| **冲突** | M006-034 | 进入修复队列（是） |
+| 细化 | 0 | — |
+| 技术性 | 0 | — |
+| 添加 | 0 | 不走 2.2 |
+
+旧 001—033 分类保持历史值（冲突 11 + 本批 034 使表内冲突格为 12；细化 16；技术性 6）。本票不重开、不改其分类。无一项被本席从候选改判为添加。技术性「无业务发明」本批无新条。
+
+HTML 原文（`docs/platform-governance-map-for-operations-v0.1.html` 第 287 行 / PGM-001；PGM-066 已拍板同文）：
+
+> 展示给用户的一切文字，包括菜单、页面标题、字段名、状态、枚举取值、按钮、提示、报错……必须全部为中文，不得出现任何英文……可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符。这四样之外，凡是展示给人看的文字一律中文，不得以“这是标识”为由保留英文。
+
+冻结引用：PGM-001、PGM-066、GEN-002、GEN-015（并集；CX 未写 GEN-002，本席原票有，候选已列入）。
+
+## 7. 工程缺陷与修复队列
+
+| ID | 工程缺陷 | 修复队列归属 | 本席 B2 |
+|---|---|---|---|
+| M006-001—033 | 历史值不变 | **否** | **维持** |
+| M006-034 | **是** | **是** | **接受** |
+
+冲突且人读报错已被两席命令/HTTP 输出钉住，故工程缺陷是、队列是。有 ID 且入队后，本批双 PASS 定稿才可交给本次授权的 Claude 修；本席不修。
+
+队列表机检：34 行、34 个唯一连续 ID、队列「是」=1（仅 034）、「否」=33、待答 0。
+
+## 8. ponytail-review（只列可删，不修）
+
+- `AccessService.java:515`: shrink 人读句里的 `USB`；不要改 `isMasterAuthority`。
+- `AccessService.java:1061` 与 `:1491`: shrink 人读句里的 `CNY`/输入币种；不要放宽允许值。
+- `AccessService.java:1694`: shrink 人读句里的 `approve`/`reject`；不要改内部允许列表。
+- `FieldLabels.java` / `GlobalExceptionHandler.java`: 不改。032 已证明改抛点即可。
+- net: 4 处字符串 + 1 个既有测试类最小回归；零新框架、零 HTML。
+
+## 9. 真实验证命令与缺口
+
+本次 B2 **未**运行会写 `backend/target` 的 Maven/测试。未创建 `.scratch/` 或 `/tmp` 编译产物。未连生产库、未 HTTP 写、未改 SQL/HTML/配置/台账。未派子代理。未启动其他模块。未读 Codex B2 V0.16 正文作投票依据。
+
+实跑（只读）：
+
+```
+git rev-parse HEAD
+→ 395e6fbab027020da1c38237fad17e8edcd7a132
+git rev-parse --abbrev-ref HEAD
+→ main
+sha256sum findings.md
+→ 0974cdae8f4cfc954a302f68773d08853e35fb63a3dc78b5bc55d866889fec7a  （250382 字节，V0.65）
+sha256sum plan/criteria-lock.md
+→ 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
+python3 读 AccessService 515/1061/1491/1694 与文件头 V0.13；测试文件头 V0.20
+→ 四处英文夹排仍在；修复未开始
+python3 解析队列表
+→ 34 唯一 ID；001—033 队列否；034 冲突/是/是
+```
+
+窄读：`AccessService` 515、1058—1062、1488—1492、1681—1695、703—724；`AccessController` 147—155；`FieldLabels.translate` 504—529；`GlobalExceptionHandler` 49—61；治理地图 287 / 「持授权 U 盘密钥的最高权限」；`questions/M006-027.md` 答复首行。
+
+**缺口（如实）：**
+
+- 未重跑本席 B5 TranslateProbe / 99 项测试，也未重跑 Codex 的 `b5InvalidDecisionResponseIsChinese`。源码四处与已落盘 B5 输出足以支持 034。
+- 未把 `request_type is invalid` 或其它未在两席本次意见中的英文键扩进本批。
+
+上述缺口不构成待补审计项。B2 审的是候选归并，不是再做一遍全模块 B5。
+
+## 10. 三十四 ID 逐条结论
+
+图例：差集=原票是否完整进入候选；本席 B2=是否接受候选**当前**分类/队列/范围。
+
+| ID | 来源 | 候选分类 | 工程缺陷 | 队列 | 本席 B2 | 说明 |
+|---|---|---|---|---|---|---|
+| 001—033 | 历史定稿（含 B5-r2 的 031/032/033） | 不变 | 不变 | **否** | 维持 | 不拼接、不重审、不改拟修。不能把本批说成 34 项新缺陷。 |
+| 034 | B5-r3 CX-B5-01 ∪ GR-01 | 冲突 | 是 | 是 | **接受** | 四处人读报错夹英文；只修显示文本与对应真实回归/版本。 |
+
+候选表队列合计 **1 是**（034）/ **33 否**（001—033）/ 待答 0。分类相对旧 33 项：新增冲突 1。机检 34 个连续唯一 ID 各一次。
+
+无漏并：CX-B5-01 + GR-01 全部进入 034。无改写本席 GR-01 事实。无新添加项、无新用户人闸、无 HTML 申请、无 LOCK-AMEND。冻结 scope 不增不减。本席不修复、不把 Claude 授权扩到后续模块。
+
+## 11. 整体结论
+
+**PASS**
+
+- 输入：`modules/M006-b5-r3-self-raw.md`（全文 40760 / `2640e139…`；独立票前 34586 / `72da471a…`）；`modules/M006-b5-r3-grok-raw.md`（21532 / `06506fb2…`）。候选 findings.md **V0.65** / `0974cdae8f4cfc954a302f68773d08853e35fb63a3dc78b5bc55d866889fec7a`。
+- 全批 2 条意见归并正确：034 冲突 / 工程缺陷是 / 队列是；旧 001—033 队列否不变。
+- 四处精确边界：`AccessService.java` 515、1061、1491、1694 仅人读 message；测试 V0.20→V0.21；源版本 V0.13→V0.14。内部枚举、币种允许值、权限、业务 HTML、翻译器均保留。
+- 0 添加、待答 0。不存在有条件通过。本票只定本批 B2 候选复核；不进入 B3、不写源代码。
+- 实际修复尚未开始。本席 FAIL 否决权未动用。双 PASS 仍须 Codex 本批 B2 自票合取；任一方 FAIL 则整批 FAIL。本席不得以任何方式覆盖或改写对方票。
diff --git a/docs/audit-2026-09/modules/M006-b1-self-raw.md b/docs/audit-2026-09/modules/M006-b1-self-raw.md
index e97aa4fe..cf8c04bd 100644
--- a/docs/audit-2026-09/modules/M006-b1-self-raw.md
+++ b/docs/audit-2026-09/modules/M006-b1-self-raw.md
@@ -788,3 +788,11 @@ PASS。本票只针对同V0.57全30项。Grok新票若FAIL则整批继续B2，
 差集为空：本批两raw共3条都已登记，无遗漏、无重复修复范围；Grok未列入finding的私有slug及wildcard注释只作既有备注，不自扩队列。候选33ID中2是31否，新增0添加项、0HTML申请；其余30项并未改分类或拟修范围。
 
 遵循完整工作区、B3-before/after覆盖untracked、阶段A冻结范围的同一规则；033不能以修改scope/mapping来处理。若Grok本批不接受033，本批留B2继续事实核对，不从031/032先修。业务HTML、锁/映射、支付/SMS开关均不动，B3仍2，运行失败仍0。
+
+
+## B2 整批归并 V0.16：M006本次例外修复前
+
+版本：V0.16
+审计结论：PASS。仅本方B2票，待Grok独立复核。
+输入：modules/M006-b5-r3-self-raw.md及modules/M006-b5-r3-grok-raw.md完整本次报告。候选findings V0.65 SHA0974cdae8f4cfc954a302f68773d08853e35fb63a3dc78b5bc55d866889fec7a。
+两份各1原始意见全部归并034，4源码位置、Grok更宽范围保留；分类冲突/工程是/队列是。旧33项当前全部否，无其他新意见、无添加/HTML申请。一次性修复主体变更与多一次修复来自操作员新指令，不冒称永久控制合同变更；内部业务定义及共有错误翻译器不改。补真实行为测试均归因034。候选不存在漏项或单方压低Grok意见，原始报告字节全部保留。
diff --git a/docs/audit-2026-09/modules/M006-b4-r3-grok-raw.md b/docs/audit-2026-09/modules/M006-b4-r3-grok-raw.md
new file mode 100644
index 00000000..a82a13de
--- /dev/null
+++ b/docs/audit-2026-09/modules/M006-b4-r3-grok-raw.md
@@ -0,0 +1,448 @@
+CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
+
+# M006 B4-r3 Grok 只读复审（审计席乙）
+
+版本：V0.01
+控制：V0.17；模块 M006 `10-backend-main-access-01`（6/322）；B3 已执行次数=3；ledger=`IN_PROGRESS`。
+本席角色：Grok。只读复审本轮完整 diff，不修复、不改方案、不执行 B3/B5、不重开其他模块、不启用第三席。
+本次完整报告范围：本文件自本行 `CRITERIA-LOCK-SHA256:` 起至文末。
+
+## 0. 技能预检（实际核读，非声称）
+
+已实际读取并按合同收窄口径使用：
+
+| 技能 | 路径 | 本轮用法 |
+|---|---|---|
+| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检；本席为派发的审计席乙，不按其“先澄清再动手”扩闸 |
+| requesting-code-review | 同套件 `requesting-code-review/SKILL.md` | B4 只由本席审；**不用**本体 `HEAD~1`、同会话子代理、无 ID 即刻修 |
+| verification-before-completion | 同套件 `verification-before-completion/SKILL.md` | PASS 前本席在 `.scratch` 隔离实跑红绿对照，不以操作员自审 98/0 当本席证据 |
+| grilling | `/home/james/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只借追问结构；**不**等操作员、**不**派子代理 |
+| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 读错误→复现→追数据流；不改生产代码、不加诊断桩 |
+| diagnosing-bugs | mattpocock `engineering/diagnosing-bugs/SKILL.md` | 031/032 红绿对照环 |
+| codebase-design | mattpocock `engineering/codebase-design/SKILL.md` | 幂等范围接缝：`CallerScopeResolver` / `IdempotencyService.execute` |
+| tdd | mattpocock `engineering/tdd/SKILL.md` | 测缝须用户确认作废；本席只核已落测试是否能红、是否打在真实接缝 |
+| ponytail-review | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删；结论见 §7 |
+| writing-for-agents | mattpocock `productivity/writing-for-agents/SKILL.md` | 本 raw 给下游席阅读 |
+| i-have-adhd | `/home/james/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 编号、先结论、无寒暄；**第一行被控制锁哈希格式覆盖** |
+| differential-review | trailofbits `differential-review/SKILL.md` + methodology/adversarial/patterns | 6 文件 SMALL+HIGH（授权/幂等）；**不**另写报告文件、**不**派 adversarial-modeler |
+| knowledge-work code-review | `/home/james/.claude/plugins/cache/knowledge-work-plugins/engineering/1.2.0/skills/code-review/SKILL.md` | 正确性/安全/错误路径；CONNECTORS.md 不存在，记缺口 |
+| sentry code-review | sentry-skills `code-review/SKILL.md` | 访问控制与测试接缝 |
+| audit-context-building | trailofbits 该技能 | 手工追 `currentActor`/`resolve`/`execute`/`FieldLabels.translate`；**不**派 function-analyzer、不写 `audit-context/` |
+
+缺口（不得冒充已调用）：
+
+- `batch-grill-me`：未找到技能目录
+- `ubiquitous-language`：未找到技能目录
+- `git-guardrails-claude-code`：存在于 mattpocock `skills/misc/`，可选；本轮无 git 写操作，未调用
+- knowledge-work `code-review/CONNECTORS.md`：文件不存在
+
+明确不用（合同划掉）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`ponytail-audit`、`ponytail-debt`、`research`、`code-review`（mattpocock）。未派第三席。
+
+## 1. 冻结输入
+
+- 本席实算 `docs/audit-2026-09/plan/criteria-lock.md` SHA256 = `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`
+- sidecar `plan/criteria-lock.sha256` 同行；`ledger.md` 顶部同行；`LOCK-AMEND-COUNT: 0`
+- `findings.md`：V0.62；241974 字节；SHA256=`abdf0b89301bd74b50b1f25c6218085e033114fc2f25e28da1b50044b62d6f2f`
+- 当前队列表唯一值：031/032 修复队列=是；001—030 与 033=否。本轮只允许归因 031/032。
+- 拟修边界（现值，不扩）：031 沿用既有主体/组织字段收窄共享 `p` 范围，缓存表不改结构；新范围键用已有 `hashJson` 适配 `operation_id` VARCHAR(128)；旧共享记录缺可靠主体归属时拒绝再次执行且不返回其内容；同主体新记录重试仍返回原结果。032 只改本模块账号类型错误分支与真实 handler 响应断言，不改 FieldLabels、不改错误码与注册审批规则。
+- before tree=`11319ccfc6c0a3b609bc1cfdd4579766a7118705`（type=tree）
+- after tree=`c77d4453db7eb9b92388de630fdd5121cdef7959`（type=tree）
+- 未读取本轮 M006-b4-r3 self raw（文件不存在）、任何 B5 raw、双方 B1/B2 论证正文；不以其他席结果作证据。findings.md 仅作闸 3 ID 归因来源。
+- 未按路径过滤 diff、未改用 HEAD、未建第三 tree。工作区另有 r1/r2 未跟踪修复源，均同时存在于 before/after 两树，故本轮 diff 无名变化。
+
+适用冻结判据（模块映射内）：GEN-001、GEN-002、GEN-003、GEN-004、GEN-005、GEN-007、GEN-009、GEN-010、GEN-015、BSM-028、BSM-114、PGM-001、PGM-066。不扩映射、不启用锁外判据、不要求改冻结 scope。
+
+## 2. 两树完整 diff 逐字原文
+
+命令：`git diff --binary 11319ccfc6c0a3b609bc1cfdd4579766a7118705 c77d4453db7eb9b92388de630fdd5121cdef7959`
+
+实算：14687 字节；SHA256=`a04dfea8324fd7a11d5b5cfebf3585fd93c9cb32ec3cd05fde2bb8b610956717`；6 路径；`grep -c '^@@'` = 12。与派发声明一致。
+
+`git ls-tree -r --name-only` 两树路径 `comm -3` 为空（无新增/删除路径，仅 6 个已有路径改内容）。6 个工作区文件 SHA256 均与 after blob 逐字相同。
+
+路径与 numstat（+ / −）：
+
+1. `backend/src/main/java/com/complex/platform/access/AccessService.java` +2/−2
+2. `backend/src/main/java/com/complex/platform/access/AuthorizationService.java` +21/−23
+3. `backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java` +8/−6
+4. `backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java` +13/−3
+5. `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` +51/−1
+6. `backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java` +34/−0
+
+下文为该命令全部未过滤输出（逐字）：
+
+````
+diff --git a/backend/src/main/java/com/complex/platform/access/AccessService.java b/backend/src/main/java/com/complex/platform/access/AccessService.java
+index 37ddec45..aded7caf 100644
+--- a/backend/src/main/java/com/complex/platform/access/AccessService.java
++++ b/backend/src/main/java/com/complex/platform/access/AccessService.java
+@@ -1,6 +1,6 @@
+ package com.complex.platform.access;
+ 
+-// 文件版本：V0.12
++// 文件版本：V0.13
+ 
+ import com.complex.platform.access.dto.AccessGrantApplyRequest;
+ import com.complex.platform.access.dto.AccessGrantReviewRequest;
+@@ -920,7 +920,7 @@ public class AccessService {
+                         String accountType = normalizeAccountType(trimToDefault(request.accountType(), "staff"));
+                         if (!ALLOWED_ACCOUNT_TYPES.contains(accountType)) {
+                             throw ApiException.badRequest("INVALID_ACCOUNT_TYPE",
+-                                    "账号类型「" + accountType + "」不允许，可选值：" + ALLOWED_ACCOUNT_TYPES);
++                                    "账号类型不允许，请重新选择");
+                         }
+                         String organizationId = requireText(request.organizationId(), "organization_id");
+                         if (!authorizationService.isPlatformAdmin(actor)
+diff --git a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
+index 9ed82813..7221c16f 100644
+--- a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
++++ b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
+@@ -1,6 +1,6 @@
+ package com.complex.platform.access;
+ 
+-// 文件版本：V0.03
++// 文件版本：V0.04
+ 
+ import com.complex.platform.auth.AuthService;
+ import com.complex.platform.common.api.ApiException;
+@@ -190,32 +190,30 @@ public class AuthorizationService implements CallerScopeResolver {
+         throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
+     }
+ 
+-    /**
+-     * Returns the caller scope token for idempotency key isolation.
+-     * "m:{merchantId}" for merchant users, "p" for platform admins/staff.
+-     * Returns "" on any error so IdempotencyService falls back to unscoped (safe degradation).
+-     */
++    /** 非商户请求按已有主体及组织隔离；商户与系统请求沿用原范围。 */
+     @Override
+     public String resolve() {
+-        try {
+-            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
+-            if (auth == null || !auth.isAuthenticated()) {
+-                return "";
+-            }
+-            Object principal = auth.getPrincipal();
+-            String merchantId = null;
+-            if (principal instanceof AccessActor actor) {
+-                merchantId = actor.merchantId();
+-            } else if (principal instanceof AuthService.UserRecord user) {
+-                merchantId = user.merchantId();
+-            }
+-            if (merchantId != null && !merchantId.isBlank()) {
+-                return "m:" + merchantId;
+-            }
+-            return "p";
+-        } catch (Exception e) {
++        String legacy = legacyScope();
++        if (!"p".equals(legacy)) {
++            return legacy;
++        }
++        AccessActor actor = currentActor();
++        if (actor.id() == null || actor.id().isBlank()) {
++            throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
++        }
++        return "u:" + actor.id().length() + ":" + actor.id() + ":"
++                + java.util.Objects.toString(actor.organizationId(), "");
++    }
++
++    @Override
++    public String legacyScope() {
++        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
++        if (auth == null || !auth.isAuthenticated()) {
+             return "";
+         }
++        AccessActor actor = currentActor();
++        String merchantId = actor.merchantId();
++        return merchantId == null || merchantId.isBlank() ? "p" : "m:" + merchantId;
+     }
+ 
+     private boolean knownPermission(String permissionCode) {
+diff --git a/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java b/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java
+index 8d4c9c08..a98a5ede 100644
+--- a/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java
++++ b/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java
+@@ -1,12 +1,14 @@
+ package com.complex.platform.common.idempotency;
+ 
+-/**
+- * Resolves the current caller's scope token for idempotency key isolation.
+- * Returns "m:{merchantId}" for merchant callers, "p" for platform admins,
+- * or "" (empty) when no scope is available (e.g., in tests).
+- * An empty scope leaves the operationId unmodified (backward-compatible).
+- */
++// 文件版本：V0.01
++
++/** Resolves the trusted caller scope used to isolate idempotent operations. */
+ @FunctionalInterface
+ public interface CallerScopeResolver {
+     String resolve();
++
++    /** Existing scopes keep their keys; changed scopes must also check their old records. */
++    default String legacyScope() {
++        return resolve();
++    }
+ }
+diff --git a/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java b/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java
+index 65260597..05f286a3 100644
+--- a/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java
++++ b/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java
+@@ -1,4 +1,7 @@
+ package com.complex.platform.common.idempotency;
++
++// 文件版本：V0.01
++
+ import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;
+ 
+ import com.complex.platform.common.api.ApiException;
+@@ -70,7 +73,15 @@ public class IdempotencyService {
+             Function<T, T> storedResponseTransformer
+     ) {
+         requireIdempotencyKey(idempotencyKey);
+-        String scopedId = scopedOperationId(operationId);
++        String scope = callerScopeResolver.resolve();
++        String legacyScope = callerScopeResolver.legacyScope();
++        String legacyId = scopedOperationId(operationId, legacyScope);
++        boolean changedScope = !java.util.Objects.equals(scope, legacyScope);
++        String scopedId = changedScope ? "caller:" + hashJson(java.util.List.of(scope, operationId)) : legacyId;
++        // 旧共享记录没有可靠主体归属：保留结果，阻止串用或再次执行。
++        if (changedScope && repository.findByOperationIdAndIdempotencyKey(legacyId, idempotencyKey).isPresent()) {
++            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "该请求编号存在旧处理记录，请先核实原操作结果");
++        }
+         String requestHash = hashJson(request);
+         Instant now = Instant.now();
+         IdempotencyRecord reservation = new IdempotencyRecord(
+@@ -111,8 +122,7 @@ public class IdempotencyService {
+                 "请求正在处理中，请稍后刷新查看结果");
+     }
+ 
+-    private String scopedOperationId(String operationId) {
+-        String scope = callerScopeResolver.resolve();
++    private String scopedOperationId(String operationId, String scope) {
+         if (scope == null || scope.isEmpty()) {
+             return operationId;
+         }
+diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+index cbf28cd3..7b63bf74 100644
+--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
++++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+@@ -1,6 +1,6 @@
+ package com.complex.platform.access;
+ 
+-// 文件版本：V0.19
++// 文件版本：V0.20
+ import static org.assertj.core.api.Assertions.assertThat;
+ import static org.assertj.core.api.Assertions.assertThatThrownBy;
+ import static org.mockito.ArgumentMatchers.*;
+@@ -592,4 +592,54 @@ class M006SecurityRegressionTests {
+         assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
+                 e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
+     }
++
++    // M006-031：实际控制器须按组织和操作人区分首次请求。
++
++    @Test void companyRequestsHaveIndependentResultsAndRetryOnlyOnce() {
++        var idem = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
++        idem.setCallerScopeResolver(authorization);
++        service = new AccessService(repository, idem, new AuditEventService(new InMemoryAuditEventRepository()),
++                authorization, credentials, null, true, 3, 3);
++        when(repository.listPermissions()).thenReturn(List.of(
++                new PermissionDefinition(LOW, "查看", "ops", "medium", false, "active"),
++                new PermissionDefinition("access.role.manage", "管理角色", "access", "medium", false, "active")));
++        for(String id: List.of("manager-a", "manager-b")) {
++            effect(id, LOW, AccessRepository.Effect.GRANT, id+"-read", false);
++            effect(id, "access.role.manage", AccessRepository.Effect.GRANT, id+"-manage", false);
++        }
++        var controller = new AccessController(service, authorization);
++        var request = new RoleRequest("company-first-role", "正常首次创建", List.of(), "巡检员", "只读", List.of(LOW), "project");
++        actor("manager-a", "staff", "org-a", null);
++        var first = controller.createRole(request).data();
++        actor("manager-b", "staff", "org-b", null);
++        var result = controller.createRole(request);
++        assertThat(roles.values()).extracting(RoleRecord::organizationId).containsExactlyInAnyOrder("org-a", "org-b");
++        assertThat(result.data().id()).isNotEqualTo(first.id());
++        assertThat(controller.createRole(request).data().id()).isEqualTo(result.data().id());
++        actor("manager-a", "staff", "org-a", null);
++        assertThat(controller.createRole(request).data().id()).isEqualTo(first.id());
++        assertThat(roles).hasSize(2);
++    }
++
++    // M006-032：实际错误响应须保持中文。
++
++    @Test void invalidAccountTypeResponseIsChinese() throws Exception {
++        when(repository.listPermissions()).thenReturn(List.of(
++            new PermissionDefinition("access.user.manage", "管理账号", "access", "medium", false, "active")));
++        var controller = new AccessController(service, authorization);
++        ApiException failure = null;
++        try {
++            controller.createAccessUser(new AccessUserRequest("b5-invalid", "检查错误提示", List.of(),
++                "example", "password", null, "platform_admin", "org", null, "active"));
++        } catch(ApiException e) { failure = e; }
++        assertThat(failure).isNotNull();
++        assertThat(failure.code()).isEqualTo("INVALID_ACCOUNT_TYPE");
++        var handler = new com.complex.platform.common.api.GlobalExceptionHandler();
++        var method = handler.getClass().getDeclaredMethod("handleApiException", ApiException.class);
++        method.setAccessible(true);
++        var response = (org.springframework.http.ResponseEntity<?>) method.invoke(handler, failure);
++        var body = (com.complex.platform.common.api.ApiResponse<?>) response.getBody();
++        assertThat(body.message()).contains("账号类型").doesNotContainPattern("[A-Za-z]");
++    }
++
+ }
+diff --git a/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java b/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java
+index 02bda0c1..f604c646 100644
+--- a/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java
++++ b/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java
+@@ -1,5 +1,7 @@
+ package com.complex.platform.common.idempotency;
+ 
++// 文件版本：V0.01
++
+ import static org.assertj.core.api.Assertions.assertThat;
+ import static org.assertj.core.api.Assertions.assertThatThrownBy;
+ 
+@@ -300,6 +302,38 @@ class IdempotencyServiceTests {
+         assertThat(responseB).isNotEqualTo(new TestResponse("secret-A"));
+     }
+ 
++
++    // M006-031：范围变更不能重放归属不明的旧结果，也不能再次执行旧请求。
++    @Test
++    void changedCallerScopePreservesOldOperationsAndBoundsNewKeys() {
++        service.setCallerScopeResolver(new CallerScopeResolver() {
++            public String resolve() { return "u:" + "user".repeat(16) + ":" + "organization".repeat(6); }
++            public String legacyScope() { return "p"; }
++        });
++        TestRequest request = new TestRequest("same-payload");
++        AtomicInteger actions = new AtomicInteger();
++        for (String code : java.util.List.of("OK", "IN_PROGRESS", "FAILED_RETRYABLE")) {
++            IdempotencyRecord old = new IdempotencyRecord(UUID.randomUUID(), "p:operation", code,
++                    hashJson(request), code, code.equals("OK") ? "{\"value\":\"old-result\"}" : null,
++                    code.equals("OK") ? 200 : 202, Instant.now(), Instant.now().plusSeconds(86400));
++            repository.put(old);
++            assertThatThrownBy(() -> service.execute("operation", code, request,
++                    () -> { actions.incrementAndGet(); return new TestResponse("duplicate"); }, TestResponse.class))
++                    .isInstanceOfSatisfying(ApiException.class, failure ->
++                            assertThat(failure.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
++            assertThat(repository.findByOperationIdAndIdempotencyKey("p:operation", code)).contains(old);
++        }
++        assertThat(actions).hasValue(0);
++        String operation = "createRole:" + "r".repeat(64);
++        TestResponse first = service.execute(operation, "fresh", request,
++                () -> { actions.incrementAndGet(); return new TestResponse("created"); }, TestResponse.class);
++        TestResponse retry = service.execute(operation, "fresh", request,
++                () -> { actions.incrementAndGet(); return new TestResponse("duplicate"); }, TestResponse.class);
++        assertThat(retry).isEqualTo(first);
++        assertThat(actions).hasValue(1);
++        assertThat(repository.records.values()).allMatch(record -> record.operationId().length() <= 128);
++    }
++
+     private String hashJson(Object value) {
+         try {
+             MessageDigest digest = MessageDigest.getInstance("SHA-256");
+````
+
+## 3. 逐块 ID 归因（闸 3）
+
+| hunk | 文件 | 内容 | 归属 |
+|---|---|---|---|
+| 1 | AccessService.java `@@ -1,6` | 版本头 V0.12→V0.13 | M006-032（本文件本轮仅 032 行为） |
+| 2 | AccessService.java `@@ -920,7` | 去掉英文输入值与 `ALLOWED_ACCOUNT_TYPES` 展示 | M006-032 |
+| 3 | AuthorizationService.java `@@ -1,6` | 版本头 V0.03→V0.04 | M006-031 |
+| 4 | AuthorizationService.java `@@ -190,32` | `legacyScope` 保留 `m:`/`p`/`""`；非商户 `p` 改为主体+组织 | M006-031 |
+| 5 | CallerScopeResolver.java `@@ -1,12` | 版本头；`default legacyScope()` 让既有实现仍只实现 `resolve()` | M006-031 |
+| 6 | IdempotencyService.java `@@ -1,4` | 版本头 V0.01 | M006-031 |
+| 7 | IdempotencyService.java `@@ -70,7` | 新范围 `caller:`+`hashJson`；旧共享键存在则 `IDEMPOTENCY_CONFLICT` | M006-031 |
+| 8 | IdempotencyService.java `@@ -111,8` | `scopedOperationId` 改接收已解析 scope，避免二次 `resolve()` | M006-031 |
+| 9 | M006SecurityRegressionTests.java `@@ -1,6` | 版本头 V0.19→V0.20 | M006-031 与 M006-032（同文件两测） |
+| 10 | M006SecurityRegressionTests.java `@@ -592,4` | `companyRequestsHaveIndependentResultsAndRetryOnlyOnce` | M006-031 |
+| 10 续 | 同上 | `invalidAccountTypeResponseIsChinese` | M006-032 |
+| 11 | IdempotencyServiceTests.java `@@ -1,5` | 版本头 V0.01 | M006-031 |
+| 12 | IdempotencyServiceTests.java `@@ -300,6` | 旧 `p:` 记录冲突、不执行、新键 ≤128、同主体重试一次 | M006-031 |
+
+无归因不到 031/032 的行。无顺手重构、无新策略/审批/HTML、无锁/scope 改动、无 033 业务修补。
+
+必要直接调用边界：生产唯一 `CallerScopeResolver` 实现是 `AuthorizationService`；`IdempotencyService.setCallerScopeResolver` 是既有注入点；接口 default 方法保持 `@FunctionalInterface` 与既有 `() -> ""` / `currentScope::get` 实现。`createRole` 本轮无方法体改动，因它已走 `idempotencyService.execute("createRole", …)`，隔离落在解析器与幂等服务接缝，符合 031「必要既有幂等直接实现」。
+
+先前 B3 合法未跟踪源（`AccessGrantPolicy.java`、`V386__…sql`、若干 M006*Tests）在 before/after 两树均存在且本轮无名差异，不构成漏审 untracked，也不构成扩 scope 理由。
+
+## 4. 三线审核
+
+### 4.1 第一性原理
+
+1. **为什么必须存在？** 删掉 031：两个公司组织员工用同一非空请求编号走 `AccessController.createRole`，会因共享 `p` 键把 org-b 做成 org-a 的角色回放，违背 BSM-028 原文「看不到别的商户和别的项目的数据」与「同一笔操作重复提交不重复生效」、BSM-114「只做自己该做的」、GEN-005 服务端按主体/对象授权。删掉 032：`GlobalExceptionHandler` 经 `FieldLabels.translate` 原样透传含 CJK 的混合文案，用户看到 `platform_admin`/`staff` 等英文枚举，违背 PGM-066「提示、报错必须全部为中文」与 PGM-001/GEN-015。
+2. **声称行为有没有被实际执行验证？** 有。本席未采信操作员「98 PASS/0 FAIL / 全后端 test-compile」自审数。本席在仓库 `.scratch` 临时目录复制 `backend/target/classes`（不编译共享 target），删除被改类后对 after 源 `javac --release 21`，再跑测试；对照结束删除 scratch。共享 `backend/target/classes` mtime 两次核对均未变。未写业务库。
+3. **哪条前提没验证就当真？** 本席未跑全后端 `test-compile -DskipTests`。已跑本 diff 直接覆盖的两类测试全集：`M006SecurityRegressionTests` 33 PASS/0 FAIL；`IdempotencyServiceTests` 9 PASS/0 FAIL。未把未跑的全仓编译写成通过。
+
+### 4.2 顺从式（对照拟修与冻结判据）
+
+031：
+
+- 非商户且原范围为 `p` 时，`resolve()` 使用既有 `AccessActor.id()` 与 `organizationId()`，不发明新身份字段。
+- 商户仍 `m:{merchantId}`；未认证 `legacyScope()` 先返回 `""`，`resolve()` 因不是 `p` 而保持空范围。
+- `UserRecord` 仍经既有 `currentActor()` 转成 `AccessActor`，未丢掉该主体路径。
+- 范围变化时键为 `caller:` + 既有 `hashJson`（SHA-256 hex 64 位）= 71 字符，低于 V002 `operation_id VARCHAR(128)`。
+- 旧 `legacyId` 命中时抛既有码 `IDEMPOTENCY_CONFLICT`，固定中文说明，不 `replay` 旧 `response_body`，`action` 计数保持 0；OK/IN_PROGRESS/FAILED_RETRYABLE 三类旧记录同一处理。
+- 同主体新键首次执行、重试返回同一结果且 action 只跑一次。
+
+032：
+
+- 仅改 `INVALID_ACCOUNT_TYPE` 抛点文案；错误码、白名单集合、审批规则、FieldLabels 未改。
+- 真实 `AccessController.createAccessUser` → 真实 `GlobalExceptionHandler.handleApiException` → `FieldLabels.translate`。含 CJK 则原样透传，故根因是去掉英文片段，而不是改翻译器。
+
+版本头均随所在文件改动更新。
+
+### 4.3 对抗式
+
+| 攻击/反例 | 结果 |
+|---|---|
+| 两公司员工同编号 `company-first-role` | after：两角色、org-a/org-b、id 不同；before：只剩 org-a。根因即共享 `p` |
+| 重放旧 `p:` 成功体 `{"value":"old-result"}` | after：冲突且 `actions=0`，仓库仍是原记录；before：期望抛错却无 throwable |
+| 伪造请求体 `merchantId` 挤进 `m:` 桶 | 范围取自认证 `AccessActor`，不取 RoleRequest 商户字段 |
+| 未认证调用幂等 | `legacyScope()` 在 `currentActor()` 之前返回 `""`，不把匿名流量打进 `p` |
+| 无法识别的 principal | 现抛 `UNAUTHENTICATED`，不再落入共享 `p`（比旧 catch 后返回 `p`/`""` 更窄） |
+| 键长：`user*16`+`organization*6`+`createRole:`+64 个 `r` | 新键走 hash，测试断言全部 `operationId.length()<=128` |
+| 032 只断言异常对象、不经 handler | 测试调用真实 handler；before 响应仍含 `platform_admin`/`staff` 等拉丁字母 |
+| `FieldLabels.translate` 遇 CJK 不剥英文 | 已读 L528–529；故必须改抛点，不能靠全局翻译器。本 diff 未改 FieldLabels，符合 032 边界 |
+| 商户隔离回归 | after 上 `IdempotencyServiceTests` 既有 `differentCallerScopes…` / `merchantBCannotReplay…` 仍 PASS |
+
+未发现可成立的越权回放、旧结果泄漏、英文枚举回响或无 ID 改动。
+
+## 5. 本席独立验证（类型 4）
+
+隔离约定：`.scratch/m006-b4-r3-grok-*`；`mvn -o dependency:build-classpath -DincludeScope=test` 只写 scratch 内 classpath 文件；`javac -d` 指向 scratch；跑完 `rmtree`；`LEFTOVER []`。
+
+| 对照 | 命令要点 | 可观察结果 |
+|---|---|---|
+| after 绿（031 公司+032） | 8 源 javac + 跑 `M006SecurityRegressionTests` 全类 | `GREEN_JAVAC_EXIT 0`；`RESULT 33 PASS / 0 FAIL`，含 `companyRequestsHaveIndependentResultsAndRetryOnlyOnce` 与 `invalidAccountTypeResponseIsChinese` |
+| after 绿（031 幂等） | 同包 runner 跑 `IdempotencyServiceTests` | 新测 1 PASS；全类 `RESULT 9 PASS / 0 FAIL` |
+| before 红 031 公司 | before 的 Auth/Caller/Idempotency + after 测试 | `Expecting actual: ["org-a"] to contain exactly … ["org-a","org-b"]` |
+| before 红 031 幂等 | before 的 Caller/Idempotency + after 新测 | `Expecting code to raise a throwable` |
+| before 红 032 | before 的 AccessService + after 测试与 handler | 响应 `账号类型「platform_admin」不允许，可选值：[sales, supervisor, outsourced_vendor, merchant_admin, merchant, staff]`，命中 `[A-Za-z]` |
+| 共享产物 | 两次核 `backend/target/classes` mtime | 未变（`1785725458.3775005`） |
+
+测试能红：031 公司测打在真实 `AccessController.createRole` + `AuthorizationService` 解析器；032 打在真实 controller + `GlobalExceptionHandler`；031 幂等测打在 `execute` 旧键查找。不是tautology。
+
+## 6. 2.1 分类（本轮 diff，只记录不发问）
+
+- 031 修复：冲突（跨组织串用角色结果）。工程缺陷已对根因下药。
+- 032 修复：冲突（展示报错夹带英文枚举）。
+- 无添加项、无 HTML 修改、无锁外判据。033 仍否，本轮未改政策源码。
+
+## 7. ponytail-review
+
+`AuthorizationService.java:L204`: shrink: 范围随后被 hash，长度前缀对解析不是负载。`u:{id}:{org}` 更短。不构成缺陷。
+
+其余：复用 `hashJson`、接口 default、不改表结构。Lean already for the authorized IDs.
+
+`net: -2 lines possible.` 不挡 PASS。
+
+## 8. 结论
+
+- **审计结论：PASS**
+- **范围结论：PASS**（12 hunk / 6 路径每一行归因 031 或 032；无越界行）
+- 新 finding：无
+- 031 根因（无 merchant 主体共享 `p`）已消除；032 根因（CJK 透传混合英文抛点）已消除
+- 不进入 B5（本席职责止于 B4 raw）；不改源码/HTML/锁/台账
+
+本席未把操作员自审「60 源 javac / 95→98 / 全后端 test-compile exit0」记为本席实跑。本席实跑范围见 §5。
diff --git a/docs/audit-2026-09/modules/M006-b4-r3-self-raw.md b/docs/audit-2026-09/modules/M006-b4-r3-self-raw.md
new file mode 100644
index 00000000..694a9352
--- /dev/null
+++ b/docs/audit-2026-09/modules/M006-b4-r3-self-raw.md
@@ -0,0 +1,476 @@
+# M006 B4-r3 Codex 修复内容审计原始报告
+
+版本：V0.01
+模块：M006 `10-backend-main-access-01`，6/322。
+步骤：B4；ledger IN_PROGRESS；B3已执行3次；方案复核已修2；运行失败0。
+审计结论：PASS。范围归因结论：PASS。仅本方票，不等于B4双PASS或模块PASS。
+CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
+
+## 输入与独立顺序
+
+本批B2由两席完整B5-r2报告回退，全部3条已统一归并：031/032队列是，033细化且工程缺陷否、队列否；Grok B2 V0.06独立接受033事实更正，原B5意见完整保留。findings V0.62/SHA abdf0b89301bd74b50b1f25c6218085e033114fc2f25e28da1b50044b62d6f2f；旧30项不重新入队。
+本轮先派Grok：2026-09-06 04:16:26 UTC，既有grok-4.6/xhigh及回执适配器，目标 modules/M006-b4-r3-grok-raw.md；本方落盘前未读Grok原始JSON、stderr、raw正文或意见，真实exit尚未取得。此后独立核对完整diff、必要直接调用边界，再写本报告。
+before tree 11319ccfc6c0a3b609bc1cfdd4579766a7118705
+after tree c77d4453db7eb9b92388de630fdd5121cdef7959
+完整未过滤git diff --binary：14687字节，SHA a04dfea8324fd7a11d5b5cfebf3585fd93c9cb32ec3cd05fde2bb8b610956717，6路径/12hunk。临时索引覆盖tracked/staged/untracked，真实索引SHA824f33c20ee9b3c04744aadfede5f7bdbb2303a35b0035b1f8496ad729cd0322前后未改。
+B3前后四值实算均等于上方锁值，④来自触发本批回退的M006-b5-r2-grok-raw.md最后一条对应行。HTML三份与HEAD逐字相同；控制提示词V0.17/SHA c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21未变。未更改锁、scope、模块映射、业务开关或数据库结构。
+
+## 整份diff逐块归因
+
+| 文件/块 | 正式ID | 每一行的必要性 |
+|---|---|---|
+| AccessService：版本头、921错误分支，共2块 | M006-032 | 只删除给人看的原始英文账号类型/可选枚举拼接，改中文；保留错误码与原允许值集合。版本按用户规则递增。 |
+| AuthorizationService：版本头、193主体范围，共2块 | M006-031 | 删除非商户主体共用p的计算；复用currentActor承接已有AccessActor/UserRecord，已有ID+组织形成范围；商户及无认证系统范围保留；拒绝未知认证主体，不回退共享空间。版本随根因修复。 |
+| CallerScopeResolver：唯一块 | M006-031 | 沿用既有函数式接口，加默认legacyScope以保持原有lambda/实现的范围；让新范围能识别其旧记录。没有新增接口文件或表。旧说明已不适用于本方修复，按实际语义收窄；补文件版本。 |
+| IdempotencyService：版本、execute入口、scope私有方法，共3块 | M006-031 | 复用hashJson为变化后的主体+操作生成有界键（现表operation_id VARCHAR128）；在占位前检查旧共享记录；无法确认归属则用已有冲突错误拒绝重复执行和错误结果返回。原reserve/CAS/事务/complete/replay不改。私有方法收取已解析scope，避免把旧scope再次计算为新scope。 |
+| M006SecurityRegressionTests：版本、末尾两测试，共2块 | M006-031、M006-032 | 版本同批变更。第一个新增方法全部归031：真实controller+Service+Authorization+Idempotency，两组织同合法编号/同body各写一次，同主体重试返回同ID且总写入2。第二个全部归032：真实controller错误经真实GlobalExceptionHandler，断言中文消息不含拉丁字母。间隔空行与注释分别随对应方法。 |
+| IdempotencyServiceTests：版本、新测试，共2块 | M006-031 | 旧OK/IN_PROGRESS/FAILED_RETRYABLE记录均保持原样、不执行回调；新范围新请求可正常首次+重试一次；用正常数据库标识长度检查operation_id不超过128。默认接口方法在首个红测试时尚不存在，测试匿名实现预先声明同名方法而不写@Override，因此红测是行为断言失败，不是编译失败。 |
+
+所有12块均归已定稿且当前队列为是的031/032；没有033代码改动、没有无ID改进。范围PASS。
+
+## 根因与三线复核
+
+本轮只审上述完整修复diff及直接必要边界，不把B4称为全模块B5。
+
+- 第一性原理：回放查询键必须区分真实主体；旧表只有operation_id/idempotency_key/request_hash/response等字段，没有可靠的操作人归属。新scope依据服务端已识别的主体ID与organizationId，长度前缀使ID/组织连接可区分；IdempotencyService再用既有JSON摘要生成67字符键，不改128字符列。旧共享记录存在时不猜归属、不回放其正文、也不重新执行；409提示先核实原操作。这是保留已有成功效果的必要兼容限制，不承诺旧共享记录仍能自动重放成功响应。
+- 顺从式：GEN-002/004/005/007/009/010与BSM-028/114、PGM-001：两个组织都可完成各自首次创建；同主体重试只执行一次；已有成功记录不能因前缀变化再次生效。GEN-002/015、PGM-001/066：账号类型失败说明全中文，原错误码和类别规则保持。只用冻结判据。
+- 反例核验：真实双组织请求在红测只写org-a，org-b断言失败；绿测两个组织角色ID不同，各自重试ID保持，总数2。旧共享记录3种状态都在回调前拒绝，原记录不改，新请求继续可用。中文反例在真实错误响应中出现platform_admin等，绿测不再含拉丁字母。
+- 调用边界：先rg全体CallerScopeResolver及IdempotencyService引用与execute调用，确认只有AuthorizationService生产实现，公共服务和AuditedOperationExecutor都走同一修复入口。生产Bearer过滤器写UserRecord/AccessActor，客人过滤器写AccessActor；两个调度Job已有SCHEDULER_ACTOR。额外实际核验两种主体表示范围相同、merchant原范围和空认证系统原范围保留（4断言PASS）。
+- 公开入口必要核对：SecurityConfig仍有默认匿名上下文；不能把匿名当业务主体。本次识别未知主体改为明确失败，检查了受影响共享执行入口：客人写操作先requireCustomer并由CustomerTokenAuthenticationFilter绑定客人；开放回调IntegrationService使用endpoint/provider自身事件去重，ThirdPartyScaffoldingService也没有公共IdempotencyService执行调用；自助机heartbeat直接保存并校验设备凭据，不走本方法。没有把这些外围文件纳为新的全审模块，未修改它们。
+
+已逐行读完整diff；再读当前CallerScopeResolver与IdempotencyService全文，核当前AuthorizationService修改块和其currentActor，原AccessService创建角色/账号类型/异常响应调用链在本批B5、B2已完整核实，当前除已列一行消息外未变。当前6文件与after tree一致；其余M006/B3源保持上一冻结内容。
+
+## 实际验证与边界
+
+当前60源fresh javac完成后，红测95PASS/3FAIL，失败均为行为断言；改生产实现后同一命令98PASS/0FAIL（M006Security33、AccessService35、Authorization21、Idempotency9）。全后端test-compile真实exit0，表示完整编译，不称跑了全部后端测试。B4追加主体兼容核验4断言PASS，临时文件自动清理。
+未运行共享数据库迁移；未重复前端测试（本轮无前端改动）；未声称真实厂商支付/SMS已接入。既有B3-r2的377测试为历史证据，本票不把它们计作本轮实跑。
+按本会话已读技能实际执行systematic-debugging/diagnosing-bugs根因追查、TDD先红后绿、requesting-code-review仅派Grok、receiving-code-review整批归并、verification-before-completion证据先行。using-superpowers已预检；grilling仅自问自答；codebase-design用于已有公共入口；ponytail采用现有接口/摘要和最小错误说明；无第三席。未核到utf8_guard.py，不声称运行；6文件严格UTF8解码且无替换符实际通过。
+
+## 验证命令与完整输出
+
+### 先红后绿（同一条命令）
+
+```powershell
+@'
+# V0.01 M006-r3 current-source behavior checks
+from pathlib import Path
+import subprocess,tempfile
+runner="package com.complex.platform.access;\npublic class M006Runner {\n public static void main(String[] args)throws Exception {\n int passed=0,failed=0;\n for(String name: args){var c=Class.forName(name);int localPass=0,localFail=0;\n for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)){\n var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);var t=ctor.newInstance();\n try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}\n m.setAccessible(true);m.invoke(t);passed++;localPass++;\n }catch(java.lang.reflect.InvocationTargetException e){failed++;localFail++;System.out.println(\"FAIL \"+name+\".\"+m.getName()+\": \"+e.getCause());}\n finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}\n }\n System.out.println(name+\" \"+localPass+\" PASS / \"+localFail+\" FAIL\");\n } System.out.println(\"RESULT \"+passed+\" PASS / \"+failed+\" FAIL\");if(failed>0)System.exit(1);\n }\n}"
+with tempfile.TemporaryDirectory(prefix="m006-r3-tests-",dir=".scratch") as d:
+ root=Path(d).resolve();cpfile=root/"classpath"
+ r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
+ if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
+ cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
+ src=root/"M006Runner.java";src.write_text(runner,encoding="utf-8");out=root/"classes";out.mkdir()
+ base=Path("backend/src/main/java/com/complex/platform");tests=Path("backend/src/test/java/com/complex/platform")
+ sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))+list((base/"common/idempotency").glob("*.java"))
+ sources += [base/p for p in ["access/staff/StaffService.java","common/api/FieldLabels.java","common/api/GlobalExceptionHandler.java","governance/review/SupervisorReviewService.java","evidence/AuditEventService.java"]]
+ sources += [tests/p for p in ["support/InMemoryAuditEventRepository.java","support/InMemoryIdempotencyRepository.java","support/TestSecurityActors.java","access/M006SecurityRegressionTests.java","access/AccessServiceTests.java","access/AuthorizationServiceTests.java","common/idempotency/IdempotencyServiceTests.java"]]+[src]
+ r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources]],capture_output=True,text=True)
+ print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
+ if r.returncode:raise SystemExit(r.returncode)
+ r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner","com.complex.platform.access.M006SecurityRegressionTests","com.complex.platform.access.AccessServiceTests","com.complex.platform.access.AuthorizationServiceTests","com.complex.platform.common.idempotency.IdempotencyServiceTests"],capture_output=True,text=True)
+ print(r.stdout,r.stderr);raise SystemExit(r.returncode)
+
+'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
+```
+
+红测：
+```text
+JAVAC_EXIT 0 SOURCES 60
+ Note: backend/src/main/java/com/complex/platform/access/staff/StaffService.java uses unchecked or unsafe operations.
+Note: Recompile with -Xlint:unchecked for details.
+
+FAIL com.complex.platform.access.M006SecurityRegressionTests.companyRequestsHaveIndependentResultsAndRetryOnlyOnce: java.lang.AssertionError: 
+Expecting actual:
+  ["org-a"]
+to contain exactly in any order:
+  ["org-a", "org-b"]
+but could not find the following elements:
+  ["org-b"]
+
+FAIL com.complex.platform.access.M006SecurityRegressionTests.invalidAccountTypeResponseIsChinese: java.lang.AssertionError: 
+Expecting actual:
+  "账号类型「platform_admin」不允许，可选值：[sales, supervisor, outsourced_vendor, merchant_admin, merchant, staff]"
+not to contain pattern:
+  "[A-Za-z]"
+com.complex.platform.access.M006SecurityRegressionTests 31 PASS / 2 FAIL
+com.complex.platform.access.AccessServiceTests 35 PASS / 0 FAIL
+com.complex.platform.access.AuthorizationServiceTests 21 PASS / 0 FAIL
+FAIL com.complex.platform.common.idempotency.IdempotencyServiceTests.changedCallerScopePreservesOldOperationsAndBoundsNewKeys: java.lang.AssertionError: 
+Expecting code to raise a throwable.
+com.complex.platform.common.idempotency.IdempotencyServiceTests 8 PASS / 1 FAIL
+RESULT 95 PASS / 3 FAIL
+ Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
+WARNING: A Java agent has been loaded dynamically (/home/james/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.10/byte-buddy-agent-1.18.10.jar)
+WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
+WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
+WARNING: Dynamic loading of agents will be disallowed by default in a future release
+OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
+
+
+```
+
+绿测：
+```text
+JAVAC_EXIT 0 SOURCES 60
+ Note: backend/src/main/java/com/complex/platform/access/staff/StaffService.java uses unchecked or unsafe operations.
+Note: Recompile with -Xlint:unchecked for details.
+
+com.complex.platform.access.M006SecurityRegressionTests 33 PASS / 0 FAIL
+com.complex.platform.access.AccessServiceTests 35 PASS / 0 FAIL
+com.complex.platform.access.AuthorizationServiceTests 21 PASS / 0 FAIL
+com.complex.platform.common.idempotency.IdempotencyServiceTests 9 PASS / 0 FAIL
+RESULT 98 PASS / 0 FAIL
+ Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
+WARNING: A Java agent has been loaded dynamically (/home/james/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.10/byte-buddy-agent-1.18.10.jar)
+WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
+WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
+WARNING: Dynamic loading of agents will be disallowed by default in a future release
+OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
+
+
+```
+
+### 完整编译
+
+```powershell
+ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && mvn -o -q -f backend/pom.xml test-compile -DskipTests'
+```
+```text
+exit_code=0
+stdout/stderr为空
+```
+
+### 主体兼容核验
+
+```powershell
+@'
+# V0.01 read-only runtime caller compatibility verification
+from pathlib import Path
+import subprocess,tempfile
+source="import com.complex.platform.access.*;\nimport com.complex.platform.auth.AuthService;\nimport org.springframework.security.authentication.UsernamePasswordAuthenticationToken;\nimport org.springframework.security.core.context.SecurityContextHolder;\nimport java.time.Instant;\nimport java.util.List;\npublic class ScopeCheck {\n static void bind(Object p) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p, null, List.of())); }\n public static void main(String[] args) {\n  AuthorizationService service=new AuthorizationService(null);\n  try {\n   bind(new AccessActor(\"user\", \"name\", \"staff\", \"org\", null, \"active\"));\n   String actorScope=service.resolve();assert service.legacyScope().equals(\"p\");\n   bind(new AuthService.UserRecord(\"user\", \"name\", \"unused\", \"staff\", \"org\", null, \"active\", Instant.now()));\n   assert actorScope.equals(service.resolve());\n   bind(new AccessActor(\"user\", \"name\", \"merchant\", \"org\", \"merchant-1\", \"active\"));\n   assert service.resolve().equals(\"m:merchant-1\");\n   SecurityContextHolder.clearContext();assert service.resolve().isEmpty();\n   System.out.println(\"SCOPE_CHECK 4 ASSERTIONS PASS\");\n  } finally { SecurityContextHolder.clearContext(); }\n }\n}"
+with tempfile.TemporaryDirectory(prefix="m006-b4-scope-",dir=".scratch") as d:
+ root=Path(d).resolve();cpfile=root/"classpath"
+ r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
+ if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
+ cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
+ p=root/"ScopeCheck.java";p.write_text(source,encoding="utf-8")
+ r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(root),str(p),"backend/src/main/java/com/complex/platform/access/AuthorizationService.java","backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java"],capture_output=True,text=True)
+ print("JAVAC_EXIT",r.returncode);print(r.stdout,r.stderr)
+ if r.returncode:raise SystemExit(r.returncode)
+ r=subprocess.run(["java","-ea","-cp",str(root)+":"+cp,"ScopeCheck"],capture_output=True,text=True)
+ print(r.stdout,r.stderr);raise SystemExit(r.returncode)
+
+'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
+```
+```text
+JAVAC_EXIT 0
+ 
+SCOPE_CHECK 4 ASSERTIONS PASS
+ 
+
+```
+
+### Grok本轮实际调用
+
+```powershell
+$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
+$auditPrompt = '仅执行 M006 第3次整批修复后的B4：只读审核本轮完整diff，不执行B3或B5，不重开其他模块。模块M006/322，ledger IN_PROGRESS，B3=3。当前定稿findings V0.62（031/032队列是；其他31项否），本次B2双方PASS已落B1self V0.15和B1grok V0.06。本轮before tree 11319ccfc6c0a3b609bc1cfdd4579766a7118705，after tree c77d4453db7eb9b92388de630fdd5121cdef7959；git diff --binary这两个tree的完整未过滤输出为14687字节、SHA256 a04dfea8324fd7a11d5b5cfebf3585fd93c9cb32ec3cd05fde2bb8b610956717、6路径12hunk。实算对照后必须读完整diff，写入本次raw全文和逐块ID归因。031：AuthorizationService主体+组织范围；复用CallerScopeResolver加默认legacyScope以保留既有实现；IdempotencyService复用hashJson把新范围键压到既有VARCHAR128并核对旧共享记录，旧归属不明记录报现有conflict而不返回其内容或再次执行，同主体新请求可重试；对应M006SecurityRegressionTests公司角色同编号与IdempotencyServiceTests旧请求/键长检查。032：AccessService非法账号类型中文提示，对应真实controller→GlobalExceptionHandler响应断言。所有文件自身版本头随改动更新，归所属ID。不得只看HEAD已提交差异，不漏untracked；不要新增判据或要求改冻结scope。核根因修复、行为正确性、必要直接调用边界和每行归因，整份diff审完收齐全部意见后才给审计PASS/FAIL和范围PASS/FAIL。当前真实验证：60源fresh javac，先95PASS/3FAIL，修复后98PASS/0FAIL；全后端test-compile -DskipTests真实exit0。不可把这些自审证据当你已实跑，你可独立验证但只用仓库.scratch临时隔离且清理自己的临时文件，不编译共享target，不写共享业务库、不修改源码/HTML/锁/台账，不创建额外审计产物，不派第三席。必要source依赖包含合法B3新文件，阶段A冻结scope不变。先实际预检适用技能，按完整V0.17全部B4规则。操作员明确这是对自己写/修改代码的开发调试和BUG质量修复，问题用具体行为/影响描述。完整报告追加到指定 docs/audit-2026-09/modules/M006-b4-r3-grok-raw.md，若不存在创建；版本从V0.01起，若已有内容须完整保留前缀再追加本次完整报告。写独立实算CRITERIA-LOCK-SHA256独占行。CLI不承担回执标记，适配器生成。以下V0.17完整原文继续适用：
+' + [IO.File]::ReadAllText('D:\AI\ccwork\160-audit-prompt.md', [Text.Encoding]::UTF8)
+@{prompt=$auditPrompt} | ConvertTo-Json -Compress | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -c ''import sys,json,subprocess,pathlib
+root=pathlib.Path("/home/james/complex-management").resolve()
+p=root/"docs/audit-2026-09/modules/M006-b4-r3-grok-raw.md"
+def meta():
+ if not p.exists(): return None
+ s=p.stat(); return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
+before=meta()
+prompt=json.load(sys.stdin)["prompt"]
+r=subprocess.run(["/home/james/.local/bin/grok","-p",prompt,"-m","grok-4.6","--reasoning-effort","xhigh","--output-format","json","--permission-mode","bypassPermissions","--no-subagents","--disable-web-search","--cwd",str(root)],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
+code=r.returncode
+err="GROK_EXIT" if code!=0 else "PATH_MISMATCH" if not p.resolve().is_relative_to(root) or p.is_symlink() else "REPORT_MISSING" if not p.is_file() else "REPORT_EMPTY" if p.stat().st_size==0 else "REPORT_STALE" if meta()==before else None
+print("ADAPTER_ERROR="+err if err else "REPORT_PATH=docs/audit-2026-09/modules/M006-b4-r3-grok-raw.md")
+print("EXIT_CODE="+str(code))
+sys.exit(1 if err else 0)
+'''
+```
+
+本方落盘时尚未取得真实exit，报告正文未读取。
+
+## 本轮完整diff（未过滤）
+
+```diff
+diff --git a/backend/src/main/java/com/complex/platform/access/AccessService.java b/backend/src/main/java/com/complex/platform/access/AccessService.java
+index 37ddec45..aded7caf 100644
+--- a/backend/src/main/java/com/complex/platform/access/AccessService.java
++++ b/backend/src/main/java/com/complex/platform/access/AccessService.java
+@@ -1,6 +1,6 @@
+ package com.complex.platform.access;
+ 
+-// 文件版本：V0.12
++// 文件版本：V0.13
+ 
+ import com.complex.platform.access.dto.AccessGrantApplyRequest;
+ import com.complex.platform.access.dto.AccessGrantReviewRequest;
+@@ -920,7 +920,7 @@ public class AccessService {
+                         String accountType = normalizeAccountType(trimToDefault(request.accountType(), "staff"));
+                         if (!ALLOWED_ACCOUNT_TYPES.contains(accountType)) {
+                             throw ApiException.badRequest("INVALID_ACCOUNT_TYPE",
+-                                    "账号类型「" + accountType + "」不允许，可选值：" + ALLOWED_ACCOUNT_TYPES);
++                                    "账号类型不允许，请重新选择");
+                         }
+                         String organizationId = requireText(request.organizationId(), "organization_id");
+                         if (!authorizationService.isPlatformAdmin(actor)
+diff --git a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
+index 9ed82813..7221c16f 100644
+--- a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
++++ b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
+@@ -1,6 +1,6 @@
+ package com.complex.platform.access;
+ 
+-// 文件版本：V0.03
++// 文件版本：V0.04
+ 
+ import com.complex.platform.auth.AuthService;
+ import com.complex.platform.common.api.ApiException;
+@@ -190,32 +190,30 @@ public class AuthorizationService implements CallerScopeResolver {
+         throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
+     }
+ 
+-    /**
+-     * Returns the caller scope token for idempotency key isolation.
+-     * "m:{merchantId}" for merchant users, "p" for platform admins/staff.
+-     * Returns "" on any error so IdempotencyService falls back to unscoped (safe degradation).
+-     */
++    /** 非商户请求按已有主体及组织隔离；商户与系统请求沿用原范围。 */
+     @Override
+     public String resolve() {
+-        try {
+-            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
+-            if (auth == null || !auth.isAuthenticated()) {
+-                return "";
+-            }
+-            Object principal = auth.getPrincipal();
+-            String merchantId = null;
+-            if (principal instanceof AccessActor actor) {
+-                merchantId = actor.merchantId();
+-            } else if (principal instanceof AuthService.UserRecord user) {
+-                merchantId = user.merchantId();
+-            }
+-            if (merchantId != null && !merchantId.isBlank()) {
+-                return "m:" + merchantId;
+-            }
+-            return "p";
+-        } catch (Exception e) {
++        String legacy = legacyScope();
++        if (!"p".equals(legacy)) {
++            return legacy;
++        }
++        AccessActor actor = currentActor();
++        if (actor.id() == null || actor.id().isBlank()) {
++            throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
++        }
++        return "u:" + actor.id().length() + ":" + actor.id() + ":"
++                + java.util.Objects.toString(actor.organizationId(), "");
++    }
++
++    @Override
++    public String legacyScope() {
++        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
++        if (auth == null || !auth.isAuthenticated()) {
+             return "";
+         }
++        AccessActor actor = currentActor();
++        String merchantId = actor.merchantId();
++        return merchantId == null || merchantId.isBlank() ? "p" : "m:" + merchantId;
+     }
+ 
+     private boolean knownPermission(String permissionCode) {
+diff --git a/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java b/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java
+index 8d4c9c08..a98a5ede 100644
+--- a/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java
++++ b/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java
+@@ -1,12 +1,14 @@
+ package com.complex.platform.common.idempotency;
+ 
+-/**
+- * Resolves the current caller's scope token for idempotency key isolation.
+- * Returns "m:{merchantId}" for merchant callers, "p" for platform admins,
+- * or "" (empty) when no scope is available (e.g., in tests).
+- * An empty scope leaves the operationId unmodified (backward-compatible).
+- */
++// 文件版本：V0.01
++
++/** Resolves the trusted caller scope used to isolate idempotent operations. */
+ @FunctionalInterface
+ public interface CallerScopeResolver {
+     String resolve();
++
++    /** Existing scopes keep their keys; changed scopes must also check their old records. */
++    default String legacyScope() {
++        return resolve();
++    }
+ }
+diff --git a/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java b/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java
+index 65260597..05f286a3 100644
+--- a/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java
++++ b/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java
+@@ -1,4 +1,7 @@
+ package com.complex.platform.common.idempotency;
++
++// 文件版本：V0.01
++
+ import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;
+ 
+ import com.complex.platform.common.api.ApiException;
+@@ -70,7 +73,15 @@ public class IdempotencyService {
+             Function<T, T> storedResponseTransformer
+     ) {
+         requireIdempotencyKey(idempotencyKey);
+-        String scopedId = scopedOperationId(operationId);
++        String scope = callerScopeResolver.resolve();
++        String legacyScope = callerScopeResolver.legacyScope();
++        String legacyId = scopedOperationId(operationId, legacyScope);
++        boolean changedScope = !java.util.Objects.equals(scope, legacyScope);
++        String scopedId = changedScope ? "caller:" + hashJson(java.util.List.of(scope, operationId)) : legacyId;
++        // 旧共享记录没有可靠主体归属：保留结果，阻止串用或再次执行。
++        if (changedScope && repository.findByOperationIdAndIdempotencyKey(legacyId, idempotencyKey).isPresent()) {
++            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "该请求编号存在旧处理记录，请先核实原操作结果");
++        }
+         String requestHash = hashJson(request);
+         Instant now = Instant.now();
+         IdempotencyRecord reservation = new IdempotencyRecord(
+@@ -111,8 +122,7 @@ public class IdempotencyService {
+                 "请求正在处理中，请稍后刷新查看结果");
+     }
+ 
+-    private String scopedOperationId(String operationId) {
+-        String scope = callerScopeResolver.resolve();
++    private String scopedOperationId(String operationId, String scope) {
+         if (scope == null || scope.isEmpty()) {
+             return operationId;
+         }
+diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+index cbf28cd3..7b63bf74 100644
+--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
++++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+@@ -1,6 +1,6 @@
+ package com.complex.platform.access;
+ 
+-// 文件版本：V0.19
++// 文件版本：V0.20
+ import static org.assertj.core.api.Assertions.assertThat;
+ import static org.assertj.core.api.Assertions.assertThatThrownBy;
+ import static org.mockito.ArgumentMatchers.*;
+@@ -592,4 +592,54 @@ class M006SecurityRegressionTests {
+         assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
+                 e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
+     }
++
++    // M006-031：实际控制器须按组织和操作人区分首次请求。
++
++    @Test void companyRequestsHaveIndependentResultsAndRetryOnlyOnce() {
++        var idem = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
++        idem.setCallerScopeResolver(authorization);
++        service = new AccessService(repository, idem, new AuditEventService(new InMemoryAuditEventRepository()),
++                authorization, credentials, null, true, 3, 3);
++        when(repository.listPermissions()).thenReturn(List.of(
++                new PermissionDefinition(LOW, "查看", "ops", "medium", false, "active"),
++                new PermissionDefinition("access.role.manage", "管理角色", "access", "medium", false, "active")));
++        for(String id: List.of("manager-a", "manager-b")) {
++            effect(id, LOW, AccessRepository.Effect.GRANT, id+"-read", false);
++            effect(id, "access.role.manage", AccessRepository.Effect.GRANT, id+"-manage", false);
++        }
++        var controller = new AccessController(service, authorization);
++        var request = new RoleRequest("company-first-role", "正常首次创建", List.of(), "巡检员", "只读", List.of(LOW), "project");
++        actor("manager-a", "staff", "org-a", null);
++        var first = controller.createRole(request).data();
++        actor("manager-b", "staff", "org-b", null);
++        var result = controller.createRole(request);
++        assertThat(roles.values()).extracting(RoleRecord::organizationId).containsExactlyInAnyOrder("org-a", "org-b");
++        assertThat(result.data().id()).isNotEqualTo(first.id());
++        assertThat(controller.createRole(request).data().id()).isEqualTo(result.data().id());
++        actor("manager-a", "staff", "org-a", null);
++        assertThat(controller.createRole(request).data().id()).isEqualTo(first.id());
++        assertThat(roles).hasSize(2);
++    }
++
++    // M006-032：实际错误响应须保持中文。
++
++    @Test void invalidAccountTypeResponseIsChinese() throws Exception {
++        when(repository.listPermissions()).thenReturn(List.of(
++            new PermissionDefinition("access.user.manage", "管理账号", "access", "medium", false, "active")));
++        var controller = new AccessController(service, authorization);
++        ApiException failure = null;
++        try {
++            controller.createAccessUser(new AccessUserRequest("b5-invalid", "检查错误提示", List.of(),
++                "example", "password", null, "platform_admin", "org", null, "active"));
++        } catch(ApiException e) { failure = e; }
++        assertThat(failure).isNotNull();
++        assertThat(failure.code()).isEqualTo("INVALID_ACCOUNT_TYPE");
++        var handler = new com.complex.platform.common.api.GlobalExceptionHandler();
++        var method = handler.getClass().getDeclaredMethod("handleApiException", ApiException.class);
++        method.setAccessible(true);
++        var response = (org.springframework.http.ResponseEntity<?>) method.invoke(handler, failure);
++        var body = (com.complex.platform.common.api.ApiResponse<?>) response.getBody();
++        assertThat(body.message()).contains("账号类型").doesNotContainPattern("[A-Za-z]");
++    }
++
+ }
+diff --git a/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java b/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java
+index 02bda0c1..f604c646 100644
+--- a/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java
++++ b/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java
+@@ -1,5 +1,7 @@
+ package com.complex.platform.common.idempotency;
+ 
++// 文件版本：V0.01
++
+ import static org.assertj.core.api.Assertions.assertThat;
+ import static org.assertj.core.api.Assertions.assertThatThrownBy;
+ 
+@@ -300,6 +302,38 @@ class IdempotencyServiceTests {
+         assertThat(responseB).isNotEqualTo(new TestResponse("secret-A"));
+     }
+ 
++
++    // M006-031：范围变更不能重放归属不明的旧结果，也不能再次执行旧请求。
++    @Test
++    void changedCallerScopePreservesOldOperationsAndBoundsNewKeys() {
++        service.setCallerScopeResolver(new CallerScopeResolver() {
++            public String resolve() { return "u:" + "user".repeat(16) + ":" + "organization".repeat(6); }
++            public String legacyScope() { return "p"; }
++        });
++        TestRequest request = new TestRequest("same-payload");
++        AtomicInteger actions = new AtomicInteger();
++        for (String code : java.util.List.of("OK", "IN_PROGRESS", "FAILED_RETRYABLE")) {
++            IdempotencyRecord old = new IdempotencyRecord(UUID.randomUUID(), "p:operation", code,
++                    hashJson(request), code, code.equals("OK") ? "{\"value\":\"old-result\"}" : null,
++                    code.equals("OK") ? 200 : 202, Instant.now(), Instant.now().plusSeconds(86400));
++            repository.put(old);
++            assertThatThrownBy(() -> service.execute("operation", code, request,
++                    () -> { actions.incrementAndGet(); return new TestResponse("duplicate"); }, TestResponse.class))
++                    .isInstanceOfSatisfying(ApiException.class, failure ->
++                            assertThat(failure.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
++            assertThat(repository.findByOperationIdAndIdempotencyKey("p:operation", code)).contains(old);
++        }
++        assertThat(actions).hasValue(0);
++        String operation = "createRole:" + "r".repeat(64);
++        TestResponse first = service.execute(operation, "fresh", request,
++                () -> { actions.incrementAndGet(); return new TestResponse("created"); }, TestResponse.class);
++        TestResponse retry = service.execute(operation, "fresh", request,
++                () -> { actions.incrementAndGet(); return new TestResponse("duplicate"); }, TestResponse.class);
++        assertThat(retry).isEqualTo(first);
++        assertThat(actions).hasValue(1);
++        assertThat(repository.records.values()).allMatch(record -> record.operationId().length() <= 128);
++    }
++
+     private String hashJson(Object value) {
+         try {
+             MessageDigest digest = MessageDigest.getInstance("SHA-256");
+
+```
+
+
+## V0.02 数字说明勘误
+版本：V0.02。原V0.01独立报告与PASS/范围PASS原票完整保留；本备注在读完Grok后追加，未重做或替换独立审计。原报告将新键长度写为67字符，算术有误，应为caller:的7字符加64字符摘要，共71字符，仍小于既有128列长度。实际代码与测试均未使用67常量；不改变任何代码、判据或审计结论。
diff --git a/docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md b/docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md
new file mode 100644
index 00000000..86ffc03e
--- /dev/null
+++ b/docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md
@@ -0,0 +1,317 @@
+CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
+
+# M006 B5-r3 Grok 席全模块审计报告
+
+版本：V0.01  
+席位：Grok（审计席乙）  
+步骤：B5（第 3 次 B3 之后的全模块复审）  
+模块：M006 `10-backend-main-access-01`（6/322）  
+工作区：`/home/james/complex-management` 主工作区 `main`  
+ledger：`IN_PROGRESS`，B3=3  
+after tree：`c77d4453db7eb9b92388de630fdd5121cdef7959`（14 个原有文件及 B3 合法新增 `AccessGrantPolicy.java`、`V386__access_grant_authorization_fingerprint.sql` 字节均与该 tree 一致）  
+源码版本：`AccessService.java` 文件头 V0.13；`AuthorizationService.java` 文件头 V0.04  
+findings 台账：V0.63，33 个已定稿 ID 当前修复队列均为「否」  
+本席未读取 `M006-b5-r3-self-raw.md`、未读取 Codex 本轮报告、未以旧 B4/B5 票代替本次全审。
+
+**本席结论：FAIL。**
+
+不存在有条件通过。新意见 1 条（GR-01）为冲突且工程缺陷成立；审完整模块后一次性写出。期间未修复、未路由、未派第三席、未改三份业务 HTML、未改冻结 A 范围。
+
+---
+
+## 0. 技能预检（按意图执行，缺则记缺口）
+
+本席在本环境实际核到并按收窄口径使用：
+
+| 技能 | 核到路径 | 本次用法 |
+|---|---|---|
+| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 |
+| `grilling` | `.../mattpocock-skills/1.2.3/./skills/productivity/grilling/SKILL.md` | 对抗/第一性原理追问结构；不派子代理、不等操作员 |
+| `grill-me` | `.../mattpocock-skills/1.2.3/./skills/productivity/grill-me/SKILL.md` | 同上收窄口径 |
+| `codebase-design` | `.../mattpocock-skills/1.2.3/./skills/engineering/codebase-design/SKILL.md` | 接口深度、seam、删除测试 |
+| `diagnosing-bugs` | `.../mattpocock-skills/1.2.3/./skills/engineering/diagnosing-bugs/SKILL.md` | 对缺陷构造可失败回路（FieldLabels/HTTP 探针） |
+| `systematic-debugging` | `.../superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流；不改生产代码 |
+| `verification-before-completion` | `.../superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | PASS/FAIL 前先跑命令 |
+| `ponytail-review` | `.../ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删/不可达 |
+| `writing-for-agents` | `.../mattpocock-skills/1.2.3/./skills/productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席阅读 |
+| knowledge-work `code-review` | `/home/james/.claude/plugins/cache/knowledge-work-plugins/engineering/1.2.0/skills/code-review/SKILL.md` | 安全/正确性维度，不另写产物 |
+
+明确不用或缺口（不写成已调用）：
+
+- `audit-context-building`、`semgrep`：会派子代理并另写产物，与双席及 §7 封闭清单冲突 → 存在但不适用。
+- `dispatching-parallel-agents` / `subagent-driven-development`：禁止第三席。
+- `batch-grill-me`、`ubiquitous-language`、`i-have-adhd`：本环境技能目录未核到独立 `SKILL.md` → **缺口**。
+- `git-guardrails-claude-code`：核到；本席只读、无 git 写操作，未调用。
+- `ponytail-audit` / `ponytail-debt` / `ponytail-gain`：提示词划掉。
+- `research`：划掉；规范原文由本席直接读 HTML/锁。
+
+---
+
+## 1. 范围、哈希与台账事实
+
+独立实算：
+
+```text
+sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
+23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
+```
+
+sidecar `plan/criteria-lock.sha256` 与 `ledger.md` 顶部 `CRITERIA-LOCK-SHA256:` 同为该值。`LOCK-AMEND-COUNT: 0`。本席不把四值校验写成本模块 PASS。
+
+`plan/modules.md` M006 原有 14 文件（未改冻结 A 范围）。B3 合法新增依赖保留在工作区并纳入阅读：`AccessGrantPolicy.java`（44 行）、`V386__access_grant_authorization_fingerprint.sql`（71 行）。未故意删除这些源后再声称构建失败。
+
+questions：`questions/M006-027.md` 的 `## 操作员答复` 首个非空行为 `保留`。无待答添加项。三份业务 HTML 只读，未申请修改。
+
+---
+
+## 2. 第一性原理三问（模块级）
+
+1. **为什么必须存在？** 删掉本模块，BSM-009/028「管理员工、岗位、职级、权限」、BSM-114「不同人只看自己该看的」、BSM-007/PGM 权限分岗、PGM-008 员工和手机入口都做不到。14 个文件分别承担主体、HTTP 入口、授权判定、凭证发放与只读汇总。
+2. **声称的行为有没有被执行验证？** 有。本席在 `.scratch` 隔离编译当前源（`-d` 到 scratch，不写 `backend/target`），对 `complex_test` 只建连接级 TEMP 表；99 个 `@Test` 全绿，含 `companyRequestsHaveIndependentResultsAndRetryOnlyOnce`（M006-031）与 `invalidAccountTypeResponseIsChinese`（M006-032）。另跑 `FieldLabels.translate` + 真实 `GlobalExceptionHandler.handleApiException`。未跑边界见 §7。
+3. **哪个前提未经验证就当真？** 「抛点英文键会在 HTTP 出口被译成中文」只覆盖 `field is required/invalid` 形态；一旦消息已含汉字，`FieldLabels.translate` 原样透传。本席用命令证明该前提对 `normalizeDecision` / `USB` / `CNY` 文案不成立。不得用「032 已修所以中文已净」代替本次全审。
+
+---
+
+## 3. 顺从式：14 文件逐项覆盖
+
+| # | 文件 | 行数 | 覆盖要点 | 本席结果 |
+|---|---|---:|---|---|
+| 1 | `AccessActor.java` | 11 | 认证主体六字段；无业务发明 | 无新缺陷 |
+| 2 | `AccessController.java` | 299 | 各写入口 `requirePermission` + 幂等键；临时授权控制器仍抛 `valid_until is required`，经出口层译为「有效期至未填写」 | 控制器英文键被翻译，不单独立项；删除入口仍存在但服务拒绝物理删 |
+| 3 | `AccessCredentialProvisioner.java` | 12 | 凭证发放 seam；`noop()` 仅测试/缺 bean | 技术性，无缺陷 |
+| 4 | `AccessGrantPositionSummary.java` | 10 | 岗位频繁申请汇总记录 | 无缺陷 |
+| 5 | `AccessGrantRequestRecord.java` | 35 | 授权申请快照容器，`permissionCodes` 防御复制 | 对应 M006-019 登记项，无新缺陷 |
+| 6 | `AccessGrantSummary.java` | 9 | 汇总壳 | 无缺陷 |
+| 7 | `AccessGrantUserSummary.java` | 8 | 人员异常汇总；中文 summary 由服务写入 | 无缺陷 |
+| 8 | `AccessRepository.java` | 259 | 仓储契约；默认实现 fail-closed；`pageGrantRequests` 走 `PageResponse.of` | 无新缺陷 |
+| 9 | `AccessService.java` | 1946 | 账号/角色/岗位/模板/授权申请/政策/职级；V0.13 | **GR-01** |
+| 10 | `AccessUser.java` | 37 | `phoneHash` `@JsonIgnore`；兼容构造器 `createdVia=legacy` | 对应 019/025 登记，无新缺陷 |
+| 11 | `AuthorizationService.java` | 496 | DENY 优先、日常管理员桥、USB 主授权、客人储值白名单、幂等 `resolve()` 收窄 `p` | 031 行为已由测试钉住；无新缺陷 |
+| 12 | `DevAccessCredentialProvisioner.java` | 48 | `local/dev` 同步到 `AuthService`；空密码拒绝；非 active 凭证 disabled | 无缺陷 |
+| 13 | `EffectivePermissions.java` | 23 | 注释仍写 wildcard=true，实现始终 `wildcard=false`，日常权限折进 grants | 注释漂移，不构成可执行缺陷 |
+| 14 | `JdbcAccessCredentialProvisioner.java` | 69 | prod BCrypt；状态白名单；空密码拒绝 | 无缺陷 |
+
+B3 合法依赖（非冻结 14 文件，完整阅读）：
+
+- `AccessGrantPolicy.java`：时间窗/阈值 BoundedInt + 四档风险审批规则；查询不能覆盖。
+- `V386__...sql`：指纹列、政策 JSON、岗位软删除列、岗位派生授权列。JDBC 测试 `migrationAppliesAndGrantPolicyRoundTripsInPostgres` 通过。
+
+---
+
+## 4. 顺从式：52 条冻结判据
+
+判定：满足 / 本模块无实现且无冲突 / **不满足（见 GR-01）**。
+
+### GEN（18）
+
+| ID | 结果 | 证据 |
+|---|---|---|
+| GEN-001 | 满足（残留不可达私有方法，不单独立项） | 账号/角色 ID 已用 UUID；`slug()` 私有且无调用点（AccessService:1898），属可删死代码，首次正常请求撞不上 |
+| GEN-002 | **不满足** | 见 GR-01：错误路径的 HTTP 正文与「全中文报错」声称不一致 |
+| GEN-003 | 满足（已跑子集） | 现存测试含能失败断言；本席实跑 99/0。未跑 2 个依赖 StaffService 的方法，见 §7 |
+| GEN-004 | 满足 | 组织/商户在服务端校验；幂等键必填；账号类型/状态白名单 |
+| GEN-005 | 满足 | DENY 优先；日常管理员不自动得高危；受益人/申请人自审拒绝；USB 主授权与职级 3 分开 |
+| GEN-006 | 满足 | 单次授权先核验再消费；JDBC 测试证明消费失败回滚、指纹只写一次 |
+| GEN-007 | 满足 | `resolve()` 对原共享 `p` 改为 `u:{len}:{id}:{org}` 再哈希；旧 `p` 记录冲突拒绝串用；同主体重试回放。测试 `companyRequestsHaveIndependentResultsAndRetryOnlyOnce` PASS |
+| GEN-008 | 满足 | DTO `@JsonAlias`；PageResponse 元数据与条数一致（字典不再伪称 50） |
+| GEN-009 | 满足（除 GR-01 文案） | 失败走 ApiException，不伪成功 |
+| GEN-010 | 满足 | 手机哈希不回传；密码 BCrypt；未知主体不填 staff/active |
+| GEN-011 | 满足 | 写路径 `recordSuccess/Failure` |
+| GEN-012 | 满足 | 授权申请默认时间窗+每页 50；字典全量但 pageSize=实际条数 |
+| GEN-013 | 满足 | 隔离 javac 当前源 73 文件成功；含 AccessGrantPolicy |
+| GEN-014 | 满足 | V386 在 after tree 中；JDBC `migrationAppliesAndGrantPolicyRoundTripsInPostgres` PASS |
+| GEN-015 | **不满足** | 与 PGM-066 同源，见 GR-01 |
+| GEN-016 | 满足 | 本模块无独立用户文档声称 |
+| GEN-017 | 满足 | 本席未把锁外主张写入队列 |
+| GEN-018 | 满足 | 结论落到 HTML 原文或命令输出 |
+
+### BEQR（6）
+
+本模块是账号/权限，不是排班考勤薪资实现。映射要求是「不得冲突、遗漏或另行发明」。未见本模块把员工五态写成账号状态、也未见发明考勤/请假/薪资规则。员工五态与账号 `active/invited/disabled` 已在定稿 M006-024 区分为不同概念，队列否。
+
+| ID | 结果 |
+|---|---|
+| BEQR-157 员工状态 | 本模块无实现且无冲突 |
+| BEQR-182 考勤日结果 | 本模块无实现且无冲突 |
+| BEQR-183 打卡方式 | 本模块无实现且无冲突 |
+| BEQR-184 请假类型 | 本模块无实现且无冲突 |
+| BEQR-185 请假单状态 | 本模块无实现且无冲突 |
+| BEQR-186 薪资单构成 | 职级薪资只存分+CNY，不发明绩效档公式 |
+| BEQR-187 员工提成发放 | 本模块无提现/现金现抽入口 |
+
+### BSM（14）
+
+| ID | 结果 | 证据 |
+|---|---|---|
+| BSM-001 | 满足（授权政策已可配） | `grantPolicy()` 读后台 JSON；查询覆盖阈值被拒绝。政策改写即生效、无草案/生效日；参数是统计窗/频繁阈值不是金额。未能证明属于「涉及钱的配置」，不另开 finding |
+| BSM-003 | 满足 | 岗位模板存档/申请删除/最高权限软删除；`deletePositionRoleTemplate` 抛 `ARCHIVE_APPROVAL_REQUIRED` |
+| BSM-009 | 满足 | 本模块承载员工账号与权限入口 |
+| BSM-028 | 满足 | 员工/岗位/职级/权限/注册审批（手工员工建号被 `STAFF_REGISTRATION_REQUIRED` 拦住，外包除外） |
+| BSM-029 | 满足 | 权限依赖组织/商户/职级 |
+| BSM-073 | 满足 | 商户范围列表 `listUsersByMerchant`；跨商户写拒绝 |
+| BSM-084 | 本模块无手机端 UI；无冲突 | 员工自助权限不在这 14 文件发放 |
+| BSM-085 | 满足（注册第三入口已关） | `createAccessUser` 对非外包抛 `STAFF_REGISTRATION_REQUIRED` |
+| BSM-086/087/088 | 本模块无实现且无冲突 | |
+| BSM-114 | 满足 | 组织/商户/项目过滤；平台管理员与商户分列 |
+| BSM-115 | 满足 | 基础权限卡住后续授权 |
+| BSM-120 | 满足相关已拍板项 | 权限最小颗粒在权限字典；本模块不发明终端开关 |
+
+### PGM（14）
+
+| ID | 结果 | 证据 |
+|---|---|---|
+| PGM-001 | **不满足** | 「提示、报错必须全部为中文」。HTTP 实测仍漏出 approve/reject、USB，见 GR-01 |
+| PGM-008 | 满足入口级 | 账号/凭证/职级在本模块；私人手机绑定在其他模块 |
+| PGM-022/024 | 本模块无空间实现且无冲突 | |
+| PGM-026 | 本模块不建商户档案；无冲突 | |
+| PGM-032/033 | 注册两条路径由 StaffService 执行；本模块拒绝手工员工建号 | 未跑 StaffService 两测，见 §7 |
+| PGM-034 | 本模块无企微实现且无冲突 | |
+| PGM-035 | 本模块无提成发放实现且无冲突 | |
+| PGM-036 | 本模块无设备领用实现且无冲突 | |
+| PGM-048 | 满足 | 商户不能看他商户账号 |
+| PGM-059 | 满足 | 建号不落手机号哈希、JSON Ignore |
+| PGM-066 | **不满足** | 与 PGM-001 同条强制中文规则，见 GR-01 |
+
+---
+
+## 5. 对抗式（假定实现错误）
+
+内部追问与反例（grilling 结构，事实自读）：
+
+1. **平台管理员是否仍通配绕过 DENY？** `isPlatformAdminBridge` 只补日常低/中且 `requiresApproval=false` 的权限；DENY 仍先 `deny()`。测试 `administratorDenyWinsAndHighPermissionNeedsExplicitGrant`、`effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard` PASS。
+2. **无组织非管理员是否读全量？** `listAccessUsers` 走 `requireProjectScope`。测试 `missingOrganizationCannotListAllUsersOrResolveGlobalProject` PASS。
+3. **跨组织同幂等键是否串用？** 无商户时 `legacyScope=p`，`resolve()` 改为带长度前缀的主体键再 SHA-256；`operation_id VARCHAR(128)`，`caller:`+64hex=71 字符。测试 `companyRequestsHaveIndependentResultsAndRetryOnlyOnce` PASS。旧共享 `p` 记录存在则 `IDEMPOTENCY_CONFLICT`「该请求编号存在旧处理记录，请先核实原操作结果」。
+4. **受益人自审？** `reviewGrant` 拒绝 `targetUserId` 与 `requestedBy`。测试 PASS。
+5. **岗位切换是否只追加？** `replacePositionGrants`；测试 `positionChangeDropsDerivedRoleButKeepsIndependentAssignment` PASS。
+6. **非法账号类型是否仍夹英文枚举？** 源码现为「账号类型不允许，请重新选择」。HTTP `INVALID_ACCOUNT_TYPE` 无拉丁字母。测试 `invalidAccountTypeResponseIsChinese` PASS。**032 已修，但同出口形态的其他中英混排未修。**
+7. **客人储值桥是否越权？** 仅 `customer.stored_value.topup/consume`，且须 `accountType=customer` 且 active。027 操作员答复保留。本席未跑客人钱包集成测试（§7）。
+8. **最高权限是否被职级 3 冒充？** `SupervisorReviewService.approve` 对 `top_authority` 另查 `isMasterAuthority`。测试 `topAuthorityReviewRequiresMasterSessionRatherThanRankAlone` PASS。
+
+ponytail-review（不单独作为 FAIL 项）：
+
+- `AccessService.java:1898` `delete:` 未使用的 `slug()`。
+- `EffectivePermissions.wildcard` 恒为 false：死字段。
+- `listGrantRequests(Integer)` 仍 `page(全量)`；控制器走分页重载。
+
+---
+
+## 6. 新意见
+
+### GR-01 — 授权审批等错误响应仍把英文枚举/缩写交给用户
+
+- **临时编号：** GR-01
+- **文件:行号：** `AccessService.java:1694`（主证）；同文件 `:515`、`:1061`、`:1491`（同类出口）
+- **2.1 分类：** 冲突（判定顺序：先冲突）
+- **工程缺陷：** 是
+- **2.3 依据类型：** 1（HTML 逐字）+ 4（可复现命令与 HTTP 输出）
+- **冻结判据：** PGM-001、PGM-066、GEN-002、GEN-015
+- **HTML 原文（PGM-001 / 治理地图「界面全中文」；PGM-066 已拍板同文）：**
+  > 展示给用户的一切文字，包括菜单、页面标题、字段名、状态、枚举取值、按钮、提示、报错……必须全部为中文，不得出现任何英文……可以不用中文的只有四样……单据号、设备编号、二维码内容、系统内部标识符。这四样之外，凡是展示给人看的文字一律中文，不得以“这是标识”为由保留英文。
+- **行为：** `GlobalExceptionHandler` 对已含汉字的消息**原样透传**（`FieldLabels.translate` 命中 CJK 则 return message）。因此 `normalizeDecision` 在非法 `decision` 时把「通过（approve）或驳回（reject）」送到 HTTP `message`。这与已修的 032 同一出口、同一强制规则，只是换了一条文案。
+- **不是 032 未修好：** 032 路径实测 `账号类型不允许，请重新选择`，无拉丁字母。本条是全模块复审新发现的同类残留。
+- **命令与输出（节选，完整在 §7）：**
+
+```text
+HTTP_CODE=VALIDATION_FAILED MSG=审批结论必须为通过（approve）或驳回（reject） LATIN=true
+HTTP_CODE=INVALID_ACCOUNT_TYPE MSG=账号类型不允许，请重新选择 LATIN=false
+HTTP_CODE=MASTER_AUTHORITY_REQUIRED MSG=删除配置须由 USB 最高权限审批 LATIN=true
+```
+
+`valid_until is required` 被译成「有效期至未填写」（LATIN=false），说明出口层对纯 `field is required` 有效，挡不住「中文句子里夹英文枚举」。
+
+- **影响（按具体行为，不写攻击词）：** 后台用户在授权申请复核传入无法识别的结论时，界面报错出现英文 `approve`/`reject`；岗位模板删除拒绝时出现英文 `USB`；非法薪资币种时出现 `CNY`/`USD`。与「报错必须全中文」直接矛盾。API 字段名 `approve`/`reject` 本身可作为内部标识留在请求体；**展示给人的 message 不能再列出英文取值。**
+
+无其他新意见。旧 33 个 ID 本席不重开；031/032 行为已被本轮测试钉住为已修。
+
+---
+
+## 7. 测试命令、输出与未跑边界
+
+### 7.1 环境
+
+- 只 export 三个变量：`COMPLEX_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/complex_test`，用户名来自 `/etc/complex/complex.env` 的同名键，库名强制改为 `complex_test`（未 `source` 整个 env）。
+- 编译输出 `-d` 到 `.scratch/m006-b5-r3-grok-<pid>/classes`，不写 `backend/target`。
+- JDBC 测试只用连接内 TEMP 表，不写共享业务库 `complex`。
+- 为避开 StaffService 拖入 gate/terminal 整树，scratch 副本去掉 2 个 `@Test`（`employeeApprovalWithoutCredentialAdapterWritesNoAccount`、`registrationCreatesAccountOnlyAfterCompanyApproval`）。原测试文件未改。同文件仍保留并跑过 `manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval`。
+
+### 7.2 翻译/HTTP 探针
+
+`javac` 当前 Access/DTO/异常出口等源后执行 `TranslateProbe`。exit 0。完整输出：
+
+```text
+RAW=valid_until is required
+TRN=有效期至未填写
+HAS_LATIN=false
+RAW=expires_at is required
+TRN=到期时间未填写
+HAS_LATIN=false
+RAW=request_type is invalid
+TRN=申请类型格式不正确
+HAS_LATIN=false
+RAW=status is invalid
+TRN=状态格式不正确
+HAS_LATIN=false
+RAW=authentication is required
+TRN=登录凭证未填写
+HAS_LATIN=false
+RAW=审批结论必须为通过（approve）或驳回（reject）
+TRN=审批结论必须为通过（approve）或驳回（reject）
+HAS_LATIN=true
+RAW=删除配置须由 USB 最高权限审批
+TRN=删除配置须由 USB 最高权限审批
+HAS_LATIN=true
+RAW=薪资币种当前仅支持人民币（CNY），实际为：USD
+TRN=薪资币种当前仅支持人民币（CNY），实际为：USD
+HAS_LATIN=true
+RAW=账号类型不允许，请重新选择
+TRN=账号类型不允许，请重新选择
+HAS_LATIN=false
+HTTP_CODE=VALIDATION_FAILED MSG=审批结论必须为通过（approve）或驳回（reject） LATIN=true
+HTTP_CODE=INVALID_ACCOUNT_TYPE MSG=账号类型不允许，请重新选择 LATIN=false
+HTTP_CODE=UNAUTHENTICATED MSG=登录凭证未填写 LATIN=false
+HTTP_CODE=VALIDATION_FAILED MSG=有效期至未填写 LATIN=false
+HTTP_CODE=MASTER_AUTHORITY_REQUIRED MSG=删除配置须由 USB 最高权限审批 LATIN=true
+```
+
+### 7.3 单元 + JDBC
+
+`JAVAC_EXIT 0`，73 个源文件，scratch 内 98 个 class。随后反射执行 `@Test`：
+
+```text
+RESULT 99 PASS / 0 FAIL / 0 SKIP
+JAVA_EXIT 0
+```
+
+关键条目：
+
+- `M006SecurityRegressionTests#companyRequestsHaveIndependentResultsAndRetryOnlyOnce` PASS
+- `M006SecurityRegressionTests#invalidAccountTypeResponseIsChinese` PASS
+- `M006JdbcRegressionTests` 12 个方法全部 PASS（含指纹、分页、软删除、消费回滚、政策 round-trip）
+
+99 条 PASS 名单已在本次命令 stdout 中逐行打印（本报告不重复 99 行）。失败数为 0。
+
+### 7.4 明确未跑
+
+- `M006SecurityRegressionTests` 中依赖 `StaffService` 的 2 个方法（注册审批闭环）。手工建号拒绝路径已跑。
+- `AccessControllerTests`、前端 `M006Access*.test.tsx` / `WalletPage` 等。
+- 全量 `mvn test` / 共享 `backend/target` 编译。
+- 生产库 `complex`。
+- 客人储值支付通道真实厂商（027 限定未配置则拒绝；本席未跑 `M006StoredValue*`）。
+- 并发真实多线程 JDBC 锁（`lockGrantAuthorizationFacts` 源码为 `LOCK TABLE ... IN SHARE MODE`，行为由 JDBC 测试覆盖行锁/事务，不宣称压测）。
+
+未跑不等于通过。
+
+---
+
+## 8. 结论
+
+| 项 | 值 |
+|---|---|
+| 审计结论 | **FAIL** |
+| 原因 | GR-01：用户可见报错仍含英文枚举/缩写，与 PGM-001/066 冲突 |
+| 旧 33 ID | 本席不重开；031/032 本轮测试为已修 |
+| 添加项 | 无新添加；027 已答复保留 |
+| HTML 人闸 | 未触发 |
+| 锁外判据 | 未主张 |
+
+B3 已执行 3 次。本席只给审计结论 FAIL，不改 ledger、不修代码。
diff --git a/docs/audit-2026-09/modules/M006-b5-r3-self-raw.md b/docs/audit-2026-09/modules/M006-b5-r3-self-raw.md
new file mode 100644
index 00000000..6d4efc80
--- /dev/null
+++ b/docs/audit-2026-09/modules/M006-b5-r3-self-raw.md
@@ -0,0 +1,309 @@
+# M006 B5-r3 Codex 全模块复审原始报告
+
+版本：V0.01
+模块：M006 `10-backend-main-access-01`，6/322。
+步骤：B5；ledger IN_PROGRESS；B3已执行3次；方案复核已修2；运行失败0。
+审计结论：FAIL（本方完整审计共1条新意见CX-B5-01）。
+CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
+
+## 输入与独立顺序
+
+控制提示词V0.17，SHA256 c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21。
+M006原14文件与52冻结判据均完整审阅；实际当前源总3262行，不将原方案2988有效行指标误称为当前物理行。
+B4-r3内容/范围双PASS已完成；findings V0.63 SHA728d950f7bc0f1c89300d250a9d8c89430b207e89d883fee7f7fb48c5dd7c254，33个旧ID全部出队。
+先于本方审阅派Grok，2026-09-06 04:33:58 UTC。第一次5分钟检查04:39:12 UTC仍运行，session44329，真实退出码尚未知。
+本方完整报告落盘前没有读取本次Grok raw正文/意见或CLI原始JSON。Grok回执适配器只暴露指定路径和真实exit；当前无Grok结论，不能冒称双PASS/FAIL。
+此次为修复后全模块复审，未发现一条就停止，未只重看031/032或修复diff。B5期间未改业务代码、测试源、三HTML或锁。
+延续本会话已读取技能的规定用法：grilling的自问与反例、systematic-debugging的真实数据流/最小复现、receiving-code-review先核事实及verification-before-completion查实证；不调第三席、不用旧票替代新审。
+
+## 14文件完整覆盖
+
+| 文件 | 当前行数/字节 | SHA256 | 全文核对结果 |
+|---|---|---|---|
+| backend/src/main/java/com/complex/platform/access/AccessActor.java | 11 / 223 | 5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a | 可信认证主体数据载体；六字段组织/商户/状态传递，无独立登录执行入口。 |
+| backend/src/main/java/com/complex/platform/access/AccessController.java | 299 / 15252 | 42e79bc67140cf31ad60cfb5a930b56b023f47e5cf70106d109b8482a0fa8d0c | 全部Controller入口与权限、请求编号、路径目标覆盖；审批review入口147—155为新反例的真实入口。 |
+| backend/src/main/java/com/complex/platform/access/AccessCredentialProvisioner.java | 12 / 335 | c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b | 既有凭证适配器契约；与Service必需依赖前置检查一致。 |
+| backend/src/main/java/com/complex/platform/access/AccessGrantPositionSummary.java | 10 / 232 | 08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df | 岗位授权统计数据记录；数量与频繁标记来自Service政策计算。 |
+| backend/src/main/java/com/complex/platform/access/AccessGrantRequestRecord.java | 35 / 934 | b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89 | 授权申请快照及状态/人员/时间记录；permissionCodes防空并复制。 |
+| backend/src/main/java/com/complex/platform/access/AccessGrantSummary.java | 9 / 219 | 7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96 | 汇总输出类型，独立展示岗位与人员统计。 |
+| backend/src/main/java/com/complex/platform/access/AccessGrantUserSummary.java | 8 / 155 | 80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e | 人员授权汇总记录，未做额外业务判断。 |
+| backend/src/main/java/com/complex/platform/access/AccessRepository.java | 259 / 10273 | c4f48cc0d0bd62bfbc356eb531f18e990cc96f8de5a04dac2e746a4190a6f680 | 完整仓储接口/默认方法；授权事实锁、指纹、分页、来源替换的契约与直接JDBC实现对应。 |
+| backend/src/main/java/com/complex/platform/access/AccessService.java | 1946 / 97674 | 786c6c697e2d13b30e05731ca7585c5c8d3a00376cc045308ffca34f250c0044 | 完整1946行：模板、角色、岗位、临时授权、复核、账号、薪资/职级、统计、校验与私有辅助。发现CX-B5-01，其余本轮未形成新意见。 |
+| backend/src/main/java/com/complex/platform/access/AccessUser.java | 37 / 1042 | 717ebbd9d9ce56217a6045f20b42f47ec4aa76fdd2a14ac97c6b49e94387ceb1 | 账号输出字段；手机号摘要JsonIgnore；身份与账号状态非人事状态。 |
+| backend/src/main/java/com/complex/platform/access/AuthorizationService.java | 496 / 22963 | 1260154b681dd7027b17f3d371b2e0304ca4df034204e885b4dd743bd271e4ad | 完整496行：主体解析、DENY优先、一次性消费、审批级别、组织/商户/平台范围与新请求范围。 |
+| backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java | 48 / 2000 | 8d7ea478ed913838609ec974b06152b744191d38c3ecd5844c7fb3e086a92f39 | 开发凭证实现只在dev/test profile；缺账号拒绝、状态激活遵循真实账号。 |
+| backend/src/main/java/com/complex/platform/access/EffectivePermissions.java | 23 / 882 | 68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e | 有效权限输出记录，集合复制；具体wildcard由Authorization给false，不凭注释授予。 |
+| backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java | 69 / 3103 | 06a4e3f9f09c2902eabb9d53161330b05b1fdfe3c7f2ae5821c129ed121a082e | JDBC凭证实际用户更新、密码编码和登录状态；找不到账号时报错，不伪成功。 |
+
+## 全部52冻结条目逐项检查
+
+“本14文件无该业务执行器”表示按模块载体检查后无可归责实现，不是删除判据，也不是该业务全仓通过。
+
+| 冻结ID | 本次核对与证据落点 |
+|---|---|
+| GEN-001 | Controller公开入口对应账号、模板、角色、岗位和授权审批；本轮只复核既有实现，不把命名或私有辅助方法的风格意见登记成行为缺陷。 |
+| GEN-002 | 98项当前源码行为检查通过；新增无效审批结论反例实际进入Controller→Service→异常处理器并复现报错文案问题。全后端test-compile在B3-r3实跑exit0，相关源码未变。 |
+| GEN-003 | 本轮重新编译60个真实源/测试文件；98项覆盖正常与错误分支；新增反例编译51源成功、执行断言失败，未用编译失败冒充BUG复现。 |
+| GEN-004 | 账号类型、组织、商户、状态、密码、授权目标/时间、权限列表均在写入前校验；reviewGrant待审记录及申请人/受益人检查先于审批落地。 |
+| GEN-005 | Controller按动作校验权限；Authorization按真实主体、DENY优先、有效期、一次性来源检查；平台管理员日常权限桥不自动授予高风险权限；33项专门回归覆盖旧问题。 |
+| GEN-006 | 修改入口@Transactional，授权事实锁覆盖审批事实与落地；岗位派生来源按关系替换，独立来源保留；凭证配置缺失拒绝账号成功返回。 |
+| GEN-007 | 当前重复请求按主体/组织隔离，真实Controller两组织同编号结果独立、各自重试只生效一次；遗留p记录三状态均拒绝混用或再次执行；一次性权限使用原子消费。 |
+| GEN-008 | AccessActor/UserRecord两种认证主体使用一致范围；DTO与接口字段按现状检查，路径用户ID覆盖请求体目标；未增加账号类型或授权申请种类。文案例外见CX-B5-01。 |
+| GEN-009 | 未知主体/缺少组织/凭证服务缺失/错误时间窗拒绝；旧请求记录返回可识别409而不伪成功；无效审批结论能够失败但提示仍含英文，见CX-B5-01。 |
+| GEN-010 | 密码经既有编码器存储；账号输出手机号摘要被@JsonIgnore阻断；日志记录结构化错误码；新旧请求结果不跨组织共享。 |
+| GEN-011 | 模板/授权/账号/岗位与职级写入关联操作编号及成功/失败事件；授权复核记录申请、审批和生效状态，查询授权汇总留痕。 |
+| GEN-012 | 授权清单数据库分页及后台统计窗口；参考字典返回真实条数，不把50条截断伪装完整；本轮无性能基准或生产容量声明。 |
+| GEN-013 | 全后端test-compile B3-r3真实exit0，58条本模块历次修复业务/测试路径仍与最终tree一致；本轮60源javac和98测试exit0。未把未跟踪的合法B3依赖人为移除后制造构建失败。 |
+| GEN-014 | M006这14文件无迁移源；直接依赖V386来源保全修复在B3-r2完成，当前源码未改；本轮不执行共享数据库迁移，不声称已部署。 |
+| GEN-015 | FAIL：授权审批无效decision报错保留approve/reject两种英文枚举；真实错误响应复现。除该新意见，已修账号类型错误提示本轮回归通过。 |
+| GEN-016 | 核对当前方法、路径、版本和实际测试输出，未以旧PASS替代本轮；当前模块为IN_PROGRESS且未发布，不宣称全仓完成。 |
+| GEN-017 | CX-B5-01按已冻结全中文原文判冲突，非添加业务；33条旧ID已B4双PASS出队，B5不自行修复、不重新解释批准规则。 |
+| GEN-018 | 主张使用冻结HTML、真实可执行源和临时验证命令；未用工具名、惯例或旧审计结论充当最终行为证据。 |
+| BEQR-157 | AccessUser账号状态是认证状态，不替代员工待入职/在职等人事状态；人工账号创建不能提前激活注册员工，注册审批对接回归通过。 |
+| BEQR-182 | 14文件没有考勤日结果计算；本模块只承载主体与权限，无另发明出勤/休息口径。 |
+| BEQR-183 | 14文件没有设备领取打卡执行器；无把账号登录当打卡的实现，具体考勤留在其既有模块。 |
+| BEQR-184 | 14文件不定义请假类型或审批单；未添加请假类型。 |
+| BEQR-185 | 14文件不执行请假/销假或薪资冲回；授权申请pending/applied不是请假状态，未混用。 |
+| BEQR-186 | assignStaffLevel/resolvePositionGrant单独要求基础工资，负数拒绝且按分存储；未在此模块重写绩效/扣款/税率。 |
+| BEQR-187 | 当前模块无现金提成发放入口或计算逻辑；职级/岗位赋值不等于发放。 |
+| BSM-001 | 组织/商户归属和后台授权政策有现有来源；政策窗口、阈值、风险级别及上下限从已有配置读取；查询参数不能覆盖政策。 |
+| BSM-003 | 模板修改、存档、恢复和删除申请沿用既有权限及记录；直接删除接口拒绝，不直接抹掉历史。 |
+| BSM-009 | 账号、权限、岗位为业务准入基础；账户建立与凭证激活受审批和凭证配置前置约束。 |
+| BSM-028 | 重复请求和数据归属规则在Controller/Authorization/共享幂等边界对齐；模板目标、角色目标跨组织/跨商户拒绝，031本轮实际回归通过。 |
+| BSM-029 | AccessActor/User保存组织/商户关联，职级和员工注册直接依赖保持；本模块未建立个人手机设备绑定替代记录。 |
+| BSM-073 | 普通商户只能列出/操作本商户用户与授权；平台列表视角保留单独判定；本模块不处理费用异议/投诉关闭。 |
+| BSM-084 | 有效权限本人查询无消费一次性权限；他人查询需权限且同组织/商户范围；本模块不提供读取其他员工薪资的直接页面或入口。 |
+| BSM-085 | 注册账号必须通过公司/商户既有审批后才创建；人工创建员工提示走注册流程；本模块没有把代订记名或爽约政策写成授权新规则。 |
+| BSM-086 | 本模块不计算班次、考勤或请假；岗位分配与班次实际执行分离，没有在此修改月度归属口径。 |
+| BSM-087 | 基础工资单独传入、职级不自动决定具体金额；本模块不实现月结可见性/绩效工资或税款算法。 |
+| BSM-088 | 不存在现金现抽/人员服务商品/营销打卡实现；权限编码引用不等于新增相应业务。 |
+| BSM-114 | 平台/员工/商户/客人认证主体按已有类型处理；共享幂等范围对客人及员工采用真实主体，商户保持商户范围；未在本模块建立新终端或支付入口。 |
+| BSM-115 | 授权来源/角色/模板/岗位保留关联ID，审计事件关联操作编号；无修改跨域收入、库存、票务的业务判定。 |
+| BSM-120 | 权限粒度按permissionCode；职级与具体薪资分离；后台开关未修改；仅检查M006对既有注册、归属、审批接口的承接，不声称涵盖全仓所有确认规则。 |
+| PGM-001 | FAIL：全中文错误提示出现英文审批枚举，CX-B5-01；归属、责任与重复提交相关部分本轮已有真实测试支持。 |
+| PGM-008 | 员工账号与注册审批、凭证建立相关连接存在；私人/工作手机行为非本14文件实现。 |
+| PGM-022 | 当前主体project scope与组织范围执行默认拒绝；本模块没有修改空间分配或把商户代替项目。 |
+| PGM-024 | 岗位和职级分配保留目标员工/组织；没有低消、容量、库位或排班计算实现。 |
+| PGM-026 | 组织、商户、角色归属在服务层校验；更改授权不转移既有财税主体关系；无把普通授权配置当公司权责审批。 |
+| PGM-032 | 员工注册状态及凭证建立使用现有StaffService衔接；开发/生产凭证适配器按profile生效，缺配置不可报告建号成功。 |
+| PGM-033 | 公司审批前不创建已激活员工账号；手工创建员工不能替代规定注册。设备绑定/换机/柜子扫码非此模块载体。 |
+| PGM-034 | Authorization.requirePlatformComplianceScope限制商户/外包主体；本模块无会话保存/清理接口，未改留存政策。 |
+| PGM-035 | 当前无提成发放、营销打卡或人员服务商品执行路径；权限本身不承诺开展这些业务。 |
+| PGM-036 | 本模块只提供主体和权限事实给设备端，未新增设备种类/绑定状态或核销执行器。 |
+| PGM-048 | 商户角色与目标用户组织/商户一致性校验，商户列表隔离；平台聚合权限与商户范围分开，正常商户授权业务未整体禁用。 |
+| PGM-059 | AccessUser及账号创建输出不返回phoneLookupDigest；手工输入手机号摘要不落入账户；凭证存密码摘要；客人查询专门通道不在本14文件另行实现。 |
+| PGM-066 | FAIL：CX-B5-01违反已确认全中文口径；其他本模块可执行部分按权限点、时间戳和自动留痕核对，未改开关或支付时间规则。 |
+
+## 三条审阅路线及必要直接依赖
+
+1. 第一性原理：从主体是谁、属于哪个组织/商户、请求针对谁、实际授予哪些权限、哪个记录产生结果及失败是否落部分数据逐层追踪。全14文件数据类型→Controller→Service→Authorization/仓储→凭证与事件路径逐个核对。审计依据为冻结HTML、当前可执行产物与下述命令；不以业务注释自身作为依据。
+2. 顺从式：52条逐一按载体核对，实际98测试覆盖账号创建与注册审批、角色与模板授予/编辑、岗位来源替换、临时授权时间窗、审批快照/申请人与受益人、权限有效集/一次性消费、范围过滤、政策统计与归档/删除审批；当前031/032均实际通过。接口返回phoneLookupDigest不可见，日常管理员权限保留，高风险权限需显式授予。
+3. 反例：未知认证主体、缺组织、其他商户目标、超出本人权限、审批后角色变动、受益人自批、缺凭证提供者、时间倒置、一次性消费竞争、同编号两组织重复提交、旧幂等记录OK/IN_PROGRESS/FAILED_RETRYABLE均有可运行检查。另主动从无效审批decision走真实错误处理器，发现CX-B5-01；随后继续覆盖其余文件和判据后才落本报告。
+直接依赖包括access/dto、IdempotencyService/CallerScopeResolver、FieldLabels/GlobalExceptionHandler、SupervisorReviewService、AuditEventService、StaffService与现存相关测试。本轮没有另开后续模块。此前同批读过真实认证Filter、CustomerService/IntegrationService/第三方接收及一体机心跳：正常公开写入口使用自身记录或先要求客人身份；未据推测宣称共享范围修复破坏公开接口。现存UserRecord/AccessActor范围一致及商户/无认证范围4断言在B4实跑通过。
+新范围使用既有用户ID长度前缀+组织，结果存为caller:加64位哈希共71字符，原operation_id列128，未另加表。遇到旧无所有者p记录不猜其归属，拒绝复用和重复执行，保留原记录。
+
+## 完整新意见
+
+### CX-B5-01 — 无效授权审批结论的报错仍显示英文枚举
+
+- 位置：backend/src/main/java/com/complex/platform/access/AccessService.java:1689—1694（根因）；同文件703—724（真实执行路径）；AccessController.java:147—155（入口）；common/api/GlobalExceptionHandler.java:49—61与FieldLabels.java:504—529（真实响应处理）。
+- 严重程度：P2；2.1分类：冲突；工程缺陷：是。仅临时ID，未进入新一批B2定稿，不在B5修复。
+- 冻结判据：PGM-001、PGM-066、GEN-015；2.3依据类型：4（下方完整可复现命令输出），并以现有冻结HTML引文/当前可执行源定位。
+- 操作与实际结果：有审核权限的后台人员对待审批授权提交无效decision时，Controller先校验两项审核权限和请求编号，Service找到pending申请并校验申请人/受益人后，normalizeDecision抛VALIDATION_FAILED。实际错误响应message为“审批结论必须为通过（approve）或驳回（reject）”。
+- 原因：业务抛错直接拼写给人看的英文审批枚举；FieldLabels对已经含中文的话术按原文返回。它不是仅日志、JSON字段名或内部编码；approve/reject在此明确作为错误提示的枚举取值展示，落在冻结HTML“提示、报错、枚举取值全部中文”的范围。
+- 业务影响：后台审核人员遇到无效选项得到混合语言提示；本反例未显示审批被错误执行，影响限定在错误提示，不夸大为资金/客人扣款问题。
+- 最小修复方向（尚未执行）：保留允许值approve/reject及错误码，仅把该校验错误的显示文字改为中文“审批结论必须为通过或驳回”，保留可失败的真实错误响应检查。
+- 与旧M006-032区分：032的定稿边界只针对账号类型报错，B4已双PASS出队；本意见是授权审批分支，不擅自当作032仍在队列。到B3上限后应交操作员决定是否恢复，而非无ID直接修改。
+- PGM-001原文关键约束：所有终端给用户看的提示/报错/枚举取值必须全部中文，仅单据号、设备编号、二维码内容和系统内部标识符可保留。PGM-066重复确认同一要求；未新增判据。
+
+## 实际验证命令及输出
+
+### 当前完整核心回归，B5-r3重新执行
+命令在160主工作区执行，临时源码/runner/classes仅.scratch临时目录，退出自动清理；生产源重新javac，未改共享target。
+```powershell
+@'
+# V0.01 M006-r3 current-source behavior checks
+from pathlib import Path
+import subprocess,tempfile
+runner="package com.complex.platform.access;\npublic class M006Runner {\n public static void main(String[] args)throws Exception {\n int passed=0,failed=0;\n for(String name: args){var c=Class.forName(name);int localPass=0,localFail=0;\n for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)){\n var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);var t=ctor.newInstance();\n try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}\n m.setAccessible(true);m.invoke(t);passed++;localPass++;\n }catch(java.lang.reflect.InvocationTargetException e){failed++;localFail++;System.out.println(\"FAIL \"+name+\".\"+m.getName()+\": \"+e.getCause());}\n finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}\n }\n System.out.println(name+\" \"+localPass+\" PASS / \"+localFail+\" FAIL\");\n } System.out.println(\"RESULT \"+passed+\" PASS / \"+failed+\" FAIL\");if(failed>0)System.exit(1);\n }\n}"
+with tempfile.TemporaryDirectory(prefix="m006-r3-tests-",dir=".scratch") as d:
+ root=Path(d).resolve();cpfile=root/"classpath"
+ r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
+ if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
+ cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
+ src=root/"M006Runner.java";src.write_text(runner,encoding="utf-8");out=root/"classes";out.mkdir()
+ base=Path("backend/src/main/java/com/complex/platform");tests=Path("backend/src/test/java/com/complex/platform")
+ sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))+list((base/"common/idempotency").glob("*.java"))
+ sources += [base/p for p in ["access/staff/StaffService.java","common/api/FieldLabels.java","common/api/GlobalExceptionHandler.java","governance/review/SupervisorReviewService.java","evidence/AuditEventService.java"]]
+ sources += [tests/p for p in ["support/InMemoryAuditEventRepository.java","support/InMemoryIdempotencyRepository.java","support/TestSecurityActors.java","access/M006SecurityRegressionTests.java","access/AccessServiceTests.java","access/AuthorizationServiceTests.java","common/idempotency/IdempotencyServiceTests.java"]]+[src]
+ r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources]],capture_output=True,text=True)
+ print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
+ if r.returncode:raise SystemExit(r.returncode)
+ r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner","com.complex.platform.access.M006SecurityRegressionTests","com.complex.platform.access.AccessServiceTests","com.complex.platform.access.AuthorizationServiceTests","com.complex.platform.common.idempotency.IdempotencyServiceTests"],capture_output=True,text=True)
+ print(r.stdout,r.stderr);raise SystemExit(r.returncode)
+
+'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
+```
+真实输出/exit：
+```text
+JAVAC_EXIT 0 SOURCES 60
+ Note: backend/src/main/java/com/complex/platform/access/staff/StaffService.java uses unchecked or unsafe operations.
+Note: Recompile with -Xlint:unchecked for details.
+
+com.complex.platform.access.M006SecurityRegressionTests 33 PASS / 0 FAIL
+com.complex.platform.access.AccessServiceTests 35 PASS / 0 FAIL
+com.complex.platform.access.AuthorizationServiceTests 21 PASS / 0 FAIL
+com.complex.platform.common.idempotency.IdempotencyServiceTests 9 PASS / 0 FAIL
+RESULT 98 PASS / 0 FAIL
+ Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
+WARNING: A Java agent has been loaded dynamically (/home/james/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.10/byte-buddy-agent-1.18.10.jar)
+WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
+WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
+WARNING: Dynamic loading of agents will be disallowed by default in a future release
+OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
+
+
+EXIT_CODE=0
+```
+
+### 新反例：真实Controller、Service和错误响应
+```powershell
+@'
+# V0.01 temporary B5 read-only verification
+from pathlib import Path
+import subprocess,tempfile
+probe="\n    @Test void b5InvalidDecisionResponseIsChinese() throws Exception {\n        when(repository.listPermissions()).thenReturn(List.of(\n            new PermissionDefinition(\"access.grant_request.review\", \"审核授权\", \"access\", \"medium\", false, \"active\"),\n            new PermissionDefinition(\"governance.supervisor_review.approve\", \"主管审核\", \"governance\", \"medium\", false, \"active\")));\n        requests.put(\"pending-language\", new ObjectMapper().readValue(\n            \"{\\\"id\\\":\\\"pending-language\\\",\\\"status\\\":\\\"pending\\\",\\\"targetUserId\\\":\\\"target\\\",\\\"requestedBy\\\":\\\"requester\\\",\\\"requestType\\\":\\\"user_role\\\"}\",\n            AccessGrantRequestRecord.class));\n        var controller = new AccessController(service, authorization);\n        ApiException failure = null;\n        try {\n            controller.reviewGrant(\"pending-language\", new AccessGrantReviewRequest(\n                \"b5-invalid-decision\", \"质量检查\", List.of(), \"wait\", \"测试无效选项\"));\n        } catch(ApiException e) { failure = e; }\n        assertThat(failure).isNotNull();\n        assertThat(failure.code()).isEqualTo(\"VALIDATION_FAILED\");\n        var handler = new com.complex.platform.common.api.GlobalExceptionHandler();\n        var method = handler.getClass().getDeclaredMethod(\"handleApiException\", ApiException.class);\n        method.setAccessible(true);\n        var response = (org.springframework.http.ResponseEntity<?>) method.invoke(handler, failure);\n        var body = (com.complex.platform.common.api.ApiResponse<?>) response.getBody();\n        System.out.println(\"B5_ERROR_RESPONSE=\"+body.message());\n        assertThat(body.message()).doesNotContainPattern(\"[A-Za-z]\");\n    }\n"
+runner="""package com.complex.platform.access;
+public class M006Runner {
+ public static void main(String[] args)throws Exception {
+ int passed=0,failed=0;
+ var c=M006SecurityRegressionTests.class;
+ for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)&&m.getName().startsWith("b5Invalid")){
+ var t=c.getDeclaredConstructor().newInstance();
+ try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}
+ m.setAccessible(true);m.invoke(t);passed++;System.out.println("PASS "+m.getName());
+ }catch(java.lang.reflect.InvocationTargetException e){failed++;System.out.println("FAIL "+m.getName()+": "+e.getCause());}
+ finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}
+ }
+ System.out.println("RESULT "+passed+" PASS / "+failed+" FAIL");if(failed>0)System.exit(1);
+ }
+}"""
+with tempfile.TemporaryDirectory(prefix="m006-b5-self-",dir=".scratch") as d:
+ root=Path(d).resolve();cpfile=root/"classpath"
+ r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
+ if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
+ cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
+ original=Path("backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java").read_text()
+ test=root/"M006SecurityRegressionTests.java";test.write_text(original[:original.rfind("}")]+probe+"\n}\n",encoding="utf-8")
+ src=root/"M006Runner.java";src.write_text(runner,encoding="utf-8");out=root/"classes";out.mkdir()
+ base=Path("backend/src/main/java/com/complex/platform")
+ sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))
+ sources += [base/"common/api/FieldLabels.java",base/"common/api/GlobalExceptionHandler.java",base/"common/idempotency/IdempotencyService.java",base/"governance/review/SupervisorReviewService.java",base/"evidence/AuditEventService.java"]
+ sources += [Path("backend/src/test/java/com/complex/platform/support/InMemoryAuditEventRepository.java"),Path("backend/src/test/java/com/complex/platform/support/InMemoryIdempotencyRepository.java"),test,src]
+ r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources]],capture_output=True,text=True)
+ print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
+ if r.returncode:raise SystemExit(r.returncode)
+ r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner"],capture_output=True,text=True)
+ print(r.stdout,r.stderr);raise SystemExit(r.returncode)
+
+'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
+```
+真实相关stdout节选（仅省略JVM/Mockito环境提示）：
+```text
+JAVAC_EXIT 0 SOURCES 51
+B5_ERROR_RESPONSE=审批结论必须为通过（approve）或驳回（reject）
+FAIL b5InvalidDecisionResponseIsChinese: java.lang.AssertionError:
+Expecting actual:
+  "审批结论必须为通过（approve）或驳回（reject）"
+not to contain pattern:
+  "[A-Za-z]"
+RESULT 0 PASS / 1 FAIL
+EXIT_CODE=1
+```
+编译成功后仅断言英文展示失败，未把环境失败视为产品缺陷。夹具只提供待审记录和已有审核权限；没有mock错误文案或FieldLabels，后者均为当前真实源码。
+
+### 同批构建与范围校验的准确界限
+B3-r3实跑：`mvn -o -q -f backend/pom.xml test-compile -DskipTests`，真实exit0，空输出；本轮未重复宣称执行全后端测试。
+B4-r3实跑当前Authorization及接口范围probe：JAVAC_EXIT0；SCOPE_CHECK 4 ASSERTIONS PASS；此前后源码未变。
+本B5再次用git diff --name-only f49ef25cdadc0d4572f169d8c33b5e971c21f024 c77d4453db7eb9b92388de630fdd5121cdef7959列出历次修复路径，排除既定审计/.scratch产物后逐一git show after:path比较当前字节：CURRENT_REPAIR_PATHS 58，DIFFER_FROM_R3_AFTER []。
+当前HEAD 395e6fbab027020da1c38237fad17e8edcd7a132。
+三HTML均逐字等于HEAD，哈希：
+- business-enum-quick-reference-v0.1.html：4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66
+- business-system-map-for-operations-v0.1.html：3edff56a7640565d08983621e34d726d149261e70fd747ecb88b8e9cc27774ea
+- platform-governance-map-for-operations-v0.1.html：9864216e609df2f319fa2cf7e51b61f06003ce45dff64a1a4098e71435865a68
+本轮未执行共享数据库迁移/写数据、真实支付商户端到端交易、全终端浏览器或安卓真机测试；不将这些未跑项说成通过。支付渠道真实生产配置不因M006回归而完成。并发专项这里只实跑既有原子消费/重试行为检查，没有新一轮共享数据库并发压测。
+
+## Grok实际投递命令及未完成项
+
+以下为本次实际执行的命令全文；-p拼接的是上方hash对应的控制提示词。原始CLI stdout/stderr由既有适配器进程内缓冲并丢弃。本方落盘时EXIT_CODE未知，Grok报告仍未读。
+```powershell
+$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
+$auditPrompt = '仅执行M006第3次修复后的B5全模块审计，模块6/322，主工作区main，ledger IN_PROGRESS，B3=3。本轮B4已双席内容/范围PASS，findings V0.63 SHA728d950f7bc0f1c89300d250a9d8c89430b207e89d883fee7f7fb48c5dd7c254，33个ID当前队列均否。严格按plan/modules.md中M006原有14文件和criteria-lock.md该模块全部52冻结条目，第一性原理、顺从式、反例三线完整复审当前整个模块；不得只看上轮diff或031/032，不得以旧B4或旧B5票代替本次全审。当前最近完整after tree c77d4453db7eb9b92388de630fdd5121cdef7959；源码与该tree一致（审计产物后续有流程记录）。AccessService现V0.13、AuthorizationService现V0.04；其余既有源正常纳入完整阅读。允许为事实核对追必要直接依赖、HTML与现存测试；B3合法新增依赖就在当前工作区，不能刻意删掉其源后声称整体构建失败，冻结A范围不得改。完整审完、把本次所有意见一次写齐后才给PASS/FAIL；发现第一条仍继续审，不中途修/路由。本次两席独立：禁止读取当前M006-b5-r3-self-raw.md或其内容，本方也在自己的完整raw落盘前不读你报告。只读业务源码/HTML/锁/台账/问题；临时验证只在.scratch隔离并清理自己的文件，不编译共享target、不写共享业务库，不新建额外审计产物，不派第三席。实际预检合同适用技能，不把缺技能写为已调用。报告必须有14文件逐项覆盖及全部52条检查的结果、具体源码和行为证据、所有新意见的临时ID/四分类/依据类型，及真实测试命令输出和明确未跑边界；无条件通过概念不适用。操作员明确本工作是对自己写/修改的代码进行开发调试和BUG质量修复，按具体行为与影响描述。完整报告写到指定 docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md，存在则保留旧内容完整追加本次报告，不存在创建V0.01；写你独立实算的CRITERIA-LOCK-SHA256独占行。CLI回执只由适配器生成，不承担标记格式。以下完整V0.17继续适用：
+' + [IO.File]::ReadAllText('D:\AI\ccwork\160-audit-prompt.md', [Text.Encoding]::UTF8)
+@{prompt=$auditPrompt} | ConvertTo-Json -Compress | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -c ''import sys,json,subprocess,pathlib
+root=pathlib.Path("/home/james/complex-management").resolve()
+p=root/"docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md"
+def meta():
+ if not p.exists(): return None
+ s=p.stat(); return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
+before=meta()
+prompt=json.load(sys.stdin)["prompt"]
+r=subprocess.run(["/home/james/.local/bin/grok","-p",prompt,"-m","grok-4.6","--reasoning-effort","xhigh","--output-format","json","--permission-mode","bypassPermissions","--no-subagents","--disable-web-search","--cwd",str(root)],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
+code=r.returncode
+err="GROK_EXIT" if code!=0 else "PATH_MISMATCH" if not p.resolve().is_relative_to(root) or p.is_symlink() else "REPORT_MISSING" if not p.is_file() else "REPORT_EMPTY" if p.stat().st_size==0 else "REPORT_STALE" if meta()==before else None
+print("ADAPTER_ERROR="+err if err else "REPORT_PATH=docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md")
+print("EXIT_CODE="+str(code))
+sys.exit(1 if err else 0)
+'''
+```
+指定结论出处：docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md。
+当前无三道人闸问题。下一步只在本报告完整落盘后、按5分钟周期取真实交付结果，再读Grok完整报告；两席完整意见收齐与B5后四值检查完成后，按B3=3上限登记BLOCKED-3ROUNDS并交操作员。本方FAIL不允许略过尚未完成的外审或自动执行第四次修复。
+
+落盘UTC：2026-09-06T04:41:55.657305+00:00
+
+
+## V0.02 — 接收Grok完整报告及终态记录（非新独立票）
+原V0.01的34586字节前缀/SHA72da471a14607a68d3c363a0a65c7ec426dfe7b2649da01da7f9b8f767f681a3完整保留。
+2026-09-06 04:49:42 UTC按5分钟周期检查取得真实EXIT_CODE=0；指定Grok报告21532字节/317行/SHA06506fb252baac014c9ed4d1f89c546b92479e54aa84289255d6b78c3498b0bb全文读完，审计FAIL，1条GR-01。此前原本方全文已封存，独立顺序未破坏。
+Grok GR-01与本方CX-B5-01共享审批文案主证，另指出515/1061/1491三处同类报错；本方接收后核对当前四处源码和既有错误翻译器，保留其完整范围，不将其意见缩成仅1694，不冒称它的探针是本方实跑。
+本方与Grok均FAIL，14文件及52条实际全量覆盖；Grok表标题对BEQR/PGM计数有文字笔误，但列中实际ID集合为7BEQR/13PGM，合计52，未因此遗漏原映射条目。其99测试明确省略两项StaffService测试，未跑项保留；本方实际98包含该两项。Grok报表只保留测试摘要而未列出73源完整javac参数/99行，故这99项是外席自述测试证据，不写成已在本方完整重放。
+B5后实算/sidecar/ledger/Grok四值：
+1: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
+2: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
+3: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
+4: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
+LOCK-AMEND-COUNT: 0
+三轮六份B4 raw经真实git diff --binary逐字包含校验全部true，完整树对/字节/SHA交付见findings V0.64。本轮仅登记终态，不修新意见。
+## M006 B5-r3完整双席意见与三轮交付（V0.64）
+
+两席本次整个模块均审完后统一判定：**B5 FAIL；模块 BLOCKED-3ROUNDS；B3已执行3次**。
+Grok于2026-09-06 04:49:42 UTC检查取得真实EXIT_CODE=0，报告317行/21532字节，SHA06506fb252baac014c9ed4d1f89c546b92479e54aa84289255d6b78c3498b0bb，全文已读；交付成功不等于审计通过。
+本方独立原始完整报告34586字节，SHA72da471a14607a68d3c363a0a65c7ec426dfe7b2649da01da7f9b8f767f681a3，先于读取Grok封存。原票FAIL，原1条意见保留；后续接收记录仅追加，不覆盖原票。
+
+### 本批全部意见（终态待恢复；未做B2定稿）
+| 原始来源 | 问题及真实影响 | 位置 | 原分类/工程缺陷 | 当前处理 |
+|---|---|---|---|---|
+| self CX-B5-01 | 无效审批结论报错给用户显示approve/reject，实际Controller→Service→错误响应复现；不改变审批结果 | AccessService.java:1694，入口147—155/703—724 | 冲突/是，PGM-001/066、GEN-015，类型4 | 原始意见保留，待操作员恢复M006后整批B2 |
+| Grok GR-01（与上条同类，完整保留其更宽范围） | 同上；另有岗位模板删除审核拒绝提示USB，以及两处薪资币种错误提示CNY/输入币种 | AccessService.java:515、1061、1491、1694 | 冲突/是，PGM-001/066、GEN-002/015，类型1+4 | 原始意见保留，四处合为一类文案问题的归并建议，尚未正式ID |
+
+原始源：[本方完整报告](modules/M006-b5-r3-self-raw.md)、[Grok完整报告](modules/M006-b5-r3-grok-raw.md)。
+本方接收后核对四处当前源码及GlobalExceptionHandler:49—61/FieldLabels:504—529；中文中夹英文原样透传。Grok额外三处的运行探针证据归属Grok，本方不冒称重跑了其99项测试。
+旧M006-031与032在本轮两席各自测试中通过，保持旧33ID全部队列否；不得把这批新问题直接塞入已出队旧ID或无ID修复。没有遗漏另一类新意见、没有新增业务规则、没有HTML申请。
+潜在最小变更仅四处提示文本；保留内部代码、允许的decision/币种值、权限与业务判断。恢复后仍须先整批B2双PASS再修，不在本轮突破三次上限。
+验证：本方当前源98项回归全通过；独立真实审批响应反例1失败；Grok报告99项所选单元/JDBC测试通过及报错探针确认英文，明确省略2个StaffService测试（本方98项中已经实际执行），不把两组数字相加称197个不同测试。未跑真实支付厂商、全端浏览器/安卓、全库迁移或完整mvn test。
+
+### B5后四值与锁
+四值均为 `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`：①当前实算；②sidecar；③ledger顶部；④本次Grok raw最后一条独占哈希行。LOCK-AMEND-COUNT=0，校验实际命令/输出在本次操作员回执与本方raw接收追加段。
+没有因失败修改锁、scope、模块映射或当前源码，三HTML仍与HEAD逐字相同。
+
+### 三轮完整diff交付
+以下所有六份raw都含对应两树**完整未过滤**git diff --binary；本次逐字包含校验均true，不仅是stat或节选。源码仍等于第三轮after，未回滚已通过修复。
+| B3轮次 | before tree | after tree | 完整diff字节及SHA256 | 双席原始diff报告 |
+|---|---|---|---|---|
+| 1 | f49ef25cdadc0d4572f169d8c33b5e971c21f024 | e845764656fb7f9c166026371371b720f10fde2a | 384478 / f8caaf8d47ea2993a9ea1d4b8a43e0c4f47246618d327e1a5410428234f6f5cc | [self](modules/M006-b4-r1-self-raw.md) / [Grok](modules/M006-b4-r1-grok-raw.md) |
+| 2 | 7fecb8e739b6b04475755fffbb8e106fa0759713 | 2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9 | 17644 / cc2fc0f92106bd49386038da79b4b1e1b862cf447bc6bc753322417efd413a41 | [self](modules/M006-b4-r2-self-raw.md) / [Grok](modules/M006-b4-r2-grok-raw.md) |
+| 3 | 11319ccfc6c0a3b609bc1cfdd4579766a7118705 | c77d4453db7eb9b92388de630fdd5121cdef7959 | 14687 / a04dfea8324fd7a11d5b5cfebf3585fd93c9cb32ec3cd05fde2bb8b610956717 | [self](modules/M006-b4-r3-self-raw.md) / [Grok](modules/M006-b4-r3-grok-raw.md) |
+
+停止原因：控制提示词V0.17第4节“第3次B3后……该次B4或B5审完仍FAIL→……状态记BLOCKED-3ROUNDS……是否重启由操作员另行下指令”。当前B5完整结束、两席均FAIL，故不自动回B2/B3、不加第四轮、不开始M007、不提交合并推送M006。
+需要操作员：新指令恢复M006；恢复后保留现有修复/原始报告，先处理本批全部意见，而非重开M001或全仓。
+
````

## 3. §5 闸 3 逐块归因

闸 3：每一行须能归因到 `findings.md` 已定稿且修复队列=是 的 ID。本轮唯一此类 ID = **M006-034**（含操作员②扩展）。

| 块 | 文件:hunk | 改动摘要 | 归因 |
|---|---|---|---|
| B1 | AccessService.java `@@ -512,7 +512,7 @@` | 人读文案 USB→「最高权限（U 盘密钥）」；码 `MASTER_AUTHORITY_REQUIRED`、`isMasterAuthority` 判断未改 | M006-034（515） |
| B2 | AccessService.java `@@ -1058,7 +1058,7 @@` | 人读文案去掉（CNY）与输入回显；`"CNY".equals(currency)` 与码 `UNSUPPORTED_CURRENCY` 未改 | M006-034（1061） |
| B3 | AccessService.java `@@ -1488,7 +1488,7 @@` | 同 B2 | M006-034（1491） |
| B4 | AccessService.java `@@ -1691,7 +1691,7 @@` | 人读文案去掉（approve）/（reject）；`List.of("approve","reject")` 与码 `VALIDATION_FAILED` 未改 | M006-034（1694） |
| B5 | StaffService.java `@@ -244,7 +244,7 @@` | 同类审批结论文案；码未改 | M006-034 + 操作员②（247）。文件在 `plan/modules.md` 属 **M008**（findings V0.66 误写 M007，不影响路径归因） |
| B6 | StaffService.java `@@ -1241,7 +1241,7 @@` | 同类；`Set.of("approve","reject")` 未改 | M006-034 + 操作员②（1244） |
| B7 | M006SecurityRegressionTests.java `@@ -642,4 +642,19 @@` | 新增 `invalidDecisionMessageIsChinese` | M006-034 验证性测试（闸 3 明文允许） |
| A1–A8 | `docs/audit-2026-09/**` 共 8 路径 / 11 hunk | 台账、ledger、第 3 轮 raw | §1.2 排除，不是业务修复 |

无归因不到 034 的业务行。无「顺手优化」「换测试框架」「补抽象」。

**范围归因结论：PASS**

## 4. 内容审计

### 4.1 第一性原理

1. **这段东西为什么必须存在？** 删掉 034 四处（及操作员点名的两处）文案修复：`GlobalExceptionHandler:49–61` 经 `FieldLabels.translate:504–529` 对已含 CJK 的消息**原样透传**，用户会再次看到 `USB` / `CNY` / `approve` / `reject`。这与 PGM-001 原文「展示给用户的一切文字……必须全部为中文，不得出现任何英文」及 PGM-066「界面全中文……提示、报错必须全中文」冲突。可以不用中文的只有单据号、设备编号、二维码内容、系统内部标识符；错误码与内部允许值 `approve`/`reject`/`CNY` 属内部标识，人读 `message` 不是这四样。
2. **声称行为有没有被实际执行验证？** 有。本席未采信 findings V0.66「同类34项全PASS」自述。本席在 `.scratch/m006-b4-r4-grok-verify*` 复制 `backend/target/classes`（不编译共享 target），删除被改 class 后对 after 源 `javac --release 21`，再跑测试；对照结束 `rmtree`。共享 `backend/target/classes` mtime 两次核对均未变（`1785725458.3775005`）；`AccessService.class` mtime 亦未变（`1788668049.7076316`）。未写业务库。
3. **哪条前提没验证就当真？** 未跑全后端 `mvn test`、真实支付、浏览器/安卓、全库迁移。515 的「U 盘」拉丁字母是否算英文，不靠印象，而靠三份 HTML 自身用词（类型 1）。StaffService:363 同类英文**不在本 diff、不在操作员②点名的两处**，本席不把它折进本轮 B4 FAIL（见 4.3）。

### 4.2 顺从式（对照拟修与冻结判据）

只改人读文案，保留错误码/允许值/权限/币种判断：

| 位置 | 错误码 | 判断未改 | after 人读文案 |
|---|---|---|---|
| AccessService:515 | `MASTER_AUTHORITY_REQUIRED` | `isMasterAuthority(...)` | 删除配置须由最高权限（U 盘密钥）审批 |
| AccessService:1061 | `UNSUPPORTED_CURRENCY` | `!"CNY".equals(currency)`；默认仍 `trimToDefault(..., "CNY")` | 薪资币种当前仅支持人民币，不支持所填币种 |
| AccessService:1491 | 同 1061 | 同 1061 | 同 1061 |
| AccessService:1694 | `VALIDATION_FAILED` | `List.of("approve","reject").contains(normalized)` | 审批结论必须为通过或驳回 |
| StaffService:247 | `VALIDATION_FAILED` | 周围通过/驳回分支未改 | 审批结论必须为通过或驳回 |
| StaffService:1244 | `VALIDATION_FAILED` | `Set.of("approve","reject").contains(request.decision())` | 审批结论必须为通过或驳回 |

文件版本头：AccessService 仍 `V0.13`，测试仍 `V0.20`，StaffService 仍 `V0.02`（拟修写的 V0.13→V0.14 / V0.20→V0.21 **未做**）。版本头不是用户可见文案，不是冻结判据，不构成本席 FAIL。

文案 vs PGM-001 / PGM-066 / GEN-015：

- 1694/247/1244/1061/1491：人读字符串无 `[A-Za-z]`。本席经真实 `FieldLabels.translate` + `GlobalExceptionHandler.handleApiException`：CJK 原样透传，handler 体与抛点一致，`latin=false`。
- 515：人读含拉丁字母 `U`。**不判冲突。** 类型 1：三份 HTML 自身把「U 盘密钥」当作中文业务用词——`platform-governance-map` 6 次、`business-system-map` 14 次、`business-enum-quick-reference` 2 次，合计 22 次「U 盘密钥」；同批 HTML 无「USB」。PGM-001 举例是 `active/draft`、`mm/dd/yyyy`、英文按钮，不是 HTML 已写死的「U 盘密钥」。把 USB 换成 HTML 原文，符合 GEN-015「与项目声明的语言约定一致」。拟修参考句「持授权密钥的最高权限人员」未写 U 盘，实际用了 HTML 更完整的词，仍只改显示、未发明新权限类别。

GEN-002：入口/错误路径仍构建可执行。本席实跑 after 上 `approve`/`reject`/`APPROVE` 仍返回内部值 `approve`/`reject`，非法 `maybe` 与空串仍抛 `VALIDATION_FAILED` + 中文。币种允许值仍仅 `CNY`（内部标识）。

GEN-003（`invalidDecisionMessageIsChinese`）：

- 执行真实 `AccessService.normalizeDecision`（`reviewGrant:723` 的直接被调用方），不是 tautology、不是断言常量等于自身。
- 独立判据：码=`VALIDATION_FAILED` 且 message 含「审批结论」且不含 `[A-Za-z]`。
- **能红：** after 测试 + before AccessService → `AssertionError`，实际 `"审批结论必须为通过（approve）或驳回（reject）"` 命中 `[A-Za-z]`；`RED_ONE_EXIT 1`。
- **能绿：** after 源 → `PASS invalidDecisionMessageIsChinese`；全类 **34 PASS / 0 FAIL**（每方法新实例；先前一次 31/3 是本席 runner 复用实例脏了 `users` 表，不是产品回归）。
- 接缝浅于 032（032 走 Controller+Handler）。本轮已用类型 4 证明 handler 对 CJK 原样透传，故 `getMessage()` 等于用户可见 `body.message()`。覆盖仅 1694 一处，未覆盖 515/1061/1491/StaffService；拟修「覆盖4处错误出口」未完全落地。其它 5 处由源码 diff + handler 探针补证，不把覆盖缺口写成测试无效。测试本身是可失败的真实行为断言。

未改 `FieldLabels` / 账号注册审批 / 支付 / 存储。

### 4.3 对抗式

| 攻击/反例 | 结果 |
|---|---|
| 非法 decision `maybe` / 空串 | after：中文「审批结论必须为通过或驳回」，码仍 `VALIDATION_FAILED`；before：夹 approve/reject，新测试红 |
| 合法 `approve`/`reject`/`APPROVE` | 仍返回内部 `approve`/`reject`，业务允许值未改成中文枚举 |
| 非 CNY 薪资 | 仍抛 `UNSUPPORTED_CURRENCY`；不再把 `CNY` 或输入币种写进 message（避免 USD/EUR 回显） |
| 非最高权限删模板 | 仍 `isMasterAuthority` 否决 + `MASTER_AUTHORITY_REQUIRED`；文案改为 HTML 用词「U 盘密钥」 |
| `FieldLabels.translate` 遇 CJK 不剥英文 | 已读 L528–529 并实跑：含 CJK 原样透传。故必须改抛点。本 diff 未改 FieldLabels，符合 034 边界 |
| `[A-Za-z]` 套到 515 | 会因「U」为真。该字母来自 HTML 词「U 盘密钥」，不是 USB 残留。测试未覆盖 515，因此不会假红；也不据此要求改 HTML（铁律 4） |
| StaffService:363 `（approved）或驳回（rejected）` | **本 diff 未改**。操作员②点名的是 247、1244 两处 approve/reject。363 是删除档案审批、内部值是 `approved`/`rejected`。属 diff 外同类残留，记给后续 B5，**不**把未授权第三处没改写成 B4 内容 FAIL |
| AccessService:567 注释仍写 USB | 注释，非用户可见；两树均有，不在本 diff hunk |
| 越权改权限/币种/错误码 | 四+二处 hunk 均为 ±1 字符串；无分支、无允许值、无校验改动 |

**内容结论：PASS**

无新 finding ID 进入本席 B4 票。363 残留分类：冲突候选、不在本轮修复队列、本席 B4 不扩 ID（B4 只审 diff；B5 再审整模块）。

## 5. 本席独立验证（类型 4）

隔离：`.scratch/m006-b4-r4-grok-verify` 与 `...-verify2`；`mvn -o dependency:build-classpath -DincludeScope=test` 只写 scratch；`javac -d` 指向 scratch；跑完 `rmtree`；`LEFTOVER []`。

| 对照 | 命令要点 | 可观察结果 |
|---|---|---|
| after 绿（034 单测） | after AccessService + after 测试，跑 `invalidDecisionMessageIsChinese` | `GREEN_JAVAC_EXIT 0`；`PASS invalidDecisionMessageIsChinese`；`RESULT 1 PASS / 0 FAIL` |
| after 绿（全类） | 每 @Test 新实例 | `ALL_EXIT 0`；`RESULT 34 PASS / 0 FAIL`，含 `invalidDecisionMessageIsChinese` 与 `invalidAccountTypeResponseIsChinese` |
| after 正路径 | `normalizeDecision("approve"/"reject"/"APPROVE")` | 返回内部 `approve`/`reject`，不抛 |
| after 错路径 | `maybe`、空串 | `VALIDATION_FAILED` + `审批结论必须为通过或驳回` |
| before 红 034 | before AccessService + after 测试 | `RED_ONE_EXIT 1`；实际 `"审批结论必须为通过（approve）或驳回（reject）"` 命中 `[A-Za-z]` |
| handler 透传 | 真实 `FieldLabels.translate` + `handleApiException` | 新中文三句 `latin=false`（515 因 HTML 词「U」为 true）；旧 USB/CNY/approve 句 `latin=true` |
| StaffService 编译 | after/before 源各 `javac --release 21` | 两次 exit 0 |
| 共享产物 | 两次核 `backend/target/classes` 与 `AccessService.class` mtime | 未变 |

未把 findings 自述 34 PASS / staff 13 源 javac 记为本席实跑。本席实跑范围即上表。未跑全仓 `mvn test`、支付厂商、浏览器/安卓、迁移。

## 6. 2.1 分类（本轮 diff，只记录不发问）

- 034 修复：冲突（用户可见报错夹英文枚举/缩写）。工程缺陷已对根因下药（改抛点，不改翻译器）。
- 操作员② 两处：同类冲突，点名扩展，非新业务发明。
- 验证测试：技术性（无业务发明），归因 034。
- 无添加项、无 HTML 修改申请、无锁外判据。
- StaffService:363 不在 diff，本步不发问、不新开 ID。

## 7. ponytail-review

Lean already. Ship.

授权范围内 6 处均为 ±1 字符串，测试 15 行最小断言。无可删抽象、无新依赖、无第二套翻译器。

`net: 0 lines possible.` 不挡 PASS。

（363 若将来修，同样只改人读一句，不在本轮砍。）

## 8. 结论

- **内容结论：PASS**
- **范围归因结论：PASS**
- **本席审计结论：PASS**（任一 FAIL 即本席 FAIL；两闸均 PASS）
- 新 finding：无（B4 票内）
- 034 根因（CJK 透传混合英文抛点）在授权 4+2 处已消除；内部码/允许值/权限/币种判断保留
- 不进入 B5（本席职责止于 B4 raw）；不改源码/HTML/锁/台账/findings

本席未把操作员或席甲自审数字记为本席实跑。本席实跑范围见 §5。
