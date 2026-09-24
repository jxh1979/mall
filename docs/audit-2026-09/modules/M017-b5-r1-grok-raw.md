CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M017 B5-r1 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.01（指定路径原先不存在，本次新建完整报告）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员、不读本轮 self-raw）
- 步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B4 双 PASS、B3 自报红→绿或 B1 历史票替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M017 同一 5 文件（FloorPlanControllerTests、FloorPlanImportParserTests、FloorPlanJdbcIntegrationTests、FloorPlanServiceTests、VenueBusinessHoursServiceTests）；计划冻结有效内容行 1352；当前工作区 `\n` 计数 **2057**（含 M015/M016/M017 B3 追加，不改计划凑数）
- 模块：M017 — 空间点位、楼层平面图与营业时段／后台验证／第1段
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值（889020 字节 / 6850 行）；sidecar 全文一行同值。独占锁行仅本文第一行，正文不再出现同名独占字段。
- 映射：`M017 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-170, BEQR-189, BSM-001, BSM-003, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-022, PGM-023, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-060, PGM-065, PGM-066]`（`criteria-lock.md` L6396）COUNT=90 unique=90 MISSING=[] EXTRA=[]（BEQR 22 / BSM 14 / GEN 18 / PGM 36）；与本席 B1 所列逐字相同；定义与 HTML 出处均从锁自读，不新造判据、不 LOCK-AMEND
- 正本检出：`HEAD=origin/main=e2abb037001ccd72650efa3cd86e9b1b4bfe3ea0`（PR #437 已合并）。B3-r1 after tree 对象 `03981e7c1bcfe2a5db58169d37475aede7c85c94` 可解析为 tree；本模块 5 文件当前工作区 blob 与该 after **MISMATCH_COUNT=0**。HEAD blob 仍为修复前（B3 未提交）
- 本席约束：只读业务代码与计划；除本 raw 外不写文件；不修源/三 HTML/锁/findings/ledger；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；不对任一 `complex_test*` 库做手工写操作。变异复现在自建 `/tmp/m017-b5-mut`（结束已按文件删除，主仓 cwd 未写模式）
- 未读取：`M017-b5-r1-self-raw`（当前亦不存在）。B4-r1 双席 raw 与 findings V1.100 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界，不代替本轮全量重审）：findings V1.100 — 本批定稿 ID 已修（M017-001…008 已修并经 B4-r1 双 PASS；009/010 否（观察））。B2 定稿禁止项（不得在 B5 升格为缺陷）：改任何实现/迁移/OpenAPI/三份 HTML/锁；新增测试文件；把 corridor 进白名单、加分页参数、加二次确认字段；把 requireText 英文键（FieldLabels 设计口径，出网经 translate）当缺陷；把导入上限 2000/1000/100 万字符（铁律 6 收窄）当必补用例。本模块不得新增 HTML 修改申请以外的锁外判据

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问 2.2、无锁外判据、无 HTML 修改申请。001–008 在当前 5 文件上独立复验成立（主仓 5 类 TEST-*.xml **83/83** 全绿；`/tmp` 副本 Temurin 21 变异：001/002/003/005/006/007/008 对应新例击红）；009/010 维持队列「否」，本席不升格。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权——本席不行使否决。

---

## 0. 技能自核（V0.17 §6，按 B5 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体。本步禁止子代理，与技能「先派子代理」冲突处按用户指令收窄 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证（10 码闭集、发布不覆盖、诚实分页、8 端点 403、corridor/CHECK、中文整句、真实 SQL、审计事件、跨图任务归属），不向操作员发问、不派子代理 |
| grill-me | 同套件 `productivity/grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（锁哈希 / `mvn -Dtest` / surefire XML / `@Transactional` 残留 SELECT / `/tmp` 变异）再下结论；B5 不修复 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未运行测试不写通过 |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流（HTML 原句→5 测试类→FloorPlanService/Controller/Parser/JdbcFloorPlanRepository/VenueBusinessHoursService/Repository→V023/V034/V423/GlobalExceptionHandler/FieldLabels/IdempotencyService）；B5 不修复 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地：B3 追加的是定稿最小用例面。Lean already. Ship. |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 主仓 `mvn` 约 8s 前台完成，未起 monitor |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| domain.md | `docs/agents/domain.md` | 词表/三 HTML 最终解释权；未改 CONTEXT.md |
| codebase-design | mattpocock `engineering/codebase-design/SKILL.md` | 删除这 5 类则 PGM-023 导入/发布、BEQR-152 项目隔离、V423 列级约束在验证面失去可红反馈。不派 DESIGN-IT-TWICE |

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
| findings | V1.100；SHA256 `77944df923b9496180c723b169d3db6a385d17dae77600702ac128fdf5f1bfcc`；798652 字节 |
| ledger | SHA256 `4ecb22d6c8904ac6a2c5d489b59a6c2e1a3da989007a85d9ed7e8404a28bbfd2`；614162 字节；M017 `IN_PROGRESS`，B3=1，B5-r1 进行中 |
| 映射行抽出 | 90 条、无重复、MISSING=[] EXTRA=[]；BEQR 22 / BSM 14 / GEN 18 / PGM 36 |
| 锁内映射 HTML 来源行 vs 现行三 HTML | 72/72 hit=True miss=0（GEN 18 条无 HTML 行） |
| 计划 FILES | 5 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 1352；当前工作区 `\n` 计数 **2057**（M015/M016/M017 B3 追加，不改计划凑数） |
| questions | 无 `questions/M017*`；无未答问询 |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| modules.md / runbook.md | SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` / `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| HEAD / origin/main | `e2abb037001ccd72650efa3cd86e9b1b4bfe3ea0` ×2 |
| B3-r1 before / after tree | `5dedaf48e6d9fdccdb3bf8ca8959b4caa8b1cfed` / `03981e7c1bcfe2a5db58169d37475aede7c85c94`（`git cat-file -t` 均为 tree） |

三 HTML 工作区 SHA（本席独立，未改；均 `same_head=Y`）：

| 文件 | 现行 | 行 | 字节 | SHA256 |
|---|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | V0.07 | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | V0.14 | 2032 | 304482 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | V0.12 | 979 | 103015 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。

说明：对工作区 `git add -A` 于临时索引得到的全树 ≠ B3 冻结 after `03981e7c…`，原因是 B3 之后写入了 findings/ledger 的 B4 完成节以及本模块 B1/B4 双席 raw（未跟踪）。**修复面 5 文件工作区 blob 与 `03981e7c…` 全等（MISMATCH_COUNT=0）**。本席不以全工作区树差否定 B3 after。

---

## 2. 5 文件当前行数/字节/SHA 与全量三线覆盖表

行=`b'\n'` 计数。全部 `endswith \n`。无 OVERSIZE。全文已读，未只读标题/搜索/截断。

| # | 路径 | 版本头 | 行 | 字节 | SHA256 | git blob | 覆盖 |
|---|---|---|---:|---:|---|---|---|
| 1 | `backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java` | V0.01 | 472 | 21466 | `acd1f485522c9c31c3ec37c1194226fb5259db54e40a970e3fb2b0ea2aaa7aac` | `574cfff99191b38689c0ae372d51c36678b32525` =after | 已读 L1–L472；第一性/顺从/对抗；CMD-T1 14/14 |
| 2 | `backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java` | V0.01 | 193 | 7611 | `a6e0f52b13a11a0d157df098a569b0e2c62a0044eaebbb3cfa5fe8382e1b758c` | `fcad00ed3e4a0931e863c3a0508bd6804c89682c` =after | 已读 L1–L193；第一性/顺从/对抗；CMD-T1 14/14 |
| 3 | `backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java` | V0.03 | 277 | 11748 | `5a5932ce4bf3dc12d3b1a652cf107144bd33663cef462f6c0d87f405bdedaa59` | `30b3866d5a0158ac2f91ebebdf4898d817b7d8a8` =after | 已读 L1–L277；第一性/顺从/对抗；CMD-T1 7/7 |
| 4 | `backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java` | V0.04 | 864 | 41404 | `9fa9ecad75631864ddae02584f533614c8c902c3914a3ca92cc79875507f5a09` | `9f3fb0ab42d5eb82824e6d8d04b5a90f36fb7e8f` =after | 已读 L1–L864；第一性/顺从/对抗；CMD-T1 36/36 |
| 5 | `backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java` | V0.02 | 251 | 11957 | `d8b3df0e5d5ad082e3589e81e489a1269ac3ef9432186d4104d3a385979dfba9` | `302a5cdc1730c61f12c3c2ad644e0411c406610a` =after | 已读 L1–L251；第一性/顺从/对抗；CMD-T1 12/12（含 `@Nested` 1 例） |

5 文件合计 **2057** 行 / **94186** 字节。`@Test` 计数：14+14+7+36+12=83，与 surefire XML `testcase` 全等。

只读依赖（不审这些文件本身，但必须追到事实；本批零改动）：`FloorPlanService.java` V0.03（`SHAPE_TYPES` 八值、`MAX_PUBLISH_CANDIDATES=200`、诚实 `PageResponse`、中文整句、`projectExists`）；`FloorPlanController.java` 8 端点 `requirePermission` + 4 写端点 `requireIdempotencyKey`；`FloorPlanImportParser.java` 10 个 error_code 与中文 `errorMessage`；`JdbcFloorPlanRepository.java` `SELECT COUNT(1) FROM projects`；`VenueBusinessHoursService.java` V0.01 审计事件名 / 星期 0–6 / 跨零点放行；`VenueBusinessHoursRepository.java` `COALESCE(project_id,'')` + `listByProject` JOIN spaces；`GlobalExceptionHandler` L61 `FieldLabels.translate`；`FieldLabels.java:14-18` 抛点英文键、出网翻译；`IdempotencyService` `transactionTemplate.execute`；`InMemoryAuditEventRepository` / `InMemoryIdempotencyRepository` / `MigrationVersions.latest()`；V023/V034/V423（complex_test Flyway 当前版本 **423**，validated 391）。

---

## 3. 第一性原理（逐文件）

### 3.1 为什么必须存在（删掉哪条 HTML 做不到）

| 文件 | 删掉之后 |
|---|---|
| ParserTests | PGM-023「解析出候选区域…人工复核候选与错误」失去 error_code 闭集与中文 errorMessage 的可红反馈 |
| ServiceTests | PGM-023 发布不覆盖、200 上限、失败任务、候选归属、诚实分页、项目/商户隔离、booth/public_area 与 corridor 负例全部失去服务层锁 |
| ControllerTests | GEN-005/PGM-040 八端点权限码与缺幂等键出网 403/400 失去 HTTP 层锁 |
| JdbcIntegrationTests | GEN-014/V423 表存在、权限种子、project_id NOT VALID、candidates CHECK、真实 `projectExists` SQL 失去库层锁 |
| VenueHoursTests | BEQR-152 场地存在/项目隔离、GEN-011 营业时段审计、跨零点/零长窗口 失去唯一配置入口的验证面 |

答得上，不删除。

### 3.2 声称的行为是否实际执行

CMD-T1（见 §8）：`mvn -o -q -Dtest=FloorPlanControllerTests,FloorPlanImportParserTests,FloorPlanJdbcIntegrationTests,FloorPlanServiceTests,VenueBusinessHoursServiceTests -Dsurefire.failIfNoSpecifiedTests=false test`，只导出三个 `COMPLEX_DATABASE_*`（URL 改写为 `complex_test`，未 `source` 整个 env）。**EXIT=0**。surefire XML `testcase`：**14+14+7+36+12=83**，failures=0 errors=0 skipped=0。

`@Nested`：`VenueBusinessHoursServiceTests$RealRepositoryCases#findVenueProjectIdAndListByProjectHitRealSpacesTable` 出现在父 suite XML，time=0.015（非 0）。surefire 文本报告对 `@Nested` 计 0 属报告器怪癖，本席以 XML 为准。

