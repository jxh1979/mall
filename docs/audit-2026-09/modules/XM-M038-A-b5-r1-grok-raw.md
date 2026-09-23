CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M038-A B5-r1 席乙（Grok）修复后整模块全量审计 raw

- 席位：Grok 独立只读审计席乙（第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B5-r1（合成模块；B3 累计 1；修复后整模块三线重审，不只看 diff；不修、不定稿、不进 B3）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：XM-M038-A；锁内映射取 M081 / M082 同一组 21 项
- 正本：`/home/james/complex-management` 分支 `main` 主工作区；两测试文件主工作区现状即发布版
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- origin/main：`bfb17f8b4f87fbacd1e0284b728901c8e3f4e053`
- merge-base HEAD origin/main：`88c7b465475fb8eb26e46200ae90ad3e00500522`
- `test -f .git/MERGE_HEAD` → **NO_MERGE**
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.14；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（未抄 sidecar / ledger / 其它报告）。sidecar 与 ledger 顶部同值，仅作交叉观察
- LOCK-AMEND-COUNT：1（ledger 顶部；本席未改锁、不新增判据）
- 映射：锁 L6466 M081 与 L6467 M082 同一组 21 项；本席解析 COUNT=21 UNIQUE=21 MISSING=[]
  `BSM-001, BSM-003, GEN-001 … GEN-018, PGM-001`
- 写入集合：仅两个测试文件。页面 / TraceLink / AdminApp / 路由属别的模块，只读对照
- findings 现行 V3.93：XM-M038-A-001…006 修复队列「否（已修，B3-r1）」；当前队列「是」=0。不把全部否当成模块已全对；允许真实新证据或回归
- 背景（只作时间线，本票独立重审）：B1 双 FAIL → B2-r1 双 PASS 定稿 001…006 → B3-r1 → B4-r1 内容/归因双 PASS（Grok raw SHA256 `38581716105406a00a6c898a7df15881d85ffd5f486727bd51781242808e6844`、席甲 SHA256 `7cd51db83a1326b8cd9b90207e3fc654b599fd9a9385f5c7c27a46bce08772bd`）
- 独立性：未打开 `modules/XM-M038-A-b5-r1-self-raw.md`。全仓检索若命中该路径片段一律丢弃不采信。旧 raw（b1 / b4-r1）可读，本票不复用旧 finding 编号当结论。未改计划 / findings / ledger / 业务代码 / 三 HTML / 锁；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席；未对数据库执行写语句；除一次性 worktree 外未建脚本、日志或临时审计产物
- 本席步骤结论：**PASS**
- 工程缺陷成立、建议本模块修复队列「是」：**无**
- 已在 B2 双席定为只登记、无新证据故维持：GR-07→M285；预部署不跑单测→M003/M005；夹具重复；测试不经 AdminApp
- 添加项 0；无 HTML 修改申请；LOCK-AMEND 0

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md。用户指令禁止子代理 / 第三席 / MCP，优先于 Arc 默认全量派席。B5 禁止修复，故 TDD 写测、ponytail 落修、receiving-code-review 的 IMPLEMENT 一律不执行。runbook「阶段 B 不创建 worktree」被本轮用户指令覆盖：前端单测只在基于 origin/main 的一次性 worktree 里进行。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读；用户指令优先于技能 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 路由到 audit；**用户禁子代理 / 禁改业务**，不跑 Arc 默认派评审、不写 `docs/arc/audits/` |
| graft | `~/.claude/skills/graft/SKILL.md` | 已读并调用：`graft ask "TraceLink click-through drilldown navigateTo focusObjectId getPaged admin pages" --source`；随后 `graft grep` 五页 `getPaged`/`TraceLink`。再全文实读两测试文件与对照页 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读。8 测前台完成；全量 vitest+tsc 超 15s 被宿主移入后台后用任务输出收回，未另挂 monitor |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`、同目录 `grill-me/`（后者仅转 `/grilling`） | 对抗线自问自答；本体「等用户 / 派子代理」作废 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | TraceLink 是深模块（无 navigateTo 或无 objectId → 纯文本；有则按钮）。本两文件是浅适配，缝在页面 props。本票查的是适配是否仍对准页面现行接口 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现；B5 禁止修复、不加诊断桩。假说用 vitest 输出与源码行验证 |
| tdd / test-driven-development | 已读 | 判断每条留下的用例能否因目标行为变坏而红；本席不写测试 |
| verification-before-completion | 已读 | PASS/FAIL 前当场：独立 sha256、cmp origin/main+diff 与主工作区两测试、一次性 worktree 8 测 + 全量 206/978 + tsc |
| receiving-code-review | 已读 | 禁止表演性同意；本轮无对方本次 raw 可收（互盲） |
| requesting-code-review | 存在 | B4 用；B5 不适用 |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → NO_MERGE |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 存在；只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B5 不修；review 口径见 §8 |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B5 不适用 |
| i-have-adhd | `.../i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 存在；正文只落 raw；CLI 不回带结论或机器标记 |
| audit-context-building | 已读 | 要求派 function-analyzer 子代理：与「只允许两席」冲突，**不执行**，记缺口 |
| batch-grill-me | 本机无独立 SKILL | **缺口**；已并入 grilling 自问自答 |
| ubiquitous-language | 本机无独立 SKILL | **缺口** |
| dispatching-parallel-agents / subagent-driven-development / research / using-git-worktrees | 存在 | 提示词划掉。一次性 `git worktree add --detach` 是本轮用户指令允许的只读核，不是 using-git-worktrees 技能本体 |

