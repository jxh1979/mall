CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M014 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 修复前整模块全量审计
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 模块：M014 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第2段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 1 文件，计划冻结有效内容行 3494，标记 OVERSIZE-SINGLE-FILE；当前 `git ls-files -z -- docs/platform-governance-product-spec-v0.2.md | xargs -0 cat | wc -l` = **3494**（与计划冻结一致）
- 冻结判据：84 条（`M014 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-189, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-060, PGM-065, PGM-066]`，与锁映射行 L6393 逐字相同；COUNT=84 unique=84 MISSING=[] EXTRA=[]）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M014-001 … GR-M014-016
- 添加项（B1 只登记，不代用户回答 2.2；本批无）：无
- 只登记、建议队列「否」：观察见 §8（不升 finding）
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓或发布门禁通过。M001–M013 及 U01–U18 已闭环，不重开。原 322 模块编号及旧结论不代表当前 471 模块完成。findings V1.86 仅作已登记边界背景。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 删除测试：本规格若删掉已废止闭集回灌与过期「当前实现」句，复杂度回到 HTML 四类绑定/十一端/状态成员本身；不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（sha256/HTML 行命中/V184 字典/V394 requires_approval/OpenAPI path/`ActionGateAspect`）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（规格陈述→HTML 原句→V184/V394/V410/`PlatformService`/OpenAPI/`adminRoutes`） |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；`verify:openapi` exit 0 ≠ 规格与 Java 枚举/HTML 一致 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推、未安装 hook（安装会写仓库外配置，B1 禁止） |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒级，未起 monitor |
| domain.md | `docs/agents/domain.md` | 词表/三 HTML 最终解释权；未改 CONTEXT.md |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit` 技能会派评审子代理并另写 `docs/arc/audits/`：与双席/§7 冲突，**不调用**
- superpowers `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄
- Trail of Bits `audit-context-building` 会派 function-analyzer 子代理并另写 `audit-context/`：与禁止子代理冲突，**不调用**

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain、MCP、第三主体。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行 `M014 → [...]`（L6393）与开工口径 84 项逐字相同
- `plan/modules.md` M014 FILES 1 条与用户清单逐字相同
- 1 文件 `git ls-files -z -- docs/platform-governance-product-spec-v0.2.md | xargs -0 cat | wc -l` → **3494**
- 该文件字节/SHA256 与用户固定清单 match=True（见 §2）

HEAD：`main` = `origin/main` = `685dca4e1088ed59e68196c3c5b45787fde9db02`（`rev-parse HEAD` / `origin/main` 两值相等）。本席未设 `GIT_INDEX_FILE`，未动主索引。

工作区：`git status --porcelain` = ` M docs/audit-2026-09/ledger.md`（M014 PENDING→IN_PROGRESS 的台账写入）。开工提示称「工作区干净」与此不完全一致；本 1 文件未改。本席只读，未再写 ledger。

ledger 只读核验（不改）：V3.22，581611 bytes，SHA256 `323888d6906630b35a6ef5482a6eb3ce758d53005d56d01aae7d62827dc82327`；M014 行 IN_PROGRESS、B3=0。findings V1.86，734491 bytes，SHA256 `2008c37d623535d144fa080b93435716895af052479ee5e2afa3dec1d77969c2`，仅作已登记边界背景（M013-001…010：V410 三角色/删父链、V394 已闭合、自营＝self_operated、空间字典活校验、DELETE 恒 409 走 lifecycle、税费权限码落点缺、evaluate 员工域已接线、私人手机「查看考勤」、OpenAPI entity_role 旧六值归 M392；U04-001；业务地图 V0.14 / 治理地图 V0.12），不代替本模块审计。

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

三份 HTML 只读、未改。锁内本模块 HTML 锚点来源行对本席读取的现行 HTML **命中**（脚本：BEQR-143 `:226`、BEQR-148 `:231`、BEQR-150 `:233` 终端十一端、BEQR-154 `:237`、BEQR-155 `:238`、BEQR-156 `:239`、BSM-004 `:566`、BSM-028 `:698`、BSM-114 `:1827`、PGM-018 `:415`、PGM-026 `:492`、考勤 L1445、空间 L469、私人手机 L741 均 hit=True）。BEQR-150 锁行是 `:233` 不是 `:234`（`:234` 是 BEQR-151 最终权限计算）。

三 HTML 当前字节/SHA256（只读核验，本席未改）：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| docs/business-enum-quick-reference-v0.1.html | 133961 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | 304482 | eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 |
| docs/platform-governance-map-for-operations-v0.1.html | 103015 | a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 |

治理地图哈希相对 M013 时代已变（PR #433 V0.12）。三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。BSM-001/PGM-001 未映射本模块，只作对照。

## 2. 全文件分段覆盖表（全文已读；行=splitlines=`\n` 计数）

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | docs/platform-governance-product-spec-v0.2.md | 3494 | 221816 | b3ee635aa1763d1f5b1e6e7c7e381fe78bc71057c162caad39ae8cd3b8bbcd31 | 已读 L1–L3494（标题 V0.05；OVERSIZE 按结构段分批阅读，raw 列全覆盖；未只读标题/搜索/截断） |

用户固定清单逐文件 match。endswith `\\n`=True。

### 2.1 结构段已读行区间（四份原文件 + §24 + 补录）

| 段 | 行区间 | 内容 | 已读 |
|---|---|---|---|
| 包装头 | L1–L21 | 标题 V0.05、R5 缺口标注、合并说明与四源归档表 | 是 |
| §0A | L22–L33 | 金额精度/合同本地 AI/当地时间/外部经销商跨篇承接 | 是 |
| **原文件 1 页面清单** `platform-governance-page-list-v0.1.md` | **L35–L787** | | |
| 定位 | L35–L48 | 文档定位；名词四层脚注（`business_entities` 一表多角色；权威指向 `merchant-space-contract-model-v0.1.md`） | 是 |
| §2 分组总览 | L50–L68 | 15 个 P0 页（含终端开关/注册审批） | 是 |
| §3 全局规则 | L70–L94 | 筛选/状态/留痕/空状态；合同八态示例 | 是 |
| §4 治理总览页 | L96–L134 | 指标、待办、商户/合同/设备分布、十一端 | 是 |
| §5 综合体项目页 | L136–L168 | 六态成员；不定义迁移边 | 是 |
| §6 空间区域页 | L170–L211 | 空间树含「项目」；废止是否可经营 | 是 |
| §7 业态管理页 | L213–L241 | 六值闭集；不定义业态状态 | 是 |
| §8 商户管理页 | L243–L286 | 六态；暂停/终止/黑名单限制新增绑定 | 是 |
| §9 合同管理页 | L288–L372 | 八态；§9.6 准入收口 supervisor_review | 是 |
| §10 员工岗位+注册审批 | L374–L458 | 员工五态；设备凭证；两条注册路径；自营标注视同公司自有 | 是 |
| §11 班次排班页 | L460–L494 | 不定义班次状态 | 是 |
| §12 设备台账页 | L496–L525 | 设备七态 | 是 |
| §13 设备领还页 | L527–L561 | 沿用设备七态 | 是 |
| §14 角色权限页 | L563–L604 | 权限五态；废止复制模板 | 是 |
| §14A 终端开关页 | L606–L650 | 十一端；废止定时启停；生效/失效为技术字段 | 是 |
| §15 操作留痕页 | L652–L688 | 事件七态；作废是动作 | 是 |
| §16 关键跳转 | L690–L715 | 页间跳转表 | 是 |
| §17 P0 页面验收 | L717–L731 | | 是 |
| §18 已确认/细化 | L733–L761 | 含 `analytics.group.view`「当前实现缺口」 | 是 |
| §19–§20 | L763–L787 | 结论与变更日志 | 是 |
| **原文件 2 字段字典** `platform-governance-field-dictionary-v0.1.md` | **L788–L1329** | | |
| 定位与规则 | L788–L837 | 脱敏七项；不提供关闭开关 | 是 |
| §3 全局字段 | L839–L873 | 废止通用名称/编号/状态门控；§3.1 十一端编码 `dealer_portal` | 是 |
| §4 项目字段 | L875–L890 | 项目六态 | 是 |
| §5 空间字段 | L892–L914 | 声称 HTML 并集含「项目」；废止是否可经营/可留痕 | 是 |
| §6 业态字段 | L916–L925 | 六值；**业态编号 bar/catering/… 必填** | 是 |
| §7 商户字段 | L927–L959 | 四类主体绑定；合作模式六类；第一/第二联系人 | 是 |
| §8 合同字段+§8.1 | L961–L1014 | 合同八态；审批三档；不折算职级 | 是 |
| §9 员工/岗位/班次 | L1016–L1069 | 员工五态；废止指纹作凭证定义 | 是 |
| §10 设备字段 | L1071–L1090 | 设备七态；类型清单含「NFC 设备」 | 是 |
| §11 权限字段 | L1092–L1112 | 权限五态；废止复制来源 | 是 |
| §12–§13 留痕/筛选 | L1114–L1146 | 留痕七态 | 是 |
| §14 状态字典 | L1148–L1240 | 各对象闭集；空间/业态/班次/注册/私人手机/终端开关不定义 | 是 |
| §15–§19 | L1242–L1329 | 覆盖关系、验收、已确认、变更日志 | 是 |
| **原文件 3 流程图** `platform-governance-flow-diagrams-v0.1.md` | **L1330–L2245** | | |
| 定位+总览 | L1330–L1391 | F1–F11 表；关系 mermaid **无 F11 边** | 是 |
| F1 项目初始化 | L1393–L1439 | 不定义项目迁移边 | 是 |
| F2 空间配置 | L1441–L1501 | 废止是否可经营路径 | 是 |
| F3 商户入驻与合同生效 | L1503–L1591 | 商户不定义迁移边；**§5.6 合同 stateDiagram 自造边**；L1541 非法 mermaid 行 | 是 |
| F4 经营范围绑定 | L1593–L1651 | 合同已生效才绑定 | 是 |
| F5 合同准入 | L1653–L1717 | 先阻断再升级；七类红线除外 | 是 |
| F6 注册/设备凭证 | L1719–L1798 | 两条路径；废止 NFC 身份/指纹 | 是 |
| F7 入职岗位班次 | L1800–L1871 | 不定义员工/班次迁移边 | 是 |
| F8 设备领还 | L1873–L1939 | 设备只定义成员 | 是 |
| F9 权限 | L1941–L2017 | 五类高危须审批；隔离无关闭开关 | 是 |
| F10 留痕 | L2019–L2082 | 事件七态；不定义迁移边 | 是 |
| F11 终端开关 | L2084–L2161 | 七档；废止定时启停 | 是 |
| §13–§17 | L2163–L2245 | 依赖、验收、已确认、变更日志 | 是 |
| **原文件 4 低保真** `platform-governance-low-fi-prototype-v0.1.md` | **L2246–L2741** | | |
| 定位+目标+IA | L2246–L2292 | §3.1 一级导航把商户合同/人员设备写成独立一级 | 是 |
| §4 全局结构 | L2294–L2358 | | 是 |
| §5–§11 各页低保真 | L2360–L2510 | 总览/项目/空间/业态/商户/合同/准入 | 是 |
| §12–§18 人员设备权限留痕 | L2512–L2664 | | 是 |
| §17A 终端开关低保真 | L2626–L2646 | **开关列表仍列生效/失效时间** | 是 |
| §19 终端说明 | L2666–L2677 | **私人手机「只适合注册、凭证、通知和状态查看」** | 是 |
| §20–§23 | L2679–L2741 | 验收；§21.12 再写 group.view 缺口 | 是 |
| **§24 有效权限/字段投影/审批映射** | **L2742–L2853** | | |
| §24.1 | L2748–L2781 | 标题 `GET /me/effective-permissions`；正文 `/api/v1/access/effective-permissions` | 是 |
| §24.2 | L2783–L2810 | 字段级投影待补齐；死代码已删 | 是 |
| §24.3 | L2812–L2853 | GOV 三档；代码核验仍写职级算术/`specific_role` | 是 |
| **继续合并补录（2026-08-28）** | **L2854–L3494** | 无低保真来源段 | |
| 补录头 | L2854–L2858 | 已废止不得回灌；以三 HTML 为准 | 是 |
| 来源：页面清单 | L2860–L3004 | 70 条摘录；空间树不含项目；废止楼栋/区块 | 是 |
| 来源：流程图 | L3006–L3230 | 110 条摘录；废止项目/商户迁移边与仅预警 | 是 |
| 来源：字段字典 | L3232–L3494 | 141 条摘录；废止业态编号；终端编码 `distributor_web`；L3343 仍写 `admission_audits` | 是 |

合计覆盖 L1–L3494，无抽样宣称。补录未单列低保真原文件来源（观察 OBS-03）。

## 3. 数据流（规格陈述 → HTML 原句 → main 实现事实）

本模块为文档与接口定义类。实现对照只读，不作修复对象。

