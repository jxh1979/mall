CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M023 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按控制合同 §4「合取不变、推翻权不对等」处理）。2026-09-20T19:29:34Z
- 控制合同 V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 V0.11（实算见首行，与 sidecar/ledger 顶部逐字相同；LOCK-AMEND-COUNT 1）。
- 正本主工作区 HEAD `e456ba96`，origin/main `89686538`；6 文件含 2026-09-11 三个未合并本地提交（3051dce9 / c6e9af1d / d96942eb＝OPEN PR #444 分支 audit-m023-r1），对 origin/main 差 105+/126−；审计对象＝主工作区现状，PR #444 不视为已审 PASS。工作区 tree `98109681a5038fef051bdb33e09a30b5ef7360c8`。三份业务 HTML 只读。
- 模块 M023「商户档案、商户类型与商户侧门户／网页与验证／第1段」，6 个前端文件（计划有效内容行 991；现行物理行 970，§1 实算），映射 21 判据（BSM-001/003、GEN-001…018、PGM-001）。历史未审。
- 顺序：Grok 先派、本席独立取证并落盘本报告；本报告落盘前未读取 `M023-b1-grok-raw.md` 任何正文（该文件此刻存在=False，未读）。
- 只读承诺：未修改任何源码/测试/文档/HTML/锁；vitest 只读源码。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| apps/web/src/admin/pages/MerchantScopePage.tsx | 216 | 13116 | 086c74d98e46c8d56c611d4c83980a1f89313bfe5b7e7007a9f029c84f05696a | 全读 |
| apps/web/src/admin/pages/MerchantScopePage.test.tsx | 144 | 7068 | 0a12649e5377c583c705881841857a43d4bfe7eee06494ed6ea667a14cbeed18 | 全读 |
| apps/web/src/admin/pages/MerchantZoneContractsPage.tsx | 178 | 9638 | 7e7328744b216cbd0897fa8455e6ab5c7f46134d614dc4b4137ea327b1eeea2d | 全读 |
| apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx | 142 | 6805 | 8e7a60b67eb0711f5d0da46d492d2fc6a27db7ba7effa85909af4a37fa5ede8d | 全读 |
| apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx | 158 | 8157 | 28428b650f8b874818cdcaf4893601270af1840cf92ae31ceb8c3699c22887c1 | 全读 |
| apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx | 132 | 6199 | 0e4d440ed8047eb36092cb7e6f19895012103ed3df825781c9d47d0fbccfe5fa | 全读 |

只读依赖（追流不审）：`apps/web/src/admin/` 的 `adminRoutes.ts`、`AdminApp.tsx`（navigateTo）、`components/TraceLink.tsx`、`types.ts`、`customer/i18n/statusLabels.ts`；后端 `merchantbilling/MerchantBillRecord`+Controller/Service、`merchantservice/MerchantWorkOrderRecord`+Service、`merchantportal/*`（M020 V0.01）、V307 结算状态 CHECK；`tests/e2e/full-admin-remaining.spec.ts:917`；三 HTML 相关原句。

## 2. 可执行证据原文（/home/james/audit-tmp/m023_b1_run.log）

