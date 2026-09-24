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

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M014 B1 Grok 独立审计原文（重开追加）

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；席甲=Claude，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 整模块全量审计（操作员从 M009 重做续作；席甲预检发现问题，按 160-audit-prompt.md V0.17 完整双席重开）
- 报告自身版本：V0.04（指定路径旧前缀 105470 bytes / 旧 V0.03 完整保留，本次追加；不覆盖）
- 模块：M014 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第2段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 1 文件，计划冻结有效内容行 **3494**（本席不改计划）；标记 OVERSIZE-SINGLE-FILE
- 受审文件：`docs/platform-governance-product-spec-v0.2.md` 现行工作区 V0.06，`splitlines`=**3491**，bytes=226970，SHA256 `dfb40482f22455c27071990d09b7a624248f3604d186a5b1293fb0ebb84f7396`，endswith `\\n`=True；`git hash-object`=`ed8b55d19314c59be042ebb511710fbdb0185af4` 与 `HEAD:` 同 blob（本文件无未提交改写）
- 冻结判据：84 条（BEQR 21 / BSM 12 / GEN 18 / PGM 33），与锁映射行 L6393 逐字相同；COUNT=84 unique=84 MISSING=[] EXTRA=[]
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M014-B1-01、GR-M014-B1-02、GR-M014-B1-03
- 添加项（B1 只登记，不代用户回答 2.2；本批无）：无
- 只登记、建议队列「否」：观察见 §8（含历史 M014-019 XM→M261、M014-020 既有边界；不重编号、不伪称新需求）
- 未修源码、未改三 HTML、未改计划/findings/ledger/锁/主索引、未 fetch/commit/push/merge/deploy、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 `M014-b1-self-raw.md` 新追加段、未问操作员、未对任一 `complex_test*` 库做写操作
- 历史：原 Claude Fable 5.1 / Grok B5-r1 双 PASS 与 B3 累计 1 保留，未清零；本次尚未 B3。旧 001–018 已修转否、019 跨模块归 M261、020 登记，当前二十项否。本席独立重核 001–018 无回归（009 的字段行已废止；页面清单/低保真「业态列表」仍列「编号」作新证据，见 GR-M014-B1-03，不重开 009 号）。
- 现行代码按主工作区工作状态读取（含已批准未提交的 M009/M010/M013 修复：V427 主体项目关联、V392 授权申请六类、绑定设计 V0.13）。文档与三 HTML 现文按现行文件核，不按旧轮结论。

## 0. 技能自核（V0.17 §6，按 B1 收窄；本席环境实核）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 V0.17 controller 优先于技能本体「先派子代理 / 先问人」 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 删除测试：本规格若删掉已废止闭集回灌、过期「当前实现」句与假完成 `project_id` 句，复杂度回到 HTML 四类绑定/十一端/状态成员/项目归属本身；不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（sha256 / HTML 行命中 / AccessController 映射 / AccessService 改职级重算 / V427 无主体 `project_id` 列 / V394 / V111 / V184 / V028 / ActionGateAspect）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（规格陈述→HTML 原句→现行 Java/迁移）；不作无 ID 插桩 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；`verify:openapi` 未跑、亦不等于规格与 HTML 一致 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推、未安装 hook（安装会写仓库外配置，B1 禁止） |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒级，未起 monitor |
| graft | `~/.claude/skills/graft/SKILL.md` | `graft ask` 职级重算、`graft grep` ProjectAssociation；只读 |
| domain.md | `docs/agents/domain.md` | 词表/三 HTML 最终解释权；未改 CONTEXT.md |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到独立技能
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- `~/.grok/bundled/skills/graft/SKILL.md`：MISSING；改用 `~/.claude/skills/graft/SKILL.md`（存在）
- Arc `audit` 技能会派评审子代理并另写 `docs/arc/audits/`：与双席/§7 冲突，**不调用**
- superpowers `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄
- Trail of Bits `audit-context-building` 会派 function-analyzer 子代理并另写 `audit-context/`：与禁止子代理冲突，**不调用**
- grilling 本体「等用户回答 / dispatch sub-agent」作废

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain、MCP、第三主体。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本次报告第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- ledger 顶部 `CRITERIA-LOCK-SHA256:` 同值（只读，未改）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行 `M014 → [...]`（L6393）与开工口径 84 项逐字相同
- `plan/modules.md` M014 FILES 1 条与用户清单逐字相同
- `git ls-files -z -- docs/platform-governance-product-spec-v0.2.md | xargs -0 cat | wc -l` → **3491**（计划冻结 3494 不改；差额来自历史 B3-r1 净删，属已知计划/文件行差，不把计划改成 3491）
- 该文件 SHA256 与用户固定清单 match=True

HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`。`origin/main` 当时读值为 `246385d9ef73d9a5463e35012cf12003a425e33d`（与 HEAD 不等）。本席未 fetch、未设 `GIT_INDEX_FILE`、未动主索引。审计对象是主工作区现行文件（含已批准未提交修复），不是只读 HEAD 旧 blob；本模块单文件与 HEAD blob 指纹一致。

工作区：`git status --porcelain` 含大量 M009–M013 已批准未提交修复及审计产物；本 1 文件未改。本席只读，未再写 ledger/findings/计划。

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

三份 HTML 只读、未改。锁内本模块 HTML 锚点来源行对现行 HTML **84/84 hit**（python 按锁「来源：`path:line`」逐条打开，全部 `1<=line<=nlines`）。

三 HTML 当前字节/SHA256（只读核验，本席未改）：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| docs/business-enum-quick-reference-v0.1.html | 133961 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | 304482 | eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 |
| docs/platform-governance-map-for-operations-v0.1.html | 103015 | a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 |

三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。BSM-001/PGM-001 未映射本模块，只作对照。

绑定设计现行：`docs/business-entity-binding-development-design-v0.1.md` V0.13，154 行，SHA256 `b85d4d42a6bf7bfcdba197da90cf132ed07b8012080a311c3110f97506f25143`（只读，作实现口径对照，不扩审该文件）。

## 2. 全文件分段覆盖表（全文已读；行=`splitlines`=`\n` 计数）

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | docs/platform-governance-product-spec-v0.2.md | 3491 | 226970 | dfb40482f22455c27071990d09b7a624248f3604d186a5b1293fb0ebb84f7396 | 已读 L1–L3491（标题 V0.06；OVERSIZE 按结构段分批阅读，raw 列全覆盖；未只读标题/搜索/截断） |

用户固定清单逐文件 match。endswith `\\n`=True。

### 2.1 结构段已读行区间（四份原文件 + §24 + 补录）

| 段 | 行区间 | 内容 | 已读 |
|---|---|---|---|
| 包装头 | L1–L23 | 标题 V0.06、R5 缺口标注（含 2026-09-11 现状行）、合并说明与四源归档表 | 是 |
| §0A | L25–L36 | 金额精度/合同本地 AI/当地时间/外部经销商跨篇承接 | 是 |
| **原文件 1 页面清单** | **L38–L790** | | |
| 定位 | L42–L51 | 文档定位；名词四层脚注（权威三 HTML + §0A） | 是 |
| §2–§20 | L53–L790 | 15 个 P0 页、§14A 终端开关、跳转、验收、已确认、变更日志 | 是 |
| **原文件 2 字段字典** | **L792–L1333** | | |
| 定位与规则 | L796–L841 | 脱敏七项；不提供关闭开关 | 是 |
| §3–§19 | L843–L1333 | 全局/项目/空间/业态/商户/合同/员工/设备/权限/留痕/状态字典/验收/变更日志 | 是 |
| **原文件 3 流程图** | **L1335–L2240** | | |
| F1–F11 / 12A | L1337–L2240 | 含 F3 无迁移边、F9 角色装配、F11 终端开关、跨流程依赖 | 是 |
| **原文件 4 低保真** | **L2242–L2737** | | |
| §1–§23 | L2246–L2737 | 一级导航分组、各页低保真、私人手机低风险闭集、集团视图已承接 | 是 |
| §24 深度补齐 | L2739–L2849 | 有效权限集接口 / 字段级投影待补齐 / 审批层级映射 | 是 |
| 继续合并补录 | L2851–L3491 | 三源历史摘录；已废止闭集标【已废止/不得回灌】 | 是 |

mermaid 代码块计数=13。未抽样。

## 3. 第一性原理三问

### 3.1 为什么必须存在？

本文件是三份业务 HTML 在「平台治理 P0 页面 / 字段 / 流程 / 低保真」上的产品规格合并入口。删掉它，HTML 的模块入口（PGM-003…016）、四类主体（BEQR-143）、项目归属（PGM-017/018）、合同八态（BEQR-156）、十一端（BEQR-150）、最终权限计算（BEQR-151）、角色装配（PGM-041）将没有可执行到页面字段与流程的规格载体。答得上 → 有业务上位需求。不因此豁免错误现行句。

### 3.2 声称的行为有没有被实际执行验证过？

文档模块不强制新测试。可执行声称用类型 4 命令 + 类型 1 HTML 原文核：

| 声称 | 命令/读取 | 结果 |
|---|---|---|
| GET `/api/v1/access/effective-permissions` | `AccessController.java:32 @RequestMapping("/api/v1/access")` + `:244 @GetMapping("/effective-permissions")` | 入口存在；self 端仅需登录 |
| GET `/api/v1/access/users/{id}/effective-permissions` | 同文件 `:250-253`；`requirePermission("access.user.view")` | 入口存在且服务端校验 `access.user.view`，与 L2756 一致 |
| 前端按 `hasPermissions` 消费 | `rg hasPermissions apps/web` | 多页引用 `../security/PermissionGate` |
| V394 `analytics.group.view` requires_approval=true | `V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql:16-18` | TRUE，L764/L2717 审批属性句为真 |
| R5 现状：gate 串开关→准入→权限 | `ActionGateAspect.java:43-49`；`StaffApprovalService.java:44,61 evaluate` | 与 L12 现状行一致 |
| V184 空间字典八码 | `V184__space_type_dictionary.sql` INSERT building/floor/zone/shop/public_area/parking/booth/other | 与 L1295 括注一致；无 mall 活码 |
| V111 终端十码无 dealer_portal | `V111` CHECK `'app'..'mobile_admin','*'` | 与 L869 诚实缺口一致 |
| V028 合规到期台账 | `V028` `compliance_expiry_items` / `compliance_reminders` | 与 L959 一致；全仓无 `merchant_credentials` |
| 改职级重算有效权限 | `AccessController:262 PUT /users/{id}/staff-level` → `AccessService:1305-1313` 已定岗改职级走 `assignUserPositionDirect` | **现行会重算**；与 L1979 排除句相反 → GR-M014-B1-01 |
| 所有对象继续携带 `project_id` | V427：`merchants` 补 `project_id`；`business_entity_projects` 多对多；`business_entities` 无 `project_id` 列（绑定设计 V0.13 L38/L56） | **假完成** → GR-M014-B1-02 |
| §24.3 仍按职级算术与 specific_role | `ApprovalLevel.java:13-14 LEVEL_STEP=3`；`SPECIFIC_ROLE`；V057 CHECK 含 `specific_role` | 仍为真；属 M014-020 既有边界，不新立项 |

### 3.3 哪个前提未经验证就当真？

- 「继续携带 `project_id`」把 HTML「项目归属」和集团 PRD 技术列名写成了当前全对象列事实；未核 V427/主体表。已拆穿。
- 「不把仅职级变化列为触发条件」把规格自己的排除写成规则，未核 HTML「层级/职位定完系统自动适配」与 `PUT /users/{id}/staff-level`。已拆穿。
- §24.2「字段级按权限动态投影待补齐」：代码核验写明未成体系，FieldProjection* 已删。诚实待对齐，不升缺陷。
- §24.3 实现算术：规格已声明「实现侧算术不得写入产品规格」；缺口归 review 实现模块（020）。
- 补录段是历史摘录载体，现行以【已废止/不得回灌】与三 HTML 为准；不把补录未废止的过期句单独当新立法，除非活正文重复。

## 4. 顺从式：84 判据适用证据

每条均读锁定义全文 + 来源 HTML 锚点行。文档模块：输入=规格陈述，状态=ENUM 成员闭集，输出=页面/字段/流程口径，错误=已废止回灌标记，权限=最终权限五源与隔离，持久化=只把迁移/入口当证据，测试=不强制新测，文档=本文件。无 finding ≠ 未审。

### 4.1 BEQR（21）

| ID | 锚点 | 本文件承接 | 结论 |
|---|---|---|---|
| BEQR-063 高危动作 | ENUM:111 | L1976/L660 对照：沽清/反结/存酒作废列入高危授出前审批；不另发明酒吧细则 | 适用，无新冲突 |
| BEQR-074 隐私桌台 | ENUM:125 | 本文件无桌台权限发明；权限页只写功能最小颗粒 | 映射适用，正文沉默不升 |
| BEQR-143 四类主体 | ENUM:226 | L937–L948、PGM-026 对照：经营对象/运营/合同/结算 + 绑定关系编号 | 适用 |
| BEQR-144 经营主体关系 | ENUM:227 | L51 权威回到 HTML；多对多非行政链（§0A/脚注）；不把 merchant-space-contract-model 当权威（001–006 已修） | 适用 |
| BEQR-145 城市项目公司 | ENUM:228 | 本文件项目页管综合体档案，不把 projects 表写成项目公司 | 适用，无相反规则 |
| BEQR-146 运营公司职责 | ENUM:229 | 无把娱乐平台写成收取单一酒吧门票 | 适用，沉默不升 |
| BEQR-147 主要收入归属 | ENUM:230 | §0A 金额/尾差；不另发明收入名目 | 适用 |
| BEQR-148 合作模式 | ENUM:231 | L949/L981 六值含「其他」；「其他」收费模板未进字段表、无相反规则 → 维持 020，不新补需求 | 适用；不立项 |
| BEQR-149 职级 | ENUM:232 | L1038/L744 M1–M10 上中下；L1979 排除仅职级变化与 HTML/实现冲突 → GR-01 | 适用，**FAIL 项** |
| BEQR-150 终端十一端 | ENUM:233 | L122/L648/L869 十一中文名；`dealer_portal` 标未实现（V111 十码） | 适用 |
| BEQR-151 最终权限计算 | ENUM:234 | L2755 五源 − 拒绝优先；接口路径已订正；通配不作成第六源 | 适用 |
| BEQR-152 项目与敏感数据 | ENUM:235 | L606/L1106 项目+商户隔离无关闭开关；L833 脱敏七项 | 适用 |
| BEQR-153 终端开关优先级 | ENUM:236 | L646/L2131 七档；缺行默认关；拉取失败兜底关；留痕例外 | 适用 |
| BEQR-154 项目状态 | ENUM:237 | L169/L1156 六成员，不定义迁移边 | 适用 |
| BEQR-155 商户状态 | ENUM:238 | L287/L1158 六成员；试运营不记录；退出缓冲期 | 适用 |
| BEQR-156 合同状态 | ENUM:239 | L332/L1169 八成员；无迁移边（007 已修）；当地时间 §0A | 适用 |
| BEQR-157 员工状态 | ENUM:240 | L415/L1196 五成员 | 适用 |
| BEQR-158 设备状态 | ENUM:241 | L527/L1182 七成员 | 适用 |
| BEQR-159 权限状态 | ENUM:242 | L605/L1209 五成员 | 适用 |
| BEQR-166 合同审批链 | ENUM:255 | L320/L1541 主线默认总经理+平行线全部通过，驳回整单退回 | 适用 |
| BEQR-189 管理费方式 | ENUM:291 | 本文件无管理费方式发明 | 映射适用，沉默不升 |

### 4.2 BSM（12）

| ID | 锚点 | 本文件承接 | 结论 |
|---|---|---|---|
| BSM-004 存档删除 | MAP:566 | 商户/空间类型等走 archive/delete-request 口径在实现模块；本文件 L285 归档/黑名单 | 适用文档层 |
| BSM-005 分步引导 | MAP:581 | 低保真表单分组；无相反规则 | 适用 |
| BSM-006 动态二维码 | MAP:584 | 本文件不立法核销二维码间隔 | 沉默不升 |
| BSM-007 平台管理员 | MAP:587 | L2756 日常功能 vs 资金/敏感/删除/最终审批须逐项授予 | 适用 |
| BSM-009 基础管理 | MAP:596 | 页面分组即基础管理入口 | 适用 |
| BSM-028 主要功能 | MAP:698 | 项目/空间/商户/合同/人员/设备 | 适用 |
| BSM-029 和谁有关 | MAP:714 | 各页主要用户表 | 适用 |
| BSM-072 商户管理 | MAP:1308 | 业态六值、合作模式六值；业态编号字段已废止，列表仍列「编号」→ GR-03 | 适用，**FAIL 项** |
| BSM-074 和谁有关 | MAP:1326 | 商户页用户 | 适用 |
| BSM-114 各类入口 | MAP:1827 | L2671 私人手机低风险闭集对齐 MAP L1865 / PGM L741 | 适用（003 已修仍成立） |
| BSM-115 相互关系 | MAP:1897 | F1–F11 依赖表 | 适用 |
| BSM-120 已确认规则 | MAP:1957 | 各「已确认」节；以 HTML 为准 | 适用 |

### 4.3 GEN（18）

| ID | 本模块用法 | 结论 |
|---|---|---|
| GEN-001 必要性 | 见 §3.1；已废止回灌已标 | 通过（不挡 GR） |
| GEN-002 可执行正确性 | 声明的 access 入口均有 Controller 映射；`project_id`/`职级排除` 声明与行为不符 | **GR-01/02 引用** |
| GEN-003 测试有效性 | 文档审计不强制新测；本模块无测试面 | 不适用强制，不 FAIL |
| GEN-004 信任边界 | 无本文件发明的输入校验器 | 文档层 N/A |
| GEN-005 授权 | §24.1 指定用户接口要求 `access.user.view` 已核 | 通过 |
| GEN-006 完整性 | 不在本文件落库 | N/A |
| GEN-007 并发幂等 | 不为极端并发补需求 | N/A |
| GEN-008 一致性 | L764 与 L849 所属项目口径互斥；业态编号废止与列表列互斥 | **GR-02/03** |
| GEN-009 错误处理 | 无伪成功接口声称未核 impl | N/A |
| GEN-010 安全 | 脱敏七项与 U 盘例外对齐 HTML | 通过 |
| GEN-011 留痕 | 操作留痕七态；作废是动作 | 通过 |
| GEN-012 资源边界 | 无 | N/A |
| GEN-013 构建 | 无 | N/A |
| GEN-014 迁移 | 只作证据：V184/V111/V028/V394/V427/V392/V057 | 证据用，不扩迁移队列 |
| GEN-015 界面 | 低保真结构；mermaid 历史非法行已改注释 | 通过 |
| GEN-016 文档可验证 | 假完成 `project_id`、职级排除与现行入口相反 | **GR-01/02** |
| GEN-017 四分类 | 本批三项按冲突→细化顺序；无添加 | 见 §6 |
| GEN-018 证据类型 | 每条 GR 落到 HTML 原文或命令输出 | 见 §6 |

### 4.4 PGM（33）

| ID | 锚点 | 本文件承接 | 结论 |
|---|---|---|---|
| PGM-002 治理顺序 | :308 | F5 先阻断再升级 | 适用 |
| PGM-003 模块入口 | :339 | 低保真一级「平台治理」下分组（015 已修） | 适用 |
| PGM-004 综合体和集团 | :342 | 项目页 + 集团视图已承接时态 | 适用 |
| PGM-005 空间和点位 | :347/:469 | 九类名词；项目为归属根非类型（001 已修仍成立） | 适用 |
| PGM-006 商户和主体 | :352 | 商户页+四类绑定字段 | 适用 |
| PGM-007 合同和准入 | :357 | §9.6 supervisor_review；admission_audits 废止（014） | 适用 |
| PGM-008 员工和手机 | :362/:741 | 设备凭证定义废止指纹；私人手机低风险闭集 | 适用 |
| PGM-009 权限和职级 | :367 | 模板+职级；GR-01 打在装配细则 | **FAIL 项** |
| PGM-010 终端开关 | :372 | §14A/F11/17A；定时启停废止 | 适用 |
| PGM-011 设备管理 | :378 | 台账+领还七态 | 适用 |
| PGM-012 商户侧 | :383 | 边界：页面不承接经营动作 | 适用 |
| PGM-013 开放接口 | :388 | 本文件不写开放平台细则 | 沉默不升 |
| PGM-014 留痕复核 | :393 | 操作留痕页 | 适用 |
| PGM-015 楼宇运营 | :399 | 招商前链在补录/HTML；本文件商户页不否定 | 适用 |
| PGM-016 应急与隐私 | :404 | 脱敏七项 | 适用 |
| PGM-017 当前一个项目未来集团 | :412 | L748/L2700 归属口径对齐；L764/L2717 `project_id` 假完成 | **GR-02** |
| PGM-018 物理项目与运营公司 | :415 | 多对多；V410 删父链已在脚注 | 适用 |
| PGM-019 各主体职责 | :424 | 不把项目页写成域公司经营 | 适用 |
| PGM-020 主要功能 | :440 | 项目档案+归属列表（商户/空间/合同/员工/设备/票务/看板） | 适用 |
| PGM-021 业务意义 | :452 | 隔离+集团总览 | 适用 |
| PGM-026 商户主要功能 | :492 | 四类绑定、联系人、合作模式 | 适用 |
| PGM-027 业务边界 | :507 | 权责配置不由普通开关改 | 适用 |
| PGM-031 外部经销商 | :555 | §0A 承接；HTML 无独立经销治理页不升 | 维持 020 |
| PGM-035 提成发放 | :599 | 本文件不发明提成发放流程 | 沉默不升 |
| PGM-040 单一功能授权 | :643 | 角色权限页 | 适用 |
| PGM-041 权限规则 | :647/:658 | 角色装配活句大部分逐字承接；末句排除职级变化与同 li「层级/职位自动适配」冲突 | **GR-01** |
| PGM-042 职级规则 | :664/:669 | 「职级影响默认权限模板」vs L1979 排除仅职级变化 | **GR-01** |
| PGM-043 外包与审计 | :673 | 本文件无相反外包规则 | 沉默不升 |
| PGM-044 桌台挂单经销权限 | :688 | 无发明 | 沉默不升 |
| PGM-048 商户侧全开放 | :749 | 边界清晰 | 适用 |
| PGM-060 会员生物信息 | :879 | 脱敏/敏感页 | 适用 |
| PGM-065 连接关系 | :935 | F 总览 | 适用 |
| PGM-066 已拍板 | :951 | 已确认节 | 适用 |

84/84 已核。映射外判据不引用。

## 5. 对抗式（grilling 结构，自问自答；事实本席自读）

❓ **Q1 — L1979 排除「仅职级变化」是否与 HTML 同 li 共存？**
➡️ 不能。PGM-041 L658 同一条「角色装配」先写 HR 定每个人的**层级/职位**后系统自动适配角色和权限模板；PGM-042 L669 写职级影响默认权限模板。规格在抄完重算段落后加「不把仅职级变化列为触发条件」，直接否定同锚点。现行 `PUT /api/v1/access/users/{id}/staff-level`（AccessController:262）在已定岗时调用 `assignUserPositionDirect`（AccessService:1305-1313）按新职级岗位映射重算。首次正常改职级就会撞上。**冲突，立项 GR-01。**

❓ **Q2 — 「所有对象继续携带 project_id」是不是 HTML 原文？**
➡️ 不是。PGM-017 L413 是「所有**基础对象**都要带综合体项目**归属**」；L444 归属对象闭集为商户、空间、合同、员工、设备、票务和看板。规格 L764/L2717 写成技术列名 `project_id` 且「继续携带」像已落地。V427 主体表仍无该列，用 `business_entity_projects` 多对多；绑定设计 V0.13 L38/L56 明文。同文件 L849 已废止把所属项目扩成全部业务对象必填。首次按规格去给 `business_entities` 加 `project_id` 列会与现行模型冲突。**细化+假完成，立项 GR-02。** 不把集团 PRD 的强制句扩进本模块当新需求源。

❓ **Q3 — 业态列表「编号」是否 009 未完成同根？**
➡️ 是活句，不是变更日志。009 最小处置只废止字段字典 L923「业态编号」行。页面清单 L232 与低保真 L2415 仍写「业态名称、编号、说明」。ENUM 业态只有六值名称。按页面清单实现会重新做出编号列。**细化，立项 GR-03。** 不重开 M014-009 号。

❓ **Q4 — 要不要把 019/020 重编号？**
➡️ 不要。019 仍是 `merchant-space-contract-model-v0.1.md` 跨模块，归 M261。020 含 §24.3 职级算术、BEQR-148 收费模板未进字段表、经销页 HTML 沉默。本次核验这些事实仍在，只登记观察。

❓ **Q5 — V392 六类授权申请是否本规格漏写？**
➡️ 否。V392 CHECK 六值是 `access_grant_requests.request_type` 实现（user_role/user_position/temporary_permission/role_permission/user_scope_change/position_high_risk_release），不是 HTML 五类高危权限。规格 L1976 五类对齐 PGM-041 L660。代码不是新业务规则。不立项。

❓ **Q6 — `POST /permission-templates/{id}/copy` 还在，是否规格回灌复制模板？**
➡️ 否。规格已废止复制模板种类。入口在 access 实现模块。read-only 追依赖只为核文档声称；不把后台改进纳入本模块队列。

❓ **Q7 — L562「见 §14.4」是否坏引用？**
➡️ 合并文件中第一个 §14.4 是角色权限关键操作（L589），字段字典设备状态是第二个 §14.4（L1182）。但 L562 冒号后已内联七态成员，首次阅读不丢成员。不升 finding；观察。

❓ **Q8 — L2128「统一按 §12A.6 执行」自指？**
➡️ 是从字段字典 §14.13 抄来的。规则紧跟在下。不升 finding；ponytail 可删该自指句。

❓ **Q9 — HTML 本身要不要改？**
➡️ 不要。PGM-041/042 与 ENUM 职级一致。缺陷在规格加的排除句和假完成句。

❓ **Q10 — 极端并发/猜想？**
➡️ 本批三条都是首次正常按文档做（改职级、按「所有对象 project_id」建列、按业态列表做编号列）可复现，不是极端并发。

## 6. 临时 findings（四分类顺序：冲突→细化→技术性→添加）

### GR-M014-B1-01 — 活句把「仅职级变化」排除出权限重算，与 HTML 角色装配/职级规则及现行入口相反

- 文件:行：`docs/platform-governance-product-spec-v0.2.md:1979`（流程图 F9「强制补充规则」末句）；同根活句 `:1990`（异常「职级与敏感权限不匹配 | 阻止保存或要求高级审批」）
- 原文：`本规则不把仅职级变化、责任范围变化或任意手工模板应用列为触发条件。`
- 更长上下文（同段先抄 HTML 再加排除）：`保存岗位、职责或同一商户、同一项目范围内的所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限……在上述变更触发重算时，需保留的员工单独授权必须重新校验新范围，超出新范围则暂停并重新审批。本规则不把仅职级变化、责任范围变化或任意手工模板应用列为触发条件。`
- HTML 原文（PGM-041，`platform-governance-map-for-operations-v0.1.html:658`）：`平台管理员配置各岗位权限模板，并定「职位/层级对应哪个模板」；HR 部门定每个人的层级/职位，定完系统自动适配对应角色和权限模板，无需逐人配权限。保存岗位、职责或同一商户、同一项目范围内的所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限……`
- HTML 原文（PGM-042，同文件 `:669`）：`职级影响默认权限模板和审批层级，但不能替代功能权限点。`
- 正常触发/影响：HR/管理员对已定岗员工只改职级（`PUT /api/v1/access/users/{id}/staff-level`）。HTML 与现行 `AccessService:1305-1313` 会按新职级岗位映射重算派生角色/模板；规格排除句会让实现者关掉这条路径，或让审计把实现重算判成「超出规格」。首次正常改职级即撞上。
- 现行入口证据（类型 4）：`AccessController.java:262 @PutMapping("/users/{id}/staff-level")` → `assignStaffLevel`；`AccessService.java:1305-1313` 注释与分支「已定岗员工改职级 = 按新职级的岗位映射重算派生角色/模板，旧派生同步失效；走与定岗相同的路径」。
- 2.1 分类：**冲突**（先于细化）。与三 HTML 明文矛盾，不是实现细节。
- 2.3 依据：HTML 原文 + 可复现命令/源码行（Controller 映射与 Service 分支）。
- 冻结 ID：PGM-041、PGM-042、BEQR-149、GEN-016、GEN-002、GEN-008、GEN-017
- 工程缺陷是否成立：**是**
- 最小拟处置：删除 L1979 排除句「本规则不把仅职级变化…列为触发条件。」；重算触发与 HTML L658 对齐（岗位、职责、同商户同项目所属组织立即重算；所属商户/跨项目先失效再审批）。L1990「职级与敏感权限不匹配阻止保存」改为不按职级挡位替代功能权限点（PGM-042「不能替代功能权限点」）；敏感授出仍走 HTML 五类审批。不改 HTML，不改 AccessService（实现已对齐 HTML）。
- 不与 M014-019/020 撞号：019 是合同模型文档 XM；020 是 §24.3 审批人解析仍用职级算术（实现模块缺口）。本条是规格活规则句排除职级**重算触发**，不是审批 rank 步进。

