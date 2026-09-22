CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M043 B4-r2 Grok 完整报告 V0.01

- 席位：Grok（乙，固定 grok-4.6）。Grok FAIL 具否决权。
- 步骤：B4-r2 修复 diff 只读审计。不是 B5，不是模块 PASS，不是授权下一轮 B3。
- 模块：M043 `高危授权审批、临时授权、角色装配重算与主管复核链／网页与验证／第1段`
- 生命周期（只读 ledger）：`IN_PROGRESS`；B3 已执行次数=**2**
- 正本：`/home/james/complex-management` 分支 `main`；本席 `git rev-parse HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`
- 提示词：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- runbook：`docs/audit-2026-09/plan/runbook.md` V0.02
- 计划三文件（`plan/modules.md` FILES）：`SupervisorReviewPage.tsx` / `SupervisorReviewPage.test.tsx` / `SupervisorReviewPage.chain.test.tsx`
- 本席未读本次 B4 self raw、未接触适配器缓冲、未改代码/测试/HTML/锁/ledger/findings/主 index/数据库、无子代理/MCP/第三席、未 commit/push/merge/deploy
- 写入：仅本指定路径（文件原先不存在，本次创建）
- 结论：**PASS**

## 0. 技能自检（prompt §6 收窄）

已读并按收窄口径使用：

| 技能 | 用法 |
|---|---|
| using-superpowers | 开工技能门 |
| grilling | 只借追问结构自问自答，事实自读，不派子代理、不等操作员 |
| requesting-code-review | B4=本席外审；本体派子代理 / HEAD~1 基线 **未用** |
| receiving-code-review | 先核源/命令再接受或反驳修复是否够；禁止表演性同意派发措辞 |
| verification-before-completion | PASS 前实跑测试、typecheck、独算哈希、独立 `git diff --binary` |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不打点、不改码 |
| ponytail-review | 只评 diff 有无越界可删项，不落地 |
| writing-for-agents | 本 raw 结构 |
| git-guardrails | 本席只读，未 commit/push/merge |

缺口（不冒充已调用）：本环境未找到独立可执行的 `grill-me` / `batch-grill-me` CLI、`ubiquitous-language`、`i-have-adhd` 技能文件。`tdd` / `ponytail` 落地属修复席，B4 不用。Arc audit 子代理与双席冲突，未用。graft 本步未检索：范围已由 `plan/modules.md` 三文件与两 tree 钉死。

## 1. 锁哈希（本席独立实算）