Flyway 日志：`Successfully validated 391 migrations`；`Current version of schema "public": 423`；`No migration necessary`。库名 `jdbc:postgresql://127.0.0.1:5432/complex_test`。

### 3.3 哪个前提未经验证 → 本轮已验证

- 嵌套真实仓库是否真跑且回滚：XML 有例；CMD-ROLLBACK `projects.id='prj_jdbc_real'` / `spaces.id IN ('sp_vbh_a','sp_vbh_b')` / 夹具平面图 / `vbh_real_%` 计数均为 **0**。`@Transactional` 回滚成立。
- mock 是否掩盖 SQL：`projectExistsQueriesRealProjectsTable` 走真实 `projects` 表；嵌套例走真实 `spaces` / `venue_business_hours`。内存仓 `Set.of("prj_1","prj_2")` 与 `RecordingRepository` 仍服务单元路径，但 007 的 SQL 面已另锁。
- 已定稿 001–008 是否仍能失败：`/tmp/m017-b5-mut` + Temurin 21 变异，见 §5。

---

## 4. 顺从式（90 冻结判据逐项）

先假定实现意图正确，再逐 ID 核对本模块映射。无实现面的判据写明原因，不升冲突。

### 4.1 GEN（18）

| ID | 适用 | 本席（B5 现态） |
|---|---|---|
| GEN-001 | 是 | 5 类均有可验证用途（§3.1）；B3 追加是定稿最小面，非抽象膨胀 |
| GEN-002 | 是 | CMD-T1 83/83 EXIT=0；声明的主路径/错误路径与断言一致 |
| GEN-003 | **是（核心）** | 001–008 均有能失败断言（§5 变异击红）；mock 不再掩盖 007 SQL；`@Nested` 真跑且回滚。剩余未锁分支见观察，不升缺陷 |
| GEN-004 | 是 | 输入校验（payload/格式/多边形/项目存在/场地存在）有负例 |
| GEN-005 | 是 | 8 端点 403 循环 + 成功路径 verify 权限码；Controller 仍是 WebMvcTest+mock（OBS-10 口径，不升） |
| GEN-006 | 是 | 发布失败不落图形；Jdbc `@Transactional` 回滚；库层 CHECK 拒非法 type |
| GEN-007 | 是 | 同键幂等/异载荷冲突/跨图同键/跨商户同键已有例；无线程并发（OBS，不升） |
| GEN-008 | 是 | 诚实分页三处+Controller 桩 `(…,1,1,1)` 并断言 `page_size`；白名单与 V423 CHECK 同八值 |
| GEN-009 | 是 | 失败码闭集（解析 10 码、VALIDATION_FAILED/NOT_FOUND/FORBIDDEN）有例；不吞错 |
| GEN-010 | 背景 | 本 5 文件无加密/注入面；Jdbc 密码 fallback 为测试源码（010 观察） |
| GEN-011 | 是 | FloorPlan 写路径 eventName + 导入 viewed；VenueHours `updated`/`viewed` 真实 AuditEventService |
| GEN-012 | 是 | 发布 200 上限 + 100 万字符上限有例；2000/1000 按 B2 禁止升格 |
| GEN-013 | 背景 | 本模块不改构建；CMD-T1 `-o` 离线复现 |
| GEN-014 | 背景 | 本 5 文件无迁移；Jdbc 断言 latest() 与 V423 正向（脆弱性仍观察） |
| GEN-015 | 是 | 已改中文整句有 getMessage/errorMessage 断言；夹具英文按测试豁免；requireText 英文键按 009 不升 |
| GEN-016 | 是 | 本 raw 命令/exit/输出可复现；83 PASS 不写成「模块已 PASS」 |
| GEN-017 | 是 | 无添加项；HTML 沉默不删；观察不升冲突 |
| GEN-018 | 是 | 结论落到 HTML 原句、测试源码、mvn/XML、psql SELECT、`/tmp` 变异 |

### 4.2 BEQR（22）

| ID | 适用 | 本席 |
|---|---|---|
| BEQR-063 | 无实现面 | 原文高危列表是反结/沽清/免单等，不含平面图发布；发布高危以 PGM-023 为准 |
| BEQR-074 | 无实现面 | 隐私桌台权限；本 5 文件无桌台 |
| BEQR-143–148 | 无实现面 | 四类主体/收入/合作模式；测试不锁主体绑定 |
| BEQR-149 | 无实现面 | 职级 M1–M10 |
| BEQR-150 | 无实现面 | 终端十一端；后台测试不是端清单 |
| BEQR-151 | 部分 | 最终权限公式在 AuthorizationService；本模块 mock 后只锁 permission 码字符串（观察：不测公式） |
| BEQR-152 | **是** | 项目隔离：Service 三例 + VenueHours 三例 + 嵌套真实 `listByProject`；Controller 无跨项目 HTTP 例（WebMvcTest 旁路，不升） |
| BEQR-153 | 无实现面 | 终端开关优先级 |
| BEQR-154–159 | 无实现面 | 项目/商户/合同/员工/设备/权限状态闭集；平面图 status 是 active/inactive/archived，不得发明成这些闭集 |
| BEQR-166 | 无实现面 | 合同审批链 |
| BEQR-170 | 无实现面 | 铺位租赁六态在合同仓（M015-014）；本测试不锁、也不把 shape/status 发明成租赁看板 |
| BEQR-189 | 无实现面 | 管理费方式 |