### GR-M014-B1-02 — 两处现行句声称「所有对象继续携带 project_id」，与 HTML 归属口径、同文件废止句及 V427 主体模型不符

- 文件:行：`docs/platform-governance-product-spec-v0.2.md:764`（页面清单 §18 已确认第 11 条）；同根活句 `:2717`（低保真 §21 后续细化第 12 条）
- 原文：`集团视图与跨项目汇总下钻已由 group-governance-rebuild-prd-v1.0.md 承接落地；所有对象继续携带 project_id 并按项目归属聚合。`
- 更长上下文：同条后半 `analytics.group.view 审批属性已由 V394 置 requires_approval=true` — 后半已用 V394:16-18 核为真，不在本条最小处置里改掉。
- HTML 原文（PGM-017 `:413`）：`当前先按一个综合体落地，但所有基础对象都要带“综合体项目归属”。`
- HTML 原文（PGM-020 `:444`）：`为商户、空间、合同、员工、设备、票务和看板提供归属。`
- 同文件 L849：`所属项目 | ……【已废止/不得回灌】把所属项目扩成全部业务对象必填 | 商户、空间、合同、员工、设备、票务、看板 | 预留`
- 正常触发/影响：读者把「继续携带 project_id」当当前全对象列约束，给 `business_entities` / 绑定表补 `project_id` 列，或把未带该列的对象判不合格。现行 V427 对主体用多对多关联、表上无该列；首次按该句做迁移即与现行模型冲突。
- 现行证据（类型 4）：
  - `V427__m009_project_profiles_and_associations.sql:9` `ALTER TABLE merchants ADD COLUMN project_id`
  - 同文件 `:13-17` `CREATE TABLE business_entity_projects (business_entity_id, project_id, PRIMARY KEY …)`
  - `V005` `business_entities` 无 `project_id`
  - 绑定设计 V0.13 L38：`V427 另建 business_entity_projects 多对多项目关联表，主体表本身仍无 project_id 列`；L56：`当前表没有 project_id`
  - `PlatformController.java:88 @PutMapping("/merchants/{merchant_id}/project")`、`:97 @PostMapping("/business-entities/{entity_id}/projects")` — 关联入口存在，不是「每行已有 project_id」
- 2.1 分类：**细化**（HTML 有项目归属上位；规格把归属写成全对象 `project_id` 列且「继续」为假完成）。非添加。
- 2.3 依据：HTML 原文 + 可复现命令（读 V427/V005/Controller/绑定设计）
- 冻结 ID：PGM-017、PGM-020、GEN-016、GEN-008、GEN-002
- 工程缺陷是否成立：**是**
- 最小拟处置：两处「所有对象继续携带 `project_id` 并按项目归属聚合」改为「基础对象保留综合体项目归属（HTML：商户、空间、合同、员工、设备、票务和看板）；主体项目归属以 `business_entity_projects` 多对多关联承载（V427），不得写成每张业务表都有 `project_id` 列」。保留 V394 句。不改 HTML、不改迁移、不把后台关联接口改进纳入本模块。
- 不是 M013-021 的重编号：本模块拥有该活句；M013 只登记跨模块观察。本席独立在本文件取证。

### GR-M014-B1-03 — 业态列表仍把「编号」写成页面列，与已废止「业态编号」字段及 ENUM 六值名称闭集冲突

- 文件:行：`docs/platform-governance-product-spec-v0.2.md:232`（页面清单 §7.3）；同根活句 `:2415`（低保真 §8.1）
- 原文：`| 业态列表 | 业态名称、编号、说明 |`
- 更长上下文：L220 页目的「统一维护……六值闭集」；L927 字段字典 `【已废止/不得回灌】业态编号 | 补录已废止：ENUM「商户业态」只有六值名称，未批独立编号字段`
- HTML：ENUM 商户业态六值名称；仅演出传媒标注系统枚举码 media（锁 BEQR 业态相关 + BSM-072）。无独立编号字段。
- 正常触发/影响：按页面清单做业态管理页会做出「编号」列/录入，回灌 009 已废止字段。首次照做即复现。
- 2.1 分类：**细化**（上位=业态六值名称闭集；编号列是被废止的下位字段回潮）
- 2.3 依据：HTML 原文（六值名称）+ 同文件 L927 废止行 + 本两处活句
- 冻结 ID：BSM-072、GEN-008、GEN-016
- 工程缺陷是否成立：**是**
- 最小拟处置：L232、L2415「业态名称、编号、说明」改为「业态名称、说明」（或「业态名称、说明；无独立编号字段」）。不重开 M014-009，不写「业态代码值」新字段（009 r2 已禁止该立法）。
- 变更日志 L1316 历史行不改。

本批无技术性独立项（L562/L2128 见观察）。本批无添加项，不写 questions。

## 7. 历史 001–020 回归（只读核，不改台账）

| ID | 队列（用户口径：二十项否） | 本次核 | 回归？ |
|---|---|---|---|
| 001 空间类型去「项目」 | 否（已修） | L896/L897/L190 九类+归属根 | 无 |
| 002 V184 字典活校验 | 否 | L1295 括注八码；V184 INSERT 八码无 mall | 无 |
| 003 私人手机低风险闭集 | 否 | L2671 含看排班/考勤等，排除代客下单等 | 无 |
| 004 V394 已闭合 | 否 | L764/L2717 审批属性句为真；同条 `project_id` 是新缺陷 GR-02，不回开 004 | 无（004 范围） |
| 005 R5 现状行 | 否 | L12；ActionGateAspect 仍串三闸 | 无 |
| 006 权威回 HTML+§0A | 否 | L51 | 无 |
| 007 合同无迁移边 | 否 | L1561 | 无 |
| 008 有效权限路径 | 否 | L2745 `/api/v1/access/effective-permissions` 有映射 | 无 |
| 009 业态编号废止 | 否 | L927 已废止；L232/L2415 列表列是新证据 GR-03，不重开 009 | 无（009 字段行） |
| 010 dealer_portal 未实现 | 否 | L869；V111 仍十码 | 无 |
| 011 开关生效失效 | 否 | L2629–L2639 | 无 |
| 012 F11 入图 | 否 | L1396–L1398 | 无 |
| 013 F3 mermaid 注释 | 否 | L1549 `%%` | 无 |
| 014 admission_audits 废止 | 否 | L3340 | 无 |
| 015 一级导航分组 | 否 | L2268 | 无 |
| 016 指纹定义 | 否 | L428 现行「手机专属的设备标识」 | 无 |
| 017 V028 合规台账 | 否 | L959 | 无 |
| 018 §2999 悬空 | 否 | L338 另见 §24.3 与补录历史行 | 无 |
| 019 XM→M261 | 否 | 本模块不改 `merchant-space-contract-model-v0.1.md`；不重编号 | 保持 |
| 020 观察登记 | 否 | §24.3 算术仍真；收费模板未进表；经销页 HTML 沉默 | 保持，不伪称新需求 |

## 8. 观察（建议队列否；不升 finding）

- OBS-B1-01：§24.3.6 L2848「现网仍按职级算术与 specific_role」仍真（`ApprovalLevel.LEVEL_STEP=3`、`SPECIFIC_ROLE`、V057 CHECK）。实现缺口归 governance/review 模块。= M014-020 边界。
- OBS-B1-02：BEQR-148「其他」须指定收费模板才能生效 — 字段表未列、无相反规则。= 020。不补字段需求。
- OBS-B1-03：PGM-031 经销治理在 §0A 承接；无独立 P0 页。HTML 无该页不升。
- OBS-B1-04：`POST /api/v1/access/permission-templates/{id}/copy` 仍在；规格已废止复制模板。归 access 实现模块，不进 M014 队列。
- OBS-B1-05：V392 六类是 grant_request 类型，不是 HTML 五类高危。规格 L1976 对齐 HTML。不混写。
- OBS-B1-06：L562「见 §14.4」在合并文件中指向歧义，但七态已内联。
- OBS-B1-07：L2128 §12A.6 自指；规则在下列点。ponytail：`L2128: delete: 自指句。留下随后五条运行时规则。`
- OBS-B1-08：§24.2 字段级动态投影仍待补齐；FieldProjection* 已删。诚实待对齐。
- OBS-B1-09：L1990 与 GR-01 同根，最小处置已并入 GR-01，不另编号。
- OBS-B1-10：计划冻结 3494 vs 现行 3491。不改计划。
- OBS-B1-11：M013-019 同根句即本文件 L1979，本席按本模块活句立项 GR-01，不使用 M014-019 号。
- OBS-B1-12：补录无独立低保真来源段（020 已登记）。
- OBS-B1-13：P0 中文页名与 `adminRoutes` 不一一对应；规格自称页面清单不写接口。
- OBS-B1-14：第一联系人=主要联系人（L935 已说明）。

## 9. ponytail-review（只列可删，不落地）

- `L1979: delete: 「不把仅职级变化…列为触发条件」。HTML 已有触发闭集，排除句是加出来的立法。`
- `L764/L2717: shrink: 「所有对象继续携带 project_id」改为 HTML 归属+V427 关联事实。`
- `L232/L2415: delete: 业态「编号」列。`
- `L2128: delete: 自指「按 §12A.6 执行」。`
- net: 约 −4 活句（不含历史变更日志）。

## 10. 结论

- 全文覆盖：1 文件 3491 行七段（包装+§0A+四原文件+§24+补录）全读。
- 84 判据：定义全文+HTML 锚点 84/84 命中；适用证据见 §4。
- 三线：第一性原理 §3；顺从式 §4；对抗式 §5。
- 临时缺陷 3（冲突 1 / 细化 2 / 技术性 0 / 添加 0），工程缺陷均成立。
- 添加 0；questions 0；HTML 修改申请 0。
- 历史 001–018 无回归；019/020 不重编号、不伪新需求。
- 单席结论：**FAIL**（有未定稿缺陷）。不修、不定新队列、不等操作员。B2 由席甲在两席完整 raw 齐后归并。

本报告完成判据：独占锁行已独立实算；覆盖表完整；84 条均有适用证据；三线落盘；每条 GR 含行号/原文/上下文/触发/最小处置/四分类/工程缺陷/冻结 ID/四类依据之一；未写 §7 以外产物。


## M014 重开B2整批归并复核（Grok 乙席）

- 文件自身版本：V0.05（本段追加；上方 V0.01–V0.04 前缀 145262 bytes 原样保留，不覆盖、不改写 B1 锁段起点；本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，grok-4.6/xhigh，独立）。步骤：M014 重开 B2 整批候选复核。不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（M014 FILES 1 / 判据 84 / 计划冻结 3494 行 OVERSIZE-SINGLE-FILE）
- 正本：`/home/james/complex-management` main 工作区。`HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`。本模块 1 文件与 B1 覆盖表全等（V0.06，3491 行，226970 bytes，SHA256 `dfb40482f22455c27071990d09b7a624248f3604d186a5b1293fb0ebb84f7396`）。
- 操作员续作：「从 M009 重新独立审计，有问题按 V0.17 完整双席流程」。B1 两席均 FAIL，现进 B2。历史 B3 累计 1 不清零，本次尚未 B3。
- 席甲：Claude（作者自审票）；本席 FAIL 具否决权。本票不照抄席甲 B2，不把席甲 V0.05 B2 结论当输入。
- 本席 B2：**PASS**（021/022/023 逐 ID PASS；020 续记否 PASS；001–020 保持否 PASS。整批同意候选。）
- 无 MCP、无子代理、无第三席、不问操作员。未改源码/测试/HTML/lock/findings/ledger。未 fetch/commit/push/merge/deploy。未写新产物。只追加本指定路径。

此票只判断 findings.md V2.39 顶部「当前 M014 重开B2整批候选」相对本次两份 B1 V0.04 追加段的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.04 B1 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M014-b1-grok-raw.md` 前缀（本席 B1 重开追加后、本段追加前） | V0.04 | 145262 | `2c8d9ffc56ecda4499ec37517c38d777eecc33539538e32df24e91277733dfd7` |
| `docs/audit-2026-09/modules/M014-b1-self-raw.md` V0.04 前缀（席甲 B1 重开追加段；截去其后 V0.05 B2，不读该结论） | V0.04 | 51926 | `0dbcb356eddf5e3f0d640226b71e73dc3135b9fb1ffb8a15cd4f05d5a3b40e83` |
| `docs/audit-2026-09/findings.md` 顶部「当前 M014 重开B2整批候选」 | V2.39 | 965150 | `c3a1944cf8e937206108dc87177a79f07f4a2c9315aee8355d7683487a002271` |

锁文件只读实算（本段不写独占标签行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar 文件正文同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。未改锁。

三 HTML 只读、未改（与 B1 V0.04 同）：ENUM `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`；BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`；PGM `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1`。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核 HTML/DEC/M013 已落地句/源码行再接受或反驳；禁止表演性同意；B2 不修复。本席 B1 拟整句删除 021 排除句，本节点按 DEC-R8-91 与 M013-019 落地句改判 |
| grilling | 只借追问树自查五问（021 保留 vs 整删、1990 是否立法、022 A vs B、023 是否重开 009、差集/11 处/变更日志），不问操作员 |
| verification-before-completion | 声称前先跑哈希、11 处 `count==1`、点读 PGM/DEC/AccessService/V427/变更日志首行 |
| ponytail-review | 只评最小删除/收窄，不落地。022 A 删句 < B 扩写 V427；021 只删冲突排除项、保留 DEC 已裁手工模板项 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（规格活句→HTML/DEC→现行入口）；不改生产代码 |
| graft | 文档/审计产物不走 graft 索引；源码按既有行号直接读 AccessService/AccessController/V427 |
| tdd / ponytail 落地 / requesting-code-review | B2 禁止修复，未落测试、未改源 |

缺口（不冒称）：`batch-grill-me` / `ubiquitous-language` 本机无独立技能。grilling 本体「等用户回答 / 派子代理」作废。

### 完整差集（两席 B1 V0.04 全部条目 → 候选）

| 来源 | 候选归属 | 本席核实 |
|---|---|---|
| CX-M014-B1-01 | 新 M014-021，冲突，是 | 合取 GR-01；成立 |
| GR-M014-B1-01 | 同 021 | 含 :1990；成立 |
| 席甲 O-M014-B1-02（:1990） | 并入 021，不进 020 | 本席 B1 已并入 GR-01（OBS-B1-09）；候选按此收 |
| CX-M014-B1-02 | 新 M014-022，细化，是 | 合取 GR-02；成立 |
| GR-M014-B1-02 | 同 022 | 成立；最小修法改采 A，见下 |
| GR-M014-B1-03 | 新 M014-023，细化，是 | 席甲 B1 漏审；:232/:2415 各 1 处活句、:927 已废止、ENUM 六值名称；接受 |
| 席甲 O-M014-B1-01 | M014-020 续记，否 | 带日期核验记录；实现缺口归 review 模块 |
| OBS-B1-01 | 同 020（与 O-01 同根） | 正确 |
| OBS-B1-02…08、10、12…14 | 020 续记，否 | 正确；均非本模块活规则缺陷 |
| OBS-B1-09 | 021（:1990），不进 020 | 候选 020 正文未重复该行 |
| OBS-B1-11 | 本文件 L1979 由 021 收；他文件同根句在跨模块观察 | 不占用 020 当未修项 |
| 历史 M014-001–018 | 保持否 | 本席 B1 §7 无回归；候选未重开 |
| M014-019（XM→M261） | 保持否 | 不重编号 |
| M014-020 | 续记否 | 不伪称新需求 |

无漏并。无本批添加项。无 HTML 修改申请。无 questions。未加判据。

### 一、021 独立裁定（冲突／是）

**排除句：保留「本规则不把任意手工模板应用列为触发条件。」——同意候选，改判本席 B1 整句删除。**

核据（本节点实读，不复述旧票）：

- 现行活句仅 L1979 一处（「仅职级变化」「责任范围变化」全文各 1 次）。末句：「本规则不把仅职级变化、责任范围变化或任意手工模板应用列为触发条件。」无 DEC 括注。
- PGM L658 角色装配：HR「定每个人的层级/职位，定完系统自动适配对应角色和权限模板」；同 li 触发闭集为岗位、职责、同商户同项目所属组织、所属商户、跨项目。未把「仅职级变化」写成不触发。
- PGM L669：「职级影响默认权限模板和审批层级，但不能替代功能权限点。」
- `AccessController.java:262` `PUT /users/{id}/staff-level` → `AccessService.java:1305–1313` 已定岗改职级走 `assignUserPositionDirect` 按新职级岗位映射重算。
- DEC-R8-91（`docs/prd-audit/business-decision-ledger.md:1936`）原文：「本裁决不扩展到未点名的“职级”或任意手工模板应用」。无「责任范围」。职级排除已被 HTML + 现行入口 + M013-019 按冲突删除；**手工模板应用不触发是 DEC 点名保留的剩余项**，不是本批新发明。
- M013-019 已落地（本席点读现行文，非台账转述）：集团 PRD L76、平台治理 PRD L407 / L656 均为「本规则不把任意手工模板应用列为触发条件（DEC-R8-91）。」只删「仅职级变化」「责任范围变化」，保留手工模板项。本文件同根句未随，故立 021，不重开 M014-019 号。

整句删除（本席 B1 拟处置）会同时丢掉 DEC 已裁、M013-019 已在两份 PRD 落地的手工模板排除，造成语料分裂。HTML 触发闭集本就不含「任意手工模板应用」；留下该排除句是收窄冲突项，不是添加业务。ponytail：`L1979: shrink: 只删「仅职级变化、责任范围变化或」。留下「任意手工模板应用」不触发。`

**L1990 改法：同意。与 PGM L669 同向，不发明新规则。**

- 现行：`| 职级与敏感权限不匹配 | 阻止保存或要求高级审批 |` 全文 1 处，属 F9 §11.5。
- 「阻止保存或要求高级审批」把职级挡位写成功能权限闸，与 L669「不能替代功能权限点」相反。席甲 B1 记观察；本席 B1 已并入 GR-01。候选并入 021 正确。
- 拟改「职级不替代功能权限点」= L669 后半句收窄复述，未否认前半「影响默认权限模板和审批层级」。
- 「敏感权限授出仍按五类高危先审批」= 同节 L1976 与 PGM L660 已有五类授出前审批，用「仍按」指向既有规则，不新造第六类、不新造职级匹配闸。
- 左列场景名保留仅为唯一替换锚；动作从发明闸改为「不适用职级替代 + 沿用五类」。不改 HTML、不改 AccessService。

分类冲突、工程缺陷成立、队列是。最小边界：L1979 末句改为「本规则不把任意手工模板应用列为触发条件。」；L1990 动作改为候选句。不在此句加 DEC 括注（现行无括注，加括注是扩写）。

本 ID：**PASS**

### 二、022 独立裁定（细化／是）

**方案 A 删句更小且不扩写；同意候选按 A 给执行文本。本席 B1 方案 B 不采用。**

核据：

- 活句 L764（页面清单 §18 第 11 条）与 L2717（低保真 §21 第 12 条）各 1 处：「所有对象继续携带 `project_id` 并按项目归属聚合」。L2992 已标【历史摘录】，不改。
- PGM L412–413 是要求：「所有基础对象都要带“综合体项目归属”」。同文件 L748 / L2700 要求句仍在，并指向 `platform-governance-prd-v0.1.md` §12 第 15 条（现行 L657 存在）。
- V427：`merchants` 补 `project_id`；`business_entity_projects` 多对多；绑定设计 V0.13 §3.1「主体表本身仍无 `project_id` 列」。L849 已废止把所属项目扩成全部对象必填。
- M013-021 定稿与落地：验收包 G7 **删**「所有对象继续携带 `project_id`」，「不另写新要求句，避免扩写」。

方案 B（本席 B1）把 HTML 对象闭集、V427 表名和「不得写成每张业务表都有 `project_id` 列」写进本规格「已确认」条，是扩写实现事实，且会再造一条须维护的「当前实现」句。方案 A 只删假完成从句，句止于「承接落地。」；V394 句因替换锚止于「聚合。」而保留。ponytail：`L764/L2717: delete: 「；所有对象继续携带 project_id 并按项目归属聚合」。要求句已在 L748/L2700 与 PRD §12.15。`

分类细化、工程缺陷成立、队列是。不改 HTML、迁移、绑定设计。

本 ID：**PASS**

### 三、023 独立裁定（细化／是）

**L232 / L2415 改为「业态名称、说明」正确，不重开 009。**

核据：

- 页面清单 §7.3 L232：`| 业态列表 | 业态名称、编号、说明 |` 下一行为「业态详情 | 业态说明、引用关系」。
- 低保真 §8.1 L2415：同列表句，下一行为「业态详情 | 说明、引用关系」（与 L232 详情行不同，故候选拆成第 7、8 两处替换）。「业态名称、编号、说明」全文恰 2 处，均覆盖。
- 字段字典 L927 已【已废止/不得回灌】业态编号；ENUM L195 商户业态只有六值名称（演出传媒括注 media），无独立编号字段。
- M014-009 最小处置只废止字段行，未改页面列。按列表实现会回灌编号列。新证据、新 ID，不重开 009，不写「业态代码值」（009 r2 已禁）。L1316 字段字典历史变更行不改。

分类细化、工程缺陷成立、队列是。详情行只作唯一上下文、不改。

本 ID：**PASS**

### 四、两席条目是否收齐

收齐，无漏并。

- 020 续记来源写成「席甲 O-01；Grok OBS-B1-01…14」是范围速记；**正文**含 OBS-01/O-01、02、03、04、05、06、07、08、10、12、13、14，**不含** :1990（O-02 / OBS-09 已进 021），**不含**「本文件 L1979 尚未立项」（OBS-11 由 021 收）。
- 跨模块观察列 021 同根余下 M421 `:67`、M439 `:601`/`:650`：本席 grep 现行文仍含「仅职级变化」活句；`:688` 为 2026-08-10 变更日志历史行，不列入待修活句，正确。他模块不在本批 B3。
- 席甲 B1 曾写「四张原文件变更日志各加一行」；候选收窄为三张（字段字典本批未改）是边界订正，不是漏并 023。

020 分类技术性/观察、工程缺陷否、队列否。只登记，不改文。本 ID：**PASS**（续记）

### 五、11 处原句 / 变更日志 / 001–020 / 业务发明

本席对现行 `docs/platform-governance-product-spec-v0.2.md` 逐条 `count`：候选 11 处原句均恰好 1 次。

| # | 锚 | 位置核实 |
|---|---|---|
| 1 | `# 平台治理产品规格 V0.06` | L1，全文 1 |
| 2 | R5 警告首行 | L8，全文 1；拟插在 L6「2026-09-11 … B3-r1」修订说明之后、R5 块之前，符合文首说明。B3-r2 命名：历史 B3=1，本次尚未 B3，下一轮为 r2 |
| 3 | L1979 排除句 | 全文 1 |
| 4 | L1990 表行 | 全文 1 |
| 5 | 「11. …聚合。」 | L764 行首句，全文 1；行其余 V394 句不在锚内，保留 |
| 6 | 「12. …聚合。」 | L2717，全文 1 |
| 7 | 业态列表+详情（详情=业态说明） | L232–233，全文 1 |
| 8 | 业态列表+详情（详情=说明） | L2415–2416，全文 1 |
| 9 | 页面清单变更日志 2026-09-11 首行 | **§20** L780，表头后第一数据行 |
| 10 | 流程图变更日志 2026-09-11 首行 | **§17** L2231，表头后第一数据行 |
| 11 | 低保真变更日志 2026-09-11 首行 | **§23** L2728，表头后第一数据行 |

字段字典 §19 首行（L1316，另一套 2026-09-11 位置列）不在 11 处内、本批不加行：**正确**（021 在流程图 §11.4/§11.5，022/023 在页面清单与低保真，字段字典本批零改）。

001–020：候选未改其分类/队列；020 仅续记观察，仍否。正确。

业务发明：本批无添加项、无 HTML 申请、无 V392 六类立法、无方案 B 的 V427 表名写入。021 保留项来自 DEC-R8-91 剩余射程，不是新需求。

### 分类 / 队列 / 允许的最小修复边界

| ID | 分类 | 工程缺陷 | 队列 | 最小边界 |
|---|---|---|---|---|
| M014-021 | 冲突 | 是 | 是 | L1979 保留手工模板排除；L1990 改动作如上；不改 HTML/AccessService |
| M014-022 | 细化 | 是 | 是 | 方案 A 两处删从句；V394 保留；L2992 不改；不写 V427 |
| M014-023 | 细化 | 是 | 是 | L232/L2415 去掉「编号」；不重开 009；不写业态代码字段 |
| M014-020 | 观察/技术性 | 否 | 否 | 只登记 |
| M014-001–019 | — | — | 否 | 不动 |

一次 B3（将为第 2 次，B3-r2）只改本模块 1 文件 V0.06→V0.07：文首一行说明 + 页面清单 §20 / 流程图 §17 / 低保真 §23 各加一行。不改三 HTML、OpenAPI、迁移、Java、锁、台账、字段字典变更日志。文档类按类型 4 最小命令举证，不强制造测试。未经双 PASS 禁止 B3。

### 本席实跑（B2，只读；无写模式）

- 锁 sha256sum：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（sidecar 正文、ledger 顶行同值）
- 输入三文件字节与 SHA：上表全等（席甲取 V0.04 前缀 51926，不读 V0.05）
- 1 文件指纹：3491 行 / 226970 / `dfb40482…7396` 与 B1 表全等
- 11 处原句 `count` 均为 1；变更日志三节首行即第 9–11 处
- 点读：PGM L658/L669/L412–413/L660；DEC-R8-91 L1936；集团 PRD L76；平台治理 PRD L407/L656/L657；AccessController:262；AccessService:1305–1313；V427 L8–16；绑定设计 §3.1；ENUM L195；规格 L1/L6/L8/L232/L748/L764/L849/L927/L1976/L1979/L1990/L2415/L2700/L2717/L2848/L2992 与 §20/§17/§23 首行
- 未跑：主仓测试、psql 写、部署、修复
- 未改 findings/ledger/锁/源/HTML/主索引

### 差集与最小修正意见

差集：无。本席 B1 与候选的唯一实质分歧是 021 整句删除 vs 保留手工模板项、022 方案 B vs A；本节点按 DEC/M013 落地句与「不另写新要求句」改判，与候选执行文本一致，不要求再改候选。

无添加项。不发 2.2。无 HTML 修改申请。无新判据。

### 结论

逐 ID：021 PASS（改判保留手工模板排除；1990 与 L669 同向、不发明）、022 PASS（A 最小）、023 PASS（不重开 009）、020 PASS（续记否）、001–019 保持否 PASS。

本席队列：**021/022/023 是（3 项）；001–020 否（020 续记）**。

本席 B2：**PASS**

同意候选整批。未经双 PASS 不得把候选当定稿，更不称模块 PASS。步骤仍 B2；M014 `IN_PROGRESS`；历史 B3=1 不清零，本次尚未 B3。不覆盖上方 V0.04 B1 **FAIL** 原文。

## M014 B2-r3 整批归并复核（Grok 乙席）

