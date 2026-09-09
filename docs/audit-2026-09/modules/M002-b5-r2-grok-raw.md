CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M002 B5-r2 Grok 修复后全量独立审计 V0.01

席位：Grok（审计席乙）。步骤：B5-r2。模块：M002 `00-authority-html-02` / 共 322。B3 已执行次数：2（本次不重复 B3/B4，次数不增）。控制：V0.17。ledger 入口事实：`IN_PROGRESS`。本席只写本文件；未读本次 b5 self raw（调用前该路径不存在）；未改 HTML/代码/lock/findings/ledger；未派子代理；未提交/推送。

## 1. 技能预检（本席 Linux 实核，不抄 Codex 路径）

按意图执行 §6，只记本席实际读到的 `SKILL.md`。

| 技能 | 本席路径 | 本次用法 |
|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 |
| grilling / grill-me | mattpocock `skills/productivity/grilling` 与 `grill-me` | 对抗式/第一性原理追问结构；**不等操作员、不派子代理** |
| codebase-design | mattpocock `skills/engineering/codebase-design/SKILL.md` | 删除测试/深度：本文件是治理规则的深模块 |
| diagnosing-bugs | mattpocock `skills/engineering/diagnosing-bugs/SKILL.md` | 001/004 残留用既有命令做红绿信号；B5 不修 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流；不改代码 |
| verification-before-completion | superpowers 同名 | PASS 前实跑命令 |
| tdd / test-driven-development | 均存在 | 文档/HTML **不强制**落测试；只跑既有 `verify-entry-anchors.test.mjs` |
| ponytail-review | ponytail `4.9.0/skills/ponytail-review/SKILL.md` | 找可删；页面壳无当删业务发明 |
| writing-for-agents | 存在 | 已核存在；B5 Grok **不写**模块收口 |
| git-guardrails-claude-code | 存在（可选） | 本次无 git 写，未调用本体 |
| resolving-merge-conflicts | 存在 | 无冲突，未用 |
| i-have-adhd | `/home/james/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 存在；**84 条逐项报告完整性优先于「列表≤5」** |
| ponytail | 存在 | **B3 专用，本次不用** |

缺口（不得冒充已调用）：

- `batch-grill-me`：本环境 `find` 无此技能名（仅有 grilling / grill-me / grill-with-docs）。
- `ubiquitous-language`：本环境未找到。

明确不用（与 §6 一致，且本席禁止子代理）：`executing-plans`、`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`finishing-a-development-branch`、`research`、`ponytail-audit/debt/gain`、Arc `audit`/`implement`（会派子代理或改码）。

## 2. 输入与覆盖

本席实算：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
```

对照（只记录，本席不改）：sidecar 同行；`ledger.md` 顶部 `CRITERIA-LOCK-SHA256:` 同行；`LOCK-AMEND-COUNT: 0`；lock 文内版本 V0.06。

| 输入 | 值 |
|---|---|
| 模块文件 | 仅 `docs/platform-governance-map-for-operations-v0.1.html` |
| `wc -l` | **979** |
| 字节 | 102733 |
| HTML SHA256 | `9864216e609df2f319fa2cf7e51b61f06003ce45dff64a1a4098e71435865a68` |
| 映射判据 | 18 GEN + 66 PGM = **84**（`criteria-lock.md` M002 行） |
| 锚点实数 | h2=18，h3=48，合计 66，与 lock「PGM h2/h3 66」一致 |
| 既有 findings 定位 | M002-001/002/003/004 队列均为「否」——**只作范围定位，不当成本次结论** |
| 未读 | 本次 `M002-b5-r2-self-raw.md`（当时不存在） |

覆盖：通读 HTML L1–979；通读 GEN-001–018 与 PGM-001–066 定义及锁内 HTML 引文；只读交叉 BSM/ENUM 中本页明文点名的相关条（容量、终端十一端、楼宇巡检类型、账户注销/来源判定、开关默认），**不扫未来模块、不把文档规则当成后端已实现**。

## 3. 第一性原理（grilling 自问自答，不等操作员）

### 3.1 为什么必须存在？

删掉本文件，铁律 4 三份最高解释权 HTML 缺「平台治理层」；PGM-001–066 无权威正文；票务/酒吧/游乐等业务会失去归属、合同、权限、设备、终端开关、留痕与红线的源头。答得上 → 有业务上位需求。

### 3.2 声称的行为有没有被实际执行验证？

- **已执行（类型 4）**：979 行、66 标题、31 个 `#` 链接全部有对应 `id`、标签开闭、锁 66 条引文均为源文件子串、无 `更新日期`/`2026-09-01`/`2026-09-02`/`V0.07`、三条门禁命令 exit 0。
- **不能在本模块执行（类型 1 文档豁免）**：红线、审批、开关、破窗、脱敏、金额精度、并发先成功等，是给后续实现的规则。**文档规则 ≠ 后端已实现。**

### 3.3 未验证就当真的前提

1. 后端/设备/安卓/客人端已按本页执行 —— **未验证**，且禁止扫未来模块。
2. 企微永久保存的法律合规 —— 正文指向《合规与个人信息保护要求》§4.4；本席未读该合规文件为冻结判据，**不新造法律判据**，只记未验证。
3. 浏览器实绘、读屏器、键盘焦点 —— **未跑浏览器**；仅静态结构 + CSS 对比度计算。
4. 注释里「已落地」（L855 等）—— 修订注释，不是用户可见完成声明；**不得当成实现证据**。

删除测试（codebase-design）：删本文件，治理复杂度会散落到 PRD/代码；它赚得住。ponytail-review：无业务发明可删；页面壳 CSS/nav 是呈现既有正文。`Lean already. Ship.`（仅就过度设计；正确性另判。）

## 4. 顺从式：84 条逐项结果

判定口径：锁引文须仍在当前源；标题须落在锁标注行；本模块是说明 HTML，可执行声称只要求文档结构可复现，不要求本文件实现业务运行时。

### 4.1 GEN-001 … GEN-018

