CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M012 B4-r2 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r2
席位：固定 Grok 外审席乙 grok-4.6/xhigh
计划：160 审计 V0.17 §4/B4、§5 闸 3
本文件路径：docs/audit-2026-09/modules/M012-b4-r2-grok-raw.md
本文件自身版本：V0.01（指定路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：FAIL。
范围归因结论：PASS。
总结论：FAIL。
以上仅为席乙本轮 B4-r2 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。B3 累计 2（历史 1 + 本次 1）不得被本票改写。席乙 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席对内容行使否决；归因不否决。

未读取：任何 `M012-b4-r2-self-raw.md`、席甲本次 B4 raw。定稿以 findings.md V2.17 顶部「当前 M012 本次重开 B2 整批定稿（双 PASS）」为准。当前队列「是」七项：M012-010–016；009 及历史 001–008 为否，不改观察。七项边界以两份 b1 raw 末尾 B2 定稿为准，不读本次 self B4。

本轮实际审查口径：两个可复核 tree 之间无过滤 binary 全文 diff；七项是否按最小边界修好；每一变更行能否归因到上述「是」ID；是否越界改后台 / types / 共享 hook 与组件源 / 全局客人标签 / 路由 / 三份业务 HTML / 锁。只用既有冻结判据，未 LOCK-AMEND。未中途修，未只审单条。审完整份 48 hunk 后才判定。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum /home/james/complex-management/docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。python3 hashlib 与 `openssl dgst -sha256` 同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger 顶部 `CRITERIA-LOCK-SHA256:` 同值。criteria-lock 版本仍 V0.11。两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M012 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／网页与验证／第1段 |
| 阶段 | B4-r2 只读审计 B3 第 2 次整批修复的「修复内容」本身 |
| 正式队列「是」（7） | M012-010、011、012、013、014、015、016 |
| 正式队列「否」 | M012-009（登记）；历史 001–008（已修，不重开） |
| B3 | 已执行累计 2 次（本轮为第 2 次），模块仍 IN_PROGRESS；尚无 B4/B5 通过 |
| before（对象类型 tree） | `0c7e7d157c2c8aa3a3580886baf01d8405993c8a` |
| after（对象类型 tree） | `d06253fa9ef48efd4abf368911981f95d518fc8b` |
| 本席自取 `git cat-file -t` | 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 0c7e7d157c2c8aa3a3580886baf01d8405993c8a d06253fa9ef48efd4abf368911981f95d518fc8b`（无路径过滤） |
| 实际差异 | 79044 bytes；SHA256 `ae56e18496e6c54b10c4e156d6625d7779a21e288ca02159b650ef4d7395e6f8`；UTF-8；6 文件；392 插入 / 142 删除；48 hunk；name-status 六处 M |
| `git diff --check` | exit 0 |
| 两树路径清单 | before 3761 / after 3761；`comm -3` 空（无增删路径） |
| 本席临时索引 `GIT_INDEX_FILE="$(mktemp -d)/index" git add -A && git write-tree` | `a886ea140d14f1222471557a87fe711c6bec0330` ≠ after。原因：当前工作区另有审计产物等未纳入本批 before/after 快照的路径。六文件 blob 与 after 全等。真实索引未改。 |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`，分支 `main` |
| findings.md / ledger.md | 本 diff `git diff --name-only -- docs/audit-2026-09/` 空 |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3`，完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |
| 后台源码 | `git diff --name-only --binary -- backend/` 空 |
| types.ts | blob `8d26e82118d6f9e607fe85e18ec638a81adc5dd4` 两树相同 |
| customer/i18n/statusLabels.ts | blob `3ac5cefeb5a3c364de707a29479ba661ec076ffa` 两树相同 |
| admin/i18n/statusLabels.ts（再导出客人表，未改源） | blob `68e19245525040b7be116356fc9249453198bdc1` 两树相同 |
| adminClient.ts / useServerPagedRows.ts / TablePager.tsx / adminRoutes.ts | 两树 blob 全等，只被 import |

六文件 blob（工作区＝after）：

| 文件 | before | after / work | 版本 | 行数 before→after |
|---|---|---|---|---|
| GovernanceTopologyPage.tsx | `1c363f479b8581416bf50d4390f622dad46c3057` | `ad3b372b59da6297b84d140e5b4c4c18e106be5e` | V0.01→V0.02 | 244→264 |
| GovernanceTopologyPage.test.tsx | `fba79864cde5e07fcdde8d6c923072f807327442` | `6236833717293dc6d37e101a96961069ccee0d99` | V0.01→V0.03 | 220→285 |
| OrganizationMerchantPage.tsx | `7d5b3ee9c5ee5f590c30401e0b02c1722efe5d94` | `06cb6e08a0fe2076d1f64be4c8cf6fe2779bc4fe` | V0.02→V0.03 | 492→512 |
| OrganizationMerchantPage.test.tsx | `823e37e36256078cc9dd7f789dc9824a5c842c40` | `5453e9d3cdb6bbe89def9636d6d35b01963f8854` | V0.02→V0.06 | 331→417 |
| SubjectBindingsPage.tsx | `ce4e76c7d45fa8ac02dbc12c4d7733016ba5fedd` | `aca4b2393c416429f2b555fd63891881dbbc8769` | V0.01→V0.02 | 251→254 |
| SubjectBindingsPage.test.tsx | `2cdcc838ba4844254e5d0de2ac08f488f5fba57a` | `8f2f1836f15674407c7e971b9f0cdefe6720f541` | V0.01→V0.03 | 217→273 |

六文件合计 1755→2005 行。测试文件版本跳跃（拓扑测试跳过 V0.02，组织测试 V0.02→V0.06）记观察，不单独作越界。

`git diff --stat --binary`：

```
 .../admin/pages/GovernanceTopologyPage.test.tsx    |  97 +++++++++++++++---
 .../web/src/admin/pages/GovernanceTopologyPage.tsx | 104 +++++++++++--------
 .../admin/pages/OrganizationMerchantPage.test.tsx  | 110 ++++++++++++++++++---
 .../src/admin/pages/OrganizationMerchantPage.tsx   | 102 +++++++++++--------
 .../src/admin/pages/SubjectBindingsPage.test.tsx   |  72 ++++++++++++--
 apps/web/src/admin/pages/SubjectBindingsPage.tsx   |  49 ++++-----
 6 files changed, 392 insertions(+), 142 deletions(-)
```

本席未改任何源文件、测试、HTML、锁、findings、ledger。未 commit/push/merge/deploy。验证：主仓只读 vitest 与 `tsc -b --noEmit`（after＝工作区六文件）。未运行浏览器 / 真实 HTTP / 后台 Maven / psql。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M012 的集合：BSM-001、BSM-003、GEN-001–018、PGM-001。本轮只用定稿 010–016 已引用的 ID。不 LOCK-AMEND。

B2 双 PASS 边界（findings V2.17 + 两份 b1 raw 末尾 V0.05，摘录执行面）：

- 只改本模块六文件及版本递增。复用已有 `getPaged` / `useServerPagedRows` / `TablePager`，只 import，不改其源。
- **010**：两页局部类型读 `merchant.projectId` 正连 `project.id`；组织项目表撤销「项目属于一个商户」。不加归属配置器，不改 types.ts/后台。历史 NULL 不可见不是本页任务。测试与 011 共同归因。
- **011**：两页商户 `project_id` 夹具、项目 `merchant_id` 可空；组织缺 `entity.binding.view` 不请求主体；绑定后端拒绝仍可见。不向 BindingRow 或其夹具捏造 `project_id`。
- **012**：导语改为「展示经营对象、运营主体、合同主体和结算主体的唯一绑定关系；变更时须选择状态并填写原因。」不新增附件上传，不改业务 HTML。
- **013**：三页 `getPaged` + `useServerPagedRows` + `TablePager`；后页可达；总数用真实 total；本页过滤/去重指标标明「本页」；关联截断写未加载而非未关联；组织 `includeArchived`/`merchantType` 随 getPaged 保留。不给后台增聚合 API，不无限抓全，不造新分页器。
- **014**：三页按对象局部中文映射；暂停≠封禁/黑名单；合法码缺中文同批补；未知码不直出英文。不改 `customer/i18n/statusLabels.ts`。
- **015**：行快捷键仅 `event.target === event.currentTarget`；子控件 Space/Enter 不被取消。
- **016**：读既有 space-types，按 `typeName` 展示，八码中文兼容；未知未识别/破折号。不改路由/字典管理。
- **009 否**：附件上传、创建绑定、归属配置器、内部 ID/TraceLink、KPI URL 键、财税守卫等不顺修。

禁止：后台 / types / 共享 hook 与组件源 / 全局客人标签 / 路由 / HTML。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary 0c7e7d157c2c8aa3a3580886baf01d8405993c8a d06253fa9ef48efd4abf368911981f95d518fc8b`

before tree = `0c7e7d157c2c8aa3a3580886baf01d8405993c8a`
after tree = `d06253fa9ef48efd4abf368911981f95d518fc8b`

字节 79044；SHA256 `ae56e18496e6c54b10c4e156d6625d7779a21e288ca02159b650ef4d7395e6f8`；UTF-8 完整原文如下。

````diff
diff --git a/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx b/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
index fba79864..62368337 100644
--- a/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
+++ b/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
@@ -1,5 +1,6 @@
-// 文件版本：V0.01
+// 文件版本：V0.03
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
+import userEvent from "@testing-library/user-event";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
 import { GovernanceTopologyPage } from "./GovernanceTopologyPage";
@@ -7,28 +8,30 @@ import { AdminApi } from "../client/adminClient";
 import type { AdminPageProps, EffectivePermissionsState } from "../types";
 
 // mock 走真实 AdminApi（stub fetch）→ unwrap 转 camelCase → 组件。platform 域 snake_case 响应。
-// M012-005：夹具只用现行闭集——主体角色 platform/project_company/management_company（V410），空间类型走 V184 字典 8 码。
+// M012-005：夹具只用现行闭集——主体角色 platform/project_company/management_company（V410），空间类型使用现行字典。
 const ENTITIES = [{ id: "org-1", entity_name: "平台经营主体", unified_social_credit_code: "ORG-1", entity_role: "management_company", status: "active" }];
 const MERCHANTS = [
-  { id: "m-1", business_entity_id: "org-1", merchant_name: "商户甲", status: "active" },
-  { id: "m-2", business_entity_id: "org-1", merchant_name: "商户乙", status: "suspended" },
+  { id: "m-1", project_id: "p-1", business_entity_id: "org-1", merchant_name: "商户甲", status: "active" },
+  { id: "m-2", project_id: "p-1", business_entity_id: "org-1", merchant_name: "商户乙", status: "suspended" },
 ];
-const PROJECTS = [{ id: "p-1", name: "项目一", code: "PJ-1", merchant_id: "m-1", venue_type: "bar", status: "active" }];
+const PROJECTS = [{ id: "p-1", name: "项目一", code: "PJ-1", merchant_id: null, venue_type: "bar", status: "active" }];
 // M012-003：后台 SpaceRecord 以 project_id 挂接项目（无 venue_id）。
 const SPACES = [{ id: "s-1", space_name: "空间甲", project_id: "p-1", floor_no: "L1", area_square_meter: 120, space_type: "shop", status: "active" }];
 
-function envelope(items: unknown[]): Response {
+function envelope(items: unknown[], total = items.length, page = 1): Response {
   return new Response(
-    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
+    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page, page_size: 500, total } }),
     { status: 200, headers: { "content-type": "application/json" } },
   );
 }
 
