CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M038-A B4-r1 席乙（Grok）修复 diff 只读审计 raw

- 席位：Grok 独立只读审计席乙（第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B4-r1（合成模块 XM-M038-A；审 B3-r1 修复 diff 本身；期间不修）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`；origin/main：`bfb17f8b4f87fbacd1e0284b728901c8e3f4e053`
- `test -f .git/MERGE_HEAD` → **NO_MERGE**
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md`；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（未抄 sidecar / ledger / 其它报告）。观察：sidecar 与 ledger 顶部同值
- findings.md：V3.92；当前定稿段「XM-M038-A B2-r1 双 PASS 定稿，6 项待修（B3-r1）」；修复队列「是」：XM-M038-A-001…006
- 输入 tree：before `6675a7ac2e81512a2c3751b4352d395dc835d2b0` → after `d07213be15b3f7b279b12ccc8cde1817c38ba3a5`
- 冻结 diff：`/home/james/audit-tmp/xm038a_b3r1.diff`；本席 `sha256sum` → `601db987c66600c21b116bca65dbcd06493e7342cf1491e14949f3b0289980c8`；`wc -c` → 5483。`git diff --binary <before> <after>` 与冻结文件 `cmp` 逐字节相同（exit 0）
- 主工作区两测试文件 `git hash-object` = after tree blob：`925cc2c8bd2c7c46eb6baa4aca487caff28722ec` / `be176c2d68879241e82f1d98387d14a375361cd9`
- 独立性：未打开 `modules/XM-M038-A-b4-r1-self-raw.md`。旧 raw 已读：`modules/XM-M038-A-b1-grok-raw.md`（含本席 B2-r1 边界，全文 SHA `ea136303361ffc938ef0988061dc3029033332c35b341a94fd895b35a1521ccc`）。未用 MCP / 子代理 / 第三席；未问人；未改业务 / HTML / 锁 / findings / ledger；未 fetch / commit / push / merge / deploy；未装依赖；未对数据库执行写语句。一次性 worktree `/home/james/wt-xm038a-b4-r1-grok` 已 `git worktree remove`（无 `--force`），路径不存在
- 本席步骤结论：**PASS**（归因票 PASS ∧ 内容票 PASS；任一 FAIL 即 FAIL，本次两票均 PASS）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md。用户指令禁止子代理 / 第三席 / MCP，优先于 Arc 默认全量派席。B4 禁止修复。runbook「阶段 B 不创建 worktree」被本轮用户指令覆盖：前端单测只在基于 origin/main 的一次性 worktree 里进行。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读；用户指令优先于技能 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 路由到 audit；**用户禁子代理 / 禁改业务**，不跑 Arc 默认派评审、不写 `docs/arc/audits/`。`references/platform-tools.md` 在本环境不存在，记缺口 |
| graft | `~/.claude/skills/graft/SKILL.md` | 已读。本任务已给精确路径与 tree id；变更符号是测试文件内局部 `makeApi.getPaged`，不在 wiring 图。未再 `graft ask` |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读。vitest / tsc / worktree 均前台完成； vitest 全量被宿主移入后台后用任务输出工具收到 exit 0，未挂 monitor |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗线自问自答；本体「等用户 / 派子代理」作废 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | TraceLink 仍是深模块（无 navigateTo 或无 objectId → 纯文本）。本 diff 只动浅适配测试桩 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追「读错误 → 复现 → 追数据流」；B4 不加诊断桩、不修。假说用 vitest 输出验证 |
| tdd / test-driven-development | 已读 | 核新断言能否因目标行为变坏而红（006 变异）；本席不写测试 |
| verification-before-completion | 已读 | PASS 前当场独立 sha256、cmp tree/冻结 diff、一次性 worktree BASE/GREEN/006 变异/tsc/全量 |
| requesting-code-review | 存在 | 本体 `HEAD~1` / 派子代理复审作废。本席即评审席，不另派 |
| receiving-code-review | 已读 | 禁止表演性同意；本轮互盲，无对方 raw 可收；B4 禁止 IMPLEMENT |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → NO_MERGE |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 存在；只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B4 不修。review：删除过期 `it` 而非改写；`getPaged` 复用同文件 `get` 的 `startsWith` 路由表；未抽公共夹具。Lean already |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B4 不适用 |
| i-have-adhd | `.../i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 存在；正文只落 raw；CLI 不回带结论或机器标记 |
| audit-context-building | 已读 | 要求派 function-analyzer 子代理：与「只允许两席」冲突，**不执行**，记缺口 |
| batch-grill-me / ubiquitous-language | 本环境未找到 SKILL.md | 缺口；grilling 收窄口径已覆盖 |
| using-git-worktrees | 规程划掉 | 本轮用户指令允许一次性 worktree 跑测，跑完已删除 |

---

## 1. 输入与范围核

`git diff --name-status` / `--numstat` / `--name-only` before→after 仅两路径：

| 路径 | 状态 | 行变 | 模式 before | 模式 after |
|---|---|---|---|---|
| `apps/web/src/admin/pages/TraceLinkDrilldown.test.tsx` | M | +9 / −14 | 100644 `a5a4b639…` | 100644 `925cc2c8…` |
| `apps/web/src/admin/pages/TraceLinkP2c.test.tsx` | M | +6 / −29 | 100644 `8366f3df…` | 100644 `be176c2d…` |

合计 2 files changed, 15 insertions, 43 deletions。文件模式未变（两侧均为 `100644`）。无页面、`TraceLink.tsx`、`AdminApp`、`adminRoutes`、客户端、后端及其余文件。符合 B2-r1 写入集合。

---

## 2. 本轮 diff 全文

```
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