| ID | 结果 | 证据 |
|---|---|---|
| GEN-001 | PASS | 本文件是三份解释权之一；无重复/不可达业务块。页面壳见 §7 技术性登记。 |
| GEN-002 | PASS（文档豁免） | 入口为静态 HTML：`<!doctype html>`、`lang="zh-CN"`、无 `<script>`/无 `<form>`。31/31 `href="#…"` 均有 `id`。业务运行时不在本文件。 |
| GEN-003 | PASS（文档不强制测；辅助回归已跑） | 本模块无测试文件。既有 `node tools/verify-entry-anchors.test.mjs` exit 0，见 §6。测试通过不替代结构证据。 |
| GEN-004 | PASS/不适用运行时 | 无用户/网络/文件输入处理。规则在正文，未在本文件执行。 |
| GEN-005 | PASS/不适用运行时 | 权限规则在 PGM-040–044 陈述；本文件无服务端。 |
| GEN-006 | PASS/不适用运行时 | 无写入/事务。 |
| GEN-007 | PASS/不适用运行时 | 「先成功为准」在 L303 陈述，未执行。 |
| GEN-008 | PASS | 本模块无第二套 DTO/OpenAPI。终端自然语言简称与 PGM-047/ENUM「终端十一端」同十一类，见对抗式；**不把 GEN-008 扩成摘要须逐字同名**。 |
| GEN-009 | PASS/不适用运行时 | 无错误路径代码。 |
| GEN-010 | PASS | 静态页无密钥、无注入面。破窗/U 盘是业务规则陈述。 |
| GEN-011 | PASS（文档） | 修订注释与决策台账记录批准；本页不是运行日志。 |
| GEN-012 | PASS/不适用 | 无循环查询。CSS `@media` 有 980px 收窄。 |
| GEN-013 | PASS（辅助门禁，不扩模块） | 004 残留：EXCLUDE 窄匹配 raw，不排除 findings/ledger/收口；`npm run verify:entry-anchors` PASS。不把 tools 所在未来模块整模块纳入。 |
| GEN-014 | PASS/不适用 | 无迁移。 |
| GEN-015 | PASS（基本可用；未做浏览器） | 见 §6 对比度计算；主文 14.76:1、muted-on-panel 4.97:1。标签色 4.46/4.22 未当缺陷：无冻结 WCAG 数值判据，且未实绘。 |
| GEN-016 | PASS | L2 仅 `<!-- 文件版本：V0.08 -->`；L974 无「更新日期」。锁 PGM-066 引文含现脚注且为源子串。 |
| GEN-017 | PASS | 冲突→细化→技术性→添加走完；无添加项；`questions/` 目录不存在。 |
| GEN-018 | PASS | 结论落到 HTML 原文、锁引文子串、命令输出；不用旧 PASS 代替。 |

### 4.2 PGM-001 … PGM-066

方法：从 lock 抽出 66 条 `````html` 引文，检验「引文 ∈ 当前 HTML」且标题出现在标注行。结果：**QUOTE_NOT_IN_SRC=0，TITLE_FAIL=0，LINE_BAD=[]。**

| ID | 锚点行 | 结果 | 要点 |
|---|---:|---|---|
| PGM-001 | 256 | PASS | 底座 + 四条全局原则原文与锁一致。全中文原则约束**平台终端**，不把本说明页 CSS/机读标识当违规。 |
| PGM-002 | 308 | PASS | 五步治理顺序完整。 |
| PGM-003 | 339 | PASS | 模块入口 h2。卡片 13 张对应 13 个业务章节；overview/flow/modules/relations/decisions 不是卡片模块。 |
| PGM-004 | 342 | PASS | 卡片「综合体和集团」→ `#project`。 |
| PGM-005 | 347 | PASS | `#space`。 |
| PGM-006 | 352 | PASS | `#merchant`。 |
| PGM-007 | 357 | PASS | `#contract`。 |
| PGM-008 | 362 | PASS | `#staff`。 |
| PGM-009 | 367 | PASS | `#permission`。卡片序在 device 前，正文 device 在 permission 前；**页未声称卡片序=阅读序**，不新造顺序判据。 |
| PGM-010 | 372 | PASS | 十一端摘要。与 PGM-046/047/ENUM 映射见 §5，非冲突。 |
| PGM-011 | 378 | PASS | `#device`。 |
| PGM-012 | 383 | PASS | `#merchant-side`。 |
| PGM-013 | 388 | PASS | `#open-platform`。 |
| PGM-014 | 393 | PASS | `#audit`。 |
| PGM-015 | 399 | PASS | `#building-ops`。 |
| PGM-016 | 404 | PASS | `#recovery-privacy`。 |
| PGM-017 | 412 | PASS | 单综合体 + 项目归属。 |
| PGM-018 | 415 | PASS | 多对多；城市项目公司≠经营域主管。 |
| PGM-019 | 424 | PASS | 六主体表 + 股权/牌照/场地/资金分记。 |
| PGM-020 | 440 | PASS | 主要功能含集团下钻/KPI 分层。 |
| PGM-021 | 452 | PASS | 业务意义三条。 |
| PGM-022 | 463 | PASS | 空间影响合同/低消/容量/设备/库存。 |
| PGM-023 | 467 | PASS | 平面图导入为需求条目，**不等于后端已实现**。 |
| PGM-024 | 476 | PASS | 容量只按单项目实时在玩人数。只读 BSM L1260/L1294 与 ENUM「容量双阈值」同口径。 |
| PGM-025 | 488 | PASS | 商户/主体。 |
| PGM-026 | 492 | PASS | 合作六类、四类主体、招商前链。 |
| PGM-027 | 507 | PASS | 商户侧边界；第三方规则不写入以免过期。 |
| PGM-028 | 518 | PASS | 合同卡住再放行。 |
| PGM-029 | 522 | PASS | 七类红线、统一审批、租赁生命周期。权限不足「不设放行」与超范围「可申请放行」不是同一命题。L535–536 注释记 2026-06-26 口径已作废，**现行正文**为统一审批模型。 |
| PGM-030 | 544 | PASS | 红线不因审批放行。 |
| PGM-031 | 555 | PASS | 经销商门户/价格/资金。 |
| PGM-032 | 572 | PASS | 人-私人手机-工作手机。 |
| PGM-033 | 576 | PASS | 两条注册路径、设备标识非硬件码。 |
| PGM-034 | 586 | PASS | 企微永久保存；合规论证属外部文件，本席不扩法律判据。 |
| PGM-035 | 599 | PASS | 提成月结、人员服务禁商品化。 |
| PGM-036 | 611 | PASS | 设备范围含入场三道。 |
| PGM-037 | 615 | PASS | 台账/领还/白名单。 |
| PGM-038 | 624 | PASS | 工作手机/平板/一体机职责。 |
| PGM-039 | 636 | PASS | 不计时、场景联动待立项。 |
| PGM-040 | 643 | PASS | 单一功能授权。 |
| PGM-041 | 647 | PASS | 权限公式、高危五类、隔离无关开关。 |
| PGM-042 | 664 | PASS | M1–M10 职级。 |
| PGM-043 | 673 | PASS | 外包/审计；脱敏七项闭集。只读 BSM「账户注销」：商户可查导本商户客户、不得删除 —— 与「来源判定」一致。 |
| PGM-044 | 688 | PASS | 隐私桌台/挂单/经销分权。 |
| PGM-045 | 712 | PASS | 终端开关总则。 |
| PGM-046 | 716 | PASS | 「能控制哪些端」七条自然语言打包，计数仍十一类，见 §5。 |
| PGM-047 | 728 | PASS | 闭集十一类原文与 ENUM「终端十一端」逐字同类。opt-in 与 BSM 已拍板开关默认条同口径。 |
| PGM-048 | 749 | PASS | 商户侧表；隔离句是规则不是实现证明。 |
| PGM-049 | 781 | PASS | 先自有开放能力。 |
| PGM-050 | 785 | PASS | 身份核验+映射。 |
| PGM-051 | 794 | PASS | 第三方资料未到。 |
| PGM-052 | 806 | PASS | 楼宇运营总则。 |
| PGM-053 | 810 | PASS | 机电四组台账。 |
| PGM-054 | 821 | PASS | 能耗；公区不分摊、电损/水损独立。只读 BSM 能耗条同方向。 |
| PGM-055 | 832 | PASS | 巡检四类。ENUM「楼宇巡检类型」=安防/消防/设施设备/环境卫生。注释「L774」是历史行号，**不新造「注释行号必须准」判据**。 |
| PGM-056 | 844 | PASS | 连接物业/财务/供应链/留痕。 |
| PGM-057 | 857 | PASS | 应急+隐私 h2。 |
| PGM-058 | 860 | PASS | 破窗/钥匙闭环。 |
| PGM-059 | 868 | PASS | 手机号唯一反查路径。 |
| PGM-060 | 879 | PASS | 会员/营销/人脸最短留存。人脸 PIPL 例外只陈述，不扩法律条文。 |
| PGM-061 | 892 | PASS | 留痕总则。 |
| PGM-062 | 896 | PASS | 必须留痕清单。 |
| PGM-063 | 911 | PASS | 常见复核。 |
| PGM-064 | 923 | PASS | 正式挂起。 |
| PGM-065 | 935 | PASS | 七条连接。 |
| PGM-066 | 951 | PASS | 17 条 `.question.confirmed`，0 条未确认。标题含「待排期」不等于声称现有待排期项。脚注无日期，锁引文已同步。 |

