CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M018 B5-r2 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.01（指定路径原先不存在，本次新建完整报告）
- 席位：固定 Grok 外审席乙（grok-4.6 / xhigh；不派子代理、不用 MCP、不问操作员、不读本轮 `M018-b5-r2-self-raw.md`）
- 步骤：B5-r2（B3 第 2 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B4 双 PASS、B3 自报或 B1/B2/B5-r1 历史票替代本轮结论）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M018 同一 7 文件；`runbook.md` V0.02；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 模块：M018 — 空间点位、楼层平面图与营业时段／网页与验证／第1段
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值（889020 字节 / 6850 行）；sidecar 全文一行同值。独占锁行仅本文第一行，正文不再出现同名独占字段。
- 映射：`M018 → [BSM-001, BSM-003, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001]`，COUNT=21 unique=21 MISSING=[] EXTRA=[]；与锁映射行 L6397 逐字相同；定义与 HTML 出处均从锁/三 HTML 自读，不新造判据、不 LOCK-AMEND
- 正本：`/home/james/complex-management` main 主工作区。`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`。派发所述 B3-r2 after tree `c9301ef0a5b95d210633c087dba236dbdb7cd3a1` 可解析为 `tree`；本模块 7 文件当前工作区 `git hash-object` 与该 after **MISMATCH_COUNT=0**（`git diff --stat c9301ef0… -- <7 paths>` 空）。现工作区 `git add -A && git write-tree` 为 `53bd11c5a0d80f9ea95c6e311848d90cab98cf07`，差值来自审计产物（本轮 self-raw 等），不在 7 文件范围内。
- 本席约束：只读业务代码与计划；除本 raw 外不写文件；不修源/三 HTML/锁/findings/ledger；不 fetch/commit/push/merge/deploy；不装依赖；不建脚本/日志/临时产物；主仓 cwd 只跑派发允许的 vitest 四文件 + `tsc -b --noEmit`
- 未读取：`M018-b5-r2-self-raw.md`（独立性禁令，整文件不打开）。旧 B1/B2/B4/B5-r1 可读，只作已定稿边界与「不把全部否当功能全实现」的对照，不作本 B5 通过依据，不复用为票
- 已定稿背景（只作边界）：findings V2.58 — M018-001…010 修复队列「否（已修）」；M015-010/011 XM 前端部分待后端，不重复立项。B3 累计 2；本轮若 FAIL 则 B3-r3 为最后一次。模块生命周期仍 `IN_PROGRESS`。本票不等于双席 B5 或模块 PASS。席甲票是作者自审票；本席 FAIL 具否决权。

本次审计结论：**PASS**。7 文件全文覆盖；21 项逐条记录；三线均执行。001–010 在当前 7 文件上独立复验已落地、无回归；M015-010/011 维持 XM 不升格。无新工程缺陷、无添加项、无待问 2.2、无锁外判据、无 HTML 修改申请。观察项见 §8，不进修复队列。

---

## 0. 技能自核（V0.17 §6，按 B5 收窄）

本席实际读取并按收窄口径使用（本机路径实核，不抄 Codex 预检表）：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户 160 提示词优先于技能本体。本步禁止子代理，与技能「先派子代理」冲突处按用户指令收窄 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；路由到本仓库 160 B5，**不执行** Arc `audit`/`review`（本体要派评审子代理并写 `docs/arc/audits/`，与双席/§7 冲突，记缺口） |
| graft | `~/.claude/skills/graft/SKILL.md` | `graft skeleton` 三页 API 后再全文读源；定位 Controller/Service 闭集与权限 |
| grilling / grill-me | mattpocock `productivity/grilling`、`grill-me`（本体转发 grilling） | 只用追问树自问自答；本体「等用户」「dispatch sub-agent」作废。四类账号第一次进每一页 + 新建/导入/发布/存档/删除审批/恢复/营业时间保存 + 中文 + 幂等键 + 分批发布 |
| verification-before-completion | superpowers 同名 | 先跑 vitest/tsc 再声称；未跑不得写绿 |
| systematic-debugging | superpowers 同名 | 只借读错误→追数据流（页→adminClient→Controller 映射→Service/Parser）；B5 不修复、不加 instrumentation |
| diagnosing-bugs | mattpocock engineering | 用契约对照构造可红信号；本机不跑 Java |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删重复，不落地、不单独立项 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读；vitest/tsc 在本席命令内跑完（秒级），未另挂 monitor |
| codebase-design | mattpocock engineering | 页为浅适配器、后端为深度模块；查 seam 不匹配 |
| writing-for-agents | mattpocock productivity | 本 raw 按完成条件 + `文件:行` 组织，供合取 |
| git-guardrails-claude-code | 可选存在 | 未装 hook；等价：不 fetch/commit/push/merge/hard reset |
| receiving-code-review | superpowers | B5 外审否决席，不 IMPLEMENT |
| requesting-code-review | superpowers | 节点为 B4，本步不派 |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、using-git-worktrees、executing-plans、dispatching-parallel-agents、subagent-driven-development、domain-modeling、research、ponytail-audit、Arc audit 全流程。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能（仅 grilling / grill-me）
- `ubiquitous-language`：本机技能树未找到
- Arc `audit`：与双席/§7 冲突，**不调用**
- Trail of Bits `audit-context-building` / `differential-review`：会派子代理或只审 diff，**不调用**

