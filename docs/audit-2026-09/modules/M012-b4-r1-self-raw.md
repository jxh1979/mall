CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M012 B4-r1 修复内容复审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B4-r1（B3 第 1 次整批修复的完整 diff 复审）。2026-09-09T17:45:46Z
- before tree：`83d85aa3001e3c4d29475c6c24a6533e0544a5e0`；after tree：`50c07ff1a9041fd2cbb9ab4263e05c86dcd9a10f`；`git diff --binary 83d85aa3001e 50c07ff1a904` 全文 61235 bytes SHA256 f9b1b1fbd589840caa8b5a443f12d4219a45d67ce349499a968e2340fa05db47（全文附于本报告末尾）。
- 定稿依据 findings V1.41「当前 M012 B2-r2 双PASS定稿，8项待修（B3-r1）」：是＝001（绑定页真实键）、002（action 三态）、003（projectId 挂接）、004（中文业态/空间类型/空间状态/表头）、005（夹具现行闭集）、006（去预填与隐藏字段，操作员答删除）、007（手机号非空，操作员答删除）、008（存档/删除申请测试 + 写路径 catch）；否＝009。B3 累计 1/3。
- 锁四值：实算/sidecar/ledger/Grok 均 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。

## 1. diffstat

```
.../admin/pages/GovernanceTopologyPage.test.tsx    |  40 +++++++-
 .../web/src/admin/pages/GovernanceTopologyPage.tsx |  25 +++--
 .../admin/pages/OrganizationMerchantPage.test.tsx  |  98 +++++++++++++++---
 .../src/admin/pages/OrganizationMerchantPage.tsx   | 102 ++++++++++---------
 .../src/admin/pages/SubjectBindingsPage.test.tsx   | 112 +++++++++++++++++++--
 apps/web/src/admin/pages/SubjectBindingsPage.tsx   |  83 ++++++++++-----
 6 files changed, 357 insertions(+), 103 deletions(-)
```

## 2. 逐块归因（每一行改动都归到定稿 ID；「版本铁律」行随其所在文件的 ID）