---

## 3. 闸 3 逐块归因表

git 元数据行（`diff --git` / `index` / `---` / `+++` / `@@`）不是源码变更。`index … 100644` 两侧模式相同。下列只列 `+`/`-` 源码行。归属 ID 均取 findings.md V3.92 已定稿且队列「是」的 XM-M038-A-001…006。

### 3.1 `TraceLinkDrilldown.test.tsx` hunk `@@ -17,7 +17,12 @@`

| 符号 | 行（摘要） | ID |
|---|---|---|
| − | `getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),` | XM-M038-A-002 |
| ＋ | `// XM-M038-A-002/005：…桩按同一路由表返回分页形状。` | XM-M038-A-002（注释点了孪生 005，本文件代码属 002） |
| ＋ | `getPaged: vi.fn((url: string) => {` | XM-M038-A-002 |
| ＋ | `const key = Object.keys(routes).find((r) => url.startsWith(r));` | XM-M038-A-002 |
| ＋ | `const items = (key ? routes[key] : []) as unknown[];` | XM-M038-A-002 |
| ＋ | `return Promise.resolve({ items, total: items.length, page: 1, size: 500 });` | XM-M038-A-002 |
| ＋ | `}),` | XM-M038-A-002 |

`startsWith` 与同函数 `get` 同一路由表。`size: 500` 对齐 `GovernanceTopologyPage.tsx` 的 `useServerPagedRows(..., 500, …)`；hook 消费 `result.items` / `result.total`。形状 `{ items, total, page, size }` 即 002 最小处置。

### 3.2 `TraceLinkDrilldown.test.tsx` hunk `@@ -52,24 +57,14 @@`

| 符号 | 行（摘要） | ID |
|---|---|---|
| − | `it("我的对账合同列点击 → navigateTo(ADM-CONT-001, contractId)", …` 整用例 10 行 | XM-M038-A-001 |
| − | `render(<MerchantZoneSettlementsPage {...baseProps(api, {})} />);` | XM-M038-A-006 |
| ＋ | `const { container } = render(<MerchantZoneSettlementsPage {...baseProps(api, {})} />);` | XM-M038-A-006 |
| − | `expect(screen.queryByTestId("trace-link-ADM-CONT-001-c-77")).toBeNull();` | XM-M038-A-006 |
| ＋ | `// XM-M038-A-006：…否定断言改查任何下钻按钮…` | XM-M038-A-006 |
| ＋ | `expect(container.querySelector('[data-testid^="trace-link-"]')).toBeNull();` | XM-M038-A-006 |

