CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M023 B4-r2 Grok 席乙只读审计（修复 diff）
版本：V0.01
席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
步骤：B4-r2（§4 B4 + §5 闸 3；B3 第 2 次）
正本：`/home/james/complex-management` main 主工作区
对象：席甲 B3-r2 整批修复 diff（网页与验证模块，仅合同页+测试）
findings 正本：`docs/audit-2026-09/findings.md` V2.75，归属「是」且已定稿：仅 M023-007
独立性：本席未读 `M023-b4-r2-self-raw.md`；未派 MCP / 子代理 / 第三席；未问人；未改业务 / HTML / 锁 / findings / ledger；未 fetch / commit / push / merge / deploy；未建脚本、日志或临时产物。旧 B1 / B2-r2 / B4-r1 / B5-r1 可对照，本轮以 V2.75 定稿处置为准。

---

## 0. 技能预检（§6，按本环境实核）

核到且按收窄口径使用：
1. `using-superpowers`：开工技能自检；用户指令覆盖 Arc 默认派席。
2. `graft`：实调 `graft grep` 定位 `listContractsByMerchant` / `drafting` / JDBC SQL；`chk_contracts_lifecycle_status` 无索引命中，改读未索引迁移 SQL。
3. `grilling` / `grill-me`：对抗式自问自答；本体「等用户 / 派子代理」作废。
4. `ponytail-review`：只找可删多余抽象；不改码。结论：Lean already。
5. `verification-before-completion`：PASS 前本席当场重跑三份单测。
6. `systematic-debugging` / `diagnosing-bugs` / `tdd` / `test-driven-development`：只借错误→复现→数据流；对照 RED/GREEN 日志后本席重跑 GREEN。不加诊断桩、不改键做破坏实验（禁修业务）。
7. `requesting-code-review`：B4＝本席即外审；不用 HEAD~1、不用子代理复审。
8. `codebase-design`：核页内局部字典 vs 不改 `types.ts` / 全局 `statusLabels` 的缝。
9. `writing-for-agents`：本 raw 给下游席读。
10. `i-have-adhd`：已读；`disable-model-invocation: true`；提示词覆盖其「第一行必须是下一步动作」——本文件第一行仍必须是判据锁哈希。
11. `using-arc`：已读；其默认「派评审子代理」与双席铁律冲突，本席不走 Arc 审计流程。
12. `resolving-merge-conflicts`：已读；`test -f .git/MERGE_HEAD` → `NO_MERGE`。
13. `long-running-background-tasks`：已读；vitest 短前台完成，未挂后台。

明确不用（提示词划掉或本步不适用）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`executing-plans`、`research`、`ponytail-audit`、mattpocock `code-review`。`brainstorming` / `writing-plans` / `ponytail` 本体属 A/B3，本步只读不修。`receiving-code-review` 是席甲收本席意见，本席不自收。

缺口（实找无目录，不冒充已调用）：`batch-grill-me`、`ubiquitous-language`。
可选存在未用：`git-guardrails-claude-code`（本席只读、无 git 写、不装 hook）。

graft 本轮检索约节省 ~189,417 tokens（`listContractsByMerchant` 两次 + `drafting` 一次；约束名无索引命中不计）。

---

## 1. 输入身份（类型 4）

本席独立实算（未从 sidecar / ledger / 派发词抄写）：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
→ b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```
sidecar / ledger 顶部同值，仅作事后对照，不作为本行来源。

冻结 diff：
```
sha256sum /home/james/audit-tmp/m023_b3_r2.diff
→ 46f33f7a7d0c7b79f6a836c82cf25ee26aaba75e539109d0cd94c2f6226177cc
wc -c → 3159
```
与派发声明一致。

tree（`git cat-file -t`）：
- before `012effdd8c2ebbe05bd1eb6e47cef1b79ac5c64a` 类型 tree
- after  `144c4edd74f86cffe1c6aa92ed1fc8d3bd8b341f` 类型 tree

```
git diff --binary <before> <after> | sha256sum
→ 46f33f7a7d0c7b79f6a836c82cf25ee26aaba75e539109d0cd94c2f6226177cc
git diff --binary … | cmp - /home/james/audit-tmp/m023_b3_r2.diff → CMP_OK
git diff --stat：2 files changed, 18 insertions(+), 3 deletions(-)
git diff --name-status：仅下列 2 个 M
git diff | grep -c '^@@' → 5
```

改动闭集：
- `apps/web/src/admin/pages/MerchantZoneContractsPage.tsx`（after blob `876d09b9…` = 工作区 `git hash-object`；SHA256 `8a932c358b95fd24fcebfada8126f006813f16130ac161317b3fd25f615dee9f`；175 行；首行 `// 文件版本：V0.02`）
- `apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx`（after blob `660dd19d…` = 工作区；SHA256 `f9a704f44313a8a83abfce7a661b0a78527f97fd8da6f9e040e357fdc52b1485`；157 行；首行 `// 文件版本：V0.02`）

