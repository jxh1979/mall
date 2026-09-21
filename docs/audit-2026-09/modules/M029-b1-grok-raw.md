CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M029 B1 Grok 席乙完整报告

- 版本：V0.01
- 步骤：B1（修复前全模块三线全审；本模块首次开工，本版历史未审）
- 席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区 HEAD `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` `6f3d25d7bec4d1238455c91e6f2376936317980d`（M028 检查点 PR #469 已合并）。主工作区含已暂存未发布 `V427` 及其它模块暂存 Java；**本模块 4 个工具文件三态一致**（工作区 / HEAD / origin/main blob 相同，`git diff HEAD` 与 `git diff origin/main` 对该 4 路径均为空）
- 模块：`plan/modules.md` V0.04「租户范围与数据隔离（技术不变量）／检查及生成工具／第1段」；标记 NORMAL；计划有效内容 729 行（过期）；`git ls-files -z -- <4 paths> | xargs -0 cat | wc -l` → **741**
- 判据锁：`plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（未抄 sidecar / ledger / 其它报告）。sidecar 与 ledger 顶部同值，仅作交叉观察，不代替本席实算
- 映射 18 项：`M029 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]`（无 HTML 派生条目）
- findings.md / ledger：只读背景；ledger 记本模块 PENDING→IN_PROGRESS、B3 累计 0、无任何本版 M029 段/finding；M028 闭环把三表分类不一致与 `inherited-or-location-scope` 转入本模块。本席不改计划 / findings / ledger
- 独立性：未读 `M029-b1-self-raw.md`；未用 MCP / 子代理 / 第三席；未改业务代码 / 三 HTML / 锁 / 台账；未 fetch/commit/push/merge/deploy；除一次性 worktree 外未建脚本、日志或临时审计产物；psql 只 SELECT，口令只从 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD=` 行 `sed -n` 读取，未 source、未回显；未带 `--write-baseline` 运行
- `test -f .git/MERGE_HEAD` → **NO_MERGE**
- 结论：**FAIL**（4 文件全文覆盖、18 项逐项核对、三线完成；临时 GR 项 2，工程缺陷成立；无未答 questions。有待补项不得 PASS）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md，不是提一嘴。用户指令禁止子代理 / 第三席 / MCP，优先于 Arc 默认全量派席。

| 技能 | 存在性 | 本轮用法 |
|---|---|---|
| using-superpowers | 已读 | 开工技能路由；用户指令优先 |
| using-arc | 已读 | 用户禁止子代理/第三席，不走 Arc `audit` 派评审席，不写 `docs/arc/audits/` |
| graft | 已读并调用 | `graft skeleton` 两工具文件：`generate-data-isolation-spec.mjs`、`verify-tenant-isolation.mjs`；随后仍全文实读四文件（本模块强制全文、不抽样） |
| long-running-background-tasks | 已读 | 本轮 sha256 / node --check / psql / worktree 均前台完成，未挂 monitor |
| grilling / grill-me | 已读 | 只借追问结构自问自答，事实自读，不问人、不派子代理 |
| batch-grill-me | **缺口** | 已并入 grilling（mattpocock changelog：无独立技能可装）；不冒充调用 |
| ubiquitous-language | **缺口** | 已并入 domain-modeling；§6 已划掉 domain-modeling，不冒充调用 |
| codebase-design | 已读 | 四工具是浅接口（npm 脚本）+ 深不变量（迁移解析 / F1 F2 / 测试库 URL）；缺陷落在「声称检查的东西实际能不能红」 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现；B1 禁止修复、不加诊断桩、不对库执行写 |
| tdd / test-driven-development | 已读 | 用来判断门禁断言能否因目标行为变坏而红；本席不写测试 |
| verification-before-completion | 已读 | 先跑 origin/main worktree 实跑 / psql 现态 / 分类差集再下结论 |
| receiving-code-review | 已读 | 禁止表演性同意；本轮无对方 raw 可收 |
| requesting-code-review | 存在，B4 用 | B1 不适用（禁止派第三席） |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → NO_MERGE |
| git-guardrails-claude-code | 可选存在 | 只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B1 不修；review 口径扫可删项，见 §8 |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B1 不适用 |
| i-have-adhd | 存在 | 本席正文只落 raw；CLI 不回带结论或机器标记 |
| audit-context-building | 已读 | 要求派 function-analyzer 子代理：与「只允许两席」冲突，**不执行**，记缺口 |

graft 本轮有储蓄行的调用：`generate-data-isolation-spec.mjs` skeleton ≈1,422；`verify-tenant-isolation.mjs` skeleton ≈3,213。合计约 **4,635** tokens。

一次性 worktree：`git worktree add --detach /home/james/wt-m029-b1-grok-ro origin/main`（HEAD `6f3d25d7`），软链 `node_modules`，核完 `git worktree remove --force`，目录已不存在；worktree 内 tracked dirty=0，未改任何已跟踪文件。

---

## 1. 模块覆盖（全文实读，行数/SHA 实算）

命令：`wc -l -c` + `sha256sum` + `git hash-object` + `git rev-parse HEAD:<path>` + `git rev-parse origin/main:<path>`。

| 路径 | 物理行 | 字节 | SHA256 | git blob（工作区=HEAD=origin/main） |
|---|---:|---:|---|---|
| `tools/generate-data-isolation-spec.mjs` | 140 | 6664 | `14d5ca6b2083890691b4ac188a597395f9f2bbe7726fc4c596e83266e3076abf` | `24ce52464cef58fc96c722f8a347d656dd348977` |
| `tools/generate-domain-ownership-scope-audit.mjs` | 232 | 14174 | `ae8568fa8b1091f694a3462785088ee28023b757a41585298e6fa1af0ec14bfa` | `23c57f4c5a94638180505a4504daad21d9ce8858` |
| `tools/verify-tenant-isolation.mjs` | 336 | 16578 | `dfec43c3fcc65759297af89d805826fff9b7082e82615b905a00e73068d959e0` | `f5433cac0a22d33db0c73474fca75035ca92ef81` |
| `tools/verify-test-db-isolation.mjs` | 33 | 1445 | `6c36a678982b21189ff3a14961be56ff7dd945d10709cdccb9c44d1d4007ffea` | `642ebb0d3b72557cdbdf2d3dc272ceeaeb256060` |
| **合计** | **741** | **38861** | — | — |

覆盖：**4/4 全文逐行，不抽样**。

相关三 HTML（只读，铁律 4 未改；本模块映射无 HTML 派生 ID，HTML 只作第一性原理类型 1 与四分类上位）：

- `docs/business-system-map-for-operations-v0.1.html` L707 红线④原文：「看不到别的商户和别的项目的数据」；同条：持密钥最高权限只可逐笔开放、七类校验不设关闭开关
- `docs/platform-governance-map-for-operations-v0.1.html` L653：「所有业务数据默认按项目隔离……数据的项目与商户隔离不提供关闭开关」
- `docs/business-enum-quick-reference-v0.1.html`：无「租户隔离列 / scope_type / 机审闸口」枚举锚点；HTML 沉默处不反推分类名或门禁实现

