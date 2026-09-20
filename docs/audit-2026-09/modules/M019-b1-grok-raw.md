CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M019 B1 席乙（Grok 4.6/xhigh）修复前全模块审计 · V0.01

- 席位：审计席乙（独立；未读取 `modules/M019-b1-self-raw.md`，不依赖席甲发现）
- 阶段：V0.17 阶段 B / B1（本模块首次开工；本席不改 ledger/findings/plan）
- 正本：`/home/james/complex-management` main 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`（`audit(M046): 钉住破窗不能无盘续期、换钥作废旧票`）
- `origin/main`：`1d84ec363c8b4c9fca15970583affb5c3346d24c`（与 HEAD 不等）
- 审计对象：主工作区当前 4 文档 blob（`git diff HEAD -- <4 paths>` 空）；含祖先提交 `d84e419b`（`audit M019: eight Chinese shape kinds and vector/standard-data import wording`，属 PR #442，**未**在 `origin/main`）；**不把 PR #442 当作已审 PASS**，不按 origin/main 旧 blob
- 计划：`plan/modules.md` V0.04 M019「空间点位、楼层平面图与营业时段／文档与接口定义／第1段」；计划冻结有效内容行 **680**；本席实算 `git ls-files -z -- <4 paths> | xargs -0 cat | wc -l` = **683**
- 判据锁：`plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` = 上文独占行
- 映射 58 项：与锁 L6398 逐字相同（BEQR 8 / BSM 17 / GEN 18 / PGM 15）；COUNT=58 unique=58 MISSING=[] EXTRA=[]；未新增判据、未扩别模块
- 结论：**FAIL**（有工程缺陷；文档模块未跑 Maven/全量测试，完成声明数字以只读计数证伪）
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M019-001 … GR-M019-009
- 添加项（B1 只登记，不代用户回答 2.2；本批无）：无
- 只登记、建议队列「否」：观察见 §8
- 本席未改任何业务文件/三 HTML/台账/锁，未 fetch/commit/push/merge/deploy，未用 MCP/子代理/第三席，未问操作员，未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓或发布门禁通过。M015/M016/M017/M018 已定稿 ID 不重开；其修复使旧文档过时处按 GEN-016 立本模块新临时编号

---

## 0. 技能预检与节点应用（本环境实核）

本席按 `160-audit-prompt.md` V0.17 §6 与 `plan/runbook.md` V0.02 §2 在 **本机路径** 核验，不抄 Codex 预检表。grilling 只借追问树自问自答（事实自己读）；Arc `audit` / Trail of Bits `audit-context-building` / `differential-review` 本体要派子代理或另写 §7 外产物，**不调用**。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由到 audit；**不执行** Arc 全量审计工作流 | 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，记缺口 |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `graft ask` 定位 FloorPlanController/Service/Parser | 无 |
| `grilling` / `grill-me` | mattpocock `productivity/grilling/SKILL.md`；`grill-me` 仅转发 | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `batch-grill-me` | — | — | **本机技能树不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | 四文档是 FloorPlan 深模块的浅接口；删除测试：删过时字段/错误码/示例 id，复杂度回到现行 DTO/服务闭集 | 不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环=只读命令（sha256/ls/rg 错误码/python 计数），不改生产代码 | 本机不跑 Maven；文档模块按 runbook 不强制测 |
| `systematic-debugging` | superpowers 同名 | 只借读错误→追数据流（文档声称→DTO→Service→Parser→V023/V034/V423→OpenAPI）；作废插桩 | 无 |
| `verification-before-completion` | superpowers | PASS/FAIL 前实跑 sha256/ls/wc/rg/python；不声称 mvn/semgrep 已跑 | 未重跑历史 160 `mvn test` / Semgrep（完成声明因此不能 PASS） |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 可删：契约中不存在的字段与错误码行、过时示例 id | 不落地 |
| `ponytail` | 同包 | B3 默认强度，B1 不用 | 节点不适用 |
| `tdd` / `test-driven-development` | 存在 | 仅 B3 可执行行为 | 节点不适用（文档修复不强制测） |
| `requesting-code-review` / `receiving-code-review` | 存在 | B4 | 节点不适用 |
| `writing-for-agents` | mattpocock productivity | raw 按完成条件+文件:行组织 | 无 |
| `resolving-merge-conflicts` | 存在 | 无合并 | 节点不适用 |
| `git-guardrails-claude-code` | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 可选；本体要问人装 hook | **存在但未安装**（只读审计、禁写仓库其它文件） |
| `i-have-adhd` | marketplace 存在 | Codex 回执模式；本席 stdout 由适配器丢弃 | 不套到本 raw 以外 |
| `long-running-background-tasks` | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒级，未起 monitor | 无 |
| `audit-context-building` | Trail of Bits | 本体强制子代理写 `audit-context/` | **不用** |
| `differential-review` | Trail of Bits | PR/diff 安审 | **不用**（B1 全量非 B4 diff；禁子代理） |
| Arc `audit` | howells | 机械检查+并行评审 | **不用** |

明确不用（提示词已划掉）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`/`debt`/`gain`、MCP、第三主体。

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