- 文件自身版本：V0.06（本段追加；上方 V0.01–V0.05 前缀 162047 bytes 原样保留，不覆盖、不改写 B1 锁段起点；本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，grok-4.6/xhigh，独立）。步骤：M014 重开 B2-r3 整批候选复核。不是 B1/B5 重审、不是修复、不是模块 PASS、不是 B3/B4。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（M014 FILES 1 / 判据 84 / 计划冻结 3494 行 OVERSIZE-SINGLE-FILE）
- 正本：`/home/james/complex-management` main 工作区。`HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`。本模块 1 文件与 B5-r2 覆盖表全等（标题 V0.07，3496 行，228454 bytes，SHA256 `f74c793e2db234b9ec458962c70e71112e1887d2285f2600c12be62bc1c45e48`，endswith `\\n`=True）。
- 操作员续作：「从 M009 重新独立审计，有问题按 V0.17 完整双席流程」。M014 B5-r2 合取 FAIL（本席 GR-M014-B5r2-01；席甲 B5-r2 票 PASS），按 V0.17 无 ID 新缺陷整批回 B2。历史 B3 累计 2 不清零，本次尚未 B3（第 3 次在 3 次上限内，无需操作员批）。
- 席甲：Claude（作者自审票）；本席 FAIL 具否决权。本票不照抄席甲、不读本次 `M014-b1-self-raw.md` V0.06 B2-r3 结论。输入是两份已封存 B5-r2 raw + 候选台账。
- 本席 B2-r3：**PASS**（024 逐项 PASS；020 续记否 PASS；001–023 保持否 PASS。整批同意候选。）
- 无 MCP、无子代理、无第三席、不问操作员。未改源码/测试/HTML/lock/findings/ledger。未 fetch/commit/push/merge/deploy。未写新产物。只追加本指定路径。

此票只判断 findings.md V2.42 顶部「当前 M014 B2-r3整批候选」相对本次两份完整 B5-r2 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2-r3 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.04 B1 **FAIL**、V0.05 重开 B2 **PASS**、B5-r2 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M014-b5-r2-grok-raw.md`（本席 B5-r2，GR-M014-B5r2-01 与观察） | V0.01 | 33526 | `0213bb82f1e26aefbda852fd3bf84d043dc0b452476ebc9727bee9a39420fa2a` |
| `docs/audit-2026-09/modules/M014-b5-r2-self-raw.md`（席甲 B5-r2 PASS 票与观察；双方已封存，本席本节点可读） | V0.01 | 11970 | `681c122bf8f67e7c2ced620611fc3fd9032242e03584995f21d458d43788105d` |
| `docs/audit-2026-09/findings.md` 顶部「当前 M014 B2-r3整批候选」 | V2.42 | 973596 | `80d1e142630654dbe804200b80d43ada0121f7c00f10dd62242da51d493208f2` |
| `docs/audit-2026-09/modules/M014-b1-grok-raw.md` 前缀（本段追加前） | V0.05 | 162047 | `473aa6aa48b28709c15195f821a0580e16c3568ec21dbf489a3c81e435f7d310` |

未读：`docs/audit-2026-09/modules/M014-b1-self-raw.md` 本次新增 B2-r3 结论（V0.06）。`ls` 见该路径存在（58526 bytes）；本席未 `read`/`cat`/`grep` 其正文。

锁文件只读实算（本段不写独占标签行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar 文件正文同值。未改锁、不加判据。`LOCK-AMEND-COUNT: 1` 沿用。

三 HTML 只读、未改（与 B5-r2 同）：ENUM `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`（308 行）；BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`（2032 行）；PGM `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1`（979 行）。

ledger 只读：M014 `IN_PROGRESS`，B3 累计 **2**。本席不改。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核规格原文/三 HTML/锁锚点/7 处 `count==1` 再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问树自查五问（024 范围漏并、处置是否最小、分类是否升冲突、变更日志落点、001–023/发明），不问操作员 |
| verification-before-completion | 声称前先跑哈希、同根 grep、7 处唯一性、点读 ENUM/PGM/BSM/§9.1–9.3/F7/补录/四份变更日志首行 |
| ponytail-review | 只评最小删除/收窄，不落地。L1851 废止前缀 < 整行删除（后者丢掉不得回灌标记，且与 L1040/L1061 同根不一致） |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（现行校验句→同文废止行→HTML 无枚举）；不改生产代码 |
| graft | 文档/审计产物不走 graft 索引；本步未运行 graft CLI（会刷新 `graft/`，超出唯一可写路径） |
| tdd / ponytail 落地 / requesting-code-review | B2 禁止修复，未落测试、未改源 |

缺口（不冒称）：`batch-grill-me` / `ubiquitous-language` 本机无独立技能。grilling 本体「等用户回答 / 派子代理」作废。

### 完整差集（两席 B5-r2 全部条目 → 候选）

| 来源 | 候选归属 | 本席核实 |
|---|---|---|
| GR-M014-B5r2-01（本席 B5-r2 FAIL；细化；建议队列是） | 新 M014-024，细化，工程缺陷是，队列是 | 成立；未改写为冲突/添加；最小处置收 L1058/L1072/L1851 三处活句，与本席 B5-r2 方向同根 |
| 席甲 B5-r2 阻断发现 | 无 | 席甲票 PASS、无新 ID。候选注「席甲 B5-r2 漏审，B2 核实成立」——本席核原文后确认缺陷成立，不因席甲漏审而降为观察 |
| 本席 B5-r2 观察（020 续记、L1852/L1853 shrink、BSM-114 锁旧摘录、019/021 跨模块） | M014-020 续记，技术性/观察，缺陷否，队列否 | 成立；只登记不改文 |
| 席甲 B5-r2 §6 观察（:2853 职级算术、021 同根余下 M421/M439） | 并入 020 续记 | 与本席 020 同向，不另开 ID |
| M014-001…023 | 保持否 | 候选表未改其分类/队列；021–023 已修转否，不重开 |

差集无漏并、无多并、无把观察升缺陷、无把本席 finding 改写成别的分类。

---

### 一、024 范围（同根 grep 与漏并）

本席对现行 `docs/platform-governance-product-spec-v0.2.md` 全文件独立 `re` 检索 `启用岗位|启用职级|不可分配员工|启用角色|岗位状态|岗位停用`，命中恰好 8 行，与候选声称逐字相同：

| 行 | 现行原句（截） | 候选处置 | 本席 |
|---:|---|---|---|
| 1040 | `选择已有岗位。【已废止/不得回灌】「必须选择启用岗位」` | 已是废止行，不改 | **同意**。再改会重写已修句 |
| 1058 | `应选择启用职级`（§9.2 默认职级校验口径） | 改 | **同意**。现行活句 |
| 1060 | `应选择启用角色`（§9.2 默认权限角色） | 不改 | **同意**。见下 |
| 1061 | `【已废止/不得回灌】是否可分配员工` … `岗位状态本规格不定义` | 已是废止行，不改 | **同意** |
| 1072 | `应为启用岗位`（§9.3 当班岗位校验口径） | 改 | **同意**。现行活句 |
| 1851 | `岗位当前不可分配员工 \| 不允许继续分配`（F7 §9.6） | 改 | **同意**。现行活句 |
| 3129 | `岗位停用 \| 不允许继续分配` | 补录历史摘录，不改 | **同意**。见下 |
| 3371 | `【已废止/不得回灌】岗位状态三态` | 已是废止行，不改 | **同意** |

页面清单 L1–L793、低保真 L2246–L2742：同一模式 **0 命中**。故本批不改 §20/§23 内容句，无漏并。

**L1060 不改**：字段是「默认权限角色」。ENUM L242 / BEQR-159 权限状态闭集为「草稿、已启用、暂停、已停用、归档」，规格 §14.9 同五值。「应选择启用角色」落在权限状态「已启用」成员上，不是岗位启用闭集。本席 B5-r2 已写「不纳入本条」。候选不改正确，不是漏并。

**L3129 不改**：该行位于 `## 继续合并补录（2026-08-28）` → 来源流程图归档（L3008 起），夹在已废止班次状态机 mermaid 残片与 L3131 废止行之间。本席 B5-r2 明确「补录 L3129 属历史摘录载体，不升现行」。022 对补录 L2992 同口径（已标历史摘录则不改）。把 024 扩到整段补录会超出本条最小边界，且 020 已登记「补录无低保真来源段 / 补录为历史摘录载体」。**不是漏并。**

更宽检索 `(启用|停用).{0,8}(岗位|职级)|(岗位|职级).{0,8}(启用|停用)|可分配员工` 未新增现行活句（L490「分配员工」是页面操作，非岗位启用门控；L1071「应为可上岗员工」走员工状态，BEQR-157，不在本条）。

三 HTML 同模式仅 PGM L363 卡片句「…和岗位状态。」一处，无启用/停用/可分配枚举。

**范围结论：只改 1058/1072/1851 三处正确，无漏并。**

---

### 二、处置写法（是否最小、是否整行删除更优）

本席 B5-r2 最小方向原文：L1058 去掉「启用」改为选择已有职级档；L1072 与 L1040 对齐为选择已有岗位；L1851 删除或改为仅用员工状态五值（停岗/离职已有 L1850）。不改 HTML、不改 AccessService、不重开 021。

候选执行文本：

| 处 | 拟改 | 相对本席方向 |
|---|---|---|
| L1058 | `应选择已有职级。【已废止/不得回灌】「应选择启用职级」` | 去掉「启用」+ 按 L1040 同根加废止前缀。保留原单元格「应选择」未改写成「选择」，比整句重写更小 |
| L1072 | `应为已有岗位。【已废止/不得回灌】「应为启用岗位」` | 与 L1040「选择已有岗位 + 废止原启用句」同根；保留原「应为」 |
| L1851 | `【已废止/不得回灌】岗位当前不可分配员工 \| 不采用——岗位状态本规格不定义，三份业务 HTML 无对应状态枚举；停岗/离职按员工状态处理` | 本席「删除或改为」的「改为」支；词汇对齐 L1061（不采用 / 岗位状态本规格不定义 / 三 HTML 无对应状态枚举）+ 指向已有 L1850 |

对照 L1040 现行：`选择已有岗位。【已废止/不得回灌】「必须选择启用岗位」`。候选 1058/1072 是同一模板，不是发明新校验。

**整行删除 L1851 是否更优？否。** 理由：

1. 同文对已废止回灌的既有修法是保留原句加 `【已废止/不得回灌】`（L1040/L1061/L3371），不是从表中物理删除。删除会丢掉 F7 异常表上的不得回灌标记，后续合并/补录更容易再次回灌。
2. L1850 已有 `| 员工停岗或离职 | 不允许加入新班次 |`；页面清单 L417、低保真 L2548–L2549、§14.7 均已按员工五值处理停岗/离职。候选第二列是指针，不是新状态机、不是新分支。
3. 删除还要另证表结构仍合法，收益只是少一行，代价是与同文件废止体例不一致。

因此 **不提供删除替代执行文本作为否决条件**。候选 L1851 废止整行 + 注，是本席「删除或改为」中与同文同根的那一支，最小、不扩写、不发明新规则。

不改 HTML、不改 AccessService、不重开 021–023、不新写岗位状态字段：与本席 B5-r2 边界一致。

---

### 三、分类细化（上位 PGM-008 / BSM-028 / BEQR-157）与 HTML L363

四分类顺序（V0.17 §2.1）：冲突 → 细化 → 技术性 → 添加。

- **冲突？** 否。冲突须与三份 HTML **明文**相矛盾。HTML 无「岗位必须启用 / 岗位停用不得分配」的正句，也无「岗位状态 = 启用/停用/归档」闭集。PGM L363 只出现概念词「岗位状态」，无取值。不能把 HTML 沉默升成明文冲突。本席 B5-r2 已走过「冲突（HTML 无启用岗位明文可矛盾）→ 细化」。
- **细化？** 是。指得出上位条目（锁内原文，本席点读）：
  - **PGM-008** 锁 `:362`（卡片 h3「员工和手机」）；同卡 p 为 **L363**：「管理员工注册、私人手机设备绑定、工作手机、企微和岗位状态。」——有概念、无枚举。支持「规格不得把未立法的岗位启用闭集写成现行校验」，不支持新写岗位三态。
  - **BSM-028** 锁 `:698`（h3「主要功能」）末条 li：「管理员工、岗位、班次、职级、权限、工作设备和员工注册审批。」——有对象、无启用门控。
  - **BEQR-157** 锁 `:240`：「员工状态 = 待入职、在职、试岗、停岗、离职」。停岗/离职已有成员；岗位启用不是该闭集的第六值，也不得另造岗位状态机替代它。
- 判据 GEN-001/008/016 与本席 B5-r2 一致。不 LOCK-AMEND。
- **不升冲突**：候选写「细化」正确。工程缺陷成立（现行校验回灌已废止门控）→ 队列「是」正确（细化 + 有工程缺陷才进队列）。
- **治理 HTML L363 是否支持本处置？** 支持。该句只提「岗位状态」概念、无枚举，与 L1061/L3371「本规格不定义 / 三 HTML 无对应状态枚举」同向；废止启用门控不是删 HTML 卡片词，也不是给 L363 补枚举。本批无 HTML 申请，正确。

BEQR-159 `:242` 权限五值含「已启用」——仅支撑 L1060 不改，不把岗位启用合法化。

---

### 四、版本 / 文首 / 变更日志 / 7 处原句唯一性

现行标题 L1：`# 平台治理产品规格 V0.07`（全文恰好 1）。拟改 V0.08。正确（本批改现行字段/流程句，须升合并文件版本）。L8 历史行仍写 V0.06→V0.07，不改历史说明——正确。

文首：唯一原句 L10 `> **⚠️ R5 业务HTML缺口审计修正标注（2026-06-18，只读审计后回填，未改代码）**`（count=1）。拟在其前插入 B3-r3（V0.07→V0.08）一行说明并保留 R5。与现行 L6（B3-r1）、L8（B3-r2）叠放体例相同。说明覆盖默认职级 / 当班岗位 / F7 三处，不把 1060/3129 写进拟改范围。正确。

变更日志落点（四原文件各有自己的 § 变更日志）：

| 原文件变更日志 | 位置 | 本批 | 理由 |
|---|---|---|---|
| 字段字典 **§19** | L1315，表头后第一数据行 L1319 = 候选原句 6 | **加一行** 2026-09-19（当前 §19 **无** 该日行，B5-r2 已记） | 活句 L1058/L1072 在 §9.2/§9.3 |
| 流程图 **§17** | L2230，表头后第一数据行 L2234 = 候选原句 7 | **加一行** 2026-09-19（已有 F9 的 2026-09-19 行；本批另记 F7） | 活句 L1851 在 F7 §9.6 异常分支 |
| 页面清单 **§20** | L778，首行 L782 已是 022/023 的 2026-09-19 | **不加** | 页面清单无本模式命中 |
| 低保真 **§23** | L2729，首行 L2733 已是 022/023 的 2026-09-19 | **不加** | 低保真无本模式命中 |

§9.2/§9.3/F7 §9.6 节标题与候选日志「位置」列一致。两行新日志均为 5 列，与既有表头「日期/类型/位置/内容/原因/依据」同构。

7 处原句 `text.count` 均为 **1**（本席实跑）：

| # | 原句定位 | 行 |
|---|---|---:|
| 1 | `# 平台治理产品规格 V0.07` | 1 |
| 2 | R5 标注行 | 10 |
| 3 | 默认职级「应选择启用职级」整行 | 1058 |
| 4 | 当班岗位「应为启用岗位」整行 | 1072 |
| 5 | `\| 岗位当前不可分配员工 \| 不允许继续分配 \|` | 1851 |
| 6 | `\| 2026-09-11 \| 改 \| §3.1 / §5 / §6 / §7 / §9.1 修订说明 / §17 \|` | 1319（§19 首数据行） |
| 7 | `\| 2026-09-19 \| 改 \| §11.4 F9 强制补充规则 / §11.5 异常分支 \|` | 2234（§17 首数据行） |

**正确。**

---

### 五、001–023 不动；无业务发明

- 候选表仅两行：024 拟是、020 续记否。其下「当前 M014 B4-r2双PASS后队列：001–023全部否」段未改。
- 024 为新正式 ID（历史已用到 023；findings 中 M014-024 仅出现在本候选段，非复用旧号）。
- 020 只登记（§24.3 职级算术、复制模板入口、收费模板、经销无独立 P0、§24.2 投影、§12A.6 自指、3494 vs 3496、补录无低保真来源、P0 页名、L1852/L1853 shrink、BSM-114 锁旧摘录 vs 现行 HTML L1865）。不改文、不 LOCK-AMEND。正确。
- 本批无添加项，不发 2.2；无 HTML 申请；`questions/M014*` 无。L1851 注不引入新枚举/新流程/新默认值。「已有职级/已有岗位」是既有关联选择（L1041 职级 M1–M10；L1040 已有岗位），不是新业务概念。

### 分类 / 队列 / 允许的最小修复边界

| ID | 分类 | 工程缺陷 | 队列 | 最小边界 |
|---|---|---|---|---|
| M014-024 | 细化 | 是 | 是 | 只改 L1058/L1072/L1851 三处活句 + 版本 V0.07→V0.08 + 文首一行 + 字段字典 §19 一行 + 流程图 §17 一行；L1060/L3129/L1040/L1061/L3371 不改；不改 HTML/AccessService；不重开 021–023；不新写岗位状态字段 |
| M014-020 | 观察/技术性 | 否 | 否 | 只登记 |
| M014-001–019、021–023 | — | — | 否 | 不动 |

一次 B3（将为第 3 次，B3-r3；在 3 次上限内）只改本模块 1 文件。不改三 HTML、OpenAPI、迁移、Java、锁、台账。文档类按类型 4 最小命令举证，不强制造测试。未经双 PASS 禁止 B3。

### 本席实跑（B2-r3，只读；无写模式）

- 锁 sha256sum：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- 输入三文件字节与 SHA：上表全等（findings V2.42 / 973596 / `80d1e142…08f2`）
- 1 文件指纹：3496 行 / 228454 / `f74c793e…5e48` 与 B5-r2 表全等
- 同根 grep 8 命中；页面清单/低保真 0 命中；三 HTML 仅 PGM L363
- 7 处原句 `count` 均为 1
- 点读：规格 L1/L8/L10/L1040/L1041/L1049/L1058/L1060/L1061/L1071/L1072/L1850/L1851/L3129/L3371、§14.7/§14.9、§19 L1319、§17 L2234、§20 L782、§23 L2733；ENUM L232/L240/L242；PGM L363；BSM L698 块内「管理员工、岗位、班次…」；锁 PGM-008/BSM-028/BEQR-157/BEQR-159 原文
- 未跑：主仓测试、psql 写、部署、修复
- 未改 findings/ledger/锁/源/HTML/主索引
- 未读本次 self b1 V0.06

### 差集与最小修正意见

差集：无。本席 B5-r2 与候选的唯一表述差是 L1851「删除或改为」vs 候选「整行废止 + 注」；本节点判定整行删除不更优，不要求改候选。L1058/L1072 加废止前缀比本席当时「只去掉启用」更贴近 L1040 同根，仍属最小、未扩写。

无添加项。不发 2.2。无 HTML 修改申请。无新判据。

### 结论

逐 ID：024 PASS（细化、队列是、三处活句、不改 1060/3129/已废止行）、020 PASS（续记否）、001–023 保持否 PASS。

本席队列：**024 是（1 项）；001–023 否（020 续记）**。

本席 B2-r3：**PASS**

同意候选整批。未经双 PASS 不得把候选当定稿，更不称模块 PASS。步骤仍 B2；M014 `IN_PROGRESS`；历史 B3=2 不清零，本次尚未 B3。不覆盖上方 B1 FAIL / 重开 B2 PASS / B5-r2 FAIL 原文。
## M014 B2-r4 整批归并复核（Grok 乙席）

- 文件自身版本：V0.07（本段追加；上方 V0.01–V0.06 前缀 181023 bytes 原样保留，不覆盖、不改写 B1 锁段起点；本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，grok-4.6/xhigh，独立）。步骤：M014 重开 B2-r4 整批候选复核。不是 B1/B5 重审、不是修复、不是模块 PASS、不是 B3/B4。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（M014 FILES 1 / 判据 84 / 计划冻结 3494 行 OVERSIZE-SINGLE-FILE）
- 正本：`/home/james/complex-management` main 工作区。`HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`。本模块 1 文件与 B5-r3 覆盖表全等（标题 V0.08，3500 行，229864 bytes，SHA256 `f5557682f6a6872c2464630ce493101e45c3f014b0fc9e2d593ae5b83c1ec92f`，endswith `\n`=True；`git hash-object`=`cdb8cfda6ac8feb8b35fa3f8d6d7e8fc094400e3`）。
- 操作员续作：「从 M009 重新独立审计，有问题按 V0.17 完整双席流程」。M014 B5-r3 两席均 FAIL（本席 GR-M014-B5r3-01；席甲 CX-M014-B5r3-01；同根：班次「异常」状态门控回灌已废止规则）。B3 已 3 次上限记 BLOCKED-3ROUNDS；操作员 2026-09-19 批准追加一次修复，现回 B2-r4。历史 B3 累计 3 不清零，本次尚未 B3（获批后的第 4 次）。
- 席甲：Claude（作者自审票）；本席 FAIL 具否决权。本票不照抄席甲、不读本次 `M014-b1-self-raw.md` 新增 B2-r4 结论（V0.07）。输入是两份已封存 B5-r3 raw + 候选台账。
- 本席 B2-r4：**PASS**（025 逐项 PASS；020 续记否 PASS；001–024 保持否 PASS。整批同意候选。）
- 无 MCP、无子代理、无第三席、不问操作员。未改源码/测试/HTML/lock/findings/ledger。未 fetch/commit/push/merge/deploy。未写新产物。只追加本指定路径。

此票只判断 findings.md V2.46 顶部「当前 M014 B2-r4整批候选」相对本次两份完整 B5-r3 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2-r4 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL**、V0.04 重开 B1 **FAIL**、V0.05 重开 B2 **PASS**、B5-r2 **FAIL**、V0.06 B2-r3 **PASS**、B5-r3 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M014-b5-r3-grok-raw.md`（本席 B5-r3，GR-M014-B5r3-01 与观察） | V0.01 | 33768 | `682e21dbd16f4353451536b65ed8db79963ccc4a461fe7bd13981a66ac991082` |
| `docs/audit-2026-09/modules/M014-b5-r3-self-raw.md`（席甲 B5-r3 FAIL 票 CX-M014-B5r3-01 与观察；双方已封存，本席本节点可读） | V0.01 | 18292 | `e04ab20c925b5e05e2d4ed63781798b6f9bfa5acf531bf56f19649ea147b9146` |
| `docs/audit-2026-09/findings.md` 顶部「当前 M014 B2-r4整批候选」 | V2.46 | — | `25527282d85b60fd7d02a1b1def0b53de8f35da6a7712fae565bcc2deebad21e`（全文件） |
| `docs/audit-2026-09/modules/M014-b1-grok-raw.md` 前缀（本段追加前） | V0.06 | 181023 | `62a8e491288f8a4626e0c17860281825ce3c3e3a1a349a17beaf2c6cd9517649` |

未读：`docs/audit-2026-09/modules/M014-b1-self-raw.md` 本次新增 B2-r4 结论（V0.07）。`ls` 见该路径存在（61878 bytes）；本席未 `read`/`cat`/`grep` 其正文。

锁文件只读实算（本段不写独占标签行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar 文件正文同值。未改锁、不加判据。`LOCK-AMEND-COUNT: 1` 沿用。

三 HTML 只读、未改（与 B5-r3 同）：ENUM `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`（308 行）；BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`（2032 行）；PGM `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1`（979 行）。

ledger 只读：M014 `IN_PROGRESS`，B3 累计 **3**。本席不改。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核规格原文/三 HTML/锁锚点/10 处 `count==1` 再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问树自查五问（写入集合漏并/过并、处置是否最小、分类是否升冲突、变更日志落点、001–024/发明），不问操作员 |
| verification-before-completion | 声称前先跑哈希、同根 grep、10 处唯一性、点读 ENUM/PGM/BSM/§11.3–11.5/§9.3/§14.1–14.2/F7 mermaid/补录/四份变更日志首行 |
| ponytail-review | 只评最小删除/收窄，不落地。496/1078/2573 标废止 < 整行物理删除（后者丢掉不得回灌标记）；484/2565 去列 < 改写成别的列 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（现行门控句→同文废止行→HTML 无枚举）；不改生产代码 |
| graft | 文档/审计产物不走 graft 索引；本步未运行 graft CLI（会刷新 `graft/`，超出唯一可写路径） |
| tdd / ponytail 落地 / requesting-code-review | B2 禁止修复，未落测试、未改源 |

缺口（不冒称）：`batch-grill-me` / `ubiquitous-language` 本机无独立技能。grilling 本体「等用户回答 / 派子代理」作废。

### 完整差集（两席 B5-r3 全部条目 → 候选）

| 来源 | 候选归属 | 本席核实 |
|---|---|---|
| GR-M014-B5r3-01（本席 B5-r3 FAIL；细化；建议队列是；写入 :496/:1078/:2573 与同根列表列 :484/:2565） | 新 M014-025，细化，工程缺陷是，队列是；五处全部收入 | 成立；未改写为冲突/添加 |
| CX-M014-B5r3-01（席甲 B5-r3 FAIL；细化；建议队列是；写入仅 :1078/:2573；:496 视为记录动作、未列 :484/:2565） | 并入 025；候选注席甲 B2 核实后接受 :496/:484/:2565 | 本席核原文后确认三处同根，收入不是过并（见下一节）；不因席甲 B5 写窄而降为观察 |
| 席甲 B5-r3 对 :1078 的最小方向（校验改「可为空」+「条件必填」改「选填」，保留字段） | 候选取整行按 :1077/:1063 废止（本席 B5-r3「去掉必填**或**整行随班次状态废止」的后一支） | 见第二节：整行废止仍属最小、且比「选填」更能收掉用途句「记录异常班次原因」；不升否决 |
| 本席 B5-r3 观察（020 续记、L1856/L1857 shrink 行号、BSM-114 锁旧摘录、设备异常说明排除） | M014-020 续记，技术性/观察，缺陷否，队列否 | 成立；shrink 现行行是 L1855/L1856（本席 B5-r3 写成 L1856/L1857 偏一行），候选 :1855/:1856 与现行文件一致 |
| 席甲 B5-r3 §6 观察（即将到期/待审核注册/保存草稿、:2853 职级算术、021 同根余下 M421/M439） | 并入 020 续记 | 与本席 020 同向，不另开 ID |
| M014-001…024 | 保持否 | 候选表未改其分类/队列；024 已修转否，不回退、不重开 |

差集无漏并、无多并、无把观察升缺陷、无把本席 finding 改写成别的分类。

---

### 一、写入集合（五处收入 / 排除项）

本席对现行 `docs/platform-governance-product-spec-v0.2.md` 全文件独立检索 `异常班次|班次异常|班次为异常|记录异常|异常说明|班次状态|异常记录`，并点读列表「状态」列。

**「异常班次」现行活句恰好 3 处，候选全收：**

| 行 | 现行原句（截） | 候选 | 本席 |
|---:|---|---|---|
| 496 | `- 记录异常班次`（§11.4 关键操作，与「标记上岗/标记下班」并列） | 改 | **同意收入**。不是「记录班次中的异常事项」；用语即「异常班次」，与 :1078/:2573/:2959 同词 |
| 1078 | `\| 异常说明 \| 记录异常班次原因 \| 条件必填 \| … \| 班次为异常时必填 \|` | 改 | **同意收入**。两席 B5-r3 均列 |
| 2573 | `- 异常班次必须填写异常说明。` | 改 | **同意收入**。两席 B5-r3 均列 |
| 2959 | `【已废止/不得回灌】异常班次原因规则——三 HTML 无班次状态机。` | 不改（补录已废止） | **同意排除** |

**班次列表「状态」列现行活句恰好 2 处，候选全收：**

| 行 | 现行原句 | 候选 | 本席 |
|---:|---|---|---|
| 484 | `\| 班次列表 \| 班次时间、员工、岗位、区域、状态 \|` | 改 | **同意收入**。与 L1265「【已废止/不得回灌】班次状态作为页面核心字段」直接并立 |
| 2565 | `\| 班次列表 \| 日期、开始时间、结束时间、员工、岗位、区域、状态 \|` | 改 | **同意收入**。低保真对页 |

「班次」且「状态」的其余命中：L501/L1077/L1211/L1265/L1844 为不定义/已废止声明；L552/L2599「领还记录｜领用人、班次、时间、状态」是设备领还记录列，不是班次状态机；补录 L2955–L3451 / changelog 引述。**未漏列。**

**排除项逐条（本席独立，不只复述）：**

