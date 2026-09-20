CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M019 B5-r1 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.01（指定路径原先不存在，本次新建完整报告）
- 席位：固定 Grok 外审席乙（grok-4.6 / xhigh；不派子代理、不用 MCP、不问操作员、不读本轮 `M019-b5-r1-self-raw.md`）
- 步骤：B5-r1（B3 第 1 次整批文档修复后的整模块全量三线审计；不是只看 diff；不可用 B4 双 PASS、B3 自报红→绿或 B1/B2 历史票替代本轮结论）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M019 同一 4 文件；`runbook.md` V0.02；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 模块：M019 — 空间点位、楼层平面图与营业时段／文档与接口定义／第1段
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值（889020 字节 / 6850 行 / blob `8db3fc02ce8a6dc998863a9784e7e88af511b960`）；sidecar 全文一行同值。独占锁行仅本文第一行，正文不再出现同名独占字段。
- 映射：`M019 → [BEQR-074, BEQR-097, BEQR-115, BEQR-156, BEQR-163, BEQR-166, BEQR-170, BEQR-192, BSM-009, BSM-027, BSM-028, BSM-029, BSM-072, BSM-073, BSM-074, BSM-075, BSM-076, BSM-077, BSM-114, BSM-115, BSM-116, BSM-117, BSM-118, BSM-119, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-007, PGM-016, PGM-022, PGM-023, PGM-026, PGM-027, PGM-028, PGM-029, PGM-030, PGM-031, PGM-057, PGM-058, PGM-059, PGM-060, PGM-066]`，COUNT=58 unique=58 MISSING=[] EXTRA=[]（BEQR 8 / BSM 17 / GEN 18 / PGM 15）；与锁映射行 L6398 逐字相同；定义与 HTML 出处均从锁/三 HTML 自读，不新造判据、不 LOCK-AMEND
- 正本：`/home/james/complex-management` main 主工作区。`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`。B3-r1 after tree 对象 `d14e6792b4f5f4f37de77ec61020f3099246fa3a` 可解析为 `tree`；本模块 4 文件当前工作区 blob 与该 after **MISMATCH_COUNT=0**（`git diff --stat d14e6792… -- <4 paths>` 空）。HEAD blob 仍为修复前（B3 未提交）
- 本席约束：只读业务代码与计划；除本 raw 外不写文件；不修源/三 HTML/锁/findings/ledger；不 fetch/commit/push/merge/deploy；不装依赖；不建脚本/日志/临时产物；文档模块不跑 Maven/Semgrep/vitest
- 未读取：`M019-b5-r1-self-raw.md`（落盘时该路径已存在，本席按独立性禁令整文件不打开）。旧 B1/B2/B4 可读，只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界，不代替本轮全量重审）：findings V2.61 — M019-001…009 修复队列「否（已修，B3-r1）」；M015-010/011 XM 不重复立项。B3 累计 1。模块生命周期仍 `IN_PROGRESS`。本票不等于双席 B5 或模块 PASS。席甲票是作者自审票；本席 FAIL 具否决权。

本次审计结论：**PASS**。001–009 在当前 4 文档上独立复验已落地、无回归；M015-010/011 维持 XM 不升格。无新工程缺陷、无添加项、无待问 2.2、无锁外判据、无 HTML 修改申请。观察见 §8，不入修复队列。

---

## 0. 技能自核（V0.17 §6，按 B5 收窄）

本席实际读取并按收窄口径使用（本机路径实核，不抄 Codex 预检表）：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户 160 提示词优先于技能本体。本步禁止子代理，与技能「先派子代理」冲突处按用户指令收窄 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；路由到本仓库 160 B5，**不执行** Arc `audit`（本体要派评审子代理并写 `docs/arc/audits/`，与双席/§7 冲突，记缺口） |
| graft | `~/.claude/skills/graft/SKILL.md` | `graft grep validateCreateRequest` / `graft callers FloorPlanController` / `graft ask` 导入发布状态机：定位 Controller 映射与 `publishImportTaskOnce`/`parse` 闭集 |
| grilling / grill-me | mattpocock `productivity/grilling`、`grill-me`（本体转发 grilling） | 只用追问树自问自答；本体「等用户」「dispatch sub-agent」作废 |
| verification-before-completion | superpowers 同名 | 先跑 sha256/ls/rg/python 计数再声称；不把未跑的 mvn/semgrep 写成当前通过 |
| systematic-debugging | superpowers 同名 | 只借读错误→追数据流（文档声称→DTO→Controller→Service/Parser→V005/V023/V034/V423→OpenAPI）；B5 不修复 |
| diagnosing-bugs | mattpocock engineering | 用契约对照构造可红信号；本机不跑 Java |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删重复，不落地、不单独立项 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读；本步验证均秒级，未另挂 monitor |
| codebase-design | mattpocock engineering | 四文档是 FloorPlan 深模块的浅接口；查 seam 不匹配 |
| writing-for-agents | mattpocock productivity | 本 raw 按完成条件 + `文件:行` 组织，供合取 |
| git-guardrails-claude-code | 可选存在 `mattpocock-skills/.../misc/git-guardrails-claude-code` | 未装 hook；等价：不 fetch/commit/push/merge/hard reset |
| receiving-code-review | superpowers | B5 外审否决席，不 IMPLEMENT |
| requesting-code-review | superpowers | 节点为 B4，本步不派 |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、using-git-worktrees、executing-plans、dispatching-parallel-agents、subagent-driven-development、domain-modeling、research、ponytail-audit。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能
- `ubiquitous-language`：本机技能树未找到
- Arc `audit`：与双席/§7 冲突，**不调用**
- Trail of Bits `audit-context-building` / `differential-review`：会派子代理或只审 diff，**不调用**