**顺从式合计：84/84 在本模块文档口径下成立。无锁外新判据。**

## 5. 对抗式（假定实现/文档错误）

最小反例与结果：

1. **日期双写仍在？** `grep` 本 HTML：`2026-09-01`/`2026-09-02`/`V0.07`/`更新日期` 均 0 命中。L2=`<!-- 文件版本：V0.08 -->`。反例不成立。
2. **锁引文漂移（尤其 PGM-066 脚注）？** 66 条引文均 ∈ 源。反例不成立。
3. **死链？** 31 href、18 unique，MISSING_HREFS=[]，IDS_WITHOUT_HREF=[]。
4. **标签不闭合？** html/head/body/main/nav/section(18/18)/table(4/4) 开闭相等；thead=tbody=4。
5. **第十二端 / 客人网页端？** 闭集 L739 与 ENUM L233 均为十一类且「客人无网页端」。L373/L718–724 是同一十一类的简称打包（客人 App+小程序=2，Web+移动管理=2，平板=1，工作手机+私人手机 App=2，商户 App/Web=2，一体机=1，经销商门户=1）。**不是相反命题，不是新枚举。** 依序：非冲突 → 有上位 PGM-047 → **细化**。无已证配置分叉。不启用「逐字同名」锁外判据。
6. **容量与另两份 HTML 相反？** PGM-024 / BSM 容量控制 / ENUM 容量双阈值均为「只按单个项目实时在玩人数，不按场次/时段或点位/设备」。一致。
7. **巡检五类残留？** 正文四类；ENUM 四类并写能耗抄表不属巡检。
8. **脱敏例外与 BSM 账户注销相反？** PGM-043 指向 BSM「账户注销」来源判定；BSM 原文：属该商户的客户可查可导、不得删除。非相反。
9. **开关默认与 BSM 已拍板条相反？** PGM-047 opt-in 与 BSM L1999 同方向。
10. **EXCLUDE 把 findings/ledger/收口放行？** 对 EXCLUDE 实测：`findings.md` KEEP、`ledger.md` KEEP、`M002-00-authority-html-02.md` KEEP、raw EXCL。测试文件对现行四类失效引用断言 status=1。反例不成立。
11. **假完成「已拍板」？** 17 条均为 confirmed 规则清单，不是「后端已上线」声明。
12. **英文展示违规？** 全中文原则对象是后台/客人端/设备/安卓。本页是说明文档；锁引文自身含 App/Web/KPI/opt-in。不扩判据到权威 HTML 零拉丁字母。

未构造「两三件不巧才成立」的极端闸。

## 6. 真实命令输出（verification-before-completion）

### 6.1 锁与源

```
$ sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
$ cat docs/audit-2026-09/plan/criteria-lock.sha256
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
$ wc -l docs/platform-governance-map-for-operations-v0.1.html
979 docs/platform-governance-map-for-operations-v0.1.html
$ sha256sum docs/platform-governance-map-for-operations-v0.1.html
9864216e609df2f319fa2cf7e51b61f06003ce45dff64a1a4098e71435865a68  docs/platform-governance-map-for-operations-v0.1.html
```

