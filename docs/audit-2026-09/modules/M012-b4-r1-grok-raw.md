CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M012 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M012-b4-r1-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：任何 `M012-b4-r1-self-raw.md`（当前亦不存在）、席甲本次 B4 raw。定稿队列以 findings.md V1.41 顶部「当前 M012 B2-r2 双PASS定稿，8项待修（B3-r1）」为准。本轮只审该定稿「是」8 项（001–008）的修复内容与 before/after 全文 diff；009 为队列「否」（登记），本 diff 不得为其加下钻用例或创建绑定入口。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、8 个定稿「是」ID 是否按最小边界修好、每一变更行能否归因到上述 ID、是否越界改三份业务 HTML / 锁 / sidecar / ledger 基准 / 权限种子 / 迁移 / 后台源码 / types.ts / statusLabels.ts / 路由权限码，以及是否新增三主体选择器、entity.binding.view 创建闸、下钻用例、创建绑定入口。只用既有冻结判据。未新造判据/角色，未询问操作员。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。criteria-lock 版本仍 V0.11。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M012 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／网页与验证／第1段 |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 正式队列「是」（8） | M012-001、002、003、008（技术性）；004（冲突）；005（细化）；006、007（添加，操作员答删除） |
| 正式队列「否」（1） | M012-009（登记，含拓扑下钻无测试） |
| B3 | 已执行 1 次（本轮为第 1 次），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `83d85aa3001e3c4d29475c6c24a6533e0544a5e0` |
| after（对象类型 tree，可解析） | `50c07ff1a9041fd2cbb9ab4263e05c86dcd9a10f` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 83d85aa3001e3c4d29475c6c24a6533e0544a5e0 50c07ff1a9041fd2cbb9ab4263e05c86dcd9a10f`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 61235 bytes；SHA256 `f9b1b1fbd589840caa8b5a443f12d4219a45d67ce349499a968e2340fa05db47`；UTF-8；6 文件；357 插入 / 103 删除；51 hunk；name-status 六处 M |
| 与 `/tmp/m012_b3_r1.diff` | 字节与 SHA256 全等（本席自算为准，副本仅对照） |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3629 / after 3629；`comm -3` 空（无增删路径） |
| 当前工作区临时索引 `git add -A && git write-tree` | `50c07ff1a9041fd2cbb9ab4263e05c86dcd9a10f`＝after |
| HEAD | `bd445728a6ec7bb36a9e7ff19fe56927410ac61e` = `origin/main` |
| findings.md / ledger.md | 两树 blob 相同（本 diff 未改）：findings `f76933332786…`，ledger `ff62a29518b8…` |
| 三 HTML 两树 blob | BSM `12334b13f3c27a815c9bbf9eea216403fb0d76d1` / PGM `c2588ca1c65a3f196b39a23f299bd56967b5652e` / BEQR `86a717930f9303d4f131ee2864478baa14450e6d`，完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |
| 全部 `backend/src/main/resources/db/migration` | 两树 ls-tree 全等（378 行）；V003 权限切片 blob `a13ebe87a1a2e5a8fb160eac6980a4cd4cd84812` |
| types.ts / statusLabels.ts / apiError.ts | 两树 blob 相同：`8d26e82118d6` / `68e192455250` / `9c669a3ac2f9` |
| 后台源码 | 本 diff `git diff --name-only -- backend/` 空 |

六文件 blob（HEAD＝before；工作区＝after）：

| 文件 | before / HEAD | after / work |
|---|---|---|
| GovernanceTopologyPage.tsx | `f0a2f2e99312d2894a14bbd2965e4722b170fda3` | `1c363f479b8581416bf50d4390f622dad46c3057` |
| GovernanceTopologyPage.test.tsx | `7d2e96b7de0e01990c38e3fbcb8d4f68995a3b51` | `fba79864cde5e07fcdde8d6c923072f807327442` |
| SubjectBindingsPage.tsx | `c8b6cf293104cd449f20f36af51b981bde4d32bc` | `ce4e76c7d45fa8ac02dbc12c4d7733016ba5fedd` |
| SubjectBindingsPage.test.tsx | `70ea091751ca5b3dadbde5eb78e1f43408d72475` | `2cdcc838ba4844254e5d0de2ac08f488f5fba57a` |
| OrganizationMerchantPage.tsx | `64b9cc0cc90740d0b6ab8941be3a3703f33a38d4` | `7d5b3ee9c5ee5f590c30401e0b02c1722efe5d94` |
| OrganizationMerchantPage.test.tsx | `5592efd6cc3f8907a3f4f89452e1d4afe9a0f84b` | `823e37e36256078cc9dd7f789dc9824a5c842c40` |

`git diff --stat --binary`：

```
 .../admin/pages/GovernanceTopologyPage.test.tsx    |  40 +++++++-
 .../web/src/admin/pages/GovernanceTopologyPage.tsx |  25 +++--
 .../admin/pages/OrganizationMerchantPage.test.tsx  |  98 +++++++++++++++---
 .../src/admin/pages/OrganizationMerchantPage.tsx   | 102 ++++++++++---------
 .../src/admin/pages/SubjectBindingsPage.test.tsx   | 112 +++++++++++++++++++--
 apps/web/src/admin/pages/SubjectBindingsPage.tsx   |  83 ++++++++++-----
 6 files changed, 357 insertions(+), 103 deletions(-)
```

`git diff --numstat`：`36 4` / `19 6` / `84 14` / `57 45` / `104 8` / `57 26`（与上表六文件同序）。

工作区相对 HEAD 另有 M011 已闭环的后台测试与审计产物（findings/ledger/M011 双席 raw 等）。那些路径在 before/after 两树 blob 相同，不在本轮 diff 内，不按本轮修复内容审计。本席未改任何源文件，只新建本 raw。验证：主仓只读 vitest（after＝工作区）；RED 在 `/tmp/m012_b4r1_redmini` 迷你工作区（before 三页面 + after 三测试）；tsc 在 `/tmp/m012_b4r1_grok_tscmini` 副本 `--noEmit`。未 commit/push/PR/merge/部署。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M012 的集合：BSM-001、BSM-003、GEN-001–018、PGM-001。本轮只用定稿 001–008 已引用的 ID（另 BSM-004 由 008 存档理由测试引用，属既有冻结 ID）。不 LOCK-AMEND。

B3-r1 边界（与 findings V1.41 顶部一致）：

- **只改本模块 6 文件**。版本：OrganizationMerchantPage.tsx/.test.tsx V0.01→V0.02；其余四文件原先无版本头，加 `// 文件版本：V0.01`。
- **001**：页内改读 merchantId/operationEntityId/contractEntityId/settlementEntityId/status/effectiveFrom/effectiveTo/changeReason；KPI 唯一绑定按 merchantId、合同主体按 contractEntityId、选中展示 merchantId；夹具真实 snake_case。types.ts 不改。
- **002**：三态中文下拉（启用/停用/暂停＝active/inactive/suspended）+ POST 带 action；绑定提交断言只一条。
- **003**：attachedSpaces 改 projectId；测试用真实 project_id 断言非 0。
- **004**：项目业态 `formatBusinessStatus`；空间类型页内 8 码中文表（与 V184 type_name 逐字一致）；空间状态可用/不可用；表头「编号」；测试中文断言。不改 statusLabels.ts、不拉 /space-types。
- **005**：夹具改 platform/project_company/management_company、lease、shop、active/inactive；断言改中文；注释六值。
- **006**：清空预填；创建请求不传三主体/区块/面积/备用联系人/租金/status/operatorNote/假证据；合作方式可空项；绑定页原因空初始必填且不传假证据。不新增三主体选择器、不把 entity.binding.view 当创建闸。
- **007**：手机号正则改非空（引导流即时拦空串）；后台不改。
- **008**：存档一条（空理由拦截与有理由 POST 同条）+ 删除申请一条 + 写路径 catch。禁止每个 verb 各写一条；禁止为下钻加用例。
- 禁止：改 types.ts/statusLabels.ts/后台/路由权限码/三份 HTML/锁。

