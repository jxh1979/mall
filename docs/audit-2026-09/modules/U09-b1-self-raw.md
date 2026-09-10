CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U09 未发布材料第 9 段（主管审批联动）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-09T23:15:29Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `c857a9fbd41dbf4e8677c07c6785e8da8d243e7c`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 9 段，1 文件 890 行（governance/review/SupervisorReviewService.java，V0.07→V0.09），main 主归属 M040；适用判据＝M040 映射行并集 106 项。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。隔离编译在 `/tmp/u-overlay/backend`（基线 242f4872 `git archive` + 76 未发布文件，`mvn -o compile`）内进行，第 12 段 `SupervisorReviewServiceTests` 在同一 overlay 离线运行（/tmp/u12_tests.log），不触主仓、不写任何 complex_test* 库。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U09-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U08 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U09-00N，处置写给发布者），不做 B3/B4/B5。
- 只读依赖追流（不审）：副本 `StaffApprovalService.java`（第 5 段：evaluate 调用方与 @EventListener 消费方）、`StaffService.java`（第 6 段：排班提交/上限调整发起）、`V407`（第 10 段：hr_review 线种子与上限修订历史）、`SupervisorReviewServiceTests.java`（第 12 段）；main `ApprovalChainService.java`、`SupervisorReviewPolicyRecord.java`、`V391`。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java | 890 | 55916 | eab1f5721c9586a0c31861c57697c02e6d501c61080dd49e4f8d24a122d8b805 | 全读 |

## 2. 可执行证据原文（/tmp/u09_b1_run.log）