| 规格声称 | HTML 原句（锁） | main 实现事实 |
|---|---|---|
| 四类主体=经营对象/运营/合同/结算，同一经营对象唯一绑定 | BEQR-143 `:226`；PGM-026 `:492` | `BusinessEntityBindingRecord` 四 ID；创建写 `active`。本规格字段字典 §7 有四主体+绑定编号 |
| 物理项目与运营公司多对多，不是固定隶属链 | BEQR-144 `:227`；PGM-018 `:415` | V410 `DROP COLUMN parent_entity_id`；`BUSINESS_ENTITY_ROLES`=platform/project_company/management_company。**本规格正文无 parent_entity_id 列名**；L48 却把仍写该列为「当前字段」的 `merchant-space-contract-model-v0.1.md` 抬成权威 |
| 合作方式六类；自营是合作方式不是独立布尔 | BEQR-148 `:231`；BSM-028 | `MERCHANT_COOPERATION_FORMS` 含 `self_operated`。规格用「商户档案上标注为自营」，未写 `is_self_operated` |
| 终端十一端 + 七档开关 | BEQR-150 `:233`；BEQR-153；PGM-010 | `ActionGateAspect` 先 `TerminalSwitchGuard.check` 再准入再权限。规格 §14A/F11 七档与十一端对齐；R5 文首仍写「后端业务接口未独立校验开关」 |
| 项目/商户/合同/员工/设备/权限状态闭集 | BEQR-154…159 | 规格状态字典与 ENUM 成员逐字同；空间/业态/班次/注册/私人手机/终端开关明确不定义 |
| 考勤打卡=领工作手机（NFC/扫码/离充，取最早） | BSM L1445 | 规格多处「NFC 仅用于领取工作手机的上班打卡」与 L1445 同旨，**不是**私人手机打卡 |
| 私人手机只开放低风险功能，含「查看考勤」 | BSM `:930`/`:1865`；PGM `:741` | 规格低保真 §19 写成「只适合注册、设备凭证绑定、通知和状态查看」——**收窄 HTML** |
| 空间对象 | PGM 空间 `:469` 楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房、设备点位 | `createSpace` 活校验 `space_type_dictionary`；V184 种子 building/floor/zone/shop/public_area/parking/booth/other（含楼栋/区块，**无 mall、无「项目」码**） |
| 集团视图 | PGM-017/020/066 | `GET /api/v1/analytics/group-overview`；`analytics.group.view` V394 `requires_approval=TRUE`。规格 L761/L2721 仍写「当前实现缺口」 |
| 合同准入先阻断再升级 | PGM-007；PGM 合同章；七类红线 `:548` | `AdmissionGuard` + `ActionGateAspect` 在 `@GuardedAction` 前拦截；`StaffApprovalService` 调 `reviews.evaluate`。规格 R5 文首仍写 evaluate 孤立、业务域零调用 |
| 最终权限五源 DENY 优先 | BEQR-151 `:234` | `GET /api/v1/access/effective-permissions` 在 OpenAPI L8098 与 `AccessController`；规格 §24.1 正文路径正确、**标题仍写 `/me/`** |
| 职位模板删除走 lifecycle | BSM-004 | 本规格不定义 DELETE 行为（无实现面）；不与恒 409 冲突 |
| OpenAPI `entity_role` 旧六值 | （实现缺口，M013-009 归 M392） | OpenAPI `BusinessEntityRequest.entity_role` 仍 operation/contract/settlement/invoice/payment/other。**本规格未复述该六值** |

`npm run verify:openapi` → **exit 0**（契约自洽）。该门禁**不**证明规格与 HTML/Java 白名单一致。
`npm run verify:entry-anchors` → **exit 0**。
`npm run verify:html-ledger-sync` → **exit 0**（本席未改 HTML）。

## 4. 三线审计

### 4.1 第一性原理（本文件三问）

1. **为何必须存在**：删掉则 PGM-003 模块入口所对应的页面/字段/流程/低保真与 §24 有效权限接口在产品规格层无统一载体；BEQR-143/148/150/154–159、PGM-005/008/010/026/040 无 P0 页面与字段闭集。有上位。OVERSIZE 单文件是计划合法切分，不是删除理由。
2. **声称是否执行**：
   - 自称替代四份分散源、§0A 与 HTML 冲突时以 HTML 为准——后文多处未执行（空间类型、私人手机功能、group.view 缺口、R5 文首实现声称）。
   - §9.6 / §24.3 声称准入落 `supervisor_review_decisions`、不建 `admission_audits`——补录 L3343 仍写写入 `admission_audits`。
   - §24.1 声称现网已落地 `/api/v1/access/effective-permissions`——OpenAPI L8098 存在，标题 `/me/` 未执行。
   - §18.11 / §21.12 声称 `analytics.group.view` 审批属性「当前实现缺口」——V394 已 `requires_approval=TRUE`，声称的「当前」未执行。
   - R5 文首声称 evaluate 孤立、业务域零调用、终端开关后端未校验——`ActionGateAspect` L40–50、`StaffApprovalService` L44/L61 否定该「当前」。
3. **未验证就当真的前提**：
   - L48「权威实体关系以 merchant-space-contract-model 为准」把非 HTML、且仍列 `parent_entity_id` 为当前字段的文档抬成裁决层（该文 L64–66）。
   - L896「空间类型以三份业务 HTML 并集收敛为项目、…」把 HTML 没有的「项目」写成 HTML 并集。
   - L1291「技术根节点为 `mall`」当作物理实现事实。
   - 修订说明把 D8 改写结果写成 `device_key_fingerprint` 现行定义。

### 4.2 顺从式（假定规格意图正确，核对 84 条）

见 §6。映射 ID 全部核对。无 finding 的 ID 仍有覆盖记录。文档类不强制可执行测试（runbook §5）；GEN-003 对本文件标注「无独立可执行测试面，不把未运行测试写通过」。

### 4.3 对抗式（假定规格错误；grilling 自答，不问操作员）

| # | 追问 | 事实 | 收窄 |
|---|---|---|---|
| Q1 | 是否仍把已删父链写成当前？ | 正文无 `parent_entity_id`；L48 引用的模型文 L64 仍列该列为当前字段 | 首次正常阅读 L48 就会把已删列当权威 → **GR-M014-006** |
| Q2 | 独立自营布尔？ | 规格用「标注为自营」；无 `is_self_operated` | 不升 |
| Q3 | entity_role 旧六值？ | 本规格未复述；OpenAPI 仍旧六值（M392） | 观察 OBS-01，不重开 M013-009 |
| Q4 | NFC/指纹作身份？ | 正文多处【已废止】；修订说明仍把指纹写成 D8 现行改写结果 | **GR-M014-016** |
| Q5 | 定时启停？ | 字段/14A/F11 已废止；17A.1 列表仍列生效/失效时间 | **GR-M014-011** |
| Q6 | 私人手机打卡？ | 规格打卡绑工作手机 NFC，与 L1445 一致 | 不升 |
| Q7 | 私人手机功能是否按 HTML 列全？ | §19 收窄 | **GR-M014-003** |
| Q8 | 空间类型是否可按规格选「项目」而过活校验？ | V184 无该码，`createSpace` 拒 | 规格当 HTML 并集 → **GR-M014-001/002** |
| Q9 | F11 是否在总览图可达？ | 表有图无 | 首次读总览就会漏 → **GR-M014-012** |
| Q10 | `/me/effective-permissions` 是否存在？ | OpenAPI 无此路径 | **GR-M014-008** |
| Q11 | 理论极端：补录 ~~~~text 读者把未标废止的 admission_audits 当现行 API？ | 补录头声明以 HTML 为准，但 L3343 未标废止 | 首次读补录当现行会误用 → **GR-M014-014**（不要两三件不巧才成立的路径） |

## 5. Findings（临时 ID；正式 ID 仅 B2 分配）

### GR-M014-001 — 空间类型把「项目」写成 HTML 并集，与治理地图空间对象闭集相反

- **文件:行号**：`docs/platform-governance-product-spec-v0.2.md:896`（并 L187、L2392 空间树）
- **原文**：`空间类型以三份业务 HTML 并集收敛为项目、楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房、设备点位；不设楼栋、区块层级`
- **HTML**：PGM 空间 `:469`「维护楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位。」——**无「项目」作为空间类型**。同文件补录 L3267 正确排除楼栋/区块且**不含项目**。
- **实现**：V184 八码无 project/mall。
- **业务影响**：页面/字段/低保真把「综合体项目」降成空间类型，读者会按十类（含项目）建模；与 HTML 空间对象及项目作为独立管理单元冲突。
- **可复现**：`sed -n '896p;187p;2392p;3267p' docs/platform-governance-product-spec-v0.2.md`；`sed -n '469p' docs/platform-governance-map-for-operations-v0.1.html`
- **冻结判据**：PGM-005、GEN-008、GEN-016、GEN-017
- **2.3 依据**：类型 1（HTML 原文）+ 类型 4（命令）
- **四分类**：**冲突**（伪称 HTML 并集；冲突优先）
- **工程缺陷**：是，建议入队
- **最小处置**：删除「项目」作为空间类型；空间树与 HTML `:469` 九类对齐；不得把项目实体写成 space_type。

### GR-M014-002 — 把 `mall` 九类写成当前物理实现，与 V184 活字典相反

- **文件:行号**：L1291–L1292
- **原文**：`物理实现为扁平 spaces：space_type 标识上述九类空间对象，技术根节点为 mall，通过 parent_space_id 串联自套层级。`
- **实现**：`V184__space_type_dictionary.sql` 种子 building/floor/zone/shop/public_area/parking/booth/other，**无 mall**；`PlatformService.createSpace` 硬校验 active `type_code`。
- **业务影响**：规格把计划/旧根类型写成当前事实；按「mall」对接会过不了活校验。V184 的「楼栋/区块」与规格「不设楼栋、区块」同时成立，文档未登记为字典可维护差距。
- **可复现**：`rg -n "mall" backend/src/main/resources/db/migration/V184__space_type_dictionary.sql` → 无命中；`sed -n '14,23p' backend/src/main/resources/db/migration/V184__space_type_dictionary.sql`
- **冻结判据**：PGM-005、GEN-008、GEN-016
- **2.3 依据**：类型 4
- **四分类**：**细化**（PGM-005 空间对象的实现转译）
- **工程缺陷**：是，建议入队
- **最小处置**：删除 `mall` 根节点声称；改为「活校验 space_type_dictionary，种子见 V184」；楼栋/区块与 HTML 九类的差登记为字典可维护，不得写成 HTML 已批层级。

### GR-M014-003 — 私人手机 P0 能力收窄，删掉 HTML 已批低风险功能（含查看考勤）

- **文件:行号**：L2675（低保真 §19）
- **原文**：`员工私人手机 App | 只适合注册、设备凭证绑定、通知和状态查看，不承接现场业务操作`
- **HTML**：BSM `:930`/`:1865` 注册、看公告、查工单、帮客人订位、看排班、**查看考勤**、请假、查薪资；PGM `:741`「员工自有手机只开放低风险功能（公告、我的工单、帮客人订位、查看考勤、请假、查自己薪资等）」
- **业务影响**：治理规格把已拍板的私人手机能力收成「通知和状态查看」，与「考勤打卡以 L1445 为准、私人手机只**查看**考勤」的现行口径冲突（不是打卡，是查看）。
- **可复现**：`sed -n '2675p' docs/platform-governance-product-spec-v0.2.md`；`sed -n '741p' docs/platform-governance-map-for-operations-v0.1.html`
- **冻结判据**：PGM-008、BSM-114、GEN-008、GEN-016
- **2.3 依据**：类型 1
- **四分类**：**冲突**
- **工程缺陷**：是，建议入队
- **最小处置**：按 HTML 列出低风险功能闭集；继续禁止现场核销/点单/收款/履约。

### GR-M014-004 — `analytics.group.view` 仍写成「当前实现缺口」，V394 已闭合

- **文件:行号**：L761、L2721
- **原文**：`analytics.group.view 审批属性的当前实现缺口按该 PRD 登记，不反向改写业务口径。`
- **实现**：`V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql` L17 将该码 `requires_approval=TRUE`。同簇 PRD/验收包已在 M013 改为「已闭合」。
- **业务影响**：本规格把已落地审批属性写成未闭合缺口，GEN-016 假当前。
- **可复现**：`rg -n "analytics.group.view" backend/src/main/resources/db/migration/V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql docs/platform-governance-product-spec-v0.2.md`
- **冻结判据**：PGM-020、PGM-017、PGM-066、GEN-016
- **2.3 依据**：类型 4
- **四分类**：**细化**（集团视图实现状态）
- **工程缺陷**：是，建议入队
- **最小处置**：改为「V394 已置 requires_approval=true」，删除「当前实现缺口」。

### GR-M014-005 — R5 文首把准入闸与终端开关写成「未接线」的当前事实