-function makeApi(): AdminApi {
+function makeApi(respond?: (url: string, init?: RequestInit) => Response | undefined): AdminApi {
   vi.stubGlobal(
     "fetch",
-    vi.fn().mockImplementation((input: RequestInfo | URL) => {
+    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
       const url = String(input);
+      const response = respond?.(url, init);
+      if (response) return Promise.resolve(response);
       if (url.includes("business-entities")) return Promise.resolve(envelope(ENTITIES));
       if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
       if (url.includes("projects")) return Promise.resolve(envelope(PROJECTS));
@@ -41,15 +44,17 @@ function makeApi(): AdminApi {
 
 const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
 
-function renderPage() {
+function renderPage(respond?: (url: string, init?: RequestInit) => Response | undefined, overrides: Partial<AdminPageProps> = {}) {
   const fullProps: AdminPageProps = {
-    api: makeApi(),
+    api: makeApi(respond),
     user: { id: "u1", username: "admin", accountType: "platform_admin" },
     setActionResult: vi.fn(),
     setGlobalError: vi.fn(),
     effectivePermissions: ADMIN_PERMS,
+    ...overrides,
   };
   render(<GovernanceTopologyPage {...fullProps} />);
+  return fullProps;
 }
 
 describe("GovernanceTopologyPage KPI 聚焦视图", () => {
@@ -98,12 +103,12 @@ describe("GovernanceTopologyPage KPI 聚焦视图", () => {
     expect(screen.queryByTestId("governance-topology-table")).toBeNull();
   });
 
-  it("渲染输出状态列为中文，不含原英文枚举 active/suspended", async () => {
-    renderPage();
+  it.each([["active", "已启用"], ["suspended", "暂停"]])("项目状态%s显示为%s", async (status, label) => {
+    renderPage((url) => url.includes("/projects") ? envelope([{ ...PROJECTS[0], status }]) : undefined);
     await waitFor(() => expect(screen.getByTestId("governance-topology-table")).toBeTruthy());
     const text = screen.getByTestId("governance-topology-table").textContent ?? "";
-    expect(text).toContain("有效");
-    expect(text).toContain("已封禁");
+    expect(text).toContain(label);
+    expect(text).not.toContain("已封禁");
     expect(text).not.toContain("suspended");
   });
 });
@@ -149,7 +154,7 @@ describe("GovernanceTopologyPage 行明细抽屉（拓扑表 / 空间表整行
     expect(drawer.textContent).toContain("平台经营主体");
     expect(drawer.textContent).toContain("商户甲");
     expect(drawer.textContent).toContain("项目一");
-    expect(drawer.textContent).toContain("有效");
+    expect(drawer.textContent).toContain("已启用");
     expect(drawer.textContent).toContain("酒吧");
     expect(drawer.textContent).not.toContain("active");
   });
@@ -218,3 +223,63 @@ describe("GovernanceTopologyPage 无 entity.binding.view 角色不请求主体
     expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
   });
 });
+
+
+describe("M012 本次归并回归", () => {
+  afterEach(() => { vi.unstubAllGlobals(); window.history.replaceState({}, "", "/admin/governance/topology"); });
+
+  it("商户按真实 project_id 归属当前综合体，项目没有反向 merchant_id 也能显示", async () => {
+    renderPage();
+    expect((await screen.findByTestId("governance-topology-row-m-2-p-1")).textContent).toContain("项目一");
+  });
+
+  it("现有自定义空间类型按字典中文名称展示", async () => {
+    renderPage((url) => url.includes("/space-types")
+      ? envelope([{ id: "type-kiosk", type_code: "kiosk", type_name: "一体机位", status: "active" }])
+      : url.includes("/spaces") ? envelope([{ ...SPACES[0], space_type: "kiosk" }]) : undefined);
+    const row = await screen.findByTestId("governance-space-row-s-1");
+    expect(row.textContent).toContain("一体机位");
+    expect(row.textContent).not.toContain("kiosk");
+  });
+
+  it.each(["merchants", "spaces"])("超过默认500条的%s能看到总数并读取第二页", async (resource) => {
+    const rows = Array.from({ length: 501 }, (_, i) => resource === "merchants"
+      ? { ...MERCHANTS[0], id: "m-page-" + i, merchant_name: "分页商户" + i }
+      : { ...SPACES[0], id: "s-page-" + i, space_name: "分页空间" + i });
+    renderPage((url) => {
+      if (!url.includes("/" + resource)) return undefined;
+      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
+      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
+    });
+    await waitFor(() => expect(screen.getByTestId("governance-topology-kpi-" + resource).textContent).toBe("501"));
+    fireEvent.click(screen.getByTestId("governance-" + resource + "-pager-next"));
+    const last = resource === "merchants" ? "governance-topology-row-m-page-500-p-1" : "governance-space-row-s-page-500";
+    expect(await screen.findByTestId(last)).toBeTruthy();
+  });
+
+  it("商户来源按钮可以用Enter下钻，行自身Enter仍打开详情", async () => {
+    const navigateTo = vi.fn();
+    renderPage(undefined, { navigateTo });
+    const button = await screen.findByTestId("trace-link-ADM-GOV-002-m-1");
+    button.focus();
+    await userEvent.setup().keyboard("{Enter}");
+    expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-002", "m-1");
+    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
+    fireEvent.keyDown(screen.getByTestId("governance-topology-row-m-1-p-1"), { key: "Enter" });
+    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
+  });
+  it("主体资料在第二页时显示未加载，翻页后呈现真实主体", async () => {
+    const rows = Array.from({ length: 501 }, (_, i) => ({ ...ENTITIES[0], id: "org-page-" + i, entity_name: "经营主体" + i }));
+    renderPage((url) => {
+      if (url.includes("/merchants")) return envelope([{ ...MERCHANTS[0], business_entity_id: "org-page-500" }]);
+      if (!url.includes("/business-entities")) return undefined;
+      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
+      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
+    });
+    const row = await screen.findByTestId("governance-topology-row-m-1-p-1");
+    expect(row.textContent).toContain("主体资料未加载");
+    fireEvent.click(screen.getByTestId("governance-entities-pager-next"));
+    await waitFor(() => expect(screen.getByTestId("governance-topology-row-m-1-p-1").textContent).toContain("经营主体500"));
+  });
+
+});
diff --git a/apps/web/src/admin/pages/GovernanceTopologyPage.tsx b/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
index 1c363f47..ad3b372b 100644
--- a/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
+++ b/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
@@ -1,31 +1,42 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
 import { AuditTrailHint } from "../components/AuditTrailHint";
 import { useAdminResource } from "../hooks/useAdminResource";
+import { useServerPagedRows } from "../hooks/useServerPagedRows";
+import { TablePager } from "../components/TablePager";
 import { hasPermissions } from "../security/PermissionGate";
 import type { AdminPageProps, BusinessEntityRow, GovernanceProjectRow, GovernanceSpaceRow, MerchantAdmissionRow } from "../types";
 import { formatBusinessStatus } from "../i18n/statusLabels";
 import { TraceLink } from "../components/TraceLink";
 import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
 
-// M012-004：空间类型字典 8 码（V184 space_type_dictionary 种子，type_name 中文）；PGM-001 零英文，页面不直出 type_code。
+type MerchantRow = MerchantAdmissionRow & { projectId?: string | null };
+type SpaceTypeRow = { typeCode: string; typeName: string };
+const PROJECT_STATUS_LABELS: Record<string, string> = { draft: "草稿", pending_activation: "待启用", active: "已启用", suspended: "暂停", terminated: "终止", archived: "归档" };
+const ORGANIZATION_STATUS_LABELS: Record<string, string> = { pending_admission: "待准入", active: "正常", suspended: "暂停", terminated: "终止", archived: "归档", blacklisted: "黑名单" };
+function formatTopologyStatus(project: GovernanceProjectRow | undefined, merchant: MerchantRow) {
+  const code = project?.status ?? merchant.status;
+  return code ? (project ? PROJECT_STATUS_LABELS : ORGANIZATION_STATUS_LABELS)[code] ?? "未识别状态" : "—";
+}
+
+// M012-016：字典显示名优先；保留原有中文种子作为兼容显示。
 const SPACE_TYPE_LABELS: Record<string, string> = { building: "楼栋", floor: "楼层", zone: "区块/区域", shop: "商铺/铺位", public_area: "公共区域", parking: "车位/车库", booth: "摊位/展位", other: "其他" };
-function formatSpaceType(code: string | null | undefined) {
-  return code ? SPACE_TYPE_LABELS[code] ?? formatBusinessStatus(code) : "—";
+function formatSpaceType(code: string | null | undefined, types: SpaceTypeRow[]) {
+  return code ? types.find((type) => type.typeCode === code)?.typeName || SPACE_TYPE_LABELS[code] || "未识别类型" : "—";
 }
 // M012-004：空间状态闭集 active/unavailable（M009-036）页内映射「可用/不可用」，不直出英文。
 const SPACE_STATUS_LABELS: Record<string, string> = { active: "可用", unavailable: "不可用" };
 function formatSpaceStatus(code: string | null | undefined) {
-  return code ? SPACE_STATUS_LABELS[code] ?? formatBusinessStatus(code) : "—";
+  return code ? SPACE_STATUS_LABELS[code] ?? "未识别状态" : "—";
 }
 
 const TEXT = {
   title: "集团/综合体拓扑",
   description: "展示集团、综合体、商户、项目和空间点位之间的治理关系，作为权限、合同、结算和运营责任归属的入口。",
-  evidence: "接口证据：读取 /api/v1/platform/business-entities、/api/v1/platform/merchants、/api/v1/platform/projects、/api/v1/platform/spaces。",
+  evidence: "接口证据：读取 /api/v1/platform/business-entities、/api/v1/platform/merchants、/api/v1/platform/projects、/api/v1/platform/spaces、/api/v1/platform/space-types。",
   boundary: "集团、综合体、商户、项目和空间点位必须用同一套拓扑口径承接权限、合同、结算和审计；拓扑页只展示关系，不做主体变更。",
   loadFailed: "治理拓扑读取失败",
   focusClear: "清除筛选，返回全部",
@@ -56,10 +67,26 @@ function kpiFocusTitle(focus: KpiFocus): string {
 }
 
 export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, navigateTo, effectivePermissions }: AdminPageProps) {
-  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
-  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
-  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
-  const [spaces, setSpaces] = React.useState<GovernanceSpaceRow[]>([]);
+  const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);
+  const entityRows = useServerPagedRows(
+    (page, size) => canEntityView
+      ? api.getPaged<BusinessEntityRow>("/api/v1/platform/business-entities?page=" + page + "&size=" + size)
+      : Promise.resolve({ items: [] as BusinessEntityRow[], total: 0, page: 1, size }),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const merchantRows = useServerPagedRows(
+    (page, size) => api.getPaged<MerchantRow>("/api/v1/platform/merchants?page=" + page + "&size=" + size),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const projectRows = useServerPagedRows(
+    (page, size) => api.getPaged<GovernanceProjectRow>("/api/v1/platform/projects?page=" + page + "&size=" + size),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const spaceRows = useServerPagedRows(
+    (page, size) => api.getPaged<GovernanceSpaceRow>("/api/v1/platform/spaces?page=" + page + "&size=" + size),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const entities = entityRows.rows;
+  const merchants = merchantRows.rows;
+  const projects = projectRows.rows;
+  const spaces = spaceRows.rows;
+  const [spaceTypes, setSpaceTypes] = React.useState<SpaceTypeRow[]>([]);
   const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
   // 行明细抽屉：拓扑关系表 / 空间点位表整行可点 → 该行完整字段（枚举中文·商户可溯源）。
   const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
@@ -87,24 +114,14 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
     (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
   }
 
-  const { loading, error, reload } = useAdminResource(
-    async () => {
-      // 跨域副资源不请求（根因修）：主体端点需 entity.binding.view，本页 gate 不含之——
-      // 缺该权限角色 403 会 reject 整个 Promise.all 致整页白屏；缺权限则不请求、主体层降级为空。
-      const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);
-      const [nextEntities, nextMerchants, nextProjects, nextSpaces] = await Promise.all([
-        canEntityView ? api.get<BusinessEntityRow[]>("/api/v1/platform/business-entities") : Promise.resolve([] as BusinessEntityRow[]),
-        api.get<MerchantAdmissionRow[]>("/api/v1/platform/merchants"),
-        api.get<GovernanceProjectRow[]>("/api/v1/platform/projects"),
-        api.get<GovernanceSpaceRow[]>("/api/v1/platform/spaces"),
-      ]);
-      setEntities(nextEntities);
-      setMerchants(nextMerchants);
-      setProjects(nextProjects);
-      setSpaces(nextSpaces);
-    },
-    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
+  const dictionary = useAdminResource(
+    async () => setSpaceTypes(await api.get<SpaceTypeRow[]>("/api/v1/platform/space-types")),
+    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed },
   );
+  const resources = [entityRows, merchantRows, projectRows, spaceRows, dictionary];
+  const loading = resources.some((resource) => resource.loading);
+  const error = resources.find((resource) => resource.error)?.error ?? null;
+  const reload = () => resources.forEach((resource) => { void resource.reload(); });
 
   const projectIds = new Set(projects.map((project) => project.id));
   // M012-003：后台 SpaceRecord 以 projectId 挂接项目（无 venueId；types.ts 的 venueId 为旧臆造字段）。
@@ -126,17 +143,17 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
   const isFocused = (merchantId?: string, projectId?: string) =>
     !!focusObjectId && (focusObjectId === merchantId || focusObjectId === projectId);
 
-  function openTopologyDetail(entity: BusinessEntityRow | undefined, merchant: MerchantAdmissionRow, project: GovernanceProjectRow | undefined) {
+  function openTopologyDetail(entity: BusinessEntityRow | undefined, merchant: MerchantRow, project: GovernanceProjectRow | undefined) {
     setDetail({
       title: `拓扑关系 · ${display(merchant.merchantName)}`,
       fields: [
-        { label: "经营主体", value: display(entity?.entityName) },
+        { label: "经营主体", value: entity ? display(entity.entityName) : merchant.businessEntityId ? "主体资料未加载" : "-" },
         { label: "统一社会信用代码", value: display(entity?.unifiedSocialCreditCode) },
         { label: "商户", value: merchant.id ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={merchant.id}>{display(merchant.merchantName)}</TraceLink> : display(merchant.merchantName) },
         { label: "项目", value: display(project?.name) },
         { label: "项目编码", value: display(project?.code) },
         { label: "业态", value: formatBusinessStatus(project?.venueType) },
-        { label: "状态", value: formatBusinessStatus(project?.status ?? merchant.status) },
+        { label: "状态", value: formatTopologyStatus(project, merchant) },
       ],
     });
   }
@@ -149,7 +166,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
         { label: "所属项目", value: display(project?.name ?? space.projectId) },
         { label: "楼层", value: display(space.floorNo) },
         { label: "面积(㎡)", value: display(space.areaSquareMeter) },
-        { label: "空间类型", value: formatSpaceType(space.spaceType) },
+        { label: "空间类型", value: formatSpaceType(space.spaceType, spaceTypes) },
         { label: "状态", value: formatSpaceStatus(space.status) },
       ],
     });
@@ -163,7 +180,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
           <thead><tr><th>经营主体</th><th>统一社会信用代码</th><th>商户</th><th>项目</th><th>项目编码</th><th>业态</th><th>状态</th><th>详情</th></tr></thead>
           <tbody>{merchants.map((merchant) => {
             const entity = entities.find((item) => item.id === merchant.businessEntityId);
-            const merchantProjects = projects.filter((project) => project.merchantId === merchant.id);
+            const merchantProjects = projects.filter((project) => project.id === merchant.projectId);
             const rows = merchantProjects.length ? merchantProjects : [undefined];
             return rows.map((project, index) => (
               <tr key={`${merchant.id}-${project?.id ?? "no-project"}-${index}`}
@@ -172,18 +189,20 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
                 className={`admin-row-clickable${isFocused(merchant.id, project?.id) ? " admin-row-selected" : ""}`}
                 tabIndex={0}
                 onClick={() => openTopologyDetail(entity, merchant, project)}
-                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openTopologyDetail(entity, merchant, project); } }}>
-                <td>{display(entity?.entityName)}</td>
+                onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openTopologyDetail(entity, merchant, project); } }}>
+                <td>{entity ? display(entity.entityName) : merchant.businessEntityId ? "主体资料未加载" : "-"}</td>
                 <td>{display(entity?.unifiedSocialCreditCode)}</td>
                 <td onClick={(event) => event.stopPropagation()}>{merchant.id ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={merchant.id}>{display(merchant.merchantName)}</TraceLink> : display(merchant.merchantName)}</td>
                 <td>{display(project?.name)}</td>
                 <td>{display(project?.code)}</td>
                 <td>{formatBusinessStatus(project?.venueType)}</td>
-                <td>{formatBusinessStatus(project?.status ?? merchant.status)}</td>
+                <td>{formatTopologyStatus(project, merchant)}</td>
               <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看拓扑关系详情" onClick={(event) => { event.stopPropagation(); openTopologyDetail(entity, merchant, project); }}>查看详情</button></td></tr>
             ));
           })}</tbody>
         </table>