D-028 / M028 闭环已核实：主工作区直接跑两个生成器 `--check` 与 `verify-tenant-isolation` 因未发布 V427（及暂存 Java 引用 `business_entity_projects`）而红，**不是本模块缺陷**。工具实跑证据取自 origin/main worktree。`complex_test` 已应用 V427+V428：多出 `business_entity_projects` 与 `merchants.project_id` 属预期。

---

## 2. 取证块（类型 2 / 4）

### 2.1 origin/main worktree 实跑（证据环境）

worktree：detached `6f3d25d7`，无 V427，有 V428，四工具 blob 与主工作区相同。

```
node tools/generate-data-isolation-spec.mjs --check
→ exit 0  data isolation spec is in sync with Flyway migration scope baseline.

node tools/generate-domain-ownership-scope-audit.mjs --check
→ exit 0  DOMAIN_OWNERSHIP_SCOPE_PASS: 335 tables classified

node tools/verify-tenant-isolation.mjs
→ exit 0  扫描 CSV 335 表（direct=173，白名单豁免=83）；Jdbc*Repository 53 文件；
          本次 finding 4（baseline 4，新增回归 0）；inherited 信息性 72 表；
          stale baseline 1 条：F1:UNSCOPED_DIRECT_READ:access_grant_requests
          PASS（棘轮）：无新增回归

node tools/verify-tenant-isolation.mjs --strict
→ exit 1  4 项：
          F2:UNREGISTERED_TABLE 表 'd' @ JdbcAnalyticsRepository.java
          F2:UNREGISTERED_TABLE 表 'd' @ JdbcSettlementConfigWriteRepository.java
          F1:UNSCOPED_DIRECT_READ integration_channel_store_map (merchant_id) @ JdbcIntegrationRepository.java
          F1:UNSCOPED_DIRECT_READ revenue_stream_attribution (project_id) @ JdbcSettlementConfigWriteRepository.java

node tools/verify-test-db-isolation.mjs
→ exit 0  TEST_DB_ISOLATION_PASS
```

`--strict` 的两条 F1（`integration_channel_store_map` / `revenue_stream_attribution`）是扫描器对 direct 表缺 `scope_col = :` 的真实命中，已在 `security-scans/tenant-isolation-baseline.json` 登记；属棘轮已知缺口，**不作为本模块新缺陷重复立项**（扫描面声称与代码一致：找到了、默认不因它们红）。`access_grant_requests` 的 F1 已不再复现（stale），工具只打印注意、不失败。

### 2.2 主工作区实跑（D-028 对照，不作本模块缺陷）

```
node tools/generate-data-isolation-spec.mjs --check          → exit 1  out of sync（V427）
node tools/generate-domain-ownership-scope-audit.mjs --check → exit 1  out of sync（V427）
node tools/verify-tenant-isolation.mjs                       → exit 1  新增 F2 business_entity_projects
                                                                  @ JdbcPlatformRepository.java
node tools/verify-test-db-isolation.mjs                      → exit 0
```

### 2.3 psql 只 SELECT（complex_test）

`psql -h 127.0.0.1 -U complex -d complex_test`，口令只读 `COMPLEX_DATABASE_PASSWORD=` 行。

- `current_database()=complex_test`，`current_user=complex`
- flyway 最新成功版本：428，其次 427
- `to_regclass('public.business_entity_projects')` → true；`merchants.project_id` 列存在
- 含 SCOPE 六列的基表 **172**；origin/main 规格生成器解析 **170**
- 差集（DB 有、生成器无）恰好 V427：`business_entity_projects=project_id`、`merchants=project_id`
- 重叠 170 表的六列集合 **无列差**
- CSV 335 行 vs DB 业务基表 336（不含 `flyway_schema_history`）；CSV 缺的唯一表是 `business_entity_projects`
- 173 张 CSV `direct` 的 `scope_source` 列在 DB 中 **全部存在**（无 findColumn 把引用列名误当本表列的现例）

命名三表现列：

| 表 | DB 相关列 | 规格 §3A | CSV |
|---|---|---|---|
| `business_process_step_records` | 仅 `process_instance_id`（无六列） | 不出现（无 SCOPE 列） | `single_complex_default` |
| `venue_business_hours` | `venue_id` | `inherited-or-location-scope` | `direct` / `venue_id` |
| `venue_sessions` | `venue_id` | `inherited-or-location-scope` | `direct` / `venue_id` |

手写规格 §3 L50：`business_process_step_records` 为 `inherited`（经 `process_instance_id` join 父表）。规格 §1 L21–24 类型闭集：`direct` / `inherited` / `single_complex_default`（另 CSV 使用 `global_reference`，§1 未列）。

### 2.4 迁移解析对真实写法

origin/main 迁移：CREATE TABLE 命中 348，两生成器均 **0 条未匹配**；无 quoted/schema/UNLOGGED/AS SELECT/单行 CREATE；无 RENAME TABLE；无对 SCOPE/direct 列的 DROP COLUMN。V350 注释已写明仓内生成器不认 RENAME、故用删旧建新。**当前仓库第一次正常跑不会撞上 DROP COLUMN/RENAME 漏解析** → 不立项，作观察。

CSV 生成器 `readMigrations().sort()` 为文件名字典序；版本号均为 3 位零填充，与规格生成器按 `Number(V)` 排序此刻等价。V1000 才会分叉 → 观察，不立项。

### 2.5 package.json 接线（只读）

| 脚本 | 命令 | 聚合 |
|---|---|---|
| `docs:data-isolation-spec` | `node tools/generate-data-isolation-spec.mjs` | 写规格自动区块 |
| `verify:data-isolation-spec` | 同上 `--check` | `verify:delivery-gates` 与 `verify:predeploy` |
| `docs:domain-ownership-scope` | `node tools/generate-domain-ownership-scope-audit.mjs` | 写 CSV |
| `verify:domain-ownership-scope` | 同上 `--check` | 同上两聚合 |
| `verify:tenant-isolation` | `node tools/verify-tenant-isolation.mjs`（**不带 --strict**） | 同上两聚合 |
| `verify:test-db-isolation` | `node tools/verify-test-db-isolation.mjs` | **仅** `verify:predeploy`（不在 `verify:delivery-gates`） |

`ops/160/complex-deploy-web.sh` 与 `complex-deploy-api.sh` 均列出 tenant-isolation 与 test-db-isolation。`package.json` 不在本模块文件清单，接线事实只登记，不对其立项。

---

## 3. 第一性原理三问

### 3.1 为什么必须存在？删掉它，三份 HTML 哪一条做不到？

四工具都不在三 HTML 里点名。上位是治理地图 L653「所有业务数据默认按项目隔离……隔离不提供关闭开关」与业务地图 L707 红线④「看不到别的商户和别的项目的数据」。删掉它们，HTML 条文本身仍在；失去的是「迁移→盘点 CSV/§3A」和「Jdbc 仓储 SQL 机审 / 测试库 URL 边界」的可执行守卫。答得上有业务上位 → **细化**，不是无主添加。

### 3.2 声称的行为有没有被实际执行验证？