| 处 | 现行 | 排除理由 | 本席 |
|---|---|---|---|
| :485 | `班次详情｜…、异常记录` | 详情记录项，无「班次为异常/必填/状态」门控 | **同意**。与 485 同类的 :521/:2323/:2585 亦是记录/留痕，不在本条 |
| :2566 | `班次详情｜…、异常说明` | 低保真详情列，无必填/门控 | **同意排除、非漏并**。:1078 整行「不采用」后，此处仍可作设备未归还等事项的展示位（与 :485/:2567 同族）；扩改会发明新列名或删掉非门控记录面。本席 B5-r3 未写入 |
| :2567 | `交接信息｜…、异常记录` | 交接记录项 | **同意** |
| :2574 | `班次结束前提示未归还设备或未处理异常。` | 提示句；HTML BSM :1588 有「设备未归还」明文 | **同意**。不是把班次标成「异常」态 |
| :1836 | mermaid `班次结束或异常处理` | 流程异常分支，可读作设备/责任链异常处理 | **同意**。本席 B5-r3 已排除；故流程图 §17 本批不加变更行 |
| :1908 | F8 `填写异常说明` | 设备「是否正常」分支，BEQR-158 | **同意不写入**（候选排除设备异常说明，此行同根） |
| :1099/:2606（及 :1267/:1931） | 设备异常说明/异常归还 | BEQR-158 设备七态含维修中/挂失 | **同意** |
| :2959/:3135/:3451 | 补录已废止句 | 历史摘录载体，再改会重写已修句 | **同意** |
| changelog / 文首 024 行 | 引述被删词 | 日志记录 | **同意** |

席甲 B5-r3 只列 :1078/:2573、把 :496 当记录动作——本席核 :496 原文在关键操作表、与「标记上岗/标记下班」并列，且补录 :2959 已废止的正是「异常班次原因规则」。**收入 :496 不是过并。** :484/:2565 是 L1265 废止「班次状态作为页面核心字段」后仍把「状态」写成列表核心列，与 011 同型。**收入不是过并。**

**范围结论：五处全部收入、所列排除正确，无漏并、无过并。**

---

### 二、处置写法（是否最小；整行删除是否更优）

本席 B5-r3 最小方向原文：L1078 去掉「班次为异常时必填」**或**整行随班次状态废止；L2573/L496 标【已废止/不得回灌】**或**删除；L484/L2565 列表「状态」列按 L1265 去掉。不改 HTML、不新写班次状态机、不改 StaffController、不重开 024。

候选执行文本：

| 处 | 拟改 | 相对本席方向 |
|---|---|---|
| L484 | `\| 班次列表 \| 班次时间、员工、岗位、区域 \|`（去掉「、状态」） | 去列。011 落地后开关列表 L633 现为「是否启用」而无「状态」列（HTML 有是否启用成员）；班次无 HTML 替代列，**只删「、状态」比改写成别的列更小**，不发明「是否启用/进行中」等 |
| L2565 | 同构去「、状态」 | 同上 |
| L496 | `- 【已废止/不得回灌】记录异常班次——三 HTML 无班次状态机` | 本席「标废止或删除」的标废止支；模板对齐 L213 `- 【已废止/不得回灌】设置是否可经营——三 HTML 无该必填开关` |
| L1078 | `\| 异常说明 \| 【已废止/不得回灌】记录异常班次原因及「班次为异常时必填」 \| 不采用 \| — \| 本规格不定义班次状态——三份业务 HTML 无对应状态枚举 \| — \|` | 本席「或整行随班次状态废止」支。字段名保留（对齐 L1077）、P0「不采用」（对齐 L1063）、校验口径不定义班次状态（对齐 L1077）。**不取席甲「可为空/选填」**：用途现行句就是「记录异常班次原因」= 补录 :2959 已废止规则，只改必填会留下活的「异常班次」立法 |
| L2573 | `- 【已废止/不得回灌】异常班次必须填写异常说明——三 HTML 无班次状态机。` | 对齐 L2414 `- 【已废止/不得回灌】将空间设为不可经营时提示影响——三 HTML 无该门控。`；注语对齐 :2959/:3135「三 HTML 无班次状态机」 |

**整行物理删除 496/1078/2573 是否更优？否。** 理由：

1. 同文对已废止回灌的既有修法是保留原句加 `【已废止/不得回灌】`（L213/L1063/L1077/L1854/L2414/L2959/L3135），不是从表中物理删除。删除会丢掉不得回灌标记，后续合并/补录更容易再次回灌（本模块 024 前车：废止框收了页面清单/流程图，漏了字段字典/低保真）。
2. L1078 与上一行 L1077「班次状态」已废止行同表；物理删 1078 会让「异常说明」从班次字段表消失，反而看不出它曾被当作状态门控、不得回灌。
3. L484/L2565 是去一个未定义列，不是废止整行「班次列表」——列表其余列（时间/员工/岗位/区域）有 BSM :940「确认班次、岗位、职级、责任区域和设备」上位，必须保留。

因此 **不提供删除替代执行文本作为否决条件**。候选四处（496/1078/2573 标废止，484/2565 去列）是本席 B5-r3「或」中与同文同根的那一支，最小、不扩写、不发明新规则、不新写班次状态机。

不改 HTML、不改 :485/:2566/:2567/:2574、不重开 024、不改 StaffController：与本席 B5-r3 边界一致。

---

### 三、分类细化（上位 BSM-028 / PGM-008 / BEQR-157）

四分类顺序（V0.17 §2.1）：冲突 → 细化 → 技术性 → 添加。

- **冲突？** 否。冲突须与三份 HTML **明文**相矛盾。本席对三 HTML `异常班次|班次异常|异常说明|班次状态` **零命中**。「记录异常」命中的是容量软上限「记录异常事件」（ENUM :145 / BSM :1266/:1294），不是班次。PGM :481「设备和员工班次要知道责任区域」、BSM :709「管理员工、岗位、班次…」、:940 上岗确认班次、:1443 排班规则、:1588「设备未归还、挂失、跨班次使用」、ENUM :279 考勤日结果——有班次对象、无班次五态、无「异常班次须填说明」。沉默 ≠ 明文冲突。本席 B5-r3 已走过「冲突（HTML 无班次五态明文可矛盾）→ 细化」。
- **细化？** 是。指得出上位条目（锁内原文，本席点读）：
  - **BSM-028** 锁 `:698`（h3「主要功能」）内 li（现行 HTML :709）：「管理员工、岗位、班次、职级、权限、工作设备和员工注册审批。」——有对象、无班次五态、不授权「异常」门控。
  - **PGM-008** 锁 `:362`（卡片 h3「员工和手机」）；同卡 p 为 **L363**：「管理员工注册、私人手机设备绑定、工作手机、企微和岗位状态。」——有岗位状态概念、无取值、无班次状态。
  - **BEQR-157** 锁 `:240`：「员工状态 = 待入职、在职、试岗、停岗、离职」。班次「异常」不是该闭集成员，也不得另造班次状态机替代它。
- 判据 GEN-001/008/016 与本席 B5-r3 一致。不 LOCK-AMEND。BEQR-158 支撑排除设备异常说明；BEQR-159 与本条无关（024 已处理启用角色）。
- **不升冲突**：候选写「细化」正确。工程缺陷成立（现行校验/列表列/关键操作回灌已废止门控）→ 队列「是」正确。
- 与 024 同型：废止框已收班次五态与「异常班次原因规则」，现行段漏了字段字典/低保真/页面清单列表列与关键操作。

---

### 四、版本 / 文首 / 变更日志 / 10 处原句唯一性

现行标题 L1：`# 平台治理产品规格 V0.08`（全文恰好 1）。拟改 V0.09。正确（本批改现行页面/字段/低保真句，须升合并文件版本）。L6/L8/L10 历史说明不改——正确。

文首：唯一原句现为 L12 `> **⚠️ R5 业务HTML缺口审计修正标注（2026-06-18，只读审计后回填，未改代码）**`（count=1）。拟在其前插入 B3-r4（V0.08→V0.09，操作员获批追加）一行说明并保留 R5。与现行 L6（B3-r1）、L8（B3-r2）、L10（B3-r3）叠放体例相同。说明覆盖「记录异常班次」、列表「状态」列、字段「异常说明」、低保真「异常班次必须填写异常说明」——即五处，不把 :485/:2566/:1836/设备/补录写进拟改范围。正确。

变更日志落点（四原文件各有自己的 § 变更日志）：

| 原文件变更日志 | 位置 | 本批 | 理由 |
|---|---|---|---|
| 页面清单 **§20** | L780，表头后第一数据行 L784 = 候选原句 8 | **加一行** 2026-09-19 | 活句 L484/L496 在 §11.3/§11.4 |
| 字段字典 **§19** | L1317，表头后第一数据行 L1321 = 候选原句 9 | **加一行** 2026-09-19 | 活句 L1078 在 §9.3 |
| 低保真 **§23** | L2733，表头后第一数据行 L2737 = 候选原句 10 | **加一行** 2026-09-19 | 活句 L2565/L2573 在 §14.1/§14.2 |
| 流程图 **§17** | L2233，首行 L2237 已是 024 的 2026-09-19 | **不加** | 本批不改 F7 mermaid :1836、不改 F8 :1908；无流程图活句进入写入集合 |

§11.3/§11.4/§9.3/§14.1/§14.2 节标题与候选日志「位置」列一致。三行新日志均为 5 列，与既有表头「日期/类型/位置/内容/原因/依据」同构。

10 处原句 `text.count` 均为 **1**（本席实跑）：

| # | 原句定位 | 行 |
|---|---|---:|
| 1 | `# 平台治理产品规格 V0.08` | 1 |
| 2 | R5 标注行 | 12 |
| 3 | `\| 班次列表 \| 班次时间、员工、岗位、区域、状态 \|` | 484 |
| 4 | `- 记录异常班次` | 496 |
| 5 | `\| 异常说明 \| 记录异常班次原因 \| 条件必填 \| 多行文本 \| 班次为异常时必填 \| 变更需留痕 \|` | 1078 |
| 6 | `\| 班次列表 \| 日期、开始时间、结束时间、员工、岗位、区域、状态 \|` | 2565 |
| 7 | `- 异常班次必须填写异常说明。` | 2573 |
| 8 | `\| 2026-09-19 \| 改 \| §7.3 / §18 后续细化 11 \|` | 784（§20 首数据行前缀） |
| 9 | `\| 2026-09-19 \| 改 \| §9.2 岗位字段 / §9.3 班次字段 \|` | 1321（§19 首数据行前缀） |
| 10 | `\| 2026-09-19 \| 改 \| §8.1 / §21 后续细化 12 \|` | 2737（§23 首数据行前缀） |

**正确。**

---

### 五、001–024 不动；024 落地不回退；无业务发明

- 候选表仅两行：025 拟是、020 续记否。其下「当前 M014 B4-r3双PASS后队列：001–024全部否」段未改。
- 025 为新正式 ID（历史已用到 024；findings 中 M014-025 仅出现在本候选段，非复用旧号）。
- 024 落地独立复验：L10 说明行、L1042/L1060/L1074 废止启用岗位/职级、L1854 废止「岗位当前不可分配员工」仍在；`启用岗位|启用职级|不可分配员工` 无未废止现行校验句。L1062「应选择启用角色」保留（BEQR-159）。**不回退。**
- 020 只登记（续记仍真；:1855/:1856 shrink；BSM-114 锁旧摘录 vs 现行 HTML :1865 不 LOCK-AMEND；即将到期/待审核注册/保存草稿观察）。不改文。正确。
- 本批无添加项，不发 2.2；无 HTML 申请；`questions/M014*` 无。拟改不引入新枚举/新流程/新默认值/新班次状态成员。「标记上岗/标记下班」仍为动作（HTML 有排班与领取工作手机打卡），不是五态，本批不改——正确。

### 分类 / 队列 / 允许的最小修复边界

| ID | 分类 | 工程缺陷 | 队列 | 最小边界 |
|---|---|---|---|---|
| M014-025 | 细化 | 是 | 是 | 只改 L484/L496/L1078/L2565/L2573 五处活句 + 版本 V0.08→V0.09 + 文首一行 + 页面清单 §20 一行 + 字段字典 §19 一行 + 低保真 §23 一行；流程图 §17 不加；:485/:2566/:2567/:2574/:1836/:1099/:2606/补录已废止行/changelog 不改；不改 HTML；不新写班次状态机；不重开 024 |
| M014-020 | 观察/技术性 | 否 | 否 | 只登记 |
| M014-001–019、021–024 | — | — | 否 | 不动；024 已修不回退 |

一次 B3（将为第 4 次，B3-r4；3 次额度 + 操作员 2026-09-19 获批 1 次）只改本模块 1 文件。不改三 HTML、OpenAPI、迁移、Java、锁、台账。文档类按类型 4 最小命令举证，不强制造测试。未经双 PASS 禁止 B3。

### 本席实跑（B2-r4，只读；无写模式）

- 锁 sha256sum：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- 输入：B5-r3 grok 33768/`682e21db…1082`；B5-r3 self 18292/`e04ab20c…9146`；findings V2.46 全文件 `25527282…d21e`；本文件前缀 181023/`62a8e491…7649`
- 1 文件指纹：3500 行 / 229864 / `f5557682…c92f` 与 B5-r3 表全等
- 同根 grep：异常班次活句 3；班次列表状态列 2；排除项均存在且非门控
- 10 处原句 `count` 均为 1
- 点读：规格 L1/L10/L12/L213/L484/L485/L496/L501/L633/L1042/L1060/L1063/L1074/L1077/L1078/L1099/L1211/L1265/L1321/L1836/L1844/L1854/L1855/L1856/L1908/L2237/L2414/L2565/L2566/L2567/L2573/L2574/L2606/L2737/L2959/L3135/L3451；ENUM L240/L241/L242/L279；PGM L363/L481；BSM L709/L940/L1443/L1588/L1865；锁 PGM-008/BSM-028/BEQR-157/BEQR-158
- 三 HTML 对「异常班次/班次异常/异常说明/班次状态」零命中
- 未跑：主仓测试、psql 写、部署、修复
- 未改 findings/ledger/锁/源/HTML/主索引
- 未读本次 self b1 V0.07

### 差集与最小修正意见

差集：无。本席 B5-r3 与候选的表述差是 L1078「去掉必填或整行废止」vs 候选「整行废止」——本节点判定整行废止更贴 L1077/L1063/L2959 同根，选填会留下用途句「记录异常班次原因」，不要求改候选。L496/L2573「标废止或删除」vs 候选「标废止」——整行物理删除不更优，不要求改候选。席甲 B5-r3 写窄（只 :1078/:2573）由本席独立核原句后确认应扩到五处，候选已扩，不是过并。

无添加项。不发 2.2。无 HTML 修改申请。无新判据。

### 结论

逐 ID：025 PASS（细化、队列是、五处活句、排除项不改）、020 PASS（续记否）、001–024 保持否 PASS。

本席队列：**025 是（1 项）；001–024 否（020 续记）**。

本席 B2-r4：**PASS**

同意候选整批。未经双 PASS 不得把候选当定稿，更不称模块 PASS。步骤仍 B2；M014 `IN_PROGRESS`；历史 B3=3 不清零，本次尚未 B3（获批后的第 4 次）。不覆盖上方 B1 FAIL / 重开 B2 PASS / B5-r2 FAIL / B2-r3 PASS / B5-r3 FAIL 原文。
CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M014 B1 Grok 独立审计原文（现锁整模块重开追加）

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；席甲=Codex；本席 FAIL 具否决权）
- 阶段：B1 修复前整模块全量审计（现锁重开；旧 PASS 已回 PENDING→IN_PROGRESS，由调度写入，本席只读）
- 报告自身版本：V0.05（指定路径旧前缀 204338 bytes / SHA256 `5d6beb8c5720ff141eb3c71b651d93304a0f267700eec3b6718cc017b961067d` / 旧 V0.04 完整保留，本次追加；不覆盖）
- 模块：M014 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第2段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 1 文件，计划冻结有效内容行 3494，标记 OVERSIZE-SINGLE-FILE；当前 `git ls-files -z -- docs/platform-governance-product-spec-v0.2.md | xargs -0 cat | wc -l` = **3505**（与用户固定清单一致；计划 3494 为切分时冻结，B3-r1…r4 已增行，本席按现行全文审）
- 冻结判据：84 条（`M014 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-189, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-060, PGM-065, PGM-066]`，与锁映射行 L6399 逐字相同；COUNT=84 unique=84 MISSING=[] EXTRA=[]）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M014-T01 … GR-M014-T08、GR-M014-T09、GR-M014-T10（正式 ID 仅 B2 分配，不抢 M014-026 起）
- 添加项（B1 只登记，不代用户回答 2.2）：GR-M014-T03
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引/findings、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本次 self-raw、未问操作员、未对任一 `complex_test*` 库做写操作、未写 `/tmp` 隔离副本或探针文件
- 旧正式 ID M014-001—025 全否：23 已修 + 019（→M261）/020（观察）登记；历史 Claude/Grok 票不代本次票。019/020 只作既有输入。历史 B3 累计 4 保持，无第 5 批授权。用户「计数清零」只作用 M013，不延用本模块
- 代码迁移及刚发布 M013 文档只证实现、不立法。本模块纯文档，不强制 Maven/数据库/造测试
- 本报告不表示全仓或发布门禁通过。M013 PR484 合并不使本模块自动 PASS

