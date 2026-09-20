CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M019 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按控制合同 §4「合取不变、推翻权不对等」处理）。2026-09-20T09:30:46Z
- 控制合同 V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 V0.11（实算见首行，与 sidecar/ledger 顶部逐字相同；LOCK-AMEND-COUNT 1）。
- 正本主工作区 HEAD `e456ba96`（含 2026-09-11 本地提交 d84e419b「audit M019」＝PR #442 文档部分，未合并 origin/main `1d84ec36`，4 文档对 origin/main 差 14+/11−）；工作区 tree `feac8a43df1ab17b76fb14f634091ec966adfc07`。三份业务 HTML 只读。
- 模块 M019「空间点位、楼层平面图与营业时段／文档与接口定义／第1段」，4 文档（计划冻结 680 行；当前 683 行，§1），映射 58 判据（BEQR 8、BSM 17、GEN 18、PGM 15）。历史未审；PR #442 不视为已审 PASS。
- 转入口径：M015-001（派生主键 SHA 形式）、002/003（project_id 必填且存在、项目 scope）、015（平台管理员全量桥）、016（发布前置 parsed|published）、M016-002/M015-013（候选区域八值无走道）、M017、M018——已定稿修复使旧文档声称过时即 GEN-016 缺陷。
- 顺序：Grok 先派、本席独立取证并落盘本报告；本报告落盘前未读取 `M019-b1-grok-raw.md` 任何正文（该文件此刻存在=False，未读）。
- 只读承诺：未修改任何源码/文档/HTML/锁/台账；命令全部只读（`git write-tree` 用临时索引）。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/p1-floor-plan-polygon-api-contract-v0.1.md | 130 | 7640 | 1c5deb13e38c2bd269b1a9cf70375ce4647fc72430baf2f267e03909fec51f25 | 全读 |
| docs/p2-floor-plan-import-api-contract-v0.1.md | 211 | 6343 | 89309e0d9d6ef3e64b61da6873544925cadd71462ec9bfb2fd231360b691980f | 全读 |
| docs/p2-floor-plan-import-completion-audit-v0.1.md | 67 | 3863 | 2b4e923a9fa153e6c29fbfff6a10aa63ec8144a4a1e965410a235c527cb9912a | 全读 |
| docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md | 275 | 11617 | eb94a20f69cb1c5afc880a6e34ebbc0a6cf0066f07320c96a573d1039996645f | 全读 |

只读依赖（追流不审）：`FloorPlanController/FloorPlanService/FloorPlanImportParser` 与 dto、`docs/openapi/openapi-p0.yaml`、V001/V023/V034/V423 迁移、`FloorPlan*Tests`。

## 2. 可执行证据原文（/home/james/audit-tmp/m019_b1_run.log）

