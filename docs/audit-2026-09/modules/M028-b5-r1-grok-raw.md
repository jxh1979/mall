CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M028 B5-r1 席乙（Grok）修复后整模块全量审计 raw

- 席位：Grok 席乙（独立，第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B5-r1（B3 累计 1 后的整模块重审，不只看 diff，不复用 B2/B4 票作结论）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：M028 租户范围与数据隔离（技术不变量）／文档与接口定义／第1段
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04；runbook V0.02；criteria-lock V0.11；提示词 `160-audit-prompt.md` V0.17
- 正本：`/home/james/complex-management` `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- origin/main：`b99a9e5deba0bca6fd053a62f18bf1e9bd382d44`
- after tree（B3-r1 冻结）：`c08263bda933e7769db142af135ab204deae164e`（两文件 blob 与当前工作区逐字节一致）
- findings 输入：V2.90，M028-001…010 修复队列「否（已修）」；GR-06 及 B2 只登记项不得无新证据改判
- 独立性：未打开 `modules/M028-b5-r1-self-raw.md` 正文。一次 `rg SEC-P0-TENANT-ISOLATION-001` 的 glob 误命中该文件一行摘要（`in register: 1`）；未继续读，结论不采信该行，同一事实另从 `security-scans/findings-register-v1.md` 独立核到。未改计划 / findings / ledger / 业务代码 / 三 HTML；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席；未对数据库执行写语句。一次性 worktree `/home/james/wt-m028-b5r1-grok-eth` 核完已 `git worktree remove --force`。
- 本席步骤结论：**PASS**（001–010 修复落地、无回归、无新的须入队工程缺陷；只登记项维持；添加 0；无 HTML 修改申请）

---

## 0. 技能自检（本机实核，缺口如实）

本席环境技能根：`/home/james/.claude/skills/`、`/home/james/.claude/plugins/`、`/home/james/.grok/bundled/skills/`、`/home/james/.claude/plugins/cache/`。按提示词 §6 收窄口径；禁止项未调用。用户指令优先于技能。B5 禁止修复，故 TDD / ponytail 修复强度 / receiving-code-review 的 IMPLEMENT 一律不执行。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读全文 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 路由到 audit；**用户禁子代理 / 禁改业务**，不跑 Arc 默认派评审、不写 `docs/arc/audits/` |
| graft | `~/.claude/skills/graft/SKILL.md` | `graft ask --source` 定位 `FinanceService.listArApItems` token 注入；随后对文档声称改用精确读文件 / 命令 |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`、同目录 `grill-me/SKILL.md` | 对抗线自问自答；本体「等用户 / 派子代理」作废 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 本模块两文件是浅模块：读者须记住的事实几乎等于全文 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 反馈环=只读命令（`--check` / `psql` SELECT / `sha256sum` / `m028_check.py`）；不改代码、不加探针 |
| verification-before-completion | `.../superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | PASS 前实跑哈希、生成器、psql、门禁、check.py；不凭印象 |
| ponytail-review | `.../ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删的过期声称；001–010 已删过期三元/行号/一律/逐行对应；剩余简写属 B2 只登记 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 按步骤+完成标准组织 |
| i-have-adhd | marketplace / cache `skills/i-have-adhd/SKILL.md` | 存在；本 raw 编号。CLI 不回带正文（适配器优先） |
| systematic-debugging | 存在 | 只借数据流（规格声称 → 生成器 / 仓储 SQL / 迁移 / 库）；作废 instrumentation |
| receiving-code-review | 存在 | 只借「先核实再接受或反驳」；B5 禁止修复 |
| tdd / test-driven-development | 存在 | 本模块无测试文件；B5 不写测试 |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 可选；本轮无 git 写（worktree add/remove 为 D-028 允许的一次性只读核） |
| audit（Arc） | Arc `skills/audit/SKILL.md` | **缺口/未按本体执行**：本体要派子代理写 `docs/arc/audits/`，与双席+§7 封闭清单冲突 |
| batch-grill-me | 无独立 SKILL | **缺口**；用 grilling 自问自答代替 |
| ubiquitous-language | 本机 skill 目录无 | **缺口** |
| brainstorming / writing-plans | 存在 | 阶段 A 技能，B5 不调用 |
| ponytail / TDD 落测试 | 存在 | 仅 B3；B5 禁止修复 |
| dispatching-parallel-agents / subagent-driven-development / research / using-git-worktrees | 存在 | 提示词划掉。一次性 `git worktree add --detach` 是 D-028 允许的只读核，不是该技能本体；核完已 remove |