## 0. 技能自核（V0.17 §6，按 B1 收窄；本席环境实核；不安装）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 删除测试：若删掉已废止闭集回灌与过期「当前实现」句，复杂度回到 HTML 四类绑定/十一端/状态成员本身；不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（sha256/HTML 行命中/OpenAPI path/`AccessController`/`AdminApp.tsx`/mermaid 孤立节点）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（规格陈述→HTML 原句→实现事实） |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；端点存在 ≠ 规格与 HTML 一致 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推、未安装 hook |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒级，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit`、Trail of Bits `audit-context-building`：会派子代理并另写产物，**不调用**

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain、MCP、第三主体。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`
- sidecar `plan/criteria-lock.sha256` 同值
- `criteria-lock.md` 版本 V0.14；判据总数 401
- ledger 顶部只读：`CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`；`LOCK-AMEND-COUNT: 1`；版本 V9.03
- 映射行 `M014 → [...]`（L6399）与开工口径 84 项逐字相同
- `plan/modules.md` M014 FILES 1 条与用户清单逐字相同
- 1 文件 `git ls-files -z -- docs/platform-governance-product-spec-v0.2.md | xargs -0 cat | wc -l` → **3505**
- 该文件字节/SHA256 与用户固定清单 match=True：3505 行 / 231468 bytes / `37f60e779b915b80ee03438196885d188a22decf30e2ccf521007bab9d00f622`
- 工作区该文件相对 HEAD 有未提交差（`git diff --stat` 44 行：V0.06→V0.09 及 021–025 收口）；本席按磁盘现行 V0.09 全文审，不把 HEAD `dfb40482…` 当现行
- HEAD=`e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main`=`12728763626f0de83d7d828fb6a36edfb1d3be1c`（两值不等）。本席未设 `GIT_INDEX_FILE`，未动主索引

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

三 HTML 当前字节/SHA256（只读核验，本席未改）：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| docs/business-enum-quick-reference-v0.1.html | 133961 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | 304482 | eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 |
| docs/platform-governance-map-for-operations-v0.1.html | 103798 | 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f |

锁内本模块 HTML 锚点行对本席读取的现行 HTML **命中**（抽样）：BEQR-143 `:226`、BEQR-148 `:231`、BEQR-150 `:233`、BEQR-151 `:234`、BEQR-154 `:237`、BSM-004 `:566`、BSM-028 `:698`、BSM-114 `:1827`、PGM-002 `:308`、PGM-018 `:415`、PGM-026 `:492`、PGM-041 `:647`、PGM-043 `:673` 均 in_line=True。BEQR-074 锁行是 `:125`（本席核命中）。

M013 收口文件 `modules/M013-entity-topology-binding-docs-seg1.md` V0.03 只证四份邻接文档已按现锁收口；不得把其「PASS」或绑定设计 V0.14 升为本规格立法。

## 2. 全文件分段覆盖表（全文已读；行=`splitlines`=`\n` 计数 = 3505）

| 段 | 行闭区间 | 结构 | 已读 |
|---|---|---|---|
| A | 1–41 | 标题 V0.09、B3-r1…r4 说明、R5 历史/2026-09-11 现状、§0A | 是 |
| B | 45–798 | 原页面清单：分组、P0 页、全局规则、§5–15、已确认/后续、变更日志 | 是 |
| C | 800–1342 | 原字段字典：通用/终端/项目/空间/业态/商户/合同/员工岗位班次/设备/权限/留痕/状态字典 | 是 |
| D | 1345–2251 | 原流程图：F1–F11 mermaid 13 块、跨流程依赖、验收 | 是 |
| E | 2254–2750 | 原低保真：导航、各页结构、§21 已确认 | 是 |
| F | 2753–2863 | §24 有效权限集接口 / 字段投影 / 审批层级映射 | 是 |
| G | 2865–3505 | 2026-08-28 补录（`~~~~text` 历史摘录） | 是 |

未抽样宣称整文件 PASS。13 个 mermaid 块均点读。

## 3. 第一性原理三问

1. **这段东西为什么必须存在？删掉它，三份 HTML 里哪一条会做不到？**
   本文件是治理层页面清单+字段字典+流程图+低保真的合并入口。删掉后 PGM-003 模块入口、PGM-017/020 项目归属、PGM-026 四类主体与联系人、BEQR-148 合作模式、BEQR-150 十一端、BEQR-151 最终权限、PGM-041 权限重算、PGM-043 查看/导出 将失去本合并件的页面/字段/流程承接。答得上 → 有业务上位需求。不因此把 HTML 未批的财务/运营联系人、选其他缺省生效、补录未标废止活句合法化。

2. **它声称的行为，有没有被实际执行验证过？**
   纯文档：不强制 Maven/库。用类型 1/4：HTML 逐字对照、`sha256sum`、行命中、`AccessController`/`AdminApp.tsx`/`openapi-p0.yaml` 路径存在性、mermaid 孤立节点解析。§24.1「现网已落地 GET /api/v1/access/effective-permissions」：`AccessController.java:32,244-253` `@RequestMapping("/api/v1/access")` + `@GetMapping("/effective-permissions")`；`AdminApp.tsx:451` 调用该路径；`docs/openapi/openapi-p0.yaml:8098`。此为实现证据，不立法。§24.2 自承字段级动态投影待补齐，不得当完成。

3. **它依赖的前提，哪一条是没验证过就当真的？**
   - R5 L14「evaluate 零调用」已被 L18 标为 2026-06-18 过期；本席不把 L14 当现行。
   - §24 章首 L2757「③准入审批层级到职级映射在 R6 已落地」与 §24.3.6 L2862「现网仍按职级算术…不得写入产品规格」同章互斥 → 见 T07。
   - 补录引言称已废止闭集均已标注，但存在未标废止活句 → 见 T08。
   - 「最高归属」活句与 PGM-018 多对多：见观察 O1，不升 finding。
   - V184 字典码括注：实现证据，不得升为产品九类闭集（O3）。

## 4. 顺从式：84 判据适用证据

判定顺序：冲突 → 细化 → 技术性 → 添加。锁外主张不进队列。

### 4.1 GEN（18）

| ID | 结论 | 证据/不适用理由 |
|---|---|---|
| GEN-001 | **FAIL** 见 T02/T06/T08 | 主要/第一联系人重复必填；F2 孤立废止节点；补录未标废止活句 |
| GEN-002 | 适用、本模块文档豁免可执行构建 | 声称入口用类型 4 核对；§24.1 路径现存 |
| GEN-003 | **不适用** | 纯文档，不强制造测试 |
| GEN-004 | 适用（文档信任边界） | 敏感字段 §2.3；T09/T10 查看/导出承接不全 |
| GEN-005 | 适用 | 最终权限五源 L753/L2771 与 BEQR-151 一致；F9 L1989 重算与 PGM-041 角色装配现文一致（021 已删「仅职级变化排除」） |
| GEN-006 | **不适用** | 无本模块持久化实现 |
| GEN-007 | **不适用** | 无并发路径 |
| GEN-008 | **FAIL** 见 T01/T04/T07 | 低保真联系人 vs HTML；合作模式缺选其他门控；§24 职级/岗位映射口径分裂 |
| GEN-009 | 适用 | 未发现伪成功完成声明除 T07 章首 |
| GEN-010 | 适用 | §2.3 七项脱敏闭集与 PGM-043 个人信息七项同构；联系人查看例外遗漏见 T09 |
| GEN-011 | 适用 | 留痕字段/F10 存在 |
| GEN-012 | **不适用** | 无运行时资源 |
| GEN-013 | **不适用** | 无构建脚本 |
| GEN-014 | **不适用** | 无迁移 |
| GEN-015 | 适用 | 低保真结构可读；T05 通用状态门控活句损害口径 |
| GEN-016 | **FAIL** 见 T05/T06/T07/T08 | 现行/历史、完成声明、图表可执行语义、补录回灌 |
| GEN-017 | 适用 | 四分类见 §6；不因 HTML 零命中删除 V184/dealer_portal 未实现括注 |
| GEN-018 | 适用 | 本报告依据 HTML 原文、命令输出、实现路径 |

### 4.2 BEQR（21）

| ID | 结论 | 证据/不适用理由 |
|---|---|---|
| BEQR-063 | 不适用（无酒吧经营立法） | 本文件 §17/§20 明确不承担票务/酒吧经营动作；F9 五类高危承接 PGM-041 而非本条沽清两档；未发明反结/沽清规则 |
| BEQR-074 | 不适用 | 规格对「隐私桌台」零命中；未发明桌台权限；由酒吧实现模块承接 |
| BEQR-143 | 适用、本条未单列 FAIL | L946–956 四类主体字段与「同一经营对象当前有效绑定关系唯一」 |
| BEQR-144 | 适用 | 名词脚注 L57 + 补录/页面未把行政隶属写成现行迁移边；「最高归属」措辞见 O1 |
| BEQR-145 | 适用 | 本文件未发明独立「资产公司」主体 |
| BEQR-146 | 适用 | 未发明域公司越权管理 |
| BEQR-147 | 适用 | 未发明收入归属；§0A 尾差由娱乐平台公司承担与 HTML 跨域通票尾差同向 |
| BEQR-148 | **FAIL** 见 T04 | HTML `:231`「选“其他”的合同必须在生效前指定套用哪几项收费模板…未指定的合同不得生效」；规格 L989 合作模式校验只列六值 |
| BEQR-149 | 适用 | L1047/L1749/L2709 M1–M10 上中下 |
| BEQR-150 | 适用 | L126/L654/L877 十一端；`dealer_portal` 标未实现（技术性，不删） |
| BEQR-151 | 适用 | L753/L2771 五源 − 显式拒绝；无通配第六源 |
| BEQR-152 | **FAIL** 见 T09/T10 | L849 查看/导出/修改分别授权已写；PGM-043 联系人查看例外与导出密码/暗纹/敏感页未承接 |
| BEQR-153 | 适用 | L652/L2141 七档；拉取失败兜底关闭+留痕例外 L2144 |
| BEQR-154 | 适用 | L175/L901/L1164 六值；不定义迁移边 |
| BEQR-155 | 适用 | L293/L1171–1175 六值+缓冲期+试运营不管 |
| BEQR-156 | 适用 | L338/L1178 八值；§0A 合同当地时间 |
| BEQR-157 | 适用 | L421/L1206 五值 |
| BEQR-158 | 适用 | L534/L1192 七值；设备「异常说明」L1102 仍活（设备七态有异常，非班次状态机） |
| BEQR-159 | 适用 | L611/L1219 五值；L1065「应选择启用角色」用本闭集「已启用」，不升岗位状态机 |
| BEQR-166 | 适用 | L326–327/L1547–1556 主线默认总经理+平行线全部通过 |
| BEQR-189 | 不适用 | 规格对服务点单管理费零命中；未发明费率 |

### 4.3 BSM（12）

| ID | 结论 | 证据/不适用理由 |
|---|---|---|
| BSM-004 | 不适用（沉默≠删除） | 本文件 P0 页未立法存档删除页；未与存档/软删除/删前占用冲突；不因映射把该页写入本合并件 |
| BSM-005 | 不适用 | 未立法「一大张表单一次填完」或相反；低保真只给表单分组 |
| BSM-006 | 不适用 | 不定义票码刷新 |
| BSM-007 | 适用 | L2771 平台管理员日常功能 vs 资金/敏感/删除/最终审批须逐项授予 |
| BSM-009 | 适用 | 页面分组覆盖综合体/空间/商户/合同/员工/设备/权限 |
| BSM-028 | 适用 | 项目/楼层区域点位、合作模式六类、多对多、城市项目公司边界、合同准入先卡住、员工岗位班次 |
| BSM-029 | 适用 | 跨流程依赖 L2168–2186 |
| BSM-072 | 适用 | 业态六值单一归属 L250/L934；商户侧边界 L740 |
| BSM-074 | 适用 | 连接基础管理 |
| BSM-114 | 适用 | L2683 私人手机 App 低风险闭集按 HTML「查看考勤」等；十一端入口在终端开关页 |
| BSM-115 | 适用 | 流程图「后续票务、酒吧…」依赖治理对象 |
| BSM-120 | 适用 | 设备标识/换机/职级薪资大范围已确认条与 HTML 已拍板同向 |

### 4.4 PGM（33）

| ID | 结论 | 证据/不适用理由 |
|---|---|---|
| PGM-002 | 适用 | 治理顺序对象→合同→人员→设备→留痕，F5/F6/F8/F10 |
| PGM-003 | 适用 | §2 页面分组覆盖卡片模块；楼宇/应急未单列 P0 页，本文件定位治理后台 P0，未发明相反模块 |
| PGM-004 | 适用 | 集团视图已承接时态 L754/L2729 |
| PGM-005 | 适用 | 空间九类+项目为归属根 L196/L904 |
| PGM-006 | **FAIL** 见 T01/T02/T03 | HTML「第一联系人和第二联系人」；低保真表单用主要/财务/运营 |
| PGM-007 | 适用 | 合同准入页+先阻断 |
| PGM-008 | 适用 | 两条注册路径 L432；设备凭证不读硬件识别码 |
| PGM-009 | 适用 | 单一功能权限点 L752 |
| PGM-010 | 适用 | §14A / F11 |
| PGM-011 | 适用 | 设备台账/领还 |
| PGM-012 | 适用 | 商户侧授权范围内 |
| PGM-013 | 适用 | 未发明未核验第三方 |
| PGM-014 | 适用 | 操作留痕页 |
| PGM-015 | 不适用（P0 页未承载楼宇运营明细） | 未发明相反规则 |
| PGM-016 | 适用（部分） | U 盘密钥最高权限 L1011；隐私见 T09/T10 |
| PGM-017 | 适用 | 综合体项目归属；022 已删「所有对象继续携带 project_id」假完成；补录 L3006「综合体项目归属」与 HTML 同向、不是 project_id 列 |
| PGM-018 | 适用 | 多对多；O1 |
| PGM-019 | 适用 | 未发明域公司越权 |
| PGM-020 | 适用 | 项目档案/状态/负责人为商户空间合同员工设备票务看板提供归属 |
| PGM-021 | 适用 | 避免多综合体数据混 |
| PGM-026 | **FAIL** 见 T01/T04 | 第一/第二联系人；选其他收费模板 |
| PGM-027 | 适用 | 商户侧全开放+看不到平台全局 |
| PGM-031 | 适用 | §0A 外部经销商行；无独立经销商 P0 页属范围裁剪，不立法相反隔离规则 |
| PGM-035 | 不适用 | 不定义提成发放流程 |
| PGM-040 | 适用 | 单一功能权限点 |
| PGM-041 | 适用（重算已对齐） | L1989 保存岗位/职责/同范围组织变更立即重算；跨项目先审批；已删仅职级排除 |
| PGM-042 | 适用 | 职级影响默认权限模板和审批层级但不能替代功能权限点；L2000 |
| PGM-043 | **FAIL** 见 T09/T10 | 查看/导出分权已写；联系人查看例外与导出密码/暗纹/敏感页未写 |
| PGM-044 | 不适用 | 隐私桌台/正式挂单零命中且页面不承担酒吧经营 |
| PGM-048 | 适用 | 商户侧边界 |
| PGM-060 | 不适用 | 不定义会员注册 |
| PGM-065 | 适用 | 连接关系表 L2168 |
| PGM-066 | 适用 | 集团视图已拍板+已承接 |

## 5. 对抗式（grilling 结构，自问自答；事实本席自读）

Q1 已废止规则是否被其他活句回灌？
→ 是。T05 低保真通用状态门控；T06 F2 已废止节点仍渲染；T08 补录岗位停用/预警/禁止领用。班次详情「异常说明」与设备领用并列、页面清单用「异常记录」：独立判定为设备异常事项而非班次状态机（与 025 排除同向，但不引用 025 作为依据）。F7 mermaid「班次结束或异常处理」读作流程异常分支，不升 finding。

Q2 项目归属是否假完成？
→ 022 已删「所有对象继续携带 project_id 并按项目归属聚合」。现行 L770/L2729 不再含 project_id。补录 L3006「所有对象继续携带综合体项目归属」与 PGM-066/017 同向。O1：§5.1「最高归属入口」措辞偏隶属，未写成行政链。

Q3 主体联系人是否与 HTML 一致？
→ 否。PGM-026/043 只要第一/第二联系人。字段字典把主要联系人与第一联系人并列为两行必填（T02），另立法财务/运营联系人（T03）；低保真新建表单写「主要联系人、电话、财务联系人、运营联系人」（T01），零「第一/第二」。

Q4 权限重算是否仍排除职级？
→ 否。L1989 现文与 PGM-041 角色装配一致，已无「仅职级变化不触发」。

Q5 查看/导出是否分权且联系人例外是否写明？
→ 分权 L849 有。第一/第二联系人手机号查看例外、导出密码/暗纹/敏感页无（T09/T10）。

Q6 端点/字段/完成声明是否把旧核验写成现行？
→ §24.1 路径现存，完成声明可复核。§24 章首「到职级映射已落地」与 24.3.6「不得写入产品规格」互斥（T07）。§24.2 自承待补齐，不当完成。

Q7 图表可执行语义？
→ F2 五个已废止节点无边仍定义，mermaid 会画出孤立框（T06）。F3 废止边已 `%%` 注释，不渲染。F9 无复制模板支路。

Q8 能否因 HTML 零命中删除 V184/dealer_portal/OpenAPI 快照？
→ 否。GEN-017：无业务发明+无冲突=技术性。V184 括注与 dealer_portal 未实现是实现差登记。

## 6. 临时 findings（四分类顺序：冲突→细化→技术性→添加）

临时编号 GR-M014-Tnn，不抢正式 ID。每条含文件:行、完整原句、HTML/GEN 证据、分类、2.3 依据类型、冻结判据、工程缺陷、最小处置。

### GR-M014-T01（建议队列是）

- 文件:行：`docs/platform-governance-product-spec-v0.2.md:2455`
- 原句：`- 联系信息：主要联系人、电话、财务联系人、运营联系人。`
- HTML：PGM-026 `docs/platform-governance-map-for-operations-v0.1.html:494`「建立商户档案、运营主体、负责人、第一联系人和第二联系人。」；PGM 对「财务联系人」「运营联系人」count=0
- 同文对照：字段字典 L961–964 已有第一/第二；L1304「【已废止/不得回灌】商户联系人 P0 区分主要/备用/财务/运营联系人并立法为必填分组」
- 分类：**冲突**（低保真表单回灌已废止分组、遗漏 HTML 第一/第二）
- 依据类型：1
- 判据：PGM-006、PGM-026、GEN-008、GEN-016
- 工程缺陷：是
- 最小处置：该分组改为「第一联系人、第一联系人电话、第二联系人、第二联系人电话」；不把财务/运营升为必填

### GR-M014-T02（建议队列是）

- 文件:行：L959–962
- 原句：
  - `| 主要联系人 | 商户日常联系人（即第一联系人） | 必填 | 文本 | 不应为空 | 变更需留痕 |`
  - `| 第一联系人 | 商户首要责任联系人，等同主要联系人 | 必填 | 文本 | 不应为空；与"主要联系人"为同一字段语义，页面任选其一展示并保持一致 | 变更需留痕 |`
- HTML：只要第一联系人（及第二）
- 分类：**细化**（同义两行必填，页面可双填不一致）
- 依据类型：1
- 判据：GEN-001、PGM-026
- 工程缺陷：是
- 最小处置：只留「第一联系人」一行必填，主要联系人改为别名说明或删除重复行

### GR-M014-T03（添加，B1 只登记）

- 文件:行：L965–966
- 原句：
  - `| 财务联系人 | 财务对账联系人 | 选填 | 文本 | 财务场景建议填写 | 变更需留痕 |`
  - `| 运营联系人 | 日常运营联系人 | 选填 | 文本 | 运营场景建议填写 | 变更需留痕 |`
- HTML：三份 HTML 对这两个字段名 count=0；PGM-026 只批第一/第二
- 分类：**添加**（业务联系人角色发明）
- 依据类型：1
- 判据：GEN-017、PGM-026
- 工程缺陷：否（待 2.2）；Grok 判添加
- 最小处置：B2 按 2.2 问保留或删除；本席不代答

### GR-M014-T04（建议队列是）

- 文件:行：L989
- 原句：`| 合作模式 | 确定经营关系类型 | 必填 | 选择 | 租赁、联营、自营、代运营、服务商接入、其他 | 变更需留痕 |`
- HTML：BEQR-148 `:231` 全文「自营、租赁、联营、代运营、服务商接入、其他。选“其他”的合同必须在生效前指定套用哪几项收费模板（固定租金/物业费/能源费、销售分成、代运营服务费、供应商应付）并写明理由，未指定的合同不得生效。」；PGM-026 同义
- 规格对「收费模板」「未指定的合同不得生效」count=0
- 分类：**冲突**（遗漏硬生效门控，读者可让「其他」无模板生效）
- 依据类型：1
- 判据：BEQR-148、PGM-026、GEN-008
- 工程缺陷：是
- 最小处置：在合作模式校验口径补 HTML 该句；不新造模板种类

### GR-M014-T05（建议队列是）

- 文件:行：L2337
- 原句：`每个对象的状态应在列表和详情中都清楚展示。状态不仅用于展示，也要影响页面操作。例如合同未生效时，经营范围绑定按钮需要禁用并说明原因。`
- 同文废止：L1284「【已废止/不得回灌】状态字段可以直接影响页面是否允许新增、编辑、绑定或只读查看——三 HTML 未批「所有对象都有状态并可门控页面」的通用规则。」；L2695 低保真验收同废止
- 合同未生效不得作经营依据是 BEQR-156/PGM-007 特例，不能回写成「每个对象」通用门控
- 分类：**冲突**
- 依据类型：1
- 判据：GEN-016、BEQR-154/157（未批通用状态门控）
- 工程缺陷：是
- 最小处置：删除「状态不仅用于展示，也要影响页面操作」通用句；保留合同未生效示例并限定为合同

### GR-M014-T06（建议队列是）

- 文件:行：L1485–1489（mermaid F2）
- 原句：
  - `F["【已废止/不得回灌】是否用于经营"]`
  - `G["【已废止/不得回灌】标记为可经营"]`
  - `H["【已废止/不得回灌】标记为通行、库房、员工区或设备点"]`
  - `I["【已废止/不得回灌】进入可绑定商户范围"]`
  - `J["【已废止/不得回灌】进入可留痕或可巡检点位"]`
- 命令：节点 {A..K} 定义；边内 {A,B,C,D,E,K}；孤立 {F,G,H,I,J}。mermaid 仍渲染五框
- 分类：**细化**（图表可执行语义：废止路径仍作为流程图节点出现）
- 依据类型：4
- 判据：GEN-016、GEN-001、PGM-005
- 工程缺陷：是
- 最小处置：从 mermaid 删除 F–J 节点，只留 `%%` 注释（同 F3 废止边写法）

### GR-M014-T07（建议队列是）

- 文件:行：L2757、L2823、L2838–2839、L2862
- 原句：
  - L2757：`本章所述三件事中，①有效权限集接口、③准入审批层级到职级映射在 R6 已落地，本章做收口确认；`
  - L2823：`### 24.3 准入审批层级到岗位映射（approval_level → resolver）`
  - L2838：`- 职级表 `staff_levels`（`main_level` M1-M10、`sub_level` upper/middle/lower、`level_rank` 1..30）与 `staff_level_assignments`（员工→职级、薪资）。`
  - L2862：`现网仍按职级算术与 `specific_role` 实现，与本节业务口径不一致。业务口径以 GOV 三档、密钥持有人与逐单转发为准；实现侧算术不得写入产品规格。`
- HTML：PGM-041 审批链按组织层级自动补全、不由发起人选择档位；字段字典 §8.1 已写不折算职级
- 分类：**冲突**（现行/历史完成声明把职级/岗位映射当已落地产品口径，并列出 staff_levels 为解析数据源）
- 依据类型：1+4
- 判据：GEN-016、BEQR-166、PGM-041
- 工程缺陷：是
- 最小处置：章首改「职级算术是实现现状、不是产品口径」；标题改为「到组织汇报链映射」；24.3.3 删除把 staff_levels/level_rank 当作审批解析来源的句子（薪资用途可留在职级字段）

### GR-M014-T08（建议队列是）

- 文件:行：补录 L2930、L2966、L3138、L3158、L3388、L3420（均无【已废止】）
- 原句：
  - L2930：`| 拦截记录 | 被阻断、预警或升级审批的业务记录 |`
  - L2966：`维修中、挂失、报废设备不可继续领用。`
  - L3138：`| 岗位停用 | 不允许继续分配 |`
  - L3158：`| 设备维修中或挂失 | 不允许领用 |`
  - L3388：`| 设备状态 | 控制设备是否可领用 | 必填 | 选择 | 待领用、在用、待归还、维修中、挂失、报废、归档 | 变更需留痕 |`
  - L3420：`| 维修中 | 设备不可使用 | 禁止领用 |`
- 同文：L1858 岗位不可分配已废止；L1192「本规格不定义…禁止条件」；仅预警已废止（L3090）
- 补录引言 L2867 声称已废止闭集已标注
- 分类：**冲突**（未标废止活句回灌）
- 依据类型：1
- 判据：GEN-016、BEQR-157、BEQR-158、PGM-007
- 工程缺陷：是
- 最小处置：上述各句加【已废止/不得回灌】或改为与正文一致；不新写岗位/班次/设备禁止条件状态机

### GR-M014-T09（建议队列是）

- 文件:行：L960–964（电话「展示需控权」）、L843–848（脱敏总则）
- 原句（L843）：`界面脱敏不提供关闭开关。凡导出内容含下列任一封闭项，一律强制脱敏（七项、无第八项）：姓名、手机号、证件信息、人脸与生物特征、住址、支付账号、精确位置。`
- HTML：PGM-043 `docs/platform-governance-map-for-operations-v0.1.html:680`「商户档案页面中的第一联系人和第二联系人手机号，按“客人与员工的个人信息保护”条中明确的查看权限例外执行；该页面查看例外不授予未脱敏导出或打印权限，也不免除敏感页的查看留痕及其他页面保护要求。」；同页 `:871` 授权人员可看完整号码、平台管理员身份不自动取得
- 规格未写该例外 → 读者会按七项一律脱敏，与 HTML 查看例外冲突
- 分类：**冲突**
- 依据类型：1
- 判据：PGM-043、BEQR-152、GEN-008
- 工程缺陷：是
- 最小处置：在 §2.3 补 HTML 该查看例外全文要点；不发明第八项

### GR-M014-T10（建议队列是）

- 文件:行：§2.3 L843–849（查看/导出已写分权与七项，缺密码/暗纹/敏感页）
- 原句（L849）：`- 敏感经营数据的查看、导出和修改分别授权，与上列个人信息封闭清单分列。`
- HTML：PGM-043 `:681`「所有导出的文件一律带密码…后台、商户专区和员工手机端的所有在线页面都带有按同一规则生成的暗纹…凡页面上出现上述两类必须强制脱敏内容的，这个页面就是敏感页…打开敏感页这件事本身也要留痕」
- 规格对「导出文件」「暗纹」「敏感页」count=0
- 分类：**冲突**（映射 PGM-043 遗漏硬规则；本文件已有敏感字段/导出节，不是无关章）
- 依据类型：1
- 判据：PGM-043、BEQR-152、GEN-016
- 工程缺陷：是
- 最小处置：在 §2.3 承接 HTML 导出密码、暗纹、敏感页与打开留痕；逐字不发明

## 7. 命令证据

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

sha256sum docs/platform-governance-product-spec-v0.2.md
# 37f60e779b915b80ee03438196885d188a22decf30e2ccf521007bab9d00f622

wc -l -c docs/platform-governance-product-spec-v0.2.md
# 3505 231468

git ls-files -z -- docs/platform-governance-product-spec-v0.2.md | xargs -0 cat | wc -l
# 3505
```

HTML 行命中与 mermaid 孤立节点见 §1/T06。`AccessController.java:244` / `AdminApp.tsx:451` / `docs/openapi/openapi-p0.yaml:8098` 证 §24.1 路径现存。未跑 Maven/psql/测试。

## 8. 观察（建议队列否；不升 finding）

- O1：L149「最高归属入口」、L2172「所有治理对象的最高归属」措辞偏隶属；PGM-018 为多对多隔离维度。未写成「平台→项目公司→商户」链；022 已删 project_id 假完成。
- O2：L2570 班次详情「异常说明」与「设备领用」并列；页面清单 L485 用「异常记录」。独立读作设备异常事项，不是班次五态门控。L2577 已废止「异常班次必须填写异常说明」。
- O3：L1840 F7 mermaid「班次结束或异常处理」为流程异常分支，不是班次状态=异常。
- O4：L2139/L1248 §12A.6 自指；规则子弹已在同节，指针空转但不立法相反开关。
- O5：L1303 V184 字典码括注=实现差登记，不得升为产品空间类型闭集。
- O6：L1859–1860「班次缺少责任区域」与「班次缺责任区域」重复，shrink。
- O7：019→M261、020 观察只作既有输入；本席不重开、不改台账。
- O8：§24.2 字段投影待补齐是缺口声明，不是假完成。
- O9：计划冻结 3494 vs 现行 3505，因 B3-r1…r4 增行；OVERSIZE 按现行全文。
- O10：HEAD ≠ origin/main；本席审磁盘 V0.09。

## 9. ponytail-review（只列可删，不落地）

- L1485-1489: delete: F2 五个已废止孤立 mermaid 节点. 改 `%%` 注释.
- L959-960: shrink: 「主要联系人」与「第一联系人」并行列. 留一行.
- L1859-1860: shrink: 班次缺责任区域重复行. 留一行.
- L2757: delete: 「到职级映射已落地」完成句. 以 24.3.6 为准.
- net: 约 -15 行可能（不含补录废止标注，标注不减业务复杂度）.

## 10. 结论

单席 B1：**FAIL**

同时满足失败条件：映射 84 条已逐项核对；三线已跑；覆盖表覆盖 3505 行；存在未定稿工程缺陷 T01/T02/T04/T05/T06/T07/T08/T09/T10；添加项 T03 未答复。不存在有条件通过。

建议 B2：上述 T01–T10 整批归并；T03 走 2.2；001–025 保持否；019/020 不重开。修复队列为空之前不得 B3。无第 5 批 B3 授权。本席不修、不问、不改 HTML/锁/ledger/findings。

步骤仍 B1 交付；M014 生命周期由调度写 IN_PROGRESS；历史 B3=4。不覆盖上方历史 B1/B2 原文。

## M014 现锁 B2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.06（本段追加；上方 V0.01–V0.05 前缀 238525 bytes / SHA256 `4d3f7f2d4e6db2b689b03808dec47e3505f46cff246378bd0c3f110f06f6e2fd` 原样保留，不覆盖、不改写 B1 锁段起点；本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，grok-4.6/xhigh，独立）。步骤：M014 现锁 B2 整批候选复核。不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.14；modules.md V0.04（M014 FILES 1 / 判据 84 / 计划冻结 3494 行 OVERSIZE-SINGLE-FILE；现行全文 3505 行）
- 正本：`/home/james/complex-management` main 工作区。`HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main`=`12728763626f0de83d7d828fb6a36edfb1d3be1c`（两值不等）。本模块 1 文件与本次 B1 覆盖表全等（标题 V0.09，3505 行，231468 bytes，SHA256 `37f60e779b915b80ee03438196885d188a22decf30e2ccf521007bab9d00f622`，endswith `\n`=True）。本席未改该文件。
- 操作员续作：按 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 执行 M014 现锁 B2 整批归并复核。历史 B3 累计 **4** 不清零，无第五批授权；M013 计数清零不延用本模块。
- 席甲：Codex（作者自审票）；本席 FAIL 具否决权。输入含席甲 B1 V0.08 与其 B2 V0.09；本票不照抄席甲 PASS，逐条核源/HTML/锁后接受或反驳。
- 本席 B2：**FAIL**（027/028 分类与否决成立；026/029–032/034–036 及 001–025 口径可接受；否决点：033 写入集合漏同表 L3422，且精确拟改 2930 仍写会废止预警记录的统一前缀；共同版本漏页面清单 §20）
- 无 MCP、无子代理、无第三席、不问操作员。未改源码/测试/HTML/lock/findings/ledger。未 fetch/commit/push/merge/deploy。未写新产物。只追加本指定路径。未装依赖。

此票只判断 findings.md V3.74 顶部「当前 M014 现锁 B2 整批候选（未定稿；2026-09-23）」相对本次两份完整 B1（self V0.08 + 本席 grok V0.05）的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲按本席意见改候选后本席再 PASS，且席甲本批亦 PASS，才能定稿。不覆盖上方 V0.05 B1 **FAIL** 原文。B2 PASS 只表示归并定稿；其后须 `BLOCKED-3ROUNDS` 等操作员，不等于修复或模块 PASS。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M014-b1-grok-raw.md` 前缀（本席 B1 V0.05，本段追加前） | V0.05 | 238525 | `4d3f7f2d4e6db2b689b03808dec47e3505f46cff246378bd0c3f110f06f6e2fd` |
| `docs/audit-2026-09/modules/M014-b1-self-raw.md`（席甲 B1 V0.08 + B2 V0.09） | V0.09 | 82848 | `fdc0959b670a8b3c158a4ebcb832e269cc03600659aa113c5dad668ffd4a72f4` |
| `docs/audit-2026-09/findings.md` 顶部「当前 M014 现锁 B2 整批候选（未定稿；2026-09-23）」 | V3.74 | 1314431 | `7612af57162fb4c35511d4c857f373c74fdded5ef5ce6106b032c8a12c62d274`（全文件） |

锁文件只读实算（本段不写独占标签行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`；sidecar 正文同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。`criteria-lock.md` 版本 V0.14。未改锁、不加判据。

三 HTML 只读、未改（与本次 B1 同）：ENUM `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`（308 行）；BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`（2032 行）；PGM `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f`（979 行）。

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

ledger 只读：模块表 M014 `IN_PROGRESS`，B3 累计 **4**。本席不改。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核规格原文/三 HTML/锁锚点/唯一性再接受或反驳；禁止表演性同意；不假定席甲反驳正确；不只审本席 T 项；B2 不修复 |
| grilling | 只借追问树自查（差集漏并、027 是否双填、028 能否指向上位、冲突 vs 遗漏、033 同根、036 或→且、变更日志落点、001–025），不问操作员、不派子代理 |
| verification-before-completion | 声称前先跑哈希、原句 `count==1`、点读 HTML 494/656/659/680/681/871、ENUM 231、规格 843–849/943–966/989/1122/1192/1284/1304/1309/1485–1490/1977/1996/2337/2455/2631/2695/2757/2805/2823/2839/2862/2867/2930/2966/2978/3138/3158/3188/3388/3392/3420/3422 |
| ponytail-review | 只评最小标记/收窄，不落地。033 就地前缀 < 整段删除补录；027 不去重别名行；028 不删选填字段 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（活句→同文废止/HTML 明文→拟改是否唯一）；不改生产代码 |
| graft | 文档/审计产物不走 graft 索引；本步未运行 graft CLI（会刷新 `graft/`，超出唯一可写路径） |
| tdd / ponytail 落地 / requesting-code-review | B2 禁止修复，未落测试、未改源 |

缺口（不冒称）：`batch-grill-me` / `ubiquitous-language` 本机无独立技能。grilling 本体「等用户回答 / 派子代理」作废。

### 完整差集（两席本次完整 B1 全部条目 → 候选）

| 来源 | 候选归属 | 本席核实 |
|---|---|---|
| GR-M014-T01 + CX-M014-02 | 新 M014-026，细化，缺陷是，队列是 | 差集正确。低保真 :2455 缺负责人/第一/第二；CX02 补负责人，T01 改联系人闭集。保留选填财务/运营与 028 一致，不是过并 |
| GR-M014-T02 | 新 M014-027，细化，缺陷否，队列否 | 差集正确。见下：本席 B1「可双填」被 :943/:961 原文否定，改判不成立 |
| GR-M014-T03 | 新 M014-028，细化，缺陷否，队列否 | 差集正确。本席 B1 添加不成立，见下；不发 2.2 |
| GR-M014-T04（020 原观察「其他」收费模板部分） | 新 M014-029，细化，缺陷是，队列是 | 差集正确。020 其余观察续记否，不把 020 整条重开 |
| GR-M014-T05 + CX-M014-03 | 新 M014-030，细化，缺陷是，队列是 | 差集正确 |
| GR-M014-T06 | 新 M014-031，细化，缺陷是，队列是 | 差集正确 |
| GR-M014-T07 | 新 M014-032，细化，缺陷是，队列是 | 差集正确。保留 2026-06-19 原文 |
| GR-M014-T08 | 新 M014-033，细化，缺陷是，队列是 | **写入集合不完整**，见 033 |
| GR-M014-T09 + CX-M014-01 | 新 M014-034，细化，缺陷是，队列是 | 差集正确。680/871 已批查看例外 |
| GR-M014-T10 | 新 M014-035，细化，缺陷是，队列是 | 差集正确。指针承接 681 |
| CX-M014-04 | 新 M014-036，冲突，缺陷是，队列是 | 差集正确。本席 B1 漏审；独立核 :1122 或→且成立 |
| 本席 O1–O10、ponytail shrink、席甲「既有输入与非问题」 | 020 观察否（收费模板部分升 029） | 成立；不顺手去重 :1859/:1860 |
| M014-001…025 | 保持否 | 候选表未改其分类/队列；025 已修不回退 |

无漏并 ID、无多开 ID、无把观察升缺陷（029 是字段校验表现行行，不是把 020 整条改「是」）。添加 0。无 HTML 申请。无锁外判据。

---

### 分类（固定顺序：冲突 → 细化 → 技术性 → 添加）

本席 B1 把 T01/T04/T05/T07/T08/T09/T10 写成冲突、T03 写成添加。B2 按 2.1 重核，**不沿用本席 B1 分类**：

- **冲突**须与三份 HTML **明文**相矛盾。遗漏、内部废止回灌、历史摘录未标，在能指向上位 HTML 条目时先归细化。GEN-017：不得因 HTML 沉默直接删除，也不得把遗漏自动升冲突。
- **026**：HTML :494 要求建立负责人、第一/第二联系人；:2455 是表单承接漏项，并未写「不得有第一联系人」。:1304 废止的是「立法为必填分组」，拟改保留选填不等于回灌必填。**细化**正确。
- **027**：:943「第一联系人与原主要联系人为同一语义（避免新增对象造成歧义）」；:961「与主要联系人为同一字段语义，页面任选其一展示并保持一致」。不是两字段双填。本席 B1「页面可双填不一致」无原文支持。风格去重不是工程缺陷。**细化、缺陷否、队列否**正确。
- **028**：:965/:966 已是选填用途。上位 HTML :494「建立商户档案…第一联系人和第二联系人」——财务/运营是同一商户档案联系人信息的用途细分，无新系统角色、权限点、独立实体或流程分支。:1304 只废止必填分组，不废止选填字段。零同名命中不足以判添加（2.1 能指向上位=细化，到不了添加）。本席 B1 添加撤回。**不发 2.2**。
- **029**：ENUM :231 / PGM :498 明文「选其他…未指定的合同不得生效」。:989 校验口径只列六值、未写相反「可以无模板生效」。遗漏硬门控=细化，不是明文矛盾。**细化**正确；现行字段表必须承接，队列是正确。
- **030**：:2337 与同文 :1284/:2695 废止并立。HTML 状态成员沉默≠明文禁止通用门控。**细化**正确。
- **031**：图表可执行语义，上位 PGM-005。**细化**正确。
- **032**：:2757 是 2026-06-19 代码核验句，不是现行产品立法；:2839 把职级表写成现行解析数据源，与 :2827/:2833/:2860/:2862 及 PGM-041 组织汇报链不一致。内部现行/历史分裂=细化。**不得改写成删除 2026-06-19 原文**。
- **033**：:2867 已声明补录为历史摘录载体，未逐条标注≠自动升现行。六处就地消歧=细化。本席 B1 冲突撤回。
- **034**：:843 七项清单挂在「凡导出内容」；:844–848 是查看规则且漏 HTML :871/:680 已批例外。遗漏已批例外=细化，不是「七项查看一律无例外」的明文相反句。
- **035**：已有 §2.3 敏感字段节应承接 PGM-043 :681。遗漏=细化。不得新造密码算法或宣称已实现。
- **036**：HTML :656「临时授权可设有效期**或**单次使用」；:1122「单次有效也应有兜底失效时间」把或收成且。这是明文矛盾，**冲突**正确。:659「临时权限申请有时间限制、到期自动失效」与 :1309「必须支持失效时间」是申请/能力要求，拟改不得废止这两条。

