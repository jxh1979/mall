CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M018 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票，按控制合同 §4「合取不变、推翻权不对等」处理。2026-09-20T07:17:10Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行，与 sidecar/ledger 顶部逐字相同；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main 主工作区，HEAD `e456ba96`（含 2026-09-11 本地合并的 PR #439「audit M018」提交 ed216aae；该 PR 仍 OPEN、未合并到 origin/main `1343640d`）；工作区 tree `6f80219df9c9f4470da99e037cbe27146dc152c8`（临时索引实算，真实索引未动）。三份业务 HTML 只读。
- 模块 M018「空间点位、楼层平面图与营业时段／网页与验证／第1段」，7 文件（计划冻结 1080 行；当前 1353 行，§1 实算），NORMAL，映射 21 判据（BSM-001、BSM-003、GEN-001…018、PGM-001）。历史未审：ledger 无任何 M018 段、findings 无 M018 ID；PR #439 不视为已审 PASS。
- 转入口径：M015-010（营业时段草案/生效日/停用另列，XM 同批，前端部分待后端）、M015-011（发布确认字段 XM 同批）、M015-013（前端默认值/闭集与后端不一致→M018）、M016-004 观察（前端 corridor / sourceFormat 默认）。
- 顺序：Grok 先派、本席独立取证并落盘本报告；本报告落盘前未读取 `M018-b1-grok-raw.md` 任何正文（该文件此刻存在=False，未读）。
- 只读承诺：未修改任何源码/文档/HTML/锁/台账；命令全部只读（vitest/tsc 只读运行；`git write-tree` 用临时索引）。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| apps/web/src/admin/pages/FloorPlanImportPage.tsx | 379 | 22672 | ae67f3e41c12d34550f4c88fe8f35627e0ff80595b0597ccedddf5ace8c37a1b | 全读（its=0） |
| apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx | 108 | 5307 | 03e2d8eb7820f78be45925edc73420082704fba9d7a06c5724a27ad65753dcb2 | 全读（its=3） |
| apps/web/src/admin/pages/FloorPlanImportPage.test.tsx | 155 | 6636 | 6c1e4f0da9bba28208a7df00d26694c191ebe935d6600ea34b91dcfc1cc32bed | 全读（its=6） |
| apps/web/src/admin/pages/SpaceTypesPage.tsx | 269 | 12783 | 16b352fd7f3cdd4c69ce88926d8d08da07ee055fdc5369ec841956f464eeab61 | 全读（its=0） |
| apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx | 106 | 5615 | a10d893b558c1c2696b8f6e3e4aa65efe830f3ba9fd21853df33286c0cfdf80f | 全读（its=5） |
| apps/web/src/admin/pages/VenueBusinessHoursPage.tsx | 248 | 10962 | 841c1e543bd573d6fbfa7e2773f7aa391e58cee430695199e5bc105be32973a9 | 全读（its=0） |
| apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx | 88 | 3798 | d965ed52b642d46694dc3c7332682b25edd9df6454d2409feabd20a41daf5e38 | 全读（its=3） |

只读依赖（追流不审，SHA 见 §2 第 0 项）：`components/HighRiskConfirm.tsx`、`components/GuidedFlowModal.tsx`、`security/Can.tsx`、`security/PermissionGate.tsx`、`client/adminClient.ts`、`hooks/useAdminResource.ts`、`components/RowDetailDrawer.tsx`、`adminRoutes.ts`、`customer/i18n/statusLabels.ts`；后端 `FloorPlanController/FloorPlanService/FloorPlanImportParser` 与 dto、`PlatformController/PlatformService`（空间种类、营业时间）、`VenueBusinessHoursRequest`、`SessionIssuer`、迁移 V023/V034/V423/V424/V425/V426。

## 2. 可执行证据原文（/home/james/audit-tmp/m018_b1_run.log、m018_b1_run2.log）

