CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M029 B5-r1 席乙（Grok）修复后整模块全量审计 raw

- 席位：Grok 独立只读审计席乙（第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B5-r1（B3 累计 1 后的整模块重审；不只看 diff；不复用 B2/B4 票作结论）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：`plan/modules.md` V0.04 M029「租户范围与数据隔离（技术不变量）／检查及生成工具／第1段」；标记 NORMAL；计划有效内容 729 行（过期）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- origin/main：`6f3d25d7bec4d1238455c91e6f2376936317980d`
- after tree（B3-r1 冻结）：`d639978e4c5b02b405365eba6f87dcfd73bcb8dd`（本模块 3 个改动工具 + 两份产物的工作区 blob 与该 tree 逐字节一致；第四工具未改）
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（未抄 sidecar / ledger / 其它报告）。sidecar 与 ledger 顶部同值，仅作交叉观察
- LOCK-AMEND-COUNT：1（ledger 顶部；本席未改锁）
- 映射 18 项：`M029 → [GEN-001 … GEN-018]`（无 HTML 派生条目）
- findings 输入：V2.93；M029-001、M029-002 修复队列「否（已修，B3-r1）」；当前队列「是」= 0。不把全部否当成工具已全对
- 独立性：未打开 `modules/M029-b5-r1-self-raw.md` 正文。旧 B1（含 B2-r1 追加段）与 B4-r1 可读，只作背景，结论以本席全文重审 + 当场实跑为准。未改计划 / findings / ledger / 业务代码 / 三 HTML / 锁；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席；未对数据库执行写语句；除 D-028 允许的一次性 worktree 外未建脚本、日志或临时审计产物。psql 只 SELECT；口令只从 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD=` 行读取，未 source、未回显
- `test -f .git/MERGE_HEAD` → **NO_MERGE**
- 本席步骤结论：**PASS**（001、002 修复落地、无回归、无新的须入队工程缺陷；B2 只登记项维持；添加 0；无 HTML 修改申请）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md，不是提一嘴。用户指令禁止子代理 / 第三席 / MCP，优先于 Arc 默认全量派席。B5 禁止修复，故 TDD / ponytail 修复强度 / receiving-code-review 的 IMPLEMENT 一律不执行。runbook「阶段 B 不创建 worktree」被 ledger「D-028 环境决定」与本轮用户指令覆盖：工具实跑只在基于 origin/main 的一次性 worktree 里进行。

本席环境技能根：`/home/james/.claude/skills/`、`/home/james/.claude/plugins/`、`/home/james/.grok/bundled/skills/`、`/home/james/.claude/plugins/cache/`。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读；用户指令优先于技能 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 路由到 audit；**用户禁子代理 / 禁改业务**，不跑 Arc 默认派评审、不写 `docs/arc/audits/` |
| graft | `~/.claude/skills/graft/SKILL.md` | 已读。本模块强制「4 文件全文、不抽样」，直接全文实读四工具；未再跑 skeleton/ask（避免以索引替代全文） |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读。本轮 sha256 / node --check / 工具实跑 / psql / worktree add-remove 均前台完成，未挂 monitor |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`、同目录 `grill-me/SKILL.md` | 对抗线自问自答；本体「等用户 / 派子代理」作废 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 四工具是浅接口（npm 脚本）+ 深不变量（迁移解析 / F1 F2 / 测试库 URL）；缺陷落在「声称检查的东西实际能不能红」 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现；B5 禁止修复、不加诊断桩、不对库执行写 |
| tdd / test-driven-development | 已读 | 用来判断门禁断言能否因目标行为变坏而红；本席不写测试 |
| verification-before-completion | 已读 | PASS 前当场：独立 sha256、after tree blob、origin/main worktree 实跑、psql 现态差集、FROM/正则夹具 |
| receiving-code-review | 已读 | 禁止表演性同意；本轮无对方 raw 可收（未读本次 self-raw） |
| requesting-code-review | 存在，B4 用 | B5 不适用（禁止派第三席） |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → NO_MERGE |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 可选存在；只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B5 不修；review 口径扫可删项，见 §8 |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B5 不适用 |
| i-have-adhd | marketplace / cache `skills/i-have-adhd/SKILL.md` | 存在；本席正文只落 raw；CLI 不回带结论或机器标记（适配器优先于该技能的「正文先写下一步」） |
| audit-context-building | 已读 | 要求派 function-analyzer 子代理：与「只允许两席」冲突，**不执行**，记缺口 |
| batch-grill-me | 无独立 SKILL | **缺口**；已并入 grilling 自问自答，不冒充调用 |
| ubiquitous-language | 本机 skill 目录无 | **缺口** |
| dispatching-parallel-agents / subagent-driven-development / research / using-git-worktrees | 存在 | 提示词划掉。一次性 `git worktree add --detach` 是 D-028 允许的只读核，不是 using-git-worktrees 技能本体；核完已 remove |