明确不用：executing-plans、finishing-a-development-branch、MCP、第三主体、Arc 默认派评审。

一次性 worktree：`git worktree add --detach /tmp/xm038a-b5-r1-grok origin/main`（HEAD `bfb17f8b`），`git apply /home/james/audit-tmp/xm038a_b3r1.diff`，软链主工作区 `node_modules` 与 `apps/web/node_modules`。`cmp` 两测试文件与主工作区发布版 **IDENTICAL**。在 `apps/web` 下：

1. `npx vitest run src/admin/pages/TraceLinkDrilldown.test.tsx src/admin/pages/TraceLinkP2c.test.tsx` → Test Files 2 passed / Tests **8 passed (8)**，Duration 1.31s，exit 0
2. `npx tsc -b --noEmit` → TSC_EXIT=0
3. `npx vitest run` → Test Files **206 passed (206)** / Tests **978 passed (978)**，Duration 18.54s，FULL_EXIT=0

收尾：先 `rm -f` 两条软链；宿主对 `git checkout` 有 deny，故用 `git apply -R /home/james/audit-tmp/xm038a_b3r1.diff` 把 worktree 恢复干净（status 空）；再 `git worktree remove /tmp/xm038a-b5-r1-grok`（无 `--force`），REMOVE_EXIT=0，`test ! -d` 成立。未装依赖。未写仓库内日志。

graft 本轮检索：ask 1 + grep 5。ask 行报 tokens saved ≈ 37,781。

---

## 1. 范围核验与锁哈希