- **文件:行号**：L7–L9
- **原文**：`SupervisorReviewService.evaluate 是孤立 HTTP 评估接口，票务/酒吧/游乐/结算等业务域在动作前零调用它…终端开关后端拒绝：…后端业务接口未独立校验开关。`
- **实现**：
  - `ActionGateAspect.gate`：终端开关 → `AdmissionGuard.check` → 权限（`backend/src/main/java/com/complex/platform/gate/ActionGateAspect.java:40-50`）
  - `AdmissionGuard.blockToReview` 调 `supervisorReviewService.blockForAdmission`
  - `StaffApprovalService.submitShifts/submitPenaltyCap` 调 `reviews.evaluate`（员工域已接线，M013-007 背景）
- **业务影响**：规格开篇把已接线闸口写成未实现，读者会按「P0 缺口」排期或反向改 HTML。
- **可复现**：`rg -n "terminalSwitchGuard.check|admissionGuard.check|reviews.evaluate" backend/src/main/java/com/complex/platform/gate/ActionGateAspect.java backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java`
- **冻结判据**：PGM-007、PGM-010、GEN-016
- **2.3 依据**：类型 4
- **四分类**：**细化**（合同准入/终端开关的实现状态）
- **工程缺陷**：是，建议入队
- **最小处置**：删除或改写 R5「当前未接线」句；准入落点以 `AdmissionGuard`+`supervisor_review_decisions` 为准；evaluate 孤立 HTTP 不再当现行。

无业务发明：不要求改 HTML 闸口规则，只要求规格停止把 2026-06-18 过期核验写成当前。

### GR-M014-006 — 名词脚注把非 HTML、仍含已删父链的模型文抬成「权威实体关系」

- **文件:行号**：L48
- **原文**：`权威实体关系以 merchant-space-contract-model-v0.1.md 为准。`
- **被引文**：该文 L64「当前字段：…`parent_entity_id`…」；L66 仍把 V318 父链当「当前代码结构」。V410 已删列。
- **HTML**：BEQR-144 / PGM-018 明文多对多、非隶属链。规格 §0A L24：后文冲突以本节和三份 HTML 为准。
- **业务影响**：本规格把 HTML 最终解释权让给一份仍写已删列的设计文，首次阅读就会把父链当现行。
- **可复现**：`sed -n '48p' docs/platform-governance-product-spec-v0.2.md`；`sed -n '64,66p' docs/merchant-space-contract-model-v0.1.md`；`rg "DROP COLUMN IF EXISTS parent_entity_id" backend/src/main/resources/db/migration/V410*.sql`
- **冻结判据**：BEQR-144、PGM-018、GEN-016、GEN-017
- **2.3 依据**：类型 1 + 4
- **四分类**：**冲突**（把非 HTML 抬成权威，且与 HTML 多对多/已删父链相反）
- **工程缺陷**：是，建议入队
- **最小处置**：删除「以该文为准」；改为三份 HTML + 本规格 §0A；父链不得再经引用回灌。

### GR-M014-007 — 合同状态在同文件「不定义迁移边」口径下仍画出完整 stateDiagram

- **文件:行号**：L1553–L1566（F3 §5.6）
- **原文**：`stateDiagram-v2` 含 `[*] --> 草拟`、`待确认 --> 草拟`、`已生效 --> 续签中` 等边。
- **对照**：同文件 F3 §5.5、F7 §9.5、F10 §12.5、项目 §5.5 均写「ENUM 只给状态成员…不定义迁移边」「【已废止】自造迁移流程」。BEQR-156 只给八个成员 + 当地时间，不给边。
- **业务影响**：合同独享一张未获 HTML 授权的迁移图，与本规格对其它 ENUM 的收口相反。
- **可复现**：`sed -n '1545,1566p' docs/platform-governance-product-spec-v0.2.md`
- **冻结判据**：BEQR-156、GEN-008、GEN-017
- **2.3 依据**：类型 1
- **四分类**：**细化**（BEQR-156 成员的流程转译越权成边；能指到合同状态锚点，故非添加）
- **工程缺陷**：是，建议入队
- **最小处置**：删除 stateDiagram 或改为「只展示八个成员、不授权边」，与商户/员工/事件口径一致。

### GR-M014-008 — §24.1 标题路径 `/me/effective-permissions` 在现网契约不存在

- **文件:行号**：L2748
- **原文**：`### 24.1 有效权限集接口 GET /me/effective-permissions`
- **正文/实现**：L2758 `GET /api/v1/access/effective-permissions`；OpenAPI L8098 同路径；无 `/me/` 操作。
- **业务影响**：按标题对接 404；与 GEN-002/016 路径可复现要求相反。
- **可复现**：`rg -n "effective-permissions" docs/openapi/openapi-p0.yaml docs/platform-governance-product-spec-v0.2.md`；`npm run verify:openapi` exit 0（不捕获此标题）
- **冻结判据**：BEQR-151、GEN-002、GEN-016
- **2.3 依据**：类型 4
- **四分类**：**技术性**（接口路径命名；HTML 无此路径。无业务发明：不新增权限算法，只纠正标题）
- **无业务发明理由**：路径是访问层标识，不引入 HTML 没有的权限源或状态。
- **工程缺陷**：是，建议入队
- **最小处置**：标题改为与正文/OpenAPI 相同的 `/api/v1/access/effective-permissions`。

### GR-M014-009 — 字段字典仍把「业态编号」六码列为 P0 必填，补录已废止且 HTML 未批独立编号字段

- **文件:行号**：L923
- **原文**：`业态编号 | … | 必填 | 选择 | bar / catering / retail / amusement / media / other`
- **对照**：补录 L3287【已废止】「系统生成或必填且唯一的业态编号。ENUM 商户业态只有六值名称」；ENUM `:195` 六值名称，仅「演出传媒」标注枚举码 `media`，未批独立必填编号字段。
- **业务影响**：正文强制一套 HTML 未批的编号字段；与后文废止裁定并存。
- **可复现**：`sed -n '923p;3287p' docs/platform-governance-product-spec-v0.2.md`；`sed -n '195p' docs/business-enum-quick-reference-v0.1.html`
- **冻结判据**：BSM-072、GEN-008、GEN-016
- **2.3 依据**：类型 1
- **四分类**：**细化**（六值的技术码；能指到商户业态锚点）
- **工程缺陷**：是，建议入队
- **最小处置**：正文跟随补录废止「业态编号必填」；页面只展示六值名称；`media` 可作为演出传媒的系统码备注，不得立法另外五码为必填编号。

### GR-M014-010 — 同一十一端的独立经销商门户产品码在正文/补录不一致

- **文件:行号**：L865 vs L3245
- **原文**：正文 `dealer_portal(独立经销商 Web 门户)`；补录 `distributor_web(独立经销商 Web 门户)`
- **HTML**：BEQR-150 只给中文名「独立经销商 Web 门户」，无这两码。
- **业务影响**：终端开关按码对接时两套编码；GEN-008。
- **可复现**：`rg -n "dealer_portal|distributor_web" docs/platform-governance-product-spec-v0.2.md`
- **冻结判据**：BEQR-150、PGM-010、GEN-008
- **2.3 依据**：类型 4
- **四分类**：**技术性**（产品细化编码；HTML 无码。无业务发明：不增减第十一端）
- **无业务发明理由**：两端都映射同一 HTML 中文名，只是内部码分裂。
- **工程缺陷**：是，建议入队
- **最小处置**：全文统一一个技术码，并声明「最终解释权仍为 HTML 中文名」。

### GR-M014-011 — 终端开关低保真列表仍把已废止的生效/失效时间当列表列

- **文件:行号**：L2634（对照 L626–L627、L870–L872 已废止定时启停）
- **原文**：`开关列表 | 终端类型(十一端)、开关范围、业务系统、模块、是否启用、生效时间、失效时间`
- **业务影响**：低保真把技术时刻画回主列表，与「不设定时启停」收口对打。
- **可复现**：`sed -n '626,627p;2634p;870,872p' docs/platform-governance-product-spec-v0.2.md`
- **冻结判据**：PGM-010、BEQR-153、GEN-008
- **2.3 依据**：类型 1（GOV 只给七档）+ 4
- **四分类**：**细化**
- **工程缺陷**：是，建议入队
- **最小处置**：开关列表与 §14A.3 对齐，生效/失效不进主列或明确「非控制项、非定时启停」。

### GR-M014-012 — F11 写入 P0 流程总览表，关系 mermaid 未接入

- **文件:行号**：L1358 vs L1362–L1391
- **原文**：表有 F11；flowchart 无 F11 节点/边。
- **业务影响**：总览图不能证明终端开关在 P0 闭环内，与 govfam 补 F11 的目的相反。
- **可复现**：`sed -n '1346,1391p' docs/platform-governance-product-spec-v0.2.md`
- **冻结判据**：PGM-010、GEN-002、GEN-016
- **2.3 依据**：类型 4（结构解析）
- **四分类**：**技术性**（图完整性；不新增开关规则）
- **无业务发明理由**：F11 正文已存在，只是总览图漏边。
- **工程缺陷**：是，建议入队
- **最小处置**：在关系图加入 F11 及其与留痕/后续业务的边。

### GR-M014-013 — F3 正常流程 mermaid 含非法节点行，图不可渲染

- **文件:行号**：L1541
- **原文**：`【已废止/不得回灌】M -- "是" --> O["商户状态可进入正常"]——不定义商户状态迁移边`
- **业务影响**：该行不是合法 mermaid，F3 主图在渲染器中失败或显示为垃圾节点；GEN-015。
- **可复现**：`sed -n '1525,1543p' docs/platform-governance-product-spec-v0.2.md`
- **冻结判据**：GEN-015、GEN-002
- **2.3 依据**：类型 4
- **四分类**：**技术性**（图语法；废止语义已用注释表达。无业务发明）
- **无业务发明理由**：意图是废止迁移边，不是新增商户状态。
- **工程缺陷**：是，建议入队
- **最小处置**：把废止句移出 mermaid 代码块，保留合法 `M -- "是" --> P[...]`。

### GR-M014-014 — 补录仍把 `admission_audits` 写成现行落库，与 §9.6/§24.3 收口相反

- **文件:行号**：L3343
- **原文**：`该 approval_level 取值同时写入 admission_audits（合同准入审计/放行记录），与放行原因、审批人、放行证据一并留痕。`
- **对照**：L335、L2830「不新建并行 admission_audits 表，统一走 supervisor_review」；R5 亦写两表从未建。
- **业务影响**：补录未标【已废止】的句子会把从未建的表当现行接口。
- **可复现**：`rg -n "admission_audits" docs/platform-governance-product-spec-v0.2.md`
- **冻结判据**：PGM-007、GEN-008、GEN-016
- **2.3 依据**：类型 4
- **四分类**：**细化**
- **工程缺陷**：是，建议入队
- **最小处置**：该句标【已废止】或改为 `supervisor_review_decisions`。

### GR-M014-015 — 低保真一级导航把商户合同/人员设备写成独立一级，与同章后续细化相反

- **文件:行号**：L2272–L2288 vs L2710–L2712
- **原文**：§3.1 表「一级导航」列出现「商户合同」「人员设备」「权限审计」；§21「一级导航名称使用平台治理…商户合同 P0 放在平台治理下，不单独做一级导航」。
- **业务影响**：原型 IA 与已确认导航规则对打，GEN-008。
- **可复现**：`sed -n '2268,2288p;2710,2712p' docs/platform-governance-product-spec-v0.2.md`
- **冻结判据**：PGM-003、GEN-008
- **2.3 依据**：类型 4
- **四分类**：**细化**
- **工程缺陷**：是，建议入队
- **最小处置**：§3.1 一级列统一为「平台治理」，商户合同/人员设备改为二级分组。

### GR-M014-016 — 多处「修订说明」仍把已废止的 device_key_fingerprint 写成 D8 现行改写结果

- **文件:行号**：L425、L1022、L1721、L2514（及变更日志 L783、L1323、L2240、L2737 的「改为…指纹」表述）
- **原文（L425）**：`把"NFC 标识/NFC 绑定"改为"设备凭证（设备公钥/不可逆设备指纹 device_key_fingerprint）"`
- **对照**：同页 L420/L439【已废止】指纹作凭证定义；已确认规则写 Keystore 指纹「属未立法实现证据，不得升为已确认规则」。HTML：私人手机「绑定一个该手机专属的设备标识…不读取硬件识别码」，零命中 fingerprint。
- **业务影响**：读者按修订说明会把指纹当现行唯一凭证，与正文废止和 HTML 冲突。
- **可复现**：`rg -n "device_key_fingerprint" docs/platform-governance-product-spec-v0.2.md | head`
- **冻结判据**：PGM-008、GEN-008、GEN-016
- **2.3 依据**：类型 1
- **四分类**：**细化**（设备凭证实现细节；上位是 HTML「手机专属设备标识」）
- **工程缺陷**：是，建议入队
- **最小处置**：修订说明改为「废止 NFC 身份与 fingerprint 立法；现行仅手机专属设备标识」。变更日志可保留历史，但不得把指纹写成仍有效的现行定义。

## 6. 84 条冻结判据核对

说明：本文件是产品规格。无独立可执行测试面的 ID，GEN-003 记「N/A-文档」；不把未运行测试写通过。冲突/过期当前事实见对应 GR。

