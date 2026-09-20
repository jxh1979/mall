CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M018 B5-r1 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.01（指定路径原先不存在，本次新建完整报告）
- 席位：固定 Grok 外审席乙（grok-4.6 / xhigh；不派子代理、不用 MCP、不问操作员、不读本轮 `M018-b5-r1-self-raw.md`）
- 步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B4 双 PASS、B3 自报红→绿或 B1/B2 历史票替代本轮结论）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M018 同一 7 文件；`runbook.md` V0.02；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 模块：M018 — 空间点位、楼层平面图与营业时段／网页与验证／第1段
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值（889020 字节 / 6850 行）；sidecar 全文一行同值。独占锁行仅本文第一行，正文不再出现同名独占字段。
- 映射：`M018 → [BSM-001, BSM-003, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001]`，COUNT=21 unique=21 MISSING=[] EXTRA=[]（BSM 2 / GEN 18 / PGM 1）；与锁映射行 L6397 逐字相同；定义与 HTML 出处均从锁/三 HTML 自读，不新造判据、不 LOCK-AMEND
- 正本：`/home/james/complex-management` main 主工作区。`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`。B3-r1 after tree 对象 `e2d60c3c41a49a27547081ea6c411ba88155bc4e` 可解析为 `tree`；本模块 7 文件当前工作区 blob 与该 after **MISMATCH_COUNT=0**（`git diff --stat e2d60c3c… -- <7 paths>` 空）。HEAD blob 仍为修复前（B3 未提交；第 7 文件 `VenueBusinessHoursPage.tsx` HEAD=after 同 blob）
- 本席约束：只读业务代码与计划；除本 raw 外不写文件；不修源/三 HTML/锁/findings/ledger；不 fetch/commit/push/merge/deploy；不装依赖；不建脚本/日志/临时产物；主仓 cwd 只跑派发允许的 vitest 四文件 + `tsc -b --noEmit`
- 未读取：`M018-b5-r1-self-raw.md`（落盘时该路径已存在，本席按独立性禁令整文件不打开）。旧 B1/B2/B4 可读，只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界，不代替本轮全量重审）：findings V2.55 — M018-001…009 修复队列「否（已修，B3-r1）」；M015-010/011 XM 前端部分待后端，不重复立项。B3 累计 1。模块生命周期仍 `IN_PROGRESS`。本票不等于双席 B5 或模块 PASS。席甲票是作者自审票；本席 FAIL 具否决权。

本次审计结论：**FAIL**。001–009 在当前 7 文件上独立复验已落地、无回归；M015-010/011 维持 XM 不升格。另有 **1 条无 ID 新工程缺陷**（GR-M018-B5-01：仅 `floor_plan.view` 的分岗账号第一次打开本页，因副资源 `GET /platform/projects` 403 被 `Promise.all` 整页拖死，与本页自测「能看见平面图表」相反）。无添加项、无待问 2.2、无锁外判据、无 HTML 修改申请。按回退统一规则：本批含无 ID 项 → 须整批回 B2，禁止直接回 B3。

---

## 0. 技能自核（V0.17 §6，按 B5 收窄）

