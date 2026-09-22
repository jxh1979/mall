CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M043 B4-r2 Codex完整diff审计
版本：V0.01
结论：PASS。步骤B4，B3累计2；模块IN_PROGRESS。Grok先派，本席未读其同轮B4；本票不覆盖外审。
before=e3939c9899d4464e4b5f2798a6b688125153076b
after=97e3cb99c8e2c7113406158539c6ea0da421af0b
完整未过滤diff仅3文件22增11删，无其余路径。逐行已审。
|diff块|归属|核验|
|---|---|---|
|Page V0.01→V0.02及新增withBase import|009/010文件版本义务；010|已有src/basePath.ts函数，无新依赖、路由或链页修改|
|Page策略行key/testid改policyCode|009|实际12政策码互异且后端都有；不以假id或回退掩盖，不动共享types|
|Page href包withBase|010|保留review_id/encodeURIComponent与原a导航；根路径原样，/complex补前缀|
|test V0.03→V0.04和POL-1选择器|009/011|同步真正身份字段，原中文/布尔/Esc断言不删|
|真实目录逐行getByTestId、点击并断言本policyCode明细、Esc清理|009/011|getByTestId遇零/多命中均失败，12条逐一核对，旧undefined可失败|
|chain首次版本头|010/011|文件被本批触及的版本义务|
|chain最终人it.each填/空，空时not.toHaveProperty|011|补原声称的缺键验证，不改产品空值实现|
|chain href双部署it.each与history路径|010/011|公共页面渲染下验证最终href，不直接测withBase；afterEach恢复原路径|
没有无法归因行。001—008原行为未重写，Java/types/ApprovalChainPage/全局字典/HTML/锁均没改。
正确性：路径相对import为pages→src的../../basePath；调用返回保持查询串，避免navigateTo的focus与review_id不一致。policyCode是现响应字段，React key不再undefined；显示旧fallback不在本轮改动，无需扩大范围。
充分性：B3在原生产字节先跑得到3FAIL37PASS，3红分别是根目录假id选择器/真实目录identity及/complex href；空finalApprover断言一开始绿，因为产品已有行为，补的是可失败测试，不冒称生产也有此bug。修后36+4=40PASS/typecheck0；stderr只有原jsdom scrollTo，不再key警告。
约束：真实index SHA d9a65108a1e87bd300c299ca7640f832bd5c2feb771f35a7a2413d32a1748331不变；B3前后四值同锁；没有浏览器/HTTP会话/后端运行声明。没有第三席/子代理。无新finding/添加；等外席完整票后才能清队和B5。
## 完整binary diff