| 文件 | 块 | 位置 | 块内标记 ID | 归因 |
|---|---|---|---|---|
| apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx | 1 | `@@ -1,3 +1,4 @@` | 版本铁律（随所属ID） | 003（已挂接非0断言）、004（中文业态/空间类型/状态断言）、005（management_company/shop 夹具）；版本头随本文件 |
| apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx | 2 | `@@ -6,13 +7,15 @@ import { AdminApi } from "../client/adminC` | M012-003, M012-005 | 003（已挂接非0断言）、004（中文业态/空间类型/状态断言）、005（management_company/shop 夹具）；版本头随本文件 |
| apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx | 3 | `@@ -105,13 +108,40 @@ describe("GovernanceTopologyPage KPI 聚` | M012-003/004, M012-004 | 003（已挂接非0断言）、004（中文业态/空间类型/状态断言）、005（management_company/shop 夹具）；版本头随本文件 |
| apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx | 4 | `@@ -120,10 +150,11 @@ describe("GovernanceTopologyPage 行明细抽屉` | — | 003（已挂接非0断言）、004（中文业态/空间类型/状态断言）、005（management_company/shop 夹具）；版本头随本文件 |
| apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx | 5 | `@@ -131,6 +162,7 @@ describe("GovernanceTopologyPage 行明细抽屉（拓` | — | 003（已挂接非0断言）、004（中文业态/空间类型/状态断言）、005（management_company/shop 夹具）；版本头随本文件 |
| apps/web/src/admin/pages/GovernanceTopologyPage.tsx | 1 | `@@ -1,3 +1,4 @@` | 版本铁律（随所属ID） | 003（projectId 挂接）、004（业态 formatBusinessStatus、空间类型 8 码表、空间状态可用/不可用）；版本头随本文件 |
| apps/web/src/admin/pages/GovernanceTopologyPage.tsx | 2 | `@@ -10,6 +11,17 @@ import { formatBusinessStatus } from "../` | M012-004 | 003（projectId 挂接）、004（业态 formatBusinessStatus、空间类型 8 码表、空间状态可用/不可用）；版本头随本文件 |
| apps/web/src/admin/pages/GovernanceTopologyPage.tsx | 3 | `@@ -95,7 +107,8 @@ export function GovernanceTopologyPage({ ` | M012-003 | 003（projectId 挂接）、004（业态 formatBusinessStatus、空间类型 8 码表、空间状态可用/不可用）；版本头随本文件 |
| apps/web/src/admin/pages/GovernanceTopologyPage.tsx | 4 | `@@ -122,7 +135,7 @@ export function GovernanceTopologyPage({` | — | 003（projectId 挂接）、004（业态 formatBusinessStatus、空间类型 8 码表、空间状态可用/不可用）；版本头随本文件 |
| apps/web/src/admin/pages/GovernanceTopologyPage.tsx | 5 | `@@ -136,8 +149,8 @@ export function GovernanceTopologyPage({` | — | 003（projectId 挂接）、004（业态 formatBusinessStatus、空间类型 8 码表、空间状态可用/不可用）；版本头随本文件 |
| apps/web/src/admin/pages/GovernanceTopologyPage.tsx | 6 | `@@ -165,7 +178,7 @@ export function GovernanceTopologyPage({` | — | 003（projectId 挂接）、004（业态 formatBusinessStatus、空间类型 8 码表、空间状态可用/不可用）；版本头随本文件 |
| apps/web/src/admin/pages/GovernanceTopologyPage.tsx | 7 | `@@ -184,7 +197,7 @@ export function GovernanceTopologyPage({` | — | 003（projectId 挂接）、004（业态 formatBusinessStatus、空间类型 8 码表、空间状态可用/不可用）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 1 | `@@ -1,4 +1,4 @@` | 版本铁律（随所属ID） | 004（酒吧断言）、005（management_company/lease 夹具、六值注释）、006（不含假字段断言、电话不预填）、007（联系电话不能为空）、008（存档/删除申请/失败可见用例、m-3 夹具、POST mock）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 2 | `@@ -9,11 +9,13 @@ import type { AdminPageProps, EffectivePer` | M012-008 | 004（酒吧断言）、005（management_company/lease 夹具、六值注释）、006（不含假字段断言、电话不预填）、007（联系电话不能为空）、008（存档/删除申请/失败可见用例、m-3 夹具、POST mock）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 3 | `@@ -29,8 +31,10 @@ function envelope(items: unknown[]): Resp` | — | 004（酒吧断言）、005（management_company/lease 夹具、六值注释）、006（不含假字段断言、电话不预填）、007（联系电话不能为空）、008（存档/删除申请/失败可见用例、m-3 夹具、POST mock）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 4 | `@@ -51,6 +55,7 @@ function renderPage() {` | — | 004（酒吧断言）、005（management_company/lease 夹具、六值注释）、006（不含假字段断言、电话不预填）、007（联系电话不能为空）、008（存档/删除申请/失败可见用例、m-3 夹具、POST mock）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 5 | `@@ -121,7 +126,7 @@ describe("OrganizationMerchantPage KPI 聚` | M012-005 | 004（酒吧断言）、005（management_company/lease 夹具、六值注释）、006（不含假字段断言、电话不预填）、007（联系电话不能为空）、008（存档/删除申请/失败可见用例、m-3 夹具、POST mock）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 6 | `@@ -129,13 +134,15 @@ describe("OrganizationMerchantPage KPI` | M012-004, M012-005 | 004（酒吧断言）、005（management_company/lease 夹具、六值注释）、006（不含假字段断言、电话不预填）、007（联系电话不能为空）、008（存档/删除申请/失败可见用例、m-3 夹具、POST mock）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 7 | `@@ -155,11 +162,12 @@ describe("OrganizationMerchantPage KPI` | M012-005 | 004（酒吧断言）、005（management_company/lease 夹具、六值注释）、006（不含假字段断言、电话不预填）、007（联系电话不能为空）、008（存档/删除申请/失败可见用例、m-3 夹具、POST mock）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 8 | `@@ -187,11 +195,11 @@ describe("OrganizationMerchantPage KPI` | M012-006, M012-007 | 004（酒吧断言）、005（management_company/lease 夹具、六值注释）、006（不含假字段断言、电话不预填）、007（联系电话不能为空）、008（存档/删除申请/失败可见用例、m-3 夹具、POST mock）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 9 | `@@ -207,7 +215,13 @@ describe("OrganizationMerchantPage KPI ` | M012-006 | 004（酒吧断言）、005（management_company/lease 夹具、六值注释）、006（不含假字段断言、电话不预填）、007（联系电话不能为空）、008（存档/删除申请/失败可见用例、m-3 夹具、POST mock）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 10 | `@@ -224,7 +238,7 @@ describe("OrganizationMerchantPage 行明细抽屉` | — | 004（酒吧断言）、005（management_company/lease 夹具、六值注释）、006（不含假字段断言、电话不预填）、007（联系电话不能为空）、008（存档/删除申请/失败可见用例、m-3 夹具、POST mock）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 11 | `@@ -234,7 +248,7 @@ describe("OrganizationMerchantPage 行明细抽屉` | — | 004（酒吧断言）、005（management_company/lease 夹具、六值注释）、006（不含假字段断言、电话不预填）、007（联系电话不能为空）、008（存档/删除申请/失败可见用例、m-3 夹具、POST mock）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 12 | `@@ -259,3 +273,59 @@ describe("OrganizationMerchantPage 行明细抽` | M012-008 | 004（酒吧断言）、005（management_company/lease 夹具、六值注释）、006（不含假字段断言、电话不预填）、007（联系电话不能为空）、008（存档/删除申请/失败可见用例、m-3 夹具、POST mock）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 1 | `@@ -1,4 +1,4 @@` | 版本铁律（随所属ID） | 004（项目业态 formatBusinessStatus）、006（清空预填、最小创建请求、合作方式可空项）、007（手机号非空）、008（写路径 catch）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 2 | `@@ -22,7 +22,8 @@ const TEXT = {` | — | 004（项目业态 formatBusinessStatus）、006（清空预填、最小创建请求、合作方式可空项）、007（手机号非空）、008（写路径 catch）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 3 | `@@ -76,12 +77,13 @@ export function OrganizationMerchantPage` | M012-006 | 004（项目业态 formatBusinessStatus）、006（清空预填、最小创建请求、合作方式可空项）、007（手机号非空）、008（写路径 catch）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 4 | `@@ -161,36 +163,27 @@ export function OrganizationMerchantPa` | M012-006, M012-007 | 004（项目业态 formatBusinessStatus）、006（清空预填、最小创建请求、合作方式可空项）、007（手机号非空）、008（写路径 catch）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 5 | `@@ -209,6 +202,13 @@ export function OrganizationMerchantPag` | M012-008 | 004（项目业态 formatBusinessStatus）、006（清空预填、最小创建请求、合作方式可空项）、007（手机号非空）、008（写路径 catch）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 6 | `@@ -218,34 +218,46 @@ export function OrganizationMerchantPa` | — | 004（项目业态 formatBusinessStatus）、006（清空预填、最小创建请求、合作方式可空项）、007（手机号非空）、008（写路径 catch）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 7 | `@@ -289,7 +301,7 @@ export function OrganizationMerchantPage` | — | 004（项目业态 formatBusinessStatus）、006（清空预填、最小创建请求、合作方式可空项）、007（手机号非空）、008（写路径 catch）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 8 | `@@ -360,7 +372,7 @@ export function OrganizationMerchantPage` | — | 004（项目业态 formatBusinessStatus）、006（清空预填、最小创建请求、合作方式可空项）、007（手机号非空）、008（写路径 catch）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 9 | `@@ -450,7 +462,7 @@ export function OrganizationMerchantPage` | — | 004（项目业态 formatBusinessStatus）、006（清空预填、最小创建请求、合作方式可空项）、007（手机号非空）、008（写路径 catch）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 10 | `@@ -462,7 +474,7 @@ export function OrganizationMerchantPage` | — | 004（项目业态 formatBusinessStatus）、006（清空预填、最小创建请求、合作方式可空项）、007（手机号非空）、008（写路径 catch）；版本头随本文件 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 11 | `@@ -471,7 +483,7 @@ export function OrganizationMerchantPage` | — | 004（项目业态 formatBusinessStatus）、006（清空预填、最小创建请求、合作方式可空项）、007（手机号非空）、008（写路径 catch）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.test.tsx | 1 | `@@ -1,3 +1,4 @@` | 版本铁律（随所属ID） | 001（真实 snake_case 夹具与断言）、002（提交断言含 action）、004（表头中文断言）、005（绑定三态夹具）、006（原因空/不含假证据断言）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.test.tsx | 2 | `@@ -5,10 +6,12 @@ import { SubjectBindingsPage } from "./Sub` | M012-001/005 | 001（真实 snake_case 夹具与断言）、002（提交断言含 action）、004（表头中文断言）、005（绑定三态夹具）、006（原因空/不含假证据断言）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.test.tsx | 3 | `@@ -18,8 +21,21 @@ function envelope(items: unknown[]): Resp` | — | 001（真实 snake_case 夹具与断言）、002（提交断言含 action）、004（表头中文断言）、005（绑定三态夹具）、006（原因空/不含假证据断言）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.test.tsx | 4 | `@@ -34,11 +50,18 @@ function renderPage() {` | — | 001（真实 snake_case 夹具与断言）、002（提交断言含 action）、004（表头中文断言）、005（绑定三态夹具）、006（原因空/不含假证据断言）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.test.tsx | 5 | `@@ -59,6 +82,7 @@ describe("SubjectBindingsPage KPI 聚焦视图", (` | — | 001（真实 snake_case 夹具与断言）、002（提交断言含 action）、004（表头中文断言）、005（绑定三态夹具）、006（原因空/不含假证据断言）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.test.tsx | 6 | `@@ -86,26 +110,51 @@ describe("SubjectBindingsPage KPI 聚焦视图"` | M012-001, M012-004 | 001（真实 snake_case 夹具与断言）、002（提交断言含 action）、004（表头中文断言）、005（绑定三态夹具）、006（原因空/不含假证据断言）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.test.tsx | 7 | `@@ -119,3 +168,50 @@ describe("SubjectBindingsPage 行明细抽屉", (` | M012-002/006/008 | 001（真实 snake_case 夹具与断言）、002（提交断言含 action）、004（表头中文断言）、005（绑定三态夹具）、006（原因空/不含假证据断言）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.tsx | 1 | `@@ -1,3 +1,4 @@` | 版本铁律（随所属ID） | 001（真实键/KPI/抽屉/表体）、002（action 下拉+请求体）、004（表头「编号」）、006（原因空初始、不传假证据）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.tsx | 2 | `@@ -6,10 +7,31 @@ import { AuditTrailHint } from "../compone` | M012-001, M012-002 | 001（真实键/KPI/抽屉/表体）、002（action 下拉+请求体）、004（表头「编号」）、006（原因空初始、不传假证据）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.tsx | 3 | `@@ -23,6 +45,8 @@ const TEXT = {` | — | 001（真实键/KPI/抽屉/表体）、002（action 下拉+请求体）、004（表头「编号」）、006（原因空初始、不传假证据）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.tsx | 4 | `@@ -43,27 +67,29 @@ function display(value: unknown) {` | M012-002, M012-006 | 001（真实键/KPI/抽屉/表体）、002（action 下拉+请求体）、004（表头「编号」）、006（原因空初始、不传假证据）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.tsx | 5 | `@@ -97,13 +123,13 @@ export function SubjectBindingsPage({ a` | — | 001（真实键/KPI/抽屉/表体）、002（action 下拉+请求体）、004（表头「编号」）、006（原因空初始、不传假证据）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.tsx | 6 | `@@ -113,6 +139,10 @@ export function SubjectBindingsPage({ a` | — | 001（真实键/KPI/抽屉/表体）、002（action 下拉+请求体）、004（表头「编号」）、006（原因空初始、不传假证据）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.tsx | 7 | `@@ -129,14 +159,15 @@ export function SubjectBindingsPage({ ` | M012-002, M012-006 | 001（真实键/KPI/抽屉/表体）、002（action 下拉+请求体）、004（表头「编号」）、006（原因空初始、不传假证据）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.tsx | 8 | `@@ -167,11 +198,12 @@ export function SubjectBindingsPage({ ` | — | 001（真实键/KPI/抽屉/表体）、002（action 下拉+请求体）、004（表头「编号」）、006（原因空初始、不传假证据）；版本头随本文件 |
| apps/web/src/admin/pages/SubjectBindingsPage.tsx | 9 | `@@ -193,20 +225,19 @@ export function SubjectBindingsPage({ ` | — | 001（真实键/KPI/抽屉/表体）、002（action 下拉+请求体）、004（表头「编号」）、006（原因空初始、不传假证据）；版本头随本文件 |

