CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M017 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 修复前整模块全量审计
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 模块：M017 — 空间点位、楼层平面图与营业时段／后台验证／第1段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 5 文件，计划冻结有效内容行 1352；`git ls-files -z -- <5 paths> | xargs -0 cat | wc -l` 实算 **1653**（M015/M016 B3 已改其中 3 文件，与开工提示一致）
- 冻结判据：锁映射行 `M017 → [BEQR-063 … PGM-066]`（`criteria-lock.md` L6396）实算 **90** 个唯一 ID；与开工清单逐字相同（MISSING=[] EXTRA=[] COUNT=90 unique=90）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M017-001、GR-M017-002、GR-M017-003、GR-M017-004、GR-M017-005、GR-M017-006、GR-M017-007、GR-M017-008
- 只登记、建议队列「否」：GR-M017-009（XM 回 M015，本模块不得改实现）；观察见 §8
- 添加项（B1 只登记，不代用户回答 2.2；本批无）：无
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做手工写操作（仅 SELECT 约束/残留探针；现有 `FloorPlanJdbcIntegrationTests` 由 Spring `@Transactional` 回滚）
- 本报告不表示全仓或发布门禁通过。M001–M016 及 U01–U18 已闭环，不重开。原 322 模块编号及旧结论不代表当前 471 模块完成。findings V1.97 仅作已登记边界背景（含 M015 定稿 001–014、M016 定稿 001–004 及两模块测试面观察），不代替本模块独立审计。

本模块是后台验证（测试）类：三线对象是「这 5 个测试类是否真能锁住 HTML 规则与已定稿 M015/M016 修复」。被测实现已闭环；本模块修复只能改这 5 个测试文件。实现缺陷登记 XM，不在本模块改实现。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| grilling | `.../productivity/grilling/SKILL.md` | 只用追问树自己取证（403 逐端点、13 处 error_message、200 上限、诚实分页、corridor/candidates CHECK、SQL 夹具、审计 mock），不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 内部 sharpen；该文件要求 `/grilling`，本席不向操作员发问 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 删除测试：删掉 5 类则 PGM-023 导入/发布、BEQR-152 项目隔离、V423 列级约束在验证面全部失去可红反馈。不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（sha256 / `mvn -Dtest` / `pg_constraint` SELECT / 变异「删哪条校验哪条用例红」）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（HTML 原句→5 测试类→FloorPlanService / Controller / Parser / JdbcFloorPlanRepository / VenueBusinessHoursService / V423 / OpenAPI） |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；58 PASS ≠ 与 HTML/已定稿修复同锁 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地：本 5 文件无额外抽象层可删；缺口是缺用例不是多用例 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推、未安装 hook |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒级前台 `mvn`，未起 monitor |
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
- sidecar `plan/criteria-lock.sha256` 同值
- `criteria-lock.md` 版本 V0.11；判据总数 401；`LOCK-AMEND-COUNT: 1`（ledger 顶部）
- 映射行 `M017 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-170, BEQR-189, BSM-001, BSM-003, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-022, PGM-023, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-060, PGM-065, PGM-066]`（L6396）与开工清单 90 ID 逐字相同
- `plan/modules.md` M017 FILES 5 条与用户清单逐字相同
- 5 文件字节/SHA256/行数与用户固定清单 **match=True**（见 §2）

HEAD：`main` = `origin/main` = `e2abb037001ccd72650efa3cd86e9b1b4bfe3ea0`（`rev-parse HEAD` / `origin/main` 两值相等）。本席未设 `GIT_INDEX_FILE`，未动主索引。

工作区：`git status --porcelain` = ` M docs/audit-2026-09/ledger.md`（M017 PENDING→IN_PROGRESS 的台账写入）。开工提示称「工作区干净」与此不完全一致；本 5 文件与三 HTML、锁、findings、modules.md 未改。本席只读，未再写 ledger。

ledger 只读核验（不改）：V3.44，609331 bytes，SHA256 `e477ff53d0167bf2bc7c763602a941473264612ea69a8f088dd29d0624032c3c`；M017 行 IN_PROGRESS、B3=0。findings V1.97，785724 bytes，SHA256 `8b0f08020e0ed05d2c4c0a936eab9855f2bfb9245f6b16c26d5461c9440a7026`，仅作已登记边界背景，不代替本模块审计。

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

三份 HTML 只读、未改。锁内本模块映射 BEQR 22 + BSM 14 + GEN 18 + PGM 36 = 90。HTML 作 2.3 类型 1 证据。对本席读取的现行 HTML，治理地图 L347/L463/L467–L472、枚举 L259「铺位租赁状态」、枚举 L187「运营营业日」（**未映射本模块**，只作对照）均 hit=True。BEQR-187 未映射，场次开歇测试锁的是 `VenueBusinessHoursService` 星期窗口，不是营业日 10:00 配置（V201 非本模块，与 M015-014 口径一致）。

三 HTML 当前字节/SHA256（只读核验，本席未改）：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| docs/business-enum-quick-reference-v0.1.html | 133961 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | 304482 | eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 |
| docs/platform-governance-map-for-operations-v0.1.html | 103015 | a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 |

三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。

## 2. 五文件清单与覆盖

`git ls-files -z -- <5 paths> | xargs -0 cat | wc -l` = **1653**。每文件完整通读（非标题/搜索/截断）。独立 `sha256sum`/`wc` 与开工固定清单逐文件 match。

| 路径 | 行 | 字节 | SHA256 | 与开工清单 |
|---|---:|---:|---|---|
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java | 407 | 17103 | ea7362efb0b519e3b711b54f52f9df9134ee510943859f0e745936e8e9f12795 | match |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java | 112 | 3859 | 597735f13baa6b08ed41f60264827377b5c6e0e3dc861e4e2eeaa5de166e44b1 | match |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java | 244 | 9639 | 018480bf503be1579a00ce6c36ea83b283c24951c161a581029ea17c33f9e9e9 | match |
| backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java | 715 | 32109 | 03a59adf8c527f0dd92fb42e49188a8c3334ad685d82829621ed449e6e874fe2 | match |
| backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java | 175 | 7649 | bf1a9d3232f5318a2ab15313533a54c255efe77831589af9ca256ff8c477cc4b | match |

覆盖：Controller L1–407、Parser L1–112、Jdbc L1–244、Service L1–715、VenueHours L1–175 均已整文件阅读。无 OVERSIZE。`@Test` 计数：12 + 4 + 5 + 28 + 9 = **58**。

## 3. 数据流（HTML → 已闭环实现 → 本模块 5 测试）

### 3.1 HTML 原句（类型 1）

治理地图 `docs/platform-governance-map-for-operations-v0.1.html`：

- L347 卡片「空间和点位」：管理楼层、区域、卡座、吧台、游乐点位、库房和设备位置。
- L463 h2「空间和点位：所有业务都要知道“发生在哪”」
- L469：维护楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位。
- L470：记录空间面积、用途、状态、负责人和所属商户。
- L472：商场平面图网页导入：在后台上传或导入商场平面布局图，系统解析出候选区域（**铺位、摊位、公共区**），由人工复核候选与错误后发布到正式平面图；**发布为高危动作，须二次确认——发布会写入正式平面图区域，不可静默覆盖既有布局证据**。

枚举速查 L259：铺位租赁状态＝空置、洽谈中、已租、即将到期、已退租、不可租。

枚举速查 L187「运营营业日」（未映射）：10:00→次日 10:00。本模块营业时段测试锁的是场地星期开歇窗口，不是该条。

业务地图 L698：建立综合体项目、楼层、区域、点位。

BEQR-152：所有数据默认按项目隔离，普通岗位不得跨项目查看。

### 3.2 已闭环实现（只读对照，不重开 M015/M016）

- `FloorPlanController` 8 端点：list/create/listShapes/saveShape/listImports/createImport/listImportCandidates/publish；权限 `floor_plan.view|manage` 与 `floor_plan.import.view|manage`；写路径 `requireIdempotencyKey`。
- `FloorPlanService` V0.03：`SHAPE_TYPES` 八值（含 booth/public_area）；`MAX_PUBLISH_CANDIDATES=200`；三处 `PageResponse` 诚实 `pageSize=max(size,1)`；`derivedId` = 前缀 + SHA-256 前 32 hex（恒 36 字符，含平面图域）；`projectExists` + 项目/商户 scope；中文整句（plan_name/candidate_ids/多边形/导入上限）。
- `FloorPlanImportParser`：10 个独立 `error_code`、13 处 `error()`/`messageFor` 中文（M015-004）。
- V423 已落 `complex_test`（Flyway version 423，391 条校验通过）：`chk_mall_floor_plans_project_id_required NOT VALID`；两表 `shape_type` 八值；`space_type_dictionary.status` 闭集。
- `VenueBusinessHoursService`：场地须存在于 `spaces`；项目 scope；星期 0–6；跨零点放行；开歇同刻拒绝；`is_draft` 复用为停用（M015-010 XM，本模块测试锁的是保存即生效）。
- OpenAPI `FloorPlanImportPublishRequest.candidate_ids` `maxItems: 200`（`docs/openapi/openapi-p0.yaml` L32992–32995）。二次确认字段不在 DTO（M015-011 XM→M392/M018）。

