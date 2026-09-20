CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M019 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙（grok-4.6 / xhigh）
计划：160 审计 V0.17 B4 与 §5 闸 3
本文件路径：`docs/audit-2026-09/modules/M019-b4-r1-grok-raw.md`
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

模块：M019 / 共 471，B3 第 1 次之后的 B4。计划名：空间点位、楼层平面图与营业时段／文档与接口定义／第1段。
修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。席甲为作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。

未读取：任何 `M019-b4-r1-self-raw.md`（落盘时该路径已存在，本席按独立性禁令整文件不打开）。已读：`160-audit-prompt.md` V0.17 B4 与 §5 闸 3、`findings.md` V2.60 顶部「当前 M019 B2-r1 双PASS定稿」、本席旧 `M019-b1-grok-raw.md` V0.01/V0.02、冻结 diff 与两 tree、现行 `FloorPlanService`/`FloorPlanImportParser`/dto、`V005`/`V034`/`V423`、`docs/openapi/openapi-p0.yaml`。ledger 只读取模块状态与 B3 次数；其中 after tree 之后写入的席甲 B4 封存行不构成本席证据。本票以两 tree 全文 diff、定稿最小处置、只读探针与本席复跑为准。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；M019-001…009 是否按 findings V2.60 定稿最小处置实现；是否最懒可行（ponytail ultra）；是否引入新缺陷或新业务发明；每一变更行能否归因到 001–009；Markdown 表格列数是否完整；是否误改历史计划原文之外内容；是否越界改后端 / OpenAPI / 三份 HTML / 锁 / sidecar / findings / ledger。文档模块不强制 TDD。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例，未派子代理。

---

## 0. 技能预检与节点应用（本环境实核）

本席按 `160-audit-prompt.md` V0.17 §6 在 **本机路径** 核验，不抄 Codex 预检表。grilling 只借追问树自问自答（事实自己读）。Arc `audit` 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，**不执行**。`requesting-code-review` 本体的 `HEAD~1` / 同会话子代理复审 / 无 ID 即刻修，一律不用——本席自身即 B4 外审。`receiving-code-review` 本节点不适用（本席产出意见，不收意见去修）。`test-driven-development` / mattpocock `tdd`：文档内容修复不强制落测试。`resolving-merge-conflicts`：工作区无进行中 merge/rebase，节点不适用。`writing-for-agents`：本 raw 按完成准则写给下游合取席读。`git-guardrails-claude-code`：存在；本席未 push/reset/clean。`i-have-adhd`：存在；与「raw 第一行必须是独占锁行」冲突时，160 提示词优先。`brainstorming` / `writing-plans`：阶段 A 技能，B4 不适用。`ponytail`：B3 默认强度，B4 只审不修。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由识别；**不执行** Arc 全量审计工作流 | 本体要派评审子代理，记缺口 |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `graft grep` 定位 `saveShapeOnce`/`derivedId`/`normalizeAllowed`/三码/`POLYLINE_NOT_CLOSED`/`saveImportTask` | 无 |
| `grilling` / `grill-me` | mattpocock `productivity/grilling/SKILL.md`；`grill-me` 仅转发 | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `batch-grill-me` | — | — | **本机技能树不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | 四文档是 FloorPlan 深模块的浅接口；本 diff 把过时字段/错误码/发布规则从接口上删掉，深度回到现行 DTO/服务闭集 | 不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环=只读命令（三工具 sha256、`cmp` 冻结 diff、`rg` 剩余幽灵字段/不存在错误码、python 表格列数、读 V005:64） | 文档模块不跑 Maven |
| `systematic-debugging` | superpowers 同名 | 只借读错误→追数据流（改写声称→DTO→Service→Parser→V005/V034/V423→OpenAPI）；作废插桩 | 无 |
| `verification-before-completion` | superpowers | PASS 前本席实跑下列命令；不声称 mvn/semgrep 已跑 | 无 |
| `requesting-code-review` | superpowers | B4=本席即外审；基线=两 tree id 而非 `HEAD~1`；不派子代理 | 本体子代理路径作废 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删的越界添加 | 不落地 |
| `long-running-background-tasks` | grok bundled | 本席无后台作业 | 节点不适用 |

graft 本席调用节省合计约 62300 tokens（`derivedId`/`POLYLINE_NOT_CLOSED`/`saveShapeOnce`/三码/`normalizeAllowed`/`saveImportTask`）。

---

## 1. 输入与独立锁实算

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。文件 889020 字节。sidecar 一行同值。ledger 顶部基准同值。criteria-lock 版本仍 V0.11。