### 4.3 BSM（14）

| ID | 适用 | 本席 |
|---|---|---|
| BSM-001 | 是 | 「先管清楚基础盘子」含空间；测试覆盖楼层平面图与营业时段主路径 |
| BSM-003 | 部分 | 权限分岗：Controller verify + 403；可改可删/引导不在本 5 文件 |
| BSM-004 | 无实现面 | 配置删除审批；本测试无删除/存档平面图 API（沉默不升冲突） |
| BSM-005 | 无实现面 | 分步引导是前端（M018） |
| BSM-006 | 无实现面 | 动态二维码 |
| BSM-007 | 是 | Jdbc 断言 `role_platform_admin` 含四条 `floor_plan.*` |
| BSM-009 | 是 | 基础管理含空间 |
| BSM-028 | 是 | 「建立综合体项目、楼层、区域、点位」；project_id 必填/存在 + 真实 SQL |
| BSM-029 | 背景 | 票务/酒吧/游乐要知道空间；本测试不连那些子系统 |
| BSM-072 | 是 | 商户不能看其他商户；`listFloorPlansFiltersToMerchantScope` / `CROSS_MERCHANT_FORBIDDEN` |
| BSM-074 | 背景 | 商户章「和谁有关」 |
| BSM-114 | 无实现面 | 入口表；本文件是 API 测试不是入口页 |
| BSM-115 | 背景 | 子系统必须连在一起 |
| BSM-120 | 无本文件专有断言 | 已确认规则总表；空间相关已由 PGM-022/023 覆盖 |

### 4.4 PGM（36）

| ID | 适用 | 本席 |
|---|---|---|
| PGM-001 | 是 | 先管归属：project_id 必填/存在有例；中文整句已锁代表句 |
| PGM-002 | 是 | 先对象再人员再留痕：权限 verify + FloorPlan/VenueHours 审计 |
| PGM-003 | 背景 | 模块入口卡片栅格 |
| PGM-004 | 背景 | 综合体和集团卡片 |
| PGM-005 | 是 | 「空间和点位」卡片原文；模块存在性 |
| PGM-006–013 | 无实现面 | 商户/合同/员工/权限/终端/设备/商户侧/开放接口卡片 |
| PGM-014 | 是 | 留痕复核；FloorPlan 写路径 + VenueHours updated/viewed |
| PGM-015–016 | 无实现面 | 楼宇运营/应急隐私 |
| PGM-017 | 是 | 对象带综合体项目归属；create 必填+存在+真实 SQL |
| PGM-018–021 | 背景 | 多对多/主体职责/项目档案/集团意义 |
| PGM-022 | 是 | 空间不仅是楼层图 |
| PGM-023 | **是** | 候选铺位/摊位/公共区（booth/public_area 正、corridor 负、candidates CHECK）；人工复核错误（10 码+failed 任务）；不可静默覆盖（先存图形再发布并存）；发布高危二次确认属 M015-011 XM，B2 禁止本模块加字段 |
| PGM-026 | 背景 | 商户档案功能 |
| PGM-027 | 是 | 商户不能看其他商户（同 BSM-072） |
| PGM-031 | 无实现面 | 经销商治理 |
| PGM-035 | 无实现面 | 提成发放 |
| PGM-040 | 是 | 单一功能单独授权；8 端点 403 + 成功 verify |
| PGM-041 | 是 | 默认按项目隔离、无关闭开关（同 BEQR-152） |
| PGM-042–044 | 无实现面 | 职级/外包审计/桌台挂单经销 |
| PGM-048 | 是 | 商户侧不跨商户（同 BSM-072） |
| PGM-060 | 无实现面 | 会员生物信息 |
| PGM-065 | 背景 | 连接关系表 |
| PGM-066 | 是 | 已拍板：所有对象带综合体项目归属（同 PGM-017） |

顺从式结论：90 项均已核对。适用项在 B3 后被 001–008 新例锁住。无实现面项不升冲突。无锁外判据、无添加项。

---

## 5. 对抗式（grilling 自答 + `/tmp` 变异）

grilling 追问树（自答，不派子代理、不问操作员）：

| 变异 | 期望红的测试 | 本席实跑（`/tmp/m017-b5-mut`，JAVA_HOME=Temurin 21） |
|---|---|---|
| Parser `POINTS_TOO_FEW` 中文改英文 | `reportsPointsTooFewWithChineseMessage` | **击红**（M1 EXIT=1；actual `"polygon needs at least 3 points"`）→ 001/006 仍能失败 |
| `listFloorPlans`/`listShapes`/`listImportTasks` pageSize 改回 50 | 两处诚实例 | **击红**（M2；expected 2 but was 50）→ 003 仍能失败 |
| `SHAPE_TYPES` 加入 corridor | `saveShapeRejectsShapeTypeOutsideClosedSet` | **击红**（M3；Expecting code to raise a throwable）→ 005 仍能失败 |
| 删 `candidateIds.size() > 200` | `publishRejectsMoreThanTwoHundredCandidatesAndEmptyList` | **击红**（M6；expected VALIDATION_FAILED but was IMPORT_CANDIDATE_NOT_FOUND）→ 002 仍能失败 |
| 改 VenueHours 审计事件名 | `saveAndListWriteAuditEvents` | **击红**（M7；actual `venue_hours.saved/listed`）→ 008 仍能失败 |
| `projectExists` 恒 true | `projectExistsQueriesRealProjectsTable` | **击红**（M8；Expecting false but was true）→ 007 仍能失败 |
| 删 `task.floorPlanId().equals(floorPlanId)` | 无对应例 | **FloorPlanServiceTests EXIT=0**（M4；36 例仍绿）→ 观察 GR-M017-B5-OBS-01，不升缺陷 |
| 把「图形不属于该平面图」改英文 | `saveShapeRejectsShapeIdBelongingToDifferentFloorPlan` 只锁 code | 代码审：该例不断言 getMessage → 观察（006 斜杠组已由「候选项不属于」代表），不升 |
| 2000 节点 / 1000 顶点 | B2 禁止当必补 | 不升 |
| 二次确认字段 | M015-011 XM | 不升 |
| requireText 英文键 | M017-009 / FieldLabels 设计口径 | 不升 |

首次 `/tmp` 副本未设 Temurin `JAVA_HOME` 时，`java-21-openjdk-amd64` **无 javac**，maven-compiler 报 `release version 21 not supported`（EXIT=1，0 条 testcase 失败）。这是副本 JDK 配置问题，**不折算为业务 finding**。改 `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64` 后变异表成立。主仓 CMD-T1 使用同一 Temurin（Spring 日志 `Java 21.0.12`）已 83/83。

Controller 缺幂等键：主仓 CMD-T1 日志出现 `raw_message=idempotency_key is required`（create/publish/saveShape/createImport 四次），与 004 新例一致。8 端点 403 循环日志 8 次 `PERMISSION_DENIED`。

`@Transactional` 回滚：CMD-ROLLBACK 夹具 id 空集。不依赖 `complex_test` 预置业务行。

---

## 6. 已定稿 ID 001–010 独立复验

