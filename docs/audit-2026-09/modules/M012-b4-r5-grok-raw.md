CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M012 B4-r5 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r5
席位：固定 Grok 外审席乙 grok-4.6
计划：160 审计 V0.17 §4/B4、§5 闸 3；runbook.md V0.02
本文件路径：docs/audit-2026-09/modules/M012-b4-r5-grok-raw.md
本文件自身版本：V0.01（指定路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r5 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。B3 累计第 5 次（操作员本轮明确「追加一次修复，仅处理上述两项及对应测试」；授权第 5 次、非第 6 次）只审本对 tree 的修复内容。席甲自审票本席未读；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。不得以根绿测、旧 B4-r4 PASS 或本席 B2 V0.08 PASS 代替本轮对这 6 hunk 的核验。

未读取：`docs/audit-2026-09/modules/M012-b4-r5-self-raw.md`（路径存在：9569 字节、mtime 2026-09-22 17:01，按互盲禁读正文）。未读适配器缓冲、未读席甲本次 B4 意见、未读原始 CLI 给对方。定稿队列以 findings.md V3.67 顶部「M012 当前锁 B1→B2 双PASS定稿」为准：仅 M012-018、M012-019 队列是；M012-009（续记）观察否；旧 001—017 保持否。本席 B2 V0.08（`M012-b1-grok-raw.md` 最后一次完整 B2）同意的最小修法作为执行绑定对照，不把 B2 票本身当证据。席甲 B2 V0.08 只核标题/结论行「本席B2：PASS」存在，不读其 B4。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；仅 018/019 是否按 B2 同意边界修好；每一行能否归因到队列为是的已定稿 ID；是否越界改后台 / HTML / 锁 / sidecar / ledger 基准 / 拓扑页 / 绑定页 / 新定位器 / 新接口 / 重开 009。未新造判据/角色，未询问操作员，未 LOCK-AMEND。未中途修，未只审单条。审完整份 6 hunk 后才判定。

---

## 0. 技能自核（§6，不冒充）

本席核到且按收窄口径使用：

| 技能 | 路径 | 本轮用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能优先；操作员口径覆盖通用派子代理 |
| requesting-code-review | 同 superpowers | B4 请审=本席即外审；禁止 `HEAD~1` 基线、禁止子代理复审、禁止无 ID 即刻修 |
| receiving-code-review | 同 superpowers | 本席是外审，不 IMPLEMENT |
| verification-before-completion | 同 superpowers | PASS 前在 `/tmp` 隔离 after 树实跑 59 单测 + typecheck + 红 4FAIL |
| systematic-debugging | 同 superpowers | 只借读错误→复现→追数据流；不改生产码、不加诊断桩 |
| test-driven-development / tdd | superpowers + mattpocock | 不改工作区回放；用 after 测试 + before 页面在 `/tmp` 复现 4 条失败语义 |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗结构自问自答；事实自取，不等操作员、不派子代理 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 横幅仍走原 `organization-merchant-focus` 缝；可见性复用已有 `visibleMerchants` / `kpiFocus` / `AdminDataState` |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 红环=无条件「已定位」横幅与当前表命中不一致；旧 prompt 把密钥直删写成一律上级审批 |
| ponytail / ponytail-review | `.../ponytail/4.9.0/skills/` | 只改一句 prompt + 一条横幅守卫 + 原测试最小断言。Lean already. |
| writing-for-agents | 存在 | 本 raw 给下游 B5/调度读 |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 存在；可选。本席不安装 hook、不写 `.git`、不设 `GIT_INDEX_FILE` |

缺口（不冒充已调用）：`batch-grill-me` 不存在；`ubiquitous-language` 不存在。划掉的 worktree/子代理/research/Arc 全仓 audit 未调用。graft SKILL.md 已读；本轮权威证据是两 tree `git diff --binary`，未调 graft CLI/MCP（V0.17 禁 MCP/子代理；graft 工具会刷新图，可能写 `graft/`，本席不用）。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum /home/james/complex-management/docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

```
python3 hashlib  b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458  bytes=891182
sha256sum        b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
openssl dgst     SHA2-256(...)= b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
```

sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger **顶行**同值。ledger 正文另有历史行 `CRITERIA-LOCK-SHA256: cf25d716…`（约 L7088），不当作当前基准。criteria-lock 版本 V0.14。`LOCK-AMEND-COUNT: 1`。本批 B3 两树 lock blob 均为 `69dccbb728feb863971f2fd7aca76da47db11e64`（内容 SHA256 同上，未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M012 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／网页与验证／第1段（6 文件） |
| 阶段 | B4-r5 只读审计 B3 累计第 5 次（操作员追加一次已批准）的「修复内容」本身 |
| 正式队列「是」（2） | 仅 M012-018、M012-019 |
| 正式队列「否」 | M012-009（续记，十项观察）及旧 001—017（已修/登记，本批不重开） |
| B3 | ledger 模块表当前 B3 列为 5；备注仍写「未获第5次许可」属台账陈旧句，**不在本对 tree diff 内**，本席不改 ledger。用户本轮指令明确授权第 5 次、非第 6 次 |
| findings.md | V3.67；工作区 SHA256 `4ae89af55ae73e60f09e4f85b1d168b3125514b794aeefa42a59f01e338232ba`（本 diff 未改 findings） |
| before（对象类型 tree，可解析） | `c218cfb17edcfdf278a96cf9b3c6e5b32192fd2e` |
| after（对象类型 tree，可解析） | `3eca41eb9aa21c6791ef70d5695deb940abeb992` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary c218cfb17edcfdf278a96cf9b3c6e5b32192fd2e 3eca41eb9aa21c6791ef70d5695deb940abeb992`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 6175 bytes；SHA256 `794a06ffc17723e33a55f27ddb66a0d6f295c051bf70c573a297c83c2ef241f1`（`git diff` 管道 / `/tmp/m012-b4-r5.diff` 同值）；UTF-8；无 NUL；93 行；2 路径；48 插入 / 4 删除；6 hunk；name-status 2×M |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3979 / after 3979；`comm -3` 空（无增删路径） |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`，分支 `main` |
| HEAD^{tree} | `ee1833d2672e5372ef7b056db4f92d48e6de875a` ≠ after。before/after 是完整全树临时索引快照，不是 HEAD diff |
| 工作区两改动文件内容 SHA256 | 全部等于 after（见 §1.1）。主工作区其它文件有未提交脏状态；本席证据不采工作区整树，只用两冻结 tree + `/tmp` 隔离 after 源 |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `6e9d813c985406aaf14c1645af2b494d342ae657`；三者两树完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |
| PlatformService.java 两树 blob | `63e53b3177610f87a5b49e82173e375dbaa7d951` 相同 |
| PermissionGate.tsx 两树 blob | `7f39e56f21fbfa6e34fff19bc609242517042f46` 相同 |
| `git diff --name-only -- backend/ docs/business-*.html docs/platform-governance-map-for-operations-v0.1.html docs/audit-2026-09/` | 空 |

冻结映射仍为 `M012 → [BSM-001, BSM-003, GEN-001 … GEN-018, PGM-001]`，21 项，未增判据。BSM-004 仍不在映射；HTML map 570/576–577 只作 019 的类型 1 解释，与 B2 口径一致。

### 1.1 六源 blob / 内容哈希 / 行数

git blob（`git rev-parse <tree>:path`）：

