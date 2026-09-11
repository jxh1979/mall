CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M016 B5-r1 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.01（指定路径原先不存在，本次新建完整报告）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员、不读本轮 self-raw）
- 步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B4 双 PASS、B3 自报红→绿或 B1 历史票替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M016 同一 4 文件（V023/V034/V184/V203，计划冻结有效内容行 164）+ 本批新增 `V423__m016_floor_plan_and_space_type_checks.sql`
- 模块：M016 — 空间点位、楼层平面图与营业时段／数据库迁移／第1段
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值（889020 字节 / 6850 行）；sidecar 全文一行同值。独占锁行仅本文第一行，正文不再出现同名独占字段。
- 映射：`M016 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]`（`criteria-lock.md` L6395）COUNT=18 unique=18 MISSING=[] EXTRA=[]；与本席 B1 所列逐字相同；定义从锁自读，不新造判据、不 LOCK-AMEND
- 正本检出：`HEAD=origin/main=fe50e3f309c04f81820f8414bd8860325d694098`（PR #436 已合并）。B3-r1 after tree 对象 `71f1468b8bf3bc19d5624d92bcebd5a3edd99cde` 可解析为 tree；本模块 4 冻结文件工作区 blob = HEAD（未改写已应用迁移）；V423 与 5 个同批修复面文件工作区 blob = 该 after tree（MISMATCH_COUNT=0）
- 本席约束：只读业务代码与计划；除本 raw 外不写文件；不修源/三 HTML/锁/findings/ledger；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；不对任一 `complex_test*` 库做手工写操作。隔离探针库名 `m016_b5_grok_probe`（TEMPLATE complex_test，结束 DROP；`pg_database` 已无此名）
- 未读取：`M016-b5-r1-self-raw`（当前亦不存在）。B4-r1 双席 raw 与 findings V1.97 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界，不代替本轮全量重审）：findings V1.97 — 本批定稿 ID 已修（M016-001…003 已修并经 B4-r1 双 PASS；004 否（观察，含 M018 登记））。B2 定稿禁止项（不得在 B5 升格为缺陷）：改写已应用迁移；加 FK / SET NOT NULL；改 OpenAPI/三份 HTML/权限种子/前端/解析器闭集；新增字段/接口/业务规则；把 corridor / geojson 入库；把跨模块登记项（M015-012 用途/负责人补列、M018 前端默认值）当本模块可修缺陷

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问 2.2、无锁外判据、无 HTML 修改申请。001–003 在当前 4+1 文件与 complex_test 实态上独立复验成立；004 维持队列「否」，本席不升格。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权——本席不行使否决。

---

## 0. 技能自核（V0.17 §6，按 B5 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体。本步禁止子代理，与技能「先派子代理」冲突处按用户指令收窄 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证（NULL/空串、corridor、geojson、NOT VALID、SET NOT NULL、脏值、权限中文、CASCADE），不向操作员发问、不派子代理 |
| grill-me | 同套件 `productivity/grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（锁哈希 / `pg_constraint` / 隔离 INSERT / `mvn -Dtest`）再下结论；B5 不修复 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未运行测试不写通过 |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流（HTML 原句→4+1 迁移→FloorPlanService / JdbcFloorPlanRepository / PlatformService.createSpace/createSpaceType / JdbcContractRepository.spaceRentalStatus / V005/V052/V159/V180/V251/V261 / 测试 / OpenAPI / 前端只读）；B5 不修复 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地：V423 三条 CHECK 是定稿最小面。Lean already. Ship. |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒～十余秒级，未起 monitor |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| domain.md | `docs/agents/domain.md` | 词表/三 HTML 最终解释权；未改 CONTEXT.md |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）、using-git-worktrees、executing-plans、dispatching-parallel-agents、subagent-driven-development。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能（有 `grilling` / `grill-me`）
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit`：与双席/§7 冲突，**不调用**
- Trail of Bits `audit-context-building`：会派 function-analyzer 子代理，**不调用**

明确不用（controller 划掉）：MCP、第三主体、修源/HTML/锁/台账。

---

