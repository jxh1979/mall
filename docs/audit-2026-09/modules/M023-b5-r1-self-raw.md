CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M023 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；6 文件现行版本均为席甲 B3 所写，自审性质加倍，以 Grok 票为准）。2026-09-20T20:07:47Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；B3-r1 after tree `1812e0f7017ccf3268183b8b94ea25521e246fc3`。
- 模块 M023 6 前端文件（§1 全读），21 判据；B3 累计 1。
- 顺序：Grok 已先派（modules/M023-b5-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| apps/web/src/admin/pages/MerchantScopePage.tsx | 217 | 11606 | c328add4b8e4767b6c21f4f9766dbdd4da41fdd05d08d26f6229853d5b7579e9 | 全读 |
| apps/web/src/admin/pages/MerchantScopePage.test.tsx | 211 | 10833 | aec26699fe171bcad839f774b012d45e0812f70de23706efeb9829f65c98be71 | 全读 |
| apps/web/src/admin/pages/MerchantZoneContractsPage.tsx | 175 | 8768 | 6c9a0b9a12ebc05a17ac1fc70a3c013cb7660d536a748a12d7d736a4527487a3 | 全读 |
| apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx | 142 | 7129 | 6013ffe5085e736203603588493681b692c9c3f4bfa8ffdbc3dbd9a8833767f5 | 全读 |
| apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx | 272 | 13660 | 271ae798a161f18a37befb5f64b3b8a4dcede4348babfc286715116ded3dd5e8 | 全读 |
| apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx | 217 | 12434 | 2f8b257279ca40e09f3246867dee177ff5876b464ec36f195da53e4044e61f4b | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m023_b5r1_run.log；B1 同一取证脚本修后重跑，含 vitest 三文件实跑）

```
START=2026-09-20T20:07:44Z HEAD=e456ba96 origin/main=89686538
== 0. six files (lines/bytes/sha256)
apps/web/src/admin/pages/MerchantScopePage.tsx 217 11606 c328add4b8e4767b6c21f4f9766dbdd4da41fdd05d08d26f6229853d5b7579e9
apps/web/src/admin/pages/MerchantScopePage.test.tsx 211 10833 aec26699fe171bcad839f774b012d45e0812f70de23706efeb9829f65c98be71
apps/web/src/admin/pages/MerchantZoneContractsPage.tsx 175 8768 6c9a0b9a12ebc05a17ac1fc70a3c013cb7660d536a748a12d7d736a4527487a3
apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx 142 7129 6013ffe5085e736203603588493681b692c9c3f4bfa8ffdbc3dbd9a8833767f5
apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx 272 13660 271ae798a161f18a37befb5f64b3b8a4dcede4348babfc286715116ded3dd5e8
apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx 217 12434 2f8b257279ca40e09f3246867dee177ff5876b464ec36f195da53e4044e61f4b
== 1. vs HEAD (expect empty) / vs origin/main / local unmerged commits / open PR
 6 files changed, 434 insertions(+), 170 deletions(-)
 6 files changed, 444 insertions(+), 201 deletions(-)
d96942eb audit M023: settlement page copy does not say submit dispute
c6e9af1d audit M023: drop settlement dispute form; bill amounts in yuan
3051dce9 audit M023: merchant portal labels, notes, and no cross-merchant dump
open PR head audit-m023-r1: 444
== 2. unit tests actually run (vitest, three files)
VITEST_EXIT=0
 ✓ src/admin/pages/MerchantZoneContractsPage.test.tsx (7 tests) 100ms
 ✓ src/admin/pages/MerchantScopePage.test.tsx (14 tests) 143ms
 ✓ src/admin/pages/MerchantZoneSettlementsPage.test.tsx (10 tests) 159ms
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
tree=3bd58d4f51a4d7ebd659d0e938614da2e3d9fbfb
END=2026-09-20T20:07:47Z
```

说明：探针 3/4/5/7 为 B1 时为查缺陷所写，修后其输出含义相反——「page reads」不再含 totalAmount/authorizedScope/auditEvidenceRef；「fake audited action」段 grep 无命中；「raw-code fallbacks」为空；「trace links」只剩对账页 → ADM-MER-006。

## 3. 三线审计