| 文件 | before blob | after blob | 版本 | 行数 before→after |
|---|---|---|---|---|
| GovernanceTopologyPage.tsx | `89af88ebf200d11fcd1d47082a788411311cd047` | 同 before | V0.04 未动 | 274→274 |
| GovernanceTopologyPage.test.tsx | `5bf4bf26411f020c81780c66681b6485099f539f` | 同 before | V0.04 未动 | 308→308 |
| OrganizationMerchantPage.tsx | `0255d620a4f9572d1a531f78ef5b45eb0ccaf728` | `2f1f1cbd965ef6c747228ab402f5d7cd951de3f1` | V0.05→V0.06 | 520→524 |
| OrganizationMerchantPage.test.tsx | `b2149d21ac69a26ebfb87d4d98632467dae50bd0` | `ce494c7535024948b157e9cf15c2c42fbcbdbfdc` | V0.07→V0.08 | 435→475 |
| SubjectBindingsPage.tsx | `0d916b81f72c4d8ea0acf46e43e85121e2ba71db` | 同 before | V0.03 未动 | 257→257 |
| SubjectBindingsPage.test.tsx | `e145f2b54a5c3271d7601c382044486e37720872` | 同 before | V0.04 未动 | 291→291 |

内容 SHA256（`git cat-file -p \| sha256sum`）：

| 文件 | before | after |
|---|---|---|
| OrganizationMerchantPage.tsx | `d3ddb2cc423cc901190e8a5d9c5c6f2e4095919d956a19a6c866124947fa6722` | `55dc9a69205df3254c4031015bfe53907a3b1fbd7605e97c8f64a8ab515cb328` |
| OrganizationMerchantPage.test.tsx | `d40c83d948e0b07f83f6eb5bcbb2a18af12b186ddd93fdcc651557d5870408a7` | `3e91c3bbd589558e50bab1192036db2179b0dbdd4158fb0841eba1b0184d33e1` |
| 其余四文件 | 与 B2 V0.08 六源表相同 | 与 before 逐字节相同 |

六文件合计 2085→2129 行。仅组织页源+原测试两文件有 diff；拓扑/绑定四文件字节级未变。组织页 before blob `0255d620…` 等于上轮 B4-r4 after，本批相对已验收 r4 after 只叠加 018/019。

`git diff --stat --binary`：

```
 .../admin/pages/OrganizationMerchantPage.test.tsx  | 42 +++++++++++++++++++++-
 .../src/admin/pages/OrganizationMerchantPage.tsx   | 10 ++++--
 2 files changed, 48 insertions(+), 4 deletions(-)
```

`git diff --numstat --binary`：`41 1` 测试、`7 3` 页面。

工作区 `git show :OrganizationMerchantPage.tsx`（index）仍为 before 内容哈希；工作区未暂存正文等于 after。本席未改真实索引。

---

## 2. 定稿依据（B2 双 PASS；本批仅 018/019）

findings.md V3.67 顶部（摘录，未改写）：

- **M012-018** 细化 / 工程缺陷是 / 队列是。事实：横幅仅凭 `focusObjectId` 显示「已从下钻定位到商户…在下表查看」，与 168–177 滚动守卫及当前可见表不一致。最小处置：仅在目标实际出现在当前可见商户表时显示已定位；未命中或未展示商户表时收窄为「当前列表尚未定位到该商户，请检查筛选或翻页」。保留原生分页/过滤，不新加接口、跨页扫描或定位器。原 test 补命中/未命中和隐藏表反馈断言。
- **M012-019** 细化 / 工程缺陷是 / 队列是。事实：`window.prompt("删除理由（提交后需上级审批）：")` 与 `PlatformService.requestDeleteMerchant` 密钥本人立即软删、审批须最高权/禁自审不符。最小处置：将现提示收窄为「删除理由（由最高权限审批；持授权 U 盘密钥的最高权限本人发起时直接执行）：」。仅现提示及最小测试，不改 HTML、后台、权限、删除或恢复功能。
- **M012-009（续记）** 技术性/观察 / 缺陷否 / 队列否。十项观察完整保留。001—008 及 010—017 保持否。

本席 B2 V0.08：**PASS**（同意上述分类与最小修法；明确不授权本席用报告制造第 5 次 B3——第 5 次现由操作员本轮指令给出）。席甲 B2 V0.08 标题段结论「本席B2：PASS」。用户本轮：「M012 追加一次修复，仅处理上述两项及对应测试」。

HTML 类型 1（不新增映射，BSM-004 不进 M012）：

- `business-system-map-for-operations-v0.1.html:570`「由具备最高权限的人审批」
- 同文件 `:576` 与 `:577` 均含「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」

after 树 `PlatformService.java`（blob 未变）`:283–295`：`isMasterAuthority()` 查占用后立即软删；非密钥最高权限只写 `pending`。`:328–335`：非最高权限禁止审批；申请人禁自审。本 diff 未改该文件。

---

## 3. 完整两 tree 无过滤 diff 全文

命令：`git diff --binary c218cfb17edcfdf278a96cf9b3c6e5b32192fd2e 3eca41eb9aa21c6791ef70d5695deb940abeb992`

字节 6175，SHA256 `794a06ffc17723e33a55f27ddb66a0d6f295c051bf70c573a297c83c2ef241f1`。下文为独立重取的整份原文，未过滤、未截断。

```diff
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
index b2149d21..ce494c75 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.07
+// 文件版本：V0.08
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import userEvent from "@testing-library/user-event";
 import { afterEach, describe, expect, it, vi } from "vitest";
@@ -314,6 +314,8 @@ describe("OrganizationMerchantPage 存档/删除写路径（M012-008）", () =>
     fireEvent.click(screen.getByTestId("merchant-actions-m-3"));
     await waitFor(() => expect(screen.getByTestId("merchant-delete-request-m-3")).toBeTruthy());
     fireEvent.click(screen.getByTestId("merchant-delete-request-m-3"));
+    // M012-019：申请框须说明密钥本人会直接执行，不能承诺所有提交均待审批。
+    expect(window.prompt).toHaveBeenCalledWith("删除理由（由最高权限审批；持授权 U 盘密钥的最高权限本人发起时直接执行）：");
     await waitFor(() => expect(postCalls()).toHaveLength(1));
     const [url, init] = postCalls()[0]!;
     expect(String(url)).toContain("/api/v1/platform/merchants/m-3/delete-request");
@@ -433,3 +435,41 @@ it("M012-013：重试成功后清除全局读取错误", async () => {
   await screen.findByTestId("organization-merchants-table");
   expect(props.setGlobalError).toHaveBeenLastCalledWith(null);
 });
+
+describe("M012-018：下钻提示只报告当前可见商户", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    vi.restoreAllMocks();
+    window.history.replaceState({}, "", "/admin/governance/organizations");
+  });
+
+  it("后页目标未加载时不称已定位，翻页看到目标后才显示成功", async () => {
+    const rows = Array.from({ length: 501 }, (_, i) => ({ ...MERCHANTS[0], id: "m-focus-" + i }));
+    renderPage((url, init) => {
+      if (init?.method === "POST" || !url.includes("/merchants")) return undefined;
+      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
+      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
+    }, { focusObjectId: "m-focus-500" });
+    expect(screen.getByTestId("organization-merchant-focus").textContent).not.toContain("已从下钻定位到商户");
+    await screen.findByTestId("organization-merchant-row-m-focus-0");
+    expect(screen.queryByTestId("organization-merchant-row-m-focus-500")).toBeNull();
+    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("当前列表尚未定位到该商户，请检查筛选或翻页");
+    fireEvent.click(screen.getByTestId("organization-merchants-pager-next"));
+    await screen.findByTestId("organization-merchant-row-m-focus-500");
+    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("已从下钻定位到商户：m-focus-500");
+  });
+
+  it.each([
+    ["?kpi=merchants&status=active", "m-2", "organization-merchants-table"],
+    ["?kpi=projects", "m-1", "organization-projects-table"],
+  ])("目标被当前视图隐藏时不称已定位：%s", async (query, target, table) => {
+    window.history.replaceState({}, "", "/admin/governance/organizations" + query);
+    renderPage(undefined, { focusObjectId: target });
+    await screen.findByTestId(table);
+    expect(screen.queryByTestId("organization-merchant-row-" + target)).toBeNull();
+    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("当前列表尚未定位到该商户，请检查筛选或翻页");
+    fireEvent.click(screen.getByTestId("organization-kpi-focus-clear"));
+    await screen.findByTestId("organization-merchant-row-" + target);
+    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("已从下钻定位到商户：" + target);
+  });
+});
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
index 0255d620..2f1f1cbd 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.05
+// 文件版本：V0.06
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -253,7 +253,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
   }
 
   async function requestDeleteMerchant(id: string) {
-    const reason = window.prompt("删除理由（提交后需上级审批）：");
+    const reason = window.prompt("删除理由（由最高权限审批；持授权 U 盘密钥的最高权限本人发起时直接执行）：");
     if (reason === null || !reason.trim()) { setGlobalError("删除理由必填"); return; }
     setGlobalError(null);
     try {
@@ -413,7 +413,11 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
     <section data-testid="organization-merchant-page">
       <AdminPageHeader title={TEXT.title} description={TEXT.description} />
       {focusObjectId && (
-        <div className="admin-state" data-testid="organization-merchant-focus">已从下钻定位到商户：{focusObjectId}（在下表中查看其档案与归属）</div>
+        <div className="admin-state" data-testid="organization-merchant-focus">
+          {!loading && !error && (!kpiFocus || kpiFocus.table === "merchants") && visibleMerchants.some((row) => row.id === focusObjectId)
+            ? <>已从下钻定位到商户：{focusObjectId}（在下表中查看其档案与归属）</>
+            : "当前列表尚未定位到该商户，请检查筛选或翻页"}
+        </div>
       )}
       <TechDetails><div className="admin-generic-evidence" data-testid="organization-merchant-evidence">{TEXT.evidence}</div></TechDetails>
       {formError ? <div className="admin-inline-error" data-testid="organization-merchant-form-error">{formError}</div> : null}
```

