CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M018 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙（grok-4.6 / xhigh）
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁；不问人）
本文件路径：docs/audit-2026-09/modules/M018-b4-r1-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲为作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。本轮为该模块第 1 次 B3 之后的 B4。

未读取：任何 `M018-b4-r1-self-raw.md`（落盘时该路径已存在，本席按独立性禁令整文件不打开）。已读：`160-audit-prompt.md` V0.17 B4 与 §5 闸 3、findings.md V2.54 顶部「当前 M018 B2-r1 双PASS定稿」、本席旧 `M018-b1-grok-raw.md` V0.01/V0.02（B2 非阻断勘误：错误字段实产 6 键含 `polygon_points`）、冻结 diff 与两 tree。定稿队列以 findings.md V2.54 为准：本批修复队列「是」仅 **M018-001…009**。GR-06/07 并入 M015-010/011（否/XM）本轮不修。ledger 只读取模块状态与 B3 次数；其中 after tree 之后写入的席甲 B4 封存行不构成本席证据。本票以两 tree 全文 diff、定稿最小处置、RED/GREEN 日志与本席复跑为准。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；001–009 是否按定稿最小处置实现；是否最懒可行（ponytail ultra）；是否引入新缺陷或新业务发明；每一变更行能否归因到 001–009；是否越界改后端 / OpenAPI / 三份 HTML / 锁 / sidecar / 其它模块源码；测试是否先红后绿且断言有效。派发点名两项同根核：`REF_TYPE_LABELS` 按后端 REF_TYPES 六值是否属 002；页面描述句「已删除的种类可恢复为启用」是否属 009。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例（未勾选是否仍全发、snake 幂等键是否仍被覆盖、disabled 行是否改新端点、数量文案是否发明全发规则），未冒充调用 grilling 子代理。

---

## 1. 输入与独立锁实算

本席哈希命令：`python3 hashlib` + `sha256sum` + `openssl dgst -sha256`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 正文一行同值。ledger 顶部基准同值。criteria-lock 版本仍 V0.11。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。sidecar blob 两树均为 `41f575026b693af74d2a36e6faf86d260ef6f6ca`。

| 项 | 值 |
|---|---|
| 模块 | M018 空间点位、楼层平面图与营业时段／前台页面／第1段（7 文件） |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 本轮修复队列 | M018-001…009（B2-r1 定稿「是」）；GR-06/07→M015-010/011 本轮不修 |
| B3 | 已执行 1 次（本轮为第 1 次），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `09c32950a7ba0c910270efaebb82dadac40660da` |
| after（对象类型 tree，可解析） | `e2d60c3c41a49a27547081ea6c411ba88155bc4e` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 09c32950a7ba0c910270efaebb82dadac40660da e2d60c3c41a49a27547081ea6c411ba88155bc4e`（无路径过滤） |
| 实际差异 | 57181 bytes；SHA256 `376b8265c2fcc9e5131694319a70d3ab7b3cb7a040906cb85077012e174ddf1e`；6 files changed, 417 insertions(+), 105 deletions(-)；36 hunk；UTF-8 |
| 与冻结文件 `/home/james/audit-tmp/m018_b3_r1.diff` | 字节、SHA256、内容全等（`live == frozen` True） |
| `git diff --check` | exit 0 |
| `git diff --name-status` | 6 处 `M`，无 A/D/R |
| 两树路径清单 | before 3804 / after 3804；增删路径空 |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`（工作区 6 改动文件 blob 与 after tree 全等） |
| findings.md | 两树 blob `a3220c4cc541361abd2f73fcfaba02c7f1b2b37f`（工作区 hash-object 同值，V2.54）；本 diff 未改 |
| ledger.md | 两树 blob `a2bf76d170b8fd3a43f56f2a9be7b8bef5889495`（本 diff 未改；工作区 ledger 在 after tree 之后另写 B3/B4 台账） |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3`，完全相同 |
| OpenAPI | 两树相同：yaml `1f056b360dc60c37b6c026c761f0fa57b3688e63` / json `32231790707d8a93a303ca47033b7819ee0e3b71` |
| 后端 / 迁移 / 锁 | `git diff --name-only` 对 `backend`、`docs/openapi`、三 HTML、`criteria-lock.md` 皆空 |
| HighRiskConfirm / Can / adminClient | 两树 blob 相同（`78e208ff` / `9b290087` / `2240d010`）；本 diff 复用既有 `precheck` / `Can` / `idempotencyKey` 契约 |

六改动文件 blob（工作区 `git hash-object` = after）与内容 SHA256（本席 `sha256sum`）：

| 文件 | before blob | after / work blob | 行 | 内容 SHA256 |
|---|---|---|---:|---|
| `FloorPlanImportPage.tsx` | `b3247bf3a858d6a4e9d9db9dad84a55b25538273` | `4e6a47446676673c5b3f09044ef7ad332148b9e6` | 432 | `47e1a50d97ca7b59daf107046020d635cfc3934eeff23326bc0dd6406e378cb6` |
| `FloorPlanImportPage.guidedcreate.test.tsx` | `6dfa59cf8ed2c1e642188099d1984b1d33e315ad` | `4132987d33f51ebf973d6920bfae87c6086e5063` | 148 | `5790a81e551db768b9ee12b23418bd949cddc5e83bd326ce40cd8f9d33a0fa97` |
| `FloorPlanImportPage.test.tsx` | `a36d6713f37d71bb721ba89e10e29bb367fdce0b` | `92776d912f3841cb9cd51b88236524b34cf5311a` | 272 | `a08a7ec69263e2ef3b08d367ec543bc28105553cb69f1dce54a7167d20da705d` |
| `SpaceTypesPage.tsx` | `a0f9bc715198daa38646c68d29ff768166875b2f` | `856a0545618948ce7142cfa144d79e88398fa790` | 280 | `4d450b0df0de1acdba22e9dec777f8fa11159defd395a3601b19171efd88deed` |
| `SpaceTypesPage.guidedcreate.test.tsx` | `0fec42e1813af7a6f1ca06edc3fc25a806779317` | `01ccdd77db8126f923477fb03973df9e0bf7027b` | 166 | `db80bdf26bdd9ce931e72591d666faf168ca06ad2aa993f91d792feefdfa9e4b` |
| `VenueBusinessHoursPage.test.tsx` | `a8545e5337c5c7b490edf5049d25a6ba1ff921ac` | `b0ae055e6c3a821e0d187bf63857f9b473da1846` | 119 | `60f3642dd6161bbabffd75483af9ba9a05d84176ebf7e7461e8c8855c6e6118c` |

路径均在 `apps/web/src/admin/pages/`。未改的模块第 7 文件 `VenueBusinessHoursPage.tsx` 两树/工作区 blob `45237715ed4e48d96d0c7d8ecaf8c62806a5395e`（内容 SHA256 `841c1e543bd573d6fbfa7e2773f7aa391e58cee430695199e5bc105be32973a9`）。006 只改其测试，符合「只改 4 个测试文件」。

`git diff --numstat`：61/26 guidedcreate；143/26 FloorPlanImportPage.test；95/42 FloorPlanImportPage.tsx；72/12 SpaceTypes guidedcreate；12/1 SpaceTypesPage；34/3 Venue test。

本席未改任何源文件，只新建本 raw。验证：主仓只读 `npx vitest run` 四文件 + `npx tsc -b --noEmit`。未 commit/push/PR/merge/部署；未对主索引 `git add`；未新建脚本/日志/临时产物。

---

## 2. 锁、分类与本轮队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M018 的集合。本轮只用定稿 001–009 已引用的 ID。不 LOCK-AMEND。四分类本轮不重开。

B2-r1 定稿（findings.md V2.54）最小处置：

- **001** 细化：弹窗补楼层必填、底图地址/宽/高、版本号；提交 `status:"active"`；validate/summary 同步；guidedcreate 断言完整 body + 缺楼层拦截。不发明 draft、不改后端。
- **002** 冲突：页内字典改后端闭集（平面图/任务/候选 + 错误字段/10 码）；去掉全局码表。B2 本席非阻断勘误：错误字段按解析器 6 键含 `polygon_points`。只改本页 + `.test`。
- **003** 细化：创建导入/发布用 `floor_plan.import.manage`；两处下钻 `Can` 包裹 `floor_plan.import.view`；新增平面图仍 `floor_plan.manage`；+1 无 import.manage 禁用例。路由不改。
- **004** 技术性：两 load try/catch → `setGlobalError((e as Error).message)`；+1 403 例。
- **005** 技术性：三处 `idempotencyKey`；断言键以 `fp-` 开头。
- **006** 技术性：四测试文件补最小用例（覆盖 002–009 与营业时间停用/降级；001 用例外挂 001）。
- **007** 细化：来源名称非空，「请填写来源名称」。
- **008** 细化：未勾选 precheck 拦截 `setGlobalError("请勾选要发布的区域")`，不默认全发；`ids` 只取勾选。派发补充：precheck+守卫，确认文案含数量。
- **009** 细化：disabled 行「恢复」走既有 `/unarchive`，文案「把已删除种类「X」恢复为启用」。不新增端点/状态。

禁止：改 FloorPlanService/Parser/Controller/OpenAPI/V023、发明 draft、改全局 `statusLabels`、新增恢复端点、未勾选改成「默认全发加文案」、VenueHours 草案/生效日、二次确认契约字段。

---

## 3. 技能预检与本步收窄口径

本席环境实核（列出目录 / 读 SKILL.md；缺则记缺口，不冒充已调用）：

| 技能 | 存在 | 本步用法 |
|---|---|---|
| using-superpowers | 是 | 开工技能路由；用户 160 提示词优先于技能本体 |
| using-arc | 是 | 用户指令优先；本步是 160 B4 只读，不走 Arc audit 子代理（本体要派评审并写 `docs/arc/audits/`，记缺口） |
| graft | 是 | `graft grep REF_TYPES` / `polygon_points`：后端闭集六值与解析器 JSON 字段落点 |
| long-running-background-tasks | 是 | 已读；vitest/tsc 在本席命令内跑完，未另挂 monitor |
| grilling / grill-me | 是 | 只借追问结构自答；本体「等用户」「dispatch sub-agent」作废 |
| batch-grill-me | **缺口** | 未冒充 |
| ubiquitous-language | **缺口** | 未冒充 |
| ponytail（ultra）+ ponytail-review | 是 | 核最小处置；结论 Lean already |
| systematic-debugging | 是 | 只借读错误→复现→追数据流；不插桩、不改生产 |
| test-driven-development / tdd | 是 | 核 RED 失败原因与 GREEN 断言 |
| requesting-code-review | 是 | B4 请审=本席；不派子代理、不用 HEAD~1 |
| receiving-code-review | 是 | 外审否决席，不 IMPLEMENT |
| verification-before-completion | 是 | PASS 前复跑 vitest 4 文件 32 例 + tsc |
| diagnosing-bugs | 是 | RED 日志作 tight loop；不进入修复 |
| codebase-design | 是 | 页为浅适配器；本 diff 未抬新缝 |
| writing-for-agents | 是 | 本 raw 给席甲合取 / 下游 B5 读 |
| git-guardrails-claude-code | 是（可选） | 未装 hook；等价：不 fetch/commit/push/merge/hard reset |
| i-have-adhd | 是 | CLI 按 V0.17 适配器收窄 |
| resolving-merge-conflicts | 是 | 无冲突 |
| audit-context-building / differential-review / Arc audit | 是 | **不用**（第三席 + §7 外产物） |
| 划掉的 skills | — | 按提示词 §6 不用 |

对抗式自问（grilling 结构，本席自答）：

1. `REF_TYPE_LABELS` 六值是否越界？否。修前已是页内字典 `{space, merchant, facility}`，`facility` 属 SHAPE_TYPES 不是 REF_TYPES；修后精确等于 `FloorPlanService.REF_TYPES`。002 根因是页内字典≠后端闭集 + PGM-001 零英文。同根。
2. 描述句「已删除的种类可恢复为启用」是否越界？否。009 给 disabled 行补恢复；页头本就陈述新增/存档/删除审批生命周期，这一句是同一入口的页面说明，上位 BSM L571「可恢复」，不新造状态/端点。
3. 未勾选是否仍全发？否。`precheck` 在 `window.confirm` 前返回 false；`publishImport` 再守卫；删除 pending 回填。
4. snake 幂等键是否仍被覆盖？否。三处改为 `idempotencyKey`。GREEN 断言 `fp-`/`fpi-`/`fpp-`（真实 `normalizeRequestBody`）。
5. disabled 行是否改新端点？否。`unarchiveType(row)`，测试 POST `/unarchive`。
6. 确认文案含数量是否发明全发？否。impact 写「N 个**已勾选**候选」；未勾选确认框不弹出。
7. 有无归因不到 001–009 的行？无。36 hunk 全部可归因。

---

## 4. 完整未过滤 binary diff

命令：`git diff --binary 09c32950a7ba0c910270efaebb82dadac40660da e2d60c3c41a49a27547081ea6c411ba88155bc4e`

字节 57181；SHA256 `376b8265c2fcc9e5131694319a70d3ab7b3cb7a040906cb85077012e174ddf1e`；与 `/home/james/audit-tmp/m018_b3_r1.diff` 全等。UTF-8 完整原文见文末 ````diff` 块（本席从冻结文件原样嵌入，未手抄、未过滤）。