```
START=2026-09-20T07:17:02Z HEAD=e456ba96 origin/main=1343640d porcelain_total=131
== 0. seven files (lines/bytes/sha256/it-cases) + read-only deps
apps/web/src/admin/pages/FloorPlanImportPage.tsx 379 22672 ae67f3e41c12d34550f4c88fe8f35627e0ff80595b0597ccedddf5ace8c37a1b its=0
apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx 108 5307 03e2d8eb7820f78be45925edc73420082704fba9d7a06c5724a27ad65753dcb2 its=3
apps/web/src/admin/pages/FloorPlanImportPage.test.tsx 155 6636 6c1e4f0da9bba28208a7df00d26694c191ebe935d6600ea34b91dcfc1cc32bed its=6
apps/web/src/admin/pages/SpaceTypesPage.tsx 269 12783 16b352fd7f3cdd4c69ce88926d8d08da07ee055fdc5369ec841956f464eeab61 its=0
apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx 106 5615 a10d893b558c1c2696b8f6e3e4aa65efe830f3ba9fd21853df33286c0cfdf80f its=5
apps/web/src/admin/pages/VenueBusinessHoursPage.tsx 248 10962 841c1e543bd573d6fbfa7e2773f7aa391e58cee430695199e5bc105be32973a9 its=0
apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx 88 3798 d965ed52b642d46694dc3c7332682b25edd9df6454d2409feabd20a41daf5e38 its=3
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
 .../FloorPlanImportPage.guidedcreate.test.tsx      |   4 +
 .../src/admin/pages/FloorPlanImportPage.test.tsx   |  37 ++++-
 apps/web/src/admin/pages/FloorPlanImportPage.tsx   | 157 ++++++++++++++++----
 .../pages/SpaceTypesPage.guidedcreate.test.tsx     |  35 ++++-
 apps/web/src/admin/pages/SpaceTypesPage.tsx        | 163 +++++++++++++++++----
 .../web/src/admin/pages/VenueBusinessHoursPage.tsx |   9 +-
 6 files changed, 339 insertions(+), 66 deletions(-)
757ef452 NOT-in-origin/main
88c7b465 NOT-in-origin/main
ed216aae NOT-in-origin/main
PR439 state=OPEN head=88c7b465 created=2026-09-11T16:43:54Z
== 2. import error codes / field names: backend parser vs page label dictionaries vs test fixture
parser_codes: JSON_PARSE_FAILED NO_SHAPES_FOUND PAYLOAD_REQUIRED POINT_OUT_OF_RANGE POINTS_REQUIRED POINTS_TOO_FEW POLYLINE_NOT_CLOSED SHAPES_REQUIRED UNSUPPORTED_SOURCE_FORMAT VIEWBOX_REQUIRED 
parser_fields: points shapes source_format source_payload viewBox 
page ERROR_CODE_LABELS: INVALID_POLYGON VALIDATION_FAILED 
page ERROR_FIELD_LABELS: geometry shape_type source_format project_id 
backend INVALID_POLYGON hits: 0 ; backend fieldName geometry hits: 0
fixture codes in tests: error_code: "INVALID_POLYGON" field_name: "geometry" shape_type: "corridor" shape_type: "shop" status: "draft" status: "pending" status: "processing" status: "published" status: "ready" 
== 3. closed sets: shape types / source formats / task+candidate status / floor plan status
backend SHAPE_TYPES: area booth entrance facility public_area risk_zone route shop 
V423 shapes CHECK: 
page SHAPE_TYPE_LABELS: area booth entrance facility public_area risk_zone route shop 
backend IMPORT_SOURCE_FORMATS: standard_json svg  ; page SOURCE_FORMAT_OPTIONS: standard_json svg  ; page default: sourceFormat: "svg" 
backend IMPORT_TASK_STATUSES: cancelled failed parsed published  ; IMPORT_CANDIDATE_STATUSES: pending published rejected  ; floor plan STATUSES: active archived inactive 
page floor plan status literals: status === "draft" status: "draft" status === "pending" status === "published" 
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
page snake keys sent: idempotency_key: planKeyRef.current idempotency_key: importKeyRef.current idempotency_key: publishKeyRef.current 
== 6. drill-down error handling in FloorPlanImportPage (loadImports/loadCandidates) and untested branches
  try/catch lines inside 106-122: 0
  publish fallback line: 160
test refs: publish=apps/web/src/admin/pages/FloorPlanImportPage.test.tsx:0 apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx:0  import-create=apps/web/src/admin/pages/FloorPlanImportPage.test.tsx:0 apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx:0  archive/delete/approve=5 window.prompt/confirm stubs=0 venue disabled/degraded=0
sortOrder coercion: 84 263 
== 7. vitest (4 files) + typecheck
 ✓ src/admin/pages/VenueBusinessHoursPage.test.tsx (3 tests) 235ms
 ✓ src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx (5 tests) 329ms
 ✓ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx (3 tests) 315ms
 ✓ src/admin/pages/FloorPlanImportPage.test.tsx (6 tests) 389ms
 Test Files  4 passed (4)
      Tests  17 passed (17)
VITEST_EXIT=0
TSC_EXIT=0
== 8. HTML + lock untouched
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1.html
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-governance-map-for-operations-v0.1.html
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
END=2026-09-20T07:17:10Z
```

