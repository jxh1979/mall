CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M030 B5-r1 席乙（Grok）修复后整模块全量审计 raw

- 席位：Grok 独立只读审计席乙（第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B5-r1（B3 累计 1 后的整模块重审；不只看 diff；不复用 B2/B4 票作结论）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：`plan/modules.md` V0.04 M030「租户范围与数据隔离（技术不变量）／质量检查记录／第1段」；标记 NORMAL；计划有效内容 22 行（B1 开工时口径，现已过期）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- origin/main：`d215fde0b51a0ca3121cf3a3145e1ec31f12621a`（M029 检查点 PR #470 已合并）
- after tree（B3-r1 冻结）：`6a1ead3603bbba4a1d941aa57c49756f90c5384b`；本文件工作区 blob `7bc98032050d2dd7d86c520aeee08dabeb538dbf` 与该 tree 同路径 blob **逐字节一致**
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（未抄 sidecar / ledger / 其它报告）。sidecar 与 ledger 顶部同值，仅作交叉观察
- LOCK-AMEND-COUNT：1（ledger 顶部；本席未改锁）
- 映射 18 项：`M030 → [GEN-001 … GEN-018]`（无 HTML 派生条目）
- findings 输入：V2.96；M030-001 修复队列「否（已修，B3-r1）」；当前队列「是」= 0。不把全部否当成文件已全对
- 独立性：未打开、未通读、未采信 `modules/M030-b5-r1-self-raw.md` 作为本席结论来源。仓库全文 `rg tenant-isolation-baseline` 命中过该路径的 3 行路径/SHA 字面（14 行 / 995 bytes / `65384ebe…`），与本席对主工作区文件的独立 `wc`/`sha256sum` 相同，**不把席甲正文当证据**。旧 B1（含 B2-r1 追加段）与 B4-r1 可读，只作「B2 已定为只登记」的边界，结论以本席全文重审 + 当场实跑为准
- 未改计划 / findings / ledger / 业务代码 / 三 HTML / 锁；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席；未对任何数据库执行语句（本模块无库内核验必要）；除 D-028 允许的一次性 worktree 外未建脚本、日志或临时审计产物。未带 `--write-baseline`
- `test -f .git/MERGE_HEAD` → **NO_MERGE**
- 本席步骤结论：**PASS**（001 修复落地、无回归、无新的须入队工程缺陷；B2 只登记项维持；添加 0；无 HTML 修改申请）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md，不是提一嘴。用户指令禁止子代理 / 第三席 / MCP，优先于 Arc 默认全量派席。B5 禁止修复，故 TDD 落测试、ponytail 修复、receiving-code-review 的 IMPLEMENT 一律不执行。runbook「阶段 B 不创建 worktree」被 ledger「D-028 环境决定」与本轮用户指令覆盖：扫描器实跑只在基于 origin/main 的一次性 worktree 里进行。

本席环境技能根：`/home/james/.claude/skills/`、`/home/james/.claude/plugins/`、`/home/james/.grok/bundled/skills/`、`/home/james/.claude/plugins/cache/`。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读；用户指令优先于技能 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 路由识别为质量审查；**用户禁子代理 / 禁改业务**，不跑 Arc 默认派评审、不写 `docs/arc/audits/` |
| graft | `~/.claude/skills/graft/SKILL.md` | 已读并调用。JSON 不在 graft 索引内，强制全文实读本文件；消费方用 `graft grep` + 仓库 grep |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读。本轮 sha256 / node 扫描 / worktree add-remove 均前台完成，未挂 monitor、未派 loop 子代理 |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗线自问自答；本体「等用户 / 派子代理」作废 |
| grill-me | 同目录 `grill-me/SKILL.md`（disable-model-invocation；正文只写 Run a /grilling session） | 已读头；并入 grilling，不另开会话 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 本文件是浅 JSON 清单 + 深不变量（棘轮 key 集合）。删除测试：删文件则 2 条活 F1 变新增回归 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现；B5 禁止修复、不加诊断桩。本轮反馈环 = worktree 默认/`--strict` |
| tdd / test-driven-development | 已读 | 用来判断棘轮断言能否因目标行为变坏而红；本席不写测试 |
| verification-before-completion | 已读 | PASS 前当场：独立 sha256、after tree blob、origin/main worktree 默认+`--strict`、CSV/仓储 SQL、JSON.parse |
| receiving-code-review | 已读 | 禁止表演性同意；本轮无对方 raw 可收（未读本次 self-raw） |
| requesting-code-review | 存在，B4 用 | B5 不适用（禁止派第三席） |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → NO_MERGE |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 可选存在；只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B5 不修；review 口径扫可删项，见 §8 |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B5 不适用 |
| i-have-adhd | marketplace / cache `skills/i-have-adhd/SKILL.md` | 存在；本席正文只落 raw；CLI 不回带结论或机器标记（适配器优先于该技能的「正文先写下一步」） |
| audit-context-building | Trail of Bits plugin `skills/audit-context-building/SKILL.md` | 要求派 function-analyzer 子代理：与「只允许两席」冲突，**不执行**，记缺口 |
| batch-grill-me | 本机无独立 SKILL.md | **缺口**；已并入 grilling 自问自答，不冒充调用 |
| ubiquitous-language | 本机 skill 目录无 | **缺口** |
| dispatching-parallel-agents / subagent-driven-development / research / using-git-worktrees | 存在 | 提示词划掉。一次性 `git worktree add --detach` 是 D-028 允许的只读核，不是 using-git-worktrees 技能本体；核完已 remove |

