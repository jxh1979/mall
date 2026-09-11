CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M016 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 修复前整模块全量审计
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 模块：M016 — 空间点位、楼层平面图与营业时段／数据库迁移／第1段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 4 文件，计划冻结有效内容行 164；当前 `git ls-files -z -- <4 paths> | xargs -0 cat | wc -l` 实算 **164**（与计划冻结一致）
- 冻结判据：锁映射行 `M016 → [GEN-001 … GEN-018]`（`criteria-lock.md` L6395）实算 **18** 个唯一 ID；与开工清单逐字相同（MISSING=[] EXTRA=[] COUNT=18 unique=18）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M016-001、GR-M016-002
- 只登记、建议队列「否」：GR-M016-003；观察见 §8
- 添加项（B1 只登记，不代用户回答 2.2；本批无）：无
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做手工写操作（现有 `FloorPlanJdbcIntegrationTests` 由 Spring `@Transactional` 回滚；本席只跑了其中只读方法 `migrationCreatesFloorPlanTablesAndSeedsPermissions`）
- 本报告不表示全仓或发布门禁通过。M001–M015 及 U01–U18 已闭环，不重开。原 322 模块编号及旧结论不代表当前 471 模块完成。findings V1.94 仅作已登记边界背景（含转入本模块的 M015-002 / M015-013 / M013-004 / M014-002 / M009-013/018 / M015-012），不代替本模块独立审计。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| grilling | `.../productivity/grilling/SKILL.md` | 只用追问树自己取证（CHECK 闭集、可空列、NULL 绕过、脏值归一、权限中文、CASCADE），不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 内部 sharpen；该文件要求 `/grilling`，本席不向操作员发问 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 删除测试：删掉 V023/V034 则平面图表与导入任务不存在，治理地图 L472 导入/发布做不到；删掉 V184 则 `createSpace` 硬校验无字典；删掉 V203 则 `spaces.status` 无闭集，`spaceRentalStatus` 的人工「不可租」无列级保护。不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（sha256 / `pg_constraint` / `SELECT 'booth' = ANY(...)` / `mvn -Dtest`）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（HTML 原句→4 份迁移→FloorPlanService / JdbcFloorPlanRepository / PlatformService.createSpace / JdbcContractRepository.spaceRentalStatus / V052 / V251 / 测试 / OpenAPI / 前端只读） |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；测试 29 PASS ≠ 与 HTML 候选区域类型一致 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地：本 4 文件无额外抽象层 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推、未安装 hook |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒级，未起 monitor |
| domain.md | `docs/agents/domain.md` | 词表/三 HTML 最终解释权；未改 CONTEXT.md |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit` 技能会派评审子代理并另写 `docs/arc/audits/`：与双席/§7 冲突，**不调用**
- superpowers `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄
- Trail of Bits `audit-context-building` 会派 function-analyzer 子代理：与禁止子代理冲突，**不调用**

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain、MCP、第三主体。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值（文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，内容一行即锁哈希）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行 `M016 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]`（L6395）与开工清单 18 ID 逐字相同
- `plan/modules.md` M016 FILES 4 条与用户清单逐字相同
- 4 文件字节/SHA256/行数与用户固定清单 **match=True**（见 §2）

HEAD：`main` = `origin/main` = `fe50e3f309c04f81820f8414bd8860325d694098`（`rev-parse HEAD` / `origin/main` 两值相等）。本席未设 `GIT_INDEX_FILE`，未动主索引。

工作区：`git status --porcelain` = ` M docs/audit-2026-09/ledger.md`（M016 PENDING→IN_PROGRESS 的台账写入）。开工提示称「工作区干净」与此不完全一致；本 4 文件与三 HTML、锁、findings、modules.md 未改。本席只读，未再写 ledger。

ledger 只读核验（不改）：V3.38，600852 bytes，SHA256 `84349acb1fbbca3abbd3fa569b23635050258fa12636d9e44daa092199c54101`；M016 行 IN_PROGRESS、B3=0。findings V1.94，775822 bytes，SHA256 `21ae323afe35d15eb7f40d2150972a230a09e57e3be722a7acd8fe9f573deda4`，仅作已登记边界背景，不代替本模块审计。

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

三份 HTML 只读、未改。锁内本模块仅映射 GEN，HTML 作 2.3 类型 1 证据（不得用未映射的 PGM/BEQR 作本模块 finding 支撑 ID）。对本席读取的现行 HTML，治理地图 L463/L467/L472、枚举 L259「铺位租赁状态」均 hit=True。BEQR-187「运营营业日」锁定义在 `:187`，**未映射本模块**，只作对照：本 4 文件不含营业日/场次开歇表（V201 非本模块）。

三 HTML 当前字节/SHA256（只读核验，本席未改）：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| docs/business-enum-quick-reference-v0.1.html | 133961 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | 304482 | eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 |
| docs/platform-governance-map-for-operations-v0.1.html | 103015 | a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 |

三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。

## 2. 四文件清单与覆盖

`git ls-files -z -- <4 paths> | xargs -0 cat | wc -l` = **164**。每文件完整通读（非标题/搜索/截断）。

| 路径 | 行 | 字节 | SHA256 | 与开工清单 |
|---|---:|---:|---|---|
| backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql | 61 | 2828 | 141970f242484accdf99ac37a896ed04762677f5c41d1d4808b2be0ba13815e6 | match |
| backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql | 73 | 3658 | fd9200070b4f4439c58cb32f4a03d6eb7f62b16c47320a57c2645ab2f101a56b | match |
| backend/src/main/resources/db/migration/V184__space_type_dictionary.sql | 24 | 1535 | 238d1529736908fcf3ff5346e0c7af4636361e035cb7629b27f8729b4895cc52 | match |
| backend/src/main/resources/db/migration/V203__spaces_status_check.sql | 6 | 540 | a159a62c1b8f23a82ddb002a6a625870c469a9250001ed51743a8cfcd3cdb83f | match |

覆盖：V023 L1–61、V034 L1–73、V184 L1–24、V203 L1–6 均已整文件阅读。无 OVERSIZE。

## 3. 数据流（HTML → 本模块 4 迁移 → 实现 → 测试库）

### 3.1 HTML 原句（类型 1）

治理地图 `docs/platform-governance-map-for-operations-v0.1.html`：

- L347 卡片「空间和点位」：管理楼层、区域、卡座、吧台、游乐点位、库房和设备位置。
- L463 h2「空间和点位：所有业务都要知道“发生在哪”」
- L469：维护楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位。
- L470：记录空间面积、用途、状态、负责人和所属商户。
- L472：商场平面图网页导入：在后台上传或导入商场平面布局图，系统解析出候选区域（**铺位、摊位、公共区**），由人工复核候选与错误后发布到正式平面图；发布为高危动作，须二次确认——发布会写入正式平面图区域，不可静默覆盖既有布局证据。

