CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M014 B5-r1 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.01（指定路径原先不存在，本次新建完整报告）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员、不读本轮 self-raw）
- 步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B4-r1 双 PASS、B3 自报红→绿或 B1 历史票替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M014 同一 1 文件；`runbook.md` V0.02；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 模块：M014 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第2段（OVERSIZE-SINGLE-FILE）
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值（889020 字节）；sidecar 全文一行同值。独占锁行仅本文第一行，正文不再出现同名独占字段。
- 映射：`M014 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-189, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-060, PGM-065, PGM-066]`，COUNT=84 unique=84 MISSING=[] EXTRA=[]；与锁映射行 L6393 逐字相同；与本席 B1 所列逐字相同；定义与 HTML 出处均从锁自读，不新造判据、不 LOCK-AMEND
- 正本检出：`HEAD=origin/main=685dca4e1088ed59e68196c3c5b45787fde9db02`（PR #434 M013 闭环检查点已合并）。B3-r1 after tree 对象 `82136f01b088c2bf59a5b480a92e2bc4c71f9923` 可解析为 tree；本模块 1 文件当前工作区 blob `ed8b55d19314c59be042ebb511710fbdb0185af4` 与该 after 全等。HEAD blob 仍为 before `0632c0fbdb1683ce7d6c7496375dfeb4788e07e0`（B3 未提交）
- 本席约束：只读业务代码与计划；除本 raw 外不写文件；不修源/三 HTML/锁/findings/ledger；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；主仓 cwd 不写模式（验证在 `/tmp/m014-b5-r1-verify`，结束已 `rm -rf`）
- 未读取：`M014-b5-r1-self-raw`（当前亦不存在）。B4-r1 双席 raw 与 findings V1.90 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界，不代替本轮全量重审）：findings V1.90 — 001–018 已修转「否」（冲突 001/003/006；细化 002/004/005/007/009/011/014/015/016；技术性 008/010/012/013/017/018）；019 为 XM→M261、020 仅登记。B2-r1 定稿禁止项（不得在 B5 升格为缺陷）：改源码/迁移/OpenAPI/三份 HTML、新增字段或接口、删除补录历史原文、为日期化历史标注（头部 R5 标注、§24 代码核验）重写现状（除定稿明示者外）。当前修复队列「是」=0。

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问 2.2、无锁外判据、无 HTML 修改申请。001–018 在当前单文件上独立复验成立；019 维持 XM→M261；020 维持仅登记。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权——本席不行使否决。

---

## 0. 技能自核（V0.17 §6，按 B5 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体。本步禁止子代理，与技能「先派子代理」冲突处按用户指令收窄 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `productivity/grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未运行测试不写通过 |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流（规格陈述→HTML 原句→V184/V394/V111/V028/`PlatformService`/`ActionGateAspect`/OpenAPI）；B5 不修复 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地；不把 B2 禁止项写成缺陷 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒级，未起 monitor |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| domain.md | `docs/agents/domain.md` | 词表/三 HTML 最终解释权；未改 CONTEXT.md |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）、using-git-worktrees、executing-plans、dispatching-parallel-agents、subagent-driven-development。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能（有 `grilling` / `grill-me` / `grill-with-docs`）
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit`：与双席/§7 冲突，**不调用**
- Trail of Bits `audit-context-building`：会派 function-analyzer 子代理，**不调用**

明确不用（controller 划掉）：MCP、第三主体、修源/HTML/锁/台账。

---

## 1. 锁、范围与源指纹

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。独立实算值见本文第一行独占锁行。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 字节 | 889020 |
| 行（`wc -l`） | 6850 |
| python3 hashlib | 与 sha256sum 同值 |
| openssl dgst -sha256 | 同值 |
| sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` | 全文一行同值 |
| 锁 blob | `8db3fc02ce8a6dc998863a9784e7e88af511b960` |
| findings | V1.90；SHA256 `48369705ee11705e0ae705ea005612c4bd2a28b63eac46874905f308e4d4114f`；754909 字节 |
| ledger | SHA256 `ad894530a54cd98476195139adcfc17870157868f23f29045c0995f5dcd6d5d4`；585956 字节；M014 `IN_PROGRESS`，B3=1，B5-r1 进行中 |
| 映射行抽出 | 84 条、无重复、MISSING=[] EXTRA=[]；BEQR 21 / BSM 12 / GEN 18 / PGM 33 |
| 锁内映射 HTML 来源行 vs 现行三 HTML | 66/66 hit=True miss=0（脚本按锁「来源：`path:line`」对照现行 HTML 同行；GEN 18 条无 HTML 行） |
| 计划 FILES | 1 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 3494（切分时 HEAD）；当前工作区 `\n` 计数 **3491**（B3-r1 +40/−43 后的现态，不改计划凑数） |
| questions | 无 `questions/M014*`；无未答问询 |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| modules.md / runbook.md | SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` / `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |

映射闭集（锁末映射行原文抽出，84 unique）见上文。GEN 无 HTML 行。

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 现行 | 行 | 字节 | SHA256 |
|---|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | V0.07 | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | V0.14 | 2032 | 304482 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | V0.12 | 979 | 103015 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

证据类型按提示词 2.3：1=三 HTML 原文；2=可执行产物行为；3=外部规范原文；4=可复现命令输出。本模块为文档，以类型 1+4 为主；无本模块测试文件，不把未运行测试写通过。未跑 `npm run verify:openapi` / 后端测试 / migrate（不装依赖；文档模块不强制可执行测试）。

`git status --porcelain` 对三 HTML / OpenAPI / 锁 / 源码 / 迁移均为空。工作区脏路径仅 findings、ledger、本模块 1 文件（B3 未提交）及 M014 B1/B4 raw（untracked）。

### 1.1 1 文件当前行数 / 字节 / SHA256（工作区实算，行=`\n` 计数 = splitlines）

