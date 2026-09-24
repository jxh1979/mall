CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M030 B1 Grok 席乙完整报告

- 版本：V0.01
- 步骤：B1（修复前全模块三线全审；本模块首次开工，本版历史未审；PENDING→IN_PROGRESS）
- 席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区 HEAD `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` `d215fde0b51a0ca3121cf3a3145e1ec31f12621a`（M029 检查点 PR #470 已合并）。主工作区与 `origin/main` 分叉（ahead 49 / behind 38），含未发布 V427 及其它模块暂存；**审计对象是本文件的主工作区现状**，扫描器实跑按 D-028 在 origin/main 一次性 worktree 中进行
- 模块：`plan/modules.md` V0.04「租户范围与数据隔离（技术不变量）／质量检查记录／第1段」；标记 NORMAL；计划有效内容 22 行；实算 **22 行 / 1431 bytes**
- 判据锁：`plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（未抄 sidecar / ledger / 其它报告）。sidecar `plan/criteria-lock.sha256` 与 ledger 顶部同值，仅作交叉观察，不代替本席实算
- 映射 18 项：`M030 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]`（无 HTML 派生条目）
- findings.md / ledger：只读背景；ledger 记本模块 PENDING→IN_PROGRESS、B3 累计 0、本版无已定稿 M030 finding ID。本席不改计划 / findings / ledger
- 独立性：未打开、未通读、未采信 `M030-b1-self-raw.md`；未用 MCP / 子代理 / 第三席；未改业务代码 / 三 HTML / 锁 / 台账；未 fetch/commit/push/merge/deploy；除一次性 worktree 外未建脚本、日志或临时审计产物；未对任何数据库执行语句（本模块无库内核验必要）；未带 `--write-baseline`
- `test -f .git/MERGE_HEAD` → **NO_MERGE**
- 结论：**FAIL**（1 文件全文覆盖、18 项逐项核对、三线完成；临时 GR 项 1，工程缺陷成立；无未答 questions。有待补项不得 PASS）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md。用户指令禁止子代理 / 第三席 / MCP，优先于 Arc 默认全量派席。grilling 本体「等用户回答 / 派子代理查事实」按提示词作废，只借追问结构自问自答。

| 技能 | 存在性 | 本轮用法 |
|---|---|---|
| using-superpowers | 已读 | 开工技能路由；用户指令优先于技能默认 |
| using-arc | 已读 | 用户禁止子代理/第三席，不走 Arc `audit` 派评审席，不写 `docs/arc/audits/` |
| graft | 已读并调用 | `graft grep tenant-isolation-baseline`（2 文件/5 hit）；`graft ask` 棘轮/消费方。JSON 与 docs 不在 graft 索引内，随后对 docs/package.json/findings-register 用仓库 grep。本模块强制全文实读 JSON，不抽样 |
| long-running-background-tasks | 已读 | sha256 / node 扫描 / worktree add-remove 均前台完成，未挂 monitor |
| grilling / grill-me | 已读 | 只借追问结构自问自答，事实自读，不问人、不派子代理 |
| batch-grill-me | **缺口** | 已并入 grilling；不冒充调用 |
| ubiquitous-language | **缺口** | 已并入 domain-modeling；§6 已划掉 domain-modeling，不冒充调用 |
| codebase-design | 已读 | 本文件是浅 JSON 清单 + 深不变量（棘轮键集合）；缺陷落在「声称仍被扫描器发现」与现行输出不一致 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现；B1 禁止修复 |
| tdd / test-driven-development | 已读 | 用来判断棘轮断言能否因目标行为变坏而红；本席不写测试 |
| verification-before-completion | 已读 | 先跑 origin/main worktree 默认/`--strict`、再对源码/CSV/HTML 下结论 |
| receiving-code-review | 已读 | 禁止表演性同意；本轮无对方 raw 可收 |
| requesting-code-review | 存在，B4 用 | B1 不适用（禁止派第三席） |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → NO_MERGE |
| git-guardrails-claude-code | 可选存在 | 只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B1 不修；最小处置见 GR-01（删两个数组元素，不跑 `--write-baseline`） |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B1 不适用 |
| i-have-adhd | 存在 | 本席正文只落 raw；CLI 不回带结论或机器标记 |
| audit-context-building | 已读 | 要求派 function-analyzer 子代理：与「只允许两席」冲突，**不执行**，记缺口 |

graft 本轮有储蓄行的调用：`tenant-isolation-baseline` grep ≈11,326；棘轮/消费方 ask ≈26,360。合计约 **37,686** tokens。

一次性 worktree：`git worktree add --detach /home/james/wt-m030-b1-grok-ro origin/main`（HEAD `d215fde0`），软链 `node_modules`，把主工作区 `security-scans/tenant-isolation-baseline.json` 拷入后跑默认与 `--strict`，`git checkout --` 恢复 origin 副本，`git worktree remove --force`；目录已不存在；未带 `--write-baseline`。

---

## 1. 模块覆盖（全文实读，行数/SHA 实算）

命令：`wc -l -c` + `sha256sum` + `git hash-object` + `git rev-parse HEAD:<path>` + `git rev-parse origin/main:<path>` + `python3 json.loads`。

| 路径 | 物理行 | 字节 | SHA256 | git blob（工作区=HEAD） | origin/main blob |
|---|---:|---:|---|---|---|
| `security-scans/tenant-isolation-baseline.json` | 22 | 1431 | `7c178f9640b3bd3010cdcc3af94a258643d2db9e9847b0def3a7e7b84459df15` | `f75a02636ae88ac16e0abcd407a80be4985d4b23` | `931b651781f2a005190e0ecc2ca845c6fa2b3a8f` |

覆盖：**1/1 全文逐行，不抽样**。合法 JSON；末尾换行；顶层键恰为 `note` / `generated_at_source` / `known_findings`；`known_findings` 4 个对象，每项恰有 `key`+`text`。

相对 `origin/main`：`git diff origin/main -- security-scans/tenant-isolation-baseline.json` → **5 行变更**（note 1 句 + 4 条 `text`；**4 个 `key` 未变**）。来自主工作区未合并本地提交：

- `66bc70a4` 2026-09-11 `audit M030: isolation baseline lists open gaps, not exemptions`
- `14180777` 2026-09-11 `audit M030: known findings stay open scanner reports only`

对应 OPEN PR **#446**（`audit-m030-r1`，title `audit M030: isolation baseline open gaps`，`gh pr view 446` state=OPEN）。审计对象是中性表述后的主工作区副本，不是 origin/main 上仍写「误报豁免 / 平台级有意读」的旧文。

相关三 HTML（只读，铁律 4 未改；本模块映射无 HTML 派生 ID，HTML 只作第一性原理类型 1 与四分类上位）：

- `docs/business-system-map-for-operations-v0.1.html:707` 红线④原文：「看不到别的商户和别的项目的数据」；同条：七类校验不设关闭开关
- `docs/platform-governance-map-for-operations-v0.1.html:653`：「所有业务数据默认按项目隔离……数据的项目与商户隔离不提供关闭开关」
- `docs/business-enum-quick-reference-v0.1.html:235` 锚点「项目与敏感数据权限」：「所有数据默认按项目隔离，普通岗位不得跨项目查看」

HTML 未点名本 JSON、未点名 F1/F2 键名。本文件是隔离红线的下位机审棘轮清单（细化，不是添加）。

---

## 2. 取证块（类型 2 / 4）

### 2.1 D-028 证据环境

```
git worktree add --detach /home/james/wt-m030-b1-grok-ro origin/main
# HEAD d215fde0 Merge pull request #470 … m029-checkpoint
ln -s /home/james/complex-management/node_modules …/node_modules
# origin 副本 SHA256 6b7d6d0236d45367230fc47d85ca32f356de26b01735f9b943eae4c80fdc808f
cp <主工作区本文件> <worktree>/security-scans/tenant-isolation-baseline.json
# 拷入后 SHA256 7c178f9640b3bd3010cdcc3af94a258643d2db9e9847b0def3a7e7b84459df15
# 未带 --write-baseline
```

worktree 内工具/CSV/Java 为 origin/main（无未发布 V427）；清单为本模块主工作区 4 键中性文本。`tools/verify-tenant-isolation.mjs` 工作区相对 origin/main **无 diff**（含 M029-002 的 `IS [NOT] DISTINCT FROM` 中和）。

### 2.2 默认模式（即 `npm run verify:tenant-isolation` / delivery-gates / predeploy 所用）

```
node tools/verify-tenant-isolation.mjs
→ 扫描 CSV 335 表（direct=173，白名单豁免=82）；Jdbc*Repository 53 文件
→ 本次 finding 2（已登记 baseline 2，新增回归 0）
→ stale baseline 2 条：
    · F1:UNSCOPED_DIRECT_READ:access_grant_requests
    · F2:UNREGISTERED_TABLE:d