枚举速查 `docs/business-enum-quick-reference-v0.1.html` L259：铺位租赁状态＝空置、洽谈中、已租、即将到期、已退租、不可租。

业务地图 L700：建立综合体项目、楼层、区域、点位。

HTML **没有**把空间类型写成封闭英文码表，也 **没有**把 `spaces.status` 写成六态租赁看板的存储列。

### 3.2 本模块四迁移落点

- **V023**：`mall_floor_plans` + `floor_plan_shapes`；形状 CHECK 六值 `area/shop/entrance/facility/risk_zone/route`；平面图 `project_id VARCHAR(64)` **无 NOT NULL、无 FK**；权限 `floor_plan.view/manage` 英文名，授 `role_platform_admin`；子表 `ON DELETE CASCADE`。
- **V034**：导入任务/候选/错误三表；候选 `shape_type` CHECK 与 V023 同六值；`source_format` CHECK `svg/standard_json`；权限 `floor_plan.import.view/manage` 英文名；子表 `ON DELETE CASCADE`。
- **V184**：`space_type_dictionary` 种子八码 `building/floor/zone/shop/public_area/parking/booth/other`；`type_code UNIQUE`；`status` 注释 `active/disabled`、**无 CHECK**；`ON CONFLICT (type_code) DO NOTHING`。
- **V203**：`UPDATE spaces SET status='active' WHERE status NOT IN ('active','unavailable')` 后加 `chk_spaces_status CHECK (status IN ('active','unavailable'))`。

后续非本模块、只作数据流背景：V052 给 `mall_floor_plans` 加可空 `merchant_id`；V251 把四条 `floor_plan.*` 的 `permission_name` 改中文；V180 给内部审计授 `floor_plan.view` / `floor_plan.import.view`。

### 3.3 读写实现（M015 已闭环，本席只读核对）

- `FloorPlanService` L33–38：`STATUSES` / `SHAPE_TYPES` / `REF_TYPES` / `IMPORT_SOURCE_FORMATS` 与 V023/V034 CHECK 同闭集（仍无 booth/public_area）。
- `FloorPlanService` L507–510：`project_id` 必填且 `projectExists`；L233 `requireText(request.projectId(),"project_id")`。
- `JdbcFloorPlanRepository.saveFloorPlan` L83–93：INSERT 含 `project_id`，**仓库层不拒绝 NULL**。
- `PlatformService.createSpace` L474–478：`space_type` 必须 ∈ `space_type_dictionary` 中 `status='active'` 的 `type_code`。
- `PlatformService.requireSpaceStatus` L687–694：白名单 `active/unavailable`，与 V203 CHECK 一致。
- `JdbcContractRepository.listSpaceRentalRaw` L443–475：六态租赁看板派生——`spaces.status=unavailable` → 不可租；否则按生效合同 / 意向 / 曾绑约派生已租/洽谈中/已退租/空置。**六态不存在 `spaces.status` 列里。**
- 前端 `FloorPlanImportPage.tsx` L17：`SHAPE_TYPE_LABELS` 含 `shop/booth/public_area/corridor/facility`；L52 默认 `sourceFormat: "geojson"`（不在 V034 CHECK）。
- OpenAPI `SpaceRequest.space_type` 枚举含 `mall/bar_table/facility`、无 `building/public_area`——活校验是 V184，契约快照口径已由 M013-004/M014-002 闭环，本模块不重开。

### 3.4 complex_test 实态（只读 SELECT）

环境文件 `COMPLEX_DATABASE_URL` 库名是 `complex`。按 AGENTS.md 测试库必须是 `complex_test`：本席用同一组账号口令改连 `complex_test`。曾对 `complex` 发出只读 SELECT（flyway 7 行 + columns），因 `ORDER BY` 别名失败于约束查询即停；**未对 `complex` 或 `complex_test` 做写操作**。下列证据均来自 `current_database()=complex_test`。

- Flyway：V023/V034/V184/V203/V052/V251/V005 均 `success=t`。JDBC 测试启动时 `DbValidate: Successfully validated 390 migrations`；`Current version=422`；`No migration necessary`。已应用迁移文件未被改写。
- `mall_floor_plans.project_id` `is_nullable=YES`，`attnotnull=f`，`fk_project=0`。
- `chk_floor_plan_shapes_shape_type` / `chk_floor_plan_import_candidates_shape_type` 仍为六值，无 booth/public_area。
- `space_type_dictionary`：8 行全 `active`；`chk_dict_status=0`（无 CHECK）。
- `spaces`：0 行；`chk_spaces_status` 二值已在；`status` 列 NOT NULL（来自 V005）。
- `mall_floor_plans` / shapes / import 各表 0 行。
- 权限名现网中文（V251）：查看/管理平面图和多边形；查看/管理平面图导入和发布候选。`risk_level` view=medium / manage=high；`requires_approval` view=f / manage=t。角色：`role_platform_admin` 四码；`role_auditor_internal` 仅 view 两码（V180，非本模块）。

只读成员证明（未 INSERT）：

```
booth_in_shape_check=f
public_area_in_shape_check=f
shop_in_shape_check=t
geojson_in_source_check=f
standard_json_in_source_check=t
null_in_check_is_unknown=t
```

PostgreSQL 对 `NULL IN (...)` 得 UNKNOWN，CHECK 会放行 NULL；本模块所有带 CHECK 的列均为 NOT NULL，故 **NULL 不能绕过这些 CHECK**。`project_id` 可空但无 CHECK。

## 4. 逐文件三线

### 4.1 V023__p1_floor_plan_polygons.sql（61 行，全读）

**第一性原理**

1. 为什么必须存在：删掉则 `mall_floor_plans` / `floor_plan_shapes` 不存在，治理地图 L472 正式平面图区域无处落库；`floor_plan.view/manage` 无种子。答得上，有业务上位。
2. 声称是否执行：表、索引、CHECK、权限种子均在 complex_test；Flyway 校验通过。`project_id` 声称归属综合体项目（实现层已必填），**列级未执行 NOT NULL/FK**（GR-M016-001）。`shape_type` 声称承载平面图区域，**闭集不含 HTML 点名的摊位/公共区**（GR-M016-002）。
3. 未验证就当真的前提：曾把「服务层必填」当成列级完整；本席用 `information_schema` / `pg_constraint` 证伪。

**顺从式**