```
START=2026-09-09T20:49:32Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java | 890 | 55916 | eab1f5721c9586a0c31861c57697c02e6d501c61080dd49e4f8d24a122d8b805
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java 890 55916 eab1f5721c9586a0c31861c57697c02e6d501c61080dd49e4f8d24a122d8b805
=== baseline 242f4872 -> unpublished tree e28dad05 (full diff, this file) ===
diff --git a/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java b/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java
index fb7d2a42..5ed1090a 100644
--- a/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java
+++ b/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java
@@ -1,6 +1,6 @@
 package com.complex.platform.governance.review;
 
-// 文件版本：V0.07
+// 文件版本：V0.09
 
 import com.complex.platform.access.AccessActor;
 import com.complex.platform.access.AuthorizationService;
@@ -36,6 +36,8 @@ public class SupervisorReviewService {
     private final AuditEventService auditEventService;
     // M087：审批链引擎（组织层级逐级节点链）。纯领域对象，随服务构造，不单独注入。
     private final ApprovalChainService chainService;
+    @org.springframework.beans.factory.annotation.Autowired(required = false)
+    private org.springframework.context.ApplicationEventPublisher eventPublisher;
 
     // specific_role 策略的"指定角色"以审批人须经角色持有的权限码表达（双轨收口 §24.3）。
     // 默认沿用督导复核审批权限 governance.supervisor_review.approve（须来自角色，非临时授权），
@@ -47,6 +49,10 @@ public class SupervisorReviewService {
     static final String TAX_RULE_ADJUST_ACTION = "finance.tax_rule.adjust";
 
     private final List<SupervisorReviewPolicyRecord> policies = List.of(
+            new SupervisorReviewPolicyRecord("staff_shift_publish", "staff.shift.publish", 0, 1, true, List.of("reason"), "active", "one_level_up"),
+            new SupervisorReviewPolicyRecord("staff_penalty_cap_adjust", "staff.payroll.penalty_cap.adjust", 0, 3, true,
+                    List.of("reason", "before_after"), "active", "top_authority", null,
+                    List.of("hr_review", "finance_review", "legal_review")),
             new SupervisorReviewPolicyRecord("manual_release_l2", "manual_release", 0, 2, true, List.of("operator_note", "evidence_attachment"), "active", "specific_role", DEFAULT_SPECIFIC_ROLE_PERMISSION),
             new SupervisorReviewPolicyRecord("bill_waiver_l3", "bill_waiver", 1, 3, true, List.of("reason", "evidence_attachment"), "active", "specific_role", DEFAULT_SPECIFIC_ROLE_PERMISSION),
             new SupervisorReviewPolicyRecord("refund_l2", "refund", 1, 2, true, List.of("reason"), "active", "specific_role", DEFAULT_SPECIFIC_ROLE_PERMISSION),
@@ -205,6 +211,12 @@ public class SupervisorReviewService {
                                 "组织层级未维护，审批链未生成；请平台管理员维护发起人的直接上级后重新生成审批链");
                     }
                     ApprovalChainNode node = resolveDecidableNode(pending, nodes, reviewerId, reviewerLevel);
+                    if ("staff.payroll.penalty_cap.adjust".equals(pending.actionType())
+                            && ApprovalChainNode.KIND_MASTER.equals(node.approverKind())
+                            && nodes.stream().anyMatch(n -> !ApprovalChainNode.MAIN_LINE.equals(n.lineCode())
+                                    && !ApprovalChainNode.APPROVED.equals(n.status()))) {
+                        throw ApiException.conflict("DEPARTMENT_REVIEWS_PENDING", "须先完成人力、财务、法务三审，再由顶层平台最终确认");
+                    }
                     requireDecisionEvidence(findPolicyByCode(pending.policyCode()), request);
                     Instant now = Instant.now();
                     // M087-I2-02（PGM-1.7.1「任一人审批即完成该节点」）：节点裁决为 status='pending' 的比较并交换，先到先得；后到者被拒。
@@ -277,6 +289,7 @@ public class SupervisorReviewService {
                             request.idempotencyKey(),
                             Map.of("decision", outcome, "action_type", pending.actionType())
                     );
+                    if (eventPublisher != null) eventPublisher.publishEvent(result);
                     return result;
                 },
                 SupervisorReviewDecisionRecord.class
=== main vs baseline vs unpublished (blob ids) ===
baseline=fb7d2a42306a488d1e82706e39fee90f3c43e361 head=fb7d2a42306a488d1e82706e39fee90f3c43e361 unpublished=5ed1090a3c648140bcce2a2b225fb9292ce56943 main_unchanged_since_baseline=yes numstat_baseline_to_head=
main_version_line=// 文件版本：V0.07 unpublished_version_line=// 文件版本：V0.09
=== probes: new policies, guard, event publisher (unpublished) ===
40:    private org.springframework.context.ApplicationEventPublisher eventPublisher;
52:            new SupervisorReviewPolicyRecord("staff_shift_publish", "staff.shift.publish", 0, 1, true, List.of("reason"), "active", "one_level_up"),
53:            new SupervisorReviewPolicyRecord("staff_penalty_cap_adjust", "staff.payroll.penalty_cap.adjust", 0, 3, true,
106:                    requireDefaultLines(policy, lines);
218:                        throw ApiException.conflict("DEPARTMENT_REVIEWS_PENDING", "须先完成人力、财务、法务三审，再由顶层平台最终确认");
292:                    if (eventPublisher != null) eventPublisher.publishEvent(result);
452:            requireDefaultLines(blockPolicy, List.of()); // M007-R6-01：策略固定要求平行线（税费三审）的事项不能经无平行线的准入阻断路
=== probes: SupervisorReviewPolicyRecord constructors (8/10 args) exist in copy/main ===
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewPolicyRec
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewPolicyRec
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewPolicyRec
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewPolicyRecord.java:15:public record SupervisorReviewPolicyRecord
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewPolicyRecord.java:32:    public SupervisorReviewPolicyRecord(
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewPolicyRecord.java:47:    public SupervisorReviewPolicyRecord(
=== probes: callers in unpublished staff services (evaluate with parallel lines / event listener filter) ===
45:                List.of(), "staff.shift.publish", "staff_shift_batch", "batch_" + UUID.randomUUID(),
62:                List.of(), "staff.payroll.penalty_cap.adjust", "staff_penalty_cap", "cap_"+UUID.randomUUID(),
64:                null, request.parallelLines()));
69:    @EventListener
71:    public void afterDecision(SupervisorReviewDecisionRecord decision) {
73:        if ("staff.payroll.penalty_cap.adjust".equals(decision.actionType())) {
77:        if (!"staff.shift.publish".equals(decision.actionType())) return;
=== probes: approval_lines seeds (hr_review) ===
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/resources/db/migration/V407__staff_penalty_cap_approval_history.sql:1
backend/src/main/resources/db/migration/V391__approval_lines_seed.sql:8:    ('finance_review', '财务审核线', 1, NULL, 'active'),
backend/src/main/resources/db/migration/V391__approval_lines_seed.sql:10:    ('legal_review',   '法务审核线', 1, NULL, 'active')
=== probes: chain engine — parallel lines vs master main line ordering (ApprovalChainService.currentNodes) ===
82:                nodes.add(node(reviewId, ApprovalChainNode.MAIN_LINE, seq++, ApprovalChainNode.KIND_USER, approver));
85:                nodes.add(node(reviewId, ApprovalChainNode.MAIN_LINE, seq, ApprovalChainNode.KIND_ROLE, null));
105:                nodes.add(node(reviewId, ApprovalChainNode.MAIN_LINE, seq++, ApprovalChainNode.KIND_USER, approver));
108:                nodes.add(node(reviewId, ApprovalChainNode.MAIN_LINE, seq, ApprovalChainNode.KIND_MASTER, null));
174:    public static List<ApprovalChainNode> currentNodes(List<ApprovalChainNode> nodes) {
185:    public static boolean allApproved(List<ApprovalChainNode> nodes) {
=== HTML greps (main 正本) ===
docs/business-system-map-for-operations-v0.1.html:1443:                <li>主管排班：排到几点几分，可指定
docs/business-system-map-for-operations-v0.1.html:1458:                <li>每位员工都有一个结构化的薪资配
docs/business-system-map-for-operations-v0.1.html:1684:                  <tr><th scope="row">顶层平台</th><td>只制
docs/business-system-map-for-operations-v0.1.html:1729:                  <tr><th scope="row">税务处理</th><td>涉税
docs/business-system-map-for-operations-v0.1.html:1745:                <li>分红税务按发放时有效的中国税法
docs/platform-governance-map-for-operations-v0.1.html:302:          <p class="lead" style="margin-top:1.2rem"><strong>�
docs/platform-governance-map-for-operations-v0.1.html:417:              <li><strong>顶层平台</strong>：制定跨项
docs/platform-governance-map-for-operations-v0.1.html:436:            <p class="lead" style="margin-top:1rem"><strong>�
docs/platform-governance-map-for-operations-v0.1.html:498:                <li>合作模式分六类：自营、租赁、
docs/platform-governance-map-for-operations-v0.1.html:660:                <li>高危授权审批（已确认）：以下
docs/platform-governance-map-for-operations-v0.1.html:680:                <li>导出文件是否脱敏，不由导出人
=== isolated compile + U12 SupervisorReviewServiceTests in /tmp/u-overlay (logs) ===
START=2026-09-09T18:37:14Z
MVN_EXIT=0
END=2026-09-09T18:37:19Z
START=2026-09-09T18:56:58Z
MVN_EXIT=0
END=2026-09-09T18:57:06Z
/tmp/u-overlay/backend/target/surefire-reports/com.complex.platform.governance.review.SupervisorReviewServiceTests.txt:Tests run: 41, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.570 s -- in com.complex.platform.governance.review.SupervisorReviewServiceTests
/tmp/u-overlay/backend/target/surefire-reports/com.complex.platform.gate.DeviceCredentialVerifierTests.txt:Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.295 s -- in com.complex.platform.gate.DeviceCredentialVerifierTests
END=2026-09-09T20:49:32Z
```