### 3.3 本模块测试实际锁住什么

| 已定稿修复 | 测试是否可红 | 落点 |
|---|---|---|
| M015-001 派生主键含平面图域 / ≤64 | 跨图同键两例 + `hasSizeLessThanOrEqualTo(64)` 可红；**不锁恒 36 字符** | ServiceTests L333–380 |
| M015-002 project_id 必填且项目存在 | 服务层两例可红；**JDBC `projectExists` SQL 无用例** | ServiceTests L398–411 |
| M015-003 项目 scope | 列表/访问/创建三例可红 | ServiceTests L415–440 |
| M015-004 解析器中文 error_message | **只 assert errorCode，改回英文仍绿** | ParserTests L52–53, L109–110 |
| M015-005/006 中文整句 | **只 assert exception.code()，改回英文仍绿** | ServiceTests / VenueHours |
| M015-007 三处诚实不分页 | **只锁 listShapes**；listFloorPlans/listImportTasks 可改回 50 | ServiceTests L384–394；ControllerTests L151 桩 `1,50,1` |
| M015-008 发布上限 200 | **无用例** | — |
| M015-009 场地存在 + 项目 scope | 服务层三例可红；**SQL 被 RecordingRepository Map 覆盖** | VenueHours L91–114 |
| M015-010/011 草案化 / 二次确认 | XM，本模块测试锁现行「保存即生效 / DTO 无确认字段」 | 观察 |
| M015-012 用途/负责人列 | 全局遗漏，本 5 文件无断言 | N/A |
| M015-013 / M016-002 八值白名单 + CHECK | 正向 booth/public_area 服务+shapes 表可红；**无负例、无 import_candidates 独立例** | ServiceTests L551–559；Jdbc L184–193 |
| M016-001 project_id NOT VALID 非空 | JDBC 空 project 被拒可红 | Jdbc L197–201 |
| M016-003 字典 status 闭集 | TDD 在 `PlatformJdbcIntegrationTests`（M011），**本 5 文件无** | 观察 |

## 4. 第一性原理线（逐文件）

### 4.1 FloorPlanControllerTests.java（L1–407，12 例）

1. **为何必须存在**：删除后 Controller 的权限码映射、未登录、缺幂等键（create/publish）失去可红反馈；PGM-040/041「单一功能单独授权」与 GEN-005 在 HTTP 层做不到。
2. **声称 vs 实际**：`@WebMvcTest` + mock `FloorPlanService`/`AuthorizationService`。8 端点均 `verify(requirePermission)`（除未登录 list）。create 有 403。create/publish 缺幂等键走真实 `requireIdempotencyKey`（日志 `idempotency_key is required`）。**不执行** Service 校验、分页诚实性、项目隔离、200 上限、中文。
3. **未验证前提**：`PageResponse(…,1,50,1)` 是 M015-007 修复前的谎报形状；只断言权限与首条 id。`P0_SMOKE` 角色 + mock 授权，不是生产权限组合，但 WebMvcTest 标准做法——授权闸在 `AuthorizationService.requirePermission` 的 verify/doThrow，不在 Spring role。saveShape/createImport 无缺幂等键例：删掉这两处 `requireIdempotencyKey`，mock 服务返回 null → 200，现测仍绿。7/8 端点无 403：`doThrow` 只绑在 create。

### 4.2 FloorPlanImportParserTests.java（L1–112，4 例）

1. **为何必须存在**：删除后 SVG 归一化与 JSON 越界/未闭合折线失去可红反馈；PGM-023「系统解析出候选区域」做不到。
2. **声称 vs 实际**：真实 `new FloorPlanImportParser()`，4 例执行 parse。覆盖：svg 成功、`POLYLINE_NOT_CLOSED`、json 成功、`POINT_OUT_OF_RANGE`。
3. **未验证前提**：实现 10 个独立 error_code（PAYLOAD_REQUIRED、UNSUPPORTED_SOURCE_FORMAT、VIEWBOX_REQUIRED、POINTS_REQUIRED、POINTS_TOO_FEW、POLYLINE_NOT_CLOSED、POINT_OUT_OF_RANGE、NO_SHAPES_FOUND、JSON_PARSE_FAILED、SHAPES_REQUIRED）+ messageFor 默认「导入候选项无效」= M015-004「13 处」口径。测试只锁 2 个码。不 assert `errorMessage` 中文。不锁 `data-shape-type=booth/public_area` 解析（默认 shop）。

### 4.3 FloorPlanJdbcIntegrationTests.java（L1–244，5 例）

1. **为何必须存在**：删除后表存在、权限种子、JSONB 多边形往返、V423 project_id 非空、shapes 八值正向失去库层可红反馈。
2. **声称 vs 实际**：`@SpringBootTest` 硬编码 `jdbc:postgresql://127.0.0.1:5432/complex_test`，密码 `${COMPLEX_DATABASE_PASSWORD:…}`。本席导出三个 `COMPLEX_DATABASE_*` 后实跑：Flyway 391 validated、current version **423**、5/5 PASS。连跑两次 EXIT=0；事后 `SELECT id FROM mall_floor_plans WHERE id IN ('fp_jdbc',…)` 空集 → `@Transactional` 回滚成立。只读 `pg_constraint` 证实 V423 两表八值 CHECK 与 `chk_mall_floor_plans_project_id_required NOT VALID` 已在库。
3. **未验证前提**：`prj_jdbc` 不经 `projectExists`（无 FK，M016-004 已登记）。`import_candidates.shape_type` 八值 CHECK 无独立写入例（Jdbc 候选仍 `shop`）。无 corridor 负例。无 `projectExists` SQL。`currentFlywayVersion()==MigrationVersions.latest()` 耦全仓最新 V，不是平面图专属。`space_type_dictionary.status` 不在本类。

### 4.4 FloorPlanServiceTests.java（L1–715，28 例）

1. **为何必须存在**：删除后创建/图形/导入/发布、幂等、跨商户/跨项目、跨图同键、booth/public_area 正向、诚实分页（shapes）失去服务层可红反馈。这是本模块最深的 seam。
2. **声称 vs 实际**：真实 `FloorPlanService` + 真实 Parser + 真实 `IdempotencyService`/`AuditEventService` + **内存仓库**。28/28 实跑 PASS。审计事件名（created/failed/published）有断言。幂等同键返回首结果、异请求冲突、校验失败不占键均有。M015-001/002/003/007（shapes）/013 正向有例。
3. **未验证前提**：`InMemoryFloorPlanRepository.projectExists` 只认 `prj_1/prj_2`，不执行 `SELECT COUNT FROM projects`。无 201 条发布。无 listFloorPlans/listImportTasks 诚实分页。无 shape_type 负例。无中文 message 断言。无 `enforceImportLimits`（2000/1000，首轮正常导入达不到，收窄为观察）。发布幂等同键无专门例（create/shape/import 有）。

### 4.5 VenueBusinessHoursServiceTests.java（L1–175，9 例）

1. **为何必须存在**：删除后星期越界、开歇同刻、跨零点、场地不存在、跨项目、停用保留行失去可红反馈；配错会让当天核销全拒（服务注释）。
2. **声称 vs 实际**：真实 `VenueBusinessHoursService` + `RecordingRepository`（`jdbcTemplate=null`，覆盖 `findVenueProjectId`/`listByProject`/`upsert`）+ mock `AuditEventService`。9/9 PASS。M015-009 三例可红（未知场地、跨项目、列表过滤）。
3. **未验证前提**：`findVenueProjectId` 生产 SQL `SELECT COALESCE(project_id,'') FROM spaces WHERE id=:id` **零执行**。审计被 mock 掉，无 `venue_business_hours.updated` 断言。无幂等（实现本就不走 IdempotencyService，M015-014）。无 day=0 正向（实现 0–6；HTML 未写 0-6 vs 1-7，不升冲突）。无中文 message。无权限 403（Controller 不在本模块）。

**删除测试**：五类各有 HTML/已定稿修复承载，不是纯转发。缺口是覆盖不足，不是应删。

