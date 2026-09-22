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

CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M002 B5-r2 Grok 修复后全量独立审计 V0.03

席位：Grok（审计席乙 / 外审否决席）。步骤：B5-r2（跳过空队列 B3/B4 后的整模块全量重审，不是只看 diff）。模块：M002 — 业务需求正本／需求HTML／第2段；计划 `docs/audit-2026-09/plan/modules.md` V0.04，**471** 模块；本模块 `NORMAL`，1 文件。B3 已执行次数：**2**（本轮新增修复 **0**，次数不增不重置）。控制：160-audit-prompt.md V0.17。ledger 入口事实：`IN_PROGRESS`（表行历史备注「当前B5-r2双PASS」只作旧轮次文字，**不得继承旧 B5 PASS**）。本席只向本路径追加；未读本次 `M002-b5-r2-self-raw.md`（该路径本会话 `ls` 存在，**未打开正文**）；未改 HTML/源码/锁/sidecar/ledger/findings/questions/收口文件；无 MCP、子代理、fetch、commit、push、worktree、部署。不继承 B1 V0.10 PASS、不继承 B2 V0.11、不继承本文件 V0.01/V0.02 PASS。本席票不是模块 PASS。

正本：`/home/james/complex-management` `main`。唯一范围：`docs/platform-governance-map-for-operations-v0.1.html` 当前 **V0.13** 全 979 行。映射 18 GEN + 66 PGM = **84**，三线。锁 **V0.14** 独立实算（值只写在本次报告首行独占字段，正文不重复该独占字段）。findings V3.34 顶部「当前 M002 新锁B2双PASS定稿」四项队列全否，仅作范围定位，不当成本次结论。操作员本轮：680/871 联系人例外与 656 单次规则保持；PGM-047 锁已同步「查看考勤」；M009 单次消费代码未修不能当成本 HTML 缺陷。全局 html-ledger-sync 红如实披露，归属 M426/M008；不回改获批 HTML，不声称全局绿。

## 0. 技能预检（本席 Linux 实核 SKILL.md，不抄旧票路径）

按意图执行 V0.17 §6。grilling / grill-me **自问自答**，不等操作员、不派子代理。ubiquitous-language 本体要写 `UBIQUITOUS_LANGUAGE.md`，本节点 **不写该文件**，只借术语对齐。i-have-adhd「列表≤5」与本票「84 项全表 + 首行独占哈希」冲突时，**任务优先**。

### 0.1 已核到且本节点按收窄口径使用

| 技能 | 实核路径 | 本次用法 |
|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检；用户指令优先于技能 |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗/第一性原理追问树；自问自答 |
| grill-me | 同目录 `grill-me/SKILL.md`（全文仅 Run grilling） | 并入自问自答 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 删除测试：本文件是三份解释权 HTML 之一 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 只做可复现命令读源；不改文件 |
| systematic-debugging | `.../superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流；用于 html-ledger-sync exit1 归因。禁止 instrumentation |
| verification-before-completion | superpowers 同名 | 任何 PASS/FAIL 前用本报告内**本次**命令输出 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 按下游可差集、可归因来写；**不写**模块收口 |
| ponytail-review | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删，只举证不修 |
| i-have-adhd | `/home/james/.claude/plugins/marketplaces/i-have-adhd/skills/i-have-adhd/SKILL.md`（cache `0.1.0` 路径亦存在） | 输出形态；84 项全表优先 |
| ubiquitous-language | `/home/james/.qwen/skills/ubiquitous-language/SKILL.md` **存在** | 收窄：对照本页专名；**不写** `UBIQUITOUS_LANGUAGE.md` |

### 0.2 已核到、本节点不适用（不冒充已调用）

| 技能 | 原因 |
|---|---|
| brainstorming / writing-plans | 阶段 A 已完成 |
| tdd / test-driven-development | B5 禁止修复；静态 HTML，runbook 不强制落测试；**不强制后端测试** |
| requesting-code-review / receiving-code-review | 非本步；禁止 IMPLEMENT |
| resolving-merge-conflicts | 无进行中的 merge/rebase |
| git-guardrails-claude-code | 存在于 mattpocock `skills/misc/`；本席无 git 写 |
| ponytail | 默认强度用于 B3，本次不用 |
| 提示词划掉的 executing-plans / 并行子代理 / worktree / finishing / research / ponytail-audit\|debt\|gain / domain-modeling / mattpocock code-review | 存在但不调用 |

### 0.3 缺口（如实）

| 技能 | 结果 |
|---|---|
| batch-grill-me | `find` `/home/james/.claude` `/home/james/.agents` `/home/james/.codex` `/home/james/.grok` `/home/james/.qwen` **无此 SKILL.md**。对抗只用已核到的 grilling/grill-me |

## 1. 本次实核 vs 同字节复用（必须分开写）

### 1.1 本次命令实核（本会话跑出，不引用旧票结论）

锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` — 独立实算值**只写在本次报告首行独占字段**。锁文版本 **V0.14**；字节 **891182**。sidecar 单行内容与本次报告首行独占字段同值；`sha256sum docs/audit-2026-09/plan/criteria-lock.sha256` = `c4f8b2c53239206e0eb7b08c1a219049cf785bf9c233f93d66f7a8f0617c111f`（哈希的是含换行的 sidecar 文件，不是锁正文）。ledger 顶行锁哈希与本席实算同值。`LOCK-AMEND-COUNT: 1`。ledger 顶部版本行现为 V7.61。本席不把四值校验写成已完成的模块收口动作。

```
sha256sum docs/platform-governance-map-for-operations-v0.1.html \
  docs/business-system-map-for-operations-v0.1.html \
  docs/business-enum-quick-reference-v0.1.html
5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f  docs/platform-governance-map-for-operations-v0.1.html
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1.html
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
```

治理 HTML：字节 **103798**、`wc -l` **979**、`splitlines` 979、空行 27 / 非空 952。UTF-8 严格解码 OK；无 BOM / NUL / CRLF；以 `\n` 结尾。L2 仅 `<!-- 文件版本：V0.13 -->`。`V0.13` 于本文件命中 **1**；`更新日期` / `2026-09-01` / `2026-09-02` / `V0.07` / `V0.12` 于本文件均为 **0 / False**。

```
git rev-parse HEAD
e456ba96c84185c8d39f18f548011daac48a6466
```

