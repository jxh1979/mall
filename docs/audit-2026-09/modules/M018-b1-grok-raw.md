CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M018 B1 席乙（Grok 4.6/xhigh）修复前全模块审计 · V0.01

- 席位：审计席乙（独立，不读 `M018-b1-self-raw.md`、不依赖席甲发现）
- 阶段：V0.17 阶段 B / B1（本模块首次开工；本席不改 ledger/findings/plan）
- 正本：`/home/james/complex-management` main 工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`（`audit(M046): 钉住破窗不能无盘续期、换钥作废旧票`）
- 审计对象：主工作区当前 7 文件 blob（与 HEAD 无 diff）；含 2026-09-11 本地合并 PR #439（`ed216aae`）的本模块改动，但 **不把 PR #439 当作已审 PASS**，不按 origin/main 旧 blob
- 计划：`plan/modules.md` V0.04 M018；计划冻结 1080 行；本席实算 `git ls-files -z -- <7 paths> | xargs -0 cat | wc -l` = **1353**
- 判据锁：`plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` = 上文独占行
- 映射 21 项：BSM-001、BSM-003、GEN-001…GEN-018、PGM-001（未新增判据、未扩别模块）
- 结论：**FAIL**（有工程缺陷；测试 17/17 绿与 `tsc` 0 不能覆盖主路径契约）
- 本席未改任何业务文件，未 fetch/commit/push/merge/deploy，未用 MCP/子代理/第三席

---

## 0. 技能预检与节点应用（本环境实核）

本席按 `160-audit-prompt.md` §6 与 `plan/runbook.md` V0.02 §2 在 **本机路径** 核验，不抄 Codex 预检表。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由到 audit；**不执行** Arc 全量审计工作流 | 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，记缺口 |
| `graft` | `~/.claude/skills/graft/SKILL.md` | skeleton/grep/callers 定位三页与后端契约 | 无 |
| `grilling` | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `grill-me` | 同包 `productivity/grill-me/SKILL.md` | 本体只转发 `/grilling`，按 grilling 收窄口径执行 | 无 |
| `batch-grill-me` | — | — | **不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | 同包 `engineering/codebase-design/SKILL.md` | 以页为浅适配器、后端为深度模块，查 seam 不匹配 | 无 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 用契约对照构造 red-capable 信号；本机不跑 Java | 本机无 Maven/后端测试环，Java 侧用源码+既有测试夹具原文，不伪称已跑 |
| `systematic-debugging` | superpowers 同名 | 只借读错误→追数据流；作废插桩/改代码 | 无 |
| `verification-before-completion` | superpowers | PASS/FAIL 前实跑 vitest/tsc | 无 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删重复；不进修复队列除非构成缺陷 | 无 |
| `ponytail` | 同包 | B3 默认强度，B1 不用 | 节点不适用 |
| `tdd` / `test-driven-development` | 存在 | 仅 B3 | 节点不适用 |
| `requesting-code-review` / `receiving-code-review` | 存在 | B4 | 节点不适用 |
| `writing-for-agents` | mattpocock productivity | raw 按完成条件+文件:行组织 | 无 |
| `resolving-merge-conflicts` | 存在 | 无合并 | 节点不适用 |
| `git-guardrails-claude-code` | mattpocock misc | 可选；本体要问人装 hook、写 settings | **存在但未安装**（只读审计、禁写仓库其它文件） |
| `i-have-adhd` | marketplace 存在 | Codex 回执模式；本席 stdout 由适配器丢弃 | 不套到本 raw 以外 |
| `audit-context-building` | Trail of Bits | 本体强制子代理写 `audit-context/` | **不用**（第三席+§7 外产物） |
| `differential-review` | Trail of Bits | PR/diff 安审；要 adversarial 子代理 | **不用**（B1 全量非 B4 diff；禁子代理） |
| Arc `audit` | howells | 机械检查+并行评审 | **不用**（同上） |

明确不用（提示词已划掉，本席亦不调用）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`。

---

## 1. 范围指纹（实算，非手抄）

命令：

```bash
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
git ls-files -z -- <7 files> | xargs -0 cat | wc -l
sha256sum <each file>
git rev-parse HEAD
```

| 行数 | SHA256 | 路径 |
|---:|---|---|
| 379 | `ae67f3e41c12d34550f4c88fe8f35627e0ff80595b0597ccedddf5ace8c37a1b` | `apps/web/src/admin/pages/FloorPlanImportPage.tsx` |
| 108 | `03e2d8eb7820f78be45925edc73420082704fba9d7a06c5724a27ad65753dcb2` | `apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx` |
| 155 | `6c1e4f0da9bba28208a7df00d26694c191ebe935d6600ea34b91dcfc1cc32bed` | `apps/web/src/admin/pages/FloorPlanImportPage.test.tsx` |
| 269 | `16b352fd7f3cdd4c69ce88926d8d08da07ee055fdc5369ec841956f464eeab61` | `apps/web/src/admin/pages/SpaceTypesPage.tsx` |
| 106 | `a10d893b558c1c2696b8f6e3e4aa65efe830f3ba9fd21853df33286c0cfdf80f` | `apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx` |
| 248 | `841c1e543bd573d6fbfa7e2773f7aa391e58cee430695199e5bc105be32973a9` | `apps/web/src/admin/pages/VenueBusinessHoursPage.tsx` |
| 88 | `d965ed52b642d46694dc3c7332682b25edd9df6454d2409feabd20a41daf5e38` | `apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx` |
| **1353** | （cat 合计） | 与操作员「当前合计 1353」一致 |

`git diff HEAD -- <7 files>` 空：审计对象 = HEAD blob。

只读追依赖（不纳入本模块队列，缺陷记跨模块观察）：`FloorPlanController`/`FloorPlanService`/`FloorPlanImportParser` 及 DTO；`PlatformController` 空间种类与营业时间端点及 `PlatformService`/`VenueBusinessHoursService`；`adminClient` unwrap/snake；`Can`/`PermissionGate`/`HighRiskConfirm`/`GuidedFlowModal`；`customer/i18n/statusLabels`；`adminRoutes`；V023/V034/V423/V425（及只读见到的 V424/V426）。

---

## 2. 转入既有口径核实（本席独立对照现状，不继承 PASS）

| 既有 ID | 转入摘要 | 本席对 7 文件现状 |
|---|---|---|
| M015-010 | 营业时段草案/生效日/停用另列，XM，前端部分待后端 | **仍成立**。页仍保存即标「已生效」，body 传 `disabled`、不传 `effectiveFrom`；类型有 `effectiveFrom` 只读未用。见 GR-M018-06。B2 应并入 M015-010，不新造业务发明 |
| M015-011 | 发布确认字段 XM | **仍成立**。发布 POST 仅 `idempotency_key`+`candidate_ids`；`HighRiskConfirm` 为 `window.confirm`。见 GR-M018-07 |
| M015-013 | 前端默认值/闭集与后端不一致 | **本工作区前端已与现行八值 CHECK + `svg`/`standard_json` 对齐**；不再默认 geojson。剩余「HTML 导入句三词 vs 八值白名单」在后端/迁移，记观察不新立项 |
| M016-004 | 前端 corridor / sourceFormat 默认 geojson | **本工作区已消除**：`SHAPE_TYPE_LABELS` 无 corridor；未知显示「未识别」；下拉无 geojson；测试钉死。观察，不重复立项 |

---

## 3. 第一性原理线（逐文件三问）

依据只能是：HTML 原文 / 可执行产物 / 外部规范原文 / 可复现命令输出。

### 3.1 `FloorPlanImportPage.tsx`（L1–379）

1. **为何必须存在？** 治理地图「空间和点位」主要功能原文：「商场平面图网页导入：在后台上传或导入商场平面布局图，系统解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误后发布到正式平面图；发布为高危动作，须二次确认——发布会写入正式平面图区域，不可静默覆盖既有布局证据。」（`docs/platform-governance-map-for-operations-v0.1.html:472`）。删掉本页，该句后台网页做不到。
2. **声称是否实际执行？** 页头声称「上传商场平面图，解析出候选区域，人工复核后发布」。可执行：vitest 覆盖引导新建、中文展示、格式下拉，**不覆盖**真实 POST 被后端校验。对照现行契约（`FloorPlanService.validateCreateRequest` L504–528、`V023` `chk_mall_floor_plans_status`）新建体缺 `image_url`/`version_no`/正宽高且 `status:"draft"` 不在 `{active,inactive,archived}`，**第一次正常点「创建平面图」会被拒**。见 GR-M018-01。
3. **未验证前提：** 测试夹具把平面图状态写成 `published`/`draft`、导入写成 `processing`，当成后端会返回的值；现行导入任务闭集是 `parsed|failed|published|cancelled`。见 GR-M018-05/09。

### 3.2 `FloorPlanImportPage.guidedcreate.test.tsx`（L1–108）

1. **为何必须存在？** BSM-003 相关 HTML「后台的新建/录入改成分步引导」（`business-system-map-for-operations-v0.1.html:581`）的测试承载；技术性验证引导弹窗。
2. **声称是否执行？** vitest 该文件 3 例全绿（本席命令输出）。L92 断言 `status === "draft"`，把与后端互斥的请求体钉成「成功」。GEN-003：测试通过不替代行为证据。
3. **未验证前提：** 注释 L9「导入任务/发布候选等动作不在本期改造范围」，但实现已有「创建导入」引导弹窗；本文件未测导入提交。

### 3.3 `FloorPlanImportPage.test.tsx`（L1–155）

1. **为何必须存在？** PGM-001 零英文与闭集展示的回归网。
2. **声称是否执行？** 6 例全绿。走廊夹具 `shape_type:"corridor"` 显示「未识别」、下拉无 geojson：证明 M016-004 前端侧已消。
3. **未验证前提：** 用 `processing`/`published`/`draft` 夹具，不能失败于真实 `parsed`/`active` 展示。无发布/导入 POST 断言。

### 3.4 `SpaceTypesPage.tsx`（L1–269）