graft 本轮 1 次 `ask --source`：tokens saved ≈ 32,949。

---

## 1. 范围、哈希、行数（实算，不抄期待值）

独立命令与输出：

```text
sha256sum docs/audit-2026-09/plan/criteria-lock.md
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

cat docs/audit-2026-09/plan/criteria-lock.sha256
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

grep '^CRITERIA-LOCK-SHA256:' docs/audit-2026-09/ledger.md
CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

head -3 docs/audit-2026-09/plan/criteria-lock.md
# 审计判据冻结
版本：V0.11

git ls-files -z -- docs/data-isolation-and-tenant-scope-security-spec-v0.1.md docs/domain-ownership-scope-audit-v0.1.csv | xargs -0 cat | wc -l
708

wc -l docs/data-isolation-and-tenant-scope-security-spec-v0.1.md docs/domain-ownership-scope-audit-v0.1.csv
   372 docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
   336 docs/domain-ownership-scope-audit-v0.1.csv
   708 total

sha256sum docs/data-isolation-and-tenant-scope-security-spec-v0.1.md docs/domain-ownership-scope-audit-v0.1.csv
e51251665bc8489dc137972245045753225ada6b4d4677c9cfd59b234dc330e9  docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
d935779dd184190cc7e59567cdde61467bdf9513d2c192eb821c305b61f22faa  docs/domain-ownership-scope-audit-v0.1.csv
```

after tree `c08263bda933e7769db142af135ab204deae164e`：

- spec blob `828f5d22cdce128908c8be0f159733681bebf1c7` SHA256 `e51251665bc8489dc137972245045753225ada6b4d4677c9cfd59b234dc330e9` 42137 bytes / 372 行，与工作区逐字节相同
- csv blob `1ab73bdfa6aa90f3d00a47e6425977764a7459fe` SHA256 `d935779dd184190cc7e59567cdde61467bdf9513d2c192eb821c305b61f22faa` 60192 bytes / 336 行，与工作区、origin/main 均逐字节相同

相对 origin/main：spec 手写段已从 V0.02（SHA `c5aa076ed11adc8d351cd16b9354bdc3ed888f4b8fb0f1bdb6fc7268f1e30dad`）变为 V0.03；自动区块 SHA256 `81b83030b0bf22cbbd91d8c2e353e0d47d9f5f834b043792cf343d148265b335` 与 origin/main 相同。CSV 未改。

`plan/modules.md` V0.04 M028 两文件，切分时有效行数 700；现工作区 708（规格 +1 行因 B3 手写段），以实算为准。标记 NORMAL。

M028 映射 33 项（实抽）：`BEQR-074, BEQR-115, BEQR-192, BSM-116, BSM-117, BSM-118, BSM-119, BSM-120, GEN-001…GEN-018, PGM-016, PGM-026, PGM-057, PGM-058, PGM-059, PGM-060, PGM-066`。

---

## 2. 文件覆盖

| 文件 | 行 | SHA256 | 覆盖 |
|---|---|---|---|
| `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` | 1–372 全文 | `e5125166…330e9` | 文首 V0.03、§1–§10、自动区块 §3A（170 表 / V428）、缺口表、6A、附录式 §9/§10 均通读 |
| `docs/domain-ownership-scope-audit-v0.1.csv` | 1–336 全文（头 + 335 数据行） | `d935779d…f22faa` | 逐行机器解析 + 分段通读；`scope_type`：direct 173 / inherited 79 / single_complex_default 76 / global_reference 7；`file_version`/`status` 恒 V0.01/covered |

三 HTML 只读为业务最终解释权（铁律 4，未改）：

- 治理地图 L510：「商户不能查看平台全局和其他商户数据」
- 治理地图 L673–676：「外包人员与审计人员」；L676 原文「审计人员分两类：内部稽核可查看本综合体全部数据，外部审计/监管只能查看划定的范围与时间段；两类都只能查看、不能修改任何业务数据。」
- 治理地图 L868–869 PGM-059：「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。」