明确不用（controller 划掉）：MCP、第三主体、修源/HTML/锁/台账。

---

## 1. 锁、范围与源指纹（实算，非手抄）

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`；`python3 hashlib`；`openssl dgst -sha256`。三工具同值，见本文第一行独占锁行。未抄 sidecar / ledger / 派发期望值。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 字节 / 行 | 889020 / 6850 |
| sidecar | 全文一行同值 |
| findings | V2.58；SHA256 `2eb85e509bdc437f1e6e58232c2ab5028b5bbb26c01db23f7df9769de4baf61a` |
| ledger | SHA256 `8f64687cd9cff6e2534ef766a5ce1a29f5c5ba794812e5e5c24d377d6f8ae311`；M018 `IN_PROGRESS`，B3=2 |
| 映射行 | L6397，21 条、无重复、MISSING=[] EXTRA=[] |
| 计划 FILES | 7 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 1080（切分时）；当前工作区 `\n` 计数 **1706**（B3-r2 后现态，不改计划凑数） |
| `git ls-files -z -- <7> \| xargs -0 cat \| wc -l` | **1706** |
| questions | 无 `questions/M018*`；无未答问询 |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| modules.md / runbook.md | SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` / `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| after tree | `c9301ef0a5b95d210633c087dba236dbdb7cd3a1`（`git cat-file -t` = tree）；7 文件 blob 全等 |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466` |

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 行 | SHA256 |
|---|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

本模块 7 文件工作区指纹（`wc -l` / `sha256sum` / `git hash-object` = after tree blob）：

| 路径 | 行 | SHA256 | git blob |
|---|---:|---|---|
| `FloorPlanImportPage.tsx` | 443 | `75f227a783ea7992b77a67aa283eb716a5d8984a8f6479707f671cffa4e10acd` | `204dafbd24d72637df5447ae97190cec78daec5f` |
| `FloorPlanImportPage.guidedcreate.test.tsx` | 148 | `5790a81e551db768b9ee12b23418bd949cddc5e83bd326ce40cd8f9d33a0fa97` | `4132987d33f51ebf973d6920bfae87c6086e5063` |
| `FloorPlanImportPage.test.tsx` | 302 | `68af58616c93dfa315ce0a8badc247dc01a48752e672bf19f3ae4dbc4a9a8a8f` | `af5fc7e8d2f30d4f7a01420757fed745cbc6b664` |
| `SpaceTypesPage.tsx` | 280 | `4d450b0df0de1acdba22e9dec777f8fa11159defd395a3601b19171efd88deed` | `856a0545618948ce7142cfa144d79e88398fa790` |
| `SpaceTypesPage.guidedcreate.test.tsx` | 166 | `db80bdf26bdd9ce931e72591d666faf168ca06ad2aa993f91d792feefdfa9e4b` | `01ccdd77db8126f923477fb03973df9e0bf7027b` |
| `VenueBusinessHoursPage.tsx` | 248 | `841c1e543bd573d6fbfa7e2773f7aa391e58cee430695199e5bc105be32973a9` | `45237715ed4e48d96d0c7d8ecaf8c62806a5395e` |
| `VenueBusinessHoursPage.test.tsx` | 119 | `60f3642dd6161bbabffd75483af9ba9a05d84176ebf7e7461e8c8855c6e6118c` | `b0ae055e6c3a821e0d187bf63857f9b473da1846` |

锁内 BSM-001 / BSM-003 / PGM-001 原文已与三 HTML 对应锚点逐字核对：BSM-001=`business-system-map` L505–522；BSM-003 锁摘录为 h2+lead（L563–564），子 h3 为 BSM-004…007 未映射本模块；PGM-001=`platform-governance-map` L256 起整段（含零英文、可点击溯源、同时操作以先成功为准）。BEQR 无本模块映射项。

---

## 2. 第一性原理线（逐文件三问）

