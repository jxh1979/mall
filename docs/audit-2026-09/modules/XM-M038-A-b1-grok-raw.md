CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M038-A B1 席乙（Grok）修复前全量审计 raw

- 席位：Grok 独立只读审计席乙（第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B1（合成模块，D-031；修复前全量；不修、不定稿、不进 B3）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：XM-M038-A（来源 `modules/M038-access-web-seg1.md` 登记的 XM-M038-A；目标模块 M081 / M082）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- origin/main：`bfb17f8b4f87fbacd1e0284b728901c8e3f4e053`
- merge-base HEAD origin/main：`88c7b465475fb8eb26e46200ae90ad3e00500522`
- `test -f .git/MERGE_HEAD` → **NO_MERGE**
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.14；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（未抄 sidecar / ledger / 其它报告）。sidecar 与 ledger 顶部同值，仅作交叉观察
- LOCK-AMEND-COUNT：1（ledger 顶部；本席未改锁）
- 映射：锁 L6466 M081 与 L6467 M082 同一组 21 项；本席解析 COUNT=21 UNIQUE=21 MISSING=[]
- 写入集合：仅两个测试文件。页面 / TraceLink / AdminApp / 路由属别的模块，只读对照
- 独立性：未打开 `modules/XM-M038-A-b1-self-raw.md` 正文。检索其它文件时若扫到该路径片段一律丢弃不采信。未改计划 / findings / ledger / 业务代码 / 三 HTML / 锁；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席；未对数据库执行写语句；除一次性 worktree 外未建脚本、日志或临时审计产物
- 本席步骤结论：**FAIL**
- 工程缺陷成立、建议本模块修复队列「是」：GR-01、GR-02、GR-03、GR-04、GR-05、GR-06
- 页面缺陷只登记、本模块队列「否」：GR-07（属 M285）
- 添加项 0；无 HTML 修改申请；LOCK-AMEND 0

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md。用户指令禁止子代理 / 第三席 / MCP，优先于 Arc 默认全量派席。B1 禁止修复，故 TDD / ponytail 修复强度 / receiving-code-review 的 IMPLEMENT 一律不执行。runbook「阶段 B 不创建 worktree」被本轮用户指令覆盖：前端单测只在基于 origin/main 的一次性 worktree 里进行。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读；用户指令优先于技能 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 路由到 audit；**用户禁子代理 / 禁改业务**，不跑 Arc 默认派评审、不写 `docs/arc/audits/` |
| graft | `~/.claude/skills/graft/SKILL.md` | 已读并调用：`graft ask "how does TraceLink navigateTo and focusObjectId work" --source --in apps/web/src/admin/`（tokens saved ≈ 34,089）；`graft grep "navigateTo" --in apps/web/src/admin/`。随后仍全文实读两测试文件与对照页 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读。vitest / sha256 / worktree add-remove 均前台完成，未挂 monitor |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`、同目录 `grill-me/` | 对抗线自问自答；本体「等用户 / 派子代理」作废 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | TraceLink 是深模块（无 navigateTo 或无 objectId → 纯文本；有则渲染可点按钮）。测试是浅适配。缺陷落在「断言的 pageId / 取数缝是否仍是页面现行接口」 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现；B1 禁止修复、不加诊断桩。假说用 vitest 输出与源码行验证 |
| tdd / test-driven-development | 已读 | 判断每条用例能否因目标行为变坏而红；本席不写测试 |
| verification-before-completion | 已读 | PASS/FAIL 前当场：独立 sha256、cmp origin/main、一次性 worktree 两文件 11 测、席甲全量日志闭集复核 |
| receiving-code-review | 已读 | 禁止表演性同意；本轮无对方 raw 可收 |
| requesting-code-review | 存在，B4 用 | B1 不适用 |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → NO_MERGE |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 存在；只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B1 不修；review 口径见 §8 |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B1 不适用 |
| i-have-adhd | `.../i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 存在；正文只落 raw；CLI 不回带结论或机器标记 |
| audit-context-building | 已读 | 要求派 function-analyzer 子代理：与「只允许两席」冲突，**不执行**，记缺口 |
| batch-grill-me | 本机无独立 SKILL | **缺口**；已并入 grilling 自问自答 |
| ubiquitous-language | 本机无独立 SKILL | **缺口** |
| dispatching-parallel-agents / subagent-driven-development / research / using-git-worktrees | 存在 | 提示词划掉。一次性 `git worktree add --detach` 是本轮用户指令允许的只读核，不是 using-git-worktrees 技能本体；核完已 remove，未用 `--force` |

明确不用：executing-plans、finishing-a-development-branch、MCP、第三主体、Arc 默认派评审。

一次性 worktree：`git worktree add --detach /home/james/wt-xm038a-b1-grok-ro origin/main`（HEAD `bfb17f8b`），软链主工作区 `node_modules` 与 `apps/web/node_modules`，在 `apps/web` 下 `npx vitest run src/admin/pages/TraceLinkDrilldown.test.tsx src/admin/pages/TraceLinkP2c.test.tsx`。因 vitest exit 1 且包装命令 `set -e`，收尾未在同一条命令内完成；随后单独先删两条软链，再 `git worktree remove`（无 `--force`）。`test ! -d /home/james/wt-xm038a-b1-grok-ro` 成立。未装依赖。未写仓库内日志。

graft 本轮检索调用：2（ask + grep）；ask 行报 tokens saved ≈ 34,089。

---

## 1. 范围核验与锁哈希

独立命令（独占锁行已写在本文第一行）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`
- sidecar 同值；ledger 顶部同值；`LOCK-AMEND-COUNT: 1`
- `criteria-lock.md` 版本 V0.14；判据总数 401
- 映射行 M081 / M082 与开工 21 项闭集相同

其它只读哈希：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |
| `docs/business-enum-quick-reference-v0.1.html` | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f` |

三 HTML 只读、未改。锁内本模块用到的锚点行对本席读取的现行 HTML **全部命中**。三 HTML 本身未发现需人闸修改的原文错误。

### 1.1 HTML 锚点实核

| 判据 | 锁出处 | 现行该行含 |
|---|---|---|
| BSM-001 | bsm:505 | `整体说明：这套系统到底管什么`（同节 `id="principle-traceable"`：数据可溯源、全链可点击） |
| BSM-003 | bsm:563 | `系统通用能力：数据可改可删、操作有引导、权限分岗` |
| PGM-001 | pgm:256 | `整体定位：平台治理层不是一个业务部门，而是所有业务的底座`（同节：全链可点击；界面全中文） |

### 1.2 两测试文件（全文覆盖）与 origin/main 逐字节

命令：`cmp <工作区> <(git show origin/main:…)` 两文件 IDENTICAL。`git hash-object` = `git ls-tree origin/main` blob。

| 路径 | 物理行 | 字节 | SHA256 | git blob | 归属 |
|---|---:|---:|---|---|---|
| `apps/web/src/admin/pages/TraceLinkDrilldown.test.tsx` | 131 | 6460 | `7425b66a9c6f9127451e5b85405e129c82524df184d89280a3d08d0decc55264` | `a5a4b639fc2a31727ea8c48a0024146d1cf4ed18` | M081 |
| `apps/web/src/admin/pages/TraceLinkP2c.test.tsx` | 107 | 4797 | `6ecec6c0a1bd0f4f5deadaf37e04e5c95fb7278c39d02da1580a303f4eb4fcd8` | `8366f3df879cf6cf6c742a8ff4b5d2fe6299e9f9` | M082 |