环境（D-028，本席复核）：V427 在磁盘且 `git status` 为 `A`（已暂存未发布），`git cat-file -e origin/main:…V427…` 失败。主工作区直接跑生成器 `--check` 会因 V427 变红，**不作本模块缺陷证据**。生成器 / `verify:tenant-isolation` / `verify:full-admin-matrix` 在基于 origin/main 的一次性 worktree 内核，并把当前 V0.03 规格与现行 CSV 覆盖进去。

---

## 3. 第一性原理三问

### 3.1 这段东西为什么必须存在？删掉它，三份 HTML 里哪一条会做不到？

- 规格文档把「商户不能查看平台全局和其他商户数据」（治理地图 L510）提升为可验收的 scope 注入、AC-ISO-1/2/3/4、机审闸口与缺口台账；删掉则隔离模型只散落在迁移注释里（文首自述），HTML 该条无法在文档层被单一权威承接。
- CSV 是闸口 F2 的权威表清单（`tools/verify-tenant-isolation.mjs` L5–16、L84–93）。删掉则「新增带租户数据的表未登记 → 失败」无法执行。
- 答得上 → 有业务上位需求。四分类：能指到治理地图 L510 / L676 / PGM-059，**细化**（010 曾为冲突，现单元格已对齐 L676）。不是添加，不是纯技术性。

### 3.2 它声称的行为，有没有被实际执行验证过？

有。证据类型 4（命令+输出）与类型 1（HTML 原文）：

1. `python3 /home/james/audit-tmp/m028_check.py` → `title: … V0.03`；`auto block sha256: 81b83030…b335`；`problems=0`；exit 0。覆盖 001–010 的手写段回归。
2. 一次性 worktree（cwd=`/home/james/wt-m028-b5r1-grok-eth`，`origin/main`=`b99a9e5d`，无 V427，覆盖当前 spec+CSV）后：
   - `node tools/generate-data-isolation-spec.mjs --check` → `data isolation spec is in sync with Flyway migration scope baseline.` exit 0
   - `node tools/generate-domain-ownership-scope-audit.mjs --check` → `DOMAIN_OWNERSHIP_SCOPE_PASS: 335 tables classified` exit 0
   - `node tools/verify-tenant-isolation.mjs` → `PASS（棘轮）：无新增回归；4 条已知缺口在台账跟踪闭环中` exit 0
   - `node tools/verify-full-admin-page-matrix.mjs` → `完整后台页面矩阵校验通过：92 个页面均有路由、权限、UI 与测试覆盖` exit 0
   - 随后 `git worktree remove --force`，目录已不存在