| ID | 队列 | B5 复验 |
|---|---|---|
| M017-001 | 已修 | Parser 10 码各 ≥1 处（PAYLOAD_REQUIRED/UNSUPPORTED_SOURCE_FORMAT/VIEWBOX_REQUIRED/POINTS_REQUIRED/POINTS_TOO_FEW/NO_SHAPES_FOUND/JSON_PARSE_FAILED/SHAPES_REQUIRED/POLYLINE_NOT_CLOSED/POINT_OUT_OF_RANGE）；SVG 越界；默认 label/shape_type/ref_type；POINTS_TOO_FEW 中文整句。M1 击红。**仍成立** |
| M017-002 | 已修 | ① 先存图形再发布并存且原样；② 全错 SVG → failed/errorCount=1/错误落库/发布被拒；③ 他任务候选中文「候选项不属于该导入任务」+ 不存在 → IMPORT_CANDIDATE_NOT_FOUND、不落图形；④ 201 →「一次发布的候选项不能超过 200 个」+ 空列表「候选项列表不能为空」；⑤ list+details 写 `import_viewed`/`import_candidate_viewed`。M6 击红。**仍成立** |
| M017-003 | 已修 | `listFloorPlans`/`listImportTasks` pageSize==total==items.size()；Controller 三处桩 `(1,1,1)` 并断言 `$.data.page_size`。M2 击红。**仍成立** |
| M017-004 | 已修 | `everyEndpointReturnsForbiddenWhenPermissionDenied` 8 端点；`saveShapeAndCreateImportWithoutIdempotencyKeyReturnValidationFailed`；CMD-T1 日志 8×PERMISSION_DENIED + 4×idempotency_key is required。**仍成立** |
| M017-005 | 已修 | `saveShapeRejectsShapeTypeOutsideClosedSet` corridor → VALIDATION_FAILED「图形类型取值无效」；Jdbc `importCandidateShapeTypeFollowsDatabaseCheck` booth/public_area 接受、corridor CHECK 拒。M3 击红。corridor 未进白名单。**仍成立** |
| M017-006 | 已修 | 闭集代表句已断言：候选项列表不能为空、多边形至少需要 3 个顶点、导入内容超过 100 万字符上限、一次发布…200、候选项不属于、场地不能为空、结束营业时间必须填写。斜杠组「图形/导入任务不属于」「开始营业时间」无独立 getMessage，见观察，不重开 006。M1 击红。**仍成立** |
| M017-007 | 已修 | Jdbc `projectExistsQueriesRealProjectsTable` 插入最小 projects 行后真/未知假；VenueHours `@Nested` 真实仓库插入最小 spaces 行（status=active, space_type=shop）后命中/empty/`listByProject` 只返本项目。XML 含该例；回滚空集。M8 击红。**仍成立** |
| M017-008 | 已修 | 真实 `AuditEventService(InMemoryAuditEventRepository)`；save/list 事件名；五处弱断言改为 `code==VALIDATION_FAILED`；admin 全量 + effectiveFrom 透传。M7 击红。**仍成立** |
| M017-009 | 否（观察） | FieldLabels 设计口径，出网经 translate。CMD-T1 debug `raw_message=idempotency_key is required` 仅日志。**不升格** |
| M017-010 | 否（观察） | 见 §7 观察清单。**不升格** |

---

## 7. 本次 findings / 观察

工程缺陷（未定稿、建议入队「是」）：**无**。

添加项：无。questions：无。HTML 修改申请：无。锁外判据：无。

### 观察（临时编号 GR-M017-B5-OBS-xx；不入修复队列）

1. **GR-M017-B5-OBS-01**（同根 002 所有权家族，不在 002 闭集五例内）`ensureImportTaskBelongsToFloorPlan` 的「导入任务不属于该平面图」无用例。`/tmp` M4 删该 if 后 `FloorPlanServiceTests` EXIT=0。首轮操作员打错平面图 URL 才走到；002 已锁候选归属/不存在。不升。
2. **GR-M017-B5-OBS-02**（同根 006 斜杠组）「图形不属于该平面图」「开始营业时间必须填写，格式 HH:mm」无 getMessage；「导入任务不属于」无路径。006 已锁同组代表句。不升。
3. **GR-M017-B5-OBS-03**（同根 005/B1 OBS-17）导入路径 `createImportTaskOnce` 对 candidate.shapeType 的 `normalizeAllowed` 无独立 corridor 例；saveShape 负例已锁白名单。B2 写明「负例已锁」「无导入 booth SVG 非阻断」。不升。
4. **GR-M017-B5-OBS-04** 空列表诚实分页：实现 `Math.max(size,1)` 使空集 pageSize=1≠total=0；现测均有条目。不发明空集必须 pageSize=0。
5. **GR-M017-B5-OBS-05** 「只有待处理的候选项才能发布」被「已发布任务不可再发布」先挡住，无独立 pending 例。
6. **GR-M017-B5-OBS-06** 维持 M017-010 / 本席 B1 OBS-01…16、18：derivedId ≤64 不锁恒 36；候选 id slug；2000/1000 上限（B2 禁止升格）；二次确认 XM；发布追加语义；BEQR-170/187 不在本测试面；P0_SMOKE 旁路；Jdbc `latest()` 耦全仓 V 号；测试源码密码 fallback；Details 非分页；COALESCE 空串；无线程并发；计划 1352 vs 现 2057。

B2 禁止项本席均未升格。

ponytail-review：B3 追加用例是定稿最小面，无可删抽象。`net: 0`。Lean already. Ship.

---

## 8. 命令与真实输出 / 退出码

### CMD-LOCK（独立实算，未抄 sidecar）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
python3 hashlib
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值，见本文第一行。sidecar 全文一行同值。`wc -l` 6850；`wc -c` 889020。

### CMD-T1（主仓，只导出三个 COMPLEX_DATABASE_*，URL 库名改写为 complex_test）

```
cd backend
mvn -o -q -Dtest=FloorPlanControllerTests,FloorPlanImportParserTests,FloorPlanJdbcIntegrationTests,FloorPlanServiceTests,VenueBusinessHoursServiceTests -Dsurefire.failIfNoSpecifiedTests=false test
```

- EXIT=**0**
- Flyway：`Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)`；`Successfully validated 391 migrations`；`Current version of schema "public": 423`；`No migration necessary`
- Java：`Starting FloorPlanJdbcIntegrationTests using Java 21.0.12`
- surefire XML `testcase`（不以文本报告的 `@Nested`=0 为准）：

| XML | tests_attr | testcase | fail | err | skip |
|---|---:|---:|---:|---:|---:|
| TEST-…FloorPlanControllerTests.xml | 14 | 14 | 0 | 0 | 0 |
| TEST-…FloorPlanImportParserTests.xml | 14 | 14 | 0 | 0 | 0 |
| TEST-…FloorPlanJdbcIntegrationTests.xml | 7 | 7 | 0 | 0 | 0 |
| TEST-…FloorPlanServiceTests.xml | 36 | 36 | 0 | 0 | 0 |
| TEST-…VenueBusinessHoursServiceTests.xml | 12 | 12 | 0 | 0 | 0 |
| **合计** | **83** | **83** | **0** | **0** | **0** |

嵌套例：`classname=…VenueBusinessHoursServiceTests$RealRepositoryCases` `name=findVenueProjectIdAndListByProjectHitRealSpacesTable` `time=0.015`。

日志摘录（鉴权/缺键，非失败）：`api error: code=PERMISSION_DENIED raw_message=permission denied` ×8（403 循环）；`code=VALIDATION_FAILED raw_message=idempotency_key is required` ×4。

### CMD-ROLLBACK（只读 SELECT，complex_test）

| SQL | n |
|---|---|
| `SELECT COUNT(*) FROM projects WHERE id='prj_jdbc_real'` | 0 |
| `SELECT COUNT(*) FROM spaces WHERE id IN ('sp_vbh_a','sp_vbh_b')` | 0 |
| 夹具平面图 id LIKE `fp_%jdbc%` 等 | 0 |
| `venue_business_hours` id LIKE `vbh_real_%` | 0 |
| `flyway_schema_history version='423' AND success` | 423 |

### CMD-TREE

`git ls-tree -r 03981e7c…` 与工作区 `git hash-object` 五文件全等。HEAD=`origin/main`=`e2abb037…`。

### CMD-MUT

副本 `/tmp/m017-b5-mut`（rsync backend，结束已删除）。第一次因 JAVA_HOME 指向无 javac 的 openjdk 目录，compiler `release version 21 not supported`——记录为环境原因，不入 finding。第二次 `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`：M1/M2/M3/M6/M7/M8 击红；M4 EXIT=0。未在主仓 cwd 写模式、未改源/HTML/锁、未对 complex_test* 手工写。

未运行的测试：无（5 类全跑）。不把未运行测试写通过。

---

## 9. 技能实用 / 缺口

实用：grilling 追问树直接指向变异表；verification-before-completion 阻止在 CMD-T1 前声称；diagnosing-bugs 要求可红环（XML 计数 + 残留 SELECT + `/tmp` 变异）；ponytail-review 确认 B3 追加不是膨胀。

缺口：见 §0。`batch-grill-me` / `ubiquitous-language` 本机无独立技能，不冒称。

---

## 10. 本席 B5 结论

**PASS**。

