CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M016 B5-r1 修复后全模块重审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-11T09:57:36Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `fe50e3f309c04f81820f8414bd8860325d694098`；工作区 tree `611d3c4a5bd555a18ab0206790396a351a2ce6a2`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 模块 M016「空间点位、楼层平面图与营业时段／数据库迁移／第1段」，4 文件（计划冻结 164 行；已应用迁移零改写）+ 本批新增 V423__m016_floor_plan_and_space_type_checks.sql，映射 18 判据（GEN-001…018）。步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线重审）。

- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `M016-b5-r1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：PR #436（M015 闭环）已合并，HEAD=origin/main=fe50e3f3；本模块 B3-r1 修复（V423 + M015 白名单同批 + M017 先红用例 + 门禁产物再生成）在工作区未提交（after tree 71f1468b8bf3bc19d5624d92bcebd5a3edd99cde）；complex_test 已随测试启动升到 V423。
- 只读依赖追流（不审）：V005/V052/V159/V251/V180/V261 迁移、`PlatformService.createSpace/createSpaceType`、`JdbcPlatformRepository`、`JdbcContractRepository.spaceRentalStatus`、前端 `FloorPlanImportPage.tsx`、三份 HTML 现文；B5 实跑 6 个测试类与 complex_test 只读探针。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql | 61 | 2828 | 141970f242484accdf99ac37a896ed04762677f5c41d1d4808b2be0ba13815e6 | 全读 |
| backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql | 73 | 3658 | fd9200070b4f4439c58cb32f4a03d6eb7f62b16c47320a57c2645ab2f101a56b | 全读 |
| backend/src/main/resources/db/migration/V184__space_type_dictionary.sql | 24 | 1535 | 238d1529736908fcf3ff5346e0c7af4636361e035cb7629b27f8729b4895cc52 | 全读 |
| backend/src/main/resources/db/migration/V203__spaces_status_check.sql | 6 | 540 | a159a62c1b8f23a82ddb002a6a625870c469a9250001ed51743a8cfcd3cdb83f | 全读 |
| backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql | 22 | 2099 | b9912e89df22d03b54305a48af70052c53a58563df8212be0192b60e73ef4043 | 全读 |

## 2. 可执行证据原文（/tmp/m016_b5r1_run.log）

```
START=2026-09-11T09:57:29Z HEAD=fe50e3f3 origin/main=fe50e3f3
== 0. 4 frozen migrations + V423 + same-batch files (lines/bytes/sha)
backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql 61 2828 141970f242484accdf99ac37a896ed04762677f5c41d1d4808b2be0ba13815e6
backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql 73 3658 fd9200070b4f4439c58cb32f4a03d6eb7f62b16c47320a57c2645ab2f101a56b
backend/src/main/resources/db/migration/V184__space_type_dictionary.sql 24 1535 238d1529736908fcf3ff5346e0c7af4636361e035cb7629b27f8729b4895cc52
backend/src/main/resources/db/migration/V203__spaces_status_check.sql 6 540 a159a62c1b8f23a82ddb002a6a625870c469a9250001ed51743a8cfcd3cdb83f
backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql 22 2099 b9912e89df22d03b54305a48af70052c53a58563df8212be0192b60e73ef4043
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java 678 31232 21e7ea67b986b73d827e20a5a24f55c96ad8340659e117bdb22e8714522a193c
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java 715 32109 03a59adf8c527f0dd92fb42e49188a8c3334ad685d82829621ed449e6e874fe2
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java 244 9639 018480bf503be1579a00ce6c36ea83b283c24951c161a581029ea17c33f9e9e9
backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java 506 24001 16e57fdb9d033c2d8e396809736790aa46349bce14c089058e0e488c3ac796b6
docs/data-isolation-and-tenant-scope-security-spec-v0.1.md 371 41083 33cd57b26ee44008d7be2435940c0ab05427a06e42aee09a305325bb62cc920e
docs/domain-ownership-scope-audit-v0.1.csv 335 59958 b33ca86505e1327e445740c75f0923300aa064e53a6a1527cb09a4f7b9e4d430
== 1. frozen four unchanged vs HEAD
(empty = unchanged)
== 2. flyway_schema_history (complex_test): V423 applied
023|p1 floor plan polygons|t|2026-09-09
034|p2 floor plan import tasks|t|2026-09-09
184|space type dictionary|t|2026-09-09
203|spaces status check|t|2026-09-09
423|m016 floor plan and space type checks|t|2026-09-11
423
== 3. constraints after V423
mall_floor_plans|chk_mall_floor_plans_project_id_required|CHECK ((project_id IS NOT NULL)) NOT VALID
floor_plan_shapes|chk_floor_plan_shapes_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying, 'booth'::character varying, 'public_area'::character varying])::text[])))
floor_plan_import_candidates|chk_floor_plan_import_candidates_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying, 'booth'::character varying, 'public_area'::character varying])::text[])))
space_type_dictionary|chk_space_type_dictionary_status|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'disabled'::character varying])::text[])))
building|active
floor|active
zone|active
shop|active
public_area|active
parking|active
booth|active
other|active
== 4. 001/002 same-batch whitelist + versions
2:// 文件版本：V0.03
35:    private static final Set<String> SHAPE_TYPES = Set.of("area", "shop", "entrance", "facility", "risk_zone", "route", "booth", "public_area");
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java:2:// 文件版本：V0.03
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java:2:// 文件版本：V0.02
backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java:2:// 文件版本：V0.05
== 5. V423 idempotency shape (DROP IF EXISTS before each ADD)
4
4
12:--    新写/更新行强制非空，存量 NULL 行容忍（NOT VALID），不发明归属值；软引用不加 FK�
15:    CHECK (project_id IS NOT NULL) NOT VALID;
== 6. gate docs in sync
data isolation spec is in sync with Flyway migration scope baseline.
DOMAIN_OWNERSHIP_SCOPE_PASS: 334 tables classified
59:> 事实源：`backend/src/main/resources/db/migration`。当前解析到最新迁移 **V423**，发现 **170** 张�
== 7. tests (main worktree, complex_test, three DB vars only)
MVN_EXIT=0
floorplan.FloorPlanControllerTests.txt:Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.735 s -- in com.complex.platform.floorplan.FloorPlanControllerTests
floorplan.FloorPlanImportParserTests.txt:Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.140 s -- in com.complex.platform.floorplan.FloorPlanImportParserTests
floorplan.FloorPlanJdbcIntegrationTests.txt:Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.961 s -- in com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
floorplan.FloorPlanServiceTests.txt:Tests run: 28, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.080 s -- in com.complex.platform.floorplan.FloorPlanServiceTests
governance.PlatformJdbcIntegrationTests.txt:Tests run: 17, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.235 s -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
governance.VenueBusinessHoursServiceTests.txt:Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.043 s -- in com.complex.platform.governance.VenueBusinessHoursServiceTests
== 8. untouched surfaces
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-governance-map-for-operations-
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
openapi/front/seed changes: 0
 M backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
 M backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
 M backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
 M docs/audit-2026-09/findings.md
 M docs/audit-2026-09/ledger.md
 M docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
?? backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql
?? docs/audit-2026-09/modules/M016-b1-grok-raw.md
?? docs/audit-2026-09/modules/M016-b1-self-raw.md
?? docs/audit-2026-09/modules/M016-b4-r1-grok-raw.md
?? docs/audit-2026-09/modules/M016-b4-r1-self-raw.md
?? docs/audit-2026-09/modules/M016-b5-r1-grok-raw.md
END=2026-09-11T09:57:36Z
```

## 3. 三线审计方法与覆盖

- 第一性原理线：从治理地图 L469-472（空间对象、候选区域「铺位、摊位、公共区」）、枚举 L259（铺位租赁状态六态）与 V184/V203 文件内拍板反推四份冻结迁移与本批 V423 应当固定的闭集/完整性契约，对照 complex_test 实态（V423 已应用；`pg_constraint`/`flyway_schema_history` 只读，§2 第 2–3 项）与读写这些表的实现（M015 `FloorPlanService` 白名单已同闭集、`PlatformService.createSpace/createSpaceType`、`JdbcContractRepository.spaceRentalStatus`）。
- 顺从式线：18 项判据逐族（§5）。对抗式线：以 booth/public_area 图形与候选落库（现能落）、以 NULL project_id 直接落库（现被 NOT VALID CHECK 拒）、以 status='Active' 写字典（现被 CHECK 拒）、重跑 V423（`DROP IF EXISTS` 幂等）、在存量 NULL project_id 行上部署（NOT VALID 不校验存量、不失败）、以 corridor/geojson 入库（仍被拒：定稿禁止项）。
- 只读依赖追流（不审）：V005（spaces/projects）、V052（merchant_id）、V159（NOT VALID 范式）、V251（权限名中文）、V180/V261（角色授权）、`generate-data-isolation-spec.mjs` / `generate-domain-ownership-scope-audit.mjs`（门禁产物生成器）、前端 `FloorPlanImportPage.tsx`（M018）。
- 4 份冻结迁移 + V423 全文逐行读完（§1）；同批改动的 M015/M017 文件与门禁产物只作修复面核对；B3-r1 diff 逐字对照（B4-r1 双 PASS）；6 个测试类在主工作区实跑（§2 第 7 项）。

## 4. 已定稿 4 项在当前工作区的状态（逐项）

| ID | 队列 | 当前状态 | 本席核对 |
|---|---|---|---|
| 001 | 否（已修） | V423 `chk_mall_floor_plans_project_id_required CHECK (project_id IS NOT NULL) NOT VALID`；不加 FK、不 SET NOT NULL | §2 第 3 项约束在 complex_test 存在；`floorPlanWithoutProjectIsRejectedByDatabase` 绿；与 spaces.project_id 软引用范式一致 |
| 002 | 否（已修） | V423 两表 shape_type CHECK 八值；`FloorPlanService.SHAPE_TYPES` 同闭集（V0.03） | §2 第 3/4 项；`shapeTypeBoothAndPublicAreaAreAcceptedByDatabase`、`saveShapeAcceptsBoothAndPublicArea` 绿；corridor 未入库 |
| 003 | 否（已修） | V423 归一脏值→disabled 后 `chk_space_type_dictionary_status CHECK (active/disabled)` | §2 第 3 项；八行仍 active；`spaceTypeDictionaryRejectsUnknownStatus` 绿 |
| 004 | 否（登记） | 观察项未动（permission_name 已由 V251 中文化、CASCADE 惰性、cancelled 无写路径、V203 归一 active、V052 merchant_id 可空、前端 geojson/corridor → M018、M015-012 补列全局遗漏） | 与定稿一致 |

## 5. 18 项判据逐族核对（修复后）

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | 四张迁移与 V423 均有 HTML 上位（空间点位/平面图导入候选区域/空间类型/铺位可租态） |
| GEN-002 可执行正确性 | 通过 | V423 在一次性库与 complex_test 两次应用成功；约束定义与定稿一致（§2 第 3 项） |
| GEN-003 测试有效性 | 通过 | 新增 4 例先红后绿锁住三条约束与白名单；6 类实跑全绿（§2 第 7 项） |
| GEN-004 信任边界 | 通过 | 闭集/非空在库层落地；字典 status 不再接受任意字符串 |
| GEN-005 授权 | 通过（登记） | 权限种子未动；高危码授平台管理员与 V261 同口径 |
| GEN-006 数据完整性 | 通过 | 001（project_id 非空契约）、003（字典 status 闭集）已落地；002 闭集与字典/前端/白名单一致 |
| GEN-007 并发幂等 | 通过 | V423 `DROP IF EXISTS` + `ADD`，可重跑；`UPDATE` 归一幂等 |
| GEN-008 接口与模式一致性 | 通过（登记） | shape_type CHECK = 白名单 = HTML 三类 + 既有标注类；cancelled 无写路径登记；门禁产物已再生成并 verify 通过（§2 第 6 项） |
| GEN-009 错误处理 | 无实现面 | 迁移无运行时错误路径；违例由应用层 DataIntegrityViolation 出网 |
| GEN-010 安全 | 通过 | 无秘密；无权限变更 |
| GEN-011 审计 | 无实现面 | 表结构不承载审计事件 |
| GEN-012 资源 | 通过（登记） | 无 DB 级顶点/候选数上限，上限在应用层 |
| GEN-013 构建 | 通过 | Flyway 全链应用（含 V423）；mvn 6 类全绿 |
| GEN-014 迁移正确性 | 通过 | 已应用四迁移零改写（§2 第 1 项 diff 为空）；V423 新 V 号、可重跑、存量 NULL 行不致部署失败（NOT VALID）；脏值归一方向保持既有可见行为 |
| GEN-015 中文 | 通过 | V423 注释中文；permission_name 已由 V251 中文化 |
| GEN-016 文档可验证性 | 通过 | V423 头注释写明三条来源 ID 与拍板依据；门禁产物与迁移目录同步 |
| GEN-017 分类 | 通过 | 无添加项；三条修复均为既有规则/契约落地，未发明业务 |
| GEN-018 证据 | 通过 | §2 命令原文（psql 只读、grep、npm verify、mvn） |

## 6. 本次 Findings

无新增 finding。（B3-r1 修复后 4+1 迁移三线全量重审、6 类实跑、门禁产物同步，未发现未定稿缺陷、添加项或 HTML 问题。）

## 7. 观察（登记，不入队；与 004 同）

- `mall_floor_plans` 存量 NULL project_id 行（若生产存在）在 NOT VALID 下继续容忍；服务层无更新平面图路径，不会因此报错；干净环境可日后 `VALIDATE CONSTRAINT`（部署期决定，不在本批）。
- M015 解析器对候选类型仍「非白名单整单失败」（定稿保持）；前端默认 sourceFormat=geojson 与 corridor 标签属 M018 登记。
- Grok B4-r1 非阻断勘误（登记不入队）：V423 L3 注释「应用白名单与前端闭集另批放宽」后半对 Java 白名单不准确（白名单已同批改，行为以 CHECK 与 `SHAPE_TYPES` 同闭集为准）；无 corridor 负例与 import_candidates 独立用例（两表 CHECK 由 SQL 与一次性库探针覆盖）；`CHECK (project_id IS NOT NULL)` 不拒空串（服务层已必填）。
- 技能：`verification-before-completion`（§2 命令实跑）、`receiving-code-review`（未读席乙 B5 报告）、`test-driven-development`（B3 先红后绿，一次性库干跑）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B5-r1 票：**PASS**。4 份冻结迁移 + V423 全文三线重审、18 判据逐族核对、4 项定稿逐项核对、6 类实跑全绿、门禁产物同步；无新增缺陷、无添加项、无 HTML 问题；未提交/推送。本报告落盘前未读取 `M016-b5-r1-grok-raw.md` 任何正文。