graft 本轮检索调用：0（全文实读）。

一次性 worktree：`git worktree add --detach /home/james/wt-m029-b5r1-grok-ro origin/main`（HEAD `6f3d25d7`），软链 `node_modules`，拷入修后 5 个文件（3 个工具 + 两份产物），核完 `git worktree remove --force`，目录已不存在。worktree 内未带 `--write-baseline`。未改 origin/main 已跟踪文件以外的拷入集。

---

## 1. 模块覆盖（全文实读，行数/SHA 实算）

命令：`wc -l -c` + `sha256sum` + `git hash-object` + `git rev-parse HEAD:<path>` / `origin/main:<path>` + `git ls-tree d639978e -- <paths>`。

| 路径 | 物理行 | 字节 | SHA256 | 工作区 git blob | HEAD / origin/main blob | after tree blob |
|---|---:|---:|---|---|---|---|
| `tools/generate-data-isolation-spec.mjs` | 140 | 6650 | `c9311231ec932fb65a3776f882078ed4d7ba8d4cea0c6c79032861c8cea944ba` | `174ed1691c41c0c96e5ef9540f13d1db9e3ca548` | `24ce52464cef58fc96c722f8a347d656dd348977` | `174ed169…`（同工作区） |
| `tools/generate-domain-ownership-scope-audit.mjs` | 233 | 14340 | `47087d2b8330828419f79397ba85be7dd8aaca0b281900cdf6180d26d0898e19` | `d2b756d093cdceb23cb61a5166764bf14539e38d` | `23c57f4c5a94638180505a4504daad21d9ce8858` | `d2b756d0…` |
| `tools/verify-tenant-isolation.mjs` | 339 | 16841 | `56dbdbd745afa7fa8c2326262082d9ba10509c855f8dfad7d9355fd2f966395f` | `c260a2db466cf581a57102d95fe080c751501d5c` | `f5433cac0a22d33db0c73474fca75035ca92ef81` | `c260a2db…` |
| `tools/verify-test-db-isolation.mjs` | 33 | 1445 | `6c36a678982b21189ff3a14961be56ff7dd945d10709cdccb9c44d1d4007ffea` | `642ebb0d3b72557cdbdf2d3dc272ceeaeb256060` | 同左（未改） | `642ebb0d…`（mode 100755） |
| **合计** | **745** | **39276** | — | — | — | — |

覆盖：**4/4 全文逐行，不抽样**。文件头自述版本：规格生成器 V0.01、CSV 生成器 V0.04、隔离机审 V0.01、测试库门禁无版本行（本批未改）。

相关生成物（非本模块 FILES，但是生成器拥有区域与 `--check` 对象；只读对照）：

| 路径 | 物理行 | 字节 | SHA256 | after tree blob |
|---|---:|---:|---|---|
| `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` | 372 | 42095 | `d0474060434d1b91b48c003c9f623de42ff6b3791fc8af8291cb0a322e53cf3e` | `f92e7cd604feaa31320ab2ef4a97b70d765d00c0` |
| `docs/domain-ownership-scope-audit-v0.1.csv` | 336 | 60163 | `1122f92a09ed43ed0c084e75ef022066fe880e30b843d5450f0911e664c9aa0b` | `078abd2474ae6a305c2f8ca664e220a6444ee609` |

相对 origin/main：规格 2 个自动区块单元格 `inherited-or-location-scope`→`direct`（`venue_business_hours`、`venue_sessions`）；CSV 1 行 `business_process_step_records` `single_complex_default`→`inherited`。规格手写段未动。`inherited-or-location-scope` 在规格全文命中 **0**。

相关三 HTML（只读，铁律 4 未改；本模块映射无 HTML 派生 ID，HTML 只作第一性原理类型 1 与四分类上位）：

- `docs/business-system-map-for-operations-v0.1.html` L707 红线④原文：「看不到别的商户和别的项目的数据」；同条：持密钥最高权限只可逐笔开放、七类校验不设关闭开关
- `docs/platform-governance-map-for-operations-v0.1.html` L653：「所有业务数据默认按项目隔离……数据的项目与商户隔离不提供关闭开关」
- `docs/business-enum-quick-reference-v0.1.html` L235「项目与敏感数据权限」复述默认按项目隔离；无「租户隔离列 / scope_type / 机审闸口」枚举锚点。HTML 沉默处不反推分类名或门禁实现

