CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M043 B4-r1 Grok 完整报告 V0.01

- 席位：Grok（乙，固定 grok-4.6）。Grok FAIL 具否决权。
- 步骤：B4-r1 修复 diff 只读审计。不是 B5，不是模块 PASS，不是授权下一轮 B3。
- 模块：M043 `高危授权审批、临时授权、角色装配重算与主管复核链／网页与验证／第1段`
- 生命周期（只读 ledger）：IN_PROGRESS；B3 已执行次数=1
- 正本：`/home/james/complex-management` 分支 `main`；本席 `git rev-parse HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`
- 提示词：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- runbook：`docs/audit-2026-09/plan/runbook.md` V0.02
- 计划三文件（`plan/modules.md` FILES）：`SupervisorReviewPage.tsx` / `SupervisorReviewPage.test.tsx` / `SupervisorReviewPage.chain.test.tsx`
- 本席未读本次 B4 self raw、未接触适配器缓冲、未改代码/测试/HTML/锁/ledger/findings、无子代理/MCP/第三席
- 写入：仅本指定路径（文件原先不存在，本次创建）
- 结论：**PASS**

## 0. 技能自检（prompt §6 收窄）

已读并按收窄口径使用：

| 技能 | 用法 |
|---|---|
| using-superpowers | 开工技能门 |
| grilling | 只借追问结构自问自答，事实自读，不派子代理、不等操作员 |
| requesting-code-review | B4=本席外审；本体派子代理 / HEAD~1 基线 **未用** |
| receiving-code-review | 先核源/命令再接受或反驳修复是否够；禁止表演性同意派发措辞 |
| verification-before-completion | PASS 前实跑测试、typecheck、独算哈希、独立 `git diff --binary` |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不打点、不改码 |
| ponytail-review | 只评 diff 有无越界可删项，不落地 |
| writing-for-agents | 本 raw 结构 |
| git-guardrails | 本席只读，未 commit/push/merge |

缺口（不冒充已调用）：本环境未找到独立 `batch-grill-me`、`ubiquitous-language`。`tdd` / `ponytail` 落地属修复席，B4 不用。Arc audit 子代理与双席冲突，未用。

## 1. 锁哈希（本席独立实算）

命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`

| 源 | 值 |
|---|---|
| ① 本席实算 | `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458` |
| ② sidecar `plan/criteria-lock.sha256` | 同 |
| ③ ledger 顶部 `CRITERIA-LOCK-SHA256:` | 同 |

本报告第一行=①。本席不写 ledger。`LOCK-AMEND-COUNT: 1` 只读。

## 2. 本轮 tree 与独立 diff 命令

派发给定：

- before=`f26b340bfa35c31ecdc0329256e8ce1d40620c0f`
- after=`47af849df0eb7c41c6a7b962723786eca1baecb9`

本席：`git cat-file -t` 两者均为 `tree`。

```
git diff --binary f26b340bfa35c31ecdc0329256e8ce1d40620c0f 47af849df0eb7c41c6a7b962723786eca1baecb9
```

不按路径过滤。实跑：

| 项 | 事实 |
|---|---|
| `--name-status` | 仅 `M` 两文件：`SupervisorReviewPage.test.tsx`、`SupervisorReviewPage.tsx` |
| `--numstat` | test 148+/13-；page 55+/37- |
| `--stat` | 2 files changed, 203 insertions(+), 50 deletions(-) |
| 两树文件数 | 各 3970；`comm` 仅在 after / 仅在 before 均为空 |
| chain.test blob | before=after=`7d30eb3297999145cb5f01a2020d0cc82c7e8a84`（未变） |
| page blob | before `cedd05d222c258fca12274f3851adcc64a2f7191` → after `96fd5009394b848fde95d3acd73df73749746f70` |
| test blob | before `1bd056113cf5a61a5f65cd433163619933db821f` → after `dafcd37f6c88d7e163dd482e44be97fa468be4d2` |
| 工作区两文件 `git hash-object` | 与 after blob 全等 |
| 真实主 index（`git ls-files -s`） | 仍为 before blob（B3 临时索引，未改主 index） |
| 三份业务 HTML | before tree 与 after tree blob 全等；本 diff 未改 HTML |
| `package.json` / lock | 本 diff 空 |
| 全局 `statusLabels.ts` | 本 diff 空 |
| diff 全文 SHA256 | `e7fe9dc86cc5aeef33b30ddd84849bcc59f6a26ff58b4bb36ad655aa32f780bc`（本席对本次 `git diff --binary` 落盘计算） |

当前 HEAD 的 `platform-governance-map-for-operations-v0.1.html` blob（`8eca4668…`）与两棵 B3 快照（`6e9d813c…`）不同：`git add -A && write-tree` 吃的是当时工作区，属快照脏树，**不是本轮 name-status 中的改动**。本 diff 未动三 HTML。

## 3. 输入（B2 定稿 + 两份 B1/B2 raw）

findings.md 顶部「M043 本次旧PR遗漏 B2整批定稿」V3.60：M043-001…008 分类/工程缺陷/队列均为定稿值，八项队列「是」。输入 raw：

- `M043-b1-grok-raw.md` V0.01 B1 FAIL + V0.02 B2 PASS
- `M043-b1-self-raw.md` V0.01 B1 FAIL + V0.02 B2 PASS

映射：GR-01→001，GR-02+CX-03/04→002，GR-03+CX-02→003，GR-04+CX-01→004，GR-05→005，GR-06→006，GR-07→007，GR-08→008。无添加项。本席不沿用派发对修法的概括，按下节源码与测试自判。

## 4. 完整 binary diff 原文

命令见 §2。全文如下（476 行）：

```diff
diff --git a/apps/web/src/admin/pages/SupervisorReviewPage.test.tsx b/apps/web/src/admin/pages/SupervisorReviewPage.test.tsx
index 1bd05611..dafcd37f 100644
--- a/apps/web/src/admin/pages/SupervisorReviewPage.test.tsx
+++ b/apps/web/src/admin/pages/SupervisorReviewPage.test.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.03
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -10,6 +11,21 @@ const POLICIES = [
   { id: "sp-2", policy_code: "POL-2", action_type: "billing_discount", required_level: 2, blocks_by_default: false, required_evidence_types: [], status: "disabled" },
 ];
 
