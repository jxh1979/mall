CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M023 B5-r2 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；6 文件现行版本均为席甲 B3 所写，且席甲 B5-r1 的 PASS 已被 Grok 推翻过一次，以 Grok 票为准）。2026-09-20T20:56:47Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；B3-r2 after tree `144c4edd74f86cffe1c6aa92ed1fc8d3bd8b341f`。
- 模块 M023 6 前端文件（§1 全读），21 判据；B3 累计 2。
- 顺序：Grok 已先派（modules/M023-b5-r2-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件。
- 本轮补的方法：B5-r1 漏在「页面字典只对了枚举 HTML，没对后端闭集」。本轮把三页每一张页内字典、每一个硬编码状态码/条目键/权限码/事件名/端点，逐个拿去对后端源码与 complex_test 库内 CHECK 闭集（只读 SELECT），结果见 §2b。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| apps/web/src/admin/pages/MerchantScopePage.tsx | 217 | 11606 | c328add4b8e4767b6c21f4f9766dbdd4da41fdd05d08d26f6229853d5b7579e9 | 全读 |
| apps/web/src/admin/pages/MerchantScopePage.test.tsx | 211 | 10833 | aec26699fe171bcad839f774b012d45e0812f70de23706efeb9829f65c98be71 | 全读 |
| apps/web/src/admin/pages/MerchantZoneContractsPage.tsx | 175 | 8771 | 8a932c358b95fd24fcebfada8126f006813f16130ac161317b3fd25f615dee9f | 全读 |
| apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx | 157 | 8092 | f9a704f44313a8a83abfce7a661b0a78527f97fd8da6f9e040e357fdc52b1485 | 全读 |
| apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx | 272 | 13660 | 271ae798a161f18a37befb5f64b3b8a4dcede4348babfc286715116ded3dd5e8 | 全读 |
| apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx | 217 | 12434 | 2f8b257279ca40e09f3246867dee177ff5876b464ec36f195da53e4044e61f4b | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m023_b5r2_run.log；B1 同一取证脚本修后重跑，含 vitest 三文件实跑）