依据类型：1=HTML 原文；2=可执行产物；4=可复现命令+输出。本模块 7 文件均为可执行 TS/TSX，无文档豁免。

### 2.1 `FloorPlanImportPage.tsx`（L1–443）全文

1. **为什么必须存在？** 治理地图「空间和点位」主要功能第三条（`platform-governance-map` L472）：「商场平面图网页导入：在后台上传或导入商场平面布局图，系统解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误后发布到正式平面图；发布为高危动作，须二次确认」。删掉本页，该条后台入口不存在。上位：BSM-001 先管基础盘子「在哪经营」；PGM-001 先管归属「哪个楼层、哪个区域、哪个点位」。
2. **声称是否实际执行？** 页头 L260「上传商场平面图，解析出候选区域，人工复核候选与错误后发布」。对照现行后端：`FloorPlanController` GET `/floor-plans` 要 `floor_plan.view`（L33，无平台 scope）；POST 创建要 `floor_plan.manage`+幂等键；导入/发布要 `floor_plan.import.manage`；下钻要 `floor_plan.import.view`；`/platform/projects` 要 `governance.project.view`+`requirePlatformScope`。本页：主资源 `api.get("/api/v1/floor-plans")`；副资源 `loadSecondaryIfPermitted(hasPermissions(..., ["governance.project.view"]), …)`（L117–133）；创建体含楼层/底图/宽高/版本号且 `status:"active"`（L160–181）；导入校验来源名称（L189–204）；发布只发勾选且 `HighRiskConfirm`+`IMPORT_MANAGE`（L212–227, L352–360）；幂等键 camel `idempotencyKey`。vitest 含创建 POST 体、导入名称拦截、未勾选拦截、VIEW_ONLY 不请求项目、projects 403 降级。类型 2+4。
3. **未验证前提？** 本机未跑 Maven/`complex_test`；后端行为以源码原文为准（类型 1 的对照源，非本模块可执行）。发布确认字段仍无（M015-011 XM）。SVG 缺 label 时解析器默认 `shape-N`（`FloorPlanImportParser.java:99`，跨模块）。不把测试全绿当成契约已实现——已用真实 `AdminApi` 走 `normalizeRequestBody` 钉死 snake 体。

### 2.2 `FloorPlanImportPage.guidedcreate.test.tsx`（L1–148）全文

1. **为什么必须存在？** GEN-003：关键分支须有能失败的断言。本文件钉死新建平面图引导步、后端必填体、`status:"active"`、幂等键 `fp-` 前缀。删掉则 M018-001/005 主路径无回归网。
2. **声称是否执行？** 本轮 `npx vitest run` 4 例全过（见 §9）。断言 `lastPostBody` 含 `project_id/plan_name/floor_no/image_url/image_width/image_height/version_no/status:"active"` 且 `idempotency_key` 匹配 `/^fp-/`；楼层/底图留空不提交。类型 2+4。
3. **未验证前提？** 夹具用 wildcard 管理员，不覆盖 VIEW_ONLY（由 sibling test 覆盖）。fetch 桩 200，不代替后端 400 网；但 POST 体字段与 `validateCreateRequest` L504–527 对齐，缺字段用例会红。

### 2.3 `FloorPlanImportPage.test.tsx`（L1–302）全文

1. **为什么必须存在？** 中文闭集、发布勾选、权限码、下钻 403、M018-010 副资源降级的能失败网。
2. **声称是否执行？** 本轮 15 例全过。夹具只用后端闭集 `active/archived/parsed/pending/POINTS_TOO_FEW/points`；未知 `corridor`→「未识别」；VIEW_ONLY `projectsRequests===0` 且表仍渲染；ADMIN+projects 403 表仍在且 `floor-plan-projects-degraded` 匹配 `/无权|加载失败/`。类型 2+4。
3. **未验证前提？** `getComputedStyle` jsdom 警告不构成失败。未模拟商户 Cookie 真 403（用 `projectsStatus:403` 等价后端信封）。

### 2.4 `SpaceTypesPage.tsx`（L1–280）全文

1. **为什么必须存在？** PGM-001「先管归属」+ BSM-003 标题「数据可改可删、操作有引导、权限分岗」。空间种类是建空间的下拉数据源。删掉本页，后台无法维护种类、无法走存档/申请删除/最高权限审批/已删除恢复。
2. **声称是否执行？** 首屏只 GET `/api/v1/platform/space-types`（后端 L169–173：`governance.space.view`+`requirePlatformScope`），与路由 `adminRoutes.ts:50` 同码。新建 GuidedFlow；存档 prompt 理由 + POST `/archive`；申请删除须 archived；审批仅 `usr_master_authority`+`master-authority`；`disabled` 行「恢复」走既有 `/unarchive`（后端 L589–595 允许 archived|disabled→active）。写权限 `governance.space.manage`。类型 1+源码对照。
3. **未验证前提？** `approveDeleteSpaceType` 控制器无 `requirePermission`（服务内 master+自批禁，跨模块 M009，OBS）。存档 `HighRiskConfirm` 默认「不可逆」文案（OBS，存档可取消）。不重复立项 M015-010。