分支 `main`。工作区相对 `HEAD` 该 HTML **未提交 3 处**（`git diff HEAD`：L2 `V0.12`→`V0.13`；L680 末句补商户档案页面查看例外切断导出/打印；L871 补第一/第二联系人授权查看例外）。审计对象是**主工作区当前字节**，不是 `HEAD` blob。这三处即操作员已批准的 680/871 文案 + 版本注释。

本会话 Python `html.parser` + 正则（未落盘 helper）：

| 检查 | 本次结果 |
|---|---|
| h2 / h3 / 合计 | 18 / 48 / **66**（等于锁闭集 PGM 66） |
| id / `href="#…"` | 18 个 id 与 18 个唯一目标一一闭合；缺失 0；孤儿 0；重复 id 0；hash href **31**（侧栏 18 + `system-card` 13） |
| 外部 href / `<script>` / onclick / form | 0 / 0 / 0 / 0 |
| `outline:none` / `@import` / `@font-face` / `url(http` | 均无 |
| HTML 解析栈 / parse error | 空 / 0 |
| `question confirmed` / 未确认 `question` | **17** / **0** |
| 「考勤打卡」于本文件 | **False**；「查看考勤」**True**（L741） |
| 锁 M002 映射 | 84 ID，唯一 84；GEN 18 + PGM 66；无其它族 |
| 锁 PGM 来源行 vs 现文该行是否为标注 tag（h2/h3） | **66/66**，失配 0 |
| 锁 PGM 标题 vs 现文标题 | **66/66**，失配 0 |
| 锁 PGM ````html` 摘录块全文包含于现文 | **66/66**。PGM-047 摘录含「查看考勤」、不含「考勤打卡」。PGM-043 / PGM-059 摘录含获批联系人句且包含成立 |

内存负例（能失败才算检查有效）：删 `</html>` → 闭合标签计数 1→0；追加 `href="#no-such-id"` → missing=`['no-such-id']`。

获批三处现文（本次逐字读）：

L656（声明保持）：

> 临时授权可设有效期或单次使用；权限的授予、收回、到期、单次消耗、失效全部留痕。

L680 末句（页面例外切断导出/打印）：

> ……纸质凭条与页面、导出同属强制脱敏面。商户档案页面中的第一联系人和第二联系人手机号，按“客人与员工的个人信息保护”条中明确的查看权限例外执行；该页面查看例外不授予未脱敏导出或打印权限，也不免除敏感页的查看留痕及其他页面保护要求。

L871（授权查看明文，不扩大客人/员工档案）：

> 员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关。商户档案中的第一联系人和第二联系人手机号除外：经单独授权且查看权限有效的人员，以及持授权 U 盘密钥的最高权限本人，可以在其获准访问的商户档案列表和档案操作结果中查看完整号码；其他人员仍只显示脱敏号码。平台管理员身份本身不自动取得该查看权限。该例外仅适用于商户档案联系人，不扩大到客人或员工档案的手机号，也不免除项目范围检查和查看留痕。

交叉只读（不重开 M001）：

- BEQR「终端十一端」十一类中文名与本文件 L739 **逐字相同**。第二句措辞不同（BEQR「适用统一的终端开关和访问留痕规则」/ PGM「适用与其他终端相同的后台开关、权限、数据范围、访问拒绝留痕和兜底关闭规则」），无相反指令。
- BSM L930 / L1865 为「查看考勤」；BSM L1445 打卡从领取工作手机起算。本文件 L741 与前两处同向，打卡不在本页私人手机低风险清单。

旧 43495 字节前缀（本文件追加前）SHA `73af16a82408c9a07b66dac2fa8c92d56ae0c0b30976e83d97ac854de865aba9`，本席追加前实核长度与哈希均相等，全部保留。

### 1.2 全局门禁（本次实跑，不折成 HTML 对错）

```
npm run verify:html-ledger-sync
[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@88c7b465。
  已改动的 HTML：
    - docs/platform-governance-map-for-operations-v0.1.html
  必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md
真实退出码 1
```

```
npm run verify:entry-anchors
[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。
真实退出码 0
```

```
node --test tools/verify-entry-anchors.test.mjs
PASS：历史 raw 放行，现行 findings、ledger、模块收口和普通文档的失效引用仍拒绝。
tests 1 pass 1 fail 0
真实退出码 0
```

**html-ledger-sync 归属（systematic-debugging：读错误 → 复现 → 追文件，B5 不修）：**

| 层 | 路径 | 计划模块 |
|---|---|---|
| 触发脏文件 | 工作区本 HTML 相对 merge-base 有口径改动（V0.13 + L680/L871 获批句） | **M002**（文件在本模块清单） |
| 门禁要求的同步目标 | `docs/prd-audit/business-decision-ledger.md` | **M426**（`plan/modules.md` L6916 段，FILES 仅该台账） |
| 门禁实现 | `tools/verify-html-ledger-sync.mjs` | **M008**（L98 段） |

根因：脚本在「三份 HTML 工作区脏」时按 merge-base↔工作区比；本 HTML 脏且决策台账未在同一范围出现 → 设计如此的红。阶段 B 禁止提交/推送，本步不能也不应改 M426。这不是 PGM 摘录失配，不是 CSV/BSM 镜像问题（entry-anchors 本次绿），不是 HTML 现文自相矛盾。

最小处置（记录，本步不执行）：日后提交本 HTML 获批改动的同一批必须改 M426 台账。**不把「门禁红」写成必须改回 L680/L871 的 HTML 缺陷。不冒称全局绿 / predeploy 绿。**

M002-004 对象是 entry-anchors 历史 raw 排除，与 html-ledger-sync 不是同一命令。004 只作输入。本次 entry-anchors 绿 **不**把 004 写成新证收口。

未跑后端 / Maven / psql（提示词：本机只有 node；用户本轮不强制后端测试）。

### 1.3 明确复用（同字节全文阅读事实）

因工作区 HTML SHA256 **与本席 B1 V0.10 所核逐字相同**（`5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f`），本席**声明复用**当时对 979 行的全文阅读事实。本会话另外重新打开：L1–2、L217–979（含全部 h2/h3、获批句、十一端、已拍板 17 条、页脚）。L3–216 为内联 CSS：本次机械核无脚本/外链字体/@import/outline:none；细则不另造阅读。

**复用阅读 ≠ 继承 PASS。** 84 项均按现锁 V0.14 **重新判定**。旧 B5 V0.01/V0.02 针对不同 HTML 字节与旧锁，结论作废于本票。

## 2. 第一性原理线（grilling 自问自答）

codebase-design 删除测试：删掉本文件，页脚「三份最高业务解释权 HTML」少一份，铁律 4 与 PGM-001 底座声明无法从纳入范围复现。必须存在。接口小（18 节 + 66 锚点），实现深（红线、权限公式、终端开关、破窗、获批联系人例外）。本模块不应再加一层「摘要必须复述全部但书」的锁外接口。

文件自证用途（L222–L224、L974）：给业务人员理解平台治理层；**不讲技术实现**。每条 PGM 当业务规则说明核对，不把未实现后端写成 HTML 缺陷。M009 单次授权代码未修，L656 已写「单次使用 / 单次消耗」，不以代码未修否定需求句。

三问：

1. **为何必须存在**：L974 解释权声明把本文件与另外两份并列。删则治理层规则（归属、资格、责任、红线、开关、破窗、脱敏、留痕、获批的商户联系人查看例外）在纳入范围失去正本。
2. **声称是否实际执行**：文档类用 2.3 类型 1/4。本次已跑：979 行、锚点闭合、UTF-8、66 条锁来源/标题/摘录全含、无脚本可跑、负例能失败。
3. **未验证前提**：本文件不声称生产后端已落地。注释「已上线 / 已落地」非展示文字。单次授权**代码**属 M009。浏览器像素、读屏器、密钥托管、真实 USB、数据库均未测。企微永久保存的法律合规指向合规文件 §4.4，本席未将该合规文件当冻结判据，只记未验证。

按章节：

| 行段 | 为何存在 | 执行证据 | 未验证前提 |
|---|---|---|---|
| L1–216 文档类型与样式 | 业务人员可读的说明页载体 | `lang="zh-CN"`、viewport、无脚本；解析栈空 | 不验证像素级视觉 |
| L217–252 顶栏与侧栏 | 理解顺序入口 | 18 个 nav href 全部命中 id | 「理解顺序」≠ 另一套业务闸 |
| L255–305 `#overview` | 底座 + 五条全局原则 | 锁 PGM-001 摘录 = 现行 L256 起 | 不验证全仓 UI 已零英文；概述不强制复述 L871 |
| L307–336 `#flow` | 对象→合同→人员→设备→留痕 | 五步 DOM 完整 | 不验证运行时拦截顺序已编码 |
| L338–409 `#modules` | 13 张 `system-card` 对 13 个业务锚点 | 卡片 href 全部命中 | 卡片排列序 ≠ 侧栏/正文序，不升格为业务闸 |
| L411–460 `#project` | 当前单项目、多对多主体 | 与 BEQR 四类主体/城市项目公司/职级同口径 | 「长沙项目公司」与 BSM 账户注销同一专名 |
| L462–485 `#space` | 位置影响合同/低消/容量/库存 | 容量句与 BSM/BEQR 实时在玩人数一致 | 平面图导入是规则说明，不验证上传服务 |
| L487–515 `#merchant` | 四类主体绑定与招商前链 | 合作六类与 BEQR 合作模式一致；L494 第一/第二联系人 | 不验证招商 CRM 已建 |
| L517–569 `#contract` | 卡住+红线+审批时限 | 七类红线与 BSM 基础管理红线同构 | PGM 对⑤多一句举例，无相反指令 |
| L571–608 `#staff` | 人/私机/工作机对应 | 与 BSM 私人手机标识句同构 | 不验证 MDM |
| L610–640 `#device` | 台账与现场职责 | 入场三道设备与 BEQR 年龄限制同构 | 智能场景「独立立项前不定义」是明确非目标 |
| L642–709 `#permission` | 最小功能点 + 30 档 + 导出脱敏 + 页面例外 | L656 单次消耗仍在；L680 指向 L871 且切断导出/打印 | 不验证授权服务；单次消耗代码属 M009 |
| L711–746 `#terminal-switch` | 十一端 + 默认关 + 钱规则不得灰度 | L739 十一类 = BEQR 名；L741「查看考勤」 | 卡片/七条用 App 别名，专项名单为准 |
| L748–778 `#merchant-side` | 授权范围内全开放 | 三类商户表与「看不到平台全局」一致 | 不验证门户代码 |
| L780–802 `#open-platform` | 先自有后第三方 | 明文「当前还没有真实第三方供应商接口资料」 | 该前提是业务状态声明 |
| L804–853 `#building-ops` | 机电/能耗/巡检与工作设备分开 | BEQR 楼宇机电设备状态、巡检四类、能源费同口径 | BSM 另有「核实默认一周」，本文件未复述 |
| L855–889 `#recovery-privacy` | 破窗 + 加密脱敏 + 会员/人脸 + 联系人例外 | L870 加密存放；L871 日常脱敏 + 获批例外；L872 客人查询仍只脱敏 | 不验证密钥托管 |
| L891–932 `#audit` | 重要动作可追 | 与 BEQR 主管复核强制项同族；L871/L680 要求查看留痕 | 不验证审计库 |
| L934–947 `#relations` | 治理如何卡住各域 | 七行连接表完整 | 无额外业务发明 |
| L949–974 `#decisions` 与页脚 | 已确认清单 + 解释权 | 17 条均为 confirmed；页脚三份平级、后确认覆盖先确认 | 标题含「待排期」而当前 0 条未确认，见对抗线 |
| L975–979 闭合 | 结构完整 | 解析栈空 | — |

## 3. 顺从式线（84 项全部核对，不抽样，不继承 B1 结论）

先假定本文件作为业务说明的意图正确，再逐条核映射 ID。无测试/无迁移/无后端的 GEN 记「文档类适用/不适用」并给命令证据，不把「未写测试」折成缺陷。判定顺序：冲突 → 细化 → 技术性 → 添加。

### 3.1 GEN-001 … GEN-018

| ID | 核对 | 证据类型 | 本席重判 |
|---|---|---|---|
| GEN-001 必要性与最小性 | 本文件承载 66 个 PGM 锚点与解释权声明；CSS 服务可读；侧栏/卡片/正文是同一组锚点的入口。删除测试见 §2。获批两句是已有脱敏/授权规则的下位例外，不是无需求抽象 | 1+4 | 满足 |
| GEN-002 可执行正确性 | 无构建入口、无脚本。声明的锚点入口全部可解析；负例能失败 | 4：href 缺失 0；parse error 0 | 满足（文档豁免可执行产物）。不把后端未实现写成不正确 |
| GEN-003 测试有效性 | 模块文件列表无测试 | runbook 文档豁免。本次 `verify-entry-anchors.test.mjs` exit 0 是 **M415/M008** 工具绿，不是本 HTML 自带测试 | 不适用强制测试，不记缺口，不冒称本模块测过 |
| GEN-004 信任边界 | 无用户/网络/文件输入处理。正文规定客人查询须先鉴权（L276、L872）；商户联系人完整号须单独授权且权限有效（L871） | 1 | 规则说明满足；实现不在本模块 |
| GEN-005 授权与最小权限 | L643–L709、L528 七类红线、L660 对照「平台管理员≠最高权限」；L871 平台管理员不自动取得联系人完整号；USB 最高本人是另列主体 | 1 | 说明与最小权限相容 |
| GEN-006 数据完整性 | L302 金额到分+税务例外+尾差主体；L528⑥ 守恒；L882 积分预算恒等式 | 1 | 规则自洽。无库可跑 |
| GEN-007 并发与幂等 | L293 先成功为准 + 重复提交只认一次，例外仅红线⑤密钥逐笔；L656 单次消耗 | 1 | HTML 已写单次消耗，代码属 M009 |
| GEN-008 接口与模式一致性 | 本模块无 API/DTO。对内：L739 十一类名 = BEQR 终端十一端。L741「查看考勤」= BSM L930/L1865，打卡在 BSM L1445 | 1 | 满足。卡片 App 别名不另造第十二端 |
| GEN-009 错误处理 | 无运行时。规则侧：开关拉取失败兜底关闭但留痕仍写（L738）；异常读数不得直接出账（L828） | 1 | 说明满足 |
| GEN-010 安全与敏感信息 | 文件无密钥/口令。L860–L872 破窗与加密脱敏；L680 个人信息七项封闭；完整号仅列表/档案操作结果、不授导出打印 | 1+4 | 满足。无秘密泄漏 |
| GEN-011 可观测与审计 | L891–L929 留痕/复核/挂起；L733 拒绝访问留痕；L681 敏感页打开留痕；L871 不免除查看留痕 | 1 | 说明满足 |
| GEN-012 资源与性能 | 单页 CSS，无循环查询。`max-height`/`overflow-y` 仅侧栏 | 4 | 满足 |
| GEN-013 构建配置与依赖 | 无 package/CI。纯静态 HTML。全局 html-ledger-sync 红的实现与台账不在本模块 FILES | 4 | 本文件可按路径读取。全局门禁见 §1.2，不把 M008/M426 缺口记成本 HTML 缺依赖 |
| GEN-014 数据库迁移 | 无 SQL | — | 不适用 |
| GEN-015 界面可用性 | `lang="zh-CN"`、viewport、原生 `<a>`、`@media (max-width: 980px)` 单列。展示文案中文。CSS 类名/字体栈属内部标识 | 1+4 | 基本可用。全中文规则约束的是平台终端，见 PGM-001/066。未跑浏览器/读屏器 |
| GEN-016 文档可验证性 | 版本注释唯一 `V0.13`；无「更新日期」双值；内部链接全通；锁来源/标题/摘录 66/66 全文包含。工作区相对 HEAD 的 3 处 diff 即获批现文，不是第二套日期 | 4 | 本文件可验证。不把「尚未与 M426 同 PR 提交」写成必须改 HTML |
| GEN-017 业务来源分类 | 本席本轮 findings 空。获批两句按冲突→细化：能指向上位脱敏/授权条目，是细化不是添加 | 1 | 满足 |
| GEN-018 第一性原理证据 | 本报告依据仅为 HTML 原文、结构命令输出、相关只读两 HTML 原文、本会话门禁输出。不引用旧票/注释/席位意见作依据 | 1+4 | 满足 |

### 3.2 PGM-001 … PGM-066

锁来源行均已与现行文件对齐；摘录 66/66 包含。下表「交叉」只读另外两份相关条款作上下文，不重开 M001。PGM-043 / PGM-047 / PGM-059 锁定义含铁律 4 批准依据。

| ID | 来源行 | 锚点标题 | 本席重判 |
|---|---|---|---|
| PGM-001 | L256 | 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 满足。摘录含归属/资格/责任、可溯源（含安检例外）、全中文、金额与税务例外、同时操作、原则仲裁。L276「打开后界面仍只显示脱敏信息」对象是**单据上的客人 / 客人查询通道**，不是商户联系人 |
| PGM-002 | L308 | 治理顺序：一件业务能不能做，要先经过这些判断 | 满足。五步完整。与 BSM「一笔业务怎么走」不同粒度，不互斥 |
| PGM-003 | L339 | 模块入口：平台治理层包含哪些管理模块 | 满足。13 卡片 + 注释记录补卡历史。卡片集合 = 业务 section 集合（#overview/#flow/#modules/#relations/#decisions 无卡，符合「管理模块」而非「全文目录」） |
| PGM-004 | L342 | 综合体和集团 | 满足。与 L412 正文同主题 |
| PGM-005 | L347 | 空间和点位 | 满足。与 L463 同主题 |
| PGM-006 | L352 | 商户和主体 | 满足。卡片写联系人，L494 落到第一/第二联系人 |
| PGM-007 | L357 | 合同和准入 | 满足。「卡流程」与 L518 卡住规则一致 |
| PGM-008 | L362 | 员工和手机 | 满足。与 L572 同主题 |
| PGM-009 | L367 | 权限和职级 | 满足。与 L643 同主题 |
| PGM-010 | L372 | 终端开关 | 满足。卡片列举 11 端，与 L739 专项名单同一闭集（命名见对抗） |
| PGM-011 | L378 | 设备管理 | 满足。标签「人员」是卡片分类，正文 L611 定义设备范围，不另造业务实体 |
| PGM-012 | L383 | 商户侧 | 满足。与 L749「授权范围内全开放、不看全局」一致 |
| PGM-013 | L388 | 开放接口 | 满足。与 L781 先自有后第三方一致 |
| PGM-014 | L393 | 留痕复核 | 满足。与 L892 一致 |
| PGM-015 | L399 | 楼宇运营 | 满足。与 L806 一致 |
| PGM-016 | L404 | 应急与隐私 | 满足。卡片短名；h2 为「应急恢复和客人隐私…」，同一 `id="recovery-privacy"` |
| PGM-017 | L412 | 综合体和集团：当前一个项目，未来能看集团 | 满足。与 BSM「当前先管理一个综合体」一致 |
| PGM-018 | L415 | 物理项目与运营公司：多对多协作，不是固定隶属层级 | 满足。与 BEQR 同构。L420「长沙项目公司」通用卡兑付：BSM 账户注销亦写同一专名 |
| PGM-019 | L424 | 各主体具体负责什么 | 满足。六行主体表与 BEQR 运营公司职责/收入归属同族。80%/20% 标明业务参数+上下限。弱电工程师明文隶属娱乐平台公司 |
| PGM-020 | L440 | 主要功能 | 满足。集团下钻/KPI：职位模板只决定层，数据范围无后台设置。与 BSM「数据隔离仍是综合体级」相容 |
| PGM-021 | L452 | 业务意义 | 满足。三条，无额外闸门 |
| PGM-022 | L463 | 空间和点位：所有业务都要知道“发生在哪” | 满足。位置影响合同/低消/容量/库存/责任 |
| PGM-023 | L467 | 主要功能 | 满足。平面图导入为高危发布、二次确认。规则说明，不验证导入器 |
| PGM-024 | L476 | 影响业务 | 满足。「游乐容量只按单个项目的实时在玩人数」与 BSM/BEQR 容量口径一致 |
| PGM-025 | L488 | 商户和主体：谁在这里经营，经营什么 | 满足。不只录名 |
| PGM-026 | L492 | 主要功能 | 满足。合作六类 = BEQR。四类主体唯一绑定。L494 建立第一/第二联系人，是 L871 例外的对象来源。线索来源四类为封闭清单 |
| PGM-027 | L507 | 业务边界 | 满足。L510 商户可查看**本商户客人**全部对接信息，交叉 L680「商户查询与导出属于本商户的客户信息不脱敏」。L871「不扩大到客人档案」约束的是**新的联系人查看例外**，不收回本商户客户这条既有来源判定 |
| PGM-028 | L518 | 合同和准入：违反规则先卡住，再按层级放行 | 满足。卡住是默认 |
| PGM-029 | L522 | 主要功能 | 满足。权限不足不设放行（换有权人）与超范围可申请放行是两件事。七类红线；①②③ 不可开、④⑤⑥⑦ 可逐笔。通用时限 3 日/涉钱 1 日 |
| PGM-030 | L544 | 业务意义 | 满足。红线不因审批放行，与 L528 并列 |
| PGM-031 | L555 | 外部经销商主体、合同价格与资金治理 | 满足。职责分离、未拆封可复售、税率只存快照 |
| PGM-032 | L572 | 员工和手机：人、私人手机、工作手机要能对应起来 | 满足。三条路径：自有注册 / 商户代注册 / 自营视同自有 |
| PGM-033 | L576 | 主要功能 | 满足。设备标识不读硬件码；换机先解绑 |
| PGM-034 | L586 | 工作手机和企业微信 | 满足。永久保存、最小授权看原文。L957 已拍板复述同一口径 |
| PGM-035 | L599 | 提成发放和人员服务治理 | 满足。月结代扣个税、禁止现金现抽。L602 营销打卡不混入考勤，与 L741「查看考勤」不是同一动作 |
| PGM-036 | L611 | 设备管理：谁领了什么设备，设备能不能用 | 满足。含入场三道设备。工作设备状态闭集在 BEQR |
| PGM-037 | L615 | 主要功能 | 满足。台账/领还/白名单/管控 |
| PGM-038 | L624 | 和现场业务的关系 | 满足。平板只点酒；一体机可辅助售票并按点位限票种 |
| PGM-039 | L636 | 智能设备场景联动范围 | 满足。明确不建设计时/钟点房；立项前不定义动作。非目标而非缺口 |
| PGM-040 | L643 | 权限和职级：每一个单一功能都可以单独授权 | 满足。L871「单独授权」落在本条颗粒度上 |
| PGM-041 | L647 | 权限规则 | 满足。L656 临时授权可设有效期或单次使用，单次消耗留痕。高危五类授出前审批；未归类先按高危。查看内部结算价等不因平台管理员自动获得，与 L871 同构 |
| PGM-042 | L664 | 职级规则 | 满足。M1–M10×上中下 = BEQR 共 30 档。职级不替代功能点 |
| PGM-043 | L673 | 外包人员与审计人员（2026-06-29 新增） | 满足。内审/外审只读。导出脱敏七项封闭。**末句（获批）**：商户档案页面第一/第二联系人手机号按 L871 查看例外执行；该页面查看例外不授予未脱敏导出或打印。锁摘录已含此句且在现文中。L681 敏感页禁选中/复制/另存/打印与「可看完整号」并列：看 ≠ 拷走 |
| PGM-044 | L688 | 桌台、挂单与经销业务权限 | 满足。隐私桌台/挂单/经销分权。结账选最低实付券、转台提成规则与 BSM 酒吧段同族 |
| PGM-045 | L712 | 终端开关：哪些端能打开，后台说了算 | 满足。关则不可用，开仍看权限/范围/设备 |
| PGM-046 | L716 | 能控制哪些端 | 满足。7 条是 11 端的归并写法，闭集与 L739/BEQR 相同 |
| PGM-047 | L728 | 怎么控制 | 满足。优先级链、默认缺行关闭、钱规则不得灰度、营业时段不得中断变更。十一类中文名与 BEQR 逐字相同。现文 L741「查看考勤」；锁摘录本次已 LOCK-AMEND 同步，包含成立，**不再滞后** |
| PGM-048 | L749 | 商户侧：在授权业务范围内全开放 | 满足。酒吧/游乐/餐饮零售三表。游乐「场次安排」= 开放批次，容量仍不按场次另设，与 PGM-024 一起读 |
| PGM-049 | L781 | 开放接口：先做好自己的开放能力，再接第三方资料 | 满足。明文当前无真实第三方资料，不阻塞自研 |
| PGM-050 | L785 | 平台开放给第三方 | 满足。身份核验、门店 ID 映射缺失挂差异 |
| PGM-051 | L794 | 第三方接入平台 | 满足。后续按资料接。与 PGM-049 时间盒一致 |
| PGM-052 | L806 | 楼宇运营：管好建筑本身的设备、能耗和巡检 | 满足。与工作设备/游乐设备分开 |
| PGM-053 | L810 | 楼宇机电设备 | 满足。状态六值 = BEQR。四组台账是运营分组，消防归入给排水消防组 |
| PGM-054 | L821 | 能耗计量 | 满足。「支持人工抄表并预留远程」与「远程自动读为主、异常转人工」是摘要/专项。公区不形成商户公区能源费应收。BSM 另有核实出厂默认一周，本条未复述，不是相反指令 |
| PGM-055 | L832 | 楼宇巡检 | 满足。四类 = BEQR。能耗抄表不在巡检重复抄 |
| PGM-056 | L844 | 和谁有关 | 满足。物业/财务/供应链/留痕四连 |
| PGM-057 | L857 | 应急恢复和客人隐私：最坏情况有后路，个人信息看不到明文 | 满足。章节总题。L871 是同章专项例外。不把概述标题未复述例外写成新判据或冲突 |
| PGM-058 | L860 | 紧急恢复（破窗机制） | 满足。旧钥先废、当面验证、不预制备用。BSM 回指本条 |
| PGM-059 | L868 | 客人与员工的个人信息保护 | 满足。L870 一律加密存放（存放 ≠ 界面）。L871 日常脱敏 + 获批例外（单独授权且查看权限有效，或 USB 最高本人，在获准访问的商户档案**列表和档案操作结果**看完整号）。锁摘录已同步且在现文中 |
| PGM-060 | L879 | 会员身份、营销与生物信息治理 | 满足。后台不得直接导入会员；积分预算恒等；禁止个人定向。人脸最短留存到期删除。与配置类主数据软删除对象不同 |
| PGM-061 | L892 | 留痕和复核：重要动作不能只靠口头说明 | 满足。谁发起/销售/服务/处理/审批/结果 |
| PGM-062 | L896 | 必须留痕的动作 | 满足。10 项，含「权限变更、临时授权和敏感权限使用」。联系人完整号查看由 L681 敏感页打开留痕 + L871「不免除查看留痕」覆盖 |
| PGM-063 | L911 | 常见复核 | 满足。免单退款调账、高价值报损、日结后补录、游乐恢复 |
| PGM-064 | L923 | 正式挂起订单审计与结案 | 满足。收银员挂起不设额度；未结清阻交班日结。赔偿规则法务审查前不得自动扣薪 |
| PGM-065 | L935 | 连接关系：平台治理层怎么连接所有业务 | 满足。票务/酒吧/游乐/供应链/财务/第三方/楼宇 七连，均有「为什么必须连」 |
| PGM-066 | L951 | 已拍板规则和待排期问题 | 满足。17 条全部 `confirmed`，0 条待排期。可溯源已拍板（L964）未复述联系人例外，同「概述不强制复述专项」。标题含「待排期」而当前空集不是原文自否 |

84/84 已核对。无锁外主张。不为每个概述未复述专项例外新增判据。

## 4. 对抗式线（grilling 自问自答，不派第三席、不问用户）

只构造**正常首次运行就会碰到**的反例。要两三件不巧才成立的组合不新增要求。

**Q1 越权：平台管理员能否看内部结算价 / 清数据 / 开红线①②③ / 看商户联系人完整号？**  
A：L660：高敏能力不因管理员身份自动获得。L528：①②③连密钥持有人也无开放入口；④⑤⑥⑦仅逐笔。L871：平台管理员身份本身不自动取得联系人完整号。**不构成缺陷**。

**Q2 重复提交 / 支付回调重入 / 单次授权重放？**  
A：L293 与红线⑤同一口径，只认一次。L656 明文「单次使用」「单次消耗」并留痕。代码未修属 M009，不否定本句。**不构成 HTML 缺陷**。

**Q3 并发：转赠领取 vs 自己核销 vs 再转他人？**  
A：L293 以先成功为准，其余失效并提示。**不构成缺陷**。

**Q4 金额：0.005 元、涉税、跨域通票负尾差？**  
A：非税四舍五入到分；涉税走当时有效税法；非税尾差由实际结算责任主体承担，跨域组合通票由娱乐平台公司承担负尾差并退回。与 BSM「实际承担结算责任的主体」一起读为细化。**不升格冲突**。

**Q5 配置：开关缺行、灰度切分账、营业中改核销？**  
A：L736 缺行默认关；L737 钱规则不得灰度；L738 营业必需能力不得在营业时段做中断变更。**不构成缺陷**。

**Q6 删除：删商户档案 vs 删人脸 vs 客人注销？**  
A：配置类软删除在 BSM；人脸 L885 到期删除；注销在 BSM。对象不同，专项规则优先。**不构成缺陷**。

**Q7 版本：会不会再出现两个「更新日期」？**  
A：现行 L2 仅 `文件版本：V0.13`；`更新日期` 于本文件命中 0。历史 M002-001 作为输入重核：现文无此冲突。**现文无此冲突**。

**Q8 导航：侧栏/卡片/正文顺序不一致是否一进页就错？**  
A：侧栏顺序 = 正文 section 顺序。卡片把「权限/终端」放在「设备」前、把「留痕」放在「楼宇/应急」前，是补卡历史造成的展示序。三组 href 集合一致、无断链。**不为展示序另造必须改 HTML 的条款**。

**Q9 终端名单 App vs 手机应用是否第十一+第十二端？**  
A：专项名单 L739 与 BEQR 十一类中文名逐字相同。L372/L716 用 App/Web 作同一闭集的别名。历史 M002-002 作为输入重核：现文仍无相反命题，属细化。**不构成缺陷**。

**Q10 标题「待排期」但 0 条未确认？**  
A：L951 是栏目名（已拍板 **和** 待排期）。当前 17 条全确认，表示待排期为空集。**不构成缺陷**。

**Q11 本说明页自己含 App/Web/KPI/P0/AI/HRBP，是否违反本页全中文原则？**  
A：L287 约束的是**平台全部终端展示给用户的文字**。本文件是需求说明，不是运行时。BEQR 终端名单自身含「Web 前端」。**不把说明文里的产品别名写成 HTML 必须先改的冲突**。

**Q12 红线⑤ PGM 比 BSM 多一句括号，是否三份冲突、要改 HTML？**  
A：PGM 括号是「重复提交」的外延举例，未缩小或扩大「只算一次」。**不申请改 HTML**。

**Q13 获批例外是否与 L276 / L857 / L872 新矛盾？**  
A：L276、L872 对象是**客人查询通道**。L871 对象是**商户档案第一/第二联系人**，并写「不扩大到客人或员工档案」。L857 h2 是章节概述；专项在同章 L871。L680 把该例外限制在页面查看，切断导出/打印，并保留敏感页留痕——与 L681 同向：可看完整号 ≠ 可拷走。USB 最高未脱敏导出仍走 L680 原句跨商户批量口子，不是页面查看例外授予的。L510/L680 本商户客户不脱敏是另一对象。**现文无新的相反指令。不申请改 HTML。**

**Q14 列表/档案操作结果是否等于导出？**  
A：L871 写「商户档案列表和档案操作结果」；L680 写「该页面查看例外不授予未脱敏导出或打印权限」。档案操作结果 = 档案读写后的界面/返回展示，不是导出口。**不构成缺陷**。

**Q15 锁 PGM-047 是否仍为「考勤打卡」？**  
A：本次实核锁摘录含「查看考勤」、不含「考勤打卡」；现文 L741 与 BSM L930/L1865 同向。旧锁滞后 **已由 LOCK-AMEND 消除**，本轮 66/66。**不构成缺陷，不新开 LOCK-AMEND。**

**Q16 html-ledger-sync exit1 是否证明本 HTML 写错、必须改回获批句？**  
A：命令输出只说「改了业务 HTML 但决策台账未同步」。脏的是获批现文。同步目标属 M426，门禁属 M008。首次跑该门禁就会红——因为工作区故意保留未提交获批改动，且阶段 B 不许提交。把 HTML 改回 HEAD 的 V0.12/无例外句，会与操作员已批准的 680/871 相反。**不构成本模块 HTML 缺陷。不冒称全局绿。**

**Q17 本文件旧 B5 PASS 能否直接继承？**  
A：不能。模块当前 `IN_PROGRESS`；旧 B5 针对不同锁/不同 HTML 字节；B2 空队列跳 B3/B4 后仍须整模块重审。本票按当前字节独立重判。

以上反例均在首次正常阅读/对照就会出现；无一进入本席临时 finding。

## 5. 979 行覆盖表（全文件，不抽样）

| 行 | 内容 | 覆盖的判据 | 本次阅读 |
|---|---|---|---|
| 1–2 | doctype；`<!-- 文件版本：V0.13 -->` | GEN-016 | 本会话新读 |
| 3–216 | `html lang="zh-CN"`、viewport、全页 CSS、响应式 | GEN-012/015 | 机械核 + 同字节复用 V0.10 全文阅读 |
| 217–226 | 顶栏标题/副标题/一句话理解 | PGM-001 定位 | 本会话新读 |
| 228–252 | 侧栏 18 链 | GEN-002/016；PGM-002–066 入口 | 本会话新读 |
| 255–305 | `#overview` 全文 | PGM-001 | 本会话新读 |
| 307–336 | `#flow` 五步 | PGM-002 | 本会话新读 |
| 338–409 | `#modules` 13 卡 | PGM-003–016 | 本会话新读 |
| 411–460 | `#project` | PGM-017–021 | 本会话新读 |
| 462–485 | `#space` | PGM-022–024 | 本会话新读 |
| 487–515 | `#merchant` | PGM-025–027 | 本会话新读 |
| 517–569 | `#contract` | PGM-028–031 | 本会话新读 |
| 571–608 | `#staff` | PGM-032–035 | 本会话新读 |
| 610–640 | `#device` | PGM-036–039 | 本会话新读 |
| 642–709 | `#permission`（含 L656 单次、L680 获批末句） | PGM-040–044 | 本会话新读 |
| 711–746 | `#terminal-switch`（含 L739 十一端、L741 查看考勤） | PGM-045–047 | 本会话新读 |
| 748–778 | `#merchant-side` | PGM-048 | 本会话新读 |
| 780–802 | `#open-platform` | PGM-049–051 | 本会话新读 |
| 804–853 | `#building-ops` | PGM-052–056 | 本会话新读 |
| 855–889 | `#recovery-privacy`（含 L871 获批例外） | PGM-057–060 | 本会话新读 |
| 891–932 | `#audit` | PGM-061–064 | 本会话新读 |
| 934–947 | `#relations` | PGM-065 | 本会话新读 |
| 949–972 | `#decisions` 17 条已拍板 | PGM-066 | 本会话新读 |
| 974 | 页脚解释权 | GEN-016；三份平级 | 本会话新读 |
| 975–979 | 关闭标签 | 结构完整 | 本会话新读 |

空行 27，非空 952。1–979 均已覆盖。完整文件 SHA256=`5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f`。

## 6. 术语（ubiquitous-language 收窄：不写文件）

本环境存在 `/home/james/.qwen/skills/ubiquitous-language/SKILL.md`，本体要求写 `UBIQUITOUS_LANGUAGE.md`，本节点禁止该产物。对照本页与 BEQR/BSM 已有专名：

| 词 | 本文件用法 | 核对 |
|---|---|---|
| 娱乐平台公司 | L429 等；酒吧域主管、跨域通票酒吧部分 | 未见废止别名「娱乐运营平台」 |
| 城市项目公司 / 长沙项目公司 | L420 资产职责 + 通用卡兑付 | 长沙专名与 BSM 账户注销同一发卡主体 |
| 商户 / 独立酒吧商户 | L429、L749 | 酒吧是独立商户，不是业态主体 |
| 顶层平台 | L417 规则与跨域审批，不自动取得收入 | 与「平台不是经营主体」一致 |
| 四类主体 | L497 | 与 BEQR 一致 |
| 第一/第二联系人 | L494、L680、L871 | 商户档案联系人，不是客人、不是员工档案 |

## 7. ponytail-review（只举证，不修）

范围：过工程复杂度。不把注释当必须删除的业务冲突。

- 本文件 HTML 注释 **26** 条。除 L2 版本注释外，其余为修订考古（含已作废的 2026-06-26「分组并行会签」口径说明，现文已按 2026-08-04 统一审批模型）。用户不可见。`net: 约可删注释行，不改变任何业务句`
- CSS 单文件内联，无 JS、无外链字体。`Lean already. Ship.`（就本说明页的运行时而言）
- 卡片与正文顺序不一不是多余模块，是补卡留下的展示序
- 获批两句是已有规则的下位例外，不是新模块

不把上述写成 findings。删除注释属于改三份 HTML，无授权。

## 8. 本席 findings（新问题全集及最小处置）

本轮临时 finding：**空集**。无冲突、无工程缺陷、无添加、无新 HTML 自身问题需铁律 4 上报。不预设 PASS：空集是 84 项 + 979 行 + 三线跑完后的结果。

历史 M002-001—004 只作输入，本席对**现文**独立重判（不继承旧 PASS，也不预设 FAIL）：

| 输入 ID | 现文重核 | 本席本轮是否新开 GR | 冻结依据 | 最小处置 |
|---|---|---|---|---|
| M002-001 日期双值 | L2 仅 V0.13；页脚 L974 无更新日期；本文件「更新日期」命中 0。现文无冲突 | 否 | GEN-016；现文无双值 | 保持既有修复，不再改 HTML；队列保持否 |
| M002-002 十一端别名 | L739 闭集与 BEQR 十一类名逐字同；L718–724 / L373 为同一闭集说明性别名，无「多一个端/少一个端」相反指令。分类仍是细化，现文无工程缺陷 | 否 | GEN-008、PGM-010/046/047 | 只登记，队列否 |
| M002-003 HTML/CSS 目录 | 页面壳无业务发明。结构命令：解析栈空、18 id 与 31 href 闭合、无脚本。现文无已证工程缺陷 | 否 | GEN-013/015/017；无业务发明 | 只登记，队列否 |
| M002-004 历史 raw 排除 | 对象是 `tools/verify-entry-anchors.mjs`（M008）及历史 raw 路径，不在本模块 FILES。本次 `verify:entry-anchors` 与其测试均为 exit 0。不把 CSV/工具门禁塞进 M002，也不因本次绿就把 004 写成「本席新证 PASS 收口」 | 否 | 历史技术性；队列否 | 保持台账 |

未把下列观察写成缺陷（判定顺序冲突→细化→技术性→添加，在冲突步停不住）：

| 观察 | 分类停在 | 冻结依据 | 为何不是 finding | 最小处置 |
|---|---|---|---|---|
| 红线⑤ PGM 比 BSM 多举例括号 | 冲突步停不住 → 细化 | PGM-029 / GEN-008 | 无相反指令 | 不改 HTML |
| 金额跨域通票尾差点名娱乐平台公司 | 细化 | PGM-001 L302 | 细化 BSM「实际结算责任主体」 | 不改 |
| 能耗核实「一周」只在 BSM | 细化（本文件沉默） | PGM-054 | 未写相反时限 | 不要求复述 |
| 全中文/仲裁顺序只在本图 | 细化 | PGM-001 | 本图全局原则；BSM 沉默 ≠ 冲突 | 不改 |
| 卡片顺序 ≠ 侧栏/正文 | 技术性展示 | GEN-002 | 非业务闸门；链接全部有效 | 不改 |
| L276/L857/L872 未复述联系人例外 | 冲突步停不住 | PGM-001/057/059 | 对象是客人查询或章节概述；L871 已限定不扩大 | 不新开判据 |
| L680「页面」vs L871「列表和档案操作结果」 | 细化 | PGM-043/059 | 同属商户档案查看面；L680 切断导出/打印 | 不改 |
| L656 单次授权代码未修 | 非本文件 | PGM-041 GEN-007 | 需求句已写单次使用/单次消耗；实现属 M009 | 不把实现转 HTML |
| 锁 PGM-047 已同步「查看考勤」 | — | PGM-047 铁律4依据 | 本轮 66/66 包含 | 不 LOCK-AMEND |
| html-ledger-sync exit1 | 技术性过程事实，**同步目标/工具不在本模块清单** | GEN-013 适用于门禁所在模块 | 触发脏文件是获批现文；缺的是 M426 台账；闸在 M008。阶段 B 不许提交 | 记录归属；不改 HTML；不冒称全局绿 |
| entry-anchors 本次绿 | — | — | 不把绿折成 004 新证收口 | 只记事实 |
| ledger 表行仍写历史「当前B5-r2双PASS」 | 非本模块 FILES | — | 顶部当前段已写 IN_PROGRESS；本席不改 ledger | 只记；不继承 |

无添加项，B5 不发问、不写 `questions/`（目录无 M002 问件）。无锁外主张，不走 LOCK-AMEND。无新的铁律 4 HTML 修改申请。现文无新矛盾需上报暂停。建议队列：无。

## 9. 本席步骤结论

- 文件覆盖：1/1，979/979 行；全文 SHA256=`5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f`（与 B1 V0.10 同字节，阅读可复用，结论不继承）
- 判据覆盖：84/84（18 GEN + 66 PGM），本会话重判
- 三线：第一性原理、顺从式、对抗式均完整执行
- 摘录：锁 PGM 66/66 来源行对齐、标题匹配、全文包含；PGM-047 已为「查看考勤」
- 本席 findings：0
- 新增 HTML 自身问题：无。不申请改三份 HTML。铁律 4 无新暂停项
- 全局门禁：html-ledger-sync **exit 1**（归属 M426 台账 + M008 工具 + 本 HTML 工作区获批脏字节）；entry-anchors 及其测试 **exit 0**。不冒称全仓绿
- 未答 questions：无
- 台账计数：不修改；B3=2、本轮新增修复=0、交付失败=0 保持
- 独占哈希：只在本次报告首行；正文不重复该独占字段
- 未读：本次 Codex self raw
- 未做：改任何受禁文件、派子代理、MCP、fetch/commit/push/worktree/部署、后端测试、重开 M001、收口文件

**本席 M002 B5-r2 PASS。**

不是模块 PASS。不是双席合取。B5 后四值校验与 ledger 改 PASS / 写收口不在本步。旧计划同号 PASS、本文件 V0.01/V0.02 PASS、本席 B1 V0.10 PASS 均不继承。

命令退出码汇总（本次实跑）：锁/HTML `sha256sum` 与结构 Python exit 0；`npm run verify:html-ledger-sync` exit 1；`npm run verify:entry-anchors` exit 0；`node --test tools/verify-entry-anchors.test.mjs` exit 0。
