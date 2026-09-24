CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M028 B1 席乙（Grok）修复前全量审计 raw

- 席位：Grok 席乙（独立，第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B1（修复前全量；本模块首次开工，本版历史未审；PENDING→IN_PROGRESS 由调度已写 ledger；本席不改 ledger / findings / 计划）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：M028 租户范围与数据隔离（技术不变量）／文档与接口定义／第1段
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04；runbook V0.02；criteria-lock V0.11；提示词 `160-audit-prompt.md` V0.17（`.scratch/module-recheck-20260908/160-audit-prompt.md`）
- 正本：`/home/james/complex-management` `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- origin/main：`b99a9e5deba0bca6fd053a62f18bf1e9bd382d44`
- 环境：按 ledger「D-028 环境决定」——两文件工作区副本已用 `git show origin/main:<路径>` 对齐到已发布版本；相对 origin/main 逐字节一致；相对 HEAD 各有 1 行差异。主工作区迁移目录含已暂存未发布 V427（M009）。生成器 `--check` 在基于 origin/main 的一次性 detach worktree 核，核完 `git worktree remove`。
- 独立性：未打开 `modules/M028-b1-self-raw.md`。一次无路径过滤的 `rg` 命中过该文件两行摘要（`SEC-P0-TENANT-ISOLATION-001 in register` / GEN-011 表行）；未继续读该文件，结论不采信那两行，证据全部来自本席对业务文件 / 三 HTML / 锁 / 迁移 / 源码 / `complex_test` 的只读命令。未改计划 / findings / ledger / 业务代码 / 三 HTML；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席。
- 本席步骤结论：**FAIL**（有工程缺陷成立的临时 GR 项；无添加项、无 HTML 修改申请）

---

## 0. 技能自检（本机实核，缺口如实）

本席环境技能根：`/home/james/.claude/skills/`、`/home/james/.claude/plugins/`、`/home/james/.grok/bundled/skills/`、`/home/james/.claude/plugins/cache/`。按提示词 §6 收窄口径；禁止项未调用。用户指令优先于技能。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读全文 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 路由到 audit；**用户禁子代理 / 禁改业务**，不跑 Arc 默认派评审、不写 `docs/arc/audits/` |
| graft | `~/.claude/skills/graft/SKILL.md` | `graft ask --source` 定位租户隔离实现；随后对文档声称改用精确 `rg` / 读文件 / 命令 |
| grilling / grill-me | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`、`.../grill-me/SKILL.md` | 对抗线自问自答；本体「等用户 / 派子代理」作废 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 本模块两文件是浅模块：读者须记住的事实几乎等于全文（隔离语义、谓词、表清单、缺口状态） |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 反馈环=只读命令（`--check` / `psql` SELECT / `sha256sum`）；不改代码、不加探针 |
| verification-before-completion | `.../superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | PASS/FAIL 前实跑哈希、生成器、psql、门禁；不凭印象 |
| ponytail-review | `.../ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删：过期行号、已 DROP 的 cam 表、过期三元参考实现 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 按步骤+完成标准组织，供 B2 归并 |
| i-have-adhd | marketplace `skills/i-have-adhd/SKILL.md` | 存在；本 raw 编号清单。CLI 不回带正文（提示词适配器优先） |
| tdd / test-driven-development | 存在 | 本模块无测试文件；B1 不写测试 |
| systematic-debugging | 存在 | 只借数据流（规格声称 → 生成器 / 仓储 SQL / 迁移 / 库）；作废 instrumentation |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 可选；本轮无 git 写，未装 hook |
| audit（Arc） | Arc `skills/audit/SKILL.md` | **缺口/未按本体执行**：本体要派子代理写 `docs/arc/audits/`，与双席+§7 封闭清单冲突 |
| batch-grill-me | 无独立 SKILL | **缺口**；用 grilling 自问自答代替 |
| ubiquitous-language | 本机 skill 目录无 | **缺口** |
| brainstorming / writing-plans | 存在 | 阶段 A 技能，B1 不调用 |
| ponytail / TDD 落测试 | 存在 | 仅 B3；B1 禁止修复 |
| dispatching-parallel-agents / subagent-driven-development / research / using-git-worktrees | 存在 | 提示词划掉。一次性 `git worktree add --detach` 是用户 D-028 允许的只读核，不是该技能本体；核完已 `git worktree remove` |

graft 本轮 1 次 `ask --source`：tokens saved ≈ 41,469。

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

wc -l docs/data-isolation-and-tenant-scope-security-spec-v0.1.md docs/domain-ownership-scope-audit-v0.1.csv
   371 docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
   336 docs/domain-ownership-scope-audit-v0.1.csv

sha256sum docs/data-isolation-and-tenant-scope-security-spec-v0.1.md docs/domain-ownership-scope-audit-v0.1.csv
c5aa076ed11adc8d351cd16b9354bdc3ed888f4b8fb0f1bdb6fc7268f1e30dad  docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
d935779dd184190cc7e59567cdde61467bdf9513d2c192eb821c305b61f22faa  docs/domain-ownership-scope-audit-v0.1.csv

git show origin/main:docs/data-isolation-and-tenant-scope-security-spec-v0.1.md | sha256sum
c5aa076ed11adc8d351cd16b9354bdc3ed888f4b8fb0f1bdb6fc7268f1e30dad  -

git show origin/main:docs/domain-ownership-scope-audit-v0.1.csv | sha256sum
d935779dd184190cc7e59567cdde61467bdf9513d2c192eb821c305b61f22faa  -

git diff --stat HEAD -- docs/data-isolation-and-tenant-scope-security-spec-v0.1.md docs/domain-ownership-scope-audit-v0.1.csv
 docs/data-isolation-and-tenant-scope-security-spec-v0.1.md | 2 +-
 docs/domain-ownership-scope-audit-v0.1.csv                 | 1 +

git diff --stat origin/main -- <同上两路径>
（空）