同时满足：本模块 5 文件均有覆盖记录；90 判据已逐项核对；三线均执行；声称均有四类证据之一（HTML 原文 / 可执行产物 / 外部规范原文 / 可复现命令）；无未答 questions；无未定稿缺陷；无添加项；无 HTML 修改申请；哈希行有效。

001–008 独立复验仍能失败；009/010 不升格；B2 禁止项不升格。观察 GR-M017-B5-OBS-01…06 登记但不入队。

本票是席乙单席 B5-r1 PASS，不是双席合取，不是模块 ledger PASS。

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M017 B5-r1 再执行 整模块全量三线审计（Grok 席乙）V0.02

- 文件版本：V0.02（同名 raw 追加；旧 33467 bytes / SHA256 95db6f0d412b283157a8f7a920d0be1d82ba106a6c41366e031e54b1924719a4 前缀完整保留，不覆盖）
- 席位：固定 Grok 独立只读审计席乙（grok-4.6 / xhigh；不派子代理、不用 MCP、不问操作员、不读本次 M017-b5-r1-self-raw.md 新追加段）
- 步骤：B5-r1 再执行（操作员 2026-09-20 裁定：历史双 PASS + 席甲单席补充复查不算完成，须席乙完整 B5 全量重审；B3 累计仍为 1；不是只看 diff；不可用历史票替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M017 同一 5 文件；计划冻结有效内容行 1352；本席实算工作区 `\n` 计数 **2124**（HEAD 已跟踪口径约 2057 + 工作区未提交 M015-015/016 测试追加净 +67；不改计划凑数）
- 模块：M017 — 空间点位、楼层平面图与营业时段／后台验证／第1段
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算三工具同值（见第一行独占锁行）：`sha256sum` / `python3 hashlib` / `openssl dgst -sha256`；889020 字节 / 6850 行。sidecar 全文一行同值。ledger 顶部 `CRITERIA-LOCK-SHA256:` 同值。正文不再散写同名独占字段。
- 映射：锁 L6396 `M017 → […]` COUNT=90 unique=90 MISSING=[] EXTRA=[]；BEQR 22 / BSM 14 / GEN 18 / PGM 36。定义从锁自读；相关三 HTML 按锁「来源」行对照现行文件，不新造判据、不 LOCK-AMEND。
- 正本：工作区 `/home/james/complex-management`；`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`（`audit(M046): …`）；`origin/main=485b7a77b7771abc28333bd3422db136304c2934`（HEAD ≠ origin/main，只记录事实）。本模块 5 文件中 3 个与 index 一致，2 个工作区已改（见 §1）。
- 本席约束：只读业务代码与计划；除本 raw 外不写文件；不修源/三 HTML/锁/findings/ledger；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；不对 `complex` 生产库或 `complex_test` 做手工写/DDL（仅跑指定 `mvn -Dtest=…`，测试类自身 `@Transactional` 由框架回滚）。本轮未建 `/tmp` 变异副本、未写脚本/日志临时产物。
- 未读取：本次 `M017-b5-r1-self-raw.md` 新追加段。旧 B1/B4/B5（含本文件 V0.01 前缀）与 findings 定稿边界可读，不作本轮通过依据。
- 已定稿边界（不代替本轮全量重审）：findings M017-001…010 当前全部「否」（001–008 已修；009/010 观察）。不把全部否当功能全实现；不重复立项已定稿 ID；允许真实新证据或回归。B2 定稿禁止项不升格为缺陷：改实现/迁移/OpenAPI/三 HTML/锁；新增测试文件；corridor 进白名单；加分页参数；加二次确认字段；requireText 英文键（FieldLabels 口径）当缺陷；导入上限 2000/1000/100 万字符铁律 6 收窄项当必补用例（100 万字符中文整句已由 006 落例，见下）。

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问 2.2、无锁外判据、无 HTML 修改申请。001–008 在当前 5 文件上独立复验仍能失败；009/010 维持队列「否」，本席不升格。模块生命周期由调度写 ledger，本票不等于双席合取或模块 PASS。

---

## 0. 技能自核（V0.17 §6，按 B5 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令优先。禁止子代理处按派发收窄 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| graft | `~/.claude/skills/graft/SKILL.md` | 实现定位用 `graft skeleton`（FloorPlanService/Parser/Controller/VenueBusinessHoursService），再按 spans 读源；不扫排除目录 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证（10 码、发布不覆盖、诚实分页、8 端点 403、corridor/CHECK、中文整句、真实 SQL、审计、currentActor 夹具），不向操作员发问、不派子代理 |
| grill-me | 同套件 `productivity/grill-me/SKILL.md`（转发 grilling） | 内部 sharpen |
| diagnosing-bugs | mattpocock `engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（锁哈希 / 指定 `mvn -Dtest` / surefire XML）再下结论；B5 不修复 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未运行测试不写通过 |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流；B5 不修复、不加诊断桩 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地。结论：当前 5 类是定稿最小可红面 + M015-015/016 夹具收紧。Lean already. Ship. |
| codebase-design | mattpocock `engineering/codebase-design/SKILL.md` | 删除这 5 类则 PGM-023 导入/发布、BEQR-152 项目隔离、V423 列级约束在验证面失去可红反馈。不派 DESIGN-IT-TWICE |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 主仓 `mvn` 前台完成（surefire 时间戳 2026-09-19 23:45），未起 monitor |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）、using-git-worktrees、executing-plans、dispatching-parallel-agents、subagent-driven-development、writing-for-agents（收口文件由席甲在双 PASS 后写）。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能（有 `grilling` / `grill-me`）
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- `git-guardrails-claude-code`：可选；本席无写 git 操作，未调用
- Arc `audit` / Trail of Bits `audit-context-building`：会派子代理，**不调用**

明确不用：MCP、第三主体、修源/HTML/锁/台账。

graft 本轮：skeleton ×4（FloorPlanService / ImportParser / Controller / VenueBusinessHoursService）。

---

## 1. 锁、范围与源指纹

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值，见本文第一行独占锁行。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 字节 / 行 | 889020 / 6850 |
| python3 hashlib | 与 sha256sum 同值 |
| openssl dgst -sha256 | 同值 |
| sidecar `plan/criteria-lock.sha256` | 全文一行同值 |
| ledger 顶部锁行 | 同值；`LOCK-AMEND-COUNT` 本席只读见到为 1（不改 ledger） |
| findings.md | 993455 字节；SHA256 `f2ccaf6ced3d3f1a9a63802257cf89781333a35d71e51595b4bff9d3769ad4e4` |
| ledger.md | 916610 字节；SHA256 `ede2c981eab53e63058e5af03deba9b1a42159e0692330b4112daeb44dc05b06` |
| 映射 L6396 | 90 条、无重复、MISSING=[] EXTRA=[]；BEQR 22 / BSM 14 / GEN 18 / PGM 36 |
| 计划 FILES | 5 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结行 | 1352；工作区 `\n` **2124**；非空行 1856 |
| questions | 无 `questions/M017*` |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| modules.md / runbook.md | SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` / `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466` |
| origin/main | `485b7a77b7771abc28333bd3422db136304c2934` |

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 行 | 字节 | SHA256 |
|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | 304482 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | 103015 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

锁「来源」行号均落在现行三 HTML 行数内（BEQR 最深 L291/308；BSM L1957/2032；PGM L951/979）。核心锚点本席对读现行文件：

- PGM-022 `platform-governance-map…html:463` 现行 `<h2>空间和点位：所有业务都要知道“发生在哪”</h2>` 与锁摘录一致
- PGM-023 `:467` `<h3>主要功能</h3>`；`:472`「商场平面图网页导入…候选区域（铺位、摊位、公共区）…不可静默覆盖既有布局证据」与锁摘录一致
- BEQR-152 `:235`「所有数据默认按项目隔离…」与锁摘录一致
- BSM-028 `:698` `<h3>主要功能</h3>` 与锁摘录一致
- PGM-005 `:347` 入口卡「空间和点位」与锁来源行一致

五测试文件指纹（工作区 `sha256sum` / `\n` 计数）：