| ID | 锁来源 | 本文件适用证据 | 结论 |
|---|---|---|---|
| BEQR-063 | ENUM `:111` 酒吧高危 | F9 资金/核销/退款/调账须审批；未发明沽清规则 | 顺从 PASS（无实现面） |
| BEQR-074 | ENUM `:125` 隐私桌台 | 未定义桌台权限页；未发明相反三权 | 顺从 PASS（无实现面） |
| BEQR-143 | `:226` 四类主体 | 字段字典 §7 四主体+唯一绑定编号 | 顺从 PASS |
| BEQR-144 | `:227` 多对多非隶属链 | 正文无父链列；**L48 引用仍含父链的模型文** | **FAIL → GR-006** |
| BEQR-145 | `:228` 城市项目公司 | 未把资产公司写成独立主体 | 顺从 PASS |
| BEQR-146 | `:229` 运营公司职责 | 未把娱乐平台写成取得单吧门票 | 顺从 PASS |
| BEQR-147 | `:230` 收入归属 | 未发明顶层自动取收入 | 顺从 PASS |
| BEQR-148 | `:231` 合作模式六类+其他须指定模板 | 六类有；「其他」生效前指定收费模板未进字段/表单 | 观察 OBS-04（未发明相反；未写全下位） |
| BEQR-149 | `:233` 职级 M1–M10 上中下 | 多处 30 档；明确不折审批 | 顺从 PASS |
| BEQR-150 | `:233` 十一端 | §14A/F11/§3.1 十一端中文闭集正确；产品码分裂 | **部分 FAIL → GR-010** |
| BEQR-151 | `:234` 最终权限五源 | §14.4 / §24.1 五源 DENY 优先；标题路径错 | **部分 FAIL → GR-008** |
| BEQR-152 | `:235` 项目隔离+敏感数据 | 隔离无关闭开关；敏感分权 | 顺从 PASS |
| BEQR-153 | `:237` 七档开关 | F11/14A 七档；废止定时启停；17A.1 回灌时刻列 | **部分 FAIL → GR-011** |
| BEQR-154 | `:237` 项目六态 | 与 ENUM 逐字同；不定义边 | 顺从 PASS |
| BEQR-155 | `:238` 商户六态+缓冲期+试运营不管 | 六态同；缓冲期在 §14.2；试运营废止 | 顺从 PASS |
| BEQR-156 | `:239` 合同八态+当地时间 | 八态同；§0A 当地时间；**F3 自造边** | **部分 FAIL → GR-007** |
| BEQR-157 | `:240` 员工五态 | 逐字同；不定义边 | 顺从 PASS |
| BEQR-158 | `:241` 设备七态 | 逐字同 | 顺从 PASS |
| BEQR-159 | `:242` 权限五态 | 逐字同 | 顺从 PASS |
| BEQR-166 | `:255` 合同审批链 | 主线默认总经理+平行线；未写成一次确认 | 顺从 PASS |
| BEQR-189 | `:291` 管理费方式 | 本文件无服务点单管理费页 | N/A-无实现面；未发明相反 |
| BSM-004 | `:566` 存档删除审批 | 规格要求归档留痕；不写 DELETE 恒成功 | 顺从 PASS（无实现面） |
| BSM-005 | `:581` 分步新建 | 低保真表单分组，未发明一次填完强制 | 顺从 PASS |
| BSM-006 | `:584` 动态码 | 核销不在本文件；工作手机扫码与 HTML 一致 | N/A-无实现面 |
| BSM-007 | `:587` 平台管理员 vs 最高权限 | §24.1 日常/高危分授；最高权限=U 盘一人 | 顺从 PASS |
| BSM-009 | `:596` 基础管理入口 | 页面分组覆盖综合体/空间/商户/合同/员工/设备/权限 | 顺从 PASS |
| BSM-028 | `:698` 基础管理主要功能 | 六类合作、多对多、项目公司边界有承接 | 顺从 PASS（父链问题见 GR-006） |
| BSM-029 | `:714` 和谁有关 | 跳转表连票务/酒吧/游乐/财务/权限 | 顺从 PASS |
| BSM-072 | `:1308` 商户业态六值+其他主管公司 | 六值有；独立业态编号必填与补录冲突 | **部分 FAIL → GR-009** |
| BSM-074 | `:1326` 商户侧边界 | 商户看不到全局；未发明跨商户 | 顺从 PASS |
| BSM-114 | `:1827` 各类入口 | 十一端+私人手机能力；§19 收窄 | **FAIL → GR-003** |
| BSM-115 | `:1897` 子系统必须连 | 流程依赖表 + F5 卡后续业务 | 顺从 PASS |
| BSM-120 | `:1957` 已确认规则 | 私人手机设备标识、寄存柜、不建设营销打卡有承接 | 顺从 PASS |
| GEN-001 | 通用 | 文件有上位；OVERSIZE 合法 | 顺从 PASS |
| GEN-002 | 通用 | `/me/` 标题不可执行；F3 mermaid 非法；F11 图漏 | **FAIL → GR-008/012/013** |
| GEN-003 | 通用 | 文档类 N/A；不把未跑测试写通过 | N/A-文档 |
| GEN-004 | 通用 | 无本文件输入校验实现 | N/A-无实现面 |
| GEN-005 | 通用 | §24.1 指定用户须 `access.user.view`；高危须审批 | 顺从 PASS（规格层） |
| GEN-006 | 通用 | 无本文件事务实现 | N/A-无实现面 |
| GEN-007 | 通用 | §24.1 单次授权不在读接口消费 | 顺从 PASS（规格层） |
| GEN-008 | 通用 | 空间类型/导航/编码/迁移边/指纹修订说明 | **FAIL → 多条 GR** |
| GEN-009 | 通用 | 无本文件错误处理实现 | N/A-无实现面 |
| GEN-010 | 通用 | 脱敏七项；U 盘清留痕 | 顺从 PASS（规格层） |
| GEN-011 | 通用 | 留痕七态+作废动作 | 顺从 PASS |
| GEN-012 | 通用 | 无本文件资源边界实现 | N/A-无实现面 |
| GEN-013 | 通用 | 所引 OpenAPI 路径除 `/me/` 外存在；admin 路由中文名不一一对应（观察） | **部分 FAIL → GR-008** |
| GEN-014 | 通用 | 规格不是迁移；错误声称 mall/V394 缺口 | **FAIL → GR-002/004** |
| GEN-015 | 通用 | 中文主文；F3 mermaid 非法行 | **FAIL → GR-013** |
| GEN-016 | 通用 | 多处把过期实现/伪 HTML 并集写成当前 | **FAIL → GR-001/002/004/005/006/016** |
| GEN-017 | 通用 | 分类按冲突优先；无添加项伪装技术性 | 本席分类见各 GR |
| GEN-018 | 通用 | 结论落到 HTML/命令；不引用席位意见当依据 | 本报告遵守 |
| PGM-002 | `:308` 治理五步 | F5+依赖表对象→合同→人员→设备→留痕 | 顺从 PASS |
| PGM-003 | `:339` 模块入口 | 页面分组覆盖卡片主题；导航自相矛盾 | **部分 FAIL → GR-015** |
| PGM-004 | `:342` 综合体和集团 | 项目页+集团已承接句；缺口句过期 | **部分 FAIL → GR-004** |
| PGM-005 | `:347` 空间和点位 | 对象列表掺「项目」；mall 根 | **FAIL → GR-001/002** |
| PGM-006 | `:352` 商户和主体 | 四类绑定+六类合作+两联系人 | 顺从 PASS |
| PGM-007 | `:357` 合同和准入 | 先阻断再升级；R5/补录落库过期 | **部分 FAIL → GR-005/014** |
| PGM-008 | `:362` 员工和手机 | 两条路径+设备标识；指纹修订说明/私人手机功能 | **FAIL → GR-003/016** |
| PGM-009 | `:367` 权限和职级 | 最小颗粒度+模板；废止复制模板 | 顺从 PASS |
| PGM-010 | `:372` 终端开关 | 十一端+七档；R5 后端未校验过期；17A 列 | **部分 FAIL → GR-005/011** |
| PGM-011 | `:378` 设备管理 | 台账/领还/七态 | 顺从 PASS |
| PGM-012 | `:383` 商户侧 | 授权范围内全开放、不看全局 | 顺从 PASS |
| PGM-013 | `:388` 开放接口 | 未在本文件展开；未发明相反 | N/A-无实现面 |
| PGM-014 | `:393` 留痕复核 | 操作留痕页+七态 | 顺从 PASS |
| PGM-015 | `:399` 楼宇运营 | 本文件不承载楼宇页 | N/A-无实现面 |
| PGM-016 | `:404` 应急与隐私 | U 盘清留痕；脱敏 | 顺从 PASS（规格层） |
| PGM-017 | `:412` 综合体和集团 h2 | 项目归属；集团已承接 vs 缺口句 | **部分 FAIL → GR-004** |
| PGM-018 | `:415` 多对多 | 见 BEQR-144 | **FAIL → GR-006** |
| PGM-019 | `:424` 各主体职责 | 未发明顶层自动取收入 | 顺从 PASS |
| PGM-020 | `:440` 集团下钻 | 已承接句正确；审批缺口过期 | **FAIL → GR-004** |
| PGM-021 | `:452` 业务意义 | 对象带项目归属 | 顺从 PASS |
| PGM-026 | `:492` 商户主要功能 | 四类绑定+六类合作 | 顺从 PASS |
| PGM-027 | `:507` 商户边界 | 不看其他商户 | 顺从 PASS |
| PGM-031 | `:555` 外部经销商 | §0A 有承接；无独立经销页面明细 | 观察 OBS-05 |
| PGM-035 | `:599` 提成/不建设营销打卡 | 未把营销打卡写入考勤 | 顺从 PASS |
| PGM-040 | `:643` 单一功能授权 | 权限点最小颗粒度 | 顺从 PASS |
| PGM-041 | `:647` 权限规则 | 隔离无关闭开关；高危审批 | 顺从 PASS |
| PGM-042 | `:664` 职级规则 | 薪资大范围；不替代权限点 | 顺从 PASS |
| PGM-043 | `:673` 外包与审计 | 本文件未展开外包页 | N/A-无实现面 |
| PGM-044 | `:688` 桌台/挂单/经销权限 | 本文件未定义桌台权限矩阵 | N/A-无实现面 |
| PGM-048 | `:749` 商户侧全开放 | 与 PGM-012 同 | 顺从 PASS |
| PGM-060 | `:879` 会员营销生物 | 本文件非会员规格 | N/A-无实现面 |
| PGM-065 | `:935` 连接关系 | 依赖表与验收「后续支撑」 | 顺从 PASS |
| PGM-066 | `:951` 已拍板 | 集团视图已承接；权限自定义；日结补录未在本文件展开 | 部分见 GR-004 |

84/84 已核对。有 FAIL 的 ID 不构成「有条件通过」。

## 7. 命令证据

```
$ git rev-parse HEAD ; git rev-parse origin/main
685dca4e1088ed59e68196c3c5b45787fde9db02
685dca4e1088ed59e68196c3c5b45787fde9db02
exit 0

$ git status --porcelain
 M docs/audit-2026-09/ledger.md
exit 0

$ python3 -c "..."  # 规格字节/行/sha256
bytes=221816 sha256=b3ee635aa1763d1f5b1e6e7c7e381fe78bc71057c162caad39ae8cd3b8bbcd31 lines=3494
exit 0

$ sha256sum docs/audit-2026-09/plan/criteria-lock.md
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
exit 0

$ npm run verify:openapi
OpenAPI contract verification passed.
exit 0

$ npm run verify:entry-anchors
[entry-anchors] PASS：…
exit 0

$ npm run verify:html-ledger-sync
[html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。
exit 0
```

未运行后端测试、未 migrate、未清理共享库。`verify:openapi` 只证明契约自洽。

## 8. 观察（不升 finding，建议队列否）

- **OBS-01**：OpenAPI `BusinessEntityRequest.entity_role` 仍为旧六值（M013-009 / M392）。本规格未复述该六值，不重开。
- **OBS-02**：`ApprovalLevel`/`SupervisorReviewService` 仍含 `specific_role` 与 level_rank 算术；§24.3.6 对此作实现差距登记，与「算术不得写入产品规格」一致，不把 2026-06-19 核验句当新根因。
- **OBS-03**：2026-08-28 补录只有页面/流程/字段三源，无低保真来源段；补录头已声明未写入 HTML 者不生效。
- **OBS-04**：BEQR-148「其他」须指定收费模板——规格六类有、模板套用未进字段表；未写相反规则。
- **OBS-05**：PGM-031 外部经销商——§0A 有跨篇承接，页面清单无独立经销治理页；HTML 沉默于「必须是 P0 治理页」时不升冲突。
- **OBS-06**：P0 页中文名与 `adminRoutes.ts` 工作台名不一一对应（规格 L43 声明不写接口/代码）。GEN-002 不对产品规格强制现网路由同名。
- **OBS-07**：`ADM-GOV-005` `/admin/governance/admission-audits` 仍聚合三列表；§9.6 已诚实收口，不把该实现页当本规格新根因。
- **OBS-08**：第一联系人=主要联系人双字段，HTML 有第一/第二；属同义展示，不升。
- **OBS-09**：变更日志中 2026-06-10「改为指纹」是历史记录；现行定义问题归 GR-016，不另开日志伪造 finding。
- **OBS-10**：本规格无 `is_self_operated` / 私人手机打卡 / 定时启停立法（17A.1 列表问题已单列）。
- **OBS-11**：三 HTML 无需人闸修改。
- **OBS-12**：controller 极端路径（要两三件不巧）未新增闸/状态/文件。