graft 本轮有储蓄行的调用：`graft ask` 定位 ≈32,447；`graft grep tenant-isolation-baseline` ≈11,326。合计约 **43,773** tokens。

一次性 worktree：`git worktree add --detach /home/james/wt-m030-b5r1-grok-ro origin/main`（HEAD `d215fde0`），软链 `node_modules`，把主工作区现行 `security-scans/tenant-isolation-baseline.json`（SHA256 `65384ebe…`）拷入后跑默认与 `--strict`，`git worktree remove --force`；`test ! -d /home/james/wt-m030-b5r1-grok-ro` 成立；未带 `--write-baseline`。主工作区本文件 SHA 核后未变。

---

## 1. 模块覆盖（全文实读，行数/SHA 实算）

命令：`wc -l -c` + `sha256sum` + `git hash-object` + `git ls-tree 6a1ead36 -- <path>` + `git show origin/main:<path> | sha256sum` + `python3 json.loads`。

| 路径 | 物理行 | 字节 | SHA256 | 工作区 git blob | HEAD blob | origin/main blob | after tree blob |
|---|---:|---:|---|---|---|---|---|
| `security-scans/tenant-isolation-baseline.json` | 14 | 995 | `65384ebe6101a762b58c0a382fcf2a3e20cb2b14a8ea81898860e78973855da2` | `7bc98032050d2dd7d86c520aeee08dabeb538dbf` | `f75a02636ae88ac16e0abcd407a80be4985d4b23` | `931b651781f2a005190e0ecc2ca845c6fa2b3a8f` | `7bc98032…`（同工作区） |

覆盖：**1/1 全文逐行，不抽样**。合法 JSON；末尾 `\n`；顶层键恰为 `note` / `generated_at_source` / `known_findings`；`known_findings` **2** 个对象，每项恰有 `key`+`text`；无重复 key。

相对 HEAD：`1 file changed, 8 deletions(-)`（B3-r1 删两个陈旧对象）。相对 origin/main：`1 file changed, 3 insertions(+), 11 deletions(-)`（含 OPEN PR #446 中性化 + B3 删键；B2 已登记中性化随检查点带入，本席不重开）。

相关三 HTML（只读，铁律 4 未改；本模块映射无 HTML 派生 ID，HTML 只作第一性原理类型 1 与四分类上位）：

- `docs/business-system-map-for-operations-v0.1.html:707` 红线④原文：「看不到别的商户和别的项目的数据」；同条：七类校验不设关闭开关
- `docs/platform-governance-map-for-operations-v0.1.html:528` 同红线④；`:510`「商户不能查看平台全局和其他商户数据」；`:653`「所有业务数据默认按项目隔离……数据的项目与商户隔离不提供关闭开关」
- `docs/business-enum-quick-reference-v0.1.html:235` 锚点「项目与敏感数据权限」：「所有数据默认按项目隔离，普通岗位不得跨项目查看」

HTML 未点名本 JSON、未点名 F1/F2 键名。本文件是隔离红线的下位机审棘轮清单（细化，不是添加）。

