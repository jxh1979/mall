CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M023 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T19:55:18Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M023 B3-r1 整批修复 diff：before tree `bccb4a116bcbd5752a105a8b5009f7b62ae21626` → after tree `1812e0f7017ccf3268183b8b94ea25521e246fc3`；冻结 diff `/home/james/audit-tmp/m023_b3_r1.diff`（68654 bytes，SHA256 c40950302b84891de3e3d776d1b498d1ae5b3ee815ea97853553b27e35b60735），全文见 §2。定稿 ID：M023-001…006（findings V2.72，队列「是」）。
- 顺序：Grok 已先派（modules/M023-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐块归因（§5 闸 3）

| 文件 | hunk | ±行数 | 归因 ID |
|---|---|---:|---|
| MerchantScopePage.test.tsx | `@@ -1,3 +1,4 @@` | 1 | 001/002/003/005 |
| MerchantScopePage.test.tsx | `@@ -5,13 +6,16 @@ import { MerchantScopePage` | 11 | 001/003 |
| MerchantScopePage.test.tsx | `@@ -43,8 +47,9 @@ function renderPage(user: ` | 3 | 005 |
| MerchantScopePage.test.tsx | `@@ -58,7 +63,7 @@ describe("MerchantScopePag` | 2 | 001/002/003/005 |
| MerchantScopePage.test.tsx | `@@ -107,11 +112,71 @@ describe("MerchantScop` | 64 | 001/002/003/005 |
| MerchantScopePage.test.tsx | `@@ -133,12 +198,14 @@ describe("MerchantScop` | 10 | 003 |
| MerchantScopePage.tsx | `@@ -1,28 +1,53 @@` | 35 | 001/002/003/005 |
| MerchantScopePage.tsx | `@@ -39,15 +64,12 @@ function kpiFocusTitle(f` | 9 | 001 |
| MerchantScopePage.tsx | `@@ -55,11 +77,11 @@ function matchesMerchant` | 8 | 001/005 |
| MerchantScopePage.tsx | `@@ -95,8 +117,8 @@ export function MerchantS` | 4 | 001 |
| MerchantScopePage.tsx | `@@ -104,49 +126,30 @@ export function Mercha` | 39 | 001/002/003/005 |
| MerchantScopePage.tsx | `@@ -156,8 +159,8 @@ export function Merchant` | 4 | 001/003/005 |
| MerchantScopePage.tsx | `@@ -168,8 +171,8 @@ export function Merchant` | 4 | 001/003/005 |
| MerchantScopePage.tsx | `@@ -181,7 +184,7 @@ export function Merchant` | 2 | 002 |
| MerchantScopePage.tsx | `@@ -194,14 +197,12 @@ export function Mercha` | 4 | 001/002 |
| MerchantZoneContractsPage.test.tsx | `@@ -1,3 +1,4 @@` | 1 | 003/005 |
| MerchantZoneContractsPage.test.tsx | `@@ -6,8 +7,9 @@ import { AdminApi } from "..` | 3 | 003 |
| MerchantZoneContractsPage.test.tsx | `@@ -37,7 +39,8 @@ function renderPage() {` | 3 | 003/005 |
| MerchantZoneContractsPage.test.tsx | `@@ -46,18 +49,16 @@ describe("MerchantZoneCo` | 8 | 005 |
| MerchantZoneContractsPage.test.tsx | `@@ -67,19 +68,21 @@ describe("MerchantZoneCo` | 6 | 003 |
| MerchantZoneContractsPage.test.tsx | `@@ -91,6 +94,17 @@ describe("MerchantZoneCon` | 11 | 005 |
| MerchantZoneContractsPage.test.tsx | `@@ -99,19 +113,22 @@ describe("MerchantZoneC` | 9 | 005 |
| MerchantZoneContractsPage.test.tsx | `@@ -122,21 +139,4 @@ describe("MerchantZoneC` | 17 | 005 |
| MerchantZoneContractsPage.tsx | `@@ -1,10 +1,10 @@` | 2 | 005 |
| MerchantZoneContractsPage.tsx | `@@ -26,6 +26,14 @@ const STATUS_LABELS: Reco` | 8 | 003 |
| MerchantZoneContractsPage.tsx | `@@ -38,7 +46,8 @@ function money(cent: numbe` | 3 | 005 |
| MerchantZoneContractsPage.tsx | `@@ -48,31 +57,27 @@ function readKpiFocusFro` | 16 | 003/005 |
| MerchantZoneContractsPage.tsx | `@@ -127,20 +132,12 @@ export function Mercha` | 16 | 003/005 |
| MerchantZoneSettlementsPage.test.tsx | `@@ -1,3 +1,4 @@` | 1 | 003/004/005/006 |
| MerchantZoneSettlementsPage.test.tsx | `@@ -5,10 +6,20 @@ import { MerchantZoneSettl` | 16 | 003/006 |
| MerchantZoneSettlementsPage.test.tsx | `@@ -18,9 +29,18 @@ function envelope(items: ` | 13 | 006 |
| MerchantZoneSettlementsPage.test.tsx | `@@ -28,69 +48,136 @@ function makeApi(): Adm` | 99 | 003/004/005/006 |
| MerchantZoneSettlementsPage.test.tsx | `@@ -99,9 +186,16 @@ describe("MerchantZoneSe` | 9 | 005/006 |
| MerchantZoneSettlementsPage.test.tsx | `@@ -113,20 +207,11 @@ describe("MerchantZone` | 19 | 005/006 |
| MerchantZoneSettlementsPage.tsx | `@@ -1,17 +1,32 @@` | 17 | 006 |
| MerchantZoneSettlementsPage.tsx | `@@ -28,10 +43,52 @@ const STATUS_LABELS: Rec` | 44 | 003/004/005/006 |
| MerchantZoneSettlementsPage.tsx | `@@ -46,9 +103,11 @@ function focusTitle(subs` | 6 | 005/006 |
| MerchantZoneSettlementsPage.tsx | `@@ -85,20 +144,62 @@ export function Merchan` | 52 | 003/004/005/006 |
| MerchantZoneSettlementsPage.tsx | `@@ -108,17 +209,18 @@ export function Mercha` | 11 | 003/005/006 |
| MerchantZoneSettlementsPage.tsx | `@@ -132,7 +234,7 @@ export function Merchant` | 2 | 006 |
| MerchantZoneSettlementsPage.tsx | `@@ -148,6 +250,18 @@ export function Merchan` | 12 | 006 |

- 6 文件 41 个 hunk 全部落到 001–006（按「文件可含 ID 集 ∩ 关键词命中」归因；版本标记/import/空白等结构行归该文件所含 ID）；未改 types.ts/AdminApp/TraceLink/路由/后端/三份 HTML/锁；归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/apps/web/src/admin/pages/MerchantScopePage.test.tsx b/apps/web/src/admin/pages/MerchantScopePage.test.tsx
index dbfdd8b6..af69c8c3 100644
--- a/apps/web/src/admin/pages/MerchantScopePage.test.tsx
+++ b/apps/web/src/admin/pages/MerchantScopePage.test.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -5,13 +6,16 @@ import { MerchantScopePage } from "./MerchantScopePage";
 import { AdminApi } from "../client/adminClient";
 import type { AdminPageProps, EffectivePermissionsState } from "../types";
 