```
START=2026-09-20T19:27:37Z HEAD=e456ba96 origin/main=89686538
== 0. six files (lines/bytes/sha256)
apps/web/src/admin/pages/MerchantScopePage.tsx 216 13116 086c74d98e46c8d56c611d4c83980a1f89313bfe5b7e7007a9f029c84f05696a
apps/web/src/admin/pages/MerchantScopePage.test.tsx 144 7068 0a12649e5377c583c705881841857a43d4bfe7eee06494ed6ea667a14cbeed18
apps/web/src/admin/pages/MerchantZoneContractsPage.tsx 178 9638 7e7328744b216cbd0897fa8455e6ab5c7f46134d614dc4b4137ea327b1eeea2d
apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx 142 6805 8e7a60b67eb0711f5d0da46d492d2fc6a27db7ba7effa85909af4a37fa5ede8d
apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx 158 8157 28428b650f8b874818cdcaf4893601270af1840cf92ae31ceb8c3699c22887c1
apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx 132 6199 0e4d440ed8047eb36092cb7e6f19895012103ed3df825781c9d47d0fbccfe5fa
== 1. vs HEAD (expect empty) / vs origin/main / local unmerged commits / open PR
 6 files changed, 105 insertions(+), 126 deletions(-)
d96942eb audit M023: settlement page copy does not say submit dispute
c6e9af1d audit M023: drop settlement dispute form; bill amounts in yuan
3051dce9 audit M023: merchant portal labels, notes, and no cross-merchant dump
open PR head audit-m023-r1: 444
== 2. unit tests actually run (vitest, three files)
VITEST_EXIT=0
 ✓ src/admin/pages/MerchantZoneSettlementsPage.test.tsx (6 tests) 84ms
 ✓ src/admin/pages/MerchantZoneContractsPage.test.tsx (7 tests) 99ms
 ✓ src/admin/pages/MerchantScopePage.test.tsx (9 tests) 115ms
 Test Files  3 passed (3)
== 3. scope page fields vs backend records
page reads:  4 row.auditEvidenceRef; 5 row.authorizedScope; 3 row.billNo; 18 row.merchantId; 2 row.priority; 4 row.status; 5 row.title; 3 row.totalAmount; 2 row.workOrderType;
MerchantBillRecord fields: id merchantId arApItemId billNo billPeriodStart billPeriodEnd dueDate totalAmountCent currency status agingDays sourceType sourceId lines createdAt updatedAt 
MerchantWorkOrderRecord has authorizedScope/auditEvidenceRef: 0; backend main total: 0
bills endpoint returns: 35:    public ApiResponse<PageResponse<MerchantBillRecord>> listBills() {
test fixtures fabricate: 9 10 13 14 
== 4. fake audited action / nonexistent events and permission
15:  evidence: "接口证据：读取 /api/v1/merchant-service/work-orders、/api/v1/merchant-billing/bills；权限由 merchant.portal.read、merchant.service_work_order
114:      status: "merchant_scope_confirmed",
115:      audit: "merchant_authorized_scope_checked",
120:      evidenceAttachmentIds: ["att-merchant-scope-confirm"],
184:      <AuditTrailHint event="merchant.scope.viewed / merchant.scope_confirmed" />
backend emits merchant.scope* events: 0; permission merchant.portal.read in migrations: 0; confirmScope calls api: 0
real list events: backend/src/main/java/com/complex/platform/merchantservice/MerchantServiceService.java:80 backend/src/main/java/com/complex/platform/merchantbilling/MerchantBillingService.java:107 
route ADM-MER-002: 48:  { pageId: "ADM-MER-002", label: "商户授权工作台", module: "商户门户", group: "governance", route: "/admin/merchant-portal/scope", requiredPermissions: ["merchant.service_work_order.view", "merchant.billing.view"] }
== 5. english leak paths
settlement STATUS_LABELS keys: pending_settlement deposit_offsetting arrears_collecting settled disputed disputed_litigation 
backend settlement statuses (V307 CHECK): IN ('pending_settlement', 'deposit_offsetting', 'arrears_collecting', 'settled', 'disputed', 'voided')
raw-code fallbacks: apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx:99 apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx:100 apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx:120 apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx:121 apps/web/src/admin/pages/MerchantZoneContractsPage.tsx:69 apps/web/src/admin/pages/MerchantZoneContractsPage.tsx:137 
formatBusinessStatus fallback: 259:  return STATUS_LABELS[code] ?? code;
viewed=0 issued=1 confirmed=1 disputed=1 paid=1 overdue=1 cancelled=1 service=1 critical=1 created=1 pending_merchant=1 resolved=1 assigned=1 processing=1 closed=1 
backend closed sets: bill statuses seen in service: "confirmed" "disputed" "issued" "overdue" "viewed" ; work order: Set.of("repair", "complaint", "rectification", "service") Set.of("low", "medium", "high", "critical") Set.of("assigned", "processing", "pending_merchant", "resolved") Set.of("closed", "cancelled") 
== 6. open KPI semantics
88:  const openRows = rows.filter((row) => row.status !== "settled");
== 7. drill-down targets reachable for the pages' audience?
17:  { pageId: "ADM-GOV-002", label: "组织与商户档案", module: "治理", group: "governance", route: "/admin/governance/organizations", requiredPermissions: ["merchant.profile.view", "governance.project.view"] },
19:  { pageId: "ADM-CONT-001", label: "合同生命周期工作台", module: "合同", group: "governance", route: "/admin/contracts", requiredPermissions: ["contract.draft.manage"] },
50:  { pageId: "ADM-GOV-007", label: "空间类型字典", module: "治理", group: "governance", route: "/admin/governance/space-types", requiredPermissions: ["governance.space.view"] },
91:  { pageId: "ADM-MER-005", label: "我的对账", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/settlements", requiredPermissions: ["merchant.portal.settlement.view"], accountForms: ["merchant"] },
92:  { pageId: "ADM-MER-006", label: "我的合同", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/contracts", requiredPermissions: ["contract.view"], accountForms: ["merchant"] },
navigateTo silently returns when not permitted:  function navigateTo(pageId: string, objectId?: string) {  const target = adminRoutes.find((item) => item.pageId === pageId);  if (!target || !hasPermissions(effectivePermissions, target.requiredPermissions)) {  return;  }  if (!routeAllowsAccountForm(target, 
trace links in pages:  4 apps/web/src/admin/pages/MerchantScopePage.tsx:pageId="ADM-GOV-002"; 4 apps/web/src/admin/pages/MerchantZoneContractsPage.tsx:pageId="ADM-CONT-001"; 2 apps/web/src/admin/pages/MerchantZoneContractsPage.tsx:pageId="ADM-GOV-007"; 2 apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx:pageId="ADM-CONT-001";
== 8. HTML anchor sentences (read-only)
商户专区自助：我的账单（该交多少、交了多少、还欠多少）；我的工单（报修/投诉/整改自己发起、进度自己看，超时自动升级催办）；我的对账（结算与分成明细，有疑问可留言提异议）；我的
商户对退租结算金额的异议在退租审批流程中提出并解决
待结算、押金抵扣中、欠费追缴中、已结清、有争议、「有争议，诉讼/仲裁」
e2e stale expectation: 917:    await expect(page.getByTestId("merchant-zone-settlement-dispute-form")).toBeVisible();
== 9. HTML + lock sha; tree
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
tree=98109681a5038fef051bdb33e09a30b5ef7360c8
END=2026-09-20T19:27:40Z
```