---

## 2. 取证块（类型 2 / 4）

环境：主工作区含未发布 V427 及其它模块暂存，**不作扫描器证据**（D-028）。证据在 origin/main 一次性 worktree + 拷入的主工作区现行副本。

```text
git worktree add --detach /home/james/wt-m030-b5r1-grok-ro origin/main
ln -s /home/james/complex-management/node_modules /home/james/wt-m030-b5r1-grok-ro/node_modules
cp <主工作区本文件> <worktree>/security-scans/tenant-isolation-baseline.json
cd /home/james/wt-m030-b5r1-grok-ro
node tools/verify-tenant-isolation.mjs          # DEFAULT_EXIT=0
node tools/verify-tenant-isolation.mjs --strict # STRICT_EXIT=1
git -C /home/james/complex-management worktree remove --force /home/james/wt-m030-b5r1-grok-ro
```

worktree HEAD = `d215fde0`。拷入副本 SHA256 = `65384ebe6101a762b58c0a382fcf2a3e20cb2b14a8ea81898860e78973855da2`。扫描器为 origin/main 已合并的 V0.01（含 `IS [NOT] DISTINCT FROM` 中和）。

默认路径关键输出：

```text
[tenant-isolation] 扫描范围：
  - 权威表清单（CSV）：335 表（direct=173，白名单豁免=82，inherited 为信息性盲区）
  - Jdbc*Repository 源：53 个文件
  - 本次扫出 finding：2（已登记 baseline：2，新增回归：0）
[tenant-isolation] PASS（棘轮）：无新增回归；2 条已知缺口在台账跟踪闭环中（见 findings-register-v1.md tenant-isolation 小节）。隔离面未劣化。
DEFAULT_EXIT:0
```

**没有**「baseline 中 N 条已不再复现」提示（B1 时该提示为 2 条，正是 M030-001 的可失败信号）。

`--strict` 关键输出（阻断的恰好 2 条，无 F2、无 `access_grant_requests`）：

```text
[tenant-isolation] FAIL: strict 模式， 2 项租户隔离问题（fail-closed，阻断合入）：
  - [F1 UNSCOPED_DIRECT_READ] 表 'integration_channel_store_map'（direct, scope=merchant_id）在 backend/src/main/java/com/complex/platform/integration/JdbcIntegrationRepository.java 中有 SELECT 读，但找不到任何带 'merchant_id = :' 的 scoped 查询 —— 服务端无法按 token scope 过滤该表，存在跨租户读越权风险（AC-ISO-1，§4）。
  - [F1 UNSCOPED_DIRECT_READ] 表 'revenue_stream_attribution'（direct, scope=project_id）在 backend/src/main/java/com/complex/platform/settlement/JdbcSettlementConfigWriteRepository.java 中有 SELECT 读，但找不到任何带 'project_id = :' 的 scoped 查询 —— 服务端无法按 token scope 过滤该表，存在跨租户读越权风险（AC-ISO-1，§4）。
STRICT_EXIT:1
```

origin/main 已发布 baseline 仍含 4 key（含两条已删陈旧键）。拷入的现行副本 2 key：`F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution`、`F1:UNSCOPED_DIRECT_READ:integration_channel_store_map`。

CSV（origin/main 与主工作区同两行）：`revenue_stream_attribution,direct,project_id`；`integration_channel_store_map,direct,merchant_id`。

仓储（`git show origin/main:…`，类型 4）：

- `JdbcSettlementConfigWriteRepository.java:688-693`：`SELECT … FROM revenue_stream_attribution WHERE is_draft = TRUE AND status = 'active'`，无 `project_id = :`。同文件有 `c.merchant_id = :merchant_id`（:387），无 `project_id = :`，故扫描器对该表 F1 成立。
- `JdbcSettlementInvoiceRepository.java:13` 仅注释提到该表名（扫描器剥注释）；无 `FROM revenue_stream_attribution`。
- `JdbcIntegrationRepository.java:302-340`：`listChannelStoreMaps` 为 `FROM integration_channel_store_map ORDER BY … LIMIT 200`（无商户谓词）；另三条按 `id` / 自然键。全文件无 `merchant_id = :` / `merchant_id IN (` SQL 谓词（INSERT bind `.addValue("merchant_id", …)` 不是扫描器所认谓词）。
- `JdbcAccessRepository.java:766`：`request.project_id = :organization_id` 仍在；`:747` 仍有按 id 的无范围读（扫描器头注 B3 盲区）。故 `F1:…:access_grant_requests` 不再被发射——B3 删除该键后默认路径不再提示陈旧。

