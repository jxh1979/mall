CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M012 B4-r3 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r3
席位：固定 Grok 外审席乙 grok-4.6/xhigh
计划：160 审计 V0.17 §4/B4、§5 闸 3
本文件路径：docs/audit-2026-09/modules/M012-b4-r3-grok-raw.md
本文件自身版本：V0.01（指定路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r3 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。B3 累计 3（历史 1 + 本次 2）不得被本票改写，不得因已到上限 3 而降低判定。席乙 FAIL 具否决权；本票内容与归因均不否决。§5 闸 3 越界判定同样须双 PASS。

未读取：任何 `M012-b4-r3-self-raw.md`、席甲本次 B4 raw。回退输入只用 `M012-b4-r2-grok-raw.md` 与 `M012-b4-r2-self-raw.md` 各自本次完整报告。定稿以 findings.md V2.18 顶部回退记录 +「当前 M012 本次重开 B2 整批定稿（双 PASS）」七项表为准。当前队列「是」七项：M012-010–016；009 及历史 001–008 为否，不改观察。七项边界以该表及两份 b1 raw 末尾 B2 定稿为准。

本轮实际审查口径：两个可复核 tree 之间无过滤 binary 全文 diff；回退两条均沿 M012-013 是否按最小边界修好；010/011/012/014/015/016 只核当前相对上轮 after 仍保留，不重修、不把旧历史当当前；每一变更行能否归因到上述「是」ID；是否越界改后台 / types / 共享 hook 与组件源 / 全局客人标签 / 路由 / 三份业务 HTML / 锁。只用冻结 M012 21 映射（BSM-001/003、GEN-001–018、PGM-001），未 LOCK-AMEND。未中途修，未只审单条。审完整份 16 hunk 后才判定。若全部验收 PASS，七项统一确认达到 B2 边界。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum /home/james/complex-management/docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。python3 hashlib 与 `openssl dgst -sha256` 同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger 顶部锁行同值。criteria-lock 版本仍 V0.11。两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M012 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／网页与验证／第1段 |
| 阶段 | B4-r3 只读审计 B3 第 3 次整批修复的「修复内容」本身 |
| 正式队列「是」（7） | M012-010、011、012、013、014、015、016 |
| 正式队列「否」 | M012-009（登记）；历史 001–008（已修，不重开） |
| B3 | 已执行累计 3 次（历史 1 + 本次 2），模块仍 IN_PROGRESS；本票不改台账 |
| 回退来源 | B4-r2 两席内容 FAIL / 归因 PASS；两条均沿已排队 M012-013 直接 B3，不新增 ID |
| before（对象类型 tree） | `7b782a64aac3428a513778eb3fbfc724f8125894` |
| after（对象类型 tree） | `9b1e71aa9eef993d24abdb1c7d8b2d4d8c0e1f2f` |
| 上轮 after（对照保留六项） | `d06253fa9ef48efd4abf368911981f95d518fc8b` |
| 本席自取 `git cat-file -t` | before / after 均为 `tree` |
| 完整 diff 命令 | `git diff --binary 7b782a64aac3428a513778eb3fbfc724f8125894 9b1e71aa9eef993d24abdb1c7d8b2d4d8c0e1f2f`（无路径过滤） |
| 实际差异 | 13673 bytes；SHA256 `d2ec98882c7d3e965aa6d1efeb0e78fc25b807bdd40524c6aece3350cd0ecb09`；UTF-8；6 文件；102 插入 / 19 删除；16 hunk；name-status 六处 M |
| `git diff --check` | exit 0 |
| 两树路径清单 | before 3763 / after 3763；`comm -3` 空（无增删路径） |
| 本席临时索引 `GIT_INDEX_FILE="$(mktemp -d)/index" git add -A && git write-tree` | `d65467b31b02158ea5960f7e3b1886c9410acd03` ≠ after。原因：当前工作区另有审计产物及其它未纳入本批 before/after 快照的路径。六文件 blob 与 after 全等。真实索引未改。 |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`，分支 `main` |
| 本 diff `git diff --name-only -- docs/audit-2026-09/` | 空（本批两树差异不含审计产物） |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3`，完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |
| 后台源码 | `git diff --name-only --binary -- backend/` 空 |
| types.ts | blob `8d26e82118d6f9e607fe85e18ec638a81adc5dd4` 两树及上轮 after 相同 |
| customer/i18n/statusLabels.ts | blob `3ac5cefeb5a3c364de707a29479ba661ec076ffa` 相同 |
| admin/i18n/statusLabels.ts | blob `68e19245525040b7be116356fc9249453198bdc1` 相同 |
| useServerPagedRows / useAdminResource / TablePager / adminClient | 两树及上轮 after blob 全等，只被 import |

