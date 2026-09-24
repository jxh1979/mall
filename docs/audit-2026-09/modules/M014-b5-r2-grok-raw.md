CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M014 B5-r2 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.01（指定路径原先不存在，本次新建完整报告）
- 席位：固定 Grok 独立只读审计席乙（grok-4.6 / xhigh；不派子代理、不用 MCP、不问操作员、不读本轮 `M014-b5-r2-self-raw.md` 及其票）
- 步骤：B5-r2（B3 第 2 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B2/B4 票、B3 自报或 B1 历史票替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M014 同一 1 文件；`runbook.md` V0.02；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 模块：M014 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第2段（OVERSIZE-SINGLE-FILE）
- 模块循环：M014 / 共 471；B3 累计 2；生命周期仍 `IN_PROGRESS`（本席不改 ledger）
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值（889020 字节）。独占锁行仅本文第一行。
- 映射：`M014 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-189, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-060, PGM-065, PGM-066]`，COUNT=84 unique=84 MISSING=[] EXTRA=[]；BEQR 21 / BSM 12 / GEN 18 / PGM 33；与锁映射行 L6393 逐字相同。定义与 HTML 出处均从锁自读，不新造判据、不 LOCK-AMEND。
- 正本：`/home/james/complex-management` 分支 `main`；`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`。B3-r2 after tree `9a975ed491a93d3029f30e7cb415e0ec609b1487`（`git cat-file -t` = tree）。本模块 1 文件工作区 blob `70150ed942b9b57c3ac205e7e291ee1be3321589` 与 after 全等；工作区 SHA256 `f74c793e2db234b9ec458962c70e71112e1887d2285f2600c12be62bc1c45e48`；`wc -l` = 3496；endswith `\\n`=True。计划冻结有效内容行 3494 不改。标题 V0.07。工作区 `git status --porcelain` 该路径为 ` M`（未入真实索引，与 B3 临时索引口径一致）。
- 本席约束：只读业务代码与计划；除本 raw 外不写文件；不修源/三 HTML/锁/findings/ledger；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；不建脚本/日志/临时产物；不跑 Java/HTTP/数据库。
- 未读取：`M014-b5-r2-self-raw.md`（`ls` 见该路径存在；本席未 `read`/`cat`/`grep` 其内容）。旧 B1/B2/B4/B5 可读作已定稿边界，不作本 B5 通过依据。
- 已定稿边界（不代替本轮全量重审）：findings V2.41 — 001–023 当前全部「否」（21 已修 + 019 XM→M261 + 020 登记）。019 归 M261；020 所列实现缺口归各实现模块；021 同根句余下在 M421/M439 只作跨模块观察。全部「否」≠ 功能全实现。

本次审计结论：**FAIL**。

工程缺陷成立、建议入 B2 的临时编号：GR-M014-B5r2-01（细化）。添加项 0。HTML 修改申请 0。questions 0。锁外判据 0。001–023 在当前单文件上独立复验：021/022/023 拟改已落地、无回归；019/020 维持跨模块/登记。本票不等于双席 B5 或模块 PASS。席甲自审票不得覆盖本席；本席 FAIL 具否决权——本席行使否决。

---