## 3. 三线审计方法与覆盖

- 对象口径：未发布提交 03acdb98 的只读输入副本第 9 段（1 文件 `governance/review/SupervisorReviewService.java` 890 行，文件版本 V0.07→V0.09），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段文件的真实改动 = `git diff 242f4872 e28dad05 -- <路径>`（§2 全文 diff：+15/−1——两条新策略 `staff_shift_publish`（one_level_up，证据 reason）与 `staff_penalty_cap_adjust`（top_authority，证据 reason+before_after，平行线 hr_review/finance_review/legal_review）；`approve` 内 penalty cap 主线顶层节点须待全部平行线通过的 `DEPARTMENT_REVIEWS_PENDING` 闸；`@Autowired(required=false) ApplicationEventPublisher` 与整单终态后 `publishEvent(result)`），main 当前版本仅作分叉对照。
- 第一性原理线：HTML BSM L1443「排班表最终发布须经该主管的上一级审批」→ 一条 one_level_up 策略；BSM L1458「各类扣除合计不得超过该员工当月总薪资的 20%（安全阈值，全平台统一可配，调整须经人力、财务、法务三审并由顶层平台公司最终确认，出厂默认 20%，须同时配置上下限）（总薪资指基础工资、各项补贴、绩效工资与奖金之和）」→ 三条平行线 + 顶层最终确认，且顶层确认须在三审之后；裁决结果要回到员工域落地（排班发布、上限生效）→ 事件发布给第 5 段 `StaffApprovalService.afterDecision`。逐一对照策略表、闸与事件。
- 顺从式线：106 项判据逐项（§5）。
- 对抗式线：新策略的证据/审批人档位是否与 HTML 一致；顶层节点闸是否只对 penalty cap（而 main 既有税费三审无同闸）；事件发布是否在幂等执行内（重放不重发）、监听方是否按 actionType 过滤（第 5 段 `afterDecision:71-79`）；`required=false` 字段注入在无发布器上下文（第 12 段单元测试）静默不落地；发起方是否给三条线指定最终审批人（第 3 段页面 `StaffPayrollLeavePage.tsx:212` 与第 6 段 `StaffPenaltyCapRequest.parallelLines`）；hr_review 线种子（第 10 段 V407）；main 分叉。
- 只读依赖追流（不审、不计入本段结论）：副本 `StaffApprovalService.java:40-79`、`StaffService.java`、`StaffPayrollLeavePage.tsx:196-212`、`V407`、`SupervisorReviewServiceTests.java`；main `chain/ApprovalChainService.java`（主线/平行线生成与 `currentNodes`/`allApproved`）、`SupervisorReviewPolicyRecord.java`（8/10 参构造 main 既有）、`V391`。
- 1 文件全文逐行读完（§1）；diff 逐块读完（§2）。可执行验证：`/tmp/u-overlay/backend` 离线 `mvn -o compile` 退出 0；第 12 段 `SupervisorReviewServiceTests` 在 overlay 离线运行通过（§2 引用 /tmp/u12_tests.log）。本材料未发布，不做 B3；处置写给发布者。