3. `psql -h 127.0.0.1 -U complex -d complex_test` 只 SELECT（口令从 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD=` 行读取，未 source、未回显）：`cam_trueup_lines`/`cam_trueup_runs`=`NULL`；`supervisor_review_decisions`/`saved_bottles` 存在；`complex_block_mutation` 触发器 41；`chk_saved_bottles_customer_phone_pii`/`chk_saved_bottles_transfer_to_phone_pii` 各 1；`chk_stock_adjustment_type.convalidated=false`；`complex_app` 角色 1。
4. 源码：`FinanceService.java:162-172` 两维下推 `listArApItemsPaged`；`JdbcInventoryRepository.java:1306` `piiCipher.encrypt`；`JdbcInventoryRowMapper.java:285` decrypt；`JdbcSettlementBillRepository.java:624-632` 配置读用列级 `IS NULL OR =`，注释禁止 COALESCE 哨兵。

文档模块不强制类型 2 可执行产物单测；类型 4 已覆盖声称。

### 3.3 它依赖的前提，哪一条是没验证过就当真的？

本席本轮**未**把下列前提当真而不核：

- 「after tree 即当前工作区」→ 对两文件 blob SHA 实比，相等。
- 「生成器 --check 在主工作区应绿」→ **不采信**；D-028：V427 使主工作区必红。改在 origin/main worktree 核。
- 「findings 表体行仍写修复队列是」→ 以 V2.90 **顶部当前段**「001…010 已修转否、队列是=0」为输入状态；表体是 B2 候选历史行，属 `docs/audit-2026-09/` 排除范围，不审台账文件本身。
- 「`verify:tenant-isolation` 已通过」→ 在正确 cwd 的 origin/main worktree 实跑 exit 0，不是读注释。
- 「ISO-GAP-04 路径不存在」→ 按 B2 同目录裸路径约定，`docs/superpowers/plans/2026-06-15-security-audit-fixes.md` 存在（4939 bytes）；**不改判 GR-06**。

仍为开放事实、本模块只陈述不关闭的：ISO-GAP-04（缺 V044–V061 统一 TDD plan，P1）、ISO-GAP-05（dev-design uuid vs VARCHAR，P2）、§5 回填/VALIDATE（`chk_stock_adjustment_type` 库内仍 `convalidated=false`）。这些在缺口表里仍标开放或待执行，与库/文件一致，**不是假完成**。

---

## 4. 顺从式：33 项逐项

判定口径：映射 ID 在本模块两文件上有可核对声称或 HTML 转述 → 适用；本文件不承载该锚点业务规则 → 无本面（仍记录）。不新增判据。

| ID | 适用/无本面 | 证据 |
|---|---|---|
| BEQR-074 隐私桌台权限 | 无本面 | 规格/CSV 不写桌台查看/紧急接管授权。HTML 原文未在本模块被转述或冲突 |
| BEQR-115 经销价格与税务快照 | 无本面 | 无合同价格目录/税率快照声称 |
| BEQR-192 分红税务 | 无本面 | 无代扣税率/股东类型声称 |
| BSM-116 系统交付与上线 | 部分适用 | §9 生产受限角色 `complex_app` + `application-prod.yml` 运行时连接，属上线加固的 DB 层承接；安装包/教程无本面 |
| BSM-117 正式上线安全加固 | 部分适用 | 同上；U 盘密钥/破窗登录无本面（见 PGM-058） |
| BSM-118 安装包+教程 | 无本面 | 无安装包声称 |
| BSM-119 和谁有关 | 部分适用 | §9 留痕表 append-only 连接「上线动作要留痕」的证据层；不写破窗事件 |
| BSM-120 已确认规则 | 无本面 | 购票入口/通票/订台等经营规则不在本两文件 |
| GEN-001 必要性与最小性 | 适用 | 见 §3.1；B3 只改手写段，自动区块/CSV 未膨胀 |
| GEN-002 可执行正确性 | 适用 | 声称的 npm 脚本在 `package.json`：`docs:data-isolation-spec` / `verify:data-isolation-spec` / `docs:domain-ownership-scope` / `verify:domain-ownership-scope` / `verify:tenant-isolation` 均存在；worktree `--check` 与闸口 exit 0 |
| GEN-003 测试有效性 | 无本面 | 本模块无测试文件；ISO-GAP-04 仍开放（缺统一回归 plan），与缺口表一致 |
| GEN-004 信任边界 | 适用 | §2「隔离列绝不取请求参数」；V044 头注同句；FinanceService 从 `AuthorizationService` 取 scope |
| GEN-005 授权与最小权限 | 适用 | AC-ISO-1/2、§10 审计两类只读、外包 `data_scope=assignee`（V178）；010 单元格已对齐 HTML L676 |
| GEN-006 数据完整性 | 适用 | §9 append-only + 现库 41 触发器；V044 `uq_daily_close_scope_date UNIQUE (business_date, scope_type, scope_id)` 在 `finance_daily_closes` |
| GEN-007 并发与幂等 | 无本面 | 本两文件不定义并发窗口 |
| GEN-008 接口与模式一致性 | 适用 | §3 历史表列宽/表数与迁移一致（V046=9、V047 VARCHAR(100)、V049=64、V051=80）；§3／§3A／CSV 分类差属 B2 只登记（M029） |
| GEN-009 错误处理 | 无本面 | 无伪成功/吞错声称需本文件实现 |
| GEN-010 安全与敏感信息 | 适用 | ISO-GAP-06 Closed + PiiCipher 写入/读出 + V396 CHECK 在库；PGM-059 上位 |
| GEN-011 可观测与审计证据 | 适用 | ISO-GAP-01 Closed：`SEC-P0-TENANT-ISOLATION-001` 在 `security-scans/findings-register-v1.md` 与 `formal-security-baseline-v1.md` 均为 resolved；007 已改为仓储方法注释，手写段不再引用不存在的 `SEC-P2-TENANT-ISOLATION-P0D-CONFIG-SCOPE-001` |
| GEN-012 资源与性能 | 无本面 | 无批处理/连接边界声称 |
| GEN-013 构建配置与依赖 | 适用 | 脚本名与 `package.json` 一致；`verify:delivery-gates` / `verify:predeploy` 含 `verify:data-isolation-spec` 与 `verify:tenant-isolation` |
| GEN-014 数据库迁移正确性 | 适用 | §3 各 V044–V061 行与对应迁移文件一致；§3A 最新 V428 / 170 表与 origin/main 迁移闭包一致 |
| GEN-015 界面可用性 | 无本面 | 无 UI 文案实现；`ADM-VND-004` 仅为落地指称 |
| GEN-016 文档可验证性 | 适用｜核心 | 本轮对参考实现、行/节引用、缺口开闭、两类谓词、§8 三者、append-only、受限角色、§10 迁移/角色/端点/页面/PRD 节号、三 HTML 转述均追到现行对象。001–010 旧不可复现句已不在手写段 |
| GEN-017 业务来源分类 | 适用 | 010 冲突已改单元格、未改 HTML；其余细化；无添加；无把业务发明标成技术性 |
| GEN-018 第一性原理证据 | 适用 | 本 raw 结论落到 HTML 原文、命令输出、迁移/源码；不把 B2/B4 票或席位意见当依据 |
| PGM-016 应急与隐私 | 部分适用 | 卡片「个人信息保护」由 ISO-GAP-06/§2 承接；破窗/U 盘无本面 |
| PGM-026 商户主要功能 | 部分适用 | 不写招商档案；隔离规格支撑「商户数据不串」——完整「不能看其他商户」原文在兄弟锚点「业务边界」L510，本模块转述 AC-ISO 与之对齐 |
| PGM-057 应急恢复和客人隐私 | 部分适用 | 标题级；明细见 058/059 |
| PGM-058 紧急恢复（破窗） | 无本面 | 规格不写钥匙 U 盘/破窗通道 |
| PGM-059 客人与员工个人信息保护 | 适用 | ISO-GAP-06 关闭声称与存酒手机号加密落库一致；不把「一律加密」扩成要求本规格列出全部 PII 列 |
| PGM-060 会员身份/营销/生物信息 | 无本面 | 无会员注册/积分预算/人脸留存声称 |
| PGM-066 已拍板规则 | 部分适用 | 「所有对象带综合体项目归属」由 `project_id` scope / CSV 承接；界面全中文等无本面 |

顺从线：**33/33 已记录**。无「适用但声称不可复现」的新项。

---

## 5. 001–010 回归（不把「队列否」当成已全对；逐条现文核对）

`m028_check.py` problems=0 是最小回归环；下列为独立源码/库/HTML 复核。

| ID | 现文位置 | 现句要点 | 现行事实 | 回归？ |
|---|---|---|---|---|
| 001 | spec L33 | `listArApItems()` 取 `currentMerchantScope()` 与 `currentProjectScopeQuietly()`，下推 `listArApItemsPaged`；两者皆空＝全量 | `FinanceService.java:162-172` 同文；graft 命中同一方法。手写段已无 `isPresent() ? listArApItemsByMerchant` | 无 |
| 002 | L238、L342 | 矩阵「5. 边界与异常场景」表「数据范围穿透」行；CSV 的 `supervisor_review_decisions` 行（无行号） | 矩阵 `p0-openapi-operation-acceptance-matrix-v0.1.md` 在 `## 5. 边界与异常场景` 表确有「数据范围穿透」行；CSV L281 为该表，`no direct scope column detected`。手写段无 `` `….md:数字` `` | 无 |
| 003 | L41/L277、L44、L45 | inventory **9** 表；V049 VARCHAR(64)；V051 VARCHAR(80) | V046 九次 `ALTER TABLE inventory_* ADD COLUMN`；V049 仅 64；V051 仅 80；V047 确为 VARCHAR(100) | 无 |
| 004 | L53、L267 | ISO-GAP-06 **已关闭**；PiiCipher + V396 CHECK | 写入 L1306 encrypt、读出 L285 decrypt；库内两 CHECK 存在；V396 头注点名 ISO-GAP-06 | 无 |
| 005 | L304 | 两类谓词；禁 COALESCE 哨兵；删「一律」「语义等价」 | 业务仓储可见 `(:merchant_id IS NULL OR …)`（如 JdbcFinance/Inventory）；配置仓储 `findComprehensiveServiceFee` 用 `(project_id IS NULL OR project_id = :project_id)` 并注释哨兵风险。手写段无「读路径一律写」「对常规数据语义等价」 | 无 |
| 006 | L345–346 | CSV=闭集，§3A=自动基线，§3=背景；新增表跑两个 `npm run docs:*`，不手改 | 现文即此。CSV 335 ≠ §3A 170 ≠ §3 历史 14 行，与改述一致。分类差（见下）只登记 | 无 |
| 007 | L304 末 | 证据见 `JdbcSettlementBillRepository` `findComprehensiveServiceFee` 注释 | 该方法 L624–626 即 COALESCE 哨兵跨租户读教训。手写段无 `SEC-P2-TENANT-ISOLATION-P0D-CONFIG-SCOPE-001` | 无 |
| 008 | L353–355 | V140 **初版** 15+15；cam 随 V274 删除；非现行闭集；以库内触发器为准 | V140 数组含 cam 两表；V274 DROP；库 `to_regclass` 空；触发器 41；V148 将 `supervisor_review_decisions` 纳入 delete-only，库内该表有触发器 | 无 |
| 009 | L371–372 | `platform-governance-prd-v0.1.md` §4 与 §4.1 | 该 PRD L153「## 4. 角色与使用场景」、L174「### 4.1 …（代码待落地）」；product-spec §4 确是「治理总览页」。手写段无裸「治理 PRD §4」 | 无 |
| 010 | L366 单元格 | 内审本综合体全部数据只读；外审划定范围与时间段；两类不能改业务数据；敏感查看属敏感操作 | 与治理地图 L676 逐字对齐。V180 `role_auditor_internal` data_scope=`complex`、`role_auditor_external` `project` + TemporaryGrant 限期。明确不可列含「无审批查看敏感原文」承接 L677 | 无 |

