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

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M016 B5-r1 再执行 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.02（指定路径已有 37973 bytes V0.01 前缀，SHA256 `b5add5923387475c2fc89971bd69b347f70286eddc6379e896cb81d0ed9279cd`；本席追加本次完整报告，不覆盖）
- 席位：固定 Grok 外审席乙（grok-4.6 / reasoning xhigh；不派子代理、不用 MCP、不问操作员）
- 步骤：B5-r1 再执行（操作员 2026-09-20 裁定：历史 2026-09-11 双 PASS 收口 + 席甲单席补充复查之后，须补派席乙完整 B5 全量重审；同一 B3 次数=1，同名 raw 追加）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M016 4 文件 + 本模块 B3 产物 `V423__m016_floor_plan_and_space_type_checks.sql`
- 模块：M016 — 空间点位、楼层平面图与营业时段／数据库迁移／第1段
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算（未抄 sidecar / ledger / 派发期望值）：`sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值；889020 字节 / 6850 行。独占锁行仅本次报告第一行。
- 映射：`criteria-lock.md` L6395 `M016 → [GEN-001 … GEN-018]` COUNT=18 unique=18 MISSING=[] EXTRA=[]
- 正本：工作区 `/home/james/complex-management` 分支 `main`。`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`；本地 `origin/main=1e5183d52f809cce89739099b47a8ac2fce5c89e`（本席未 fetch）。本模块 4 冻结文件 + V423 工作区 blob = `HEAD` blob = `origin/main` blob（未改写已应用迁移）。
- 互盲：本席未读 `M016-b5-r1-self-raw.md` 任何段落（含席甲 2026-09-20 补充复查与本次再执行追加段）。可读且已读：旧 B1/B4/B5 grok-raw 前缀、findings/ledger 定稿边界、160 提示词 V0.17、三 HTML 相关锚点。旧票不作本轮通过依据。
- 已定稿边界（只作边界，不代替本轮全量重审）：M016-001…003 已修于 V423、队列按操作员/ledger 叙事为「否」；M016-004 观察「否」。B2 禁止项仍禁止：改写已应用迁移；加 FK / SET NOT NULL；改 OpenAPI/三份 HTML/权限种子/前端/解析器闭集；把 corridor / geojson 入库；把 M018/M009 后续迁移当本模块缺陷立项。
- 本席约束：只读；除本 raw 外不写文件；不修源/HTML/锁/findings/ledger；不 fetch/commit/push/merge/deploy；不对 `complex` 生产库或 `complex_test` 做手工写/DDL；不建脚本/日志/临时产物。

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问 2.2、无锁外判据、无 HTML 修改申请。001–003 在当前 4+1 文件与 `complex_test` 实态上独立复验仍成立（003 的两值 CHECK 已被后续 M018 V425 扩为四值，见登记、不立项）。004 维持队列「否」。模块生命周期由席甲在合取后写入；本票不等于双席 B5。

---

## 0. 技能自核（V0.17 §6，按 B5 收窄；环境实用强制技能如实记录）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令优先。禁止子代理处按派发收窄 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自问自答（闭集/CHECK/NOT VALID/软引用/脏值归一/幂等重跑/权限种子）；不等操作员、不派子代理 |
| grill-me | 同套件 `productivity/grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（锁哈希 / `pg_constraint` / Flyway 行 CRC / `mvn -Dtest` XML）再下结论；B5 不修复 |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流；B5 不修 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未跑的不写通过 |
| codebase-design | mattpocock 同名 | 只借 seam/interface 词汇：库层 CHECK 是迁移模块对外接口；不改代码 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地。V423 三条 CHECK 仍是定稿最小面。Lean already. Ship. |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步 `mvn` 约十余秒级完成，未起 monitor |
| i-have-adhd | `~/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 存在；`disable-model-invocation`；本 raw 用编号章节，CLI 不回正文 |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 可选存在。本体要装 hook 并问人，B5 不安装、不调用；本席未 push/reset |
| writing-for-agents | mattpocock 同名 | B5 不写收口文件，不调用本体 |
| tdd / test-driven-development | 均存在 | 仅 B3；本席核既有先红用例仍能失败（`Active` / NULL project_id / corridor），不新写测试 |
| resolving-merge-conflicts | 存在 | 无进行中 merge，不调用 |
| graft | `~/.claude/skills/graft/SKILL.md` | 存在。派发已给出精确路径且 5 文件全文已读，本轮未调用 graft 工具 |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、ponytail 落地（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、using-git-worktrees、executing-plans、dispatching-parallel-agents、subagent-driven-development。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能
- `ubiquitous-language`：未找到（`domain-modeling` 存在，V0.17 明确不用）

明确不用：MCP、第三主体、修源/HTML/锁/台账。

---

## 1. 锁、范围与源指纹

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算）。三工具同值见本次报告第一行。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 字节 / 行 | 889020 / 6850 |
| sidecar `plan/criteria-lock.sha256` | 全文一行同值 |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| modules.md / runbook.md | SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` / `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| findings.md | SHA256 `f2ccaf6ced3d3f1a9a63802257cf89781333a35d71e51595b4bff9d3769ad4e4`；993455 字节。表行 M016-001…003 仍写缺陷「是」/队列「是」（B2 当时定稿快照）；操作员/ledger 叙事已改为「否」。`docs/audit-2026-09/**` 排除范围，不作本模块代码缺陷 |
| ledger.md | SHA256 `e911d88c37472f2bae2a736abd453025325ee4647cd09916ac2d9c62f5b9e19a`；908827 字节。M016 行 `IN_PROGRESS`，B3=1 |
| 映射抽出 | 18 条、无重复、MISSING=[] EXTRA=[] |
| 计划 FILES | 4 条与 modules.md FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 164；`git ls-files -z -- <4 paths> \| xargs -0 cat \| wc -l` = **164** |
| questions | 无 `questions/M016*` |
| HEAD / origin/main（未 fetch） | `e456ba96…` / `1e5183d5…`（ahead/behind 本地记账；本模块 5 文件 blob 与 origin/main 全等） |

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 现行 | 行 | 字节 | SHA256 |
|---|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | V0.07 | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | V0.14 | 2032 | 304482 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | V0.12 | 979 | 103015 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

三 HTML 本身未发现需铁律 4 人闸修改的原文错误。锁内本模块仅映射 GEN；HTML 作 2.3 类型 1 证据，不得用未映射的 PGM/BEQR/BSM ID 作本模块 finding 支撑。

---

## 2. 4+1 文件覆盖表（全文已读，未只看 diff）

行 = `b'\n'` 计数。4 冻结 + V423：`git hash-object` = `HEAD:<path>` = `origin/main:<path>`。全部 `endswith \\n`。无 OVERSIZE。

| # | 路径 | 行 | 字节 | SHA256 | git blob | 覆盖 |
|---|---|---:|---:|---|---|---|
| 1 | `backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql` | 61 | 2828 | `141970f242484accdf99ac37a896ed04762677f5c41d1d4808b2be0ba13815e6` | `60f2b52a20f52a1b255b5d0c2058b58cc1c5ccf7` | 已读 L1–L61；三线 |
| 2 | `backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql` | 73 | 3658 | `fd9200070b4f4439c58cb32f4a03d6eb7f62b16c47320a57c2645ab2f101a56b` | `52f4768935fec73b702c4a390e73688b26681173` | 已读 L1–L73；三线 |
| 3 | `backend/src/main/resources/db/migration/V184__space_type_dictionary.sql` | 24 | 1535 | `238d1529736908fcf3ff5346e0c7af4636361e035cb7629b27f8729b4895cc52` | `617d4b20eb8e4f70588e789ae27088f381f53548` | 已读 L1–L24；三线 |
| 4 | `backend/src/main/resources/db/migration/V203__spaces_status_check.sql` | 6 | 540 | `a159a62c1b8f23a82ddb002a6a625870c469a9250001ed51743a8cfcd3cdb83f` | `a0a893b77a67effda8bc33c2e074563ad46bd8c5` | 已读 L1–L6；三线 |
| 5 | `backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql` | 22 | 2099 | `b9912e89df22d03b54305a48af70052c53a58563df8212be0192b60e73ef4043` | `93543319367f4a0ba7277761ba3c3ce3893b0cc3` | 已读 L1–L22；三线 |

4 文件合计 **164** 行。5 文件合计 186 行。

Flyway 行 CRC32（逐行 UTF-8、不含换行，signed 32-bit）与 `flyway_schema_history.checksum` **逐条相等**（类型 4）：V023=1247380439、V034=1294355959、V184=-1668892912、V203=83253196、V423=1707295493。已应用迁移未被改写。

只读依赖（不扩进 M016 FILES）：`FloorPlanService.java` 版本头 V0.04，`SHAPE_TYPES` 八值含 booth/public_area、无 corridor（L34–35）；`IMPORT_SOURCE_FORMATS` = svg/standard_json（L37）；`validateCreateRequest` `requireText(projectId)` + `projectExists`（L508–513）；`Strings.requireText` 空白→VALIDATION_FAILED（`Strings.java` L27–32）。`PlatformService.createSpaceType` 现写死 `status="active"`（L557，属后续服务层，非本 5 文件）。`JdbcPlatformRepository.listActiveSpaceTypeCodes` 只认 `status='active'`（L324–327）。V052 可空 `merchant_id`。V251 四条 `floor_plan.*` 权限名中文。V180 审计只读码。前端 corridor / 默认 geojson 属 M018。

---

## 3. 第一性原理三问（逐文件）

HTML 类型 1 原文（本席本轮重读，非凭印象）：

- PGM `docs/platform-governance-map-for-operations-v0.1.html` L463 h2「空间和点位：所有业务都要知道“发生在哪”」；L469「维护楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位。」；L470「记录空间面积、用途、状态、负责人和所属商户。」；L472「商场平面图网页导入：…系统解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误后发布到正式平面图；发布为高危动作，须二次确认——发布会写入正式平面图区域，不可静默覆盖既有布局证据。」
- PGM L442–443：综合体项目「为商户、空间、合同、员工、设备、票务和看板提供归属。」
- BSM L566–577 h3「配置数据可以存档、也可以删除（删除要审批）」：存档隐藏、删除走审批软删除。示例列举商户/票种等，**未把空间类型字典写成该条的封闭名单**；后续 M018 用四值承接该通用规则，对本模块只登记。
- BSM L587：平台管理员拥有全部日常功能；高危须单独授予。V023/V034 manage 码 `risk_level=high, requires_approval=TRUE`。
- BSM L694–700：「建立综合体项目、楼层、区域、点位」。
- BEQR L259：「铺位租赁状态」空置/洽谈中/已租/即将到期/已退租/不可租。**不是** `spaces.status` 存储闭集。
- 三份 HTML **没有**空间类型英文码闭集，**没有** `corridor` / `geojson`。

### 3.1 V023

1. 为什么必须存在：删掉则 `mall_floor_plans` / `floor_plan_shapes` 不存在，PGM L472 正式平面图区域与 L469 楼层/区域/点位无处落库；`floor_plan.view/manage` 无种子。答得上。
2. 声称是否执行：表、索引、原 CHECK、权限种子均在 `complex_test`（类型 4 `pg_constraint` / `permissions`）。`project_id` 源文仍可空——列级非空由 **V423 NOT VALID CHECK** 执行。`shape_type` 源文仍六值——运行时闭集由 **V423 重建 CHECK 八值** 执行。
3. 未验证就当真的前提：不得把「服务层必填」当成列级完整；本席用 `information_schema.columns.is_nullable=YES` 与 `convalidated=f` 钉死列仍可空、约束 NOT VALID。

### 3.2 V034

1. 删掉则导入任务/候选/错误无处落，L472「人工复核候选与错误后发布」做不到。答得上。
2. 三表、原 CHECK、索引、权限种子已应用。候选 `shape_type` 运行时八值来自 V423。`source_format` 仍 svg/standard_json。
3. 不得把前端默认 geojson 当成库已接受——CHECK 与服务白名单均不含 geojson（M018 登记）。

### 3.3 V184

1. 删掉则无空间类型字典，PGM L469 商铺/摊位/公共区等种类无平台级参考数据；`createSpace` 活校验无 active 码表。答得上。HTML 未写死八个英文码，八码是细化（GEN-017）。
2. 八行种子均在且 `status=active`（类型 4 SELECT）。源文无 CHECK；V423 曾加两值 CHECK；活库现为 V425 四值。
3. 不得把注释 `-- active/disabled` 当成当前活约束；活约束以 `pg_get_constraintdef` 为准。

### 3.4 V203

1. 删掉则 `spaces.status` 无闭集，手抖脏值会让铺位漏出/漏回出租率分母（文件 L1–3 自述与 BEQR L259 六态看板的存储/派生分离）。答得上：本列是「可租/不可租」存储，六态由合同侧派生（非本模块文件）。
2. `chk_spaces_status` 活库二值 `active/unavailable`，`convalidated=t`。当前 `spaces` 0 行，脏值归一在空表上为无操作。
3. 不得把 `spaces.status` 当成租赁六态存储。

### 3.5 V423

1. 为什么必须存在：B3 对 001–003 的唯一合法处置（已应用迁移不得改写）。删掉则 project_id 可空、shape_type 无摊位/公共区、字典 status 无 CHECK。答得上。
2. 三条语句均曾应用到 `complex_test`（Flyway V423 success=t，checksum 与文件行 CRC 相等）。其后 V425 `DROP` 了字典两值 CHECK 并重建四值——这是 M018 后续迁移，不是 V423 文件被改写。
3. 不得把「V423 源文两值」当成「活库现在两值」。

---

## 4. 顺从式：GEN-001 … GEN-018 逐项

每条：适用判定 + 证据（类型 1/2/3/4）。不新增判据。

| ID | 适用 | 证据 |
|---|---|---|
| GEN-001 必要性与最小性 | 适用 | 五文件各有 HTML 上位或 B3 定稿修复用途（§3）。V423 只重建 CHECK / NOT VALID 非空 / 字典闭集，无新表新列。ponytail-review：无可删抽象。Lean already. Ship. |
| GEN-002 可执行正确性 | 适用 | Flyway `DbValidate` 395 条成功；`Current version 427`；`No migration necessary`（mvn 日志，类型 2）。本席 psql `current_database=complex_test`。四条先红用例所在测试类 XML 全绿（§8）。 |
| GEN-003 测试有效性 | 适用（测试文件属 M017/M011，本席按派发实跑） | `FloorPlanJdbcIntegrationTests` 7、`PlatformJdbcIntegrationTests` 23、`FloorPlanServiceTests` 39；XML tests/failures/errors/skipped=7/0/0/0、23/0/0/0、39/0/0/0。无 `@Nested`。能失败的断言仍在：`floorPlanWithoutProjectIsRejectedByDatabase`（NULL）、`shapeTypeBoothAndPublicAreaAreAcceptedByDatabase`、`importCandidateShapeTypeFollowsDatabaseCheck`（corridor 拒）、`saveShapeAcceptsBoothAndPublicArea`、`saveShapeRejectsShapeTypeOutsideClosedSet`、`spaceTypeDictionaryRejectsUnknownStatus`（`Active`）。测试通过本身不替代 §8 命令输出。 |
| GEN-004 信任边界校验 | 适用 | 库层：status/shape_type/ref_type/source_format/polygon 数组/image_size/candidate_no 均有 CHECK。服务层（只读追流）`requireText`/`normalizeAllowed` 在首次落库前校验。字典 `type_code` UNIQUE。 |
| GEN-005 授权与最小权限 | 适用 | V023 L46–61、V034 L58–73 种子 view=medium/无需审批、manage=high/需审批，授 `role_platform_admin`。活库权限名中文（V251），auditor 仅 view 两码（V180）。高危发布不在本 5 文件实现二次确认（服务/前端 M015/M018）。 |
| GEN-006 数据完整性 | 适用 | 001：`chk_mall_floor_plans_project_id_required CHECK (project_id IS NOT NULL) NOT VALID`，`convalidated=f`，列 `is_nullable=YES`（无 SET NOT NULL）。002：两表 shape_type 八值 CHECK `convalidated=t`。003：字典 status CHECK 仍存在；活库四值（V425）。V203 二值。子表 FK ON DELETE CASCADE。无 project_id FK（软引用，与 `spaces.project_id` 同范式）。 |
| GEN-007 并发与幂等 | 适用 | V023/V034 权限 `ON CONFLICT DO UPDATE` / `DO NOTHING`；V184 种子 `ON CONFLICT (type_code) DO NOTHING`；V423 `DROP CONSTRAINT IF EXISTS` 后重建。Flyway 已应用则不再跑，防 V423 的 UPDATE 在四值时代把 archived 打回 disabled。 |
| GEN-008 接口与模式一致性 | 适用 | 服务 `SHAPE_TYPES` 八值 = 活库两表 CHECK 八值 = PGM L472 铺位/摊位/公共区（shop/booth/public_area）+ 既有六值。`STATUSES` 三值 = 平面图/形状 CHECK。`IMPORT_SOURCE_FORMATS` = V034 CHECK。字典源文注释两值 ≠ 活库四值：属 M018 扩闭集，源文件不得改写。 |
| GEN-009 错误处理与恢复 | 适用 | CHECK 拒绝走 SQL 完整性错误；Jdbc 测试断言 `DataIntegrityViolationException`。服务层非闭集 `VALIDATION_FAILED` 中文（`saveShapeRejectsShapeTypeOutsideClosedSet`）。V203/V423 先归一再加 CHECK，避免加约束失败留下半成品。 |
| GEN-010 安全与敏感信息 | 适用 | 五文件无秘密、无动态 SQL 拼接。权限名/码非密钥。polygon JSONB 类型检查防非数组。 |
| GEN-011 可观测与审计证据 | 适用（弱） | 表/约束/权限行可查。操作审计事件在服务层（非本 FILES）。Flyway history 保留 applied 证据。 |
| GEN-012 资源与性能边界 | 适用（弱） | 索引：`idx_mall_floor_plans_project_floor`、shapes (plan,status,z_index)、import 任务/候选/错误。无无界循环。导入条数上限在服务层（M015），非本迁移。 |
| GEN-013 构建配置与依赖 | 适用 | 迁移被 Flyway 校验；测试只 export 三个 `COMPLEX_DATABASE_*`，URL 库名改为 `complex_test`，未 source `/etc/complex/complex.env`，未连 `complex`。`SPRING_PROFILES_ACTIVE` 未导出。 |
| GEN-014 数据库迁移正确性 | 适用 | 版本串行：023/034/184/203/423 均 success。checksum 与文件行 CRC 全等。破坏性动作：V203/V423 UPDATE 脏值有文件内授权说明；V423 NOT VALID 明确不发明归属值。后续 V424–V426/V427 不在本 FILES，见 §7。 |
| GEN-015 界面可用性与可访问性 | 不适用本 5 文件 | 无 UI。相关前端 M018。权限种子英文已被 V251 中文化（展示约定在后续迁移，不改写 V023/V034）。 |
| GEN-016 文档可验证性 | 适用（弱） | 文件头注释与活约束不完全同文（V184/V423 仍写 active/disabled）。已应用迁移不得改注释。登记为观察，不立项。 |
| GEN-017 业务来源分类 | 适用 | 本轮无新冲突/添加。001 细化、002 细化（遗漏）、003 技术性（闭集取自 V184 自身注释、无业务发明）、004 观察。V425 四值是 HTML「存档/删除审批」在 **M018** 的细化，不在本模块开添加项。 |
| GEN-018 第一性原理证据 | 适用 | 本报告结论落到 HTML 原文、mvn/XML、Flyway/psql 命令输出；不把旧 B5 票、席甲补充复查或「看起来对」当依据。 |

---

## 5. 对抗式 grilling（自问自答；不等操作员）

追问树本轮全部走到证据。铁律 6：第一次正常跑会撞上才修；两件不巧才成立的不立项。

1. **闭集 shape_type 含 booth/public_area、拒 corridor？** 活库两表 CHECK 八值含 booth/public_area、不含 corridor（`pg_get_constraintdef`）。`importCandidateShapeTypeFollowsDatabaseCheck` XML 绿。服务 `SHAPE_TYPES` 无 corridor。正常路径：API corridor → 服务 VALIDATION_FAILED；直写 SQL corridor → CHECK 拒。**不升缺陷。**
2. **CHECK 是否 NOT VALID 被误当成已 VALIDATE？** `chk_mall_floor_plans_project_id_required` def 含 `NOT VALID`，`convalidated=f`。存量 NULL 会被放行；当前 `mall_floor_plans` 0 行。`SET NOT NULL` 被 B2 禁止。**001 仍按定稿成立。**
3. **空串 `project_id=''` 能过 IS NOT NULL？** CHECK 语义上空串非 NULL。服务 `Strings.requireText` 把空白当缺失（L15–32）。正常 API 路径第一次就会在服务层失败。直写 JDBC 绕过服务才可能落 `''`。两件不巧，**不立项**（观察 G-OBS-01）。
4. **软引用：project_id / ref_id / space_type 无 FK？** `pg_constraint` 无 `mall_floor_plans.project_id`→`projects`，无 `spaces.space_type`→字典，无 `floor_plan_shapes.ref_id`→对象。与 V005 `spaces.project_id` 可空软引用同范式。B2 明确不加 FK。**不升。**
5. **脏值归一是否打错目标？** V203：非 active/unavailable → active（拍板防漏出租率分母）。V423：字典非 active/disabled → disabled（当时两值；脏值本就不在下拉）。V426：disabled → archived（M018，历史停用改存档）。活库八行全 active，无脏值。**不升。**
6. **幂等重跑 V423？** 源文 `DROP IF EXISTS` 可重跑。但活库若已是四值，手工重跑 V423 的 UPDATE 会把 archived/pending_delete 打回 disabled。Flyway checksum 已钉死 V423，不会自动重跑。**不把「禁止手工重跑已应用迁移」写成新需求。**
7. **权限种子越权？** 活库：`role_platform_admin` 四码；`role_auditor_internal` 仅 view 两码；manage 两码 `requires_approval=t` / `risk_level=high`。英文种子已被 V251 覆盖为中文。**不升。**
8. **字典 `Active` 大小写？** `spaceTypeDictionaryRejectsUnknownStatus` 仍插 `'Active'` 并期望完整性异常；四值 CHECK 同样不含 `Active`。XML 该类 23/0/0/0。**003 的红例仍有效。**
9. **V425 四值是否把 003 修回去？** 否。003 要的是「有闭集 CHECK、拒未知值」。闭集从两值扩到四值是 M018 按 BSM 存档/删除审批做的授权变更，测试仍拒 `Active`。**登记，不立项、不重复 003。**
10. **CASCADE 物理删平面图会删形状/导入？** FK `ON DELETE CASCADE` 仍在。应用层软删 archived。0 处业务 `DELETE FROM mall_floor_plans` 不在本 FILES 审计为新缺陷（004 已登记）。**不升。**

---

## 6. 已定稿 ID 复验（不重复立项）

| ID | 分类（定稿） | 队列（操作员/ledger 叙事） | 本轮复验 |
|---|---|---|---|
| M016-001 | 细化 | 否（已修） | V423 L13–15 仍在源；活库约束存在、NOT VALID、`convalidated=f`、列可空；`floorPlanWithoutProjectIsRejectedByDatabase` 绿。**仍成立。** |
| M016-002 | 细化（遗漏） | 否（已修） | V423 L4–9 八值；活库两表 CHECK 八值含 booth/public_area；服务白名单八值；Jdbc + Service 用例绿。**仍成立。** |
| M016-003 | 技术性 | 否（已修） | V423 L19–22 两值 CHECK 源文未改；活库同名约束为四值（V425 替换）；`Active` 仍拒。**闭集机制仍在；取值集合的后续扩展属 M018。** |
| M016-004 | 观察 | 否 | corridor/geojson、CASCADE、cancelled 无写路径、候选无 UNIQUE(task,no)、软引用、权限英文种子被 V251 覆盖——本轮均仍在，不升格。 |

findings.md 表行 001–003 的「修复队列」列仍为「是」，与 B4 后叙事不一致。该文件在排除目录，**不作为本模块 FAIL 依据**，也不在本席改。

---

## 7. 后续迁移登记（不立项）

| 文件 | 行/字节/SHA256 | 归属 | 对本模块 |
|---|---|---|---|
| `V424__space_type_delete_requests.sql` | 8 / 239 / `9625314129ba6b8ad47601e034a0a82c42240ff9ef1a1c604d427092a7af0295` | M018（PR #439，本地 HEAD 有、`origin/main` 无此路径） | 删除申请表。只登记 |
| `V425__space_type_status_archive_pending.sql` | 6 / 569 / `81803091a24a655924293ac0bc1362693cb456441eb7d4ec5752a616bf3eff1c` | M018 | 把 `chk_space_type_dictionary_status` 扩为 active/disabled/archived/pending_delete。活库即此闭集。只登记 |
| `V426__space_type_legacy_disabled_to_archived.sql` | 2 / 182 / `2686332155001257a8992b818bcd9fb075e6a25040ea92ffa2cee286a00c1627` | M018 | `disabled`→`archived`。只登记 |
| `V427__m009_project_profiles_and_associations.sql` | 20 / 705 / `cc8c391bda2bdb2658d5640ceff6036736bfe9d2c3b864de8f13e8474f9a3b13` | M009（暂存/未在 HEAD） | Flyway 已应用到 complex_test（version=427）。只登记 |

Flyway history（`psql` 类型 4）：023/034/184/203/423/424/425/426/427 均 `success=t`。latest=427，max_rank=395。V424–426 checksum 亦与文件行 CRC 全等。

不把「全部否」当成功能全实现：用途/负责人列仍属 M015-012 全局遗漏；二次确认发布在服务/前端；字典存档删除审批在 M018。

---

## 8. 可执行证据（命令 / exit / 输出）

凭据：从 `/etc/complex/complex.env` **逐行读取**三个 `COMPLEX_DATABASE_*`，URL 库名由 `complex` 改为 `complex_test`。未 source 该 env。未连生产库 `complex`。psql 会话 `current_database=complex_test`，`current_user=complex`。密码未入报告。

### 8.1 只读 psql（exit 0；首轮 ORDER BY 别名失败 exit 3 后去掉别名重跑 exit 0）

```
psql -h 127.0.0.1 -U complex -d complex_test -v ON_ERROR_STOP=1
```

关键输出（已记录）：

- `current_database=complex_test`
- flyway：V023 checksum 1247380439 rank 23；V034 1294355959 r34；V184 -1668892912 r181；V203 83253196 r200；V423 1707295493 r391；V424 1769526014 r392；V425 -449210332 r393；V426 342080550 r394；V427 216061286 r395；latest=427
- `chk_mall_floor_plans_project_id_required` = `CHECK ((project_id IS NOT NULL)) NOT VALID`，convalidated=f
- 两表 shape_type CHECK 八值含 booth/public_area
- `chk_space_type_dictionary_status` 四值 active/disabled/archived/pending_delete，convalidated=t
- `chk_spaces_status` active/unavailable
- `mall_floor_plans.project_id` is_nullable=YES；0 行；null_pid=0 empty_pid=0
- 字典 8 行全 active
- `spaces` 0 行
- 权限四码中文名；admin 四码、auditor 两 view 码

本席 **未** 对 complex_test 做 INSERT/UPDATE/DDL（派发禁止）。CHECK 拒绝证据改用已有 Jdbc 测试（类型 2）+ 约束定义（类型 4）。

### 8.2 Maven（exit 0）

```
export COMPLEX_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/complex_test
export COMPLEX_DATABASE_USERNAME=complex
export COMPLEX_DATABASE_PASSWORD=<REDACTED>
# SPRING_PROFILES_ACTIVE 未导出
mvn -o -q --file backend/pom.xml \
  -Dtest=PlatformJdbcIntegrationTests,FloorPlanJdbcIntegrationTests,FloorPlanServiceTests \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- `MVN_EXIT=0`
- 日志：`Successfully validated 395 migrations`；`Current version of schema "public": 427`；`Schema "public" is up to date. No migration necessary.`
- `TEST-*.xml`（mtime 2026-09-19 23:08:35 PDT，本席本次运行写入）：

| 类 | tests | failures | errors | skipped | @Nested |
|---|---:|---:|---:|---:|---|
| FloorPlanJdbcIntegrationTests | 7 | 0 | 0 | 0 | 无（classname 单一） |
| FloorPlanServiceTests | 39 | 0 | 0 | 0 | 无 |
| PlatformJdbcIntegrationTests | 23 | 0 | 0 | 0 | 无 |
| **合计** | **69** | **0** | **0** | **0** | |

未运行、因而不声称通过：全后端测试、Controller/Parser/VenueHours、部署、手工 `flyway:migrate`、`npm run verify:migration-version-order`、任何对 `complex_test*` 的手工写。

本席未改 git 主索引、未 commit/push/PR/merge/deploy。

---

## 9. 本轮问题清单

按冲突→细化→技术性→添加审完全部文件后统一判定。

**未定稿工程缺陷：0。添加项：0。HTML 人闸：0。锁外判据：0。**

观察（不立项、不重复 004 ID）：

| 临时编号 | 位置 | 原句/事实 | 正常触发？ | 四分类 | 已有判据 | 证据类型 | 处置 |
|---|---|---|---|---|---|---|---|
| G-OBS-01 | 活库 `chk_mall_floor_plans_project_id_required` | `CHECK ((project_id IS NOT NULL)) NOT VALID` 放行 `''` | 否（API `requireText` 先拒空白） | 细化 | GEN-006；001 定稿即 NOT VALID 非空，不加 SET NOT NULL | 4+源码 | 不修 |
| G-OBS-02 | V184 L8 注释「active/disabled」；V423 L17–22 两值 | 活约束已是四值 | 否（后续 V425 授权） | 技术性/文档陈旧 | GEN-016/GEN-014；已应用迁移不得改写 | 1 文件原文 + 4 活库 | 登记 XM→M018 |
| G-OBS-03 | 前端 corridor / geojson（非本 FILES） | 与 V023/V034/V423 CHECK 不一致 | 服务层拒绝，进不了库 | — | 004 / M018 | 只读追流 | 维持 004 |
| G-OBS-04 | V424–V426 / V427 | 见 §7 | — | — | 操作员已裁定只登记 | 4 Flyway | 不立项 |

不为「直写 SQL 空串」「手工重跑 V423」等理论路径加需求。

---

## 10. 审计结论

**PASS**

理由：整模块 4 冻结迁移 + V423 全文三线已审完；18 条 GEN 均有适用/证据记录；001–003 独立复验仍成立；003 活闭集被 M018 V425 扩展为四值已登记不立项；004 维持观察。无新增未定稿缺陷、无添加项、无未答 2.2、无 HTML 人闸、无锁外判据。不存在有条件通过。B5 禁止修复，本席未修。本票不等于双席 B5，也不等于模块 PASS。