questions/M012-006.md、M012-007.md 操作员答复首行均为「删除」。四分类本轮不重开。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary 83d85aa3001e3c4d29475c6c24a6533e0544a5e0 50c07ff1a9041fd2cbb9ab4263e05c86dcd9a10f`

before tree = `83d85aa3001e3c4d29475c6c24a6533e0544a5e0`
after tree = `50c07ff1a9041fd2cbb9ab4263e05c86dcd9a10f`

字节 61235；SHA256 `f9b1b1fbd589840caa8b5a443f12d4219a45d67ce349499a968e2340fa05db47`；UTF-8 完整原文如下。

````diff
diff --git a/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx b/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
index 7d2e96b7..fba79864 100644
--- a/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
+++ b/apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -6,13 +7,15 @@ import { AdminApi } from "../client/adminClient";
 import type { AdminPageProps, EffectivePermissionsState } from "../types";
 
 // mock 走真实 AdminApi（stub fetch）→ unwrap 转 camelCase → 组件。platform 域 snake_case 响应。
-const ENTITIES = [{ id: "org-1", entity_name: "平台经营主体", unified_social_credit_code: "ORG-1", entity_role: "operation", status: "active" }];
+// M012-005：夹具只用现行闭集——主体角色 platform/project_company/management_company（V410），空间类型走 V184 字典 8 码。
+const ENTITIES = [{ id: "org-1", entity_name: "平台经营主体", unified_social_credit_code: "ORG-1", entity_role: "management_company", status: "active" }];
 const MERCHANTS = [
   { id: "m-1", business_entity_id: "org-1", merchant_name: "商户甲", status: "active" },
   { id: "m-2", business_entity_id: "org-1", merchant_name: "商户乙", status: "suspended" },
 ];
 const PROJECTS = [{ id: "p-1", name: "项目一", code: "PJ-1", merchant_id: "m-1", venue_type: "bar", status: "active" }];
-const SPACES = [{ id: "s-1", space_name: "空间甲", project_id: "p-1", floor_no: "L1", area_square_meter: 120, space_type: "hall", status: "active" }];
+// M012-003：后台 SpaceRecord 以 project_id 挂接项目（无 venue_id）。
+const SPACES = [{ id: "s-1", space_name: "空间甲", project_id: "p-1", floor_no: "L1", area_square_meter: 120, space_type: "shop", status: "active" }];
 
 function envelope(items: unknown[]): Response {
   return new Response(
@@ -105,13 +108,40 @@ describe("GovernanceTopologyPage KPI 聚焦视图", () => {
   });
 });
 
+describe("GovernanceTopologyPage 真实字段与全中文（M012-003/004）", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    window.history.replaceState({}, "", "/admin/governance/topology");
+  });
+
+  it("项目业态与空间类型渲染为中文，不直出 bar/shop 英文码（PGM-001）", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("governance-topology-table")).toBeTruthy());
+    const topology = screen.getByTestId("governance-topology-table").textContent ?? "";
+    expect(topology).toContain("酒吧");
+    expect(topology).not.toMatch(/\bbar\b/);
+    const spaces = screen.getByTestId("governance-topology-spaces-table").textContent ?? "";
+    expect(spaces).toContain("商铺/铺位");
+    expect(spaces).not.toMatch(/\bshop\b/);
+    // M012-004：空间状态闭集 active/unavailable 页内映射「可用/不可用」（M009-036 口径）。
+    expect(spaces).toContain("可用");
+  });
+
+  it("「已挂接」按后台 project_id 计数（读臆造 venue_id 时恒为 0，本用例必红）", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("governance-topology-summary")).toBeTruthy());
+    expect(screen.getByTestId("governance-topology-summary").textContent).toContain("1 个已挂接");
+  });
+
+});
+
 describe("GovernanceTopologyPage 行明细抽屉（拓扑表 / 空间表整行可点）", () => {
   afterEach(() => {
     vi.unstubAllGlobals();
     window.history.replaceState({}, "", "/admin/governance/topology");
   });
 
-  it("点拓扑关系行 → 抽屉含主体/商户/项目与中文状态", async () => {
+  it("点拓扑关系行 → 抽屉含主体/商户/项目与中文状态、中文业态", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("governance-topology-row-m-1-p-1")).toBeTruthy());
     fireEvent.click(screen.getByTestId("governance-topology-row-m-1-p-1"));
@@ -120,10 +150,11 @@ describe("GovernanceTopologyPage 行明细抽屉（拓扑表 / 空间表整行
     expect(drawer.textContent).toContain("商户甲");
     expect(drawer.textContent).toContain("项目一");
     expect(drawer.textContent).toContain("有效");
+    expect(drawer.textContent).toContain("酒吧");
     expect(drawer.textContent).not.toContain("active");
   });
 
-  it("点空间点位行 → 抽屉含空间名/所属项目/面积", async () => {
+  it("点空间点位行 → 抽屉含空间名/所属项目/面积/中文空间类型", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("governance-space-row-s-1")).toBeTruthy());
     fireEvent.click(screen.getByTestId("governance-space-row-s-1"));
@@ -131,6 +162,7 @@ describe("GovernanceTopologyPage 行明细抽屉（拓扑表 / 空间表整行
     expect(drawer.textContent).toContain("空间甲");
     expect(drawer.textContent).toContain("项目一");
     expect(drawer.textContent).toContain("120");
+    expect(drawer.textContent).toContain("商铺/铺位");
   });
 
   it("Esc 关闭抽屉 + 点遮罩关闭", async () => {
diff --git a/apps/web/src/admin/pages/GovernanceTopologyPage.tsx b/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
index f0a2f2e9..1c363f47 100644
--- a/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
+++ b/apps/web/src/admin/pages/GovernanceTopologyPage.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -10,6 +11,17 @@ import { formatBusinessStatus } from "../i18n/statusLabels";
 import { TraceLink } from "../components/TraceLink";
 import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
 
+// M012-004：空间类型字典 8 码（V184 space_type_dictionary 种子，type_name 中文）；PGM-001 零英文，页面不直出 type_code。
+const SPACE_TYPE_LABELS: Record<string, string> = { building: "楼栋", floor: "楼层", zone: "区块/区域", shop: "商铺/铺位", public_area: "公共区域", parking: "车位/车库", booth: "摊位/展位", other: "其他" };
+function formatSpaceType(code: string | null | undefined) {
+  return code ? SPACE_TYPE_LABELS[code] ?? formatBusinessStatus(code) : "—";
+}
+// M012-004：空间状态闭集 active/unavailable（M009-036）页内映射「可用/不可用」，不直出英文。
+const SPACE_STATUS_LABELS: Record<string, string> = { active: "可用", unavailable: "不可用" };
+function formatSpaceStatus(code: string | null | undefined) {
+  return code ? SPACE_STATUS_LABELS[code] ?? formatBusinessStatus(code) : "—";
+}
+
 const TEXT = {
   title: "集团/综合体拓扑",
   description: "展示集团、综合体、商户、项目和空间点位之间的治理关系，作为权限、合同、结算和运营责任归属的入口。",
@@ -95,7 +107,8 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
   );
 
   const projectIds = new Set(projects.map((project) => project.id));
-  const attachedSpaces = spaces.filter((space) => space.venueId && projectIds.has(space.venueId)).length;
+  // M012-003：后台 SpaceRecord 以 projectId 挂接项目（无 venueId；types.ts 的 venueId 为旧臆造字段）。
+  const attachedSpaces = spaces.filter((space) => space.projectId && projectIds.has(space.projectId)).length;
 
   // 下钻定位:从别的页点商户/综合体进来(?focus=id)→ 滚动到拓扑中对应节点行(商户或项目)并高亮;无 focus 时不动。
   React.useEffect(() => {
@@ -122,7 +135,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
         { label: "商户", value: merchant.id ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={merchant.id}>{display(merchant.merchantName)}</TraceLink> : display(merchant.merchantName) },
         { label: "项目", value: display(project?.name) },
         { label: "项目编码", value: display(project?.code) },
-        { label: "业态", value: display(project?.venueType) },
+        { label: "业态", value: formatBusinessStatus(project?.venueType) },
         { label: "状态", value: formatBusinessStatus(project?.status ?? merchant.status) },
       ],
     });
@@ -136,8 +149,8 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
         { label: "所属项目", value: display(project?.name ?? space.projectId) },
         { label: "楼层", value: display(space.floorNo) },
         { label: "面积(㎡)", value: display(space.areaSquareMeter) },
-        { label: "空间类型", value: formatBusinessStatus(space.spaceType) },
-        { label: "状态", value: formatBusinessStatus(space.status) },
+        { label: "空间类型", value: formatSpaceType(space.spaceType) },
+        { label: "状态", value: formatSpaceStatus(space.status) },
       ],
     });
   }
@@ -165,7 +178,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
                 <td onClick={(event) => event.stopPropagation()}>{merchant.id ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={merchant.id}>{display(merchant.merchantName)}</TraceLink> : display(merchant.merchantName)}</td>
                 <td>{display(project?.name)}</td>
                 <td>{display(project?.code)}</td>
-                <td>{display(project?.venueType)}</td>
+                <td>{formatBusinessStatus(project?.venueType)}</td>
                 <td>{formatBusinessStatus(project?.status ?? merchant.status)}</td>
               <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看拓扑关系详情" onClick={(event) => { event.stopPropagation(); openTopologyDetail(entity, merchant, project); }}>查看详情</button></td></tr>
             ));
@@ -184,7 +197,7 @@ export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, nav
           <tbody>{spaces.map((space) => {
             const project = projects.find((item) => item.id === space.projectId);
             const spaceFocused = !!focusObjectId && (focusObjectId === space.id || focusObjectId === space.projectId);
-            return <tr key={space.id} data-testid={`governance-space-row-${space.id}`} className={`admin-row-clickable${spaceFocused ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openSpaceDetail(space, project)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSpaceDetail(space, project); } }}><td>{display(space.spaceName)}</td><td>{display(project?.name ?? space.projectId)}</td><td>{display(space.floorNo)}</td><td>{display(space.areaSquareMeter)}</td><td>{formatBusinessStatus(space.spaceType)}</td><td>{formatBusinessStatus(space.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看空间点位详情" onClick={(event) => { event.stopPropagation(); openSpaceDetail(space, project); }}>查看详情</button></td></tr>;
+            return <tr key={space.id} data-testid={`governance-space-row-${space.id}`} className={`admin-row-clickable${spaceFocused ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openSpaceDetail(space, project)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSpaceDetail(space, project); } }}><td>{display(space.spaceName)}</td><td>{display(project?.name ?? space.projectId)}</td><td>{display(space.floorNo)}</td><td>{display(space.areaSquareMeter)}</td><td>{formatSpaceType(space.spaceType)}</td><td>{formatSpaceStatus(space.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看空间点位详情" onClick={(event) => { event.stopPropagation(); openSpaceDetail(space, project); }}>查看详情</button></td></tr>;
           })}</tbody>
         </table>
       </div>
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
index 5592efd6..823e37e3 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -9,11 +9,13 @@ import type { AdminPageProps, EffectivePermissionsState } from "../types";
 // mock 按后端真实 snake_case 响应结构造数据（platform 域 Jackson 全局 SNAKE_CASE，字段以实测响应为准），
 // 并走真实 AdminApi（stub fetch）→ unwrap 转 camelCase → 组件。字段再错位（读臆造字段名）本文件必红。
 const ENTITIES = [
-  { id: "org-1", entity_name: "平台经营主体", entity_role: "operation", unified_social_credit_code: "91330100MA27XW8H", legal_representative: "张三", bank_account_ref: "vault://demo", status: "active", created_at: "2026-07-01T00:00:00Z" },
+  { id: "org-1", entity_name: "平台经营主体", entity_role: "management_company", unified_social_credit_code: "91330100MA27XW8H", legal_representative: "张三", bank_account_ref: "vault://demo", status: "active", created_at: "2026-07-01T00:00:00Z" },
 ];
 const MERCHANTS = [
-  { id: "m-1", merchant_name: "正常商户甲", merchant_type: "catering", business_entity_id: "org-1", contact_name: "联系人甲", contact_phone: "13800000001", mall_block_code: "1F-A", area_square_meter: 128.5, leased_area_square_meter: 120, cooperation_form: "fixed_rent", status: "active" },
+  { id: "m-1", merchant_name: "正常商户甲", merchant_type: "catering", business_entity_id: "org-1", contact_name: "联系人甲", contact_phone: "13800000001", mall_block_code: "1F-A", area_square_meter: 128.5, leased_area_square_meter: 120, cooperation_form: "lease", status: "active" },
   { id: "m-2", merchant_name: "停用商户乙", contact_name: "联系人乙", status: "suspended" },
+  // M012-008：已存档商户（行内动作为取消存档/申请删除下拉）。status 取 suspended 以不改变「正常商户（1）」断言。
+  { id: "m-3", merchant_name: "已存档商户丙", contact_name: "联系人丙", status: "suspended", archived_at: "2026-07-01T00:00:00Z", delete_status: "none" },
 ];
 const PROJECTS = [
   { id: "p-1", name: "项目一", code: "PJ-1", merchant_id: "m-1", venue_type: "bar", floor: "1F", capacity: 80, status: "active" },
@@ -29,8 +31,10 @@ function envelope(items: unknown[]): Response {
 function makeApi(): AdminApi {
   vi.stubGlobal(
     "fetch",
-    vi.fn().mockImplementation((input: RequestInfo | URL) => {
+    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
       const url = String(input);
+      // 写路径（创建/存档/删除申请）默认返回成功信封；失败场景由用例 mockImplementationOnce 覆盖。
+      if (init?.method === "POST") return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "m-new" } }), { status: 200, headers: { "content-type": "application/json" } }));
       if (url.includes("business-entities")) return Promise.resolve(envelope(ENTITIES));
       if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
       if (url.includes("projects")) return Promise.resolve(envelope(PROJECTS));
@@ -51,6 +55,7 @@ function renderPage() {
     effectivePermissions: ADMIN_PERMS,
   };
   render(<OrganizationMerchantPage {...fullProps} />);
+  return fullProps;
 }
 
 // 聚焦视图断言：只剩商户表（active 行），其余表/表单/KPI 卡全隐藏，横幅在场。
@@ -121,7 +126,7 @@ describe("OrganizationMerchantPage KPI 聚焦视图（带参重进本页）", ()
     const entitiesText = screen.getByTestId("organization-entities-table").textContent ?? "";
     expect(entitiesText).toContain("平台经营主体");
     expect(entitiesText).toContain("91330100MA27XW8H");
-    expect(entitiesText).toContain("运营主体");
+    expect(entitiesText).toContain("业态主体（管理公司）"); // M012-005：V410 三角色，不再用已删的 operation
     // 商户表：商户名/联系人/电话/区块/面积（租赁面积优先）/合作形式中文。
     const m1 = screen.getByTestId("organization-merchant-row-m-1").textContent ?? "";
     expect(m1).toContain("正常商户甲");
@@ -129,13 +134,15 @@ describe("OrganizationMerchantPage KPI 聚焦视图（带参重进本页）", ()
     expect(m1).toContain("13800000001");
     expect(m1).toContain("1F-A");
     expect(m1).toContain("120");
-    expect(m1).toContain("固定租金");
+    expect(m1).toContain("租赁"); // M012-005：合作方式六类，不再用已退役的 fixed_rent
     // 项目表：项目名/编码/商户下钻（merchant_id）/业态。
     const projectsText = screen.getByTestId("organization-projects-table").textContent ?? "";
     expect(projectsText).toContain("项目一");
     expect(projectsText).toContain("PJ-1");
     expect(projectsText).toContain("m-1");
-    expect(projectsText).toContain("bar");
+    // M012-004：业态列中文（PGM-001 零英文），不再断言英文码。
+    expect(projectsText).toContain("酒吧");
+    expect(projectsText).not.toMatch(/\bbar\b/);
   });
 
   it("KPI 子标签渲染为中文，不含英文", async () => {
@@ -155,11 +162,12 @@ describe("OrganizationMerchantPage KPI 聚焦视图（带参重进本页）", ()
     await waitFor(() => expect(screen.getByTestId("organization-merchant-create-open")).toBeTruthy());
     fireEvent.click(screen.getByTestId("organization-merchant-create-open"));
     // 第一步：基本信息（商户类型下拉在此步）。
-    // V348 业态五值：酒吧/餐饮/零售/游乐/其他，全中文、不预选（必填）。
+    // V348/V384 业态六值：酒吧/餐饮/零售/游乐/传媒/其他，全中文、不预选（必填）。M012-005
     const typeText = screen.getByTestId("organization-merchant-type").textContent ?? "";
     expect(typeText).toContain("酒吧");
     expect(typeText).toContain("餐饮");
     expect(typeText).toContain("游乐");
+    expect(typeText).toContain("传媒");
     expect(typeText).toContain("其他");
     expect(typeText).not.toContain("amusement");
     expect(typeText).not.toContain("catering");
@@ -187,11 +195,11 @@ describe("OrganizationMerchantPage KPI 聚焦视图（带参重进本页）", ()
     fireEvent.change(screen.getByTestId("organization-merchant-name"), { target: { value: "引导流程商户" } });
     fireEvent.click(screen.getByTestId("organization-merchant-flow-next"));
     expect(screen.getByTestId("organization-merchant-flow-error").textContent).toContain("必须选择商户业态");
-    // 选业态但电话非法 → 命中手机号校验。
+    // 选业态但电话为空 → 命中「联系电话不能为空」（M012-007：不再前端写死 11 位大陆手机号规则；M012-006：电话不预填）。
     fireEvent.change(screen.getByTestId("organization-merchant-type"), { target: { value: "bar" } });
-    fireEvent.change(screen.getByTestId("organization-merchant-contact-phone"), { target: { value: "12345" } });
+    expect((screen.getByTestId("organization-merchant-contact-phone") as HTMLInputElement).value).toBe("");
     fireEvent.click(screen.getByTestId("organization-merchant-flow-next"));
-    expect(screen.getByTestId("organization-merchant-flow-error").textContent).toContain("手机号");
+    expect(screen.getByTestId("organization-merchant-flow-error").textContent).toContain("联系电话不能为空");
     // 修正电话 → 下一步进确认页。
     fireEvent.change(screen.getByTestId("organization-merchant-contact-phone"), { target: { value: "13912345678" } });
     fireEvent.click(screen.getByTestId("organization-merchant-flow-next"));
@@ -207,7 +215,13 @@ describe("OrganizationMerchantPage KPI 聚焦视图（带参重进本页）", ()
     const postCall = fetchMock.mock.calls.find(([, init]) => (init as RequestInit | undefined)?.method === "POST");
     expect(postCall).toBeTruthy();
     expect(String(postCall![0])).toContain("/api/v1/platform/merchants");
-    expect(String((postCall![1] as RequestInit).body)).toContain("引导流程商户");
+    const body = String((postCall![1] as RequestInit).body);
+    expect(body).toContain("引导流程商户");
+    // M012-006：不再静默提交验收默认值/假主体/假证据/租金面积。
+    expect(body).not.toContain("contract-subject-1");
+    expect(body).not.toContain("att-merchant-profile-create");
+    expect(body).not.toContain("1F-A");
+    expect(body).not.toContain("二期测试商户");
   });
 });
 
@@ -224,7 +238,7 @@ describe("OrganizationMerchantPage 行明细抽屉（三业务表整行可点）
     const drawer = screen.getByTestId("row-detail-drawer");
     expect(drawer.textContent).toContain("平台经营主体");
     expect(drawer.textContent).toContain("91330100MA27XW8H");
-    expect(drawer.textContent).toContain("运营主体");
+    expect(drawer.textContent).toContain("业态主体（管理公司）");
   });
 
   it("点商户行 → 抽屉含商户名/联系人/合作形式中文", async () => {
@@ -234,7 +248,7 @@ describe("OrganizationMerchantPage 行明细抽屉（三业务表整行可点）
     const drawer = screen.getByTestId("row-detail-drawer");
     expect(drawer.textContent).toContain("正常商户甲");
     expect(drawer.textContent).toContain("联系人甲");
-    expect(drawer.textContent).toContain("固定租金");
+    expect(drawer.textContent).toContain("租赁");
     expect(drawer.textContent).not.toContain("fixed_rent");
   });
 
@@ -259,3 +273,59 @@ describe("OrganizationMerchantPage 行明细抽屉（三业务表整行可点）
     await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
   });
 });
+
+describe("OrganizationMerchantPage 存档/删除写路径（M012-008）", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    vi.restoreAllMocks();
+    window.history.replaceState({}, "", "/admin/governance/organizations");
+  });
+
+  function postCalls() {
+    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
+    return fetchMock.mock.calls.filter(([, init]) => (init as RequestInit | undefined)?.method === "POST");
+  }
+
+  it("存档：空理由被拦截不发请求（BSM-004 存档须填理由）；填写理由后 POST /merchants/{id}/archive 且请求体含理由", async () => {
+    const prompt = vi.spyOn(window, "prompt").mockReturnValue("   ");
+    const props = renderPage();
+    await waitFor(() => expect(screen.getByTestId("merchant-archive-m-1")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("merchant-archive-m-1"));
+    expect(props.setGlobalError).toHaveBeenCalledWith("存档理由必填");
+    expect(postCalls()).toHaveLength(0);
+
+    prompt.mockReturnValue("退场清理");
+    fireEvent.click(screen.getByTestId("merchant-archive-m-1"));
+    await waitFor(() => expect(postCalls()).toHaveLength(1));
+    const [url, init] = postCalls()[0]!;
+    expect(String(url)).toContain("/api/v1/platform/merchants/m-1/archive");
+    expect(String((init as RequestInit).body)).toContain("退场清理");
+  });
+
+  it("已存档商户：申请删除 → POST /merchants/{id}/delete-request 带理由", async () => {
+    vi.spyOn(window, "prompt").mockReturnValue("录入错误，退场清理");
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("merchant-actions-m-3")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("merchant-actions-m-3"));
+    await waitFor(() => expect(screen.getByTestId("merchant-delete-request-m-3")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("merchant-delete-request-m-3"));
+    await waitFor(() => expect(postCalls()).toHaveLength(1));
+    const [url, init] = postCalls()[0]!;
+    expect(String(url)).toContain("/api/v1/platform/merchants/m-3/delete-request");
+    expect(String((init as RequestInit).body)).toContain("录入错误，退场清理");
+  });
+
+  it("写路径失败可见：存档接口报错 → setGlobalError 收到错误信息，不成未处理拒绝", async () => {
+    vi.spyOn(window, "prompt").mockReturnValue("退场清理");
+    const props = renderPage();
+    await waitFor(() => expect(screen.getByTestId("merchant-archive-m-1")).toBeTruthy());
+    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
+    fetchMock.mockImplementationOnce(() => Promise.resolve(new Response(
+      JSON.stringify({ success: false, code: "MERCHANT_NOT_FOUND", message: "商户不存在" }),
+      { status: 404, headers: { "content-type": "application/json" } },
+    )));
+    fireEvent.click(screen.getByTestId("merchant-archive-m-1"));
+    // ApiError.message 经 shared/apiError 翻译层改写为中文业务话术（不一定含后端原文），此处只锁「失败被 catch 并以字符串报给 setGlobalError」。
+    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.any(String)));
+  });
+});
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
index 64b9cc0c..7d5b3ee9 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -22,7 +22,8 @@ const TEXT = {
   loadFailed: "组织与商户档案读取失败",
   merchantNameRequired: "商户名称不能为空。",
   merchantTypeRequired: "必须选择商户业态（酒吧／餐饮／零售／游乐／传媒／其他）。",
-  contactPhoneInvalid: "联系电话必须是 11 位中国大陆手机号。",
+  contactPhoneRequired: "联系电话不能为空。",
+  cooperationPlaceholder: "请选择合作形式",
   focusClear: "清除筛选，返回全部",
 };
 
@@ -76,12 +77,13 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
   const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
   const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
   const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
-  const [merchantName, setMerchantName] = React.useState("二期测试商户");
+  // M012-006：不预填验收默认值；商户名/联系人/电话/合作形式均由用户填写或选择。
+  const [merchantName, setMerchantName] = React.useState("");
   // 业态必填 → 不预选，逼招商自己选；选错＝通票分账份额拆给错的业态。
   const [merchantType, setMerchantType] = React.useState("");
-  const [contactName, setContactName] = React.useState("运营联系人");
-  const [contactPhone, setContactPhone] = React.useState("13800000000");
-  const [cooperationForm, setCooperationForm] = React.useState("lease");
+  const [contactName, setContactName] = React.useState("");
+  const [contactPhone, setContactPhone] = React.useState("");
+  const [cooperationForm, setCooperationForm] = React.useState("");
   const [formError, setFormError] = React.useState("");
   // 引导式创建流（一期范式）：主按钮 → 分步弹窗 → 确认创建。
   const [createOpen, setCreateOpen] = React.useState(false);
@@ -161,36 +163,27 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       setFormError(TEXT.merchantTypeRequired);
       return;
     }
-    if (!/^1[3-9]\d{9}$/.test(contactPhone.trim())) {
-      setFormError(TEXT.contactPhoneInvalid);
+    // M012-007：三份 HTML 无联系电话格式规则，只做非空校验（操作员 2.2 答删除前端 11 位大陆手机号正则）。
+    if (!contactPhone.trim()) {
+      setFormError(TEXT.contactPhoneRequired);
       return;
     }
     setFormError("");
     setGlobalError(null);
+    // M012-006：只提交用户填写的档案字段；经营/合同/结算主体由四类主体绑定流程承接（页内边界文案「不在本页替代四类主体绑定」），
+    // 不再静默提交主体 id/区块/面积/备用联系人/租金/状态/验收备注/证据附件（后台对这些均可空，状态缺省由后台决定）。
     const request = {
       idempotencyKey: `merchant-profile-create-${Date.now()}`,
-      operatorNote: "完整后台组织与商户档案验收创建",
-      evidenceAttachmentIds: ["att-merchant-profile-create"],
       merchantName,
       merchantType,
-      businessEntityId: entities[0]?.id ?? "org-1",
-      contractEntityId: "contract-subject-1",
-      settlementEntityId: "settlement-subject-1",
       contactName,
       contactPhone: contactPhone.trim(),
-      mallBlockCode: "1F-A",
-      areaSquareMeter: 128.5,
-      leasedAreaSquareMeter: 120,
-      secondaryContactName: "备用联系人",
-      secondaryContactPhone: "13900000000",
-      rentPerSquareMeter: 12.3,
-      cooperationForm,
-      status: "active",
+      ...(cooperationForm ? { cooperationForm } : {}),
     };
     // 范式约定（引导式流程）：提交失败 = 弹窗留开 + error prop 在弹窗内显错，不静默。
     try {
       const result = await api.post<unknown>("/api/v1/platform/merchants", request);
-      setActionResult({ action: "merchant_profile_created", evidenceAttachmentIds: request.evidenceAttachmentIds, request, result });
+      setActionResult({ action: "merchant_profile_created", request, result });
       setCreateOpen(false);
       await reload();
     } catch (submitError) {
@@ -209,6 +202,13 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
     // eslint-disable-next-line react-hooks/exhaustive-deps
   }, [showArchived, typeFilter]);
 
+  // M012-008：存档/删除写路径失败须可见（与创建流一致），不成未处理拒绝。
+  function reportWriteFailure(error: unknown, fallback: string) {
+    const message = error instanceof Error ? error.message : fallback;
+    setGlobalError(message);
+    setActionResult({ action: "merchant_write_failed", status: "failed", message });
+  }
+
   async function archiveMerchant(id: string, archive: boolean) {
     setGlobalError(null);
     let reason: string | undefined;
@@ -218,34 +218,46 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       if (!r.trim()) { setGlobalError("存档理由必填"); return; }
       reason = r.trim();
     }
-    await api.post(`/api/v1/platform/merchants/${id}/${archive ? "archive" : "unarchive"}`, {
-      idempotencyKey: `merchant-${archive ? "archive" : "unarchive"}-${id}-${Date.now()}`,
-      ...(reason ? { reason } : {}),
-    });
-    setActionResult({ action: archive ? "merchant_archived" : "merchant_unarchived", id });
-    await reload();
+    try {
+      await api.post(`/api/v1/platform/merchants/${id}/${archive ? "archive" : "unarchive"}`, {
+        idempotencyKey: `merchant-${archive ? "archive" : "unarchive"}-${id}-${Date.now()}`,
+        ...(reason ? { reason } : {}),
+      });
+      setActionResult({ action: archive ? "merchant_archived" : "merchant_unarchived", id });
+      await reload();
+    } catch (actionError) {
+      reportWriteFailure(actionError, archive ? "商户存档失败" : "取消存档失败");
+    }
   }
 
   async function requestDeleteMerchant(id: string) {
     const reason = window.prompt("删除理由（提交后需上级审批）：");
     if (reason === null || !reason.trim()) { setGlobalError("删除理由必填"); return; }
     setGlobalError(null);
-    await api.post(`/api/v1/platform/merchants/${id}/delete-request`, {
-      idempotencyKey: `merchant-delete-request-${id}-${Date.now()}`,
-      reason,
-    });
-    setActionResult({ action: "merchant_delete_requested", id });
-    await reload();
+    try {
+      await api.post(`/api/v1/platform/merchants/${id}/delete-request`, {
+        idempotencyKey: `merchant-delete-request-${id}-${Date.now()}`,
+        reason,
+      });
+      setActionResult({ action: "merchant_delete_requested", id });
+      await reload();
+    } catch (actionError) {
+      reportWriteFailure(actionError, "商户删除申请失败");
+    }
   }
 
   async function approveDeleteMerchant(id: string, decision: "approved" | "rejected") {
     setGlobalError(null);
-    const decided = await api.post<{ deleted?: boolean }>(`/api/v1/platform/merchants/${id}/delete-approve`, {
-      idempotencyKey: `merchant-delete-approve-${id}-${decision}-${Date.now()}`,
-      decision,
-    });
-    setActionResult({ action: `merchant_delete_${decision}`, id, deleted: decided?.deleted ?? false });
-    await reload();
+    try {
+      const decided = await api.post<{ deleted?: boolean }>(`/api/v1/platform/merchants/${id}/delete-approve`, {
+        idempotencyKey: `merchant-delete-approve-${id}-${decision}-${Date.now()}`,
+        decision,
+      });
+      setActionResult({ action: `merchant_delete_${decision}`, id, deleted: decided?.deleted ?? false });
+      await reload();
+    } catch (actionError) {
+      reportWriteFailure(actionError, "商户删除审批失败");
+    }
   }
 
   // 状态子集 KPI（正常商户）聚焦时对商户表做客户端筛选：仅过滤已加载的内存数组，不发起后端查询。
@@ -289,7 +301,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
         { label: "项目", value: display(row.name) },
         { label: "编码", value: display(row.code) },
         { label: "商户", value: row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId) },
-        { label: "业态", value: display(row.venueType) },
+        { label: "业态", value: formatBusinessStatus(row.venueType) },
         { label: "状态", value: formatBusinessStatus(row.status) },
       ],
     });
@@ -360,7 +372,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
       <div className="admin-table-scroll" data-testid="organization-projects-section">
         <table className="admin-table" data-testid="organization-projects-table">
           <thead><tr><th>项目</th><th>编码</th><th>商户</th><th>业态</th><th>状态</th><th>详情</th></tr></thead>
-          <tbody>{projects.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-project-row-${row.id}`} onClick={() => openProjectDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openProjectDetail(row); } }}><td>{display(row.name)}</td><td>{display(row.code)}</td><td onClick={(event) => event.stopPropagation()}>{row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId)}</td><td>{display(row.venueType)}</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看项目档案详情" onClick={(event) => { event.stopPropagation(); openProjectDetail(row); }}>查看详情</button></td></tr>)}</tbody>
+          <tbody>{projects.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-project-row-${row.id}`} onClick={() => openProjectDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openProjectDetail(row); } }}><td>{display(row.name)}</td><td>{display(row.code)}</td><td onClick={(event) => event.stopPropagation()}>{row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId)}</td><td>{formatBusinessStatus(row.venueType)}</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看项目档案详情" onClick={(event) => { event.stopPropagation(); openProjectDetail(row); }}>查看详情</button></td></tr>)}</tbody>
         </table>
       </div>
     </>
@@ -450,7 +462,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
           {
             title: "联系人",
             description: "填写商户对接联系人及电话。",
-            validate: () => (/^1[3-9]\d{9}$/.test(contactPhone.trim()) ? null : TEXT.contactPhoneInvalid),
+            validate: () => (contactPhone.trim() ? null : TEXT.contactPhoneRequired),
             content: (
               <>
                 <label>联系人<input data-testid="organization-merchant-contact-name" value={contactName} onChange={(event) => setContactName(event.target.value)} /></label>
@@ -462,7 +474,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
             title: "经营与租赁",
             description: "选择与商户约定的合作形式。",
             content: (
-              <label>合作形式<select data-testid="organization-merchant-cooperation" value={cooperationForm} onChange={(event) => setCooperationForm(event.target.value)}>{COOPERATION_FORMS.map((form) => <option key={form.code} value={form.code}>{form.label}</option>)}</select></label>
+              <label>合作形式<select data-testid="organization-merchant-cooperation" value={cooperationForm} onChange={(event) => setCooperationForm(event.target.value)}><option value="">{TEXT.cooperationPlaceholder}</option>{COOPERATION_FORMS.map((form) => <option key={form.code} value={form.code}>{form.label}</option>)}</select></label>
             ),
           },
         ]}
@@ -471,7 +483,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
           { label: "业态", value: merchantType ? formatBusinessStatus(merchantType) : "未选择" },
           { label: "联系人", value: contactName },
           { label: "联系电话", value: contactPhone },
-          { label: "合作形式", value: cooperationLabel(cooperationForm) },
+          { label: "合作形式", value: cooperationForm ? cooperationLabel(cooperationForm) : "未选择" },
         ]}
       />
       <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
diff --git a/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx b/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
index 70ea0917..2cdcc838 100644
--- a/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
+++ b/apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -5,10 +6,12 @@ import { SubjectBindingsPage } from "./SubjectBindingsPage";
 import { AdminApi } from "../client/adminClient";
 import type { AdminPageProps, EffectivePermissionsState } from "../types";
 
-// 2 绑定：b-1 生效、b-2 终止 → 唯一绑定对象2、生效绑定1。
+// M012-001/005：夹具按后台 BusinessEntityBindingRecord 真实字段（platform 域 snake_case）造数，
+// 走真实 AdminApi（stub fetch）→ unwrap 转 camelCase → 组件。字段再错位（读臆造键）本文件必红。
+// 2 绑定：b-1 生效（active）、b-2 停用（inactive；绑定三态 active/inactive/suspended）→ 唯一绑定对象2、生效绑定1。
 const BINDINGS = [
-  { id: "b-1", business_object_id: "m-1", operator_subject: "运营甲", contract_subject: "合同甲", settlement_subject: "结算甲", status: "active" },
-  { id: "b-2", business_object_id: "m-2", operator_subject: "运营乙", contract_subject: "合同乙", settlement_subject: "结算乙", status: "terminated" },
+  { id: "b-1", merchant_id: "m-1", operation_entity_id: "be_op_1", contract_entity_id: "be_ct_1", settlement_entity_id: "be_st_1", status: "active", effective_from: "2026-06-14", effective_to: null, change_reason: "initial" },
+  { id: "b-2", merchant_id: "m-2", operation_entity_id: "be_op_2", contract_entity_id: "be_ct_2", settlement_entity_id: "be_st_2", status: "inactive", effective_from: "2026-06-01", effective_to: "2026-06-30", change_reason: "contract changed" },
 ];
 
 function envelope(items: unknown[]): Response {
@@ -18,8 +21,21 @@ function envelope(items: unknown[]): Response {
   );
 }
 
+function envelopeOne(item: unknown): Response {
+  return new Response(
+    JSON.stringify({ success: true, code: "OK", message: "success", data: item }),
+    { status: 200, headers: { "content-type": "application/json" } },
+  );
+}
+
 function makeApi(): AdminApi {
-  vi.stubGlobal("fetch", vi.fn().mockImplementation(() => Promise.resolve(envelope(BINDINGS))));
+  vi.stubGlobal(
+    "fetch",
+    vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) => {
+      if (init?.method === "POST") return Promise.resolve(envelopeOne({ ...BINDINGS[0], status: "inactive" }));
+      return Promise.resolve(envelope(BINDINGS));
+    }),
+  );
   return new AdminApi("test-token");
 }
 
@@ -34,11 +50,18 @@ function renderPage() {
     effectivePermissions: ADMIN_PERMS,
   };
   render(<SubjectBindingsPage {...fullProps} />);
+  return fullProps;
+}
+
+function postCalls() {
+  const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
+  return fetchMock.mock.calls.filter(([, init]) => (init as RequestInit | undefined)?.method === "POST");
 }
 
 describe("SubjectBindingsPage KPI 聚焦视图", () => {
   afterEach(() => {
     vi.unstubAllGlobals();
+    vi.restoreAllMocks();
     window.history.replaceState({}, "", "/admin/governance/subject-bindings");
   });
 
@@ -59,6 +82,7 @@ describe("SubjectBindingsPage KPI 聚焦视图", () => {
   it("点“唯一绑定对象”数字 → 全量表", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("subject-bindings-kpi-unique")).toBeTruthy());
+    expect(screen.getByTestId("subject-bindings-kpi-unique").textContent).toBe("2");
     fireEvent.click(screen.getByTestId("subject-bindings-kpi-unique"));
     expect(window.location.search).toContain("kpi=bindings");
     expect(window.location.search).not.toContain("status=");
@@ -86,26 +110,51 @@ describe("SubjectBindingsPage KPI 聚焦视图", () => {
     expect(screen.getByTestId("subject-bindings-table").textContent).not.toContain("m-2");
   });
 
-  it("KPI 子标签与状态列为中文，不含英文 active", async () => {
+  it("KPI 子标签、表头与状态列为中文，不含英文 active/ID", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("subject-bindings-summary")).toBeTruthy());
     expect(screen.getByTestId("subject-bindings-summary").textContent).not.toContain("active");
-    expect(screen.getByTestId("subject-bindings-table").textContent).toContain("有效");
+    const table = screen.getByTestId("subject-bindings-table");
+    expect(table.textContent).toContain("有效");
+    // M012-004：表头字段名全中文（「合同主体编号」而非「合同主体 ID」）。
+    expect(table.querySelector("thead")?.textContent ?? "").not.toMatch(/\bID\b/);
+    expect(table.querySelector("thead")?.textContent ?? "").toContain("合同主体编号");
+  });
+});
+
+describe("SubjectBindingsPage 真实字段渲染（M012-001）", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    vi.restoreAllMocks();
+    window.history.replaceState({}, "", "/admin/governance/subject-bindings");
+  });
+
+  it("表格按后台真实字段渲染商户/三主体编号与变更原因（读臆造键时整行是“-”，本用例必红）", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("subject-bindings-row-b-1")).toBeTruthy());
+    const row = screen.getByTestId("subject-bindings-row-b-1").textContent ?? "";
+    expect(row).toContain("m-1");
+    expect(row).toContain("be_op_1");
+    expect(row).toContain("be_ct_1");
+    expect(row).toContain("be_st_1");
+    expect(row).toContain("initial");
   });
 });
 
 describe("SubjectBindingsPage 行明细抽屉", () => {
   afterEach(() => {
     vi.unstubAllGlobals();
+    vi.restoreAllMocks();
     window.history.replaceState({}, "", "/admin/governance/subject-bindings");
   });
 
-  it("点绑定行 → 抽屉含该行字段（状态中文），Esc 关闭", async () => {
+  it("点绑定行 → 抽屉含该行三主体编号（状态中文），Esc 关闭", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("subject-bindings-row-b-1")).toBeTruthy());
     fireEvent.click(screen.getByTestId("subject-bindings-row-b-1"));
     const drawer = screen.getByTestId("row-detail-drawer");
-    expect(drawer.textContent).toContain("运营甲");
+    expect(drawer.textContent).toContain("be_op_1");
+    expect(drawer.textContent).toContain("be_ct_1");
     expect(drawer.textContent).toContain("有效");
     expect(drawer.textContent).not.toContain("active");
     fireEvent.keyDown(window, { key: "Escape" });
@@ -119,3 +168,50 @@ describe("SubjectBindingsPage 行明细抽屉", () => {
     expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
   });
 });
+
+describe("SubjectBindingsPage 绑定变更提交（M012-002/006/008）", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    vi.restoreAllMocks();
+    window.history.replaceState({}, "", "/admin/governance/subject-bindings");
+  });
+
+  it("未选变更后状态 → 表单报错、不发请求（后台 action 为必填三态）", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("subject-bindings-select-b-1")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("subject-bindings-select-b-1"));
+    fireEvent.change(screen.getByTestId("subject-bindings-change-reason"), { target: { value: "合同到期停用" } });
+    fireEvent.click(screen.getByTestId("subject-bindings-submit-change"));
+    expect(screen.getByTestId("subject-bindings-form-error").textContent).toContain("请选择变更后的绑定状态");
+    expect(postCalls()).toHaveLength(0);
+  });
+
+  it("原因为空 → 表单报错、不发请求（原因不再预填验收句）", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("subject-bindings-select-b-1")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("subject-bindings-select-b-1"));
+    fireEvent.change(screen.getByTestId("subject-bindings-change-action"), { target: { value: "inactive" } });
+    expect((screen.getByTestId("subject-bindings-change-reason") as HTMLInputElement).value).toBe("");
+    fireEvent.click(screen.getByTestId("subject-bindings-submit-change"));
+    expect(screen.getByTestId("subject-bindings-form-error").textContent).toContain("变更原因不能为空");
+    expect(postCalls()).toHaveLength(0);
+  });
+
+  it("选行+选状态+填原因 → 确认后 POST …/change，请求体含 action 与 reason、不含假证据附件", async () => {
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("subject-bindings-select-b-1")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("subject-bindings-select-b-1"));
+    fireEvent.change(screen.getByTestId("subject-bindings-change-action"), { target: { value: "inactive" } });
+    fireEvent.change(screen.getByTestId("subject-bindings-change-reason"), { target: { value: "合同到期停用" } });
+    fireEvent.click(screen.getByTestId("subject-bindings-submit-change"));
+    await waitFor(() => expect(postCalls()).toHaveLength(1));
+    const [url, init] = postCalls()[0]!;
+    expect(String(url)).toContain("/api/v1/platform/business-entity-bindings/b-1/change");
+    const body = String((init as RequestInit).body);
+    expect(body).toContain("inactive");
+    expect(body).toContain("合同到期停用");
+    expect(body).not.toContain("evidence-full-admin");
+    expect(body).not.toContain("完整后台验收变更");
+  });
+});
diff --git a/apps/web/src/admin/pages/SubjectBindingsPage.tsx b/apps/web/src/admin/pages/SubjectBindingsPage.tsx
index c8b6cf29..ce4e76c7 100644
--- a/apps/web/src/admin/pages/SubjectBindingsPage.tsx
+++ b/apps/web/src/admin/pages/SubjectBindingsPage.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -6,10 +7,31 @@ import { AuditTrailHint } from "../components/AuditTrailHint";
 import { HighRiskConfirm } from "../components/HighRiskConfirm";
 import { useAdminResource } from "../hooks/useAdminResource";
 import { useSelectedRow } from "../hooks/useSelectedRow";
-import type { AdminPageProps, SubjectBindingRow } from "../types";
+import type { AdminPageProps } from "../types";
 import { formatBusinessStatus } from "../i18n/statusLabels";
 import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
 
+// M012-001：按后台 BusinessEntityBindingRecord 真实字段读取（AdminApi 已把 snake_case 转 camelCase）；
+// types.ts 的 SubjectBindingRow 七个臆造字段（businessObjectId/operatorSubject/…）后端不存在，本页不再读取。
+type BindingRow = {
+  id: string;
+  merchantId?: string;
+  operationEntityId?: string;
+  contractEntityId?: string;
+  settlementEntityId?: string;
+  status?: string;
+  effectiveFrom?: string;
+  effectiveTo?: string;
+  changeReason?: string;
+};
+
+// M012-002：后台 changeBusinessEntityBinding 必填 action，闭集 active/inactive/suspended（PlatformService 白名单，OpenAPI required）。
+const BINDING_ACTIONS: ReadonlyArray<{ code: string; label: string }> = [
+  { code: "active", label: "启用" },
+  { code: "inactive", label: "停用" },
+  { code: "suspended", label: "暂停" },
+];
+
 // 后端写权限码（PlatformController：/business-entity-bindings/{id}/change）
 const BINDING_MANAGE_PERMISSION = "entity.binding.manage";
 
@@ -23,6 +45,8 @@ const TEXT = {
   submitChange: "提交绑定变更",
   select: "选择",
   focusClear: "清除筛选，返回全部",
+  selectAction: "请选择变更后状态",
+  actionRequired: "请选择变更后的绑定状态（启用/停用/暂停）。",
 };
 
 // KPI 聚焦视图（照 OrganizationMerchantPage 样板）：?kpi=bindings[&status=active]；点 KPI 数字=带参重进本页只渲染绑定表（对应子集）。
@@ -43,27 +67,29 @@ function display(value: unknown) {
 }
 
 export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
-  const [bindings, setBindings] = React.useState<SubjectBindingRow[]>([]);
+  const [bindings, setBindings] = React.useState<BindingRow[]>([]);
   const { setSelectedId, selected } = useSelectedRow(bindings);
-  const [reason, setReason] = React.useState("完整后台验收变更");
+  // M012-006：原因不预填验收句，由用户填写；M012-002：变更后状态由用户选择。
+  const [reason, setReason] = React.useState("");
+  const [action, setAction] = React.useState("");
   const [formError, setFormError] = React.useState("");
   const [submitting, setSubmitting] = React.useState(false);
   const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
   // 行明细抽屉：整行可点 → 该主体绑定完整字段（状态中文·四类主体展开·留痕证据）。
   const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
 
-  function openBindingDetail(row: SubjectBindingRow) {
+  function openBindingDetail(row: BindingRow) {
     setDetail({
-      title: `主体绑定明细 · ${display(row.businessObjectId)}`,
+      title: `主体绑定明细 · ${display(row.merchantId)}`,
       fields: [
-        { label: "业务对象", value: display(row.businessObjectId) },
-        { label: "运营主体", value: display(row.operatorSubject) },
-        { label: "合同主体", value: display(row.contractSubject) },
-        { label: "合同主体编号", value: display(row.contractSubjectId) },
-        { label: "结算主体", value: display(row.settlementSubject) },
-        { label: "结算主体编号", value: display(row.settlementSubjectId) },
+        { label: "商户编号", value: display(row.merchantId) },
+        { label: "运营主体编号", value: display(row.operationEntityId) },
+        { label: "合同主体编号", value: display(row.contractEntityId) },
+        { label: "结算主体编号", value: display(row.settlementEntityId) },
         { label: "状态", value: formatBusinessStatus(row.status) },
-        { label: "留痕证据", value: display(row.auditEvidenceRef) },
+        { label: "生效起", value: display(row.effectiveFrom) },
+        { label: "生效止", value: display(row.effectiveTo) },
+        { label: "变更原因", value: display(row.changeReason) },
         { label: "绑定编号", value: display(row.id) },
       ],
     });
@@ -97,13 +123,13 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
 
   const { loading, error, reload } = useAdminResource(
     async () => {
-      setBindings(await api.get<SubjectBindingRow[]>("/api/v1/platform/business-entity-bindings"));
+      setBindings(await api.get<BindingRow[]>("/api/v1/platform/business-entity-bindings"));
     },
     { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
   );
 
   const activeCount = bindings.filter((row) => row.status === "active").length;
-  const uniqueObjects = new Set(bindings.map((row) => row.businessObjectId).filter(Boolean)).size;
+  const uniqueObjects = new Set(bindings.map((row) => row.merchantId).filter(Boolean)).size;
   // 聚焦时按状态子集过滤内存数组；无子集=全量。
   const visibleBindings = kpiFocus?.status ? bindings.filter((row) => row.status === kpiFocus.status) : bindings;
   const focusTitle = kpiFocus?.status === "active" ? "生效绑定" : "主体绑定";
@@ -113,6 +139,10 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
       setFormError("请先在表格中选择要变更的主体绑定记录。");
       return false;
     }
+    if (!action) {
+      setFormError(TEXT.actionRequired);
+      return false;
+    }
     if (!reason.trim()) {
       setFormError("变更原因不能为空。");
       return false;
@@ -129,14 +159,15 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
     setFormError("");
     setGlobalError(null);
     setSubmitting(true);
+    // M012-002：请求体带后台必填的 action；M012-006：不再提交假证据附件号（证据附件上传能力属既有功能缺口，登记，不在本页发明）。
     const body = {
+      action,
       reason,
       idempotencyKey: `full-admin-subject-binding-change-${selected.id}-${Date.now()}`,
-      evidenceAttachmentIds: ["evidence-full-admin"],
     };
     try {
       const result = await api.post<unknown>(`/api/v1/platform/business-entity-bindings/${encodeURIComponent(selected.id)}/change`, body);
-      setActionResult({ action: "subject_binding_change_submitted", bindingId: selected.id, result, ...body });
+      setActionResult({ action: "subject_binding_change_submitted", bindingId: selected.id, bindingAction: body.action, reason: body.reason, idempotencyKey: body.idempotencyKey, result });
       await reload();
     } catch (actionError) {
       const message = actionError instanceof Error ? actionError.message : "主体绑定变更失败";
@@ -167,11 +198,12 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
                 <div className="admin-kpi-grid" data-testid="subject-bindings-summary">
                   <div className="admin-kpi-card admin-kpi-card--clickable"><span>唯一绑定对象</span><button type="button" className="admin-kpi-value" data-testid="subject-bindings-kpi-unique" onClick={() => enterKpiFocus()}>{uniqueObjects}</button><small>唯一绑定</small></div>
                   <div className="admin-kpi-card admin-kpi-card--clickable"><span>生效绑定</span><button type="button" className="admin-kpi-value" data-testid="subject-bindings-kpi-active" onClick={() => enterKpiFocus("active")}>{activeCount}</button><small>生效中</small></div>
-                  <div className="admin-kpi-card"><span>合同主体</span><strong>{bindings.filter((row) => row.contractSubject).length}</strong><small>合同口径</small></div>
-                  <div className="admin-kpi-card"><span>选中绑定</span><strong className="admin-kpi-value--text" title={selected?.businessObjectId ?? "-"}>{selected?.businessObjectId ?? "-"}</strong><small>{selected?.status ? formatBusinessStatus(selected.status) : "-"}</small></div>
+                  <div className="admin-kpi-card"><span>合同主体</span><strong>{bindings.filter((row) => row.contractEntityId).length}</strong><small>合同口径</small></div>
+                  <div className="admin-kpi-card"><span>选中绑定</span><strong className="admin-kpi-value--text" title={selected?.merchantId ?? "-"}>{selected?.merchantId ?? "-"}</strong><small>{selected?.status ? formatBusinessStatus(selected.status) : "-"}</small></div>
                 </div>
 
                 <div className="admin-form-grid">
+                  <label>变更后状态<select data-testid="subject-bindings-change-action" value={action} onChange={(event) => setAction(event.target.value)}><option value="">{TEXT.selectAction}</option>{BINDING_ACTIONS.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}</select></label>
                   <label>变更原因<input data-testid="subject-bindings-change-reason" value={reason} onChange={(event) => setReason(event.target.value)} /></label>
                 </div>
                 <div className="admin-form-row">
@@ -193,20 +225,19 @@ export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effe
 
             <div className="admin-table-scroll">
               <table className="admin-table" data-testid="subject-bindings-table">
-                <thead><tr><th>{TEXT.select}</th><th>业务对象</th><th>运营主体</th><th>合同主体</th><th>合同主体 ID</th><th>结算主体</th><th>结算主体 ID</th><th>状态</th><th>留痕证据</th><th>详情</th></tr></thead>
+                <thead><tr><th>{TEXT.select}</th><th>商户编号</th><th>运营主体编号</th><th>合同主体编号</th><th>结算主体编号</th><th>状态</th><th>生效起</th><th>变更原因</th><th>详情</th></tr></thead>
                 <tbody>{visibleBindings.map((row) => (
                   <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`subject-bindings-row-${row.id}`}
                     onClick={() => openBindingDetail(row)}
                     onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBindingDetail(row); } }}>
                     <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此主体绑定 ${row.id}`} data-testid={`subject-bindings-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
-                    <td>{display(row.businessObjectId)}</td>
-                    <td>{display(row.operatorSubject)}</td>
-                    <td>{display(row.contractSubject)}</td>
-                    <td>{display(row.contractSubjectId)}</td>
-                    <td>{display(row.settlementSubject)}</td>
-                    <td>{display(row.settlementSubjectId)}</td>
+                    <td>{display(row.merchantId)}</td>
+                    <td>{display(row.operationEntityId)}</td>
+                    <td>{display(row.contractEntityId)}</td>
+                    <td>{display(row.settlementEntityId)}</td>
                     <td>{formatBusinessStatus(row.status)}</td>
-                    <td>{display(row.auditEvidenceRef)}</td>
+                    <td>{display(row.effectiveFrom)}</td>
+                    <td>{display(row.changeReason)}</td>
                   <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看主体绑定详情" onClick={(event) => { event.stopPropagation(); openBindingDetail(row); }}>查看详情</button></td></tr>
                 ))}</tbody>
               </table>
````

---

## 4. §5 闸3 逐行归因（51 hunk / 全部命中）

每一变更行（`+`/`-`，不含 hunk 头与文件头）都能指到队列为「是」的已定稿 ID。版本头按操作员版本铁律随该文件所属 ID。上下文行（空格前缀，含既有 `focusObjectId` 形参/高亮）不是本轮变更。无无 ID 行。

### 4.1 GovernanceTopologyPage.test.tsx（无版本→V0.01；5 hunk）

| hunk | 变更 | 归因 |
|---|---|---|
| `@@ -1,3` | 加 `// 文件版本：V0.01` | 003/004/005 版本铁律 |
| `@@ -6,13` | `entity_role` operation→management_company；`space_type` hall→shop；注释标明 project_id 挂接 | **005**＝角色/空间类型闭集；**003**＝注释（夹具本已有 `project_id`，本轮只改类型码并写明依据） |
| `@@ -105,13` | 新增 describe「真实字段与全中文」：业态/空间类型中文、「已挂接」=1 | **004**＝酒吧/商铺/铺位/可用；**003**＝已挂接非 0 |
| `@@ -120,10` | 抽屉断言加「酒吧」 | 004 |
| `@@ -131,6` | 抽屉断言加「商铺/铺位」 | 004（展示词）/005（shop 夹具） |

### 4.2 GovernanceTopologyPage.tsx（无版本→V0.01；7 hunk）

| hunk | 变更 | 归因 |
|---|---|---|
| `@@ -1,3` | 加版本头 V0.01 | 003/004 版本铁律 |
| `@@ -10,6` | 页内 `SPACE_TYPE_LABELS` 8 码与 `SPACE_STATUS_LABELS` 可用/不可用 | 004（V184 type_name 逐字：楼栋/楼层/区块/区域/商铺/铺位/公共区域/车位/车库/摊位/展位/其他） |
| `@@ -95,7` | `space.venueId`→`space.projectId` | 003 |
| `@@ -122,7` | 抽屉业态 `display(venueType)`→`formatBusinessStatus` | 004 |
| `@@ -136,8` | 抽屉空间类型/状态改页内函数 | 004 |
| `@@ -165,7` | 表列业态同样改 `formatBusinessStatus` | 004 |
| `@@ -184,7` | 空间表类型/状态同样改页内函数 | 004 |

### 4.3 OrganizationMerchantPage.test.tsx（V0.01→V0.02；12 hunk）

| hunk | 变更 | 归因 |
|---|---|---|
| `@@ -1,4` | 版本 V0.01→V0.02 | 004/005/006/007/008 版本铁律 |
| `@@ -9,11` | 夹具 operation→management_company、fixed_rent→lease；加已存档 m-3 | **005**＝闭集；**008**＝m-3 供删除申请 |
| `@@ -29,8` | fetch 对 POST 回成功信封 | 006/008（创建与写路径用例需要） |
| `@@ -51,6` | `renderPage` 返回 props | 008（失败可见要读 `setGlobalError`） |
| `@@ -121,7` | 「运营主体」→「业态主体（管理公司）」 | 005 |
| `@@ -129,13` | 「固定租金」→「租赁」；`bar`→「酒吧」且 `not.toMatch(/\bbar\b/)` | **005**＝合作方式；**004**＝业态中文 |
| `@@ -155,11` | 注释五值→六值，断言加「传媒」 | 005 |
| `@@ -187,11` | 非法手机号「12345」改为断言电话空串 +「联系电话不能为空」 | **007**＝非空校验；**006**＝电话不预填 |
| `@@ -207,7` | POST 体 `not.toContain` 假主体/假证据/区块/预填名 | 006 |
| `@@ -224,7` | 抽屉「运营主体」→「业态主体（管理公司）」 | 005 |
| `@@ -234,7` | 抽屉「固定租金」→「租赁」 | 005 |
| `@@ -259,3` | 新增存档/删除申请/失败可见三例 | 008 |

### 4.4 OrganizationMerchantPage.tsx（V0.01→V0.02；11 hunk）

| hunk | 变更 | 归因 |
|---|---|---|
| `@@ -1,4` | 版本 V0.01→V0.02 | 004/006/007/008 版本铁律 |
| `@@ -22,7` | `contactPhoneInvalid`→`contactPhoneRequired`；加合作方式可空占位文案 | **007** / **006** |
| `@@ -76,12` | 商户名/联系人/电话/合作方式预填清空 | 006 |
| `@@ -161,36` | 正则改非空；创建请求去掉三主体/区块/面积/备用联系人/租金/status/operatorNote/假证据；合作方式仅非空才写入 | **007** / **006** |
| `@@ -209,6` | `reportWriteFailure` | 008 |
| `@@ -218,34` | archive/unarchive、delete-request、delete-approve 包 try/catch | 008（「写路径 catch」；未为每个 verb 单开用例） |
| `@@ -289,7` | 抽屉业态 `formatBusinessStatus` | 004 |
| `@@ -360,7` | 项目表业态列同样 | 004 |
| `@@ -450,7` | 引导流电话校验改非空 | 007 |
| `@@ -462,7` | 合作形式下拉加空 option | 006 |
| `@@ -471,7` | 汇总空合作方式显示「未选择」 | 006 |

### 4.5 SubjectBindingsPage.test.tsx（无版本→V0.01；7 hunk）

| hunk | 变更 | 归因 |
|---|---|---|
| `@@ -1,3` | 加版本头 V0.01 | 001/002/004/005/006/008 版本铁律 |
| `@@ -5,10` | 夹具改真实 snake_case；terminated→inactive | **001**＝字段；**005**＝三态闭集 |
| `@@ -18,8` | POST 成功信封 | 002 |
| `@@ -34,11` | 返回 props；`postCalls` 辅助 | 002/006/008 |
| `@@ -59,6` | 唯一绑定对象断言为 2 | 001 |
| `@@ -86,26` | 表头「合同主体编号」、不含 `\bID\b`；新增真实字段渲染用例 | **004**＝编号；**001**＝真实键 |
| `@@ -119,3` | 未选 action / 空原因 / 一条 POST（含 action、不含假证据） | **002**＝action；**006**＝原因空+不传假证据；**008**＝提交路径覆盖收在 002 那一条 POST |

### 4.6 SubjectBindingsPage.tsx（无版本→V0.01；9 hunk）

| hunk | 变更 | 归因 |
|---|---|---|
| `@@ -1,3` | 加版本头 V0.01 | 001/002/004/006 版本铁律 |
| `@@ -6,10` | 去掉 `SubjectBindingRow` 进口；页内 `BindingRow`；`BINDING_ACTIONS` 三态中文 | **001** / **002** |
| `@@ -23,6` | 下拉占位/必填文案 | 002 |
| `@@ -43,27` | 原因空初始；action 状态；抽屉改读真实键（编号） | **006** / **002** / **001**（表头「编号」兼 004） |
| `@@ -97,13` | get 类型与 unique 改 merchantId | 001 |
| `@@ -113,6` | 未选 action 拦截 | 002 |
| `@@ -129,14` | POST 带 action、去掉假证据；`setActionResult` 用 `bindingAction` 避免与事件名 `action` 重复 | **002** / **006**（tsc 去重属 002 落地，不是新业务字段） |
| `@@ -167,11` | 合同主体改 contractEntityId；选中展示 merchantId；加三态下拉 | **001** / **002** |
| `@@ -193,20` | 表头/单元格改真实键与「编号」，去掉「合同主体 ID」与臆造留痕列 | **001** / **004** |

### 4.7 闸3 结论

51 hunk、357+/103- 全部命中队列「是」的 001–008。无 009 行（无下钻用例、无创建绑定入口）。无锁外新判据，无无 ID 行。越界判定见第 7 节。

---

## 5. 逐 ID 修复内容（是否修好 / 是否最懒 / 有无新缺陷或业务发明）

分类顺序冲突→细化→技术性→添加。

### M012-004（冲突／是）— PASS

- **修好**：拓扑页与商户页项目业态由 `display(venueType)` 改为 `formatBusinessStatus`（`bar`→「酒吧」，statusLabels 既有词，本席核对 `customer/i18n/statusLabels.ts:197`）。空间类型不再走 `formatBusinessStatus` 回退英文码，改为页内 8 码表，与 V184 种子 `type_name` 逐字相同。空间状态页内映射 active/unavailable→可用/不可用。绑定表头「合同主体 ID」「结算主体 ID」改为「合同主体编号」「结算主体编号」。测试改断言中文且 `not.toContain("bar")` / `not.toMatch(/\bID\b/)`。
- **最懒**：不改 statusLabels.ts、不请求 `/space-types`。
- **无新发明**：8 码中文不是页面自撰，抄 V184。空间「可用/不可用」是定稿并入的 OBS-18，不是新闭集。

### M012-005（细化／是）— PASS

- **修好**：拓扑夹具 `entity_role=management_company`、`space_type=shop`；商户夹具 `management_company` + `lease`；绑定夹具 `inactive`（不再 `terminated`）。断言「业态主体（管理公司）」「租赁」「商铺/铺位」；下拉仍 `not.toContain("fixed_rent")`。注释「业态五值」改为「V348/V384 业态六值」并补「传媒」。
- **最懒**：夹具只用闭集中的一个角色值，不为三个角色各造一行。
- **无新发明**：三角色/六类合作方式/字典 8 码/绑定三态均是既有后台闭集。

### M012-001（技术性／是）— PASS

- **修好**：页面不再读 `businessObjectId/operatorSubject/contractSubject/contractSubjectId/settlementSubject/settlementSubjectId/auditEvidenceRef`。改读后台真实键。KPI 唯一绑定 `Set(merchantId)`，合同主体数按 `contractEntityId`，选中展示 `merchantId`。夹具改为 `merchant_id` 等 snake_case。本席 RED：未改页面时表格整行是「-」，唯一对象不是 2；GREEN 后行内出现 `m-1/be_op_1/be_ct_1/be_st_1/initial`。
- **最懒**：页内局部 `BindingRow`，不改 types.ts。
- **无新发明**：未把臆造字段写回类型文件。

### M012-002（技术性／是）— PASS

- **修好**：表单「启用/停用/暂停」下拉，值为 active/inactive/suspended。`validateChange` 未选则报中文且不发请求。POST JSON 含 `action` 与 `reason`。提交成功断言只有一条。GREEN-1 的 tsc TS2783（`setActionResult` 展开 body 后 `action` 键重复）在 after 树用 `bindingAction` 收掉，属同一 ID 的编译落地，不是新 API。
- **最懒**：不为非法 action 另写用例——下拉闭集已不可选非法值；未选即失败。
- **无新发明**：三态是 PlatformService/OpenAPI 既有白名单。`bindingAction` 只进页面 `setActionResult` 调试载荷。

### M012-003（技术性／是）— PASS

- **修好**：`attachedSpaces` 改为 `space.projectId && projectIds.has(space.projectId)`。测试断言「1 个已挂接」。本席 RED：未改页面时文案为「0 个已挂接」（`toContain("1 个已挂接")` 失败）。
- **最懒**：一行过滤器替换，不改 SpaceRecord。
- **无新发明**：`projectId` 是既有真实字段。

### M012-008（技术性／是）— PASS

- **修好**：archive/unarchive、delete-request、delete-approve 均 catch 后 `setGlobalError`。测试：存档空理由拦截 + 有理由 POST 同条；已存档行申请删除一条；失败可见一条。本席 RED：失败可见用例在未 catch 的页面上只看到 `setGlobalError(null)`，并出现未处理拒绝 `ApiError: 数据不存在或已被删除`（`apiError.ts:192` 把 404/NOT_FOUND 译成该句，故 GREEN-1 锁「商户不存在」原文必然失败）。after 改为 `expect.any(String)`，仍能失败（RED 已证明），且指向「失败被 catch 并以字符串上报」这一真实行为。
- **最懒**：未为 unarchive/approve 各写用例；catch 覆盖四条写路径但测试按定稿两条+失败可见。m-3 用 `status=suspended` 以免改「正常商户（1）」旧断言。
- **无新发明**：未改存档/删除审批业务规则。

### M012-006（添加／是／操作员答删除）— PASS

- **修好**：预填「二期测试商户 / 运营联系人 / 13800000000 / lease」与绑定原因「完整后台验收变更」全部改空。创建请求只保留用户填写的档案字段 + 可选合作方式，**不出现** businessEntityId/contractEntityId/settlementEntityId/mallBlockCode/areaSquareMeter/leasedAreaSquareMeter/secondaryContact*/rentPerSquareMeter/status/operatorNote/evidenceAttachmentIds（本席读 after `createMerchant` 请求对象全文）。绑定 POST 不再带 `evidence-full-admin`。合作方式下拉有空项。无三主体选择器，创建不读 `entity.binding.view`。本席 RED：电话仍预填 `13800000000`，原因输入框仍是「完整后台验收变更」。
- **最懒**：删除隐藏字段，不新造选择器或附件上传器。
- **无新发明**：未把 entity.binding.view 当创建前置；未发明必填三主体。

### M012-007（添加／是／操作员答删除）— PASS

- **修好**：`/^1[3-9]\d{9}$/` 在 `createMerchant` 与引导流 `validate` 两处均改为 `contactPhone.trim()` 非空。文案「联系电话不能为空。」。本席读 after 全文无该正则。后台未改。
- **最懒**：只改前端即时拦空串，不放宽到后台。
- **无新发明**：未另定电话格式闭集。

---

## 6. TDD 记录与本席复跑

B3 自报（`/tmp/m012_b3r1_run.log` 等，本席只作对照、不以之代替复跑）：

1. RED：先换三份测试、页面仍为 before → 16 失败（绑定 9 / 拓扑 4 / 商户 3），exit 1。
2. GREEN：再改三页面 → 36/37；失败点为失败可见用例锁「商户不存在」，实收翻译句「数据不存在或已被删除」；同时 tsc TS2783 `action` 重复。中间树 `7ea7b6c95938b7839672e6a9b3ad5912c107190e` 不在本轮 B4 的 before/after 对中。
3. GREEN-2：`bindingAction` + `expect.any(String)` → 37/37，tsc 六文件 0 错误。after 树即本轮 `50c07ff…`。

本席独立复跑（不得只信日志）：

| 步骤 | 环境 | 命令 | 结果 |
|---|---|---|---|
| GREEN（after） | 主仓只读 | `npm run --workspace apps/web test:unit -- src/admin/pages/GovernanceTopologyPage.test.tsx src/admin/pages/OrganizationMerchantPage.test.tsx src/admin/pages/SubjectBindingsPage.test.tsx` | exit 0；3 files / **37 passed**；Duration 1.79s |
| RED（after 测试 + before 页面） | `/tmp/m012_b4r1_redmini`（三页面 blob＝before） | 同上 npm workspace 脚本 | exit 1；**16 failed / 21 passed (37)**；另 1 个未处理拒绝（存档 404 无 catch）。失败句与 B3 `/tmp/m012_b3r1_red.log` 同构：业态仍 `bar`、已挂接为 0、真实键整行「-」、电话预填 `13800000000`、无 action 下拉、`setGlobalError` 只有 null |
| tsc（after 副本） | `/tmp/m012_b4r1_grok_tscmini` | `tsc --noEmit -p tsconfig.json` | exit 0；输出空；六文件 0 `error TS` |

新增/改写断言能失败：RED 已把 001 真实字段、002 action 控件、003 已挂接、004 中文业态、006 空预填、007 空电话、008 失败可见全部打红。008 的「空理由存档 / 删除申请」两条在 before 页面上已绿——因为空理由拦截与 delete-request POST 在修复前就存在；008 缺陷本体是缺 catch，由失败可见用例见红。不构成「假绿」。

---

## 7. 越界检查（§5 闸3）

本 diff 路径闭集＝模块 6 文件，与派发及 plan/modules.md M012 FILES 一致。

| 禁止项 | 本席核对 |
|---|---|
| 三份业务 HTML | 两树 blob 相同 |
| 锁 / sidecar | 两树 blob 相同 |
| ledger 基准 / findings | 本 diff 未改（两树相同） |
| 权限种子 / 迁移 | 迁移 ls-tree 全等 |
| 后台源码 | name-only `-- backend/` 空 |
| types.ts / statusLabels.ts | blob 相同 |
| 路由 / 权限码 | 六文件外无变更；页面仍用既有 `entity.binding.manage` / `merchant.profile.manage`，未改码值 |
| 三主体选择器 | after 创建流无 `/business-entities` 下拉；请求不传三主体键 |
| entity.binding.view 创建闸 | 创建路径不读该权限；既有列表降级不在本 diff 新增 |
| 下钻用例 / 创建绑定入口 | 测试无 `focusObjectId` 新用例，无创建绑定按钮/路由 |
| 009 范围 | 无下钻测试、无创建绑定入口 |

`focusObjectId` 仅出现在既有函数签名与行高亮的上下文行，不是本轮新增逻辑。diff 内「三主体」字样只出现在测试标题（001 字段渲染），不是选择器。

越界判定：未越界。PASS。

---

## 8. 问题二分（回退统一规则）

**有定稿 ID、仍属本批修复队列：** 无。001–008 均已按最小边界落地，未见未修完的工程缺陷。

**无定稿 ID（新缺陷/新发明）：** 无。

观察（不进入回退，不阻本席 PASS）：

1. 006 创建 POST 测试锁的是假值（`contract-subject-1` / `att-merchant-profile-create` / `1F-A` / `二期测试商户`），未写 `not.toContain("businessEntityId")` 等键名。若将来改回 `businessEntityId: entities[0]?.id`（值为 `org-1`），该用例不会红。**实现**已省略三主体键本身（V1.41 对修复内容的要求已满足）。属测试锁偏松，不是产品回归。
2. 008 存档空理由 / 删除申请两条在 before 页面已绿；真正迫使 008 见红的是失败可见 + 未处理拒绝。
3. 绑定表状态列仍走 `formatBusinessStatus`，`inactive` 展示「未激活」而下落款是「停用」。statusLabels.ts 不在本模块、定稿禁止改；002 只要求下拉三态中文。属既有词典，不是本 diff 新发明。
4. `TEXT.description` 仍写「必须带原因、幂等键和证据附件」（上下文未改）。006 已不再提交假证据；附件能力缺口属 009 登记口径。
5. 抽屉注释仍含「留痕证据」字样（上下文行，非变更行）。

---

## 9. 结论

| 项 | 结论 |
|---|---|
| 内容修复是否成立 | PASS |
| 范围归因是否全部命中 | PASS |
| 越界（闸3） | PASS（未越界） |
| 总结论 | PASS |

本席不行使否决。B4 双 PASS 与否待席甲自审票合取；合取后才允许把 001–008 队列改否并进入 B5。本票不等于模块 PASS。