有。origin/main worktree：两生成器 `--check` 绿、默认 tenant-isolation 棘轮绿、`--strict` 红 4 项、test-db 绿。主工作区因 V427 红，与 D-028 一致。psql 对照证明：在扣除 V427 预期差后，规格生成器 170 表六列与 `complex_test` 一致；CSV 335 表与库差 1 张 V427 表。

声称与实跑不一致之处见 GR-01 / GR-02（`--check` 绿并不能证明类型合法，F2 会把 `IS NOT DISTINCT FROM` 右侧别名当成表）。

### 3.3 哪个前提没验证就当真？

1. 「两生成器对同一迁移事实源给出可与规格 §1 对齐的 `scope_type`」——未验证，且已被现生成物证伪（GR-01）。
2. 「F2 抽出的 token 都是表」——未验证；`EXTRACT(... FROM ...)` 已中和，`IS NOT DISTINCT FROM` 未中和（GR-02）。
3. 「默认 `verify:tenant-isolation` = fail-closed、发现即非 0」——文件头 L10 如此写，默认路径是棘轮；`--strict` 才是全量硬失败。头注后文 L253–257 已说明棘轮，B1–B4 盲区与代码一致，不把棘轮政策本身当新缺陷。
4. 「Jdbc*Repository.java 覆盖全部带 SQL 的 Java」——头注 B4 已登记否。本席数得：扫描面 53；其它 Java 1145；其中 17 个含 SQL 痕迹（明确仓储/SQL 宿主含 `VenueBusinessHoursRepository`、`RevenueStreamAttributionRepository`、`JdbcTicketingSql`、`TicketFacePhotoRepository`、`JdbcAccessCredentialProvisioner`、`JdbcWecomWebhookNonceStore`）。不重复立项。

---

## 4. 顺从式：18 项逐条

每条先抄定义要点，再写本面适用/无本面及证据。

| ID | 定义要点 | 本面 | 证据 |
|---|---|---|---|
| GEN-001 | 有可验证用途；不为假设场景加抽象 | **适用** | 四工具各有 npm 入口与失败出口。不把 DROP COLUMN/RENAME/缺测试目录/V1000 排序分叉立成需求（当前迁移无这些写法、测试目录存在）。GR-01 的未定义类型是现生成物，不是假设。 |
| GEN-002 | 声明入口/主路径/错误路径须能执行且与声称一致 | **适用，有缺陷** | 入口均能跑。`--check` 只比字节，不能因 §1 未定义类型而红（GR-01）。F2 声称「未登记表」实际会命中 `IS NOT DISTINCT FROM d.col`（GR-02）。test-db 正例（裸 `complex`、Spring 默认占位）HIT、`complex_test` 不 HIT，与头注一致。 |
| GEN-003 | 测试须执行目标行为且能失败 | **适用（弱）** | 本模块无单测文件；「测试」即四条 npm 门禁。test-db 与两 `--check` 有能失败断言。tenant-isolation 默认对 baseline 内 finding 不能失败；`--strict` 能。CSV `--check` 的 `uncovered` 分支因 `status` 写死 `covered` 不可达（观察，不立项）。 |
| GEN-004 | 不可信输入在首次使用前校验 | **适用，无缺陷** | 输入是本仓迁移/CSV/Java/测试源，非用户请求。CSV 缺文件 / 缺列 / baseline JSON 坏 → FATAL 非 0。test-db 对 ROOTS 缺失 `try/catch` 忽略（目录现存，不立项）。 |
| GEN-005 | 受保护操作服务端授权 | **无本面** | 四文件无鉴权/角色实现。F1 只静态看仓储是否提供 scoped 读通道，不判定 token。 |
| GEN-006 | 状态/唯一/引用由事务或约束保护 | **无本面** | 工具不写库（`--check` / verify 只读；生成器写文档）。未跑 `--write-baseline`。 |
| GEN-007 | 可重试路径幂等 | **无本面** | 无并发写入；`--check` 是纯函数比较。 |
| GEN-008 | 跨层名称/类型/枚举一致 | **适用，有缺陷** | 规格生成器类型 ∈ {`direct`,`inherited-or-location-scope`}；CSV ∈ {`direct`,`inherited`,`global_reference`,`single_complex_default`}；规格 §1 ∈ {`direct`,`inherited`,`single_complex_default`}。三表分类不一致（GR-01）。 |
| GEN-009 | 错误可检测、不可伪成功 | **适用** | `--check` 不同步非 0；F2 新表（主工作区 `business_entity_projects`）非 0。伪成功风险：默认棘轮把 F2:`d` 当已知缺口（GR-02）；`--check` 对非法类型伪成功（GR-01）。 |
| GEN-010 | 无注入/越权/秘密泄露 | **适用** | 工具本身不拼用户 SQL。test-db 挡测试源生产库 URL。口令未回显。F1 是隔离机审主路径，对已登记 F1 缺口按棘轮放行（政策，头注已写）。 |
| GEN-011 | 关键操作可关联证据 | **适用** | 闸口 stdout 含表名/文件/key；baseline 有 key。findings-register 只有系列 ID `SEC-P0-TENANT-ISOLATION-001`，并非每条 baseline key 一条 SEC-*（工具注释略满，观察）。 |
| GEN-012 | 循环/查询有界 | **适用，无缺陷** | 扫描范围有界（迁移目录 / Java 树 / `backend/src/test`）。 |
| GEN-013 | 依赖与脚本可复现 | **适用** | 四脚本在 package.json；CI/predeploy 已接。test-db 不在 `verify:delivery-gates`、在 `verify:predeploy` 与 deploy 脚本（接线事实，package.json 非本模块文件）。 |
| GEN-014 | 迁移顺序确定、与模式一致 | **适用（只读消费迁移）** | 规格生成器按版本号数值排；CSV 按文件名排（当前 3 位等价）。解析结果与 `complex_test` 扣除 V427 后一致。 |
| GEN-015 | UI 可访问 | **无本面** | 无界面。 |
| GEN-016 | 文档路径/命令/状态可复现 | **适用，有缺陷** | `npm run verify:*` 命令可复现。规格 §3A 写入 §1 没有的类型名，且 `--check` 把它锁成「同步」（GR-01）。文件头 B1–B4 与代码一致，不重复立项。 |
| GEN-017 | 冲突→细化→技术性→添加 | **适用** | 本席 findings 按该序；均能指到 HTML 隔离红线/治理地图 L653 → 细化。无添加项，不触发 2.2。 |
| GEN-018 | 结论落到四类证据之一 | **适用** | 本报告命令输出 + 文件行 + HTML 原文；不用「历史如此」或他席意见。 |

---

## 5. 对抗式（grilling 自问自答）

❓ **Q1 — `--check` 绿是否等于分类正确？**  
➡️ 否。`--check` 只比较将要写出的字节与现文件。origin/main 两生成器均 exit 0，同时 §3A 含未定义类型、CSV 与 §3A 对 `venue_*` 类型不同。绿闸不能证伪 GR-01。