---

## 5. 逐块归因表（36 hunk；闸 3）

归因判据：每一变更行须能指到 findings.md V2.54 已定稿且归属「是」的 ID。验证性测试归因到对应 ID 合法。同根（同一闭集/同一入口的必要字典或页面说明）合法。

| # | 文件 | hunk | 归属 | 理由 |
|---|---|---|---|---|
| 01 | FloorPlanImportPage.guidedcreate.test.tsx | 注释改 001/005 契约 | 001/005 | 测试说明指向定稿请求体与幂等键 |
| 02 | 同上 | 夹具 status published→active | 001/002 | 夹具改后端闭集；001 提交 active |
| 03 | 同上 | lastPostBody 类型收窄 | 001/005 | 为完整 body / idempotency_key 断言服务 |
| 04 | 同上 | openCreate / fillPlanForm | 001 | 采集底图/宽高/版本的测试缝 |
| 05 | 同上 | 创建断言改写 + 缺楼层/底图例 | 001/005 | 定稿「完整 body + status active + 缺楼层拦截 + fp- 前缀」 |
| 06 | FloorPlanImportPage.test.tsx | 夹具闭集 + 注释 | 002 | published/draft/processing/INVALID_POLYGON → active/archived/parsed/POINTS_TOO_FEW |
| 07 | 同上 | errorEnvelope | 004 | 403 显错用例的 HTTP 信封 |
| 08 | 同上 | makeApi POST 捕获 / 403 / 权限夹具 / openDetails | 003/004/005/006/007/008 | 发布/权限/下钻/导入用例共享缝 |
| 09 | 同上 | 英文全清四例改写 | 002 | 启用/已存档/已解析/待复核/顶点不足；RED 打中「有效/parsed/待处理/未识别」 |
| 10 | 同上 | 候选抽屉中文 | 002 | 待复核替代 formatBusinessStatus |
| 11 | 同上 | 错误抽屉中文 | 002 | 顶点/顶点不足替代「未识别」 |
| 12 | 同上 | 导入名称/发布/权限/403 新例 | 003/004/005/006/007/008 | 006 清单 + 各 ID 最小例 |
| 13 | FloorPlanImportPage.tsx | 去 formatBusinessStatus，引入 Can | 002/003 | 去掉全局码表；下钻 Can 包裹所需 |
| 14 | 同上 | 错误字段/码/三套状态字典 + REF_TYPE_LABELS | 002 | 见 §6 同根核 |
| 15 | 同上 | IMPORT_VIEW/MANAGE + EMPTY_PLAN_FORM | 001/003 | 权限码与 001 表单初值 |
| 16 | 同上 | planForm 改用 EMPTY_PLAN_FORM | 001 | 底图/宽高/版本进入状态 |
| 17 | 同上 | load try/catch；createPlan 校验/body/camel 键 | 001/004/005 | 004 catch；001 必填+active；005 camel |
| 18 | 同上 | importFormError + 导入 camel 键 | 005/007 | 来源名称必填；idempotencyKey |
| 19 | 同上 | requireSelectedCandidates + 发布 camel 键 | 005/008 | 不默认全发；幂等键 |
| 20 | 同上 | 候选抽屉状态 fpLabel | 002 | 不再 formatBusinessStatus |
| 21 | 同上 | 打开弹窗重置 EMPTY_PLAN_FORM | 001 | 与新字段同步 |
| 22 | 同上 | 下钻 IconButton 包 Can IMPORT_VIEW | 003 | 定稿「两处下钻 Can 包裹」之一 |
| 23 | 同上 | 创建导入 IMPORT_MANAGE；任务状态 fpLabel；候选下钻 Can | 002/003 | 权限码 + 已解析中文 |
| 24 | 同上 | 候选表状态 fpLabel | 002 | 待复核 |
| 25 | 同上 | HighRiskConfirm precheck + 数量文案 + IMPORT_MANAGE | 003/008 | 见 §6 |
| 26 | 同上 | 步描述去掉「选填/草稿」 | 001 | validate/summary 同步 |
| 27 | 同上 | 底图地址/宽/高/版本输入 | 001 | 定稿采集字段 |
| 28 | 同上 | summary 加底图/宽高/版本/状态「启用」 | 001 | 不发明草稿 |
| 29 | 同上 | 导入步 validate=importFormError | 007 | 来源名称进入步内校验 |
| 30 | SpaceTypesPage.guidedcreate.test.tsx | LIFECYCLE_ROWS | 006/009 | disabled/archived/pending_delete 夹具 |
| 31 | 同上 | makeApi 可注入 rows；MASTER_USER | 006 | 审批/恢复用例 |
| 32 | 同上 | 恢复可见 + 生命周期 describe | 006/009 | 存档/申请删除/审批/恢复 |
| 33 | SpaceTypesPage.tsx | 页头描述加「已删除的种类可恢复为启用。」 | 009 | 见 §6 同根核 |
| 34 | 同上 | disabled 行「恢复」HighRiskConfirm | 009 | 走 unarchive；文案命中定稿 |
| 35 | VenueBusinessHoursPage.test.tsx | makeApi spacesFail | 006 | 降级用例缝 |
| 36 | 同上 | 停用 disabled:true + 降级提示 | 006 | 定稿「营业时间停用 1、降级提示 1」 |

无未归因 hunk。无新文件、无删除文件、无锁/HTML/后端/OpenAPI 行。

---

## 6. 内容审计（按 ID；最小处置 / 回归 / 业务发明）

### 001 — PASS
弹窗增加底图地址/宽/高/版本号；`planFormError` 楼层/底图/正宽高/版本号必填；POST 含 `image_url`/`image_width`/`image_height`/`version_no`/`status:"active"`（Number 宽高）。summary 状态「启用」，文案去掉「草稿」「楼层选填」。不改后端。RED：三例因缺 `floor-plan-image-url` 在修前弹窗失败（字段不存在=功能缺失）；GREEN 断言完整 body 与 `status:"active"`、`idempotency_key` `/^fp-/`。

### 002 — PASS（含 REF_TYPE_LABELS 同根）
去掉 `formatBusinessStatus`。页内三套状态字典用词与 B2 本席已核用词一致（启用/停用/已存档；已解析/解析失败/已发布/已取消；待复核/已发布/已驳回）。错误字段 6 键：`source_format`/`viewBox`/`points`/`polygon_points`/`shapes`/`source_payload`，与 `FloorPlanImportParser` 实产一致（B2 勘误，不得只抄候选表 5 键）。错误码 10 个与解析器闭集一致，中文短语与 `errorMessage` 同义（错误表「说明」列仍展示后端原文）。夹具改真实码。RED：`有效` 不含「启用」、`parsed` 直出、`待处理` 不含「待复核」、错误列「未识别」不含「顶点不足」。

**REF_TYPE_LABELS 六值 = 002 同根，非越界。** 修前该常量已存在且错误（`facility` 不是 REF_TYPES）。修后 `space/merchant/project/amusement_project/device/custom` 与 `FloorPlanService.java:36 REF_TYPES` 逐值相等。用词「空间/商户/综合体项目/游乐项目/设备/自定义」是既有域称谓，不新增枚举/状态机。002 的冲突根是页内字典对不上后端闭集以致英文或错域中文直出；关联类型同属该页字典、同属 PGM-001 零英文。派发「关联类型按 REF_TYPES」与此一致。