命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`

| 源 | 值 |
|---|---|
| ① 本席实算 | `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458` |
| ② sidecar `plan/criteria-lock.sha256` | 同 |
| ③ ledger 顶部 `CRITERIA-LOCK-SHA256:` | 同 |

本报告第一行=①。本席不写 ledger。`LOCK-AMEND-COUNT: 1` 只读。本步不是 B5 后四值闸；④ 取本文件本次独占行，由调度在两席落盘后核。

## 2. 本轮 tree 与独立 diff 命令

派发给定：

- before=`e3939c9899d4464e4b5f2798a6b688125153076b`
- after=`97e3cb99c8e2c7113406158539c6ea0da421af0b`

本席：`git cat-file -t` 两者均为 `tree`。

```
git diff --binary e3939c9899d4464e4b5f2798a6b688125153076b 97e3cb99c8e2c7113406158539c6ea0da421af0b
```

不按路径过滤。实跑：

| 项 | 事实 |
|---|---|
| `--name-status` | 仅 `M` 三文件：`SupervisorReviewPage.chain.test.tsx`、`SupervisorReviewPage.test.tsx`、`SupervisorReviewPage.tsx` |
| `--numstat` | chain 8+/5-；test 10+/3-；page 4+/3- |
| `--stat` | 3 files changed, 22 insertions(+), 11 deletions(-) |
| 两树文件数 | 各 3974；路径集合 `comm -3` 空（无增删路径） |
| page blob | before `96fd5009394b848fde95d3acd73df73749746f70` → after `853ec439d3d7bb2f3766833e4bb0438f510423ae` |
| test blob | before `dafcd37f6c88d7e163dd482e44be97fa468be4d2` → after `6f2cd57355a160e9ae3e7162ab5d4a68a9975f6c` |
| chain blob | before `7d30eb3297999145cb5f01a2020d0cc82c7e8a84` → after `8314976f7db2e1746fc03bef4678a693a8e026d9` |
| 工作区三文件 `git hash-object` | 与 after blob 全等 |
| 真实主 index（`git ls-files -s`） | page=`cedd05d2…`、test=`1bd05611…`、chain=`7d30eb32…`（B3 临时索引协议，主 index 未跟 after；本席未改主 index） |
| 越界对象两树 blob | `types.ts` / `ApprovalChainPage.tsx` / `basePath.ts` / 三份业务 HTML / `SupervisorReviewPolicyRecord.java` **全部 before=after** |
| `package.json` / lock | 本 diff 空 |
| diff 全文 SHA256 | `ae01ab4af55e6ee84a9e67892168d17e6d7e9417fe257f3f35afce81523a3a4c`（本席对本次 `git diff --binary` 计算，8047 字节） |

HEAD 工作区另有大量其它模块未提交文件，**不在这两棵 tree 的 name-status 内**，不纳入本 B4 归因。本 diff 未动三 HTML。

现文行数（`wc -l`，after/工作区）：page 486、test 465、chain 78；`git ls-files -z -- <3 paths> \| xargs -0 cat \| wc -l` = **1029**（before 对应 485+458+75=1018，净 +11）。

现文 SHA256：

| 文件 | SHA256 |
|---|---|
| `SupervisorReviewPage.tsx` | `2f7526d005caf7901d64d3f3a28c27fbf6fe39fb83fbef41c68e55629f5bb46f` |
| `SupervisorReviewPage.test.tsx` | `d9ca54c9f8e8937fd1e7b8f35ab655cf367e258e7f2dc039bddbbaf62e63ab75` |
| `SupervisorReviewPage.chain.test.tsx` | `543e69399eab5f89d9812966cbf791d3a931c45fb21976b3c5b4281671a08862` |

## 3. 输入（B5-r1 两席 + b1 raw 尾部 B2 票 + findings 定稿）

本批由 B5-r1 合取 FAIL 回退 B2，再 B3 第 2 次。本席完整阅读：

| 输入 | 结论 | 本批发现 |
|---|---|---|
| `M043-b5-r1-grok-raw.md` V0.01 | **FAIL** | GR-01 策略行 `key`/`testid`=`row.id`；GR-02 链入口裸 `/admin` href；GR-03 测试不能红前两项且空最终人过声称 |
| `M043-b5-r1-self-raw.md` V0.01 | **PASS** | 无新正式 ID；把真实目录缺 id 的 React key 警告与链测空最终人写成**观察** |
| `M043-b1-grok-raw.md` 尾部 V0.03 B2 | **PASS** | GR-01→009 细化/是/是；GR-02→010 细化/是/是；GR-03→011 技术性/是/是；001—008 不重开 |
| `M043-b1-self-raw.md` 尾部 V0.03 B2 | **PASS** | 同三 ID、同分类/队列；明确不覆盖 Grok 否决 |
| `findings.md` V3.63 顶部 | B2 双 PASS 定稿 | 009/010/011 工程缺陷**是**、队列**是**；001—008 队列**否** |

差集：B5 席甲观察 vs 席乙三票——B2 已按 Grok 否决纳入 009—011，本 B4 不重开分类辩论，只审 after 是否按定稿最小边界修够。B5 席乙 §8 七条观察（按钮仍 enabled、KPI 聚焦、预填对象等）B2 不入队，本 diff 也未去改它们，合法。无添加项、无 HTML 申请、无新判据。

定稿最小边界（本席按 findings 原文，不沿用派发概括当证据）：

- **M043-009** 细化：仅本页策略行 `key` 和 `testid` 改用现有 `policyCode`；不新增后端 id、不改全局类型、不造兼容层；原测试选择器及真实目录逐行唯一性验证同步。
- **M043-010** 细化：本页现有 `a href` 复用已存在 `../../basePath.withBase`，保留 `review_id` 编码与浏览器导航；不新路由、不改链页；链测试根路径/`/complex` 两种。
- **M043-011** 技术性：只在原两测试文件补真实目录 `policyCode` 各行与明细对应、根路径/`/complex` 链 href、空和非空 `finalApprover` 请求体；保留原权限/金额/身份/节点断言，无新业务发明。

## 4. 完整 binary diff 原文

命令见 §2。全文如下（106 行）：

```diff
diff --git a/apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx b/apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx
index 7d30eb32..8314976f 100644
--- a/apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx
+++ b/apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -52,24 +53,26 @@ describe("SupervisorReviewPage 指定最终审批人与审批链入口", () => {
     window.history.replaceState({}, "", "/admin/governance/supervisor-review");
   });
 