+// M023-001：夹具按后端真实返回形状（MerchantWorkOrderRecord / MerchantBillRecord 的蛇形键），不自带后端不存在的字段。
 const WORK_ORDERS = [
-  { id: "wo-1", merchant_id: "m-1", work_order_type: "repair", title: "工单甲", priority: "high", status: "processing", authorized_scope: "工单" },
-  { id: "wo-2", merchant_id: "m-2", work_order_type: "repair", title: "工单乙", priority: "high", status: "processing", authorized_scope: "工单" },
+  { id: "wo-1", merchant_id: "m-1", work_order_type: "repair", title: "工单甲", priority: "high", status: "processing" },
+  { id: "wo-2", merchant_id: "m-2", work_order_type: "repair", title: "工单乙", priority: "high", status: "processing" },
 ];
 const BILLS = [
-  { id: "bl-1", merchant_id: "m-1", bill_no: "B-1", title: "账单甲", total_amount: 100, status: "unpaid", authorized_scope: "账单" },
-  { id: "bl-2", merchant_id: "m-2", bill_no: "B-2", title: "账单乙", total_amount: 200, status: "unpaid", authorized_scope: "账单" },
+  { id: "bl-1", merchant_id: "m-1", bill_no: "B-1", total_amount_cent: 10000, currency: "CNY", status: "issued" },
+  { id: "bl-2", merchant_id: "m-2", bill_no: "B-2", total_amount_cent: 20000, currency: "CNY", status: "issued" },
+  { id: "bl-3", merchant_id: "m-1", bill_no: "B-3", total_amount_cent: 2550, currency: "CNY", status: "viewed" },
+  { id: "bl-4", merchant_id: "m-1", bill_no: "B-4", total_amount_cent: 0, currency: "CNY", status: "weird_code" },
 ];
 
 function envelope(items: unknown[]): Response {
@@ -43,8 +47,9 @@ function renderPage(user: AdminPageProps["user"] = { id: "u1", username: "mercha
     setActionResult: vi.fn(),
     setGlobalError: vi.fn(),
     effectivePermissions: ADMIN_PERMS,
+    navigateTo: vi.fn(),
   };
-  render(<MerchantScopePage {...fullProps} />);
+  return render(<MerchantScopePage {...fullProps} />);
 }
 
 describe("MerchantScopePage KPI 聚焦视图", () => {
@@ -58,7 +63,7 @@ describe("MerchantScopePage KPI 聚焦视图", () => {
     await waitFor(() => expect(screen.getByTestId("merchant-scope-kpi-bills")).toBeTruthy());
     fireEvent.click(screen.getByTestId("merchant-scope-kpi-bills"));
     expect(window.location.search).toContain("kpi=bills");
-    expect(screen.getByTestId("merchant-scope-kpi-focus-banner").textContent).toContain("正在查看：商户账单（1）");
+    expect(screen.getByTestId("merchant-scope-kpi-focus-banner").textContent).toContain("正在查看：商户账单（3）");
     expect(screen.getByTestId("merchant-scope-bills-table")).toBeTruthy();
     expect(screen.queryByTestId("merchant-scope-work-orders-table")).toBeNull();
     expect(screen.queryByTestId("merchant-scope-summary")).toBeNull();
@@ -107,11 +112,71 @@ describe("MerchantScopePage KPI 聚焦视图", () => {
     expect(screen.queryByTestId("merchant-scope-bill-row-bl-2")).toBeNull();
   });
 
-  it("平台管理员无商户范围时不看全量", async () => {
+  it("平台管理员无商户范围时不请求、不看全量，给中文提示", async () => {
     renderPage({ id: "u-admin", username: "admin", accountType: "platform_admin" });
-    await waitFor(() => expect(screen.getByTestId("merchant-scope-page")).toBeTruthy());
+    await waitFor(() => expect(screen.getByTestId("admin-error")).toBeTruthy());
+    expect(screen.getByTestId("admin-error").textContent).toContain("缺少商户授权范围");
     expect(screen.queryByTestId("merchant-scope-work-order-row-wo-1")).toBeNull();
     expect(screen.queryByTestId("merchant-scope-bill-row-bl-1")).toBeNull();
+    expect(vi.mocked(fetch)).not.toHaveBeenCalled();
+  });
+});
+
+describe("MerchantScopePage 按后端真实字段展示（M023-001/002/003/005）", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    window.history.replaceState({}, "", "/admin/merchant/scope");
+  });
+
+  it("金额读 total_amount_cent 分转元，账单金额 KPI 为本商户合计", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("merchant-scope-bill-row-bl-1")).toBeTruthy());
+    expect(screen.getByTestId("merchant-scope-bill-row-bl-1").textContent).toContain("100.00 元");
+    expect(screen.getByTestId("merchant-scope-bill-row-bl-3").textContent).toContain("25.50 元");
+    expect(screen.getByTestId("merchant-scope-kpi-bill-amount").textContent).toBe("125.50 元");
+  });
+
+  it("账单状态用账单字典：issued→已出账（非票务义「已出票」）、viewed→已查看、未知码→未知状态，无英文原码", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("merchant-scope-bills-table")).toBeTruthy());
+    const text = screen.getByTestId("merchant-scope-bills-table").textContent ?? "";
+    expect(text).toContain("已出账");
+    expect(text).not.toContain("已出票");
+    expect(text).toContain("已查看");
+    expect(text).toContain("未知状态");
+    expect(text).not.toContain("issued");
+    expect(text).not.toContain("viewed");
+    expect(text).not.toContain("weird_code");
+  });
+
+  it("不渲染后端不存在的列，也没有不调后端的「确认授权范围」按钮", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("merchant-scope-summary")).toBeTruthy());
+    const page = screen.getByTestId("merchant-scope-page").textContent ?? "";
+    expect(page).not.toContain("授权范围0");
+    expect(screen.getByTestId("merchant-scope-work-orders-table").textContent).not.toContain("留痕证据");
+    expect(screen.getByTestId("merchant-scope-bills-table").textContent).not.toContain("留痕证据");
+    expect(screen.getByTestId("merchant-scope-bills-table").querySelector("thead")?.textContent).toBe("商户账单号金额状态详情");
+    expect(screen.queryByTestId("merchant-scope-confirm")).toBeNull();
+  });
+
+  it("接口证据写真实权限码与真实留痕事件", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("merchant-scope-summary")).toBeTruthy());
+    const evidence = screen.getByTestId("merchant-scope-evidence").textContent ?? "";
+    expect(evidence).toContain("merchant.service_work_order.view");
+    expect(evidence).toContain("merchant.billing.view");
+    expect(evidence).not.toContain("merchant.portal.read");
+    const page = screen.getByTestId("merchant-scope-page").textContent ?? "";
+    expect(page).toContain("merchant_billing.bill.viewed");
+    expect(page).not.toContain("merchant.scope_confirmed");
+  });
+
+  it("商户号为纯文本，不渲染点了没反应的下钻按钮", async () => {
+    const { container } = renderPage();
+    await waitFor(() => expect(screen.getByTestId("merchant-scope-bill-row-bl-1")).toBeTruthy());
+    expect(container.querySelector('[data-testid^="trace-link-"]')).toBeNull();
+    expect(screen.getByTestId("merchant-scope-bill-row-bl-1").textContent).toContain("m-1");
   });
 });
 
@@ -133,12 +198,14 @@ describe("MerchantScopePage 行明细抽屉", () => {
     await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
   });
 
-  it("点账单行 → 抽屉含该行账单号", async () => {
+  it("点账单行 → 抽屉含该行账单号、分转元金额与中文状态", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("merchant-scope-bill-row-bl-1")).toBeTruthy());
     fireEvent.click(screen.getByTestId("merchant-scope-bill-row-bl-1"));
-    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("B-1");
-    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("100.00 元");
-    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("CNY");
+    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
+    expect(text).toContain("B-1");
+    expect(text).toContain("100.00 元");
+    expect(text).toContain("已出账");
+    expect(text).not.toContain("CNY");
   });
 });
diff --git a/apps/web/src/admin/pages/MerchantScopePage.tsx b/apps/web/src/admin/pages/MerchantScopePage.tsx
index 628435f0..5c297a83 100644
--- a/apps/web/src/admin/pages/MerchantScopePage.tsx
+++ b/apps/web/src/admin/pages/MerchantScopePage.tsx
@@ -1,28 +1,53 @@
+// 文件版本：V0.01
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
 import { AuditTrailHint } from "../components/AuditTrailHint";
 import { useAdminResource } from "../hooks/useAdminResource";
-import type { AdminPageProps, MerchantScopeBillRow, MerchantScopeWorkOrderRow } from "../types";
+import type { AdminPageProps } from "../types";
 import { formatBusinessStatus } from "../i18n/statusLabels";
-import { TraceLink } from "../components/TraceLink";
 import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
 
+// M023-001：行类型按后端真实返回收口（MerchantWorkOrderRecord / MerchantBillRecord），不读后端不存在的字段。
+type WorkOrderRow = { id: string; merchantId?: string; workOrderType?: string; priority?: string; status?: string; title?: string };
+type BillRow = { id: string; merchantId?: string; billNo?: string; totalAmountCent?: number | null; status?: string };
+
 const TEXT = {
   title: "商户授权工作台",
   description: "商户侧只查看已授权业务范围内的工单和账单；平台治理、财务总账、第三方配置等跨主体能力继续由权限网关拦截。",
-  evidence: "接口证据：读取 /api/v1/merchant-service/work-orders、/api/v1/merchant-billing/bills；权限由 merchant.portal.read、merchant.service_work_order.view、merchant.billing.view 控制。",
+  evidence: "接口证据：读取 /api/v1/merchant-service/work-orders、/api/v1/merchant-billing/bills；权限由 merchant.service_work_order.view、merchant.billing.view 控制。",
   boundary: "商户角色不得访问平台治理、财务总账、第三方配置和其它商户数据；真实多商户隔离仍以后端权限和数据范围策略为准，本页只展示当前授权范围。",
   empty: "暂无授权商户数据",
   loadFailed: "商户授权数据读取失败",
   scopeMissing: "当前账号缺少商户授权范围，无法展示任何商户数据，请联系管理员配置。",
-  confirm: "确认授权范围",
   focusClear: "清除筛选，返回全部",
 };
 
+// M023-003：账单状态用页内字典（与商户专区「我的账单」同词）；未知码给中文占位，不回显英文原码。
+const BILL_STATUS_LABELS: Record<string, string> = {
+  issued: "已出账",
+  viewed: "已查看",
+  confirmed: "已确认",
+  disputed: "有异议",
+  overdue: "已逾期",
+  closed: "已结清",
+};
+const UNKNOWN = "未知状态";
+
+function billStatus(code: string | null | undefined): string {
+  if (!code) return "-";
+  return BILL_STATUS_LABELS[code] ?? UNKNOWN;
+}
+
+function zh(code: string | null | undefined): string {
+  if (!code) return "-";
+  const label = formatBusinessStatus(code);
+  return label === code ? UNKNOWN : label;
+}
+
 // KPI 聚焦视图（照 OrganizationMerchantPage 样板）：?kpi=<workOrders|bills>；点 KPI 数字=带参重进本页只渲染对应表。
-// 授权范围/账单金额是汇总口径，无独立承载表，其 KPI 数字保持纯文本不可点。
+// 账单金额是汇总口径，无独立承载表，其 KPI 数字保持纯文本不可点。
 type KpiTarget = "workOrders" | "bills";
 type KpiFocus = { table: KpiTarget };
 