全文实读，无抽样。Drilldown 6 个 `it`（L40–130）；P2c 5 个 `it`（L39–106）。夹具 `makeApi` / `baseProps` 两文件同构：`get` 按前缀分发，`getPaged` 恒 `{ items: [], total: 0, page: 1, size: 10 }`（Drilldown L13–26；P2c L12–25）。用户夹具无 `merchantId`（Drilldown L31；P2c L30）。

### 1.3 只读对照（git hash-object = origin/main tree blob）

页面不在本合成模块写入集合。

| 路径 | 行 | SHA256 | 归属 |
|---|---:|---|---|
| `ContractWorkbenchPage.tsx` | 878 | `0f4a60060a16437ec232694257b5993f6c4d901ca249da3fecbd3514b3685c12` | M259 |
| `MerchantZoneSettlementsPage.tsx` | 272 | `271ae798a161f18a37befb5f64b3b8a4dcede4348babfc286715116ded3dd5e8` | M023 |
| `AuditEventsPage.tsx` | 437 | `6bf1d4ad32f6055684ad19267cb5f7b4bf8c47aa460ca57bf4d419a5a95d63fc` | M096 |
| `GovernanceTopologyPage.tsx` | 274 | `6338225cb0367502fefb3db2348673fe602ee9e6bed3cdae321feeea9a9b7036` | M012 |
| `MerchantZoneBillsPage.tsx` | 188 | `01de6aa58cf4b2720dc519753e365fde49c861621c3e4fdca65d4a8dfffc132e` | M285 |
| `MerchantZoneContractsPage.tsx` | 175 | `8a932c358b95fd24fcebfada8126f006813f16130ac161317b3fd25f615dee9f` | M023 |
| `MerchantScopePage.tsx` | 217 | `c328add4b8e4767b6c21f4f9766dbdd4da41fdd05d08d26f6229853d5b7579e9` | M023 |
| `OrganizationMerchantPage.tsx` | 524 | `55dc9a69205df3254c4031015bfe53907a3b1fbd7605e97c8f64a8ab515cb328` | M012 |
| `components/TraceLink.tsx` | 30 | `213c965bdab77902a5a0518520513b9a43f400727713ce8ee88071c76f24818a` | 共享 |
| `AdminApp.tsx` | 745 | `3b4258fb264da856864955cc15a34681fe6d43938e38877936a410dd1f070b8a` | 共享 |
| `adminRoutes.ts` | 188 | `b88500ae13ca94036f7a8e0df5d7bd4d4e7d18fdeb6031bf499d7f55e57d1112` | 共享 |

已定稿对照：

- M023-005（findings 定稿；M023 收口 `M023-merchant-portal-web-seg1.md`：队列已否 / 已修）：对账页合同下钻改指 ADM-MER-006；合同页合同号/空间/续签自与授权工作台商户号降纯文本；不改 AdminApp / TraceLink / 路由。
- M012-013（M012 收口 `M012-governance-topology-web-01.md`：001–019 队列全否；分页改造保留）：拓扑页与组织商户页走 `api.getPaged` + `useServerPagedRows`。

---

## 2. 实跑取证（类型 2 / 4）

### 2.1 本席一次性 worktree（origin/main `bfb17f8b`）

命令：

```
git worktree add --detach /home/james/wt-xm038a-b1-grok-ro origin/main
ln -s /home/james/complex-management/node_modules …/node_modules
ln -s /home/james/complex-management/apps/web/node_modules …/apps/web/node_modules
cd …/apps/web && npx vitest run src/admin/pages/TraceLinkDrilldown.test.tsx src/admin/pages/TraceLinkP2c.test.tsx
```

结果（exit 1）：

```
Test Files  2 failed (2)
Tests  5 failed | 6 passed (11)
```

| 文件 | 结果 |
|---|---|
| Drilldown 合同工作台商户列点击 → navigateTo(ADM-GOV-002, merchantId) | 通过 |
| Drilldown 我的对账合同列点击 → navigateTo(ADM-CONT-001, contractId) | 失败 |
| Drilldown 无 navigateTo 时降级纯文本（不可点） | 通过 |
| Drilldown 合同工作台带 focusObjectId → 高亮该合同行 | 通过 |
| Drilldown 审计事件页带 focusObjectId(类型:id) → … | 通过 |
| Drilldown 治理拓扑带 focusObjectId(merchantId) → 高亮该商户行 | 失败 |
| P2c 我的账单来源合同(sourceType=contract)点击 → navigateTo(ADM-CONT-001, sourceId) | 通过 |
| P2c 我的账单非合同来源(sourceType=meter) → 不渲染合同链接 | 通过 |
| P2c 我的合同：合同号→ADM-CONT-001、空间→ADM-GOV-007 均可点 | 失败 |
| P2c 商户授权工作台：账单商户列点击 → navigateTo(ADM-GOV-002, merchantId) | 失败 |
| P2c 带 focusObjectId(merchantId) → 高亮命中的商户档案行 | 失败 |

失败原句（vitest）：

1. `Unable to find an element by: [data-testid="trace-link-ADM-CONT-001-c-77"]`（Drilldown L61）。同次 DOM 已有 `data-testid="trace-link-ADM-MER-006-c-77"`。
2. `Error: focus node not rendered yet`（Drilldown L126）。DOM：`data-testid="governance-topology-empty"`「暂无治理拓扑数据」。
3. `Unable to find an element by: [data-testid="trace-link-ADM-CONT-001-c-1"]`（P2c L72）。页面已渲染「我的合同」表。
4. `Unable to find an element by: [data-testid="trace-link-ADM-GOV-002-m-5"]`（P2c L87）。DOM：`data-testid="admin-error"`「当前账号缺少商户授权范围，无法展示任何商户数据，请联系管理员配置。」
5. `Unable to find an element by: [data-testid="organization-merchant-row-m-2"]`（P2c L103）。DOM：`organization-merchant-focus`「当前列表尚未定位到该商户，请检查筛选或翻页」+ `organization-merchant-empty`。

### 2.2 席甲全量日志（可读可复核，闭集交叉）

`/home/james/audit-tmp/xm038a_full.log` 去色后：

```
Test Files  2 failed | 204 passed (206)
Tests  5 failed | 976 passed (981)
```

失败 5 条名称与本席两文件实跑完全相同，无第六条前端单测失败。本席未重跑 981 条全量（本模块范围是这两个文件；全量日志证明失败闭集不超出这两文件）。

---

## 3. 第一性原理三问

### 3.1 为什么必须存在？删除后哪条 HTML 做不到？

两文件是 PGM-001 / BSM-001「数据可溯源、全链可点击」在后台网页上的可执行证据：源页引用可点、目标页按 `?focus=` 定位、无 `navigateTo` 降纯文本。删掉它们，HTML 原文仍在，但 M081/M082 对这两处声称会失去类型 2 证据。答得上上位需求 → 不删除，走四分类。存在理由成立。

