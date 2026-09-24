CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M023 B4-r1 Grok 席乙只读审计（修复 diff）
版本：V0.01
席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
步骤：B4-r1（§4 B4 + §5 闸 3）
正本：`/home/james/complex-management` main 主工作区
对象：席甲 B3-r1 整批修复 diff（网页与验证模块）
findings 正本：`docs/audit-2026-09/findings.md` V2.72，归属「是」且已定稿：M023-001…006
独立性：本席未读 `M023-b4-r1-self-raw.md`；未派 MCP / 子代理 / 第三席；未问人；未改业务 / HTML / 锁 / findings / ledger；未 fetch / commit / push / merge / deploy；未建脚本、日志或临时产物。旧 B1 可对照，本轮以 V2.72 定稿处置为准。

---

## 0. 技能预检（§6，按本环境实核）

核到且按收窄口径使用：
1. `using-superpowers`：开工技能自检。
2. `graft`：定位后端记录、portal 异议、AdminApi、PermissionButton、路由与 navigateTo（未改那些文件，只读对照）。
3. `grilling` / `grill-me`：对抗式自问自答；本体「等用户 / 派子代理」作废。
4. `ponytail-review`：只找可删的多余抽象；不改码。
5. `verification-before-completion`：PASS 前当场跑三份单测。
6. `systematic-debugging` / `diagnosing-bugs` / `tdd`：只借读错误→复现→追数据流；对照 RED/GREEN 日志后本席重跑 GREEN。
7. `requesting-code-review`：B4＝本席即外审；不用 HEAD~1、不用子代理复审。
8. `codebase-design`：核页内局部类型 vs 不改 `types.ts` 的缝。
9. `writing-for-agents`：本 raw 给下游席读。
10. `i-have-adhd`：编号、无寒暄；本文件第一行仍必须是判据锁哈希（提示词覆盖输出模式）。
11. `using-arc`：已读；其默认「派评审子代理」与双席铁律冲突，本席不走 Arc 审计流程。

明确不用（提示词划掉或本步不适用）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`executing-plans`、`research`、`ponytail-audit`、mattpocock `code-review`。`brainstorming` / `writing-plans` / `ponytail` 本体属 A/B3，本步只读不修。

缺口（实找无目录，不冒充已调用）：`batch-grill-me`、`ubiquitous-language`。
可选存在未用：`git-guardrails-claude-code`（本席只读、无 git 写）。

---

## 1. 输入身份（类型 4）

本席独立实算：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
→ b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```
未从 sidecar / ledger / 派发词抄写。

冻结 diff：
```
sha256sum /home/james/audit-tmp/m023_b3_r1.diff
→ c40950302b84891de3e3d776d1b498d1ae5b3ee815ea97853553b27e35b60735
wc -c → 68654
```
与派发声明一致。

tree：
- before `bccb4a116bcbd5752a105a8b5009f7b62ae21626` 类型 tree
- after  `1812e0f7017ccf3268183b8b94ea25521e246fc3` 类型 tree

```
git diff --binary <before> <after> | sha256sum
→ c40950302b84891de3e3d776d1b498d1ae5b3ee815ea97853553b27e35b60735
git diff --stat：6 files changed, 434 insertions(+), 170 deletions(-)
git diff --name-status：仅下列 6 个 M
```

改动闭集（即本模块全部单元，均首行 `// 文件版本：V0.01`）：
- `apps/web/src/admin/pages/MerchantScopePage.tsx`
- `apps/web/src/admin/pages/MerchantScopePage.test.tsx`
- `apps/web/src/admin/pages/MerchantZoneContractsPage.tsx`
- `apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx`
- `apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx`
- `apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx`

主工作区这 6 个文件的 git blob 与 after tree 全等。  
`types.ts` / `AdminApp.tsx` / `TraceLink` / `adminRoutes.ts` / `backend/**` 不在两树 diff 内。

基线：before tree 含 2026-09-11 未合并本地提交（PR #444）现状，不是 origin/main。留言表单对照另用 `origin/main` 旧实现（见 §4.6）。

hunk 数：冻结 diff 41 个 `@@`（与执行日志 `hunks=41` 一致）。闸 3：每一行须归到 V2.72 归属「是」的 ID；归不到即越界。测试整段重写按用例归 ID。