## 0. 技能自核（V0.17 §6，按 B5 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体。本步禁止子代理，与技能「先派子代理」冲突处按用户指令收窄 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| graft | `~/.claude/skills/graft/SKILL.md` | 已读 SKILL.md；**未运行** `graft` CLI：工具会刷新 `graft/` 图，超出本席唯一可写路径。入口核验用仓库内 `grep`/`read` |
| grilling / grill-me | mattpocock `skills/productivity/grilling`；`grill-me` 转发 grilling | 只用追问树自己取证，不等操作员、不派子代理 |
| codebase-design | mattpocock `engineering/codebase-design` | 删除测试：本规格若删掉已废止闭集回灌与过期「当前实现」句，复杂度回到 HTML 四类绑定/十一端/状态成员本身；不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | mattpocock `engineering/diagnosing-bugs` | 先建可红反馈环（sha256 / HTML 行命中 / Controller 映射 / V184 / V394 / V410 / V427 / V028）再下结论；文档类不造 harness |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流（规格陈述→HTML 原句→Controller/迁移）；B5 不加 instrumentation、不修 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未运行测试不写通过；Java/HTTP/DB 未实跑 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地；不把 B2 禁止项写成缺陷 |
| writing-for-agents | mattpocock `productivity/writing-for-agents` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code` | 存在；本席未改 git、未直推、未安装 hook（安装会写仓库外配置，B5 禁止） |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒级，未起 monitor |
| i-have-adhd | 会话规则 | 编号、复述「模块 M014 / 共 471，B3 第 2 次」；正文留 raw。CLI 按派发不回正文 |

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
| 计划冻结有效内容行 | 3494（切分时 HEAD）；当前工作区 `\\n` 计数 **3496**（B3-r2 后现态，不改计划凑数） |
| after tree | `9a975ed491a93d3029f30e7cb415e0ec609b1487`；规格 blob `70150ed942b9b57c3ac205e7e291ee1be3321589` = 工作区 `git hash-object` |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`；`HEAD^{tree}` ≠ after（修复未提交） |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| modules.md / runbook.md | SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` / `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| questions/M014* | 无 |
| 当地日 | `date -u` 2026-09-19；变更记录日期 2026-09-19 与 UTC 日一致 |

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 行 | SHA256 |
|---|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | 308 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 2032 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 979 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

锁内本模块 66 条 HTML 锚点来源行（非 GEN）对本席读取的现行 HTML 逐条命中（抽核：BEQR-063 `:111`、BEQR-143 `:226`、BEQR-148 `:231`、BEQR-150 `:233`、BEQR-154 `:237`、BSM-004 `:566`、BSM-028 `:698`、BSM-114 `:1827`、PGM-018 `:415`、PGM-026 `:492`、PGM-041 `:647`、PGM-042 `:664`）。GEN 18 条无 HTML 行。三 HTML 本身未发现需铁律 4 人闸修改的原文错误。HTML 沉默处不升冲突。

---

## 2. 全文件分段覆盖表（全文已读；行=`\\n` 计数）

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | docs/platform-governance-product-spec-v0.2.md | 3496 | 228454 | f74c793e2db234b9ec458962c70e71112e1887d2285f2600c12be62bc1c45e48 | 已读 L1–L3496（标题 V0.07；OVERSIZE 按结构段分批阅读，raw 列全覆盖；未只读标题/搜索/截断/diff） |

endswith `\\n`=True。与 after tree blob 全等。

### 2.1 结构段已读行区间

| 段 | 行区间 | 内容 | 已读 |
|---|---|---|---|
| 包装头 | L1–L25 | 标题 V0.07、2026-07-04/09-01/09-11/09-19 说明、R5 标注、四源归档表 | 是 |
| §0A | L27–L36 | 金额精度/合同本地 AI/当地时间/外部经销商跨篇承接 | 是 |
| 原文件 1 页面清单 | L40–L793 | 定位至变更日志 §20 | 是 |
| 原文件 2 字段字典 | L795–L1336 | 定位至变更日志 §19 | 是 |
| 原文件 3 流程图 | L1338–L2244 | F1–F11、跨依赖、验收、变更日志 §17 | 是 |
| 原文件 4 低保真 | L2246–L2742 | 信息架构至变更日志 §23 | 是 |
| §24 | L2744–L2854 | 有效权限集接口 / 字段投影 / 审批层级映射 | 是 |
| 继续合并补录 | L2856–L3496 | 三源历史摘录载体（无低保真来源段，与 020 观察一致） | 是 |

四源归档路径 `docs/_archive/20260610-retention-audit/merged-layer-spec-sources/` 下 page-list / field-dictionary / flow-diagrams / low-fi 四文件 `ls` 均存在（`_archive` 按 §1.2 排除，不审其内容）。`docs/group-governance-rebuild-prd-v1.0.md`、`docs/platform-governance-prd-v0.1.md`、`docs/merchant-space-contract-model-v0.1.md` 路径存在。

---

## 3. 第一性原理三问（对本模块 1 文件）

1. **为什么必须存在？删掉它，三份 HTML 哪一条做不到？**  
   本文件是平台治理层页面清单 + 字段字典 + 流程图 + 低保真的合并入口，把 PGM-003 模块入口、PGM-017/020 综合体与集团、BEQR-143 四类主体、BEQR-150 十一端、BEQR-154–159 状态闭集、PGM-041/042 权限与职级转成可开发的页面/字段/流程。删掉它，PGM-003「平台治理层包含哪些管理模块」与 BSM-009「基础管理」在产品规格层无单一入口。答得上 → 有业务上位。OVERSIZE 不可再切，标记合法。