### 3.2 声称的行为是否实际执行？

11 条声称里 5 条 vitest 红（§2.1）。红的 5 条不是页面把已定稿行为改坏，而是测试仍断言旧 pageId / 旧 `get` 缝：

- 对账页现行 `MerchantZoneSettlementsPage.tsx:191,221`：`pageId="ADM-MER-006"`（M023-005 已修）。
- 合同页现行 `MerchantZoneContractsPage.tsx:60` 不接收 `navigateTo`；L70–80 / L135–140 合同号、空间、续签自为纯文本（M023-005 已修）。
- 授权工作台现行 `MerchantScopePage.tsx:80` 不接收 `navigateTo`；L110–117 无 `user.merchantId` 即抛 `TEXT.scopeMissing`；L163/175 商户列为 `display(row.merchantId)`（M023 收口 + M023-005）。
- 拓扑 / 组织商户现行 `GovernanceTopologyPage.tsx:70–95`、`OrganizationMerchantPage.tsx:137–153` 走 `api.getPaged`；测试 `getPaged` 恒空页（M012-013 已修的分页缝）。

通过的 6 条：vitest 绿，且与对照页现行代码一致（§5）。「测试通过」本身不替代行为证据（GEN-003）；本席用页面源码 + 能失败分析补证。

### 3.3 哪个前提未经验证？

1. 源页 TraceLink 的 `pageId` 仍是 ADM-CONT-001 / ADM-GOV-007 / ADM-GOV-002（对商户专页）。已被 M023-005 与现行页面推翻。
2. 列表仍走 `api.get` 返回数组。拓扑 / 组织商户已改 `getPaged`；`makeApi` 的 `getPaged` 恒空，测试未验证「有数据时 focus 高亮」。
3. 页面单测里的 `navigateTo: vi.fn()` 等于 AdminApp 真跳转。`AdminApp.tsx:545-552` 无权限或账号形态不允许则静默 `return`。M023-005 已写明该前提；本模块仍有一条绿测试把不可达目标锁住（GR-07）。
4. `baseProps` 用户是 `platform_admin` 且无 `merchantId`，仍能渲染授权工作台账单表。现行 `MerchantScopePage.tsx:110` 直接 fail-closed。

---

## 4. 顺从式：21 项逐项

每项：适用 / 证据 / 是否通过。无 finding 不等于未审。

| ID | 适用 | 证据 | 本模块 |
|---|---|---|---|
| BSM-001 | 适用。全链可点击是同节原则 | 测试应钉住现行可点目标与 focus 定位。GR-01/03/04 仍钉 M023-005 已改掉的目标 | 不通过 |
| BSM-003 | 适用（通用能力标题；本文件不实现存档/分步新建） | 测试覆盖下钻引导，不覆盖存档删除或分岗。权限闸在 AdminApp，本文件用 wildcard 夹具属页面单测缝，不另立需求 | 通过（本文件范围内） |
| GEN-001 | 适用 | 两文件有可验证用途（§3.1），不是纯转发。过期断言应改不应删 | 通过 |
| GEN-002 | 适用 | 5 条声称入口与现行页面不一致（§2.1） | 不通过（GR-01…05） |
| GEN-003 | 适用 | 红 5 条：断言未能执行现行目标行为。绿 1 条「无 navigateTo」否定断言绑已废弃 testid，不能捕获现行 pageId 下的「仍可点」回归（GR-06）。绿的账单合同下钻测的是 `vi.fn()`，测不到 AdminApp 拦截（GR-07 页面侧） | 不通过 |
| GEN-004 | 适用面窄 | 测试无外部输入信任边界；夹具对象为测试自造 | 通过（N/A 到本文件实现） |
| GEN-005 | 适用面窄 | 本文件不实现授权。`AdminApp.navigateTo` 默认拒绝在对照层已存在。测试用 wildcard / 假 navigateTo，测不到最小权限；不要求本文件重写 AdminApp | 通过（对照层；本文件不新增授权规则） |
| GEN-006 | 不适用 | 无状态转换 / 金额写入 | 通过（N/A） |
| GEN-007 | 不适用 | 无并发写路径 | 通过（N/A） |
| GEN-008 | 适用 | 测试期望的 pageId / 取数方法与页面、`adminRoutes.ts` 不一致（GR-01…05）。账单页测试与页面一致、与路由可达性不一致（GR-07，属 M285） | 不通过 |
| GEN-009 | 适用面窄 | 授权工作台缺 `merchantId` 走可见中文错误（页面行为，M023）。测试未断言该错误路径，只找已不存在的链接 | 通过（错误在对照页；本文件未吞错） |
| GEN-010 | 适用面窄 | 无秘密、无注入面 | 通过（N/A） |
| GEN-011 | 适用面窄 | 审计事件用例仍打到 `/api/v1/audit/operation-events`（绿）。本文件不是审计实现 | 通过 |
| GEN-012 | 不适用 | 无无界批处理 | 通过（N/A） |
| GEN-013 | 不适用 | 无构建/依赖声明 | 通过（N/A） |
| GEN-014 | 不适用 | 无迁移 | 通过（N/A） |
| GEN-015 | 适用 | 用例名/页面文案中文。`data-testid` 与事件名属系统标识。TraceLink `title`「下钻到来源：…」中文。本文件无英文界面发明 | 通过 |
| GEN-016 | 适用 | 测试名称/断言把已修页面写成当前事实（ADM-CONT-001 / ADM-GOV-007 / 非分页 get） | 不通过（GR-01…05） |
| GEN-017 | 适用 | 本席按冲突→细化→技术性→添加分类，见 §6。无把业务发明写成技术性 | 通过（本席分类） |
| GEN-018 | 适用 | 结论落到 HTML 原文、vitest 输出、源码行，不以席位意见为据 | 通过（本席取证） |
| PGM-001 | 适用 | 全链可点击须受众可达。M023-005 已按此改三页；本模块测试未跟。账单页（M285）仍指 ADM-CONT-001，商户账号点了静默不跳 | 不通过（GR-01/03/04 测试；GR-07 页面） |

21/21 均有记录。不通过项绑定 GR-01…07，无锁外判据。

---

## 5. 十一条用例：根因 / 能否失败 / 与已定稿是否一致

根因二分：测试过期 vs 页面回归。页面回归 = 对照页偏离其所属模块已定稿行为。

### 5.1 失败（5）

**F1 Drilldown L55-63「我的对账合同列点击 → navigateTo(ADM-CONT-001, contractId)」**  
根因：**测试过期**。页面 `MerchantZoneSettlementsPage.tsx:221` 渲染 `trace-link-ADM-MER-006-c-77`。vitest DOM 已给出该按钮。M023-005 最小处置即改指 ADM-MER-006。不是页面回归。  
正常触发：origin/main 跑该文件即红。

