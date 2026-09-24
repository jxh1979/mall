CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M001 B5-r4 Grok 原始报告

文件自身版本：V0.01
席位：Grok（审计席乙，独立）
节点：160-audit-prompt.md V0.17 阶段 B / M001 / B5-r4
模块：M001 — 业务需求正本／需求HTML／第1段
本票性质：修复后整模块全量审计。本票不是模块 PASS，不得写入 ledger 模块 PASS。

## 0. 本回合身份与禁令执行

- 仅本席独立审计；未启动子代理、未派第三席、未读 Codex raw、未读旧审计 raw/结论、不继承旧 PASS。
- 未修改三份业务 HTML、criteria-lock、ledger、findings、questions；未提交、未推送、未部署；未进入 M002。
- B2 已双 PASS 且队列空：本席按指令跳过新的 B3/B4。B3 累计 4，额外仅 A/D 一次已用尽，无第五次修复。
- ledger 只读顶部：`CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；`LOCK-AMEND-COUNT: 1`；版本 V1.57。当前 M001 行：`IN_PROGRESS | B3=4 | 方案复核已修=2 | PASS证据=- | 子代理失败重跑=6`。
- 操作员当前直接业务解释原话（必须纳入，且仅作用于指定两处）：「b c两项原文不冲突。就是我的原意。」仅针对 BSM L1020 客源及 L1068 打折原因现行可配原文；现行两项即预期业务，不是容忍已确认缺陷；不扩大到其他分类，无需再次申请这两处修改。
- 累计授权：此前已逐条批准 BSM L499 目录、L2003 页脚日期和 L2 日期，已在 r3 实施；r4 仅批准 L1380 概括句及 L2 版本 V0.12→V0.13。新「仅 A/D」没有撤销此前批准。核修复权限用 r4 完整 before/after 树，不用 HEAD 累计 diff 指称越权。

## 1. 本席独立实算与工作区事实（类型 4）

命令与输出均为本回合实跑。

```
$ sha256sum /home/james/complex-management/docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
$ cat docs/audit-2026-09/plan/criteria-lock.sha256
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
$ git rev-parse HEAD
242f487233d028173cb9198eecd079f5d877405c
$ wc -l docs/business-enum-quick-reference-v0.1.html docs/business-system-map-for-operations-v0.1.html
   308 docs/business-enum-quick-reference-v0.1.html
  2032 docs/business-system-map-for-operations-v0.1.html
  2340 total
$ wc -c docs/audit-2026-09/plan/criteria-lock.md
889020
$ sha256sum docs/business-enum-quick-reference-v0.1.html docs/business-system-map-for-operations-v0.1.html
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa  docs/business-system-map-for-operations-v0.1.html
```

sidecar 与本席实算相等；ledger 顶部基准与本席实算相等。本席未从派发指令读取期望哈希。

### 1.1 r4 修复权限（before/after 树，非 HEAD 累计 diff）

```
$ git cat-file -t 4263c8c1338031a38d977741ffb111a0752b7092
tree
$ git cat-file -t 7d6c4f7544de2957faec09e04aa2a262ba4aad86
tree
$ git diff --stat 4263c8c… 7d6c4f75…
 docs/business-system-map-for-operations-v0.1.html | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)