- L1–16 建平面图表：`floor_no/plan_name/image_url/image_width/height/version_no/status/created_at/updated_at` NOT NULL；`project_id`/`building_code` 可空；`chk_mall_floor_plans_image_size`、`chk_mall_floor_plans_status(active/inactive/archived)`。
- L18–19 索引 `(project_id, building_code, floor_no, status)`。
- L21–37 形状表：FK CASCADE；`shape_type` 六值；`ref_type` 六值；`status` 三值；`polygon_points` 数组且长度≥3。
- L46–61 权限英文种子 + 仅 `role_platform_admin`。现网中文由 V251 覆盖。
- 与 FloorPlanService 白名单一致；与前端 `booth/public_area` 不一致。

**对抗式**

- 直接 SQL 可插入 `project_id=NULL` 或指向不存在的 projects.id（无 FK）。本席未执行 INSERT，以 `is_nullable=YES` + `fk_project=0` 为最小反例证据。
- `shape_type='booth'` / `'public_area'` 不在 CHECK 数组（SELECT 成员=f）。服务层 `SHAPE_TYPES` 同样拒绝，故 API 首轮也不会落库——这不是「服务救了 CHECK」，而是两层一起漏。
- 硬 DELETE 平面图会 CASCADE 删形状。应用代码 0 处 `DELETE FROM mall_floor_plans`；软删走 `status=archived`。按铁律 6 不把休眠 CASCADE 当首轮必撞缺陷。
- 无 `(project_id, floor_no, version_no)` UNIQUE。HTML 未要求一楼一版。观察。

### 4.2 V034__p2_floor_plan_import_tasks.sql（73 行，全读）

**第一性原理**

1. 删掉则导入任务/候选/错误无处落，L472「人工复核候选与错误后发布」做不到。
2. 三表、CHECK、索引、权限种子均已应用。`source_format` 闭集与服务层一致（svg/standard_json）；前端默认 geojson 会被服务层拒绝，进不了 CHECK——处置在 M018，不在本模块放宽 CHECK（HTML 沉默 geojson，不升冲突/添加）。
3. 前提：候选 `shape_type` 与正式形状同一闭集。证伪同 GR-M016-002。

**顺从式**

- 任务状态 `parsed/failed/published/cancelled`；候选 `pending/published/rejected`；计数 ≥0；`candidate_no>0`。
- `operator_note` 可空 VARCHAR(500)，仓库有读写（与 M015 观察「创建平面图的 operatorNote 不落 mall_floor_plans」不是同一列）。
- 权限 `import.manage` high + requires_approval=TRUE，对齐 L472「发布为高危动作」。

**对抗式**

- 候选 CHECK 同样拒绝 booth/public_area。
- `(import_task_id, candidate_no)` 仅有非唯一索引，无 UNIQUE。服务层按序编号；首轮正常 API 不并发双写同一序号。铁律 6 观察。
- 错误表无 `error_code` CHECK：错误码是技术通道，HTML 未闭集。细化，无缺陷。

### 4.3 V184__space_type_dictionary.sql（24 行，全读）

**第一性原理**

1. 删掉则 `createSpace` 硬校验无 active 码表，空间类型下拉无种子。上位是 L469 维护楼层/区域/点位/商铺等 + 后台可维护字典（实现注释）。HTML 未写死八个英文码。
2. 八行种子均在，全 active；`type_code UNIQUE` 在。
3. 注释声称 status=active/disabled，**库无 CHECK**（GR-M016-003）。`listActiveSpaceTypeCodes` 只认 `status='active'`，其它字符串事实停用。

**顺从式**

- 平台级、无 `project_id`：与「全综合体共享参考数据」一致。
- `ON CONFLICT DO NOTHING`：重跑不覆盖已改中文名，符合种子幂等。
- 无本表权限种子：复用 V005 `governance.space.manage`（`PlatformService.createSpaceType`）。本模块无重复授权发明。

**对抗式**

- `createSpaceType` `trimToDefault(request.status(),"active")` 无白名单，可写入 `inactive/foo`。首轮默认 active；要撞上需调用方显式传错 status。铁律 6：不把「管理员传错字」当必须扩 CHECK 的首轮缺陷，只登记。
- 八码 vs OpenAPI `mall/bar_table`、vs HTML「卡座/吧台位/游乐入口」名词：活校验＝字典、契约快照≠活闭集，已由 M013-004/M014-002 闭环。本模块不重开、不把 HTML 名词升格为必须多种子码（`other` 可承载）。
- 无 `mall` 码：与 M014-002 一致，不是本模块新根因。

### 4.4 V203__spaces_status_check.sql（6 行，全读）

**第一性原理**

1. 删掉则 `spaces.status` 无闭集，手填脏值会让 `spaceRentalStatus` 既认不出 unavailable、又可能把乱码当「可租」进分母（注释所述）。上位是 L259「不可租」作为人工标注 + L470「记录…状态」。
2. CHECK 已在；服务层 `SPACE_STATUSES` 一致；测试 `createSpaceRejectsStatusOutsideCheckSet` 本席实跑 PASS。
3. `UPDATE … SET status='active'` 会丢掉原脏值字符串。complex_test 当前 0 行 spaces，无脏值可丢。六态租赁不存在本列（由合同仓派生）。不把「CHECK 只有二值」判成与六态冲突。

**顺从式**

- 列已 NOT NULL（V005），CHECK 不能被 NULL 绕过。
- `DROP CONSTRAINT IF EXISTS` + `ADD`：手工重跑该 SQL 可过；Flyway 不会重跑已应用版本。
- 实现：`unavailable` 优先于合同派生（JdbcContractRepository L464）。

**对抗式**

- 若历史上把租赁态（let/vacant）写进 `spaces.status`，UPDATE 会全部变成 active，再靠合同重算。这是占用/租赁分列后的归一，不是把六态塞进 CHECK。已应用、当前 0 行。观察，不建议改写 V203。
- CHECK 作用于全部 space_type（含楼栋/公共区）。「不可租」原文在铺位租赁状态。分析分母只用 shop/booth。给非铺位标 unavailable 是占用开关，HTML 未禁止。不升冲突。

## 5. 十八冻结判据核对

本模块映射仅 GEN。无实现面的写明原因。