D-028：主工作区迁移目录含已暂存未发布 V427，直接跑两个生成器 `--check` 与 `verify-tenant-isolation` 会因未发布内容而红，**不是本模块缺陷**。工具实跑证据取自 origin/main worktree（无 V427，有 V428）。`complex_test` 已应用 V427+V428：多出 `business_entity_projects` 与 `merchants.project_id` 属预期。

---

## 2. 取证块（类型 2 / 4）

### 2.1 origin/main worktree 实跑（证据环境）

worktree：detached `6f3d25d7`，无 V427，有 V428。拷入主工作区修后 5 文件后 SHA 与主工作区一致。`node --check` 四工具均 0。

```text
node tools/generate-data-isolation-spec.mjs --check
→ data isolation spec is in sync with Flyway migration scope baseline.
SPEC_CHECK_EXIT:0

node tools/generate-domain-ownership-scope-audit.mjs --check
→ DOMAIN_OWNERSHIP_SCOPE_PASS: 335 tables classified
CSV_CHECK_EXIT:0

node tools/verify-tenant-isolation.mjs
→ 权威表清单（CSV）：335 表（direct=173，白名单豁免=82，inherited 为信息性盲区）
→ Jdbc*Repository 源：53 个文件
→ 本次扫出 finding：2（已登记 baseline：2，新增回归：0）
→ 注意：baseline 中 2 条已不再复现：
    · F1:UNSCOPED_DIRECT_READ:access_grant_requests
    · F2:UNREGISTERED_TABLE:d
→ PASS（棘轮）：无新增回归；2 条已知缺口在台账跟踪闭环中
ISO_DEFAULT_EXIT:0

node tools/verify-tenant-isolation.mjs --strict
→ FAIL: strict 模式，2 项
    · F1 UNSCOPED_DIRECT_READ 表 'integration_channel_store_map'（direct, scope=merchant_id）
    · F1 UNSCOPED_DIRECT_READ 表 'revenue_stream_attribution'（direct, scope=project_id）
→ 输出中无「表 'd'」、无其它 F2
ISO_STRICT_EXIT:1

node tools/verify-test-db-isolation.mjs
→ TEST_DB_ISOLATION_PASS: 测试源未引用生产库 complex（均用 complex_test）
TDB_EXIT:0
```

未使用 `--write-baseline`。worktree `git status --untracked-files=no` 仅上述 5 路径为 `M`；第四工具不脏。核完已 `git worktree remove --force`。

### 2.2 complex_test 现态（psql 只 SELECT）

口令：`sed -n 's/^COMPLEX_DATABASE_PASSWORD=//p' /etc/complex/complex.env`（行存在、无引号、未回显、未 source）。`psql -h 127.0.0.1 -U complex -d complex_test`。

```text
current_database|current_user|inet_server_addr
complex_test|complex|172.18.0.4

flyway 最新 5 条：428 m021 settlement format share media；427 m009 project profiles and associations；426…424
public BASE TABLE 计数：337
to_regclass('public.business_entity_projects') IS NOT NULL → t
merchants.project_id 列计数 → 1
```

CSV（origin/main 口径、335 表）对 DB：

- 库有、CSV 无：`business_entity_projects`、`flyway_schema_history`
- CSV 有、库无：**空**

规格 §3A（170 张、类型全 `direct`）对 DB 的 SCOPE_COLUMNS（`organization_id/merchant_id/project_id/venue_id/bar_venue_id/space_id`）：

- 库有 scope 列、§3A 无表：`business_entity_projects`、`merchants`（二者均因未发布 V427 的 `project_id` 才进入该列集；origin/main 迁移里 `merchants` 本无 SCOPE_COLUMNS）
- §3A 有、库无：**空**
- 共有表的列差集（库多 / 库少）：**双向空**
- §3A 170 张在 CSV 中类型 ≠ `direct`：**0**

`venue_business_hours` / `venue_sessions`：§3A 与 CSV 均为 `direct` + `venue_id`。  
`business_process_step_records`：CSV `inherited` / `process_instance_id` / `process_instance_id -> business_process_instances -> merchant_id/project_id`，与规格手写 §3 L50 一致。

结论：两个生成器输出与 `complex_test` 真实结构的差集，恰好等于 D-028 声明的未发布 V427 两项 + 系统表 `flyway_schema_history`。无漏表、无错列、无残留 `inherited-or-location-scope`。

