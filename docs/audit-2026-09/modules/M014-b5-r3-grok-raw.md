CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M014 B5-r3 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.01（指定路径原先不存在，本次新建完整报告）
- 席位：固定 Grok 独立只读审计席乙（grok-4.6 / xhigh；不派子代理、不用 MCP、不问操作员、不读本轮 `M014-b5-r3-self-raw.md` 及其票）
- 步骤：B5-r3（B3 第 3 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B2/B4 票、B3 自报或 B1 历史票替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M014 同一 1 文件；`runbook.md` V0.02；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 模块：M014 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第2段（OVERSIZE-SINGLE-FILE）
- 模块循环：M014 / 共 471；B3 累计 3（上限）；生命周期仍 `IN_PROGRESS`（本席不改 ledger）。本轮若 FAIL，合取后应 `BLOCKED-3ROUNDS` 交操作员；本席不擅加第 4 次 B3
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值（889020 字节 / 6850 行）。独占锁行仅本文第一行
- 映射：`M014 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-189, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-060, PGM-065, PGM-066]`，COUNT=84 unique=84 MISSING=[] EXTRA=[]；BEQR 21 / BSM 12 / GEN 18 / PGM 33；与锁映射行 L6393 逐字相同。定义与 HTML 出处均从锁自读，不新造判据、不 LOCK-AMEND
- 正本：`/home/james/complex-management` 分支 `main`；`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`。B3-r3 after tree `79438e8ae8cbdcc8b83b49ca43542cde343fd893`（`git cat-file -t` = tree）。本模块 1 文件工作区 blob `cdb8cfda6ac8feb8b35fa3f8d6d7e8fc094400e3` 与 after `git ls-tree` 全等；工作区 SHA256 `f5557682f6a6872c2464630ce493101e45c3f014b0fc9e2d593ae5b83c1ec92f`；`wc -l` = 3500；`endswith \\n`=True。计划冻结有效内容行 3494 不改。标题 V0.08。工作区 `git status --porcelain` 该路径为 ` M`（未入真实索引，与 B3 临时索引口径一致）。`HEAD^{tree}=ee1833d2672e5372ef7b056db4f92d48e6de875a` ≠ after（修复未提交）
- 本席约束：只读业务代码与计划；除本 raw 外不写文件；不修源/三 HTML/锁/findings/ledger；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；不建脚本/日志/临时产物；不跑 Java/HTTP/数据库
- 未读取：`M014-b5-r3-self-raw.md`（`ls` 见该路径存在；本席未 `read`/`cat`/`grep` 其内容）。读 `ledger.md` 核模块状态时见到席甲已封存摘要一行；本席仍按规格原文与三 HTML 独立取证，临时编号自编，不采用对方编号、不以该摘要替代发现
- 旧 B1/B2/B4/B5 可读作已定稿边界，不作本 B5 通过依据
- 已定稿边界（不代替本轮全量重审）：findings V2.44 — 001–024 当前全部「否」（22 已修 + 019 XM→M261 + 020 登记）。019 归 M261；020 所列实现缺口归各实现模块；021 同根句余下在 M421/M439 只作跨模块观察。全部「否」≠ 功能全实现

本次审计结论：**FAIL**。

工程缺陷成立、建议入 B2 的临时编号：GR-M014-B5r3-01（细化）。添加项 0。HTML 修改申请 0。questions 0。锁外判据 0。001–024 在当前单文件上独立复验：024 拟改已落地、无回归；021/022/023 无回归；019/020 维持跨模块/登记。本票不等于双席 B5 或模块 PASS。席甲自审票不得覆盖本席；本席 FAIL 具否决权——本席行使否决。B3 已 3/3：合取若 FAIL 即 BLOCKED-3ROUNDS 交操作员。

---