| ID | 适用 | 结论 | 证据 |
|---|---|---|---|
| GEN-001 | 是 | 四文件均有用途；无纯转发。入口/设施/风险区/路线是平面图标注细化，HTML 未点名但不构成无需求。不删表。 | 删除测试见 §0 codebase-design；HTML L469/L472 |
| GEN-002 | 是 | DDL 已应用；Flyway validate 390 通过；相关现有测试 29 PASS。**行为与 HTML 候选类型不完全一致**（见 002）。 | Flyway 日志；surefire |
| GEN-003 | 本模块无测试文件 | 由 M017 覆盖。本席实跑：`FloorPlanJdbcIntegrationTests#migrationCreatesFloorPlanTablesAndSeedsPermissions` PASS；`FloorPlanServiceTests` 27 PASS（含 `createFloorPlanRequiresProjectId` / `createFloorPlanRejectsUnknownProject`）；`PlatformServiceTests#createSpaceRejectsStatusOutsideCheckSet` PASS。缺口：无用例断言 booth/public_area 可落库——随 002 同批补在 M017，不单列 finding。未把「测试通过」当与 HTML 一致。 | surefire XML 02:03 时间戳 |
| GEN-004 | 是 | 服务层校验 project_id/shape_type/status；**列级 project_id 未校验存在性**（001）。字典 status 无 CHECK（003，队列否）。JSONB 只检 typeof=array 且长度≥3，顶点结构在服务层。 | pg_constraint；FloorPlanService L507/L533 |
| GEN-005 | 是 | 四码种子；manage/import.manage = high + requires_approval，对齐发布高危。本文件只授 platform_admin。审计只读由 V180 后加。无「仅靠界面隐藏」。 | permissions / role_permissions SELECT |
| GEN-006 | 是 | **FAIL 点：project_id 可空且无 FK**（001）。形状/导入 FK CASCADE 到平面图。V184 type_code UNIQUE。V203 CHECK + spaces.status NOT NULL。候选序号无 UNIQUE：观察。 | information_schema；pg_constraint |
| GEN-007 | 是 | 权限/字典种子 ON CONFLICT；PK；Flyway 不重跑已应用版本。服务层幂等键在 M015，非本 SQL。 | 迁移文本；Flyway no migration necessary |
| GEN-008 | 是 | **FAIL 点：shape_type CHECK ≠ HTML 摊位/公共区 ≠ 前端 booth/public_area**，但 = 后端白名单（002）。source_format = 后端 ≠ 前端默认 geojson（观察，改前端）。V184 八码 ≠ OpenAPI 快照（已闭环观察）。V203 二值 = 服务层 ≠ 六态存储（六态派生，非冲突）。 | SELECT 成员；前端 L17/L52；OpenAPI SpaceRequest |
| GEN-009 | 是 | 导入错误表 + failed 状态。V203 先 UPDATE 再 ADD CHECK，失败不会半加约束（同事务迁移）。无吞错伪成功于本 SQL。 | V034 L45–53；V203 |
| GEN-010 | 是 | 静态 SQL，无拼接用户输入；无密钥。权限码非秘密。 | 四文件全文 |
| GEN-011 | 是 | 证据在 flyway_schema_history + 权限种子。本模块无审计表。服务层写 audit event 属 M015。 | flyway SELECT；测试启动 validate |
| GEN-012 | 是 | 索引齐全；image_width/height>0；候选计数≥0。无无界批处理在 SQL。 | pg_indexes |
| GEN-013 | 是 | V 号 023<034<184<203，均小于当前 422；validate 通过。不依赖未声明本机状态。 | flyway_schema_history |
| GEN-014 | 是 | 已应用且 checksum 通过 validate，**不可改写这 4 个文件**；缺陷只能新 V。幂等：IF NOT EXISTS / ON CONFLICT。脏值归一见 V203 观察。CHECK 列 NOT NULL，NULL 不绕过。001/002 的修复必须新 V，且提示要求同批核 data-isolation spec（`mall_floor_plans` 已在 spec L167：merchant_id+project_id / V023+V052）。 | DbValidate 390；spec L167 |
| GEN-015 | 权限名面向操作员 | 本文件英文种子；**现网中文**（V251 L193–196）。不建议改写已应用 V023/V034。观察 OBS-01。 | permissions SELECT |
| GEN-016 | 注释 | V184/V203 中文注释与实现分列一致；注释本身不是 2.3 依据。data-isolation spec 已列 mall_floor_plans。 | 文件原文 |
| GEN-017 | 是 | 分类顺序冲突→细化→技术性→添加。001/002=细化+缺陷；003=细化/技术性无首轮必修；无添加项。未把 HTML 沉默当冲突。 | §7 |
| GEN-018 | 是 | 结论落到 HTML 原文、Flyway/测试产物、PostgreSQL 文档性行为（NULL CHECK）、可复现命令。未用惯例/历史席位意见当依据。 | 本文命令节 |

## 6. Findings（临时 ID，正式 ID 仅 B2 分配）

### GR-M016-001

- **文件:行号**：`backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql:3`（`project_id VARCHAR(64),` 无 NOT NULL）；对照实现 `FloorPlanService.java:507-510`；实态 `information_schema.columns` `mall_floor_plans.project_id` `is_nullable=YES`；`fk_project=0`。
- **完整原文**：`    project_id VARCHAR(64),`
- **业务影响**：平面图声称归属综合体项目（治理 L463/业务地图 L700；实现已必填并查 `projects`）。列级可空 + 无 FK，允许 SQL/仓库路径写入 `project_id=NULL` 或指向不存在项目，隔离索引与 `findAllFloorPlansByProject` 会漏行或挂孤儿。M015-002 应用层已修，列级明确归本模块。
- **可复现命令/最小反例**（只读，未 INSERT）：
  - `SELECT column_name, is_nullable FROM information_schema.columns WHERE table_name='mall_floor_plans' AND column_name='project_id';` → `project_id | YES`
  - `SELECT COUNT(*) AS fk_project FROM pg_constraint con JOIN pg_class c ON c.oid=con.conrelid WHERE c.relname='mall_floor_plans' AND con.contype='f' AND pg_get_constraintdef(con.oid) ILIKE '%project_id%';` → `0`
  - 服务层反例已有测试 `createFloorPlanRequiresProjectId` / `createFloorPlanRejectsUnknownProject`（本席 27/27 PASS），证明**假设**是必填且须存在，与列级不一致。
- **冻结判据**：GEN-006、GEN-014、GEN-008（列可空性与实现假设）
- **2.3 依据类型**：4（可复现命令）兼 2（服务层测试实际拒绝缺 project_id）
- **四分类**：细化（上位：空间/平面图归属综合体项目；不是新业务实体）
- **工程缺陷**：成立
- **建议入队**：是
- **技术性「无业务发明」**：不适用（走细化）
- **最小处置**：不得改写已应用 V023。新 V：`ALTER TABLE mall_floor_plans ALTER COLUMN project_id SET NOT NULL;`（complex_test 当前 0 行平面图，无回填；铁律 8 不为存量兼容）。再按 V159 口径加 `FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE RESTRICT`（配置主数据硬删应拦，不用 CASCADE）。同批核/重生成 `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md`（表已列 mall_floor_plans，核 project_id 必填是否写清）。服务层已必填，不必再发明新 API。

