CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M018 B5-r2 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T08:47:04Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；B3-r2 after tree `c9301ef0a5b95d210633c087dba236dbdb7cd3a1`。
- 模块 M018 7 文件（§1 全读），21 判据；B3 累计 2。B5-r1 席甲票经核源撤回的教训（首屏两个请求的后端权限分开核）本轮已按「每个请求 × 四类账号」逐条追。
- 顺序：Grok 已先派（modules/M018-b5-r2-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| apps/web/src/admin/pages/FloorPlanImportPage.tsx | 443 | 28070 | 75f227a783ea7992b77a67aa283eb716a5d8984a8f6479707f671cffa4e10acd | 全读（its=0） |
| apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx | 148 | 6654 | 5790a81e551db768b9ee12b23418bd949cddc5e83bd326ce40cd8f9d33a0fa97 | 全读（its=4） |
| apps/web/src/admin/pages/FloorPlanImportPage.test.tsx | 302 | 15313 | 68af58616c93dfa315ce0a8badc247dc01a48752e672bf19f3ae4dbc4a9a8a8f | 全读（its=15） |
| apps/web/src/admin/pages/SpaceTypesPage.tsx | 280 | 13541 | 4d450b0df0de1acdba22e9dec777f8fa11159defd395a3601b19171efd88deed | 全读（its=0） |
| apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx | 166 | 9633 | db80bdf26bdd9ce931e72591d666faf168ca06ad2aa993f91d792feefdfa9e4b | 全读（its=10） |
| apps/web/src/admin/pages/VenueBusinessHoursPage.tsx | 248 | 10962 | 841c1e543bd573d6fbfa7e2773f7aa391e58cee430695199e5bc105be32973a9 | 全读（its=0） |
| apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx | 119 | 5539 | 60f3642dd6161bbabffd75483af9ba9a05d84176ebf7e7461e8c8855c6e6118c | 全读（its=5） |

## 2. 可执行证据原文（/home/james/audit-tmp/m018_b5r2_run.log；修后重跑）