## 1. 锁、范围与源指纹

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。独立实算值见本文第一行独占锁行。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 字节 | 889020 |
| 行（`wc -l` / `b'\n'` 计数） | 6850 |
| python3 hashlib | 与 sha256sum 同值 |
| openssl dgst -sha256 | 同值 |
| sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` | 全文一行同值 |
| findings | V1.97；SHA256 `8b0f08020e0ed05d2c4c0a936eab9855f2bfb9245f6b16c26d5461c9440a7026`；785724 字节 |
| ledger | SHA256 `aad119169c1b086092e352ddd59e3a55921d4246597c0a5b19d01a8e1beaf846`；605981 字节；M016 `IN_PROGRESS`，B3=1，B5-r1 进行中 |
| 映射行抽出 | 18 条、无重复、MISSING=[] EXTRA=[] |
| 计划 FILES | 4 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 164；当前工作区 4 文件 `\n` 计数 **164**（与冻结一致；V423 不计入计划冻结行） |
| questions | 无 `questions/M016*`；无未答问询 |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| modules.md / runbook.md | SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` / `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| HEAD / origin/main | `fe50e3f309c04f81820f8414bd8860325d694098` ×2 |
| B3-r1 before / after tree | `ced99bc97a1ccceaf075d804ed39e5f6ad74b3e0` / `71f1468b8bf3bc19d5624d92bcebd5a3edd99cde`（`git cat-file -t` 均为 tree） |

三 HTML 工作区 SHA（本席独立，未改；均 `same_head=Y`）：

| 文件 | 现行 | 行 | 字节 | SHA256 |
|---|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | V0.07 | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | V0.14 | 2032 | 304482 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | V0.12 | 979 | 103015 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。锁内本模块仅映射 GEN，HTML 作 2.3 类型 1 证据（不得用未映射的 PGM/BEQR 作本模块 finding 支撑 ID）。

未改（相对 HEAD blob 全等）：OpenAPI `docs/openapi/openapi-p0.yaml`、前端 `apps/web/src/admin/pages/FloorPlanImportPage.tsx`、解析器 `FloorPlanImportParser.java`、`docs/domain-ownership-scope-audit-v0.1.csv`、锁、三 HTML、四份已应用迁移。

说明：对工作区 `git add -A` 于临时索引得到的全树 `dfccf6fd47f3f65a9c2385e0a5f649b05fa53f17` ≠ B3 冻结 after `71f1468…`，原因是 B3 之后写入了 findings/ledger 的 B4 完成节以及本模块 B1/B4 双席 raw（未跟踪）。**修复面 6 文件工作区 blob 与 `71f1468…` 全等**。本席不以全工作区树差否定 B3 after。

---

## 2. 4+1 文件当前行数/字节/SHA 与全量三线覆盖表

行=`b'\n'` 计数。4 冻结文件 `git hash-object` = HEAD blob。V423 `git hash-object` = after tree blob `93543319367f4a0ba7277761ba3c3ce3893b0cc3`。全文已读，未只读标题/搜索/截断。

| # | 路径 | 行 | 字节 | SHA256 | git blob | 覆盖 |
|---|---|---:|---:|---|---|---|
| 1 | `backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql` | 61 | 2828 | `141970f242484accdf99ac37a896ed04762677f5c41d1d4808b2be0ba13815e6` | `60f2b52a20f52a1b255b5d0c2058b58cc1c5ccf7` =HEAD | 已读 L1–L61；第一性/顺从/对抗 |
| 2 | `backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql` | 73 | 3658 | `fd9200070b4f4439c58cb32f4a03d6eb7f62b16c47320a57c2645ab2f101a56b` | `52f4768935fec73b702c4a390e73688b26681173` =HEAD | 已读 L1–L73；第一性/顺从/对抗 |
| 3 | `backend/src/main/resources/db/migration/V184__space_type_dictionary.sql` | 24 | 1535 | `238d1529736908fcf3ff5346e0c7af4636361e035cb7629b27f8729b4895cc52` | `617d4b20eb8e4f70588e789ae27088f381f53548` =HEAD | 已读 L1–L24；第一性/顺从/对抗 |
| 4 | `backend/src/main/resources/db/migration/V203__spaces_status_check.sql` | 6 | 540 | `a159a62c1b8f23a82ddb002a6a625870c469a9250001ed51743a8cfcd3cdb83f` | `a0a893b77a67effda8bc33c2e074563ad46bd8c5` =HEAD | 已读 L1–L6；第一性/顺从/对抗 |
| 5 | `backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql` | 22 | 2099 | `b9912e89df22d03b54305a48af70052c53a58563df8212be0192b60e73ef4043` | `93543319367f4a0ba7277761ba3c3ce3893b0cc3` 未跟踪 | 已读 L1–L22；第一性/顺从/对抗 |

4 文件合计 **164** 行（与计划冻结一致）。5 文件合计 186 行。全部 `endswith \n`。无 OVERSIZE。

修复面核对（非本模块 FILES，只核 001–003 同批落地，不把这些文件扩进 M016 范围）：

| 路径 | 行 | 字节 | SHA256 | after blob | 核对 |
|---|---:|---:|---|---|---|
| `FloorPlanService.java` | 678 | 31232 | `21e7ea67b986b73d827e20a5a24f55c96ad8340659e117bdb22e8714522a193c` | `1a08caed…c12f` | `SHAPE_TYPES` 八值含 booth/public_area；无 corridor；版本头 V0.03 |
| `FloorPlanJdbcIntegrationTests.java` | 244 | 9639 | `018480bf503be1579a00ce6c36ea83b283c24951c161a581029ea17c33f9e9e9` | `9bbcb755…f785` | +2 例打库层；本席实跑 PASS |
| `FloorPlanServiceTests.java` | 715 | 32109 | `03a59adf8c527f0dd92fb42e49188a8c3334ad685d82829621ed449e6e874fe2` | `ef4b7afb…a3cb` | +1 例打白名单；本席实跑 PASS |
| `PlatformJdbcIntegrationTests.java` | 506 | 24001 | `16e57fdb9d033c2d8e396809736790aa46349bce14c089058e0e488c3ac796b6` | `b155b4bf…833a` | +1 例 `status='Active'`；属 M011 文件、本批 TDD 授权 |
| `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` | 371 | 41083 | `33cd57b26ee44008d7be2435940c0ab05427a06e42aee09a305325bb62cc920e` | `7bd106b7…c8db` | 自动块最新迁移 **V423**；表数仍 170 |
| `docs/domain-ownership-scope-audit-v0.1.csv` | 335 | 59958 | `b33ca86505e1327e445740c75f0923300aa064e53a6a1527cb09a4f7b9e4d430` | =HEAD | V423 未建表，零 diff |

只读依赖（不审这些文件本身，但必须追到事实）：V005 `spaces` 列（无用途/负责人）；V052 `mall_floor_plans.merchant_id` 可空；V159 NOT VALID 范式；V180 审计只读码；V251 权限中文；V261 无 `floor_plan*` 命中（高危授 `role_platform_admin` 已在 V023/V034）；`PlatformService.createSpace` L474–478 活校验字典 active 码；`createSpaceType` L457 `trimToDefault(status,"active")` 无白名单；`JdbcPlatformRepository.saveSpaceType` 直写 status；`JdbcContractRepository.listSpaceRentalRaw` L443–477 六态派生；前端 `FloorPlanImportPage.tsx` L17 `corridor`、L52 默认 `geojson`。

---

## 3. 数据流（HTML → 4+1 迁移 → 实现 → 测试库）

### 3.1 HTML 原句（类型 1，本席本轮重读）

治理地图 `docs/platform-governance-map-for-operations-v0.1.html` V0.12：

- L463 h2「空间和点位：所有业务都要知道“发生在哪”」
- L469：维护楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位。
- L470：记录空间面积、用途、状态、负责人和所属商户。
- L472：商场平面图网页导入：在后台上传或导入商场平面布局图，系统解析出候选区域（**铺位、摊位、公共区**），由人工复核候选与错误后发布到正式平面图；发布为高危动作，须二次确认——发布会写入正式平面图区域，不可静默覆盖既有布局证据。

枚举速查 V0.07 L259：铺位租赁状态＝空置、洽谈中、已租、即将到期、已退租、不可租。

HTML **没有**把空间类型写成封闭英文码表，也 **没有**把 `spaces.status` 写成六态租赁看板的存储列。`corridor` / `geojson` 未出现在这三份 HTML。

### 3.2 本模块 4+1 迁移落点（当前）

- **V023**（未改写）：`mall_floor_plans` + `floor_plan_shapes`；形状 CHECK 源文仍六值；`project_id VARCHAR(64)` 源文仍可空、无 FK；权限英文种子授 `role_platform_admin`；子表 `ON DELETE CASCADE`。
- **V034**（未改写）：导入三表；候选 `shape_type` 源文仍六值；`source_format` CHECK `svg/standard_json`；权限英文种子；子表 CASCADE。
- **V184**（未改写）：字典八码；`status` 注释 `active/disabled`、源文仍无 CHECK；`ON CONFLICT (type_code) DO NOTHING`。
- **V203**（未改写）：脏值归一 `active` 后 `chk_spaces_status` 二值。
- **V423**（本批新增，已应用到 complex_test）：两表 shape_type CHECK 重建为八值（+booth/public_area，不含 corridor）；`chk_mall_floor_plans_project_id_required CHECK (project_id IS NOT NULL) NOT VALID`；字典 status 先 UPDATE 脏值→disabled 再 CHECK `(active, disabled)`。无 FK、无 `SET NOT NULL`、不改权限种子、不改已应用四文件正文。

后续非本模块、只作数据流背景：V052 可空 `merchant_id`；V251 四条 `floor_plan.*` 权限名中文；V180 给 `role_auditor_internal` 只授 view 两码。

### 3.3 读写实现（修复面核对）

- `FloorPlanService` L35：`SHAPE_TYPES` 八值，与 V423 CHECK 同闭集，**不含 corridor**。
- L37：`IMPORT_SOURCE_FORMATS` = `svg/standard_json`，与 V034 CHECK 同闭集，**不含 geojson**。
- L508–510：`project_id` 必填且 `projectExists`（M015 已闭环）。
- L320：导入候选 `normalizeAllowed(…, SHAPE_TYPES)`——解析器本身不闭集 shape_type（只抽取），非白名单在服务层整单失败（`idempotencyService.execute` → `transactionTemplate.execute`，异常回滚）。
- `JdbcFloorPlanRepository.saveFloorPlan`：Java `null` 绑 SQL NULL，现被 V423 CHECK 拒绝。
- `PlatformService.createSpace`：`space_type` ∈ `listActiveSpaceTypeCodes()`（只认 `status='active'`）。
- `PlatformService.createSpaceType`：status 仍 `trimToDefault`、无白名单；库层现拒非 `active/disabled`。
- `JdbcContractRepository.listSpaceRentalRaw`：`spaces.status=unavailable` → 不可租；否则按合同/意向派生。六态不在本列。
- 前端未改：L17 仍含 corridor；L52 默认 geojson。服务层拒绝，进不了库（M018）。

### 3.4 complex_test 实态（只读 SELECT；`current_database()=complex_test`）

- Flyway：V023/034/184/203/052/251/005/159/180/261/422/423 均 `success=t`。最新 `423` checksum=`1707295493` installed_on=`2026-09-11 02:28:14`。success_count=**391**。本席 mvn 日志：`DbValidate: Successfully validated 391 migrations`；`Current version of schema "public": 423`；`No migration necessary`。
- `mall_floor_plans.project_id` `is_nullable=YES`（未 SET NOT NULL）；`chk_mall_floor_plans_project_id_required` def=`CHECK ((project_id IS NOT NULL)) NOT VALID`，`convalidated=f`；`fk_project=0`。
- `chk_floor_plan_shapes_shape_type` / `chk_floor_plan_import_candidates_shape_type` 均为 ARRAY 八值含 booth/public_area，**不含 corridor**；`convalidated=t`。
- `chk_space_type_dictionary_status` ARRAY `active/disabled`；`convalidated=t`。八行种子全 `active`，dirty=0。
- `chk_spaces_status` 二值仍在；`spaces` 0 行；`mall_floor_plans` 0 行（Jdbc 测试 `@Transactional` 回滚）。
- 权限名中文（V251）；view=medium/requires_approval=f；manage=high/requires_approval=t。角色：`role_platform_admin` 四码；`role_auditor_internal` 仅 view 两码。
- `source_format` CHECK 仍 `svg/standard_json`（geojson 不在）。

测试后只读复验：`complex_test|423|0|8`（库名/版本/平面图行/字典行）。

---

## 4. 逐文件三线

### 4.1 V023（61 行，全读；正文未改）

**第一性原理**

1. 为什么必须存在：删掉则 `mall_floor_plans` / `floor_plan_shapes` 不存在，治理地图 L472 正式平面图区域无处落库；`floor_plan.view/manage` 无种子。答得上。
2. 声称是否执行：表、索引、原 CHECK、权限种子均在。`project_id` 源文仍可空——列级非空现由 **V423 NOT VALID CHECK** 执行（新写拒 NULL；`is_nullable` 仍 YES）。`shape_type` 源文仍六值——运行时闭集现由 **V423 重建 CHECK** 执行八值。
3. 未验证就当真的前提：曾把「服务层必填」当列级完整。本席用 `pg_constraint` 证伪该前提的**修复前**状态，并证实修复后约束已在。

**顺从式**

- L1–16 平面图表：`floor_no/plan_name/image_* /version_no/status/timestamps` NOT NULL；`building_code` 可空；`chk_*_image_size`、`chk_*_status(active/inactive/archived)`。与服务层 `STATUSES` 一致。
- L21–37 形状表：FK CASCADE；ref_type 六值；polygon 数组≥3。shape_type 源文六值已被 V423 覆盖为运行时八值。
- L46–61 英文权限种子；现网中文由 V251 覆盖，不改写本文件。

**对抗式**

- 直接 SQL `project_id=NULL`：隔离库 NOTICE `NULL_PROJECT rejected`（V423）。空串 `''` 仍被 NOT NULL CHECK 放行（见观察 OBS-01）；服务层 `requireText` 拒空串，唯一 API 写路径不经此。
- `shape_type='booth'/'public_area'`：隔离库 INSERT 成功；`'corridor'` 拒。
- 硬 DELETE 平面图仍 CASCADE。应用 0 处 `DELETE FROM mall_floor_plans`；软删 archived。不把休眠 CASCADE 升缺陷（004 / B2 禁止项）。
- 无 `(project_id, floor_no, version_no)` UNIQUE。HTML 未要求一楼一版。观察，不升。

### 4.2 V034（73 行，全读；正文未改）

**第一性原理**

1. 删掉则导入任务/候选/错误无处落，L472「人工复核候选与错误后发布」做不到。
2. 三表、原 CHECK、索引、权限种子均已应用。候选 shape_type 运行时闭集已被 V423 放宽为八值。`source_format` 仍 `svg/standard_json`；前端默认 geojson 被服务层拒绝，进不了 CHECK（M018，不升）。
3. 发布高危：`import.manage` high + requires_approval=TRUE，对齐 L472。

**顺从式**

- 任务状态含 `cancelled`（无写路径，004 观察）。候选 `pending/published/rejected`。计数 ≥0；`candidate_no>0`。
- `operator_note` 可空 VARCHAR(500)。
- 解析器不改：非白名单在 `normalizeAllowed` 失败；`transactionTemplate.execute` 回滚，不留半成品任务。

**对抗式**

- 隔离库：候选 `booth` 收、`corridor` 拒；任务 `geojson` 拒。
- `(import_task_id, candidate_no)` 仍无 UNIQUE（非唯一索引）。服务层按序编号。004 观察，不升。
- 错误表无 `error_code` CHECK：错误码是技术通道。细化，无缺陷。

### 4.3 V184（24 行，全读；正文未改）

**第一性原理**

1. 删掉则 `createSpace` 硬校验无 active 码表。上位 L469 + 后台可维护字典。HTML 未写死八个英文码。
2. 八行种子均在，全 active；`type_code UNIQUE` 在。
3. 注释声称 status=active/disabled——运行时闭集现由 **V423 CHECK** 执行（`convalidated=t`）。

**顺从式**

- 平台级、无 `project_id`。`ON CONFLICT DO NOTHING` 重跑不覆盖已改中文名。
- 无本表权限种子：复用 `governance.space.manage`。无重复授权发明。
- OpenAPI `mall/bar_table` ≠ 八码：活校验＝字典，已由 M013-004/M014-002 闭环。不重开。

**对抗式**

- 隔离库：`status='Active'` 拒；`disabled` 收。
- `createSpaceType` 仍无服务层白名单（B2：属 M009/M010 不改）。错值现撞 CHECK → `DataIntegrityViolationException` → `GlobalExceptionHandler` 不拦父类、走 500 固定文案（观察 OBS-04，不升本模块缺陷）。
- 无 `mall` 码：与 M014-002 一致。

### 4.4 V203（6 行，全读；正文未改）

**第一性原理**

1. 删掉则 `spaces.status` 无闭集，脏值会让 `spaceRentalStatus` 漏「不可租」或把乱码当可租分母。上位 L259「不可租」人工标注 + L470「记录…状态」。
2. CHECK 已在；服务层 `requireSpaceStatus` 二值一致。六态不在本列。
3. `UPDATE … SET status='active'` 会丢原脏值字符串。complex_test 0 行 spaces。不把「CHECK 只有二值」判成与六态冲突。

**顺从式**

- 列已 NOT NULL（V005）。`DROP IF EXISTS` + `ADD` 可手工重跑；Flyway 不重跑已应用版本。
- 实现：`unavailable` 优先于合同派生（JdbcContractRepository L464）。

**对抗式**

- CHECK 作用于全部 space_type。给非铺位标 unavailable 是占用开关，HTML 未禁止。不升冲突。
- 不改写已应用 V203。

### 4.5 V423（22 行，全读；本批新增）

**第一性原理**

1. 为什么必须存在：已应用四文件不可改写（Flyway checksum；本席 `DbValidate 391` 证明 V023/034/184/203 校验和稳定）。001/002/003 只能新 V。删掉 V423 则列级 project_id 可空、shape_type 无摊位/公共区、字典 status 无闭集——与 L472/实现假设再次不一致。
2. 声称是否执行：三条约束均在 complex_test；隔离库 INSERT 反例全部按声称红/绿；mvn 4 个先红用例现绿。
3. 未验证前提：NOT VALID 是否放行存量 NULL、是否拦新写。隔离库：DROP 后插入 `fp_legacy_null`，再 ADD NOT VALID → `is_null=t` 且 `convalidated=f`；新写 NULL 仍拒；对该存量行 `UPDATE plan_name` 亦拒（新行映像走 CHECK）。`SET NOT NULL` 在有存量 NULL 时失败（NOTICE `contains null values`）——印证定稿不选 SET NOT NULL。

**顺从式**

- L4–9：两表 DROP IF EXISTS + 八值 CHECK，既有六值保留，+booth/public_area，无 corridor。与 `SHAPE_TYPES` 同闭集。
- L13–15：project_id IS NOT NULL NOT VALID；无 FK。
- L19–22：脏值→disabled 后 CHECK active/disabled；无业务发明（闭集取自 V184 注释；归一范式取自 V203）。
- 可重跑：隔离库整段 `\i` 等价 DROP/ADD 成功，`UPDATE 0`。Flyway 不会重跑已应用版本。
- 门禁产物：data-isolation 最新迁移 V423；csv 未变（未建表）。

**对抗式**

- corridor / geojson 仍拒（禁止项遵守）。
- 空串 project_id 放行（OBS-01）。
- 注释 L3「应用白名单与前端闭集另批放宽」：Java 白名单实际同批已放宽，前端仍另批。注释略旧（OBS-02），不是行为缺陷。
- 无新增字段/接口/权限种子/HTML。
- V 号 423 > origin/main 当时最大 422；目录 maxV=423，文件数 391 与 Flyway validate 391 一致。

---

## 5. 十八冻结判据核对

本模块映射仅 GEN。无实现面的写明原因。

| ID | 适用 | 结论 | 证据 |
|---|---|---|---|
| GEN-001 | 是 | 4+1 均有用途；V423 是已应用迁移不可改写下的最小新 V，无纯转发。entrance/facility/risk_zone/route 是平面图标注细化。不删表。 | 删除测试：无 V023/V034 则 L472 落库失败；无 V184 则 createSpace 无字典；无 V203 则租赁「不可租」无列级保护；无 V423 则 001–003 复发 |
| GEN-002 | 是 | DDL 已应用；Flyway validate 391；指定 6 类测试 75/75 exit 0。行为与 L472 候选类型现一致（库+服务）。 | mvn 日志；surefire XML |
| GEN-003 | 本模块无测试文件；由 M017/M011/M015 测试类覆盖 | 本席实跑：Jdbc 5/5（含 `floorPlanWithoutProjectIsRejectedByDatabase` / `shapeTypeBoothAndPublicAreaAreAcceptedByDatabase`）、PlatformJdbc 17/17（含 `spaceTypeDictionaryRejectsUnknownStatus`）、Service 28/28（含 `saveShapeAcceptsBoothAndPublicArea`）、Controller 12/12、Parser 4/4、VenueHours 9/9。断言打真实 CHECK 或白名单，能失败。缺口：无单独用例打 import_candidates booth（隔离 INSERT 已补证，OBS-03）。未把「测试通过」当与 HTML 一致的替代。 | surefire 时间戳 2026-09-11T02:49 |
| GEN-004 | 是 | 服务层校验 project_id/shape_type/source_format/status；列级 project_id 现 NOT VALID CHECK 拒 NULL；字典 status 现 CHECK。JSONB 只检 array≥3，顶点在服务层。空串 project_id 列级不拒（OBS-01）；API `requireText` 拒。 | pg_constraint；隔离 INSERT；FloorPlanService L508/L635 |
| GEN-005 | 是 | 四码种子；manage/import.manage = high + requires_approval，对齐发布高危。本文件只授 platform_admin。审计只读由 V180 后加。无「仅靠界面隐藏」。V423 未改授权。 | permissions / role_permissions SELECT |
| GEN-006 | 是 | project_id 新写非空已落地（NOT VALID）；形状/导入 FK CASCADE；V184 type_code UNIQUE；V203 + 字典 status CHECK。候选序号无 UNIQUE：004 观察。不加 FK 为 B2 禁止项，服务层 `projectExists` 仍在。 | information_schema；pg_constraint；隔离 SET NOT NULL 失败 |
| GEN-007 | 是 | 权限/字典种子 ON CONFLICT；PK；Flyway 不重跑已应用版本；V423 DROP IF EXISTS 可重跑。导入写路径走 `idempotencyService.execute` → `transactionTemplate.execute`。 | 迁移文本；IdempotencyService L171；隔离 rerun OK |
| GEN-008 | 是 | shape_type 运行时 CHECK = 服务白名单 = L472 铺位/摊位/公共区（shop/booth/public_area）；≠ 前端 corridor（M018，服务拒）。source_format = 后端 ≠ 前端默认 geojson（M018）。V184 八码 ≠ OpenAPI 快照（已闭环）。V203 二值 = 服务层；六态派生非冲突。 | pg_get_constraintdef；SHAPE_TYPES L35；前端 L17/L52 |
| GEN-009 | 是 | 导入错误表 + failed 状态。V423 先 UPDATE 再 ADD CHECK，同事务迁移。隔离库 UPDATE 存量 NULL 行被拒（不留下「改了一半」的成功）。createSpaceType 错 status 走 500（OBS-04，他模块映射）。 | V034 L45–53；V423 L19–22；GlobalExceptionHandler L151–167 |
| GEN-010 | 是 | 静态 SQL，无拼接用户输入；无密钥。权限码非秘密。隔离探针结束 DROP。 | 五文件全文 |
| GEN-011 | 是 | 证据在 flyway_schema_history + 权限种子 + 本 raw 命令输出。本模块无审计表。服务层 audit event 属 M015。 | flyway SELECT；DbValidate 391 |
| GEN-012 | 是 | 索引齐全；image_width/height>0；候选计数≥0。V423 无无界批处理。 | pg_constraint / 原索引仍在 |
| GEN-013 | 是 | V 号 023<034<184<203<423；validate 391 通过；`MigrationVersions.latest()` 动态读目录（%03d，423 原样）。不依赖未声明本机状态。只导出三个 COMPLEX_DATABASE_*。 | flyway_schema_history；MigrationVersions.java L25–40 |
| GEN-014 | 是 | 四已应用文件 checksum 通过 validate，**未改写**（blob=HEAD）。缺陷以新 V423 落地且已应用 success=t。破坏性动作：无 DROP TABLE / SET NOT NULL / 删权限。NOT VALID 受 V159 已记录范式与 B2 授权。 | DbValidate 391；HEAD blob 全等；spec L59 V423 |
| GEN-015 | 本模块无界面 | 权限名面向操作员：本文件英文种子，**现网中文**（V251 L193–196）。不建议改写已应用 V023/V034。无键盘/焦点面。 | permissions SELECT；无 UI 文件在 FILES |
| GEN-016 | 注释 + 门禁产物 | V184/V203 注释与实现分列一致；data-isolation 最新迁移 V423 可复现；csv 未漂。V423 L3「另批」对前端为真、对 Java 白名单已同批（OBS-02）。注释本身不是 2.3 依据。 | spec L59；V423 L1–3 |
| GEN-017 | 是 | 分类顺序冲突→细化→技术性→添加。001/002 细化已修；003 技术性已修；004 观察否。本轮无添加、无新冲突。未把 HTML 沉默（corridor/geojson/卡座码）升冲突或添加。 | §6–7 |
| GEN-018 | 是 | 结论落到 HTML 原文、Flyway/surefire 产物、PostgreSQL NOT VALID/CHECK 行为、可复现命令。未用惯例/历史席位意见当依据。B4 PASS 不作本票证据。 | 本文命令节 |

---

## 6. 定稿 ID 独立复验（不代替全量三线）

| ID | 分类 | 工程缺陷（历史） | 当前队列 | 本席 B5 复验 |
|---|---|---|---|---|
| M016-001 | 细化 | 是（已修） | 否 | **已修成立**。complex_test：`CHECK ((project_id IS NOT NULL)) NOT VALID`、`convalidated=f`、`is_nullable=YES`、`fk_project=0`。Jdbc 例 `floorPlanWithoutProjectIsRejectedByDatabase` PASS（0.137s）。隔离库新写 NULL 拒；存量 NULL + NOT VALID 保留；SET NOT NULL 失败。未改写 V023。 |
| M016-002 | 细化 | 是（已修） | 否 | **已修成立**。两表 CHECK 八值含 booth/public_area、无 corridor。`SHAPE_TYPES` 同八值。Jdbc 例 + Service 例 PASS。隔离库 shapes/candidates booth 收、corridor 拒。解析器/前端未改。 |
| M016-003 | 技术性 | 是（已修） | 否 | **已修成立**。`chk_space_type_dictionary_status` 二值 `convalidated=t`。八行仍 active。Jdbc 例 `status='Active'` PASS。隔离库 Active 拒、disabled 收。无业务发明。 |
| M016-004 | 技术性/正向 | 否 | 否 | **维持否**。diff/当前树：前端 corridor/geojson 仍在且未入库；权限中文仍来自 V251；auditor 只读；CASCADE 休眠；cancelled 无写路径；候选无 UNIQUE(task,no)；spaces.space_type 软引用；V052 merchant_id 可空；M015-012 用途/负责人仍无列。本席不升格。 |

---

## 7. 本次 findings / 观察

无新的工程缺陷成立项。无添加项。无 HTML 修改申请。无锁外判据。

### 观察（队列否，不升缺陷）

#### GR-M016-B5-OBS-01

- **文件:行号**：`V423__m016_floor_plan_and_space_type_checks.sql:15` `CHECK (project_id IS NOT NULL) NOT VALID`
- **事实**：隔离库 INSERT `project_id=''` NOTICE `EMPTY_STRING_PROJECT accepted`。PostgreSQL `IS NOT NULL` 不拒空串。
- **同根因**：M016-001 的残余边界。B2 最小处置明文是 `IS NOT NULL`（不是 `btrim<>''`），服务层 `requireText` 已拒空白。唯一 API 写路径不经此。
- **冻结判据**：GEN-004（列级格式）；不单列缺陷
- **四分类**：技术性；工程缺陷 **否**；不入队
- **禁止升格**：把空串补进 CHECK 将超出 B2 最小面，且接近「新业务规则」。本席不升。

#### GR-M016-B5-OBS-02

- **文件:行号**：`V423:3` 注释「应用白名单（FloorPlanService.SHAPE_TYPES）与前端闭集另批放宽」
- **事实**：Java `SHAPE_TYPES` 已同批改为八值；前端仍未改（M018）。注释对前端为真、对 Java 已过时。
- **冻结判据**：GEN-016
- **四分类**：技术性；工程缺陷 **否**（注释不影响运行时闭集）

#### GR-M016-B5-OBS-03

- **文件:行号**：`V423:7–9` 候选表 CHECK；测试只打 `floor_plan_shapes`
- **事实**：`pg_get_constraintdef` 两表闭集逐字相同；隔离库候选 booth 收、corridor 拒。GEN-003 关键分支有命令证据，缺的是 M017 永久用例。
- **四分类**：技术性；工程缺陷 **否**（禁止本席写永久测试）

#### GR-M016-B5-OBS-04

- **文件:行号**：`PlatformService.java:457` `trimToDefault(request.status(),"active")`；`GlobalExceptionHandler.java:151–167`
- **事实**：字典 CHECK 落地后，API 传 `Active` 不再「假停用」，而撞 CHECK → 500 固定文案。B2 明示服务层 status 校验属 M009/M010 不改。
- **同根因**：M016-003 的服务层半边（已登记不改）
- **四分类**：技术性；工程缺陷 **否**（本模块）；不升、不 LOCK-AMEND

#### 004 登记项（不新开 ID）

权限英文种子已被 V251 覆盖；高危码授 platform_admin；auditor 只读来自 V180；CASCADE 惰性；`cancelled` 无写路径；候选无 UNIQUE(task,no)；V203 脏值归一属拍板；`spaces.space_type` 软引用；V052 `merchant_id` 可空；M015-012 用途/负责人全局遗漏；前端 corridor / sourceFormat 默认 geojson（M018）；M017 夹具 `prj_jdbc` 不经 projectExists（本批不加 FK）。均保持观察。

---

## 8. 添加项

无。不把前端 `geojson`/`corridor`、OpenAPI `mall`、HTML「卡座/吧台位」名词写成必须新增的 CHECK/种子码。不申请改三份 HTML。

---

## 9. 命令证据摘要

环境：只从 `/etc/complex/complex.env` **grep 三行** `COMPLEX_DATABASE_URL` / `_USERNAME` / `_PASSWORD`，不 source 整文件。URL 改写库名为 `complex_test`。口令在本报告中一律 REDACTED。

```
sha256sum / python3 hashlib / openssl dgst -sha256
  docs/audit-2026-09/plan/criteria-lock.md
  → b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a ×3