---

### 逐 ID

#### M014-026 PASS（分类/队列）；拟改可执行

原句 :2455 count=1。拟改补负责人、第一（主要）及电话、第二及电话，财务/运营沿字典选填、不另加必填。不改 HTML/库/页面代码。与 028 保留选填一致。

#### M014-027 PASS（否）

:943/:961 原文已排除双填。保留别名行。不作风格去重。

#### M014-028 PASS（否）

上位 HTML :494。选填用途细分。不新设岗位、不升必填。与 :1304 已废止必填分组区分。

#### M014-029 PASS（是）

原句 :989 count=1。拟改把 ENUM :231 / PGM :498 收费模板、理由、未指定不得生效写入该行校验口径，不实现新收费机制。020 仅此部分升级，其余 020 观察仍否。

#### M014-030 PASS（是）

原句 :2337 count=1。拟改收窄通用门控、保留合同未生效示例，不造状态/迁移边。

#### M014-031 PASS（是）

F–J 五节点 count 各=1，边集仍为 {A,B,C,D,E,K}。拟改加 `%%` 与同图 :1490 及 F3 :1559 先例一致，不再渲染孤立框。

#### M014-032 PASS（是）

标题 :2823 count=1；职级表数据源 :2839 count=1；:2757「到职级映射已落地」count=1。精确拟改改标题为「到审批人解析」、数据源改为组织汇报链+密钥持有人、职级仅薪资。其余段落：:2757 整段不删，前缀标「历史代码核验，非当前完成声明」，段尾指回 §24.3 三档。不改审批代码。与 020 实现观察（specific_role）不抢修。

#### M014-033 FAIL（写入集合与 2930 拟改）

六处原句 count 各=1，均在 :2867 历史摘录段、均无【已废止】。就地加历史/废止标记、不新写设备/岗位状态机，方向正确。

**否决 1 — 漏同表同根 L3422。** 现行 `:3420` `| 维修中 | 设备不可使用 | 禁止领用 |` 已收入；同表下一数据行 `:3422` `| 挂失 | 设备丢失或找不到 | 禁止领用并提示异常 |` count=1、同样未标、同样是 :1192「本规格不定义…禁止条件」的成员级禁止领用。:2966/:3158 已覆盖挂失事实句，不能当 :3422 已处理——024/025 的前车就是废止框收了部分同根、漏了字段表/低保真。历史 B3=4、无第五批：本批若漏 :3422，B5 必再发现、无法再 B3。

**否决 2 — 精确拟改 2930 与其余段落互斥。** 精确拟改把 :2930 写成 `【历史摘录；已废止/不得回灌为现行门控】| 拦截记录 | …预警… |`，读者会把预警记录整行废止。其余段落要求收窄为 `【历史摘录；不得解释为预警可代替阻断，现行见合同准入规则】`，且不废止预警记录本身。HTML 准入是先阻断；预警记录可以存在，但不能代替阻断。B3 若先执行精确拟改块就会写错。候选必须让精确拟改 2930 与其余段落同一句，不得两套原文并存。

其余五处（:2966/:3138/:3158/:3388/:3420）用统一历史/废止前缀可接受。不把 :3178 补录 mermaid「填写授权原因和失效时间」并入 033 或 036——那是 :659 申请时限路径，禁止误废止。

#### M014-034 PASS（是）

:843–849 与 :2805 投影规则均 count=1。其余段落在 :849 后另起一段承接 HTML :871 查看例外全文要点 + :680「不授予未脱敏导出或打印、不免除留痕/敏感页」。:2805 尾追加「商户档案两联系人号码查看例外及边界同§2.3」。不发明第八项、不把查看例外扩成导出。

#### M014-035 PASS（是）

同在 §2.3 另起独立段，指针+原文要点承接 HTML :681（导出密码、不可复制/编辑、暗纹、敏感页、打开留痕），并写明「直接引用该 HTML 原文，不另造算法；本段为规格要求，不作为代码已完成声明」。顺序：:849 后先 034 段、再 035 段。

#### M014-036 PASS（分类/队列/1122 与三处历史）；边界正确

:1122 count=1，与 HTML :656 或规则及同文 :1977/:1996/:2631 相反。拟改改为「可设有效期或单次使用；采用有效期时填写，不能要求单次有效授权同时填写兜底失效时间」。:2978/:3188/:3392 各 count=1，标废止并指回现行或规则。:1309 能力要求保留。:659 申请有时间限制不写入废止集。不删有时限临时申请专门要求。

#### M014-001…025 PASS（保持否）

B4-r4 双 PASS 后队列 001–025 全否仍在 findings 历史段。020 仅收费模板观察升 029，其余续记否。019→M261 不重开。025 已修不回退。

---

### 共同版本 / 变更日志 FAIL

候选：「字段字典 §19、流程图 §17、低保真 §23 各加一行」。先例：活句落在哪个原文件，就给哪个变更日志加行（B3-r4：§20/§19/§23，流程图未改不加）。

| 活句 | 原文件变更日志 | 候选 |
|---|---|---|
| 026/030 低保真 | §23 | 有 |
| 029/036 :1122 字段字典 | §19 | 有 |
| 031 流程图 | §17 | 有 |
| 032 §24 | 低保真 §23（先例 :2749 增 §24） | 可落 §23 |
| **034/035 §2.3** | **页面清单 §20** | **无** |
| 033/036 补录历史句 | 无独立日志 | 不加，正确 |

**否决 3：** 034/035 改页面清单 §2.3，共同版本必须给 §20 加一行（内容列 034/035）。文首追加本批 ID、标题 V0.09→V0.10 可接受。不改三 HTML/源码/测试/OpenAPI/锁。

---

### 分类 / 队列（本席本票）

| ID | 分类 | 工程缺陷 | 队列 | 本票 |
|---|---|---|---|---|
| M014-026 | 细化 | 是 | 是 | PASS |
| M014-027 | 细化 | 否 | 否 | PASS |
| M014-028 | 细化 | 否 | 否 | PASS |
| M014-029 | 细化 | 是 | 是 | PASS |
| M014-030 | 细化 | 是 | 是 | PASS |
| M014-031 | 细化 | 是 | 是 | PASS |
| M014-032 | 细化 | 是 | 是 | PASS |
| M014-033 | 细化 | 是 | 是 | **FAIL**（漏 :3422；2930 精确拟改未收窄） |
| M014-034 | 细化 | 是 | 是 | PASS |
| M014-035 | 细化 | 是 | 是 | PASS |
| M014-036 | 冲突 | 是 | 是 | PASS |
| M014-001–025 | — | — | 否 | PASS（020 部分升 029） |

添加 0。不发 2.2。无 HTML 申请。无新判据。

---

### 全部不同意项及可执行最小修正（无修复）

1. **033 写入集合补 :3422。** 原句：`| 挂失 | 设备丢失或找不到 | 禁止领用并提示异常 |`（count=1）。拟改与 :3420 同一统一前缀：`【历史摘录；已废止/不得回灌为现行门控】| 挂失 | 设备丢失或找不到 | 禁止领用并提示异常 |`。不改 :3414/:3416/:3418/:3424 等非禁止领用成员行；不新写设备状态机。
2. **033 精确拟改 2930 整块替换为其余段落收窄句，删除统一前缀那一版。** 拟改唯一文本：`【历史摘录；不得解释为预警可代替阻断，现行见合同准入规则】| 拦截记录 | 被阻断、预警或升级审批的业务记录 |`。不废止预警记录本身，不允许以预警代替阻断。
3. **共同版本补页面清单 §20 一行。** 位置列 §2.3；内容列 M014-034/035（查看例外与 :681 指针）；与 §19/§17/§23 并列。032 若只改 §24 可继续落 §23。033/036 补录不加日志。

改完这三处后重送 B2。其余 ID 不必改分类或队列。本席不代写 findings。

---

### 本席实跑（B2，只读；无写模式）

- 锁 sha256sum：`b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`
- 输入：grok 前缀 238525/`4d3f7f2d…e2fd`；self 82848/`fdc0959b…72f4`；findings V3.74 全文件 `7612af57…d274`
- 1 文件指纹：3505 行 / 231468 / `37f60e77…f622` 与用户固定清单及本次 B1 全等
- 点读：规格 L1/L843–849/L943–966/L989/L1122/L1192/L1284/L1304/L1309/L1485–1490/L1559/L1977/L1996/L2337/L2455/L2631/L2695/L2749/L2757/L2805/L2823/L2839/L2862/L2867/L2930/L2966/L2978/L3138/L3158/L3188/L3388/L3392/L3420/L3422；ENUM L231；PGM L494/L498/L656/L659/L680/L681/L871；锁 PGM-026/041/043、BEQR-148、GEN-016/017
- 上列拟改原句 `text.count` 均为 1（:3422 亦 1）
- 未跑：主仓测试、psql 写、部署、修复
- 未改 findings/ledger/锁/源/HTML/主索引

### 结论

逐 ID：026/027/028/029/030/031/032/034/035/036 及 001–025 PASS；**033 FAIL**；**共同版本 FAIL**。

本席队列若按候选：026/029–036 是、027/028 否——**在 033 与 §20 按上列三处改完之前不得定稿**。

本席 B2：**FAIL**

未经按上列三处修订后的双 PASS，不得把候选当定稿，更不称模块 PASS，不得 B3。步骤仍 B2；M014 `IN_PROGRESS`；历史 B3=4 不清零，无第五批授权。即使日后本批 B2 双 PASS，也只表示归并定稿，其后须 `BLOCKED-3ROUNDS` 等操作员，不等于修复或模块 PASS。不覆盖上方 B1 FAIL 原文。

## M014 现锁 B2 第二版整批归并复核（Grok 乙席）

- 文件自身版本：V0.07（本段追加；上方 V0.01–V0.06 前缀 258129 bytes / SHA256 `3afda398cb0272a5353341b3d9378d9606d75df190a2408edc90a96a71b4bd48` 原样保留，不覆盖、不改写 B1 锁段起点；本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，grok-4.6/xhigh，独立）。步骤：M014 现锁 B2 第二版完整候选复核。不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.14；modules.md V0.04（M014 FILES 1 / 判据 84 / 计划冻结 3494 行 OVERSIZE-SINGLE-FILE；现行全文 3505 行）
- 正本：`/home/james/complex-management` main 工作区。`HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main`=`12728763626f0de83d7d828fb6a36edfb1d3be1c`（两值不等）。本模块 1 文件仍为标题 V0.09，3505 行，231468 bytes，SHA256 `37f60e779b915b80ee03438196885d188a22decf30e2ccf521007bab9d00f622`。本席未改该文件。
- 操作员续作：按 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 执行 M014 B2 第二版完整归并复核。历史 B3 累计 **4** 不清零，无第五批授权；M013 计数清零不延用本模块。即使本批双 PASS，只定稿然后 `BLOCKED-3ROUNDS` 等操作员，**不执行 B3**。
- 席甲：Codex（作者自审票）；本席 FAIL 具否决权。输入含席甲 B1 V0.08、其 B2 V0.09/V0.10、本席 B1 V0.05 与 B2 V0.06（整体 FAIL）。本票不照抄席甲 PASS，不以 V0.06 旧结论替代现行源文章节；逐条核源/HTML/锁后接受或反驳。
- 本席 B2 第二版：**PASS**（V0.06 三项否决均已按源闭合：033 补 :3422；2930 精确拟改与后附收窄句同一句；共同版本按字段字典 §19 而非页面清单 §20。001–036 整批分类/队列/差集/最小处置可定稿。）
- 无 MCP、无子代理、无第三席、不问操作员。未改源码/测试/HTML/lock/findings/ledger。未 fetch/commit/push/merge/deploy。未写新产物。只追加本指定路径。未装依赖。未跑 graft CLI（会刷新 `graft/`，超出唯一可写路径）。

此票只判断 findings.md V3.75 顶部「当前 M014 现锁 B2 第二版完整候选（未定稿；2026-09-23）」相对本次两份完整 B1（self V0.08 + 本席 grok V0.05）及本席 B2 V0.06 FAIL 三项的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。下一标题「当前 M014 现锁 B2 整批候选」起为旧候选，只作历史，不定稿依据。必须另有席甲本批 B2 第二版 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.05 B1 **FAIL**、V0.06 B2 **FAIL** 原文。B2 PASS 只表示归并定稿；其后须 `BLOCKED-3ROUNDS` 等操作员，不等于修复或模块 PASS。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M014-b1-grok-raw.md` 前缀（本席 B1 V0.05 + B2 V0.06，本段追加前） | V0.06 | 258129 | `3afda398cb0272a5353341b3d9378d9606d75df190a2408edc90a96a71b4bd48` |
| `docs/audit-2026-09/modules/M014-b1-self-raw.md`（席甲 B1 V0.08 + B2 V0.09/V0.10） | V0.10 | 83382 | `a8dcd476c56324804b7ccb05e9a826e709e85027e7120c913cbd3aa6d924b479` |
| `docs/audit-2026-09/findings.md` 顶部「当前 M014 现锁 B2 第二版完整候选（未定稿；2026-09-23）」至下一旧候选标题前 | V3.75 | 1329503 | `f17a7dbdf965d25d9b1976e7da0537162fd18afef80257730e89c76a30fe964c`（全文件） |

锁文件只读实算（本段不写独占标签行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`；sidecar 正文同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。`criteria-lock.md` 版本 V0.14；映射行 L6399 COUNT=84 unique=84。未改锁、不加判据。

三 HTML 只读、未改（与本次 B1/V0.06 同）：ENUM `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`（308 行）；BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`（2032 行）；PGM `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f`（979 行）。

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

ledger 只读：M014 现锁重开 IN_PROGRESS，B3 累计 **4**（r1 001–018、r2 021–023、r3 024、r4 025 获批追加）。本席不改。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核规格原文/三 HTML/锁锚点/唯一性再接受或反驳；禁止表演性同意；不假定席甲反驳或 V0.06 章节归属正确；不只审两处修订；B2 不修复 |
| grilling | 只借追问树自查（差集漏并、027 双填、028 上位、冲突 vs 遗漏、033 同根、036 或→且、变更日志落点、001–025、§2.3 属哪份原文件），不问操作员、不派子代理。本体「等用户回答 / 派子代理」作废 |
| verification-before-completion | 声称前先跑哈希、原句 `count==1`、点读源 782/798/800/802/841/843–849/1320/2237/2737/2749/2805 与 HTML 494/498/656/659/680/681/871、ENUM 231 |
| ponytail-review | 只评最小标记/收窄，不落地。033 就地前缀 < 整段删除补录；027 不去重别名行；028 不删选填字段；共同版本不加空的 §20 行 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（活句→同文废止/HTML 明文→拟改是否唯一；章节号→原文件边界）；不改生产代码 |
| graft | 文档/审计产物不走 graft 索引；本步未运行 graft CLI |
| tdd / ponytail 落地 / requesting-code-review | B2 禁止修复，未落测试、未改源 |

缺口（不冒称）：`batch-grill-me` / `ubiquitous-language` 本机无独立技能。

### V0.06 三项否决 → 第二版对照（先核源，不采信转述）

本席 V0.06：**FAIL**。否决三条：① 033 写入集合漏同表 :3422；② 精确拟改 2930 仍写会废止预警记录的统一前缀，与后附收窄句两套原文并存；③ 共同版本漏页面清单 §20（当时把 034/035 的 §2.3 记成页面清单）。

| V0.06 否决 | 第二版现行 | 本席核源 |
|---|---|---|
| ① 033 补 :3422 | 表列为 2930/2966/3138/3158/3388/3420/**3422**；精确块「补同表挂失行」原句 `\| 挂失 \| 设备丢失或找不到 \| 禁止领用并提示异常 \|` count=1（:3422），拟改与 :3420 同一历史/废止前缀 | **闭合**。:3414/:3416/:3418/:3424 非禁止领用成员行未收入，正确 |
| ② 2930 精确拟改换收窄句 | 精确块拟改唯一文本：`【历史摘录；不得解释为预警可代替阻断，现行见合同准入规则】\| 拦截记录 \| …`；其余段落同一句；当前段**不含** `【历史摘录；已废止/不得回灌为现行门控】\| 拦截记录` | **闭合**。不废止预警记录本身，不允许以预警代替阻断。与 HTML 先阻断同向 |
| ③ 共同版本补 §20 | 第二版**不接受**该事实前提，改为 §19 记 029/034/035/036，§20 不加 | 见下节。**V0.06 第三条的章节归属被源证推翻**；第二版映射按源成立 |

---

### 第三项：章节归属（以现行源文件为证，不以 V0.06 替代）

V0.06 把 034/035 改的「§2.3」写成页面清单，因而要求给页面清单 §20 加行。本席本票重读合并件原文件边界：

| 行 | 现行原文 |
|---|---|
| 44 | `## 原文件：platform-governance-page-list-v0.1.md` |
| 782 | `## 20. 变更日志`（**页面清单**自己的日志） |
| 798 | `---` 分隔 |
| 800 | `## 原文件：platform-governance-field-dictionary-v0.1.md` |
| 802 | `# 平台治理层字段字典 V0.1` |
| 841 | `### 2.3 敏感字段原则` |
| 843–849 | 脱敏总则 / 七项 / 查看分权（034/035 写入点） |
| 1320 | `## 19. 变更日志`（**字段字典**自己的日志） |
| 1345 | 原文件流程图 |
| 2237 | 流程图 `## 17. 变更日志` |
| 2254 | 原文件低保真 |
| 2737 | 低保真 `## 23. 变更日志` |
| 2749 | 先例：`增 \| §24 \| …` 记在低保真 §23 |
| 2753–2863 | §24（含 2805 投影表、2823/2839/2757） |
| 2865 | 2026-08-28 补录 |

全文件只有一处 `### 2.3`（841），位于 800 原文件之后、1320 字段字典日志之前。页面清单没有 §2.3。V0.06 把字段字典自己的节号误认成页面清单节号，未核 798/800 边界。

本批 yes 活句落点：

| ID | 活句 | 原文件 | 日志 |
|---|---|---|---|
| 026 :2455 / 030 :2337 | 低保真 | 低保真 | §23 |
| 029 :989 / 036 :1122 | 合作模式校验、失效时间校验 | 字段字典 | §19 |
| 034 :843–849 / 035 :849 后 | 敏感字段原则 | 字段字典 | §19 |
| 034 :2805 | §24.2 投影规则 | §24（低保真之后） | §23（沿 2749） |
| 032 :2823/:2839/:2757 | §24.3 标题/数据源/章首核验 | §24 | §23（沿 2749） |
| 031 :1485–1489 | F2 mermaid | 流程图 | §17 |
| 033 七处 / 036 :2978/:3188/:3392 | 补录历史句 | 补录 | 无独立日志；文首本批摘要 |

页面清单 44–798 **无本批活句**。共同版本「§19 记 029/034/035/036；§17 记 031；§23 记 026/030/032/034；§20 不加；033/036 补录记文首」与源落点一致。**认可第二版，撤回 V0.06 对 §20 的否决。** 不新增判据。

---

### 完整差集（两席本次完整 B1 全部条目 → 候选）

| 来源 | 候选归属 | 本席核实 |
|---|---|---|
| GR-M014-T01 + CX-M014-02 | 新 M014-026，细化，缺陷是，队列是 | 差集正确。低保真 :2455 缺负责人/第一/第二；CX02 补负责人，T01 改联系人闭集。保留选填财务/运营与 028 一致 |
| GR-M014-T02 | 新 M014-027，细化，缺陷否，队列否 | 差集正确。:943/:961 原文否定「可双填」 |
| GR-M014-T03 | 新 M014-028，细化，缺陷否，队列否 | 差集正确。本席 B1 添加不成立；不发 2.2 |
| GR-M014-T04（020 原观察「其他」收费模板部分） | 新 M014-029，细化，缺陷是，队列是 | 差集正确。020 其余观察续记否 |
| GR-M014-T05 + CX-M014-03 | 新 M014-030，细化，缺陷是，队列是 | 差集正确 |
| GR-M014-T06 | 新 M014-031，细化，缺陷是，队列是 | 差集正确 |
| GR-M014-T07 | 新 M014-032，细化，缺陷是，队列是 | 差集正确。保留 2026-06-19 原文 |
| GR-M014-T08 | 新 M014-033，细化，缺陷是，队列是 | 第二版写入集合完整（七处含 :3422）；2930 前缀已收窄 |
| GR-M014-T09 + CX-M014-01 | 新 M014-034，细化，缺陷是，队列是 | 差集正确。680/871 已批查看例外 |
| GR-M014-T10 | 新 M014-035，细化，缺陷是，队列是 | 差集正确。指针承接 681 |
| CX-M014-04 | 新 M014-036，冲突，缺陷是，队列是 | 差集正确。本席 B1 漏审；:1122 或→且成立 |
| 本席 O1–O10、ponytail shrink、席甲「既有输入与非问题」 | 020 观察否（收费模板部分升 029） | 成立；不顺手去重 :1859/:1860 |
| M014-001…025 | 保持否 | 候选表未改其分类/队列；025 已修不回退 |

无漏并 ID、无多开 ID、无把观察升缺陷（029 是字段校验表现行，不是把 020 整条改「是」）。添加 0。无 HTML 申请。无锁外判据。

---

### 分类（固定顺序：冲突 → 细化 → 技术性 → 添加）

本席 B1 把 T01/T04/T05/T07/T08/T09/T10 写成冲突、T03 写成添加。B2 按 2.1 重核，**不沿用本席 B1 分类**：

- **冲突**须与三份 HTML **明文**相矛盾。遗漏、内部废止回灌、历史摘录未标，在能指向上位 HTML 条目时先归细化。GEN-017：不得因 HTML 沉默直接删除，也不得把遗漏自动升冲突。
- **026**：HTML :494「建立商户档案、运营主体、负责人、第一联系人和第二联系人。」；:2455 是表单承接漏项，并未写「不得有第一联系人」。:1304 废止的是「立法为必填分组」，拟改保留选填不等于回灌必填。**细化**。
- **027**：:943「第一联系人与原主要联系人为同一语义（避免新增对象造成歧义）」；:961「与主要联系人为同一字段语义，页面任选其一展示并保持一致」。不是两字段双填。风格去重不是工程缺陷。**细化、缺陷否、队列否**。
- **028（本票明确复核，维持细化否）**：:965/:966 已是选填用途。上位：PGM :489「谁负责、联系方式」+ :494「负责人、第一联系人和第二联系人」。财务/运营是同一商户档案联系人信息的用途细分，无新系统角色、权限点、独立实体、枚举、状态或流程分支。:1304 只废止必填分组，不废止选填字段。PGM/ENUM 对「财务联系人」「运营联系人」count=0 **不足以判添加**（2.1 能指向上位=细化）。本席 B1 添加撤回。**不发 2.2**。
- **029**：ENUM :231 / PGM :498 明文「选其他…未指定的合同不得生效」。规格对「收费模板」「未指定的合同不得生效」count=0。:989 校验口径只列六值、未写相反「可以无模板生效」。遗漏硬门控=细化。
- **030**：:2337 与同文 :1284/:2695 废止并立。HTML 状态成员沉默≠明文禁止通用门控。**细化**。
- **031**：图表可执行语义，上位 PGM-005。**细化**。
- **032**：:2757 是 2026-06-19 代码核验句；:2839 把职级表写成现行解析数据源，与 :2827/:2833/:2860/:2862 及 PGM-041 组织汇报链不一致。内部现行/历史分裂=细化。不得删 2026-06-19 原文。
- **033**：:2867 已声明补录为历史摘录载体，未逐条标注≠自动升现行。七处就地消歧=细化。
- **034**：:843 七项清单挂在「凡导出内容」；:844–848 是查看规则且漏 HTML :871/:680 已批例外。遗漏已批例外=细化。
- **035**：已有字段字典 §2.3 应承接 PGM-043 :681。规格对「暗纹」「敏感页」「导出文件」count=0。遗漏=细化。不得新造密码算法或宣称已实现。
- **036**：HTML :656「临时授权可设有效期**或**单次使用」；:1122「单次有效也应有兜底失效时间」把或收成且。明文矛盾，**冲突**。:659「临时权限申请有时间限制」与 :1309「必须支持失效时间」是申请/能力要求，拟改不得废止。:3178 补录 mermaid「填写授权原因和失效时间」属 :659 申请时限路径，**不写入** 033/036。

---

### 逐 ID

#### M014-026 PASS（分类/队列）；拟改可执行

原句 :2455 count=1。拟改补负责人、第一（主要）及电话、第二及电话，财务/运营沿字典选填、不另加必填。不改 HTML/库/页面代码。与 028 保留选填一致。

#### M014-027 PASS（否）

:943/:961 原文已排除双填。保留别名行。不作风格去重。

#### M014-028 PASS（否）

上位 HTML :494/:489。选填用途细分。不新设岗位、不升必填。与 :1304 已废止必填分组区分。添加 0。

#### M014-029 PASS（是）

原句 :989 count=1。拟改把 ENUM :231 / PGM :498 收费模板、理由、未指定不得生效写入该行校验口径，不实现新收费机制。020 仅此部分升级，其余 020 观察仍否。

#### M014-030 PASS（是）

原句 :2337 count=1。拟改收窄通用门控、保留合同未生效示例，不造状态/迁移边。

#### M014-031 PASS（是）

F–J 五节点 count 各=1，边集仍为 {A,B,C,D,E,K}。拟改加 `%%` 与同图 :1490 及 F3 :1559 先例一致，不再渲染孤立框。

#### M014-032 PASS（是）

标题 :2823 count=1；职级表数据源 :2839 count=1；:2757「到职级映射已落地」count=1。精确拟改改标题为「到审批人解析」、数据源改为组织汇报链+密钥持有人、职级仅薪资。其余段落：:2757 整段不删，前缀标「历史代码核验，非当前完成声明」，段尾指回 §24.3 三档。不改审批代码。与 020 实现观察（specific_role）不抢修。

#### M014-033 PASS（是）

七处原句 count 各=1，均在 :2867 历史摘录段、均无【已废止】。:3422 已收入，与 :3420 同一统一前缀。2930 精确拟改与其余段落均为收窄句，不废止预警记录。其余五处+挂失行用统一历史/废止前缀。不新写设备/岗位状态机。不把 :3178 并入。

#### M014-034 PASS（是）

:843–849 与 :2805 投影规则均 count=1。其余段落在 :849 后另起一段承接 HTML :871 查看例外全文要点 + :680「不授予未脱敏导出或打印、不免除留痕/敏感页」。:2805 尾追加「商户档案两联系人号码查看例外及边界同§2.3」。不发明第八项、不把查看例外扩成导出。写入点在字段字典 §2.3 与 §24.2，不是页面清单。

#### M014-035 PASS（是）

同在字段字典 §2.3 另起独立段，指针+原文要点承接 HTML :681（导出密码、不可复制/编辑、暗纹、敏感页、打开留痕），并写明「直接引用该 HTML 原文，不另造算法；本段为规格要求，不作为代码已完成声明」。顺序：:849 后先 034 段、再 035 段。

#### M014-036 PASS（分类/队列/1122 与三处历史）；边界正确

:1122 count=1，与 HTML :656 或规则及同文 :1977/:1996/:2631 相反。拟改改为「可设有效期或单次使用；采用有效期时填写，不能要求单次有效授权同时填写兜底失效时间」。:2978/:3188/:3392 各 count=1，标废止并指回现行或规则。:1309 能力要求保留。:659 申请有时间限制不写入废止集。不删有时限临时申请专门要求。