sed -n '6407p' docs/audit-2026-09/plan/criteria-lock.md
M028 → [BEQR-074, BEQR-115, BEQR-192, BSM-116, BSM-117, BSM-118, BSM-119, BSM-120, GEN-001 … GEN-018, PGM-016, PGM-026, PGM-057, PGM-058, PGM-059, PGM-060, PGM-066]
```

sidecar / ledger 顶部 / 本席实算三值相同。LOCK-AMEND-COUNT 只读见到 `1`（未改 ledger）。计划写有效内容 700；现行物理行 **371 + 336 = 707**（以实算为准；含 CSV 表头）。全文覆盖：两文件每一行均已读，不抽样。模块内无测试文件。只读追证的代码 / HTML / 迁移 / `package.json` / `complex_test` **不在本模块 FILES 列表**，仅作 GEN-016 复现对象。

| 文件 | 物理行 | SHA256 | 相对 origin/main | 相对 HEAD |
|---|---:|---|---|---|
| `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` | 371 | `c5aa076ed11adc8d351cd16b9354bdc3ed888f4b8fb0f1bdb6fc7268f1e30dad` | 逐字节一致 | 2 行 diff（1 插入 1 删除） |
| `docs/domain-ownership-scope-audit-v0.1.csv` | 336 | `d935779dd184190cc7e59567cdde61467bdf9513d2c192eb821c305b61f22faa` | 逐字节一致 | 1 行插入 |

---

## 2. 全文覆盖表

### 2.1 规格 `data-isolation-and-tenant-scope-security-spec-v0.1.md`（371 行）

| 段 | 行 | 覆盖 |
|---|---|---|
| 标题 / 元数据 / 机器标记 | 1–10 | 已读。状态自称「权威安全规格」；计数漂移改由 §3A 生成器纳管 |
| §1 隔离边界与 scope 语义 | 12–24 | 已读。三种 `scope_type`：direct / inherited / single_complex_default。未列 `global_reference`、`inherited-or-location-scope` |
| §2 token 注入约定 + 参考实现 | 26–34 | 已读。参考实现声称见 GR-04；旧行 nullable 见 GR-03 |
| §3 历史人工清单 | 36–55 | 已读。14 行迁移快照；V046 表数见 GR-08；V049 `VARCHAR(?)` 实为 64（观察 OBS-1） |
| §3A 自动基线 | 57–234 | 已读。声称最新 **V428**、**170** 张表。本席逐行计数 170。origin/main `--check` 绿；工作区因 V427 红（D-028，非本模块缺陷） |
| §4 AC-ISO-1…4 | 236–251 | 已读。行号绑定见 GR-02；AC-ISO-4 与 §6A.1 见 GR-03 |
| §5 旧行回填 | 253–257 | 已读。`chk_stock_adjustment_type` 仍 NOT VALID（库内 `convalidated=f`，与「待 VALIDATE」一致） |
| §6 ISO-GAP-01…06 | 259–289 | 已读。01/02/03 关闭证据成立；04 路径见 GR-06；05/06 仍为开放且可复现 |
| §6A 机审闸口 | 291–319 | 已读。谓词「一律」见 GR-03；P0D 证据 ID 见 GR-07；闸口接线与 origin/main PASS 见 §6 |
| §7 V050 主管复核模型 | 321–342 | 已读。V050 列与 pending 索引属实；CSV 行号见 GR-01；表后来加列（观察 OBS-2） |
| §8 CSV 双向绑定 | 344–346 | 已读。见 GR-05 |
| §9 append-only / `complex_app` | 348–357 | 已读。V140 原文 15+15 属实；现行库 41 张触发器、cam 表已 DROP，见 GR-09 |
| §10 特殊人员 | 359–371 | 已读。V178/V179/V180 / 端点 / ADM-VND-004 存在；表内审计可见范围见 GR-11；「治理 PRD §4」见 GR-10 |

### 2.2 CSV `domain-ownership-scope-audit-v0.1.csv`（336 行）

| 项 | 实算 |
|---|---|
| 表头 | `table_name,scope_type,scope_source,scope_path,multi_complex_note,file_version,status`（L1） |
| 数据行 | 335 |
| `table_name` 唯一 | 335/335，无重复 |
| 排序 | 按 `table_name` 字典序，与生成器 `sort()` 一致 |
| `status` | 全部 `covered` |
| `file_version` | 全部 `V0.01` |
| `scope_type` | direct 173 / inherited 79 / single_complex_default 76 / global_reference 7 |
| 与 §3A | §3A 170 张全部在 CSV；CSV 多 165 张（inherited / global / single_complex + 3 张 direct 列不在生成器 `SCOPE_COLUMNS`：`analytics_business_metric_snapshots`、`merchants`、`staff_policy_settings`） |
| 与 `complex_test` | CSV 335 张全部存在；库 337 = CSV + `flyway_schema_history` + `business_entity_projects`（后者为工作区已应用的未发布 V427，D-028） |
| origin/main `--check` | `DOMAIN_OWNERSHIP_SCOPE_PASS: 335 tables classified` exit 0 |

CSV 335 行数据每行已读。生成器 `findColumn` 按 `directColumns` 顺序取**第一**个命中列，故双键表在 CSV 只登记 `project_id`（或 `venue_id`），而 §3A 列出全部 scope 列——这是两套生成器的设计差，不是 CSV 内部自相矛盾。

---

## 3. 第一性原理三问

### 3.1 这两份东西为什么必须存在？

删掉规格：三份 HTML 仍有「商户不能查看平台全局和其他商户数据」（PGM-026 / `platform-governance-map-for-operations-v0.1.html:510`）和「所有对象带综合体项目归属」（PGM-066）。但规格自己写明隔离模型此前只散落在迁移注释里、无单一权威文档（L3–4）。`verify:tenant-isolation` / `verify:data-isolation-spec` 的人类说明和 AC-ISO 口径也落在这份规格。答得上：有业务上位 + 技术闸口说明。

删掉 CSV：`verify:tenant-isolation.mjs` L5–7 写死「权威表清单」就是这份 CSV；`verify:predeploy` / `verify:delivery-gates` 都跑 `verify:domain-ownership-scope`。删掉则门禁失去表闭集。

分类：整体是 HTML 商户/项目边界的**细化**，加生成器/闸口的**技术性**；不是添加。

### 3.2 声称的行为有没有被实际执行验证？

已用类型 4 命令验证（见 §6）。成立的声称：§3A 与 origin/main 迁移同步；CSV 335 且 `--check` 绿；ISO-GAP-01/02/03 关闭证据；V178/179/180 与 ADM-VND-004 / `PartnerSubordinateCompletionService` / `GET /api/v1/partner/subordinates/completion` 存在；`verify:tenant-isolation` 在 origin/main worktree PASS；`verify:full-admin-matrix` 工作区 PASS；`complex_app` 角色存在；V044/V046/V047/V049/V065/V140 与库内列宽/索引/触发器函数一致的部分。

未成立的声称进入 GR-01…GR-11。

### 3.3 哪个前提没验证就当真？

1. 「§3 表 ↔ CSV 逐行对应」（§8）——从未在现行 170 vs 335 上成立。
2. 「读路径一律 `(<scope_col> IS NULL OR <scope_col> = :<scope_col>)`」（§6A.1）——与同文档 AC-ISO-4 及参考实现 `FinanceService.listArApItems` 的现行 SQL 不是同一谓词。
3. 「§9 两份 15 张表」是现行完整清单——库内 41 张触发器；`cam_trueup_*` 已于 V274 DROP。
4. 「治理 PRD §4」能定位到外包口径——`platform-governance-product-spec-v0.2.md` §4 是治理总览页。

---

## 4. 顺从式：映射 33 项逐项

判据定义已从 `criteria-lock.md` 全文阅读（GEN-001…018 L12–100；BEQR-074 L907–916；BEQR-115 L1358–1367；BEQR-192 L2205–2214；BSM-116…120 L4840–4920 段；PGM-016 L5293–5304；PGM-026 L5481–5501；PGM-057…060 L6156–6226；PGM-066 L6340–6371）。三 HTML 对应锚点已对照原文，不凭印象。

| ID | 适用? | 理由与证据 |
|---|---|---|
| BEQR-074 隐私桌台权限 | 无本面 | 两文件无「查看/操作/经理紧急接管」「无查看权限时桌台不可见」。不发明桌台授权规则 |
| BEQR-115 经销价格与税务快照 | 无本面 | 无通知付款固化合同价目/税率快照。不发明税务规则 |
| BEQR-192 分红税务 | 无本面 | §3A 有 `dividend_*` 隔离列盘点，不陈述「股息红利」代扣或 20% 默认。不发明税务 |
| BSM-116 系统交付与上线 | 部分适用 | §9 生产受限角色/append-only 属上线加固技术承接；不覆盖安装包叙事 |
| BSM-117 正式上线安全加固 | 部分适用 | `application-prod.yml` L10–20 与规格 §9 一致：运行时 `complex_app`、Flyway 仍 owner。不覆盖 U 盘密钥/弱口令替换步骤本身 |
| BSM-118 安装包+教程 | 无本面 | 无安装包、无环境探测、无教程 |
| BSM-119 和谁有关 | 部分适用 | 连接留痕：§6A.2 / §9。不展开入口总钥匙 |
| BSM-120 已确认规则 | 部分适用 | 「对象带综合体项目归属」与 §1/`project_id`、§3A 对齐；其余票务/订台/积分条目无本面 |
| PGM-016 应急与隐私 | 部分适用 | §10 敏感查看须审批+留痕；不覆盖破窗 U 盘 |
| PGM-026 商户主要功能 | **适用** | HTML「商户不能查看平台全局和其他商户数据」（治理地图 L510）是本规格 §2/§4 的上位。规格未缩小该句；AC-ISO-1/2 是其下位验收 |
| PGM-057 应急恢复和客人隐私 | 部分适用 | ISO-GAP-06 与「个人信息看不到明文」相关，规格记未实现，未宣称已加密 |
| PGM-058 紧急恢复（破窗） | 无本面 | 无钥匙 U 盘/破窗通道 |
| PGM-059 客人与员工个人信息保护 | **适用** | HTML「手机号、身份信息一律加密存放」。规格 ISO-GAP-06 记 `saved_bottles.customer_phone` 明文（V061 注释「加密至 rest 为后续项」；Java 无 SavedBottle 加密实现）。这是对缺口的记录，不是把明文说成已加密 |
| PGM-060 会员/营销/生物信息 | 无本面 | 无注册、积分预算、人脸留存规则 |
| PGM-066 已拍板规则 | 部分适用 | 「所有对象带综合体项目归属」适用；界面全中文/下单双时间戳等无本面 |
| GEN-001 必要性与最小性 | 适用 | 见 §3.1。历史 §3 与 §3A 双轨有重复，但 §3 已降为背景；未为假设场景加抽象到要立项删除整文件 |
| GEN-002 可执行正确性 | 适用 | 生成入口 `npm run docs:data-isolation-spec` / `docs:domain-ownership-scope` 及 `--check` 在 origin/main 可执行且绿；工作区红是 V427 环境 |
| GEN-003 测试有效性 | 无本面（模块内无测试文件） | 规格要求 AC-ISO 与闸口，测试文件属其他模块。声称由 GEN-016 核 |
| GEN-004 信任边界校验 | 适用 | §2「隔离列绝不取请求参数」是信任边界。参考实现是否仍如所写见 GR-04 |
| GEN-005 授权与最小权限 | 适用 | AC-ISO + §10 外包 assignee / 审计只读。谓词自相矛盾见 GR-03；审计表缩小 HTML 见 GR-11 |
| GEN-006 数据完整性 | 适用 | §9 触发器 / CASCADE 中止。清单相对现行库不完整见 GR-09 |
| GEN-007 并发与幂等 | 无本面 | 文档不定义并发窗口 |
| GEN-008 接口与模式一致性 | 适用 | 规格内部 AC-ISO-4 vs §6A.1、§3 vs CSV、§3A vs CSV 列集合不一致处见 GR-03/GR-05 |
| GEN-009 错误处理 | 无本面 | 无伪成功路径 |
| GEN-010 安全与敏感信息 | 适用 | ISO-GAP-06 明文电话；append-only；token 注入。P0D 证据 ID 悬空见 GR-07 |
| GEN-011 可观测与审计证据 | 适用 | `SEC-P0-TENANT-ISOLATION-001` 在 `security-scans/findings-register-v1.md:26` 与 `formal-security-baseline-v1.md:146`，状态 resolved |
| GEN-012 资源与性能 | 无本面 | |
| GEN-013 构建配置与依赖 | 适用 | `package.json`：`docs:data-isolation-spec`、`verify:data-isolation-spec`（`--check`）、`docs:domain-ownership-scope`、`verify:domain-ownership-scope`、`verify:tenant-isolation` 均存在；`verify:delivery-gates` 与 `verify:predeploy` 均串入后两者；`.github/workflows/predeploy-gates.yml:27` 与 `ops/ci/predeploy-gates.yml:27` 跑 `npm run verify:predeploy` |
| GEN-014 数据库迁移 | 适用 | 规格引用的 V044–V061、V065、V140、V170、V178–V180、V226、V243 等文件均存在。历史表数/列宽见 GR-08 与 OBS-1 |
| GEN-015 界面可用性 | 无本面 | |
| GEN-016 文档可验证性 | **适用（本模块核心）** | 失败项见 GR-01…GR-11 |
| GEN-017 业务来源分类 | 适用 | 本席按冲突→细化→技术性→添加走完；无添加项 |
| GEN-018 第一性原理证据 | 适用 | 每条 GR 落到 HTML 原文或只读命令输出 |

---

## 5. 对抗式（grilling 自问自答）

❓ **Q1 — NULL 旧行到底对商户可见吗？**
AC-ISO-4（L249）默认不可见；§5（L254）说不回填则商户看不到历史数据；§6A.1（L304）却要写列级 `(<scope_col> IS NULL OR <scope_col> = :<scope_col>)`，这会使 **列值为 NULL 的行对任何带该参数的调用方为真**。
现行仓储并非「一律」：`JdbcFinanceRepository` L168–169 是**参数**空才放行（`(:merchant_id IS NULL OR merchant_id = :merchant_id)`），商户 token 下 `merchant_id = :id`，NULL 列不可见，与 AC-ISO-4 一致；`JdbcSettlementBillRepository` L632–633 才是**列**空（配置默认槽对所有项目可见），注释写明来自 2026-07-28 COALESCE 哨兵教训。规格把配置继承谓词写成所有带 scope 列的读路径铁律，和自己的 AC-ISO-4、和自己点名的 Finance 参考实现都反了。→ GR-03、GR-04。

❓ **Q2 — 第一次按 §8「逐行对应」正常做会怎样？**
新增带租户列的表：生成器改 §3A 和 CSV，不会改历史 §3。按 §8 还要「在本规格 §3 增行」，与 L59「不要手工修改本区块」和 L5「历史人工清单仅作解释背景」打架。CSV 335 行 vs 历史 §3 14 行 vs §3A 170 行，第一次打开三处对表名就会失败。→ GR-05。

❓ **Q3 — 按 §9 的 15+15 当现行完整清单去验收会漏什么？**
`psql` 现行 `complex_block_mutation` 触发器 41 张表。规格仍列已被 V274 DROP 的 `cam_trueup_lines` / `cam_trueup_runs`（`to_regclass` 空）。漏列 `supervisor_review_decisions`、`ar_ap_items`、`finance_daily_closes` 等后续迁移补上的表。第一次按文档点名验收会在不存在的表上找触发器、又漏真正受保护的表。→ GR-09。

❓ **Q4 — 「治理 PRD §4」第一次去打开会落到外包口径吗？**
`docs/platform-governance-product-spec-v0.2.md` §4 标题是「治理总览页」（L105），内容是指标和待办，零外包。外包口径在 `docs/platform-governance-prd-v0.1.md` 约 L174 起，且 §4.1 标题仍写「代码待落地」，与规格 L371「已全部落地」对撞。→ GR-10。

❓ **Q5 — 审计人员可见范围有没有缩小 HTML？**
HTML（治理地图 L676）：「内部稽核可查看本综合体全部数据」。规格 §10 表（L366）：「跨域只读留痕/审计/复核记录」。同节落地段（L370）又写「内审 complex 全量只读」，与 HTML / V180 `complex-scoped read-only` 一致。表单元格缩小了 HTML。→ GR-11。

❓ **Q6 — 行号引用第一次跟着点开会落到所称对象吗？**
`csv:131` 实际是 `facility_inspection_templates`，`supervisor_review_decisions` 在 CSV L281。`p0-openapi-operation-acceptance-matrix-v0.1.md:272-273` 实际是票务退款 API；「数据范围穿透」在 L798。→ GR-01、GR-02。

❓ **Q7 — `SEC-P2-TENANT-ISOLATION-P0D-CONFIG-SCOPE-001` 能在台账找到吗？**
对 `security-scans/` 全文 `rg` 零命中；全仓（排除 `docs/audit-2026-09`）只命中规格 L304 自己。→ GR-07。

反例构造到此为止：上述路径都是打开文档、按所写路径/行号/表清单/谓词照做就会偏离的，不依赖两件不巧同时发生。

---

## 6. GEN-016 声称核验（命令级）

### 6.1 生成器一致性（D-028 指定环境）

工作区（含已暂存 V427，**预期不同步，非本模块缺陷**）：

```text
node tools/generate-data-isolation-spec.mjs --check
# exit 1  data isolation spec is out of sync with Flyway migrations.

