CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M018 B4-r2 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r2
席位：固定 Grok 外审席乙（grok-4.6 / xhigh）
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁；不问人）
本文件路径：docs/audit-2026-09/modules/M018-b4-r2-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r2 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲为作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。本轮为该模块第 2 次 B3 之后的 B4。

未读取：任何 `M018-b4-r2-self-raw.md`（落盘时该路径已存在，本席按独立性禁令整文件不打开）。已读：`160-audit-prompt.md` V0.17 B4 与 §5 闸 3、findings.md V2.57 顶部「当前 M018 B2-r2 双PASS定稿」、本席旧 `M018-b4-r1-grok-raw.md` 仅作落盘格式参照、冻结 diff 与两 tree。定稿队列以 findings.md V2.57 为准：本批修复队列「是」仅 **M018-010**；001–009 保持「否」，本轮不重审其内容，只确认本 diff 未改它们且 GREEN 未回退。ledger 只读取模块状态与 B3 次数；其中 after tree 之后写入的席甲 B4-r2 封存行不构成本席证据。本票以两 tree 全文 diff、定稿最小处置、RED/GREEN 日志与本席复跑为准。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；010 是否按定稿最小处置实现；是否最懒可行（ponytail ultra）；是否引入新缺陷或新业务发明；每一变更行能否归因到 010；是否越界改后端 / OpenAPI / 三份 HTML / 锁 / sidecar / 其它模块源码；测试是否先红后绿且断言有效；是否与同模块 `VenueBusinessHoursPage` 的副资源降级范式同构。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例（无权限是否仍发 projects、403 是否仍整页「读取失败」、VIEW_ONLY 例是否只打请求计数打不中原 bug、无权限时 notice=null 是否发明提示、是否改路由/后端），未冒充调用 grilling 子代理。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 正文一行同值。ledger 顶部基准同值。criteria-lock 版本仍 V0.11。本批 B3-r2 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。sidecar blob 两树均为 `41f575026b693af74d2a36e6faf86d260ef6f6ca`。