### GR-M016-002

- **文件:行号**：`V023__p1_floor_plan_polygons.sql:33`；`V034__p2_floor_plan_import_tasks.sql:36`；对照 `FloorPlanService.java:34`；前端 `apps/web/src/admin/pages/FloorPlanImportPage.tsx:17`；HTML 治理地图 L472。
- **完整原文**：
  - V023: `CONSTRAINT chk_floor_plan_shapes_shape_type CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route')),`
  - V034: `CONSTRAINT chk_floor_plan_import_candidates_shape_type CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route')),`
  - HTML L472：`系统解析出候选区域（铺位、摊位、公共区）`
- **业务影响**：摊位/公共区无法作为 `shape_type` 落正式形状或导入候选（CHECK + 服务白名单双拒）。前端已准备 `booth`/`public_area` 标签。与 M015-013 转入口径独立复核后仍成立。`corridor` 仅前端有、HTML 未写，**不**并入本 CHECK（否则是添加）。
- **可复现命令/最小反例**：
  - `SELECT 'booth' = ANY (ARRAY['area','shop','entrance','facility','risk_zone','route']::text[]) AS booth_in_shape_check, 'public_area' = ANY (ARRAY['area','shop','entrance','facility','risk_zone','route']::text[]) AS public_area_in_shape_check;` → `f, f`
  - `pg_get_constraintdef` 两约束仍为上述六值。
  - 未手工 INSERT（禁写）。
- **冻结判据**：GEN-008、GEN-014、GEN-006（闭集约束与业务对象不一致）
- **2.3 依据类型**：1（HTML L472 逐字）+ 4（SELECT/pg_constraint）
- **四分类**：细化（遗漏）。上位条目即 L472 候选区域（铺位、摊位、公共区）；闭集过窄是实现遗漏，不是另造相反规则。不把 HTML 未点名的 entrance/facility/risk_zone/route 判冲突（那些是平面图标注细化）。
- **工程缺陷**：成立
- **建议入队**：是
- **最小处置**：不得改写 V023/V034。新 V：两表 `DROP CONSTRAINT` 后 `ADD CHECK (shape_type IN ('area','shop','entrance','facility','risk_zone','route','booth','public_area'))`。同批改 M015 文件 `FloorPlanService.SHAPE_TYPES`（及解析器若仍不校验闭集则保持「非白名单整单失败」）。前端 `corridor` 不入库。新 V 即使只改 CHECK，仍按提示同批核 data-isolation spec 无漂移。测试补在 M017。

### GR-M016-003

- **文件:行号**：`V184__space_type_dictionary.sql:8`（`status VARCHAR(32) NOT NULL DEFAULT 'active',   -- active/disabled`）；`pg_constraint` 该表 `contype='c'` 计数 0。
- **完整原文**：`    status VARCHAR(32) NOT NULL DEFAULT 'active',   -- active/disabled`
- **业务影响**：字典状态无闭集，`createSpaceType` 可写入任意非空字符串；仅 `status='active'` 进入 `createSpace` 硬校验。错值表现为「假停用」，不破坏占用/租赁。
- **可复现**：`SELECT COUNT(*) AS chk_dict_status FROM pg_constraint con JOIN pg_class c ON c.oid=con.conrelid WHERE c.relname='space_type_dictionary' AND con.contype='c';` → `0`。complex_test 8 行全是 active。
- **冻结判据**：GEN-008、GEN-004
- **2.3 依据类型**：4
- **四分类**：细化（字典可维护状态的下位实现）兼技术性（无新业务概念；注释已写 active/disabled）
- **无业务发明理由**：HTML 未规定字典行状态码；active/disabled 只是启用/停用实现开关，不引入新业态或流程。
- **工程缺陷**：成立（注释闭集未落约束），但 **首轮正常路径默认 active、现网 8 行均为 active**；要撞上需调用方传错 status。铁律 6 收窄。
- **建议入队**：否
- **最小处置**：登记。若日后新 V 已因 001/002 打开，可顺手加 `CHECK (status IN ('active','disabled'))`，不单独为一件事加迁移。

## 7. 添加项

无。不把前端 `geojson`/`corridor`、OpenAPI `mall`、HTML「卡座/吧台位」名词写成必须新增的 CHECK/种子码。

## 8. 观察（无新根因或队列否）

| ID | 内容 | 处置 |
|---|---|---|
| OBS-01 | V023 L47–49 / V034 L60–61 `permission_name` 英文。现网已被 V251 改为中文。Fresh migrate 顺序 V023→…→V251，首轮结束后操作员看到中文。 | 不改写已应用迁移；不新 V 重复 V251 |
| OBS-02 | 前端默认 `sourceFormat: "geojson"` 与 V034 CHECK `svg/standard_json` 及服务层白名单不一致。服务层会 VALIDATION_FAILED，进不了库。HTML 未写格式名。 | 改前端默认属 M018；本模块不放宽 CHECK |
| OBS-03 | 前端 `corridor`、OpenAPI `SpaceRequest.space_type` 含 mall/bar_table。活校验＝V184。 | 已闭环 M013-004/M014-002；本模块不重开 |
| OBS-04 | V203 UPDATE 脏值→active 会丢原字符串。当前 spaces 0 行；六态在合同仓派生。CHECK 二值与 `requireSpaceStatus` 一致。 | 不改写 V203 |
| OBS-05 | 子表 `ON DELETE CASCADE`。应用 0 处 DELETE 平面图；软删用 archived。 | 铁律 6 不修 |
| OBS-06 | `(import_task_id, candidate_no)` 无 UNIQUE。 | 铁律 6 不修 |
| OBS-07 | V052 `merchant_id` 可空无 FK，非本 4 文件。 | 不纳入本模块修复 |
| OBS-08 | L470 用途/负责人：`spaces` 无这些列（V005，M010）。M015-012 已登记全局遗漏，补列不在本模块加迁移。 | 不重开 |
| OBS-09 | `role_auditor_internal` 持有 view 两码来自 V180，不是 V023/V034 越权授予 manage。 | 正向 |
| OBS-10 | M017 `FloorPlanJdbcIntegrationTests.saveAndList…` 走仓库 INSERT `prj_jdbc`，不经 `projectExists`。本席未跑该方法。 | 属 M017；001 修列级后该测试若无对应 projects 行会红，同批夹具 |
| OBS-11 | Flyway checksum 算法 ≠ 本席 zlib.crc32；以 `DbValidate Successfully validated 390 migrations` 证明文件与已应用 checksum 一致。 | 正向 GEN-014 |
| OBS-12 | 环境 URL 库名 `complex`；证据以显式连接的 `complex_test` 为准。曾对 `complex` 只读 SELECT 失败于 ORDER BY 别名，无写。 | 记录 |