1. **为何必须存在？** PGM-001「先管归属：哪个综合体、哪个楼层、哪个区域、哪个点位」；空间建档要种类字典。BSM-003 相关段：配置可存档/删除审批、分步引导、平台管理员与最高权限分开。
2. **声称是否执行？** 引导新建、存档填理由、先存档再申请删除、最高权限审批按钮（`usr_master_authority`+`master-authority` 与 `SessionIssuer` 常量一致）、占用由后端拦。与 `PlatformService.archive/requestDelete/approveDelete` 对得上。缺口：`disabled`（已删除）无取消存档入口，后端 `unarchiveSpaceType` 接受 `disabled`。见 GR-M018-08。
3. **未验证前提：** 前端 master 判断只藏按钮；后端 `approveDeleteSpaceType` 再查 master。信任边界在服务端，本页 UI 门不构成越权成功。

### 3.5 `SpaceTypesPage.guidedcreate.test.tsx`（L1–106）

1. **为何必须存在？** 引导新建与「已删除不再申请删除」的测试网。
2. **声称是否执行？** 5 例全绿。未 POST 存档/删除申请/审批，未断言理由字段。
3. **未验证前提：** mock `api.get` 直接返回数组，不经 `adminClient.unwrap`。

### 3.6 `VenueBusinessHoursPage.tsx`（L1–248）

1. **为何必须存在？** 业务地图客人端「查看综合体介绍、营业时间、活动公告、票务入口」（`business-system-map-for-operations-v0.1.html:856`）；后台必须有配置面，否则客人端无源。BSM-001 策略参数含「时刻」，涉及钱的配置须草案+转正+生效日。
2. **声称是否执行？**  vitest 3 例证明跨零点文案、同时刻拦截、POST `{venueId,dayOfWeek,openTime,closeTime,disabled:false}`。页在已保存非跨零点日显示「已生效」（L223–224），不采集生效日、无草案。与 BSM-001「不即改即生效」冲突，与现行 `VenueBusinessHoursService`「保存即生效」注释一致——即 M015-010 前端残留。见 GR-M018-06。
3. **未验证前提：** `effectiveFrom` 在行类型 L18 存在但保存不传；把后端 `is_draft` 映射来的 `disabled` 当成「本日营业」开关。

### 3.7 `VenueBusinessHoursPage.test.tsx`（L1–88）

1. **为何必须存在？** 跨零点与同时刻的最小网。
2. **声称是否执行？** 3 例全绿，断言的正是「保存即生效」body，不能失败于缺草案/生效日。
3. **未验证前提：** mock client 不走 `normalizeRequestBody`；无权限/无场地/无 `effectiveFrom` 例。

---

## 4. 顺从式线（21 项逐条适用证据）

每条均读锁内定义全文；BSM-003 另读其子段 HTML（存档删除/分步引导/平台管理员与最高权限）；PGM-001 另读治理地图「空间和点位」导入句；BSM-001 另读客人端营业时间句。不可用锁外 ID 支撑 finding。

| ID | 适用 | 证据（本模块） | 结论 |
|---|---|---|---|
| BSM-001 | 适用 | 整体说明+「业务策略一律可配置」+时刻属策略参数+「先存为草案…不即改即生效」。营业时间页保存即「已生效」。GR-M018-06 | **不满足**（冲突，转入 M015-010） |
| BSM-003 | 适用 | 锁内 HTML 原文至 lead；相关子段：空间种类有存档/申请删除/最高权限审批/占用拦截；新建平面图与空间种类走 `GuidedFlowModal`；审批按钮仅 master 可见。删除后无恢复。GR-M018-08 | 部分满足 |
| GEN-001 | 适用 | 三页各有 HTML 上位。`zhOrDash`/`fpLabel` 重复为技术重复，不单独立项 | 满足（重复记观察） |
| GEN-002 | 适用 | 新建平面图体与后端校验互斥；导入名称可空；未勾选发布全部 pending。GR-01/02/03 | **不满足** |
| GEN-003 | 适用 | `npx vitest run` 4 文件 17 passed；断言钉死错误 `draft`、不测发布/导入 POST/存档删除。GR-M018-09 | **不满足** |
| GEN-004 | 适用 | 前端校验名称/payload/同时刻；信任边界在后端。导入名称前端不校验 | 部分（GR-02） |
| GEN-005 | 适用 | 页路由 `floor_plan.view` / `governance.space.view`。写按钮 `floor_plan.manage` 对导入/发布，后端要 `floor_plan.import.manage`（V034 独立权限、high、requires_approval）。下钻 `IconButton` 无门。空间种类审批后端查 master。GR-M018-04 | **不满足**（导入/发布按钮码） |
| GEN-006 | 适用 | 本页不写库；发布一次最多 200 为后端限。前端未勾选可一次丢出全部 pending（可超 200） | 观察+GR-03 |
| GEN-007 | 适用 | 幂等键页内 ref + `adminClient` 补键；发布成功后键不重置，重试走幂等。非极端并发不立项 | 满足 |
| GEN-008 | 适用 | `status:"draft"` vs CHECK 三值；任务状态码表错译/英文；权限码不一致。GR-01/04/05 | **不满足** |
| GEN-009 | 适用 | 引导失败留弹窗。未勾选发布当「发布全部」是静默扩大范围。GR-03 | **不满足** |
| GEN-010 | 适用 | 无密钥/注入面；幂等用 `Math.random` 非秘密。不立项 | 满足 |
| GEN-011 | 适用 | `AuditTrailHint` 收进技术详情；写路径后端 `AuditEventService`。本页不造假成功（除测试 mock） | 满足 |
| GEN-012 | 适用 | 导入 payload 上限在后端 100 万字符；发布 200。前端无提示 | 观察 |
| GEN-013 | 适用 | 模块无 package/CI 文件；`npx tsc -b --noEmit` 在 `apps/web` exit 0 | N/A（无本模块构建文件）+ 命令证据 |
| GEN-014 | 适用 | 7 文件无 SQL。闭集只读 V023/V034/V423/V425 | N/A（迁移不在本模块） |
| GEN-015 | 适用 | 候选/错误行键盘 Enter/Space；空间种类 `window.prompt` 要理由。`parsed`/`archived` 英文状态。GR-05 | **不满足**（零英文） |
| GEN-016 | 适用 | 模块无文档文件；页内描述须与契约一致。引导文案「楼层选填」「初始状态草稿」与后端相反。GR-01 | **不满足**（页内声称） |
| GEN-017 | 适用 | 本 raw 按冲突→细化→技术性→添加；无把业务发明标成技术性 | 本席过程满足 |
| GEN-018 | 适用 | 每条 GR 落到 HTML/命令/源码可复现输出；不引用席甲 raw | 本席过程满足 |
| PGM-001 | 适用 | 底座归属+零英文+见名可点。导入句已引。零英文：`formatBusinessStatus("parsed")` 因码表无键而原样返回（命令见 §8）。见名可点：候选/错误有抽屉；空间种类/营业时间行弱，记观察不单独立项以免扩判据 | **不满足**（零英文，GR-05） |

相关 HTML 原文定位（只读，未改）：

- 治理地图 L463–472「空间和点位」及平面图网页导入句（全文见锁外 HTML，判据仍只用已映射 PGM-001/BSM-003/BSM-001）
- 业务地图 L563–588 系统通用能力（存档/删除审批/分步引导/平台管理员≠最高权限）
- 业务地图 L856 客人端营业时间句

---

## 5. 对抗式线（grilling 结构自问自答）

事实由本席自读，不等操作员、不派子代理。

❓ **Q1 — 第一次正常新建平面图会不会成功？**
➡️ 不会。请求体 L132–135 只送 `idempotency_key/plan_name/project_id/building_code/floor_no/status:"draft"`。后端 `validateCreateRequest` 还要 `floor_no` 非空、`image_url`、`version_no`、正 `image_width/height`、`status∈{active,inactive,archived}`。`V023:15` CHECK 同闭集。`FloorPlanServiceTests.createRequest` 夹具用 `active`+图片 URL+1600×1200+v1。引导测试却断言 `draft` 且 mock fetch 200。

❓ **Q2 — 未勾选「待发布区域」点发布，会不会当成用户已复核？**
➡️ 会。L160 在 `publishCandidateIds.length===0` 时改用全部 `pending` id。复选框默认未勾。HTML 要求人工复核后发布。这是第一次正常「看见复选框、一个都不勾、点发布」就会打出的路径，不是极端并发。

❓ **Q3 — 只授 `floor_plan.manage`、不授 `floor_plan.import.manage` 的分岗账号？**
➡️ 页能进（路由只要 `floor_plan.view`），创建导入/发布按钮 `permissions={MANAGE}` 会亮，POST 被 Controller L71/L91 拒绝。反向：有 import.manage 无 manage 则按钮不亮。V034 把两码分成独立权限。分岗是 HTML/PGM 正常路径。

❓ **Q4 — 导入任务解析成功后状态列是中文吗？**
➡️ 否。`formatBusinessStatus(task.status)`。码表无 `parsed`、无 `archived`（本席 python 抽键：`parsed in_map=False`，`archived in_map=False`）。函数在未知码时 `return code`（`statusLabels.ts:255–259`）。后端任务成功态是 `parsed`。测试夹具用 `processing`（有「处理中」）所以 17 例仍绿。

❓ **Q5 — 最高权限审批能否被前端伪造 user.id 绕过？**
➡️ 按钮可被伪造显示；`PlatformService.approveDeleteSpaceType` L642–644 无 master 则 403。控制器 L215–220 未 `requirePermission`，授权在服务内。不把后端缺口写入本模块队列。

❓ **Q6 — 营业时间是否「时刻」策略、第一次保存是否即生效？**
➡️ 是。BSM-001 明文时刻+草案。页 L223–224「已生效」，L120–126 不传 `effectiveFrom`。第一次保存就影响场次——与现行后端注释一致，与 HTML 冲突。归入已定稿 M015-010 的前端残留。

❓ **Q7 — 为极端并发补幂等算法？**
➡️ 否。第一次正常跑不依赖加密随机；`adminClient` 已补键。不立项。

---

## 6. Findings（临时 GR；B2 再分配正式 ID）

分类顺序均为：冲突 → 细化 → 技术性 → 添加。工程缺陷「成立」= 第一次正常运行可复现。

### GR-M018-01 — 新建平面图请求体与后端/库闭集互斥

- **文件:行：** `apps/web/src/admin/pages/FloorPlanImportPage.tsx:125–135`、`318–343`；测试钉死 `FloorPlanImportPage.guidedcreate.test.tsx:88–92`
- **原文：**