`await screen.findByText("c-77")` 未改，保留文本可见。

### 3.3 `TraceLinkP2c.test.tsx` hunk `@@ -2,8 +2,6 @@`

| 符号 | 行 | ID |
|---|---|---|
| − | `import { MerchantZoneContractsPage } from "./MerchantZoneContractsPage";` | XM-M038-A-003（删用例后闲置导入） |
| − | `import { MerchantScopePage } from "./MerchantScopePage";` | XM-M038-A-004（删用例后闲置导入） |

### 3.4 `TraceLinkP2c.test.tsx` hunk `@@ -16,7 +14,12 @@`

与 3.1 同构的 `getPaged` 六行 ＋ / 一行 − → **XM-M038-A-005**（注释同样点了孪生 002，本文件代码属 005）。`size: 500` 对齐 `OrganizationMerchantPage.tsx` 的 `useServerPagedRows(..., 500, …)`。

### 3.5 `TraceLinkP2c.test.tsx` hunk `@@ -61,32 +64,6 @@`

| 符号 | 行（摘要） | ID |
|---|---|---|
| − | `it("我的合同：合同号→ADM-CONT-001、空间→ADM-GOV-007 均可点", …` 整用例 | XM-M038-A-003 |
| − | `it("商户授权工作台：账单商户列点击 → navigateTo(ADM-GOV-002, merchantId)", …` 整用例 | XM-M038-A-004 |

未改的「我的账单」两条仍锁 `ADM-CONT-001`（B2 只登记 GR-07→M285），符合「不把账单页改指写进本批」。`fireEvent` 仍被账单点击用例使用，未误删。

**越界行：0。** 归因票：**PASS**。

---

## 4. 内容审计（定稿最小处置 × 本席 B2-r1 边界）

B2-r1 本席允许/禁止表（`XM-M038-A-b1-grok-raw.md` V0.02 §六）对照：

| ID | 定稿最小处置 | diff 实际 | 判定 |
|---|---|---|---|
| 001 | 删除 Drilldown「我的对账合同列点击 → ADM-CONT-001」用例 | 该 `it` 整块删除 | 合 |
| 002 | `makeApi.getPaged` 按同一路由表返回 `{ items, total, page, size }` | Drilldown 桩改为 `startsWith` 查表并返回该形状 | 合 |
| 003 | 删除 P2c「我的合同：合同号/空间均可点」用例 | 该 `it` 整块删除 | 合 |
| 004 | 删除 P2c「授权工作台商户列点击」用例 | 该 `it` 整块删除 | 合 |
| 005 | 与 002 同一桩 | P2c `makeApi.getPaged` 同构 | 合 |
| 006 | 断言容器内无 `[data-testid^="trace-link-"]`，保留文本 `c-77` | `container.querySelector('[data-testid^="trace-link-"]')` + `findByText("c-77")` | 合 |

禁止项：未改页面 / `TraceLink.tsx` / `AdminApp` / `adminRoutes` / 客户端 / 后端 / 三 HTML / 锁 / 台账；未给商户加平台权限；未抽公共夹具；未把 GR-07 写入本批；未为预部署加 vitest。`AuditEventsPage` 用例自带的 `getPaged` 空桩未动。

顺从式：六条 ID 各有对应最小行，无漏修。对抗式见 §5。ponytail：能删的三条过期 `it` 已删，未改写成第二份本页断言。

内容票：**PASS**。

---

## 5. 新断言能否失败（当场实跑）

一次性 worktree：`git worktree add --detach /home/james/wt-xm038a-b4-r1-grok origin/main`（HEAD `bfb17f8b` = origin/main）。软链主工作区 `node_modules` 与 `apps/web/node_modules`。不装依赖。