```
START=2026-09-20T09:27:27Z HEAD=e456ba96 origin/main=1d84ec36
== 0. four files (lines/bytes/sha256)
docs/p1-floor-plan-polygon-api-contract-v0.1.md 130 7640 1c5deb13e38c2bd269b1a9cf70375ce4647fc72430baf2f267e03909fec51f25
docs/p2-floor-plan-import-api-contract-v0.1.md 211 6343 89309e0d9d6ef3e64b61da6873544925cadd71462ec9bfb2fd231360b691980f
docs/p2-floor-plan-import-completion-audit-v0.1.md 67 3863 2b4e923a9fa153e6c29fbfff6a10aa63ec8144a4a1e965410a235c527cb9912a
docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md 275 11617 eb94a20f69cb1c5afc880a6e34ebbc0a6cf0066f07320c96a573d1039996645f
== 1. vs HEAD (expect empty) / porcelain / vs origin/main (PR #442 docs part not upstream)
 docs/p1-floor-plan-polygon-api-contract-v0.1.md                  | 4 ++--
 docs/p2-floor-plan-import-api-contract-v0.1.md                   | 9 +++++----
 docs/p2-floor-plan-import-completion-audit-v0.1.md               | 5 +++--
 .../plans/2026-06-15-p2-floor-plan-svg-json-import.md            | 7 ++++---
 4 files changed, 14 insertions(+), 11 deletions(-)
== 2. p1 contract fields vs DTO / OpenAPI
p1 4.1 fields: idempotency_key operator_note evidence_attachment_ids project_id building_code floor_no plan_name image_url image_width image_height version_no status operator_user_id 
DTO FloorPlanCreateRequest: idempotencyKey operatorNote projectId buildingCode floorNo planName imageUrl imageWidth imageHeight versionNo status 
p1 4.2 fields: idempotency_key operator_note evidence_attachment_ids shape_id shape_type ref_type ref_id label polygon_points z_index status operator_user_id 
DTO FloorPlanShapeSaveRequest: idempotencyKey operatorNote shapeId shapeType refType refId label polygonPoints zIndex status 
openapi FloorPlanCreateRequest: idempotency_key operator_note project_id building_code floor_no plan_name image_url image_width image_height version_no status  additionalProperties=additionalProperties: false
sed: -e expression #1, char 1: unknown command: `,'
sed: -e expression #1, char 1: unknown command: `,'
openapi FloorPlanShapeSaveRequest:  additionalProperties=
operator_user_id in floorplan main: 0 files; evidence_attachment_ids in floorplan dto: 0 files
== 3. p1 error matrix vs service codes
p1 6 codes: FLOOR_PLAN_NOT_FOUND FLOOR_PLAN_SHAPE_NOT_FOUND IDEMPOTENCY_CONFLICT VALIDATION_FAILED 
service codes: CROSS_MERCHANT_FORBIDDEN CROSS_PROJECT_FORBIDDEN FLOOR_PLAN_IMPORT_TOO_MANY_NODES FLOOR_PLAN_IMPORT_TOO_MANY_POINTS FLOOR_PLAN_NOT_FOUND IMPORT_CANDIDATE_NOT_FOUND IMPORT_TASK_NOT_FOUND PROJECT_NOT_FOUND VALIDATION_FAILED 
FLOOR_PLAN_SHAPE_NOT_FOUND in backend: 0 files; shape update unknown id: 261:        String requestedShapeId = trimToNull(request.shapeId());;266:            throw ApiException.badRequest("VALIDATION_FAILED", "图形不属于该平面图");;
== 4. p2 contract / completion audit claims vs current code
p2 3.4 publish rule line: 151:- 仅 `parsed` 状态的导入任务允许发布。
audit 2 publish line: 18:| 人工发布 | 仅 `parsed` 任务和 `pending` 候选允许发布 |
service publish guard: 367:        if (!"parsed".equals(task.status()) && !"published".equals(task.status())) {
p2 4.1 non-eight rule: 165:- 可通过 `data-shape-type`、`data-ref-type`、`data-ref-id` 覆盖；不在八种内的记入错误明细，不生成候选。
service candidate loop normalizeAllowed: 321:                    normalizeAllowed(candidate.shapeType(), "shape_type", SHAPE_TYPES),; try/catch around candidate loop: 0
p2 4.1 polygon-only line: 160:- 仅解析 `<polygon>`。; parser polyline closed accepted: 18:    private static final Pattern SVG_SHAPE_PATTERN = Pattern.compile("(?is)<(polygon|polyline)\\b([^>]*)/?\\s*>");;79:            if ("polyline".equals(tag) && !samePoint(rawPoints.getFirst(), rawPoints.getLast())) {;80:                errors.add(error(shapeNo, "points", "POLYLINE_NOT_CLOSED", "折线（polyline）必须首尾闭合后才能导入"));;
p2 3.1 example id: 56:    "id": "fpi_floor-import-create-001",; service derivedId: 298:        String taskId = derivedId("fpi_", floorPlanId + "_" + request.idempotencyKey());
p2 4.1 defaults: 163:- 默认 `shape_type=shop`（铺位），默认 `ref_type=custom`。; parser defaults:  defaultText(attributes.get("data-shape-type"), "shop"),; defaultText(attributes.get("data-ref-type"), "custom"),; blankToNull(attributes.get("data-ref-id")),; defaultText(firstNonBlank(attributes.get("data-label"), attributes.get("id")), "shape-" + shapeNo),;
p2/audit permission+event names vs service events: docs=15 service=14; diff: < floor_plan.import 
== 5. referenced paths in the four docs exist?
OK backend/src/main/java/com/complex/platform/floorplan
OK backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportCreateRequest.java
OK backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportPublishRequest.java
OK backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java
OK backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportCandidateRecord.java
OK backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportErrorRecord.java
OK backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParseResult.java
OK backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java
OK backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportTaskRecord.java
OK backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java
OK backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
OK backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java
MISSING backend/src/main/resources/db/migration/V001__initial_schema.sql
OK backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql
OK backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java
OK backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java
OK backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
OK backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
OK docs/00-current-effective-document-entry-v0.1.md
OK docs/master-roadmap.md
OK docs/p0-user-story-map-and-sprint-slices-v0.1.md
OK docs/p2-floor-plan-import-api-contract-v0.1.md
OK docs/superpowers/plans/2026-06-14-p1-floor-plan-polygons.md
OK docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md
== 6. HTML anchor sentences (read-only)
472:                <li>商场平面图网页导入：在后台上传或导入商场平面布局图，系统解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误后发布到正式平面图；发布为高危动作，须二次�
700:                <li>建立综合体项目、楼层、区域、点位；当前一个综合体，未来可扩展为多综合体。</li>
== 7. HTML + lock
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
tree=feac8a43df1ab17b76fb14f634091ec966adfc07
END=2026-09-20T09:27:28Z
```

## 3. 三线审计方法与覆盖

- 对象口径：4 文档全文逐行读完；判据锁 M019 映射 58 项；文档声称按对象只读追现行 DTO/OpenAPI/服务/解析器/迁移（§2 探针 2–5）。
- 第一性原理线：治理地图 L469-472（维护楼层/区域/点位；商场平面图网页导入→候选区域（铺位、摊位、公共区）→人工复核→发布，高危二次确认，不可静默覆盖）、业务地图 L700「建立综合体项目、楼层、区域、点位」为四文档的上位；四文档分别是 P1 契约、P2-A 契约、P2-A 完成审计、P2 开发计划——存在理由＝把 HTML 的平面图能力固化为可编码契约与验证记录；GEN-016 三问：路径/字段/错误码/规则/示例是否可从当前纳入范围复现。
- 顺从式线：58 判据逐族（§5；GEN-016 为核心，GEN-008 次之）。对抗式线（grilling）：照文档写请求会不会被现行后端拒绝（§4 第 1 行）；照文档预期的错误码会不会与实际不同（§4 第 2 行）；文档说的发布/解析规则被 M015-016 与解析器现行实现推翻了哪些（§4 第 3–5 行）；引用路径 ls 是否存在（探针 5）；事件/权限/状态闭集是否一致（探针 4，一致）。
- 文档模块不强制运行测试（runbook §5）；取证为只读命令原文。本席禁止修复。

## 4. 与现行实现 / 契约的分叉对照（要害）

| 文档声称 | 现行事实（§2 探针） | 结论 |
|---|---|---|
| p1 §4.1/§4.2：`evidence_attachment_ids`（否）、`operator_user_id`（**必填**）为创建平面图/保存标注字段 | DTO `FloorPlanCreateRequest`/`FloorPlanShapeSaveRequest` 均无此两字段；OpenAPI `FloorPlanCreateRequest` 字段集同 DTO 且 `additionalProperties: false`；floorplan 包 0 处 operator_user_id | 照文档带 `operator_user_id` 提交即 400；「必填」字段根本不存在（CX-001） |
| p1 §6：标注不存在 → 404 `FLOOR_PLAN_SHAPE_NOT_FOUND`；矩阵只列 4 个业务码 | 后端 0 处该码；`FloorPlanService:261-266` 未知/他图 shape_id → 400 VALIDATION_FAILED「图形不属于该平面图」；现行另有 PROJECT_NOT_FOUND / CROSS_PROJECT_FORBIDDEN / CROSS_MERCHANT_FORBIDDEN（M015-002/003）未入矩阵 | 错误矩阵与实现分叉（CX-001） |
| p1 §9 引用 `backend/src/main/resources/db/migration/V001__initial_schema.sql` | 路径不存在（现为 `V001__baseline.sql`；P0 轻量 `polygon_points` 在 `V005__p0_platform_master_data.sql:64`） | 引用失效（CX-001） |
| p2 §3.4「仅 `parsed` 状态的导入任务允许发布」；完成审计 §2「仅 `parsed` 任务和 `pending` 候选允许发布」 | `FloorPlanService:367` M015-016：parsed 或 published 均可继续发布剩余 pending 候选 | 规则过时（CX-002） |
| p2 §4.1「不在八种内的记入错误明细，不生成候选」 | `createImportTaskOnce` 候选循环 `normalizeAllowed(candidate.shapeType…)`（:321）无 try/catch → 整单 400 VALIDATION_FAILED，不落任务、不落错误明细（M017-005 用例锁定白名单拒绝） | 解析规则与实现分叉（CX-002） |
| p2 §4.1「仅解析 `<polygon>`」 | 解析器 `SVG_SHAPE_PATTERN` 同时匹配 polygon/polyline，闭合 polyline 生成候选（:79-84），未闭合记 POLYLINE_NOT_CLOSED | 措辞与实现分叉（CX-002） |
| p2 §3.1 响应示例 `"id": "fpi_floor-import-create-001"` | M015-001：`derivedId("fpi_", floorPlanId + "_" + key)` = 前缀 + SHA-256 前 32 位（36 字符） | 示例过时（CX-002） |
| p2 §2 权限、§3 事件、完成审计 §3 接口/权限、计划 §5 状态闭集 | 与 V034、Controller、服务事件（探针 4：事件名集合一致）、`IMPORT_TASK_STATUSES/IMPORT_CANDIDATE_STATUSES` 一致 | 通过 |
| 四文档其余引用路径 23 条 | 全部存在（探针 5） | 通过 |

## 5. 58 项判据逐族核对

| 判据族 | 结论 | 依据 |
|---|---|---|
| BEQR-074/097/115/156/163/166/170/192 | 无本面（登记） | 四文档不涉隐私桌台、供应合同、税务快照、合同状态/变更/审批链、租赁状态、分红税务；无冲突无发明 |
| BSM-009/027/028/029 | 通过 | 平面图/点位属基础管理盘子，L700 上位；文档不发明多综合体规则 |
| BSM-072…077、114、115、116…119 | 无本面（登记） | 商户/招商/入口矩阵/上线加固不在四文档实现面；p1 §3 权限表与 BSM-114「只做自己该做的」一致 |
| BSM-120 / PGM-066 | 通过（登记） | 文档不把待排期项写成已实现；完成审计 §6 未纳入范围明确 |
| PGM-007/016/026…031/057…060 | 无本面（登记） | 合同准入、应急隐私、经销治理不在四文档 |
| PGM-022 / PGM-023 | 通过（登记） | 候选区域八值含 HTML 三例且无走道（PR #442 已改措辞）；发布高危/不可静默覆盖在 p2 §5 与计划 §11 有对应 |
| GEN-001 必要性 | 通过 | 四文档各有上位与用途 |
| GEN-002 可执行正确性 | 不通过 | CX-001（照契约写的请求被现行 OpenAPI/DTO 拒绝） |
| GEN-003 测试有效性 | 通过（登记） | p1 §8 / p2 §6 列出的测试类存在；测试有效性本体在 M017 |
| GEN-004/005/006/007/010/011/012 | 通过（登记） | 文档对校验、权限码、幂等、事件、上限（F-CROSS-10 注）的描述与实现一致 |
| GEN-008 接口一致性 | 不通过 | CX-001（字段/错误码）、CX-002（规则/示例） |
| GEN-009 错误处理 | 不通过 | CX-001 错误矩阵缺现行码、含不存在码 |
| GEN-013 / GEN-014 | 无本面 | 无构建/迁移文件；计划 §5 与 V034 列一致 |
| GEN-015 | 通过 | 文档用词中文；候选种类「给人看只用这八种中文」 |
| GEN-016 文档可验证性 | 不通过 | CX-001（不存在字段/码/路径）、CX-002（过时规则/示例） |
| GEN-017 分类 | 通过 | 两条均细化（HTML 上位 L472/L700 的下位契约文档），无添加项 |
| GEN-018 证据 | 通过 | §2 命令原文 + 源码/OpenAPI/迁移原文 + HTML 逐字 |

## 6. 本次 Findings（临时 ID CX-M019-00N；正式 ID 由 B2 分配）

### CX-M019-001 · 细化 · P1 契约的字段集、错误矩阵与引用路径与现行 DTO/OpenAPI/服务分叉

- 位置：`docs/p1-floor-plan-polygon-api-contract-v0.1.md:45`（`evidence_attachment_ids`）、`:55`（`operator_user_id` 必填）、`:63`、`:72`（同两字段）、`:94`（`FLOOR_PLAN_SHAPE_NOT_FOUND` 404）、`:85-95`（矩阵无 PROJECT_NOT_FOUND / CROSS_PROJECT_FORBIDDEN / CROSS_MERCHANT_FORBIDDEN）、`:130`（`V001__initial_schema.sql`）。
- 事实：DTO 与 OpenAPI（`additionalProperties: false`）均无两字段（§2 探针 2）；后端 0 处 FLOOR_PLAN_SHAPE_NOT_FOUND，未知 shape_id 现行 400 VALIDATION_FAILED「图形不属于该平面图」（`FloorPlanService:261-266`）；M015-002/003 新增三码未入矩阵（探针 3）；引用路径不存在（探针 5）。照文档写请求第一次即 400；照文档处理错误码即错。
- 依据类型：2（DTO/OpenAPI/服务原文）+ 4（探针）。HTML 上位：治理地图 L469/L472、业务地图 L700。判据：GEN-016 / GEN-008 / GEN-002 / GEN-009。四分类：细化。工程缺陷：是。建议入队：是。
- 最小处置：删 §4.1/§4.2 的 `evidence_attachment_ids`、`operator_user_id` 两行（各 2 处）；§6 `FLOOR_PLAN_SHAPE_NOT_FOUND` 行改为「标注不属于该平面图或不存在 | 更新时 `shape_id` 非本图 | 400 | `VALIDATION_FAILED` | 拒绝更新」；补三行现行码（项目不存在 404 PROJECT_NOT_FOUND；越项目 403 CROSS_PROJECT_FORBIDDEN；越商户 403 CROSS_MERCHANT_FORBIDDEN）；§9 引用改为 `V005__p0_platform_master_data.sql`（P0 轻量 `polygon_points` 所在）；标题版本 V0.1→V0.2 + 文首一行变更记录。只改本文件。

### CX-M019-002 · 细化 · P2-A 契约与完成审计的发布/解析规则、示例 id 已被现行实现推翻

- 位置：`docs/p2-floor-plan-import-api-contract-v0.1.md:151`（仅 parsed 允许发布）、`:160`（仅解析 polygon）、`:165`（不在八种内的记入错误明细）、`:56`（示例 id）；`docs/p2-floor-plan-import-completion-audit-v0.1.md:18`（仅 parsed 任务允许发布）。
- 事实：`FloorPlanService:367`（M015-016）parsed|published 均可继续发布剩余 pending；候选循环 `normalizeAllowed` 整单 400（:321，无 try/catch），不生成错误明细；解析器接受闭合 polyline（:79-84）；任务 id 为 `fpi_` + SHA-256 前 32 位（M015-001，:298）。
- 依据类型：2 + 4。HTML 上位：治理地图 L472。判据：GEN-016 / GEN-008。四分类：细化。工程缺陷：是。建议入队：是。
- 最小处置：p2 §3.4 首条改「`parsed` 或 `published` 状态的导入任务允许继续发布，仅 `pending` 候选允许发布；`failed`/`cancelled` 拒绝」；§4.1 首条改「解析 `<polygon>` 与首尾闭合的 `<polyline>`」；`:165` 改「不在八种内的候选整单拒绝（400 VALIDATION_FAILED，不生成任务与错误明细）」；`:56` 示例改为 `fpi_` + 32 位十六进制形式；完成审计 `:18` 同步改写；两文件版本 V0.1→V0.2 + 变更记录一行。只改本模块 2 文件。

## 7. 观察（登记、不入队）

- OBS-1：完成审计 §5 的「33 个测试通过 / 528 个 / 181 个 operation」是无日期的历史快照；文件本身为完成审计记录，不写「当前」；登记不改。
- OBS-2：计划文档全部任务 `[x]`、§5 数据结构与 V034 一致、§3.1「解析 polygon 和 polyline」与解析器一致；历史计划不按现行契约重写。
- OBS-3：p2 §3.1 F-CROSS-10 注与服务 2000/1000 上限一致；p2 §2/§3 事件与权限与 V034/Controller 一致。
- OBS-4：PR #442（2026-09-11）文档部分已在主工作区（d84e419b）、未合并 origin/main；其 PlatformServiceTests +48 属后端模块；本模块检查点只带 4 文档。
- OBS-5：无添加项；无 HTML 修改申请；无 questions；未发现须回 M015/M017 的实现缺陷（文档过时是文档侧）。

## 8. 结论

本席 B1 票：**FAIL**（CX-M019-001/002 为 GEN-016 文档可验证性缺陷且建议入队；只改本模块 3 个文档）。4 文档全文三线审完、58 判据逐族核对、只读探针为证据；未修文、未改 HTML/锁/台账。本报告落盘前未读取 `M019-b1-grok-raw.md` 任何正文。


## M019 B2-r1 整批候选复核（席甲票）

- 时间：2026-09-20T09:47:06Z。输入：modules/M019-b1-grok-raw.md V0.01（40410 bytes，SHA256 32adea173eb78bfcd6f907c1ef3d43ce1089e0a5b6fb1a283b2dccbd09d59646）、modules/M019-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M019 B2-r1 整批候选」段（全文件 SHA256 6c64c3d2ba731d0029a8c4c82221d03a838d4094573afdb224d443bfe3ea39fb）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：Grok GR-001…009 与席甲 CX-001/002 全部归并（CX-001→001/002/003、CX-002→004/005/006；Grok 独有 007/008/009，席甲核源：完成审计数字不可复现且无日期、计划「分页列表」与 M015-007 不符、发布契约无 L472 二次确认句、V034 六值/V423 八值属实）；无漏并、无改写。席甲 B1 漏判的 007/009 与 z_index/svg 默认/未知 shape_id 新建三处细节均经核源成立，如实登记。
- 分类：003/007 技术性（修引用与工程数字）；其余细化（治理地图 L469/L472、业务地图 L700 下位契约文档）；008 不并入 M015-011——011 是代码/OpenAPI/前端 XM，本条是本模块文档缺 HTML 明文句，一句可在本模块内补齐。
- 最小处置：均只改本模块 4 文档，不改代码/OpenAPI/迁移/HTML，不发明业务规则（002 用现行文案；005 示例改派生形态；006 按现行解析器/服务口径；007 加快照说明不删数字；009 一句引用 V423）。
- 无添加项、无 HTML 申请、无 questions；无须升为添加的项。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