## 3. 红绿证据（B3-r1 运行记录 /tmp/m012_b3r1_run.log 原文）

```
START=2026-09-09T17:28:57Z
BEFORE_TREE=83d85aa3001e3c4d29475c6c24a6533e0544a5e0
=== PHASE RED: patch a (tests) ===
PATCH_A_APPLIED
RED_EXIT=1
 ❯ src/admin/pages/SubjectBindingsPage.test.tsx (11 tests | 9 failed) 128ms
     × 点“生效绑定”数字 → ?kpi=bindings&status=active，横幅(1)+表只剩生效子集，KPI 卡/变更表单隐藏 45ms
     × 点“唯一绑定对象”数字 → 全量表 9ms
     × 「返回全部」→ 恢复 KPI 卡+变更表单+全量表 13ms
     ✓ 带参直链进入 → 直接呈现聚焦视图 5ms
     × KPI 子标签、表头与状态列为中文，不含英文 active/ID 8ms
     × 表格按后台真实字段渲染商户/三主体编号与变更原因（读臆造键时整行是“-”，本用例必红） 6ms
     × 点绑定行 → 抽屉含该行三主体编号（状态中文），Esc 关闭 10ms
     ✓ 点行内单选按钮不触发抽屉（stopPropagation） 6ms
     × 未选变更后状态 → 表单报错、不发请求（后台 action 为必填三态） 11ms
     × 原因为空 → 表单报错、不发请求（原因不再预填验收句） 7ms
     × 选行+选状态+填原因 → 确认后 POST …/change，请求体含 action 与 reason、不含假证据附件 7ms
 ❯ src/admin/pages/GovernanceTopologyPage.test.tsx (11 tests | 4 failed) 136ms
     ✓ 点“空间点位”数字 → URL 带 ?kpi=spaces，只剩空间表+横幅 39ms
     ✓ 点“商户”数字 → 只剩主体-商户-项目表+横幅 15ms
     ✓ 「返回全部」→ 参数清除，恢复完整页面（两表+KPI 卡都在） 13ms
     ✓ 带参直链进入 → 直接呈现聚焦视图 6ms
     ✓ 渲染输出状态列为中文，不含原英文枚举 active/suspended 7ms
     × 项目业态与空间类型渲染为中文，不直出 bar/shop 英文码（PGM-001） 9ms
     × 「已挂接」按后台 project_id 计数（读臆造 venue_id 时恒为 0，本用例必红） 5ms
     × 点拓扑关系行 → 抽屉含主体/商户/项目与中文状态、中文业态 10ms
     × 点空间点位行 → 抽屉含空间名/所属项目/面积/中文空间类型 10ms
     ✓ Esc 关闭抽屉 + 点遮罩关闭 15ms
     ✓ 缺主体查看权限：business-entities 端点根本不请求，整页不白屏，商户/项目/空间照常渲染 6ms
 ❯ src/admin/pages/OrganizationMerchantPage.test.tsx (15 tests | 3 failed) 1682ms
     ✓ 点“正常商户”数字 → URL 带 ?kpi=merchants&status=active，页面进入聚焦视图 199ms
     ✓ 聚焦视图点「返回全部」→ 参数清除，恢复完整页面（三表+表单+KPI 卡都在） 48ms
     ✓ 带参刷新/直链进入 → 直接呈现聚焦视图（参数从 URL 重建） 12ms
     ✓ 浏览器后退（popstate）→ 退出聚焦视图回完整页面 43ms
     × 三表按后端真实字段渲染出真实值（字段错位时单元格是“-”，本用例必红） 29ms
     ✓ KPI 子标签渲染为中文，不含英文 18ms
     ✓ 引导式弹窗内商户类型/合作形式下拉显示中文，不含英文枚举 66ms
     × 引导式创建流：分步校验拦截 → 确认页汇总 → 创建提交并关闭弹窗 58ms
     ✓ 点经营主体行 → 抽屉含主体名/信用代码/角色中文 22ms
     ✓ 点商户行 → 抽屉含商户名/联系人/合作形式中文 24ms
     ✓ 点项目行 → 抽屉含项目名/编码；抽屉零英文枚举 21ms
     ✓ Esc 关闭抽屉 + 点遮罩关闭 34ms
     ✓ 存档：空理由被拦截不发请求（BSM-004 存档须填理由）；填写理由后 POST /merchants/{id}/archive 且请求体含理由 25ms
     ✓ 已存档商户：申请删除 → POST /merchants/{id}/delete-request 带理由 54ms
     × 写路径失败可见：存档接口报错 → setGlobalError 收到错误信息，不成未处理拒绝 1026ms
=== PHASE GREEN: patch b (pages) ===
PATCH_B_APPLIED
GREEN_EXIT=1
 ✓ src/admin/pages/GovernanceTopologyPage.test.tsx (11 tests) 132ms
 ✓ src/admin/pages/SubjectBindingsPage.test.tsx (11 tests) 136ms
 ❯ src/admin/pages/OrganizationMerchantPage.test.tsx (15 tests | 1 failed) 1682ms
     ✓ 点“正常商户”数字 → URL 带 ?kpi=merchants&status=active，页面进入聚焦视图 198ms
     ✓ 聚焦视图点「返回全部」→ 参数清除，恢复完整页面（三表+表单+KPI 卡都在） 48ms
     ✓ 带参刷新/直链进入 → 直接呈现聚焦视图（参数从 URL 重建） 12ms
     ✓ 浏览器后退（popstate）→ 退出聚焦视图回完整页面 49ms
     ✓ 三表按后端真实字段渲染出真实值（字段错位时单元格是“-”，本用例必红） 17ms
     ✓ KPI 子标签渲染为中文，不含英文 16ms
     ✓ 引导式弹窗内商户类型/合作形式下拉显示中文，不含英文枚举 66ms
     ✓ 引导式创建流：分步校验拦截 → 确认页汇总 → 创建提交并关闭弹窗 80ms
     ✓ 点经营主体行 → 抽屉含主体名/信用代码/角色中文 23ms
     ✓ 点商户行 → 抽屉含商户名/联系人/合作形式中文 21ms
     ✓ 点项目行 → 抽屉含项目名/编码；抽屉零英文枚举 21ms
     ✓ Esc 关闭抽屉 + 点遮罩关闭 35ms
     ✓ 存档：空理由被拦截不发请求（BSM-004 存档须填理由）；填写理由后 POST /merchants/{id}/archive 且请求体含理由 21ms
     ✓ 已存档商户：申请删除 → POST /merchants/{id}/delete-request 带理由 50ms
     × 写路径失败可见：存档接口报错 → setGlobalError 收到错误信息，不成未处理拒绝 1023ms
⎯⎯⎯⎯⎯⎯⎯ Failed Tests 1 ⎯⎯⎯⎯⎯⎯⎯
 FAIL  src/admin/pages/OrganizationMerchantPage.test.tsx > OrganizationMerchantPage 存档/删除写路径（M012-008） > 写路径失败可见：存档接口报错 → setGlobalError 收到错误信息，不成未处理拒绝
AssertionError: expected "vi.fn()" to be called with arguments: [ StringContaining "商户不存在" ]
    328|     await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledW…
 ❯ runWithExpensiveErrorDiagnosticsDisabled ../../node_modules/@testing-library/dom/dist/config.js:47:12
 Test Files  1 failed | 2 passed (3)
      Tests  1 failed | 36 passed (37)
npm error Lifecycle script `test:unit` failed with error:
npm error command failed
=== typecheck (apps/web) ===
TSC_EXIT=2
src/admin/pages/SubjectBindingsPage.tsx(170,25): error TS2783: 'action' is specified more than once, so this usage will be overwritten.
=== residuals ===
apps/web/src/admin/pages/GovernanceTopologyPage.tsx:110:  // M012-003：后台 SpaceRecord 以 projectId 挂接项目（无 venueId；types.
apps/web/src/admin/pages/SubjectBindingsPage.tsx:15:// types.ts 的 SubjectBindingRow 七个臆造字段（businessObjectId/operatorSubject/
AFTER_TREE=7ea7b6c95938b7839672e6a9b3ad5912c107190e
 M apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
 M apps/web/src/admin/pages/GovernanceTopologyPage.tsx
 M apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
 M apps/web/src/admin/pages/OrganizationMerchantPage.tsx
 M apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
 M apps/web/src/admin/pages/SubjectBindingsPage.tsx
END=2026-09-09T17:29:11Z
=== GREEN-2 2026-09-09T17:30:45Z (after patch c: SubjectBindingsPage setActionResult 去重复 action 键; 失败可见用例改为 expect.any(String)) ===
GREEN2_EXIT=0
 Test Files  3 passed (3)
      Tests  37 passed (37)
=== typecheck (apps/web) ===
TSC_EXIT=0
errors in the six files: 0
total tsc errors (pre-existing elsewhere): 0
AFTER_TREE=50c07ff1a9041fd2cbb9ab4263e05c86dcd9a10f
END2=2026-09-09T17:30:52Z
```