---

## 4. 逐块 ID 归因（§5 闸 3）

每一变更行必须落到 findings 当前值为「是」的已定稿 ID。本批仅 018、019 为是。

| hunk | 位置 | 变更 | 归属 ID | 闸 3 |
|---|---|---|---|---|
| H1 | `OrganizationMerchantPage.test.tsx` `@@ -1,4` | 文件版本 V0.07→V0.08 | M012-018 + M012-019 | 通过。AGENTS 自身版本铁律：本文件因两 ID 被改，版本 +1，不是第三功能 |
| H2 | 同文件 `@@ -314,6` | 在既有删除申请用例上断言新 prompt 全文；原 POST `/delete-request` 与理由体仍在 | M012-019 | 通过。B2「原测试对 prompt 文案的最小断言」 |
| H3 | 同文件 `@@ -433,3` | 新增 describe：后页未命中/翻页命中；`it.each` 状态筛选隐藏 m-2、KPI 项目表隐藏商户表；清除筛选后命中。复用 `organization-merchant-focus` 与既有 `renderPage`/`envelope`/500 页大小 | M012-018 | 通过。命中/未命中/隐藏表/加载（首条在 `findByTestId` 行之前断言不出现「已定位」） |
| H4 | `OrganizationMerchantPage.tsx` `@@ -1,4` | 文件版本 V0.05→V0.06 | M012-018 + M012-019 | 通过。同 H1 |
| H5 | 同文件 `@@ -253,7` | 仅替换 `window.prompt` 字符串；仍 POST `.../delete-request`；理由非空、catch、权限按钮未动 | M012-019 | 通过。与 findings 最小处置逐字相同 |
| H6 | 同文件 `@@ -413,7` | 横幅仍 `data-testid="organization-merchant-focus"`。成功文案仅当 `!loading && !error && (!kpiFocus \|\| kpiFocus.table === "merchants") && visibleMerchants.some(id)`；否则指定收窄句 | M012-018 | 通过。未见新定位器、新接口、跨页扫描 |

无第三路径、无拓扑/绑定 hunk、无 HTML/后台/锁 hunk。无「无 ID」行。

滚动守卫 after `:169–177` 仍为 `merchants.some` + 既有 `querySelector`；before/after `querySelector` 出现次数均为 3。删除 prompt 出现次数均为 1。未改 `requestDeleteMerchant` 的网络形状。

---

## 5. 修复内容：018 是否按最小边界落地

**声称（B2/findings）：** 当前可见商户表匹配才提示已定位；未命中/加载/错表收窄为指定句；不增定位器。

**after 实现（`:415–420`）：**

1. **命中：** `visibleMerchants` 已是 KPI 状态子集过滤后的当前页数组（`:286–287`），`.some(row.id === focusObjectId)` 才渲染原成功句「已从下钻定位到商户：{id}（在下表中查看其档案与归属）」。
2. **错表：** `(!kpiFocus \|\| kpiFocus.table === "merchants")`。`kpi=projects` / `kpi=entities` 时商户表不渲染（`:427–434` 只渲染一张表），即使内存 `merchants` 含目标也不称「在下表查看」。
3. **加载/读错：** `loading`/`error` 来自三资源 `useServerPagedRows`（`:160–161`）。`AdminDataState`（after 树未改）在 `loading` 时只渲染「加载中...」、在 `error` 时只渲染错误、**不渲染子表**。横幅在 `AdminDataState` 之外，故加载/错读时表未展示，收窄句成立。这不是新错误通道，是「未展示商户表」的既有 DOM 事实。
4. **未命中：** 当前页或状态过滤后 `visibleMerchants` 无该 id → 收窄句。501 条、page size 500（`:144–148` 与既有测试同一契约），`m-focus-500` 在第 2 页。
5. **不增定位器：** 未新增 testid、未新增搜索接口、未改滚动 effect、未扫描其它页。

夹具：`MERCHANTS[1]`（m-2）`status: "suspended"`，`?kpi=merchants&status=active` 时被 `visibleMerchants` 滤掉且行不在 DOM，与 `expectActiveMerchantsFocusView` 既有「不见 m-2」一致。

**第一性原理：** 删掉这条收窄，HTML 没有「横幅句式」条目，但页面自己的「已定位 / 在下表查看」是可执行声称（GEN-002/009/015、BSM-001、PGM-001）。before 在第 2 页目标、筛选隐藏、项目表聚焦时仍写成功句，是伪成功。after 把声称绑到当前可见表。

**顺从式：** 与 018 最小处置字面一致；分页/业态/存档筛选保留；成功句未发明新业务状态。

**对抗式 / grilling（自问自答）：**

1. 是否把 013 的「501 项目」重开？否。本用例是本项目商户分页，B2 已区分。
2. 是否新定位器？否。仍用原横幅节点。
3. `!error` 是否多余？否。error 时 `AdminDataState` 不展示商户表，属于「未展示」。
4. 加载时收窄句含「请检查筛选或翻页」是否新文案发明？否。B2/findings 指定唯一收窄句，加载归「未展示」。
5. 是否测试私有布尔而非用户可见句？否。断言的是横幅 `textContent` 字面量。

018 **充分**。不把未单列的「error+focus」用例写成新缺陷：最小处置未要求单独 error 用例，且 error 时表本就不渲染。

---

## 6. 修复内容：019 是否按最小边界落地

**声称：** 删除 prompt 复述最高权审批及密钥本人直接执行；不改后台/HTML。

**after `requestDeleteMerchant`（`:255–268`）：** prompt 字符串与 findings 最小处置、席甲 B2、本席 B2 **逐字相同**。随后仍：`reason` 空则 `删除理由必填`；`POST /api/v1/platform/merchants/${id}/delete-request`；失败走既有 `reportWriteFailure`。批准/驳回函数、权限码、按钮集未动。

Python 存在性断言（exit 0）：`after_has_new_prompt=True`；`after_has_old_prompt=False`；`before_has_old=True`；`before_has_new=False`。

两树 `PlatformService.java` blob 全等；三 HTML blob 全等。未把禁自审写进申请框（B2 明确那是审批入口）。

原测试仍覆盖 POST 与理由体；H2 只加 prompt 全文断言，能红（见 §8）。

019 **充分**，未扩到 Bar/票务同文案、未改删除状态机。