2. **声称的行为有没有被实际执行验证过？**  
   文档类：不强制可执行产物。用类型 1（三 HTML 逐字）+ 类型 4（`sha256sum`/`wc`/`git cat-file`/`git hash-object`、Controller `@RequestMapping`+`@GetMapping`/`@PutMapping`/`@PostMapping` 只读对照、迁移文件只读对照）。**未**跑 Maven/Java/HTTP/psql，不冒称实跑。

3. **哪个前提未经验证就当真？**  
   - 「全部否 = 功能全实现」——明确拒绝。§24.2 投影待补齐、§24.3 职级算术、复制模板入口、收费模板字段、经销独立 P0 页仍按 020 归实现模块。  
   - 文中「当前实现 / 现网 / 端点」凡写入口：按对象 grep Controller 映射，不以服务提示语或注释代替。  
   - 补录段是历史摘录载体，不升现行规则。变更日志引述被删词属日志记录。废止框不当新规则。

---

## 4. 顺从式：84 项逐条适用与证据

适用口径：本模块是治理产品规格文档。HTML 派生判据核「规格是否与锚点原文冲突/遗漏/发明」；GEN 核文档可验证性、一致性、分类与证据。实现缺口已由 020 登记或跨模块观察的，本席核对事实仍在、不升新缺陷。

### 4.1 GEN-001 … GEN-018

| ID | 适用 | 证据（类型） | 结论 |
|---|---|---|---|
| GEN-001 | 是 | 合并四源避免重复入口；已废止闭集用【已废止/不得回灌】收窄。现行仍有启用岗位回灌（见 GR-M014-B5r2-01） | 本项支撑该 finding |
| GEN-002 | 是（文档） | 文称不写接口，但 §24 写了当前端点。Controller 映射核验见 §6。文档主路径与错误路径以 HTML 对照，不造执行测试 | 端点声称与映射一致；启用岗位声称无 HTML 枚举支撑 |
| GEN-003 | 本文件无测试 | 文档/说明性 md 不强制测试；不把「测试通过」当证据 | 不适用可执行测试；不宣称测试通过 |
| GEN-004 | 弱适用 | 规格要求输入校验（合同日期、金额分）；无本模块代码 | 无本文件实现缺陷 |
| GEN-005 | 是 | 权限点、项目隔离、高危五类、拒绝优先与 PGM-041/BEQR-151 对照 | 无新冲突 |
| GEN-006 | 弱适用 | 四类主体唯一绑定、合同八态成员；不定义迁移边 | 无新冲突 |
| GEN-007 | 弱适用 | 幂等/并发非本文件实现 | 无本文件缺陷 |
| GEN-008 | 是 | 状态闭集与 BEQR-154–159 对齐；业态六值对齐 BEQR/BSM-072；**岗位「启用」与同文废止不一致** | 支撑 GR-M014-B5r2-01 |
| GEN-009 | 弱适用 | 异常分支表存在；F3 非法迁移已改注释 | 无新缺陷 |
| GEN-010 | 弱适用 | 脱敏七项、密钥持有人；无本文件密钥 | 无本文件缺陷 |
| GEN-011 | 是 | 留痕七态、作废是动作 | 与 BEQR 事件口径一致 |
| GEN-012 | 否（无循环/连接） | — | 不适用 |
| GEN-013 | 弱适用 | 依赖声明非本文件 | 不适用构建 |
| GEN-014 | 弱适用 | 文引 V028/V184/V394/V410/V427，只读迁移文件存在（见 §6） | 引用可复现；不冒称 migrate |
| GEN-015 | 是 | 低保真结构/空状态/中文 | 无新缺陷 |
| GEN-016 | 是 | 路径/版本/完成声明：V0.07、3496 行、after 全等；`dealer_portal` 诚实标未实现；集团视图写「承接落地」+ V394 审批属性，不把项目隔离写成已完成（G7 口径）。**启用岗位写成现行校验** | 支撑 GR-M014-B5r2-01 |
| GEN-017 | 是 | 本席 findings 依冲突→细化→技术性→添加 | GR-M014-B5r2-01 = 细化 |
| GEN-018 | 是 | 结论落到 HTML 原文 + 可复现命令；不以 B4 票为据 | 本 raw 遵守 |