| 项 | 值 |
|---|---|
| 模块 | M019 文档与接口定义／第1段（4 文件） |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 本轮修复队列 | findings.md V2.60：M019-001…009 全部归属「是」（003/007 技术性，其余细化）；队列「否」0 |
| B3 | 已执行 1 次；模块仍 IN_PROGRESS |
| before（对象类型 tree） | `ce3a76f543b56b781fe93887a824548e0569500c` |
| after（对象类型 tree） | `d14e6792b4f5f4f37de77ec61020f3099246fa3a` |
| `git cat-file -t` | 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary ce3a76f543b56b781fe93887a824548e0569500c d14e6792b4f5f4f37de77ec61020f3099246fa3a`（无路径过滤） |
| 实际差异 | 13911 bytes；SHA256 `3c37ef2df171937200fece80e55b6b2f2a66b1126232ddddc499c25941c1628a`；4 files changed, 37 insertions(+), 25 deletions(-)；19 hunk；UTF-8 |
| 与冻结文件 `/home/james/audit-tmp/m019_b3_r1.diff` | 字节、SHA256、`cmp` 全等 |
| `git diff --check` | exit 0 |
| `git diff --name-only` | 仅下列 4 路径，全 `M`，无 A/D/R |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`（4 改动文件工作区 blob 与 after tree 全等） |
| 后端 / OpenAPI / 三 HTML / 锁 / findings / ledger | 本 diff 路径清单为空 |
| 只读探针 | `/home/james/audit-tmp/m019_b3r1_post.log`；本席复跑剩余计数与之同向（见 §4） |
| `package.json` `verify:predeploy` | 脚本键存在（python 读 scripts） |

四改动文件 blob（`git rev-parse TREE:path` / 工作区 `git hash-object`）与内容 SHA256：

| 文件 | before blob | after / work blob | 行 | 内容 SHA256 |
|---|---|---|---:|---|
| `docs/p1-floor-plan-polygon-api-contract-v0.1.md` | `9045ec9585bbeb21192b9c0017246305ca90bedb` | `b83cfa51563d32a535f29ad7191cb8c5ea2ca1ac` | 131 | `9a5f5c9949022dc78553410e162191ecb19d95ca5af9ad22cd3a11c50aa8e7ce` |
| `docs/p2-floor-plan-import-api-contract-v0.1.md` | `60f38bb688d53ed8d58e01b74b0575edd3a63d97` | `00797fe66bd8a9ff856f1cf0902874900983ec45` | 214 | `e36e6160b33372c708ee2acc5c4d426167cca525ed48abca3be9b68f168de921` |
| `docs/p2-floor-plan-import-completion-audit-v0.1.md` | `aca8448afd9696dfc29851c1a622dad859518d89` | `f7eadfbb17ed52b375c66da53422f6fea892aec5` | 71 | `54e364a7e151fcd7f84a04df1d2d96ff6e3aee9ce4566b76ed3432a82d429e2c` |
| `docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md` | `6f41b39fec2c72e28ec5abcce7965ab8334a9fc0` | `6a89f9ccb53b7e33fd1e760bfab50d416df32eac` | 279 | `05f1296760679eee73a9491aa06e5bd1bd554a14fcafddbe571cab3529fac388` |

`git diff --numstat`：11/10 p1；11/8 p2；8/4 完成审计；7/3 计划。文件名仍带 `v0.1.md`（001 只要求标题 V0.1→V0.2，未要求改路径）。

本席未改任何源文件，只新建本 raw。未 commit/push/PR/merge/部署；未对主索引 `git add`；未新建脚本/日志/临时产物（复核用 `/tmp/m019_b3_r1_recheck.diff` 已删）。

---

## 2. 锁、分类与本轮队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M019 的集合。本轮只用定稿 001–009 已引用的 ID。不 LOCK-AMEND。四分类本轮不重开。

B2-r1 定稿（findings.md V2.60）最小处置（摘录，作为本席内容票的尺子）：

- **001** 细化：删四行幽灵字段，`z_index` 改「否，缺省 0」；标题 V0.1→V0.2 + 文首一行变更记录。只改 p1 契约。
- **002** 细化：该行改为现行两种行为（未知 id 新建；他图 id 400）、补三行现行码。不重开 M015-002/003。
- **003** 技术性：文件名改为实际迁移、两处锚点改真实标题或去掉 fragment。无业务发明。
- **004** 细化：三处改为「任务 parsed 或 published 且所选候选均 pending；failed/cancelled 拒绝」，计划勾选描述随改。不重开 M015-016。
- **005** 细化：示例改为派生形态并加一句「id 由平面图 id 与幂等键派生，示意」。不重开 M015-001。
- **006** 细化：三句与完成审计一句改为现行口径（必填无默认 / 闭合 polyline / 整单拒绝）。不改解析器/服务。
- **007** 技术性：完成审计 §5 表前加快照说明；p2 §6 末句改 verify:predeploy 门禁；计划 :151 改「列表（PageResponse 信封，单页全量）」。
- **008** 细化：§3.4 规则加二次确认句并注明 M015-011 同批、本文尚未列入请求体。不改代码、不重开 011。
- **009** 细化：两处各加一句「shape_type 八值 CHECK 见 V423（V034 原六值已被其后迁移替换）」。不改 V034。

---

## 3. diff 逐块归因（闸 3）

19 hunk，全部变更行都能绑到 V2.60 归属「是」的 ID。混合 hunk 按下表拆到行。无无 ID 行。