明确不用（controller 划掉）：MCP、第三主体、修源/HTML/锁/台账。

graft 本席调用节省合计约 23300 tokens（`validateCreateRequest` / `FloorPlanController` / import publish）。

---

## 1. 锁、范围与源指纹（实算，非手抄）

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`；`python3 hashlib`；`openssl dgst -sha256`。三工具同值，见本文第一行独占锁行。未抄 sidecar / ledger / 派发期望值。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 字节 / 行 | 889020 / 6850 |
| 锁 blob | `8db3fc02ce8a6dc998863a9784e7e88af511b960` |
| sidecar | 全文一行同值 |
| findings | V2.61；SHA256 `b27477597b3305854bbf5242e73fb671f6874d95c25ba78fb08e65d7ebd72dff` |
| ledger | SHA256 `7a1442bc8d6aac980ad742e527f5d96fa89655ea563604f0a6fbabb57e453003`；M019 `IN_PROGRESS`，B3=1，B5-r1 进行中；顶行锁哈希与本席同值；`LOCK-AMEND-COUNT: 1` |
| 映射行 | L6398，58 条、无重复、MISSING=[] EXTRA=[] |
| 计划 FILES | 4 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 680（切分时）；当前工作区 `\n` 计数 **695**（B3-r1 后现态，不改计划凑数） |
| `git ls-files -z -- <4> \| xargs -0 cat \| wc -l` | **695** |
| questions | 无 `questions/M019*`；无未答问询 |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| modules.md / runbook.md | SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` / `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| after tree | `d14e6792b4f5f4f37de77ec61020f3099246fa3a`（`git cat-file -t` = tree） |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466` |

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 行 | SHA256 | 现行版本注释 |
|---|---:|---|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` | V0.07 |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` | V0.14 |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` | V0.12 |

治理地图 L463/L467/L472 现行原文（本席读文件）：

> L463 `空间和点位：所有业务都要知道“发生在哪”`
> L469 `维护楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位。`
> L472 `商场平面图网页导入：在后台上传或导入商场平面布局图，系统解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误后发布到正式平面图；发布为高危动作，须二次确认——发布会写入正式平面图区域，不可静默覆盖既有布局证据。`

业务地图 L700（BSM-028）：`建立综合体项目、楼层、区域、点位；当前一个综合体，未来可扩展为多综合体。`

---

## 2. 全文件覆盖表（全文已读，不抽样）

行=`\n` 计数（与 `git ls-files -z | xargs -0 cat | wc -l` 同口径）。四文件均 `endswith \\n`=True；均 git tracked；相对 after tree 无 diff；相对 HEAD 为 B3 未提交改动（`4 files changed, 37 insertions(+), 25 deletions(-)`）。

| # | 路径 | 行 | 字节 | SHA256 | 工作区 blob | after blob | 全文 |
|---|---|---:|---:|---|---|---|---|
| 1 | `docs/p1-floor-plan-polygon-api-contract-v0.1.md` | 131 | 8402 | `9a5f5c9949022dc78553410e162191ecb19d95ca5af9ad22cd3a11c50aa8e7ce` | `b83cfa51563d32a535f29ad7191cb8c5ea2ca1ac` | 同 | 已读 L1–L131 |
| 2 | `docs/p2-floor-plan-import-api-contract-v0.1.md` | 214 | 7349 | `e36e6160b33372c708ee2acc5c4d426167cca525ed48abca3be9b68f168de921` | `00797fe66bd8a9ff856f1cf0902874900983ec45` | 同 | 已读 L1–L214 |
| 3 | `docs/p2-floor-plan-import-completion-audit-v0.1.md` | 71 | 4488 | `54e364a7e151fcd7f84a04df1d2d96ff6e3aee9ce4566b76ed3432a82d429e2c` | `f7eadfbb17ed52b375c66da53422f6fea892aec5` | 同 | 已读 L1–L71 |
| 4 | `docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md` | 279 | 12116 | `05f1296760679eee73a9491aa06e5bd1bd554a14fcafddbe571cab3529fac388` | `6a89f9ccb53b7e33fd1e760bfab50d416df32eac` | 同 | 已读 L1–L279 |
| | **合计** | **695** | | | | | |

HEAD blob（修复前，对照用，非审计对象）：p1 `9045ec9585bbeb21192b9c0017246305ca90bedb`；p2 `60f38bb688d53ed8d58e01b74b0575edd3a63d97`；完成审计 `aca8448afd9696dfc29851c1a622dad859518d89`；计划 `6f41b39fec2c72e28ec5abcce7965ab8334a9fc0`。

### 2.1 结构段覆盖

| 文件 | 段 | 行 | 已读 |
|---|---|---|---|
| p1 | 标题/变更记录 V0.2 | L1–L6 | 是 |
| p1 | §1 范围表 | L7–L16 | 是 |
| p1 | §2 坐标规则 | L17–L27 | 是 |
| p1 | §3 接口 | L28–L36 | 是 |
| p1 | §4.1 创建字段 | L37–L56 | 是 |
| p1 | §4.2 标注字段 | L57–L70 | 是 |
| p1 | §5 事件 | L71–L79 | 是 |
| p1 | §6 错误矩阵 | L81–L96 | 是 |
| p1 | §7 Anti-Patterns | L98–L106 | 是 |
| p1 | §8 测试规格 | L108–L122 | 是 |
| p1 | §9 References | L124–L131 | 是 |
| p2 | §1 定位 | L1–L9 | 是 |
| p2 | §2 表与权限 | L11–L26 | 是 |
| p2 | §3.1 创建导入 | L30–L81 | 是 |
| p2 | §3.2–3.3 查询 | L83–L129 | 是 |
| p2 | §3.4 发布 | L131–L157 | 是 |
| p2 | §4 解析规则 | L159–L191 | 是 |
| p2 | §5 验收 | L194–L203 | 是 |
| p2 | §6 当前验证 | L205–L214 | 是 |
| 完成审计 | §1–§7 全文 | L1–L71 | 是 |
| 计划 | §1–§3 目标/边界 | L1–L44 | 是 |
| 计划 | §4 文件变更 | L47–L71 | 是 |
| 计划 | §5 数据结构 | L73–L124 | 是 |
| 计划 | §6 接口 | L126–L181 | 是 |
| 计划 | §7–§8 事件与 TDD 勾选 | L183–L234 | 是 |
| 计划 | §9–§12 故事/验收/风险 | L236–L279 | 是 |

### 2.2 引用路径逐一 `ls`（类型 4）

| 路径 | 存在 |
|---|---|
| `docs/superpowers/plans/2026-06-14-p1-floor-plan-polygons.md` | EXISTS |
| `docs/master-roadmap.md` | EXISTS（L217 标题「阶段 8：市场基准增强与 SaaS 化深度能力」） |
| `docs/p0-user-story-map-and-sprint-slices-v0.1.md` | EXISTS（§1.1 L19 / 表行 L26「第二期 / P1」） |
| `backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql` | EXISTS（`spaces.polygon_points` L64） |
| `backend/src/main/resources/db/migration/V001__initial_schema.sql` | **MISSING**（现文已不再引用） |
| `backend/src/main/resources/db/migration/V001__baseline.sql` | EXISTS（本席 rg `polygon_points` 该文件 0 命中；P0 轻量坐标以 V005 为准） |
| `backend/src/main/resources/db/migration/V023__p1_floor_plans.sql` | **MISSING**（实名为 `V023__p1_floor_plan_polygons.sql`，EXISTS；四文档未写错名） |
| `backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql` | EXISTS |
| `backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql` | EXISTS（L5–8 八值 CHECK） |
| `backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java` | EXISTS |
| `FloorPlanService.java` / `FloorPlanImportParser.java` / `FloorPlanRepository.java` / `JdbcFloorPlanRepository.java` | EXISTS |
| 计划 §4.1 新增 Java 记录/Parser/DTO + 四测试类 | 均 EXISTS |
| `docs/00-current-effective-document-entry-v0.1.md` | EXISTS |
| `docs/openapi/openapi-p0.yaml` | EXISTS（不在 `backend/src/main/resources/`） |
| `package.json` 键 `verify:p0-contract` / `verify:predeploy` | 均存在 |

只读追实现（不纳入本模块队列；跨层漂移记观察）：`FloorPlanController`/`FloorPlanService`/`FloorPlanImportParser` 及 dto、`docs/openapi/openapi-p0.yaml`、V005/V023/V034/V423、`FloorPlan*Tests`。

---

## 3. 已定稿 001–009 独立复验（不把「否」当功能全实现）

对照现行 4 文档 + 源码/OpenAPI/迁移。表内「落地」= 定稿最小处置在现文可复现，且照现文第一次正常调用不再踩原缺陷。

| ID | 定稿最小处置要点 | 现文证据 | 源码对照 | 回归？ |
|---|---|---|---|---|
| M019-001 | 删 `evidence_attachment_ids`/`operator_user_id`；`z_index` 改否、缺省 0 | 字段表 L43–L70 无此两字段；`z_index` L69「否…缺省 0」。`rg` 四文档仅变更记录 L5 提及旧名 | DTO `FloorPlanCreateRequest`/`FloorPlanShapeSaveRequest` 无该两字段；OpenAPI CreateRequest `additionalProperties: false` 亦无；`saveShapeOnce:277` `zIndex()==null ? 0` | 无 |
| M019-002 | 删不存在的 `FLOOR_PLAN_SHAPE_NOT_FOUND`；未知 id 新建；他图 400；补三码 | 矩阵 L91–L95：`PROJECT_NOT_FOUND` 404、`CROSS_PROJECT_FORBIDDEN`/`CROSS_MERCHANT_FORBIDDEN` 403、他图 `VALIDATION_FAILED`「图形不属于该平面图」、未知 id 新建 | `rg FLOOR_PLAN_SHAPE_NOT_FOUND backend` = 0；`saveShapeOnce:260-270`；`validateCreateRequest:511-518`；`checkProjectOwnership:609-611`；`checkMerchantOwnership:623-627` | 无 |
| M019-003 | 引用改为真实文件/标题 | §9 L128–L131：计划路径 EXISTS；roadmap 用真实标题不用坏 fragment；故事图指 §1.1「第二期 / P1」行；迁移 `V005__p0_platform_master_data.sql` | V005:64 `polygon_points`；roadmap L217；故事图 L19/L26 | 无 |
| M019-004 | parsed **或** published 可继续发剩余 pending；failed/cancelled 拒 | P2 L153；完成审计 L20；计划 L209 | `publishImportTaskOnce:367-368`；测试 `publishRemainingPendingCandidatesAfterPartialPublish` | 无 |
| M019-005 | 示例改为派生形态并加「示意」 | P2 L58 `fpi_`+32 hex；L71 派生规则 +「本文示例 id 均为示意」；计划 L176 占位 `<候选 id 1>` | `derivedId:664-665` 前缀+SHA-256 前 32 hex；任务 `:298` `fpi_` | 无（3.3 短 id 见 OBS-1） |
| M019-006 | `source_format` 必填无默认；闭合 polyline；非八种整单 400 | P2 L72/L163–L168；完成审计 L15 | `validateImportCreateRequest:555` `normalizeAllowed`；Parser L18 `(polygon\|polyline)`、L79-83 `POLYLINE_NOT_CLOSED`；候选循环 `:321` `normalizeAllowed(SHAPE_TYPES)`；`IdempotencyService:169-172` 有事务模板时 action 回滚，HTTP 路径不落半成品任务 | 无 |
| M019-007 | §5 标快照；契约 §6 以 `verify:predeploy` 为准；计划列表改信封不分页 | 完成审计 L51 快照声明；P2 L214；计划 L155「PageResponse 信封，单页全量，不分页」 | `listImportTasks:139-152` `new PageResponse<>(records, 1, max(size,1), size)`；`package.json` 两键存在。只读计数：四测试类 `@Test` **74**（14+39+14+7）；backend `@Test+@ParameterizedTest` **2987**；`operationId` **732**（快照 33/528/181 已标明非当前门禁） | 无（§1 完成句见 OBS-2） |
| M019-008 | §3.4 补二次确认句，不改请求体、不重开 011 | P2 L157「须二次确认…本文尚未列入请求体（M015-011）」 | DTO `FloorPlanImportPublishRequest` 仍三字段；OpenAPI PublishRequest 无确认字段。M015-011 队列否/XM | 无 |
| M019-009 | 八值 CHECK 见 V423 | 完成审计 L25；计划 L125 | V034:36 仍历史六值；V423:5-8 八值（含 booth/public_area） | 无 |

M015-010（营业时段草案/生效日，XM）与 M015-011（发布确认字段，XM）：本模块 4 文档未把 XM 写成已落地代码；008 明确「尚未列入」。不重复立项。

---

## 4. 第一性原理三问（逐文件）

### 4.1 `docs/p1-floor-plan-polygon-api-contract-v0.1.md`

1. **为何必须存在？** 治理地图 PGM-022/023 与业务地图 BSM-028 L700 要求维护楼层/区域/点位，并以平面图为底。删掉本契约，P1 图片+多边形的字段/权限/事件没有给实施/前端的稳定接口定义。上位：PGM-023 L469 + L472 导入句的底图承载。答得上 → 有业务上位；分类细化。
2. **声称是否被执行验证？** 接口路径/权限与 `FloorPlanController.java:31-56` 一致（`GET/POST /api/v1/floor-plans`、`.../shapes`；`floor_plan.view`/`manage`）。坐标规则与 `validateShapeRequest:539-546` 一致（≥3 点、x/y∈[0,1]）。事件 `floor_plan.created`/`shape_saved`/`shape_viewed` 与服务一致。§4 字段与 DTO 对齐（无幽灵字段）。§6 三码与 `saveShapeOnce` 他图/未知 id 行为与服务一致。§9 四条引用 `ls` 均可打开。§8 测试 ID（FP-UT-*）在测试类中无同名，对应行为散落在 M015/M017 测试（文档模块不强制跑测，不因此立缺陷）。
3. **未验证前提？** 本席未跑 `mvn test` / `verify:p0-contract`（文档模块豁免）。OpenAPI CreateRequest 仍写 `project_id`「可为空」（跨模块，P1 已必填且与服务一致，见 OBS-3）。`operator_note`「默认 created/shape_saved」服务创建路径不落该字段（OBS-4）。均不构成第一次按现文调用即失败。

### 4.2 `docs/p2-floor-plan-import-api-contract-v0.1.md`

1. **为何必须存在？** PGM-023 L472 明文「商场平面图网页导入…解析出候选区域…人工复核…发布」。无此契约则导入权限/事件/解析规则无接口定义。
2. **声称是否被执行？** 四端点与 Controller `:61-93` 一致；权限 `floor_plan.import.view`/`manage` 与 V034 L58–61 一致；1_000_000 字符与 TOO_MANY_NODES/POINTS 与 Service `:558-577` 一致；无候选 → `failed` 与 `:299` 一致；发布 parsed/published、派生 id、source_format 必填、闭合 polyline、非八种整单 400、二次确认未入体，均与现行服务/DTO 一致。
3. **未验证前提？** GET `/imports` 响应信封（PageResponse）契约 §3.2 未画出 JSON（计划 6.2 已写）；3.3 仍用短 id `fpi_1`/`fpic_1` 但 L71 声明「均为示意」。照 §3.1 规则写请求/解析 id 不会与实现相悖。

### 4.3 `docs/p2-floor-plan-import-completion-audit-v0.1.md`

1. **为何必须存在？** 作为 P2-A 完成边界与验证入口。删掉则完成声明无处落。分类：细化（实现完成记录），不是添加。
2. **声称是否被执行？** 闭环「导入→候选→错误→人工发布→正式 shape」与现行服务路径一致。矢量图含闭合 polyline、分批发布、V423 八值 CHECK、关键文件 `ls` 均存在。§5 数字已标明 2026-06 快照，当前以四测试类与 `verify:predeploy` 为准。
3. **未验证前提？** §1 仍用现在完成时写「已在 160 机器通过…」（OBS-2）。本席**未**重跑 `mvn test` 与 Semgrep，故不把历史通过写成当前门禁 PASS；§5 已切断该误用。只读计数证明快照数字与现仓不同，这正是快照声明要防止的误读，不构成新缺陷。

### 4.4 `docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md`

1. **为何必须存在？** 原 TDD 编码计划；与契约重复处是浅副本。删掉计划本身不删 HTML 能力，但入口文档仍引用它。分类：细化（实现计划）。文首已标明其余为历史编码计划原文。
2. **声称是否被执行？** §4.1 列出的新增 Java/SQL 均 EXISTS；§8 勾选 [x] 表示历史任务完成。发布状态机、列表示意、V423、示例候选 id 已按 004/005/007/009 改写。
3. **未验证前提？** 勾选「在 160 运行 mvn test / verify:p0-contract / Semgrep」是历史编码计划，不是当前门禁声明（与完成审计 §5 分工）。不把历史 [x] 当本次跑测证据。

---

## 5. 顺从式：58 判据适用证据

每条均读锁定义全文，并核 HTML 原文行（BEQR `td.k` / BSM·PGM `h2/h3`）。本模块是文档，判据「实现、测试和文档须与锚点原文一致」在此落在**四文档声称**上。

### 5.1 GEN-001…018（通用）

| ID | 适用 | 结论 | 证据 |
|---|---|---|---|
| GEN-001 | 是 | 四文档有上位（PGM-023/BSM-028）；计划与契约部分重复，不单独立项删除 | 删除测试：删过时字段/错误码后接口回到现行 DTO；B3 已做 |
| GEN-002 | 是 | 现文入口/主路径/错误路径与 Controller/Service/Parser 一致 | §3 复验表；grilling Q1–Q5 按现文调用不再与实现相悖 |
| GEN-003 | 是 | 文档模块不强制跑测；完成审计用测试计数处已标快照 | 只读 `@Test` 74 / 全仓 2987 / operationId 732；未当当前 PASS |
| GEN-004 | 是 | 契约写了 payload 上限与节点/顶点上限，与服务一致 | Service `:558-577` |
| GEN-005 | 是 | 八端点权限与 Controller/V023/V034 一致；`import.manage` high+requires_approval=TRUE（V034:61） | 文档风险列「高」；审批闸细节见 OBS-5 |
| GEN-006 | 是 | 计划/V034 FK、候选不直接写正式表，与实现一致；主键派生与文档 L71 一致 | `createImportTaskOnce:294-357` 先候选后正式 shape 仅在 publish |
| GEN-007 | 是 | 写接口幂等与文档一致 | Controller `requireIdempotencyKey`；P1 矩阵 409 `IDEMPOTENCY_CONFLICT` |
| GEN-008 | 是 | 四文档 vs DTO/服务/迁移枚举现已对齐；OpenAPI 个别漂移不在本 4 文件 | OBS-3/6；不把跨模块 OpenAPI 欠账算本文档缺陷 |
| GEN-009 | 是 | P1 矩阵现行码与服务一致；未知 shape_id 不再伪 404 | L91–L95 vs Service |
| GEN-010 | 是 | F-CROSS-10 上限在契约 L75–79 与代码一致；无秘密泄漏声称 | PASS 本条 |
| GEN-011 | 是 | 文档事件名与 Java `recordSuccess/Failure` 一致（`shape_saved` 下划线） | Java `:282` vs P1 §5；OpenAPI `floor_plan.shape.save` 点号为跨模块观察 |
| GEN-012 | 是 | 2000/1000/1e6 在契约；发布 200 在 OpenAPI/服务，契约未写上限 | OBS-7 |
| GEN-013 | 是 | `verify:p0-contract` / `verify:predeploy` 脚本键存在 | python 读 `package.json` |
| GEN-014 | 是 | V023/V034 存在；现行八值 CHECK 在 V423，完成审计/计划已点名 | V423:5-8；V034:36 历史六值已说明 |
| GEN-015 | 是 | 给人看的种类为八种中文、导入源为矢量图/标准数据；机内码仍 svg/standard_json | 与 `SHAPE_TYPES`/`IMPORT_SOURCE_FORMATS` 一致 |
| **GEN-016** | **是（本模块核心）** | **路径/字段/枚举/错误码/状态/示例 id/完成声明现可从当前范围复现**；§5 快照切断旧数字 | 001–009 复验落地；OBS-1/2 不构成第一次按规则调用失败 |
| GEN-017 | 是 | 本席 findings 按冲突→细化→技术性→添加；无新缺陷、无添加项 | §7 |
| GEN-018 | 是 | 依据为 HTML 原文、ls/rg/python 输出、现行源码（类型 1+4；文档不强制类型 2 可执行产物） | 本 raw |

### 5.2 BEQR（8）

| ID | 锁出处 HTML | 本模块四文档 | 结论 |
|---|---|---|---|
| BEQR-074 隐私桌台权限 | `:125` | 未声称桌台查看/接管 | 适用无相关声称，无冲突 |
| BEQR-097 供应合同模式 | `:159` | 未声称 | 同上 |
| BEQR-115 经销价格与税务快照 | `:180` | 未声称 | 同上 |
| BEQR-156 合同状态 | `:239` | 未把合同八态写入平面图 status | 无冲突（shape/plan status 是 active/inactive/archived） |
| BEQR-163 合同变更类型 | `:251` | 未声称 | 无冲突 |
| BEQR-166 合同审批链 | `:255` | 未声称 | 无冲突 |
| BEQR-170 铺位租赁状态 | `:259` 空置/洽谈中/已租/即将到期/已退租/不可租 | 四文档用 shape/plan `active/inactive/archived`，未记载六态或着色 | **无冲突**（不同对象）；着色见 PGM-029/BSM-076，本 API 未承载 → OBS-8，不发明字段 |
| BEQR-192 分红税务 | `:298` | 未声称 | 无冲突 |

### 5.3 BSM（17）

| ID | 锚点 | 本模块 | 结论 |
|---|---|---|---|
| BSM-009 | `:596` 基础管理卡片 | 平面图属基础管理空间能力 | 无冲突 |
| BSM-027 | `:694` h2 盘子管清楚 | 同上 | 无冲突 |
| BSM-028 | `:698` 主要功能；L700 项目/楼层/区域/点位 | P1 创建含 project_id/floor_no；导入发布正式区域 | 无冲突；project_id 必填与 M015-002 对齐 |
| BSM-029 | `:714` 和谁有关 | 未发明票务/酒吧点位规则 | 无冲突 |
| BSM-072…074 | 商户管理 | 未声称商户专区 | 无冲突 |
| BSM-075…077 | 招商租赁；铺位看板六态着色 | 四文档未写铺位看板 | 看板≠本多边形 API；OBS-8 |
| BSM-114 | `:1827` 各类入口 | 文档权限码对应管理后台，未发明十一端 | 无冲突 |
| BSM-115 | `:1897` 基础管理连所有业务 | 平面图作为空间底图，未否定连结 | 无冲突 |
| BSM-116…119 | 上线加固/安装包 | 完成审计 Semgrep/测试是工程完成声明（已标快照），不是上线加固业务发明 | 无冲突 |
| BSM-120 | `:1957` 已确认规则 | 四文档未改写已确认清单 | 无冲突 |

### 5.4 PGM（15）

| ID | 锚点 | 本模块 | 结论 |
|---|---|---|---|
| PGM-007 | `:357` 合同和准入卡片 | 未声称合同卡口 | 无冲突 |
| PGM-016 | `:404` 应急与隐私卡片 | 未声称破窗 | 无冲突 |
| **PGM-022** | `:463` 空间和点位 h2 | P1 定位为标准地图数据源 | 无冲突 |
| **PGM-023** | `:467` 主要功能；**L472 导入句** | 导入闭环有文档；二次确认已入契约且标明字段未落地（M015-011）；分批发布与实现一致；八种中文覆盖 HTML 三例并扩展为已定稿八值 | 不重开 013/011；无新冲突 |
| PGM-026 | `:492` 商户主要功能 | 未声称招商前链 | 无冲突 |
| PGM-027 | `:507` 商户业务边界 | 未开放商户看全局 | 无冲突 |
| PGM-028 | `:518` 合同准入 h2 | 未声称 | 无冲突 |
| PGM-029 | `:522`；铺位租赁状态在平面图着色 | 本契约无着色字段 | OBS-8，不立项新字段（避免添加） |
| PGM-030 | `:544` 业务意义 | 未声称 | 无冲突 |
| PGM-031 | `:555` 经销商 | 未声称 | 无冲突 |
| PGM-057…060 | 应急/隐私/会员 | 未声称 | 无冲突 |
| PGM-066 | `:951` 已拍板；含全中文、项目归属 | 八种中文/矢量图措辞与 PGM-066 全中文一致；project_id 必填与「对象带综合体项目归属」一致 | 无新冲突 |

顺从式收口：映射 58 项均已核对；无锁外判据。核心 GEN-016 现文可复现。

---

## 6. 对抗式（grilling 结构自问自答；不等操作员、不派子代理）

按用户指定的第一性对抗：照文档写请求 / 处理错误码 / 解析 id / 分批发布；引用是否可打开；完成声明是否写成当前事实。

❓ **Q1 — 照现 P1 契约写创建/保存请求，第一次正常调用会怎样？**
➡️ 不带已删除的 `operator_user_id`/`evidence_attachment_ids`：DTO/OpenAPI CreateRequest 均无此键（`additionalProperties: false`），请求可过反序列化。`project_id` 必填，与 `validateCreateRequest:510-512` 一致。省略 `z_index`：服务缺省 0，与 L69 一致。带他图 `shape_id`：400 `VALIDATION_FAILED`「图形不属于该平面图」（L95 / `:265-266`）。带未知 `shape_id`：按该 id 新建，不是 404。`rg FLOOR_PLAN_SHAPE_NOT_FOUND backend` = 0。**不再与实现相悖。**

❓ **Q2 — 照现 P1 矩阵处理错误码？**
➡️ `PROJECT_NOT_FOUND` / `CROSS_PROJECT_FORBIDDEN` / `CROSS_MERCHANT_FORBIDDEN` / `FLOOR_PLAN_NOT_FOUND` / `IDEMPOTENCY_CONFLICT` / `VALIDATION_FAILED` 均在服务中有对应抛点（`:512/:517/:610/:626/:597` 与幂等服务）。第一次按矩阵写客户端错误分支，与现行码一致。

❓ **Q3 — 照 P2 §3.1 规则解析导入任务 id，再用于后续 GET/publish？**
➡️ 规则：`fpi_` + SHA-256(平面图 id + `_` + 幂等键) 前 32 hex，共 36 字符，不拼接幂等键。与 `derivedId("fpi_", floorPlanId+"_"+key)` `:298/:664-665` 一致。响应示例 L58 为该形态。L71「本文示例 id 均为示意」覆盖 §3.3 短 id `fpi_1`/`fpic_1`（OBS-1）：**按规则（L71）解析不会 404**；若无视规则把 `fpic_1` 当真实主键才会 `IMPORT_CANDIDATE_NOT_FOUND`（文档未列该码，属省略，见 OBS-9）。

❓ **Q4 — 第一次发布一部分候选后，再发剩余 pending？**
➡️ 现文 P2 L153 / 完成审计 L20 / 计划 L209：`parsed` 或 `published` 可继续发 pending；`failed`/`cancelled` 拒；已发布候选不可再发。实现 `:367-368` + 测试 `publishRemainingPendingCandidatesAfterPartialPublish`。**照文档第二次发布是合法路径，与实现一致。**

❓ **Q5 — SVG 里闭合 polyline、或 `data-shape-type=corridor`、或省略 `source_format`？**
➡️ 闭合 polyline：Parser 生成候选（L18/L79-83），与 P2 L163 一致。未闭合：`POLYLINE_NOT_CLOSED` 记错误、不生成候选。`corridor`：Parser 原样入草稿，Service `:321` 整单 `VALIDATION_FAILED`「图形类型取值无效」；生产路径 `IdempotencyService` 事务回滚，不落任务/候选/明细，与 L168 一致。省略 `source_format`：`normalizeAllowed` 必填 → `VALIDATION_FAILED`，无默认，与 L72 一致。

❓ **Q6 — §9 / 完成审计 / 计划中的引用能否打开？**
➡️ 本席 `ls`：p1 四条引用均 EXISTS 且标题/行可定位；V005:64 有 `polygon_points`；V034/V423/Controller/Service/Parser/四测试类/计划 §4.1 文件均 EXISTS。`V001__initial_schema.sql` 现文已不引用。`openapi-p0.yaml` 在 `docs/openapi/`。**引用可打开。**

❓ **Q7 — 完成声明是否写成当前事实？**
➡️ 完成审计 §5 L51 明确「2026-06 编码完成时的 160 记录快照，非当前门禁；当前验证以上述四个测试类与 `npm run verify:predeploy` 为准」。P2 §6 L214 同口径。只读计数 74/2987/732 ≠ 快照 33/528/181，证明快照声明必要且已写上。§1 L7 仍用现在完成时「已在 160 机器通过…」（OBS-2），但同文件 §5 已禁止把该数字当当前门禁。本席未跑 mvn/semgrep，不把 §1 当当前 PASS 证据，也不把已限定的快照升为新缺陷。

❓ **Q8 — 越权/并发/金额/发布确认？**
➡️ 权限码与 Controller 一致，未发现文档把 view 写成可写。金额精度本模块无。幂等冲突码与实现同码。发布二次确认：契约 L157 已写明须确认且字段未入体（M015-011 XM），不是假完成。

❓ **Q9 — 不为理论场景加需求？**
➡️ 下列不立项：铺位六态着色字段（OBS-8）；P2 补全错误矩阵（OBS-9）；发布 200 上限写入契约（OBS-7）；OpenAPI `project_id` 可空（OBS-3）；`operator_note` 默认文案（OBS-4）。第一次按现文规则调用不会因这些省略而把合法请求做成非法，或把非法做成合法。

---

## 7. Findings（本轮临时编号）

四分类顺序：冲突 → 细化 → 技术性 → 添加。

**工程缺陷成立、建议入修复队列：无。**

无冲突（未与三 HTML 明文唱反调到必须删除业务能力）。无添加。M015-010/011 不重复立项。001–009 不重开。

---

## 8. 观察（不入修复队列）

| ID | 内容 | 为何不立项 |
|---|---|---|
| OBS-1 | P2 §3.3/§3.4 示例仍用 `fpi_1`/`fpic_1`（L108/L113/L147）；候选实际为 `fpic_`+slug(taskId_n)，非 SHA 32 hex | L71 已声明「本文示例 id 均为示意」且 §3.1 给出可复现派生规则。按规则不会拼错；把短 id 当算法才失败。005 最小处置已覆盖危险的 `fpi_`+幂等键拼接。不为示意短 id 再开 ID |
| OBS-2 | 完成审计 §1 L7 现在完成时「已在 160 机器通过聚焦测试、全量后端测试、P0 契约闸口和 Semgrep」 | 同文件 §5 已标快照并改当前门禁指针。第一次把 §5 当验证入口不会误用 33/528/181。不把已限定的历史完成句再当 007 回归 |
| OBS-3 | OpenAPI FloorPlanCreateRequest `project_id` 不在 required，描述「可为空时使用当前默认项目」；服务/P1 必填 | 本模块 P1 L47 已必填，与 M015-002 对齐。OpenAPI 属别模块 |
| OBS-4 | P1 `operator_note` 写默认 `created`/`shape_saved`；`createFloorPlanOnce` 不把该字段写入 `FloorPlanRecord` | 省略该可选字段第一次调用仍成功；默认只影响审计文案理解。不构成错误码/字段相悖 |
| OBS-5 | `floor_plan.import.manage` `requires_approval=TRUE` 文档未写「须审批」 | 权限表已写风险「高」；审批闸是权限种子细节 |
| OBS-6 | OpenAPI saveShape `additionalProperties: true`、`x-audit-event-success: floor_plan.shape.save`（点号）vs Java/P1 `floor_plan.shape_saved` | OpenAPI 属别模块；本契约与 Java 一致 |
| OBS-7 | 发布一次最多 200 候选（Service `MAX_PUBLISH_CANDIDATES` / OpenAPI `maxItems: 200`）契约未写 | 超限走已有 `VALIDATION_FAILED`；契约未写错误上限。第一次按契约发少量 id 不失败。不补需求 |
| OBS-8 | PGM-029 / BSM-076 / BEQR-170：铺位租赁六态在平面图着色。四文档 shape.status=active/inactive/archived | 着色是铺位看板/合同状态叠加，不是本多边形 API。HTML 未要求把六态写入 `floor_plan_shapes`。立项会变成添加字段 |
| OBS-9 | P2 无独立错误矩阵：`IMPORT_TASK_NOT_FOUND` / `IMPORT_CANDIDATE_NOT_FOUND` / 候选项不属于任务 等未列 | 契约未声称这些码；第一次按已写规则处理主路径错误码不失败。补矩阵是加需求 |
| OBS-10 | GET `/floor-plans` 与 GET `.../imports` 返回 `PageResponse` 信封；P1 §3 / P2 §3.2 未画 JSON。计划 6.2 已写导入列表信封 | 契约未把 `data` 画成数组。实现信封与 M015-007 诚实不分页一致。不为理论客户端误解析加需求 |
| OBS-11 | P1 L15「本次不实现 CAD/SVG」是 **P1 切片**范围，同模块 P2 已定义导入 | 有 P2 契约并列，读者有导入口径 |
| OBS-12 | 计划与 P2 契约大段重复 | ponytail-review 可删计划中已迁入契约的接口表；非正确性缺陷 |
| OBS-13 | source_payload 列类型 JSONB，JDBC 把 SVG 文本序列化成 JSON 字符串再 CAST | API 仍收 SVG 文本；属实现，文档不必改 |
| OBS-14 | 四测试类 `@Test` 现 74（B1 当时计 75）、全仓 2987（当时 3014） | 快照已标明非当前门禁；本席未跑测，不把计数差当回归 |

---

## 9. ponytail-review（只列可删，不落地）

- 计划 §6 与 P2 契约接口表重复：`shrink:` 计划保留文件清单/TDD 勾选，接口细节以契约为准。Replacement: 计划加一句「以契约为准」。
- P2 §3.3 短 id：`shrink:` 可改成与 §3.1 同形态的 36 字符示意，非必须。
- net: 文档可再少约 20 行重复接口表。不增业务字段。
- Lean already 对 001–009 幽灵字段/错误码：B3 已删。本轮无必须再删的正确性残留。

---

## 10. PASS/FAIL

单席 PASS 条件（runbook §9）：

| 条件 | 本席 |
|---|---|
| 本模块所有文件有覆盖记录 | 是（4/4 全文 + 结构段表） |
| 映射的所有判据 ID 已核对 | 是（58/58） |
| 三线均执行 | 是（§4/§5/§6） |
| 所有声称均有四类证据之一 | 是（类型 1 HTML + 类型 4 ls/rg/python/读源；文档不强制类型 2） |
| 无未答 questions | 是 |
| 无未定稿缺陷 | 是（本轮新工程缺陷 0；001–009 已否且复验落地） |
| 锁哈希行有效 | 是（文首独占行，三工具同值） |

**本席结论：PASS**

添加项：无。HTML 无需人闸修改。未跑：`mvn test`、`npm run verify:p0-contract`、Semgrep（文档模块豁免；完成数字不当当前 PASS）。子代理失败重跑次数：0。MCP：未用。未读本轮 self-raw。