---

## 2. 定稿最小处置（审计标尺，摘 V2.72）

| ID | 分类 | 最小处置 |
|---|---|---|
| M023-001 | 细化 | 金额读 `totalAmountCent` 分→元；删后端不存在的列与「授权范围」KPI；行类型页内收口（不改 `types.ts`）；夹具改真实形状。不新增列（spaceId/contractId 展示＝可选增强，不入本条） |
| M023-002 | 细化 | 删确认按钮 / `confirmScope` / `TEXT.confirm`；AuditTrailHint 改真实事件；TEXT.evidence 改真实权限码；测试同步 |
| M023-003 | 细化 | 授权工作台账单状态页内字典（与 `MerchantZoneBillsPage` 同词）；结算字典补 `voided: "已作废"`；三页未知码中文占位 |
| M023-004 | 细化 | 未结清＝状态不在 `(settled, voided)`；测试补 voided 行 |
| M023-005 | 细化 | 对账页合同下钻改 ADM-MER-006；合同页合同号/空间/续签自、授权工作台商户号降纯文本；不改 AdminApp/TraceLink/路由 |
| M023-006 | 冲突 | 抽屉渲染 `settlementItems`（条目中文名、amountCent 分→元、note）；恢复留言：可留言态 pending_settlement/deposit_offsetting/arrears_collecting，理由≤500 字，既有 POST，权限 `merchant.portal.settlement.dispute`；文案写明不改状态、不暂停付款、正式异议走退租审批。不改后端、不发明新状态/KPI |

只登记不入队（不得混进本 diff）：工单 spaceId/contractId 展示；月度结算/分成明细；合同关键条款正文等。

---

## 3. diff 逐块归因（41 hunk）

归因符号：主 ID；兼有则并列。版本行随该文件主修复 ID 记，不另开无 ID 行。

### 3.1 MerchantScopePage.test.tsx（H01–H06）

| hunk | 旧→新 | 内容 | ID |
|---|---|---|---|
| H01 `@@ -1,3 +1,4` | +版本行 | `// 文件版本：V0.01` | 001（本文件主修；派发指定 6 文件均加） |
| H02 `@@ -5,13 +6,16` | 夹具 | 删 `authorized_scope`/`title`/`total_amount`；账单改 `total_amount_cent`+`issued`；加 viewed/未知码行 | 001 夹具形状；003 状态夹具 |
| H03 `@@ -43,8 +47,9` | renderPage | 补 `navigateTo: vi.fn()`；`return render(...)` | 005（AdminPageProps 仍要该槽；容器查询用返回值） |
| H04 `@@ -58,7 +63,7` | KPI 横幅 | 「商户账单（1）」→「（3）」 | 001+003（本商户账单夹具变为 bl-1/3/4） |
| H05 `@@ -107,11 +112,71` | 既有平台管理员用例收紧 + 新 describe | 无范围：断言中文「缺少商户授权范围」且 `fetch` 未调用（页面该路径未改，随本文件重写收紧既有「不看全量」用例，非新行为）。新用例：分转元金额与 KPI 合计；issued≠已出票 / viewed / 未知状态；无幽灵列与确认按钮；真实权限码与 `merchant_billing.bill.viewed`；商户号无 trace-link | 001（金额/列/KPI/既有范围用例）；002（确认按钮与权限/事件文案）；003（账单字典）；005（纯文本商户号） |
| H06 `@@ -133,12 +198,14` | 抽屉 | 加分转元与「已出账」、无 CNY | 001+003 |

import 行：本文件 import 集合未增业务模块；H03 只补 props。无越界 import。

### 3.2 MerchantScopePage.tsx（H07–H15）