**F2 Drilldown L113-130「治理拓扑带 focusObjectId(merchantId) → 高亮该商户行」**  
根因：**测试过期**。页面 `GovernanceTopologyPage.tsx:78-83` 调 `api.getPaged`；`useServerPagedRows.ts:34,71` 用 `result.items`。`makeApi` L20 `getPaged` 恒空 → 空态。页面仍有 `data-focus-node` / `admin-row-selected`（L199-200）。M012-013 分页已落地。若 `getPaged` 按同一路由表返回 `{items,total}`，该用例才能检验高亮。不是页面回归。  
正常触发：同上。

**F3 P2c L64-76「我的合同：合同号→ADM-CONT-001、空间→ADM-GOV-007 均可点」**  
根因：**测试过期**。页面 `MerchantZoneContractsPage.tsx:60` 不接收 `navigateTo`；L135-140 合同号 `shortId`、空间 `join("、")`、续签自纯文本。M023-005：无商户可达目标者降纯文本。`adminRoutes.ts:19,50`：ADM-CONT-001 需 `contract.draft.manage`，ADM-GOV-007 是空间类型字典。不是页面回归。

**F4 P2c L78-89「商户授权工作台：账单商户列点击 → navigateTo(ADM-GOV-002, merchantId)」**  
根因：**测试过期**（两层，同一次正常跑都撞上）。① `MerchantScopePage.tsx:80` 不接收 `navigateTo`；L163/175 商户列为纯文本（M023-005）。② 夹具用户无 `merchantId`，L110-117 抛「缺少商户授权范围」，表根本不渲染。`adminRoutes.ts:17` ADM-GOV-002 需 `merchant.profile.view`。不是页面回归。

**F5 P2c L93-106「带 focusObjectId(merchantId) → 高亮命中的商户档案行」**  
根因：**测试过期**。同 F2：`OrganizationMerchantPage.tsx:144-148` `getPaged`；空 items → L415-419 显示「尚未定位到该商户」且无 `organization-merchant-row-m-2`。页面仍有 `data-testid={organization-merchant-row-${id}}` 与 `admin-row-selected`（L390）。M012-013 已修分页。不是页面回归。

### 5.2 通过（6）— 能否失败、是否与现行已定稿一致

**P1 Drilldown L40-53 合同工作台商户列 → ADM-GOV-002**  
能失败：去掉 TraceLink 或改 pageId 即红。现行 `ContractWorkbenchPage.tsx:487-490` 仍 `pageId="ADM-GOV-002"`，列表仍 `api.get("/api/v1/contracts")`（L158）。与页面一致。合同工作台非商户专页（`adminRoutes.ts:19`），点向组织商户档案与 M023-005 三页处置不冲突。

**P2 Drilldown L66-73 无 navigateTo 降纯文本**  
部分能失败：缺文本 `c-77` 会红。不能失败的缺口：现行有 navigateTo 时 pageId 已是 ADM-MER-006；本用例 `queryByTestId("trace-link-ADM-CONT-001-c-77")` 在「无 navigateTo 却用 ADM-MER-006 渲染按钮」时仍为 null。见 GR-06。文本 `c-77` 与 `TraceLink.tsx:16-18` 降纯文本仍一致。

**P3 Drilldown L77-93 合同工作台 focusObjectId 高亮**  
能失败：去掉 `admin-row-selected` 或 `data-testid="contract-c-2"` 即红。现行 L481-482 仍按 `row.id === focusObjectId` 加类。与页面一致。

**P4 Drilldown L95-111 审计事件 focus `merchant:m-42`**  
能失败：URL 缺 `business_type` / `business_id` 或表无 `merchant.updated` 即红。现行 `AuditEventsPage.tsx:120-132,191-199` 仍 `api.get` 操作日志并带过滤参数。与页面一致。

**P5 P2c L39-50 账单 sourceType=contract → ADM-CONT-001**  
能失败：改 pageId 即红。现行 `MerchantZoneBillsPage.tsx:124-125,150-151` 仍 `pageId="ADM-CONT-001"`，列表仍 `api.get`（L101）。**与现行页面一致，与 PGM-001 受众可达不一致**——该页是 ADM-MER-003、`accountForms: ["merchant"]`（`adminRoutes.ts:89`），目标需 `contract.draft.manage`（L19），`AdminApp.tsx:547-551` 静默 return。属 M285 页面缺陷（GR-07），本测试锁住现行页面，本模块不改页面。

**P6 P2c L52-62 账单 sourceType=meter 不渲染合同链接**  
能失败：给 meter 行渲染合同 TraceLink 即红。现行仅 `sourceType === "contract" && sourceId` 才渲染。与页面一致。

---

## 6. 对抗式（grilling 自问自答）

事实自己读，不派子代理、不问人。

**Q1 五条红是不是页面回归？**  
A：对照页与 origin/main 逐字节相同，且分别落在 M023-005 / M012-013 已修行为上。vitest DOM 对 F1 直接给出 ADM-MER-006 按钮。根因是测试过期。

**Q2 只改 `getPaged` 桩、不改页面，F2/F5 会不会绿？**  
A：`useServerPagedRows` 只吃 `getPaged` 的 `items`。现行桩恒空页 → 空态。把 `getPaged` 改成按同一路由表返回分页形状，是测试侧就能验证的前提；本席不改文件。源码结构已足够，不必另造探针文件。

**Q3 「无 navigateTo」绿了是否证明不可点？**  
A：不能。否定断言绑死 ADM-CONT-001。页面已改 ADM-MER-006。见 GR-06。第一次要靠这条用例拦住「无 navigateTo 仍出按钮」时，现行 pageId 下它拦不住。

**Q4 账单合同下钻绿了是否证明全链可点？**  
A：测的是 `vi.fn()` 被调用，不是 AdminApp。商户账号点 ADM-CONT-001 会静默失败。这是 M023-005 已写明的同一机制，落在 M285 页。不为理论场景发明新目标页；只登记。

**Q5 要不要把账单也改指 ADM-MER-006？**  
A：那是 M285 的处置，不是本模块写入集合。本席不为别的模块立法。

**Q6 越权 / 重复点击 / 并发？**  
A：本文件无写接口。TraceLink 单击调 `navigateTo`。重复点击会多次调用假函数；真路径的权限闸在 AdminApp。不为页面单测补 AdminApp 重测。

**Q7 空 objectId？**  
A：`TraceLink.tsx:16-18` 无 value 降纯文本。本文件未单列该分支；组件属共享模块，本文件覆盖「无 navigateTo」。不另加需求。

**Q8 英文泄漏？**  
A：用例名中文。testid / 事件名是系统标识。无 GEN-015 冲突。

**Q9 预部署门禁为何没拦住？**  
A：`package.json:96` `verify:predeploy` 不含 `test:unit` / vitest。`test:unit` 是独立脚本（L89-90）。属 M003 `package.json`。不为「加一道门禁」发明新闸（铁律 6）；只登记观察。

**Q10 能否 PASS？**  
A：有 GR-01…06 待入队，不能 PASS。

---

## 7. 问题清单（GR-01 起）

四分类顺序：冲突 → 细化 → 技术性 → 添加。每条含临时编号 / 文件行 / 原句 / 正常触发 / 分类 / 已有判据 / 证据类型 / 工程缺陷 / 建议队列。

### GR-01