git rev-parse HEAD origin/main
  → fe50e3f309c04f81820f8414bd8860325d694098 ×2
git cat-file -t 71f1468b8bf3bc19d5624d92bcebd5a3edd99cde ced99bc97a1ccceaf075d804ed39e5f6ad74b3e0
  → tree / tree
4 冻结文件 git hash-object = HEAD blob（same_head=True）
V423 blob 93543319367f4a0ba7277761ba3c3ce3893b0cc3 = after tree
4 文件 nl 合计 164

psql -d complex_test（只读）exit 0
  current_database=complex_test
  flyway latest=423 success=t checksum=1707295493 success_count=391
  project_id is_nullable=YES；chk_mall_floor_plans_project_id_required NOT VALID convalidated=f
  fk_project=0
  shape/candidate CHECK 八值含 booth/public_area 无 corridor
  dict CHECK active/disabled；8 行全 active dirty=0
  permissions 四码中文；auditor 仅 view 两码
  mall_floor_plans 0 行；spaces 0 行
  测后复验：complex_test|423|0|8

createdb m016_b5_grok_probe -T complex_test ；事后 DROP（pg_database 已无此名）
  NULL_PROJECT rejected
  EMPTY_STRING_PROJECT accepted
  booth/public_area shapes INSERT 0 2
  CORRIDOR rejected
  GEOJSON rejected
  candidate booth 收；CAND_CORRIDOR rejected
  DICT_Active rejected；disabled 收
  NOT VALID：存量 NULL is_null=t convalidated=f；新写 NULL 拒
  SET_NOT_NULL failed: column "project_id" … contains null values
  V423_RERUN_OK
  UPDATE_LEGACY_NULL rejected
  PROBE_SQL_OK