### 2.3 迁移解析闭合（origin/main worktree 迁移目录）

395 个 `V\d+__*.sql`；字典序与按 `Number(V)` 序相等；全部三位零填充；最高 V428。  
`CREATE TABLE` 字面 350，两生成器正则各命中 **348**；差额 2 条均在注释（V143 L12、V350 L25），不是漏表。  
DROP TABLE 13 张；348 − 13 = **335** = CSV 行数。  
DROP COLUMN 7 处，目标均非 SCOPE_COLUMNS（`high_value_threshold_cents`、`entitlement_type`、`retention_bps/retention_cent/spv_pool_cent/giveaway_cost_cent`、`parent_entity_id`）。  
`ALTER TABLE ONLY`：0。`RENAME TABLE`：0。V350 头注写明用删旧建新绕开 RENAME。  
quoted / UNLOGGED / AS SELECT / 单行 CREATE / schema 限定：0。

### 2.4 F2 抽取面（修后代码就地复现，不改文件）

把 `extractReferencedTables`（含 EXTRACT 中和 + `IS [NOT] DISTINCT FROM` 中和）对 53 个 `Jdbc*Repository*.java` 跑一遍：引用 328 个表名，相对 CSV + 基础设施白名单的未登记 token **0**。`--strict` 无 F2，与此一致。

夹具（类型 4）：

| 输入 | 抽出的表 |
|---|---|
| `… FROM merchants WHERE a IS NOT DISTINCT FROM d.merchant_id` | `merchants`（无 `d`） |
| `… IS DISTINCT FROM d.merchant_id` | `merchants`（无 `d`） |
| `EXTRACT(EPOCH FROM open_time) FROM venue_sessions` | `venue_sessions`（无 `open_time`） |
| `SUBSTRING(x FROM 1 FOR 2) FROM merchants` | `merchants`（`1` 不是标识符） |
| `TRIM(BOTH 'x' FROM col) FROM merchants` | `col` **和** `merchants`（理论假阳性；见观察） |
| `OVERLAY(col PLACING 'a' FROM 1) FROM merchants` | `merchants` |
| `FOR UPDATE SKIP LOCKED` | `merchants`（`skip` 在噪声表） |
| `WITH d AS (SELECT 1 FROM merchants) SELECT * FROM d` | `merchants`（CTE `d` 排除） |
| `FROM totally_new_table` | `totally_new_table`（F2 仍能红） |

仓储内 `IS (NOT )?DISTINCT FROM`：`JdbcAnalyticsRepository` 1、`JdbcSettlementConfigWriteRepository` 2、`JdbcSettlementBillRepository` 1、`JdbcCustomerRepository` 10，合计 **14**，与 B1 计数相同；修后均不再产生 `d`。Jdbc 源中无 SQL `TRIM(`/`SUBSTRING(`/`OVERLAY(`。

### 2.5 测试库门禁正则正反例（类型 4）

头注正则 `jdbc:postgresql:\/\/[^"'\s]*\/complex(?!_test)\b`：

| 样本 | 命中 | 期望 |
|---|---|---|
| `jdbc:postgresql://127.0.0.1:5432/complex` | true | 红 |
| `…/complex?ssl=true` | true | 红 |
| `${COMPLEX_DATABASE_URL:jdbc:postgresql://localhost:5432/complex}` | true | 红 |
| `…/complex_test` | false | 不红 |
| Spring `…/complex_test` | false | 不红 |
| `…/complex_test?ssl=true` | false | 不红 |
| `…/complex_other` | false | 不红（`_` 不是词界） |

`backend/src/test` 现有 `jdbc:postgresql:` 命中均为 `complex_test`。  
`cd /tmp && node <该脚本>` → 仍打印 `TEST_DB_ISOLATION_PASS`（根目录缺失被 `try/catch` 吞掉）——复现 B2 只登记项，不升队。

### 2.6 文件头自述 vs 代码 / npm

| 文件头声称 | 代码 / package.json | 判定 |
|---|---|---|
| 规格生成器：Flyway 为事实源，写 §3A，`--check` 比字节 | `docs:data-isolation-spec` / `verify:data-isolation-spec` 指向该文件；`--check` 绿 | 一致 |
| CSV 生成器 V0.04：逐表分类 + `--check` | `docs:domain-ownership-scope` / `verify:domain-ownership-scope --check`；335 表绿 | 一致；`status` 写死 `covered` 使 uncovered 分支不可达（只登记） |
| 隔离机审 L10「fail-closed，发现即非 0」vs L253–257 棘轮 | 默认棘轮 PASS；`--strict` 对 2 条已登记 F1 非 0 | 字面差已登记；行为与 L253–257 一致 |
| 隔离机审 F1/F2 口径、白名单、B1–B4 盲区 | F1 在 `--strict` 仍能红；F2 对 DISTINCT FROM 不再误报；扫描面仍是 `Jdbc*Repository*.java` | 一致 |
| 测试库门禁：允许 `complex_test`、禁裸 `complex` | 正则正反例与现树扫描一致 | 一致 |