## 3. 三线审计方法与覆盖

- 对象口径：6 文件 970 行逐行读完、22 个单测逐个对照页面与后端真实现；21 判据逐项；页面每个后端字段/事件名/权限码/下钻目标按对象追现行代码（§2 探针 3–7）。
- 第一性原理线：上位是业务地图 L1322「商户专区自助：我的账单…我的工单…我的对账（结算与分成明细，有疑问可留言提异议）；我的合同（关键条款、租期、到期情况）」、L1848-1850 商户侧页面「不看平台全局」、枚举 L252「商户对退租结算金额的异议在退租审批流程中提出并解决」与退租结算六态、治理地图 L256 段（全链可点击、界面全中文、留痕）。三问：用途成立；实际行为——三页单测 22/22 绿（探针 2），但商户授权工作台读取的多个字段后端根本不返回（CX-001）、「确认授权范围」不调后端却宣称留痕（CX-002）、状态有英文外泄路径（CX-003）、「未结清」把已作废算进去（CX-004）、下钻目标对本页唯一受众不可达（CX-005）；未经验证前提——测试夹具以后端不存在的字段形状喂页面，绿不代表真实后端下可用。
- 顺从式线：21 判据逐项（§5）。对抗式线（grilling）：照后端真实返回渲染账单行会怎样（金额/标题/授权范围/留痕证据恒「-」，账单金额 KPI 恒 0.00 元）；点「确认授权范围」后端留下什么（无请求、无审计）；后端返回 voided / viewed 状态屏幕上显示什么（英文原码）；商户点合同/空间/商户下钻会怎样（`AdminApp.navigateTo:545-552` 无权限即静默 return，按钮点了没反应）；对账页没有异议表单是否违背 HTML（否——本页展示的是退租/到期结算单，枚举 L252 明文该类异议走退租审批；2026-09-11 提交 c6e9af1d 去掉表单与 HTML 一致，M020 OBS-5 转入项就此判「不成立」）。
- 本轮实跑三个测试文件；本席禁止修复。