- 位置：`TraceLinkDrilldown.test.tsx:55-63` 原句 `我的对账合同列点击 → navigateTo(ADM-CONT-001, contractId)` / `findByTestId("trace-link-ADM-CONT-001-c-77")`
- 正常触发：origin/main `npx vitest run src/admin/pages/TraceLinkDrilldown.test.tsx` 即红；DOM 实为 `trace-link-ADM-MER-006-c-77`
- 分类：细化（上位 PGM-001 / BSM-001；目标口径已由 M023-005 定稿）
- 判据：PGM-001、BSM-001、GEN-002、GEN-003、GEN-008、GEN-016
- 证据：类型 4（本席 vitest）+ 类型 2（DOM）+ `MerchantZoneSettlementsPage.tsx:191,221`
- 工程缺陷：成立。根因：测试过期
- 建议本模块队列：是。最小处置：断言改为 ADM-MER-006；不改页面

### GR-02

- 位置：`TraceLinkDrilldown.test.tsx:113-130` 原句 `治理拓扑带 focusObjectId(merchantId) → 高亮该商户行`
- 正常触发：同上文件即红；`governance-topology-empty`
- 分类：细化（上位 GEN-002/003；取数缝已由 M012-013 定稿为 getPaged）
- 判据：GEN-002、GEN-003、GEN-008、GEN-016
- 证据：类型 4 + `GovernanceTopologyPage.tsx:70-95` + `useServerPagedRows.ts:34,71` + Drilldown L20
- 工程缺陷：成立。根因：测试过期（getPaged 桩恒空）。页面高亮逻辑仍在 L199-200
- 建议本模块队列：是。最小处置：`makeApi` 的 `getPaged` 按同一路由表返回分页形状；不改页面

### GR-03

- 位置：`TraceLinkP2c.test.tsx:64-76` 原句 `我的合同：合同号→ADM-CONT-001、空间→ADM-GOV-007 均可点`
- 正常触发：该文件即红
- 分类：细化（M023-005 已降纯文本）
- 判据：PGM-001、GEN-002、GEN-003、GEN-008、GEN-016
- 证据：类型 4 + `MerchantZoneContractsPage.tsx:60,70-80,135-140`
- 工程缺陷：成立。根因：测试过期
- 建议本模块队列：是。最小处置：改为断言无上述 TraceLink、合同号/空间为纯文本（与 M023 本页测试同口径）；不改页面

### GR-04

- 位置：`TraceLinkP2c.test.tsx:78-89` 原句 `商户授权工作台：账单商户列点击 → navigateTo(ADM-GOV-002, merchantId)`
- 正常触发：该文件即红；DOM 为缺少商户授权范围
- 分类：细化（M023-005 降纯文本；M023 收口要求无 merchantId fail-closed）
- 判据：PGM-001、GEN-002、GEN-003、GEN-008、GEN-016
- 证据：类型 4 + `MerchantScopePage.tsx:80,110-117,163,175` + 夹具 P2c L30 无 merchantId
- 工程缺陷：成立。根因：测试过期（断言已删除的链接，且夹具不满足现行 fail-closed）
- 建议本模块队列：是。最小处置：不再断言 ADM-GOV-002 链接；夹具若要见到表须带 `user.merchantId` 并与行上 merchantId 相同，再断言纯文本。不改页面

### GR-05

- 位置：`TraceLinkP2c.test.tsx:93-106` 原句 `带 focusObjectId(merchantId) → 高亮命中的商户档案行，未命中行不高亮`
- 正常触发：该文件即红；「尚未定位到该商户」
- 分类：细化（同 GR-02 / M012-013）
- 判据：GEN-002、GEN-003、GEN-008、GEN-016
- 证据：类型 4 + `OrganizationMerchantPage.tsx:144-148,390,415-419` + P2c L19
- 工程缺陷：成立。根因：测试过期
- 建议本模块队列：是。最小处置：与 GR-02 同一 `getPaged` 桩；不改页面

### GR-06

- 位置：`TraceLinkDrilldown.test.tsx:66-73` 原句 `无 navigateTo 时降级纯文本（不可点）` / `queryByTestId("trace-link-ADM-CONT-001-c-77")`
- 正常触发：现行即空转——用例绿，但把 TraceLink 改成无 navigateTo 仍用 ADM-MER-006 出按钮时本断言仍绿
- 分类：技术性（测试否定断言与现行 pageId 脱钩；无业务发明；服务于「能被测试」）
- 无业务发明理由：只修正「不可点」的可失败断言，不增加下钻目标或业务规则
- 判据：GEN-003
- 证据：类型 4（该用例绿）+ 类型 2（F1 同页有 navigateTo 时 testid 已是 ADM-MER-006）+ `TraceLink.tsx:16-18`
- 工程缺陷：成立
- 建议本模块队列：是。最小处置：断言不存在 `button.admin-trace-link` 或任何 `trace-link-*`，保留文本 `c-77`

### GR-07（只登记，页面属 M285）

- 位置：页面 `MerchantZoneBillsPage.tsx:124-125,150-151`；本模块测试 `TraceLinkP2c.test.tsx:39-50` 锁住该目标
- 原句：`pageId="ADM-CONT-001"` / 测试 `navigateTo(ADM-CONT-001, sourceId)`
- 正常触发：商户账号打开 ADM-MER-003（`adminRoutes.ts:89` `accountForms: ["merchant"]`），点来源合同 → `AdminApp.tsx:547-551` 因缺 `contract.draft.manage` 或账号形态静默 return，按钮无跳转。单测 `vi.fn()` 绿
- 分类：细化（上位 PGM-001 受众可达；与 M023-005 同机制。处置由 M285 定，本席不指定必须改指哪一页）
- 判据：PGM-001、GEN-002、GEN-003、GEN-005
- 证据：类型 1（PGM-001 原文）+ 源码行 + `adminRoutes.ts:19,89`
- 工程缺陷：成立（页面）。根因：页面未按受众可达改目标；**不是本两测试文件的回归**
- 建议本模块队列：**否**。M285 修页面时须同批改 `TraceLinkP2c.test.tsx` 该用例，否则重演 GR-01

---

## 8. 观察（不入修复队列）

- OBS-1：`package.json:96` `verify:predeploy` 不含 `verify:unit` / vitest。M023 / M012 检查点只跑本模块测试，未拦住本两文件。属 M003。不为预部署清单加 vitest（铁律 6）。
- OBS-2：两文件 `makeApi` / `baseProps` 重复，是隔离夹具不是业务发明。ponytail-review：不为此抽共享测试工具。`Lean already` 于夹具重复；净可删行 0（过期断言是正确性，不是复杂度）。
- OBS-3：测试不经过 `AdminApp.navigateTo`。M023-005 已记录。除 GR-07 页面外，不要求本模块补集成测。

ponytail-review：无 `delete:` / `yagni:` 针对本两文件的业务抽象。过期断言走 GR-01…06，不走删除测试文件。

---

## 9. 覆盖表（禁止抽样宣称 PASS）