冲突优先：010 现文不再比 HTML 窄。其余按细化保持。无新冲突。

---

## 6. 其余可核实声称（001–010 之外，本轮仍追）

| 声称 | 对象 | 结果 |
|---|---|---|
| §2 token 注入 / `currentActor()` / `currentMerchantScope()` | `AuthorizationService.java:209,425`；FinanceService 调用 | 方法存在。工作区 `currentMerchantScope` 无商户号会抛 `MERCHANT_SCOPE_REQUIRED`（未发布，相对 origin/main；B2 只登记，同 M025）——**不升缺陷** |
| §3 V044 部分索引 + 日结唯一约束 | V044 L6–14 | `idx_ar_ap_items_merchant WHERE merchant_id IS NOT NULL`；`uq_daily_close_scope_date` 在 `finance_daily_closes` |
| §3 V048 `roles.organization_id` VARCHAR(64) | V048 | 命中 |
| §3 V052 / V055 / V057 / V059 / V060 / V061 | 对应迁移 | 表名与列存在；V057 确加 `approval_level` / `required_approver_rank`；V050 CREATE 列与 §7 表一致；`idx_supervisor_review_decisions_pending` 部分索引存在 |
| §3A 最新 **V428**、**170** 张 | 自动区块 L59；生成器 `parseSchema` | worktree `--check` 绿；自动区块与 origin 相同 SHA |
| §4 绑定矩阵「数据范围穿透」 | 矩阵 §5 表 | 原文「每个查询和写入接口必须按 project/merchant/venue/bar/space 等范围过滤」 |
| L6 / L238 `UI-ADM-MER-002` | `full-admin-ui-acceptance-matrix-v0.1.md:55` | 存在 |
| ISO-GAP-01/02 Closed | 基线文件 + `package.json` `verify:delivery-gates` 含 `verify:tenant-isolation`；worktree 闸口 PASS | 成立 |
| ISO-GAP-03 Closed V065 | V065 4 条 CREATE INDEX | 成立 |
| ISO-GAP-04 仍 P1 | 所引 plan 按裸路径在 `docs/superpowers/plans/…` 存在，正文为 V038/V039，不含 V044–V061 统一清单 | 缺口事实仍真；路径约定只登记 |
| ISO-GAP-05 仍 P2 | 本模块只登记「早期 design 写 uuid」；不在本模块改 design 文档 | 与表一致 |
| §6A.1 闸口职责 | 工具扫描 CSV + Jdbc*Repository；F1/F2 | worktree PASS（4 条 baseline，0 新回归） |
| §6A.2 台账 | `security-scans/findings-register-v1.md` / `formal-security-baseline-v1.md`（仓库根，不是 `docs/security-scans/`） | 存在；`SEC-P0-TENANT-ISOLATION-001` resolved |
| §7 OpenAPI `governance.supervisor_review.view/manage/approve` | 验收矩阵 L287–289；admin 路由 ADM-GOV-006 | 存在 |
| §8 两个 npm 脚本 | `package.json` | `docs:domain-ownership-scope`、`docs:data-isolation-spec` |
| §9 `complex_app` + 回退 owner | `application-prod.yml` L10–25：`COMPLEX_DATABASE_APP_USERNAME` 回退 `COMPLEX_DATABASE_USERNAME`；Flyway 用 owner | 与文一致 |
| §10 `outsourced_vendor` + 三角色 + assignee | 账号类型在 access/前端；V178 `OUTSOURCED_FACILITY` / `OUTSOURCED_SECURITY_CLEANING` `data_scope=assignee`；V179 `OUTSOURCED_CONSTRUCTION` | 成立 |
| V178/V179/V180 | 迁移文件存在 | 成立 |
| `PartnerSubordinateCompletionService` + subordinates-completion + ADM-VND-004 | Java 类存在；端点实为 `GET /api/v1/partner/subordinates/completion`（`PartnerController` `@GetMapping("/subordinates/completion")`）；矩阵 CSV `ADM-VND-004` 已实现 | 简写名 B2 只登记，不改判 |
| `platform-governance-prd-v0.1.md` §4.1 标题「代码待落地」 | PRD L174 | 仍过期标题；属该文档模块，只登记 |
| 权限矩阵外包工作台 ADM-VND | `full-admin-page-permission-field-matrix-v0.1.csv` ADM-VND-001…004 | 存在；worktree `verify:full-admin-matrix` 绿 |