- 第一性原理线：上位句同 B1（业务地图 L1322 商户专区自助、L1848-1850 商户侧页面、枚举 L252、治理地图 L256 段）。修后逐条追现行实现：授权工作台只读后端真实字段（totalAmountCent 分→元、合计正确），无伪动作，文案的事件名/权限码真实；三页状态全中文（含 voided、viewed、未知码占位）；未结清不含已作废；下钻只保留商户可达目标，其余纯文本；对账页展示结算明细数字并可留言（note-only，文案同时满足 L1322 与 L252）。
- 顺从式线：21 判据逐项——B1 不通过的 PGM-001/GEN-001/002/003/008/011/015/016 由 001–006 闭合；其余同 B1（通过/登记/无本面）。
- 对抗式线（grilling）：真实后端形状夹具下金额/KPI/状态正确；无留言权限 → 按钮禁用且不发请求；已结清/已作废 → 禁用；空理由/超 500 字 → 中文提示不发请求；提交 → POST 到该结算单端点且带 reason 与 idempotency_key；平台管理员开授权工作台 → 不发请求、中文提示；把任一页面行为改回旧实现，RED 日志中的 18 例即再红。无新缺陷、无添加项、无 HTML 问题。

## 4. 已定稿 001–006 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。

## 5. 本次 Findings

无新增。

## 6. 观察（登记）

- 同 B1/B2 登记项（「我的对账」无月度结算与分成明细属后端/产品一期范围，已报操作员；e2e 与验收矩阵对「确认授权范围」的期待随 002 过时，属 e2e/文档模块；ADM-MER-002 路由分组；MerchantBillingService 死分支；可选增强未加）。

## 7. 结论

席甲 B5-r1 票：**PASS**。6 文件全读、21 判据逐项、取证脚本与 vitest 复核；未改任何文件；落盘前未读 Grok 本次正文。


---
## 席甲 B5-r1 票复核撤回与更正（V0.02，2026-09-20T20:36:08Z；不新增独占锁行，本段属上方 V0.01 报告的追加）

- 两席落齐后读 Grok V0.01（modules/M023-b5-r1-grok-raw.md 28046 bytes，SHA256 4e9a232dfd76f972324e08ce429c2bf285ad0ad1eb1cbd4e7abb46ff28d5b6cd）：结论 FAIL，GR-01「我的合同把后端真码 drafting 当成未知状态」。
- 席甲核源（只读）：`apps/web/src/admin/pages/MerchantZoneContractsPage.tsx:18-27` 字典首键 `draft`；`ContractService.java:206/291/330` 写 `"drafting"`；complex_test 实态 `chk_contracts_lifecycle_status` 闭集为 drafting、pending_confirmation、pending_effective、effective、renewing、expired、terminated、archived、amended（只读 SELECT pg_constraint；与 `V314__contract_scope_grant_and_amendment.sql:15-17` 一致），0 处 `draft`；`listContractsByMerchant` 无状态过滤。成立。席甲 B1 与 B5 的「合同八态字典与后端一致」是凭页面字典与枚举 L239 对照下的判断，没有去对后端 CHECK 闭集——这一步漏了。
- 同类复查（本次补做，只读）：账单 `chk_merchant_bill_status`（6 值）、退租结算 `chk_contract_settlements_status`（6 值含 voided）、结算触发（3 值）、工单状态/优先级/类型（7/4/4 值，经全局 `formatBusinessStatus` 全部有中文）均与页面字典逐个对上；只有合同字典有缺口。另：后端第 9 态 `amended`（V314：被变更子合同接替的原合同；`ContractService.java:1180` 真路径）在 apps/web 0 处中文名，三份 HTML 也未给出该态名称（枚举 L239 只列八态；业务地图 L1352 只描述「变更子合同」）——修后该态在本页显示「未知状态」，不泄英文；命名属业务用词，席甲不自拟，转 B2-r2 登记并请 Grok 给意见。
- 更正（§2 末「说明」句）：原文称修后「page reads」不再含 totalAmount，不准确——探针正则 `row\.\(totalAmount\|…\)` 会前缀命中 `row.totalAmountCent`，日志里 `3 row.totalAmount` 即来源于此。固定计数：MerchantScopePage.tsx 中 `row.totalAmountCent` 3 处、不带 Cent 的 `row.totalAmount` 0 处。结论（页面只读 totalAmountCent）不变，表述以本条为准。
- 席甲 B5-r1 票撤回为 **FAIL**；合取 FAIL。依据无正式 ID 且 001–006 已全否 → 按回退统一规则整批回 B2（B2-r2），席甲不直接修。