#### M014-001…025 PASS（保持否）

B4-r4 双 PASS 后队列 001–025 全否仍在 findings 历史段。020 仅收费模板观察升 029，其余续记否。019→M261 不重开。025 已修不回退。

---

### 共同版本 / 变更日志 PASS

第二版：「字段字典 §19 记 029/034/035/036；流程图 §17 记 031；低保真 §23 记 026/030/032/034（2805 投影表属于 §24，沿 2749 先例）；页面清单本批无活句改动，§20 不加；033 和 036 补录无独立日志，记录文首本批摘要。」文首追加本批 ID、标题 V0.09→V0.10。不改三 HTML/源码/测试/OpenAPI/锁。

与上节源落点表逐项相同。V0.06 要求的 §20 行因章节误认撤回。不把 027/028 写入日志。

---

### 分类 / 队列（本席本票）

| ID | 分类 | 工程缺陷 | 队列 | 本票 |
|---|---|---|---|---|
| M014-026 | 细化 | 是 | 是 | PASS |
| M014-027 | 细化 | 否 | 否 | PASS |
| M014-028 | 细化 | 否 | 否 | PASS |
| M014-029 | 细化 | 是 | 是 | PASS |
| M014-030 | 细化 | 是 | 是 | PASS |
| M014-031 | 细化 | 是 | 是 | PASS |
| M014-032 | 细化 | 是 | 是 | PASS |
| M014-033 | 细化 | 是 | 是 | PASS |
| M014-034 | 细化 | 是 | 是 | PASS |
| M014-035 | 细化 | 是 | 是 | PASS |
| M014-036 | 冲突 | 是 | 是 | PASS |
| M014-001–025 | — | — | 否 | PASS（020 部分升 029） |

添加 0。不发 2.2。无 HTML 申请。无新判据。无新功能。

---

### 全部不同意项

**无。** V0.06 三项均已按现行源与第二版候选闭合；整批 001–036 无剩余否决。不代写 findings、不执行 B3。

---

### 本席实跑（B2 第二版，只读；无写模式）

- 锁 sha256sum：`b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`
- 输入：grok 前缀 258129/`3afda398…bd48`；self 83382/`a8dcd476…b479`；findings V3.75 全文件 `f17a7dbd…964c`
- 1 文件指纹：3505 行 / 231468 / `37f60e77…f622` 与用户固定清单及本次 B1 全等
- 点读：规格 L1/L44/L782/L798/L800/L802/L841/L843–849/L943–966/L989/L1122/L1192/L1284/L1304/L1309/L1320/L1485–1490/L1559/L1977/L1996/L2237/L2337/L2455/L2631/L2695/L2737/L2749/L2757/L2805/L2823/L2839/L2862/L2867/L2930/L2966/L2978/L3138/L3158/L3178/L3188/L3388/L3392/L3420/L3422/L3424；ENUM L231；PGM L489/L494/L498/L656/L659/L680/L681/L871；锁 PGM-026/041/043、BEQR-148、GEN-016/017
- 上列拟改原句 `text.count` 均为 1（含 :3422、:2805、:2757）
- 未跑：主仓测试、psql 写、部署、修复、graft CLI
- 未改 findings/ledger/锁/源/HTML/主索引

### 结论

逐 ID：026/027/028/029/030/031/032/033/034/035/036 及 001–025 均 PASS。共同版本按源落点 PASS。

本席队列：026/029–036 是（9），027/028 否（2），001–025 否。添加 0。异议 0。

本席 B2 第二版：**PASS**

未经席甲本批亦 PASS 的合取，不得把候选当定稿，更不称模块 PASS，不得 B3。步骤仍 B2；M014 `IN_PROGRESS`；历史 B3=4 不清零，无第五批授权。即使本批 B2 双 PASS，也只表示归并定稿，其后须 `BLOCKED-3ROUNDS` 等操作员，不等于修复或模块 PASS，本席不执行 B3。不覆盖上方 B1 FAIL 与 B2 V0.06 FAIL 原文。

## M014 B5-r5回退B2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.08（本段追加；上方 V0.01–V0.07 前缀 279409 bytes / SHA256 `457b45cfc8c0d626e80e29d5382acdebfb72c228b3945cc4c8e9b9a4c09c1f4c` 原样保留，不覆盖、不改写 B1 锁段起点；本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，grok-4.6/xhigh，独立）。步骤：M014 B5-r5 回退 B2，复核 findings 顶部「当前 M014 B5-r5回退B2完整候选：M014-037（未定稿）」。不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.14；modules.md V0.04（M014 FILES 1 / 判据 84 / 计划冻结 3494 行 OVERSIZE-SINGLE-FILE；现行全文 3514 行 V0.10）
- 正本：`/home/james/complex-management` main 工作区。`HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main`=`12728763626f0de83d7d828fb6a36edfb1d3be1c`（两值不等）。本模块 1 文件标题 V0.10，3514 行，235267 bytes，SHA256 `18dcb0f1353d1476b4b2af1a2398cbfc2a9b5958a23d662ed6dce164ed62f85b`，endswith `\n`=True。本席未改该文件。
- 操作员：仅授权 r5 九项且已用完；B3 累计 **5**，无第六次授权。B2 PASS 只表示本候选可定稿并登记 `BLOCKED-3ROUNDS`，不表示可修复、B5 通过或可发布。不得把九项那一次授权再消费到 037。
- 席甲：Codex（作者自审票）；本席 FAIL 具否决权。输入：B5-r5 self V0.02 **PASS**、B5-r5 grok V0.01 **FAIL**（真实 EXIT_CODE=0）、席甲 B2 V0.11 **PASS**、findings V3.78 当前最前候选。不以自审 PASS 覆盖本席 B5 FAIL。不拿历史 001–036 候选替代本批。
- 本席本票：**PASS**
- 无 MCP、无子代理、无第三席、不问操作员。未改源码/测试/HTML/lock/findings/ledger。未 fetch/commit/push/merge/deploy。未写新产物。只追加本指定路径。未装依赖。未跑 graft CLI。

此票只判断 findings.md V3.78 顶部「当前 M014 B5-r5回退B2完整候选：M014-037（未定稿）」相对本次两份完整 B5-r5 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。下一标题「当前 M014 B4-r5 双PASS」起为已修九项历史，不定稿依据。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 B5-r5 **FAIL** 原文。B2 PASS 只表示归并定稿；其后须 `BLOCKED-3ROUNDS` 等操作员明确授权「仅 M014-037 追加一次修复」，不等于修复或模块 PASS。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M014-b5-r5-grok-raw.md` | V0.01 FAIL | 27488 | `83e5e080fbfec6c9960833502a44578ed9c62d57be0385de9078297ee56101cc` |
| `docs/audit-2026-09/modules/M014-b5-r5-self-raw.md` | V0.02 PASS | 17366 | `e9baa229664ec71e05f0d5c2049cad849eb8c67accc7ff1b855c06cfcce19777` |
| `docs/audit-2026-09/modules/M014-b1-self-raw.md` 末段 | V0.11 PASS | 84273（全文件） | `b970691ed514ad93b77e9cf580f6e9f4ac9f0faebbadff1f614e69f00c9bd70b` |
| `docs/audit-2026-09/modules/M014-b1-grok-raw.md` 前缀（本段追加前） | V0.07 | 279409 | `457b45cfc8c0d626e80e29d5382acdebfb72c228b3945cc4c8e9b9a4c09c1f4c` |
| `docs/audit-2026-09/findings.md` 顶部「当前 M014 B5-r5回退B2完整候选：M014-037（未定稿）」 | V3.78 | 1335476 | `26d0727d672e4e7023cdf4d2a8e114f65b41698194f36dd52abd042e9888da7d`（全文件） |

锁文件只读实算（本段不写独占标签行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`；sidecar 正文同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。`criteria-lock.md` 版本 V0.14；映射行 `M014 →` COUNT=84 unique=84。未改锁、不加判据。

三 HTML 只读、未改：ENUM `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`（308 行）；BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`（2032 行）；PGM `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f`（979 行）。

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

ledger 只读：M014 `IN_PROGRESS`，B3 累计 **5**。本席不改。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核规格原文/三 HTML/锁锚点/唯一性再接受或反驳；禁止表演性同意；不假定席甲并入 2053 正确；B2 不修复 |
| grilling | 只借追问树自查（差集漏并、2053 是否同根、3091 是否当前歧义、分类冲突 vs 细化、001–036 是否回队、日志落点、HTML 沉默），不问操作员、不派子代理。本体「等用户回答 / 派子代理」作废 |
| verification-before-completion | 声称前先跑哈希、原句 `count==1`、点读规格 350/364/374/381/2053/2506/2516/2876/2937/2943/3091 与 HTML 357/518/525/526、BSM 707 |
| ponytail-review | 只评最小删列/废止标记，不落地。不新建状态机；3091 不顺手改写 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（活句→同文 381/2937/2943→拟改是否唯一）；不改生产代码 |
| graft | 文档/审计产物不走 graft 索引；本步未运行 graft CLI |
| tdd / ponytail 落地 / requesting-code-review | B2 禁止修复，未落测试、未改源 |

缺口（不冒称）：`batch-grill-me` / `ubiquitous-language` 本机无独立技能。

### 完整差集（两席本次完整 B5-r5 全部条目 → 候选）

| 来源 | 候选归属 | 本席核实 |
|---|---|---|
| 本席 B5-r5 GR-01（:364/:374/:2516；建议 :2506） | 新 M014-037 | 差集正确。四处活规格 + 建议 2506 均 count=1 |
| Codex 同根全文检索 :2053 | 并入 037，不另开 ID | **确认同根**。见下节 |
| 席甲 B5-r5 无新 finding | 无新 ID | 正确 |
| 本席 B5 其他观察：027 同义、028 选填、1316 期限能力、3187 历史申请图、历史完成声明、HTML035 指针 | 不升新 ID | 已核，仍否 |
| M014-001–025 | 保持否 | 候选未改其分类/队列 |
| M014-026/029–036 | 保持否（已修） | 原九项已修，不是重新待修 |
| M014-027/028 | 保持否 | 无缺陷保留 |
| 019/020 观察 | 保留否 | 020 收费模板已由 029 解决的部分不重复计 |

无漏并 ID、无多开 ID、无把观察升缺陷。添加 0。无 HTML 申请。无锁外判据。

---

### 分类（固定顺序：冲突 → 细化 → 技术性 → 添加）

- **冲突**须与三份 HTML **明文**相矛盾。三 HTML 检索：`规则状态`/`启用准入`/`暂停准入` 命中 0。PGM:357「合同不是只归档，还会决定后续业务能不能走。」；PGM:518/525/526「把合同摘要转成准入规则」「超合同范围…先阻断」；BSM:707「把合同和基础定义变成准入规则…必须先被卡住」。HTML **没有**「不得列规则状态 / 不得有启停按钮」的反文句。HTML 沉默 ≠ 冲突（GEN-017）。
- **上位可指**：PGM-007（HTML:357 + 合同节 HTML:518–526）及 BSM-028 同向句。合同摘要转准入规则、先阻断再按层级放行是已有条目的下位页面/流程/低保真承接。**细化**正确。
- 不是添加：未发明新业务实体、新枚举、新收费或新流程分支；拟改是去掉无需求承载的状态列与启停动作。
- 不是技术性：页面清单列、关键操作、留痕清单、低保真列是业务规格文字。
- **工程缺陷成立**：同文活规格 L381「准入规则状态：本规格不定义……不另建准入规则状态机。」与补录 L2937/L2943 已废止规则状态闭集；活句 L364/L374/L2053/L2506/L2516 仍给启停动作和状态列。GEN-001（无需求承载的状态机操作）、GEN-008（同文层次不一致）、GEN-016（已废止证据写成现行结构）、PGM-007（把准入卡控写成可暂停）。「暂停准入规则」可被读成关掉卡控，与 HTML「先卡住」同向相反。
- 队列：**是**（细化且有工程缺陷）。

---

### 2053 同根确认（Codex 新检出，本席对照）

本席 B5-r5 GR-01 正文列 :364/:374/:2516，最小处置另建议 :2506。未把流程留痕清单 :2053 写入 finding 位置表。

现行 L2053 原文 count=1：`- 合同准入规则启用、暂停、阻断、升级审批。` 位于流程图原文件 §12.3「以下动作必须形成留痕」。该句把「启用、暂停」写成现行必须留痕的动作，与 L374 启停操作、L381 不建状态机是同一根：废除启停后，留痕闭集若仍要求记录启停，即把废止动作写成当前必做项（GEN-016）。并入 037、不另开 ID 正确。拟改只删「启用、暂停、」，保留「阻断、升级审批」——阻断与审批仍必须留痕，与 HTML:526 及 L350 一致。

活规格 1–2873 内，准入规则启停/规则状态现行句仅此五处（364/374/2053/2506/2516）；其余「启用、暂停」属项目/权限/商户状态成员或总览跳转，不并入 037。

---

### 3091（明确不写入本批）

现行 L3091：`- 准入规则已启用，或可按合同摘要即时检查。` count=1。位于 `## 继续合并补录（2026-08-28）` 之后，受 L2876 整体历史边界约束（历史摘录；与 HTML 冲突以 HTML 为准；未写入 HTML 的内容不因出现在本文而取得效力）。

本席 B5-r5 已写：GR-01 问题在活规格而非补录。3091 是历史流程摘录中的「已启用」措辞，带「或可按合同摘要即时检查」并列，不是现行页面清单/关键操作/留痕闭集。本轮不顺手改写、不扩 037 写入集。若日后操作员授权第六批，仍须单独判断是否就地加历史标记，不得当作本批已授权范围。

---

### 逐处原文 / 拟改 / 影响（本席结论）

共同背景 L381 count=1、L350 count=1 属对照句，不改。本次只消除「可暂停准入规则」规格歧义，不关闭合同准入检查，不新增状态或开关。

#### 1. L364 PASS

原文：`| 准入规则列表 | 合同、商户、业务域、规则状态、准入处理顺序（系统固定、只读） |` count=1  
拟改：`| 准入规则列表 | 合同、商户、业务域、准入处理顺序（系统固定、只读） |`（拟改文本当前 0 命中，可唯一替换）  
影响：去掉不再定义的规则状态列，保留合同、商户、业务域与固定检查顺序。不另造替代状态字段。

#### 2. L374 PASS

原文：`- 启用/暂停准入规则` count=1  
拟改：`- 【已废止/不得回灌】启用/暂停准入规则——本规格不另建准入规则状态机，见§9.6.5。`（当前 0 命中）  
影响：保留旧句供追溯，明确不能据此开发独立启停按钮；既有生成/编辑准入规则以及阻断后审批继续保留。本席 B5 允许删除或废止标记，候选取废止+指针，与同文 2937/2943 体例一致，最小。

#### 3. L2053 PASS（同根并入）

原文：`- 合同准入规则启用、暂停、阻断、升级审批。` count=1  
拟改：`- 合同准入规则阻断、升级审批。`（当前 0 命中）  
影响：流程留痕清单同步去掉不再采用的启停动作；阻断和审批仍必须留痕。

#### 4. L2506 PASS

原文：`- 已生效合同可生成或启用准入规则，后续业务超范围时先阻断，再按配置的更高级审核层级放行。` count=1  
拟改：`- 已生效合同可生成准入规则，后续业务超范围时先阻断，再按配置的更高级审核层级放行。`（当前 0 命中）  
影响：仅删「或启用」。L2523「从合同详情生成准入规则」已是生成口径，不必改。保持合同生效、生成规则、先阻断后按层级放行。

#### 5. L2516 PASS

原文：`| 规则列表 | 合同、商户、业务域、状态、准入处理顺序（系统固定、只读） |` count=1  
拟改：`| 规则列表 | 合同、商户、业务域、准入处理顺序（系统固定、只读） |`（当前 0 命中）  
影响：低保真列表与 364 页面清单一致。L2385「状态记录 \| 启用、暂停、终止、归档历史」属综合体**项目**低保真，不并入。

---

### 共同版本 / 变更日志 PASS

活句落点（V0.10 现行标题）：

| 活句 | 原文件 | 变更日志 |
|---|---|---|
| 364/374 | 页面清单 L46 起 | `## 20. 变更日志` L784 |
| 2053 | 流程图 L1352 起 | `## 17. 变更日志` L2244 |
| 2506/2516 | 低保真 L2262 起 | `## 23. 变更日志` L2745 |
| 无字典活句 | 字段字典 L802 起 | 不加 `## 19. 变更日志` L1326 |

若另获授权执行第六批：仅同一 MD V0.10→V0.11、文首批次记录，§20 记 364/374、§17 记 2053、§23 记 2506/2516。既有 9 项和历史 B3 全部保留；三 HTML、代码、SQL、测试不改。

---

### 分类 / 队列（本席本票）

| ID | 分类 | 工程缺陷 | 队列 | 本票 |
|---|---|---|---|---|
| M014-037 | 细化 | 是 | 是 | **PASS** |
| M014-001–025 | — | — | 否 | PASS（保持否） |
| M014-026/029–036 | — | — | 否 | PASS（已修保持否） |
| M014-027/028 | 细化 | 否 | 否 | PASS（保持否） |

添加 0。不发 2.2。无 HTML 申请。无新判据。最小改法无需调整。

---

### grilling 自答（不等操作员）

1. 2053 是否新 ID？否，与 GR-01 启停同一根。
2. HTML 无启停句是否冲突？否，沉默不升冲突；上位 PGM-007 在，归细化。
3. 3091 是否必须本批改？否，在 2876 历史边界内，本席明确不写入。
4. B2 PASS 能否 B3？否。B3=5 已耗尽，须操作员「仅 M014-037 追加一次修复」的新授权。
5. 九项授权能否延用？否。

ponytail-review（不落地）：`L374: delete/废止 启用/暂停准入规则。` `L364/L2516: shrink 去掉规则状态/状态列。` `L2053: shrink 去掉启用、暂停。` `L2506: shrink 删「或启用」。` net: −1 操作、−2 列、−2 个启停词。

### 本席实跑（B2，只读；无写模式）

- 锁 sha256sum：`b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`
- 规格：3514 / 235267 / `18dcb0f1353d1476b4b2af1a2398cbfc2a9b5958a23d662ed6dce164ed62f85b`
- 点读：规格 L350/L364/L374/L381/L784/L1326/L2053/L2244/L2385/L2506/L2516/L2523/L2745/L2876/L2937/L2943/L3091；PGM L357/L518/L525/L526；BSM L707；锁 PGM-007、GEN-001/008/016/017
- 五处原文 `text.count` 均为 1；五处拟改当前均为 0
- 未跑：主仓测试、psql 写、部署、修复、graft CLI
- 未改 findings/ledger/锁/源/HTML/主索引

### 结论

全量并集：**仅新 ID M014-037**（GR-01 四处 + 建议 2506 + Codex 同根 2053）。001–036 均不进本批队列。添加 0。

逐处 364/374/2053/2506/2516 及共同版本、3091 边界均为 PASS。最小改法无需调整。

本席 B2：**PASS**

定稿条件：席甲本批亦 PASS（其 V0.11 已 PASS）。定稿后只登记 `BLOCKED-3ROUNDS`，必须向操作员交付以上原句/拟改/影响，请求「仅 M014-037 追加一次修复」的明确授权。本席不修复。不覆盖上方 B5-r5 FAIL 原文。

## M014 B5-r7回退B2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.09（本段追加；上方 V0.01–V0.08 前缀 295001 bytes / SHA256 `6ed01f2f97220dd35a185da4712fad1ea79930d1f037745aaf538db89dca87f2` 原样保留，不覆盖、不改写 B1 锁段起点与 B5-r7 PASS 原文；本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，grok-4.6/xhigh，独立）。步骤：M014 B5-r7 回退 B2，复核 findings 顶部「当前 M014 B5-r7回退B2候选（未定稿，不可修）」。不是 B1/B5 重审、不是修复、不是模块 PASS、不是 B3/B4。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.14；modules.md V0.04（M014 FILES 1 / 判据 84 / 计划冻结 3494 行 OVERSIZE-SINGLE-FILE；现行全文 3521 行 V0.12）
- 正本：`/home/james/complex-management` main 工作区。`HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main`=`12728763626f0de83d7d828fb6a36edfb1d3be1c`（两值不等）。本模块 1 文件标题 V0.12，3521 行，236452 bytes，SHA256 `d085275e9ea1d325c3e463717a9044ea85c92e6cdcf63b8cdee252848d42583d`，`git hash-object`=`26de2735a22e056f536e767f955319a25c4b126a`，endswith `\n`=True。`git status --porcelain` 为 ` M`。本席未改该文件。
- 操作员：仅 037 日志追加一次授权已执行；B3 累计 **7**，无第 8 次授权。B2 PASS 只表示本候选可定稿并登记 `BLOCKED-3ROUNDS`，不表示可修复、B5 通过或可发布。不得把已消耗的 037 授权再消费到 029/038。
- 席甲：Codex（作者自审票）；本席 FAIL 具否决权。输入：B5-r7 self V0.01 **FAIL**、B5-r7 grok V0.01 **PASS**（真实 EXIT_CODE=0）、findings V3.82 当前最前候选。候选写明 CX-01/CX-02 仅 Codex 来源。本席 B5-r7 未报这两条，属漏检；B2 按当前文件事实独立核差集，**不回改** B5-r7 PASS 原文。不以席甲 B2 V0.12 自审票替代本席核证。
- 本席本票：**PASS**
- 无 MCP、无子代理、无第三席、不问操作员。未改源码/测试/HTML/lock/findings/ledger/index/git。未 fetch/commit/push/merge/deploy。未写新产物。只追加本指定路径。未装依赖。未跑 graft CLI。

此票只判断 findings.md V3.82 顶部「当前 M014 B5-r7回退B2候选」相对本次两份完整 B5-r7 raw 的全批差集、2.1 四分类、工程缺陷、修复队列与最小修法。下一标题「当前 M014 B4-r7双PASS」起为 001–037 全否历史快照，不定稿依据。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 B5-r7 **PASS** 原文。B2 PASS 只表示归并定稿；其后须 `BLOCKED-3ROUNDS` 等操作员新指令，不等于修复或模块 PASS。本席不修复。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M014-b5-r7-self-raw.md` | V0.01 FAIL | 15778 | `dd24be86ae5286997ba8826b39ae88f6f7b1551b4b0339c423882dee8d959052` |
| `docs/audit-2026-09/modules/M014-b5-r7-grok-raw.md` | V0.01 PASS | 26717 | `2e84eec528e37495305617f925d96ebec940f34f469171e18ebf32797fe9f1d4` |
| `docs/audit-2026-09/modules/M014-b1-self-raw.md` 全文件（含席甲本批 V0.12 段） | V0.12 PASS | 85298 | `ce28a4aae0713597cc95b4657c9c8453ac5518b9cb1bc6d6378ec7ef7e005568` |
| `docs/audit-2026-09/modules/M014-b1-grok-raw.md` 前缀（本段追加前） | V0.08 | 295001 | `6ed01f2f97220dd35a185da4712fad1ea79930d1f037745aaf538db89dca87f2` |
| `docs/audit-2026-09/findings.md` 顶部「当前 M014 B5-r7回退B2候选（未定稿，不可修）」 | V3.82 | 1338386 | `e8441b069182048b48bbc2888239b83357dccdbd93f81e1513044c1b9a42b51`（全文件） |

锁文件只读实算（本段不写独占标签行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`；sidecar 正文同值；ledger 顶部 `CRITERIA-LOCK-SHA256:` 同值；B5-r7 grok raw 第一行同值；`LOCK-AMEND-COUNT: 1`。`criteria-lock.md` 版本 V0.14；映射行 `M014 →` COUNT=84 unique=84。未改锁、不加判据。

三 HTML 只读、未改：ENUM `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`（308 行）；BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`（2032 行）；PGM `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f`（979 行）。

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

ledger 只读：M014 `IN_PROGRESS`，B3 累计 **7**。本席不改。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核规格原文/三 HTML/锁锚点/唯一性再接受或反驳；禁止表演性同意；可否决分类/ID/最小处置；B2 不修复 |
| grilling | 只借追问树自查（差集漏并、029 回补 vs 新 ID、038 vs 012、分类冲突 vs 细化、037 是否回队、日志落点、HTML 沉默），不问操作员、不派子代理。本体「等用户回答 / 派子代理」作废 |
| verification-before-completion | 声称前先跑哈希、原句 `count==1`、点读规格 950/984/1000/1335/1371/1375–1385/1418–1420/368/378/385/2058/2512/2522/2755 与 PGM 339/492、BSM 596、锁 GEN-016/PGM-003/026/BSM-009 |
| ponytail-review | 只评最小改词，不落地。不改合作模式业务正文、不改 F11 图、不扩 验收表 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（日志定位→字典标题；「十条」→表行计数）；不改生产代码 |
| graft | 文档/审计产物不走 graft 索引；本步未运行 graft CLI |
| tdd / ponytail 落地 / requesting-code-review | B2 禁止修复，未落测试、未改源 |

缺口（不冒称）：`batch-grill-me` / `ubiquitous-language` 本机无独立技能。

### 完整差集（两席本次完整 B5-r7 全部条目 → 候选）

| 来源 | 候选归属 | 本席核实 |
|---|---|---|
| 席甲 B5-r7 CX-01 | 回补已有 ID M014-029（当前队列否 → 经 B2 再入队） | 差集正确。L1335 `§7 合作模式` count=1；字典 L950 为 `## 7. 商户/运营主体字段`，L984 为 `## 8. 合同与合作关系字段`，L1000 合作模式收费模板句 count=1。§7 块无 `| 合作模式 |` 行 |
| 席甲 B5-r7 CX-02 | 新 ID M014-038 | 差集正确。L1371「十条流程」count=1；L1375–1385 连续 F1–F11 共 11 行；拟改「十一条流程」当前 0 命中 |
| 本席 B5-r7 | 临时 findings 无 | 属漏检，不伪造本席当时已发现；不回改 B5-r7 PASS |
| M014-012 | 不并入 038、不回退 | 原定义：总览表已有 F11，关系 mermaid 无 F11 节点/边；现行 L1418 节点、L1419 `L --> J`、L1420 `L --> K` 均 count=1。012 拟改已落地 |
| M014-037 | 保持否 | 正文五处 + 低保真日志 L2755 `§10.3 / §11.1` 均保留且 count=1 |
| M014-001–028、030–036 | 保持否 | 候选未改其分类/队列（029 除外） |
| 019/020 观察 | 保留否 | 020 收费模板已由 029 字段行解决的部分不重复计 |

无漏并 ID、无多开 ID、无把观察升缺陷。添加 0。无 HTML 申请。无锁外判据。

B5 合取：self FAIL + grok PASS = FAIL，整批回 B2。符合 §4「无 ID 或已有 ID 但不在修复队列 → 须经 B2」：029 已有 ID 且当前否；038 无 ID。

---

### 分类（固定顺序：冲突 → 细化 → 技术性 → 添加）

#### M014-029（CX-01）

- **冲突**：三 HTML 检索「十条」/「十一条」与日志节号无关；PGM:492 只规定合作六类及「其他」收费模板生效条件，**没有**「变更日志必须写 §7」的反文句。日志写错节号不构成与 HTML 明文矛盾。不升冲突。
- **上位可指**：PGM-026（HTML:492「主要功能」）原文含「区分自营、租赁、联营、代运营、服务商接入、其他等合作方式」及「其他」须在生效前指定收费模板。字典 §8 L1000 正是该条下位字段；L1335 是该次 029 修复的字典 §19 日志。**细化**成立。
- 不是添加：拟改只改日志定位词，不发明合作模式、收费项或流程。
- 不是技术性：2.1 能指到 HTML 上位则先归细化，不进技术性通道。
- **工程缺陷成立**：GEN-016。字典 §19 日志「位置」写 `§7 合作模式`，同文件字典 §7 是商户/运营主体（L950）；收费模板生效条件写在 §8 L1000。§7 仅有「商户类型｜区分合作模式」（L967）与「合作模式摘要」（L969），**均无** 029 写入的收费模板句。指针会把读者带到错误表。类型 4：行号点读 + `text.count`。
- 队列：**是**（细化且有工程缺陷）。原 029 字段行已修、队列曾否；本条是 r5 修复附带日志错误，属「已有 ID 但不在修复队列」，经 B2 回补同一 ID，不另开号（对照 037 B4-r6 日志节号沿用 037）。