CSV 要点（全文 335 数据行）：`supervisor_review_decisions` L281 `single_complex_default` + 「no direct scope column detected」与 spec L342 一致；`space_type_delete_requests` 已在 CSV（origin/main 口径）；`business_entity_projects` **不在** CSV——对应未发布 V427，D-028 只登记。

---

## 7. 对抗式（grilling 自问自答；不等操作员、不派子代理）

❓ **Q1 — 001–010 是否只改了字面、事实仍错？**  
➡️ 否。check.py 专打旧字面；另用迁移/源码/psql/HTML 打现句。10 条现句均可复现。

❓ **Q2 — 「队列全部否」会不会掩盖新的假完成？**  
➡️ 本轮按对象重追。缺口表 04/05 仍开放、§5 VALIDATE 库内仍 false，与文档一致，不是把计划写成当前完成。ISO-GAP-01/02/03/06 Closed 均有生成器/迁移/库/源码。

❓ **Q3 — §8 改完后，§6A.1 仍写「未在 CSV/§3 登记」是否新冲突？**  
➡️ 闸口源码 F2 **只查 CSV**（`verify-tenant-isolation.mjs` L15–16）。§3 含 §3A 自动区块；§8 要求跑生成器更新 CSV 与 §3A、禁止手改历史 §3。第一次正常新增表：按 §8 跑两个 docs 脚本即过 F2。要把「手改历史 §3」理解成门禁要求，须同时忽略 §8 与 §3A「不要手工修改本区块」。按提示词「两三件不巧才成立 → 不修」。**不升 finding**（与 006 改述后的操作路径一致）。