```125:135:apps/web/src/admin/pages/FloorPlanImportPage.tsx
  const createPlan = async () => {
    if (!planForm.projectId.trim()) { setPlanError("请选择所属项目"); return; }
    if (!planForm.planName.trim()) { setPlanError("请填写平面图名称"); return; }
    ...
      await api.post("/api/v1/floor-plans", {
        idempotency_key: planKeyRef.current, plan_name: planForm.planName, project_id: planForm.projectId,
        building_code: planForm.buildingCode, floor_no: planForm.floorNo, status: "draft",
      });
```

引导文案 L319「楼栋/楼层选填」、L343「初始状态：草稿」。

更长上下文：后端 `FloorPlanService.java:33` `STATUSES = active|inactive|archived`；`504–528` 还 `requireText` `floor_no`/`image_url`/`version_no` 且宽高必须 >0；`V023__p1_floor_plan_polygons.sql:15` `CHECK (status IN ('active', 'inactive', 'archived'))`；`image_url VARCHAR(500) NOT NULL`。服务测试夹具 `FloorPlanServiceTests.java:554–566` 用 `active` + `https://assets.example.com/floor-1.png` + 1600×1200 + `v1`。

- **正常触发/影响：** 平台管理员打开「商场平面图导入」→「＋ 新增平面图」→选项目、填名称（楼层可空）→确认创建。后端返回 `VALIDATION_FAILED`（空楼层先撞 `floor_no is required`；填了楼层则撞 `image_url is required`；即便补图也会因 `draft` 撞「状态取值无效」）。页面声称能建草稿平面图，实际建不成。导入链路的前置对象缺失。
- **四分类：** 非与导入句明文相反的业务规则（HTML 未写 draft/图片字段）→ **细化**（平面图网页导入的下位新建）。非纯构建。非添加。
- **工程缺陷：** **成立**
- **冻结 ID：** GEN-002、GEN-008、GEN-016、PGM-001（导入能力做不到）
- **依据类型：** 4 可复现命令（`rg` 页 `status: "draft"`；`rg` `STATUSES = Set.of`；`rg` `chk_mall_floor_plans_status`）+ 后端源码闭集。本机未跑 Java，不伪称后端测试输出。
- **最小拟处置：** 请求体与引导文案改为现行契约：`status: "active"`（或停发 status 由服务默认）；必填楼层；采集或由服务默认 `image_url/version_no/image_width/image_height`（默认策略属后端模块，本页至少不再送非法 `draft`、不再把楼层标成选填）。改引导测试：mock 改为断言合法字段，并增加「draft 必须失败」仅当产品决定保留草稿语义时走 LOCK-AMEND。本模块只修前端字段与文案。

### GR-M018-02 — 创建导入不校验来源名称

- **文件:行：** `FloorPlanImportPage.tsx:142–150`、`356–360`
- **原文：** `validate: () => (importForm.sourcePayload.trim() ? null : "请填写来源内容")`；POST `source_name: importForm.sourceName`（可空串）。
- **更长上下文：** `FloorPlanService.validateImportCreateRequest` L556 `requireText(request.sourceName(), "source_name")`；`V034` `source_name VARCHAR(160) NOT NULL`。
- **正常触发：** 选平面图→创建导入→只贴 SVG/JSON、名称留空→提交。前端放行，后端 `source_name is required`。
- **四分类：** 细化（导入下位字段）
- **工程缺陷：** 成立
- **冻结 ID：** GEN-002、GEN-004、GEN-008
- **依据类型：** 4（`rg` 页 validate 与 `requireText(request.sourceName()`）
- **最小拟处置：** 步内校验名称非空，与后端必填对齐。

### GR-M018-03 — 未勾选候选时发布全部 pending

- **文件:行：** `FloorPlanImportPage.tsx:158–165`、`277–300`
- **原文：** `const ids = publishCandidateIds.length > 0 ? publishCandidateIds : pendingCandidates.map(...)`；复选框默认未勾；`HighRiskConfirm` 无「至少勾选一项」precheck。
- **更长上下文：** HTML L472「由人工复核候选与错误后发布」；后端空列表拒、且一次最多 200（`FloorPlanService.java:376–377,587–591`）。前端在未勾选时把全部 pending 塞进列表，既绕过「选择=复核」，也可能一次超过 200。
- **正常触发：** 打开候选明细（有待发布行）、一个复选框都不勾、确认「发布候选到正式平面图」→全部 pending 被发布。
- **四分类：** 细化（人工复核的下位交互）；与「不可静默覆盖」精神相近但 HTML 未写 checkbox 语义，故非冲突。
- **工程缺陷：** 成立
- **冻结 ID：** GEN-002、GEN-009、PGM-001（导入句复核）
- **依据类型：** 1 HTML 导入句 + 4 `rg` L160
- **最小拟处置：** 未勾选则拦截并中文提示「请勾选要发布的区域」；可选提示 200 上限。不要默认全发。

### GR-M018-04 — 导入/发布按钮权限码与后端不一致

- **文件:行：** `FloorPlanImportPage.tsx:58`、`208–211`、`229`、`293–298`、`220`/`238` IconButton
- **原文：** `const MANAGE = ["floor_plan.manage"];` 用于新增平面图、创建导入、发布。`IconButton` 查看导入/候选无权限门。
- **更长上下文：** `FloorPlanController.java:62–91` 列表/详情 `floor_plan.import.view`，创建导入/发布 `floor_plan.import.manage`。`adminRoutes.ts:95` 整页只要 `floor_plan.view`。`V034:60–61` 独立插入 import.view/manage，risk high、requires_approval TRUE。`V251` 中文名「管理平面图导入和发布候选」≠「管理平面图和多边形」。
- **正常触发：** 分岗只授平面图管理、不授导入管理：按钮可点，API 403。只授导入管理：按钮灭。仅 view：仍可点下钻 IconButton 打 import.view 接口。
- **四分类：** 细化（BSM-003 权限分岗 / GEN-005）
- **工程缺陷：** 成立
- **冻结 ID：** GEN-005、GEN-008、BSM-003
- **依据类型：** 4 `rg` 权限码三处
- **最小拟处置：** 创建导入/发布改 `floor_plan.import.manage`；下钻改 `floor_plan.import.view`（或隐藏无权限下钻）。新建平面图仍 `floor_plan.manage`。

### GR-M018-05 — 状态展示英文或错域中文

- **文件:行：** `FloorPlanImportPage.tsx:45–49`、`218–219`、`236`；码表 `apps/web/src/customer/i18n/statusLabels.ts:23–24,44–45,255–259`
- **原文：** `floorPlanStatusLabel` 只特判 `published→已发布`、`draft→草稿`，否则 `formatBusinessStatus`。导入任务列直接 `formatBusinessStatus(task.status)`。`formatBusinessStatus`：未知码原样返回。
- **更长上下文：** 命令：`parsed in_map=False`、`archived in_map=False`、`active in_map=True`（「有效」）、`published in_map=True`（「已上架」）。后端平面图状态 `active|inactive|archived`；导入任务 `parsed|failed|published|cancelled`。PGM-001：「不得出现任何英文（如 active/draft 英文状态）」。测试 `FloorPlanImportPage.test.tsx:8–14,79–86` 用 `published`/`draft`/`processing`，断言中文且 `not.toContain("published")`，**不能**在 `parsed` 上失败。
- **正常触发：** 列表真实平面图 `active` 显示「有效」（会员域）；`archived` 显示英文 `archived`。导入成功任务显示英文 `parsed`；发布后任务显示「已上架」。
- **四分类：** **冲突**（与 PGM-001 零英文明文，含「英文状态」例句）优先于细化
- **工程缺陷：** 成立
- **冻结 ID：** PGM-001、GEN-015、GEN-008、GEN-003
- **依据类型：** 1 PGM-001 HTML 零英文段 + 4 python 抽码表键
- **最小拟处置：** 页内映射 `active→启用/有效平面`（需中文业务词，不得用会员「有效」）、`inactive`、`archived→已存档`、`parsed→已解析`、任务 `published→已发布`。测试夹具改真实闭集。

### GR-M018-06 — 营业时段保存即生效、停用走 disabled、无生效日（转入 M015-010）

- **文件:行：** `VenueBusinessHoursPage.tsx:102–126`、`218–224`；测试 `VenueBusinessHoursPage.test.tsx:73–86`
- **原文：** POST `{ venueId, dayOfWeek, openTime, closeTime, disabled }` 无 `effectiveFrom`；已保存非跨零点显示「已生效」。
- **更长上下文：** BSM-001 L2295「所有涉及钱的配置一律版本化…先存为草案、经高敏权限转正…不即改即生效」；「时刻」为策略参数；「无法判断是否涉及钱的一律按涉及钱处理」。客人端 L856 要展示营业时间。现行 `VenueBusinessHoursService.java:24–26` 仍写「保存即生效」「is_draft 复用为停用」。
- **正常触发：** 选场地、填开歇、保存 → 文案「已生效」，无草案、无生效日。
- **四分类：** **冲突**（BSM-001 明文）
- **工程缺陷：** 成立（前端部分；与后端现状同批）
- **冻结 ID：** BSM-001
- **依据类型：** 1 BSM-001 HTML
- **最小拟处置：** 并入既有 M015-010 XM，不新造发明。本页在后端草案契约落地时：去掉「已生效」即改语义、停用与草案分列、采集生效日。在后端未改前单独改前端会与现行 API 再分裂——故修复队列归属由 B2 按 XM 维持。

### GR-M018-07 — 发布请求无确认字段（转入 M015-011）

- **文件:行：** `FloorPlanImportPage.tsx:163–165`、`293–300`
- **原文：** body 仅 `idempotency_key`、`candidate_ids`。`HighRiskConfirm` = `window.confirm`。
- **更长上下文：** `FloorPlanImportPublishRequest.java` 无确认字段。HTML「发布为高危动作，须二次确认」——UI 弹窗已满足「二次确认」字面；既有 XM 要求服务端确认闸+契约字段。V034 import.manage `requires_approval=TRUE`。
- **正常触发：** 点发布 → 浏览器 confirm → POST 无确认字段。
- **四分类：** 细化（二次确认的下位实现）
- **工程缺陷：** 成立（相对既有 M015-011 服务端闸；相对 HTML 字面 UI 已有确认）
- **冻结 ID：** PGM-001、GEN-005
- **依据类型：** 1 导入句「须二次确认」+ 4 `rg` 发布 body
- **最小拟处置：** B2 并入 M015-011；前端在契约加字段后同批送确认值。

