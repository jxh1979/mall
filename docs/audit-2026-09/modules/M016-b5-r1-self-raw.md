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


---
# M016 席甲独立补充复查完整报告
版本：V0.02（2026-09-20T03:56:12Z；上方 V0.01 为 2026-09-11 B5-r1 原文原样保留，16411 bytes，SHA256 6ef6ad5d81260d5c083b1b5cc7f7b69670b6f9529d7a568af16df6a435cb34d7）

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票）。操作员 2026-09-19 指令「从 M009 重新独立审计，有问题按 V0.17 完整双席流程」续作；M013、M014 已重开收口并合并（PR #453 / #454），M015 补充复查 PASS。本次为 M016 单席补充复查：无问题只记录、不是新双 PASS；发现问题则重开 B1 完整双席。
- 正本 `/home/james/complex-management` main 工作区，HEAD `e456ba96`；工作区含未提交的 M009 阶段性修改（本模块 4 冻结文件与 V423 均未改）。
- 范围：`plan/modules.md` V0.04 的 4 文件（V023/V034/V184/V203，冻结 164 行，当前 164 行）+ 本模块 B3-r1 新增 V423；判据 18 项（GEN-001…018），与 B5-r1 同一冻结集合；锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。
- 只读承诺：未改源码/迁移/测试/三份 HTML/锁/findings；数据库探针全部 SELECT；测试在主工作区、`complex_test`（Flyway 427）、只导出 `COMPLEX_DATABASE_URL/_USERNAME/_PASSWORD`（`SPRING_PROFILES_ACTIVE` 未导出，§5 log「== 8.」计数 0）。
- Grok：NOT-CALLED（无命令/退出码/新票）。旧 Grok B5-r1 V0.01 raw SHA256 `b5add5923387475c2fc89971bd69b347f70286eddc6379e896cb81d0ed9279cd`、其锁行同值，保持不动、不冒称本次调用。

## 1. 文件覆盖表（当前；行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql | 61 | 2828 | 141970f242484accdf99ac37a896ed04762677f5c41d1d4808b2be0ba13815e6 | 全读 |
| backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql | 73 | 3658 | fd9200070b4f4439c58cb32f4a03d6eb7f62b16c47320a57c2645ab2f101a56b | 全读 |
| backend/src/main/resources/db/migration/V184__space_type_dictionary.sql | 24 | 1535 | 238d1529736908fcf3ff5346e0c7af4636361e035cb7629b27f8729b4895cc52 | 全读 |
| backend/src/main/resources/db/migration/V203__spaces_status_check.sql | 6 | 540 | a159a62c1b8f23a82ddb002a6a625870c469a9250001ed51743a8cfcd3cdb83f | 全读 |
| backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql | 22 | 2099 | b9912e89df22d03b54305a48af70052c53a58563df8212be0192b60e73ef4043 | 全读 |
| backend/src/main/resources/db/migration/V424__space_type_delete_requests.sql | 8 | 239 | 9625314129ba6b8ad47601e034a0a82c42240ff9ef1a1c604d427092a7af0295 | 全读 |
| backend/src/main/resources/db/migration/V425__space_type_status_archive_pending.sql | 6 | 569 | 81803091a24a655924293ac0bc1362693cb456441eb7d4ec5752a616bf3eff1c | 全读 |
| backend/src/main/resources/db/migration/V426__space_type_legacy_disabled_to_archived.sql | 2 | 182 | 2686332155001257a8992b818bcd9fb075e6a25040ea92ffa2cee286a00c1627 | 全读 |
| backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql | 20 | 705 | cc8c391bda2bdb2658d5640ceff6036736bfe9d2c3b864de8f13e8474f9a3b13 | 全读 |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java | 472 | 21466 | acd1f485522c9c31c3ec37c1194226fb5259db54e40a970e3fb2b0ea2aaa7aac | 只读依赖（未审，实跑） |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java | 193 | 7611 | a6e0f52b13a11a0d157df098a569b0e2c62a0044eaebbb3cfa5fe8382e1b758c | 只读依赖（未审，实跑） |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java | 277 | 11748 | 5a5932ce4bf3dc12d3b1a652cf107144bd33663cef462f6c0d87f405bdedaa59 | 只读依赖（未审，实跑） |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 864 | 41404 | 9fa9ecad75631864ddae02584f533614c8c902c3914a3ca92cc79875507f5a09 | 只读依赖（未审，实跑） |
| backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java | 734 | 40076 | 6dce8606a79e06b46d7d302ef7ca8048bb300a67191b9e6980ff8bb81cabb95f | 只读依赖（未审，实跑） |
| backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java | 251 | 11957 | d8b3df0e5d5ad082e3589e81e489a1269ac3ef9432186d4104d3a385979dfba9 | 只读依赖（未审，实跑） |
| backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 678 | 31232 | 21e7ea67b986b73d827e20a5a24f55c96ad8340659e117bdb22e8714522a193c | 全读 |

## 2. 与 B5-r1 的差异定位