## 9. ponytail-review（只列可删，不落地）

- L7–L9: delete: 过期 R5「未接线」当前事实。用 ActionGate 现状句替换。
- L48: delete: 「以 merchant-space-contract-model 为准」。HTML 已够。
- L896「项目」作为空间类型: delete。
- L1291 `mall` 根: delete。
- L1553–1566 合同 stateDiagram: delete。
- L2634 生效/失效列表列: delete。
- L2675 私人手机能力收窄句: shrink 为 HTML 低风险列表。
- L761/L2721「当前实现缺口」: delete。
- L3343 admission_audits 现行句: delete 或标废止。
- net: 规格变短且不再把过期实现当现行。

## 10. 审计结论

**FAIL**

原因（须同时成立才 PASS，本席不满足）：映射 84 条已核对且全文件有覆盖，但存在未定稿工程缺陷 GR-M014-001…016；无未答 questions；无添加项；哈希行有效。不存在有条件通过。

建议 B2：冲突项（001/003/006）入修复队列「是」；有工程缺陷的细化/技术性（其余 GR）入队列「是」；观察否。本席不修复、不分配正式 ID。

## M014 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 53179 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M014 FILES 1 / 判据 84 / 计划冻结 3494 行 OVERSIZE-SINGLE-FILE）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `685dca4e1088ed59e68196c3c5b45787fde9db02`（PR #434 M013 闭环检查点已合并）。本模块 1 文件指纹与 B1 覆盖表全等。B3=0。无源修复。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类。
- 本席 B2-r1：**FAIL**（001 最小处置未收全同根活句；009 把五码写成「业态代码值」字段，改写本席 GR-009 原意。其余 002–008、010–020 逐 ID PASS。添加 0、不发 2.2）
- 正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 与 ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要复现只读。

此票只判断 findings.md V1.87 顶部「当前 M014 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M014-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 53179 | `ebadd23d29e84e2553381b28aa75b732302e26a717df04df92e2b36a3990e584` |
| `docs/audit-2026-09/modules/M014-b1-self-raw.md` | V0.01 | 29222 | `08a613b24236b34d9c2013e99d683d950b6bc6904839080f098d8c48783d6881` |
| `docs/audit-2026-09/findings.md` | V1.87 | 747935 | `a43263fa2753d713b2d527e09bf4c5ab1d2243c5615221caf9649a5a02cf0b8d` |

ledger 只读：V3.23，582492 bytes，SHA256 `552832ae3cbd02de4795349445dbad426808eb0dd7c369243ec969cea50bd72a`；M014 IN_PROGRESS、B3=0。映射行 `M014 → [BEQR-063 … PGM-066]`（`criteria-lock.md` L6393）COUNT=84 unique=84 MISSING=[] EXTRA=[]。1 文件 `git ls-files -z -- docs/platform-governance-product-spec-v0.2.md | xargs -0 cat | wc -l` = **3494**。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

三 HTML 只读核验（本席未改；与 B1 同）：

| 文件 | 现行 | SHA256 |
|---|---|---|
| 业务地图 | V0.14 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| 治理地图 | V0.12 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| 枚举速查 | V0.07 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

本模块 1 文件 SHA 与 B1 覆盖表全等：`docs/platform-governance-product-spec-v0.2.md` 3494 行 / 221816 bytes / `b3ee635aa1763d1f5b1e6e7c7e381fe78bc71057c162caad39ae8cd3b8bbcd31`。

工作区只读：`git status --porcelain` 见 findings.md / ledger.md 已改（席甲落候选）、本两份 raw 未跟踪。本席只追加本指定路径。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核源/HTML/命令再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：001 是否漏同根活句、005 加行是否等于改写、009 只读行是否立法五码、007 删图是否丢审批链、017/018 是否技术性「是」、020 是否写错观察 |
| verification-before-completion | 声称前先跑只读哈希/行号/源码核对 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 删除测试：本规格深度落在 HTML 四类绑定/十一端/状态成员，不落已废止闭集或过期「当前实现」 |
| ponytail-review | 只评最小删除/收窄，不落地。001 不删「项目根」=最懒；009 废止行比新建「业态代码值」更懒；007 删 stateDiagram=最懒；013 `%%` 比移出块更懒 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |

### 完整差集（GR-001…016、OBS-01…12、席甲 CX-001…007、OBS-1…5）

漏并：无。两席 B1 全部临时 ID 仍落在 001–020。本席 GR-001…016 原意：001/009 的候选最小处置改写了原意（见下），分类/队列未改写。席甲独有 CX-006/007 本席 B1 未登记，本票独立核源后归 017/018，不是照抄席甲分类。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M014-001 | GR-M014-001（席甲 B1 未发现） | 冲突/是/是 | **FAIL（冲突/是/是成立；最小处置不足：L892/L900 同根活句未收）** |
| M014-002 | GR-M014-002 + CX-M014-002 | 细化/是/是 | **PASS（细化/是/是；与 M013-004 同构）** |
| M014-003 | GR-M014-003（席甲 B1 未发现） | 冲突/是/是 | **PASS（冲突/是/是；合取 PGM L741+BSM L930，含看排班/查看考勤）** |
| M014-004 | GR-M014-004 + CX-M014-001 | 细化/是/是 | **PASS（细化/是/是）** |
| M014-005 | GR-M014-005 + 席甲 OBS-1 | 细化/是/是 | **PASS（细化/是/是；加一行满足「改写」支，最懒）** |
| M014-006 | GR-M014-006（席甲 B1 未发现） | 冲突/是/是 | **PASS（冲突/是/是；L48 收口到三 HTML+§0A）** |
| M014-007 | GR-M014-007（席甲 B1 未发现） | 细化/是/是 | **PASS（细化/是/是；删图最懒，不丢 BEQR-166）** |
| M014-008 | GR-M014-008（席甲 B1 未发现） | 技术性/是/是 | **PASS（技术性/是/是；无业务发明）** |
| M014-009 | GR-M014-009 + CX-M014-004 | 细化/是/是 | **FAIL（细化/是/是成立；最小处置把五码立法为「业态代码值」字段，改写 GR-009）** |
| M014-010 | GR-M014-010 + CX-M014-003 | 技术性/是/是 | **PASS（技术性/是/是；统一 dealer_portal + 尚未实现）** |
| M014-011 | GR-M014-011 + CX-M014-005 | 细化/是/是 | **PASS（细化/是/是；三处未漏）** |
| M014-012 | GR-M014-012（席甲 B1 未发现） | 技术性/是/是 | **PASS（技术性/是/是；无业务发明）** |
| M014-013 | GR-M014-013（席甲 B1 未发现） | 技术性/是/是 | **PASS（技术性/是/是；%% 最懒）** |
| M014-014 | GR-M014-014（席甲 B1 未发现） | 细化/是/是 | **PASS（细化/是/是）** |
| M014-015 | GR-M014-015（席甲 B1 未发现） | 细化/是/是 | **PASS（细化/是/是）** |
| M014-016 | GR-M014-016 + 席甲 OBS-5 | 细化/是/是 | **PASS（细化/是/是；升席甲观察成立）** |
| M014-017 | CX-M014-006（本席 B1 未登记） | 技术性/是/是 | **PASS（技术性/是/是；无业务发明）** |
| M014-018 | CX-M014-007（本席 B1 未登记） | 技术性/是/是 | **PASS（技术性/是/是；无业务发明）** |
| M014-019 | GR-M014-006 引文侧拆出 | 技术性（跨模块）/是/否（XM→M261） | **PASS（否合规；未写错 B1）** |
| M014-020 | 双方观察 | 技术性/正向 /否/否 | **PASS（否；未把应升项留观察）** |

冲突 3（001、003、006）。细化入队 9（002、004、005、007、009、011、014、015、016）。技术性入队 6（008、010、012、013、017、018）。技术性跨模块否 1（019）。观察否 1（020）。添加 0。

### 001 独立裁定（冲突／是；最小处置不足 → 本 ID FAIL）

核源（只读）：

- PGM V0.12 L469：「维护楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位。」无「项目」作为空间类型。
- 规格 L896 伪称「三份业务 HTML 并集收敛为项目、楼层…」；L903 空间类型选项含「项目」；L187/L2392 空间树首项「项目」。
- V184 八码无 project/mall；`governance/PlatformService.java:476-478` `createSpace` 硬校验 `space_type_dictionary` active 码。
- L56/L174/L2276「项目根」是空间树容器措辞，不是 space_type 选项。L902「必填，项目顶层除外」同属归属根。
- 同根现行活句候选未列：L892 节标题「空间对象字段（项目、楼层…）」；L900「空间名称 | 识别项目、楼层…」。变更日志 L781/L1315/L2736 为历史行。

四分类：**冲突**（与 GR-001 原意相同）。上位=PGM-005 九类；伪称 HTML 并集含项目，冲突优先于细化。工程缺陷是。无业务发明。

不删 L56/L174/L2276「项目根」：**足够且最懒**。项目是 `projects` 表归属根，空间树从项目挂下去，不是空间类型。删「项目根」会丢掉「空间属于项目」的页面目的句，比标注更重，且超出 GR-001「不得把项目实体写成 space_type」。

只改 L896/L903 + 标注 L187/L2392：**不足**。§5 开篇 L892/L900 仍把项目写进空间对象闭集；首次读字段字典会按十类（含项目）建模。与 M013-002「同根现行活句须收全」同一标准。候选方向（类型去掉项目、树列表标归属根）对，覆盖不够。

票：**FAIL**。r2 最小处置须补 L892 节标题与 L900 空间名称（同样标「归属根，非空间类型」或移出空间对象闭集）；L56/L174/L2276/L902 保留；变更日志可不改。不改 HTML。

### 002 独立裁定（细化／是；与 M013-004 同构）

核源：L1291 括注「技术根节点为 mall」；V184 种子 building/floor/zone/shop/public_area/parking/booth/other，无 mall；`createSpace:476-478` 活校验字典。OpenAPI 仍可有 mall 快照（M013-004 已登记非活码）。

四分类：**细化**（PGM-005 实现转译；席甲 CX-002 标技术性，不采用）。最小处置「字典活校验 + mall 仅契约快照 + 字典码与 HTML 九类差登记为可维护」与 M013-004「活校验＝字典；OpenAPI/本表只是契约快照，不得当活闭集」同构。不改 yaml/V184。无业务发明。

票：**PASS**。

### 003 独立裁定（冲突／是；合取对齐，非单行逐字）

核源：

- 规格 L2675：「只适合注册、设备凭证绑定、通知和状态查看」
- PGM V0.12 L741：「员工自有手机只开放低风险功能（公告、我的工单、帮客人订位、查看考勤、请假、查自己薪资等）」——无「看排班」二字，有「等」
- BSM V0.14 L930：注册、看公告、查自己的工单、帮客人订位、**看排班**、**查看考勤**、请假、查薪资；现场＝代客下单、出示公司收款码、完成履约、扫码核销放行
- BSM L1865：注册、提交资料审批、收公告通知、查自己的工单、帮客人订位、看排班、查看考勤、请假、查薪资等
- BSM L1445 仍为工作手机打卡（本句不是私人手机能力）

两源本就不是同一句，无法对单行逐字。候选正列表合取了 L741 六项 + L930「看排班」，并保留「后台按设备类型放权」（L741 原句）。这是 GR-003「按 HTML 列出低风险功能闭集」的正确合取，含看排班、查看考勤。继续禁止现场操作，不发明打卡进私人手机。

非阻断（不否决）：禁止列表用「核销、点单、放行、结算、补录」不是 L930 逐字；B3 对齐 BSM 现场句（代客下单、收款码、履约、扫码核销放行）。「补录」是工作手机事项（BSM L1860），写入私人手机禁止现场操作不发明新规则，但不得单独立法新禁项。

票：**PASS**。

### 004 独立裁定（细化／是）

核源：L761、L2721「当前实现缺口」；V394:17 已 `analytics.group.view` `requires_approval=TRUE`；M013-002 已改集团 PRD/验收包/PRD。两处改「已由 V394 置 true」与 GR-004 同旨。无业务发明。

票：**PASS**。

### 005 独立裁定（细化／是；加一行满足 GR-005「改写」支）

核源：R5 L6–L11 日期 2026-06-18「只读审计后回填」。L8 现在时写 evaluate 孤立、业务域零调用、闸口不触发；L9 写后端未独立校验开关。实现：`ActionGateAspect.gate:40-50` 串 `terminalSwitchGuard.check` → `admissionGuard.check` → `requirePermission`；`AdmissionGuard.blockToReview` → `SupervisorReviewService.blockForAdmission`；`StaffApprovalService:44,61` 两处 `reviews.evaluate`。