```
START2=2026-09-20T07:17:10Z
== 9. floor plan create: page body keys vs backend validateCreateRequest vs OpenAPI vs V023
page POST /api/v1/floor-plans body keys: idempotency_key plan_name project_id building_code floor_no status 
page floor_no/building optional per UI text: 1
backend validateCreateRequest requires:
  requireText(request.idempotencyKey(), "idempotency_key")
  requireText(request.projectId(), "project_id")
  requireText(request.floorNo(), "floor_no")
  requireField(request.planName(), "平面图名称")
  requireText(request.imageUrl(), "image_url")
  requireText(request.versionNo(), "version_no")
  requirePositive(request.imageWidth(), "image_width")
  requirePositive(request.imageHeight(), "image_height")
backend STATUSES: STATUSES = Set.of("active", "inactive", "archived")
FIELD_LABELS keys: 
openapi FloorPlanCreateRequest at line 27476:
        required: true
V023 mall_floor_plans columns:
      project_id VARCHAR(64),
      building_code VARCHAR(64),
      floor_no VARCHAR(64) NOT NULL,
      image_url VARCHAR(500) NOT NULL,
      image_width INTEGER NOT NULL,
      image_height INTEGER NOT NULL,
      version_no VARCHAR(64) NOT NULL,
      status VARCHAR(32) NOT NULL,
      CONSTRAINT chk_mall_floor_plans_image_size CHECK (image_width > 0 AND image_height > 0),
      CONSTRAINT chk_mall_floor_plans_status CHECK (status IN ('active', 'inactive', 'archived'))
      ON mall_floor_plans (project_id, building_code, floor_no, status);
guidedcreate test asserts status draft: 1 ; asserts version_no/image_url: 1
== 10. floor plan list status labels: backend values vs page floorPlanStatusLabel vs global table
page floorPlanStatusLabel branches: status === "published" status === "draft"  ; global table active/inactive/archived: active:"有效" inactive:"未激活" 
test fixtures floor plan status values:       1 "draft"       2 "pending"       2 "processing"       2 "published"       2 "ready" 
== 11. worktree tree id (temp index, real index untouched)
tree=6f80219df9c9f4470da99e037cbe27146dc152c8
END2=2026-09-20T07:17:10Z
```

## 3. 三线审计方法与覆盖

- 对象口径：主工作区 7 文件全文逐行读完（§1）；三份业务 HTML 取正本（BSM V0.14、PGM V0.12、BEQR V0.07）；判据锁 V0.11，M018 映射 21 项。页面声称的行为一律对照后端现行契约（只读追直接依赖）与客户端键归一实跑核实，不按注释/测试通过推定。
- 第一性原理线：从治理地图 L463-472「空间和点位…维护楼层、区域、点位…商场平面图网页导入：在后台上传或导入商场平面布局图，系统解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误后发布到正式平面图；发布为高危动作，须二次确认——不可静默覆盖既有布局证据」、业务地图 L566-577「配置数据可以存档、也可以删除（删除要审批）…存档（先下线）…删除（走审批·软删除）…由具备最高权限的人审批通过后执行（申请人不能审批自己的申请）…删前自动查占用…删除留痕」、L579-580「后台的新建/录入改成分步引导」、L588「平台管理员与最高权限是两个不同的角色」、L856「查看综合体介绍、营业时间…」、PGM-001/BSM-001 全中文运营界面，反推三页每个动作「为什么必须存在／是否被实际执行验证／哪个前提未验证」（§4）。
- 顺从式线：21 判据逐族（§5；GEN-002/003/005/007/008/009/015 为本模块要害）。对抗式线（grilling 自问自答）：把前端当作错误实现——真实后端下点「创建平面图」会怎样（§2 探针 9）；正常态 parsed / 真实错误码在界面显示什么（探针 2/3/10）；持有 import.manage 而无 floor_plan.manage 的账号能否发布（探针 4）；下钻 403 用户看到什么（探针 6）；网络失败后重试是否同键（探针 5 实跑）；发布时一个都不勾会发什么（:160）；最高权限身份前端硬编码与后端常量是否一致（探针 4）。
- 可执行产物：4 个测试文件在 160 `apps/web` 实跑 17/17 通过、`tsc -b --noEmit` 退出 0（探针 7）——测试通过不构成行为证据（判据 GEN-003），见 §4 夹具与后端不符各项。
- 本席禁止修复；无添加项、无 HTML 修改申请。

## 4. 与 HTML / 后端契约事实的分叉对照（要害）