cd backend
export COMPLEX_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/complex_test
export COMPLEX_DATABASE_USERNAME=complex
export COMPLEX_DATABASE_PASSWORD=<REDACTED>
mvn -o -q -Dtest=FloorPlanJdbcIntegrationTests,PlatformJdbcIntegrationTests,FloorPlanServiceTests,FloorPlanControllerTests,FloorPlanImportParserTests,VenueBusinessHoursServiceTests -Dsurefire.failIfNoSpecifiedTests=false test
  MVN_EXIT=0
  Flyway: validated 391 ；schema public v423 up to date
  surefire:
    FloorPlanJdbcIntegrationTests     tests=5  failures=0 errors=0 skipped=0
    PlatformJdbcIntegrationTests      tests=17 failures=0 errors=0 skipped=0
    FloorPlanServiceTests             tests=28 failures=0 errors=0 skipped=0
    FloorPlanControllerTests          tests=12 failures=0 errors=0 skipped=0
    FloorPlanImportParserTests        tests=4  failures=0 errors=0 skipped=0
    VenueBusinessHoursServiceTests    tests=9  failures=0 errors=0 skipped=0
  合计 75/75
  先红四例均 PASS：
    floorPlanWithoutProjectIsRejectedByDatabase
    shapeTypeBoothAndPublicAreaAreAcceptedByDatabase
    saveShapeAcceptsBoothAndPublicArea
    spaceTypeDictionaryRejectsUnknownStatus
```

未运行、因而不声称通过：全后端测试、部署、`flyway:migrate` 手工、`npm run verify:migration-version-order`（本席以目录 maxV=423 与 DbValidate 391 代替）、任何对 `complex_test*` 的手工 INSERT/UPDATE/DDL。

本席未改 git 主索引、未 commit/push/PR/merge。`git status --porcelain` 在本席落盘本 raw **之前**仅为 B3 修复面 + findings/ledger + 本模块 B1/B4 raw。

---

## 10. 审计结论

**PASS**

理由：整模块 4 冻结迁移 + V423 与 18 条 GEN 已三线审完；001–003 在库实态、隔离反例与现有测试上独立复验已修；004 及本轮四条观察均不构成未定稿缺陷。无添加项、无未答 2.2、无 HTML 人闸、无锁外判据。不存在有条件通过。B5 禁止修复，本席未修。本票不等于双席 B5，也不等于模块 PASS。