主工作区这两文件与 after tree 无 diff（`git diff after -- <2 files>` 0 字节）。
本模块另外 4 文件（Scope 页+测、Settlements 页+测）before→after 与 after→工作区均为 0。
`types.ts` / `statusLabels.ts` / `backend/**` 不在两树 `git diff --name-only` 内。

闸 3：每一改动行须归到 V2.75 归属「是」的 ID（仅 M023-007）；归不到即越界。

工作区：`main` / `HEAD e456ba96c84185c8d39f18f548011daac48a6466`；`NO_MERGE`。

---

## 2. 定稿最小处置（审计标尺，摘 V2.75）

| ID | 分类 | 最小处置 |
|---|---|---|
| M023-007 | 细化 | 字典键 `draft` 改为 `drafting: "草拟"`；夹具加一条 `lifecycle_status: "drafting"` 并断言表格行与抽屉均显示「草拟」、不含「未知状态」、不含英文 `drafting`；两文件版本标记 V0.01→V0.02。不改 `types.ts`／后端／全局 `statusLabels`（全局 `drafting→起草中` 与页内「草拟」不同词，本页沿用页内字典）；其余 7 键已与 CHECK 闭集逐个对上，不动 |

只登记不入队（不得混进本 diff）：后端第 9 态 `amended`（B2-r2 已定：三份 HTML 未给中文名，席甲不自拟；007 修后该态继续走 003「未知状态」占位）。001–006 已修转「否」，不得再改。

---

## 3. 冻结 diff 全文（5 hunk）

```
diff --git a/apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx b/apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx
index b38f6f1c..660dd19d 100644
--- a/apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx
+++ b/apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 ...
@@ -10,6 +10,7 @@ const CONTRACTS = [
   { id: "contract-aaaa1111", ... effective ... },
   { id: "contract-bbbb2222", ... expired ... },
   { id: "contract-cccc3333", ... weird_status ... },
+  { id: "contract-dddd4444", lifecycle_status: "drafting", effective_date: "2027-01-01", expiry_date: "2027-12-31", deposit_cent: 0, space_ids: [] },
 ];
 ...
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
 ...
diff --git a/apps/web/src/admin/pages/MerchantZoneContractsPage.tsx b/apps/web/src/admin/pages/MerchantZoneContractsPage.tsx
index c9ca5a51..876d09b9 100644
--- a/apps/web/src/admin/pages/MerchantZoneContractsPage.tsx
+++ b/apps/web/src/admin/pages/MerchantZoneContractsPage.tsx
@@ -1,4 +1,4 @@
-// 文件版本：V0.01
+// 文件版本：V0.02
 ...
@@ -16,7 +16,7 @@ const TEXT = {
 const STATUS_LABELS: Record<string, string> = {
-  draft: "草拟",
+  drafting: "草拟",
   pending_confirmation: "待确认",
```

`git diff --unified=0` 的全部 `+/-` 内容行（不含 `+++`/`---` 头）即上表；无第 6 个 hunk、无第三文件。

---

## 4. diff 逐块归因（5 hunk，逐行）

归因符号：一律 M023-007。版本行随该文件主修复 ID 记（007 明文要求 V0.01→V0.02），不另开无 ID 行。

### 4.1 MerchantZoneContractsPage.test.tsx（H01–H03）

| hunk | 旧→新 | 内容 | ID |
|---|---|---|---|
| H01 `@@ -1,4 +1,4` | `-V0.01` / `+V0.02` | 测试文件版本标记 | 007（最小处置「两文件版本标记」） |
| H02 `@@ -10,6 +10,7` | `+` 夹具一行 | `contract-dddd4444` / `lifecycle_status: "drafting"`；日期/押金/空间形状与既有三行同构 | 007（「夹具加一条 drafting」） |
| H03 `@@ -87,6 +88,20` | `+` 新 `it` 14 行 + 空行 | 表格行与抽屉均 `toContain("草拟")`、`not.toContain("未知状态")`、`not.toContain("drafting")` | 007（「断言表格行与抽屉」） |