## 9. 命令证据摘要

```
sha256sum criteria-lock.md → b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
git rev-parse HEAD origin/main → fe50e3f309c04f81820f8414bd8860325d694098 ×2
git ls-files -z -- <4> | xargs -0 cat | wc -l → 164
4 文件 python sha256/行/字节 → 与开工清单 match=True
psql -d complex_test → current_database=complex_test；V023/034/184/203 success=t
  project_id is_nullable=YES；fk_project=0；shape CHECK 六值；dict CHECK 0；spaces 0 行
  permissions 四码中文；booth_in_shape_check=f
mvn -o -q -Dtest=FloorPlanJdbcIntegrationTests#migrationCreatesFloorPlanTablesAndSeedsPermissions,PlatformServiceTests#createSpaceRejectsStatusOutsideCheckSet,FloorPlanServiceTests test
  exit 0；surefire：JDBC 1/1、Service 27/27、Platform 1/1，failures=0
  过程日志：Flyway validate 390 OK；schema v422 up to date
git status --porcelain →  M docs/audit-2026-09/ledger.md
本 4 文件 / 三 HTML / 锁 / findings / modules.md diff --stat 空
```

未运行、因而不声称通过：全后端测试、部署、`flyway:migrate`、任何 INSERT/UPDATE/DELETE。

## 10. 审计结论

**FAIL**

理由：整模块四文件与 18 条 GEN 已三线审完；存在未修复的工程缺陷 GR-M016-001（列级 project_id 可空且无 FK）与 GR-M016-002（shape_type CHECK 不能落摊位/公共区）。无添加项、无未答人闸、无 HTML 人闸。不存在有条件通过。B1 禁止修复，本席未修。

建议 B2 队列：001 是、002 是、003 否；观察全否。修复只能新 V 号，禁止改写已应用的 V023/V034/V184/V203。

## M016 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 35809 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6 xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（铁律共 **7** 条，无「铁律 8」）；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M016 FILES 4 / 锁映射 18 / 计划冻结 164 行）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `fe50e3f309c04f81820f8414bd8860325d694098`（PR #436 M015 闭环检查点已合并）。本模块 4 文件指纹与 B1 覆盖表全等。B3=0。无源修复。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类。
- 本席 B2-r1：**PASS**（001–004 逐 ID 全 PASS。添加 0、不发 2.2。非阻断勘误见各 ID，不否决）
- 正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 `M016 → [GEN-001 … GEN-018]`（`criteria-lock.md` L6395）COUNT=18 unique=18。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要复现只读（迁移/Java/前端全文与 `rg`；未对任一 `complex_test*` 库做写操作，本批未再连库）。

此票只判断 findings.md V1.95 顶部「当前 M016 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。ledger 所记 m016_scratch 干跑 **不是** 本票依据。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M016-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 35809 | `decd1a0138575976de7128cc400fc4b7c26db94de8c54597cb2c3654ca7c0cef` |
| `docs/audit-2026-09/modules/M016-b1-self-raw.md` | V0.01 | 30020 | `59d183dfab0fe56ef32db60afde616207509fac7a602a10396e8e78810a6661a` |
| `docs/audit-2026-09/findings.md` | V1.95 | 782108 | `74dc52d6d4d730919d9bd618975133f7214b8f19153e352bd97dc11c1bd06383` |

ledger 只读：V3.39，602180 bytes，SHA256 `44f949cdbe373a7b9ef880d6edfaebec67d9e285b9bcc52247f542dcce784639`；M016 IN_PROGRESS、B3=0。4 文件 `git ls-files -z -- <4 paths> | xargs -0 cat | wc -l` = **164**，与计划冻结一致。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

controller / runbook / modules.md 只读核验（本席未改；与 B1 同）：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |

三 HTML 只读核验（本席未改；与 B1 同）：

| 文件 | 现行 | SHA256 |
|---|---|---|
| 业务地图 | V0.14 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| 治理地图 | V0.12 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| 枚举速查 | V0.07 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

工作区只读：`git status --porcelain` = `M findings.md`、`M ledger.md`（席甲落候选）、本两份 raw 未跟踪。本席只追加本指定路径。

本模块 4 文件指纹（与 B1 / 开工清单 match）：

| 路径 | 行 | bytes | SHA256 |
|---|---:|---:|---|
| `V023__p1_floor_plan_polygons.sql` | 61 | 2828 | `141970f242484accdf99ac37a896ed04762677f5c41d1d4808b2be0ba13815e6` |
| `V034__p2_floor_plan_import_tasks.sql` | 73 | 3658 | `fd9200070b4f4439c58cb32f4a03d6eb7f62b16c47320a57c2645ab2f101a56b` |
| `V184__space_type_dictionary.sql` | 24 | 1535 | `238d1529736908fcf3ff5346e0c7af4636361e035cb7629b27f8729b4895cc52` |
| `V203__spaces_status_check.sql` | 6 | 540 | `a159a62c1b8f23a82ddb002a6a625870c469a9250001ed51743a8cfcd3cdb83f` |

当前最高已应用迁移文件为 V422；仓内无 V423。`REFERENCES projects` 在 `backend/src/main/resources/db/migration/*.sql` 全文 **0** 处。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核源/HTML/命令再接受或反驳；禁止表演性同意；B2 不修复。本批重点反驳本席 B1 的 SET NOT NULL+FK 与「铁律 8」 |
| grilling | 只借追问结构自查：001 NOT VALID 是否仍闭合列级必填、FK 是否仓内范式、002 八值是否最懒、003 队列「是」是否改写「否」、004 是否写错 OBS-02/OBS-10 |
| verification-before-completion | 声称前先跑只读哈希/行号/源码/`rg` |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 删除测试：本模块深度落在四迁移的列级契约与 CHECK 闭集，不落前端/OpenAPI |
| ponytail-review | 只评最小删除/收窄，不落地。001 不加 FK、不做 SET NOT NULL；002 不含 corridor；003 顺手进已开的 V423，不单开迁移 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |

### 完整差集（GR-001…003、OBS-01…12、席甲 CX-001…003、OBS-1…10）