❓ **Q4 — 6A.1「未注入 scope 的读/写查询 → 失败」是否夸大闸口（工具自陈不查写路径）？**  
➡️ 工具 L25–31 写明 B2 写路径 / 请求体覆盖为盲区。ISO-GAP-02 关闭句与 6A.3 验收是「缺 scoped 读通道 / 未登记表」，与 F1/F2 一致。第一次正常跑闸口按 F1/F2 工作。把「AC-ISO-2 已被 CI 兜住」当成事实，须忽略 6A.3 与工具盲区段。属 M029 工具覆盖范围 vs 规格职责句的张力，B1 未入队；**无新证据改判**，不为本模块发明「闸口必须覆盖写路径」的新需求。

❓ **Q5 — §1 三种 scope_type 仍缺 CSV 的 `global_reference` 与 §3A 的 `inherited-or-location-scope`？**  
➡️ 仍缺。B1 OBS-8 / B2 只登记（生成器 M029，与 006 同源）。**无新证据，不改判。**

❓ **Q6 — 三表分类差是否在 B3 后恶化？**  
➡️ `business_process_step_records`：§3 `inherited` / CSV `single_complex_default`；`venue_business_hours`/`venue_sessions`：§3A `inherited-or-location-scope` / CSV `direct,venue_id`。与 B2 登记相同。CSV 未改。不改判。