→ PASS（棘轮）：无新增回归；2 条已知缺口在台账跟踪闭环中
DEFAULT_EXIT:0
```

### 2.3 `--strict`（忽略 baseline，对现行 finding 硬失败）

```
node tools/verify-tenant-isolation.mjs --strict
→ 同上扫描面；同样 stale 提示 2 条
→ FAIL: strict 模式，2 项：
  - [F1 UNSCOPED_DIRECT_READ] 表 'integration_channel_store_map'（direct, scope=merchant_id）
    在 backend/src/main/java/com/complex/platform/integration/JdbcIntegrationRepository.java
    中有 SELECT 读，但找不到任何带 'merchant_id = :' 的 scoped 查询
  - [F1 UNSCOPED_DIRECT_READ] 表 'revenue_stream_attribution'（direct, scope=project_id）
    在 backend/src/main/java/com/complex/platform/settlement/JdbcSettlementConfigWriteRepository.java
    中有 SELECT 读，但找不到任何带 'project_id = :' 的 scoped 查询
STRICT_EXIT:1
```

核完 `git checkout -- security-scans/tenant-isolation-baseline.json` 恢复 origin 副本（SHA256 回到 `6b7d6d03…`），`git worktree remove --force`；`test ! -d /home/james/wt-m030-b1-grok-ro` 成立。主工作区本文件未被扫描器改写。

### 2.4 扫描器如何消费本文件（origin/main = 现行源码）

`tools/verify-tenant-isolation.mjs`：

- L40 路径钉死 `security-scans/tenant-isolation-baseline.json`
- L42 `--strict`：忽略 baseline，全部 finding 硬失败
- L44 `--write-baseline`：用**当前** finding 覆盖本文件（本轮未跑）
- L265–266 **只取 `known_findings[].key` 进 Set**；`note`/`text`/`generated_at_source` 运行时不参与判定
- L288–290 `newFindings` = 现行 finding 键不在 baseline；`staleBaseline` = baseline 键不在现行 finding
- L302–304 陈旧键只 `console.log` 提示「应从 baseline 与台账移除以收紧闸口」，**默认路径不失败**
- L308 `blocking = strict ? findings : newFindings` → 默认只对清单**之外**的新增硬失败

`package.json:95` `"verify:tenant-isolation": "node tools/verify-tenant-isolation.mjs"`（无 `--strict`）；该脚本在 `verify:delivery-gates`（约 L61）与 `verify:predeploy`（L96）链上。

### 2.5 四键与现行扫描器/源码对照

origin/main CSV：

| 表 | scope_type | scope_source |
|---|---|---|
| `access_grant_requests` | direct | project_id |
| `revenue_stream_attribution` | direct | project_id |
| `integration_channel_store_map` | direct | merchant_id |

`F2:UNREGISTERED_TABLE:d` 不是表。

| baseline key | 现行是否复现 | 本文件 text 声称 | 判定 |
|---|---|---|---|
| `F1:UNSCOPED_DIRECT_READ:access_grant_requests` | **否**（stale） | 「扫描器报：表 access_grant_requests 有 SELECT 读，未见 project_id = : 的 scoped 查询。尚未收紧。」 | **假**：扫描器已不报；origin/main `JdbcAccessRepository.java:766` 有 `request.project_id = :organization_id`，按扫描器文件级近似（`hasScopedRead`）即视为存在 scoped 通道 |
| `F2:UNREGISTERED_TABLE:d` | **否**（stale） | 「扫描器报未登记表名 d。尚未收紧。」 | **假**：origin/main 扫描器 L149–150 已中和 `IS [NOT] DISTINCT FROM`（M029-002），不再把其后别名 `d` 当表 |
| `F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution` | **是**（`--strict` 第 2 条） | 「扫描器报：表 revenue_stream_attribution 存在无项目范围的读。尚未收紧。」 | **真**（摘要成立；CSV scope=`project_id`） |
| `F1:UNSCOPED_DIRECT_READ:integration_channel_store_map` | **是**（`--strict` 第 1 条） | 「扫描器报：表 integration_channel_store_map 存在无商户范围的读。尚未收紧。」 | **真**（摘要成立；CSV scope=`merchant_id`） |

access_grant_requests 为何从 F1 消失（origin/main 源码，类型 4 的对照）：

```
JdbcAccessRepository.java:761  FROM access_grant_requests request
:766  AND (request.project_id IS NULL OR request.project_id = :organization_id)
```

同文件仍有按主键读、无 scope 谓词的 SELECT（`:747` `WHERE id = :id`）。扫描器头注 B3 写明不逐调用点核验、按「Repository 至少提供 scoped 通道」近似——这是扫描器口径（M029），不是本文件再发明的业务规则。本文件的问题是：**扫描器已不再产出该键，清单却仍写「扫描器报 / 尚未收紧」**。

### 2.6 消费方与改动牵动

| 消费方 | 怎么用本文件 | 改 key | 改 text/note |
|---|---|---|---|
| `tools/verify-tenant-isolation.mjs` 默认路径 | 键集合 = 允许暂存的缺口 | 删陈旧键 → 收紧；若该缺口回归则默认硬失败。加键 → 放宽 | 无运行时效果 |
| 同上 `--strict` | 忽略本文件 | 无 | 无 |
| 同上 `--write-baseline` | **覆盖**本文件 | 写成当前 finding 全集（本次会变成 2 条活键） | 用扫描器长 `text`，并把 `note` 写回带 findings-register 要求的旧稿 |
| `package.json` `verify:tenant-isolation` / delivery-gates / predeploy | 调默认路径 | 同默认路径 | 无 |
| `tools/generate-formal-security-baseline.mjs` L125, L182 | `readOptional`；`tenantIsolationBaselineReady = trim().length > 0` | 只要文件非空，布尔不变 | 内容不进判定 |
| `security-scans/findings-register-v1.md:26` / `formal-security-baseline-v1.md` | 文件名出现在 SEC-P0-TENANT-ISOLATION-001 证据句 | 不解析 JSON | 不解析 JSON |
| `findings-register-v1.md:27` SEC-P2-TENANT-ISOLATION-PROJECT-READ-001 | 叙述「access_grant_requests 保留 baseline」「收紧至 1 条」 | 该台账已过时，**不在本模块文件集合**，不立项 | — |

origin/main 旧 `text` 指向的 `SEC-P3-TENANT-ISOLATION-BATCH1-ALIAS-001` / `SEC-P2-TENANT-ISOLATION-BATCH1-PLATFORM-READ-001` / `SEC-P2-TENANT-ISOLATION-REFLOW-CHANNEL-MAP-001`：**当前 findings-register 无这些 id**（只搜到 P0-001 与 P2-PROJECT-READ-001）。主工作区中性 `text` 已改为「证据：generated_at_source」，不再指向不存在的 SEC 键。

`generated_at_source` 现值：`"static scan of Jdbc*Repository.java vs domain-ownership-scope-audit-v0.1.csv"`——方法说明，不是时间戳、不是文件:行、不是一次扫描日志。活键的可定位证据是本次 `--strict` 输出（类型 4）；陈旧键的「扫描器报」无法从现行输出定位。

---

## 3. 第一性原理三问（2.3）

### 3.1 这段东西为什么必须存在？删掉它，三份 HTML 哪一条做不到？

本文件是 `verify:tenant-isolation` 的已知缺口棘轮底座。删掉整个文件：扫描器 L263 `existsSync` 为假 → `baselineKeys` 空 → 现行 2 条 F1 全部变成「新增回归」→ 默认路径 exit 1，delivery-gates/predeploy 红。HTML 并不点名本 JSON；上位是 BSM:707 / PGM:528+653 / BEQR:235 的项目/商户隔离红线。闸口本身在 M029 工具里；本文件让「已发现、尚未修」的缺口不阻断合入，同时挡住清单外新增。答得上业务上位 → **细化，不是添加**。答不上「HTML 逐字要求这一份 JSON」≠ 该删。

### 3.2 声称的行为有没有被实际执行验证过？

| 声称（本文件原文） | 验证 |
|---|---|
| 闸口对清单之外的新增 finding 硬失败 | **已验证**（类型 4）：本次新增回归 0，默认 exit 0；代码 L288+308 |
| 本清单只登记扫描器已发现、尚未收紧的项 | **未成立**：4 键里 2 键扫描器已不复现，工具自己打印「已不再复现…应从 baseline 移除」 |
| 指向 generated_at_source | 字段存在；不是可定位的一次扫描产物 |
| 不得把缺口写成已修复或豁免 | 现行 `text` 未写「已修复/豁免」；陈旧两条改写成「扫描器报 / 尚未收紧」，方向相反的假完成 |
| 收紧后从本清单移除 | 过程句；陈旧两条未移除 |
| 重建：`node tools/verify-tenant-isolation.mjs --write-baseline` | 命令在扫描器 L44/L273–284 真实存在；本轮按 D-028 **未执行** |
| 四条 `text` 的「扫描器报…尚未收紧」 | 仅后两条与 `--strict` 现行输出一致；前两条为假 |

### 3.3 哪个前提未经验证就当真？

- 把 4 个 key 都当成「扫描器此刻仍会报」——已被默认/`--strict` 输出证伪（2 stale / 2 live）。
- 把 `generated_at_source` 当成可复核证据位置——它只是静态方法串。
- 把「陈旧键留着只是提示、无安全后果」当真——默认路径用 `baselineKeys.has(f.key)` 屏蔽同键 finding；陈旧键等于给该键留了一个**回归豁免孔**（见 GR-01 影响）。这不是理论推演：就是 L288 的现行语义，首次正常跑默认闸口即可看见 stale 提示仍 exit 0。

---

## 4. 顺从式：18 项判据逐项（定义已全文读自 lock `## general`）

