CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M012 B4-r3 Codex 修复内容完整审计
文件版本：V0.01
步骤：B4-r3；模块IN_PROGRESS；B3累计3（历史1+本次2）。
内容：PASS。归因：PASS。最终：PASS（仅本席；未宣称双PASS/B5/模块PASS）。

本席先派Grok，再独立审完本轮全量diff、依赖行为并封存本报告；封存前未读本次Grok报告/原始JSON。当前Grok实际exit待定，结论路径modules/M012-b4-r3-grok-raw.md；完整派发命令在ledger V3.84。独立输入仅冻结锁、已定稿findings V2.18顶部回退说明/七项表与上轮两份完整B4-r2原文。无子代理/第三审/MCP。

## 1. 两树与审计范围
before=7b782a64aac3428a513778eb3fbfc724f8125894
after=9b1e71aa9eef993d24abdb1c7d8b2d4d8c0e1f2f
完整未过滤命令 git diff --binary 7b782a64aac3428a513778eb3fbfc724f8125894 9b1e71aa9eef993d24abdb1c7d8b2d4d8c0e1f2f
实际13673bytes；SHA256 d2ec98882c7d3e965aa6d1efeb0e78fc25b807bdd40524c6aece3350cd0ecb09；6文件16hunk，git diff --check exit0。全树快照覆盖tracked/staged/untracked，真实index SHA256 1657ef91568fc6c00e7fd66f58f4c00461260651323d2ecbf616238602b973a2不变。本次六文件2088行。
取before前相对上一after d06253fa9ef48efd4abf368911981f95d518fc8b仅audit记录变化。取after后才增加B3至3。两树diff没有HTML/后台/types/shared/hooks/routes/依赖/审计产物变更。
当前锁仍V0.11、21映射：BSM-001/003、GEN-001–018、PGM-001。B3前后四值均取回退来源r2 Grok首锁，全等；LOCK-AMEND-COUNT=1。本文独立重算首行，不改锁。

## 2. 本轮全部问题与处理
B4-r2双方内容FAIL/归因PASS后才返回B3。CX-B4-01和Grok权限未请求文案均对应仍在队列的M012-013，无新ID、无重开NO项，符合整批直接回退。其余010/011/012/014/015/016上轮双方均认为已达边界，保留不重写；七项仍待本轮整批验收。
A. 原useAdminResource onStart清全局错误，在分页换hook时遗漏。三页原有getPaged回调现在开始请求前setGlobalError(null)，拓扑字典恢复已有onStart。清除发生在发请求之前，原catch/onError保留，真实失败仍上报。不会在每次render/effect完成后无条件清错；fetchRef更新不触发请求effect，所以父状态更新不制造重新请求循环。没有修改共享hook或新加抽象。
B. 拓扑表和抽屉两处仅在canEntityView且有businessEntityId而当前主体页没有实体时显示未加载。缺权仍不请求端点、显示原“-”；有权且第二页关联未加载提示及翻页真名均保留。没有添加权限或隐藏原可见主内容。
依据仍为013原GEN-002/003/008/012与BSM-003，错误反馈对应既有GEN面；无新增判据。无添加项/HTML问题/未答问题。

## 3. 整批七项内容验收（本席）
| ID | 本席内容 | 本轮检查 |
|---|---|---|
|M012-010|PASS|当前仅013改动；merchant.projectId→project.id及组织商户所属项目/撤销项目反属保留；真实关系用例56套件通过。|
|M012-011|PASS|snake_case真实关系夹具、无主体权限不请求、绑定409失败仍可见保留；本轮仅在原缺权用例扩文案/抽屉断言。|
|M012-012|PASS|导语仍逐字是B2拟文，未重新添加附件或改HTML。|
|M012-013|PASS|r2已验证的真分页/total/本页统计/筛选/末页回退及关联缺项保持，r3两条遗漏均修正；3重试成功回归与缺权限表/抽屉全通过；另4只读重试仍失败探针通过。|
|M012-014|PASS|暂停与黑名单分开、合法枚举局部中文及未知中文保留，客人词表源未改。|
|M012-015|PASS|三页target=currentTarget收窄保持；56套件含真实按钮Enter、radio Space、行自身Enter。|
|M012-016|PASS|真实space-types字典中文优先/八码兼容保持；字典请求仍同权限，恢复清错不改变数据来源。|
历史001–008已修NO、009登记NO不重开。附件上传/创建绑定/项目归属配置器/后台财税守卫等观察不冒称已完成。