| # | 路径 | 行 | 字节 | SHA256 | git blob（work=after） | HEAD blob | 全文 |
|---|---|---:|---:|---|---|---|---|
| 1 | `docs/platform-governance-product-spec-v0.2.md` | 3491 | 226970 | `dfb40482f22455c27071990d09b7a624248f3604d186a5b1293fb0ebb84f7396` | `ed8b55d19314c59be042ebb511710fbdb0185af4` | `0632c0fbdb1683ce7d6c7496375dfeb4788e07e0` | 已读 L1–L3491（标题 V0.06；OVERSIZE 按结构段分批阅读，raw 列全覆盖；未只读标题/搜索/截断） |

endswith `\n`=True。`git cat-file -p 82136f01…:docs/platform-governance-product-spec-v0.2.md | sha256sum` 与工作区全等。`git cat-file -t 82136f01…` = `tree`。

### 1.2 结构段全量三线覆盖表（全文已读）

| 段 | 行区间 | 内容 | 已读 | 三线 |
|---|---|---|---|---|
| 包装头 | L1–L23 | 标题 V0.06、2026-09-11 B3 变更记录、R5 历史+005 现状行、合并说明与四源归档表 | 是 | 第一性/顺从/对抗 |
| §0A | L25–L36 | 金额精度/合同本地 AI/当地时间/外部经销商跨篇承接 | 是 | 同左 |
| **原文件 1 页面清单** | **L38–L790** | | | |
| 定位 | L40–L51 | 文档定位；名词四层脚注（权威=三 HTML+§0A；模型文仅参考、V410 删父链） | 是 | 同左 |
| §2 分组总览 | L53–L71 | 15 个 P0 页（含终端开关/注册审批）；空间区域页「项目根及九类」 | 是 | 同左 |
| §3 全局规则 | L73–L97 | 筛选/状态/留痕/空状态；合同八态示例 | 是 | 同左 |
| §4 治理总览页 | L99–L137 | 指标、待办、商户/合同/设备分布、十一端 | 是 | 同左 |
| §5 综合体项目页 | L139–L171 | 六态成员；不定义迁移边 | 是 | 同左 |
| §6 空间区域页 | L173–L214 | 空间树首项「项目（归属根，非空间类型）」；废止是否可经营 | 是 | 同左 |
| §7 业态管理页 | L216–L244 | 六值闭集；不定义业态状态；列表仍列「编号」（观察） | 是 | 同左 |
| §8 商户管理页 | L246–L289 | 六态；暂停/终止/黑名单限制新增绑定 | 是 | 同左 |
| §9 合同管理页 | L291–L375 | 八态；§9.6 准入收口 supervisor_review；悬空 §2999 已订正 | 是 | 同左 |
| §10 员工岗位+注册审批 | L377–L461 | 员工五态；两条注册路径；自营标注视同公司自有；修订说明指纹改现行 | 是 | 同左 |
| §11 班次排班页 | L463–L497 | 不定义班次状态 | 是 | 同左 |
| §12 设备台账页 | L499–L528 | 设备七态 | 是 | 同左 |
| §13 设备领还页 | L530–L564 | 沿用设备七态 | 是 | 同左 |
| §14 角色权限页 | L566–L607 | 权限五态；废止复制模板 | 是 | 同左 |
| §14A 终端开关页 | L609–L653 | 十一端；废止定时启停；开关列表无未定义「状态」列 | 是 | 同左 |
| §15 操作留痕页 | L655–L691 | 事件七态；作废是动作 | 是 | 同左 |
| §16 关键跳转 | L693–L718 | 页间跳转表 | 是 | 同左 |
| §17 P0 页面验收 | L720–L734 | | 是 | 同左 |
| §18–§20 | L736–L790 | 已确认/细化（group.view=V394 已闭合）；结论；变更日志（含 2026-09-11 一行，历史行未改） | 是 | 同左 |
| **原文件 2 字段字典** | **L792–L1333** | | | |
| 定位与规则 | L794–L841 | 脱敏七项；不提供关闭开关 | 是 | 同左 |
| §3 全局字段 | L843–L877 | 废止通用名称/编号/状态门控；§3.1 十一端编码 `dealer_portal` 并标未实现 | 是 | 同左 |
| §4 项目字段 | L879–L894 | 项目六态 | 是 | 同左 |
| §5 空间字段 | L896–L918 | 节标题/口径/空间名称/类型选项去「项目」作空间类型；废止是否可经营/可留痕 | 是 | 同左 |
| §6 业态字段 | L920–L929 | 六值；业态编号标废止；演出传媒括注 media | 是 | 同左 |
| §7 商户字段 | L931–L963 | 四类主体绑定；合作模式六类；第一/第二联系人；资质走 V028 | 是 | 同左 |
| §8 合同字段+§8.1 | L965–L1018 | 合同八态；审批三档；不折算职级 | 是 | 同左 |
| §9 员工/岗位/班次 | L1020–L1073 | 员工五态；废止指纹作凭证定义 | 是 | 同左 |
| §10 设备字段 | L1075–L1094 | 设备七态 | 是 | 同左 |
| §11 权限字段 | L1096–L1116 | 权限五态；废止复制来源 | 是 | 同左 |
| §12–§13 留痕/筛选 | L1118–L1150 | 留痕七态 | 是 | 同左 |
| §14 状态字典 | L1152–L1244 | 各对象闭集；空间/业态/班次/注册/私人手机/终端开关不定义 | 是 | 同左 |
| §15–§19 | L1246–L1333 | 覆盖关系、验收、已确认（物理实现括注改字典活校验）、变更日志 | 是 | 同左 |
| **原文件 3 流程图** | **L1335–L2240** | | | |
| 定位+总览 | L1337–L1399 | F1–F11 表；关系 mermaid **有 F11 边** | 是 | 同左 |
| F1 项目初始化 | L1401–L1447 | 不定义项目迁移边 | 是 | 同左 |
| F2 空间配置 | L1449–L1509 | 废止是否可经营路径（孤立节点留在图内，观察） | 是 | 同左 |
| F3 商户入驻与合同生效 | L1511–L1586 | 商户不定义迁移边；合同 stateDiagram 已删改成员说明；非法行已改 `%%` | 是 | 同左 |
| F4 经营范围绑定 | L1588–L1646 | 合同已生效才绑定 | 是 | 同左 |
| F5 合同准入 | L1648–L1712 | 先阻断再升级；七类红线除外 | 是 | 同左 |
| F6 注册/设备凭证 | L1714–L1793 | 两条路径；废止 NFC 身份/指纹 | 是 | 同左 |
| F7 入职岗位班次 | L1795–L1866 | 不定义员工/班次迁移边 | 是 | 同左 |
| F8 设备领还 | L1868–L1934 | 设备只定义成员 | 是 | 同左 |
| F9 权限 | L1936–L2012 | 五类高危须审批；隔离无关闭开关 | 是 | 同左 |
| F10 留痕 | L2014–L2077 | 事件七态；不定义迁移边 | 是 | 同左 |
| F11 终端开关 | L2079–L2156 | 七档；废止定时启停 | 是 | 同左 |
| §13–§17 | L2158–L2240 | 依赖、验收、已确认、变更日志 | 是 | 同左 |
| **原文件 4 低保真** | **L2242–L2736** | | | |
| 定位+目标+IA | L2244–L2288 | §3.1 表头改分组（均在一级「平台治理」下） | 是 | 同左 |
| §4 全局结构 | L2290–L2325 | | 是 | 同左 |
| §5–§11 各页低保真 | L2327–L2506 | 总览/项目/空间/业态/商户/合同/准入 | 是 | 同左 |
| §12–§18 人员设备权限留痕 | L2508–L2660 | | 是 | 同左 |
| §17A 终端开关低保真 | L2622–L2642 | 开关列表生效/失效为技术字段；「不能晚于」已废止 | 是 | 同左 |
| §19 终端说明 | L2662–L2673 | 私人手机按 HTML 低风险闭集+现场禁止项 | 是 | 同左 |
| §20–§23 | L2675–L2736 | 验收；§21.12 group.view=V394 已闭合 | 是 | 同左 |
| **§24 有效权限/字段投影/审批映射** | **L2739–L2849** | | | |
| §24.1 | L2745–L2778 | 标题与正文均为 `GET /api/v1/access/effective-permissions` | 是 | 同左 |
| §24.2 | L2780–L2807 | 字段级投影待补齐；死代码已删 | 是 | 同左 |
| §24.3 | L2809–L2849 | GOV 三档；代码核验仍写职级算术/`specific_role`（020 登记，禁止改写） | 是 | 同左 |
| **继续合并补录（2026-08-28）** | **L2851–L3491** | 无低保真来源段 | | |
| 补录头 | L2851–L2855 | 已废止不得回灌；以三份 HTML 为准 | 是 | 同左 |
| 来源：页面清单 | L2857–L3001 | 70 条摘录；空间树不含项目；废止楼栋/区块 | 是 | 同左 |
| 来源：流程图 | L3003–L3227 | 110 条摘录；废止项目/商户迁移边与仅预警 | 是 | 同左 |
| 来源：字段字典 | L3229–L3491 | 141 条摘录；废止业态编号；`distributor_web` 订正为 dealer_portal；admission_audits 标废止 | 是 | 同左 |