| 文件 | 版本头 | nl | SHA256 | vs index |
|---|---|---:|---|---|
| `FloorPlanControllerTests.java` | V0.01 | 472 | `acd1f485522c9c31c3ec37c1194226fb5259db54e40a970e3fb2b0ea2aaa7aac` | 未改 |
| `FloorPlanImportParserTests.java` | V0.01 | 193 | `a6e0f52b13a11a0d157df098a569b0e2c62a0044eaebbb3cfa5fe8382e1b758c` | 未改 |
| `FloorPlanJdbcIntegrationTests.java` | V0.03 | 277 | `5a5932ce4bf3dc12d3b1a652cf107144bd33663cef462f6c0d87f405bdedaa59` | 未改 |
| `FloorPlanServiceTests.java` | V0.05 | 897 | `ea7aa63ecdb36e9cb8e0b921c62f012e5931d1a157a99ae202015a6b4ceda31d` | **已改**（相对 HEAD `9f3fb0ab`：+部分发布 / 平台管理员与孤儿员工 / `currentActor` 夹具） |
| `VenueBusinessHoursServiceTests.java` | V0.03 | 285 | `3585ca694bf16fdd809689b819d258641792c6a8c964448374f295dff85c9c76` | **已改**（相对 HEAD `302a5cdc`：平台管理员全量 / 孤儿员工 / `currentActor` 夹具） |

`git diff --stat`：ServiceTests 127 行变更（+117/−50）；VenueHours 40 行变更。本席按**工作区当前字节**全量审，不按 HEAD 旧 blob 审。

---

## 2. 文件覆盖（全文已读，非抽样）

| # | 路径 | 覆盖 | 方法/嵌套（源 `@Test`） | 声称行为是否执行 |
|---|---|---|---|---|
| 1 | FloorPlanControllerTests.java | 1–472 全文 | 14 例；8 端点 | WebMvcTest 实跑；见 XML 14/14 |
| 2 | FloorPlanImportParserTests.java | 1–193 全文 | 14 例；解析器 10 码闭集 | 直接 `new FloorPlanImportParser().parse`；XML 14/14 |
| 3 | FloorPlanJdbcIntegrationTests.java | 1–277 全文 | 7 例；`@SpringBootTest` `@Transactional` | 连 `127.0.0.1:5432/complex_test`；XML 7/7 |
| 4 | FloorPlanServiceTests.java | 1–897 全文 | 39 例；内存仓 + 真 Parser/Idempotency/Audit | XML 39/39 |
| 5 | VenueBusinessHoursServiceTests.java | 1–285 全文 | 14 例（含 `@Nested RealRepositoryCases` 1 例） | 单元 13 + JDBC 1；**计数以 XML 为准=14**（surefire `.txt` 外壳类写 Tests run: 0，属嵌套类记录方式，不采 txt） |

无 OVERSIZE 分段豁免。无未读文件。被测实现只作对照（M015/M016 已闭环）；实现缺陷若有只作跨模块观察。

---

## 3. 可执行证据（指定命令，本席实跑）

**环境（未 `source` `/etc/complex/complex.env`）**：逐行读取后只 `export` 三个变量；将 URL 库名 `complex` 改为 `complex_test`。导出后核验：`URL_DBNAME=complex_test`，`USER=complex`，`PW_LEN=48`。未打印口令。未连生产库 `complex`。未手工写库/DDL。

**命令**：