结构脚本（stdout 摘要）：H2 18 H3 48 HEAD_TOTAL 66；HREF_COUNT 31 UNIQUE 18 MISSING_HREFS []；LOCK_PGM_COUNT 66 PARSED_PGM 66 TITLE_FAIL 0 QUOTE_NOT_IN_SRC 0；QUESTION_CONFIRMED 17 QUESTION_PLAIN_LINES 0。

### 6.2 既有无副作用门禁

```
$ node tools/verify-entry-anchors.test.mjs
PASS：历史 raw 放行，现行 findings、ledger、模块收口和普通文档的失效引用仍拒绝。
EXIT:0

$ npm run verify:entry-anchors
[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1563 条；被检 Markdown 350 份；条目名引用 1514 处（注册表原名 1357 处，正文唯一定位 157 处）。
[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。
EXIT:0

$ npm run verify:html-ledger-sync
[html-ledger-sync] PASS: 改动了 1 份业务 HTML，决策台账已同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@001878a1。
EXIT:0
```

`html-ledger-sync` 的「1 份业务 HTML」对应工作区相对 origin/main 仍有治理地图未提交改动（申请20/M002-001）。B3 本就不提交。本席不提交。

### 6.3 对比度（静态计算，非浏览器）

ink-on-panel 14.76；muted-on-panel 4.97；muted-on-bg 4.68；blue-on-white 5.17；green-tag 4.46；amber-tag 4.22。未当缺陷，理由见 GEN-015。

## 7. 修复残留与技术性页面壳

### 7.1 M002-001（日期）残留

申请20批准范围：L2 去掉日期并 V0.07→V0.08；L974 只删「更新日期：2026-09-01。」。

现文符合。镜像：`entry-registry.csv` PGM-E0307 L1049 `entry_body` 以「用途：」起、无该日期；`authority-rule-ledger.csv` PGM-0192 L723 `source_text` 同。`business-decision-ledger.md` V0.18 记录批准及现 SHA `9864216e…865a68`。无源码回退。**残留关闭，无新缺陷。**

### 7.2 M002-004（门禁历史 raw）残留

`tools/verify-entry-anchors.mjs:40`：

```
EXCLUDE = /^docs\/audit-2026-09\/modules\/M\d{3}-b(?:1|4-r\d+|5-r\d+)-(?:self|grok)-raw\.md$|(changelog|…)/i
```

实测不排除 findings/ledger/模块收口/plan。测试 V0.01 用真实 CLI：历史 raw 放行、现行四类拒绝。全库门禁 PASS。**残留关闭。**

### 7.3 M002-002 终端简称

独立重判：细化、无工程缺陷。不重新开冲突。

### 7.4 技术性页面壳（无业务发明，逐项登记，不新造 ID）

范围：L1–216 头/CSS、L217–253 导航、节壳、L974–979 脚注闭合（日期已归 001）。

无业务发明理由：只呈现本页已有说明；不引入实体/枚举/状态/权限/价格/默认值/流程。无 script/form。导航 18 个目标 = 18 个 section id。UTF-8/zh-CN。卡片序≠正文序不构成业务规则。

工程缺陷：无。仅登记，对应既有 M002-003 同类，**本次不新开 GR**。

## 8. 发现清单（一次性列齐）

本次新的临时 GR：**无。**

无冲突待删、无添加、无未答 `questions/`、无铁律 4 HTML 改申请。未执行边界（后端、法律实证、浏览器）已在 §3.3 写明，**不折成待补项**。

## 9. B5 结论

**B5 PASS。**

84/84 在文档模块口径下成立；001/004 残留已用源文+镜像+命令关闭；002/003 无新缺陷；锁引文与 979 行源一致；三条既有门禁 exit 0。模块最终 PASS 仍须两席完整报告及四值校验后由调度席判定；本席不得收口、不得改 ledger。

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M002 B5-r2 Grok 修复后全量独立审计 V0.02

席位：Grok（唯一外审）。步骤：B5-r2。模块：M002 — 业务需求正本／需求HTML／第2段。当前 471 模块计划。B3 累计 2（本轮跳过 B3/B4，次数不增不重置）。ledger 本模块行：`IN_PROGRESS`。控制提示词：160-audit-prompt.md V0.17。

本席只向本路径追加；未改 HTML/源码/锁/sidecar/ledger/findings/questions/计划；未派子代理；未第三审；未提交/推送/部署/安装/新 worktree；未新增测试/探针/新审计产物。不继承 B1/B2/旧 B5 PASS。本席票不是模块 PASS。

正本：`/home/james/complex-management` `main`，`HEAD == origin/main == 242f487233d028173cb9198eecd079f5d877405c`。范围仅 `docs/platform-governance-map-for-operations-v0.1.html`（979 行，文件头注释 V0.11）。映射 18GEN+66PGM=84。findings V0.89 现行 M002 队列 001—004 全否，仅作队列事实，不代替本席发现。

用户已明确：BSM1020「来源枚举项为业务参数，按项目可配」、BSM1068「打折原因分类可配」是原意，不是冲突或缺陷。无新 HTML 修改授权。不重开 M001。

## 0. 技能预检与收窄使用

按意图执行 V0.17§6；先读实际 `SKILL.md`，技能不得要求人闸/第三席/改文件/加判据。grilling/grill-me 自问自答，不等操作员、不派子代理。ubiquitous-language 只借术语对齐，不写 `UBIQUITOUS_LANGUAGE.md`。