合计覆盖 L1–L3491，无抽样宣称。补录未单列低保真原文件来源（观察 OBS-03，与 B1 同，不升）。

必要只读依赖（验证本模块声称的当前实现，不扩审模块；本席未改）：

| 路径 | 用途 | 与 HEAD |
|---|---|---|
| `V028__compliance_expiry_and_reminders.sql` | `compliance_expiry_items` / `compliance_reminders` | 只读 |
| `V050__governance_supervisor_review_persistence.sql` | `supervisor_review_decisions` 基表 | 只读 |
| `V055__access_staff_levels_and_salary.sql` | `staff_levels` 30 档 | 只读 |
| `V057__governance_supervisor_review_approval_level.sql` | approval_level CHECK 含 `specific_role` | 只读 |
| `V111__terminal_type_authority_code_alignment.sql` | 终端 CHECK 前十值+`*`，无 dealer_portal | 只读 |
| `V184__space_type_dictionary.sql` | 字典八码，无 mall/project | 只读 |
| `V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql` | `analytics.group.view` `requires_approval=TRUE` | 只读 |
| `V410`（只读点名） | `DROP COLUMN parent_entity_id` | 只读 |
| `PlatformService.java` | `BUSINESS_ENTITY_ROLES` 三值、`MERCHANT_COOPERATION_FORMS` 六类、`createSpace` 字典硬校验 | 只读 |
| `AccessController.java` | `/api/v1/access` + `effective-permissions` | 只读 |
| `AuthorizationService.java` | DENY 优先 `computeEffectivePermissions` | 只读 |
| `SupervisorReviewService.java` | `evaluate`；仍含 `specific_role` 与 level_rank 算术 | 只读 |
| `StaffApprovalService.java:44,61` | 两处 `reviews.evaluate` | 只读 |
| `gate/ActionGateAspect.java` | terminalSwitchGuard.check → AdmissionGuard.check → requirePermission | 只读 |
| `apps/web/src/admin/adminRoutes.ts` | P0 页中文名不一一对应 | 只读 |
| `docs/openapi/openapi-p0.yaml` | `effective-permissions` 存在；`entity_role` 旧六值；未改 | 未改 |

---

## 2. 数据流（规格陈述 → HTML 原句 → main 实现事实）

本模块为文档与接口定义类。实现对照只读，不作修复对象。

