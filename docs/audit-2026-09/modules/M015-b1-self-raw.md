CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M015 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票，按控制合同 §4「合取不变、推翻权不对等」处理。2026-09-11T06:15:09Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行，与 sidecar/ledger 顶部逐字相同；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `a041458d06051fed3811d562764cd5900bba6f72`；工作区 tree `2212bc914e44c51244b202e8fdab2e4349c0177f`。三份业务 HTML 只读。
- 模块 M015「空间点位、楼层平面图与营业时段／后台实现／第1段」，23 文件（计划冻结 1999 行），映射 89 判据（BEQR 22、BSM 14、GEN 18、PGM 35）。
- 本模块为后台实现：现有 5 个测试类 43 用例在 160 主工作区实跑通过（§2 末尾附 mvn 摘要；complex_test，仅导出三个 COMPLEX_DATABASE_* 变量）；本报告对象为 main HEAD a041458d（PR #435 已合并）。
- 顺序：Grok 先派、真实退出后本席才写本报告；本报告落盘前未读取 `M015-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 只读承诺：未修改任何源码/文档/HTML/锁/台账；命令全部只读。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java | 95 | 4323 | 33e14ba8868dd24c5235ba55658e51a38c7ff52572d3cb59fd10c07849941bef | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanRecord.java | 21 | 503 | 9db3b6126ab0ca3e624e857684f68e680d7c99f9fe3e0ef6cf8a31080d373bce | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java | 47 | 1987 | af5b478aa351e2e77c60da6f481b938b7df5108f0311c3be15f714ce7551a186 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 627 | 27664 | cd51ee50a1a0c8f54ea0f11b87779fccb033aa347f2493997b9af5c4c60a3e22 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java | 446 | 21684 | f002ed04ccfc7b3b58f67f8cfd8e7abc5d8c87699dc17bebfd73a5933718f54a | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportCandidateRecord.java | 23 | 596 | 7bf4056cf8b920718b377272334b32f7d64c76f329078042f9e77bdedb065a33 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportDetailsResponse.java | 14 | 458 | 5b0011b08d106da9a797ecd6d9915d665460c8823c03c50140b68354a11ae8d5 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportErrorRecord.java | 14 | 295 | 6cfdb0787b4f3b6a4b785106e9f89f26b366d5b25a0fe326f9c9f6032b6b4e92 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParseResult.java | 35 | 912 | 3315458ff08fb345a499a6fba09a296a671e92a6b07a717f8634ce0faab1666b | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 302 | 12287 | 96741bca52bb7f7bf739523331ea6a141554e3d73d1c7f89fd2a8fff95ef5796 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportTaskRecord.java | 19 | 440 | 164d55ea7a81e8099c1ad8f44ef07bd3b3e474dfe7fb9f9d3f69893ac423a270 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanPoint.java | 7 | 112 | cc32d4e81cc6e58813c676f5ebfeeff86e2e48d4ea360ad2183843ffd661b6f5 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanShapeRecord.java | 22 | 546 | 4cfece0c8c5a9f13f2171b0d667eb29c109fe6cc3c9bfdf58590a575607e168f | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanCreateRequest.java | 16 | 383 | 0bc12e7e268a7d8904645c560027f48262a330412cada0f44b0d7da47bef8625 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportCreateRequest.java | 10 | 240 | 7ecd9fca8fab9864815285d809a18375c6532c5036990599d11b12f3a99598a8 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportPublishRequest.java | 13 | 348 | ae890f5f0877b2b3d120d626434b63a0158cffa87f75afa6fb503ef8f2359859 | 全读 |
| backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanShapeSaveRequest.java | 18 | 438 | b890d08f0bfb9e5e1675dc4d2f84a5b21e5f59427b39d59053653e545e07e20d | 全读 |
| backend/src/main/java/com/complex/platform/governance/ProjectRecord.java | 17 | 342 | be4cac0713c505a6e2734af8a0fee0392008b1ae6762e776a8047d8bb5ce95c5 | 全读 |
| backend/src/main/java/com/complex/platform/governance/SpaceRecord.java | 22 | 515 | f13f27eaf2dbdb754f18d4f841f1ebfd515d973e5863624583deab8adac7eac1 | 全读 |
| backend/src/main/java/com/complex/platform/governance/SpaceTypeRecord.java | 15 | 386 | 388191c191993bac3ae452374d001a2236f5245a41257b6b9e2c6e7f649b7bd1 | 全读 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRecord.java | 29 | 1244 | ec04b55b8b3c747b9623450bd7460e00b6a585b2d2d0f8485f80cfc59def6957 | 全读 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java | 90 | 4643 | a3ec4ab2ae792171ed1c16b3a155ce96f38ec827125ac7d823873cb75219aa3e | 全读 |
| backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java | 97 | 4756 | 9d42049d73771cd918dd5890249c6a73b43ff45313964d889bf9fcd65e2766af | 全读 |

## 2. 可执行证据原文（/tmp/m015_b1_run_all.log）

```
START=2026-09-11T05:39:29Z HEAD=685dca4e origin/main=685dca4e
== 1. id derivation from client idempotency key (cross-plan collision) + upsert columns
223:                "fp_" + UUID.randomUUID(),
259:                requestedShapeId == null ? "fps_" + slug(request.idempotencyKey()) : requestedShapeId,
287:        String taskId = "fpi_" + slug(request.idempotencyKey());
307:                    "fpic_" + slug(taskId + "_" + candidateNo),
326:                    "fpie_" + slug(taskId + "_" + errorNo),
384:                    "fps_" + slug(importTaskId + "_" + candidate.id()),
1:84:                ON CONFLICT (id) DO UPDATE SET
5:131:                ON CONFLICT (id) DO UPDATE SET
6:132-                    floor_plan_id = EXCLUDED.floor_plan_id,
9:156:                ON CONFLICT (id) DO UPDATE SET
10:157-                    source_format = EXCLUDED.source_format,
13:202:                ON CONFLICT (id) DO UPDATE SET
17:246:                ON CONFLICT (id) DO UPDATE SET
114:                "saveFloorPlanShape:" + id,
168:                    "createFloorPlanImport:" + id,
199:                    "publishFloorPlanImport:" + taskId,
439:            String operationId,
450:            return idempotencyService.execute(operationId, idempotencyKey, request, action, FloorPlanRecord.class);
466:            String operationId,
== 2. transaction boundaries
floorplan_transactional_hits=0
289:        FloorPlanImportTaskRecord task = repository.saveImportTask(new FloorPlanImportTaskRecord(
310:                    normalizeAllowed(candidate.shapeType(), "shape_type", SHAPE_TYPES),
311:                    normalizeAllowed(candidate.refType(), "ref_type", REF_TYPES),
412:        FloorPlanImportTaskRecord published = repository.saveImportTask(new FloorPlanImportTaskRecord(
== 3. project_id optional
224:                trimToNull(request.projectId()),
3:    project_id VARCHAR(64),
0
== 4. shape types vs HTML 候选区域（铺位、摊位、公共区）
33:    private static final Set<String> SHAPE_TYPES = Set.of("area", "shop", "entrance", "facility", "risk_zone", "route");
34:    private static final Set<String> REF_TYPES = Set.of("space", "merchant", "project", "amusement_project", "device", "custom");
backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql:33:    CONSTRAINT chk_floor_plan_shapes_shape_type CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', '
backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql:36:    CONSTRAINT chk_floor_plan_import_candidates_shape_type CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility',
95:                    defaultText(attributes.get("data-shape-type"), "shop"),
141:                    textOrDefault(shapeNode, "shape_type", "shop"),
商场平面图网页导入：在后台上传或导入商场平面布局图，系统解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误后发布到正式平面图�
== 5. English user-facing messages (parser errors persisted for 人工复核; service messages)
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:36:                    List.of(error(null, "source_payload", "PAYLOAD_
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:44:                    List.of(error(null, "source_format", "UNSUPPORT
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:56:                    List.of(error(null, "viewBox", "VIEWBOX_REQUIRE
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:67:                errors.add(error(shapeNo, "points", "POINTS_REQUIRE
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:74:                errors.add(error(shapeNo, "points", "POINTS_TOO_FEW
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:79:                errors.add(error(shapeNo, "points", "POLYLINE_NOT_C
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:89:                errors.add(error(shapeNo, "points", "POINT_OUT_OF_R
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:106:            errors.add(error(null, "source_payload", "NO_SHAPES_FO
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:121:                    List.of(error(null, "source_payload", "JSON_PA
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:127:                    List.of(error(null, "shapes", "SHAPES_REQUIRED
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:241:            case "POINT_OUT_OF_RANGE" -> "polygon point x/y must b
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:242:            case "POINTS_TOO_FEW" -> "polygon requires at least 3 
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java:243:            default -> "import candidate is invalid";
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:365:            throw ApiException.badRequest("VALIDATION_FAILED", "candida
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:564:            throw ApiException.badRequest("VALIDATION_FAILED", "candida
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java:601:            throw ApiException.badRequest("VALIDATION_FAILED", field + 
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java:50:            throw ApiException.badRequest("VALIDATION_FAILED",
chinese_msgs_in_service=12
== 6. spaces columns vs PGM-023（面积、用途、状态、负责人、所属商户）
id project_id merchant_id parent_space_id space_name space_type floor_no area_square_meter polygon_points status created_at updated_at 
主要功能
维护楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位。
记录空间面积、用途、状态、负责人和所属商户。
779:| M009-013 | GR-M009-008 + 本方OBS-02 | 细化（遗漏） | 是 | 是（B2裁定） | projects即综合体项目/场地实体（compliance:92与analytics:73按projects.id判项目），记录只有merchant_id/venue_type/name/code/floor/capacity/status�
== 7. permissions
48:    ('floor_plan.view', 'View floor plans and polygon shapes', 'floor_plan', 'medium', FALSE, 'active'),
49:    ('floor_plan.manage', 'Manage floor plans and polygon shapes', 'floor_plan', 'high', TRUE, 'active')
60: WHERE permission_code IN ('floor_plan.view', 'floor_plan.manage')
60:    ('floor_plan.import.view', 'View floor plan import tasks and candidates', 'floor_plan', 'medium', FALSE, 'active'),
61:    ('floor_plan.import.manage', 'Manage floor plan imports and publish candidates', 'floor_plan', 'high', TRUE, 'active')
72: WHERE permission_code IN ('floor_plan.import.view', 'floor_plan.import.manage')
== 8. web double confirm for publish
30:const PUBLISH_CONFIRM_TITLE = "发布平面图导入候选";
34: * 人工复核候选与错误后,发布到正式平面图区域。发布=高危动作,经 confirmHighImpactAction 二次确认。
230:                impact={`${PUBLISH_CONFIRM_TITLE}：发布后候选会写入正式平面图区域，请确认已完成业务复核，发
== 9. business day / leasing status elsewhere
backend/src/main/java/com/complex/platform/common/BusinessDayResolver.java
470:                status = "negotiating";
474:                status = "vacant";
V201__business_day_cutoff_config.sql
V226__entitlement_session_kind_batch3.sql
== 10. venue_business_hours schema
22:    CHECK (consumption_window IS NULL OR consumption_window IN ('per_use','per_session','per_day','unlimited_in_day'));
25:    CHECK (consumption_window IS NULL OR consumption_window IN ('per_use','per_session','per_day','unlimited_in_day'));
29:--    is_draft/effective_from 留草稿态与生效锚点(运营编辑先存草稿再发布);唯一键(venue_id,day_of_week)。
30:CREATE TABLE IF NOT EXISTS venue_business_hours (
33:    day_of_week    SMALLINT     NOT NULL CHECK (day_of_week BETWEEN 0 AND 6), -- 0=周日..6=周六(Java DayOfWeek 归一在代码侧)
36:    is_draft       BOOLEAN      NOT NULL DEFAULT FALSE,
40:    CONSTRAINT uq_vbh_venue_dow UNIQUE (venue_id, day_of_week)
45:CREATE TABLE IF NOT EXISTS venue_sessions (
== 11. tests inventory
floorplan/FloorPlanControllerTests: 13 @Test
floorplan/FloorPlanImportParserTests: 4 @Test
floorplan/FloorPlanJdbcIntegrationTests: 3 @Test
floorplan/FloorPlanServiceTests: 18 @Test
governance/VenueBusinessHoursServiceTests: 6 @Test
274:    void saveShapeRejectsShapeIdBelongingToDifferentFloorPlan() {
315:    void createFloorPlanWithSameKeyAcrossMerchantsDoesNotOverwrite() {
== 12. HTML sha
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-
END=2026-09-11T05:39:29Z
START=2026-09-11T05:37:40Z TESTS=FloorPlanControllerTests,FloorPlanImportParserTests,FloorPlanJdbcIntegrationTests,FloorPlanServiceTests,VenueBusinessHoursServiceTests HEAD=685dca4e
env: URL=jdbc:postgresql://127.0.0.1:5432/complex_test USER=set PROFILES=unset
MVN_EXIT=0
target/surefire-reports/com.complex.platform.floorplan.FloorPlanControllerTests.txt:Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.726 s -- in com.complex.platform.floorplan.FloorPlanControllerTests
target/surefire-reports/com.complex.platform.floorplan.FloorPlanImportParserTests.txt:Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.145 s -- in com.complex.platform.floorplan.FloorPlanImportParserTests
target/surefire-reports/com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests.txt:Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.140 s -- in com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
target/surefire-reports/com.complex.platform.floorplan.FloorPlanServiceTests.txt:Tests run: 18, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.058 s -- in com.complex.platform.floorplan.FloorPlanServiceTests
target/surefire-reports/com.complex.platform.governance.VenueBusinessHoursServiceTests.txt:Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.043 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
=== failures
END=2026-09-11T05:37:53Z
```

## 3. 三线审计方法与覆盖

- 对象口径：main HEAD 的 23 文件（floorplan 包 17 + governance 包 6，计划冻结 1999 行，实算见 §1）；三份业务 HTML 取 main 正本（BSM V0.14、PGM V0.12、BEQR V0.07）；判据锁 V0.11，M015 映射 89 项（BEQR 22、BSM 14、GEN 18、PGM 35）。
- 第一性原理线：从治理地图「空间和点位」（PGM-022/023：维护九类空间对象、记录面积/用途/状态/负责人/所属商户）与「商场平面图网页导入…发布为高危动作，须二次确认——不可静默覆盖既有布局证据」、枚举「铺位租赁状态」、业务地图/枚举「营业日」与场次开放时间反推后台应保证什么；对照本模块 Controller/Service/Repository/Parser 与迁移 V005/V023/V034/V226 的实际行为。
- 顺从式线：89 项判据逐族对照（§5）。对抗式线：客户端幂等键跨平面图复用、导入候选含非白名单类型、project_id 缺省、英文错误文本落库后给复核人看、跨商户访问、越界坐标与超限 payload、失败路径的事务边界。
- 只读依赖追流：`IdempotencyService`（`runAndComplete:169-171` 在有事务管理器时以 `TransactionTemplate` 包裹 action+complete）、`AuthorizationService.currentMerchantScope`、`AuditEventService`、V005 `spaces`、V023/V034 表与 CHECK、V226 `venue_business_hours`、V201 `business_day_cutoff_config` 与 `BusinessDayResolver`、`contract/JdbcContractRepository`（铺位租赁状态）、前端 `FloorPlanImportPage.tsx`（二次确认弹窗）。
- 23 文件全文逐行读完（§1）；现有 5 个测试类在 160 主工作区实跑（§2：`mvn -o -q -Dtest=…`，complex_test，仅导出三个 COMPLEX_DATABASE_* 变量）——43/43 通过、MVN_EXIT=0。本席禁止修复。

## 4. 与 HTML / 实现事实的分叉对照（要害）

| 陈述 / 行为 | 事实（§2 探针） | 结论 |
|---|---|---|
| `saveShapeOnce`：新图形 ID = `"fps_" + slug(idempotencyKey)`（`FloorPlanService:259`）；`createImportTaskOnce`：任务 ID = `"fpi_" + slug(idempotencyKey)`（`:287`）；幂等操作键却按平面图分域（`"saveFloorPlanShape:" + id`、`"createFloorPlanImport:" + id`） | `JdbcFloorPlanRepository.saveShape` ON CONFLICT (id) DO UPDATE **含** `floor_plan_id = EXCLUDED.floor_plan_id`（:131-140）；`saveImportTask` ON CONFLICT DO UPDATE **不含** floor_plan_id（:156-165）→ 平面图 B 用与平面图 A 相同的客户端键：图形被整体改挂到 B 并覆盖几何；导入任务留在 A 但 payload/状态/计数被 B 覆盖，候选 `fpic_<task>_<n>` 同步覆写 | 跨平面图静默覆盖既有布局证据（CX-001） |
| `createFloorPlanOnce` `trimToNull(request.projectId())`（:224）；V023 `mall_floor_plans.project_id VARCHAR(64)` 可空；`FloorPlanRepository` 无项目存在性查询 | PGM-017「保证所有对象带综合体项目归属」、PGM-022「所有业务都要知道发生在哪」；M009-013/M014-001 口径：项目是空间树归属根 | 平面图可无项目归属（CX-002） |
| `FloorPlanImportParser` 12 处错误文本与 `FloorPlanService:365/564/601` 校验文本为英文（`"points attribute is required"`、`"polyline must be closed before import"`、`"candidate_ids is required"`、`field + " is invalid"` 等） | 解析错误按 `floor_plan_import_errors.error_message` 落库并在候选详情返回给「人工复核候选与错误」的操作员；PGM-001 界面全中文、GEN-015；同文件另有 12 条中文校验消息 | 面向操作员的英文文本（CX-003） |
| `SHAPE_TYPES = area/shop/entrance/facility/risk_zone/route`（:33）；V023/V034 CHECK 同六值；解析默认 `shop`；候选类型在落库循环内 `normalizeAllowed` | PGM L472「系统解析出候选区域（铺位、摊位、公共区）」——摊位、公共区无对应类型；带 `data-shape-type="booth"` 的 SVG 整单 VALIDATION_FAILED 而非记入错误清单 | 候选类型遗漏（CX-004，需迁移，XM→M016） |
| `spaces` 列：id/project_id/merchant_id/parent_space_id/space_name/space_type/floor_no/area_square_meter/polygon_points/status（V005，无后续 ADD COLUMN） | PGM-023「记录空间面积、用途、状态、负责人和所属商户」——无「用途」「负责人」列；`SpaceRecord` 同 | 全局遗漏（CX-005，同 M009-013 口径） |
| 多行落库的事务边界（`createImportTaskOnce:283-335`、`publishImportTaskOnce:382-425`；floorplan 包 0 处 `@Transactional`） | `IdempotencyService.runAndComplete:169-171`：注入 `PlatformTransactionManager` 时 `transactionTemplate.execute(...)` 包裹 action+complete，失败留痕在外落——生产路径无半写 | 一致（观察 OBS-2） |
| 发布高危二次确认 | 后端：`floor_plan.import.manage` risk_level=high、requires_approval=TRUE（V034）；发布只新建 `fps_<task>_<candidate>` 图形，不删既有图形；前端 `FloorPlanImportPage.tsx:30/230` `confirmHighImpactAction` 弹窗承载二次确认 | 一致（观察） |
| 权限码 `floor_plan.view/manage/import.view/import.manage` | V023/V034 种子存在；Controller 逐端点 `requirePermission`；跨商户 `CROSS_MERCHANT_FORBIDDEN` | 一致 |
| 导入上限 1_000_000 字符 / 2000 形状 / 1000 顶点；坐标 0–1 归一化校验 | `enforceImportLimits`、`validateShapeRequest` | 一致（GEN-004/GEN-012） |
| 场地营业时间 `venue_business_hours`（V226：per venue × 星期，`is_draft`） | 服务把 `is_draft` 复用为「停用」（V226 注释为草稿/生效锚点）；开歇同刻拒绝、跨零点允许；审计事件落 `venue_business_hours.updated` | 语义复用已在记录注释说明（观察）；HTML「营业日」安全阈值/最高权限审批属 V201 `business_day_cutoff_config`（非本模块） |
| 铺位租赁状态（BEQR-170） | `contract/JdbcContractRepository:460-474` 派生 negotiating/vacant…（非本模块文件） | 本模块无实现面 |
| 测试 | `FloorPlanControllerTests` 12、`FloorPlanImportParserTests` 4、`FloorPlanJdbcIntegrationTests` 3、`FloorPlanServiceTests` 18、`VenueBusinessHoursServiceTests` 6：43/43 通过；`createFloorPlanWithSameKeyAcrossMerchantsDoesNotOverwrite` 只覆盖平面图（UUID），图形/导入任务跨图同键无用例 | 通过但缺 CX-001/002 的可失败用例 |

## 5. 89 项判据逐族核对

| 判据族 | 结论 | 依据 |
|---|---|---|
| BEQR-143…148/149/151/152/157/159/166/189、BSM-028/029/072/074/114/115/120、PGM-002…004/006…021/026/027/031/035/040…044/048/060/065/066 | 无实现面或通过（登记） | 本模块只承载空间/平面图/营业时间后台，不涉主体、合同、员工、权限规则本体；`projects.capacity`（只按项目实时在玩人数控制容量）与 PGM-017 同向 |
| BEQR-150/153/154/155/156/158 | 无实现面 | 终端、项目/商户/合同/设备状态闭集不在本包 |
| BEQR-063/074 | 无实现面 | 高危动作/隐私桌台不在本包 |
| BEQR-170 铺位租赁状态 | 无实现面（登记） | 派生在 contract 仓 |
| BSM-001/BSM-003/PGM-001 | 不通过 | CX-001（数据可被他图静默覆盖，与「数据可溯源」相反）、CX-003（面向操作员英文） |
| BSM-004/005/006/007/009 | 通过（登记） | 平面图三态含 archived；无物理删除 |
| PGM-005/022/023 | 不通过 | CX-002（无项目归属）、CX-004（候选区域类型）、CX-005（用途/负责人） |
| GEN-001 必要性 | 通过 | 平面图导入/发布、空间记录、场次营业时间均有 HTML 上位 |
| GEN-002 可执行正确性 | 不通过 | CX-001 |
| GEN-003 测试有效性 | 通过（登记缺口） | 43/43 实跑通过；缺跨图同键与 project_id 必填的可失败用例（随 CX-001/002 补） |
| GEN-004 信任边界 | 通过（登记） | 坐标范围、payload/形状/顶点上限、格式白名单；project_id 未校验存在见 CX-002 |
| GEN-005 授权 | 通过 | 逐端点权限码；高危码 requires_approval；商户 scope |
| GEN-006 数据完整性 | 不通过 | CX-001（跨图覆盖） |
| GEN-007 并发幂等 | 不通过 | CX-001（幂等键作全局主键） |
| GEN-008 接口一致性 | 通过（登记） | 白名单与 CHECK 一致；`SpaceRecord.polygonPoints` 为 `List<Map>`（观察） |
| GEN-009 错误处理 | 通过（登记） | 失败路径经 IdempotencyService 事务回滚并 `recordFailure`；非白名单类型整单拒绝而非错误清单（观察，并入 CX-004） |
| GEN-010 安全 | 通过 | 无秘密；SVG 以正则解析且受长度上限 |
| GEN-011 审计 | 通过 | 每个读/写路径 `recordSuccess/recordFailure` |
| GEN-012 资源 | 通过 | 上限见 §4 |
| GEN-013 构建 | 通过 | mvn 可编译运行 |
| GEN-014 迁移 | 无实现面 | 本模块无 SQL（迁移归 M016） |
| GEN-015 中文 | 不通过 | CX-003 |
| GEN-016 文档 | 通过（登记） | 类注释与 V226 注释对 `is_draft` 语义不一致（观察） |
| GEN-017 分类 | 通过 | 无添加项 |
| GEN-018 证据 | 通过 | §2 命令与测试原文 |

## 6. 本次 Findings（临时 ID CX-M015-00N；正式 ID 由 B2 分配）

### CX-M015-001 · 技术性 · 图形与导入任务 ID 由客户端幂等键派生，跨平面图同键静默覆盖既有布局证据

- 位置：`FloorPlanService.java:259`（`"fps_" + slug(request.idempotencyKey())`）、`:287`（`"fpi_" + slug(request.idempotencyKey())`）；`JdbcFloorPlanRepository.java:131-140`（图形 upsert 改写 floor_plan_id）、`:156-165`（任务 upsert 不改写 floor_plan_id）。
- 事实：幂等操作键按平面图分域，但主键只含客户端键；平面图 B 重用 A 的键（如 `shape-1`）即改挂/覆盖 A 的图形；导入任务同理覆盖 A 的 payload/状态并覆写候选 `fpic_<task>_<n>`。首次正常使用（两位操作员各自从 1 编号）即可发生。
- 判据：GEN-006/GEN-007/GEN-002/BSM-001（数据可溯源）/PGM-022「不可静默覆盖既有布局证据」；依据类型 4。工程缺陷：是。建议入队：是。最小处置：主键加入平面图域——`"fps_" + slug(floorPlanId + "_" + key)`、`"fpi_" + slug(floorPlanId + "_" + key)`（候选/错误 ID 随任务 ID 变化）；先写跨图同键不覆盖的可失败测试（图形、导入任务各一）再改。

### CX-M015-002 · 细化 · 平面图可不带综合体项目归属

- 位置：`FloorPlanService.java:224`（`trimToNull(request.projectId())`）；V023 `mall_floor_plans.project_id` 可空；`FloorPlanRepository` 无项目存在性查询。
- HTML：PGM-017「保证所有对象带综合体项目归属」、PGM-022「所有业务都要知道发生在哪」；M014-001 定稿：综合体项目是空间树归属根。
- 判据：PGM-017/PGM-022/GEN-004；依据类型 1+4。工程缺陷：是。建议入队：是。最小处置：`validateCreateRequest` 要求 `project_id` 非空并校验 `projects` 存在（`FloorPlanRepository.projectExists` 一条 SELECT，缺失抛 `PROJECT_NOT_FOUND`）；列级 NOT NULL 归 M016（XM 登记）；先写「缺 project_id / 项目不存在被拒」的可失败测试。

### CX-M015-003 · 技术性 · 面向操作员的英文错误文本

- 位置：`FloorPlanImportParser.java:36/44/56/67/74/79/89/106/121/127/241-243`（落库到 `floor_plan_import_errors.error_message`，候选详情原样返回给复核人）；`FloorPlanService.java:365/564/601`（`candidate_ids is required`、`<field> is invalid`）；`VenueBusinessHoursService.java:50`（`venue_id is required`）。
- 判据：GEN-015/PGM-001；依据类型 4。工程缺陷：是。建议入队：是。最小处置：文本改中文（错误码不变），测试夹具/断言若含英文随改；不改错误码与结构；`Strings.requireText` 的通用英文文案在 common 包（非本模块，登记）。

### CX-M015-004 · 细化（遗漏，跨模块）· 候选区域类型无「摊位」「公共区」

- 位置：`FloorPlanService.java:33` 白名单六值；V023:33 / V034:36 CHECK 同六值；解析默认 `shop`；非白名单类型整单 VALIDATION_FAILED。
- HTML：PGM L472「系统解析出候选区域（铺位、摊位、公共区）」。
- 判据：PGM-022/GEN-008；依据类型 1+4。工程缺陷：是。建议入队：否（XM→M016：需新迁移放宽两处 CHECK 并同批扩白名单为含 booth/public_area；本模块不加迁移）。

### CX-M015-005 · 细化（遗漏，全局）· 空间无「用途」「负责人」记录面

- 位置：V005 `spaces` 列集；`SpaceRecord.java`。
- HTML：PGM-023「记录空间面积、用途、状态、负责人和所属商户」。
- 判据：PGM-023/GEN-008；依据类型 1+4。工程缺陷：是。建议入队：否（全局遗漏登记，与 M009-013/018 同口径：补列属新建能力，迁移归 M016/M010 目录）。

## 7. 观察（登记、不入队）

- OBS-1：发布二次确认由前端 `FloorPlanImportPage.tsx` `confirmHighImpactAction` 承载（`:30/:230`），后端以 `floor_plan.import.manage` high/requires_approval 与「只新建不删除」满足「不可静默覆盖」（CX-001 除外）。
- OBS-2：floorplan 包无 `@Transactional`，但 `IdempotencyService.runAndComplete:169-171` 在注入事务管理器时以 `TransactionTemplate` 包裹 action+complete，失败留痕在外落——导入/发布多行写入在生产路径原子；单测 `new IdempotencyService(...)` 不注入时为直跑（测试形态，不升）。
- OBS-3：`VenueBusinessHoursService` 把 V226 `is_draft` 复用为「停用」，与迁移注释「草稿态/生效锚点」语义漂移，记录类注释已说明；`effectiveFrom` 存而不校验。HTML「营业日」起止/上下限/最高权限审批口径属 V201 营业日配置，非本模块。
- OBS-4：`FloorPlanRepository.findAllFloorPlansByProject` 零调用（注释说明为隔离门禁保留）；`listFloorPlans` 返回 `PageResponse(records,1,50,size)` 不分页；`SpaceRecord.polygonPoints` 为 `List<Map>` 而平面图为 `FloorPlanPoint`。
- OBS-5：铺位租赁状态（BEQR-170）在 `JdbcContractRepository` 派生；本模块无实现面。
- OBS-6：现有 43 个测试全部通过（§2），`createFloorPlanWithSameKeyAcrossMerchantsDoesNotOverwrite` 已覆盖平面图创建（UUID 主键），图形/导入任务未覆盖（CX-001）。
- 技能：`verification-before-completion`（mvn 实跑与探针原文附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。23 文件全文三线审计、89 项判据逐族核对、5 个测试类 43/43 实跑；5 项候选（技术性 2、细化 3，其中 2 项为跨模块/全局遗漏建议「否」）；无添加项；三份 HTML 无需人闸修改；未修源、未提交/推送。本报告落盘前未读取 `M015-b1-grok-raw.md` 任何正文。


## M015 B2-r1 整批候选复核（席甲票）+ B1 勘误
版本：V0.02（本文件自身；B1 正文 V0.01 原字节 31125 bytes 保留，本段追加）

- 输入：`modules/M015-b1-self-raw.md` V0.01 前缀 31125 bytes SHA256 0f51db18a789833b1c056e7f0fbcb94016169cf65372ec2dc1078d21c9417f34；`modules/M015-b1-grok-raw.md` V0.01 前缀 43052 bytes SHA256 649f4b190d5d598b99e76d91b91a62aee7e861e22444d0b4e3ec2b433c41ca98（Grok B1：FAIL，GR-M015-001…012，OBS-01…13）；候选 `findings.md` V1.91 766855 bytes SHA256 03bbb2096a56f46e1bb6dfdf1f338d5ebe01601545175c6b65cb50db21808088「当前 M015 B2-r1 整批候选」M015-001…014。
- 本段不写独占锁行（B2 规约）；锁实算仍为 b012328f…6695a。席位：Claude Fable 5.1（席甲，作者自审票）。本段在 Grok B2-r1 真实退出后、读取其追加段之前落盘。

### 勘误（席甲 B1 §0/§3/§5）

- 判据计数：本席 B1 写「89 判据（PGM 35）」；锁映射行 `M015 → [...]` 实算 90 个唯一 ID（PGM 36：002–021 共 20 项 + 022/023/026/027/031/035/040–044/048/060/065/066 共 16 项）。Grok B1 §1 已实算 90，以 90 为准；§5 判据族核对未因此漏族（PGM-022/023 已在族内核对）。

### 差集与 Grok 独有项核实（receiving-code-review，§2 探针 + 本批复核）

- GR-002/004：`AuthorizationService.currentProjectScopeQuietly:510-515` 为项目员工返回项目 ID，仓内 ServiceOrder/Finance/Loyalty/HighValue 服务已用；`FloorPlanService:65-69` 只分商户 scope，`findAllFloorPlansByProject` 零调用；`VenueBusinessHoursRepository.listAll` 无 WHERE、`PlatformController` 只 `requirePlatformScope()`。接受，003/009 细化。
- GR-006：`FieldLabels:279/413` candidate_ids→「招商对象」、plan_name→「方案名称」；本模块出网必错译。接受，005。
- GR-008/009：伪分页与 candidate_ids 无上限（契约 maxItems 200）核实。接受，007/008 技术性。
- GR-011：venue＝spaces.id（前端 `VenueBusinessHoursPage` 读 `/api/v1/platform/spaces` 取场地选项），服务不校验存在。接受，并入 009。
- GR-012：`FloorPlanService:517/535` 与 `VenueBusinessHoursService:50/81/86` 夹带英文键。接受，006。
- GR-003：BSM L521「所有涉及钱的配置一律版本化…先存为草案、经高敏权限转正审批通过后按指定日期生效，不即改即生效」「时刻…档位取值这类策略参数」「无法判断是否涉及钱的一律按涉及钱处理」——开歇业时刻决定场次与入场资格核销，属该原则；V226 `is_draft/effective_from` 本为草稿/生效锚点；结算配置中心已有「落草案→差人复核转正」范式（`FinanceService:1602/732`）。冲突成立。但转正端点在 `PlatformController`、起草人列需迁移、前端与核销侧（只读 `is_draft=FALSE`、不读 `effective_from`）均在模块外——本模块单独把 save 改草案会使现网配置永不能生效。本席置 010「冲突／否（XM 同批）」，交 Grok 裁定。
- GR-001：`FloorPlanImportPublishRequest` 无确认字段；OpenAPI 该 schema `additionalProperties:false`；前端 `HighRiskConfirm` 弹窗承载二次确认。后端单独加必填字段会使现网发布 400 且违反契约。本席置 011「细化／否（XM 同批）」，交 Grok 裁定。
- GR-007：与 CX-005 同根（spaces 无用途/负责人），按 M009-013/018 口径「否（全局遗漏）」，012。
- 席甲独有 001（跨图同键覆盖）：Grok B1 未发现；事实以 `FloorPlanService:259/287` 与 `JdbcFloorPlanRepository:131-140/156-165` 类型 4 依据成立，B3 干跑已验证 RED（4 例失败）→GREEN（47/47）。
- 差集：GR-001…012 → 011/003/010/009/004/005/012/007/008/002/009/006；OBS-01…13 → 014（OBS-04/05 并 013）；CX-001…005 → 001/002/004/013/012。无遗漏、无改写 Grok 原意。

### 逐 ID 票

| ID | 本席票 | 理由 |
|---|---|---|
| 001 | PASS（技术性／是） | 主键含平面图域即闭合，不发明业务；先红后绿 |
| 002 | PASS（细化／是） | 必填 + 存在性一条 SELECT；NOT NULL 归 M016 |
| 003 | PASS（细化／是） | 复用既有项目 scope 通道与零调用的仓储方法 |
| 004 | PASS（细化／是） | 文案改中文，错误码不变 |
| 005 | PASS（细化／是） | 本模块抛中文整句，不改 FieldLabels |
| 006 | PASS（细化／是） | 去英文键 |
| 007 | PASS（技术性／是） | 复用 `PageResponse.of` |
| 008 | PASS（技术性／是） | 与契约一致的上限 |
| 009 | PASS（细化／是） | 场地存在性 + 项目谓词，服务注入 AuthorizationService |
| 010 | PASS（冲突／否，XM 同批） | 理由见上；若外审裁定须「是」，r2 按其处置修订 |
| 011 | PASS（细化／否，XM 同批） | 同上 |
| 012 | PASS（否） | 全局遗漏口径 |
| 013 | PASS（否，XM→M016/M018） | CHECK 与前端契约同批 |
| 014 | PASS（否） | 含本席判据计数勘误 |

本席 B2-r1 票：**PASS**（队列是＝001–009；否＝010–014）。B3-r1 边界：只改 `FloorPlanService/FloorPlanRepository/JdbcFloorPlanRepository/FloorPlanImportParser/VenueBusinessHoursService/VenueBusinessHoursRepository` 与 `FloorPlanServiceTests/VenueBusinessHoursServiceTests/FloorPlanJdbcIntegrationTests`（各加「// 文件版本：V0.01」），TDD 先红后绿，不改迁移/OpenAPI/三份 HTML/锁/PlatformController/前端。不以本席票覆盖外审；外审 FAIL 则按其意见修订为 r2 候选。


## M015 B2-r2 修订候选复核（席甲票）
版本：V0.03（本文件自身；前 36670 bytes 原字节保留，本段追加）

- 输入：`modules/M015-b1-grok-raw.md` V0.02 前缀 67177 bytes SHA256 418ba0b106e22b8708c514dd5b135532839f19a72b4d64a1476120e4a0655a73（Grok B2-r1：FAIL，否决点 007，其余 001–006、008–014 PASS）；r2 候选 `findings.md` V1.92 771076 bytes SHA256 dcfec423ffb55e3027d5cdd2110343170f472ba47d190c458c62a57d677baded「当前 M015 B2-r2 修订候选」＋「当前 M015 B2-r1 整批候选」原表。
- 本段不写独占锁行；锁实算仍为 b012328f…6695a。席位：Claude Fable 5.1（席甲，作者自审票）。本段在 Grok B2-r2 真实退出后、读取其追加段之前落盘。

### 对 Grok B2-r1 否决点的核实与修订（receiving-code-review）

- 007：本席复核 `PageResponse.java:16-21`——`of(all,page,size)` 以 `subList(from,to)` 按 size 切片，`of(records,1,500)` 在 `listShapes` 超过 500 行（`MAX_IMPORT_CANDIDATES=2000`）时 items 只剩前 500，且 `FloorPlanController` 三个列表端点无 page/size 参数，多出的图形不可达。本席 r1 对 007 的 PASS 票错误，撤回。r2 改三处 `new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size())`（诚实不分页：pageSize=total=真实条数、items 全量；空列表 pageSize=1 与 `of()` 的 safeSize 同约定），不硬编码 500、不新增分页参数；TDD 先加 `listShapesReportsHonestPageSizeAndTotal` 一例（现网 pageSize 谎报 50 即红）。接受。
- 非阻断：001「不可静默覆盖既有布局证据」属 PGM-023 L472（发布正式区域）而非 PGM-022，判据列改 GEN-006/GEN-007/GEN-002/BSM-001；010 V226 在 M247、转正端点在 M009、不新加起草人列（GR-003 原处置无此要求）；012 V005 在 M010；014 补 `VenueBusinessHoursService.save` 靠 V226 UNIQUE last-write-wins；003 商户 scope 优先、平台管理员桥接为空、只在商户 scope 为空时查项目 scope（`currentProjectScopeQuietly` 对无 organizationId 抛 PROJECT_SCOPE_REQUIRED）已写入 B3 边界。均接受。

### 逐 ID 票（r2）

| ID | 本席票 | 理由 |
|---|---|---|
| 007 | PASS（技术性／是） | 诚实不分页，不丢数据、不新增参数、不硬编码 |
| 001 / 010 / 012 / 014 | PASS（分类／队列不变） | 仅判据列或事实栏勘误，处置不变 |
| 002–006、008、009、011、013 | PASS（r2 未变） | 与 r1 票相同 |

本席 B2-r2 票：**PASS**（队列是＝001–009；否＝010–014）。B3-r1 边界以 findings V1.92 r2 节 + r1 原表为准。不以本席票覆盖外审。


## M015 B2-r3 整批候选复核（席甲票）

- 时间：2026-09-20T04:53:51Z。输入：modules/M015-b5-r1-grok-raw.md V0.02 追加段（全文 73711 bytes，SHA256 837327dbb73b46d13675de525863593d8e87fadc12ab8a98d65e99e40bb104a6）、modules/M015-b5-r1-self-raw.md V0.03+V0.04（全文 91043 bytes，SHA256 fea086dc5d05969ddea74705786e0d4c718425bbe5a68ae0f66a0f4579cda291）；候选 findings.md 顶部「当前 M015 B2-r3 整批候选」段（全文件 SHA256 866e95e4b6109438228505843bcedfea02eb54834aebe7eecbbc1e1f33cf53fa）。本票落盘前未读 Grok B2-r3 追加段。不写独占锁行。
- 差集：Grok 两条 GR-M015-B5r1-01/02 ↔ 候选 015/016 一一对应；席甲撤回段无第三条；无漏并、无改写。
- 015：分类细化（BEQR-152「所有业务数据默认按项目隔离…只有项目之上的获授权岗位才可以跨项目汇总」、BSM-007「平台管理员拥有全部日常功能权限」的下位实现错误）；工程缺陷成立（`AuthorizationService.java:504-507/289-294` 原文 + M006 回归 `:150`）；最小处置只在两服务内加 `projectScopeOrAll()` 并改五处调用、测试桩改生产语义各加一例先红，不碰 `AuthorizationService`（红线④）/Controller/OpenAPI/HTML；队列是。同意。
- 016：分类细化（PGM-023 L472「人工复核候选与错误后发布」的下位状态机）；工程缺陷成立（`FloorPlanService.java:365-367/422-435`）；最小处置为前置放宽到 parsed/published + 文案 + 一例先红，不新增候选状态、不改 DTO；同意。
- 001–014 不动，不回潮；无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r3 票：**PASS**（整批候选同意）。