❓ **Q2 — 新表没有 scope 列时，哪道检查会红？**  
➡️ 只要跑生成器写出 CSV，该表落入 `single_complex_default` + 写死 `status=covered`：`--check` 的 uncovered 分支永不走；F1 跳过非 direct；F2 因已登记而不红。这是兜底分类的现行为。与 HTML「默认按项目隔离」相比偏松，但是 P0 单综合体口径在规格 §1 已定义 `single_complex_default`。**不为「将来多综合体」补新判据**；只把与规格/CSV 现文矛盾的三表写入 GR-01。

❓ **Q3 — F2 会不会在该红时不红？**  
➡️ 真新表未登记：会红（主工作区 `business_entity_projects` 实证）。假表名 `d`：本该不是 finding，却被扫出后塞进 baseline，默认路径不红（GR-02）。F1 对已 baseline 的表加更多无 scope 读：key 仍是表级，不红——棘轮按表而非按查询，头注/代码已写，不立项。

❓ **Q4 — F1 的判定面是否等于「每条 SELECT 都注入 token scope」？**  
➡️ 否。判定面=某 direct 表在任一 Jdbc*Repository 里出现 `FROM <table>`，且**同一文件某处**出现 `<scope_col> = :` 或 `IN (`。JOIN-only、写路径、Service 门控、动态 SQL 均不在判定面。头注 F1 定义 + B2/B3/B4 与代码一致。`venue_business_hours` 在 `JdbcTicketingRepository` 的查询本身带 `venue_id = :venue_id`，即使换成逐条 SELECT 也会过。

❓ **Q5 — 测试库门禁能否被非 Java 夹具绕过？**  
➡️ 只扫 `backend/src/test` 下 `.java|.yml|.yaml|.properties`。当前该树后缀只有 `.properties`(1) + `.java`(269)，无其它类型含 `jdbc:postgresql:`。缺根目录时静默 PASS（代码 L26），但根目录存在。正例 HIT 裸 `complex` 与 `${COMPLEX_DATABASE_URL:.../complex}`；反例 `complex_test` 不 HIT。不立项。

❓ **Q6 — 头注「fail-closed，发现即非 0」是否被代码打脸？**  
➡️ 开篇 L10 与默认棘轮字面冲突；同文件 L253–257 把棘轮写成设计。`--strict` 存在且本席已跑红。按「头注已登记的政策不重复立项」，记观察不立项。

---

## 6. 工具审计要点（与代码对照）

### 6.1 规格生成器 `generate-data-isolation-spec.mjs`

- 用途：从 Flyway 目录生成规格 `<!-- DATA_ISOLATION_SPEC_AUTO_* -->` 区块；`--check` 比字节。
- CREATE：`CREATE TABLE … ( … \n) ;`（要求 `)` 前有换行）；现仓 348/348 命中。
- ALTER：任意 `ALTER TABLE` 后再找 `ADD COLUMN`；不处理 DROP COLUMN。
- DROP TABLE：按版本序 `tables.delete`。
- 分类：六列中出现 `merchant_id|project_id|organization_id` → `direct`，否则 **`inherited-or-location-scope`**。现仓后者仅 2 表（`venue_business_hours` / `venue_sessions`，均只有 `venue_id`）。从不发射 `inherited` / `single_complex_default` / `global_reference`。
- 对 `complex_test`：扣除 V427 后逐表逐列一致。

### 6.2 CSV 生成器 `generate-domain-ownership-scope-audit.mjs`（文件头 V0.03）

- 分类次序（写死）：`staff_policy_settings` 特例 → analytics 4 表覆盖 → `findColumn(directColumns)`（**首次命中**：`project_id` 先于 `venue_id` 先于 `merchant_id` …）→ `globalTables` → `inheritedRules` 首条正则 → **兜底 `single_complex_default`**。
- `directColumns` 含 `venue_id` 与一批 `*_entity_id`，**不含** `bar_venue_id` / `space_id`（与规格生成器六列不完全相同）。
- `status` 恒为 `"covered"`；`--check` 在字节同步之后再滤 `!line.endsWith(",covered")` → 对生成结果**永真**，该错误路径不可达。
- `globalTables` 含 `roles`，但 `roles` 有 `organization_id`，在 direct 步已被截走，该白名单项当前不可达。
- `inheritedRules` 无 `business_process_step_records`，故落入兜底（与手写规格 §3「inherited」不一致）。

### 6.3 `verify-tenant-isolation.mjs`

- 权威表清单=CSV。扫描= `/^Jdbc.*Repository.*\.java$/`（53 文件）。
- **F1**：仅 `scope_type==="direct"`；读=文件内 `\bFROM\s+<table>`（不含 JOIN）；谓词=同文件 `<scope_col> = :` 或 `IN (`；`scope_col` 空则 `return true`（放行）。
- **F2**：FROM/JOIN/INTO/UPDATE 的标识符，减 CTE / SQL 噪声 / EXTRACT FROM / 基础设施白名单。
- 默认阻断面= baseline 之外的 finding；`--strict` 才全量。CI 脚本不传 `--strict`。
- 头注 B1–B4 与代码一致（inherited 信息性、写路径不做、同文件近似、非 Jdbc*Repository 不扫）。
- 扫描面外带 SQL 痕迹的 Java：17（见 §3.3），不重复立项。

### 6.4 `verify-test-db-isolation.mjs`

- 根：`backend/src/test`；类型：java/yml/yaml/properties；正则：`jdbc:postgresql://…/complex(?!_test)\b`。
- 正例 HIT：裸生产 URL、`${COMPLEX_DATABASE_URL:jdbc:postgresql://…/complex}`。
- 反例不 HIT：`complex_test`、`complex_testing`、`complexdb`、非 jdbc 的 `COMPLEX_DATABASE_NAME=complex`。
- 注释里写生产 URL 也会 HIT（当前测试树无此行）。
- 缺根目录：catch 后 0 hits → PASS。现目录存在，不立项。

---

## 7. Findings

判定顺序每条都走完：冲突（无 HTML 明文矛盾）→ **细化**（治理地图 L653 / 业务地图红线④ 的下位机审实现）→ 故不入技术性/添加。

### GR-01 — 两生成器分类不一致；规格自动区块写入 §1 未定义的 `inherited-or-location-scope`

- **文件:行**
  - `tools/generate-data-isolation-spec.mjs:84-88`（`scopeType` 兜底）
  - `tools/generate-domain-ownership-scope-audit.mjs:10-20`（`directColumns` 含 `venue_id`）
  - 同文件 `:148-188`（`classify`：direct 列优先，否则 inheritedRules，否则 `single_complex_default`）
  - 同文件 `:196-205`（每行 `status: "covered"`）
  - 生成物（origin/main 与主工作区副本一致）：`docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:21-24`（§1 类型闭集）、`:50`（手写 inherited）、`:225-226`（§3A 两行）、`docs/domain-ownership-scope-audit-v0.1.csv:79`、`:324-325`
- **原文（工具）**

```
function scopeType(columns) {
  const names = [...columns.keys()];
  if (names.includes("merchant_id") || names.includes("project_id") || names.includes("organization_id")) return "direct";
  return "inherited-or-location-scope";
}
```

