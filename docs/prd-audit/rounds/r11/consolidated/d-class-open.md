# R11 · D 类：证据未闭合项（还差什么证据 + 下一步查哪里）

基线 `1c5c0c7d`。

**D 类 ≠ B 类**：D 是「**没查清**」，B 是「**查清了但须用户在两个合法方向间择一**」。
本轮已按此界把 3 项从各 shard 的「待裁」中拆出改判为 D（见文末「从 B 拆出的项」）。

---

## D-01 ｜ P0 ｜ 代码轮输入报告是否用过「`value_flow` 非空 ⇒ 涉钱」做筛选

**差什么证据**：`rounds/final/code-round-input-money.md` 与 `code-round-input-nonmoney.md`
两份报告的**钱路/非钱路分流依据**未知。

**为什么要紧**：实测账本 `value_flow` 非空的 406 格里有 **204 格（50.2%）只是 `source_text` 逐字副本**，
不构成资金流陈述（C-4）。若这两份报告用该列做过分流，其钱路清单会**虚高约一半**，
而这两份报告已经作为**代码轮的输入**发出（记忆项：「代码轮输入报告已入库」）。

**下一步查哪里**（具体到可执行）：
1. `git show 1c5c0c7d:docs/prd-audit/rounds/final/code-round-input-money.md` 的方法/口径章，
   检索 `value_flow`／`非空`／`涉钱判定`／`筛选`；
2. 同法查 `code-round-input-nonmoney.md`；
3. `git show 1c5c0c7d:docs/prd-audit/rounds/final/worklog.md` 检索这两份报告的生成过程；
4. 若命中，把两份报告的钱路条目清单与「204 格副本行」的 `rule_id` 取交集，
   交集即为需重判的条目。

**谁未做**：shard-04 与 shard-08 均声明超出授权范围未核；本 Consolidator 亦未核（写权限只在 `consolidated/`，
且核这个需要读并可能重算两份报告，超出归并职责）。

**⚠ 不得包装成 B**：这不是「请用户在两个方案间选」，是「我们还不知道事实」。
查清之后可能根本不需要用户裁决（若两份报告根本没用该列筛，此项直接关闭）。

---

## D-02 ｜ P1 ｜ 账本 source refresh 工装的输入基线（回滚风险）

**差什么证据**：**没有**「source refresh 流程」的可执行脚本或流程文档，
只有一份输出快照 `docs/prd-audit/rounds/r9/ledger-source-refresh.csv`。