上轮 after → 本轮 before：六源文件 blob 全等；差异仅 findings.md / ledger.md / M012-b4-r2 两份 raw（审计产物）。上轮 after → 本轮 after：六源文件 + 上述审计产物。故「当前相对上轮 after 仅 013 源改与审计产物」成立，两次完整 diff 可核。

六文件 blob（工作区＝after）：

| 文件 | before / 上轮 after | after / work | 版本 | 行数 before→after |
|---|---|---|---|---|
| GovernanceTopologyPage.tsx | `ad3b372b59da6297b84d140e5b4c4c18e106be5e` | `d7a39b9c7a4361b364391df91e4e82bfe36aae62` | V0.02→V0.03 | 264→276 |
| GovernanceTopologyPage.test.tsx | `6236833717293dc6d37e101a96961069ccee0d99` | `5bf4bf26411f020c81780c66681b6485099f539f` | V0.03→V0.04 | 285→308 |
| OrganizationMerchantPage.tsx | `06cb6e08a0fe2076d1f64be4c8cf6fe2779bc4fe` | `a2cb209930a4146a88b2090ed25ef3967d773a3d` | V0.03→V0.04 | 512→521 |
| OrganizationMerchantPage.test.tsx | `5453e9d3cdb6bbe89def9636d6d35b01963f8854` | `b2149d21ac69a26ebfb87d4d98632467dae50bd0` | V0.06→V0.07 | 417→435 |
| SubjectBindingsPage.tsx | `aca4b2393c416429f2b555fd63891881dbbc8769` | `0d916b81f72c4d8ea0acf46e43e85121e2ba71db` | V0.02→V0.03 | 254→257 |
| SubjectBindingsPage.test.tsx | `8f2f1836f15674407c7e971b9f0cdefe6720f541` | `e145f2b54a5c3271d7601c382044486e37720872` | V0.03→V0.04 | 273→291 |

六文件合计 2005→2088 行。各文件版本逐次 +1。

`git diff --stat --binary`：

```
 .../admin/pages/GovernanceTopologyPage.test.tsx    | 25 +++++++++++++++++-
 .../web/src/admin/pages/GovernanceTopologyPage.tsx | 30 +++++++++++++++-------
 .../admin/pages/OrganizationMerchantPage.test.tsx  | 20 ++++++++++++++-
 .../src/admin/pages/OrganizationMerchantPage.tsx   | 19 ++++++++++----
 .../src/admin/pages/SubjectBindingsPage.test.tsx   | 20 ++++++++++++++-
 apps/web/src/admin/pages/SubjectBindingsPage.tsx   |  7 +++--
 6 files changed, 102 insertions(+), 19 deletions(-)
```

findings.md 实算 SHA256 `de37985464c776051924b16ac45dcc3f4f0a836abb7c343a8c3dc15d8af5a027`，版本 V2.18。冻结映射仍为 `M012 → [BSM-001, BSM-003, GEN-001 … GEN-018, PGM-001]`，21 项，未增判据。

---

## 2. 回退依据（B4-r2 两席本次完整报告）

Grok r2：拓扑缺 `entity.binding.view` 时不请求 `/business-entities`（011 仍成立），但表/抽屉在 `merchant.businessEntityId` 有值时写「主体资料未加载」，pager 因 `!canEntityView` 不出现，用户无法加载。定稿 013 的「未加载」仅约束独立首页截断，不得把权限未请求写成待加载（GEN-002/015/008）。修复前该列是 `-`。

Codex r2：三页把 `useAdminResource` 换成 `useServerPagedRows` 后，未保留原 `onStart: () => setGlobalError(null)`。共享 hook 只清自己的 `error`，不调 `setGlobalError(null)`。首次 GET 503 后点既有「重试」、服务恢复 200，表出来但宿主顶部仍留「服务暂时不可用，请稍后重试」。

两条均有 ID 且仍在修复队列 → 沿 M012-013 直接 B3，不新分配 ID、不扩大范围。010/011/012/014/015/016 上轮两席视为够，本轮只核仍保留。