## 4. 与现行实现 / 契约的分叉对照（要害）

| 页面声称 / 读取 | 现行事实（§2 探针） | 结论 |
|---|---|---|
| `MerchantScopePage.tsx:146/:172/:108` 读 `row.totalAmount`、`:145/:172` 账单 `row.title`、`:107/:133/:148/:160/:172` `authorizedScope`、`:134/:149` `auditEvidenceRef`；测试夹具 `total_amount/title/authorized_scope`（test:9-14） | `/api/v1/merchant-billing/bills` 返回 `MerchantBillRecord`：金额字段是 `totalAmountCent`（分），无 title、无 authorizedScope/auditEvidenceRef；`MerchantWorkOrderRecord` 同样无后两者（后端 main 0 处） | CX-001 |
| `MerchantScopePage.tsx:111-122` `confirmScope()` 写出 `status: merchant_scope_confirmed / audit: merchant_authorized_scope_checked / evidenceAttachmentIds: [att-merchant-scope-confirm]`；`:184` 留痕提示 `merchant.scope.viewed / merchant.scope_confirmed`；`:15` 「权限由 merchant.portal.read…控制」 | confirmScope 0 次 api 调用；后端 0 处 merchant.scope* 事件；迁移 0 处 `merchant.portal.read`；真实列表事件是 `merchant_service.work_order.viewed`、`merchant_billing.bill.viewed`；路由 ADM-MER-002 要求的是 work_order.view + billing.view | CX-002 |
| `MerchantZoneSettlementsPage.tsx:24-31` 结算状态字典；`:99/:100/:120/:121`、`MerchantZoneContractsPage.tsx:69/:137` `?? row.xxx` 原码回退；`MerchantScopePage.tsx` 经 `formatBusinessStatus`（未知码原样返回） | V307 CHECK 含 `voided`，字典无；账单状态 `viewed` 不在 statusLabels；未知码一律把英文原码显示给用户 | CX-003 |
| `MerchantZoneSettlementsPage.tsx:88` `openRows = status !== "settled"`，KPI「未结清」 | voided（已作废）被算作未结清 | CX-004 |
| 三页 TraceLink：对账/合同页 → ADM-CONT-001（需 contract.draft.manage）、ADM-GOV-007（空间类型字典，需 governance.space.view）；授权工作台 → ADM-GOV-002（需 merchant.profile.view + governance.project.view） | ADM-MER-005/006 为 `accountForms: ["merchant"]` 的商户专页；`navigateTo` 对无权限目标静默 return → 这些按钮对本页唯一受众点了没反应；且空间 id 指向的是「空间类型字典」页 | CX-005 |
| `MerchantZoneSettlementsPage.tsx:13` 「退租金额异议在退租审批中提出。本页只读…」、无异议表单（test:50-59） | 枚举 L252 明文同句；本页对象是退租/到期结算单 | 通过（M020 OBS-5 转入项不成立；e2e:917 仍找表单属 e2e 模块，OBS-2） |
| `MerchantZoneContractsPage.tsx:18-27` 合同八态字典、押金「元」、中文日期 | 与后端生命周期态一致（draft/pending_confirmation/pending_effective/effective/renewing/expired/terminated/archived）；测试锁中文与「元」 | 通过 |

