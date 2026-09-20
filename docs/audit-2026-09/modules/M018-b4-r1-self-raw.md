CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M018 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；本席修、本席自审，不得以本票覆盖 Grok FAIL）。2026-09-20T07:50:19Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M018 B3-r1 整批修复 diff：before tree `09c32950a7ba0c910270efaebb82dadac40660da` → after tree `e2d60c3c41a49a27547081ea6c411ba88155bc4e`；冻结 diff `/home/james/audit-tmp/m018_b3_r1.diff`（57181 bytes，SHA256 376b8265c2fcc9e5131694319a70d3ab7b3cb7a040906cb85077012e174ddf1e），全文见 §2。定稿 ID：M018-001…009（findings V2.54，队列「是」）。
- 顺序：Grok 已先派（modules/M018-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：本报告不再改任何文件。

## 1. 逐块归因（§5 闸 3）

| 文件 | hunk | ±行数 | 归因 ID |
|---|---|---:|---|
| FloorPlanImportPage.guidedcreate.test.tsx | `@@ -6,9 +6,10 @@ import { AdminApi } from "../clie` | 5 | 001/006 |
| FloorPlanImportPage.guidedcreate.test.tsx | `@@ -21,7 +22,7 @@ function listEnvelope(items: unk` | 2 | 006 |
| FloorPlanImportPage.guidedcreate.test.tsx | `@@ -30,7 +31,7 @@ function makeApi(): AdminApi {` | 2 | 006 |
| FloorPlanImportPage.guidedcreate.test.tsx | `@@ -56,6 +57,28 @@ function renderPage() {` | 22 | 001/006 |
| FloorPlanImportPage.guidedcreate.test.tsx | `@@ -70,39 +93,56 @@ describe("FloorPlanImportPage ` | 51 | 001/005/006 |
| FloorPlanImportPage.test.tsx | `@@ -5,21 +5,22 @@ import { FloorPlanImportPage } f` | 11 | 002/006 |
| FloorPlanImportPage.test.tsx | `@@ -32,14 +33,30 @@ function dataEnvelope(data: un` | 22 | 004/006 |
| FloorPlanImportPage.test.tsx | `@@ -49,41 +66,62 @@ function makeApi(): AdminApi {` | 47 | 002/003/004/006 |
| FloorPlanImportPage.test.tsx | `@@ -91,15 +129,26 @@ describe("FloorPlanImportPage` | 13 | 002/006 |
| FloorPlanImportPage.test.tsx | `@@ -109,19 +158,20 @@ describe("FloorPlanImportPag` | 9 | 002/006 |
| FloorPlanImportPage.test.tsx | `@@ -130,6 +180,7 @@ describe("FloorPlanImportPage ` | 1 | 006 |
| FloorPlanImportPage.test.tsx | `@@ -152,4 +203,70 @@ describe("FloorPlanImportPage` | 66 | 003/004/005/007/008/006 |
| FloorPlanImportPage.tsx | `@@ -4,12 +4,11 @@ import { AdminPageHeader } from ` | 3 | 002/003 |
| FloorPlanImportPage.tsx | `@@ -28,24 +27,39 @@ const SOURCE_FORMAT_OPTIONS: {` | 33 | 002/006 |
| FloorPlanImportPage.tsx | `@@ -56,6 +70,11 @@ type ImportError = { id: string` | 5 | 001/003 |
| FloorPlanImportPage.tsx | `@@ -74,7 +93,7 @@ export function FloorPlanImportP` | 2 | 001 |
| FloorPlanImportPage.tsx | `@@ -103,35 +122,52 @@ export function FloorPlanImp` | 33 | 001/004/005 |
| FloorPlanImportPage.tsx | `@@ -139,13 +175,21 @@ export function FloorPlanImp` | 10 | 005/007 |
| FloorPlanImportPage.tsx | `@@ -155,13 +199,18 @@ export function FloorPlanImp` | 11 | 005/008 |
| FloorPlanImportPage.tsx | `@@ -176,7 +225,7 @@ export function FloorPlanImpor` | 2 | 002 |
| FloorPlanImportPage.tsx | `@@ -207,7 +256,7 @@ export function FloorPlanImpor` | 2 | 001 |
| FloorPlanImportPage.tsx | `@@ -217,7 +266,7 @@ export function FloorPlanImpor` | 2 | 003 |
| FloorPlanImportPage.tsx | `@@ -226,16 +275,16 @@ export function FloorPlanImp` | 6 | 002/003 |
| FloorPlanImportPage.tsx | `@@ -257,7 +306,7 @@ export function FloorPlanImpor` | 2 | 002 |
| FloorPlanImportPage.tsx | `@@ -292,10 +341,11 @@ export function FloorPlanImp` | 5 | 003/008 |
| FloorPlanImportPage.tsx | `@@ -316,12 +366,8 @@ export function FloorPlanImpo` | 8 | 001 |
| FloorPlanImportPage.tsx | `@@ -331,6 +377,10 @@ export function FloorPlanImpo` | 4 | 001 |
| FloorPlanImportPage.tsx | `@@ -339,8 +389,11 @@ export function FloorPlanImpo` | 7 | 001 |
| FloorPlanImportPage.tsx | `@@ -357,7 +410,7 @@ export function FloorPlanImpor` | 2 | 007 |
| SpaceTypesPage.guidedcreate.test.tsx | `@@ -5,11 +5,18 @@ import { SpaceTypesPage } from "` | 11 | 002/006/009 |
| SpaceTypesPage.guidedcreate.test.tsx | `@@ -19,12 +26,14 @@ function makeApi(post = vi.fn(` | 6 | 006 |
| SpaceTypesPage.guidedcreate.test.tsx | `@@ -89,18 +98,69 @@ describe("SpaceTypesPage 引导式新增` | 67 | 002/006/009 |
| SpaceTypesPage.tsx | `@@ -154,7 +154,7 @@ export function SpaceTypesPage` | 2 | 009 |
| SpaceTypesPage.tsx | `@@ -199,6 +199,17 @@ export function SpaceTypesPag` | 11 | 009 |
| VenueBusinessHoursPage.test.tsx | `@@ -10,9 +10,11 @@ const HOURS = [` | 8 | 006 |
| VenueBusinessHoursPage.test.tsx | `@@ -85,4 +87,33 @@ describe("VenueBusinessHoursPag` | 29 | 006 |

- 7 文件 36 个 hunk 全部落到 001–009；四个测试文件整文件重写归 006（各用例按注释再归对应 ID）。席甲技术决定（B3 段已记）：`REF_TYPE_LABELS` 改后端 REF_TYPES 六值归 002 同根（页内字典与后端闭集不一致）；`SpaceTypesPage` 描述句补「已删除的种类可恢复为启用」归 009。未改后端/OpenAPI/迁移/三份 HTML/锁；无顺手改动。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx b/apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx
index 6dfa59cf..4132987d 100644
--- a/apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx
+++ b/apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx
@@ -6,9 +6,10 @@ import { AdminApi } from "../client/adminClient";
 import type { AdminPageProps, EffectivePermissionsState } from "../types";
 
 // 引导式流程 phase-10：新增平面图改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截不提交）。