| 页面声称 / 应锁住的规则 | 事实（§2 探针） | 结论 |
|---|---|---|
| 「＋ 新增平面图」分步引导（BSM L579；PGM L469/472）：提交 `idempotency_key/plan_name/project_id/building_code/floor_no/status:"draft"`，楼栋/楼层选填 | 后端 `FloorPlanService.validateCreateRequest:502-526` 必填 floor_no / image_url / version_no / image_width>0 / image_height>0，status ∈ {active,inactive,archived}（:33；V023:6-16 五列 NOT NULL + CHECK）；`FIELD_LABELS` 无这些键→报错「image_url不能为空」；guidedcreate 用例以 fetch 桩断言 status "draft"、不断言缺失字段 | 真实后端下第一次正常提交必失败（CX-001） |
| 平面图/导入任务/候选/错误全中文（PGM-001；页面 :16 注释「英文全清」；PGM L472 人工复核候选与错误） | 平面图 status 后端 active/inactive/archived，页面只译 published/draft（:45-49）→「有效/未激活/archived」；任务 parsed（正常态）无码表→直出 "parsed"，published→「已上架」；错误字段/错误码字典键后端 0 处（探针 2）→两列恒「未识别」；夹具用后端不产生的 published/draft/processing/INVALID_POLYGON/geometry | 枚举字典与闭集不一致（CX-002） |
| 按钮级权限「与后端 requirePermission 对齐」（Can.tsx:40；GEN-005） | 创建导入/发布按钮用 floor_plan.manage（:58/:229/:293），后端 :71/:91 要求 floor_plan.import.manage（V034 独立高危码）；下钻需 import.view，路由只要求 floor_plan.view | 权限码不一致（CX-003） |
| 下钻「查看导入任务／候选明细」失败可感知（GEN-009） | `loadImports/loadCandidates:106-122` 无 try/catch、`void` 调用（:220/:238）→ 未处理拒绝，无提示 | 静默失败（CX-004） |
| 失败重试复用同一幂等键（:131/:146/:162 KeyRef 设计；GEN-007） | 客户端 `normalizeRequestBody:90-92` 生成键覆盖页面 snake 键（探针 5 实跑：三处均 admin-web-<generated>）；SpaceTypesPage camel 键保留 | 页面幂等键失效（CX-005） |
| 发布为高危动作须二次确认（PGM L472） | `HighRiskConfirm`（:293-300）window.confirm + 权限门控；:160 未勾选＝全部待发布候选，确认文案不含数量；发布流 0 用例 | 通过（登记 OBS-1；用例缺口入 CX-006） |
| 空间种类：存档→申请删除→最高权限审批、申请人不能批自己、删前查占用、留理由（BSM L566-577） | 页面动作与后端 `PlatformService:562-668` 一致（存档/申请删除 window.prompt 理由；审批仅 master 可见 :202；后端 `approveDeleteSpaceType` 校验 master 与自批）；前端 master 身份硬编码 :35 与 `SessionIssuer` 常量一致；「已删除」行无恢复入口（后端 unarchive 允许 disabled） | 通过（登记 OBS-2/OBS-9）；生命周期动作 0 用例（CX-006） |
| 空间种类新建：编码/显示名必填、排序、新建即启用（页 :246） | 后端 `SpaceTypeRequest` 同名字段、V425 四值闭集与页面四值一致（探针 3）；`Number(form.sortOrder) \|\| 100` 填 0 静默成 100（:84/:263） | 通过（登记 OBS-3） |
| 场地营业时间：按星期配开/歇、跨零点提示、停用保留配置（M017-003 口径） | POST 体与 `VenueBusinessHoursRequest` 同名（探针 5 venue_page_body）；`effectiveFrom` 类型有、未用（:18）；「已生效」说明与后端「保存即生效」一致（M015-010 XM 待后端）；停用/降级/校验提示 0 用例 | 通过（登记 OBS-4/OBS-7）；用例缺口入 CX-006 |

## 5. 21 项判据逐族核对