---

## 7. 越界与 009

| 检查 | 结果 |
|---|---|
| 无 ID 行 | 无。6 hunk 均落到 018 和/或 019 |
| 重开 009 | 无。未补附件、绑定创建、项目分配、项目资料、KPI 英文键、Date.now、掩码 |
| 新功能 | 无。无新接口、无跨页扫描、无新权限、无新 HTML 映射、无 LOCK-AMEND |
| 后台 / 三份 HTML / 锁 | 两树 blob 全等 |
| 拓扑页 / 绑定页 | blob 全等 |
| 测试框架替换 | 无。仍 vitest + 原 `renderPage` |
| 顺手优化 | 无。无格式化整文件、无抽象层 |

ponytail-review：`net: -0`。Fragment 因插值 JSX 必需；不把横幅抽成新组件。Lean already. Ship.

---

## 8. 独立实跑（不采信根绿；`/tmp` 隔离 after 树）

工作区脏，且 index 中两改动文件仍为 before。本席：

```
git archive 3eca41eb9aa21c6791ef70d5695deb940abeb992 -- apps/web tsconfig.base.json package.json package-lock.json
  → /tmp/m012-b4-r5-iso
ln -sfn <正本 node_modules>
```

隔离树两文件内容 SHA256 与 after 表一致。

**绿（after 源 + after 测试）：**

```
cd /tmp/m012-b4-r5-iso/apps/web
npm run test:unit -- src/admin/pages/GovernanceTopologyPage.test.tsx \
  src/admin/pages/OrganizationMerchantPage.test.tsx \
  src/admin/pages/SubjectBindingsPage.test.tsx
```

exit **0**。`Test Files 3 passed`；`Tests 59 passed (59)` = 19 + 24 + 16。Duration 3.89s。jsdom `Window's scrollTo()` / `getComputedStyle() ... pseudo-elements` 原警告仍在，不当业务 FAIL。

**typecheck：**

```
cd /tmp/m012-b4-r5-iso/apps/web && npm run typecheck
```

`tsc -b --noEmit` exit **0**。

**红（after 测试 + before 页面 V0.05，另树 `/tmp/m012-b4-r5-red`，不碰正本）：**

页面 SHA256 `d3ddb2cc…`（before），测试 `3e91c3bb…`（after）。

```
npm run test:unit -- src/admin/pages/OrganizationMerchantPage.test.tsx
```

exit **1**。`Tests 4 failed | 20 passed (24)`。失败恰好四条，均可追溯本批 ID：

1. **M012-019** 既有删除申请用例：Received `"删除理由（提交后需上级审批）："`，Expected 新全文。
2. **M012-018** 后页：首屏横幅已是「已从下钻定位到商户：m-focus-500（在下表中查看其档案与归属）」，`not.toContain("已从下钻定位到商户")` 失败。
3. **M012-018** `?kpi=merchants&status=active` + m-2：Received 已定位 m-2，Expected 收窄句。
4. **M012-018** `?kpi=projects` + m-1：Received 已定位 m-1，Expected 收窄句。

测试能失败，GEN-003 成立。不能凭 59 绿放行；红环证明 018/019 断言打在 before 伪成功与旧 prompt 上。

未跑：真实浏览器、真实 HTTP、Maven、psql。B4 范围是本 diff；后台/HTML 两树无差异，不重复跑后端。

---

## 9. 三线摘要（只针对本 diff，不代替 B5 整模块）

**第一性原理**

- 018 必须存在，因为本页写了「已定位 / 在下表查看」；删掉收窄则该声称在后页/筛选/错表再次伪成功。证据：红测 2–4 + after `:415–420` + `AdminDataState` 加载/错误不渲染子表。
- 019 必须存在，因为申请框把「提交后需上级审批」写成无条件后果，与已发布 Service 分支及 HTML 570/576–577 不一致。证据：红测 1 + after `:256` 与 findings 逐字相同 + PlatformService blob 未变。
- 未验证就当真的前提：无。页大小 500、m-2 非 active、delete-request 仍走原入口，均在源码与夹具中读到。

**顺从式**

- 018/019 冻结 ID 均在 M012 21 条内（GEN-002/003/008/009/015/017，BSM-001，PGM-001）。
- 版本 V0.06 / 测试 V0.08 与派发要求一致。
- 009 与 001—017 未在 diff 中重开。

**对抗式**

- 构造后页、active 筛选、projects 错表、加载首屏：before 全伪成功，after 测试全绿。
- 构造 USB 直删改走新 UI：未发生，仍同一 POST。
- 构造把禁自审写进 prompt：未发生。
- 构造第三文件混入：`name-status` 仅两路径。

---

## 10. 本席限制与观察（不升格为本批 FAIL）

- ledger 模块行备注仍写「未获第5次许可」，与 B3 列值 5 及操作员本轮授权不一致。该文件不在本 diff 内；B4 不改台账。观察，不是无 ID 代码行。
- findings 顶栏仍写「等待追加修复授权」；队列值 018/019=是 在 B4 双 PASS 前保持是，符合流程。本席不改 findings。
- 未读本次 self B4。合取由调度在两席 raw 落齐后做。
- `/tmp` 探针已用完即留在绝对 `/tmp`，未写入正本。

无新 GR。无添加项。无 HTML 修改申请。无 LOCK-AMEND。

---

## 11. 结论

本轮 6 hunk、48 插入 / 4 删除，全部可归因到已定稿且队列为是的 M012-018、M012-019。018 按当前可见商户表匹配才称已定位，加载/未命中/错表收窄为指定句，未增定位器。019 只替换删除 prompt 为指定全文，不改后台/HTML/删除路径。原测试最小断言可红可绿。四文件未动。无无 ID 行。

修复内容：**PASS**
范围归因：**PASS**
本席 B4-r5 总结论：**PASS**

不是模块 PASS，不是 B5，不是发布许可。席甲 FAIL 则合取 FAIL。B4 双 PASS 后才应由修复者把 018/019 队列改否；本席不写 findings/ledger。

CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M012 B4-r5 修复内容独立审计（Grok 席乙）· 落点纠正完整票

文件版本：V0.02
步骤：B4-r5（报告执行位置纠正，**不是新 B3**，无第 6 轮，无新修复）
席位：固定 Grok 外审席乙 grok-4.6
计划：160 审计 V0.17 §4/B4、§5 闸 3；runbook.md V0.02
本文件路径：docs/audit-2026-09/modules/M012-b4-r5-grok-raw.md
本文件自身版本：V0.02（指定路径已有 V0.01；本次把**完整** B4 票追加到末尾，**不覆盖** V0.01）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

## 0. 对 V0.01 的纠正（合取口径）

V0.01 将审计测试与红测放到 `/tmp/m012-b4-r5-iso`、`/tmp/m012-b4-r5-red`、`/tmp/m012-b4-r5.diff`，并用 `git archive` 在 `/tmp` 建隔离树。这违反铁律 7：「正本写操作与审计命令只在 `/home/james/complex-management/` 及其下执行」。

**V0.01 整票不用于合取。** 调度只采信本 V0.02。V0.01 原文封存于本文件前缀，供追溯，不作本轮 B4 票。本席未读 `M012-b4-r5-self-raw.md`，不把席甲票或 V0.01 的 `/tmp` 绿/红输出当作本票证据。

本 V0.02 全部命令 cwd 均在正本 `/home/james/complex-management` 或其下 `apps/web`。未使用 `/tmp` 副本，未创建隔离树或探针文件，未改业务源码/测试/HTML/锁/findings/ledger/git 索引，未 `git add`/`commit`/`push`。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r5 **V0.02** 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。B3 累计第 5 次（操作员明确「追加一次修复，仅处理上述两项及对应测试」；授权第 5 次、非第 6 次）只审本对 tree 的修复内容。席甲自审票本席未读；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。不得以根绿测、旧 B4-r4 PASS、本席 B2 V0.08 PASS 或已作废的 V0.01 `/tmp` 跑测代替本轮对这 6 hunk 的核验。