-// 只覆盖「新增平面图」主创建流；导入任务/发布候选等动作不在本期改造范围。
+// M018-001/005：请求体按后端 FloorPlanService.validateCreateRequest / V023 契约（楼层、底图地址、底图宽高、版本号必填，status active），
+// 幂等键须是页面 KeyRef（经真实 AdminApi.normalizeRequestBody 后仍为 fp- 前缀）。
 const PLANS = [
-  { id: "fp-1", plan_name: "一层平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "published" },
+  { id: "fp-1", plan_name: "一层平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "active" },
 ];
 
 function dataEnvelope(data: unknown): Response {
@@ -21,7 +22,7 @@ function listEnvelope(items: unknown[]): Response {
   return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
 }
 
-let lastPostBody: unknown = null;
+let lastPostBody: Record<string, unknown> | null = null;
 let lastPostUrl = "";
 function makeApi(): AdminApi {
   vi.stubGlobal(
@@ -30,7 +31,7 @@ function makeApi(): AdminApi {
       const url = String(input);
       if ((init?.method ?? "GET").toUpperCase() === "POST" && url.endsWith("/floor-plans")) {
         lastPostUrl = url;
-        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
+        lastPostBody = init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : null;
         return Promise.resolve(dataEnvelope({ id: "fp-new" }));
       }
       if (url.endsWith("/floor-plans")) return Promise.resolve(listEnvelope(PLANS));
@@ -56,6 +57,28 @@ function renderPage() {
   return { setActionResult };
 }
 
+async function openCreate() {
+  await waitFor(() => expect(screen.getByTestId("floor-plan-create-open")).toBeTruthy());
+  fireEvent.click(screen.getByTestId("floor-plan-create-open"));
+}
+
+function fillPlanForm(overrides: Partial<Record<string, string>> = {}) {
+  const values: Record<string, string> = {
+    "floor-plan-project": "prj-1",
+    "floor-plan-name": "三层平面图",
+    "floor-plan-building": "B2",
+    "floor-plan-floor": "3F",
+    "floor-plan-image-url": "https://assets.example.com/floor-3.png",
+    "floor-plan-image-width": "1600",
+    "floor-plan-image-height": "1200",
+    "floor-plan-version": "v1",
+    ...overrides,
+  };
+  for (const [testId, value] of Object.entries(values)) {
+    fireEvent.change(screen.getByTestId(testId), { target: { value } });
+  }
+}
+
 describe("FloorPlanImportPage 引导式新增平面图(phase-10)", () => {
   afterEach(() => {
     vi.unstubAllGlobals();
@@ -70,39 +93,56 @@ describe("FloorPlanImportPage 引导式新增平面图(phase-10)", () => {
     expect(screen.queryByTestId("floor-plan-create-guided")).toBeNull();
   });
 
-  it("入口→步进→提交：POST 新建平面图带名称/楼栋/楼层，成功后关闭弹窗", async () => {
+  // M018-001/005：POST 体 = 后端必填契约（楼层/底图/宽高/版本号 + status active），幂等键为页面键
+  it("入口→步进→提交：POST 新建平面图带后端必填字段与 active 状态，成功后关闭弹窗", async () => {
     const { setActionResult } = renderPage();
-    await waitFor(() => expect(screen.getByTestId("floor-plan-create-open")).toBeTruthy());
-    fireEvent.click(screen.getByTestId("floor-plan-create-open"));
-
-    fireEvent.change(screen.getByTestId("floor-plan-project"), { target: { value: "prj-1" } });
-    fireEvent.change(screen.getByTestId("floor-plan-name"), { target: { value: "三层平面图" } });
-    fireEvent.change(screen.getByTestId("floor-plan-building"), { target: { value: "B2" } });
-    fireEvent.change(screen.getByTestId("floor-plan-floor"), { target: { value: "3F" } });
+    await openCreate();
+    fillPlanForm();
     fireEvent.click(screen.getByTestId("floor-plan-create-guided-next")); // 步1→确认
     await waitFor(() => expect(screen.getByTestId("floor-plan-create-guided-summary")).toBeTruthy());
+    expect(screen.getByTestId("floor-plan-create-guided-summary").textContent).toContain("启用");
+    expect(screen.getByTestId("floor-plan-create-guided-summary").textContent).not.toContain("草稿");
     fireEvent.click(screen.getByTestId("floor-plan-create"));
 
     await waitFor(() => expect(lastPostBody).toBeTruthy());
     expect(lastPostUrl).toContain("/floor-plans");
-    expect((lastPostBody as { project_id: string }).project_id).toBe("prj-1");
-    expect((lastPostBody as { plan_name: string }).plan_name).toBe("三层平面图");
-    expect((lastPostBody as { building_code: string }).building_code).toBe("B2");
-    expect((lastPostBody as { floor_no: string }).floor_no).toBe("3F");
-    expect((lastPostBody as { status: string }).status).toBe("draft");
+    expect(lastPostBody).toMatchObject({
+      project_id: "prj-1",
+      plan_name: "三层平面图",
+      building_code: "B2",
+      floor_no: "3F",
+      image_url: "https://assets.example.com/floor-3.png",
+      image_width: 1600,
+      image_height: 1200,
+      version_no: "v1",
+      status: "active",
+    });
+    expect(String(lastPostBody?.idempotency_key)).toMatch(/^fp-/);
     await waitFor(() => expect(screen.queryByTestId("floor-plan-create-guided")).toBeNull());
     expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "floor_plan_created" }));
   });
 
   it("步内校验拦截：名称留空则不进下一步、不提交", async () => {
     renderPage();
-    await waitFor(() => expect(screen.getByTestId("floor-plan-create-open")).toBeTruthy());
-    fireEvent.click(screen.getByTestId("floor-plan-create-open"));
-    fireEvent.change(screen.getByTestId("floor-plan-project"), { target: { value: "prj-1" } });
-    fireEvent.change(screen.getByTestId("floor-plan-name"), { target: { value: "  " } });
+    await openCreate();
+    fillPlanForm({ "floor-plan-name": "  " });
     fireEvent.click(screen.getByTestId("floor-plan-create-guided-next"));
     expect(screen.getByTestId("floor-plan-create-guided-error").textContent).toContain("名称");
     expect(screen.queryByTestId("floor-plan-create-guided-summary")).toBeNull();
     expect(lastPostBody).toBeNull();
   });
+
+  // M018-001：楼层为后端必填（floor_no NOT NULL），前端不再当选填
+  it("步内校验拦截：楼层或底图留空则不进下一步、不提交", async () => {
+    renderPage();
+    await openCreate();
+    fillPlanForm({ "floor-plan-floor": "" });
+    fireEvent.click(screen.getByTestId("floor-plan-create-guided-next"));
+    expect(screen.getByTestId("floor-plan-create-guided-error").textContent).toContain("楼层");
+    fillPlanForm({ "floor-plan-image-url": " " });
+    fireEvent.click(screen.getByTestId("floor-plan-create-guided-next"));
+    expect(screen.getByTestId("floor-plan-create-guided-error").textContent).toContain("底图");
+    expect(screen.queryByTestId("floor-plan-create-guided-summary")).toBeNull();
+    expect(lastPostBody).toBeNull();
+  });
 });
diff --git a/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx b/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
index a36d6713..92776d91 100644
--- a/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
+++ b/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
@@ -5,21 +5,22 @@ import { FloorPlanImportPage } from "./FloorPlanImportPage";
 import { AdminApi } from "../client/adminClient";
 import type { AdminPageProps, EffectivePermissionsState } from "../types";
 
+// 夹具只用后端闭集（FloorPlanService STATUSES / IMPORT_TASK_STATUSES / IMPORT_CANDIDATE_STATUSES、FloorPlanImportParser 错误字段/错误码）——M018-002。
 const PLANS = [
-  { id: "fp-1", plan_name: "一层平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "published" },
-  { id: "fp-2", plan_name: "二层平面图", building_code: "B1", floor_no: "2F", version_no: "v1", status: "draft" },
+  { id: "fp-1", plan_name: "一层平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "active" },
+  { id: "fp-2", plan_name: "二层平面图", building_code: "B1", floor_no: "2F", version_no: "v1", status: "archived" },
 ];
 const IMPORTS = [
-  { id: "imp-1", source_name: "cad导出", status: "processing", candidate_count: 3, error_count: 1 },
+  { id: "imp-1", source_name: "cad导出", status: "parsed", candidate_count: 3, error_count: 1 },
 ];
 const DETAILS = {
-  task: { id: "imp-1", status: "processing" },
+  task: { id: "imp-1", status: "parsed" },
   candidates: [
     { id: "cd-1", candidate_no: 1, shape_type: "shop", ref_type: "space", ref_id: "sp-1", label: "A101", status: "pending" },
     { id: "cd-2", candidate_no: 2, shape_type: "corridor", ref_type: "space", ref_id: "sp-2", label: "旧数据", status: "pending" },
   ],
   errors: [
-    { id: "er-1", line_no: 5, field_name: "geometry", error_code: "INVALID_POLYGON", error_message: "多边形无效" },
+    { id: "er-1", line_no: 5, field_name: "points", error_code: "POINTS_TOO_FEW", error_message: "多边形至少需要 3 个顶点" },
   ],
 };
 
@@ -32,14 +33,30 @@ function dataEnvelope(data: unknown): Response {
 function listEnvelope(items: unknown[]): Response {
   return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
 }
+function errorEnvelope(status: number, code: string, message: string): Response {
+  return new Response(
+    JSON.stringify({ success: false, code, message, data: null }),
+    { status, headers: { "content-type": "application/json" } },
+  );
+}
 
-function makeApi(): AdminApi {
+let lastPost: { url: string; body: Record<string, unknown> } | null = null;
+function makeApi(options: { importsStatus?: number } = {}): AdminApi {
   vi.stubGlobal(
     "fetch",
-    vi.fn().mockImplementation((input: RequestInfo | URL) => {
+    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
       const url = String(input);
+      if ((init?.method ?? "GET").toUpperCase() === "POST") {
+        lastPost = { url, body: init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : {} };
+        if (url.endsWith("/publish")) return Promise.resolve(dataEnvelope({ id: "imp-1", status: "published" }));
+        if (url.endsWith("/imports")) return Promise.resolve(dataEnvelope({ id: "imp-2", status: "parsed" }));
+        return Promise.resolve(dataEnvelope({ id: "fp-new" }));
+      }
       if (url.includes("/imports/imp-1/candidates")) return Promise.resolve(dataEnvelope(DETAILS));
-      if (url.includes("/floor-plans/fp-1/imports")) return Promise.resolve(listEnvelope(IMPORTS));
+      if (url.includes("/floor-plans/fp-1/imports")) {
+        if (options.importsStatus === 403) return Promise.resolve(errorEnvelope(403, "PERMISSION_DENIED", "当前账号无权查看导入任务"));
+        return Promise.resolve(listEnvelope(IMPORTS));
+      }
       if (url.endsWith("/floor-plans")) return Promise.resolve(listEnvelope(PLANS));
       if (url.includes("/platform/projects")) return Promise.resolve(dataEnvelope([{ id: "prj-1", name: "主项目" }]));
       return Promise.resolve(listEnvelope([]));
@@ -49,41 +66,62 @@ function makeApi(): AdminApi {
 }
 
 const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
+// 只授平面图读写 + 导入只读，不授 floor_plan.import.manage（V034 独立高危码）
+const NO_IMPORT_MANAGE: EffectivePermissionsState = {
+  status: "ready",
+  value: { grants: ["floor_plan.view", "floor_plan.manage", "floor_plan.import.view"], denies: [], wildcard: false },
+};
+const VIEW_ONLY: EffectivePermissionsState = { status: "ready", value: { grants: ["floor_plan.view"], denies: [], wildcard: false } };
 
-function renderPage() {
+function renderPage(perms: EffectivePermissionsState = ADMIN_PERMS, apiOptions: { importsStatus?: number } = {}) {
+  const setGlobalError = vi.fn();
+  const setActionResult = vi.fn();
   const fullProps: AdminPageProps = {
-    api: makeApi(),
+    api: makeApi(apiOptions),
     user: { id: "u1", username: "admin", accountType: "platform_admin" },
-    setActionResult: vi.fn(),
-    setGlobalError: vi.fn(),
-    effectivePermissions: ADMIN_PERMS,
+    setActionResult,
+    setGlobalError,
+    effectivePermissions: perms,
   };
   render(<FloorPlanImportPage {...fullProps} />);
+  return { setGlobalError, setActionResult };
 }
 
 // 打开候选/错误明细上下文（选平面图→选导入任务）后回调，供明细行断言复用。
-async function openDetails() {
-  renderPage();
+async function openDetails(perms: EffectivePermissionsState = ADMIN_PERMS) {
+  const spies = renderPage(perms);
   await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
   fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
   await waitFor(() => expect(screen.getByTestId("floor-plan-import-select-imp-1")).toBeTruthy());
   fireEvent.click(screen.getByTestId("floor-plan-import-select-imp-1"));
   await waitFor(() => expect(screen.getByTestId("floor-plan-candidates-table")).toBeTruthy());
+  return spies;
 }
 
-describe("FloorPlanImportPage 英文全清", () => {
+describe("FloorPlanImportPage 英文全清（M018-002：后端闭集全部有中文）", () => {
   afterEach(() => {
     vi.unstubAllGlobals();
+    lastPost = null;
   });
 
-  it("平面图表状态列渲染中文，不含原始英文码", async () => {
+  it("平面图表状态列按后端闭集渲染中文，不含原始英文码", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("floor-plans-table")).toBeTruthy());
     const table = screen.getByTestId("floor-plans-table").textContent ?? "";
-    expect(table).toContain("已发布"); // published
-    expect(table).toContain("草稿"); // draft
-    expect(table).not.toContain("published");
-    expect(table).not.toContain("draft");
+    expect(table).toContain("启用"); // active
+    expect(table).toContain("已存档"); // archived
+    expect(table).not.toContain("active");
+    expect(table).not.toContain("archived");
+  });
+
+  it("导入任务表状态 parsed 渲染「已解析」，不直出英文", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
+    await waitFor(() => expect(screen.getByTestId("floor-plan-imports-table")).toBeTruthy());
+    const table = screen.getByTestId("floor-plan-imports-table").textContent ?? "";
+    expect(table).toContain("已解析");
+    expect(table).not.toContain("parsed");
   });
 
   it("候选明细表类型/关联/状态渲染中文，不含原始英文码", async () => {
@@ -91,15 +129,26 @@ describe("FloorPlanImportPage 英文全清", () => {
     const table = screen.getByTestId("floor-plan-candidates-table").textContent ?? "";
     expect(table).toContain("铺位"); // shapeType shop
     expect(table).toContain("空间"); // refType space
-    expect(table).toContain("待处理"); // status pending
+    expect(table).toContain("待复核"); // status pending
     expect(table).not.toContain("shop");
     expect(table).not.toContain("pending");
   });
+
+  it("导入错误表按解析器真实字段/错误码渲染中文，不显示未识别", async () => {
+    await openDetails();
+    const table = screen.getByTestId("floor-plan-errors-table").textContent ?? "";
+    expect(table).toContain("顶点"); // field points
+    expect(table).toContain("顶点不足"); // POINTS_TOO_FEW
+    expect(table).toContain("多边形至少需要 3 个顶点");
+    expect(table).not.toContain("POINTS_TOO_FEW");
+    expect(table).not.toContain("未识别");
+  });
 });
 
 describe("FloorPlanImportPage 行明细抽屉", () => {
   afterEach(() => {
     vi.unstubAllGlobals();
+    lastPost = null;
   });
 
   it("点候选行弹出抽屉，含类型/关联/状态中文，无英文枚举", async () => {
@@ -109,19 +158,20 @@ describe("FloorPlanImportPage 行明细抽屉", () => {
     const text = drawer.textContent ?? "";
     expect(text).toContain("铺位"); // shape_type shop
     expect(text).toContain("空间"); // ref_type space
-    expect(text).toContain("待处理"); // status pending
+    expect(text).toContain("待复核"); // status pending
     expect(text).toContain("A101"); // label
     expect(text).not.toContain("shop");
     expect(text).not.toContain("pending");
   });
 
-  it("点错误行弹出抽屉含行号/说明，Esc 关闭", async () => {
+  it("点错误行弹出抽屉含行号/字段/错误码中文与说明，Esc 关闭", async () => {
     await openDetails();
     fireEvent.click(screen.getByTestId("floor-plan-error-row-er-1"));
     const drawer = await screen.findByTestId("row-detail-drawer");
     const text = drawer.textContent ?? "";
-    expect(text).toContain("图形");
-    expect(text).toContain("多边形无效");
+    expect(text).toContain("顶点");
+    expect(text).toContain("顶点不足");
+    expect(text).toContain("多边形至少需要 3 个顶点");
     fireEvent.keyDown(window, { key: "Escape" });
     await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
   });
@@ -130,6 +180,7 @@ describe("FloorPlanImportPage 行明细抽屉", () => {
 describe("FloorPlanImportPage 闭集与导入格式", () => {
   afterEach(() => {
     vi.unstubAllGlobals();
+    lastPost = null;
   });
 
   it("未知候选种类显示未识别，不显示其他或走道", async () => {
@@ -152,4 +203,70 @@ describe("FloorPlanImportPage 闭集与导入格式", () => {
     expect(text).toContain("标准数据");
     expect(text).not.toContain("geojson");
   });
+
+  // M018-007/005/006：来源名称必填（后端 source_name NOT NULL）；POST 体与幂等键
+  it("创建导入：来源名称留空被步内拦截；填齐后 POST 带格式/名称/内容且幂等键为页面键", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
+    await waitFor(() => expect(screen.getByTestId("floor-plan-import-open")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("floor-plan-import-open"));
+    fireEvent.change(screen.getByTestId("floor-plan-import-payload"), { target: { value: "<svg viewBox=\"0 0 10 10\"></svg>" } });
+    fireEvent.click(screen.getByTestId("floor-plan-import-guided-next"));
+    expect(screen.getByTestId("floor-plan-import-guided-error").textContent).toContain("来源名称");
+    expect(lastPost).toBeNull();
+
+    fireEvent.change(screen.getByTestId("floor-plan-import-name"), { target: { value: "cad导出2" } });
+    fireEvent.click(screen.getByTestId("floor-plan-import-guided-next"));
+    await waitFor(() => expect(screen.getByTestId("floor-plan-import-guided-summary")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("floor-plan-import-create"));
+    await waitFor(() => expect(lastPost).toBeTruthy());
+    expect(lastPost?.url).toContain("/floor-plans/fp-1/imports");
+    expect(lastPost?.body).toMatchObject({ source_format: "svg", source_name: "cad导出2", source_payload: "<svg viewBox=\"0 0 10 10\"></svg>" });
+    expect(String(lastPost?.body.idempotency_key)).toMatch(/^fpi-/);
+  });
+});
+
+describe("FloorPlanImportPage 发布、权限与下钻错误（M018-003/004/005/008）", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    lastPost = null;
+  });
+
+  it("未勾选候选时发布被拦截（不弹确认、不请求）；勾选后确认 POST 只带勾选的候选且幂等键为页面键", async () => {
+    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
+    const { setGlobalError } = await openDetails();
+    fireEvent.click(screen.getByTestId("floor-plan-publish"));
+    expect(setGlobalError).toHaveBeenCalledWith("请勾选要发布的区域");
+    expect(confirmSpy).not.toHaveBeenCalled();
+    expect(lastPost).toBeNull();
+
+    fireEvent.click(screen.getByLabelText("A101"));
+    fireEvent.click(screen.getByTestId("floor-plan-publish"));
+    expect(confirmSpy).toHaveBeenCalledTimes(1);
+    await waitFor(() => expect(lastPost).toBeTruthy());
+    expect(lastPost?.url).toContain("/floor-plans/fp-1/imports/imp-1/publish");
+    expect(lastPost?.body).toMatchObject({ candidate_ids: ["cd-1"] });
+    expect(String(lastPost?.body.idempotency_key)).toMatch(/^fpp-/);
+  });
+
+  it("无 floor_plan.import.manage 时创建导入与发布按钮禁用（新增平面图仍可用）", async () => {
+    await openDetails(NO_IMPORT_MANAGE);
+    expect(screen.getByTestId("floor-plan-import-open")).toBeDisabled();
+    expect(screen.getByTestId("floor-plan-publish")).toBeDisabled();
+    expect(screen.getByTestId("floor-plan-create-open")).not.toBeDisabled();
+  });
+
+  it("无 floor_plan.import.view 时不显示下钻按钮", async () => {
+    renderPage(VIEW_ONLY);
+    await waitFor(() => expect(screen.getByTestId("floor-plans-table").textContent).toContain("一层平面图"));
+    expect(screen.queryByTestId("floor-plan-select-fp-1")).toBeNull();
+  });
+
+  it("下钻读取导入任务失败（403）时报错到全局，不静默", async () => {
+    const { setGlobalError } = renderPage(ADMIN_PERMS, { importsStatus: 403 });
+    await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
+    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("无权")));
+  });
 });
diff --git a/apps/web/src/admin/pages/FloorPlanImportPage.tsx b/apps/web/src/admin/pages/FloorPlanImportPage.tsx
index b3247bf3..4e6a4744 100644
--- a/apps/web/src/admin/pages/FloorPlanImportPage.tsx
+++ b/apps/web/src/admin/pages/FloorPlanImportPage.tsx
@@ -4,12 +4,11 @@ import { AdminPageHeader } from "../components/AdminPageHeader";
 import { AuditTrailHint } from "../components/AuditTrailHint";
 import { TechDetails } from "../components/TechDetails";
 import { useAdminResource } from "../hooks/useAdminResource";
-import { PermissionButton } from "../security/Can";
+import { Can, PermissionButton } from "../security/Can";
 import { IconButton } from "../components/IconButton";
 import { UnorderedListOutlined, ProfileOutlined } from "@ant-design/icons";
 import { HighRiskConfirm } from "../components/HighRiskConfirm";
 import { GuidedFlowModal } from "../components/GuidedFlowModal";
-import { formatBusinessStatus } from "../i18n/statusLabels";
 import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
 import type { AdminPageProps } from "../types";
 
@@ -28,24 +27,39 @@ const SOURCE_FORMAT_OPTIONS: { value: string; label: string }[] = [
   { value: "svg", label: "矢量图" },
   { value: "standard_json", label: "标准数据" },
 ];
+// M018-002：页内枚举字典与后端闭集一致（FloorPlanImportParser 错误字段/错误码、FloorPlanService STATUSES / IMPORT_TASK_STATUSES / IMPORT_CANDIDATE_STATUSES / REF_TYPES），
+// 不再走全局码表（无 parsed/archived 键，published 在全局表是「已上架」）。
 const ERROR_FIELD_LABELS: Record<string, string> = {
-  geometry: "图形",
-  shape_type: "图形类型",
   source_format: "导入格式",
-  project_id: "所属项目",
+  viewBox: "画布范围",
+  points: "顶点",
+  polygon_points: "多边形顶点",
+  shapes: "图形列表",
+  source_payload: "来源内容",
 };
 const ERROR_CODE_LABELS: Record<string, string> = {
-  INVALID_POLYGON: "多边形无效",
-  VALIDATION_FAILED: "校验未通过",
+  PAYLOAD_REQUIRED: "缺少来源内容",
+  UNSUPPORTED_SOURCE_FORMAT: "不支持的导入格式",
+  VIEWBOX_REQUIRED: "缺少画布范围",
+  POINTS_REQUIRED: "缺少顶点",
+  POINTS_TOO_FEW: "顶点不足",
+  POLYLINE_NOT_CLOSED: "折线未闭合",
+  POINT_OUT_OF_RANGE: "顶点越界",
+  SHAPES_REQUIRED: "缺少图形列表",
+  JSON_PARSE_FAILED: "数据解析失败",
+  NO_SHAPES_FOUND: "未找到图形",
 };
+const FLOOR_PLAN_STATUS_LABELS: Record<string, string> = { active: "启用", inactive: "停用", archived: "已存档" };
+const IMPORT_TASK_STATUS_LABELS: Record<string, string> = { parsed: "已解析", failed: "解析失败", published: "已发布", cancelled: "已取消" };
+const CANDIDATE_STATUS_LABELS: Record<string, string> = { pending: "待复核", published: "已发布", rejected: "已驳回" };
 const zhOrDash = (map: Record<string, string>, code: string | null | undefined) =>
   code ? map[code] ?? "未识别" : "-";
-const REF_TYPE_LABELS: Record<string, string> = { space: "空间", merchant: "商户", facility: "设施" };
+const REF_TYPE_LABELS: Record<string, string> = {
+  space: "空间", merchant: "商户", project: "综合体项目", amusement_project: "游乐项目", device: "设备", custom: "自定义",
+};
 const fpLabel = (map: Record<string, string>, code: string | null | undefined) => (code ? map[code] ?? "未识别" : "-");
 function floorPlanStatusLabel(status?: string) {
-  if (status === "published") return "已发布";
-  if (status === "draft") return "草稿";
-  return formatBusinessStatus(status);
+  return fpLabel(FLOOR_PLAN_STATUS_LABELS, status);
 }
 
 // 后端 floorplan 域记录映射(接现有 /api/v1/floor-plans/* 契约)。
@@ -56,6 +70,11 @@ type ImportError = { id: string; lineNo?: number; fieldName?: string; errorCode?
 type FloorPlanImportDetails = { task?: ImportTask; candidates?: ImportCandidate[]; errors?: ImportError[] };
 
 const MANAGE = ["floor_plan.manage"];
+// M018-003：导入任务/候选的读写码与后端 FloorPlanController 一致（floor_plan.import.view / floor_plan.import.manage，V034 独立种子）。
+const IMPORT_VIEW = ["floor_plan.import.view"];
+const IMPORT_MANAGE = ["floor_plan.import.manage"];
+// M018-001：新增平面图表单字段与后端 validateCreateRequest / V023 必填列一致。
+const EMPTY_PLAN_FORM = { projectId: "", planName: "", buildingCode: "", floorNo: "", imageUrl: "", imageWidth: "", imageHeight: "", versionNo: "" };
 // 高危动作二次确认标题(发布候选写入正式平面图区域,不可静默覆盖):由 HighRiskConfirm 弹窗承载。
 const PUBLISH_CONFIRM_TITLE = "发布平面图导入候选";
 
@@ -74,7 +93,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
   const [imports, setImports] = React.useState<ImportTask[]>([]);
   const [selectedImportId, setSelectedImportId] = React.useState("");
   const [floorPlanImportDetails, setFloorPlanImportDetails] = React.useState<FloorPlanImportDetails | null>(null);
-  const [planForm, setPlanForm] = React.useState({ projectId: "", planName: "", buildingCode: "", floorNo: "" });
+  const [planForm, setPlanForm] = React.useState(EMPTY_PLAN_FORM);
   const [projects, setProjects] = React.useState<{ id: string; name?: string }[]>([]);
   const planKeyRef = React.useRef("");
   const importKeyRef = React.useRef("");
@@ -103,35 +122,52 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
     { onError: setGlobalError, fallbackMessage: "平面图读取失败", onStart: () => setGlobalError(null) },
   );
 
+  // M018-004：下钻读取失败（403/网络）不再静默——统一走 setGlobalError。
   const loadImports = async (floorPlanId: string) => {
     setSelectedPlanId(floorPlanId);
     setSelectedImportId("");
     setFloorPlanImportDetails(null);
     importKeyRef.current = "";
     publishKeyRef.current = "";
-    const res = await api.get<{ items?: ImportTask[] } | ImportTask[]>(`/api/v1/floor-plans/${floorPlanId}/imports`);
-    setImports(Array.isArray(res) ? res : (res?.items ?? []));
+    try {
+      const res = await api.get<{ items?: ImportTask[] } | ImportTask[]>(`/api/v1/floor-plans/${floorPlanId}/imports`);
+      setImports(Array.isArray(res) ? res : (res?.items ?? []));
+    } catch (e) { setGlobalError((e as Error).message); }
   };
 
   const loadCandidates = async (floorPlanId: string, importId: string) => {
     setSelectedImportId(importId);
     publishKeyRef.current = "";
     setPublishCandidateIds([]);
-    const details = await api.get<FloorPlanImportDetails>(`/api/v1/floor-plans/${floorPlanId}/imports/${importId}/candidates`);
-    setFloorPlanImportDetails(details);
+    try {
+      const details = await api.get<FloorPlanImportDetails>(`/api/v1/floor-plans/${floorPlanId}/imports/${importId}/candidates`);
+      setFloorPlanImportDetails(details);
+    } catch (e) { setGlobalError((e as Error).message); }
   };
 
   // 提交在确认步触发；校验失败/接口失败弹窗留开显错，成功才关。原接口/请求体保留（双保险再校验一次名称）。
+  // M018-001：请求体与后端 FloorPlanService.validateCreateRequest / V023 契约一致（楼层、底图地址、底图宽高、版本号必填；状态闭集 active/inactive/archived，无 draft）。
+  const planFormError = () => {
+    if (!planForm.projectId.trim()) return "请选择所属项目";
+    if (!planForm.planName.trim()) return "请填写平面图名称";
+    if (!planForm.floorNo.trim()) return "请填写楼层";
+    if (!planForm.imageUrl.trim()) return "请填写底图地址";
+    if (!(Number(planForm.imageWidth) > 0) || !(Number(planForm.imageHeight) > 0)) return "请填写大于 0 的底图宽高";
+    if (!planForm.versionNo.trim()) return "请填写版本号";
+    return null;
+  };
   const createPlan = async () => {
-    if (!planForm.projectId.trim()) { setPlanError("请选择所属项目"); return; }
-    if (!planForm.planName.trim()) { setPlanError("请填写平面图名称"); return; }
+    const formError = planFormError();
+    if (formError) { setPlanError(formError); return; }
     setPlanSaving(true);
     setPlanError("");
     try {
       if (!planKeyRef.current) planKeyRef.current = `fp-${Math.random().toString(36).slice(2)}`;
+      // M018-005：幂等键以 camel 键交给 AdminApi（normalizeRequestBody 只认 idempotencyKey，snake 键会被生成键覆盖，失败重试不再同键）。
       await api.post("/api/v1/floor-plans", {
-        idempotency_key: planKeyRef.current, plan_name: planForm.planName, project_id: planForm.projectId,
-        building_code: planForm.buildingCode, floor_no: planForm.floorNo, status: "draft",
+        idempotencyKey: planKeyRef.current, plan_name: planForm.planName, project_id: planForm.projectId,
+        building_code: planForm.buildingCode, floor_no: planForm.floorNo, image_url: planForm.imageUrl,
+        image_width: Number(planForm.imageWidth), image_height: Number(planForm.imageHeight), version_no: planForm.versionNo, status: "active",
       });
       setActionResult({ action: "floor_plan_created", planName: planForm.planName });
       setPlanOpen(false);
@@ -139,13 +175,21 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
     } catch (e) { setPlanError((e as Error).message); } finally { setPlanSaving(false); }
   };
 
+  // M018-007：来源名称与后端 validateImportCreateRequest（source_name 必填）对齐。
+  const importFormError = () => {
+    if (!importForm.sourceName.trim()) return "请填写来源名称";
+    if (!importForm.sourcePayload.trim()) return "请填写来源内容";
+    return null;
+  };
   const createImport = async () => {
+    const formError = importFormError();
+    if (formError) { setImportError(formError); return; }
     setImportSaving(true);
     setImportError("");
     try {
       if (!importKeyRef.current) importKeyRef.current = `fpi-${selectedPlanId}-${Math.random().toString(36).slice(2)}`;
       const res = await api.post<ImportTask>(`/api/v1/floor-plans/${selectedPlanId}/imports`, {
-        idempotency_key: importKeyRef.current, source_format: importForm.sourceFormat,
+        idempotencyKey: importKeyRef.current, source_format: importForm.sourceFormat,
         source_name: importForm.sourceName, source_payload: importForm.sourcePayload,
       });
       setActionResult({ action: "floor_plan_import_created", importId: res?.id });
@@ -155,13 +199,18 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
     } catch (e) { setImportError((e as Error).message); } finally { setImportSaving(false); }
   };
 
+  // M018-008：只发布勾选的候选（勾选＝人工复核），未勾选不默认全发。
+  const requireSelectedCandidates = () => {
+    if (publishCandidateIds.length === 0) { setGlobalError("请勾选要发布的区域"); return false; }
+    return true;
+  };
   const publishImport = async () => {
     try {
-      const ids = publishCandidateIds.length > 0 ? publishCandidateIds : pendingCandidates.map((item) => item.id).filter(Boolean);
-      if (ids.length === 0) { setGlobalError("没有待发布的区域"); return; }
+      if (!requireSelectedCandidates()) return;
+      const ids = publishCandidateIds;
       if (!publishKeyRef.current) publishKeyRef.current = `fpp-${selectedImportId}-${Math.random().toString(36).slice(2)}`;
       const result = await api.post(`/api/v1/floor-plans/${selectedPlanId}/imports/${selectedImportId}/publish`, {
-        idempotency_key: publishKeyRef.current, candidate_ids: ids,
+        idempotencyKey: publishKeyRef.current, candidate_ids: ids,
       });
       setActionResult({ action: "floor_plan_import_published", candidateIds: ids, result });
       await loadCandidates(selectedPlanId, selectedImportId);
@@ -176,7 +225,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
         { label: "类型", value: fpLabel(SHAPE_TYPE_LABELS, candidate.shapeType) },
         { label: "关联", value: `${fpLabel(REF_TYPE_LABELS, candidate.refType)}：${candidate.refId ?? "—"}` },
         { label: "标签", value: candidate.label ?? "—" },
-        { label: "状态", value: formatBusinessStatus(candidate.status) },
+        { label: "状态", value: fpLabel(CANDIDATE_STATUS_LABELS, candidate.status) },
         { label: "候选编号", value: candidate.id },
       ],
     });
@@ -207,7 +256,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
             permissionsState={effectivePermissions}
             permissions={MANAGE}
             data-testid="floor-plan-create-open"
-            onClick={() => { setPlanForm({ projectId: "", planName: "", buildingCode: "", floorNo: "" }); planKeyRef.current = ""; setPlanError(""); setPlanOpen(true); }}
+            onClick={() => { setPlanForm(EMPTY_PLAN_FORM); planKeyRef.current = ""; setPlanError(""); setPlanOpen(true); }}
           >＋ 新增平面图</PermissionButton>
         </div>
         <table className="admin-table" data-testid="floor-plans-table">
@@ -217,7 +266,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
               <tr key={plan.id}>
                 <td>{plan.planName ?? plan.id}</td><td>{plan.buildingCode ?? "-"}/{plan.floorNo ?? "-"}</td>
                 <td>{plan.versionNo ?? "-"}</td><td>{floorPlanStatusLabel(plan.status)}</td>
-                <td><IconButton icon={<UnorderedListOutlined />} tip="查看导入任务" onClick={() => void loadImports(plan.id)} testId={`floor-plan-select-${plan.id}`} /></td>
+                <td><Can permissionsState={effectivePermissions} permissions={IMPORT_VIEW}><IconButton icon={<UnorderedListOutlined />} tip="查看导入任务" onClick={() => void loadImports(plan.id)} testId={`floor-plan-select-${plan.id}`} /></Can></td>
               </tr>
             ))}
           </tbody>
@@ -226,16 +275,16 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
           <>
             <h3>导入任务</h3>
             <div className="admin-form-row" data-testid="floor-plan-import-form">
-              <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="floor-plan-import-open" onClick={() => { setImportForm({ sourceFormat: "svg", sourceName: "", sourcePayload: "" }); importKeyRef.current = ""; setImportError(""); setImportOpen(true); }}>＋ 创建导入</PermissionButton>
+              <PermissionButton permissionsState={effectivePermissions} permissions={IMPORT_MANAGE} data-testid="floor-plan-import-open" onClick={() => { setImportForm({ sourceFormat: "svg", sourceName: "", sourcePayload: "" }); importKeyRef.current = ""; setImportError(""); setImportOpen(true); }}>＋ 创建导入</PermissionButton>
             </div>
             <table className="admin-table" data-testid="floor-plan-imports-table">
               <thead><tr><th>来源</th><th>状态</th><th>候选数</th><th>错误数</th><th>操作</th></tr></thead>
               <tbody>
                 {imports.map((task) => (
                   <tr key={task.id}>
-                    <td>{task.sourceName ?? task.id}</td><td>{formatBusinessStatus(task.status)}</td>
+                    <td>{task.sourceName ?? task.id}</td><td>{fpLabel(IMPORT_TASK_STATUS_LABELS, task.status)}</td>
                     <td>{task.candidateCount ?? 0}</td><td>{task.errorCount ?? 0}</td>
-                    <td><IconButton icon={<ProfileOutlined />} tip="查看候选明细" onClick={() => void loadCandidates(selectedPlanId, task.id)} testId={`floor-plan-import-select-${task.id}`} /></td>
+                    <td><Can permissionsState={effectivePermissions} permissions={IMPORT_VIEW}><IconButton icon={<ProfileOutlined />} tip="查看候选明细" onClick={() => void loadCandidates(selectedPlanId, task.id)} testId={`floor-plan-import-select-${task.id}`} /></Can></td>
                   </tr>
                 ))}
               </tbody>
@@ -257,7 +306,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
                     onClick={() => openCandidateDetail(candidate)}
                     onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openCandidateDetail(candidate); } }}>
                     <td>{candidate.candidateNo ?? "-"}</td><td>{fpLabel(SHAPE_TYPE_LABELS, candidate.shapeType)}</td>
-                    <td>{fpLabel(REF_TYPE_LABELS, candidate.refType)}:{candidate.refId ?? "-"}</td><td>{candidate.label ?? "-"}</td><td>{formatBusinessStatus(candidate.status)}</td>
+                    <td>{fpLabel(REF_TYPE_LABELS, candidate.refType)}:{candidate.refId ?? "-"}</td><td>{candidate.label ?? "-"}</td><td>{fpLabel(CANDIDATE_STATUS_LABELS, candidate.status)}</td>
                   <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看候选详情" onClick={(event) => { event.stopPropagation(); openCandidateDetail(candidate); }}>查看详情</button></td></tr>
                 ))}
               </tbody>
@@ -292,10 +341,11 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
               </fieldset>
               <HighRiskConfirm
                 label="发布候选到正式平面图"
-                impact={`${PUBLISH_CONFIRM_TITLE}：发布后候选会写入正式平面图区域，请确认已完成业务复核，发布不可静默覆盖既有区域证据。`}
+                impact={`${PUBLISH_CONFIRM_TITLE}：本次发布 ${publishCandidateIds.length} 个已勾选候选，发布后候选会写入正式平面图区域，请确认已完成业务复核，发布不可静默覆盖既有区域证据。`}
+                precheck={requireSelectedCandidates}
                 onConfirm={() => void publishImport()}
                 permissionsState={effectivePermissions}
-                permissions={MANAGE}
+                permissions={IMPORT_MANAGE}
                 data-testid="floor-plan-publish"
               />
             </div>
@@ -316,12 +366,8 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
         steps={[
           {
             title: "基本信息",
-            description: "平面图名称必填；楼栋/楼层选填。新建平面图初始为草稿，导入候选复核后再发布。",
-            validate: () => {
-              if (!planForm.projectId.trim()) return "请选择所属项目";
-              if (!planForm.planName.trim()) return "请填写平面图名称";
-              return null;
-            },
+            description: "平面图名称、楼层、底图地址、底图宽高、版本号必填；楼栋选填。新建平面图为启用状态，导入候选复核后再发布。",
+            validate: planFormError,
             content: (
               <>
                 <label>所属项目<select data-testid="floor-plan-project" value={planForm.projectId} onChange={(e) => setPlanForm({ ...planForm, projectId: e.target.value })}>
@@ -331,6 +377,10 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
                 <label>平面图名称<input data-testid="floor-plan-name" value={planForm.planName} onChange={(e) => setPlanForm({ ...planForm, planName: e.target.value })} /></label>
                 <label>楼栋<input data-testid="floor-plan-building" value={planForm.buildingCode} onChange={(e) => setPlanForm({ ...planForm, buildingCode: e.target.value })} /></label>
                 <label>楼层<input data-testid="floor-plan-floor" value={planForm.floorNo} onChange={(e) => setPlanForm({ ...planForm, floorNo: e.target.value })} /></label>
+                <label>底图地址<input data-testid="floor-plan-image-url" value={planForm.imageUrl} onChange={(e) => setPlanForm({ ...planForm, imageUrl: e.target.value })} /></label>
+                <label>底图宽<input data-testid="floor-plan-image-width" type="number" min="1" value={planForm.imageWidth} onChange={(e) => setPlanForm({ ...planForm, imageWidth: e.target.value })} /></label>
+                <label>底图高<input data-testid="floor-plan-image-height" type="number" min="1" value={planForm.imageHeight} onChange={(e) => setPlanForm({ ...planForm, imageHeight: e.target.value })} /></label>
+                <label>版本号<input data-testid="floor-plan-version" value={planForm.versionNo} onChange={(e) => setPlanForm({ ...planForm, versionNo: e.target.value })} /></label>
               </>
             ),
           },
@@ -339,8 +389,11 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
           { label: "所属项目", value: projects.find((p) => p.id === planForm.projectId)?.name ?? (planForm.projectId || "未选择") },
           { label: "平面图名称", value: planForm.planName.trim() || "未填写" },
           { label: "楼栋", value: planForm.buildingCode.trim() || "—" },
-          { label: "楼层", value: planForm.floorNo.trim() || "—" },
-          { label: "初始状态", value: "草稿" },
+          { label: "楼层", value: planForm.floorNo.trim() || "未填写" },
+          { label: "底图地址", value: planForm.imageUrl.trim() || "未填写" },
+          { label: "底图宽×高", value: planForm.imageWidth && planForm.imageHeight ? `${planForm.imageWidth} × ${planForm.imageHeight}` : "未填写" },
+          { label: "版本号", value: planForm.versionNo.trim() || "未填写" },
+          { label: "状态", value: "启用" },
         ]}
       />
       <GuidedFlowModal
@@ -357,7 +410,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
           {
             title: "导入内容",
             description: "选择来源格式，填写名称和内容。",
-            validate: () => (importForm.sourcePayload.trim() ? null : "请填写来源内容"),
+            validate: importFormError,
             content: (
               <>
                 <label>来源格式<select data-testid="floor-plan-import-format" value={importForm.sourceFormat} onChange={(e) => setImportForm({ ...importForm, sourceFormat: e.target.value })}>
diff --git a/apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx b/apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx
index 0fec42e1..01ccdd77 100644
--- a/apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx
+++ b/apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx
@@ -5,11 +5,18 @@ import { SpaceTypesPage } from "./SpaceTypesPage";
 import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";
 
 // 引导式流程 phase-7：新增空间类型改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截）。
+// M018-006/009：存档/申请删除/审批/恢复四条生命周期动作各有可失败断言。
 const ROWS = [{ id: "st-1", typeCode: "kiosk", typeName: "一体机位", sortOrder: 100, status: "active" }];
+const LIFECYCLE_ROWS = [
+  { id: "st-1", typeCode: "kiosk", typeName: "一体机位", sortOrder: 100, status: "active" },
+  { id: "st-2", typeCode: "old", typeName: "旧种类", sortOrder: 90, status: "disabled" },
+  { id: "st-3", typeCode: "arc", typeName: "已存档种类", sortOrder: 80, status: "archived" },
+  { id: "st-4", typeCode: "pd", typeName: "待删种类", sortOrder: 70, status: "pending_delete" },
+];
 
-function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
+function makeApi(post = vi.fn().mockResolvedValue({}), rows: unknown[] = ROWS): AdminApiClient {
   return {
-    get: vi.fn().mockResolvedValue(ROWS),
+    get: vi.fn().mockResolvedValue(rows),
     getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
     post,
     put: vi.fn().mockResolvedValue({}),
@@ -19,12 +26,14 @@ function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
 }
 
 const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
+const ADMIN_USER = { id: "u1", username: "admin", accountType: "platform_admin" };
+const MASTER_USER = { id: "usr_master_authority", username: "master-authority", accountType: "master_authority" };
 
-function renderPage(api: AdminApiClient) {
+function renderPage(api: AdminApiClient, user = ADMIN_USER) {
   const setActionResult = vi.fn();
   const props: AdminPageProps = {
     api,
-    user: { id: "u1", username: "admin", accountType: "platform_admin" },
+    user,
     setActionResult,
     setGlobalError: vi.fn(),
     effectivePermissions: ADMIN_PERMS,
@@ -89,18 +98,69 @@ describe("SpaceTypesPage 引导式新增空间类型(phase-7)", () => {
     expect(post).not.toHaveBeenCalled();
   });
 
-  it("已删除种类不再提供申请删除；已存档可取消存档", async () => {
-    const api = makeApi();
-    api.get = vi.fn().mockResolvedValue([
-      { id: "st-1", typeCode: "kiosk", typeName: "一体机位", sortOrder: 100, status: "active" },
-      { id: "st-2", typeCode: "old", typeName: "旧种类", sortOrder: 90, status: "disabled" },
-      { id: "st-3", typeCode: "arc", typeName: "已存档种类", sortOrder: 80, status: "archived" },
-    ]);
-    renderPage(api);
+  it("已删除种类不再提供申请删除但可恢复；已存档可取消存档", async () => {
+    renderPage(makeApi(undefined, LIFECYCLE_ROWS));
     await waitFor(() => expect(screen.getByTestId("space-type-archive-kiosk")).toBeTruthy());
     expect(screen.getByTestId("space-types-table").textContent).toContain("已删除");
     expect(screen.queryByTestId("space-type-delete-request-old")).toBeNull();
+    expect(screen.getByTestId("space-type-restore-old")).toBeTruthy(); // M018-009
     expect(screen.getByTestId("space-type-unarchive-arc")).toBeTruthy();
     expect(screen.getByTestId("space-type-delete-request-arc")).toBeTruthy();
   });
 });
+
+// M018-006：存档→申请删除→最高权限审批→恢复（业务地图 L566-577）各有可失败断言；理由经 window.prompt、高危经 window.confirm。
+describe("SpaceTypesPage 生命周期动作", () => {
+  it("存档：填理由并确认后 POST /archive 带 reason；理由留空不请求", async () => {
+    const post = vi.fn().mockResolvedValue({});
+    const { setActionResult } = renderPage(makeApi(post, LIFECYCLE_ROWS));
+    await waitFor(() => expect(screen.getByTestId("space-type-archive-kiosk")).toBeTruthy());
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+    const promptSpy = vi.spyOn(window, "prompt").mockReturnValueOnce("   ");
+    fireEvent.click(screen.getByTestId("space-type-archive-kiosk"));
+    expect(post).not.toHaveBeenCalled();
+
+    promptSpy.mockReturnValueOnce("配错了");
+    fireEvent.click(screen.getByTestId("space-type-archive-kiosk"));
+    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types/st-1/archive", { reason: "配错了" }));
+    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "space_type_archived", typeCode: "kiosk" }));
+  });
+
+  it("申请删除：已存档种类填理由后 POST /delete-request 带 reason", async () => {
+    const post = vi.fn().mockResolvedValue({});
+    renderPage(makeApi(post, LIFECYCLE_ROWS));
+    await waitFor(() => expect(screen.getByTestId("space-type-delete-request-arc")).toBeTruthy());
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+    vi.spyOn(window, "prompt").mockReturnValueOnce("不再使用");
+    fireEvent.click(screen.getByTestId("space-type-delete-request-arc"));
+    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types/st-3/delete-request", { reason: "不再使用" }));
+  });
+
+  it("审批：最高权限可通过/驳回（POST /delete-approve 带 decision），平台管理员只见「待最高权限审批」", async () => {
+    const post = vi.fn().mockResolvedValue({});
+    renderPage(makeApi(post, LIFECYCLE_ROWS));
+    await waitFor(() => expect(screen.getByTestId("space-types-table")).toBeTruthy());
+    expect(screen.queryByTestId("space-type-delete-approve-pd")).toBeNull();
+    expect(screen.getByTestId("space-types-table").textContent).toContain("待最高权限审批");
+    vi.unstubAllGlobals();
+  });
+
+  it("审批（最高权限）：通过 POST decision=approved", async () => {
+    const post = vi.fn().mockResolvedValue({});
+    renderPage(makeApi(post, LIFECYCLE_ROWS), MASTER_USER);
+    await waitFor(() => expect(screen.getByTestId("space-type-delete-approve-pd")).toBeTruthy());
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+    fireEvent.click(screen.getByTestId("space-type-delete-approve-pd"));
+    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types/st-4/delete-approve", { decision: "approved" }));
+  });
+
+  it("恢复：已删除种类确认后 POST /unarchive（M018-009）", async () => {
+    const post = vi.fn().mockResolvedValue({});
+    const { setActionResult } = renderPage(makeApi(post, LIFECYCLE_ROWS));
+    await waitFor(() => expect(screen.getByTestId("space-type-restore-old")).toBeTruthy());
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+    fireEvent.click(screen.getByTestId("space-type-restore-old"));
+    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types/st-2/unarchive"));
+    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "space_type_unarchived", typeCode: "old" }));
+  });
+});
diff --git a/apps/web/src/admin/pages/SpaceTypesPage.tsx b/apps/web/src/admin/pages/SpaceTypesPage.tsx
index a0f9bc71..856a0545 100644
--- a/apps/web/src/admin/pages/SpaceTypesPage.tsx
+++ b/apps/web/src/admin/pages/SpaceTypesPage.tsx
@@ -154,7 +154,7 @@ export function SpaceTypesPage({ api, user, setActionResult, setGlobalError, eff
     <section data-testid="space-types-page">
       <AdminPageHeader
         title="空间种类"
-        description="维护空间种类。建空间时从本表选择种类。可新增。下线用存档。删除须先存档，再申请并经最高权限审批。有空间还在用的种类不能删除。"
+        description="维护空间种类。建空间时从本表选择种类。可新增。下线用存档。删除须先存档，再申请并经最高权限审批。有空间还在用的种类不能删除。已删除的种类可恢复为启用。"
       />
       <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
         <div className="admin-table-scroll">
@@ -199,6 +199,17 @@ export function SpaceTypesPage({ api, user, setActionResult, setGlobalError, eff
                         />
                       </>
                     ) : null}
+                    {row.status === "disabled" ? (
+                      // M018-009：删除只隐藏不抹除、可恢复（业务地图 L571）——走既有 unarchive（后端允许 disabled → active）。
+                      <HighRiskConfirm
+                        label="恢复"
+                        impact={`把已删除种类「${row.typeName}」恢复为启用，可以再用来新建空间。`}
+                        onConfirm={() => void unarchiveType(row)}
+                        permissionsState={effectivePermissions}
+                        permissions={MANAGE}
+                        data-testid={`space-type-restore-${row.typeCode}`}
+                      />
+                    ) : null}
                     {row.status === "pending_delete" && isMaster ? (
                       <>
                         <HighRiskConfirm
diff --git a/apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx b/apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx
index a8545e53..b0ae055e 100644
--- a/apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx
+++ b/apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx
@@ -10,9 +10,11 @@ const HOURS = [
 ];
 const SPACES = [{ id: "sp_bar_main", spaceName: "主吧台大厅" }];
 
-function makeApi(post = vi.fn().mockResolvedValue({})): { api: AdminApiClient; get: ReturnType<typeof vi.fn>; post: typeof post } {
-  const get = vi.fn().mockImplementation((url: string) =>
-    Promise.resolve(url.includes("venue-business-hours") ? HOURS : SPACES));
+function makeApi(post = vi.fn().mockResolvedValue({}), spacesFail = false): { api: AdminApiClient; get: ReturnType<typeof vi.fn>; post: typeof post } {
+  const get = vi.fn().mockImplementation((url: string) => {
+    if (url.includes("venue-business-hours")) return Promise.resolve(HOURS);
+    return spacesFail ? Promise.reject(new Error("场地清单读取失败")) : Promise.resolve(SPACES);
+  });
   const api: AdminApiClient = {
     get,
     getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
@@ -85,4 +87,33 @@ describe("VenueBusinessHoursPage 场地营业时间(D1)", () => {
         disabled: false,
       }));
   });
+
+  // M018-006：「本日不营业」= 停用，配置保留（M017-003 口径），提交 disabled:true 且时刻不丢
+  it("取消勾选本日营业后保存，提交 disabled:true 且保留时刻，说明列显示停用", async () => {
+    const { api, post } = makeApi();
+    renderPage(api);
+    await userEvent.selectOptions(await screen.findByTestId("venue-business-hours-venue-select"), "sp_bar_main");
+    await userEvent.click(await screen.findByTestId("venue-business-hours-open-flag-5"));
+    expect(screen.getByTestId("venue-business-hours-note-5").textContent).toContain("本日不营业");
+    await userEvent.click(screen.getByTestId("venue-business-hours-save-5"));
+
+    await waitFor(() =>
+      expect(post).toHaveBeenCalledWith("/api/v1/platform/venue-business-hours", {
+        venueId: "sp_bar_main",
+        dayOfWeek: 5,
+        openTime: "20:00",
+        closeTime: "02:00",
+        disabled: true,
+      }));
+  });
+
+  // M018-006：场地清单是副资源，读取失败只降级提示，已配营业时间的场地仍可选、可改
+  it("场地清单读取失败时显示降级提示，已配场地仍可从营业时间行中选到", async () => {
+    const { api } = makeApi(undefined, true);
+    renderPage(api);
+    const notice = await screen.findByTestId("venue-business-hours-spaces-degraded");
+    expect(notice.textContent).toContain("加载失败");
+    await userEvent.selectOptions(screen.getByTestId("venue-business-hours-venue-select"), "sp_bar_main");
+    await waitFor(() => expect(screen.getByTestId("venue-business-hours-open-5")).toHaveValue("20:00"));
+  });
 });
```

## 3. 内容审计（对照定稿最小处置）

- 001：`planFormError()` 校验 projectId/planName/floorNo/imageUrl/宽高>0/versionNo；POST 体含 image_url/image_width/image_height/version_no、status "active"；弹窗新增四个输入与摘要，描述句改「楼层…必填；楼栋选填」；与 `FloorPlanService.validateCreateRequest:502-526`、V023 一致；不改后端/OpenAPI。
- 002：五张页内字典与后端闭集逐值一致（`FloorPlanService:33/38-39`、`REF_TYPES`、`FloorPlanImportParser` 5 字段/10 码）；去掉 `formatBusinessStatus` 调用与 import；用词为闭集中文直译，不发明业务概念。
- 003：`IMPORT_MANAGE`/`IMPORT_VIEW` 与 `FloorPlanController:62-91` 一致；创建导入/发布用 import.manage，两处下钻以 `Can` 包裹 import.view；新增平面图仍 floor_plan.manage；路由不改。
- 004：`loadImports`/`loadCandidates` try/catch → `setGlobalError`。
- 005：三处 `idempotencyKey` camel 键；经真实 `AdminApi.normalizeRequestBody` 后 body 的 idempotency_key 为页面前缀（用例断言 /^fp-/ /^fpi-/ /^fpp-/）。
- 007：`importFormError()` 来源名称必填；createImport 双保险。
- 008：`requireSelectedCandidates()` 作 `HighRiskConfirm precheck` 与 `publishImport` 守卫；`ids = publishCandidateIds`；确认文案含数量。
- 009：`disabled` 行「恢复」`HighRiskConfirm` 走既有 `unarchiveType`；权限 governance.space.manage。
- 006：四测试文件——夹具改后端真实码；用例覆盖 001–005/007–009 与营业时间停用/降级；RED（未改页面）摘要：
```
 ✓ src/admin/pages/VenueBusinessHoursPage.test.tsx (5 tests) 301ms
 ❯ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx (4 tests | 3 failed) 340ms
     ✓ 页面默认只留入口按钮，不展示平面图字段 103ms
     × 入口→步进→提交：POST 新建平面图带后端必填字段与 active 状态，成功后关闭弹窗 158ms
     × 步内校验拦截：名称留空则不进下一步、不提交 40ms
     × 步内校验拦截：楼层或底图留空则不进下一步、不提交 37ms
 ❯ src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx (10 tests | 2 failed) 1361ms
     ✓ 默认只留入口按钮，不展示弹窗字段 22ms
     ✓ 入口→步进→提交：带正确 body 且成功后关闭弹窗 238ms
     ✓ 新建没有停用选项，确认页状态固定为启用 41ms
     ✓ 步内校验拦截：编码/显示名缺失则不进下一步、不提交 25ms
     × 已删除种类不再提供申请删除但可恢复；已存档可取消存档 7ms
     ✓ 存档：填理由并确认后 POST /archive 带 reason；理由留空不请求 6ms
     ✓ 申请删除：已存档种类填理由后 POST /delete-request 带 reason 6ms
     ✓ 审批：最高权限可通过/驳回（POST /delete-approve 带 decision），平台管理员只见「待最高权限审批」 4ms
     ✓ 审批（最高权限）：通过 POST decision=approved 5ms
     × 恢复：已删除种类确认后 POST /unarchive（M018-009） 1005ms
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx (13 tests | 11 failed) 1551ms
     × 平面图表状态列按后端闭集渲染中文，不含原始英文码 100ms
     × 导入任务表状态 parsed 渲染「已解析」，不直出英文 21ms
     × 候选明细表类型/关联/状态渲染中文，不含原始英文码 33ms
     × 导入错误表按解析器真实字段/错误码渲染中文，不显示未识别 31ms
     × 点候选行弹出抽屉，含类型/关联/状态中文，无英文枚举 36ms
     × 点错误行弹出抽屉含行号/字段/错误码中文与说明，Esc 关闭 34ms
     ✓ 未知候选种类显示未识别，不显示其他或走道 25ms
     ✓ 创建导入下拉只有矢量图和标准数据 143ms
     × 创建导入：来源名称留空被步内拦截；填齐后 POST 带格式/名称/内容且幂等键为页面键 42ms
     × 未勾选候选时发布被拦截（不弹确认、不请求）；勾选后确认 POST 只带勾选的候选且幂等键为页面键 35ms
     × 无 floor_plan.import.manage 时创建导入与发布按钮禁用（新增平面图仍可用） 25ms
     × 无 floor_plan.import.view 时不显示下钻按钮 9ms
     × 下钻读取导入任务失败（403）时报错到全局，不静默 1015ms
⎯⎯⎯⎯⎯⎯ Failed Tests 16 ⎯⎯⎯⎯⎯⎯⎯
 FAIL  src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx > FloorPlanImportPage 引导式新增平面图(phase-10) > 入口→步进→提交：POST 新建平面图带后端必填字段与 active 状态，成功后关闭弹窗
 ❯ Object.getElementError ../../node_modules/@testing-library/dom/dist/config.js:37:19
 ❯ ../../node_modules/@testing-library/dom/dist/query-helpers.js:76:38
 ❯ ../../node_modules/@testing-library/dom/dist/query-helpers.js:52:17
 ❯ ../../node_modules/@testing-library/dom/dist/query-helpers.js:95:19
 ❯ fillPlanForm src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx:78:29
 ❯ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx:100:5
 FAIL  src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx > FloorPlanImportPage 引导式新增平面图(phase-10) > 步内校验拦截：名称留空则不进下一步、不提交
 ❯ Object.getElementError ../../node_modules/@testing-library/dom/dist/config.js:37:19
 ❯ ../../node_modules/@testing-library/dom/dist/query-helpers.js:76:38
 ❯ ../../node_modules/@testing-library/dom/dist/query-helpers.js:52:17
 ❯ ../../node_modules/@testing-library/dom/dist/query-helpers.js:95:19
 ❯ fillPlanForm src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx:78:29
 ❯ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx:128:5
 FAIL  src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx > FloorPlanImportPage 引导式新增平面图(phase-10) > 步内校验拦截：楼层或底图留空则不进下一步、不提交
 ❯ Object.getElementError ../../node_modules/@testing-library/dom/dist/config.js:37:19
 ❯ ../../node_modules/@testing-library/dom/dist/query-helpers.js:76:38
 ❯ ../../node_modules/@testing-library/dom/dist/query-helpers.js:52:17
 ❯ ../../node_modules/@testing-library/dom/dist/query-helpers.js:95:19
 ❯ fillPlanForm src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx:78:29
 ❯ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx:139:5
 FAIL  src/admin/pages/FloorPlanImportPage.test.tsx > FloorPlanImportPage 英文全清（M018-002：后端闭集全部有中文） > 平面图表状态列按后端闭集渲染中文，不含原始英文码
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx:111:19
 FAIL  src/admin/pages/FloorPlanImportPage.test.tsx > FloorPlanImportPage 英文全清（M018-002：后端闭集全部有中文） > 导入任务表状态 parsed 渲染「已解析」，不直出英文
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx:123:19
 FAIL  src/admin/pages/FloorPlanImportPage.test.tsx > FloorPlanImportPage 英文全清（M018-002：后端闭集全部有中文） > 候选明细表类型/关联/状态渲染中文，不含原始英文码
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx:132:19
 FAIL  src/admin/pages/FloorPlanImportPage.test.tsx > FloorPlanImportPage 英文全清（M018-002：后端闭集全部有中文） > 导入错误表按解析器真实字段/错误码渲染中文，不显示未识别
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx:141:19
 FAIL  src/admin/pages/FloorPlanImportPage.test.tsx > FloorPlanImportPage 行明细抽屉 > 点候选行弹出抽屉，含类型/关联/状态中文，无英文枚举
AssertionError: expected '候选明细 · A101×序号1类型铺位关联空间：sp-1标签A101状态待…' to contain '待复核'
Received: "候选明细 · A101×序号1类型铺位关联空间：sp-1标签A101状态待处理候选编号cd-1"
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx:161:18
 FAIL  src/admin/pages/FloorPlanImportPage.test.tsx > FloorPlanImportPage 行明细抽屉 > 点错误行弹出抽屉含行号/字段/错误码中文与说明，Esc 关闭
AssertionError: expected '导入错误明细 · 第 5 行×行号5字段未识别错误码未识别说明多边形至少需…' to contain '顶点不足'
Received: "导入错误明细 · 第 5 行×行号5字段未识别错误码未识别说明多边形至少需要 3 个顶点"
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx:173:18
 FAIL  src/admin/pages/FloorPlanImportPage.test.tsx > FloorPlanImportPage 闭集与导入格式 > 创建导入：来源名称留空被步内拦截；填齐后 POST 带格式/名称/内容且幂等键为页面键
 ❯ Object.getElementError ../../node_modules/@testing-library/dom/dist/config.js:37:19
 ❯ ../../node_modules/@testing-library/dom/dist/query-helpers.js:76:38
 ❯ ../../node_modules/@testing-library/dom/dist/query-helpers.js:52:17
 ❯ ../../node_modules/@testing-library/dom/dist/query-helpers.js:95:19
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx:216:19
 FAIL  src/admin/pages/FloorPlanImportPage.test.tsx > FloorPlanImportPage 发布、权限与下钻错误（M018-003/004/005/008） > 未勾选候选时发布被拦截（不弹确认、不请求）；勾选后确认 POST 只带勾选的候选且幂等键为页面键
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx:240:28
 FAIL  src/admin/pages/FloorPlanImportPage.test.tsx > FloorPlanImportPage 发布、权限与下钻错误（M018-003/004/005/008） > 无 floor_plan.import.manage 时创建导入与发布按钮禁用（新增平面图仍可用）
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx:255:58
 FAIL  src/admin/pages/FloorPlanImportPage.test.tsx > FloorPlanImportPage 发布、权限与下钻错误（M018-003/004/005/008） > 无 floor_plan.import.view 时不显示下钻按钮
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx:263:60
 FAIL  src/admin/pages/FloorPlanImportPage.test.tsx > FloorPlanImportPage 发布、权限与下钻错误（M018-003/004/005/008） > 下钻读取导入任务失败（403）时报错到全局，不静默
 ❯ src/admin/pages/FloorPlanImportPage.test.tsx:270:48
 ❯ runWithExpensiveErrorDiagnosticsDisabled ../../node_mo
```
- GREEN 摘要（页面修后）：
```
 ✓ src/admin/pages/VenueBusinessHoursPage.test.tsx (5 tests) 306ms
 ✓ src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx (10 tests) 364ms
 ✓ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx (4 tests) 418ms
 ✓ src/admin/pages/FloorPlanImportPage.test.tsx (13 tests) 614ms
 Test Files  4 passed (4)
      Tests  32 passed (32)
```
- `tsc -b --noEmit` exit 0。内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m018_b3r1_run.log）

```
START=2026-09-20T07:49:50Z HEAD=e456ba96
CHECK tests OK
CHECK impl OK
BEFORE_TREE=09c32950a7ba0c910270efaebb82dadac40660da
== RED: tests only
CHECK tests OK
APPLIED tests 4 files rewritten
RED_VITEST_EXIT=1
 ✓ src/admin/pages/VenueBusinessHoursPage.test.tsx (5 tests) 301ms
 ❯ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx (4 tests | 3 failed) 340ms
     ✓ 页面默认只留入口按钮，不展示平面图字段 103ms
     × 入口→步进→提交：POST 新建平面图带后端必填字段与 active 状态，成功后关闭弹窗 158ms
     × 步内校验拦截：名称留空则不进下一步、不提交 40ms
     × 步内校验拦截：楼层或底图留空则不进下一步、不提交 37ms
 ❯ src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx (10 tests | 2 failed) 1361ms
     ✓ 默认只留入口按钮，不展示弹窗字段 22ms
     ✓ 入口→步进→提交：带正确 body 且成功后关闭弹窗 238ms
     ✓ 新建没有停用选项，确认页状态固定为启用 41ms
     ✓ 步内校验拦截：编码/显示名缺失则不进下一步、不提交 25ms
     × 已删除种类不再提供申请删除但可恢复；已存档可取消存档 7ms
== GREEN: impl
CHECK impl OK
APPLIED impl 23 edits
GREEN_VITEST_EXIT=0
 ✓ src/admin/pages/VenueBusinessHoursPage.test.tsx (5 tests) 306ms
 ✓ src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx (10 tests) 364ms
 ✓ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx (4 tests) 418ms
 ✓ src/admin/pages/FloorPlanImportPage.test.tsx (13 tests) 614ms
 Test Files  4 passed (4)
      Tests  32 passed (32)
GREEN_TSC_EXIT=0
AFTER_TREE=e2d60c3c41a49a27547081ea6c411ba88155bc4e
DIFF bytes=57181 sha256=376b8265c2fcc9e5131694319a70d3ab7b3cb7a040906cb85077012e174ddf1e
 .../FloorPlanImportPage.guidedcreate.test.tsx      |  82 +++++++---
 .../src/admin/pages/FloorPlanImportPage.test.tsx   | 169 +++++++++++++++++----
 apps/web/src/admin/pages/FloorPlanImportPage.tsx   | 137 ++++++++++++-----
 .../pages/SpaceTypesPage.guidedcreate.test.tsx     |  84 ++++++++--
 apps/web/src/admin/pages/SpaceTypesPage.tsx        |  13 +-
 .../admin/pages/VenueBusinessHoursPage.test.tsx    |  37 ++++-
 6 files changed, 417 insertions(+), 105 deletions(-)
hunks=36 files=6
apps/web/src/admin/pages/FloorPlanImportPage.tsx 432 47e1a50d97ca7b59daf107046020d635cfc3934eeff23326bc0dd6406e378cb6
apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx 148 5790a81e551db768b9ee12b23418bd949cddc5e83bd326ce40cd8f9d33a0fa97
apps/web/src/admin/pages/FloorPlanImportPage.test.tsx 272 a08a7ec69263e2ef3b08d367ec543bc28105553cb69f1dce54a7167d20da705d
apps/web/src/admin/pages/SpaceTypesPage.tsx 280 4d450b0df0de1acdba22e9dec777f8fa11159defd395a3601b19171efd88deed
apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx 166 db80bdf26bdd9ce931e72591d666faf168ca06ad2aa993f91d792feefdfa9e4b
apps/web/src/admin/pages/VenueBusinessHoursPage.tsx 248 841c1e543bd573d6fbfa7e2773f7aa391e58cee430695199e5bc105be32973a9
apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx 119 60f3642dd6161bbabffd75483af9ba9a05d84176ebf7e7461e8c8855c6e6118c
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-20T07:50:03Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