| 项 | 值 |
|---|---|
| 模块 | M018 空间点位、楼层平面图与营业时段／网页与验证／第1段（7 文件） |
| 阶段 | B4-r2 只读审计 B3 第 2 次整批修复的「修复内容」本身 |
| 本轮修复队列 | 仅 M018-010（B2-r2 定稿「是」）；001–009 本轮不重审内容 |
| B3 | 已执行 2 次（本轮为第 2 次），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `216496ee6e7865a7935d9b23cd7874aa3ed13b3f` |
| after（对象类型 tree，可解析） | `c9301ef0a5b95d210633c087dba236dbdb7cd3a1` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 216496ee6e7865a7935d9b23cd7874aa3ed13b3f c9301ef0a5b95d210633c087dba236dbdb7cd3a1`（无路径过滤） |
| 实际差异 | 8161 bytes；SHA256 `bdb434c16cd0e449915cc90d6c08b9537e4aeaf2b5b5ba2de2ad1b7fbc7a2920`；2 files changed, 47 insertions(+), 6 deletions(-)；8 hunk；UTF-8；无 NUL |
| 与冻结文件 `/home/james/audit-tmp/m018_b3_r2.diff` | 字节、SHA256、内容全等（`live == frozen` True） |
| `git diff --check` | exit 0 |
| `git diff --name-status` | 2 处 `M`，无 A/D/R |
| 两树路径清单 | before 3808 / after 3808；`comm -3` 空 |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`（工作区两改动文件 blob 与 after tree 全等；工作区另有其它模块脏文件，不在本 diff） |
| findings.md | 两树 blob `05ff0a8e343563a9071835ce0bff4bb7811f978c`（工作区 hash-object 同值，V2.57，内容 SHA256 `b1ddbeda2d06d42ffdbb38de3659067feb6b1c259ae33bdf85686d862bd3f044`）；本 diff 未改 |
| ledger.md | 两树 blob `6b019847df9e5bdfcfee383b1b0134a2b424dd78`（本 diff 未改；工作区 ledger 在 after tree 之后另写 B3/B4 台账，hash-object `30a3ae0d414437e82c12b4e44acf5ddafa9bbdac`，不构成本席证据） |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3`，完全相同 |
| OpenAPI | 两树相同：yaml `1f056b360dc60c37b6c026c761f0fa57b3688e63` / json `32231790707d8a93a303ca47033b7819ee0e3b71`（路径 `docs/openapi/openapi-p0.yaml` / `.json`） |
| 后端 / 迁移 / 锁 / 路由 | `git diff --name-only` 对 `backend`、`docs/openapi`、三 HTML、`criteria-lock.md`、`adminRoutes.ts` 皆空 |
| 既有契约 blob 两树相同 | `useAdminResource.ts` `d4817eb295fc38ed0c6cd9e3a01cdd5021d17a3b`；`PermissionGate.tsx` `7f39e56f21fbfa6e34fff19bc609242517042f46`；`Can.tsx` `9b290087a53308659430f28ad6ca8085fe82b171`；`adminClient.ts` `2240d010c1feb03bd54e04774fd6bf0ce96256f6`；`HighRiskConfirm.tsx` `78e208ffca254f19d315036f38d12992577544e2`；`adminRoutes.ts` `4faa9e2afda7d9195aee817cfd49425849749d7b`；`FloorPlanController.java` `723f582bf253a1e34a172a568bd26bce5804df1f`；`PlatformController.java` `52d3f0d27191d6fd4cc923a1607a9263bc3eb7fa` |

两改动文件 blob（工作区 `git hash-object` = after）与内容 SHA256（本席 `sha256sum`）：

| 文件 | before blob | after / work blob | 行 | 内容 SHA256 |
|---|---|---|---:|---|
| `FloorPlanImportPage.tsx` | `4e6a47446676673c5b3f09044ef7ad332148b9e6` | `204dafbd24d72637df5447ae97190cec78daec5f` | 443 | `75f227a783ea7992b77a67aa283eb716a5d8984a8f6479707f671cffa4e10acd` |
| `FloorPlanImportPage.test.tsx` | `92776d912f3841cb9cd51b88236524b34cf5311a` | `af5fc7e8d2f30d4f7a01420757fed745cbc6b664` | 302 | `68af58616c93dfa315ce0a8badc247dc01a48752e672bf19f3ae4dbc4a9a8a8f` |

路径均在 `apps/web/src/admin/pages/`。未改的模块其余 5 文件两树 blob / 内容 SHA256 与 B4-r1 after 一致：

| 文件 | blob | 内容 SHA256 |
|---|---|---|
| `FloorPlanImportPage.guidedcreate.test.tsx` | `4132987d33f51ebf973d6920bfae87c6086e5063` | `5790a81e551db768b9ee12b23418bd949cddc5e83bd326ce40cd8f9d33a0fa97` |
| `SpaceTypesPage.tsx` | `856a0545618948ce7142cfa144d79e88398fa790` | `4d450b0df0de1acdba22e9dec777f8fa11159defd395a3601b19171efd88deed` |
| `SpaceTypesPage.guidedcreate.test.tsx` | `01ccdd77db8126f923477fb03973df9e0bf7027b` | `db80bdf26bdd9ce931e72591d666faf168ca06ad2aa993f91d792feefdfa9e4b` |
| `VenueBusinessHoursPage.tsx` | `45237715ed4e48d96d0c7d8ecaf8c62806a5395e` | `841c1e543bd573d6fbfa7e2773f7aa391e58cee430695199e5bc105be32973a9` |
| `VenueBusinessHoursPage.test.tsx` | `b0ae055e6c3a821e0d187bf63857f9b473da1846` | `60f3642dd6161bbabffd75483af9ba9a05d84176ebf7e7461e8c8855c6e6118c` |

`git diff --numstat`：33/3 `FloorPlanImportPage.test.tsx`；14/3 `FloorPlanImportPage.tsx`。

本席未改任何源文件，只新建本 raw。验证：主仓只读 `npx vitest run` 四文件 + `npx tsc -b --noEmit`。未 commit/push/PR/merge/部署；未对主索引 `git add`；未新建脚本/日志/临时产物。比对 live/frozen 时曾写入 `/tmp/m018_b4r2_live.diff`，当即删除，无残留。

---

## 2. 锁、分类与本轮队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M018 的集合。本轮只用定稿 010 已引用的 ID（GEN-002 / GEN-005 / GEN-009 / GEN-003 / BSM-003）。不 LOCK-AMEND。四分类本轮不重开。

B2-r2 定稿（findings.md V2.57）最小处置：

- **M018-010** 细化／工程缺陷是／队列是。根因：`FloorPlanImportPage` 首屏 `useAdminResource` 用 `Promise.all` 同时取 `/api/v1/floor-plans`（`FloorPlanController:33` 要 `floor_plan.view`）与 `/api/v1/platform/projects`（`PlatformController:148-149` 要 `governance.project.view` + `requirePlatformScope`）；路由 `adminRoutes.ts:95` 只要求 `floor_plan.view`。只授 `floor_plan.view` 的分岗账号（或商户账号）第一次打开本页：平面图 200、项目 403 → `Promise.all` 整体拒绝 → 报「平面图读取失败」，平面图表不渲染。项目列表只服务于新建弹窗下拉。同模块 `VenueBusinessHoursPage.tsx:54-61` 对场地清单已用 `loadSecondary` 降级。
- 最小处置：项目列表改 `loadSecondaryIfPermitted(hasPermissions(effectivePermissions, ["governance.project.view"]), () => api.get(...), [])`——无权限不发请求、失败只降级；`projects=[]` 时平面图表仍渲染，新建弹窗项目下拉旁显示降级提示（`floor-plan-projects-degraded`），已有「请选择所属项目」拦截保留；+2 用例（仅 `floor_plan.view`：不请求 projects 且表渲染；projects 403：表仍渲染、提示出现、不报「读取失败」）。不改路由/后端/权限码。

禁止：改 FloorPlanService/Parser/Controller/OpenAPI/三 HTML/锁、改路由 requiredPermissions、新增权限码、把项目清单升成主资源、发明新状态/端点。

---

## 3. 技能预检与本步收窄口径

本席环境实核（列出目录 / 读 SKILL.md；缺则记缺口，不冒充已调用）：

| 技能 | 存在 | 本步用法 |
|---|---|---|
| using-superpowers | 是 | 开工技能路由；用户 160 提示词优先于技能本体 |
| using-arc | 是 | 用户指令优先；本步是 160 B4 只读，不走 Arc audit 子代理（本体要派评审并写 `docs/arc/audits/`，记缺口） |
| graft | 是 | `graft grep loadSecondaryIfPermitted --in apps/web/`：helper 落点与既有页调用，确认本 diff 只消费既有 helper、未改 hook |
| long-running-background-tasks | 是 | 已读；vitest/tsc 在本席命令内跑完，未另挂 monitor |
| grilling / grill-me | 是 | 只借追问结构自答；本体「等用户」「dispatch sub-agent」作废 |
| batch-grill-me | **缺口** | 未冒充 |
| ubiquitous-language | **缺口** | 未冒充 |
| ponytail（ultra）+ ponytail-review | 是 | 核最小处置；结论 Lean already |
| systematic-debugging | 是 | 只借读错误→复现→追数据流；不插桩、不改生产。RED DOM 已显示 `/api/v1/platform/projects 403` 拖死整页 |
| test-driven-development / tdd | 是 | 核 RED 失败原因与 GREEN 断言；本体「测缝须经用户确认」按铁律 5 作废 |
| requesting-code-review | 是 | B4 请审=本席；不派子代理、不用 HEAD~1 |
| receiving-code-review | 是 | 外审否决席，不 IMPLEMENT |
| verification-before-completion | 是 | PASS 前本席复跑 vitest 4 文件 34 例 + tsc exit 0 |
| diagnosing-bugs | 是 | RED 日志作 tight loop；不进入修复 |
| codebase-design | 是 | 页为浅适配器；本 diff 消费既有 `loadSecondaryIfPermitted` 缝，未抬新缝 |
| writing-for-agents | 是 | 本 raw 给席甲合取 / 下游 B5 读 |
| git-guardrails-claude-code | 是（可选） | 未装 hook；等价：不 fetch/commit/push/merge/hard reset |
| i-have-adhd | 是 | CLI 按 V0.17 适配器收窄（不回正文或机器标记） |
| resolving-merge-conflicts | 是 | 无冲突 |
| audit-context-building / differential-review / Arc audit | 是 | **不用**（第三席 + §7 外产物） |
| 划掉的 skills | — | 按提示词 §6 不用 |

对抗式自问（grilling 结构，本席自答）：

1. 每个 hunk 能否归因到 010？能。8 hunk 全部是 010 的页面副资源降级或其验证性测试（闸 3：验证性测试归因到对应 ID 合法）。无「顺手优化 / 换框架 / 补抽象」。
2. 有无超出最小处置？无。未改 helper 本体、未改路由/后端/OpenAPI/HTML/锁、未新增 CSS/组件。复用已有 `loadSecondaryIfPermitted` / `hasPermissions` / `admin-muted-action`。注释与 `projectsNotice` 状态是定稿指定提示的必要载体。
3. 是否与 `VenueBusinessHoursPage` 同构？是。同构点：`Promise.all(主资源, 副资源包装)` → `setX(data)` + `setXNotice(notice)` → 下拉旁 `{notice ? <span className="admin-muted-action" data-testid="…-degraded">`。差异点合法：营业时间页用 `loadSecondary`（场地清单失败才降级）；本页定稿点名 `loadSecondaryIfPermitted` + `governance.project.view`，因为路由权限码 ≠ 副资源权限码，无权限必须不发请求。
4. VIEW_ONLY 新例 RED 只断言 `projectsRequests===0`（收到 1），表在旧实现下因 projects 默认 200 仍能渲染——是否测不准原 bug？该例打的是定稿第一句「无权限不发请求」。原 bug（403 整页读取失败）由第二例 RED 打中：`Unable to find … floor-plans-table`，DOM `admin-error` 文案「读取失败」，技术详情 `/api/v1/platform/projects 403`。两例合取覆盖定稿 +2 用例，不是 tautology。
5. `loadSecondaryIfPermitted(!permitted)` 返回 `notice: null`，VIEW_ONLY 打开新建弹窗不会出现降级提示——是否漏实现「projects=[] 时显示提示」？否。helper 契约写明「缺权限不发请求、无提示」（缺席是预期）。VIEW_ONLY 无 `floor_plan.manage`，`PermissionButton` 的新建按钮本就禁用。提示用例绑定 403 失败路径（有权限才请求、失败才 notice），与定稿 +2 用例一致。未发明「空列表一律弹无权」。
6. 403 例用 `ADMIN_PERMS` wildcard 而非商户账号——是否测错主体？否。wildcard 使 `hasPermissions(… project.view)` 为真，才会发请求；桩返回 403 模拟 `requirePlatformScope` / 后端拒绝。这正是「失败只降级」路径。无权限路径由 VIEW_ONLY 覆盖。
7. 「请选择所属项目」拦截是否还在？在。`planFormError` 首句仍是 `if (!planForm.projectId.trim()) return "请选择所属项目"`；guidedcreate 001 用例仍绿（本 diff 未改该测试文件）。
8. 001–009 有无回归？无。其余 5 个模块文件两树 blob 全等；RED 阶段旧 32 例全过；本席 GREEN 复跑 34 例全过。
9. 有无归因不到 010 的行？无。

ponytail-review：`Lean already. Ship.` 净可删行 0。未手写 try/catch 替代既有 helper，未加新依赖。

---

## 4. 完整未过滤 binary diff

命令：`git diff --binary 216496ee6e7865a7935d9b23cd7874aa3ed13b3f c9301ef0a5b95d210633c087dba236dbdb7cd3a1`

字节 8161；SHA256 `bdb434c16cd0e449915cc90d6c08b9537e4aeaf2b5b5ba2de2ad1b7fbc7a2920`；与 `/home/james/audit-tmp/m018_b3_r2.diff` 全等。UTF-8 完整原文见文末 ````diff` 块（本席从冻结文件原样嵌入，未手抄、未过滤）。

---

## 5. 逐块归因表（8 hunk；闸 3）

归因判据：每一变更行须能指到 findings.md V2.57 已定稿且归属「是」的 ID。本批「是」只有 **M018-010**。验证性测试归因到 010 合法。同根（同一副资源入口的必要 import / state / 提示 DOM）合法。

| # | 文件 | hunk | 归属 | 理由 |
|---|---|---|---|---|
| 01 | FloorPlanImportPage.test.tsx | `@@ -41,7 +41,9 @@` `projectsRequests` + `makeApi` 增 `projectsStatus` | 010 | 为「不请求 / 403」两例提供计数与状态缝 |
| 02 | 同上 | `@@ -58,7 +60,11 @@` projects 桩计数 + 403 envelope | 010 | 旧桩对 projects 一律 200 掩盖原 bug；改为可 403 |
| 03 | 同上 | `@@ -73,7 +79,7 @@` `renderPage` 透传 `projectsStatus` | 010 | 测试入口接到 010 桩 |
| 04 | 同上 | `@@ -270,3 +276,27 @@` 新 describe +2 例 | 010 | 定稿指定的两例：VIEW_ONLY 不请求且表渲染；403 表渲染 + 提示 + 不报读取失败 |
| 05 | FloorPlanImportPage.tsx | `@@ -3,8 +3,9 @@` 改 import | 010 | 消费既有 `loadSecondaryIfPermitted` / `hasPermissions`，未新造 helper |
| 06 | 同上 | `@@ -95,6 +96,7 @@` `projectsNotice` state | 010 | 降级提示载体 |
| 07 | 同上 | `@@ -110,14 +112,22 @@` `Promise.all` 内改 `loadSecondaryIfPermitted(hasPermissions(…project.view), …)`，`setProjects` 走 `.data`，`setProjectsNotice` | 010 | 定稿最小处置正文 |
| 08 | 同上 | `@@ -374,6 +384,7 @@` 下拉旁 `data-testid="floor-plan-projects-degraded"` | 010 | 定稿指定 testid 与提示位置 |

无剩余行。无 A/D/R。无第三文件。

---

## 6. 内容审计（是否按定稿最小处置）

修前（before tree `FloorPlanImportPage.tsx:113-121`）确为：

```ts
const [list, projectList] = await Promise.all([
  api.get(... "/api/v1/floor-plans"),
  api.get(... "/api/v1/platform/projects"),
]);
setFloorPlans(...);
setProjects(Array.isArray(projectList) ? projectList : (projectList?.items ?? []));
```

`AdminApi.request` 对非 2xx `throw apiErrorFromResponse`；`useAdminResource` catch 后 `onError("…")` + fallback「平面图读取失败」。RED 第二例 DOM 已复现：`admin-error`「读取失败」+ 技术详情 `/api/v1/platform/projects 403`。

修后精确实现定稿：

- `hasPermissions(effectivePermissions, ["governance.project.view"])`：`PermissionGate.tsx:5-17` wildcard/grants/denies 既有语义，本 diff 未改该文件。VIEW_ONLY 仅 `floor_plan.view` → false → helper 直接 `{ data: fallback, notice: null }`，不调用 load，故 `projectsRequests===0`。
- `loadSecondaryIfPermitted`（`useAdminResource.ts:95-102`，本 diff 未改）：`!permitted` 不发请求；`permitted` 走 `loadSecondary`，403 → notice「当前账号无权查看该数据」，其它失败 →「该数据加载失败…」。403 例 `toMatch(/无权|加载失败/)` 打中前者。
- `projects=[]` 时 `AdminDataState` 仍渲染 `floor-plans-table`（`empty={false}`，主资源成功即出表）。
- 新建弹窗下拉旁 `{projectsNotice ? <span className="admin-muted-action" data-testid="floor-plan-projects-degraded">`：与营业时间页 `venue-business-hours-spaces-degraded`（`VenueBusinessHoursPage.tsx:171`）同构，testid 按定稿命名。
- `planFormError` 首句「请选择所属项目」未动。
- 未改 `adminRoutes.ts:95` `requiredPermissions: ["floor_plan.view"]`；未改 `FloorPlanController` / `PlatformController`；未改权限种子。

与营业时间页的差异不是越界：定稿写明要用 `loadSecondaryIfPermitted` 而不是裸 `loadSecondary`，因为本页路由权限码不能覆盖 projects 端点。

未引入新业务发明（无 draft、无新权限码、无新端点、无改二次确认契约）。001–009 页面注释与行为块（必填/闭集/import.manage/下钻显错/幂等键/来源名称/未勾选拦截/unarchive）仍在，本 diff 未触。

---

## 7. 测试有效性（先红后绿且断言能失败）

冻结日志（本席只读，未改）：

| 日志 | 字节 | 要点 |
|---|---:|---|
| `/home/james/audit-tmp/m018_b3r2_run.log` | 3114 | `RED_VITEST_EXIT=1` 在 `APPLIED impl` 之前；`GREEN_VITEST_EXIT=0` 4 文件 34 例；`GREEN_TSC_EXIT=0`；after tree / diff sha 与冻结一致 |
| `/home/james/audit-tmp/m018_b3r2_red.log` | 11594 | 4 文件：32 passed / **2 failed**（均为新 010 例） |
| `/home/james/audit-tmp/m018_b3r2_green.log` | 1561 | 4 passed / 34 passed |
| `/home/james/audit-tmp/m018_b3r2_tsc.log` | 0 | 空文件 + run.log `GREEN_TSC_EXIT=0`（tsc 无诊断输出属正常） |

RED 失败原因（能失败、打中目标行为，不是 typo）：

1. `仅 floor_plan.view：不请求项目清单…` → `AssertionError: expected 1 to be +0` at `expect(projectsRequests).toBe(0)`。旧实现无条件请求 projects。表已渲染（默认桩 200），所以该例专门锁「无权限不发请求」。
2. `项目清单 403：…` → `TestingLibraryElementError: Unable to find [data-testid="floor-plans-table"]`（waitFor 超时）。DOM：`data-testid="admin-error"`「读取失败」/「当前账号无权进行此操作」，`<pre>/api/v1/platform/projects 403` + `PERMISSION_DENIED`。这是原 bug 的用户可见症状。

GREEN：页面 5 处替换后两例转绿；旧 32 例保持绿。

断言有效性：

- VIEW_ONLY：`getByTestId("floor-plans-table")` 含「一层平面图」（真实渲染，非 mock 自指）；`projectsRequests===0`（独立计数器，不读实现内部）；`setGlobalError` 不含「读取失败」。
- 403：表渲染；`projectsRequests===1`（有权限才请求一次）；`setGlobalError` 不含「读取失败」；打开新建后 `floor-plan-projects-degraded` 文本匹配无权/加载失败（来自 helper 真实 notice，不是测试里写死的字符串等于生产常量副本）。

本席复跑（fresh，`apps/web`）：

```
npx vitest run src/admin/pages/FloorPlanImportPage.test.tsx \
  src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx \
  src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx \
  src/admin/pages/VenueBusinessHoursPage.test.tsx
```

`Test Files  4 passed (4)` / `Tests  34 passed (34)` / Duration 2.08s。

`npx tsc -b --noEmit` → `TSC_EXIT:0`。

001–009 回归：guidedcreate 4 例（001/005 必填与 active/幂等键）、SpaceTypes 10 例（含 009 恢复）、VenueHours 5 例（停用/降级）、本页旧 13 例（002 中文闭集 / 003 权限按钮 / 004 下钻 403 / 007 来源名称 / 008 未勾选）均在 34 例内且通过。

---

## 8. 越界与三 HTML / 后端 / OpenAPI / 锁

- 本 diff 文件闭集 = 2 个 M018 页面/测试文件。无 backend、无 OpenAPI、无三 HTML、无 lock/sidecar、无 adminRoutes、无 helper 本体、无 Can/HighRiskConfirm/adminClient。
- 工作区另有其它模块脏文件（M009 等），**不在两 tree 之间**，本席不审、不修。
- 铁律 4：三 HTML 两树 blob 全等。

---

## 9. 票

| 轴 | 票 | 依据 |
|---|---|---|
| 内容 | **PASS** | 010 按定稿最小处置落地；与营业时间页副资源范式同构（IfPermitted 为定稿指定差异）；001–009 无回归；无业务发明；本席复跑 34 例 + tsc 0 |
| 归因（闸 3） | **PASS** | 8 hunk / 47+/6- 全部可归因到唯一「是」ID M018-010；无越界行 |
| 总结论 | **PASS** | 两轴均 PASS；任一 FAIL 即 FAIL，本轮未触发 |

本票不是模块收口，不是 B5。B5 须整模块三线重审。

---

## 10. 完整 diff 原文（冻结文件原样）

```diff
diff --git a/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx b/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
index 92776d91..af5fc7e8 100644
--- a/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
+++ b/apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
@@ -41,7 +41,9 @@ function errorEnvelope(status: number, code: string, message: string): Response
 }
 
 let lastPost: { url: string; body: Record<string, unknown> } | null = null;
-function makeApi(options: { importsStatus?: number } = {}): AdminApi {
+let projectsRequests = 0;
+function makeApi(options: { importsStatus?: number; projectsStatus?: number } = {}): AdminApi {
+  projectsRequests = 0;
   vi.stubGlobal(
     "fetch",
     vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
@@ -58,7 +60,11 @@ function makeApi(options: { importsStatus?: number } = {}): AdminApi {
         return Promise.resolve(listEnvelope(IMPORTS));
       }
       if (url.endsWith("/floor-plans")) return Promise.resolve(listEnvelope(PLANS));
-      if (url.includes("/platform/projects")) return Promise.resolve(dataEnvelope([{ id: "prj-1", name: "主项目" }]));
+      if (url.includes("/platform/projects")) {
+        projectsRequests += 1;
+        if (options.projectsStatus === 403) return Promise.resolve(errorEnvelope(403, "PERMISSION_DENIED", "当前账号无权查看项目"));
+        return Promise.resolve(dataEnvelope([{ id: "prj-1", name: "主项目" }]));
+      }
       return Promise.resolve(listEnvelope([]));
     }),
   );
@@ -73,7 +79,7 @@ const NO_IMPORT_MANAGE: EffectivePermissionsState = {
 };
 const VIEW_ONLY: EffectivePermissionsState = { status: "ready", value: { grants: ["floor_plan.view"], denies: [], wildcard: false } };
 
-function renderPage(perms: EffectivePermissionsState = ADMIN_PERMS, apiOptions: { importsStatus?: number } = {}) {
+function renderPage(perms: EffectivePermissionsState = ADMIN_PERMS, apiOptions: { importsStatus?: number; projectsStatus?: number } = {}) {
   const setGlobalError = vi.fn();
   const setActionResult = vi.fn();
   const fullProps: AdminPageProps = {
@@ -270,3 +276,27 @@ describe("FloorPlanImportPage 发布、权限与下钻错误（M018-003/004/005/
     await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("无权")));
   });
 });
+
+// M018-010：项目清单是副资源——只授 floor_plan.view 的账号不请求它、平面图表照常；projects 403 只降级提示，不整页「平面图读取失败」。
+describe("FloorPlanImportPage 项目清单副资源降级（M018-010）", () => {
+  afterEach(() => {
+    vi.unstubAllGlobals();
+    lastPost = null;
+  });
+
+  it("仅 floor_plan.view：不请求项目清单，平面图表照常渲染", async () => {
+    const { setGlobalError } = renderPage(VIEW_ONLY);
+    await waitFor(() => expect(screen.getByTestId("floor-plans-table").textContent).toContain("一层平面图"));
+    expect(projectsRequests).toBe(0);
+    expect(setGlobalError).not.toHaveBeenCalledWith(expect.stringContaining("读取失败"));
+  });
+
+  it("项目清单 403：平面图表仍渲染，新建弹窗显示降级提示，不报整页读取失败", async () => {
+    const { setGlobalError } = renderPage(ADMIN_PERMS, { projectsStatus: 403 });
+    await waitFor(() => expect(screen.getByTestId("floor-plans-table").textContent).toContain("一层平面图"));
+    expect(projectsRequests).toBe(1);
+    expect(setGlobalError).not.toHaveBeenCalledWith(expect.stringContaining("读取失败"));
+    fireEvent.click(screen.getByTestId("floor-plan-create-open"));
+    expect(screen.getByTestId("floor-plan-projects-degraded").textContent).toMatch(/无权|加载失败/);
+  });
+});
diff --git a/apps/web/src/admin/pages/FloorPlanImportPage.tsx b/apps/web/src/admin/pages/FloorPlanImportPage.tsx
index 4e6a4744..204dafbd 100644
--- a/apps/web/src/admin/pages/FloorPlanImportPage.tsx
+++ b/apps/web/src/admin/pages/FloorPlanImportPage.tsx
@@ -3,8 +3,9 @@ import { AdminDataState } from "../components/AdminDataState";
 import { AdminPageHeader } from "../components/AdminPageHeader";
 import { AuditTrailHint } from "../components/AuditTrailHint";
 import { TechDetails } from "../components/TechDetails";
-import { useAdminResource } from "../hooks/useAdminResource";
+import { loadSecondaryIfPermitted, useAdminResource } from "../hooks/useAdminResource";
 import { Can, PermissionButton } from "../security/Can";
+import { hasPermissions } from "../security/PermissionGate";
 import { IconButton } from "../components/IconButton";
 import { UnorderedListOutlined, ProfileOutlined } from "@ant-design/icons";
 import { HighRiskConfirm } from "../components/HighRiskConfirm";
@@ -95,6 +96,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
   const [floorPlanImportDetails, setFloorPlanImportDetails] = React.useState<FloorPlanImportDetails | null>(null);
   const [planForm, setPlanForm] = React.useState(EMPTY_PLAN_FORM);
   const [projects, setProjects] = React.useState<{ id: string; name?: string }[]>([]);
+  const [projectsNotice, setProjectsNotice] = React.useState<string | null>(null);
   const planKeyRef = React.useRef("");
   const importKeyRef = React.useRef("");
   const publishKeyRef = React.useRef("");
@@ -110,14 +112,22 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
   // 行明细抽屉：候选/错误明细数据行整行可点 → 完整字段（枚举中文·序号/行号原样）。
   const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
 
+  // M018-010：项目清单只服务于新建弹窗下拉，是副资源——只授 floor_plan.view 的账号不发请求（后端 /platform/projects 要 governance.project.view + 平台 scope），
+  // 读取失败只降级提示，平面图表照常渲染（同模块营业时间页的 loadSecondary 范式）。
   const { loading, error, reload } = useAdminResource(
     async () => {
       const [list, projectList] = await Promise.all([
         api.get<{ items?: FloorPlan[] } | FloorPlan[]>("/api/v1/floor-plans"),
-        api.get<{ items?: { id: string; name?: string }[] } | { id: string; name?: string }[]>("/api/v1/platform/projects"),
+        loadSecondaryIfPermitted(
+          hasPermissions(effectivePermissions, ["governance.project.view"]),
+          () => api.get<{ items?: { id: string; name?: string }[] } | { id: string; name?: string }[]>("/api/v1/platform/projects"),
+          [] as { items?: { id: string; name?: string }[] } | { id: string; name?: string }[],
+        ),
       ]);
       setFloorPlans(Array.isArray(list) ? list : (list?.items ?? []));
-      setProjects(Array.isArray(projectList) ? projectList : (projectList?.items ?? []));
+      const projectData = projectList.data;
+      setProjects(Array.isArray(projectData) ? projectData : (projectData?.items ?? []));
+      setProjectsNotice(projectList.notice);
     },
     { onError: setGlobalError, fallbackMessage: "平面图读取失败", onStart: () => setGlobalError(null) },
   );
@@ -374,6 +384,7 @@ export function FloorPlanImportPage({ api, setActionResult, setGlobalError, effe
                   <option value="">请选择</option>
                   {projects.map((project) => <option key={project.id} value={project.id}>{project.name ?? project.id}</option>)}
                 </select></label>
+                {projectsNotice ? <span className="admin-muted-action" data-testid="floor-plan-projects-degraded">{projectsNotice}</span> : null}
                 <label>平面图名称<input data-testid="floor-plan-name" value={planForm.planName} onChange={(e) => setPlanForm({ ...planForm, planName: e.target.value })} /></label>
                 <label>楼栋<input data-testid="floor-plan-building" value={planForm.buildingCode} onChange={(e) => setPlanForm({ ...planForm, buildingCode: e.target.value })} /></label>
                 <label>楼层<input data-testid="floor-plan-floor" value={planForm.floorNo} onChange={(e) => setPlanForm({ ...planForm, floorNo: e.target.value })} /></label>
```