## 0. 技能自核（V0.17 §6，按 B5 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体。本步禁止子代理，与技能「先派子代理」冲突处按用户指令收窄 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| graft | `~/.claude/skills/graft/SKILL.md` | 已读 SKILL.md；**未运行** `graft` CLI：工具会刷新 `graft/` 图，超出本席唯一可写路径。入口核验用仓库内 `grep`/`read` |
| grilling / grill-me | mattpocock `skills/productivity/grilling`；`grill-me` 为转发（`Run a /grilling session.`） | 只用追问树自己取证，不等操作员、不派子代理 |
| codebase-design | mattpocock `engineering/codebase-design` | 删除测试：本规格若删掉已废止闭集回灌与过期「当前实现」句，复杂度回到 HTML 四类绑定/十一端/状态成员本身；不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | mattpocock `engineering/diagnosing-bugs` | 先建可红反馈环（sha256 / HTML 行命中 / Controller 映射 / V184 / V394 / V410 / V028）再下结论；文档类不造 harness |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流（规格陈述→HTML 原句→Controller/迁移）；B5 不加 instrumentation、不修 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未运行测试不写通过；Java/HTTP/DB 未实跑 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地；不把 B2 禁止项写成缺陷 |
| writing-for-agents | mattpocock `productivity/writing-for-agents` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code` | 存在；本席未改 git、未直推、未安装 hook（安装会写仓库外配置，B5 禁止） |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒级，未起 monitor |
| i-have-adhd | 会话规则 | 编号、复述「模块 M014 / 共 471，B3 第 3 次」；正文留 raw。CLI 按派发不回正文 |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、requesting-code-review（B4 已过）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）、using-git-worktrees、executing-plans、dispatching-parallel-agents、subagent-driven-development。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能（有 `grilling` / `grill-me`）
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit`：与双席/§7 冲突，**不调用**
- Trail of Bits `audit-context-building`：会派 function-analyzer 子代理，**不调用**

明确不用（controller 划掉）：MCP、第三主体、修源/HTML/锁/台账。

---