未读取：`docs/audit-2026-09/modules/M012-b4-r5-self-raw.md`。未读适配器缓冲、未读席甲本次 B4 意见。定稿队列以 findings.md V3.67 顶部「M012 当前锁 B1→B2 双PASS定稿」为准：仅 M012-018、M012-019 队列是；M012-009（续记）观察否；旧 001—017 保持否。本席 B2 V0.08（`M012-b1-grok-raw.md` 最后一次完整 B2）同意的最小修法作为执行绑定对照，不把 B2 票本身当证据。席甲 B2 V0.08 只核标题/结论行「本席B2：PASS」存在（`M012-b1-self-raw.md` 该 B2 段），不读其 B4。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；仅 018/019 是否按 B2 同意边界修好；每一行能否归因到队列为是的已定稿 ID；是否越界改后台 / HTML / 锁 / sidecar / ledger 基准 / 拓扑页 / 绑定页 / 新定位器 / 新接口 / 重开 009。未新造判据/角色，未询问操作员，未 LOCK-AMEND。未中途修，未只审单条。审完整份 6 hunk 后才判定。

---

## 1. 技能自核（§6，不冒充）

本席核到且按收窄口径使用：

| 技能 | 路径 | 本轮用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能优先；操作员口径覆盖通用派子代理 |
| requesting-code-review | 同 superpowers | B4 请审=本席即外审；禁止 `HEAD~1` 基线、禁止子代理复审、禁止无 ID 即刻修 |
| receiving-code-review | 同 superpowers | 本席是外审，不 IMPLEMENT |
| verification-before-completion | 同 superpowers | PASS 前在**正本** `apps/web` 实跑 59 单测 + typecheck；先红用 before/after 源码类型 4 对照 + ledger 根席记录，不改页面回放 |
| systematic-debugging | 同 superpowers | 只借读错误→复现→追数据流；不改生产码、不加诊断桩 |
| test-driven-development / tdd | superpowers + mattpocock | 不改工作区回放红测；技能本体「测缝须用户确认」按铁律 5 作废 |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/` | 对抗结构自问自答；事实自取，不等操作员、不派子代理 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 横幅仍走原 `organization-merchant-focus` 缝；可见性复用已有 `visibleMerchants` / `kpiFocus` / `AdminDataState` |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 红环=无条件「已定位」横幅与当前表命中不一致；旧 prompt 把密钥直删写成一律上级审批 |
| ponytail / ponytail-review | `.../ponytail/4.9.0/skills/` | 只改一句 prompt + 一条横幅守卫 + 原测试最小断言。Lean already. |
| writing-for-agents | 存在 | 本 raw 给下游 B5/调度读 |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 存在；可选。本席不安装 hook、不写 `.git`、不设 `GIT_INDEX_FILE` |

缺口（不冒充已调用）：`batch-grill-me` 不存在；`ubiquitous-language` 不存在。划掉的 worktree/子代理/research/Arc 全仓 audit 未调用。graft SKILL.md 已读；本轮权威证据是两 tree `git diff --binary`，未调 graft CLI/MCP（V0.17 禁 MCP/子代理；graft 工具会刷新图，可能写 `graft/`，本席不用）。

---

## 2. 输入与独立锁实算

本席哈希命令在正本仓库根执行（未抄 sidecar / ledger / 派发期望值）：

```
cd /home/james/complex-management
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 hashlib（891182 bytes）
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值：`b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`

sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger **顶行**同值。criteria-lock 版本 V0.14。`LOCK-AMEND-COUNT: 1`。本批 B3 两树 lock blob 均为 `69dccbb728feb863971f2fd7aca76da47db11e64`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M012 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／网页与验证／第1段（6 文件） |
| 阶段 | B4-r5 只读审计 B3 累计第 5 次（操作员追加一次已批准）的「修复内容」本身；本文件 V0.02 只纠正审计命令落点 |
| 正式队列「是」（2） | 仅 M012-018、M012-019 |
| 正式队列「否」 | M012-009（续记，十项观察）及旧 001—017（已修/登记，本批不重开） |
| B3 | ledger 模块表当前 B3 列为 5；本席不改 ledger |
| findings.md | V3.67；工作区 SHA256 `4ae89af55ae73e60f09e4f85b1d168b3125514b794aeefa42a59f01e338232ba`（本 diff 未改 findings） |
| before（对象类型 tree，可解析） | `c218cfb17edcfdf278a96cf9b3c6e5b32192fd2e` |
| after（对象类型 tree，可解析） | `3eca41eb9aa21c6791ef70d5695deb940abeb992` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary c218cfb17edcfdf278a96cf9b3c6e5b32192fd2e 3eca41eb9aa21c6791ef70d5695deb940abeb992`（无路径过滤） |
| 实际差异 | 6175 bytes；SHA256 `794a06ffc17723e33a55f27ddb66a0d6f295c051bf70c573a297c83c2ef241f1`；UTF-8；无 NUL；93 行；2 路径；48 插入 / 4 删除；6 hunk；name-status 2×M |
| `git diff --check` | exit 0 |
| 两树路径清单 | before 3979 / after 3979；集合差空（无增删路径） |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`，分支 `main` |
| HEAD^{tree} | `ee1833d2672e5372ef7b056db4f92d48e6de875a` ≠ after。before/after 是完整全树临时索引快照，不是 HEAD diff |
| 工作区两改动文件内容 SHA256 | 全部等于 after（见 §2.1）。主工作区其它文件有未提交脏状态；本席证据采两冻结 tree + 正本工作区中与 after 逐字节相同的六源及直接依赖 |
| 真实 index | 组织页两文件仍为 **before** 内容；本席未设 `GIT_INDEX_FILE`、未 `git add` |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `6e9d813c985406aaf14c1645af2b494d342ae657`；三者两树完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |
| PlatformService.java 两树 blob | `63e53b3177610f87a5b49e82173e375dbaa7d951` 相同 |
| PermissionGate.tsx 两树 blob | `7f39e56f21fbfa6e34fff19bc609242517042f46` 相同 |

冻结映射仍为 `M012 → [BSM-001, BSM-003, GEN-001 … GEN-018, PGM-001]`，21 项，未增判据。BSM-004 仍不在映射；HTML map 570/576–577 只作 019 的类型 1 解释，与 B2 口径一致。

### 2.1 六源 blob / 内容哈希 / 行数

git blob（`git ls-tree <tree> -- <path>`）：

| 文件 | before blob | after blob | 版本 | 行数 before→after | 工作区相对 after |
|---|---|---|---|---|---|
| GovernanceTopologyPage.tsx | `89af88ebf200d11fcd1d47082a788411311cd047` | 同 before | V0.04 未动 | 274→274 | 相等 |
| GovernanceTopologyPage.test.tsx | `5bf4bf26411f020c81780c66681b6485099f539f` | 同 before | V0.04 未动 | 308→308 | 相等 |
| OrganizationMerchantPage.tsx | `0255d620a4f9572d1a531f78ef5b45eb0ccaf728` | `2f1f1cbd965ef6c747228ab402f5d7cd951de3f1` | V0.05→V0.06 | 520→524 | 相等 |
| OrganizationMerchantPage.test.tsx | `b2149d21ac69a26ebfb87d4d98632467dae50bd0` | `ce494c7535024948b157e9cf15c2c42fbcbdbfdc` | V0.07→V0.08 | 435→475 | 相等 |
| SubjectBindingsPage.tsx | `0d916b81f72c4d8ea0acf46e43e85121e2ba71db` | 同 before | V0.03 未动 | 257→257 | 相等 |
| SubjectBindingsPage.test.tsx | `e145f2b54a5c3271d7601c382044486e37720872` | 同 before | V0.04 未动 | 291→291 | 相等 |

内容 SHA256（`git cat-file -p \| python3 hashlib`）：