### 4.2 BEQR-063 … BEQR-189

| ID | 锚点 | 适用 | 证据 | 结论 |
|---|---|---|---|---|
| BEQR-063 | `:111` 高危动作 | 是 | 规格高危五类含沽清/反结/存酒作废；不另发明酒吧状态机 | 无冲突 |
| BEQR-074 | `:125` 隐私桌台 | 是 | 低保真/权限页不展开桌台三权细节；不与查看/操作/紧急接管矛盾 | 无冲突 |
| BEQR-143 | `:226` 四类主体 | 是 | 商户字段经营对象/运营/合同/结算 + 绑定关系编号；脚注 `business_entities` 一表多角色 | 无冲突 |
| BEQR-144 | `:227` 经营主体关系 | 是 | §0A/名词脚注/页面清单否定固定隶属链 | 无冲突 |
| BEQR-145 | `:228` 城市项目公司 | 是 | 规格不把项目公司写成管酒吧/游乐 | 无冲突 |
| BEQR-146 | `:229` 运营公司职责 | 是 | 不把娱乐平台写成收单一酒吧门票 | 无冲突 |
| BEQR-147 | `:230` 收入归属 | 是 | 本规格不另设未写明平台收费名目 | 无冲突 |
| BEQR-148 | `:231` 合作模式 | 是 | 六值闭集在商户/合同字段；「其他」须指定收费模板。字段表未列收费模板细项——020 已登记，不升新缺陷 | 无新缺陷 |
| BEQR-149 | `:232` 职级 | 是 | M1–M10 上/中/下；职级只绑薪资大范围（多处已确认） | 无冲突 |
| BEQR-150 | `:233` 终端十一端 | 是 | 十一端中文名全篇一致；产品码 `dealer_portal` 标 V111/OpenAPI 尚无该码 | 诚实未实现，非假完成 |
| BEQR-151 | `:234` 最终权限 | 是 | 五源公式 + 拒绝优先；无通配第六源 | 无冲突 |
| BEQR-152 | `:235` 项目与敏感数据 | 是 | 默认按项目隔离、无关闭开关；敏感经营数据分权 | 无冲突 |
| BEQR-153 | `:236` 开关优先级 | 是 | 七档 + 兜底关闭 + 留痕例外 + 涉钱禁灰度 | 无冲突 |
| BEQR-154 | `:237` 项目状态 | 是 | 六成员；不定义迁移边 | 无冲突 |
| BEQR-155 | `:238` 商户状态 | 是 | 六成员 + 缓冲期 + 不记录试运营 | 无冲突 |
| BEQR-156 | `:239` 合同状态 | 是 | 八值 + 当地时间（§0A） | 无冲突 |
| BEQR-157 | `:240` 员工状态 | 是 | 五值 | 无冲突 |
| BEQR-158 | `:241` 设备状态 | 是 | 七值 | 无冲突 |
| BEQR-159 | `:242` 权限状态 | 是 | 五值 | 无冲突 |
| BEQR-166 | `:255` 合同审批链 | 是 | 主线默认总经理 + 三平行线；三类不得录入即生效 | 无冲突 |
| BEQR-189 | `:291` 管理费方式 | 弱适用 | 规格不展开服务点单管理费；无相反规则 | 无冲突 |

### 4.3 BSM-004 … BSM-120