## 4. 实际验证
先测试后生产：新增3个读取重试用例，扩原1个缺权用例；生产改前56项中4失败52通过，失败精确为三页last setGlobalError应为null但没有调用，以及无权限主体列期望“-”实得“主体资料未加载”。原有测试目的未删除。
B3修后完整56/56通过；typecheck0。不是只跑新增测试。本席不无理由重复同一56套件，下面引用本会话B3对当前相同六文件执行的结果。

```text

> apps-web@0.1.0 test:unit
> vitest run GovernanceTopologyPage.test.tsx OrganizationMerchantPage.test.tsx SubjectBindingsPage.test.tsx


 RUN  v4.1.9 /home/james/complex-management/apps/web

 ✓ src/admin/pages/SubjectBindingsPage.test.tsx (16 tests) 405ms
 ✓ src/admin/pages/GovernanceTopologyPage.test.tsx (19 tests) 482ms
 ✓ src/admin/pages/OrganizationMerchantPage.test.tsx (21 tests) 2620ms
     ✓ 分页保留商户归档和业态筛选参数，后页商户可达  1110ms
     ✓ 存档最后一页的唯一商户后回到仍有记录的第一页  648ms

 Test Files  3 passed (3)
      Tests  56 passed (56)
   Start at  14:02:29
   Duration  3.71s (transform 311ms, setup 168ms, import 947ms, tests 3.51s, environment 852ms)


Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's getComputedStyle() method: with pseudo-elements
Not implemented: Window's getComputedStyle() method: with pseudo-elements

ACTUAL_EXIT 0
\n
> apps-web@0.1.0 typecheck
> tsc -b --noEmit



ACTUAL_EXIT 0

```

## 5. B4只读补充：重试仍失败不会被清错覆盖
对三页主请求及拓扑字典分别持续返回503，首次失败后点重试，最终全局错误仍为规范化503信息。4通过、56跳过；六源SHA不变。只在Vite transform追加内存测试，没有写源码或新增文件。实际Node命令 node --input-type=module -，工作目录apps/web，stdin如下：
```javascript
import {startVitest} from "vitest/node";
const probes={"GovernanceTopologyPage.test.tsx":"\nit.each([\"/merchants\",\"/space-types\"])(\"M012 B4-r3只读：重试仍失败保留错误 %s\", async (resource) => {\n  const props = renderPage((url, init) => init?.method !== \"POST\" && url.includes(resource)\n    ? new Response(JSON.stringify({success:false,code:\"TEMPORARY_ERROR\",message:\"临时失败\"}), {status:503,headers:{\"content-type\":\"application/json\"}}) : undefined);\n  await screen.findByTestId(\"admin-retry\");\n  vi.mocked(props.setGlobalError).mockClear();\n  fireEvent.click(screen.getByTestId(\"admin-retry\"));\n  await screen.findByTestId(\"admin-retry\");\n  expect(props.setGlobalError).toHaveBeenLastCalledWith(\"服务暂时不可用，请稍后重试\");\n});","OrganizationMerchantPage.test.tsx":"\nit.each([\"/merchants\"])(\"M012 B4-r3只读：重试仍失败保留错误 %s\", async (resource) => {\n  const props = renderPage((url, init) => init?.method !== \"POST\" && url.includes(resource)\n    ? new Response(JSON.stringify({success:false,code:\"TEMPORARY_ERROR\",message:\"临时失败\"}), {status:503,headers:{\"content-type\":\"application/json\"}}) : undefined);\n  await screen.findByTestId(\"admin-retry\");\n  vi.mocked(props.setGlobalError).mockClear();\n  fireEvent.click(screen.getByTestId(\"admin-retry\"));\n  await screen.findByTestId(\"admin-retry\");\n  expect(props.setGlobalError).toHaveBeenLastCalledWith(\"服务暂时不可用，请稍后重试\");\n});","SubjectBindingsPage.test.tsx":"\nit.each([\"/business-entity-bindings\"])(\"M012 B4-r3只读：重试仍失败保留错误 %s\", async (resource) => {\n  const props = renderPage((url, init) => init?.method !== \"POST\" && url.includes(resource)\n    ? new Response(JSON.stringify({success:false,code:\"TEMPORARY_ERROR\",message:\"临时失败\"}), {status:503,headers:{\"content-type\":\"application/json\"}}) : undefined);\n  await screen.findByTestId(\"admin-retry\");\n  vi.mocked(props.setGlobalError).mockClear();\n  fireEvent.click(screen.getByTestId(\"admin-retry\"));\n  await screen.findByTestId(\"admin-retry\");\n  expect(props.setGlobalError).toHaveBeenLastCalledWith(\"服务暂时不可用，请稍后重试\");\n});"};
const ctx=await startVitest("test",["src/admin/pages/GovernanceTopologyPage.test.tsx","src/admin/pages/OrganizationMerchantPage.test.tsx","src/admin/pages/SubjectBindingsPage.test.tsx"],{watch:false,testNamePattern:"M012 B4-r3只读"},{plugins:[{name:"m012-readonly-repeat-failure",enforce:"pre",transform(code,id){const n=id.split("?")[0].split("/").at(-1);if(probes[n])return {code:code+"\n"+probes[n],map:null};}}]});await ctx.close();
```
```text

 RUN  v4.1.9 /home/james/complex-management/apps/web

 ✓ src/admin/pages/SubjectBindingsPage.test.tsx (17 tests | 16 skipped) 30ms
 ✓ src/admin/pages/GovernanceTopologyPage.test.tsx (21 tests | 19 skipped) 44ms
 ✓ src/admin/pages/OrganizationMerchantPage.test.tsx (22 tests | 21 skipped) 31ms

 Test Files  3 passed (3)
      Tests  4 passed | 56 skipped (60)
   Start at  14:05:07
   Duration  1.03s (transform 275ms, setup 152ms, import 854ms, tests 106ms, environment 816ms)



NODE_EXIT 0
SIX_SOURCE_UNCHANGED True

```

