CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M043 B4-r1 Codex修复内容审计
版本：V0.01
步骤B4；模块IN_PROGRESS；B3累计1。先派Grok，再由本席完整审查并封存本票；未读取Grok本次B4报告。结论：PASS；§5闸3逐行归因PASS。此票不是模块PASS。

## 基线与完整范围
before=f26b340bfa35c31ecdc0329256e8ce1d40620c0f
after=47af849df0eb7c41c6a7b962723786eca1baecb9
临时新索引git add -A / write-tree覆盖全部路径；全量binary diff仅Page与原test两文件，203增50删；chain.test未改。主index SHA d9a65108a1e87bd300c299ca7640f832bd5c2feb771f35a7a2413d32a1748331未变。无HTML、Java、全局字典、计划或锁修改。原旧PR尚未发布的更早增量是before现文，B5会整模块重审；本票不以两文件diff代替B5。

## 全部差异归因（含版本、测试装置）
- Page版本头：001—008本批原文件修复的自身版本义务；test版本头：007测试修复，自V0.01→V0.02误断言修正→V0.03数组确定夹具非空类型修正，未跳过版本。
- Page新增局部REVIEW_LABELS及formatReviewValue、策略表/抽屉/待审全部调用替换：001。复用全局已有中文，仅补真实服务目录缺中文码；unknown英码为中文占位，单据/对象/用户编号保留；不增加下拉业务动作。码表是显示映射，不是002禁止的业务策略别名。
- description/boundary删除一律附件过声称、PendingRow补API已有policyCode、validateApprove改名validateDecision及两个handler/precheck：002/005。查的是服务端政策键；未命中不造别名不硬拒。只有evidence_attachment才附件必填，其他reason等由已有意见/服务端执行。两request空附件[]。身份、意见、PermissionButton/HighRiskConfirm仍在。
- makeResult的reviewStatus分支和HighRiskConfirm影响语句：003。与现有ApprovalChainPage语义一致，中间pending_review与最终approved不同，驳回/evaluate/forward分支未改变。
- evaluate三动作beneficiary校验：006。仅下拉已有免单/退款/调账，现服务端requiresBeneficiary已要求；合同例外保留，无新业务约束。
- 删除无受益人一概拒转及schedule?.：004。保留缺申请人拒、申请人/已登记受益人ID及当前username拒；资金缺受益人仍API拒，错误通过existing useAdminAction展示。
- 受益人/终审人/转交label与hint/error：008。明确用户编号非登录名；不发明解析接口，不声称能接受第三人任意username。
- 测试REAL_POLICIES、makeApi可选真实信封/失败响应、renderPage同套边界参数、select公共交互/afterEach清理：007与对应001—006的可执行装置；未引入框架、私有函数测试或网络依赖。
- 原错误合同转交测试改为请求可达：004；原目录找不到拒测改服务端可达：002。其余21条原用例（权限、最终人、无申请人、KPI、drawer）未删。
- 新pending_review/approved参数测：003/007，检查结果、decision、空附件、意见及当前节点确认。
- 已知policyCode免单两decision缺/带附件：002/005，actionType仍bar.bill_waiver确保不是按actionType匹配；reason-only/refund与contract_snapshot不强要附件：002。
- API approve/forward真实400测试：002/004，失败结果只回传一次failed对象，错误显示，不报告中文成功。
- 自审/受益人自审直接approve测试：007；三动作评估先拒后发并验证金额、beneficiary：006；真实12目录全中文/未知中文/待审及抽屉和IDlabels：001/007/008；第三人按beneficiary ID禁转：008/004。

## 复核证据与边界
完整diff逐行读完；追读useAdminAction确认400只返回failed、不会调用makeResult；原chain75行全文读取，最终审批人和真实链链接仍保留。数据从AdminApi已有snake→camel转换，policyCode/证据数组无需新接口。
可执行红绿：测试增量在原Page上14FAIL24PASS，行为失败含误拦/中文/节点/三动作缺受益人。首次实现后36PASS2FAIL是新测试对failed回传的误假设（没有掩盖生产失败），按原hook改成明确failed且单次回传。最终38PASS（36+2），typecheck exit0。只断言公共UI和fetch信封，不测内部私有函数。
现有React无id key提示被真实目录fixture显出：页row无本地状态、key从旧代码未变，本轮未发现首次正常操作的数据错位；保留观察，不冒充已修。jsdom scrollTo提示为原KPI测试环境能力，现真实断言通过。未做浏览器/真实登录端到端，不声称已部署。
无新finding，无添加，八项最小修法全部覆盖。没有把服务端拒绝删掉，也没有拿测试绿代替逐行审计。Grok仍享否决；本票仅self PASS，双票后才能进B5。

## 完整未过滤binary diff

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