`verify:test-db-isolation` 在 `verify:predeploy`，**不在** `verify:delivery-gates`（package.json 非本模块文件，只登记）。

---

## 3. 第一性原理三问（2.3）

**Q1 这段东西为什么必须存在？删掉它，三份 HTML 里哪一条会做不到？**

四工具是红线④ / 治理地图「默认按项目隔离、隔离不提供关闭开关」的下位机审：生成器把迁移里的 scope 列与表分类锁进规格 §3A 和 CSV；隔离机审拦「direct 表没有任何 scoped 读通道」和「SQL 引用了未登记表」；测试库门禁拦测试源写回生产库 URL。删掉它们，HTML 条文本身不会从页面消失，但「声称在查隔离」的可执行闸口会没。答得上用途 → 有业务上位；四分类走细化（见 §5），不是添加。

**Q2 它声称的行为，有没有被实际执行验证过？证据在哪？**

有。§2.1–2.5：两个 `--check` 在 origin/main+修后 5 文件上 exit 0；隔离默认棘轮 exit 0、`--strict` exit 1 且仅两条已登记 F1；F2 不再报 `d`；测试库门禁 exit 0；psql 差集仅 V427 预期项 + `flyway_schema_history`；14 处 `IS NOT DISTINCT FROM` 抽出 0 个假表名。类型 2（可执行产物）+ 类型 4（命令输出）。

**Q3 它依赖的前提，哪一条是没验证过就当真的？**

已当场验证、不再当「想当然」的：§1 类型闭集与生成器输出对齐；F2 把 `FROM` 后标识符当表名时至少中和了 EXTRACT 与 `IS [NOT] DISTINCT FROM`；CSV 335 表与库（减预期差）一一对应。  
仍属已登记、本次无新证据的前提：`status=covered` 写死 → uncovered 检查不可达；F1 是文件级近似（头注 B3）；扫描面不含非 `Jdbc*Repository`；缺测试根目录时门禁静默 PASS；baseline 陈旧键不使默认路径变红（属 M030）。不为这些理论缺口加需求。

---

## 4. 顺从式：18 项逐条

定义全文见 `plan/criteria-lock.md` L12–100。每条先适用/无本面，再给本轮证据。不新增判据。