| 文件 | before | after |
|---|---|---|
| OrganizationMerchantPage.tsx | `d3ddb2cc423cc901190e8a5d9c5c6f2e4095919d956a19a6c866124947fa6722` | `55dc9a69205df3254c4031015bfe53907a3b1fbd7605e97c8f64a8ab515cb328` |
| OrganizationMerchantPage.test.tsx | `d40c83d948e0b07f83f6eb5bcbb2a18af12b186ddd93fdcc651557d5870408a7` | `3e91c3bbd589558e50bab1192036db2179b0dbdd4158fb0841eba1b0184d33e1` |
| 其余四文件 | 与 B2 V0.08 六源表相同 | 与 before 逐字节相同 |

六文件合计 2085→2129 行。仅组织页源+原测试两文件有 diff；拓扑/绑定四文件字节级未变。组织页 before blob `0255d620…` 等于上轮 B4-r4 after，本批相对已验收 r4 after 只叠加 018/019。

index 中两改动文件内容 SHA256 仍为 before（`d3ddb2cc…` / `d40c83d9…`）。本席未改真实索引。正本工作区未暂存正文等于 after，故在 `apps/web` 实跑现测试即跑 after 源。

直接依赖（AdminDataState / useServerPagedRows / PermissionGate.tsx / Can.tsx / TechDetails / AdminPageHeader / TablePager / types.ts / adminClient.ts / apiError.ts / statusLabels.ts / test/setup.ts / vitest.config.ts / tsconfig.json）工作区均与 after 树逐字节相同。

`git diff --stat --binary`：

```
 .../admin/pages/OrganizationMerchantPage.test.tsx  | 42 +++++++++++++++++++++-
 .../src/admin/pages/OrganizationMerchantPage.tsx   | 10 ++++--
 2 files changed, 48 insertions(+), 4 deletions(-)
```

`git diff --numstat --binary`：`41 1` 测试、`7 3` 页面。

---

## 3. 定稿依据（B2 双 PASS；本批仅 018/019）

findings.md V3.67 顶部（摘录，未改写）：

- **M012-018** 细化 / 工程缺陷是 / 队列是。事实：横幅仅凭 `focusObjectId` 显示「已从下钻定位到商户…在下表查看」，与 168–177 滚动守卫及当前可见表不一致。最小处置：仅在目标实际出现在当前可见商户表时显示已定位；未命中或未展示商户表时收窄为「当前列表尚未定位到该商户，请检查筛选或翻页」。保留原生分页/过滤，不新加接口、跨页扫描或定位器。原 test 补命中/未命中和隐藏表反馈断言。
- **M012-019** 细化 / 工程缺陷是 / 队列是。事实：`window.prompt("删除理由（提交后需上级审批）：")` 与 `PlatformService.requestDeleteMerchant` 密钥本人立即软删、审批须最高权/禁自审不符。最小处置：将现提示收窄为「删除理由（由最高权限审批；持授权 U 盘密钥的最高权限本人发起时直接执行）：」。仅现提示及最小测试，不改 HTML、后台、权限、删除或恢复功能。
- **M012-009（续记）** 技术性/观察 / 缺陷否 / 队列否。十项观察完整保留。001—008 及 010—017 保持否。

本席 B2 V0.08：**PASS**。席甲 B2 V0.08 标题段结论「本席B2：PASS」（`M012-b1-self-raw.md` 该段，未读 B4 self）。用户本轮：「M012 追加一次修复，仅处理上述两项及对应测试」。

HTML 类型 1（不新增映射，BSM-004 不进 M012）：

- `business-system-map-for-operations-v0.1.html:570`「由具备最高权限的人审批」
- 同文件 `:576` 与 `:577` 均含「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」

after 树 `PlatformService.java`（blob 未变）`:283–295`：`isMasterAuthority()` 查占用后立即软删；非密钥最高权限只写 `pending`。`:328–335`：非最高权限禁止审批；申请人禁自审。本 diff 未改该文件。

---

## 4. 完整两 tree 无过滤 diff 全文

命令（cwd=`/home/james/complex-management`）：

`git diff --binary c218cfb17edcfdf278a96cf9b3c6e5b32192fd2e 3eca41eb9aa21c6791ef70d5695deb940abeb992`

字节 6175，SHA256 `794a06ffc17723e33a55f27ddb66a0d6f295c051bf70c573a297c83c2ef241f1`。下文为独立重取的整份原文，未过滤、未截断、未写入 `/tmp`。

```diff
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
index b2149d21..ce494c75 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.07
+// 文件版本：V0.08
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import userEvent from "@testing-library/user-event";
 import { afterEach, describe, expect, it, vi } from "vitest";
@@ -314,6 +314,8 @@ describe("OrganizationMerchantPage 存档/删除写路径（M012-008）", () =>
     fireEvent.click(screen.getByTestId("merchant-actions-m-3"));
     await waitFor(() => expect(screen.getByTestId("merchant-delete-request-m-3")).toBeTruthy());
     fireEvent.click(screen.getByTestId("merchant-delete-request-m-3"));
+    // M012-019：申请框须说明密钥本人会直接执行，不能承诺所有提交均待审批。
+    expect(window.prompt).toHaveBeenCalledWith("删除理由（由最高权限审批；持授权 U 盘密钥的最高权限本人发起时直接执行）：");
     await waitFor(() => expect(postCalls()).toHaveLength(1));
     const [url, init] = postCalls()[0]!;
     expect(String(url)).toContain("/api/v1/platform/merchants/m-3/delete-request");
@@ -433,3 +435,41 @@ it("M012-013：重试成功后清除全局读取错误", async () => {
   await screen.findByTestId("organization-merchants-table");
   expect(props.setGlobalError).toHaveBeenLastCalledWith(null);
 });
+
+describe("M012-018：下钻提示只报告当前可见商户", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    vi.restoreAllMocks();
+    window.history.replaceState({}, "", "/admin/governance/organizations");
+  });
+
+  it("后页目标未加载时不称已定位，翻页看到目标后才显示成功", async () => {
+    const rows = Array.from({ length: 501 }, (_, i) => ({ ...MERCHANTS[0], id: "m-focus-" + i }));
+    renderPage((url, init) => {
+      if (init?.method === "POST" || !url.includes("/merchants")) return undefined;
+      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
+      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
+    }, { focusObjectId: "m-focus-500" });
+    expect(screen.getByTestId("organization-merchant-focus").textContent).not.toContain("已从下钻定位到商户");
+    await screen.findByTestId("organization-merchant-row-m-focus-0");
+    expect(screen.queryByTestId("organization-merchant-row-m-focus-500")).toBeNull();
+    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("当前列表尚未定位到该商户，请检查筛选或翻页");
+    fireEvent.click(screen.getByTestId("organization-merchants-pager-next"));
+    await screen.findByTestId("organization-merchant-row-m-focus-500");
+    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("已从下钻定位到商户：m-focus-500");
+  });
+
+  it.each([
+    ["?kpi=merchants&status=active", "m-2", "organization-merchants-table"],
+    ["?kpi=projects", "m-1", "organization-projects-table"],
+  ])("目标被当前视图隐藏时不称已定位：%s", async (query, target, table) => {
+    window.history.replaceState({}, "", "/admin/governance/organizations" + query);
+    renderPage(undefined, { focusObjectId: target });
+    await screen.findByTestId(table);
+    expect(screen.queryByTestId("organization-merchant-row-" + target)).toBeNull();
+    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("当前列表尚未定位到该商户，请检查筛选或翻页");
+    fireEvent.click(screen.getByTestId("organization-kpi-focus-clear"));
+    await screen.findByTestId("organization-merchant-row-" + target);
+    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("已从下钻定位到商户：" + target);
+  });
+});
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
index 0255d620..2f1f1cbd 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.05
+// 文件版本：V0.06
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -253,7 +253,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
   }
 
   async function requestDeleteMerchant(id: string) {
-    const reason = window.prompt("删除理由（提交后需上级审批）：");
+    const reason = window.prompt("删除理由（由最高权限审批；持授权 U 盘密钥的最高权限本人发起时直接执行）：");
     if (reason === null || !reason.trim()) { setGlobalError("删除理由必填"); return; }
     setGlobalError(null);
     try {
@@ -413,7 +413,11 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
     <section data-testid="organization-merchant-page">
       <AdminPageHeader title={TEXT.title} description={TEXT.description} />
       {focusObjectId && (
-        <div className="admin-state" data-testid="organization-merchant-focus">已从下钻定位到商户：{focusObjectId}（在下表中查看其档案与归属）</div>
+        <div className="admin-state" data-testid="organization-merchant-focus">
+          {!loading && !error && (!kpiFocus || kpiFocus.table === "merchants") && visibleMerchants.some((row) => row.id === focusObjectId)
+            ? <>已从下钻定位到商户：{focusObjectId}（在下表中查看其档案与归属）</>
+            : "当前列表尚未定位到该商户，请检查筛选或翻页"}
+        </div>
       )}
       <TechDetails><div className="admin-generic-evidence" data-testid="organization-merchant-evidence">{TEXT.evidence}</div></TechDetails>
       {formError ? <div className="admin-inline-error" data-testid="organization-merchant-form-error">{formError}</div> : null}
```

