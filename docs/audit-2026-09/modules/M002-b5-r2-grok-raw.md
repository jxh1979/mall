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