### GR-M018-08 — 已删除空间种类无恢复入口

- **文件:行：** `SpaceTypesPage.tsx:22–27`、`172–220`
- **原文：** `disabled→已删除`；`取消存档`/`申请删除` 仅 `status==="archived"`；`disabled` 行无按钮。
- **更长上下文：** BSM-003 相关 HTML「删除只隐藏不抹除…可查、可审计、可恢复」。`PlatformService.unarchiveSpaceType` L593 允许 `archived` 或 `disabled` 回到 `active`。第一次：审批通过删除后，本页无法恢复。
- **四分类：** 细化（删除可恢复）
- **工程缺陷：** 成立
- **冻结 ID：** BSM-003、GEN-002
- **依据类型：** 1 存档删除段 HTML + 服务源码
- **最小拟处置：** 对 `disabled` 提供「恢复」走既有 unarchive；或说明已删除只能走数据清理工具（若走后者须能指到 HTML 清理工具句，仍是细化）。

### GR-M018-09 — 测试网不能失败于主路径契约错误

- **文件:行：** 四个测试文件全文；尤其 `FloorPlanImportPage.guidedcreate.test.tsx:73–95`、`FloorPlanImportPage.test.tsx:8–14,74–97`、`SpaceTypesPage.guidedcreate.test.tsx` 无 archive/delete POST、`VenueBusinessHoursPage.test.tsx:73–86`
- **原文：** 引导测试 `expect(status).toBe("draft")` 且 mock 200；导入测试用 `processing`；无 publish POST；无 source_name 空串；无权限码。
- **正常触发：** `npx vitest run` 17 passed（本席实跑）同时 GR-01/02/03/04/05 仍在。
- **四分类：** 技术性（测试无业务发明）但 **查出工程缺陷**
- **工程缺陷：** 成立
- **冻结 ID：** GEN-003
- **依据类型：** 4 vitest 输出（17 passed / exit 0）对照 GR-01 契约
- **最小拟处置：** 引导测试断言合法 `active`+必填字段（或断言非法体）；增加导入名称空、未勾选发布、`parsed` 中文、权限码用例。不测后端 Java。

---

## 7. 观察（不进修复队列建议）

| 编号 | 内容 | 分类 |
|---|---|---|
| OBS-01 | M016-004 corridor/geojson 默认在本工作区 7 文件中已消除；测试钉死「未识别 / 无走道 / 无 geojson」 | 登记 |
| OBS-02 | M015-013 前端标签现含 booth/public_area 及 area/entrance/facility/risk_zone/route，与 V423 八值一致；HTML 导入句三词 vs 八值属后端闭集，不在本页发明 | 跨模块观察 |
| OBS-03 | 空间种类/营业时间行无抽屉式见名可点；平面图主表用 IconButton 下钻。PGM-001 全局原则，本席不单独立项以免把全后台表格模式扩成新判据 | 观察 |
| OBS-04 | 存档使用 `HighRiskConfirm` 且默认「不可逆」，但存档可取消。日常存档 HTML 不用审批≠不能确认 | 观察 |
| OBS-05 | SVG 缺 label 时解析器默认 `shape-N`（英文），展示在本页标签列。默认来自 `FloorPlanImportParser.java:99` | 跨模块（M015/M017） |
| OBS-06 | 营业时间是按日保存而非「＋新建」，未接 `GuidedFlowModal`。BSM-005 列举「正持续改造」，不强制本页已完工 | 观察 |
| OBS-07 | `PlatformController.deleteSpaceType` 仍在，本页不调用；服务抛须审批 | 跨模块 M009 |
| OBS-08 | `approveDeleteSpaceType` 控制器无 `requirePermission`，服务内 master 检查 | 跨模块 M009 |
| OBS-09 | `zhOrDash` 与 `fpLabel` 同实现 | ponytail shrink，无业务发明 |
| OBS-10 | 后端 `requireText` 英文 `"field is required"` 会回显到本页错误区 | 跨模块 |
| OBS-11 | HEAD 含未合并到 origin/main 的 PR #439；本席按工作区现状审，不把该 PR 当 PASS | 事实 |
| OBS-12 | 本机未跑 Maven/`complex_test`；后端行为依据源码+既有测试夹具原文 | 证据边界 |
| OBS-13 | `pending_delete` 不在全局码表，页内已映射「待删除审批」 | 正向 |
| OBS-14 | 发布成功后 `publishKeyRef` 不清空，重试走幂等 | 正向 |

ponytail-review：`FloorPlanImportPage.tsx:L41-44` duplicate helpers。`net: -4 lines possible.` 不单独立项。

---

## 8. 可执行证据（本机，只读）

### 8.1 vitest

```bash
cd /home/james/complex-management/apps/web && npx vitest run \
  src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx \
  src/admin/pages/FloorPlanImportPage.test.tsx \
  src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx \
  src/admin/pages/VenueBusinessHoursPage.test.tsx \
  --reporter=verbose
```

（首次误把 `FloorPlanImportPage.tsx` 列入路径，vitest 忽略非测试文件，实际仍 4 个测试文件。）

输出摘要：

```
 Test Files  4 passed (4)
      Tests  17 passed (17)
   Start at  00:26:52
   Duration  1.88s
```

exit code **0**。17 例清单见工具输出；含引导创建断言 `status:"draft"`、导入 `processing` 夹具。**绿 ≠ 主路径正确**（GEN-003）。

### 8.2 tsc

```bash
cd /home/james/complex-management/apps/web && npx tsc -b --noEmit
```

无诊断输出。`TSC_EXIT:0`（本席复查再跑一次同结果）。

### 8.3 码表键

```bash
python3 - <<'PY'
# 读 apps/web/src/customer/i18n/statusLabels.ts 对象键
```

输出：`parsed in_map=False`；`archived in_map=False`；`active in_map=True`；`published in_map=True`；`draft in_map=True`；`pending in_map=True`；`processing in_map=True`。

### 8.4 未跑

后端 Maven / `complex_test` / 浏览器 E2E：runbook 写明本机仅 node；提示词允许 vitest+tsc。不把未跑 Java 折成 PASS。

---

## 9. 文件覆盖表（全文，非抽样）

| 文件 | 行 | 覆盖 | 三问 | 主要 finding/观察 |
|---|---:|---|---|---|
| FloorPlanImportPage.tsx | 1–379 | 全文 | §3.1 | GR-01..05,07 |
| FloorPlanImportPage.guidedcreate.test.tsx | 1–108 | 全文 | §3.2 | GR-01,09 |
| FloorPlanImportPage.test.tsx | 1–155 | 全文 | §3.3 | GR-05,09；OBS-01 |
| SpaceTypesPage.tsx | 1–269 | 全文 | §3.4 | GR-08；OBS-04/07/08 |
| SpaceTypesPage.guidedcreate.test.tsx | 1–106 | 全文 | §3.5 | GR-09 |
| VenueBusinessHoursPage.tsx | 1–248 | 全文 | §3.6 | GR-06；OBS-06 |
| VenueBusinessHoursPage.test.tsx | 1–88 | 全文 | §3.7 | GR-06,09 |

无 OVERSIZE-SINGLE-FILE。

---

## 10. 本席结论

- 覆盖：7/7 文件全文；21/21 判据有适用证据
- 三线：第一性原理 / 顺从式 / 对抗式均执行
- 临时 finding：GR-M018-01…09（其中 06/07 建议 B2 并入既有 M015-010/011）
- 添加项：无
- HTML 自身问题：未发现须铁律 4 上报的三 HTML 缺陷
- 未答 questions：无（B1 不发问）
- **单席审计结论：FAIL**

FAIL 理由（缺一即不得 PASS）：存在未定稿工程缺陷（至少 GR-01 新建平面图主路径不可用、GR-05 零英文、GR-03 未选即全发、GR-04 权限码错位）。vitest 全绿与 tsc 0 已记录，不能推翻上述缺陷。

本席不修复、不改计划/台账。B2 归并时以本 raw 最后一次 `CRITERIA-LOCK-SHA256:` 起至文末为本份完整报告。

## M018 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 36814 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6 xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（铁律共 **7** 条）；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M018 FILES 7 / 锁映射 21 / 计划冻结 1080 行，现文 1353 行）
- 正本：`/home/james/complex-management`。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `1343640d28954385971e1ac71d9c0ada27dc251f`（PR #439 仍 OPEN）。本模块 7 文件指纹与 B1 覆盖表全等。B3=0。无源修复。三份 HTML 现文：治理地图 V0.12、业务地图 V0.14、枚举 V0.07。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类。本席 **未读** `M018-b1-self-raw.md` 在 34780 字节之后的 V0.02 B2 结论段。
- 本席 B2-r1：**PASS**（001–009 逐 ID 全 PASS；GR-06/07 并入 M015-010/011 不新开 ID。添加 0、不发 2.2。非阻断勘误见 002 字段闭集，不否决）
- 正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 `M018 → [BSM-001, BSM-003, GEN-001…GEN-018, PGM-001]` COUNT=21 unique=21。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要复现只读（源码/`rg`/哈希/从 `adminClient.ts` 抽取函数的 node 探针）；未对任一 `complex_test*` 库做写操作。

此票只判断 findings.md V2.53 顶部「当前 M018 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M018-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 36814 | `e94832c95ab57fad2f621a173ba9a3894c57bc779368e2c8f1da260962cd9f6a` |
| `docs/audit-2026-09/modules/M018-b1-self-raw.md` 前缀（席甲 B1；本席只读此前缀） | V0.01 | 34780 | `fa76354a020730444690b1e50cbab8e54f1de33caa815cb8652621523d62a9a6` |
| `docs/audit-2026-09/findings.md` | V2.53 | 1004690 | `430c9671431fb76570e0b1b9dd6035604b1abc6fc1f3262f8390bba98f6ac058` |

席甲 raw 全文现 36566 bytes（SHA `2e6570269fc8be13291ef55311736851ab7eb672c0130bb15530f31c0342522d`）；本席按指令只取前 34780 字节，不读其后 V0.02 B2 段。本文件独占锁哈希字段行仍仅文首一处（B1 文末提及该字段名的说明句不算新独占行），本段未新增。

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

本模块 7 文件指纹（与 B1 覆盖表 match）：