### 2.5 `SpaceTypesPage.guidedcreate.test.tsx`（L1–166）全文

1. **为什么必须存在？** 新建引导 + 生命周期四动作（存档/申请删除/审批/恢复）的能失败断言（原 M018-006/009）。
2. **声称是否执行？** 本轮 10 例全过。新建 POST `typeCode/typeName/sortOrder/status:"active"`；空显示名拦截；存档空理由不请求、填理由 POST `{reason}`；申请删除 POST `/delete-request`；非 master 只见「待最高权限审批」；master POST `decision:"approved"`；已删除 POST `/unarchive`。类型 2+4。
3. **未验证前提？** 使用 mock `AdminApiClient` 而非真实 `AdminApi`，不断言 wire snake（与页面 camel 键一致，真实客户端会转 snake，后端 record 驼峰+Jackson）。取消存档按钮存在性有断言、点击 POST 与恢复共用 `unarchiveType`，不单独立项。

### 2.6 `VenueBusinessHoursPage.tsx`（L1–248）全文

1. **为什么必须存在？** 入场/无限权益按该表划场次（服务注释与核销侧依赖）。BSM-001 时刻属策略参数。删掉本页，运营只能改库。治理地图未单列「营业时间配置页」，但「发生在哪」+场次判定需要时刻配置；BSM-001 可配置原则覆盖时刻。
2. **声称是否执行？** 首屏 `Promise.all`：主资源 GET `/platform/venue-business-hours`（`governance.space.view`+平台 scope，`PlatformController` L232–236）+ 副资源 `loadSecondary(GET /platform/spaces)`（同码同 scope，L160–166）。两接口权限码与 scope **相同**，不构成 M018-010 那种「主 200 / 副 403 拖死」分裂。保存 POST `{venueId,dayOfWeek,openTime,closeTime,disabled}`；`AdminApi.normalizeRequestBody` 会补 `idempotencyKey`；控制器 `requireIdempotencyKey`（L244）；仓库按 `(venue_id,day_of_week)` upsert（`VenueBusinessHoursRepository` L78–90）。跨零点中文、同时刻拦截、停用保留时刻。类型 2+4。
3. **未验证前提？** 页显示「已生效」、不采集 `effectiveFrom`、保存即生效——与现行 `VenueBusinessHoursService` L24–26 一致，与 BSM-001「不即改即生效」冲突，**维持并入 M015-010 XM，不新开**。本页无页面级 KeyRef（依赖客户端生成键+自然键 upsert），失败重试会换键但仍覆盖同一天，不构成重复营业行。

### 2.7 `VenueBusinessHoursPage.test.tsx`（L1–119）全文

1. **为什么必须存在？** 跨零点文案、同时刻拦截、保存体、停用 `disabled:true`、场地清单降级。
2. **声称是否执行？** 本轮 5 例全过。降级提示含「加载失败」，已配场地仍可选。类型 2+4。
3. **未验证前提？** mock client，不经 `normalizeRequestBody`；不断言幂等键（生产路径由 AdminApi 注入）。未覆盖未选场地/未填时刻（代码 L105–111 有中文拦截，属已有分支，不因缺例升缺陷）。

---

## 3. 顺从式线（21 项全部适用/证据）

先假定实现意图正确，再逐项核对本模块映射。不适用须写清「本模块 7 文件无该产物」；不新增判据。