每项先抄适用结论，再给本面证据或无本面理由。不新增判据。

### GEN-001 必要性与最小性 — **适用**

棘轮清单有可验证用途（挡住清单外新增、暂存已知 F1）。重复/已不可达的键应收窄。陈旧两键无当前扫描器承载，违反最小性。见 GR-01。

### GEN-002 可执行正确性 — **适用**

声明的默认入口 `node tools/verify-tenant-isolation.mjs` / `npm run verify:tenant-isolation` 可执行（DEFAULT_EXIT=0）。`note` 与两条陈旧 `text` 与现行行为不一致。见 GR-01。

### GEN-003 测试有效性 — **适用**（本面是棘轮断言集，不是测试源码）

本模块无测试文件。本 JSON 的 `key` 就是扫描器默认路径「允许失败」的断言集合；`--strict` 是能失败的全量断言。活键仍能让 `--strict` 红；陈旧键已不能作为现行 finding 出现，默认路径也不会因它们红。扫描器运行即执行目标行为。

### GEN-004 信任边界校验 — **无本面**

本文件是仓库内受控 JSON，不接受用户/网络输入。解析与 fail-closed 在扫描器（M029），不在本清单。

### GEN-005 授权与最小权限 — **无本面**

不实现授权；不发放权限。隔离读缺口的业务闸在仓储/服务，本文件只登记扫描器键。