## 5. 顺从式线（90 冻结判据）

每项均已对照锁定义区原文 + HTML 出处。本模块是测试，GEN-003 为核心。无实现面的写明原因。不把 HTML 沉默判冲突。

### 5.1 GEN（18）

| ID | 适用 | 证据 | 本席 |
|---|---|---|---|
| GEN-001 | 是 | 5 类均有 HTML/闭环修复承载；无纯转发可删文件 | 通过（最小性） |
| GEN-002 | 是 | 58 例实跑 0 失败（CMD-T1）；但可执行通过 ≠ 锁住声称 | 部分：缺口见 findings |
| GEN-003 | **核心** | 多处 mock/桩/缺负例使删实现校验仍绿 | **FAIL** 001–008 |
| GEN-004 | 是 | 图形越界/缺顶点/缺 project 有例；发布 200、解析器多数 error_code 无例 | 部分 |
| GEN-005 | 是 | create 403 + 各端点 verify；7 端点无 403；saveShape/createImport 无缺键 | **FAIL** 004 |
| GEN-006 | 是 | JDBC 非空/八值正向、服务跨图同键；candidates CHECK 无独立例 | 部分 |
| GEN-007 | 是 | 同键/冲突/校验不占键；publish 同键与并发无线程例 | 部分（并发收窄观察） |
| GEN-008 | 是 | OpenAPI maxItems 200 无测试；Controller 桩 pageSize=50 与服务诚实分页不一致 | **FAIL** 002/003 |
| GEN-009 | 是 | 多条 VALIDATION_FAILED/NOT_FOUND 有例；解析器多数错误路径无例 | 部分 |
| GEN-010 | 是 | 无注入/越权成功路径；Jdbc 密码走环境变量（硬编码 fallback 观察） | 通过 |
| GEN-011 | 是 | FloorPlan 写路径有 eventName 断言；VenueHours mock 掉审计 | **FAIL** 008 |
| GEN-012 | 是 | 200 上限实现有、测试无；2000/1000 首轮达不到 | **FAIL** 002 |
| GEN-013 | 是 | `mvn -o -Dtest=…` 离线可跑；Jdbc 覆盖 URL 到 complex_test | 通过 |
| GEN-014 | 背景 | 本 5 文件无迁移；Jdbc 断言 latest() 与 V423 正向 | 部分（candidates 无独立例） |
| GEN-015 | 是 | 测试名/夹具英文不算；**面向操作员的 error_message 无断言** | **FAIL** 006；XM 009 |
| GEN-016 | 是 | 本 raw 命令/exit/输出可复现；58 PASS 不写成「锁已完成」 | 本报告遵守 |
| GEN-017 | 是 | findings 按冲突→细化→技术性→添加；无添加项；HTML 沉默不删 | 本报告遵守 |
| GEN-018 | 是 | 结论落到 HTML 原句、测试源码、mvn 输出、pg_constraint SELECT | 本报告遵守 |

### 5.2 BEQR（22）

| ID | 适用 | 说明 | 本席 |
|---|---|---|---|
| BEQR-063 | 无实现面 | 原文高危列表是反结/沽清/免单等，不含平面图发布；发布高危以 PGM-023 为准，不把 063 发明到导入 | N/A |
| BEQR-074 | 无实现面 | 隐私桌台权限；本 5 文件无桌台 | N/A |
| BEQR-143–148 | 无实现面 | 四类主体/收入/合作模式；测试不锁主体绑定 | N/A |
| BEQR-149 | 无实现面 | 职级 M1–M10 | N/A |
| BEQR-150 | 无实现面 | 终端十一端；后台测试不是端清单 | N/A |
| BEQR-151 | 部分 | 最终权限公式在 AuthorizationService；本模块 mock 后只锁 permission 码字符串 | 观察：不测公式 |
| BEQR-152 | **是** | 项目隔离：Service 三例 + VenueHours 三例；Controller 无跨项目；SQL 被夹具掩盖 | 部分 **FAIL** 007 |
| BEQR-153 | 无实现面 | 终端开关优先级 | N/A |
| BEQR-154–159 | 无实现面 | 项目/商户/合同/员工/设备/权限状态闭集；平面图 status 是 active/inactive/archived，不得发明成这些闭集 | N/A |
| BEQR-166 | 无实现面 | 合同审批链 | N/A |
| BEQR-170 | 无实现面 | 铺位租赁六态在合同仓（M015-014）；本测试不锁、也不把 shape/status 发明成租赁看板 | N/A |
| BEQR-189 | 无实现面 | 管理费方式 | N/A |

### 5.3 BSM（14）

| ID | 适用 | 说明 | 本席 |
|---|---|---|---|
| BSM-001 | 是 | 「先管清楚基础盘子」含空间；测试覆盖楼层平面图主路径 | 部分（缺口 GEN-003） |
| BSM-003 | 部分 | 权限分岗：Controller verify；可改可删/引导不在本 5 文件 | 部分 |
| BSM-004 | 无实现面 | 配置删除审批；本测试无删除/存档平面图 API | N/A（沉默不升冲突） |
| BSM-005 | 无实现面 | 分步引导是前端（M018） | N/A |
| BSM-006 | 无实现面 | 动态二维码 | N/A |
| BSM-007 | 部分 | Jdbc 断言 `role_platform_admin` 含四条 floor_plan.* 日常码 | 通过（种子） |
| BSM-009 | 是 | 基础管理含空间 | 同 PGM-005 |
| BSM-028 | 是 | 「建立综合体项目、楼层、区域、点位」；project_id 必填有例 | 部分 |
| BSM-029 | 背景 | 票务/酒吧/游乐要知道空间；本测试不连那些子系统 | N/A 连接叙述 |
| BSM-072 | 是 | 商户不能看其他商户；`listFloorPlansFiltersToMerchantScope` / `CROSS_MERCHANT_FORBIDDEN` | 通过 |
| BSM-074 | 背景 | 商户章「和谁有关」 | N/A |
| BSM-114 | 无实现面 | 入口表（后台/Web/商户侧）；本文件是 API 测试不是入口页 | N/A |
| BSM-115 | 背景 | 子系统必须连在一起 | N/A |
| BSM-120 | 无本文件专有断言 | 已确认规则总表；空间相关已由 PGM-022/023 覆盖 | N/A 不发明 |

### 5.4 PGM（36）

| ID | 适用 | 说明 | 本席 |
|---|---|---|---|
| PGM-001 | 是 | 先管归属：project_id 必填/存在有例 | 部分 |
| PGM-002 | 是 | 先对象再人员再留痕：权限 verify + 部分审计 | 部分（Venue 审计缺） |
| PGM-003 | 背景 | 模块入口卡片栅格 | N/A 卡片 |
| PGM-004 | 背景 | 综合体和集团卡片 | N/A |
| PGM-005 | 是 | 「空间和点位」卡片原文 | 通过（存在性） |
| PGM-006–013 | 无实现面 | 商户/合同/员工/权限/终端/设备/商户侧/开放接口卡片 | N/A（PGM-009/012 细则见 040/048） |
| PGM-014 | 是 | 留痕复核；FloorPlan 写路径有 eventName | 部分 **FAIL** 008 |
| PGM-015–016 | 无实现面 | 楼宇运营/应急隐私 | N/A |
| PGM-017 | 是 | 对象带综合体项目归属；create 必填+存在 | 部分（SQL 掩盖） |
| PGM-018–021 | 背景 | 多对多/主体职责/项目档案/集团意义 | N/A |
| PGM-022 | 是 | 空间不仅是楼层图 | 通过（模块存在） |
| PGM-023 | **是** | 候选铺位/摊位/公共区；发布高危二次确认；不可静默覆盖 | 正向 booth/public_area；**负例/candidates/二次确认/200 上限缺口**；二次确认属 M015-011 XM，本模块不能单测变绿 |
| PGM-026 | 背景 | 商户档案功能 | N/A |
| PGM-027 | 是 | 商户不能看其他商户 | 同 BSM-072 |
| PGM-031 | 无实现面 | 经销商治理 | N/A |
| PGM-035 | 无实现面 | 提成发放 | N/A |
| PGM-040 | 是 | 单一功能单独授权 | 权限码有 verify；403 不齐 |
| PGM-041 | 是 | 默认按项目隔离、无关闭开关 | 同 BEQR-152 |
| PGM-042–044 | 无实现面 | 职级/外包审计/桌台挂单经销 | N/A |
| PGM-048 | 是 | 商户侧不跨商户 | 同 BSM-072 |
| PGM-060 | 无实现面 | 会员生物信息 | N/A |
| PGM-065 | 背景 | 连接关系表 | N/A |
| PGM-066 | 是 | 已拍板：所有对象带综合体项目归属 | 同 PGM-017 |