---

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
cat docs/audit-2026-09/plan/criteria-lock.sha256
# 同值
git ls-files -z -- <4 files> | xargs -0 cat | wc -l
# 683
```

- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行 `M019 → [...]`（L6398）与开工口径 58 项逐字相同
- `plan/modules.md` M019 FILES 4 条与用户清单逐字相同
- sidecar 与本席实算哈希相等；ledger 顶部 `CRITERIA-LOCK-SHA256:` 同行（只读，未改）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读：M019 行 `IN_PROGRESS`、B3=0（本席不写台账）。findings V2.58 仅作已登记边界背景（M015-001/002/003/013/015/016、M016-002、M017、M018 已定稿），不代替本模块审计。

三份 HTML 只读、未改；未发现需铁律 4 人闸修改的原文错误。

| 文件 | 字节 | SHA256 |
|---|---:|---|
| docs/business-enum-quick-reference-v0.1.html | 133961 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | 304482 | eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 |
| docs/platform-governance-map-for-operations-v0.1.html | 103015 | a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 |

治理地图 L472 现行原文（本席读文件 splitlines `[471]`）：

> 商场平面图网页导入：在后台上传或导入商场平面布局图，系统解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误后发布到正式平面图；发布为高危动作，须二次确认——发布会写入正式平面图区域，不可静默覆盖既有布局证据。

业务地图「基础管理」L700：`建立综合体项目、楼层、区域、点位；当前一个综合体，未来可扩展为多综合体。`（BSM-028 锚点 L698）

`d84e419b is ancestor of HEAD` = true；`git diff origin/main...HEAD -- <4 files>` = 该提交的 4 文件 14+/11-（八种中文与矢量图/标准数据措辞）。审计对象是工作区现状（已含该措辞），仍须用现行源码核 GEN-016，不因该提交存在而 PASS。

---

## 2. 全文件覆盖表（全文已读，不抽样）

行=splitlines=`\n` 计数（与 `git ls-files -z | xargs -0 cat | wc -l` 同口径）。

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | docs/p1-floor-plan-polygon-api-contract-v0.1.md | 130 | 7640 | `1c5deb13e38c2bd269b1a9cf70375ce4647fc72430baf2f267e03909fec51f25` | 已读 L1–L130 |
| 2 | docs/p2-floor-plan-import-api-contract-v0.1.md | 211 | 6343 | `89309e0d9d6ef3e64b61da6873544925cadd71462ec9bfb2fd231360b691980f` | 已读 L1–L211 |
| 3 | docs/p2-floor-plan-import-completion-audit-v0.1.md | 67 | 3863 | `2b4e923a9fa153e6c29fbfff6a10aa63ec8144a4a1e965410a235c527cb9912a` | 已读 L1–L67 |
| 4 | docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md | 275 | 11617 | `eb94a20f69cb1c5afc880a6e34ebbc0a6cf0066f07320c96a573d1039996645f` | 已读 L1–L275 |
| | **合计** | **683** | | | 与操作员「当前 683」一致；计划冻结 680 |

四文件均 `endswith \\n`=True；均 git tracked；相对 HEAD 无 diff。

### 2.1 结构段覆盖

| 文件 | 段 | 行 | 已读 |
|---|---|---|---|
| p1 | 标题/定位 | L1–L6 | 是 |
| p1 | §1 范围表 | L7–L16 | 是 |
| p1 | §2 坐标规则 | L17–L27 | 是 |
| p1 | §3 接口 | L28–L36 | 是 |
| p1 | §4.1 创建字段 | L37–L57 | 是 |
| p1 | §4.2 标注字段 | L58–L73 | 是 |
| p1 | §5 事件 | L74–L82 | 是 |
| p1 | §6 错误矩阵 | L83–L96 | 是 |
| p1 | §7 Anti-Patterns | L97–L106 | 是 |
| p1 | §8 测试规格 | L107–L122 | 是 |
| p1 | §9 References | L123–L130 | 是 |
| p2 | §1 定位 | L1–L8 | 是 |
| p2 | §2 表与权限 | L9–L25 | 是 |
| p2 | §3.1 创建导入 | L26–L80 | 是 |
| p2 | §3.2–3.3 查询 | L81–L128 | 是 |
| p2 | §3.4 发布 | L129–L155 | 是 |
| p2 | §4 解析规则 | L156–L190 | 是 |
| p2 | §5 验收 | L191–L201 | 是 |
| p2 | §6 当前验证 | L202–L211 | 是 |
| 完成审计 | §1–§7 全文 | L1–L67 | 是 |
| 计划 | §1–§3 目标/边界 | L1–L44 | 是 |
| 计划 | §4 文件变更 | L45–L70 | 是 |
| 计划 | §5 数据结构 | L71–L122 | 是 |
| 计划 | §6 接口 | L123–L177 | 是 |
| 计划 | §7–§8 事件与 TDD 勾选 | L178–L231 | 是 |
| 计划 | §9–§12 故事/验收/风险 | L232–L275 | 是 |

引用路径逐一 `ls`（§1.2 类型 4）：

| 路径 | 存在 |
|---|---|
| docs/superpowers/plans/2026-06-14-p1-floor-plan-polygons.md | EXISTS |
| docs/master-roadmap.md | EXISTS（fragment 见 GR-M019-003） |
| docs/p0-user-story-map-and-sprint-slices-v0.1.md | EXISTS（fragment 见 GR-M019-003） |
| backend/src/main/resources/db/migration/V001__initial_schema.sql | **MISSING** |
| backend/src/main/resources/db/migration/V001__baseline.sql | EXISTS（`polygon_points` L64） |
| backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql | EXISTS |
| backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql | EXISTS |
| backend/src/main/resources/db/migration/V423__m016_floor_plan_and_space_type_checks.sql | EXISTS（计划/完成审计未引用） |
| docs/00-current-effective-document-entry-v0.1.md | EXISTS（计划 L69 修改清单） |
| FloorPlanController/Service/ImportParser/Repository/Jdbc + 四测试类 + 计划 §4.1 新增 Java | 均 EXISTS |
| docs/openapi/openapi-p0.yaml | EXISTS |

只读追实现（不纳入本模块队列，缺陷记跨模块观察）：`FloorPlanController`/`FloorPlanService`/`FloorPlanImportParser` 及 dto、`docs/openapi/openapi-p0.yaml`、V023/V034/V423、`FloorPlan*Tests`。

---

## 3. 转入既有口径核实（独立对照现状，不继承 PASS）

| 定稿 ID | 现行实现（本席核源） | 本模块文档是否过时 |
|---|---|---|
| M015-001 派生主键 SHA 形式 | `FloorPlanService.java:664-665` `derivedId` = 前缀 + SHA-256 前 32 hex；任务 `fpi_`+域（`:298`）；图形 `fps_`（`:270`）；候选 `fpic_`+slug(taskId_n)（`:318`） | **是**：P2 响应示例 `fpi_floor-import-create-001`（L56）、`fpi_1`/`fpic_1`（L106–111）；计划 L173 `fpic_1`/`fpic_2`。不重开 001 → GR-M019-005 |
| M015-002/003 project_id 必填且存在、项目 scope | `:508-520` `requireText(project_id)` + `projectExists` → `PROJECT_NOT_FOUND`；跨项目 `CROSS_PROJECT_FORBIDDEN` | P1 L47 已写 project_id 必填（与 002 对齐）。**错误矩阵未列** `PROJECT_NOT_FOUND`/`CROSS_*` → GR-M019-002 |
| M015-015 平台管理员全量桥 | `:616-620` `projectScopeOrAll` | 四文档未声称列表 scope；无过时句。观察 |
| M015-016 发布前置 parsed **或** published、分批发布 | `:366-368` 文案「只有解析完成或已发布的导入任务才能继续发布」；测试 `publishImportTaskRejectsAlreadyPublishedTask` 已改为可继续发剩余 pending | **是**：P2 L151「仅 `parsed`」；完成审计 L18；计划 L205「published task 不允许重复发布」。不重开 016 → GR-M019-004 |
| M016-002 / M015-013 候选八值（无走道） | `SHAPE_TYPES` `:35` 八值；V423 L5–8 CHECK 八值；V034 L36 **仍为历史六值** | PR #442 已把四文档写成八种中文+无走道，与白名单一致。完成审计/计划仍只点名 V034 → GR-M019-009 |
| M017 / M018 | ledger PASS（测试/前端已对齐） | 文档未同步 001/016 等服务层口径；前端对齐不使文档 PASS |

---

## 4. 第一性原理三问（逐文件）

### 4.1 `docs/p1-floor-plan-polygon-api-contract-v0.1.md`

1. **为何必须存在？** 治理地图 PGM-022/023 与业务地图 BSM-028 L700 要求维护楼层/区域/点位，并以平面图为底。删掉本契约，P1 图片+多边形的字段/权限/事件没有给实施/前端的稳定接口定义。上位：PGM-023 L469「维护楼层、区域、点位…」+ L472 导入句的底图承载。
2. **声称是否被执行验证？** 接口路径/权限与 `FloorPlanController.java:31-56` 一致（`GET/POST /api/v1/floor-plans`、`.../shapes`；`floor_plan.view`/`manage`）。坐标规则与 `validateShapeRequest:539-546` 一致（≥3 点、x/y∈[0,1]）。事件 `floor_plan.created`/`shape_saved`/`shape_viewed` 与服务一致。§8 测试 ID（FP-UT-*）在测试类中**无同名**，对应行为散落在 M015/M017 测试（文档模块不强制跑测）。§4/§6/§9 有不可复现声称 → GR-M019-001/002/003。
3. **未验证前提？** 把「P0 模板字段」`operator_user_id`/`evidence_attachment_ids` 当成已实现；把 `FLOOR_PLAN_SHAPE_NOT_FOUND` 当成 404；把 `#阶段-8-…` 当成真实标题 slug。均被 ls/rg 证伪。

### 4.2 `docs/p2-floor-plan-import-api-contract-v0.1.md`