| hunk | 内容 | ID |
|---|---|---|
| H07 `@@ -1,28 +1,53` | 版本行；import 去掉 `MerchantScopeBillRow`/`MerchantScopeWorkOrderRow`/`TraceLink`，保留 `AdminPageProps`+`formatBusinessStatus`；页内 `WorkOrderRow`/`BillRow`；`TEXT.evidence` 去掉 `merchant.portal.read`；删 `TEXT.confirm`；页内 `BILL_STATUS_LABELS`（已出账/已查看/已确认/有异议/已逾期/已结清）+ `billStatus`/`zh` | 001 类型收口；002 evidence/confirm；003 字典与未知码；005 去掉 TraceLink import |
| H08 `@@ -39,15 +64,12` | `display` 不再把数字当元；`moneyYuan`→`moneyCent`（/100） | 001 |
| H09 `@@ -55,11 +77,11` | 签名去掉 `setActionResult`/`navigateTo`；state 改页内行类型；抽屉注释去掉 TraceLink | 002+005+001 |
| H10 `@@ -95,8 +117,8` | `api.get` 泛型改页内类型 | 001 |
| H11 `@@ -104,49 +126,30` | 删 `authorizedScopes` 与 `totalAmount` 汇总；`totalBillCent`；整段删除 `confirmScope`；抽屉去幽灵字段/假下钻，金额用 cent，状态用 `billStatus`/`zh` | 001+002+003+005 |
| H12 `@@ -156,8 +159,8` | 工单表去「授权范围」「留痕证据」列与 TraceLink | 001+003+005 |
| H13 `@@ -168,8 +171,8` | 账单表去标题/授权范围/留痕证据；金额 cent；`billStatus` | 001+003+005 |
| H14 `@@ -181,7 +184,7` | AuditTrailHint → `merchant_service.work_order.viewed / merchant_billing.bill.viewed` | 002 |
| H15 `@@ -194,14 +197,12` | 删「授权范围」KPI；账单金额 KPI 改 cent+testid；删确认按钮 | 001+002 |

未读 `spaceId`/`contractId`，未扩 `types.ts`。`zh()` 只包 `formatBusinessStatus`：已知码（repair→报修、processing→处理中、high→高）仍中文，未知码才「未知状态」，落 003「三页未知码」。

### 3.3 MerchantZoneContractsPage.test.tsx（H16–H23）

| hunk | 内容 | ID |
|---|---|---|
| H16 版本行 | V0.01 | 003/005（本文件两 ID；版本标记） |
| H17 夹具 | 续签自、双空间、`weird_status` | 005+003 |
| H18 renderPage | 返回 `{props, container}` | 005 |
| H19 KPI 聚焦 | 去掉「TraceLink 保留」；按 row testid 断言 | 005 |
| H20 状态列 | 未知码「未知状态」；不含 `weird_status` | 003 |
| H21 新用例 | 合同号/空间/续签自纯文本，无 `trace-link-`，`navigateTo` 未调用 | 005 |
| H22 抽屉 | 全量合同号、押金、空间、续签自；无假按钮 | 005（展示已有字段改纯文本）；金额格式 001 同类分转元既有 `money` |
| H23 删除 | 「点合同下钻 TraceLink」整用例 | 005 |

import 行未新增。

### 3.4 MerchantZoneContractsPage.tsx（H24–H28）

| hunk | 内容 | ID |
|---|---|---|
| H24 | 版本行；删除 `TraceLink` import | 005 |
| H25 | `UNKNOWN_STATUS`+`statusLabel` | 003 |
| H26 | 注释去掉「TraceLink 保留」；写明商户不可达故纯文本 | 005 |
| H27 | 签名去掉 `navigateTo`；抽屉合同/空间/续签自改文本；状态走 `statusLabel` | 005+003 |
| H28 | 表内三处 TraceLink 改纯文本；状态占位 | 005+003 |

无新列、无新 KPI。

### 3.5 MerchantZoneSettlementsPage.test.tsx（H29–H34）

测试文件大段重写，按用例归 ID：

| hunk / 用例 | ID |
|---|---|
| H29 版本行 | 006（本文件主修） |
| H30 夹具 `settlement_items` / voided / weird_* | 006 明细形状；004 voided；003 未知码 |
| H31 `makeApi` 记录 POST、`single()` 信封、dispute 桩返回 `pending_settlement`（不改成 disputed） | 006 |
| H32 从「没有表单」改写成留言 describe：表单+文案+无「有异议」KPI；trim 后 POST `/merchant-portal/settlements/{id}/dispute` 带 `reason`+`idempotency_key`；空/501 字不发请求；settled/voided 按钮禁用；无 `merchant.portal.settlement.dispute` 时按钮禁用且无 POST。KPI：未结清计数 3、聚焦排除 settled+voided。中文：已作废/未知状态/未知触发 | 006；004；003 |
| H33 抽屉明细中文名+分转元+ADM-MER-006 | 006+005 |
| H34 下钻 `navigateTo("ADM-MER-006","c-1")`，断言无 ADM-CONT-001 | 005 |