```diff
diff --git a/apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx b/apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx
index 7d30eb32..8314976f 100644
--- a/apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx
+++ b/apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -52,24 +53,26 @@ describe("SupervisorReviewPage 指定最终审批人与审批链入口", () => {
     window.history.replaceState({}, "", "/admin/governance/supervisor-review");
   });
 
-  it("评估时填写最终审批人 → 请求体带 final_approver_id；不填则不带", async () => {
+  it.each(["usr_gm", ""])("评估时最终审批人 %s：填写则带字段，不填则不带", async (approver) => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("supervisor-review-form")).toBeTruthy());
     fireEvent.change(screen.getByTestId("supervisor-review-violation-summary"), { target: { value: "合同主体不一致" } });
-    fireEvent.change(screen.getByTestId("supervisor-review-final-approver"), { target: { value: "usr_gm" } });
+    fireEvent.change(screen.getByTestId("supervisor-review-final-approver"), { target: { value: approver } });
     fireEvent.click(screen.getByTestId("supervisor-review-evaluate"));
     const fetchMock = vi.mocked(globalThis.fetch);
     await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/supervisor-review/evaluate"))).toBe(true));
     const call = fetchMock.mock.calls.find(([input]) => String(input).includes("/supervisor-review/evaluate"));
     const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
-    expect(body.final_approver_id).toBe("usr_gm");
+    if (approver) expect(body.final_approver_id).toBe(approver);
+    else expect(body).not.toHaveProperty("final_approver_id");
   });
 
-  it("待复核清单每行带审批链入口链接（携带 review_id）", async () => {
+  it.each(["", "/complex"])("部署前缀 %s：待复核清单审批链链接保留前缀与 review_id", async (prefix) => {
+    window.history.replaceState({}, "", `${prefix}/admin/governance/supervisor-review`);
     renderPage();
     await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-review_p1")).toBeTruthy());
     const link = screen.getByTestId("supervisor-review-chain-link-review_p1") as HTMLAnchorElement;
-    expect(link.getAttribute("href")).toBe("/admin/governance/approval-chain?review_id=review_p1");
+    expect(link.getAttribute("href")).toBe(`${prefix}/admin/governance/approval-chain?review_id=review_p1`);
     expect(link.textContent).toContain("审批链");
   });
 });
diff --git a/apps/web/src/admin/pages/SupervisorReviewPage.test.tsx b/apps/web/src/admin/pages/SupervisorReviewPage.test.tsx
index dafcd37f..6f2cd573 100644
--- a/apps/web/src/admin/pages/SupervisorReviewPage.test.tsx
+++ b/apps/web/src/admin/pages/SupervisorReviewPage.test.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.03
+// 文件版本：V0.04
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -324,8 +324,8 @@ describe("SupervisorReviewPage 行明细抽屉", () => {
 
   it("点策略行 → 抽屉含该行动作类型（中文）与默认阻断（是/否），无英文枚举，Esc 关闭", async () => {
     renderPage();
-    await waitFor(() => expect(screen.getByTestId("supervisor-review-row-sp-1")).toBeTruthy());
-    fireEvent.click(screen.getByTestId("supervisor-review-row-sp-1"));
+    await waitFor(() => expect(screen.getByTestId("supervisor-review-row-POL-1")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("supervisor-review-row-POL-1"));
     const drawer = screen.getByTestId("row-detail-drawer");
     expect(drawer.textContent).toContain("合同违规");
     expect(drawer.textContent).toContain("是");
@@ -433,6 +433,13 @@ describe("M043 真实审批信封与既有规则", () => {
       const evidenceCell = screen.getByText(row.policy_code).closest("tr")?.children[4];
       for (const code of row.required_evidence_types) expect(evidenceCell?.textContent).not.toContain(code);
     }
+    for (const row of REAL_POLICIES) {
+      const policyRow = screen.getByTestId(`supervisor-review-row-${row.policy_code}`);
+      fireEvent.click(policyRow);
+      expect(screen.getByTestId("row-detail-drawer").textContent).toContain(row.policy_code);
+      fireEvent.keyDown(window, { key: "Escape" });
+      await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
+    }
     expect(table.textContent).not.toContain("future_action");
     expect(table.textContent).not.toContain("future_evidence");
     const pending = screen.getByTestId("supervisor-review-pending").textContent;
diff --git a/apps/web/src/admin/pages/SupervisorReviewPage.tsx b/apps/web/src/admin/pages/SupervisorReviewPage.tsx
index 96fd5009..853ec439 100644
--- a/apps/web/src/admin/pages/SupervisorReviewPage.tsx
+++ b/apps/web/src/admin/pages/SupervisorReviewPage.tsx
@@ -1,5 +1,6 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 import React from "react";
+import { withBase } from "../../basePath";
 import { formatBusinessStatus } from "../i18n/statusLabels";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -366,7 +367,7 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
       <table className="admin-table" data-testid="supervisor-review-table">
         <thead><tr><th>{TEXT.policy}</th><th>{TEXT.actionType}</th><th>{TEXT.reviewerLevel}</th><th>{TEXT.defaultBlocked}</th><th>{TEXT.requiredEvidence}</th><th>{TEXT.status}</th><th>详情</th></tr></thead>
         <tbody>{(kpiFocus ? focusRows : rows).map((row) => (
-          <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`supervisor-review-row-${row.id}`}
+          <tr key={row.policyCode} className="admin-row-clickable" tabIndex={0} data-testid={`supervisor-review-row-${row.policyCode}`}
             onClick={() => openPolicyDetail(row)}
             onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openPolicyDetail(row); } }}>
             <td>{row.policyCode ?? row.id}</td>
@@ -419,7 +420,7 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
                       <td>{formatReviewValue(row.decision.approvalLevel ?? "")}（主管级 {displayValue(row.decision.requiredLevel)}）</td>
                       <td>{row.schedule?.dueAt ? cnDate(row.schedule.dueAt) : "-"}</td>
                       <td>{(row.schedule?.escalationCount ?? 0) > 0 ? `已升级 ${row.schedule?.escalationCount} 次` : "待审"}{row.schedule?.forwardedTo ? `，已转交给 ${row.schedule.forwardedTo}` : ""}</td>
-                      <td><a data-testid={`supervisor-review-chain-link-${row.decision.reviewId}`} href={`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.decision.reviewId)}`}>审批链</a> <button type="button" data-testid={`supervisor-review-pending-select-${row.decision.reviewId}`} onClick={() => { setForwardReviewId(row.decision.reviewId); setLatestReviewId(row.decision.reviewId); }}>{TEXT.selectForForward}</button></td>
+                      <td><a data-testid={`supervisor-review-chain-link-${row.decision.reviewId}`} href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.decision.reviewId)}`)}>审批链</a> <button type="button" data-testid={`supervisor-review-pending-select-${row.decision.reviewId}`} onClick={() => { setForwardReviewId(row.decision.reviewId); setLatestReviewId(row.decision.reviewId); }}>{TEXT.selectForForward}</button></td>
                     </tr>
                   ))}</tbody>
                 </table>

```