顺从式结论：90 项均已核对。适用项中 GEN-003/005/008/011/012/015、BEQR-152、PGM-023/041 被测试缺口打穿。其余无实现面项不升冲突。

## 6. 对抗式线（变异：删哪条实现校验，哪条用例红）

grilling 追问树（自答，不派子代理）：

| 变异（首轮正常会做的操作） | 期望红的测试 | 实际 |
|---|---|---|
| 删 `requirePermission("floor_plan.manage")` 于 POST create | `createPermissionDeniedReturnsForbidden` | 会红 |
| 删 GET list 的 `requirePermission` | `listUsesViewPermissionAndPageShape` 的 verify | 会红（但不是 403） |
| 删 GET list 后只测 HTTP 403 | 无 403 例 | **仍绿** |
| 删 saveShape/createImport 的 `requireIdempotencyKey` | 无对应缺键例 | **仍绿**（mock 200） |
| 删 create/publish 的 `requireIdempotencyKey` | createWithout… / publish…RequiresIdempotencyKey | 会红 |
| 把 Parser 13 处中文改回英文 | 无 message 断言 | **仍绿** |
| 删 VIEWBOX_REQUIRED / PAYLOAD_REQUIRED / NO_SHAPES_FOUND / JSON_PARSE_FAILED / SHAPES_REQUIRED / POINTS_REQUIRED / POINTS_TOO_FEW / UNSUPPORTED_SOURCE_FORMAT | Parser 4 例 | **仍绿** |
| 删 POLYLINE_NOT_CLOSED / POINT_OUT_OF_RANGE | Parser 对应 2 例 | 会红 |
| 删 `candidateIds.size() > 200` | 无 | **仍绿**（M015-008） |
| `listFloorPlans`/`listImportTasks` 改回 `pageSize=50` | 仅 listShapes 诚实例 | **那两处仍绿**；Controller 桩本来就是 1,50,1 |
| 白名单去掉 booth/public_area | `saveShapeAcceptsBoothAndPublicArea` | 会红 |
| 白名单接受任意字符串（含 corridor） | 无负例 | **仍绿** |
| 删 `import_candidates` 八值 CHECK，保留 shapes CHECK | Jdbc booth 例只写 shapes | **仍绿** |
| 删 `chk_mall_floor_plans_project_id_required` | `floorPlanWithoutProjectIsRejectedByDatabase` | 会红 |
| `JdbcFloorPlanRepository.projectExists` 恒 true | 内存仓库自己的 Set | **仍绿** |
| `VenueBusinessHoursRepository.findVenueProjectId` 改错表 | RecordingRepository Map | **仍绿** |
| 删 VenueHours `recordSuccess` | mock AuditEventService | **仍绿** |
| 删跨图 derivedId 域 | `saveShapeWithSameKeyAcrossFloorPlansDoesNotOverwrite` 等 | 会红 |
| 发布已发布任务 | `publishImportTaskRejectsAlreadyPublishedTask` | 会红 |
| 开歇同刻 | `rejectsIdenticalOpenAndCloseTime` | 会红 |
| 跨零点误拦 | `allowsWindowCrossingMidnight` | 会红 |
| 二次确认字段（M015-011 XM） | DTO 无字段，加测会相对现网红 | 不在本模块单修 |
| 2000 节点 / 100 万字符 | 首轮正常导入达不到 | 收窄观察 |

`@Transactional`：Jdbc 类级注解。本席连跑两次 EXIT=0，事后 SELECT 夹具 id 空集，回滚成立。不依赖 `complex_test` 预置业务行（表/权限/Flyway 来自迁移）。

ControllerTests 只测 mock 映射：参数校验除幂等键外不进 Service；权限映射有 verify，403 不齐。

## 7. Findings（GR-M017-001 起）

### GR-M017-001 — Parser 4 例未锁全部导入 error_code

- 文件:行号：`FloorPlanImportParserTests.java:12-111`；对照实现 `FloorPlanImportParser.java:34-46,54-58,68,75,80,90,107,121,127,228-245`
- 完整原文（锁）：GEN-003「测试须实际执行目标行为、具备能失败的断言，并覆盖本模块关键分支」；PGM-023 HTML L472「系统解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误」
- 业务影响：操作员导入缺 viewBox、空 payload、非法 JSON、无图形、不支持格式时，解析器改回静默/错码/英文，复核页失去可红保护。
- 可复现：CMD-T1 下 Parser 4/4 绿；实现 10 个独立 error_code，测试只出现 `POLYLINE_NOT_CLOSED`、`POINT_OUT_OF_RANGE`。最小反例：删 `VIEWBOX_REQUIRED` 分支，现 4 例仍绿。
- 冻结判据：GEN-003、GEN-009、PGM-023
- 2.3 依据类型：1 HTML + 2 可执行产物 + 4 命令
- 四分类：**技术性**（无业务发明：error_code 闭集来自现实现，不是新规则）
- 工程缺陷：成立。建议入队：**是**
- 最小处置：为其余 error_code 各补最小负例，并 assert 中文 `errorMessage`（与 M015-004 同批口径）。不发明第 11 种业务码。

### GR-M017-002 — 发布上限 200 无专门用例（转入的 M015-008 观察在本模块变为缺陷）

- 文件:行号：`FloorPlanService.java:46-47,588-589`（实现有闸）；本模块 **无** 对应测试方法。OpenAPI `docs/openapi/openapi-p0.yaml:32992-32995` `maxItems: 200`
- 完整原文：GEN-012「批处理须有与实际负载相称的边界」；GEN-004 首次可信使用前校验；GEN-008 契约一致
- 业务影响：一次发布 201 个候选是后台复核后的正常批量（契约上限）；删掉服务闸后现 58 例仍绿，契约与实现可再漂移。
- 可复现：CMD-T1 58 PASS；grep 本 5 文件无 `200`/`MAX_PUBLISH`。最小反例：注释 `size() > 200` 分支，全绿。
- 冻结判据：GEN-003、GEN-004、GEN-008、GEN-012
- 2.3 依据类型：2 + 4（契约是实现事实，不当新需求；用 GEN 锁一致性）
- 四分类：**技术性**
- 工程缺陷：成立。建议入队：**是**
- 最小处置：`FloorPlanServiceTests` 一例 `candidate_ids` 201 条 → `VALIDATION_FAILED` 且中文含「200」。不改实现。

### GR-M017-003 — 诚实不分页只锁 listShapes；Controller 桩固化 pageSize=50

- 文件:行号：`FloorPlanServiceTests.java:384-394`（仅 shapes）；`FloorPlanService.java:89,105,151`（三处诚实）；`FloorPlanControllerTests.java:150-157,163-164,231-237` `new PageResponse<>(…, 1, 50, 1)` 只断言 `items[0].id` / `total`
- 完整原文：GEN-008 前后端口径一致；GEN-003 能失败的断言；M015-007 定稿三处
- 业务影响：列表接口谎报 pageSize=50 时，shapes 例会红，平面图列表与导入任务列表不会；Controller 测试还用旧谎报形状做桩，读者会以为 50 仍合法。
- 可复现：把 `listFloorPlans` 改回 `new PageResponse<>(records,1,50,records.size())`，现测除 shapes 例外仍绿。
- 冻结判据：GEN-003、GEN-008、GEN-012
- 2.3 依据类型：2 + 4
- 四分类：**技术性**
- 工程缺陷：成立。建议入队：**是**
- 最小处置：为 listFloorPlans、listImportTasks 各补 `pageSize==total==items.size()`；Controller 桩改为诚实形状或断言 pageSize 与服务一致。不新增分页参数。

### GR-M017-004 — 8 端点未逐端点 403；两写端点无缺幂等键

- 文件:行号：`FloorPlanController.java:31-94` 八端点；`FloorPlanControllerTests.java:65-291`：403 仅 L125–147 create；缺幂等键仅 create L76–93 与 publish L256–270；saveShape L175–203、createImport L206–227 无缺键、无 403
- 完整原文：GEN-005「受保护操作须在服务端按主体、对象和动作授权；默认拒绝」
- 业务影响：未授权员工打 shapes/imports/publish：若有人删掉该端点 `requirePermission`，verify 只覆盖「被调用过」的成功路径；saveShape/createImport 缺幂等键会在 mock 下 200。首轮后台操作会打这些端点。
- 可复现：CMD-T1 日志仅 create 打出 `PERMISSION_DENIED`；publish/create 打出 `idempotency_key is required`。最小反例：删除 `saveShape` 的 `requireIdempotencyKey`，现 12 例仍绿。
- 冻结判据：GEN-005、GEN-007、GEN-003、PGM-040
- 2.3 依据类型：2 + 4
- 四分类：**技术性**
- 工程缺陷：成立。建议入队：**是**
- 最小处置：其余 7 端点各 1 个 `doThrow(forbidden)`→403；saveShape、createImport 各 1 个缺幂等键→400。夹具英文 `permission denied` 按 GEN-015 测试夹具豁免。