| 规格声称（当前 after） | HTML 原句（锁/现文） | main 实现事实 |
|---|---|---|
| 四类主体=经营对象/运营/合同/结算，同一经营对象唯一绑定 | BEQR-143 `:226`；PGM-026 `:492` | 字段字典 §7 有四主体+绑定编号。实现对照不在本模块改 |
| 物理项目与运营公司多对多，不是固定隶属链 | BEQR-144 `:227`；PGM-018 `:415` | V410:9 `DROP COLUMN parent_entity_id`。规格 L51 权威=三 HTML+§0A；模型文仅参考并点名 V410。模型文 L64 仍列该列（019 XM→M261） |
| 合作方式六类；自营是合作方式不是独立布尔 | BEQR-148 `:231` | `MERCHANT_COOPERATION_FORMS` 含 `self_operated`。规格用「商户档案上标注为自营」，无 `is_self_operated` |
| 终端十一端 + 七档开关 | BEQR-150 `:233`；BEQR-153 `:236`；PGM-010 `:372` | `ActionGateAspect.gate` 先 `terminalSwitchGuard.check`。规格 §14A/F11 七档与十一端中文对齐；`dealer_portal` 标 V111/OpenAPI 尚无该码 |
| 项目/商户/合同/员工/设备/权限状态闭集 | BEQR-154…159 `:237–242` | 规格状态字典与 ENUM 成员逐字同；空间/业态/班次/注册/私人手机/终端开关明确不定义 |
| 考勤打卡=领工作手机（NFC/扫码/离充，取最早） | BSM L1445 | 规格多处「NFC 仅用于领取工作手机的上班打卡」与 L1445 同旨，不是私人手机打卡 |
| 私人手机只开放低风险功能，含「查看考勤」 | BSM `:930`/`:1865`；PGM `:741` | 规格低保真 §19 L2671 已按闭集列出公告/工单/订位/看排班/查看考勤/请假/查薪资，禁止项对齐现场句 |
| 空间对象 | PGM 空间 L469 九类（无「项目」） | `createSpace` 活校验 `space_type_dictionary`；V184 八码 building/floor/zone/shop/public_area/parking/booth/other（含楼栋/区块，无 mall、无「项目」码）。规格 L900 九类对齐 HTML；L1295 括注登记字典差 |
| 集团视图 | PGM-017/020/066 | `analytics.group.view` V394:16–18 已 `requires_approval=TRUE`。规格 L764/L2717 写已闭合 |
| 合同准入先阻断再升级 | PGM-007；七类红线 | `AdmissionGuard` + `ActionGateAspect`；酒吧 `BarService` 用 FQCN `@com.complex.platform.gate.GuardedAction`；票务/游乐/结算/库存同。`StaffApprovalService:44,61` 调 `evaluate`。R5 L10–11 为历史，L12 为 2026-09-11 现状 |
| 最终权限五源 DENY 优先 | BEQR-151 `:234` | `GET /api/v1/access/effective-permissions` OpenAPI L8098 与 `AccessController:244`；指定用户须 `access.user.view`（`:253`）。规格 §24.1 标题与正文路径一致 |
| 职位模板删除走 lifecycle | BSM-004 `:566` | 本规格不定义 DELETE 行为（无实现面）；不与恒 409 冲突 |
| OpenAPI `entity_role` 旧六值 | （实现缺口，M013-009 归 M392） | OpenAPI `BusinessEntityRequest.entity_role` 仍 operation/contract/settlement/invoice/payment/other。本规格未复述该六值 |
| 证照到期台账 | （实现证据） | V028 `compliance_expiry_items`/`compliance_reminders`。规格 L959 已改；`merchant_credentials` 0 处生产命中 |

---

## 3. 三线审计

### 3.1 第一性原理（本文件三问）

1. **为何必须存在**：删掉则 PGM-003 模块入口所对应的页面/字段/流程/低保真与 §24 有效权限接口在产品规格层无统一载体；BEQR-143/148/150/154–159、PGM-005/008/010/026/040 无 P0 页面与字段闭集。有上位。OVERSIZE 单文件是计划合法切分，不是删除理由。
2. **声称是否执行**（对当前 after，不以 B3 自报为准）：
   - 自称替代四份分散源、§0A 与 HTML 冲突时以 HTML 为准——空间类型、私人手机功能、group.view、R5 现状、权威脚注、§24.1 路径现与 HTML/实现对齐。
   - §9.6 / §24.3 声称准入落 `supervisor_review_decisions`、不建 `admission_audits`——补录 L3340 已标废止并指向该表；V050 建表、V057 扩展。
   - §24.1 声称现网已落地 `/api/v1/access/effective-permissions`——OpenAPI L8098 与 `AccessController:244` 存在；标题不再写 `/me/`。
   - §18.11 / §21.12 声称 `analytics.group.view` 已由 V394 置 `requires_approval=true`——V394:16–18 为真。
   - R5 文首 L10–11 仍写 evaluate 孤立、后端未校验开关，但 L12 明确为 2026-06-18 历史、不得当现行；`ActionGateAspect:43–50`、`StaffApprovalService:44,61`、酒吧/票务 FQCN 注解否定该「当前」。符合 B2「不删历史行、加现状行」。
3. **未验证就当真的前提**：无未验证即当真的现行前提。L51 不再把模型文抬成裁决层。L900 不再伪称 HTML 并集含「项目」。L1295 不再把 mall 当活根。四处修订说明不再把 fingerprint 写成 D8 现行改写结果。§24.3.6 代码核验句仍为 2026-06-19 日期化差距登记（020；实现仍含 `specific_role`，本席复验为真，禁止在 B5 改写）。

### 3.2 顺从式（假定规格意图正确，核对 84 条）

见 §5。映射 ID 全部核对。无 finding 的 ID 仍有覆盖记录。文档类不强制可执行测试（runbook §5）；GEN-003 对本文件标注「无独立可执行测试面，不把未运行测试写通过」。

### 3.3 对抗式（假定规格错误；grilling 自答，不问操作员）