```
START=2026-09-20T20:56:27Z HEAD=e456ba96 origin/main=89686538
== 0. six files (lines/bytes/sha256)
apps/web/src/admin/pages/MerchantScopePage.tsx 217 11606 c328add4b8e4767b6c21f4f9766dbdd4da41fdd05d08d26f6229853d5b7579e9
apps/web/src/admin/pages/MerchantScopePage.test.tsx 211 10833 aec26699fe171bcad839f774b012d45e0812f70de23706efeb9829f65c98be71
apps/web/src/admin/pages/MerchantZoneContractsPage.tsx 175 8771 8a932c358b95fd24fcebfada8126f006813f16130ac161317b3fd25f615dee9f
apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx 157 8092 f9a704f44313a8a83abfce7a661b0a78527f97fd8da6f9e040e357fdc52b1485
apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx 272 13660 271ae798a161f18a37befb5f64b3b8a4dcede4348babfc286715116ded3dd5e8
apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx 217 12434 2f8b257279ca40e09f3246867dee177ff5876b464ec36f195da53e4044e61f4b
== 1. vs HEAD (expect empty) / vs origin/main / local unmerged commits / open PR
 6 files changed, 450 insertions(+), 171 deletions(-)
 6 files changed, 459 insertions(+), 201 deletions(-)
d96942eb audit M023: settlement page copy does not say submit dispute
c6e9af1d audit M023: drop settlement dispute form; bill amounts in yuan
3051dce9 audit M023: merchant portal labels, notes, and no cross-merchant dump
open PR head audit-m023-r1: 444
== 2. unit tests actually run (vitest, three files)
VITEST_EXIT=0
 ✓ src/admin/pages/MerchantZoneContractsPage.test.tsx (8 tests) 112ms
 ✓ src/admin/pages/MerchantScopePage.test.tsx (14 tests) 141ms
 ✓ src/admin/pages/MerchantZoneSettlementsPage.test.tsx (10 tests) 161ms
 Test Files  3 passed (3)
== 3. scope page fields vs backend records
page reads:  3 row.billNo; 6 row.merchantId; 2 row.priority; 4 row.status; 3 row.title; 3 row.totalAmount; 2 row.workOrderType;
MerchantBillRecord fields: id merchantId arApItemId billNo billPeriodStart billPeriodEnd dueDate totalAmountCent currency status agingDays sourceType sourceId lines createdAt updatedAt 
MerchantWorkOrderRecord has authorizedScope/auditEvidenceRef: 0; backend main total: 0
bills endpoint returns: 35:    public ApiResponse<PageResponse<MerchantBillRecord>> listBills() {
test fixtures fabricate: 15 16 17 18 131 
== 4. fake audited action / nonexistent events and permission
backend emits merchant.scope* events: 0; permission merchant.portal.read in migrations: 0; confirmScope calls api: 0
real list events: backend/src/main/java/com/complex/platform/merchantservice/MerchantServiceService.java:80 backend/src/main/java/com/complex/platform/merchantbilling/MerchantBillingService.java:107 
route ADM-MER-002: 48:  { pageId: "ADM-MER-002", label: "商户授权工作台", module: "商户门户", group: "governance", route: "/admin/merchant-portal/scope", requiredPermissions: ["merchant.service_work_order.view", "merchant.billing.view"] }
== 5. english leak paths
settlement STATUS_LABELS keys: pending_settlement deposit_offsetting arrears_collecting settled disputed disputed_litigation voided 
backend settlement statuses (V307 CHECK): IN ('pending_settlement', 'deposit_offsetting', 'arrears_collecting', 'settled', 'disputed', 'voided')
raw-code fallbacks: 
formatBusinessStatus fallback: 259:  return STATUS_LABELS[code] ?? code;
viewed=0 issued=1 confirmed=1 disputed=1 paid=1 overdue=1 cancelled=1 service=1 critical=1 created=1 pending_merchant=1 resolved=1 assigned=1 processing=1 closed=1 
backend closed sets: bill statuses seen in service: "confirmed" "disputed" "issued" "overdue" "viewed" ; work order: Set.of("repair", "complaint", "rectification", "service") Set.of("low", "medium", "high", "critical") Set.of("assigned", "processing", "pending_merchant", "resolved") Set.of("closed", "cancelled") 
== 6. open KPI semantics
== 7. drill-down targets reachable for the pages' audience?
17:  { pageId: "ADM-GOV-002", label: "组织与商户档案", module: "治理", group: "governance", route: "/admin/governance/organizations", requiredPermissions: ["merchant.profile.view", "governance.project.view"] },
19:  { pageId: "ADM-CONT-001", label: "合同生命周期工作台", module: "合同", group: "governance", route: "/admin/contracts", requiredPermissions: ["contract.draft.manage"] },
50:  { pageId: "ADM-GOV-007", label: "空间类型字典", module: "治理", group: "governance", route: "/admin/governance/space-types", requiredPermissions: ["governance.space.view"] },
91:  { pageId: "ADM-MER-005", label: "我的对账", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/settlements", requiredPermissions: ["merchant.portal.settlement.view"], accountForms: ["merchant"] },
92:  { pageId: "ADM-MER-006", label: "我的合同", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/contracts", requiredPermissions: ["contract.view"], accountForms: ["merchant"] },
navigateTo silently returns when not permitted:  function navigateTo(pageId: string, objectId?: string) {  const target = adminRoutes.find((item) => item.pageId === pageId);  if (!target || !hasPermissions(effectivePermissions, target.requiredPermissions)) {  return;  }  if (!routeAllowsAccountForm(target, 
trace links in pages:  2 apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx:pageId="ADM-MER-006";
== 8. HTML anchor sentences (read-only)
商户专区自助：我的账单（该交多少、交了多少、还欠多少）；我的工单（报修/投诉/整改自己发起、进度自己看，超时自动升级催办）；我的对账（结算与分成明细，有疑问可留言提异议）；我的
商户对退租结算金额的异议在退租审批流程中提出并解决
待结算、押金抵扣中、欠费追缴中、已结清、有争议、「有争议，诉讼/仲裁」
e2e stale expectation: 917:    await expect(page.getByTestId("merchant-zone-settlement-dispute-form")).toBeVisible();
== 9. HTML + lock sha; tree
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
tree=3b42950c460c3bde0539db3a348f1faa32186884
END=2026-09-20T20:56:29Z
```

说明：探针 3 的正则 `row\.\(totalAmount\|…\)` 会前缀命中 `row.totalAmountCent`，所以「page reads」里的 `row.totalAmount` 计数实为 totalAmountCent（B5-r1 self raw V0.02 已更正）；探针 4「fake audited action」无命中；「raw-code fallbacks」为空；「trace links」只剩对账页 → ADM-MER-006。

## 2b. 页内字典与硬编码码值 vs 后端真值（/home/james/audit-tmp/m023_b5r2_dict.log）