+        <TablePager testid="governance-merchants-pager" page={merchantRows.page} pageCount={merchantRows.pageCount} total={merchantRows.total} onPrev={merchantRows.prev} onNext={merchantRows.next} />
+        {canEntityView && entityRows.pageCount > 1 ? <><span>经营主体资料分页（用于关系表）</span><TablePager testid="governance-entities-pager" page={entityRows.page} pageCount={entityRows.pageCount} total={entityRows.total} onPrev={entityRows.prev} onNext={entityRows.next} /></> : null}
       </div>
     </>
   );
@@ -197,14 +216,15 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
           <tbody>{spaces.map((space) => {
             const project = projects.find((item) => item.id === space.projectId);
             const spaceFocused = !!focusObjectId && (focusObjectId === space.id || focusObjectId === space.projectId);
-            return <tr key={space.id} data-testid={`governance-space-row-${space.id}`} className={`admin-row-clickable${spaceFocused ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openSpaceDetail(space, project)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSpaceDetail(space, project); } }}><td>{display(space.spaceName)}</td><td>{display(project?.name ?? space.projectId)}</td><td>{display(space.floorNo)}</td><td>{display(space.areaSquareMeter)}</td><td>{formatSpaceType(space.spaceType)}</td><td>{formatSpaceStatus(space.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看空间点位详情" onClick={(event) => { event.stopPropagation(); openSpaceDetail(space, project); }}>查看详情</button></td></tr>;
+            return <tr key={space.id} data-testid={`governance-space-row-${space.id}`} className={`admin-row-clickable${spaceFocused ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openSpaceDetail(space, project)} onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openSpaceDetail(space, project); } }}><td>{display(space.spaceName)}</td><td>{display(project?.name ?? space.projectId)}</td><td>{display(space.floorNo)}</td><td>{display(space.areaSquareMeter)}</td><td>{formatSpaceType(space.spaceType, spaceTypes)}</td><td>{formatSpaceStatus(space.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看空间点位详情" onClick={(event) => { event.stopPropagation(); openSpaceDetail(space, project); }}>查看详情</button></td></tr>;
           })}</tbody>
         </table>
+        <TablePager testid="governance-spaces-pager" page={spaceRows.page} pageCount={spaceRows.pageCount} total={spaceRows.total} onPrev={spaceRows.prev} onNext={spaceRows.next} />
       </div>
     </>
   );
 