### GR-M017-005 — 无 corridor/非法 shape_type 负例；无 import_candidates 独立 CHECK 例

- 文件:行号：`FloorPlanServiceTests.java:551-559` 只正向 booth/public_area；`FloorPlanJdbcIntegrationTests.java:184-193` 只写 `floor_plan_shapes`；`FloorPlanJdbcIntegrationTests.java:142-160` 候选 `shape_type=shop`；V423 两表同八值（只读 `pg_constraint` 已见）
- 完整原文：PGM-023「候选区域（铺位、摊位、公共区）」；GEN-008 服务白名单与 CHECK 同闭集。corridor 是前端多出的值（M016-004 观察，HTML 未写）——负例是「非闭集须拒」，不是把 corridor 写成业务需求。
- 业务影响：服务白名单若再放行任意字符串，正向 booth 例仍绿；candidates 表 CHECK 若回退六值，shapes 例仍绿。导入摊位/公共区走候选表，与人工画在 shapes 表不是同一条约束。
- 可复现：complex_test 两表 CHECK 均含 booth/public_area；本模块无 `assertThatThrownBy` 对 `shape_type=corridor` 或对 candidates 写入 booth。
- 冻结判据：GEN-003、GEN-008、PGM-023
- 2.3 依据类型：1 + 2 + 4
- 四分类：**技术性**（corridor 负例不发明新区域类型；只锁闭集拒绝）
- 工程缺陷：成立。建议入队：**是**
- 最小处置：Service 一例非法 `shape_type` → VALIDATION_FAILED；Jdbc 一例 `import_candidates` 写入 booth/public_area 被接受，一例非法 type 被拒。不把 corridor 写进白名单。

### GR-M017-006 — 已闭环中文错误文案无断言

- 文件:行号：ParserTests L52–53/L109–110 只 extract errorCode；ServiceTests 一律 `exception.code()`；VenueHours 同；Jdbc L167 中文是**夹具写入**不是 parser 产出
- 完整原文：GEN-015「展示文案与项目声明的语言约定一致」；铁律 7 面向操作员中文；M015-004/005/006 已改实现
- 业务影响：把 13 处 error_message 或「候选项列表不能为空」改回英文，58 例仍绿。复核人看到英文即回归 M015 缺陷。
- 可复现：CMD-T1 全绿；grep 本 5 文件测试断言侧无「不能为空」「多边形至少」。
- 冻结判据：GEN-015、GEN-003、PGM-001
- 2.3 依据类型：1 + 2 + 4
- 四分类：**技术性**
- 工程缺陷：成立。建议入队：**是**
- 最小处置：对 M015-004/005/006 已改的出网中文各补 `exception.getMessage()`/`errorMessage()` 断言（不含测试名/夹具英文）。不改 FieldLabels。

### GR-M017-007 — 仓库 SQL 被内存/覆盖夹具掩盖（projectExists / findVenueProjectId）

- 文件:行号：`FloorPlanServiceTests.java:611-613` `Set.of("prj_1","prj_2")`；`JdbcFloorPlanRepository.java:65-68` 真实 `SELECT COUNT(1) FROM projects`；`VenueBusinessHoursServiceTests.java:41-43` Map；`VenueBusinessHoursRepository.java:51-53` 真实 `SELECT … FROM spaces`
- 完整原文：GEN-003 mock 不得掩盖缺陷；BEQR-152/M015-002/009 的存在性闸是 SQL
- 业务影响：`projectExists` 恒 true 或场地查询改错表时，服务测试仍绿，生产会把未知项目/未知场地写进去。首轮创建平面图、配营业时段都会打这两条 SQL。
- 可复现：本 5 文件无 `repository.projectExists` 对真实库；VenueHours 测试 `jdbcTemplate=null` 保证「一条 SQL 都不该发出去」（L30 注释）。
- 冻结判据：GEN-003、GEN-005、BEQR-152、PGM-017、PGM-041
- 2.3 依据类型：2 + 4
- 四分类：**技术性**
- 工程缺陷：成立。建议入队：**是**
- 最小处置：Jdbc 集成一例未知 `project_id` 走真实 `projectExists`（或服务+Jdbc 仓库，`@Transactional`）；VenueHours 用 `@Transactional` 真实仓库测不存在 venue / 跨项目（本模块允许只改该测试文件）。不改生产 SQL。

### GR-M017-008 — 营业时段审计被 mock 掉，无 GEN-011 断言

- 文件:行号：`VenueBusinessHoursServiceTests.java:76` `mock(AuditEventService.class)`；`VenueBusinessHoursService.java:48-50,85-92` 写 `venue_business_hours.viewed/updated`
- 完整原文：GEN-011「关键操作须留下可关联、可复现的结构化证据」；PGM-002 最后留痕；PGM-014
- 业务影响：配错营业时段会影响当天核销；删掉 `recordSuccess` 现 9 例仍绿。
- 可复现：grep VenueHours 测试无 `eventName`/`recordSuccess` verify。
- 冻结判据：GEN-011、GEN-003、PGM-002、PGM-014
- 2.3 依据类型：2 + 4
- 四分类：**技术性**
- 工程缺陷：成立。建议入队：**是**
- 最小处置：改用真实 `AuditEventService`+内存库（与 FloorPlanServiceTests 同构），save/list 各 assert 一条事件名。不发明新事件类型。

### GR-M017-009 — XM：服务层 `requireText` 仍出网英文 `field is required`（回 M015）

- 文件:行号：实现 `Strings.java:27-31` `field + " is required"`；`FloorPlanService.java` 多处 `requireText(...,"floor_no"|"image_url"|…)`；Controller 日志 `idempotency_key is required`（CMD-T1）
- 完整原文：GEN-015；M015-005/006 已修 plan_name/candidate_ids/多边形/导入上限/场地，**未覆盖** requireText 通用英文句
- 业务影响：操作员漏填楼层号等首轮必填字段，看到英文键。不是本模块测试能单独改绿的实现面（改 Strings 会影响全仓）。
- 可复现：CMD-T1 Controller 日志 `raw_message=idempotency_key is required`
- 冻结判据：GEN-015、GEN-008
- 2.3 依据类型：2 + 4
- 四分类：**技术性**
- 工程缺陷：成立。建议入队：**否（XM 回 M015 单独立项；本模块不得改实现）**
- 最小处置：M015 后续对出网 requireText 改中文整句或本模块只在测试中锁已改过的那些字段（001–008）。不在本模块改 `Strings`。
- 无业务发明：中文是铁律 7 / GEN-015 已有口径，不是新业务规则。

**添加项**：无。

## 8. 观察（无新根因，完整登记不漏并）