```
START=2026-09-20T08:46:50Z HEAD=e456ba96 origin/main=1343640d porcelain_total=145
== 0. seven files (lines/bytes/sha256/it-cases) + read-only deps
apps/web/src/admin/pages/FloorPlanImportPage.tsx 443 28070 75f227a783ea7992b77a67aa283eb716a5d8984a8f6479707f671cffa4e10acd its=0
apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx 148 6654 5790a81e551db768b9ee12b23418bd949cddc5e83bd326ce40cd8f9d33a0fa97 its=4
apps/web/src/admin/pages/FloorPlanImportPage.test.tsx 302 15313 68af58616c93dfa315ce0a8badc247dc01a48752e672bf19f3ae4dbc4a9a8a8f its=15
apps/web/src/admin/pages/SpaceTypesPage.tsx 280 13541 4d450b0df0de1acdba22e9dec777f8fa11159defd395a3601b19171efd88deed its=0
apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx 166 9633 db80bdf26bdd9ce931e72591d666faf168ca06ad2aa993f91d792feefdfa9e4b its=10
apps/web/src/admin/pages/VenueBusinessHoursPage.tsx 248 10962 841c1e543bd573d6fbfa7e2773f7aa391e58cee430695199e5bc105be32973a9 its=0
apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx 119 5539 60f3642dd6161bbabffd75483af9ba9a05d84176ebf7e7461e8c8855c6e6118c its=5
dep apps/web/src/admin/components/HighRiskConfirm.tsx 90 49815863a435cac4cc9a769fce84bbf6e0aa2e9397b0d7d9d9c606719d4a3b0e
dep apps/web/src/admin/components/GuidedFlowModal.tsx 146 910a25d0d3841bd47e7d4bbaaf75b6ed83549f681e8cc70f5b96ecbdf9a0c362
dep apps/web/src/admin/security/Can.tsx 84 bfd3fbeb22a5fd1ee620b63c139ae0045fc36caeaa3afd9d2766edb11061d193
dep apps/web/src/admin/security/PermissionGate.tsx 44 a15438dae20ba874cd4385edbd2a65cef85cda71724dd6cf10ce8e750d28a4a5
dep apps/web/src/admin/client/adminClient.ts 253 01127146ffc4ea4e1243fc10ae76c1f582a830ca35e5222562d33c6a67c2ec82
dep apps/web/src/admin/hooks/useAdminResource.ts 102 dba5f9a069cd8b185acbc0646ff219ee0770b66d5e88319a1a835e55bf94676b
dep apps/web/src/admin/components/RowDetailDrawer.tsx 97 7f044de11761ecdfc9f25ddc9cadbff90687a11710edfdfc11c9a962fc317bc5
dep apps/web/src/admin/adminRoutes.ts 188 b88500ae13ca94036f7a8e0df5d7bd4d4e7d18fdeb6031bf499d7f55e57d1112
dep apps/web/src/customer/i18n/statusLabels.ts 260 80252b0662da16df241f8cf9710bf43ab34f5e21d499883f1687fb181057cf6f
== 1. seven files vs HEAD (expect empty) / porcelain / vs origin/main (PR #439 not merged upstream)
 .../FloorPlanImportPage.guidedcreate.test.tsx      |  82 ++++++---
 .../src/admin/pages/FloorPlanImportPage.test.tsx   | 201 ++++++++++++++++++---
 apps/web/src/admin/pages/FloorPlanImportPage.tsx   | 154 +++++++++++-----
 .../pages/SpaceTypesPage.guidedcreate.test.tsx     |  84 +++++++--
 apps/web/src/admin/pages/SpaceTypesPage.tsx        |  13 +-
 .../admin/pages/VenueBusinessHoursPage.test.tsx    |  37 +++-
 6 files changed, 462 insertions(+), 109 deletions(-)
 M apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx
 M apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
 M apps/web/src/admin/pages/FloorPlanImportPage.tsx
 M apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx
 M apps/web/src/admin/pages/SpaceTypesPage.tsx
 M apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx
 .../FloorPlanImportPage.guidedcreate.test.tsx      |  80 +++++--
 .../src/admin/pages/FloorPlanImportPage.test.tsx   | 232 +++++++++++++++---
 apps/web/src/admin/pages/FloorPlanImportPage.tsx   | 261 +++++++++++++++++----
 .../pages/SpaceTypesPage.guidedcreate.test.tsx     | 103 +++++++-
 apps/web/src/admin/pages/SpaceTypesPage.tsx        | 174 ++++++++++++--
 .../admin/pages/VenueBusinessHoursPage.test.tsx    |  37 ++-
 .../web/src/admin/pages/VenueBusinessHoursPage.tsx |   9 +-
 7 files changed, 761 insertions(+), 135 deletions(-)
757ef452 NOT-in-origin/main
88c7b465 NOT-in-origin/main
ed216aae NOT-in-origin/main
PR439 state=OPEN head=88c7b465 created=2026-09-11T16:43:54Z
== 2. import error codes / field names: backend parser vs page label dictionaries vs test fixture
parser_codes: JSON_PARSE_FAILED NO_SHAPES_FOUND PAYLOAD_REQUIRED POINT_OUT_OF_RANGE POINTS_REQUIRED POINTS_TOO_FEW POLYLINE_NOT_CLOSED SHAPES_REQUIRED UNSUPPORTED_SOURCE_FORMAT VIEWBOX_REQUIRED 
parser_fields: points shapes source_format source_payload viewBox 
page ERROR_CODE_LABELS: PAYLOAD_REQUIRED UNSUPPORTED_SOURCE_FORMAT VIEWBOX_REQUIRED POINTS_REQUIRED POINTS_TOO_FEW POLYLINE_NOT_CLOSED POINT_OUT_OF_RANGE SHAPES_REQUIRED JSON_PARSE_FAILED NO_SHAPES_FOUND 
page ERROR_FIELD_LABELS: source_format points polygon_points shapes source_payload 
backend INVALID_POLYGON hits: 0 ; backend fieldName geometry hits: 0
fixture codes in tests: error_code: "POINTS_TOO_FEW" field_name: "points" shape_type: "corridor" shape_type: "shop" status: "active" status: "archived" status: "parsed" status: "pending" status: "published" status: "ready" 
== 3. closed sets: shape types / source formats / task+candidate status / floor plan status
backend SHAPE_TYPES: area booth entrance facility public_area risk_zone route shop 
V423 shapes CHECK: 
page SHAPE_TYPE_LABELS: area booth entrance facility public_area risk_zone route shop 
backend IMPORT_SOURCE_FORMATS: standard_json svg  ; page SOURCE_FORMAT_OPTIONS: standard_json svg  ; page default: sourceFormat: "svg" 
backend IMPORT_TASK_STATUSES: cancelled failed parsed published  ; IMPORT_CANDIDATE_STATUSES: pending published rejected  ; floor plan STATUSES: active archived inactive 
page floor plan status literals: status: "active" status === "pending" 
global status table hits: parsed=0 failed=1 published=1 cancelled=1 rejected=1 pending=1 processing=1
V425 space type status CHECK: active archived disabled pending_delete  ; page spaceKindStatusLabel: active archived disabled pending_delete 
== 4. permission codes: page buttons vs backend requirePermission
FloorPlanImportPage MANAGE: const MANAGE = ["floor_plan.manage"] ; route: requiredPermissions: ["floor_plan.view"]
  controller:33 floor_plan.view
  controller:39 floor_plan.manage
  controller:46 floor_plan.view
  controller:55 floor_plan.manage
  controller:62 floor_plan.import.view
  controller:71 floor_plan.import.manage
  controller:81 floor_plan.import.view
  controller:91 floor_plan.import.manage
  mapping:31 @GetMapping
  mapping:37 @PostMapping
  mapping:44 @GetMapping("/{id}/shapes")
  mapping:50 @PostMapping("/{id}/shapes")
  mapping:60 @GetMapping("/{id}/imports")
  mapping:66 @PostMapping("/{id}/imports")
  mapping:76 @GetMapping("/{id}/imports/{importId}/candidates")
  mapping:85 @PostMapping("/{id}/imports/{importId}/publish")
SpaceTypesPage MANAGE: const MANAGE = ["governance.space.manage"] ; VenueBusinessHoursPage MANAGE: const MANAGE = ["governance.space.manage"]
  platform:1:    @PostMapping("/space-types")
  platform:4:        authorizationService.requirePermission("governance.space.manage");
  platform:8:    @DeleteMapping("/space-types/{id}")
  platform:10:        authorizationService.requirePermission("governance.space.manage");
  platform:15:    @PostMapping("/space-types/{id}/archive")
  platform:19:        authorizationService.requirePermission("governance.space.manage");
  platform:24:    @PostMapping("/space-types/{id}/unarchive")
  platform:26:        authorizationService.requirePermission("governance.space.manage");
  platform:31:    @PostMapping("/space-types/{id}/delete-request")
  platform:35:        authorizationService.requirePermission("governance.space.manage");
  platform:40:    @PostMapping("/space-types/{id}/delete-approve")
seed codes:       3 'floor_plan.import.manage'       3 'floor_plan.import.view'       3 'floor_plan.manage'       3 'floor_plan.view' 
master hard-code: frontend 1 ; backend SessionIssuer MASTER_USER_ID = "usr_master_authority" MASTER_USERNAME = "master-authority"  ; delete-approve controller requirePermission lines: 0
== 5. client request-key normalization executed (functions extracted verbatim from adminClient.ts)
floorplan_page_body: {"idempotency_key":"admin-web-<generated>","plan_name":"x","project_id":"p","status":"draft"}
spacetype_page_body: {"idempotency_key":"space-type-PAGEKEY","type_code":"x","sort_order":100,"status":"active"}
venue_page_body: {"venue_id":"v","day_of_week":5,"open_time":"20:00","close_time":"02:00","disabled":false,"idempotency_key":"admin-web-<generated>"}
publish_body: {"idempotency_key":"admin-web-<generated>","candidate_ids":["a","b"]}
page snake keys sent: 
== 6. drill-down error handling in FloorPlanImportPage (loadImports/loadCandidates) and untested branches
  try/catch lines inside 106-122: 0
  publish fallback line: 
test refs: publish=apps/web/src/admin/pages/FloorPlanImportPage.test.tsx:3 apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx:0  import-create=apps/web/src/admin/pages/FloorPlanImportPage.test.tsx:1 apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx:0  archive/delete/approve=23 window.prompt/confirm stubs=8 venue disabled/degraded=3
sortOrder coercion: 84 274 
== 7. vitest (4 files) + typecheck
 ✓ src/admin/pages/VenueBusinessHoursPage.test.tsx (5 tests) 303ms
 ✓ src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx (10 tests) 359ms
 ✓ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx (4 tests) 413ms
 ✓ src/admin/pages/FloorPlanImportPage.test.tsx (15 tests) 646ms
 Test Files  4 passed (4)
      Tests  34 passed (34)
VITEST_EXIT=0
TSC_EXIT=0
== 8. HTML + lock untouched
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1.html
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-governance-map-for-operations-v0.1.html
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
END=2026-09-20T08:46:58Z
```