独立命令（独占锁行已写在本文第一行）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`
- sidecar 文件自身哈希（旁路文件，不是 lock 正文）：`c4f8b2c53239206e0eb7b08c1a219049cf785bf9c233f93d66f7a8f0617c111f`；sidecar **内容行** = 上列 lock 实算值
- ledger 顶部：`CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458` / `LOCK-AMEND-COUNT: 1`
- `criteria-lock.md` 版本 V0.14；判据总数 401
- 映射行 M081 / M082 与开工 21 项闭集相同

其它只读哈希：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |
| 枚举速查 HTML | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| 业务地图 HTML | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| 治理地图 HTML | `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f` |
| `TraceLinkDrilldown.test.tsx`（发布版） | `6e56106913f9b78308b6e9a8c8bb015fc8b287fecae2157d109e10966a8041ae` |
| `TraceLinkP2c.test.tsx`（发布版） | `41b7e16467a0c97503e250df19186903420019909c404759be7de107da16f235` |
| `TraceLink.tsx` | `213c965bdab77902a5a0518520513b9a43f400727713ce8ee88071c76f24818a` |
| `useServerPagedRows.ts` | `3d67811bc23e1876541ca6f1ec6805b938ba96d863ca4b068e84c9ad6afab58a` |
| 冻结 B3-r1 diff | `601db987c66600c21b116bca65dbcd06493e7342cf1491e14949f3b0289980c8`（与 `git diff HEAD --` 两测试文件逐字节相同） |

模块文件（`plan/modules.md`）：M081 含 `TraceLinkDrilldown.test.tsx`；M082 含 `TraceLinkP2c.test.tsx`。本合成模块只这两份。全文已读，未抽样。

主工作区 `git status --porcelain` 对该两文件为 `M`（B3-r1 未提交，即发布版）。

---

## 2. 第一性原理三问

### 2.1 这段东西为什么必须存在？删掉它，三份 HTML 里哪一条会做不到？

两文件是后台「全链下钻」的跨页回归网，钉住：源页引用字段可点并调用 `navigateTo(pageId, objectId)`；无 `navigateTo` 时降纯文本；目标页按 `focusObjectId` 高亮/反查。

HTML 上位（逐字）：

- PGM-001（`platform-governance-map-for-operations-v0.1.html` 锁 L5006–5022）：「平台内展示的所有数据、单据、对象以及涉及的人员与角色，都必须可点击、可追溯……不允许出现无法点击溯源的孤立数据。」见名可点 / 上下可钻 / 正反可查。
- BSM-001（`business-system-map-for-operations-v0.1.html` 锁 L2296）：「另一条贯穿全局的原则：数据可溯源、全链可点击」。

删掉这两份测试，上述原则在本切片的可执行声称会失去跨页网。**不等于**本模块要实现全部页面下钻——页面属别的模块。本文件的用途是钉住已定稿的页面行为，使回归第一次正常跑就会红。

答得上 → 有业务上位。留下的 8 条是细化（下位测试）。已删除的 3 条原先钉的是 M023-005 已改掉的目标，继续留着会把过期声称写成当前事实（GEN-016）；删除后由各页自己的测试守住现行行为（见 §5.2）。

### 2.2 它声称的行为，有没有被实际执行验证过？证据在哪？

有。类型 2 + 类型 4：origin/main 一次性 worktree 套 B3-r1 diff 后实跑（§0）。8/8 绿、206 文件 978 测绿、`tsc -b --noEmit` 0。每条留下的用例都驱动真实页面组件（非只读快照），断言可因目标行为变坏而失败（§5.1）。

### 2.3 它依赖的前提，哪一条是没验证过就当真的？

已验证：

- `TraceLink`：无 `navigateTo` 或无 `objectId` → 片段纯文本；有则 `data-testid={`trace-link-${pageId}-${value}`}` 并 `onClick → navigateTo`（`TraceLink.tsx:14-30`）。
- 分页：`getPaged` 返回 `{ items, total, page, size }`（`adminClient.ts:60-65`）；`useServerPagedRows` 读 `result.items` / `result.total`，`pageSize` 传入 500（`useServerPagedRows.ts:34,68-71`；拓扑 L70-95、组织商户 L137-155）。
- 合同工作台商户列 `pageId="ADM-GOV-002"`（`ContractWorkbenchPage.tsx:488-490`）；focus 行 class 含 `admin-row-selected`（L481-482）。
- 我的对账合同列 `pageId="ADM-MER-006"`（`MerchantZoneSettlementsPage.tsx:191,221`）。
- 我的账单合同来源 `pageId="ADM-CONT-001"` 且仅 `sourceType === "contract"`（`MerchantZoneBillsPage.tsx:124-126,150-152`）；列表仍 `api.get`（L101）。
- 审计反查 `parseFocusObject` 后切操作日志并 `api.get` `/api/v1/audit/operation-events` 带 `business_type`/`business_id`（`AuditEventsPage.tsx:121-127,191-199,212`）。
- 治理拓扑 `data-focus-node` + `admin-row-selected`（`GovernanceTopologyPage.tsx:199-200`）。
- 组织商户 `organization-merchant-row-${id}` + `admin-row-selected`（`OrganizationMerchantPage.tsx:390`）。

未在本模块用可执行产物验证、且 **B2 已只登记、本票无新证据故不改判**：

- 单测 `navigateTo: vi.fn()` ≠ `AdminApp.navigateTo` 的权限/账号形态静默 return（`AdminApp.tsx:545-551`）。
- 我的账单页是 ADM-MER-003、`accountForms: ["merchant"]`（`adminRoutes.ts:89`），目标 ADM-CONT-001 需 `contract.draft.manage`（L19）——商户账号点了不跳。这是 GR-07→M285。
- `package.json:96` `verify:predeploy` 仍不含 `verify:unit`（`verify:unit` 在 L90，走 `verify:delivery-gates`）。

这些前提本票重新读到，与 B2 登记事实相同，无新触发面。

---

## 3. 顺从式：21 项逐项

每项：适用 / 证据 / 是否通过。无 finding 不等于未审。假定实现意图正确，再核冻结判据与 HTML。

| ID | 适用 | 证据 | 本模块 |
|---|---|---|---|
| BSM-001 | 适用。全链可点击是同节原则 | 留下的可点/定位用例与现行页面 pageId、focus 高亮一致；已删 3 条不再把 M023-005 已改目标写成当前事实。vitest 8/8 | 通过 |
| BSM-003 | 适用（通用能力标题；本文件不实现存档/分步新建） | 测试覆盖下钻引导，不覆盖存档删除或分岗。权限闸在 AdminApp，本文件用 wildcard 夹具属页面单测缝 | 通过（本文件范围内） |
| GEN-001 | 适用 | 两文件有可验证用途（§2.1）。过期三条已删，不是纯转发；夹具重复不另抽共享工具（B2 只登记） | 通过 |
| GEN-002 | 适用 | 8 条声称入口均在 worktree 实跑通过，与对照页接口一致（§5.1） | 通过 |
| GEN-003 | 适用 | 8 条均有能失败断言（§5.1）。006 否定断言改为 `container.querySelector('[data-testid^="trace-link-"]')`，无 navigateTo 仍出 TraceLink 按钮即红。已删三条由本页测试守住（§5.2） | 通过 |
| GEN-004 | 适用面窄 | 测试无外部输入信任边界；夹具对象为测试自造 | 通过（N/A 到本文件实现） |
| GEN-005 | 适用面窄 | 本文件不实现授权。`AdminApp.navigateTo` 默认拒绝在对照层已存在。测试用 wildcard / 假 navigateTo，测不到最小权限；不要求本文件重写 AdminApp（B2 只登记） | 通过（对照层；本文件不新增授权规则） |
| GEN-006 | 不适用 | 无状态转换 / 金额写入 | 通过（N/A） |
| GEN-007 | 不适用 | 无并发写路径 | 通过（N/A） |
| GEN-008 | 适用 | `makeApi.getPaged` 按同一路由表返回 `{ items, total, page, size }`，`size: 500` 与两页 `useServerPagedRows(..., 500)` 一致；`url.startsWith` 吃带 `?page=&size=` 的路径。`PagedResult`（`adminClient.ts:60-65`）同形。账单页仍走 `get`，桩 `get` 仍回数组，与页面一致 | 通过 |
| GEN-009 | 适用面窄 | 本文件未吞错、无伪成功。授权工作台缺 `merchantId` 的可见中文错误在对照页（M023），本文件已不再对该页下钻作过期断言 | 通过 |
| GEN-010 | 适用面窄 | 无秘密、无注入面 | 通过（N/A） |
| GEN-011 | 适用面窄 | 审计事件用例仍打到 `/api/v1/audit/operation-events` 且带 `business_type`/`business_id`（绿）。本文件不是审计实现 | 通过 |
| GEN-012 | 不适用 | 无无界批处理 | 通过（N/A） |
| GEN-013 | 不适用 | 无构建/依赖声明。worktree 软链已有 node_modules，未装依赖 | 通过（N/A） |
| GEN-014 | 不适用 | 无迁移 | 通过（N/A） |
| GEN-015 | 适用 | 用例名/页面文案中文。`data-testid` 与事件名属系统标识。TraceLink `title`「下钻到来源：…」中文。本文件无英文界面发明 | 通过 |
| GEN-016 | 适用 | 留下的测试名称/断言与现行页面一致（ADM-GOV-002 / ADM-MER-006 降级否定前缀 / ADM-CONT-001 仅账单合同来源 / getPaged 形状）。不再把 ADM-CONT-001 对账列、我的合同可点、授权工作台商户列可点写成当前事实 | 通过 |
| GEN-017 | 适用 | 本席按冲突→细化→技术性→添加分类，见 §6。无把业务发明写成技术性；无新添加项 | 通过（本席分类） |
| GEN-018 | 适用 | 结论落到 HTML 原文、vitest 输出、源码行，不以席位意见或「001…006 已否」为据 | 通过（本席取证） |
| PGM-001 | 适用 | 全链可点击须受众可达。留下的可点用例：合同工作台→ADM-GOV-002、账单合同来源→ADM-CONT-001（后者受众问题 B2 已归 M285，本票无新证据）。已删三条对应页现行是商户可达目标或纯文本，由本页测试守住 | 通过（本文件范围内；GR-07 维持只登记） |

21/21 均有记录。不通过项 0。无锁外判据。

---

## 4. 对抗式（grilling 自问自答）

事实自己读，不派子代理、不问人。先假定实现错误。

**Q1 001…006 全部否，是否等于模块已全对？**  
A：不等于。本票按 8 条留下的用例 + 21 项 + 三问重审。绿测本身不替代「断言能否失败 / 是否对准现行页面」。审完 8 条后未发现新的能在第一次正常跑就撞上的缺陷。

**Q2 留下的每条用例能否失败？**  
A：能。见 §5.1 逐条「能失败」列。不是恒真断言。

**Q3 断言与页面现行已定稿行为是否一致？**  
A：一致。对照主工作区发布版页面行号（§5.1）。origin/main 上合同工作台 / 账单 / 审计事件与 HEAD 无 diff；拓扑与组织商户的 `getPaged`+focus 选择器在 origin/main 已存在（测试实跑环境）。`cmp` 两测试文件 origin/main+diff ≡ 主工作区。

**Q4 删除的三条是否确有本页测试守住？**  
A：有。见 §5.2。不是「删了就没人钉」。三条现行行为分别是 ADM-MER-006 可点、我的合同纯文本、授权工作台商户号纯文本——与 M023-005 已定稿一致，不是本模块该把旧可点目标补回来。

**Q5 分页桩形状与页面读取是否一致？**  
A：一致。页面 `useServerPagedRows` 要 `Promise<PagedResult<T>>`，读 `data.items`。桩：`{ items, total: items.length, page: 1, size: 500 }`。`size: 500` 与拓扑 / 组织商户传入的 pageSize 相同。`url.startsWith(routeKey)` 对 `/api/v1/platform/merchants?page=1&size=500` 与 `?includeArchived=false&page=…` 都能命中。桩忽略请求的 page/size、一次倒出该路由全部 items——本模块用例每路由最多 2 行，第一次正常跑不会因翻页而空表。分页正确性由 `GovernanceTopologyPage.test.tsx`「超过默认500条…第二页」等本页测试守住，不在本模块声称范围。不为理论翻页场景加需求。

**Q6 有无新引入问题（修复回归）？**  
A：8/8 与全量 978 均绿，tsc 0。否定前缀断言仍能在「无 navigateTo 仍出 `trace-link-*` 按钮」时失败。删除导入 `MerchantZoneContractsPage` / `MerchantScopePage` 后 P2c 不再渲染那两页，与删除用例匹配，无闲置导入。未改页面 / TraceLink / AdminApp / 路由。未发现新工程缺陷。

**Q7 要不要把账单 ADM-CONT-001 改判进本模块队列？**  
A：不要。页面仍 `pageId="ADM-CONT-001"`（`MerchantZoneBillsPage.tsx:124-125,150-151`），与测试 L50-52 一致。受众不可达是同一事实，B2 已定 GR-07→M285。无新证据。本测试锁住现行页面，M285 若改 pageId 须同批改本用例，否则重演 001。

**Q8 否定断言只查 `trace-link-` 前缀、不查任意 button，是否仍落空？**  
A：对 TraceLink 实现：有 navigateTo 必带该前缀 testid。本用例不传 navigateTo，现行页面走 TraceLink。第一次要拦的回归就是「无 navigateTo 仍出 TraceLink 按钮」——前缀选择器能红。另造无 testid 的裸 button 要换组件，不是本页现行路径，不为该理论场景加断言。

**Q9 拓扑夹具用 `displayName`、页面展示 `merchantName`，是否测空？**  
A：该用例断言 `data-focus-node="m-2"` 与 `admin-row-selected`，按 `merchant.id` 高亮，不读显示名。实跑绿。改 focus 逻辑即红。不是缺陷。

**Q10 要不要为本模块补 AdminApp 集成测 / 抽共享 makeApi / 把 vitest 塞进 predeploy？**  
A：不要。B2 已只登记（测试不经 AdminApp；夹具重复；预部署→M003/M005）。铁律 6：第一次正常跑不会因这三件事单独失败。无新证据不改判。

ponytail-review（复杂度，非正确性）：两文件 `makeApi` 重复是隔离夹具。`Lean already. Ship.` 于过期抽象。净可删业务行 0。已删三条是正确性修复的结果，不是本票再删测试文件。

---

## 5. 八条留下的用例 + 三条已删覆盖

根因二分：测试过期 vs 页面回归。本票 8 条均为「与已定稿页面一致且能失败」。

### 5.1 留下的 8 条（全文行号=主工作区发布版）

**L1 Drilldown L45-58「合同工作台商户列点击 → navigateTo(ADM-GOV-002, merchantId)」**  
原句：`expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-002", "m-9");` / `findByTestId("trace-link-ADM-GOV-002-m-9")`  
页面：`ContractWorkbenchPage.tsx:488-490` `pageId="ADM-GOV-002"`。列表走 `api.get("/api/v1/contracts")` 等（L157-161），与桩 `get` 回数组一致。  
能失败：改 pageId、不渲染 TraceLink、不点不调 navigateTo 即红。  
与已定稿：一致。分类：细化（上位 PGM-001/BSM-001）。判据：PGM-001、BSM-001、GEN-002、GEN-003。证据类型：2+4。