| 技能 | 路径与核验 | 本次用法 |
|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` 已读 | 开工技能自检；用户指令优先于技能 |
| grilling | mattpocock `skills/productivity/grilling/SKILL.md` 已读 | 对抗/第一性原理追问结构；自问自答 |
| grill-me | mattpocock `skills/productivity/grill-me/SKILL.md` 已读（本体=Run grilling） | 同上 |
| codebase-design | mattpocock `skills/engineering/codebase-design/SKILL.md` 已读 | 支撑已冻结 general：本文件是治理规则的深模块（18 节导航接口 / 66 锚点实现） |
| diagnosing-bugs | mattpocock `skills/engineering/diagnosing-bugs/SKILL.md` 已读 | 只读源与复现；门禁红信号用于范围外事实核验，不修 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` 已读 | 只借读错误→复现→追数据流；禁止 instrumentation/改码 |
| verification-before-completion | superpowers 同名已读 | PASS 前实跑命令，不凭印象 |
| ponytail-review | ponytail `4.9.0/skills/ponytail-review/SKILL.md` 已读 | 只提证据不改 |
| tdd / test-driven-development | 均已读 | HTML 不强制落测试；本阶段禁止新增测试 |
| ubiquitous-language | `/home/james/.qwen/skills/ubiquitous-language/SKILL.md` 已读 | 收窄：对照 CONTEXT.md 与三 HTML 术语；**不写文件** |
| batch-grill-me | 本环境技能树无独立 `SKILL.md` | **缺口**，不冒充已调用 |
| git-guardrails-claude-code | 存在于 mattpocock `skills/misc/` | 本阶段无 git 写，不调用 |
| writing-for-agents | 存在 | 本阶段禁止写收口文件，不调用本体 |

CONTEXT.md V0.01 已读作诊断入口（平台/项目公司/十一类终端/功能权限点等），业务最终解释权仍归三份 HTML，不以 CONTEXT 反向改写 PGM。

## 1. 锁与基线（本席独立实算）

命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`

| 对象 | 实算/读取 |
|---|---|
| 锁文件 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 字节，锁文版本 V0.11） |
| sidecar `plan/criteria-lock.sha256` | 单行同值 |
| ledger 顶部 | 同值；`LOCK-AMEND-COUNT: 1` |
| PGM HTML | SHA256 `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c`；979 行；UTF-8 严格解码 OK；无 BOM/NUL/CRLF；`<!-- 文件版本：V0.11 -->`；页脚无「更新日期」 |
| M002 映射 | 84 ID，GEN 18 + PGM 66，无缺无重无其它族 |
| PGM 定义 | 66 条来源行均指向现 HTML 对应 `h2`/`h3`；66 条摘录标题与源行标题逐字相等 |

本席不把四值校验写成已完成的模块收口动作（那是根在两席落盘后的闸 1）。

## 2. 979 行全覆盖表

文件：`docs/platform-governance-map-for-operations-v0.1.html`。`wc -l`=979；`splitlines`=979。无脚本、无表单、无外部 http(s)。

| 行段 | 结构 | 覆盖的冻结 ID | 本席动作 |
|---|---|---|---|
| 1–215 | doctype / 版本注释 / CSS / head | GEN-001/012/015/016 | 全文读；标签开合；`lang=zh-CN` |
| 216–252 | 页头、侧栏 18 链 | GEN-015/016；导航目标=18 section | 逐链对照 |
| 253–305 | `#overview` | PGM-001；GEN-001/006/007/016/018 | 五条全局原则+但书 |
| 306–336 | `#flow` | PGM-002 | 五步顺序 |
| 337–409 | `#modules` 13 卡 | PGM-003–016 | 卡 href 与正文锚点一一对应（存在双射，不是同序） |
| 410–460 | `#project` | PGM-017–021 | 主体表、股权/场地、集团下钻 |
| 462–485 | `#space` | PGM-022–024 | 平面图发布高危；容量口径 |
| 487–515 | `#merchant` | PGM-025–027 | 六类合作、四类主体、招商细则 |
| 517–569 | `#contract` | PGM-028–031 | 七类红线、审批时限、经销资金 |
| 571–608 | `#staff` | PGM-032–035 | 注册双路径、企微永久保存、提成 |
| 610–640 | `#device` | PGM-036–039 | 三道环节设备；平板只点酒；不计时 |
| 642–709 | `#permission` | PGM-040–044 | 权限公式、高危五类、脱敏闭集、挂单 |
| 711–746 | `#terminal-switch` | PGM-045–047 | 十一端闭集+别名；opt-in；优先级 |
| 748–778 | `#merchant-side` | PGM-048 | 三类商户能/不能 |
| 780–802 | `#open-platform` | PGM-049–051 | 先自有后第三方 |
| 804–853 | `#building-ops` | PGM-052–056 | 机电/能耗/巡检四类 |
| 855–889 | `#recovery-privacy` | PGM-057–060 | 破窗、查询通道、会员/人脸 |
| 891–932 | `#audit` | PGM-061–064 | 留痕清单、复核、挂起结案 |
| 934–948 | `#relations` | PGM-065 | 七条连接 |
| 950–972 | `#decisions` | PGM-066 | 已拍板列表；待排期当前空集 |
| 974–979 | 页脚解释权 / 闭合 | GEN-016/018；与另两份平级 | 全文读 |

结构命令摘要：`h2`=18、`h3`=48、合计 66=锁闭集；`section` 开合 18/18；`nav` 18 href 与 18 id 差集空；`system-card` 13 href 均落在 id 上；全部 31 个 `#` 链接无失效；`html.parser` errors=0、unclosed=[]。

## 3. 第一性原理线

grilling 自问自答（不等操作员）：

❓ **Q1 为何必须存在**：删掉本文件，三份权威缺治理层正本，PGM-001–066 无承载。答：必须存在。证据类型 1。

❓ **Q2 声称的行为是否实际执行**：本文件自称「不讲技术实现，只讲管理关系」（L223）。可验证的是文档自身：版本、锚点、UTF-8、链接、与锁摘录标题一致。不可用静态文本声称后端交易/设备/合规已跑通。证据类型 4（命令）+ 1（原文）。

❓ **Q3 未验证前提**：服务端授权、金额事务、设备受管、开关运行时兜底、破窗 U 盘、人脸最短留存、CSV 与本 HTML 同步——均不在 M002 文件清单内，本席标「未验证」，不发明本模块缺陷。

逐块：

1. **为何存在**：L224 一句话理解 + L974 用途；删则票务/酒吧/游乐/供应链/财务/第三方失去「先听这层规则」的权威出处（PGM-001/065）。
2. **实际执行证据**（类型 4）：
   - `wc -l` → 979
   - `python3` UTF-8 严格解码 OK；bytes=103015
   - 31 链 / 18 唯一目标全部可解析；内存注入 `href="#no-such-anchor"` 后 broken=`['no-such-anchor']`（负例证明检查能失败）
   - 锁 66 条来源行 tag 与标题 0 mismatch