-  const focusCount = kpiFocus ? (kpiFocus.table === "spaces" ? spaces.length : merchants.length) : 0;
+  const focusCount = kpiFocus ? (kpiFocus.table === "spaces" ? spaceRows.total : merchantRows.total) : 0;
 
   return (
     <section data-testid="governance-topology-page">
@@ -212,7 +232,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
       <TechDetails><div className="admin-generic-evidence" data-testid="governance-topology-evidence">{TEXT.evidence}</div></TechDetails>
       <AuditTrailHint event="governance_topology.viewed" />
       <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
-        {entities.length === 0 && merchants.length === 0 && projects.length === 0 && spaces.length === 0 ? (
+        {entityRows.total === 0 && merchantRows.total === 0 && projectRows.total === 0 && spaceRows.total === 0 ? (
           <div className="admin-state admin-empty" data-testid="governance-topology-empty">暂无治理拓扑数据</div>
         ) : kpiFocus ? (
           <>
@@ -225,10 +245,10 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
         ) : (
           <>
             <div className="admin-kpi-grid" data-testid="governance-topology-summary">
-              <div className="admin-kpi-card"><span>经营主体</span><strong>{entities.length}</strong><small>主体口径</small></div>
-              <div className="admin-kpi-card admin-kpi-card--clickable"><span>商户</span><button type="button" className="admin-kpi-value" data-testid="governance-topology-kpi-merchants" onClick={() => enterKpiFocus("merchants")}>{merchants.length}</button><small>商户档案</small></div>
-              <div className="admin-kpi-card"><span>项目</span><strong>{projects.length}</strong><small>门店/场馆</small></div>
-              <div className="admin-kpi-card admin-kpi-card--clickable"><span>空间点位</span><button type="button" className="admin-kpi-value" data-testid="governance-topology-kpi-spaces" onClick={() => enterKpiFocus("spaces")}>{spaces.length}</button><small>{attachedSpaces} 个已挂接</small></div>
+              <div className="admin-kpi-card"><span>经营主体</span><strong>{entityRows.total}</strong><small>主体口径</small></div>
+              <div className="admin-kpi-card admin-kpi-card--clickable"><span>商户</span><button type="button" className="admin-kpi-value" data-testid="governance-topology-kpi-merchants" onClick={() => enterKpiFocus("merchants")}>{merchantRows.total}</button><small>商户档案</small></div>
+              <div className="admin-kpi-card"><span>项目</span><strong>{projectRows.total}</strong><small>门店/场馆</small></div>
+              <div className="admin-kpi-card admin-kpi-card--clickable"><span>空间点位</span><button type="button" className="admin-kpi-value" data-testid="governance-topology-kpi-spaces" onClick={() => enterKpiFocus("spaces")}>{spaceRows.total}</button><small>本页 {attachedSpaces} 个已挂接</small></div>
             </div>
 
             <div className="admin-muted-action" data-testid="governance-topology-boundary">{TEXT.boundary}</div>
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
index 823e37e3..5453e9d3 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
@@ -1,5 +1,6 @@
-// 文件版本：V0.02
+// 文件版本：V0.06
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
+import userEvent from "@testing-library/user-event";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
 import { OrganizationMerchantPage } from "./OrganizationMerchantPage";
@@ -12,27 +13,29 @@ const ENTITIES = [
   { id: "org-1", entity_name: "平台经营主体", entity_role: "management_company", unified_social_credit_code: "91330100MA27XW8H", legal_representative: "张三", bank_account_ref: "vault://demo", status: "active", created_at: "2026-07-01T00:00:00Z" },
 ];
 const MERCHANTS = [
-  { id: "m-1", merchant_name: "正常商户甲", merchant_type: "catering", business_entity_id: "org-1", contact_name: "联系人甲", contact_phone: "13800000001", mall_block_code: "1F-A", area_square_meter: 128.5, leased_area_square_meter: 120, cooperation_form: "lease", status: "active" },
-  { id: "m-2", merchant_name: "停用商户乙", contact_name: "联系人乙", status: "suspended" },
+  { id: "m-1", project_id: "p-1", merchant_name: "正常商户甲", merchant_type: "catering", business_entity_id: "org-1", contact_name: "联系人甲", contact_phone: "13800000001", mall_block_code: "1F-A", area_square_meter: 128.5, leased_area_square_meter: 120, cooperation_form: "lease", status: "active" },
+  { id: "m-2", project_id: "p-1", merchant_name: "停用商户乙", contact_name: "联系人乙", status: "suspended" },
   // M012-008：已存档商户（行内动作为取消存档/申请删除下拉）。status 取 suspended 以不改变「正常商户（1）」断言。
-  { id: "m-3", merchant_name: "已存档商户丙", contact_name: "联系人丙", status: "suspended", archived_at: "2026-07-01T00:00:00Z", delete_status: "none" },
+  { id: "m-3", project_id: "p-1", merchant_name: "已存档商户丙", contact_name: "联系人丙", status: "suspended", archived_at: "2026-07-01T00:00:00Z", delete_status: "none" },
 ];
 const PROJECTS = [
-  { id: "p-1", name: "项目一", code: "PJ-1", merchant_id: "m-1", venue_type: "bar", floor: "1F", capacity: 80, status: "active" },
+  { id: "p-1", name: "项目一", code: "PJ-1", merchant_id: null, venue_type: "bar", floor: "1F", capacity: 80, status: "active" },
 ];
 
-function envelope(items: unknown[]): Response {
+function envelope(items: unknown[], total = items.length, page = 1): Response {
   return new Response(
-    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
+    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page, page_size: 500, total } }),
     { status: 200, headers: { "content-type": "application/json" } },
   );
 }
 
-function makeApi(): AdminApi {
+function makeApi(respond?: (url: string, init?: RequestInit) => Response | undefined): AdminApi {
   vi.stubGlobal(
     "fetch",
     vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
       const url = String(input);
+      const response = respond?.(url, init);
+      if (response) return Promise.resolve(response);
       // 写路径（创建/存档/删除申请）默认返回成功信封；失败场景由用例 mockImplementationOnce 覆盖。
       if (init?.method === "POST") return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "m-new" } }), { status: 200, headers: { "content-type": "application/json" } }));
       if (url.includes("business-entities")) return Promise.resolve(envelope(ENTITIES));
@@ -46,13 +49,14 @@ function makeApi(): AdminApi {
 
 const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
 
-function renderPage() {
+function renderPage(respond?: (url: string, init?: RequestInit) => Response | undefined, overrides: Partial<AdminPageProps> = {}) {
   const fullProps: AdminPageProps = {
-    api: makeApi(),
+    api: makeApi(respond),
     user: { id: "u1", username: "admin", accountType: "platform_admin" },
     setActionResult: vi.fn(),
     setGlobalError: vi.fn(),
     effectivePermissions: ADMIN_PERMS,
+    ...overrides,
   };
   render(<OrganizationMerchantPage {...fullProps} />);
   return fullProps;
@@ -60,7 +64,7 @@ function renderPage() {
 
 // 聚焦视图断言：只剩商户表（active 行），其余表/表单/KPI 卡全隐藏，横幅在场。
 function expectActiveMerchantsFocusView() {
-  expect(screen.getByTestId("organization-kpi-focus-banner").textContent).toContain("正在查看：正常商户（1）");
+  expect(screen.getByTestId("organization-kpi-focus-banner").textContent).toContain("正在查看：本页正常商户（1）");
   expect(screen.getByTestId("organization-merchant-row-m-1")).toBeTruthy();
   expect(screen.queryByTestId("organization-merchant-row-m-2")).toBeNull();
   expect(screen.queryByTestId("organization-entities-table")).toBeNull();
@@ -139,7 +143,8 @@ describe("OrganizationMerchantPage KPI 聚焦视图（带参重进本页）", ()
     const projectsText = screen.getByTestId("organization-projects-table").textContent ?? "";
     expect(projectsText).toContain("项目一");
     expect(projectsText).toContain("PJ-1");
-    expect(projectsText).toContain("m-1");
+    expect(projectsText).not.toContain("m-1");
+    expect(screen.getByTestId("organization-merchant-row-m-1").textContent).toContain("项目一");
     // M012-004：业态列中文（PGM-001 零英文），不再断言英文码。
     expect(projectsText).toContain("酒吧");
     expect(projectsText).not.toMatch(/\bbar\b/);
@@ -329,3 +334,84 @@ describe("OrganizationMerchantPage 存档/删除写路径（M012-008）", () =>
     await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.any(String)));
   });
 });
+
+
+describe("M012 本次组织档案回归", () => {
+  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); window.history.replaceState({}, "", "/admin/governance/organizations"); });
+
+  it("缺主体查看权仍展示商户且不请求主体接口", async () => {
+    renderPage(undefined, { effectivePermissions: { status: "ready", value: { grants: ["merchant.profile.view", "governance.project.view"], denies: [], wildcard: false } } });
+    expect(await screen.findByTestId("organization-merchant-row-m-1")).toBeTruthy();
+    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).includes("/business-entities"))).toBe(false);
+  });
+
+  it("暂停、待准入和黑名单是不同的中文商户状态", async () => {
+    renderPage((url) => url.includes("/merchants")
+      ? envelope([{ ...MERCHANTS[0], status: "pending_admission" }, MERCHANTS[1], { ...MERCHANTS[2], status: "blacklisted" }])
+      : undefined);
+    expect((await screen.findByTestId("organization-merchant-row-m-1")).textContent).toContain("待准入");
+    expect(screen.getByTestId("organization-merchant-row-m-2").textContent).toContain("暂停");
+    expect(screen.getByTestId("organization-merchant-row-m-3").textContent).toContain("黑名单");
+    expect(screen.getByTestId("organization-merchants-table").textContent).not.toContain("已封禁");
+  });
+
+  it("分页保留商户归档和业态筛选参数，后页商户可达", async () => {
+    const rows = Array.from({ length: 501 }, (_, i) => ({ ...MERCHANTS[0], id: "m-page-" + i, merchant_name: "分页商户" + i }));
+    renderPage((url) => {
+      if (!url.includes("/merchants")) return undefined;
+      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
+      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
+    });
+    await screen.findByTestId("organization-merchant-row-m-page-0");
+    fireEvent.click(screen.getByTestId("organization-merchants-pager-next"));
+    expect(await screen.findByTestId("organization-merchant-row-m-page-500")).toBeTruthy();
+    const pageTwoUrl = vi.mocked(fetch).mock.calls.map(([url]) => String(url)).find(url => url.includes("/merchants") && url.includes("page=2"));
+    expect(pageTwoUrl).toContain("includeArchived=false");
+    expect(screen.getByTestId("organization-merchants-pager-status").textContent).toContain("共 501 条");
+    fireEvent.click(screen.getByTestId("merchant-show-archived").querySelector("input")!);
+    fireEvent.change(await screen.findByTestId("merchant-type-filter"), { target: { value: "retail" } });
+    await waitFor(() => {
+      const urls = vi.mocked(fetch).mock.calls.map(([url]) => String(url)).filter(url => url.includes("/merchants"));
+      const params = new URL(urls.at(-1)!, "http://localhost").searchParams;
+      expect(params.get("includeArchived")).toBe("true");
+      expect(params.get("merchantType")).toBe("retail");
+      expect(params.get("page")).toBe("1");
+    });
+    await screen.findByTestId("organization-merchant-row-m-page-0");
+    fireEvent.click(screen.getByTestId("organization-merchants-pager-next"));
+    await screen.findByTestId("organization-merchant-row-m-page-500");
+    const urls = vi.mocked(fetch).mock.calls.map(([url]) => String(url)).filter(url => url.includes("/merchants"));
+    expect(urls.at(-1)).toContain("merchantType=retail");
+    expect(urls.at(-1)).toContain("includeArchived=true");
+
+  });
+
+  it("商户存档按钮用Enter触发原操作而不打开行抽屉", async () => {
+    const prompt = vi.spyOn(window, "prompt").mockReturnValue(null);
+    renderPage();
+    const button = await screen.findByTestId("merchant-archive-m-1");
+    button.focus();
+    await userEvent.setup().keyboard("{Enter}");
+    expect(prompt).toHaveBeenCalledWith("存档（下线）理由：");
+    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
+  });
+  it("存档最后一页的唯一商户后回到仍有记录的第一页", async () => {
+    let archived = false;
+    const rows = Array.from({ length: 501 }, (_, i) => ({ ...MERCHANTS[0], id: "m-last-" + i }));
+    renderPage((url, init) => {
+      if (init?.method === "POST" && url.includes("/archive")) { archived = true; return envelope([]); }
+      if (!url.includes("/merchants")) return undefined;
+      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
+      const current = archived ? rows.slice(0, 500) : rows;
+      return envelope(current.slice((page - 1) * 500, page * 500), current.length, page);
+    });
+    await screen.findByTestId("organization-merchant-row-m-last-0");
+    fireEvent.click(screen.getByTestId("organization-merchants-pager-next"));
+    await screen.findByTestId("organization-merchant-row-m-last-500");
+    vi.spyOn(window, "prompt").mockReturnValue("结束经营");
+    fireEvent.click(screen.getByTestId("merchant-archive-m-last-500"));
+    await waitFor(() => expect(screen.getByTestId("organization-merchants-pager-status").textContent).toContain("第 1 / 1 页"));
+    expect(await screen.findByTestId("organization-merchant-row-m-last-0")).toBeTruthy();
+  });
+
+});
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
index 7d5b3ee9..06cb6e08 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
@@ -1,19 +1,29 @@
-// 文件版本：V0.02
+// 文件版本：V0.03
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
 import { AuditTrailHint, auditTraceField } from "../components/AuditTrailHint";
-import { useAdminResource } from "../hooks/useAdminResource";
+import { useServerPagedRows } from "../hooks/useServerPagedRows";
+import { TablePager } from "../components/TablePager";
 import { hasPermissions } from "../security/PermissionGate";
 import type { AdminPageProps, BusinessEntityRow, GovernanceProjectRow, MerchantAdmissionRow } from "../types";
 import { PermissionButton } from "../security/Can";
 import { RowActions, type RowAction } from "../components/RowActions";
 import { formatBusinessStatus } from "../i18n/statusLabels";
-import { TraceLink } from "../components/TraceLink";
 import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
 import { GuidedFlowModal } from "../components/GuidedFlowModal";
 
