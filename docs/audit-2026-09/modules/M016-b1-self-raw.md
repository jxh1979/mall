CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M016 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票，按控制合同 §4「合取不变、推翻权不对等」处理。2026-09-11T09:11:13Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行，与 sidecar/ledger 顶部逐字相同；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `fe50e3f309c04f81820f8414bd8860325d694098`；工作区 tree `33aaabfd5bdee92d82384777b8d050c0c550b8df`。三份业务 HTML 只读。
- 模块 M016「空间点位、楼层平面图与营业时段／数据库迁移／第1段」，4 文件（计划冻结 164 行），映射 18 判据（GEN-001…018；HTML 判据未映射，HTML 仅作 GEN-006/008 的业务参照）。
- 转入口径：M015-002（project_id 列级 NOT NULL）、M015-013（shape_type CHECK 放宽须新迁移）、M013-004/M014-002（V184 字典活校验）、M009-013/018 与 M015-012（补列＝全局遗漏不在模块内加迁移）。测试库 complex_test 只读探针（三个 COMPLEX_DATABASE_* 变量，不 source env）；M017 Jdbc 集成测试 3/3 实跑。已应用迁移不改写，处置只能新 V 号（当前最高 V422）。
- 顺序：Grok 先派、真实退出后本席才写本报告；本报告落盘前未读取 `M016-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 只读承诺：未修改任何源码/文档/HTML/锁/台账；命令全部只读。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql | 61 | 2828 | 141970f242484accdf99ac37a896ed04762677f5c41d1d4808b2be0ba13815e6 | 全读 |
| backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql | 73 | 3658 | fd9200070b4f4439c58cb32f4a03d6eb7f62b16c47320a57c2645ab2f101a56b | 全读 |
| backend/src/main/resources/db/migration/V184__space_type_dictionary.sql | 24 | 1535 | 238d1529736908fcf3ff5346e0c7af4636361e035cb7629b27f8729b4895cc52 | 全读 |
| backend/src/main/resources/db/migration/V203__spaces_status_check.sql | 6 | 540 | a159a62c1b8f23a82ddb002a6a625870c469a9250001ed51743a8cfcd3cdb83f | 全读 |

## 2. 可执行证据原文（/tmp/m016_b1_run.log）

```
START=2026-09-11T08:54:04Z HEAD=fe50e3f3 origin/main=fe50e3f3 porcelain=1
== 0. four files (lines/bytes/sha)
backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql 61 2828 141970f242484accdf99ac37a896ed04762677f5c41d1d4808b2be0ba13815e6
backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql 73 3658 fd9200070b4f4439c58cb32f4a03d6eb7f62b16c47320a57c2645ab2f101a56b
backend/src/main/resources/db/migration/V184__space_type_dictionary.sql 24 1535 238d1529736908fcf3ff5346e0c7af4636361e035cb7629b27f8729b4895cc52
backend/src/main/resources/db/migration/V203__spaces_status_check.sql 6 540 a159a62c1b8f23a82ddb002a6a625870c469a9250001ed51743a8cfcd3cdb83f
== 1. flyway_schema_history (complex_test, read-only): applied + success
023|p1 floor plan polygons|1247380439|t|2026-09-09
034|p2 floor plan import tasks|1294355959|t|2026-09-09
184|space type dictionary|-1668892912|t|2026-09-09
203|spaces status check|83253196|t|2026-09-09
390|422
== 2. constraints on the six tables (pg_constraint)
spaces|chk_spaces_status|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'unavailable'::character varying])::text[])))
spaces|spaces_pkey|PRIMARY KEY (id)
mall_floor_plans|chk_mall_floor_plans_image_size|CHECK (((image_width > 0) AND (image_height > 0)))
mall_floor_plans|chk_mall_floor_plans_status|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'inactive'::character varying, 'archived'::character varying])::text[])))
mall_floor_plans|mall_floor_plans_pkey|PRIMARY KEY (id)
floor_plan_shapes|chk_floor_plan_shapes_points_array|CHECK (((jsonb_typeof(polygon_points) = 'array'::text) AND (jsonb_array_length(polygon_points) >= 3)))
floor_plan_shapes|chk_floor_plan_shapes_ref_type|CHECK (((ref_type)::text = ANY ((ARRAY['space'::character varying, 'merchant'::character varying, 'project'::character varying, 'amusement_project'::character varying, 'device'::character varying, 'custom'::character varying])::text[])))
floor_plan_shapes|chk_floor_plan_shapes_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying])::text[])))
floor_plan_shapes|chk_floor_plan_shapes_status|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'inactive'::character varying, 'archived'::character varying])::text[])))
floor_plan_shapes|floor_plan_shapes_floor_plan_id_fkey|FOREIGN KEY (floor_plan_id) REFERENCES mall_floor_plans(id) ON DELETE CASCADE
floor_plan_shapes|floor_plan_shapes_pkey|PRIMARY KEY (id)
floor_plan_import_tasks|chk_floor_plan_import_tasks_counts|CHECK (((candidate_count >= 0) AND (error_count >= 0)))
floor_plan_import_tasks|chk_floor_plan_import_tasks_source_format|CHECK (((source_format)::text = ANY ((ARRAY['svg'::character varying, 'standard_json'::character varying])::text[])))
floor_plan_import_tasks|chk_floor_plan_import_tasks_status|CHECK (((status)::text = ANY ((ARRAY['parsed'::character varying, 'failed'::character varying, 'published'::character varying, 'cancelled'::character varying])::text[])))
floor_plan_import_tasks|floor_plan_import_tasks_floor_plan_id_fkey|FOREIGN KEY (floor_plan_id) REFERENCES mall_floor_plans(id) ON DELETE CASCADE
floor_plan_import_tasks|floor_plan_import_tasks_pkey|PRIMARY KEY (id)
floor_plan_import_candidates|chk_floor_plan_import_candidates_no|CHECK ((candidate_no > 0))
floor_plan_import_candidates|chk_floor_plan_import_candidates_points_array|CHECK (((jsonb_typeof(polygon_points) = 'array'::text) AND (jsonb_array_length(polygon_points) >= 3)))
floor_plan_import_candidates|chk_floor_plan_import_candidates_ref_type|CHECK (((ref_type)::text = ANY ((ARRAY['space'::character varying, 'merchant'::character varying, 'project'::character varying, 'amusement_project'::character varying, 'device'::character varying, 'custom'::character varying])::text[])))
floor_plan_import_candidates|chk_floor_plan_import_candidates_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying])::text[])))
floor_plan_import_candidates|chk_floor_plan_import_candidates_status|CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'published'::character varying, 'rejected'::character varying])::text[])))
floor_plan_import_candidates|floor_plan_import_candidates_import_task_id_fkey|FOREIGN KEY (import_task_id) REFERENCES floor_plan_import_tasks(id) ON DELETE CASCADE
floor_plan_import_candidates|floor_plan_import_candidates_pkey|PRIMARY KEY (id)
floor_plan_import_errors|floor_plan_import_errors_import_task_id_fkey|FOREIGN KEY (import_task_id) REFERENCES floor_plan_import_tasks(id) ON DELETE CASCADE
floor_plan_import_errors|floor_plan_import_errors_pkey|PRIMARY KEY (id)
space_type_dictionary|space_type_dictionary_pkey|PRIMARY KEY (id)
space_type_dictionary|space_type_dictionary_type_code_key|UNIQUE (type_code)
== 3. nullability of key columns
mall_floor_plans|merchant_id|YES|
mall_floor_plans|project_id|YES|
mall_floor_plans|status|NO|
space_type_dictionary|status|NO|'active'::character varying
spaces|project_id|YES|
spaces|space_type|NO|
spaces|status|NO|
== 4. space_type_dictionary rows + status values used by code
building|楼栋|10|active
floor|楼层|20|active
zone|区块/区域|30|active
shop|商铺/铺位|40|active
public_area|公共区域|50|active
parking|车位/车库|60|active
booth|摊位/展位|70|active
other|其他|999|active
319:                "SELECT type_code FROM space_type_dictionary WHERE status = 'active' ORDER BY sort_order, type_code",
327:                  FROM space_type_dictionary
338:                INSERT INTO space_type_dictionary (id, type_code, type_name, sort_order, status, created_at, updated_at)
502:                   AND status = 'active'
== 5. spaces.space_type / status live check in code
                "governance.space.manage",
                "space",
                () -> {
                    String spaceName = requireText(request == null ? null : request.spaceName(), "space_name");
                    String spaceType = requireText(request == null ? null : request.spaceType(), "space_type");
                    // 条目3 硬校验：space_type 必须是空间类型字典中处于 active 的取值（前端走下拉、后台可维护字典�
                    if (!repository.listActiveSpaceTypeCodes().contains(spaceType)) {
                        throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED",
                                "铺位类型必须是铺位类型字典中的启用值");
                    }
                    String projectId = trimToNull(request.projectId());
                    if (projectId != null && !repository.projectExists(projectId)) {
                        throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
64:    public static final Set<String> SPACE_STATUSES = Set.of("active", "unavailable");
476:                    if (!repository.listActiveSpaceTypeCodes().contains(spaceType)) {
690:        if (!SPACE_STATUSES.contains(value)) {
== 6. shape_type closed set: CHECK vs backend whitelist vs frontend labels vs HTML L472
backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql:33:    CONSTRAINT chk_floor_plan_shapes_shape_type CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', '
backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql:36:    CONSTRAINT chk_floor_plan_import_candidates_shape_type CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility',
34:    private static final Set<String> SHAPE_TYPES = Set.of("area", "shop", "entrance", "facility", "risk_zone", "route");
35:    private static final Set<String> REF_TYPES = Set.of("space", "merchant", "project", "amusement_project", "device", "custom");
17:const SHAPE_TYPE_LABELS: Record<string, string> = { shop: "商铺", booth: "摊位", public_area: "公共区", corridor: "走道", facility: "设施" };
122:        { label: "类型", value: fpLabel(SHAPE_TYPE_LABELS, candidate.shapeType) },
208:                    <td>{candidate.candidateNo ?? "-"}</td><td>{fpLabel(SHAPE_TYPE_LABELS, candidate.shapeType)}</td>
商场平面图网页导入：在后台上传或导入商场平面布局图，系统解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误后发布到正式平面图�
== 7. dictionary codes vs shape_type CHECK (booth/public_area present in V184, absent in V023/V034)
building,floor,zone,shop,public_area,parking,booth,other
== 8. permission seeds: names now Chinese (V251), grants
floor_plan.import.manage|管理平面图导入和发布候选|high|t
floor_plan.import.view|查看平面图导入任务和候选|medium|f
floor_plan.manage|管理平面图和多边形|high|t
floor_plan.view|查看平面图和多边形|medium|f
193:UPDATE permissions SET permission_name = '管理平面图导入和发布候选' WHERE permission_code = 'floor_plan.import.manage';
194:UPDATE permissions SET permission_name = '查看平面图导入任务和候选' WHERE permission_code = 'floor_plan.import.view';
195:UPDATE permissions SET permission_name = '管理平面图和多边形' WHERE permission_code = 'floor_plan.manage';
196:UPDATE permissions SET permission_name = '查看平面图和多边形' WHERE permission_code = 'floor_plan.view';
role_auditor_internal|floor_plan.import.view
role_auditor_internal|floor_plan.view
role_platform_admin|floor_plan.import.manage
role_platform_admin|floor_plan.import.view
role_platform_admin|floor_plan.manage
role_platform_admin|floor_plan.view
backend/src/main/resources/db/migration/V180__auditor_roles_readonly_seed.sql
backend/src/main/resources/db/migration/V181__outsourced_adjustments_audit_lock.sql
backend/src/main/resources/db/migration/V185__audit_operation_log_query_permissions.sql
== 9. later migrations touching these tables
backend/src/main/resources/db/migration/V052__floorplan_merchant_isolation.sql backend/src/main/resources/db/migration/V256__bar_pos_catalog_category_image.sql backend/src/main/resources/db/migration/V351__bar_void_reason_code_dictionary.sql 
ALTER TABLE mall_floor_plans ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_mall_floor_plans_merchant
    ON mall_floor_plans (merchant_id)
 WHERE merchant_id IS NOT NULL;
== 10. physical delete paths against these tables (CASCADE relevance)
delete_hits=0
== 11. V203 consumer + HTML 铺位租赁状态
            // 6 态（对齐 HTML §十 铺位租赁状态）：不可租(人工 status=unavailable) > 已租(生效合同；service 再判即将到�
            //   > 洽谈中(招商意向占用) > 已退租(曾绑约、现无生效) > 空置
            String contractId = rs.getString("contract_id");
            String status;
            Integer days = null;
            if ("unavailable".equals(rs.getString("space_status"))) {
                status = "unavailable";
            } else if (contractId != null) {
铺位租赁状态空置、洽谈中、已租、即将到期、已退租、不可租
(complex_test spaces rows: 0)
== 12. data-isolation spec / tenant isolation gate mentions
docs/audit-2026-09/plan/module-union.txt
docs/audit-2026-09/plan/modules.md
docs/audit-2026-09/plan/scope.txt
docs/audit-2026-09/plan/untracked.txt
docs/domain-ownership-scope-audit-v0.1.csv
docs/domain-ownership-scope-audit-v0.1.csv:141:floor_plan_import_candidates,inherited,floor_plan_id/task_id,floor_plan_id/task_id -> mall_floor_plans -> project
docs/domain-ownership-scope-audit-v0.1.csv:142:floor_plan_import_errors,inherited,floor_plan_id/task_id,floor_plan_id/task_id -> mall_floor_plans -> project/ven
docs/domain-ownership-scope-audit-v0.1.csv:143:floor_plan_import_tasks,inherited,floor_plan_id/task_id,floor_plan_id/task_id -> mall_floor_plans -> project/venu
docs/domain-ownership-scope-audit-v0.1.csv:144:floor_plan_shapes,inherited,floor_plan_id/task_id,floor_plan_id/task_id -> mall_floor_plans -> project/venue scop
== 13. M017 tests exercising these tables (Jdbc integration)
29:    @Test
52:    @Test
55:        repository.saveFloorPlan(new FloorPlanRecord(
108:    @Test
111:        repository.saveFloorPlan(new FloorPlanRecord(
== 14. HTML + lock sha
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-governance-map-for-operations-
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
END=2026-09-11T08:54:05Z
== 15. M017 Jdbc integration tests against the V023/V034 tables (main worktree, complex_test, three DB vars only) 2026-09-11T08:55:54Z
MVN_EXIT=0
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.364 s -- in com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
END2=2026-09-11T08:56:09Z
```

## 3. 三线审计方法与覆盖

- 对象口径：main HEAD 的 4 份迁移（计划冻结 164 行，实算见 §1）；三份业务 HTML 取 main 正本（BSM V0.14、PGM V0.12、BEQR V0.07）；判据锁 V0.11，M016 映射 18 项（GEN-001…018）。HTML 判据未映射本模块，但 HTML 仍是 GEN-008「模式一致性」与 GEN-006「数据完整性」的业务参照来源（治理地图 L469-472 空间对象与平面图导入候选区域、枚举 L259 铺位租赁状态）。
- 第一性原理线：从 HTML 反推四张迁移应当固定什么闭集与完整性契约，对照 complex_test 实态（`pg_constraint`/`information_schema`/`flyway_schema_history`，只读 SELECT，§2 第 1–4 项）与读写这些表的实现（M015 已闭环的 `FloorPlanService`/`JdbcFloorPlanRepository`、`PlatformService.createSpace/createSpaceType`、`JdbcPlatformRepository.listActiveSpaceTypeCodes/saveSpaceType`、`contract/JdbcContractRepository.spaceRentalStatus`、后续迁移 V052/V251）。
- 顺从式线：18 项判据逐族（§5）。对抗式线：管理员在字典页把 status 填成「Active」或「enabled」（V184 无 CHECK、服务 `trimToDefault(status,"active")` 不校验）；导入含 `data-shape-type="booth"` 的 SVG（CHECK 六值拒绝，与 HTML/字典/前端不一致）；直接 SQL 或未来写路径把 `project_id` 留空（列可空，与 M015 B3 服务契约不一致）；把 spaces.status 写成 NULL（V005 列 NOT NULL，不可能）；重跑四份迁移（`IF NOT EXISTS`/`ON CONFLICT`/`DROP IF EXISTS`，可重跑）；物理删平面图（无代码路径，CASCADE 惰性）。
- 只读依赖追流（不审）：V005 `spaces`/`projects`（M010）、V052（`mall_floor_plans.merchant_id`）、V251（权限名中文化）、V180（`role_auditor_internal` 只读授权）、V261（平台管理员全权限）、`docs/domain-ownership-scope-audit-v0.1.csv`（四张平面图表 inherited scope）、前端 `FloorPlanImportPage.tsx`（`SHAPE_TYPE_LABELS`，M018）、M017 `FloorPlanJdbcIntegrationTests`（§2 第 15 项：3/3 通过，MVN_EXIT=0）。
- 4 文件全文逐行读完（§1）。本席禁止修复；缺陷处置只能是新 V 号迁移（当前最高 V422）。

## 4. 与 HTML / 实现事实的分叉对照（要害）

| 陈述 / 定义 | 事实（§2 探针） | 结论 |
|---|---|---|
| V023:33 / V034:36 `shape_type` CHECK 六值 `area/shop/entrance/facility/risk_zone/route` | 治理地图 L472「系统解析出候选区域（铺位、摊位、公共区）」；V184 字典已有 `booth`「摊位/展位」、`public_area`「公共区域」；前端 `SHAPE_TYPE_LABELS` 已含 booth/public_area（另有 HTML 未提的 corridor）；后端白名单同六值（M015-013 定稿：CHECK 放宽须新迁移，白名单同批） | 闭集遗漏（CX-001，转入 M015-013） |
| V023:3 `mall_floor_plans.project_id VARCHAR(64)` 可空、无 FK | M015-002 定稿并已修：服务层 `project_id` 必填且经 `projects` 存在性校验；列级仍允许 NULL（`is_nullable=YES`）；`spaces.project_id` 同为软引用无 FK（仓内范式，V084 注释） | 列契约落后于服务契约（CX-002，转入 M015-002） |
| V184:8 `status VARCHAR(32) NOT NULL DEFAULT 'active' -- active/disabled` 无 CHECK | `listActiveSpaceTypeCodes` 只取 `status = 'active'`；`createSpaceType` 只 `trimToDefault(request.status(),"active")`，任意字符串直接 upsert；`POST /space-types` 为管理员维护入口 | 状态闭集只在注释里（CX-003） |
| V203：`UPDATE … SET status='active' WHERE status NOT IN ('active','unavailable')` 后加 CHECK 二值 | V005 `spaces.status NOT NULL`（NULL 不可能绕过 CHECK）；`PlatformService.SPACE_STATUSES = {active, unavailable}`；`JdbcContractRepository:459-466` 六态派生只认人工 `unavailable`；枚举 L259 六态；BI S3 拍板注释在文件内 | 一致；脏值归一为 active（可租）是文件内记录的拍板，登记观察 |
| 四份迁移幂等性与已应用状态 | `CREATE TABLE IF NOT EXISTS`/`CREATE INDEX IF NOT EXISTS`/`INSERT … ON CONFLICT`/`DROP CONSTRAINT IF EXISTS` 后 `ADD`；`flyway_schema_history` 023/034/184/203 均 success=t，当前 390 条已应用、最高 V422；M015 B5 mvn 日志 Flyway validated 390 | GEN-014 通过 |
| 权限种子 `floor_plan.view/manage/import.view/import.manage`（V023:46-61、V034:58-73）英文 `permission_name`，高危码 requires_approval=TRUE 直接授 `role_platform_admin` | V251:193-196 已把四条改为中文；现库中文；`role_auditor_internal` 由 V180 获 view 两码；平台管理员授高危码与 V261 全权限口径一致 | 一致（英文种子由 V251 闭合，登记观察） |
| `ON DELETE CASCADE`（shapes/tasks/candidates/errors → 平面图/任务） | Java 中 `DELETE FROM mall_floor_plans/floor_plan_*/spaces` 0 处；平面图三态含 archived（软删口径） | 惰性，登记观察 |
| `floor_plan_import_tasks.status` CHECK 含 `cancelled` | 服务无写 cancelled 路径（M015-014 观察） | 模式多于实现，登记观察 |
| `floor_plan_import_candidates` 无 `UNIQUE (import_task_id, candidate_no)` | id 由任务 id + 序号确定派生（PK 即唯一），且 `idx_…_task (import_task_id, candidate_no, status)` 非唯一 | 登记观察 |
| `spaces.space_type` 无 FK/CHECK 指向字典 | 唯一写路径 `createSpace` 硬校验 active 取值（V184 注释与实现一致）；无 `UPDATE spaces` 路径；软引用为仓内范式 | 一致（登记观察） |
| M017 测试面 | `FloorPlanJdbcIntegrationTests` 3/3 只走合法数据，不断言 CHECK 违例 | GEN-003 无本模块测试面，登记 |

## 5. 18 项判据逐族核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | 四张迁移均有 HTML 上位（空间点位/平面图导入/空间类型/铺位可租态） |
| GEN-002 可执行正确性 | 通过（登记） | SQL 可应用（flyway success）；V203 归一语义按拍板注释；无逻辑错误 |
| GEN-003 测试有效性 | 无本模块测试面（登记） | 迁移无直接测试；M017 Jdbc 集成 3/3 只走合法数据（§2 第 15 项） |
| GEN-004 信任边界 | 通过 | CHECK 覆盖状态/类型/尺寸/顶点数；JSONB 形状校验在应用层（M015） |
| GEN-005 授权 | 通过（登记） | 四权限码 risk_level/requires_approval 与 HTML「发布为高危」一致；高危码授平台管理员与 V261 同口径 |
| GEN-006 数据完整性 | 不通过 | CX-002（project_id 列可空 vs 服务必填）、CX-003（字典 status 无闭集） |
| GEN-007 并发幂等 | 通过 | 种子 `ON CONFLICT`；V203 `DROP IF EXISTS` 再 `ADD`；无并发面 |
| GEN-008 接口与模式一致性 | 不通过 | CX-001（shape_type CHECK 与 HTML/字典/前端不一致）；观察：`cancelled` 无写路径 |
| GEN-009 错误处理 | 无实现面 | 迁移无运行时错误路径 |
| GEN-010 安全 | 通过 | 无秘密；权限种子闭集 |
| GEN-011 审计 | 无实现面 | 表结构不承载审计事件（应用层 M015 已核） |
| GEN-012 资源 | 通过（登记） | 无 DB 级顶点/候选数上限，上限在应用层（1000/2000/200） |
| GEN-013 构建 | 通过 | Flyway 全链应用成功；mvn 集成测试通过 |
| GEN-014 迁移正确性 | 通过 | 四份可重跑、已应用校验和稳定；已应用迁移不改写，处置只能新 V 号（V423 起） |
| GEN-015 中文 | 通过（登记） | 面向操作员的 permission_name 已由 V251 中文化；迁移注释中文 |
| GEN-016 文档可验证性 | 通过（登记） | V184/V203 注明拍板日期与消费者；V023/V034 无头注释（早期迁移惯例） |
| GEN-017 分类 | 通过 | 本席无添加项；三条 finding 均为既有规则/契约的落地缺口 |
| GEN-018 证据 | 通过 | §2 命令原文（psql 只读、grep、mvn） |

## 6. 本次 Findings（临时 ID CX-M016-00N；正式 ID 由 B2 分配）

### CX-M016-001 · 细化（遗漏，转入 M015-013）· shape_type CHECK 闭集缺「摊位」「公共区」

- 位置：`V023__p1_floor_plan_polygons.sql:33` `chk_floor_plan_shapes_shape_type`、`V034__p2_floor_plan_import_tasks.sql:36` `chk_floor_plan_import_candidates_shape_type`，均为六值 `area/shop/entrance/facility/risk_zone/route`。
- 事实：治理地图 L472 候选区域为「铺位、摊位、公共区」；V184 字典已有 `booth`/`public_area`；前端 `SHAPE_TYPE_LABELS` 已含 booth/public_area；M015 定稿 013：CHECK 放宽须新迁移，白名单同批（M015 已闭环，白名单改动作 XM 单独立项）。
- 业务影响：带 `data-shape-type="booth"`/`public_area` 的候选即便放开应用白名单也会被 CHECK 拒绝落库；HTML 明写的三类候选中两类在库层不可表达。
- 判据：GEN-008（依据类型 1 HTML 原文 + 2 可执行产物）。四分类：细化（遗漏）。工程缺陷：是。建议入队：是。
- 最小处置：新迁移 `V423__floor_plan_shape_type_booth_public_area.sql`：两张表 `DROP CONSTRAINT IF EXISTS` 后重建 CHECK 为 `('area','shop','entrance','facility','risk_zone','route','booth','public_area')`（不删既有值、不改数据，可重跑）；同批重生成 data-isolation spec；M015 `SHAPE_TYPES` 白名单与解析默认作 cross_module_observations（XM 单独立项）；前端 `corridor` 不在 HTML，登记 M018。无业务发明：闭集取自 HTML 原句。

### CX-M016-002 · 细化（转入 M015-002）· mall_floor_plans.project_id 列级可空

- 位置：`V023__p1_floor_plan_polygons.sql:3` `project_id VARCHAR(64)`（`is_nullable=YES`）。
- 事实：M015 B3 已使服务层 `project_id` 必填且经 `projects` 存在性校验（PGM-017 项目归属根）；列级仍允许 NULL，直接 SQL/其它写路径可落无归属平面图；`spaces.project_id` 同为软引用无 FK（仓内范式），故不加 FK。
- 业务影响：库层无法保证 M015-002 的归属契约；`findAllFloorPlansByProject` 过滤对 NULL 行不可见（项目隔离闸 BEQR-152 的库层假设）。
- 判据：GEN-006（依据类型 2）。四分类：细化。工程缺陷：是。建议入队：是。
- 最小处置：新迁移 `ALTER TABLE mall_floor_plans ADD CONSTRAINT chk_mall_floor_plans_project_id_required CHECK (project_id IS NOT NULL) NOT VALID`（新写/更新行强制、存量 NULL 行容忍，不发明归属值；可重跑用 `DROP CONSTRAINT IF EXISTS` 前置）；若 complex_test/生产无 NULL 行则可 `VALIDATE CONSTRAINT`（部署期决定）。不加 FK（与 spaces 软引用范式一致）。

### CX-M016-003 · 技术性 · space_type_dictionary.status 无闭集约束

- 位置：`V184__space_type_dictionary.sql:8` `status VARCHAR(32) NOT NULL DEFAULT 'active'   -- active/disabled`。
- 事实：`JdbcPlatformRepository.listActiveSpaceTypeCodes` 只取 `status = 'active'`；`PlatformService.createSpaceType` 对 status 只 `trimToDefault(...,"active")`，任意字符串经 `saveSpaceType` upsert 落库；`POST /space-types` 为管理员维护入口（V184 注释「后台可由管理员维护」）。
- 业务影响：管理员把 status 填成「Active」「enabled」等，该类型静默从下拉与硬校验中消失（既有空间仍持该类型），且无法从库层察觉；与 V203 对 spaces.status 上 CHECK 的防呆口径不一致。
- 判据：GEN-006（依据类型 2；最小反例：`INSERT INTO space_type_dictionary … status='Active'` 成功，随后 `listActiveSpaceTypeCodes` 不含该码）。四分类：技术性（闭集来自 V184 自身注释，不新增状态值）。工程缺陷：是。建议入队：是。
- 最小处置：同一新迁移追加 `ALTER TABLE space_type_dictionary ADD CONSTRAINT chk_space_type_dictionary_status CHECK (status IN ('active','disabled'))`（先 `UPDATE … SET status='disabled' WHERE status NOT IN ('active','disabled')`？——不：存量脏值应先核；complex_test 八行均 active，生产以只读查询确认后再定归一方向，默认按 V203 范式先归一再上 CHECK）；服务侧 status 校验属 M009/M010（PlatformService），XM 登记不在本模块改。

## 7. 观察（登记、不入队）

- OBS-1：V023/V034 权限种子 `permission_name` 为英文，V251:193-196 已改中文；全新部署同一链内闭合，不入队。
- OBS-2：高危码（requires_approval）直接授 `role_platform_admin`（V023:57-61、V034:69-73），与 V261 平台管理员全权限口径一致；`role_auditor_internal` 由 V180 获两只读码。
- OBS-3：`ON DELETE CASCADE` 四处：代码无物理删除路径（archived 软删），惰性；未来若加物理删除会连带删已发布图形证据，届时按 BSM-004…009 口径处理（本模块不映射）。
- OBS-4：`floor_plan_import_tasks.status` CHECK 含 `cancelled`，服务无写路径（M015-014 已登记）。
- OBS-5：`floor_plan_import_candidates` 无 `UNIQUE (import_task_id, candidate_no)`；id 确定派生使 PK 已唯一。
- OBS-6：V203 把所有非法值归一为 `active`（可租）而非 `unavailable`（不可租，fail-closed）；为 BI S3 拍板并在文件内记录；`spaces.status NOT NULL` 使 NULL 不可能绕过 CHECK。
- OBS-7：`spaces.space_type` 对字典无 FK/CHECK；唯一写路径 `createSpace` 硬校验、无 `UPDATE spaces`；软引用为仓内范式（V084）；不入队。
- OBS-8：V023/V034 无头注释（拍板/消费者），V184/V203 有；GEN-016 登记。
- OBS-9：`docs/domain-ownership-scope-audit-v0.1.csv:141-144` 把四张平面图表标为 inherited（经 mall_floor_plans → project）；新迁移落地须 `npm run docs:data-isolation-spec` 同批（predeploy 门禁）。
- OBS-10：无添加项；无 HTML 修改申请；无 questions。

## 8. 结论

本席 B1 票：**FAIL**（CX-M016-001…003 为工程缺陷且建议入队：001/002 转入 M015-013/M015-002 的本模块落地面，003 为 V184 自身闭集缺口）。4 文件全文三线审完、18 判据逐族核对、complex_test 只读实态与 M017 集成测试 3/3 为证据；未修源、未改 HTML/锁/台账。本报告落盘前未读取 `M016-b1-grok-raw.md` 任何正文。


## M016 B2-r1 整批候选复核（席甲票）
版本：V0.02（本文件自身；B1 正文 V0.01 原字节 30020 bytes 保留，本段追加）

- 输入：`modules/M016-b1-self-raw.md` V0.01 前缀 30020 bytes SHA256 59d183dfab0fe56ef32db60afde616207509fac7a602a10396e8e78810a6661a；`modules/M016-b1-grok-raw.md` V0.01 前缀 35809 bytes SHA256 decd1a0138575976de7128cc400fc4b7c26db94de8c54597cb2c3654ca7c0cef（Grok B1：FAIL，GR-M016-001…003，OBS-01…12）；候选 `findings.md`   bytes SHA256 「当前 M016 B2-r1 整批候选」M016-001…004。
- 本段不写独占锁行；锁实算仍为 b012328f…6695a。席位：Claude Fable 5.1（席甲，作者自审票）。本段在 Grok B2-r1 真实退出后、读取其追加段之前落盘。

### 差集与 Grok 独有意见核实（receiving-code-review）

- 根因集合：GR-001↔CX-002、GR-002↔CX-001、GR-003↔CX-003，两席同集，无漏并。
- GR-001 处置分歧：Grok 提 `SET NOT NULL` + `FOREIGN KEY … REFERENCES projects ON DELETE RESTRICT`（引「铁律 8」「V159 口径」）。本席核源：控制合同 V0.17 铁律共 7 条，无「铁律 8」；V159 注释明写「外键一律 NOT VALID + ON DELETE RESTRICT：放行存量、对新写强制」，即 NOT VALID 才是 V159 口径；仓内 20 张带 `project_id` 的表 0 张有 FK（`spaces.project_id` 同为软引用，V084 注释「软引用不强制 FK」）；`projects` 在 complex_test 0 行、M017 夹具用不存在的 `prj_jdbc`。候选取 `CHECK (project_id IS NOT NULL) NOT VALID`、不加 FK：收窄已有列契约（铁律 6 修法②），部署不因存量 NULL 失败，与同域软引用范式一致。
- GR-002：与 CX-001 同；同批改 M015 `SHAPE_TYPES`（M015-013 定稿已授权）；corridor 不入库。接受。
- GR-003：Grok 判「否」但允许「新 V 已开可顺手加」；本批 V423 已因 001/002 打开，顺手加一条 CHECK 是最懒；归一方向取 disabled（脏值此前已不在下拉/硬校验中，保持行为）。候选置「是/技术性」。
- 观察：OBS-02（前端 geojson 默认）、OBS-10（夹具）并入 004；均不入队。

### 逐 ID 票

| ID | 本席票 | 理由 |
|---|---|---|
| 001 | PASS（细化／是） | NOT VALID CHECK 为 V159 已记录范式；不加 FK 与同域软引用一致；先红用例已在干跑中红（空 project_id 未拒） |
| 002 | PASS（细化／是） | 八值取自 HTML L472 + 既有六值；白名单同批经 M015-013 定稿授权；干跑 booth 违反 CHECK 先红 |
| 003 | PASS（技术性／是） | 同一 V423 顺手加，闭集取自 V184 注释；干跑 status='Active' 未拒先红 |
| 004 | PASS（否） | 观察完整登记，无应升项 |

本席 B2-r1 票：**PASS**（队列是＝001–003；否＝004）。B3-r1 边界以 findings  候选节为准。不以本席票覆盖外审。