$ git diff --name-only 4263c8c… 7d6c4f75…
docs/business-system-map-for-operations-v0.1.html
$ git diff --binary 4263c8c… 7d6c4f75… | sha256sum
57e3f181e0415c093f50322a26c826eb26623484992492499e30eadd65d0be46
$ git diff --binary 4263c8c… 7d6c4f75… | wc -c
2013
$ git hash-object docs/business-system-map-for-operations-v0.1.html
12334b13f3c27a815c9bbf9eea216403fb0d76d1
$ git rev-parse 7d6c4f7544de2957faec09e04aa2a262ba4aad86:docs/business-system-map-for-operations-v0.1.html
12334b13f3c27a815c9bbf9eea216403fb0d76d1
```

统一 diff 仅两 hunk：

1. L2：`文件版本：V0.12；更新日期：2026-09-08` → `文件版本：V0.13；更新日期：2026-09-08`（日期未改）
2. L1380：`处理后回访客人或商户，验收合格才关闭。` → `处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。`

工作树 BSM blob 等于 after tree；BEQR blob 在 after tree 与工作树相同（r4 未改 BEQR）。r4 未改目录、页脚、客源、打折原因、L521。当前 L2=`<!-- 文件版本：V0.13；更新日期：2026-09-08 -->`；L499=`<a href="#questions">已确认规则和实施前输入项</a>`；L2003 页脚日期 `2026-09-08`。与累计授权一致，不构成越权。

### 1.2 模块定义与闭集

`plan/modules.md` M001：文件 2；有效行 2340；NORMAL。文件：

- `docs/business-enum-quick-reference-v0.1.html`
- `docs/business-system-map-for-operations-v0.1.html`

映射行实拆：BEQR-001…197 + BSM-001…120 + GEN-001…018 = 335，无 PGM。与指令闭集一致。

锚点实跑：BEQR `td.k` = 197；BSM `h2` 27 + `h3` 93 = 120。BEQR 197 条答案均非空。HTMLParser 对两份文件 `parser_errors=[]`。BSM TOC href 27 与 `section id` 27 全等，broken href 空。

## 2. 技能预检（按 V0.17 §6 收窄，本节点 B5）

本席在本回合实际读取并按收窄口径使用：

| 技能 | 路径存在 | 本节点用法 |
|---|---|---|
| using-superpowers | 是 | 开工技能自检 |
| grilling / grill-me | 是 | 对抗式+第一性追问结构；作废等用户/派子代理 |
| verification-before-completion | 是 | PASS 前只采本回合命令输出 |
| ponytail-review | 是 | 只找可删；B5 不改码 |
| receiving-code-review | 是 | 先核实再接受/反驳；作废 IMPLEMENT |
| systematic-debugging | 是 | 只借读错误→复现→追数据流；不作无 ID 改动 |
| diagnosing-bugs | 是 | 以结构命令作 tight loop；文档豁免 Maven |
| codebase-design | 是 | 删除测试/深度：两份 HTML 是需求正本模块 |
| writing-for-agents | 是 | 本 raw 给下游席/操作员读 |
| i-have-adhd | 是 | 输出形态 |
| tdd / test-driven-development | 是 | 文档/HTML 不强制落测试 |
| resolving-merge-conflicts | 是 | 本回合无合并冲突，不适用 |
| git-guardrails-claude-code | 是（misc/） | 可选；宿主非 Claude Code hooks，未安装；本回合未执行 push/reset --hard |

缺口（存在性未核到，不冒充已调用）：`batch-grill-me` 不存在；`ubiquitous-language` 不存在。

明确不用（提示词划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、ponytail-audit、ponytail-debt、ponytail-gain、domain-modeling、code-review（mattpocock）、wait-what。B3 用 ponytail 修复强度：本席 B5 不是修复者，未调用 ponytail 本体做改码。

文档类型豁免：未跑 Maven / 业务系统测试，不因此自动 FAIL。本模块产物为两份说明性 HTML，证据用 HTML 逐字对照、结构解析、可复现命令。

## 3. 第一性原理线

依据只使用：① 三份 HTML 逐字原文；④ 本回合可复现命令输出。本模块无单独可执行二进制，不强制类型 2。未把惯例、历史结论、席位意见当依据。

### 3.1 为什么必须存在

- 删掉 BEQR：三份最高解释权声明（BEQR L23/L306、BSM L2003、PGM 页脚）要求枚举/状态/规则以写入这三份 HTML 为准；197 个 `td.k` 将失去权威答案。
- 删掉 BSM：经营闭环、配置删除审批、票务/酒吧/物业等 120 个 h2/h3 将失去正文。
- 两份共同组成 M001；PGM 在 M002，本席只取交叉上下文（全中文、可溯源、终端开关 opt-in、破窗、金额精度、解释权时间线），不开 M002。

### 3.2 声称行为是否被实际验证

- 文档不声称自身可启动业务系统。已验证：行数 308+2032=2340；锚点 197+120；锁摘录 335/335 与源文件逐字（标题在声称源行、摘录行均在源文件中）；TOC 可点；r4 两处改动在工作树。
- BSM-079 锁摘录含新 L1380 句，不含旧「验收合格才关闭」句。`rg` 在 BSM 正本对旧句 0 命中。

### 3.3 未验证就当真的前提

- 未把「B2 双 PASS」或任何旧 raw 当作本席 B5 证据。
- 未假设实现者会忽略同文件其他明示条款；对 L521 与 L1020/L1068、L1380 与 L1384、BEQR-172 与 BSM 工单分支，按全文共同执行检验（见对抗式）。
- 未新增「每处概述须重抄全部细则 / 问题列标题全文件唯一 / 仅一份 HTML 排他解释另两份」等锁外判据。

## 4. 顺从式：335 闭集逐条映射

冻结判据对 HTML 派生 ID 的共同正文：「映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。」本模块实现即两份 HTML 自身。核对方法：锁内 HTML 原文围栏 ⊆ 正本字节；标题落在声称源行；本席通读正本全文（BEQR 1–308，BSM 1–2032）。

GEN-001…018 作为文档模块的适用方式：

- GEN-001 必要性：两文件承载铁律 4 需求正本，不可删。
- GEN-002/003/014：文档豁免可执行构建与测试；未声称 Maven PASS。
- GEN-004…013：静态 HTML 无运行时输入/权限/并发/迁移；样式与导航脚本无业务发明。
- GEN-015：`lang="zh-CN"`，中文正文，nav 为真实 `<a href>`，表格有 `th`/`scope`；BEQR/BSM 展示文案为中文（单据号等四类除外，与 PGM 全中文原则交叉一致）。
- GEN-016：BSM 版本 V0.13 与页脚 2026-09-08 一致；TOC 链接可复现；BEQR 版本 V0.07 / 2026-09-01 为该文件自身未在 r4 修改的记录，不是假完成。
- GEN-017：本席按冲突→细化→技术性→添加顺序分类；无锁外主张。
- GEN-018：结论落到 HTML 原文与本回合命令。

下列每一行：三线均已覆盖；2.1 分类对本模块正文为「需求正本自身」（不是添加）；工程缺陷：否。

| 冻结ID | 标题 | 来源 | 顺从核对 |
|---|---|---|---|
| BEQR-001 | 票种 | business-enum-quick-reference-v0.1.html:29 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-002 | 票务策略状态 | business-enum-quick-reference-v0.1.html:30 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-003 | 定价、售票与收款主体 | business-enum-quick-reference-v0.1.html:32 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-004 | 年龄限制 | business-enum-quick-reference-v0.1.html:33 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-005 | 通票赠酒下限 | business-enum-quick-reference-v0.1.html:34 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-006 | 通票类型 | business-enum-quick-reference-v0.1.html:35 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-007 | 权益类型 | business-enum-quick-reference-v0.1.html:36 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-008 | 餐饮/零售券两种用法 | business-enum-quick-reference-v0.1.html:37 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-009 | 资格判定 | business-enum-quick-reference-v0.1.html:39 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-010 | 订单状态 | business-enum-quick-reference-v0.1.html:40 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-011 | 票/权益状态 | business-enum-quick-reference-v0.1.html:41 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-012 | 核销记录状态 | business-enum-quick-reference-v0.1.html:43 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-013 | 手环入场 | business-enum-quick-reference-v0.1.html:44 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-014 | 退款单状态 | business-enum-quick-reference-v0.1.html:45 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-015 | 有效期类型 | business-enum-quick-reference-v0.1.html:46 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-016 | 售票与退款原则 | business-enum-quick-reference-v0.1.html:47 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-017 | 到期未核销款 | business-enum-quick-reference-v0.1.html:48 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-018 | 域归属对照 | business-enum-quick-reference-v0.1.html:49 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-019 | 跨域通票分配 | business-enum-quick-reference-v0.1.html:51 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-020 | 酒吧通票可分配池 | business-enum-quick-reference-v0.1.html:52 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-021 | 收费与免费权益结算 | business-enum-quick-reference-v0.1.html:53 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-022 | 项目内部结算价 | business-enum-quick-reference-v0.1.html:54 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-023 | 核销顺序 | business-enum-quick-reference-v0.1.html:55 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-024 | 游乐核销归属 | business-enum-quick-reference-v0.1.html:56 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-025 | 门票综合服务费 | business-enum-quick-reference-v0.1.html:57 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-026 | 动态价与会员价 | business-enum-quick-reference-v0.1.html:58 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-027 | 人脸验龄数据生命周期 | business-enum-quick-reference-v0.1.html:60 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-028 | 会员唯一识别 | business-enum-quick-reference-v0.1.html:65 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-029 | 会员注册 | business-enum-quick-reference-v0.1.html:66 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-030 | 会员身份状态 | business-enum-quick-reference-v0.1.html:68 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-031 | 等级与成长值 | business-enum-quick-reference-v0.1.html:70 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-032 | 等级权益对照 | business-enum-quick-reference-v0.1.html:71 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-033 | 积分与成长值来源 | business-enum-quick-reference-v0.1.html:72 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-034 | 票务人次计算 | business-enum-quick-reference-v0.1.html:73 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-035 | 有效消费与冻结 | business-enum-quick-reference-v0.1.html:75 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-036 | 积分明细类型 | business-enum-quick-reference-v0.1.html:77 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-037 | 积分兑换定价 | business-enum-quick-reference-v0.1.html:78 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-038 | 商品规格 | business-enum-quick-reference-v0.1.html:79 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-039 | 项目零售参考价 | business-enum-quick-reference-v0.1.html:80 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-040 | 发券与核销结算 | business-enum-quick-reference-v0.1.html:81 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-041 | 重复兑换与转赠 | business-enum-quick-reference-v0.1.html:82 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-042 | 券包状态 | business-enum-quick-reference-v0.1.html:83 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-043 | 积分结算账期 | business-enum-quick-reference-v0.1.html:84 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-044 | 会员系统服务费 | business-enum-quick-reference-v0.1.html:85 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-045 | 储值卡类型与收款主体 | business-enum-quick-reference-v0.1.html:86 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-046 | 会员忠诚度扩展 | business-enum-quick-reference-v0.1.html:87 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-047 | 会员注册与开卡 | business-enum-quick-reference-v0.1.html:89 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-048 | 学生认证 | business-enum-quick-reference-v0.1.html:90 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-049 | 会员积分活动状态 | business-enum-quick-reference-v0.1.html:91 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-050 | 积分活动预算守恒 | business-enum-quick-reference-v0.1.html:92 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-051 | 充值积分 | business-enum-quick-reference-v0.1.html:93 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-052 | 会员营销边界 | business-enum-quick-reference-v0.1.html:94 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-053 | 发券对象 | business-enum-quick-reference-v0.1.html:95 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-054 | 桌台/卡座/吧台位状态 | business-enum-quick-reference-v0.1.html:100 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-055 | 账单状态 | business-enum-quick-reference-v0.1.html:101 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-056 | 订台定金状态 | business-enum-quick-reference-v0.1.html:103 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-057 | 订卡邀请函状态 | business-enum-quick-reference-v0.1.html:105 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-058 | 点酒方式 | business-enum-quick-reference-v0.1.html:106 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-059 | 支付方式 | business-enum-quick-reference-v0.1.html:107 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-060 | 销售提成 | business-enum-quick-reference-v0.1.html:108 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-061 | 服务员提成 | business-enum-quick-reference-v0.1.html:109 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-062 | 退菜退酒 | business-enum-quick-reference-v0.1.html:110 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-063 | 高危动作（需审批/双证据） | business-enum-quick-reference-v0.1.html:111 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-064 | 出品票据状态 | business-enum-quick-reference-v0.1.html:112 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-065 | 账单操作 | business-enum-quick-reference-v0.1.html:114 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-066 | 收银交班状态 | business-enum-quick-reference-v0.1.html:115 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-067 | 最低消费 | business-enum-quick-reference-v0.1.html:117 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-068 | 服务费 | business-enum-quick-reference-v0.1.html:118 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-069 | 小费 | business-enum-quick-reference-v0.1.html:119 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-070 | 客人存酒有效期与转存 | business-enum-quick-reference-v0.1.html:120 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-071 | 存酒瓶数 | business-enum-quick-reference-v0.1.html:122 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-072 | 套餐赠券适用范围 | business-enum-quick-reference-v0.1.html:123 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-073 | 非低消商品 | business-enum-quick-reference-v0.1.html:124 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-074 | 隐私桌台权限 | business-enum-quick-reference-v0.1.html:125 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-075 | 桌台会话与菜单 | business-enum-quick-reference-v0.1.html:126 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-076 | 桌台计时产品 | business-enum-quick-reference-v0.1.html:127 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-077 | 门店支付方式 | business-enum-quick-reference-v0.1.html:128 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-078 | 扫码点单身份 | business-enum-quick-reference-v0.1.html:129 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-079 | 优惠券自动选择 | business-enum-quick-reference-v0.1.html:130 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-080 | 优惠券付款预留 | business-enum-quick-reference-v0.1.html:131 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-081 | 正式挂起订单状态 | business-enum-quick-reference-v0.1.html:132 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-082 | 正式挂起订单规则 | business-enum-quick-reference-v0.1.html:133 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-083 | 挂单取消与付款互斥 | business-enum-quick-reference-v0.1.html:134 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-084 | 挂单坏账损耗 | business-enum-quick-reference-v0.1.html:135 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-085 | 项目开放状态 | business-enum-quick-reference-v0.1.html:140 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-086 | 场次状态 | business-enum-quick-reference-v0.1.html:142 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-087 | 游乐「当天／每天」口径 | business-enum-quick-reference-v0.1.html:144 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-088 | 容量双阈值 | business-enum-quick-reference-v0.1.html:145 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-089 | 园区容量 | business-enum-quick-reference-v0.1.html:146 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-090 | 设备关键/辅助分级 | business-enum-quick-reference-v0.1.html:147 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-091 | 全园故障与补偿通票 | business-enum-quick-reference-v0.1.html:148 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-092 | 安全严重度 | business-enum-quick-reference-v0.1.html:149 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-093 | 巡检任务状态 | business-enum-quick-reference-v0.1.html:150 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-094 | 越界类型 | business-enum-quick-reference-v0.1.html:151 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-095 | 团体/包场 | business-enum-quick-reference-v0.1.html:153 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-096 | 游乐增强阶段 | business-enum-quick-reference-v0.1.html:154 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-097 | 供应合同模式 | business-enum-quick-reference-v0.1.html:159 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-098 | 库房分类 | business-enum-quick-reference-v0.1.html:160 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-099 | 物品大类 | business-enum-quick-reference-v0.1.html:161 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-100 | 库存流水类型 | business-enum-quick-reference-v0.1.html:162 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-101 | 高价值判定 | business-enum-quick-reference-v0.1.html:164 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-102 | 跨主体借调 | business-enum-quick-reference-v0.1.html:166 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-103 | 智能采购建议 | business-enum-quick-reference-v0.1.html:168 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-104 | 门店要货履约状态 | business-enum-quick-reference-v0.1.html:169 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-105 | 经销商状态 | business-enum-quick-reference-v0.1.html:170 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-106 | 经销询价状态 | business-enum-quick-reference-v0.1.html:171 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-107 | 经销价格目录状态 | business-enum-quick-reference-v0.1.html:172 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-108 | 经销订单状态 | business-enum-quick-reference-v0.1.html:173 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-109 | 经销退货单状态 | business-enum-quick-reference-v0.1.html:174 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-110 | 经销库存外显 | business-enum-quick-reference-v0.1.html:175 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-111 | 经销支付方式 | business-enum-quick-reference-v0.1.html:176 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-112 | 经销库存预留 | business-enum-quick-reference-v0.1.html:177 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-113 | 经销职责分离 | business-enum-quick-reference-v0.1.html:178 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-114 | 经销取消、退货与开票 | business-enum-quick-reference-v0.1.html:179 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-115 | 经销价格与税务快照 | business-enum-quick-reference-v0.1.html:180 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-116 | 经销商停用撤权 | business-enum-quick-reference-v0.1.html:181 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-117 | 经销资金归属 | business-enum-quick-reference-v0.1.html:182 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-118 | 运营营业日 | business-enum-quick-reference-v0.1.html:187 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-119 | 对账口径 | business-enum-quick-reference-v0.1.html:188 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-120 | 对账差异四类 | business-enum-quick-reference-v0.1.html:189 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-121 | 日结状态 | business-enum-quick-reference-v0.1.html:190 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-122 | 差异处理状态 | business-enum-quick-reference-v0.1.html:191 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-123 | 结算状态 | business-enum-quick-reference-v0.1.html:192 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-124 | 综合通票定价 | business-enum-quick-reference-v0.1.html:193 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-125 | 商户业态 | business-enum-quick-reference-v0.1.html:195 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-126 | 合作模式 | business-enum-quick-reference-v0.1.html:196 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-127 | 退款资金流向 | business-enum-quick-reference-v0.1.html:197 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-128 | 物业费 | business-enum-quick-reference-v0.1.html:198 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-129 | 能源费与损耗费 | business-enum-quick-reference-v0.1.html:199 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-130 | 税费配置 | business-enum-quick-reference-v0.1.html:201 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-131 | 三方对账框架 | business-enum-quick-reference-v0.1.html:202 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-132 | 开票状态 | business-enum-quick-reference-v0.1.html:203 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-133 | 接入的外部系统 | business-enum-quick-reference-v0.1.html:208 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-134 | 客户标签 | business-enum-quick-reference-v0.1.html:209 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-135 | 统一原则 | business-enum-quick-reference-v0.1.html:210 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-136 | 接入状态 | business-enum-quick-reference-v0.1.html:211 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-137 | 回流结果状态 | business-enum-quick-reference-v0.1.html:212 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-138 | 风险四级 | business-enum-quick-reference-v0.1.html:217 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-139 | 事件状态 | business-enum-quick-reference-v0.1.html:218 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-140 | 主管复核强制项 | business-enum-quick-reference-v0.1.html:219 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-141 | 经营异常处置状态 | business-enum-quick-reference-v0.1.html:220 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-142 | 异常状态的统一出口 | business-enum-quick-reference-v0.1.html:221 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-143 | 四类主体 | business-enum-quick-reference-v0.1.html:226 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-144 | 经营主体关系 | business-enum-quick-reference-v0.1.html:227 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-145 | 城市项目公司 | business-enum-quick-reference-v0.1.html:228 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-146 | 运营公司职责 | business-enum-quick-reference-v0.1.html:229 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-147 | 主要收入归属 | business-enum-quick-reference-v0.1.html:230 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-148 | 合作模式 | business-enum-quick-reference-v0.1.html:231 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-149 | 职级 | business-enum-quick-reference-v0.1.html:232 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-150 | 终端十一端 | business-enum-quick-reference-v0.1.html:233 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-151 | 最终权限计算 | business-enum-quick-reference-v0.1.html:234 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-152 | 项目与敏感数据权限 | business-enum-quick-reference-v0.1.html:235 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-153 | 终端开关优先级 | business-enum-quick-reference-v0.1.html:236 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-154 | 综合体项目状态 | business-enum-quick-reference-v0.1.html:237 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-155 | 商户/运营主体状态 | business-enum-quick-reference-v0.1.html:238 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-156 | 合同状态 | business-enum-quick-reference-v0.1.html:239 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-157 | 员工状态 | business-enum-quick-reference-v0.1.html:240 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-158 | 设备状态 | business-enum-quick-reference-v0.1.html:241 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-159 | 权限状态 | business-enum-quick-reference-v0.1.html:242 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-160 | 招商线索状态 | business-enum-quick-reference-v0.1.html:248 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-161 | 招商意向阶段 | business-enum-quick-reference-v0.1.html:249 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-162 | 品牌/客户库类型 | business-enum-quick-reference-v0.1.html:250 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-163 | 合同变更类型 | business-enum-quick-reference-v0.1.html:251 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-164 | 退租结算状态 | business-enum-quick-reference-v0.1.html:252 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-165 | 内部应付执行状态 | business-enum-quick-reference-v0.1.html:253 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-166 | 合同审批链 | business-enum-quick-reference-v0.1.html:255 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-167 | 租金/账单类型 | business-enum-quick-reference-v0.1.html:256 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-168 | 营业额抽成模式 | business-enum-quick-reference-v0.1.html:257 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-169 | 公区能源费 | business-enum-quick-reference-v0.1.html:258 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-170 | 铺位租赁状态 | business-enum-quick-reference-v0.1.html:259 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-171 | 物业工单类型 | business-enum-quick-reference-v0.1.html:260 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-172 | 物业工单状态 | business-enum-quick-reference-v0.1.html:262 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-173 | 商户服务工单状态 | business-enum-quick-reference-v0.1.html:264 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-174 | 工单时效 | business-enum-quick-reference-v0.1.html:265 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-175 | 楼宇机电设备状态 | business-enum-quick-reference-v0.1.html:266 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-176 | 设备维保类型 | business-enum-quick-reference-v0.1.html:267 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-177 | 能耗表计类型 | business-enum-quick-reference-v0.1.html:268 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-178 | 抄表状态 | business-enum-quick-reference-v0.1.html:269 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-179 | 能耗归集对象 | business-enum-quick-reference-v0.1.html:270 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-180 | 楼宇巡检任务状态 | business-enum-quick-reference-v0.1.html:271 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-181 | 楼宇巡检类型 | business-enum-quick-reference-v0.1.html:273 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-182 | 考勤日结果 | business-enum-quick-reference-v0.1.html:279 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-183 | 打卡方式 | business-enum-quick-reference-v0.1.html:280 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-184 | 请假类型 | business-enum-quick-reference-v0.1.html:281 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-185 | 请假单状态 | business-enum-quick-reference-v0.1.html:282 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-186 | 薪资单构成 | business-enum-quick-reference-v0.1.html:283 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-187 | 员工提成发放 | business-enum-quick-reference-v0.1.html:285 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-188 | 服务订单状态 | business-enum-quick-reference-v0.1.html:290 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-189 | 管理费方式 | business-enum-quick-reference-v0.1.html:291 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-190 | 分红基数 | business-enum-quick-reference-v0.1.html:296 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-191 | 分红比例 | business-enum-quick-reference-v0.1.html:297 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-192 | 分红税务 | business-enum-quick-reference-v0.1.html:298 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-193 | 分红周期 | business-enum-quick-reference-v0.1.html:299 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-194 | 分红单状态 | business-enum-quick-reference-v0.1.html:300 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-195 | 可选扣除项 | business-enum-quick-reference-v0.1.html:301 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-196 | 发放保护线留存 | business-enum-quick-reference-v0.1.html:302 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BEQR-197 | 期间关账 | business-enum-quick-reference-v0.1.html:303 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-001 | 整体说明：这套系统到底管什么 | business-system-map-for-operations-v0.1.html:505 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-002 | 一笔业务怎么走 | business-system-map-for-operations-v0.1.html:526 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-003 | 系统通用能力：数据可改可删、操作有引导、权限分岗 | business-system-map-for-operations-v0.1.html:563 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-004 | 配置数据可以存档、也可以删除（删除要审批） | business-system-map-for-operations-v0.1.html:566 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-005 | 后台的"新建/录入"改成分步引导 | business-system-map-for-operations-v0.1.html:581 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-006 | 验证用的二维码一律动态刷新（间隔可在后台设定） | business-system-map-for-operations-v0.1.html:584 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-007 | 平台管理员拥有全部日常功能权限 | business-system-map-for-operations-v0.1.html:587 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-008 | 子系统入口 | business-system-map-for-operations-v0.1.html:592 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-009 | 基础管理 | business-system-map-for-operations-v0.1.html:596 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-010 | 票务权益 | business-system-map-for-operations-v0.1.html:601 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-011 | 客人端 | business-system-map-for-operations-v0.1.html:606 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-012 | 酒吧平板/工作手机 | business-system-map-for-operations-v0.1.html:611 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-013 | 触摸屏一体机 | business-system-map-for-operations-v0.1.html:616 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-014 | 酒吧管理 | business-system-map-for-operations-v0.1.html:621 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-015 | 供应链库存 | business-system-map-for-operations-v0.1.html:626 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-016 | 游乐管理 | business-system-map-for-operations-v0.1.html:631 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-017 | 商户管理 | business-system-map-for-operations-v0.1.html:637 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-018 | 招商租赁 | business-system-map-for-operations-v0.1.html:643 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-019 | 物业服务 | business-system-map-for-operations-v0.1.html:650 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-020 | 楼宇运营 | business-system-map-for-operations-v0.1.html:656 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-021 | 第三方接入 | business-system-map-for-operations-v0.1.html:661 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-022 | 留痕与异常 | business-system-map-for-operations-v0.1.html:666 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-023 | 财务对账 | business-system-map-for-operations-v0.1.html:671 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-024 | 持股分红 | business-system-map-for-operations-v0.1.html:676 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-025 | 经营看板 | business-system-map-for-operations-v0.1.html:681 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-026 | 各类入口 | business-system-map-for-operations-v0.1.html:686 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-027 | 基础管理：先把综合体的盘子管清楚 | business-system-map-for-operations-v0.1.html:694 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-028 | 主要功能 | business-system-map-for-operations-v0.1.html:698 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-029 | 和谁有关 | business-system-map-for-operations-v0.1.html:714 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-030 | 票务权益：管卖什么票、给什么权益、怎么核销 | business-system-map-for-operations-v0.1.html:727 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-031 | 主要功能 | business-system-map-for-operations-v0.1.html:731 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-032 | 和谁有关 | business-system-map-for-operations-v0.1.html:760 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-033 | 票种、通票与权益类型（系统支持哪些种类） | business-system-map-for-operations-v0.1.html:772 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-034 | 核销、退款与有效期规则 | business-system-map-for-operations-v0.1.html:796 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-035 | 会员与积分规则 | business-system-map-for-operations-v0.1.html:813 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-036 | 人脸验龄数据使用与删除 | business-system-map-for-operations-v0.1.html:840 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-037 | 客人端：客人自己买票、预定、查询、积分和出示二维码 | business-system-map-for-operations-v0.1.html:850 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-038 | 主要功能 | business-system-map-for-operations-v0.1.html:854 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-039 | 和谁有关 | business-system-map-for-operations-v0.1.html:873 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-040 | 登录与关键规则 | business-system-map-for-operations-v0.1.html:883 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-041 | 会员注册、认证与收费边界 | business-system-map-for-operations-v0.1.html:898 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-042 | 会员积分活动与发券 | business-system-map-for-operations-v0.1.html:907 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-043 | 会员营销明确边界 | business-system-map-for-operations-v0.1.html:916 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-044 | 酒吧平板/工作手机：平板点酒，工作手机核销 | business-system-map-for-operations-v0.1.html:929 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-045 | 主要功能 | business-system-map-for-operations-v0.1.html:933 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-046 | 平板图形点单（POS） | business-system-map-for-operations-v0.1.html:953 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-047 | 和谁有关 | business-system-map-for-operations-v0.1.html:963 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-048 | 触摸屏一体机：权益领取、现场服务和辅助售票 | business-system-map-for-operations-v0.1.html:978 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-049 | 主要功能 | business-system-map-for-operations-v0.1.html:982 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-050 | 和谁有关 | business-system-map-for-operations-v0.1.html:996 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-051 | 酒吧管理：从入场、开台到结账关台 | business-system-map-for-operations-v0.1.html:1010 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-052 | 主要功能 | business-system-map-for-operations-v0.1.html:1014 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-053 | 和谁有关 | business-system-map-for-operations-v0.1.html:1041 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-054 | 桌台/账单状态与经营规则 | business-system-map-for-operations-v0.1.html:1053 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-055 | 桌台、菜单与优惠券 | business-system-map-for-operations-v0.1.html:1088 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-056 | 正式未付款订单挂起 | business-system-map-for-operations-v0.1.html:1101 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-057 | 服务点单：客人点专项服务，公司统一收款、再按名录分账 | business-system-map-for-operations-v0.1.html:1120 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-058 | 怎么点、怎么收钱 | business-system-map-for-operations-v0.1.html:1124 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-059 | 钱怎么分、怎么退 | business-system-map-for-operations-v0.1.html:1135 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-060 | 供应链库存：管供应商、采购、库房、消耗和存酒 | business-system-map-for-operations-v0.1.html:1151 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-061 | 主要功能 | business-system-map-for-operations-v0.1.html:1155 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-062 | 和谁有关 | business-system-map-for-operations-v0.1.html:1174 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-063 | 库存类型与规则 | business-system-map-for-operations-v0.1.html:1186 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-064 | 采购建议与门店要货 | business-system-map-for-operations-v0.1.html:1208 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-065 | 外部经销商准入、价格与库存 | business-system-map-for-operations-v0.1.html:1216 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-066 | 经销订单、付款与履约 | business-system-map-for-operations-v0.1.html:1226 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-067 | 经销退货、退款、红冲与资金归属 | business-system-map-for-operations-v0.1.html:1238 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-068 | 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | business-system-map-for-operations-v0.1.html:1253 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-069 | 主要功能 | business-system-map-for-operations-v0.1.html:1257 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-070 | 和谁有关 | business-system-map-for-operations-v0.1.html:1276 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-071 | 游乐类型与规则 | business-system-map-for-operations-v0.1.html:1287 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-072 | 商户管理：商户在授权范围内完整使用自己的系统 | business-system-map-for-operations-v0.1.html:1308 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-073 | 主要功能 | business-system-map-for-operations-v0.1.html:1313 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-074 | 和谁有关 | business-system-map-for-operations-v0.1.html:1326 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-075 | 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | business-system-map-for-operations-v0.1.html:1339 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-076 | 主要功能 | business-system-map-for-operations-v0.1.html:1343 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-077 | 和谁有关 | business-system-map-for-operations-v0.1.html:1357 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-078 | 物业服务：商户和现场的报修、投诉、整改都进工单 | business-system-map-for-operations-v0.1.html:1370 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-079 | 主要功能 | business-system-map-for-operations-v0.1.html:1374 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-080 | 和谁有关 | business-system-map-for-operations-v0.1.html:1388 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-081 | 楼宇运营：机电设备、能耗抄表、楼宇巡检 | business-system-map-for-operations-v0.1.html:1401 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-082 | 主要功能 | business-system-map-for-operations-v0.1.html:1405 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-083 | 和谁有关 | business-system-map-for-operations-v0.1.html:1414 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-084 | 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | business-system-map-for-operations-v0.1.html:1427 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-085 | 日常工作 | business-system-map-for-operations-v0.1.html:1431 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-086 | 排班、考勤与请假 | business-system-map-for-operations-v0.1.html:1441 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-087 | 我的薪资 | business-system-map-for-operations-v0.1.html:1455 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-088 | 员工提成、活动执行与人员服务边界 | business-system-map-for-operations-v0.1.html:1467 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-089 | 第三方接入：外部系统继续做专业系统，平台拿回结果 | business-system-map-for-operations-v0.1.html:1479 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-090 | 机器人配送与扫码交接 | business-system-map-for-operations-v0.1.html:1545 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-091 | 视频监控（留点位/时间） | business-system-map-for-operations-v0.1.html:1554 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-092 | 重要事项短信通知（2026-06-29 新增） | business-system-map-for-operations-v0.1.html:1563 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-093 | 留痕与异常：重要动作留得住，出了问题追得回 | business-system-map-for-operations-v0.1.html:1574 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-094 | 必须关注的动作 | business-system-map-for-operations-v0.1.html:1578 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-095 | 怎样减少人工填写 | business-system-map-for-operations-v0.1.html:1596 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-096 | 处理结果 | business-system-map-for-operations-v0.1.html:1607 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-097 | 风险等级与风控规则 | business-system-map-for-operations-v0.1.html:1619 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-098 | 财务对账：把经营事实整理成财务能确认的口径 | business-system-map-for-operations-v0.1.html:1636 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-099 | 内部结算价目录（按商品规格逐项建目录） | business-system-map-for-operations-v0.1.html:1639 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-100 | 结算配置中心（后台总控页） | business-system-map-for-operations-v0.1.html:1641 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-101 | 项目零售参考价 | business-system-map-for-operations-v0.1.html:1643 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-102 | 主要功能 | business-system-map-for-operations-v0.1.html:1648 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-103 | 和谁有关 | business-system-map-for-operations-v0.1.html:1666 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-104 | 收入归属：哪类收入归哪个业态主体 | business-system-map-for-operations-v0.1.html:1678 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-105 | 三方对账框架：系统流水、渠道对账单、资金到账两两核对 | business-system-map-for-operations-v0.1.html:1697 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-106 | 对账与结算类型规则 | business-system-map-for-operations-v0.1.html:1709 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-107 | 持股分红：按持股把经营成果定期分给主理人 | business-system-map-for-operations-v0.1.html:1737 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-108 | 主要功能 | business-system-map-for-operations-v0.1.html:1741 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-109 | 谁会用 | business-system-map-for-operations-v0.1.html:1760 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-110 | 和谁有关 | business-system-map-for-operations-v0.1.html:1768 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-111 | 经营看板：让管理层和运营看得懂、能追问 | business-system-map-for-operations-v0.1.html:1781 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-112 | 主要指标 | business-system-map-for-operations-v0.1.html:1785 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-113 | 谁会看 | business-system-map-for-operations-v0.1.html:1813 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-114 | 各类入口：不同人只看自己该看的、只做自己该做的 | business-system-map-for-operations-v0.1.html:1827 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-115 | 相互关系：为什么这些子系统必须连在一起 | business-system-map-for-operations-v0.1.html:1897 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-116 | 系统交付与上线：正式上线要做的安全加固和安装交付 | business-system-map-for-operations-v0.1.html:1928 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-117 | 正式上线时的安全加固 | business-system-map-for-operations-v0.1.html:1932 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-118 | 能自动识别环境的安装包 + 内附教程 | business-system-map-for-operations-v0.1.html:1940 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-119 | 和谁有关 | business-system-map-for-operations-v0.1.html:1947 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| BSM-120 | 已确认规则和实施前输入项 | business-system-map-for-operations-v0.1.html:1957 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-001 | 必要性与最小性 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-002 | 可执行正确性 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-003 | 测试有效性 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-004 | 信任边界校验 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-005 | 授权与最小权限 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-006 | 数据完整性 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-007 | 并发与幂等 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-008 | 接口与模式一致性 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-009 | 错误处理与恢复 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-010 | 安全与敏感信息 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-011 | 可观测与审计证据 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-012 | 资源与性能边界 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-013 | 构建配置与依赖 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-014 | 数据库迁移正确性 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-015 | 界面可用性与可访问性 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-016 | 文档可验证性 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-017 | 业务来源分类 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |
| GEN-018 | 第一性原理证据 | 通用工程判据 | 原文在锁摘录与正本对应；本席已读该锚点正文；无锁内冲突/遗漏/发明 |

闭集计数：335 行，与映射 335 相等。

## 5. 对抗式（grilling 结构，事实自读）

假定实现错误，用整文边界构造正常可复现反例，再以全文能否共同执行判定是否成立。不派子代理、不等操作员。

### 5.1 反例：L521 分类不可配 vs L1020 客源可配、L1068 打折原因可配

业务上下文：BSM L521「业务分类定义也不在此列……有哪几种的清单……不做后台可配」。BSM L1020「来源枚举项为业务参数，按项目可配」。BSM L1068「现场打折必须选择原因分类：招待、客诉补偿、活动执行、员工亲友、其他（枚举项业务参数，按项目可配）」。

操作员原话：「b c两项原文不冲突。就是我的原意。」指令限定：现行两项即预期业务，不是容忍缺陷；不扩大到其他分类。

共同执行：L521 约束未在条款中标明可配的「有哪几种」清单；L1020/L1068 是同文件对这两项枚举的明示可配。实现者同时遵守两条：只把这两项按项目可配，其余分类清单仍须先写入 HTML。L521 所举可配对象是阈值/比例/时长/次数/金额/时刻/档位取值；枚举项可配不是那类数值上下限参数。不把「概述必须重抄细则」做成新判据。

分类：不构成冲突。工程缺陷：否。冻结ID：BSM-001（L521 所在整体说明）、BSM-052（L1020）、BSM-054（L1068）。

### 5.2 反例：工单「验收合格才关闭」vs 类型分支

r4 前 L1380 旧句与 L1384/BEQR-172 分支（保洁完成即结；客人投诉客户关怀部记录后可关）不能共同执行。r4 后 L1380 改为指向下述已确认分支；BEQR-172 同分支。旧句在 BSM 正本 `rg` 0 命中。BSM 物业 lead/卡片仍出现「回访验收」字样，属该章能力概述，细则在 L1380+L1384；不另造「概述须重抄细则」。

分类：原冲突已按授权修复。工程缺陷：否。冻结ID：BSM-079、BEQR-172。

### 5.3 反例：r4 越权（用 HEAD 累计 diff）

指令禁止用 HEAD 累计 diff。r4 树 diff 仅 BSM 一文件、两处获批句。L499/L2003/L2 日期属 r3 已批，仍在 after tree，不是 r4 越权。

分类：不成立。闸 3 归因不在本席 B5 重开队列（B2 队列空、跳过新 B3/B4）。

### 5.4 反例：核销「不提供人工放行」vs 人脸误拒复核 / 营业兜底免票

BEQR-012：任何岗位不能让不满足规则的核销通过。BEQR-004/BSM 年龄核验：误拒可人工复核并发一次性放行凭据，豁免的是年龄核验记录校验，不是改写票务规则。BSM 核销单位：设备损坏营业兜底是场馆级免票，不是单笔违规核销放行。共同执行成立。

工程缺陷：否。冻结ID：BEQR-004、BEQR-012、BSM-034。

### 5.5 反例：小费转台归属

BEQR-065/BSM 拆单合单：小费跟实际提供服务的人，转台后归新台。BSM 小费行：主责跟台位、不跟客人。共同执行：人绑定台位不随客人迁移；转台后新台有自己的主责，小费归新台主责。

工程缺陷：否。冻结ID：BEQR-065、BEQR-069、BSM-054。

### 5.6 反例：子系统入口卡片缺少服务点单/员工手机端

TOC 与后文 h2 均有这两章。卡片网格未列。若把「卡片必须等于 TOC」写成判据，即自行新增。全文共同执行仍要求实现这两章（BSM-057、BSM-084）。不作为 finding。

### 5.7 反例：BEQR 物业工单类型含内部协作工单，BSM 物业创建列表未写

BEQR-171 含内部协作工单。BSM L1376 物业创建列表未列该名。BSM L578 通用能力专写内部部门间工单。共同执行：内部协作走通用内部工单，物业服务创建列表覆盖报修等现场类。不把「两份清单必须逐字同构」当判据。

工程缺陷：否。冻结ID：BEQR-171、BSM-004、BSM-079。

### 5.8 反例：BEQR 主管复核强制项含人工补录，BSM 强制项表行未列

BEQR-140 含人工补录。BSM L1627 表行未列。BSM L1611 明文「补录一律进入主管复核队列」。共同执行成立。

工程缺陷：否。冻结ID：BEQR-140、BSM-096。

### 5.9 反例：BEQR 物业工单状态枚举未在 BSM 重抄

BEQR-172 列出待派单…已超时。BSM 用分支规则而非重抄状态机。共同执行：状态以 BEQR 为准，关闭规则以 BSM L1384 与 BEQR-172 分支为准。不新增「概述须重抄全部细则」。

### 5.10 反例：两处「合作模式」同名

BEQR-126 与 BEQR-148 标题同为合作模式，后者多「其他」须指定收费模板。锁按 `td.k` 行各一条，不要求全文件标题唯一。共同执行：同一六类 + 合同侧额外约束。

工程缺陷：否。

### 5.11 PGM 交叉（不开 M002）

- 可溯源 / 安检例外：PGM 与 BSM L522、L800 一致。
- 全中文四例外：PGM 与两份中文正文一致；本模块 HTML 源码标签/class 属实现，不是展示文案。
- 终端开关缺行默认关：PGM L736 与 BSM L1999 一致。
- 金额精度：PGM 对跨域尾差指定售票收款主体；BSM L1726 指定实际承担结算责任主体。跨域组合通票收款主体为娱乐平台公司，两句可共同执行。不把 PGM 专章展开为 M002 审计。
- 解释权时间线：三份页脚同构，未发现本模块两份之间无法共同执行的明文互否。

### 5.12 ponytail-review

r4 diff 净 +0 行量级（2 增 2 删）。需求正本无「可删业务句」授权。`Lean already. Ship.` `net: -0 lines possible.`

## 6. 四分类收口

按冲突 → 细化 → 技术性 → 添加走完。对抗式 5.1–5.11 均未形成可复现、锁内、全文不能共同执行的冲突。无下位实现缺陷。无业务发明添加项。样式/锚点脚本为技术性且无缺陷，不另开添加闸。

## 7. Findings

本席本次完整报告 findings 集合为空。

未使用临时 ID。无工程缺陷成立项。无新的 HTML 审批问题。不申请改三份 HTML。

## 8. 本席结论

- **本席 B5：PASS**
- **新 HTML 问题：无**
- **本票不是模块 PASS。** 模块生命周期仍为 ledger 所载 `IN_PROGRESS`；B5 双 PASS 且四值校验是收口前置，本席不得先记模块 PASS。
- 跳过新的 B3/B4；B3 累计保持 4；无第五次修复额度。
- 未改任何禁止路径。报告落盘本文件 V0.01。

CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M001 B5-r4 Grok 原始报告

文件自身版本：V0.02（仅本追加段；保留调用前 77534 字节 / SHA256 1334b1e26cd7d4c99ee942534ad6e44eab5ae5c99676cd020fd1ea992c68bbad 原前缀与 V0.01 正文，不覆盖、不删、不改原字节。既有 V0.01 只用于识别历史票，不作为本次结论依据，不继承旧 B1/B5 PASS。）
席位：Grok（审计席乙，独立，固定只读）
节点：160-audit-prompt.md V0.17 阶段 B / M001 / B5-r4（新锁 V0.14 全模块重审；同一 B3 次数=4 下向同名 raw 追加，不另编号 r5）
模块：M001 — 业务需求正本／需求HTML／第1段
工作区：`/home/james/complex-management` 的 `main` 正本
本票性质：B2 双 PASS、队列空、按规约跳过新的 B3/B4 之后的整模块全量审计。本票不是模块 PASS，不得写入 ledger 模块 PASS，不得写收口文件。
日期：2026-09-22

## 0. 本回合身份、禁令与技能

- 仅本席独立审计；未启动 MCP、未派子代理、未 fetch、未 commit、未 push、未 worktree、未部署。
- 未读本次 Codex self 任何报告（未打开 `M001-b1-self-raw.md` / `M001-b5-r4-self-raw.md`）。
- 未修改三份业务 HTML、源码、锁、sidecar、ledger、findings、questions。
- 既有输入（只作边界，不继承结论）：
  1. 本席 `modules/M001-b1-grok-raw.md` **最后独占锁行 L3695 起**完整 V0.21 B1 + 其后 V0.22 B2（B2 PASS、当前 finding 空集、同意空队列跳 B5）。
  2. `findings.md` **顶部**「当前 M001 新锁B2双PASS定稿：无待修项」（V3.32）：16 旧 ID 队列均否，无添加、无 M001 questions；跳 B3/B4 进 B5-r4；累计 B3 仍 4；当前不是模块 PASS；PGM 决策台账门禁红归属其它模块。
  3. 操作员口径：B/C 原文即原意、不新增限制；A/D 历史修复保留；BSM L930/L1865 查看考勤、L1445 工作手机打卡；旧 BSM-120 日期副本已完整 LOCK-AMEND；新问题如实列出、不强行放行；M009 单次授权代码不在本模块。
- 复用声明：两份必审 HTML 本轮 `sha256sum` 与本席 V0.21 记录逐字相同（BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`；BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`）。因此 **复用本席 V0.21 对同字节 HTML 的全文阅读事实**（BEQR L1–L308；BSM 27 个 section/h2 通读）。本轮对全范围、全 335 判据仍做独立重新判断；不继承 V0.21 B1 PASS、不继承 V0.01 本文件旧 B5 PASS、不继承 V0.22 B2 PASS 作为本步结论。
- 交叉只读、不扩模块：`docs/platform-governance-map-for-operations-v0.1.html`（979 行 / SHA256 `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f`；相对 HEAD 有改，属 M002 现场）。
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` 版本 **V0.14**；映射 `M001 → BEQR-001…197 + BSM-001…120 + GEN-001…018` = **335**，无缺号、无重复、无 PGM。本席独立 `sha256sum` 得本段第一行；未从派发读取期望哈希。sidecar 单行与 ledger 顶部与本段第一行同值。`LOCK-AMEND-COUNT: 1`。
- ledger 只读：版本字段 **V7.57**；M001 表行 `IN_PROGRESS`，B3=4，方案复核已修=2，子代理失败重跑=6。本席不写 ledger。
- 本追加段正文不再另写独占锁哈希行；不要求 stdout 机器标记。