| ID | 判据要旨 | 适用 | 本轮证据 |
|---|---|---|---|
| GEN-001 | 有可验证用途；不为假设场景加抽象 | **适用** | 四工具各有 npm 入口与失败出口。001 去掉闭集外类型、002 中和运算符，均落在现生成物/现 SQL。不把 TRIM FROM / V1000 / 缺测试目录立成新需求 |
| GEN-002 | 声明入口/主路径/错误路径须能执行且与声称一致 | **适用，本轮无未修缺陷** | 入口均能跑。`--check` 现与 §1 闭集及 CSV 分类一致（001 已修）。F2 对 `IS NOT DISTINCT FROM d.col` 不再把 `d` 当表（002 已修，`--strict` 无 F2）。`--strict` 仍能因真实 F1 非 0。test-db 正反例与头注一致 |
| GEN-003 | 测试须执行目标行为且能失败 | **适用（弱）** | 本模块无单测文件；「测试」即四条 npm 门禁。test-db 与两 `--check` 有能失败断言。tenant-isolation 默认对 baseline 内 finding 不能失败；`--strict` 能（本轮 exit 1）。CSV `--check` 的 uncovered 分支因写死 `covered` 不可达（只登记） |
| GEN-004 | 首次可信使用前校验输入 | **适用** | CSV 缺文件/缺列头 FATAL 非 0；baseline JSON 坏则 FATAL。迁移目录读失败会抛。口令不进 argv |
| GEN-005 | 受保护操作须服务端授权 | **适用（下位机审，非授权实现）** | 工具本身不授权。F1 声称抓住「direct 表无 scoped 读通道」：`--strict` 仍红 2 张已登记表，说明该判定面未废。写路径/Service 门控是头注 B2/B3 盲区，不重复立项 |
| GEN-006 | 状态/引用完整性 | **适用（弱）** | 生成器按版本序 CREATE→ALTER→DROP，CSV 335 = 命中 CREATE − DROP。不写库。DROP COLUMN 不解析，现 7 处均非 scope 列（只登记） |
| GEN-007 | 并发与幂等 | **无本面** | 四工具无共享可变状态、无重试窗口、无并发入口。不为假设的并行 CI 加锁 |
| GEN-008 | 跨层名称/类型/枚举一致 | **适用，001 已修** | §3A 类型闭集现仅为 `direct`（170/170）。CSV ∈ {direct 173, inherited 80, global_reference 7, single_complex_default 75}。`business_process_step_records` 现为 inherited，与规格手写 §3 一致。`venue_*` 现为 direct，与 CSV 一致。`global_reference` 由隔离机审白名单使用，规格 §1 未列该名——属 M028 文档闭集，B2 未入本模块队列，无新证据不改判 |
| GEN-009 | 错误可检测、不可伪成功 | **适用** | `--check` 不同步非 0。F2 真新表仍能红（夹具 `totally_new_table`）。001 的「非法类型伪成功」已随恒 `direct` 消失。002 的「`d` 被 baseline 吞掉」：默认路径对 `d` 现报 stale、不作为新增回归；`--strict` 已无 `d`。棘轮已知 F1 默认不红是设计，只登记 |
| GEN-010 | 不得泄密/注入 | **适用** | 脚本不打印连接串或口令。本席取证未回显口令。正则输入为硬编码表/列名。无路径穿越写（写路径仅生成器对已知相对路径） |
| GEN-011 | 关键操作可关联证据 | **适用** | 闸口 stdout 含表名/文件/key；baseline 4 key：`access_grant_requests`、`d`、`revenue_stream_attribution`、`integration_channel_store_map`。前两条本轮标 stale（M030 清理，本模块不动） |
| GEN-012 | 资源边界 | **适用（弱）** | 读迁移/53 个 Java 为有界目录遍历。无循环放大。测试库 walk 现树可完成 |
| GEN-013 | 构建配置与依赖可复现 | **适用** | 仅 Node 标准库。npm 脚本名与文件头一致。worktree 须软链 `node_modules` 才能走 npm；本席直接 `node tools/…` 不依赖未声明本机状态 |
| GEN-014 | 迁移顺序确定、与模式一致 | **适用（解析器，非迁移正文）** | 两生成器在现 395 个三位版本文件上排序等价。解析结果对 `complex_test`（减 V427 预期差）零漏零错。生成器不是 Flyway 执行器 |
| GEN-015 | 界面可用性 | **无本面** | 无 UI、无文案面。CLI 英文/中文混合不影响本判据 |
| GEN-016 | 文档路径/命令/状态可复现 | **适用，001 已修** | `npm run verify:*` / `docs:*` 可复现。§3A 不再写入 §1 没有的类型名。文件头 B1–B4 与代码一致。CSV 行内 `file_version` 恒 V0.01（生成器文件头已 V0.04）属输出格式字段，只登记 |
| GEN-017 | 冲突→细化→技术性→添加 | **适用** | 见 §5。本轮无新冲突/添加。001/002 已是细化且已修 |
| GEN-018 | 结论落到四类证据之一 | **适用** | 本 raw 每条结论落到 HTML 原文、可执行产物或可复现命令。未用「上一轮就是这么判的」代替证据；B2/B4 票只作背景 |

---

## 5. 对抗式（grilling 自问自答）

❓ **Q1 — `--check` 绿是否证明类型合法、跨产物一致？**  
➡️ 现在：origin/main+修后生成器 `--check` 绿，且 §3A 170/170 为 `direct`、无 `inherited-or-location-scope`、CSV `business_process_step_records` 为 inherited。绿闸与 §1/手写清单不再互相证伪。001 的原反例已不存在。`--check` 仍只比字节，不能在「未来有人再写闭集外字符串」时单独因类型名红——那是字节锁，不是类型枚举器。不为「再加一层枚举校验」加需求：现输出已落在闭集内。

❓ **Q2 — `business_process_step_records` 是否仍会被兜底成 single_complex_default，从而 F1 跳过、F2 因已登记不红？**  
➡️ 否。CSV 该行现为 `inherited` + `process_instance_id`。F1 对 inherited 只信息性列出（本轮 KNOWN-BLINDSPOT B1 = 73，含该路径类表）。这是头注 B1 盲区，B2 已只登记。首次正常路径不再出现「与规格手写 inherited 矛盾的 scd」。