---

## 3. 完整两 tree 无过滤 diff 全文

命令：`git diff --binary 7b782a64aac3428a513778eb3fbfc724f8125894 9b1e71aa9eef993d24abdb1c7d8b2d4d8c0e1f2f`

字节 13673，SHA256 `d2ec98882c7d3e965aa6d1efeb0e78fc25b807bdd40524c6aece3350cd0ecb09`。下文为独立重取的整份原文。

```diff
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
```

---

## 4. 16 逐 hunk 正式 ID 归因

闸 3：每一行须能归因到 findings.md 当前归属为「是」的已定稿 ID。本批全部变更是 B4-r2 回退两条沿 M012-013 的最小修复与对应测试/版本行。空白行随新增测试段局部产生，无相邻重构。共享 hook 源未改（fetchPage 经 ref，内联 getter 不会每渲染重拉）。

| # | 文件 | hunk（两树原始范围） | 定稿 ID | 归因票 | 说明 |
|---|---|---|---|---|---|
| 01 | GovernanceTopologyPage.test.tsx | `-1,4 +1,4` | M012-013 | PASS | 文件版本 V0.03→V0.04 |
| 02 | GovernanceTopologyPage.test.tsx | `-221,6 +221,11` | M012-013 | PASS | 既有缺权用例扩表首列 `-`、抽屉不含「主体资料未加载」/「平台经营主体」；不请求断言仍在 |
| 03 | GovernanceTopologyPage.test.tsx | `-283,3 +288,21` | M012-013 | PASS | 503 后重试成功，`setGlobalError` 最后一次为 `null` |
| 04 | GovernanceTopologyPage.tsx | `-1,4 +1,4` | M012-013 | PASS | 文件版本 V0.02→V0.03 |
| 05 | GovernanceTopologyPage.tsx | `-69,18 +69,30` | M012-013 | PASS | 四路 getPaged 回调请求开始 `setGlobalError(null)`；缺权仍 `Promise.resolve` 空页、不发主体请求 |
| 06 | GovernanceTopologyPage.tsx | `-116,7 +128,7` | M012-013 | PASS | 字典恢复 `useAdminResource` 原 `onStart` |
| 07 | GovernanceTopologyPage.tsx | `-147,7 +159,7` | M012-013 | PASS | 抽屉经营主体：`canEntityView && businessEntityId` 才「未加载」，否则 `-` |
| 08 | GovernanceTopologyPage.tsx | `-190,7 +202,7` | M012-013 | PASS | 表列同源条件；015 的 `target===currentTarget` 在上下文未改 |
| 09 | OrganizationMerchantPage.test.tsx | `-1,4 +1,4` | M012-013 | PASS | 文件版本 V0.06→V0.07 |
| 10 | OrganizationMerchantPage.test.tsx | `-415,3 +415,21` | M012-013 | PASS | 组织页 503 重试清全局错误 |
| 11 | OrganizationMerchantPage.tsx | `-1,4 +1,4` | M012-013 | PASS | 文件版本 V0.03→V0.04 |
| 12 | OrganizationMerchantPage.tsx | `-135,14 +135,23` | M012-013 | PASS | 三路 getPaged 开始清错；缺权仍不请求主体 |
| 13 | SubjectBindingsPage.test.tsx | `-1,4 +1,4` | M012-013 | PASS | 文件版本 V0.03→V0.04 |
| 14 | SubjectBindingsPage.test.tsx | `-271,3 +271,21` | M012-013 | PASS | 绑定页 503 重试清全局错误；上一行仍是 011 财税拒绝用例 |
| 15 | SubjectBindingsPage.tsx | `-1,4 +1,4` | M012-013 | PASS | 文件版本 V0.02→V0.03 |
| 16 | SubjectBindingsPage.tsx | `-73,7 +73,10` | M012-013 | PASS | 绑定 getPaged 开始清错 |

16/16 均可归 M012-013（当前「是」）。无归因不到的行，无禁改路径（后台 / types / 共享 hook 与组件源 / 全局标签 / 路由 / 三 HTML / 锁）进入 diff。归因 **PASS**。

---

## 5. 七项内容审查（审完全部 16 hunk 后统一判定）

### M012-013 — 本批唯一源改（回退两条）

**A. 缺权不请求、不误导；有权分页缺项提示保留**

after `GovernanceTopologyPage.tsx:162` 与 `:205`：