| ID | 锚点 | 适用 | 证据 | 结论 |
|---|---|---|---|---|
| BSM-004 | `:566` 存档删除 | 弱适用 | 规格留痕/归档；不发明真删除 | 无冲突 |
| BSM-005 | `:581` 分步引导 | 是 | 低保真表单分组，未写成一次填完唯一形态 | 无冲突 |
| BSM-006 | `:584` 动态码 | 弱适用 | 规格不立法核销码刷新间隔 | 无冲突 |
| BSM-007 | `:587` 平台管理员 | 是 | §24.1 返回体复述日常 vs 高危 | 无冲突 |
| BSM-009 | `:596` 基础管理 | 是 | P0 页覆盖综合体/空间/商户/合同/员工/设备/权限 | 无冲突 |
| BSM-028 | `:698` 主要功能 | 是 | 项目/空间/商户六类合作/多对多/合同准入/员工岗位班次 | 无冲突 |
| BSM-029 | `:714` 和谁有关 | 是 | 票务/酒吧/游乐/财务/权限依赖基础对象 | 无冲突 |
| BSM-072 | `:1308` 商户管理 | 是 | 业态六值 + media 括注 | 无冲突 |
| BSM-074 | `:1326` 和谁有关 | 是 | 连接基础管理 | 无冲突 |
| BSM-114 | `:1827` 各类入口 | 是 | 现行 HTML L1865「查看考勤」；规格 L2675「查看考勤」。私人手机不承接现场核销。历史口径保留 | 无冲突 |
| BSM-115 | `:1897` 相互关系 | 是 | 基础管理连所有业务 | 无冲突 |
| BSM-120 | `:1957` 已确认 | 是 | 职级/设备标识/寄存柜等已确认条与规格已确认节一致 | 无冲突 |

### 4.4 PGM-002 … PGM-066

| ID | 锚点 | 适用 | 证据 | 结论 |
|---|---|---|---|---|
| PGM-002 | `:308` 治理顺序 | 是 | 对象→合同→人员→设备→留痕在流程总览 | 无冲突 |
| PGM-003 | `:339` 模块入口 | 是 | P0 页覆盖卡片模块；楼宇/应急/开放接口非本 P0 页，HTML 沉默不升 | 无冲突 |
| PGM-004 | `:342` 综合体和集团 | 是 | 预留集团视图 | 无冲突 |
| PGM-005 | `:347` 空间和点位 | 是 | 九类 + 项目为归属根 | 无冲突 |
| PGM-006 | `:352` 商户和主体 | 是 | 四类主体绑定 | 无冲突 |
| PGM-007 | `:357` 合同和准入 | 是 | 先阻断再升级 | 无冲突 |
| PGM-008 | `:362` 员工和手机 | 是 | 两条注册路径、设备凭证、换机解绑。卡片「岗位状态」无枚举；规格废止岗位三态后仍写启用岗位 → finding | 支撑 GR-M014-B5r2-01 |
| PGM-009 | `:367` 权限和职级 | 是 | 最小颗粒度 | 无冲突 |
| PGM-010 | `:372` 终端开关 | 是 | 十一端 | 无冲突 |
| PGM-011 | `:378` 设备管理 | 是 | 台账/领还 | 无冲突 |
| PGM-012 | `:383` 商户侧 | 是 | 授权范围内全开放、不看全局 | 无冲突 |
| PGM-013 | `:388` 开放接口 | 弱适用 | 规格不阻塞第三方资料 | 无冲突 |
| PGM-014 | `:393` 留痕复核 | 是 | 操作留痕页 | 无冲突 |
| PGM-015 | `:399` 楼宇运营 | 弱适用 | 无独立 P0 页；HTML 有模块卡片，规格不发明相反规则 | 无冲突 |
| PGM-016 | `:404` 应急与隐私 | 弱适用 | 脱敏/密钥在字段原则 | 无冲突 |
| PGM-017 | `:412` 综合体和集团 | 是 | 基础对象带综合体项目归属（商户/空间/合同/员工/设备/票务/看板）；**不**写成每张业务表都有 `project_id` 列（022 已删假完成） | 无回归 |
| PGM-018 | `:415` 多对多 | 是 | 否定固定隶属 | 无冲突 |
| PGM-019 | `:424` 各主体职责 | 是 | 不越权写收入 | 无冲突 |
| PGM-020 | `:440` 主要功能 | 是 | 项目档案 + 归属 + 集团下钻已承接时态 | 项目聚合≠结果隔离（历史口径） |
| PGM-021 | `:452` 业务意义 | 是 | 避免数据混 | 无冲突 |
| PGM-026 | `:492` 商户主要功能 | 是 | 第一/第二联系人、六类合作、四类绑定 | 无冲突 |
| PGM-027 | `:507` 业务边界 | 是 | 商户侧全开放 | 无冲突 |
| PGM-031 | `:555` 经销治理 | 是 | §0A 承接；无独立 P0 页——020 已登记 | 不升新缺陷 |
| PGM-035 | `:599` 提成发放 | 弱适用 | 规格不展开提成发放 | 无冲突 |
| PGM-040 | `:643` 单一功能授权 | 是 | 权限点示例 | 无冲突 |
| PGM-041 | `:647` 权限规则 | 是 | 五源、隔离、高危五类、岗位/组织变更重算。021 已删「仅职级变化不触发」。触发列表仍与 HTML 同句（岗位/职责/所属组织），职级变化由代码 `PUT .../staff-level` 重算，规格不再排除 | 021 无回归 |
| PGM-042 | `:664` 职级规则 | 是 | 职级影响默认模板但不能替代功能权限点；L1993 已改 | 021 无回归 |
| PGM-043 | `:673` 外包审计 | 弱适用 | 脱敏七项在字段原则 | 无冲突 |
| PGM-044 | `:688` 桌台挂单经销权限 | 弱适用 | 规格不发明相反权限组 | 无冲突 |
| PGM-048 | `:749` 商户侧 | 是 | 边界表不在 P0 页展开，无相反规则 | 无冲突 |
| PGM-060 | `:879` 会员生物 | 弱适用 | 规格不立法会员注册 | 无冲突 |
| PGM-065 | `:935` 连接关系 | 是 | 跨流程依赖表 | 无冲突 |
| PGM-066 | `:951` 已拍板 | 是 | 集团视图/权限颗粒度/留痕/开关 | 无冲突 |