本席实际读取并按收窄口径使用（本机路径实核，不抄 Codex 预检表）：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户 160 提示词优先于技能本体。本步禁止子代理，与技能「先派子代理」冲突处按用户指令收窄 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；路由到本仓库 160 B5，**不执行** Arc `audit`（本体要派评审子代理并写 `docs/arc/audits/`，与双席/§7 冲突，记缺口） |
| graft | `~/.claude/skills/graft/SKILL.md` | `graft grep validateCreateRequest` / `graft ask` 平面图导入权限与闭集 / `graft callers FloorPlanController`：定位 Controller 映射与服务闭集 |
| grilling / grill-me | mattpocock `productivity/grilling`、`grill-me`（本体转发 grilling） | 只用追问树自问自答；本体「等用户」「dispatch sub-agent」作废 |
| verification-before-completion | superpowers 同名 | 先跑 vitest/tsc 再声称；误 cwd 的失败不折算 |
| systematic-debugging | superpowers 同名 | 只借读错误→追数据流（页→adminClient→Controller 映射→Service/Parser→V023/V034/V423/V425）；B5 不修复 |
| diagnosing-bugs | mattpocock engineering | 用契约对照构造可红信号；本机不跑 Java |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删重复，不落地、不单独立项 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读；vitest/tsc 在本席命令内跑完（秒级），未另挂 monitor |
| codebase-design | mattpocock engineering | 页为浅适配器、后端为深度模块；查 seam 不匹配 |
| writing-for-agents | mattpocock productivity | 本 raw 按完成条件 + `文件:行` 组织，供合取/回 B2 |
| git-guardrails-claude-code | 可选存在 | 未装 hook；等价：不 fetch/commit/push/merge/hard reset |
| receiving-code-review | superpowers | B5 外审否决席，不 IMPLEMENT |
| requesting-code-review | superpowers | 节点为 B4，本步不派 |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、using-git-worktrees、executing-plans、dispatching-parallel-agents、subagent-driven-development、domain-modeling、research、ponytail-audit。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能
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
| 锁 blob | `8db3fc02ce8a6dc998863a9784e7e88af511b960` |
| sidecar | 全文一行同值 |
| findings | V2.55；SHA256 `092f9ca3e94bde2c9ce27729f9ca25f0d9b6135b36ffb146a454b3079c3be568` |
| ledger | SHA256 `0f21f7d89fdf9fbc426a945ebd9d202798ee8677772188543740d1b78c324ac3`；M018 `IN_PROGRESS`，B3=1，B5-r1 进行中 |
| 映射行 | L6397，21 条、无重复、MISSING=[] EXTRA=[] |
| 计划 FILES | 7 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 1080（切分时）；当前工作区 `\n` 计数 **1665**（B3-r1 后现态，不改计划凑数） |
| `git ls-files -z -- <7> \| xargs -0 cat \| wc -l` | **1665** |
| questions | 无 `questions/M018*`；无未答问询 |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| modules.md / runbook.md | SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` / `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| after tree | `e2d60c3c41a49a27547081ea6c411ba88155bc4e`（`git cat-file -t` = tree） |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466` |

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 行 | SHA256 |
|---|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

七文件指纹（工作区 `sha256sum` + `git hash-object` = after blob）：

| 行 | SHA256 | blob | 路径 |
|---:|---|---|---|
| 432 | `47e1a50d97ca7b59daf107046020d635cfc3934eeff23326bc0dd6406e378cb6` | `4e6a47446676673c5b3f09044ef7ad332148b9e6` | `apps/web/src/admin/pages/FloorPlanImportPage.tsx` |
| 148 | `5790a81e551db768b9ee12b23418bd949cddc5e83bd326ce40cd8f9d33a0fa97` | `4132987d33f51ebf973d6920bfae87c6086e5063` | `apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx` |
| 272 | `a08a7ec69263e2ef3b08d367ec543bc28105553cb69f1dce54a7167d20da705d` | `92776d912f3841cb9cd51b88236524b34cf5311a` | `apps/web/src/admin/pages/FloorPlanImportPage.test.tsx` |
| 280 | `4d450b0df0de1acdba22e9dec777f8fa11159defd395a3601b19171efd88deed` | `856a0545618948ce7142cfa144d79e88398fa790` | `apps/web/src/admin/pages/SpaceTypesPage.tsx` |
| 166 | `db80bdf26bdd9ce931e72591d666faf168ca06ad2aa993f91d792feefdfa9e4b` | `01ccdd77db8126f923477fb03973df9e0bf7027b` | `apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx` |
| 248 | `841c1e543bd573d6fbfa7e2773f7aa391e58cee430695199e5bc105be32973a9` | `45237715ed4e48d96d0c7d8ecaf8c62806a5395e` | `apps/web/src/admin/pages/VenueBusinessHoursPage.tsx` |
| 119 | `60f3642dd6161bbabffd75483af9ba9a05d84176ebf7e7461e8c8855c6e6118c` | `b0ae055e6c3a821e0d187bf63857f9b473da1846` | `apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx` |
| **1665** | （cat 合计） | | |

只读追依赖（不纳入本模块队列；缺陷记跨模块观察，除非本页声称在本页不可达）：`FloorPlanController`/`FloorPlanService`/`FloorPlanImportParser` 及 DTO；`PlatformController` 空间种类与营业时间端点及 `PlatformService`/`VenueBusinessHoursService`；`adminClient` unwrap/snake/`idempotencyKey`；`Can`/`PermissionButton`/`HighRiskConfirm`/`GuidedFlowModal`；`adminRoutes`；V023/V034/V423/V425（及只读见到的 V424/V426）。HTTP 入口以 Controller `@RequestMapping`/`@GetMapping`/`@PostMapping` 为准，不以服务注释代替。

---

## 2. 转入既有口径（独立对照现状，不继承 PASS）

| 既有 ID | 转入摘要 | 本席对 7 文件现状 |
|---|---|---|
| M018-001…009 | B3-r1 已修，队列否 | **独立复验成立（见 §6）**，无回归 |
| M015-010 | 营业时段草案/生效日/停用另列，XM，前端部分待后端 | **仍成立，不重复立项。** 页仍保存即标「已生效」，body 传 `disabled`、不传 `effectiveFrom`。与现行 `VenueBusinessHoursService`「保存即生效 / is_draft 复用为停用」同批，与 BSM-001 明文仍冲突 |
| M015-011 | 发布确认字段 XM | **仍成立，不重复立项。** 发布 POST 仅 `idempotency_key`+`candidate_ids`；`HighRiskConfirm` 为 `window.confirm`。HTML「须二次确认」字面 UI 已有 |
| M015-013 / M016-004 | 前端闭集 / corridor / geojson | **本工作区前端已与现行八值 + svg/standard_json 对齐**；未知 `corridor` 显示「未识别」。观察，不立项 |

不把「001–009 全部否」当成功能全实现。

---

## 3. 第一性原理线（逐文件三问）

依据只能是：HTML 原文 / 可执行产物 / 外部规范原文 / 可复现命令输出。全文覆盖，非抽样。

### 3.1 `FloorPlanImportPage.tsx`（L1–432）

1. **为何必须存在？** 治理地图「空间和点位」主要功能原文：「商场平面图网页导入：在后台上传或导入商场平面布局图，系统解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误后发布到正式平面图；发布为高危动作，须二次确认——发布会写入正式平面图区域，不可静默覆盖既有布局证据。」（`docs/platform-governance-map-for-operations-v0.1.html:472`）。删掉本页，该句后台网页做不到。
2. **声称是否实际执行？** 页头声称「上传商场平面图，解析出候选区域，人工复核候选与错误后发布」。引导新建现送 `status:"active"` + 楼层/底图/宽高/版本号（L150–171），与 `FloorPlanService.validateCreateRequest` L504–528、`V023` CHECK `{active,inactive,archived}` 及五列 NOT NULL 对齐。创建导入校验来源名称（L178–194），与 `validateImportCreateRequest` `requireText(sourceName)` / V034 `source_name NOT NULL` 对齐。发布 `ids = publishCandidateIds` 且 `precheck` 未勾选拦截（L202–214、L342–349），不再默认全发。创建导入/发布按钮 `IMPORT_MANAGE`、下钻 `IMPORT_VIEW`（L73–75、L269、L278、L287、L347），与 `FloorPlanController` L60–93 对齐。页内字典覆盖平面图/任务/候选/错误字段与 10 个解析器错误码。幂等键 `idempotencyKey`（camel），经 `normalizeRequestBody` 后仍为页面 `fp-`/`fpi-`/`fpp-` 前缀（vitest 实跑）。**缺口：** 主资源加载把 `GET /floor-plans`（`floor_plan.view`）与 `GET /platform/projects`（`governance.project.view` + `requirePlatformScope`）绑在同一 `Promise.all`（L113–121）。仅有 `floor_plan.view` 的分岗账号（路由 `adminRoutes.ts:95` 允许进页）第一次打开即整页失败。见 GR-M018-B5-01。
3. **未验证前提：** 本机未跑 Java/HTTP；后端行为以 Controller 映射 + 服务源码 + 既有测试夹具原文为准。测试夹具把 `GET /platform/projects` 一律 200，不能失败于真实 403。

### 3.2 `FloorPlanImportPage.guidedcreate.test.tsx`（L1–148）

1. **为何必须存在？** BSM-003 相关段「后台的新建/录入改成分步引导」的测试承载；钉死 001/005 契约。
2. **声称是否执行？** 本席在 `apps/web` 下 vitest 该文件 4 例全绿。断言完整 body（含 `status:"active"`、底图/宽高/版本）与 `idempotency_key` 匹配 `/^fp-/`；缺楼层/底图/名称拦截。不再钉 `draft`。
3. **未验证前提：** 走真实 `AdminApi`+fetch 桩，不覆盖 projects 403、不覆盖导入/发布（在另一测试文件）。

### 3.3 `FloorPlanImportPage.test.tsx`（L1–272）

1. **为何必须存在？** PGM-001 零英文、002 闭集、003/004/005/007/008 的回归网。
2. **声称是否执行？** 13 例全绿。`parsed`→「已解析」、`archived`→「已存档」、错误 `points`/`POINTS_TOO_FEW` 中文；未勾选发布不弹 confirm、不请求；勾选后 `candidate_ids:["cd-1"]` 且 `fpp-`；无 `import.manage` 时导入/发布禁用、新增平面图仍可用；无 `import.view` 时下钻按钮不存在；imports 403 调 `setGlobalError`。
3. **未验证前提：** `VIEW_ONLY` 桩仍 200 返回 projects，故「能看见平面图表」在 mock 下成立、在真实后端分岗下不成立（GR-M018-B5-01）。无 `inactive`/`failed`/`cancelled` 夹具（页内已有映射，记观察）。反向「有 import.manage 无 floor_plan.manage」未测（代码分码，记观察）。

### 3.4 `SpaceTypesPage.tsx`（L1–280）

1. **为何必须存在？** PGM-001「先管归属：哪个综合体、哪个楼层、哪个区域、哪个点位」；空间建档要种类字典。BSM-003 相关段：配置可存档/删除审批、分步引导、平台管理员与最高权限分开。
2. **声称是否执行？** 引导新建 `status:"active"`（后端 `createSpaceType` 亦强制 active，忽略请求 status）。存档填理由、先存档再申请删除、占用由后端 `countSpacesByTypeCode` 拦。审批按钮仅 `user.id==="usr_master_authority" && user.username==="master-authority"`（与 `SessionIssuer.MASTER_USER_ID/MASTER_USERNAME` 逐字相同）；后端 `approveDeleteSpaceType` L642–644 再查 master。`disabled` 行「恢复」走既有 `POST …/unarchive`（L202–211），后端 `unarchiveSpaceType` L593 允许 `archived|disabled` → `active`。V425 CHECK `{active,disabled,archived,pending_delete}` 与页内四词对齐。
3. **未验证前提：** 前端 master 判断只藏按钮；信任边界在服务端。`formatBusinessStatus` 仅作未知码兜底；现行四值均页内映射。

### 3.5 `SpaceTypesPage.guidedcreate.test.tsx`（L1–166）

1. **为何必须存在？** 引导新建与存档/申请删除/审批/恢复的测试网（006/009）。
2. **声称是否执行？** 10 例全绿。存档空理由不请求；填理由 POST `/archive` `{reason}`；申请删除 POST `/delete-request`；平台管理员不见审批按钮、见「待最高权限审批」；master POST `/delete-approve` `{decision:"approved"}`；恢复 POST `/unarchive`。
3. **未验证前提：** mock `AdminApiClient` 不经 `unwrap`/`normalizeRequestBody`。驳回 `decision:"rejected"` 无点击例（代码对称，记观察）。

### 3.6 `VenueBusinessHoursPage.tsx`（L1–248）

1. **为何必须存在？** 业务地图客人端「查看综合体介绍、营业时间、活动公告、票务入口」（`business-system-map-for-operations-v0.1.html:856`）；后台必须有配置面。BSM-001 策略参数含「时刻」。
2. **声称是否执行？** 按场地×星期几保存；跨零点文案；同时刻拦截；停用勾掉「本日营业」提交 `disabled:true` 且保留时刻（与现行服务 `Boolean.TRUE.equals(request.disabled())`、库 `is_draft` 复用停用一致）。场地清单 `loadSecondary`：失败只降级提示，已配场地仍可选。权限 `governance.space.manage` 对保存，与 `PlatformController` L239–245 对齐；读 `governance.space.view`（L232–236）与路由 L51 对齐。**仍：** 已保存非跨零点显示「已生效」（L223–224），不采集 `effectiveFrom`、无草案——M015-010 XM，不新立项。
3. **未验证前提：** `effectiveFrom` 在行类型 L18 存在但保存不传。页不持 KeyRef；`adminClient` 自动补 `idempotencyKey`；服务 `upsert` 按场地+星期几，重试不造第二行。

### 3.7 `VenueBusinessHoursPage.test.tsx`（L1–119）

1. **为何必须存在？** 跨零点、同时刻、保存 body、停用、场地清单降级。
2. **声称是否执行？** 5 例全绿。停用断言 `disabled:true` 且时刻保留；降级提示含「加载失败」（`loadSecondary` 非 403 文案）。
3. **未验证前提：** mock client 不走 `normalizeRequestBody`；无权限矩阵例。生产 Jackson `SNAKE_CASE` 接受 camel→snake 后的 `venue_id` 等（`application.yml` `property-naming-strategy: SNAKE_CASE`）。

---

## 4. 顺从式线（21 项逐条适用证据）

每条均读锁内定义全文。BSM-003 锁内原文仅 h2+lead；grilling 要求的存档/删除/引导/分岗读其 HTML 子段（BSM-004/005/007 锚点），**不把未映射 ID 当本模块判据**。PGM-001 另读治理地图导入句 L472。BSM-001 另读客人端营业时间句 L856。不可用锁外 ID 支撑 finding。

| ID | 适用 | 证据（本模块） | 结论 |
|---|---|---|---|
| BSM-001 | 适用 | 锁内「时刻」属策略参数 +「先存为草案…不即改即生效」。营业时间页保存即「已生效」、不传 `effectiveFrom`。与现行后端注释一致 | **不满足**（冲突；已并入 M015-010 XM，本轮不重复立项） |
| BSM-003 | 适用 | 锁内「整套系统都遵守的通用规则」。相关子段：空间种类有存档/申请删除/最高权限审批/占用拦截/disabled 恢复；新建平面图与空间种类走 `GuidedFlowModal`；审批按钮仅 master。导入页分岗按钮码已对齐 Controller；**仅 view 账号整页被 projects 403 拖死**（GR-M018-B5-01） | 部分满足（分岗主路径按钮已修；view 进页失败） |
| GEN-001 | 适用 | 三页各有 HTML 上位。`zhOrDash`/`fpLabel` 重复为技术重复，不单独立项 | 满足（重复记观察） |
| GEN-002 | 适用 | 平台管理员第一次新建/导入/发布：请求体与后端闭集对齐（001/007/008 已修，vitest 钉死）。仅 `floor_plan.view` 第一次打开本页不可达（GR-M018-B5-01） | **不满足**（分岗进页） |
| GEN-003 | 适用 | `apps/web` 下 `npx vitest run` 4 文件 **32 passed**；断言能失败于 draft/未勾选/空名称/403。不能失败于 projects 403。测试通过不替代行为证据 | **不满足**（新路径无红网） |
| GEN-004 | 适用 | 前端校验名称/楼层/底图/宽高/版本/payload/同时刻；信任边界在后端 `requireText`/`normalizeAllowed`/`requirePermission` | 满足（主路径） |
| GEN-005 | 适用 | 路由 `floor_plan.view` / `governance.space.view`。写按钮：新建平面图 `floor_plan.manage`；导入/发布 `floor_plan.import.manage`；下钻 `Can`+`floor_plan.import.view`。空间种类写 `governance.space.manage`；审批服务内 master。**进页副资源权限严于路由**（GR-M018-B5-01） | **不满足**（view 进页） |
| GEN-006 | 适用 | 本页不写库；发布一次最多 200 为后端限。前端须勾选，不再一次丢全部 pending。勾选超过 200 仍可能（需 201 个待发布，记观察） | 满足（主路径）+ 观察 |
| GEN-007 | 适用 | 三处 `idempotencyKey`；`normalizeRequestBody` 只认 camel 键再转 snake。vitest 断言 `fp-`/`fpi-`/`fpp-`。发布成功后 `loadCandidates` 清空 `publishKeyRef`。营业时间靠 upsert+客户端自动补键 | 满足 |
| GEN-008 | 适用 | 页内字典 = `STATUSES`/`IMPORT_TASK_STATUSES`/`IMPORT_CANDIDATE_STATUSES`/`REF_TYPES`/`SHAPE_TYPES`/`IMPORT_SOURCE_FORMATS` + Parser 10 码 6 字段。权限码 = Controller。DTO/Jackson SNAKE_CASE。001–009 已对齐 | 满足（本页闭集） |
| GEN-009 | 适用 | 引导失败留弹窗。下钻 403 `setGlobalError`。未勾选发布拦截。**projects 403 掩盖平面图列表**（GR-M018-B5-01）。同模块营业时间已用 `loadSecondary` | **不满足** |
| GEN-010 | 适用 | 无密钥/注入面；幂等用 `Math.random` 非秘密。不立项 | 满足 |
| GEN-011 | 适用 | `AuditTrailHint` 收进技术详情；写路径后端 `AuditEventService`。本页不造假成功（除测试 mock） | 满足 |
| GEN-012 | 适用 | 导入 payload 上限在后端 100 万字符；发布 200；解析后 2000/1000。前端无提示 | 观察 |
| GEN-013 | 适用 | 模块无 package/CI 文件；`cd apps/web && npx tsc -b --noEmit` exit 0 | N/A（无本模块构建文件）+ 命令证据 |
| GEN-014 | 适用 | 7 文件无 SQL。闭集只读 V023/V034/V423/V425 | N/A（迁移不在本模块） |
| GEN-015 | 适用 | 候选/错误行键盘 Enter/Space；空间种类 `window.prompt` 要理由。正常态 `parsed`/`archived` 页内中文。未知码「未识别」非英文原文 | 满足（本页字典） |
| GEN-016 | 适用 | 模块无文档文件；页内描述「楼层、底图、版本号必填」「新建为启用」「已删除的种类可恢复为启用」与现行契约一致。营业时间「已生效」属 M015-010 | 页内声称主路径满足；营业时间 XM |
| GEN-017 | 适用 | 本 raw 按冲突→细化→技术性→添加；未把业务发明标成技术性；未把 XM 升格 | 本席过程满足 |
| GEN-018 | 适用 | 每条落到 HTML/命令/源码可复现输出；不引用席甲本次 self-raw | 本席过程满足 |
| PGM-001 | 适用 | 底座归属+零英文+见名可点。导入句 L472 已引。零英文：页内闭集中文，vitest 钉 `parsed`/`archived` 不含原码。见名可点：候选/错误有抽屉；空间种类/营业时间行弱，记观察不单独立项以免扩判据。二次确认 UI 已有；服务端确认字段 XM M015-011 | 本页字典满足；XM 不升格 |

相关 HTML 原文定位（只读，未改）：

- 治理地图 L463–472「空间和点位」及平面图网页导入句
- 业务地图 L563–588 系统通用能力（存档/删除审批/分步引导/平台管理员≠最高权限）
- 业务地图 L505 BSM-001 整体说明（时刻+草案）
- 业务地图 L856 客人端营业时间句
- 治理地图 L256 PGM-001 零英文段（锁内全文）

---

## 5. 对抗式线（grilling 结构自问自答）

事实由本席自读，不等操作员、不派子代理。追问树按派发点名项一次问完。

❓ **Q1 — 真实后端契约下，第一次正常新建平面图是否可达？**
➡️ **平台管理员：可达。** 引导校验非空楼层/底图/正宽高/版本号后 POST `status:"active"` + 必填列；`validateCreateRequest` 不再因 `draft`/缺 `image_url` 拒绝。vitest 断言 body 与后端夹具同形（`active` + URL + 1600×1200 + v1）。`requirePermission("floor_plan.manage")` 与按钮 `MANAGE` 一致。

❓ **Q2 — 第一次正常创建导入是否可达？**
➡️ **平台管理员：可达。** 来源名称+内容步内必填；格式下拉仅 `svg`/`standard_json`（= `IMPORT_SOURCE_FORMATS` / V034 CHECK）。POST `source_format/source_name/source_payload` + `idempotencyKey`。空名称被拦，不再打到 `source_name is required`。

❓ **Q3 — 第一次正常发布是否可达？**
➡️ **勾选后可达，未勾选不可达（有意）。** `precheck` 在 `window.confirm` 前返回 false；`publishImport` 再守卫；`ids` 只取勾选。后端空列表拒、一次最多 200。HTML「人工复核后发布」：勾选＝复核。UI 二次确认已有；契约确认字段仍 M015-011 XM。

❓ **Q4 — 四类权限账号的按钮与下钻？**
➡️ 以 Controller 四码为准（`floor_plan.view` / `floor_plan.manage` / `floor_plan.import.view` / `floor_plan.import.manage`）：
1. **仅 view**：路由放行。按钮：新增/导入/发布均无 manage 码应禁用或不可见；下钻无 import.view 应隐藏。测试在 mock 200 下证明下钻隐藏。**真实后端：`GET /platform/projects` 要 `governance.project.view`，与 view 码不同，第一次打开整页失败，表都看不见。** GR-M018-B5-01。
2. **manage 无 import.manage**：新增平面图亮；导入/发布灭；下钻若有 import.view 仍可见。测试已钉。
3. **import.manage 无 manage**：新增灭、导入/发布亮（代码分码）。无反向用例，记观察。
4. **wildcard / 平台管理员**：全亮。BSM-007 日常功能自动同步；第一次主路径可达。

空间种类：平台管理员可见存档/申请删除、不见审批（见「待最高权限审批」）；master 可见通过/驳回；`disabled` 见恢复。后端审批再查 master，前端伪造 id 不能越权成功。

❓ **Q5 — 正常态 parsed 与错误码是否中文？**
➡️ **页内字典：是。** `IMPORT_TASK_STATUS_LABELS.parsed="已解析"`；Parser 10 码与 6 字段均有中文。vitest 钉死 `parsed` 不含原码、`POINTS_TOO_FEW`→「顶点不足」。未知码「未识别」非英文原文。Parser `errorMessage` 个别夹杂 `points`/`polyline`/`svg`（`FloorPlanImportParser.java:45,68,80`）属后端文案，跨模块观察，不把 M015 升格为本模块新 ID。

❓ **Q6 — 幂等键是否仍被 snake 覆盖？**
➡️ **否。** 三处 `idempotencyKey`。`adminClient.ts:90-92` 先认 camel 再 `toSnakeCaseKey`。已 snake 的 `plan_name` 无大写、不会变成 `plan__name`。vitest 经真实 `AdminApi` 断言前缀 `fp-`/`fpi-`/`fpp-`。

❓ **Q7 — 空间种类存档 / 删除审批 / 恢复？**
➡️ **可达且与现行服务一致。** 存档 `POST /archive {reason}`；申请删除仅 `archived`；占用后端拦；master 审批 `decision`；`disabled` 恢复走 `/unarchive`（不新造端点）。V425 四值 CHECK。最高权限本人申请删除服务内直接办结（L619–628），页仍走同一申请按钮。

❓ **Q8 — 营业时间停用 / 降级？**
➡️ **与现行后端一致，与 BSM-001 草案仍冲突（XM）。** 停用 `disabled:true` 保留时刻；场地清单失败 `loadSecondary` 提示「该数据加载失败…」且已配场地可选。不新立项。

❓ **Q9 — 为极端并发改幂等算法 / 为 201 个候选加前端 200 上限？**
➡️ **否。** 第一次正常跑不依赖加密随机、不依赖 201 勾选。不立项。

---

## 6. 001–009 独立复验（不把队列否当通过依据）

| ID | 定稿最小处置 | 本席对 after 7 文件 | 回归？ |
|---|---|---|---|
| 001 | 弹窗补必填；`status:"active"`；测试改断言 | L150–171、L365–397；guidedcreate L109–120、L136–146 | 无 |
| 002 | 页内字典=后端闭集；去掉全局码表 | L32–59；test 夹具 `active/archived/parsed/pending/points/POINTS_TOO_FEW`；走廊→未识别 | 无 |
| 003 | 导入/发布 `import.manage`；下钻 `import.view` | L73–75、L269、L278、L287、L347；test L253–264 | 无 |
| 004 | 两 load try/catch + 403 例 | L126–146；test L266–271 | 无 |
| 005 | 三处 `idempotencyKey`；断言 `fp-` | L167、L192、L213；test 三处前缀 | 无 |
| 006 | 四测试文件补最小用例 | 32 例覆盖 002–009 与停用/降级 | 无（不能红于 B5-01） |
| 007 | 来源名称非空 | L178–182；test L208–217 | 无 |
| 008 | 未勾选 precheck 拦截，不默认全发 | L202–210、L342–349；test L236–251 | 无 |
| 009 | disabled「恢复」走 `/unarchive` | SpaceTypesPage L202–211；guidedcreate L157–165 | 无 |

---

## 7. Findings（本轮新证据；001–009 不重开）

分类顺序均为：冲突 → 细化 → 技术性 → 添加。工程缺陷「成立」= 第一次正常运行可复现（含派发点名的分岗第一次进页）。

### GR-M018-B5-01 — 仅 `floor_plan.view` 第一次打开本页被项目列表 403 整页拖死

- **文件:行：** `apps/web/src/admin/pages/FloorPlanImportPage.tsx:113-121`；对照 `apps/web/src/admin/adminRoutes.ts:95`；`backend/src/main/java/com/complex/platform/governance/PlatformController.java:144-150`；`backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java:31-34`；同模块已有降级缝 `apps/web/src/admin/hooks/useAdminResource.ts:79-85`（营业时间页已用，本页未用）
- **原句：**

```113:121:apps/web/src/admin/pages/FloorPlanImportPage.tsx
  const { loading, error, reload } = useAdminResource(
    async () => {
      const [list, projectList] = await Promise.all([
        api.get<{ items?: FloorPlan[] } | FloorPlan[]>("/api/v1/floor-plans"),
        api.get<{ items?: { id: string; name?: string }[] } | { id: string; name?: string }[]>("/api/v1/platform/projects"),
      ]);
      setFloorPlans(Array.isArray(list) ? list : (list?.items ?? []));
      setProjects(Array.isArray(projectList) ? projectList : (projectList?.items ?? []));
    },
```

路由：`requiredPermissions: ["floor_plan.view"]`。`GET /api/v1/floor-plans` → `requirePermission("floor_plan.view")`。`GET /api/v1/platform/projects` → `requirePermission("governance.project.view")` 且 `requirePlatformScope()`。项目列表只用于新建弹窗下拉（L373–376），平面图表本身不需要。

同模块 `VenueBusinessHoursPage.tsx:54-61` 对场地清单已走 `loadSecondary`，失败只出降级提示、主表仍可改——对比说明本页不是「没有这种缝」。

本页自测 `FloorPlanImportPage.test.tsx:74,260-263` 的 `VIEW_ONLY` 声称：无 `import.view` 时**仍能看见**「一层平面图」表、只是无下钻。fetch 桩对 `/platform/projects` 一律 200，故 32 例不能在真实 403 上失败。

- **正常触发/影响：** 分岗账号只授 `floor_plan.view`（菜单按路由放行）→ 打开「商场平面图导入」。`GET /floor-plans` 200，`GET /platform/projects` 403，`Promise.all` 拒绝，`useAdminResource` 走 `fallbackMessage: "平面图读取失败"`，列表不渲染。第一次正常「进页看平面图」失败。不是并发、不是要两件无关故障同时发生：这是派发点名的四类权限账号之一的第一次点击。
- **四分类：** 非与导入句明文相反（HTML 未写项目下拉权限）→ **细化**（权限分岗 / 页面主路径可读的下位实现）。能指到 BSM-003 lead「整套系统都遵守」+ 子段「其他岗位按岗位分配各自能看能做的范围」（L588）及 PGM-001 导入句「在后台上传或导入」。非纯构建。非添加（不发明业务规则；同模块已有副资源降级范式）。
- **工程缺陷：** **成立**
- **冻结 ID：** GEN-002、GEN-005、GEN-009、GEN-003、BSM-003
- **依据类型：** 4 可复现命令（`rg` 页 `Promise.all`+`/platform/projects`；`rg` Controller `governance.project.view`；`rg` `adminRoutes` `floor_plan.view`）+ 源码对照。本机未跑 Java，不伪称后端测试输出。
- **最小拟处置：** 项目列表改为 `loadSecondary` / `loadSecondaryIfPermitted(hasPermissions(..., ["governance.project.view"]))`：无权限或失败时 `projects=[]`、平面图表仍渲染；新建弹窗无项目时已有「请选择所属项目」拦截。+1 用例：projects 403 → 表仍有平面图、`setGlobalError` 不被该 403 单独拖死（或仅副资源提示）。不改路由、不改后端、不发明权限码。

001–009 不重开。M015-010/011 不升格。

---

## 8. 观察（不进修复队列建议）

| 编号 | 内容 | 分类 |
|---|---|---|
| OBS-01 | 有 `import.manage` 无 `floor_plan.manage` 的反向按钮矩阵未测；代码已分码 | 观察 |
| OBS-02 | 页内已映射 `inactive`/`failed`/`cancelled`，测试夹具未覆盖；未知码「未识别」 | 观察 |
| OBS-03 | 空间种类驳回 `decision:"rejected"` 无点击例；通过例已有 | 观察 |
| OBS-04 | 存档/恢复 `HighRiskConfirm` 默认「不可逆」，存档实可取消 | 观察 |
| OBS-05 | Parser 默认标签 `shape-N`、部分 `errorMessage` 夹英文标识，展示在本页。来自 `FloorPlanImportParser` | 跨模块（M015/M017） |
| OBS-06 | 空间种类/营业时间行无抽屉式见名可点。PGM-001 全局原则，不单独立项 | 观察 |
| OBS-07 | 营业时间未接 `GuidedFlowModal`。BSM-005「正持续改造」，不强制本页已完工 | 观察 |
| OBS-08 | `zhOrDash` 与 `fpLabel` 同实现。ponytail `FloorPlanImportPage.tsx:L55-60` shrink。`net: -4 lines possible.` 不立项 | 技术重复 |
| OBS-09 | 后端 `requireText("image_url")` 等未进 `FIELD_LABELS` 的键会英文回显；本页先校验，主路径打不到 | 跨模块 |
| OBS-10 | 发布勾选超过 200 仍可能打到后端上限；需 201 个 pending 且全勾，非第一次正常 | 观察 |
| OBS-11 | `approveDeleteSpaceType` 控制器无 `requirePermission`，服务内 master 检查 | 跨模块 M009 |
| OBS-12 | 本机未跑 Maven/`complex_test`；后端行为依据源码+既有测试夹具原文 | 证据边界 |
| OBS-13 | 本席首次误在仓库根跑 vitest，命中 `.scratch` 副本且无 jsdom（`document is not defined`）。不作为本模块缺陷。更正 `cd apps/web` 后 32/32 | 取证卫生 |
| OBS-14 | `createSpaceType` 忽略请求 `status` 强制 `active`；页也送 `active` | 正向对齐 |
| OBS-15 | 发布成功后 `loadCandidates` 清空 `publishKeyRef`；失败重试同键 | 正向 |

---

## 9. 可执行证据（本机，只读）

### 9.1 误 cwd（不作为模块缺陷）

仓库根 `npx vitest run <四相对路径>`：无 jsdom、并扫进 `.scratch/…` 副本，45 failed / `document is not defined`。exit 1。按派发「在 apps/web 目录运行」作废此次输出。

### 9.2 vitest（有效）

```bash
cd /home/james/complex-management/apps/web && npx vitest run \
  src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx \
  src/admin/pages/FloorPlanImportPage.test.tsx \
  src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx \
  src/admin/pages/VenueBusinessHoursPage.test.tsx
```

exit **0**。

```
 ✓ src/admin/pages/VenueBusinessHoursPage.test.tsx (5 tests) 325ms
 ✓ src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx (10 tests) 382ms
 ✓ src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx (4 tests) 427ms
 ✓ src/admin/pages/FloorPlanImportPage.test.tsx (13 tests) 649ms

 Test Files  4 passed (4)
      Tests  32 passed (32)
   Duration  2.12s
```

（夹杂 jsdom `getComputedStyle() … pseudo-elements` 提示，非失败。）

### 9.3 tsc

```bash
cd /home/james/complex-management/apps/web && npx tsc -b --noEmit
```

exit **0**（无诊断输出）。仓库根同命令因无 `tsconfig.json` 报 TS5083，不作数。

### 9.4 只读契约核（非 Java 实跑）

- `FloorPlanController`：`GET ""` view；`POST ""` manage；`GET/POST …/imports` import.view / import.manage；`GET …/candidates` import.view；`POST …/publish` import.manage。
- `FloorPlanService` 闭集：STATUSES 3、SHAPE 8、REF 6、FORMAT 2、TASK 4、CANDIDATE 3；发布空列表拒、最多 200。
- `FloorPlanImportParser` 10 个大写错误码与页内 `ERROR_CODE_LABELS` 集合相等；字段 6 个含 `polygon_points`。
- `PlatformController`：space-types GET view / POST manage / archive / unarchive / delete-request manage；delete-approve 无 `requirePermission`（服务内 master）；venue-business-hours GET view / POST manage + `requireIdempotencyKey`。
- `adminClient.normalizeRequestBody`：补 `idempotencyKey` 再 snake；`unwrap` 若 `data.items` 则拍平为数组。
- `HighRiskConfirm.precheck` 在 `window.confirm` 前；无权限不弹窗。
- V023 status CHECK 三值 + 五列 NOT NULL；V034 任务四态 + import.view/manage 种子；V423 shape 八值；V425 空间种类四态。
- Jackson `property-naming-strategy: SNAKE_CASE`。

---

## 10. 覆盖表（全文件，非抽样）

| 文件 | 行 | 覆盖 |
|---|---:|---|
| FloorPlanImportPage.tsx | 1–432 | 全文 |
| FloorPlanImportPage.guidedcreate.test.tsx | 1–148 | 全文 |
| FloorPlanImportPage.test.tsx | 1–272 | 全文 |
| SpaceTypesPage.tsx | 1–280 | 全文 |
| SpaceTypesPage.guidedcreate.test.tsx | 1–166 | 全文 |
| VenueBusinessHoursPage.tsx | 1–248 | 全文 |
| VenueBusinessHoursPage.test.tsx | 1–119 | 全文 |

21 项判据均有 §4 行。三线均执行。声称均有 §3/§5/§9 四类证据之一。

---

## 11. 结论

- 单席步骤结论：**FAIL**
- 原因：存在未定稿工程缺陷 GR-M018-B5-01（无 findings ID，且 001–009 已否故不在修复队列）
- 001–009：独立复验已落地，无回归
- M015-010/011：维持 XM，不重复立项
- 无添加项、无 2.2 待问、无锁外判据、无三 HTML 修改申请
- 按 V0.17 回退统一规则：本批任一条无 ID 或不在修复队列 → **整批回 B2** 分类定稿后再谈 B3；禁止把无 ID 项直接丢给 B3
- 本票 ≠ 双席合取；席甲自审票不得覆盖本席 FAIL
- 本席未改任何业务文件，未 fetch/commit/push/merge/deploy，未用 MCP/子代理/第三席，未读本次 self-raw