`package.json`：`verify:tenant-isolation` = `node tools/verify-tenant-isolation.mjs`（不带 `--strict`）；该脚本名出现在 `verify:delivery-gates` 与 `verify:predeploy`。

核完 worktree 已删除；主工作区本文件 SHA 未变。未跑 `--write-baseline`。未跑全链 `verify:predeploy`（B5 对本 JSON 的可执行声称是扫描器默认/`--strict`；全链属 B3 证据，本席不把未当场重跑的全链当本票依据）。

---

## 3. 第一性原理三问（2.3）

### 3.1 这段东西为什么必须存在？删掉它，三份 HTML 哪一条做不到？

本文件是 `verify:tenant-isolation` 的已知缺口棘轮底座。删掉整个文件：扫描器 L263 `existsSync` 为假 → `baselineKeys` 空 → 现行 2 条 F1 全部变成「新增回归」→ 默认路径 exit 1，delivery-gates/predeploy 红。HTML 并不点名本 JSON；上位是 BSM:707 / PGM:528+653 / BEQR:235 的项目/商户隔离红线。闸口本身在 M029 工具里；本文件让「已发现、尚未修」的缺口不阻断合入，同时挡住清单外新增。答得上业务上位 → **细化，不是添加**。答不上「HTML 逐字要求这一份 JSON」≠ 该删。

### 3.2 声称的行为有没有被实际执行验证过？

| 声称（本文件原文） | 验证 |
|---|---|
| 闸口对清单之外的新增 finding 硬失败 | **已验证**（类型 4）：本次新增回归 0，默认 exit 0；代码 L288+308。`--strict` 把同一 2 条当阻断，证明键集合 = 现行 finding 集合 |
| 本清单只登记扫描器已发现、尚未收紧的项 | **现成立**：2 个 key 均在 `--strict` 输出中复现；默认路径无 stale 提示 |
| 指向 generated_at_source | 字段存在，值与扫描器 `--write-baseline` 内置串相同。不是时间戳、不是文件:行、不是一次扫描日志。B2 已定为只登记，无新证据不改判 |
| 不得把缺口写成已修复或豁免 | 现行两条 `text` 均写「尚未收紧」，未写已修复/豁免 |
| 收紧后从本清单移除 | 过程句。B3 已对不再复现的两键执行；留下的两键 `--strict` 仍复现，不应移除 |
| 重建：`node tools/verify-tenant-isolation.mjs --write-baseline` | 命令在扫描器 L44/L273–284 真实存在；本轮按 D-028 **未执行** |
| 两条 `text` 的「扫描器报…尚未收紧」 | 与 `--strict` 现行两条 F1 一致（表名、项目/商户范围与 CSV `scope_source` 一致） |

### 3.3 哪个前提未经验证就当真？

- 「全部否 = 文件已全对」——本席不当真；用默认/`--strict` 差集当场核。
- 把 `generated_at_source` 当成可复核的一次扫描定位符——它只是静态方法串。B2 只登记，无新证据。
- 把「活键也应删掉逼修仓储」当真——会把仍存在的扫描器 finding 变成新增回归，改变闸口策略，超出本文件「登记已发现尚未收紧项」的职责；首次正常跑闸口并不要求本模块去改 Jdbc。不为该理论场景加需求。
- 主工作区直接跑扫描器会因 V427 红——D-028 已声明，不作本模块缺陷。

---

## 4. 顺从式：18 项判据逐项（定义已全文读自 lock `## general`）

每项先抄适用结论，再给本面证据或无本面理由。不新增判据。

### GEN-001 必要性与最小性 — **适用**

棘轮清单有可验证用途（挡住清单外新增、暂存已知 F1）。B3 已收窄两条不可达键。留下的 2 键均被 `--strict` 复现，再删会改变闸口策略。当前无重复/不可达键。满足。

### GEN-002 可执行正确性 — **适用**

声明的默认入口 `node tools/verify-tenant-isolation.mjs` / `npm run verify:tenant-isolation` 可执行（DEFAULT_EXIT=0）。`note` 与两条活 `text` 与现行扫描器输出一致。满足。