| 路径 | 行 | bytes | SHA256 |
|---|---:|---:|---|
| `FloorPlanImportPage.tsx` | 379 | 22672 | `ae67f3e41c12d34550f4c88fe8f35627e0ff80595b0597ccedddf5ace8c37a1b` |
| `FloorPlanImportPage.guidedcreate.test.tsx` | 108 | 5307 | `03e2d8eb7820f78be45925edc73420082704fba9d7a06c5724a27ad65753dcb2` |
| `FloorPlanImportPage.test.tsx` | 155 | 6636 | `6c1e4f0da9bba28208a7df00d26694c191ebe935d6600ea34b91dcfc1cc32bed` |
| `SpaceTypesPage.tsx` | 269 | 12783 | `16b352fd7f3cdd4c69ce88926d8d08da07ee055fdc5369ec841956f464eeab61` |
| `SpaceTypesPage.guidedcreate.test.tsx` | 106 | 5615 | `a10d893b558c1c2696b8f6e3e4aa65efe830f3ba9fd21853df33286c0cfdf80f` |
| `VenueBusinessHoursPage.tsx` | 248 | 10962 | `841c1e543bd573d6fbfa7e2773f7aa391e58cee430695199e5bc105be32973a9` |
| `VenueBusinessHoursPage.test.tsx` | 88 | 3798 | `d965ed52b642d46694dc3c7332682b25edd9df6454d2409feabd20a41daf5e38` |

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核源/HTML/命令再接受或反驳；禁止表演性同意；B2 不修复。本批重点独立核 004/005（本席 B1 未发现 / 误判 GEN-007 满足） |
| grilling | 只借追问结构自查：001 前端采集是否本模块最小、002 用词有无发明、003 下钻 Can 包裹、008 未勾选拦截、009 走既有 unarchive、006 用例是否覆盖各 ID、有无应升添加 |
| verification-before-completion | 声称前先跑只读哈希/行号/源码/抽取函数探针 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 本模块深度落在三页适配器对齐既有后端闭集/权限码/契约字段，不把默认策略发明进后端 |
| ponytail-review | 只评最小删除/收窄，不落地。001 不改后端默认；008 用既有 `precheck`；009 不新增端点；005 改 camel 键与 SpaceTypesPage 同范式 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |

### 完整差集（GR-M018-01…09、OBS-01…14、席甲 CX-M018-001…006、OBS-1…11）

漏并：无。改写原意：无（分类改判见 002/003，事实未改）。两席 B1 全部临时缺陷 ID 仍落在 001–009 或既有 XM。无单席独有缺陷被丢。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M018-001 | GR-M018-01 + CX-M018-001 | 细化/是/是 | **PASS（细化/是/是；前端采集必填为本模块最小）** |
| M018-002 | GR-M018-05 + CX-M018-002 | 冲突/是/是 | **PASS（冲突优先 PGM-001 零英文；页内用词无业务发明。非阻断：错误字段实产 6 键）** |
| M018-003 | GR-M018-04 + CX-M018-003 | 细化/是/是 | **PASS（细化 BSM-003 权限分岗；含下钻 Can 包裹 import.view）** |
| M018-004 | CX-M018-004（本席 B1 未发现） | 技术性/是/是 | **PASS（独立核源成立）** |
| M018-005 | CX-M018-005（本席 B1 误判 GEN-007 满足） | 技术性/是/是 | **PASS（独立探针成立）** |
| M018-006 | GR-M018-09 + CX-M018-006 | 技术性/是/是 | **PASS（技术性/是/是；清单覆盖 002–009，001 用例在 001 自身）** |
| M018-007 | GR-M018-02（席甲 B1 未发现） | 细化/是/是 | **PASS** |
| M018-008 | GR-M018-03（席甲 B1 只记 OBS-1） | 细化/是/是 | **PASS（未勾选拦截、不默认全发）** |
| M018-009 | GR-M018-08（席甲 B1 只记 OBS-9） | 细化/是/是 | **PASS（disabled 行走既有 /unarchive）** |
| （不新开） | GR-M018-06 + 席甲 OBS-4 | 并入 M015-010 否/XM | **PASS（不新开 ID）** |
| （不新开） | GR-M018-07 + 席甲 OBS-5 | 并入 M015-011 否/XM | **PASS（不新开 ID）** |

冲突入队 1（002）。细化入队 5（001、003、007、008、009）。技术性入队 3（004、005、006）。观察否：双方 OBS 按候选「只登记不入队」。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001–009（9）；「否」本批新开 = 0。

### 一、差集核实

| B1 临时 ID | 候选落点 | 核实 |
|---|---|---|
| GR-01 + CX-001 | 001 | 同根：新建体缺 image_url/version_no/正宽高且 `status:"draft"` 不在 `{active,inactive,archived}`。未改写。 |
| GR-05 + CX-002 | 002 | 同根：平面图/任务/候选展示 + 错误字典。席甲原判细化，候选改冲突＝本席 B1 原判，事实未改写。 |
| GR-04 + CX-003 | 003 | 同根：创建导入/发布码 `floor_plan.manage` ≠ 后端 `floor_plan.import.manage`；下钻无门。候选最小处置并入本席「下钻隐藏/改 import.view」，未丢。 |
| CX-004 | 004 | 席甲独有，本席 B1 未立。候选原样入队。见下节独立核实：**成立**。 |
| CX-005 | 005 | 席甲独有；本席 B1 把 GEN-007 写成满足。候选原样入队。见下节独立核实：**成立**。 |
| GR-09 + CX-006 | 006 | 同根：17 例绿不能失败于主路径。候选把各 ID 的最小用例收进同一测试网，未改「测试无效」原意。 |
| GR-02 | 007 | 本席独有：导入 `source_name` 可空。未改写。 |
| GR-03 | 008 | 本席独有（席甲 OBS-1）。升队合法：B1 已立工程缺陷，不是把观察偷偷升添加。 |
| GR-08 | 009 | 本席独有（席甲 OBS-9）。升队合法：B1 已立「已删除无恢复」。 |
| GR-06 | M015-010 | 既有冲突/否/XM（含本页前端）。不新开。与席甲 OBS-4 同根。 |
| GR-07 | M015-011 | 既有细化/否/XM（契约加字段后前端同批）。不新开。与席甲 OBS-5 同根。 |

观察未入队（本席核：无应升添加、无应升本批缺陷被降）：席甲 OBS-2/3/6/7/8/10/11；本席 OBS-01…14（含 PR #439 未合并 origin/main、`shape-N` 默认标签属解析器/M015/M017、`approveDeleteSpaceType` 控制器无 requirePermission 属 M009、`zhOrDash`/`fpLabel` 重复）。席甲 OBS-1/9 已分别升 008/009，不在观察里留应升项。

### 二、004 / 005 独立核实（本席 B1 未发现 / 误判满足）

#### 004 · 下钻 `loadImports`/`loadCandidates` 无 try/catch 静默失败 —— **成立**

核源（只读，行号与候选一致）：

- `FloorPlanImportPage.tsx:106-114` `loadImports`：`await api.get(.../imports)` 后 `setImports`，函数体无 `try/catch`。
- `:116-122` `loadCandidates`：`await api.get(.../candidates)` 后 `setFloorPlanImportDetails`，无 `try/catch`。
- `:220` `onClick={() => void loadImports(plan.id)}`；`:238` `onClick={() => void loadCandidates(...)}`。`void` 丢弃 Promise，拒绝成为未处理拒绝。
- 对照同页 `:139` `createPlan`、`:155` `createImport`、`:168` `publishImport` 均 `catch` 后 `setPlanError`/`setImportError`/`setGlobalError`。
- 后端 `FloorPlanController.java:62/:81` 两处下钻要 `floor_plan.import.view`；路由 `adminRoutes.ts:95` 整页只要 `floor_plan.view`。仅 view 的账号第一次点「查看导入任务」即 403。
- GEN-009：错误须可检测，不得吞错。页面无提示、旧表不变＝吞错。

四分类：**技术性**（补 catch + 显错，无业务发明）。工程缺陷：**是**。队列：**是**。最小处置（两函数 `try/catch` → `setGlobalError((e as Error).message)`；+1 例 imports 403）即原意且最懒。票：**PASS**。

#### 005 · 页面 snake `idempotency_key` 被 `normalizeRequestBody` 生成键覆盖 —— **成立**

核源：

- 页 `:131-135/:146-150/:162-165` 传 `idempotency_key: xxxKeyRef.current`（`:131` 生成 `fp-…`，失败不重置 ref，意图重试复用）。
- `adminClient.ts:90-92` 原文：`mapKeys({ ...body, idempotencyKey: body.idempotencyKey ?? nextIdempotencyKey() }, toSnakeCaseKey)`。只认 camel `idempotencyKey`；缺则 `nextIdempotencyKey()` → `admin-web-${Date.now()}-…`。随后 `toSnakeCaseKey("idempotencyKey")` 与页面已有 `idempotency_key` 撞同一键，`Object.fromEntries` 后写覆盖先写。
- 对照 `SpaceTypesPage.tsx:81` `idempotencyKey: createKeyRef.current`（camel）走保留分支。

本席独立抽取 `isPlainObject`/`toSnakeCaseKey`/`mapKeys`/`nextIdempotencyKey`/`normalizeRequestBody`（生成串钉成 `admin-web-<generated>` 以便对照），node 实跑：

```
floorplan_page_body.idempotency_key = "admin-web-<generated>"   // 页面 fp-PAGEKEY 丢失
import_page_body.idempotency_key    = "admin-web-<generated>"
publish_body.idempotency_key        = "admin-web-<generated>"
spacetype_page_body.idempotency_key = "space-type-PAGEKEY"      // camel 保留
floorplan_camel_body.idempotency_key= "fp-PAGEKEY"              // 候选最小处置的对照
```

与席甲 §2 探针 5 同结论。本席 B1 Q7/GEN-007「adminClient 已补键」把覆盖当成补键，**撤回该满足判定**。第一次成功落库、响应失败后重试携新键 → 重复创建平面图/导入任务。GEN-007/GEN-008。

四分类：**技术性**（改键名对齐既有客户端契约，无业务发明）。工程缺陷：**是**。队列：**是**。最小处置：三处改 `idempotencyKey: xxxKeyRef.current`；guidedcreate 断言 `lastPostBody.idempotency_key` 以 `"fp-"` 开头。票：**PASS**。

（Venue 页不传键、每次生成新键＝席甲 OBS-7，upsert 唯一性在后端，本批不升队。）