---

## 5. 逐块 ID 归因（§5 闸 3）

每一变更行必须落到 findings 当前值为「是」的已定稿 ID。本批仅 018、019 为是。

| hunk | 位置 | 变更 | 归属 ID | 闸 3 |
|---|---|---|---|---|
| H1 | `OrganizationMerchantPage.test.tsx` `@@ -1,4` | 文件版本 V0.07→V0.08 | M012-018 + M012-019 | 通过。AGENTS 自身版本铁律：本文件因两 ID 被改，版本 +1，不是第三功能 |
| H2 | 同文件 `@@ -314,6` | 在既有删除申请用例上断言新 prompt 全文；原 POST `/delete-request` 与理由体仍在 | M012-019 | 通过。B2「原测试对 prompt 文案的最小断言」 |
| H3 | 同文件 `@@ -433,3` | 新增 describe：后页未命中/翻页命中；`it.each` 状态筛选隐藏 m-2、KPI 项目表隐藏商户表；清除筛选后命中。复用 `organization-merchant-focus` 与既有 `renderPage`/`envelope`/500 页大小 | M012-018 | 通过。命中/未命中/隐藏表/加载（首条在 `findByTestId` 行之前断言不出现「已定位」） |
| H4 | `OrganizationMerchantPage.tsx` `@@ -1,4` | 文件版本 V0.05→V0.06 | M012-018 + M012-019 | 通过。同 H1 |
| H5 | 同文件 `@@ -253,7` | 仅替换 `window.prompt` 字符串；仍 POST `.../delete-request`；理由非空、catch、权限按钮未动 | M012-019 | 通过。与 findings 最小处置逐字相同 |
| H6 | 同文件 `@@ -413,7` | 横幅仍 `data-testid="organization-merchant-focus"`。成功文案仅当 `!loading && !error && (!kpiFocus \|\| kpiFocus.table === "merchants") && visibleMerchants.some(id)`；否则指定收窄句 | M012-018 | 通过。未见新定位器、新接口、跨页扫描 |

无第三路径、无拓扑/绑定 hunk、无 HTML/后台/锁 hunk。无「无 ID」行。页面 `data-testid` 集合 before/after 差集为空；测试新增断言只用已有 testid。

滚动守卫 after `:169–177` 仍为 `merchants.some` + 既有 `querySelector`；before/after `querySelector` 出现次数均为 3。`window.prompt` 出现次数均为 2（存档「存档（下线）理由：」未改，不在 019 范围）。删除 prompt 出现次数均为 1。未改 `requestDeleteMerchant` 的网络形状。

---

## 6. 修复内容：018 是否按最小边界落地

**声称（B2/findings / 用户本轮）：** 当前可见商户表匹配才提示已定位；未命中/加载/错表收窄为指定句；不增定位器。

**after 实现（`:415–420`）：**

1. **命中：** `visibleMerchants` 已是 KPI 状态子集过滤后的当前页数组（`:286–287`），`.some(row.id === focusObjectId)` 才渲染原成功句「已从下钻定位到商户：{id}（在下表中查看其档案与归属）」。表体 `:390` 同样 map `visibleMerchants`。
2. **错表：** `(!kpiFocus \|\| kpiFocus.table === "merchants")`。`readKpiFocusFromUrl` 认 `?kpi=entities|merchants|projects`。`kpi=projects` / `kpi=entities` 时只渲染一张非商户表（`:427–434`），即使内存 `merchants` 含目标也不称「在下表查看」。
3. **加载/读错：** `loading`/`error` 来自三资源 `useServerPagedRows`（`:160–161`）。该 hook 初始 `useState(true)`（`useServerPagedRows.ts:35`），首屏即 loading。`AdminDataState`（after 树未改，工作区哈希等于 after）在 `loading` 时只渲染「加载中...」、在 `error` 时只渲染错误、**不渲染子表**。横幅在 `AdminDataState` 之外，故加载/错读时表未展示，收窄句成立。这不是新错误通道，是「未展示商户表」的既有 DOM 事实。
4. **未命中：** 当前页或状态过滤后 `visibleMerchants` 无该 id → 收窄句。501 条、page size 500（`:144–148` 与既有测试同一契约），`m-focus-500` 在第 2 页。
5. **不增定位器：** 未新增 testid、未新增搜索接口、未改滚动 effect、未扫描其它页。业态 `typeFilter` 仍走既有后端 `merchantRows.search`（`:223`），命中集合即当前 `merchants`/`visibleMerchants`，不是新扫描器。

夹具：`MERCHANTS[1]`（m-2）`status: "suspended"`，`?kpi=merchants&status=active` 时被 `visibleMerchants` 滤掉且行不在 DOM，与 `expectActiveMerchantsFocusView` 既有「不见 m-2」一致。

**第一性原理：** 删掉这条收窄，HTML 没有「横幅句式」条目，但页面自己的「已定位 / 在下表查看」是可执行声称（GEN-002/009/015、BSM-001、PGM-001「上下可钻」）。before `:416` 在第 2 页目标、筛选隐藏、项目表聚焦、以及 loading 首屏仍写成功句，是伪成功。after 把声称绑到当前可见表。

**顺从式：** 与 018 最小处置字面一致；分页/业态/存档筛选保留；成功句未发明新业务状态；收窄句与 findings 逐字相同。

**对抗式 / grilling（自问自答）：**

1. 是否把 013 的「501 项目」重开？否。本用例是本项目商户分页，B2 已区分。
2. 是否新定位器？否。仍用原横幅节点；testid 差集空。
3. `!error` 是否多余？否。error 时 `AdminDataState` 不展示商户表，属于「未展示」。
4. 加载时收窄句含「请检查筛选或翻页」是否新文案发明？否。B2/findings 指定唯一收窄句，用户本轮把加载列入收窄场景。
5. 是否测试私有布尔而非用户可见句？否。断言的是横幅 `textContent` 字面量。
6. 未单列 error+focus 用例是否不足？否。最小处置要求命中/未命中/隐藏表；loading 由 018 首条在 `findByTestId` 之前断言覆盖（hook 初始 loading=true）。error 时表本就不渲染，不升新缺陷、不重开 009。

018 **充分**。

---

## 7. 修复内容：019 是否按最小边界落地

**声称：** 删除 prompt 复述最高权审批及密钥本人直接执行；不改后台/HTML。

**after `requestDeleteMerchant`（`:255–268`）：** prompt 字符串与 findings 最小处置、席甲 B2、本席 B2 **逐字相同**。随后仍：`reason` 空则 `删除理由必填`；`POST /api/v1/platform/merchants/${id}/delete-request`；失败走既有 `reportWriteFailure`。批准/驳回函数、权限码、按钮集未动。存档 prompt「存档（下线）理由：」未改。