### 3.6 MerchantZoneSettlementsPage.tsx（H35–H41）

| hunk | 内容 | ID |
|---|---|---|
| H35 `@@ -1,17 +1,32` | 版本行；import `useAdminAction`/`useSelectedRow`/`PermissionButton`（既有钩子，非新抽象文件）；`DISPUTE_PERMS`；`REASON_MAX=500`；TEXT 改为 L1322 留言口径 | 006 |
| H36 `@@ -28,10 +43,52` | `voided: "已作废"`；`ITEM_LABELS` 三键与 `TerminationSettlementMath` 一致；未知状态/触发/条目；`DISPUTABLE` 三态（无 `in_progress`）；`CLOSED={settled,voided}`；页内 `SettlementItem`；`money`/`nextKey`/`makeResult` | 003+006+004 |
| H37 | 签名加 `setActionResult`/`effectivePermissions`（006 恢复表单所需既有 props） | 006 |
| H38 | `useSelectedRow`+理由；`submitDispute`（选中、可留言态、trim、500、既有 POST、成功才清空）；`openRows` 改 `!CLOSED` | 006+004 |
| H39 | 表头加「选择」、理由列改「留言理由」；radio；合同 TraceLink→ADM-MER-006；`statusLabel`/`triggerLabel`；抽屉渲染 `settlementItems` | 006+005+003 |
| H40 | AuditTrailHint 补真实事件 `merchant_portal.settlement.disputed` | 006（对照 002 的「改真实事件」；后端 `MerchantPortalService:160` 确有该事件） |
| H41 | 摘要区留言表单：`PermissionButton` 门控、hint 文案、formError | 006 |

### 3.7 归因结论

41 hunk 均可落到 M023-001…006。未见「顺手优化 / 换框架 / 抽共享包 / 改 types 或路由」无 ID 行。  
版本标记 6 处、import 增删均能归到该文件主 ID。  
**归因票：PASS。**

---

## 4. 内容审计

### 4.1 与现行后端/前端依赖（只读对照，类型 4 + 源码原文）

**MerchantBillRecord**（`MerchantBillRecord.java:7-24`）：`id, merchantId, arApItemId, billNo, billPeriodStart/End, dueDate, totalAmountCent, currency, status, agingDays, sourceType, sourceId, lines, createdAt, updatedAt`。无 `title` / `authorizedScope` / `auditEvidenceRef` / `totalAmount`。页内 `BillRow` 只取真实子集；金额走 `totalAmountCent`。

**MerchantWorkOrderRecord**：有 `title`、`workOrderType`、`priority`、`status`、`merchantId`；有 `spaceId`/`contractId` 但本 diff 不展示（001 明确可选增强不入条）。无授权范围/留痕证据字段。

**AdminApi**：测试夹具蛇形键 + `{success,code,data:{items}}` 信封；`unwrap` 驼峰化并拍成数组；POST `normalizeRequestBody` 把 `idempotencyKey` 写成 `idempotency_key`（测试断言的正是线格式）。页读 `settlementItems`/`amountCent` 与 unwrap 后形状一致。`types.ts` 已有 `MerchantZoneSettlementRow.settlementItems?: unknown[]`，本 diff 未改该文件，页内再收窄 `SettlementItem`。

**MerchantPortalController V0.01**：`POST /api/v1/merchant-portal/settlements/{settlement_id}/dispute`。  
**MerchantPortalService V0.01**：`DISPUTABLE_STATUSES = pending_settlement | deposit_offsetting | arrears_collecting`（无 `in_progress`）；`disputeOnce` 只合并 `disputeReason`，构造新 record 时 `settlement.status()` 原样写回；审计事件 `merchant_portal.settlement.disputed`；权限 `merchant.portal.settlement.dispute`；列宽 500。页侧 DISPUTABLE / 权限码 / 事件名 / 500 与此同文。

**TerminationSettlementMath:40-75** item 键：`unpaid_receivable_summary`、`deposit_disposal`、`termination_penalty`。页 `ITEM_LABELS` 三键全覆盖；未知键「其他条目」。amountCent 分→元。