### GEN-006 数据完整性 — **无本面**

无状态转换、无事务、无金额。

### GEN-007 并发与幂等 — **无本面**

无并发路径。

### GEN-008 接口与模式一致性 — **适用**

键格式与扫描器发射式一致：`F1:UNSCOPED_DIRECT_READ:<table>` / `F2:UNREGISTERED_TABLE:<token>`。顶层形状与扫描器 `--write-baseline` payload 三键相同。活键的「无项目范围 / 无商户范围」与 CSV `scope_source` 一致。陈旧键与现行扫描器输出不一致，归 GR-01，不另开接口项。

### GEN-009 错误处理与恢复 — **无本面**

JSON 坏了由扫描器 L267–269 FATAL exit 1。本文件当前可解析。

### GEN-010 安全与敏感信息 — **适用**

本文件是隔离机审的回归底座。陈旧键留下同键回归豁免孔（默认路径不会把该键当新增）。无秘密、无注入面。见 GR-01。

### GEN-011 可观测与审计证据 — **适用**

质量检查记录应可关联、可复现。活键可由 `--strict` 定位到具体 Repository；陈旧键的「扫描器报」与 `证据：generated_at_source` 无法从现行输出定位。见 GR-01；`generated_at_source` 命名弱、作观察。

### GEN-012 资源与性能边界 — **无本面**

22 行静态清单，无循环/连接/缓存。

### GEN-013 构建配置与依赖 — **适用**

被 npm 交付/预发布闸口默认调用。依赖扫描器 + CSV + Jdbc*Repository 同时存在；本文件自身无未声明本机状态。路径写死相对 cwd。

### GEN-014 数据库迁移正确性 — **无本面**

不是迁移。

### GEN-015 界面可用性与可访问性 — **无本面**

无 UI。

### GEN-016 文档可验证性 — **适用**

`note` 与每条 `text` 都是当前事实声称。两条陈旧 `text` 与 `note`「只登记扫描器已发现、尚未收紧的项」把旧证据写成当前事实。见 GR-01。重建命令路径可从扫描器复现。

### GEN-017 业务来源分类 — **适用**

本文件整体：冲突？与三 HTML 明文无矛盾（未宣称「已隔离」或「允许关隔离开关」）。能指向上位 HTML 隔离红线 → **细化**。不是业务发明（无新实体/枚举/流程）。GR-01 同为细化下的工程缺陷，不是添加。

### GEN-018 第一性原理证据 — **适用**

本报告结论落到：HTML 原文（类型 1）、扫描器可执行输出与 git show 源码（类型 2/4）。不把 PR #446 意图、席位意见或「历史豁免」当依据。

**18/18 已逐项记录。** 映射无 HTML 派生 ID，无漏项。

---

## 5. 对抗式（grilling 结构自问自答）

❓ **Q1 — 这文件是否根本不该存在？** 没有它，现行 2 条 F1 会让默认闸口红，合入被阻断；有它才是「已知缺口暂存 + 新增硬失败」。保留文件，收紧内容。

➡️ 不删文件；删陈旧键。

❓ **Q2 — 陈旧键会不会是有意豁免（扫描器误报 / 服务层闸）？** `note` 明文「不得把缺口写成已修复或豁免」。`F2:d` 在 M029-002 之后是扫描器误报的消失，不是表 `d`。`access_grant_requests` 按扫描器自己的 F1 定义已经不再命中。把它们留在「尚未收紧」清单里，既假又与 note 自我合同相反。findings-register:27 仍写「access_grant_requests 保留 baseline / 收紧至 1 条」——那是**另一文件的过时叙述**，不能反过来证明本清单应继续挂这两键。

➡️ 不是豁免；应移除。

❓ **Q3 — 只改 text、不删 key，能否过关？** 扫描器不读 text。把陈旧 text 改成「已不复现」而不删 key，默认路径仍给该键留回归豁免孔。首次正常照做（跑默认闸口）要收紧的是 **key 集合**。

➡️ 最小处置是删对象，不是改措辞。

❓ **Q4 — 后两条活 F1 是否也该从本清单拿掉（从而让默认闸口红，逼修仓储）？** `--strict` 仍稳定复现它们。本模块是质量检查记录，不是去修 `JdbcIntegrationRepository` / `JdbcSettlementConfigWriteRepository`。拿掉活键会把仍存在的扫描器 finding 变成「新增回归」，改变闸口策略，超出本文件「登记已发现尚未收紧项」的职责，也不是首次跑闸口就能叫做「本 JSON 写错」。

➡️ 活键保留；不在本模块修仓储。

❓ **Q5 — 用 `--write-baseline` 一键重建是否更小？** 它会：(1) 丢掉两陈旧键（好事）；(2) 把两条活 `text` 写回扫描器长文（含「跨租户读越权风险」），撤销主工作区已做的中性表述；(3) 把 `note` 写回「每条须在 findings-register 有 SEC-*-TENANT-ISOLATION-*」——而 findings-register 并无对应 BATCH1/REFLOW id。本轮禁止跑该开关；B3 最小处置也不该跑。

➡️ 手工删除两个 `known_findings` 元素。

❓ **Q6 — 陈旧键的回归豁免孔是不是理论场景？** 不是另造需求。现行代码 L288：`newFindings = findings.filter(f => !baselineKeys.has(f.key))`。今天跑默认闸口已经：stale 提示 + exit 0。这就是「首次正常照做」的复现。未来若有人删掉 `project_id = :` 或回退 DISTINCT FROM 中和，默认闸口对这两键仍然不红——这是该语义的直接推论，用来说明影响，不是单独立项理由。立项理由是：**清单自述与现行扫描器输出已经不一致**（GEN-002/016）。

➡️ 立项成立。

❓ **Q7 — JSON 形状、缺字段、重复 key、非法 JSON？** `json.loads` 成功；三顶层键；四对象均 `key`+`text`；无重复 key；有末尾 `\n`。扫描器解析不会 FATAL。

➡️ 形状无缺陷。

❓ **Q8 — 中性表述本身是否把「越权风险」抹成未完成扫描器报告，构成文档假完成？** 对**仍复现**的两条：text 只声称「扫描器报无范围读 / 尚未收紧」，与 `--strict` 一致，没有写成已修复。对**不复现**的两条：中性表述变成了假的「扫描器报」。问题在陈旧，不在中性风格。

➡️ 不把 PR #446 的中性化当缺陷。

---

## 6. 全文覆盖（22 行，不抽样）