**L2 Drilldown L60-68「无 navigateTo 时降级纯文本（不可点）」**  
原句：`await screen.findByText("c-77");` + `expect(container.querySelector('[data-testid^="trace-link-"]')).toBeNull();`  
页面：`MerchantZoneSettlementsPage.tsx:221` 仍渲染 TraceLink（pageId ADM-MER-006）；无 navigateTo 时 `TraceLink.tsx:16-18` 回 children。文本 `c-77` 仍可见。  
能失败：无 navigateTo 仍出任意 `trace-link-*` 按钮即红（006 的修复点）。  
与已定稿：一致。分类：技术性（无业务发明：只让「不可点」断言能失败）。判据：GEN-003。证据类型：2+4。

**L3 Drilldown L72-88「合同工作台带 focusObjectId → 高亮该合同行」**  
原句：`findByTestId("contract-c-2")` class 含 `admin-row-selected`；`contract-c-1` 不含。  
页面：`ContractWorkbenchPage.tsx:481-482` `row.id === focusObjectId ? "admin-row-selected"`。  
能失败：全表高亮、都不高亮、testid 改名即红。  
与已定稿：一致。分类：细化。判据：PGM-001、GEN-002、GEN-003。证据类型：2+4。

**L4 Drilldown L90-106「审计事件页带 focusObjectId(类型:id) → 切操作日志视图并按业务类型/对象服务端反查」**  
原句：`api.get` 调用含 `/api/v1/audit/operation-events`、`business_type=merchant`、`business_id=m-42`；表含 `merchant.updated`。  
页面：`AuditEventsPage.tsx:121-127,191-199,212`。本条自建 api，不走 makeApi。  
能失败：不切操作日志、漏 query、表无事件即红。  
与已定稿：一致。分类：细化（正反可查）。判据：PGM-001、GEN-002、GEN-003、GEN-011。证据类型：2+4。