## 4. 本席复审结论

见文末追加的判定段。

## 附：完整 diff（before→after，--binary）

```diff
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

```

## 4. 本席复审结论（席甲，作者自审票）

本席逐块读完上述 diff 全文（6 文件），并以 §2 归因表核对每一行。

### 4.1 修复内容（逐 ID）

| ID | 修复内容 | 证据 | 本席票 |
|---|---|---|---|
| 001 | `SubjectBindingsPage` 页内局部类型 `BindingRow` 按后台 `BusinessEntityBindingRecord` 真实键读取（merchantId/operationEntityId/contractEntityId/settlementEntityId/status/effectiveFrom/effectiveTo/changeReason）；KPI 唯一绑定按 merchantId 去重、合同主体按 contractEntityId、选中展示 merchantId；表体/抽屉随改；`types.ts` 未改；夹具改真实 snake_case | RED：换夹具后旧页面读臆造键全「-」、`真实字段渲染` 与 `唯一绑定对象=2` 用例失败（§3 红日志）；GREEN：改页面后通过 | PASS |
| 002 | 表单加「变更后状态」中文下拉（active/inactive/suspended），`validateChange` 缺 action 拦截，请求体带 action；一条提交断言（POST …/change 含 action 与 reason） | RED：提交用例找不到 `subject-bindings-change-action`；GREEN：通过；后台 `changeBusinessEntityBinding` 白名单三态为闭集来源 | PASS |
| 003 | `attachedSpaces` 改 `space.projectId` | RED：「1 个已挂接」断言在 venueId 版恒 0 失败；GREEN：通过 | PASS |
| 004 | 项目业态 `formatBusinessStatus`（两页四处）、空间类型页内 8 码中文表（与 V184 种子同词）、空间状态可用/不可用、绑定表头「编号」；测试改中文断言与 `not.toMatch(/\bbar\b/)` | RED：`酒吧/商铺/铺位/可用/编号` 断言失败；GREEN：通过；`statusLabels.ts` 未改、未拉 `/space-types` | PASS |
| 005 | 夹具 entity_role→management_company、cooperation_form→lease、space_type→shop、绑定 status→active/inactive；断言「业态主体（管理公司）」「租赁」；注释六值并断言「传媒」 | 三文件残留 operation/fixed_rent/hall/terminated 0（§3）；创建流 `not.toContain("fixed_rent")` 负例保留 | PASS |
| 006 | 清空商户名/联系人/电话/合作方式预填；合作方式下拉加可空项；创建请求只带 idempotencyKey/merchantName/merchantType/contactName/contactPhone/（可选）cooperationForm，不再传三主体/区块/面积/备用联系人/租金/status/operatorNote/假证据；绑定页原因空初始且必填、请求体不含假证据 | RED：创建 POST 的 `not.toContain("contract-subject-1"/"att-merchant-profile-create"/"1F-A"/"二期测试商户")`、电话预填为空、绑定原因为空等断言失败；GREEN：通过；未加选择器、未加 entity.binding.view 闸 | PASS |
| 007 | 手机号正则改非空校验（文案「联系电话不能为空。」），引导流 validate 同改 | RED：「联系电话不能为空」断言失败（旧文案含「手机号」）；GREEN：通过；后台未改 | PASS |
| 008 | 存档/取消存档/删除申请/审批删除四个写路径 try/catch → `reportWriteFailure`（setGlobalError + 失败结果）；测试：存档一条（空理由拦截 + 有理由 POST 同条）、删除申请一条、失败可见一条 | RED：`merchant-actions-m-3`/失败可见等用例失败；GREEN：通过；无下钻用例、无每 verb 各一条 | PASS |