**已确证的事实**（shard-08 之外由 shard-06 EV-06-06 实读）：
该快照 `:121`（`PGM-0089`）存的是 **R9 时点的 PGM 旧文**，与现文差异**远不止数词**——
「跨商户跨**综合体**」（现文＝跨项目）、「敏感数据（成本价/内部结算价）」
（现文＝敏感经营数据的查看/导出/修改）、「高危业务（…/**人工放行**）」（现文已无「人工放行」）、
「税费规则与税费调整」当时**尚不存在**、「审批人按权限风险等级确定（上一级/上两级/最高），
复用主管复核三档」**现文已删**。

**风险**：若日后再跑一次 source refresh 并以该快照作为比对/回填基线，
会把 `authority-rule-ledger.csv:620` 已订正的内容**静默回滚**，且因两侧都「自洽」而不报错。
（同型于记忆铁律「校验脚本与修改脚本共用盲区＝自证无效」。）

**下一步查哪里**：
1. `git log --all --diff-filter=A -- docs/prd-audit/rounds/r9/ledger-source-refresh.csv` 找出生成它的提交；
2. 该提交内是否附带脚本？若无，查 `docs/prd-audit/rounds/r9/worklog.md` 的复现说明；
3. `.160` 上 `/tmp/sync1/`、`~/ops/` 下是否有残留的 refresh 脚本
   （shard-08 已实地 `ls /tmp/sync1/` 确认只有 `apply-ledger.mjs` 等**改写**脚本，
   **扫描/刷新判定式不在其中**）；
4. 若确认无脚本 → 该项从「工装风险」改判为「**流程从未机读化**」，
   处置由「修工装」改为「**写死流程要求：refresh 必须以三份 HTML 现文为唯一输入重算**」。

**已收窄的部分**：shard-06 实读后已把「会被工装再读」的风险名单从 3 个文件
收窄为 `rounds/r9/ledger-source-refresh.csv` **一处**——
`rounds/regext/draft-02.jsonl:10` 与 `draft-09.jsonl:22` 的「四类」命中经实读**不在 PGM-0089 条目上**
（`draft-02.jsonl:10` 是 `BSM-E0053`「平台管理员与最高权限是两个不同的角色」的长行内命中），属假阳性。

**不改任何快照文件**（改了就毁掉历史留痕的价值）。

---

## D-03 ｜ P1 ｜ sync1 灵敏钱路判定式不可复现

**差什么证据**：判定式**没有任何机读留存**。全库唯一记录是
`rounds/sync1/worklog.md:139` 的一句散文词表。

**两处不可复现**（shard-08 实测）：
- 「**首版**判定式本身全库零记录」——只说了在它基础上「补」，没说它是什么；
- 「把『计入』『归…』**放宽**」是操作描述不是模式，`归…` 的省略号无定义。

**重建后实跑结果**（证明重建式不可用）：
```
sync1-end non-ORPHANED NOT_STATED: 866   (worklog 隐含 866  ✅ 对上)
predicate hits: 236                       (worklog 记 206    ❌ 差 30)
hits == sync1-199 list? False
  in199 not hit: 1  ['BSM-0768']          ← 重建式漏掉他们命中的 1 行
  hit not in199: 38
```
→ **重建式既非其超集也非其子集**，不能用来划分 667/199。

**下一步查哪里**（两条路，都不在仓里）：
1. sync1 那一轮的扫描脚本原件（`.160` 的 `/tmp/sync1/` 已 `ls` 确认无扫描判定式）；
2. sync1 那批 **206 行命中清单**（worklog 只落了残余 199，命中的另 7 行点名在正文里，
   被回填的 20+7 行有表，**唯独 206 的全集没落**）。

**已给出的绕开方案（不必等这条查清）**：shard-08 建议**放弃复现**，改为全池人工判定
（669 与 667 只差 2 行，过滤毫无收益）。故本项**不阻塞** G-1，只影响历史可追溯性。

---

## D-04 ｜ P2 ｜ 5 个 finding 的 `file`/`line` 不可解析

**差什么证据**（shard-07a 实测，46 行代码类 open finding 中）：

- `line` 列为空或 `-`（`implementation_evidence` 只能填到文件级）：
  `R10-O-02`（`tools/generate-partial-requirement-mock-coverage.mjs`）、
  `FINAL-02`、`FINAL-03`
- `file` 列不是可解析路径（写的是自然语言描述）：
  `FINAL-02` = `backend（通票分账瀑布 S1~S11）`、
  `FINAL-03` = `backend（酒吧入场可分配池排名档位）`

**下一步查哪里**：
- `FINAL-02`（票务订单积分发行成本在域间拆分前扣除且只扣一次）→ 查
  `backend/src/main/java/com/complex/platform/settlement/TicketDistributionSettlementService.java`
  的 S1~S11 瀑布各段，与 shard-07b P0-4 同一文件，**建议与 P0-4 合批定位**；
- `FINAL-03`（酒吧入场可分配池排名档位）→ **shard-07b P0-4 已经把它定位到了**：
  `TicketDistributionSettlementService.java:445-451/460/581-588` +
  `JdbcSettlementBillRepository.java:1189 RankTier`。
  **即 FINAL-03 的 file/line 现在可以填实了**，本项对 FINAL-03 可直接关闭。
- `R10-O-02` → 该 tools 脚本全文较短，逐行定位即可。

**注**：全部 46 行的 `file` 列均非空（findings.csv 中无 `file` 为空的代码类 open 行）。

---

## D-05 ｜ P2 ｜ 游乐人工放行链是否真的绕过规则校验（链 3 未逐行追）

**差什么证据**：shard-07b 实测证实链 3 存在且可达
（`AmusementController.java:39/45` route + `V014:123/133` 建表 `amusement_manual_overrides`
`CHECK (override_type IN ('allow','deny','pause','resume'))` +
`AmusementService.java:120` `@GuardedAction(capability="amusement.verification.override",
admissionAction="amusement.manual_override")` wiring），
但**未逐行追它是否绕过规则校验**（链 1 已确证不跑任何核销规则校验，链 3 未确证）。

**下一步查哪里**：
1. `AmusementService.java` 中处理 `manual_override` 的方法体，看是否在写 override 前执行容量/安全/资格校验；
2. `amusement_manual_overrides` 表的写入是否会反向影响核销记录状态（对照链 2 只产出审批决策记录、不回写）；
3. `capability = "amusement.verification.override"` 的权限种子授给了哪些角色（比照链 1 的 `role_platform_admin`）。

**不影响 C-6 的合并结论**：口径禁的是「**任何岗位、任何权限都不能**以人工方式让…通过」，
链 3 的存在本身已构成业务面矛盾；本项只影响链 3 的严重度定级精度。

---

## D-06 ｜ P2 ｜ `verify:closed-loop-runtime` 等 39 条子闸的最近一次真实执行时点

**差什么证据**：shard-07c 确证 `verify:delivery-gates` 独有的 39 条子闸
在 CI / 部署脚本 / pre-push **三个执行点都不跑**（三点跑的都是 `verify:predeploy` 同一份 15 条子集），
且 `verify:closed-loop-runtime` **不在任何聚合闸口里**，只能人手敲。

**已知的最后一次记录**：`docs/closed-loop-live-acceptance-audit-20260618.md:13,27` 记
「`verify:delivery-gates` exit 0 通过」，是 **2026-06-18 的一次性人工跑**。

**差的是**：2026-06-18 之后（代码改了约两个月）有没有任何一次真实执行。

**下一步查哪里**：
1. `.160` 上 `~/ops/` 与部署日志，检索 `delivery-gates` 的执行记录；
2. `docs/runtime-acceptance-evidence-latest.json` 的 `captured_command` 与时间戳；
3. `tools/verify-final-closed-loop-audit.mjs:122,160` —— 注意它把
   `"verify:closed-loop-runtime"` 当**字符串**去 `requireRegex` 匹配验收文档正文，
   **校验的是「文档里写没写这条命令」，不是「这条命令跑没跑过」**，
   且这个校验器自己又在 CI 之外。**不能拿它的绿当执行证据。**

---

## D-07 ｜ P2 ｜ `R8-U-02` 未登记条目数：161 还是 233

**差什么证据**：两处数字不一致。
- `findings.csv:146` `R8-U-02` 逐字：「重锚后仍有 **161 行** HTML 内容无账本落点，本轮只补任务书点名部分」；
- shard-07c EV-07c-14 的缺口表与记忆项均写「R9 遗留 **233 条**未登记」。

**下一步查哪里**：
1. `scripts/out/unregistered-lines.txt`（`R8-U-02` 自己点名的证据文件）实际行数；
2. `docs/prd-audit/rounds/r9/` 下的条目注册表扩登记记录（`DEC-R8-69` 记「注册表差集 226 条中
   判为规则 134 条、非规则 92 条」——**226 又是第三个数**）；
3. 三个数（161 / 226 / 233）分别对应哪个时点与哪个口径，须归因后才能用于
   「门禁⑤保护面」的量化陈述。

**影响**：`verify-entry-anchors.mjs` 检查⑤自述「未登记不在保护范围」，
该豁免面的规模是引文门禁立项（R11-07c-A）的规模判据之一，数不准会影响优先级排序。

---

## D-08 ｜ P2 ｜ shard-08 的 0.56% 失配率是否幸存者偏差

**差什么证据**：shard-08 的「非 `trigger` 列只有 5 格片段级失配（5/886 = 0.56%）」结论
**只覆盖 `norm` 后 ≥12 字的格**。短格（尤其 `trigger` 之外的短枚举片段）**没有可靠检法**——
低于 12 字的片段在 6 万字 HTML 里必然偶然命中，检了等于没检。

**shard-08 已自陈**：「这个结论的置信范围止于 ≥12 字」。

**下一步查哪里**：
1. 统计 9 个镜像列中 `norm` 后 <12 字的非哨兵格数（分母未知，先量化盲区规模）；
2. 对这批短格抽样人工比对，估算真实失配率；
3. 若盲区规模小（如 <50 格），可直接全量人工核而不必设阈值。

**不阻塞**：路线①（推荐方案）已要求在门禁 PASS 文案里**明写豁免范围**
（照检查⑤ 现有做法「未登记条目不在保护范围，见 R8-U-02」），
故盲区是**已声明的已知上限**而非隐藏缺陷。

---

## D-09 ｜ P1 ｜ 三项大工程本身（G-1 / G-2 / G-3）—— deferred 到专项批

**性质**：不是「证据不足」，是「规模超出单轮容量」，按 D 类登记以免被当作本轮遗漏。

| 项 | 规模判据 | 估工 | deferred 安全性（本轮不做为何不出错） |
|---|---|---|---|
| **G-1** FINAL-05 `value_flow` 缺值 | 669 行需逐行人工语义判定；669 行原文约 15 万字符，单会话必然截断 | 5 agent × 2 轮 = 10 会话（另 +1 会话做 G-1-P0 定口径） | 该列**不是 PRD 修复的输入**；现行禁令「不得据缺值推断 HTML 未写钱路」是**保守方向**，只导致漏查不导致误改；缺值是「保守缺失」不是「错误断言」 |
| **G-2** FINAL-06 九个镜像列无机检 | 同表混存两种语义（`trigger` 合成 vs 其余逐字）无字段区分；路线②需重判 1718 格 | 路线①（推荐）1 agent × 1 轮；路线② 3~6 agent × 2 轮 | `source_text` 列**今天实测失配 0**，而那正是引 HTML 原话的唯一一列；9 个镜像列不是 PRD 修复输入；非 `trigger` 列实测只有 5 格失配且**无一格是内容错误** |
| **G-3** R8-U-04 逐句核验 | 涉 16 份 PRD（扩到 57 条则 18 份）；每条需读 HTML 原文 + 通读数千行 PRD | 6 agent × 2 轮 = 12 会话 | `PARTIAL` 本身就是「未确认」标签、方向保守；`recommended_change` 已把处置写在行内；性质是「PRD 可能缺口径」不是「PRD 写错了」，而本轮改 PRD 只会**缩小**这批缺口 |

**合计约 26 个 agent-会话**，明显超出单轮容量 → **defer 正确**。

**建议顺序**（shard-08 论证，Consolidator 确认）：
`G-2` → `G-1-P0` → `G-1 主体` → `G-3`。
理由：G-2 最便宜且落地后 `value_flow` 进入机检保护，G-1 回填成果从第一天起有兜底；
反过来先做 G-1，那 669 格在 G-2 落地前是裸奔的。

**三项里唯一建议本轮就动的**：G-3 的 12 条 FULL 降级或加注 → **已升入 B-10**，
不在此 defer。

**⚠ 已隔离的两个具体口子（本轮必须遵守，否则 defer 不安全）**：
1. `BEQ-0117`（分红代扣个税 20%）是确凿钱路缺值且**从未被扫过**——
   本轮 PRD 修复若触及分红税务，**必须直接读 ENUM 现文，不得查账本**；
2. `BEQ-0151/0152/0154/0156/0157`、`BSM-0534/0536` 这 7 条挂着 FULL 绿标但过半正文未参与判定——
   本轮若触及，**不得凭 FULL 标签跳过，必须回读 HTML 现文**。

---

# 从 B 类拆出改判为 D 的项（Evidence Gap 不得包装成「需用户裁决」）

按任务书第 4 条要求逐 shard 检查了「有没有把『没查清』包装成『需用户裁决』」，实测：

| 项 | 原提出方 | 原标 | 改判 | 判据 |
|---|---|---|---|---|
| `code-round-input-*.md` 是否用过 value_flow 筛选 | shard-08「交回编排者的待办」第 1 项 | 待办（未标 B） | **D-01** | 提出方**未**标为待裁，措辞规范。本 Consolidator 予以确认并正式登记为 D。 |
| source refresh 工装回滚风险 | shard-06 EV-06-06 | `disposition=D` | **D-02**（维持） | 提出方已自标 D，措辞规范：「具体门禁设计超出本 shard 范围，仅登记」。 |
| sync1 判定式不可复现 | shard-08 G-1 第 4 点 | 如实报告 | **D-03** | 提出方标题即「**复现失败，如实报告**」，未包装。 |

**结论：本轮 10 份 shard 中，未发现任何一份把 Evidence Gap 包装成 B 类的情形。**
三份涉及未闭合证据的 shard（06/08）均主动标注了 D 或「如实报告」。

**反向核查**：也检查了有没有把本该 B 的写成 A（更危险的方向）——发现 **1 例已修正**：
shard-06 EV-06-04 第 16 项②自标 `disposition=C`「建议由 R11 编排定」，
但其两种写法（改「五类」vs 保留「四类」加限定）涉及历史事实陈述的准确性，
编排无权代用户定，**已升入 B-07**。
另 **1 例反向修正**：shard-07c EV-07c-11 自标「须裁决」，
实查三处后确认已由 `DEC-R4-2026-08-04-43`／`DEC-R8-08`／`DEC-R8-46` 三度裁定，
**降为 A（代码轮下线）**，不占问询预算。
