CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M012 B4-r5 Codex完整差异审计
版本：V0.01

用户明确追加一次、限018/019及原测试。两席B2 V0.08 PASS，findings已定稿018/019队列是。B3累计5，IN_PROGRESS；本报告独立封存前未读取本次Grok B4正文/JSON。实际使用requesting-code-review按V0.17只派Grok、verification-before-completion据实验证，不派第三席。

## 完整范围与逐块归因
before=c218cfb17edcfdf278a96cf9b3c6e5b32192fd2e；after=3eca41eb9aa21c6791ef70d5695deb940abeb992。
完整binary diff SHA256 794a06ffc17723e33a55f27ddb66a0d6f295c051bf70c573a297c83c2ef241f1；只有两个文件，六hunk，未过滤diff。主index SHA仍d9a65108a1e87bd300c299ca7640f832bd5c2feb771f35a7a2413d32a1748331。
- test首行V0.07→V0.08：018/019测试变更版本。
- test314hunk新增prompt注释与断言：019真实申请删除点击路径的实际prompt参数；既有reason/URL/POST断言保留。
- test433hunk新增describe：018，现renderPage真实组件+AdminApi、只mock网络；501分页先未命中后命中，正常商户KPI过滤隐藏及项目KPI整表隐藏，返回全部后均命中。清理test state归018防本新用例污染。
- Page首行V0.05→V0.06：018/019页面变更版本。
- Page253hunk只改现prompt：019，与后台最高权审批/master本人即时执行一致，不改权限/请求/删除方法。
- Page413hunk原无条件成功变条件：018，!loading&&!error与AdminDataState的展示前提一致，非商户KPI隐藏表不能成功，visibleMerchants真正包含目标才成功；否则B2已定稿文案。无跨页扫描/新接口/额外数据访问。

## 正确性/反例
返回全部后既有状态更新令提示立即成功；参数化两个隐藏场景各有真实行不存在断言。后页测试使用501条真实DTO分页，页1无目标、页2有目标；成功断言含目标ID而非自生成预期。loading/error先决阻止旧rows在加载/错误页形成假成功。focus缺省仍无横幅。没有变更原滚动效应、筛选、分页、权限、后端。
019一条文案包含两种真实后果，理由校验、审批权限、占用、自审守卫均未变。HTML三个文件未改；其它M012四源未改。

## 验证
先仅落测试：npm run test:unit -- src/admin/pages/OrganizationMerchantPage.test.tsx，exit1，4FAIL20PASS：原prompt错误1、初始假定位1、两种隐藏假定位2。失败均期望行为对照，非类型/测试装配错误。
最小代码后：npm run test:unit -- src/admin/pages/GovernanceTopologyPage.test.tsx src/admin/pages/OrganizationMerchantPage.test.tsx src/admin/pages/SubjectBindingsPage.test.tsx，exit0，3files59PASS（19/24/16）。npm run typecheck exit0。git diff --check两树exit0。jsdom原scrollTo/getComputedStyle提示如实保留；未跑真实浏览器、HTTP、后台或DB。
B3前后锁实算/sidecar/ledger当前顶行/触发B1grok四值全部首行值，LOCK-AMEND-COUNT1。没有本次无ID源码行、无越界副作用；报告/ledger为规定审计产物不混源码diff。