84/84 已核对。无锁外主张。

---

## 5. 对抗式（grilling 自问，不等操作员）

Frontier（已用现行全文收口，不留未答）：

1. **021 是否只删了排除句、触发列表仍漏职级？** HTML PGM-041 重算句本身是「保存岗位、职责或所属组织变更」，与规格 L1982 同文；「HR 定层级/职位即自动适配」是另一句。B2 最小修是去掉「仅职级变化…不触发」。现网 `AccessController:262 PUT /users/{id}/staff-level` → `AccessService:1305–1313` 已定岗改职级重算。规格不再排除。同根句在 M421/M439 按操作员口径只观察。**不新开 021。**

2. **022 补录 L2997 是否回灌 project_id 假完成？** 该行标【历史摘录，现行时态已更新】，写「综合体项目归属」而非「所有对象继续携带 `project_id` 并按项目归属聚合」。现行 L766/L2721 止于「承接落地。」。**不新开 022。** 项目聚合≠项目结果隔离。

3. **023 变更日志/补录是否回灌业态编号？** 现行 L234/L2419「业态名称、说明」。L930 字段「业态编号」已废止。L782/L2733 日志引述被删词。补录 L3288 业态编号行已废止。**不新开 023。**

4. **启用岗位/职级是否理论场景？** 否。班次保存与 F7 分配是 P0 正常路径。同文件 L1040 已废止「必须选择启用岗位」、L1061 已废止「是否可分配员工」，现行 L1072/L1851/L1058 仍作校验。第一次按规格实现班次/分配就会回灌岗位状态机。

5. **§24.1 端点是否存在？** `AccessController` `@RequestMapping("/api/v1/access")` + `@GetMapping("/effective-permissions")` L244–247；`@GetMapping("/users/{id}/effective-permissions")` L250–253 且 `requirePermission("access.user.view")`。`AccessService.effectivePermissionsForUser` 转到 `AuthorizationService.effectivePermissionsForUser`。`AdminApp.tsx` 调用 `hasPermissions`。类型 4 只读，未 HTTP 实跑。

6. **复制模板入口？** `@PostMapping("/permission-templates/{id}/copy")` L76 仍在。规格已废止复制种类。020 归 access 实现模块。**不在本模块改、不升新缺陷。**

7. **§24.3 职级算术？** L2853 带日期核验「现网仍按职级算术与 specific_role」。020 已登记，归 governance/review。**不升新缺陷。**

8. **V392 六类？** 本文件无把 grant_request 六类写成 HTML 高危五类。历史口径保留。

9. **私人手机打卡？** 现行 HTML BSM-114 L1865 与 PGM L741 均为「查看考勤」；规格 L2675 同。打卡仅工作手机。历史口径保留。