| # | 文件 | hunk | 变更行（摘要） | ID | 越界? |
|---|---|---|---|---|---|
| 1 | p1 契约 | `@@ -1,7 +1,9` | 标题 V0.1→V0.2 | 001（最小处置明文要求版本） | 否 |
| 1 | 同上 | 同上 | 文首变更记录点名 001/002/003 | 001+002+003（001 要求「一行」；同文件另两 ID 写入该行） | 否 |
| 2 | p1 契约 | `@@ -42,7 +44,6` | 删 4.1 `evidence_attachment_ids` | 001 | 否 |
| 3 | p1 契约 | `@@ -52,7 +53,6` | 删 4.1 `operator_user_id` | 001 | 否 |
| 4 | p1 契约 | `@@ -60,16 +60,14` | 删 4.2 幽灵两行；`z_index` 是→否、缺省 0 | 001 | 否 |
| 5 | p1 契约 | `@@ -90,8 +88,11` | 补 PROJECT_NOT_FOUND / CROSS_PROJECT_FORBIDDEN / CROSS_MERCHANT_FORBIDDEN 三行；替换 FLOOR_PLAN_SHAPE_NOT_FOUND 行为他图 400 + 未知 id 新建 | 002 | 否 |
| 6 | p1 契约 | `@@ -125,6 +126,6` | 两处 fragment→真实标题；V001__initial_schema.sql→V005__p0_platform_master_data.sql | 003 | 否 |
| 7 | p2 契约 | `@@ -1,4 +1,6` | 标题 V0.2 + 变更记录点名 004/005/006/007/008 | 004+005+006+007+008（与本文件内容 ID 同集） | 否 |
| 8 | p2 契约 | `@@ -53,7 +55,7` | 创建响应 id `fpi_floor-import-create-001`→`fpi_`+32 hex | 005 | 否 |
| 9 | p2 契约 | `@@ -66,8 +68,8` | 任务 id 派生形态 +「本文示例 id 均为示意」 | 005 | 否 |
| 9 | 同上 | 同上 | `source_format` 必填无默认 | 006 | 否 |
| 10 | p2 契约 | `@@ -148,21 +150,22` | parsed 或 published 可继续发布；failed/cancelled 拒绝 | 004 | 否 |
| 10 | 同上 | 同上 | 高危二次确认句 + M015-011 同批、未列入请求体 | 008 | 否 |
| 11 | p2 契约 | 同上后半 / `### 4.1` | 解析 polygon 与闭合 polyline；非八种整单 400，不落任务/候选/明细 | 006 | 否 |
| 12 | p2 契约 | `@@ -208,4 +211,4` | §6 末句改 verify:predeploy，并指向完成审计 §5 快照 | 007 | 否 |
| 13 | 完成审计 | `@@ -1,4 +1,6` | 标题 V0.2 + 变更记录点名 004/006/007/009 | 004+006+007+009（与本文件内容 ID 同集） | 否 |
| 14 | 完成审计 | `@@ -10,17 +12,17` | 矢量图补闭合 polyline | 006 | 否 |
| 14 | 同上 | 同上 | 人工发布 parsed/published 分批 | 004 | 否 |
| 14 | 同上 | 同上 | 数据库行加 V423 八值 CHECK | 009 | 否 |
| 15 | 完成审计 | `@@ -46,6 +48,8` | §5 表前快照说明 | 007 | 否 |
| 16 | 开发计划 | `@@ -2,6 +2,8` | 文首说明点名 004/005/007/009；声明其余为历史原文 | 004+005+007+009（与本文件内容 ID 同集） | 否 |
| 17 | 开发计划 | `@@ -120,6 +122,8` | 候选 shape_type 八值 CHECK 见 V423 | 009 | 否 |
| 18 | 开发计划 | `@@ -148,7 +152,7` | 「分页列表」→「PageResponse 信封，单页全量，不分页」 | 007 | 否 |
| 19 | 开发计划 | `@@ -170,7 +174,7` | `fpic_1/fpic_2`→占位示意 | 005 | 否 |
| 20 | 开发计划 | `@@ -202,7 +206,7` | published 可继续发剩余 pending（M015-016 后口径） | 004 | 否 |

上表末行在 git 计数上是第 19 hunk（计划文件 5 个 hunk：16–19 在 unified diff 为 5 段；本表把第 19 hunk 的勾选改写单列）。hunk 计数 19，与 `git diff` `@@` 数一致。

文首变更记录与版本号：

| 文件 | 标题版本 | 变更记录点名 ID | 该文件实际内容 ID | 同集? |
|---|---|---|---|---|
| p1 契约 | V0.2 | 001/002/003 | 001/002/003 | 是 |
| p2 契约 | V0.2 | 004/005/006/007/008 | 004/005/006/007/008 | 是 |
| 完成审计 | V0.2 | 004/006/007/009 | 004/006/007/009 | 是 |
| 开发计划 | 无 V 号（历史计划标题未改） | 004/005/007/009 | 004/005/007/009 | 是 |

计划未改 006（B1 已记计划 L29 较准、定稿「三句与完成审计一句」不含计划解析段）。未把 001–003 写入 p2/计划。未把 008 写入完成审计/计划。

归因票：**PASS**。无越界行。

---

## 4. 内容审计（最小处置 × 现行源码）

### 4.1 M019-001

删 4.1/4.2 各两行幽灵字段；`z_index` 改选填缺省 0。现行 `FloorPlanCreateRequest` 11 字段与 4.1 表 11 行一一对应；`FloorPlanShapeSaveRequest` 10 字段（含 `Integer zIndex`）与 4.2 表 10 行一一对应。`docs/openapi/openapi-p0.yaml` `FloorPlanCreateRequest` `additionalProperties: false`，properties 无 `evidence_attachment_ids`/`operator_user_id`。saveShape 请求体 properties 含 `z_index` 且不在 required。`saveShapeOnce:277` `request.zIndex() == null ? 0 : request.zIndex()`。dto 包 `rg evidence_attachment|operator_user_id` = 0。