| # | 追问 | 事实 | 收窄 |
|---|---|---|---|
| Q1 | 是否仍把已删父链写成当前？ | 正文无 `parent_entity_id` 列；L51 权威=三 HTML+§0A，并点名 V410 已删。模型文 L64 仍列该列 | 019 XM→M261，不升本模块缺陷 |
| Q2 | 独立自营布尔？ | 规格用「标注为自营」；无 `is_self_operated` | 不升 |
| Q3 | entity_role 旧六值？ | 本规格未复述；OpenAPI 仍旧六值（M392） | OBS-01，不重开 M013-009 |
| Q4 | NFC/指纹作身份？ | 四处修订说明已改现行；正文【已废止】；变更日志/补录历史句保留 | 016 已修；历史行按禁止项不升 |
| Q5 | 定时启停？ | 字段/14A/F11 已废止；17A.1 列表与 §14A.3 对齐；L2639 废止「不能晚于」 | 011 已修 |
| Q6 | 私人手机打卡？ | 规格打卡绑工作手机 NFC，与 L1445 一致 | 不升 |
| Q7 | 私人手机功能是否按 HTML 列全？ | L2671 闭集+现场禁止项对齐 PGM L741 / BSM L930 | 003 已修 |
| Q8 | 空间类型是否可按规格选「项目」而过活校验？ | 活句已去「项目」作类型；V184 无该码，`createSpace` 拒 | 001/002 已修 |
| Q9 | F11 是否在总览图可达？ | L1396–L1398 有节点并连到后续业务与留痕 | 012 已修 |
| Q10 | `/me/effective-permissions` 是否存在？ | 规格无 `/me/`；OpenAPI 无此路径；现网为 `/api/v1/access/...` | 008 已修 |
| Q11 | 补录未标废止的 admission_audits 当现行 API？ | L3340 已【已废止】并指向 `supervisor_review_decisions` | 014 已修 |
| Q12 | 一级导航是否仍把商户合同写成独立一级？ | L2268 表头改分组；行内为分组名 | 015 已修 |
| Q13 | 业态编号是否仍必填英码？ | L927 标废止；L922 仅演出传媒括注 media；不新建「业态代码值」 | 009 已修。页面/低保真列表仍列「编号」见 OBS-10，不升缺陷 |
| Q14 | R5 是否仍把闸口写成未接线当前事实？ | L12 现状行声明 L10–11 过期；酒吧/票务等确有 `@GuardedAction`（酒吧为 FQCN） | 005 已修 |
| Q15 | 005 现状行是否虚标酒吧域？ | `BarService` 多处 `@com.complex.platform.gate.GuardedAction`；票务 `TicketingService`、游乐 `AmusementService`、库存、财务同 | 不升 |
| Q16 | 合同 stateDiagram 是否仍自造边？ | 全文 0 处 `stateDiagram`；L1559 只给八成员 | 007 已修 |
| Q17 | F3 mermaid 非法行？ | L1549 为 `%%` 注释；合法 `M -- "是" --> P[...]` 保留 | 013 已修 |
| Q18 | merchant_credentials 是否当现行表？ | L959 改 V028；生产 0 处该标识作表名 | 017 已修 |
| Q19 | §2999 悬空？ | 活句无 §2999；L338 改为补录历史行 | 018 已修 |
| Q20 | 是否越界改 HTML/源/OpenAPI/补录原文？ | 三 HTML/OpenAPI/源/迁移 porcelain 空；补录只加废止前缀/订正括注 | 不升；符合 B2 禁止项 |

---

## 4. 定稿 ID 001–020 独立复验（不代替 84 条）

| ID | 分类 | 队列 | 当前 after 复验 | 结论 |
|---|---|---|---|---|
| M014-001 | 冲突 | 否（已修） | L900 九类对齐 PGM L469，明确项目=归属根非空间类型；L896 节标题、L904 空间名称、L907 类型选项无「项目」作类型；L190/L2388 树首项括注归属根；L56/L174/L2272/L906 项目根保留 | 成立 |
| M014-002 | 细化 | 否（已修） | L1295 括注 V184 八码活校验；mall=契约快照；字典差登记。`PlatformService:476` 硬校验；V184 无 mall | 成立 |
| M014-003 | 冲突 | 否（已修） | L2671 低风险闭集含查看考勤/看排班等；禁止项=代客下单/收款码/履约/扫码核销放行，对齐 BSM L930 | 成立 |
| M014-004 | 细化 | 否（已修） | L764、L2717 写 V394 已闭合。V394:16–18 `analytics.group.view` TRUE | 成立 |
| M014-005 | 细化 | 否（已修） | L12 紧接 R5 L10–11，声明历史过期；闸口与开关拒绝已由 gate 包落地。未删 L10–11 | 成立 |
| M014-006 | 冲突 | 否（已修） | L51 权威=三 HTML+§0A；模型文仅参考并点名 V410 | 成立 |
| M014-007 | 细化 | 否（已修） | 全文无 `stateDiagram`；L1559 成员说明 | 成立 |
| M014-008 | 技术性 | 否（已修） | L2745 标题=`GET /api/v1/access/effective-permissions`；OpenAPI L8098；`AccessController:244` | 成立 |
| M014-009 | 细化 | 否（已修） | L927 【已废止】业态编号；L922 演出传媒括注 media；未新建「业态代码值」字段 | 成立 |
| M014-010 | 技术性 | 否（已修） | L869 `dealer_portal`+未实现括注；补录 L3242 订正 `distributor_web`→dealer_portal。V111 CHECK 前十值无该码 | 成立 |
| M014-011 | 细化 | 否（已修） | L629 开关列表无「状态」列；L2630 对齐技术字段；L2639 废止「不能晚于」 | 成立 |
| M014-012 | 技术性 | 否（已修） | L1396–L1398 F11 节点连 J/K | 成立 |
| M014-013 | 技术性 | 否（已修） | L1549 `%%` 注释 | 成立 |
| M014-014 | 细化 | 否（已修） | L3340 【已废止】admission_audits，统一 `supervisor_review_decisions` | 成立 |
| M014-015 | 细化 | 否（已修） | L2268 表头=分组（均在一级「平台治理」下） | 成立 |
| M014-016 | 细化 | 否（已修） | L428/L1026/L1716/L2510 四处修订说明改现行；变更日志历史行未改 | 成立 |
| M014-017 | 技术性 | 否（已修） | L959 V028 `compliance_expiry_items`/`compliance_reminders` | 成立 |
| M014-018 | 技术性 | 否（已修） | L338 改为 §24.3 与补录合同管理页历史行 | 成立 |
| M014-019 | 技术性 XM | 否 | `merchant-space-contract-model-v0.1.md:64` 仍列 `parent_entity_id` 为当前字段。本模块未改该文 | 维持 XM→M261 |
| M014-020 | 观察 | 否 | §24.3.6 代码核验句仍为真（`SupervisorReviewService` 仍含 `specific_role`；V057 CHECK 含该值）。禁止改写该日期化核验 | 维持仅登记 |