### 003 — PASS
`IMPORT_MANAGE` 用于创建导入 PermissionButton 与发布 HighRiskConfirm；新增平面图仍 `MANAGE=["floor_plan.manage"]`。两处 IconButton 包 `<Can permissions={IMPORT_VIEW}>`（无权限 fallback=null）。RED：无 import.manage 时 `floor-plan-import-open` 仍可点；无 import.view 时下钻按钮仍在。额外「无 import.view 隐藏下钻」是 003 实现的验证例，闸 3 允许。路由/adminRoutes 未改。

### 004 — PASS
两函数 `try { ... } catch (e) { setGlobalError((e as Error).message); }`。RED：403 信封下 `waitFor(setGlobalError 含「无权」)` 超时。真实 `AdminApi` 解析 403。未改 load 成功路径。

### 005 — PASS
createPlan / createImport / publishImport 三处 `idempotencyKey: *KeyRef.current`。其它字段仍 snake（定稿只改键名对齐 `normalizeRequestBody`）。断言 `fp-`/`fpi-`/`fpp-`。不改 adminClient。

### 006 — PASS
四测试文件整文件重写。32 例 = 既有例保留 + 各 ID 最小例。营业时间页实现未改，只补停用/降级表征例（RED 即绿，因修前实现已正确；断言 `disabled:true` 且时刻保留、降级 testid 含「加载失败」——改坏会红）。空间种类存档/申请删除/审批通过 RED 即绿（按钮本已存在）；恢复 RED 缺 `space-type-restore-old`。审批驳回 POST 无单独例：定稿「审批各 1」已被 `decision=approved` + 非 master「待最高权限审批」覆盖。记观察，不升越界。

### 007 — PASS
`importFormError` 先检 `sourceName.trim()`，文案「请填写来源名称」；导入步 `validate: importFormError`。RED：填 payload 不填名称后无 `floor-plan-import-guided-error`（修前只校验 payload，步进成功）。GREEN：错误含「来源名称」且 `lastPost` 空，补名称后 POST `source_name`。

### 008 — PASS（precheck+守卫+数量文案同根）
删除 `pendingCandidates.map` 回填。`requireSelectedCandidates`：空则 `setGlobalError("请勾选要发布的区域")` 返回 false。`HighRiskConfirm precheck={requireSelectedCandidates}`（组件在 `window.confirm` 前调用）。`publishImport` 再调同一守卫；`ids = publishCandidateIds`。impact：`本次发布 ${publishCandidateIds.length} 个已勾选候选`——数量是勾选范围的可见化，不是「未勾选改成默认全发加文案」。RED：`setGlobalError` 被 `null`（onStart）调用，未收到「请勾选要发布的区域」。勾选 A101 后 POST `candidate_ids:["cd-1"]` 不含 cd-2。

### 009 — PASS（描述句同根）
disabled 行 HighRiskConfirm label「恢复」，impact「把已删除种类「X」恢复为启用，可以再用来新建空间。」（定稿文案 + 与 archived 行「取消存档」同构的后半句），`onConfirm` 走既有 `unarchiveType`，权限 `MANAGE=["governance.space.manage"]`。不新增端点/状态。

**页头「已删除的种类可恢复为启用。」= 009 同根，非越界。** 修前描述已覆盖新增/存档/删除审批，未提恢复；009 补的正是「审批通过删除后本页无法恢复」。这一句是新按钮的页面说明，上位 L571「可恢复」，无新业务流程。闸 3 问能否归因，能指到 009，不是「顺手优化」无 ID 行。

### 未改边界 — PASS
三 HTML、OpenAPI yaml/json、criteria-lock 与 sidecar、backend、迁移、`statusLabels.ts`、`adminClient.ts`、`Can.tsx`、`HighRiskConfirm.tsx`、VenueBusinessHoursPage.tsx 实现：两树无 diff。未修 M015-010/011。

### ponytail ultra
Lean already。无新依赖/新组件/新端点。`requireSelectedCandidates` 四行复用 precheck 与守卫，不是多余抽象。`REF_TYPE_LABELS` 替换错误三值，不是新层。描述句 17 字。net: 无可删而不破坏定稿的行。

---

## 7. 测试有效性（先红后绿；断言能否失败于修前）

操作员证据：`/home/james/audit-tmp/m018_b3r1_run.log` RED_VITEST_EXIT=1 后 GREEN 4 文件 32 例、GREEN_TSC_EXIT=0。`m018_b3r1_tsc.log` 为空文件（0 字节）；本席不以空日志当 tsc 证据。

本席复跑（工作区 6 文件 = after tree）：

```
cd apps/web && npx vitest run src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx \
  src/admin/pages/FloorPlanImportPage.test.tsx \
  src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx \
  src/admin/pages/VenueBusinessHoursPage.test.tsx
```
Test Files 4 passed；Tests 32 passed；Duration 2.17s。

```
cd apps/web && npx tsc -b --noEmit
```
TSC_EXIT=0。

RED 日志（测试已落、页面未改）16 失败，能失败于修前实现的关键断言：

| ID | RED 实际失败 | 修前行为 |
|---|---|---|
| 001 | 缺 `floor-plan-image-url`（TestingLibraryElementError） | 弹窗无底图字段 |
| 002 | expected 含「启用」received「有效」；含「已解析」received `parsed`；含「待复核」received「待处理」；含「顶点不足」received「未识别」 | 全局码表/错字典 |
| 003 | import-open `toBeDisabled` 失败（aria-disabled=false）；下钻按钮 `toBeNull` 失败 | manage 码错误；下钻无门 |
| 004 | waitFor setGlobalError 含「无权」超时 | 无 catch |
| 007 | 缺 `floor-plan-import-guided-error` | 只校验 payload |
| 008 | setGlobalError 未以「请勾选要发布的区域」被调（实际 `null`） | 默认全发，先弹 confirm |
| 009 | 缺 `space-type-restore-old`（可见例与恢复 POST 例） | disabled 行无动作 |

TDD 杂质（不构成内容 FAIL）：(a) guidedcreate「名称留空」RED 先撞缺底图 testid，不是打中旧名称校验；GREEN 后该例仍能失败于去掉名称校验。(b) 006 对**修前已正确**的存档/审批/营业时间停用/降级是表征测试，RED 即绿；有效性在「改坏 decision/disabled:true/降级提示会红」，不在「对错误实现先红」。

005 的 `fp-` 前缀在 guidedcreate 创建例里，RED 因缺字段未跑到 POST 断言；GREEN 跑到且键为页面键。导入/发布例另断言 `fpi-`/`fpp-`，属 005 三处同根。

---

## 8. 最终票

| 轴 | 票 | 依据 |
|---|---|---|
| 内容 | **PASS** | 001–009 均按定稿最小处置落地；无回归；无业务发明；三 HTML/后端/OpenAPI/锁未改 |
| 归因 | **PASS** | 36 hunk / 6 文件全部可指到 001–009；`REF_TYPE_LABELS` 六值=002 同根；描述句=009 同根；确认数量文案=008 同根 |
| 总结论 | **PASS** | 两轴皆 PASS |

观察（不入本轮队列、不否决）：审批驳回 POST 无单独断言；guidedcreate 名称留空 RED 形态为缺 testid；006 表征例 RED 即绿；页内旧注释「状态码走全局表兜底」未改（不在变更行）；`zhOrDash`/`fpLabel` 修前已并存。

本席不行使否决。§5 闸 3 越界判定须双 PASS。本票不是模块 PASS，不是 B5。

---

## 9. 全文 diff 原文

下面为冻结 diff 原样（57181 bytes）。

````diff
diff --git a/apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx b/apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx
index 6dfa59cf..4132987d 100644
--- a/apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx
+++ b/apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx
@@ -6,9 +6,10 @@ import { AdminApi } from "../client/adminClient";
 import type { AdminPageProps, EffectivePermissionsState } from "../types";
 
 // 引导式流程 phase-10：新增平面图改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截不提交）。