更长上下文：规格生成器 SCOPE 六列含 `venue_id`，但 `scopeType` 不把仅有 `venue_id` 的表标 `direct` 或 `inherited`，只发射自造类型。CSV 生成器把 `venue_id` 当作 direct 列，又对无 direct 列且未进 `inheritedRules` 的表写死 `single_complex_default` + `covered`。

规格 §1：「scope 类型（与盘点 CSV 的 `scope_type` 对齐）：`direct` / `inherited` / `single_complex_default`。」无 `inherited-or-location-scope`，亦无「仅有 venue_id = direct」。

- **正常触发 / 影响**：`npm run docs:data-isolation-spec` 后 `--check` 仍绿，把非法类型锁进规格自动区块。现仓仅 `venue_business_hours`、`venue_sessions` 走该兜底（均 `venue_id`）；CSV 标 `direct`。`business_process_step_records` 无六列故不进 §3A，CSV 标 `single_complex_default`，手写 §3 标 `inherited`。F1 只审 direct：venue 两表被 CSV 收进 F1（现查询带 `venue_id = :`，F1 不红）；步骤表被当成 scd，与 inherited 一样跳过 F1，机审行为碰巧相同，**盘点类型仍互相矛盾**。`--check` 不能因类型非法或跨工具不一致而失败。
- **最小拟处置**：规格生成器 `scopeType` 只允许 §1/CSV 已有取值；仅有 `venue_id`/`bar_venue_id`/`space_id` 的表与 CSV 用同一规则（并与 §1「继承到 merchant」对齐）。CSV `inheritedRules` 补上规格已写明 join 路径的 `business_process_step_records`（或删除写死 `covered` 让未裁决表能红）。然后在 origin/main 口径重跑两 `--check`。不改 HTML。
- **四分类**：冲突？否（HTML 未写这些类型名）。细化？是（隔离盘点是红线④ / L653 的下位实现）。故 **细化**。
- **工程缺陷是否成立**：**是**（现生成物即可复现，非理论路径）。
- **冻结 ID**：GEN-008、GEN-002、GEN-016
- **依据类型**：2+4（worktree `--check` 绿 + `--strict` 无此类型项；§3A/CSV 现文；psql 三表列）

### GR-02 — F2 把 `IS NOT DISTINCT FROM <别名>.col` 当成表名；`d` 被 baseline 吞掉

- **文件:行**
  - `tools/verify-tenant-isolation.mjs:145-167`（`extractReferencedTables`：中和 EXTRACT FROM，然后 `\bFROM\s+TABLE_TOKEN`）
  - 同文件 `:10-16`（F2 定义：FROM/JOIN/INTO/UPDATE 引用的表）
  - 触发 SQL（只作复现，非本模块修复面）：`backend/src/main/java/com/complex/platform/analytics/JdbcAnalyticsRepository.java:1246`；`backend/src/main/java/com/complex/platform/settlement/JdbcSettlementConfigWriteRepository.java:485`
  - 棘轮：`security-scans/tenant-isolation-baseline.json:9-11` key `F2:UNREGISTERED_TABLE:d`
- **原文**

```
  sql = sql.replace(/\bEXTRACT\s*\(\s*\w+\s+FROM\b/gi, "EXTRACT(");
  const patterns = [
    new RegExp(`\\bFROM\\s+${TABLE_TOKEN}`, "gi"),
```

更长上下文：`JdbcAnalyticsRepository` 自连接别名 `d`，谓词 `l.merchant_id IS NOT DISTINCT FROM d.merchant_id`。运算符里的 `FROM` 加别名 `d` 被当成表。EXTRACT 已中和，本运算符未中和。`--strict` 两次报 `表 'd'`（同一 key）。默认路径因 baseline 含该 key 而 PASS。

- **正常触发 / 影响**：凡 Jdbc*Repository 写 `IS NOT DISTINCT FROM <ident>` 且 ident 不是已登记表，F2 报未登记表。ident=`d` 时被棘轮吞掉，默认不红；换个别名会作为「新增回归」阻断（该绿却红）。真新表未登记仍会红（主工作区 V427 实证），故不是「未登记表全都不红」，是 F2 判定面混入了 SQL 运算符。
- **最小拟处置**：在抽取表名之前像 EXTRACT 一样中和 `\bIS\s+NOT\s+DISTINCT\s+FROM\b`。不把 `d` 继续当隔离缺口。baseline 的处理属 M030，本模块只修扫描器。
- **四分类**：冲突？否。细化？是。**细化**。
- **工程缺陷是否成立**：**是**（`--strict` 每次复现）。
- **冻结 ID**：GEN-002
- **依据类型**：4（`--strict` stdout 含 `表 'd'`；源码行；baseline JSON）

无添加项。无 HTML 冲突项。无 questions。

---

## 8. 观察（不立项）

1. CSV `--check` uncovered 分支因 `status` 写死 `covered` 永真（`generate-domain-ownership-scope-audit.mjs:201-205` + `:223-227`）。兜底 `single_complex_default` 使「不会分类」不会失败。当前是 P0 盘点策略，不另发明「未知表必须红」的新判据。
2. `verify-tenant-isolation.mjs:10`「fail-closed，发现即非 0」与默认棘轮字面冲突；L253–257 已写棘轮；`--strict` 可用。
3. stale baseline `F1:UNSCOPED_DIRECT_READ:access_grant_requests`：工具打印注意仍 PASS。findings-register 称该条是服务层闸、静态器不识别。
4. F1 已登记的 `integration_channel_store_map` / `revenue_stream_attribution`：扫描器找得到，默认不红。属棘轮已知缺口，不在本模块重复立项。
5. DROP COLUMN / RENAME TABLE：现迁移无 SCOPE 列删除、无 RENAME TABLE；V350 已用删旧建新绕开。文件头未声称支持 RENAME。
6. 版本号 3 位时两生成器排序等价；V1000 才会分叉。
7. test-db：缺根目录静默 PASS；只扫四种后缀。现树不触发。
8. `verify:test-db-isolation` 不在 `verify:delivery-gates`，在 `verify:predeploy` 与 deploy 脚本。package.json 非本模块文件。
9. 扫描面外 17 个含 SQL 痕迹的 Java：头注 B4。
10. `globalTables` 的 `roles` 因 direct 列先命中而不可达。
11. CSV 行内 `file_version` 恒 `V0.01`，生成器文件头 V0.03：输出格式字段，不是声称漂移。
12. 主工作区两 `--check` 与 tenant-isolation 因 V427 红：D-028，非本模块缺陷。

ponytail-review（只列可删，B1 不修）：`generate-domain-ownership-scope-audit.mjs:223-227` uncovered 检查（写死 covered，零触发）；`inherited-or-location-scope` 自造类型（GR-01）；baseline 里的表名 `d`（应修解析器，GR-02）。test-db 33 行：Lean already.

---

## 9. 步骤结论

**FAIL**

- 全文 4/4 覆盖，18 判据逐项有适用或无本面记录，三线已跑。
- 工程缺陷 2 条（GR-01、GR-02），均细化、均有冻结 ID 与类型 2/4 证据，首次正常跑可复现。
- 有待补项 → 不得 PASS。
- 无添加项、无 HTML 修改申请、无 LOCK-AMEND 主张。