| 行 | 内容 | 覆盖结论 |
|---:|---|---|
| 1 | `{` | 合法对象起 |
| 2 | `note` | 见 §3.2：第 2 句棘轮语义真；「只登记扫描器已发现、尚未收紧的项」假（GR-01）；禁止豁免句与陈旧 text 的「尚未收紧」冲突；重建命令真 |
| 3 | `generated_at_source` | 与扫描器 writeBaseline 同串；非时间、非日志路径。活键证据实际是本次扫描输出 |
| 4 | `"known_findings": [` | 数组；扫描器 `?? []` 可容忍缺省，本文件未缺 |
| 5–8 | key `F1:UNSCOPED_DIRECT_READ:access_grant_requests` + text | **陈旧**；text 每句（扫描器报 / 未见 project_id = : / 尚未收紧 / 证据 generated_at_source）均不能从现行扫描器输出复现。GR-01 |
| 9–12 | key `F2:UNREGISTERED_TABLE:d` + text | **陈旧**；text「扫描器报未登记表名 d。尚未收紧」假。GR-01 |
| 13–16 | key `F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution` + text | **仍复现**；「无项目范围」与 CSV `project_id` 及 `--strict` 文本一致 |
| 17–20 | key `F1:UNSCOPED_DIRECT_READ:integration_channel_store_map` + text | **仍复现**；「无商户范围」与 CSV `merchant_id` 及 `--strict` 文本一致 |
| 21 | `]` | 数组结束 |
| 22 | `}` | 对象结束 + 文件换行 |

无未读行。无 HTML 锚点遗漏（映射本无 HTML ID）。

---

## 7. 临时 finding

### GR-01 — 棘轮清单含两条扫描器已不再复现的键，note/text 仍写成「扫描器报 / 尚未收紧」

- **位置**：`security-scans/tenant-isolation-baseline.json:2`（note）；`:5-8`（`F1:UNSCOPED_DIRECT_READ:access_grant_requests`）；`:9-12`（`F2:UNREGISTERED_TABLE:d`）
- **原文**：
  - L2：`本清单只登记扫描器已发现、尚未收紧的项，并指向 generated_at_source。不得把缺口写成已修复或豁免。收紧后从本清单移除。`
  - L6：`"key": "F1:UNSCOPED_DIRECT_READ:access_grant_requests"`
  - L7：`"text": "[F1 UNSCOPED_DIRECT_READ] 扫描器报：表 access_grant_requests 有 SELECT 读，未见 project_id = : 的 scoped 查询。尚未收紧。证据：generated_at_source。"`
  - L10：`"key": "F2:UNREGISTERED_TABLE:d"`
  - L11：`"text": "[F2 UNREGISTERED_TABLE] 扫描器报未登记表名 d。尚未收紧。证据：generated_at_source。"`
- **更长上下文**：同文件 L13–20 两条活 F1 仍被 `--strict` 复现，说明清单机制本身可用；问题限于这两键未随扫描器/仓储现状收紧。扫描器 L302–304 对本次拷入的主工作区副本打印：`baseline 中 2 条已不再复现（疑似已修复），应从 baseline 与台账移除以收紧闸口：` 其后正是上述两 key。
- **正常触发 / 影响**：在基于 origin/main 的环境放入本文件，跑 `node tools/verify-tenant-isolation.mjs`（即 `npm run verify:tenant-isolation`，delivery-gates/predeploy 默认路径）。**可复现**：DEFAULT_EXIT=0，stale 提示 2 条，现行 finding 仅 2。后果：(1) 质量记录把未复现项写成当前扫描器报告；(2) 默认闸口继续把这两 key 当「已知」，同键回归不会作为新增失败（L288）。`--strict` 不受陈旧键拖累，仍只红两条活 F1。
- **最小拟处置**（ponytail）：从 `known_findings` **删除**上述两个对象，保留两条仍复现的 F1。不要跑 `--write-baseline`（会改写活键中性 text 和 note）。不要改扫描器、不要改仓储、不要改 HTML。删完后 `note`「只登记扫描器已发现、尚未收紧的项」即与现行 2 键清单一致，无需改 note。
- **四分类（顺序走完）**：
  1. 冲突？否。未与三 HTML「必须隔离 / 不得关隔离开关」的明文相反；也未宣称「已修好」。
  2. **细化**。上位：BSM:707 红线④「看不到别的商户和别的项目的数据」；PGM:653「数据的项目与商户隔离不提供关闭开关」；BEQR:235「所有数据默认按项目隔离」。本文件是该红线的机审棘轮清单。
  3. 技术性：不进入（已能指向上位 HTML）。
  4. 添加：否。无新业务概念。
- **工程缺陷是否成立**：**成立**
- **冻结 ID**：GEN-002、GEN-016、GEN-010（棘轮同键豁免孔）；分类依据 GEN-017
- **第一性依据类型**：**4**（可复现命令 + 输出：worktree 内 `node tools/verify-tenant-isolation.mjs` 与 `--strict`；辅以类型 2 扫描器 L266/L288/L302–308 与 origin/main `JdbcAccessRepository.java:766`、扫描器 L149–150）

---

## 8. 观察（不立项）

1. 扫描器运行时不读 `text`/`note`/`generated_at_source`，只读 key。假 text 单独不会让 CI 红；假的是「清单仍包含该 key」。
2. `--write-baseline` 会用扫描器内置旧 `note`（要求 findings-register 的 SEC-*-TENANT-ISOLATION-* 一一对应）覆盖主工作区中性 note；BATCH1/REFLOW 那些 id 在现行 findings-register **不存在**。故重建命令可作为文档存在，B3 不要用它当修复工具。
3. `generated_at_source` 不是 generated_at，不能当一次扫描的定位符。活键用 `--strict` 输出即可定位，不单独立项。
4. `findings-register-v1.md:27`「access_grant_requests 保留 baseline」「收紧至 1 条」与本文件 4 键现状及扫描器 2 stale+2 live 均不一致。该文件不在 M030 范围。
5. `generate-formal-security-baseline.mjs` 只检查本文件非空；改 key/text 不会单独让 `verify:security-baseline` 因本文件内容红。
6. 同文件仍有 `FROM access_grant_requests … WHERE id = :id` 的无 scope 读（origin/main `:747`）。扫描器 B3 盲区（不逐调用点）。不在本模块发明「必须逐语句 scope」的新需求。
7. 主工作区相对 origin/main 的 5 行差异全是 note/text 中性化，**未改变 4 个 key**。陈旧不是 PR #446 引入的，是后续仓储 scoped 谓词 + M029-002 扫描器中和使两键不再被发射，清单未收紧。
8. 未在主工作区直接跑扫描器作本模块证据（V427 等暂存会使 F2 新增，属 D-028 已声明的环境伪红）。