#### M014-038（CX-02）

- **冲突**：三 HTML「十条」「十一条」「F11」命中均为 0。HTML 未写死流程条数。不升冲突。
- **上位可指**：PGM-003（HTML:339 模块入口）与 BSM-009（HTML:596「管理综合体、空间、商户、合同、员工、设备、权限」）。流程图原文件 §2「P0 应先打通以下…流程」是该基础治理闭集的下位目录句。**细化**成立。
- 不是添加：拟改只改数词，不增删 F1–F11、不新造流程。
- 不是 012：012 是技术性/图结构（mermaid 缺 F11 节点/边，判据 PGM-010/GEN-002/GEN-016），最小处置「关系图加 F11 节点并连到留痕与后续业务」。现行图已含节点与到后续业务/留痕的边。本条是总览**数量句**与表 11 行不一致，修改点不同。独立新 ID 正确，已修图不回退。
- **工程缺陷成立**：GEN-016。L1371 声明「十条」，紧随表 L1375–1385 为 F1–F11 十一行；F11 专节 L2101 `## 12A. F11` 存在。类型 4：表行计数。
- 队列：**是**。

两处均无业务发明、无 HTML 改动申请。

---

### 原 012 / 原 029 对照（独立去重）

#### 原 M014-012（findings 历史定义，不回队）

- 事实栏：`:1358` 总览表有 F11；`:1362-1391` 关系 mermaid 无 F11 节点/边
- 分类：技术性；队列当时是；判据 PGM-010/GEN-002/GEN-016
- 最小处置：关系图加 F11 节点并连到留痕与后续业务
- 现行：L1418 `A --> L["F11 终端开关启停、灰度与被拒访问"]`；L1419 `L --> J`；L1420 `L --> K`；流程日志 L2257 已记「总览关系图补 F11」
- 本席：012 已修完。数量句当时表已有 F11 却未写入 012 拟改，属另一修改点，不得并回 012、不得拆已修边

#### 原 M014-029（findings 第二版完整候选定义）

- 原句：`| 合作模式 | 确定经营关系类型 | 必填 | 选择 | 租赁、联营、自营、代运营、服务商接入、其他 | 变更需留痕 |`
- 拟改：在选项后追加「其他」收费模板/理由/未指定不得生效
- 现行 L1000 已是拟改全文，count=1
- 本席：字段行保持已修，**禁止**再改 L1000 / 合作六类 / 收费模板正文。本次只回补该次 B3-r5 写进字典 §19 的位置列

---

### 逐处原文 / 拟改 / 影响（本席结论）

#### 1. M014-029 L1335 PASS（回补；只改位置列中「§7 合作模式」）

原文 count=1：

```text
| 2026-09-23 | 改 | §7 合作模式 / §2.3 敏感字段 / §11 失效时间 | 承接其他合作模式收费模板生效条件；同步两联系人号码查看例外与HTML敏感页保护；去掉单次授权必须同时有失效时间的额外限制 | M014-029/034/035/036；现锁B2双PASS、操作员追加一次 |
```

拟改（「§8 合作模式」当前 0 命中，可唯一替换）：

```text
| 2026-09-23 | 改 | §8 合作模式 / §2.3 敏感字段 / §11 失效时间 | 承接其他合作模式收费模板生效条件；同步两联系人号码查看例外与HTML敏感页保护；去掉单次授权必须同时有失效时间的额外限制 | M014-029/034/035/036；现锁B2双PASS、操作员追加一次 |
```

影响：日志指向字典 §8 合同与合作关系字段（L984 标题、L1000 合作模式行）。同格 `§2.3 敏感字段`（L848）与 `§11 失效时间`（L1133 036 现行句）count=1，保持不改。不改 L1000 业务正文，不改 HTML。

#### 2. M014-038 L1371 PASS（新 ID）

原文 count=1：

```text
平台治理层 P0 应先打通以下十条流程：
```

拟改（「十一条流程」当前 0 命中）：

```text
平台治理层 P0 应先打通以下十一条流程：
```

影响：数词与表 L1375–1385 十一行及 F11 专节一致。不增删流程、不改 mermaid、不改 P0 验收表（验收点 13 行是另一对象，不并入）。版本号若另获授权执行第 8 次 B3 再随批次递增；本席不写版本。

037 回归（保持否，不改）：

| 活句 | 现行 count=1 |
|---|---|
| L368 准入规则列表无规则状态列 | 是 |
| L378 启用/暂停标废止 | 是 |
| L385 不另建状态机 | 是 |
| L2058 留痕为阻断、升级审批 | 是 |
| L2512 无「或启用」 | 是 |
| L2522 规则列表无状态列 | 是 |
| L2755 低保真日志 `§10.3 / §11.1` | 是 |

---

### 分类 / 队列（本席本票）

| ID | 分类 | 工程缺陷 | 队列 | 本票 |
|---|---|---|---|---|
| M014-029 | 细化 | 是 | 是 | **PASS**（回补；只改日志 §7→§8） |
| M014-038 | 细化 | 是 | 是 | **PASS**（新 ID；十条→十一条；不并 012） |
| M014-037 | — | — | 否 | PASS（保持否；正文与日志均正确） |
| M014-001–028、030–036 | — | — | 否 | PASS（保持否；012 图不回退） |

添加 0。不发 2.2。无 HTML 申请。无新判据。最小改法无需调整。否决项 0。

---

### grilling 自答（不等操作员）

1. 029 应新开号吗？否。字段行已修；缺陷是该 ID 的 r5 日志定位。§4「已有 ID 但不在修复队列」走 B2，仅无 ID 者分配新 ID。037 日志节号沿用 037 为同构先例。
2. 038 应并入 012 吗？否。012 拟改是图节点/边，已在 L1418–1420；本条是数量句。
3. HTML 无「十一条」是否冲突/添加？否。沉默不升冲突；拟改不发明流程。
4. 本席 B5 PASS 是否挡住本批？否。合取已因席甲 FAIL 回 B2；B2 核的是差集事实，不回写 B5。
5. B2 PASS 能否 B3？否。B3=7 已耗尽，须操作员新指令。037 那一次授权不可延用。
6. 要不要改 §7 合作模式摘要？否。摘要无收费模板句，不是 029 修改点。
7. 要不要改验收表条数？否。验收点 ≠ F1–F11 目录句。

ponytail-review（不落地）：`L1335: shrink §7 合作模式 → §8 合作模式。` `L1371: shrink 十条 → 十一条。` net: 0 行，2 个定位词。

### 本席实跑（B2，只读；无写模式）

- 锁 sha256sum：`b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`
- 规格：3521 / 236452 / `d085275e9ea1d325c3e463717a9044ea85c92e6cdcf63b8cdee252848d42583d`
- 点读：规格 L368/L378/L385/L848/L950/L967/L969/L984/L1000/L1133/L1335/L1371/L1375–1385/L1418–1420/L2058/L2101/L2257/L2512/L2522/L2755；PGM L339/L492；BSM L596；锁 GEN-016、PGM-003/026、BSM-009、原 012/029 定义
- L1335/L1371/L1000/F11 节点与边 `text.count` 均为 1；拟改「§8 合作模式」「十一条流程」当前均为 0
- 三 HTML「十条」「十一条」均为 0
- 未跑：主仓测试、psql 写、部署、修复、graft CLI
- 未改 findings/ledger/锁/源/HTML/主索引

### 结论

全量并集：**回补 M014-029**（CX-01，仅日志 §7→§8）＋ **新 ID M014-038**（CX-02，十条→十一条）。037 及其余 001–028/030–036 均不进本批队列。添加 0。012 已修图不回退。

逐处 029/038 及 037 回归均为 PASS。最小改法无需调整。分类、ID、队列、最小处置本席均接受，无否决。

本席 B2：**PASS**

定稿条件：席甲本批亦 PASS（其 self-raw V0.12 已 PASS）。定稿后只登记 `BLOCKED-3ROUNDS`，必须向操作员交付以上原句/拟改/影响，请求仅 029 日志定位与 038 数词的新修复授权。本席不修复。不覆盖上方 B5-r7 **PASS** 原文。

## M014 B5-r8回退B2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.10（本段追加；上方 V0.01–V0.09 前缀 311639 bytes / SHA256 `50a256058b737bfd2bd35a7ce07eee993c52afe4b8081a5939cfa4d4c0800ae3` 原样保留，不覆盖、不改写 B1 锁段起点与 B5-r8 FAIL 原文；本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，grok-4.6/xhigh，独立）。步骤：M014 B5-r8 回退 B2，复核 findings 顶部「当前 M014 B5-r8回退B2：025同根再核，未定稿」。不是 B1/B5 重审、不是修复、不是模块 PASS、不是 B3/B4。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.14；modules.md V0.04（M014 FILES 1 / 判据 84 / 计划冻结 3494 行 OVERSIZE-SINGLE-FILE；现行全文 3521 行 V0.13）
- 正本：`/home/james/complex-management` main 工作区。`HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main`=`12728763626f0de83d7d828fb6a36edfb1d3be1c`（两值不等）。本模块 1 文件标题 V0.13，3521 行，236455 bytes，SHA256 `020cceb047d5511a65dd8c08d2f06425d52727ee86cca6e769b8f96eeaf84017`，`git hash-object`=`b5dcd8188d49b26e7efcaa7ae4b771c603666b4d`，endswith `\n`=True。本席未改该文件。
- 操作员：B3 累计 **8**，无第 9 次授权。本轮 B2 任何结论都不改源。队列若「是」只能登记 `BLOCKED-3ROUNDS` 等新指令；队列空且双 PASS 后仍须同 r8 再做完整 B5。不得把已消耗的 029/038 授权再消费到 025。
- 席甲：Codex（作者自审票）；本席 FAIL 具否决权。输入：B5-r8 self V0.01 **PASS**、B5-r8 grok V0.01 **FAIL**（完整 277 行）、findings V3.85 当前最前候选。候选对 GR-01 提出反证，本席按当前正文独立核实后**接受**（细化 / 工程缺陷否 / 队列否），不把 B5-r8 FAIL 原文改写成 PASS。不以席甲 self-raw V0.13 自审票替代本席核证。
- 本席本票：**PASS**
- 无 MCP、无子代理、无第三席、不问操作员。未改源码/测试/HTML/lock/findings/ledger/index/git。未 fetch/commit/push/merge/deploy。未写新产物。只追加本指定路径。未装依赖。未跑 graft CLI。

此票只判断 findings.md V3.85 顶部「当前 M014 B5-r8回退B2：025同根再核」相对本次两份完整 B5-r8 raw 的全批差集、2.1 四分类、工程缺陷、修复队列与最小修法。下一标题「当前 M014 B4-r8双PASS」起为 001–038 全否历史快照，不定稿依据。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 B5-r8 **FAIL** 原文。B2 PASS 只表示归并定稿；队列空则跳过 B3/B4，仍须同 r8 完整 B5。本席不修复。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M014-b5-r8-self-raw.md` | V0.01 PASS | 15334 | `edcd60aab89679c1d11d0175324862736d8663eee40175ec7ed3e7074c944fcc` |
| `docs/audit-2026-09/modules/M014-b5-r8-grok-raw.md` | V0.01 FAIL | 19587 | `44aa455d407e5d9e4f07bd9ef81b14d16f5df2dd038c9a78eb580490001d9345` |
| `docs/audit-2026-09/modules/M014-b1-grok-raw.md` 前缀（本段追加前） | V0.09 | 311639 | `50a256058b737bfd2bd35a7ce07eee993c52afe4b8081a5939cfa4d4c0800ae3` |
| `docs/audit-2026-09/findings.md` 顶部「当前 M014 B5-r8回退B2：025同根再核，未定稿」 | V3.85 | 1341668 | `321ba39fcd0319bfa5e9ec9654870de5e2bc99c4659e59a80cf0a34ee0b93b39`（全文件） |

锁文件只读实算（本段不写独占标签行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`；sidecar 正文同值；ledger 顶部 `CRITERIA-LOCK-SHA256:` 同值；B5-r8 grok raw 第一行同值；`LOCK-AMEND-COUNT: 1`。`criteria-lock.md` 版本 V0.14；映射行 `M014 →` COUNT=84 unique=84。未改锁、不加判据。

三 HTML 只读、未改：ENUM `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`（308 行）；BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`（2032 行）；PGM `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f`（979 行）。

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

ledger 只读：M014 `IN_PROGRESS`，B3 累计 **8**。本席不改。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核规格原文/三 HTML/锁锚点/唯一性再接受或反驳；禁止表演性同意；可否决分类/ID/缺陷/队列；B2 不修复。本票接受候选对 GR-01 的反证 |
| grilling | 只借追问树自查（差集漏并、025 同根 vs 新 ID、L2584 是否重立废止契约、同名是否等于回灌、分类冲突 vs 细化、001–038 是否回队、HTML 沉默），不问操作员、不派子代理。本体「等用户回答 / 派子代理」作废 |
| verification-before-completion | 声称前先跑哈希、原句 `count==1`、点读规格 493/504/509/1091/1092/1113/1279/1281/1335/1371/1375–1385/1418–1420/1924/2058/2512/2522/2583–2585/2591/2592/2603/2624/2755 与 PGM 362–363/481、BSM 709/940/1588、锁 GEN-008/016/PGM-008/BSM-028/BEQR-157/158 |
| ponytail-review | 只评可删，不落地。接受缺陷否之后，不把 L2584 改成「异常记录」或加废止标记（那是发明列名/删非门控记录面） |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（GR-01 声称的录入/必填 → 当前 L2584 有无该动词/校验 → 废止契约落在 1092/2591 而非 2584）；不改生产代码。B2 禁止修复，不走技能本体的插桩/最小改码 |
| codebase-design | 删除测试：删掉 L2584「异常说明」一词会丢掉 025 明确保留的设备未归还等事项展示位；复杂度回到页面清单 L493「异常记录」与 L2592 提示句。不派 DESIGN-IT-TWICE |
| writing-for-agents | 本 raw 写给下游席读：差集、契约表、完成判据同文件 |
| graft | 文档/审计产物不走 graft 索引；本步未运行 graft CLI |
| tdd / ponytail 落地 / requesting-code-review | B2 禁止修复，未落测试、未改源 |

缺口（不冒称）：`batch-grill-me` / `ubiquitous-language` 本机无独立技能。`git-guardrails-claude-code` 本宿主无 Claude Code hooks。

### 完整差集（两席本次完整 B5-r8 全部条目 → 候选）

| 来源 | 候选归属 | 本席核实 |
|---|---|---|
| 本席 B5-r8 GR-01（L2584；对照 493/1092/1279/2585/2591） | 既有 M014-025 同根再核，不分新 ID | 差集正确。唯一新主张。§4「已有 ID 但不在修复队列 → 须经 B2；仅无 ID 者分配新 ID」 |
| 席甲 B5-r8 | 临时 findings 无 | 正确。合取因本席 FAIL 回 B2 |
| 本席 B5-r8 观察：027 同义、028 选填、019/020、3091 补录、F7 L1871/L1872 同义两行、导航「权限留痕」/「权限审计」 | 不升新 ID | 已核，仍否 |
| M014-001–024、026–038 | 保持否 | 候选未改其分类/队列 |
| M014-025 历史五处写入 | 保持已修 | 见回归节；本批不回退、不扩写入集 |

无漏并 ID、无多开 ID、无把观察升缺陷。添加 0。无 HTML 申请。无锁外判据。

B5 合取：self PASS + grok FAIL = FAIL，整批回 B2。符合 §4「无 ID 或已有 ID 但不在修复队列」：GR-01 无定稿新 ID，主题落在已修且当前队列否的 025。

---

### 原意（GR-01 主张 vs 025 已定稿边界 vs 当前正文）

本席 B5-r8 GR-01 原文主张四句：

1. L2584 把「异常说明」写成现行低保真**结构字段**。
2. 与字典 L1092「不采用」、页面清单 L493「异常记录」、同表 L2585「异常记录」不一致。
3. 实现会按详情区重新做出班次「异常说明」**录入**。
4. 最小处置：L2584「异常说明」改为「异常记录」，或就地标废止并指向字典 §9.3。

025 已定稿缺陷（findings 历史段 + 本席 B2-r4 V0.07 写入集，当时行号）是**班次「异常」状态门控**：

| 当时行 | 现行行 | 当时活契约 | 025 处置 |
|---|---|---|---|
| :484 | L492 | 班次列表含「状态」列 | 去列 |
| :496 | L504 | 关键操作「记录异常班次」 | 标【已废止/不得回灌】 |
| :1078 | L1092 | 字段用途「记录异常班次原因」+ 校验「班次为异常时必填」 | 整行不采用，字段名保留作墓碑 |
| :2565 | L2583 | 低保真列表含「状态」列 | 去列 |
| :2573 | L2591 | 「异常班次必须填写异常说明」 | 标【已废止/不得回灌】 |

025 **明确排除**当时 :2566（今 L2584）「班次详情｜…、异常说明」：低保真详情列，无必填/门控；1078 不采用后仍可作设备未归还等事项的展示位；扩改会发明新列名或删掉非门控记录面。同批排除 :485（今 L493）、:2567（今 L2585）、:2574（今 L2592）。该排除是原语义对照，不是本轮免审。本席按当前 3521 行重新取证。

---

### L2584 是否重立已废止契约（类型 4）

现行原文 `text.count==1`：

```text
L493| 班次详情 | 上岗人员、责任点位、设备领用、异常记录 |
L504|- 【已废止/不得回灌】记录异常班次——三 HTML 无班次状态机
L509|班次状态：本规格不定义——三份业务 HTML 无对应状态枚举。班次绑定责任区域与设备领用，用于责任留痕。
L1092| 异常说明 | 【已废止/不得回灌】记录异常班次原因及「班次为异常时必填」 | 不采用 | — | 本规格不定义班次状态——三份业务 HTML 无对应状态枚举 | — |
L1113| 异常说明 | 记录损坏、丢失、未归还原因 | 条件必填 | 多行文本 | 异常状态时必填 | 变更需留痕 |
L2583| 班次列表 | 日期、开始时间、结束时间、员工、岗位、区域 |
L2584| 班次详情 | 当班员工、主管、责任区域、设备领用、异常说明 |
L2585| 交接信息 | 未完成事项、下一班次、异常记录 |
L2591|- 【已废止/不得回灌】异常班次必须填写异常说明——三 HTML 无班次状态机。
L2592|- 班次结束前提示未归还设备或未处理异常。
L2624|- 异常归还必须填写异常说明。
```

L2584 所在表头是「区域 \| 低保真内容」，不是字段字典的「字段名称 / P0 / 录入方式 / 校验口径」。本席对 L2584 逐词核：`填写`/`必填`/`录入`/`输入`/`状态`/`异常班次` 均为 False。

| 已废止契约 | 权威落点 | L2584 是否重立 |
|---|---|---|
| 记录异常班次原因 | L1092 用途（已冠废止） | 否。无该用途句 |
| 班次为异常时必填 | L1092 校验（已冠废止） | 否。无必填/条件/「班次为异常」 |
| 异常班次必须填写异常说明 | L2591（已冠废止） | 否。该禁令仍在，未撤回 |
| 关键操作「记录异常班次」 | L504（已冠废止） | 否 |
| 班次列表「状态」列 | L492 / L2583 | 否。两表均无「状态」 |
| 班次五态含「异常」 | L1225 / L1860 / 补录 L3472 | 否。均不定义或已废止 |

同名「异常说明」的**活**契约在设备域，不是班次状态机：

- 字典 L1113：记录损坏、丢失、未归还原因；异常状态时必填（BEQR-158 设备七态）
- 覆盖表 L1281：设备领还核心字段含异常说明
- 低保真 L2624：异常归还必须填写异常说明
- F8 L1924：`K -- "否" --> M["填写异常说明"]`，判定框是「设备是否正常」

L2592 活交互「班次结束前提示未归还设备或未处理异常」与 BSM `:1588`「设备未归还、挂失、跨班次使用」同向。L2584 与「设备领用」并列，读作该事项展示位，与 025 排除理由一致。

页面清单 L493 用「异常记录」、低保真 L2584 用「异常说明」、交接 L2585 用「异常记录」：三处都无类型/可空/枚举/必填。页面清单与低保真本就粒度不同（L2584 多「主管」；「上岗人员」对「当班员工」）。仅同名/近义不足以证明重建 L1092 那条已废止字段。把 L2584 改成「异常记录」或加废止标记，正是 025 已拒绝的「发明新列名或删掉非门控记录面」。

「实现会按详情区重新做出录入」不是当前规格已写明的行为。关键交互里唯一「必须填写异常说明」的活句是 L2624 设备异常归还；班次侧对应句在 L2591，已经废止。不得用假设录入充当已明确规格。

GEN-008 原文管的是前后端/库/OpenAPI/迁移/DTO/夹具对名称、类型、可空、枚举及错误语义一致。文档模块可类比「同一契约不得互相否定」。L2584 未给出与 L1092 相反的类型/可空/枚举/必填，只给出展示位名词。GEN-016 禁止把旧证据写成当前事实；L2584 没有把「班次为异常时必填 / 记录异常班次原因」写成现行用途。PGM-008 HTML `:363` 是「管理员工注册、私人手机设备绑定、工作手机、企微和岗位状态」——详情区展示异常事项不另造岗位/班次状态。

三 HTML：`异常班次`/`班次异常`/`异常说明`/`班次状态` 命中 0。HTML 沉默不等于「详情不得出现异常事项」（GEN-017）。上位仍在：BSM-028 `:709`「管理员工、岗位、班次…」、BSM `:940` 确认班次/责任区域/设备、BSM `:1588` 设备未归还、PGM `:481` 设备和员工班次要知道责任区域。

**工程缺陷不成立。** 025 原排除在当前正文仍成立。候选「细化 / 缺陷否 / 队列否」接受。不坚持 GR-01 入队，故不给新的最小处置。

---

### 分类（固定顺序：冲突 → 细化 → 技术性 → 添加）

#### M014-025（GR-01 同根再核）

- **冲突**：须与三份 HTML **明文**相矛盾。三 HTML 无「不得列出异常说明 / 不得展示异常事项」反文句。不升冲突。
- **上位可指**：BSM-028（HTML:709 管理班次）与 PGM-008（HTML:362–363 员工和手机）。班次页信息结构是已有条目的下位页面/低保真承接。**细化**成立。
- 不是添加：未发明班次状态、必填门控或新流程。候选保持展示位，不新开字段。
- 不是技术性：2.1 能指到 HTML 上位则先归细化。
- **工程缺陷否**：见上节契约表。GEN-008/016 打不穿；PGM-008 不因名词同名而 FAIL。
- 队列：**否**（细化且无工程缺陷 → 定稿置否）。025 历史五处已修，本批不回队、不扩写。

#### M014-001–024、026–038（保持否）

见下一节逐族核验。本次两席无把它们重新入队的依据。

两处均无业务发明、无 HTML 改动申请。

---

### 001–038 现行核验（保持否；不是一句带过）

| ID 族 | 本席按 V0.13 点读 | 队列 |
|---|---|---|
| 001–018、021–026、030–036 | 对应废止标记、闭集、V394/dealer_portal/gate 现状行、F11 图、投影例外、单次授权失效时间等仍在现行层；本批无回归主张 | 否 |
| 019/020 | 观察保留：本文件无独立 `organizations` 表立法；跨模块实现观察不是本文件新发明闭集 | 否 |
| 024 | 启用岗位/职级废止句仍在；不回退 | 否 |
| **025** | 五处写入仍在：L492/L2583 列表无「状态」；L504/L1092/L2591 均带【已废止/不得回灌】。L2584 仍是被排除的详情展示位，不构成未修完 | **否**（再核后仍否） |
| 027 | L972「与主要联系人同一字段语义、页面任选其一展示并保持一致」count=1 | 否 |
| 028 | L976/L977 财务/运营联系人选填；L2469 表单与字典一致 | 否 |
| 029 | L1335「§8 合作模式」count=1，对 L984 标题 / L1000「其他」收费模板正文；业务正文未改 | 否 |
| 037 | L368 无规则状态列；L378 启停废止；L385 不另建状态机；L2058 留痕为阻断、升级审批；L2512 无「或启用」；L2522 无状态列；L2755 日志 `§10.3 / §11.1` | 否 |
| 038 | L1371「十一条」；L1375–1385 共 11 行 F1–F11；L1418–1420 F11 节点与边均 count=1；标题 V0.13 | 否 |
| 012 | 已修图不回退（L1418 `A --> L["F11…"]` / L1419 `L --> J` / L1420 `L --> K`） | 否 |

F7 L1871/L1872 同义两行、导航用词并列：行为相同，不构成可执行冲突，不升 ID。L3091 在补录 `~~~~text`、节首 L2883–2885 历史边界内，不升活。

---

### 分类 / 队列（本席本票）

| ID | 分类 | 工程缺陷 | 队列 | 本票 |
|---|---|---|---|---|
| M014-025 | 细化 | **否** | **否** | **PASS**（GR-01 同根再核；接受反证；不回队、不新开 039） |
| M014-001–024、026–038 | 保持既有分类 | 保持历史结论 | 否 | PASS（保持否；029/038 已修不回退，037 正文与日志正确，012 图不回退，019/020 观察，027/028 无缺陷） |

添加 0。不发 2.2。无 HTML 申请。无新判据。无可执行最小处置。否决项 0。

---

### grilling 自答（不等操作员）

1. GR-01 应新开 039 吗？否。主题是班次「异常说明」；§4 仅无 ID 者分配新号。对已排除展示位再核，沿用 025。
2. 同名「异常说明」是否等于重建 L1092？否。L1092 废止的是用途+「班次为异常时必填」；L2584 无该用途、无必填、无状态。
3. HTML 零命中能否禁止详情展示异常事项？否。GEN-017；BSM :1588 有设备未归还明文。
4. 「实现会做录入」能否当缺陷？否。当前规格未写班次详情录入/必填；活的「必须填写异常说明」在 L2624 设备异常归还。
5. 本席 B5 FAIL 是否挡住本批 B2 PASS？否。B5 原文保留；B2 核的是差集能否入队。接受反证后队列空。
6. B2 PASS 能否 B3？否。队列空则跳过 B3/B4；B3=8 已耗尽。仍须同 r8 再做完整 B5。
7. 要不要把 L2584 改成「异常记录」以求字面一致？否。025 已判那是发明列名。

ponytail-review（不落地）：`Lean already. Ship.` 对 L2584 无删除项。`net: -0`。

### 本席实跑（B2，只读；无写模式）

- 锁 sha256sum：`b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`
- 规格：3521 / 236455 / `020cceb047d5511a65dd8c08d2f06425d52727ee86cca6e769b8f96eeaf84017`
- 点读：规格 L368/L378/L385/L492–L509/L793/L950/L972/L976–L977/L984/L1000/L1091–L1092/L1113/L1223–L1225/L1279/L1281/L1335–L1336/L1371/L1375–L1385/L1418–L1420/L1860/L1871–L1872/L1924/L2058/L2469/L2512/L2522/L2583–L2585/L2591–L2592/L2603/L2618/L2624/L2755/L2757/L2883–L2885/L3472；PGM L363/L481；BSM L709/L940/L1588；ENUM L279；锁 GEN-008/016/017、PGM-008、BSM-028、BEQR-157/158
- L2584/L493/L1092/L2591/L2592/L1113/L2624/L1335/L1371 `text.count` 均为 1
- L2584 不含 填写/必填/录入/输入/状态/异常班次
- 三 HTML「异常班次/班次异常/异常说明/班次状态」均为 0
- 未跑：主仓测试、psql 写、部署、修复、graft CLI
- 未改 findings/ledger/锁/源/HTML/主索引

### 结论

全量并集：**仅 GR-01 → 既有 M014-025 同根再核**。分类细化；工程缺陷否；队列否。001–024、026–038 均不进本批队列。添加 0。

本席先前 B5-r8 把展示位名词写成「结构字段」并推定录入，对当前正文过严；按 receiving-code-review 先核实再接受候选反证。B5-r8 FAIL 原文仍在，本段不覆盖。

本席 B2：**PASS**

定稿条件：席甲本批亦 PASS（其 self-raw V0.13 已写 PASS，本席未以其替代核证）。定稿后修复队列空，跳过 B3/B4，B3 仍为 8，须同 r8 再做完整 B5 双审。不得第 9 次修复。本席不修复、不动源。不覆盖上方 B5-r8 **FAIL** 原文。