## 5. 21 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| BSM-001 / BSM-003 | 通过（登记） | 三页均为 L1322 商户专区自助项的只读视图；不发明业务规则（OBS-1 功能面缺口登记） |
| PGM-001 全局原则 | 不通过 | CX-003（界面全中文）、CX-005（全链可点击：点了没反应）、CX-002（留痕声称为假） |
| GEN-001 必要性最小性 | 不通过（并入 CX-001/002） | 幽灵列、假按钮 |
| GEN-002 可执行正确性 | 不通过 | CX-001（金额恒「-」/KPI 恒 0）、CX-004 |
| GEN-003 测试有效性 | 不通过 | CX-001（夹具伪造后端形状得假绿）；CX-003/004 无 voided/viewed 用例；管理员用例未断言提示文案 |
| GEN-004 / GEN-006 / GEN-007 | 通过（登记） | 三页只读，无写入；确认按钮无请求（CX-002） |
| GEN-005 授权与最小权限 | 通过 | 授权工作台前端按 user.merchantId 再过滤、无商户号即拒；专区两页 accountForms merchant |
| GEN-008 接口与模式一致性 | 不通过 | CX-001（字段名/单位）、CX-002（事件名/权限码） |
| GEN-009 错误处理与恢复 | 通过 | useAdminResource + AdminDataState 重试；scopeMissing 中文提示 |
| GEN-010 安全与敏感信息 | 通过 | 无敏感字段外泄；前端过滤为纵深防御 |
| GEN-011 可观测与审计证据 | 不通过 | CX-002 |
| GEN-012 资源与性能边界 | 通过 | 前端 10 行分页；后端默认页 500 |
| GEN-013 / GEN-014 | 无本面 | 无构建/迁移 |
| GEN-015 界面可用性 | 不通过 | CX-005（死按钮）、CX-003 |
| GEN-016 文档可验证性 | 不通过 | CX-002（页面文案的权限/事件声称不可复现） |
| GEN-017 业务来源分类 | 通过 | 五条均细化（L1322/L1850/L256/L252 下位实现），无添加项 |
| GEN-018 第一性原理证据 | 通过 | §2 命令原文（vitest 实跑、grep 后端记录/事件/迁移/路由）+ 源码原文 + HTML 逐字 |

## 6. 本次 Findings（临时 ID CX-M023-00N；正式 ID 由 B2 分配）

### CX-M023-001 · 细化 · 商户授权工作台读取后端不存在的字段：账单金额恒「-」、KPI 恒 0，测试夹具伪造后端形状

- 位置：`MerchantScopePage.tsx:107-108`（授权范围/账单金额汇总）、`:133-134`、`:145-149`、`:159-160`、`:171-172`（表头与单元格）、`:197`、`:200`；`MerchantScopePage.test.tsx:8-15`（夹具）、`:141`（断言「100.00 元」）。
- 事实：§4 第 1 行。真实后端下：账单「金额」「标题」列恒「-」，「账单金额」KPI 恒「0.00 元」，两表「授权范围」「留痕证据」列恒「-」，「授权范围」KPI 恒 0；单测因夹具自带 `total_amount/title/authorized_scope` 而绿。
- 依据类型：2（后端记录/Controller 原文）+ 4（探针 3）。HTML 上位：L1322「我的账单（该交多少、交了多少、还欠多少）」。判据：GEN-002 / GEN-008 / GEN-003 / GEN-001。四分类：细化。工程缺陷：是。入队：是。
- 最小拟处置：金额读 `totalAmountCent`（分→元）；删后端不存在的列与 KPI（账单「标题」、两表「授权范围」「留痕证据」、「授权范围」KPI），账单表按真实字段给「账期/到期日」不属本条（不加）；夹具改为后端真实形状（`total_amount_cent` 等），断言金额与 KPI 合计。行类型若在 `types.ts`（别模块单元），优先在页内局部类型收口。

### CX-M023-002 · 细化 · 「确认授权范围」不调后端却宣称留痕；页面文案的事件名与权限码不存在

- 位置：`MerchantScopePage.tsx:111-122`、`:203-206`（按钮）、`:184`（AuditTrailHint）、`:15`（TEXT.evidence）、`:20`。
- 事实：§4 第 2 行。按钮只 `setActionResult` 一个伪造结果（含虚构附件号），用户以为完成了留痕动作，后端无任何记录。
- 依据类型：4（探针 4）+ 2。HTML：治理地图 L256 段留痕与可溯源。判据：GEN-011 / GEN-016 / GEN-002 / GEN-008 / PGM-001。四分类：细化。工程缺陷：是。入队：是。
- 最小拟处置：删除确认按钮、confirmScope 与 TEXT.confirm；AuditTrailHint 改为真实事件 `merchant_service.work_order.viewed / merchant_billing.bill.viewed`；TEXT.evidence 的权限改为路由真实要求的两码；测试同步。