```bash
mvn -o -q --file backend/pom.xml \
  -Dtest=FloorPlanControllerTests,FloorPlanImportParserTests,FloorPlanJdbcIntegrationTests,FloorPlanServiceTests,VenueBusinessHoursServiceTests \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

**exit code**：0（进程结束；surefire 五 XML 均 `errors=0 failures=0 skipped=0`，无残留 `mvn -Dtest=FloorPlan…` 进程）。

**关键输出（节录，类型 4）**：

- JDBC：`Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)`；`Successfully validated 395 migrations`；`Current version of schema "public": 427`；`Schema "public" is up to date. No migration necessary.`
- Controller 日志可见 `PERMISSION_DENIED` / `VALIDATION_FAILED` / `idempotency_key is required`（与 004/009 口径一致：抛点英文键，出网经 translate；本席不升格 009）
- `VenueBusinessHoursServiceTests$RealRepositoryCases` 启动了 SpringBoot 上下文（嵌套 JDBC 例确跑）

**TEST-*.xml 口径（权威）**：

| XML | tests | errors | failures | skipped |
|---|---:|---:|---:|---:|
| TEST-…FloorPlanControllerTests.xml | 14 | 0 | 0 | 0 |
| TEST-…FloorPlanImportParserTests.xml | 14 | 0 | 0 | 0 |
| TEST-…FloorPlanJdbcIntegrationTests.xml | 7 | 0 | 0 | 0 |
| TEST-…FloorPlanServiceTests.xml | 39 | 0 | 0 | 0 |
| TEST-…VenueBusinessHoursServiceTests.xml | 14 | 0 | 0 | 0 |
| **合计** | **88** | **0** | **0** | **0** |

相对历史 B5-r1 的 83 例：工作区多 5 例（`publishRemainingPendingCandidatesAfterPartialPublish`、两边 `platformAdminWithoutOrganization*`、两边 `staffWithoutProjectScopeIsRejected`）。88/88 全绿不能单独当 PASS；下面三线与 90 项才是。

---

## 4. 第一性原理（逐文件三问）

### 4.1 FloorPlanImportParserTests

1. **为何必须存在**：PGM-023「系统解析出候选区域（铺位、摊位、公共区）」；无本类则 10 个解析错误码与归一化多边形无最小可红面。删除后 HTML 导入句仍在，但验证面为空。
2. **是否实际执行**：`new FloorPlanImportParser().parse(...)` 直达生产解析器，非空跑。14 例 XML 全绿。
3. **未验证前提**：解析器生产码闭集是否恰为这 10 个——已对源 `FloorPlanImportParser.java`：`PAYLOAD_REQUIRED` / `UNSUPPORTED_SOURCE_FORMAT` / `VIEWBOX_REQUIRED` / `POINTS_REQUIRED` / `POINTS_TOO_FEW` / `POLYLINE_NOT_CLOSED` / `POINT_OUT_OF_RANGE` / `NO_SHAPES_FOUND` / `JSON_PARSE_FAILED` / `SHAPES_REQUIRED`。每码至少一例；SVG 与 JSON 两侧均锁 `POINT_OUT_OF_RANGE`；`POINTS_TOO_FEW` 锁中文「多边形至少需要 3 个顶点」。闭合 polyline 成功路径无例——HTML 未要求 polyline，属技术性残余，不升缺陷。

### 4.2 FloorPlanControllerTests

1. **为何必须存在**：PGM-040/041 单一功能授权 + GEN-005/007；8 个 REST 端点是平面图导入/发布的 HTTP 闸。
2. **是否实际执行**：`@WebMvcTest(FloorPlanController)` + 真 `SecurityConfig`/`GlobalExceptionHandler`；服务与授权为 mock。14 例 XML 全绿。Controller 源恰 8 个映射方法，与 `everyEndpointReturnsForbiddenWhenPermissionDenied` 的 8 次循环一一对应。
3. **未验证前提**：`P0_SMOKE` 是 WebMvcTest 旁路而非授权矩阵（010 已观察）。`unauthenticatedListReturnsUnauthorizedOrForbidden` 接受 401 **或** 403（L67–73）——能失败（200 会红），但不区分匿名 401 vs 误配 403；不升新 ID（与 010「P0_SMOKE 旁路」同类弱锁）。缺幂等键 400 已覆盖 create/saveShape/createImport/publish。

### 4.3 FloorPlanJdbcIntegrationTests

1. **为何必须存在**：GEN-003/014 + PGM-017 项目归属 + V423 CHECK；内存仓 `projectExists=Set.of("prj_1","prj_2")` 盖不住真实 `SELECT COUNT(1) FROM projects`。
2. **是否实际执行**：`@SpringBootTest` 连 `complex_test`；Flyway 427；7 例 XML 全绿。`projectExistsQueriesRealProjectsTable` 先对缺失 id 断言 false 再 INSERT 最小 projects 行后 true。`importCandidateShapeTypeFollowsDatabaseCheck` 接受 booth/public_area、拒 corridor。`floorPlanWithoutProjectIsRejectedByDatabase` 锁 project_id NOT NULL。
3. **未验证前提**：口令 fallback `complex_dev_pwd`（L20）在本命令已导出真实口令故未触发；010 已登记。`currentFlywayVersion()==MigrationVersions.latest()` 耦全仓 V 号，010 已登记。本席命令未对库做手工写；`@Transactional` 回滚。

### 4.4 FloorPlanServiceTests

1. **为何必须存在**：PGM-023 发布/人工复核/不静默覆盖；BEQR-152 项目隔离；GEN-003/007/011。删除后服务层可红面消失。
2. **是否实际执行**：真 `FloorPlanService` + 真 Parser/Idempotency/AuditEventService；仓库为内存适配器。39 例 XML 全绿。工作区 V0.05 将授权夹具改为只覆盖 `currentActor()`、走生产 `isPlatformAdmin`/`currentProjectScopeQuietly`（`AccessActor.accountType`=`platform_admin`/`staff`/`merchant_staff` 与 `AuthorizationService.isPlatformAdminBridge` 一致），不再桩 `currentProjectScopeQuietly→Optional.empty()` 那种生产不存在的组合。
3. **未验证前提**：内存 `projectExists` 仍是 `Set.of("prj_1","prj_2")`——真实 SQL 由 JDBC 例锁。导入 2000 节点/1000 顶点上限无例（010）。发布二次确认无例（010 / M015-011 XM）。`import_publish_failed` 审计无例（010）。分批发布后对已 published 候选再发：`publishImportTaskRejectsAlreadyPublishedTask` 只断言 `VALIDATION_FAILED` 码；pending 校验仍会使该例红（源 L387–388）。

### 4.5 VenueBusinessHoursServiceTests

1. **为何必须存在**：场地营业窗口是入场/核销场次边界（服务注释对齐 BSM-003 配置可改、BEQR-152 项目隔离）。无本类则星期/HH:mm/跨零点/停用/审计无验证面。
2. **是否实际执行**：RecordingRepository 覆盖 SQL 入口；审计改真实 `AuditEventService`； nested JDBC 打真实 `spaces`/`venue_business_hours`。XML 14/14。
3. **未验证前提**：实现 `dayOfWeek < 0 || > 6`（周日=0）。用例锁 7/null 非法、5/6 合法，**无 day=0 正向例**——把下界 0→1 全绿。这不是本文件声称的闭集锁（没有「0–6 逐值」声明），也不为理论场景加需求；记观察不立项。`savesDisabledDayWithoutDeletingConfiguration` 走假仓 upsert，不证生产不 DELETE——实现对照 `upsert` 且 M015 闭环，不 XM 新缺陷。

---

## 5. 顺从式：90 项逐项适用/证据

分类口径：能指到本 5 文件的测试声称或被测入口=适用；映射继承但本测试面无声称、且与锁内 HTML 原文无冲突=不适用（不发明、不删）。证据类型：1 源码/测试行；4 本席 `mvn`+XML。不新增判据。

### GEN（18，全部适用——本模块是测试面，GEN-003 为核心）

| ID | 适用 | 证据 |
|---|---|---|
| GEN-001 | 是 | 5 类均有可验证用途（解析/HTTP 闸/SQL/服务/营业时段）。无纯转发测试类 |
| GEN-002 | 是 | 指定命令 exit 0；XML 88/88；日志库名 `complex_test` |
| GEN-003 | 是 | 见 §6 变异表；001–008 原缺口在当前文件上仍能红；夹具改为真 `currentActor` |
| GEN-004 | 是 | 解析 10 码、缺幂等键 400、尺寸/点/项目/场地校验均有负例 |
| GEN-005 | 是 | Controller 8 端点 403；服务层 CROSS_PROJECT/CROSS_MERCHANT/PROJECT_SCOPE_REQUIRED |
| GEN-006 | 是 | 发布失败不落图形；校验失败不占幂等键；JDBC `@Transactional` |
| GEN-007 | 是 | 同键同请求返回首次；跨商户/跨平面图同键不覆盖；缺幂等键 400 |
| GEN-008 | 是 | 诚实分页 page_size；shape_type 与 V423 八值；中文整句与实现一致 |
| GEN-009 | 是 | failed 任务不可发布；错误落库；中文 message 断言 |
| GEN-010 | 是 | 未连生产库；口令未写入报告；匿名列表非 200 |
| GEN-011 | 是 | 平面图 created/shape_saved/import_* / 营业时段 updated/viewed 有事件名断言 |
| GEN-012 | 是 | 发布 ≤200；payload 1_000_001 字符拒；2000/1000 上限 010 收窄 |
| GEN-013 | 是 | `-o` 离线 Maven；数据源指向 complex_test；未声明本机状态未当证据 |
| GEN-014 | 是 | JDBC 表存在 + Flyway 427 + 权限种子；本模块不改迁移 |
| GEN-015 | 是 | 出网中文整句有断言；抛点英文键 009 观察不升格 |
| GEN-016 | 是 | 版本头/行数/SHA 以实算为准（2124 非派发稿 2057） |
| GEN-017 | 是 | 本轮无新冲突/添加；工作区追加例属 M015-015/016 细化，无业务发明 |
| GEN-018 | 是 | 结论落到 HTML 原文、源码行、XML/命令输出 |

### BEQR（22）

| ID | 适用 | 证据 |
|---|---|---|
| BEQR-063 高危动作 | 部分 | PGM-023 称发布为高危须二次确认——测试面无二次确认例（010/M015-011 XM，B2 禁止加字段）。不升格 |
| BEQR-074 隐私桌台 | 否 | 无桌台权限测试声称；无冲突 |
| BEQR-143…151、153–159、166、189 | 否 | 主体/职级/终端/合同/设备/管理费等无本测试声称；无冲突 |
| BEQR-152 项目隔离 | 是 | ServiceTests `listFloorPlansFiltersToProjectScope` L449–457、`accessingFloorPlanBelongingToDifferentProjectIsForbidden` L460–466、`createFloorPlanOutsideProjectScopeIsForbidden` L469–475；VenueHours `rejectsVenueOutsideProjectScope` L115–120、`listFiltersByProjectScope` L122–131；孤儿员工 fail-closed 两边均有。HTML「所有数据默认按项目隔离」 |
| BEQR-170 铺位租赁状态 | 否 | 空置/已租等状态无测试声称（010 已登记）；平面图 shape 不是租赁状态机 |

### BSM（14）

| ID | 适用 | 证据 |
|---|---|---|
| BSM-001 整体说明 | 背景 | 综合体/空间盘子；测试不实现该段，无冲突 |
| BSM-003 通用能力 | 部分 | 配置可改：营业时段 upsert/停用；引导式新建属前端 M018，本测试面无 |
| BSM-004 存档删除 | 否 | 平面图测试无存档/删除入口 |
| BSM-005 分步引导 | 否 | 前端 |
| BSM-006 动态码 | 否 | 无 |
| BSM-007 平台管理员日常权 | 是 | `platformAdminWithoutOrganizationSeesAllFloorPlans` L478–486；VenueHours `platformAdminWithoutOrganizationListsAllVenues` L134–143；夹具 accountType=platform_admin 对齐生产桥 |
| BSM-009 基础管理 | 背景 | 空间是准入基础；测试锁空间/平面图/场地，无冲突 |
| BSM-028 主要功能 | 是 | 「建立…楼层、区域、点位」+ 红线④⑥：跨项目不可见、重复提交不重复生效——项目隔离与幂等例 |
| BSM-029 和谁有关 | 背景 | 票务/酒吧要知道空间；本测试面提供空间归属证据 |
| BSM-072/074 商户管理 | 部分 | 商户 scope 列过滤与 CROSS_MERCHANT_FORBIDDEN（ServiceTests L322–347） |
| BSM-114 各类入口 | 部分 | Controller 权限点 floor_plan.view/manage/import.* |
| BSM-115 相互关系 | 背景 | 无冲突 |
| BSM-120 已确认规则 | 背景 | 无冲突 |

### PGM（36）

| ID | 适用 | 证据 |
|---|---|---|
| PGM-001 治理底座 | 背景 | 无冲突 |
| PGM-002 治理顺序 | 是 | 先权限后动作：Controller 403 循环在缺权时不达服务桩 |
| PGM-003…016 模块入口卡 | 背景 | PGM-005 空间和点位入口与本域相关；其余入口卡无测试声称 |
| PGM-017 项目归属 | 是 | `createFloorPlanRequiresProjectId` / `RejectsUnknownProject`；JDBC `projectExistsQueriesRealProjectsTable`；`floorPlanWithoutProjectIsRejectedByDatabase` |
| PGM-018…021 主体职责 | 否/背景 | 无定价/收入测试声称 |
| PGM-022 空间和点位 | 是 | 平面图/图形/导入/场地营业时段均落在「发生在哪」 |
| PGM-023 主要功能（导入） | 是 | 解析铺位/摊位/公共区（shop/booth/public_area 正例 + corridor 负例）；人工复核分批发布（`publishRemainingPendingCandidatesAfterPartialPublish` L279–307）；`publishImportTaskKeepsExistingShapes` L586–600 锁「不可静默覆盖既有布局证据」。二次确认仍 010 XM |
| PGM-026/027 商户 | 部分 | 商户隔离例；无合同绑定测试 |
| PGM-031/035 | 否 | 经销/提成无声称 |
| PGM-040/041 权限 | 是 | 8 端点单一功能权限点；拒绝优先（403） |
| PGM-042 职级 | 否 | 无职级模板测试 |
| PGM-043/044 | 否 | 外包/桌台挂单无声称 |
| PGM-048 商户侧开放 | 部分 | 商户员工可见本商户平面图、不可跨商户 |
| PGM-060 会员生物信息 | 否 | 无 |
| PGM-065 连接关系 | 背景 | 治理连接楼宇/空间 |
| PGM-066 已拍板 | 部分 | 「界面全中文」由 006 中文整句断言覆盖出网文案；「所有对象带综合体项目归属」由 PGM-017 例覆盖 |

90/90 已核对。无锁外主张。

---

## 6. 对抗式（grilling 变异树；不写临时变异副本）

问：改坏哪条实现分支，哪条用例会红？弱断言？夹具是否伪造生产不存在的组合？

| 变异（对实现，本席只读推理） | 会红的用例 | 结论 |
|---|---|---|
| 解析器丢掉 10 码之一 | 对应 `reports*` / `rejectsUnclosed*` | 闭集已锁 |
| 发布改为先删后写图形 | `publishImportTaskKeepsExistingShapes`（原图形 `contains(existing)`） | 不静默覆盖已锁 |
| `pageSize` 改回 50 | `listShapesReportsHonestPageSizeAndTotal` + `listFloorPlansAndImportTasksReportHonestPageSizeAndTotal` + Controller `$.data.page_size` | 三处诚实分页已锁 |
| 某一端点去掉 `requirePermission` | `everyEndpointReturnsForbiddenWhenPermissionDenied` 该轮 403 失败 | 8/8 已锁 |
| 白名单放行 corridor | `saveShapeRejectsShapeTypeOutsideClosedSet`；JDBC CHECK 仍拒 | 服务+库双锁 |
| 中文整句改回英文 | `reportsPointsTooFewWithChineseMessage`、`publishRejects…` 中文、`rejectsMalformedTime…`、1M 上限文案 | 006 仍能红 |
| `projectExists` SQL 改错表 | `projectExistsQueriesRealProjectsTable`（missing=false / 插入后 true） | 007 真实 projects 已锁 |
| 场地 `findVenueProjectId` 改错表 | Nested `findVenueProjectIdAndListByProjectHitRealSpacesTable` | 007 真实 spaces 已锁 |
| 审计改回 mock 且不 verify | `saveAndListWriteAuditEvents`；平面图 created/import_viewed | 008 已锁 |
| 已发布任务禁止再发剩余 pending | `publishRemainingPendingCandidatesAfterPartialPublish` | M015-016 新例能红 |
| 平台管理员 `isPlatformAdmin` 误判 | `platformAdminWithoutOrganizationSeesAllFloorPlans` / `…ListsAllVenues` | 真 accountType 夹具能红 |
| 无项目员工 fail-open | 两边 `staffWithoutProjectScopeIsRejected` | 红线④ 能红 |
| 缺幂等键仍 200 | create/saveShape/createImport/publish 400 例 | 004 已锁 |

**夹具伪造**：旧 HEAD 桩 `currentProjectScopeQuietly→empty` 对平台管理员是生产不存在组合（真方法会抛 `PROJECT_SCOPE_REQUIRED`）。工作区 V0.05/V0.03 已改为只替换 `currentActor`，与 `isPlatformAdminBridge`（accountType=platform_admin 且 active）一致。RecordingRepository 的 venue Map 仍是单元隔离；真实 SQL 由 Nested 例承担——可接受的双适配器（codebase-design：两个适配器=真缝）。

**弱断言残余（不立项）**：Controller 匿名 401|403；`publishImportTaskRejectsAlreadyPublishedTask` 只比 code；营业时段无 day=0 正向。均非「声称锁闭集却锁不住」的 001 类问题，且第一次正常跑不依赖 day=0 测试才能发现实现仍接受 0。

**不为理论场景加需求**：不要求补 2000/1000 上限例、二次确认例、无线程并发例、全 8 个 shape_type 正例（HTML 要求的铺位/摊位/公共区已正锁，corridor 已负锁）。

---

## 7. 001–010 独立复验（不复用历史票当通过依据）

| ID | 队列 | 本席对照当前文件 |
|---|---|---|
| 001 | 否（已修） | ParserTests 14 例覆盖 10 码 + SVG 越界 + 默认值 + 中文 POINTS_TOO_FEW。成立 |
| 002 | 否（已修） | 既有图形并存 L586–600；failed 任务 L603–618；他任务/不存在候选 L622–639；201 个 + 空列表 L643–660；列表+详情+import_viewed/candidate_viewed L664–679。成立 |
| 003 | 否（已修） | listFloorPlans/listImportTasks 诚实分页 L696–711；Controller 桩 `(…,1,1,1)` 且断言 page_size。成立 |
| 004 | 否（已修） | 8 端点 403 循环 L299–322；saveShape/createImport 缺键 L326–339。成立 |
| 005 | 否（已修） | corridor 服务拒绝 L715–724；JDBC 候选 booth/public_area 接受、corridor CHECK 拒 L214–227。成立 |
| 006 | 否（已修） | 多边形/候选项/200/空列表/100 万字符/场地/HH:mm 中文 getMessage。成立 |
| 007 | 否（已修） | JDBC `projectExists` 真表 L204–210；VenueHours Nested 真 spaces L258–273。成立 |
| 008 | 否（已修） | 真 AuditEventService；updated/viewed；code=VALIDATION_FAILED；admin 全量；effectiveFrom。成立 |
| 009 | 否（观察） | Controller 日志 `idempotency_key is required` 仍现；FieldLabels 出网翻译不在本测试面改。不升格 |
| 010 | 否（观察） | 2000/1000、二次确认、*_failed 部分事件、密码 fallback、Flyway latest() 耦合、txt vs XML 嵌套计数——本席确认仍在，不重复立项 |

无回归使已修项变回「是」。无新的冲突/有缺陷细化/添加。

工作区相对 HEAD 的 5 个新例：属 M015-015/016 已闭环实现的测试收紧（真 currentActor、分批发布），四分类=细化，无业务发明，无工程缺陷。

跨模块观察（不入本模块修复队列）：实现若在 `createImportTaskOnce` 先落 task 再 `normalizeAllowed(candidate.shapeType())`，corridor SVG 可能在候选循环抛错（解析器不校验 shape_type）。本测试面已在 saveShape 负锁 corridor；导入路径未单列。不新开 ID（实现属 M015，且 010 已收窄同类上限/失败事件）。

---

## 8. 四分类收口

- 冲突：0
- 细化（无工程缺陷）：工作区 M015-015/016 测试追加；001–008 已修内容
- 技术性（无业务发明）：WebMvcTest 桩、内存仓、RecordingRepository、JDBC `@Transactional` 夹具
- 添加：0（无 2.2）

HTML 修改申请：无。LOCK-AMEND：无。

---

## 9. 技能实用 / 缺口

实用：grilling 变异表直接指向会红用例；verification-before-completion 阻止在 XML 前声称；diagnosing-bugs 以 XML+库名日志为环；ponytail-review 确认追加的是可红夹具而非膨胀；graft skeleton 把实现 spans 接到测试声称。

缺口：见 §0。`batch-grill-me` / `ubiquitous-language` 不冒称。

未运行的测试：无（5 类 XML 全覆盖）。不把未运行测试写通过。本轮未做 `/tmp` 变异编译（派发禁止临时产物）；变异结论来自源码对照，不是未跑冒称实跑。

---

## 10. 本席 B5-r1 再执行结论

**PASS**。

同时满足：本模块 5 文件均有全文覆盖记录；90 判据已逐项核对；三线均执行；声称均有四类证据之一（HTML 原文 / 可执行产物 / 可复现命令）；无未答 questions；无未定稿缺陷；无添加项；无 HTML 修改申请；本报告第一行哈希行有效且为独立实算。

001–008 独立复验仍能失败；009/010 不升格；B2 禁止项不升格。无新 GR 缺陷票。

本票是席乙单席 B5-r1 再执行 PASS，不是双席合取，不是模块 ledger PASS。