无添加项，无 HTML 修改申请，无 questions 草稿。

---

## 9. 步骤结论

| 检查 | 结果 |
|---|---|
| 范围文件全文覆盖 | 1/1 |
| 映射 18 项逐项适用/无本面 | 18/18 |
| 三线 | 第一性 / 顺从 / 对抗均落盘 |
| 临时 GR（工程缺陷成立） | 1（GR-01） |
| 未答 questions | 0 |
| 四类证据 | GR-01 为类型 4 |
| 审计结论 | **FAIL** |

B1 结束。不修复、不改 findings/ledger。

## M030 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **31502** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6/xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02。不加判据。
- 正本：`/home/james/complex-management` main 主工作区 HEAD `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` `d215fde0b51a0ca3121cf3a3145e1ec31f12621a`。`test -f .git/MERGE_HEAD` → NO_MERGE。
- 席甲：Claude Fable 5.1，作者自审票。本席只读 `M030-b1-self-raw.md` 前 **13545** 字节（V0.01 封存）；其后自「M030 B2-r1 整批候选复核」标题起的追加段不读、不采、不作本票依据。
- 本席 B2-r1：**PASS**（001 逐 ID PASS；两席其余观察全部只登记 PASS；添加 0、不发 2.2、无 HTML 申请、无 questions、不需更正段）
- 正文实算锁（非独占行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，内容同值。本段未新增独占锁哈希字段行。独占锁哈希字段行仍仅文首一处。
- 无 MCP、无子代理、无第三席。未改源码/文档/HTML/lock/findings/ledger；未 fetch/commit/push/merge/deploy；未对任何数据库执行写语句；未写本指定路径以外的任何文件；未重跑扫描器（不重新审模块）。

此票只判断 findings.md V2.94 顶部「当前 M030 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。不加判据。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M030-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 31502 | `f105b34a6f4b05e31cb3cb72463a157ea12c3cfe26c578497d22e5f1717f5d84` |
| `docs/audit-2026-09/modules/M030-b1-self-raw.md` 前缀（席甲 B1；本席只读此前缀） | V0.01 | 13545 | `420c3f8ab0051567a59fc655ea27709bd33cf849945b70b7c80baa9f9be82c6e` |
| `docs/audit-2026-09/findings.md`（全文件；只采顶部「当前 M030 B2-r1 整批候选」至该节结束） | V2.94 | 1104694 | `f80c4f2cec0c731b00ddc78b9bfd8abc98a647ddf8d445101bfc63803d94e67d` |

席甲 raw 全文现 14538 bytes；本席用前 13545 字节得与封存值相同的 SHA，其后自 B2 标题起不作为本票依据。封存前缀以席甲 B1「席甲 B1 票：**FAIL**」段结束。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

controller / runbook / modules.md 只读核验（本席未改）：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |

三 HTML 只读核验（本席未改；001 细化上位用业务地图 L707 与治理地图 L653；BEQR L235 为同义佐证）：

| 文件 | SHA256 |
|---|---|
| `docs/business-system-map-for-operations-v0.1.html` | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| `docs/business-enum-quick-reference-v0.1.html` | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

对象文件（未改；只核最小处置边界，不重审模块）：`security-scans/tenant-isolation-baseline.json` 22 行 1431 bytes SHA256 `7c178f9640b3bd3010cdcc3af94a258643d2db9e9847b0def3a7e7b84459df15`；合法 JSON，顶层键恰为 `note` / `generated_at_source` / `known_findings`，数组 4 对象。扫描器 `tools/verify-tenant-isolation.mjs` L265 `JSON.parse`、L273–282 `--write-baseline` payload 三键（本步只读该段，不跑扫描器）。`questions/M030*`：不存在。候选段止于「修复队列『否』（0）。」；其下他模块历史段不重审。写入集合＝本模块 1 文件（`plan/modules.md` FILES 仅此路径）。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核差集/分类/队列/最小边界再接受或反驳；禁止表演性同意；B2 不修复。本批重点：GR-01 与 CX-001 是否同一事实同一处置、其余观察是否漏并或应升队、细化上位句、JSON 不加版本标记 |
| grilling | 只借追问结构自查：活键是否该入队、台账是否该同批改、`--write-baseline` 是否更小、`//` 版本行会不会让 `JSON.parse` FATAL |
| verification-before-completion | 声称前先核两份 B1 前缀 SHA、findings V2.94 全文件 SHA、JSON 四键形状、扫描器 writeBaseline 内置 note、三 HTML 上位句、`questions/M030*` 不存在。不重跑扫描器 |
| systematic-debugging / diagnosing-bugs | 只借读错→对照两 raw 与候选；不改生产代码、不重审模块结论 |
| codebase-design / ponytail-review | 最小是删两个 `known_findings` 对象；不跑 `--write-baseline`、不改扫描器/仓储/台账、不加 JSON 注释或版本键 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| graft | 本步未再检索；JSON/docs 不在 graft 索引。最小处置边界按 B1 已给坐标直读 JSON 与扫描器 L265/L273–282 |
| audit-context-building | **未用**（本体强制派 function-analyzer 子代理） |

### 完整差集（GR-01、席甲 CX-M030-001、本席观察 1…8、席甲 OBS-1…3）

漏并（findings）：无。改写原意：无。两席 B1 全部临时缺陷 ID 仍落在 001 或只登记段。无单席独有缺陷被丢。无本批新开「否」ID。无添加项。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M030-001 | Grok GR-01 + 席甲 CX-M030-001（两席同见、处置相同） | 细化/是/是（从 `known_findings` 删 `access_grant_requests` 与 `d` 两个对象；保留仍复现的两条 F1 与 note、generated_at_source 原文逐字不动；不跑 `--write-baseline`；不改扫描器、仓储、安全台账） | **PASS（细化/是/是）** |
| （无新 ID） | 两席其余观察 | 只登记不入队 | **PASS（只登记；无应升队项）** |

冲突入队 0。细化入队 1（001）。技术性入队 0。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001（1）；「否」本批新开 ID = 0。B1 两席均 FAIL，合取 FAIL，本批不撤回任何 B1 票。

**整批候选：同意。**

---

### 一、差集核实（逐项独立）

**001 — GR-01 与 CX-M030-001 同一事实、同一处置。票：同意入队；同意归并为 M030-001。**

对照两份封存 B1，不是重审模块：