### 三、分类

2.1 顺序：冲突 → 细化 → 技术性 → 添加。能指到 HTML 上位的一律先归细化，不进技术性。

**002 冲突（非细化）**：PGM-001 锁内 HTML 原文「必须全部为中文，不得出现任何英文（如 active/draft 英文状态）」。本席 python 抽码表：`parsed in_map=False`、`archived in_map=False`；`formatBusinessStatus` 未知码 `return code`（`statusLabels.ts:255-259`）。后端任务成功态 `parsed`、平面图 `archived` 第一次正常列表即英文直出。这是与零英文明文矛盾，冲突优先于「补中文映射」的细化。席甲 B1 原判细化不成立。候选改冲突＝本席 B1 GR-05。票：**PASS（冲突）**。

**003 细化（非技术性）**：BSM-003 标题即「权限分岗」；同节 BSM-007「其他岗位按岗位分配各自能看能做的范围」。V034 把 `floor_plan.import.manage` 做成独立高危码（`requires_approval=TRUE`）。按钮码与后端 `requirePermission` 不一致是该分岗的下位实现，能指到 HTML，2.1 禁止进技术性。席甲原判技术性不成立。候选改细化＝本席 B1 GR-04。票：**PASS（细化）**。

**001 细化上位**：治理地图 L469「维护楼层、区域、点位…」、L472「在后台上传或导入商场平面布局图」。HTML 未写 draft/图片字段，故非冲突；底图/楼层/版本是 V023/OpenAPI 既有契约的下位，指得出。票：**PASS（细化）**。

**007 细化上位**：L472 导入链路的 `source_name` 下位字段；后端 `requireText(sourceName)` + V034 `source_name VARCHAR(160) NOT NULL`。票：**PASS（细化）**。

**008 细化上位**：L472「由人工复核候选与错误后发布」。页已有「待发布区域」复选框作为复核面；未勾却把全部 pending 塞进 POST 是静默扩大范围（GEN-009），不是 HTML 禁止「全发」的明文冲突，也不是新发明复核流程。票：**PASS（细化）**。

**009 细化上位**：BSM-003 节下 L571「删除只隐藏不抹除…可查、可审计、可恢复」；本页对 `archived` 已有「取消存档」走 `/unarchive`；后端 `unarchiveSpaceType:593` 已允许 `disabled`。给 `disabled` 行补同一入口是「可恢复」的下位，不新造状态/端点。票：**PASS（细化）**。

**004/005/006 技术性、无业务发明**：catch 显错、camel 幂等键、补能失败断言。不引入实体/枚举/状态/权限/价格/流程。票：**PASS（技术性）**。

### 四、最小处置

**001 前端采集是否本模块内最小 —— 是。** 本席 B1 写过「采集或由服务默认（默认策略属后端模块）」。独立再核：`validateCreateRequest:521-527` 对 `floor_no`/`image_url`/`version_no`/正宽高/`status∈STATUSES` 均 `require*`，`createFloorPlanOnce:239-243` 原样写入、无默认；OpenAPI `FloorPlanCreateRequest` required 含 `floor_no/plan_name/image_url/image_width/image_height/version_no/status`，status enum 三值无 draft；V023 五列 NOT NULL + `CHECK (status IN ('active','inactive','archived'))`。现行契约无默认。在本模块发明服务默认＝2.1「业务默认值」→添加，且属 M015。引导弹窗补齐既有必填并送 `status:"active"`（闭集内取值，不发明 draft）是本 7 文件内唯一最小路径。楼层改必填对齐 `floor_no NOT NULL`。不改后端/OpenAPI/HTML。票：**PASS**。

**003 下钻以 Can 包裹 `import.view` —— 同意。** `Can.tsx:22-36` 无权限默认 `fallback=null`（隐藏），与本席 B1「下钻改 import.view（或隐藏无权限下钻）」的隐藏支同构。`HighRiskConfirm`/`PermissionButton` 已有 `permissionsState` 门；两处 IconButton 现无门。创建导入/发布改 `IMPORT_MANAGE=["floor_plan.import.manage"]`；新增平面图仍 `floor_plan.manage`。路由不改。票：**PASS**。

**008 未勾选拦截、不默认全发 —— 同意。** `HighRiskConfirm` 已有 `precheck?: () => boolean`（`:13/:65-67`）。未勾选时 precheck 拦 + `setGlobalError("请勾选要发布的区域")`；`ids` 只取 `publishCandidateIds`。删除 `:160` 的 pending 回填。不发明新确认组件、不默认全发。票：**PASS**。

**009 `disabled` 行恢复走既有 unarchive —— 同意。** `unarchiveType:115-125` 已 POST `/api/v1/platform/space-types/${id}/unarchive`；后端 `:593` 接受 `archived|disabled`。`disabled` 行加「恢复」`HighRiskConfirm`（权限 `governance.space.manage`），文案「把已删除种类「X」恢复为启用」与 archived 行「恢复为启用」同构。不新增端点/状态。票：**PASS**。

**002 页内映射用词有无业务发明 —— 无。** 闭集仍是后端三套码，只换展示中文：
- 平面图 `active`「启用」/ `inactive`「停用」/ `archived`「已存档」——同页空间种类已用「启用」「已存档」；不用会员域 `formatBusinessStatus("active")`＝「有效」。
- 任务 `parsed`「已解析」/ `failed`「解析失败」/ `published`「已发布」/ `cancelled`「已取消」——「解析/发布」是 L472 动词；`cancelled` 全局表已是「已取消」；不用「已上架」。
- 候选 `pending`「待复核」/ `published`「已发布」/ `rejected`「已驳回」——「复核」是 L472 动词；`rejected` 全局表已是「已驳回」。
不新增枚举值/状态机。错误码 10 个与解析器中文 `errorMessage` 同义。非阻断：候选写「错误字段 5 键」漏 JSON 路径 `polygon_points`（`FloorPlanImportParser.java:138`）。解析器实产字段为 `source_format` / `viewBox` / `points` / `shapes` / `source_payload` / `polygon_points`（6）。B3 按实产闭集映射，不得只抄 5 而留 JSON 错误「未识别」。10 码闭集（含 `PAYLOAD_REQUIRED`）成立。不否决。票：**PASS**。

**006 用例清单是否覆盖各 ID —— 覆盖（001 的创建体例在 001 自身）。**

| ID | 候选 006 / 各 ID 自带例 |
|---|---|
| 001 | 001 自身：guidedcreate 断言完整 body + status active；缺楼层步内拦截 |
| 002 | parsed/archived 中文与真实错误码 1 |
| 003 | 无 import.manage 时发布按钮 disabled 1 |
| 004 | imports 403 → setGlobalError 1 |
| 005 | lastPostBody.idempotency_key 以 "fp-" 开头 1 |
| 007 | 创建导入 1 + 来源名称空被拦 1 |
| 008 | 勾选 1 个候选→confirm→POST candidate_ids 1；未勾选被拦 1 |
| 009 | 存档/申请删除/审批/恢复各 1（stub prompt/confirm） |
| 营业时间页（006 原面，非新 ID） | 停用 1、降级提示 1 |

只改 4 个测试文件（模块内仅此 4 个 `*.test.tsx`）。不测 Java。改坏 `publishImport.candidate_ids` / `approveDeleteType.decision` 须能红。票：**PASS**。

### 五、添加 / HTML / questions

本批无添加项、无 HTML 申请、无 questions。本席逐条核：001 采集的是 OpenAPI/V023 已有必填，不是新业务字段；002 不新增码；008 不发明「必须全选」规则（复选框已在页上）；009 不发明恢复 API。无应升 2.2 的项。不 BLOCKED-ASK。

### 分类 / 队列 / 允许的最小修复边界

本席 B2-r1 **PASS**。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3 只改本模块 7 文件（三页 + 四测试），不改后端/OpenAPI/迁移/三份 HTML/锁/`adminClient.ts`/`statusLabels.ts` 全局表。

- 001：弹窗补楼层必填、底图地址/宽/高、版本号；提交 `status:"active"`；validate/summary 同步；guidedcreate 改断言完整 body + 缺楼层拦截。不发明 draft、不改服务默认。
- 002：页内字典改后端闭集（用词见上）；错误字段按解析器 6 键、错误码 10 码中文短语；夹具改真实码。只改本页 + `.test`。
- 003：`IMPORT_MANAGE` 用于创建导入与发布；两处下钻 IconButton 以 `Can permissions={["floor_plan.import.view"]}` 包裹；+1 权限例。路由不改。
- 004：两 load 函数 try/catch → setGlobalError；+1 403 例。
- 005：三处 `idempotencyKey`；断言键以 `fp-` 开头。
- 006：上表用例；只改 4 个测试文件。
- 007：步内校验来源名称非空（「请填写来源名称」）。
- 008：未勾选 precheck 拦截，不默认全发。
- 009：`disabled` 行「恢复」走既有 `/unarchive`。
- GR-06/07：不修；等 M015-010/011 XM。

禁止：改 FloorPlanService/Parser/Controller/OpenAPI/V023、发明 draft 或服务端图片默认、改全局 `statusLabels` 把会员「有效/已上架」改成平面图词、新增恢复端点、把未勾选改成「默认全发加文案」、给 VenueHours 单改草案/生效日、加二次确认契约字段。

### 结论

全批差集无漏并、无原意改写。004/005 经源码与抽取函数探针独立核实成立，本席 B1 对 GEN-007 的满足判定撤回。002 冲突、003 细化、001/007/008/009 细化上位指得出。001 前端采集为本模块最小。008 未勾选拦截。009 走既有 unarchive。002 用词无业务发明。006 覆盖各 ID。添加 0。非阻断：002 错误字段按解析器 6 键（含 `polygon_points`）。逐 ID：001 PASS、002 PASS、003 PASS、004 PASS、005 PASS、006 PASS、007 PASS、008 PASS、009 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M018 IN_PROGRESS；B3=0。