`entity ? display(entity.entityName) : canEntityView && merchant.businessEntityId ? "主体资料未加载" : "-"`

`?:` 右结合、`&&` 优先于 `?:`，即无 entity 时仅在「有权且商户带主体 ID」写未加载。缺 `entity.binding.view` 恢复原 `-`。主体 pager 仍 `canEntityView && entityRows.pageCount > 1`。缺权 getter 仍 `Promise.resolve` 空页，不打 `/business-entities`。

本席实跑既有缺权用例：不请求主体、KPI 出现、首列精确 `-`、抽屉不含「主体资料未加载」也不含夹具名「平台经营主体」。有权且主体在第 2 页：先见「主体资料未加载」，点 `governance-entities-pager-next` 后见「经营主体500」。组织页缺权用例仍：商户行可见且 fetch 不含 `/business-entities`（该页本无「未加载」文案，未扩表/抽屉是对的）。

**B. 请求开始清全局错误，不掩盖真实后续读/写失败**

`useServerPagedRows` 用 ref 持有 `fetchPage`，effect 依赖 `[page, pageSize, keyword, tick, fallback]`，内联闭包不会每渲染重拉。`setGlobalError(null)` 只在真正开始拉页时同步执行，然后才 `getPaged`。失败走 `onError: setGlobalError`（503 经 `apiError.ts:202` 规范为「服务暂时不可用，请稍后重试」）。同一次挂载/重试里，各资源 effect 先同步清错再异步结束，后完成的失败仍能写入；`AdminDataState` 在任一 `resource.error` 时只显示错误+重试、不渲染表，读失败期间不能点 pager。写失败路径（组织 `reportWriteFailure`、绑定 `submitChange` catch）失败后不 `reload()`，表单/全局错误保留；既有「存档接口报错 → setGlobalError 收到字符串」「409 财税复核可见且原因仍在」本席实跑仍过。字典只在拓扑恢复原 `onStart`，与换 hook 前一致。

本席实跑三页「M012-013：重试成功后清除全局读取错误」：先见 `admin-retry` 且全局错误为规范文案，点重试后见表，且 `setGlobalError` **最后一次**为 `null`。

**C. 原 013 分页边界仍在（本 diff 未删）**

实跑仍过：拓扑 merchants/spaces 501 见总数并翻第 2 页；主体第 2 页未加载/翻页真名；绑定 501 翻页且摘要含「本页」；组织翻页保留 `includeArchived=false`、存档末页唯一商户后回到仍有记录的第一页。未抬固定上限、未改共享分页器源、未新增聚合 API。

**013 内容：够。**

### M012-010 / 011 / 012 / 014 / 015 / 016 — 核当前仍保留，不重修

对照：本轮 before 六文件 blob = 上轮 after `d06253fa…`；本轮 16 hunk 只叠加 013 清错、未加载条件、对应测试与版本行。上轮认为够的行为在 after 源码与本席 56 例中仍在：

| ID | 当前仍保留的证据（after 源 + 本席实跑） |
|---|---|
| 010 | `projects.filter((project) => project.id === merchant.projectId)`；用例「商户按真实 project_id 归属…项目没有反向 merchant_id 也能显示」过 |
| 011 | 缺权不请求主体（拓扑+组织）；绑定 409 财税复核 + 原因保留过；夹具仍无 Binding `project_id` |
| 012 | `SubjectBindingsPage.tsx` 导语逐字「…变更时须选择状态并填写原因。」；请求体仍无附件 |
| 014 | 局部中文状态表仍在；项目已启用/暂停、商户待准入/暂停/黑名单、绑定暂停；「已封禁」用例仍过 |
| 015 | 三页 `tr` 仍 `event.target === event.currentTarget`；TraceLink Enter、存档按钮 Enter、radio Space 用例仍过 |
| 016 | `formatSpaceType` 字典名优先；kiosk→一体机位且不含 kiosk 仍过 |

未把历史 001–009 当当前队列。009 仍登记否，diff 无附件上传/创建绑定/归属配置器。

**六项内容：仍够。**

### M012-009 与历史 001–008

diff 不涉及。不改观察。

---

## 6. 内容票 / 归因票 / 七项边界