漏并：无。两席 B1 全部临时 ID 仍落在 001–004。无单席独有缺陷。本席 GR-001 原最小处置（SET NOT NULL + FK）被候选换成 NOT VALID CHECK、不加 FK——**缺陷原意未改写**，修法按铁律 6 / GEN-014 / 仓内范式重裁（见 001）。本席 GR-003 队列「否」加「新 V 已开可顺手加」被候选置「是」——**原意是不单开迁移，不是禁止进已开的 V**，不构成改写。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M016-001 | GR-M016-001 + CX-M016-002（转入 M015-002） | 细化/是/是 | **PASS（细化/是/是；NOT VALID CHECK、不加 FK 最懒可部署）** |
| M016-002 | GR-M016-002 + CX-M016-001（转入 M015-013） | 细化（遗漏）/是/是 | **PASS（细化遗漏/是/是；八值 + 同批白名单，不含 corridor）** |
| M016-003 | GR-M016-003 + CX-M016-003 | 技术性/是/是 | **PASS（技术性/是/是；顺手同一 V423 合规，归 disabled 保持行为）** |
| M016-004 | 双方观察（OBS-01…12 + OBS-1…10） | 技术性/正向 /否/否 | **PASS（否；OBS-02→M018、OBS-10 不加 FK 故夹具不受影响，未写错）** |

冲突 0。细化入队 2（001、002）。技术性入队 1（003）。观察否 1（004）。添加 0。questions 0。无 HTML 修改申请。

### 001 独立裁定（细化／是；候选取 NOT VALID CHECK、不加 FK）

本席 B1 GR-001 最小处置写过 `ALTER COLUMN project_id SET NOT NULL` 再 `FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE RESTRICT`，并引「铁律 8 不为存量兼容」。本票按控制合同与仓内源码重裁，**不沿用那两步**。

核源（只读）：

- `V023:3` `project_id VARCHAR(64),` 无 NOT NULL、无 FK。`spaces.project_id` 同为 `V005:57` `VARCHAR(64)` 可空、无 FK。
- M015 已闭环：`FloorPlanService:508` `projectExists`；`JdbcFloorPlanRepository:65-68` `SELECT COUNT(1) FROM projects WHERE id = :id`。仓库 `saveFloorPlan:83-105` 仍原样 INSERT `project_id`，不拒 NULL。
- M015-002 定稿转入口径是「列级 NOT NULL 归 M016」，**没有**要求 FK。
- `rg 'REFERENCES projects' backend/src/main/resources/db/migration` → **0 处**。带 `project_id` 列的 CREATE/ALTER 表远多于候选所写「20」（本席按迁移文本实算 99，其中含 amusement 等非综合体 `projects.id` 语义）。关键事实是 **0 张 REFERENCES projects**，不是「恰好 20」。
- V084 L16 原文是 `bar_tables.space_id` / `amusement_projects.space_id`「软引用…不强制 FK」，不是 `spaces.project_id` 的列注释。软引用作为仓内身份列惯例仍成立，引用点应写 V005/V023 列定义本身。
- V159 记录的是票务血缘 FK「NOT VALID + ON DELETE RESTRICT」，不是 `project_id → projects` 的通用范式。同仓 CHECK NOT VALID 先例在 V339/V399/V046（放行存量、对新写强制）。
- `FloorPlanJdbcIntegrationTests:57/113` 直接仓库 INSERT `prj_jdbc`，不经 `projectExists`、不造 `projects` 行。加 FK 会迫使该夹具（M017）造主表行。
- 控制合同 V0.17 铁律 1–7。本席 B1「铁律 8」来自仓库 AGENTS.md 项目规则，**不是**本审计冻结铁律，本批不得当判据。铁律 6：收窄已有规则；不为「以防万一」加新范式；修法优先删除/收窄，禁止发明归属值。

四分类：**细化**。上位是综合体项目归属（治理 L463 / 业务地图 L700；M015-002 服务层已落地）。列级可空是实现缺口，不是新业务实体。工程缺陷成立。无业务发明。

SET NOT NULL 不可取：PostgreSQL `SET NOT NULL` 立即全表校验；生产若有存量 NULL，迁移失败，且没有可归一的 `project_id`（发明归属＝业务发明）。complex_test 当前 0 行不能外推生产。GEN-014 要求迁移能在目标库执行。

候选取 `CHECK (project_id IS NOT NULL) NOT VALID`（前置 `DROP CONSTRAINT IF EXISTS`）：新 INSERT/UPDATE 强制非空，存量 NULL 放行、不发明值；UPDATE 触及 NULL 行时仍会受 CHECK。写路径强制与「列级必填」等价，`is_nullable` 元数据仍为 YES——不否决，本批不必 `VALIDATE` / `SET NOT NULL`。

不加 FK：与仓内 `project_id` 零 FK 惯例一致；服务层已 `projectExists` 拦 API 孤儿；加 FK 是新范式，并误伤 M017 `prj_jdbc`。孤儿直 SQL 不是铁律 6「第一次正常跑就会撞上」的路径。

最小处置（维持候选）：新 V423 `ALTER TABLE mall_floor_plans ADD CONSTRAINT chk_mall_floor_plans_project_id_required CHECK (project_id IS NOT NULL) NOT VALID`；TDD `FloorPlanJdbcIntegrationTests` 空 `project_id` 落库被拒一例先红。禁止 FK、禁止 SET NOT NULL、禁止回填归属。同批核/重生成 data-isolation spec（表已在 spec L167）。

非阻断：候选「20 张表」计数不准；V084 引用点应改 V005/V023。不改分类/队列/处置。

票：**PASS**。

### 002 独立裁定（细化遗漏／是；与 GR-002 一致）

核源：

- `V023:33` / `V034:36` CHECK 六值 `area/shop/entrance/facility/risk_zone/route`，无 booth/public_area。
- 治理地图 L472：「系统解析出候选区域（**铺位、摊位、公共区**）」。
- V184 种子已有 `booth`/`public_area`。前端 `FloorPlanImportPage.tsx:17` `SHAPE_TYPE_LABELS` 含 booth/public_area，另有 HTML 未写的 `corridor`。
- `FloorPlanService.java:2` 文件版本 V0.02；`:34` `SHAPE_TYPES` 仍六值。`FloorPlanServiceTests` 无 booth/public_area 用例。
- M015-013 定稿：CHECK 放宽须新迁移，白名单同批；队列否是因为当时不在 M015 23 文件改 CHECK。M015 已 PASS，本批改 `SHAPE_TYPES` 一行是该 XM 授权的同批动作，不是重开 M015。

