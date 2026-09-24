CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M018 B4-r2 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T08:35:08Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M018 B3-r2（第 2 次）整批修复 diff：before tree `216496ee6e7865a7935d9b23cd7874aa3ed13b3f` → after tree `c9301ef0a5b95d210633c087dba236dbdb7cd3a1`；冻结 diff `/home/james/audit-tmp/m018_b3_r2.diff`（8161 bytes，SHA256 bdb434c16cd0e449915cc90d6c08b9537e4aeaf2b5b5ba2de2ad1b7fbc7a2920），全文见 §2。定稿 ID：M018-010（findings V2.57，队列「是」）。
- 顺序：Grok 已先派（modules/M018-b4-r2-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐块归因（§5 闸 3）

| 文件 | hunk | ±行数 | 归因 ID |
|---|---|---:|---|
| FloorPlanImportPage.test.tsx | `@@ -41,7 +41,9 @@ function errorEnvelope(status: n` | 4 | 010 |
| FloorPlanImportPage.test.tsx | `@@ -58,7 +60,11 @@ function makeApi(options: { imp` | 6 | 010 |
| FloorPlanImportPage.test.tsx | `@@ -73,7 +79,7 @@ const NO_IMPORT_MANAGE: Effectiv` | 2 | 010 |
| FloorPlanImportPage.test.tsx | `@@ -270,3 +276,27 @@ describe("FloorPlanImportPage` | 24 | 010 |
| FloorPlanImportPage.tsx | `@@ -3,8 +3,9 @@ import { AdminDataState } from "..` | 3 | 010 |
| FloorPlanImportPage.tsx | `@@ -95,6 +96,7 @@ export function FloorPlanImportP` | 1 | 010 |
| FloorPlanImportPage.tsx | `@@ -110,14 +112,22 @@ export function FloorPlanImp` | 12 | 010 |
| FloorPlanImportPage.tsx | `@@ -374,6 +384,7 @@ export function FloorPlanImpor` | 1 | 010 |

- 2 文件 8 个 hunk 全部落到 010（页面：import、`projectsNotice` 状态、`loadSecondaryIfPermitted` 副资源、弹窗降级提示；测试：桩 projectsStatus/计数、renderPage 签名、新增 describe 2 例）。未改路由/后端/OpenAPI/三份 HTML/锁；归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx b/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
index 92776d91..af5fc7e8 100644
--- a/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
+++ b/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
@@ -41,7 +41,9 @@ function errorEnvelope(status: number, code: string, message: string): Response
 }
 
 let lastPost: { url: string; body: Record<string, unknown> } | null = null;
-function makeApi(options: { importsStatus?: number } = {}): AdminApi {
+let projectsRequests = 0;
+function makeApi(options: { importsStatus?: number; projectsStatus?: number } = {}): AdminApi {
+  projectsRequests = 0;
   vi.stubGlobal(
     "fetch",
     vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
@@ -58,7 +60,11 @@ function makeApi(options: { importsStatus?: number } = {}): AdminApi {
         return Promise.resolve(listEnvelope(IMPORTS));
       }
       if (url.endsWith("/floor-plans")) return Promise.resolve(listEnvelope(PLANS));
-      if (url.includes("/platform/projects")) return Promise.resolve(dataEnvelope([{ id: "prj-1", name: "主项目" }]));
+      if (url.includes("/platform/projects")) {
+        projectsRequests += 1;
+        if (options.projectsStatus === 403) return Promise.resolve(errorEnvelope(403, "PERMISSION_DENIED", "当前账号无权查看项目"));
+        return Promise.resolve(dataEnvelope([{ id: "prj-1", name: "主项目" }]));
+      }
       return Promise.resolve(listEnvelope([]));
     }),
   );
@@ -73,7 +79,7 @@ const NO_IMPORT_MANAGE: EffectivePermissionsState = {
 };
 const VIEW_ONLY: EffectivePermissionsState = { status: "ready", value: { grants: ["floor_plan.view"], denies: [], wildcard: false } };
 
-function renderPage(perms: EffectivePermissionsState = ADMIN_PERMS, apiOptions: { importsStatus?: number } = {}) {
+function renderPage(perms: EffectivePermissionsState = ADMIN_PERMS, apiOptions: { importsStatus?: number; projectsStatus?: number } = {}) {
   const setGlobalError = vi.fn();
   const setActionResult = vi.fn();
   const fullProps: AdminPageProps = {
@@ -270,3 +276,27 @@ describe("FloorPlanImportPage 发布、权限与下钻错误（M018-003/004/005/
     await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("无权")));
   });
 });