- 4 份冻结迁移 + V423 的 SHA256 与 V0.01 §1 覆盖表逐一相等（§5 log「== 0./== 1.」：对 HEAD 与 origin/main 的 diff 均为空，工作区 porcelain 无该五文件），已应用迁移零改写。
- 本模块闭环之后，同表出现 4 份后续迁移（§5 log「== 2.」git 出处）：M018 闭环 V424（`space_type_delete_requests` 建表）、V425（把 V423 的 `chk_space_type_dictionary_status` 由两值改为 `active/disabled/archived/pending_delete` 四值）、V426（历史 `disabled` 行改 `archived`）；M009 在途 V427（projects 三列、merchants.project_id、business_entity_projects；已暂存未提交）。complex_test 已应用 V423–V427（§5 log「== 3.」）。
- 对本模块 18 判据的影响：V425 以 `DROP CONSTRAINT IF EXISTS` 后重建，V423 本身不改、迁移链仍可重跑（GEN-007/014）；M016-003「字典 status 闭集」的两值口径已被 M018 授权的四值口径取代，属后续模块变更而非 M016 缺陷；V184 :8 行注释 `-- active/disabled` 与 V423 :17 注释「V184 注释 active/disabled」在四值口径下已陈旧——两者均为已应用迁移（零改写范围）且不是可执行规则，登记为观察（XM→M018 已闭环）。001（`project_id` NOT VALID CHECK）与 002（两表 shape_type 八值 CHECK＝`FloorPlanService.SHAPE_TYPES`）在活库约束实查中原样存在（§5 log「== 4./== 5.」）。

## 3. 三线审计（4+1 迁移全文重读，V424–V427 只读追流）

- 第一性原理线：从治理地图「空间和点位」「候选区域铺位/摊位/公共区」、枚举「铺位租赁状态六态」与 V184/V203 文件内拍板反推四份冻结迁移与 V423 应固定的闭集/完整性契约，对照 complex_test 实态：mall_floor_plans 状态三值 + project_id NOT VALID 非空；floor_plan_shapes / import_candidates shape_type 八值、ref_type 六值、status 三值、多边形 ≥3 点；spaces status 二值；字典 status 现为四值（V425）。均成立。
- 顺从式线：18 判据逐族结论与 V0.01 §5 相同；变化：GEN-006/008/014/016 补记 V425/V426 取代 V423 两值 CHECK 的后续事实与 V184/V423 注释陈旧观察；GEN-003 按当前 6 类 106 例（XML 口径）实跑；GEN-008 门禁产物 `verify:data-isolation-spec` exit=1、`verify:domain-ownership-scope` exit=1（主工作区含 M009 暂存产物，结果如实记）。
- 对抗式线：以 booth/public_area 图形与候选落库（`shapeTypeBoothAndPublicAreaAreAcceptedByDatabase` 绿）、以 NULL project_id 直接落库（`floorPlanWithoutProjectIsRejectedByDatabase` 绿）、以 `Active`/未知字符串写字典 status（`spaceTypeDictionaryRejectsUnknownStatus` 绿；四值 CHECK 仍拒未知值）、重跑 V423/V425 幂等形态（各 `DROP IF EXISTS` 后 `ADD`）；字典八行现为 active（V426 无历史 disabled 行可改）；`space_type_delete_requests` 空表。未发现未定稿缺陷、添加项或 HTML 问题。
- 只读依赖追流（不审）：V005/V052/V159/V251/V180/V261、`FloorPlanService.SHAPE_TYPES`（M015 单元集，今日 M015 补充复查已核）、门禁产物生成器、三份 HTML 现文（SHA 与 B5-r1 相同）。

## 4. 已定稿 4 项在当前工作区的状态

001/002 与 V0.01 §4 逐项相同（约束原样、用例绿）；003：V423 归一 + 两值 CHECK 已落地后，M018 V425/V426 按其授权扩为四值并把历史 disabled 改 archived——M016 侧不回退、不改已应用迁移，登记为后续变更；004 观察不变（其中「前端 geojson/corridor → M018」已随 M018 闭环处理，不在本模块复核）。修复队列仍 4 个 ID 全否。

## 5. 可执行证据原文（`/home/james/audit-tmp/m016_supp_run.log`）