B2 禁止项抽查：未改源/迁移/OpenAPI/三 HTML；未新增字段或接口；补录历史原文未删（只加废止前缀/订正括注）；R5 历史行与 §24.3.6 核验句未重写为「已对齐」。

---

## 5. 84 条冻结判据核对

说明：本文件是产品规格。无独立可执行测试面的 ID，GEN-003 记「N/A-文档」；不把未运行测试写通过。冲突/过期当前事实已由 001–018 收口；本轮无新 FAIL。

| ID | 锁来源 | 本文件适用证据 | 结论 |
|---|---|---|---|
| BEQR-063 | ENUM `:111` 酒吧高危 | F9 资金/核销/退款/调账须审批；未发明沽清规则 | 顺从 PASS（无实现面） |
| BEQR-074 | ENUM `:125` 隐私桌台 | 未定义桌台权限页；未发明相反三权 | 顺从 PASS（无实现面） |
| BEQR-143 | `:226` 四类主体 | 字段字典 §7 四主体+唯一绑定编号 | 顺从 PASS |
| BEQR-144 | `:227` 多对多非隶属链 | 正文无父链列；L51 权威回到 HTML；模型文缺口归 019 | 顺从 PASS |
| BEQR-145 | `:228` 城市项目公司 | 未把资产公司写成独立主体 | 顺从 PASS |
| BEQR-146 | `:229` 运营公司职责 | 未把娱乐平台写成取得单吧门票 | 顺从 PASS |
| BEQR-147 | `:230` 收入归属 | 未发明顶层自动取收入 | 顺从 PASS |
| BEQR-148 | `:231` 合作模式六类+其他须指定模板 | 六类有；「其他」生效前指定收费模板未进字段/表单 | 观察 OBS-04（未发明相反；未写全下位） |
| BEQR-149 | `:232` 职级 M1–M10 上中下 | 多处 30 档；明确不折审批 | 顺从 PASS |
| BEQR-150 | `:233` 十一端 | §14A/F11/§3.1 十一端中文闭集正确；`dealer_portal` 标未实现 | 顺从 PASS |
| BEQR-151 | `:234` 最终权限五源 | §14 / §24.1 五源 DENY 优先；路径与 OpenAPI/Controller 一致 | 顺从 PASS |
| BEQR-152 | `:235` 项目隔离+敏感数据 | 隔离无关闭开关；敏感分权 | 顺从 PASS |
| BEQR-153 | `:236` 七档开关 | F11/14A 七档；废止定时启停；17A.1 不再回灌时刻列作控制 | 顺从 PASS |
| BEQR-154 | `:237` 项目六态 | 与 ENUM 逐字同；不定义边 | 顺从 PASS |
| BEQR-155 | `:238` 商户六态+缓冲期+试运营不管 | 六态同；缓冲期在 §14.2；试运营废止 | 顺从 PASS |
| BEQR-156 | `:239` 合同八态+当地时间 | 八态同；§0A 当地时间；F3 不再自造边 | 顺从 PASS |
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
| BSM-028 | `:698` 基础管理主要功能 | 六类合作、多对多、项目公司边界有承接 | 顺从 PASS |
| BSM-029 | `:714` 和谁有关 | 跳转表连票务/酒吧/游乐/财务/权限 | 顺从 PASS |
| BSM-072 | `:1308` 商户业态六值+其他主管公司 | 六值有；业态编号已废止；仅演出传媒括注 media | 顺从 PASS |
| BSM-074 | `:1326` 商户侧边界 | 商户看不到全局；未发明跨商户 | 顺从 PASS |
| BSM-114 | `:1827` 各类入口 | 十一端+私人手机能力按 HTML 低风险闭集 | 顺从 PASS |
| BSM-115 | `:1897` 子系统必须连 | 流程依赖表 + F5 卡后续业务 | 顺从 PASS |
| BSM-120 | `:1957` 已确认规则 | 私人手机设备标识、寄存柜、不建设营销打卡有承接 | 顺从 PASS |
| GEN-001 | 通用 | 文件有上位；OVERSIZE 合法 | 顺从 PASS |
| GEN-002 | 通用 | `/me/` 已订正；F3 mermaid 非法行已改注释；F11 图已补 | 顺从 PASS |
| GEN-003 | 通用 | 文档类 N/A；不把未跑测试写通过 | N/A-文档 |
| GEN-004 | 通用 | 无本文件输入校验实现 | N/A-无实现面 |
| GEN-005 | 通用 | §24.1 指定用户须 `access.user.view`；高危须审批 | 顺从 PASS（规格层） |
| GEN-006 | 通用 | 无本文件事务实现 | N/A-无实现面 |
| GEN-007 | 通用 | §24.1 单次授权不在读接口消费 | 顺从 PASS（规格层） |
| GEN-008 | 通用 | 空间类型/导航/编码/迁移边/指纹修订说明已收口 | 顺从 PASS |
| GEN-009 | 通用 | 无本文件错误处理实现 | N/A-无实现面 |
| GEN-010 | 通用 | 脱敏七项；U 盘清留痕 | 顺从 PASS（规格层） |
| GEN-011 | 通用 | 留痕七态+作废动作 | 顺从 PASS |
| GEN-012 | 通用 | 无本文件资源边界实现 | N/A-无实现面 |
| GEN-013 | 通用 | 所引 OpenAPI 路径存在；admin 路由中文名不一一对应（观察） | 顺从 PASS（规格层） |
| GEN-014 | 通用 | 规格不是迁移；mall/V394 声称已改现行 | 顺从 PASS |
| GEN-015 | 通用 | 中文主文；F3 mermaid 非法行已修 | 顺从 PASS |
| GEN-016 | 通用 | 过期实现/伪 HTML 并集已改；R5 历史+现状分行 | 顺从 PASS |
| GEN-017 | 通用 | 无把观察升冲突；无添加项伪装技术性 | 顺从 PASS |
| GEN-018 | 通用 | 结论落到 HTML/命令；不引用席位意见当依据 | 本报告遵守 |
| PGM-002 | `:308` 治理五步 | F5+依赖表对象→合同→人员→设备→留痕 | 顺从 PASS |
| PGM-003 | `:339` 模块入口 | 页面分组覆盖卡片主题；导航表头已改分组 | 顺从 PASS |
| PGM-004 | `:342` 综合体和集团 | 项目页+集团已承接句；V394 已闭合 | 顺从 PASS |
| PGM-005 | `:347` 空间和点位 | 对象列表去「项目」作类型；mall 根改为字典活校验 | 顺从 PASS |
| PGM-006 | `:352` 商户和主体 | 四类绑定+六类合作+两联系人 | 顺从 PASS |
| PGM-007 | `:357` 合同和准入 | 先阻断再升级；R5 现状+补录落库已收口 | 顺从 PASS |
| PGM-008 | `:362` 员工和手机 | 两条路径+设备标识；指纹修订说明/私人手机功能已修 | 顺从 PASS |
| PGM-009 | `:367` 权限和职级 | 最小颗粒度+模板；废止复制模板 | 顺从 PASS |
| PGM-010 | `:372` 终端开关 | 十一端+七档；R5 后端未校验标为历史；17A 列已收口 | 顺从 PASS |
| PGM-011 | `:378` 设备管理 | 台账/领还/七态 | 顺从 PASS |
| PGM-012 | `:383` 商户侧 | 授权范围内全开放、不看全局 | 顺从 PASS |
| PGM-013 | `:388` 开放接口 | 未在本文件展开；未发明相反 | N/A-无实现面 |
| PGM-014 | `:393` 留痕复核 | 操作留痕页+七态 | 顺从 PASS |
| PGM-015 | `:399` 楼宇运营 | 本文件不承载楼宇页 | N/A-无实现面 |
| PGM-016 | `:404` 应急与隐私 | U 盘清留痕；脱敏 | 顺从 PASS（规格层） |
| PGM-017 | `:412` 综合体和集团 h2 | 项目归属；集团已承接且审批属性已闭合 | 顺从 PASS |
| PGM-018 | `:415` 多对多 | 见 BEQR-144 | 顺从 PASS |
| PGM-019 | `:424` 各主体职责 | 未发明顶层自动取收入 | 顺从 PASS |
| PGM-020 | `:440` 集团下钻 | 已承接句正确；审批属性已闭合 | 顺从 PASS |
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
| PGM-066 | `:951` 已拍板 | 集团视图已承接；权限自定义；日结补录未在本文件展开 | 顺从 PASS |

