CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M038-A B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Opus 5.5（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-23T21:31:32Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。对象：XM-M038-A B3-r1 修复 diff：before tree `6675a7ac2e81512a2c3751b4352d395dc835d2b0` → after tree `d07213be15b3f7b279b12ccc8cde1817c38ba3a5`；冻结 diff `/home/james/audit-tmp/xm038a_b3r1.diff`（5483 bytes，SHA256 601db987c66600c21b116bca65dbcd06493e7342cf1491e14949f3b0289980c8；`git diff --binary` 两 tree 与冻结文件逐字节相同）。定稿 ID：XM-M038-A-001…006（findings V3.92）。
- 顺序：Grok 已先派（modules/XM-M038-A-b4-r1-grok-raw.md），本报告落盘前未读其正文。

## 1. 逐块归因（§5 闸 3）

| 文件 | 块 | ± 行数 | 归因 ID | 说明 |
|---|---|---|---|---|
| TraceLinkDrilldown.test.tsx | @@ -17,7 +17,12 makeApi | −1 ＋6 | 002 | `getPaged` 恒空 → 按同一路由表返回 `{items,total,page,size}`（含 1 行注释） |
| TraceLinkDrilldown.test.tsx | @@ -52,24 +57,14 删「我的对账合同列点击 → ADM-CONT-001」 | −11 | 001 | 整条过期用例及其后空行 |
| TraceLinkDrilldown.test.tsx | 同块「无 navigateTo 时降级纯文本」 | −2 ＋3 | 006 | `render` 取 `container`；否定断言改查任何 `[data-testid^="trace-link-"]`（含 1 行注释） |
| TraceLinkP2c.test.tsx | @@ -2,8 +2,6 导入 | −2 | 003、004 | 删用例后闲置的 `MerchantZoneContractsPage`（003）与 `MerchantScopePage`（004）导入 |
| TraceLinkP2c.test.tsx | @@ -16,7 +14,12 makeApi | −1 ＋6 | 005 | 同 002 |
| TraceLinkP2c.test.tsx | @@ -61,32 +64,6 删两条用例 | −13（003）−13（004） | 003、004 | 「我的合同：合同号/空间均可点」与「商户授权工作台：账单商户列点击」整条过期用例及空行 |

- 合计 15 ＋、43 −，全部可归因到 001…006；无 mode 变更；其余文件未动（stat 仅两文件）。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/apps/web/src/admin/pages/TraceLinkDrilldown.test.tsx b/apps/web/src/admin/pages/TraceLinkDrilldown.test.tsx
index a5a4b639..925cc2c8 100644
--- a/apps/web/src/admin/pages/TraceLinkDrilldown.test.tsx
+++ b/apps/web/src/admin/pages/TraceLinkDrilldown.test.tsx
@@ -17,7 +17,12 @@ function makeApi(routes: Record<string, unknown>): AdminApiClient {
   });
   return {
     get,
-    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
+    // XM-M038-A-002/005：治理拓扑与组织商户页已改分页取数（getPaged），桩按同一路由表返回分页形状。
+    getPaged: vi.fn((url: string) => {
+      const key = Object.keys(routes).find((r) => url.startsWith(r));
+      const items = (key ? routes[key] : []) as unknown[];
+      return Promise.resolve({ items, total: items.length, page: 1, size: 500 });
+    }),
     post: vi.fn().mockResolvedValue({}),
     put: vi.fn().mockResolvedValue({}),
     patch: vi.fn().mockResolvedValue({}),
@@ -52,24 +57,14 @@ describe("全链下钻 P1-A：源页引用字段成可点链接", () => {
     expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-002", "m-9");
   });
 
-  it("我的对账合同列点击 → navigateTo(ADM-CONT-001, contractId)", async () => {
-    const navigateTo = vi.fn();
-    const api = makeApi({
-      "/api/v1/merchant-portal/settlements": [{ id: "s-1", contractId: "c-77", status: "pending_settlement" }],
-    });
-    render(<MerchantZoneSettlementsPage {...baseProps(api, { navigateTo })} />);
-    const link = await screen.findByTestId("trace-link-ADM-CONT-001-c-77");
-    fireEvent.click(link);
-    expect(navigateTo).toHaveBeenCalledWith("ADM-CONT-001", "c-77");
-  });
-
   it("无 navigateTo 时降级纯文本（不可点）", async () => {
     const api = makeApi({
       "/api/v1/merchant-portal/settlements": [{ id: "s-1", contractId: "c-77", status: "pending_settlement" }],
     });
-    render(<MerchantZoneSettlementsPage {...baseProps(api, {})} />);
+    const { container } = render(<MerchantZoneSettlementsPage {...baseProps(api, {})} />);
     await screen.findByText("c-77");
-    expect(screen.queryByTestId("trace-link-ADM-CONT-001-c-77")).toBeNull();
+    // XM-M038-A-006：页面下钻已改指 ADM-MER-006，否定断言改查任何下钻按钮，才能在「无 navigateTo 仍出按钮」时失败。
+    expect(container.querySelector('[data-testid^="trace-link-"]')).toBeNull();
   });
 });
 