```
START=2026-09-20T03:52:35Z HEAD=e456ba96 origin/main=246385d9 URL=jdbc:postgresql://127.0.0.1:5432/complex_test
== 0. 4 frozen migrations + V423 + later same-table migrations + 6 test classes (lines/bytes/sha256)
backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql 61 2828 141970f242484accdf99ac37a896ed04762677f5c41d1d4808b2be0ba13815e6
backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql 73 3658 fd9200070b4f4439c58cb32f4a03d6eb7f62b16c47320a57c2645ab2f101a56b
backend/src/main/resources/db/migration/V184__space_type_dictionary.sql 24 1535 238d1529736908fcf3ff5346e0c7af4636361e035cb7629b27f8729b4895cc52
backend/src/main/resources/db/migration/V203__spaces_status_check.sql 6 540 a159a62c1b8f23a82ddb002a6a625870c469a9250001ed51743a8cfcd3cdb83f
backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql 22 2099 b9912e89df22d03b54305a48af70052c53a58563df8212be0192b60e73ef4043
backend/src/main/resources/db/migration/V424__space_type_delete_requests.sql 8 239 9625314129ba6b8ad47601e034a0a82c42240ff9ef1a1c604d427092a7af0295
backend/src/main/resources/db/migration/V425__space_type_status_archive_pending.sql 6 569 81803091a24a655924293ac0bc1362693cb456441eb7d4ec5752a616bf3eff1c
backend/src/main/resources/db/migration/V426__space_type_legacy_disabled_to_archived.sql 2 182 2686332155001257a8992b818bcd9fb075e6a25040ea92ffa2cee286a00c1627
backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql 20 705 cc8c391bda2bdb2658d5640ceff6036736bfe9d2c3b864de8f13e8474f9a3b13
backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java 472 21466 acd1f485522c9c31c3ec37c1194226fb5259db54e40a970e3fb2b0ea2aaa7aac
backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java 193 7611 a6e0f52b13a11a0d157df098a569b0e2c62a0044eaebbb3cfa5fe8382e1b758c
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java 277 11748 5a5932ce4bf3dc12d3b1a652cf107144bd33663cef462f6c0d87f405bdedaa59
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java 864 41404 9fa9ecad75631864ddae02584f533614c8c902c3914a3ca92cc79875507f5a09
backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java 734 40076 6dce8606a79e06b46d7d302ef7ca8048bb300a67191b9e6980ff8bb81cabb95f
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java 251 11957 d8b3df0e5d5ad082e3589e81e489a1269ac3ef9432186d4104d3a385979dfba9
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java 678 31232 21e7ea67b986b73d827e20a5a24f55c96ad8340659e117bdb22e8714522a193c
== 1. frozen four + V423 unchanged vs HEAD and vs origin/main (empty = unchanged)
A  backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql
== 2. later migrations touching the same tables (M018 V424–V426, M009 V427) — git provenance
ed0bf4f4 2026-09-11 audit M018: hide deleted space kinds, archive reason and unarchive, reject, no other-shape
e35dd757 2026-09-11 audit M018: new space kinds always active, archive replaces disable, delete needs approval
757ef452 2026-09-11 audit M018: space type archive, delete request, master approval
== 3. flyway_schema_history (complex_test, SELECT only)
023|p1 floor plan polygons|t|2026-09-09
034|p2 floor plan import tasks|t|2026-09-09
184|space type dictionary|t|2026-09-09
203|spaces status check|t|2026-09-09
423|m016 floor plan and space type checks|t|2026-09-11
424|space type delete requests|t|2026-09-14
425|space type status archive pending|t|2026-09-14
426|space type legacy disabled to archived|t|2026-09-14
427|m009 project profiles and associations|t|2026-09-14
427
== 4. constraints now (SELECT only)
spaces|chk_spaces_status|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'unavailable'::character varying])::text[])))
mall_floor_plans|chk_mall_floor_plans_image_size|CHECK (((image_width > 0) AND (image_height > 0)))
mall_floor_plans|chk_mall_floor_plans_project_id_required|CHECK ((project_id IS NOT NULL)) NOT VALID
mall_floor_plans|chk_mall_floor_plans_status|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'inactive'::character varying, 'archived'::character varying])::text[])))
floor_plan_shapes|chk_floor_plan_shapes_points_array|CHECK (((jsonb_typeof(polygon_points) = 'array'::text) AND (jsonb_array_length(polygon_points) >= 3)))
floor_plan_shapes|chk_floor_plan_shapes_ref_type|CHECK (((ref_type)::text = ANY ((ARRAY['space'::character varying, 'merchant'::character varying, 'project'::character varying, 'amusement_project'::character varying, 'device'::character varying, 'custom'::character varying])::text[])))
floor_plan_shapes|chk_floor_plan_shapes_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying, 'booth'::character varying, 'public_area'::character varying])::text[])))
floor_plan_shapes|chk_floor_plan_shapes_status|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'inactive'::character varying, 'archived'::character varying])::text[])))
floor_plan_import_candidates|chk_floor_plan_import_candidates_no|CHECK ((candidate_no > 0))
floor_plan_import_candidates|chk_floor_plan_import_candidates_points_array|CHECK (((jsonb_typeof(polygon_points) = 'array'::text) AND (jsonb_array_length(polygon_points) >= 3)))
floor_plan_import_candidates|chk_floor_plan_import_candidates_ref_type|CHECK (((ref_type)::text = ANY ((ARRAY['space'::character varying, 'merchant'::character varying, 'project'::character varying, 'amusement_project'::character varying, 'device'::character varying, 'custom'::character varying])::text[])))
floor_plan_import_candidates|chk_floor_plan_import_candidates_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying, 'booth'::character varying, 'public_area'::character varying])::text[])))
floor_plan_import_candidates|chk_floor_plan_import_candidates_status|CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'published'::character varying, 'rejected'::character varying])::text[])))
space_type_dictionary|chk_space_type_dictionary_status|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'disabled'::character varying, 'archived'::character varying, 'pending_delete'::character varying])::text[])))
building|active
floor|active
zone|active
shop|active
public_area|active
parking|active
booth|active
other|active
0|0
0
== 5. 002 same-batch whitelist + version headers
2:// 文件版本：V0.03
35:    private static final Set<String> SHAPE_TYPES = Set.of("area", "shop", "entrance", "facility", "risk_zone", "route", "booth", "public_area");
backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java:2:// 文件版本：V0.01
backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java:2:// 文件版本：V0.01
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java:2:// 文件版本：V0.03
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java:2:// 文件版本：V0.04
backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java:2:// 文件版本：V0.08
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java:2:// 文件版本：V0.02
== 6. V423 idempotency shape + V425 supersedes V423 status CHECK
4
4
backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql:22:    CHECK (status IN ('active', 'disabled'));
backend/src/main/resources/db/migration/V425__space_type_status_archive_pending.sql:6:    CHECK (status IN ('active', 'disabled', 'archived', 'pending_delete'));
backend/src/main/resources/db/migration/V184__space_type_dictionary.sql:8:    status VARCHAR(32) NOT NULL DEFAULT 'active',   -- active/disabled
backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql:17:-- 3) 空间类型字典 status 闭集（V184 注释 active/disabled；listActiveSpaceTypeCodes 只认 active）：
== 7. gate docs (read-only verify; main worktree contains M009 staged files)
data isolation spec is out of sync with Flyway migrations. Run: npm run docs:data-isolation-spec
DATA_ISOLATION_EXIT=1
docs/domain-ownership-scope-audit-v0.1.csv is out of sync with Flyway migrations; run npm run docs:domain-ownership-scope
DOMAIN_OWNERSHIP_EXIT=1
== 8. tests (main worktree, complex_test, three DB vars only)
0
CLEAN_EXIT=0
20:52:48.294 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests]: FloorPlanJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
20:52:48.350 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
20:52:48.380 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests]: FloorPlanJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
20:52:48.381 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-19T20:52:48.537-07:00  INFO 1520467 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Starting FloorPlanJdbcIntegrationTests using Java 21.0.12.1 with PID 1520467 (started by james in /home/james/complex-management/backend)
2026-09-19T20:52:48.537-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Running with Spring Boot v4.1.0, Spring v7.0.8
2026-09-19T20:52:48.538-07:00  INFO 1520467 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : No active profile set, falling back to 1 default profile: "local"
2026-09-19T20:52:49.413-07:00  INFO 1520467 --- [complex-api] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-09-19T20:52:49.485-07:00  INFO 1520467 --- [complex-api] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@340c7479
2026-09-19T20:52:49.486-07:00  INFO 1520467 --- [complex-api] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-09-19T20:52:49.496-07:00  INFO 1520467 --- [complex-api] [           main] org.flywaydb.core.FlywayExecutor         : Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)
2026-09-19T20:52:49.598-07:00  INFO 1520467 --- [complex-api] [           main] o.f.core.internal.command.DbValidate     : Successfully validated 395 migrations (execution time 00:00.071s)
2026-09-19T20:52:49.608-07:00  INFO 1520467 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Current version of schema "public": 427
2026-09-19T20:52:49.609-07:00  INFO 1520467 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Schema "public" is up to date. No migration necessary.
2026-09-19T20:52:50.471-07:00  INFO 1520467 --- [complex-api] [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name userDetailsService
2026-09-19T20:52:50.896-07:00  INFO 1520467 --- [complex-api] [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoints beneath base path '/actuator'
2026-09-19T20:52:50.929-07:00  INFO 1520467 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Started FloorPlanJdbcIntegrationTests in 2.53 seconds (process running for 4.337)
2026-09-19T20:52:50.974-07:00  INFO 1520467 --- [complex-api] [           main] .p.c.c.MultiPartyContactPhonePiiBackfill : PII_BACKFILL_CLEAN: 六个多主体手机号列均无明文残留
2026-09-19T20:52:50.975-07:00  WARN 1520467 --- [complex-api] [           main] o.s.core.events.SpringDocAppInitializer  : SpringDoc /docs/openapi endpoint is enabled by default. To disable it in production, set the property 'springdoc.api-docs.enabled=false'
2026-09-19T20:52:50.975-07:00  WARN 1520467 --- [complex-api] [           main] o.s.core.events.SpringDocAppInitializer  : SpringDoc /docs/swagger-ui endpoint is enabled by default. To disable it in production, set the property 'springdoc.swagger-ui.enabled=false'
2026-09-19T20:52:51.298-07:00  INFO 1520467 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanControllerTests
2026-09-19T20:52:51.300-07:00  INFO 1520467 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanControllerTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-19T20:52:51.328-07:00  INFO 1520467 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Starting FloorPlanControllerTests using Java 21.0.12.1 with PID 1520467 (started by james in /home/james/complex-management/backend)
2026-09-19T20:52:51.328-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Running with Spring Boot v4.1.0, Spring v7.0.8
2026-09-19T20:52:51.328-07:00  INFO 1520467 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : No active profile set, falling back to 1 default profile: "local"
2026-09-19T20:52:51.652-07:00  INFO 1520467 --- [complex-api] [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name userDetailsService
2026-09-19T20:52:51.721-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.auth.CustomerSessionCookieFilter   : Filter 'customerSessionCookieFilter' configured for use
2026-09-19T20:52:51.721-07:00 DEBUG 1520467 --- [complex-api] [           main] .c.p.g.DeviceCredentialBodyCachingFilter : Filter 'deviceCredentialBodyCachingFilter' configured for use
2026-09-19T20:52:51.721-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.auth.BearerAuthenticationFilter    : Filter 'bearerAuthenticationFilter' configured for use
2026-09-19T20:52:51.721-07:00 DEBUG 1520467 --- [complex-api] [           main] .c.p.a.CustomerTokenAuthenticationFilter : Filter 'customerTokenAuthenticationFilter' configured for use
2026-09-19T20:52:51.722-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.auth.DoubleSubmitCsrfFilter        : Filter 'doubleSubmitCsrfFilter' configured for use
2026-09-19T20:52:51.722-07:00  INFO 1520467 --- [complex-api] [           main] o.s.b.t.m.w.SpringBootMockServletContext : Initializing Spring TestDispatcherServlet ''
2026-09-19T20:52:51.722-07:00  INFO 1520467 --- [complex-api] [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-09-19T20:52:51.723-07:00  INFO 1520467 --- [complex-api] [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 1 ms
2026-09-19T20:52:51.737-07:00  INFO 1520467 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Started FloorPlanControllerTests in 0.433 seconds (process running for 5.146)
2026-09-19T20:52:51.886-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T20:52:51.894-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T20:52:51.917-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T20:52:51.921-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T20:52:51.942-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T20:52:51.947-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T20:52:51.949-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T20:52:51.951-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T20:52:51.953-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T20:52:51.955-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T20:52:51.958-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T20:52:51.959-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T20:52:51.961-07:00 DEBUG 1520467 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T20:52:51.969-07:00  INFO 1520467 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.PlatformJdbcIntegrationTests]: PlatformJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T20:52:51.980-07:00  INFO 1520467 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.PlatformJdbcIntegrationTests
2026-09-19T20:52:51.980-07:00  INFO 1520467 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.PlatformJdbcIntegrationTests]: PlatformJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T20:52:51.981-07:00  INFO 1520467 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.PlatformJdbcIntegrationTests
2026-09-19T20:52:52.910-07:00  INFO 1520467 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases]: RealRepositoryCases does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T20:52:52.911-07:00  INFO 1520467 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases
2026-09-19T20:52:52.911-07:00  INFO 1520467 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases]: RealRepositoryCases does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T20:52:52.912-07:00  INFO 1520467 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests]: VenueBusinessHoursServiceTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T20:52:52.912-07:00  INFO 1520467 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases
MVN_EXIT=0
TEST-com.complex.platform.floorplan.FloorPlanControllerTests: tests="14" errors="0" skipped="0" failures="0" 
TEST-com.complex.platform.floorplan.FloorPlanImportParserTests: tests="14" errors="0" skipped="0" failures="0" 
TEST-com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests: tests="7" errors="0" skipped="0" failures="0" 
TEST-com.complex.platform.floorplan.FloorPlanServiceTests: tests="36" errors="0" skipped="0" failures="0" 
TEST-com.complex.platform.governance.PlatformJdbcIntegrationTests: tests="23" errors="0" skipped="0" failures="0" 
TEST-com.complex.platform.governance.VenueBusinessHoursServiceTests: tests="12" errors="0" skipped="0" failures="0" 
== 9. HTML + lock untouched
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1.html
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-governance-map-for-operations-v0.1.html
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
END=2026-09-20T03:52:53Z
```

