# R11 worklog

> 本文件由 R11 Implementer-2（阶段⑦）撰写。写权限只在四个新建文件，未改任何既有文件，未 commit / push。
> 凡本文件中的数字，除明确标注「编排者转述」外，均为本轮在 `~/wt-r11` 实测。

---

## 1 · 基线

```
AUDIT_BASE_SHA          = 1c5c0c7d812349497240bbf57655b7d34d1e4442
                          （chore: 将 CLAUDE.md 纳入版本控制 (#351)）
LAST_ACCEPTED_AUDIT_SHA = 5d004374…（Merge pull request #349 from jxh1979/docs/audit-closeout
                          「文档审计收官批——value_flow 清账、镜像列重取、锚点清理、铁律对表」）
worktree                = ~/wt-r11        分支 = docs/prd-audit-r11
```

`git log --oneline` 实测链：`1c5c0c7d` → `4f28a314`(#350) → `7c54ded9`(两份代码轮输入报告入库) →
`5d004374`(#349) → `f4be2382` → `81fdc81b`(#348) → `a0d805d9` → `34f955b0`(#347)。

三份 HTML @`1c5c0c7d` 实测行数：BSM 1881 / PGM 889 / BEQ 256。

### 1.1 临时例外声明（写在前面，因为它会改变后续所有 agent 的判读）

> **`docs/prd-audit/rounds/r8/findings.csv` 虽位于历史轮次目录 `rounds/r8/` 之下，
> 当前仍是 live canonical ledger（活动缺口台账），是「历史 rounds 目录零 diff」规则的唯一例外。**

配套索引已新建：`docs/prd-audit/CANONICAL_LEDGER_INDEX.md`。
迁移到 `docs/prd-audit/findings.csv` 记为 `MIGRATION_DEFERRED: R12+`，迁移面见 §7。

---

## 2 · shard 清单（10 份）

| shard | 行数（实测） | 主题 |
|---|---:|---|
| shard-01 | 601 | 承接矩阵 13 行 MISSING 与账实不符、GAP-21 |
| shard-02 | 560 | 积分域 DEC-POINTS 索引读反、企微合法性论证 |
| shard-03 | 300 | points PRD 两处零承接 + 算术检查 |
| shard-04 | 338 | finance PRD 清账纪律零承接 |
| shard-05 | 620 | spec 侧 12 条 FINAL（54 个 EV） |
| shard-06 | 855 | 「四类→五类」下游同步 + face 四字段落点 |
| **shard-07a** | 73 | findings.csv 代码类 open 46 行机械提取 |
| **shard-07b** | 526 | 六项 P0 代码位置复核 |
| **shard-07c** | 400 | 仓外 scratch 三类未登记项复核 |
| shard-08 | 959 | 三个大工程（G-1/G-2/G-3）方案 |

**S7 拆三份的原因**：S7 原为一份，连续两次 API stall（各约 600s 无进展）。
根因＝**单 shard 同时扛三个互不相干的数据源**（① findings.csv 表格提取、② 六项 P0 的代码全库取证、
③ 仓外 scratch 目录复核），三条取数路径各自都要全量扫描，上下文与工具调用互相挤占。
按数据来源拆成 07a／07b／07c 后三份全部一次通过。

---

## 3 · 用户裁决 11 条（B-01 ～ B-11）· 全部已裁

问询清单原件在 `docs/prd-audit/rounds/r11/consolidated/b-class-questions.md`（404 行，实测）。
裁决时间：**2026-08-07，编排对话内口头裁定**，由编排者于本轮补传全文。

> **本节的传达轨迹须留痕**：裁决最初只写进 Implementer-1 的任务书，漏给 Implementer-2；
> 本 Implementer 第一版 worklog 曾据实把其中 10 条记为「未传达（未核实）」，
> 编排者确认为编排失误并补传后改为本节。**记此轨迹是为了让 R12 知道
> 「口头裁定 + 分头下发任务书」这条路径会漏，不是为了追责。**

| # | P | 主题 | **裁决结论** |
|---|---|---|---|
| B-01 | P0 | `face_verify_result` 值域 ＋ 两个「默认」 | 值域收敛为**两档：已满 18 岁 / 未满 18 岁**，删「不确定」。原始人脸照片是否保留**取决于第三方人脸识别 SDK 允不允许**——允许就按两档分类保留，不允许就不保留，**平台不自设开关**。BSM:873「采集前须单独取得同意」义务保留，不因第三方允许保留而免除。<br>用户原话：「它只有两个结果，已满18岁和未满18岁…如果第三方人脸系统允许我们保留照片那我们就保留…如果不允许我们保留我们就不保留，这是第三方SDK的事」 |
| B-02 | P0 🔴**铁律** | 年龄核验降级：HTML「抽查放行」vs spec/kit「阻断入场」 | **设备缺失或故障时一律人工抽查放行，客人继续进场。所有文档必须与此对齐。**<br>用户原话：「这个问题已经回答过很多遍了，你把它记成最重要的铁律，所有文档必须跟它对齐人工抽查放行」<br>⚠ **该口径此前只存在于口头裁决**——决策台账检索 `阻断入场`／`供应商不可用`／`供应商超时` **全部零命中**，这正是它反复被当成待裁项的根因。**本轮必须落台账（新 DEC），不得继续只存在于对话。**（落台账动作不在本 Implementer 写权限内，转交编排/Implementer-1） |
| B-03 | P0 🔴**铁律** | `DEC-CFG-2026-08-03-07` 与 `DEC-R8-83` 是否构成取代 | **年龄核验没有开关，必须有年龄核验，必须有人核验。** 设备坏了→人工核验；设备没到齐→人工核验。<br>用户原话：「就是年龄核验是没有开关的，必须有年龄核验，设备坏了就人工核验设备没到齐也是人工核验，但是必须有人核验。这也是个铁律对齐」<br>台账处置：`DEC-CFG-2026-08-03-07` **加括注**——其「刷脸开关本就允许关闭」表述已被 `DEC-R8-83` 推翻，但「不进例外清单」结论仍有效（即 B-03 问询稿的甲案方向） |
| B-04 | P1 | `GAP-21` 的 `status` | **甲**——改 `ruled-pending-implementation`，**不新增状态值**；`disposition` 写明「已并入 GAP-28、不再单独统计」 |
| B-05 | P1 | `FINAL-26③` 定性与 J3 前提 | **甲**——`FINAL-26③` 改判为「**缺条**」，删「方向相反」半句；J3 由「删列＋迁移」改为「**保留字段 ＋ 补齐约束 ＋ 补缺条**」 |
| B-06 | P1 | 补写「人工抽查期间三不保存」的措辞 | **甲**——按 `BSM:1024` **逐字转写，零自创**（决策台账 §18 `:355-360` 有现成中文转写版可直接照抄） |
| B-07 | P1 | `group-governance-rebuild-prd-v1.0.md:58` 历史陈述 | **不删也不改**——那句历史陈述保持「这四类」原样、**不加限定词**（即甲乙丙三案均不取，维持原文）。理由与逐条留证见 §4 |
| B-08 | P2 | 积分结算「一个月以内完成」目标 | 给**出账日**加一道校验，比照 `DEC-R8-01` 付账日同构闸门：「出账日须配置为使本账期存在满足顺序的付账日，否则拒绝保存」。**本轮只改 PRD 文本，不改代码** |
| B-09 | P1 | 账本三条 schema／口径变更 | **三条一起批**——(a) 新增禁令「不得据 `value_flow` 非空推断该条涉钱」＋机检硬约束；(b) `trigger` 列语义约定为「**合成描述、不承诺逐字**」；(c) 新增 `UNVERIFIED`／`CONFLICT` 两个 coverage 状态。<br>⚠ **编排层补充执行口径**：**204 行副本不在本轮清理**；机检按本项目既有 `param-tier` **棘轮范式**落地——建 204 行基线，**门禁只拦新增不拦存量，基线只缩不增**。清理留 **G-1 专项批** |
| B-10 | P0 | 12 条 `FULL` 是否本轮降级 | **甲**——12 条 FULL **本轮就降级**为 `PARTIAL`，同批注明是**口径纠正、非新增缺口**。承接矩阵 `PARTIAL` **74 → 86**（本轮实测降级前 `PARTIAL`＝74，与该目标值自洽） |
| B-11 | P2 | `REGX-11` 插入行标注体例 | **甲**——`finance-settlement-product-prd-v0.1.md` §4B.4 插入行**不加来源标注**（同节现有两条 bullet 均无标注，属原始转写补漏而非改写） |

> **B-10 计数冲销说明（2026-08-07 R11 返工补记）**：B-10 裁决记的目标值是 `PARTIAL 74 → 86`，
> 本轮落地后 `docs/prd-audit/html-prd-coverage.csv` **实测 `PARTIAL = 85`**，差 1 已核实、**非漏改**。
> 差额来自同批另一笔独立改动：`BSM-0566`（内部工单）由 `PARTIAL` 升为 `FULL`——
> `platform-governance-prd-v0.1.md` §15E.2 已对全部内部工单逐字承接闭环约束
> （2026-08-07 DOCFIX 批，PR #345 / commit `21256abd`），原判残余缺口不再成立，
> 该升级自身有据、与 B-10 的 12 条降级互不相干。
> 对账：**74（基线实测）＋ 12（B-10 降级）− 1（BSM-0566 升 FULL）＝ 85**，账实相符。
> 同口径复核基线 `1c5c0c7d` 与本轮落地后的四态分布：
> `FULL 1078 → 1077`、`PARTIAL 74 → 85`、`STALE 69 → 69`、`MISSING 13 → 3`，总行数 1234 不变。

### 本 Implementer 与这 11 条的关系（划界）

上述裁决**全部落在既有文件上**（三份 HTML／四张台账／PRD／spec／合规文档），
而本 Implementer 的写权限只有四个新建文件。**故本节只做记录，未执行任何一条。**
执行归 Implementer-1 与编排者；本节可作为其落笔清单与 Verifier 的对表基准。

**其中两条须编排者额外确认已落地，否则本轮等于没做**：
1. **B-02 的铁律必须落进决策台账**（新 DEC）——它今天在台账里仍是零命中；
2. **B-03 的 `DEC-CFG-2026-08-03-07` 括注**——不加则该行与 `DEC-R8-83` 继续并存且无射程说明，
   下游按「取日期较新者」的常规启发式判读仍会得到不确定结果。

---

## 4 · B-07 的「不改」理由（逐条留证）

**裁决：不删也不改**——`docs/group-governance-rebuild-prd-v1.0.md:58` 那句历史陈述保持「这四类」原样，
**既不随 `DEC-R8-84` 同步改「五类」，也不加限定词**。
即问询稿的甲（照改五类）／乙（保留四类＋加「当时已有的」限定）／丙（整句改写去数词）**三案均不取，维持原文**。

用户原话：「这是研发环境，系统还没有上线呢影响没有那么大。删和不删」
（与铁律记忆一致：`.160` 是研发/测试环境，不为存量数据做兼容设计；此处同理——
不为一句历史陈述加防御性限定词。）

理由：**该句是历史事实陈述，不是现行口径陈述。**

**基线原文**（`git show 1c5c0c7d:docs/group-governance-rebuild-prd-v1.0.md | sed -n '58p'`，逐字）：

> - 审批闸门：资金/核销/退款/调账/分红发放、提权或跨商户跨综合体、敏感数据、高危业务四类，**加上税费规则与税费调整**（共“四类 + 税费”）——上述权限授出前必须先审批才生效。**（2026-08-03 已收口）** 原 `access.grant_approval.enabled` 配置项可一票关闭**这四类**强制审批（且关闭时无审计事件、无启动告警，静默失效），属本文此前登记的待对齐缺口；现已从 `AccessService` 移除，审批闸门在生产恒开、无任何配置可关，仅测试构造器保留入参。

**工作区现状**（本轮实测，由并发作业的 Implementer-1 已改，见 §11）：同一行前半已改为
「……敏感数据、高危业务、**税费规则与税费调整**（共“五类”）」，
而后半的「可一票关闭**这四类**强制审批」**保持未动**——**与本裁决一致**。

三点支撑：

1. **句子主语是已被移除的旧配置项**（「原 `access.grant_approval.enabled`」「现已从 `AccessService` 移除」），
   陈述的是该开关**当年**能关掉什么，不是今天强制审批有几类。
2. **改成「五类」会扩大历史事实的陈述范围**：该开关是否也覆盖第五类（税费规则与税费调整）从未核实。
3. **事实上当年不存在税费类权限码**——同文件实测 `:66`（**注：B-07 问询稿与 shard-06 引作 `:65`，
   `:65` 实为第 5 类口径正文，「实现待对齐」块在 `:66`，此处引证订正**）逐字写：
   > **实现待对齐（2026-08-04 登记）**：当前实现**无税费类 `requires_approval=true` 权限码**
   > （全库 `*.sql`/`*.java` 检索无 `finance.tax` / `tax_rule` 命中）……按事实源优先级**不反向改写 PRD**。

   即：一个当年不存在的权限类别，不可能被当年的开关「一票关闭」。改数词即造假。

**同批注意（不属本裁决但同文件）**：基线里 `:59`「高危权限配置」句写「（四类 + 税费规则与税费调整）」、
`:60` 写「高危覆盖口径（四类 + 税费）：」，工作区现状均已改为「五类」（同为 Implementer-1 所改）。
即改毕后同一行内「五类」（现行口径）与「这四类」（历史事实）并存是**正确**的，不是自相矛盾；
本裁决要防的正是有人把后者也一并改掉。
`DEC-R8-84`（台账 `:1753`）本身只裁 PGM HTML 该条数词，未涉任何下游历史陈述句。

---

## 5 · D-01 结论 —— **已关闭**

**结论：两份代码轮输入报告 `docs/prd-audit/rounds/final/code-round-input-money.md` 与
`code-round-input-nonmoney.md` 从未用过 `value_flow` 列做钱路/非钱路分流。**

分流母体是 `docs/prd-audit/rounds/r8/findings.csv` 的 `status=open` 集合，共 **66** 行
（该文件在报告自述基线 `7da3aaa1` 时点 total 204 / open 66；16＋50＝66 吻合）。
逐字依据（两份报告全文仅此一句涉及分流）：

> 「findings open 66 条中 16 条归钱路组，本报告覆盖 50 条 + 2 项调查中新发现」
> —— `docs/prd-audit/rounds/final/code-round-input-nonmoney.md:5`

五条实证见 `consolidated/d01-resolution.md`（61 行，实测），要点：
findings.csv 表头 8 列无 `value_flow`；钱路报告通篇 0 个账本 `rule_id`；
非钱路报告出现的 ID 全是 `finding_id`；且钱路报告收录了一条 `value_flow=NOT_STATED` 的条目
（`BEQ-0117` 个税代扣安全阈值三审）作为反证。

### 必须同时记下的限制（不得写成「判据已查明」）

**两份报告都没有方法／口径／取数章节。**
检索 `value_flow`／`非空`／`涉钱`／`筛选`／`取数`／`判据`／`口径`／`方法`／`分流`：
钱路报告 0 命中，非钱路报告仅命中上引第 5 行那一句；
`docs/prd-audit/rounds/final/worklog.md`（627 行）全文检索「钱路」／`value_flow` 亦未记该拆分过程。

> **「这 66 条里哪 16 条归钱路组」所依据的具体判定规则，两份报告一字未写。**
> 本项按事实记为「报告未写方法」，不作推测。D-01 的原始担心（钱路清单被 204 格 `source_text`
> 逐字副本虚高约一半）在这两份报告上**不成立**，D-01 可关闭；但「分流规则不可复现」是**新的、独立的**限制。

---

## 6 · 本轮失败模式记录（供 R12）

| # | 现象 | 根因 | 处置 | 结果 |
|---|---|---|---|---|
| F-1 | **S7 两次 API stall**，各约 600s 无进展 | 单 shard 同时扛三个独立数据源（findings.csv 提取／六项 P0 代码取证／仓外 scratch 复核），三条全量扫描路径互相挤占 | 拆成 shard-07a / 07b / 07c，一份一个数据源 | 三份全部一次通过 |
| F-2 | **S6 一次 stall** | 同因：A-11「四类」全库检索 ＋ A-13 三处落点，两个独立子任务塞进一个 shard | 同上思路 | 完成 |
| F-3 | **shard-05 一次自造污染** | 用 `ssh "cat >> …"` 写文件时，内容里的反引号被**本地** shell 当命令替换执行 | 改用 stdin 管道（`ssh 'cat > path' < localfile`） | 解决；本 Implementer 全程沿用 stdin 管道，未再复现 |
| **F-4** | **并发 worktree 下工作区不是稳定取证面**（详见下方独立条目） | 同一 worktree 被 Implementer-1 并发写入，读工作区文件读到的是别人已改后的文本 | 取基线原文一律 `git show <BASE_SHA>:<path>` | 已发现并纠正；**须 Verifier 复核本轮所有引「基线原文」之处** |

### F-4（独立条目）· 并发取证陷阱 —— R12 必读

> **「并发 worktree 下工作区不是稳定取证面，同一命令先后两次可给出不同答案且不报错。」**

- **现象**：核 B-07 时执行 `sed -n '58p' docs/group-governance-rebuild-prd-v1.0.md`（读**工作区文件**），
  读到的是并发作业的 Implementer-1 **已改后**的文本（「共“五类”」），
  而基线 `1c5c0c7d` 该行实为「高危业务四类，**加上税费规则与税费调整**（共“四类 + 税费”）」。
- **后果**：差点把**工作区现状**当成**基线原文**写进 worklog。
  若未发现，B-07 的整段论证会建立在一个不存在的基线上——而 B-07 恰恰是一条
  「保持历史陈述原样」的裁决，用错基线正好会把结论推向相反方向。
- **为什么难发现**：命令**不报错**，输出格式完全正常，行号也对得上；
  唯一的异常信号是 `git status --porcelain` 从开工时的 1 项变成收工时的 15 项。
- **规避（写死）**：**凡要引「基线原文」一律走 `git show <BASE_SHA>:<path>`，禁止读工作区文件。**
  工作区文件只在「我要看别人改成什么样了」时才读，且必须显式标注这是工作区现状而非基线。
- **影响面**：**Verifier 须复核本轮所有引「基线原文」之处。**
  已自查：本 worklog 除 §4 那一处（已订正并双版对照）外，其余引证均走 `git show 1c5c0c7d:` 或
  `git grep … 1c5c0c7d`；`shard-06`／`shard-07b`／`shard-07c` 三份自陈用 `git show 1c5c0c7d:` 取证，
  未受影响。**`shard-01～05`、`shard-08` 的取证方式本 Implementer 未逐份核，属未核实项。**

> **教训（写给 R12 的一句话）：shard 粒度应按「单一数据来源」切，不按「主题」切。**
> 「人工放行」是一个主题，但它的证据分散在 findings 台账、backend 代码、apps/web 串、迁移种子、
> tools 门禁五个来源上——按主题切必然在一个 shard 里跑五种全量扫描，就是 F-1/F-2 的形状。
> 按来源切之后，跨来源的合并交给 Consolidator（本轮 `collisions.md` C-1～C-10 即此角色的产物）。

> **附带教训（F-3 的一般化）**：任何「把内容送到远端」的动作，只要内容含反引号、`$`、`!`，
> 都不要走命令行参数或非引号 heredoc；一律 stdin 管道。本轮实测：`ssh 'cat > f' <<'EOF'` 形式
> 在含 `"` 与全角标点的长 CSV 上仍会被本地 shell 解析失败（本 Implementer 实际撞到一次，
> 退出码 2 `unexpected EOF while looking for matching`），改为**本地落文件再管道**后一次成功。

---

## 7 · 转述数字被实测推翻的清单

全部为本轮 `~/wt-r11` @`1c5c0c7d` 实测（`csv.DictReader` 计数或 `wc -l`，非目测）。

| # | 项 | 转述值 | **实测值** | 实测方法 |
|---|---|---:|---:|---|
| N-1 | PRD 总行数 | 10999 | **10499** | `awk '{s+=$2}' rounds/r11/prd-scope-manifest.txt`（26 份 PRD 的行数列求和） |
| N-2 | findings.csv 数据行 | 208 | **233** | `wc -l` 234 − 表头 1；末字节为 `0a` 无截断 |
| N-3 | 「四类→五类」下游 | 5 处 | **11 个活文件 18 处** | shard-06 EV-06-02：`git grep -n "四类"` 447 → 域筛 86 → 逐条判读；FINAL-19 漏 6 个活文件（`CONTEXT.md`×2、`html-prd-coverage.csv:212`×2、`business-decision-ledger.md:21`、`AccessService.java:81`） |
| N-4 | `face_*` 四字段 | 9 处 | **27 次 / 11 行 / 6 文件** | shard-06 EV-06-08 三口径分别计数；「9 处」对不上任何一种口径 |
| N-5 | `face_*` 落点 | 3 处 | **4 处** | 合规 §2.2、合规矩阵 C1 行(:315)、`third-party-access-layer-spec` §8.1.2、`third-party-supplier-integration-kit:129` |
| N-6 | `phase=r8-rebuild` 新增 PARTIAL | 45 | **47** | 本轮复算：`sum(1 for r in coverage if r.phase=='r8-rebuild' and r.coverage_status=='PARTIAL')` = 47；45 是「且 anchor 含『14 字滑窗』」的更窄集合 |
| N-7 | `R8-U-02` 未登记条目 | 233 | **92（entry 口径）／242（门禁真实放弃面）** | 本轮复算：registry 1210 行中 `(html_file, entry_name)` 在 `authority-rule-ledger.csv` 无对应行者 **242**；其中 `entry_kind=='entry'` 者 **92**（另 150 为 `entry_kind=='section'`）。findings.csv:146 `R8-U-02` 自述的是第三个数「161 行 HTML 内容无账本落点」，`DEC-R8-69` 又记「注册表差集 226 条」——**四个数四个口径**，见 D-07 |

**顺带实测（本轮新测，非推翻）**：
- 三张 canonical 台账数据行：entry-registry **1210**（entry 1059 / section 151）、
  authority-rule-ledger **1225**、html-prd-coverage **1234**（FULL 1078 / PARTIAL 74 / STALE 69 / MISSING 13）。
- findings.csv `status` 分布：`fixed` 78 / `open` 75 / `resolved` 31 / `implemented` 28 /
  `fixed-in-rework` 8 / `fixed-in-r9` 5 / `ruled-pending-implementation` 3 / `closed-doc-layer` 2 /
  `in-progress` 1 / `fixed-in-r10` 1 / `accepted-by-user` 1。
- `docs/prd-audit` 树下 `*.csv` 共 **21** 个，git 已跟踪 **20** 个
  （另 1 个是 R11 本轮未提交的 `rounds/r11/consolidated/findings-candidates.csv`）。

### MIGRATION_DEFERRED 迁移面（`CANONICAL_LEDGER_INDEX.md` 回指本节）

实测 `git grep -l "rounds/r8/findings.csv" 1c5c0c7d`：**11 个文件、34 处引用**。

```
docs/prd-audit/business-decision-ledger.md               9 处
docs/prd-audit/rounds/r8/changelog-html-revision-b2.md   6 处
docs/prd-audit/rounds/r10/worklog.md                     4 处
docs/prd-audit/rounds/regext/worklog.md                  3 处
docs/prd-audit/rounds/r8/changelog-rework.md             3 处
docs/prd-audit/rounds/final/worklog.md                   3 处
docs/prd-audit/rounds/r8/changelog-html-revision.md      2 处
docs/prd-audit/rounds/r8/findings.csv                    1 处（自引）
docs/prd-audit/rounds/docfix/worklog.md                  1 处
docs/prd-audit/rounds/b6/worklog.md                      1 处
docs/pending-development-backlog-v0.1.md                 1 处
```

**关键实测：机读面为零。**
`git grep -n "findings.csv" 1c5c0c7d -- tools/ scripts/ package.json .github/` → **0 命中**。
即无任何门禁、脚本或 CI 配置以该路径取数，迁移不会打断任何自动检查；
但 11 个文件里有 7 个在 `rounds/` 历史目录下，改它们会与「历史 rounds 零 diff」规则冲突
——**这正是本轮 §1.1 例外声明存在的原因，也是把迁移 defer 到 R12+ 的原因**：
迁移必须与「历史 rounds 目录可否被改写」一并裁决，不能单独做。

---

## 8 · deferred 项：G-1 / G-2 / G-3

三项均为「规模超出单轮容量」而非「证据不足」，合计约 26 个 agent-会话（shard-08 估）。
建议顺序 `G-2 → G-1-P0 → G-1 主体 → G-3`（G-2 最便宜，落地后 `value_flow` 进入机检保护，
G-1 回填成果从第一天起有兜底；反过来先做 G-1，那 669 格在 G-2 落地前是裸奔的）。

| 项 | 内容 | 规模判据 | **为什么不做不影响本轮正确性** |
|---|---|---|---|
| **G-1** | `FINAL-05` `value_flow` 缺值回填（669 行逐行人工语义判定） | 669 行原文约 15 万字符，单会话必然截断；估 5 agent × 2 轮 ＋ 1 会话定口径 | 该列**不是 PRD 修复的输入**，本轮任何产出物都没有从 `value_flow` 取过数（D-01 已证两份代码轮输入报告同样没用过它）。现行禁令「不得据缺值推断 HTML 未写钱路」是**保守方向**，只导致漏查、不导致误改；缺值是「保守缺失」不是「错误断言」。 |
| **G-2** | `FINAL-06` 九个镜像列无机检 | 同表混存两种语义（`trigger` 合成 vs 其余逐字）无字段区分；路线②需重判 1718 格 | `source_text` 列本轮实测**失配 0**——而那正是引 HTML 原话的唯一一列，门禁⑤在这一列上确实起作用。9 个镜像列不是 PRD 修复输入；非 `trigger` 列实测只有 5 格片段级失配（`PGM-0041.preconditions`／`PGM-0186.permission`／`BSM-0552.evidence_required`／`BSM-0558.separation_of_duties`／`BSM-0558.evidence_required`），逐格看过**无一格是内容错误**，全是多段摘录拼接的技术性失配。 |
| **G-3** | `R8-U-04` 逐句核验（PARTIAL 行） | 涉 16 份 PRD（扩到 57 条则 18 份）；每条需读 HTML 原文＋通读数千行 PRD；估 6 agent × 2 轮 | `PARTIAL` 本身就是「未确认」标签，方向保守；`recommended_change` 已把处置写在行内；性质是「PRD 可能缺口径」不是「PRD 写错了」，而本轮改 PRD 只会**缩小**这批缺口。 |

### ⚠ defer 安全的两个前置条件（本轮及后续轮必须遵守，否则 defer 不成立）

1. **`BEQ-0117`**（分红代扣个税、安全阈值、三审、出厂默认 20%）是确凿钱路但 `value_flow=NOT_STATED`，
   且因 sync1 时为 SUPERSEDED、batch6 又改回 AUTHORITATIVE，**从未被任何判定式扫描过**。
   凡触及分红税务，**必须直接读 ENUM 现文，不得查账本**。
2. **`BEQ-0151/0152/0154/0156/0157`、`BSM-0534/0536`** 这 7 条挂 `FULL` 绿标但过半正文未参与过判定
   （最高 `BEQ-0152` 74% 未参与）。凡触及，**不得凭 FULL 标签跳过，必须回读 HTML 现文**。
   （此 7 条即 B-10 丙案所指。**B-10 已裁甲案**——12 条 FULL 本轮就降为 `PARTIAL`，
   故本条在 B-10 落地后自动失效：降级后它们不再挂 FULL 绿标，不存在「凭标签跳过」的口子。
   **在 B-10 落地之前，本条仍然有效。**）

**G-3 中唯一不 defer 的部分**：12 条 FULL 的降级或加注已升入 **B-10**，等用户裁决。

---

## 9 · 本轮产出物（四个新建文件，实测行数）

| 文件 | 行数 | 数据行 | 说明 |
|---|---:|---:|---|
| `docs/prd-audit/CANONICAL_LEDGER_INDEX.md` | 60 | — | canonical 台账索引 ＋ `rounds/r8/findings.csv` 例外声明 |
| `docs/prd-audit/rounds/r11/implementation-gaps.csv` | 72 | **71** | 8 列全行校验通过（`csv.DictReader` 每行 8 字段） |
| `docs/prd-audit/rounds/r11/invariant-registry.csv` | 25 | **18** | 5 列；`EXPLICIT` 14 / `DERIVED` 4；`status` 全 `CANDIDATE` |
| `docs/prd-audit/rounds/r11/worklog.md` | 本文件 | — | — |

### `implementation-gaps.csv` 的构成与已知限制

- **46 行**取自 `shard-07a`（findings.csv `status=open` 且属代码类），**字段逐行照抄未改写**。
- **6 行** `R11-P0-1～6` 取自 `shard-07b`（六项 P0 复核），带 `IMPLEMENTATION_ACTIVE`／`INERT` 标注。
- **19 行** `EV-07c-01～19` 取自 `shard-07c`（废词批 B 残留 ／ 引文门禁缺位 ／ delivery-gates 整链）。
- 合计 **71** 行。

**限制 1（如实保留，未编造）**：37 条 `GAP-xx` 在 findings.csv 里的 `file` 列全部指向同一份变更日志
`docs/prd-audit-fix-change-log-20260804.md:§7.4/§7.4A`，**没有任何代码路径**；
`affected_component` 机械推出即 `docs`（实测 71 行中 `docs` 39 行）。
本轮**未为它们编造代码位置**，保持 `docs`。真正带代码路径的 07a 行只有 4 条：
`FINAL-01`(SettlementPostingService.java:237)、`FINAL-04`(JdbcFinanceRepository.java:1071)、
`FINAL-06`／`FINAL-10`(tools/verify-entry-anchors.mjs)。
另 `FINAL-02`／`FINAL-03` 的 `file` 列写的是自然语言描述（`backend（通票分账瀑布 S1~S11）`／
`backend（酒吧入场可分配池排名档位）`），`R10-O-02`／`FINAL-02`／`FINAL-03` 的 `line` 列为 `-`
——即 D-04 登记的 5 个不可解析项，本轮**保持原样未补**。
（注：`FINAL-03` 已可由 `R11-P0-4` 行填实，两行之间以 `canonical_findings_id=FINAL-03` 关联。）

**限制 2（`severity` 词表混用，未强行归一）**：三个来源的 severity 词表不同——
07a 沿用 findings.csv 的 `important`／`info`／`advisory`，07b 用 `blocking-P0`，07c 用 `P1`／`P2`。
归一化需要一张跨表映射，而该映射本身是判断（`important` 是否等于 `P1`？无既有裁决）。
本轮**保持各来源原词**并在此登记，留待编排/用户定映射。

### `shard-07b` 三处引证订正已在本文件与 CSV 中采纳

1. `V270` 真名 = **`V270__ticket_manual_override_permission.sql`**
   （本轮实测 `git ls-tree -r --name-only 1c5c0c7d backend/src/main/resources/db/migration/ | grep V270` 唯一命中；
   shard-07c 正确，shard-07b 写的 `V270__verification_manual_override_permission.sql` 是笔误）。
2. 终端开关 DDL 在 **`V010__p0_foundation_residual_platform_terminal_evidence.sql:64-70`**
   （本轮实测：`:64` `CREATE TABLE IF NOT EXISTS terminal_capability_switches`，
   `:70` `enabled BOOLEAN NOT NULL`，**无 DEFAULT 子句**，整表至 `:75`），
   **不是** `V068:46`（该处 `enabled BOOLEAN NOT NULL DEFAULT true` 属 `supervisor_review_policies` 表）。
   实操含义：opt-out 语义**完全由 Java 层 `Optional.isPresent()` 产生**，
   修复点在 `TerminalSwitchGuard.java` 一处条件反转，**不能用「改 DB 默认值」来修**
   （缺行场景压根没有行，DEFAULT 永远不会被求值）；但反转后既有 capability 会立刻全部被拒，
   须同步补显式开启种子迁移，属「改一行、配一片」型。
3. 人工放行是**三条链**，不是两条：票务（`TicketingController.java:483-490`）＋
   治理督导（`SupervisorReviewService.java:36`）＋**游乐**（`AmusementController.java:39/45`、
   `AmusementService.java:120` `@GuardedAction(capability="amusement.verification.override")`、
   `V014__p0_amusement_safety_session_verification.sql:123/133`）。

### `invariant-registry.csv` 的纪律

- 18 条全部 `status=CANDIDATE`，**未经用户逐条批准前不得作为审计标尺、不得进门禁**。
- `EXPLICIT` 14 条全部给到 HTML 行号或 DEC 编号＋台账行号，本轮**逐条 grep 验证过出处存在**
  （BSM:899/1024/1386/1501/1574/1848、PGM:570/672/868、BEQ:41/49/154/207，
  台账 :1009/:1141/:1142/:1512/:1584）。
- `DERIVED` 4 条（INV-15 记账期归属、INV-16 新 capability 默认关、INV-17 负基数不得钳零、
  INV-18「永久」须可表达）**各自注明由哪两条 EXPLICIT 组合推导**；
  文件头已写明纪律：**DERIVED 即使推导看似必然也须单独批准**，
  理由是推导前提（两条规则的适用域是否真的相交）本身是判断，判断权在用户不在 agent。
- 文件头的纪律段用 `#` 前缀注释承载（CSV 无注释语义）。目前无任何脚本读取本文件，
  若后续进门禁，须先决定是跳过 `#` 行还是把纪律挪出文件。

---

## 10 · 本 Implementer 未做 / 未核实（如实登记）

1. **B-01～B-11 全部已裁并记入 §3，但本 Implementer 一条都未执行**——
   11 条裁决全部落在既有文件上（三份 HTML／四张台账／PRD／spec／合规文档），
   超出本 Implementer 的四文件写权限。执行归 Implementer-1 与编排者。
   其中 **B-02（人工抽查放行铁律须落新 DEC）** 与 **B-03（`DEC-CFG-2026-08-03-07` 加括注）**
   须编排者额外确认已落地，否则本轮等于没做。
2. 未修改任何既有文件；未 commit、未 push；未碰三份 HTML、四张台账、`business-decision-ledger.md`、
   `audit-shards/**`、`consolidated/**`、代码、配置、迁移。
3. 未核实：`shard-07b` P0-1 引用的 runtime 事实
   （`SETTLEMENT_MONTHLY_POSTING_ENABLED=true` 已 go-live、2026-08-01 已真落账）——
   该点为 `.160` 运行时环境事实，本轮未在 `1c5c0c7d` 代码内二次取证，`shard-07b` 自己也已如此声明。
4. 未核实：D-05（游乐人工放行链是否真的绕过规则校验）、D-06（39 条子闸最近一次真实执行时点）、
   D-07（`R8-U-02` 四个数的归因）、D-08（shard-08 的 0.56% 失配率是否幸存者偏差）——
   均为 D 类未闭合项，见 `consolidated/d-class-open.md`。
5. 未运行任何 lint／typecheck／测试／构建：本轮产出物是四个新建的 md/csv 文档，
   仓内**无任何门禁读取这四个路径**（实测 `git grep` 在 `tools/`、`scripts/`、`package.json`、
   `.github/` 下对 `implementation-gaps`／`invariant-registry`／`CANONICAL_LEDGER_INDEX` 零命中），
   故无可跑的验证命令。CSV 结构以 `csv.DictReader` 逐行字段数校验代替（全部通过）。

---

## 11 · 并发作业记录（本 Implementer 未改这些文件）

本 Implementer 全程**只对四个新路径执行 `cat >`**，下列 `M` 条目全部由**并发作业的 Implementer-1** 产生。

三次 `git status --porcelain` 实测快照（同一 worktree，同一轮内）：

| 时点 | `M` 条目数 |
|---|---:|
| 开工 | **0**（只有 `?? docs/prd-audit/rounds/r11/`） |
| 首次收工核对 | **14** |
| 补传裁决后再核对 | **19** |

> **数字在本轮内从 0 → 14 → 19，本身就是「工作区正在被并发写入」的直接证据**，
> 也是 F-4 成立的实证：任何以工作区为取证面的结论都有一个未声明的时间戳。

最后一次快照（19 `M` ＋ 2 `??`）：

```
 M CONTEXT.md
 M docs/bar-business-operations-product-prd-v0.1.md
 M docs/bar-operations-development-design.md
 M docs/business-html-source-of-truth-acceptance-spec-v0.1.md
 M docs/compliance-and-personal-information-protection-requirements-v0.1.md
 M docs/data-risk-ai-development-design.md
 M docs/finance-settlement-product-prd-v0.1.md
 M docs/foundation-database-design.md
 M docs/group-governance-rebuild-prd-v1.0.md
 M docs/member-points-p1-ops-parameter-tables-v0.1.md
 M docs/merchant-points-settlement-rules-v1.0.md
 M docs/platform-governance-prd-v0.1.md
 M docs/platform-governance-product-spec-v0.2.md
 M docs/points-and-voucher-product-prd-v1.0.md
 M docs/prd-audit/phases/03-governance-and-common.md
 M docs/product-requirements-v0.1.md
 M docs/requirement-traceability-matrix-v0.1.md
 M docs/third-party-access-layer-spec-v0.2.md
 M docs/third-party-supplier-integration-kit-v0.1.md
?? docs/prd-audit/CANONICAL_LEDGER_INDEX.md   ← 本 Implementer 新建
?? docs/prd-audit/rounds/r11/                 ← 含本 Implementer 新建的三个文件
```

**注意：本清单仍是一个时间点快照，Implementer-1 可能在其后继续写入。**
Verifier 应以自己取的 `git status` 与完整 `git diff` 为准，不要引用本清单当最终面。
另：四张 canonical 台账（`entry-registry.csv`／`authority-rule-ledger.csv`／`html-prd-coverage.csv`／
`rounds/r8/findings.csv`）在最后一次快照中**均未出现 `M`**——
而 B-04／B-09／B-10 三条裁决都要求改台账，**即这三条截至本 Implementer 收工时尚未落地**（如实登记，非结论）。

由此暴露的取证陷阱已**独立成条：见 §6 的 F-4**（含现象／后果／规避／影响面与 Verifier 复核要求）。
B-07 的基线原文与工作区现状两版逐字对照见 §4。