diff --git a/apps/web/src/admin/pages/TraceLinkP2c.test.tsx b/apps/web/src/admin/pages/TraceLinkP2c.test.tsx
index 8366f3df..be176c2d 100644
--- a/apps/web/src/admin/pages/TraceLinkP2c.test.tsx
+++ b/apps/web/src/admin/pages/TraceLinkP2c.test.tsx
@@ -2,8 +2,6 @@ import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { describe, expect, it, vi } from "vitest";
 
 import { MerchantZoneBillsPage } from "./MerchantZoneBillsPage";
-import { MerchantZoneContractsPage } from "./MerchantZoneContractsPage";
-import { MerchantScopePage } from "./MerchantScopePage";
 import { OrganizationMerchantPage } from "./OrganizationMerchantPage";
 import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";
 
@@ -16,7 +14,12 @@ function makeApi(routes: Record<string, unknown>): AdminApiClient {
   });
   return {
     get,
-    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
+    // XM-M038-A-002/005：治理拓扑与组织商户页已改分页取数（getPaged），桩按同一路由表返回分页形状。
+    getPaged: vi.fn((url: string) => {
+      const key = Object.keys(routes).find((r) => url.startsWith(r));
+      const items = (key ? routes[key] : []) as unknown[];
+      return Promise.resolve({ items, total: items.length, page: 1, size: 500 });
+    }),
     post: vi.fn().mockResolvedValue({}),
     put: vi.fn().mockResolvedValue({}),
     patch: vi.fn().mockResolvedValue({}),
@@ -61,32 +64,6 @@ describe("全链下钻 P2-C：商户门户源页引用可点", () => {
     expect(screen.queryByTestId("trace-link-ADM-CONT-001-m-x")).toBeNull();
   });
 
-  it("我的合同：合同号→ADM-CONT-001、空间→ADM-GOV-007 均可点", async () => {
-    const navigateTo = vi.fn();
-    const api = makeApi({
-      "/api/v1/merchant-portal/contracts": [
-        { id: "c-1", lifecycleStatus: "effective", spaceIds: ["sp-9"] },
-      ],
-    });
-    render(<MerchantZoneContractsPage {...baseProps(api, { navigateTo })} />);
-    fireEvent.click(await screen.findByTestId("trace-link-ADM-CONT-001-c-1"));
-    expect(navigateTo).toHaveBeenCalledWith("ADM-CONT-001", "c-1");
-    fireEvent.click(screen.getByTestId("trace-link-ADM-GOV-007-sp-9"));
-    expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-007", "sp-9");
-  });
-
-  it("商户授权工作台：账单商户列点击 → navigateTo(ADM-GOV-002, merchantId)", async () => {
-    const navigateTo = vi.fn();
-    const api = makeApi({
-      "/api/v1/merchant-service/work-orders": [],
-      "/api/v1/merchant-billing/bills": [
-        { id: "sb-1", merchantId: "m-5", billNo: "SB1", totalAmount: 10, status: "issued" },
-      ],
-    });
-    render(<MerchantScopePage {...baseProps(api, { navigateTo })} />);
-    fireEvent.click(await screen.findByTestId("trace-link-ADM-GOV-002-m-5"));
-    expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-002", "m-5");
-  });
 });
 
 describe("全链下钻 P2-C：组织与商户档案按 ?focus= 定位", () => {
```

## 3. 内容审计

- 001/003/004：删除的三条正是 M023-005 已改指/降纯文本的过期断言；现行行为分别由 MerchantZoneSettlementsPage.test.tsx（ADM-MER-006 下钻与点击）、MerchantZoneContractsPage.test.tsx:113-121、MerchantScopePage.test.tsx:175-180 守住，变异证据 xm038a_mut2.log（B2 两席已核）。两文件其余全链下钻用例（合同工作台商户列、审计事件反查、我的账单来源合同/非合同来源、三条定位）保留，文件整体仍是 PGM-001 的可执行证据。
- 002/005：桩只改 `getPaged`，`get` 与其余方法不变；分页形状 `{items,total,page,size}` 与 `useServerPagedRows` 所读字段一致；GREEN 下两条定位用例绿。
- 006：否定断言改查前缀选择器，覆盖任何 pageId；变异（TraceLink 无 navigateTo 也出按钮）下该用例红（xm038a_b3_run.log），修前同一变异下绿（xm038a_mut1.log）。
- 未改页面、TraceLink、AdminApp、adminRoutes、客户端、后端；未抽公共夹具；未给预部署加单测（均在 B2 禁止表内）。
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/xm038a_b3_run.log）

```
START=2026-09-23T21:29:20Z HEAD=e456ba96 origin/main=bfb17f8b
same-as-origin/main apps/web/src/admin/pages/TraceLinkDrilldown.test.tsx
same-as-origin/main apps/web/src/admin/pages/TraceLinkP2c.test.tsx
before tree=6675a7ac2e81512a2c3751b4352d395dc835d2b0
scratch at bfb17f8b
VITEST_EXIT(base)=1
     × 我的对账合同列点击 → navigateTo(ADM-CONT-001, contractId) 1011ms
     × 治理拓扑带 focusObjectId(merchantId) → 高亮该商户行 1004ms
     × 我的合同：合同号→ADM-CONT-001、空间→ADM-GOV-007 均可点 1009ms
     × 商户授权工作台：账单商户列点击 → navigateTo(ADM-GOV-002, merchantId) 1008ms
     × 带 focusObjectId(merchantId) → 高亮命中的商户档案行，未命中行不高亮 1009ms
 Test Files  2 failed (2)
      Tests  5 failed | 6 passed (11)
patched TraceLinkDrilldown.test.tsx
patched TraceLinkP2c.test.tsx
VITEST_EXIT(green)=0
 ✓ src/admin/pages/TraceLinkP2c.test.tsx (3 tests) 197ms
 ✓ src/admin/pages/TraceLinkDrilldown.test.tsx (5 tests) 299ms
 Test Files  2 passed (2)
      Tests  8 passed (8)
== 变异：TraceLink 无 navigateTo 也渲染按钮，只跑 006
VITEST_EXIT(mutant006)=1
     × 无 navigateTo 时降级纯文本（不可点） 37ms
⎯⎯⎯⎯⎯⎯⎯ Failed Tests 1 ⎯⎯⎯⎯⎯⎯⎯
      Tests  1 failed | 4 skipped (5)
== 前端全量（补丁后）
VITEST_EXIT(full)=0
 Test Files  206 passed (206)
      Tests  978 passed (978)
TSC_EXIT=0
scratch dirty after cleanup: 0
scratch removed
== 主工作区落补丁
patched TraceLinkDrilldown.test.tsx
patched TraceLinkP2c.test.tsx
after tree=d07213be15b3f7b279b12ccc8cde1817c38ba3a5
frozen diff: 5483 bytes sha256 601db987c66600c21b116bca65dbcd06493e7342cf1491e14949f3b0289980c8
 .../src/admin/pages/TraceLinkDrilldown.test.tsx    | 23 ++++++--------
 apps/web/src/admin/pages/TraceLinkP2c.test.tsx     | 35 ++++------------------
 2 files changed, 15 insertions(+), 43 deletions(-)
主工作区本轮 diff 与一次性 worktree 里验证过的 diff 是否逐字节相同：same
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-23T21:29:55Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