10. **废止框当新规则？** 废止框本身不立法。问题是现行校验行回灌了废止内容（finding）。

11. **L1852/L1853 重复「班次缺责任区域」？** 重复行，ponytail shrink；不构成业务发明或 HTML 冲突。观察，不进队列。

12. **§12A.6 自指？** L2132「统一按 §12A.6 执行」即本节。020 已登记。不升。

13. **计划 3494 vs 现行 3496？** 不改计划。020 已记行数漂移类观察。

对抗最小反例（文档类）：按 L1072 实现「当班岗位必须启用」→ 与 L1040 废止及 HTML 无岗位启用闭集冲突。成立。

---

## 6. 当前实现声称 → 源码/迁移/入口（只读）

凡文中写「端点/入口/接口」，按对象 grep Controller 映射：

| 声称 | 规格位置 | 核验 | 结果 |
|---|---|---|---|
| `GET /api/v1/access/effective-permissions` | L2750/L2760/L2783 | `AccessController.java:32` + `:244-247` `effectivePermissions()` | 入口存在。未 HTTP 实跑 |
| `GET /api/v1/access/users/{id}/effective-permissions` | L2761/L2783 | `:250-253`；`access.user.view` | 入口存在。未 HTTP 实跑 |
| `PUT /users/{id}/staff-level`（对照 021，非本文件新接口） | 021 背景 | `:262-278` → `AccessService:1305-1313` 已定岗改职级重算 | 入口存在 |
| `POST /permission-templates/{id}/copy` | 废止项 / 020 | `:76-84` 仍在 | 实现缺口归 access；规格已废止种类 |
| `contract_key_dates` / `compliance_expiry_items` | L1001/L962 | `V028__p1_contract_compliance_expiry.sql` 建表 | 迁移文件存在。未跑库 |
| `space_type_dictionary` V184 | L1289 | `V184__space_type_dictionary.sql` 种子 building/floor/zone/shop/public_area/parking/booth/other | 与括注一致；与 HTML 九类差已诚实登记 |
| V394 `analytics.group.view` `requires_approval=true` | L766/L2721 | `V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql:16-17` 含该码 | 迁移文件存在。未跑库 |
| V410 删 `parent_entity_id` | L53 | `V410__m010_business_entity_topology_and_role_isolation.sql:9` `DROP COLUMN IF EXISTS parent_entity_id` | 迁移文件存在 |
| V427 `business_entity_projects` | 022 背景 | `V427__m009_project_profiles_and_associations.sql:13` | 多对多表存在。规格不再写每对象 `project_id` 列 |
| `dealer_portal` 未进 V111/OpenAPI | L872 | 规格自承未实现 | 非假完成 |
| `ActionGateAspect` / evaluate 现状 | L14 R5 2026-09-11 行 | 本席不把 2026-06-18 过期句当现行；005 已修 | 无回归 |

未冒称 Java/HTTP/数据库实跑。

---

## 7. 021/022/023 独立复验（不复用 B4 票）

| ID | 现行原句 | 行 | 复验 |
|---|---|---|---|
| 021 | `本规则不把任意手工模板应用列为触发条件。` | 1982 | 不含「仅职级变化、责任范围变化或」 |
| 021 | `职级与敏感权限不匹配 \| 职级不替代功能权限点；敏感权限授出仍按五类高危先审批` | 1993 | 不以职级挡位替代功能权限点 |
| 022 | 后续细化 11 止于「承接落地。」 | 766 | 无「所有对象继续携带 `project_id` 并按项目归属聚合」；V394 句保留 |
| 022 | 低保真后续细化 12 同 | 2721 | 同上 |
| 023 | `业态列表 \| 业态名称、说明` | 234、2419 | 无「编号」列；详情行未改坏 |
| 版本 | `# 平台治理产品规格 V0.07` | 1 | 是 |
| 文首 | 2026-09-19 B3-r2 说明 + 空行 + R5 原句 | 8–10 | 是 |
| 日志 | 页面清单 §20 / 流程图 §17 / 低保真 §23 各 2026-09-19 行 | 782、2234、2733 | 是；字段字典 §19 无该日行（定稿如此） |

无回归。

---

## 8. 本次新问题

### GR-M014-B5r2-01