| 判据 | 结论 | 依据 |
|---|---|---|
| BSM-001 整体说明 | 通过（登记） | 三页均为基础管理空间域的后台入口；无越域发明 |
| BSM-003 数据可改可删、操作有引导、权限分岗 | 通过（登记） | 空间种类存档/删除审批/最高权限口径与 L566-577 一致；分步引导两处；权限分岗见 CX-003（码不一致属技术性） |
| PGM-001 平台治理底座 | 不通过 | CX-002（正常态英文直出、错误字典失效） |
| GEN-001 必要性 | 通过 | 三页各对应 HTML 上位条目；无死代码（`effectiveFrom` 登记 OBS-4） |
| GEN-002 可执行正确性 | 不通过 | CX-001（新增平面图在真实后端必失败） |
| GEN-003 测试有效性 | 不通过 | CX-001/CX-002 夹具锁错误契约；CX-006 关键分支 0 用例 |
| GEN-004 信任边界 | 通过（登记） | 表单步内校验 + 后端校验；发布未勾选＝全部（OBS-1） |
| GEN-005 授权 | 不通过 | CX-003（按钮权限码与后端不一致；服务端兜底成立） |
| GEN-006 数据完整性 | 通过 | 写路径全走后端事务；前端无本地状态写入 |
| GEN-007 并发幂等 | 不通过 | CX-005（页面幂等键被覆盖） |
| GEN-008 接口一致性 | 不通过 | CX-001/CX-002/CX-003/CX-005 |
| GEN-009 错误处理 | 不通过 | CX-004（下钻静默失败） |
| GEN-010 安全 | 通过 | 无秘密；权限 fail-safe；高危确认 |
| GEN-011 审计证据 | 通过 | `AuditTrailHint` 事件名与后端事件一致（floor_plan.created/import_published、venue_business_hours.viewed/updated） |
| GEN-012 资源边界 | 通过 | 列表由后端诚实不分页（M015-007）；无无界循环 |
| GEN-013 构建配置 | 通过 | vitest/jsdom/jest-dom/user-event 已声明；tsc 0 |
| GEN-014 迁移 | 无实现面 | 本模块无迁移；闭集对照 V023/V034/V423/V425 |
| GEN-015 界面可用性 | 不通过 | CX-002（英文直出/「已上架」）；键盘/aria 通过（候选行 tabIndex+onKeyDown、aria-label） |
| GEN-016 文档可验证性 | 通过（登记） | `TechDetails` 证据串只列 /api/v1/floor-plans（OBS-8） |
| GEN-017 分类 | 通过 | 六条 finding 均为细化/技术性；无添加项 |
| GEN-018 证据 | 通过 | §2 命令原文 + HTML 逐字 + 后端源码/迁移原文 |

## 6. 本次 Findings（临时 ID CX-M018-00N；正式 ID 由 B2 分配）

### CX-M018-001 · 细化 · 新增平面图请求体与后端必填契约不符——真实后端下功能必失败，测试锁住错误契约

- 位置：`FloorPlanImportPage.tsx:125-140`（createPlan 请求体：idempotency_key / plan_name / project_id / building_code / floor_no / status:"draft"）、`:319`「平面图名称必填；楼栋/楼层选填」、`:320-324`（validate 只校验 projectId/planName）、`:343`「初始状态：草稿」；后端 `FloorPlanService.java:502-526 validateCreateRequest` 必填 floor_no、image_url、version_no、image_width>0、image_height>0，`normalizeAllowed(status, STATUSES)`，`:33 STATUSES = active/inactive/archived`；`V023__p1_floor_plan_polygons.sql:6-16` 五列 NOT NULL + CHECK；`FloorPlanService.FIELD_LABELS`（:49-50）无 image_url/version_no/floor_no 键 → 报错文案「image_url不能为空」。测试 `FloorPlanImportPage.guidedcreate.test.tsx:73-95` fetch 桩断言 status "draft"，不断言缺失字段。
- 事实：第一次正常提交必 400 VALIDATION_FAILED（楼层留空→「floor_no不能为空」；填楼层→「image_url不能为空」；再补齐→「状态取值无效」）。功能在生产不可用；用例全绿。
- 依据类型：2（后端源码/迁移原文）+ 4（§2 探针 9）。HTML 上位：治理地图 L469「维护楼层、区域、点位…」、L472「在后台上传或导入商场平面布局图」——底图/版本/尺寸是 V023 既有契约的下位细节，非新业务。判据：GEN-002 / GEN-008 / GEN-003。四分类：细化。工程缺陷：是。建议入队：是。
- 最小处置：引导弹窗补齐后端必填（楼层改必填；底图地址、底图宽、高、版本号），提交 status "active"（后端闭集，不发明 draft）；validate/summary 同步；guidedcreate 用例改断言完整 body（含 image_url/version_no/image_width/image_height、status active）并加一例缺楼层被步内校验拦截。不改后端/OpenAPI（FIELD_LABELS 缺键登记 OBS-5 归 M015）。

### CX-M018-002 · 细化 · 平面图/导入任务/候选/错误的枚举字典与后端闭集不一致，正常状态英文直出