-// 只覆盖「新增平面图」主创建流；导入任务/发布候选等动作不在本期改造范围。
+// M018-001/005：请求体按后端 FloorPlanService.validateCreateRequest / V023 契约（楼层、底图地址、底图宽高、版本号必填，status active），
+// 幂等键须是页面 KeyRef（经真实 AdminApi.normalizeRequestBody 后仍为 fp- 前缀）。
 const PLANS = [
-  { id: "fp-1", plan_name: "一层平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "published" },
+  { id: "fp-1", plan_name: "一层平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "active" },
 ];
 
 function dataEnvelope(data: unknown): Response {
@@ -21,7 +22,7 @@ function listEnvelope(items: unknown[]): Response {
   return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
 }
 
-let lastPostBody: unknown = null;
+let lastPostBody: Record<string, unknown> | null = null;
 let lastPostUrl = "";
 function makeApi(): AdminApi {
   vi.stubGlobal(
@@ -30,7 +31,7 @@ function makeApi(): AdminApi {
       const url = String(input);
       if ((init?.method ?? "GET").toUpperCase() === "POST" && url.endsWith("/floor-plans")) {
         lastPostUrl = url;
-        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
+        lastPostBody = init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : null;
         return Promise.resolve(dataEnvelope({ id: "fp-new" }));
       }
       if (url.endsWith("/floor-plans")) return Promise.resolve(listEnvelope(PLANS));
@@ -56,6 +57,28 @@ function renderPage() {
   return { setActionResult };
 }
 
+async function openCreate() {
+  await waitFor(() => expect(screen.getByTestId("floor-plan-create-open")).toBeTruthy());
+  fireEvent.click(screen.getByTestId("floor-plan-create-open"));
+}
+
+function fillPlanForm(overrides: Partial<Record<string, string>> = {}) {
+  const values: Record<string, string> = {
+    "floor-plan-project": "prj-1",
+    "floor-plan-name": "三层平面图",
+    "floor-plan-building": "B2",
+    "floor-plan-floor": "3F",
+    "floor-plan-image-url": "https://assets.example.com/floor-3.png",
+    "floor-plan-image-width": "1600",
+    "floor-plan-image-height": "1200",
+    "floor-plan-version": "v1",
+    ...overrides,
+  };
+  for (const [testId, value] of Object.entries(values)) {
+    fireEvent.change(screen.getByTestId(testId), { target: { value } });
+  }
+}
+
 describe("FloorPlanImportPage 引导式新增平面图(phase-10)", () => {
   afterEach(() => {
     vi.unstubAllGlobals();
@@ -70,39 +93,56 @@ describe("FloorPlanImportPage 引导式新增平面图(phase-10)", () => {
     expect(screen.queryByTestId("floor-plan-create-guided")).toBeNull();
   });
 
-  it("入口→步进→提交：POST 新建平面图带名称/楼栋/楼层，成功后关闭弹窗", async () => {
+  // M018-001/005：POST 体 = 后端必填契约（楼层/底图/宽高/版本号 + status active），幂等键为页面键
+  it("入口→步进→提交：POST 新建平面图带后端必填字段与 active 状态，成功后关闭弹窗", async () => {
     const { setActionResult } = renderPage();
-    await waitFor(() => expect(screen.getByTestId("floor-plan-create-open")).toBeTruthy());
-    fireEvent.click(screen.getByTestId("floor-plan-create-open"));
-
-    fireEvent.change(screen.getByTestId("floor-plan-project"), { target: { value: "prj-1" } });
-    fireEvent.change(screen.getByTestId("floor-plan-name"), { target: { value: "三层平面图" } });
-    fireEvent.change(screen.getByTestId("floor-plan-building"), { target: { value: "B2" } });
-    fireEvent.change(screen.getByTestId("floor-plan-floor"), { target: { value: "3F" } });
+    await openCreate();
+    fillPlanForm();
     fireEvent.click(screen.getByTestId("floor-plan-create-guided-next")); // 步1→确认
     await waitFor(() => expect(screen.getByTestId("floor-plan-create-guided-summary")).toBeTruthy());
+    expect(screen.getByTestId("floor-plan-create-guided-summary").textContent).toContain("启用");
+    expect(screen.getByTestId("floor-plan-create-guided-summary").textContent).not.toContain("草稿");
     fireEvent.click(screen.getByTestId("floor-plan-create"));
 
     await waitFor(() => expect(lastPostBody).toBeTruthy());
     expect(lastPostUrl).toContain("/floor-plans");
-    expect((lastPostBody as { project_id: string }).project_id).toBe("prj-1");
-    expect((lastPostBody as { plan_name: string }).plan_name).toBe("三层平面图");
-    expect((lastPostBody as { building_code: string }).building_code).toBe("B2");
-    expect((lastPostBody as { floor_no: string }).floor_no).toBe("3F");
-    expect((lastPostBody as { status: string }).status).toBe("draft");
+    expect(lastPostBody).toMatchObject({
+      project_id: "prj-1",
+      plan_name: "三层平面图",
+      building_code: "B2",
+      floor_no: "3F",
+      image_url: "https://assets.example.com/floor-3.png",
+      image_width: 1600,
+      image_height: 1200,
+      version_no: "v1",
+      status: "active",
+    });
+    expect(String(lastPostBody?.idempotency_key)).toMatch(/^fp-/);
     await waitFor(() => expect(screen.queryByTestId("floor-plan-create-guided")).toBeNull());
     expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "floor_plan_created" }));
   });
 
   it("步内校验拦截：名称留空则不进下一步、不提交", async () => {
     renderPage();
-    await waitFor(() => expect(screen.getByTestId("floor-plan-create-open")).toBeTruthy());
-    fireEvent.click(screen.getByTestId("floor-plan-create-open"));
-    fireEvent.change(screen.getByTestId("floor-plan-project"), { target: { value: "prj-1" } });
-    fireEvent.change(screen.getByTestId("floor-plan-name"), { target: { value: "  " } });
+    await openCreate();
+    fillPlanForm({ "floor-plan-name": "  " });
     fireEvent.click(screen.getByTestId("floor-plan-create-guided-next"));
     expect(screen.getByTestId("floor-plan-create-guided-error").textContent).toContain("名称");
     expect(screen.queryByTestId("floor-plan-create-guided-summary")).toBeNull();
     expect(lastPostBody).toBeNull();
   });
+
+  // M018-001：楼层为后端必填（floor_no NOT NULL），前端不再当选填
+  it("步内校验拦截：楼层或底图留空则不进下一步、不提交", async () => {
+    renderPage();
+    await openCreate();
+    fillPlanForm({ "floor-plan-floor": "" });
+    fireEvent.click(screen.getByTestId("floor-plan-create-guided-next"));
+    expect(screen.getByTestId("floor-plan-create-guided-error").textContent).toContain("楼层");
+    fillPlanForm({ "floor-plan-image-url": " " });
+    fireEvent.click(screen.getByTestId("floor-plan-create-guided-next"));
+    expect(screen.getByTestId("floor-plan-create-guided-error").textContent).toContain("底图");
+    expect(screen.queryByTestId("floor-plan-create-guided-summary")).toBeNull();
+    expect(lastPostBody).toBeNull();
+  });
 });
diff --git a/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx b/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
index a36d6713..92776d91 100644
--- a/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
+++ b/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
@@ -5,21 +5,22 @@ import { FloorPlanImportPage } from "./FloorPlanImportPage";
 import { AdminApi } from "../client/adminClient";
 import type { AdminPageProps, EffectivePermissionsState } from "../types";
 
+// 夹具只用后端闭集（FloorPlanService STATUSES / IMPORT_TASK_STATUSES / IMPORT_CANDIDATE_STATUSES、FloorPlanImportParser 错误字段/错误码）——M018-002。
 const PLANS = [
-  { id: "fp-1", plan_name: "一层平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "published" },
-  { id: "fp-2", plan_name: "二层平面图", building_code: "B1", floor_no: "2F", version_no: "v1", status: "draft" },
+  { id: "fp-1", plan_name: "一层平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "active" },
+  { id: "fp-2", plan_name: "二层平面图", building_code: "B1", floor_no: "2F", version_no: "v1", status: "archived" },
 ];
 const IMPORTS = [
-  { id: "imp-1", source_name: "cad导出", status: "processing", candidate_count: 3, error_count: 1 },
+  { id: "imp-1", source_name: "cad导出", status: "parsed", candidate_count: 3, error_count: 1 },
 ];
 const DETAILS = {
-  task: { id: "imp-1", status: "processing" },
+  task: { id: "imp-1", status: "parsed" },
   candidates: [
     { id: "cd-1", candidate_no: 1, shape_type: "shop", ref_type: "space", ref_id: "sp-1", label: "A101", status: "pending" },
     { id: "cd-2", candidate_no: 2, shape_type: "corridor", ref_type: "space", ref_id: "sp-2", label: "旧数据", status: "pending" },
   ],
   errors: [
-    { id: "er-1", line_no: 5, field_name: "geometry", error_code: "INVALID_POLYGON", error_message: "多边形无效" },
+    { id: "er-1", line_no: 5, field_name: "points", error_code: "POINTS_TOO_FEW", error_message: "多边形至少需要 3 个顶点" },
   ],
 };
 
@@ -32,14 +33,30 @@ function dataEnvelope(data: unknown): Response {
 function listEnvelope(items: unknown[]): Response {
   return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
 }
+function errorEnvelope(status: number, code: string, message: string): Response {
+  return new Response(
+    JSON.stringify({ success: false, code, message, data: null }),
+    { status, headers: { "content-type": "application/json" } },
+  );
+}
 
-function makeApi(): AdminApi {
+let lastPost: { url: string; body: Record<string, unknown> } | null = null;
+function makeApi(options: { importsStatus?: number } = {}): AdminApi {
   vi.stubGlobal(
     "fetch",
-    vi.fn().mockImplementation((input: RequestInfo | URL) => {
+    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
       const url = String(input);
+      if ((init?.method ?? "GET").toUpperCase() === "POST") {
+        lastPost = { url, body: init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : {} };
+        if (url.endsWith("/publish")) return Promise.resolve(dataEnvelope({ id: "imp-1", status: "published" }));
+        if (url.endsWith("/imports")) return Promise.resolve(dataEnvelope({ id: "imp-2", status: "parsed" }));
+        return Promise.resolve(dataEnvelope({ id: "fp-new" }));
+      }
       if (url.includes("/imports/imp-1/candidates")) return Promise.resolve(dataEnvelope(DETAILS));
-      if (url.includes("/floor-plans/fp-1/imports")) return Promise.resolve(listEnvelope(IMPORTS));
+      if (url.includes("/floor-plans/fp-1/imports")) {
+        if (options.importsStatus === 403) return Promise.resolve(errorEnvelope(403, "PERMISSION_DENIED", "当前账号无权查看导入任务"));
+        return Promise.resolve(listEnvelope(IMPORTS));
+      }
       if (url.endsWith("/floor-plans")) return Promise.resolve(listEnvelope(PLANS));
       if (url.includes("/platform/projects")) return Promise.resolve(dataEnvelope([{ id: "prj-1", name: "主项目" }]));
       return Promise.resolve(listEnvelope([]));
@@ -49,41 +66,62 @@ function makeApi(): AdminApi {
 }
 
 const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
+// 只授平面图读写 + 导入只读，不授 floor_plan.import.manage（V034 独立高危码）
+const NO_IMPORT_MANAGE: EffectivePermissionsState = {
+  status: "ready",
+  value: { grants: ["floor_plan.view", "floor_plan.manage", "floor_plan.import.view"], denies: [], wildcard: false },
+};
+const VIEW_ONLY: EffectivePermissionsState = { status: "ready", value: { grants: ["floor_plan.view"], denies: [], wildcard: false } };
 
-function renderPage() {
+function renderPage(perms: EffectivePermissionsState = ADMIN_PERMS, apiOptions: { importsStatus?: number } = {}) {
+  const setGlobalError = vi.fn();
+  const setActionResult = vi.fn();
   const fullProps: AdminPageProps = {
-    api: makeApi(),
+    api: makeApi(apiOptions),
     user: { id: "u1", username: "admin", accountType: "platform_admin" },
-    setActionResult: vi.fn(),
-    setGlobalError: vi.fn(),
-    effectivePermissions: ADMIN_PERMS,
+    setActionResult,
+    setGlobalError,
+    effectivePermissions: perms,
   };
   render(<FloorPlanImportPage {...fullProps} />);
+  return { setGlobalError, setActionResult };
 }
 
 // 打开候选/错误明细上下文（选平面图→选导入任务）后回调，供明细行断言复用。
-async function openDetails() {
-  renderPage();
+async function openDetails(perms: EffectivePermissionsState = ADMIN_PERMS) {
+  const spies = renderPage(perms);
   await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
   fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
   await waitFor(() => expect(screen.getByTestId("floor-plan-import-select-imp-1")).toBeTruthy());
   fireEvent.click(screen.getByTestId("floor-plan-import-select-imp-1"));
   await waitFor(() => expect(screen.getByTestId("floor-plan-candidates-table")).toBeTruthy());
+  return spies;
 }
 
-describe("FloorPlanImportPage 英文全清", () => {
+describe("FloorPlanImportPage 英文全清（M018-002：后端闭集全部有中文）", () => {
   afterEach(() => {
     vi.unstubAllGlobals();
+    lastPost = null;
   });
 
-  it("平面图表状态列渲染中文，不含原始英文码", async () => {
+  it("平面图表状态列按后端闭集渲染中文，不含原始英文码", async () => {
     renderPage();
     await waitFor(() => expect(screen.getByTestId("floor-plans-table")).toBeTruthy());
     const table = screen.getByTestId("floor-plans-table").textContent ?? "";
-    expect(table).toContain("已发布"); // published
-    expect(table).toContain("草稿"); // draft
-    expect(table).not.toContain("published");
-    expect(table).not.toContain("draft");
+    expect(table).toContain("启用"); // active
+    expect(table).toContain("已存档"); // archived
+    expect(table).not.toContain("active");
+    expect(table).not.toContain("archived");
+  });
+
+  it("导入任务表状态 parsed 渲染「已解析」，不直出英文", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
+    await waitFor(() => expect(screen.getByTestId("floor-plan-imports-table")).toBeTruthy());
+    const table = screen.getByTestId("floor-plan-imports-table").textContent ?? "";
+    expect(table).toContain("已解析");
+    expect(table).not.toContain("parsed");
   });
 
   it("候选明细表类型/关联/状态渲染中文，不含原始英文码", async () => {
@@ -91,15 +129,26 @@ describe("FloorPlanImportPage 英文全清", () => {
     const table = screen.getByTestId("floor-plan-candidates-table").textContent ?? "";
     expect(table).toContain("铺位"); // shapeType shop
     expect(table).toContain("空间"); // refType space
-    expect(table).toContain("待处理"); // status pending
+    expect(table).toContain("待复核"); // status pending
     expect(table).not.toContain("shop");
     expect(table).not.toContain("pending");
   });
+
+  it("导入错误表按解析器真实字段/错误码渲染中文，不显示未识别", async () => {
+    await openDetails();
+    const table = screen.getByTestId("floor-plan-errors-table").textContent ?? "";
+    expect(table).toContain("顶点"); // field points
+    expect(table).toContain("顶点不足"); // POINTS_TOO_FEW
+    expect(table).toContain("多边形至少需要 3 个顶点");
+    expect(table).not.toContain("POINTS_TOO_FEW");
+    expect(table).not.toContain("未识别");
+  });
 });
 
 describe("FloorPlanImportPage 行明细抽屉", () => {
   afterEach(() => {
     vi.unstubAllGlobals();
+    lastPost = null;
   });
 
   it("点候选行弹出抽屉，含类型/关联/状态中文，无英文枚举", async () => {
@@ -109,19 +158,20 @@ describe("FloorPlanImportPage 行明细抽屉", () => {
     const text = drawer.textContent ?? "";
     expect(text).toContain("铺位"); // shape_type shop
     expect(text).toContain("空间"); // ref_type space
-    expect(text).toContain("待处理"); // status pending
+    expect(text).toContain("待复核"); // status pending
     expect(text).toContain("A101"); // label
     expect(text).not.toContain("shop");
     expect(text).not.toContain("pending");
   });
 
-  it("点错误行弹出抽屉含行号/说明，Esc 关闭", async () => {
+  it("点错误行弹出抽屉含行号/字段/错误码中文与说明，Esc 关闭", async () => {
     await openDetails();
     fireEvent.click(screen.getByTestId("floor-plan-error-row-er-1"));
     const drawer = await screen.findByTestId("row-detail-drawer");
     const text = drawer.textContent ?? "";
-    expect(text).toContain("图形");
-    expect(text).toContain("多边形无效");
+    expect(text).toContain("顶点");
+    expect(text).toContain("顶点不足");
+    expect(text).toContain("多边形至少需要 3 个顶点");
     fireEvent.keyDown(window, { key: "Escape" });
     await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
   });
@@ -130,6 +180,7 @@ describe("FloorPlanImportPage 行明细抽屉", () => {
 describe("FloorPlanImportPage 闭集与导入格式", () => {
   afterEach(() => {
     vi.unstubAllGlobals();
+    lastPost = null;
   });
 
   it("未知候选种类显示未识别，不显示其他或走道", async () => {
@@ -152,4 +203,70 @@ describe("FloorPlanImportPage 闭集与导入格式", () => {
     expect(text).toContain("标准数据");
     expect(text).not.toContain("geojson");
   });
+
+  // M018-007/005/006：来源名称必填（后端 source_name NOT NULL）；POST 体与幂等键
+  it("创建导入：来源名称留空被步内拦截；填齐后 POST 带格式/名称/内容且幂等键为页面键", async () => {
+    renderPage();
+    await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
+    await waitFor(() => expect(screen.getByTestId("floor-plan-import-open")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("floor-plan-import-open"));
+    fireEvent.change(screen.getByTestId("floor-plan-import-payload"), { target: { value: "<svg viewBox=\"0 0 10 10\"></svg>" } });
+    fireEvent.click(screen.getByTestId("floor-plan-import-guided-next"));
+    expect(screen.getByTestId("floor-plan-import-guided-error").textContent).toContain("来源名称");
+    expect(lastPost).toBeNull();
+
+    fireEvent.change(screen.getByTestId("floor-plan-import-name"), { target: { value: "cad导出2" } });
+    fireEvent.click(screen.getByTestId("floor-plan-import-guided-next"));
+    await waitFor(() => expect(screen.getByTestId("floor-plan-import-guided-summary")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("floor-plan-import-create"));
+    await waitFor(() => expect(lastPost).toBeTruthy());
+    expect(lastPost?.url).toContain("/floor-plans/fp-1/imports");
+    expect(lastPost?.body).toMatchObject({ source_format: "svg", source_name: "cad导出2", source_payload: "<svg viewBox=\"0 0 10 10\"></svg>" });
+    expect(String(lastPost?.body.idempotency_key)).toMatch(/^fpi-/);
+  });
+});
+
+describe("FloorPlanImportPage 发布、权限与下钻错误（M018-003/004/005/008）", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    lastPost = null;
+  });
+
+  it("未勾选候选时发布被拦截（不弹确认、不请求）；勾选后确认 POST 只带勾选的候选且幂等键为页面键", async () => {
+    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
+    const { setGlobalError } = await openDetails();
+    fireEvent.click(screen.getByTestId("floor-plan-publish"));
+    expect(setGlobalError).toHaveBeenCalledWith("请勾选要发布的区域");
+    expect(confirmSpy).not.toHaveBeenCalled();
+    expect(lastPost).toBeNull();
+
+    fireEvent.click(screen.getByLabelText("A101"));
+    fireEvent.click(screen.getByTestId("floor-plan-publish"));
+    expect(confirmSpy).toHaveBeenCalledTimes(1);
+    await waitFor(() => expect(lastPost).toBeTruthy());
+    expect(lastPost?.url).toContain("/floor-plans/fp-1/imports/imp-1/publish");
+    expect(lastPost?.body).toMatchObject({ candidate_ids: ["cd-1"] });
+    expect(String(lastPost?.body.idempotency_key)).toMatch(/^fpp-/);
+  });
+
+  it("无 floor_plan.import.manage 时创建导入与发布按钮禁用（新增平面图仍可用）", async () => {
+    await openDetails(NO_IMPORT_MANAGE);
+    expect(screen.getByTestId("floor-plan-import-open")).toBeDisabled();
+    expect(screen.getByTestId("floor-plan-publish")).toBeDisabled();
+    expect(screen.getByTestId("floor-plan-create-open")).not.toBeDisabled();
+  });
+
+  it("无 floor_plan.import.view 时不显示下钻按钮", async () => {
+    renderPage(VIEW_ONLY);
+    await waitFor(() => expect(screen.getByTestId("floor-plans-table").textContent).toContain("一层平面图"));
+    expect(screen.queryByTestId("floor-plan-select-fp-1")).toBeNull();
+  });
+
+  it("下钻读取导入任务失败（403）时报错到全局，不静默", async () => {
+    const { setGlobalError } = renderPage(ADMIN_PERMS, { importsStatus: 403 });
+    await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
+    fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
+    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("无权")));
+  });
 });
diff --git a/apps/web/src/admin/pages/FloorPlanImportPage.tsx b/apps/web/src/admin/pages/FloorPlanImportPage.tsx
index b3247bf3..4e6a4744 100644
--- a/apps/web/src/admin/pages/FloorPlanImportPage.tsx
+++ b/apps/web/src/admin/pages/FloorPlanImportPage.tsx
@@ -4,12 +4,11 @@ import { AdminPageHeader } from "../components/AdminPageHeader";
 import { AuditTrailHint } from "../components/AuditTrailHint";
 import { TechDetails } from "../components/TechDetails";
 import { useAdminResource } from "../hooks/useAdminResource";
-import { PermissionButton } from "../security/Can";
+import { Can, PermissionButton } from "../security/Can";
 import { IconButton } from "../components/IconButton";
 import { UnorderedListOutlined, ProfileOutlined } from "@ant-design/icons";
 import { HighRiskConfirm } from "../components/HighRiskConfirm";
 import { GuidedFlowModal } from "../components/GuidedFlowModal";
-import { formatBusinessStatus } from "../i18n/statusLabels";
 import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
 import type { AdminPageProps } from "../types";
 
@@ -28,24 +27,39 @@ const SOURCE_FORMAT_OPTIONS: { value: string; label: string }[] = [
   { value: "svg", label: "矢量图" },
   { value: "standard_json", label: "标准数据" },
 ];
+// M018-002：页内枚举字典与后端闭集一致（FloorPlanImportParser 错误字段/错误码、FloorPlanService STATUSES / IMPORT_TASK_STATUSES / IMPORT_CANDIDATE_STATUSES / REF_TYPES），
+// 不再走全局码表（无 parsed/archived 键，published 在全局表是「已上架」）。
 const ERROR_FIELD_LABELS: Record<string, string> = {
-  geometry: "图形",
-  shape_type: "图形类型",
   source_format: "导入格式",
-  project_id: "所属项目",
+  viewBox: "画布范围",
+  points: "顶点",
+  polygon_points: "多边形顶点",
+  shapes: "图形列表",
+  source_payload: "来源内容",
 };
 const ERROR_CODE_LABELS: Record<string, string> = {
-  INVALID_POLYGON: "多边形无效",
-  VALIDATION_FAILED: "校验未通过",
+  PAYLOAD_REQUIRED: "缺少来源内容",
+  UNSUPPORTED_SOURCE_FORMAT: "不支持的导入格式",
+  VIEWBOX_REQUIRED: "缺少画布范围",
+  POINTS_REQUIRED: "缺少顶点",
+  POINTS_TOO_FEW: "顶点不足",
+  POLYLINE_NOT_CLOSED: "折线未闭合",
+  POINT_OUT_OF_RANGE: "顶点越界",
+  SHAPES_REQUIRED: "缺少图形列表",
+  JSON_PARSE_FAILED: "数据解析失败",
+  NO_SHAPES_FOUND: "未找到图形",
 };
+const FLOOR_PLAN_STATUS_LABELS: Record<string, string> = { active: "启用", inactive: "停用", archived: "已存档" };
+const IMPORT_TASK_STATUS_LABELS: Record<string, string> = { parsed: "已解析", failed: "解析失败", published: "已发布", cancelled: "已取消" };
+const CANDIDATE_STATUS_LABELS: Record<string, string> = { pending: "待复核", published: "已发布", rejected: "已驳回" };
 const zhOrDash = (map: Record<string, string>, code: string | null | undefined) =>
   code ? map[code] ?? "未识别" : "-";
-const REF_TYPE_LABELS: Record<string, string> = { space: "空间", merchant: "商户", facility: "设施" };
+const REF_TYPE_LABELS: Record<string, string> = {
+  space: "空间", merchant: "商户", project: "综合体项目", amusement_project: "游乐项目", device: "设备", custom: "自定义",
+};
 const fpLabel = (map: Record<string, string>, code: string | null | undefined) => (code ? map[code] ?? "未识别" : "-");
 function floorPlanStatusLabel(status?: string) {
-  if (status === "published") return "已发布";
-  if (status === "draft") return "草稿";
-  return formatBusinessStatus(status);
+  return fpLabel(FLOOR_PLAN_STATUS_LABELS, status);
 }
 
 // 后端 floorplan 域记录映射(接现有 /api/v1/floor-plans/* 契约)。
@@ -56,6 +70,11 @@ type ImportError = { id: string; lineNo?: number; fieldName?: string; errorCode?
 type FloorPlanImportDetails = { task?: ImportTask; candidates?: ImportCandidate[]; errors?: ImportError[] };
 
 const MANAGE = ["floor_plan.manage"];
+// M018-003：导入任务/候选的读写码与后端 FloorPlanController 一致（floor_plan.import.view / floor_plan.import.manage，V034 独立种子）。
+const IMPORT_VIEW = ["floor_plan.import.view"];
+const IMPORT_MANAGE = ["floor_plan.import.manage"];
+// M018-001：新增平面图表单字段与后端 validateCreateRequest / V023 必填列一致。
+const EMPTY_PLAN_FORM = { projectId: "", planName: "", buildingCode: "", floorNo: "", imageUrl: "", imageWidth: "", imageHeight: "", versionNo: "" };
 // 高危动作二次确认标题(发布候选写入正式平面图区域,不可静默覆盖):由 HighRiskConfirm 弹窗承载。
 const PUBLISH_CONFIRM_TITLE = "发布平面图导入候选";
 
@@ -74,7 +93,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
   const [imports, setImports] = React.useState<ImportTask[]>([]);
   const [selectedImportId, setSelectedImportId] = React.useState("");
   const [floorPlanImportDetails, setFloorPlanImportDetails] = React.useState<FloorPlanImportDetails | null>(null);
-  const [planForm, setPlanForm] = React.useState({ projectId: "", planName: "", buildingCode: "", floorNo: "" });
+  const [planForm, setPlanForm] = React.useState(EMPTY_PLAN_FORM);
   const [projects, setProjects] = React.useState<{ id: string; name?: string }[]>([]);
   const planKeyRef = React.useRef("");
   const importKeyRef = React.useRef("");
@@ -103,35 +122,52 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
     { onError: setGlobalError, fallbackMessage: "平面图读取失败", onStart: () => setGlobalError(null) },
   );
 
+  // M018-004：下钻读取失败（403/网络）不再静默——统一走 setGlobalError。
   const loadImports = async (floorPlanId: string) => {
     setSelectedPlanId(floorPlanId);
     setSelectedImportId("");
     setFloorPlanImportDetails(null);
     importKeyRef.current = "";
     publishKeyRef.current = "";
-    const res = await api.get<{ items?: ImportTask[] } | ImportTask[]>(`/api/v1/floor-plans/${floorPlanId}/imports`);
-    setImports(Array.isArray(res) ? res : (res?.items ?? []));
+    try {
+      const res = await api.get<{ items?: ImportTask[] } | ImportTask[]>(`/api/v1/floor-plans/${floorPlanId}/imports`);
+      setImports(Array.isArray(res) ? res : (res?.items ?? []));
+    } catch (e) { setGlobalError((e as Error).message); }
   };
 
   const loadCandidates = async (floorPlanId: string, importId: string) => {
     setSelectedImportId(importId);
     publishKeyRef.current = "";
     setPublishCandidateIds([]);
-    const details = await api.get<FloorPlanImportDetails>(`/api/v1/floor-plans/${floorPlanId}/imports/${importId}/candidates`);
-    setFloorPlanImportDetails(details);
+    try {
+      const details = await api.get<FloorPlanImportDetails>(`/api/v1/floor-plans/${floorPlanId}/imports/${importId}/candidates`);
+      setFloorPlanImportDetails(details);
+    } catch (e) { setGlobalError((e as Error).message); }
   };
 
   // 提交在确认步触发；校验失败/接口失败弹窗留开显错，成功才关。原接口/请求体保留（双保险再校验一次名称）。
+  // M018-001：请求体与后端 FloorPlanService.validateCreateRequest / V023 契约一致（楼层、底图地址、底图宽高、版本号必填；状态闭集 active/inactive/archived，无 draft）。
+  const planFormError = () => {
+    if (!planForm.projectId.trim()) return "请选择所属项目";
+    if (!planForm.planName.trim()) return "请填写平面图名称";
+    if (!planForm.floorNo.trim()) return "请填写楼层";
+    if (!planForm.imageUrl.trim()) return "请填写底图地址";
+    if (!(Number(planForm.imageWidth) > 0) || !(Number(planForm.imageHeight) > 0)) return "请填写大于 0 的底图宽高";
+    if (!planForm.versionNo.trim()) return "请填写版本号";
+    return null;
+  };
   const createPlan = async () => {
-    if (!planForm.projectId.trim()) { setPlanError("请选择所属项目"); return; }
-    if (!planForm.planName.trim()) { setPlanError("请填写平面图名称"); return; }
+    const formError = planFormError();
+    if (formError) { setPlanError(formError); return; }
     setPlanSaving(true);
     setPlanError("");
     try {
       if (!planKeyRef.current) planKeyRef.current = `fp-${Math.random().toString(36).slice(2)}`;
+      // M018-005：幂等键以 camel 键交给 AdminApi（normalizeRequestBody 只认 idempotencyKey，snake 键会被生成键覆盖，失败重试不再同键）。
       await api.post("/api/v1/floor-plans", {
-        idempotency_key: planKeyRef.current, plan_name: planForm.planName, project_id: planForm.projectId,
-        building_code: planForm.buildingCode, floor_no: planForm.floorNo, status: "draft",
+        idempotencyKey: planKeyRef.current, plan_name: planForm.planName, project_id: planForm.projectId,
+        building_code: planForm.buildingCode, floor_no: planForm.floorNo, image_url: planForm.imageUrl,
+        image_width: Number(planForm.imageWidth), image_height: Number(planForm.imageHeight), version_no: planForm.versionNo, status: "active",
       });
       setActionResult({ action: "floor_plan_created", planName: planForm.planName });
       setPlanOpen(false);
@@ -139,13 +175,21 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
     } catch (e) { setPlanError((e as Error).message); } finally { setPlanSaving(false); }
   };
 
+  // M018-007：来源名称与后端 validateImportCreateRequest（source_name 必填）对齐。
+  const importFormError = () => {
+    if (!importForm.sourceName.trim()) return "请填写来源名称";
+    if (!importForm.sourcePayload.trim()) return "请填写来源内容";
+    return null;
+  };
   const createImport = async () => {
+    const formError = importFormError();
+    if (formError) { setImportError(formError); return; }
     setImportSaving(true);
     setImportError("");
     try {
       if (!importKeyRef.current) importKeyRef.current = `fpi-${selectedPlanId}-${Math.random().toString(36).slice(2)}`;
       const res = await api.post<ImportTask>(`/api/v1/floor-plans/${selectedPlanId}/imports`, {
-        idempotency_key: importKeyRef.current, source_format: importForm.sourceFormat,
+        idempotencyKey: importKeyRef.current, source_format: importForm.sourceFormat,
         source_name: importForm.sourceName, source_payload: importForm.sourcePayload,
       });
       setActionResult({ action: "floor_plan_import_created", importId: res?.id });
@@ -155,13 +199,18 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
     } catch (e) { setImportError((e as Error).message); } finally { setImportSaving(false); }
   };
 
+  // M018-008：只发布勾选的候选（勾选＝人工复核），未勾选不默认全发。
+  const requireSelectedCandidates = () => {
+    if (publishCandidateIds.length === 0) { setGlobalError("请勾选要发布的区域"); return false; }
+    return true;
+  };
   const publishImport = async () => {
     try {
-      const ids = publishCandidateIds.length > 0 ? publishCandidateIds : pendingCandidates.map((item) => item.id).filter(Boolean);
-      if (ids.length === 0) { setGlobalError("没有待发布的区域"); return; }
+      if (!requireSelectedCandidates()) return;
+      const ids = publishCandidateIds;
       if (!publishKeyRef.current) publishKeyRef.current = `fpp-${selectedImportId}-${Math.random().toString(36).slice(2)}`;
       const result = await api.post(`/api/v1/floor-plans/${selectedPlanId}/imports/${selectedImportId}/publish`, {
-        idempotency_key: publishKeyRef.current, candidate_ids: ids,
+        idempotencyKey: publishKeyRef.current, candidate_ids: ids,
       });
       setActionResult({ action: "floor_plan_import_published", candidateIds: ids, result });
       await loadCandidates(selectedPlanId, selectedImportId);
@@ -176,7 +225,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
         { label: "类型", value: fpLabel(SHAPE_TYPE_LABELS, candidate.shapeType) },
         { label: "关联", value: `${fpLabel(REF_TYPE_LABELS, candidate.refType)}：${candidate.refId ?? "—"}` },
         { label: "标签", value: candidate.label ?? "—" },
-        { label: "状态", value: formatBusinessStatus(candidate.status) },
+        { label: "状态", value: fpLabel(CANDIDATE_STATUS_LABELS, candidate.status) },
         { label: "候选编号", value: candidate.id },
       ],
     });
@@ -207,7 +256,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
             permissionsState={effectivePermissions}
             permissions={MANAGE}
             data-testid="floor-plan-create-open"
-            onClick={() => { setPlanForm({ projectId: "", planName: "", buildingCode: "", floorNo: "" }); planKeyRef.current = ""; setPlanError(""); setPlanOpen(true); }}
+            onClick={() => { setPlanForm(EMPTY_PLAN_FORM); planKeyRef.current = ""; setPlanError(""); setPlanOpen(true); }}
           >＋ 新增平面图</PermissionButton>
         </div>
         <table className="admin-table" data-testid="floor-plans-table">
@@ -217,7 +266,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
               <tr key={plan.id}>
                 <td>{plan.planName ?? plan.id}</td><td>{plan.buildingCode ?? "-"}/{plan.floorNo ?? "-"}</td>
                 <td>{plan.versionNo ?? "-"}</td><td>{floorPlanStatusLabel(plan.status)}</td>
-                <td><IconButton icon={<UnorderedListOutlined />} tip="查看导入任务" onClick={() => void loadImports(plan.id)} testId={`floor-plan-select-${plan.id}`} /></td>
+                <td><Can permissionsState={effectivePermissions} permissions={IMPORT_VIEW}><IconButton icon={<UnorderedListOutlined />} tip="查看导入任务" onClick={() => void loadImports(plan.id)} testId={`floor-plan-select-${plan.id}`} /></Can></td>
               </tr>
             ))}
           </tbody>
@@ -226,16 +275,16 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
           <>
             <h3>导入任务</h3>
             <div className="admin-form-row" data-testid="floor-plan-import-form">
-              <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="floor-plan-import-open" onClick={() => { setImportForm({ sourceFormat: "svg", sourceName: "", sourcePayload: "" }); importKeyRef.current = ""; setImportError(""); setImportOpen(true); }}>＋ 创建导入</PermissionButton>
+              <PermissionButton permissionsState={effectivePermissions} permissions={IMPORT_MANAGE} data-testid="floor-plan-import-open" onClick={() => { setImportForm({ sourceFormat: "svg", sourceName: "", sourcePayload: "" }); importKeyRef.current = ""; setImportError(""); setImportOpen(true); }}>＋ 创建导入</PermissionButton>
             </div>
             <table className="admin-table" data-testid="floor-plan-imports-table">
               <thead><tr><th>来源</th><th>状态</th><th>候选数</th><th>错误数</th><th>操作</th></tr></thead>
               <tbody>
                 {imports.map((task) => (
                   <tr key={task.id}>
-                    <td>{task.sourceName ?? task.id}</td><td>{formatBusinessStatus(task.status)}</td>
+                    <td>{task.sourceName ?? task.id}</td><td>{fpLabel(IMPORT_TASK_STATUS_LABELS, task.status)}</td>
                     <td>{task.candidateCount ?? 0}</td><td>{task.errorCount ?? 0}</td>
-                    <td><IconButton icon={<ProfileOutlined />} tip="查看候选明细" onClick={() => void loadCandidates(selectedPlanId, task.id)} testId={`floor-plan-import-select-${task.id}`} /></td>
+                    <td><Can permissionsState={effectivePermissions} permissions={IMPORT_VIEW}><IconButton icon={<ProfileOutlined />} tip="查看候选明细" onClick={() => void loadCandidates(selectedPlanId, task.id)} testId={`floor-plan-import-select-${task.id}`} /></Can></td>
                   </tr>
                 ))}
               </tbody>
@@ -257,7 +306,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
                     onClick={() => openCandidateDetail(candidate)}
                     onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openCandidateDetail(candidate); } }}>
                     <td>{candidate.candidateNo ?? "-"}</td><td>{fpLabel(SHAPE_TYPE_LABELS, candidate.shapeType)}</td>
-                    <td>{fpLabel(REF_TYPE_LABELS, candidate.refType)}:{candidate.refId ?? "-"}</td><td>{candidate.label ?? "-"}</td><td>{formatBusinessStatus(candidate.status)}</td>
+                    <td>{fpLabel(REF_TYPE_LABELS, candidate.refType)}:{candidate.refId ?? "-"}</td><td>{candidate.label ?? "-"}</td><td>{fpLabel(CANDIDATE_STATUS_LABELS, candidate.status)}</td>
                   <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看候选详情" onClick={(event) => { event.stopPropagation(); openCandidateDetail(candidate); }}>查看详情</button></td></tr>
                 ))}
               </tbody>
@@ -292,10 +341,11 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
               </fieldset>
               <HighRiskConfirm
                 label="发布候选到正式平面图"
-                impact={`${PUBLISH_CONFIRM_TITLE}：发布后候选会写入正式平面图区域，请确认已完成业务复核，发布不可静默覆盖既有区域证据。`}
+                impact={`${PUBLISH_CONFIRM_TITLE}：本次发布 ${publishCandidateIds.length} 个已勾选候选，发布后候选会写入正式平面图区域，请确认已完成业务复核，发布不可静默覆盖既有区域证据。`}
+                precheck={requireSelectedCandidates}
                 onConfirm={() => void publishImport()}
                 permissionsState={effectivePermissions}
-                permissions={MANAGE}
+                permissions={IMPORT_MANAGE}
                 data-testid="floor-plan-publish"
               />
             </div>
@@ -316,12 +366,8 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
         steps={[
           {
             title: "基本信息",
-            description: "平面图名称必填；楼栋/楼层选填。新建平面图初始为草稿，导入候选复核后再发布。",
-            validate: () => {
-              if (!planForm.projectId.trim()) return "请选择所属项目";
-              if (!planForm.planName.trim()) return "请填写平面图名称";
-              return null;
-            },
+            description: "平面图名称、楼层、底图地址、底图宽高、版本号必填；楼栋选填。新建平面图为启用状态，导入候选复核后再发布。",
+            validate: planFormError,
             content: (
               <>
                 <label>所属项目<select data-testid="floor-plan-project" value={planForm.projectId} onChange={(e) => setPlanForm({ ...planForm, projectId: e.target.value })}>
@@ -331,6 +377,10 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
                 <label>平面图名称<input data-testid="floor-plan-name" value={planForm.planName} onChange={(e) => setPlanForm({ ...planForm, planName: e.target.value })} /></label>
                 <label>楼栋<input data-testid="floor-plan-building" value={planForm.buildingCode} onChange={(e) => setPlanForm({ ...planForm, buildingCode: e.target.value })} /></label>
                 <label>楼层<input data-testid="floor-plan-floor" value={planForm.floorNo} onChange={(e) => setPlanForm({ ...planForm, floorNo: e.target.value })} /></label>
+                <label>底图地址<input data-testid="floor-plan-image-url" value={planForm.imageUrl} onChange={(e) => setPlanForm({ ...planForm, imageUrl: e.target.value })} /></label>
+                <label>底图宽<input data-testid="floor-plan-image-width" type="number" min="1" value={planForm.imageWidth} onChange={(e) => setPlanForm({ ...planForm, imageWidth: e.target.value })} /></label>
+                <label>底图高<input data-testid="floor-plan-image-height" type="number" min="1" value={planForm.imageHeight} onChange={(e) => setPlanForm({ ...planForm, imageHeight: e.target.value })} /></label>
+                <label>版本号<input data-testid="floor-plan-version" value={planForm.versionNo} onChange={(e) => setPlanForm({ ...planForm, versionNo: e.target.value })} /></label>
               </>
             ),
           },
@@ -339,8 +389,11 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
           { label: "所属项目", value: projects.find((p) => p.id === planForm.projectId)?.name ?? (planForm.projectId || "未选择") },
           { label: "平面图名称", value: planForm.planName.trim() || "未填写" },
           { label: "楼栋", value: planForm.buildingCode.trim() || "—" },
-          { label: "楼层", value: planForm.floorNo.trim() || "—" },
-          { label: "初始状态", value: "草稿" },
+          { label: "楼层", value: planForm.floorNo.trim() || "未填写" },
+          { label: "底图地址", value: planForm.imageUrl.trim() || "未填写" },
+          { label: "底图宽×高", value: planForm.imageWidth && planForm.imageHeight ? `${planForm.imageWidth} × ${planForm.imageHeight}` : "未填写" },
+          { label: "版本号", value: planForm.versionNo.trim() || "未填写" },
+          { label: "状态", value: "启用" },
         ]}
       />
       <GuidedFlowModal
@@ -357,7 +410,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
           {
             title: "导入内容",
             description: "选择来源格式，填写名称和内容。",
-            validate: () => (importForm.sourcePayload.trim() ? null : "请填写来源内容"),
+            validate: importFormError,
             content: (
               <>
                 <label>来源格式<select data-testid="floor-plan-import-format" value={importForm.sourceFormat} onChange={(e) => setImportForm({ ...importForm, sourceFormat: e.target.value })}>
diff --git a/apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx b/apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx
index 0fec42e1..01ccdd77 100644
--- a/apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx
+++ b/apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx
@@ -5,11 +5,18 @@ import { SpaceTypesPage } from "./SpaceTypesPage";
 import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";
 
 // 引导式流程 phase-7：新增空间类型改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截）。
+// M018-006/009：存档/申请删除/审批/恢复四条生命周期动作各有可失败断言。
 const ROWS = [{ id: "st-1", typeCode: "kiosk", typeName: "一体机位", sortOrder: 100, status: "active" }];
+const LIFECYCLE_ROWS = [
+  { id: "st-1", typeCode: "kiosk", typeName: "一体机位", sortOrder: 100, status: "active" },
+  { id: "st-2", typeCode: "old", typeName: "旧种类", sortOrder: 90, status: "disabled" },
+  { id: "st-3", typeCode: "arc", typeName: "已存档种类", sortOrder: 80, status: "archived" },
+  { id: "st-4", typeCode: "pd", typeName: "待删种类", sortOrder: 70, status: "pending_delete" },
+];
 
-function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
+function makeApi(post = vi.fn().mockResolvedValue({}), rows: unknown[] = ROWS): AdminApiClient {
   return {
-    get: vi.fn().mockResolvedValue(ROWS),
+    get: vi.fn().mockResolvedValue(rows),
     getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
     post,
     put: vi.fn().mockResolvedValue({}),
@@ -19,12 +26,14 @@ function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
 }
 
 const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
+const ADMIN_USER = { id: "u1", username: "admin", accountType: "platform_admin" };
+const MASTER_USER = { id: "usr_master_authority", username: "master-authority", accountType: "master_authority" };
 
-function renderPage(api: AdminApiClient) {
+function renderPage(api: AdminApiClient, user = ADMIN_USER) {
   const setActionResult = vi.fn();
   const props: AdminPageProps = {
     api,
-    user: { id: "u1", username: "admin", accountType: "platform_admin" },
+    user,
     setActionResult,
     setGlobalError: vi.fn(),
     effectivePermissions: ADMIN_PERMS,
@@ -89,18 +98,69 @@ describe("SpaceTypesPage 引导式新增空间类型(phase-7)", () => {
     expect(post).not.toHaveBeenCalled();
   });
 
-  it("已删除种类不再提供申请删除；已存档可取消存档", async () => {
-    const api = makeApi();
-    api.get = vi.fn().mockResolvedValue([
-      { id: "st-1", typeCode: "kiosk", typeName: "一体机位", sortOrder: 100, status: "active" },
-      { id: "st-2", typeCode: "old", typeName: "旧种类", sortOrder: 90, status: "disabled" },
-      { id: "st-3", typeCode: "arc", typeName: "已存档种类", sortOrder: 80, status: "archived" },
-    ]);
-    renderPage(api);
+  it("已删除种类不再提供申请删除但可恢复；已存档可取消存档", async () => {
+    renderPage(makeApi(undefined, LIFECYCLE_ROWS));
     await waitFor(() => expect(screen.getByTestId("space-type-archive-kiosk")).toBeTruthy());
     expect(screen.getByTestId("space-types-table").textContent).toContain("已删除");
     expect(screen.queryByTestId("space-type-delete-request-old")).toBeNull();
+    expect(screen.getByTestId("space-type-restore-old")).toBeTruthy(); // M018-009
     expect(screen.getByTestId("space-type-unarchive-arc")).toBeTruthy();
     expect(screen.getByTestId("space-type-delete-request-arc")).toBeTruthy();
   });
 });
+
+// M018-006：存档→申请删除→最高权限审批→恢复（业务地图 L566-577）各有可失败断言；理由经 window.prompt、高危经 window.confirm。
+describe("SpaceTypesPage 生命周期动作", () => {
+  it("存档：填理由并确认后 POST /archive 带 reason；理由留空不请求", async () => {
+    const post = vi.fn().mockResolvedValue({});
+    const { setActionResult } = renderPage(makeApi(post, LIFECYCLE_ROWS));
+    await waitFor(() => expect(screen.getByTestId("space-type-archive-kiosk")).toBeTruthy());
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+    const promptSpy = vi.spyOn(window, "prompt").mockReturnValueOnce("   ");
+    fireEvent.click(screen.getByTestId("space-type-archive-kiosk"));
+    expect(post).not.toHaveBeenCalled();
+
+    promptSpy.mockReturnValueOnce("配错了");
+    fireEvent.click(screen.getByTestId("space-type-archive-kiosk"));
+    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types/st-1/archive", { reason: "配错了" }));
+    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "space_type_archived", typeCode: "kiosk" }));
+  });
+
+  it("申请删除：已存档种类填理由后 POST /delete-request 带 reason", async () => {
+    const post = vi.fn().mockResolvedValue({});
+    renderPage(makeApi(post, LIFECYCLE_ROWS));
+    await waitFor(() => expect(screen.getByTestId("space-type-delete-request-arc")).toBeTruthy());
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+    vi.spyOn(window, "prompt").mockReturnValueOnce("不再使用");
+    fireEvent.click(screen.getByTestId("space-type-delete-request-arc"));
+    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types/st-3/delete-request", { reason: "不再使用" }));
+  });
+
+  it("审批：最高权限可通过/驳回（POST /delete-approve 带 decision），平台管理员只见「待最高权限审批」", async () => {
+    const post = vi.fn().mockResolvedValue({});
+    renderPage(makeApi(post, LIFECYCLE_ROWS));
+    await waitFor(() => expect(screen.getByTestId("space-types-table")).toBeTruthy());
+    expect(screen.queryByTestId("space-type-delete-approve-pd")).toBeNull();
+    expect(screen.getByTestId("space-types-table").textContent).toContain("待最高权限审批");
+    vi.unstubAllGlobals();
+  });
+
+  it("审批（最高权限）：通过 POST decision=approved", async () => {
+    const post = vi.fn().mockResolvedValue({});
+    renderPage(makeApi(post, LIFECYCLE_ROWS), MASTER_USER);
+    await waitFor(() => expect(screen.getByTestId("space-type-delete-approve-pd")).toBeTruthy());
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+    fireEvent.click(screen.getByTestId("space-type-delete-approve-pd"));
+    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types/st-4/delete-approve", { decision: "approved" }));
+  });
+
+  it("恢复：已删除种类确认后 POST /unarchive（M018-009）", async () => {
+    const post = vi.fn().mockResolvedValue({});
+    const { setActionResult } = renderPage(makeApi(post, LIFECYCLE_ROWS));
+    await waitFor(() => expect(screen.getByTestId("space-type-restore-old")).toBeTruthy());
+    vi.spyOn(window, "confirm").mockReturnValue(true);
+    fireEvent.click(screen.getByTestId("space-type-restore-old"));
+    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types/st-2/unarchive"));
+    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "space_type_unarchived", typeCode: "old" }));
+  });
+});
diff --git a/apps/web/src/admin/pages/SpaceTypesPage.tsx b/apps/web/src/admin/pages/SpaceTypesPage.tsx
index a0f9bc71..856a0545 100644
--- a/apps/web/src/admin/pages/SpaceTypesPage.tsx
+++ b/apps/web/src/admin/pages/SpaceTypesPage.tsx
@@ -154,7 +154,7 @@ export function SpaceTypesPage({ api, user, setActionResult, setGlobalError, eff
     <section data-testid="space-types-page">
       <AdminPageHeader
         title="空间种类"
-        description="维护空间种类。建空间时从本表选择种类。可新增。下线用存档。删除须先存档，再申请并经最高权限审批。有空间还在用的种类不能删除。"
+        description="维护空间种类。建空间时从本表选择种类。可新增。下线用存档。删除须先存档，再申请并经最高权限审批。有空间还在用的种类不能删除。已删除的种类可恢复为启用。"
       />
       <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
         <div className="admin-table-scroll">
@@ -199,6 +199,17 @@ export function SpaceTypesPage({ api, user, setActionResult, setGlobalError, eff
                         />
                       </>
                     ) : null}
+                    {row.status === "disabled" ? (
+                      // M018-009：删除只隐藏不抹除、可恢复（业务地图 L571）——走既有 unarchive（后端允许 disabled → active）。
+                      <HighRiskConfirm
+                        label="恢复"
+                        impact={`把已删除种类「${row.typeName}」恢复为启用，可以再用来新建空间。`}
+                        onConfirm={() => void unarchiveType(row)}
+                        permissionsState={effectivePermissions}
+                        permissions={MANAGE}
+                        data-testid={`space-type-restore-${row.typeCode}`}
+                      />
+                    ) : null}
                     {row.status === "pending_delete" && isMaster ? (
                       <>
                         <HighRiskConfirm
diff --git a/apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx b/apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx
index a8545e53..b0ae055e 100644
--- a/apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx
+++ b/apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx
@@ -10,9 +10,11 @@ const HOURS = [
 ];
 const SPACES = [{ id: "sp_bar_main", spaceName: "主吧台大厅" }];
 
-function makeApi(post = vi.fn().mockResolvedValue({})): { api: AdminApiClient; get: ReturnType<typeof vi.fn>; post: typeof post } {
-  const get = vi.fn().mockImplementation((url: string) =>
-    Promise.resolve(url.includes("venue-business-hours") ? HOURS : SPACES));
+function makeApi(post = vi.fn().mockResolvedValue({}), spacesFail = false): { api: AdminApiClient; get: ReturnType<typeof vi.fn>; post: typeof post } {
+  const get = vi.fn().mockImplementation((url: string) => {
+    if (url.includes("venue-business-hours")) return Promise.resolve(HOURS);
+    return spacesFail ? Promise.reject(new Error("场地清单读取失败")) : Promise.resolve(SPACES);
+  });
   const api: AdminApiClient = {
     get,
     getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
@@ -85,4 +87,33 @@ describe("VenueBusinessHoursPage 场地营业时间(D1)", () => {
         disabled: false,
       }));
   });
+
+  // M018-006：「本日不营业」= 停用，配置保留（M017-003 口径），提交 disabled:true 且时刻不丢
+  it("取消勾选本日营业后保存，提交 disabled:true 且保留时刻，说明列显示停用", async () => {
+    const { api, post } = makeApi();
+    renderPage(api);
+    await userEvent.selectOptions(await screen.findByTestId("venue-business-hours-venue-select"), "sp_bar_main");
+    await userEvent.click(await screen.findByTestId("venue-business-hours-open-flag-5"));
+    expect(screen.getByTestId("venue-business-hours-note-5").textContent).toContain("本日不营业");
+    await userEvent.click(screen.getByTestId("venue-business-hours-save-5"));
+
+    await waitFor(() =>
+      expect(post).toHaveBeenCalledWith("/api/v1/platform/venue-business-hours", {
+        venueId: "sp_bar_main",
+        dayOfWeek: 5,
+        openTime: "20:00",
+        closeTime: "02:00",
+        disabled: true,
+      }));
+  });
+
+  // M018-006：场地清单是副资源，读取失败只降级提示，已配营业时间的场地仍可选、可改
+  it("场地清单读取失败时显示降级提示，已配场地仍可从营业时间行中选到", async () => {
+    const { api } = makeApi(undefined, true);
+    renderPage(api);
+    const notice = await screen.findByTestId("venue-business-hours-spaces-degraded");
+    expect(notice.textContent).toContain("加载失败");
+    await userEvent.selectOptions(screen.getByTestId("venue-business-hours-venue-select"), "sp_bar_main");
+    await waitFor(() => expect(screen.getByTestId("venue-business-hours-open-5")).toHaveValue("20:00"));
+  });
 });
````