+type MerchantRow = MerchantAdmissionRow & { projectId?: string | null };
+const ORGANIZATION_STATUS_LABELS: Record<string, string> = { pending_admission: "待准入", active: "正常", suspended: "暂停", terminated: "终止", archived: "归档", blacklisted: "黑名单" };
+const PROJECT_STATUS_LABELS: Record<string, string> = { draft: "草稿", pending_activation: "待启用", active: "已启用", suspended: "暂停", terminated: "终止", archived: "归档" };
+function formatOrganizationStatus(code?: string | null) {
+  return code ? ORGANIZATION_STATUS_LABELS[code] ?? "未识别状态" : "—";
+}
+function formatProjectStatus(code?: string | null) {
+  return code ? PROJECT_STATUS_LABELS[code] ?? "未识别状态" : "—";
+}
+
 const TEXT = {
   title: "组织与商户档案",
   description: "维护经营主体、商户和项目档案，确保运营主体、合同主体和结算主体后续可以唯一绑定、可追溯。",
@@ -69,14 +79,11 @@ function readKpiFocusFromUrl(): KpiFocus | null {
 function kpiFocusTitle(focus: KpiFocus): string {
   if (focus.table === "entities") return "经营主体";
   if (focus.table === "projects") return "项目档案";
-  if (focus.status === "active") return "正常商户";
-  return focus.status ? `商户档案（${formatBusinessStatus(focus.status)}）` : "商户档案";
+  if (focus.status === "active") return "本页正常商户";
+  return focus.status ? `本页商户档案（${formatOrganizationStatus(focus.status)}）` : "商户档案";
 }
 
 export function OrganizationMerchantPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo, focusObjectId }: AdminPageProps) {
-  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
-  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
-  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
   // M012-006：不预填验收默认值；商户名/联系人/电话/合作形式均由用户填写或选择。
   const [merchantName, setMerchantName] = React.useState("");
   // 业态必填 → 不预选，逼招商自己选；选错＝通票分账份额拆给错的业态。
@@ -126,22 +133,28 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
     (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
   }
 
-  const { loading, error, reload } = useAdminResource(
-    async () => {
-      // 跨域副资源不请求（根因修）：主体端点需 entity.binding.view，本页 gate 仅 merchant.profile.view + governance.project.view——
-      // 缺该权限角色 403 会 reject 整个 Promise.all 致整页白屏；缺权限则不请求、主体下拉降级为空。
-      const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);
-      const [nextEntities, nextMerchants, nextProjects] = await Promise.all([
-        canEntityView ? api.get<BusinessEntityRow[]>("/api/v1/platform/business-entities") : Promise.resolve([] as BusinessEntityRow[]),
-        api.get<MerchantAdmissionRow[]>(`/api/v1/platform/merchants?includeArchived=${showArchived}${typeFilter ? `&merchantType=${typeFilter}` : ""}`),
-        api.get<GovernanceProjectRow[]>("/api/v1/platform/projects"),
-      ]);
-      setEntities(nextEntities);
-      setMerchants(nextMerchants);
-      setProjects(nextProjects);
-    },
-    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
-  );
+  const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);
+  const entityRows = useServerPagedRows(
+    (page, size) => canEntityView ? api.getPaged<BusinessEntityRow>("/api/v1/platform/business-entities?page=" + page + "&size=" + size)
+      : Promise.resolve({ items: [] as BusinessEntityRow[], total: 0, page: 1, size }),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const merchantRows = useServerPagedRows(
+    (page, size, query) => api.getPaged<MerchantRow>("/api/v1/platform/merchants?" + (query || "includeArchived=false") + "&page=" + page + "&size=" + size),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const projectRows = useServerPagedRows(
+    (page, size) => api.getPaged<GovernanceProjectRow>("/api/v1/platform/projects?page=" + page + "&size=" + size),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const entities = entityRows.rows;
+  const merchants = merchantRows.rows;
+  const projects = projectRows.rows;
+  const resources = [entityRows, merchantRows, projectRows];
+  const loading = resources.some((resource) => resource.loading);
+  const error = resources.find((resource) => resource.error)?.error ?? null;
+  const reload = () => resources.forEach((resource) => resource.reload());
+
+  React.useEffect(() => {
+    if (!merchantRows.loading && merchantRows.page > merchantRows.pageCount) merchantRows.search(merchantRows.keyword);
+  }, [merchantRows.loading, merchantRows.page, merchantRows.pageCount, merchantRows.keyword]);
 
   // 下钻定位：从别的页点商户进来（?focus=商户id）→ 滚动高亮该商户档案行；无 focus 或未命中时行为不变。
   React.useEffect(() => {
@@ -198,7 +211,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       firstArchivedToggle.current = false;
       return;
     }
-    void reload();
+    merchantRows.search(new URLSearchParams({ includeArchived: String(showArchived), ...(typeFilter ? { merchantType: typeFilter } : {}) }).toString());
     // eslint-disable-next-line react-hooks/exhaustive-deps
   }, [showArchived, typeFilter]);
 
@@ -272,23 +285,28 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
         { label: "统一社会信用代码", value: display(row.unifiedSocialCreditCode) },
         { label: "主体角色", value: formatBusinessStatus(row.entityRole) },
         { label: "法定代表人", value: display(row.legalRepresentative) },
-        { label: "状态", value: formatBusinessStatus(row.status) },
+        { label: "状态", value: formatOrganizationStatus(row.status) },
       ],
     });
   }
 
-  function openMerchantDetail(row: MerchantAdmissionRow) {
+  function merchantProjectName(row: MerchantRow) {
+    return projects.find((project) => project.id === row.projectId)?.name ?? (row.projectId ? "项目资料未加载" : "-");
+  }
+
+  function openMerchantDetail(row: MerchantRow) {
     setDetail({
       title: `商户档案 · ${display(row.merchantName)}`,
       fields: [
         { label: "商户", value: display(row.merchantName) },
+        { label: "所属项目", value: merchantProjectName(row) },
         { label: "业态", value: row.merchantType ? formatBusinessStatus(row.merchantType) : "-" },
         { label: "联系人", value: display(row.contactName) },
         { label: "联系电话", value: display(row.contactPhone) },
         { label: "区块", value: display(row.mallBlockCode) },
         { label: "面积", value: display(row.leasedAreaSquareMeter ?? row.areaSquareMeter) },
         { label: "合作形式", value: cooperationLabel(row.cooperationForm) },
-        { label: "状态", value: formatBusinessStatus(row.status) },
+        { label: "状态", value: formatOrganizationStatus(row.status) },
         auditTraceField(navigateTo, "merchant", row.id),
       ],
     });
@@ -300,9 +318,8 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       fields: [
         { label: "项目", value: display(row.name) },
         { label: "编码", value: display(row.code) },
-        { label: "商户", value: row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId) },
         { label: "业态", value: formatBusinessStatus(row.venueType) },
-        { label: "状态", value: formatBusinessStatus(row.status) },
+        { label: "状态", value: formatProjectStatus(row.status) },
       ],
     });
   }
@@ -313,8 +330,9 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       <div className="admin-table-scroll" data-testid="organization-entities-section">
         <table className="admin-table" data-testid="organization-entities-table">
           <thead><tr><th>主体</th><th>统一社会信用代码</th><th>主体角色</th><th>状态</th><th>详情</th></tr></thead>
-          <tbody>{entities.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-entity-row-${row.id}`} onClick={() => openEntityDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openEntityDetail(row); } }}><td>{display(row.entityName)}</td><td>{display(row.unifiedSocialCreditCode)}</td><td>{formatBusinessStatus(row.entityRole)}</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看经营主体详情" onClick={(event) => { event.stopPropagation(); openEntityDetail(row); }}>查看详情</button></td></tr>)}</tbody>
+          <tbody>{entities.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-entity-row-${row.id}`} onClick={() => openEntityDetail(row)} onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openEntityDetail(row); } }}><td>{display(row.entityName)}</td><td>{display(row.unifiedSocialCreditCode)}</td><td>{formatBusinessStatus(row.entityRole)}</td><td>{formatOrganizationStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看经营主体详情" onClick={(event) => { event.stopPropagation(); openEntityDetail(row); }}>查看详情</button></td></tr>)}</tbody>
         </table>
+        <TablePager testid="organization-entities-pager" page={entityRows.page} pageCount={entityRows.pageCount} total={entityRows.total} onPrev={entityRows.prev} onNext={entityRows.next} />
       </div>
     </>
   );
@@ -359,9 +377,10 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       <div className="admin-table-scroll" data-testid="organization-merchants-section">
         <table className="admin-table" data-testid="organization-merchants-table">
           {/* 后端无“公司名/风险/留痕证据”字段（原按臆造字段读致整表“-”），列对齐真实字段：联系人/联系电话替换。 */}
-          <thead><tr><th>商户</th><th>业态</th><th>联系人</th><th>联系电话</th><th>区块</th><th>面积</th><th>合作形式</th><th>状态</th><th>存档状态</th><th>操作</th><th>详情</th></tr></thead>
-          <tbody>{visibleMerchants.map((row) => <tr key={row.id} data-testid={`organization-merchant-row-${row.id}`} className={`admin-row-clickable${row.id === focusObjectId ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openMerchantDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openMerchantDetail(row); } }}><td>{display(row.merchantName)}</td><td>{row.merchantType ? formatBusinessStatus(row.merchantType) : "-"}</td><td>{display(row.contactName)}</td><td>{display(row.contactPhone)}</td><td>{display(row.mallBlockCode)}</td><td>{display(row.leasedAreaSquareMeter ?? row.areaSquareMeter)}</td><td>{cooperationLabel(row.cooperationForm)}</td><td>{formatBusinessStatus(row.status)}</td><td>{merchantArchiveLabel(row)}</td><td onClick={(event) => event.stopPropagation()}>{merchantActions(row)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看商户档案详情" onClick={(event) => { event.stopPropagation(); openMerchantDetail(row); }}>查看详情</button></td></tr>)}</tbody>
+          <thead><tr><th>商户</th><th>所属项目</th><th>业态</th><th>联系人</th><th>联系电话</th><th>区块</th><th>面积</th><th>合作形式</th><th>状态</th><th>存档状态</th><th>操作</th><th>详情</th></tr></thead>
+          <tbody>{visibleMerchants.map((row) => <tr key={row.id} data-testid={`organization-merchant-row-${row.id}`} className={`admin-row-clickable${row.id === focusObjectId ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openMerchantDetail(row)} onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openMerchantDetail(row); } }}><td>{display(row.merchantName)}</td><td>{merchantProjectName(row)}</td><td>{row.merchantType ? formatBusinessStatus(row.merchantType) : "-"}</td><td>{display(row.contactName)}</td><td>{display(row.contactPhone)}</td><td>{display(row.mallBlockCode)}</td><td>{display(row.leasedAreaSquareMeter ?? row.areaSquareMeter)}</td><td>{cooperationLabel(row.cooperationForm)}</td><td>{formatOrganizationStatus(row.status)}</td><td>{merchantArchiveLabel(row)}</td><td onClick={(event) => event.stopPropagation()}>{merchantActions(row)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看商户档案详情" onClick={(event) => { event.stopPropagation(); openMerchantDetail(row); }}>查看详情</button></td></tr>)}</tbody>
         </table>
+        <TablePager testid="organization-merchants-pager" page={merchantRows.page} pageCount={merchantRows.pageCount} total={merchantRows.total} onPrev={merchantRows.prev} onNext={merchantRows.next} />
       </div>
     </>
   );
@@ -371,14 +390,15 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       <div className="admin-section-title">项目档案</div>
       <div className="admin-table-scroll" data-testid="organization-projects-section">
         <table className="admin-table" data-testid="organization-projects-table">
-          <thead><tr><th>项目</th><th>编码</th><th>商户</th><th>业态</th><th>状态</th><th>详情</th></tr></thead>
-          <tbody>{projects.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-project-row-${row.id}`} onClick={() => openProjectDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openProjectDetail(row); } }}><td>{display(row.name)}</td><td>{display(row.code)}</td><td onClick={(event) => event.stopPropagation()}>{row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId)}</td><td>{formatBusinessStatus(row.venueType)}</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看项目档案详情" onClick={(event) => { event.stopPropagation(); openProjectDetail(row); }}>查看详情</button></td></tr>)}</tbody>
+          <thead><tr><th>项目</th><th>编码</th><th>业态</th><th>状态</th><th>详情</th></tr></thead>
+          <tbody>{projects.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-project-row-${row.id}`} onClick={() => openProjectDetail(row)} onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openProjectDetail(row); } }}><td>{display(row.name)}</td><td>{display(row.code)}</td><td>{formatBusinessStatus(row.venueType)}</td><td>{formatProjectStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看项目档案详情" onClick={(event) => { event.stopPropagation(); openProjectDetail(row); }}>查看详情</button></td></tr>)}</tbody>
         </table>
+        <TablePager testid="organization-projects-pager" page={projectRows.page} pageCount={projectRows.pageCount} total={projectRows.total} onPrev={projectRows.prev} onNext={projectRows.next} />
       </div>
     </>
   );
 