- 位置：`FloorPlanImportPage.tsx:45-49 floorPlanStatusLabel`（只译 published/draft）、`:236/:179/:260`（任务、候选状态走 `formatBusinessStatus`）、`:31-40 ERROR_FIELD_LABELS / ERROR_CODE_LABELS`、`:41-44 zhOrDash/fpLabel`；后端 `FloorPlanService.java:33/38-39` 闭集（平面图 active/inactive/archived；任务 parsed/failed/published/cancelled；候选 pending/published/rejected）、`FloorPlanImportParser.java:45-128` 错误码 10 个 + 字段 source_format/viewBox/points/shapes/source_payload；`customer/i18n/statusLabels.ts:255-260` 未知码回落原码，无 parsed/archived 键，published＝「已上架」。测试 `FloorPlanImportPage.test.tsx:8-24` 夹具 published/draft/processing/INVALID_POLYGON/geometry（后端 0 处）。
- 事实：平面图列表显示「有效/未激活/archived」；导入任务解析成功的正常态显示英文 "parsed"，发布后显示「已上架」；导入错误表「字段」「错误码」两列对全部真实错误恒「未识别」。全部 6 例绿（探针 2/3/10）。
- 依据类型：2 + 4。HTML 上位：治理地图 L472「由人工复核候选与错误后发布」；PGM-001。判据：GEN-008 / GEN-015 / GEN-003。四分类：细化。工程缺陷：是。建议入队：是。
- 最小处置：页内字典改为后端闭集——平面图 active「启用」/inactive「停用」/archived「已存档」，任务 parsed「已解析」/failed「解析失败」/published「已发布」/cancelled「已取消」，候选 pending「待复核」/published「已发布」/rejected「已驳回」，错误字段 5 键、错误码 10 码中文短语（与解析器中文 errorMessage 同义，不另发明）；夹具改真实码并断言 parsed/failed/published 与真实错误码中文。只改本模块 2 文件（页 + .test）。

### CX-M018-003 · 技术性 · 创建导入/发布按钮权限码与后端 requirePermission 不一致

- 位置：`FloorPlanImportPage.tsx:58 MANAGE=["floor_plan.manage"]` 用于 `:206-211` 新增平面图（后端 `FloorPlanController.java:39` floor_plan.manage，一致）、`:229` 创建导入（后端 `:71` floor_plan.import.manage）、`:293-300` 发布（后端 `:91` floor_plan.import.manage）；下钻 `:112/:120` 需 floor_plan.import.view（`:62/:81`），路由 `adminRoutes.ts:95` 只要求 floor_plan.view；`V034:60-61` 两码独立种子（import.manage high / requires_approval=TRUE）；`Can.tsx:40`「与后端 requirePermission 对齐」。
- 事实：持有 floor_plan.manage 而无 import.manage 的账号按钮可点→后端 403（服务端拦截成立）；持有 import.manage 而无 floor_plan.manage 的账号前端禁用→功能不可达。
- 依据类型：2（后端源码/迁移）+ 4（探针 4）。判据：GEN-005 / GEN-008。四分类：技术性（只对齐既有权限码，无业务发明）。工程缺陷：是。建议入队：是。
- 最小处置：`IMPORT_MANAGE=["floor_plan.import.manage"]` 用于创建导入与发布两处；+1 用例（无 import.manage 时发布按钮 disabled）。路由入口不改。

### CX-M018-004 · 技术性 · 下钻加载无错误处理（静默失败）

- 位置：`FloorPlanImportPage.tsx:106-114 loadImports`、`:116-122 loadCandidates` 无 try/catch，`:220/:238` 以 `void` 调用；对照 `createPlan:139`、`createImport:155`、`publishImport:168` 均有 catch。
- 事实：接口失败（含 CX-003 场景的 403、网络错误）成为未处理拒绝，页面无任何提示、旧表不变（探针 6：区间内 try/catch 0）。
- 依据类型：4（源码原文 + 探针）。判据：GEN-009。四分类：技术性。工程缺陷：是。建议入队：是。
- 最小处置：两函数包 try/catch → `setGlobalError((e as Error).message)`；+1 用例（imports 接口返回 403 → setGlobalError 被调）。

### CX-M018-005 · 技术性 · 页面幂等键以 snake 键传入被客户端覆盖，失败重试幂等失效