| ID | 适用 | 证据（本模块，文件:行 / 命令） | 本轮 |
|---|---|---|---|
| BSM-001 | 适用 | 锁原文 L2278–2296 / HTML L505–522：先管基础盘子「在哪经营」+时刻属策略参数。三页分别承载平面图导入、空间种类、场地时刻。营业时间「已生效」与「不即改即生效」冲突维持 M015-010 XM，**不把全部否当该原则已在本页落地** | 满足（XM 不升格） |
| BSM-003 | 适用 | 锁原文仅 h2+lead（HTML L563–564）。实现：空间种类存档/申请删除/最高权限审批/占用由后端拦/已删除可恢复（`SpaceTypesPage.tsx:172–212`）；新建走 `GuidedFlowModal`；写按钮按岗码。平面图导入页不是种类生命周期页，不要求本页再造存档按钮 | 满足 |
| GEN-001 | 适用 | 三页各有 HTML/场次上位；测试文件钉契约。`zhOrDash`/`fpLabel` 重复为 OBS 可删，非无需求承载 | 满足 |
| GEN-002 | 适用 | 创建/导入/发布/存档/审批/恢复/保存路径与现行 Controller/Service 闭集对齐。vitest 34/34、tsc 0。VIEW_ONLY 首屏不再被 projects 403 拖死 | 满足 |
| GEN-003 | 适用 | 本轮 vitest 4 文件 34 passed；断言能失败于：缺楼层/底图、缺来源名、未勾选发布、无 import.manage 按钮禁用、下钻 403、仅 view 不请求项目、projects 403 降级、存档空理由、停用保存、清单降级 | 满足 |
| GEN-004 | 适用 | 前端步内校验 project/名称/楼层/底图/宽高/版本号、来源名、候选勾选、种类编码显示名、开歇时刻；所有权/存在性由后端 `projectExists`/`VENUE_NOT_FOUND`/占用检查。输入在首次提交前有类型与非空校验 | 满足 |
| GEN-005 | 适用 | 路由：平面图 `floor_plan.view`；空间种类/营业时间 `governance.space.view`。写：`floor_plan.manage` 仅新建平面图；导入/发布 `floor_plan.import.manage`；下钻 `Can`+`floor_plan.import.view`；种类/时刻写 `governance.space.manage`。审批服务端 master+自批禁。服务端仍是权威；界面隐藏不代替 403 | 满足 |
| GEN-006 | 适用 | 本模块无直接写库。发布/创建走后端事务+幂等；营业时间 upsert 唯一键；种类删除占用检查。前端失败不本地假写入 | 满足 |
| GEN-007 | 适用 | 平面图三写 `idempotencyKey: *KeyRef`（camel，避免 snake 被 `normalizeRequestBody` L90–91 覆盖）。导入成功清空 importKey；发布成功后 `loadCandidates` 清空 publishKey。营业时间自然键 upsert，重复提交覆盖同一天。PGM-001「同一操作只认一次」在平面图路径由幂等键、在时刻路径由唯一键 | 满足 |
| GEN-008 | 适用 | 平面图 status 闭集 active/inactive/archived；任务 parsed/failed/published/cancelled；候选 pending/published/rejected；形状八值与 `FloorPlanService.SHAPE_TYPES` 一致；错误字段/码与 Parser 十码一致。创建体字段与 `validateCreateRequest` 一致。空间种类 status 页内四态。时刻 day 0–6 与库注释一致 | 满足 |
| GEN-009 | 适用 | 下钻 try/catch→`setGlobalError`；创建/导入弹窗内错误不关窗；发布失败 setGlobalError；副资源 403/网络只 notice。VIEW_ONLY 不再把副资源失败写成「平面图读取失败」 | 满足 |
| GEN-010 | 适用 | 无密钥、无注入点（React 文本插值）；导入 payload 本页不执行 SVG。错误走中文业务话术 | 满足 |
| GEN-011 | 适用 | `AuditTrailHint` 事件名收入「操作留痕」技术折叠（`AuditTrailHint.tsx:40–46` 用户裁决原样保留）。真实留痕在后端 `auditEventService`。候选/错误抽屉可点 | 满足 |
| GEN-012 | 适用 | 列表吃后端默认分页（projects/spaces size=500）；发布上限 200 在后端，本页按勾选发送。首轮正常导入达不到 2000 候选。无无界轮询 | 满足 |
| GEN-013 | 适用（弱） | 本模块无 package/CI 文件。可执行证明：`npx tsc -b --noEmit` EXIT 0（apps/web） | 满足 |
| GEN-014 | 不适用 | 7 文件无 SQL/迁移。模式闭集对照 V023/V034/V226/V423 只作契约阅读，不在本模块执行 migrate | 不适用（已核） |
| GEN-015 | 适用 | 可见标签/按钮/校验/状态均为中文；未知码「未识别」不直出英文枚举。`data-testid`/API 路径/事件名在技术折叠或测试选择器，属内部标识。键盘：候选/错误行 tabIndex+Enter/空格 | 满足 |
| GEN-016 | 不适用 | 7 文件无说明性 md。页内 description 与 HTML L472 一致，不作完成声明 | 不适用（已核） |
| GEN-017 | 适用 | 本轮无新业务发明。已定稿 001–010 分类不重开。M015-010/011 保持 XM。观察不升添加 | 满足 |
| GEN-018 | 适用 | 结论落到 HTML 原文、vitest/tsc 输出、后端源码原文、可复现命令。不引用「B4 已 PASS」或本席 B5-r1 票作为依据 | 满足 |
| PGM-001 | 适用 | 零英文：页内闭集中文（测试钉死不含 active/parsed/shop/pending/POINTS_TOO_FEW）。可点击：候选/错误抽屉；主表下钻导入（OBS：种类/时刻行无抽屉，不升格以免把全后台表模式扩成新判据）。同时操作以先成功为准：发布/创建幂等键。归属：平面图必选项目、种类用于空间、时刻绑场地 | 满足（XM 发布确认字段不升格） |