+
+// M018-010：项目清单是副资源——只授 floor_plan.view 的账号不请求它、平面图表照常；projects 403 只降级提示，不整页「平面图读取失败」。
+describe("FloorPlanImportPage 项目清单副资源降级（M018-010）", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    lastPost = null;
+  });
+
+  it("仅 floor_plan.view：不请求项目清单，平面图表照常渲染", async () => {
+    const { setGlobalError } = renderPage(VIEW_ONLY);
+    await waitFor(() => expect(screen.getByTestId("floor-plans-table").textContent).toContain("一层平面图"));
+    expect(projectsRequests).toBe(0);
+    expect(setGlobalError).not.toHaveBeenCalledWith(expect.stringContaining("读取失败"));
+  });
+
+  it("项目清单 403：平面图表仍渲染，新建弹窗显示降级提示，不报整页读取失败", async () => {
+    const { setGlobalError } = renderPage(ADMIN_PERMS, { projectsStatus: 403 });
+    await waitFor(() => expect(screen.getByTestId("floor-plans-table").textContent).toContain("一层平面图"));
+    expect(projectsRequests).toBe(1);
+    expect(setGlobalError).not.toHaveBeenCalledWith(expect.stringContaining("读取失败"));
+    fireEvent.click(screen.getByTestId("floor-plan-create-open"));
+    expect(screen.getByTestId("floor-plan-projects-degraded").textContent).toMatch(/无权|加载失败/);
+  });
+});
diff --git a/apps/web/src/admin/pages/FloorPlanImportPage.tsx b/apps/web/src/admin/pages/FloorPlanImportPage.tsx
index 4e6a4744..204dafbd 100644
--- a/apps/web/src/admin/pages/FloorPlanImportPage.tsx
+++ b/apps/web/src/admin/pages/FloorPlanImportPage.tsx
@@ -3,8 +3,9 @@ import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
 import { AuditTrailHint } from "../components/AuditTrailHint";
 import { TechDetails } from "../components/TechDetails";
-import { useAdminResource } from "../hooks/useAdminResource";
+import { loadSecondaryIfPermitted, useAdminResource } from "../hooks/useAdminResource";
 import { Can, PermissionButton } from "../security/Can";
+import { hasPermissions } from "../security/PermissionGate";
 import { IconButton } from "../components/IconButton";
 import { UnorderedListOutlined, ProfileOutlined } from "@ant-design/icons";
 import { HighRiskConfirm } from "../components/HighRiskConfirm";
@@ -95,6 +96,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
   const [floorPlanImportDetails, setFloorPlanImportDetails] = React.useState<FloorPlanImportDetails | null>(null);
   const [planForm, setPlanForm] = React.useState(EMPTY_PLAN_FORM);
   const [projects, setProjects] = React.useState<{ id: string; name?: string }[]>([]);
+  const [projectsNotice, setProjectsNotice] = React.useState<string | null>(null);
   const planKeyRef = React.useRef("");
   const importKeyRef = React.useRef("");
   const publishKeyRef = React.useRef("");
@@ -110,14 +112,22 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
   // 行明细抽屉：候选/错误明细数据行整行可点 → 完整字段（枚举中文·序号/行号原样）。
   const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
 
+  // M018-010：项目清单只服务于新建弹窗下拉，是副资源——只授 floor_plan.view 的账号不发请求（后端 /platform/projects 要 governance.project.view + 平台 scope），
+  // 读取失败只降级提示，平面图表照常渲染（同模块营业时间页的 loadSecondary 范式）。
   const { loading, error, reload } = useAdminResource(
     async () => {
       const [list, projectList] = await Promise.all([
         api.get<{ items?: FloorPlan[] } | FloorPlan[]>("/api/v1/floor-plans"),
-        api.get<{ items?: { id: string; name?: string }[] } | { id: string; name?: string }[]>("/api/v1/platform/projects"),
+        loadSecondaryIfPermitted(
+          hasPermissions(effectivePermissions, ["governance.project.view"]),
+          () => api.get<{ items?: { id: string; name?: string }[] } | { id: string; name?: string }[]>("/api/v1/platform/projects"),
+          [] as { items?: { id: string; name?: string }[] } | { id: string; name?: string }[],
+        ),
       ]);
       setFloorPlans(Array.isArray(list) ? list : (list?.items ?? []));
-      setProjects(Array.isArray(projectList) ? projectList : (projectList?.items ?? []));
+      const projectData = projectList.data;
+      setProjects(Array.isArray(projectData) ? projectData : (projectData?.items ?? []));
+      setProjectsNotice(projectList.notice);
     },
     { onError: setGlobalError, fallbackMessage: "平面图读取失败", onStart: () => setGlobalError(null) },
   );