+const REAL_POLICIES = [
+  {"policy_code": "staff_shift_publish", "action_type": "staff.shift.publish", "required_evidence_types": ["reason"], "status": "active", "blocks_by_default": true},
+  {"policy_code": "staff_penalty_cap_adjust", "action_type": "staff.payroll.penalty_cap.adjust", "required_evidence_types": ["reason", "before_after"], "status": "active", "blocks_by_default": true},
+  {"policy_code": "manual_release_l2", "action_type": "manual_release", "required_evidence_types": ["operator_note", "evidence_attachment"], "status": "active", "blocks_by_default": true},
+  {"policy_code": "bill_waiver_l3", "action_type": "bill_waiver", "required_evidence_types": ["reason", "evidence_attachment"], "status": "active", "blocks_by_default": true},
+  {"policy_code": "refund_l2", "action_type": "refund", "required_evidence_types": ["reason"], "status": "active", "blocks_by_default": true},
+  {"policy_code": "ar_ap_adjustment_l3", "action_type": "ar_ap_adjustment", "required_evidence_types": ["reason", "before_after"], "status": "active", "blocks_by_default": true},
+  {"policy_code": "contract_violation_l3", "action_type": "contract_violation", "required_evidence_types": ["violation_summary", "contract_snapshot"], "status": "active", "blocks_by_default": true},
+  {"policy_code": "base_definition_violation_l3", "action_type": "base_definition_violation", "required_evidence_types": ["violation_summary", "definition_snapshot"], "status": "active", "blocks_by_default": true},
+  {"policy_code": "permission_grant_l1", "action_type": "permission_grant_l1", "required_evidence_types": ["reason"], "status": "active", "blocks_by_default": true},
+  {"policy_code": "permission_grant_l2", "action_type": "permission_grant_l2", "required_evidence_types": ["reason"], "status": "active", "blocks_by_default": true},
+  {"policy_code": "permission_grant_l3", "action_type": "permission_grant_l3", "required_evidence_types": ["reason"], "status": "active", "blocks_by_default": true},
+  {"policy_code": "tax_rule_adjustment_l3", "action_type": "finance.tax_rule.adjust", "required_evidence_types": ["reason", "before_after"], "status": "active", "blocks_by_default": true},
+];
+
 function envelope(items: unknown[]): Response {
   return new Response(
     JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
@@ -37,12 +53,14 @@ function raw(data: unknown): Response {
   );
 }
 
-function makeApi(): AdminApi {
+function makeApi(options: { policies?: unknown[]; pending?: unknown[]; reviewStatus?: string; rejectAction?: string } = {}): AdminApi {
   vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
     const url = String(input);
-    if (url.includes("supervisor-review-policies")) return Promise.resolve(envelope(POLICIES));
+    if (url.includes("supervisor-review-policies")) return Promise.resolve(envelope(options.policies ?? POLICIES));
     if (url.includes("supervisor-review/evaluate")) return Promise.resolve(raw({ review_id: "review_1", blocked: true, review_status: "pending_review", required_level: 3 }));
-    if (url.includes("supervisor-review/pending")) return Promise.resolve(envelope(PENDING));
+    if (url.includes("supervisor-review/pending")) return Promise.resolve(envelope(options.pending ?? PENDING));
+    if (options.rejectAction && url.endsWith(`/${options.rejectAction}`)) return Promise.resolve(new Response(JSON.stringify({ success: false, code: "BENEFICIARY_REQUIRED", message: "直接受益人未登记" }), { status: 400, headers: { "content-type": "application/json" } }));
+    if (url.includes("/approve")) return Promise.resolve(raw({ review_id: "review_p1", review_status: options.reviewStatus ?? "approved" }));
     if (url.includes("/forward")) return Promise.resolve(raw({ review_id: "review_p1", forwarded_to: "usr_x", due_at: "2026-09-09T02:00:00Z", escalation_count: 0 }));
     return Promise.resolve(envelope([]));
   }));
@@ -52,9 +70,9 @@ function makeApi(): AdminApi {
 const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
 const MANAGE_ONLY_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: ["governance.supervisor_review.view", "governance.supervisor_review.manage"], denies: [], wildcard: false } };
 