GR-005 最小：「删除**或改写** R5『当前未接线』句」。候选走「改写」支：不删历史行，在块末加 2026-09-11 现状并声明「余下内容为历史记录」。这是最懒可行——R5 本就是日期化审计注记，删三句会丢掉 2026-06-18 证据；加一行把整块认识论改成「历史+现状」，evaluate 孤立不再当现行。满足「改写」，未走「删除」。

非阻断：新行必须紧接 L9、写明 L7–L9 两条现状句已过期、不得当现行。不得把该行写到块外。

票：**PASS**。

### 006 / 019 独立裁定（006 冲突／是；019 跨模块否合规）

核源：L48「权威实体关系以 merchant-space-contract-model-v0.1.md 为准」；该文 L64–66 仍列 `parent_entity_id` 为当前字段、V318 父链为当前代码结构；V410:9 已 `DROP COLUMN IF EXISTS parent_entity_id`。规格 §0A L24 已写后文冲突以本节和三份 HTML 为准。`plan/modules.md` M261 FILES 含 `docs/merchant-space-contract-model-v0.1.md`（合同履约文档段）。本模块 FILES 仅规格 1 文件。

006：删除「以该文为准」、改三 HTML+§0A，与 GR-006 原意相同。括注「仅作实现参考、父链已删、见 019」防止再经引用回灌，不是把该文抬回权威。冲突成立（非 HTML 抬成裁决层且与 BEQR-144/PGM-018 多对多相反）。

019：引文侧拆出 XM→M261，与 M013-001/009 把 OpenAPI 半边拆到 M392 同构。缺陷在该文成立；本模块 B3 不改该文。不是把观察升成跨模块项。

票：006 **PASS**；019 **PASS（否）**。

### 007 独立裁定（细化／是；删图最懒，不丢审批链）

核源：L1553–1566 `stateDiagram-v2` 画出 `[*]→草拟`、`待确认→草拟`、`已生效→续签中` 等边。同文件 F3 §5.5（L1545）「不定义商户状态迁移边」；BEQR-156 L239 只给八成员 + 当地时间，不给边。BEQR-166 合同审批链已在 §9.4（L317–318 主线默认总经理 + 三条平行线）和 F3 正常流程（L1533–1537）承接，**不在**这张状态迁移图里。

删除 stateDiagram、改一句「八个成员、不定义迁移边」＝GR-007 允许的「删除」支，与商户/员工/事件口径一致。不会丢失 HTML 已批审批链。改成「只展示成员、不授权边」不是更懒。无业务发明。

票：**PASS**。

### 008 独立裁定（技术性／是）

核源：L2748 标题 `/me/effective-permissions`；L2758 与 OpenAPI `:8098` 为 `/api/v1/access/effective-permissions`。无 `/me/` 操作。路径标识，不新增权限源。标题改为正文路径。无业务发明。

票：**PASS**。

### 009 独立裁定（细化／是；最小处置立法五码 → 本 ID FAIL）

核源：L923「业态编号 | 必填 | 选择 | bar/catering/retail/amusement/media/other」；补录 L3287 已废止独立编号字段；ENUM L195 六值中文，仅「演出传媒」标注枚举码 `media`；L921 业态名称行已是六值中文必填选择；`PlatformService`/`V384` 六码是实现标识。

GR-009 原意：「正文跟随补录废止『业态编号必填』；页面只展示六值名称；`media` 可作为演出传媒的系统码备注，**不得立法另外五码为必填编号**。」

候选把该行改名为「业态代码值 | 实现映射 | 只读展示 | 系统记录 | bar/…/other」。必填是去掉了，但字段字典仍多出一条 P0 行，校验口径闭集仍是五码+media。只读不等于未立法——P0 字段行就是字段。席甲 CX-004 原处置（保留六码对照）改写了本席原意，本票不采用。

与 002/M013-004 的差别：mall 是既有契约快照上的导语收口，不新增字段名；此处是把已废止「业态编号」替换成新字段名「业态代码值」。最懒＝该行跟随补录标【已废止/不得回灌】/不采用，业态名称行不动；media 只括注在「演出传媒」。

票：**FAIL**。

### 010 独立裁定（技术性／是；与「全文统一一个技术码」一致）

核源：L865 `dealer_portal` vs 补录 L3245 `distributor_web`；V111 `chk_tcs_terminal_type` 仅十值+`*`，无第十一端码；HTML BEQR-150 只给中文名「独立经销商 Web 门户」。

GR-010：「全文统一一个技术码，并声明最终解释权仍为 HTML 中文名。」候选统一为正文已用的 `dealer_portal`，并标注 V111/OpenAPI 尚无该值。与「一个技术码」一致，且不把未实现码写成活 CHECK。不改 OpenAPI/迁移。无业务发明（不增减第十一端）。

票：**PASS**。

### 011 独立裁定（细化／是；三处未漏）

核源：L2634 列表含生效/失效时间列（对照 L2635/L627 已标技术字段）；L2643「生效时间不能晚于失效时间」与 L2146 已废止同一校验；L626 开关列表含未定义「状态」（§14A.5 L643 终端开关状态不定义）。席甲补的 L2643/L626 与 GR-011 的 L2634 同根，合入 011 正确。L636「设置生效/失效时刻（技术字段）」已收口，不属漏。补录 L3477 已标废止。

非阻断：L626 现行已是「是否启用、状态」——应**删除「状态」**，不要改成第二个「是否启用」；L2643 只废止前半句「不能晚于」，保留「不设定时启用/定时停用」。

票：**PASS**。

### 012 独立裁定（技术性／是）

核源：总览表 L1358 有 F11；关系 mermaid L1362–1391 无 F11 节点/边。补边不新增开关规则。无业务发明。

票：**PASS**。

### 013 独立裁定（技术性／是；%% 最懒）

核源：L1541 在 mermaid 块内，非法语句；L1542 合法 `M -- "是" --> P[...]`。GR-013 允许「移出代码块」；候选改为 `%%` 注释，图可渲染、废止语义仍在源里。比移出更懒。无业务发明。

票：**PASS**。

### 014 独立裁定（细化／是）

核源：补录 L3343 未标废止的 `admission_audits` 现行写入句；L335/L2830 已收口 `supervisor_review_decisions`，该表从未建。其余 `admission_audits` 命中已是「从未建」或作废。最小：该行加【已废止】并注统一走 supervisor_review。无业务发明。

票：**PASS**。

### 015 独立裁定（细化／是）

核源：低保真 L2272–L2288 一级列出现「商户合同」「人员设备」「权限审计」；L2710–L2712 已确认一级只用「平台治理」。表头改为分组、行内一级列改分组名，与 §21 对齐。无业务发明。

票：**PASS**。

### 016 独立裁定（细化／是）

核源：L425/L1022/L1721/L2514 修订说明仍写「改为…device_key_fingerprint」作 D8 现行改写结果；同节 L420/L439/L1038 已【已废止】指纹作凭证定义；HTML 只写「手机专属的设备标识，不读取硬件识别码」。席甲 OBS-5 认为已废止标注足够；首次读修订说明仍会把指纹当现行唯一凭证。升队成立。四处修订说明改写、变更日志历史行不改，与 GR-016 一致。补录 L2929/L3345 受补录头「未写入 HTML 者不生效」约束，不另开 ID。

票：**PASS**。

### 017 独立裁定（技术性／是；席甲独有，核源成立）

本席 B1 未登记。核源：L955「按 `merchant_credentials` 扩展或通用到期事项台账」；迁移与 `src/main/java` **0** 处 `merchant_credentials`；V028 建 `compliance_expiry_items`/`compliance_reminders`/`contract_key_dates`。其它文档（foundation-database-design 等）仍写该表名，不在本模块 FILES，本 ID 只改本规格这一句。

四分类技术性：表名/落库对象，HTML 证照到期能力不变。无业务发明。首次读字段字典会去找不存在的表，GEN-016/008，队列「是」。最小：改为 V028 合规到期台账。不发明新证照规则。

票：**PASS**。

### 018 独立裁定（技术性／是；席甲独有，核源成立）

本席 B1 未登记。核源：L335「另见 §2999、§24.3」；全文无 §2999 节；L2999 是补录历史行「复杂审批页进入 P1」，对不上。悬空内部引用，GEN-016。无业务发明。最小：删「§2999、」或改为补录段历史行。删三字更懒，候选「另见 §24.3 与补录段合同管理页历史行」可接受。

票：**PASS**。

### 020 独立裁定（观察／否；未写错应升项）

候选未把本席应入队项留在观察：GR-001…016 均在 001–016；席甲 OBS-1 已升 005、OBS-5 已升 016。OBS-01（OpenAPI 旧六值，M013-009/M392，本规格未复述）仍应留否、不重开——来源格写了 OBS-01…12，事实栏未点名，不构成把观察写成缺陷。

非阻断（不否决）：事实栏写入「V217 薪资税档权限码非第 5 类（M013-006 口径）」**不是**本席 M014 B1 观察（本席 OBS 无此条）。属邻模块口径误抄入。r2 事实栏删掉或标「非本模块 B1」。OBS-02/席甲 OBS-2（specific_role 实现缺口归 review 模块）登记正确。席甲 OBS-3 无总变更日志留否正确。

票：**PASS（否）**。

### 分类 / 队列 / 允许的最小修复边界

本批**不得定稿**（本席 FAIL）。r2 须改 001、009 最小处置后再送 B2。下列边界供 r2 候选使用；未经双 PASS 禁止 B3。一次 B3 将只改本模块 1 文件，不改三 HTML、OpenAPI、迁移、Java、锁、台账、M261 文件。文档类按类型 4 最小命令举证，不强制造测试。

- 001（须改候选）：L896 去掉「项目」作为 HTML 并集空间类型；L903 选项去掉「项目」；L187/L2392 标「项目（归属根，非空间类型）」；**补** L892 节标题、L900 空间名称同样标注或移出空间对象闭集；**保留** L56/L174/L2276「项目根」与 L902「项目顶层除外」；变更日志可不改。
- 002：L1291 括注改字典活校验；mall 仅契约快照；字典与 HTML 九类差登记为可维护；不改 yaml/V184。
- 003：L2675 合取 PGM L741 + BSM L930 低风险闭集（含看排班、查看考勤）；禁止现场项对齐 BSM L930；不写私人手机打卡。
- 004：L761/L2721 改 V394 已闭合。
- 005：R5 块末紧接 L9 加 2026-09-11 现状行，声明 L7–L9 为历史；不删历史行。
- 006：L48 改三 HTML+§0A；该文仅实现参考、父链已删见 019。
- 007：删除合同 stateDiagram，改一句八成员不定义边；§9.4/F3 审批链不动。
- 008：标题改 `/api/v1/access/effective-permissions`。
- 009（须改候选）：L923 **跟随补录废止**，不改名为「业态代码值」；业态名称行保留六值中文；media 只括注演出传媒。
- 010：全文统一 `dealer_portal`，标注尚未实现，解释权仍 HTML 中文名；补录 L3245 订正括注。
- 011：L2634 列表对齐 §14A.3；L2643 只废止「不能晚于」；L626 **删「状态」列**（不要重复「是否启用」）。
- 012：关系图加 F11 节点与边。
- 013：L1541 改 `%%` 注释，保留 L1542。
- 014：L3343 标【已废止】并注 supervisor_review_decisions。
- 015：§3.1 一级列收口到「平台治理」下的分组。
- 016：四处修订说明改「手机专属设备标识；指纹未立法」；变更日志不改。
- 017：L955 改为 V028 合规到期台账。
- 018：L335 去掉悬空 §2999。
- 019/020：本模块 B3 不修。

禁止：把项目写成 space_type；把 mall 当活码；私人手机列表保留「只通知和状态查看」而不列 HTML 闭集；把 R5 现在时当 2026-09 现状；把 merchant-space-contract-model 当权威；合同状态自造边；业态五码立法为字段；本模块改 yaml/HTML/M261 文件。

改动文件自身版本递增。

### 结论

全批差集无漏并。001 不删「项目根」正确且最懒，但 L892/L900 同根活句未收全 → **FAIL**。002 与 M013-004 同构。003 合取 HTML 闭集含看排班/查看考勤。005 加一行满足 GR-005「改写」支。006/019 拆出 XM→M261 合规。007 删图不丢 BEQR-166。009 只读映射行仍立法五码，改写 GR-009 → **FAIL**。010 统一 dealer_portal 与「一个技术码」一致。011 三处未漏。013 `%%` 最懒。017/018 席甲独有项核源成立为技术性「是」。020 未把应升项留观察（V217 误抄入为非阻断）。逐 ID：001 FAIL、009 FAIL，其余 PASS。本席 B2-r1 **FAIL**。未经双 PASS 不得把候选当定稿，更不称模块 PASS。步骤仍 B2；M014 IN_PROGRESS；B3=0。