---

## 4. 对抗式线（grilling 自问自答，四类账号 × 三页 × 主路径）

grilling/grill-me 本体「问用户、派子代理」作废；事实本席自读。不问人、不为理论叠加需求。判定：第一次正常跑会撞上才立缺陷。

### 4.1 四类账号第一次进入每一页

权限码与 scope 以现行后端为准（类型 1 对照源）：

| 请求 | 权限码 | scope |
|---|---|---|
| GET `/api/v1/floor-plans` | `floor_plan.view` | 无 `requirePlatformScope` |
| GET `/api/v1/platform/projects` | `governance.project.view` | `requirePlatformScope`（平台管理员桥放行；商户/外包 403；项目员工无 merchantId 则放行） |
| GET `/api/v1/platform/space-types` | `governance.space.view` | 平台 scope |
| GET `/api/v1/platform/venue-business-hours` | `governance.space.view` | 平台 scope |
| GET `/api/v1/platform/spaces` | `governance.space.view` | 平台 scope |

路由：`adminRoutes.ts:95` 平面图只 `floor_plan.view`；`:50/:51` 种类/时刻只 `governance.space.view`（前端路由不查平台 scope）。

| 账号 | 平面图导入 | 空间种类 | 场地营业时间 |
|---|---|---|---|
| 平台管理员（wildcard，平台桥） | 主资源 200；副资源 projects 发请求且 200；表渲染；新建/导入/发布按钮可用 | GET 200；新建/存档可用；不见审批 | hours+spaces 均 200；保存可用 |
| 最高权限（`usr_master_authority`/`master-authority`） | 无本页特判；有权限则同管理员 | `pending_delete` 显示审批通过/驳回（L213–226）；服务端仍禁自批 | 同管理员（若有 space.manage） |
| 分岗只读 `floor_plan.view`（无 project.view） | **不请求** projects（`loadSecondaryIfPermitted` L100 短路）；表照常；无下钻；新建因无 manage 禁用 | 无 space.view 则路由挡在门外 | 同左 |
| 分岗 `governance.space.view`（平台员工，无 manage） | 无 floor_plan.view 则门外 | 首屏单请求 200；写按钮禁用 | 两请求同码同 scope 均 200；保存禁用；副资源失败才降级 |
| 商户（merchantId 非空） | 有 floor_plan.view：主资源 200；若另有 project.view 则 projects 403→`loadSecondary` notice「无权」+表仍在（测试 L294–301）；无 project.view 则不发请求 | 有 space.view：GET 403 平台 scope→整页「空间种类读取失败」（主资源失败，fail-closed，与副资源拖死不同） | 两接口均 403→整页「场地营业时间读取失败」（主资源失败） |

M018-010 根因（仅 view 第一次进平面图页被 `Promise.all` 拖死）在当前代码+测试上不可再复现。种类/时刻页主副权限码相同，商户 403 落在主资源，不构成新的分裂缺陷。

### 4.2 真实后端契约下的写路径

- **新建平面图**：`planFormError` 与 `validateCreateRequest` 对齐；`status:"active"` 在 STATUSES；`idempotencyKey` camel。guidedcreate 实测 POST 体。第一次正常提交不再 400。
- **创建导入**：来源名称+内容必填；`source_format` 仅 svg/standard_json 中文下拉。名称留空被步内拦截（测试）。
- **发布**：未勾选 `precheck` 拦截「请勾选要发布的区域」，不默认全发。勾选后 POST `candidate_ids` 仅勾选项。后端允许 `parsed|published` 继续发剩余 pending（`publishImportTaskOnce` L367–368）——分批发布本页不拦截已发布任务上的剩余勾选，刷新后 pending 仍可勾。一次 >200 由后端 VALIDATION_FAILED；首轮正常达不到，不立缺陷。
- **存档/申请删除/审批/恢复**：存档须理由；删除须先 archived；占用后端冲突；master 审批；disabled 恢复 `/unarchive`。测试执行 POST。
- **营业时间保存**：缺场地/缺时刻/同时刻中文拦截；停用提交 `disabled:true` 且时刻保留；后端 upsert 不插第二行。