## 4. 与发布基线 / main 的分叉对照

- main HEAD `SupervisorReviewService.java` blob ＝ 基线 blob（§2：M009–M012 未触及该文件，M040 待审）→ 未发布版＝基线 + 15/−1，快进合并、无冲突；策略记录 8/10 参构造为 main 既有（M087 税费三审已用 10 参）。

## 5. 106 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | 两条策略、顶层闸、事件发布均有 HTML 上位（L1443/L1458） |
| GEN-002 可执行正确性 | 通过（登记） | 三审后顶层确认由 `:214-219` 闸保证；`requireDefaultLines` fail-closed 要求发起时逐线指定最终审批人，第 3/6 段已提供；一次 approve 裁决一个节点、整单终态由节点表归约（main 既有 M087）；监听方抛错（SHIFT_STARTED 等）使整单裁决回滚，审批人可在修正后重试（OBS-3） |
| GEN-003 测试有效性 | 通过（登记） | 第 12 段单元测试覆盖策略/闸；`eventPublisher` 在单元测试上下文为 null 时事件不发（OBS-1），落地路径只能由第 11 段 DB 测试覆盖 |
| GEN-004 信任边界 | 通过 | 审批人级别取自会话；动作类型小写规范化 |
| GEN-005 授权 | 通过 | 顶层节点须 USB 主会话；发起人/受益人不得自审（main 既有） |
| GEN-006 数据完整性 | 通过 | 复核单行锁 + 节点 CAS（main 既有）；事件在同一事务内发布 |
| GEN-007 并发幂等 | 通过 | `publishEvent` 在 `idempotencyService.execute` lambda 内，重放不重发 |
| GEN-008 接口一致性 | 通过（登记） | 顶层闸以动作字面量 `staff.payroll.penalty_cap.adjust` 判定，与策略表 `parallelLineCodes` 重复声明（OBS-2）；main 既有税费三审 `tax_rule_adjustment_l3` 无同类闸（OBS-2，归 M040/M087） |
| GEN-009 错误处理 | 通过 | 中文 ApiException（DEPARTMENT_REVIEWS_PENDING 等） |
| GEN-010 安全 | 通过 | 无新 PII |
| GEN-011 审计证据 | 通过 | 节点裁决/整单裁决/链生成均留痕（main 既有） |
| GEN-012 资源 | 通过 | 无新增循环/查询 |
| GEN-013 构建 | 通过 | 隔离 `mvn -o compile` 退出 0 |
| GEN-014 迁移 | 不适用（登记） | hr_review 线种子在第 10 段 V407（main V391 仅 finance/tax/legal） |
| GEN-015 界面 | 不适用 | 无界面 |
| GEN-016 文档 | 通过 | 版本行 V0.07→V0.09 与改动一致 |
| GEN-017 分类 | 通过 | 无添加：三审+顶层确认、上一级审批均为 HTML L1458/L1443 原句 |
| GEN-018 证据 | 通过 | 结论落到文件:行、HTML 行、§2 命令 |
| 其余 M040 映射行（BSM/PGM/BEQR 88 项） | 通过／不适用 | 本段仅新增两条员工域策略与顶层闸/事件，审批链、转发、超时升级、待办可见范围等 M040 既有条款无实现面改动；M040 待审项不在本段重复登记 |