H03 插在既有 003 未知码用例与租期用例之间，未改那两例正文。H02 只追加第 4 行，未改 aaaa/bbbb/cccc 三行（003 的 `weird_status`、005 的纯文本夹具仍在）。

import 集合未变。无越界 import。

### 4.2 MerchantZoneContractsPage.tsx（H04–H05）

| hunk | 内容 | ID |
|---|---|---|
| H04 `@@ -1,4 +1,4` | `-V0.01` / `+V0.02` | 007（版本标记） |
| H05 `@@ -16,7 +16,7` | `-  draft: "草拟",` / `+  drafting: "草拟",` | 007（「字典键 draft 改为 drafting，词仍为草拟」） |

H05 只替换首键名；后 7 键（`pending_confirmation`…`archived`）上下文未改。`UNKNOWN_STATUS` / `statusLabel` / 列 / KPI / 纯文本合同号 均不在两树 diff。

### 4.3 闸 3 闭集核对

`git diff --unified=0` 内容行共 21 条（3 删 + 18 增），全部落入上表 5 hunk，全部归因 007。无「顺手优化」、无新抽象、无改测试框架、无第三文件。

**归因票：PASS**

---

## 5. 内容审计

### 5.1 是否按定稿最小处置（顺从式逐条）

| 处置句 | 证据 | 判 |
|---|---|---|
| 字典键 `draft`→`drafting: "草拟"` | after L18-27：`drafting: "草拟"`；`draft` 计数 0；词面仍「草拟」非「起草中」 | 是 |
| 夹具加 `lifecycle_status: "drafting"` | test L13：`contract-dddd4444` | 是 |
| 表格行显示草拟、不含未知状态、不含英文 drafting | test L94-97 对 `merchant-zone-contract-row-contract-dddd4444` | 是 |
| 抽屉同上 | test L98-102 点行后读 `row-detail-drawer` | 是 |
| 两文件 V0.01→V0.02 | 两文件 L1；before 树均为 V0.01 | 是 |
| 不改 `types.ts` | 两树 `name-only` 无此路径 | 是 |
| 不改后端 | 同上，`backend/**` 空 | 是 |
| 不改全局 `statusLabels` | 同上；现值仍 `drafting: "起草中"`（`apps/web/src/customer/i18n/statusLabels.ts:54`） | 是 |
| 其余 7 键不动 | after 键序：`drafting, pending_confirmation, pending_effective, effective, renewing, expired, terminated, archived` | 是 |
| 不为 `amended` 自拟中文名 | 页内字典 `amended` 计数 0；无新中文词 | 是 |

### 5.2 与现行后端是否一致（第一性原理，类型 4）

数据流（只读）：

1. `ContractService.createDraft` L206 落库 `"drafting"`；`submit` L291 只认 `"drafting"`；驳回 L330 写回 `"drafting"`。仓库内 `draft` 作为合同生命周期真码：本路径 0。
2. `V314__contract_scope_grant_and_amendment.sql:15-17` `chk_contracts_lifecycle_status` 闭集：
   `drafting, pending_confirmation, pending_effective, effective, renewing, expired, terminated, archived, amended`。
   含 `drafting`、不含 `draft`。第 9 态 `amended` 是 B2-r2 登记不入队对象。
3. `JdbcContractRepository.listContractsByMerchant` L286-289：
   `SELECT … FROM contracts WHERE merchant_id = :mid ORDER BY created_at DESC, id`——无 `lifecycle_status` 谓词。
4. `MerchantPortalService.listMyContracts` L63-64：`PageResponse.of(contractRepository.listContractsByMerchant(merchantId), page, size)`，无再过滤。
5. 页面 `statusLabel`（L32-35）：`STATUS_LABELS[code] ?? UNKNOWN_STATUS`。键改为 `drafting` 后，真码命中「草拟」；未知码仍走 003 的「未知状态」（`amended` 继续占位，不泄英文）。

页内 8 键与 CHECK 前 8 项逐个对上；`amended` 故意不加——符合「登记不入队」。全局 `起草中` 未引入本页，符合「本页沿用页内字典」。枚举 HTML L239 八态中文「草拟…归档」仅作分类对照（007 已写明不作本条判据）；本 diff 未把 L239 八态扩成九态中文。

### 5.3 有无业务发明或越界

对抗式自问：