| 段 | 行 | 审完 |
|---|---|---|
| Drilldown 夹具 makeApi/baseProps | 1-37 | 是 |
| Drilldown P1-A 源页三例 | 39-74 | 是（1 红 2 绿，绿含 GR-06） |
| Drilldown P1-A 目标页三例 | 76-131 | 是（1 红 2 绿） |
| P2c 夹具 | 1-36 | 是 |
| P2c 源页四例 | 38-90 | 是（2 红 2 绿，绿含 GR-07 锁） |
| P2c 目标页一例 | 92-107 | 是（红） |

11/11 用例审完。对照 8 页 + TraceLink + navigateTo + adminRoutes + 两条已定稿 finding 已读。

---

## 10. 结论

**FAIL**

- 本合成模块 2 文件全文覆盖、21 项逐项、三线完成。
- 5 条 vitest 失败全部是测试过期，对照页与 M023-005 / M012-013 已定稿行为一致，不是页面回归。
- 6 条通过用例中：4 条与现行已定稿页面一致且能失败；1 条否定断言落空（GR-06）；1 条与现行页面一致但锁住商户不可达目标（GR-07 → M285）。
- 建议本模块修复队列「是」：GR-01…GR-06。GR-07 只登记。添加 0。无 HTML 修改申请。无 LOCK-AMEND。
- 不为理论场景加需求；不改页面 / TraceLink / AdminApp / 路由。

（完）

## XM-M038-A B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **33643** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：固定 Grok 只读外审席乙（grok-4.6 / xhigh）。步骤：B2-r1 整批候选复核。不是 B1/B5 重审、不是修复、不是模块 PASS、不是 B3/B4。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02；criteria-lock V0.14；modules.md V0.04
- 正本：`/home/james/complex-management` 分支 `main`。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `bfb17f8b4f87fbacd1e0284b728901c8e3f4e053`。B3=0。无源修复。
- 席甲：Claude Opus 5.5，作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类。
- 独立性：输入仅为两份已封存 B1 完整 raw + findings 顶部候选。席甲 raw 全文现 14981 bytes；本席只取派发指定的前 **13816** 字节（SHA256 `c510672e103911ea3952884ca13f232ab2a12e0107c7d4f91cdca47d8f273a8b`）。核前缀长度时见到后缀标题「XM-M038-A B2-r1 整批候选复核（席甲票，V0.02）」及输入哈希声明，**未继续读取其后票面与结论，不以该后缀为证据**。未用 MCP / 子代理 / 第三席；不问操作员。未改源码 / 文档 / HTML / lock / findings / ledger；未 fetch / commit / push / merge / deploy；未写本指定路径以外的任何新产物；未对任何数据库执行写语句。一次性 worktree `/home/james/wt-xm038a-b2-r1-grok-mut`（origin/main `bfb17f8b`）核完已先删两条 `node_modules` 软链，再 `git worktree remove`（无 `--force`）；`test ! -d` 成立。
- 本席 B2-r1 票：**PASS**（整批候选同意；001…006 逐 ID PASS；GR-07 与两席其余观察只登记 PASS；添加 0、无 HTML 申请、无 questions）
- 正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（与 B1 独占锁行同值；本段未新增独占锁行）。sidecar 与 ledger 顶部同值，仅作交叉观察。LOCK-AMEND-COUNT 1。

此票只判断 findings.md V3.91 顶部「当前 XM-M038-A B2-r1 整批候选」相对本次两份完整 B1 raw 的差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文，亦不以本票代替 B5。不加判据。无「有条件通过」。B2 不写任何新的独占 `CRITERIA-LOCK-SHA256` 行。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/XM-M038-A-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 33643 | `8dbe09193bedec995ca1c4f886a412ef616ee24cb69c50782fad91de469e9b64` |
| `docs/audit-2026-09/modules/XM-M038-A-b1-self-raw.md` 前缀（席甲 B1；本席只读此前缀） | V0.01 | 13816 | `c510672e103911ea3952884ca13f232ab2a12e0107c7d4f91cdca47d8f273a8b` |
| `docs/audit-2026-09/findings.md` 顶部「当前 XM-M038-A B2-r1 整批候选」 | V3.91 | 1355974（全文件） | `84226279284993dca234031b07778883b4c7443738d6d4892af9eb37d0400e7d` |

席甲 raw 全文现大于 13816 bytes（本席用前 13816 字节得与封存值相同的 SHA，其后自 B2 标题起不作为本票依据）。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

只读核验（本席未改）：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |
| 业务地图 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| 治理地图 | `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f` |
| 枚举速查 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

两测试文件与 origin/main 逐字节相同；对照页 `MerchantZoneSettlementsPage` / `MerchantZoneContractsPage` / `MerchantScopePage` / `GovernanceTopologyPage` / `OrganizationMerchantPage` / `MerchantZoneBillsPage` 亦与 origin/main 逐字节相同。`docs/audit-2026-09/questions/` 无 `XM-M038-A*`。

上位句（本席 B2 只核分类所需原文，不重审模块）：

- 业务地图 L522 `id="principle-traceable"`：`数据可溯源、全链可点击`
- 治理地图 L272：`全局原则 · 数据可溯源、全链可点击（无孤岛数据）`

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核差集/分类/队列/最小边界再接受或反驳；禁止表演性同意；B2 不修复。本批重点：001/003/004 删除是否仍满足判据、002/005 细化→技术性、006 采本席处置、GR-07→M285 |
| grilling | 只借追问结构自查：差集有无漏并/改写、删除是否空了覆盖、分类是否误用 GEN 当 HTML 上位、有无应升队观察 |
| verification-before-completion | 声称前先跑只读哈希/前缀字节/modules.md 归属/三页测试变异/getPaged 探针 |
| systematic-debugging / diagnosing-bugs | 只借读错→对照两 raw 与候选；不改生产代码、不重审两文件的模块结论 |
| codebase-design / ponytail-review | 最小是删已被本页测试守住的过期用例、补分页桩、收紧否定断言；能删就不改 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 已读 SKILL.md；vitest / worktree 均前台完成，未挂 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| graft | B2 目标文件已由派发点名，直读测试/页面/modules.md；未再调 graft |
| audit-context-building | **未用**（本体强制派 function-analyzer 子代理） |

### 完整差集（GR-01…06、GR-07、OBS-1…3、席甲 CX-01…06、OBS-1…3）

漏并：无。改写原意：无。两席 B1 全部临时缺陷 ID 仍落在 001–006 或只登记段。无单席独有缺陷被丢。无本批新开「否」。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| XM-M038-A-001 | CX-01＋GR-01 | 细化/是/是（删除过期用例） | **PASS（细化/是/是）** |
| XM-M038-A-002 | CX-02＋GR-02 | 技术性/是/是（getPaged 按同一路由表返回分页形状） | **PASS（技术性/是/是）** |
| XM-M038-A-003 | CX-03＋GR-03 | 细化/是/是（删除过期用例） | **PASS（细化/是/是）** |
| XM-M038-A-004 | CX-04＋GR-04 | 细化/是/是（删除过期用例） | **PASS（细化/是/是）** |
| XM-M038-A-005 | CX-05＋GR-05 | 技术性/是/是（同 002） | **PASS（技术性/是/是）** |
| XM-M038-A-006 | CX-06＋GR-06 | 技术性/是/是（容器内无 `trace-link-` 前缀元素，保留文本 c-77） | **PASS（技术性/是/是）** |