❓ **Q3 — F2 该红的真新表还会红吗？假表名 `d` 还会红吗？**  
➡️ 真新表：夹具 `totally_new_table` 被抽出。假表名 `d`：14 处 `IS NOT DISTINCT FROM d.col` 抽出 0 个 `d`；`--strict` 无 F2。默认路径把 baseline 里的 `d` 标为「已不再复现」。换个别名不再作为「新增回归」误阻断。002 原触发路径已死。

❓ **Q4 — 其它含 FROM 的 SQL 语法还会被误当表名吗？**  
➡️ 现扫描面：EXTRACT 已中和；DISTINCT FROM 已中和；FOR UPDATE SKIP/OF 在噪声表；CTE 名排除。Jdbc 源无 `TRIM(`/`SUBSTRING(`/`OVERLAY(` SQL。夹具显示 `TRIM(BOTH 'x' FROM col)` **会**把 `col` 当表——当前 53 个仓储没有该语法，首次正常跑不触发。按「不为理论场景加需求」维持观察，不立项。

❓ **Q5 — F1 该红时会不会不红？**  
➡️ `--strict` 仍对 `integration_channel_store_map`、`revenue_stream_attribution` 非 0，说明判定面未废。默认路径对这两条走棘轮、不红，是 L253–257 设计，B2 只登记。`hasScopedRead` 仍不中和 DISTINCT FROM（只 F2 抽表名中和）——这是文件级近似的一部分（头注 B3），现 `--strict` 未因此多出或少掉 F1。无新证据不改判。

❓ **Q6 — 测试库门禁正反例与静默 PASS？**  
➡️ 正例（裸 `complex`、带 query、Spring 默认占位）HIT；`complex_test` 不 HIT。现树全是 `complex_test` 故 PASS。`cwd=/tmp` 根目录缺失仍打印 PASS：B2 只登记，现树 npm 路径不触发。

❓ **Q7 — 写死 covered / 扫描面外 / baseline 陈旧键 / V1000 排序，有无新证据必须改判？**  
➡️ 无。`status` 仍全部 `covered`（335/335）。扫描面仍是 `Jdbc*Repository*.java` 53 个；本席用更宽启发式扫到 21 个含 SELECT/FROM 痕迹的非该 glob Java（含 `JdbcWecomWebhookNonceStore`、`JdbcTicketingSql` 等），与 B1「17」计数口径不同，头注 B4 仍成立，不升队。baseline 4 key 中 2 条 stale（`access_grant_requests`、`d`）属 M030。版本号仍三位，lex=num。`globalTables.roles` 仍因 `organization_id` 先走 direct（CSV `roles` = direct）。

❓ **Q8 — 主工作区因 V427 红，是回归吗？**  
➡️ 否。D-028。V427 增加 `business_entity_projects`（有 `project_id`）与 `merchants.project_id`。worktree 无 V427，检查绿。

---

## 6. 001 / 002 修复落地（独立重证，不抄 B4）

### M029-001

定稿最小处置：① `scopeType` 恒返回 `direct` 并注明依据；② `inheritedRules` 增 `business_process_step_records`；③ 版本标记；④ 机械重生成 2 单元格 + 1 行。

现码：`generate-data-isolation-spec.mjs:85-88` 无参 `return "direct"`；注释写明「本区块只收自带 scope 列的表：按规格 §1 定义即 direct」。`generate-domain-ownership-scope-audit.mjs:1` V0.04；`:27` 规则原文与定稿一致。  
现物：§3A 无闭集外类型；`venue_*` 为 direct；CSV 该行为 inherited。`--check` 双绿。  
分类回顾：细化（上位 HTML 红线④ / 治理 L653）。工程缺陷已不复现。

### M029-002

定稿最小处置：抽取前中和 `\bIS\s+(?:NOT\s+)?DISTINCT\s+FROM\b`；版本 V0.01；baseline `d` 键不动（M030）。

现码：`verify-tenant-isolation.mjs:149-150`。  
现跑：默认 stale `F2:UNREGISTERED_TABLE:d`；`--strict` 无 `d`、无其它 F2；53 文件抽出未登记 token 0。baseline JSON 仍含该 key（本模块未改，符合定稿）。  
分类回顾：细化。工程缺陷已不复现。

闸 3 归因不在 B5 范围（B4 已双 PASS）；B5 只确认整模块当前行为。

---

## 7. Findings（本轮须入队）

**无。** 临时 GR 项 0。未发现回归。未发现须新开 ID 的工程缺陷。