- 位置：`FloorPlanImportPage.tsx:131-135`（planKeyRef）、`:146-150`（importKeyRef）、`:162-165`（publishKeyRef）传 `idempotency_key: xxxKeyRef.current`；`client/adminClient.ts:90-92 normalizeRequestBody` 先补 `idempotencyKey ?? nextIdempotencyKey()` 再整体转 snake，后者覆盖前者。
- 事实：§2 探针 5 用从 adminClient.ts 逐字抽取的函数实跑——floorplan_page_body / publish_body 的 idempotency_key 均为 admin-web-<generated>，页面键丢失；SpaceTypesPage（camel 键 :81）保留。`:131 if (!planKeyRef.current)` 的「失败重试复用同键」设计全部失效：首次请求服务端已成功而响应失败后，重试携新键→重复创建平面图/导入任务（发布因候选已 published 会被 `FloorPlanService:385` 拒）。
- 依据类型：4（可复现命令 + 输出）。判据：GEN-007 / GEN-008。四分类：技术性。工程缺陷：是。建议入队：是。
- 最小处置：三处改 `idempotencyKey: xxxKeyRef.current`（与 SpaceTypesPage 同范式）；guidedcreate 用例断言 `lastPostBody.idempotency_key` 以 "fp-" 开头。

### CX-M018-006 · 技术性 · 三页关键分支无用例

- 位置：`FloorPlanImportPage.test.tsx` / `.guidedcreate.test.tsx`：发布流（勾选→确认→POST candidate_ids；`:161` 空候选提示）0 处 `floor-plan-publish` 引用，创建导入 POST body（source_format/source_name/source_payload）0 处；`SpaceTypesPage.guidedcreate.test.tsx:92-105` 只断言按钮存在——存档（window.prompt 理由 + confirm → POST /archive {reason}）、取消存档、申请删除、最高权限审批通过/驳回（`SpaceTypesPage.tsx:202` isMaster 分支）与非 master「待最高权限审批」提示 0 处执行（prompt/confirm 桩 0）；`VenueBusinessHoursPage.test.tsx`：停用保存 disabled:true、场地清单降级提示、未选场地/未填时刻提示 0 处（探针 6）。
- 事实：发布（PGM L472 高危动作）、删除审批链（BSM L570）、停用保留配置（M017-003 口径）这三条 HTML 直接规定的行为在前端没有可失败断言；改坏 `publishImport` 的 candidate_ids 或 `approveDeleteType` 的 decision 值，17 例仍绿。
- 依据类型：4。判据：GEN-003。四分类：技术性。工程缺陷：是。建议入队：是。
- 最小处置：每页补最小用例——发布 1（勾选 1 个候选→confirm→POST body candidate_ids）、创建导入 1、空间种类存档/申请删除/审批各 1（stub window.prompt/confirm）、营业时间停用 1、降级提示 1；只改 3 个测试文件。

## 7. 观察（登记、不入队）

- OBS-1：`FloorPlanImportPage.tsx:160` 未勾选＝发布全部待发布候选，`HighRiskConfirm` 文案不含本次数量；治理地图 L472「人工复核后发布」不禁止，登记（>200 时后端 M015-008 拒绝并显错）。
- OBS-2：`SpaceTypesPage.tsx:35` 最高权限身份前端硬编码，与后端 `SessionIssuer.MASTER_USER_ID/MASTER_USERNAME` 一致；后端 `PlatformController:215-221 delete-approve` 无 requirePermission、由 `PlatformService.approveDeleteSpaceType:643-668` 校验 master 与自批（后端归 M009 系列模块）；前端审批按钮无 permissionsState 门控与之一致。
- OBS-3：`SpaceTypesPage.tsx:84/:263` `Number(form.sortOrder) || 100`：排序填 0 静默成 100（确认页可见）。
- OBS-4：`VenueBusinessHoursPage.tsx:18` `effectiveFrom` 类型字段未用；「已生效」说明（:224）与后端「保存即生效」一致；M015-010 XM（草案/生效日/停用另列）前端部分待后端同批，不在本模块单独改。
- OBS-5：后端 `FloorPlanService.FIELD_LABELS` 无 image_url/version_no/floor_no 等键，报错文案英文键（M015-005 口径「中文整句」的遗漏，归 M015/XM）。
- OBS-6：PR #439（2026-09-11，含本模块 6 文件 + V424–V426 + PlatformService/JdbcPlatformRepository 等）未合并 origin/main，主工作区 HEAD 已含其合并提交 ed216aae；本模块收口检查点须携带或先合并该 PR 才能让 origin/main 的前端调到存档/删除审批端点（检查点时处理，记入 ledger）。
- OBS-7：`VenueBusinessHoursPage` 无页面级幂等键（客户端每次生成），upsert 唯一性由后端/V226（M247）保证；登记。
- OBS-8：`FloorPlanImportPage.tsx:203` TechDetails 证据串只列 /api/v1/floor-plans，未列 imports/candidates/publish；登记。
- OBS-9：`SpaceTypesPage` 对 status=disabled（已删除）行不提供任何动作，后端 `unarchiveSpaceType:593` 允许 archived/disabled 恢复；业务地图 L571「数据始终留在库里可查、可审计、可恢复」未指定恢复入口在前台还是后台；登记待 B2 与 Grok 意见合并裁定，不单方立项。
- OBS-10：M016-004 所记「前端 sourceFormat 默认 geojson」已在 PR #439 改为 svg/standard_json，与后端 `IMPORT_SOURCE_FORMATS` 一致（探针 3）；SHAPE_TYPE_LABELS 八值与 SHAPE_TYPES/V423 一致；corridor 显示「未识别」有用例。
- OBS-11：无添加项；无 HTML 修改申请；无 questions；本模块 7 文件之外未发现需 XM 的新后端缺陷（OBS-2/OBS-5 为登记）。