-function renderPage(effectivePermissions: EffectivePermissionsState = ADMIN_PERMS) {
+function renderPage(effectivePermissions: EffectivePermissionsState = ADMIN_PERMS, options: Parameters<typeof makeApi>[0] = {}) {
   const props: AdminPageProps = {
-    api: makeApi(),
+    api: makeApi(options),
     user: { id: "u1", username: "admin", accountType: "platform_admin" },
     setActionResult: vi.fn(),
     setGlobalError: vi.fn(),
@@ -138,15 +156,13 @@ describe("SupervisorReviewPage 结果提示全中文 + 审批权限点", () => {
     expect(screen.getByTestId("supervisor-review-pending-review_p2").textContent ?? "").toContain("-");
   });
 
-  it("合同违规待审行没有直接受益人 → 转交被拦且不发请求", async () => {
+  it("合同违规待审行没有直接受益人 → 仍可提交合法转交", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-select-review_p2")).toBeTruthy());
     fireEvent.click(screen.getByTestId("supervisor-review-pending-select-review_p2"));
     fireEvent.change(screen.getByTestId("supervisor-review-forward-target"), { target: { value: "usr_x" } });
     fireEvent.click(screen.getByTestId("supervisor-review-forward"));
-    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("直接受益人未登记，不能转交");
-    const fetchMock = vi.mocked(globalThis.fetch);
-    expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/forward"))).toBe(false);
+    await waitFor(() => expect(vi.mocked(fetch).mock.calls.some(([input]) => String(input).includes("/review_p2/forward"))).toBe(true));
   });
 
   it("退款待审行没有申请人 → 转交被拦且不发请求", async () => {
@@ -239,16 +255,14 @@ describe("SupervisorReviewPage 结果提示全中文 + 审批权限点", () => {
     expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/approve"))).toBe(false);
   });
 
-  it("待审动作对不上策略 → 不能放行", async () => {
+  it("待审动作对不上目录 → 提交服务端校验", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-select-review_p1")).toBeTruthy());
     fireEvent.click(screen.getByTestId("supervisor-review-pending-select-review_p1"));
     fireEvent.change(screen.getByTestId("supervisor-review-approval-comment"), { target: { value: "同意放行" } });
     vi.spyOn(window, "confirm").mockReturnValue(true);
     fireEvent.click(screen.getByTestId("supervisor-review-approve"));
-    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("找不到对应复核策略，不能放行");
-    const fetchMock = vi.mocked(globalThis.fetch);
-    expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/approve"))).toBe(false);
+    await waitFor(() => expect(vi.mocked(fetch).mock.calls.some(([input]) => String(input).includes("/review_p1/approve"))).toBe(true));
   });
 
   it("动作类型选项与服务端策略一致：无「账单折扣」，有免单 / 退款 / 调账", async () => {
@@ -321,3 +335,124 @@ describe("SupervisorReviewPage 行明细抽屉", () => {
     await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
   });
 });
+
+describe("M043 真实审批信封与既有规则", () => {
+  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); window.history.replaceState({}, "", "/admin/governance/supervisor-review"); });
+
+  async function select(id = "review_p1") {
+    await waitFor(() => expect(screen.getByTestId(`supervisor-review-pending-select-${id}`)).toBeTruthy());
+    fireEvent.click(screen.getByTestId(`supervisor-review-pending-select-${id}`));
+    fireEvent.change(screen.getByTestId("supervisor-review-approval-comment"), { target: { value: "已核对凭据" } });
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+  }
+
+  it.each([
+    ["pending_review", "当前节点已通过，等待后续节点审批"],
+    ["approved", "全部节点已通过，单据生效"],
+  ])("批准返回 %s 如实显示节点状态", async (status, message) => {
+    const props = renderPage(ADMIN_PERMS, { reviewStatus: status });
+    await select();
+    fireEvent.click(screen.getByTestId("supervisor-review-approve"));
+    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringContaining(message)));
+    const call = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes("/review_p1/approve"));
+    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({ decision: "approved", evidence_attachment_ids: [], comment: "已核对凭据" });
+    expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining("当前节点"));
+  });
+
+  it.each(["approve", "reject"])("%s 按 policyCode 而非动作名要求免单附件", async (button) => {
+    const pending = [{ ...PENDING[0], decision: { ...PENDING[0]!.decision, policy_code: "bill_waiver_l3" } }];
+    const props = renderPage(ADMIN_PERMS, { policies: REAL_POLICIES, pending });
+    await select();
+    fireEvent.click(screen.getByTestId(`supervisor-review-${button}`));
+    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("请填写证据附件编号");
+    expect(window.confirm).not.toHaveBeenCalled();
+    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).includes("/approve"))).toBe(false);
+    fireEvent.change(screen.getByTestId("supervisor-review-evidence-attachment"), { target: { value: " att-1 " } });
+    fireEvent.click(screen.getByTestId(`supervisor-review-${button}`));
+    await waitFor(() => expect(props.setActionResult).toHaveBeenCalled());
+    const call = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes("/approve"));
+    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({ evidence_attachment_ids: ["att-1"], decision: button === "approve" ? "approved" : "rejected" });
+  });
+
+  it.each(["contract_violation_l3", "refund_l2"])("%s 非附件证据不强迫填附件", async (policyCode) => {
+    const pending = [{ ...PENDING[0], decision: { ...PENDING[0]!.decision, policy_code: policyCode, action_type: policyCode === "refund_l2" ? "refund" : "contract_violation" } }];
+    const props = renderPage(ADMIN_PERMS, { policies: REAL_POLICIES, pending });
+    await select();
+    fireEvent.click(screen.getByTestId("supervisor-review-approve"));
+    await waitFor(() => expect(props.setActionResult).toHaveBeenCalled());
+    const call = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes("/approve"));
+    expect(JSON.parse(String(call?.[1]?.body)).evidence_attachment_ids).toEqual([]);
+  });
+
+  it.each(["approve", "forward"])("%s 如实显示服务端拒绝，不报告成功", async (action) => {
+    const pending = [{ ...PENDING[0], schedule: { ...PENDING[0]!.schedule, beneficiary_id: null } }];
+    const props = renderPage(ADMIN_PERMS, { pending, rejectAction: action });
+    await select();
+    fireEvent.change(screen.getByTestId("supervisor-review-forward-target"), { target: { value: "usr_x" } });
+    fireEvent.click(screen.getByTestId(`supervisor-review-${action}`));
+    await waitFor(() => expect(screen.getByTestId("supervisor-review-error").textContent).toContain("直接受益人未登记"));
+    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).endsWith(`/${action}`))).toBe(true);
+    expect(props.setActionResult).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ status: "failed", message: "直接受益人未登记" }));
+  });
+
+  it.each([
+    ["review_p4", "不能复核自己发起的单"],
+    ["review_p5", "直接受益人不能复核"],
+  ])("%s 放行拒绝利益冲突", async (id, message) => {
+    renderPage();
+    await select(id);
+    fireEvent.click(screen.getByTestId("supervisor-review-approve"));
+    expect(screen.getByTestId("supervisor-review-error").textContent).toContain(message);
+    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).includes("/approve"))).toBe(false);
+  });
+
+  it.each(["bill_waiver", "refund", "ar_ap_adjustment"])("%s 评估先提示受益人，填写后提交", async (action) => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("supervisor-review-action-type")).toBeTruthy());
+    fireEvent.change(screen.getByTestId("supervisor-review-action-type"), { target: { value: action } });
+    fireEvent.change(screen.getByTestId("supervisor-review-violation-summary"), { target: { value: "已核对业务原因" } });
+    fireEvent.click(screen.getByTestId("supervisor-review-evaluate"));
+    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("请填写直接受益人用户编号");
+    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).includes("/evaluate"))).toBe(false);
+    fireEvent.change(screen.getByTestId("supervisor-review-beneficiary"), { target: { value: "usr_benef" } });
+    fireEvent.click(screen.getByTestId("supervisor-review-evaluate"));
+    await waitFor(() => expect(screen.getByTestId("supervisor-review-current-review").textContent).toContain("review_1"));
+    const call = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes("/evaluate"));
+    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({ action_type: action, beneficiary_id: "usr_benef", amount_cent: 0 });
+  });
+
+  it("真实目录、待审枚举及抽屉均显示中文，未知枚举不泄漏原码", async () => {
+    renderPage(ADMIN_PERMS, { policies: [...REAL_POLICIES, { policy_code: "unknown-policy-id", action_type: "future_action", required_evidence_types: ["future_evidence"], status: "active" }] });
+    await select();
+    const table = screen.getByTestId("supervisor-review-table");
+    for (const row of REAL_POLICIES) {
+      expect(table.textContent).toContain(row.policy_code);
+      const actionCell = screen.getByText(row.policy_code).closest("tr")?.children[1];
+      expect(actionCell?.textContent).toMatch(/[\u4e00-\u9fff]/);
+      expect(actionCell?.textContent).not.toContain(row.action_type);
+      const evidenceCell = screen.getByText(row.policy_code).closest("tr")?.children[4];
+      for (const code of row.required_evidence_types) expect(evidenceCell?.textContent).not.toContain(code);
+    }
+    expect(table.textContent).not.toContain("future_action");
+    expect(table.textContent).not.toContain("future_evidence");
+    const pending = screen.getByTestId("supervisor-review-pending").textContent;
+    for (const code of ["bar.bill_waiver", "bar_bill", "one_level_up", "two_levels_up"]) expect(pending).not.toContain(code);
+    fireEvent.click(screen.getByText("bill_waiver_l3"));
+    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("免单");
+    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("证据附件");
+    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("evidence_attachment");
+    expect(screen.getByTestId("supervisor-review-forward-target").getAttribute("aria-label")).toContain("用户编号");
+    expect(screen.getByTestId("supervisor-review-forward-target").getAttribute("placeholder")).toContain("不是登录名");
+    expect(screen.getByTestId("supervisor-review-final-approver").closest("label")?.textContent).toContain("用户编号");
+    expect(screen.getByTestId("supervisor-review-beneficiary").closest("label")?.textContent).toContain("用户编号");
+  });
+
+  it("第三人不能按用户编号转交给直接受益人", async () => {
+    renderPage();
+    await select();
+    fireEvent.change(screen.getByTestId("supervisor-review-forward-target"), { target: { value: "usr_benef" } });
+    fireEvent.click(screen.getByTestId("supervisor-review-forward"));
+    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("不得转交给直接受益人");
+    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).includes("/forward"))).toBe(false);
+  });
+});
diff --git a/apps/web/src/admin/pages/SupervisorReviewPage.tsx b/apps/web/src/admin/pages/SupervisorReviewPage.tsx
index cedd05d2..96fd5009 100644
--- a/apps/web/src/admin/pages/SupervisorReviewPage.tsx
+++ b/apps/web/src/admin/pages/SupervisorReviewPage.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import React from "react";
 import { formatBusinessStatus } from "../i18n/statusLabels";
 import { TechDetails } from "../components/TechDetails";