### GEN-003 测试有效性 — **适用**（本面是棘轮断言集，不是测试源码）

本模块无测试文件。本 JSON 的 `key` 就是扫描器默认路径「允许失败」的断言集合；`--strict` 是能失败的全量断言。2 活键仍能让 `--strict` 红（STRICT_EXIT=1）；默认路径不因它们红（棘轮语义）。扫描器运行即执行目标行为。满足。

### GEN-004 信任边界校验 — **无本面**

本文件是仓库内受控 JSON，不接受用户/网络输入。解析与 fail-closed 在扫描器（M029），不在本清单。

### GEN-005 授权与最小权限 — **无本面**

不实现授权；不发放权限。两条活 F1 描述的是他模块仓储缺 scoped 谓词，本文件只登记扫描器键。不在本模块发明「必须逐语句 scope」或「必须立刻修仓储」。

### GEN-006 数据完整性 — **无本面**

无状态转换、无事务、无金额。

### GEN-007 并发与幂等 — **无本面**

无并发路径。

### GEN-008 接口与模式一致性 — **适用**

键格式与扫描器发射式一致：`F1:UNSCOPED_DIRECT_READ:<table>`。顶层形状与扫描器 `--write-baseline` payload 三键相同（`note`/`generated_at_source`/`known_findings`）。活键的「无项目范围 / 无商户范围」与 CSV `scope_source`（`project_id` / `merchant_id`）一致。JSON.parse 成功。满足。

### GEN-009 错误处理与恢复 — **无本面**

JSON 坏了由扫描器 L267–269 FATAL exit 1。本文件当前可解析。本面无独立错误路径。

### GEN-010 安全与敏感信息 — **适用**

本文件是隔离机审的回归底座。当前 key 集合 = 现行 finding 集合：同键回归仍被当作已知（这是棘轮的设计，不是豁免孔误留）。无秘密、无注入面。满足。

### GEN-011 可观测与审计证据 — **适用**

质量检查记录应可关联、可复现。两条活键可由 `--strict` 定位到具体 Repository + scope 列。`generated_at_source` 仍不是一次扫描定位符——B2 只登记，无新证据不立项。满足。

### GEN-012 资源与性能边界 — **无本面**

14 行静态清单，无循环/连接/缓存。

### GEN-013 构建配置与依赖 — **适用**

被 npm 交付/预发布闸口默认调用。依赖扫描器 + CSV + Jdbc*Repository 同时存在；本文件自身无未声明本机状态。路径写死相对 cwd。满足。

### GEN-014 数据库迁移正确性 — **无本面**

不是迁移。

### GEN-015 界面可用性与可访问性 — **无本面**

无 UI。

### GEN-016 文档可验证性 — **适用**

`note` 与每条 `text` 都是当前事实声称。逐句核验见 §6：与扫描器现行输出和 origin/main 源码一致。重建命令路径可从扫描器复现。中性 `text` 与扫描器长文不是逐字相同——B2 已把 PR #446 中性化登记为随检查点带入，且活键声称（扫描器报 / 无项目或商户范围读 / 尚未收紧）可被 `--strict` 复现，不构成把旧证据写成当前事实。满足。

### GEN-017 业务来源分类 — **适用**

本文件整体：冲突？与三 HTML 明文无矛盾（未宣称「已隔离」或「允许关隔离开关」）。能指向上位 HTML 隔离红线 → **细化**。不是业务发明（无新实体/枚举/流程）。001 已修；本轮无新分类项。满足。

### GEN-018 第一性原理证据 — **适用**

本报告结论落到：HTML 原文（类型 1）、扫描器可执行输出与 `git show` 源码（类型 2/4）。不把 B2/B4 票、席位意见或「历史豁免」当依据。满足。

**18/18 已逐项记录。** 映射无 HTML 派生 ID，无漏项。不新增判据。

---

## 5. 对抗式（grilling 结构自问自答）

事实自读，不问人、不派子代理。

❓ **Q1 — M030-001 是否只是「清单变短」而棘轮仍对已消失的键留豁免孔？** 默认路径本次 **0 条 stale**、**2 已登记 / 0 新增**。已删 key `F1:UNSCOPED_DIRECT_READ:access_grant_requests` 与 `F2:UNREGISTERED_TABLE:d` 不在现行 `known_findings`。若这两键回归，将落入 `newFindings` 而默认路径红。首次正常跑默认闸口即可看见：B1 的 stale 提示消失。