标题 V0.1→V0.2 + 一行变更记录：满足 001。

### 4.2 M019-002

`rg FLOOR_PLAN_SHAPE_NOT_FOUND backend` = 0；after 四文档除变更记录叙述外 0。`saveShapeOnce:261-270`：查到但 `floorPlanId` 不符 → 400 `VALIDATION_FAILED`「图形不属于该平面图」；查不到则用该 `shape_id` 新建。`validateCreateRequest:512` 404 `PROJECT_NOT_FOUND`；`:517`/`:610` 403 `CROSS_PROJECT_FORBIDDEN`；`:626` 403 `CROSS_MERCHANT_FORBIDDEN`。矩阵补三行码与 HTTP 语义与服务一致。两种行为写在同一矩阵行的「处理」列，符合「该行改为现行两种行为」。不重开 M015-002/003。

### 4.3 M019-003（V001__baseline.sql:64 复核）

本席 B1 写「P0 轻量 `polygon_points` 在 `V001__baseline.sql:64`」**误记**。本轮实核：

- `V001__baseline.sql` 存在，**10 行**，只建 `platform_migration_baseline`，无 `polygon_points`，无第 64 行。
- `V001__initial_schema.sql` 不存在（与 B1 一致）。
- `rg polygon_points backend/src/main/resources/db/migration`：P0 空间档案轻量坐标在 `V005__p0_platform_master_data.sql:64`（`spaces.polygon_points JSONB NOT NULL DEFAULT '[]'::jsonb`）；V023/V034 是 P1/P2 shape 表，不是 P0 轻量字段。

B2 尺子「所在迁移以 grep 为准」。B3 改为 `V005__p0_platform_master_data.sql` 与 grep 实态一致。roadmap L217 标题「阶段 8：市场基准增强与 SaaS 化深度能力」；故事图 L26 表行「第二期 / P1」。两处去掉失效 fragment、改真实标题。无业务发明。

### 4.4 M019-004

`publishImportTaskOnce:367-369`：非 parsed 且非 published → 400；`:387-389` 仅 pending 候选。三处（p2 §3.4、完成审计人工发布行、计划 Task 2 勾选）均改为该口径。`failed/cancelled` 拒绝写明。不重开 M015-016。计划勾选加「M015-016 后口径」是出处标注，不是新需求。

### 4.5 M019-005

`derivedId:664-665`：前缀 + SHA-256 前 32 hex，恒 36 字符。创建响应示例改为 `fpi_0123456789abcdef0123456789abcdef`（形态正确，值为示意）。规则句：「任务 id 为 `fpi_` + …派生…；本文示例 id 均为示意。」计划 `candidate_ids` 改为占位符而非伪造 32 hex（候选 id 现行是 `fpic_`+`slug(taskId+"_"+n)`，不是独立 SHA-256；伪造派生值会发明形态）。

残留：p2 §3.3/§3.4 JSON 仍有 `fpi_1`/`fpic_1`。本席 B1 拟处置含「或标示意」。V2.60 要求派生形态**并**加示意句。拼接幂等键的危险示例已改；全局「均为示意」覆盖剩余短桩。不把短桩当 FAIL（不是新缺陷，也不是越界）。B5 若要更干净可再换短桩，非本轮闸 3 失败。

### 4.6 M019-006

`validateImportCreateRequest:555` `normalizeAllowed(source_format)`，OpenAPI required 含 `source_format`，无默认。Parser L18 `(polygon|polyline)`；L79-83 未闭合才 `POLYLINE_NOT_CLOSED`。候选循环 L321 `normalizeAllowed(..., SHAPE_TYPES)` 无 try/catch；`IdempotencyService.runAndComplete` 有 `TransactionTemplate`，异常回滚 → 不落任务/候选/明细。完成审计矢量图行同步。不改解析器/服务。

### 4.7 M019-007

完成审计 §5 表数字未改（仍 33/528/181），表前加「2026-06 编码完成时 160 记录快照，非当前门禁」。p2 §6 删「后续完成前仍需」，改 `npm run verify:predeploy`（scripts 键存在）。计划「分页列表」→「PageResponse 信封，单页全量，不分页」。`listImportTasks:152` `new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size())`。多写的「不分页」与 M015-007 诚实不分页同义，不是新业务。

### 4.8 M019-008

§3.4 加句与定稿几乎逐字相同，并加治理地图「空间和点位」定位。DTO `FloorPlanImportPublishRequest` 仍仅 idempotencyKey/operatorNote/candidateIds。OpenAPI schema `additionalProperties: false`，required=`idempotency_key`+`candidate_ids`，无确认字段。M015-011 仍 XM。不改代码。

### 4.9 M019-009

完成审计数据库行与计划状态约束后各加一句，指向 `V423__m016_floor_plan_and_space_type_checks.sql`。V034:36 仍六值；V423:5-8 八值（含 booth/public_area）。计划 §4.1 仍列 V034 为当时新增文件（历史原文，009 不要求改文件清单）。不改 V034。

### 4.10 表格列数

python 扫改后四文件全部 Markdown 表：header 列数 = separator = 每行单元格。p1 4.1 4 列 11 行；4.2 4 列 10 行；§6 5 列 12 行；§9 2 列 4 行。完成审计范围表 2 列 11 行。无破表。

### 4.11 历史计划原文