node tools/generate-domain-ownership-scope-audit.mjs --check
# exit 1  docs/domain-ownership-scope-audit-v0.1.csv is out of sync with Flyway migrations
```

一次性 worktree（已删除）：

```text
git worktree add --detach /home/james/wt-m028-b1-origin-readonly origin/main
# HEAD b99a9e5deba0bca6fd053a62f18bf1e9bd382d44
node tools/generate-data-isolation-spec.mjs --check
# exit 0  data isolation spec is in sync with Flyway migration scope baseline.
node tools/generate-domain-ownership-scope-audit.mjs --check
# exit 0  DOMAIN_OWNERSHIP_SCOPE_PASS: 335 tables classified
node tools/verify-tenant-isolation.mjs
# exit 0  PASS（棘轮）：无新增回归；4 条已知缺口在 baseline；注意 baseline 中 F1:access_grant_requests 已不再复现
git worktree remove --force /home/james/wt-m028-b1-origin-readonly
# 目录已不存在
```

§3A「最新迁移 **V428**，发现 **170** 张」在 origin/main 上成立。工作区最新文件名含 V427+V428；`complex_test` `flyway_schema_history` 最新成功版本 428，且已应用 427。

### 6.2 参考实现

规格 L33：`FinanceService.listArApItems()`（`currentMerchantScope().isPresent() ? listArApItemsByMerchant : listArApItems`）。

现行 `FinanceService.java` L162–172：取 `currentMerchantScope().orElse(null)` **和** `currentProjectScopeQuietly().orElse(null)`，调用 `listArApItemsPaged(merchantId, projectId, keyword, page, size)`。该三元运算不在 `listArApItems` 主路径。`listArApItemsByMerchant` 仍存在（仓储 L122–126：`WHERE merchant_id = :merchant_id`，NULL 列不可见）。→ GR-04。

### 6.3 历史清单 vs 迁移

| 规格声称 | 实算 |
|---|---|
| V044 `ar_ap_items.merchant_id VARCHAR(64)` + `uq_daily_close_scope_date` | 迁移与库内均成立 |
| V046 inventory **10 表** + `saved_bottles` | V046 对 **9** 张 `inventory_*` + `saved_bottles` 加 `VARCHAR(64)`（再加 NOT VALID 约束）。→ GR-08 |
| V047 6 张 bar 表 `VARCHAR(100)` | 迁移与库 `bar_tables`/`bar_orders` 列宽 100 成立 |
| V049 amusement 4 表 `VARCHAR(?)` | 实为 `VARCHAR(64)`（V049 L4–7；库内 64）。观察 OBS-1，规格用问号未写错宽 |
| V065 四张 amusement 部分索引 | 库内四索引存在；ISO-GAP-03 关闭成立 |
| V050 列 + `idx_supervisor_review_decisions_pending` | V050 原文与库内 pending 索引成立 |

### 6.4 行号引用

| 规格 | 跟着打开实际落到 |
|---|---|
| L239 `p0-openapi-operation-acceptance-matrix-v0.1.md:272-273` | L272–273 是票务退款 API。`数据范围穿透` 在 L798 |
| L342 `domain-ownership-scope-audit-v0.1.csv:131` | L131 `facility_inspection_templates`。`supervisor_review_decisions` 在 L281 |
| L55 / L6 CSV 文件名 | 文件存在 |
| L6 `full-admin-ui-acceptance-matrix` UI-ADM-MER-002 | 该矩阵 L55 存在且内容匹配商户授权工作台 |

### 6.5 门禁脚本

`package.json`：`verify:tenant-isolation`、`verify:data-isolation-spec`、`verify:domain-ownership-scope` 均存在；`verify:delivery-gates` 与 `verify:predeploy` 均包含它们。CI 两份 workflow 跑 `npm run verify:predeploy`。`node tools/verify-full-admin-page-matrix.mjs` 工作区 exit 0（「92 个页面」）。ISO-GAP-01/02「已纳入闸口」成立。origin/main 上 `verify:tenant-isolation` exit 0。

### 6.6 缺口表

| 编号 | 规格状态 | 本席 |
|---|---|---|
| ISO-GAP-01 | Closed | `SEC-P0-TENANT-ISOLATION-001` 在 findings-register 与 formal-security-baseline，resolved |
| ISO-GAP-02 | Closed | 脚本存在且接入 delivery-gates / predeploy；origin/main PASS |
| ISO-GAP-03 | Closed | V065 四索引在库内 |
| ISO-GAP-04 | P1 开放 | 所引路径 `superpowers/plans/2026-06-15-security-audit-fixes.md` **不存在**；实文件在 `docs/superpowers/plans/...`，且该 plan 只覆盖 V038/V039，并不含 V044–V061 TDD 清单。缺口「仍缺统一回归 plan」本身仍真，路径写错 → GR-06 |
| ISO-GAP-05 | P2 开放 | `docs/bar-operations-development-design.md` 仍多处 `merchant_id \| uuid`；`foundation-database-design.md` 已加 F5 注释指向本规格。开放状态仍真 |
| ISO-GAP-06 | P1 开放 | `saved_bottles.customer_phone VARCHAR(64)` 无加密实现；V061 注释写明后续项。开放状态仍真 |

### 6.7 append-only / 受限角色 / 特殊人员

- V140 函数 `complex_block_mutation` 与规格所列 15+15 **原文一致**。
- 现行库触发器 **41** 张；规格仍列已 DROP 的 cam 两表。→ GR-09。
- `pg_roles`：`complex_app` 存在且 `rolcanlogin=t`。`application-prod.yml` L10–20 与规格「未配 app 专用 env 则回退 owner」一致。
- V178 三角色、V179 `OUTSOURCED_CONSTRUCTION`+进场报备、V180 `role_auditor_internal` / `role_auditor_external` 存在。`AccessService` 账号类型含 `outsourced_vendor`。`PartnerController` L75 `@GetMapping("/subordinates/completion")`。矩阵 `ADM-VND-004` 存在。V387 把角色显示名改为中文，规格仍用英文码（观察 OBS-3）。

### 6.8 HTML 转述

PGM-026「商户不能查看平台全局和其他商户数据」未缩小。§10 表对内审可见范围缩小了治理地图 L676「本综合体全部数据」。外包「被指派的工单/区域/任务」收成 `data_scope=assignee` 是下位细化，不是发明。HTML 未写英文角色码、V 号、端点名——那些是实现细化。

---

## 7. 临时 findings（GR）

判定顺序均为：冲突 → 细化 → 技术性 → 添加。依据类型：1=HTML 原文，2=可执行产物，4=可复现命令输出。

### GR-01 — CSV 行号引用已漂移

- **位置**：`docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:342`
- **原文**：`该表自身**无 scope 列**（`domain-ownership-scope-audit-v0.1.csv:131` 标注 "no direct scope column detected"）`
- **上下文**：§7 讨论 `supervisor_review_decisions` 隔离缺口。
- **正常触发**：按所写打开 CSV 第 131 行，落到 `facility_inspection_templates,direct,project_id,...`。目标表在第 281 行，`single_complex_default` / `no direct scope column detected`。
- **影响**：第一次按规格引用核对 CSV 就会指错表。
- **最小拟处置**：把 `:131` 改成现行行号，或改成按 `table_name` 检索、不写死行号。
- **分类**：细化（文档对自己盘点表的定位）。**工程缺陷成立**。
- **冻结 ID**：GEN-016。
- **依据**：类型 4。`sed -n '131p;281p' docs/domain-ownership-scope-audit-v0.1.csv`。

### GR-02 — OpenAPI 矩阵行号引用已漂移

- **位置**：同文件 L239
- **原文**：`与 p0-openapi-operation-acceptance-matrix-v0.1.md:272-273 绑定`
- **上下文**：§4「每个查询和写入 operation 必测两条」。
- **正常触发**：打开矩阵 L272–273 是 `confirmTicketRefundPayout` / `approveTicketRefund`。场景表「数据范围穿透」在 L798。
- **影响**：第一次按行号绑定验收矩阵，会绑到退款接口而非越权场景。
- **最小拟处置**：改为 L798 或按场景名引用。
- **分类**：细化。**工程缺陷成立**。
- **冻结 ID**：GEN-016。
- **依据**：类型 4。`sed -n '272,273p;798p' docs/p0-openapi-operation-acceptance-matrix-v0.1.md`。

### GR-03 — 「NULL 旧行可见性」文档自相矛盾，且 §6A.1 与参考实现谓词不是同一句话

- **位置**：L249、L254、L304；对照 `JdbcFinanceRepository.java:168-169`、`JdbcSettlementBillRepository.java:624-633`
- **原文**：
  - L249：`AC-ISO-4 旧行可见性：明确 merchant_id IS NULL 旧行对商户角色是否可见（默认：不可见，除非显式回填）。`
  - L254：`否则商户角色看不到历史数据。`
  - L304：`读路径一律写 (<scope_col> IS NULL OR <scope_col> = :<scope_col>)`
- **上下文**：L304 声称该列级写法与 COALESCE 哨兵「对常规数据语义等价」，并禁止哨兵因 `__DEFAULT__` 撞名导致「该行对所有调用方可见」。
- **正常触发**：实现者按「一律」把租户行表写成列级 `IS NULL OR`，则 `merchant_id IS NULL` 的旧行对**每一个**商户 token 都可见，直接违反同文档 AC-ISO-4 / §5。Finance 分页实际写的是**参数**级 `(:merchant_id IS NULL OR merchant_id = :merchant_id)`，商户调用下 NULL 列不可见。结算费率配置则故意用列级 IS NULL 表示默认槽（L632–633 注释：2026-07-28 替换 COALESCE）。两种谓词都真实存在，规格把第二种写成全体铁律。
- **影响**：第一次按 §6A.1 改仓储读路径，会把隔离行表改成「NULL 对所有商户可见」——这是可复现的错误实现指引，不是理论绕过。
- **最小拟处置**：拆开两种语义：租户行表用参数空=平台全量 / 列等于 token（NULL 列对商户不可见）；配置默认槽才用列级 IS NULL。删掉「一律」和「与 COALESCE 常规语义等价」（两者对 NULL 列并不等价）。
- **分类**：细化（隔离谓词的下位实现）。**工程缺陷成立**。
- **冻结 ID**：GEN-016、GEN-008、GEN-005。
- **依据**：类型 4（读仓储 SQL + 规格三处原文）。类型 1 上位：治理地图 L510「商户不能查看平台全局和其他商户数据」。

### GR-04 — 点名的参考实现已不是 `listArApItems` 的现行行为

- **位置**：L33
- **原文**：`参考实现：FinanceService.listArApItems()（currentMerchantScope().isPresent() ? listArApItemsByMerchant : listArApItems）。`
- **上下文**：§2 读取路径「平台级（scope 为空）才走全量」。
- **正常触发**：打开 `FinanceService.listArApItems`（L162–172）看不到该三元；看到的是 merchant+project 两维 `listArApItemsPaged`。
- **影响**：第一次按规格去核「参考实现」会对不上号，也无法用它证明 AC-ISO-4。
- **最小拟处置**：改成现行 `listArApItemsPaged(merchantId, projectId, …)` 与参数级谓词；或删掉过期三元、改指仍使用 `listArApItemsByMerchant` 的路径。
- **分类**：细化。**工程缺陷成立**。
- **冻结 ID**：GEN-016。
- **依据**：类型 4。`sed -n '162,174p' backend/src/main/java/com/complex/platform/finance/FinanceService.java`。

### GR-05 — §8「§3 表 ↔ CSV 逐行对应」在现行文件上不成立

- **位置**：L345–346
- **原文**：`本规格 §3 表 ↔ domain-ownership-scope-audit-v0.1.csv 逐行对应。任何新增带租户数据的表，必须：(1) 在 CSV 增行…；(2) 在本规格 §3 增行；(3) 补 AC-ISO-1/2 用例。`
- **上下文**：L5 / L36 已把历史 §3 降为「仅作背景」；L59 禁止手改 §3A。
- **正常触发**：数行：历史 §3 数据行 14；§3A 170；CSV 335。§3A 全部在 CSV；CSV 多 165 张。同名表分类还不一致：
  - `business_process_step_records`：历史 §3 L50 写 `inherited`（经 `process_instance_id`）；CSV L79 写 `single_complex_default`（闸口白名单，不要求 scoped 读）。库内该表确有 `process_instance_id`、无 merchant/project 列。
  - `venue_business_hours` / `venue_sessions`：§3A L225–226 `inherited-or-location-scope`（且 §1 三种类型里没有这个值）；CSV L324–325 `direct,venue_id`。
- **影响**：第一次按 §8 做「新增表三步」会去手改已声明勿改的自动区块/背景清单；第一次用 CSV 当权威清单时，`business_process_step_records` 被当成单综合体默认而不是 inherited。
- **最小拟处置**：§8 改为「CSV 是表闭集权威；§3A 是含 scope 列的迁移基线；历史 §3 不参与对应」。CSV inherited 规则是否补 `business_process_step_records` 属生成器（M029），本模块只改规格表述。
- **分类**：细化。**工程缺陷成立**。
- **冻结 ID**：GEN-016、GEN-008。
- **依据**：类型 4。本席 Python 计数 + `sed -n '79p;324,325p'` CSV + 规格 L50 / L225–226。

### GR-06 — ISO-GAP-04 所引 plan 路径不存在

- **位置**：L266
- **原文**：`对齐 superpowers/plans/2026-06-15-security-audit-fixes.md 的 V038/V039 记录`
- **正常触发**：`ls superpowers/plans/2026-06-15-security-audit-fixes.md` → No such file。实文件：`docs/superpowers/plans/2026-06-15-security-audit-fixes.md`（存在）。该 plan 正文只列 V038/V039，不含 V044–V061 回归清单。
- **影响**：第一次按路径去对齐 TDD plan 打开失败。缺口「仍无 V044–V061 统一回归 plan」本身仍真。
- **最小拟处置**：改路径加 `docs/`；或删掉失效对齐，只保留「待补」事实。
- **分类**：细化。**工程缺陷成立**。
- **冻结 ID**：GEN-016。
- **依据**：类型 4。`ls` 两路径。

### GR-07 — P0-D 证据 ID 在安全台账中不存在

- **位置**：L304
- **原文**：`证据见 SEC-P2-TENANT-ISOLATION-P0D-CONFIG-SCOPE-001`
- **正常触发**：`rg SEC-P2-TENANT-ISOLATION-P0D-CONFIG-SCOPE-001 security-scans/` 零命中。findings-register 有 `SEC-P0-TENANT-ISOLATION-001`、`SEC-P2-TENANT-ISOLATION-PROJECT-READ-001`，无 P0D 这一条。仓储注释把同一教训写在 `JdbcSettlementBillRepository` L624–626，不使用该 ID。
- **影响**：第一次按 ID 去台账闭环，找不到条目。
- **最小拟处置**：删掉该 ID，或改成真实存在的 finding / 代码注释定位。
- **分类**：细化。**工程缺陷成立**。
- **冻结 ID**：GEN-016、GEN-011。
- **依据**：类型 4。`rg` 于 `security-scans/`。

### GR-08 — 历史清单 V046「inventory 10 表 + saved_bottles」比迁移多算

- **位置**：L41；L277 复述
- **原文**：`V046 | inventory 10 表 + saved_bottles | merchant_id | VARCHAR(64)`
- **正常触发**：`rg ALTER TABLE V046__inventory_security_fixes.sql` 得到 9 张 `inventory_*` + `saved_bottles`（另加 NOT VALID 约束）。不是 10+1。
- **影响**：第一次按「10 张库存表」去对迁移，对不齐。历史快照虽标背景，仍是可核声称。
- **最小拟处置**：改为「9 张 inventory_* + saved_bottles」或「共 10 张表（含 saved_bottles）」。
- **分类**：细化。**工程缺陷成立**。
- **冻结 ID**：GEN-016、GEN-014。
- **依据**：类型 4。V046 全文 62 行。

### GR-09 — §9 把 V140 当时的 15+15 写成现行完整清单，且包含已删除表

- **位置**：L352–354
- **原文**：`不可变表（UPDATE+DELETE 双锁，15 张）：… cam_trueup_lines、cam_trueup_runs …`；`永久留痕表（仅 DELETE 锁，15 张）：…`
- **上下文**：L348「由 DB 层硬强制」为现在时。
- **正常触发**：
  - `to_regclass('cam_trueup_lines')` / `cam_trueup_runs` 为空（V274 DROP）。
  - `psql` 现行 `complex_block_mutation` 触发器 **41** 张：UD 18、仅 DELETE 23。规格未列的包括 `supervisor_review_decisions`（V148 注释写明「V140 清单遗漏」）、`ar_ap_items`、`finance_daily_closes`、`merchant_expense_entries`、`dividend_statement_lines`、`voucher_redemption_records`、`pass_gift_cost_variance_ledger`、`settlement_transactions`、`ticket_refunds`、`property_fee_bills` 等。
- **影响**：第一次按 §9 点名验收会找不存在的 cam 表，并漏掉已保护表。
- **最小拟处置**：标明「V140 初版清单，非现行闭集」；删 cam 两表；或改为「以库内 `trg_append_only_*` / 后续迁移为准，不在本规格维护闭集」（闭集维护属迁移模块）。
- **分类**：细化。**工程缺陷成立**。
- **冻结 ID**：GEN-016、GEN-006。
- **依据**：类型 4。`psql` 触发器列表；`V274__drop_abolished_cam_tables.sql` L6–7。

### GR-10 — 「治理 PRD §4」第一次打开落不到外包口径

- **位置**：L370、L371
- **原文**：`详见治理 PRD §4`；`需求口径见治理 PRD §4`
- **正常触发**：打开本仓库现行治理产品规格 `docs/platform-governance-product-spec-v0.2.md` §4（L105）是「治理总览页」，无外包/审计/下属账号。外包口径在 `docs/platform-governance-prd-v0.1.md` L174 附近，且 §4.1 标题仍为「需求口径，代码待落地」，与规格「已全部落地」相反。实现侧 `PartnerSubordinateCompletionService`、ADM-VND-004、completion 端点确实存在（本席已核）。
- **影响**：第一次按「§4」去对需求口径，要么落到总览页，要么落到仍写「待落地」的旧 PRD 小节。
- **最小拟处置**：改成带文件名的定位（例如 `platform-governance-prd-v0.1.md` 外包节），或删掉该交叉引用，只保留已核的矩阵/代码路径。
- **分类**：细化。**工程缺陷成立**。
- **冻结 ID**：GEN-016。
- **依据**：类型 4。`sed -n '105,108p' docs/platform-governance-product-spec-v0.2.md`；`sed -n '174,186p' docs/platform-governance-prd-v0.1.md`。

### GR-11 — §10 表把内审可见范围缩小到「留痕/审计/复核」，与 HTML 明文及同节落地段矛盾

- **位置**：L366 表「审计人员 / 可见/可操作范围」单元格
- **原文**：`跨域**只读**留痕/审计/复核记录（按授权综合体范围）`
- **更长上下文**：同表落地列写 V180；L370「内审 complex 全量只读 / 外审 project 限期只读」。V180 注释：`Internal auditor role with complex-scoped read-only access`。
- **HTML 原文**（`platform-governance-map-for-operations-v0.1.html:676`，判据 PGM-043 定义区有全文；本模块映射未含 PGM-043，冲突分类仍按 2.1 对三 HTML 明文）：`审计人员分两类：内部稽核可查看本综合体全部数据，外部审计/监管只能查看划定的范围与时间段；两类都只能查看、不能修改任何业务数据。`
- **正常触发**：只读该表单元格会得到「审计只能看留痕表」，比 HTML「本综合体全部数据」窄。落地段与 V180 反而对齐 HTML。
- **影响**：第一次按表格做权限验收会把内审做成仅审计表只读，小于已拍板 HTML。
- **最小拟处置**：表单元格改为与 HTML / L370 / V180 相同：「内审=综合体全量只读；外审=项目+限期只读；敏感原文另审批」。
- **分类**：**冲突**（与治理地图明文矛盾；同节落地段已是正确转述，应改表而不是改 HTML）。**工程缺陷成立**。
- **冻结 ID**：GEN-016、GEN-005。上位 HTML 见上（类型 1）。映射未含 PGM-043，不另启 LOCK-AMEND。
- **依据**：类型 1 + 类型 4（V180 L7–8）。

无添加项。无需要铁律 4 暂停的三 HTML 自身问题。

---

## 8. 观察（不立项）

| ID | 内容 | 为何不立项 |
|---|---|---|
| OBS-1 | V049 写 `VARCHAR(?)`，实为 64 | 问号不是错误宽度；第一次不会按问号去建错列 |
| OBS-2 | `supervisor_review_decisions` 在 V050 之后增加了 `initiated_by` / `approval_level` / `due_at` 等列；§7 仍是 V050 快照 | 小节标题限定 V050；§3 V057 已点名审批留痕字段非隔离键 |
| OBS-3 | V387 将外包/审计 `role_name` 改为中文；规格仍用英文码 | id 仍是 `role_outsourced_facility` 等；英文码仍能在 V178/V180 对上 |
| OBS-4 | 工作区两生成器 `--check` 红 | D-028：V427 未发布。origin/main 绿 |
| OBS-5 | `complex_test` 已应用 V427，多表 `business_entity_projects` 不在 origin/main CSV | 环境，不是已发布 CSV 漏行 |
| OBS-6 | `chk_stock_adjustment_type` 仍 `convalidated=f` | 规格 §5 已写待 VALIDATE，与库一致；是否补 VALIDATE 属发布决策（M027 已观察） |
| OBS-7 | origin/main `verify:tenant-isolation` 提示 baseline 中 `F1:access_grant_requests` 已不再复现 | 规格未声称该 F1 仍开放；register L27 已说明服务层隔离。收紧 baseline 属 M029 闸口 |
| OBS-8 | §1 的三种 `scope_type` 未包含 CSV 的 `global_reference` 与 §3A 的 `inherited-or-location-scope` | 与 GR-05 同源；不另开 ID |
| OBS-9 | ISO-GAP-04/05/06 作为开放缺口的**事实**仍真 | 只修 GR-06 的错误路径，不把仍缺的 TDD plan 当成已关闭 |
| OBS-10 | `listArApItemsByMerchant` 方法仍在 | 不能用来证明 L33 三元仍是 `listArApItems` 主路径 |

ponytail-review（只记可删，不修）：`L239` 过期行号；`L342` 过期行号；`L353` `cam_trueup_*`；`L33` 过期三元。`net: 可短于现状`。

---

## 9. 结论

- 两文件全文覆盖（371 + 336）。
- 映射 33 项均已逐项给适用或无本面及理由。
- 三线均已落盘。
- origin/main 上两份生成物与迁移同步；CSV 唯一、有序、335 张均在库中。
- ISO-GAP-01/02/03 关闭声称可复现；特殊人员相关迁移/端点/页面存在。
- **有工程缺陷成立的 findings（GR-01…GR-11），本席 B1 = FAIL。**
- 修复队列候选：上述 11 条（B2 正式 ID）。无添加项、无 questions、无 HTML 申请。
- 本席未修任何业务文件。

## M028 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 **42611** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6/xhigh）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（M028 FILES 2：规格 + CSV）。
- 正本：`/home/james/complex-management` main 主工作区。`HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `b99a9e5deba0bca6fd053a62f18bf1e9bd382d44`（未 fetch）。B3=0。无源修复。NO_MERGE。
- 席甲：Claude Fable 5.1，作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类，**不读**席甲 raw 在封存 25936 字节之后的 B2 结论段。
- 本席 B2-r1：**PASS**（001–010 逐 ID 全 PASS；GR-06 只登记不入队 PASS；添加 0、不发 2.2、无 HTML 申请、无 questions。本席撤回 B1 对 GR-06 的入队主张）
- 正文实算锁（非独占行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，内容同值；ledger 顶行同值，LOCK-AMEND-COUNT 1。本段未新增独占锁哈希字段行。独占锁哈希字段行仍仅文首一处。
- 无 MCP、无子代理、无第三席、不问操作员。未改源码/迁移/HTML/lock/findings/ledger；未 fetch/commit/push/merge/deploy；未写本指定路径以外的任何文件。数据库仅对 `complex_test` 做 SELECT（`-h 127.0.0.1 -U complex`，口令只从 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD` 行读取且不回显，未 source 该文件），无任何写语句。

此票只判断 findings.md V2.88 顶部「当前 M028 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。不加判据。B2 不写任何新的独占 CRITERIA-LOCK-SHA256 行。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M028-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 42611 | `9b5bc5636987e448bfd4ed51e9193815874a6102aa84dcd0324ef9fa98829e6c` |
| `docs/audit-2026-09/modules/M028-b1-self-raw.md` 前缀（席甲 B1 V0.01；本席只读此前缀） | V0.01 | 25936 | `89e247d2b56a3ed62d78c0e740446ba3c17b32a93c1e7df9e2df1c8deb49786b` |
| `docs/audit-2026-09/findings.md`（全文件；只采顶部「当前 M028 B2-r1 整批候选」至该节结束） | V2.88 | 1093505 | `65e6cdf27591a8eb106d06ec808cf3d90fd1eafbfaec2388cf682da498b5362c` |

席甲 raw 全文现大于 25936 bytes；本席用前 25936 字节得与封存值相同的 SHA，其后自 B2 标题起不作为本票依据。封存前缀内 U+FFFD = 0。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

controller / runbook / modules.md 只读核验（本席未改）：

| 文件 | SHA256 |
|---|---|
| `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/audit-2026-09/plan/runbook.md` V0.02 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `docs/audit-2026-09/plan/modules.md` V0.04 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |

三 HTML 只读核验（本席未改；010 用治理地图 L676 原文）：

| 文件 | SHA256 |
|---|---|
| `docs/business-system-map-for-operations-v0.1.html` | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| `docs/business-enum-quick-reference-v0.1.html` | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

对象文件（未改）：`docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` 371 行，SHA `c5aa076ed11adc8d351cd16b9354bdc3ed888f4b8fb0f1bdb6fc7268f1e30dad`（文首 V0.02）；`docs/domain-ownership-scope-audit-v0.1.csv` 336 行，SHA `d935779dd184190cc7e59567cdde61467bdf9513d2c192eb821c305b61f22faa`。`questions/M028*`：不存在。候选段止于「修复队列『否』（0）。」；其下他模块历史段不重审。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核差集/分类/队列/最小边界再接受或反驳；禁止表演性同意；B2 不修复。本批重点：席甲单方 003 列宽与 004 加密、010 冲突原文、GR-06 路径基准、文首升版是否最小 |
| grilling | 只借追问结构自查：漏并/改写、V049 升队、关闭 ISO-GAP-06 是否过称、GR-06 为何只改一处、010 是否发明敏感操作句 |
| verification-before-completion | 声称前先跑只读哈希/前缀字节/迁移原文/`ls` 两路径/HTML L676/仓储加密点/V396 头注/psql SELECT CHECK 与 to_regclass |
| systematic-debugging / diagnosing-bugs | 只借读错→对照两 raw 与候选；不改生产代码、不重审模块结论 |
| codebase-design / ponytail-review | 最小是改手写段过期声称；不重列闭集、不手改自动区块、不改 CSV |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| graft | 本步对象是审计文档差集；加密/列宽/路径按 B1 已给坐标直读，未跑 graft 检索 |
| audit-context-building | **未用**（本体强制派 function-analyzer 子代理） |

### 完整差集（GR-01…11、席甲 CX-M028-001…005、OBS 双方）

漏并（findings）：无。改写原意：无。两席 B1 全部临时缺陷 ID 仍落在 001–010 或只登记段。无单席独有缺陷被丢。无本批新开「否」ID。无添加项。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M028-001 | GR-04 + CX-M028-001 | 细化/是/是（括号内改为现行两维 `listArApItemsPaged`） | **PASS（细化/是/是）** |
| M028-002 | GR-01 + GR-02 + CX-M028-002 | 细化/是/是（去行号，不换新行号） | **PASS（细化/是/是）** |
| M028-003 | GR-08 + CX-M028-003（列宽两处席甲单方） | 细化/是/是（9 表×2、VARCHAR(80)、VARCHAR(64)） | **PASS（细化/是/是）** |
| M028-004 | CX-M028-004（本席 B1 未报） | 细化/是/是（ISO-GAP-06 改为已关闭 + V061 括注同步） | **PASS（细化/是/是）** |
| M028-005 | GR-03 + CX-M028-005 | 细化/是/是（拆两类谓词，删「一律」与「语义等价」） | **PASS（细化/是/是）** |
| M028-006 | GR-05（席甲 B1 漏） | 细化/是/是（改述 §8 三者关系；重生成+补用例；分类差异只登记） | **PASS（细化/是/是）** |
| M028-007 | GR-07（席甲 B1 漏） | 细化/是/是（P0D ID 改仓储方法注释） | **PASS（细化/是/是）** |
| M028-008 | GR-09（席甲 B1 漏） | 细化/是/是（标 V140 初版；注明 cam 已删；不重列闭集） | **PASS（细化/是/是）** |
| M028-009 | GR-10（席甲 B1 漏） | 细化/是/是（两处治理 PRD 引用补文件名） | **PASS（细化/是/是）** |
| M028-010 | GR-11（席甲 B1 漏） | **冲突**/是/是（单元格按治理地图原文改写，不改 HTML） | **PASS（冲突/是/是）** |
| （无新 ID） | GR-06 | 只登记不入队（路径按同目录裸路径约定可解析） | **PASS（只登记；本席撤回 B1 入队）** |

冲突入队 1（010）。细化入队 9（001–009）。技术性入队 0。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 001–010（10）；「否」本批新开 ID = 0。

**整批候选：同意。**

---

### 一、差集核实（逐项独立）

**001 — GR-04 + CX-001。票：同意。**

同根：规格 L33 点名 `FinanceService.listArApItems()` 三元 `isPresent() ? listArApItemsByMerchant : listArApItems`。本席本步实读 `FinanceService.java:162-172`：取 `currentMerchantScope().orElse(null)` 与 `currentProjectScopeQuietly().orElse(null)`，下推 `listArApItemsPaged(merchantId, projectId, keyword, page, size)`；注释写「平台口径两者皆 null=全量」。三元只剩 `FinanceRepository.java:160/183` 默认方法。席甲补默认方法残留坐标，不改写本席「主路径已不是该三元」原意。候选处置采用现行写法，不采本席 B1「或改指仍使用 `listArApItemsByMerchant` 的路径」备选——正确（那会继续用过期主路径当参考实现）。

**002 — GR-01 + GR-02 + CX-002。票：同意。**

同根两处行号漂移。本席本步实读：规格 L238 绑 `p0-openapi-operation-acceptance-matrix-v0.1.md:272-273`，现行 272–273 为 `approveTicketRefund` / `getTicketQrRefreshConfig`（票务行）；「5. 边界与异常场景」在 L791，表「数据范围穿透」在 L798。规格 L342 绑 CSV `:131`，实为 `facility_inspection_templates`；`supervisor_review_decisions` 在 CSV L281，`no direct scope column detected` 在该行。候选去行号、改为章节/表名/表名行，**不换新行号**——与本席 B1 备选之一及席甲 CX-002 一致；换新行号还会再漂。未改写「第一次按行号对不上」原意。本席 B1 写 L239，绑定句在 L238，坐标收窄不是改写。

**003 — GR-08 + CX-003（列宽席甲单方）。票：同意。**

本席 GR-08：L41 与 L277「inventory 10 表 + saved_bottles」。本步实读 `V046__inventory_security_fixes.sql`：9 张 `inventory_*` + `saved_bottles`（另加 NOT VALID 约束）。四处之一、之二成立。

席甲单方两处，按指令核迁移原文：

- V051：规格 L45 写 `VARCHAR(64)`；`V051__compliance_security_fixes.sql:6` 原文 `ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(80)`。假数字，GEN-016 成立。本席 B1 §6.3 未核 V051 列宽，漏报，采席甲。
- V049：规格 L44 写 `VARCHAR(?)`；`V049__amusement_security_fixes.sql:4-7` 四表均为 `VARCHAR(64)`。本席 B1 作 OBS-1（问号不是可执行的错误宽度）。CX-003 把它升进同一张 §3 历史表的事实校正。本步同意并入 003：该格是类型声称，最小改法是写成迁移原值 64，不引入新规则；不是改写 GR-08 的 10→9。四处改成迁移原值，最小。

**004 — CX-004（本席 B1 未报）。票：同意。**

本席 B1 把 ISO-GAP-06 写成仍开放、「Java 无 SavedBottle 加密实现」，事实错误。本步独立核源，不采席甲 B2 段：

- `JdbcInventoryRepository.java:1306` `piiCipher.encrypt(savedBottle.customerPhone())`；同方法 `:1312` 加密 `transfer_to_phone`；INSERT/UPSERT `:697/:706/:726` 使用该参数源，无第二条明文写入通道。
- `JdbcInventoryRowMapper.java:285/291` `piiCipher.decrypt`。
- `PiiCipher.encrypt` 为 AES/GCM，信封前缀 `enc:v1:`，不是空操作。
- `V396__pii_phone_envelope_check_widen.sql` 头注原文：「下列五列自 ISO-GAP-06 起已按 PiiCipher 加密落库」；「存酒三列写入点在仓储层（JdbcInventoryRepository）」。
- `complex_test` 只读：`chk_saved_bottles_customer_phone_pii`、`chk_saved_bottles_transfer_to_phone_pii` 均存在，定义为 `NULL OR '' OR ~~ 'enc:v1:%'`。

规格 L53「加密为后续项」、L268 ISO-GAP-06「未实现（明文存储）」相对现行实现不可复现。处置只改缺口行状态与 V061 括注，命名「存酒手机号列」+ V396 CHECK，不把票务/券转赠两列写成本规格缺口、不发明新加密方案。本席 B1 OBS-9「06 作为开放缺口仍真」在实现侧撤回，文档侧过期声称入队。无漏并。

**005 — GR-03 + CX-005。票：同意。**

同根：L304「一律」列级 `(<scope_col> IS NULL OR …)` 使 NULL 行对所有调用方可见，与 L249 AC-ISO-4 / L254「否则商户看不到历史数据」及 Finance 参数级 `(:p IS NULL OR col = :p)` 相反；配置默认槽才用列级 IS NULL（`JdbcSettlementBillRepository.findComprehensiveServiceFee` `:632-633` 与 `:624-626` 注释）。候选把读谓词拆成业务数据表 / 配置默认槽两类，删「一律」与「对常规数据语义等价，但」，禁止哨兵的 ①② 原样保留。不把事实栏「12 处 / 23 处」写入规格。未改写本席「两种谓词都真实存在、规格把第二种写成全体铁律」原意。

**006 — GR-05。票：同意。**

席甲 B1 漏。规格 L345–346「§3 表 ↔ CSV 逐行对应」+ 新增表三步含「在 §3 增行」，与 L5/L36 背景声明、L59「不要手工修改本区块」打架。候选改述三者关系（CSV 闭集、§3A 自动基线、§3 不参与对应），新增表改为跑已有 `npm run docs:domain-ownership-scope` 与 `docs:data-isolation-spec` 重生成（不手改）并补 AC-ISO-1/2 用例；末句「CSV 不再是 orphan」保留。同名表分类差异（`business_process_step_records`、`venue_business_hours`、`venue_sessions`）与 `inherited-or-location-scope` 归生成器模块 M029，只登记——与本席 B1 最小拟处置一致。不手改自动区块、不改 CSV。

**007 — GR-07。票：同意。**

席甲 B1 漏。`SEC-P2-TENANT-ISOLATION-P0D-CONFIG-SCOPE-001` 在 `security-scans/` 与 docs（除本句）0 命中。现行落点本步实读：`JdbcSettlementBillRepository.java:624-626` 方法 `findComprehensiveServiceFee` 上的项目维改用 `(project_id IS NULL OR …)` 注释。候选改指该方法注释，不发明台账条目。最小。

**008 — GR-09。票：同意。**

席甲 B1 只核 V140 文本有表名、未核现库。本步独立：`V274__drop_abolished_cam_tables.sql:6-7` DROP `cam_trueup_lines` / `cam_trueup_runs`；`to_regclass` 两表皆空；`complex_block_mutation` 触发器 **41**。候选：两小标题加「V140 初版」；清单保留 cam 两名并注明已随 V274 删除；清单后声明非现行闭集、现行以库内触发器为准；**不在本规格重列闭集**。比本席 B1「删 cam 两表」更贴历史快照，且满足「不把 41 写成新闭集」。事实栏 41 不进入规格正文。最小。

**009 — GR-10。票：同意。**

席甲 B1 漏。`platform-governance-product-spec-v0.2.md` §4 标题「治理总览页」（L105），无外包/审计。外包/审计角色在 `platform-governance-prd-v0.1.md` §4「角色与使用场景」（L153 起，L165–172 含审计两级）；自助下属在 §4.1（L174，标题仍「代码待落地」）。规格 L370 讲外包与审计已落地 → 补文件名指向 §4；L371 讲自助下属层 → 指向 §4.1。比本席 B1「例如外包节」更精确，不是改写。PRD §4.1 过期标题属该文档所在模块，只登记，本批不改。

**010 — GR-11。票：同意。**

席甲 B1 漏。治理地图 `platform-governance-map-for-operations-v0.1.html:676` 原文：「审计人员分两类：内部稽核可查看本综合体全部数据，外部审计/监管只能查看划定的范围与时间段；两类都只能查看、不能修改任何业务数据。」规格 L366 单元格「跨域**只读**留痕/审计/复核记录」窄于该句；同节 L370 与 V180 已是正确转述。候选按 HTML 两类改写单元格，不改 HTML。尾句「查看企微原文/视频证据/财务敏感信息属敏感操作」来自**现行单元格已有短语**（相邻「明确不可」列亦有「无审批查看敏感原文」），不是本批新发明；HTML L677 另要求敏感查看须审批。不把 V180「project 限期」写进冲突单元格去替换 HTML「划定的范围与时间段」。映射未含 PGM-043，不另启 LOCK-AMEND。

**GR-06 — 分歧项，拟只登记。票：同意只登记，撤回 B1 入队。见第四节。**

观察覆盖（无应升本批缺陷被压）：席甲 OBS-1…7 分别落在只登记段（简写表名、端点路径、170 vs 173、CSV 恒值、VALIDATE/回填、历史快照日期、`currentMerchantScope` 未发布联动）。本席 OBS-2/3/4/5/7/8/10 不另开 ID：OBS-8 与 006 登记的分类差异同源；OBS-4/5 与 D-028/V427 登记同源；OBS-6 VALIDATE 与 M027 登记一致。本席 B1 OBS-1（V049 `?`）并入 003，已在上条核迁移原文。无漏并。

---

### 二、分类理由（席甲 B1 技术性，本批采细化；010 采冲突）

四分类顺序（GEN-017）：冲突 → 细化 → 技术性 → 添加。

- 001–009：均有治理地图「商户不能查看平台全局和其他商户数据」（`:510`）或同文档隔离/append-only/特殊人员下位声称可指；文档过期、自相矛盾或定位失效使该下位不可复现。细化先于技术性。席甲 B1 五条均判技术性——按顺序不采。本席 B1 已判细化，候选跟随。
- 010：与治理地图 L676 明文不能同时成立（单元格把「本综合体全部数据」收成「留痕/审计/复核记录」）。冲突。应改单元格、不改 HTML。本席 B1 已判冲突，候选跟随。
- 无添加：处置都不发明 HTML 沉默的业务规则（不新造隔离谓词语义、不重列受保护表闭集、不改加密算法、不改 PRD 过期标题）。

工程缺陷：十条均为手写段不可复现声称或 HTML 转述缩小，首次按文档照做会指错对象或写出错误谓词。全部「是」。队列全部「是」（010 冲突入队，其余细化入队）。

---

### 三、最小处置边界（逐条核候选表原文；写入集合）

写入集合限规格文档 **1** 个文件：`docs/data-isolation-and-tenant-scope-security-spec-v0.1.md`。CSV 不改。`<!-- DATA_ISOLATION_SPEC_AUTO_START -->` … `END` 自动区块不得手改。一次 B3 禁止改生成器、迁移、仓储、三 HTML、锁、台账。

| ID | 候选最小处置（表内原文） | 是否最小 | 有无新的不可复现声称 |
|---|---|---|---|
| 001 | 括号内改为现行写法——取两维范围，下推 `listArApItemsPaged(merchantId, projectId, …)`；平台口径两者皆空＝全量 | 是。只改 L33 参考实现括注 | 无。与 `FinanceService.java:162-172` 现行注释同句 |
| 002 | 去行号：L238 改「矩阵「5. 边界与异常场景」表「数据范围穿透」行」；L342 改「CSV 的 `supervisor_review_decisions` 行」。不换新行号 | 是。本席备选「换现行行号」不采 | 无。章节名与表名本步已命中 |
| 003 | 四处改成迁移原值（「inventory 9 表」×2、VARCHAR(80)、VARCHAR(64)） | 是。只改 §3/ISO-GAP-04 历史数字 | 无。V046/V049/V051 原文已核 |
| 004 | ISO-GAP-06 改为「**已关闭**：存酒手机号列已按 PiiCipher 加密落库（写入点 `JdbcInventoryRepository`），V396 补 `enc:v1:` 信封 CHECK」、级别 Closed；V061 括注改为「敏感字段 at-rest 加密已落地，见 §6 ISO-GAP-06」 | 是。只改缺口行与历史括注 | 无。写入点、V396 头注、库内 CHECK 已核。不把票务/券列写进本规格 |
| 005 | 拆两类谓词；删「一律」与「对常规数据语义等价，但」；禁止哨兵两条理由原样保留 | 是。只改 L304 一句 | 无。不把 12/23 处写入规格 |
| 006 | §8 改述三者关系；新增表须重生成 CSV 与 §3A（不手改）并补 AC-ISO-1/2；分类差异只登记 | 是。命令已在文首/L59 存在 | 无。不手改自动区块、不改 CSV |
| 007 | 改为「证据见 `JdbcSettlementBillRepository` 配置读取方法（`findComprehensiveServiceFee`）上的同一教训注释」 | 是。只替换悬空 ID | 无。方法与注释本步已命中 |
| 008 | 小标题加「V140 初版」；注明 cam 两表已随 V274 删除；声明非现行闭集、以库内 `complex_block_mutation()` 触发器为准。不重列闭集 | 是。不把 41 写成新清单 | 无 |
| 009 | L370「详见 `platform-governance-prd-v0.1.md` §4」；L371「需求口径见 `platform-governance-prd-v0.1.md` §4.1」 | 是。只补文件名与节号 | 无。§4 / §4.1 标题本步已核。不改 PRD |
| 010 | 单元格改为「内部稽核：本综合体全部数据只读；外部审计/监管：只读划定的范围与时间段；两类都不能修改任何业务数据；查看企微原文/视频证据/财务敏感信息属敏感操作」 | 是。只改一个单元格，不改 HTML | 无新声称。前三句=L676；尾句=现行单元格已有 |

**文首 V0.02 → V0.03：同意属同批最小处置。** 文首版本号是手写元数据，位于自动区块之外。本批 001–010 全部改手写段；修后若仍标 V0.02，本身即 GEN-016 过期版本声称。升 V0.03 与 M024 同批升文首版本同构。不得手改自动区块内「V428 / 170 张」等生成数字；不得改 CSV `file_version`（生成器写死，本批 CSV 不改）。

禁止：重列 append-only 闭集；手改 §3A；改 CSV；改 `PiiCipher` / 仓储加密实现；改已应用迁移；把 PRD §4.1「代码待落地」在本模块改掉；给 GR-06 单加 `docs/` 前缀；发 2.2；改三 HTML。

---

### 四、GR-06 分歧项——只登记；不入队；不主张只改这一处

本席 B1：`ls superpowers/plans/2026-06-15-security-audit-fixes.md` 无文件；实文件在 `docs/superpowers/plans/…`。拟加 `docs/` 前缀或删失效对齐。

席甲复核（本席按点名独立核，不读其 B2 段）：本规格位于 `docs/` 下，手写段引用同目录文档写相对 `docs/` 的裸路径。本步实读手写段（自动区块 L57–234 除外）：

- 同约定且文件在 `docs/` 下存在：`domain-ownership-scope-audit-v0.1.csv`（L55/L300/L342/L346）、`p0-openapi-operation-acceptance-matrix-v0.1.md`（L238）、`platform-governance-product-spec-v0.2.md`（L341）、`p0-operation-permission-matrix-v0.1.md`（L361）。席甲「6 处」按 csv 四次+矩阵+产品规格计，成立；L361 为同约定第 7 处出现，不改变基准。
- GR-06 路径 `superpowers/plans/2026-06-15-security-audit-fixes.md` 按同一基准解析为 `docs/superpowers/plans/2026-06-15-security-audit-fixes.md`，**文件存在**（本步 `ls`）。plan 正文确有 V038/V039 记录；「仍缺 V044–V061 统一回归 plan」是 ISO-GAP-04 缺口本身，路径不是「不存在」。
- 本席 B1 在仓库根目录 `ls`，用的不是本文手写段的同目录约定。
- 根相对路径另有：L6 三处 `docs/…` 前缀；L309 `security-scans/findings-register-v1.md`（仓库根存在，`docs/security-scans/` **不存在**）；自动区块 L59 `backend/src/main/resources/db/migration`。两种基准并存属观察，不是只修 GR-06 能消除的。

**为何不入队、不主张只改这一处：** 若单给 GR-06 加 `docs/` 前缀，该句改成根相对，与 L238/L341/L55 等裸路径不一致，第一次按「同文同一约定」打开反而要记两个规则。若要统一基准，必须同时改同约定其余出现处（并决定 L309 `security-scans/` 与 L6 `docs/` 前缀怎么收），超出「只改这一处」且超出本批最小。ISO-GAP-04 仍为 P1 开放（缺 V044–V061 回归 plan）——只登记路径约定，不把仍缺的 plan 标成已关闭。

本席撤回 B1 入队主张。票：**PASS（只登记不入队）**。

---

### 五、添加／HTML／questions

本批无添加项、无 HTML 修改申请、无 questions。`docs/audit-2026-09/questions/` 无 `M028*`。不 BLOCKED-ASK。不按铁律 4 申请改三 HTML。010 改规格单元格对齐 HTML，不改 HTML。

只登记不入队（候选其余段，本席同意）：GR-06；§3／§3A／CSV 对三张表的分类差异与 `inherited-or-location-scope`（M029）；§9 handovers/steps/exceptions 简写；§10 `subordinates-completion` 实为 `/subordinates/completion`；自动区块 170 与 CSV direct 173；CSV `file_version`／`status` 恒值；§5 回填与 V046 VALIDATE（与 M027 登记一致）；§6 末「截至 2026-06-18」历史快照；PRD §4.1 标题「代码待落地」（该文档所在模块）；主工作区 `currentMerchantScope()` 未发布改动（同 M025 登记）；CSV 与测试库只差未发布 V427 的 `business_entity_projects`（D-028）。无应升本批缺陷。

### 分类／队列／允许的最小修复边界

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止 B3。一次 B3 只改本模块规格文档 1 个文件；CSV 不改；自动区块不得手改；同批文首 V0.02→V0.03。

禁止：改 CSV / 生成器 / 已应用迁移 / 仓储加密实现 / 三 HTML / 锁 / 台账；重列触发器闭集；手改 §3A；给 GR-06 单加 `docs/`；把仍缺的 TDD plan 标关闭；发 2.2。

### 结论

全批差集无漏并、无原意改写。001–009 细化／是／是（席甲 B1 技术性按四分类顺序不采）。010 冲突／是／是（采本席 GR-11；单元格按治理地图 L676 改写，不改 HTML）。003 列宽两处席甲单方：V051 迁移原文 VARCHAR(80)、V049 原文 VARCHAR(64)，同意并入。004 本席 B1 漏报，独立核写入加密、V396 头注、库内 CHECK 后同意入队。002 只去行号不换新行号。005 拆两类并删「一律」「语义等价」。006 改述三者关系 + 重生成加补用例，分类差异只登记。008 标 V140 初版并注明 cam 已删，不重列闭集。009 补 PRD 文件名。GR-06 只登记：同目录裸路径约定下文件存在，本席 B1 在仓库根 `ls`；不主张只改这一处。文首升 V0.03 属同批最小。添加 0。HTML 申请 0。questions 0。写入集合＝规格 1 文件，CSV 不改。逐 ID：001 PASS、002 PASS、003 PASS、004 PASS、005 PASS、006 PASS、007 PASS、008 PASS、009 PASS、010 PASS、GR-06 只登记 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M028 IN_PROGRESS；B3=0。