❓ **Q7 — HTML L676 与单元格、V180 是否再窄一档？**  
➡️ 单元格现含内审全量只读 + 外审范围时间段 + 两类不能改。V180 两角色 data_scope complex/project。不比 HTML 窄。L677 审批看敏感信息落在「明确不可／无审批查看敏感原文」。不升。

❓ **Q8 — 主工作区 `verify:tenant-isolation` 报 `business_entity_projects` 未登记是否本模块缺陷？**  
➡️ 否。该表来自暂存未发布 V427（`git status` `A`，不在 origin/main）。在 origin/main worktree 闸口 PASS、0 新回归。D-028 已登记。

❓ **Q9 — 自动区块 170 vs CSV direct 173 是否文档自相矛盾？**  
➡️ 两生成器口径不同（迁移出现 scope 列 vs 逐表分类）。各自 `--check` 在 origin/main 绿。B2 只登记。不改判。

❓ **Q10 — 还有没有第一次打开文档就会走错的路径/行号/表数？**  
➡️ 手写段已无 `file:行号` 引用。§9 现标明 V140 初版 + cam 已删。§2 参考实现指向现行方法。§10 PRD 带文件名。剩余简写（handovers/steps/exceptions、subordinates-completion）B2 已只登记：按 V140 全文或 OpenAPI 仍能落到实名。

ponytail-review（只找可删）：手写段已去掉过期三元、失效行号、十表、未实现加密、「一律」、假 ID、把已删 cam 表写成现行闭集、过窄审计范围。再删会伤 AC-ISO/缺口/§9 初版清单。`Lean already. Ship.`（本模块文档侧）

---

## 8. 只登记项（B2 双席已定；无新证据，维持）

- GR-06：ISO-GAP-04 路径按同目录裸路径 → `docs/superpowers/plans/2026-06-15-security-audit-fixes.md` 存在；不单给此处加 `docs/` 前缀。
- §3／§3A／CSV 对 `business_process_step_records`、`venue_business_hours`、`venue_sessions` 的分类差与 `inherited-or-location-scope`（M029）。
- §9 `handovers`/`steps`/`exceptions` 简写（实名 `inventory_trace_handover_steps` / `_exceptions`）。
- §10 `subordinates-completion` 实为 `/subordinates/completion`。
- 170 vs CSV direct 173。
- CSV `file_version`/`status` 恒值。
- §5 回填与 V046 VALIDATE 仍待执行（库 `convalidated=false`，与 M027 登记一致）。
- §6 末「截至 2026-06-18」历史快照。
- PRD §4.1 标题「代码待落地」（该文档所在模块）。
- 主工作区 `currentMerchantScope()` 未发布暂存改动（同 M025）。
- CSV 与测试库只差未发布 V427 的 `business_entity_projects`（D-028）。
- §1 未列 `global_reference`（OBS-8，与生成器口径同源）。

---

## 9. 四分类总览 / 添加 / HTML

- 冲突：010 已按 HTML L676 改单元格，现文不冲突。本轮无新冲突。
- 细化：001–009 及隔离规格整体。
- 技术性：无新项需登记。
- 添加：0。不发 2.2。三 HTML 本身无上报问题。

本轮**无新临时 GR 项**。不为理论场景加需求，不启用锁外判据。

---

## 10. 覆盖完成标准与结论

- 模块全部文件有覆盖记录：2/2 全文。
- 映射 33 项均有适用或无本面记录。
- 三线均执行（第一性三问、顺从 33、grilling Q1–Q10）。
- 声称有类型 1 或 4 证据；文档豁免类型 2。
- 无未答 questions。
- 无未定稿新缺陷。
- 本文件第一行独立实算锁哈希，与 sidecar / ledger 同值 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。
- 一次性 worktree 已删除。

**本席 B5-r1 步骤结论：PASS。**

不是模块收口（须双席合取且 B5 后四值全等，由席甲在双方 raw 封存后判定）。本票不是授权改业务/HTML/锁/findings/ledger。