1. **OBS-01** `derivedIdsFitVarchar64Columns` 断言 `≤64` 不锁恒 36 字符。跨图同键两例已锁「含平面图域」。36 是算法选择，VARCHAR(64) 才是溢出根因。不升缺陷。
2. **OBS-02** 候选/错误 ID 仍 `"fpic_"/"fpie_"+slug(taskId+no)`（约 43 字符），不是 derivedId 36。已随任务 ID 分域，不再出现原 68 溢出。不升缺陷。
3. **OBS-03** `enforceImportLimits` 2000 节点 / 1000 顶点、payload 100 万字符：首轮正常导入达不到。收窄观察。
4. **OBS-04** 发布二次确认：DTO/OpenAPI 无确认字段，前端 HighRiskConfirm（M015-011 XM→M392/M018）。本模块加必填字段测会相对现网红。不单独立项。
5. **OBS-05** 发布是追加 shapes、拒已发布任务；不覆盖已有 shape id。HTML「不可静默覆盖既有布局证据」由跨图同键 + 拒重复发布部分锁住。对「已有布局上再发布叠加」HTML 未写禁止叠加，不发明。
6. **OBS-06** M016-003 字典 status 闭集的 TDD 在 M011 `PlatformJdbcIntegrationTests`，本 5 文件不重锁。只读 pg_constraint 已见 CHECK。不重开 M016。
7. **OBS-07** BEQR-170 铺位租赁六态不在平面图测试；合同仓。不把 `spaces.status`/`shape.status` 发明成租赁看板。
8. **OBS-08** 营业日 10:00（BEQR-187）未映射本模块；VenueHours 锁的是星期开歇。day=0 无正向例；HTML 未规定 0–6 vs 1–7，不升冲突。
9. **OBS-09** VenueHours 保存即生效 vs BSM-001 涉及钱须草案：M015-010 XM。本测试锁现行行为，不加草案测（会相对现网红）。
10. **OBS-10** Controller `P0_SMOKE` + mock AuthorizationService：不是伪造生产角色授权矩阵，是 WebMvcTest 鉴权旁路。不升缺陷。
11. **OBS-11** Jdbc `currentFlywayVersion()==latest()` 耦全仓 V 号；Flyway 391/423 本次绿。观察脆弱性。
12. **OBS-12** Jdbc 密码 fallback `complex_dev_pwd` 写在测试源码；160 导出环境变量后走真实密码。不升缺陷。
13. **OBS-13** `listImportCandidates` 返回 Details 不是 PageResponse；诚实分页 N/A。
14. **OBS-14** 开工「工作区干净」与 `ledger.md` 已改 IN_PROGRESS 不完全一致；本 5 文件/三 HTML/锁未改。
15. **OBS-15** GEN-007 无线程并发例；同键顺序幂等已覆盖主写路径。首轮双人同键冲突已有例。不升缺陷。
16. **OBS-16** `findVenueProjectId` 的 `COALESCE(project_id,'')`：场地存在但 project_id NULL 时返回空串而非 empty。首轮 spaces 是否可空未在本模块证明。不升 XM。
17. **OBS-17** Parser 不校验 shape_type 闭集，非白名单在 `createImportTaskOnce` 的 `normalizeAllowed` 整单失败。无「导入 booth SVG」例。并入 005 处置即可。
18. **OBS-18** ponytail-review：5 文件无可删抽象。`net: 0`。缺口是缺用例。

## 9. 命令证据（类型 4）

### CMD-T1 五类测试（只导出三个 COMPLEX_DATABASE_*，不 source 全文件，`-o` 不装依赖）

```
cd backend
# 仅从 /etc/complex/complex.env 导出 COMPLEX_DATABASE_URL / _USERNAME / _PASSWORD
mvn -o -Dtest=FloorPlanControllerTests,FloorPlanImportParserTests,FloorPlanJdbcIntegrationTests,FloorPlanServiceTests,VenueBusinessHoursServiceTests test
```

- exit **0**
- Tests run: **58**, Failures: **0**, Errors: **0**, Skipped: **0**
- Parser 4 / Jdbc 5 / Service 28 / Controller 12 / VenueHours 9
- Jdbc：`Database: jdbc:postgresql://127.0.0.1:5432/complex_test`；`Successfully validated 391 migrations`；`Current version of schema "public": 423`；`Schema "public" is up to date`
- Controller 日志：`PERMISSION_DENIED raw_message=permission denied`；`VALIDATION_FAILED raw_message=idempotency_key is required`（create 与 publish 各一次）
- BUILD SUCCESS，Total time 5.329 s
- 环境 `COMPLEX_DATABASE_URL` 主机段指向 `complex`，Jdbc 测试属性覆盖为 `complex_test`（未对生产库 `complex` 做写操作）

未使用用户写的 `-q`，以便保留类名与日志；语义同离线单测。

### CMD-T2 Jdbc 连跑两次 + 残留 SELECT

两次 `mvn -o -q -Dtest=FloorPlanJdbcIntegrationTests test` EXIT=0。  
`SELECT id FROM mall_floor_plans WHERE id IN ('fp_jdbc','fp_import_jdbc','fp_shape_types','fp_no_project')` → 空。回滚成立。SELECT only。

### CMD-T3 只读 pg_constraint

`chk_floor_plan_shapes_shape_type` 与 `chk_floor_plan_import_candidates_shape_type` 均为八值（含 booth、public_area）；`chk_mall_floor_plans_project_id_required CHECK (project_id IS NOT NULL) NOT VALID`；`chk_space_type_dictionary_status` active/disabled。SELECT only。

### CMD-LOCK / 文件 SHA

见 §1–§2。五文件 match=True。

58 PASS **不是** 与 HTML/已定稿修复同锁的证据。

## 10. 审计结论

**FAIL**

理由（须同时满足才 PASS：本模块所有文件有覆盖记录；90 判据已核对；三线均执行；声称有四类证据；无未答 questions；无未定稿缺陷；哈希行有效）：

- 覆盖、90 核对、三线、证据、哈希、无 questions：**成立**
- **未定稿缺陷成立**：GR-M017-001…008 建议入队「是」；GR-M017-009 XM 否
- 因此单席结论 FAIL。不存在有条件通过。

B2 请主调度按 2.2 处理 001–008（本模块只改 5 个测试文件）与 009（XM 回 M015）。本席禁止修复。

下游席完成判据：读完本文件最后独占锁行至 EOF；勿读本轮 self-raw；合取时以本 FAIL 为否决票，除非差集证明 001–008 根因不成立。

## M017 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 46628 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6 xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（铁律共 **7** 条）；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M017 FILES 5 / 锁映射 90 / 计划冻结 1352 行，现文 1653 行）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `e2abb037001ccd72650efa3cd86e9b1b4bfe3ea0`（PR #437 M016 闭环检查点已合并）。本模块 5 文件指纹与 B1 覆盖表全等。B3=0。无源修复。三份 HTML 现文：治理地图 V0.12、业务地图 V0.14、枚举 V0.07。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类。
- 本席 B2-r1：**PASS**（001–010 逐 ID 全 PASS。添加 0、不发 2.2。非阻断勘误见各 ID，不否决）
- 正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 `M017 → [BEQR-063 … PGM-066]`（`criteria-lock.md` L6396）COUNT=90 unique=90。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要复现只读（Java/HTML/`rg`/哈希）；未对任一 `complex_test*` 库做写操作，本批未再连库。ledger 所记 `/tmp/m017_dry` 干跑 **不是** 本票依据。

此票只判断 findings.md V1.98 顶部「当前 M017 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M017-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 46628 | `1fb88c732d7f9dce504fab5fb52e7bc432eaa438af8c4d173a08c1ccdd0a1075` |
| `docs/audit-2026-09/modules/M017-b1-self-raw.md` | V0.01 | 24255 | `c365aede3b4bb9c86f4dd44ff75979515bbdba3f4424762f1a087f1ed4e9c8e6` |
| `docs/audit-2026-09/findings.md` | V1.98 | 794881 | `0d8e23ca2bd1c5d44b3b971ca879d91007a6089b0313fe05b4cc595fd668e1e5` |

ledger 只读：V3.45，610554 bytes，SHA256 `912b10e9dac8f6c8f07f04315651e0dfa42c4dcea79d67cfefda51ee8c403c30`；M017 IN_PROGRESS、B3=0。5 文件 `git ls-files -z -- <5 paths> | xargs -0 cat | wc -l` = **1653**（计划冻结 1352；差额为 M015/M016 B3 追加，与 B1 同）。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

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

本模块 5 文件指纹（与 B1 / 开工清单 match）：

| 路径 | 行 | bytes | SHA256 |
|---|---:|---:|---|
| `FloorPlanControllerTests.java` | 407 | 17103 | `ea7362efb0b519e3b711b54f52f9df9134ee510943859f0e745936e8e9f12795` |
| `FloorPlanImportParserTests.java` | 112 | 3859 | `597735f13baa6b08ed41f60264827377b5c6e0e3dc861e4e2eeaa5de166e44b1` |
| `FloorPlanJdbcIntegrationTests.java` | 244 | 9639 | `018480bf503be1579a00ce6c36ea83b283c24951c161a581029ea17c33f9e9e9` |
| `FloorPlanServiceTests.java` | 715 | 32109 | `03a59adf8c527f0dd92fb42e49188a8c3334ad685d82829621ed449e6e874fe2` |
| `VenueBusinessHoursServiceTests.java` | 175 | 7649 | `bf1a9d3232f5318a2ab15313533a54c255efe77831589af9ca256ff8c477cc4b` |

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核源/HTML/命令再接受或反驳；禁止表演性同意；B2 不修复。本批重点反驳本席 B1 GR-009「出网英文」：核到抛点英文是机读、出网经 `FieldLabels.translate` |
| grilling | 只借追问结构自查：009 出网是否仍英文、002 是否发明禁止叠加、007 同文件嵌套集成是否原意、008 真实审计库是否合规、003–006 是否最懒、010 是否写错观察 |
| verification-before-completion | 声称前先跑只读哈希/行号/源码/`rg` |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 删除测试：本模块深度落在 5 个测试类对已闭环实现的可红反馈，不落实现/迁移/前端 |
| ponytail-review | 只评最小删除/收窄，不落地。009 不 XM、不改 Strings；002 锁不覆盖不禁叠加；007 不新增文件；006 不断言 requireText 英文抛点 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |

### 完整差集（GR-001…009、OBS-01…18、席甲 CX-001…003、OBS-1…7）

漏并：无。两席 B1 全部临时 ID 仍落在 001–010。无单席独有缺陷被丢。本席 GR-009 原「缺陷成立、XM 回 M015、本模块队列否」被候选改成「缺陷否、不 XM、登记 010」——**抛点英文的事实未改写**，工程缺陷判定按出网路径重裁（见 009）。席甲独有弱断言/审计（CX-003）并入 008，与本席 GR-008 同文件同根。席甲 CX-002 的「先存图形再发布」与本席 OBS-05 合取，未发明禁止叠加（见 002）。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M017-001 | GR-M017-001 + CX-M017-001 | 技术性/是/是 | **PASS（技术性/是/是；未锁码 8 个各一例，括注「10 例」非阻断）** |
| M017-002 | GR-M017-002 + CX-M017-002 | 细化/是/是 | **PASS（细化/是/是；①锁不覆盖、允许叠加，与 OBS-05 一致）** |
| M017-003 | GR-M017-003 | 技术性/是/是 | **PASS（技术性/是/是；两列表诚实分页 + Controller 桩改诚实形状）** |
| M017-004 | GR-M017-004 | 技术性/是/是 | **PASS（技术性/是/是；7 端点 403 + 2 写端点缺键）** |
| M017-005 | GR-M017-005 + OBS-17 | 技术性/是/是 | **PASS（技术性/是/是；corridor 只作负例，不进白名单）** |
| M017-006 | GR-M017-006 | 技术性/是/是 | **PASS（技术性/是/是；只锁已改中文整句，不改 FieldLabels）** |
| M017-007 | GR-M017-007 | 技术性/是/是 | **PASS（技术性/是/是；同文件嵌套真实仓库，不新增文件）** |
| M017-008 | GR-M017-008 + CX-M017-003 | 技术性/是/是 | **PASS（技术性/是/是；真实 AuditEventService + 五处 code）** |
| M017-009 | GR-M017-009 | 技术性/正向 /否/否 | **PASS（否；不再是缺陷，不 XM）** |
| M017-010 | 双方观察 + GR-009 | 技术性/正向 /否/否 | **PASS（否；未把应升项留下，OBS-01 压缩非阻断）** |

冲突 0。细化入队 1（002）。技术性入队 7（001、003–008）。观察否 2（009、010）。添加 0。questions 0。无 HTML 修改申请。

### 009 独立裁定（技术性／正向；缺陷否；队列否）——本席 B1 GR-009 重裁

本席 B1：`Strings.requireText` 抛 `field + " is required"`，Controller 日志 `raw_message=idempotency_key is required`，判工程缺陷成立、本模块队列否、XM 回 M015。候选置「否/观察」。本票按出网路径重裁，**不沿用「出网英文」那句**。

核源（只读）：

- `FieldLabels.java:11-19` 原文：「抛点保留英文键（`order_id is required`）不动：那串是给日志/排障看的机读信息，只在写响应体的一瞬间换成中文」。`:13-16` 收口点是 `GlobalExceptionHandler`，不是各抛点。
- `Strings.java:22-30` 同口径：英文字段键是机读；写 HTTP 响应体时由 Handler 经 `translate` 出网；并写明不要在抛点各自拼中文（两套文案）。
- `GlobalExceptionHandler.java:56-61`：`:57` `raw_message` 仅 `log.debug`；`:61` 响应体 `FieldLabels.translate(exception.getMessage())`。
- `FieldLabels.translate:511-536`：`^field is (required|invalid)$` → 有标签则「未填写／格式不正确」，`idempotency_key` 整句「请勿重复提交，请刷新后再试」，未知键走中文兜底 `FALLBACK`，**绝不回退英文键**。已含汉字原样透传。
- `FieldLabelsTests:21-39` 已锁上述形态。平面图键已有中文名：`floor_no`→楼层、`image_url`→图片地址、`version_no`→版本号、`label`→标签、`source_name`→来源名称、`source_payload`→来源报文、`floor_plan_id`→平面图。
- 本席 B1 复现点正是 CMD-T1 的 debug `raw_message=idempotency_key is required`。那是机读日志，不是操作员响应体。出网该键是「请勿重复提交，请刷新后再试」。
- M015-005/006 定稿：只把 **FieldLabels 会错译的本模块整句**（`candidate_ids`→「招商对象」、`plan_name`→「方案名称」、括号英文）改成中文整句，**明确不改 FieldLabels**。剩余 `requireText(...,"floor_no")` 等走翻译层「楼层未填写」，不是回归。

GEN-015 锁的是展示文案。抛点英文 + 出网翻译是仓内既定设计，不是未修的中文违规。本模块测试若对 `exception.getMessage()` 断言「floor_no is required」的中文，会把机读抛点误当成展示面。006 已写「不断言测试夹具/日志英文」，与此合取。

四分类：候选「技术性/正向」=登记观察。工程缺陷：**否**。队列：**否**。不 XM、不回 M015、不改 `Strings`/`FieldLabels`/`ApiRequests.requireIdempotencyKey`。登记于 010。

票：**PASS（否）**。

### 002 独立裁定（细化／是；与 OBS-05 一致，未发明禁止叠加）

核源：

- 治理地图 L472：「发布会写入正式平面图区域，**不可静默覆盖既有布局证据**」。原文是「覆盖」，不是「禁止在已有布局上再写入」。
- `FloorPlanService:392-405` 发布对每个候选 `saveShape` 追加；发布图形 id = `derivedId("fps_", importTaskId + "_" + candidate.id())`，与手工 `saveShape` 的派生 id 不同域，不会撞上已有 shape id。
- 现测 `publishImportTaskCreatesShapesAndMarksTaskPublished` 在空平面图上 `containsExactly("A101")`：把发布改成先清空再写入仍绿。本席 OBS-05：发布是追加、不覆盖已有 shape id；HTML 未禁止叠加，不发明。
- 席甲 CX-002 例①「先存图形再发布 → 两者并存且原图形原样」：并存＝叠加被允许；原图形原样＝已有证据未被静默覆盖。这是锁「不覆盖」，不是锁「禁止叠加」。与 OBS-05 同向。

四分类：**细化**。头条是 PGM-023 HTML 已写的覆盖规则，测试缺口。GR-002 的 200 上限本是技术性，并入④不另开 ID，不升添加。无业务发明。

最小处置五例与原意合取且最懒：① 不覆盖；② 全错 SVG → `failed` 且发布被拒（实现 `:365` 非 parsed）；③ 他任务候选 / 不存在候选；④ 201 → `VALIDATION_FAILED` 且消息含「200」（即 GR-002）；⑤ `listImportTasks`/`getImportDetails` 写 `import_viewed`/`import_candidate_viewed`。2000/1000 收窄 010＝OBS-03。只改 `FloorPlanServiceTests`。

非阻断：② `errorCount=1` 依赖最小输入（缺 viewBox 或无图形）才能稳定为 1；B3 用能稳定产 1 条错误的 payload，或写 `>=1`。不否决。

票：**PASS**。

### 007 独立裁定（技术性／是；即原意且最懒，不新增文件）

本席 B1：Jdbc 一例未知 `project_id` 走真实 `projectExists`；VenueHours 用 `@Transactional` 真实仓库测不存在 venue / 跨项目；只改该测试文件；不改生产 SQL。

候选：`FloorPlanJdbcIntegrationTests` 插入最小 `projects` 行后 `projectExists` 真/未知假；`VenueBusinessHoursServiceTests` **同文件** `@Nested @SpringBootTest @Transactional`，插入最小 `spaces` 行后 `findVenueProjectId` 命中、未知 empty、`listByProject` 只返本项目。

合取：未知假＝原「未知 project_id」；命中真是同一条 SQL 的对称最小正例，不是新规则。Venue 三例覆盖本席「不存在 / 跨项目」（empty + listByProject 过滤）。本模块不得新增文件，嵌套集成是把 Spring 上下文关在内嵌类、外层 RecordingRepository 单测保持零 SQL 的最懒写法。`VenueBusinessHoursRepository.findVenueProjectId` 为包可见，同包内嵌类可调。