**L5 Drilldown L108-125「治理拓扑带 focusObjectId(merchantId) → 高亮该商户行」**  
原句：`document.querySelector('[data-focus-node="m-2"]')` class 含 `admin-row-selected`。  
页面：`GovernanceTopologyPage.tsx:78-83` `api.getPaged`；L199-200 `data-focus-node` / `admin-row-selected`。桩 getPaged 按路由表回 items（002/005）。  
能失败：getPaged 再恒空 → 无节点抛 `focus node not rendered yet`；高亮错行即红。  
与已定稿：一致。分类：技术性（桩形状，无业务发明）+ 细化（focus 定位）。判据：GEN-008、GEN-002、GEN-003、PGM-001。证据类型：2+4。

**L6 P2c L42-53「我的账单来源合同(sourceType=contract)点击 → navigateTo(ADM-CONT-001, sourceId)」**  
原句：`findByTestId("trace-link-ADM-CONT-001-c-88")`；`toHaveBeenCalledWith("ADM-CONT-001", "c-88")`。  
页面：`MerchantZoneBillsPage.tsx:124-126,150-152` 仍 ADM-CONT-001；`api.get` 账单（L101）。  
能失败：改 pageId 即红。与现行页面一致。受众不可达维持 GR-07→M285，本条不进本模块队列。  
分类：细化。判据：PGM-001、GEN-002、GEN-003。证据类型：2+4。