@@ -374,6 +384,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
                   <option value="">请选择</option>
                   {projects.map((project) => <option key={project.id} value={project.id}>{project.name ?? project.id}</option>)}
                 </select></label>
+                {projectsNotice ? <span className="admin-muted-action" data-testid="floor-plan-projects-degraded">{projectsNotice}</span> : null}
                 <label>平面图名称<input data-testid="floor-plan-name" value={planForm.planName} onChange={(e) => setPlanForm({ ...planForm, planName: e.target.value })} /></label>
                 <label>楼栋<input data-testid="floor-plan-building" value={planForm.buildingCode} onChange={(e) => setPlanForm({ ...planForm, buildingCode: e.target.value })} /></label>
                 <label>楼层<input data-testid="floor-plan-floor" value={planForm.floorNo} onChange={(e) => setPlanForm({ ...planForm, floorNo: e.target.value })} /></label>
```

## 3. 内容审计

- 010：`hasPermissions(effectivePermissions, ["governance.project.view"])` 为假不发请求（`loadSecondaryIfPermitted:95-102`），为真走 `loadSecondary`（403 → 「当前账号无权查看该数据」，其他失败 → 通用提示），平面图主资源不受影响；`projects` 空时新建弹窗仍有「请选择所属项目」拦截；与 `VenueBusinessHoursPage.tsx:54-61` 范式同构；不改路由/权限码/后端。RED 摘要：
```
 ✓ src/admin/pages/VenueBusinessHoursPage.test.tsx (5 tests) 312ms
 ✓ src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx (10 tests) 384ms
 ✓ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx (4 tests) 407ms
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx (15 tests | 2 failed) 1645ms
     ✓ 平面图表状态列按后端闭集渲染中文，不含原始英文码 107ms
     ✓ 导入任务表状态 parsed 渲染「已解析」，不直出英文 22ms
     ✓ 候选明细表类型/关联/状态渲染中文，不含原始英文码 37ms
     ✓ 导入错误表按解析器真实字段/错误码渲染中文，不显示未识别 31ms
     ✓ 点候选行弹出抽屉，含类型/关联/状态中文，无英文枚举 37ms
     ✓ 点错误行弹出抽屉含行号/字段/错误码中文与说明，Esc 关闭 39ms
     ✓ 未知候选种类显示未识别，不显示其他或走道 26ms
     ✓ 创建导入下拉只有矢量图和标准数据 138ms
     ✓ 创建导入：来源名称留空被步内拦截；填齐后 POST 带格式/名称/内容且幂等键为页面键 65ms
     ✓ 未勾选候选时发布被拦截（不弹确认、不请求）；勾选后确认 POST 只带勾选的候选且幂等键为页面键 36ms
     ✓ 无 floor_plan.import.manage 时创建导入与发布按钮禁用（新增平面图仍可用） 24ms
     ✓ 无 floor_plan.import.view 时不显示下钻按钮 4ms
     ✓ 下钻读取导入任务失败（403）时报错到全局，不静默 61ms
     × 仅 floor_plan.view：不请求项目清单，平面图表照常渲染 12ms
     × 项目清单 403：平面图表仍渲染，新建弹窗显示降级提示，不报整页读取失败 1004ms
⎯⎯⎯⎯⎯⎯⎯ Failed Tests 2 ⎯⎯⎯⎯⎯⎯⎯
 FAIL  src/admin/pages/FloorPlanImportPage.test.tsx > FloorPlanImportPage 项目清单副资源降级（M018-010） > 仅 floor_plan.view：不请求项目清单，平面图表照常渲染
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx:290:30
 FAIL  src/admin/pages/FloorPlanImportPage.test.tsx > FloorPlanImportPage 项目清单副资源降级（M018-010） > 项目清单 403：平面图表仍渲染，新建弹窗显示降级提示，不报整页读取失败
 ❯ waitForWrapper ../../node_modules/@testing-library/dom/dist/wait-for.js:163:27
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx:296:11
 Test Files  1 failed | 3 passed (4)
      Tests  2 failed | 32 passed (34)