四分类：**细化（遗漏）**。闭集过窄，不是另造规则。`entrance/facility/risk_zone/route` 是平面图标注细化，HTML 未点名不升冲突。`corridor` 仅前端有、HTML 未写，**不得**进 CHECK（否则添加）。

最小处置与 GR-002 同支且最懒：同一 V423 两表 `DROP CONSTRAINT IF EXISTS` 后重建八值（既有六值 + booth、public_area）；同批 `SHAPE_TYPES` 加这两码（版本头 V0.02→V0.03）+ `FloorPlanServiceTests` booth 被接受一例先红 + `FloorPlanJdbcIntegrationTests` booth/public_area 落库一例先红。解析器仍不放宽闭集——非白名单在服务 `normalizeAllowed` 整单失败，保持现网。前端 corridor/geojson 默认属 M018，本模块不放宽 `source_format` CHECK。

票：**PASS**。

### 003 独立裁定（技术性／是；队列「是」合规）

本席 B1：缺陷成立，建议入队 **否**（首轮默认 active、现网 8 行均为 active，要撞上需传错 status；铁律 6 收窄），并明示「若日后新 V 已因 001/002 打开，可顺手加 CHECK，不单独为一件事加迁移」。

001/002 已打开 V423。候选把 003 置「是」并写入同一 V，是在执行该顺手条款，**不是**把「否」改写成「必须单开迁移」。分类取 **技术性**（闭集来自 V184:8 自身注释 `active/disabled`，HTML 未规定字典行状态码）比本席 B1「细化兼技术性」更贴 GEN-017；无业务发明。

核源：`pg` 侧本席 B1 已见该表 CHECK 0；`createSpaceType:457` `trimToDefault(request.status(),"active")` 无白名单；`listActiveSpaceTypeCodes:319` 只认 `status='active'`；`saveSpaceType` upsert 原样落库。脏值已经不在下拉/硬校验中，事实等于停用。

归一方向：候选 `UPDATE … SET status='disabled' WHERE status NOT IN ('active','disabled')` 后 `ADD CHECK (status IN ('active','disabled'))`。V203 的「先 UPDATE 再 ADD CHECK」是步骤范式；V203 **目标值**是 `active`（占用列、拍板把脏值当可租）。字典脏值若归一成 `active` 会突然进入下拉，改变现网行为。归 `disabled` 才保持「假停用」。complex_test 八行均 active 时 UPDATE 为空操作。服务层 status 白名单属 M009/M011（`PlatformService`），本模块不改——CHECK 拦直 SQL / 漏网写入即可。

TDD：`PlatformJdbcIntegrationTests` `status='Active'` 被拒。该文件在 **M011 FILES**（`modules.md` L178），不在 M017。非阻断：B3 边界应写作 M011，不否决。

票：**PASS**。

### 004 独立裁定（观察／否；未写错应升项）

候选来源格写双方观察全集。应入队项均在 001–003；无观察被偷偷升「是」，无缺陷被降观察。

逐条核本席观察是否被写错：

- OBS-01 英文种子 / V251 中文 → 事实栏有。
- **OBS-02** 前端默认 `sourceFormat: "geojson"`（`FloorPlanImportPage.tsx:52`）与 V034 CHECK `svg/standard_json` 及服务白名单不一致；服务层拒绝、进不了库 → 事实栏有，处置 **M018 登记**，本模块不放宽 CHECK。未写错。
- OBS-03 corridor / OpenAPI `mall`：corridor 已由 002「不含 corridor」+ M018 收口；OpenAPI 活校验＝V184 已由 M013-004/M014-002 闭环。事实栏未点名 OpenAPI 快照——非阻断摘要压缩，不是把应升项留下。
- OBS-04 V203 归一 active + 六态在合同仓 → 有。
- OBS-05 CASCADE 惰性 → 有。
- OBS-06 候选序号无 UNIQUE → 有（PK 派生）。
- OBS-07 V052 `merchant_id` 非本 4 文件 → 有。
- OBS-08 用途/负责人＝M015-012 全局遗漏 → 有。
- OBS-09 审计只读来自 V180 → 有（与席甲 OBS-2 合并）。
- **OBS-10** `prj_jdbc` 不经 `projectExists`：核源 `FloorPlanJdbcIntegrationTests:55-69/111-125` 成立。因 001 **不加 FK**，该夹具不受影响——事实栏如此写，与 001 裁定合取，未写错。若误加 FK 则本句会假。
- OBS-11 Flyway validated 390 → 有。
- OBS-12 环境 URL `complex` vs 证据库 `complex_test`：过程记录，不进 finding 事实栏。不构成写错。

席甲 OBS-8（V023/V034 无头注释）未进事实栏，属 GEN-016 登记，不升缺陷。非阻断。

票：**PASS（否）**。

### 分类 / 队列 / 允许的最小修复边界

本席 B2-r1 **PASS**。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3 只新增 `V423__m016_floor_plan_and_space_type_checks.sql`（不改写 V023/V034/V184/V203）及下列同批文件，不改三 HTML、OpenAPI、前端、锁、权限种子、解析器闭集。

- 001：`CHECK (project_id IS NOT NULL) NOT VALID`；TDD 空 project_id 落库被拒。禁止 FK / SET NOT NULL / 回填。
- 002：两表 CHECK 八值（+booth、+public_area，不含 corridor）；`FloorPlanService.SHAPE_TYPES` 同批；`FloorPlanServiceTests` + `FloorPlanJdbcIntegrationTests` 先红。解析器不放宽。
- 003：同一 V423：脏值先归 `disabled` 再 `CHECK (status IN ('active','disabled'))`；TDD 在 M011 `PlatformJdbcIntegrationTests`（候选写作 M017 为模块号笔误）。服务层 status 白名单不改。
- 004：不修。geojson/corridor 不入库。
- 同批 `npm run docs:data-isolation-spec` / `docs:domain-ownership-scope` 仅因 001–003 约束变化重生成门禁产物。

禁止：加 FK、`SET NOT NULL`、改前端/OpenAPI/三份 HTML/锁/权限种子/解析器闭集、把 corridor 或 geojson 入库、单为 003 另开 V。

改动文件自身版本递增。

### 结论

全批差集无漏并、无改写本席原意。001 列级必填成立，修法改 NOT VALID CHECK、不加 FK，比本席 B1 的 SET NOT NULL+FK 更懒且可部署；「铁律 8」不在控制合同，本批作废该引用。002 与 GR-002 一致。003 队列「是」是顺手条款落地，归 disabled 保持假停用。004 未写错 OBS-02/OBS-10。逐 ID：001 PASS、002 PASS、003 PASS、004 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M016 IN_PROGRESS；B3=0。