-  it("评估时填写最终审批人 → 请求体带 final_approver_id；不填则不带", async () => {
+  it.each(["usr_gm", ""])("评估时最终审批人 %s：填写则带字段，不填则不带", async (approver) => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("supervisor-review-form")).toBeTruthy());
     fireEvent.change(screen.getByTestId("supervisor-review-violation-summary"), { target: { value: "合同主体不一致" } });
-    fireEvent.change(screen.getByTestId("supervisor-review-final-approver"), { target: { value: "usr_gm" } });
+    fireEvent.change(screen.getByTestId("supervisor-review-final-approver"), { target: { value: approver } });
     fireEvent.click(screen.getByTestId("supervisor-review-evaluate"));
     const fetchMock = vi.mocked(globalThis.fetch);
     await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/supervisor-review/evaluate"))).toBe(true));
     const call = fetchMock.mock.calls.find(([input]) => String(input).includes("/supervisor-review/evaluate"));
     const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
-    expect(body.final_approver_id).toBe("usr_gm");
+    if (approver) expect(body.final_approver_id).toBe(approver);
+    else expect(body).not.toHaveProperty("final_approver_id");
   });
 
-  it("待复核清单每行带审批链入口链接（携带 review_id）", async () => {
+  it.each(["", "/complex"])("部署前缀 %s：待复核清单审批链链接保留前缀与 review_id", async (prefix) => {
+    window.history.replaceState({}, "", `${prefix}/admin/governance/supervisor-review`);
     renderPage();
     await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-review_p1")).toBeTruthy());
     const link = screen.getByTestId("supervisor-review-chain-link-review_p1") as HTMLAnchorElement;
-    expect(link.getAttribute("href")).toBe("/admin/governance/approval-chain?review_id=review_p1");
+    expect(link.getAttribute("href")).toBe(`${prefix}/admin/governance/approval-chain?review_id=review_p1`);
     expect(link.textContent).toContain("审批链");
   });
 });