计划 5 hunk 仅：文首说明、V423 一句、分页措辞、候选 id 占位、发布勾选。§1–4 文件清单、§5 字段列表、Task 1/3 勾选未动。与变更记录「其余为历史编码计划原文」一致。

### 4.12 只读剩余计数（本席复跑，非抄探针）

after 四文档：

- 字段表行 `evidence_attachment_ids`/`operator_user_id`：0（变更记录叙述命中不算残留行）
- `FLOOR_PLAN_SHAPE_NOT_FOUND`：0
- `V001__initial_schema` / `V001__baseline`：0
- 「仅 `parsed` 允许发布」类过时发布规则：0
- 「仅解析 `<polygon>`」：0
- `source_format` 现写「无默认值」（rg `svg.*默认` 命中的是「无默认值」句，不是旧默认）

与 `m019_b3r1_post.log`「剩余 0」同向。探针 §2 对 ShapeSaveRequest 的 sed 失败不构成本席证据；本席直接读 OpenAPI 原文。

### 4.13 无业务发明

未新增权限、事件、状态、八种之外的 shape_type、确认字段、分页参数、迁移。008 的「须二次确认」来自治理地图 L472 与 M015-011 已定稿 XM，契约只声明未列入请求体。

内容票：**PASS**。

---

## 5. 三线（作用于本 diff，不是整模块重审）

**第一性原理。** 每处改写的删除测试：删掉该行，对应定稿 ID 的文档/实现分叉仍在。四文件都必须改：001–003 只在 p1；004–008 在 p2（004/006/007 另落完成审计；004/005/007/009 另落计划）。无「顺手」抽象或改代码。

**顺从式。** 001–009 最小处置逐条对照 §4；现行 DTO/OpenAPI/Service/Parser/V005:64/V034 六值/V423 八值/verify:predeploy 脚本键均核对。映射判据仍只用 GEN-016/GEN-008/GEN-009/GEN-003/GEN-013/GEN-014/PGM-023，未扩锁。

**对抗式（grilling 自问自答，不派子代理、不问人）。**

1. 版本行是否无 ID？→ 001 明文要求 p1 版本；p2/完成审计版本与该文件变更记录 ID 同集。
2. 变更记录多 ID 是否把未改内容算进去？→ 四文件点名集 = 该文件内容 ID 集。
3. 未知 shape_id 写进 400 行是否发明 400？→ 处理列写「不报错/按该 id 新建」，HTTP 列只描述他图 400。与代码一致。
4. 「不生成任务」是否在无事务时说谎？→ `IdempotencyService` `TransactionTemplate.execute` 包住 action；异常回滚。
5. 计划占位符是否发明新 id 形态？→ 候选并非独立 SHA-256；占位 + 契约「示意」比伪造 32 hex 更懒、更真。
6. 改计划 §4.1 V034 清单才算 009？→ 定稿「两处各加一句」，已加在完成审计数据库行与计划状态约束后。清单是 2026-06 新增文件史实。
7. 幽灵字段名出现在变更记录是否算残留？→ 不是字段表行；after `git grep` 仅 L5 叙述。
8. 是否改了三 HTML / 锁 / 源码？→ `git diff --name-only` 对那些前缀空。

**ponytail-review（只评本 diff 可删性）：** Lean already. Ship. 变更记录是 001 要求的一行；「不分页」与「单页全量」同义，可缩但不是越界。`net: -0 lines required.`

---

## 6. 本席未做 / 不声称

- 未跑 `mvn test` / Semgrep / `npm run verify:predeploy`（文档模块；007 把那些数字标为快照）。
- 未读 `M019-b4-r1-self-raw.md`。
- 未改 findings/ledger/锁/HTML/业务源码。
- 未 fetch/commit/push/merge/deploy。
- 未用 MCP / 子代理 / 第三席。
- 未问操作员。

---

## 7. 最终票

| 轴 | 票 | 理由 |
|---|---|---|
| 归因（闸 3） | PASS | 19 hunk 全部变更行可绑 001–009；无无 ID 行；版本与文首记录 ID 同集 |
| 内容 | PASS | 九条最小处置均落地且与现行 Service/Parser/dto/OpenAPI/V005:64/V034/V423 一致；无业务发明；表列完整；计划未改历史原文之外内容 |
| **总结论** | **PASS** | 任一轴 FAIL 即 FAIL；本轮两轴均 PASS |

不等于 B4 双 PASS，不等于 B5，不等于模块 PASS。

---

## 8. 本轮完整 diff 全文（冻结件，与两 tree 实算 `cmp` 相等）