## M018 B2-r2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.03（本段追加；上方 V0.01 B1 + V0.02 B2-r1 前缀 **60271** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：固定 Grok 只读外审席乙（grok-4.6 / xhigh；独立）。步骤：B2-r2 整批候选复核，不是 B1/B5 重审、不是修复、不是模块 PASS、不是 B3。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（铁律共 **7** 条）；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M018 FILES 7 / 锁映射 21 / 计划冻结 1080 行，现文 1665 行）
- 正本：`/home/james/complex-management` main 主工作区。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `1343640d28954385971e1ac71d9c0ada27dc251f`（未 fetch）。本模块 7 文件指纹与 B5-r1 覆盖表全等（B3-r1 after，未再改源）。B3 累计 1；本批定稿后 B3-r2 为第 2 次。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账。本席 **未读** `M018-b1-self-raw.md` 本次新增的 V0.03 B2-r2 结论段。
- 背景：M018 B5-r1 本席票 FAIL（GR-M018-B5-01）；席甲 B5 票 PASS 经核源撤回为 FAIL；合取 FAIL。依据无 ID 且 001–009 全否 → 按回退统一规则整批回 B2。
- 本席 B2-r2：**PASS**（差集一一对应、无第三条、无改写；010 分类细化、队列是、最小处置成立；001–009 不动；无添加项、无 HTML 申请、无 questions）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 同值。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 fetch/commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要复现只读（源码 / `rg` / 哈希 / graft CLI 本地检索）；未对任一 `complex_test*` 库做写操作。