1. **为何必须存在？** PGM-023 L472 明文「商场平面图网页导入…解析出候选区域…人工复核…发布」。无此契约则导入权限/事件/解析规则无接口定义。
2. **声称是否被执行？** 四端点与 Controller `:61-93` 一致；权限 `floor_plan.import.view`/`manage` 与 V034 L58–61 一致；1_000_000 字符与 TOO_MANY_NODES/POINTS 与 Service `:558-577` 一致；无候选 → `failed` 与 `:299` 一致。发布仅 parsed、示例 id、解析「仅 polygon / 非法类型记明细 / svg 默认」被源码证伪。
3. **未验证前提？** 把 M015-016 修复前的发布状态机和 M015-001 修复前的 slug id 写成当前响应。

### 4.3 `docs/p2-floor-plan-import-completion-audit-v0.1.md`

1. **为何必须存在？** 作为 P2-A 完成边界与验证入口（`00-current-effective-document-entry` 将其标为 S1 完成结论）。删掉则完成声明无处落。
2. **声称是否被执行？** 闭环「导入→候选→错误→人工发布→正式 shape」与现行服务路径一致。**完成数字**（33/528/181）与本席只读计数（75/3014/732）不符；「仅 parsed」过时。本席**未**重跑 `mvn test` 与 Semgrep，故不能把历史 160 输出当作当前事实（GEN-016 + verification-before-completion）。
3. **未验证前提？** 把一次历史 CI 计数写成永久当前事实。

### 4.4 `docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md`

1. **为何必须存在？** 原 TDD 编码计划；与契约重复处是浅副本。删掉计划本身不删 HTML 能力，但入口文档仍引用它。分类：细化（实现计划），不是添加。
2. **声称是否被执行？** §4.1 列出的新增 Java/SQL 均 EXISTS；§8 勾选 [x] 表示历史任务完成。L205 published 不可再发、L151「分页列表」、示例 `fpic_1` 过时。
3. **未验证前提？** 把勾选计划和旧状态机当作现行接口说明。

---

## 5. 顺从式：58 判据适用证据

每条均读锁定义全文，并核 HTML 原文行（BEQR `td.k` / BSM·PGM `h2/h3`）。本模块是文档，判据「实现、测试和文档须与锚点原文一致」在此落在**四文档声称**上。

### 5.1 GEN-001…018（通用）

| ID | 适用 | 结论 | 证据 |
|---|---|---|---|
| GEN-001 | 是 | 四文档有上位（PGM-023/BSM-028）；计划与契约部分重复，不单独立项删除 | 删除测试：删过时字段/错误码，接口回到现行 DTO |
| GEN-002 | 是 | 文档声称的入口须与可执行路径一致；字段/错误码/发布状态机不一致 → 缺陷 | GR-001/002/004/006 |
| GEN-003 | 是 | 文档模块不强制跑测；完成审计用测试计数作完成声明，计数不可复现 → GEN-016 | GR-007 |
| GEN-004 | 是 | 契约写了 payload 上限与节点/顶点上限，与服务一致 | Service `:558-577` |
| GEN-005 | 是 | 八端点权限与 Controller/V023/V034 一致；`import.manage` high+requires_approval=TRUE（V034:61） | 文档未写 requires_approval，登记观察 |
| GEN-006 | 是 | 计划/V034 FK、候选不直接写正式表，与实现一致；主键形式文档过时 | GR-005 |
| GEN-007 | 是 | 写接口幂等与文档一致（IdempotencyService） | Controller requireIdempotencyKey |
| GEN-008 | 是 | 文档 vs DTO/服务/OpenAPI/迁移枚举须一致；多处分叉 | GR-001/002/004/005/006 |
| GEN-009 | 是 | 错误矩阵缺现行码、发明不存在码 | GR-002 |
| GEN-010 | 是 | F-CROSS-10 上限在契约 L75–79 与代码一致；无秘密泄漏声称 | PASS 本条 |
| GEN-011 | 是 | 文档事件名与 Java `recordSuccess/Failure` 一致（`shape_saved` 下划线）；OpenAPI `floor_plan.shape.save` 点号为跨模块观察 | Java `:282` vs OpenAPI L3750 |
| GEN-012 | 是 | 2000/1000/1e6/发布 200 在契约或 OpenAPI；计划未写发布 200 | 观察 |
| GEN-013 | 是 | `verify:p0-contract` 脚本键存在于 package.json L43 | 完成审计「181 operation」过时 GR-007 |
| GEN-014 | 是 | V023/V034 存在；现行八值 CHECK 在 V423，完成审计只写 V034 | GR-009 |
| GEN-015 | 是 | PR #442 已把给人看的种类写成八种中文、导入源写成矢量图/标准数据；机内码仍 svg/standard_json | 与 SHAPE_TYPES/IMPORT_SOURCE_FORMATS 一致 |
| **GEN-016** | **是（本模块核心）** | **路径/字段/枚举/错误码/状态/示例 id/完成声明须可从当前范围复现** | **GR-001…009** |
| GEN-017 | 是 | 本席 findings 按冲突→细化→技术性→添加；无添加项 | §7 |
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
| BEQR-170 铺位租赁状态 | `:259` 空置/洽谈中/已租/即将到期/已退租/不可租 | 四文档用 shape/plan `active/inactive/archived`，未记载六态或着色 | **无冲突**（不同对象）；着色见 PGM-029/BSM-076，本 API 未承载 → §8 OBS-1，不发明字段 |
| BEQR-192 分红税务 | `:298` | 未声称 | 无冲突 |

### 5.3 BSM（17）

| ID | 锚点 | 本模块 | 结论 |
|---|---|---|---|
| BSM-009 | `:596` 基础管理卡片 | 平面图属基础管理空间能力 | 无冲突 |
| BSM-027 | `:694` h2 盘子管清楚 | 同上 | 无冲突 |
| BSM-028 | `:698` 主要功能；L700 项目/楼层/区域/点位 | P1 创建含 project_id/floor_no；导入发布正式区域 | 无冲突；project_id 必填与 002 对齐 |
| BSM-029 | `:714` 和谁有关 | 未发明票务/酒吧点位规则 | 无冲突 |
| BSM-072…074 | 商户管理 | 未声称商户专区 | 无冲突 |
| BSM-075…077 | 招商租赁；BSM-076 L3873 铺位看板六态着色 | 四文档未写铺位看板 | 看板≠本多边形 API；OBS-1 |
| BSM-114 | `:1827` 各类入口 | 文档权限码对应管理后台，未发明十一端 | 无冲突 |
| BSM-115 | `:1897` 基础管理连所有业务 | 平面图作为空间底图，未否定连结 | 无冲突 |
| BSM-116…119 | 上线加固/安装包 | 完成审计 Semgrep/测试是工程完成声明，不是上线加固业务发明 | GR-007 只打过时数字，不升上线规则 |
| BSM-120 | `:1957` 已确认规则 | 四文档未改写已确认清单 | 无冲突 |

### 5.4 PGM（15）

| ID | 锚点 | 本模块 | 结论 |
|---|---|---|---|
| PGM-007 | `:357` 合同和准入卡片 | 未声称合同卡口 | 无冲突 |
| PGM-016 | `:404` 应急与隐私卡片 | 未声称破窗 | 无冲突 |
| **PGM-022** | `:463` 空间和点位 h2 | P1 定位为标准地图数据源 | 无冲突 |
| **PGM-023** | `:467` 主要功能；**L472 导入句** | 导入闭环有文档；**「须二次确认」未入发布契约**；「仅 parsed」与分批发布冲突；八种中文已覆盖 HTML 三例并扩展为已定稿八值 | GR-004/008；八值不重开 013 |
| PGM-026 | `:492` 商户主要功能 | 未声称招商前链 | 无冲突 |
| PGM-027 | `:507` 商户业务边界 | 未开放商户看全局 | 无冲突 |
| PGM-028 | `:518` 合同准入 h2 | 未声称 | 无冲突 |
| PGM-029 | `:522`；L5567「铺位租赁状态…在平面图着色」 | 本契约无着色字段 | OBS-1，不立项新字段（避免添加） |
| PGM-030 | `:544` 业务意义 | 未声称 | 无冲突 |
| PGM-031 | `:555` 经销商 | 未声称 | 无冲突 |
| PGM-057…060 | 应急/隐私/会员 | 未声称 | 无冲突 |
| PGM-066 | `:951` 已拍板；含全中文、项目归属 | PR #442 八种中文/矢量图措辞与 PGM-066 全中文一致；project_id 必填与「对象带综合体项目归属」一致 | 示例 id 英文 slug 过时见 GR-005；无新冲突 |