| 测试类（XML testsuite） | tests | failures | errors | skipped |
|---|---:|---:|---:|---:|
| com.complex.platform.floorplan.FloorPlanControllerTests | 14 | 0 | 0 | 0 |
| com.complex.platform.floorplan.FloorPlanImportParserTests | 14 | 0 | 0 | 0 |
| com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests | 7 | 0 | 0 | 0 |
| com.complex.platform.floorplan.FloorPlanServiceTests | 36 | 0 | 0 | 0 |
| com.complex.platform.governance.PlatformJdbcIntegrationTests | 23 | 0 | 0 | 0 |
| com.complex.platform.governance.VenueBusinessHoursServiceTests | 12 | 0 | 0 | 0 |

6 类合计 106 例、0 失败/错误/跳过（surefire `.txt` 对含 @Nested 的类摘要记 0，以 XML 为准）。

## 6. 本次 Findings

无新增 finding。

## 7. 观察（登记，不入队；与 004 同）

- V184 :8 `-- active/disabled` 与 V423 :17 注释在 V425 四值口径下陈旧；两者为已应用迁移（零改写），非可执行规则；XM→M018（已闭环）。
- M009 在途 V427 已暂存未提交且已应用于 complex_test；不在本模块裁决。
- surefire `.txt` 摘要对 @Nested 类记 0 属工具口径，证据以 XML 为准。