1. **BASE**（未打补丁）`npx vitest run src/admin/pages/TraceLinkDrilldown.test.tsx src/admin/pages/TraceLinkP2c.test.tsx`  
   `Tests  5 failed | 6 passed (11)`，`BASE_EXIT=1`。红闭集：对账合同列点击（001）、治理拓扑 focus（002）、我的合同均可点（003）、授权工作台商户列（004）、组织商户 focus（005）。此时「无 navigateTo」旧断言仍绿（查 `trace-link-ADM-CONT-001-c-77`，页面实为 `ADM-MER-006`，否定断言空转）。

2. **APPLY** `git apply /home/james/audit-tmp/xm038a_b3r1.diff` exit 0；worktree `git diff --binary | sha256sum` = `601db987c66600c21b116bca65dbcd06493e7342cf1491e14949f3b0289980c8`。

3. **GREEN** 同两文件：`Tests  8 passed (8)`，`GREEN_EXIT=0`（Drilldown 5 + P2c 3）。

4. **006 变异**：只改 worktree 内 `TraceLink.tsx` 守卫 `if (!navigateTo \|\| !value)` → `if (!value)`（无 navigateTo 也渲染按钮）。`npx vitest run src/admin/pages/TraceLinkDrilldown.test.tsx -t "无 navigateTo"`：  
   `Tests  1 failed | 4 skipped (5)`，`MUT_EXIT=1`。失败点 `TraceLinkDrilldown.test.tsx:67`：`expected <button … data-testid="trace-link-ADM-MER-006-c-77"> to be null`。前缀选择器抓住现行 pageId 的按钮；旧 `queryByTestId("trace-link-ADM-CONT-001-c-77")` 在此变异下仍会空转通过。随后 `git checkout --` 还原 TraceLink。

5. **tsc** `npx tsc --noEmit --pretty false` → `TSC_EXIT=0`。

6. **前端全量** `npx vitest run` → `Test Files  206 passed (206)` / `Tests  978 passed (978)`，`FULL_EXIT=0`。

清理：还原两测试文件与 TraceLink；`rm -f` 两条软链；`git status --porcelain` 空；`git worktree remove` 无 `--force`，`REMOVE_EXIT=0`，路径已不存在。主工作区两测试文件仍为 after blob，本席未改业务。

---

## 6. 对抗式自问（grilling 收窄口径）

- `startsWith` 会不会误匹配？与同文件既有 `get` 相同，属「同一路由表」，不是新匹配器。
- `size: 500` 会不会越界？两页 `useServerPagedRows` 第二参就是 500；hook 用调用方 `pageSize` 算 `pageCount`，用 `result.items` 当行。不是新业务字段。
- 前缀选择器会不会误杀无关 testid？本用例只渲染对账页且不传 `navigateTo`；变异实锤收到的就是 `trace-link-ADM-MER-006-c-77`。
- 001/003/004 删了会不会失去能失败网？B2-r1 已核 M023 本页测试守住现行行为；本席 B4 不重跑那三页变异（定稿已采信删除）。本两文件仍留合同工作台点击、对账无 navigateTo、三处 focus、账单两条。
- 账单 `ADM-CONT-001` 用例未改：正确（GR-07 只登记）。

无新缺陷、无应升队项、无添加、无 HTML 申请。

---

## 7. 双票与步骤结论

| 票 | 结论 | 依据 |
|---|---|---|
| 闸 3 归因 | **PASS** | 全部 `+`/`-` 源码行归到 XM-M038-A-001…006；越界行 0；仅两测试文件；模式 100644 未变 |
| 内容 | **PASS** | 六条均落定稿最小处置与 B2-r1 边界；新 006 断言在「无 navigateTo 仍出按钮」变异下红；GREEN 8/8、全量 978、tsc 通过 |

合取：归因 PASS ∧ 内容 PASS → 本席 B4-r1 **PASS**。本票不是模块 PASS，不是授权改 findings 队列。步骤仍 B4；XM-M038-A IN_PROGRESS；B3=1。未读席甲本轮 raw。