此票只判断 findings.md V2.56 顶部「当前 M018 B2-r2 整批候选」相对本次两份完整 B5-r1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL**、V0.02 B2-r1 **PASS**，亦不以本票代替 B5。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M018-b5-r1-grok-raw.md`（本席 B5-r1 票 FAIL：GR-M018-B5-01） | V0.01 | 38891 | `cd5789ec692859bdbe39d419157e27f9e32cbf399a1145afd7ed3e659e0a57c6` |
| `docs/audit-2026-09/modules/M018-b5-r1-self-raw.md`（席甲 V0.01 PASS + V0.02 撤回段 FAIL） | V0.01+V0.02 | 15626 | `a37b3c3972f6fcda0de61389a2542c416b796d6441c217d5155d3dd827043d8d` |
| `docs/audit-2026-09/findings.md` 顶部「当前 M018 B2-r2 整批候选」 | V2.56 | 1008487 | `2fe3ed62c97a2877fbcc36d089bf03bec4399e3846b8403ab8586c6c4d20f08f` |
| 本文件前缀（本席 B1+B2-r1，本段追加前） | V0.02 | 60271 | `4f051bbd76abd31f8aa6ba5abe9ab9bc744fc7c28d1fcb2a9c6ee9f081b12025` |

三输入字节/SHA 与派发清单全等。本文件独占锁哈希字段行仍仅文首一处（B1 文末提及该字段名的说明句不算新独占行），本段未新增。未读席甲 `M018-b1-self-raw.md` V0.03。

controller / runbook / modules.md 只读核验（本席未改）：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |

三 HTML 只读核验（本席未改；与 B5-r1 同）：

| 文件 | 现行 | SHA256 |
|---|---|---|
| 业务地图 | V0.14 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| 治理地图 | V0.12 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| 枚举速查 | V0.07 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

本模块 7 文件指纹（与 B5-r1 覆盖表 match；B3-r1 after）：

| 路径 | 行 | bytes | SHA256 |
|---|---:|---|---|
| `FloorPlanImportPage.tsx` | 432 | 27122 | `47e1a50d97ca7b59daf107046020d635cfc3934eeff23326bc0dd6406e378cb6` |
| `FloorPlanImportPage.guidedcreate.test.tsx` | 148 | 6654 | `5790a81e551db768b9ee12b23418bd949cddc5e83bd326ce40cd8f9d33a0fa97` |
| `FloorPlanImportPage.test.tsx` | 272 | 13644 | `a08a7ec69263e2ef3b08d367ec543bc28105553cb69f1dce54a7167d20da705d` |
| `SpaceTypesPage.tsx` | 280 | 13541 | `4d450b0df0de1acdba22e9dec777f8fa11159defd395a3601b19171efd88deed` |
| `SpaceTypesPage.guidedcreate.test.tsx` | 166 | 9633 | `db80bdf26bdd9ce931e72591d666faf168ca06ad2aa993f91d792feefdfa9e4b` |
| `VenueBusinessHoursPage.tsx` | 248 | 10962 | `841c1e543bd573d6fbfa7e2773f7aa391e58cee430695199e5bc105be32973a9` |
| `VenueBusinessHoursPage.test.tsx` | 119 | 5539 | `60f3642dd6161bbabffd75483af9ba9a05d84176ebf7e7461e8c8855c6e6118c` |

候选形态：V2.56 顶部为 r2 **新 ID 段**（非整表重写 001–009）。表内仅 M018-010 一行；其下「修复队列是（1）／否（不变）001–009」。本席按此读取，不把历史 B2-r1 表残留单元格当作本批有效处置。`questions/M018*` 不存在。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| graft | 已读。本批目标符号已能点名：`graft grep loadSecondaryIfPermitted` / `loadSecondary`，再按 span 读源；未派 MCP graft |
| receiving-code-review | 先核源/HTML/两份 B5-r1 raw 再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：差集有无第三条、应用 `loadSecondary` 还是 `loadSecondaryIfPermitted`、应否改路由/后端、001–009 有无回潮 |
| verification-before-completion | 声称前先跑只读哈希与源码点读；本票不声称 vitest 本轮新绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 页为浅适配器：副资源走既有 hook，不改后端权限面 |
| ponytail-review | 只评最小删除/收窄，不落地。不改路由/后端比前端降级更大 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |

### 一、差集核实

本席 B5-r1 V0.01：**FAIL**，临时缺陷仅 **GR-M018-B5-01** 一条，无已定稿可沿用且仍在队列的 ID。席甲 V0.01 票原 PASS、Findings「无新增」；V0.02 撤回段核源后改 FAIL，只确认本席这一条，未另立第三条。

| 依据 | 所在 raw | 候选落点 | 同根？ | 本席票 |
|---|---|---|---|---|
| GR-M018-B5-01 仅 `floor_plan.view` 第一次进页被 `GET /platform/projects` 403 经 `Promise.all` 整页拖死 | 乙 V0.01 §7 | M018-010 | 是。甲 V0.02：「`PlatformController.java:144-150` `GET /projects` 要 `governance.project.view` + `requirePlatformScope()`；`FloorPlanImportPage.tsx:113-121` 把它与 `/floor-plans` 放在同一 `Promise.all` 里作主资源；路由只要求 `floor_plan.view`——成立。」候选位置/因果与两席原文同根，未改写为「缺项目下拉」或「按钮码错误」 | **并入，无改写** |
| 乙 OBS-01…15 | 乙 V0.01 §8 | 未立项 | 观察原判定不入缺陷。含反向按钮矩阵未测、夹具未覆盖 `inactive/failed/cancelled`、驳回无点击例、Parser 英文夹杂（跨模块）、`approveDeleteSpaceType` 无 `requirePermission`（M009）、`zhOrDash`/`fpLabel` 重复等。候选未升格 | **同意不立项** |
| 席甲 V0.02 撤回段 | 甲 | — | 只确认 GR-M018-B5-01，无独立临时编号、无新 OBS 升队 | **无第三条可并** |
| 席甲 V0.01 OBS（M015-010/011 XM、PR #439 未合并 origin/main、`sortOrder` 填 0 静默成 100、`approveDeleteSpaceType` 控制器无 `requirePermission`） | 甲 V0.01 §6 | 未入本批表 | 不在撤回段，不是 B5 缺陷票。XM 维持不升格；后两项跨模块/观察。不并入 010、不新开 ID | **同意不并** |

漏并：无。改写：无。第三条：无。新 ID 仅 010，符合「仅给无 ID 条目分配新 ID」。001–009 未出现在本批候选表行（顶部无 `| M018-001 |`…`| M018-009 |`），队列「否（不变）」与 B4-r1 收口一致。B1 两席均漏本条——属实（B1 未立此缺陷；B2-r1 定稿 001–009 不覆盖本根）。无本批添加项、无 HTML 申请、无 questions 文件。

票：**PASS（差集）**。

### 二、M018-010 分类与最小处置（独立核源）

**事实（类型 4，工作区源；与候选一致）：**

- `FloorPlanImportPage.tsx:113-121`：`useAdminResource` 的 fetcher 用 `Promise.all` 同时 `GET /api/v1/floor-plans` 与 `GET /api/v1/platform/projects`，再 `setFloorPlans` / `setProjects`。任一支拒绝 → hook `:52-55` catch → `fallbackMessage: "平面图读取失败"` → `AdminDataState.tsx:8-23` 有 error 即不渲染 children（平面图表在 `:262-273`，属于 children）。
- `FloorPlanController.java:31-34`：`GET ""` → `requirePermission("floor_plan.view")`。
- `PlatformController.java:144-150`：`GET /projects` → `requirePermission("governance.project.view")`（`:148`）且 `requirePlatformScope()`（`:149`）。候选写 `:148-149` 即这两闸，与本席 B5 `:144-150`（含方法签名）同一处。
- `adminRoutes.ts:95`：`requiredPermissions: ["floor_plan.view"]`。无 `accountForms` 限制。
- 项目列表只服务新建弹窗下拉 `:373-376`；`planFormError:151` 已有「请选择所属项目」。平面图表本身不读 `projects`。
- 同模块 `VenueBusinessHoursPage.tsx:54-61` 对场地清单走 `loadSecondary`；`:171` 在场地 `<select>` 旁 `{spacesNotice ? <span data-testid="venue-business-hours-spaces-degraded">`。该页路由 `:51` 与 `GET /spaces:164`、`GET /venue-business-hours:234` **同码** `governance.space.view`，故 `loadSecondaryIfPermitted` 对能进页的账号恒真，用 `loadSecondary` 只覆盖网络/403。
- `useAdminResource.ts:79-86` `loadSecondary`：失败不抛，403 文案「当前账号无权查看该数据」，其它「该数据加载失败…」。`:95-102` `loadSecondaryIfPermitted`：`permitted===false` **不调用 load**、`notice:null`+fallback；有权限才进 `loadSecondary`。hook 注释写明这是「无权限不请求」的根因修。仓内 Access/Vendor/Ticket 等多页已用。`hasPermissions`（`PermissionGate.tsx:5-18`）DENY 优先、wildcard 放行。
- 测试 `FloorPlanImportPage.test.tsx:61` fetch 桩对 `/platform/projects` 一律 200；`VIEW_ONLY:74` 只授 `floor_plan.view`；`:260-263` 声称仍能看见「一层平面图」。32 例不能在真实 403 上失败。

**正常触发：** 分岗只授 `floor_plan.view`（菜单按路由放行）打开「商场平面图导入」→ 平面图 200、项目 403 → `Promise.all` 拒绝 → 整页「平面图读取失败」，表不渲染。商户账号同理（无 `governance.project.view`，且 `requirePlatformScope` 也会 403）。不是要两件无关故障同时发生。派发点名的四类权限账号之一的第一次点击。

**四分类（2.1 顺序：冲突 → 细化 → 技术性 → 添加）：**

- 冲突？**否。** 治理地图 L472 未写「进页必须带项目列表权限」，也未写「无 `governance.project.view` 则整页失败」。现网不是「多给了一条与 HTML 相反的规则」，是把副资源绑进主资源 Promise。
- 细化？**是。** 上位可指：业务地图 L588「其他岗位（营销员、服务员、收银、外包、审计等）按岗位分配各自能看能做的范围」（本席点读 L588 原文命中）；治理地图 L472「商场平面图网页导入：在后台上传或导入…由人工复核候选与错误后发布」（view 进页看表是该句后台网页的第一次正常读路径）。下位把「能看平面图」误绑到「还能看平台项目列表」。冻结判据 GEN-002 / GEN-005 / GEN-009 / GEN-003 / BSM-003 与本席 B5 冻结集相同，本段不加判据（不把 PGM-001 写入冻结集；L472 只作上位指针）。
- 不到技术性（能指 HTML 上位，2.1 禁止进技术性）。
- 不到添加（不发明权限码/状态/流程；用既有 hook 与既有 `governance.project.view`）。

工程缺陷：**是**。不重开 003（003 已否=导入/发布按钮码与下钻门；本条是进页副资源）。队列：**是**（本模块页 + 其测试可修）。

**最小处置核实：** 项目列表改 `loadSecondaryIfPermitted(hasPermissions(effectivePermissions, ["governance.project.view"]), () => api.get(.../platform/projects), [])`——无权限不发请求、失败只降级；`projects=[]` 时平面图表仍渲染；新建弹窗项目下拉旁显示降级提示（`floor-plan-projects-degraded`）；已有「请选择所属项目」拦截保留；+2 用例（仅 `floor_plan.view`：不请求 projects 且表渲染；projects 403：表仍渲染、提示出现、不报「读取失败」）。不改路由/后端/权限码。

- **最小？是。** 只动本模块 `FloorPlanImportPage.tsx` + `FloorPlanImportPage.test.tsx`。主资源仍走 `useAdminResource`；副资源不 reject。不改 `useAdminResource.ts` / `PermissionGate.tsx`（只 import 已有符号）。不改另外两页。
- **与同模块 `VenueBusinessHoursPage` 的 `loadSecondary` 范式同构？同族，不是同一函数。** 同构点：副资源失败不得拖死主表；降级提示绑 `notice`、放在对应 `<select>` 旁（营业时间 `:171` `venue-business-hours-spaces-degraded`）。不同点：营业时间副资源与进页同码，`IfPermitted` 无意义；本页 `floor_plan.view` ≠ `governance.project.view`，正是 hook 文档里「缺码根本不发请求」的分支。候选从本席 B5「`loadSecondary` / `loadSecondaryIfPermitted`」二选一收窄为后者，**未改写缺陷原意**。
- **应用 `loadSecondary` 而非 `loadSecondaryIfPermitted`？不同意。** `loadSecondary` 也能让 `Promise.all` 不因 403 拒绝（整页不再死），但仅 view 账号每次进页仍打 `/platform/projects` 并在主流程弹出「无权查看」——缺席是预期，不是故障。`IfPermitted(false)` 的 `notice:null` 避免惊扰看表。+2 用例的第一支（不请求）只在 `IfPermitted` 下有意义。
- **应改路由？不同意。** 路由加上 `governance.project.view` 会让仅 view 账号连页都进不去，比现状更差，且与 L472 后台可读路径相反。
- **应改后端？不同意。** 放宽 `GET /projects` 到 `floor_plan.view` 会把平台项目列表泄漏给平面图只读岗，越出本模块 7 文件，属权限面发明。`requirePlatformScope` 也不在本页改。
- **有无业务发明？无。** 不新权限码、不新状态、不改「请选择所属项目」拦截、不发明「无项目也可创建」。空下拉仍拦创建。
- **+2 用例是否最小？是。** 对应 `IfPermitted` 两支：无码不请求；有码但 403 只降级。本席 B5 原写 +1（403）是按 `loadSecondary` 单支；收窄到 `IfPermitted` 后第二支（不请求）必须有红网（GEN-003）。只改 `FloorPlanImportPage.test.tsx`（可给 `makeApi` 加 `projectsStatus`）；不测 Java。现有 VIEW_ONLY「能看见一层平面图」在落地后仍应绿，不要改坏。

**B3 非阻断边界（不构成否决）：**

- 提示必须绑 `notice`（失败降级），**不要**绑 `projects.length===0`（真·空列表 200 不是降级）。+2 用例已区分：仅 view 不要求提示出现；403 才要求 `floor-plan-projects-degraded`。
- 项目 `<select>` 在 `GuidedFlowModal` 内（默认关闭）。403 用例若只断言表渲染，提示需点开「新增平面图」才能见到 testid——与营业时间页「选择旁」同构。不要为了用例把提示发明到主表区，除非复用同一 `notice`。
- `api.get` 现返回信封或数组（`:119-120` 已拍平 `items`）。`load` 回调内或 `setProjects` 处保持拍平，避免 envelope 进下拉。
- `IfPermitted` 只闸权限码。持有 `governance.project.view` 但被 `requirePlatformScope` 拒的账号仍会请求并走 `loadSecondary` 403 降级，这是正确的失败支，不必在前端再发明平台范围判断。
- 仅 `floor_plan.manage`、无 `governance.project.view` 的账号：不请求、无 notice、新建弹窗空下拉，仍被「请选择所属项目」拦截。不在派发点名的「仅 view 进页」范围内，本批不升第三条、不改创建按钮权限。

票：**PASS（细化 / 缺陷是 / 队列是；处置最小；不同意改用 `loadSecondary`，不同意改路由/后端）**。

### 三、001–009 不动

| ID | 本批候选 | 本席 |
|---|---|---|
| 001–009 | 否（已修，B3-r1） | **同意不动、不回潮。** 本席 B5-r1 §6 已逐条复验落地。010 不重开 003（按钮码/下钻）或 004（下钻 catch）或 006（测试网）。不把 001–009 改回「是」。 |

无回潮、无把已修项改「是」、无新添加项。M015-010/011 维持 XM，不升格、不新开。

### 四、添加 / HTML / questions

本批无添加项、无 HTML 申请、无 questions。本席逐条核：010 用既有 `loadSecondaryIfPermitted` + 既有 `governance.project.view`，不是新业务规则；不改三 HTML、不 LOCK-AMEND。无应升 2.2 的项。不 BLOCKED-ASK。

### 分类 / 队列 / 允许的最小修复边界

本席 B2-r2 **PASS**。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3-r2 只改本模块 7 文件中的导入页 + 其 `.test.tsx`（必要时只动这一对），不改后端/OpenAPI/迁移/三份 HTML/锁/`adminClient.ts`/`adminRoutes.ts`/`useAdminResource.ts`/`PermissionGate.tsx`/`VenueBusinessHoursPage.*`/`SpaceTypesPage.*`。

- 010：`FloorPlanImportPage.tsx:113-121` 项目列表改 `loadSecondaryIfPermitted(hasPermissions(effectivePermissions, ["governance.project.view"]), load, [])`；平面图主资源仍直接 `api.get("/floor-plans")` 进同一 `Promise.all`。无权限不发 `/platform/projects`；失败只降级（`notice` 非空）。`setProjects` 空时表仍渲染。新建弹窗项目下拉旁 `{notice ? <span data-testid="floor-plan-projects-degraded">{notice}</span> : null}`，拦截「请选择所属项目」保留。+2 用例见上。不改路由/后端/权限码。

禁止：改 `PlatformController.listProjects` / `requirePlatformScope`；路由加 `governance.project.view`；把主资源也改成副资源；用 `loadSecondary` 替代 `IfPermitted` 作为本条落地（会丢掉「不请求」支）；把 001–009 改回「是」；给营业时间页再改草案/生效日；加二次确认契约字段。

改动文件自身版本递增。B3 累计将为 2；本票不预支 B4/B5 结论。

### 本席实跑（B2-r2，只读；无写模式）

- 锁文件 sha256sum：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 sidecar、本文件既有文首独占行同值）
- 输入三文件字节与 SHA：上表全等（与派发清单全等）
- 本文件追加前：60271 字节，SHA `4f051bbd76abd31f8aa6ba5abe9ab9bc744fc7c28d1fcb2a9c6ee9f081b12025`；独占锁哈希字段行仅文首 1 行
- 点读：`FloorPlanImportPage.tsx:113-121/151/262-273/373-376`；`AdminDataState.tsx:6-23`；`useAdminResource.ts:46-56/79-102`；`PermissionGate.tsx:5-18`；`VenueBusinessHoursPage.tsx:52-64/171`；`adminRoutes.ts:51/95`；`FloorPlanController.java:31-34`；`PlatformController.java:144-150/160-165/232-236`；`FloorPlanImportPage.test.tsx:60-74/260-263`；业务地图 L588 岗位句；治理地图 L472 导入句
- 未跑：本轮 vitest/tsc 新绿（B5 已跑 32/32，本步不重复当通过依据）、Maven、psql 写、部署、修复
- 未改 findings/ledger/锁/源/HTML/主索引
- 未读：`M018-b1-self-raw.md` V0.03

### 结论

全批差集无漏并、无原意改写、无第三条。010 细化上位指得出（L588 分岗范围 + L472 导入主路径），工程缺陷是，队列是；最小处置 `loadSecondaryIfPermitted` 成立，与同模块营业时间页同族（副资源不拖死主表），因权限码不一致必须用 `IfPermitted` 而非裸 `loadSecondary`。不同意改路由/后端。001–009 不动不回潮。添加 0。非阻断见 §二（提示绑 notice、信封拍平、403 提示在弹窗内）。逐 ID：010 PASS。本席 B2-r2 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M018 IN_PROGRESS；B3=1。