每条「看起来像问题」的现况均落在 B2 只登记清单，且本轮无新证据：

---

## 8. 观察（不立项；无新证据不改判）

B2-r1 双席定为只登记、本席当场复证仍成立：

1. CSV 写死 `status=covered`，uncovered 检查不可达（`generate-domain-ownership-scope-audit.mjs:205` + `:223-227`）。P0 盘点策略，不另立「未知表必须红」。
2. 头注 L10 fail-closed 与默认棘轮字面差；L253–257 已写棘轮，`--strict` 可用。
3. stale `F1:UNSCOPED_DIRECT_READ:access_grant_requests`：工具打印注意仍默认 PASS。属 M030／台账。
4. 已登记 F1：`integration_channel_store_map` / `revenue_stream_attribution`。`--strict` 仍红；默认棘轮不红。
5. DROP COLUMN／RENAME TABLE：现 DROP COLUMN 7 处均非 scope 列；无 RENAME TABLE。头注未声称支持。V350 用删旧建新。
6. 版本号三位时两生成器排序等价；V1000 才会分叉。现 395 文件 lex=num。
7. 测试库门禁根目录缺失静默 PASS、只扫四种后缀。`cwd=/tmp` 当场复现 PASS。现树与 npm 路径不触发。
8. `verify:test-db-isolation` 不在 `verify:delivery-gates`，在 `verify:predeploy`。package.json 非本模块文件。
9. 扫描面外含 SQL 的 Java（头注 B4）。本席宽口径 21 个文件 vs B1「17」，口径不同，不升队。
10. `globalTables` 的 `roles` 因 `organization_id` 先命中 direct 而不可达。CSV `roles` = direct。
11. CSV 行内 `file_version` 恒 V0.01（生成器文件头现 V0.04）。输出格式字段。
12. 主工作区因未发布 V427 三检查红（D-028）。
13. 四工具无自带单测（席甲 B1 OBS-7）。门禁即断言面。
14. `TRIM(… FROM <标识符>)` 夹具仍会把标识符当表名；现 Jdbc 仓储无该 SQL，不立项。
15. `findColumn` 以整段定义词元匹配（含注释文字）；本轮对库差集无误判。

ponytail-review（B5 不修）：`generate-domain-ownership-scope-audit.mjs:223-227` uncovered 检查仍零触发（`yagni:` 死分支）。`inherited-or-location-scope` 与假表名 `d` 的解析器问题已删/已中和。test-db 33 行：Lean already.  
`net: uncovered 检查可删则少约 5 行；不入队。`

---

## 9. 四分类总表（本模块内容，顺序：冲突 → 细化 → 技术性 → 添加）

| 对象 | 冲突？ | 细化？ | 技术性？ | 添加？ | 结论 |
|---|---|---|---|---|---|
| 四工具整体（生成隔离基线 / 机审 / 测试库 URL 门禁） | 否（与红线④、治理 L653 不矛盾） | 是（上位：BSM L707 红线④；PGM L653 默认按项目隔离、不提供关闭开关） | 不进入 | 不进入 | 细化；正常审计 |
| 001 已修的恒 `direct` + inherited 规则 + 产物 3 处 | 否 | 是（同上） | — | — | 已修 |
| 002 已修的 DISTINCT FROM 中和 | 否 | 是 | — | — | 已修 |
| 写死 covered、棘轮、扫描面、静默 PASS 等 | 否 | 是（同一上位的实现限制/政策） | 部分亦可技术性，但细化在先 | 否 | 只登记 |

添加项 0。无 questions。无 HTML 修改申请。

---

## 10. 步骤结论

**PASS**

- 全文 4/4 覆盖（745 行，SHA 实算），18 判据逐项有适用或无本面记录，三线已跑。
- 001、002 修复在现码与 origin/main worktree 实跑中落地：§3A 类型闭集内、CSV 步骤表 inherited、F2 不再把 `d` 当表；`--strict` 仍能因真实 F1 变红。
- 生成器输出对 `complex_test` 逐表逐列差集 = 未发布 V427 的 `business_entity_projects` + `merchants.project_id` + 系统表 `flyway_schema_history`。
- 工程缺陷新开 0；回归 0；B2 只登记项无新证据，不改判。
- 添加 0；questions 0；无 HTML 申请。
- 本票是 B5-r1 单席整模块结论，不是合取收口。合取须待席甲 raw 与四值校验。

本席未读本次 `M029-b5-r1-self-raw.md`。worktree `/home/james/wt-m029-b5r1-grok-ro` 已删除。