➡️ 001 落地，不是表面删行。

❓ **Q2 — 清单里每个键现在是否仍被扫描器复现？有没有现行 finding 不在清单里？** `--strict` 只红上述 2 条 F1，key 与清单逐字相同。默认输出「本次扫出 finding：2（已登记 baseline：2，新增回归：0）」。差集两边皆空。

➡️ 无漏登记、无陈旧残留。

❓ **Q3 — 活键是否其实已经收紧、text 在撒谎？** origin/main SQL：`FROM revenue_stream_attribution WHERE is_draft = TRUE AND status = 'active'` 无 `project_id = :`；`listChannelStoreMaps` 对 `integration_channel_store_map` 无 `merchant_id = :`。CSV 两表仍为 direct + 对应 scope 列。`--strict` 稳定复现。

➡️ text「扫描器报 / 尚未收紧」成立。

❓ **Q4 — 应否把两条活 F1 从本清单拿掉，让默认闸口红、逼修仓储？** `--strict` 仍稳定复现它们。本模块是质量检查记录，不是去修 `JdbcIntegrationRepository` / `JdbcSettlementConfigWriteRepository`。拿掉活键会把仍存在的扫描器 finding 变成「新增回归」，改变闸口策略，超出本文件「登记已发现尚未收紧项」的职责。要两三件不巧同时发生才「必须本模块去修仓储」——提示词禁止为理论场景加需求。

➡️ 活键保留；不在本模块修仓储。不立项。

❓ **Q5 — 中性 text 与扫描器长文（含「跨租户读越权风险」与文件路径）不一致，是否文档假完成？** 扫描器运行时 **不读 text**（只读 key）。活键 text 声称的可验证部分（表名、无项目/商户范围读、尚未收紧）与 `--strict` 一致，没有写成已修复。B2 已把 PR #446 中性化定为只登记、随检查点带入。无新证据。

➡️ 不把中性风格当缺陷。

❓ **Q6 — 用 `--write-baseline` 一键重建是否更「正确」？** 它会把两条活 `text` 写回扫描器长文、把 `note` 写回「每条须在 findings-register 有 SEC-*-TENANT-ISOLATION-*」——现行台账并无与这两表一一对应的 BATCH id。B2 已把该内置旧 note 定为只登记。本轮禁止跑该开关。

➡️ 不跑；不立项。

❓ **Q7 — JSON 形状、缺字段、重复 key、非法 JSON？** `json.loads` 成功；三顶层键；两对象均 `key`+`text`；无重复 key；有末尾 `\n`。扫描器解析不会 FATAL。

➡️ 形状无缺陷。

❓ **Q8 — `generated_at_source` 不是扫描定位符、安全台账「收紧至 1 条 / access_grant_requests 保留 baseline」过期，是否本轮新缺陷？** 字段值未变；台账/生成器不在本模块 FILES。B2 明确只登记。无新证据。

➡️ 维持只登记。

❓ **Q9 — origin/main `:747` 仍有按 id 的无范围读，扫描器却因 `:766` 的 scoped 谓词不再报 F1，是否本清单漏登？** 扫描器 F1 定义是「该表在某 Repository **没有任何一条**带 scope 谓词的读」（文件级近似，头注 B3）。`:766` 使 `scopedSomewhere=true`，F1 按工具自己的定义不发射。要求逐调用点登记 = 新判据。B2 已观察。

➡️ 不立项。

❓ **Q10 — 默认 PASS 文案「2 条已知缺口在台账跟踪闭环中（见 findings-register-v1.md tenant-isolation 小节）」是否本文件撒谎？** 该句是扫描器 L324–328 打印的，不是本 JSON 的 `note`/`text`。本文件 note 已不再要求 SEC-*-TENANT-ISOLATION-* 一一对应。扫描器文案属 M029。

➡️ 不归本模块。

---

## 6. 全文覆盖（14 行，不抽样）+ 逐句声称