3. **未验证前提**：实现模块（M009 起）是否按原文执行；`verify:entry-anchors` 基线红（见 §7），其失败项全是 BSM 镜像，不是本文件 PGM 条款。

codebase-design：外部缝是「三份 HTML 共同解释」；接口小（18 节+66 锚点），实现深（红线、权限公式、终端开关、破窗）。删除测试：删本文件则复杂度回到全体调用方各自发明治理规则。本模块不应再加一层「摘要必须复述全部但书」的锁外接口。

## 4. 顺从式线：84 条逐项

判定口径：冲突→细化→技术性→添加。文档模块的 GEN 适用于「本 HTML 是否自洽、可导航、无业务发明伪装」；不把未纳入范围的后端缺实现写成 M002 缺陷。

### 4.1 GEN-001 … GEN-018

| ID | 判定 | 2.3 | 说明 |
|---|---|---|---|
| GEN-001 | 满足 | 1 | 979 行均承载治理规则或阅读壳；CSS/目录服务阅读，无重复不可达业务块 |
| GEN-002 | 满足（文档豁免可执行产物） | 4 | 声明为说明页；导航/结构命令与声明一致；不声称本文件可构建后端 |
| GEN-003 | 满足（HTML 不强制测试） | 4 | 未新增测试；既有 `verify-entry-anchors.test.mjs` 红在 BSM CSV，见 §7，不冒称绿 |
| GEN-004 | 满足 | 4 | 无 script/form/外链，无可信输入首次使用面 |
| GEN-005 | 满足（陈述，非实现） | 1 | 原文规定默认拒绝、显式拒绝优先、高危先批；本文件不是服务端 |
| GEN-006 | 满足（陈述） | 1 | 金额到分+涉税例外+尾差主体；守恒红线⑥；无本模块事务实现 |
| GEN-007 | 满足（陈述） | 1 | 先成功为准；重试只认一次；例外仅红线⑤ U 盘逐笔 |
| GEN-008 | 满足 | 1 | 十一端闭集与 BEQR「终端十一端」逐字同一串；容量/巡检四类/能源费与另两份合读不冲突 |
| GEN-009 | 满足（陈述） | 1 | 阻断、升级、不得默认通过/拒绝、映射缺失挂差异 |
| GEN-010 | 满足 | 1+4 | 无注入面；陈述加密/脱敏/暗纹/导出密码；不凭静态文本称已落地 |
| GEN-011 | 满足（陈述） | 1 | 留痕清单、开关变更留痕、破窗复盘 |
| GEN-012 | 满足 | 4 | 静态页；`@media` 收窄栅格；无循环/查询 |
| GEN-013 | 不适用本文件构建 | 4 | 无本模块依赖/CI 入口；不把全仓门禁绿当作本模块证据 |
| GEN-014 | 不适用 | 4 | 无迁移 |
| GEN-015 | 满足 | 1+4 | `lang=zh-CN`；18 链可键盘激活的原生 `<a>`；对比色 token；窄屏单列；展示约定见 PGM-001，本页是说明页不是终端 |
| GEN-016 | 满足 | 4 | V0.11、979 行、链接全部可复现；页脚无旧更新日期；完成声明未把 CSV 门禁写成已绿 |
| GEN-017 | 满足 | 1 | 本席按四分类走完；findings 空；不把 B/C 当缺陷 |
| GEN-018 | 满足 | 1+4 | 结论落到 HTML 原文与命令输出；不引用他席意见为据 |

### 4.2 PGM-001 … PGM-066

