CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M023 B4-r2 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T20:46:14Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M023 B3-r2 修复 diff：before tree `012effdd8c2ebbe05bd1eb6e47cef1b79ac5c64a` → after tree `144c4edd74f86cffe1c6aa92ed1fc8d3bd8b341f`；冻结 diff `/home/james/audit-tmp/m023_b3_r2.diff`（3159 bytes，SHA256 46f33f7a7d0c7b79f6a836c82cf25ee26aaba75e539109d0cd94c2f6226177cc），全文见 §2。定稿 ID：M023-007（队列「是」）。
- 顺序：Grok 已先派（modules/M023-b4-r2-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐块归因（§5 闸 3）

| 文件 | hunk | ±行数 | 归因 ID |
|---|---|---:|---|
| MerchantZoneContractsPage.test.tsx | `@@ -1,4 +1,4 @@` | 2 | 007 |
| MerchantZoneContractsPage.test.tsx | `@@ -10,6 +10,7 @@ const CONTRACTS = [` | 1 | 007 |
| MerchantZoneContractsPage.test.tsx | `@@ -87,6 +88,20 @@ describe("MerchantZoneCon` | 14 | 007 |
| MerchantZoneContractsPage.tsx | `@@ -1,4 +1,4 @@` | 2 | 007 |
| MerchantZoneContractsPage.tsx | `@@ -16,7 +16,7 @@ const TEXT = {` | 2 | 007 |

- 2 文件 5 个 hunk，逐行核对：页面＝版本标记行 + 字典键一行；测试＝版本标记行 + 夹具 drafting 一行 + 新用例一段。全部属 007；001–006 的代码行未被触碰；未改 types.ts/后端/全局字典/三份 HTML/锁。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx b/apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx
index b38f6f1c..660dd19d 100644
--- a/apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx
+++ b/apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -10,6 +10,7 @@ const CONTRACTS = [
   { id: "contract-aaaa1111", lifecycle_status: "effective", effective_date: "2026-01-01", expiry_date: "2026-12-31", deposit_cent: 100000, space_ids: ["sp-1", "sp-2"], renewed_from_contract_id: "contract-bbbb2222" },
   { id: "contract-bbbb2222", lifecycle_status: "expired", effective_date: "2025-01-01", expiry_date: "2025-12-31", deposit_cent: 50000, space_ids: [] },
   { id: "contract-cccc3333", lifecycle_status: "weird_status", effective_date: "2024-01-01", expiry_date: "2024-12-31", deposit_cent: 0, space_ids: [] },
+  { id: "contract-dddd4444", lifecycle_status: "drafting", effective_date: "2027-01-01", expiry_date: "2027-12-31", deposit_cent: 0, space_ids: [] },
 ];
 
 function envelope(items: unknown[]): Response {
@@ -87,6 +88,20 @@ describe("MerchantZoneContractsPage KPI 聚焦视图", () => {
     expect(table).not.toContain("CNY");
   });
 
+  it("后端起草态真码 drafting 显示「草拟」，不落到未知状态（M023-007）", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("merchant-zone-contract-row-contract-dddd4444")).toBeTruthy());
+    const rowText = screen.getByTestId("merchant-zone-contract-row-contract-dddd4444").textContent ?? "";
+    expect(rowText).toContain("草拟");
+    expect(rowText).not.toContain("未知状态");
+    expect(rowText).not.toContain("drafting");
+    fireEvent.click(screen.getByTestId("merchant-zone-contract-row-contract-dddd4444"));
+    const drawerText = screen.getByTestId("row-detail-drawer").textContent ?? "";
+    expect(drawerText).toContain("草拟");
+    expect(drawerText).not.toContain("未知状态");
+    expect(drawerText).not.toContain("drafting");
+  });
+
   it("租期列为中文日期，不含 ISO 原文", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("merchant-zone-contracts-table")).toBeTruthy());