84/84 已核对。无 FAIL 的 ID。无添加项。

---

## 6. 本次 findings / 观察

### 6.1 工程缺陷（临时编号；入队须 B2）

无。无未定稿缺陷，无 HTML 修改申请，无锁外判据，无添加项。

### 6.2 观察（不升缺陷；临时编号 GR-M014-B5-xx）

- **GR-M014-B5-01**（同根因 M013-009 / 本模块 OBS-01）：OpenAPI `BusinessEntityRequest.entity_role` 仍为旧六值。本规格未复述该六值，不重开。
- **GR-M014-B5-02**（同根因 M014-020）：§24.3.6 代码核验（2026-06-19）「仍按职级算术与 specific_role」仍为真（`SupervisorReviewService` 策略与 V057 CHECK）。B2 禁止改写该日期化核验；实现缺口归 review 模块。
- **GR-M014-B5-03**（同根因 B1 OBS-03）：2026-08-28 补录只有页面/流程/字段三源，无低保真来源段；补录头已声明未写入 HTML 者不生效。
- **GR-M014-B5-04**（同根因 B1 OBS-04 / 020）：BEQR-148「其他」须指定收费模板——规格六类有、模板套用未进字段表；未写相反规则。
- **GR-M014-B5-05**（同根因 B1 OBS-05 / 020）：PGM-031 外部经销商——§0A 有跨篇承接，页面清单无独立经销治理页；HTML 沉默于「必须是 P0 治理页」时不升冲突。
- **GR-M014-B5-06**（同根因 B1 OBS-06 / 020）：P0 页中文名与 `adminRoutes.ts` 工作台名不一一对应（规格 L46 声明不写接口/代码）。GEN-002 不对产品规格强制现网路由同名。
- **GR-M014-B5-07**（同根因 B1 OBS-07 / 020）：`ADM-GOV-005` `/admin/governance/admission-audits` 仍聚合三列表；§9.6 已诚实收口，不把该实现页当本规格新根因。
- **GR-M014-B5-08**（同根因 B1 OBS-08 / 020）：第一联系人=主要联系人双字段，HTML 有第一/第二；属同义展示，不升。
- **GR-M014-B5-09**（同根因 M014-016 / B2 禁止改历史行）：四张原文件变更日志 2026-06-10 行仍写「改为…device_key_fingerprint」；补录 L2926/L3342 历史修订说明同。B2 禁止删补录原文、禁止改变更日志历史行。现行定义以四处修订说明与正文【已废止】为准。
- **GR-M014-B5-10**（同根因 M014-009，弱于已修必填字段）：页面清单 L232、低保真 L2415「业态列表 | 业态名称、编号、说明」仍把「编号」列为展示列。字段字典 L927 已废止独立必填编号。不构成「必填英码闭集」回灌，不升缺陷。
- **GR-M014-B5-11**：F2 mermaid（L1475–L1479）废止节点未连边，主路径仍可渲染。不升 GEN-015 缺陷。
- **GR-M014-B5-12**：F5 L1683「写入准入审计和操作留痕」是概念表述，未点名 `admission_audits` 表。§9.6/§24.3/补录已收口落库。不升。
- **GR-M014-B5-13**（同根因 M014-019）：`docs/merchant-space-contract-model-v0.1.md:64–66` 仍把 `parent_entity_id` 写成当前字段。本模块只去掉「以该文为准」，不改该文。
- **GR-M014-B5-14**（同根因 M014-001，历史行）：变更日志 2026-08-27 行仍写「空间树/类型收敛为项目、楼层…」。B2 禁止改变更日志历史行；活句已改。
- **GR-M014-B5-15**（同根因 020）：合并文件无总变更日志、版本仅各原文件标题+包装头 V0.06。不升。