## 1. 锁、范围与源指纹

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
cd /home/james/complex-management
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 正文一行同值（对照，非抄写来源）。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 字节 | 889020 |
| 行（`wc -l`） | 6850 |
| sidecar | 全文一行同值 |
| 映射抽出 | 84 条、无重复、MISSING=[] EXTRA=[] |
| 计划 FILES | 1 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 3494（切分时 HEAD）；当前工作区 `\\n` 计数 **3500**（B3-r3 后现态，不改计划凑数） |
| after tree | `79438e8ae8cbdcc8b83b49ca43542cde343fd893`；规格 blob `cdb8cfda6ac8feb8b35fa3f8d6d7e8fc094400e3` = 工作区 `git hash-object` |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`；`HEAD^{tree}` ≠ after（修复未提交） |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| modules.md / runbook.md | SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` / `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| questions/M014* | 无 |
| 当地日 | `date -u` 2026-09-19；变更记录日期 2026-09-19 与 UTC 日一致 |

规格独立哈希：`sha256sum` 与 `python3 hashlib` 同值 `f5557682f6a6872c2464630ce493101e45c3f014b0fc9e2d593ae5b83c1ec92f`（229864 字节）。

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 行 | SHA256 |
|---|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

锁内本模块 66 条 HTML 锚点来源行（非 GEN）对本席读取的现行 HTML 逐条命中（python 抽核 66/66，行号均在文件内且该行含 `td.k`/`h2`/`h3`）。抽核：BEQR-063 `:111`、BEQR-143 `:226`、BEQR-148 `:231`、BEQR-150 `:233`、BEQR-154 `:237`、BEQR-157 `:240`、BEQR-159 `:242`、BSM-004 `:566`、BSM-028 `:698`、BSM-114 `:1827`、PGM-008 `:362`、PGM-018 `:415`、PGM-026 `:492`、PGM-041 `:647`、PGM-042 `:664`。GEN 18 条无 HTML 行。三 HTML 本身未发现需铁律 4 人闸修改的原文错误。HTML 沉默处不升冲突。

锁冻结原文与现行 HTML 已知差（不 LOCK-AMEND、不升本模块缺陷）：BSM-114 锁内 HTML 原文「考勤打卡」，现行 HTML `:1865` 为「查看考勤」。规格现行 L2679 已写「查看考勤」，与现行 HTML 一致。打卡仅工作手机（HTML `:1445`「考勤打卡从领取工作手机开始算」）。此差已由 020 续记，本席核对仍真。

---

## 2. 全文件分段覆盖表（全文已读；行=`\\n` 计数）

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | docs/platform-governance-product-spec-v0.2.md | 3500 | 229864 | f5557682f6a6872c2464630ce493101e45c3f014b0fc9e2d593ae5b83c1ec92f | 已读 L1–L3500（标题 V0.08；OVERSIZE 按结构段分批阅读，raw 列全覆盖；未只读标题/搜索/截断/diff） |

endswith `\\n`=True。与 after tree blob 全等。

### 2.1 结构段已读行区间

| 段 | 行区间 | 内容 | 已读 |
|---|---|---|---|
| 包装头 | L1–L25 | 标题 V0.08、2026-07-04/09-01/09-11/09-19 说明（含 B3-r3/024）、R5 标注、四源归档表 | 是 |
| §0A | L27–L36 | 金额精度/合同本地 AI/当地时间/外部经销商跨篇承接 | 是 |
| 原文件 1 页面清单 | L40–L793 | 定位至变更日志 §20 | 是 |
| 原文件 2 字段字典 | L795–L1336 | 定位至变更日志 §19 | 是 |
| 原文件 3 流程图 | L1338–L2244 | F1–F11、跨依赖、验收、变更日志 §17 | 是 |
| 原文件 4 低保真 | L2246–L2742 | 信息架构至变更日志 §23 | 是 |
| §24 | L2744–L2857 | 有效权限集接口 / 字段投影 / 审批层级映射 | 是 |
| 继续合并补录 | L2860–L3500 | 三源历史摘录载体（无低保真来源段，与 020 观察一致） | 是 |

四源归档路径 `docs/_archive/20260610-retention-audit/merged-layer-spec-sources/` 下 page-list / field-dictionary / flow-diagrams / low-fi 四文件 `ls` 均存在（`_archive` 按 §1.2 排除，不审其内容）。`docs/group-governance-rebuild-prd-v1.0.md`、`docs/platform-governance-prd-v0.1.md`、`docs/merchant-space-contract-model-v0.1.md` 路径存在。

---

## 3. 第一性原理三问（对本模块 1 文件）

1. **为什么必须存在？删掉它，三份 HTML 哪一条做不到？**  
   本文件是平台治理层页面清单 + 字段字典 + 流程图 + 低保真的合并入口，把 PGM-003 模块入口、PGM-017/020 综合体与集团、BEQR-143 四类主体、BEQR-150 十一端、BEQR-154–159 状态闭集、PGM-041/042 权限与职级转成可开发的页面/字段/流程。删掉它，PGM-003「平台治理层包含哪些管理模块」与 BSM-009「基础管理」在产品规格层无单一入口。答得上 → 有业务上位。OVERSIZE 不可再切，标记合法。

2. **声称的行为有没有被实际执行验证过？**  
   文档类：不强制可执行产物。用类型 1（三 HTML 逐字）+ 类型 4（`sha256sum`/`wc`/`git cat-file`/`git hash-object`、Controller `@RequestMapping`+`@GetMapping` 只读对照、迁移文件只读对照）。**未**跑 Maven/Java/HTTP/psql，不冒称实跑。

3. **哪个前提未经验证就当真？**  
   - 「全部否 = 功能全实现」——明确拒绝。§24.2 投影待补齐、§24.3 职级算术、复制模板入口、收费模板字段、经销独立 P0 页仍按 020 归实现模块。  
   - 文中「当前实现 / 现网 / 端点」凡写入口：按对象 grep Controller 映射，不以服务提示语或注释代替。  
   - 补录段是历史摘录载体，不升现行规则。变更日志引述被删词属日志记录。废止框不当新规则。  
   - 「班次状态本规格不定义」不等于现行段已清干净——见 GR-M014-B5r3-01。

---

## 4. 顺从式：84 项逐条适用与证据

适用口径：本模块是治理产品规格文档。HTML 派生判据核「规格是否与锚点原文冲突/遗漏/发明」；GEN 核文档可验证性、一致性、分类与证据。实现缺口已由 020 登记或跨模块观察的，本席核对事实仍在、不升新缺陷。

### 4.1 GEN-001 … GEN-018

| ID | 适用 | 证据（类型） | 结论 |
|---|---|---|---|
| GEN-001 | 是 | 合并四源避免重复入口；已废止闭集用【已废止/不得回灌】收窄。现行仍有班次异常回灌（见 GR-M014-B5r3-01） | 本项支撑该 finding |
| GEN-002 | 是（文档） | 文称不写接口，但 §24 写了当前端点。Controller 映射核验见 §6。文档主路径与错误路径以 HTML 对照，不造执行测试 | 端点声称与映射一致；班次异常声称无 HTML 枚举支撑 |
| GEN-003 | 本文件无测试 | 文档/说明性 md 不强制测试；不把「测试通过」当证据 | 不适用可执行测试；不宣称测试通过 |
| GEN-004 | 弱适用 | 规格要求输入校验（合同日期、金额分）；无本模块代码 | 无本文件实现缺陷 |
| GEN-005 | 是 | 权限点、项目隔离、高危五类、拒绝优先与 PGM-041/BEQR-151 对照 | 无新冲突 |
| GEN-006 | 弱适用 | 四类主体唯一绑定、合同八态成员；不定义迁移边 | 无新冲突 |
| GEN-007 | 弱适用 | 幂等/并发非本文件实现；规格不另立法 | 无新冲突 |
| GEN-008 | 是 | 同文 L1077/L501/L1211/L1265/L1844 声明班次状态不定义，L1078/L2573/L496 仍立法「异常班次」；前后不一致 | 本项支撑该 finding |
| GEN-009 | 弱适用 | 异常分支表存在；文档不执行 | 无新冲突（班次异常属口径回灌，不单独立此项） |
| GEN-010 | 弱适用 | 敏感字段脱敏原则 L840–L846 与 PGM-043 七项封闭清单对照 | 无新冲突 |
| GEN-011 | 弱适用 | 留痕七态、高危须留痕 | 无新冲突 |
| GEN-012 | 弱适用 | 文档无无界循环实现 | 无本文件缺陷 |
| GEN-013 | 弱适用 | 无本文件构建脚本 | 不适用 |
| GEN-014 | 弱适用（引用） | 文称 V028/V184/V394/V410 文件均 `ls` 存在；V410 含 `DROP COLUMN parent_entity_id`；V394 含 `analytics.group.view` `requires_approval=TRUE`。未跑 migrate | 引用路径可核；不冒称迁移已执行 |
| GEN-015 | 弱适用 | 低保真键盘/空状态/引导；无本文件 UI 代码 | 无新冲突 |
| GEN-016 | 是 | 路径/版本/完成声明须可复现。班次异常现行句把已废止闭集写成当前校验，属把旧证据写成当前事实 | 本项支撑该 finding |
| GEN-017 | 是 | 本票按冲突→细化→技术性→添加顺序；能指 BSM-028/PGM-008 上位，不升添加 | 分类见 §8 |
| GEN-018 | 是 | 结论落到 HTML 原文与可复现命令，不以席位意见为据 | 本席遵守 |

### 4.2 BEQR（21）

| ID | 适用 | 证据 | 结论 |
|---|---|---|---|
| BEQR-063 | 弱 | 规格不立法沽清/反结细则；高危五类在 F9 L1982 与 PGM-041 对照 | 无冲突 |
| BEQR-074 | 弱 | 规格不展开隐私桌台；PGM-044 在锁映射内，规格未发明查看/操作/紧急接管三权 | 无冲突 |
| BEQR-143 | 是 | L942–L952 经营对象/运营/合同/结算主体唯一绑定 | 与「四类主体」原文一致 |
| BEQR-144 | 是 | L55 脚注、§0A、F3 不写固定隶属链 | 与「多对多」原文一致 |
| BEQR-145 | 是 | 项目责任主体/资产职责不越权管酒吧游乐 | 无冲突 |
| BEQR-146 | 是 | 域主管公司职责未在本文件改写成城市项目公司管酒吧 | 无冲突 |
| BEQR-147 | 弱 | 收入归属不在本文件展开计算 | 无发明 |
| BEQR-148 | 是 | L953/L986 合作模式六值；「其他」须指定收费模板在 HTML，规格未删 | 无冲突；收费模板字段实现缺口归 020 |
| BEQR-149 | 是 | L1043/L1819 M1–M10 上中下；职级绑薪资大范围 | 与原文一致 |
| BEQR-150 | 是 | L126/L652/L874 十一端中文名；`dealer_portal` 标明未实现 | 与原文一致；未实现码归 020 |
| BEQR-151 | 是 | L751/L2766 五源 − 显式拒绝 | 与原文一致 |
| BEQR-152 | 是 | L601/L1218 项目与商户隔离不提供关闭开关 | 与原文一致 |
| BEQR-153 | 是 | L650/L2137 七档优先级；拉取失败兜底关闭、留痕例外 | 与原文一致 |
| BEQR-154 | 是 | L173 项目六态；不定义迁移边 | 与原文一致 |
| BEQR-155 | 是 | L291 商户六态 + 缓冲期；试运营系统不管 | 与原文一致 |
| BEQR-156 | 是 | L336 合同八态；当地时间 L32 | 与原文一致 |
| BEQR-157 | 是 | L419 员工五态。班次「异常」不是员工状态成员 | 支撑 GR-M014-B5r3-01（班次异常无此闭集） |
| BEQR-158 | 是 | L532 设备七态 | 与原文一致 |
| BEQR-159 | 是 | L609 权限五态含「已启用」。L1062「应选择启用角色」有此上位，不属 024/本票 | 与原文一致 |
| BEQR-166 | 是 | L323–L325 / F3 主线默认总经理 + 三平行线 | 与原文一致 |
| BEQR-189 | 弱 | 管理费方式非本文件主路径 | 无冲突 |

### 4.3 BSM（12）

| ID | 适用 | 证据 | 结论 |
|---|---|---|---|
| BSM-004 | 弱 | 规格有归档/留痕；删除审批细节不在本文件立法新闭集 | 无冲突 |
| BSM-005 | 弱 | 低保真表单分组，未发明「一次填完」为唯一路径 | 无冲突 |
| BSM-006 | 弱 | 动态码非本文件主路径 | 无冲突 |
| BSM-007 | 是 | L2766 平台管理员日常功能 vs 高危逐项授予 | 与原文一致 |
| BSM-009 | 是 | 第 2 节 P0 页覆盖综合体、空间、商户、合同、员工、设备、权限 | 有业务上位 |
| BSM-028 | 是 | 「管理员工、岗位、班次」是班次页上位；不授权班次五态。规格现行把「异常」写成班次状态 | 支撑 GR-M014-B5r3-01 |
| BSM-029 | 是 | 票务/酒吧/游乐/财务/权限与基础对象的连接在跨流程依赖 L2164 | 无冲突 |
| BSM-072 | 是 | 业态六值 L247；商户侧边界在低保真 | 无冲突 |
| BSM-074 | 弱 | 连接基础管理/权限 | 无冲突 |
| BSM-114 | 是 | L2679 私人手机低风险闭集写「查看考勤」，与现行 HTML `:1865` 一致；打卡仅工作手机 | 无冲突；锁冻结「考勤打卡」不 LOCK-AMEND |
| BSM-115 | 是 | 基础管理连所有业务 | 无冲突 |
| BSM-120 | 是 | 已确认规则与 §18/§17/§21 已拍板条对照（设备凭证、职级、权限颗粒度、集团视图） | 无新冲突 |

### 4.4 PGM（33）

| ID | 适用 | 证据 | 结论 |
|---|---|---|---|
| PGM-002 | 是 | 治理顺序对象→合同→人员→设备→留痕体现在 F5/F6/F8/F10 | 无冲突 |
| PGM-003 | 是 | 第 2 节模块入口覆盖综合体/空间/商户/合同/员工/权限/终端/设备/留痕 | 无冲突 |
| PGM-004 | 是 | 综合体项目页 P0 | 无冲突 |
| PGM-005 | 是 | 空间九类 + 项目为归属根 L194/L911 | 无冲突 |
| PGM-006 | 是 | 商户/四类主体字段 | 无冲突 |
| PGM-007 | 是 | 合同准入先阻断再升级 | 无冲突 |
| PGM-008 | 是 | 员工注册两条路径、设备凭证、工作手机。卡片「岗位状态」无取值闭集；规格已废止岗位三态（L1063/L3375）。班次异常无此卡片授权 | 支撑 GR-M014-B5r3-01 |
| PGM-009 | 是 | 角色权限页 + 职级 | 无冲突 |
| PGM-010 | 是 | §14A / F11 十一端七档 | 无冲突 |
| PGM-011 | 是 | 设备台账/领还 | 无冲突 |
| PGM-012 | 是 | 商户侧边界，不看平台全局 | 无冲突 |
| PGM-013 | 弱 | 开放接口非 P0 页主路径 | 无发明 |
| PGM-014 | 是 | 操作留痕页 | 无冲突 |
| PGM-015 | 弱 | 楼宇运营非本文件 P0 页 | 无发明 |
| PGM-016 | 弱 | 应急与隐私原则在脱敏/U 盘清理句 | 无冲突 |
| PGM-017 | 是 | 综合体归属 + 集团视图已承接（L752/L768） | 无冲突 |
| PGM-018 | 是 | 多对多，非固定隶属 | 无冲突 |
| PGM-019 | 弱 | 各主体职责未改写成冲突句 | 无冲突 |
| PGM-020 | 是 | 项目档案/状态/负责人 | 无冲突 |
| PGM-021 | 是 | 避免多综合体数据混 | 无冲突 |
| PGM-026 | 是 | 商户档案、联系人、合作模式、四类绑定 | 无冲突；收费模板实现归 020 |
| PGM-027 | 是 | 商户侧全开放边界 | 无冲突 |
| PGM-031 | 是 | §0A 外部经销商行；独立门户 P0 页未单列——020 观察仍真 | 规格承接 HTML；实现缺口不升新缺陷 |
| PGM-035 | 弱 | 提成/考勤不混营销打卡；规格未立法现金现抽 | 无冲突 |
| PGM-040 | 是 | 单一功能权限点 | 无冲突 |
| PGM-041 | 是 | 五源、隔离、高危五类、HR 定层级自动适配（021 已删相反排除） | 无新冲突 |
| PGM-042 | 是 | 职级 30 档只绑薪资；影响默认模板但不能替代功能权限点（L1996） | 与原文一致 |
| PGM-043 | 弱 | 外包/审计/导出脱敏七项在字段字典 §2.3 | 无冲突 |
| PGM-044 | 弱 | 隐私桌台/挂单/经销权限组未在本文件发明新权 | 无冲突 |
| PGM-048 | 是 | 商户类型开放范围表未改写成可看全局 | 无冲突 |
| PGM-060 | 弱 | 会员治理非 P0 主路径 | 无发明 |
| PGM-065 | 是 | 治理连接各业务在跨流程依赖 | 无冲突 |
| PGM-066 | 是 | 已拍板集团视图/权限颗粒度/留痕 | 无冲突 |

84/84 均有适用记录。无 finding 的项不等于未审。

---

## 5. 对抗式（grilling 追问树，事实自读）

Frontier 已结算、不待操作员：

1. **班次有没有 HTML 状态闭集？** 无。BEQR 状态闭集止于项目/商户/合同/员工/设备/权限。三 HTML `rg`「异常班次|班次状态|班次为异常」零命中（PGM-008 仅卡片「岗位状态」无取值）。  
2. **同文有没有已经废止班次五态？** 有。L1077/L501/L1211/L1265/L1844 现行声明不定义；补录 L2955/L2959/L3135/L3443–L3451 标【已废止/不得回灌】未排班/已排班/进行中/已结束/异常及「异常班次原因规则」。  
3. **现行段有没有把「异常」再写成班次状态门控？** 有。L1078「班次为异常时必填」、L2573「异常班次必须填写异常说明」、L496「记录异常班次」；L484/L2565 班次列表仍列「状态」，与 L1265「【已废止/不得回灌】班次状态作为页面核心字段」直接并立。第一次正常排班把班次标异常就会撞上必填。  
4. **是否 024 回归？** 否。`rg "启用岗位|启用职级|不可分配员工"` 现行活句均在废止框或 024 说明/changelog 内；L1060/L1074/L1854 已按 024 改写。L1062「应选择启用角色」因 BEQR-159 权限状态含「已启用」不属 024、本票也不扩。  
5. **是否设备「异常说明」同病？** 否。设备七态已由 BEQR-158 批出，L1099「异常状态时必填」、L2606「异常归还必须填写异常说明」指向维修中/挂失等已有成员。  
6. **补录 L3133「岗位停用 \| 不允许继续分配」？** 在补录历史摘录载体内、非现行段；按已批口径不升现行规则。  
7. **L1856/L1857 重复「班次缺责任区域」？** shrink 观察，非业务发明，020 已续记，不新开票。  
8. **§24 端点是否真实存在？** 见 §6。存在 ≠ 功能全实现。  
9. **理论极端路径要不要加需求？** 不要。本票只收第一次正常跑就会撞上的回灌。  
10. **BSM-114 锁文「考勤打卡」vs 现行 HTML「查看考勤」？** 锁只读；规格现行已跟现行 HTML。不 LOCK-AMEND。

`ponytail-review`：`L1078-L1078: delete: 班次异常必填。Nothing replaces it（字段可改为选填事故说明或整行随班次状态废止）。` `L2573: delete: 异常班次必须填写。Nothing replaces it。` `L496: delete: 记录异常班次关键操作。Nothing replaces it。` `net: 可删现行回灌句，不新写状态机。`

---

## 6. 文中「端点/入口/接口」只读核验（类型 4；未跑 Java/HTTP/DB）

规格 L50 声明「不写接口」，§24 仍写当前实现端点，故按对象 grep Controller。

| 文中声称 | 核验 |
|---|---|
| `GET /api/v1/access/effective-permissions`（L2754/L2764） | `AccessController.java` L32 `@RequestMapping("/api/v1/access")` + L244 `@GetMapping("/effective-permissions")` → `authorizationService.effectivePermissions()`。入口对该对象存在 |
| `GET /api/v1/access/users/{id}/effective-permissions`（L2765） | 同文件 L250 `@GetMapping("/users/{id}/effective-permissions")`，L252 `requirePermission("access.user.view")`。入口存在 |
| 前端 `AdminApp.tsx` 已改用该接口（L2787） | 本模块 FILES 不含该 tsx；不把服务注释当入口证据。实现缺口不在本文件修 |
| §24.2 字段投影待补齐（L2814/L2816） | 规格自己写明未成体系；020 登记，不升新缺陷 |
| §24.3 现网仍按职级算术与 `specific_role`（L2857） | 规格禁止把算术写入产品口径；实现缺口归 020/实现模块 |
| V394 `analytics.group.view` `requires_approval=true`（L768） | `V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql` 含对该码的 `UPDATE ... requires_approval = TRUE`。文件存在，未跑 migrate |
| V410 删除 `parent_entity_id`（L55，019） | `V410__m010_business_entity_topology_and_role_isolation.sql` 含 `DROP COLUMN IF EXISTS parent_entity_id`。019 归 M261 |
| V028 合规到期台账（L964） | `V028__p1_contract_compliance_expiry.sql` 存在 |
| V184 空间类型字典（L1300） | `V184__space_type_dictionary.sql` 存在 |
| `dealer_portal` 尚无该码（L874） | 规格已标未实现；V111 存在。020 |

`AccessController` 另有 `POST /permission-templates/{id}/copy`（L76）。规格现行已废止复制模板种类（L598）。入口存在属实现侧，020 已记「复制模板入口」，本席不新开文档缺陷。

---

## 7. 001–024 独立复验（当前全部「否」≠ 全实现）

| ID | 复验 | 结论 |
|---|---|---|
| 001–018 | B3-r1 拟改仍在现行段（空间归属根、设备凭证、私人手机闭集、V394、合同八态成员、dealer_portal 未实现标注、F11、废止业态编号等） | 无回归 |
| 019 | L55 指向 V410 删除 `parent_entity_id`；迁移文件存在。跨模块 XM→M261 | 维持否 |
| 020 | §24.2 待补齐、§24.3 职级算术、复制模板入口在代码、收费模板字段、经销无独立 P0 页、§12A.6 自指、计划 3494 vs 现行 3500、补录无低保真来源段、P0 页名与 adminRoutes 不在本 FILES | 维持登记否 |
| 021 | F9 L1980–L1985 无「仅职级变化/责任范围变化排除」活规则；L1996「职级不替代功能权限点」。仅 changelog/文首引述被删词 | 无回归 |
| 022 | 现行页面清单/低保真无「所有对象继续携带 project_id 并按项目归属聚合」。补录 L3001 为历史摘录载体 | 无回归 |
| 023 | L236 业态列表仅「业态名称、说明」 | 无回归 |
| 024 | L10/L1060/L1074/L1321/L1854/L2237 已按拟改落地。`rg` 启用岗位/启用职级/不可分配员工 无未废止现行校验句。L1062 启用角色保留 | 无回归 |

---

## 8. 新问题（审完全文后统一给出）

### GR-M014-B5r3-01 — 现行段回灌已废止的班次「异常」状态门控

- **文件:行号**：`docs/platform-governance-product-spec-v0.2.md:496`、`:1078`、`:2573`；同根列表列 `:484`、`:2565`
- **原句（现行，非废止框、非补录、非 changelog）**：
  - L496：`记录异常班次`
  - L1078：`| 异常说明 | 记录异常班次原因 | 条件必填 | 多行文本 | 班次为异常时必填 | 变更需留痕 |`
  - L2573：`- 异常班次必须填写异常说明。`
  - L484：`| 班次列表 | 班次时间、员工、岗位、区域、状态 |`
  - L2565：`| 班次列表 | 日期、开始时间、结束时间、员工、岗位、区域、状态 |`
- **同文已废止/不定义对照（不当作新规则，只证明回灌）**：L501/L1077/L1211/L1265/L1844 声明班次状态本规格不定义；L1265 废止「班次状态作为页面核心字段」；补录 L2959「异常班次原因规则」、L3135「班次异常必须填写说明」、L3451「异常（班次五态）」均【已废止/不得回灌】
- **正常触发**：现场主管打开班次排班页，把某班次标为「异常」并保存——字段字典要求异常说明必填，低保真要求必须填写。第一次正常跑就会撞上，不依赖并发/极端组合
- **四分类顺序**：冲突？三 HTML 无班次五态明文，沉默 ≠ 冲突。→ **细化**（上位 BSM-028「管理员工、岗位、班次」、PGM-008 员工和手机、BEQR-157 员工状态五值不含班次异常）。工程缺陷成立。不是技术性（有业务状态发明）。不是添加（指得出 HTML 上位）
- **已有判据**：GEN-001、GEN-008、GEN-016；BSM-028；BEQR-157；PGM-008
- **证据类型**：1（三 HTML 对「异常班次/班次状态闭集」零命中；BEQR-157 原文「待入职、在职、试岗、停岗、离职」）+ 4（`rg`/`read` 上列行）
- **工程缺陷是否成立**：是
- **无业务发明？** 否——本票是细化下的缺陷，不是技术性豁免
- **建议最小修复边界（本席不修）**：按 024 同根写法，L1078 去掉「班次为异常时必填」或整行随班次状态废止；L2573/L496 标【已废止/不得回灌】或删除；L484/L2565 列表「状态」列按 L1265 去掉。不改 HTML、不新写班次状态机、不改 StaffController、不重开 024
- **明确排除**：L1062 启用角色（BEQR-159）；设备异常说明（BEQR-158）；补录已废止句；changelog 引述；L1836 mermaid「班次结束或异常处理」可读作流程异常分支，不单独立项；L1856/L1857 重复句归 020 shrink

添加项 0。HTML 修改申请 0。questions 0。

同根扫描：`【已废止/不得回灌】` 全文 284 处。抽核启用岗位/职级、复制模板、定时启停、是否可经营、业态编号、合同即将到期作状态——现行活句均已在废止框或属到期提醒（L271/L701「即将到期合同」是提醒视图，不是合同第九态）。其余不成立只登记。

---

## 9. 观察（不进修复队列）

- 020 续记仍真（§24.3 职级算术、复制模板代码入口、收费模板字段、经销无独立 P0 页、§24.2 投影待补齐、§12A.6 自指、计划 3494 vs 现行 3500、补录无低保真来源段）。
- 锁 BSM-114 冻结「考勤打卡」vs 现行 HTML「查看考勤」：不 LOCK-AMEND。
- L1856/L1857 重复「班次缺责任区域」：shrink，非业务发明。

---

## 10. 覆盖与 PASS 门

| 门 | 结果 |
|---|---|
| 本模块所有文件有覆盖记录 | 是（1/1，L1–L3500） |
| 映射全部 84 项已核对 | 是 |
| 三线均执行 | 是 |
| 所有声称有四类证据之一 | 是（文档类型 1+4；未冒称类型 2 实跑） |
| 无未答 questions | 是 |
| 无未定稿缺陷 | **否**（GR-M014-B5r3-01） |
| Grok 哈希行有效 | 是（本文第一行） |

单席 PASS 须同时满足上列全部。缺「无未定稿缺陷」→ 本席 **FAIL**。

---

## 11. 最终结论

**FAIL**

临时 finding：GR-M014-B5r3-01（细化，工程缺陷成立）。B3 累计已 3。本席不修、不加第 4 次。