## 6. 完整binary diff
````diff
diff --git a/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx b/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
index 62368337..5bf4bf26 100644
--- a/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
+++ b/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.03
+// 文件版本：V0.04
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import userEvent from "@testing-library/user-event";
 import { afterEach, describe, expect, it, vi } from "vitest";
@@ -221,6 +221,11 @@ describe("GovernanceTopologyPage 无 entity.binding.view 角色不请求主体
     // 有权限的主内容仍请求、页面正常渲染
     expect(urls.some((u) => u.includes("merchants"))).toBe(true);
     expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
+    const row = screen.getByTestId("governance-topology-row-m-1-p-1");
+    expect(row.querySelector("td")?.textContent).toBe("-");
+    fireEvent.click(row);
+    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("主体资料未加载");
+    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("平台经营主体");
   });
 });
 
@@ -283,3 +288,21 @@ describe("M012 本次归并回归", () => {
   });
 
 });
+
+
+it("M012-013：重试成功后清除全局读取错误", async () => {
+  let failed = false;
+  const props = renderPage((url, init) => {
+    if (init?.method !== "POST" && url.includes("/merchants") && !failed) {
+      failed = true;
+      return new Response(JSON.stringify({ success: false, code: "TEMPORARY_ERROR", message: "临时读取失败" }), { status: 503, headers: { "content-type": "application/json" } });
+    }
+    return undefined;
+  });
+  await screen.findByTestId("admin-retry");
+  expect(props.setGlobalError).toHaveBeenCalledWith("服务暂时不可用，请稍后重试");
+  vi.mocked(props.setGlobalError).mockClear();
+  fireEvent.click(screen.getByTestId("admin-retry"));
+  await screen.findByTestId("governance-topology-table");
+  expect(props.setGlobalError).toHaveBeenLastCalledWith(null);
+});
diff --git a/apps/web/src/admin/pages/GovernanceTopologyPage.tsx b/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
index ad3b372b..d7a39b9c 100644
--- a/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
+++ b/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.02
+// 文件版本：V0.03
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -69,18 +69,30 @@ function kpiFocusTitle(focus: KpiFocus): string {
 export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, navigateTo, effectivePermissions }: AdminPageProps) {
   const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);
   const entityRows = useServerPagedRows(
-    (page, size) => canEntityView
+    (page, size) => {
+      setGlobalError(null);
+      return canEntityView
       ? api.getPaged<BusinessEntityRow>("/api/v1/platform/business-entities?page=" + page + "&size=" + size)
-      : Promise.resolve({ items: [] as BusinessEntityRow[], total: 0, page: 1, size }),
+      : Promise.resolve({ items: [] as BusinessEntityRow[], total: 0, page: 1, size });
+    },
     500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
   const merchantRows = useServerPagedRows(
-    (page, size) => api.getPaged<MerchantRow>("/api/v1/platform/merchants?page=" + page + "&size=" + size),
+    (page, size) => {
+      setGlobalError(null);
+      return api.getPaged<MerchantRow>("/api/v1/platform/merchants?page=" + page + "&size=" + size);
+    },
     500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
   const projectRows = useServerPagedRows(
-    (page, size) => api.getPaged<GovernanceProjectRow>("/api/v1/platform/projects?page=" + page + "&size=" + size),
+    (page, size) => {
+      setGlobalError(null);
+      return api.getPaged<GovernanceProjectRow>("/api/v1/platform/projects?page=" + page + "&size=" + size);
+    },
     500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
   const spaceRows = useServerPagedRows(
-    (page, size) => api.getPaged<GovernanceSpaceRow>("/api/v1/platform/spaces?page=" + page + "&size=" + size),
+    (page, size) => {
+      setGlobalError(null);
+      return api.getPaged<GovernanceSpaceRow>("/api/v1/platform/spaces?page=" + page + "&size=" + size);
+    },
     500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
   const entities = entityRows.rows;
   const merchants = merchantRows.rows;
@@ -116,7 +128,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
 
   const dictionary = useAdminResource(
     async () => setSpaceTypes(await api.get<SpaceTypeRow[]>("/api/v1/platform/space-types")),
-    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed },
+    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
   );
   const resources = [entityRows, merchantRows, projectRows, spaceRows, dictionary];
   const loading = resources.some((resource) => resource.loading);
@@ -147,7 +159,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
     setDetail({
       title: `拓扑关系 · ${display(merchant.merchantName)}`,
       fields: [
-        { label: "经营主体", value: entity ? display(entity.entityName) : merchant.businessEntityId ? "主体资料未加载" : "-" },
+        { label: "经营主体", value: entity ? display(entity.entityName) : canEntityView && merchant.businessEntityId ? "主体资料未加载" : "-" },
         { label: "统一社会信用代码", value: display(entity?.unifiedSocialCreditCode) },
         { label: "商户", value: merchant.id ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={merchant.id}>{display(merchant.merchantName)}</TraceLink> : display(merchant.merchantName) },
         { label: "项目", value: display(project?.name) },
@@ -190,7 +202,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
                 tabIndex={0}
                 onClick={() => openTopologyDetail(entity, merchant, project)}
                 onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openTopologyDetail(entity, merchant, project); } }}>
-                <td>{entity ? display(entity.entityName) : merchant.businessEntityId ? "主体资料未加载" : "-"}</td>
+                <td>{entity ? display(entity.entityName) : canEntityView && merchant.businessEntityId ? "主体资料未加载" : "-"}</td>
                 <td>{display(entity?.unifiedSocialCreditCode)}</td>
                 <td onClick={(event) => event.stopPropagation()}>{merchant.id ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={merchant.id}>{display(merchant.merchantName)}</TraceLink> : display(merchant.merchantName)}</td>
                 <td>{display(project?.name)}</td>
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
index 5453e9d3..b2149d21 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.06
+// 文件版本：V0.07
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import userEvent from "@testing-library/user-event";
 import { afterEach, describe, expect, it, vi } from "vitest";
@@ -415,3 +415,21 @@ describe("M012 本次组织档案回归", () => {
   });
 
 });
+
+
+it("M012-013：重试成功后清除全局读取错误", async () => {
+  let failed = false;
+  const props = renderPage((url, init) => {
+    if (init?.method !== "POST" && url.includes("/merchants") && !failed) {
+      failed = true;
+      return new Response(JSON.stringify({ success: false, code: "TEMPORARY_ERROR", message: "临时读取失败" }), { status: 503, headers: { "content-type": "application/json" } });
+    }
+    return undefined;
+  });
+  await screen.findByTestId("admin-retry");
+  expect(props.setGlobalError).toHaveBeenCalledWith("服务暂时不可用，请稍后重试");
+  vi.mocked(props.setGlobalError).mockClear();
+  fireEvent.click(screen.getByTestId("admin-retry"));
+  await screen.findByTestId("organization-merchants-table");
+  expect(props.setGlobalError).toHaveBeenLastCalledWith(null);
+});
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
index 06cb6e08..a2cb2099 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.03
+// 文件版本：V0.04
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -135,14 +135,23 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
 
   const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);
   const entityRows = useServerPagedRows(
-    (page, size) => canEntityView ? api.getPaged<BusinessEntityRow>("/api/v1/platform/business-entities?page=" + page + "&size=" + size)
-      : Promise.resolve({ items: [] as BusinessEntityRow[], total: 0, page: 1, size }),
+    (page, size) => {
+      setGlobalError(null);
+      return canEntityView ? api.getPaged<BusinessEntityRow>("/api/v1/platform/business-entities?page=" + page + "&size=" + size)
+      : Promise.resolve({ items: [] as BusinessEntityRow[], total: 0, page: 1, size });
+    },
     500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
   const merchantRows = useServerPagedRows(
-    (page, size, query) => api.getPaged<MerchantRow>("/api/v1/platform/merchants?" + (query || "includeArchived=false") + "&page=" + page + "&size=" + size),
+    (page, size, query) => {
+      setGlobalError(null);
+      return api.getPaged<MerchantRow>("/api/v1/platform/merchants?" + (query || "includeArchived=false") + "&page=" + page + "&size=" + size);
+    },
     500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
   const projectRows = useServerPagedRows(
-    (page, size) => api.getPaged<GovernanceProjectRow>("/api/v1/platform/projects?page=" + page + "&size=" + size),
+    (page, size) => {
+      setGlobalError(null);
+      return api.getPaged<GovernanceProjectRow>("/api/v1/platform/projects?page=" + page + "&size=" + size);
+    },
     500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
   const entities = entityRows.rows;
   const merchants = merchantRows.rows;
diff --git a/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx b/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
index 8f2f1836..e145f2b5 100644
--- a/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
+++ b/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.03
+// 文件版本：V0.04
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import userEvent from "@testing-library/user-event";
 import { afterEach, describe, expect, it, vi } from "vitest";
@@ -271,3 +271,21 @@ describe("M012 本次绑定回归", () => {
     expect(screen.getByTestId("subject-bindings-form-error").textContent).toContain("财税复核");
   });
 });
+
+
+it("M012-013：重试成功后清除全局读取错误", async () => {
+  let failed = false;
+  const props = renderPage((url, init) => {
+    if (init?.method !== "POST" && url.includes("/business-entity-bindings") && !failed) {
+      failed = true;
+      return new Response(JSON.stringify({ success: false, code: "TEMPORARY_ERROR", message: "临时读取失败" }), { status: 503, headers: { "content-type": "application/json" } });
+    }
+    return undefined;
+  });
+  await screen.findByTestId("admin-retry");
+  expect(props.setGlobalError).toHaveBeenCalledWith("服务暂时不可用，请稍后重试");
+  vi.mocked(props.setGlobalError).mockClear();
+  fireEvent.click(screen.getByTestId("admin-retry"));
+  await screen.findByTestId("subject-bindings-table");
+  expect(props.setGlobalError).toHaveBeenLastCalledWith(null);
+});
diff --git a/apps/web/src/admin/pages/SubjectBindingsPage.tsx b/apps/web/src/admin/pages/SubjectBindingsPage.tsx
index aca4b239..0d916b81 100644
--- a/apps/web/src/admin/pages/SubjectBindingsPage.tsx
+++ b/apps/web/src/admin/pages/SubjectBindingsPage.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.02
+// 文件版本：V0.03
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -73,7 +73,10 @@ function display(value: unknown) {
 
 export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
   const bindingRows = useServerPagedRows(
-    (page, size) => api.getPaged<BindingRow>("/api/v1/platform/business-entity-bindings?page=" + page + "&size=" + size),
+    (page, size) => {
+      setGlobalError(null);
+      return api.getPaged<BindingRow>("/api/v1/platform/business-entity-bindings?page=" + page + "&size=" + size);
+    },
     500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
   const bindings = bindingRows.rows;
   const { loading, error, reload } = bindingRows;

````

## 7. 逐hunk归因（全部16，无预过滤）
|文件|hunk|正式ID|说明|
|---|---|---|---|
|apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx|@@ -1,4 +1,4 @@|M012-013|本次013变更的文件版本递增|
|apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx|@@ -221,6 +221,11 @@ describe("GovernanceTopologyPage 无 entity.binding.view 角色不请求主体|M012-013|013缺权文案或读取重试的最小行为回归，保留已有断言|
|apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx|@@ -283,3 +288,21 @@ describe("M012 本次归并回归", () => {|M012-013|013缺权文案或读取重试的最小行为回归，保留已有断言|
|apps/web/src/admin/pages/GovernanceTopologyPage.tsx|@@ -1,4 +1,4 @@|M012-013|本次013变更的文件版本递增|
|apps/web/src/admin/pages/GovernanceTopologyPage.tsx|@@ -69,18 +69,30 @@ function kpiFocusTitle(focus: KpiFocus): string {|M012-013|013恢复原请求开始清全局错误；既有错误捕获不变|
|apps/web/src/admin/pages/GovernanceTopologyPage.tsx|@@ -116,7 +128,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav|M012-013|013恢复原请求开始清全局错误；既有错误捕获不变|
|apps/web/src/admin/pages/GovernanceTopologyPage.tsx|@@ -147,7 +159,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav|M012-013|013未加载文案仅用于有查看权的分页缺项|
|apps/web/src/admin/pages/GovernanceTopologyPage.tsx|@@ -190,7 +202,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav|M012-013|013未加载文案仅用于有查看权的分页缺项|
|apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx|@@ -1,4 +1,4 @@|M012-013|本次013变更的文件版本递增|
|apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx|@@ -415,3 +415,21 @@ describe("M012 本次组织档案回归", () => {|M012-013|013缺权文案或读取重试的最小行为回归，保留已有断言|
|apps/web/src/admin/pages/OrganizationMerchantPage.tsx|@@ -1,4 +1,4 @@|M012-013|本次013变更的文件版本递增|
|apps/web/src/admin/pages/OrganizationMerchantPage.tsx|@@ -135,14 +135,23 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,|M012-013|013恢复原请求开始清全局错误；既有错误捕获不变|
|apps/web/src/admin/pages/SubjectBindingsPage.test.tsx|@@ -1,4 +1,4 @@|M012-013|本次013变更的文件版本递增|
|apps/web/src/admin/pages/SubjectBindingsPage.test.tsx|@@ -271,3 +271,21 @@ describe("M012 本次绑定回归", () => {|M012-013|013缺权文案或读取重试的最小行为回归，保留已有断言|
|apps/web/src/admin/pages/SubjectBindingsPage.tsx|@@ -1,4 +1,4 @@|M012-013|本次013变更的文件版本递增|
|apps/web/src/admin/pages/SubjectBindingsPage.tsx|@@ -73,7 +73,10 @@ function display(value: unknown) {|M012-013|013恢复原请求开始清全局错误；既有错误捕获不变|

全部修改行归既有YES M012-013，其他六项已有修复保留；内容PASS/归因PASS。不存在无ID行、顺改共享源或新业务规则。

## 8. 当前六文件实算
```json
{
  "apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx": {
    "sha256": "985196ad40864145f1be814cad1f5db6667883a114aed27e61453188219a28cb",
    "bytes": 16571,
    "lines": 308,
    "version": "// 文件版本：V0.04"
  },
  "apps/web/src/admin/pages/GovernanceTopologyPage.tsx": {
    "sha256": "6204706ba5c97e53bfeb200799ddc7a39b615a18f69e1d71607dfb56765a5a89",
    "bytes": 18169,
    "lines": 276,
    "version": "// 文件版本：V0.03"
  },
  "apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx": {
    "sha256": "d40c83d948e0b07f83f6eb5bcbb2a18af12b186ddd93fdcc651557d5870408a7",
    "bytes": 26133,
    "lines": 435,
    "version": "// 文件版本：V0.07"
  },
  "apps/web/src/admin/pages/OrganizationMerchantPage.tsx": {
    "sha256": "28f31ae9b3b731c7e3695cb3d928fc69613b1124b7d5dd970a3586e58b2c4d49",
    "bytes": 32157,
    "lines": 521,
    "version": "// 文件版本：V0.04"
  },
  "apps/web/src/admin/pages/SubjectBindingsPage.test.tsx": {
    "sha256": "ec22a9250ed24f61031f1997119b5537d77d1af537e388eff2018fa947d8591b",
    "bytes": 15482,
    "lines": 291,
    "version": "// 文件版本：V0.04"
  },
  "apps/web/src/admin/pages/SubjectBindingsPage.tsx": {
    "sha256": "94f4d4dcfcb24c85701450794544574efe26f209a4aed242ff5c428af98d247c",
    "bytes": 14784,
    "lines": 257,
    "version": "// 文件版本：V0.03"
  }
}
```

## 9. 限制及最终票
本轮无真实浏览器/HTTP/后台或数据库执行；jsdom原scrollTo/getComputedStyle提示保留，不隐藏。无HTML修改/提交/推送/合并/部署。技能沿本任务已经读取的TDD、请求审查/接收审查、验证收口和ponytail收窄口径；只由Grok外审，不派第三席。
最终 PASS；内容PASS、归因PASS。当前只封存本席，须收齐Grok本次完整票才合取；若其FAIL不得覆盖。当前B3=3，仍IN_PROGRESS，不提前写模块PASS。