本席未读 `M029-b1-self-raw.md`。worktree 已删除。

## M029 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **32315** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6/xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（M029 FILES 4：两生成器 + 隔离机审 + 测试库门禁；生成物规格/CSV 属 M028 FILES）。
- 正本：`/home/james/complex-management` main 主工作区。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `6f3d25d7bec4d1238455c91e6f2376936317980d`（未 fetch）。B3=0。无源修复。NO_MERGE。
- 席甲：Claude Fable 5.1，作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类，**不读**席甲 raw 在封存 16960 字节之后的 B2 结论段。
- 本席 B2-r1：**PASS**（001–002 逐 ID 全 PASS；其余观察只登记不入队 PASS；添加 0、不发 2.2、无 HTML 申请、无 questions）
- 正文实算锁（非独占行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，内容同值；ledger 顶行同值，LOCK-AMEND-COUNT 1。本段未新增独占锁哈希字段行。独占锁哈希字段行仍仅文首一处。
- 无 MCP、无子代理、无第三席、不问操作员。未改源码/迁移/HTML/lock/findings/ledger；未 fetch/commit/push/merge/deploy；未写本指定路径以外的任何文件。数据库未连、无任何写语句。本步核源为文件/迁移/CSV/规格自动区块/HTML 只读。

此票只判断 findings.md V2.91 顶部「当前 M029 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。不加判据。B2 不写任何新的独占 CRITERIA-LOCK-SHA256 行。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M029-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 32315 | `8f4c619b9cb3f2a14695a8914eac5b03eef2752bfe264ac8063975b06d0ea3df` |
| `docs/audit-2026-09/modules/M029-b1-self-raw.md` 前缀（席甲 B1 V0.01；本席只读此前缀） | V0.01 | 16960 | `27a366a09fd3f04bf4325e7e08df08fefaa2a8bf9fa49fe803049ba80ba3b672` |
| `docs/audit-2026-09/findings.md`（全文件；只采顶部「当前 M029 B2-r1 整批候选」至该节结束） | V2.91 | 1100482 | `671da1321fb4250a7ddd77acc4ebe5954520793941fd0c35ad19ab8ca5f5e2ca` |

席甲 raw 全文现大于 16960 bytes；本席用前 16960 字节得与封存值相同的 SHA，其后自 B2 标题起不作为本票依据。封存前缀内 U+FFFD = 0。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

controller / runbook / modules.md 只读核验（本席未改）：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |

三 HTML 只读核验（本席未改；001/002 细化上位用治理地图 L510 / L653 与业务地图 L707）：

| 文件 | SHA256 |
|---|---|
| `docs/business-system-map-for-operations-v0.1.html` | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| `docs/business-enum-quick-reference-v0.1.html` | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

对象文件（未改）：`tools/generate-data-isolation-spec.mjs` 140 行 SHA `14d5ca6b2083890691b4ac188a597395f9f2bbe7726fc4c596e83266e3076abf`；`tools/generate-domain-ownership-scope-audit.mjs` 232 行 SHA `ae8568fa8b1091f694a3462785088ee28023b757a41585298e6fa1af0ec14bfa`（文首 `// 文件版本：V0.03`）；`tools/verify-tenant-isolation.mjs` 336 行 SHA `dfec43c3fcc65759297af89d805826fff9b7082e82615b905a00e73068d959e0`；`tools/verify-test-db-isolation.mjs` 33 行 SHA `6c36a678982b21189ff3a14961be56ff7dd945d10709cdccb9c44d1d4007ffea`。四工具相对 `origin/main` 无 diff。`questions/M029*`：不存在。候选段止于「修复队列『否』（0）。」；其下他模块历史段不重审。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核差集/分类/队列/最小边界再接受或反驳；禁止表演性同意；B2 不修复。本批重点：OBS-3 撤回、恒 direct、生成物同批带入、IS 可选 NOT、三文件版本标记 |
| grilling | 只借追问结构自查：恒 direct 是否小于「与 CSV 同一规则」、M021 先例是否被 B4 改写、GEN-003 是否加判据、findColumn 观察是否漏并 |
| verification-before-completion | 声称前先跑只读哈希/前缀字节/自动区块 170 行解析/CSV 对照/V059 原文/`IS NOT DISTINCT FROM` 计数/HTML L510·L653·L707 |
| systematic-debugging / diagnosing-bugs | 只借读错→对照两 raw 与候选；不改生产代码、不重审模块结论 |
| codebase-design / ponytail-review | 最小是改生成器分类闭集与 F2 抽取前中和；不删写死 covered、不动 baseline、不扩 CSV directColumns |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| graft | 本步对三工具 `graft skeleton`：索引无 wiring graph，未当证据；分类/抽取按 B1 已给坐标直读源文件 |
| audit-context-building | **未用**（本体强制派 function-analyzer 子代理） |

### 完整差集（GR-01…02、席甲 OBS-1…7、本席观察 1…12）

漏并（findings）：无。改写原意：无。两席 B1 全部临时缺陷 ID 仍落在 001–002 或只登记段。无单席独有缺陷被丢。无本批新开「否」ID。无添加项。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M029-001 | GR-01（席甲 B1 只登记为 OBS-3；B2 撤回、采本席） | 细化/是/是（`scopeType` 恒 `direct`；`inheritedRules` 补步骤表；origin/main 口径重生成 2 单元格 + 1 行；不删 covered） | **PASS（细化/是/是）** |
| M029-002 | GR-02（席甲 B1 漏） | 细化/是/是（抽取前中和 `IS [NOT] DISTINCT FROM` 一行；baseline `d` 键属 M030 不动） | **PASS（细化/是/是）** |
| （无新 ID） | 两席其余观察 | 只登记不入队 | **PASS（只登记；无应升队项）** |

冲突入队 0。细化入队 2（001–002）。技术性入队 0。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001–002（2）；「否」本批新开 ID = 0。席甲 B1 票 PASS 经复核撤回，合取 FAIL——本席同意该撤回（OBS-3 的「牵动 M028 产物」是流程顾虑，不是对非法类型/跨产物矛盾的技术反驳）。

**整批候选：同意。**

---

### 一、差集核实（逐项独立）

**001 — GR-01（席甲 OBS-3 同一事实）。票：同意入队；同意撤回席甲只登记。**

同根三件，本步独立核源，不是重审模块：

1. `tools/generate-data-isolation-spec.mjs:84-88` `scopeType` 对没有 `merchant_id`/`project_id`/`organization_id` 的表返回 `inherited-or-location-scope`。规格 §1 L21–24 类型闭集只有 `direct` / `inherited` / `single_complex_default`，且写明与 CSV `scope_type` 对齐。本步解析 `<!-- DATA_ISOLATION_SPEC_AUTO_* -->`：**170** 张表，类型计数 `direct=168`、`inherited-or-location-scope=2`，非 direct 恰好 `venue_business_hours`、`venue_sessions`（列均为 `venue_id`）。
2. 自动区块 170 张表在 CSV 的 `scope_type` **全部为 direct**（≠direct 的 0 张；CSV 缺表 0）。走兜底的两张 CSV 均为 `direct` / `venue_id`。
3. `tools/generate-domain-ownership-scope-audit.mjs:22-65` `inheritedRules` 无 `business_process_step_records`。CSV L79 现为 `single_complex_default` / `P0_SINGLE_COMPLEX`。规格手写 §3 L50 与 V059 头注均为 `inherited`：经 `process_instance_id` join `business_process_instances` 继承 `merchant_id`/`project_id`。V059 原文：父表确有 `merchant_id` + `project_id`（另有 `venue_id`，不进入本条路径声称）；步骤表 `process_instance_id … REFERENCES business_process_instances(id)`，无六列 scope。该表不进自动区块（无 SCOPE 六列），只改 CSV 一行。