| 行 | 内容 | 覆盖结论 |
|---:|---|---|
| 1 | `{` | 合法对象起 |
| 2 | `note` | 见下表逐句 |
| 3 | `generated_at_source` | 与扫描器 writeBaseline 同串；非时间、非日志路径。B2 观察维持 |
| 4 | `"known_findings": [` | 数组；扫描器 `?? []` 可容忍缺省，本文件未缺 |
| 5–8 | key `F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution` + text | **仍复现**；见 text 逐句 |
| 9–12 | key `F1:UNSCOPED_DIRECT_READ:integration_channel_store_map` + text | **仍复现**；见 text 逐句 |
| 13 | `]` | 数组结束 |
| 14 | `}` | 对象结束 + 文件换行 |

无未读行。无 HTML 锚点遗漏（映射本无 HTML ID）。

### 6.1 note 逐句（L2）

| 句 | 原文 | 与扫描器现行输出 / 源码 |
|---|---|---|
| 1 | verify:tenant-isolation 已知缺口 baseline（棘轮底座） | 扫描器 L256–260 同语义。成立 |
| 2 | 闸口对此清单之外的新增 finding 硬失败 | 默认路径 `blocking = newFindings`（L308）；本次新增 0、exit 0。成立 |
| 3 | 本清单只登记扫描器已发现、尚未收紧的项，并指向 generated_at_source | 2 key 均被 `--strict` 复现。`generated_at_source` 字段存在。成立（定位符弱 = B2 观察） |
| 4 | 不得把缺口写成已修复或豁免 | 两条 text 均「尚未收紧」。成立 |
| 5 | 收紧后从本清单移除 | 陈旧两键已移除；活键未收紧故仍在。成立 |
| 6 | 重建：node tools/verify-tenant-isolation.mjs --write-baseline | 扫描器 L44/L273 入口真实存在。本轮未执行。成立 |

### 6.2 清单 key 差集

| 清单 key | 扫描器现行是否复现 | 结论 |
|---|---|---|
| `F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution` | 是（`--strict` 第 2 条） | 仍应留 |
| `F1:UNSCOPED_DIRECT_READ:integration_channel_store_map` | 是（`--strict` 第 1 条） | 仍应留 |
| （已删）`F1:UNSCOPED_DIRECT_READ:access_grant_requests` | 否 | 001 修复；回归将作为新增失败 |
| （已删）`F2:UNREGISTERED_TABLE:d` | 否 | 001 修复；回归将作为新增失败 |
| 清单外现行 finding | 无（新增回归 0） | 无漏登 |

### 6.3 text 逐句

**`revenue_stream_attribution` text**

| 句 | 原文 | 核对 |
|---|---|---|
| 1 | `[F1 UNSCOPED_DIRECT_READ] 扫描器报：表 revenue_stream_attribution 存在无项目范围的读` | `--strict` 发射同表 F1；CSV `scope_source=project_id`；仓储 WHERE 无 `project_id = :`。成立 |
| 2 | `尚未收紧` | `--strict` 仍红。成立 |
| 3 | `证据：generated_at_source` | 指向同文件字段。可定位证据实际是本次 `--strict` + `JdbcSettlementConfigWriteRepository.java:693`。B2 观察维持 |

**`integration_channel_store_map` text**

| 句 | 原文 | 核对 |
|---|---|---|
| 1 | `[F1 UNSCOPED_DIRECT_READ] 扫描器报：表 integration_channel_store_map 存在无商户范围的读` | `--strict` 发射同表 F1；CSV `scope_source=merchant_id`；`listChannelStoreMaps` 无 `merchant_id = :`。成立 |
| 2 | `尚未收紧` | `--strict` 仍红。成立 |
| 3 | `证据：generated_at_source` | 同上。B2 观察维持 |

---

## 7. 本文件被哪些工具或文档消费

扫描器只读 `known_findings[].key`，不读 `text`/`note`/`generated_at_source`。