```
diff --git a/docs/p1-floor-plan-polygon-api-contract-v0.1.md b/docs/p1-floor-plan-polygon-api-contract-v0.1.md
index 9045ec95..b83cfa51 100644
--- a/docs/p1-floor-plan-polygon-api-contract-v0.1.md
+++ b/docs/p1-floor-plan-polygon-api-contract-v0.1.md
@@ -1,7 +1,9 @@
-# P1 商场平面图图片与多边形坐标 API 契约 V0.1
+# P1 商场平面图图片与多边形坐标 API 契约 V0.2
 
 文档类型：Implementation / API Contract
 
+> 2026-09-20 160 全量审计 M019（B3-r1，V0.1→V0.2）：删去现行 DTO/OpenAPI 中不存在的 `evidence_attachment_ids`、`operator_user_id` 字段行，`z_index` 改为选填缺省 0（M019-001）；错误矩阵按现行实现改写——他图 `shape_id` 为 400 `VALIDATION_FAILED`、未知 `shape_id` 按该 id 新建，补 `PROJECT_NOT_FOUND`/`CROSS_PROJECT_FORBIDDEN`/`CROSS_MERCHANT_FORBIDDEN`（M019-002）；§9 引用改为现行迁移文件与真实标题（M019-003）。
+
 本文档固化 P1-FLOOR-PLAN-01「商场平面图先做图片 + 多边形坐标」的最小可编码契约。P0 空间档案中的轻量 `polygon_points` 保持兼容；本 P1 模块作为后续商户、游乐项目、设备、视频点位、机器人点位和 CAD/SVG 导入的标准地图数据源。
 
 ## 1. 范围
@@ -42,7 +44,6 @@
 |---|---|---|---|
 | `idempotency_key` | 是 | string | 同一创建请求幂等 |
 | `operator_note` | 否 | string | 审计说明，默认 `created` |
-| `evidence_attachment_ids` | 否 | string[] | 仅存附件 ID 引用 |
 | `project_id` | 是 | string | 所属综合体/项目 |
 | `building_code` | 否 | string | 楼栋编码 |
 | `floor_no` | 是 | string | 楼层，如 `B1`、`1F`、`3F` |
@@ -52,7 +53,6 @@
 | `image_height` | 是 | integer | 必须大于 0 |
 | `version_no` | 是 | string | 图片版本号 |
 | `status` | 是 | enum | `active`/`inactive`/`archived` |
-| `operator_user_id` | 是 | string | 操作人 |
 
 ### 4.2 保存多边形标注
 
@@ -60,16 +60,14 @@
 |---|---|---|---|
 | `idempotency_key` | 是 | string | 同一标注写入请求幂等 |
 | `operator_note` | 否 | string | 审计说明，默认 `shape_saved` |
-| `evidence_attachment_ids` | 否 | string[] | 仅存附件 ID 引用 |
 | `shape_id` | 否 | string | 为空时创建；不为空时更新该标注 |
 | `shape_type` | 是 | enum | `shop` 铺位 / `booth` 摊位 / `public_area` 公共区 / `area` 区域 / `entrance` 入口 / `facility` 设施 / `risk_zone` 风险区 / `route` 路径。给人看只用这八种中文，无走道 |
 | `ref_type` | 是 | enum | `space`/`merchant`/`project`/`amusement_project`/`device`/`custom` |
 | `ref_id` | 否 | string | 被引用对象 ID；`custom` 可为空 |
 | `label` | 是 | string | 标注名称 |
 | `polygon_points` | 是 | point[] | 至少 3 个点，每个点包含 `x`、`y` |
-| `z_index` | 是 | integer | 前端渲染层级 |
+| `z_index` | 否 | integer | 前端渲染层级，缺省 0 |
 | `status` | 是 | enum | `active`/`inactive`/`archived` |
-| `operator_user_id` | 是 | string | 操作人 |
 
 ## 5. 事件
 
@@ -90,8 +88,11 @@
 | 状态非法 | 非 `active`/`inactive`/`archived` | 400 | `VALIDATION_FAILED` | 拒绝写入 |
 | 多边形点数不足 | 少于 3 个点 | 400 | `VALIDATION_FAILED` | 拒绝写入 |
 | 坐标越界 | 任一点 `x`/`y` 不在 `[0, 1]` | 400 | `VALIDATION_FAILED` | 拒绝写入 |
+| 项目不存在 | 创建时 `project_id` 查询不到 | 404 | `PROJECT_NOT_FOUND` | 拒绝创建 |
+| 越项目 | 项目员工读写其他综合体项目的平面图 | 403 | `CROSS_PROJECT_FORBIDDEN` | 拒绝 |
+| 越商户 | 商户账号读写其他商户的平面图 | 403 | `CROSS_MERCHANT_FORBIDDEN` | 拒绝 |
 | 平面图不存在 | `floor_plan_id` 查询不到 | 404 | `FLOOR_PLAN_NOT_FOUND` | 拒绝保存标注 |
-| 标注不存在 | 更新时 `shape_id` 查询不到 | 404 | `FLOOR_PLAN_SHAPE_NOT_FOUND` | 拒绝更新 |
+| 标注属于其他平面图 | 更新时 `shape_id` 属于其他平面图 | 400 | `VALIDATION_FAILED` | 拒绝更新（文案「图形不属于该平面图」）；查询不到的 `shape_id` 按该 id 新建标注，不报错 |
 | 幂等冲突 | 同 key 不同请求体 | 409 | `IDEMPOTENCY_CONFLICT` | 要求换 key 或查询原请求 |
 
 ## 7. Anti-Patterns
@@ -125,6 +126,6 @@
 | Topic | Location |
 |---|---|
 | 本切片计划 | `docs/superpowers/plans/2026-06-14-p1-floor-plan-polygons.md` |
-| P1 总路线 | `docs/master-roadmap.md#阶段-8-市场基准增强为-p1p2-演进` |
-| P0/P1 用户故事分层 | `docs/p0-user-story-map-and-sprint-slices-v0.1.md#第二期-p1` |
-| 空间档案 P0 轻量坐标 | `backend/src/main/resources/db/migration/V001__initial_schema.sql` |
+| P1 总路线 | `docs/master-roadmap.md`（「阶段 8：市场基准增强与 SaaS 化深度能力」） |
+| P0/P1 用户故事分层 | `docs/p0-user-story-map-and-sprint-slices-v0.1.md`（§1.1 版本分层「第二期 / P1」行） |
+| 空间档案 P0 轻量坐标 | `backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql` |
diff --git a/docs/p2-floor-plan-import-api-contract-v0.1.md b/docs/p2-floor-plan-import-api-contract-v0.1.md
index 60f38bb6..00797fe6 100644
--- a/docs/p2-floor-plan-import-api-contract-v0.1.md
+++ b/docs/p2-floor-plan-import-api-contract-v0.1.md
@@ -1,4 +1,6 @@
-# P2-A 平面图 SVG/标准 JSON 导入接口契约 V0.1
+# P2-A 平面图 SVG/标准 JSON 导入接口契约 V0.2
+
+> 2026-09-20 160 全量审计 M019（B3-r1，V0.1→V0.2）：发布前置改为 `parsed`/`published` 可继续发布剩余待处理候选（M019-004）；示例 id 改为派生形态并加说明（M019-005）；`source_format` 必填无默认、解析闭合 `<polyline>`、非八种整单拒绝（M019-006）；§6 验证口径以 verify:predeploy 为准（M019-007）；§3.4 补高危二次确认句（M019-008）。
 
 ## 1. 定位
 