- 是否发明第 9 态中文名？否。字典无 `amended`。
- 是否把全局「起草中」写进本页？否。词仍「草拟」。既有 003 用例仍断言表格 `not.toContain("起草中")`（本 diff 未改该断言），若误用全局词会红。
- 是否改 `types.ts` / 后端 / `statusLabels` / Scope / Settlements？两树 `name-only` 仅 2 文件。
- 夹具多出的日期/押金/空空间是否新业务？否。与 `cccc3333` 同构，只为让行能渲染；007 要求加这一条。
- 新用例插在 003 与租期用例之间是否改了 003/005 语义？否。003 仍靠 `weird_status` 出「未知状态」；005 仍用 `aaaa1111` 纯文本。新增第 4 行后「已生效」仍为 1（KPI 用例未破）。
- 版本行是否无 ID？否。007 明文要求升 V0.02。

ponytail-review：无新组件、无新依赖、无双字典、无 `formatBusinessStatus` 回潮。`net: -0`。Lean already. Ship.

### 5.4 测试是否真能红（GEN-003）

禁止改工作区键做破坏实验。用已有 RED 产物 + 源码路径证明「键改回 `draft` 该用例失败」：

- 执行日志：`== RED: new test against old page`；旧页仍是 `draft: "草拟"`。
- `/home/james/audit-tmp/m023_b3r2_red.log`：新用例 1 失败 / 其余 31 绿。
  失败点 test L95：`expected '…dddd4444…未知状态…' to contain '草拟'`。
  即 `statusLabel("drafting")` 在 `draft` 键下走 `?? UNKNOWN_STATUS`。若键改回 `draft`，同一断言必再红（`toContain("草拟")` 与 `not.toContain("未知状态")` 两条都会失败）。
- GREEN 日志：三文件 32/32。
- 本席当场重跑（只读，未写日志文件）：
  ```
  cd apps/web && npm run test:unit -- \
    src/admin/pages/MerchantScopePage.test.tsx \
    src/admin/pages/MerchantZoneContractsPage.test.tsx \
    src/admin/pages/MerchantZoneSettlementsPage.test.tsx
  → Test Files  3 passed (3)
    Tests  32 passed (32)
  Duration 1.44s  exit 0
  ```
- 派发 tsc 日志：`TSC_EXIT=0`（`tsc -b --noEmit` 无诊断）。本席未另建产物。

断言打在真实渲染文本（行 testid + 抽屉 testid），不是字典对象本身——把键改回 `draft` 或改成显示英文码/「起草中」都会红。非 tautological。

### 5.5 001 至 006 有无被碰

| ID | 原写入面 | 本 diff |
|---|---|---|
| 001 / 002 | Scope 页+测 | 两树 0 字节 |
| 003 | 三页未知码占位；合同页 `UNKNOWN_STATUS` + 测「未知状态」 | 合同页该常量与 003 用例子未改；仅共享夹具多一行 drafting（007 要求） |
| 004 / 006 | Settlements 页+测 | 两树 0 字节 |
| 005 | 合同页纯文本 + 测 trace-link | 005 用例子未改 |

结论：001–006 处置面未被改写。

**内容票：PASS**

---

## 6. 第一性原理三问（2.3）

1. **这段东西为什么必须存在？** 删掉键改，商户专区「我的合同」（业务地图 L1322）在平台写入 `drafting` 后状态列/抽屉落到「未知状态」，与页内已有词「草拟」及后端真码不一致（GEN-008）。HTML 枚举 L239 有「草拟」无 `draft` 英码。答得上 → 细化，非添加。
2. **声称的行为有没有被实际执行验证？** 有。RED：新用例对旧页失败在「未知状态」而非「草拟」。GREEN + 本席重跑：32 绿。源码 `statusLabel` 是唯一展示缝。
3. **哪条前提没验证就当真？** 无。后端真码、CHECK 闭集、列表无状态过滤均当场读到。未假设「全局 statusLabels 会救本页」——本页不用它，且 003 禁止「起草中」。

---

## 7. 最终票

| 维度 | 票 | 说明 |
|---|---|---|
| 归因（闸 3） | **PASS** | 5 hunk / 21 条 `+/-` 内容行全部归 M023-007；无越界行 |
| 内容 | **PASS** | 最小处置逐条落地；与 ContractService / V314 / `listContractsByMerchant` 一致；无业务发明；测试能红；001–006 未碰 |
| 合取 | **PASS** | 两票皆 PASS 才 PASS；本席不修、不问 |

本席 B4-r2 结论：**PASS**

（CLI 不回正文或机器标记。本文件即结论出处。）