diff --git a/apps/web/src/admin/pages/MerchantZoneContractsPage.tsx b/apps/web/src/admin/pages/MerchantZoneContractsPage.tsx
index c9ca5a51..876d09b9 100644
--- a/apps/web/src/admin/pages/MerchantZoneContractsPage.tsx
+++ b/apps/web/src/admin/pages/MerchantZoneContractsPage.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 import React from "react";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
@@ -16,7 +16,7 @@ const TEXT = {
 };
 
 const STATUS_LABELS: Record<string, string> = {
-  draft: "草拟",
+  drafting: "草拟",
   pending_confirmation: "待确认",
   pending_effective: "待生效",
   effective: "已生效",
```

## 3. 内容审计（对照定稿最小处置与现行后端）

- 页面：固定字符串计数 {'drafting: "草拟"': 1, 'draft: "草拟"': 0, 'amended': 0}——`drafting: "草拟"` 1 处、旧键 0 处、未为 amended 自拟名称（0 处）。其余 7 键与 `V314:15-17` CHECK 闭集逐个一致。
- 测试：夹具新增后端形状的 `lifecycle_status: "drafting"` 行；新用例断言表格行与抽屉均含「草拟」、不含「未知状态」、不含英文 `drafting`。RED 证明把键留在 `draft` 时该用例失败（见下）。既有「未知码给中文占位」用例保留（weird_status 仍走占位）。
- TDD 与类型检查证据（/home/james/audit-tmp/m023_b3r2_run.log 摘要）：
```
RED_VITEST_EXIT=1
 ❯ src/admin/pages/MerchantZoneContractsPage.test.tsx (8 tests | 1 failed) 110ms
     × 后端起草态真码 drafting 显示「草拟」，不落到未知状态（M023-007） 9ms
 ✓ src/admin/pages/MerchantScopePage.test.tsx (14 tests) 141ms
 ✓ src/admin/pages/MerchantZoneSettlementsPage.test.tsx (10 tests) 163ms
 Test Files  1 failed | 2 passed (3)
      Tests  1 failed | 31 passed (32)
GREEN_VITEST_EXIT=0
 ✓ src/admin/pages/MerchantZoneContractsPage.test.tsx (8 tests) 120ms
 ✓ src/admin/pages/MerchantScopePage.test.tsx (14 tests) 141ms
 ✓ src/admin/pages/MerchantZoneSettlementsPage.test.tsx (10 tests) 166ms
 Test Files  3 passed (3)
      Tests  32 passed (32)
TSC_EXIT=0
dict keys: drafting=1 draft=0 amended=0
other files touched: 0
```
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m023_b3r2_run.log）

```
START=2026-09-20T20:45:50Z HEAD=e456ba96
check OK
BEFORE_TREE=012effdd8c2ebbe05bd1eb6e47cef1b79ac5c64a
== RED: new test against old page
wrote MerchantZoneContractsPage.test.tsx f9a704f44313a8a83abfce7a661b0a78527f97fd8da6f9e040e357fdc52b1485
RED_VITEST_EXIT=1
 ❯ src/admin/pages/MerchantZoneContractsPage.test.tsx (8 tests | 1 failed) 110ms
     × 后端起草态真码 drafting 显示「草拟」，不落到未知状态（M023-007） 9ms
 ✓ src/admin/pages/MerchantScopePage.test.tsx (14 tests) 141ms
 ✓ src/admin/pages/MerchantZoneSettlementsPage.test.tsx (10 tests) 163ms
AssertionError: expected '…dddd44442027年1月1日 ~ 2027年12月31日-未知状态…' to contain '草拟'
 ❯ src/admin/pages/MerchantZoneContractsPage.test.tsx:95:21
 Test Files  1 failed | 2 passed (3)
      Tests  1 failed | 31 passed (32)
== GREEN: page key fixed + new test
wrote MerchantZoneContractsPage.tsx 8a932c358b95fd24fcebfada8126f006813f16130ac161317b3fd25f615dee9f
GREEN_VITEST_EXIT=0
 ✓ src/admin/pages/MerchantZoneContractsPage.test.tsx (8 tests) 120ms
 ✓ src/admin/pages/MerchantScopePage.test.tsx (14 tests) 141ms
 ✓ src/admin/pages/MerchantZoneSettlementsPage.test.tsx (10 tests) 166ms
 Test Files  3 passed (3)
      Tests  32 passed (32)
== typecheck (apps/web tsc -b --noEmit)
TSC_EXIT=0
== post-fix probes
dict keys: drafting=1 draft=0 amended=0
AFTER_TREE=144c4edd74f86cffe1c6aa92ed1fc8d3bd8b341f
other files touched: 0
DIFF bytes=3159 sha256=46f33f7a7d0c7b79f6a836c82cf25ee26aaba75e539109d0cd94c2f6226177cc
 .../src/admin/pages/MerchantZoneContractsPage.test.tsx  | 17 ++++++++++++++++-
 apps/web/src/admin/pages/MerchantZoneContractsPage.tsx  |  4 ++--
 2 files changed, 18 insertions(+), 3 deletions(-)
hunks=5 files=2
apps/web/src/admin/pages/MerchantZoneContractsPage.tsx 175 8a932c358b95fd24fcebfada8126f006813f16130ac161317b3fd25f615dee9f // 文件版本：V0.02
apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx 157 f9a704f44313a8a83abfce7a661b0a78527f97fd8da6f9e040e357fdc52b1485 // 文件版本：V0.02
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-20T20:45:58Z
```

## 5. 结论

席甲 B4-r2 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