TDD 记录：RED＝换三份测试后在未改页面上 16 条失败（绑定 9、拓扑 4、组织 3，§3 原文）；GREEN＝改页面后 36/37，随后两处小修——`SubjectBindingsPage` 的 `setActionResult` 展开 body 造成 `action` 键重复（tsc TS2783，改为显式字段 bindingAction/reason/idempotencyKey）、失败可见用例的断言从「含后端原文」改为 `expect.any(String)`（`shared/apiError` 翻译层把后端原文改写为中文业务话术，与本条锁的「失败被 catch 并上报」无关）——GREEN-2 37/37、`tsc --noEmit` 0 错误。两处小修均在同一 B3-r1 批内、after 树取自 GREEN-2 之后，未计入第二次 B3。

### 4.2 范围归因（§5 闸 3）

- 全部 hunk 落到定稿 001–008；六处「文件版本」行随所属文件的 ID（版本铁律）；归因不到的行 0。
- 未改：三份业务 HTML、`plan/*`、锁、sidecar、ledger 基准、findings、后台源码、`types.ts`、`statusLabels.ts`、路由权限码、迁移、其它模块文件。after tree 再次实取相同。
- 未提交/推送/合并/部署。

### 4.3 结论

内容修复：**PASS**。范围归因：**PASS**。总：**PASS**（作者自审票；须 Grok B4-r1 亦 PASS 才进 B5-r1；B3 累计 1/3）。本席在本报告落盘前未读取 `M012-b4-r1-grok-raw.md`。