@@ -11,9 +12,28 @@ import { useAdminResource } from "../hooks/useAdminResource";
 import type { AdminPageProps, SupervisorReviewPolicyRow } from "../types";
 import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
 
+const REVIEW_LABELS: Record<string, string> = {
+  "staff.shift.publish": "发布排班", "staff.payroll.penalty_cap.adjust": "调整工资扣罚上限",
+  bill_waiver: "免单", "bar.bill_waiver": "免单", refund: "退款", ar_ap_adjustment: "调账",
+  permission_grant_l1: "一级权限授权", permission_grant_l2: "二级权限授权", permission_grant_l3: "三级权限授权",
+  "finance.tax_rule.adjust": "调整税费规则",
+  reason: "原因说明", before_after: "变更前后资料", operator_note: "操作说明",
+  evidence_attachment: "证据附件", violation_summary: "违规说明",
+  contract_snapshot: "合同快照", definition_snapshot: "基础定义快照",
+  one_level_up: "一级上级审批", two_levels_up: "两级上级审批",
+  top_authority: "最高权限审批", specific_role: "指定岗位审批",
+  contract: "合同", bar_bill: "酒吧账单", access_grant_request: "权限授权申请",
+};
+
+function formatReviewValue(value?: string | null) {
+  if (!value) return "-";
+  const label = REVIEW_LABELS[value] ?? formatBusinessStatus(value);
+  return /[a-zA-Z]/.test(label) ? "未识别类型" : label;
+}
+
 const TEXT = {
   title: "主管复核与违规阻断",
-  description: "覆盖高风险业务动作的默认阻断、主管复核评估和审批放行留痕；所有放行动作必须带幂等键、证据附件和审批意见。",
+  description: "覆盖高风险业务动作的默认阻断、主管复核评估和审批放行留痕；放行动作必须带幂等键和审批意见，按策略提供证据附件。",
   evidence: "接口证据：读取主管复核策略、提交评估、提交审批。路径为系统内部接口。",
   loadFailed: "主管复核策略读取失败",
   actionFailed: "主管复核操作失败",
@@ -25,16 +45,16 @@ const TEXT = {
   objectType: "对象类型",
   objectId: "对象编号",
   amountCent: "金额（分）",
-  beneficiary: "直接受益人",
+  beneficiary: "直接受益人用户编号（不是登录名）",
   evidenceAttachment: "证据附件编号",
-  forwardHint: "不得转交给申请人或直接受益人",
+  forwardHint: "填写用户编号（不是登录名）；不得转交给申请人或直接受益人",
   violationSummary: "违规说明",
   reviewerLevel: "所需主管级别",
   approvalComment: "审批意见",
   evaluate: "评估违规动作",
   approve: "主管审批放行",
   reject: "主管驳回退回起草人",
-  boundary: "违规动作必须先阻断并生成待复核记录；主管审批只能放行当前复核单，不得绕过证据附件、幂等键和审计留痕。",
+  boundary: "违规动作必须先阻断并生成待复核记录；主管审批只能放行当前复核单，不得绕过策略要求的证据、幂等键和审计留痕。",
   policy: "策略",
   requiredEvidence: "所需证据",
   status: "状态",
@@ -42,7 +62,7 @@ const TEXT = {
   pending: "待复核清单",
   noPending: "当前没有待复核记录",
   dueAt: "到期",
-  forwardTarget: "转交给（账号）",
+  forwardTarget: "转交给（用户编号，不是登录名）",
   forward: "转交审批",
   selectForForward: "转交",
 };
@@ -71,7 +91,9 @@ function makeResult(action: string, request: Record<string, unknown>, result: un
   if (action === "supervisor_review_rejected") {
     return `复核单 ${reviewId} 已驳回，整单退回起草人`;
   }
-  return `复核单 ${reviewId} 已审批通过`;
+  return (result as { reviewStatus?: string } | null)?.reviewStatus === "approved"
+    ? `复核单 ${reviewId} 全部节点已通过，单据生效`
+    : `复核单 ${reviewId} 当前节点已通过，等待后续节点审批`;
 }
 
 function reviewIdFrom(result: unknown) {
@@ -93,7 +115,7 @@ const SUPERVISOR_APPROVE_PERMISSION = "governance.supervisor_review.approve";
 // URL 约定：?kpi=policies[&subset=blocked|active]。
 type KpiFocus = { subset: "all" | "blocked" | "active" };
 type PendingRow = {
-  decision: { reviewId: string; actionType?: string | null; objectType?: string | null; objectId?: string | null; initiatedBy?: string | null; approvalLevel?: string | null; requiredLevel?: number | null };
+  decision: { reviewId: string; policyCode?: string | null; actionType?: string | null; objectType?: string | null; objectId?: string | null; initiatedBy?: string | null; approvalLevel?: string | null; requiredLevel?: number | null };
   schedule?: { dueAt?: string | null; escalationCount?: number | null; forwardedTo?: string | null; beneficiaryId?: string | null } | null;
 };
 
@@ -197,6 +219,10 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
     }
     const finalApprover = finalApproverId.trim();
     const beneficiary = beneficiaryId.trim();
+    if (["bill_waiver", "refund", "ar_ap_adjustment"].includes(actionType) && !beneficiary) {
+      setFormError("请填写直接受益人用户编号");
+      return;
+    }
     if (finalApprover && sameAccount(finalApprover, user)) {
       setFormError("不能指定申请人为最终审批人");
       return;
@@ -227,15 +253,11 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
 
   async function forwardReview() {
     if (!forwardReviewId || !forwardTarget.trim()) {
-      setFormError("请先选择待复核记录并填写转交账号");
+      setFormError("请先选择待复核记录并填写转交用户编号（不是登录名）");
       return;
     }
     const pending = selectedPending(forwardReviewId);
     const target = forwardTarget.trim();
-    if (!pending?.schedule?.beneficiaryId) {
-      setFormError("直接受益人未登记，不能转交");
-      return;
-    }
     if (!pending?.decision.initiatedBy) {
       setFormError("申请人未登记，不能转交");
       return;
@@ -244,7 +266,7 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
       setFormError("不得转交给申请人本人");
       return;
     }
-    if (pending.schedule.beneficiaryId && (target === pending.schedule.beneficiaryId || (sameAccount(pending.schedule.beneficiaryId, user) && sameAccount(target, user)))) {
+    if (pending.schedule?.beneficiaryId && (target === pending.schedule.beneficiaryId || (sameAccount(pending.schedule.beneficiaryId, user) && sameAccount(target, user)))) {
       setFormError("不得转交给直接受益人");
       return;
     }
@@ -283,17 +305,13 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
     return true;
   }
 
-  function validateApprove() {
+  function validateDecision() {
     if (!validateDecisionActor()) {
       return false;
     }
     const pending = selectedPending(latestReviewId);
-    const policy = rows.find((row) => row.actionType === (pending?.decision.actionType ?? actionType));
-    if (!policy) {
-      setFormError("找不到对应复核策略，不能放行");
-      return false;
-    }
-    const needsAttachment = Array.isArray(policy.requiredEvidenceTypes) && policy.requiredEvidenceTypes.length > 0;
+    const policy = rows.find((row) => row.policyCode === pending?.decision.policyCode);
+    const needsAttachment = policy?.requiredEvidenceTypes?.includes("evidence_attachment");
     if (needsAttachment && !evidenceAttachment.trim()) {
       setFormError("请填写证据附件编号");
       return false;
@@ -302,13 +320,13 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
   }
 
   async function approveReview() {
-    if (!validateApprove()) {
+    if (!validateDecision()) {
       return;
     }
     const request = {
       idempotencyKey: nextKey("supervisor-review-approve"),
       operatorNote: "完整后台主管审批放行",
-      evidenceAttachmentIds: [evidenceAttachment.trim()],
+      evidenceAttachmentIds: evidenceAttachment.trim() ? [evidenceAttachment.trim()] : [],
       decision: "approved",
       comment: approvalComment,
     };
@@ -316,13 +334,13 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
   }
   // M007-R6-03（PGM-1.7.1「任一节点驳回则整单退回起草人」）：驳回当前节点，整单退回起草人。
   async function rejectReview() {
-    if (!validateDecisionActor()) {
+    if (!validateDecision()) {
       return;
     }
     const request = {
       idempotencyKey: nextKey("supervisor-review-reject"),
       operatorNote: "完整后台主管驳回",
-      evidenceAttachmentIds: [evidenceAttachment.trim()],
+      evidenceAttachmentIds: evidenceAttachment.trim() ? [evidenceAttachment.trim()] : [],
       decision: "rejected",
       comment: approvalComment,
     };
@@ -334,11 +352,11 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
       title: `${TEXT.policy}明细 · ${row.policyCode ?? row.id}`,
       fields: [
         { label: TEXT.policy, value: row.policyCode ?? row.id },
-        { label: TEXT.actionType, value: formatBusinessStatus(row.actionType) },
+        { label: TEXT.actionType, value: formatReviewValue(row.actionType) },
         { label: TEXT.reviewerLevel, value: displayValue(row.requiredLevel) },
         { label: TEXT.defaultBlocked, value: displayValue(row.blocksByDefault) },
-        { label: TEXT.requiredEvidence, value: Array.isArray(row.requiredEvidenceTypes) ? row.requiredEvidenceTypes.map((item) => formatBusinessStatus(String(item))).join("、") : displayValue(row.requiredEvidenceTypes) },
-        { label: TEXT.status, value: formatBusinessStatus(row.status) },
+        { label: TEXT.requiredEvidence, value: Array.isArray(row.requiredEvidenceTypes) ? row.requiredEvidenceTypes.map((item) => formatReviewValue(String(item))).join("、") : displayValue(row.requiredEvidenceTypes) },
+        { label: TEXT.status, value: formatReviewValue(row.status) },
       ],
     });
   }
@@ -352,11 +370,11 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
             onClick={() => openPolicyDetail(row)}
             onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openPolicyDetail(row); } }}>
             <td>{row.policyCode ?? row.id}</td>
-            <td>{formatBusinessStatus(row.actionType)}</td>
+            <td>{formatReviewValue(row.actionType)}</td>
             <td>{displayValue(row.requiredLevel)}</td>
             <td>{displayValue(row.blocksByDefault)}</td>
-            <td>{Array.isArray(row.requiredEvidenceTypes) ? row.requiredEvidenceTypes.map((item) => formatBusinessStatus(String(item))).join("、") : displayValue(row.requiredEvidenceTypes)}</td>
-            <td>{formatBusinessStatus(row.status)}</td>
+            <td>{Array.isArray(row.requiredEvidenceTypes) ? row.requiredEvidenceTypes.map((item) => formatReviewValue(String(item))).join("、") : displayValue(row.requiredEvidenceTypes)}</td>
+            <td>{formatReviewValue(row.status)}</td>
           <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看复核策略详情" onClick={(event) => { event.stopPropagation(); openPolicyDetail(row); }}>查看详情</button></td></tr>
         ))}</tbody>
       </table>