### 4.3 正常态中文

平面图表 active→启用、archived→已存档；任务 parsed→已解析；候选 shop/space/pending→铺位/空间/待复核；错误 points/POINTS_TOO_FEW→顶点/顶点不足。测试断言不含原始英文码。种类四态中文。时刻「次日凌晨」「本日不营业」。Parser 英文夹在 `errorMessage` 原文（viewBox/svg）属后端 M015/M017，不在本模块发明。

### 4.4 幂等键

`adminClient.ts:90-91`：`idempotencyKey: body.idempotencyKey ?? nextIdempotencyKey()` 再整体 snake。页面必须传 **camel** `idempotencyKey`，否则页面 KeyRef 被生成键覆盖（原 M018-005）。当前三处写路径均为 camel。测试经真实 AdminApi 断言 `idempotency_key` 前缀 `fp-`/`fpi-`/`fpp-`。

### 4.5 反例未成立（不立项）

- 种类/时刻行无抽屉：PGM-001 全局，B1 已 OBS，不把全后台表模式扩成新判据。
- 存档按钮默认「不可逆」：存档可取消，日常确认≠审批。
- 时刻无 GuidedFlow：按日保存不是「＋新建」；BSM-005「正持续改造」。
- 发布无服务端确认字段：M015-011 XM。
- 时刻保存即生效：M015-010 XM。
- `approveDelete` 控制器无权限注：服务内 master，跨模块。
- 201 个候选一次发布：非第一次正常路径。

---

## 5. 已定稿 001–010 独立复验（不把「否」当未读）

每条对照**当前**源码与本轮测试，不抄 B4 归因。

| ID | 原缺陷要点 | 当前证据 | 回归？ |
|---|---|---|---|
| M018-001 | 创建体缺必填、status draft | `FloorPlanImportPage.tsx:160–181` `planFormError`+`status:"active"`；guidedcreate L109–120 断言完整 snake 体 | 无 |
| M018-002 | 英文码/错域中文 | L53–55 闭集字典；test L113–151 钉中文、不含 active/parsed/shop/POINTS_TOO_FEW | 无 |
| M018-003 | 导入/发布用错 manage 码、下钻无门 | L75–76 `IMPORT_VIEW/MANAGE`；L279/L297 Can；L288/L357 PermissionButton/HighRiskConfirm；test L259–270 | 无 |
| M018-004 | 下钻无 catch | L142–155 try/catch `setGlobalError`；test L272–277 403 含「无权」 | 无 |
| M018-005 | snake 幂等键被覆盖 | L177/L202/L223 `idempotencyKey`；三测试前缀 fp-/fpi-/fpp- | 无 |
| M018-006 | 主路径无失败断言 | 发布勾选/未勾选、导入名称、权限、403、幂等、存档/删除/审批/恢复、停用保存、降级均有例；本轮 34 例 | 无 |
| M018-007 | 来源名称可空 | L189–191；test L214–223 留空拦截 | 无 |
| M018-008 | 未勾选默认全发 | L213–220 只取 `publishCandidateIds`；test L242–248 confirm 未调用 | 无 |
| M018-009 | disabled 无恢复 | L202–211 恢复→`/unarchive`；test L157–164 | 无 |
| M018-010 | 首屏 Promise.all 被 projects 403 拖死 | L117–133 `loadSecondaryIfPermitted`；无 `governance.project.view` 不发请求；有权限失败只 notice。test L287–301：VIEW_ONLY `projectsRequests===0` 且表有「一层平面图」；403 表仍在且 degraded 匹配无权/加载失败，不报整页读取失败 | 无 |

---

## 6. Findings（本轮临时 GR）

无。不把已否 ID 重开；不把 M015-010/011 前端残留升新 ID。

四分类顺序已走：无冲突新证据（零英文闭集已落地；时刻草案冲突仍归 XM）；无新细化缺陷；无新纯技术缺陷；无添加项。

---

## 7. 观察（不进修复队列）