按 V0.17 §6 收窄、本节点为 B5 文档模块。grilling 只借追问结构，事实自行读取，不派子代理、不等操作员。systematic-debugging 只借「读错误→复现→追数据流」，不作任何代码改动。verification-before-completion：凡 PASS/FAIL 声称均附本回合命令或明确复用标记。ponytail-review 只列可删，不改文件。tdd 本节点无修复，不强制落测试。

| 技能 | 本席核验路径 | 本节点使用 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检；用户指令优先于技能 |
| `using-arc` | `/home/james/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 用户本批禁令优先；本节点不走 Arc implement/commit |
| `graft` | `/home/james/.claude/skills/graft/SKILL.md` | 本模块两文件为 docs HTML，graft 索引源码；V0.17 禁止 MCP。未用 graft 检索业务源码 |
| `grilling` / `grill-me` | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗式自问自答；本体等用户/派子代理作废 |
| `codebase-design` | `.../engineering/codebase-design/SKILL.md` | 文档作为接口：锚点唯一、两文件非浅转发 |
| `diagnosing-bugs` | `.../engineering/diagnosing-bugs/SKILL.md` | 对疑似 nav 漏项与日期副本构造可复现对照 |
| `systematic-debugging` | superpowers 该技能 | 根因停在原文/解析，不进入修复 |
| `verification-before-completion` | superpowers 该技能 | 哈希、摘录、链接、门禁均本回合实跑或标明复用 |
| `ponytail-review` | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 可删项：重复「合作模式」键是否可删 |
| `writing-for-agents` | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写全，不另产收口文件 |
| `batch-grill-me` | 本机技能树未找到独立可调用包 | **缺口**，未冒充调用 |
| `ubiquitous-language` | 同上未找到 | **缺口**，未冒充调用 |

明确不用：`dispatching-parallel-agents`、`subagent-driven-development`、`research`、`executing-plans`、`using-git-worktrees`、`finishing-a-development-branch`、`ponytail`（B3）、`test-driven-development`/`tdd`、`requesting-code-review`（B4）、`brainstorming`/`writing-plans`（阶段 A）。

## 1. 本回合独立取证（类型 4）与复用区别

工作目录：`/home/james/complex-management`。未 fetch、未 commit、未安装依赖。

### 1.1 本轮实际新跑

```
$ git rev-parse HEAD
e456ba96c84185c8d39f18f548011daac48a6466
$ git rev-parse --abbrev-ref HEAD
main
$ git rev-parse origin/main
ebb1eb99c610df330c07d24278798c2d3611299a
$ git log -1 --oneline HEAD
e456ba96 audit(M046): 钉住破窗不能无盘续期、换钥作废旧票
$ sha256sum docs/audit-2026-09/plan/criteria-lock.md
b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
$ wc -c docs/audit-2026-09/plan/criteria-lock.md
891182
$ cat docs/audit-2026-09/plan/criteria-lock.sha256
b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
$ sha256sum docs/business-enum-quick-reference-v0.1.html docs/business-system-map-for-operations-v0.1.html docs/platform-governance-map-for-operations-v0.1.html
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-enum-quick-reference-v0.1.html
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-system-map-for-operations-v0.1.html
5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f  docs/platform-governance-map-for-operations-v0.1.html
$ wc -l -c docs/business-enum-quick-reference-v0.1.html docs/business-system-map-for-operations-v0.1.html
   308 133961 docs/business-enum-quick-reference-v0.1.html
  2032 304482 docs/business-system-map-for-operations-v0.1.html
  2340 438443 total