| 消费者 | 路径 | 怎么用 | 改本文件会怎样 |
|---|---|---|---|
| 隔离机审（本棘轮的执行者） | `tools/verify-tenant-isolation.mjs:40,263-290,308` | `JSON.parse`；key 集合做差集；清单外 finding 默认硬失败；`--strict` 忽略清单 | 删活键 → 默认路径红；加未复现键 → stale 提示；坏 JSON → FATAL exit 1 |
| npm 默认闸口 | `package.json` `verify:tenant-isolation` / `verify:delivery-gates` / `verify:predeploy` | 调上述脚本，不带 `--strict` | 同上 |
| 安全基线生成器 | `tools/generate-formal-security-baseline.mjs:29,125,182` | `readOptional`；`tenantIsolationBaselineReady = trim().length > 0`。本席未找到该布尔的其它引用 | 只要文件非空，生成器对该文件内容不敏感。B2 观察维持 |
| 生成的安全台账 | `security-scans/findings-register-v1.md:26-27`、`formal-security-baseline-v1.md:146` | 点名本文件路径；`:27` 仍写「access_grant_requests 保留 baseline…收紧至 1 条」 | 过期描述属台账/生成器模块。B2 只登记，无新证据 |
| 隔离规格 | `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` §6A | 描述闸口与台账链路，不解析本 JSON 字段 | 不因本文件 2 键/4 键而变 |
| 本模块计划 | `plan/modules.md` M030 FILES | 唯一纳入路径 | — |

不在本模块改扫描器、仓储、台账、生成器、规格。

---

## 8. ponytail-review（只找可删，不修）

`Lean already. Ship.`

活的 2 个对象都是 `--strict` 仍发射的 key；再删会让默认闸口对真实缺口红，不是死代码。`note` / `generated_at_source` 被 `--write-baseline` 契约钉死三键形状，不能删。net: -0。

---

## 9. 临时 finding / 观察

### 临时 GR（工程缺陷成立）

**无。** 全模块审完未发现须入队的新缺陷；M030-001 的可失败信号（stale 提示 + 陈旧 key）已消失，未回归。

### 观察（不立项；B2 已定只登记，无新证据不改判）

1. 扫描器运行时不读 `text`/`note`/`generated_at_source`，只读 key。
2. `--write-baseline` 内置旧 `note` 要求 findings-register 的 SEC-*-TENANT-ISOLATION-* 一一对应；现行台账无与这两表一一对应的 id。重建命令可作为文档存在，不要当修复工具。
3. `generated_at_source` 不是 generated_at，不能当一次扫描的定位符。活键用 `--strict` 输出即可定位。
4. `findings-register-v1.md:27` / 生成器 L304-310「access_grant_requests 保留 baseline」「收紧至 1 条」与本文件 2 键现状不符。该文件不在 M030 范围。
5. `generate-formal-security-baseline.mjs` 只检查本文件非空；改 key/text 不会单独让 `verify:security-baseline` 因本文件内容红。
6. origin/main `JdbcAccessRepository.java:747` 仍有按 id 的无范围读。扫描器 B3 盲区（不逐调用点）。不在本模块发明「必须逐语句 scope」的新需求。
7. 主工作区相对 origin/main 仍含 PR #446 中性化差异；B2 登记随检查点带入。
8. 未在主工作区直接跑扫描器作本模块证据（V427 等暂存会使 F2 新增，属 D-028）。
9. 扫描器默认 PASS 文案指向 findings-register「tenant-isolation 小节」；该文案是扫描器打印，不是本 JSON 声称。

无添加项，无 HTML 修改申请，无 questions 草稿。不为理论场景补需求。

四分类（本文件整体，顺序走完）：冲突？否。细化（上位 HTML 隔离红线）。不进技术性/添加。

---

## 10. 步骤结论

| 检查 | 结果 |
|---|---|
| 范围文件全文覆盖 | 1/1（14 行，不抽样） |
| 映射 18 项逐项适用/无本面 | 18/18 |
| 三线 | 第一性 / 顺从 / 对抗均落盘 |
| JSON 形状 | 合法；三顶层键；2 对象 key+text |
| 清单 key vs 扫描器现行 | 2/2 仍复现；清单外现行 finding 0；已删 2 键不再复现 |
| note/text 逐句 | 与扫描器现行输出和 origin/main 源码一致（中性措辞/定位符弱维持 B2 观察） |
| M030-001 | 落地：默认无 stale 提示；同键回归会作为新增失败 |
| 临时 GR（工程缺陷成立） | 0 |
| 未答 questions | 0 |
| 四类证据 | 类型 1（HTML 原文）+ 类型 4（worktree 命令输出 / git show） |
| 审计结论 | **PASS** |

B5-r1 结束。不修复、不改 findings/ledger。不覆盖任何前轮 raw。本席未读本次席甲 self-raw 正文。