diff --git a/apps/web/src/admin/pages/SupervisorReviewPage.test.tsx b/apps/web/src/admin/pages/SupervisorReviewPage.test.tsx
index dafcd37f..6f2cd573 100644
--- a/apps/web/src/admin/pages/SupervisorReviewPage.test.tsx
+++ b/apps/web/src/admin/pages/SupervisorReviewPage.test.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.03
+// 文件版本：V0.04
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -324,8 +324,8 @@ describe("SupervisorReviewPage 行明细抽屉", () => {
 
   it("点策略行 → 抽屉含该行动作类型（中文）与默认阻断（是/否），无英文枚举，Esc 关闭", async () => {
     renderPage();
-    await waitFor(() => expect(screen.getByTestId("supervisor-review-row-sp-1")).toBeTruthy());
-    fireEvent.click(screen.getByTestId("supervisor-review-row-sp-1"));
+    await waitFor(() => expect(screen.getByTestId("supervisor-review-row-POL-1")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("supervisor-review-row-POL-1"));
     const drawer = screen.getByTestId("row-detail-drawer");
     expect(drawer.textContent).toContain("合同违规");
     expect(drawer.textContent).toContain("是");
@@ -433,6 +433,13 @@ describe("M043 真实审批信封与既有规则", () => {
       const evidenceCell = screen.getByText(row.policy_code).closest("tr")?.children[4];
       for (const code of row.required_evidence_types) expect(evidenceCell?.textContent).not.toContain(code);
     }
+    for (const row of REAL_POLICIES) {
+      const policyRow = screen.getByTestId(`supervisor-review-row-${row.policy_code}`);
+      fireEvent.click(policyRow);
+      expect(screen.getByTestId("row-detail-drawer").textContent).toContain(row.policy_code);
+      fireEvent.keyDown(window, { key: "Escape" });
+      await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
+    }
     expect(table.textContent).not.toContain("future_action");
     expect(table.textContent).not.toContain("future_evidence");
     const pending = screen.getByTestId("supervisor-review-pending").textContent;
diff --git a/apps/web/src/admin/pages/SupervisorReviewPage.tsx b/apps/web/src/admin/pages/SupervisorReviewPage.tsx
index 96fd5009..853ec439 100644
--- a/apps/web/src/admin/pages/SupervisorReviewPage.tsx
+++ b/apps/web/src/admin/pages/SupervisorReviewPage.tsx
@@ -1,5 +1,6 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 import React from "react";
+import { withBase } from "../../basePath";
 import { formatBusinessStatus } from "../i18n/statusLabels";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -366,7 +367,7 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
       <table className="admin-table" data-testid="supervisor-review-table">
         <thead><tr><th>{TEXT.policy}</th><th>{TEXT.actionType}</th><th>{TEXT.reviewerLevel}</th><th>{TEXT.defaultBlocked}</th><th>{TEXT.requiredEvidence}</th><th>{TEXT.status}</th><th>详情</th></tr></thead>
         <tbody>{(kpiFocus ? focusRows : rows).map((row) => (
-          <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`supervisor-review-row-${row.id}`}
+          <tr key={row.policyCode} className="admin-row-clickable" tabIndex={0} data-testid={`supervisor-review-row-${row.policyCode}`}
             onClick={() => openPolicyDetail(row)}
             onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openPolicyDetail(row); } }}>
             <td>{row.policyCode ?? row.id}</td>
@@ -419,7 +420,7 @@ export function SupervisorReviewPage({ api, user, setActionResult, setGlobalErro
                       <td>{formatReviewValue(row.decision.approvalLevel ?? "")}（主管级 {displayValue(row.decision.requiredLevel)}）</td>
                       <td>{row.schedule?.dueAt ? cnDate(row.schedule.dueAt) : "-"}</td>
                       <td>{(row.schedule?.escalationCount ?? 0) > 0 ? `已升级 ${row.schedule?.escalationCount} 次` : "待审"}{row.schedule?.forwardedTo ? `，已转交给 ${row.schedule.forwardedTo}` : ""}</td>
-                      <td><a data-testid={`supervisor-review-chain-link-${row.decision.reviewId}`} href={`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.decision.reviewId)}`}>审批链</a> <button type="button" data-testid={`supervisor-review-pending-select-${row.decision.reviewId}`} onClick={() => { setForwardReviewId(row.decision.reviewId); setLatestReviewId(row.decision.reviewId); }}>{TEXT.selectForForward}</button></td>
+                      <td><a data-testid={`supervisor-review-chain-link-${row.decision.reviewId}`} href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.decision.reviewId)}`)}>审批链</a> <button type="button" data-testid={`supervisor-review-pending-select-${row.decision.reviewId}`} onClick={() => { setForwardReviewId(row.decision.reviewId); setLatestReviewId(row.decision.reviewId); }}>{TEXT.selectForForward}</button></td>
                     </tr>
                   ))}</tbody>
                 </table>
```

## 5. 逐块 / 逐行 ID 归因（闸 3）

队列仍为「是」的定稿 ID 仅 009 / 010 / 011。每一行都能落到其中。版本头随被改文件走，不另开 ID。

| 块 | 文件:行（after） | 改动 | ID |
|---|---|---|---|
| A | `SupervisorReviewPage.tsx:1` | 文件版本 V0.01→V0.02 | 009+010（版本同步） |
| B | `SupervisorReviewPage.tsx:3` | `import { withBase } from "../../basePath"` | 010 |
| C | `SupervisorReviewPage.tsx:370` | `key`/`data-testid`：`row.id`→`row.policyCode` | 009 |
| D | `SupervisorReviewPage.tsx:423` | `href={/admin/...}`→`href={withBase(`/admin/...?review_id=${encodeURIComponent(...)}`)}`；仍是 `<a>`，未改 `navigateTo` | 010 |
| E | `SupervisorReviewPage.test.tsx:1` | 文件版本 V0.03→V0.04 | 009+011（版本同步） |
| F | `SupervisorReviewPage.test.tsx:327-328` | 抽屉选择器 `supervisor-review-row-sp-1`→`…-POL-1`（夹具 `policy_code` 为 POL-1） | 009（选择器同步）+011 |
| G | `SupervisorReviewPage.test.tsx:436-442` | 对 `REAL_POLICIES` 逐行 `getByTestId(supervisor-review-row-${policy_code})`、点行、抽屉含该码、Esc 关闭 | 011（兼 009 能失败验证） |
| H | `SupervisorReviewPage.chain.test.tsx:1` | 补文件版本 V0.01 | 010+011（版本同步） |
| I | `SupervisorReviewPage.chain.test.tsx:56-67` | `it.each(["usr_gm",""])`：填则 `final_approver_id===值`，空则 `not.toHaveProperty("final_approver_id")` | 011 |
| J | `SupervisorReviewPage.chain.test.tsx:70-77` | `it.each(["","/complex"])`：`replaceState` 后断言 href=`${prefix}/admin/governance/approval-chain?review_id=review_p1` | 010+011 |

无 ID 行：无。无「顺手优化 / 换框架 / 改 types / 改 Java / 改链页 / 改 HTML」。

未改而保留（不在本 diff，故不要求本轮归因）：

- 展示列 / 抽屉标题仍 `row.policyCode ?? row.id`（B2 009 明文：展示回退不在必改范围；身份键不得再 `?? row.id`——after 身份键已只用 `policyCode`）。
- 待审表 `key={row.decision.reviewId}`（复核单键，不是策略目录 id 缺陷）。
- `types.ts` 仍 `id: string` 必填（B2 禁止改共享类型）。
- 产品空最终人：`finalApproverId.trim() \|\| undefined` + `adminClient.mapKeys` 过滤 `undefined`（011 只补能失败断言，不改产品空值逻辑）。

## 6. 三 ID 充分性（独立判断，不沿用根绿测）

对照判据：GEN-002 / GEN-008 / GEN-003 / PGM-014 / BEQR-166。本步是 B4，不把 106 映射整表重审写成 B5。

### 6.1 M043-009 — 够

根因：后端 `SupervisorReviewPolicyRecord` 首字段是 `policyCode`，无 `id`；`listPolicies()` 内存 12 条同样无 id。`AdminApi.unwrap` 只做 snake→camel，不会发明 `id`。before 用 `row.id` 当 React key 与 testid，真实目录渲染即全部 `undefined`，testid 撞名。

after：策略 `<tr>` 身份键只用 `row.policyCode`。本席 `rg` 页面无残留 `key={row.id}` / `supervisor-review-row-${row.id}`。未改 `types.ts`、未给 Java 加 id、未写 `id\|\|policyCode` 兼容层。

测试：假夹具 `id:"sp-1", policy_code:"POL-1"` 的抽屉选择器改为 `…-POL-1`。`REAL_POLICIES` 12 码本席实算全部互异，且与后端 12 条 `policyCode` 同集；`getByTestId` 对重复 testid 会抛，故「每行唯一」是能失败断言。点行后抽屉含该 `policy_code`，对应明细。

若身份键仍用 `id`：真实目录用例会在 `getByTestId('supervisor-review-row-staff_shift_publish')` 处红（testid 会变成 `…-undefined` 并重复）。本席未回滚工作区重跑红（只读禁改三文件）；该合取由 before 源码 + after 断言直接推出。

### 6.2 M043-010 — 够

根因：`withBase`（`apps/web/src/basePath.ts:25-26`，本 diff 未改其 blob）在 `pathname.startsWith("/complex")` 时给 App 内根路径补前缀；before 裸 `href="/admin/governance/approval-chain?..."` 在生产整页离开前缀。

after：相对路径 `../../basePath` 从 `admin/pages/` 指向已有模块，正确。`encodeURIComponent(reviewId)` 保留。仍是浏览器 `<a>` 导航，**没有**改成 `navigateTo`（该函数拼 `?focus=`，链页读 `review_id`）。`ApprovalChainPage.tsx` 两树 blob 相同。

测试用 `history.replaceState` 把 pathname 设为 `/admin/...` 与 `/complex/admin/...`，再读锚点 `href`。这与 `basePath.test.ts` 的 `defineProperty(location.pathname)` 不是同一句桩，但是同一运行时输入（`window.location.pathname`）。`withBase` 在 **调用时** 读 location，不是 import 时绑定。本席实跑 `/complex` 枝通过，证明本环境 jsdom 的 `replaceState` 确实改变了 `isDeployed()` 所见 pathname，不是假绿。根路径枝期望无前缀，与 `withBase` 未部署时原样返回一致。

### 6.3 M043-011 — 够

技术性、无业务发明：不新实体/枚举/默认值/路由。

- 真实目录每行唯一 testid + 抽屉对应：§6.1 块 G。
- 根路径 / `/complex` href：§6.2 块 J。
- 空/填最终人：产品 L244 `finalApproverId: finalApproverId.trim() \|\| undefined`；`mapKeys` `filter item !== undefined`；JSON 体无键。`it.each(["usr_gm",""])` 填值断言 `final_approver_id`，空串走 `not.toHaveProperty`。空路径产品本来就对，011 修的是测试过声称。

主文件仍 36 例（权限/金额/身份/节点/中文/附件/转交未删）；链文件由 2 例变为 4 例（参数化，不是新文件、不是新框架）。`it.each` 原测试文件已在用。

### 6.4 001—008

本 diff 未改评估闸、中文映射、节点文案、转交受益人、驳回附件、用户编号标签。队列保持否，不重开。

## 7. 三线（针对本 diff，不是整模块 B5）

### 7.1 第一性原理

1. **为何必须存在**：删 009，PGM-014「敏感操作进复核队列」的策略表在真实目录下无稳定行身份，首次打开即撞 React 调和。删 010，BEQR-166/PGM-014 的「审批链」入口在 `/complex` 部署下离开前缀。删 011，009/010 可再次被假 id 夹具与只测填值的链测藏绿。
2. **声称是否执行**：§8 本席实跑 vitest 40 与 typecheck，不是读代码推测。
3. **未验证前提**：未跑真实浏览器点击「审批链」看 404；未跑 Maven/全后端。010 的生产缺陷由 `withBase` 契约 + 源码 href 推出，B2 已接受该证据类型。本席不声称看见了浏览器 404。

### 7.2 顺从式

假定修复意图正确：009 用已有 `policyCode`；010 复用已有 `withBase` 且保留 `review_id`；011 只补能失败断言。与冻结最小边界逐条对齐。KPI 聚焦与全表共用同一 `policyTable`，身份键一次改两处视图。假夹具仍带 `id`，但身份键已不读它，不能再藏生产路径。

### 7.3 对抗式（grilling 自答）

1. **重复 policyCode？** `REAL_POLICIES` 12 唯一；后端 list 12 唯一。未知码 `unknown-policy-id` 不在 011 循环内，但自身也唯一。
2. **空 policyCode 当 key？** 目录无空码；身份键未回退到 `id`。
3. **replaceState 不改 pathname → `/complex` 假绿？** 本席实跑该例 exit 0；若 `withBase` 仍见根路径，href 不会带 `/complex`，该例必红。
4. **testid 唯一不等于 React key 唯一？** 二者同一字段。若 key 仍为 `id` 而 testid 已改 `policyCode`，真实目录例仍会因 DOM testid 缺失/重复而红。stderr 本席本次无 `unique "key" prop`。
5. **改成 SPA `navigateTo`？** 未改。禁止项未发生。
6. **空最终人只测 `""` 不测空白？** 产品 `trim()`；011 要求的是标题所声称的空/填。空白属极端，B2 观察口径，不升票。
7. **越界改 types/Java/链页/HTML？** 两树 blob 相等。

## 8. 真实验证命令与 exit（本席实跑，不沿用根输出）

工作区三文件 hash-object = after blob。命令在 `apps/web`：

```
npm run test:unit -- src/admin/pages/SupervisorReviewPage.test.tsx src/admin/pages/SupervisorReviewPage.chain.test.tsx
```

- 进程 **exit 0**
- Test Files **2 passed**
- Tests **40 passed / 40**（chain 4 + main 36；相对 B5 的 38 = 链文件 `it.each` 各 +1）
- Duration 1.22s（vitest v4.1.9）
- stderr：**无** `Each child in a list should have a unique "key" prop`；仅有 `NO_COLOR` 与 jsdom `Window's scrollTo()`（与本批无关的既有 jsdom 缺口）

```
npm run typecheck
```

- `tsc -b --noEmit` **exit 0**，stdout 仅脚本名，stderr 空。

派发所述「根 B3 先红 3FAIL37PASS 后 40PASS / typecheck 0，React key 警告消失」：绿侧与 typecheck、stderr 无 key 警告由**本席命令**证实。红侧本席未把工作区换成 before 重跑（只读）；before 源码与 after 断言合取对应抽屉 POL-1、真实目录 testid、`/complex` href 三处会红（38+2=40 中 3 红 → 37 绿），与派发叙述结构一致，但**不是**本席 exit 输出，不拿它当本席验证。

未跑：真实浏览器、带登录 E2E、Maven、`complex_test`、数据库写。不声称那些已完成。

## 9. ponytail-review（只评本 diff）

`Lean already. Ship.`

- 身份键删除错误字段，改用已有 `policyCode`，未加兼容层。
- href 复用已有 `withBase`，未新路由。
- 测试在原两文件 `it.each` / 原 `REAL_POLICIES` 上补能失败断言。
- net: 无应删的新增抽象。`net: -0 lines possible` 作为本 diff 的可删项（不是要求再改产品）。

## 10. 本席本批 findings

无新临时编号。无添加。无锁外主张。无 HTML 铁律 4 申请。

观察（不升票）：`types.ts` 仍把 `id` 写成必填，与后端记录不一致——B2 已禁止本模块改它，归类型模块，不在本 B4 闸 3。展示 `?? row.id` 保留，按 009 边界合法。

## 11. 结论

**PASS**

不是模块 PASS，不是 B5，不是有条件通过。本轮两树完整 binary diff 仅三文件 22+/11-；每一行归因到队列仍为「是」的 009 / 010 / 011；修法落在三文件最小边界内；本席独立 vitest 40 exit 0、typecheck exit 0、真实目录渲染 stderr 不再出现 unique key 警告。001—008 不重开。无越界行。

本席不改 findings/ledger、不修复、不写其它审计产物。