## 8. 结论

席甲补充复查：**PASS**（不是新双 PASS；历史 B5-r1 Claude/Grok 双 PASS 保留）。Grok NOT-CALLED。未改源码/迁移/测试/三份 HTML/锁/findings；未提交/推送。锁实算/sidecar/ledger 首行三值全等 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；旧 Grok B5-r1 raw 锁行同值但不作本次调用报数；LOCK-AMEND-COUNT 1。先写 ledger V4.55 保留 PASS，再更新收口文件，本报告最后落盘。


---
CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M016 B5-r1 再执行 · 席甲完整报告
版本：V0.03（2026-09-20T05:59:50Z；上方 V0.01/V0.02 原样保留，50012 bytes，SHA256 c702e7e91420f405f5e913ee784610004f118810cf99fb60cb269d8c5df624be）

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票）。操作员 2026-09-20 裁定：M015–M017 补派 Grok B5 全量重审，两席同一 B3 次数（累计 1）下向同名 b5 raw 追加完整报告，双 PASS 才维持 PASS。本报告是席甲本次 B5 再执行的完整报告：从本行上方独占锁行起至文件末尾。
- 范围：4 冻结迁移（V023/V034/V184/V203）+ 本模块 B3 产物 V423；18 判据（GEN-001…018）；锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 顺序：Grok 已先派（追加到 modules/M016-b5-r1-grok-raw.md，存在=True），本报告落盘前未读其本次追加段；只读承诺同 V0.02（未改源码/测试/三 HTML/锁/findings，测试只导出三个 DB 变量、complex_test）。

## 1. 与 V0.02 审本的一致性

- V0.02（席甲补充复查完整报告）覆盖表 16 个文件当前 SHA256 逐一复算：本模块文件 5 个全部相等——V0.02 的三线审计、判据逐族核对与 finding 结论对本次工作区状态直接有效，本报告不重复抄写；只读依赖 `FloorPlanServiceTests.java`（现 SHA256 ea7aa63ecdb3…）、`VenueBusinessHoursServiceTests.java`（现 SHA256 3585ca694bf1…）、`FloorPlanService.java`（现 SHA256 5cad799d091f…） 因 M015 第 3 轮修复（M015-015/016：`projectScopeOrAll()`、发布前置放宽、测试桩生产语义）已变，不属本模块修复面，其变更已经 M015 B4-r3/B5-r3 两席审计；本席复读其中与本模块相关段（`FloorPlanService.SHAPE_TYPES` 八值、V423 约束引用）未动，V0.02 结论不受影响。
- 三份业务 HTML 与锁的 SHA 与 V0.02 相同（§2 log 末段）。