| ID | 源行 | 判定 | 2.3 | 要点（合读但书，不要求短摘要复述全部例外） |
|---|---|---|---|---|
| PGM-001 | 256 | 满足 | 1 | 归属/资格/责任；可溯源+安检例外；全中文四样例外；金额到分+涉税+尾差；先成功；仲裁顺序法定>证据>可配置 |
| PGM-002 | 308 | 满足 | 1 | 对象→合同→人员→设备→留痕，五步齐全 |
| PGM-003 | 339 | 满足 | 4 | 入口 h2 存在；13 卡覆盖 13 个模块章 |
| PGM-004 | 342 | 满足 | 1 | 一综合体+预留集团 |
| PGM-005 | 347 | 满足 | 1 | 楼层到设备位置 |
| PGM-006 | 352 | 满足 | 1 | 商户/主体/联系人/合作/范围 |
| PGM-007 | 357 | 满足 | 1 | 合同卡流程 |
| PGM-008 | 362 | 满足 | 1 | 注册/私机/工作机/企微 |
| PGM-009 | 367 | 满足 | 1 | 单一功能权限 |
| PGM-010 | 372 | 满足 | 1 | 卡片列举十一端，与 L739 闭集同指，App/Web 为说明别名 |
| PGM-011 | 378 | 满足 | 1 | 设备台账卡 |
| PGM-012 | 383 | 满足 | 1 | 商户侧边界卡 |
| PGM-013 | 388 | 满足 | 1 | 开放接口卡 |
| PGM-014 | 393 | 满足 | 1 | 留痕复核卡 |
| PGM-015 | 399 | 满足 | 1 | 楼宇运营卡 |
| PGM-016 | 404 | 满足 | 1 | 应急与隐私卡 |
| PGM-017 | 412 | 满足 | 1 | 项目归属+未来集团视图 |
| PGM-018 | 415 | 满足 | 1 | 顶层平台/物理项目/域主管/城市项目公司；长沙通用卡兑付≠管理权 |
| PGM-019 | 424 | 满足 | 1 | 六主体表+股权/牌照/场地/借款分红；80/20 仅默认建议 |
| PGM-020 | 440 | 满足 | 1 | 档案/集团下钻/KPI 分层；数据范围无后台设置项 |
| PGM-021 | 452 | 满足 | 1 | 防混数据、总览预留 |
| PGM-022 | 463 | 满足 | 1 | 空间影响合同/低消/容量/设备/库存/责任 |
| PGM-023 | 467 | 满足 | 1 | 平面图导入；发布高危二次确认 |
| PGM-024 | 476 | 满足 | 1 | 容量只按项目实时在玩人数；与 BSM/BEQR 容量条一致 |
| PGM-025 | 488 | 满足 | 1 | 商户不是只录名 |
| PGM-026 | 492 | 满足 | 1 | 六类合作、四类主体、权责配置、招商前链、黑名单例外须单独授权 |
| PGM-027 | 507 | 满足 | 1 | 本商户全开放；他商户/全局不可看；第三方规则不写入以免过期 |
| PGM-028 | 518 | 满足 | 1 | 违规先卡 |
| PGM-029 | 522 | 满足 | 1 | 权限不足无放行通道；超品类可申请放行；七类红线；①②③连 U 盘不可开；④⑤⑥⑦ U 盘仅逐笔；审批超时升级不默认；证照宽限一次 |
| PGM-030 | 544 | 满足 | 1 | 红线不因审批放行 |
| PGM-031 | 555 | 满足 | 1 | 经销门户启用条件、价格目录、职责分离、停用即断会话 |
| PGM-032 | 572 | 满足 | 1 | 人/私机/工作机对应 |
| PGM-033 | 576 | 满足 | 1 | 双注册路径；自营视同公司自有；换机先解绑 |
| PGM-034 | 586 | 满足 | 1 | 企微永久保存；完整原文最小授权；与 PGM-066 同口径 |
| PGM-035 | 599 | 满足 | 1 | 提成月结代扣个税；陪酒不得商品化 |
| PGM-036 | 611 | 满足 | 1 | 含入场三道环节设备 |
| PGM-037 | 615 | 满足 | 1 | 台账/领还/白名单/管控留痕 |
| PGM-038 | 624 | 满足 | 1 | 工作机核销+交接；平板只点酒；一体机能力闭集 |
| PGM-039 | 636 | 满足 | 1 | 不计时/不钟点房；场景联动未立项不定义 |
| PGM-040 | 643 | 满足 | 1 | 单一功能权限点 |
| PGM-041 | 647 | 满足 | 1 | 最终权限公式；项目隔离无关闭开关；高危五类先批；新功能未归类先按高危 |
| PGM-042 | 664 | 满足 | 1 | M1–M10 职级不替代功能点 |
| PGM-043 | 673 | 满足 | 1 | 外包最小够用；审计只读；七项个人信息闭集+商户本客例外；导出强制密码 |
| PGM-044 | 688 | 满足 | 1 | 隐私桌台/挂单/经销权限；结账锁券；经销账号不套内部模板 |
| PGM-045 | 712 | 满足 | 1 | 关则不可用，开仍校权限范围设备 |
| PGM-046 | 716 | 满足 | 1 | 七条说明别名，合读 L739 仍是十一类，不新增端类 |
| PGM-047 | 728 | 满足 | 1 | 优先级；缺行 opt-in 关；拉取失败兜底关（留痕例外）；钱规则禁止灰度；十一端闭集与 BEQR 同一字符串 |
| PGM-048 | 749 | 满足 | 1 | 三商户类型表；末段游乐闸门与表合读，不把餐饮行发明成游乐专用权限 |
| PGM-049 | 781 | 满足 | 1 | 第三方资料未到不阻塞自有开放 |
| PGM-050 | 785 | 满足 | 1 | 身份核验+范围+门店映射；缺映射挂差异 |
| PGM-051 | 794 | 满足 | 1 | 后续按资料接 |
| PGM-052 | 806 | 满足 | 1 | 楼宇本体≠工作设备/游乐设备 |
| PGM-053 | 810 | 满足 | 1 | 四组台账+状态枚举 |
| PGM-054 | 821 | 满足 | 1 | 代收不加价；公区不摊；损费另计；与 BEQR 能源费条合读（分表含设备/回路归集） |
| PGM-055 | 832 | 满足 | 1 | 巡检四类；能耗抄表不在巡检；与 BEQR「楼宇巡检类型」一致 |
| PGM-056 | 844 | 满足 | 1 | 连物业/财务/供应链/留痕 |
| PGM-057 | 857 | 满足 | 1 | 破窗+隐私章题 |
| PGM-058 | 860 | 满足 | 1 | 无备用钥匙；作废即不可用；全程留痕 |
| PGM-059 | 868 | 满足 | 1 | 加密；界面脱敏无关闭；手机号唯一查询路径；单据点开客人=查询 |
| PGM-060 | 879 | 满足 | 1 | 禁止后台导入会员；积分预算守恒；禁止个人定向；人脸最短留存 |
| PGM-061 | 892 | 满足 | 1 | 过程可追溯 |
| PGM-062 | 896 | 满足 | 1 | 十类必须留痕 |
| PGM-063 | 911 | 满足 | 1 | 免单退款调账等复核 |
| PGM-064 | 923 | 满足 | 1 | 挂起无额度但阻交班日结；取消与付款互斥 |
| PGM-065 | 935 | 满足 | 1 | 七条连接均有「为什么必须连」 |
| PGM-066 | 951 | 满足 | 1 | 现行条目均为已拍板/已确认；标题含「待排期」但当前无待排期项，空集不是原文自否；L960 留痕条含安检例外，与 PGM-001 合读 |

卡片顺序（权限/终端/设备、audit 先于楼宇）与侧栏/正文顺序不同：注释要求的是「卡片与正文锚点一一对应」，实测 13/13 双射、0 失效。不另设「必须同序」锁外要求。

## 5. 对抗式线（正常首次使用可遇，非极端多前提）

grilling 构造正负例；事实已用原文+命令核。

**导航**

- 正：点击侧栏 `#contract` → 存在 L517 `section id="contract"`。
- 负：内存把首链改为 `#no-such-anchor` → broken=`['no-such-anchor']`。现网 31 链无此失败。

**权限 / 红线（PGM-029/041）**

- 正：超合同品类下单 → 当场拦下，可申请审批放行（L527）。
- 负：本人权限不足 → 「不设放行通道」，须换有权限的人（L526），与超品类放行不是同一口子。
- 负：未满 18 岁 / 未巡检开放 / 坐满仍放人 → ①②③「系统不提供开放入口」，U 盘最高权限也不可（L528）。
- 正：重复提交只生效一次（⑤）；U 盘可对当前这一笔开放 ⑤⑥⑦，不得整类关校验。