**PermissionButton**（`Can.tsx:56-84`）：`blocked = disabled \|\| !allowed`；`onClick` 内 `if (!allowed) return`。无权限时视觉禁用且拦截请求。VIEW_ONLY 用例覆盖。

**useAdminAction / useSelectedRow**：既有钩子。后者空列表清选择、否则无匹配则落到首行——空理由用例不先点 radio 仍打到「必须填写理由」，与首行 st-1 可留言态一致，不是漏测。

**adminRoutes ADM-MER-006**：`label: "我的合同"`，`requiredPermissions: ["contract.view"]`，`accountForms: ["merchant"]`。商户账号可达。  
**AdminApp.navigateTo:545-552**：无权限或账号形态不匹配则静默 return。故 005 把不可达目标改纯文本 / 改指 006，而不改 AdminApp。

**MerchantZoneBillsPage:18-25**：issued/viewed/confirmed/disputed/overdue/closed ＝ 已出账/已查看/已确认/有异议/已逾期/已结清。Scope 页 `BILL_STATUS_LABELS` 逐词相同。

### 4.2 按 ID 核最小处置

**001** 金额 cent、幽灵列与授权范围 KPI 已删、页内类型、夹具真实形状。未加 spaceId/contractId 列。通过。

**002** 确认按钮/`confirmScope`/`TEXT.confirm` 删除；after 树 `MerchantScopePage.tsx` 对 `merchant-scope-confirm`/`confirmScope` 0 命中；证据码改为 `merchant.service_work_order.view`+`merchant.billing.view`；事件改为工单/账单 viewed。通过。

**003** 账单字典与账单专区同词；结算补「已作废」；三页未知码中文占位（未知状态/未知触发/其他条目）。RED 日志旧页 `issued`→「已出票」、`viewed`/`weird_code` 原样英文，恰是本条要修的。通过。

**004** `CLOSED={settled,voided}`；KPI 未结清＝3（pending×2 + weird_status；settled/voided 排除）。未知态计入未结清符合「不在 (settled,voided)」公式，未发明第三关闭态。通过。

**005** 对账合同下钻 ADM-MER-006；合同页与授权工作台商户号无 `trace-link-*`。未改 TraceLink 组件/路由/AdminApp。通过。

**006** 抽屉渲染 items；表单走既有 POST；权限码一致；文案含不改状态、不暂停付款、退租审批。POST 桩返回 `status: "pending_settlement"`，测试不期望变成 disputed。无「有异议」KPI（testid 断言 null）。通过。

### 4.3 业务发明 / 越界

未改 types.ts / AdminApp / TraceLink / 路由 / 后端。  
未新增 KPI 卡片（授权范围 KPI 是删除；有异议 KPI 未恢复）。  
未新增业务状态值：`voided` 是 V307 已有态的中文标签。  
`STATUS_LABELS.disputed = "有争议"` 在 before 树已存在，本 diff 只追加 voided，未把结算推进 disputed。  
未把工单 spaceId/contractId 做成新列。  
**无越界、无只登记项混入。**

### 4.4 界面语言

用户可见文案（标题、表头、按钮、hint、错误、占位「如：违约金多算一期」、aria-label「选择此结算单」）均为中文。  
权限码、API 路径、审计事件名出现在 TechDetails / AuditTrailHint，属内部标识（V2.72 只登记项已说明 TechDetails 路径/事件名）。未知码不回显英文原码。

### 4.5 测试是否按真实后端形状与真实现

- 夹具：账单 `total_amount_cent`+真实状态码；结算 `settlement_items` 蛇形键；经真实 `AdminApi` unwrap。
- POST 断言路径 `/api/v1/merchant-portal/settlements/st-2/dispute`、`reason` 为 trim 后文本、`idempotency_key` 非空。
- 权限缺省不发 POST；settled/voided 不能留言。
- RED：新测试对旧页面 18 失败（本席读 `/home/james/audit-tmp/m023_b3r1_red.log`：Contracts 3 + Scope 6 + Settlements 9）。旧账单行实为 `已出票` 且含「标题/授权范围/留痕证据」，证明断言能红。
- GREEN 日志 31 全过；`tsc -b --noEmit` TSC_EXIT=0。
- 本席当场：`apps/web` `npm run test:unit --` 三文件，exit 0，**31 passed / 3 files**（2026-09-20 13:03:02，665ms）。
- 改坏页面会红：RED 已表明页面回退则 18 条新断言失败。