@@ -394,11 +412,11 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
                   <tbody>{pendingRows.map((row) => (
                     <tr key={row.decision.reviewId} data-testid={`supervisor-review-pending-${row.decision.reviewId}`}>
                       <td>{row.decision.reviewId}</td>
-                      <td>{formatBusinessStatus(row.decision.actionType ?? "")}</td>
-                      <td>{formatBusinessStatus(row.decision.objectType ?? "")} / {row.decision.objectId ?? "-"}</td>
+                      <td>{formatReviewValue(row.decision.actionType ?? "")}</td>
+                      <td>{formatReviewValue(row.decision.objectType ?? "")} / {row.decision.objectId ?? "-"}</td>
                       <td>{row.decision.initiatedBy ?? "-"}</td>
                       <td>{row.schedule?.beneficiaryId || "-"}</td>
-                      <td>{formatBusinessStatus(row.decision.approvalLevel ?? "")}（主管级 {displayValue(row.decision.requiredLevel)}）</td>
+                      <td>{formatReviewValue(row.decision.approvalLevel ?? "")}（主管级 {displayValue(row.decision.requiredLevel)}）</td>
                       <td>{row.schedule?.dueAt ? cnDate(row.schedule.dueAt) : "-"}</td>
                       <td>{(row.schedule?.escalationCount ?? 0) > 0 ? `已升级 ${row.schedule?.escalationCount} 次` : "待审"}{row.schedule?.forwardedTo ? `，已转交给 ${row.schedule.forwardedTo}` : ""}</td>
                       <td><a data-testid={`supervisor-review-chain-link-${row.decision.reviewId}`} href={`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.decision.reviewId)}`}>审批链</a> <button type="button" data-testid={`supervisor-review-pending-select-${row.decision.reviewId}`} onClick={() => { setForwardReviewId(row.decision.reviewId); setLatestReviewId(row.decision.reviewId); }}>{TEXT.selectForForward}</button></td>
@@ -419,7 +437,7 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
               <label>{TEXT.objectId}<input data-testid="supervisor-review-object-id" value={objectId} onChange={(event) => { setObjectId(event.target.value); resetPendingReview(); }} /></label>
               <label>{TEXT.amountCent}<input data-testid="supervisor-review-amount-cent" type="number" value={amountCent} onChange={(event) => { setAmountCent(event.target.value); resetPendingReview(); }} /></label>
               <label>{TEXT.violationSummary}<input data-testid="supervisor-review-violation-summary" value={violationSummary} onChange={(event) => { setViolationSummary(event.target.value); resetPendingReview(); }} /></label>
-              <label>主线最终审批人（可空）<input data-testid="supervisor-review-final-approver" value={finalApproverId} onChange={(event) => { setFinalApproverId(event.target.value); resetPendingReview(); }} /></label>
+              <label>主线最终审批人用户编号（不是登录名，可空）<input data-testid="supervisor-review-final-approver" value={finalApproverId} onChange={(event) => { setFinalApproverId(event.target.value); resetPendingReview(); }} /></label>
               <label>{TEXT.beneficiary}<input data-testid="supervisor-review-beneficiary" value={beneficiaryId} onChange={(event) => { setBeneficiaryId(event.target.value); resetPendingReview(); }} /></label>
               <label>{TEXT.evidenceAttachment}<input data-testid="supervisor-review-evidence-attachment" value={evidenceAttachment} onChange={(event) => setEvidenceAttachment(event.target.value)} /></label>
               <label>{TEXT.approvalComment}<input data-testid="supervisor-review-approval-comment" value={approvalComment} onChange={(event) => setApprovalComment(event.target.value)} /></label>
@@ -434,9 +452,9 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
               >{TEXT.evaluate}</PermissionButton>
               <HighRiskConfirm
                 label={TEXT.approve}
-                impact={`将对复核单 ${latestReviewId || "未生成"} 记录审批通过结论并写入主管审批留痕。`}
+                impact={`将对复核单 ${latestReviewId || "未生成"} 批准当前节点并写入主管审批留痕；全部节点通过后整单才生效。`}
                 disabled={busyAction !== "" || !hasPendingReview}
-                precheck={validateApprove}
+                precheck={validateDecision}
                 onConfirm={() => void approveReview()}
                 permissionsState={effectivePermissions}
                 permissions={[SUPERVISOR_APPROVE_PERMISSION]}
@@ -446,7 +464,7 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
                 label={TEXT.reject}
                 impact={`将驳回复核单 ${latestReviewId || "未生成"} 当前节点，整单退回起草人并写入主管审批留痕。`}
                 disabled={busyAction !== "" || !hasPendingReview}
-                precheck={validateDecisionActor}
+                precheck={validateDecision}
                 onConfirm={() => void rejectReview()}
                 permissionsState={effectivePermissions}
                 permissions={[SUPERVISOR_APPROVE_PERMISSION]}
```

## 5. 逐块 ID 归因（§5 闸 3）

归因对象：findings 顶部已定稿且队列为「是」的 M043-001…008。版本头：两文件原先无版本行，本批因修这些 ID 首次加头；Page 为 V0.01、test 为 V0.03（与派发陈述一致，本席核 diff 首行均为新增 `// 文件版本`）。版本头不是新功能，归「本批触碰该文件」的队列，不单开号。

### 5.1 `SupervisorReviewPage.tsx`（after 485 行，头 V0.01）

| 块 | 位置（after） | 改动 | 归属 |
|---|---|---|---|
| P-H0 | L1 | 首次版本头 V0.01 | 001–008 同文件 |
| P-H1 | L15–32 | 页内 `REVIEW_LABELS` + `formatReviewValue`：先局部表，再 `formatBusinessStatus`，仍含拉丁字母则「未识别类型」 | **001** |
| P-H2 | L36 / L57 | description/boundary 去掉「所有放行必须附件」，改为按策略提供证据 | **002** |
| P-H3 | L48–50 / L65 / L256 | 受益人/转交/hint 改为用户编号不是登录名 | **008** |
| P-H4 | L94–96 | `makeResult` 按 `reviewStatus==="approved"` 分整单通过 vs 当前节点通过 | **003** |
| P-H5 | L118 | `PendingRow.decision` 增读已有信封 `policyCode`（类型声明，无新接口） | **002** |
| P-H6 | L222–225 | evaluate 仅 `bill_waiver`/`refund`/`ar_ap_adjustment` 必填受益人 | **006** |
| P-H7 | L257–264 删除 | 删除「无受益人一律不能转交」 | **004** |
| P-H8 | L269 | `pending.schedule?.beneficiaryId` 可空后再做利益冲突 | **004** |
| P-H9 | L308–320 | `validateApprove`→`validateDecision`：按 `policyCode` 找已加载策略；未命中不拒；仅 `evidence_attachment` 要附件 | **002**（005 共用此预检） |
| P-H10 | L329 / L343 | 空附件发 `[]` 不发 `[""]` | **002**+**005** |
| P-H11 | L337 | 驳回改走 `validateDecision` | **005** |
| P-H12 | L355–377 / L415–419 | 策略表/抽屉/待审动作·证据·对象类型·层级·状态改 `formatReviewValue` | **001** |
| P-H13 | L440 | 终审人标签改为用户编号不是登录名 | **008** |
| P-H14 | L455 / L457 | 确认影响改为节点批准；整单待全部节点 | **003** |
| P-H15 | L457 / L467 | 放行/驳回 `precheck={validateDecision}` | **002**+**005** |

未改：`key={row.id}`（before L351 = after L369 同文）；`sameAccount` 利益冲突；chain 链接；下拉六动作；权限点；无新 fetch 路径。

### 5.2 `SupervisorReviewPage.test.tsx`（after 458 行，头 V0.03）

| 块 | 位置（after） | 改动 | 归属 |
|---|---|---|---|
| T-H0 | L1 | 首次版本头 V0.03 | 001–008 同文件 |
| T-H1 | L14–27 | `REAL_POLICIES` 对齐后端 `listPolicies()` 目录码（无 `id`，见 §8） | **001**+**007** |
| T-H2 | L56–68 | `makeApi` 可注入 policies/pending/reviewStatus/rejectAction；补 `/approve` 成功信封 | **003**+**004**+**007** |
| T-H3 | L73–82 | `renderPage` 第二参透传 | **007** |
| T-H4 | L159–166 | 原「无受益人转交被拦」改为合同无受益人仍 POST `/review_p2/forward` | **004** |
| T-H5 | L258–266 | 原「对不上策略不能放行」改为未知目录仍提交 `/review_p1/approve` | **002** |
| T-H6 | L349–360 | 批准返回 `pending_review` / `approved` 两句；body `decision=approved` 且附件 `[]`；确认框含「当前节点」 | **003**+**007** |
| T-H7 | L362–375 | approve/reject 按 `bill_waiver_l3` 要附件；空则不 confirm；填附件后 decision 分支 | **002**+**005**+**007** |
| T-H8 | L377–385 | `contract_violation_l3`/`refund_l2` 不强迫附件，发出 `[]` | **002**+**007** |
| T-H9 | L387–396 | approve/forward 服务端 400：DOM 中文错误 + `setActionResult` 既有 failed 对象恰好一次 | **004**+**007** |
| T-H10 | L398–407 | review_p4 自审 / review_p5 受益人自审，不发 `/approve` | **007** |
| T-H11 | L409–422 | 三资金动作评估先拦受益人，填写后提交；默认合同路径仍由既有 `evaluateOnce` 无受益人可评估覆盖例外 | **006**+**007** |
| T-H12 | L424–448 | 真实目录/待审/抽屉中文；未知不漏原码；转交/终审/受益人标签含用户编号 | **001**+**007**+**008** |
| T-H13 | L450–457 | 第三人按用户编号转给受益人仍拒 | **004**+**008**+**007** |

`PENDING[0]!` 非空断言、`it.each` 属既有 vitest API，不改框架。原权限/驳回/金额/KPI/抽屉/链断言未删。chain.test 零 diff。

### 5.3 闸 3 结论

本 diff 每一插入/删除行均可归 001–008 之一或同文件版本头。无顺手抽象、无新依赖、无后端、无全局字典、无 ApprovalChainPage、无 HTML。无无法归因行。

## 6. 按 ID 正确性 / 充分性（独立，不沿用派发结论）

### 001 页内局部显示映射 / 未知中文

- 源：`formatReviewValue` 只在本页；`git diff` 对 `customer/i18n/statusLabels.ts` 与 admin re-export 为空。
- 已知码走 `REVIEW_LABELS` 或已有 `STATUS_LABELS`（本席核 `contract_violation`/`manual_release`/`base_definition_violation`/`active`→有效/`disabled`→已禁用/`pending_review`→待复核）。
- 未知：`/[a-zA-Z]/` 则「未识别类型」，测试用 `future_action`/`future_evidence` 断言表中不出现原码。
- 标识例外：策略码、复核单号、对象编号、发起人/受益人/转交目标原值保留。
- 表/抽屉/待审三处均改。待审 `bar.bill_waiver`/`bar_bill`/`one_level_up` 有测不泄漏。
- **充分。** 不是改全局字典，不是新动作。

### 002 按 policyCode 查明确附件，未知交服务端

- 删除 `actionType ===` 未命中即拒。
- `rows.find(policyCode === pending.decision.policyCode)`；未找到时 `needsAttachment` 为假，请求仍发。默认夹具 `bar_bill_waiver_block` 不在 POLICIES，测试「待审动作对不上目录 → 提交服务端校验」能失败于旧闸。
- 仅 `includes("evidence_attachment")`。reason-only 退款与合同快照策略有测发 `[]`。
- 空附件 `trim() ? [trim] : []`。
- 不造 `bar.bill_waiver→bill_waiver` 别名表。
- **充分。**

### 003 节点通过 vs 整单

- `makeResult` 读 camelCase `reviewStatus`（AdminApi `unwrap`/`toCamelCaseKey` 把 `review_status` 转过来；成功测依赖此点，本席实跑绿）。
- 非 approved（含 `pending_review`）走「当前节点已通过，等待后续节点审批」；approved 走「全部节点已通过，单据生效」。句式与 B1 所引 ApprovalChainPage 语义一致，本 diff 未改链页。
- 确认框 impact 改为批准当前节点、全部节点通过后整单才生效；测试断言 confirm 含「当前节点」。
- 驳回分支仍先匹配 `supervisor_review_rejected`，不被 003 误伤。
- **充分。**

### 004 删无受益人一律拒转，保留利益冲突

- 无条件 `!beneficiaryId` return 已删。
- 申请人未登记、转给申请人、转给已有受益人（含当前用户 `sameAccount` 登录名）仍在。
- 可空 `schedule?.beneficiaryId`。
- 合同无受益人转交改为发请求。
- 资金缺受益人：测试用 `rejectAction` 模拟 400 `BENEFICIARY_REQUIRED`，断言 DOM 与 `useAdminAction` L55 `{status:"failed", message}` **恰好一次**，不把成功句当结果。本席核 hook：catch 路径必 `setActionResult({action,status:"failed",message})`，并非「失败不回传」。
- 前端未复制 `requiresBeneficiary` 谓词。
- **充分。**

### 005 两 decision 同预检 / 空数组

- `rejectReview` 与放行同 `validateDecision`；HighRiskConfirm 驳回 `precheck` 从 `validateDecisionActor` 改为 `validateDecision`。
- 缺免单附件时 `window.confirm` 不被调用（HighRiskConfirm L65 预检失败直接 return）。
- 两动作空附件 `[]`。参数化 approve/reject。
- 与 002 同批，未在保留目录闸的情况下把旧 `validateApprove` 套到驳回。
- **充分。**

### 006 仅表单既有三资金动作提示

- 条件字面量只有下拉已有的三码。合同/基础定义/人工放行不进该分支。
- 既有 `evaluateOnce` 默认 `contract_violation`、不填受益人仍生成 `review_1`，可作为合同例外的能失败断言（若误把必填扩到合同，该测会红）。
- 不改后端、不抄 grant/authorization/`.money`。
- **充分。**

### 007 原测试文件公共 UI + fetch

- 新 describe 全走 `renderPage`、点击、`fetch` 信封，不测私有函数、不换框架。
- 覆盖：批准成功两终态、自审/受益人拒绝、真实目录中文、002/004/005/006 行为。
- 原 21 条权限/驳回/转交冲突/KPI/抽屉仍在；chain 2 条未改。
- **充分。**

### 008 ID 标签明确

- `forwardTarget`/`forwardHint`/空目标错误/终审人 label/受益人 TEXT 均含「用户编号」「不是登录名」。
- `sameAccount` 当前用户登录名冲突保留；第三人填受益人 **ID** 仍拒（新测）。
- 无解析服务、无搜索、无新接口。不宣称 username 可接审。
- **充分。**

## 7. 测试与 typecheck（本席实跑，verification-before-completion）

```
cd /home/james/complex-management/apps/web && npm run test:unit -- src/admin/pages/SupervisorReviewPage.test.tsx src/admin/pages/SupervisorReviewPage.chain.test.tsx
```

- exit **0**
- `SupervisorReviewPage.chain.test.tsx (2 tests)`
- `SupervisorReviewPage.test.tsx (36 tests)`
- **Tests 38 passed (38)**；Duration ~1.07s
- jsdom `scrollTo` 未实现提示：非失败（B1 已记录）

```
cd /home/james/complex-management/apps/web && npm run typecheck
```

- `tsc -b --noEmit`；**TYPECHECK_EXIT:0**

派发所述「根红 14FAIL/24PASS」发生在 B3 中途，本席只读禁止回滚 after 树复现红态；只对**现 after 工作区**验证 38 绿与 typecheck 0。绿测不替代逐行归因（已在 §5–6 完成）。

`setActionResult` 失败对象：与 `useAdminAction.ts` L51–56 一致；400 经 `apiErrorFromResponse` 取 JSON `message`。断言 `toHaveBeenCalledExactlyOnceWith` 可失败于「仍报成功」。

## 8. React key 警告（独立事实，不虚 PASS）

命令输出（本席实跑 stderr，vitest 对重复警告只打一条代表）：

```
stderr | src/admin/pages/SupervisorReviewPage.test.tsx > M043 真实审批信封与既有规则 > approve 按 policyCode 而非动作名要求免单附件
Each child in a list should have a unique "key" prop.
Check the render method of `SupervisorReviewPage`.
```

事实：

1. 页面 `key={row.id}` / `data-testid=...-${row.id}` 在 **before 树 L351 已存在**，本 diff 未改该行。
2. 后端 `SupervisorReviewPolicyRecord` **无 id 字段**（`policyCode` 起首）；真实 `listPolicies()` JSON 不会带 `id`。
3. 前端 `SupervisorReviewPolicyRow` 类型声明 `id: string`，与真实信封不一致，属既有类型/渲染缝，不在 001–008 队列。
4. `REAL_POLICIES` 无 `id` 是对真实目录的复现（001「测试用真实服务目录」）；默认 `POLICIES` 仍带假 `id`，故旧测不报 key。
5. 本席判断：这是**既有生产路径**在真实目录夹具下被测试暴露的 React 警告，不是本批引入的新业务规则，也不是 001–008 未修完。给假 `id` 会偏离真实目录；改 `key={row.policyCode}` 不在本批队列，B3 若顺手改将构成闸 3 越界。
6. 不把「测试绿」说成「无 key 警告」。警告存在。B4 不因未修队列外既有警告而整轮 FAIL。

## 9. 对抗式自问（grilling 收窄）

1. 未知策略仍要附件？否，未命中不设 `needsAttachment`。
2. 免单待审 actionType=`bar.bill_waiver` 但 policyCode=`bill_waiver_l3`？按 code 命中附件闸，测已覆盖。
3. 中间节点文案是否仍「已审批通过」？`已审批通过` 字面已从 makeResult 删除。
4. 合同无受益人转交？发 `/forward`。
5. 驳回免单缺附件是否仍先弹确认？precheck 挡住 confirm。
6. 合同评估是否被 006 误伤？`evaluateOnce` 仍绿。
7. 失败是否被当成成功句？failed 对象断言恰好一次。
8. 是否改了 Java/HTML/字典/依赖？binary diff 仅两前端文件。
9. 是否新功能？否，删闸/收窄/改已有文案与测试。
10. chain 是否被「顺手」改？blob 不变。

## 10. 本席明确未验证

- 未开真实浏览器带会话点放行。
- 未跑后端 Java / `complex_test`。
- 未复现 B3 中途红 14/24（只读不回滚）。
- 未读 B4 self raw。

以上不构成 001–008 未修完：源码控制流 + 公共 UI 测 + 独立 diff 已够本步 B4。

## 11. 结论

**B4-r1 本席：PASS**

无条件。正确性：八项均落到定稿最小边界且能被本模块测试失败。充分性：无把根改成标签游戏（004 根是删死闸；008 根是采集键暗示）。闸 3：逐块可归 001–008。无新增功能/依赖/后端/HTML。真实目录无 id 的 key 警告记为既有事实，不虚称不存在，也不把未入队项写成本轮 FAIL。

本票不表示模块 PASS，不进入 B5 的代替，不改 findings 队列。合取仍须席甲本批 B4 亦 PASS。