vitest 4 文件 34 例通过、`tsc -b --noEmit` exit 0。

## 3. 三线审计

- 第一性原理线（首屏请求 × 账号）：`FloorPlanImportPage` 首屏 `/floor-plans`（floor_plan.view）为主资源，`/platform/projects`（governance.project.view + 平台 scope）经 `loadSecondaryIfPermitted` 为副资源——仅 floor_plan.view 账号不发请求、表照常；商户账号（平台 scope 拒）只降级提示；下钻 import.view、创建导入/发布 import.manage、新建 floor_plan.manage 与 `FloorPlanController` 逐端点一致（010/003 修）。`SpaceTypesPage` 首屏 `/platform/space-types`（governance.space.view + 平台 scope）与路由 governance.space.view 一致；写动作 governance.space.manage；审批 master 由后端校验。`VenueBusinessHoursPage` 首屏 `/venue-business-hours`（governance.space.view）为主、`/platform/spaces` 经 `loadSecondary` 为副（同权限码，失败降级）。真实后端契约下第一次正常新建（001）、导入（007）、发布（008）、存档/删除申请/审批/恢复（009）、营业时间保存/停用均可达。
- 顺从式线：21 判据逐族——GEN-002/003/005/009 由 010 闭合，其余同 B5-r1 报告（001–009 已修，M015-010/011 XM 登记）。
- 对抗式线（grilling）：把 `loadSecondaryIfPermitted` 改回直接 `api.get` → 新增 2 例红；四类账号 × 三页首屏逐一推演无整页拖死；parsed/archived 中文、幂等键、未勾选拦截、分批发布同 B5-r1 复验；无新缺陷、无添加项、无 HTML 问题。

## 4. 已定稿 001–010 现态

全部已修（B3-r1/B3-r2），B4-r1/B4-r2 双 PASS；修复队列「是」= 0。

## 5. 本次 Findings

无新增。

## 6. 观察（登记）

- 同 B5-r1 §6（M015-010/011 XM；PR #439 随检查点合入并重生成门禁产物；sortOrder 0→100；`approveDeleteSpaceType` 控制器无 requirePermission 属后端模块）。

## 7. 结论

席甲 B5-r2 票：**PASS**。7 文件全读、21 判据逐族、vitest 34 例 + tsc 0 实跑；未改任何文件；落盘前未读 Grok 本次正文。