## M014 B2-r2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.03（本段追加；上方 V0.01 B1 前缀 53179 字节 + V0.02 B2-r1 追加，全文 78928 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r2 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M014 FILES 1 / 判据 84 / 计划冻结 3494 行 OVERSIZE-SINGLE-FILE）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `685dca4e1088ed59e68196c3c5b45787fde9db02`（PR #434 已合并）。本模块 1 文件指纹与 B1 覆盖表全等。B3=0。无源修复。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲 B2-r1 PASS，只核 r2 候选是否闭合本席 B2-r1 两条否决与四条非阻断。
- 本席 B2-r2：**PASS**（001–020 逐 ID 均 PASS；r1 否决点 001/009 已闭合；003/005/011/020 非阻断已按原意并入 B3 边界）
- 本追加段不写独占锁哈希字段行。正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要复现只读。

此票只判断 findings.md V1.88 顶部「当前 M014 B2-r2 修订候选」相对本次两份完整 B1 raw（含各自 B2-r1 追加段）与其下「当前 M014 B2-r1 整批候选」原表，是否落实本席 B2-r1（V0.02）否决与非阻断，有无新的改写或遗漏。必须另有席甲本批 B2-r2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL**、V0.02 B2-r1 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M014-b1-grok-raw.md` 前缀（本席 B1+B2-r1，本段追加前） | V0.02 | 78928 | `8518cb715242c96a6362df6a7c56389d1bb67e0d137078b0c1f7d3965617a531` |
| 其中 B1 前缀 | V0.01 | 53179 | `ebadd23d29e84e2553381b28aa75b732302e26a717df04df92e2b36a3990e584` |
| `docs/audit-2026-09/modules/M014-b1-self-raw.md`（席甲 B1+B2-r1） | V0.02 | 33645 | `fcd74ce48867c21e4ddcb7f4c22616cba9fd4e6cf4dea8956a77f3f08e4b3639` |
| 其中席甲 B1 前缀 | V0.01 | 29222 | `08a613b24236b34d9c2013e99d683d950b6bc6904839080f098d8c48783d6881` |
| `docs/audit-2026-09/findings.md` | V1.88 | 750964 | `557fd7bfa6e2d6175381bf2c871289ee3e21225f517596859fdff6a71542e907` |

ledger 只读：V3.24，583261 bytes，SHA256 `2faeb03fba9b6339d9f6cc34046f213bddbd7486309871fe754b0b283d92cf3c`；M014 IN_PROGRESS、B3=0。映射行 `M014 → [BEQR-063 … PGM-066]`（`criteria-lock.md` L6393）COUNT=84 unique=84 MISSING=[] EXTRA=[]。1 文件 `git ls-files -z -- docs/platform-governance-product-spec-v0.2.md | xargs -0 cat | wc -l` = **3494**。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

候选形态：V1.88 顶部为 r2 **条目覆盖段**（001/009 最小处置 + 非阻断并入 B3 边界）；其下「当前 M014 B2-r1 整批候选」001–020 原表保留。未改条目（002–008、010–020 的分类/队列）沿用 r1 表七列；修订条目（001/009）以顶部两行覆盖对应最小处置。003/005/011/020 分类/队列不变，B3 措辞以顶部「非阻断意见并入 B3 边界」为准。本席按此读取，不把 r1 表残留「L896/L903/L187/L2392 即全量」「业态代码值 | 只读展示 | bar/…/other」当作本批有效处置。

三 HTML 只读核验（本席未改；与 B1/B2-r1 同）：

| 文件 | 现行 | SHA256 |
|---|---|---|
| 业务地图 | V0.14 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| 治理地图 | V0.12 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| 枚举速查 | V0.07 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

本模块 1 文件 SHA 与 B1 覆盖表全等：`docs/platform-governance-product-spec-v0.2.md` 3494 行 / 221816 bytes / `b3ee635aa1763d1f5b1e6e7c7e381fe78bc71057c162caad39ae8cd3b8bbcd31`。

工作区只读：`git status --porcelain` 见 findings.md / ledger.md 已改（席甲落 r2 候选）、本两份 raw 未跟踪。本席只追加本指定路径。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核源/HTML/命令再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：001 是否仍漏同根活句、009 是否仍立法五码、003/005/011/020 非阻断是否按原意进 B3、有无新改写 |
| verification-before-completion | 声称前先跑只读哈希/行号/HTML 点读 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 删除测试：001 深度落在 HTML 九类 vs 归属根，不落删「项目根」；009 深度落在废止编号行，不落新建代码值字段 |
| ponytail-review | 只评最小删除/收窄，不落地。001 标归属根或移出闭集=最懒；009 跟随补录废止=最懒 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |

### r1 意见 → r2 候选对照（本席 B2-r1 原文，不采信席甲转述）

本席 B2-r1：**FAIL**。否决点两条：001 最小处置未收 L892/L900 同根活句；009 把五码写成「业态代码值」字段，改写 GR-009。另提出四条非阻断（不否决）：003 禁止项对齐 BSM L930 现场句且不立法「补录」；005 现状行紧接 L9、写明 L7–L9 为历史；011 L626 删「状态」不重复「是否启用」，L2643 只废止「不能晚于」；020 事实栏删 V217 误抄。其余 002–008、010–020 票 PASS，口径不变。

| 本席 r1 要求 | r2 候选（V1.88 顶部） | 落实？ |
|---|---|---|
| 001 补 L892 节标题与 L900 空间名称（标归属根或移出空间对象闭集）；保留 L56/L174/L2276/L902；L896/L903/L187/L2392 原方向保留 | L892 改「空间对象字段（楼层…设备点位；项目为归属根，非空间类型）」；L900 改「识别楼层…设备点位」（移出闭集）；L896 九类+归属根；L903 去「项目」；L187/L2392 标归属根；`:56`/`:174`/`:2276`/`:902` 保留；变更日志不改 | 是 |
| 009 L923 跟随补录废止、不改名为「业态代码值」；业态名称行保留六值中文；media 只括注演出传媒；不写其余五码 | L923「【已废止/不得回灌】业态编号…不采用…不得作为必填/唯一编号字段」；L922「演出传媒」括注「（系统枚举码 media）」；不写其余五码 | 是 |
| 003 禁止项对齐 BSM L930（代客下单、收款码、履约、扫码核销放行）；不写「补录」为私人手机新禁项 | 「003 禁止项对齐 BSM L930 现场句（代客下单、出示公司收款码、完成履约、扫码核销放行），不写「补录」为私人手机新禁项」并入 B3 边界 | 是 |
| 005 现状行紧接 L9，写明 L7–L9 两条现状句已过期、不得当现行 | 「005 现状行紧接 R5 块 L9、写明 L7–L9 两条现状句已过期为历史」并入 B3 边界 | 是 |
| 011 L626 删「状态」不重复「是否启用」；L2643 只废止「不能晚于」前半句 | 「011 L626 删「、状态」而不重复「是否启用」，L2643 只废止「不能晚于」前半句」并入 B3 边界 | 是 |
| 020 事实栏 V217 句删掉或标非本模块 B1 | 「020 事实栏「V217 薪资税档权限码非第 5 类」为邻模块口径误抄，作废」并入 B3 边界 | 是 |
| 002–008、010–018、019 口径不变 | 「其余 ID 分类/队列/处置不变」；r1 表对应七列未改写分类/队列 | 是 |

**r1 裁定未被误读。** 无把「001 补 L892/L900」读成仍只改 L896/L903；无把「009 跟随补录废止」读成仍建「业态代码值」；无把四条非阻断读成改分类/队列；无新永久 ID、无锁外判据、无 HTML 修改申请、无添加项、不进 BLOCKED-ASK。

### 完整差集（GR-001…016、OBS-01…12、席甲 CX-001…007、OBS-1…5）

漏并：无。两席 B1 全部临时 ID 仍落在 001–020。001/009 分类/队列未改写，只改最小处置。席甲独有 017/018 仍技术性「是」。

| 正式ID | 本次来源（不改写原意） | r2 分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M014-001 | GR-M014-001（席甲 B1 未发现） | 冲突/是/是（处置扩至 L892/L900） | **PASS（冲突/是/是；同根活句已收全）** |
| M014-002 | GR-M014-002 + CX-M014-002 | 细化/是/是 | **PASS（细化/是/是；r2未变）** |
| M014-003 | GR-M014-003（席甲 B1 未发现） | 冲突/是/是（B3 禁止项按非阻断收口） | **PASS（冲突/是/是；r2未变分类/队列）** |
| M014-004 | GR-M014-004 + CX-M014-001 | 细化/是/是 | **PASS（细化/是/是；r2未变）** |
| M014-005 | GR-M014-005 + 席甲 OBS-1 | 细化/是/是（B3 行位按非阻断收口） | **PASS（细化/是/是；r2未变分类/队列）** |
| M014-006 | GR-M014-006（席甲 B1 未发现） | 冲突/是/是 | **PASS（冲突/是/是；r2未变）** |
| M014-007 | GR-M014-007（席甲 B1 未发现） | 细化/是/是 | **PASS（细化/是/是；r2未变）** |
| M014-008 | GR-M014-008（席甲 B1 未发现） | 技术性/是/是 | **PASS（技术性/是/是；r2未变）** |
| M014-009 | GR-M014-009 + CX-M014-004 | 细化/是/是（L923 跟随补录废止，不新建字段） | **PASS（细化/是/是；不再立法五码）** |
| M014-010 | GR-M014-010 + CX-M014-003 | 技术性/是/是 | **PASS（技术性/是/是；r2未变）** |
| M014-011 | GR-M014-011 + CX-M014-005 | 细化/是/是（B3 L626/L2643 按非阻断收口） | **PASS（细化/是/是；r2未变分类/队列）** |
| M014-012 | GR-M014-012（席甲 B1 未发现） | 技术性/是/是 | **PASS（技术性/是/是；r2未变）** |
| M014-013 | GR-M014-013（席甲 B1 未发现） | 技术性/是/是 | **PASS（技术性/是/是；r2未变）** |
| M014-014 | GR-M014-014（席甲 B1 未发现） | 细化/是/是 | **PASS（细化/是/是；r2未变）** |
| M014-015 | GR-M014-015（席甲 B1 未发现） | 细化/是/是 | **PASS（细化/是/是；r2未变）** |
| M014-016 | GR-M014-016 + 席甲 OBS-5 | 细化/是/是 | **PASS（细化/是/是；r2未变）** |
| M014-017 | CX-M014-006（本席 B1 未登记） | 技术性/是/是 | **PASS（技术性/是/是；r2未变）** |
| M014-018 | CX-M014-007（本席 B1 未登记） | 技术性/是/是 | **PASS（技术性/是/是；r2未变）** |
| M014-019 | GR-M014-006 引文侧拆出 | 技术性（跨模块）/是/否（XM→M261） | **PASS（否合规；r2未变）** |
| M014-020 | 双方观察 | 技术性/正向 /否/否（V217 句作废） | **PASS（否；r2未变队列）** |

冲突 3（001、003、006）。细化入队 9（002、004、005、007、009、011、014、015、016）。技术性入队 6（008、010、012、013、017、018）。技术性跨模块否 1（019）。观察否 1（020）。添加 0。

### 001 修订核实（冲突／是；r1 否决点已闭合）

本席 r1 FAIL：只改 L896/L903 + 标注 L187/L2392 **不足**；§5 开篇 L892 节标题、L900 空间名称仍把项目写进空间对象闭集。允许的补法：同样标「归属根，非空间类型」**或**移出空间对象闭集。保留 L56/L174/L2276「项目根」与 L902「项目顶层除外」。变更日志可不改。

本批类型 4 核源（只读，规格未改，核的是候选覆盖是否对准现行活句）：

- PGM V0.12 L469：「维护楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位。」无「项目」作为空间类型。
- 现行活句仍在：L892 节标题含「（项目、楼层…）」；L896 伪称 HTML 并集含项目；L900「识别项目、楼层…」；L903 选项含「项目」；L187/L2392 空间树首项「项目」。
- KEEP 集仍在且应保留：L56/L174/L2276「项目根」；L902「必填，项目顶层除外」。L1291「空间层级 P0 为项目根节点以及楼层…设备点位」同属归属根措辞（「上述九类」指以及之后的九个空间类型，不含项目根）；mall 括注归 002，**不是** 001 残留否决点。
- 变更日志 L781/L1315/L2736 为历史行。补录 L3265/L3267 已无「项目」作空间类型。L2401「所属项目」是字段不是 space_type。

r2 覆盖：

- L892：标「项目为归属根，非空间类型」——命中 r1「或」支的标注支。
- L900：改「识别楼层…设备点位」——命中 r1「或」支的移出闭集支；与补录 L3265 同构。空间名称不再识别项目实体（项目名称在 §4 项目字段），正确。
- L896/L903/L187/L2392：方向与 r1 已准许的原处置相同（九类 + 去选项「项目」+ 树首项标归属根）。
- KEEP 四句明确保留。变更日志不改。

同根现行活句已收全。不删「项目根」仍为最懒且正确。无业务发明。不改 HTML。

非阻断（不否决）：r2 L896 写成「收敛为九类」而未点名 HTML 九类名词。B3 宜写成「楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房、设备点位」九类，避免「九类」被回读成含项目。L892/L903 已点名，不构成覆盖缺口。

票：**PASS**。r1 否决点闭合。

### 009 修订核实（细化／是；r1 否决点已闭合）

本席 r1 FAIL：候选把 L923 改名为「业态代码值 | 实现映射 | 只读展示 | bar/…/other」。必填去掉了，但字段字典仍多出一条 P0 行，校验口径闭集仍是五码+media。只读不等于未立法。GR-009：「正文跟随补录废止『业态编号必填』；页面只展示六值名称；`media` 可作为演出传媒的系统码备注，**不得立法另外五码为必填编号**。」

本批类型 4 核源：

- 现行 L923：「业态编号 | 必填 | 选择 | bar / catering / retail / amusement / media / other」。全文活句仅此一行仍立法六码编号（补录 L3287 已【已废止/不得回灌】且「不采用」）。
- 现行 L922：「酒吧、餐饮、零售、游乐、演出传媒、其他六值闭集」——六值中文，无五码。
- ENUM V0.07 L195：「酒吧、餐饮、零售、游乐、演出传媒（系统枚举码 media）、其他。」仅演出传媒标注 `media`。
- BSM-072 映射在本模块 84 条内。

r2 覆盖：

- L923 改为「【已废止/不得回灌】业态编号 | 补录已废止：ENUM 只有六值名称，未批独立编号字段 | 不采用 | — | 不得作为必填/唯一编号字段 | —」。跟随补录 L3287，**不**改名为「业态代码值」，**不**保留 bar/catering/… 校验闭集。废止行不是新字段。
- L922 仅对「演出传媒」括注「（系统枚举码 media）」；不写其余五码。与 ENUM L195 逐字同构，是系统码备注，不是必填编号字段。

不再立法五码。无业务发明。不改 HTML/Java/V384。

票：**PASS**。r1 否决点闭合。

### 003 非阻断核实（冲突／是；r2未变分类/队列）

r1 票 PASS。非阻断：禁止列表「核销、点单、放行、结算、补录」不是 BSM L930 逐字；B3 对齐现场句（代客下单、出示公司收款码、完成履约、扫码核销放行）；「补录」是工作手机事项，不得单独立法为私人手机新禁项。

核源：BSM V0.14 L930 现场句确为「服务点单的代客下单、出示公司收款码、完成履约、扫码核销放行」；PGM V0.12 L741 低风险六项 +「等」；BSM L930 另含看排班。r1 正列表合取仍成立。

r2 顶部将该非阻断并入 B3 边界，分类/队列未改。r1 表 003 处置单元格仍写「不承接现场核销、点单、放行、结算、补录」——属未覆盖的历史单元格，**不是**本批有效 B3 口径。

票：**PASS**。r2未变。

### 005 非阻断核实（细化／是；r2未变分类/队列）

r1 票 PASS（加一行满足「改写」支）。非阻断：新行必须紧接 L9，写明 L7–L9 两条现状句已过期、不得当现行；不得写到块外。

核源：R5 L6 块头；L8 写 evaluate 孤立/闸口不触发；L9 写后端未独立校验开关；L10 业务裁决；L11 候选验收。「两条现状句」实为 L8 与 L9；「紧接 L9」= 插在 L9 与 L10 之间。r2「紧接 R5 块 L9、写明 L7–L9 两条现状句已过期为历史」落实行位。不删历史行。最懒未变。

票：**PASS**。r2未变。

### 011 非阻断核实（细化／是；r2未变分类/队列）

r1 票 PASS（三处未漏）。非阻断：L626 现行「是否启用、状态」应**删除「状态」**，不要改成第二个「是否启用」；L2643 只废止前半句「不能晚于」，保留「不设定时启用/定时停用」。

核源：L626「终端类型、开关范围、业务系统、模块、是否启用、状态」；L2643「生效时间不能晚于失效时间。不设定时启用/定时停用。」r2「L626 删「、状态」而不重复「是否启用」，L2643 只废止「不能晚于」前半句」与 r1 原意逐字同向。r1 表 011 处置「L626「状态」改「是否启用（…）」」为历史单元格，B3 以 r2 覆盖为准。

票：**PASS**。r2未变。

### 020 非阻断核实（观察／否；r2未变队列）

r1 票 PASS（否）。非阻断：事实栏「V217 薪资税档权限码非第 5 类（M013-006 口径）」不是本席 M014 B1 观察，属邻模块误抄；r2 删掉或标非本模块 B1。

r2「作废（本条不是任一席 M014 B1 观察）」= 删除该句。其余观察（specific_role 实现缺口归 review 模块、无总变更日志等）仍否、不升队。未把应升项留观察。

票：**PASS（否）**。r2未变。

### 002 / 004 / 006–008 / 010 / 012–019（r2未变，复述 r1 票）

无新事实。规格 1 文件指纹与 B1 全等，HTML 三份哈希与 B1 全等。分类/队列/最小处置与 r1 表一致（006/019 拆分、007 删图、010 `dealer_portal`、013 `%%` 等均未改写）。

| ID | r1 票（本席 V0.02） | 本票 |
|---|---|---|
| 002 | PASS（细化/是/是；与 M013-004 同构） | **PASS；r2未变** |
| 004 | PASS（细化/是/是） | **PASS；r2未变** |
| 006 | PASS（冲突/是/是；L48 收口到三 HTML+§0A） | **PASS；r2未变** |
| 007 | PASS（细化/是/是；删图最懒，不丢 BEQR-166） | **PASS；r2未变** |
| 008 | PASS（技术性/是/是） | **PASS；r2未变** |
| 010 | PASS（技术性/是/是；统一 dealer_portal） | **PASS；r2未变** |
| 012 | PASS（技术性/是/是） | **PASS；r2未变** |
| 013 | PASS（技术性/是/是；%% 最懒） | **PASS；r2未变** |
| 014 | PASS（细化/是/是） | **PASS；r2未变** |
| 015 | PASS（细化/是/是） | **PASS；r2未变** |
| 016 | PASS（细化/是/是） | **PASS；r2未变** |
| 017 | PASS（技术性/是/是） | **PASS；r2未变** |
| 018 | PASS（技术性/是/是） | **PASS；r2未变** |
| 019 | PASS（否，XM→M261） | **PASS；r2未变** |

### 残留措辞（不构成新改写，不因此 FAIL）

r1 表 001 处置仍写只改 L896/L903/L187/L2392；009 仍写「业态代码值 | 只读展示 | bar/…/other」；003 仍写「核销、点单、放行、结算、补录」；011 仍写 L626「状态」改「是否启用」。这是未覆盖条目沿用原表的历史单元格，**不是**本批有效口径。定稿后须以 V1.88 顶部 r2 覆盖段 + 「非阻断意见并入 B3 边界」覆盖这些单元格；B3 若按 r1 表 009 新建「业态代码值」或按 r1 表 001 漏改 L892/L900，即违反本席本票。

### 分类 / 队列 / 允许的最小修复边界

本批可定稿（仍须席甲本批 B2-r2 PASS 合取）。队列是=001–018；否=019、020。一次 B3 将只改本模块 1 文件（V0.05→V0.06，头部一行变更记录 + 三张原文件变更日志各一行），不改源码/迁移/OpenAPI/三份 HTML/锁/其它文档/M261 文件。补录历史原文只加订正括注或废止前缀，不删。无测试面，B4 以 diff 逐块归因复审。文档类按类型 4 最小命令举证，不强制造测试。

- 001：L896 改「以三份业务 HTML 收敛为楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房、设备点位九类；综合体项目是空间树的归属根（projects 表），不是空间类型」；L903 用途/选项去「项目」；L892 节标题改「空间对象字段（楼层…设备点位；项目为归属根，非空间类型）」；L900 改「识别楼层…设备点位」；L187/L2392 首项改「项目（归属根，非空间类型）」；**保留** L56/L174/L2276「项目根」与 L902「项目顶层除外」；变更日志历史行不改。
- 002：L1291 括注改字典活校验；mall 仅契约快照；字典与 HTML 九类差登记为可维护；不改 yaml/V184。
- 003：L2675 合取 PGM L741 + BSM L930 低风险闭集（含看排班、查看考勤）；禁止现场项对齐 BSM L930（代客下单、出示公司收款码、完成履约、扫码核销放行）；不写「补录」为私人手机新禁项；不写私人手机打卡。
- 004：L761/L2721 改 V394 已闭合。
- 005：R5 现状行**紧接 L9**，写明 L8/L9（evaluate 孤立、开关未独立校验）已过期为历史；不删历史行；不得写到块外。
- 006：L48 改三 HTML+§0A；该文仅实现参考、父链已删见 019。
- 007：删除合同 stateDiagram，改一句八成员不定义边；§9.4/F3 审批链不动。
- 008：标题改 `/api/v1/access/effective-permissions`。
- 009：L923 **跟随补录废止**（【已废止/不得回灌】、不采用），不改名为「业态代码值」、不保留五码校验闭集；L922 业态名称行保留六值中文，仅「演出传媒」括注「（系统枚举码 media）」。
- 010：全文统一 `dealer_portal`，标注尚未实现，解释权仍 HTML 中文名；补录 L3245 订正括注。
- 011：L2634 列表对齐 §14A.3；L2643 只废止「不能晚于」前半句，保留「不设定时启用/定时停用」；L626 **删「、状态」**（不要重复「是否启用」）。
- 012：关系图加 F11 节点与边。
- 013：L1541 改 `%%` 注释，保留 L1542。
- 014：L3343 标【已废止】并注 supervisor_review_decisions。
- 015：§3.1 一级列收口到「平台治理」下的分组。
- 016：四处修订说明改「手机专属设备标识；指纹未立法」；变更日志不改。
- 017：L955 改为 V028 合规到期台账。
- 018：L335 去掉悬空 §2999。
- 019/020：本模块 B3 不修。020 事实栏不得回写 V217 句。

禁止：把项目写成 space_type；漏改 L892/L900；把 mall 当活码；私人手机列表保留「只通知和状态查看」而不列 HTML 闭集；把「补录」写成私人手机新禁项；把 R5 现在时当 2026-09 现状；把 merchant-space-contract-model 当权威；合同状态自造边；业态五码立法为字段或改名为「业态代码值」；L626 把「状态」改成第二个「是否启用」；本模块改 yaml/HTML/M261 文件。

改动文件自身版本递增。

### 本席实跑（B2-r2，只读；无写模式）

- 锁文件 sha256sum：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 sidecar 内容、ledger 顶行、本文件既有文首独占行同值）
- 输入三文件字节与 SHA：上表全等
- 1 文件指纹：3494 行 / 221816 / `b3ee635a…bbcd31` 与 B1 表全等
- 点读：PGM L469 九类无项目；ENUM L195 仅演出传媒括注 media；BSM L930 现场四项；规格 L892/L900/L923/L922/L626/L2643/L8–L11/L56/L174/L2276/L902/L1291/L3287 行文如上
- `HEAD`=`origin/main`=`685dca4e1088ed59e68196c3c5b45787fde9db02`
- 未跑：主仓测试、psql 写、部署、修复
- 未改 findings/ledger/锁/源/HTML/主索引

### 差集与最小修正意见

差集：无。r1 两条否决均已按原意闭合；四条非阻断均已并入 B3 边界；其余 14 个 ID 无新事实。

最小修正（非阻断，不否决、不挡定稿）：

1. B3 L896「九类」点名 HTML 九类名词（见上 001 边界）。
2. 定稿/B3 以 V1.88 顶部覆盖段为准，不得回退 r1 表 001/009/003/011 残留单元格。

无添加项。不发 2.2。无 HTML 修改申请。

### 结论

候选按本席 B2-r1 意见修订：001 补 L892 标注归属根、L900 移出空间对象闭集，KEEP 四句保留；009 L923 跟随补录废止、L922 仅演出传媒括注 media，不再立法五码。003/005/011/020 非阻断已并入 B3 边界。002–008、010–020 中其余项口径未改。全量差集无漏并、无新 ID、无新判据、无本批添加项。r1 任一裁定均未被误读。r1 表残留单元格不改变已裁定的队列/分类。

逐 ID：001 PASS、002 PASS（r2未变）、003 PASS（r2未变）、004 PASS（r2未变）、005 PASS（r2未变）、006 PASS（r2未变）、007 PASS（r2未变）、008 PASS（r2未变）、009 PASS、010 PASS（r2未变）、011 PASS（r2未变）、012 PASS（r2未变）、013 PASS（r2未变）、014 PASS（r2未变）、015 PASS（r2未变）、016 PASS（r2未变）、017 PASS（r2未变）、018 PASS（r2未变）、019 PASS（r2未变）、020 PASS（r2未变）。

本席队列：**001–018 是（18 项）；019、020 否（2 项）**。

本席 B2-r2：**PASS**

未经双 PASS 不进 B3，更不称模块 PASS。步骤仍 B2，M014 `IN_PROGRESS`，B3=0。不覆盖上方 B1 FAIL 与 B2-r1 FAIL 原文。