```
- GREEN 摘要：
```
 ✓ src/admin/pages/VenueBusinessHoursPage.test.tsx (5 tests) 304ms
 ✓ src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx (10 tests) 371ms
 ✓ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx (4 tests) 403ms
 ✓ src/admin/pages/FloorPlanImportPage.test.tsx (15 tests) 659ms
 Test Files  4 passed (4)
      Tests  34 passed (34)
```
- `tsc -b --noEmit` exit 0。001–009 无回归（同一 4 文件全绿）。内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m018_b3r2_run.log）

```
START=2026-09-20T08:34:49Z HEAD=e456ba96
CHECK tests OK
CHECK impl OK
BEFORE_TREE=216496ee6e7865a7935d9b23cd7874aa3ed13b3f
== RED: tests only
CHECK tests OK
APPLIED tests 4 edits
RED_VITEST_EXIT=1
 ✓ src/admin/pages/VenueBusinessHoursPage.test.tsx (5 tests) 312ms
 ✓ src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx (10 tests) 384ms
 ✓ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx (4 tests) 407ms
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx (15 tests | 2 failed) 1645ms
     ✓ 平面图表状态列按后端闭集渲染中文，不含原始英文码 107ms
     ✓ 导入任务表状态 parsed 渲染「已解析」，不直出英文 22ms
     ✓ 候选明细表类型/关联/状态渲染中文，不含原始英文码 37ms
     ✓ 导入错误表按解析器真实字段/错误码渲染中文，不显示未识别 31ms
     ✓ 点候选行弹出抽屉，含类型/关联/状态中文，无英文枚举 37ms
     ✓ 点错误行弹出抽屉含行号/字段/错误码中文与说明，Esc 关闭 39ms
     ✓ 未知候选种类显示未识别，不显示其他或走道 26ms
     ✓ 创建导入下拉只有矢量图和标准数据 138ms
== GREEN: impl
CHECK impl OK
APPLIED impl 5 edits
GREEN_VITEST_EXIT=0
 ✓ src/admin/pages/VenueBusinessHoursPage.test.tsx (5 tests) 304ms
 ✓ src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx (10 tests) 371ms
 ✓ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx (4 tests) 403ms
 ✓ src/admin/pages/FloorPlanImportPage.test.tsx (15 tests) 659ms
 Test Files  4 passed (4)
      Tests  34 passed (34)
GREEN_TSC_EXIT=0
AFTER_TREE=c9301ef0a5b95d210633c087dba236dbdb7cd3a1
DIFF bytes=8161 sha256=bdb434c16cd0e449915cc90d6c08b9537e4aeaf2b5b5ba2de2ad1b7fbc7a2920
 .../src/admin/pages/FloorPlanImportPage.test.tsx   | 36 ++++++++++++++++++++--
 apps/web/src/admin/pages/FloorPlanImportPage.tsx   | 17 ++++++++--
 2 files changed, 47 insertions(+), 6 deletions(-)
hunks=8 files=2
apps/web/src/admin/pages/FloorPlanImportPage.tsx 443 75f227a783ea7992b77a67aa283eb716a5d8984a8f6479707f671cffa4e10acd
apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx 148 5790a81e551db768b9ee12b23418bd949cddc5e83bd326ce40cd8f9d33a0fa97
apps/web/src/admin/pages/FloorPlanImportPage.test.tsx 302 68af58616c93dfa315ce0a8badc247dc01a48752e672bf19f3ae4dbc4a9a8a8f
apps/web/src/admin/pages/SpaceTypesPage.tsx 280 4d450b0df0de1acdba22e9dec777f8fa11159defd395a3601b19171efd88deed
apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx 166 db80bdf26bdd9ce931e72591d666faf168ca06ad2aa993f91d792feefdfa9e4b
apps/web/src/admin/pages/VenueBusinessHoursPage.tsx 248 841c1e543bd573d6fbfa7e2773f7aa391e58cee430695199e5bc105be32973a9
apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx 119 60f3642dd6161bbabffd75483af9ba9a05d84176ebf7e7461e8c8855c6e6118c
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-20T08:35:03Z
```

## 5. 结论

席甲 B4-r2 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