| OBS | 内容 | 处置 |
|---|---|---|
| OBS-01 | 空间种类/营业时间行无抽屉式见名可点；平面图主表用 IconButton 下钻。PGM-001 全局原则，不单独立项以免把全后台表格模式扩成新判据 | 观察 |
| OBS-02 | 存档 `HighRiskConfirm` 默认「此操作不可逆」，但存档可取消 | 观察 |
| OBS-03 | SVG 缺 label 时解析器默认 `shape-N`，展示在标签列。来源 `FloorPlanImportParser.java:99` | 跨模块 |
| OBS-04 | 营业时间按日保存未接 GuidedFlowModal。BSM-005「正持续改造」 | 观察 |
| OBS-05 | `PlatformController.deleteSpaceType` 仍在，本页不调用；`approveDeleteSpaceType` 控制器无 `requirePermission`，服务内 master | 跨模块 M009 |
| OBS-06 | `zhOrDash` 与 `fpLabel` 同实现（`FloorPlanImportPage.tsx:56–61`） | ponytail shrink，无业务发明 |
| OBS-07 | Parser `errorMessage` 含 viewBox/svg 英文，本页 说明列原样展示 | 跨模块 |
| OBS-08 | 营业时间「已生效」+不传 `effectiveFrom` = M015-010 XM 前端残留 | 不新开 |
| OBS-09 | 发布 POST 无服务端确认字段 = M015-011 XM | 不新开 |
| OBS-10 | 本机未跑 Maven/`complex_test`；后端以源码原文对照 | 证据边界 |
| OBS-11 | 现工作区 write-tree `53bd11c5…` ≠ after `c9301ef0…`，差在审计产物；7 文件 blob 全等 | 事实 |
| OBS-12 | 种类/时刻 vitest 用 mock client，wire snake 由 AdminApi 生产路径保证；平面图两测试文件用真实 AdminApi | 证据边界 |

ponytail-review（不落地）：`FloorPlanImportPage.tsx:L56-61: shrink: zhOrDash 与 fpLabel 同实现。留一个。` net: -3 行可能。Lean 其余。

---

## 8. 可执行证据（本机，只读）

### 8.1 vitest

命令（cwd=`/home/james/complex-management/apps/web`）：

```
npx vitest run src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx src/admin/pages/FloorPlanImportPage.test.tsx src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx src/admin/pages/VenueBusinessHoursPage.test.tsx
```

输出要点：

```
✓ src/admin/pages/VenueBusinessHoursPage.test.tsx (5 tests) 301ms
✓ src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx (10 tests) 362ms
✓ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx (4 tests) 403ms
✓ src/admin/pages/FloorPlanImportPage.test.tsx (15 tests) 647ms
Test Files  4 passed (4)
     Tests  34 passed (34)
Duration  2.24s
```

EXIT 0。jsdom `getComputedStyle` 伪元素 Not implemented 警告，0 失败。

### 8.2 tsc

命令（cwd=`/home/james/complex-management/apps/web`）：

```
npx tsc -b --noEmit
```

无诊断输出。EXIT 0。

### 8.3 未跑

后端测试、Maven、psql/`complex_test`：160 提示词写明本机只有 node。后端契约以 Java 源码原文对照（类型 1 的对照，非本模块可执行产物）。

---

## 9. 文件覆盖表（全文，非抽样）

| 文件 | 行跨度 | 覆盖 | 三线落点 |
|---|---|---|---|
| FloorPlanImportPage.tsx | 1–443 | 全文 | §2.1 三问；§3 GEN/BSM/PGM；§4 四账号/发布/幂等；§5 001–005,007,008,010 |
| FloorPlanImportPage.guidedcreate.test.tsx | 1–148 | 全文 | §2.2；§5 001/005 |
| FloorPlanImportPage.test.tsx | 1–302 | 全文 | §2.3；§5 002–008,010 |
| SpaceTypesPage.tsx | 1–280 | 全文 | §2.4；§4 存档删除审批恢复；§5 009 |
| SpaceTypesPage.guidedcreate.test.tsx | 1–166 | 全文 | §2.5；§5 006/009 |
| VenueBusinessHoursPage.tsx | 1–248 | 全文 | §2.6；§4 时刻保存/首屏权限；M015-010 XM |
| VenueBusinessHoursPage.test.tsx | 1–119 | 全文 | §2.7；§5 006 停用/降级 |

无 OVERSIZE。无只看 diff。无复用 B2/B4 票作为本轮通过依据。

---

## 10. 本席结论

- 三线：第一性原理、顺从式 21/21、对抗式（四账号×三页、写路径、中文、幂等、分批发布）均已执行并落证据
- 临时 finding：无
- 添加项：无
- 未答 questions：无
- 锁外判据：无
- HTML 自身问题：未发现需铁律 4 上报项
- 001–010：独立复验已落地，无回归
- M015-010/011：维持 XM，不重复立项
- 哈希行：本文第一行，三工具同值
- **单席审计结论：PASS**

本票不等于模块收口。双席合取与 B5 后四值校验由席甲在双方 raw 封存后进行。