**L7 P2c L55-65「我的账单非合同来源(sourceType=meter) → 不渲染合同链接」**  
原句：`findByText("B002")`；`queryByTestId("trace-link-ADM-CONT-001-m-x")` 为 Null。  
页面：仅 `sourceType === "contract" && sourceId` 才渲染 TraceLink。  
能失败：给 meter 行渲染该 testid 即红。  
与已定稿：一致。分类：细化。判据：GEN-003、PGM-001。证据类型：2+4。

**L8 P2c L70-83「带 focusObjectId(merchantId) → 高亮命中的商户档案行，未命中行不高亮」**  
原句：`organization-merchant-row-m-2` 含 `admin-row-selected`；`m-1` 不含。  
页面：`OrganizationMerchantPage.tsx:144-148` `getPaged`；L390 按 `row.id === focusObjectId` 加 class。桩 getPaged 同形（002/005）。  
能失败：getPaged 恒空找不到 testid；两行都高亮即红。  
与已定稿：一致。分类：技术性（桩）+ 细化（定位）。判据：GEN-008、GEN-002、GEN-003、PGM-001。证据类型：2+4。

### 5.2 删除的三条：本页测试仍守住现行行为

**D1（原 001）「我的对账合同列点击 → navigateTo(ADM-CONT-001, contractId)」**  
现行页面：ADM-MER-006（`MerchantZoneSettlementsPage.tsx:191,221`）。  
本页测试：`MerchantZoneSettlementsPage.test.tsx:210-216` 原句「合同下钻跳「我的合同」并带合同号、不弹抽屉」：`getAllByTestId("trace-link-ADM-MER-006-c-1")` 点击后 `navigateTo` 为 `("ADM-MER-006", "c-1")`；抽屉 L197-198 断言有 ADM-MER-006、无 ADM-CONT-001。origin/main 同在。  
删除本模块过期用例后覆盖未空。