添加项：无。须完整列出供 2.2 者：无。

---

## 7. 命令证据

以下均在只读命令下实跑。主仓 cwd 未 `git add` / 未写模式。验证副本 `/tmp/m014-b5-r1-verify` 结束已删除。

```
$ sha256sum docs/audit-2026-09/plan/criteria-lock.md
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
exit 0

$ python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
exit 0

$ openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
SHA2-256(docs/audit-2026-09/plan/criteria-lock.md)= b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
exit 0

$ cat docs/audit-2026-09/plan/criteria-lock.sha256
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
exit 0

$ git rev-parse HEAD ; git rev-parse origin/main
685dca4e1088ed59e68196c3c5b45787fde9db02
685dca4e1088ed59e68196c3c5b45787fde9db02
exit 0

$ git cat-file -t 82136f01b088c2bf59a5b480a92e2bc4c71f9923
tree
exit 0

$ git ls-tree 82136f01b088c2bf59a5b480a92e2bc4c71f9923 docs/platform-governance-product-spec-v0.2.md
100644 blob ed8b55d19314c59be042ebb511710fbdb0185af4	docs/platform-governance-product-spec-v0.2.md
exit 0

$ git hash-object docs/platform-governance-product-spec-v0.2.md
ed8b55d19314c59be042ebb511710fbdb0185af4
exit 0

$ python3 -c "...spec bytes/lines/sha256..."
bytes 226970
splitlines 3491
n_count 3491
endswith_nl True
sha256 dfb40482f22455c27071990d09b7a624248f3604d186a5b1293fb0ebb84f7396
exit 0

$ git cat-file -p 82136f01b088c2bf59a5b480a92e2bc4c71f9923:docs/platform-governance-product-spec-v0.2.md | sha256sum
dfb40482f22455c27071990d09b7a624248f3604d186a5b1293fb0ebb84f7396  -
exit 0

$ git status --porcelain -- docs/business-enum-quick-reference-v0.1.html docs/business-system-map-for-operations-v0.1.html docs/platform-governance-map-for-operations-v0.1.html docs/openapi/openapi-p0.yaml docs/audit-2026-09/plan/criteria-lock.md backend/
(empty)
exit 0

$ rg -n "stateDiagram|/me/effective|当前实现缺口" docs/platform-governance-product-spec-v0.2.md
(no matches)
exit 1   # 无命中，属预期；不折算为业务 finding

$ sed -n '12p;51p;338p;629p;764p;869p;900p;907p;927p;959p;1295p;1396,1398p;1549p;1559p;2268p;2630p;2671p;2745p;3340p' docs/platform-governance-product-spec-v0.2.md
（各行与 §4 复验表一致；exit 0）

$ rg -n "analytics.group.view" backend/src/main/resources/db/migration/V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql
16:UPDATE permissions SET requires_approval = TRUE
17: WHERE permission_code IN ('analytics.group.view', ...
exit 0

$ sed -n '14,23p' backend/src/main/resources/db/migration/V184__space_type_dictionary.sql
（八码 building/floor/zone/shop/public_area/parking/booth/other；exit 0）

$ rg -n "parent_entity_id" backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql
9:ALTER TABLE business_entities DROP COLUMN IF EXISTS parent_entity_id;
exit 0

$ sed -n '38,50p' backend/src/main/java/com/complex/platform/gate/ActionGateAspect.java
（terminalSwitchGuard.check → admissionGuard.check → requirePermission；exit 0）

$ rg -n "reviews.evaluate" backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java
44:        var review = reviews.evaluate(...)
61:        var review = reviews.evaluate(...)
exit 0

$ rg -n "effective-permissions" backend/src/main/java/com/complex/platform/access/AccessController.java docs/openapi/openapi-p0.yaml
AccessController.java:244:    @GetMapping("/effective-permissions")
AccessController.java:250:    @GetMapping("/users/{id}/effective-permissions")
openapi-p0.yaml:8098:  /api/v1/access/effective-permissions:
openapi-p0.yaml:8122:  /api/v1/access/users/{id}/effective-permissions:
exit 0
```

未运行：后端测试、Flyway、`npm run verify:openapi`（本模块无测试文件；不装依赖；不把未运行测试写通过）。`rg` 对 `stateDiagram` 等无命中返回 exit 1，属检索空集，不是命令故障。

子代理失败重跑次数：0（本席未派子代理）。

---

## 8. 技能实用 / 缺口

实用：grilling 追问树（Q1–Q20）在 B5 全量重审中定位到「005 是否虚标酒吧域」——用 FQCN 注解复验后否定，避免误 FAIL；verification-before-completion 阻止把 B4 双 PASS 写成 B5 通过；systematic-debugging 把规格「当前」句追到 V394/V184/`ActionGateAspect`/OpenAPI。

缺口：见 §0。`batch-grill-me` 不存在，未冒称调用。

ponytail-review（不落地）：`Lean already` 于本模块活句。可删复杂度在补录历史摘录与四源重复变更日志，属合并载体的既有形态，B2 禁止删除补录原文，不升缺陷。

---

## 9. 本席 B5 结论

**PASS**。

同时满足：本模块 1 文件有全文件覆盖记录；映射 84 判据已核对；三线均执行；声称有类型 1 或 4 证据；无未答 questions；无未定稿缺陷；无添加项未答；无 HTML 修改申请；第一行独占锁哈希有效。无有条件通过。

本票不等于双席合取，不等于模块 ledger PASS。