核源：`JdbcFloorPlanRepository:65-68` `SELECT COUNT(1) FROM projects`；`VenueBusinessHoursRepository:51-53` `SELECT COALESCE(project_id,'') FROM spaces`。V005 `projects` 必填列为 `venue_type/name/status/created_at/updated_at`；`spaces` 必填列为 `space_name/space_type/status/时间戳`，`polygon_points` 有默认；V203 `spaces.status IN ('active','unavailable')`。无后续 `ALTER TABLE projects/spaces` 加 NOT NULL。

非阻断（B3 边界，不否决）：`projects`/`spaces` 插入用候选所列最小列；`spaces.status` 取 `active`；`space_type` 用已有字典码（如 `shop`），勿发明新类型。内嵌类复用 Jdbc 类的 `complex_test` 数据源属性。`@Transactional` 回滚，不写永久行。

票：**PASS**。

### 008 独立裁定（技术性／是；真实审计库合规）

本席 GR-008：改用真实 `AuditEventService` + 内存库（与 `FloorPlanServiceTests` 同构），save/list 各断言事件名。席甲 CX-003：五处弱 `isInstanceOf` 改 code、mock `verify`、admin 全量列表、`effectiveFrom` 透传。

候选取本席的真实 `AuditEventService(new InMemoryAuditEventRepository())`，不用席甲 mock `verify`——更懒且能红「删掉 `recordSuccess`」。核源 `AuditEventService:17` 单参构造；`VenueBusinessHoursService:48-50/85-92` 写 `viewed/updated`。

五处弱断言（`:148-163`：星期 7 / null、时刻「晚上八点」、结束时间为 null、场地空白）现网均抛 `VALIDATION_FAILED`（`:59-60` 场地空、`:71-72` 星期、`:96-104` 时刻）。空白场地不是 `VENUE_NOT_FOUND`（那是未知 id，已有强断言）。改 `code==VALIDATION_FAILED` 合规，不发明码。

admin 全量与 `effectiveFrom` 是 CX-003 同文件顺手例：`list()` 在 scope 空走 `listAll`（`:45-47`），`upsert` 透传 `effectiveFrom`。不是新业务。可留。

票：**PASS**。

### 003 / 004 / 005 / 006 独立裁定（技术性／是；最小处置最懒）

**003**：本席 GR-003 两列表补 `pageSize==total==items.size()`；Controller 桩改诚实形状或断言与服务一致。候选：Service 各一例；三处桩 `(…,1,50,1)`→`(…,1,1,1)` 并断言 `page_size`/`total`。1 条 items 时诚实 pageSize=1。不新增分页参数。即原意。**PASS**。

**004**：8 端点 create 已有 403；saveShape/createImport 已有 `requireIdempotencyKey`（Controller `:56/:72`）但无缺键例。其余 7 端点 `doThrow→403` + 两写端点缺键 400。成功路径 `verify` 锁的是「被调用过」，不锁 HTTP 403 翻译；缺键删掉后 mock 仍 200。参数化收拢可接受。夹具英文 `permission denied` 按 GEN-015 测试豁免。席甲 OBS-2「verify 足够」不覆盖缺键洞，本席 B1 已独立入队。**PASS**。

**005**：服务非法 `shape_type`（corridor 作负例）→ `VALIDATION_FAILED`；Jdbc `import_candidates` booth/public_area 接受、非法 type 被 CHECK 拒。corridor 不进白名单。Parser 不校验闭集（OBS-17）由服务 `normalizeAllowed` 整单失败，负例已锁。无「导入 booth SVG」正向句——非阻断，正向 booth 已有 `saveShapeAcceptsBoothAndPublicArea`，不否决。**PASS**。

**006**：只对 M015-004/005/006 已改整句补 `getMessage()`/`errorMessage()`（候选项列表不能为空、多边形至少需要 3 个顶点、一次发布…200、场地不能为空、开始/结束营业时间…、不属于…）。解析器中文归 001。不改 FieldLabels。不断言 requireText 抛点英文（与 009 合取）。「导入内容超过 100 万字符上限」用 `"x".repeat(1_000_001)` 即可，不是 OBS-03 的 2000 图形负载。**PASS**。

### 001 独立裁定（技术性／是）

实现 10 个独立 error_code，现测只锁 `POLYLINE_NOT_CLOSED` 与 JSON 侧 `POINT_OUT_OF_RANGE`。未锁 8 码与事实栏列举一致。中文 `errorMessage` 至少一例（POINTS_TOO_FEW「多边形至少需要 3 个顶点」）＝ M015-004 回归锁。不发明第 11 码。SVG 越界与默认 label/shape_type/ref_type 来自 CX-001，仍是现实现分支，无业务发明。

非阻断：处置括注「10 例」应读作闭集 10；未锁码是列出的 **8** 个各一例，加上 SVG 越界与默认值，不是再造 2 个码。不否决。

票：**PASS**。

### 010 独立裁定（观察／否；未写错应升项）

应入队项均在 001–008。GR-009 改登记（见上）不是把缺陷降观察冒充。无观察被偷偷升「是」。

逐条核本席观察：

- OBS-01 现测 `≤64` 不锁恒 36 → 事实栏写成「derivedId 36 字符与 ≤64 断言」，压缩易读成已锁 36。非阻断：B3 不得借本条去加恒 36 断言（那是算法选择，VARCHAR(64) 才是溢出根因）。
- OBS-02…16、18 均在事实栏。OBS-17 并入 005，不在 010 留应升项。
- OBS-05 与 002 ① 合取，事实栏写「HTML 未禁止叠加」，未写错。
- 席甲 OBS-1/2 已升 003/004；OBS-3 candidates CHECK 已升 005；OBS-5 行数 1352/1653 有；OBS-7「无 XM」被 009 重裁覆盖。OBS-4 图形/导入异载荷冲突、OBS-3 V251 中文 permission_name 未点名——本席 B1 未升缺陷，摘要压缩，不构成写错。

票：**PASS（否）**。

### TDD 口径

本模块 FILES 全是测试。被测实现已闭环，主工作区新例对现网应绿。候选「先红＝一次性副本上的实现变异体被新例击红；主工作区只落测试文件」可接受：与 GEN-003「能失败的断言」同构，且不在正本改实现。席甲 `/tmp/m017_dry` 干跑不是本票证据；B3 须自备变异体击红记录。禁止把会相对现网红的草案/二次确认字段写进本 5 文件。

### 分类 / 队列 / 允许的最小修复边界

本席 B2-r1 **PASS**。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3 只改本模块 5 个测试文件，不改任何实现/迁移/OpenAPI/三份 HTML/锁/FieldLabels/Strings。各文件版本头递增：ParserTests/ControllerTests 新加 V0.01，ServiceTests V0.03→V0.04，JdbcIntegrationTests V0.02→V0.03，VenueTests V0.01→V0.02。

- 001：未锁 8 码各最小负例 + SVG 越界一例 + 默认值一例 + 至少一例中文 errorMessage。不发明新码。
- 002：五例（① 并存且原图形原样＝不覆盖；② 失败任务拒发；③ 候选归属；④ 201；⑤ 列表/详情审计）。2000/1000 不测。
- 003：两列表诚实分页；Controller 三处桩改 `(1,1,1)`。
- 004：7 端点 403 + saveShape/createImport 缺键 400。
- 005：corridor 负例；candidates CHECK 正/负。corridor 不进白名单。
- 006：已改中文整句 `getMessage`；可并入 002/008/既有例。
- 007：Jdbc `projectExists`；Venue 同文件嵌套真实仓库。spaces.status=`active`。
- 008：真实 `AuditEventService` + 内存库；五处 `VALIDATION_FAILED`；save/list 事件名。
- 009/010：不修。

禁止：改 Strings/FieldLabels/FloorPlanService/Parser/Controller/VenueBusinessHoursService、加分页参数、把 corridor 进白名单、加二次确认字段、新增测试文件、XM 回 M015 改 requireText、对 debug `raw_message` 断言中文。

### 结论

全批差集无漏并。009 经 Handler/FieldLabels 核源后不再是展示文案缺陷，队列否、不 XM，纠正本席 B1 把 debug `raw_message` 当成出网。002 ① 与 OBS-05 同锁「不覆盖」而非「禁止叠加」。007 同文件嵌套集成即原意。008 真实审计库 + 五处 code 合规。003–006 最懒且只改本 5 文件。010 未写错应升观察。TDD 变异体口径可接受。逐 ID：001 PASS、002 PASS、003 PASS、004 PASS、005 PASS、006 PASS、007 PASS、008 PASS、009 PASS、010 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M017 IN_PROGRESS；B3=0。