| 票 | 结论 | 依据 |
|---|---|---|
| 内容 | **PASS** | 013 回退两条已按定稿落地；010–012、014–016 相对上轮 after 仍保留 |
| 归因 | **PASS** | 16 hunk / 六文件均可归仍为「是」的 M012-013；无禁改路径 |
| 总结论 | **PASS** | 内容与归因均 PASS |

七项统一确认：M012-010、011、012、013、014、015、016 均达到 B2 已定稿最小修复边界。本票是 B4 席乙单票，B4 双 PASS 后才允许把本轮已修 ID 的修复队列改「否」；本席不改 findings/ledger。有未完不得 PASS——本席未发现未完项。不等于 B5、不等于模块 PASS。B3=3 未降低判定标准。

无新问题、无新 ID、无添加项、无 HTML 申请、无 LOCK-AMEND。观察（不升号、不否决）：三页新重试用例外置在末个 `describe` 之后的顶层 `it`，vitest 仍执行且本席实跑通过。

---

## 7. 实际命令、验证与限制

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
python3 hashlib；openssl dgst -sha256
git cat-file -t 7b782a64aac3428a513778eb3fbfc724f8125894
git cat-file -t 9b1e71aa9eef993d24abdb1c7d8b2d4d8c0e1f2f
git diff --binary <before> <after>   # 13673 bytes，SHA256 d2ec98882c7d3e965aa6d1efeb0e78fc25b807bdd40524c6aece3350cd0ecb09
git diff --name-status/--stat/--numstat/--check --binary <before> <after>
git ls-tree -r --name-only 两树 | wc -l；comm -3 空
git diff --name-only d06253fa9ef48efd4abf368911981f95d518fc8b <before> / <after>
GIT_INDEX_FILE=临时 git add -A && git write-tree   # d65467b3…；真实索引未改
cd apps/web && npm run test:unit -- src/admin/pages/GovernanceTopologyPage.test.tsx src/admin/pages/OrganizationMerchantPage.test.tsx src/admin/pages/SubjectBindingsPage.test.tsx
cd apps/web && npm run typecheck
```

测试实跑：3 files / **56 passed**（拓扑 19、组织 21、绑定 16），exit 0，Duration 4.34s。verbose 复跑同 56/56，含缺权表/抽屉、第 2 页未加载/翻页真名、501 后页、筛选参数、末页存档、三页 503 重试清错、010/014/015/016/011 既有例。保留 jsdom `Window's scrollTo()` / `getComputedStyle() method: with pseudo-elements` 提示，**不称浏览器、不称真实 HTTP**。typecheck `tsc -b --noEmit` exit 0。

未运行：真实浏览器、真实 HTTP、后台测试、Maven、psql、数据库写入。后台/路由/错误映射为只读追流（`useServerPagedRows` ref+effect、`useAdminResource` onStart、`AdminDataState` 重试、`apiError.ts` 5xx 文案）。

未把主工作区回滚到 before 重放「先加测试后改实现」的中间 4 红：两冻结树是整批前/后，before 六文件=上轮 after（当时 53 例且不断言本次两条）；中间红灯不在可复核 tree 对里。after 56 绿 + before 源缺 `canEntityView` 条件与 getPaged 清错，与回退说明一致。

未调用：MCP、任何子代理、操作员提问。未改源码/测试/业务 HTML/锁/findings/ledger/其它报告。未读本次 self-raw。

### 技能（V0.17 §6 收窄，本节点实核路径存在）

| 技能 | 本节点 |
|---|---|
| receiving-code-review | 只借先核源/命令再接受或反驳；禁止表演性同意 56 绿 |
| verification-before-completion | 声称前先跑哈希/diff/vitest/tsc |
| ponytail-review | 只评是否最小恢复 onStart/条件收窄，不落地；未改共享 hook |
| diagnosing-bugs / systematic-debugging | 只读追 013 文案与清错时序 |
| requesting-code-review / tdd / 派子代理 | **未用**（与双席冲突；B4 禁止修复） |
| batch-grill-me | **缺口：本环境无独立技能文件**；未冒充调用 |

---

## 8. 整批结论

**PASS**

内容 PASS，归因 PASS。回退两条均沿 M012-013 修好；其余六项当前仍保留。七项达到 B2 边界。有未完不得 PASS——本席未发现未完项。本票不授权单方进 B5，不修改台账。不覆盖 B1 FAIL 原文与 B2 双 PASS 定稿。历史 001–008 保持已修否；009 保持登记否。不等于模块 PASS。B4 仍须双席合取。