## 8. 结论

本席 B1 票：**FAIL**（CX-M018-001…006 为细化/技术性工程缺陷且建议入队；均只改本模块 7 文件之内，不改后端/OpenAPI/HTML）。7 文件全文三线审完、21 判据逐族核对、4 测试文件 17/17 与 tsc 0 实跑为证据；未修源、未改 HTML/锁/台账。本报告落盘前未读取 `M018-b1-grok-raw.md` 任何正文。


## M018 B2-r1 整批候选复核（席甲票）

- 时间：2026-09-20T07:36:52Z。输入：modules/M018-b1-grok-raw.md V0.01（36814 bytes，SHA256 e94832c95ab57fad2f621a173ba9a3894c57bc779368e2c8f1da260962cd9f6a）、modules/M018-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M018 B2-r1 整批候选」段（全文件 SHA256 430c9671431fb76570e0b1b9dd6035604b1abc6fc1f3262f8390bba98f6ac058）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：Grok GR-01…09 与席甲 CX-001…006 全部归并，同根四组合并（001/002/003/006），Grok 独有 GR-02/03/08 → 007/008/009（席甲核源：`validateImportCreateRequest` requireText(source_name) 成立；:160 未勾即全发成立；`unarchiveSpaceType:593` 允许 disabled 恢复成立），席甲独有 CX-004/005 → 004/005；GR-06/07 与席甲 OBS-4/5 同根并入 M015-010/011（否/XM）；无漏并、无改写。
- 分类：接受 Grok 对 002 的冲突改判（PGM-001 零英文明文）与 003 细化（BSM-003 权限分岗上位）；001/007/008/009 细化上位为治理地图 L469/L472 与业务地图 L571；004/005/006 技术性无业务发明。
- 最小处置：001 由弹窗采集后端必填并送 active（后端无默认策略，前端采集为本模块内最小；不改后端/OpenAPI）；002 页内映射用词不发明业务概念（启用/停用/已存档/已解析/解析失败/已发布/已取消/待复核/已驳回，与解析器中文文案同义）；003 含下钻 Can 包裹；008 未勾选拦截；009 恢复走既有 unarchive；006 用例清单覆盖各 ID。均只改本模块 7 文件。
- 无添加项、无 HTML 申请、无 questions；无须升为添加的项。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。


## M018 B2-r2 整批候选复核（席甲票）

- 时间：2026-09-20T08:25:19Z。输入：modules/M018-b5-r1-grok-raw.md V0.01（38891 bytes，SHA256 cd5789ec692859bdbe39d419157e27f9e32cbf399a1145afd7ed3e659e0a57c6）、modules/M018-b5-r1-self-raw.md V0.01+V0.02（15626 bytes，SHA256 a37b3c3972f6fcda0de61389a2542c416b796d6441c217d5155d3dd827043d8d）；候选 findings.md 顶部「当前 M018 B2-r2 整批候选」段（全文件 SHA256 2fe3ed62c97a2877fbcc36d089bf03bec4399e3846b8403ab8586c6c4d20f08f）。本票落盘前未读 Grok B2-r2 追加段。不写独占锁行。
- 差集：GR-M018-B5-01 ↔ M018-010 一一对应；席甲撤回段无第三条；Grok OBS-01…15 登记不入队；无漏并、无改写。
- 010：分类细化（业务地图 L588 岗位范围 / 治理地图 L472 的下位实现——首屏主资源与副资源分离）；工程缺陷成立（`PlatformController:148-149`、`FloorPlanImportPage.tsx:113-121`、`adminRoutes.ts:95` 原文）；最小处置沿用同模块 `loadSecondaryIfPermitted` 范式，不改路由/后端/权限码，+2 用例；队列是。同意。
- 001–009 不动、不回潮；无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r2 票：**PASS**（整批候选同意）。