`--check` 只比字节，不能因类型非法或跨产物矛盾而红。席甲 OBS-3 记录了同一口径差，只因「改生成器会牵动已闭环 M028 产物」不立项。那不是「事实不成立」，是流程顾虑。M028-006 已把这三张表的分类差异与 `inherited-or-location-scope` **明示交给 M029**。撤回只登记、采本席 GR-01 入队，不改写原意。

**002 — GR-02（席甲漏）。票：同意入队。**

`tools/verify-tenant-isolation.mjs:145-167` 抽取前只中和 `\bEXTRACT\s*\(\s*\w+\s+FROM\b`，随后 `\bFROM\s+TABLE_TOKEN`。`JdbcAnalyticsRepository.java:1246` 与 `JdbcSettlementConfigWriteRepository.java:485` 为 `IS NOT DISTINCT FROM d.<col>`，别名 `d` 被当成表。`security-scans/tenant-isolation-baseline.json` 含 `F2:UNREGISTERED_TABLE:d`（该文件属 M030）。本步 `Jdbc*Repository` 中 `IS NOT DISTINCT FROM` **14** 处（analytics 1 + settlement-config 2 + settlement-bill 1 + customer 10）；`IS DISTINCT FROM`（无 NOT）0 处。席甲 B1 探针 `TENANT_GATE_STRICT_EXIT=1` 但 `[m029_t3s tail]` 只抄了两条 F1，未抄两条 `表 'd'`——漏报成立，不是事实不成立。未改写本席「F2 判定面混入 SQL 运算符」原意。

**其余观察 — 只登记。票：同意；无漏并缺陷、无应升队项。**

| 来源 | 落点 |
|---|---|
| 本席观察 1 / 席甲 OBS-1 | 写死 `status=covered`、uncovered 检查不可达 → 只登记（见 001 丙：不采删 covered） |
| 本席观察 2 | 头注 fail-closed 与默认棘轮字面差；L253–257 已写棘轮，`--strict` 可用 → 只登记 |
| 本席观察 3 / 席甲 OBS-5 | stale `F1:UNSCOPED_DIRECT_READ:access_grant_requests` → 只登记（M030／台账） |
| 本席观察 4 | 已登记 F1：`integration_channel_store_map` / `revenue_stream_attribution` → 只登记（棘轮已知缺口） |
| 本席观察 5 | DROP COLUMN／RENAME TABLE 现迁移无此情形 → 只登记 |
| 本席观察 6 / 席甲 OBS-6 前半 | 版本号 V1000 两生成器排序分叉 → 只登记 |
| 本席观察 7 / 席甲 OBS-2 | 测试库门禁根目录缺失静默 PASS、只扫四种后缀；npm 路径不触发 → 只登记 |
| 本席观察 8 | `verify:test-db-isolation` 不在 delivery-gates（package.json 非本模块文件）→ 只登记 |
| 本席观察 9 / 席甲 OBS-4 | 扫描面外 17 个含 SQL 的 Java；头注 B4 → 只登记 |
| 本席观察 10 | `globalTables` 的 `roles` 因 direct 列先命中不可达 → 只登记 |
| 本席观察 11 | CSV 行内 `file_version` 恒 V0.01 → 只登记 |
| 本席观察 12 | 主工作区因未发布 V427 三检查红（D-028）→ 只登记 |
| 席甲 OBS-7 | 四工具无自带单测 → 只登记 |
| 席甲 OBS-6 后半 | `findColumn` 以整段定义词元匹配（含注释文字）、现库对照无误判 → 只登记段未单独点名；与「现迁移解析无误判」同类，**不升队、不单开 ID，不构成漏并缺陷** |

无应把 covered 死检查、棘轮政策、扫描面 B4、测试库 cwd、V1000、D-028 升进本批队列的项。

---

### 二、分类理由

四分类顺序（GEN-017）：冲突 → 细化 → 技术性 → 添加。

- 001、002：均有治理地图 L510「商户不能查看平台全局和其他商户数据」、L653「所有业务数据默认按项目隔离……数据的项目与商户隔离不提供关闭开关」与业务地图 L707 红线④「看不到别的商户和别的项目的数据」可指。本模块是该隔离红线的下位机审/盘点实现：生成器写出的类型闭集与 F2「未登记表」判定面直接影响能否守住「表怎么分类、仓储引用了哪些表」。细化先于技术性。席甲 B1 GEN-017 把四工具写成「全部技术性」——按顺序不采；候选跟随本席 B1 细化。
- 无冲突：HTML 未写 `inherited-or-location-scope` 或 `IS NOT DISTINCT FROM` 这些实现名，不存在与明文不能同时成立的句子。
- 无添加：恒 `direct` 是 §1 L22 已有定义（表自带 scope 列）；补 `inheritedRules` 是把规格 §3 / V059 已写明的 join 路径写进 CSV 生成器；中和运算符是让 F2 与头注「FROM/JOIN/INTO/UPDATE 引用的表」一致。不发明新 scope 类型、不新造隔离键、不改 HTML。

工程缺陷：两条均为首次正常跑可复现（现生成物非法类型 + `--strict` 每次 `表 'd'`）。全部「是」。队列全部「是」。

---

### 三、最小处置边界（含用户点名的甲乙丙丁、002、版本标记）

写入集合＝本模块 **3** 个工具文件 + 两份生成物在生成器拥有区域的机械重生成（规格自动区块 2 个单元格、CSV 1 行）。`tools/verify-test-db-isolation.mjs` 不在写入集合。一次 B3 禁止改三 HTML、锁、台账、baseline、手写规格段、已应用迁移、仓储 Java。

#### 001 甲 — `scopeType` 恒返回 `direct`

规格 §1 L22：`direct`＝「表自带 scope 列（如 `merchant_id`），直接过滤。」L23：`inherited`＝「**无自带列**，通过外键 join」。L19 的「`venue_id` / `bar_venue_id` / `space_id` … 继承到 merchant」是**列的层级/来源**，不是把含这些列的表标成 scope_type=`inherited`。自动区块只收 `SCOPE_COLUMNS` 六列出现过的表（`organization_id`/`merchant_id`/`project_id`/`venue_id`/`bar_venue_id`/`space_id`），按 L22 即 direct。

本步实证：170 张自动区块表 CSV 全为 direct；缺 merchant/project/org 的只有 venue 两表，且自带 `venue_id`、CSV 已是 direct。把它们标 `inherited` 会与 L23「无自带列」矛盾。