### CX-M023-003 · 细化 · 状态英文外泄：结算字典缺 voided、未知码回退原文、账单 viewed 无中文

- 位置：`MerchantZoneSettlementsPage.tsx:24-31`、`:99-100`、`:120-121`；`MerchantZoneContractsPage.tsx:69`、`:137`；`MerchantScopePage.tsx:129-132`、`:147`、`:160`、`:172`（formatBusinessStatus）。
- 事实：§4 第 3 行；V307 起后端会返回 voided；账单状态 viewed 属后端闭集而 statusLabels 无；回退一律输出英文码。
- 依据类型：2 + 4（探针 5）。HTML：治理地图 L287「状态、枚举取值…必须全部为中文」。判据：PGM-001 / GEN-015 / GEN-003。四分类：细化。工程缺陷：是。入队：是。
- 最小拟处置：结算字典补 `voided: "已作废"`；三页未知码回退改中文占位（不回显英文原码）；授权工作台对账单状态用页内字典覆盖后端闭集（含 viewed）；测试补 voided/viewed 与未知码用例。

### CX-M023-004 · 细化 · 「未结清」把已作废结算单算进去

- 位置：`MerchantZoneSettlementsPage.tsx:88-90`、`:149`。
- 事实：§4 第 4 行。已作废不是待结清的钱，KPI 与聚焦子集口径错。
- 判据：GEN-002 / GEN-003。四分类：细化。工程缺陷：是。入队：是。
- 最小拟处置：未结清 = 状态不在 (settled, voided)；测试补 voided 行。

### CX-M023-005 · 细化 · 下钻按钮对本页唯一受众不可达（点了没反应），空间 id 还指向「空间类型字典」

- 位置：`MerchantZoneSettlementsPage.tsx:98`、`:119`；`MerchantZoneContractsPage.tsx:61`、`:66`、`:74`、`:131`、`:135`、`:141`；`MerchantScopePage.tsx:128`、`:143`、`:160`、`:172`；三测试文件中对 `trace-link-ADM-CONT-001-*` 的断言。
- 事实：§4 第 5 行。ADM-MER-005/006 只对商户账号开放；目标页 ADM-CONT-001 / ADM-GOV-007 / ADM-GOV-002 需要平台权限，`navigateTo` 静默 return。渲染成按钮却点了没反应，违背「见名可点」的本意；单测用 `navigateTo: vi.fn()` 直接断言被调用，测不到权限拦截。
- 依据类型：2（AdminApp/adminRoutes 原文）+ 4（探针 7）。HTML：治理地图 L256 段「全链可点击」。判据：PGM-001 / GEN-015 / GEN-002 / GEN-003。四分类：细化。工程缺陷：是。入队：是。
- 最小拟处置：对账页合同下钻改指商户可达的「我的合同」ADM-MER-006；合同页自身合同号、续签自、空间，授权工作台商户号——无商户可达目标页的降为纯文本（TraceLink 不传 navigateTo 即纯文本）；测试同步。不改 AdminApp/TraceLink/路由（别模块）。

## 7. 观察（登记、不入队）

- OBS-1（功能面缺口，报操作员）：HTML L1322「我的对账（结算与分成明细，有疑问可留言提异议）」；现行「我的对账」页只展示退租/到期结算单（3.18 方案 A 一期），无月度结算与分成明细、无留言入口；属后端/产品范围，不在本 6 文件内可闭合。
- OBS-2（XM → e2e 模块）：`tests/e2e/full-admin-remaining.spec.ts:917` 仍期待异议表单可见，与现行页面及枚举 L252 不符。
- OBS-3（XM → M020 回看）：M020 保留的 `POST /merchant-portal/settlements/{id}/dispute` 无任何前端调用方；枚举 L252 明文退租结算金额异议走退租审批流程——该端点的必要性由操作员/后续模块裁定。
- OBS-4（XM → 路由模块）：ADM-MER-002 在 governance 组且无 accountForms，平台管理员可进但页面对其只显示「缺少商户授权范围」；`/merchant-billing/bills` 的 `scope.isPresent() ? … : findAllBills()` 与 M020-001 同一错误前提（merchantbilling 模块）。
- OBS-5：PR #444 三个本地提交（标签中文化、去异议表单、不给平台管理员全量）方向与 HTML 一致，随本模块检查点整体带入。
- 无添加项；无 HTML 修改申请；无 questions。