| 项 | 本席 GR-01 | 席甲 CX-M030-001 | 候选 001 |
|---|---|---|---|
| 文件 | `security-scans/tenant-isolation-baseline.json` | 同 | 同 |
| 对象 | `:5-8` `F1:UNSCOPED_DIRECT_READ:access_grant_requests`；`:9-12` `F2:UNREGISTERED_TABLE:d`；note `:2` | `:5-12` 两条目 + `:2` note | `:5-12` 两条目；事实栏含 note `:2` 自述不符 |
| 现行复现 | 否（两席 B1 默认路径均 stale 提示这 2 key；`--strict` 只红另两条活 F1） | 同 | 同 |
| 原因 | origin/main `JdbcAccessRepository.java:766` 已有 `project_id = :organization_id`；M029-002 扫描器已中和 `IS [NOT] DISTINCT FROM` | 同（席甲写 `IS NOT DISTINCT FROM d.col`） | 同 |
| 最小处置 | 删除这两个 `known_findings` 对象；保留两条仍复现 F1；不跑 `--write-baseline`；不改扫描器/仓储/HTML；删完后 note 已自洽、无需改 note | 删去这两个条目；保留另两条与 note、generated_at_source 原文；不带 `--write-baseline` | 删除这两个对象；保留仍被复现的两条 F1 与 note、generated_at_source 原文逐字不动；不跑 `--write-baseline`；不改扫描器、仓储、安全台账 |

事实同一：清单仍挂两条扫描器已不发射的 key，`text`/`note` 仍写成「扫描器报 / 尚未收紧」，默认闸口继续把这两 key 当「已知」。处置同一：手工删对象，不动活键与顶层 note / generated_at_source，禁止用 `--write-baseline` 当修复工具。未改写任一座原意。候选把两席临时 ID 合成 001，符合「同一事实同一处置不拆 ID」。

**其余观察 — 只登记。票：同意；无漏并缺陷、无应升队项。**

| 来源 | 落点 |
|---|---|
| 席甲 OBS-1 / 本席观察 4 | `tools/generate-formal-security-baseline.mjs:304-310` 与生成的 `security-scans/findings-register-v1.md:27` 仍写「access_grant_requests 保留 baseline / 收紧至 1 条」→ 只登记（安全基线生成器与台账所在模块；用户本批明示不改安全台账） |
| 本席观察 2 | `--write-baseline` 内置旧 note 要求 findings-register 的 `SEC-*-TENANT-ISOLATION-*` 一一对应，现行台账无 BATCH1/REFLOW 那些 id → 只登记（扫描器模块，M029 已闭环）；同时构成 001 禁止跑该开关的理由，不另开 ID |
| 本席观察 3 | `generated_at_source` 不是一次扫描的定位符 → 只登记 |
| 本席观察 6 | origin/main `JdbcAccessRepository.java:747` 仍有按 id 的无范围读（扫描器头注 B3 盲区）→ 只登记；不在本模块发明「必须逐语句 scope」 |
| 本席观察 7 / 席甲 OBS-2 | 主工作区相对 origin/main 的 5 行差异（OPEN PR #446 中性化）随检查点带入；**4 个 key 未变** → 只登记 |
| 席甲 OBS-3 | 陈旧键只提示不失败是扫描器既定行为（M029 已审）→ 只登记；001 改的是本清单 key 集合，不是去改扫描器「stale 不红」政策 |

本席观察 1（扫描器运行时只读 key、不读 text/note）：是 001「必须删对象、不能只改 text」的机制说明，已吸收进最小处置，候选未单列。**不构成漏并**——无应升队项。

本席观察 5（`generate-formal-security-baseline.mjs` 只检查本文件非空，删键不会让 `verify:security-baseline` 因内容红）：是 001 最小处置安全的旁证，席甲 B1 对抗式亦写「删两条后仍绿」。候选未单列。**不构成漏并**。

本席观察 8（未在主工作区直接跑扫描器，D-028 / V427 伪红）：方法观察，B1 本就不立项。候选未点名。**不构成漏并**。

无应把下列升进本批队列的项：两条仍复现的 F1（拿掉活键会改变闸口策略、超出本文件「登记已发现尚未收紧项」职责，两席 B1 均保留）；安全台账/生成器过时叙述（非本模块 FILES）；`:747` 按 id 读（扫描器口径盲区，不加新需求）；`--write-baseline` 一键重建（会覆盖中性 note 与活键 text）；把扫描器 stale 政策改成失败（M029 已审）。

---

### 二、分类理由

四分类顺序（GEN-017）：冲突 → 细化 → 技术性 → 添加。能指到 HTML 上位条目的一律先归细化。

- **001：细化。** 上位（本席 B1 列出、本步只读核验仍在原文）：业务地图 `docs/business-system-map-for-operations-v0.1.html:707` 红线④「看不到别的商户和别的项目的数据」；治理地图 `docs/platform-governance-map-for-operations-v0.1.html:653`「数据的项目与商户隔离不提供关闭开关」。本文件是该隔离红线的机审棘轮清单，不是业务发明。候选写「采 Grok 列出的上位句」，与上两条主句一致。
- 非阻断勘误（不否决）：本席 B1 另列枚举速查 `docs/business-enum-quick-reference-v0.1.html:235`「所有数据默认按项目隔离，普通岗位不得跨项目查看」为同义佐证。候选未写入分类栏。三条同指项目/商户隔离、无一改变 001 原意，不要求改候选表。
- 席甲 §4 GEN-017 表写「技术性记录文件；无业务规则」，与席甲 §5 本条四分类「细化」自相矛盾。按顺序：能指向上位 HTML 即细化，细化先于技术性。候选采本席细化，同意；不采表内「技术性」。
- 无冲突：三 HTML 未点名本 JSON / F1 / F2 键名，清单也未宣称「已隔离」或「允许关隔离开关」，不存在与明文不能同时成立的句子。
- 无添加：删陈旧键、保留活键，不新造隔离规则、不新造表、不改 HTML。
- 工程缺陷 **是**（两席 B1 均类型 4：默认路径 stale 提示 + exit 0，`--strict` 只红活键）。队列 **是**。

冻结 ID：候选 GEN-002 / GEN-016 / GEN-005 / GEN-010 / GEN-001。本席 B1 冻结 GEN-002、GEN-016、GEN-010（棘轮同键豁免孔），分类依据 GEN-017；席甲冻结 GEN-016 / GEN-002 / GEN-005 / GEN-001。候选为两席并集。GEN-005 本席 B1 判无本面（本文件不实现授权），席甲把「同键回归被静默放行」记在 GEN-005；GEN-010 本席用来记同一豁免孔。二者均在本模块映射 18 项内，**不是加锁判据**。同意保留并列，不要求删。不把 GEN-017 写入冻结列（分类栏已承担）。