## 2. 本次重跑证据原文（/home/james/audit-tmp/m016_reexec_run.log）

```
START=2026-09-20T05:58:43Z HEAD=e456ba96 origin/main=1e5183d5 URL=jdbc:postgresql://127.0.0.1:5432/complex_test
== 0. 4 frozen migrations + V423 + later same-table migrations + 6 test classes (lines/bytes/sha256)
backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql 61 2828 141970f242484accdf99ac37a896ed04762677f5c41d1d4808b2be0ba13815e6
backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql 73 3658 fd9200070b4f4439c58cb32f4a03d6eb7f62b16c47320a57c2645ab2f101a56b
backend/src/main/resources/db/migration/V184__space_type_dictionary.sql 24 1535 238d1529736908fcf3ff5346e0c7af4636361e035cb7629b27f8729b4895cc52
backend/src/main/resources/db/migration/V203__spaces_status_check.sql 6 540 a159a62c1b8f23a82ddb002a6a625870c469a9250001ed51743a8cfcd3cdb83f
backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql 22 2099 b9912e89df22d03b54305a48af70052c53a58563df8212be0192b60e73ef4043
backend/src/main/resources/db/migration/V424__space_type_delete_requests.sql 8 239 9625314129ba6b8ad47601e034a0a82c42240ff9ef1a1c604d427092a7af0295
backend/src/main/resources/db/migration/V425__space_type_status_archive_pending.sql 6 569 81803091a24a655924293ac0bc1362693cb456441eb7d4ec5752a616bf3eff1c
backend/src/main/resources/db/migration/V426__space_type_legacy_disabled_to_archived.sql 2 182 2686332155001257a8992b818bcd9fb075e6a25040ea92ffa2cee286a00c1627
backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql 20 705 cc8c391bda2bdb2658d5640ceff6036736bfe9d2c3b864de8f13e8474f9a3b13
backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java 472 21466 acd1f485522c9c31c3ec37c1194226fb5259db54e40a970e3fb2b0ea2aaa7aac
backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java 193 7611 a6e0f52b13a11a0d157df098a569b0e2c62a0044eaebbb3cfa5fe8382e1b758c
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java 277 11748 5a5932ce4bf3dc12d3b1a652cf107144bd33663cef462f6c0d87f405bdedaa59
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java 897 44445 ea7aa63ecdb36e9cb8e0b921c62f012e5931d1a157a99ae202015a6b4ceda31d
backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java 734 40076 6dce8606a79e06b46d7d302ef7ca8048bb300a67191b9e6980ff8bb81cabb95f
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java 285 14140 3585ca694bf16fdd809689b819d258641792c6a8c964448374f295dff85c9c76
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java 689 32061 5cad799d091fd43b58d99ff05d12e10afbc16e63fb7e59857dcd94c042ccdd80
== 1. frozen four + V423 unchanged vs HEAD and vs origin/main (empty = unchanged)
A  backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql
== 2. later migrations touching the same tables (M018 V424–V426, M009 V427) — git provenance
ed0bf4f4 2026-09-11 audit M018: hide deleted space kinds, archive reason and unarchive, reject, no other-shape
e35dd757 2026-09-11 audit M018: new space kinds always active, archive replaces disable, delete needs approval
757ef452 2026-09-11 audit M018: space type archive, delete request, master approval
== 3. flyway_schema_history (complex_test, SELECT only)
023|p1 floor plan polygons|t|2026-09-09
034|p2 floor plan import tasks|t|2026-09-09
184|space type dictionary|t|2026-09-09
203|spaces status check|t|2026-09-09
423|m016 floor plan and space type checks|t|2026-09-11
424|space type delete requests|t|2026-09-14
425|space type status archive pending|t|2026-09-14
426|space type legacy disabled to archived|t|2026-09-14
427|m009 project profiles and associations|t|2026-09-14
427
== 4. constraints now (SELECT only)
spaces|chk_spaces_status|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'unavailable'::character varying])::text[])))
mall_floor_plans|chk_mall_floor_plans_image_size|CHECK (((image_width > 0) AND (image_height > 0)))
mall_floor_plans|chk_mall_floor_plans_project_id_required|CHECK ((project_id IS NOT NULL)) NOT VALID
mall_floor_plans|chk_mall_floor_plans_status|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'inactive'::character varying, 'archived'::character varying])::text[])))
floor_plan_shapes|chk_floor_plan_shapes_points_array|CHECK (((jsonb_typeof(polygon_points) = 'array'::text) AND (jsonb_array_length(polygon_points) >= 3)))
floor_plan_shapes|chk_floor_plan_shapes_ref_type|CHECK (((ref_type)::text = ANY ((ARRAY['space'::character varying, 'merchant'::character varying, 'project'::character varying, 'amusement_project'::character varying, 'device'::character varying, 'custom'::character varying])::text[])))
floor_plan_shapes|chk_floor_plan_shapes_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying, 'booth'::character varying, 'public_area'::character varying])::text[])))
floor_plan_shapes|chk_floor_plan_shapes_status|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'inactive'::character varying, 'archived'::character varying])::text[])))
floor_plan_import_candidates|chk_floor_plan_import_candidates_no|CHECK ((candidate_no > 0))
floor_plan_import_candidates|chk_floor_plan_import_candidates_points_array|CHECK (((jsonb_typeof(polygon_points) = 'array'::text) AND (jsonb_array_length(polygon_points) >= 3)))
floor_plan_import_candidates|chk_floor_plan_import_candidates_ref_type|CHECK (((ref_type)::text = ANY ((ARRAY['space'::character varying, 'merchant'::character varying, 'project'::character varying, 'amusement_project'::character varying, 'device'::character varying, 'custom'::character varying])::text[])))
floor_plan_import_candidates|chk_floor_plan_import_candidates_shape_type|CHECK (((shape_type)::text = ANY ((ARRAY['area'::character varying, 'shop'::character varying, 'entrance'::character varying, 'facility'::character varying, 'risk_zone'::character varying, 'route'::character varying, 'booth'::character varying, 'public_area'::character varying])::text[])))
floor_plan_import_candidates|chk_floor_plan_import_candidates_status|CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'published'::character varying, 'rejected'::character varying])::text[])))
space_type_dictionary|chk_space_type_dictionary_status|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'disabled'::character varying, 'archived'::character varying, 'pending_delete'::character varying])::text[])))
building|active
floor|active
zone|active
shop|active
public_area|active
parking|active
booth|active
other|active
0|0
0
== 5. 002 same-batch whitelist + version headers
2:// 文件版本：V0.04
35:    private static final Set<String> SHAPE_TYPES = Set.of("area", "shop", "entrance", "facility", "risk_zone", "route", "booth", "public_area");
backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java:2:// 文件版本：V0.01
backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java:2:// 文件版本：V0.01
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java:2:// 文件版本：V0.03
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java:2:// 文件版本：V0.05
backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java:2:// 文件版本：V0.08
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java:2:// 文件版本：V0.03
== 6. V423 idempotency shape + V425 supersedes V423 status CHECK
4
4
backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql:22:    CHECK (status IN ('active', 'disabled'));
backend/src/main/resources/db/migration/V425__space_type_status_archive_pending.sql:6:    CHECK (status IN ('active', 'disabled', 'archived', 'pending_delete'));
backend/src/main/resources/db/migration/V184__space_type_dictionary.sql:8:    status VARCHAR(32) NOT NULL DEFAULT 'active',   -- active/disabled
backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql:17:-- 3) 空间类型字典 status 闭集（V184 注释 active/disabled；listActiveSpaceTypeCodes 只认 active）：
== 7. gate docs (read-only verify; main worktree contains M009 staged files)
data isolation spec is out of sync with Flyway migrations. Run: npm run docs:data-isolation-spec
DATA_ISOLATION_EXIT=1
docs/domain-ownership-scope-audit-v0.1.csv is out of sync with Flyway migrations; run npm run docs:domain-ownership-scope
DOMAIN_OWNERSHIP_EXIT=1
== 8. tests (main worktree, complex_test, three DB vars only)
0
CLEAN_EXIT=0
22:58:55.469 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests]: FloorPlanJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
22:58:55.526 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests
22:58:55.556 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests]: FloorPlanJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
22:58:55.557 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-19T22:58:55.715-07:00  INFO 1595314 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Starting FloorPlanJdbcIntegrationTests using Java 21.0.12.1 with PID 1595314 (started by james in /home/james/complex-management/backend)
2026-09-19T22:58:55.715-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Running with Spring Boot v4.1.0, Spring v7.0.8
2026-09-19T22:58:55.716-07:00  INFO 1595314 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : No active profile set, falling back to 1 default profile: "local"
2026-09-19T22:58:56.568-07:00  INFO 1595314 --- [complex-api] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-09-19T22:58:56.638-07:00  INFO 1595314 --- [complex-api] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@2f2d8770
2026-09-19T22:58:56.639-07:00  INFO 1595314 --- [complex-api] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-09-19T22:58:56.648-07:00  INFO 1595314 --- [complex-api] [           main] org.flywaydb.core.FlywayExecutor         : Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)
2026-09-19T22:58:56.741-07:00  INFO 1595314 --- [complex-api] [           main] o.f.core.internal.command.DbValidate     : Successfully validated 395 migrations (execution time 00:00.064s)
2026-09-19T22:58:56.756-07:00  INFO 1595314 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Current version of schema "public": 427
2026-09-19T22:58:56.758-07:00  INFO 1595314 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Schema "public" is up to date. No migration necessary.
2026-09-19T22:58:57.563-07:00  INFO 1595314 --- [complex-api] [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name userDetailsService
2026-09-19T22:58:58.008-07:00  INFO 1595314 --- [complex-api] [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoints beneath base path '/actuator'
2026-09-19T22:58:58.042-07:00  INFO 1595314 --- [complex-api] [           main] c.c.p.f.FloorPlanJdbcIntegrationTests    : Started FloorPlanJdbcIntegrationTests in 2.467 seconds (process running for 3.112)
2026-09-19T22:58:58.084-07:00  INFO 1595314 --- [complex-api] [           main] .p.c.c.MultiPartyContactPhonePiiBackfill : PII_BACKFILL_CLEAN: 六个多主体手机号列均无明文残留
2026-09-19T22:58:58.085-07:00  WARN 1595314 --- [complex-api] [           main] o.s.core.events.SpringDocAppInitializer  : SpringDoc /docs/openapi endpoint is enabled by default. To disable it in production, set the property 'springdoc.api-docs.enabled=false'
2026-09-19T22:58:58.085-07:00  WARN 1595314 --- [complex-api] [           main] o.s.core.events.SpringDocAppInitializer  : SpringDoc /docs/swagger-ui endpoint is enabled by default. To disable it in production, set the property 'springdoc.swagger-ui.enabled=false'
2026-09-19T22:58:58.421-07:00  INFO 1595314 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanControllerTests
2026-09-19T22:58:58.424-07:00  INFO 1595314 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.floorplan.FloorPlanControllerTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-19T22:58:58.439-07:00  INFO 1595314 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Starting FloorPlanControllerTests using Java 21.0.12.1 with PID 1595314 (started by james in /home/james/complex-management/backend)
2026-09-19T22:58:58.439-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Running with Spring Boot v4.1.0, Spring v7.0.8
2026-09-19T22:58:58.439-07:00  INFO 1595314 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : No active profile set, falling back to 1 default profile: "local"
2026-09-19T22:58:58.820-07:00  INFO 1595314 --- [complex-api] [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name userDetailsService
2026-09-19T22:58:58.940-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.auth.CustomerSessionCookieFilter   : Filter 'customerSessionCookieFilter' configured for use
2026-09-19T22:58:58.940-07:00 DEBUG 1595314 --- [complex-api] [           main] .c.p.g.DeviceCredentialBodyCachingFilter : Filter 'deviceCredentialBodyCachingFilter' configured for use
2026-09-19T22:58:58.940-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.auth.BearerAuthenticationFilter    : Filter 'bearerAuthenticationFilter' configured for use
2026-09-19T22:58:58.940-07:00 DEBUG 1595314 --- [complex-api] [           main] .c.p.a.CustomerTokenAuthenticationFilter : Filter 'customerTokenAuthenticationFilter' configured for use
2026-09-19T22:58:58.940-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.auth.DoubleSubmitCsrfFilter        : Filter 'doubleSubmitCsrfFilter' configured for use
2026-09-19T22:58:58.941-07:00  INFO 1595314 --- [complex-api] [           main] o.s.b.t.m.w.SpringBootMockServletContext : Initializing Spring TestDispatcherServlet ''
2026-09-19T22:58:58.941-07:00  INFO 1595314 --- [complex-api] [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-09-19T22:58:58.942-07:00  INFO 1595314 --- [complex-api] [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 1 ms
2026-09-19T22:58:58.959-07:00  INFO 1595314 --- [complex-api] [           main] c.c.p.f.FloorPlanControllerTests         : Started FloorPlanControllerTests in 0.531 seconds (process running for 4.029)
2026-09-19T22:58:59.113-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:58:59.122-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T22:58:59.145-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T22:58:59.148-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T22:58:59.170-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=VALIDATION_FAILED raw_message=idempotency_key is required
2026-09-19T22:58:59.175-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:58:59.177-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:58:59.178-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:58:59.180-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:58:59.181-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:58:59.182-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:58:59.184-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:58:59.185-07:00 DEBUG 1595314 --- [complex-api] [           main] c.c.p.common.api.GlobalExceptionHandler  : api error: code=PERMISSION_DENIED raw_message=permission denied
2026-09-19T22:58:59.193-07:00  INFO 1595314 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.PlatformJdbcIntegrationTests]: PlatformJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T22:58:59.205-07:00  INFO 1595314 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.PlatformJdbcIntegrationTests
2026-09-19T22:58:59.205-07:00  INFO 1595314 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.PlatformJdbcIntegrationTests]: PlatformJdbcIntegrationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T22:58:59.206-07:00  INFO 1595314 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.PlatformJdbcIntegrationTests
2026-09-19T22:58:59.951-07:00  INFO 1595314 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases]: RealRepositoryCases does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T22:58:59.952-07:00  INFO 1595314 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases
2026-09-19T22:58:59.952-07:00  INFO 1595314 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases]: RealRepositoryCases does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T22:58:59.952-07:00  INFO 1595314 --- [complex-api] [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.complex.platform.governance.VenueBusinessHoursServiceTests]: VenueBusinessHoursServiceTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-19T22:58:59.953-07:00  INFO 1595314 --- [complex-api] [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.complex.platform.ComplexApiApplication for test class com.complex.platform.governance.VenueBusinessHoursServiceTests$RealRepositoryCases
MVN_EXIT=0
TEST-com.complex.platform.floorplan.FloorPlanControllerTests: tests="14" errors="0" skipped="0" failures="0" 
TEST-com.complex.platform.floorplan.FloorPlanImportParserTests: tests="14" errors="0" skipped="0" failures="0" 
TEST-com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests: tests="7" errors="0" skipped="0" failures="0" 
TEST-com.complex.platform.floorplan.FloorPlanServiceTests: tests="39" errors="0" skipped="0" failures="0" 
TEST-com.complex.platform.governance.PlatformJdbcIntegrationTests: tests="23" errors="0" skipped="0" failures="0" 
TEST-com.complex.platform.governance.VenueBusinessHoursServiceTests: tests="14" errors="0" skipped="0" failures="0" 
== 9. HTML + lock untouched
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1.html
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-governance-map-for-operations-v0.1.html
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
END=2026-09-20T05:59:00Z
```

6 类（含 PlatformJdbcIntegrationTests/FloorPlanJdbcIntegrationTests 等）+ complex_test 只读探针 本次实跑：6 类 111 例（TEST-*.xml 口径）零失败/错误/跳过，MVN_EXIT=0。

## 3. 三线结论

- 第一性原理线 / 顺从式线 / 对抗式线的逐条内容以 V0.02 为准（同一文件集合、SHA 未变）；本次再执行重跑全部可执行证据，结果与 V0.02 一致，未发现新缺陷、添加项或 HTML 问题。
- 修复队列不变（历史 finding 全否）。

## 4. 结论

席甲 B5-r1 再执行票：**PASS**。Grok 本次追加段未读；两席落齐后做四值校验并合取判定。