$ git diff --name-only HEAD -- docs/business-enum-quick-reference-v0.1.html docs/business-system-map-for-operations-v0.1.html
<empty>
$ git diff --stat HEAD -- docs/platform-governance-map-for-operations-v0.1.html
 docs/platform-governance-map-for-operations-v0.1.html | 6 +++---
 1 file changed, 3 insertions(+), 3 deletions(-)
$ file -bi docs/business-enum-quick-reference-v0.1.html docs/business-system-map-for-operations-v0.1.html
text/html; charset=utf-8
text/html; charset=utf-8
$ node --test tools/verify-entry-anchors.test.mjs
PASS：历史 raw 放行，现行 findings、ledger、模块收口和普通文档的失效引用仍拒绝。
tests 1 / pass 1 / fail 0 / EXIT 0
$ node tools/verify-html-ledger-sync.mjs
[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。
  已改动的 HTML：docs/platform-governance-map-for-operations-v0.1.html
  必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md
  比对范围：工作区 ↔ 基线 origin/main@88c7b465
EXIT 1
```

本轮 Python 结构/摘录（一次性扫完全部锚点，不是抽样后宣称整文件）：

- UTF-8 可解码、BOM=0、NUL=0、CR=0、CRLF=0、U+FFFD=0。
- BEQR 开闭：html/head/body/table/thead 差 0；script=0；td.k **197**；物理行 29…303 与锁来源行全等；摘录与该 `tr` **197/197 逐字相等**。
- BSM 开闭：html/head/body/table/thead/script/section 差 0；h2 **27** + h3 **93** = **120**；摘录 **116 EQ + 4 EQ_rstrip**（BSM-003/004/005/006 段末比摘录多 1 个 LF，摘录为真前缀，文案无差）+ **0 NE**。
- **BSM-120**：摘录长度 5525 == `join(bsm_lines[1956:])`；页脚 `更新日期：2026-09-10`；摘录与切片均无 `2026-09-08`。
- 锁全文 `2026-09-08` 命中 **0**。锁定义区 401（GEN18+BEQR197+BSM120+PGM66），缺号空。M001 映射 335 unique、三前缀各自 1…N 连续，无 PGM。
- BSM `<nav class="nav">` 内 href **27** 与 `<section id>` **27** 双向全等（含 `#overview`；该 `<a>` 带 `class="active"`，不得用 `href=">` 紧邻正则漏计）。BEQR href 0。BSM 全文件 href 45，全部 `#` 片段，无外链。
- 表：BEQR 13、BSM 13，无 th 的表 = 0。无 `<img>`。`lang="zh-CN"`。
- BSM L2=`<!-- 文件版本：V0.14；更新日期：2026-09-10 -->`；L2003 页脚同日。
- `questions/` 8 个文件，无 `M001-*`。findings 版本 V3.32，字节 1220324。
- 追加前本 grok B5 raw：77534 字节 / SHA256 1334b1e26cd7d4c99ee942534ad6e44eab5ae5c99676cd020fd1ea992c68bbad。

本地 `origin/main` 与 HEAD 不等；此差不纳入本模块 finding，也不据此改任何文件。本席未 fetch。

### 1.2 明确复用（本轮未重跑）

| 项 | 复用来源 | 本轮如何钉住未变 |
|---|---|---|
| 两份 HTML 2340 行全文阅读（BEQR 十三章正文、BSM 27 个 section 逐段） | 本席 V0.21 对同字节的阅读事实 | 本轮 sha256 与 V0.21 记录全等；相对 HEAD 无 diff |
| Maven / 业务系统测试 / 预发布全链 | 文档豁免；不强制造后台测试 | 未跑；不因此自动 FAIL |
| `verify:predeploy` 其余项 | 未跑 | html-ledger-sync 已红，其后项会停；不宣称全局绿 |
| 历史 grok raw 在 L3695 之前的海量命令 | 按指令不读 | 只读最后独占锁行起段 |
| 旧 B5-r4 V0.01 结论 | 不继承 | 仅保留原字节 |

### 1.3 模块闭集

`plan/modules.md` M001：文件 2；有效行 2340；NORMAL。

- `docs/business-enum-quick-reference-v0.1.html`
- `docs/business-system-map-for-operations-v0.1.html`

## 2. 第一性原理（2.3 三问）

复用 V0.21 同字节全文阅读；本轮用命令复核结构与摘录后独立重答。不把「上一轮自己就是这么判的」当依据。

### 2.1 `docs/business-enum-quick-reference-v0.1.html`（本轮覆盖 L1–L308）

1. **为何必须存在**：三份 HTML 拥有最终解释权（L23、L306）。删掉本表，BEQR-001…197 的「有哪几种 / 有哪几个状态 / 按什么规则」闭集在权威源上消失；BSM 多处「见枚举速查表××条」失去上位清单。必要性成立（类型 1）。
2. **声称行为是否执行**：说明性 HTML，不是可执行服务。本回合类型 4：197 个 `td.k`、锁摘录逐字、UTF-8/开闭标签。无脚本。豁免成立。
3. **未验证前提**：L86「长沙项目公司」与 L228「城市项目公司」是否同一法律主体——三份 HTML 未写恒等式。本席不把二者判冲突，也不把等同当真。无冻结 ID 要求证明法人同一。BEQR 页眉日期 2026-09-01 与 BSM 页脚 2026-09-10 是两份文件各自版本，不是同一锚点滞后。

### 2.2 `docs/business-system-map-for-operations-v0.1.html`（本轮覆盖 L1–L2032）

1. **为何必须存在**：业务系统地图是经营动作与连接关系的权威叙述（L2003）。删掉则 BSM-001…120 的子系统、入口、红线、工单、考勤现场规则无权威源。必要性成立（类型 1）。
2. **声称行为是否执行**：说明性 HTML + 页内目录高亮脚本。类型 4：120 锚点、27 nav 双向、13 表有 th、锁摘录对照。未跑浏览器渲染；脚本只做 scroll 高亮（L2009 `document.querySelectorAll(".nav a")`），无网络、无存储。豁免成立。
3. **未验证前提**：旧 BSM-120 锁摘录页脚 09-08 的前提本轮不存在：L2 与 L2003 为 2026-09-10，摘录与 L1957–EOF 逐字相等。HTML 内部日期不自相矛盾，锁副本不再滞后。

### 2.3 两文件作为模块为何必须同时存在

删掉任一，三份共同解释权不完整。BEQR 给闭集枚举，BSM 给流程与现场分工。二者不是互相可删的浅转发（删除测试——复杂度会回到另一份未承载的条款上）。概述不要求重复全部细则；冲突只在明文互否时成立。

## 3. 顺从式：335 条映射判据逐项核对

判定口径：锁定义区全文为唯一判据来源；模块文件须与该锚点现行 HTML 原文一致。BEQR 对照 `td.k` 整行；BSM 对照该 `h2/h3` 至下一标题（末条至 EOF，切片含页脚与 script）。GEN 按文档产物类型适用。本表由本轮实算生成，不是抄 V0.21 的 PASS 列，也不是抄旧 B5 V0.01。

证据类型：1=三份 HTML 逐字原文；4=本轮可复现命令输出。文档面无类型 2 强制。

### 3.1 文件覆盖表（不能只抽样）

| 文件 | 行 | 本轮动作 | 复用 |
|---|---:|---|---|
| BEQR L1–L28 | 头/样式/h1/tip/第一章表头 | 本轮实读 | 其余散文复用 V0.21 同字节 |
| BEQR L29–L303 | 197 个 td.k | 本轮逐行摘录对照 + 关键行实读 | 非关键行正文复用 V0.21 |
| BEQR L304–L308 | 解释权/收尾 | 本轮实读 | — |
| BSM L1–L30 | doctype/版本/CSS 起 | 本轮实读 L2 | 样式块复用 V0.21 |
| BSM L469–L505 | nav 27 + overview | 本轮实读 | — |
| BSM 27 个 section | 505–2001 | 本轮 120 锚点切片对照 | 段内未定点行复用 V0.21 通读 |
| BSM 定点 | 499/521/522/800/930/1020/1068/1260–1261/1374–1384/1445/1611/1627/1726/1865/1999/2003 | 本轮实读 | — |
| BSM L2003–L2032 | 页脚+script | 本轮实读页脚；script 用途核 L2009 | — |
| PGM 交叉 | 979 行不审全文 | 本轮 SHA/行数/L2/L736/L974/金额原则/四例外 | 不扩 M002 |

### 3.2 BEQR-001…197

| ID | 标题 | HTML行 | 摘录对照（本轮） | 交叉/适用 | 证据 | 本席 |
|---|---|---:|---|---|---|---|
| BEQR-001 | 票种 | 29 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-002 | 票务策略状态 | 30 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-003 | 定价、售票与收款主体 | 32 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-004 | 年龄限制 | 33 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-005 | 通票赠酒下限 | 34 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-006 | 通票类型 | 35 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-007 | 权益类型 | 36 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-008 | 餐饮/零售券两种用法 | 37 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-009 | 资格判定 | 39 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-010 | 订单状态 | 40 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-011 | 票/权益状态 | 41 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-012 | 核销记录状态 | 43 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-013 | 手环入场 | 44 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-014 | 退款单状态 | 45 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-015 | 有效期类型 | 46 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-016 | 售票与退款原则 | 47 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-017 | 到期未核销款 | 48 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-018 | 域归属对照 | 49 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-019 | 跨域通票分配 | 51 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-020 | 酒吧通票可分配池 | 52 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-021 | 收费与免费权益结算 | 53 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-022 | 项目内部结算价 | 54 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-023 | 核销顺序 | 55 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-024 | 游乐核销归属 | 56 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-025 | 门票综合服务费 | 57 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-026 | 动态价与会员价 | 58 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-027 | 人脸验龄数据生命周期 | 60 | EQ 锁摘录==td.k整行 | 票务闭集在本表；BSM-030/033/034 给流程与现场，不要求地图重抄本行全部取值 | 1+4 | PASS |
| BEQR-028 | 会员唯一识别 | 65 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-029 | 会员注册 | 66 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-030 | 会员身份状态 | 68 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-031 | 等级与成长值 | 70 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-032 | 等级权益对照 | 71 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-033 | 积分与成长值来源 | 72 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-034 | 票务人次计算 | 73 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-035 | 有效消费与冻结 | 75 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-036 | 积分明细类型 | 77 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-037 | 积分兑换定价 | 78 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-038 | 商品规格 | 79 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-039 | 项目零售参考价 | 80 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-040 | 发券与核销结算 | 81 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-041 | 重复兑换与转赠 | 82 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-042 | 券包状态 | 83 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-043 | 积分结算账期 | 84 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-044 | 会员系统服务费 | 85 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-045 | 储值卡类型与收款主体 | 86 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-046 | 会员忠诚度扩展 | 87 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-047 | 会员注册与开卡 | 89 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-048 | 学生认证 | 90 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-049 | 会员积分活动状态 | 91 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-050 | 积分活动预算守恒 | 92 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-051 | 充值积分 | 93 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-052 | 会员营销边界 | 94 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-053 | 发券对象 | 95 | EQ 锁摘录==td.k整行 | 会员闭集在本表；BSM-035/041–043 给边界与现场，交叉解释不重复全部档位 | 1+4 | PASS |
| BEQR-054 | 桌台/卡座/吧台位状态 | 100 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-055 | 账单状态 | 101 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-056 | 订台定金状态 | 103 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-057 | 订卡邀请函状态 | 105 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-058 | 点酒方式 | 106 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-059 | 支付方式 | 107 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-060 | 销售提成 | 108 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-061 | 服务员提成 | 109 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-062 | 退菜退酒 | 110 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-063 | 高危动作（需审批/双证据） | 111 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-064 | 出品票据状态 | 112 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-065 | 账单操作 | 114 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-066 | 收银交班状态 | 115 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-067 | 最低消费 | 117 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-068 | 服务费 | 118 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-069 | 小费 | 119 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-070 | 客人存酒有效期与转存 | 120 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-071 | 存酒瓶数 | 122 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-072 | 套餐赠券适用范围 | 123 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-073 | 非低消商品 | 124 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-074 | 隐私桌台权限 | 125 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-075 | 桌台会话与菜单 | 126 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-076 | 桌台计时产品 | 127 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-077 | 门店支付方式 | 128 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-078 | 扫码点单身份 | 129 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-079 | 优惠券自动选择 | 130 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-080 | 优惠券付款预留 | 131 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-081 | 正式挂起订单状态 | 132 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-082 | 正式挂起订单规则 | 133 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-083 | 挂单取消与付款互斥 | 134 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-084 | 挂单坏账损耗 | 135 | EQ 锁摘录==td.k整行 | 酒吧闭集在本表；BSM-051/054–056 给开台到关台，B/C可配取值不增种类 | 1+4 | PASS |
| BEQR-085 | 项目开放状态 | 140 | EQ 锁摘录==td.k整行 | 游乐闭集在本表；BSM-068/071 给容量/安全现场，概述不重抄双阈值细则 | 1+4 | PASS |
| BEQR-086 | 场次状态 | 142 | EQ 锁摘录==td.k整行 | 游乐闭集在本表；BSM-068/071 给容量/安全现场，概述不重抄双阈值细则 | 1+4 | PASS |
| BEQR-087 | 游乐「当天／每天」口径 | 144 | EQ 锁摘录==td.k整行 | 游乐闭集在本表；BSM-068/071 给容量/安全现场，概述不重抄双阈值细则 | 1+4 | PASS |
| BEQR-088 | 容量双阈值 | 145 | EQ 锁摘录==td.k整行 | 游乐闭集在本表；BSM-068/071 给容量/安全现场，概述不重抄双阈值细则 | 1+4 | PASS |
| BEQR-089 | 园区容量 | 146 | EQ 锁摘录==td.k整行 | 游乐闭集在本表；BSM-068/071 给容量/安全现场，概述不重抄双阈值细则 | 1+4 | PASS |
| BEQR-090 | 设备关键/辅助分级 | 147 | EQ 锁摘录==td.k整行 | 游乐闭集在本表；BSM-068/071 给容量/安全现场，概述不重抄双阈值细则 | 1+4 | PASS |
| BEQR-091 | 全园故障与补偿通票 | 148 | EQ 锁摘录==td.k整行 | 游乐闭集在本表；BSM-068/071 给容量/安全现场，概述不重抄双阈值细则 | 1+4 | PASS |
| BEQR-092 | 安全严重度 | 149 | EQ 锁摘录==td.k整行 | 游乐闭集在本表；BSM-068/071 给容量/安全现场，概述不重抄双阈值细则 | 1+4 | PASS |
| BEQR-093 | 巡检任务状态 | 150 | EQ 锁摘录==td.k整行 | 游乐闭集在本表；BSM-068/071 给容量/安全现场，概述不重抄双阈值细则 | 1+4 | PASS |
| BEQR-094 | 越界类型 | 151 | EQ 锁摘录==td.k整行 | 游乐闭集在本表；BSM-068/071 给容量/安全现场，概述不重抄双阈值细则 | 1+4 | PASS |
| BEQR-095 | 团体/包场 | 153 | EQ 锁摘录==td.k整行 | 游乐闭集在本表；BSM-068/071 给容量/安全现场，概述不重抄双阈值细则 | 1+4 | PASS |
| BEQR-096 | 游乐增强阶段 | 154 | EQ 锁摘录==td.k整行 | 游乐闭集在本表；BSM-068/071 给容量/安全现场，概述不重抄双阈值细则 | 1+4 | PASS |
| BEQR-097 | 供应合同模式 | 159 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-098 | 库房分类 | 160 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-099 | 物品大类 | 161 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-100 | 库存流水类型 | 162 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-101 | 高价值判定 | 164 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-102 | 跨主体借调 | 166 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-103 | 智能采购建议 | 168 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-104 | 门店要货履约状态 | 169 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-105 | 经销商状态 | 170 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-106 | 经销询价状态 | 171 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-107 | 经销价格目录状态 | 172 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-108 | 经销订单状态 | 173 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-109 | 经销退货单状态 | 174 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-110 | 经销库存外显 | 175 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-111 | 经销支付方式 | 176 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-112 | 经销库存预留 | 177 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-113 | 经销职责分离 | 178 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-114 | 经销取消、退货与开票 | 179 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-115 | 经销价格与税务快照 | 180 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-116 | 经销商停用撤权 | 181 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-117 | 经销资金归属 | 182 | EQ 锁摘录==td.k整行 | 供应链闭集在本表；BSM-060/063–067 给履约，经销细则以本表为准 | 1+4 | PASS |
| BEQR-118 | 运营营业日 | 187 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-119 | 对账口径 | 188 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-120 | 对账差异四类 | 189 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-121 | 日结状态 | 190 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-122 | 差异处理状态 | 191 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-123 | 结算状态 | 192 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-124 | 综合通票定价 | 193 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-125 | 商户业态 | 195 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-126 | 合作模式 | 196 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-127 | 退款资金流向 | 197 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-128 | 物业费 | 198 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-129 | 能源费与损耗费 | 199 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-130 | 税费配置 | 201 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-131 | 三方对账框架 | 202 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-132 | 开票状态 | 203 | EQ 锁摘录==td.k整行 | 财务闭集在本表；BSM-098/104–106 给对账现场，尾差交叉PGM售票收款主体 | 1+4 | PASS |
| BEQR-133 | 接入的外部系统 | 208 | EQ 锁摘录==td.k整行 | 第三方闭集在本表；BSM-089–092 给接入现场 | 1+4 | PASS |
| BEQR-134 | 客户标签 | 209 | EQ 锁摘录==td.k整行 | 第三方闭集在本表；BSM-089–092 给接入现场 | 1+4 | PASS |
| BEQR-135 | 统一原则 | 210 | EQ 锁摘录==td.k整行 | 第三方闭集在本表；BSM-089–092 给接入现场 | 1+4 | PASS |
| BEQR-136 | 接入状态 | 211 | EQ 锁摘录==td.k整行 | 第三方闭集在本表；BSM-089–092 给接入现场 | 1+4 | PASS |
| BEQR-137 | 回流结果状态 | 212 | EQ 锁摘录==td.k整行 | 第三方闭集在本表；BSM-089–092 给接入现场 | 1+4 | PASS |
| BEQR-138 | 风险四级 | 217 | EQ 锁摘录==td.k整行 | 风控闭集在本表；BSM-093/096/097 共同解释；短表未列不等于否定长段 | 1+4 | PASS |
| BEQR-139 | 事件状态 | 218 | EQ 锁摘录==td.k整行 | 风控闭集在本表；BSM-093/096/097 共同解释；短表未列不等于否定长段 | 1+4 | PASS |
| BEQR-140 | 主管复核强制项 | 219 | EQ 锁摘录==td.k整行 | 风控闭集在本表；BSM-093/096/097 共同解释；短表未列不等于否定长段 | 1+4 | PASS |
| BEQR-141 | 经营异常处置状态 | 220 | EQ 锁摘录==td.k整行 | 风控闭集在本表；BSM-093/096/097 共同解释；短表未列不等于否定长段 | 1+4 | PASS |
| BEQR-142 | 异常状态的统一出口 | 221 | EQ 锁摘录==td.k整行 | 风控闭集在本表；BSM-093/096/097 共同解释；短表未列不等于否定长段 | 1+4 | PASS |
| BEQR-143 | 四类主体 | 226 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-144 | 经营主体关系 | 227 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-145 | 城市项目公司 | 228 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-146 | 运营公司职责 | 229 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-147 | 主要收入归属 | 230 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-148 | 合作模式 | 231 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-149 | 职级 | 232 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-150 | 终端十一端 | 233 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-151 | 最终权限计算 | 234 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-152 | 项目与敏感数据权限 | 235 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-153 | 终端开关优先级 | 236 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-154 | 综合体项目状态 | 237 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-155 | 商户/运营主体状态 | 238 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-156 | 合同状态 | 239 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-157 | 员工状态 | 240 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-158 | 设备状态 | 241 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-159 | 权限状态 | 242 | EQ 锁摘录==td.k整行 | 治理枚举在本表；主体/终端细则交叉PGM，本模块不审M002全文 | 1+4 | PASS |
| BEQR-160 | 招商线索状态 | 248 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-161 | 招商意向阶段 | 249 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-162 | 品牌/客户库类型 | 250 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-163 | 合同变更类型 | 251 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-164 | 退租结算状态 | 252 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-165 | 内部应付执行状态 | 253 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-166 | 合同审批链 | 255 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-167 | 租金/账单类型 | 256 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-168 | 营业额抽成模式 | 257 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-169 | 公区能源费 | 258 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-170 | 铺位租赁状态 | 259 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-171 | 物业工单类型 | 260 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-172 | 物业工单状态 | 262 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-173 | 商户服务工单状态 | 264 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-174 | 工单时效 | 265 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-175 | 楼宇机电设备状态 | 266 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-176 | 设备维保类型 | 267 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-177 | 能耗表计类型 | 268 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-178 | 抄表状态 | 269 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-179 | 能耗归集对象 | 270 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-180 | 楼宇巡检任务状态 | 271 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-181 | 楼宇巡检类型 | 273 | EQ 锁摘录==td.k整行 | 地产闭集在本表；BSM-075/078/081 给招商物业楼宇现场；工单关闭以BEQR-172+BSM L1384为准 | 1+4 | PASS |
| BEQR-182 | 考勤日结果 | 279 | EQ 锁摘录==td.k整行 | 考勤薪资闭集在本表；BSM L930/1865查看考勤、L1445工作手机打卡，交叉不互否 | 1+4 | PASS |
| BEQR-183 | 打卡方式 | 280 | EQ 锁摘录==td.k整行 | 考勤薪资闭集在本表；BSM L930/1865查看考勤、L1445工作手机打卡，交叉不互否 | 1+4 | PASS |
| BEQR-184 | 请假类型 | 281 | EQ 锁摘录==td.k整行 | 考勤薪资闭集在本表；BSM L930/1865查看考勤、L1445工作手机打卡，交叉不互否 | 1+4 | PASS |
| BEQR-185 | 请假单状态 | 282 | EQ 锁摘录==td.k整行 | 考勤薪资闭集在本表；BSM L930/1865查看考勤、L1445工作手机打卡，交叉不互否 | 1+4 | PASS |
| BEQR-186 | 薪资单构成 | 283 | EQ 锁摘录==td.k整行 | 考勤薪资闭集在本表；BSM L930/1865查看考勤、L1445工作手机打卡，交叉不互否 | 1+4 | PASS |
| BEQR-187 | 员工提成发放 | 285 | EQ 锁摘录==td.k整行 | 考勤薪资闭集在本表；BSM L930/1865查看考勤、L1445工作手机打卡，交叉不互否 | 1+4 | PASS |
| BEQR-188 | 服务订单状态 | 290 | EQ 锁摘录==td.k整行 | 服务点单闭集在本表；BSM-057–059 给分账退款现场 | 1+4 | PASS |
| BEQR-189 | 管理费方式 | 291 | EQ 锁摘录==td.k整行 | 服务点单闭集在本表；BSM-057–059 给分账退款现场 | 1+4 | PASS |
| BEQR-190 | 分红基数 | 296 | EQ 锁摘录==td.k整行 | 分红闭集在本表；BSM-107–110 给发放现场 | 1+4 | PASS |
| BEQR-191 | 分红比例 | 297 | EQ 锁摘录==td.k整行 | 分红闭集在本表；BSM-107–110 给发放现场 | 1+4 | PASS |
| BEQR-192 | 分红税务 | 298 | EQ 锁摘录==td.k整行 | 分红闭集在本表；BSM-107–110 给发放现场 | 1+4 | PASS |
| BEQR-193 | 分红周期 | 299 | EQ 锁摘录==td.k整行 | 分红闭集在本表；BSM-107–110 给发放现场 | 1+4 | PASS |
| BEQR-194 | 分红单状态 | 300 | EQ 锁摘录==td.k整行 | 分红闭集在本表；BSM-107–110 给发放现场 | 1+4 | PASS |
| BEQR-195 | 可选扣除项 | 301 | EQ 锁摘录==td.k整行 | 分红闭集在本表；BSM-107–110 给发放现场 | 1+4 | PASS |
| BEQR-196 | 发放保护线留存 | 302 | EQ 锁摘录==td.k整行 | 分红闭集在本表；BSM-107–110 给发放现场 | 1+4 | PASS |
| BEQR-197 | 期间关账 | 303 | EQ 锁摘录==td.k整行 | 分红闭集在本表；BSM-107–110 给发放现场 | 1+4 | PASS |

BEQR 汇总：**197 PASS / 0 FAIL**。每条权威源即本模块文件自身；锁摘录与现行 HTML 逐字相等即满足「不得冲突、遗漏或另行发明」。

### 3.3 BSM-001…120

| ID | 标题 | HTML行 | 摘录对照（本轮） | 交叉/适用 | 证据 | 本席 |
|---|---|---:|---|---|---|---|
| BSM-001 | 整体说明：这套系统到底管什么 | 505 | EQ 锁摘录==锚点段 | 总述；子系统细则见后续h2/h3与BEQR对应章 | 1+4 | PASS |
| BSM-002 | 一笔业务怎么走 | 526 | EQ 锁摘录==锚点段 | 总述；子系统细则见后续h2/h3与BEQR对应章 | 1+4 | PASS |
| BSM-003 | 系统通用能力：数据可改可删、操作有引导、权限分岗 | 563 | EQ_rstrip 段末多1字节LF，文案无差 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-004 | 配置数据可以存档、也可以删除（删除要审批） | 566 | EQ_rstrip 段末多1字节LF，文案无差 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-005 | 后台的"新建/录入"改成分步引导 | 581 | EQ_rstrip 段末多1字节LF，文案无差 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-006 | 验证用的二维码一律动态刷新（间隔可在后台设定） | 584 | EQ_rstrip 段末多1字节LF，文案无差 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-007 | 平台管理员拥有全部日常功能权限 | 587 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-008 | 子系统入口 | 592 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-009 | 基础管理 | 596 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-010 | 票务权益 | 601 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-011 | 客人端 | 606 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-012 | 酒吧平板/工作手机 | 611 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-013 | 触摸屏一体机 | 616 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-014 | 酒吧管理 | 621 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-015 | 供应链库存 | 626 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-016 | 游乐管理 | 631 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-017 | 商户管理 | 637 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-018 | 招商租赁 | 643 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-019 | 物业服务 | 650 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-020 | 楼宇运营 | 656 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-021 | 第三方接入 | 661 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-022 | 留痕与异常 | 666 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-023 | 财务对账 | 671 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-024 | 持股分红 | 676 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-025 | 经营看板 | 681 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-026 | 各类入口 | 686 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-027 | 基础管理：先把综合体的盘子管清楚 | 694 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-028 | 主要功能 | 698 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-029 | 和谁有关 | 714 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-030 | 票务权益：管卖什么票、给什么权益、怎么核销 | 727 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-031 | 主要功能 | 731 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-032 | 和谁有关 | 760 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-033 | 票种、通票与权益类型（系统支持哪些种类） | 772 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-034 | 核销、退款与有效期规则 | 796 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-035 | 会员与积分规则 | 813 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-036 | 人脸验龄数据使用与删除 | 840 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-037 | 客人端：客人自己买票、预定、查询、积分和出示二维码 | 850 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-038 | 主要功能 | 854 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-039 | 和谁有关 | 873 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-040 | 登录与关键规则 | 883 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-041 | 会员注册、认证与收费边界 | 898 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-042 | 会员积分活动与发券 | 907 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-043 | 会员营销明确边界 | 916 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-044 | 酒吧平板/工作手机：平板点酒，工作手机核销 | 929 | EQ 锁摘录==锚点段 | 私人手机「查看考勤」≠工作手机「考勤打卡」(L1445/BEQR-183) | 1+4 | PASS |
| BSM-045 | 主要功能 | 933 | EQ 锁摘录==锚点段 | 私人手机「查看考勤」≠工作手机「考勤打卡」(L1445/BEQR-183) | 1+4 | PASS |
| BSM-046 | 平板图形点单（POS） | 953 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-047 | 和谁有关 | 963 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-048 | 触摸屏一体机：权益领取、现场服务和辅助售票 | 978 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-049 | 主要功能 | 982 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-050 | 和谁有关 | 996 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-051 | 酒吧管理：从入场、开台到结账关台 | 1010 | EQ 锁摘录==锚点段 | L1020来源可配、L1068打折原因可配=用户原意B/C；L521禁止增删「有哪几种」 | 1+4 | PASS |
| BSM-052 | 主要功能 | 1014 | EQ 锁摘录==锚点段 | L1020来源可配、L1068打折原因可配=用户原意B/C；L521禁止增删「有哪几种」 | 1+4 | PASS |
| BSM-053 | 和谁有关 | 1041 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-054 | 桌台/账单状态与经营规则 | 1053 | EQ 锁摘录==锚点段 | L1020来源可配、L1068打折原因可配=用户原意B/C；L521禁止增删「有哪几种」 | 1+4 | PASS |
| BSM-055 | 桌台、菜单与优惠券 | 1088 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-056 | 正式未付款订单挂起 | 1101 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-057 | 服务点单：客人点专项服务，公司统一收款、再按名录分账 | 1120 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-058 | 怎么点、怎么收钱 | 1124 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-059 | 钱怎么分、怎么退 | 1135 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-060 | 供应链库存：管供应商、采购、库房、消耗和存酒 | 1151 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-061 | 主要功能 | 1155 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-062 | 和谁有关 | 1174 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-063 | 库存类型与规则 | 1186 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-064 | 采购建议与门店要货 | 1208 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-065 | 外部经销商准入、价格与库存 | 1216 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-066 | 经销订单、付款与履约 | 1226 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-067 | 经销退货、退款、红冲与资金归属 | 1238 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-068 | 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | 1253 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-069 | 主要功能 | 1257 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-070 | 和谁有关 | 1276 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-071 | 游乐类型与规则 | 1287 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-072 | 商户管理：商户在授权范围内完整使用自己的系统 | 1308 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-073 | 主要功能 | 1313 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-074 | 和谁有关 | 1326 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-075 | 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | 1339 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-076 | 主要功能 | 1343 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-077 | 和谁有关 | 1357 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-078 | 物业服务：商户和现场的报修、投诉、整改都进工单 | 1370 | EQ 锁摘录==锚点段 | L1380概括指向分支；关闭规则以L1384与BEQR-172为准，无超时默认关闭 | 1+4 | PASS |
| BSM-079 | 主要功能 | 1374 | EQ 锁摘录==锚点段 | L1380概括指向分支；关闭规则以L1384与BEQR-172为准，无超时默认关闭 | 1+4 | PASS |
| BSM-080 | 和谁有关 | 1388 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-081 | 楼宇运营：机电设备、能耗抄表、楼宇巡检 | 1401 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-082 | 主要功能 | 1405 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-083 | 和谁有关 | 1414 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-084 | 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 1427 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-085 | 日常工作 | 1431 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-086 | 排班、考勤与请假 | 1441 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-087 | 我的薪资 | 1455 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-088 | 员工提成、活动执行与人员服务边界 | 1467 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-089 | 第三方接入：外部系统继续做专业系统，平台拿回结果 | 1479 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-090 | 机器人配送与扫码交接 | 1545 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-091 | 视频监控（留点位/时间） | 1554 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-092 | 重要事项短信通知（2026-06-29 新增） | 1563 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-093 | 留痕与异常：重要动作留得住，出了问题追得回 | 1574 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-094 | 必须关注的动作 | 1578 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-095 | 怎样减少人工填写 | 1596 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-096 | 处理结果 | 1607 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-097 | 风险等级与风控规则 | 1619 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-098 | 财务对账：把经营事实整理成财务能确认的口径 | 1636 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-099 | 内部结算价目录（按商品规格逐项建目录） | 1639 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-100 | 结算配置中心（后台总控页） | 1641 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-101 | 项目零售参考价 | 1643 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-102 | 主要功能 | 1648 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-103 | 和谁有关 | 1666 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-104 | 收入归属：哪类收入归哪个业态主体 | 1678 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-105 | 三方对账框架：系统流水、渠道对账单、资金到账两两核对 | 1697 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-106 | 对账与结算类型规则 | 1709 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-107 | 持股分红：按持股把经营成果定期分给主理人 | 1737 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-108 | 主要功能 | 1741 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-109 | 谁会用 | 1760 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-110 | 和谁有关 | 1768 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-111 | 经营看板：让管理层和运营看得懂、能追问 | 1781 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-112 | 主要指标 | 1785 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-113 | 谁会看 | 1813 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-114 | 各类入口：不同人只看自己该看的、只做自己该做的 | 1827 | EQ 锁摘录==锚点段 | 私人手机「查看考勤」≠工作手机「考勤打卡」(L1445/BEQR-183) | 1+4 | PASS |
| BSM-115 | 相互关系：为什么这些子系统必须连在一起 | 1897 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-116 | 系统交付与上线：正式上线要做的安全加固和安装交付 | 1928 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-117 | 正式上线时的安全加固 | 1932 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-118 | 能自动识别环境的安装包 + 内附教程 | 1940 | EQ 锁摘录==锚点段 | 与对应BEQR章共同执行；概述不重抄全部细则 | 1+4 | PASS |
| BSM-119 | 和谁有关 | 1947 | EQ 锁摘录==锚点段 | 入口/关系概述，不要求重复该子系统全部枚举取值 | 1+4 | PASS |
| BSM-120 | 已确认规则和实施前输入项 | 1957 | EQ 锁摘录==锚点段 | 摘录含页脚2026-09-10，与L2一致；旧09-08副本已LOCK-AMEND，本轮逐字相等 | 1+4 | PASS |

BSM 汇总：**120 PASS / 0 FAIL**。BSM-003…006 仅段末多 1 个换行，不构成文案差，不单独立项。

### 3.4 GEN-001…018

| ID | 标题 | HTML行 | 摘录对照 | 适用证据 | 证据 | 本席 |
|---|---|---:|---|---|---|---|
| GEN-001 | 必要性与最小性 | 0 | 文档面适用 | 两文件为需求正本。删BEQR则197条闭集消失；删BSM则120条经营动作无权威源。解释权声明L23/L306/L2003。不是浅转发。 | 1+4 | PASS |
| GEN-002 | 可执行正确性 | 0 | 文档面适用 | 说明性HTML。本轮类型4：UTF-8/开闭/197 td.k/120锚点/摘录。script只做nav高亮。文档豁免可执行服务。未跑Maven。 | 4 | PASS |
| GEN-003 | 测试有效性 | 0 | 文档面适用 | 文档不强制造后台测试。本轮实跑verify-entry-anchors（pass1/fail0）；未用测试通过代替正文对照。 | 4 | PASS |
| GEN-004 | 信任边界校验 | 0 | 文档面适用 | 静态HTML无外部输入首次使用面。script仅querySelector/scroll。无finding。 | 1+4 | PASS |
| GEN-005 | 授权与最小权限 | 0 | 文档面适用 | 本模块无受保护操作实现。权限口径写在HTML供其它模块映射。M009单次授权代码不在本模块。 | 1 | PASS |
| GEN-006 | 数据完整性 | 0 | 文档面适用 | 无事务写入面。状态机以BEQR正文为口径，本文件不落库。 | 1 | PASS |
| GEN-007 | 并发与幂等 | 0 | 文档面适用 | 无可重试执行路径。PGM「先成功为准」交叉只读，不扩M002。 | 1 | PASS |
| GEN-008 | 接口与模式一致性 | 0 | 文档面适用 | 两文件与锁197+120锚点对齐（mismatch=0）。PGM交叉可共同执行。不把PGM专章当本模块文件。 | 1+4 | PASS |
| GEN-009 | 错误处理与恢复 | 0 | 文档面适用 | 无运行时错误路径。 | 4 | PASS |
| GEN-010 | 安全与敏感信息 | 0 | 文档面适用 | 无密钥/注入面。EXPIRED仅在券包积分退回终态描述且有「不可用」。 | 1+4 | PASS |
| GEN-011 | 可观测与审计证据 | 0 | 文档面适用 | 无运行时日志面。HTML自身要求留痕，属需求正本而非本文件实现。 | 1 | PASS |
| GEN-012 | 资源与性能边界 | 0 | 文档面适用 | 无循环查询连接面。BSM script对27个nav节点，有界。 | 4 | PASS |
| GEN-013 | 构建配置与依赖 | 0 | 文档面适用 | 单文件自包含CSS/HTML。file -bi=text/html; charset=utf-8。 | 4 | PASS |
| GEN-014 | 数据库迁移正确性 | 0 | 文档面适用 | 无迁移。 | 4 | PASS |
| GEN-015 | 界面可用性与可访问性 | 0 | 文档面适用 | lang=zh-CN；13+13表均有th；BSM nav 27与section 27双向全等（含overview带class=active）。无img。 | 4 | PASS |
| GEN-016 | 文档可验证性 | 0 | 文档面适用 | BSM L2与L2003均为V0.14/2026-09-10。BSM-120摘录5525字节==L1957–EOF。锁全文2026-09-08命中0。BEQR页眉V0.07/2026-09-01为该文件独立版本，文件内自洽。 | 1+4 | PASS |
| GEN-017 | 业务来源分类 | 0 | 文档面适用 | 冲突→细化→技术性→添加已走完。B/C保持原意不升冲突；样式/nav为技术性无业务发明；无添加项。 | 1 | PASS |
| GEN-018 | 第一性原理证据 | 0 | 文档面适用 | 结论落到HTML原文或本轮命令输出；不引用旧B1/B5票本身作依据。 | 1+4 | PASS |

GEN 汇总：**18 PASS / 0 FAIL**。

### 3.5 335 合计

**335 PASS / 0 FAIL**。无未审条。不继承旧票。

### 3.6 考勤三处（本轮定点重读，不是只看日期）

- BSM L930 lead：私人手机可办事项含「查看考勤」，不含「考勤打卡」。锁 BSM-044 摘录与该段 EQ。
- BSM L1865 入口表「员工私人手机 App」：同样「查看考勤」。锁 BSM-114 摘录 EQ。
- BSM L1445：上班打卡仍以领取**工作手机**开始（NFC/扫码/离开充电设施）。BEQR-183 L280 打卡方式同口径。

私人手机「查看」与工作手机「打卡」不是同一动作，无明文互否。不判冲突。M009 单次授权代码不在本模块。

### 3.7 B/C 原文（必须保持，不是容忍缺陷）

本轮定点重读：

- **B** BSM L1020：开台来源自动判定 +「来源枚举项为业务参数，按项目可配。」
- **C** BSM L1068（高危动作长行内）：「现场打折必须选择原因分类：招待、客诉补偿、活动执行、员工亲友、其他（枚举项业务参数，按项目可配）」
- L521：「有哪几种」清单不做后台可配；可配的是阈值、比例、时长、次数、金额、时刻、档位取值。

操作员原话「b c两项原文不冲突。就是我的原意。」本席独立重读：B 配的是来源枚举项、C 配的是打折原因分类，与 L521 并不形成互否——可配的是已批准清单内的取值集合，不是增删种类。分类：**细化**，工程缺陷**不成立**。不是拒绝修复后带病保留。不引入新限制。

### 3.8 A/D 现文（独立核，不继承已修）

- **A** BSM L1380 现为「处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。」L1384 按商户投诉 / 消费客人投诉分支，无「超时默认关闭」。BEQR-172 L262 同口径。
- **D** BSM L2 与 L2003 均为 2026-09-10，文件内版本与页脚一致。锁 BSM-120 摘录现与 L1957–EOF 一致。旧日期副本已完整 LOCK-AMEND，不再滞后。

## 4. 对抗式（grilling 结构，自问自答）

事实自行读取，不等操作员。本轮对同字节 HTML 重问；日期项按新锁重答。发现新问题必须列出，不强行放行。

❓ **Q1 私人手机查看考勤是否让打卡规则作废？**  
➡️ 否。L930/L1865 是离场查看；L1445/BEQR-183 是现场领取工作手机记上班。反例「在私人手机上打卡」没有 HTML 授权句。不成立冲突。

❓ **Q2 B/C「业务参数，按项目可配」是否发明新种类？**  
➡️ 否。L521 禁止后台增删「有哪几种」。B/C 是已列种类的配置面。按原意保持。

❓ **Q3 手环入场（BEQR L44）是否跳过三道环节？**  
➡️ 否。核销记录状态（BEQR L43）写死三道须逐道通过、不提供人工放行。手环句未写豁免安检/年龄核验。不成立冲突。

❓ **Q4 两处「合作模式」键是否程序冲突？**  
➡️ 否。L196（财务结算章短列）与 L231（平台治理章：选其他须指定收费模板）。不同父 h2，不是程序主键。锁未要求全局 `td.k` 唯一。不另发明「标题必须唯一」判据。

❓ **Q5 BEQR 物业工单类型含「内部协作工单」，BSM L1376 创建列表未点名，是否冲突？**  
➡️ 否。BSM 未写「仅此几类、不设内部协作」。沉默不是相反命题。无冻结 ID 要求地图重复枚举每一个取值。不扩锁。

❓ **Q6 BEQR-140 主管复核强制项含「人工补录」，BSM L1627 短表未列，是否冲突？**  
➡️ 否。BSM L1611 已写「补录一律进入主管复核队列」。短表与长段共同解释，非互否。

❓ **Q7 包场：BEQR 停止对外核销 vs BSM L1300 需主管审批，是否冲突？**  
➡️ 否。一给禁核销效果，一给审批门。无「地图允许对外核销」句。

❓ **Q8 大屏叫号单独成 li 是否无用途？**  
➡️ 否。L1260 已写排队预计等待时间由现场大屏提示。L1261 是该能力的标题项。有现场显示用途。

❓ **Q9 EXPIRED 是否产品界面英文？**  
➡️ 否。BEQR L83 在积分退回流水终态描述里，并有「不可用」中文。不是 UI 文案证据。四例外交叉 PGM，本处属内部终态标识。

❓ **Q10 治理商户联系人明文例外是否本模块 HTML 新冲突？**  
➡️ 否。例外在 PGM，已获批。本模块两文件无该句。M009 单次授权代码未修不塞入。

❓ **Q11 BSM-120 页脚日期锁是否仍滞后？**  
➡️ 否。本轮摘录与 HTML L1957–EOF 5525 字节逐字相等，页脚均为 2026-09-10，与 L2 一致。上轮 GR-01 的事实前提已消失。不把已消除的副本滞后写成当前缺陷。

❓ **Q12 全局 html-ledger-sync 红是否证明两份 M001 HTML 故障？**  
➡️ 否。本轮命令点名的已改文件是 PGM。M001 两文件相对 HEAD 无 diff。红保留归属观察，不折进本模块产品 finding，不声称全局绿。

❓ **Q13 nav 是否缺 overview（本轮解析曾报 26 vs 27）？**  
➡️ 否。根因是正则要求 `href=">` 紧邻结束，漏掉 `<a href="#overview" class="active">`。正确解析 nav 27 与 section 27 全等。不构成缺陷。

❓ **Q14 BEQR 页眉 2026-09-01 与 BSM 2026-09-10 是否新的日期冲突？**  
➡️ 否。两份文件各自版本字段，不是同一锚点的锁副本滞后。各文件内部 L2 与自身声明一致。不新造缺陷。

❓ **Q15 长沙项目公司 vs 城市项目公司是否必须判冲突？**  
➡️ 否。无冻结 ID 要求二者恒等或互斥。未验证前提保留，不升冲突，不加锁。

## 5. 历史 16 条独立重判（输入，不继承旧 PASS）

| ID | 独立现判 | 分类 | 工程缺陷 | 说明 |
|---|---|---|---|---|
| M001-001 | 现文不再呈现原地图状态口径病句 | 冲突（历史） | 现不成立 | 独立未复现需再删的明文 |
| M001-002 | BEQR L39 不足一年不可、满一年可再享 | 冲突（历史） | 现不成立 | 资格规则现文自洽 |
| M001-003 | BEQR L251 合同变更八类现齐 | 冲突（历史） | 现不成立 | 合同变更八类现齐 |
| M001-004 | CSS/目录/静态结构 | 技术性 | 否 | 无业务发明；27 nav 双向；只登记 |
| M001-005 | L1380/L1384/BEQR-172 分支关闭，无超时默认关闭 | 冲突（历史） | 现不成立 | 不吸收为新 GR |
| M001-006 | BEQR L146、BSM 容量均引用已有容量条 | 细化（历史） | 现不成立 | 空答案已不在 |
| M001-007 | 手环与三环节合取，见 Q3 | 细化 | 否 | 无豁免明文 |
| M001-008 | 13+13 表皆有 th | 技术性（历史） | 现不成立 | 表头语义现在 |
| M001-009 | EXPIRED 在流水终态句且有中文 | 细化 | 否 | 见 Q9 |
| M001-010 | 大屏叫号有现场用途 | 细化 | 否 | 见 Q8 |
| M001-011 | 包场两份共同解释 | 细化 | 否 | 见 Q7 |
| M001-012 | BSM L2 与页脚同为 2026-09-10 | 技术性（历史） | 现不成立于 HTML | 文件内双日期已消除 |
| M001-013 | 同上 | 技术性（历史） | 现不成立于 HTML | 不把已对齐的锁摘录日期算进这两条旧 ID |
| M001-014 | B 原文即原意 | 细化 | 否 | 见 §3.7；非容忍缺陷 |
| M001-015 | C 原文即原意 | 细化 | 否 | 见 §3.7；非容忍缺陷 |
| M001-016 | A/L1380 与 D/L2 现文在 | 冲突（历史） | 现不成立于 HTML | 不复用为本段 GR |

16 个 ID 连续唯一。当前修复队列「是」= **空集**。B3 累计 **4** 保持，本批新增 0。无第五次修复授权。无添加、无未答 M001 questions。

## 6. 三 HTML 交叉（不开 M002，不要求概述重复全部细则）

- 解释权声明：BEQR L306、BSM L2003、PGM L974 同构；冲突按确认时间线。本模块两份之间未发现无法共同执行的明文互否。
- 可溯源 / 安检例外：PGM 与 BSM L522、L1993 一致。
- 全中文四例外：PGM 与两份中文正文一致；本模块 HTML 源码标签/class 属实现，不是展示文案。
- 终端开关缺行默认关：PGM L736 与 BSM L1999 一致。
- 金额精度：PGM 对跨域尾差指定售票收款主体；BSM L1726 指定实际承担结算责任主体。跨域组合通票收款主体为娱乐平台公司，两句可共同执行。
- 考勤：PGM 含考勤/打卡字样属治理层交叉，不把 M002 专章展开为本模块缺陷。
- PGM 工作区相对 HEAD 有改、决策台账未同步：见 §8，不折进 M001 两文件。

## 7. ponytail-review（只列可删，不改）

- 两处 `td.k`「合作模式」：不是死代码。短列与带模板约束分属不同章。`delete:` 不成立。
- BSM 页内 `script`：27 个目录高亮，有可观察用途。`yagni:` 不成立。
- BSM-003…006 段末 LF：锁摘录少 1 换行，不是可删业务句。
- `Lean already. Ship.` `net: -0 lines possible.`

## 8. 全局门禁归属（不折进本模块文件缺陷）

| 门禁 | 本回合结果 | 归属 |
|---|---|---|
| 锁实算 / sidecar / ledger 顶部 | 全等（见本段第一行） | 闸 1 输入；本席第四值即本段首行。B5 后四值合取由根席在两席 raw 落齐后做，本席不写 ledger |
| `verify-entry-anchors` | PASS（本轮新跑，pass 1 / fail 0） | 入口锚点测试；现绿 |
| `verify-html-ledger-sync` | FAIL（PGM 相对 origin 改了、台账未同步） | 治理 HTML + `docs/prd-audit/business-decision-ledger.md`（非 M001 FILES） |
| 两份业务 HTML vs HEAD | 无 diff | 本模块 |

不声称全局绿。PGM 决策台账门禁红仍属其它模块。

## 9. 本次 findings（临时 GR）

**当前发现集合：空。**

无 GR-01、无第二、第三 GR。分类/冻结依据/最小处置：无对象。不申请改三份 HTML。不造 M001-017。不挪用 012/013/016。不把 M009 代码塞入。不新加判据。

四分类走完：冲突 0（新）→ 细化 0（新缺陷）→ 技术性无新缺陷 → 添加 0。

## 10. 步骤结论（不是模块 PASS）

单席 B5 收口条件：全文件覆盖、335 条逐项核对、三线均跑、四类证据、无未答 questions、无未定稿缺陷、独占哈希行有效。

本轮 335 条均满足冻结原文与现行模块文件一致；新 finding 空集；队列保持空；B3 累计 4。

**本席 B5：PASS。**

- **新 HTML 问题：无**
- **本票不是模块 PASS。** 模块生命周期仍为 ledger 所载 `IN_PROGRESS`。B5 双 PASS 且四值校验是收口前置，本席不得先记模块 PASS，不得写 `M001-*.md` 收口文件。
- 跳过新的 B3/B4；B3 累计保持 4；无第五次修复额度。
- 未改任何禁止路径。报告追加本文件 V0.02。其后无第二段独占锁哈希行。