---

### 三、最小处置边界（含 JSON 版本标记确认）

写入集合＝本模块 **1** 文件：`security-scans/tenant-isolation-baseline.json`。一次 B3 禁止改扫描器、仓储 Java、安全台账 / findings-register / formal-security-baseline、三 HTML、锁、CSV、生成器。

#### 甲 — 从 `known_findings` 删除两个对象

同意。现数组顺序：① `F1:UNSCOPED_DIRECT_READ:access_grant_requests`（L5–8）② `F2:UNREGISTERED_TABLE:d`（L9–12）③ `F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution`（L13–16）④ `F1:UNSCOPED_DIRECT_READ:integration_channel_store_map`（L17–20）。删除 ①②。修后数组只留 ③④，key 与 text 字面量不动。

只读核现文件：顶层三键、四对象均 `key`+`text`、无重复 key、末尾 `\n`、无 JSON 注释、无尾逗号。B3 手工删除时须保持 2 空格缩进与文件末换行，不得在最后一个对象后留逗号，否则 `JSON.parse`（扫描器 L265）FATAL。

#### 乙 — 保留仍复现的两条 F1，以及 note、generated_at_source 原文逐字不动

同意。本席 B1 Q4：拿掉活键会把仍存在的扫描器 finding 变成「新增回归」，改变闸口策略，超出本文件职责。席甲 B1 对抗式同样保留这两条。note 在删陈旧键后与「只登记扫描器已发现、尚未收紧的项」自洽，无需改 note（本席 B1 拟处置原文）。`generated_at_source` 现值 `"static scan of Jdbc*Repository.java vs domain-ownership-scope-audit-v0.1.csv"` 与扫描器 writeBaseline 同串，本批不是去把它改成时间戳。

#### 丙 — 不跑 `--write-baseline`

同意，且必须禁止。扫描器 L273–282 现稿会：(1) 丢掉两陈旧键（碰巧是好事）；(2) 用扫描器长 `text` 覆盖两条活键的中性表述；(3) 把 `note` 写回「每条须在 findings-register 有对应 SEC-*-TENANT-ISOLATION-* 证据」——现行台账并无 BATCH1/REFLOW 那些 id。用户本批亦明示不跑该开关。重建命令可作为 note 里的文档句保留（逐字不动），B3 不当修复工具执行。

#### 丁 — 不改扫描器、仓储、安全台账

同意。`:747` 无范围读、stale 只提示不失败、生成器 L304–310 / findings-register:27 过时叙述，两席均只登记。扫描器提示「应从 baseline **与台账**移除」不把台账拉进本模块 FILES。用户本批明示不改这三类。

#### 戊 — JSON 无注释语法，本文件不加版本标记（确认）

**确认，同意。** 理由：

1. 本文件是纯 JSON。RFC 8259 / ECMA-404 无注释语法。扫描器 L265 `JSON.parse(fs.readFileSync(baselinePath, "utf8"))`，插入 `// 文件版本：V0.01` 或 `/* */` 会直接 FATAL exit 1，delivery-gates/predeploy 红。
2. 顶层形状是扫描器 writeBaseline 的三键闭集：`note` / `generated_at_source` / `known_findings`。另加 `version` / `file_version` 顶层键会偏离该闭集；扫描器虽只把 `known_findings[].key` 进 Set，本批最小处置是删对象，不是扩 schema。
3. M020–M029 给 JS/Java/md 加或升 `// 文件版本` / `# 文件版本` / 文首版本行，那些语言有注释语法。该做法**不适用于**本模块这一个纯 JSON 文件。`security-scans/` 下现 JSON 亦无版本标记。
4. 候选最小处置未要求加标记，与「不加」一致。B3 **不得**为「跟前模块惯例」另加注释或新顶层键。本确认不是新判据，是对本文件介质的边界意见。

禁止：跑 `--write-baseline`；改两条活 F1 的 key/text；改 note / generated_at_source；改扫描器 / 仓储 / 台账 / 三 HTML / 锁；给 JSON 加注释或版本键；把活键从清单拿掉以逼修仓储；把 `:747` 或 findings-register 过时句塞进本条。

---

### 四、添加／HTML／questions

本批无添加项、无 HTML 修改申请、无 questions。`docs/audit-2026-09/questions/` 无 `M030*`。不 BLOCKED-ASK。不按铁律 4 申请改三 HTML。001 不发明业务规则（不改隔离谓词、不改扫描器口径、不给 `d` 当表名起业务含义）。

只登记不入队（候选段，本席同意）：生成器与 findings-register 对 access_grant_requests 的过时叙述；`--write-baseline` 旧 note 的 SEC-* 一一对应在现行台账不存在；`generated_at_source` 非一次扫描定位符；`:747` 按 id 无范围读（扫描器 B3 盲区）；PR #446 五行动随检查点带入；陈旧键只提示不失败是扫描器既定行为。无应升本批缺陷。

### 分类／队列／允许的最小修复边界

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3 只改本模块 1 文件：`security-scans/tenant-isolation-baseline.json`。

- 001：从 `known_findings` 删除 `F1:UNSCOPED_DIRECT_READ:access_grant_requests` 与 `F2:UNREGISTERED_TABLE:d` 两个对象；保留 `F1:UNSCOPED_DIRECT_READ:revenue_stream_attribution` 与 `F1:UNSCOPED_DIRECT_READ:integration_channel_store_map` 的 key/text 原文；`note` 与 `generated_at_source` 逐字不动；保持合法 JSON（2 空格缩进、文件末 `\n`、无尾逗号、无注释）。不加版本标记。不跑 `--write-baseline`。不改其它文件。

禁止：改扫描器 / 仓储 / 安全台账 / 三 HTML / 锁；发 2.2；给本 JSON 加 `//` 或新顶层版本键；删除或改写两条活 F1。

### 结论

全批差集无漏并、无原意改写。GR-01 与 CX-M030-001 同一事实同一处置 → 001。两席其余观察全部只登记，观察 1/5/8 未单列不构成漏并，无应升队项。001 细化／是／是；上位采本席 BSM:707 与 PGM:653，BEQR:235 未列入不否决。最小处置只删两个陈旧对象，活键与 note、generated_at_source 不动；不跑 `--write-baseline`；不改扫描器、仓储、安全台账。**确认：纯 JSON 无注释语法，本文件不加版本标记，M020–M029 源码/文档加标记的做法不适用。** 添加 0。HTML 申请 0。questions 0。写入集合＝本模块 1 文件。逐 ID：001 PASS、其余观察只登记 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M030 IN_PROGRESS；B3=0。