本席 B1 写「只允许 §1/CSV 已有取值；仅有 venue_id/bar_venue_id/space_id 的表与 CSV 用同一规则」。对**现库存量**：恒 `direct` 就是与 CSV 同一规则，且不再发射闭集外的值——**满足**。

更小改法：只把兜底字符串改成 `"direct"`，if 两支都返回 direct，功能等价，但留下永远无区分的分支。候选「恒返回 + 注明依据（本区块只列自带 scope 列的表，按 §1 即 direct）」删掉误导性分支，仍属最小。

看似更准、不采：把 `venue_id` 加入 if、只含 `space_id`/`bar_venue_id` 的表标 `inherited`——与 L22/L23 矛盾；现自动区块无此类表。把 CSV `directColumns` 扩到 `space_id`/`bar_venue_id` 以「完全同一规则」——会改变 CSV 分类闭集与 F1 扫描面，超出本条、无现表可复现。列集合差留作观察。

#### 001 乙 — `inheritedRules` 增 `business_process_step_records`

同意。形状与现有规则一致：`pattern` 精确表名、`source: process_instance_id`、`path: process_instance_id -> business_process_instances -> merchant_id/project_id`。V059 头注与规格 L50 同句。F1 对 scd 与 inherited 均跳过，机审阻断面碰巧不变，盘点类型与手写清单对齐。不把父表 `venue_id` 写进 path（规格未声称那条继承）。

#### 001 丙 — 不采删写死 `covered`

同意。两席均登记为观察。删 covered 会要另造 status 来源，等于发明「未知表必须红」的新闸，铁律 6 禁止为极端情况加东西。当前 P0 兜底策略，不入本条。

#### 001 丁 — 机械重生成带入已闭环 M028 文件；B4 归因

**可接受，且同批带入是刚需。** 理由：

1. `--check` 比字节。只改生成器不重生成，origin/main 上两 `--check` 必红，缺陷面未闭合。
2. 变化被生成器拥有的标记围住：规格 `<!-- DATA_ISOLATION_SPEC_AUTO_START -->`…`END` 内 2 个「scope 类型」单元格 `inherited-or-location-scope`→`direct`；CSV `business_process_step_records` **1 行**（`scope_type`/`scope_source`/`scope_path`/`multi_complex_note` 随规则变，`file_version`/`status` 仍由生成器写死）。手写 §3/§6/§8 等 M028 已审段不动。
3. M028-006 已把这三张表的分类差异交给 M029，不是越界发明。
4. 先例：M021 B2 定稿写「随新迁移重生成这两份产物」。M021 B4 后来把重生成留到检查点，是因为 **V428 本身不改生成物**，且当时主工作区跑生成器会带入 V424–V427 漂移、那些行不能归 M021-001。本批相反：改的是生成器逻辑，不重生成则 `--check` 红；候选已写 **origin/main 口径**，正好避开主工作区未跟踪 `V427__m009_project_profiles_and_associations.sql`（`origin/main` 无此路径）。在主工作区跑会多出 `business_entity_projects` 与 `merchants.project_id`，那些行**不能**归 001。

**B4 归因：成立。** 修后 origin/main 口径产物里，自动区块那 2 个单元格与 CSV 那 1 行归 001；三工具文件的分类/规则/版本标记归 001/002。若 diff 出现手写段、V427 表/列、baseline、测试库门禁，即越界。

#### 002 — 抽取前中和 `IS [NOT] DISTINCT FROM`；baseline 不动

同意。与 EXTRACT 同样就地一行：`sql = sql.replace(/\bIS\s+(?:NOT\s+)?DISTINCT\s+FROM\b/gi, "IS_DISTINCT(")` 或等价吞掉 FROM 的替换。可选 NOT 比本席 B1 只写 `IS NOT DISTINCT FROM` 更完整，仍一行；现仓无 `IS DISTINCT FROM`（无 NOT）命中，不扩大修复面。不改 14 处仓储 SQL。baseline `F2:UNREGISTERED_TABLE:d` 属 M030；修后默认路径会按现逻辑打印「已不再复现」，清理不在本模块。

冻结 ID：GEN-002 为主。候选并列 GEN-003 不是加锁判据（GEN-003 已在本模块映射 18 项内）。弱适用：抽取器无单测；默认棘轮因 baseline 含 `d` 对假表名不能红。同意保留并列，不要求删。

#### 四、三个被改工具文件的版本标记

属同批最小处置，不是功能修复本身，也不超出最小。先例：本轮对「本次改动的本模块文件」加或升 `文件版本`（M025/M026 等）。候选拆开：001 给规格生成器**新增** `V0.01`（现仅 `// docs:data-isolation-spec / …`，无版本行）、CSV 生成器 `V0.03`→`V0.04`；002 给 `verify-tenant-isolation.mjs` 新增 `V0.01`。测试库门禁本批不改，不升版。不得改 CSV 行内写死的 `file_version` 列（观察 11）。

禁止：删写死 covered；改 baseline；改三 HTML；发 2.2；在主工作区带 V427 重生成；手改自动区块或 CSV 行（必须跑修后生成器）；改 `verify-test-db-isolation.mjs`；把 space_id/bar_venue_id 扩进 CSV `directColumns`；把 `d` 键的清理塞进本模块。

---

### 四、添加／HTML／questions

本批无添加项、无 HTML 修改申请、无 questions。`docs/audit-2026-09/questions/` 无 `M029*`。不 BLOCKED-ASK。不按铁律 4 申请改三 HTML。

只登记不入队（候选段，本席同意）：写死 covered；测试库门禁 cwd/后缀；test-db 不在 delivery-gates；fail-closed 与棘轮字面差；stale access_grant_requests 与两条已登记 F1；扫描面外 17 文件；DROP/RENAME；V1000 排序；`globalTables.roles`；CSV 行内 file_version；四工具无单测；D-028/V427。无应升本批缺陷。

### 分类／队列／允许的最小修复边界

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3 改本模块 3 个工具文件，并用修后生成器在 **origin/main 迁移闭集** 上重生成规格自动区块 2 单元格与 CSV 1 行（只动生成器拥有区域）；三文件版本标记如上。

禁止：主工作区带 V427 重生成；手改自动区块/CSV；改 baseline/HTML/锁/台账；删 covered；扩 CSV directColumns；改测试库门禁文件。

### 结论

全批差集无漏并、无原意改写。001 细化／是／是（席甲 OBS-3 撤回只登记，采本席 GR-01）。002 细化／是／是（席甲漏，采本席 GR-02）。恒 `direct` 满足 §1 L22 与现 170 张 CSV 全 direct，无更小且同样清楚的改法必须改成别的值。不采删 covered。origin/main 口径同批重生成可接受，B4 把那 2 单元格 + 1 行归 001 成立。002 一行中和可选 NOT，baseline 不动。三工具版本标记属同批最小。添加 0。HTML 申请 0。questions 0。写入集合＝3 个工具文件 + 两份生成物的生成器区域。逐 ID：001 PASS、002 PASS、其余观察只登记 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M029 IN_PROGRESS；B3=0。