## 8. 结论

本席 B1 票：**FAIL**（CX-M023-001…005 为工程缺陷且建议入队；写入集合为本模块 6 文件）。6 文件全文三线审完、21 判据逐项核对、vitest 实跑与 grep 后端记录/事件/路由为证据；未修文、未改 HTML/锁/台账。本报告落盘前未读取 `M023-b1-grok-raw.md` 任何正文。


## M023 B2-r1 整批候选复核（席甲票）

- 时间：2026-09-20T19:41:24Z。输入：modules/M023-b1-grok-raw.md V0.01（33144 bytes，SHA256 9ed7728a96bf3d7b4a28622995d3c68e4107b6d2176ecd8e77db04bcde2f7404）、modules/M023-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M023 B2-r1 整批候选」段（全文件 SHA256 746ae14bad44b198bd97ced7f97f2944be1aeb013dc96ba2df1bba72c3d531c5）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：Grok GR-01…06 与席甲 CX-001…005 全部归并（见候选「来源归并」列）；观察全部登记；无漏并、无改写。
- 自审纠正：席甲 B1 §4 第 6 行与 §3 对抗线把「对账页无异议表单」判为与 HTML 一致、M020 OBS-5 转入项不成立——只看了枚举 L252，漏看 M020 闭环的上位依据正是业务地图 L1322「可留言提异议」，也漏看 L1322「结算…明细」要求的数字在页面上不可见。该判断撤回，采 Grok GR-04（冲突）。
- 006 处置同时满足两句：L1322→明细数字可见 + 专区内可留言；L252→文案写明正式异议走退租审批、留言不改状态不暂停付款（与 M020 note-only 端点一致）。不改后端、不发明状态或 KPI。
- 001–005 处置只改本模块 6 文件；不改 types.ts / AdminApp / TraceLink / 路由 / 后端；不给商户加平台权限。无业务发明。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。


## M023 B2-r2 整批候选复核（席甲票）

- 时间：2026-09-20T20:36:19Z。输入：modules/M023-b5-r1-grok-raw.md V0.01（28046 bytes，SHA256 4e9a232dfd76f972324e08ce429c2bf285ad0ad1eb1cbd4e7abb46ff28d5b6cd）、modules/M023-b5-r1-self-raw.md V0.01+V0.02（12857 bytes，SHA256 135bf4f27092a63b7f47dc63f1fbd48c30e49e9c3ff7bd4098c00ba09fe90788）；候选 findings.md 顶部「当前 M023 B2-r2 整批候选」段（全文件 SHA256 9701c3e3e43ac38a09d2b000f7f87c5b3cd9e69f978b24caf4edbf0b5441a575）。本票落盘前未读 Grok B2-r2 追加段。不写独占锁行。
- 差集：GR-01 ↔ M023-007 一一对应；席甲撤回段无第二条缺陷；Grok 其余观察登记不入队；无漏并、无改写。
- 007：分类细化（业务地图 L1322「我的合同」只读页的状态展示，属下位实现）；工程缺陷成立（页面键 `draft` vs 后端/库闭集 `drafting`，原文见候选行）；最小处置只改键名 + 夹具/断言 + 两文件版本标记，不改 types.ts／后端／全局字典；队列是。同意。
- 登记项 amended：后端真态、前端无名、三份 HTML 未命名；席甲不自拟业务用词，修后显示中文占位不泄英文；九态 vs 八态差异归合同状态机模块（M254/M041）并报操作员。席甲认为登记成立；若 Grok 指出 HTML 原文出处或主张 2.2 发问，以其意见为准再议。
- 001–006 不动、不回潮；无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r2 票：**PASS**（整批候选同意）。