**D2（原 003）「我的合同：合同号→ADM-CONT-001、空间→ADM-GOV-007 均可点」**  
现行页面：`MerchantZoneContractsPage.tsx:50,60` 不接收 navigateTo；合同号/空间/续签自纯文本。  
本页测试：`MerchantZoneContractsPage.test.tsx:113-121` 原句「合同号 / 空间 / 续签自为纯文本，不渲染商户点了没反应的下钻按钮（M023-005）」：`querySelector('[data-testid^="trace-link-"]')` 为 Null，`navigateTo` 未被调用。origin/main 同在。

**D3（原 004）「商户授权工作台：账单商户列点击 → navigateTo(ADM-GOV-002, merchantId)」**  
现行页面：`MerchantScopePage.tsx:80` 不接收 navigateTo；商户列为纯文本。  
本页测试：`MerchantScopePage.test.tsx:175-180` 原句「商户号为纯文本，不渲染点了没反应的下钻按钮」：前缀 `trace-link-` 为 Null，行文本含 `m-1`。该页夹具带商户归属。origin/main 同在。

不为「把纯文本再改回可点」加需求——那是 M023-005 已定稿的受众可达处置，上位仍是 PGM-001，实现细节由那一模块守。

---

## 6. 四分类（冲突 → 细化 → 技术性 → 添加）