@@ -53,7 +55,7 @@
 {
   "code": "OK",
   "data": {
-    "id": "fpi_floor-import-create-001",
+    "id": "fpi_0123456789abcdef0123456789abcdef",
     "floor_plan_id": "fp_1",
     "source_format": "svg",
     "source_name": "1F.svg",
@@ -66,8 +68,8 @@
 
 规则：
 
-- `idempotency_key` 必填。
-- `source_format` 给人看只有两种：矢量图（`svg`，默认）、标准数据（`standard_json`）。不接受其它格式。
+- `idempotency_key` 必填；任务 id 为 `fpi_` + 由平面图 id 与幂等键派生的 SHA-256 前 32 位十六进制（共 36 字符），不直接拼接幂等键；本文示例 id 均为示意。
+- `source_format` 必填，给人看只有两种：矢量图（`svg`）、标准数据（`standard_json`）。不接受其它格式，无默认值。
 - `source_payload` 最大 1,000,000 字符。
 - 导入只生成候选和错误，不直接改写正式 `floor_plan_shapes`。
 - 若没有任何合法候选，任务状态为 `failed`。
@@ -148,21 +150,22 @@
 
 规则：
 
-- 仅 `parsed` 状态的导入任务允许发布。
+- `parsed` 或 `published` 状态的导入任务允许继续发布剩余待处理候选；`failed`/`cancelled` 拒绝。
 - 仅 `pending` 候选允许发布。
 - 发布后生成正式 `floor_plan_shapes`，候选状态变为 `published`，任务状态变为 `published`。
 - 发布必须幂等，同一请求重复提交不得重复创建正式 shape。
+- 发布为高危动作，须二次确认（治理地图「空间和点位」）；服务端确认字段与 OpenAPI/前端同批落地（M015-011），本文尚未列入请求体。
 
 ## 4. 解析规则
 
 ### 4.1 SVG
 
-- 仅解析 `<polygon>`。
+- 解析 `<polygon>` 与首尾闭合的 `<polyline>`。
 - `<polyline>` 必须已闭合；未闭合时记录 `POLYLINE_NOT_CLOSED`，不生成候选。
 - 必须存在正向 `viewBox`，用于把点位归一化到 0 到 1。
 - 默认 `shape_type=shop`（铺位），默认 `ref_type=custom`。
 - `shape_type` 只允许八种：铺位 `shop`、摊位 `booth`、公共区 `public_area`、区域 `area`、入口 `entrance`、设施 `facility`、风险区 `risk_zone`、路径 `route`。无走道。
-- 可通过 `data-shape-type`、`data-ref-type`、`data-ref-id` 覆盖；不在八种内的记入错误明细，不生成候选。
+- 可通过 `data-shape-type`、`data-ref-type`、`data-ref-id` 覆盖；不在八种内的整单拒绝（400 `VALIDATION_FAILED`），不生成任务、候选与错误明细。
 - 候选 label 优先取 `data-label`，其次取 `id`。
 
 ### 4.2 标准 JSON
@@ -208,4 +211,4 @@
 - `FloorPlanControllerTests`
 - `FloorPlanJdbcIntegrationTests`
 
-后续完成前仍需运行全量后端测试、P0 契约校验和 Semgrep 聚焦扫描。
+全量后端测试、P0 契约校验与安全扫描以 `npm run verify:predeploy` 门禁为准（完成审计 §5 的数字为编码完成时快照）。
diff --git a/docs/p2-floor-plan-import-completion-audit-v0.1.md b/docs/p2-floor-plan-import-completion-audit-v0.1.md
index aca8448a..f7eadfbb 100644
--- a/docs/p2-floor-plan-import-completion-audit-v0.1.md
+++ b/docs/p2-floor-plan-import-completion-audit-v0.1.md
@@ -1,4 +1,6 @@
-# P2-A 平面图导入编码完成审计 V0.1
+# P2-A 平面图导入编码完成审计 V0.2
+
+> 2026-09-20 160 全量审计 M019（B3-r1，V0.1→V0.2）：人工发布规则按现行实现改写（M019-004）；矢量图导入范围补闭合 `<polyline>`（M019-006）；§5 数字标为编码完成时快照（M019-007）；候选 `shape_type` 八值 CHECK 指向 V423（M019-009）。
 
 ## 1. 结论
 
@@ -10,17 +12,17 @@ P2-A 商场平面图 SVG/标准 JSON 导入能力已完成当前可编码范围
 
 | 范围 | 结论 |
 |---|---|
-| 矢量图导入 | 支持 `<polygon>`；未闭合 `<polyline>` 记录错误，不生成候选 |
+| 矢量图导入 | 支持 `<polygon>` 与首尾闭合的 `<polyline>`；未闭合 `<polyline>` 记录错误，不生成候选 |
 | 标准数据导入 | 支持 `shapes` 数组和 `polygon_points` |
 | 候选种类 | 只发布铺位、摊位、公共区、区域、入口、设施、风险区、路径八种 |
 | 候选区 | 导入结果先进入 `floor_plan_import_candidates`，不直接覆盖正式 shape |
 | 错误明细 | 非法点位、未闭合 polyline、JSON 错误等进入错误明细 |
-| 人工发布 | 仅 `parsed` 任务和 `pending` 候选允许发布 |
+| 人工发布 | `parsed`/`published` 任务的 `pending` 候选允许分批发布；`failed`/`cancelled` 任务拒绝 |
 | 正式 shape | 发布后写入既有 `floor_plan_shapes` |
 | 权限 | 新增 `floor_plan.import.view`、`floor_plan.import.manage` |
 | 留痕 | 覆盖导入创建、查看候选、发布、失败事件 |
 | 幂等 | 创建导入和发布导入均接入 idempotency |
-| 数据库 | 新增 V034 迁移，包含导入任务、候选、错误表和权限种子 |
+| 数据库 | 新增 V034 迁移，包含导入任务、候选、错误表和权限种子；候选 `shape_type` 八值 CHECK 见 V423（V034 原六值已被其后迁移替换） |
 
 ## 3. 新增接口
 
@@ -46,6 +48,8 @@ P2-A 商场平面图 SVG/标准 JSON 导入能力已完成当前可编码范围
 
 ## 5. 验证证据
 
+> 下表为 2026-06 编码完成时的 160 记录快照，非当前门禁；当前验证以上述四个测试类与 `npm run verify:predeploy` 门禁为准。
+
 | 验证项 | 160 结果 |
 |---|---|
 | 聚焦测试 | `FloorPlanImportParserTests,FloorPlanServiceTests,FloorPlanControllerTests,FloorPlanJdbcIntegrationTests` 共 33 个测试通过 |
diff --git a/docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md b/docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md
index 6f41b39f..6a89f9cc 100644
--- a/docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md
+++ b/docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md
@@ -2,6 +2,8 @@
 
 > **执行要求：** 编码前先按本计划做 TDD。先写失败测试并确认失败，再做最小实现，再运行聚焦测试、全量测试、契约校验和安全扫描。
 
+> 2026-09-20 160 全量审计 M019（B3-r1）：发布状态机、示例候选 id、列表信封与八值 CHECK 出处按现行实现改写（M019-004/005/007/009）；其余为历史编码计划原文。
+
 ## 1. 目标
 
 在 P1 “图片 + 多边形坐标”能力基础上，新增 P2-A 平面图导入能力：运营或实施人员可以上传/提交 SVG 或标准化 JSON，系统解析为候选区域，人工审核后发布为正式 `floor_plan_shapes`。
@@ -120,6 +122,8 @@
 - import task：`parsed`、`failed`、`published`、`cancelled`
 - candidate：`pending`、`published`、`rejected`
 
+候选 `shape_type` 八值 CHECK 见 `V423__m016_floor_plan_and_space_type_checks.sql`（V034 原六值已被其后迁移替换）。
+
 ## 6. 接口变化
 
 ### 6.1 创建导入任务
@@ -148,7 +152,7 @@
 
 权限：`floor_plan.import.view`
 
-返回：导入任务分页列表。
+返回：导入任务列表（PageResponse 信封，单页全量，不分页）。
 
 ### 6.3 查询导入候选
 
@@ -170,7 +174,7 @@
 {
   "idempotency_key": "floor-import-1f-publish-001",
   "operator_note": "publish reviewed candidates",
-  "candidate_ids": ["fpic_1", "fpic_2"]
+  "candidate_ids": ["<候选 id 1>", "<候选 id 2>"]
 }
 ```
 
@@ -202,7 +206,7 @@
 - [x] 写失败测试：导入到不存在的 floor plan 返回 `FLOOR_PLAN_NOT_FOUND`。
 - [x] 写失败测试：重复 idempotency key 同请求返回首个结果，不同请求返回冲突。
 - [x] 写失败测试：发布候选会写入正式 `floor_plan_shapes`，并将 task/candidate 标记为 published。
-- [x] 写失败测试：failed/cancelled/published task 不允许重复发布。
+- [x] 写失败测试：failed/cancelled task 不允许发布；published task 可继续发布剩余 pending 候选，已发布候选不得重复发布（M015-016 后口径）。
 - [x] 实现 service 最小逻辑。
 - [x] 运行 `cd /home/james/complex-management/backend && mvn -Dtest=FloorPlanServiceTests test`。
```