顺从式收口：映射 58 项均已核对；核心失败在 GEN-016 叠加 PGM-023 发布口径。无锁外判据。

---

## 6. 对抗式（grilling 结构自问自答；不等操作员、不派子代理）

❓ **Q1 — 照 P1 契约写创建/保存请求，第一次正常调用会怎样？**
➡️ 带上文档必填 `operator_user_id`：OpenAPI FloorPlanCreateRequest `additionalProperties: false`（yaml `:32883`），契约字段不在 DTO（`FloorPlanCreateRequest.java` 无此字段）。带上 `evidence_attachment_ids` 同。缺这些字段则与「必填 operator_user_id」相反——实现不要求它。照 §6 处理「shape_id 不存在 → 404 FLOOR_PLAN_SHAPE_NOT_FOUND」：后端 **0 处**该码；`saveShapeOnce:261-270` 未知 id 当作新主键写入。第一次按文档做错误处理即错。→ GR-001/002

❓ **Q2 — 照 P2 示例响应解析导入任务 id，再用于后续 GET/publish？**
➡️ 示例 `"id": "fpi_floor-import-create-001"`。现行 `derivedId("fpi_", floorPlanId+"_"+key)` 为 `fpi_`+32 hex。用示例形态拼 id 会 404 `IMPORT_TASK_NOT_FOUND`（文档未列该码）。→ GR-005

❓ **Q3 — 第一次发布一部分候选后，再发剩余 pending？**
➡️ 文档：仅 parsed 可发，发完任务变 published。实现：parsed **或** published 均可继续发 pending（M015-016）。照文档会把第二次合法发布当成错误。→ GR-004

❓ **Q4 — SVG 里闭合 polyline、或 `data-shape-type=corridor`？**
➡️ 文档「仅解析 polygon」；Parser L18 匹配 polygon\|polyline，闭合折线进候选。非法种类文档说记错误明细；Parser 不校验闭集，Service `normalizeAllowed` 整单 `VALIDATION_FAILED`「图形类型取值无效」，无半写明细。→ GR-006

❓ **Q5 — 省略 `source_format`（文档写 svg 默认）？**
➡️ `validateImportCreateRequest` `requireText(source_format)`；OpenAPI required 含 source_format。省略 → VALIDATION_FAILED，无默认。→ GR-006

❓ **Q6 — 完成审计的 33/528/181 今日能否复现？**
➡️ 四测试类 `@Test|@ParameterizedTest` **75**；backend/src/test/java 合计 **3014**；openapi-p0.yaml `operationId` **732**。未跑 mvn/semgrep，故历史「通过」不能当当前 PASS。→ GR-007

❓ **Q7 — 发布是否满足 HTML「须二次确认」？**
➡️ 契约 §3.4 请求仅 idempotency_key/operator_note/candidate_ids；DTO 同。HTML L472 明文须二次确认。M015-011 已 XM 登记代码侧，本模块文档仍把无确认字段的体写成完整发布 API。不重开 011。→ GR-008

❓ **Q8 — 越权/并发/金额？**
➡️ 权限码与 Controller 一致，未发现文档把 view 写成可写。金额精度本模块无。幂等冲突码 `IDEMPOTENCY_CONFLICT` 由共享 IdempotencyService 提供，文档有列，与实现同码（跨模块服务，本席只核名称存在）。

❓ **Q9 — PR #442 是否已把本模块审完？**
➡️ 该提交只改八种中文与矢量图/标准数据措辞（4 文件 14+/11-）。发布状态机、示例 id、错误矩阵、完成数字、引用路径均未改。不能当 PASS。

---

## 7. Findings（临时 GR；B2 再分配永久 ID）

四分类顺序：冲突 → 细化 → 技术性 → 添加。下列均能指到 PGM-023/BSM-028 或 GEN 通用条，故 **细化或技术性**；无冲突（未与 HTML 明文唱反调到必须删除业务能力）；**无添加**。

### GR-M019-001

- **文件:行**：`docs/p1-floor-plan-polygon-api-contract-v0.1.md:45`、`:55`、`:63`、`:70`、`:72`
- **原文**：`evidence_attachment_ids` 否/string[]；`operator_user_id` **是**/操作人（创建与保存各一）；`z_index` **是**
- **更长上下文**：§4.1/§4.2 字段表把 P0 通用写模板字段列为平面图请求体。
- **正常触发**：按契约组 JSON 调用 `POST /api/v1/floor-plans` 或 `.../shapes`。
- **影响**：DTO `FloorPlanCreateRequest`/`FloorPlanShapeSaveRequest` 无这两字段；OpenAPI CreateRequest `additionalProperties: false` 且无这两键。`z_index` 服务 `:277` 空则 0。照文档必填操作人则与实现相反；照文档省 z_index 文档说非法、实现成功。
- **分类**：细化（PGM-023 平面图 API 下位字段）
- **工程缺陷**：是
- **冻结 ID**：GEN-016 / GEN-008 / PGM-023
- **依据类型**：4（`ls`/读 DTO）+ 现行源码只读；HTML 未要求 operator_user_id
- **最小拟处置**：删 §4.1/§4.2 的 `evidence_attachment_ids`、`operator_user_id` 四行；`z_index` 改为否、缺省 0。不改 Java。
- **无业务发明**：删文档幽灵字段，不增业务概念。

### GR-M019-002

- **文件:行**：`docs/p1-floor-plan-polygon-api-contract-v0.1.md:94-95`（及矩阵 L85–95 缺行）
- **原文**：`标注不存在 | 更新时 shape_id 查询不到 | 404 | FLOOR_PLAN_SHAPE_NOT_FOUND | 拒绝更新`；幂等冲突 409 `IDEMPOTENCY_CONFLICT`
- **上下文**：矩阵只列 VALIDATION_FAILED / FLOOR_PLAN_NOT_FOUND / FLOOR_PLAN_SHAPE_NOT_FOUND / IDEMPOTENCY_CONFLICT
- **正常触发**：更新时带已删除或不存在的 `shape_id`。
- **影响**：`rg FLOOR_PLAN_SHAPE_NOT_FOUND backend` = **zero**。`saveShapeOnce:262-270`：查不到则 **用该 shape_id 新建**；仅当查到但 `floorPlanId` 不符才 400 `VALIDATION_FAILED`「图形不属于该平面图」。现行另有 `PROJECT_NOT_FOUND`（`:512`）、`CROSS_PROJECT_FORBIDDEN`（`:517/:610`）、`CROSS_MERCHANT_FORBIDDEN`（`:626`）未入矩阵。按文档等 404 会落到 200 新建。
- **分类**：细化
- **工程缺陷**：是
- **冻结 ID**：GEN-016 / GEN-009 / GEN-008 / PGM-023
- **依据类型**：4（rg 零命中）+ 源码 L261-270
- **最小拟处置**：删除 `FLOOR_PLAN_SHAPE_NOT_FOUND` 行，改为现行「未知 shape_id 按该 id 创建；他图 shape_id → 400 VALIDATION_FAILED」；补 PROJECT_NOT_FOUND / CROSS_PROJECT_FORBIDDEN / CROSS_MERCHANT_FORBIDDEN 三行（中文与服务一致）。不重开 M015-002/003。