```
START=2026-09-20T20:56:29Z
-- 我的合同 STATUS_LABELS vs chk_contracts_lifecycle_status
   页面键(8): drafting=草拟、pending_confirmation=待确认、pending_effective=待生效、effective=已生效、renewing=续签中、expired=已到期、terminated=已终止、archived=归档
   后端闭集(9): amended、archived、drafting、effective、expired、pending_confirmation、pending_effective、renewing、terminated
   后端有而页面无: ['amended']；页面有而后端无: 无
-- 我的对账 STATUS_LABELS vs chk_contract_settlements_status
   页面键(7): pending_settlement=待结算、deposit_offsetting=押金抵扣中、arrears_collecting=欠费追缴中、settled=已结清、disputed=有争议、disputed_litigation=有争议，诉讼/仲裁、voided=已作废
   后端闭集(6): arrears_collecting、deposit_offsetting、disputed、pending_settlement、settled、voided
   后端有而页面无: 无；页面有而后端无: ['disputed_litigation']
   contract_settlements 触发类 CHECK 名： ['chk_contract_settlements_trigger']
-- 我的对账 TRIGGER_LABELS vs chk_contract_settlements_trigger
   页面键(3): move_out=退租、expiry=到期、early_termination=提前终止
   后端闭集(3): early_termination、expiry、move_out
   后端有而页面无: 无；页面有而后端无: 无
-- 我的对账 ITEM_LABELS vs TerminationSettlementMath put("item", …)
   页面键(3): unpaid_receivable_summary=未清应收合计、deposit_disposal=押金处置、termination_penalty=提前退租违约金
   后端闭集(3): deposit_disposal、termination_penalty、unpaid_receivable_summary
   后端有而页面无: 无；页面有而后端无: 无
-- 我的对账 DISPUTABLE vs MerchantPortalService.DISPUTABLE_STATUSES
   页面: ['arrears_collecting', 'deposit_offsetting', 'pending_settlement']
   后端: ['arrears_collecting', 'deposit_offsetting', 'pending_settlement']
   一致: True
-- 授权工作台 BILL_STATUS_LABELS vs chk_merchant_bill_status
   页面键(6): issued=已出账、viewed=已查看、confirmed=已确认、disputed=有异议、overdue=已逾期、closed=已结清
   后端闭集(6): closed、confirmed、disputed、issued、overdue、viewed
   后端有而页面无: 无；页面有而后端无: 无
-- 授权工作台 工单状态（经全局 formatBusinessStatus）vs chk_merchant_service_work_order_status
   页面键(7): assigned=已派单、cancelled=已取消、closed=已关闭、created=已创建、pending_merchant=商户待处理、processing=处理中、resolved=已解决
   后端闭集(7): assigned、cancelled、closed、created、pending_merchant、processing、resolved
   后端有而页面无: 无；页面有而后端无: 无
-- 授权工作台 工单优先级（经全局 formatBusinessStatus）vs chk_merchant_service_work_order_priority
   页面键(4): critical=紧急、high=高、low=低、medium=中
   后端闭集(4): critical、high、low、medium
   后端有而页面无: 无；页面有而后端无: 无
-- 授权工作台 工单类型（经全局 formatBusinessStatus）vs chk_merchant_service_work_order_type
   页面键(4): complaint=投诉、rectification=整改、repair=报修、service=服务
   后端闭集(4): complaint、rectification、repair、service
   后端有而页面无: 无；页面有而后端无: 无
-- 权限码/事件名/端点 在后端的出现次数（>0 即真实存在）
   merchant.portal.settlement.dispute: 6
   merchant_service.work_order.viewed: 1
   merchant_billing.bill.viewed: 1
   "/api/v1/merchant-portal": 1
   "/contracts": 1
   "/settlements": 1
   "/settlements/{settlement_id}/dispute": 1
   "/api/v1/merchant-service: 1
   "/work-orders": 2
   "/api/v1/merchant-billing: 2
   "/bills": 3
END=2026-09-20T20:56:30Z
```

## 3. 三线审计

- 第一性原理线：上位句同 B1（业务地图 L1322 商户专区自助、L1848-1850 商户侧页面、枚举 L252、治理地图 L256 段）。修后逐条追现行实现：授权工作台只读后端真实字段，无伪动作；三页状态全中文；合同起草态真码 `drafting` 显示「草拟」（007）；未结清不含已作废；下钻只保留商户可达目标；对账页展示结算明细并可留言（note-only）。
- 顺从式线：21 判据逐项——B1 不通过的 PGM-001/GEN-001/002/003/008/011/015/016 由 001–007 闭合；其余同 B1（通过/登记/无本面）。
- 对抗式线（grilling）：后端每个真实状态码在屏幕上显示什么——§2b 逐键对照，合同 8 键、账单 6 键、结算 6 键、触发 3 键、条目 3 键、工单 7/4/4 键全部命中中文；唯一落到「未知状态」的真态是 `amended`（B2-r2 双席定为登记不入队，无 HTML 用词，不自拟）。其余同 B5-r1：无留言权限/已结清/已作废 → 禁用不发请求；空理由/超 500 字 → 中文提示；提交 → POST 带 reason 与 idempotency_key；平台管理员开授权工作台 → 不发请求；把字典键改回 `draft`，007 用例即红（RED 日志）。无新缺陷、无添加项、无 HTML 问题。

## 4. 已定稿 001–007 现态

全部已修（B3-r1 / B3-r2），B4-r1、B4-r2 双 PASS；修复队列「是」= 0。

## 5. 本次 Findings

无新增。

## 6. 观察（登记）

- 后端合同第 9 态 `amended` 无中文名（三份 HTML 未命名；跨模块观察 M254/M041，已报操作员）。其余同 B1/B2 登记项。

## 7. 结论

席甲 B5-r2 票：**PASS**。6 文件全读、21 判据逐项、取证脚本与 vitest 复核、字典逐键对后端闭集；未改任何文件；落盘前未读 Grok 本次正文。