## 6. 本次 Findings（临时 ID CX-U09-00N；正式 ID 由 B2 分配）

无新的工程缺陷。两条策略与顶层闸逐字对应 HTML L1443/L1458；事件发布在幂等事务内、监听方按 actionType 过滤；发起方（第 3/6 段）与线种子（第 10 段）齐备；main 同文件未改、快进合并。

## 7. 观察（登记、不入队）

- OBS-1：`eventPublisher` 以字段 `@Autowired(required = false)` 注入（其余依赖构造注入，`:39-40`），`:292` 为 null 时静默不发——Spring 上下文中恒非 null；单元测试上下文中审批通过不触发排班发布/上限生效，只能靠第 11 段 DB 测试证明落地（GEN-003）。ponytail：改为构造参数即无 null 分支；不落地。
- OBS-2：顶层闸 `:214-219` 以动作字面量判定且仅对 penalty cap 生效；策略表已声明 `parallelLineCodes`，可改为「凡有平行线的策略主线 master 节点须待平行线全通过」的通用规则，同时覆盖 main 既有 `tax_rule_adjustment_l3`（HTML L1745 同样「再由老板最终确认」）——main 既有，归 M040/M087。
- OBS-3：监听方 `afterDecision` 同步同事务，抛 `SHIFT_STARTED/SHIFT_CHANGED/POLICY_CHANGED` 时整个 approve 回滚（节点裁决与审计一并回滚），审批人须待起草人重提；HTML 对此沉默，属 fail-closed。
- OBS-4：`staff.payroll.penalty_cap.adjust` 不在 `isMoneyAction`（`:626-631`）→ 默认 3 个工作日时限而非涉钱 1 日；HTML L1458 对时限沉默。
- OBS-5：`requireDefaultLines` 只校验最终审批人非空，不校验其是否持有对应线（hr/finance/legal）资格；main 既有范式（税费三审同），归 M040。
- 技能：`using-superpowers`、`verification-before-completion`（编译/单测实跑附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**PASS**。1 文件全文三线审计、106 项判据逐项核对；无新增工程缺陷、无添加项、无 HTML 修改申请；5 项观察（OBS-2/5 为 main 既有归 M040）；隔离编译与第 12 段单测绿；未修源、未提交/推送。本报告落盘前未读取 `U09-b1-grok-raw.md` 任何正文。
---