本票 **无新 finding**。对留下内容的分类只作覆盖记录，不分配新临时号、不进队列：

- 留下的可点/focus 用例：细化（HTML 上位 PGM-001 / BSM-001 全链可点击、上下可钻）。
- getPaged 桩形状、006 否定前缀：技术性（无业务发明：测试与页面取数/testid 约定；不引入票务/商户/权限等业务概念）。
- 已删三条：曾为细化下的过期断言，B3 已按定稿删除；本票确认删除后覆盖由本页测试承接。
- 冲突：无（测试与三份 HTML 明文无矛盾）。
- 添加：0。不为账单另发明商户可达目标页（属 M285）。

GR-07 等只登记项：本票复核事实仍在，**不改判、不升队列、不新编号**。

---

## 7. 覆盖表（禁止抽样宣称 PASS）

| 段 | 行 | 审完 |
|---|---|---|
| Drilldown 夹具 makeApi/baseProps | 1-42 | 是 |
| Drilldown P1-A 源页两例（商户列可点；无 navigateTo） | 44-69 | 是（2 绿，含 006 否定前缀） |
| Drilldown P1-A 目标页三例（合同 focus；审计反查；拓扑 focus） | 71-126 | 是（3 绿，含 getPaged 桩） |
| P2c 夹具 | 1-39 | 是（已删两行闲置导入） |
| P2c 源页两例（账单合同可点；meter 不可点） | 41-67 | 是（2 绿；账单可点维持 GR-07 只登记） |
| P2c 目标页一例（组织商户 focus） | 69-84 | 是（1 绿，含 getPaged 桩） |
| 已删三条的本页守门 | 见 §5.2 | 是 |

8/8 用例审完。对照页 + TraceLink + navigateTo + adminRoutes + PagedResult + 三条本页测试已读。21 项逐项。三线完成。

---

## 8. 观察（不入修复队列；无新证据不改判）

- OBS-A（原 GR-07）：`MerchantZoneBillsPage.tsx:124-125,150-151` 仍 `pageId="ADM-CONT-001"`；本模块 `TraceLinkP2c.test.tsx:42-53` 锁住该目标。商户账号打开 ADM-MER-003（`adminRoutes.ts:89`）点来源合同 → `AdminApp.tsx:547-551` 静默 return。属 M285。本票只登记维持。
- OBS-B（原预部署）：`package.json:96` `verify:predeploy` 不含 `verify:unit`。属 M003/M005。
- OBS-C（夹具重复）：两文件 `makeApi`/`baseProps` 重复，隔离夹具，无业务发明。ponytail-review 不抽共享测试工具。
- OBS-D（测试不经 AdminApp）：`navigateTo: vi.fn()`。M023-005 已记录。除 OBS-A 页面外，不要求本模块补集成测。

---

## 9. 结论

**PASS**

- 本合成模块 2 文件全文覆盖、21 项逐项、三线完成。
- 实跑：origin/main `bfb17f8b` + 冻结 diff，两文件 8/8 绿，全量 206 文件 978 测绿，`tsc -b --noEmit` 0。worktree 已先删软链再 `git worktree remove`（无 `--force`）。
- 留下的 8 条均能失败，且与页面现行已定稿行为一致。删除的 3 条由各页自己的测试守住现行行为。分页桩 `{ items, total, page, size: 500 }` 与 `useServerPagedRows` / `PagedResult` 读取一致。
- 001…006 已修事实被本票独立复核，不是「台账写否就过」。无新工程缺陷，无回归。
- 本模块修复队列建议「是」：空。GR-07 / 预部署 / 夹具重复 / 不经 AdminApp 维持只登记。添加 0。无 HTML 修改申请。无 LOCK-AMEND。
- 不为理论场景加需求；不改页面 / TraceLink / AdminApp / 路由 / 锁 / findings / ledger。

（完）