执行日志「scope page: … confirm=14」：after 树页面无 `merchant-scope-confirm` 字符串；仅测试负向断言。与派发说明一致，按固定字符串计，非残留。

### 4.6 恢复的留言表单 vs origin/main 旧实现

`git show origin/main:apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx`：

| 项 | origin/main | 本 after |
|---|---|---|
| DISPUTABLE | **含 `in_progress`** | 仅三态，**无 in_progress** |
| 「有异议」KPI / subset=disputed / 「待平台裁决」 | 有 | **无**（测试断言 kpi-disputed 为 null） |
| 理由 | 只查 `!reason.trim()`，**POST 仍送未 trim 的 reason**；无 500 上限 | **trim 后发送**；`trimmed.length>500` 拦截；input `maxLength=500` |
| 可留言态校验 | 仅按钮 disabled | 按钮 + `submitDispute` 内再查 DISPUTABLE |
| 成功才清空理由 | `runAction` 后无条件 `setReason("")` | `if (ok) setReason("")` |
| 文案 | 「提交异议」「平台裁决后继续结算」 | 「提交留言」+ 不改状态/不暂停付款/正式走退租审批 |
| PermissionButton | 已有 | 仍用，未弱化 |

本恢复去掉了已废的 `in_progress` 可留言态，且未把「有异议」KPI 带回来，符合 006「不发明新状态/KPI、不推进 disputed」。

### 4.7 派发点名的其余核项

- **提交前 trim 与 500 字**：`submitDispute` 先 `reason.trim()`，空则中文「必须填写理由」，超 500 则「留言不能超过 500 字」且不 `runAction`。测试：`"  违约金多算一期  "` → body.reason `"违约金多算一期"`；`多.repeat(501)` 无 POST。通过。
- **无权限 PermissionButton 拦截**：`VIEW_ONLY_PERMS` 仅 `merchant.portal.settlement.view`；按钮 `disabled===true`；`fireEvent.click` 后无 POST（组件内 `if (!allowed) return`）。通过。
- **可选增强未混入**：无 spaceId/contractId 列、无新 KPI、无月度分成、无关键条款正文、无 types/路由/后端改动。

### 4.8 ponytail-review（只评多余，不改码）

`nextKey`/`makeResult`/`useAdminAction`/`useSelectedRow`/`PermissionButton` 均为 origin 已用或仓内既有模式，006 恢复表单所需。页内字典复制账单专区用词，未抽新共享模块（抽模块会越界到别文件）。  
Lean already. Ship. net: -0。

### 4.9 对抗式自问（grilling 收窄：事实自读）

1. 表单是否会把状态打成 disputed？POST 体只有 idempotencyKey+reason；服务端写回原 status。否。
2. KPI 聚焦里没有表单是否越界？origin 同样只在非聚焦摘要区放表单；本 diff 同结构。否。
3. `merchant_zone_settlement_disputed` 动作名是否发明新状态？origin 已用的 `useAdminAction` action 键，不写入结算 status。否。
4. 平台管理员用例加「不请求」是否新需求？before 已 throw `TEXT.scopeMissing`；只是测试对齐真实错误节点。否。

### 4.10 内容票

最小处置覆盖 001–006；与现行依赖一致；无业务发明；中文界面；测试按真形状且能红能绿。  
**内容票：PASS。**

---

## 5. 最终

- 归因：PASS（41 hunk / 全部 +/− 行均可归 M023-001…006）
- 内容：PASS
- **B4-r1 本席：PASS**（两票皆 PASS；任一 FAIL 才 FAIL）

证据命令（本席实跑）：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
sha256sum /home/james/audit-tmp/m023_b3_r1.diff
git diff --binary bccb4a1… 1812e0f… | sha256sum
cd apps/web && npm run test:unit -- src/admin/pages/MerchantScopePage.test.tsx \
  src/admin/pages/MerchantZoneContractsPage.test.tsx \
  src/admin/pages/MerchantZoneSettlementsPage.test.tsx
# exit 0；Tests 31 passed (31)
```