## 完整diff（逐字）
```diff
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
index b2149d21..ce494c75 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.07
+// 文件版本：V0.08
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import userEvent from "@testing-library/user-event";
 import { afterEach, describe, expect, it, vi } from "vitest";
@@ -314,6 +314,8 @@ describe("OrganizationMerchantPage 存档/删除写路径（M012-008）", () =>
     fireEvent.click(screen.getByTestId("merchant-actions-m-3"));
     await waitFor(() => expect(screen.getByTestId("merchant-delete-request-m-3")).toBeTruthy());
     fireEvent.click(screen.getByTestId("merchant-delete-request-m-3"));
+    // M012-019：申请框须说明密钥本人会直接执行，不能承诺所有提交均待审批。
+    expect(window.prompt).toHaveBeenCalledWith("删除理由（由最高权限审批；持授权 U 盘密钥的最高权限本人发起时直接执行）：");
     await waitFor(() => expect(postCalls()).toHaveLength(1));
     const [url, init] = postCalls()[0]!;
     expect(String(url)).toContain("/api/v1/platform/merchants/m-3/delete-request");
@@ -433,3 +435,41 @@ it("M012-013：重试成功后清除全局读取错误", async () => {
   await screen.findByTestId("organization-merchants-table");
   expect(props.setGlobalError).toHaveBeenLastCalledWith(null);
 });
+
+describe("M012-018：下钻提示只报告当前可见商户", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    vi.restoreAllMocks();
+    window.history.replaceState({}, "", "/admin/governance/organizations");
+  });
+
+  it("后页目标未加载时不称已定位，翻页看到目标后才显示成功", async () => {
+    const rows = Array.from({ length: 501 }, (_, i) => ({ ...MERCHANTS[0], id: "m-focus-" + i }));
+    renderPage((url, init) => {
+      if (init?.method === "POST" || !url.includes("/merchants")) return undefined;
+      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
+      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
+    }, { focusObjectId: "m-focus-500" });
+    expect(screen.getByTestId("organization-merchant-focus").textContent).not.toContain("已从下钻定位到商户");
+    await screen.findByTestId("organization-merchant-row-m-focus-0");
+    expect(screen.queryByTestId("organization-merchant-row-m-focus-500")).toBeNull();
+    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("当前列表尚未定位到该商户，请检查筛选或翻页");
+    fireEvent.click(screen.getByTestId("organization-merchants-pager-next"));
+    await screen.findByTestId("organization-merchant-row-m-focus-500");
+    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("已从下钻定位到商户：m-focus-500");
+  });
+
+  it.each([
+    ["?kpi=merchants&status=active", "m-2", "organization-merchants-table"],
+    ["?kpi=projects", "m-1", "organization-projects-table"],
+  ])("目标被当前视图隐藏时不称已定位：%s", async (query, target, table) => {
+    window.history.replaceState({}, "", "/admin/governance/organizations" + query);
+    renderPage(undefined, { focusObjectId: target });
+    await screen.findByTestId(table);
+    expect(screen.queryByTestId("organization-merchant-row-" + target)).toBeNull();
+    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("当前列表尚未定位到该商户，请检查筛选或翻页");
+    fireEvent.click(screen.getByTestId("organization-kpi-focus-clear"));
+    await screen.findByTestId("organization-merchant-row-" + target);
+    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("已从下钻定位到商户：" + target);
+  });
+});
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
index 0255d620..2f1f1cbd 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.05
+// 文件版本：V0.06
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -253,7 +253,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
   }
 
   async function requestDeleteMerchant(id: string) {
-    const reason = window.prompt("删除理由（提交后需上级审批）：");
+    const reason = window.prompt("删除理由（由最高权限审批；持授权 U 盘密钥的最高权限本人发起时直接执行）：");
     if (reason === null || !reason.trim()) { setGlobalError("删除理由必填"); return; }
     setGlobalError(null);
     try {
@@ -413,7 +413,11 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
     <section data-testid="organization-merchant-page">
       <AdminPageHeader title={TEXT.title} description={TEXT.description} />
       {focusObjectId && (
-        <div className="admin-state" data-testid="organization-merchant-focus">已从下钻定位到商户：{focusObjectId}（在下表中查看其档案与归属）</div>
+        <div className="admin-state" data-testid="organization-merchant-focus">
+          {!loading && !error && (!kpiFocus || kpiFocus.table === "merchants") && visibleMerchants.some((row) => row.id === focusObjectId)
+            ? <>已从下钻定位到商户：{focusObjectId}（在下表中查看其档案与归属）</>
+            : "当前列表尚未定位到该商户，请检查筛选或翻页"}
+        </div>
       )}
       <TechDetails><div className="admin-generic-evidence" data-testid="organization-merchant-evidence">{TEXT.evidence}</div></TechDetails>
       {formError ? <div className="admin-inline-error" data-testid="organization-merchant-form-error">{formError}</div> : null}
```

## 完整结论
本席B4-r5正确性PASS；逐行归因与边界PASS。未发现本批待补项，无添加/HTML闸。须Grok同轮PASS才清018/019队列并进入全模块B5；此票不是模块PASS、不代表已发布。不消费第6轮。