# U09 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.60「当前 U09 B2-r1 整批候选」U09-001…006；modules/U09-b1-grok-raw.md V0.01（38123bytes SHA72d025094461cd81ee81db640c04ec3dd689cd0b8de6120c3b089be4c9688f9a）；本文件 B1 段（22073bytes SHA9f9816472ce0e2c3fe353886d6e951b3baab8bc576b9dde2caa4f228db9022fe）。2026-09-09T23:30:15Z
- 席甲 B2 核实（只读，见 ledger V2.68 U09 节）：`isMoneyAction` `:626-631` 无 `staff.payroll.*`、`scheduleDeadline` `:616-618` 回落分档；`hr_review` 只在 U10 V407 种子、main V391 无；U12 测试 diff 只改目录断言；Grok 保留 main 测试 41 run/1 fail 与席甲 §4 blob＝基线一致；`eventPublisher` `required=false` `:39-40/:292`。

## 勘误（席甲 B1 段）

- 席甲 B1 票 PASS 有误：OBS-1（可选事件发布）、OBS-4（时限分档）与 U12 体 OBS-1（单测不经过新闸）在只合本文件/只合部分段的首轮发布即导致「批准不生效」「处罚上限走 3 日」「回归不覆盖新分支」，属发布门缺陷而非观察；B2 接受 Grok GR-001…005 为 U09-001…005。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | main blob＝基线、只合本文件打红 main M042 单测（Grok 实跑）；叠法处置同 U05-008 |
| 002 | PASS | `isMoneyAction` 未纳入处罚上限；BSM L1458「安全阈值」+ BSM-001 涉钱参数足以支撑；处置只把新动作纳入既有分档 |
| 003 | PASS | V391/V407 事实经席甲 U05 预注核实；同批不发明第四条线 |
| 004 | PASS | U12 diff 只改目录；被测面在本段、用例归 U12/U11 同 PR 合规 |
| 005 | PASS | `required=false` + null 静默；构造注入或显式落地最小 |
| 006 | PASS | 观察归并未改写双方原意 |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 5 条，定稿以此为准）

1. 002 支撑链：BSM-087 定身份（处罚上限属薪资安全阈值）、BSM-001 定「按涉及钱处理」；「1 个工作日」来自本文件既有 `isMoneyAction` 分档，不是 BSM-087 原文数字，也不引用 PGM-029。
2. 004 来源读作「席甲 B2 核实 U12 `:55-63`」；席甲 U09 B1 GEN-003 行「覆盖策略/闸」为低估，不是既有观察。
3. 006 抽样未列 Grok §8 第 1/6/7/8/11/12/14 条；其中 1/6/11/12 已并入 001/004/005；定稿不逐条抄全，不得写成「Grok 无这些条」。
4. ledger U09 开工节所写「+15/−1」以 git 实算 +14/−1 为准（定稿节勘误，不改开工节原字）。
5. BSM-001 锁出处行为 `:505`（h2），涉钱句在现行正本 L521。

## 结论

席甲 B2-r1 票：**PASS**（候选 U09-001…006 成立；发布前须修「是」5 / 「否」1；添加 0；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 61720bytes SHAc07d6b31474f6f0730d973c20a28e7fa9044835d321b5e6fa7f806332f55d07c，前 38123bytes 原字节不变）。