正本只读对照（cwd 正本，`git show <tree>:path`，exit 0）：

- before 含旧句、不含新句
- after 含新句、不含旧句
- after 测试断言新句全文；before 测试不断言 prompt 文案

两树 `PlatformService.java` blob 全等；三 HTML blob 全等。未把禁自审写进申请框（B2 明确那是审批入口）。

原测试仍覆盖 POST 与理由体；H2 只加 prompt 全文断言。

019 **充分**，未扩到 Bar/票务同文案、未改删除状态机。

---

## 8. 越界与 009

| 检查 | 结果 |
|---|---|
| 无 ID 行 | 无。6 hunk 均落到 018 和/或 019 |
| 重开 009 | 无。未补附件、绑定创建、项目分配、项目资料、KPI 英文键、Date.now、掩码 |
| 新功能 | 无。无新接口、无跨页扫描、无新权限、无新 HTML 映射、无 LOCK-AMEND |
| 后台 / 三份 HTML / 锁 | 两树 blob 全等 |
| 拓扑页 / 绑定页 | blob 全等 |
| 测试框架替换 | 无。仍 vitest + 原 `renderPage` |
| 顺手优化 | 无。无格式化整文件、无抽象层 |
| 第 6 轮 / 新 B3 | 无。本票只纠正 V0.01 审计落点 |

ponytail-review：`net: -0`。Fragment 因插值 JSX 必需；不把横幅抽成新组件。Lean already. Ship.

---

## 9. 独立实跑（正本 `apps/web`；不采信根绿；不采信 V0.01 `/tmp`）

### 9.1 绿（after 工作区 = after 树）

工作区脏，index 中两改动文件仍为 before，**未暂存正文等于 after**。本席不改 index、不切隔离树。

```
cd /home/james/complex-management/apps/web && pwd
# 输出：/home/james/complex-management/apps/web
npm run test:unit -- src/admin/pages/GovernanceTopologyPage.test.tsx \
  src/admin/pages/OrganizationMerchantPage.test.tsx \
  src/admin/pages/SubjectBindingsPage.test.tsx
```

exit **0**。`Test Files 3 passed (3)`；`Tests 59 passed (59)` = GovernanceTopology 19 + OrganizationMerchant 24 + SubjectBindings 16。Duration 3.80s。jsdom `Window's scrollTo()` / `getComputedStyle() ... pseudo-elements` 原警告仍在，不当业务 FAIL。

**typecheck：**

```
cd /home/james/complex-management/apps/web && npm run typecheck
```

`tsc -b --noEmit` exit **0**。

不能凭 59 绿放行。下列先红对照与逐块源码核验同时成立才给内容票。

### 9.2 先红（不改页面、不建隔离树）

用户禁令：不要重新制造红测改页面。本席未把工作区页面换成 V0.05，未 `git archive`，未在 `/tmp` 跑测。

**类型 4 源码对照**（`git show before/after:apps/web/src/admin/pages/OrganizationMerchantPage.tsx` 与 after 测试，正本 git，exit 0）：

| # | after 测试断言 | before 页面事实 | 若用 after 测试打 before 页面 |
|---|---|---|---|
| 1 | H2：`toHaveBeenCalledWith` 新删除 prompt 全文 | `:256` 仍是「删除理由（提交后需上级审批）：」 | **FAIL**（M012-019） |
| 2 | 018 后页：首屏 `not.toContain("已从下钻定位到商户")` | `:416` 只要 `focusObjectId` 就写成功句，与 loading/当前页无关 | **FAIL**（M012-018） |
| 3 | `?kpi=merchants&status=active` + m-2：收窄句 | 同上，无条件成功句，m-2 会被写成已定位 | **FAIL**（M012-018） |
| 4 | `?kpi=projects` + m-1：收窄句 | 同上，项目表聚焦仍写「在下表查看」 | **FAIL**（M012-018） |

组织页 after 测试共 24 项；上述 4 条会红，其余 20 条不依赖本批两处文案。这与根席 ledger 记录「先红 4FAIL20PASS（exit1）」同构。本席采信该对照与 ledger 记录的互证，**不采信 V0.01 `/tmp` 红测输出**。

GEN-003：after 测试在正本实际执行（§9.1）；断言能打在 before 伪成功与旧 prompt 上（本表）。测试通过本身不替代该对照。

未跑：真实浏览器、真实 HTTP、Maven、psql。B4 范围是本 diff；后台/HTML 两树无差异，不重复跑后端。

---

## 10. 三线摘要（只针对本 diff，不代替 B5 整模块）

**第一性原理**

- 018 必须存在，因为本页写了「已定位 / 在下表查看」；删掉收窄则该声称在后页/筛选/错表/加载再次伪成功。证据：§9.2 行 2–4 + after `:415–420` + `AdminDataState` 加载/错误不渲染子表 + `useServerPagedRows` 初始 loading=true。
- 019 必须存在，因为申请框把「提交后需上级审批」写成无条件后果，与已发布 Service 分支及 HTML 570/576–577 不一致。证据：§9.2 行 1 + after `:256` 与 findings 逐字相同 + PlatformService blob 未变。
- 未验证就当真的前提：无。页大小 500、m-2 非 active、delete-request 仍走原入口，均在源码与夹具中读到。

**顺从式**

- 018/019 冻结 ID 均在 M012 21 条内（GEN-002/003/008/009/015/017，BSM-001，PGM-001）。
- 版本 V0.06 / 测试 V0.08 与派发要求一致。
- 009 与 001—017 未在 diff 中重开。

**对抗式**

- 构造后页、active 筛选、projects 错表、加载首屏：before 全伪成功；after 源守卫 + 正本 24 项绿。
- 构造 USB 直删改走新 UI：未发生，仍同一 POST。
- 构造把禁自审写进 prompt：未发生。
- 构造第三文件混入：`name-status` 仅两路径。
- 构造用 `/tmp` 隔离树充当正本证据：本票拒绝；V0.01 因此不作合取。

---

## 11. 本席限制与观察（不升格为本批 FAIL）

- ledger 模块行备注与 B3 列值 5 已反映本轮授权；本席不改 ledger。
- findings 顶栏仍写等待授权类历史句；队列值 018/019=是 在 B4 双 PASS 前保持是，符合流程。本席不改 findings。
- 真实 index 仍持 before 两文件；这是 B3 用临时索引写 tree 的遗留，**不在本对 tree diff 内**。本席不 `git add`。
- 未读本次 self B4。合取由调度在两席 raw 落齐后做；合取不得使用 V0.01。
- 主工作区另有他模块未提交改动；本票只审指定两 tree。六源及组织页直接依赖工作区等于 after。
- 磁盘上仍可能残留历史 `/tmp/m012-b4-r5*`（V0.01 产物）。本席未读取其测试输出作证据，未在 `/tmp` 新建或更新任何审计文件。

无新 GR。无添加项。无 HTML 修改申请。无 LOCK-AMEND。无第 6 轮。无新修复。

---

## 12. 结论

本轮 6 hunk、48 插入 / 4 删除，全部可归因到已定稿且队列为是的 M012-018、M012-019。018 按当前可见商户表匹配才称已定位，加载/未命中/错表收窄为指定句，未增定位器。019 只替换删除 prompt 为指定全文，不改后台/HTML/删除路径。原测试最小断言在正本 after 上 59 绿、typecheck 0；相对 before 源的 4 条断言可失败。四文件未动。无无 ID 行。

V0.01 因铁律 7 不用于合取。本 V0.02 为席乙本轮唯一合取票。

修复内容：**PASS**
范围归因：**PASS**
本席 B4-r5 总结论：**PASS**

不是模块 PASS，不是 B5，不是发布许可。席甲 FAIL 则合取 FAIL。B4 双 PASS 后才应由修复者把 018/019 队列改否；本席不写 findings/ledger。