### GR-M019-003

- **文件:行**：`docs/p1-floor-plan-polygon-api-contract-v0.1.md:127-130`
- **原文**：`docs/master-roadmap.md#阶段-8-市场基准增强为-p1p2-演进`；`docs/p0-user-story-map-and-sprint-slices-v0.1.md#第二期-p1`；`backend/src/main/resources/db/migration/V001__initial_schema.sql`
- **正常触发**：按引用打开文件/锚点。
- **影响**：`ls V001__initial_schema.sql` → MISSING；P0 轻量 `polygon_points` 在 `V001__baseline.sql:64`。`master-roadmap.md` 标题为 `### 阶段 8：市场基准增强与 SaaS 化深度能力`（L217），fragment 字符串 0 命中。`p0-user-story-map` 无标题「第二期-p1」（仅表行「第二期 / P1」L26）。
- **分类**：技术性（无业务发明：修复链接/文件名）
- **工程缺陷**：是
- **冻结 ID**：GEN-016
- **依据类型**：4（ls + 读标题行）
- **最小拟处置**：V001 改为 `V001__baseline.sql`；两处 Markdown 锚点改为真实标题或去掉 fragment。
- **无业务发明**：只修引用。

### GR-M019-004

- **文件:行**：`docs/p2-floor-plan-import-api-contract-v0.1.md:151-153`；`docs/p2-floor-plan-import-completion-audit-v0.1.md:18`；`docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md:205`
- **原文**：
  - P2：「仅 `parsed` 状态的导入任务允许发布。」「发布后…任务状态变为 `published`。」
  - 完成审计：「仅 `parsed` 任务和 `pending` 候选允许发布」
  - 计划：「写失败测试：failed/cancelled/**published** task 不允许重复发布。」
- **正常触发**：第一次发布子集候选后，再对同一任务发布剩余 pending（M015-016 的正常复核路径）。
- **影响**：服务 `:366-368` 接受 parsed **或** published；failed/cancelled 仍拒。照文档会拒绝第二次合法发布。不重开 M015-016。
- **分类**：细化（PGM-023「人工复核后发布」的下位状态机）
- **工程缺陷**：是
- **冻结 ID**：GEN-016 / PGM-023 / GEN-008
- **依据类型**：4（读服务 L366-368）+ 类型 1（L472 复核发布，未禁止分批）
- **最小拟处置**：三处改为「任务状态 ∈ {parsed, published} 且所选候选均为 pending；failed/cancelled 拒绝」。计划 L205 勾选描述随改。

### GR-M019-005

- **文件:行**：`docs/p2-floor-plan-import-api-contract-v0.1.md:56`、`:106-111`；计划 `:139`、`:173`
- **原文**：响应 `"id": "fpi_floor-import-create-001"`；详情示例 `"id": "fpi_1"`、`"id": "fpic_1"`；计划 `candidate_ids: ["fpic_1", "fpic_2"]`
- **正常触发**：把示例 id 当作可拼出的主键形态做客户端或测试夹具。
- **影响**：`derivedId` `:664-665` 为前缀+SHA-256 前 32 位（M015-001）；候选 `:318` `fpic_`+slug(taskId+"_"+n)，taskId 已是 `fpi_`+32hex。示例形态不可从当前实现复现。不重开 001。
- **分类**：细化
- **工程缺陷**：是
- **冻结 ID**：GEN-016 / GEN-008
- **依据类型**：4（读 derivedId）
- **最小拟处置**：示例改为「`fpi_` / `fps_` / `fpic_` + 32 位 hex（由平面图 id 与幂等键派生）」并给一则说明，或标「示意，非真实 id 形态」。

### GR-M019-006

- **文件:行**：`docs/p2-floor-plan-import-api-contract-v0.1.md:70`、`:160-165`；完成审计 `:13`；计划 `:29`
- **原文**：`source_format`「矢量图（`svg`，**默认**）」；「仅解析 `<polygon>`」；「不在八种内的记入错误明细，不生成候选」
- **正常触发**：(a) 省略 source_format；(b) 只含闭合 polyline 的 SVG；(c) `data-shape-type="corridor"`。
- **影响**：
  - (a) `requireText(source_format)`，无默认 → VALIDATION_FAILED。
  - (b) Parser `SVG_SHAPE_PATTERN` L18 `(polygon|polyline)`，闭合折线生成候选（L79-83 仅未闭合才 `POLYLINE_NOT_CLOSED`）。计划 L29 写解析 polygon **和** polyline，与契约「仅 polygon」互相矛盾；实现与计划更近。
  - (c) Parser L96 原样采用 data-shape-type；Service `:321` `normalizeAllowed(..., SHAPE_TYPES)` 整单失败，**不**落错误明细。corridor 不在八值（与 M015-013/M016-002 一致）。
- **分类**：细化
- **工程缺陷**：是
- **冻结 ID**：GEN-016 / GEN-008 / PGM-023
- **依据类型**：4（读 Parser L18-83、Service L321/555）
- **最小拟处置**：契约改为与 Parser 一致（解析 polygon 与已闭合 polyline）；非法 shape_type 改为「整单 VALIDATION_FAILED，不生成候选/错误行」或（若保持文档）须改代码——B1 只改文档则以前者对齐现行；删「svg 默认」，标明必填。计划 L29 已较准，保留。不把 corridor 写入八值。

### GR-M019-007

- **文件:行**：完成审计 `:51-54`；P2 `:211`；计划 `:151`
- **原文**：聚焦测试「共 **33** 个测试通过」；全量后端「**528** 个测试通过」；P0 契约「**181** 个 operation 仍同步」；P2「后续完成前仍需运行全量…」；计划「返回：导入任务**分页**列表」
- **正常触发**：把完成审计当当前门禁基线；或按「分页」实现页码参数。
- **影响**：本席只读计数：四测试类 15+14+7+39=**75** `@Test`；`backend/src/test/java` **3014**；openapi `operationId` **732**。`listImportTasks` 返回 `PageResponse(records,1,max(size,1),size)`（M015-007 诚实不分页）。P2 L211 与完成审计「已通过」自相矛盾。本席未跑 mvn/semgrep，故 Semgrep「0 findings」亦不可当作当前事实。
- **分类**：技术性（完成计数/分页信封，无业务发明）
- **工程缺陷**：是
- **冻结 ID**：GEN-016 / GEN-003 / GEN-013
- **依据类型**：4（python/rg 计数；读 Service L139-152）
- **最小拟处置**：完成审计验证表改为「以当时 160 记录为准，数字须重跑后回写」或删具体 33/528/181，改引用现行测试类名；P2 §6 删「后续仍需」或与完成审计统一为「非当前门禁」；计划「分页列表」改为「列表（PageResponse 信封，单页全量）」。
- **无业务发明**：修过时工程数字。

### GR-M019-008

- **文件:行**：`docs/p2-floor-plan-import-api-contract-v0.1.md:139-147`；完成审计 `:18`；计划 `:167-174`
- **原文**：发布请求体仅 `idempotency_key` / `operator_note` / `candidate_ids`
- **HTML 原文**（PGM-023 / 治理地图 L472）：「发布为高危动作，**须二次确认**」
- **正常触发**：只按契约 POST publish，无确认字段。
- **影响**：DTO `FloorPlanImportPublishRequest` 三字段，与契约一致，与 HTML 不一致。M015-011 已把**代码/OpenAPI/前端**登记为 XM 同批，修复队列否。本模块文档仍把无确认的体写成完整发布 API，读者会以为 HTML 二次确认已由契约覆盖。不重开 M015-011。
- **分类**：细化（遗漏 HTML 明文下位）
- **工程缺陷**：是（文档假完成，非新业务发明）
- **冻结 ID**：PGM-023 / GEN-016
- **依据类型**：1（HTML L472）+ 4（读 DTO/契约）
- **最小拟处置**：在 §3.4 规则增一句「发布为高危，须二次确认；确认字段与 OpenAPI 同批（见 M015-011），本文尚未列入请求体」——或等 011 落地后补字段。B1 不改代码。

### GR-M019-009

- **文件:行**：完成审计 `:23`；计划 `:49`、`:71-122`；对照 `V034:36` 与 `V423:5-8`
- **原文**：完成审计「数据库：新增 V034 迁移，包含…约束」；计划数据结构只列 V034。PR #442 后文档声称八种候选。
- **正常触发**：只读 V034 CHECK 实现八值。
- **影响**：V034 `chk_floor_plan_import_candidates_shape_type` 仍为历史六值；现行八值在 `V423__m016_floor_plan_and_space_type_checks.sql` L5–8。不重开 M016-002。
- **分类**：细化
- **工程缺陷**：是
- **冻结 ID**：GEN-016 / GEN-014 / GEN-008
- **依据类型**：4（读两则 SQL）
- **最小拟处置**：完成审计/计划加一句「shape_type 八值 CHECK 见 V423（V034 原六值已被其后迁移替换）」；不改 V034 历史文件。

---

## 8. 观察（不入修复队列）

| ID | 内容 | 为何不立项 |
|---|---|---|
| OBS-1 | PGM-029 L5567 / BSM-076 L3873 / BEQR-170：铺位租赁六态在平面图着色。四文档 shape.status=active/inactive/archived，无六态字段 | 着色是铺位看板/合同状态叠加，不是本多边形 API 的 shape_type。HTML 未要求把六态写入 floor_plan_shapes。立项会变成添加字段。登记以免与 BEQR-170 误冲突 |
| OBS-2 | OpenAPI `x-audit-event-success: floor_plan.shape.save`（点号）vs Java/P1 `floor_plan.shape_saved` | OpenAPI 属别模块；本契约与 Java 一致 |
| OBS-3 | OpenAPI FloorPlanCreateRequest `project_id`「可为空」且不在 required；服务必填 | 契约 P1 L47 已必填，与 M015-002 对齐；OpenAPI 漂移不入本队列 |
| OBS-4 | OpenAPI `plan_name` maxLength 120 vs V023 VARCHAR(160) 与 P1「1-160」 | 文档与库一致；OpenAPI 跨模块 |
| OBS-5 | OpenAPI saveShape `additionalProperties: true`、缺 polygon 必填 | 跨模块 |
| OBS-6 | P1 L15「本次不实现 CAD/SVG」是 **P1 切片**范围，同模块 P2 已定义导入；不把切片历史句当产品级假完成 | 有 P2 契约并列，读者有导入口径 |
| OBS-7 | `import.manage` requires_approval=TRUE 文档未写 | 权限表写了「高」；审批闸是权限种子细节，不升 finding |
| OBS-8 | source_payload 列类型 JSONB，JDBC `writeJson` 把 SVG 文本序列化成 JSON 字符串再 CAST | API 仍收 SVG 文本；属实现，文档不必改 |
| OBS-9 | 计划与 P2 契约大段重复 | ponytail-review 可删计划中已迁入契约的接口表；非正确性缺陷 |
| OBS-10 | `verify:p0-contract` 键存在；本席未跑该 npm 脚本 | 文档模块不强制；完成数字已用 operationId 计数证伪 |

---

## 9. ponytail-review（只列可删，不落地）

- p1:L45,L63: `delete:` `evidence_attachment_ids` 两行。实现与 OpenAPI 皆无。Replacement: nothing.
- p1:L55,L72: `delete:` `operator_user_id` 两行。
- p1:L94: `delete:` `FLOOR_PLAN_SHAPE_NOT_FOUND` 行。Replacement: 现行 VALIDATION_FAILED 句。
- p2:L56: `shrink:` 示例 id 改派生形态说明，删不可复现的 `fpi_floor-import-create-001`。
- net: 文档可净减少约 8–12 行幽灵字段/错误码，另改若干过时句。不增业务字段。

---

## 10. PASS/FAIL

单席 PASS 条件（runbook §9）：全文件覆盖 ✓；58 判据已核 ✓；三线已跑 ✓；声称有四类证据 ✓；无未答 questions ✓；**无未定稿缺陷 ✗**。

**本席结论：FAIL**

建议入 B2 修复队列（工程缺陷是）：GR-M019-001 … GR-M019-009。添加项：无。HTML 无需人闸修改。

未跑：`mvn test`、`npm run verify:p0-contract`、Semgrep（文档模块豁免；完成声明因此不能当 PASS）。子代理失败重跑次数：0。MCP：未用。

## M019 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 40410 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6/xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（铁律共 **7** 条）；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M019 FILES 4 / 锁映射 58 / 计划冻结 680 行，现文 683 行）
- 正本：`/home/james/complex-management`。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `1d84ec363c8b4c9fca15970583affb5c3346d24c`。本模块 4 文档指纹与 B1 覆盖表全等。B3=0。无源修复。三份 HTML 现文：治理地图 V0.12、业务地图 V0.14、枚举 V0.07。
- 席甲：Claude Fable 5.1，作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类。本席只读 `M019-b1-self-raw.md` 前 20314 字节（V0.01）；其后 V0.02 B2 结论段不作为本票依据。
- 本席 B2-r1：**PASS**（001–009 逐 ID 全 PASS；008 不并入 M015-011。添加 0、不发 2.2。非阻断勘误见 003 迁移文件名以 grep 为准、007 门禁名用仓内既有 `verify:predeploy`，不否决）
- 正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 `M019 → [...]` COUNT=58 unique=58。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。本步只读哈希/行号/两份 B1 前缀/候选段/HTML L469·L472·L700/M015-011 定稿行；未对任一 `complex_test*` 库做写操作，未重审四文档、未跑 mvn/semgrep。

此票只判断 findings.md V2.59 顶部「当前 M019 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。不加判据。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M019-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 40410 | `32adea173eb78bfcd6f907c1ef3d43ce1089e0a5b6fb1a283b2dccbd09d59646` |
| `docs/audit-2026-09/modules/M019-b1-self-raw.md` 前缀（席甲 B1；本席只读此前缀） | V0.01 | 20314 | `6f60c608d38970dea0126e3531dd9456485693ff9f85f33b57124d315c8dfd60` |
| `docs/audit-2026-09/findings.md` | V2.59 | 1017274 | `6c64c3d2ba731d0029a8c4c82221d03a838d4094573afdb224d443bfe3ea39fb` |

席甲 raw 全文现大于 20314 bytes；本席按指令只取前 20314 字节，不读其后 V0.02 B2 段。本文件独占锁哈希字段行仍仅文首一处（B1 正文提及该字段名的说明句不算新独占行），本段未新增。

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

上位句（本席 B2 只核分类所需原文，不重审模块）：

- 治理地图 L469：`维护楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位。`
- 治理地图 L472：`商场平面图网页导入：在后台上传或导入商场平面布局图，系统解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误后发布到正式平面图；发布为高危动作，须二次确认——发布会写入正式平面图区域，不可静默覆盖既有布局证据。`
- 业务地图 L700：`建立综合体项目、楼层、区域、点位；当前一个综合体，未来可扩展为多综合体。`

`questions/M019*`：不存在。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核差集/分类/队列/最小边界再接受或反驳；禁止表演性同意；B2 不修复。本批重点：008 是否并入 M015-011、002 矩阵两分行为、006 整单拒绝、007 加快照说明 |
| grilling | 只借追问结构自查：差集有无漏并/改写、003/007 技术性是否指得出、其余细化上位、008 应否并入 011、有无应升添加 |
| verification-before-completion | 声称前先跑只读哈希/前缀字节/HTML 行号/M015-011 定稿行 |
| systematic-debugging / diagnosing-bugs | 只借读错→对照两 raw 与候选；不改生产代码、不重审四文档 |
| codebase-design / ponytail-review | 本模块深度落在四文档对齐现行 DTO/服务/迁移闭集；最小是删幽灵字段、改过时句、加引用，不发明业务规则 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| graft | 本步对象是审计文档与 HTML 行，未查源码图 |

### 完整差集（GR-M019-001…009、OBS-1…10、席甲 CX-M019-001/002、OBS-1…5）

漏并：无。改写原意：无。两席 B1 全部临时缺陷 ID 仍落在 001–009。无单席独有缺陷被丢。无本批新开「否」。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M019-001 | GR-M019-001 + CX-M019-001（字段部分） | 细化/是/是 | **PASS（细化/是/是；含本席 z_index 缺省 0，席甲 B1 未单列）** |
| M019-002 | GR-M019-002 + CX-M019-001（错误矩阵部分） | 细化/是/是 | **PASS（细化/是/是；矩阵改写用现行两分行为，不采用席甲单行 400）** |
| M019-003 | GR-M019-003 + CX-M019-001（引用部分） | 技术性/是/是 | **PASS（技术性/是/是；含本席两处锚点。非阻断：所在迁移文件名以 grep 为准）** |
| M019-004 | GR-M019-004 + CX-M019-002（发布规则部分） | 细化/是/是 | **PASS（细化/是/是；含本席计划 L205，不重开 M015-016）** |
| M019-005 | GR-M019-005 + CX-M019-002（示例 id 部分） | 细化/是/是 | **PASS（细化/是/是；派生形态 + 示意，不重开 M015-001）** |
| M019-006 | GR-M019-006 + CX-M019-002（解析规则部分） | 细化/是/是 | **PASS（细化/是/是；整单拒绝＝现行 normalizeAllowed，不改解析器/服务）** |
| M019-007 | GR-M019-007（席甲 B1 只记 OBS-1） | 技术性/是/是 | **PASS（技术性/是/是；加快照说明不删数字。升队合法）** |
| M019-008 | GR-M019-008（席甲 OBS-5 同根 M015-011） | 细化/是/是 | **PASS（细化/是/是；不并入 M015-011，本模块新开 ID）** |
| M019-009 | GR-M019-009 | 细化/是/是 | **PASS（细化/是/是；一句引用 V423，不改 V034、不重开 M016-002）** |

冲突入队 0。细化入队 7（001、002、004、005、006、008、009）。技术性入队 2（003、007）。观察否：双方 OBS 按候选「只登记不入队」（席甲 OBS-1/5 已分别落入 007/008）。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001–009（9）；「否」本批新开 = 0。

### 一、差集核实

| B1 临时 ID | 候选落点 | 核实 |
|---|---|---|
| GR-001 + CX-001 字段 | 001 | 同根：`evidence_attachment_ids` / `operator_user_id` 幽灵字段。本席另有 `z_index` 标必填而服务缺省 0，候选写入 001，未丢。席甲 CX-001 把字段/矩阵/引用捆成一条，按本席粒度拆入 001/002/003，原意未改写。 |
| GR-002 + CX-001 矩阵 | 002 | 同根：`FLOOR_PLAN_SHAPE_NOT_FOUND` 后端 0 处；缺 `PROJECT_NOT_FOUND` / `CROSS_*`。最小处置取本席两分行为（未知 id 新建；他图 id 400），未采用席甲把「不存在」也写成 400 拒绝——后者会改写现行 `saveShapeOnce` 语义。不重开 M015-002/003。 |
| GR-003 + CX-001 引用 | 003 | 同根：`V001__initial_schema.sql` 不存在。本席另有 roadmap / 故事图 fragment 失效，候选写入 003，未丢。席甲拟改 `V005__…`、本席拟改 `V001__baseline.sql`：候选「所在迁移以 grep 为准」不裁定业务，不改写「引用失效」原意。 |
| GR-004 + CX-002 发布 | 004 | 同根：仅 parsed 可发 vs M015-016 parsed\|published。本席计划 L205「published 不允许重复发布」候选写入 004。不重开 M015-016。席甲 CX-002 捆发布/示例/解析，按本席粒度拆入 004/005/006。 |
| GR-005 + CX-002 示例 id | 005 | 同根：`fpi_floor-import-create-001` 等不可复现。本席另有 `:106/:111` 与计划 `:173`，候选写入 005。不重开 M015-001。 |
| GR-006 + CX-002 解析 | 006 | 同根：仅 polygon、非法种类记明细。本席另有 `svg` 默认、完成审计「支持 polygon」，候选写入 006。计划 L29 本席 B1 已准、席甲 OBS-2 亦认与解析器一致，候选不改计划 L29，未漏亦未扩。 |
| GR-007（席甲 OBS-1） | 007 | 本席独有。席甲 OBS-1 同根（33/528/181 无日期快照，席甲「登记不改」）。升队合法：本席 B1 已立工程缺陷，不是把观察偷偷升添加。 |
| GR-008（席甲 OBS-5） | 008 | 本席独有：P2 §3.4 缺 L472「须二次确认」明文句。席甲 OBS-5 写「未发现须回 M015/M017 的实现缺陷（文档过时是文档侧）」——与「不重开 011、本模块补文句」同根，不是把 011 的代码/OpenAPI/前端 XM 拉进本批。 |
| GR-009 | 009 | 本席独有：V034 仍六值 CHECK，八值在 V423。席甲 B1 未立。候选原样入队。 |

观察未入队（本席核：无应升添加、无应升本批缺陷被降）：席甲 OBS-2/3/4（计划历史勾选、F-CROSS-10 一致、PR #442 文档部分未合并 origin/main）；本席 OBS-1…10（铺位六态着色不入本 API、OpenAPI 事件名/plan_name 长度/saveShape additionalProperties 属别模块、P1 L15 切片范围句、requires_approval 未写、JSONB 存储、计划与契约重复、未跑 verify:p0-contract）。席甲 OBS-1/5 已分别落入 007/008，不在观察里留应升项。

### 二、分类

2.1 顺序：冲突 → 细化 → 技术性 → 添加。能指到 HTML 上位的一律先归细化，不进技术性。无冲突（四文档未与 L469/L472/L700 明文唱反调到必须删除业务能力）。无添加。

**003 技术性（非细化）**：失效文件名与 Markdown fragment。HTML 未要求 `V001__initial_schema.sql` 或该两处锚点。修引用、无业务发明。票：**PASS（技术性）**。

**007 技术性（非细化）**：完成计数/P2「仍需运行」自相矛盾/计划「分页列表」信封。修工程数字与分页措辞，无业务发明。L472 不规定测试个数。票：**PASS（技术性）**。

**001 细化上位**：L469「维护楼层、区域、点位…」+ L472 平面图导入的底图承载 + L700「建立…楼层、区域、点位」。P1 字段表是该能力的下位契约。幽灵字段/必填 z_index 与现行 DTO 分叉是 GEN-016，不是新实体。票：**PASS（细化）**。

**002 细化上位**：同一 P1 API 的错误矩阵，下位仍是 L469/L472 平面图维护。补 M015-002/003 已定稿码、改写不存在码，不新开错误族。票：**PASS（细化）**。

**004 细化上位**：L472「由人工复核候选与错误后发布」。parsed\|published 分批发布是该复核路径的下位状态机（M015-016 已定稿）。票：**PASS（细化）**。

**005 细化上位**：L472 导入链路的任务/候选主键形态（M015-001 已定稿派生规则）。示例不可复现＝GEN-016，不是新编号规则。票：**PASS（细化）**。

**006 细化上位**：L472「系统解析出候选区域」。polygon/polyline、source_format 必填、非法种类整单拒绝，均为现行解析器/服务口径的下位。票：**PASS（细化）**。

**008 细化上位**：L472 明文「发布为高危动作，须二次确认」。契约 §3.4 未写该句＝遗漏 HTML 明文下位，不是与 HTML 相反（非冲突），也不是把确认字段做成新业务（非添加）。票：**PASS（细化）**。

**009 细化上位**：L472「候选区域（铺位、摊位、公共区）」经 M016-002 收成八值 CHECK。完成审计/计划只点名 V034（仍六值）使该闭集不可从所称迁移复现。加一句指向 V423 是该闭集的下位引用，不是新种类。票：**PASS（细化）**。

### 三、008 是否应并入 M015-011（结论：否，新开 M019-008）

M015-011 定稿（findings 历史行，队列否/XM）：跨模块同批给 OpenAPI `FloorPlanImportPublishRequest`、前端 `FloorPlanImportPage` 发布体、后端 DTO/服务加确认字段；单独加字段会使现网前端 400。那是**代码/OpenAPI/前端 XM**。

本条是**本模块 P2 契约文档**缺 L472 明文句。本席 B1 GR-008 最小拟处置已是 §3.4 加一句并指向 011、不改请求体、不重开 011。候选同此。

先例对照：M018 GR-07 并入 011，因为 M018 就是 011 的前端 XM 面。M019 是文档面：把 008 并进 011 会使本模块 GEN-016 空洞一直挂到 XM 落地（011 队列已否）。一句「须二次确认；服务端确认字段与 OpenAPI/前端同批落地（M015-011），本文尚未列入请求体」不实现 011、不改代码。

票：**PASS（不并入 011，新开 008）**。

### 四、最小处置

全部只改本模块 4 文档（p1 契约、p2 契约、完成审计、计划），不改代码/OpenAPI/迁移/HTML，不发明业务规则。

**002 矩阵改写用词 —— 同意现行两分行为。** 未知 `shape_id` 按该 id 新建、他图 id → 400 `VALIDATION_FAILED`「图形不属于该平面图」，补三行现行码。席甲 B1 把「不存在」也写成 400 拒绝，会把现行 upsert 写成拒绝规则，等于发明。候选取本席口径。不改 `saveShapeOnce`。票：**PASS**。

**005 示例 id 形态 —— 同意派生形态 + 示意。** 改为前缀 + SHA-256 前 32 位示意，并加「id 由平面图 id 与幂等键派生，示意」。不把可拼 slug 写成客户端算法，不重开 M015-001。候选 `fpic_` 亦「示意」即可（候选 id 实为 `fpic_`+slug(taskId_n)，示意句已覆盖近似）。票：**PASS**。

**006「整单拒绝」措辞 —— 同意现行口径。** `source_format` 必填无默认；解析 `<polygon>` 与首尾闭合 `<polyline>`；不在八种内 → 整单 400 `VALIDATION_FAILED`，不落任务/错误明细。这是 `normalizeAllowed` 现行行为，不是新拒绝规则。不改解析器/服务、不把 corridor 写入八值。完成审计「支持 polygon」随改。票：**PASS**。

**007 完成快照 —— 同意加快照说明而非删数字。** §5 表前加「2026-06 编码完成时 160 记录快照，非当前门禁」；P2 §6 末句改以仓内既有 `verify:predeploy`（含本席 B1 写过的 `verify:p0-contract`）为准；计划 :151 改「列表（PageResponse 信封，单页全量）」。不把 33/528/181 改成现行 75/3014/732（那要重跑，B3 文档修复不强制），不删历史数字。非阻断：`verify:predeploy` 是 `package.json` 既有键，不是新门禁名。票：**PASS**。

**009 一句引用 V423 —— 同意。** 完成审计/计划各加「shape_type 八值 CHECK 见 V423（V034 原六值已被其后迁移替换）」。不改 V034 历史文件、不重开 M016-002。票：**PASS**。

**001 字段 —— 同意。** 删四行幽灵字段；`z_index` 改「否，缺省 0」；标题 V0.1→V0.2 + 文首一行。只改 p1 契约。不改 Java/OpenAPI。票：**PASS**。

**003 引用 —— 同意。** 文件名改为实际存在的迁移（grep 为准）；两处锚点改真实标题或去掉 fragment。不发明新迁移。票：**PASS**。

**004 发布 —— 同意。** 三处改为「任务 parsed 或 published 且所选候选均 pending；failed/cancelled 拒绝」；计划勾选描述随改。不重开 M015-016。票：**PASS**。

**008 二次确认句 —— 同意。** §3.4 规则加一句「发布为高危动作，须二次确认；服务端确认字段与 OpenAPI/前端同批落地（M015-011），本文尚未列入请求体」。不改请求体、不改代码、不重开 011。票：**PASS**。

一次 B3 允许改的文件：上述 4 文档。禁止：FloorPlanService/Parser/Controller/DTO、OpenAPI、V023/V034/V423、三份 HTML、锁、台账、测试、前端。同文件多 ID 版本铁律只前进一次。

### 五、添加 / HTML / questions

本批无添加项、无 HTML 申请、无 questions。本席逐条核：001 删字段不是新业务；002 不新增错误族；005 不发明主键算法；006 不发明解析种类；008 不把确认字段写入请求体；009 不改 CHECK。Grok OBS-1 铺位六态着色若立项会变成添加字段——候选正确不升。无应升 2.2 的项。不 BLOCKED-ASK。

### 分类 / 队列 / 允许的最小修复边界

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3 只改本模块 4 文档，不改代码/OpenAPI/迁移/三份 HTML/锁。

- 001：删 `evidence_attachment_ids`/`operator_user_id` 四行；`z_index` 否、缺省 0；p1 版本 + 文首一行。
- 002：该行改为未知 id 新建 / 他图 id 400；补三行现行码。不重开 M015-002/003。
- 003：迁移文件名改实际存在者；两处 fragment 改真实标题或去掉。
- 004：三处发布规则改 parsed\|published + pending；计划 L205 随改。不重开 M015-016。
- 005：示例改派生形态 + 「示意」句。不重开 M015-001。
- 006：三句 + 完成审计一句改现行口径（必填 source_format、polygon+闭合 polyline、整单 400）。不改解析器。
- 007：完成审计加快照说明，不删数字；P2 §6 改门禁句；计划列表改 PageResponse 单页全量。
- 008：§3.4 加二次确认句并指向 M015-011。不改请求体。
- 009：两处各加一句 V423。不改 V034。

禁止：改 Java/OpenAPI/迁移/HTML、把未知 shape_id 写成 400 拒绝、把确认字段列入请求体、删完成审计历史数字、把 corridor 写入八值、重开 M015-001/002/003/011/016 或 M016-002。

### 结论

全批差集无漏并、无原意改写。CX-001 拆入 001/002/003、CX-002 拆入 004/005/006；本席独有 007/008/009；席甲 OBS-1→007、OBS-5→008。003/007 技术性、其余细化，L469/L472/L700 上位指得出。008 不并入 M015-011。最小处置只改本模块 4 文档，002/005/006/007/009 用词无业务发明。添加 0。逐 ID：001 PASS、002 PASS、003 PASS、004 PASS、005 PASS、006 PASS、007 PASS、008 PASS、009 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M019 IN_PROGRESS；B3=0。