冲突入队 0。细化入队 3（001/003/004）。技术性入队 3（002/005/006）。观察否：GR-07 与两席其余 OBS 只登记。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001–006（6）；「否」本批新开 = 0。

**整批候选：同意。**

---

### 一、差集核实

| B1 临时 ID | 候选落点 | 核实 |
|---|---|---|
| CX-01＋GR-01 | 001 | 同根：`TraceLinkDrilldown.test.tsx:55-64` 原句「我的对账合同列点击 → navigateTo(ADM-CONT-001, contractId)」，断言 `trace-link-ADM-CONT-001-c-77`；页面 `MerchantZoneSettlementsPage.tsx:191/221` 已是 `ADM-MER-006`。origin/main 即红。事实未改写。最小处置由两席 B1 的「改写断言」收为「删除该用例」，依据见第二节，覆盖同一过期断言，属边界收窄。 |
| CX-02＋GR-02 | 002 | 同根：Drilldown L20 `getPaged` 恒空；L113-130 治理拓扑 focus 高亮红，DOM `governance-topology-empty`。页面 `GovernanceTopologyPage.tsx:74-93` 走 `api.getPaged`。事实未改写。分类见第三节。 |
| CX-03＋GR-03 | 003 | 同根：`TraceLinkP2c.test.tsx:64-76`「我的合同：合同号→ADM-CONT-001、空间→ADM-GOV-007 均可点」；页面 `MerchantZoneContractsPage.tsx:60` 不接收 `navigateTo`，L135-140 合同号/空间/续签自纯文本。事实未改写。处置收为删除，见第二节。 |
| CX-04＋GR-04 | 004 | 同根：`TraceLinkP2c.test.tsx:78-90`「商户授权工作台：账单商户列点击 → navigateTo(ADM-GOV-002, merchantId)」；页面 `MerchantScopePage.tsx:80` 不接收 `navigateTo`，L175 商户号 `display(row.merchantId)`，且 L110 无 `user.merchantId` 即 `scopeMissing`。事实未改写。处置收为删除，见第二节。 |
| CX-05＋GR-05 | 005 | 同根：P2c L19 `getPaged` 恒空；L93-106 组织商户 focus 高亮红。页面 `OrganizationMerchantPage.tsx:140-153` 走 `api.getPaged`。事实未改写。分类同 002。 |
| CX-06＋GR-06 | 006 | 同根：Drilldown L66-73「无 navigateTo 时降级纯文本」查 `trace-link-ADM-CONT-001-c-77`；页面现行 pageId 已是 ADM-MER-006，否定断言落空。席甲 B1 变异（TraceLink 无 navigateTo 也渲染按钮）下该用例仍绿。事实未改写。处置采本席 GR-06，见第四节。 |
| GR-07＝席甲 OBS-1 | 只登记→M285 | 同根：「我的账单」`MerchantZoneBillsPage.tsx:124-125/150-151` 来源合同仍 `ADM-CONT-001`；`adminRoutes.ts:89` ADM-MER-003 `accountForms: ["merchant"]`，L19 ADM-CONT-001 需 `contract.draft.manage`；`TraceLinkP2c.test.tsx:39-50` 锁住该目标。席甲 B1 写成 M099；`plan/modules.md` L4566–4577 M285 FILES 含该页，M099 是 `V103__unified_risk_alerts.sql`。候选采本席归属 M285。本模块队列否。 |

观察未入队（本席核：无应升添加、无应升本批缺陷被降）：

- 本席 OBS-1 ∪ 席甲 OBS-2：`verify:predeploy` 与 `predeploy-gates.yml` 不跑前后端单测 → M003/M005。铁律 6：不为预部署清单加 vitest。
- 本席 OBS-2 ＝ 席甲 OBS-3：两文件各复制一份 `makeApi`/`baseProps`。隔离夹具，不影响正确性。
- 本席 OBS-3：测试不经 `AdminApp.navigateTo`（假函数）。M023-005 已记录；除 GR-07 页面外不要求本模块补集成测。

---

### 二、001 / 003 / 004 最小处置：删除过期用例（变异独立核实）

两席 B1 对这三条的事实一致，拟修都是改写断言（001 改 ADM-MER-006；003/004 改断言纯文本，004 另补 `merchantId`）。候选把最小处置收为删除，依据是席甲读本席 B1 后补跑的 `/home/james/audit-tmp/xm038a_mut2.log`。本席不采信日志结论句，在一次性 worktree 里独立复跑。

命令：`git worktree add --detach /home/james/wt-xm038a-b2-r1-grok-mut origin/main`（HEAD `bfb17f8b`），软链主工作区 `node_modules` 与 `apps/web/node_modules`，在 `apps/web` 下 `npx vitest run`。三页与其测试相对 origin/main 逐字节相同。

**基线（未改页面）：** 三文件合计 `Test Files  3 passed (3)` / `Tests  32 passed (32)`，exit 0。三页 `renderPage` 均传 `navigateTo: vi.fn()`（`MerchantZoneSettlementsPage.test.tsx:60`、`MerchantZoneContractsPage.test.tsx:41`、`MerchantScopePage.test.tsx:50`），夹具非空转。`MerchantScopePage.test.tsx:43` 默认 `user.merchantId: "m-1"`，与账单行 `merchant_id: "m-1"` 相同。

**变异体 1 — 合同页表内合同号/空间/续签自加回 ADM-CONT-001 / ADM-GOV-007 TraceLink（抽屉字段保持纯文本）：**

```
Tests  1 failed | 7 passed (8)
× 合同号 / 空间 / 续签自为纯文本，不渲染商户点了没反应的下钻按钮（M023-005）
  expected <button … data-testid="trace-link-ADM-CONT-001-contract-aaaa1111"> to be null
  MerchantZoneContractsPage.test.tsx:116
```

还原后 `dirty=0`。

**变异体 2 — 对账页两处 `pageId="ADM-MER-006"` 改回 `ADM-CONT-001`：**

```
Tests  2 failed | 8 passed (10)
× 点结算单行 -> 抽屉含结算明细条目中文名与分转元金额，合同下钻指向商户可达的「我的合同」
× 合同下钻跳「我的合同」并带合同号、不弹抽屉
  waitFor getAllByTestId("trace-link-ADM-MER-006-c-1")；DOM 实为 trace-link-ADM-CONT-001-c-1
  MerchantZoneSettlementsPage.test.tsx:180 / :210
```

还原后 `dirty=0`。与 `xm038a_mut2.log` 失败闭集相同。

**变异体 3 — 授权工作台账单商户号加回 ADM-GOV-002 TraceLink：**

```
Tests  1 failed | 13 passed (14)
× 商户号为纯文本，不渲染点了没反应的下钻按钮
  expected <button … data-testid="trace-link-ADM-GOV-002-m-1"> to be null
  MerchantScopePage.test.tsx:178
```

还原后 `dirty=0`。与 `xm038a_mut2.log` 失败闭集相同。