@@ -39,15 +64,12 @@ function kpiFocusTitle(focus: KpiFocus): string {
 
 function display(value: unknown) {
   if (value === null || value === undefined || value === "") return "-";
-  if (typeof value === "number") return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(value);
   return String(value);
 }
 
-function moneyYuan(value: unknown) {
-  if (value === null || value === undefined || value === "") return "-";
-  const n = typeof value === "number" ? value : Number(value);
-  if (!Number.isFinite(n)) return "-";
-  return `${new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(n)} 元`;
+function moneyCent(cent: number | null | undefined) {
+  if (cent === null || cent === undefined || !Number.isFinite(cent)) return "-";
+  return `${new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cent / 100)} 元`;
 }
 
 function matchesMerchant(merchantId: string | null | undefined, rowMerchantId: string | undefined) {
@@ -55,11 +77,11 @@ function matchesMerchant(merchantId: string | null | undefined, rowMerchantId: s
   return rowMerchantId === merchantId;
 }
 
-export function MerchantScopePage({ api, user, setActionResult, setGlobalError, navigateTo }: AdminPageProps) {
-  const [workOrders, setWorkOrders] = React.useState<MerchantScopeWorkOrderRow[]>([]);
-  const [bills, setBills] = React.useState<MerchantScopeBillRow[]>([]);
+export function MerchantScopePage({ api, user, setGlobalError }: AdminPageProps) {
+  const [workOrders, setWorkOrders] = React.useState<WorkOrderRow[]>([]);
+  const [bills, setBills] = React.useState<BillRow[]>([]);
   const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
-  // 行明细抽屉：整行可点 → 该工单/账单完整字段（枚举中文·引用 TraceLink）。
+  // 行明细抽屉：整行可点 → 该工单/账单完整字段（枚举中文）。
   const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
 
   React.useEffect(() => {
@@ -95,8 +117,8 @@ export function MerchantScopePage({ api, user, setActionResult, setGlobalError,
         throw new Error(TEXT.scopeMissing);
       }
       const [workOrderRows, billRows] = await Promise.all([
-        api.get<MerchantScopeWorkOrderRow[]>("/api/v1/merchant-service/work-orders"),
-        api.get<MerchantScopeBillRow[]>("/api/v1/merchant-billing/bills"),
+        api.get<WorkOrderRow[]>("/api/v1/merchant-service/work-orders"),
+        api.get<BillRow[]>("/api/v1/merchant-billing/bills"),
       ]);
       setWorkOrders(workOrderRows.filter((row) => matchesMerchant(user.merchantId, row.merchantId)));
       setBills(billRows.filter((row) => matchesMerchant(user.merchantId, row.merchantId)));
@@ -104,49 +126,30 @@ export function MerchantScopePage({ api, user, setActionResult, setGlobalError,
     { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
   );
 
-  const authorizedScopes = Array.from(new Set([...workOrders, ...bills].map((row) => row.authorizedScope).filter(Boolean))).sort();
-  const totalBillAmount = bills.reduce((sum, row) => sum + Number(row.totalAmount ?? 0), 0);
+  const totalBillCent = bills.reduce((sum, row) => sum + Number(row.totalAmountCent ?? 0), 0);
   const hasRows = workOrders.length > 0 || bills.length > 0;
 
-  function confirmScope() {
-    setGlobalError(null);
-    setActionResult({
-      status: "merchant_scope_confirmed",
-      audit: "merchant_authorized_scope_checked",
-      merchantId: user.merchantId ?? "unscoped",
-      authorizedScopes,
-      workOrderCount: workOrders.length,
-      billCount: bills.length,
-      evidenceAttachmentIds: ["att-merchant-scope-confirm"],
-    });
-  }
-
-  function openWorkOrderDetail(row: MerchantScopeWorkOrderRow) {
+  function openWorkOrderDetail(row: WorkOrderRow) {
     setDetail({
       title: `授权工单明细 · ${display(row.title)}`,
       fields: [
-        { label: "商户", value: row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId) },
-        { label: "类型", value: formatBusinessStatus(row.workOrderType) },
+        { label: "商户", value: display(row.merchantId) },
+        { label: "类型", value: zh(row.workOrderType) },
         { label: "标题", value: display(row.title) },
-        { label: "优先级", value: formatBusinessStatus(row.priority) },
-        { label: "状态", value: formatBusinessStatus(row.status) },
-        { label: "授权范围", value: display(row.authorizedScope) },
-        { label: "留痕证据", value: display(row.auditEvidenceRef) },
+        { label: "优先级", value: zh(row.priority) },
+        { label: "状态", value: zh(row.status) },
       ],
     });
   }
 
-  function openBillDetail(row: MerchantScopeBillRow) {
+  function openBillDetail(row: BillRow) {
     setDetail({
       title: `授权账单明细 · ${display(row.billNo)}`,
       fields: [
-        { label: "商户", value: row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId) },
+        { label: "商户", value: display(row.merchantId) },
         { label: "账单号", value: display(row.billNo) },
-        { label: "标题", value: display(row.title) },
-        { label: "金额", value: moneyYuan(row.totalAmount) },
-        { label: "状态", value: formatBusinessStatus(row.status) },
-        { label: "授权范围", value: display(row.authorizedScope) },
-        { label: "留痕证据", value: display(row.auditEvidenceRef) },
+        { label: "金额", value: moneyCent(row.totalAmountCent) },
+        { label: "状态", value: billStatus(row.status) },
       ],
     });
   }
@@ -156,8 +159,8 @@ export function MerchantScopePage({ api, user, setActionResult, setGlobalError,
       <h3>授权工单</h3>
       <div className="admin-table-scroll">
         <table className="admin-table" data-testid="merchant-scope-work-orders-table">
-          <thead><tr><th>商户</th><th>类型</th><th>标题</th><th>优先级</th><th>状态</th><th>授权范围</th><th>留痕证据</th><th>详情</th></tr></thead>
-          <tbody>{workOrders.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-scope-work-order-row-${row.id}`} onClick={() => openWorkOrderDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openWorkOrderDetail(row); } }}><td onClick={(event) => event.stopPropagation()}>{row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId)}</td><td>{formatBusinessStatus(row.workOrderType)}</td><td>{display(row.title)}</td><td>{formatBusinessStatus(row.priority)}</td><td>{formatBusinessStatus(row.status)}</td><td>{display(row.authorizedScope)}</td><td>{display(row.auditEvidenceRef)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看授权工单详情" onClick={(event) => { event.stopPropagation(); openWorkOrderDetail(row); }}>查看详情</button></td></tr>)}</tbody>
+          <thead><tr><th>商户</th><th>类型</th><th>标题</th><th>优先级</th><th>状态</th><th>详情</th></tr></thead>
+          <tbody>{workOrders.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-scope-work-order-row-${row.id}`} onClick={() => openWorkOrderDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openWorkOrderDetail(row); } }}><td>{display(row.merchantId)}</td><td>{zh(row.workOrderType)}</td><td>{display(row.title)}</td><td>{zh(row.priority)}</td><td>{zh(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看授权工单详情" onClick={(event) => { event.stopPropagation(); openWorkOrderDetail(row); }}>查看详情</button></td></tr>)}</tbody>
         </table>
       </div>
     </>
@@ -168,8 +171,8 @@ export function MerchantScopePage({ api, user, setActionResult, setGlobalError,
       <h3>授权账单</h3>
       <div className="admin-table-scroll">
         <table className="admin-table" data-testid="merchant-scope-bills-table">
-          <thead><tr><th>商户</th><th>账单号</th><th>标题</th><th>金额</th><th>状态</th><th>授权范围</th><th>留痕证据</th><th>详情</th></tr></thead>
-          <tbody>{bills.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-scope-bill-row-${row.id}`} onClick={() => openBillDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBillDetail(row); } }}><td onClick={(event) => event.stopPropagation()}>{row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId)}</td><td>{display(row.billNo)}</td><td>{display(row.title)}</td><td>{moneyYuan(row.totalAmount)}</td><td>{formatBusinessStatus(row.status)}</td><td>{display(row.authorizedScope)}</td><td>{display(row.auditEvidenceRef)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看授权账单详情" onClick={(event) => { event.stopPropagation(); openBillDetail(row); }}>查看详情</button></td></tr>)}</tbody>
+          <thead><tr><th>商户</th><th>账单号</th><th>金额</th><th>状态</th><th>详情</th></tr></thead>
+          <tbody>{bills.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-scope-bill-row-${row.id}`} onClick={() => openBillDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBillDetail(row); } }}><td>{display(row.merchantId)}</td><td>{display(row.billNo)}</td><td className="admin-num">{moneyCent(row.totalAmountCent)}</td><td>{billStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看授权账单详情" onClick={(event) => { event.stopPropagation(); openBillDetail(row); }}>查看详情</button></td></tr>)}</tbody>
         </table>
       </div>
     </>
@@ -181,7 +184,7 @@ export function MerchantScopePage({ api, user, setActionResult, setGlobalError,
     <section data-testid="merchant-scope-page">
       <AdminPageHeader title={TEXT.title} description={TEXT.description} />
       <TechDetails><div className="admin-generic-evidence" data-testid="merchant-scope-evidence">{TEXT.evidence}</div></TechDetails>
-      <AuditTrailHint event="merchant.scope.viewed / merchant.scope_confirmed" />
+      <AuditTrailHint event="merchant_service.work_order.viewed / merchant_billing.bill.viewed" />
       <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
         {!hasRows ? <div className="admin-state admin-empty" data-testid="merchant-scope-empty">{TEXT.empty}</div> : kpiFocus ? (
           <>
@@ -194,14 +197,12 @@ export function MerchantScopePage({ api, user, setActionResult, setGlobalError,
         ) : (
           <>
             <div className="admin-kpi-grid" data-testid="merchant-scope-summary">
-              <div className="admin-kpi-card"><span>授权范围</span><strong>{authorizedScopes.length}</strong><small>{authorizedScopes.join(" / ") || "-"}</small></div>
               <div className="admin-kpi-card admin-kpi-card--clickable"><span>商户工单</span><button type="button" className="admin-kpi-value" data-testid="merchant-scope-kpi-work-orders" onClick={() => enterKpiFocus("workOrders")}>{workOrders.length}</button><small>当前商户</small></div>
               <div className="admin-kpi-card admin-kpi-card--clickable"><span>商户账单</span><button type="button" className="admin-kpi-value" data-testid="merchant-scope-kpi-bills" onClick={() => enterKpiFocus("bills")}>{bills.length}</button><small>授权账单</small></div>
-              <div className="admin-kpi-card"><span>账单金额</span><strong>{moneyYuan(totalBillAmount)}</strong><small>只读汇总</small></div>
+              <div className="admin-kpi-card"><span>账单金额</span><strong data-testid="merchant-scope-kpi-bill-amount">{moneyCent(totalBillCent)}</strong><small>只读汇总</small></div>
             </div>
 
             <div className="admin-form-row">
-              <button type="button" data-testid="merchant-scope-confirm" onClick={confirmScope}>{TEXT.confirm}</button>
               <span className="admin-muted-action" data-testid="merchant-scope-boundary">{TEXT.boundary}</span>
             </div>
 
diff --git a/apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx b/apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx
index da2029b1..b38f6f1c 100644
--- a/apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx
+++ b/apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -6,8 +7,9 @@ import { AdminApi } from "../client/adminClient";
 import type { AdminPageProps, EffectivePermissionsState } from "../types";
 
 const CONTRACTS = [
-  { id: "contract-aaaa1111", lifecycle_status: "effective", effective_date: "2026-01-01", expiry_date: "2026-12-31", deposit_cent: 100000, space_ids: ["sp-1"] },
+  { id: "contract-aaaa1111", lifecycle_status: "effective", effective_date: "2026-01-01", expiry_date: "2026-12-31", deposit_cent: 100000, space_ids: ["sp-1", "sp-2"], renewed_from_contract_id: "contract-bbbb2222" },
   { id: "contract-bbbb2222", lifecycle_status: "expired", effective_date: "2025-01-01", expiry_date: "2025-12-31", deposit_cent: 50000, space_ids: [] },
+  { id: "contract-cccc3333", lifecycle_status: "weird_status", effective_date: "2024-01-01", expiry_date: "2024-12-31", deposit_cent: 0, space_ids: [] },
 ];
 
 function envelope(items: unknown[]): Response {
@@ -37,7 +39,8 @@ function renderPage() {
     effectivePermissions: ADMIN_PERMS,
     navigateTo: vi.fn(),
   };
-  render(<MerchantZoneContractsPage {...props} />);
+  const view = render(<MerchantZoneContractsPage {...props} />);
+  return { props, container: view.container };
 }
 
 describe("MerchantZoneContractsPage KPI 聚焦视图", () => {
@@ -46,18 +49,16 @@ describe("MerchantZoneContractsPage KPI 聚焦视图", () => {
     window.history.replaceState({}, "", "/admin/merchant-zone/contracts");
   });
 
-  it("点“已生效” → URL 带 ?kpi=contracts&subset=effective，聚焦只剩已生效合同，合同下钻 TraceLink 保留", async () => {
+  it("点“已生效” → URL 带 ?kpi=contracts&subset=effective，聚焦只剩已生效合同", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("merchant-zone-contracts-kpi-effective")).toBeTruthy());
     fireEvent.click(screen.getByTestId("merchant-zone-contracts-kpi-effective"));
     expect(window.location.search).toContain("kpi=contracts");
     expect(window.location.search).toContain("subset=effective");
     expect(screen.getByTestId("merchant-zone-contracts-kpi-focus-banner").textContent).toContain("正在查看：已生效合同（1）");
-    const table = screen.getByTestId("merchant-zone-contracts-table").textContent ?? "";
-    expect(table).toContain("aaaa1111");
-    expect(table).not.toContain("bbbb2222");
+    expect(screen.getByTestId("merchant-zone-contract-row-contract-aaaa1111")).toBeTruthy();
+    expect(screen.queryByTestId("merchant-zone-contract-row-contract-bbbb2222")).toBeNull();
     expect(screen.queryByTestId("merchant-zone-contracts-summary")).toBeNull();
-    expect(screen.getByTestId("trace-link-ADM-CONT-001-contract-aaaa1111")).toBeTruthy();
   });
 
   it("聚焦后点「返回全部」→ 恢复 KPI 卡+全量合同", async () => {
@@ -67,19 +68,21 @@ describe("MerchantZoneContractsPage KPI 聚焦视图", () => {
     fireEvent.click(screen.getByTestId("merchant-zone-contracts-kpi-focus-clear"));
     expect(window.location.search).not.toContain("kpi=");
     expect(screen.getByTestId("merchant-zone-contracts-summary")).toBeTruthy();
-    expect(screen.getByTestId("merchant-zone-contracts-table").textContent).toContain("bbbb2222");
+    expect(screen.getByTestId("merchant-zone-contract-row-contract-bbbb2222")).toBeTruthy();
   });
 
-  it("生命周期状态列为中文，不含英文枚举码", async () => {
+  it("生命周期状态列为中文：未知码给中文占位，不含英文枚举码", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("merchant-zone-contracts-table")).toBeTruthy());
     const table = screen.getByTestId("merchant-zone-contracts-table").textContent ?? "";
     expect(table).toContain("已生效");
     expect(table).toContain("已到期");
+    expect(table).toContain("未知状态");
     expect(table).not.toContain("履约中");
     expect(table).not.toContain("起草中");
     expect(table).not.toContain("effective");
     expect(table).not.toContain("expired");
+    expect(table).not.toContain("weird_status");
     expect(table).toContain("元");
     expect(table).not.toContain("CNY");
   });
@@ -91,6 +94,17 @@ describe("MerchantZoneContractsPage KPI 聚焦视图", () => {
     expect(table).toContain("2026年1月1日");
     expect(table).not.toContain("2026-01-01");
   });
+
+  it("合同号 / 空间 / 续签自为纯文本，不渲染商户点了没反应的下钻按钮（M023-005）", async () => {
+    const { container, props } = renderPage();
+    await waitFor(() => expect(screen.getByTestId("merchant-zone-contract-row-contract-aaaa1111")).toBeTruthy());
+    expect(container.querySelector('[data-testid^="trace-link-"]')).toBeNull();
+    const row = screen.getByTestId("merchant-zone-contract-row-contract-aaaa1111").textContent ?? "";
+    expect(row).toContain("aaaa1111");
+    expect(row).toContain("sp-1、sp-2");
+    expect(row).toContain("bbbb2222");
+    expect(props.navigateTo).not.toHaveBeenCalled();
+  });
 });
 
 describe("MerchantZoneContractsPage 行明细抽屉（整行可点·日期中文）", () => {
@@ -99,19 +113,22 @@ describe("MerchantZoneContractsPage 行明细抽屉（整行可点·日期中文
     window.history.replaceState({}, "", "/admin/merchant-zone/contracts");
   });
 
-  it("点合同行 -> 抽屉含租期中文日期+中文状态+押金，合同下钻 TraceLink 保留", async () => {
+  it("点合同行 -> 抽屉含合同号全量、租期中文日期、中文状态、押金与空间，无下钻假按钮", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("merchant-zone-contract-row-contract-aaaa1111")).toBeTruthy());
     fireEvent.click(screen.getByTestId("merchant-zone-contract-row-contract-aaaa1111"));
     const drawer = screen.getByTestId("row-detail-drawer");
     const text = drawer.textContent ?? "";
+    expect(text).toContain("contract-aaaa1111");
     expect(text).toContain("2026年1月1日");
     expect(text).toContain("已生效");
     expect(text).not.toContain("履约中");
-    expect(text).toContain("元");
+    expect(text).toContain("1,000.00 元");
+    expect(text).toContain("sp-1、sp-2");
+    expect(text).toContain("contract-bbbb2222");
     expect(text).not.toContain("CNY");
     expect(text).not.toContain("effective");
-    expect(drawer.querySelector('[data-testid="trace-link-ADM-CONT-001-contract-aaaa1111"]')).toBeTruthy();
+    expect(drawer.querySelector('[data-testid^="trace-link-"]')).toBeNull();
   });
 
   it("点合同行 -> Esc 关闭抽屉", async () => {
@@ -122,21 +139,4 @@ describe("MerchantZoneContractsPage 行明细抽屉（整行可点·日期中文
     fireEvent.keyDown(window, { key: "Escape" });
     await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
   });
-
-  it("点合同下钻 TraceLink 触发下钻、不弹抽屉（stopPropagation）", async () => {
-    const navigateTo = vi.fn();
-    const props: AdminPageProps = {
-      api: makeApi(),
-      user: { id: "u1", username: "merchant", accountType: "merchant_admin" },
-      setActionResult: vi.fn(),
-      setGlobalError: vi.fn(),
-      effectivePermissions: ADMIN_PERMS,
-      navigateTo,
-    };
-    render(<MerchantZoneContractsPage {...props} />);
-    await waitFor(() => expect(screen.getByTestId("trace-link-ADM-CONT-001-contract-aaaa1111")).toBeTruthy());
-    fireEvent.click(screen.getByTestId("trace-link-ADM-CONT-001-contract-aaaa1111"));
-    expect(navigateTo).toHaveBeenCalled();
-    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
-  });
 });
diff --git a/apps/web/src/admin/pages/MerchantZoneContractsPage.tsx b/apps/web/src/admin/pages/MerchantZoneContractsPage.tsx
index a4c49c03..c9ca5a51 100644
--- a/apps/web/src/admin/pages/MerchantZoneContractsPage.tsx
+++ b/apps/web/src/admin/pages/MerchantZoneContractsPage.tsx
@@ -1,10 +1,10 @@
+// 文件版本：V0.01
 import React from "react";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
 import { AuditTrailHint } from "../components/AuditTrailHint";
 import { usePagedRows, TablePager } from "../components/TablePager";
 import { useAdminResource } from "../hooks/useAdminResource";
-import { TraceLink } from "../components/TraceLink";
 import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
 import type { AdminPageProps, MerchantZoneContractRow } from "../types";
 
@@ -26,6 +26,14 @@ const STATUS_LABELS: Record<string, string> = {
   archived: "归档",
 };
 
+// M023-003：未知码给中文占位，不把英文原码显示给用户。
+const UNKNOWN_STATUS = "未知状态";
+
+function statusLabel(code: string | null | undefined): string {
+  if (!code) return "-";
+  return STATUS_LABELS[code] ?? UNKNOWN_STATUS;
+}
+
 // ponytail: ID 展示统一截尾 8 位 + title 全量；等宽由 admin-id-cell 类保证。
 function shortId(id: string | null | undefined): string {
   if (!id) return "-";
@@ -38,7 +46,8 @@ function money(cent: number | null | undefined) {
 }
 
 // KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染合同表对应子集。
-// URL 约定：?kpi=contracts[&subset=effective]。TraceLink（合同/空间下钻）在聚焦表内保留。
+// URL 约定：?kpi=contracts[&subset=effective]。
+// M023-005：本页只对商户账号开放，合同工作台/空间页需平台权限、商户点了没反应，故合同号/空间/续签自一律纯文本（整行点开明细抽屉）。
 type KpiFocus = { subset: "all" | "effective" };
 
 function readKpiFocusFromUrl(): KpiFocus | null {
@@ -48,31 +57,27 @@ function readKpiFocusFromUrl(): KpiFocus | null {
   return { subset: params.get("subset") === "effective" ? "effective" : "all" };
 }
 
-export function MerchantZoneContractsPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
+export function MerchantZoneContractsPage({ api, setGlobalError }: AdminPageProps) {
   const [rows, setRows] = React.useState<MerchantZoneContractRow[]>([]);
   const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
-  // 行明细抽屉：整行可点 → 该合同完整字段（日期中文·金额¥·状态中文·合同/空间引用 TraceLink）。
+  // 行明细抽屉：整行可点 → 该合同完整字段（日期中文·金额元·状态中文）。
   const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
 
   function openContractDetail(row: MerchantZoneContractRow) {
     setDetail({
       title: `合同明细 · ${shortId(row.id)}`,
       fields: [
-        { label: "合同", value: <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.id}>{row.id}</TraceLink> },
+        { label: "合同", value: row.id },
         { label: "租期", value: `${cnDate(row.effectiveDate)} ~ ${cnDate(row.expiryDate)}` },
         {
           label: "空间",
-          value: (row.spaceIds ?? []).length > 0
-            ? (row.spaceIds ?? []).map((sid, i) => <React.Fragment key={sid}>{i > 0 ? "、" : ""}<TraceLink navigateTo={navigateTo} pageId="ADM-GOV-007" objectId={sid}>{sid}</TraceLink></React.Fragment>)
-            : "—",
+          value: (row.spaceIds ?? []).length > 0 ? (row.spaceIds ?? []).join("、") : "—",
         },
-        { label: "状态", value: STATUS_LABELS[row.lifecycleStatus ?? ""] ?? row.lifecycleStatus ?? "—" },
+        { label: "状态", value: statusLabel(row.lifecycleStatus) },
         { label: "押金", value: money(row.depositCent) },
         {
           label: "续签自",
-          value: row.renewedFromContractId
-            ? <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.renewedFromContractId}>{row.renewedFromContractId}</TraceLink>
-            : "—",
+          value: row.renewedFromContractId ?? "—",
         },
       ],
     });
@@ -127,20 +132,12 @@ export function MerchantZoneContractsPage({ api, setGlobalError, navigateTo }: A
             <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-zone-contract-row-${row.id}`}
               onClick={() => openContractDetail(row)}
               onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openContractDetail(row); } }}>
-              <td className="admin-id-cell" title={row.id} onClick={(event) => event.stopPropagation()}>
-                <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.id}>{shortId(row.id)}</TraceLink>
-              </td>
+              <td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td>
               <td>{cnDate(row.effectiveDate)} ~ {cnDate(row.expiryDate)}</td>
-              <td onClick={(event) => event.stopPropagation()}>{(row.spaceIds ?? []).length > 0
-                ? (row.spaceIds ?? []).map((sid, i) => <React.Fragment key={sid}>{i > 0 ? "、" : ""}<TraceLink navigateTo={navigateTo} pageId="ADM-GOV-007" objectId={sid}>{sid}</TraceLink></React.Fragment>)
-                : "-"}</td>
-              <td>{STATUS_LABELS[row.lifecycleStatus ?? ""] ?? row.lifecycleStatus ?? "-"}</td>
+              <td>{(row.spaceIds ?? []).length > 0 ? (row.spaceIds ?? []).join("、") : "-"}</td>
+              <td>{statusLabel(row.lifecycleStatus)}</td>
               <td className="admin-num">{money(row.depositCent)}</td>
-              <td className="admin-id-cell" title={row.renewedFromContractId ?? undefined} onClick={(event) => event.stopPropagation()}>
-                {row.renewedFromContractId
-                  ? <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.renewedFromContractId}>{shortId(row.renewedFromContractId)}</TraceLink>
-                  : shortId(row.renewedFromContractId)}
-              </td>
+              <td className="admin-id-cell" title={row.renewedFromContractId ?? undefined}>{shortId(row.renewedFromContractId)}</td>
             <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看合同详情" onClick={(event) => { event.stopPropagation(); openContractDetail(row); }}>查看详情</button></td></tr>
           ))}
         </tbody>
diff --git a/apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx b/apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx
index 9a8ba281..c3ed26bf 100644
--- a/apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx
+++ b/apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -5,10 +6,20 @@ import { MerchantZoneSettlementsPage } from "./MerchantZoneSettlementsPage";
 import { AdminApi } from "../client/adminClient";
 import type { AdminPageProps, EffectivePermissionsState } from "../types";
 
+// 夹具按后端 ContractSettlementRecord 真实形状（含 settlement_items 与 V307 的 voided）。
 const SETTLEMENTS = [
-  { id: "st-1", contract_id: "c-1", trigger_type: "move_out", status: "pending_settlement", dispute_reason: null },
-  { id: "st-2", contract_id: "c-2", trigger_type: "expiry", status: "pending_settlement", dispute_reason: "金额不符" },
-  { id: "st-3", contract_id: "c-3", trigger_type: "expiry", status: "settled", dispute_reason: null },
+  {
+    id: "st-1", contract_id: "c-1", trigger_type: "move_out", status: "pending_settlement", dispute_reason: null,
+    settlement_items: [
+      { item: "unpaid_receivable_summary", amount_cent: 30000, status: "listed", note: "未清应收合计（已在应收台账，不重复挂账）" },
+      { item: "deposit_disposal", amount_cent: 100000, direction: "payable", status: "payable_posted", note: "押金退还" },
+      { item: "mystery_item", amount_cent: 1, note: null },
+    ],
+  },
+  { id: "st-2", contract_id: "c-2", trigger_type: "expiry", status: "pending_settlement", dispute_reason: "金额不符", settlement_items: [] },
+  { id: "st-3", contract_id: "c-3", trigger_type: "expiry", status: "settled", dispute_reason: null, settlement_items: [] },
+  { id: "st-4", contract_id: "c-4", trigger_type: "move_out", status: "voided", dispute_reason: null, settlement_items: [] },
+  { id: "st-5", contract_id: "c-5", trigger_type: "weird_trigger", status: "weird_status", dispute_reason: null, settlement_items: [] },
 ];
 
 function envelope(items: unknown[]): Response {
@@ -18,9 +29,18 @@ function envelope(items: unknown[]): Response {
   );
 }
 
-function makeApi(): AdminApi {
-  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
+function single(data: unknown): Response {
+  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
+}
+
+type Call = { url: string; method: string; body: Record<string, unknown> | null };
+
+function makeApi(calls: Call[] = []): AdminApi {
+  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
     const url = String(input);
+    const method = (init?.method ?? "GET").toUpperCase();
+    calls.push({ url, method, body: typeof init?.body === "string" ? JSON.parse(init.body) as Record<string, unknown> : null });
+    if (method === "POST" && url.includes("/dispute")) return Promise.resolve(single({ id: "st-1", status: "pending_settlement" }));
     if (url.includes("/settlements")) return Promise.resolve(envelope(SETTLEMENTS));
     return Promise.resolve(envelope([]));
   }));
@@ -28,69 +48,136 @@ function makeApi(): AdminApi {
 }
 
 const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
+const VIEW_ONLY_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: ["merchant.portal.settlement.view"], denies: [], wildcard: false } };
 
-function renderPage() {
+function renderPage(overrides: Partial<AdminPageProps> = {}, calls: Call[] = []) {
   const props: AdminPageProps = {
-    api: makeApi(),
+    api: makeApi(calls),
     user: { id: "u1", username: "merchant", accountType: "merchant_admin" },
     setActionResult: vi.fn(),
     setGlobalError: vi.fn(),
     effectivePermissions: ADMIN_PERMS,
     navigateTo: vi.fn(),
+    ...overrides,
   };
   render(<MerchantZoneSettlementsPage {...props} />);
+  return props;
 }
 
-describe("MerchantZoneSettlementsPage KPI 聚焦视图", () => {
+describe("MerchantZoneSettlementsPage 留言提异议（M023-006）", () => {
   afterEach(() => {
     vi.unstubAllGlobals();
     window.history.replaceState({}, "", "/admin/merchant-zone/settlements");
   });
 
-  it("没有「有异议」KPI，本页不提交异议", async () => {
+  it("留言表单可见，文案写明不改状态、不暂停付款、正式异议走退租审批；没有「有异议」KPI", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("merchant-zone-settlements-summary")).toBeTruthy());
+    expect(screen.getByTestId("merchant-zone-settlement-dispute-form")).toBeTruthy();
+    const hint = screen.getByTestId("merchant-zone-settlement-dispute-hint").textContent ?? "";
+    expect(hint).toContain("不改变结算状态");
+    expect(hint).toContain("不暂停付款");
+    expect(hint).toContain("退租审批流程");
     expect(screen.queryByTestId("merchant-zone-settlements-kpi-disputed")).toBeNull();
-    expect(screen.queryByTestId("merchant-zone-settlement-dispute-form")).toBeNull();
-    expect(screen.queryByTestId("merchant-zone-settlement-dispute-submit")).toBeNull();
-    const page = screen.getByTestId("merchant-zone-settlements-page").textContent ?? "";
-    expect(page).not.toContain("有异议");
-    expect(page).not.toContain("待平台裁决");
+    expect(screen.getByTestId("merchant-zone-settlements-page").textContent).not.toContain("待平台裁决");
+  });
+
+  it("选中待结算单填写理由 → POST 到该结算单的 dispute 端点，带 reason 与 idempotency_key", async () => {
+    const calls: Call[] = [];
+    const props = renderPage({}, calls);
+    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-pick-st-2")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-2"));
+    fireEvent.change(screen.getByTestId("merchant-zone-settlement-dispute-reason"), { target: { value: "  违约金多算一期  " } });
+    fireEvent.click(screen.getByTestId("merchant-zone-settlement-dispute-submit"));
+    await waitFor(() => expect(calls.some((call) => call.method === "POST")).toBe(true));
+    const post = calls.find((call) => call.method === "POST")!;
+    expect(post.url).toContain("/api/v1/merchant-portal/settlements/st-2/dispute");
+    expect(post.body?.reason).toBe("违约金多算一期");
+    expect(String(post.body?.idempotency_key ?? "")).not.toBe("");
+    await waitFor(() => expect(props.setActionResult).toHaveBeenCalled());
+  });
+
+  it("理由为空或超过 500 字 → 不发请求并给中文提示", async () => {
+    const calls: Call[] = [];
+    renderPage({}, calls);
+    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-dispute-submit")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("merchant-zone-settlement-dispute-submit"));
+    expect(screen.getByTestId("merchant-zone-settlement-form-error").textContent).toContain("必须填写理由");
+    fireEvent.change(screen.getByTestId("merchant-zone-settlement-dispute-reason"), { target: { value: "多".repeat(501) } });
+    fireEvent.click(screen.getByTestId("merchant-zone-settlement-dispute-submit"));
+    expect(screen.getByTestId("merchant-zone-settlement-form-error").textContent).toContain("500");
+    expect(calls.some((call) => call.method === "POST")).toBe(false);
+  });
+
+  it("已结清 / 已作废的结算单不可留言（按钮禁用）", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-pick-st-3")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-3"));
+    expect((screen.getByTestId("merchant-zone-settlement-dispute-submit") as HTMLButtonElement).disabled).toBe(true);
+    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-4"));
+    expect((screen.getByTestId("merchant-zone-settlement-dispute-submit") as HTMLButtonElement).disabled).toBe(true);
+    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-1"));
+    expect((screen.getByTestId("merchant-zone-settlement-dispute-submit") as HTMLButtonElement).disabled).toBe(false);
+  });
+
+  it("没有 merchant.portal.settlement.dispute 权限 → 提交按钮禁用且不发请求", async () => {
+    const calls: Call[] = [];
+    renderPage({ effectivePermissions: VIEW_ONLY_PERMS }, calls);
+    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-dispute-submit")).toBeTruthy());
+    fireEvent.change(screen.getByTestId("merchant-zone-settlement-dispute-reason"), { target: { value: "少算" } });
+    const submit = screen.getByTestId("merchant-zone-settlement-dispute-submit") as HTMLButtonElement;
+    expect(submit.disabled).toBe(true);
+    fireEvent.click(submit);
+    expect(calls.some((call) => call.method === "POST")).toBe(false);
+  });
+});
+
+describe("MerchantZoneSettlementsPage KPI 聚焦视图", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    window.history.replaceState({}, "", "/admin/merchant-zone/settlements");
   });
 
-  it("点“未结清” → 聚焦排除已结清，点「返回全部」恢复完整页面", async () => {
+  it("「未结清」不含已结清与已作废；点开聚焦排除这两类，点「返回全部」恢复完整页面", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("merchant-zone-settlements-kpi-open")).toBeTruthy());
+    expect(screen.getByTestId("merchant-zone-settlements-kpi-open").textContent).toBe("3");
     fireEvent.click(screen.getByTestId("merchant-zone-settlements-kpi-open"));
-    expect(screen.getByTestId("merchant-zone-settlements-kpi-focus-banner").textContent).toContain("正在查看：未结清（2）");
+    expect(screen.getByTestId("merchant-zone-settlements-kpi-focus-banner").textContent).toContain("正在查看：未结清（3）");
     expect(screen.queryByTestId("merchant-zone-settlement-row-st-3")).toBeNull();
+    expect(screen.queryByTestId("merchant-zone-settlement-row-st-4")).toBeNull();
     fireEvent.click(screen.getByTestId("merchant-zone-settlements-kpi-focus-clear"));
     expect(window.location.search).not.toContain("kpi=");
     expect(screen.getByTestId("merchant-zone-settlements-summary")).toBeTruthy();
     expect(screen.getByTestId("merchant-zone-settlement-row-st-3")).toBeTruthy();
+    expect(screen.getByTestId("merchant-zone-settlement-row-st-4")).toBeTruthy();
   });
 
-  it("触发/状态列为中文，不含英文枚举码", async () => {
+  it("触发/状态列为中文：已作废有中文，未知码给中文占位，不含英文枚举码", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("merchant-zone-settlements-table")).toBeTruthy());
     const table = screen.getByTestId("merchant-zone-settlements-table").textContent ?? "";
     expect(table).toContain("退租");
     expect(table).toContain("待结算");
     expect(table).toContain("已结清");
-    expect(table).not.toContain("有异议");
-    expect(table).not.toContain("进行中");
+    expect(table).toContain("已作废");
+    expect(table).toContain("未知状态");
+    expect(table).toContain("未知触发");
+    expect(table).not.toContain("voided");
+    expect(table).not.toContain("weird_status");
+    expect(table).not.toContain("weird_trigger");
     expect(table).not.toContain("move_out");
     expect(table).not.toContain("pending_settlement");
   });
 });
 
-describe("MerchantZoneSettlementsPage 行明细抽屉（整行可点·时间中文）", () => {
+describe("MerchantZoneSettlementsPage 行明细抽屉（整行可点·结算明细数字）", () => {
   afterEach(() => {
     vi.unstubAllGlobals();
     window.history.replaceState({}, "", "/admin/merchant-zone/settlements");
   });
 
-  it("点结算单行 -> 抽屉含结算单号+中文触发/状态，合同 TraceLink 保留", async () => {
+  it("点结算单行 -> 抽屉含结算明细条目中文名与分转元金额，合同下钻指向商户可达的「我的合同」", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-row-st-1")).toBeTruthy());
     fireEvent.click(screen.getByTestId("merchant-zone-settlement-row-st-1"));
@@ -99,9 +186,16 @@ describe("MerchantZoneSettlementsPage 行明细抽屉（整行可点·时间中
     expect(text).toContain("st-1");
     expect(text).toContain("退租");
     expect(text).toContain("待结算");
+    const items = screen.getByTestId("merchant-zone-settlement-items").textContent ?? "";
+    expect(items).toContain("未清应收合计：300.00 元");
+    expect(items).toContain("押金处置：1,000.00 元");
+    expect(items).toContain("其他条目：0.01 元");
+    expect(items).not.toContain("deposit_disposal");
+    expect(items).not.toContain("mystery_item");
     expect(text).not.toContain("move_out");
     expect(text).not.toContain("pending_settlement");
-    expect(drawer.querySelector('[data-testid="trace-link-ADM-CONT-001-c-1"]')).toBeTruthy();
+    expect(drawer.querySelector('[data-testid="trace-link-ADM-MER-006-c-1"]')).toBeTruthy();
+    expect(drawer.querySelector('[data-testid="trace-link-ADM-CONT-001-c-1"]')).toBeNull();
   });
 
   it("点结算单行 -> Esc 关闭抽屉", async () => {
@@ -113,20 +207,11 @@ describe("MerchantZoneSettlementsPage 行明细抽屉（整行可点·时间中
     await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
   });
 
-  it("合同下钻 TraceLink 触发下钻、不弹抽屉", async () => {
-    const navigateTo = vi.fn();
-    const props: AdminPageProps = {
-      api: makeApi(),
-      user: { id: "u1", username: "merchant", accountType: "merchant_admin" },
-      setActionResult: vi.fn(),
-      setGlobalError: vi.fn(),
-      effectivePermissions: ADMIN_PERMS,
-      navigateTo,
-    };
-    render(<MerchantZoneSettlementsPage {...props} />);
-    await waitFor(() => expect(screen.getByTestId("trace-link-ADM-CONT-001-c-1")).toBeTruthy());
-    fireEvent.click(screen.getByTestId("trace-link-ADM-CONT-001-c-1"));
-    expect(navigateTo).toHaveBeenCalled();
+  it("合同下钻跳「我的合同」并带合同号、不弹抽屉", async () => {
+    const props = renderPage();
+    await waitFor(() => expect(screen.getAllByTestId("trace-link-ADM-MER-006-c-1").length).toBeGreaterThan(0));
+    fireEvent.click(screen.getAllByTestId("trace-link-ADM-MER-006-c-1")[0]!);
+    expect(props.navigateTo).toHaveBeenCalledWith("ADM-MER-006", "c-1");
     expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
   });
 });
diff --git a/apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx b/apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx
index 7617e532..17786bac 100644
--- a/apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx
+++ b/apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx
@@ -1,17 +1,32 @@
+// 文件版本：V0.01
 import React from "react";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
 import { AuditTrailHint } from "../components/AuditTrailHint";
 import { usePagedRows, TablePager } from "../components/TablePager";
 import { useAdminResource } from "../hooks/useAdminResource";
+import { useAdminAction } from "../hooks/useAdminAction";
+import { useSelectedRow } from "../hooks/useSelectedRow";
+import { PermissionButton } from "../security/Can";
 import { TraceLink } from "../components/TraceLink";
 import type { AdminPageProps, MerchantZoneSettlementRow } from "../types";
 import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
 
+const DISPUTE_PERMS = ["merchant.portal.settlement.dispute"];
+// 与后端 dispute_reason 列宽（500）一致；后端另有合并后长度校验。
+const REASON_MAX = 500;
+
 const TEXT = {
   title: "我的对账",
-  description: "查看本商户结算单。退租金额异议在退租审批中提出。本页只读，不改变结算状态、不暂停付款。",
+  description: "查看本商户的退租 / 到期结算单与结算明细；有疑问可留言提异议。留言不改变结算状态、不暂停付款；退租金额异议的正式处理在退租审批流程中进行。",
+  disputeHint: "留言不改变结算状态、不暂停付款；退租金额异议的正式处理在退租审批流程中进行。",
   loadFailed: "对账数据读取失败",
+  actionFailed: "留言提交失败",
+  reasonRequired: "提交留言必须填写理由。",
+  reasonTooLong: "留言不能超过 500 字。",
+  noSelection: "请先选择要留言的结算单。",
+  notDisputable: "该结算单当前状态不可留言。",
+  submit: "提交留言",
   focusClear: "清除筛选，返回全部",
 };
 
@@ -28,10 +43,52 @@ const STATUS_LABELS: Record<string, string> = {
   settled: "已结清",
   disputed: "有争议",
   disputed_litigation: "有争议，诉讼/仲裁",
+  voided: "已作废",
+};
+
+// M023-006：结算明细条目（后端 TerminationSettlementMath 的 item 键）中文名。
+const ITEM_LABELS: Record<string, string> = {
+  unpaid_receivable_summary: "未清应收合计",
+  deposit_disposal: "押金处置",
+  termination_penalty: "提前退租违约金",
 };
 
+// 未知码一律中文占位，不把英文原码显示给用户（M023-003）。
+const UNKNOWN_STATUS = "未知状态";
+const UNKNOWN_TRIGGER = "未知触发";
+const UNKNOWN_ITEM = "其他条目";
+
+// 可留言的驻留态（与后端 MerchantPortalService.DISPUTABLE_STATUSES 一致）；已结清/已作废不计入「未结清」（M023-004）。
+const DISPUTABLE = new Set(["pending_settlement", "deposit_offsetting", "arrears_collecting"]);
+const CLOSED = new Set(["settled", "voided"]);
+
+type SettlementItem = { item?: string; amountCent?: number | null; note?: string | null };
+
+function statusLabel(code: string | null | undefined): string {
+  if (!code) return "-";
+  return STATUS_LABELS[code] ?? UNKNOWN_STATUS;
+}
+
+function triggerLabel(code: string | null | undefined): string {
+  if (!code) return "-";
+  return TRIGGER_LABELS[code] ?? UNKNOWN_TRIGGER;
+}
+
+function money(cent: number | null | undefined) {
+  if (cent === undefined || cent === null || !Number.isFinite(cent)) return "-";
+  return `${new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cent / 100)} 元`;
+}
+
+function nextKey(prefix: string) {
+  return `${prefix}-${Date.now()}`;
+}
+
+function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
+  return { action, idempotencyKey: request.idempotencyKey, result };
+}
+
 // KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染结算单表对应子集。
-// URL 约定：?kpi=settlements[&subset=open]。TraceLink（合同下钻）在聚焦表内保留。
+// URL 约定：?kpi=settlements[&subset=open]。
 type KpiFocus = { subset: "all" | "open" };
 
 function readKpiFocusFromUrl(): KpiFocus | null {
@@ -46,9 +103,11 @@ function focusTitle(subset: KpiFocus["subset"]): string {
   return "结算单";
 }
 
-export function MerchantZoneSettlementsPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
+export function MerchantZoneSettlementsPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
   const [rows, setRows] = React.useState<MerchantZoneSettlementRow[]>([]);
-  // 行明细抽屉：整行可点 -> 该结算单完整字段（合同 TraceLink·触发/状态中文·开账时间中文）。
+  const { selectedId, setSelectedId, selected } = useSelectedRow(rows);
+  const [reason, setReason] = React.useState("");
+  // 行明细抽屉：整行可点 -> 该结算单完整字段（结算明细数字·触发/状态中文·开账时间中文）。
   const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
   const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
 
@@ -85,20 +144,62 @@ export function MerchantZoneSettlementsPage({ api, setGlobalError, navigateTo }:
     { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
   );
 
-  const openRows = rows.filter((row) => row.status !== "settled");
+  const { busyAction, formError, setFormError, runAction } = useAdminAction({
+    reload,
+    setGlobalError,
+    setActionResult,
+    makeResult,
+    fallbackMessage: TEXT.actionFailed,
+  });
+
+  // M023-006：留言走既有 POST /merchant-portal/settlements/{id}/dispute（只记理由，不改状态、不暂停付款）。
+  async function submitDispute() {
+    if (!selected) {
+      setFormError(TEXT.noSelection);
+      return;
+    }
+    if (!DISPUTABLE.has(selected.status ?? "")) {
+      setFormError(TEXT.notDisputable);
+      return;
+    }
+    const trimmed = reason.trim();
+    if (!trimmed) {
+      setFormError(TEXT.reasonRequired);
+      return;
+    }
+    if (trimmed.length > REASON_MAX) {
+      setFormError(TEXT.reasonTooLong);
+      return;
+    }
+    const request = { idempotencyKey: nextKey("merchant-zone-settlement-dispute"), reason: trimmed };
+    const ok = await runAction("merchant_zone_settlement_disputed", request,
+      () => api.post(`/api/v1/merchant-portal/settlements/${selected.id}/dispute`, request));
+    if (ok) setReason("");
+  }
+
+  const openRows = rows.filter((row) => !CLOSED.has(row.status ?? ""));
   const openCount = openRows.length;
   const focusRows = kpiFocus?.subset === "open" ? openRows : rows;
   const paged = usePagedRows(kpiFocus ? focusRows : rows, 10, kpiFocus ? kpiFocus.subset : "");
 
   function openSettlementDetail(row: MerchantZoneSettlementRow) {
+    const items = (row.settlementItems ?? []) as SettlementItem[];
     setDetail({
       title: `结算单明细 · ${row.id}`,
       fields: [
         { label: "结算单", value: row.id },
-        { label: "合同", value: <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.contractId}>{row.contractId ?? "-"}</TraceLink> },
-        { label: "触发", value: TRIGGER_LABELS[row.triggerType ?? ""] ?? row.triggerType ?? "-" },
-        { label: "状态", value: STATUS_LABELS[row.status ?? ""] ?? row.status ?? "-" },
-        { label: "异议理由", value: row.disputeReason ?? "-" },
+        { label: "合同", value: <TraceLink navigateTo={navigateTo} pageId="ADM-MER-006" objectId={row.contractId}>{row.contractId ?? "-"}</TraceLink> },
+        { label: "触发", value: triggerLabel(row.triggerType) },
+        { label: "状态", value: statusLabel(row.status) },
+        {
+          label: "结算明细",
+          value: items.length > 0
+            ? <ul data-testid="merchant-zone-settlement-items">{items.map((item, index) => (
+              <li key={`${item.item ?? "item"}-${index}`}>{ITEM_LABELS[item.item ?? ""] ?? UNKNOWN_ITEM}：{money(item.amountCent)}{item.note ? `（${item.note}）` : ""}</li>
+            ))}</ul>
+            : "-",
+        },
+        { label: "留言理由", value: row.disputeReason ?? "-" },
         { label: "开账时间", value: cnDate(row.createdAt) },
       ],
     });
@@ -108,17 +209,18 @@ export function MerchantZoneSettlementsPage({ api, setGlobalError, navigateTo }:
     <div className="admin-table-scroll">
       <table className="admin-table" data-testid="merchant-zone-settlements-table">
         <thead>
-          <tr><th>结算单</th><th>合同</th><th>触发</th><th>状态</th><th>异议理由</th><th>开账时间</th><th>详情</th></tr>
+          <tr><th>选择</th><th>结算单</th><th>合同</th><th>触发</th><th>状态</th><th>留言理由</th><th>开账时间</th><th>详情</th></tr>
         </thead>
         <tbody>
           {paged.pageRows.map((row) => (
-            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-zone-settlement-row-${row.id}`}
+            <tr key={row.id} className={`admin-row-clickable${row.id === selectedId ? " admin-row-selected" : ""}`} tabIndex={0} data-testid={`merchant-zone-settlement-row-${row.id}`}
               onClick={() => openSettlementDetail(row)}
               onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSettlementDetail(row); } }}>
+              <td onClick={(event) => event.stopPropagation()}><input type="radio" name="mz-settlement" aria-label={`选择此结算单 ${row.id}`} data-testid={`merchant-zone-settlement-pick-${row.id}`} checked={row.id === selectedId} onChange={() => setSelectedId(row.id)} /></td>
               <td>{row.id}</td>
-              <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.contractId}>{row.contractId ?? "-"}</TraceLink></td>
-              <td>{TRIGGER_LABELS[row.triggerType ?? ""] ?? row.triggerType ?? "-"}</td>
-              <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "-"}</td>
+              <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-MER-006" objectId={row.contractId}>{row.contractId ?? "-"}</TraceLink></td>
+              <td>{triggerLabel(row.triggerType)}</td>
+              <td>{statusLabel(row.status)}</td>
               <td>{row.disputeReason ?? "-"}</td>
               <td>{cnDate(row.createdAt)}</td>
             <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看结算单详情" onClick={(event) => { event.stopPropagation(); openSettlementDetail(row); }}>查看详情</button></td></tr>
@@ -132,7 +234,7 @@ export function MerchantZoneSettlementsPage({ api, setGlobalError, navigateTo }:
   return (
     <section data-testid="merchant-zone-settlements-page">
       <AdminPageHeader title={TEXT.title} description={TEXT.description} />
-      <AuditTrailHint event="merchant_portal.settlement.viewed" />
+      <AuditTrailHint event="merchant_portal.settlement.viewed / merchant_portal.settlement.disputed" />
       <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
         {kpiFocus ? (
           <>
@@ -148,6 +250,18 @@ export function MerchantZoneSettlementsPage({ api, setGlobalError, navigateTo }:
               <div className="admin-kpi-card admin-kpi-card--clickable"><span>结算单</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-settlements-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>本商户</small></div>
               <div className="admin-kpi-card admin-kpi-card--clickable"><span>未结清</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-settlements-kpi-open" onClick={() => enterKpiFocus("open")}>{openCount}</button><small>待结算</small></div>
             </div>
+            <div className="admin-form-row" data-testid="merchant-zone-settlement-dispute-form">
+              <label>留言理由<input data-testid="merchant-zone-settlement-dispute-reason" maxLength={REASON_MAX} value={reason} onChange={(event) => setReason(event.target.value)} placeholder="如：违约金多算一期" /></label>
+              <PermissionButton
+                permissionsState={effectivePermissions}
+                permissions={DISPUTE_PERMS}
+                data-testid="merchant-zone-settlement-dispute-submit"
+                disabled={busyAction === "merchant_zone_settlement_disputed" || !selected || !DISPUTABLE.has(selected.status ?? "")}
+                onClick={() => { void submitDispute(); }}
+              >{TEXT.submit}</PermissionButton>
+              <span className="admin-muted-action" data-testid="merchant-zone-settlement-dispute-hint">{TEXT.disputeHint}</span>
+            </div>
+            {formError ? <div className="admin-form-error" data-testid="merchant-zone-settlement-form-error">{formError}</div> : null}
             {settlementsTable}
           </>
         )}
```

## 3. 内容审计（对照定稿最小处置与现行依赖）

- 001：`BillRow.totalAmountCent` 分→元（`moneyCent`），账单金额 KPI 为本商户合计；删账单「标题」、两表「授权范围」「留痕证据」、「授权范围」KPI；行类型页内收口（不改 `types.ts`）；夹具改后端真实蛇形键并断言 100.00 元 / 25.50 元 / 合计 125.50 元。
- 002：删确认按钮与 `confirmScope`；`AuditTrailHint` 改 `merchant_service.work_order.viewed / merchant_billing.bill.viewed`；`TEXT.evidence` 改路由真实两码。固定字符串残留计数（MerchantScopePage.tsx）：{'confirmScope': 0, 'merchant_scope_confirmed': 0, 'att-merchant-scope-confirm': 0, 'merchant.portal.read': 0, 'merchant.scope.viewed': 0, 'merchant.scope_confirmed': 0, 'authorizedScope': 0, 'auditEvidenceRef': 0}（执行日志 confirm=14 为探针正则误命中 data-testid 前缀，见 B3 段说明）。
- 003：授权工作台账单状态页内字典（与 `MerchantZoneBillsPage` 同词）；工单枚举经 `formatBusinessStatus`、未知码（回退原码时）改「未知状态」；结算字典补 `voided: "已作废"`，未知状态/触发给中文占位；合同页未知码「未知状态」。测试含 viewed / weird_code / voided / weird_status / weird_trigger。
- 004：`CLOSED = (settled, voided)`，未结清 KPI 与聚焦子集同口径；测试断言 3 与聚焦排除 st-3/st-4。
- 005：对账页合同下钻 → `ADM-MER-006`（商户可达）；合同页合同号/空间/续签自、授权工作台商户号 → 纯文本；测试断言无 `trace-link-` 假按钮、`navigateTo("ADM-MER-006", "c-1")`。
- 006：明细抽屉渲染 `settlementItems`（三项闭集中文名 + 未知「其他条目」，amountCent 分→元，note）；留言表单恢复（`useSelectedRow` 选单、`useAdminAction` 提交、`PermissionButton` 按 `merchant.portal.settlement.dispute` 门控、仅三种驻留态可提交、理由 trim 且 ≤500、POST 既有端点、AdminApi 蛇形化 `idempotency_key`）；去掉旧实现的 `in_progress` 可留言态与「有异议」KPI；文案写明不改状态/不暂停付款/正式异议走退租审批。
- TDD 与类型检查证据（/home/james/audit-tmp/m023_b3r1_run.log 摘要）：
```
RED_VITEST_EXIT=1
 ❯ src/admin/pages/MerchantZoneContractsPage.test.tsx (7 tests | 3 failed) 110ms
 ❯ src/admin/pages/MerchantScopePage.test.tsx (14 tests | 6 failed) 159ms
 ❯ src/admin/pages/MerchantZoneSettlementsPage.test.tsx (10 tests | 9 failed) 5139ms
 Test Files  3 failed (3)
GREEN_VITEST_EXIT=0
 ✓ src/admin/pages/MerchantZoneContractsPage.test.tsx (7 tests) 105ms
 ✓ src/admin/pages/MerchantScopePage.test.tsx (14 tests) 145ms
 ✓ src/admin/pages/MerchantZoneSettlementsPage.test.tsx (10 tests) 165ms
 Test Files  3 passed (3)
      Tests  31 passed (31)
TSC_EXIT=0
scope page: ghost fields=0 totalAmountCent=4 confirm=14 real-events=1
raw-code fallbacks left: apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx:0 apps/web/src/admin/pages/MerchantZoneContractsPage.tsx:0 ; voided label: 1; open excludes voided: 1
trace targets:  2 apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx:pageId="ADM-MER-006";
dispute: form=1 post=1 items=1 gate=2
```
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m023_b3r1_run.log）

```
START=2026-09-20T19:51:59Z HEAD=e456ba96
check OK
BEFORE_TREE=bccb4a116bcbd5752a105a8b5009f7b62ae21626
== RED: new tests against old pages
wrote MerchantScopePage.test.tsx aec26699fe171bcad839f774b012d45e0812f70de23706efeb9829f65c98be71
wrote MerchantZoneContractsPage.test.tsx 6013ffe5085e736203603588493681b692c9c3f4bfa8ffdbc3dbd9a8833767f5
wrote MerchantZoneSettlementsPage.test.tsx 2f8b257279ca40e09f3246867dee177ff5876b464ec36f195da53e4044e61f4b
RED_VITEST_EXIT=1
 ❯ src/admin/pages/MerchantZoneContractsPage.test.tsx (7 tests | 3 failed) 110ms
     × 生命周期状态列为中文：未知码给中文占位，不含英文枚举码 9ms
     × 合同号 / 空间 / 续签自为纯文本，不渲染商户点了没反应的下钻按钮（M023-005） 8ms
     × 点合同行 -> 抽屉含合同号全量、租期中文日期、中文状态、押金与空间，无下钻假按钮 10ms
 ❯ src/admin/pages/MerchantScopePage.test.tsx (14 tests | 6 failed) 159ms
     × 金额读 total_amount_cent 分转元，账单金额 KPI 为本商户合计 9ms
     × 账单状态用账单字典：issued→已出账（非票务义「已出票」）、viewed→已查看、未知码→未知状态，无英文原码 6ms
     × 不渲染后端不存在的列，也没有不调后端的「确认授权范围」按钮 7ms
     × 接口证据写真实权限码与真实留痕事件 6ms
     × 商户号为纯文本，不渲染点了没反应的下钻按钮 8ms
     × 点账单行 → 抽屉含该行账单号、分转元金额与中文状态 10ms
 ❯ src/admin/pages/MerchantZoneSettlementsPage.test.tsx (10 tests | 9 failed) 5139ms
     × 留言表单可见，文案写明不改状态、不暂停付款、正式异议走退租审批；没有「有异议」KPI 34ms
     × 选中待结算单填写理由 → POST 到该结算单的 dispute 端点，带 reason 与 idempotency_key 1014ms
     × 理由为空或超过 500 字 → 不发请求并给中文提示 1012ms
     × 已结清 / 已作废的结算单不可留言（按钮禁用） 1013ms
     × 没有 merchant.portal.settlement.dispute 权限 → 提交按钮禁用且不发请求 1011ms
     × 「未结清」不含已结清与已作废；点开聚焦排除这两类，点「返回全部」恢复完整页面 17ms
     × 触发/状态列为中文：已作废有中文，未知码给中文占位，不含英文枚举码 7ms
     × 点结算单行 -> 抽屉含结算明细条目中文名与分转元金额，合同下钻指向商户可达的「我的合同」 11ms
     × 合同下钻跳「我的合同」并带合同号、不弹抽屉 1009ms
 ❯ src/admin/pages/MerchantScopePage.test.tsx:134:76
 ❯ src/admin/pages/MerchantScopePage.test.tsx:143:18
 ❯ src/admin/pages/MerchantScopePage.test.tsx:156:22
 ❯ src/admin/pages/MerchantScopePage.test.tsx:169:26
 ❯ src/admin/pages/MerchantScopePage.test.tsx:178:69
 ❯ src/admin/pages/MerchantScopePage.test.tsx:207:18
 ❯ src/admin/pages/MerchantZoneContractsPage.test.tsx:80:19
 ❯ src/admin/pages/MerchantZoneContractsPage.test.tsx:101:69
 ❯ src/admin/pages/MerchantZoneContractsPage.test.tsx:131:66
 ❯ src/admin/pages/MerchantZoneSettlementsPage.test.tsx:76:19
 ❯ src/admin/pages/MerchantZoneSettlementsPage.test.tsx:88:11
 ❯ src/admin/pages/MerchantZoneSettlementsPage.test.tsx:103:11
 ❯ src/admin/pages/MerchantZoneSettlementsPage.test.tsx:114:11
 ❯ src/admin/pages/MerchantZoneSettlementsPage.test.tsx:126:11
 ❯ src/admin/pages/MerchantZoneSettlementsPage.test.tsx:144:82
 ❯ src/admin/pages/MerchantZoneSettlementsPage.test.tsx:163:19
 ❯ src/admin/pages/MerchantZoneSettlementsPage.test.tsx:189:26
 ❯ src/admin/pages/MerchantZoneSettlementsPage.test.tsx:212:11
 Test Files  3 failed (3)
== GREEN: new pages + new tests
wrote MerchantScopePage.tsx c328add4b8e4767b6c21f4f9766dbdd4da41fdd05d08d26f6229853d5b7579e9
wrote MerchantZoneContractsPage.tsx 6c9a0b9a12ebc05a17ac1fc70a3c013cb7660d536a748a12d7d736a4527487a3
wrote MerchantZoneSettlementsPage.tsx 271ae798a161f18a37befb5f64b3b8a4dcede4348babfc286715116ded3dd5e8
GREEN_VITEST_EXIT=0
 ✓ src/admin/pages/MerchantZoneContractsPage.test.tsx (7 tests) 105ms
 ✓ src/admin/pages/MerchantScopePage.test.tsx (14 tests) 145ms
 ✓ src/admin/pages/MerchantZoneSettlementsPage.test.tsx (10 tests) 165ms
 Test Files  3 passed (3)
      Tests  31 passed (31)
== typecheck (apps/web tsc -b --noEmit)
TSC_EXIT=0
== post-fix probes
scope page: ghost fields=0 totalAmountCent=4 confirm=14 real-events=1
raw-code fallbacks left: apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx:0 apps/web/src/admin/pages/MerchantZoneContractsPage.tsx:0 ; voided label: 1; open excludes voided: 1
trace targets:  2 apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx:pageId="ADM-MER-006";
dispute: form=1 post=1 items=1 gate=2
other files touched: 12 (pre-existing dirt excluded below)
AFTER_TREE=1812e0f7017ccf3268183b8b94ea25521e246fc3
DIFF bytes=68654 sha256=c40950302b84891de3e3d776d1b498d1ae5b3ee815ea97853553b27e35b60735
 .../web/src/admin/pages/MerchantScopePage.test.tsx |  91 ++++++++++--
 apps/web/src/admin/pages/MerchantScopePage.tsx     | 109 +++++++-------
 .../admin/pages/MerchantZoneContractsPage.test.tsx |  58 ++++----
 .../src/admin/pages/MerchantZoneContractsPage.tsx  |  45 +++---
 .../pages/MerchantZoneSettlementsPage.test.tsx     | 157 ++++++++++++++++-----
 .../admin/pages/MerchantZoneSettlementsPage.tsx    | 144 +++++++++++++++++--
 6 files changed, 434 insertions(+), 170 deletions(-)
hunks=41 files=6
apps/web/src/admin/pages/MerchantScopePage.tsx 217 c328add4b8e4767b6c21f4f9766dbdd4da41fdd05d08d26f6229853d5b7579e9 // 文件版本：V0.01
apps/web/src/admin/pages/MerchantScopePage.test.tsx 211 aec26699fe171bcad839f774b012d45e0812f70de23706efeb9829f65c98be71 // 文件版本：V0.01
apps/web/src/admin/pages/MerchantZoneContractsPage.tsx 175 6c9a0b9a12ebc05a17ac1fc70a3c013cb7660d536a748a12d7d736a4527487a3 // 文件版本：V0.01
apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx 142 6013ffe5085e736203603588493681b692c9c3f4bfa8ffdbc3dbd9a8833767f5 // 文件版本：V0.01
apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx 272 271ae798a161f18a37befb5f64b3b8a4dcede4348babfc286715116ded3dd5e8 // 文件版本：V0.01
apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx 217 2f8b257279ca40e09f3246867dee177ff5876b464ec36f195da53e4044e61f4b // 文件版本：V0.01
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-20T19:52:13Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