**金额（PGM-001/031/044）**

- 正：非涉税金额四舍五入到分。
- 负：用「统一到分」改写涉税结果 → 原文禁止。
- 正：跨域组合通票尾差由娱乐平台公司承担（L302）。
- 负：把尾差默认甩给城市项目公司 → 禁止。
- 正：结账锁券与支付同时生效，任一失败均不生效（L700）。

**参数 / 开关（PGM-047）**

- 正：后台无开关配置行 → 默认关闭（与 BSM 已拍板第 35 条同一 opt-in 括注，说明页术语，不把 opt-in 发明成第十二类终端或新状态机）。
- 负：部署脚本「视同打开」缺行 → 禁止。
- 正：全局关 + 商户开 → 高层级关闭压倒。
- 负：分账规则按设备灰度 → 禁止；参数取值按已写粒度配置 ≠ 灰度。

**删除 / 停用 / 切换**

- 正：经销商停用 → 立即终止会话与对接授权（L564）。
- 正：换手机 → 先解绑旧再绑新（L580）。
- 负：所属商户/跨项目变更后旧范围权限立即失效，新范围须先审批（L658）；高危不因在模板内免批。
- 正：证照宽限期经最高权限批一次；超过不得再延长（L531）。
- 负：转租 → 不支持，须先退租再招商（L537）。

**终端别名（PGM-010/046/047）**

- 正：L739 十一类与 BEQR 终端十一端同一字符串。
- 负（驳回错误发明）：把「商户侧 App/Web」「工作人员平板 App」当成第 12/13 类 → 与用户口径及 L739 闭集不合。说明页自然语言别名 ≠ 新增机器端类。

**查询 / 隐私（PGM-059/043）**

- 正：用手机号走专门通道可定位客人。
- 负：用姓名/订单号反查客人 → 禁止。
- 正：商户导出本商户客户信息不脱敏（指向 BSM 账户注销来源判定）。
- 负：跨商户未脱敏导出 → 仅 U 盘最高权限逐次留痕。

**B/C 原意（不重开 M001）**

- BSM L1020 来源枚举按项目可配、L1068 打折原因分类按项目可配，用户明确就是原意。本席不把它们写成 PGM 冲突，也不对 PGM 加「枚举不可配」锁外句。

**未把下列写成 M002 缺陷**

- 卡片与侧栏不同序（双射成立）。
- PGM-066 无待排期项（空集）。
- L540「铺位租赁状态随意向与合同自动联动」与 BEQR 六态/BSM 看板合读：括注举例+「等」+「不可租人工标注」，不要求短句复述空置/已退租。
- 页内可见 App/Web/KPI/P0/AI/HRBP/VIP/opt-in：或为三 HTML 共用说明术语，或为禁止英文的反例（active/draft）；规则主语是「平台所有终端」展示，本文件是说明正本不是终端产品 UI。
- `verify:entry-anchors` 六处红：全是 BSM 镜像，见 §7。

ponytail-review（只列不改）：`Lean already. Ship.` 权威 HTML 无当删业务发明；CSS 为 GEN-015 阅读壳。`net: -0 lines possible.` 无修改授权。

## 6. 本席 findings

空集。无冲突、无工程缺陷、无添加、无新 HTML 自身问题需铁律 4 上报。不把范围外 CSV 门禁塞进 M002，也不复用旧 001–004 当本轮新缺陷。

建议队列：无。

## 7. 范围外已披露事实的独立实核（非本轮预置 PASS）

命令 1：`npm run verify:entry-anchors`  
真实退出码 **1**。输出要点：

```
[entry-anchors] FAIL：6 处不合格。
  - 注册表条目正文在 HTML 现文中找不到：BSM-E0433 BSM「处理后回访客人或商户，验收合格才关闭」
  - 注册表条目正文在 HTML 现文中找不到：BSM-E0736 BSM「文档版本」
  - 账本正文与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」
  - 账本正文与 HTML 现文对不上：BSM-0530「文档版本」
  - 账本镜像列与 HTML 现文对不上：BSM-0283 … preconditions 片段
  - 账本镜像列与 HTML 现文对不上：BSM-0283 … state_transition 片段
```

六处均为 **BSM** 的 A 回访概括 + 此前页脚/文档版本镜像（对应披露的 L2003 日期类），**不是** D 的 L2 注释，**没有 PGM 不一致**。

命令 2：`node --test tools/verify-entry-anchors.test.mjs`  
真实退出码 **1**。断言在测试文件 L28 基线镜像检查即失败（`1 !== 0`）；tests 1 / pass 0 / fail 1。未到任何「历史 raw 排除」断言。

归属（计划原文，非本模块审计）：

- `docs/prd-audit/authority-rule-ledger.csv` → M425（`plan/modules.md` L6905 段，FILES 含该 CSV）
- `docs/prd-audit/entry-registry.csv` → M427（L6927 段）
- `tools/verify-entry-anchors.mjs` → M008（L98 段）
- `tools/verify-entry-anchors.test.mjs` → M415（L6745 段）

M002 只含 PGM HTML，无本模块发布。不得冒称上述命令绿，也不得把失败写成 M002/旧 004 复发。

## 8. 本席结论与边界

- **本席 B5：PASS**（整模块 979 行覆盖、84 ID 核对、三线执行、证据为 HTML 原文或可复现命令、questions 三份首个答复分别为删除/保留/保留、无未定稿缺陷、本报告首行锁哈希有效）。
- **新 HTML 问题：无。**
- **模块 PASS：否。** 不得以本席单票冒称；须双席合取且 B5 后四值全等后由根写 ledger。
- **B3 计数：** 保持累计 2；本轮无 B3，文件名仍 r2。
- **未验证：** 后端/设备/支付/合规运行时行为。
- **未做：** 改任何受禁文件、派子代理、读他席 B5/self 正文取意见、重开 M001。

命令退出码汇总：`sha256sum`/`python` 结构检查 exit 0；`npm run verify:entry-anchors` exit 1；`node --test tools/verify-entry-anchors.test.mjs` exit 1。