-  const focusCount = kpiFocus ? (kpiFocus.table === "entities" ? entities.length : kpiFocus.table === "projects" ? projects.length : visibleMerchants.length) : 0;
+  const focusCount = kpiFocus ? (kpiFocus.table === "entities" ? entityRows.total : kpiFocus.table === "projects" ? projectRows.total : merchantStatusFilter ? visibleMerchants.length : merchantRows.total) : 0;
 
   return (
     <section data-testid="organization-merchant-page">
@@ -390,7 +410,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       <AuditTrailHint event="organization.viewed / merchant_profile.created" />
       {formError ? <div className="admin-inline-error" data-testid="organization-merchant-form-error">{formError}</div> : null}
       <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
-        {entities.length === 0 && merchants.length === 0 && projects.length === 0 ? (
+        {entityRows.total === 0 && merchantRows.total === 0 && projectRows.total === 0 ? (
           <div className="admin-state admin-empty" data-testid="organization-merchant-empty">暂无组织与商户档案</div>
         ) : kpiFocus ? (
           <>
@@ -406,21 +426,21 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
             <div className="admin-kpi-grid" data-testid="organization-merchant-summary">
               <div className="admin-kpi-card admin-kpi-card--clickable">
                 <span>经营主体</span>
-                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-entities" onClick={() => enterKpiFocus("entities")}>{entities.length}</button>
+                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-entities" onClick={() => enterKpiFocus("entities")}>{entityRows.total}</button>
                 <small>组织档案</small>
               </div>
               <div className="admin-kpi-card admin-kpi-card--clickable">
                 <span>商户档案</span>
-                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-merchants" onClick={() => enterKpiFocus("merchants")}>{merchants.length}</button>
+                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-merchants" onClick={() => enterKpiFocus("merchants")}>{merchantRows.total}</button>
                 <small>商户档案数</small>
               </div>
               <div className="admin-kpi-card admin-kpi-card--clickable">
                 <span>项目档案</span>
-                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-projects" onClick={() => enterKpiFocus("projects")}>{projects.length}</button>
+                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-projects" onClick={() => enterKpiFocus("projects")}>{projectRows.total}</button>
                 <small>项目数</small>
               </div>
               <div className="admin-kpi-card admin-kpi-card--clickable">
-                <span>正常商户</span>
+                <span>本页正常商户</span>
                 <button type="button" className="admin-kpi-value" data-testid="organization-kpi-active-merchants" onClick={() => enterKpiFocus("merchants", "active")}>{merchants.filter((row) => row.status === "active").length}</button>
                 <small>正常商户数</small>
               </div>
diff --git a/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx b/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
index 2cdcc838..8f2f1836 100644
--- a/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
+++ b/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
@@ -1,5 +1,6 @@
-// 文件版本：V0.01
+// 文件版本：V0.03
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
+import userEvent from "@testing-library/user-event";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
 import { SubjectBindingsPage } from "./SubjectBindingsPage";
@@ -14,9 +15,9 @@ const BINDINGS = [
   { id: "b-2", merchant_id: "m-2", operation_entity_id: "be_op_2", contract_entity_id: "be_ct_2", settlement_entity_id: "be_st_2", status: "inactive", effective_from: "2026-06-01", effective_to: "2026-06-30", change_reason: "contract changed" },
 ];
 
-function envelope(items: unknown[]): Response {
+function envelope(items: unknown[], total = items.length, page = 1): Response {
   return new Response(
-    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
+    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page, page_size: 500, total } }),
     { status: 200, headers: { "content-type": "application/json" } },
   );
 }
@@ -28,10 +29,13 @@ function envelopeOne(item: unknown): Response {
   );
 }
 
-function makeApi(): AdminApi {
+function makeApi(respond?: (url: string, init?: RequestInit) => Response | undefined): AdminApi {
   vi.stubGlobal(
     "fetch",
-    vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) => {
+    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
+      const url = String(input);
+      const response = respond?.(url, init);
+      if (response) return Promise.resolve(response);
       if (init?.method === "POST") return Promise.resolve(envelopeOne({ ...BINDINGS[0], status: "inactive" }));
       return Promise.resolve(envelope(BINDINGS));
     }),
@@ -41,13 +45,14 @@ function makeApi(): AdminApi {
 
 const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
 
-function renderPage() {
+function renderPage(respond?: (url: string, init?: RequestInit) => Response | undefined, overrides: Partial<AdminPageProps> = {}) {
   const fullProps: AdminPageProps = {
-    api: makeApi(),
+    api: makeApi(respond),
     user: { id: "u1", username: "admin", accountType: "platform_admin" },
     setActionResult: vi.fn(),
     setGlobalError: vi.fn(),
     effectivePermissions: ADMIN_PERMS,
+    ...overrides,
   };
   render(<SubjectBindingsPage {...fullProps} />);
   return fullProps;
@@ -71,7 +76,7 @@ describe("SubjectBindingsPage KPI 聚焦视图", () => {
     fireEvent.click(screen.getByTestId("subject-bindings-kpi-active"));
     expect(window.location.search).toContain("kpi=bindings");
     expect(window.location.search).toContain("status=active");
-    expect(screen.getByTestId("subject-bindings-kpi-focus-banner").textContent).toContain("正在查看：生效绑定（1）");
+    expect(screen.getByTestId("subject-bindings-kpi-focus-banner").textContent).toContain("正在查看：本页生效绑定（1）");
     expect(screen.queryByTestId("subject-bindings-summary")).toBeNull();
     expect(screen.queryByTestId("subject-bindings-submit-change")).toBeNull();
     const text = screen.getByTestId("subject-bindings-table").textContent ?? "";
@@ -215,3 +220,54 @@ describe("SubjectBindingsPage 绑定变更提交（M012-002/006/008）", () => {
     expect(body).not.toContain("完整后台验收变更");
   });
 });
+
+
+describe("M012 本次绑定回归", () => {
+  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); window.history.replaceState({}, "", "/admin/governance/subject-bindings"); });
+
+  it("空格选择第二条绑定，不被行快捷键截获", async () => {
+    renderPage();
+    const radio = await screen.findByTestId("subject-bindings-select-b-2");
+    radio.focus();
+    await userEvent.setup().keyboard(" ");
+    expect((radio as HTMLInputElement).checked).toBe(true);
+    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
+    fireEvent.keyDown(screen.getByTestId("subject-bindings-row-b-2"), { key: "Enter" });
+    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
+  });
+
+  it("绑定暂停状态不会显示成封禁", async () => {
+    renderPage((_url, init) => init?.method === "POST" ? undefined : envelope([{ ...BINDINGS[0], status: "suspended" }]));
+    const row = await screen.findByTestId("subject-bindings-row-b-1");
+    expect(row.textContent).toContain("暂停");
+    expect(row.textContent).not.toContain("已封禁");
+  });
+
+  it("501条绑定可以翻到第二页，本页去重数不会冒称全量", async () => {
+    const rows = Array.from({ length: 501 }, (_, i) => ({ ...BINDINGS[0], id: "b-page-" + i, merchant_id: "m-page-" + i }));
+    renderPage((url, init) => {
+      if (init?.method === "POST") return undefined;
+      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
+      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
+    });
+    await screen.findByTestId("subject-bindings-row-b-page-0");
+    expect(screen.getByTestId("subject-bindings-summary").textContent).toContain("本页");
+    expect(screen.getByTestId("subject-bindings-pager-status").textContent).toContain("共 501 条");
+    fireEvent.click(screen.getByTestId("subject-bindings-pager-next"));
+    expect(await screen.findByTestId("subject-bindings-row-b-page-500")).toBeTruthy();
+  });
+
+  it("后台拒绝绑定变更时保留原因并显示错误", async () => {
+    const props = renderPage((_url, init) => init?.method === "POST"
+      ? new Response(JSON.stringify({ success: false, code: "BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW", message: "生效绑定变更须经财税复核" }), { status: 409, headers: { "content-type": "application/json" } })
+      : undefined);
+    await screen.findByTestId("subject-bindings-row-b-1");
+    fireEvent.change(screen.getByTestId("subject-bindings-change-action"), { target: { value: "inactive" } });
+    fireEvent.change(screen.getByTestId("subject-bindings-change-reason"), { target: { value: "申请调整" } });
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+    fireEvent.click(screen.getByTestId("subject-bindings-submit-change"));
+    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("财税复核")));
+    expect((screen.getByTestId("subject-bindings-change-reason") as HTMLInputElement).value).toBe("申请调整");
+    expect(screen.getByTestId("subject-bindings-form-error").textContent).toContain("财税复核");
+  });
+});
diff --git a/apps/web/src/admin/pages/SubjectBindingsPage.tsx b/apps/web/src/admin/pages/SubjectBindingsPage.tsx
index ce4e76c7..aca4b239 100644
--- a/apps/web/src/admin/pages/SubjectBindingsPage.tsx
+++ b/apps/web/src/admin/pages/SubjectBindingsPage.tsx
@@ -1,14 +1,14 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
 import { AuditTrailHint } from "../components/AuditTrailHint";
 import { HighRiskConfirm } from "../components/HighRiskConfirm";
-import { useAdminResource } from "../hooks/useAdminResource";
+import { useServerPagedRows } from "../hooks/useServerPagedRows";
+import { TablePager } from "../components/TablePager";
 import { useSelectedRow } from "../hooks/useSelectedRow";
 import type { AdminPageProps } from "../types";
-import { formatBusinessStatus } from "../i18n/statusLabels";
 import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
 
 // M012-001：按后台 BusinessEntityBindingRecord 真实字段读取（AdminApi 已把 snake_case 转 camelCase）；
@@ -35,9 +35,14 @@ const BINDING_ACTIONS: ReadonlyArray<{ code: string; label: string }> = [
 // 后端写权限码（PlatformController：/business-entity-bindings/{id}/change）
 const BINDING_MANAGE_PERMISSION = "entity.binding.manage";
 
+const BINDING_STATUS_LABELS: Record<string, string> = { active: "有效", inactive: "停用", suspended: "暂停" };
+function formatBindingStatus(code?: string | null) {
+  return code ? BINDING_STATUS_LABELS[code] ?? "未识别状态" : "—";
+}
+
 const TEXT = {
   title: "四类主体绑定",
-  description: "展示经营对象、运营主体、合同主体和结算主体的唯一绑定关系；所有变更必须带原因、幂等键和证据附件。",
+  description: "展示经营对象、运营主体、合同主体和结算主体的唯一绑定关系；变更时须选择状态并填写原因。",
   evidence: "接口证据：读取 /api/v1/platform/business-entity-bindings；变更调用 /api/v1/platform/business-entity-bindings/{id}/change。",
   boundary: "四类主体绑定是合同、结算、利润报表和责任归属的基础口径；同一业务对象必须唯一绑定，历史合同和流程留痕不得被覆盖。",
   empty: "暂无主体绑定",
@@ -50,7 +55,7 @@ const TEXT = {
 };
 
 // KPI 聚焦视图（照 OrganizationMerchantPage 样板）：?kpi=bindings[&status=active]；点 KPI 数字=带参重进本页只渲染绑定表（对应子集）。
-// 唯一绑定对象=全量绑定表；生效绑定=status=active 子集。合同主体/选中绑定无独立行子集，KPI 数字保持纯文本不可点。
+// 唯一对象、生效绑定及合同主体指标只统计本页；翻页继续读取服务器数据。
 type KpiFocus = { status: string | null };
 
 function readKpiFocusFromUrl(): KpiFocus | null {
@@ -67,7 +72,11 @@ function display(value: unknown) {
 }
 
 export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
-  const [bindings, setBindings] = React.useState<BindingRow[]>([]);
+  const bindingRows = useServerPagedRows(
+    (page, size) => api.getPaged<BindingRow>("/api/v1/platform/business-entity-bindings?page=" + page + "&size=" + size),
+    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
+  const bindings = bindingRows.rows;
+  const { loading, error, reload } = bindingRows;
   const { setSelectedId, selected } = useSelectedRow(bindings);
   // M012-006：原因不预填验收句，由用户填写；M012-002：变更后状态由用户选择。
   const [reason, setReason] = React.useState("");
@@ -86,7 +95,7 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
         { label: "运营主体编号", value: display(row.operationEntityId) },
         { label: "合同主体编号", value: display(row.contractEntityId) },
         { label: "结算主体编号", value: display(row.settlementEntityId) },
-        { label: "状态", value: formatBusinessStatus(row.status) },
+        { label: "状态", value: formatBindingStatus(row.status) },
         { label: "生效起", value: display(row.effectiveFrom) },
         { label: "生效止", value: display(row.effectiveTo) },
         { label: "变更原因", value: display(row.changeReason) },
@@ -121,18 +130,11 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
     (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
   }
 
-  const { loading, error, reload } = useAdminResource(
-    async () => {
-      setBindings(await api.get<BindingRow[]>("/api/v1/platform/business-entity-bindings"));
-    },
-    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
-  );
-
   const activeCount = bindings.filter((row) => row.status === "active").length;
   const uniqueObjects = new Set(bindings.map((row) => row.merchantId).filter(Boolean)).size;
-  // 聚焦时按状态子集过滤内存数组；无子集=全量。
+  // 聚焦时按状态过滤本页；总条数和后页入口由服务器分页提供。
   const visibleBindings = kpiFocus?.status ? bindings.filter((row) => row.status === kpiFocus.status) : bindings;
-  const focusTitle = kpiFocus?.status === "active" ? "生效绑定" : "主体绑定";
+  const focusTitle = kpiFocus?.status === "active" ? "本页生效绑定" : "本页主体绑定";
 
   function validateChange() {
     if (!selected?.id) {
@@ -186,7 +188,7 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
       <AuditTrailHint event="entity_binding.viewed / entity_binding.changed" />
       {error ? <div className="admin-inline-error" data-testid="subject-bindings-error">{error.message}</div> : null}
       <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
-        {bindings.length === 0 ? <div className="admin-state admin-empty" data-testid="subject-bindings-empty">{TEXT.empty}</div> : (
+        {bindingRows.total === 0 ? <div className="admin-state admin-empty" data-testid="subject-bindings-empty">{TEXT.empty}</div> : (
           <>
             {kpiFocus ? (
               <div className="admin-state" data-testid="subject-bindings-kpi-focus-banner">
@@ -196,10 +198,10 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
             ) : (
               <>
                 <div className="admin-kpi-grid" data-testid="subject-bindings-summary">
-                  <div className="admin-kpi-card admin-kpi-card--clickable"><span>唯一绑定对象</span><button type="button" className="admin-kpi-value" data-testid="subject-bindings-kpi-unique" onClick={() => enterKpiFocus()}>{uniqueObjects}</button><small>唯一绑定</small></div>
-                  <div className="admin-kpi-card admin-kpi-card--clickable"><span>生效绑定</span><button type="button" className="admin-kpi-value" data-testid="subject-bindings-kpi-active" onClick={() => enterKpiFocus("active")}>{activeCount}</button><small>生效中</small></div>
-                  <div className="admin-kpi-card"><span>合同主体</span><strong>{bindings.filter((row) => row.contractEntityId).length}</strong><small>合同口径</small></div>
-                  <div className="admin-kpi-card"><span>选中绑定</span><strong className="admin-kpi-value--text" title={selected?.merchantId ?? "-"}>{selected?.merchantId ?? "-"}</strong><small>{selected?.status ? formatBusinessStatus(selected.status) : "-"}</small></div>
+                  <div className="admin-kpi-card admin-kpi-card--clickable"><span>本页唯一绑定对象</span><button type="button" className="admin-kpi-value" data-testid="subject-bindings-kpi-unique" onClick={() => enterKpiFocus()}>{uniqueObjects}</button><small>唯一绑定</small></div>
+                  <div className="admin-kpi-card admin-kpi-card--clickable"><span>本页生效绑定</span><button type="button" className="admin-kpi-value" data-testid="subject-bindings-kpi-active" onClick={() => enterKpiFocus("active")}>{activeCount}</button><small>生效中</small></div>
+                  <div className="admin-kpi-card"><span>本页合同主体</span><strong>{bindings.filter((row) => row.contractEntityId).length}</strong><small>含合同主体的绑定记录</small></div>
+                  <div className="admin-kpi-card"><span>选中绑定</span><strong className="admin-kpi-value--text" title={selected?.merchantId ?? "-"}>{selected?.merchantId ?? "-"}</strong><small>{selected?.status ? formatBindingStatus(selected.status) : "-"}</small></div>
                 </div>
 
                 <div className="admin-form-grid">
@@ -229,18 +231,19 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
                 <tbody>{visibleBindings.map((row) => (
                   <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`subject-bindings-row-${row.id}`}
                     onClick={() => openBindingDetail(row)}
-                    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBindingDetail(row); } }}>
+                    onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openBindingDetail(row); } }}>
                     <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此主体绑定 ${row.id}`} data-testid={`subject-bindings-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                     <td>{display(row.merchantId)}</td>
                     <td>{display(row.operationEntityId)}</td>
                     <td>{display(row.contractEntityId)}</td>
                     <td>{display(row.settlementEntityId)}</td>
-                    <td>{formatBusinessStatus(row.status)}</td>
+                    <td>{formatBindingStatus(row.status)}</td>
                     <td>{display(row.effectiveFrom)}</td>
                     <td>{display(row.changeReason)}</td>
                   <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看主体绑定详情" onClick={(event) => { event.stopPropagation(); openBindingDetail(row); }}>查看详情</button></td></tr>
                 ))}</tbody>
               </table>
+              <TablePager testid="subject-bindings-pager" page={bindingRows.page} pageCount={bindingRows.pageCount} total={bindingRows.total} onPrev={bindingRows.prev} onNext={bindingRows.next} />
             </div>
           </>
         )}
````

---

## 4. 逐 hunk 正式 ID 归因（闸 3）

48 个 hunk 全部落在 M012 六文件内。无第七文件。无 009、无 001–008 重开、无后台/types/共享源/客人标签/路由/HTML。混合 hunk 按块内变更分列已定稿「是」ID；版本头随该文件本批 ID。

### 4.1 GovernanceTopologyPage.test.tsx（6 hunk）

| hunk | 正式 ID | 说明 |
|---|---|---|
| `@@ -1,5 +1,6 @@` | 015（兼 011/013/016 测试入口） | 版本 V0.01→V0.03；`userEvent` 供行内 Enter |
| `@@ -7,28 +8,30 @@` | 010、011、013、016 | 商户 `project_id`、项目 `merchant_id: null`；envelope 改 page_size=500/total；注释改为现行字典 |
| `@@ -41,15 +44,17 @@` | 011、013、015 | `makeApi`/`renderPage` 可覆盖响应与 props |
| `@@ -98,12 +103,12 @@` | 014 | 删除「已封禁」钉死；项目 active→已启用、suspended→暂停 |
| `@@ -149,7 +154,7 @@` | 014 | 抽屉「有效」改为「已启用」 |
| `@@ -218,3 +223,63 @@` | 010、013、015、016 | 正连项目、字典中文、501 翻页、TraceLink Enter、主体第二页未加载 |

### 4.2 GovernanceTopologyPage.tsx（10 hunk）

| hunk | 正式 ID | 说明 |
|---|---|---|
| `@@ -1,31 +1,42 @@` | 010、013、014、016 | 版本；局部 MerchantRow；项目/商户状态表；字典优先的空间类型；未知「未识别」 |
| `@@ -56,10 +67,26 @@` | 013 | 四资源 `useServerPagedRows` size=500；缺 `entity.binding.view` 不请求主体 |
| `@@ -87,24 +114,14 @@` | 013、016 | 去掉一次性 `api.get`；字典 `api.get("/space-types")`；合并 loading/error/reload |
| `@@ -126,17 +143,17 @@` | 013、014 | 主体未加载文案；状态改 `formatTopologyStatus` |
| `@@ -149,7 +166,7 @@` | 016 | 空间类型传入字典 |
| `@@ -163,7 +180,7 @@` | 010 | `project.id === merchant.projectId` 替换 `project.merchantId === merchant.id` |
| `@@ -172,18 +189,20 @@` | 013、014、015 | target===currentTarget；未加载列；商户/主体 pager |
| `@@ -197,14 +216,15 @@` | 013、015、016 | 空间行键盘收窄；空间 pager |
| `@@ -212,7 +232,7 @@` | 013 | 空态改用 total |
| `@@ -225,10 +245,10 @@` | 013 | KPI 用 total；已挂接标明「本页」 |

### 4.3 OrganizationMerchantPage.test.tsx（6 hunk）

| hunk | 正式 ID | 说明 |
|---|---|---|
| `@@ -1,5 +1,6 @@` | 015（兼 010/011/013/014 测试入口） | 版本 V0.02→V0.06；userEvent |
| `@@ -12,27 +13,29 @@` | 010、011、013 | 商户 project_id；项目 merchant_id null；envelope 500 |
| `@@ -46,13 +49,14 @@` | 011、013 | makeApi 可覆盖 |
| `@@ -60,7 +64,7 @@` | 013 | 聚焦横幅「本页正常商户」 |
| `@@ -139,7 +143,8 @@` | 010 | 项目表不再含 m-1；商户行含项目一 |
| `@@ -329,3 +334,84 @@` | 011、013、014、015 | 缺权限不请求；暂停/待准入/黑名单；筛选随翻页；存档按钮 Enter；末页存档回第 1 页 |

### 4.4 OrganizationMerchantPage.tsx（11 hunk）

| hunk | 正式 ID | 说明 |
|---|---|---|
| `@@ -1,19 +1,29 @@` | 013、014 | 版本 V0.02→V0.03；import pager；删 TraceLink；局部状态表 |
| `@@ -69,14 +79,11 @@` | 013、014 | 聚焦标题加「本页」并用局部状态词 |
| `@@ -126,22 +133,28 @@` | 011、013 | getPaged；缺权限空页；超页回退 search |
| `@@ -198,7 +211,7 @@` | 013 | 归档/业态筛选走 `merchantRows.search`，保留 includeArchived/merchantType |
| `@@ -272,23 +285,28 @@` | 010、014 | 主体/商户状态局部词；所属项目；截断「项目资料未加载」 |
| `@@ -300,9 +318,8 @@` | 010、014 | 项目抽屉删除商户反属列 |
| `@@ -313,8 +330,9 @@` | 013、014、015 | 主体行键盘；主体 pager |
| `@@ -359,9 +377,10 @@` | 010、013、014、015 | 商户表加所属项目；局部状态；键盘；商户 pager |
| `@@ -371,14 +390,15 @@` | 010、013、014、015 | 项目表删商户列；项目 pager |
| `@@ -390,7 +410,7 @@` | 013 | 空态改 total |
| `@@ -406,21 +426,21 @@` | 013 | KPI total；「本页正常商户」 |

### 4.5 SubjectBindingsPage.test.tsx（6 hunk）

| hunk | 正式 ID | 说明 |
|---|---|---|
| `@@ -1,5 +1,6 @@` | 015 | 版本 V0.01→V0.03；userEvent |
| `@@ -14,9 +15,9 @@` | 013 | envelope 500；夹具仍无 project_id |
| `@@ -28,10 +29,13 @@` | 011、013 | makeApi 可覆盖写失败 |
| `@@ -41,13 +45,14 @@` | 011、013、015 | renderPage 可覆盖 |
| `@@ -71,7 +76,7 @@` | 013 | 横幅「本页生效绑定」 |
| `@@ -215,3 +220,54 @@` | 011、013、014、015 | radio Space；暂停≠封禁；501 翻页；409 财税复核仍可见 |

### 4.6 SubjectBindingsPage.tsx（9 hunk）

| hunk | 正式 ID | 说明 |
|---|---|---|
| `@@ -1,14 +1,14 @@` | 013、014 | 版本；改用 getPaged/pager；删除 formatBusinessStatus 导入 |
| `@@ -35,9 +35,14 @@` | 014 | 绑定局部 active/inactive/suspended |
| `@@ -50,7 +55,7 @@` | 012 | 导语删除幂等键/附件过声称 |
| `@@ -67,7 +72,11 @@` | 013 | 改用 bindingRows |
| `@@ -86,7 +95,7 @@` | 014 | 抽屉状态局部词 |
| `@@ -121,18 +130,11 @@` | 013 | 删除 api.get；本页过滤注释 |
| `@@ -186,7 +188,7 @@` | 013 | 空态改 total |
| `@@ -196,10 +198,10 @@` | 013、014 | KPI 标明本页；选中状态局部词 |
| `@@ -229,18 +231,19 @@` | 013、014、015 | 行键盘收窄；状态列；pager |

**归因结论：PASS。** 无行落到 009/历史已修 ID，无无 ID 行，无禁改路径。闸 3 本席不因归因否决。

---

## 5. 七项内容审查（审完全部 diff 后统一判定）

### M012-010 — 局部正连 merchant.projectId；组织撤销项目反属

拓扑 `GovernanceTopologyPage.tsx:183`：`projects.filter((project) => project.id === merchant.projectId)`。组织商户列/抽屉 `merchantProjectName` 按 `project.id === row.projectId` 取名。项目表表头与抽屉删除「商户」列及 TraceLink。测试：拓扑 `governance-topology-row-m-2-p-1` 含「项目一」（夹具项目 `merchant_id: null`）；组织项目表 `not.toContain("m-1")` 且商户行含「项目一」。

后台只读：`listProjectsPaged` `WHERE id = :project_id`（当前综合体 0/1 行）；`listMerchantsPaged` `WHERE project_id = :project_id`。本页不恢复历史 NULL。未改 types.ts，仅页内 `MerchantRow = MerchantAdmissionRow & { projectId?: string | null }`。

**010 内容：够。**

### M012-011 — 真实夹具 / 组织缺权限 / 绑定失败

三测试夹具商户带 `project_id`，项目 `merchant_id` 可空。Binding 夹具仍无 `project_id`。组织新例：缺 `entity.binding.view` 仍渲染商户且 `fetch` 不含 `/business-entities`。绑定新例：POST 409 `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW` 后 `setGlobalError` 含「财税复核」、原因输入仍为「申请调整」、`subject-bindings-form-error` 可见。拓扑既有缺权限不请求主体用例仍过。

**011 内容：够。** 组织缺权限路径未把「未加载」写进商户所属项目列（项目仍独立请求）。拓扑主体列的权限误用见 013。

### M012-012 — 导语

`SubjectBindingsPage.tsx:45` 现文：「展示经营对象、运营主体、合同主体和结算主体的唯一绑定关系；变更时须选择状态并填写原因。」与定稿拟文逐字一致。请求体仍仅 action/reason/idempotencyKey。无附件控件。非业务 HTML。

**012 内容：够。**

### M012-013 — 真分页 / total / 本页指标 / 筛选 / 关联未加载

三页均 `api.getPaged` + `useServerPagedRows(..., 500)` + `TablePager`。本席实跑：拓扑 merchants/spaces 501 可见总数并点下一页出现 `*-500` 行；绑定 501 翻页且摘要含「本页」、pager「共 501 条」；组织翻页 URL 含 `includeArchived=false`，勾选存档+业态 retail 后 `page=1` 且随后 page=2 仍带 `merchantType=retail&includeArchived=true`；存档末页唯一行后回到第 1/1 页。KPI/去重/已挂接/正常商户等派生量加「本页」或改用信封 total。组织筛选走 keyword/`search`，未改 hook 源。

后台只读：商户/空间按当前项目可超过 500；项目按当前 id 至多 1 行，拓扑无项目 pager 与生产默认一致，不构成后页不可达。主体按项目关联公司可超过 500，拓扑在 `canEntityView && pageCount > 1` 时提供主体 pager，并有「第二页主体显示未加载、翻页后呈现」测试。

**013 未修好（内容 FAIL 依据，沿用本 ID）：**

ADM-GOV-001 路由权限是 `governance.project.view` + `governance.space.view` + `merchant.profile.view`，**不含** `entity.binding.view`。拓扑页主路径即可缺主体查看权。此时 `canEntityView` 为假，不请求 `/business-entities`（011 仍成立），`entities=[]`。`GovernanceTopologyPage.tsx:193`（抽屉同源 `:150`）：

```
entity ? display(entity.entityName) : merchant.businessEntityId ? "主体资料未加载" : "-"
```

夹具与生产商户通常带 `businessEntityId`。缺权限的第一次正常打开会把经营主体列写成「主体资料未加载」，主体 pager 因 `!canEntityView` 不出现，用户无法加载。定稿 013 要求的是「独立首页截断」时**明确**未加载、而非未关联；「仅」约束的是局部截断语义。把**未请求（权限降级）**写成「未加载」，是对已加载页的虚假操作声称（GEN-002/015/008）。修复前该列是 `display(entity?.entityName)` → `-`，能力同为不展示主体名，但不谎称资料待加载。组织页未把该字符串用在缺权限商户行，对比说明拓扑此处不是截断所必需。

既有缺权限测试只断言不请求、不白屏、KPI 出现，不断言列文案，故 53 绿不能掩盖该主路径反例。本席未改测试凑绿；反例由 after 源码 + 路由只读得出。

观察（不升新 ID、不单独否决）：多资源 `loading` 做 OR 后，点任一 pager 会经 `AdminDataState` 整页换成「加载中...」再回来，属刷新闪烁，翻页后数据仍到。字典失败会挡住整页，但 space-types 与 spaces 同 `governance.space.view` + `requirePlatformScope()`，不构成新权限类用户。focus 跨页不自动跳页，与旧默认 500 截断同类。这些不另开号。

**013 内容：FAIL（未加载用在权限未请求主路径）。**

### M012-014 — 局部中文状态

三页删除 `formatBusinessStatus` 对状态列的使用（拓扑项目/商户、组织主体/商户/项目、绑定）。局部表：

- 项目：草稿/待启用/已启用/暂停/终止/归档（对 BEQR 六态）
- 商户/组织：待准入/正常/暂停/终止/归档/黑名单
- 绑定：有效/停用/暂停
- 空间状态未知改「未识别状态」不再回落到客人「已封禁」

客人 `statusLabels.ts` 与 admin 再导出未改。测试删除「已封禁」锁定；组织待准入/暂停/黑名单分列；绑定 suspended→暂停。业态/角色/合作形式仍走既有 `formatBusinessStatus`，属 014「保留已正确标签」。未知码「未识别状态/类型」或破折号。

**014 内容：够。**

### M012-015 — 父行仅 currentTarget

三页 `tr onKeyDown` 均加 `event.target === event.currentTarget`。测试：拓扑 TraceLink Enter 下钻且不开抽屉、行自身 Enter 仍开抽屉；组织真实存档按钮 Enter 触发 prompt 且不开抽屉（初轮误选不存在控件后的实际按钮）；绑定 radio Space 选中第二条、不开抽屉、行 Enter 仍开。未造键盘抽象。

**015 内容：够。**

### M012-016 — space-types 显示名

拓扑 `api.get("/space-types")`（字典接口无分页参数，与 SpaceTypesPage 同形，不是新 API）。`formatSpaceType`：字典 `typeName` → 八码中文 →「未识别类型」。证据行补 space-types。路由未改；ADM-GOV-001 已含 `governance.space.view`。测试 kiosk→一体机位且不含 kiosk。

**016 内容：够。**

### M012-009 与历史 001–008

diff 无附件上传、无创建绑定入口、无项目归属配置器、无 types/后台。KPI URL 内部键未改。绑定 409 测试验证的是失败可见，不是放行停用。不改观察。

---

## 6. 内容票 / 归因票 / 回退分类

| 票 | 结论 | 依据 |
|---|---|---|
| 内容 | **FAIL** | 013 未加载语义越出「首页截断」，落到 ADM-GOV-001 缺 `entity.binding.view` 的主路径 |
| 归因 | **PASS** | 48 hunk / 六文件均可归 010–016；无禁改路径 |
| 总结论 | **FAIL** | 任一内容 FAIL 即整批 FAIL |

FAIL 条目分类（§4 回退二分）：

| 依据 | 正式 ID | 队列当前值 | 路由 |
|---|---|---|---|
| 拓扑缺主体权仍写「主体资料未加载」且无 pager | M012-013 | 是（修复队列） | 沿用 013，不分配新号 |

本批每条 FAIL 均为「有 ID 且仍在修复队列」→ 整批应直接回 B3（须两席 raw 落齐后由调度执行，本席不修）。无添加项、无 HTML 申请、无 LOCK-AMEND。010/011/012/014/015/016 本席视为已按边界落地，回 B3 时不得借 013 顺修 009 或禁改路径。

---

## 7. 实际命令、验证与限制

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
python3 hashlib；openssl dgst -sha256
git cat-file -t 0c7e7d157c2c8aa3a3580886baf01d8405993c8a
git cat-file -t d06253fa9ef48efd4abf368911981f95d518fc8b
git diff --binary <before> <after>   # 79044 bytes，SHA256 ae56e18496e6c54b10c4e156d6625d7779a21e288ca02159b650ef4d7395e6f8
git diff --name-status/--stat/--numstat/--check --binary <before> <after>
git ls-tree -r --name-only 两树 | wc -l；comm -3 空
GIT_INDEX_FILE=临时 git add -A && git write-tree   # a886ea14…；真实索引未改
cd apps/web && npx vitest run GovernanceTopologyPage.test.tsx OrganizationMerchantPage.test.tsx SubjectBindingsPage.test.tsx --reporter=verbose
cd apps/web && npm run typecheck
```

测试实跑：3 files / **53 passed**（拓扑 18、组织 20、绑定 15），exit 0，Duration 3.49s。保留 jsdom `Window's scrollTo()` / `getComputedStyle() method: with pseudo-elements` 提示，**不称浏览器**。typecheck `tsc -b --noEmit` exit 0。53 绿不覆盖 013 权限列文案反例。

未运行：真实浏览器、真实 HTTP、后台测试、Maven、psql、数据库写入。后台/路由/字典权限为只读追流（PlatformController/Service/JdbcPlatformRepository、adminRoutes）。

未调用：MCP、任何子代理、操作员提问。未改源码/测试/业务 HTML/锁/findings/ledger/其它报告。

### 技能（V0.17 §6 收窄，本节点实核路径存在）

| 技能 | 本节点 |
|---|---|
| receiving-code-review | 只借先核源/命令再接受或反驳；禁止表演性同意 53 绿 |
| verification-before-completion | 声称前先跑哈希/diff/vitest/tsc |
| ponytail-review | 只评是否最小删除/复用，不落地 |
| diagnosing-bugs / systematic-debugging | 只读追 013 文案数据流 |
| requesting-code-review / tdd / 派子代理 | **未用**（与双席冲突） |
| batch-grill-me | **缺口：本环境无独立技能文件**；未冒充调用 |

---

## 8. 整批结论

**FAIL**

内容 FAIL，归因 PASS。未完成项：M012-013 权限未请求路径的「主体资料未加载」声称。有未完不得 PASS。本票不授权继续 B5，不修改台账。不覆盖 B1 FAIL 原文与 B2 双 PASS 定稿。历史 001–008 保持已修否；009 保持登记否。不等于模块 PASS。