变异成立：把三处 M023-005 现行行为逐个改回旧样子，对应本页测试分别变红。现行行为已由 M023 本页测试守住。

**删除是否仍满足冻结判据：**

- GEN-001 最小性：本席 B1 写「过期断言应改不应删」指两文件整体仍是 PGM-001/BSM-001 的可执行证据，禁止删掉整个测试文件。001/003/004 三条与 M023 本页测试重复：001 的正向钉（对账合同 → ADM-MER-006 且调用 `navigateTo`）已由 `MerchantZoneSettlementsPage.test.tsx:210-214` 守住；003 的「无下钻按钮 + 合同号/空间纯文本」已由 `MerchantZoneContractsPage.test.tsx:113-121` 守住；004 的「商户号纯文本」已由 `MerchantScopePage.test.tsx:175-180` 守住。在 TraceLink 文件里改写只会得到同一行为的第二份断言。删除这三条满足最小性；两文件其余用例仍在。
- GEN-002 / GEN-016：删掉与现行页面不符的现在时声称，两文件不再把已废 pageId 写成当前事实。
- GEN-003：上述三条的能失败网在 M023 本页；本合成模块 006 仍覆盖「无 navigateTo 降纯文本」（对账页）。合同工作台源页、账单源页、三处 focus 定位仍由本两文件守住。
- PGM-001 / BSM-001：受众可达口径已由 M023-005 定稿；本模块删除过期用例后不再与该口径打架。

**是否比改写更小：** 删除是从两文件去掉过期 `it`，零新断言。改写 001 会在 Drilldown 再写一遍 ADM-MER-006 点击；改写 003 会在 P2c 再写一遍纯文本；改写 004 还须给夹具补 `user.merchantId`（否则 L110 `scopeMissing`，断言无行可落）。删除更小。

分类保持细化：上位是治理地图 L272 / 业务地图 L522「数据可溯源、全链可点击」；M023-005 已按受众可达收口。测试过期是该上位的下位实现偏差。票：**PASS（细化／是／是；最小处置＝删除）**。

---

### 三、002 与 005 分类复核

本席 B1 把 GR-02/GR-05 记为细化，上位写 GEN-002/003。2.1 细化须指向三份 HTML 的某一具体条目。`getPaged` 分页取数接口约定不在 HTML；GEN-002/003 是冻结判据，不是 HTML 上位。按顺序：与 HTML 明文无矛盾 → 指不出 HTML 句子 → 无业务发明（不引入实体/枚举/状态/角色/校验/价格/流程）且服务于「被测试」→ **技术性**。席甲 B1 取技术性。候选请本席复核。本票改判技术性，与候选一致。

处置两席 B1 已相同：`makeApi` 的 `getPaged` 按同一路由表返回 `{ items, total, page, size }`；不改页面。`useServerPagedRows.ts:34,71` 吃 `result.items`。本席在同一 worktree 只改两文件 `getPaged` 桩后跑 11 测：`Tests  3 failed | 8 passed (11)`（基线是 5 红 6 绿）。转绿的两条即拓扑 focus 与组织商户 focus；仍红的三条即 001/003/004 过期源页断言。页面定位高亮未回归。

票：**PASS（技术性／是／是）**。

---

### 四、006 采本席 B1 处置

`TraceLink.tsx:16-18`：无 `navigateTo` 或无 `objectId` 降纯文本；有则 `data-testid={`trace-link-${pageId}-${value}`}`。006 不传 `navigateTo`，文本 `c-77` 仍可见。席甲 B1 拟把否定断言改绑 `trace-link-ADM-MER-006-c-77`；候选采本席 GR-06：容器内不存在任何 `[data-testid^="trace-link-"]`，保留文本 `c-77`。

独立核：绑死 ADM-CONT-001 会在现行 ADM-MER-006 下空转（本席 B1 已写；席甲 `xm038a_mut1.log` 同向）。绑死 ADM-MER-006 仍放过「无 navigateTo 却用第三种 pageId 出按钮」。前缀选择器覆盖「不可点」。无业务发明。分类技术性（测试有效性）。票：**PASS（技术性／是／是）**。

---

### 五、GR-07 与观察；有无应升队项

GR-07＝席甲 OBS-1，归属 **M285**（`plan/modules.md` L4576 `MerchantZoneBillsPage.tsx`）。席甲 B1 误写 M099，候选已更正。页面缺陷成立，写入集合不含该页；本模块队列否。M285 修页面时须同批改 `TraceLinkP2c.test.tsx:39-50`，否则重演 001。

其余观察只登记，无应升队：预部署不跑单测属 M003/M005；夹具重复不影响正确性；页面单测走 `vi.fn()` 是既有缝，除账单页外不升本批。

---

### 六、添加／HTML／questions

本批无添加项、无 HTML 修改申请、无 questions。`docs/audit-2026-09/questions/` 无 `XM-M038-A*`。不 BLOCKED-ASK。不按铁律 4 申请改三 HTML。

写入集合只两个测试文件：

- `apps/web/src/admin/pages/TraceLinkDrilldown.test.tsx`（001 删「我的对账合同列点击」用例；002 `getPaged` 按路由表返回分页形状；006 否定断言改前缀选择器并保留 `c-77`）
- `apps/web/src/admin/pages/TraceLinkP2c.test.tsx`（003 删「我的合同均可点」用例；004 删「授权工作台商户列点击」用例；005 与 002 同一 `getPaged` 桩）

禁止：改页面、`TraceLink.tsx`、`AdminApp.tsx`、`adminRoutes.ts`、客户端、后端、三 HTML、锁、台账；不给商户加平台权限；不把 GR-07 账单页改指写进本批；不抽公共夹具；不为预部署加 vitest。

同文件多 ID：两测试文件现无版本标记则 B3 只前进一次（各文件至多加一处 V0.01）。

### 分类／队列／允许的最小修复边界

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止开工 B3。一次 B3 只改本模块 2 文件。

- 001：删除 Drilldown「我的对账合同列点击 → ADM-CONT-001」用例。
- 002：`makeApi.getPaged` 按同一路由表返回 `{ items, total, page, size }`。
- 003：删除 P2c「我的合同：合同号/空间均可点」用例。
- 004：删除 P2c「商户授权工作台：账单商户列点击」用例。
- 005：与 002 同一桩。
- 006：断言容器内无 `[data-testid^="trace-link-"]`，保留文本 `c-77`。

### 结论

全批差集无漏并、无原意改写。001/003/004 细化／是／是，删除过期用例（本席独立变异成立，M023 本页测试已守住现行行为，删除满足 GEN-001/002/003/016 与 PGM-001/BSM-001，比改写更小）。002/005 技术性／是／是（本席 B1 细化改判；无 HTML 句子可指，无业务发明；补桩后定位用例转绿）。006 技术性／是／是，采本席 GR-06。GR-07 只登记→M285。两席其余观察只登记，无应升队。添加 0。HTML 申请 0。questions 0。最小写入集合＝两个测试文件。逐项：差集 PASS、001 PASS、002 PASS、003 PASS、004 PASS、005 PASS、006 PASS、GR-07 只登记 PASS、观察 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；XM-M038-A IN_PROGRESS；B3=0。