- **文件:行号**：`docs/platform-governance-product-spec-v0.2.md:1058`、`:1072`、`:1851`（对照同文件 `:1040`、`:1061`）
- **原句**：
  - L1058：`应选择启用职级`
  - L1072：`应为启用岗位`
  - L1851：`岗位当前不可分配员工 | 不允许继续分配`
  - 对照 L1040：`选择已有岗位。【已废止/不得回灌】「必须选择启用岗位」`
  - 对照 L1061：`【已废止/不得回灌】是否可分配员工` … `岗位状态本规格不定义——三份业务 HTML 无对应状态枚举`
- **正常触发**：平台运营在班次排班页保存班次并选择当班岗位，或在 F7 为已审核员工分配岗位。按现行校验句会要求「启用岗位 / 可分配员工 / 启用职级」。HTML 无岗位启用/停用闭集、无「是否可分配员工」字段；同文件已废止这两项。第一次按规格实现即回灌岗位状态机。
- **四分类顺序**：冲突（HTML 无「启用岗位」明文可矛盾）→ **细化**（上位：PGM-008 员工和手机 / BSM-028「管理员工、岗位、班次」/ BEQR-157 员工状态五值；指得出 HTML 条目）→ 不进技术性 → 非添加。
- **已有判据**：GEN-001、GEN-008、GEN-016、PGM-008、BEQR-157。不 LOCK-AMEND。
- **证据类型**：1（HTML：员工状态仅为待入职/在职/试岗/停岗/离职；无岗位启用枚举；PGM-008 卡片「岗位状态」无取值）+ 4（本席 `read` 上列行；`grep`「启用岗位|启用职级|不可分配员工」仅这些现行命中；补录 L3129「岗位停用」属历史摘录载体，不升现行）。
- **工程缺陷是否成立**：是。建议修复队列「是」。最小方向（本席不修）：L1058 去掉「启用」改为选择已有职级档；L1072 与 L1040 对齐为选择已有岗位；L1851 删除或改为仅用员工状态五值（停岗/离职已有 L1850）。不改 HTML、不改 AccessService、不重开 021。
- **不纳入本条**：补录 L3129（历史摘录）；L1060「应选择启用角色」（权限状态含「已启用」，BEQR-159 有明文）。

添加项：无。  
HTML 申请：无。

### 观察（不进修复队列，不升缺陷）

- 020 续记仍真：§24.3 职级算术、复制模板入口、收费模板字段、经销无独立 P0 页、§24.2 投影待补齐、§12A.6 自指、计划 3494 vs 现行 3496、补录无低保真来源段、P0 页名与 adminRoutes 不一一对应。
- 019 仍归 M261。
- 021 同根句在 M421/M439 只观察。
- L1852 与 L1853 重复「班次缺责任区域」——shrink，非业务发明。
- 锁内 BSM-114 冻结原文曾写「考勤打卡」；现行 HTML L1865 已是「查看考勤」，规格跟随现行 HTML 与已批口径。不 LOCK-AMEND、不升冲突。

---

## 9. 三线执行记录

| 线 | 执行 | 结果 |
|---|---|---|
| 第一性原理 | 三问落盘；OVERSIZE 七段全读 | 见 §3 |
| 顺从式 | 84/84 逐项 | 见 §4；1 项支撑新 finding |
| 对抗式 | grilling 12 问 + 启用岗位反例 | 见 §5–§8 |

覆盖：本模块 1/1 文件有覆盖记录。  
声称：端点/表/迁移均有类型 1 或 4 证据；未答 questions = 0。  
Grok 哈希行：本文第一行，独立实算。

---

## 10. PASS/FAIL 闸

单席 PASS 必须：全文件覆盖 ✓；84 项核对 ✓；三线执行 ✓；声称有四类证据之一 ✓；无未答 questions ✓；**无未定稿缺陷 ✗（GR-M014-B5r2-01）**；哈希行有效 ✓。

因此本席步骤结论：**FAIL**。

模块生命周期不得写作 PASS。B3 次数本席不改。回退按 V0.17：本条无 ID 且不在修复队列 → 整批先回 B2。

本席未修业务/HTML/锁/findings/ledger，未 fetch/commit/push/merge/deploy/写 DB，未建脚本日志临时产物，未读本轮 self-raw。
